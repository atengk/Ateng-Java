package io.github.atengk.basic.controller;

import io.github.atengk.basic.service.TenantService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 多租户测试控制器
 *
 * @author Ateng
 * @since 2026-04-10
 */
@RestController
public class TenantController {

    private final TenantService tenantService;

    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    /**
     * 测试接口
     *
     * curl 示例：
     * curl http://localhost:8080/test/tenant
     * curl -H "X-Tenant-Id: tenantA" http://localhost:8080/test/tenant
     *
     * @return 租户数据
     */
    @GetMapping("/test/tenant")
    public String test() {
        return tenantService.queryData();
    }
}
