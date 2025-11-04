package io.github.atengk.service.context;

import io.github.atengk.enums.LogTypeEnum;
import io.github.atengk.service.factory.LogStrategyFactory;
import io.github.atengk.service.strategy.LogStrategy;
import org.springframework.stereotype.Component;

/**
 * 策略上下文类
 * 封装策略选择逻辑，简化外部调用
 */
@Component
public class LogStrategyContext {

    private final LogStrategyFactory logStrategyFactory;

    public LogStrategyContext(LogStrategyFactory logStrategyFactory) {
        this.logStrategyFactory = logStrategyFactory;
    }

    /**
     * 执行日志记录操作
     *
     * @param type    日志类型枚举
     * @param message 日志消息内容
     */
    public void executeLog(LogTypeEnum type, String message) {
        LogStrategy strategy = logStrategyFactory.getStrategy(type);
        strategy.log(message);
    }

}
