# 依赖注入模式

依赖注入模式是 Spring Boot 项目中最基础、最常用的工程实践模式，属于当前设计模式文档体系中的 **Spring Boot 实战补充模式**。它的核心作用是由外部容器装配对象依赖，而不是由业务对象自己创建依赖对象。Spring 的核心基础之一就是依赖注入。

在实际项目中，Controller 注入 Service、Service 注入 Repository、策略类注入多个实现、配置类注入属性对象、测试环境替换 Mock Bean，本质上都属于依赖注入的使用场景。

## 基础配置

本示例基于 **JDK 21 + Spring Boot 3**，使用一个“通知发送”场景说明依赖注入模式。业务目标是：根据请求中的通知类型，自动选择短信、邮件或站内信发送器，而业务层不直接 `new` 具体实现类。

普通写法中，业务代码可能会直接创建依赖：

```java
NotifySender sender = new EmailNotifySender();
sender.send(message);
```

这种写法会让业务层和具体实现强耦合。使用依赖注入后，发送器由 Spring 容器管理，业务层只依赖接口和注册器：

```java
NotifySender sender = notifySenderRegistry.getSender(request.notifyType());
sender.send(message);
```

### 项目依赖

文件位置：`pom.xml`

下面配置 Web、Validation、Configuration Processor、Hutool 和 Lombok。示例中使用配置属性注入、多实现注入、构造器注入和接口注入。

```xml
<dependencies>
    <!-- Spring Web：提供 REST API 能力 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Spring Validation：用于请求参数和配置属性校验 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- Configuration Processor：生成配置元数据，提升 application.yml 提示体验 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-configuration-processor</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- Hutool：常用工具类，简化字符串、集合、对象等处理 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.8.35</version>
    </dependency>

    <!-- Lombok：减少构造器、日志对象等样板代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

### 应用配置

文件位置：`src/main/resources/application.yml`

这里配置默认通知渠道和启用的通知渠道。后续通过 `@ConfigurationProperties` 注入到配置对象中。

```yaml
spring:
  application:
    name: design-pattern-dependency-injection

server:
  port: 8080

notify:
  default-type: EMAIL       # 默认通知类型
  enabled-types:            # 当前启用的通知类型
    - SMS
    - EMAIL
    - IN_APP
```

### 文件结构

依赖注入模式本身不是某一个固定类，而是一种对象装配方式。下面示例按 Controller、Service、Sender、Registry、Properties 分层组织。

```text
src/main/java/io/github/atengk/dependencyinjection
├── DependencyInjectionApplication.java
├── config
│   └── NotifyProperties.java
├── controller
│   └── NotifyController.java
├── dto
│   └── SendNotifyRequest.java
├── registry
│   └── NotifySenderRegistry.java
├── sender
│   ├── EmailNotifySender.java
│   ├── InAppNotifySender.java
│   ├── NotifyMessage.java
│   ├── NotifySender.java
│   ├── NotifyType.java
│   └── SmsNotifySender.java
├── service
│   ├── NotifyService.java
│   └── impl
│       └── NotifyServiceImpl.java
└── vo
    └── SendNotifyResponse.java
```

## 模式说明

依赖注入模式的核心思想是：对象不主动创建依赖，而是由外部容器把依赖传进来。

不推荐的写法：

```text
NotifyServiceImpl
 -> new SmsNotifySender()
 -> new EmailNotifySender()
 -> new InAppNotifySender()
```

推荐的写法：

```text
Spring Container
 -> 创建 SmsNotifySender
 -> 创建 EmailNotifySender
 -> 创建 InAppNotifySender
 -> 注入 NotifySenderRegistry
 -> 注入 NotifyServiceImpl
 -> 注入 NotifyController
