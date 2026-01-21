package org.example.campus_performance_ticketing.logic.dto.venue;

import lombok.Data;
import org.example.campus_performance_ticketing.model.Venue;
import org.springframework.beans.BeanUtils;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class VenueDetailDto {

    private Long id;
    private String name;
    private String description;
    private String address;
    private String coverImage;

    /**
     * 将 JSON 字符串转为对象列表给前端
     */
    private List<VenuePhotoInfo> photoList;

    private Integer capacity;
    private Integer type;

    /**
     * 将 JSON 字符串转为通用对象给前端
     */
    private Object equipmentInfo;

    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 精简后的管理员信息
     */
    private VenueManagerDto manager;

    /**
     * 静态工厂方法：从 Entity 转换为 DTO
     * (实际转换逻辑通常在 Service 中处理复杂字段)
     */
    public static VenueDetailDto fromEntitySimple(Venue venue) {
        VenueDetailDto dto = new VenueDetailDto();
        BeanUtils.copyProperties(venue, dto, "photoList", "equipmentInfo", "manager");
        return dto;
    }
}