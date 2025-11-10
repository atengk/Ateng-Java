package io.github.atengk.controller;

import io.github.atengk.service.interpreter.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 解释器模式控制器演示
 */
@RestController
public class InterpreterController {

    /**
     * 解释权限表达式
     *
     * @param userRoles 用户角色字符串，例如："USER VIP"
     * @return 是否通过表达式
     */
    @GetMapping("/interpreter/check")
    public String checkPermission(@RequestParam String userRoles) {

        // 构建表达式：ADMIN OR (USER AND VIP)
        Expression admin = new TerminalExpression("ADMIN");
        Expression user = new TerminalExpression("USER");
        Expression vip = new TerminalExpression("VIP");
        Expression userAndVip = new AndExpression(user, vip);
        Expression expression = new OrExpression(admin, userAndVip);

        boolean result = expression.interpret(userRoles);
        return "用户角色 [" + userRoles + "] 权限检查结果: " + result;
    }
}
