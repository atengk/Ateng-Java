# 构建者模式 + 简单工厂模式

构建者模式和简单工厂模式组合使用时，通常用于处理 **根据业务类型创建不同复杂对象** 的场景。

构建者模式负责分步骤组装复杂对象，避免构造方法参数过多、对象创建逻辑混乱；简单工厂模式负责根据类型选择对应的构建器，让调用方不直接感知具体构建过程。这个组合适合放在你原文档“设计模式组合使用”章节中，符合原文档中“简单工厂负责选择对象类型，构建者模式负责组装复杂对象”的定义。

```text
简单工厂模式：根据业务类型选择对应构建器
构建者模式：负责组装复杂对象
```

## 适用场景

本示例以“通知消息构建”为业务场景。系统支持多种通知渠道，不同渠道需要构建不同的消息对象：

| 通知渠道  | 说明    | 构建差异                |
| ----- | ----- | ------------------- |
| EMAIL | 邮件通知  | 需要收件邮箱、邮件标题、HTML 内容 |
| SMS   | 短信通知  | 需要手机号、短信内容、短信签名     |
| APP   | 站内信通知 | 需要用户ID、站内信标题、跳转路径   |

如果直接在 Service 中写构建逻辑，代码通常会变成：

```text
if EMAIL:
  构建邮件消息
else if SMS:
  构建短信消息
else if APP:
  构建站内信消息
```

这种写法会导致 Service 同时承担“选择类型”和“组装对象”的职责。随着通知渠道增加，Service 会越来越复杂。

使用“构建者模式 + 简单工厂模式”后，结构变成：

```text
Controller
  -> Service
      -> NotificationMessageBuilderFactory：根据通知渠道获取构建器
          -> EmailNotificationMessageBuilder
          -> SmsNotificationMessageBuilder
          -> AppNotificationMessageBuilder
              -> 构建 NotificationMessage
```

## 基础配置

这里使用 JDK 21、Spring Boot 3、Maven、Lombok、Hutool 和 Spring Validation。

文件位置：`pom.xml`

```xml
<dependencies>
    <!-- Spring Web：提供 REST API 能力 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Validation：用于请求参数校验 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- Hutool：提供字符串、对象、集合、ID、日期等常用工具 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.8.36</version>
    </dependency>

    <!-- Lombok：减少 Getter、Setter、Builder、构造器、日志等样板代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

建议目录结构如下：

```text
src/main/java/io/github/atengk/pattern/combination/builderfactory
├── builder
│   ├── NotificationMessageBuilder.java
│   └── impl
│       ├── AppNotificationMessageBuilder.java
│       ├── EmailNotificationMessageBuilder.java
│       └── SmsNotificationMessageBuilder.java
├── controller
│   └── NotificationController.java
├── domain
│   └── NotificationMessage.java
├── dto
│   ├── NotificationCreateRequest.java
│   └── NotificationCreateResponse.java
├── enums
│   └── NotificationChannelEnum.java
├── factory
│   └── NotificationMessageBuilderFactory.java
├── handler
│   └── GlobalExceptionHandler.java
├── result
│   └── Result.java
└── service
    ├── NotificationCreateService.java
    └── impl
        └── NotificationCreateServiceImpl.java
```

## 核心代码

这一部分给出核心实现。重点看两个位置：

```text
NotificationMessageBuilderFactory：简单工厂，根据通知渠道选择构建器
NotificationMessageBuilder：构建者接口，负责构建复杂通知消息对象
```

调用方只传入统一请求对象，具体使用哪个构建器由简单工厂决定。每个构建器内部只负责一种通知渠道的对象组装逻辑。

### 通知渠道枚举

通知渠道枚举用于统一维护通知渠道编码。

文件位置：`src/main/java/io/github/atengk/pattern/combination/builderfactory/enums/NotificationChannelEnum.java`

```java
package io.github.atengk.pattern.combination.builderfactory.enums;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 通知渠道枚举
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Getter
@RequiredArgsConstructor
public enum NotificationChannelEnum {

    EMAIL("EMAIL", "邮件通知"),

    SMS("SMS", "短信通知"),

    APP("APP", "站内信通知");

    private final String code;

    private final String description;

