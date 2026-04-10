package io.github.atengk.basic.service;

import io.github.atengk.basic.holder.TenantContextHolder;
import org.springframework.stereotype.Service;

/**
 * 示例业务类（多租户）
 *
 * @author Ateng
 * @since 2026-04-10
 */
@Service
public class TenantService {

    /**
     * 查询数据（根据租户隔离）
     */
    public String queryData() {
        String tenantId = TenantContextHolder.get();
        return "当前租户：" + tenantId + "，返回对应数据";
    }
}