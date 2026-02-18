package org.example.campus_performance_ticketing.logic;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid; // 如果是 Spring Boot 2.x 可能是 javax.validation.Valid
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.example.campus_performance_ticketing.dao.OrganizationInfoRepository;
import org.example.campus_performance_ticketing.dao.PerformanceSessionRepository;
import org.example.campus_performance_ticketing.dao.UserRepository;
import org.example.campus_performance_ticketing.dao.VenueRepository;
import org.example.campus_performance_ticketing.logic.dto.ApiResponse;
import org.example.campus_performance_ticketing.logic.dto.venue.*;
import org.example.campus_performance_ticketing.model.PerformanceSession;
import org.example.campus_performance_ticketing.model.UserInfo;
import org.example.campus_performance_ticketing.model.Venue;
import org.example.campus_performance_ticketing.util.AvatarUrlUtil;
import org.example.campus_performance_ticketing.util.FileUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
@Validated // 关键：开启此类的方法参数校验
@RequiredArgsConstructor

public class VenueService {

    private final VenueRepository venueRepository;
    private final UserRepository userRepository;
    private final VenueOpeningHoursService venueOpeningHoursService;
    private final PerformanceSessionRepository performanceSessionRepository;
    private final OrganizationInfoRepository organizationRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Logger logger = Logger.getLogger(VenueService.class.getName());


    @Value("${venue.album.upload-dir:./data/venue/}")
    private String venueAlbumUploadDir;

    @Value("${file.base.url}")
    private String fileBaseUrl;


