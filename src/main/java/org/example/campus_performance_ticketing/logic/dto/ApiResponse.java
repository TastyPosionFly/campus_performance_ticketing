package org.example.campus_performance_ticketing.logic.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一 API 响应结构
 * @param <T> data 类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

    /** 是否成功 */
    private boolean success;

    /** 返回数据 */
    private T data;

    /** 提示信息 */
    private String message;

    /** 可选扩展字段，比如 token */
    private String token;

    /** 快速构造成功返回 */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null, null);
    }

    /** 成功并带 token（登录接口用） */
    public static <T> ApiResponse<T> successWithToken(T data, String token) {
        return new ApiResponse<>(true, data, null, token);
    }

    /** 失败返回 */
    public static <T> ApiResponse<T> fail(String message) {
        return new ApiResponse<>(false, null, message, null);
    }
}