    /**
     * 根据编码获取通知渠道
     *
     * @param code 通知渠道编码
     * @return 通知渠道枚举
     */
    public static NotificationChannelEnum of(String code) {
        String actualCode = StrUtil.trimToEmpty(code);
        for (NotificationChannelEnum item : values()) {
            if (StrUtil.equalsIgnoreCase(item.getCode(), actualCode)) {
                return item;
            }
        }
        throw new IllegalArgumentException("不支持的通知渠道：" + code);
    }
}
```

### 通知创建请求对象

通知创建请求对象用于接收不同渠道的通知参数。实际项目中也可以拆成不同请求对象，这里为了突出组合模式，使用一个统一请求对象。

文件位置：`src/main/java/io/github/atengk/pattern/combination/builderfactory/dto/NotificationCreateRequest.java`

```java
package io.github.atengk.pattern.combination.builderfactory.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

/**
 * 通知创建请求
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
public class NotificationCreateRequest {

    /**
     * 业务编号
     */
    @NotBlank(message = "业务编号不能为空")
    private String bizNo;

    /**
     * 通知渠道：EMAIL、SMS、APP
     */
    @NotBlank(message = "通知渠道不能为空")
    private String channel;

    /**
     * 接收人，邮件为邮箱，短信为手机号，站内信可为空
     */
    private String recipient;

    /**
     * 用户ID，站内信通知时使用
     */
    private Long userId;

    /**
     * 通知标题
     */
    private String title;

    /**
     * 通知内容
     */
    @NotBlank(message = "通知内容不能为空")
    private String content;

    /**
     * 扩展参数
     */
    private Map<String, Object> extraParams;
}
```

### 通知创建响应对象

通知创建响应对象用于返回构建后的通知结果摘要。

文件位置：`src/main/java/io/github/atengk/pattern/combination/builderfactory/dto/NotificationCreateResponse.java`

```java
package io.github.atengk.pattern.combination.builderfactory.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 通知创建响应
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
@Builder
public class NotificationCreateResponse {

    /**
     * 消息编号
     */
    private String messageNo;

    /**
     * 业务编号
     */
    private String bizNo;

    /**
     * 通知渠道
     */
    private String channel;

    /**
     * 目标地址
     */
    private String targetAddress;

    /**
     * 通知标题
     */
    private String title;

    /**
     * 内容预览
     */
    private String contentPreview;

    /**
     * 跳转路径
     */
    private String jumpPath;

    /**
     * 消息标签
     */
    private List<String> tags;

    /**
     * 响应消息
     */
    private String message;
}
```

### 通知消息领域对象

通知消息对象是构建者最终要组装的复杂对象。它包含邮件、短信、站内信共用字段，也包含不同渠道的差异字段。

文件位置：`src/main/java/io/github/atengk/pattern/combination/builderfactory/domain/NotificationMessage.java`

```java
package io.github.atengk.pattern.combination.builderfactory.domain;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 通知消息领域对象
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
@Builder
public class NotificationMessage {

    /**
     * 消息编号
     */
    private String messageNo;

    /**
     * 业务编号
     */
    private String bizNo;

    /**
     * 通知渠道
     */
    private String channel;

    /**
     * 接收目标
     */
    private String targetAddress;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 标题
     */
    private String title;

    /**
     * 内容
     */
    private String content;

    /**
     * 内容类型，例如 TEXT、HTML
     */
    private String contentType;

    /**
     * 短信签名
     */
    private String smsSign;

    /**
     * 站内信跳转路径
     */
    private String jumpPath;

    /**
     * 消息标签
     */
    @Builder.Default
    private List<String> tags = new ArrayList<>();

    /**
     * 扩展参数
     */
    private Map<String, Object> extraParams;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
```

### 通知消息构建器接口

构建器接口定义不同通知渠道的对象构建行为。

文件位置：`src/main/java/io/github/atengk/pattern/combination/builderfactory/builder/NotificationMessageBuilder.java`

```java
package io.github.atengk.pattern.combination.builderfactory.builder;

import io.github.atengk.pattern.combination.builderfactory.domain.NotificationMessage;
import io.github.atengk.pattern.combination.builderfactory.dto.NotificationCreateRequest;
import io.github.atengk.pattern.combination.builderfactory.enums.NotificationChannelEnum;

/**
 * 通知消息构建器接口
 *
 * @author Ateng
 * @since 2026-05-13
 */
public interface NotificationMessageBuilder {

