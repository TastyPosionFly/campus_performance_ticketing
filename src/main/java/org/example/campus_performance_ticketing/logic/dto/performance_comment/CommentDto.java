package org.example.campus_performance_ticketing.logic.dto.performance_comment;

import lombok.Data;
import org.example.campus_performance_ticketing.model.PerformanceComment;
import org.example.campus_performance_ticketing.util.AvatarUrlUtil;

import java.time.LocalDateTime;

@Data
public class CommentDto {

    private Long id;
    private String content;
    private Integer status;
    private LocalDateTime createTime;

    // 评论者信息
    private Long userId;
    private Integer userStatus;
    private String userStatusDesc;
    private String nickname;
    private String avatarUrl;

    public static CommentDto from(PerformanceComment comment, String baseUrl) {
        CommentDto dto = new CommentDto();
        dto.setId(comment.getId());
        dto.setContent(comment.getContent());
        dto.setStatus(comment.getStatus());
        dto.setCreateTime(comment.getCreateTime());

        if (comment.getUser() != null) {
            dto.setUserId(comment.getUser().getId());
            dto.setNickname(comment.getUser().getNickname());
            dto.setUserStatusDesc(comment.getUser().getStatus());
            // 处理头像全路径
            dto.setAvatarUrl(AvatarUrlUtil.buildAvatarUrl(comment.getUser().getAvatar(), baseUrl));
        }
        return dto;
    }

    /**
     * 账号状态
     * 0-封禁 1-正常
     */
    public void setUserStatusDesc(Integer userStatus) {
        this.userStatus = userStatus;
        switch (userStatus) {
            case 0 -> this.userStatusDesc = "封禁";
            case 1 -> this.userStatusDesc = "正常";
            default -> this.userStatusDesc = "未知状态";
        }
    }
}