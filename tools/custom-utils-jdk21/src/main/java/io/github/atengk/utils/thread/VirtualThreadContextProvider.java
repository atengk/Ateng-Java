package io.github.atengk.utils.thread;

/**
 * 虚拟线程上下文提供者。
 *
 * @author Ateng
 * @since 2026-04-30
 */
public interface VirtualThreadContextProvider {

    /**
     * 捕获当前线程上下文。
     *
     * @return 上下文快照
     */
    Object capture();

    /**
     * 将上下文快照恢复到当前线程。
     *
     * @param context 上下文快照
     */
    void restore(Object context);

    /**
     * 清理当前线程上下文。
     */
    void clear();
}
