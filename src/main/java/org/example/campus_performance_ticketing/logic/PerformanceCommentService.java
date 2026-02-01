package org.example.campus_performance_ticketing.logic;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.campus_performance_ticketing.dao.PerformanceCommentRepository;
import org.example.campus_performance_ticketing.dao.PerformanceRepository;
import org.example.campus_performance_ticketing.dao.PerformanceStatsRepository;
import org.example.campus_performance_ticketing.dao.UserRepository;
import org.example.campus_performance_ticketing.logic.dto.ApiResponse;
import org.example.campus_performance_ticketing.logic.dto.performance_comment.AuditCommentCmd;
import org.example.campus_performance_ticketing.logic.dto.performance_comment.CommentDto;
import org.example.campus_performance_ticketing.logic.dto.performance_comment.CreateCommentCmd;
import org.example.campus_performance_ticketing.model.Performance;
import org.example.campus_performance_ticketing.model.PerformanceComment;
import org.example.campus_performance_ticketing.model.PerformanceStats;
import org.example.campus_performance_ticketing.model.UserInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 演出评论业务服务
 */
@Slf4j
@Service
@Valid
@RequiredArgsConstructor
public class PerformanceCommentService {
    private final PerformanceCommentRepository commentRepository;
    private final PerformanceRepository performanceRepository;
    private final UserRepository userRepository;
    private final PerformanceStatsRepository statsRepository;

    @Value("${file.base.url}")
    private String baseUrl;

    // 定义允许观众互动的状态：1-已发布, 3-已结束(含回放)
    private static final List<Integer> VISIBLE_STATUSES = Arrays.asList(1, 3);

    // 系统角色常量
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_SUPER_ADMIN = "SUPER_ADMIN";

    // ================= 防刷配置 (本地内存版) =================
    // 频率限制 Map: Key=OpenId, Value=上次评论时间戳
    private final Map<String, Long> frequencyCache = new ConcurrentHashMap<>();
    // 内容限制 Map: Key=OpenId:MD5, Value=该内容发送时间戳
    private final Map<String, Long> contentCache = new ConcurrentHashMap<>();

    private static final long LIMIT_INTERVAL_MS = 60 * 1000L; // 60秒
    private static final long DUPLICATE_INTERVAL_MS = 5 * 60 * 1000L; // 5分钟

    /**
     * 用户发表评论
     */
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<Void> postComment(@NotBlank String userOpenId,
                                         @Valid CreateCommentCmd cmd) {
        try {
            long now = System.currentTimeMillis();

            // ================= 防刷校验 (本地内存) =================
            // 1. 频率校验
            Long lastTime = frequencyCache.get(userOpenId);
            if (lastTime != null && (now - lastTime) < LIMIT_INTERVAL_MS) {
                throw new SecurityException("您评论得太快了，请休息一下再试！");
            }

            // 2. 内容重复校验
            String contentHash = DigestUtils.md5DigestAsHex(cmd.getContent().trim().getBytes(StandardCharsets.UTF_8));
            String contentKey = userOpenId + ":" + contentHash;
            Long contentTime = contentCache.get(contentKey);

            if (contentTime != null && (now - contentTime) < DUPLICATE_INTERVAL_MS) {
                throw new SecurityException("请勿重复发送相同的评论内容！");
            }
            // ========================================================

            // 3. 用户校验
            UserInfo user = userRepository.findByOpenid(userOpenId)
                    .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

            if (Integer.valueOf(0).equals(user.getStatus())) {
                throw new SecurityException("您的账号状态异常，暂时无法发表评论");
            }

            // 4. 演出校验
            Performance performance = performanceRepository.findById(cmd.getPerformanceId())
                    .orElseThrow(() -> new IllegalArgumentException("演出不存在"));

            if (performance.getPublishStatus() == null || !VISIBLE_STATUSES.contains(performance.getPublishStatus())) {
                log.warn("用户尝试评论不可见演出: perfId={}, status={}", performance.getId(), performance.getPublishStatus());
                throw new IllegalArgumentException("该演出未公开或已下架，无法评论");
            }

            // 5. 保存评论
            PerformanceComment comment = new PerformanceComment();
            comment.setPerformance(performance);
            comment.setUser(user);
            comment.setContent(cmd.getContent());
            comment.setStatus(1);

            commentRepository.save(comment);

            // 6. 更新计数
            adjustCommentCount(performance, 1);

            // ================= 记录防刷缓存 =================
            frequencyCache.put(userOpenId, now);
            contentCache.put(contentKey, now);
            // ===============================================

            return ApiResponse.success(null);

        } catch (IllegalArgumentException | SecurityException e) {
            return ApiResponse.fail(e.getMessage());
        } catch (Exception e) {
            log.error("发表评论异常: userOpenId={}, cmd={}", userOpenId, cmd, e);
            return ApiResponse.fail("系统繁忙，请稍后再试");
        }
    }

