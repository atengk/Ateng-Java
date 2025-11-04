package io.github.atengk.controller;

import io.github.atengk.enums.LogTypeEnum;
import io.github.atengk.service.factory.LogStrategyFactory;
import io.github.atengk.service.strategy.LogStrategy;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 工厂模式示例控制器。
 *
 * <p>说明：
 * 本控制器用于演示如何在业务中调用工厂模式。
 * 通过接口参数选择不同的日志类型，动态获取对应策略 Bean 执行日志操作。
 * </p>
 */
@RestController
@RequestMapping("/designpattern/factory")
public class FactoryDemoController {

    private final LogStrategyFactory logStrategyFactory;

    /**
     * 构造注入日志工厂。
     *
     * @param logStrategyFactory 工厂实例
     */
    public FactoryDemoController(LogStrategyFactory logStrategyFactory) {
        this.logStrategyFactory = logStrategyFactory;
    }

    /**
     * 通过日志类型参数调用对应策略。
     *
     * 示例：
     * <ul>
     *   <li>GET /designpattern/factory/log?type=FILE&msg=系统启动</li>
     *   <li>GET /designpattern/factory/log?type=DB&msg=用户登录</li>
     * </ul>
     *
     * @param logType 日志类型（FILE / DB）
     * @param msg  日志内容
     * @return 执行结果
     */
    @GetMapping("/log")
    public Map<String, Object> log(@RequestParam("logType") LogTypeEnum logType, @RequestParam("msg") String msg) {
        Map<String, Object> result = new HashMap<>(4);
        try {
            LogStrategy strategy = logStrategyFactory.getStrategy(logType);
            strategy.log(msg);
            result.put("success", true);
            result.put("type", logType.name());
            result.put("message", "日志记录成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        return result;
    }
}