```

在 Spring Boot 项目中，常见依赖注入方式有三种：

| 注入方式    | 示例                                                         | 推荐程度     |
| ----------- | ------------------------------------------------------------ | ------------ |
| 构造器注入  | `private final NotifyService notifyService` + `@RequiredArgsConstructor` | 推荐         |
| Setter 注入 | `setNotifyService(...)`                                      | 适合可选依赖 |
| 字段注入    | `@Autowired private NotifyService notifyService`             | 不推荐       |

推荐优先使用构造器注入，因为它可以保证依赖不可变、便于单元测试，也更容易发现循环依赖问题。

## 核心代码

这一节给出依赖注入模式的完整关键代码。示例重点体现四点：

```text
1. Controller 依赖 Service 接口。
2. Service 依赖 NotifySenderRegistry 和配置属性对象。
3. NotifySenderRegistry 注入所有 NotifySender 实现。
4. 新增通知渠道时，业务层不需要修改核心调用逻辑。
```

### 启动类

文件位置：`src/main/java/io/github/atengk/dependencyinjection/DependencyInjectionApplication.java`

启动类用于启动 Spring Boot 应用，并开启配置属性扫描。

```java
package io.github.atengk.dependencyinjection;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * 依赖注入模式示例启动类
 *
 * @author Ateng
 * @since 2026-05-13
 */
@ConfigurationPropertiesScan
@SpringBootApplication
public class DependencyInjectionApplication {

    public static void main(String[] args) {
        SpringApplication.run(DependencyInjectionApplication.class, args);
    }

}
```

### 通知配置属性

文件位置：`src/main/java/io/github/atengk/dependencyinjection/config/NotifyProperties.java`

该配置类用于接收 `application.yml` 中的 `notify` 配置，实现配置属性注入。

```java
package io.github.atengk.dependencyinjection.config;

import io.github.atengk.dependencyinjection.sender.NotifyType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * 通知配置属性
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Validated
@ConfigurationProperties(prefix = "notify")
public record NotifyProperties(

        @NotNull(message = "默认通知类型不能为空")
        NotifyType defaultType,

        @NotEmpty(message = "启用通知类型不能为空")
        List<NotifyType> enabledTypes

) {
}
```

### 通知类型枚举

文件位置：`src/main/java/io/github/atengk/dependencyinjection/sender/NotifyType.java`

该枚举定义系统支持的通知类型。

```java
package io.github.atengk.dependencyinjection.sender;

/**
 * 通知类型
 *
 * @author Ateng
 * @since 2026-05-13
 */
public enum NotifyType {

    SMS,

    EMAIL,

    IN_APP

}
```

### 通知消息对象

文件位置：`src/main/java/io/github/atengk/dependencyinjection/sender/NotifyMessage.java`

该对象用于承载通知发送所需数据。

```java
package io.github.atengk.dependencyinjection.sender;

import java.time.LocalDateTime;

/**
 * 通知消息对象
 *
 * @author Ateng
 * @since 2026-05-13
 */
public record NotifyMessage(

        String receiver,

        String title,

        String content,

        LocalDateTime createTime

) {
}
```

### 通知发送器接口

文件位置：`src/main/java/io/github/atengk/dependencyinjection/sender/NotifySender.java`

该接口定义通知发送器的统一行为。业务层不直接依赖短信、邮件、站内信等具体实现。

```java
package io.github.atengk.dependencyinjection.sender;

/**
 * 通知发送器接口
 *
 * @author Ateng
 * @since 2026-05-13
 */
public interface NotifySender {

    /**
     * 获取通知类型
     *
     * @return 通知类型
     */
    NotifyType notifyType();

