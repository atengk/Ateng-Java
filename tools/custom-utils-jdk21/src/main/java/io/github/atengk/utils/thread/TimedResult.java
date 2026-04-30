package io.github.atengk.utils.thread;

import java.time.Duration;

/**
 * 带耗时的任务结果。
 *
 * @param <T> 结果类型
 * @author Ateng
 * @since 2026-04-30
 */
public final class TimedResult<T> {

    private final T value;
    private final Duration duration;

    /**
     * 创建带耗时的任务结果。
     *
     * @param value    返回值
     * @param duration 执行耗时
     */
    public TimedResult(T value, Duration duration) {
        this.value = value;
        this.duration = duration == null ? Duration.ZERO : duration;
    }

    /**
     * 获取返回值。
     *
     * @return 返回值
     */
    public T getValue() {
        return value;
    }

    /**
     * 获取执行耗时。
     *
     * @return 执行耗时
     */
    public Duration getDuration() {
        return duration;
    }
}
