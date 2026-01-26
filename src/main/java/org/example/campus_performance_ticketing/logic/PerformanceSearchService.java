package org.example.campus_performance_ticketing.logic;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.campus_performance_ticketing.dao.PerformanceRepository;
import org.example.campus_performance_ticketing.logic.dto.ApiResponse;
import org.example.campus_performance_ticketing.logic.dto.performance.PerformanceDetailDto;
import org.example.campus_performance_ticketing.model.Performance;
import org.example.campus_performance_ticketing.util.AvatarUrlUtil;
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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Valid
@RequiredArgsConstructor
public class PerformanceSearchService {

    private final PerformanceRepository performanceRepository;

    @Value("${file.base.url}")
    private String baseUrl;

    /**
     * 分页查询演出列表
     * 支持：关键词搜索、分类筛选、状态筛选
     *
     * @param keyword    搜索关键词 (标题或描述)
     * @param categoryId 分类ID
     * @param status     发布状态 (如: 1-已发布)
     * @param page       页码 (从0开始)
     * @param size       每页大小
     * @return 分页结果 DTO
     */
    @Transactional(readOnly = true)
    public ApiResponse<Page<PerformanceDetailDto>> searchPerformances(String keyword, Integer categoryId, Integer status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<Performance> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. 状态筛选 (如果不传，默认只查已发布的，或者可以查全部)
            if (status != null) {
                predicates.add(cb.equal(root.get("publishStatus"), status));
            }

            // 2. 分类筛选
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("categoryId"), categoryId));
            }

            // 3. 关键词搜索 (标题 OR 描述)
            if (StringUtils.hasText(keyword)) {
                String likePattern = "%" + keyword + "%";
                Predicate titleLike = cb.like(root.get("title"), likePattern);
                Predicate descLike = cb.like(root.get("description"), likePattern);
                predicates.add(cb.or(titleLike, descLike));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Performance> performancePage = performanceRepository.findAll(spec, pageable);

        // 转换 Entity -> DTO 并处理图片 URL
        Page<PerformanceDetailDto> dtoPage = performancePage.map(this::convertToDtoWithUrl);

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
            return ApiResponse.success(dto);
        } catch (Exception e) {
            return ApiResponse.fail("获取演出详情失败: " + e.getMessage());
        }
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