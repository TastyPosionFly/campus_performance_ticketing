package org.example.campus_performance_ticketing.logic.dto.venue;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Map;

@Data
public class CreateVenueDto {
    @NotBlank(message = "场地名称不能为空")
    @Size(max = 50, message = "场地名称不能超过50个字符")
    private String name;

    @Size(max = 1000, message = "描述过长")
    private String description;

    @NotBlank(message = "场地地址不能为空")
    private String address;

    private String coverImageUrl;
    private MultipartFile coverImageFile;

    private List<String> photoUrlList;
    private List<MultipartFile> photoFiles;

    @NotNull(message = "容纳人数不能为空")
    @Min(value = 1, message = "容纳人数至少为1")
    private Integer capacity;

    @NotNull(message = "场地类型不能为空")
    private Integer type;

    private String equipmentInfo;

    private Long managerId;
}