    /**
     * 发送通知
     *
     * @param message 通知消息
     */
    void send(NotifyMessage message);

}
```

## 多实现注入

这一节定义三个通知发送器实现。它们都实现 `NotifySender` 接口，并通过 `@Component` 注册到 Spring 容器中。

### 短信通知发送器

文件位置：`src/main/java/io/github/atengk/dependencyinjection/sender/SmsNotifySender.java`

该实现类负责短信通知发送。示例中使用日志模拟真实短信服务调用。

```java
package io.github.atengk.dependencyinjection.sender;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 短信通知发送器
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class SmsNotifySender implements NotifySender {

    /**
     * 获取通知类型
     *
     * @return 通知类型
     */
    @Override
    public NotifyType notifyType() {
        return NotifyType.SMS;
    }

    /**
     * 发送短信通知
     *
     * @param message 通知消息
     */
    @Override
    public void send(NotifyMessage message) {
        String smsContent = StrUtil.format("【系统通知】{}：{}", message.title(), message.content());
        log.info("发送短信通知，receiver={}，content={}", message.receiver(), smsContent);

        // 真实项目中可以在这里调用短信供应商 API。
    }

}
```

### 邮件通知发送器

文件位置：`src/main/java/io/github/atengk/dependencyinjection/sender/EmailNotifySender.java`

该实现类负责邮件通知发送。示例中使用日志模拟真实邮件服务调用。

```java
package io.github.atengk.dependencyinjection.sender;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 邮件通知发送器
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class EmailNotifySender implements NotifySender {

    /**
     * 获取通知类型
     *
     * @return 通知类型
     */
    @Override
    public NotifyType notifyType() {
        return NotifyType.EMAIL;
    }

    /**
     * 发送邮件通知
     *
     * @param message 通知消息
     */
    @Override
    public void send(NotifyMessage message) {
        String subject = StrUtil.format("系统通知 - {}", message.title());
        log.info("发送邮件通知，receiver={}，subject={}，content={}",
                message.receiver(), subject, message.content());

        // 真实项目中可以在这里调用 JavaMail、企业邮箱或第三方邮件服务。
    }

}
```

### 站内信通知发送器

文件位置：`src/main/java/io/github/atengk/dependencyinjection/sender/InAppNotifySender.java`

该实现类负责站内信通知发送。示例中使用日志模拟保存站内信记录。

```java
package io.github.atengk.dependencyinjection.sender;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 站内信通知发送器
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class InAppNotifySender implements NotifySender {

    /**
     * 获取通知类型
     *
     * @return 通知类型
     */
    @Override
    public NotifyType notifyType() {
        return NotifyType.IN_APP;
    }

    /**
     * 发送站内信通知
     *
     * @param message 通知消息
     */
    @Override
    public void send(NotifyMessage message) {
        log.info("保存站内信通知，receiver={}，title={}，content={}，createTime={}",
                message.receiver(), message.title(), message.content(), message.createTime());

        // 真实项目中可以在这里写入站内信表或推送 WebSocket 消息。
    }

}
```

## 注册器代码

当一个接口存在多个实现时，Spring 可以直接注入 `List<NotifySender>` 或 `Map<String, NotifySender>`。本示例使用 `List<NotifySender>` 构建一个按通知类型索引的注册器。

### 通知发送器注册器

文件位置：`src/main/java/io/github/atengk/dependencyinjection/registry/NotifySenderRegistry.java`

该注册器通过构造器注入所有 `NotifySender` 实现，并提供按类型获取发送器的能力。

```java
package io.github.atengk.dependencyinjection.registry;

import cn.hutool.core.collection.CollUtil;
import io.github.atengk.dependencyinjection.config.NotifyProperties;
import io.github.atengk.dependencyinjection.sender.NotifySender;
import io.github.atengk.dependencyinjection.sender.NotifyType;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 通知发送器注册器
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class NotifySenderRegistry {

    private final List<NotifySender> notifySenders;

    private final NotifyProperties notifyProperties;

    private final Map<NotifyType, NotifySender> senderMap = new EnumMap<>(NotifyType.class);

    public NotifySenderRegistry(List<NotifySender> notifySenders, NotifyProperties notifyProperties) {
        this.notifySenders = CollUtil.emptyIfNull(notifySenders);
        this.notifyProperties = notifyProperties;
    }

    /**
     * 初始化通知发送器映射
     */
    @PostConstruct
    public void init() {
        for (NotifySender notifySender : notifySenders) {
            NotifyType notifyType = notifySender.notifyType();

            if (!notifyProperties.enabledTypes().contains(notifyType)) {
                log.info("通知发送器未启用，notifyType={}，sender={}",
                        notifyType, notifySender.getClass().getSimpleName());
                continue;
            }

            if (senderMap.containsKey(notifyType)) {
                throw new IllegalStateException("通知类型存在重复发送器：" + notifyType);
            }

            senderMap.put(notifyType, notifySender);
            log.info("注册通知发送器成功，notifyType={}，sender={}",
                    notifyType, notifySender.getClass().getSimpleName());
        }

        if (CollUtil.isEmpty(senderMap)) {
            throw new IllegalStateException("没有可用的通知发送器");
        }
    }

    /**
     * 根据通知类型获取发送器
     *
     * @param notifyType 通知类型
     * @return 通知发送器
     */
    public NotifySender getSender(NotifyType notifyType) {
        NotifyType actualType = notifyType == null ? notifyProperties.defaultType() : notifyType;
        NotifySender notifySender = senderMap.get(actualType);

        if (notifySender == null) {
            throw new IllegalArgumentException("通知类型未启用或不存在：" + actualType);
        }

        return notifySender;
    }

}
```

这段代码体现了依赖注入在多实现扩展中的价值。新增一个 `WebhookNotifySender` 时，只要实现 `NotifySender` 并加上 `@Component`，注册器就可以自动接收到它。业务 Service 不需要新增 `new WebhookNotifySender()`。

## 业务代码

这一节给出接口请求对象、响应对象、Service 和 Controller。业务层只关心通知发送能力，不关心具体实现类的创建过程。

### 请求 DTO

文件位置：`src/main/java/io/github/atengk/dependencyinjection/dto/SendNotifyRequest.java`

该 DTO 用于接收通知发送请求。

```java
package io.github.atengk.dependencyinjection.dto;

