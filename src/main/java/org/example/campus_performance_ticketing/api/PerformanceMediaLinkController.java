package org.example.campus_performance_ticketing.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.campus_performance_ticketing.logic.PerformanceMediaLinkService;
import org.example.campus_performance_ticketing.logic.dto.ApiResponse;
import org.example.campus_performance_ticketing.logic.dto.performance_media_link.CreateMediaLinkCmd;
import org.example.campus_performance_ticketing.logic.dto.performance_media_link.MediaLinkDto;
import org.example.campus_performance_ticketing.model.PerformanceMediaLink;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
@Validated
public class PerformanceMediaLinkController {

    private final PerformanceMediaLinkService mediaLinkService;

    /**
     * 上传/添加媒体外链
     * POST /api/media/add
     */
    @PostMapping("/add")
    public ApiResponse<Void> addMediaLink(HttpServletRequest request,
                                          @RequestBody @Valid CreateMediaLinkCmd cmd) {
        String operatorOpenId = (String) request.getAttribute("openid");
        return mediaLinkService.addMediaLink(operatorOpenId, cmd);
    }

    /**
     * 删除媒体外链 (支持举办者自删，或管理员强制下架)
     * DELETE /api/media/{mediaLinkId}
     */
    @DeleteMapping("/{mediaLinkId}")
    public ApiResponse<Void> deleteMediaLink(HttpServletRequest request,
                                             @PathVariable Long mediaLinkId) {
        String operatorOpenId = (String) request.getAttribute("openid");
        return mediaLinkService.deleteMediaLink(operatorOpenId, mediaLinkId);
    }

    /**
     * 查询某演出的所有外链
     * GET /api/media/list?performanceId=1
     */
    @GetMapping("/list")
    public ApiResponse<List<MediaLinkDto>> getMediaLinks(@RequestParam Long performanceId) {
        return mediaLinkService.getMediaLinks(performanceId);
    }
}