    /**
     * 当前构建器支持的通知渠道
     *
     * @return 通知渠道
     */
    NotificationChannelEnum channel();

    /**
     * 构建通知消息
     *
     * @param request 通知创建请求
     * @return 通知消息
     */
    NotificationMessage build(NotificationCreateRequest request);
}
```

### 邮件通知构建器

邮件通知构建器负责构建邮件消息对象。它会校验收件邮箱，设置 HTML 内容类型，并添加邮件相关标签。

文件位置：`src/main/java/io/github/atengk/pattern/combination/builderfactory/builder/impl/EmailNotificationMessageBuilder.java`

```java
package io.github.atengk.pattern.combination.builderfactory.builder.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.pattern.combination.builderfactory.builder.NotificationMessageBuilder;
import io.github.atengk.pattern.combination.builderfactory.domain.NotificationMessage;
import io.github.atengk.pattern.combination.builderfactory.dto.NotificationCreateRequest;
import io.github.atengk.pattern.combination.builderfactory.enums.NotificationChannelEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 邮件通知消息构建器
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class EmailNotificationMessageBuilder implements NotificationMessageBuilder {

    /**
     * 当前构建器支持的通知渠道
     *
     * @return 通知渠道
     */
    @Override
    public NotificationChannelEnum channel() {
        return NotificationChannelEnum.EMAIL;
    }

    /**
     * 构建邮件通知消息
     *
     * @param request 通知创建请求
     * @return 通知消息
     */
    @Override
    public NotificationMessage build(NotificationCreateRequest request) {
        if (StrUtil.isBlank(request.getRecipient()) || !StrUtil.contains(request.getRecipient(), "@")) {
            throw new IllegalArgumentException("邮件通知必须填写合法邮箱地址");
        }

        String title = StrUtil.blankToDefault(request.getTitle(), "系统邮件通知");
        String content = StrUtil.format("<html><body><h3>{}</h3><p>{}</p></body></html>", title, request.getContent());

        NotificationMessage message = NotificationMessage.builder()
                .messageNo("MSG" + IdUtil.getSnowflakeNextIdStr())
                .bizNo(request.getBizNo())
                .channel(channel().getCode())
                .targetAddress(request.getRecipient())
                .title(title)
                .content(content)
                .contentType("HTML")
                .tags(List.of("EMAIL", "HTML"))
                .extraParams(request.getExtraParams())
                .createTime(LocalDateTime.now())
                .build();

        log.info("邮件通知消息构建完成，业务编号：{}，收件人：{}，消息编号：{}",
                request.getBizNo(), request.getRecipient(), message.getMessageNo());

        return message;
    }
}
```

### 短信通知构建器

短信通知构建器负责构建短信消息对象。它会校验手机号，设置短信签名，并控制短信内容格式。

文件位置：`src/main/java/io/github/atengk/pattern/combination/builderfactory/builder/impl/SmsNotificationMessageBuilder.java`

```java
package io.github.atengk.pattern.combination.builderfactory.builder.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.pattern.combination.builderfactory.builder.NotificationMessageBuilder;
import io.github.atengk.pattern.combination.builderfactory.domain.NotificationMessage;
import io.github.atengk.pattern.combination.builderfactory.dto.NotificationCreateRequest;
import io.github.atengk.pattern.combination.builderfactory.enums.NotificationChannelEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 短信通知消息构建器
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class SmsNotificationMessageBuilder implements NotificationMessageBuilder {

    private static final String PHONE_REGEX = "^1[3-9]\\d{9}$";

    private static final String SMS_SIGN = "【业务系统】";

    /**
     * 当前构建器支持的通知渠道
     *
     * @return 通知渠道
     */
    @Override
    public NotificationChannelEnum channel() {
        return NotificationChannelEnum.SMS;
    }

    /**
     * 构建短信通知消息
     *
     * @param request 通知创建请求
     * @return 通知消息
     */
    @Override
    public NotificationMessage build(NotificationCreateRequest request) {
        if (StrUtil.isBlank(request.getRecipient()) || !ReUtil.isMatch(PHONE_REGEX, request.getRecipient())) {
            throw new IllegalArgumentException("短信通知必须填写合法手机号");
        }

        String content = StrUtil.format("{}{}", SMS_SIGN, request.getContent());

        NotificationMessage message = NotificationMessage.builder()
                .messageNo("MSG" + IdUtil.getSnowflakeNextIdStr())
                .bizNo(request.getBizNo())
                .channel(channel().getCode())
                .targetAddress(request.getRecipient())
                .title("短信通知")
                .content(content)
                .contentType("TEXT")
                .smsSign(SMS_SIGN)
                .tags(List.of("SMS", "TEXT"))
                .extraParams(request.getExtraParams())
                .createTime(LocalDateTime.now())
                .build();

        log.info("短信通知消息构建完成，业务编号：{}，手机号：{}，消息编号：{}",
                request.getBizNo(), request.getRecipient(), message.getMessageNo());

        return message;
    }
}
```

### 站内信通知构建器

站内信通知构建器负责构建站内信消息对象。它会校验用户ID，设置跳转路径，并添加站内信标签。

文件位置：`src/main/java/io/github/atengk/pattern/combination/builderfactory/builder/impl/AppNotificationMessageBuilder.java`

```java
package io.github.atengk.pattern.combination.builderfactory.builder.impl;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.pattern.combination.builderfactory.builder.NotificationMessageBuilder;
import io.github.atengk.pattern.combination.builderfactory.domain.NotificationMessage;
import io.github.atengk.pattern.combination.builderfactory.dto.NotificationCreateRequest;
import io.github.atengk.pattern.combination.builderfactory.enums.NotificationChannelEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 站内信通知消息构建器
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class AppNotificationMessageBuilder implements NotificationMessageBuilder {

    /**
     * 当前构建器支持的通知渠道
     *
     * @return 通知渠道
     */
    @Override
    public NotificationChannelEnum channel() {
        return NotificationChannelEnum.APP;
    }

    /**
     * 构建站内信通知消息
     *
     * @param request 通知创建请求
     * @return 通知消息
     */
    @Override
    public NotificationMessage build(NotificationCreateRequest request) {
        if (ObjectUtil.isNull(request.getUserId())) {
            throw new IllegalArgumentException("站内信通知必须填写用户ID");
        }

        Map<String, Object> extraParams = request.getExtraParams();
        String jumpPath = MapUtil.isNotEmpty(extraParams)
                ? StrUtil.toString(extraParams.getOrDefault("jumpPath", "/message/detail"))
                : "/message/detail";

        String title = StrUtil.blankToDefault(request.getTitle(), "站内信通知");

        NotificationMessage message = NotificationMessage.builder()
                .messageNo("MSG" + IdUtil.getSnowflakeNextIdStr())
                .bizNo(request.getBizNo())
                .channel(channel().getCode())
                .targetAddress(StrUtil.format("user:{}", request.getUserId()))
                .userId(request.getUserId())
                .title(title)
                .content(request.getContent())
                .contentType("TEXT")
                .jumpPath(jumpPath)
                .tags(List.of("APP", "INBOX"))
                .extraParams(extraParams)
                .createTime(LocalDateTime.now())
                .build();

        log.info("站内信通知消息构建完成，业务编号：{}，用户ID：{}，消息编号：{}",
                request.getBizNo(), request.getUserId(), message.getMessageNo());

        return message;
    }
}
```

### 通知消息构建器简单工厂

简单工厂负责根据通知渠道获取对应构建器。这里通过 Spring 自动注入所有构建器，然后组装成映射表。

文件位置：`src/main/java/io/github/atengk/pattern/combination/builderfactory/factory/NotificationMessageBuilderFactory.java`

```java
package io.github.atengk.pattern.combination.builderfactory.factory;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.pattern.combination.builderfactory.builder.NotificationMessageBuilder;
import io.github.atengk.pattern.combination.builderfactory.enums.NotificationChannelEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 通知消息构建器简单工厂
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class NotificationMessageBuilderFactory {

    private final Map<NotificationChannelEnum, NotificationMessageBuilder> builderMap;

    /**
     * 初始化通知消息构建器工厂
     *
     * @param builderList 通知消息构建器集合
     */
    public NotificationMessageBuilderFactory(List<NotificationMessageBuilder> builderList) {
        if (CollUtil.isEmpty(builderList)) {
            throw new IllegalStateException("通知消息构建器不能为空");
        }

        Map<NotificationChannelEnum, NotificationMessageBuilder> tempBuilderMap = new EnumMap<>(NotificationChannelEnum.class);
        for (NotificationMessageBuilder builder : builderList) {
            NotificationMessageBuilder oldBuilder = tempBuilderMap.put(builder.channel(), builder);
            if (ObjectUtil.isNotNull(oldBuilder)) {
                throw new IllegalStateException("通知消息构建器重复注册：" + builder.channel().getCode());
            }
        }

        this.builderMap = Collections.unmodifiableMap(tempBuilderMap);
        log.info("通知消息构建器工厂初始化完成，构建器数量：{}", this.builderMap.size());
    }

    /**
     * 根据通知渠道获取构建器
     *
     * @param channelCode 通知渠道编码
     * @return 通知消息构建器
     */
    public NotificationMessageBuilder getBuilder(String channelCode) {
        NotificationChannelEnum channel = NotificationChannelEnum.of(channelCode);
        NotificationMessageBuilder builder = builderMap.get(channel);
        if (ObjectUtil.isNull(builder)) {
            throw new IllegalArgumentException("未找到通知消息构建器：" + channelCode);
        }

        log.info("命中通知消息构建器，通知渠道：{}，构建器类：{}",
                channel.getCode(), builder.getClass().getSimpleName());
        return builder;
    }
}
```

### Service 接口

Service 接口对外暴露通知创建能力。

文件位置：`src/main/java/io/github/atengk/pattern/combination/builderfactory/service/NotificationCreateService.java`

```java
package io.github.atengk.pattern.combination.builderfactory.service;

import io.github.atengk.pattern.combination.builderfactory.dto.NotificationCreateRequest;
import io.github.atengk.pattern.combination.builderfactory.dto.NotificationCreateResponse;

/**
 * 通知创建服务接口
 *
 * @author Ateng
 * @since 2026-05-13
 */
public interface NotificationCreateService {

