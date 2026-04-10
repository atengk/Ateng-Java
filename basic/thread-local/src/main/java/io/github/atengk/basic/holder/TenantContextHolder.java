package io.github.atengk.basic.holder;

import cn.hutool.core.util.StrUtil;

/**
 * 租户上下文工具类（基于 ThreadLocal 实现）
 *
 * @author Ateng
 * @since 2026-04-10
 */
public class TenantContextHolder {

    /**
     * 存储租户ID
     */
    private static final ThreadLocal<String> TENANT_THREAD_LOCAL = new ThreadLocal<>();

    /**
     * 默认租户
     */
    private static final String DEFAULT_TENANT = "default";

    /**
     * 设置租户ID
     *
     * @param tenantId 租户ID
     */
    public static void set(String tenantId) {
        if (StrUtil.isNotBlank(tenantId)) {
            TENANT_THREAD_LOCAL.set(tenantId);
        }
    }

    /**
     * 获取租户ID
     *
     * @return tenantId
     */
    public static String get() {
        String tenantId = TENANT_THREAD_LOCAL.get();
        return StrUtil.isNotBlank(tenantId) ? tenantId : DEFAULT_TENANT;
    }

    /**
     * 清理
     */
    public static void clear() {
        TENANT_THREAD_LOCAL.remove();
    }
}