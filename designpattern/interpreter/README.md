# 🧩 解释器模式（Interpreter Pattern）

---

## 🌟 一、模式简介

**解释器模式（Interpreter Pattern）** 是一种**行为型设计模式**，用于定义**语言的文法表示**，并提供一个解释器来解释这些语句。

在 **Spring Boot** 项目中，解释器模式常用于：

* 简单表达式计算
* 配置规则解析
* 小型 DSL（领域特定语言）解释器
* 公式、策略表达式执行

---

## 🧠 二、场景举例（实战导向）

假设我们有一个「权限表达式系统」，管理员可以定义权限规则：

```
"ADMIN OR (USER AND VIP)"
```

系统可以解释这些规则，判断某个用户是否有访问权限。
解释器模式可以通过构建语法树（AST）来解析并执行这些表达式。

---

## 🏗️ 三、项目结构

```
io.github.atengk
 ├── controller/
 │    └── InterpreterController.java
 ├── service/
 │    └── interpreter/
 │          ├── Expression.java
 │          ├── TerminalExpression.java
 │          ├── OrExpression.java
 │          └── AndExpression.java
 └── DesignPatternApplication.java
```

---

## 💡 四、代码实现（Spring Boot 实战版）

---

### 1️⃣ 抽象表达式接口：`Expression`

```java
package io.github.atengk.service.interpreter;

/**
 * 抽象表达式接口
 */
public interface Expression {

    /**
     * 解释输入的上下文
     *
     * @param context 输入上下文
     * @return 是否匹配
     */
    boolean interpret(String context);
}
```

---

### 2️⃣ 终结符表达式：`TerminalExpression`

```java
package io.github.atengk.service.interpreter;

/**
 * 终结符表达式
 */
public class TerminalExpression implements Expression {

    private final String data;

    public TerminalExpression(String data) {
        this.data = data;
    }

    @Override
    public boolean interpret(String context) {
        return context.contains(data);
    }
}
```

---

### 3️⃣ 非终结符表达式：`OrExpression` 与 `AndExpression`

```java
package io.github.atengk.service.interpreter;

/**
 * OR 表达式
 */
public class OrExpression implements Expression {

    private final Expression expr1;
    private final Expression expr2;

    public OrExpression(Expression expr1, Expression expr2) {
        this.expr1 = expr1;
        this.expr2 = expr2;
    }

    @Override
    public boolean interpret(String context) {
        return expr1.interpret(context) || expr2.interpret(context);
    }
}

/**
 * AND 表达式
 */
public class AndExpression implements Expression {

    private final Expression expr1;
    private final Expression expr2;

    public AndExpression(Expression expr1, Expression expr2) {
        this.expr1 = expr1;
        this.expr2 = expr2;
    }

    @Override
    public boolean interpret(String context) {
        return expr1.interpret(context) && expr2.interpret(context);
    }
}
```

---

### 4️⃣ 控制层：`InterpreterController`

```java
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
```

---

## 🧩 五、运行效果

请求：

```
http://localhost:8080/interpreter/check?userRoles=USER VIP
```

返回：

```
用户角色 [USER VIP] 权限检查结果: true
```

请求：

```
http://localhost:8080/interpreter/check?userRoles=USER
```

返回：

```
用户角色 [USER] 权限检查结果: false
```

---

## 📘 六、总结与要点

| 特性              | 说明                                                                               |
| --------------- | -------------------------------------------------------------------------------- |
| **模式类型**        | 行为型（Behavioral Pattern）                                                          |
| **核心角色**        | 抽象表达式（Expression） + 终结符（TerminalExpression） + 非终结符（AndExpression / OrExpression） |
| **Spring 实战应用** | 权限解析、规则引擎、DSL 解释器、公式计算                                                           |
| **适用场景**        | 需要对语言或表达式进行解释、解析、执行                                                              |

