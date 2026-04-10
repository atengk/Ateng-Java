package io.github.atengk.basic.holder;

import cn.hutool.core.collection.CollUtil;

import java.util.Set;

/**
 * 权限上下文工具类（基于 ThreadLocal 实现）
 *
 * @author Ateng
 * @since 2026-04-10
 */
public class PermissionContextHolder {

    /**
     * 存储当前用户权限集合
     */
    private static final ThreadLocal<Set<String>> PERMISSION_THREAD_LOCAL = new ThreadLocal<>();

    /**
     * 设置权限集合
     *
     * @param permissions 权限集合
     */
    public static void set(Set<String> permissions) {
        if (CollUtil.isNotEmpty(permissions)) {
            PERMISSION_THREAD_LOCAL.set(permissions);
        }
    }

    /**
     * 获取权限集合
     *
     * @return 权限集合
     */
    public static Set<String> get() {
        return PERMISSION_THREAD_LOCAL.get();
    }

    /**
     * 判断是否拥有某权限
     *
     * @param permission 权限码
     * @return 是否拥有
     */
    public static boolean hasPermission(String permission) {
        Set<String> permissions = get();
        return CollUtil.isNotEmpty(permissions) && permissions.contains(permission);
    }

    /**
     * 清理
     */
    public static void clear() {
        PERMISSION_THREAD_LOCAL.remove();
    }
}
