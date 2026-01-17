package org.example.campus_performance_ticketing.dao;

import org.example.campus_performance_ticketing.model.OrganizationAlbum;
import org.example.campus_performance_ticketing.model.OrganizationInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrganizationAlbumRepository extends JpaRepository<OrganizationAlbum, Long> {
    // 按组织查询相册内容
    List<OrganizationAlbum> findByOrganizationId(Long organizationId);

    /**
     * 批量按组织列表查询相册内容
     * @param orgIds
     * @return
     */
    List<OrganizationAlbum> findByOrganizationIdIn(List<Long> orgIds);

    /**
     * 按组织实体查询相册内容
     * @param organization
     * @return
     */
    List<OrganizationAlbum> findByOrganization(OrganizationInfo organization);



    /**
     * 删除指定组织的相册内容
     * @param organizationId
     */
    void deleteByOrganizationId(Long organizationId);

    /**
     * 批量删除指定组织列表的相册内容
     * @param organizationIdList
     */
    void deleteByOrganizationIdIn(List<Long> organizationIdList);

}