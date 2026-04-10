package io.github.atengk.basic.manager;

import cn.hutool.core.exceptions.ExceptionUtil;
import io.github.atengk.basic.holder.TransactionContextHolder;

import javax.sql.DataSource;
import java.sql.Connection;


/**
 * 手动事务管理工具
 *
 * @author Ateng
 * @since 2026-04-10
 */
public class TransactionManager {

    private final DataSource dataSource;

    public TransactionManager(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * 开启事务
     */
    public void begin() {
        try {
            Connection connection = dataSource.getConnection();
            connection.setAutoCommit(false);
            TransactionContextHolder.bind(connection);
        } catch (Exception e) {
            throw new RuntimeException("开启事务失败：" + ExceptionUtil.getMessage(e), e);
        }
    }

    /**
     * 提交事务
     */
    public void commit() {
        Connection connection = TransactionContextHolder.get();
        if (connection == null) {
            return;
        }
        try {
            connection.commit();
        } catch (Exception e) {
            throw new RuntimeException("提交事务失败：" + ExceptionUtil.getMessage(e), e);
        } finally {
            close(connection);
        }
    }

    /**
     * 回滚事务
     */
    public void rollback() {
        Connection connection = TransactionContextHolder.get();
        if (connection == null) {
            return;
        }
        try {
            connection.rollback();
        } catch (Exception e) {
            throw new RuntimeException("回滚事务失败：" + ExceptionUtil.getMessage(e), e);
        } finally {
            close(connection);
        }
    }

    /**
     * 关闭连接并清理
     */
    private void close(Connection connection) {
        try {
            connection.close();
        } catch (Exception ignored) {
        }
        TransactionContextHolder.clear();
    }
}
