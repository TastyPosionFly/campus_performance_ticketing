package org.example.campus_performance_ticketing.dao;

import org.example.campus_performance_ticketing.model.OrganizationPhoto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrganizationPhotoRepository extends JpaRepository<OrganizationPhoto, Long> {

    /**
     * 根据组织ID查询该组织的所有照片
     */
    List<OrganizationPhoto> findByOrganizationId(Long organizationId);

    /**
     * 根据组织ID删除该组织的所有照片
     */
    void deleteByOrganizationId(Long organizationId);

    /**
     * 分页查询组织照片，按上传时间倒序
     */
    Page<OrganizationPhoto> findByOrganizationIdOrderByCreateTimeDesc(Long organizationId, Pageable pageable);

}
