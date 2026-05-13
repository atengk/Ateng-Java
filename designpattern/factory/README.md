# 简单工厂模式

简单工厂模式属于 Java 项目常用扩展模式，不属于 GoF 23 种设计模式，但在 Java 和 Spring Boot 项目中非常常见。它的核心作用是把对象创建逻辑集中到一个工厂类中，调用方只需要传入类型标识，由工厂返回对应对象。当前设计模式总览中也将 `factory` 作为创建型模式扩展模块单独保留。

## 基础配置

本示例基于 JDK 21、Spring Boot 3、Maven、Hutool、Lombok 编写。示例场景是“消息通知发送”，系统根据请求中的通知渠道 `EMAIL`、`SMS`、`DING_TALK` 自动选择对应的发送器。

文件位置：`pom.xml`

```xml
<dependencies>
    <!-- Spring Boot Web，用于提供简单工厂模式验证接口 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Spring Boot Validation，用于请求参数校验 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- Hutool 工具类，用于字符串、集合、对象、JSON 等常用处理 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.8.36</version>
    </dependency>

    <!-- Lombok，用于减少构造方法、getter、setter 和日志样板代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- Spring Boot 测试依赖，用于验证工厂选择逻辑 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

文件位置：`src/main/resources/application.yml`

```yaml
server:
  port: 8080

demo:
  factory:
    # 邮件通知开关
    email-enabled: true
    # 短信通知开关
    sms-enabled: true
    # 钉钉通知开关
    ding-talk-enabled: true
```

## 模式说明

简单工厂模式主要解决“根据类型创建对象”的问题。调用方不直接 `new EmailSender()`、`new SmsSender()`，而是通过工厂获取对象。

它适合以下场景：

```text
对象类型数量不多
创建逻辑比较简单
调用方不想关心具体实现类
根据请求类型、配置类型、枚举类型选择不同处理器
```

在 Spring Boot 项目中，简单工厂常见于：

```text
根据支付渠道选择支付处理器
根据通知渠道选择消息发送器
根据文件类型选择文件解析器
根据导出类型选择导出服务
根据登录类型选择认证处理器
根据订单类型选择订单处理器
```

简单工厂的缺点也很明显：如果使用传统 `switch` 或 `if else` 创建对象，每新增一种类型就要修改工厂类，不完全符合开闭原则。在 Spring Boot 中更推荐把具体实现注册成 Bean，然后由工厂类根据类型从 Map 中选择 Bean，这样可以减少分支判断。

## 项目结构

本示例使用 Spring Bean 方式实现简单工厂。所有发送器都交给 Spring 容器管理，工厂类只负责根据渠道选择对应的发送器。

```text
src/main/java/io/github/atengk/pattern/factory
├── FactoryApplication.java
├── config
│   └── FactoryDemoProperties.java
├── controller
│   └── NotifyController.java
├── dto
│   └── NotifySendDTO.java
├── enums
│   └── NotifyChannelEnum.java
├── factory
│   └── NotifySenderFactory.java
├── sender
│   ├── NotifySender.java
│   ├── EmailNotifySender.java
│   ├── SmsNotifySender.java
│   └── DingTalkNotifySender.java
├── service
│   ├── NotifyService.java
│   └── NotifyServiceImpl.java
└── vo
    └── NotifySendVO.java
```

## 核心代码

这一部分给出简单工厂模式的完整核心代码。重点是 `NotifySenderFactory`，它不负责具体业务发送，只负责根据渠道返回对应的 `NotifySender` 实现。

文件位置：`src/main/java/io/github/atengk/pattern/factory/FactoryApplication.java`

```java
package io.github.atengk.pattern.factory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 简单工厂模式示例启动类
 *
 * @author Ateng
 * @since 2026-05-13
 */
@SpringBootApplication
public class FactoryApplication {