    /**
     * 定时清理内存中的过期 Key (防止内存泄漏)
     * 每 1 分钟执行一次
     */
    @Scheduled(fixedRate = 60000)
    public void cleanUpCache() {
        long now = System.currentTimeMillis();

        // 清理频率缓存 (过期时间 60s)
        Iterator<Map.Entry<String, Long>> freqIt = frequencyCache.entrySet().iterator();
        while (freqIt.hasNext()) {
            if ((now - freqIt.next().getValue()) > LIMIT_INTERVAL_MS) {
                freqIt.remove();
            }
        }

        // 清理内容缓存 (过期时间 5min)
        Iterator<Map.Entry<String, Long>> contentIt = contentCache.entrySet().iterator();
        while (contentIt.hasNext()) {
            if ((now - contentIt.next().getValue()) > DUPLICATE_INTERVAL_MS) {
                contentIt.remove();
            }
        }
    }

    /**
     * 管理员/超管 审核评论 (修改状态)
     * 场景：隐藏违规评论，或恢复被误删的评论
     */
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<Void> auditComment(@NotBlank String operatorOpenId,
                                          @Valid AuditCommentCmd cmd) {
        try {
            UserInfo operator = userRepository.findByOpenid(operatorOpenId)
                    .orElseThrow(() -> new IllegalArgumentException("管理员不存在"));

            // 权限校验：必须是 ADMIN 或 SUPER_ADMIN
            if (!ROLE_ADMIN.equals(operator.getRole()) && !ROLE_SUPER_ADMIN.equals(operator.getRole())) {
                throw new SecurityException("无权操作");
            }

            PerformanceComment comment = commentRepository.findById(cmd.getCommentId())
                    .orElseThrow(() -> new IllegalArgumentException("评论不存在"));

            // 修改状态 (0-隐藏, 1-正常)
            comment.setStatus(cmd.getStatus());
            commentRepository.save(comment);

            log.info("管理员 [{}] 修改评论 [{}] 状态为 {}", operator.getNickname(), cmd.getCommentId(), cmd.getStatus());
            return ApiResponse.success(null);

        } catch (Exception e) {
            log.error("审核评论失败", e);
            return ApiResponse.fail(e.getMessage());
        }
    }

    /**
     * 超级管理员 物理删除评论
     * 场景：删除严重违规或垃圾广告评论，并同步减少统计数
     */
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<Void> deleteComment(@NotBlank String operatorOpenId,
                                           @NotNull Long commentId) {
        try {
            UserInfo operator = userRepository.findByOpenid(operatorOpenId)
                    .orElseThrow(() -> new IllegalArgumentException("管理员不存在"));

            // 权限校验：仅 SUPER_ADMIN 可物理删除
            if (!ROLE_SUPER_ADMIN.equals(operator.getRole())) {
                throw new SecurityException("只有超级管理员可执行删除操作");
            }

            PerformanceComment comment = commentRepository.findById(commentId)
                    .orElseThrow(() -> new IllegalArgumentException("评论不存在"));

            Performance performance = comment.getPerformance();

            // 执行物理删除
            commentRepository.delete(comment);

            // 更新计数 (-1)
            adjustCommentCount(performance, -1);

            log.info("超级管理员 [{}] 物理删除了评论 [{}]", operator.getNickname(), commentId);
            return ApiResponse.success(null);

        } catch (Exception e) {
            log.error("删除评论失败", e);
            return ApiResponse.fail(e.getMessage());
        }
    }

    /**
     * 分页查询某演出的评论列表
     * 场景：用户在演出详情页查看评论，仅展示 status=1 (正常) 的评论
     *
     * @param performanceId 演出 ID
     * @param page          页码 (0开始)
     * @param size          页大小
     * @return 分页评论列表
     */
    @Transactional(readOnly = true)
    public ApiResponse<Page<CommentDto>> getComments(Long performanceId, int page, int size) {
        try {
            // 按照发布时间倒序排列 (最新的在最上面)
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createTime"));

            // 只查询状态为 1 (正常) 的评论
            Page<PerformanceComment> commentPage = commentRepository
                    .findByPerformanceIdAndStatusOrderByCreateTimeDesc(performanceId, 1, pageable);

            // 转换为 DTO (包含头像处理)
            Page<CommentDto> dtoPage = commentPage.map(c -> CommentDto.from(c, baseUrl));

            return ApiResponse.success(dtoPage);
        } catch (Exception e) {
            log.error("查询评论列表失败: performanceId={}", performanceId, e);
            return ApiResponse.fail("获取评论失败");
        }
    }

    /**
     * 调整 PerformanceStats 中的 commentCount
     * @param delta 变化量 (+1 或 -1)
     */
    private void adjustCommentCount(Performance performance, int delta) {
        PerformanceStats stats = statsRepository.findByPerformanceId(performance.getId())
                .orElseGet(() -> {
                    PerformanceStats newStats = new PerformanceStats();
                    newStats.setPerformance(performance);
                    return newStats;
                });

        long current = stats.getCommentCount() == null ? 0 : stats.getCommentCount();
        long next = current + delta;

        // 防止减成负数
        if (next < 0) next = 0;

        stats.setCommentCount(next);
        statsRepository.save(stats);
    }
}