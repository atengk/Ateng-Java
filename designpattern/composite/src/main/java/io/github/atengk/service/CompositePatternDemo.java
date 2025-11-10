package io.github.atengk.service;


import io.github.atengk.composite.Department;
import io.github.atengk.leaf.Employee;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

/**
 * 组合模式示例演示类
 */
@Component
public class CompositePatternDemo {

    @PostConstruct
    public void runDemo() {
        System.out.println("=== 组合模式（Composite Pattern）示例 ===");

        // 顶层部门
        Department root = new Department("公司总部");

        // 一级部门
        Department techDept = new Department("技术部");
        Department hrDept = new Department("人事部");

        // 二级部门
        Department backendTeam = new Department("后端组");
        Department frontendTeam = new Department("前端组");

        // 添加员工
        backendTeam.add(new Employee("张三"));
        backendTeam.add(new Employee("李四"));
        frontendTeam.add(new Employee("王五"));
        hrDept.add(new Employee("赵六"));

        // 组装结构
        techDept.add(backendTeam);
        techDept.add(frontendTeam);
        root.add(techDept);
        root.add(hrDept);

        // 显示组织结构
        root.show(0);
    }
}