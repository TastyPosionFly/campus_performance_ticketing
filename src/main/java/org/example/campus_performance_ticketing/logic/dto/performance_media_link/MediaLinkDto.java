package org.example.campus_performance_ticketing.logic.dto.performance_media_link;

import lombok.Data;
import org.example.campus_performance_ticketing.model.PerformanceMediaLink;

import java.time.LocalDateTime;

@Data
public class MediaLinkDto {
    private Long id;
    private Long performanceId; // 只返回 ID，不返回整个对象
    private Integer type;
    private String typeName;
    private Integer platform;
    private String platformName;
    private String externalKey;
    private String title;
    private Integer sortOrder;
    private String path;
    private LocalDateTime createTime;

    // 静态工厂方法：Entity -> DTO
    public static MediaLinkDto from(PerformanceMediaLink entity) {
        MediaLinkDto dto = new MediaLinkDto();
        dto.setId(entity.getId());
        dto.setPerformanceId(entity.getPerformance().getId());
        dto.setType(entity.getType());
        dto.setPlatform(entity.getPlatform());
        dto.setExternalKey(entity.getExternalKey());
        dto.setTitle(entity.getTitle());
        dto.setSortOrder(entity.getSortOrder());
        dto.setPath(entity.getPath());
        dto.setCreateTime(entity.getCreateTime());
        return dto;
    }

    public void setType(Integer type) {
        if (type == 1) {
            this.typeName = "录像回放";
        } else if (type == 2) {
            this.typeName = "在线直播";
        } else {
            this.typeName = "未知类型";
        }
    }

    public void setPlatform(Integer platform) {
        if (platform == 1) {
            this.platformName = "Bilibili";
        } else if (platform == 2) {
            this.platformName = "微信视频号";
        } else if (platform == 3) {
            this.platformName = "其他链接";
        } else {
            this.platformName = "未知平台";
        }
    }

}