    /**
     * 创建通知消息
     *
     * @param request 通知创建请求
     * @return 通知创建响应
     */
    NotificationCreateResponse create(NotificationCreateRequest request);
}
```

### Service 实现类

Service 实现类只负责编排：通过简单工厂获取构建器，再调用构建器组装复杂消息对象。

文件位置：`src/main/java/io/github/atengk/pattern/combination/builderfactory/service/impl/NotificationCreateServiceImpl.java`

```java
package io.github.atengk.pattern.combination.builderfactory.service.impl;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.pattern.combination.builderfactory.builder.NotificationMessageBuilder;
import io.github.atengk.pattern.combination.builderfactory.domain.NotificationMessage;
import io.github.atengk.pattern.combination.builderfactory.dto.NotificationCreateRequest;
import io.github.atengk.pattern.combination.builderfactory.dto.NotificationCreateResponse;
import io.github.atengk.pattern.combination.builderfactory.factory.NotificationMessageBuilderFactory;
import io.github.atengk.pattern.combination.builderfactory.service.NotificationCreateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 通知创建服务实现类
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationCreateServiceImpl implements NotificationCreateService {

    private final NotificationMessageBuilderFactory builderFactory;

    /**
     * 创建通知消息
     *
     * @param request 通知创建请求
     * @return 通知创建响应
     */
    @Override
    public NotificationCreateResponse create(NotificationCreateRequest request) {
        NotificationMessageBuilder builder = builderFactory.getBuilder(request.getChannel());
        NotificationMessage message = builder.build(request);

        log.info("通知消息创建完成，业务编号：{}，通知渠道：{}，消息编号：{}",
                message.getBizNo(), message.getChannel(), message.getMessageNo());

        return NotificationCreateResponse.builder()
                .messageNo(message.getMessageNo())
                .bizNo(message.getBizNo())
                .channel(message.getChannel())
                .targetAddress(message.getTargetAddress())
                .title(message.getTitle())
                .contentPreview(StrUtil.subPre(message.getContent(), 40))
                .jumpPath(message.getJumpPath())
                .tags(message.getTags())
                .message("通知消息创建成功")
                .build();
    }
}
```

### 统一响应对象

统一响应对象用于包装接口返回结果。

文件位置：`src/main/java/io/github/atengk/pattern/combination/builderfactory/result/Result.java`

```java
package io.github.atengk.pattern.combination.builderfactory.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一响应结果
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

