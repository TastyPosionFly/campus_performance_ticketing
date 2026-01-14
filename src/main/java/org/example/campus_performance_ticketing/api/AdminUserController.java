package org.example.campus_performance_ticketing.api;

import org.example.campus_performance_ticketing.logic.UserService;
import org.example.campus_performance_ticketing.logic.dto.ApiResponse;
import org.example.campus_performance_ticketing.model.UserInfo;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

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
            @RequestParam boolean ban
    ) {
        // 从拦截器获取当前管理员信息
        String role = (String) request.getAttribute("role");

        if (!"ADMIN".equalsIgnoreCase(role) && !"SUPER_ADMIN".equalsIgnoreCase(role)) {
            return ApiResponse.fail("没有权限操作");
        }

        return userService.banOrUnbanUser(openId, ban, role);
    }

    /**
     * 查询用户列表（ADMIN, SUPER_ADMIN）
     */
    @GetMapping("/list")
    public ApiResponse<Iterable<UserInfo>> listUsers(
            HttpServletRequest request
            ) {
        // 从拦截器获取当前管理员信息
        String role = (String) request.getAttribute("role");

        if (!"ADMIN".equalsIgnoreCase(role) && !"SUPER_ADMIN".equalsIgnoreCase(role)) {
            return ApiResponse.fail("没有权限操作");
        }

        return userService.getAllUsers();
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
        // 从拦截器获取当前管理员信息
        String role = (String) request.getAttribute("role");

        if (!"SUPER_ADMIN".equalsIgnoreCase(role)) {
            return ApiResponse.fail("没有权限操作");
        }

        return userService.updateUserRole(openId, newRole, role);
    }

}
