# 设计模式：桥接模式

桥接模式用于把抽象部分和实现部分分离，使它们可以独立变化。在 JDK21 和 Spring Boot 3 项目中，桥接模式常用于多通知类型和多发送渠道、多文件导出类型和多存储方式、多支付业务和多支付渠道、多报表格式和多投递方式、多设备控制和多厂商驱动等场景。

需要注意：桥接模式关注的是“两个维度独立扩展”。如果只是接口不兼容转换，更适合适配器模式；如果只是根据类型选择一个算法，更适合策略模式；如果需要创建一整套产品族，更适合抽象工厂模式。

## 基础配置

本示例基于 JDK21、Spring Boot 3、Maven 项目。示例包路径统一使用 `io.github.atengk`。

文件位置：`pom.xml`

```xml
<dependencies>
    <!-- Spring Boot Web，用于提供接口验证桥接模式行为 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Hutool 工具类，用于字符串、ID、集合等通用处理 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.8.27</version>
    </dependency>

    <!-- Lombok，简化日志对象、构造方法等样板代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- Spring Boot 测试依赖，用于单元测试验证 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

如果项目使用 Spring Boot 3，建议使用 JDK17 及以上版本。当前文档以 JDK21 为基准，示例代码可以直接用于 Spring Boot 3 项目。

## 核心概念

桥接模式的核心目标是避免两个维度交叉组合导致类爆炸。例如消息业务有“普通通知、营销通知、告警通知”三个类型，发送渠道有“短信、邮件、站内信”三个渠道。如果不用桥接模式，可能需要写 9 个类。

交叉继承结构如下：

```text
普通短信通知
普通邮件通知
普通站内信通知
营销短信通知
营销邮件通知
营销站内信通知
告警短信通知
告警邮件通知
告警站内信通知
```

桥接模式会把“通知类型”和“发送渠道”拆成两个独立维度：

```text
通知类型维度：普通通知、营销通知、告警通知
发送渠道维度：短信渠道、邮件渠道、站内信渠道
```

常见角色如下：

| 角色                | 说明                           |
| ------------------- | ------------------------------ |
| Abstraction         | 抽象部分，定义高层业务行为     |
| RefinedAbstraction  | 扩展抽象部分，表示不同业务类型 |
| Implementor         | 实现部分接口，定义底层实现能力 |
| ConcreteImplementor | 具体实现部分，表示不同底层实现 |
| Client              | 调用方，组合抽象部分和实现部分 |

在 Spring Boot 项目中，常见优先级通常是：

```text
Spring Bean 桥接 > 普通 Java 桥接 > 多层 if else / 类爆炸
```

桥接模式适合两个维度都可能扩展的场景。如果只有一个维度变化，策略模式或工厂方法通常更简单。

## 普通 Java 桥接模式

普通 Java 桥接模式适合不依赖 Spring 容器的双维度组合场景。下面以消息通知为例，通知类型和发送渠道是两个独立变化维度。

整体关系如下：

```text
MessageNotification
    -> MessageSender

NormalNotification
MarketingNotification
AlertNotification

SmsMessageSender
EmailMessageSender
```

通知类型负责组织业务内容，发送渠道负责具体发送方式。

### 文件结构

```text
src/main/java/io/github/atengk/design/bridge/simple/
├── MessageSender.java
├── SmsMessageSender.java
├── EmailMessageSender.java
├── MessageNotification.java
├── NormalNotification.java
├── MarketingNotification.java
└── AlertNotification.java
```

文件位置：`src/main/java/io/github/atengk/design/bridge/simple/MessageSender.java`

下面是消息发送渠道接口，也就是桥接模式中的实现部分。

```java
package io.github.atengk.design.bridge.simple;

/**
 * 消息发送器
 *
 * @author Ateng
 * @since 2026-04-30
 */
public interface MessageSender {

    /**
     * 发送消息
     *
     * @param receiver 接收人
     * @param title    标题
     * @param content  内容
     * @return 发送结果
     */
    String send(String receiver, String title, String content);

    /**
     * 获取发送渠道
     *
     * @return 发送渠道
     */
    String channel();
}
```

文件位置：`src/main/java/io/github/atengk/design/bridge/simple/SmsMessageSender.java`

下面是短信发送器实现。

```java
package io.github.atengk.design.bridge.simple;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 短信消息发送器
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
public class SmsMessageSender implements MessageSender {

    /**
     * 发送消息
     *
     * @param receiver 接收人
     * @param title    标题
     * @param content  内容
     * @return 发送结果
     */
    @Override
    public String send(String receiver, String title, String content) {
        if (StrUtil.hasBlank(receiver, content)) {
            log.warn("短信发送失败，接收人或内容为空");
            throw new IllegalArgumentException("短信接收人和内容不能为空");
        }

        String bizId = "SMS" + IdUtil.getSnowflakeNextId();
        log.info("短信发送成功，接收人：{}，标题：{}，业务ID：{}", receiver, title, bizId);
        return bizId;
    }

