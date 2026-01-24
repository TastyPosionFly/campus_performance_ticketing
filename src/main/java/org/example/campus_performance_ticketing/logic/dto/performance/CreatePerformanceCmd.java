package org.example.campus_performance_ticketing.logic.dto.performance;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/** 提交演出申请/创建演出命令 */
@Data
public class CreatePerformanceCmd {
    @NotBlank(message = "标题不能为空")
    private String title;

    private String description;
    private String posterUrl;
    private Integer categoryId;

    // 申请者身份：USER 或 ORGANIZATION
    @NotBlank
    private String organizerType;

    // 如果是 USER，通常从 Token 解析；如果是 ORGANIZATION，前端需传 orgId
    @NotNull
    private Long organizerId;

    @NotEmpty(message = "至少包含一个场次")
    @Valid
    private List<SessionCmd> sessions;

    @Valid
    private List<StaffCmd> staffList;

    // 扩展字段：申请理由（存入 Application.extraData）
    private String applyReason;
}
