package org.example.campus_performance_ticketing.logic.dto.ticket;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
public class TicketTemplateUpdateDTO {

    /**
     * 关联的场次ID列表 (必填，确定要改哪些场次的模板)
     */
    @NotEmpty(message = "场次 ID 不能为空")
    private List<Long> sessionIds;

    /**
     * 新的状态: 0-下架 1-上架 (选填)
     * 如果为 null，则不修改原状态
     */
    private Integer status;

    /**
     * 仅用于逻辑传递，Controller 层手动注入
     * 如果为 null，则不修改原图片
     */
    private MultipartFile imageFile;
}