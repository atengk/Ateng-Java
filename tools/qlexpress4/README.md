# QLExpress 规则引擎

QLExpress 是由阿里巴巴开源的一款轻量级 Java 动态表达式引擎，主要用于在业务中实现灵活的规则计算与脚本执行。它支持类 Java 语法，具备自定义函数、操作符扩展能力，并可通过上下文动态传参，适用于风控、计费、流程条件等场景。该引擎具有高性能（支持表达式缓存）、良好的安全控制机制，以及表达式执行追踪能力，便于分析规则执行过程。同时，QLExpress 原生支持 JSON、函数式编程等特性，能够快速构建业务 DSL，提升复杂规则系统的开发效率。

- [Github](https://github.com/alibaba/QLExpress)



## 基础配置

**添加依赖**

```xml
<!-- 规则/表达式引擎：QLExpress -->
<dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>qlexpress4</artifactId>
    <version>4.1.0</version>
</dependency>
```



## 基础使用

### 基础表达式

```java
/**
 * 基础表达式
 */
@Test
void test_basic_calculate() {
    // 1. 创建表达式执行器（建议生产环境单例）
    Express4Runner runner = new Express4Runner(InitOptions.DEFAULT_OPTIONS);

    // 2. 构建上下文（表达式中的变量来源）
    Map<String, Object> context = new HashMap<>();
    context.put("a", 1);
    context.put("b", 2);
    context.put("c", 3);

    // 3. 执行表达式（支持类 Java 语法）
    QLResult result = runner.execute(
            "a + b * c",
            context,
            QLOptions.DEFAULT_OPTIONS
    );

    // 4. 获取执行结果
    Object value = result.getResult();
    System.out.println("执行结果：" + value);
    // 执行结果：7
}
```

### 条件表达式

```java
/**
 * 条件表达式测试（if / else）
 * 根据年龄判断是否成年
 */
@Test
void test_if_else() {
    Express4Runner runner = new Express4Runner(InitOptions.DEFAULT_OPTIONS);

    Map<String, Object> context = new HashMap<>();
    context.put("age", 20);

    QLResult result = runner.execute(
            "if (age >= 18) { return '成人'; } else { return '未成年'; }",
            context,
            QLOptions.DEFAULT_OPTIONS
    );

    System.out.println("执行结果：" + result.getResult());
    // 执行结果：成人
}
```

### 逻辑表达式

```java
/**
 * 逻辑表达式测试（&& / ||）
 * 判断是否满足业务条件
 */
@Test
void test_logic() {
    Express4Runner runner = new Express4Runner(InitOptions.DEFAULT_OPTIONS);

    Map<String, Object> context = new HashMap<>();
    context.put("score", 80);
    context.put("level", 3);

    QLResult result = runner.execute(
            "score > 60 && level >= 3",
            context,
            QLOptions.DEFAULT_OPTIONS
    );

    System.out.println("执行结果：" + result.getResult());
    // 执行结果：true
}
```

### 字符串表达式

```java
/**
 * 字符串表达式测试
 * 实现字符串拼接
 */
@Test
void test_string() {
    Express4Runner runner = new Express4Runner(InitOptions.DEFAULT_OPTIONS);

    Map<String, Object> context = new HashMap<>();
    context.put("name", "Ateng");

    QLResult result = runner.execute(
            " 'Hello ' + name ",
            context,
            QLOptions.DEFAULT_OPTIONS
    );

    System.out.println("执行结果：" + result.getResult());
    // 执行结果：Hello Ateng
}
```

### 三元表达式

```java
/**
 * 三元表达式测试
 * 简化 if else 写法
 */
@Test
void test_ternary() {
    Express4Runner runner = new Express4Runner(InitOptions.DEFAULT_OPTIONS);

    Map<String, Object> context = new HashMap<>();
    context.put("score", 75);

    QLResult result = runner.execute(
            "score >= 60 ? '及格' : '不及格'",
            context,
            QLOptions.DEFAULT_OPTIONS
    );

    System.out.println("执行结果：" + result.getResult());
    // 执行结果：及格
}
```

### 自定义函数

```java
/**
 * 自定义函数测试
 * 注册 max 函数并调用
 */
@Test
void test_custom_function() {
    Express4Runner runner = new Express4Runner(InitOptions.DEFAULT_OPTIONS);

    runner.addFunction("max", new CustomFunction() {
        @Override
        public Object call(QContext qContext, Parameters parameters) throws Throwable {

            // 1. 解包参数
            Object aObj = parameters.get(0).get();
            Object bObj = parameters.get(1).get();

            // 2. 类型转换
            int a = ((Number) aObj).intValue();
            int b = ((Number) bObj).intValue();

            return Math.max(a, b);
        }
    });

    Map<String, Object> context = new HashMap<>();
    context.put("a", 10);
    context.put("b", 20);

    QLResult result = runner.execute(
            "max(a, b)",
            context,
            QLOptions.DEFAULT_OPTIONS
    );

    System.out.println("执行结果：" + result.getResult());
    // 执行结果：20
}
```

### Null 安全表达式

```java
/**
 * Null 安全表达式
 */
@Test
void test_null_safe() {
    Express4Runner runner = new Express4Runner(InitOptions.DEFAULT_OPTIONS);

    Map<String, Object> context = new HashMap<>();
    context.put("user", null);

    QLResult result = runner.execute(
            "user == null ? '空用户' : '正常用户'",
            context,
            QLOptions.DEFAULT_OPTIONS
    );

    System.out.println("执行结果：" + result.getResult());
    // 执行结果：空用户
}
```

### 集合 / Map 访问

```java
/**
 * 集合 / Map 访问
 */
@Test
void test_map_access() {
    Express4Runner runner = new Express4Runner(InitOptions.DEFAULT_OPTIONS);

    Map<String, Object> user = new HashMap<>();
    user.put("age", 25);
    user.put("level", 3);

    Map<String, Object> context = new HashMap<>();
    context.put("user", user);

    QLResult result = runner.execute(
            "user['age'] > 18 && user['level'] >= 3",
            context,
            QLOptions.DEFAULT_OPTIONS
    );

    System.out.println("执行结果：" + result.getResult());
    // 执行结果：true
}
```

### 多条件规则

```java
/**
 * 多条件规则
 */
@Test
void test_risk_rule() {
    Express4Runner runner = new Express4Runner(InitOptions.DEFAULT_OPTIONS);

    Map<String, Object> context = new HashMap<>();
    context.put("amount", 12000);
    context.put("userLevel", 2);
    context.put("blackList", false);

    QLResult result = runner.execute(
            "amount > 10000 && userLevel < 3 && !blackList",
            context,
            QLOptions.DEFAULT_OPTIONS
    );

    System.out.println("执行结果：" + result.getResult());
    // 执行结果：true
}
```



## 在 SpringBoot 中使用

### 创建核心配置（ExpressRunner 单例）

SpringBoot 中必须把 Runner 做成单例，否则性能会很差。

```java
package io.github.atengk.config;

import com.alibaba.qlexpress4.Express4Runner;
import com.alibaba.qlexpress4.InitOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * QLExpress 配置类
 *
 * @author Ateng
 * @date 2026/04/07
 */
@Configuration
public class QLExpressConfig {

    /**
     * 全局唯一执行器
     */
    @Bean
    public Express4Runner express4Runner() {
        return new Express4Runner(InitOptions.DEFAULT_OPTIONS);
    }
}

```

### 封装执行工具类（核心入口）

所有业务统一走这个工具类，避免散落调用。

```java
package io.github.atengk.util;

import com.alibaba.qlexpress4.Express4Runner;
import com.alibaba.qlexpress4.QLOptions;
import com.alibaba.qlexpress4.QLResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * QLExpress 执行工具类
 *
 * @author Ateng
 * @date 2026/04/07
 */
@Component
@RequiredArgsConstructor
public class QLExpressUtil {

    private final Express4Runner runner;

    /**
     * 执行表达式
     */
    public Object execute(String expression, Map<String, Object> params) {

        QLResult result = runner.execute(
                expression,
                params,
                QLOptions.DEFAULT_OPTIONS
        );

        return result.getResult();
    }
}

```

### Controller 测试接口（验证使用）

```java
package io.github.atengk.controller;

import io.github.atengk.util.QLExpressUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * QLExpress 测试接口
 *
 * @author Ateng
 * @date 2026/04/07
 */
@RestController
@RequestMapping("/ql")
@RequiredArgsConstructor
public class QLExpressController {

    private final QLExpressUtil qlExpressUtil;

    /**
     * 测试表达式执行
     */
    @GetMapping("/test")
    public Object test() {

        Map<String, Object> params = new HashMap<>();
        params.put("a", 10);
        params.put("b", 20);

        String expression = "a + b * 2";

        return qlExpressUtil.execute(expression, params);
    }
}
```

