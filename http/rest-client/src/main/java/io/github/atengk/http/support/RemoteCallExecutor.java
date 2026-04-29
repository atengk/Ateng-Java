package io.github.atengk.http.support;

import java.util.function.Supplier;

/**
 * 远程调用执行器
 *
 * @author Ateng
 * @since 2026-04-29
 */
public interface RemoteCallExecutor {

    /**
     * 执行远程调用
     *
     * @param clientName 客户端名称
     * @param supplier   调用逻辑
     * @param <T>        返回类型
     * @return 调用结果
     */
    <T> T execute(String clientName, Supplier<T> supplier);

    /**
     * 执行无返回值远程调用
     *
     * @param clientName 客户端名称
     * @param runnable   调用逻辑
     */
    default void execute(String clientName, Runnable runnable) {
        execute(clientName, () -> {
            runnable.run();
            return null;
        });
    }

}