    /**
     * 创建场地
     *
     * @param dto    前端传输的 DTO
     * @param openId 操作人 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<Void> createVenue(@Valid CreateVenueDto dto,
                                         @NotBlank String openId) {

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
            return ApiResponse.fail("必须上传一张封面图片");
        }

        Venue venue = new Venue();
        venue.setName(dto.getName());
        venue.setDescription(dto.getDescription());
        venue.setAddress(dto.getAddress());
        venue.setCapacity(dto.getCapacity());
        venue.setType(dto.getType());
        venue.setStatus(2); // 默认正常 1:正常, 0:维护, 2:停用

        // 2. 处理封面图
        String coverPath;
        try {
            if (hasCoverFile) {
                // 情况A: 上传了文件 -> 保存文件
                coverPath = FileUtil.saveImage(dto.getCoverImageFile(), venueAlbumUploadDir);
            } else {
                // 情况B: 提供了URL -> 下载并保存到本地 (修改了这里)
                // 原代码: coverPath = dto.getCoverImageUrl();
                coverPath = FileUtil.saveImageFromUrl(dto.getCoverImageUrl(), venueAlbumUploadDir);
            }
        } catch (IOException e) {
            logger.warning("封面图保存/下载失败: " + e.getMessage());
            return ApiResponse.fail("封面图处理失败: " + e.getMessage());
        }
        venue.setCoverImage(coverPath);

        // 改用对象列表，而不是 String 列表
        List<VenuePhotoInfo> photoList = new ArrayList<>();

        // 3.1 处理 URL 列表
        if (dto.getPhotoUrlList() != null) {
            for (String url : dto.getPhotoUrlList()) {
                try {
                    String localPath = FileUtil.saveImageFromUrl(url, venueAlbumUploadDir);
                    if (localPath != null) {
                        // 生成唯一ID，并记录
                        photoList.add(new VenuePhotoInfo(
                                UUID.randomUUID().toString(),
                                localPath,
                                UUID.randomUUID().toString()
                        ));
                    }
                } catch (IOException e) {
                    logger.warning("轮播图URL下载失败: " + url);
                }
            }
        }

        logger.info("照片列表为空吗？ " + (photoList.isEmpty() ? "是" : "否") + " URL数量: " + (dto.getPhotoUrlList() == null ? 0 : dto.getPhotoUrlList().size()));

        // 3.2 处理文件列表
        if (dto.getPhotoFiles() != null && !dto.getPhotoFiles().isEmpty()) {
            for (MultipartFile file : dto.getPhotoFiles()) {
                try {
                    logger.info("正在处理轮播图文件: " + file.getOriginalFilename());
                    String path = FileUtil.saveImage(file, venueAlbumUploadDir);
                    if (path != null) {
                        // 生成唯一ID，并记录原文件名
                        photoList.add(new VenuePhotoInfo(
                                UUID.randomUUID().toString(),
                                path,
                                file.getOriginalFilename()
                        ));
                    }
                } catch (IOException e) {
                    logger.warning("轮播图上传失败: " + file.getOriginalFilename());
                }
            }
        }

        logger.info("照片列表内容: " + photoList.stream().map(VenuePhotoInfo::getUrl).collect(Collectors.joining(", ")));

        try {
            String photoListJson = objectMapper.writeValueAsString(photoList);
            venue.setPhotoList(photoListJson);
        } catch (JsonProcessingException e) {
            logger.warning("序列化轮播图列表失败: " + e.getMessage());
            venue.setPhotoList("[]"); // 降级方案：保存空数组 JSON
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

    /**
     * 更新场地信息 (支持部分更新)
     *
     * @param dto    更新参数
     * @param openId 操作人 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<Void> updateVenueBasicInfo(@Valid UpdateVenueDto dto,
                                                  @NotBlank String openId) {
        // 用于收集非致命错误消息
        List<String> warningMessages = new ArrayList<>();

        // 1. 查找用户与权限校验
        UserInfo operator = userRepository.findByOpenid(openId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

        Venue venue = venueRepository.findById(dto.getId())
                .orElseThrow(() -> new IllegalArgumentException("场地不存在"));

        boolean isManager = venue.getManager() != null && venue.getManager().getId().equals(operator.getId());
        boolean isSuperAdmin = "SUPER_ADMIN".equals(operator.getRole());

        // 核心权限卡点：如果连改场地的基本权限都没有，直接报错（这是致命错误）
        if (!isManager && !isSuperAdmin) {
            return ApiResponse.fail("您没有权限修改该场地信息");
        }

        // 2. 更新基础字段 (保持不变)
        if (StringUtils.hasText(dto.getName())) venue.setName(dto.getName());
        if (StringUtils.hasText(dto.getDescription())) venue.setDescription(dto.getDescription());
        if (StringUtils.hasText(dto.getAddress())) venue.setAddress(dto.getAddress());
        if (dto.getCapacity() != null) venue.setCapacity(dto.getCapacity());
        if (dto.getType() != null) venue.setType(dto.getType());
        if (dto.getStatus() != null) venue.setStatus(dto.getStatus());

        // 3. 更新管理员 (改为非致命错误处理)
        if (dto.getManagerId() != null) {
            if (isSuperAdmin) {
                UserInfo newManager = userRepository.findById(dto.getManagerId()).orElse(null);
                if (newManager != null) {
                    venue.setManager(newManager);
                } else {
                    warningMessages.add("指定的管理员ID不存在，未变更管理员");
                }
            } else {
                // 如果不是超管想改管理员，不报错，但记录警告
                warningMessages.add("您不是超级管理员，无法移交场地管理权");
            }
        }

        // 4. 更新封面图 (改为非致命错误处理)
        boolean hasNewCoverFile = dto.getCoverImageFile() != null && !dto.getCoverImageFile().isEmpty();
        boolean hasNewCoverUrl = StringUtils.hasText(dto.getCoverImageUrl());

        if (hasNewCoverFile || hasNewCoverUrl) {
            try {
                String newCoverPath;
                if (hasNewCoverFile) {
                    newCoverPath = FileUtil.saveImage(dto.getCoverImageFile(), venueAlbumUploadDir);
                } else {
                    newCoverPath = FileUtil.saveImageFromUrl(dto.getCoverImageUrl(), venueAlbumUploadDir);
                }

                // 只有新图成功保存了，才删除旧图并更新
                if (newCoverPath != null) {
                    if (StringUtils.hasText(venue.getCoverImage())) {
                        FileUtil.deletePhysicalFile(venue.getCoverImage());
                    }
                    venue.setCoverImage(newCoverPath);
                }
            } catch (IOException e) {
                logger.warning("更新封面图失败: " + e.getMessage());
                warningMessages.add("封面图上传失败，已保留原封面");
            }
        }

        // 5. 更新轮播图
        List<VenuePhotoInfo> currentPhotos = new ArrayList<>();
        if (StringUtils.hasText(venue.getPhotoList())) {
            try {
                currentPhotos = objectMapper.readValue(venue.getPhotoList(),
                        new com.fasterxml.jackson.core.type.TypeReference<>() {
                        });
                if (currentPhotos == null) currentPhotos = new ArrayList<>();
            } catch (JsonProcessingException e) {
                // 更详细的日志，包含原始 JSON 内容和异常，便于排查数据问题
                String raw = venue.getPhotoList();
                logger.warning("解析场地轮播图失败 ID=" + venue.getId() + " rawPhotoList=" + (raw == null ? "<null>" : raw));
                logger.log(java.util.logging.Level.WARNING, "解析轮播图JSON失败，venueId=" + venue.getId(), e);
                //dto.setPhotoList(new ArrayList<>());
            }
        }

        // A. 删除 (保持不变)
        if (dto.getDeletePhotoIds() != null) {
            // ... (同原代码)
            currentPhotos.removeIf(photo -> {
                if (dto.getDeletePhotoIds().contains(photo.getId())) {
                    FileUtil.deletePhysicalFile(photo.getUrl());
                    return true;
                }
                return false;
            });
        }

        // B. 替换 (改为非致命错误处理)
        if (StringUtils.hasText(dto.getReplacePhotoMap()) && dto.getReplaceFiles() != null) {
            try {
                java.util.Map<String, Integer> replaceMap = objectMapper.readValue(
                        dto.getReplacePhotoMap(),
                        new com.fasterxml.jackson.core.type.TypeReference<>() {
                        }
                );

                for (VenuePhotoInfo photo : currentPhotos) {
                    if (replaceMap.containsKey(photo.getId())) {
                        int fileIndex = replaceMap.get(photo.getId());
                        if (fileIndex >= 0 && fileIndex < dto.getReplaceFiles().size()) {
                            MultipartFile newFile = dto.getReplaceFiles().get(fileIndex);
                            try {
                                String newPath = FileUtil.saveImage(newFile, venueAlbumUploadDir);
                                if (newPath != null) {
                                    FileUtil.deletePhysicalFile(photo.getUrl());
                                    photo.setUrl(newPath);
                                    photo.setOriginalName(newFile.getOriginalFilename());
                                }
                            } catch (IOException e) {
                                // 单张失败不影响其他
                                logger.warning("替换图片失败: " + photo.getId());
                                warningMessages.add("图片 " + photo.getOriginalName() + " 替换失败");
                            }
                        }
                    }
                }
            } catch (Exception e) {
                warningMessages.add("替换指令解析失败，部分图片未替换");
            }
        }

        // 5.3 新增 URL (改为非致命)
        if (dto.getNewPhotoUrlList() != null) {
            for (String url : dto.getNewPhotoUrlList()) {
                try {
                    String path = FileUtil.saveImageFromUrl(url, venueAlbumUploadDir);
                    if (path != null) {
                        currentPhotos.add(new VenuePhotoInfo(UUID.randomUUID().toString(), path, "network-image"));
                    }
                } catch (IOException e) {
                    logger.warning("新增轮播图URL失败: " + url);
                    warningMessages.add("网络图片下载失败: " + url);
                }
            }
        }

        // 5.4 新增文件 (改为非致命)
        if (dto.getNewPhotoFiles() != null) {
            for (MultipartFile file : dto.getNewPhotoFiles()) {
                try {
                    String path = FileUtil.saveImage(file, venueAlbumUploadDir);
                    if (path != null) {
                        currentPhotos.add(new VenuePhotoInfo(UUID.randomUUID().toString(), path, file.getOriginalFilename()));
                    }
                } catch (IOException e) {
                    logger.warning("新增轮播图文件失败: " + file.getOriginalFilename());
                    warningMessages.add("文件上传失败: " + file.getOriginalFilename());
                }
            }
        }

        // 5.5 序列化
        try {
            venue.setPhotoList(objectMapper.writeValueAsString(currentPhotos));
        } catch (JsonProcessingException e) {
            // 这个属于严重错误，如果序列化都失败了，数据库数据会坏掉，建议还是报错
            logger.severe("轮播图序列化异常");
            return ApiResponse.fail("系统内部错误：图片列表保存失败");
        }

        // 6. 设备信息
        if (StringUtils.hasText(dto.getEquipmentInfo())) {
            try {
                objectMapper.readTree(dto.getEquipmentInfo());
                venue.setEquipmentInfo(dto.getEquipmentInfo());
            } catch (JsonProcessingException e) {
                // 非核心字段，可以选择报错，也可以选择忽略并警告
                warningMessages.add("设备信息格式错误，未更新设备列表");
            }
        }

        // 7. 保存
        venueRepository.save(venue);

        // ================== 构建返回消息 ==================

        ApiResponse<Void> response = ApiResponse.success(null);

        if (warningMessages.isEmpty()) {
            response.setMessage("场地信息更新完全成功");
        } else {
            // 如果有警告，拼接消息
            // 格式示例： "保存成功，但有以下问题：1. 封面图上传失败; 2. 网络图片下载失败"
            StringBuilder msg = new StringBuilder("保存成功，但部分操作未生效：");
            for (int i = 0; i < warningMessages.size(); i++) {
                msg.append(i + 1).append(". ").append(warningMessages.get(i)).append("; ");
            }
            response.setMessage(msg.toString());
        }

        return response;
    }

    /**
     * 删除场地
     *
     * @param venueId 场地ID
     * @param openId  操作人OpenID
     */
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<Void> deleteVenue(@NotNull Long venueId, @NotBlank String openId) {
        // 1. 身份校验
        UserInfo operator = userRepository.findByOpenid(openId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

        Venue venue = venueRepository.findById(venueId)
                .orElseThrow(() -> new IllegalArgumentException("场地不存在"));

        // 权限校验：只有超级管理员可以删除
        boolean isSuperAdmin = "SUPER_ADMIN".equals(operator.getRole());

        if (!isSuperAdmin) {
            logger.warning("用户尝试删除无权场地: " + openId);
            return ApiResponse.fail("您没有权限删除该场地");
        }

        // 删除数据库记录
        venueRepository.delete(venue);

        ApiResponse<Void> response = ApiResponse.success(null);
        response.setMessage("场地及相关资源已成功删除");
        return response;
    }

    /**
     * 获取场地详情 (包含由 JSON 转换而来的复杂对象)
     *
     * @param venueId 场地 ID
     * @return 场地详情 DTO
     */
    public ApiResponse<VenueDetailDto> getVenueDetail(Long venueId) {
        Venue venue = venueRepository.findById(venueId).orElse(null);
        if (venue == null) {
            return ApiResponse.fail("场地不存在");
        }

        VenueDetailDto dto = convertToDetailDto(venue);
        // 填充当天的开放时间与屏蔽标记
        try {
            java.time.LocalDate today = java.time.LocalDate.now();
            int dayOfWeek = today.getDayOfWeek().getValue(); // 1 (Monday) - 7 (Sunday)

            // 获取当天的开放时间（如果未配置则为 null）
            OpeningHoursDto todayHours = venueOpeningHoursService.getOpeningHoursForDay(venue.getId(), dayOfWeek);
            dto.setTodayOpeningHours(todayHours);

            // 检查当天是否被屏蔽（通过 service 提供的 helper）
            boolean blocked = venueOpeningHoursService.isBlockedOnDate(venue.getId(), today);
            dto.setTodayBlocked(blocked);
        } catch (Exception e) {
            logger.warning("填充当天开放时间失败: " + e.getMessage());
            dto.setTodayOpeningHours(null);
            dto.setTodayBlocked(false);
        }

        return ApiResponse.success(dto);
    }

    /**
     * 搜索/列表查询
     *
     * @param name   名称模糊搜索 (可选)
     * @param type   类型 (可选)
     * @param status 状态 (可选)
     * @return 场地列表
     */
     public ApiResponse<List<VenueDetailDto>> searchVenues(String name, Integer type, Integer status) {
         List<Venue> venues;

        // 简单的组合查询逻辑 (实际项目中可用 Specification 或 QueryDSL 优化)
        if (StringUtils.hasText(name)) {
            venues = venueRepository.findByNameContainingIgnoreCase(name);
            // 内存过滤其他条件
            if (type != null) venues = venues.stream().filter(v -> v.getType().equals(type)).collect(Collectors.toList());
            if (status != null) venues = venues.stream().filter(v -> v.getStatus().equals(status)).collect(Collectors.toList());
        } else if (type != null) {
            venues = venueRepository.findByType(type);
            if (status != null) venues = venues.stream().filter(v -> v.getStatus().equals(status)).collect(Collectors.toList());
        } else if (status != null) {
            venues = venueRepository.findByStatus(status);
        } else {
            venues = venueRepository.findAll();
        }

        List<VenueDetailDto> dtoList = venues.stream()
                .map(this::convertToDetailDto)
                .collect(Collectors.toList());

        // 为每个返回的 DTO 批量填充当天的开放时间与屏蔽标记，避免前端再额外调用接口（避免 N+1 查询）
        try {
            java.time.LocalDate today = java.time.LocalDate.now();
            java.util.List<Long> ids = dtoList.stream().map(VenueDetailDto::getId).collect(Collectors.toList());
            java.util.Map<String, java.util.Map<Long, Object>> bulk = venueOpeningHoursService.getBulkTodayInfo(ids, today);
            java.util.Map<Long, Object> hoursMap = bulk.getOrDefault("hours", new java.util.HashMap<>());
            java.util.Map<Long, Object> blockedMap = bulk.getOrDefault("blocked", new java.util.HashMap<>());

            for (VenueDetailDto dto : dtoList) {
                try {
                    Object h = hoursMap.get(dto.getId());
                    if (h instanceof OpeningHoursDto) dto.setTodayOpeningHours((OpeningHoursDto) h);
                    else dto.setTodayOpeningHours(null);

                    Object b = blockedMap.get(dto.getId());
                    dto.setTodayBlocked(b instanceof Boolean ? (Boolean) b : false);
                } catch (Exception inner) {
                    dto.setTodayOpeningHours(null);
                    dto.setTodayBlocked(false);
                }
            }
        } catch (Exception e) {
            logger.warning("填充列表当天开放时间失败: " + e.getMessage());
        }

         return ApiResponse.success(dtoList);
    }

    /**
     * 私有辅助方法：Entity -> DTO 转换器
     */
    private VenueDetailDto convertToDetailDto(Venue venue) {
        VenueDetailDto dto = VenueDetailDto.fromEntitySimple(venue);

        if (StringUtils.hasText(venue.getCoverImage())) {
            dto.setCoverImage(AvatarUrlUtil.buildAvatarUrl(venue.getCoverImage(), fileBaseUrl));
        }

        // 1. 处理管理员信息 (关键：脱敏)
        if (venue.getManager() != null) {
            VenueManagerDto managerDto = new VenueManagerDto(
                    venue.getManager().getId(),
                    venue.getManager().getNickname(),// 使用昵称作为姓名
                    AvatarUrlUtil.buildAvatarUrl(venue.getManager().getAvatar(), fileBaseUrl),
                    venue.getManager().getPhone()
            );
            dto.setManager(managerDto);
        }

        // 2. 处理图片列表 JSON -> List
        // 3. 处理图片列表 JSON -> List (并拼接前缀)
        if (StringUtils.hasText(venue.getPhotoList())) {
            String raw = venue.getPhotoList();
            List<VenuePhotoInfo> photos = null;

            // 优先：把 photoList 解析为字符串数组（每个元素都是地址），并映射成 VenuePhotoInfo
            try {
                List<String> urlList = objectMapper.readValue(raw, new com.fasterxml.jackson.core.type.TypeReference<>() {});
                if (urlList != null) {
                    photos = new ArrayList<>();
                    for (String u : urlList) {
                        photos.add(new VenuePhotoInfo(UUID.randomUUID().toString(), u == null ? "" : u, ""));
                    }
                }
            } catch (Exception ignored) {}

            // 次优：正常解析为 List<VenuePhotoInfo>
            if (photos == null) {
                try {
                    photos = objectMapper.readValue(raw, new com.fasterxml.jackson.core.type.TypeReference<>() {});
                } catch (Exception ignored) {}
            }

            // 其它回退：保留之前的多种策略（双重序列化、单对象包数组、单引号、宽松 Map->对象）
            if (photos == null) {
                try {
                    if (raw.startsWith("\"") && raw.endsWith("\"")) {
                        String unwrapped = objectMapper.readValue(raw, String.class);
                        photos = objectMapper.readValue(unwrapped, new com.fasterxml.jackson.core.type.TypeReference<>() {});
                    }
                } catch (Exception ignored) {}
            }

            if (photos == null) {
                try {
                    if (raw.trim().startsWith("{") && raw.trim().endsWith("}")) {
                        String wrapped = "[" + raw + "]";
                        photos = objectMapper.readValue(wrapped, new com.fasterxml.jackson.core.type.TypeReference<>() {});
                    }
                } catch (Exception ignored) {}
            }

            if (photos == null) {
                try {
                    if (raw.contains("'")) {
                        String replaced = raw.replace('\'', '"');
                        photos = objectMapper.readValue(replaced, new com.fasterxml.jackson.core.type.TypeReference<>() {});
                    }
                } catch (Exception ignored) {}
            }

            if (photos == null) {
                try {
                    List<java.util.Map<String, Object>> list = objectMapper.readValue(raw, new com.fasterxml.jackson.core.type.TypeReference<>() {});
                    photos = new ArrayList<>();
                    if (list != null) {
                        for (java.util.Map<String, Object> m : list) {
                            String id = m.getOrDefault("id", m.getOrDefault("ID", "")).toString();
                            String url = m.getOrDefault("url", m.getOrDefault("path", m.getOrDefault("image", ""))).toString();
                            String orig = m.getOrDefault("originalName", m.getOrDefault("original_name", "")).toString();
                            photos.add(new VenuePhotoInfo(id, url, orig));
                        }
                    }
                } catch (Exception ignored) {}
            }

            if (photos != null) {
                for (VenuePhotoInfo photo : photos) {
                    if (StringUtils.hasText(photo.getUrl())) {
                        photo.setUrl(AvatarUrlUtil.buildAvatarUrl(photo.getUrl(), fileBaseUrl));
                    }
                }
                dto.setPhotoList(photos);
            } else {
                // 全部解析尝试失败，记录详细日志便于排查
                logger.warning("解析场地轮播图失败 ID=" + venue.getId() + " rawPhotoList=" + (raw == null ? "<null>" : raw));
                logger.warning("尝试了多种解析策略仍失败，已回退为空列表");
                dto.setPhotoList(new ArrayList<>());
            }
        } else {
            dto.setPhotoList(new ArrayList<>());
        }

        // 3. 处理设备信息 JSON -> Object
        if (StringUtils.hasText(venue.getEquipmentInfo())) {
            try {
                Object eq = objectMapper.readValue(venue.getEquipmentInfo(), Object.class);
                dto.setEquipmentInfo(eq);
            } catch (JsonProcessingException e) {
                dto.setEquipmentInfo(null);
            }
        }

        return dto;
    }

    /**
     * 定时任务：每天凌晨3点清理已删除的场地图片文件
     * 逻辑：查找 deleted_at 不为空的记录 -> 删物理文件 -> 删数据库物理记录
     */
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional // 确保 physicalDeleteById 的执行和异常回滚
    public void purgeSoftDeletedVenues() {
        logger.info("开始执行场地清理定时任务...");

        // 1. 获取所有软删除的场地 (使用原生SQL查询)
        List<Venue> toPurge = venueRepository.findAllSoftDeletedVenues();

        if (toPurge.isEmpty()) {
            logger.info("没有需要清理的已删除场地。");
            return;
        }

        int filesDeletedCount = 0;
        int dbRecordsDeletedCount = 0;

        for (Venue v : toPurge) {
            try {
                // 2. 清理封面图
                if (StringUtils.hasText(v.getCoverImage())) {
                    boolean success = FileUtil.deletePhysicalFile(v.getCoverImage());
                    if (success) filesDeletedCount++;
                }

                // 3. 清理轮播图
                if (StringUtils.hasText(v.getPhotoList())) {
                    try {
                        List<VenuePhotoInfo> photos = objectMapper.readValue(v.getPhotoList(),
                                new com.fasterxml.jackson.core.type.TypeReference<>() {});

                        if (photos != null) {
                            for (VenuePhotoInfo photo : photos) {
                                if (StringUtils.hasText(photo.getUrl())) {
                                    boolean success = FileUtil.deletePhysicalFile(photo.getUrl());
                                    if (success) filesDeletedCount++;
                                }
                            }
                        }
                    } catch (JsonProcessingException e) {
                        // 仅记录日志，不阻断后续的数据库删除
                        logger.warning("清理场地[ID:" + v.getId() + "]时，轮播图JSON解析失败: " + e.getMessage());
                    }
                }

                // 4. 物理删除数据库记录 (关键：使用原生SQL删除)
                venueRepository.physicalDeleteById(v.getId());
                dbRecordsDeletedCount++;

            } catch (Exception e) {
                // 捕获单个场地的处理异常，防止因为一条数据有问题导致整个定时任务中断
                logger.severe("清理场地[ID:" + v.getId() + "]失败: " + e.getMessage());
            }
        }

        logger.info("VenueCleanupJob 完成：物理移除场地 " + dbRecordsDeletedCount + " 条，删除文件 " + filesDeletedCount + " 个");
    }

    /**
     * 查询场馆在指定日期区间内的所有场次（轻量 DTO）
     *
     * @param venueId   场馆 ID
     * @param startDate yyyy-MM-dd 范围开始（包含）
     * @param endDate   yyyy-MM-dd 范围结束（包含）
     * @return 事件列表（不包含票数，仅包含你需要的字段）
     */
    public List<VenueEventDto> getEventsForVenue(Long venueId, LocalDate startDate, LocalDate endDate) {
        if (venueId == null || startDate == null || endDate == null) {
            return java.util.Collections.emptyList();
        }

        long spanDays = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate);
        if (spanDays < 0 || spanDays > 365) {
            throw new IllegalArgumentException("查询区间不合法或跨度过大（建议 ≤ 365 天）");
        }

        // 左闭右开时间范围：startDate.atStartOfDay() .. endDate.plusDays(1).atStartOfDay()
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

        // 使用 repository 中已有的 findConflicts 查询（s.startTime < :end AND s.endTime > :start）
        List<PerformanceSession> sessions = performanceSessionRepository.findConflicts(venueId, startDateTime, endDateTime);
        if (sessions == null || sessions.isEmpty()) return java.util.Collections.emptyList();

        return sessions.stream().map(s -> {
            VenueEventDto dto = new VenueEventDto();
            dto.setSessionId(s.getId());

            if (s.getPerformance() != null) {
                dto.setPerformanceId(s.getPerformance().getId());
                dto.setPerformanceName(s.getPerformance().getTitle());
                // 从 performance 的 organizerType / organizerId 获取举办者名称
                String organizerName = resolveOrganizerName(
                        s.getPerformance().getOrganizerType(),
                        s.getPerformance().getOrganizerId()
                );
                dto.setOrganizerName(StringUtils.hasText(organizerName) ? organizerName : null);
            }

            if (s.getStartTime() != null) {
                dto.setStartTime(s.getStartTime());
                dto.setPerformanceDate(s.getStartTime().toLocalDate());
            }
            if (s.getEndTime() != null) dto.setEndTime(s.getEndTime());

            return dto;
        }).sorted((a, b) -> {
            LocalDateTime at = a.getStartTime();
            LocalDateTime bt = b.getStartTime();
            if (at == null && bt == null) return 0;
            if (at == null) return 1;
            if (bt == null) return -1;
            return at.compareTo(bt);
        }).collect(Collectors.toList());
    }

    /**
     * 根据 organizerType + organizerId 解析举办方名称
     * 支持：USER / ORGANIZATION
     */
    private String resolveOrganizerName(String organizerType, Long organizerId) {
        if (!StringUtils.hasText(organizerType) || organizerId == null) return null;

        if ("USER".equalsIgnoreCase(organizerType)) {
            return userRepository.findById(organizerId)
                    .map(u -> { // UserInfo 中使用 getNickname 作为展示名（与你之前的代码一致）
                        try {
                            return u.getNickname();
                        } catch (Exception e) {
                            return null;
                        }
                    })
                    .orElse(null);
        }

        if ("ORGANIZATION".equalsIgnoreCase(organizerType)) {
            // 组织机构表可能有 name 字段
            return organizationRepository.findById(organizerId)
                    .map(org -> {
                        try {
                            // 假定 Organization 实体有 getName()
                            return org.getName();
                        } catch (Exception e) {
                            return null;
                        }
                    })
                    .orElse(null);
        }

        // 其它类型可以扩展
        return null;
    }
}


