package org.example.campus_performance_ticketing.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.campus_performance_ticketing.logic.PerformanceCommentService;
import org.example.campus_performance_ticketing.logic.dto.ApiResponse;
import org.example.campus_performance_ticketing.logic.dto.performance_comment.AuditCommentCmd;
import org.example.campus_performance_ticketing.logic.dto.performance_comment.CommentDto;
import org.example.campus_performance_ticketing.logic.dto.performance_comment.CreateCommentCmd;
import org.springframework.data.domain.Page;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/comment")
@RequiredArgsConstructor
@Validated
public class PerformanceCommentController {

    private final PerformanceCommentService commentService;

    /**
     * 用户发表评论
     * POST /api/comment/post
     */
    @PostMapping("/post")
    public ApiResponse<Void> postComment(HttpServletRequest request,
                                         @RequestBody @Valid CreateCommentCmd cmd) {
        String userOpenId = (String) request.getAttribute("openid");
        return commentService.postComment(userOpenId, cmd);
    }

    /**
     * 分页查询某演出的评论列表 （可选状态过滤：0-隐藏 1-正常）
     * GET /api/comment/list?performanceId=1&page=0&size=10
     */
    @GetMapping("/list")
    public ApiResponse<Page<CommentDto>> getComments(
            @RequestParam Long performanceId,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return commentService.getComments(performanceId, status, page, size);
    }

    // --- 管理员接口 ---

    /**
     * [管理员/超管] 审核评论 (修改状态: 隐藏/正常)
     * PUT /api/comment/audit
     */
    @PutMapping("/audit")
    public ApiResponse<Void> auditComment(HttpServletRequest request,
                                          @RequestBody @Valid AuditCommentCmd cmd) {
        String operatorOpenId = (String) request.getAttribute("openid");
        return commentService.auditComment(operatorOpenId, cmd);
    }

    /**
     * [超管] 物理删除评论
     * DELETE /api/comment/{commentId}
     */
    @DeleteMapping("/{commentId}")
    public ApiResponse<Void> deleteComment(HttpServletRequest request,
                                           @PathVariable Long commentId) {
        String operatorOpenId = (String) request.getAttribute("openid");
        return commentService.deleteComment(operatorOpenId, commentId);
    }
}