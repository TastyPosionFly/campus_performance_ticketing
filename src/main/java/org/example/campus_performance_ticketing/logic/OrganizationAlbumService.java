package org.example.campus_performance_ticketing.logic;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.example.campus_performance_ticketing.dao.OrganizationAlbumRepository;
import org.example.campus_performance_ticketing.dao.OrganizationInfoRepository;
import org.example.campus_performance_ticketing.dao.OrganizationMemberRepository;
import org.example.campus_performance_ticketing.dao.UserRepository;
import org.example.campus_performance_ticketing.logic.dto.ApiResponse;
import org.example.campus_performance_ticketing.logic.dto.organization.PublicOrganizationAlbumInfo;
import org.example.campus_performance_ticketing.logic.dto.organization.UploadAlbumPhotoRequest;
import org.example.campus_performance_ticketing.logic.dto.user.PublicUserInfo;
import org.example.campus_performance_ticketing.model.OrganizationAlbum;
import org.example.campus_performance_ticketing.model.OrganizationInfo;
import org.example.campus_performance_ticketing.model.OrganizationMember;
import org.example.campus_performance_ticketing.model.UserInfo;
import org.example.campus_performance_ticketing.util.AvatarUrlUtil;
import org.example.campus_performance_ticketing.util.FileUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.logging.Logger;

@Service
@Validated
public class OrganizationAlbumService {
    private final OrganizationAlbumRepository organizationAlbumRepository;
    private final OrganizationInfoRepository organizationInfoRepository;
    private final OrganizationMemberRepository organizationMemberRepository;

    private final static Logger logger = Logger.getLogger(OrganizationAlbumService.class.getName());
    private final UserRepository userRepository;

    @Value("${file.base.url}")
    private String fileBaseUrl;

    @Value("${org.album.upload-dir}")
    private String albumUploadDir;

    public OrganizationAlbumService(OrganizationAlbumRepository organizationAlbumRepository,
                                    OrganizationInfoRepository organizationInfoRepository,
                                    OrganizationMemberRepository organizationMemberRepository, UserRepository userRepository) {
        this.organizationAlbumRepository = organizationAlbumRepository;
        this.organizationInfoRepository = organizationInfoRepository;
        this.organizationMemberRepository = organizationMemberRepository;
        this.userRepository = userRepository;
    }

    /**
     * 上传图片 组织 LEADER / MANAGER 成员可上传
     */
    /**
     * Service 层统一处理：权限校验 + 先入库(占位) + 保存文件/下载URL + 更新入库 + 失败补偿
     */
    public ApiResponse<Void> uploadAlbumPhoto(String openId,
                                              UploadAlbumPhotoRequest body,
                                              MultipartFile photoFile) {
        String safeDir = FileUtil.normalizeUploadDir(albumUploadDir);

        // 参数基本校验
        if (body == null || body.getOrganizationId() == null) {
            return ApiResponse.fail("organizationId 不能为空");
        }

        // 1) 先创建数据库记录（占位）
        Long albumId;
        try {
            albumId = createAlbumRecord(openId, body.getOrganizationId(), "PENDING", body.getDescription());
        } catch (Exception e) {
            return ApiResponse.fail("创建相册记录失败：" + e.getMessage());
        }

        // 2) 保存图片到服务器（或从URL下载）
        String savedPhotoUrl;
        try {
            if (photoFile != null && !photoFile.isEmpty()) {
                savedPhotoUrl = FileUtil.saveAvatar(photoFile, safeDir, null);
            } else if (body.getPhotoUrl() != null && body.getPhotoUrl().startsWith("http")) {
                savedPhotoUrl = FileUtil.saveAvatarFromUrl(body.getPhotoUrl(), safeDir, null);
            } else {
                deleteAlbumRecordQuietly(albumId);
                return ApiResponse.fail("必须上传图片文件或提供图片URL");
            }
        } catch (Exception e) {
            deleteAlbumRecordQuietly(albumId);
            return ApiResponse.fail("图片保存失败：" + e.getMessage());
        }

        // 3) 更新数据库 photoUrl
        try {
            updateAlbumPhotoUrl(albumId, savedPhotoUrl);
            ApiResponse<Void> resp = ApiResponse.success(null);
            resp.setMessage("图片上传成功");
            return resp;
        } catch (Exception e) {
            deleteAlbumRecordQuietly(albumId);
            FileUtil.deletePhysicalFile(savedPhotoUrl);
            return ApiResponse.fail("保存图片信息失败：" + e.getMessage());
        }
    }

