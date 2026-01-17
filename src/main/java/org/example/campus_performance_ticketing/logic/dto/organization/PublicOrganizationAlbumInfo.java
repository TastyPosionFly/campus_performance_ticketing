package org.example.campus_performance_ticketing.logic.dto.organization;

import lombok.Data;
import org.example.campus_performance_ticketing.logic.dto.user.PublicUserInfo;

@Data
public class PublicOrganizationAlbumInfo {
    private Long id;
    private PublicUserInfo uploader;
    private String photoUrl;
    private String uploadTime;
}
