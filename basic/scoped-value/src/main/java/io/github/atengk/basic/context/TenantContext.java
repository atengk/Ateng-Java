package io.github.atengk.basic.context;

/**
 * 多租户上下文
 *
 * 使用 ScopedValue 实现 tenantId 线程隔离与链路透传
 *
 * @author Ateng
 * @since 2026-04-10
 */
public final class TenantContext {

    /**
     * 租户ID
     */
    public static final ScopedValue<String> TENANT_ID = ScopedValue.newInstance();

    private TenantContext() {
    }

    /**
     * 获取当前租户ID
     *
     * @return tenantId
     */
    public static String getTenantId() {
        return TENANT_ID.isBound() ? TENANT_ID.get() : null;
    }
}
