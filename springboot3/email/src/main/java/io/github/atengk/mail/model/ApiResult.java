package io.github.atengk.mail.model;

import java.time.LocalDateTime;

/**
 * 接口统一响应对象
 *
 * @author Ateng
 * @since 2026-04-30
 */
public record ApiResult<T>(
        Integer code,
        String message,
        T data,
        LocalDateTime timestamp
) {

    /**
     * 返回成功结果
     *
     * @param data 响应数据
     * @return 接口响应
     */
    public static <T> ApiResult<T> success(T data) {
        return new ApiResult<>(200, "操作成功", data, LocalDateTime.now());
    }

    /**
     * 返回失败结果
     *
     * @param message 失败信息
     * @return 接口响应
     */
    public static <T> ApiResult<T> failure(String message) {
        return new ApiResult<>(500, message, null, LocalDateTime.now());
    }
}