package io.github.atengk.service.strategy.impl;

import io.github.atengk.enums.LogTypeEnum;
import io.github.atengk.service.strategy.LogStrategy;
import org.springframework.stereotype.Component;

/**
 * 数据库日志实现类。
 *
 * <p>说明：
 * 模拟将日志写入数据库（实际业务可使用 MyBatis、JPA 等）。
 * </p>
 */
@Component("databaseLogStrategy")
public class DatabaseLogStrategy implements LogStrategy {

    @Override
    public void log(String message) {
        System.out.println("[DatabaseLog] 插入日志到数据库表：" + message);
    }

    @Override
    public LogTypeEnum getType() {
        return LogTypeEnum.DB;
    }
}