    /**
     * 启动简单工厂模式示例应用
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(FactoryApplication.class, args);
    }

}
```

文件位置：`src/main/java/io/github/atengk/pattern/factory/config/FactoryDemoProperties.java`

```java
package io.github.atengk.pattern.factory.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 简单工厂模式示例配置
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
@Component
@ConfigurationProperties(prefix = "demo.factory")
public class FactoryDemoProperties {

    /**
     * 邮件通知开关
     */
    private Boolean emailEnabled = true;

    /**
     * 短信通知开关
     */
    private Boolean smsEnabled = true;

    /**
     * 钉钉通知开关
     */
    private Boolean dingTalkEnabled = true;

}
```

下面的枚举用于定义通知渠道，避免在业务代码中直接使用字符串判断。

文件位置：`src/main/java/io/github/atengk/pattern/factory/enums/NotifyChannelEnum.java`

```java
package io.github.atengk.pattern.factory.enums;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;

import java.util.Arrays;

/**
 * 通知渠道枚举
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Getter
public enum NotifyChannelEnum {

    EMAIL("EMAIL", "邮件通知"),

    SMS("SMS", "短信通知"),

    DING_TALK("DING_TALK", "钉钉通知");

    private final String code;

    private final String description;

    NotifyChannelEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根据编码解析通知渠道
     *
     * @param code 渠道编码
     * @return 通知渠道
     */
    public static NotifyChannelEnum parse(String code) {
        return Arrays.stream(values())
                .filter(item -> StrUtil.equalsIgnoreCase(item.getCode(), code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(StrUtil.format("不支持的通知渠道：{}", code)));
    }

}
```

下面的 DTO 是接口请求对象，调用方通过 `channel` 字段指定通知渠道。

文件位置：`src/main/java/io/github/atengk/pattern/factory/dto/NotifySendDTO.java`

```java
package io.github.atengk.pattern.factory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 通知发送请求对象
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
public class NotifySendDTO {

    /**
     * 通知渠道：EMAIL、SMS、DING_TALK
     */
    @NotBlank(message = "通知渠道不能为空")
    private String channel;

    /**
     * 接收人
     */
    @NotBlank(message = "接收人不能为空")
    private String receiver;

    /**
     * 通知标题
     */
    @NotBlank(message = "通知标题不能为空")
    @Size(max = 100, message = "通知标题长度不能超过100个字符")
    private String title;

    /**
     * 通知内容
     */
    @NotBlank(message = "通知内容不能为空")
    @Size(max = 500, message = "通知内容长度不能超过500个字符")
    private String content;

}
```

文件位置：`src/main/java/io/github/atengk/pattern/factory/vo/NotifySendVO.java`

```java
package io.github.atengk.pattern.factory.vo;

/**
 * 通知发送响应对象
 *
 * @author Ateng
 * @since 2026-05-13
 */
public record NotifySendVO(
        String channel,
        String receiver,
        Boolean success,
        String message,
        String sendTime
) {
}
```

下面的接口定义所有通知发送器的统一行为。简单工厂依赖接口，不依赖具体实现类。

文件位置：`src/main/java/io/github/atengk/pattern/factory/sender/NotifySender.java`

```java
package io.github.atengk.pattern.factory.sender;

import io.github.atengk.pattern.factory.dto.NotifySendDTO;
import io.github.atengk.pattern.factory.enums.NotifyChannelEnum;
import io.github.atengk.pattern.factory.vo.NotifySendVO;

/**
 * 通知发送器接口
 *
 * @author Ateng
 * @since 2026-05-13
 */
public interface NotifySender {

    /**
     * 获取当前发送器支持的通知渠道
     *
     * @return 通知渠道
     */
    NotifyChannelEnum supportChannel();