    /**
     * 响应编码
     */
    private Integer code;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 成功响应
     *
     * @param data 响应数据
     * @param <T>  数据类型
     * @return 统一响应结果
     */
    public static <T> Result<T> success(T data) {
        return Result.<T>builder()
                .code(200)
                .message("操作成功")
                .data(data)
                .build();
    }

    /**
     * 失败响应
     *
     * @param code    响应编码
     * @param message 响应消息
     * @param <T>     数据类型
     * @return 统一响应结果
     */
    public static <T> Result<T> fail(Integer code, String message) {
        return Result.<T>builder()
                .code(code)
                .message(message)
                .data(null)
                .build();
    }
}
```

### 全局异常处理器

全局异常处理器用于统一处理参数异常和业务异常。

文件位置：`src/main/java/io/github/atengk/pattern/combination/builderfactory/handler/GlobalExceptionHandler.java`

```java
package io.github.atengk.pattern.combination.builderfactory.handler;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.pattern.combination.builderfactory.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理参数校验异常
     *
     * @param exception 参数校验异常
     * @return 统一响应结果
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException exception) {
        FieldError fieldError = exception.getBindingResult().getFieldError();
        String message = ObjectUtil.isNotNull(fieldError)
                ? StrUtil.blankToDefault(fieldError.getDefaultMessage(), "请求参数不合法")
                : "请求参数不合法";

        log.warn("请求参数校验失败：{}", message);
        return Result.fail(400, message);
    }

    /**
     * 处理非法参数异常
     *
     * @param exception 非法参数异常
     * @return 统一响应结果
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public Result<Void> handleIllegalArgumentException(IllegalArgumentException exception) {
        log.warn("通知消息创建异常：{}", exception.getMessage());
        return Result.fail(400, exception.getMessage());
    }

    /**
     * 处理系统异常
     *
     * @param exception 系统异常
     * @return 统一响应结果
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception exception) {
        log.error("系统异常", exception);
        return Result.fail(500, "系统繁忙，请稍后再试");
    }
}
```

### Controller

Controller 提供通知消息创建接口。

文件位置：`src/main/java/io/github/atengk/pattern/combination/builderfactory/controller/NotificationController.java`

```java
package io.github.atengk.pattern.combination.builderfactory.controller;

import io.github.atengk.pattern.combination.builderfactory.dto.NotificationCreateRequest;
import io.github.atengk.pattern.combination.builderfactory.dto.NotificationCreateResponse;
import io.github.atengk.pattern.combination.builderfactory.result.Result;
import io.github.atengk.pattern.combination.builderfactory.service.NotificationCreateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 通知消息控制器
 *
 * @author Ateng
 * @since 2026-05-13
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationCreateService notificationCreateService;

    /**
     * 创建通知消息
     *
     * @param request 通知创建请求
     * @return 通知创建响应
     */
    @PostMapping
    public Result<NotificationCreateResponse> create(@Valid @RequestBody NotificationCreateRequest request) {
        return Result.success(notificationCreateService.create(request));
    }
}
```

## 使用方式

启动 Spring Boot 项目后，可以通过通知创建接口验证构建者模式和简单工厂模式是否同时生效。

接口信息如下：

```text
请求地址：POST /api/notifications
Content-Type：application/json
```

邮件通知请求示例：

```bash
curl -X POST "http://localhost:8080/api/notifications" \
  -H "Content-Type: application/json" \
  -d '{
    "bizNo": "BIZ202605130001",
    "channel": "EMAIL",
    "recipient": "user@example.com",
    "title": "订单支付成功",
    "content": "您的订单已经支付成功，请及时查看订单详情。"
  }'
