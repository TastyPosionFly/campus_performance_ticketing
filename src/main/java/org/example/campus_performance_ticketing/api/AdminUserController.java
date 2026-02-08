package org.example.campus_performance_ticketing.api;

import org.example.campus_performance_ticketing.logic.UserService;
import org.example.campus_performance_ticketing.logic.dto.ApiResponse;
import org.example.campus_performance_ticketing.model.UserInfo;
import org.springframework.web.bind.annotation.*;

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
    public ApiResponse<Iterable<UserInfo>> listUsers(HttpServletRequest request) {
        // 从拦截器获取当前管理员角色
        String role = (String) request.getAttribute("role");

        // 权限校验在 Service 层完成
        return userService.getAllUsers(role);
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
    public ApiResponse<List<UserInfo>> searchUsers(
            HttpServletRequest request,
            @RequestParam String keyword
    ) {
        // 从拦截器获取当前管理员角色
        String role = (String) request.getAttribute("role");

        // 权限校验在 Service 层完成
        return userService.searchUsersByNickname(keyword, role);
    }
}