    /**
     * 发送通知
     *
     * @param sendDTO 发送请求
     * @return 发送结果
     */
    NotifySendVO send(NotifySendDTO sendDTO);

}
```

下面是邮件通知发送器实现。真实项目中可以在这里对接邮箱服务、企业邮件平台或消息中台。

文件位置：`src/main/java/io/github/atengk/pattern/factory/sender/EmailNotifySender.java`

```java
package io.github.atengk.pattern.factory.sender;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.pattern.factory.config.FactoryDemoProperties;
import io.github.atengk.pattern.factory.dto.NotifySendDTO;
import io.github.atengk.pattern.factory.enums.NotifyChannelEnum;
import io.github.atengk.pattern.factory.vo.NotifySendVO;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class EmailNotifySender implements NotifySender {

    private final FactoryDemoProperties properties;

    /**
     * 获取当前发送器支持的通知渠道
     *
     * @return 邮件通知渠道
     */
    @Override
    public NotifyChannelEnum supportChannel() {
        return NotifyChannelEnum.EMAIL;
    }

    /**
     * 发送邮件通知
     *
     * @param sendDTO 发送请求
     * @return 发送结果
     */
    @Override
    public NotifySendVO send(NotifySendDTO sendDTO) {
        if (!Boolean.TRUE.equals(properties.getEmailEnabled())) {
            log.warn("邮件通知已关闭，receiver：{}", sendDTO.getReceiver());
            return new NotifySendVO(supportChannel().getCode(), sendDTO.getReceiver(), false, "邮件通知已关闭", DateUtil.now());
        }

        String content = StrUtil.format("邮件标题：{}，邮件内容：{}", sendDTO.getTitle(), sendDTO.getContent());
        log.info("发送邮件通知，receiver：{}，content：{}", sendDTO.getReceiver(), content);

        return new NotifySendVO(supportChannel().getCode(), sendDTO.getReceiver(), true, "邮件通知发送成功", DateUtil.now());
    }

}
```

下面是短信通知发送器实现。真实项目中可以在这里对接阿里云短信、腾讯云短信或运营商短信网关。

文件位置：`src/main/java/io/github/atengk/pattern/factory/sender/SmsNotifySender.java`

```java
package io.github.atengk.pattern.factory.sender;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.pattern.factory.config.FactoryDemoProperties;
import io.github.atengk.pattern.factory.dto.NotifySendDTO;
import io.github.atengk.pattern.factory.enums.NotifyChannelEnum;
import io.github.atengk.pattern.factory.vo.NotifySendVO;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class SmsNotifySender implements NotifySender {

    private final FactoryDemoProperties properties;

    /**
     * 获取当前发送器支持的通知渠道
     *
     * @return 短信通知渠道
     */
    @Override
    public NotifyChannelEnum supportChannel() {
        return NotifyChannelEnum.SMS;
    }

    /**
     * 发送短信通知
     *
     * @param sendDTO 发送请求
     * @return 发送结果
     */
    @Override
    public NotifySendVO send(NotifySendDTO sendDTO) {
        if (!Boolean.TRUE.equals(properties.getSmsEnabled())) {
            log.warn("短信通知已关闭，receiver：{}", sendDTO.getReceiver());
            return new NotifySendVO(supportChannel().getCode(), sendDTO.getReceiver(), false, "短信通知已关闭", DateUtil.now());
        }

        String content = StrUtil.format("短信内容：[{}] {}", sendDTO.getTitle(), sendDTO.getContent());
        log.info("发送短信通知，receiver：{}，content：{}", sendDTO.getReceiver(), content);

        return new NotifySendVO(supportChannel().getCode(), sendDTO.getReceiver(), true, "短信通知发送成功", DateUtil.now());
    }

}
```

下面是钉钉通知发送器实现。真实项目中可以在这里对接钉钉机器人 Webhook 或企业内部消息平台。

文件位置：`src/main/java/io/github/atengk/pattern/factory/sender/DingTalkNotifySender.java`

```java
package io.github.atengk.pattern.factory.sender;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.pattern.factory.config.FactoryDemoProperties;
import io.github.atengk.pattern.factory.dto.NotifySendDTO;
import io.github.atengk.pattern.factory.enums.NotifyChannelEnum;
import io.github.atengk.pattern.factory.vo.NotifySendVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 钉钉通知发送器
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DingTalkNotifySender implements NotifySender {

    private final FactoryDemoProperties properties;

    /**
     * 获取当前发送器支持的通知渠道
     *
     * @return 钉钉通知渠道
     */
    @Override
    public NotifyChannelEnum supportChannel() {
        return NotifyChannelEnum.DING_TALK;
    }

    /**
     * 发送钉钉通知
     *
     * @param sendDTO 发送请求
     * @return 发送结果
     */
    @Override
    public NotifySendVO send(NotifySendDTO sendDTO) {
        if (!Boolean.TRUE.equals(properties.getDingTalkEnabled())) {
            log.warn("钉钉通知已关闭，receiver：{}", sendDTO.getReceiver());
            return new NotifySendVO(supportChannel().getCode(), sendDTO.getReceiver(), false, "钉钉通知已关闭", DateUtil.now());
        }

        String content = StrUtil.format("钉钉消息：{} - {}", sendDTO.getTitle(), sendDTO.getContent());
        log.info("发送钉钉通知，receiver：{}，content：{}", sendDTO.getReceiver(), content);

        return new NotifySendVO(supportChannel().getCode(), sendDTO.getReceiver(), true, "钉钉通知发送成功", DateUtil.now());
    }

}
```

下面的工厂类是简单工厂模式的核心。它在启动时收集所有 `NotifySender` 实现，并按照渠道编码注册到 Map 中。

文件位置：`src/main/java/io/github/atengk/pattern/factory/factory/NotifySenderFactory.java`

```java
package io.github.atengk.pattern.factory.factory;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.pattern.factory.enums.NotifyChannelEnum;
import io.github.atengk.pattern.factory.sender.NotifySender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 通知发送器简单工厂
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class NotifySenderFactory {

    private final Map<NotifyChannelEnum, NotifySender> senderMap;

    /**
     * 初始化通知发送器工厂
     *
     * @param notifySenders 通知发送器列表
     */
    public NotifySenderFactory(List<NotifySender> notifySenders) {
        if (CollUtil.isEmpty(notifySenders)) {
            this.senderMap = Collections.emptyMap();
            log.warn("未发现任何通知发送器实现");
            return;
        }

        Map<NotifyChannelEnum, NotifySender> tempSenderMap = new EnumMap<>(NotifyChannelEnum.class);
        for (NotifySender notifySender : notifySenders) {
            NotifyChannelEnum channel = notifySender.supportChannel();
            if (tempSenderMap.containsKey(channel)) {
                throw new IllegalStateException(StrUtil.format("通知渠道存在重复实现：{}", channel.getCode()));
            }
            tempSenderMap.put(channel, notifySender);
            log.info("注册通知发送器，channel：{}，sender：{}", channel.getCode(), notifySender.getClass().getSimpleName());
        }

        this.senderMap = Collections.unmodifiableMap(tempSenderMap);
    }

    /**
     * 根据通知渠道获取发送器
     *
     * @param channelCode 通知渠道编码
     * @return 通知发送器
     */
    public NotifySender getSender(String channelCode) {
        NotifyChannelEnum channel = NotifyChannelEnum.parse(channelCode);
        NotifySender notifySender = senderMap.get(channel);

        if (notifySender == null) {
            throw new IllegalArgumentException(StrUtil.format("通知渠道未注册发送器：{}", channel.getCode()));
        }

        return notifySender;
    }

    /**
     * 获取已注册发送器数量
     *
     * @return 已注册发送器数量
     */
    public int registeredCount() {
        return MapUtil.size(senderMap);
    }

}
```

下面的 Service 调用简单工厂获取发送器。业务层只依赖工厂和接口，不直接依赖 `EmailNotifySender`、`SmsNotifySender`、`DingTalkNotifySender`。

文件位置：`src/main/java/io/github/atengk/pattern/factory/service/NotifyService.java`

```java
package io.github.atengk.pattern.factory.service;

