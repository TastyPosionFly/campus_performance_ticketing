package org.example.campus_performance_ticketing.logic.dto.performance_media_link;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

@Data
public class CreateMediaLinkCmd {

    @NotNull(message = "演出 ID 不能为空")
    private Long performanceId;

    /**
     * 资源类型: 1-录像回放 2-在线直播
     */
    @NotNull(message = "资源类型不能为空")
    private Integer type;

    /**
     * 平台: 1-Bilibili 2-微信视频号 3-其他链接
     */
    @NotNull(message = "平台类型不能为空")
    private Integer platform;

    /**
     * 核心外链数据 (URL 或 BV号)
     */
    @NotBlank(message = "外链地址不能为空")
    @Length(max = 500, message = "链接过长")
    private String externalKey;

    @Length(max = 100, message = "标题不能超过100字")
    private String title;

    private Integer sortOrder = 0;

    // 小程序跳转专用 (可选)
    private String appId;
    private String path;
}