```

响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "messageNo": "MSG1988267712300011520",
    "bizNo": "BIZ202605130001",
    "channel": "EMAIL",
    "targetAddress": "user@example.com",
    "title": "订单支付成功",
    "contentPreview": "<html><body><h3>订单支付成功</h3><p>您的订单已经支付",
    "jumpPath": null,
    "tags": [
      "EMAIL",
      "HTML"
    ],
    "message": "通知消息创建成功"
  }
}
```

短信通知请求示例：

```bash
curl -X POST "http://localhost:8080/api/notifications" \
  -H "Content-Type: application/json" \
  -d '{
    "bizNo": "BIZ202605130002",
    "channel": "SMS",
    "recipient": "13800138000",
    "content": "您的验证码为 123456，5 分钟内有效。"
  }'
```

响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "messageNo": "MSG1988267712300011521",
    "bizNo": "BIZ202605130002",
    "channel": "SMS",
    "targetAddress": "13800138000",
    "title": "短信通知",
    "contentPreview": "【业务系统】您的验证码为 123456，5 分钟内有效。",
    "jumpPath": null,
    "tags": [
      "SMS",
      "TEXT"
    ],
    "message": "通知消息创建成功"
  }
}
```

站内信通知请求示例：

```bash
curl -X POST "http://localhost:8080/api/notifications" \
  -H "Content-Type: application/json" \
  -d '{
    "bizNo": "BIZ202605130003",
    "channel": "APP",
    "userId": 1001,
    "title": "系统提醒",
    "content": "您有一条新的审批任务，请及时处理。",
    "extraParams": {
      "jumpPath": "/approval/detail/10086"
    }
  }'
