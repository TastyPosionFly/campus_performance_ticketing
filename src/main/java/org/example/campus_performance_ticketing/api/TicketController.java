package org.example.campus_performance_ticketing.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.campus_performance_ticketing.logic.TicketTemplateService;
import org.example.campus_performance_ticketing.logic.dto.ApiResponse;
import org.example.campus_performance_ticketing.logic.dto.ticket.TicketTemplateUpdateDTO;
import org.example.campus_performance_ticketing.logic.dto.ticket.TicketTemplateUploadDTO;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/ticket")
@RequiredArgsConstructor
@Validated
public class TicketController {

    private final TicketTemplateService ticketTemplateService;

    /**
     * 上传电子票模板 (图片)
     * @param request
     * @param dto
     * @param imageFile
     * @return
     */
    @PostMapping("/template/upload")
    public ApiResponse<Void> uploadTicketTemplate(
            HttpServletRequest request,
            @RequestPart("data") @Valid TicketTemplateUploadDTO dto,
            @RequestPart(value = "imageFile", required = true) MultipartFile imageFile
    ) {
        String openId = (String) request.getAttribute("openid");
        return ticketTemplateService.createOrUpdateTicketTemplate(openId, dto, imageFile);
    }

    /**
     * 更新电子票模板 (图片可选)
     * @param request
     * @param dto
     * @param imageFile
     * @return
     */
    @PostMapping("/template/update")
    public ApiResponse<Void> updateTicketTemplate(
            HttpServletRequest request,
            @RequestPart("dto") TicketTemplateUpdateDTO dto,
            @RequestPart(value = "imageFile", required = false) MultipartFile imageFile // 允许为空
    ) {
        String openId = (String) request.getAttribute("openid");
        return ticketTemplateService.updateTicketTemplate(openId, dto, imageFile);
    }

    /**
     * 获取指定场次生效的电子票背景图 (仅上架状态)
     */
    @GetMapping("/template/url/{sessionId}")
    public ApiResponse<String> getTicketTemplateUrl(@PathVariable Long sessionId) {
        return ticketTemplateService.getActiveTicketTemplateUrl(sessionId);
    }
}
