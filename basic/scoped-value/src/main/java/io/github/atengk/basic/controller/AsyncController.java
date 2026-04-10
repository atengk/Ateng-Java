package io.github.atengk.basic.controller;

import io.github.atengk.basic.service.AsyncUserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 异步任务控制器
 *
 * @author Ateng
 * @since 2026-04-10
 */
@RestController
public class AsyncController {

    private final AsyncUserService asyncUserService;

    public AsyncController(AsyncUserService asyncUserService) {
        this.asyncUserService = asyncUserService;
    }

    /**
     * 异步任务测试接口
     *
     * curl -H "X-USER-ID: u100" -H "X-TENANT-ID: t001" -H "X-TRACE-ID: abc123" http://localhost:10005/async/test
     *
     * @return 结果
     */
    @GetMapping("/async/test")
    public String test() {
        asyncUserService.processAsync();
        return "ok";
    }
}
