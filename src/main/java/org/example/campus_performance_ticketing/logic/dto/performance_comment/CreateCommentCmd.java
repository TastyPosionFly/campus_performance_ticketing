package org.example.campus_performance_ticketing.logic.dto.performance_comment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 发表评论请求参数
 */
@Data
public class CreateCommentCmd {

    /**
     * 关联的演出 ID
     */
    @NotNull(message = "演出 ID 不能为空")
    private Long performanceId;

    /**
     * 评论内容
     */
    @NotBlank(message = "评论内容不能为空")
    @Size(min = 1, max = 1000, message = "评论内容长度需在1-1000字之间")
    private String content;
}