    /**
     * 获取发送渠道
     *
     * @return 发送渠道
     */
    @Override
    public String channel() {
        return "sms";
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/bridge/simple/EmailMessageSender.java`

下面是邮件发送器实现。

```java
package io.github.atengk.design.bridge.simple;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 邮件消息发送器
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
public class EmailMessageSender implements MessageSender {

    /**
     * 发送消息
     *
     * @param receiver 接收人
     * @param title    标题
     * @param content  内容
     * @return 发送结果
     */
    @Override
    public String send(String receiver, String title, String content) {
        if (StrUtil.hasBlank(receiver, title, content)) {
            log.warn("邮件发送失败，接收人、标题或内容为空");
            throw new IllegalArgumentException("邮件接收人、标题和内容不能为空");
        }

        String messageId = "EMAIL" + IdUtil.getSnowflakeNextId();
        log.info("邮件发送成功，接收人：{}，标题：{}，消息ID：{}", receiver, title, messageId);
        return messageId;
    }

    /**
     * 获取发送渠道
     *
     * @return 发送渠道
     */
    @Override
    public String channel() {
        return "email";
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/bridge/simple/MessageNotification.java`

下面是消息通知抽象类。它持有 `MessageSender`，把具体发送动作委托给发送器。

```java
package io.github.atengk.design.bridge.simple;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 消息通知抽象类
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
public abstract class MessageNotification {

    protected final MessageSender messageSender;

    /**
     * 创建消息通知
     *
     * @param messageSender 消息发送器
     */
    protected MessageNotification(MessageSender messageSender) {
        if (messageSender == null) {
            throw new IllegalArgumentException("消息发送器不能为空");
        }

        this.messageSender = messageSender;
    }

    /**
     * 发送通知
     *
     * @param receiver 接收人
     * @param content  原始内容
     * @return 发送结果
     */
    public String notify(String receiver, String content) {
        if (StrUtil.hasBlank(receiver, content)) {
            log.warn("发送通知失败，接收人或内容为空");
            throw new IllegalArgumentException("接收人和通知内容不能为空");
        }

        String title = buildTitle();
        String formattedContent = buildContent(content);

        log.info("准备发送通知，通知类型：{}，发送渠道：{}，接收人：{}",
                notificationType(), messageSender.channel(), receiver);

        return messageSender.send(receiver, title, formattedContent);
    }

    /**
     * 构建通知标题
     *
     * @return 通知标题
     */
    protected abstract String buildTitle();

    /**
     * 构建通知内容
     *
     * @param content 原始内容
     * @return 通知内容
     */
    protected abstract String buildContent(String content);

    /**
     * 获取通知类型
     *
     * @return 通知类型
     */
    protected abstract String notificationType();
}
```

文件位置：`src/main/java/io/github/atengk/design/bridge/simple/NormalNotification.java`

下面是普通通知类型。

```java
package io.github.atengk.design.bridge.simple;

import cn.hutool.core.util.StrUtil;

/**
 * 普通通知
 *
 * @author Ateng
 * @since 2026-04-30
 */
public class NormalNotification extends MessageNotification {

    /**
     * 创建普通通知
     *
     * @param messageSender 消息发送器
     */
    public NormalNotification(MessageSender messageSender) {
        super(messageSender);
    }

    /**
     * 构建通知标题
     *
     * @return 通知标题
     */
    @Override
    protected String buildTitle() {
        return "普通通知";
    }

    /**
     * 构建通知内容
     *
     * @param content 原始内容
     * @return 通知内容
     */
    @Override
    protected String buildContent(String content) {
        return StrUtil.format("[普通通知] {}", content);
    }

    /**
     * 获取通知类型
     *
     * @return 通知类型
     */
    @Override
    protected String notificationType() {
        return "normal";
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/bridge/simple/MarketingNotification.java`

下面是营销通知类型。

```java
package io.github.atengk.design.bridge.simple;

import cn.hutool.core.util.StrUtil;

/**
 * 营销通知
 *
 * @author Ateng
 * @since 2026-04-30
 */
public class MarketingNotification extends MessageNotification {

    /**
     * 创建营销通知
     *
     * @param messageSender 消息发送器
     */
    public MarketingNotification(MessageSender messageSender) {
        super(messageSender);
    }

    /**
     * 构建通知标题
     *
     * @return 通知标题
     */
    @Override
    protected String buildTitle() {
        return "营销活动通知";
    }

    /**
     * 构建通知内容
     *
     * @param content 原始内容
     * @return 通知内容
     */
    @Override
    protected String buildContent(String content) {
        return StrUtil.format("[营销活动] {}。退订请回复TD", content);
    }

    /**
     * 获取通知类型
     *
     * @return 通知类型
     */
    @Override
    protected String notificationType() {
        return "marketing";
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/bridge/simple/AlertNotification.java`

下面是告警通知类型。

```java
package io.github.atengk.design.bridge.simple;

import cn.hutool.core.util.StrUtil;

/**
 * 告警通知
 *
 * @author Ateng
 * @since 2026-04-30
 */
public class AlertNotification extends MessageNotification {

    /**
     * 创建告警通知
     *
     * @param messageSender 消息发送器
     */
    public AlertNotification(MessageSender messageSender) {
        super(messageSender);
    }

    /**
     * 构建通知标题
     *
     * @return 通知标题
     */
    @Override
    protected String buildTitle() {
        return "系统告警通知";
    }

    /**
     * 构建通知内容
     *
     * @param content 原始内容
     * @return 通知内容
     */
    @Override
    protected String buildContent(String content) {
        return StrUtil.format("[重要告警] {}，请立即处理", content);
    }

    /**
     * 获取通知类型
     *
     * @return 通知类型
     */
    @Override
    protected String notificationType() {
        return "alert";
    }
}
```

使用方式：

```java
MessageSender smsSender = new SmsMessageSender();
MessageNotification alertNotification = new AlertNotification(smsSender);

String result = alertNotification.notify("13800138000", "订单服务响应时间超过阈值");
```

如果想把告警通知从短信切换为邮件，只需要替换实现部分：

```java
MessageSender emailSender = new EmailMessageSender();
MessageNotification alertNotification = new AlertNotification(emailSender);
```

通知类型和发送渠道可以独立扩展，避免为每一种组合都创建一个类。

## Spring Boot 桥接模式

Spring Boot 项目中更常见的桥接写法，是把两个维度都交给 Spring 管理：一个维度作为抽象业务处理器，另一个维度作为实现接口。下面以“通知类型 + 发送渠道”为例。

整体流程如下：

```text
Controller
    -> NotificationBridgeService
        -> NotificationContentBuilder  通知内容维度
        -> NotificationSender          发送渠道维度
```

示例支持两个维度：

```text
通知类型：normal、marketing、alert
发送渠道：sms、email、site
```

这样总共可以支持 3 × 3 = 9 种组合，但代码只需要维护 3 个内容构建器和 3 个发送器。

### 文件结构

```text
src/main/java/io/github/atengk/design/
├── BridgeApplication.java
├── builder/
│   ├── NotificationContentBuilder.java
│   ├── NormalContentBuilder.java
│   ├── MarketingContentBuilder.java
│   └── AlertContentBuilder.java
├── controller/
│   └── NotificationController.java
├── dto/
│   ├── NotificationRequest.java
│   ├── NotificationContent.java
│   └── NotificationResponse.java
├── sender/
│   ├── NotificationSender.java
│   ├── SmsNotificationSender.java
│   ├── EmailNotificationSender.java
│   └── SiteNotificationSender.java
└── service/
    ├── NotificationBridgeService.java
    └── impl/
        └── NotificationBridgeServiceImpl.java
```

文件位置：`src/main/java/io/github/atengk/design/BridgeApplication.java`

下面是 Spring Boot 启动类。

```java
package io.github.atengk.design;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 桥接模式示例启动类
 *
 * @author Ateng
 * @since 2026-04-30
 */
@SpringBootApplication
public class BridgeApplication {

    /**
     * 应用启动入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(BridgeApplication.class, args);
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/dto/NotificationRequest.java`

下面是通知请求对象，包含通知类型和发送渠道两个维度。

```java
package io.github.atengk.design.dto;

/**
 * 通知请求
 *
 * @param type     通知类型
 * @param channel  发送渠道
 * @param receiver 接收人
 * @param content  原始内容
 * @author Ateng
 * @since 2026-04-30
 */
public record NotificationRequest(
        String type,
        String channel,
        String receiver,
        String content
) {
}
```

文件位置：`src/main/java/io/github/atengk/design/dto/NotificationContent.java`

下面是通知内容对象，由内容构建器生成，再交给发送器发送。

```java
package io.github.atengk.design.dto;

/**
 * 通知内容
 *
 * @param title   标题
 * @param content 内容
 * @author Ateng
 * @since 2026-04-30
 */
public record NotificationContent(String title, String content) {
}
```

文件位置：`src/main/java/io/github/atengk/design/dto/NotificationResponse.java`

下面是通知响应对象。

```java
package io.github.atengk.design.dto;

/**
 * 通知响应
 *
 * @param type     通知类型
 * @param channel  发送渠道
 * @param receiver 接收人
 * @param bizId    业务ID
 * @param message  响应消息
 * @author Ateng
 * @since 2026-04-30
 */
public record NotificationResponse(
        String type,
        String channel,
        String receiver,
        String bizId,
        String message
) {
}
```

文件位置：`src/main/java/io/github/atengk/design/builder/NotificationContentBuilder.java`

下面是通知内容构建器接口，表示通知类型维度。

```java
package io.github.atengk.design.builder;

import io.github.atengk.design.dto.NotificationContent;

/**
 * 通知内容构建器
 *
 * @author Ateng
 * @since 2026-04-30
 */
public interface NotificationContentBuilder {

    /**
     * 获取支持的通知类型
     *
     * @return 通知类型
     */
    String supportType();

    /**
     * 构建通知内容
     *
     * @param rawContent 原始内容
     * @return 通知内容
     */
    NotificationContent build(String rawContent);
}
```

文件位置：`src/main/java/io/github/atengk/design/builder/NormalContentBuilder.java`

下面是普通通知内容构建器。

```java
package io.github.atengk.design.builder;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.design.dto.NotificationContent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 普通通知内容构建器
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
public class NormalContentBuilder implements NotificationContentBuilder {

    /**
     * 获取支持的通知类型
     *
     * @return 通知类型
     */
    @Override
    public String supportType() {
        return "normal";
    }

    /**
     * 构建通知内容
     *
     * @param rawContent 原始内容
     * @return 通知内容
     */
    @Override
    public NotificationContent build(String rawContent) {
        if (StrUtil.isBlank(rawContent)) {
            log.warn("构建普通通知失败，内容为空");
            throw new IllegalArgumentException("通知内容不能为空");
        }

        return new NotificationContent("普通通知", StrUtil.format("[普通通知] {}", rawContent));
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/builder/MarketingContentBuilder.java`

下面是营销通知内容构建器。

```java
package io.github.atengk.design.builder;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.design.dto.NotificationContent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 营销通知内容构建器
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
public class MarketingContentBuilder implements NotificationContentBuilder {

    /**
     * 获取支持的通知类型
     *
     * @return 通知类型
     */
    @Override
    public String supportType() {
        return "marketing";
    }

    /**
     * 构建通知内容
     *
     * @param rawContent 原始内容
     * @return 通知内容
     */
    @Override
    public NotificationContent build(String rawContent) {
        if (StrUtil.isBlank(rawContent)) {
            log.warn("构建营销通知失败，内容为空");
            throw new IllegalArgumentException("通知内容不能为空");
        }

        return new NotificationContent("营销活动通知", StrUtil.format("[营销活动] {}。退订请回复TD", rawContent));
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/builder/AlertContentBuilder.java`

下面是告警通知内容构建器。

```java
package io.github.atengk.design.builder;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.design.dto.NotificationContent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 告警通知内容构建器
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
public class AlertContentBuilder implements NotificationContentBuilder {

    /**
     * 获取支持的通知类型
     *
     * @return 通知类型
     */
    @Override
    public String supportType() {
        return "alert";
    }

    /**
     * 构建通知内容
     *
     * @param rawContent 原始内容
     * @return 通知内容
     */
    @Override
    public NotificationContent build(String rawContent) {
        if (StrUtil.isBlank(rawContent)) {
            log.warn("构建告警通知失败，内容为空");
            throw new IllegalArgumentException("通知内容不能为空");
        }

        return new NotificationContent("系统告警通知", StrUtil.format("[重要告警] {}，请立即处理", rawContent));
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/sender/NotificationSender.java`

下面是通知发送器接口，表示发送渠道维度。

```java
package io.github.atengk.design.sender;

import io.github.atengk.design.dto.NotificationContent;

/**
 * 通知发送器
 *
 * @author Ateng
 * @since 2026-04-30
 */
public interface NotificationSender {

    /**
     * 获取支持的发送渠道
     *
     * @return 发送渠道
     */
    String supportChannel();

    /**
     * 发送通知
     *
     * @param receiver 接收人
     * @param content  通知内容
     * @return 业务ID
     */
    String send(String receiver, NotificationContent content);
}
```

文件位置：`src/main/java/io/github/atengk/design/sender/SmsNotificationSender.java`

下面是短信通知发送器。

```java
package io.github.atengk.design.sender;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.design.dto.NotificationContent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 短信通知发送器
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
public class SmsNotificationSender implements NotificationSender {

    /**
     * 获取支持的发送渠道
     *
     * @return 发送渠道
     */
    @Override
    public String supportChannel() {
        return "sms";
    }

    /**
     * 发送通知
     *
     * @param receiver 接收人
     * @param content  通知内容
     * @return 业务ID
     */
    @Override
    public String send(String receiver, NotificationContent content) {
        if (StrUtil.isBlank(receiver) || content == null || StrUtil.isBlank(content.content())) {
            log.warn("短信通知发送失败，接收人或内容为空");
            throw new IllegalArgumentException("短信接收人和内容不能为空");
        }

        String bizId = "SMS" + IdUtil.getSnowflakeNextId();
        log.info("短信通知发送成功，接收人：{}，标题：{}，业务ID：{}", receiver, content.title(), bizId);
        return bizId;
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/sender/EmailNotificationSender.java`

下面是邮件通知发送器。

```java
package io.github.atengk.design.sender;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.design.dto.NotificationContent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 邮件通知发送器
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
public class EmailNotificationSender implements NotificationSender {

    /**
     * 获取支持的发送渠道
     *
     * @return 发送渠道
     */
    @Override
    public String supportChannel() {
        return "email";
    }

    /**
     * 发送通知
     *
     * @param receiver 接收人
     * @param content  通知内容
     * @return 业务ID
     */
    @Override
    public String send(String receiver, NotificationContent content) {
        if (StrUtil.isBlank(receiver) || content == null || StrUtil.hasBlank(content.title(), content.content())) {
            log.warn("邮件通知发送失败，接收人、标题或内容为空");
            throw new IllegalArgumentException("邮件接收人、标题和内容不能为空");
        }

        String messageId = "EMAIL" + IdUtil.getSnowflakeNextId();
        log.info("邮件通知发送成功，接收人：{}，标题：{}，消息ID：{}", receiver, content.title(), messageId);
        return messageId;
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/sender/SiteNotificationSender.java`

下面是站内信通知发送器。

```java
package io.github.atengk.design.sender;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.design.dto.NotificationContent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 站内信通知发送器
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
public class SiteNotificationSender implements NotificationSender {

    /**
     * 获取支持的发送渠道
     *
     * @return 发送渠道
     */
    @Override
    public String supportChannel() {
        return "site";
    }

    /**
     * 发送通知
     *
     * @param receiver 接收人
     * @param content  通知内容
     * @return 业务ID
     */
    @Override
    public String send(String receiver, NotificationContent content) {
        if (StrUtil.isBlank(receiver) || content == null || StrUtil.isBlank(content.content())) {
            log.warn("站内信通知发送失败，接收人或内容为空");
            throw new IllegalArgumentException("站内信接收人和内容不能为空");
        }

        String noticeId = "SITE" + IdUtil.getSnowflakeNextId();
        log.info("站内信通知发送成功，接收人：{}，标题：{}，通知ID：{}", receiver, content.title(), noticeId);
        return noticeId;
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/service/NotificationBridgeService.java`

下面是通知桥接服务接口。

```java
package io.github.atengk.design.service;

import io.github.atengk.design.dto.NotificationRequest;
import io.github.atengk.design.dto.NotificationResponse;

/**
 * 通知桥接服务
 *
 * @author Ateng
 * @since 2026-04-30
 */
public interface NotificationBridgeService {

    /**
     * 发送通知
     *
     * @param request 通知请求
     * @return 通知响应
     */
    NotificationResponse notify(NotificationRequest request);
}
```

文件位置：`src/main/java/io/github/atengk/design/service/impl/NotificationBridgeServiceImpl.java`

下面是通知桥接服务实现。它将通知类型维度和发送渠道维度桥接起来。

```java
package io.github.atengk.design.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.design.builder.NotificationContentBuilder;
import io.github.atengk.design.dto.NotificationContent;
import io.github.atengk.design.dto.NotificationRequest;
import io.github.atengk.design.dto.NotificationResponse;
import io.github.atengk.design.sender.NotificationSender;
import io.github.atengk.design.service.NotificationBridgeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 通知桥接服务实现
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Service
public class NotificationBridgeServiceImpl implements NotificationBridgeService {

    private final Map<String, NotificationContentBuilder> builderMap;
    private final Map<String, NotificationSender> senderMap;

    /**
     * 创建通知桥接服务
     *
     * @param builders 通知内容构建器列表
     * @param senders  通知发送器列表
     */
    public NotificationBridgeServiceImpl(List<NotificationContentBuilder> builders,
                                         List<NotificationSender> senders) {
        if (CollUtil.isEmpty(builders)) {
            log.warn("通知内容构建器列表为空");
            this.builderMap = Map.of();
        } else {
            this.builderMap = builders.stream()
                    .collect(Collectors.toUnmodifiableMap(
                            builder -> StrUtil.trim(builder.supportType()).toLowerCase(),
                            Function.identity()
                    ));
        }

        if (CollUtil.isEmpty(senders)) {
            log.warn("通知发送器列表为空");
            this.senderMap = Map.of();
        } else {
            this.senderMap = senders.stream()
                    .collect(Collectors.toUnmodifiableMap(
                            sender -> StrUtil.trim(sender.supportChannel()).toLowerCase(),
                            Function.identity()
                    ));
        }

        log.info("初始化通知桥接服务，支持类型：{}，支持渠道：{}", builderMap.keySet(), senderMap.keySet());
    }

    /**
     * 发送通知
     *
     * @param request 通知请求
     * @return 通知响应
     */
    @Override
    public NotificationResponse notify(NotificationRequest request) {
        validateRequest(request);

        String type = StrUtil.trim(request.type()).toLowerCase();
        String channel = StrUtil.trim(request.channel()).toLowerCase();

        NotificationContentBuilder builder = builderMap.get(type);
        if (builder == null) {
            log.warn("发送通知失败，不支持的通知类型：{}", request.type());
            throw new IllegalArgumentException("不支持的通知类型：" + request.type());
        }

        NotificationSender sender = senderMap.get(channel);
        if (sender == null) {
            log.warn("发送通知失败，不支持的发送渠道：{}", request.channel());
            throw new IllegalArgumentException("不支持的发送渠道：" + request.channel());
        }

        NotificationContent notificationContent = builder.build(request.content());
        String bizId = sender.send(request.receiver(), notificationContent);

        log.info("通知发送完成，通知类型：{}，发送渠道：{}，接收人：{}，业务ID：{}",
                type, channel, request.receiver(), bizId);

        return new NotificationResponse(
                type,
                channel,
                request.receiver(),
                bizId,
                "发送成功"
        );
    }

    /**
     * 校验通知请求
     *
     * @param request 通知请求
     */
    private void validateRequest(NotificationRequest request) {
        if (request == null) {
            log.warn("发送通知失败，请求参数为空");
            throw new IllegalArgumentException("请求参数不能为空");
        }

        if (StrUtil.hasBlank(request.type(), request.channel(), request.receiver(), request.content())) {
            log.warn("发送通知失败，通知类型、发送渠道、接收人或内容为空");
            throw new IllegalArgumentException("通知类型、发送渠道、接收人和内容不能为空");
        }
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/controller/NotificationController.java`

下面是通知接口，用于验证桥接模式效果。

```java
package io.github.atengk.design.controller;

import io.github.atengk.design.dto.NotificationRequest;
import io.github.atengk.design.dto.NotificationResponse;
import io.github.atengk.design.service.NotificationBridgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 通知控制器
 *
 * @author Ateng
 * @since 2026-04-30
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/bridge/notification")
public class NotificationController {

    private final NotificationBridgeService notificationBridgeService;

    /**
     * 发送通知
     *
     * @param type     通知类型
     * @param channel  发送渠道
     * @param receiver 接收人
     * @param content  原始内容
     * @return 通知响应
     */
    @PostMapping("/send")
    public NotificationResponse send(@RequestParam String type,
                                     @RequestParam String channel,
                                     @RequestParam String receiver,
                                     @RequestParam String content) {
        NotificationRequest request = new NotificationRequest(
                type,
                channel,
                receiver,
                content
        );

        return notificationBridgeService.notify(request);
    }
}
```

接口调用示例：

```bash
curl -X POST "http://localhost:8080/bridge/notification/send?type=alert&channel=sms&receiver=13800138000&content=订单服务响应时间超过阈值"

curl -X POST "http://localhost:8080/bridge/notification/send?type=marketing&channel=email&receiver=ateng@example.com&content=限时优惠活动开始"

curl -X POST "http://localhost:8080/bridge/notification/send?type=normal&channel=site&receiver=10001&content=你的订单已创建"
```

告警短信可能返回：

```json
{
  "type": "alert",
  "channel": "sms",
  "receiver": "13800138000",
  "bizId": "SMS2019776866538487808",
  "message": "发送成功"
}
```

这种方式的优点是通知类型和发送渠道可以独立扩展。新增一种通知类型，不需要修改发送渠道；新增一种发送渠道，也不需要修改通知类型。

## 扩展一个新通知类型

在桥接模式中，扩展抽象维度通常只需要新增一个实现类。下面以“审批通知”为例，新增通知类型 `approval`。

文件位置：`src/main/java/io/github/atengk/design/builder/ApprovalContentBuilder.java`

下面是审批通知内容构建器。新增后会自动加入桥接服务的内容构建器映射中。

```java
package io.github.atengk.design.builder;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.design.dto.NotificationContent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 审批通知内容构建器
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
public class ApprovalContentBuilder implements NotificationContentBuilder {

    /**
     * 获取支持的通知类型
     *
     * @return 通知类型
     */
    @Override
    public String supportType() {
        return "approval";
    }

    /**
     * 构建通知内容
     *
     * @param rawContent 原始内容
     * @return 通知内容
     */
    @Override
    public NotificationContent build(String rawContent) {
        if (StrUtil.isBlank(rawContent)) {
            log.warn("构建审批通知失败，内容为空");
            throw new IllegalArgumentException("通知内容不能为空");
        }

        return new NotificationContent("审批待处理通知", StrUtil.format("[审批待处理] {}", rawContent));
    }
}
```

调用示例：

```bash
curl -X POST "http://localhost:8080/bridge/notification/send?type=approval&channel=site&receiver=10001&content=你有一条请假审批待处理"
```

新增审批通知后，短信、邮件、站内信三个发送渠道都可以直接复用。

## 扩展一个新发送渠道

扩展实现维度也只需要新增一个实现类。下面以企业微信通知渠道为例，新增发送渠道 `wecom`。

文件位置：`src/main/java/io/github/atengk/design/sender/WecomNotificationSender.java`

下面是企业微信通知发送器。新增后会自动加入桥接服务的发送器映射中。

```java
package io.github.atengk.design.sender;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.design.dto.NotificationContent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 企业微信通知发送器
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
public class WecomNotificationSender implements NotificationSender {

    /**
     * 获取支持的发送渠道
     *
     * @return 发送渠道
     */
    @Override
    public String supportChannel() {
        return "wecom";
    }

    /**
     * 发送通知
     *
     * @param receiver 接收人
     * @param content  通知内容
     * @return 业务ID
     */
    @Override
    public String send(String receiver, NotificationContent content) {
        if (StrUtil.isBlank(receiver) || content == null || StrUtil.hasBlank(content.title(), content.content())) {
            log.warn("企业微信通知发送失败，接收人、标题或内容为空");
            throw new IllegalArgumentException("企业微信接收人、标题和内容不能为空");
        }

        String msgId = "WECOM" + IdUtil.getSnowflakeNextId();
        log.info("企业微信通知发送成功，接收人：{}，标题：{}，消息ID：{}", receiver, content.title(), msgId);
        return msgId;
    }
}
```

调用示例：

```bash
curl -X POST "http://localhost:8080/bridge/notification/send?type=alert&channel=wecom&receiver=ateng&content=支付服务异常"
```

新增企业微信渠道后，普通通知、营销通知、告警通知、审批通知都可以直接使用该渠道。

## 桥接模式和策略模式的区别

桥接模式和策略模式都可能表现为“接口 + 多个实现 + 上下文选择”，但二者关注点不同。

| 对比项   | 桥接模式             | 策略模式                 |
| -------- | -------------------- | ------------------------ |
| 核心目的 | 拆分两个独立变化维度 | 从多个算法中选择一种执行 |
| 维度数量 | 通常至少两个维度     | 通常一个变化维度         |
| 组合关系 | 抽象部分持有实现部分 | 上下文选择一个策略       |
| 典型场景 | 通知类型 × 发送渠道  | 优惠算法、计费算法       |
| 扩展收益 | 避免组合类爆炸       | 避免算法分支判断         |

简单理解：

```text
桥接模式：两个维度都在变，需要拆开组合。
策略模式：一个维度在变，需要选择一个算法。
```

通知类型和发送渠道都可能扩展，适合桥接模式。订单优惠从满减、折扣、新人优惠中选择一种，适合策略模式。

## 桥接模式和适配器模式的区别

桥接模式和适配器模式都通过接口隔离具体实现，但意图不同。

| 对比项           | 桥接模式               | 适配器模式                |
| ---------------- | ---------------------- | ------------------------- |
| 核心目的         | 拆分抽象和实现两个维度 | 转换不兼容接口            |
| 使用时机         | 设计初期主动拆分维度   | 接入已有接口时被动适配    |
| 关注点           | 独立扩展               | 接口兼容                  |
| 典型场景         | 通知类型 × 发送渠道    | 旧短信 SDK 转统一短信接口 |
| 是否强调产品组合 | 强调组合关系           | 不强调                    |

简单理解：

```text
桥接模式：原本就设计成两个维度独立变化。
适配器模式：已有接口不合适，包一层转换成目标接口。
```

如果系统一开始就知道通知类型和发送渠道会分别扩展，适合桥接模式。如果已经有一个旧短信 SDK，但接口和系统不兼容，适合适配器模式。

## 桥接模式和抽象工厂模式的关系

桥接模式和抽象工厂模式不是互斥关系。桥接模式解决结构解耦问题，抽象工厂模式解决对象族创建问题。

例如多云文件处理场景中：

```text
桥接模式：文件业务类型 × 存储实现方式
抽象工厂模式：根据云厂商创建对象存储、短信、MQ 一整套客户端
```

二者可以组合使用：

```text
ReportExporter
    -> StorageClient

StorageClient 可以由 CloudResourceFactory 创建
```

简单理解：

```text
桥接模式：怎么把两个维度组合起来。
抽象工厂模式：怎么创建一整套相关对象。
```

如果重点是避免“通知类型 × 发送渠道”的组合类爆炸，使用桥接模式。如果重点是保证“阿里云存储 + 阿里云短信 + 阿里云MQ”来自同一产品族，使用抽象工厂模式。

## 验证方式

启动 Spring Boot 项目：

```bash
mvn spring-boot:run
```

执行告警短信通知：

```bash
curl -X POST "http://localhost:8080/bridge/notification/send?type=alert&channel=sms&receiver=13800138000&content=订单服务响应时间超过阈值"
```

执行营销邮件通知：

```bash
curl -X POST "http://localhost:8080/bridge/notification/send?type=marketing&channel=email&receiver=ateng@example.com&content=限时优惠活动开始"
```

执行普通站内信通知：

```bash
curl -X POST "http://localhost:8080/bridge/notification/send?type=normal&channel=site&receiver=10001&content=你的订单已创建"
```

如果桥接模式正常，可以看到类似日志：

```text
初始化通知桥接服务，支持类型：[normal, marketing, alert]，支持渠道：[sms, email, site]
短信通知发送成功，接收人：13800138000，标题：系统告警通知，业务ID：SMS2019776866538487808
通知发送完成，通知类型：alert，发送渠道：sms，接收人：13800138000，业务ID：SMS2019776866538487808
```

执行不支持的通知类型：

```bash
curl -X POST "http://localhost:8080/bridge/notification/send?type=unknown&channel=sms&receiver=13800138000&content=测试内容"
```

异常日志示例：

```text
发送通知失败，不支持的通知类型：unknown
```

执行不支持的发送渠道：

```bash
curl -X POST "http://localhost:8080/bridge/notification/send?type=alert&channel=unknown&receiver=13800138000&content=测试内容"
```

异常日志示例：

```text
发送通知失败，不支持的发送渠道：unknown
```

实际项目中建议结合全局异常处理器，将业务异常转换成统一响应结构。

## 注意事项

桥接模式适合两个维度都需要独立扩展的场景，不适合只有一个维度变化的简单分发逻辑。过度使用桥接模式会让类数量变多，反而降低可读性。

适合使用桥接模式的场景：

```text
通知类型 × 发送渠道
报表类型 × 导出格式
文件类型 × 存储方式
支付业务 × 支付渠道
设备操作 × 厂商驱动
消息内容 × 投递通道
```

不太适合使用桥接模式的场景：

```text
只有一个变化维度
只有一两个固定实现
对象组合关系不稳定
用普通策略模式即可解决
为了模式强行拆分接口
```

不要把两个维度继续写死在一个类名中。

不推荐写法：

```java
public class AlertSmsNotification {
}

public class AlertEmailNotification {
}

public class MarketingSmsNotification {
}
```

推荐拆成两个维度：

```java
public class AlertContentBuilder implements NotificationContentBuilder {
}

public class SmsNotificationSender implements NotificationSender {
}
```

桥接模式中的两个维度要职责清晰。通知内容构建器只负责构建内容，发送器只负责发送，不要互相侵入职责。

不推荐在发送器中写通知类型判断：

```java
public String send(String receiver, NotificationContent content) {
    if ("alert".equals(type)) {
        // 构建告警内容
    }
    return doSend(receiver, content);
}
```

推荐让内容构建器负责内容，让发送器负责发送：

```java
NotificationContent notificationContent = builder.build(request.content());
String bizId = sender.send(request.receiver(), notificationContent);
```

Spring Bean 默认是单例，桥接实现类中不要保存请求级状态。

错误示例：

```java
private String currentReceiver;
private String currentContent;
private String currentBizId;
```

推荐使用方法参数和局部变量：

```java
public String send(String receiver, NotificationContent content) {
    String bizId = "SMS" + IdUtil.getSnowflakeNextId();
    return bizId;
}
```

如果发送渠道是真实外部服务，例如短信、邮件、企业微信、钉钉、站内信，需要额外考虑超时、重试、限流、幂等、模板审核、内容脱敏和失败补偿。桥接模式只解决结构解耦问题，不自动保证外部调用可靠性。

生产环境中，通知发送通常还需要：

```text
发送记录表
通知模板表
失败重试任务
渠道降级策略
频率限制
敏感信息脱敏
发送结果回调
消息队列异步发送
```

如果桥接后的组合结果需要权限控制，例如某些通知类型不能走某些渠道，可以在桥接服务中增加组合规则校验。

示例规则：

```text
营销通知不允许走站内信
告警通知必须支持短信或企业微信
验证码通知不允许走邮件
```

规则复杂时，可以单独抽出 `NotificationBridgeRuleService`，不要把所有规则塞进 Controller。

## 总结

在 JDK21 和 Spring Boot 3 项目中，桥接模式的实践重点是把两个独立变化维度拆开，通过组合方式建立连接，避免类数量随着组合数量爆炸式增长。

普通 Java 桥接模式适合理解抽象部分和实现部分的分离。Spring Boot 项目中更推荐使用“业务维度接口 + 实现维度接口 + 桥接服务组合”的结构。对于通知类型和发送渠道、报表类型和导出格式、文件类型和存储方式等场景，桥接模式可以让两个维度独立扩展，减少重复类和复杂分支。

桥接模式不是为了替代所有接口分发，而是为了处理“两个维度都在变化，并且需要自由组合”的场景。实际落地时，需要控制两个维度的职责边界，并结合规则校验、外部调用可靠性和异常补偿机制，才能让结构解耦真正服务于业务扩展。