import io.github.atengk.pattern.factory.dto.NotifySendDTO;
import io.github.atengk.pattern.factory.vo.NotifySendVO;

/**
 * 通知服务接口
 *
 * @author Ateng
 * @since 2026-05-13
 */
public interface NotifyService {

    /**
     * 发送通知
     *
     * @param sendDTO 发送请求
     * @return 发送结果
     */
    NotifySendVO send(NotifySendDTO sendDTO);

}
```

文件位置：`src/main/java/io/github/atengk/pattern/factory/service/NotifyServiceImpl.java`

```java
package io.github.atengk.pattern.factory.service;

import cn.hutool.json.JSONUtil;
import io.github.atengk.pattern.factory.dto.NotifySendDTO;
import io.github.atengk.pattern.factory.factory.NotifySenderFactory;
import io.github.atengk.pattern.factory.sender.NotifySender;
import io.github.atengk.pattern.factory.vo.NotifySendVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 通知服务实现
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotifyServiceImpl implements NotifyService {

    private final NotifySenderFactory notifySenderFactory;

    /**
     * 发送通知
     *
     * @param sendDTO 发送请求
     * @return 发送结果
     */
    @Override
    public NotifySendVO send(NotifySendDTO sendDTO) {
        log.info("准备发送通知，请求参数：{}", JSONUtil.toJsonStr(sendDTO));

        NotifySender notifySender = notifySenderFactory.getSender(sendDTO.getChannel());
        NotifySendVO result = notifySender.send(sendDTO);

        log.info("通知发送完成，响应结果：{}", JSONUtil.toJsonStr(result));
        return result;
    }

}
```

文件位置：`src/main/java/io/github/atengk/pattern/factory/controller/NotifyController.java`

```java
package io.github.atengk.pattern.factory.controller;