import io.github.atengk.dependencyinjection.sender.NotifyType;
import jakarta.validation.constraints.NotBlank;

/**
 * 发送通知请求
 *
 * @author Ateng
 * @since 2026-05-13
 */
public record SendNotifyRequest(

        NotifyType notifyType,

        @NotBlank(message = "接收人不能为空")
        String receiver,

        @NotBlank(message = "通知标题不能为空")
        String title,

        @NotBlank(message = "通知内容不能为空")
        String content

) {
}
```

### 响应 VO

文件位置：`src/main/java/io/github/atengk/dependencyinjection/vo/SendNotifyResponse.java`

该 VO 用于返回通知发送结果。

```java
package io.github.atengk.dependencyinjection.vo;

import io.github.atengk.dependencyinjection.sender.NotifyType;

import java.time.LocalDateTime;

/**
 * 发送通知响应
 *
 * @author Ateng
 * @since 2026-05-13
 */
public record SendNotifyResponse(

        Boolean success,

        NotifyType notifyType,

        String receiver,

        String message,

        LocalDateTime sendTime

) {
}
```

### Service 接口

文件位置：`src/main/java/io/github/atengk/dependencyinjection/service/NotifyService.java`

该接口定义通知发送能力。

```java
package io.github.atengk.dependencyinjection.service;

import io.github.atengk.dependencyinjection.dto.SendNotifyRequest;
import io.github.atengk.dependencyinjection.vo.SendNotifyResponse;

/**
 * 通知业务接口
 *
 * @author Ateng
 * @since 2026-05-13
 */
public interface NotifyService {

    /**
     * 发送通知
     *
     * @param request 发送通知请求
     * @return 发送通知响应
     */
    SendNotifyResponse send(SendNotifyRequest request);

}
```

### Service 实现

文件位置：`src/main/java/io/github/atengk/dependencyinjection/service/impl/NotifyServiceImpl.java`

该实现类通过构造器注入 `NotifySenderRegistry`，再由注册器选择具体发送器。

```java
package io.github.atengk.dependencyinjection.service.impl;

import io.github.atengk.dependencyinjection.dto.SendNotifyRequest;
import io.github.atengk.dependencyinjection.registry.NotifySenderRegistry;
import io.github.atengk.dependencyinjection.sender.NotifyMessage;
import io.github.atengk.dependencyinjection.sender.NotifySender;
import io.github.atengk.dependencyinjection.sender.NotifyType;
import io.github.atengk.dependencyinjection.service.NotifyService;
import io.github.atengk.dependencyinjection.vo.SendNotifyResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 通知业务实现
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotifyServiceImpl implements NotifyService {

    private final NotifySenderRegistry notifySenderRegistry;

    /**
     * 发送通知
     *
     * @param request 发送通知请求
     * @return 发送通知响应
     */
    @Override
    public SendNotifyResponse send(SendNotifyRequest request) {
        NotifySender notifySender = notifySenderRegistry.getSender(request.notifyType());
        NotifyType actualNotifyType = notifySender.notifyType();

        NotifyMessage message = new NotifyMessage(
                request.receiver(),
                request.title(),
                request.content(),
                LocalDateTime.now()
        );

        notifySender.send(message);
        log.info("通知发送完成，notifyType={}，receiver={}", actualNotifyType, request.receiver());

        return new SendNotifyResponse(
                true,
                actualNotifyType,
                request.receiver(),
                "发送成功",
                LocalDateTime.now()
        );
    }

}
```

### Controller 接口

文件位置：`src/main/java/io/github/atengk/dependencyinjection/controller/NotifyController.java`

该 Controller 通过构造器注入 `NotifyService`。它只依赖业务接口，不依赖具体实现。

```java
package io.github.atengk.dependencyinjection.controller;

