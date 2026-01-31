package org.example.campus_performance_ticketing.logic.dto.ticket;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
public class TicketTemplateUploadDTO {

    /**
     * 关联的场次ID列表
     * 支持一次性给多个场次设置同一张背景图
     */
    @NotEmpty(message = "场次 ID 不能为空")
    private List<Long> sessionIds;


    /**
     * 初始状态：0-下架 1-上架
     * 默认为 1
     */
    private Integer status = 1;
}