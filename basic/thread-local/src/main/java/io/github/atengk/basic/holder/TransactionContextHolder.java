package io.github.atengk.basic.holder;

import java.sql.Connection;

/**
 * 事务上下文工具类（基于 ThreadLocal 实现）
 *
 * @author Ateng
 * @since 2026-04-10
 */
public class TransactionContextHolder {

    /**
     * 存储当前线程的数据库连接
     */
    private static final ThreadLocal<Connection> CONNECTION_THREAD_LOCAL = new ThreadLocal<>();

    /**
     * 绑定连接
     *
     * @param connection 数据库连接
     */
    public static void bind(Connection connection) {
        if (connection != null) {
            CONNECTION_THREAD_LOCAL.set(connection);
        }
    }

    /**
     * 获取当前连接
     *
     * @return Connection
     */
    public static Connection get() {
        return CONNECTION_THREAD_LOCAL.get();
    }

    /**
     * 移除连接
     */
    public static void clear() {
        CONNECTION_THREAD_LOCAL.remove();
    }
}