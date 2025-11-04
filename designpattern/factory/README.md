# 工厂模式

工厂模式用于封装对象创建逻辑，通过工厂方法来创建不同类型的对象，而不是直接使用 `new`。在 Spring 中，`BeanFactory` 和 `ApplicationContext` 就是典型应用。可有效减少代码耦合，让对象创建更灵活。

## 简介

**工厂模式（Factory Pattern）**用于封装对象创建逻辑，让对象的实例化与使用分离，调用方只需关心“要什么类型”，而不用关心“怎么创建”。

在 Spring Boot 项目中，这个模式经常用于：

- 日志策略选择（DB 日志、文件日志、MQ 日志）
- 支付通道创建（支付宝、微信、银联）
- 消息发送器（钉钉、企业微信、飞书）
- 文件存储适配（本地、MinIO、OSS、COS）

## 项目结构

```
.
├── controller
│   └── FactoryDemoController.java
├── DesignpatternFactoryApplication.java
├── enums
│   └── LogTypeEnum.java
└── service
    ├── factory
    │   └── LogStrategyFactory.java
    └── strategy
        ├── impl
        │   ├── DatabaseLogStrategy.java
        │   └── FileLogStrategy.java
        └── LogStrategy.java
```



## 定义策略

### 策略枚举

```java
package io.github.atengk.enums;

/**
 * 日志类型枚举。
 *
 * <p>说明：
 * 通过枚举定义可扩展的日志类型，便于统一管理和校验。
 * </p>
 */
public enum LogTypeEnum {

    /**
     * 文件日志。
     */
    FILE("fileLogStrategy","文件日志"),

    /**
     * 数据库日志。
     */
    DB("databaseLogStrategy","数据库日志");

    private final String code;
    private final String name;

    LogTypeEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

}


```

### 策略接口

```java
package io.github.atengk.service.strategy;

import io.github.atengk.enums.LogTypeEnum;

/**
 * 日志策略接口。
 *
 * <p>说明：
 * 定义统一的日志输出接口，不同类型的日志策略（文件、数据库、MQ 等）实现此接口。
 * 这样调用方无需关心日志的实现细节，只需要选择对应类型即可。
 * </p>
 */
public interface LogStrategy {

    /**
     * 执行日志记录操作。
     *
     * @param message 日志消息内容
     */
    void log(String message);

    /**
     * 获取策略的名称，用于工厂识别。
     *
     * @return 策略标识字符串
     */
    LogTypeEnum getType();
}


```

### 策略实现类

#### 文件策略

```java
package io.github.atengk.service.strategy.impl;

import io.github.atengk.enums.LogTypeEnum;
import io.github.atengk.service.strategy.LogStrategy;
import org.springframework.stereotype.Component;

/**
 * 文件日志实现类。
 *
 * <p>说明：
 * 模拟将日志输出到文件（实际业务中可写入磁盘或对象存储）。
 * 使用 {@code @Component} 注册为 Spring Bean，便于工厂统一管理。
 * </p>
 */
@Component("fileLogStrategy")
public class FileLogStrategy implements LogStrategy {

    @Override
    public void log(String message) {
        System.out.println("[FileLog] 保存日志到文件系统：" + message);
    }

    @Override
    public LogTypeEnum getType() {
        return LogTypeEnum.FILE;
    }
}

```

### 数据库策略

```java
package io.github.atengk.service.strategy.impl;

import io.github.atengk.enums.LogTypeEnum;
import io.github.atengk.service.strategy.LogStrategy;
import org.springframework.stereotype.Component;

/**
 * 数据库日志实现类。
 *
 * <p>说明：
 * 模拟将日志写入数据库（实际业务可使用 MyBatis、JPA 等）。
 * </p>
 */
@Component("databaseLogStrategy")
public class DatabaseLogStrategy implements LogStrategy {

    @Override
    public void log(String message) {
        System.out.println("[DatabaseLog] 插入日志到数据库表：" + message);
    }

    @Override
    public LogTypeEnum getType() {
        return LogTypeEnum.DB;
    }
}

```

## 策略工厂类