import io.github.atengk.dependencyinjection.dto.SendNotifyRequest;
import io.github.atengk.dependencyinjection.service.NotifyService;
import io.github.atengk.dependencyinjection.vo.SendNotifyResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 通知接口
 *
 * @author Ateng
 * @since 2026-05-13
 */
@RestController
@RequestMapping("/notifies")
@RequiredArgsConstructor
public class NotifyController {

    private final NotifyService notifyService;

    /**
     * 发送通知
     *
     * @param request 发送通知请求
     * @return 发送通知响应
     */
    @PostMapping("/send")
    public SendNotifyResponse send(@Valid @RequestBody SendNotifyRequest request) {
        return notifyService.send(request);
    }

}
```

## 使用方式

本示例提供一个发送通知接口。请求中可以指定通知类型，如果不传 `notifyType`，系统会使用配置中的默认通知类型 `EMAIL`。

### 发送短信通知

接口信息：

| 项目         | 内容                 |
| ------------ | -------------------- |
| 请求路径     | `/notifies/send`     |
| 请求方法     | `POST`               |
| Content-Type | `application/json`   |
| 主要作用     | 根据通知类型发送通知 |

请求示例：

```bash
curl -X POST 'http://localhost:8080/notifies/send' \
  -H 'Content-Type: application/json' \
  -d '{
    "notifyType": "SMS",
    "receiver": "13800000000",
    "title": "订单创建成功",
    "content": "您的订单已经创建成功，请及时支付"
  }'
```

响应示例：

```json
{
  "success": true,
  "notifyType": "SMS",
  "receiver": "13800000000",
  "message": "发送成功",
  "sendTime": "2026-05-13T10:30:00"
}
```

### 发送邮件通知

请求示例：

```bash
curl -X POST 'http://localhost:8080/notifies/send' \
  -H 'Content-Type: application/json' \
  -d '{
    "notifyType": "EMAIL",
    "receiver": "user@example.com",
    "title": "账户安全提醒",
    "content": "您的账户刚刚完成一次登录"
  }'
```

### 使用默认通知类型

如果请求中不传 `notifyType`，注册器会使用 `notify.default-type` 配置的默认类型。

```bash
curl -X POST 'http://localhost:8080/notifies/send' \
  -H 'Content-Type: application/json' \
  -d '{
    "receiver": "user@example.com",
    "title": "默认渠道通知",
    "content": "这条通知会使用默认通知类型发送"
  }'
```

响应中的 `notifyType` 应为：

```json
{
  "notifyType": "EMAIL"
}
```

## 验证方式

可以通过接口响应和日志验证依赖注入是否生效。

启动项目：

```bash
mvn spring-boot:run
```

发送短信通知：

```bash
curl -X POST 'http://localhost:8080/notifies/send' \
  -H 'Content-Type: application/json' \
  -d '{
    "notifyType": "SMS",
    "receiver": "13800000000",
    "title": "测试短信",
    "content": "这是一条短信通知"
  }'
```

发送站内信通知：

```bash
curl -X POST 'http://localhost:8080/notifies/send' \
  -H 'Content-Type: application/json' \
  -d '{
    "notifyType": "IN_APP",
    "receiver": "10001",
    "title": "测试站内信",
    "content": "这是一条站内信通知"
  }'
```

验证点：

```text
1. 启动日志中可以看到 SmsNotifySender、EmailNotifySender、InAppNotifySender 被注册。
2. NotifyController 中没有 new NotifyServiceImpl。
3. NotifyServiceImpl 中没有 new SmsNotifySender、new EmailNotifySender。
4. NotifySenderRegistry 通过 List<NotifySender> 自动获得所有发送器实现。
5. 新增发送器实现后，只要交给 Spring 管理，就可以被自动注入。
```

如果启动失败，重点检查：

```text
1. 启动类是否添加 @ConfigurationPropertiesScan。
2. NotifyProperties 是否使用 @ConfigurationProperties(prefix = "notify")。
3. application.yml 中 notify.default-type 是否能转换为 NotifyType。
4. 所有 NotifySender 实现是否添加 @Component。
5. 是否存在两个发送器返回相同 notifyType。
```

## 构造器注入

构造器注入是 Spring Boot 项目中最推荐的依赖注入方式。它可以让依赖在对象创建时一次性传入，并且配合 `final` 字段保证依赖不可变。

推荐写法：

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class NotifyServiceImpl implements NotifyService {

    private final NotifySenderRegistry notifySenderRegistry;

}
```

