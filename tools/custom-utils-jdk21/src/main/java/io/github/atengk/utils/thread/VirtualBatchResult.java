package io.github.atengk.utils.thread;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 虚拟线程批量任务执行结果。
 *
 * @param <T> 结果类型
 * @author Ateng
 * @since 2026-04-30
 */
public final class VirtualBatchResult<T> {

    private final List<VirtualTaskResult<T>> results;
    private final Duration duration;

    /**
     * 创建批量任务执行结果。
     *
     * @param results  任务结果集合
     * @param duration 执行总耗时
     */
    public VirtualBatchResult(List<VirtualTaskResult<T>> results, Duration duration) {
        Objects.requireNonNull(results, "results must not be null");
        this.results = Collections.unmodifiableList(new ArrayList<>(results));
        this.duration = duration == null ? Duration.ZERO : duration;
    }

    /**
     * 获取全部任务结果。
     *
     * @return 全部任务结果
     */
    public List<VirtualTaskResult<T>> getResults() {
        return results;
    }

    /**
     * 获取成功任务结果。
     *
     * @return 成功任务结果
     */
    public List<VirtualTaskResult<T>> getSuccessResults() {
        return results.stream().filter(VirtualTaskResult::isSuccess).collect(Collectors.toUnmodifiableList());
    }

    /**
     * 获取失败任务结果。
     *
     * @return 失败任务结果
     */
    public List<VirtualTaskResult<T>> getFailureResults() {
        return results.stream().filter(VirtualTaskResult::isFailure).collect(Collectors.toUnmodifiableList());
    }

    /**
     * 获取成功返回值集合。
     *
     * @return 成功返回值集合
     */
    public List<T> getSuccessValues() {
        return results.stream().filter(VirtualTaskResult::isSuccess).map(VirtualTaskResult::getValue).collect(Collectors.toUnmodifiableList());
    }

    /**
     * 获取执行总耗时。
     *
     * @return 执行总耗时
     */
    public Duration getDuration() {
        return duration;
    }

    /**
     * 获取任务统计信息。
     *
     * @return 任务统计信息
     */
    public VirtualTaskStats stats() {
        int success = (int) results.stream().filter(VirtualTaskResult::isSuccess).count();
        int failure = (int) results.stream().filter(VirtualTaskResult::isFailure).count();
        int timeout = (int) results.stream().filter(VirtualTaskResult::isTimeout).count();
        int cancelled = (int) results.stream().filter(VirtualTaskResult::isCancelled).count();
        return new VirtualTaskStats(results.size(), success, failure, timeout, cancelled, duration);
    }

    /**
     * 判断是否全部成功。
     *
     * @return 是否全部成功
     */
    public boolean isAllSuccess() {
        return !results.isEmpty() && results.stream().allMatch(VirtualTaskResult::isSuccess);
    }

    /**
     * 判断是否存在失败任务。
     *
     * @return 是否存在失败任务
     */
    public boolean hasFailure() {
        return results.stream().anyMatch(result -> !result.isSuccess());
    }
}
