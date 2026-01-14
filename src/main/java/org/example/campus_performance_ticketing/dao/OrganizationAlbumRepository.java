package org.example.campus_performance_ticketing.dao;

import org.example.campus_performance_ticketing.model.OrganizationAlbum;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrganizationAlbumRepository extends JpaRepository<OrganizationAlbum, Long> {
    // 按组织查询相册内容
    List<OrganizationAlbum> findByOrganizationId(Long organizationId);
}