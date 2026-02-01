package org.example.campus_performance_ticketing.logic.dto.performance_comment;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AuditCommentCmd {

    @NotNull(message = "评论 ID不能为空")
    private Long commentId;

    /**
     * 目标状态: 1-正常, 0-隐藏/违规
     */
    @NotNull(message = "状态不能为空")
    private Integer status;
}