package io.github.atengk.utils.thread;

import java.time.Duration;
import java.util.Objects;

/**
 * 虚拟线程任务失败信息。
 *
 * @author Ateng
 * @since 2026-04-30
 */
public final class VirtualTaskError {

    private final int index;
    private final String message;
    private final Throwable throwable;
    private final Duration duration;

    private VirtualTaskError(int index, String message, Throwable throwable, Duration duration) {
        this.index = index;
        this.message = message;
        this.throwable = throwable;
        this.duration = duration;
    }

    /**
     * 创建任务失败信息。
     *
     * @param index     任务下标
     * @param throwable 异常对象
     * @param duration  执行耗时
     * @return 任务失败信息
     */
    public static VirtualTaskError of(int index, Throwable throwable, Duration duration) {
        Throwable actual = Objects.requireNonNull(throwable, "throwable must not be null");
        return new VirtualTaskError(index, actual.getMessage(), actual, duration == null ? Duration.ZERO : duration);
    }

    /**
     * 获取任务下标。
     *
     * @return 任务下标
     */
    public int getIndex() {
        return index;
    }

    /**
     * 获取异常消息。
     *
     * @return 异常消息
     */
    public String getMessage() {
        return message;
    }

    /**
     * 获取异常对象。
     *
     * @return 异常对象
     */
    public Throwable getThrowable() {
        return throwable;
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
