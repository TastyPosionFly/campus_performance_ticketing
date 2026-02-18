package org.example.campus_performance_ticketing.api;

import org.example.campus_performance_ticketing.logic.UserService;
import org.example.campus_performance_ticketing.logic.dto.ApiResponse;
import org.example.campus_performance_ticketing.model.UserInfo;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import org.springframework.util.StringUtils;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 封禁 / 解封用户（管理员）
     */
    @PutMapping("/ban")
    public ApiResponse<UserInfo> banOrUnban(
            HttpServletRequest request,
            @RequestParam String openId,
            @RequestParam Boolean ban
    ) {
        // 从拦截器获取当前管理员角色
        String role = (String) request.getAttribute("role");

        // 权限校验在 Service 层完成
        return userService.banOrUnbanUser(openId, ban, role);
    }

    /**
     * 查询用户列表（ADMIN, SUPER_ADMIN）
     */
    @GetMapping("/list")
    public ApiResponse<org.springframework.data.domain.Page<UserInfo>> listUsers(
            HttpServletRequest request,
            @RequestParam(name = "page", required = false, defaultValue = "0") int page,
            @RequestParam(name = "size", required = false, defaultValue = "20") int size,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "userRole", required = false) String userRole
    ) {
        // 从拦截器获取当前管理员角色
        String role = (String) request.getAttribute("role");

        // 如果传了 keyword 则走分页模糊搜索，否则返回全部分页
        if (StringUtils.hasText(keyword)) {
            return userService.searchUsersByNicknamePaginated(keyword, role, userRole, page, size);
        }

        // 权限校验与分页查询在 Service 层完成
        return userService.getAllUsersPaginated(role, page, size);
    }

    /**
     * SUPER_ADMIN专用：更改用户权限
     */
    @PutMapping("/role")
    public ApiResponse<UserInfo> changeUserRole(
            HttpServletRequest request,
            @RequestParam String openId,
            @RequestParam String newRole
    ) {
        // 从拦截器获取当前管理员角色
        String role = (String) request.getAttribute("role");

        // 权限校验在 Service 层完成
        return userService.updateUserRole(openId, newRole, role);
    }

    /**
     * 根据用户名查询用户（精确匹配）
     */
    @GetMapping("/search")
    public ApiResponse<?> getUserByNickname(
            HttpServletRequest request,
            @RequestParam String nickname
    ) {
        // 从拦截器获取当前管理员角色
        String role = (String) request.getAttribute("role");

        // 权限校验在 Service 层完成
        return userService.findUserByNickname(nickname, role);
    }

    /**
     * 根据用户名模糊搜索用户列表
     */
    @GetMapping("/search/fuzzy")
    public ApiResponse<Page<UserInfo>> searchUsers(
            HttpServletRequest request,
            @RequestParam String keyword,
            @RequestParam(required = false) String userRole,
            @RequestParam(name = "page", required = false, defaultValue = "0") int page,
            @RequestParam(name = "size", required = false, defaultValue = "20") int size
    ) {
        // 从拦截器获取当前管理员角色
        String role = (String) request.getAttribute("role");

        // 权限校验在 Service 层完成
        return userService.searchUsersByNicknamePaginated(keyword, role, userRole, page, size);
    }
}