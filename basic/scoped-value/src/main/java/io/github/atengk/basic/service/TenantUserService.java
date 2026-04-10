package io.github.atengk.basic.service;

import io.github.atengk.basic.context.RequestContext;
import io.github.atengk.basic.context.TenantContext;
import io.github.atengk.basic.util.LogUtil;
import org.springframework.stereotype.Service;

/**
 * 租户业务示例
 * <p>
 * 演示 tenantId 在业务链路中的自动隔离能力
 *
 * @author Ateng
 * @since 2026-04-10
 */
@Service
public class TenantUserService {

    /**
     * 模拟查询用户数据（按租户隔离）
     */
    public void queryUser() {

        String tenantId = TenantContext.getTenantId();
        String userId = RequestContext.USER_ID.get();

        LogUtil.info("查询用户数据，tenantId=" + tenantId + ", userId=" + userId);

        // 模拟 SQL：select * from user where tenant_id = ?
    }
}