```java
package io.github.atengk.service.factory;

import io.github.atengk.enums.LogTypeEnum;
import io.github.atengk.service.strategy.LogStrategy;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;

/**
 * 日志策略工厂类。
 *
 * <p>说明：
 * 本工厂类结合 Spring 的依赖注入功能，将所有实现 {@link LogStrategy} 接口的 Bean 自动注入到一个 Map 中。
 * Key 为 Bean 名称或类型标识，Value 为具体实现类。
 * 这样可以避免使用 if-else 或 switch 来判断类型，从而使工厂具备可扩展性。
 * </p>
 */
@Component
public class LogStrategyFactory {

    /**
     * Spring 自动注入所有 LogStrategy Bean，形成策略映射表。
     * key：Bean 名称
     * value：对应的策略实现类实例
     */
    private final Map<String, LogStrategy> strategyMap;

    /**
     * 构造方法注入策略映射。
     *
     * @param strategyMap 所有 LogStrategy Bean 的映射
     */
    public LogStrategyFactory(Map<String, LogStrategy> strategyMap) {
        this.strategyMap = strategyMap;
    }

    /**
     * 根据枚举类型获取对应策略实例
     *
     * @param type 日志类型枚举
     * @return 对应的日志策略实现类
     * @throws IllegalArgumentException 当未匹配到策略时抛出
     */
    public LogStrategy getStrategy(LogTypeEnum type) {
        if (type == null) {
            throw new IllegalArgumentException("日志类型不能为空");
        }
        LogStrategy strategy = strategyMap.get(type.getCode());
        if (strategy == null) {
            throw new IllegalArgumentException("未找到对应策略：" + type.getCode());
        }
        return strategy;
    }

    /**
     * 获取所有策略 Bean（只读）
     *
     * @return Map<String, NoticeStrategy> 不可修改视图
     */
    public Map<String, LogStrategy> getAllStrategies() {
        return Collections.unmodifiableMap(strategyMap);
    }

}


```

## 控制层调用示例

```java
package io.github.atengk.controller;

import io.github.atengk.enums.LogTypeEnum;
import io.github.atengk.service.factory.LogStrategyFactory;
import io.github.atengk.service.strategy.LogStrategy;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 工厂模式示例控制器。
 *
 * <p>说明：
 * 本控制器用于演示如何在业务中调用工厂模式。
 * 通过接口参数选择不同的日志类型，动态获取对应策略 Bean 执行日志操作。
 * </p>
 */
@RestController
@RequestMapping("/designpattern/factory")
public class FactoryDemoController {

    private final LogStrategyFactory logStrategyFactory;

    /**
     * 构造注入日志工厂。
     *
     * @param logStrategyFactory 工厂实例
     */
    public FactoryDemoController(LogStrategyFactory logStrategyFactory) {
        this.logStrategyFactory = logStrategyFactory;
    }

    /**
     * 通过日志类型参数调用对应策略。
     *
     * 示例：
     * <ul>
     *   <li>GET /designpattern/factory/log?type=FILE&msg=系统启动</li>
     *   <li>GET /designpattern/factory/log?type=DB&msg=用户登录</li>
     * </ul>
     *
     * @param logType 日志类型（FILE / DB）
     * @param msg  日志内容
     * @return 执行结果
     */
    @GetMapping("/log")
    public Map<String, Object> log(@RequestParam("logType") LogTypeEnum logType, @RequestParam("msg") String msg) {
        Map<String, Object> result = new HashMap<>(4);
        try {
            LogStrategy strategy = logStrategyFactory.getStrategy(logType);
            strategy.log(msg);
            result.put("success", true);
            result.put("type", logType.name());
            result.put("message", "日志记录成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        return result;
    }
}


```

## 验证方式

1. 启动 Spring Boot 应用；
2. 访问接口：
   - `GET http://localhost:18002/designpattern/factory/log?logType=FILE&msg=系统启动`
   - `GET http://localhost:18002/designpattern/factory/log?logType=DB&msg=用户登录`

控制台会输出：

```
[FileLog] 保存日志到文件系统：系统启动
[DatabaseLog] 插入日志到数据库表：用户登录
```
