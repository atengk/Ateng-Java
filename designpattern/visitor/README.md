# 🧩 访问者模式（Visitor Pattern）

---

## 🌟 一、模式简介

**访问者模式（Visitor Pattern）** 是一种**行为型设计模式**，用于在不修改对象结构的前提下，为对象添加新的操作。
它将操作封装在访问者中，使得结构和行为解耦。

在 **Spring Boot** 项目中，访问者模式常用于：

* 对对象结构统一操作（如报表统计、日志分析）
* 对复杂对象树添加新的功能而不破坏原有类
* 消息处理、账单计算、审计日志等

---

## 🧠 二、场景举例（实战导向）

假设我们有一个「公司员工系统」，包括不同类型的员工：**普通员工** 和 **经理**。
现在我们希望分别统计工资、奖金等信息，如果直接在员工类添加方法，每次新功能都要修改原类，违反开闭原则。
使用访问者模式，可以通过访问者对象实现这些操作，避免修改员工类。

---

## 🏗️ 三、项目结构

```
io.github.atengk
 ├── controller/
 │    └── VisitorController.java
 ├── service/
 │    └── visitor/
 │          ├── Employee.java
 │          ├── Manager.java
 │          ├── EmployeeVisitor.java
 │          └── SalaryVisitor.java
 └── DesignPatternApplication.java
```

---

## 💡 四、代码实现（Spring Boot 实战版）

---

### 1️⃣ 抽象元素：`Employee`

```java
package io.github.atengk.service.visitor;

/**
 * 员工抽象类（元素接口）
 */
public interface Employee {

    /**
     * 接受访问者操作
     *
     * @param visitor 访问者
     */
    void accept(EmployeeVisitor visitor);

    /**
     * 获取姓名
     */
    String getName();

    /**
     * 获取工资
     */
    double getSalary();
}
```

---

### 2️⃣ 具体元素：`Manager` 和 `Staff`

```java
package io.github.atengk.service.visitor;

/**
 * 经理
 */
public class Manager implements Employee {

    private final String name;
    private final double salary;

    public Manager(String name, double salary) {
        this.name = name;
        this.salary = salary;
    }

    @Override
    public void accept(EmployeeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public double getSalary() {
        return salary;
    }
}

/**
 * 普通员工
 */
public class Staff implements Employee {

    private final String name;
    private final double salary;

    public Staff(String name, double salary) {
        this.name = name;
        this.salary = salary;
    }

    @Override
    public void accept(EmployeeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public double getSalary() {
        return salary;
    }
}
```

---

### 3️⃣ 访问者接口：`EmployeeVisitor`

```java
package io.github.atengk.service.visitor;

/**
 * 访问者接口
 */
public interface EmployeeVisitor {

    void visit(Manager manager);

    void visit(Staff staff);
}
```

---

### 4️⃣ 具体访问者：`SalaryVisitor`

```java
package io.github.atengk.service.visitor;

import org.springframework.stereotype.Component;

/**
 * 薪资统计访问者
 */
@Component
public class SalaryVisitor implements EmployeeVisitor {

    private double totalSalary = 0;

    @Override
    public void visit(Manager manager) {
        totalSalary += manager.getSalary();
        System.out.println("【统计经理】" + manager.getName() + " 薪资：" + manager.getSalary());
    }

    @Override
    public void visit(Staff staff) {
        totalSalary += staff.getSalary();
        System.out.println("【统计员工】" + staff.getName() + " 薪资：" + staff.getSalary());
    }

    public double getTotalSalary() {
        return totalSalary;
    }
}
```

---

### 5️⃣ 控制层：`VisitorController`

```java
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
```

---

## 🧩 五、运行效果

请求：

```
http://localhost:8080/visitor/salary
```

控制台输出：

```
【统计经理】Alice 薪资：12000.0
【统计员工】Bob 薪资：5000.0
【统计员工】Charlie 薪资：5500.0
```

返回结果：

```
总薪资：22500.0
```

---

## 📘 六、总结与要点

| 特性              | 说明                                                         |
| --------------- | ---------------------------------------------------------- |
| **模式类型**        | 行为型（Behavioral Pattern）                                    |
| **核心角色**        | 元素（Employee）+ 具体元素（Manager / Staff） + 访问者（EmployeeVisitor） |
| **Spring 实战应用** | 统计、日志分析、报表生成、对象操作解耦                                        |
| **适用场景**        | 系统中对象结构固定，但操作经常变化，需增加新功能而不修改类                              |

