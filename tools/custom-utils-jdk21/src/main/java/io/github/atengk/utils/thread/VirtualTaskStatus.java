package io.github.atengk.utils.thread;

/**
 * 虚拟线程任务状态。
 *
 * @author Ateng
 * @since 2026-04-30
 */
public enum VirtualTaskStatus {

    /**
     * 任务执行成功。
     */
    SUCCESS,

    /**
     * 任务执行失败。
     */
    FAILURE,

    /**
     * 任务执行超时。
     */
    TIMEOUT,

    /**
     * 任务被取消。
     */
    CANCELLED
}
