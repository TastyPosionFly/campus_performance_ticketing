package org.example.campus_performance_ticketing.logic;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid; // 如果是 Spring Boot 2.x 可能是 javax.validation.Valid
import org.example.campus_performance_ticketing.dao.UserRepository;
import org.example.campus_performance_ticketing.dao.VenueRepository;
import org.example.campus_performance_ticketing.logic.dto.ApiResponse;
import org.example.campus_performance_ticketing.logic.dto.venue.CreateVenueDto;
import org.example.campus_performance_ticketing.model.UserInfo;
import org.example.campus_performance_ticketing.model.Venue;
import org.example.campus_performance_ticketing.util.FileUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

@Service
@Validated // 关键：开启此类的方法参数校验
public class VenueService {

    private final VenueRepository venueRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper  = new ObjectMapper();;

    private static final Logger logger = Logger.getLogger(VenueService.class.getName());


    @Value("${venue.album.upload-dir:./data/venue/}")
    private String venueAlbumUploadDir;

    public VenueService(VenueRepository venueRepository,
                        UserRepository userRepository) {
        this.venueRepository = venueRepository;
        this.userRepository = userRepository;
    }

    /**
     * 创建场地
     * @param dto 前端传输的 DTO
     * @param openId 操作人 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<Void> createVenue(@Valid CreateVenueDto dto, String openId) { // 关键：@Valid 触发 DTO 内部注解

        // 身份校验
        UserInfo operator = userRepository.findByOpenid(openId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

        if (!operator.getRole().equals("SUPER_ADMIN")) {
            logger.warning("没有权限创建场地: " + operator.getOpenid());
            return ApiResponse.fail("没有权限创建场地");
        }

        // 1. 手动业务校验：图片二选一
        boolean hasCoverFile = dto.getCoverImageFile() != null && !dto.getCoverImageFile().isEmpty();
        boolean hasCoverUrl = StringUtils.hasText(dto.getCoverImageUrl());

        if (!hasCoverFile && !hasCoverUrl) {
            // 这个异常最好自定义，例如 BusinessException，会被拦截器捕获
            logger.warning("封面图未提供");
            return ApiResponse.fail("必须上传一张封面图片或提供图片 URL");
        }

        Venue venue = new Venue();
        venue.setName(dto.getName());
        venue.setDescription(dto.getDescription());
        venue.setAddress(dto.getAddress());
        venue.setCapacity(dto.getCapacity());
        venue.setType(dto.getType());
        venue.setStatus(1); // 默认正常 1:正常, 0:维护, 2:停用

        // 2. 处理封面图
        String coverPath = null;
        try {
            if (hasCoverFile) {
                // 情况A: 上传了文件 -> 保存文件
                coverPath = FileUtil.saveImage(dto.getCoverImageFile(), venueAlbumUploadDir);
            } else if (hasCoverUrl) {
                // 情况B: 提供了URL -> 下载并保存到本地 (修改了这里)
                // 原代码: coverPath = dto.getCoverImageUrl();
                coverPath = FileUtil.saveImageFromUrl(dto.getCoverImageUrl(), venueAlbumUploadDir);
            }
        } catch (IOException e) {
            logger.warning("封面图保存/下载失败: " + e.getMessage());
            return ApiResponse.fail("封面图处理失败: " + e.getMessage());
        }
        venue.setCoverImage(coverPath);

        // 3. 处理轮播图 (合并 URL 和 文件)
        List<String> finalPhotoList = new ArrayList<>();

        // 3.1 处理 URL 列表 -> 下载并保存 (修改了这里)
        if (dto.getPhotoUrlList() != null) {
            for (String url : dto.getPhotoUrlList()) {
                try {
                    String localPath = FileUtil.saveImageFromUrl(url, venueAlbumUploadDir);
                    if (localPath != null) {
                        finalPhotoList.add(localPath);
                    }
                } catch (IOException e) {
                    logger.warning("轮播图URL下载失败，已跳过: " + url);
                    // 也可以选择不跳过，而是直接存URL作为降级方案：finalPhotoList.add(url);
                }
            }
        }

        // 3.2 处理文件列表 -> 保存文件 (保持不变)
        if (dto.getPhotoFiles() != null && !dto.getPhotoFiles().isEmpty()) {
            for (MultipartFile file : dto.getPhotoFiles()) {
                try {
                    String path = FileUtil.saveImage(file, venueAlbumUploadDir);
                    if (path != null) finalPhotoList.add(path);
                } catch (IOException e) {
                    logger.warning("轮播图上传失败忽略: " + file.getOriginalFilename());
                }
            }
        }

        // 4. 设备信息处理
        // 修正点 1: 直接获取 String，不要 .toString()
        String eqInfo = dto.getEquipmentInfo();

        // 修正点 2: 判空校验
        if (StringUtils.hasText(eqInfo)) {
            try {
                // 验证 JSON 格式合法性
                objectMapper.readTree(eqInfo);

                // 验证通过，存入实体
                venue.setEquipmentInfo(eqInfo);
            } catch (JsonProcessingException e) {
                logger.warning("前端传入的设备信息不是合法的 JSON 格式: " + eqInfo);
                return ApiResponse.fail("设备信息格式错误，请检查 JSON 语法");
            }
        }

        // 5. 关联人员
        venue.setCreator(operator);

        // 如果没指定管理员，默认创建人就是管理员
        if (dto.getManagerId() == null) {
            venue.setManager(operator);
        }

        if (dto.getManagerId() != null) {
            UserInfo manager = userRepository.findById(dto.getManagerId()).orElse(null);
            venue.setManager(manager);
        }

        venueRepository.save(venue);

        ApiResponse<Void> response = ApiResponse.success(null);
        response.setMessage("创建场地成功: " + venue.getName());

        return response;
    }
}