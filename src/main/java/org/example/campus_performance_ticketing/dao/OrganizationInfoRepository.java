package org.example.campus_performance_ticketing.dao;

import org.example.campus_performance_ticketing.model.OrganizationInfo;
import org.example.campus_performance_ticketing.model.UserInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrganizationInfoRepository extends JpaRepository<OrganizationInfo, Long> {

    /**
     * 根据状态查找组织列表
     * @param status
     * @return
     */
    List<OrganizationInfo> findByStatus(int status);

    /**
     * 删除指定状态的组织
     * @param status
     */
    void deleteByStatus(int status);

    /**
     * 根据负责人 ID 检查是否存在组织
     * @param leaderId
     * @return
     */
    boolean existsByLeaderId(Long leaderId);

    /**
     * 根据负责人ID查找组织列表
     * @param leaderId
     * @return
     */
    List<OrganizationInfo> findAllByLeaderId(Long leaderId);

    /**
     * 按名称模糊（不区分大小写）搜索组织，同时排除指定 status（例如 status = 2 表示不可用/已解散）
     *
     * 使用数据库层面的 like/ILIKE（由 JPA 实现），避免在内存中过滤全部记录，性能更优。
     *
     * @param name 部分或全部组织名称
     * @param statusToExclude 排除的状态值（例如 2）
     * @return 匹配的组织列表
     */
    List<OrganizationInfo> findByNameContainingIgnoreCaseAndStatusNot(String name, int statusToExclude);

    /**
     * 如果需要仅按名称模糊匹配（不排除任何 status），可以使用：
     */
    List<OrganizationInfo> findByNameContainingIgnoreCase(String name);

    // 分页版按名称模糊查询并排除指定状态
    org.springframework.data.domain.Page<OrganizationInfo> findByNameContainingIgnoreCaseAndStatusNot(String name, int statusToExclude, org.springframework.data.domain.Pageable pageable);

    /**
     * 分页查询：排除某个状态（例如已解散 status=2），用于列表分页显示
     */
    org.springframework.data.domain.Page<OrganizationInfo> findByStatusNot(int status, org.springframework.data.domain.Pageable pageable);
}