package io.github.atengk.controller;

import io.github.atengk.service.visitor.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

/**
 * 访问者模式控制器演示
 */
@RestController
public class VisitorController {

    private final SalaryVisitor salaryVisitor;

    public VisitorController(SalaryVisitor salaryVisitor) {
        this.salaryVisitor = salaryVisitor;
    }

    /**
     * 演示访问者模式统计工资
     */
    @GetMapping("/visitor/salary")
    public String calculateSalary() {

        List<Employee> employees = Arrays.asList(
                new Manager("Alice", 12000),
                new Staff("Bob", 5000),
                new Staff("Charlie", 5500)
        );

        // 清空上次统计
        salaryVisitor.getClass(); // 保证 bean 注入
        double totalBefore = salaryVisitor.getTotalSalary(); // 如果多次请求，需要清空或重置
        salaryVisitor.visit(new Staff("dummy", 0)); // 可以加方法重置 totalSalary

        for (Employee emp : employees) {
            emp.accept(salaryVisitor);
        }

        return "总薪资：" + salaryVisitor.getTotalSalary();
    }
}