    @Transactional
    public Long createAlbumRecord(@NotBlank String openId,
                                  @NotNull Long organizationId,
                                  @NotBlank String pendingUrl,
                                  String description) {
        // 权限校验 + 插入记录（同你之前 uploadPhoto 里的逻辑）
        UserInfo user = userRepository.findByOpenid(openId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

        OrganizationInfo organization = organizationInfoRepository.findById(organizationId)
                .orElseThrow(() -> new IllegalArgumentException("组织不存在"));

        OrganizationMember member = organizationMemberRepository
                .findByOrganizationAndUser(organization, user)
                .orElseThrow(() -> new IllegalArgumentException("用户不是该组织成员"));

        if (member.getStatus() != 1) throw new IllegalStateException("用户不是该组织有效成员");
        if (!"LEADER".equalsIgnoreCase(member.getMemberRole()) && !"MANAGER".equalsIgnoreCase(member.getMemberRole())) {
            throw new SecurityException("没有权限上传图片");
        }

        OrganizationAlbum album = new OrganizationAlbum();
        album.setOrganization(organization);
        album.setUploader(user);
        album.setPhotoUrl(pendingUrl);
        album.setDescription(description);

        return organizationAlbumRepository.save(album).getId();
    }

    @Transactional
    public void updateAlbumPhotoUrl(Long albumId, String photoUrl) {
        OrganizationAlbum album = organizationAlbumRepository.findById(albumId)
                .orElseThrow(() -> new IllegalArgumentException("相册记录不存在"));
        album.setPhotoUrl(photoUrl);
        organizationAlbumRepository.save(album);
    }

    @Transactional
    public void deleteAlbumRecordQuietly(Long albumId) {
        try {
            organizationAlbumRepository.deleteById(albumId);
        } catch (Exception ignore) {
        }
    }

    /**
     * 删除图片 组织 LEADER / MANAGER 成员可删除
     */
    @Transactional
    public ApiResponse<Void> deletePhoto(@NotBlank String openId,
                                   @NotNull Long photoId) {
        try {
            // 检查用户是否为该组织的 LEADER 或 MANAGER
            UserInfo user = userRepository.findByOpenid(openId)
                    .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

            OrganizationAlbum album = organizationAlbumRepository.findById(photoId)
                    .orElseThrow(() -> new IllegalArgumentException("图片不存在"));

            OrganizationMember member = organizationMemberRepository.findByOrganizationAndUser(album.getOrganization(), user)
                    .orElseThrow(() -> new IllegalArgumentException("用户不是该组织成员"));

            // 有效成员检查
            if (member.getStatus() != 1) {
                return ApiResponse.fail("用户不是该组织有效成员");
            }

            if (!"LEADER".equalsIgnoreCase(member.getMemberRole()) && !"MANAGER".equalsIgnoreCase(member.getMemberRole())) {
                return ApiResponse.fail("没有权限删除图片");
            }

            // 删除图片文件
            FileUtil.deletePhysicalFile(album.getPhotoUrl());

            // 删除图片信息
            organizationAlbumRepository.delete(album);

            ApiResponse<Void> resp = ApiResponse.success(null);
            resp.setMessage("图片删除成功");
            return resp;
        } catch (Exception e) {
            logger.severe("删除图片异常: " + e.getMessage());
            return ApiResponse.fail("删除图片失败: " + e.getMessage());
        }
    }

    public ApiResponse<List<PublicOrganizationAlbumInfo>> getOrganizationPhotos(@NotNull Long organizationId) {
        try {
            OrganizationInfo organization = organizationInfoRepository.findById(organizationId)
                    .orElseThrow(() -> new IllegalArgumentException("组织不存在"));

            List<OrganizationAlbum> photos = organizationAlbumRepository.findByOrganization(organization);

            List<PublicOrganizationAlbumInfo> dtoList = new java.util.ArrayList<>(photos.size());

            for (OrganizationAlbum album : photos) {
                PublicOrganizationAlbumInfo dto = new PublicOrganizationAlbumInfo();

                dto.setId(album.getId());
                dto.setPhotoUrl(AvatarUrlUtil.buildAvatarUrl(album.getPhotoUrl(), fileBaseUrl));
                dto.setUploadTime(String.valueOf(album.getUploadTime()));

                // uploader
                if (album.getUploader() != null) {
                    PublicUserInfo uploaderInfo = new PublicUserInfo();
                    uploaderInfo.setNickname(album.getUploader().getNickname());
                    uploaderInfo.setAvatar(AvatarUrlUtil.buildAvatarUrl(album.getUploader().getAvatar(), fileBaseUrl));
                    uploaderInfo.setMajor(album.getUploader().getMajor());
                    uploaderInfo.setCollege(album.getUploader().getCollege());
                    dto.setUploader(uploaderInfo);
                }

                dtoList.add(dto);
            }

            return ApiResponse.success(dtoList);

        } catch (Exception e) {
            logger.severe("获取组织图片异常: " + e.getMessage());
            return ApiResponse.fail("获取组织图片失败: " + e.getMessage());
        }
    }
}