这段代码通过 Lombok 的 `@RequiredArgsConstructor` 生成构造器。Spring 会自动使用这个构造器注入 `NotifySenderRegistry`。

不推荐字段注入：

```java
@Autowired
private NotifySenderRegistry notifySenderRegistry;
```

字段注入的问题是：

```text
1. 不利于单元测试手动构造对象。
2. 依赖关系不够明确。
3. 字段不能声明为 final。
4. 容易隐藏循环依赖问题。
```

## 多实现注入

当一个接口有多个实现时，Spring Boot 支持多种注入方式。

### 注入指定实现

如果只想注入某一个实现，可以使用 `@Qualifier`。

示例：

```java
package io.github.atengk.dependencyinjection.service.impl;

import io.github.atengk.dependencyinjection.sender.NotifyMessage;
import io.github.atengk.dependencyinjection.sender.NotifySender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 审计通知业务
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Service
public class AuditNotifyService {

    private final NotifySender emailNotifySender;

    public AuditNotifyService(@Qualifier("emailNotifySender") NotifySender emailNotifySender) {
        this.emailNotifySender = emailNotifySender;
    }

    /**
     * 发送审计通知
     *
     * @param receiver 接收人
     * @param content 通知内容
     */
    public void sendAuditNotify(String receiver, String content) {
        NotifyMessage message = new NotifyMessage(
                receiver,
                "审计通知",
                content,
                LocalDateTime.now()
        );

        emailNotifySender.send(message);
        log.info("审计通知发送完成，receiver={}", receiver);
    }

}
```

这里的 `emailNotifySender` Bean 名称默认来自类名 `EmailNotifySender` 的首字母小写形式。

### 注入所有实现

如果需要根据业务类型动态选择实现，推荐注入 `List<NotifySender>` 或 `Map<String, NotifySender>`。

示例：

```java
public NotifySenderRegistry(List<NotifySender> notifySenders, NotifyProperties notifyProperties) {
    this.notifySenders = CollUtil.emptyIfNull(notifySenders);
    this.notifyProperties = notifyProperties;
}
```

这种方式非常适合策略模式、命令模式、责任链模式、事件处理器注册等场景。

## 配置属性注入

业务代码中不要把配置写死。例如默认通知类型、启用通知类型、发送频率、第三方接口地址等，都应该通过配置注入。

推荐写法：

```java
@ConfigurationProperties(prefix = "notify")
public record NotifyProperties(
        NotifyType defaultType,
        List<NotifyType> enabledTypes
) {
}
```

相比直接使用 `@Value`：

```java
@Value("${notify.default-type}")
private String defaultType;
```

`@ConfigurationProperties` 更适合复杂配置对象，优势是：

```text
1. 支持配置分组。
2. 支持类型转换。
3. 支持参数校验。
4. 支持配置元数据提示。
5. 更适合多字段配置。
```

`@Value` 更适合少量、简单、临时的配置读取。

## 测试中的依赖替换

依赖注入的一个重要价值是测试友好。业务层依赖接口后，测试时可以替换具体实现，而不用改业务代码。

例如测试 `NotifyServiceImpl` 时，可以手动构造一个假的发送器注册器，或者在 Spring Boot 测试中使用 Mock Bean 替换依赖。

示例思路：

```java
package io.github.atengk.dependencyinjection.service.impl;

import io.github.atengk.dependencyinjection.dto.SendNotifyRequest;
import io.github.atengk.dependencyinjection.registry.NotifySenderRegistry;
import io.github.atengk.dependencyinjection.sender.NotifyMessage;
import io.github.atengk.dependencyinjection.sender.NotifySender;
import io.github.atengk.dependencyinjection.sender.NotifyType;
import io.github.atengk.dependencyinjection.vo.SendNotifyResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

/**
 * 通知业务测试
 *
 * @author Ateng
 * @since 2026-05-13
 */
class NotifyServiceImplTest {

    @Test
    void shouldSendNotifySuccess() {
        NotifySenderRegistry registry = mock(NotifySenderRegistry.class);
        NotifySender sender = mock(NotifySender.class);

        when(sender.notifyType()).thenReturn(NotifyType.EMAIL);
        when(registry.getSender(NotifyType.EMAIL)).thenReturn(sender);

        NotifyServiceImpl notifyService = new NotifyServiceImpl(registry);
        SendNotifyRequest request = new SendNotifyRequest(
                NotifyType.EMAIL,
                "user@example.com",
                "测试标题",
                "测试内容"
        );

        SendNotifyResponse response = notifyService.send(request);

        Assertions.assertTrue(response.success());
        Assertions.assertEquals(NotifyType.EMAIL, response.notifyType());
        verify(sender, times(1)).send(any(NotifyMessage.class));
    }

}
```