```

响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "messageNo": "MSG1988267712300011522",
    "bizNo": "BIZ202605130003",
    "channel": "APP",
    "targetAddress": "user:1001",
    "title": "系统提醒",
    "contentPreview": "您有一条新的审批任务，请及时处理。",
    "jumpPath": "/approval/detail/10086",
    "tags": [
      "APP",
      "INBOX"
    ],
    "message": "通知消息创建成功"
  }
}
```

不支持的通知渠道请求示例：

```bash
curl -X POST "http://localhost:8080/api/notifications" \
  -H "Content-Type: application/json" \
  -d '{
    "bizNo": "BIZ202605130004",
    "channel": "DINGTALK",
    "recipient": "user001",
    "content": "测试通知"
  }'
```

响应示例：

```json
{
  "code": 400,
  "message": "不支持的通知渠道：DINGTALK",
  "data": null
}
```

## 新增通知渠道

当业务新增一种通知渠道时，例如新增“企业微信通知”，不需要修改 Service，也不需要修改已有构建器，只需要新增枚举和构建器实现类。

第一步，在 `NotificationChannelEnum` 中新增枚举：

```java
WECHAT_WORK("WECHAT_WORK", "企业微信通知");
```

第二步，新增企业微信通知构建器。

文件位置：`src/main/java/io/github/atengk/pattern/combination/builderfactory/builder/impl/WechatWorkNotificationMessageBuilder.java`

```java
package io.github.atengk.pattern.combination.builderfactory.builder.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.pattern.combination.builderfactory.builder.NotificationMessageBuilder;
import io.github.atengk.pattern.combination.builderfactory.domain.NotificationMessage;
import io.github.atengk.pattern.combination.builderfactory.dto.NotificationCreateRequest;
import io.github.atengk.pattern.combination.builderfactory.enums.NotificationChannelEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 企业微信通知消息构建器
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class WechatWorkNotificationMessageBuilder implements NotificationMessageBuilder {

    /**
     * 当前构建器支持的通知渠道
     *
     * @return 通知渠道
     */
    @Override
    public NotificationChannelEnum channel() {
        return NotificationChannelEnum.WECHAT_WORK;
    }

    /**
     * 构建企业微信通知消息
     *
     * @param request 通知创建请求
     * @return 通知消息
     */
    @Override
    public NotificationMessage build(NotificationCreateRequest request) {
        if (StrUtil.isBlank(request.getRecipient())) {
            throw new IllegalArgumentException("企业微信通知必须填写接收人账号");
        }

        String title = StrUtil.blankToDefault(request.getTitle(), "企业微信通知");

        NotificationMessage message = NotificationMessage.builder()
                .messageNo("MSG" + IdUtil.getSnowflakeNextIdStr())
                .bizNo(request.getBizNo())
                .channel(channel().getCode())
                .targetAddress(request.getRecipient())
                .title(title)
                .content(request.getContent())
                .contentType("MARKDOWN")
                .tags(List.of("WECHAT_WORK", "MARKDOWN"))
                .extraParams(request.getExtraParams())
                .createTime(LocalDateTime.now())
                .build();

        log.info("企业微信通知消息构建完成，业务编号：{}，接收人：{}，消息编号：{}",
                request.getBizNo(), request.getRecipient(), message.getMessageNo());

        return message;
    }
}
```

