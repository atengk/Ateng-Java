package io.github.atengk.utils.thread;

import java.time.Duration;

/**
 * 虚拟线程任务统计信息。
 *
 * @author Ateng
 * @since 2026-04-30
 */
public final class VirtualTaskStats {

    private final int totalCount;
    private final int successCount;
    private final int failureCount;
    private final int timeoutCount;
    private final int cancelledCount;
    private final Duration totalDuration;

    /**
     * 创建任务统计信息。
     *
     * @param totalCount     总数量
     * @param successCount   成功数量
     * @param failureCount   失败数量
     * @param timeoutCount   超时数量
     * @param cancelledCount 取消数量
     * @param totalDuration  总耗时
     */
    public VirtualTaskStats(int totalCount, int successCount, int failureCount, int timeoutCount, int cancelledCount, Duration totalDuration) {
        this.totalCount = Math.max(totalCount, 0);
        this.successCount = Math.max(successCount, 0);
        this.failureCount = Math.max(failureCount, 0);
        this.timeoutCount = Math.max(timeoutCount, 0);
        this.cancelledCount = Math.max(cancelledCount, 0);
        this.totalDuration = totalDuration == null ? Duration.ZERO : totalDuration;
    }

    /**
     * 获取任务总数量。
     *
     * @return 任务总数量
     */
    public int getTotalCount() {
        return totalCount;
    }

    /**
     * 获取成功数量。
     *
     * @return 成功数量
     */
    public int getSuccessCount() {
        return successCount;
    }

    /**
     * 获取失败数量。
     *
     * @return 失败数量
     */
    public int getFailureCount() {
        return failureCount;
    }

    /**
     * 获取超时数量。
     *
     * @return 超时数量
     */
    public int getTimeoutCount() {
        return timeoutCount;
    }

    /**
     * 获取取消数量。
     *
     * @return 取消数量
     */
    public int getCancelledCount() {
        return cancelledCount;
    }

    /**
     * 获取总耗时。
     *
     * @return 总耗时
     */
    public Duration getTotalDuration() {
        return totalDuration;
    }
}
