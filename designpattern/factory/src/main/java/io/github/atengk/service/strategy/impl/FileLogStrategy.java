package io.github.atengk.service.strategy.impl;

import io.github.atengk.enums.LogTypeEnum;
import io.github.atengk.service.strategy.LogStrategy;
import org.springframework.stereotype.Component;

/**
 * 文件日志实现类。
 *
 * <p>说明：
 * 模拟将日志输出到文件（实际业务中可写入磁盘或对象存储）。
 * 使用 {@code @Component} 注册为 Spring Bean，便于工厂统一管理。
 * </p>
 */
@Component("fileLogStrategy")
public class FileLogStrategy implements LogStrategy {

    @Override
    public void log(String message) {
        System.out.println("[FileLog] 保存日志到文件系统：" + message);
    }

    @Override
    public LogTypeEnum getType() {
        return LogTypeEnum.FILE;
    }
}
