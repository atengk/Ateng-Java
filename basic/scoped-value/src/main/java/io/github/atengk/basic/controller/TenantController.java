package io.github.atengk.basic.controller;

import io.github.atengk.basic.service.TenantUserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 多租户控制器示例
 *
 * @author Ateng
 * @since 2026-04-10
 */
@RestController
public class TenantController {

    private final TenantUserService tenantUserService;

    public TenantController(TenantUserService tenantUserService) {
        this.tenantUserService = tenantUserService;
    }

    /**
     * 租户查询测试接口
     * <p>
     * curl -H "X-TENANT-ID: t001" -H "X-USER-ID: u100" -H "X-TRACE-ID: abc123" http://localhost:10005/tenant/query
     *
     * @return 结果
     */
    @GetMapping("/tenant/query")
    public String query() {
        tenantUserService.queryUser();
        return "ok";
    }
}