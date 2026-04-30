package io.github.atengk.utils.thread;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/**
 * 虚拟线程任务执行结果。
 *
 * @param <T> 结果类型
 * @author Ateng
 * @since 2026-04-30
 */
public final class VirtualTaskResult<T> {

    private final int index;
    private final VirtualTaskStatus status;
    private final T value;
    private final Throwable throwable;
    private final Duration duration;

    private VirtualTaskResult(int index, VirtualTaskStatus status, T value, Throwable throwable, Duration duration) {
        this.index = index;
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.value = value;
        this.throwable = throwable;
        this.duration = duration == null ? Duration.ZERO : duration;
    }

    /**
     * 创建成功结果。
     *
     * @param index    任务下标
     * @param value    返回值
     * @param duration 执行耗时
     * @param <T>      结果类型
     * @return 成功结果
     */
    public static <T> VirtualTaskResult<T> success(int index, T value, Duration duration) {
        return new VirtualTaskResult<>(index, VirtualTaskStatus.SUCCESS, value, null, duration);
    }

    /**
     * 创建失败结果。
     *
     * @param index     任务下标
     * @param throwable 异常对象
     * @param duration  执行耗时
     * @param <T>       结果类型
     * @return 失败结果
     */
    public static <T> VirtualTaskResult<T> failure(int index, Throwable throwable, Duration duration) {
        return new VirtualTaskResult<>(index, VirtualTaskStatus.FAILURE, null, Objects.requireNonNull(throwable, "throwable must not be null"), duration);
    }

    /**
     * 创建超时结果。
     *
     * @param index     任务下标
     * @param throwable 异常对象
     * @param duration  执行耗时
     * @param <T>       结果类型
     * @return 超时结果
     */
    public static <T> VirtualTaskResult<T> timeout(int index, Throwable throwable, Duration duration) {
        return new VirtualTaskResult<>(index, VirtualTaskStatus.TIMEOUT, null, Objects.requireNonNull(throwable, "throwable must not be null"), duration);
    }

    /**
     * 创建取消结果。
     *
     * @param index     任务下标
     * @param throwable 异常对象
     * @param duration  执行耗时
     * @param <T>       结果类型
     * @return 取消结果
     */
    public static <T> VirtualTaskResult<T> cancelled(int index, Throwable throwable, Duration duration) {
        return new VirtualTaskResult<>(index, VirtualTaskStatus.CANCELLED, null, throwable, duration);
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
     * 获取任务状态。
     *
     * @return 任务状态
     */
    public VirtualTaskStatus getStatus() {
        return status;
    }

    /**
     * 获取任务返回值。
     *
     * @return 任务返回值
     */
    public T getValue() {
        return value;
    }

    /**
     * 获取任务返回值可选对象。
     *
     * @return 任务返回值可选对象
     */
    public Optional<T> valueOptional() {
        return Optional.ofNullable(value);
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
     * 获取异常对象可选对象。
     *
     * @return 异常对象可选对象
     */
    public Optional<Throwable> throwableOptional() {
        return Optional.ofNullable(throwable);
    }

    /**
     * 获取任务耗时。
     *
     * @return 任务耗时
     */
    public Duration getDuration() {
        return duration;
    }

    /**
     * 判断任务是否成功。
     *
     * @return 是否成功
     */
    public boolean isSuccess() {
        return status == VirtualTaskStatus.SUCCESS;
    }

    /**
     * 判断任务是否失败。
     *
     * @return 是否失败
     */
    public boolean isFailure() {
        return status == VirtualTaskStatus.FAILURE;
    }

    /**
     * 判断任务是否超时。
     *
     * @return 是否超时
     */
    public boolean isTimeout() {
        return status == VirtualTaskStatus.TIMEOUT;
    }

    /**
     * 判断任务是否取消。
     *
     * @return 是否取消
     */
    public boolean isCancelled() {
        return status == VirtualTaskStatus.CANCELLED;
    }
}