import io.github.atengk.pattern.factory.dto.NotifySendDTO;
import io.github.atengk.pattern.factory.service.NotifyService;
import io.github.atengk.pattern.factory.vo.NotifySendVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 通知发送接口
 *
 * @author Ateng
 * @since 2026-05-13
 */
@RestController
@RequestMapping("/api/patterns/factory/notify")
@RequiredArgsConstructor
public class NotifyController {

    private final NotifyService notifyService;

    /**
     * 发送通知
     *
     * @param sendDTO 发送请求
     * @return 发送结果
     */
    @PostMapping("/send")
    public NotifySendVO send(@Valid @RequestBody NotifySendDTO sendDTO) {
        return notifyService.send(sendDTO);
    }

}
```

## 使用方式

启动项目后，通过接口传入不同的 `channel`，简单工厂会返回不同的发送器实现。

接口地址：

```text
POST /api/patterns/factory/notify/send
```

邮件通知请求：

```bash
curl -X POST "http://localhost:8080/api/patterns/factory/notify/send" \
  -H "Content-Type: application/json" \
  -d '{
    "channel": "EMAIL",
    "receiver": "user@example.com",
    "title": "订单支付成功",
    "content": "您的订单已支付成功，请等待系统发货。"
  }'
```

响应示例：

```json
{
  "channel": "EMAIL",
  "receiver": "user@example.com",
  "success": true,
  "message": "邮件通知发送成功",
  "sendTime": "2026-05-13 11:20:30"
}
```

短信通知请求：

```bash
curl -X POST "http://localhost:8080/api/patterns/factory/notify/send" \
  -H "Content-Type: application/json" \
  -d '{
    "channel": "SMS",
    "receiver": "13800000000",
    "title": "登录验证码",
    "content": "您的验证码是 9527，5 分钟内有效。"
  }'
```

响应示例：

```json
{
  "channel": "SMS",
  "receiver": "13800000000",
  "success": true,
  "message": "短信通知发送成功",
  "sendTime": "2026-05-13 11:21:06"
}
```

钉钉通知请求：

```bash
curl -X POST "http://localhost:8080/api/patterns/factory/notify/send" \
  -H "Content-Type: application/json" \
  -d '{
    "channel": "DING_TALK",
    "receiver": "ops-group",
    "title": "服务告警",
    "content": "订单服务响应时间超过阈值，请及时处理。"
  }'
