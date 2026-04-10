package io.github.atengk.basic.controller;

import io.github.atengk.basic.service.LogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 日志上下文测试控制器
 *
 * @author Ateng
 * @since 2026-04-10
 */
@RestController
public class LogController {

    private final LogService logService;

    public LogController(LogService logService) {
        this.logService = logService;
    }

    /**
     * 测试日志
     *
     * http://localhost:8080/test/log
     */
    @GetMapping("/test/log")
    public String test() {
        return logService.process();
    }
}
