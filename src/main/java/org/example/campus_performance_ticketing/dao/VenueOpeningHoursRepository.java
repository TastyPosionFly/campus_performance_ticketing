package org.example.campus_performance_ticketing.dao;

import org.example.campus_performance_ticketing.model.VenueOpeningHours;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VenueOpeningHoursRepository extends JpaRepository<VenueOpeningHours, Long> {

    /**
     * 查询某场地的所有开放时间配置
     *
     * @param venueId 场地 ID
     * @return 配置列表
     */
    List<VenueOpeningHours> findByVenueId(Long venueId);

    /**
     * 查询某场地在星期几的配置
     * 用于检查某一天是否开放
     *
     * @param venueId   场地 ID
     * @param dayOfWeek 星期 (1-7)
     * @return 配置详情
     */
    Optional<VenueOpeningHours> findByVenueIdAndDayOfWeek(Long venueId, Integer dayOfWeek);

    /**
     * 删除某场地的所有配置 (通常在重新设置规则时先删后增)
     *
     * @param venueId 场地 ID
     */
    void deleteByVenueId(Long venueId);
}