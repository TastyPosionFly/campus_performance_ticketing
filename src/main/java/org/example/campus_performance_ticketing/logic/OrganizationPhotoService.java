package org.example.campus_performance_ticketing.logic;

import org.example.campus_performance_ticketing.dao.OrganizationPhotoRepository;
import org.example.campus_performance_ticketing.dao.OrganizationRepository;
import org.example.campus_performance_ticketing.logic.dto.ApiResponse;
import org.example.campus_performance_ticketing.model.Organization;
import org.example.campus_performance_ticketing.model.OrganizationPhoto;
import org.example.campus_performance_ticketing.util.JwtTokenUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OrganizationPhotoService {

    private final OrganizationPhotoRepository repository;
    private final OrganizationRepository organizationRepository;
    private final JwtTokenUtil jwtTokenUtil;

    public OrganizationPhotoService(OrganizationPhotoRepository repository,
                                    OrganizationRepository organizationRepository,
                                    JwtTokenUtil jwtTokenUtil) {
        this.repository = repository;
        this.organizationRepository = organizationRepository;
        this.jwtTokenUtil = jwtTokenUtil;
    }

    /** 添加照片（仅组织首领） */
    @Transactional
    public ApiResponse<OrganizationPhoto> addPhoto(String token, OrganizationPhoto photo) {
        try {
            // 验证用户是否为组织首领
            Organization org = organizationRepository.findById(photo.getOrganizationId())
                    .orElseThrow(() -> new RuntimeException("组织不存在"));

            Long userId = ((Number) jwtTokenUtil.parseToken(token).get("userId")).longValue();
            if (!org.getLeaderUserId().equals(userId)) {
                return ApiResponse.fail("只有组织首领可以添加照片");
            }

            OrganizationPhoto saved = repository.save(photo);
            return ApiResponse.success(saved);

        } catch (Exception e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    /** 查询某组织所有照片（所有人可查看） */
    public ApiResponse<List<OrganizationPhoto>> getPhotosByOrganization(Long organizationId) {
        try {
            List<OrganizationPhoto> photos = repository.findByOrganizationId(organizationId);
            return ApiResponse.success(photos);
        } catch (Exception e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    /** 分页查询某组织照片，按时间倒序（所有人可查看） */
    public ApiResponse<Page<OrganizationPhoto>> getPhotosByOrganizationPaged(Long organizationId, int page, int size) {
        try {
            Page<OrganizationPhoto> photos = repository.findByOrganizationIdOrderByCreateTimeDesc(
                    organizationId, PageRequest.of(page, size));
            return ApiResponse.success(photos);
        } catch (Exception e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    /** 删除指定照片（仅组织首领） */
    @Transactional
    public ApiResponse<String> deletePhoto(String token, Long photoId) {
        try {
            OrganizationPhoto photo = repository.findById(photoId)
                    .orElseThrow(() -> new RuntimeException("照片不存在"));

            Organization org = organizationRepository.findById(photo.getOrganizationId())
                    .orElseThrow(() -> new RuntimeException("组织不存在"));

            Long userId = ((Number) jwtTokenUtil.parseToken(token).get("userId")).longValue();
            if (!org.getLeaderUserId().equals(userId)) {
                return ApiResponse.fail("只有组织首领可以删除照片");
            }

            repository.deleteById(photoId);
            return ApiResponse.success("照片删除成功");

        } catch (Exception e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    /** 删除某组织所有照片（仅组织首领） */
    @Transactional
    public ApiResponse<String> deletePhotosByOrganization(String token, Long organizationId) {
        try {
            Organization org = organizationRepository.findById(organizationId)
                    .orElseThrow(() -> new RuntimeException("组织不存在"));

            Long userId = ((Number) jwtTokenUtil.parseToken(token).get("userId")).longValue();
            if (!org.getLeaderUserId().equals(userId)) {
                return ApiResponse.fail("只有组织首领可以删除组织照片");
            }

            repository.deleteByOrganizationId(organizationId);
            return ApiResponse.success("组织照片全部删除成功");

        } catch (Exception e) {
            return ApiResponse.fail(e.getMessage());
        }
    }
}
