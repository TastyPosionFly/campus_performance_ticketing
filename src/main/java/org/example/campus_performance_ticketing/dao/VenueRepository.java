package org.example.campus_performance_ticketing.dao;

import org.example.campus_performance_ticketing.model.Venue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VenueRepository extends JpaRepository<Venue, Long> {

    /**
     * 根据场地名称模糊查询
     *
     * @param name 场地名称
     * @return 场地列表
     */
    List<Venue> findByNameContaining(String name);

    /**
     * 根据场地类型查询
     *
     * @param type 场地类型
     * @return 场地列表
     */
    List<Venue> findByType(Integer type);

    /**
     * 根据状态查询所有场地 (例如查询所有正常开放的场地)
     *
     * @param status 状态 (1:正常)
     * @return 场地列表
     */
    List<Venue> findByStatus(Integer status);

    /**
     * 根据管理员ID查询场地
     * 用于 VenueAdmin 查看自己管理的场地
     *
     * @param managerId 管理员 ID
     * @return 场地列表
     */
    List<Venue> findByManagerId(Long managerId);

    /**
     * 查找所有拥有特定设备（JSON查询）的场地 (可选的高级查询)
     * 注意：JSON 查询依赖数据库方言，这里仅作 MySQL 的原生查询示例
     *
     * @param equipmentKey 设备Key (如 "wifi")
     * @return 场地列表
     */
    @Query(value = "SELECT * FROM venues v WHERE JSON_EXTRACT(v.equipment_info, CONCAT('$.', ?1)) = true AND v.deleted_at IS NULL", nativeQuery = true)
    List<Venue> findByEquipmentKey(String equipmentKey);
}