```

响应示例：

```json
{
  "channel": "DING_TALK",
  "receiver": "ops-group",
  "success": true,
  "message": "钉钉通知发送成功",
  "sendTime": "2026-05-13 11:22:18"
}
```

## 新增一种通知渠道

简单工厂模式的扩展方式取决于具体实现。如果是传统 `switch` 写法，新增类型需要修改工厂类。如果使用 Spring Bean 注册方式，只需要新增实现类和枚举值，工厂类不需要修改。

例如新增 `WECHAT` 企业微信通知：

```java
WECHAT("WECHAT", "企业微信通知");
```

然后新增实现类：

```java
@Component
public class WechatNotifySender implements NotifySender {

    @Override
    public NotifyChannelEnum supportChannel() {
        return NotifyChannelEnum.WECHAT;
    }

    @Override
    public NotifySendVO send(NotifySendDTO sendDTO) {
        // 对接企业微信通知逻辑
        return new NotifySendVO(supportChannel().getCode(), sendDTO.getReceiver(), true, "企业微信通知发送成功", DateUtil.now());
    }

}
```

这种扩展方式中，工厂类只负责“根据类型找对象”，具体对象由 Spring 自动注入，避免工厂类膨胀。

## 传统简单工厂写法

传统简单工厂通常使用静态方法和 `switch` 创建对象。这种写法直观，但不适合依赖 Spring Bean 的复杂业务类。

下面示例只适合普通 Java 对象创建，不推荐用于需要注入配置、数据库、Redis、远程客户端的 Spring 业务对象。

文件位置：`src/main/java/io/github/atengk/pattern/factory/standalone/StandaloneNotifySenderFactory.java`

```java
package io.github.atengk.pattern.factory.standalone;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.pattern.factory.enums.NotifyChannelEnum;
import io.github.atengk.pattern.factory.sender.DingTalkNotifySender;
import io.github.atengk.pattern.factory.sender.EmailNotifySender;
import io.github.atengk.pattern.factory.sender.NotifySender;
import io.github.atengk.pattern.factory.sender.SmsNotifySender;

/**
 * 传统静态简单工厂示例
 *
 * @author Ateng
 * @since 2026-05-13
 */
public final class StandaloneNotifySenderFactory {

    private StandaloneNotifySenderFactory() {
    }

    /**
     * 根据通知渠道创建发送器
     *
     * @param channelCode 通知渠道编码
     * @return 通知发送器
     */
    public static NotifySender create(String channelCode) {
        NotifyChannelEnum channel = NotifyChannelEnum.parse(channelCode);

        return switch (channel) {
            case EMAIL -> throw new UnsupportedOperationException("邮件发送器依赖 Spring 配置，不适合通过静态工厂直接创建");
            case SMS -> throw new UnsupportedOperationException("短信发送器依赖 Spring 配置，不适合通过静态工厂直接创建");
            case DING_TALK -> throw new UnsupportedOperationException("钉钉发送器依赖 Spring 配置，不适合通过静态工厂直接创建");
            default -> throw new IllegalArgumentException(StrUtil.format("不支持的通知渠道：{}", channelCode));
        };
    }

}
```

上面这段代码刻意没有直接 `new EmailNotifySender()`，因为当前发送器依赖 `FactoryDemoProperties`。这正是 Spring Boot 项目中传统静态工厂的常见问题：一旦具体实现类需要依赖容器能力，静态工厂就会变得不自然。

在 Spring Boot 项目中，更推荐使用前面的 `NotifySenderFactory`。

## 验证方式

可以通过单元测试验证工厂是否能根据不同渠道返回正确实现类。

文件位置：`src/test/java/io/github/atengk/pattern/factory/NotifySenderFactoryTest.java`

```java
package io.github.atengk.pattern.factory;

import io.github.atengk.pattern.factory.factory.NotifySenderFactory;
import io.github.atengk.pattern.factory.sender.DingTalkNotifySender;
import io.github.atengk.pattern.factory.sender.EmailNotifySender;
import io.github.atengk.pattern.factory.sender.NotifySender;
import io.github.atengk.pattern.factory.sender.SmsNotifySender;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 通知发送器简单工厂测试
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@SpringBootTest
class NotifySenderFactoryTest {

