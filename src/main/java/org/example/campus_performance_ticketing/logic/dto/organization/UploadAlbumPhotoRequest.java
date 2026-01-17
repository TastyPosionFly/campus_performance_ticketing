package org.example.campus_performance_ticketing.logic.dto.organization;

import lombok.Data;

// 上传相册图片请求
@Data
public class UploadAlbumPhotoRequest {
    private Long organizationId;
    private String photoUrl;
    private String description;
}
