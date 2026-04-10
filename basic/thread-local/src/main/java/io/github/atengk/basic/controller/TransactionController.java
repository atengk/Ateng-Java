package io.github.atengk.basic.controller;

import io.github.atengk.basic.manager.TransactionManager;
import io.github.atengk.basic.service.AccountService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;

/**
 * 事务测试控制器
 *
 * @author Ateng
 * @since 2026-04-10
 */
@RestController
public class TransactionController {

    private final DataSource dataSource;

    private final AccountService accountService;

    public TransactionController(DataSource dataSource, AccountService accountService) {
        this.dataSource = dataSource;
        this.accountService = accountService;
    }

    /**
     * 测试事务提交
     * <p>
     * http://localhost:8080/test/tx/commit
     */
    @GetMapping("/test/tx/commit")
    public String commit() {

        TransactionManager txManager = new TransactionManager(dataSource);

        try {
            txManager.begin();
            // 这里不抛异常，模拟成功
            System.out.println("执行正常逻辑...");
            txManager.commit();
            return "事务提交成功";
        } catch (Exception e) {
            txManager.rollback();
            return "事务回滚：" + e.getMessage();
        }
    }

    /**
     * 测试事务回滚
     * <p>
     * http://localhost:8080/test/tx/rollback
     */
    @GetMapping("/test/tx/rollback")
    public String rollback() {

        TransactionManager txManager = new TransactionManager(dataSource);

        try {
            txManager.begin();
            accountService.transfer(); // 内部抛异常
            txManager.commit();
            return "事务提交成功";
        } catch (Exception e) {
            txManager.rollback();
            return "事务回滚：" + e.getMessage();
        }
    }
}
