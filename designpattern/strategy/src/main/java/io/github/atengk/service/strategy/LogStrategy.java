package io.github.atengk.service.strategy;

import io.github.atengk.enums.LogTypeEnum;

/**
 * 日志策略接口。
 *
 * <p>说明：
 * 定义统一的日志输出接口，不同类型的日志策略（文件、数据库、MQ 等）实现此接口。
 * 这样调用方无需关心日志的实现细节，只需要选择对应类型即可。
 * </p>
 */
public interface LogStrategy {

    /**
     * 执行日志记录操作。
     *
     * @param message 日志消息内容
     */
    void log(String message);

    /**
     * 获取策略的名称，用于工厂识别。
     *
     * @return 策略标识字符串
     */
    LogTypeEnum getType();
}