    private final NotifySenderFactory notifySenderFactory;

    NotifySenderFactoryTest(NotifySenderFactory notifySenderFactory) {
        this.notifySenderFactory = notifySenderFactory;
    }

    /**
     * 验证不同通知渠道能够获取不同发送器
     */
    @Test
    void shouldGetDifferentSenderByChannel() {
        NotifySender emailSender = notifySenderFactory.getSender("EMAIL");
        NotifySender smsSender = notifySenderFactory.getSender("SMS");
        NotifySender dingTalkSender = notifySenderFactory.getSender("DING_TALK");

        log.info("邮件发送器：{}", emailSender.getClass().getSimpleName());
        log.info("短信发送器：{}", smsSender.getClass().getSimpleName());
        log.info("钉钉发送器：{}", dingTalkSender.getClass().getSimpleName());

        Assertions.assertInstanceOf(EmailNotifySender.class, emailSender);
        Assertions.assertInstanceOf(SmsNotifySender.class, smsSender);
        Assertions.assertInstanceOf(DingTalkNotifySender.class, dingTalkSender);
    }

    /**
     * 验证不支持的通知渠道会抛出异常
     */
    @Test
    void shouldThrowExceptionWhenChannelUnsupported() {
        IllegalArgumentException exception = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> notifySenderFactory.getSender("UNKNOWN")
        );

        log.info("不支持渠道异常信息：{}", exception.getMessage());
        Assertions.assertTrue(exception.getMessage().contains("不支持的通知渠道"));
    }

}
```

执行测试：

```bash
mvn test -Dtest=NotifySenderFactoryTest
```

命令说明：`-Dtest=NotifySenderFactoryTest` 表示只运行当前工厂测试类，用于快速验证工厂注册和对象选择逻辑。

## 常见问题

简单工厂和策略模式容易混淆。简单工厂关注“创建或获取对象”，策略模式关注“根据不同算法或业务规则执行不同逻辑”。在 Spring Boot 项目中，二者经常一起出现：工厂负责根据类型找到策略对象，策略对象负责执行业务逻辑。

简单工厂和工厂方法模式也容易混淆。简单工厂通常只有一个工厂类，通过类型参数决定返回哪个对象；工厂方法模式会为每一种产品创建对应的工厂类，把对象创建延迟到具体工厂中。

在实际项目中，如果类型较少、创建逻辑简单，简单工厂足够使用。如果类型很多、创建过程复杂、产品族之间存在组合关系，可以考虑工厂方法模式或抽象工厂模式。

## 开发建议

在 Spring Boot 项目中使用简单工厂时，建议遵循以下原则：

```text
优先依赖接口，不要让业务层依赖具体实现类
优先使用枚举定义类型，减少魔法字符串
优先让具体实现类注册成 Spring Bean
工厂类只做对象选择，不写具体业务逻辑
实现类需要依赖配置、数据库、Redis、第三方客户端时，不要使用静态工厂 new 对象
新增类型时，尽量只新增枚举和实现类，避免反复修改工厂核心逻辑
```

简单工厂模式的重点不是“工厂类本身”，而是把对象选择逻辑从业务流程中剥离出来。这样 Controller 和 Service 不需要写大量 `if else`，后续新增类型也更容易定位和维护。

## 总结

简单工厂模式适合处理“根据类型获取对象”的场景。在 Spring Boot 项目中，最实用的写法是“接口 + 多个实现类 + Spring Bean 自动注入 + 工厂 Map 路由”。

它的核心流程可以概括为：

```text
定义统一接口
为每种类型创建一个实现类
每个实现类声明自己支持的类型
工厂类启动时收集所有实现类
业务层根据类型从工厂获取实现
调用统一接口完成业务处理
```

对于通知、支付、导入导出、文件解析、登录认证、订单处理等业务，简单工厂都可以有效减少分支判断，让对象选择逻辑更集中、更清晰。
