package org.example.campus_performance_ticketing.logic;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.campus_performance_ticketing.dao.PerformanceRecommendationRepository;
import org.example.campus_performance_ticketing.dao.PerformanceStatsRepository;
import org.example.campus_performance_ticketing.logic.dto.ApiResponse;
import org.example.campus_performance_ticketing.logic.dto.performance_recommendation.PerformanceCardDto;
import org.example.campus_performance_ticketing.model.Performance;
import org.example.campus_performance_ticketing.model.PerformanceRecommendation;
import org.example.campus_performance_ticketing.model.PerformanceStats;
import org.example.campus_performance_ticketing.util.AvatarUrlUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 推荐聚合服务
 * 负责合并 [人工推荐] 和 [自动热度]
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationAggregationService {

    private final PerformanceRecommendationRepository recommendationRepository;
    private final PerformanceStatsRepository statsRepository;

    @Value("${file.base.url}")
    private String baseUrl;

    /**
     * 获取混合推荐列表
     * @param type 推荐类型 (1-首页轮播, 2-列表置顶)
     * @param limit 限制数量
     */
    @Transactional(readOnly = true)
    public ApiResponse<List<PerformanceCardDto>> getMixedRecommendationList(Integer type, int limit) {

        List<PerformanceCardDto> resultList = new ArrayList<>();
        Set<Long> existingIds = new HashSet<>();

        // 1. 获取人工推荐 (Active) - 优先级最高
        List<PerformanceRecommendation> manualList = recommendationRepository
                .findActiveRecommendations(type, LocalDateTime.now());

        for (PerformanceRecommendation rec : manualList) {
            Performance p = rec.getPerformance();
            // 去重检查 (虽然数据库层应该保证，但双重保险)
            if (!existingIds.contains(p.getId())) {
                PerformanceCardDto dto = convertToCardDto(p);
                dto.setRecommendationTag("官方推荐"); // 打上标签
                resultList.add(dto);
                existingIds.add(p.getId());
            }
        }

        // 2. 如果数量还不够 limit，则用自动热度榜补齐
        if (resultList.size() < limit) {
            // 多查一些，因为可能会有重复ID被过滤掉
            Pageable pageable = PageRequest.of(0, limit + existingIds.size());
            List<PerformanceStats> hotStatsList = statsRepository.findTopHot(pageable);

            for (PerformanceStats stats : hotStatsList) {
                if (resultList.size() >= limit) break; // 够了就停

                Performance p = stats.getPerformance();
                if (!existingIds.contains(p.getId())) {
                    PerformanceCardDto dto = convertToCardDto(p);
                    // 这里的 stats 已经是查询出来的热度数据，直接填进去
                    dto.fillStats(stats);
                    // dto.setRecommendationTag("热度飙升"); // 可选
                    resultList.add(dto);
                    existingIds.add(p.getId());
                }
            }
        } else {
            // 如果人工推荐本身就超过了 limit，截断
            resultList = resultList.subList(0, limit);
        }

        // 3. 批量补全人工推荐数据的统计信息
        // (因为第一步从 Recommendation 表查出的 Performance 还没关联 Stats 数据)
        populateStatsForManualItems(resultList);

        return ApiResponse.success(resultList);
    }

    /**
     * 为列表中的 DTO 批量填充统计数据
     */
    private void populateStatsForManualItems(List<PerformanceCardDto> dtoList) {
        // 找出还没填充热度分(HotScore == 0.0)的 ID
        // 注意：如果你第一步填了默认值0，这里需要判断逻辑。
        // 或者简单粗暴地：收集所有ID，一次性查出来，重新覆盖一遍 map。

        if (dtoList.isEmpty()) return;

        List<Long> ids = dtoList.stream()
                .map(PerformanceCardDto::getId)
                .collect(Collectors.toList());

        // 批量查询 Stats
        List<PerformanceStats> statsList = statsRepository.findByPerformanceIdIn(ids);

        // 转为 Map: ID -> Stats
        Map<Long, PerformanceStats> statsMap = statsList.stream()
                .collect(Collectors.toMap(
                        s -> s.getPerformance().getId(),
                        Function.identity(),
                        (existing, replacement) -> existing
                ));

        // 遍历 DTO 填充
        for (PerformanceCardDto dto : dtoList) {
            if (statsMap.containsKey(dto.getId())) {
                dto.fillStats(statsMap.get(dto.getId()));
            }
        }
    }

    /**
     * 实体转 DTO + URL处理
     */
    private PerformanceCardDto convertToCardDto(Performance performance) {
        PerformanceCardDto dto = PerformanceCardDto.from(performance);
        // 处理海报 URL (拼接域名)
        dto.setPosterUrl(AvatarUrlUtil.buildAvatarUrl(dto.getPosterUrl(), baseUrl));
        return dto;
    }
}