package org.example.campus_performance_ticketing.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.example.campus_performance_ticketing.logic.VenueService;
import org.example.campus_performance_ticketing.logic.dto.ApiResponse;
import org.example.campus_performance_ticketing.logic.dto.venue.CreateVenueDto;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/venues")
public class VenueController {

    private final VenueService venueService;

    public VenueController(VenueService venueService) {
        this.venueService = venueService;
    }

    /**
     * 创建场地
     *
     * @param dto 前端表单数据 (包含文件和普通字段)
     *            注意：前端需要使用 FormData 发送请求
     *            Content-Type: multipart/form-data
     * @param  request HttpServletRequest 对象，用于获取拦截器中设置的 openId
     * @return 响应结果
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Void> createVenue(
            @Valid @ModelAttribute CreateVenueDto dto,
            HttpServletRequest request
    ) {
        String openId = (String) request.getAttribute("openid");
        // 调用 Service
        return venueService.createVenue(dto, openId);
    }
}