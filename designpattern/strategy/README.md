# 策略模式

策略模式定义一系列算法或行为，并将它们封装成独立的策略类，使得它们可以相互替换。常用于根据不同类型执行不同逻辑，例如支付方式、日志记录方式、消息发送方式等。通常结合工厂模式使用，实现“可插拔式”业务逻辑。

## 简介

**策略模式（Strategy Pattern）** 是一种定义一系列算法、将每个算法封装起来并使它们可以相互替换的设计模式。
 它让算法的变化独立于使用它的客户端，常用于“行为根据条件不同而变化”的场景。

例如：
 在一个系统中，我们可能需要根据“支付方式”、“日志类型”、“消息发送渠道”等不同策略执行不同逻辑。
 如果使用传统的 `if-else` 判断，会造成代码臃肿、扩展性差。
 而通过 **策略模式 + Spring Bean 管理 + 工厂或枚举注册机制**，可以优雅地解决这一问题。

## 项目结构

```
.
├── controller
│   └── DemoController.java
├── DesignpatternStrategyApplication.java
├── enums
│   └── LogTypeEnum.java
└── service
    ├── context
    │   └── LogStrategyContext.java
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

#### 数据库策略

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

## 策略上下文

```java
package io.github.atengk.service.context;

import io.github.atengk.enums.LogTypeEnum;
import io.github.atengk.service.factory.LogStrategyFactory;
import io.github.atengk.service.strategy.LogStrategy;
import org.springframework.stereotype.Component;

/**
 * 策略上下文类
 * 封装策略选择逻辑，简化外部调用
 */
@Component
public class LogStrategyContext {

    private final LogStrategyFactory logStrategyFactory;

    public LogStrategyContext(LogStrategyFactory logStrategyFactory) {
        this.logStrategyFactory = logStrategyFactory;
    }

    /**
     * 执行日志记录操作
     *
     * @param type    日志类型枚举
     * @param message 日志消息内容
     */
    public void executeLog(LogTypeEnum type, String message) {
        LogStrategy strategy = logStrategyFactory.getStrategy(type);
        strategy.log(message);
    }

}

```



## 控制层调用示例

```java
package io.github.atengk.controller;

import io.github.atengk.enums.LogTypeEnum;
import io.github.atengk.service.context.LogStrategyContext;
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
@RequestMapping("/designpattern/strategy")
public class DemoController {

    private final LogStrategyContext logStrategyContext;

    /**
     * 构造注入日志上下文。
     *
     * @param logStrategyContext 上下文实例
     */
    public DemoController(LogStrategyContext logStrategyContext) {
        this.logStrategyContext = logStrategyContext;
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
            logStrategyContext.executeLog(logType, msg);
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
   - `GET http://localhost:18003/designpattern/strategy/log?logType=FILE&msg=系统启动`
   - `GET http://localhost:18003/designpattern/strategy/log?logType=DB&msg=用户登录`

控制台会输出：

```
[FileLog] 保存日志到文件系统：系统启动
[DatabaseLog] 插入日志到数据库表：用户登录
```
