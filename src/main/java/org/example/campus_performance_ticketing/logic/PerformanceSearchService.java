package org.example.campus_performance_ticketing.logic;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.campus_performance_ticketing.dao.OrganizationInfoRepository;
import org.example.campus_performance_ticketing.dao.PerformanceRepository;
import org.example.campus_performance_ticketing.dao.PerformanceStatsRepository;
import org.example.campus_performance_ticketing.logic.dto.ApiResponse;
import org.example.campus_performance_ticketing.logic.dto.performance.PerformanceDetailDto;
import org.example.campus_performance_ticketing.model.*;
import org.example.campus_performance_ticketing.util.AvatarUrlUtil;
import org.example.campus_performance_ticketing.util.WeChatHelper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Valid
@RequiredArgsConstructor
public class PerformanceSearchService {

    private final PerformanceRepository performanceRepository;
    private final PerformanceStatsRepository statsRepository;
    private final OrganizationInfoRepository organizationRepository;

    private final WeChatHelper weChatHelper;

    @Value("${wechat.appid}")
    private String wechatAppid;

    @Value("${wechat.secret}")
    private String wechatSecret;


    @Value("${file.base.url}")
    private String baseUrl;

    /**
     * 分页查询演出列表
     * 支持：关键词搜索、分类筛选、状态筛选、场地名称筛选
     * @param keyword    搜索关键词 (标题或描述)
     * @param categoryId 分类 ID
     * @param status     发布状态 (如: 1-已发布)
     * @param venueName  场地名称（模糊匹配）
     * @param page       页码 (从0开始)
     * @param size       每页大小
     * @return 分页结果 DTO
     */
    @Transactional(readOnly = true)
    public ApiResponse<Page<PerformanceDetailDto>> searchPerformances(
            String keyword,
            Integer categoryId,
            Integer status,
            String venueName,
            int page,
            int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<Performance> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            query.distinct(true);

            predicates.add(cb.not(root.get("publishStatus").in(0, 4, 5)));

            if (status != null) predicates.add(cb.equal(root.get("publishStatus"), status));
            if (categoryId != null) predicates.add(cb.equal(root.get("categoryId"), categoryId));

            if (StringUtils.hasText(keyword)) {
                String likePattern = "%" + keyword + "%";
                predicates.add(cb.or(
                        cb.like(root.get("title"), likePattern),
                        cb.like(root.get("description"), likePattern)
                ));
            }

            if (StringUtils.hasText(venueName)) {
                Join<Performance, PerformanceSession> sessionJoin = root.join("sessions", JoinType.LEFT);
                Join<PerformanceSession, Venue> venueJoin = sessionJoin.join("venue", JoinType.LEFT);
                predicates.add(cb.like(venueJoin.get("name"), "%" + venueName + "%"));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Performance> performancePage = performanceRepository.findAll(spec, pageable);

        // 1) 收集本页 performanceId
        List<Long> ids = performancePage.getContent().stream()
                .map(Performance::getId)
                .filter(Objects::nonNull)
                .toList();

        // 2) 批量查 stats -> map
        Map<Long, PerformanceStats> statsMap = ids.isEmpty()
                ? Collections.emptyMap()
                : statsRepository.findByPerformanceIdIn(ids).stream()
                .collect(Collectors.toMap(
                        s -> s.getPerformance().getId(),
                        Function.identity(),
                        (a, b) -> a
                ));

        // 3) 转 DTO 时回填 viewCount/commentCount
        Page<PerformanceDetailDto> dtoPage = performancePage.map(p -> {
            PerformanceDetailDto dto = convertToDtoWithUrl(p);

            PerformanceStats stats = statsMap.get(p.getId());
            if (stats != null) {
                dto.setViewCount(stats.getViewCount());
                dto.setCommentCount(stats.getCommentCount());
            } else {
                dto.setViewCount(0L);
                dto.setCommentCount(0L);
            }
            return dto;
        });

        return ApiResponse.success(dtoPage);
    }

    /**
     * 获取演出详情
     *
     * @param performanceId 演出 ID
     * @return 演出详情 DTO
     */
    @Transactional(readOnly = true)
    public ApiResponse<PerformanceDetailDto> getPerformanceDetail(Long performanceId) {
        try {
            Performance performance = performanceRepository.findById(performanceId)
                    .orElseThrow(() -> new IllegalArgumentException("演出不存在: " + performanceId));

            PerformanceDetailDto dto = convertToDtoWithUrl(performance);

            // 组织举办时，补全社长/负责人ID
            if ("ORGANIZATION".equals(performance.getOrganizerType())) {
                OrganizationInfo org = organizationRepository.findById(performance.getOrganizerId())
                        .orElseThrow(() -> new IllegalArgumentException("组织不存在: " + performance.getOrganizerId()));
                dto.setOrganizerLeaderId(org.getLeader().getId());
            } else {
                dto.setOrganizerLeaderId(null);
            }

            // 补充统计数据：浏览量/评论数（查不到就返回0）
            PerformanceStats stats = statsRepository.findByPerformanceId(performanceId).orElse(null);
            if (stats != null) {
                dto.setViewCount(stats.getViewCount() == null ? 0L : stats.getViewCount());
                dto.setCommentCount(stats.getCommentCount() == null ? 0L : stats.getCommentCount());
                dto.setHotScore(stats.getHotScore() == null ? 0.0 : stats.getHotScore());
            } else {
                dto.setViewCount(0L);
                dto.setCommentCount(0L);
                dto.setHotScore(0.0);
            }

            return ApiResponse.success(dto);
        } catch (Exception e) {
            return ApiResponse.fail("获取演出详情失败: " + e.getMessage());
        }
    }


    /**
     * 生成演出小程序码并写回响应
     * 规则：
     * 1. 演出必须存在，否则返回 404 错误。
     * 2. scene 参数控制在 32 字符内，格式为 "id=123"。
     * 3. 调用 WeChatHelper 获取小程序码 PNG bytes。
     * 4. 设置响应 Content-Type 为 image/png，并写回 PNG 数据。
     */
    @Transactional(readOnly = true)
    public void writePerformanceWxaCodeToResponse(Long performanceId, HttpServletResponse response) throws Exception {
        // 1) 用 repository 校验演出是否存在（更轻量）
        boolean exists = performanceRepository.existsById(performanceId);
        if (!exists) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "演出不存在");
            return;
        }

        // 2) scene 控制在 32 字符内
        String scene = "id=" + performanceId;

        // 3) 调微信生成小程序码（PNG bytes）
        byte[] png = weChatHelper.getWxaCodeUnlimited(
                scene,
                "pages/performance/detail",
                wechatAppid,
                wechatSecret
        );

        // 4) 写回 PNG
        response.setContentType("image/png");
        response.setHeader("Cache-Control", "no-store");
        response.getOutputStream().write(png);
        response.flushBuffer();
    }

    /**
     * 辅助方法：转换 DTO 并拼接完整图片 URL
     */
    private PerformanceDetailDto convertToDtoWithUrl(Performance performance) {
        PerformanceDetailDto dto = PerformanceDetailDto.from(performance);

        // 处理海报 URL
        dto.setPosterUrl(AvatarUrlUtil.buildAvatarUrl(dto.getPosterUrl(), baseUrl));

        // 处理演职人员头像 URL
        if (dto.getStaff() != null) {
            dto.getStaff().forEach(staff ->
                    staff.setStaffAvatar(AvatarUrlUtil.buildAvatarUrl(staff.getStaffAvatar(), baseUrl))
            );
        }
        return dto;
    }
}