如果项目中使用 Mockito，需要补充测试依赖：

```xml
<!-- Spring Boot Test：包含 JUnit、Mockito、AssertJ 等常用测试组件 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

构造器注入让测试可以直接 `new NotifyServiceImpl(registry)`，不用启动完整 Spring 容器。

## 适用场景

依赖注入适合绝大多数 Spring Boot 组件协作场景。

常见适用场景：

```text
Controller 注入 Service：
- 用户接口调用用户业务
- 订单接口调用订单业务
- 支付接口调用支付业务

Service 注入 Repository：
- 用户业务访问用户仓储
- 订单业务访问订单仓储
- 商品业务访问库存仓储

业务注入多个实现：
- 多通知渠道
- 多支付渠道
- 多文件存储实现
- 多导入解析器
- 多风控规则

配置注入：
- 第三方接口地址
- 默认业务参数
- 开关配置
- 限流阈值

测试替换：
- 替换外部接口
- 替换消息发送器
- 替换远程服务客户端
- 替换数据库访问组件
```

## 不适用场景

依赖注入虽然常用，但不代表所有对象都必须交给 Spring 管理。

不建议交给 Spring 容器管理的对象：

```text
1. 简单 DTO、VO、Command、Query 对象。
2. 每次请求都不同的临时业务数据对象。
3. 纯局部变量对象。
4. 无依赖、无状态、只是一次性使用的普通值对象。
5. 不需要生命周期管理的简单工具返回对象。
```

例如下面这些对象通常不需要注册为 Bean：

```text
UserCreateRequest
OrderCreateCommand
UserAccountVO
Money
Address
NotifyMessage
```

这些对象一般由接口反序列化、构造方法、工厂方法或业务流程临时创建即可。

## 和工厂模式的区别

依赖注入模式和工厂模式都能解决对象创建问题，但侧重点不同。

| 对比项   | 依赖注入模式               | 工厂模式            |
| -------- | -------------------------- | ------------------- |
| 核心关注 | 谁来装配依赖               | 谁来创建对象        |
| 创建方   | Spring 容器                | 工厂类              |
| 使用方式 | 注入 Bean                  | 调用 factory.create |
| 适合对象 | 稳定组件、服务、策略、配置 | 运行时动态创建对象  |
| 生命周期 | 通常由容器管理             | 通常由工厂控制      |

简单理解：

```text
依赖注入适合管理长期存在的组件。
工厂模式适合按参数动态创建对象。
```

在 Spring Boot 中，二者经常结合使用。例如 `CouponReceiveSpecificationFactory` 可以由 Spring 注入到 Service 中，而 Factory 内部负责组合规格对象。

## 和策略模式的关系

依赖注入经常和策略模式一起使用。策略模式负责定义多种算法或业务处理方式，依赖注入负责把这些策略实现装配到业务代码中。

例如通知发送：

```text
策略接口：NotifySender
策略实现：SmsNotifySender、EmailNotifySender、InAppNotifySender
策略注册：NotifySenderRegistry
依赖注入：Spring 自动注入 List<NotifySender>
```

策略模式回答：

```text
不同通知类型怎么发送？
```

依赖注入回答：

```text
这些通知发送器由谁创建、谁装配、谁传给业务对象？
```

二者不是竞争关系，而是互补关系。

## 和单例模式的关系

Spring Bean 默认通常是单例作用域，这和单例模式有关，但二者不是完全相同。

| 对比项   | Spring 单例 Bean | 传统单例模式          |
| -------- | ---------------- | --------------------- |
| 管理方   | Spring 容器      | 类自己管理            |
| 创建方式 | 容器创建         | 私有构造器 + 静态实例 |
| 测试替换 | 容易替换         | 较困难                |
| 依赖管理 | 容器注入         | 自己查找或创建        |
| 推荐程度 | Spring 项目推荐  | Spring 项目中较少手写 |

在 Spring Boot 项目中，不建议到处手写传统单例。更推荐把无状态服务注册为 Spring Bean，由容器统一管理。

## 项目落地建议

在 Spring Boot 项目中使用依赖注入模式时，应优先保证依赖清晰、边界稳定、注入方式一致。

建议：

```text
1. 优先使用构造器注入。
2. 依赖字段尽量声明为 final。
3. 不推荐字段注入。
4. Controller 依赖 Service 接口。
5. Service 依赖 Repository 接口或业务组件接口。
6. 多实现接口使用 List、Map、@Qualifier 或注册器统一管理。
7. 配置项较多时使用 @ConfigurationProperties。
8. 不要把 DTO、VO、普通值对象注册成 Bean。
9. 不要在业务代码中频繁 ApplicationContext.getBean。
10. 避免 Bean 之间形成循环依赖。
```

推荐写法：

```java
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;

    private final ApplicationEventPublisher eventPublisher;

}
```

不推荐写法：

```java
@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderRepository orderRepository;

}
```

更不推荐写法：

```java
OrderRepository orderRepository = ApplicationContextHolder.getBean(OrderRepository.class);
```

直接从容器中取 Bean 会隐藏依赖关系，让代码更难测试和维护。

## 常见问题

### 为什么不推荐字段注入

字段注入虽然少写代码，但缺点明显：

```text
1. 无法使用 final 保证依赖不可变。
2. 单元测试时不方便手动构造对象。
3. 依赖关系隐藏在字段上，不如构造器清晰。
4. 容易掩盖循环依赖。
5. 对象脱离 Spring 容器后不可用。
```

构造器注入可以让依赖一眼可见，也更符合不可变对象设计。

### Bean 找不到怎么办

常见原因：

```text
1. 类没有添加 @Component、@Service、@Repository、@Configuration。
2. 类不在 Spring Boot 启动类扫描路径下。
3. 接口有多个实现，但没有指定 @Qualifier 或 @Primary。
4. 配置条件不满足，例如 @ConditionalOnProperty。
5. 配置属性类没有开启 @ConfigurationPropertiesScan。
```

解决思路：

```text
1. 检查类是否被 Spring 管理。
2. 检查包路径是否在启动类同级或子级。
3. 检查是否存在多个候选 Bean。
4. 检查配置属性是否正确绑定。
5. 检查启动日志中的 Bean 注册信息。
```

### 多个实现注入时如何选择

常见方式有四种：

| 方式             | 适合场景               |
| ---------------- | ---------------------- |
| `@Qualifier`     | 明确指定某个实现       |
| `@Primary`       | 设置默认实现           |
| `List<T>`        | 获取全部实现后自行筛选 |
| `Map<String, T>` | 按 Bean 名称获取实现   |

如果是业务类型选择，例如支付类型、通知类型、文件类型，推荐使用注册器：

```text
List<NotifySender>
 -> NotifySenderRegistry
 -> 按 NotifyType 获取具体实现
```

这样比在 Service 中写大量 `if else` 更清晰。

### 是否所有类都要交给 Spring 管理

不是。只有需要被复用、被注入、被代理、被配置管理或参与生命周期管理的组件，才适合交给 Spring 管理。

适合注册为 Bean：

```text
Service
Repository
Controller
Config
Client
Sender
Handler
Strategy
Factory
Registry
Listener
```

不适合注册为 Bean：

```text
DTO
VO
Entity
Command
Query
Message
普通值对象
临时计算对象
```

## 总结

依赖注入模式是 Spring Boot 项目中最基础的工程模式之一。它把对象创建和依赖装配交给 Spring 容器，让业务代码只依赖抽象接口和稳定组件。

推荐落地方式：

```text
Controller
 -> 注入 Service

Service
 -> 注入 Repository、Registry、Factory、Publisher

Registry
 -> 注入多个 Strategy、Sender、Handler 实现

Properties
 -> 注入 application.yml 配置
```

在真实项目中，依赖注入通常会和策略模式、工厂模式、仓储模式、事件驱动模式一起使用。它本身不直接解决某个业务算法问题，但能让对象协作关系更清晰，代码更容易扩展、替换、测试和维护。
