package org.example.campus_performance_ticketing.dao;

import org.example.campus_performance_ticketing.model.VenueBlockedDay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface VenueBlockedDayRepository extends JpaRepository<VenueBlockedDay, Long> {

    /**
     * 查询某场地所有的屏蔽记录
     *
     * @param venueId 场地 ID
     * @return 屏蔽记录列表
     */
    List<VenueBlockedDay> findByVenueId(Long venueId);

    /**
     * 检查某场地在特定日期是否被屏蔽
     * 返回 true 表示存在记录（即被屏蔽）
     *
     * @param venueId     场地 ID
     * @param blockedDate 日期
     * @return 是否存在
     */
    boolean existsByVenueIdAndBlockedDate(Long venueId, LocalDate blockedDate);

    /**
     * 查询某场地在一段时间内的所有屏蔽日期
     * 用于前端日历渲染不可用状态
     *
     * @param venueId   场地 ID
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 屏蔽记录列表
     */
    List<VenueBlockedDay> findByVenueIdAndBlockedDateBetween(Long venueId, LocalDate startDate, LocalDate endDate);

    /**
     * 批量查询指定日期（精确）在给定场馆列表中的屏蔽记录
     * @param venueIds 场馆ID列表
     * @param date 指定日期
     * @return 屏蔽记录
     */
    List<VenueBlockedDay> findByVenueIdInAndBlockedDate(List<Long> venueIds, LocalDate date);

    /**
     * 查找未来的所有屏蔽记录（清理历史数据或展示预告用）
     *
     * @param venueId 场地 ID
     * @param date 当前日期
     * @return 未来的屏蔽记录
     */
    List<VenueBlockedDay> findByVenueIdAndBlockedDateAfter(Long venueId, LocalDate date);
}