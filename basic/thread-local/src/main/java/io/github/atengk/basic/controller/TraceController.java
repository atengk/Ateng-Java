package io.github.atengk.basic.controller;

import io.github.atengk.basic.service.TraceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * TraceId 测试控制器
 *
 * @author Ateng
 * @since 2026-04-10
 */
@RestController
public class TraceController {

    private final TraceService traceService;

    public TraceController(TraceService traceService) {
        this.traceService = traceService;
    }

    /**
     * 测试接口
     *
     * 1. 不传请求头：自动生成 TraceId
     * 2. 传请求头 X-Trace-Id：使用传入值
     *
     * curl 示例：
     * curl http://localhost:8080/test/trace
     * curl -H "X-Trace-Id: abc123" http://localhost:8080/test/trace
     *
     * @return TraceId
     */
    @GetMapping("/test/trace")
    public String testTrace() {
        return "TraceId：" + traceService.process();
    }
}
