package io.github.atengk.service.factory;

import io.github.atengk.enums.LogTypeEnum;
import io.github.atengk.service.strategy.LogStrategy;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;

/**
 * 日志策略工厂类。
 *
 * <p>说明：
 * 本工厂类结合 Spring 的依赖注入功能，将所有实现 {@link LogStrategy} 接口的 Bean 自动注入到一个 Map 中。
 * Key 为 Bean 名称或类型标识，Value 为具体实现类。
 * 这样可以避免使用 if-else 或 switch 来判断类型，从而使工厂具备可扩展性。
 * </p>
 */
@Component
public class LogStrategyFactory {

    /**
     * Spring 自动注入所有 LogStrategy Bean，形成策略映射表。
     * key：Bean 名称
     * value：对应的策略实现类实例
     */
    private final Map<String, LogStrategy> strategyMap;

    /**
     * 构造方法注入策略映射。
     *
     * @param strategyMap 所有 LogStrategy Bean 的映射
     */
    public LogStrategyFactory(Map<String, LogStrategy> strategyMap) {
        this.strategyMap = strategyMap;
    }

    /**
     * 根据枚举类型获取对应策略实例
     *
     * @param type 日志类型枚举
     * @return 对应的日志策略实现类
     * @throws IllegalArgumentException 当未匹配到策略时抛出
     */
    public LogStrategy getStrategy(LogTypeEnum type) {
        if (type == null) {
            throw new IllegalArgumentException("日志类型不能为空");
        }
        LogStrategy strategy = strategyMap.get(type.getCode());
        if (strategy == null) {
            throw new IllegalArgumentException("未找到对应策略：" + type.getCode());
        }
        return strategy;
    }

    /**
     * 获取所有策略 Bean（只读）
     *
     * @return Map<String, NoticeStrategy> 不可修改视图
     */
    public Map<String, LogStrategy> getAllStrategies() {
        return Collections.unmodifiableMap(strategyMap);
    }

}

