package io.github.atengk.basic.service;

import io.github.atengk.basic.holder.TransactionContextHolder;
import org.springframework.stereotype.Service;

import java.sql.Connection;

/**
 * 示例业务类（手动事务）
 *
 * @author Ateng
 * @since 2026-04-10
 */
@Service
public class AccountService {

    /**
     * 模拟转账操作
     */
    public void transfer() throws Exception {

        // 从 ThreadLocal 获取连接
        Connection connection = TransactionContextHolder.get();

        if (connection == null) {
            throw new RuntimeException("当前无事务连接");
        }

        // 模拟 SQL 执行
        System.out.println("执行扣款操作...");
        System.out.println("执行加款操作...");

        // 模拟异常（测试回滚）
        if (true) {
            throw new RuntimeException("模拟异常");
        }
    }
}