新增完成后，`NotificationMessageBuilderFactory` 会自动收集新的构建器。只要 `channel()` 不重复，就能通过简单工厂获取新构建器。

## 验证方式

可以从以下几个方面验证组合模式是否生效：

```text
1. 启动项目时，日志应输出通知消息构建器工厂初始化完成
2. 请求 EMAIL 时，应命中 EmailNotificationMessageBuilder
3. 请求 SMS 时，应命中 SmsNotificationMessageBuilder
4. 请求 APP 时，应命中 AppNotificationMessageBuilder
5. 不同渠道构建出的 NotificationMessage 字段不同
6. 新增通知渠道时，不需要修改 NotificationCreateServiceImpl
7. 请求不存在的通知渠道时，应返回 400 错误
```

正常请求日志示例：

```text
通知消息构建器工厂初始化完成，构建器数量：3
命中通知消息构建器，通知渠道：EMAIL，构建器类：EmailNotificationMessageBuilder
邮件通知消息构建完成，业务编号：BIZ202605130001，收件人：user@example.com，消息编号：MSG1988267712300011520
通知消息创建完成，业务编号：BIZ202605130001，通知渠道：EMAIL，消息编号：MSG1988267712300011520
```

短信通知日志示例：

```text
命中通知消息构建器，通知渠道：SMS，构建器类：SmsNotificationMessageBuilder
短信通知消息构建完成，业务编号：BIZ202605130002，手机号：13800138000，消息编号：MSG1988267712300011521
通知消息创建完成，业务编号：BIZ202605130002，通知渠道：SMS，消息编号：MSG1988267712300011521
```

## 组合效果

构建者模式和简单工厂模式组合后，各自负责不同变化点：

| 模式     | 职责          | 在示例中的体现                                                                                                      |
| ------ | ----------- | ------------------------------------------------------------------------------------------------------------ |
| 简单工厂模式 | 根据业务类型选择构建器 | `NotificationMessageBuilderFactory` 根据通知渠道获取构建器                                                              |
| 构建者模式  | 分步骤组装复杂对象   | `EmailNotificationMessageBuilder`、`SmsNotificationMessageBuilder`、`AppNotificationMessageBuilder` 分别构建不同消息对象 |

这种组合适合处理下面两类变化：

```text
第一类变化：对象类型会变化
例如通知渠道从 EMAIL、SMS、APP 扩展到企业微信、钉钉、飞书

第二类变化：对象构建过程复杂
例如不同渠道需要不同字段、默认值、内容格式、标签、扩展参数
```

相比只使用简单工厂模式，这个组合可以避免工厂类中堆积大量对象组装细节。相比只使用构建者模式，这个组合可以避免调用方自己判断类型并手动选择构建器。

## 注意事项

构建者模式和简单工厂模式组合使用时，要明确两者边界：

```text
简单工厂负责选择构建器
构建者负责组装复杂对象
```

不要把具体对象组装逻辑写进简单工厂。简单工厂如果同时负责类型选择和字段组装，就会退化成一个复杂的对象创建中心。

也不要让调用方直接选择具体构建器。调用方只应该传入业务类型，由简单工厂返回对应构建器，这样新增类型时可以减少对业务主流程的影响。

如果对象字段很少、构造过程很简单，直接使用构造方法或 Lombok `@Builder` 就足够了。只有当对象构建过程包含较多默认值、校验、格式转换、字段组合、不同类型差异时，才更适合使用“构建者模式 + 简单工厂模式”。

这个组合最适合的判断标准是：**类型选择归工厂，对象组装归构建者**。
