package org.example.campus_performance_ticketing.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.example.campus_performance_ticketing.logic.TicketService;
import org.example.campus_performance_ticketing.logic.TicketTemplateService;
import org.example.campus_performance_ticketing.logic.dto.ApiResponse;
import org.example.campus_performance_ticketing.logic.dto.ticket.*;
import org.springframework.data.domain.Page;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/ticket")
@RequiredArgsConstructor
@Validated
public class TicketController {

    private final TicketTemplateService ticketTemplateService;
    private final TicketService ticketService;

    /**
     * 创建或更新电子票模板（图片必传）
     * 场次列表会覆盖原有的关联关系
     * @param request
     * @param sessionIds
     * @param status
     * @param imageFile
     * @return
     */
    @PostMapping("/template/upload")
    public ApiResponse<Void> uploadTicketTemplate(
            HttpServletRequest request,
            @RequestParam("sessionIds") List<Long> sessionIds,
            @RequestParam(value = "status", required = false, defaultValue = "1") Integer status,
            @RequestPart("imageFile") MultipartFile imageFile
    ) {
        String openId = (String) request.getAttribute("openid");

        TicketTemplateUploadDTO dto = new TicketTemplateUploadDTO();
        dto.setSessionIds(sessionIds);
        dto.setStatus(status);

        return ticketTemplateService.createOrUpdateTicketTemplate(openId, dto, imageFile);
    }

    /**
     * 更新电子票模板（图片可选传）
     * @param request
     * @param sessionIds
     * @param status
     * @param imageFile
     * @return
     */
    @PostMapping("/template/update")
    public ApiResponse<Void> updateTicketTemplate(
            HttpServletRequest request,
            @RequestParam("sessionIds") List<Long> sessionIds,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestPart(value = "imageFile", required = false) MultipartFile imageFile
    ) {
        String openId = (String) request.getAttribute("openid");

        TicketTemplateUpdateDTO dto = new TicketTemplateUpdateDTO();
        dto.setSessionIds(sessionIds);
        dto.setStatus(status);
        dto.setImageFile(imageFile); // DTO 里字段仅用于逻辑传递

        return ticketTemplateService.updateTicketTemplate(openId, dto, imageFile);
    }

    /**
     * 获取指定场次生效的电子票背景图 (仅上架状态)
     */
    @GetMapping("/template/url/{sessionId}")
    public ApiResponse<String> getTicketTemplateUrl(@PathVariable Long sessionId) {
        return ticketTemplateService.getActiveTicketTemplateUrl(sessionId);
    }

    /**
     * 用户预约/抢票
     * 权限：需登录
     */
    @PostMapping("/book")
    public ApiResponse<TicketDetailDTO> bookTicket(
            HttpServletRequest request,
            @RequestBody @Valid TicketBookingDTO dto
    ) {
        String openId = (String) request.getAttribute("openid");
        return ticketService.bookTicket(openId, dto);
    }

    /**
     * 获取单张票据详情
     * 权限：需登录，且只能查看自己的票
     */
    @GetMapping("/{ticketId}")
    public ApiResponse<TicketDetailDTO> getTicketDetail(
            HttpServletRequest request,
            @PathVariable Long ticketId
    ) {
        String openId = (String) request.getAttribute("openid");
        return ticketService.getTicketDetail(openId, ticketId);
    }

    /**
     * 获取我的票夹列表 (分页)
     * 权限：需登录
     *
     * GET /api/ticket/my?page=0&size=10
     * GET /api/ticket/my?page=0&size=10&status=0 (只看已预约)
     * GET /api/ticket/my?page=0&size=10&performanceId=123 (只看某演出的票)
     * GET /api/ticket/my?page=0&size=10&performanceId=123&status=0 (只看某演出的已预约票)
     * GET /api/ticket/my?page=0&size=10&upcomingFirst=true (优先显示即将开始的票)
     */
    @GetMapping("/my")
    public ApiResponse<Page<TicketDetailDTO>> getMyTickets(
            HttpServletRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long performanceId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) boolean upcomingFirst // 是否优先显示即将开始的票
    ) {
        String openId = (String) request.getAttribute("openid");
        return ticketService.getMyTickets(openId, page, size, performanceId, status, upcomingFirst);
    }

    /**
     * 管理员或演出的举办者或者演出举办组织的成员扫码核销 (检票)
     * 权限：需登录 (Service层会进一步校验是否为该场地的管理员)
     * 参数通过 Query String 传递: POST /api/ticket/check-in?ticketCode=XXXX
     */
    @PostMapping("/check-in")
    public ApiResponse<Void> checkInTicket(
            HttpServletRequest request,
            @RequestParam @Valid @NotBlank(message = "核销码不能为空") String ticketCode
    ) {
        String openId = (String) request.getAttribute("openid");
        return ticketService.checkInTicket(openId, ticketCode);
    }

    /**
     * 获取实际到场人员名单
     */
    @GetMapping("/attendance/{sessionId}")
    public ApiResponse<Page<TicketAttendanceDTO>> getAttendanceList(
            HttpServletRequest request,
            @PathVariable Long sessionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        String openId = (String) request.getAttribute("openid");
        return ticketService.getAttendanceList(openId, sessionId, page, size);
    }

    /**
     * 导出实际到场人员名单为 Excel
     */
    @GetMapping("/attendance/{sessionId}/export")
    public void exportAttendanceExcel(
            HttpServletRequest request,
            HttpServletResponse response,
            @PathVariable Long sessionId
    ) throws Exception {
        String openId = (String) request.getAttribute("openid");
        ticketService.exportAttendanceExcelForWeixin(openId, sessionId, response);
    }
}
