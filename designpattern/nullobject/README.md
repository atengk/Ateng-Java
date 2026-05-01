# 设计模式：空对象模式

空对象模式用于用一个“什么也不做但行为安全”的对象代替 `null`，从而减少大量空判断和空指针风险。在 JDK21 和 Spring Boot 3 项目中，空对象模式常用于默认策略、默认处理器、默认配置、空用户、空订单、空支付回调、空通知发送器、空权限对象、空查询结果等场景。

需要注意：空对象模式不是 GoF 23 种设计模式之一，属于这次设计模式文档里的“遗漏补充”。它适合“没有对象时仍然希望调用方可以安全调用统一接口”的场景，但不适合掩盖本来应该暴露的业务异常。

## 基础配置

本示例基于 JDK21、Spring Boot 3、Maven 项目。示例包路径统一使用 `io.github.atengk`。

文件位置：`pom.xml`

```xml
<dependencies>
    <!-- Spring Boot Web，用于提供接口验证空对象模式行为 -->
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

    <!-- Lombok，简化日志对象、Getter、构造方法等样板代码 -->
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

空对象模式的核心目标是让调用方不再频繁判断对象是否为 `null`。当真实对象不存在时，返回一个实现了相同接口的空对象。调用方继续调用接口方法，空对象内部提供安全的默认行为。

常见角色如下：

| 角色           | 说明                         |
| -------------- | ---------------------------- |
| AbstractObject | 抽象对象，定义统一行为       |
| RealObject     | 真实对象，执行业务逻辑       |
| NullObject     | 空对象，提供安全默认行为     |
| Client         | 调用方，统一面向抽象对象调用 |

典型结构如下：

```text
Client
    -> UserProfile
        -> RealUserProfile
        -> NullUserProfile
```

没有空对象模式时，调用方经常写成：

```java
UserProfile profile = userProfileService.getProfile(userId);
if (profile != null) {
    String nickname = profile.nickname();
} else {
    String nickname = "游客";
}
```

使用空对象模式后，调用方只需要面向统一接口：

```java
UserProfile profile = userProfileService.getProfile(userId);
String nickname = profile.nickname();
```

空对象模式适合默认行为明确的场景。如果对象不存在本身就是错误，例如支付单不存在、订单不存在、权限记录不存在，就不应该返回空对象掩盖异常。

## 普通 Java 空对象模式

普通 Java 空对象模式适合先理解“真实对象”和“空对象”实现同一接口。下面以用户资料为例，系统可能查到真实用户，也可能查不到用户。查不到用户时返回一个游客资料对象，避免调用方判空。

整体关系如下：

```text
UserProfile
├── RealUserProfile
└── NullUserProfile

UserProfileService
    -> 查到用户返回 RealUserProfile
    -> 查不到用户返回 NullUserProfile
```

### 文件结构

```text
src/main/java/io/github/atengk/design/nullobject/simple/
├── UserProfile.java
├── RealUserProfile.java
├── NullUserProfile.java
└── UserProfileService.java
```

文件位置：`src/main/java/io/github/atengk/design/nullobject/simple/UserProfile.java`

下面是用户资料接口，真实用户和空用户都实现该接口。

```java
package io.github.atengk.design.nullobject.simple;

/**
 * 用户资料接口
 *
 * @author Ateng
 * @since 2026-05-01
 */
public interface UserProfile {

    /**
     * 获取用户ID
     *
     * @return 用户ID
     */
    Long userId();

    /**
     * 获取用户昵称
     *
     * @return 用户昵称
     */
    String nickname();

    /**
     * 获取手机号
     *
     * @return 手机号
     */
    String mobile();

    /**
     * 判断是否为空对象
     *
     * @return true 表示空对象，false 表示真实对象
     */
    boolean isNull();
}
```

文件位置：`src/main/java/io/github/atengk/design/nullobject/simple/RealUserProfile.java`

下面是真实用户资料对象。

```java
package io.github.atengk.design.nullobject.simple;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;

/**
 * 真实用户资料
 *
 * @author Ateng
 * @since 2026-05-01
 */
@Getter
public class RealUserProfile implements UserProfile {

    private final Long userId;
    private final String nickname;
    private final String mobile;

    /**
     * 创建真实用户资料
     *
     * @param userId   用户ID
     * @param nickname 用户昵称
     * @param mobile   手机号
     */
    public RealUserProfile(Long userId, String nickname, String mobile) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("用户ID必须大于0");
        }
        if (StrUtil.hasBlank(nickname, mobile)) {
            throw new IllegalArgumentException("用户昵称和手机号不能为空");
        }

        this.userId = userId;
        this.nickname = nickname;
        this.mobile = mobile;
    }

    /**
     * 获取用户ID
     *
     * @return 用户ID
     */
    @Override
    public Long userId() {
        return userId;
    }

    /**
     * 获取用户昵称
     *
     * @return 用户昵称
     */
    @Override
    public String nickname() {
        return nickname;
    }

    /**
     * 获取手机号
     *
     * @return 手机号
     */
    @Override
    public String mobile() {
        return mobile;
    }

    /**
     * 判断是否为空对象
     *
     * @return false 表示真实对象
     */
    @Override
    public boolean isNull() {
        return false;
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/nullobject/simple/NullUserProfile.java`

下面是空用户资料对象。它不会返回 `null`，而是返回安全默认值。

```java
package io.github.atengk.design.nullobject.simple;

/**
 * 空用户资料
 *
 * @author Ateng
 * @since 2026-05-01
 */
public final class NullUserProfile implements UserProfile {

    /**
     * 空用户资料单例
     */
    public static final NullUserProfile INSTANCE = new NullUserProfile();

    /**
     * 创建空用户资料
     */
    private NullUserProfile() {
    }

    /**
     * 获取用户ID
     *
     * @return 默认用户ID
     */
    @Override
    public Long userId() {
        return 0L;
    }

    /**
     * 获取用户昵称
     *
     * @return 默认昵称
     */
    @Override
    public String nickname() {
        return "游客";
    }

    /**
     * 获取手机号
     *
     * @return 默认手机号
     */
    @Override
    public String mobile() {
        return "";
    }

    /**
     * 判断是否为空对象
     *
     * @return true 表示空对象
     */
    @Override
    public boolean isNull() {
        return true;
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/nullobject/simple/UserProfileService.java`

下面是用户资料服务。查不到用户时返回空对象，而不是返回 `null`。

```java
package io.github.atengk.design.nullobject.simple;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用户资料服务
 *
 * @author Ateng
 * @since 2026-05-01
 */
@Slf4j
public class UserProfileService {

    private final Map<Long, UserProfile> userMap = new ConcurrentHashMap<>();

    /**
     * 创建用户资料服务
     */
    public UserProfileService() {
        userMap.put(10001L, new RealUserProfile(10001L, "Ateng", "13800138000"));
        userMap.put(10002L, new RealUserProfile(10002L, "Blair", "13900139000"));
    }

    /**
     * 根据用户ID获取用户资料
     *
     * @param userId 用户ID
     * @return 用户资料，查不到时返回空用户资料
     */
    public UserProfile getProfile(Long userId) {
        if (userId == null || userId <= 0) {
            log.warn("获取用户资料失败，用户ID不合法，用户ID：{}", userId);
            return NullUserProfile.INSTANCE;
        }

        UserProfile profile = userMap.getOrDefault(userId, NullUserProfile.INSTANCE);
        log.info("获取用户资料完成，用户ID：{}，是否空对象：{}", userId, profile.isNull());
        return profile;
    }
}
```

使用方式：

```java
UserProfileService userProfileService = new UserProfileService();

UserProfile realProfile = userProfileService.getProfile(10001L);
String realNickname = realProfile.nickname();

UserProfile nullProfile = userProfileService.getProfile(99999L);
String defaultNickname = nullProfile.nickname();
```

调用方不需要写 `profile == null` 判断。查不到用户时，`nickname()` 会返回 `游客`，`mobile()` 会返回空字符串。

## Spring Boot 空对象模式

Spring Boot 项目中，空对象模式常用于默认处理器、默认策略、默认通知器等场景。下面以通知发送为例，系统支持短信、邮件、站内信。如果传入未知通知渠道，不直接返回 `null` 或抛空指针，而是返回一个空通知发送器，记录日志并安全返回。

整体流程如下：

```text
Controller
    -> NoticeService
        -> NoticeSenderContext
            -> SmsNoticeSender
            -> EmailNoticeSender
            -> SiteNoticeSender
            -> NullNoticeSender
```

空对象在这里的意义是：未知渠道不会导致空指针，系统可以安全降级。但生产业务中是否允许未知渠道静默降级，需要根据业务决定。

### 文件结构

```text
src/main/java/io/github/atengk/design/
├── NullObjectApplication.java
├── controller/
│   └── NoticeController.java
├── dto/
│   ├── NoticeSendRequest.java
│   └── NoticeSendResponse.java
├── sender/
│   ├── NoticeSender.java
│   ├── SmsNoticeSender.java
│   ├── EmailNoticeSender.java
│   ├── SiteNoticeSender.java
│   └── NullNoticeSender.java
├── context/
│   └── NoticeSenderContext.java
└── service/
    ├── NoticeService.java
    └── impl/
        └── NoticeServiceImpl.java
```

文件位置：`src/main/java/io/github/atengk/design/NullObjectApplication.java`

下面是 Spring Boot 启动类。

```java
package io.github.atengk.design;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 空对象模式示例启动类
 *
 * @author Ateng
 * @since 2026-05-01
 */
@SpringBootApplication
public class NullObjectApplication {

    /**
     * 应用启动入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(NullObjectApplication.class, args);
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/dto/NoticeSendRequest.java`

下面是通知发送请求对象。

```java
package io.github.atengk.design.dto;

/**
 * 通知发送请求
 *
 * @param channel  通知渠道
 * @param receiver 接收人
 * @param title    标题
 * @param content  内容
 * @author Ateng
 * @since 2026-05-01
 */
public record NoticeSendRequest(
        String channel,
        String receiver,
        String title,
        String content
) {
}
```

文件位置：`src/main/java/io/github/atengk/design/dto/NoticeSendResponse.java`

下面是通知发送响应对象。

```java
package io.github.atengk.design.dto;

/**
 * 通知发送响应
 *
 * @param channel  通知渠道
 * @param receiver 接收人
 * @param success  是否成功
 * @param message  响应消息
 * @param bizId    业务ID
 * @author Ateng
 * @since 2026-05-01
 */
public record NoticeSendResponse(
        String channel,
        String receiver,
        Boolean success,
        String message,
        String bizId
) {
}
```

文件位置：`src/main/java/io/github/atengk/design/sender/NoticeSender.java`

下面是通知发送器接口。真实发送器和空发送器都实现该接口。

```java
package io.github.atengk.design.sender;

import io.github.atengk.design.dto.NoticeSendRequest;
import io.github.atengk.design.dto.NoticeSendResponse;

/**
 * 通知发送器
 *
 * @author Ateng
 * @since 2026-05-01
 */
public interface NoticeSender {

    /**
     * 获取支持的通知渠道
     *
     * @return 通知渠道
     */
    String supportChannel();

    /**
     * 发送通知
     *
     * @param request 通知发送请求
     * @return 通知发送响应
     */
    NoticeSendResponse send(NoticeSendRequest request);

    /**
     * 判断是否为空发送器
     *
     * @return true 表示空对象
     */
    default boolean isNull() {
        return false;
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/sender/SmsNoticeSender.java`

下面是短信通知发送器。

```java
package io.github.atengk.design.sender;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.design.dto.NoticeSendRequest;
import io.github.atengk.design.dto.NoticeSendResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 短信通知发送器
 *
 * @author Ateng
 * @since 2026-05-01
 */
@Slf4j
@Component
public class SmsNoticeSender implements NoticeSender {

    /**
     * 获取支持的通知渠道
     *
     * @return 通知渠道
     */
    @Override
    public String supportChannel() {
        return "sms";
    }

    /**
     * 发送短信通知
     *
     * @param request 通知发送请求
     * @return 通知发送响应
     */
    @Override
    public NoticeSendResponse send(NoticeSendRequest request) {
        if (request == null || StrUtil.hasBlank(request.receiver(), request.content())) {
            log.warn("短信通知发送失败，接收人或内容为空");
            throw new IllegalArgumentException("短信接收人和内容不能为空");
        }

        String bizId = "SMS" + IdUtil.getSnowflakeNextId();
        log.info("短信通知发送成功，接收人：{}，标题：{}，业务ID：{}",
                request.receiver(), request.title(), bizId);

        return new NoticeSendResponse(supportChannel(), request.receiver(), true, "短信发送成功", bizId);
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/sender/EmailNoticeSender.java`

下面是邮件通知发送器。

```java
package io.github.atengk.design.sender;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.design.dto.NoticeSendRequest;
import io.github.atengk.design.dto.NoticeSendResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 邮件通知发送器
 *
 * @author Ateng
 * @since 2026-05-01
 */
@Slf4j
@Component
public class EmailNoticeSender implements NoticeSender {

    /**
     * 获取支持的通知渠道
     *
     * @return 通知渠道
     */
    @Override
    public String supportChannel() {
        return "email";
    }

    /**
     * 发送邮件通知
     *
     * @param request 通知发送请求
     * @return 通知发送响应
     */
    @Override
    public NoticeSendResponse send(NoticeSendRequest request) {
        if (request == null || StrUtil.hasBlank(request.receiver(), request.title(), request.content())) {
            log.warn("邮件通知发送失败，接收人、标题或内容为空");
            throw new IllegalArgumentException("邮件接收人、标题和内容不能为空");
        }

        String bizId = "EMAIL" + IdUtil.getSnowflakeNextId();
        log.info("邮件通知发送成功，接收人：{}，标题：{}，业务ID：{}",
                request.receiver(), request.title(), bizId);

        return new NoticeSendResponse(supportChannel(), request.receiver(), true, "邮件发送成功", bizId);
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/sender/SiteNoticeSender.java`

下面是站内信通知发送器。

```java
package io.github.atengk.design.sender;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.design.dto.NoticeSendRequest;
import io.github.atengk.design.dto.NoticeSendResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 站内信通知发送器
 *
 * @author Ateng
 * @since 2026-05-01
 */
@Slf4j
@Component
public class SiteNoticeSender implements NoticeSender {

    /**
     * 获取支持的通知渠道
     *
     * @return 通知渠道
     */
    @Override
    public String supportChannel() {
        return "site";
    }

    /**
     * 发送站内信通知
     *
     * @param request 通知发送请求
     * @return 通知发送响应
     */
    @Override
    public NoticeSendResponse send(NoticeSendRequest request) {
        if (request == null || StrUtil.hasBlank(request.receiver(), request.content())) {
            log.warn("站内信通知发送失败，接收人或内容为空");
            throw new IllegalArgumentException("站内信接收人和内容不能为空");
        }

        String bizId = "SITE" + IdUtil.getSnowflakeNextId();
        log.info("站内信通知发送成功，接收人：{}，标题：{}，业务ID：{}",
                request.receiver(), request.title(), bizId);

        return new NoticeSendResponse(supportChannel(), request.receiver(), true, "站内信发送成功", bizId);
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/sender/NullNoticeSender.java`

下面是空通知发送器。它不执行真实发送，只记录降级日志并返回安全响应。

```java
package io.github.atengk.design.sender;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.design.dto.NoticeSendRequest;
import io.github.atengk.design.dto.NoticeSendResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 空通知发送器
 *
 * @author Ateng
 * @since 2026-05-01
 */
@Slf4j
@Component
public class NullNoticeSender implements NoticeSender {

    /**
     * 获取支持的通知渠道
     *
     * @return 通知渠道
     */
    @Override
    public String supportChannel() {
        return "null";
    }

    /**
     * 执行空通知发送
     *
     * @param request 通知发送请求
     * @return 通知发送响应
     */
    @Override
    public NoticeSendResponse send(NoticeSendRequest request) {
        String channel = request == null ? "unknown" : StrUtil.blankToDefault(request.channel(), "unknown");
        String receiver = request == null ? "" : StrUtil.blankToDefault(request.receiver(), "");

        log.warn("通知渠道未匹配，使用空通知发送器降级处理，渠道：{}，接收人：{}", channel, receiver);

        return new NoticeSendResponse(
                channel,
                receiver,
                false,
                "通知渠道不支持，已安全跳过",
                ""
        );
    }

    /**
     * 判断是否为空发送器
     *
     * @return true 表示空对象
     */
    @Override
    public boolean isNull() {
        return true;
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/context/NoticeSenderContext.java`

下面是通知发送器上下文。找不到真实发送器时返回空发送器。

```java
package io.github.atengk.design.context;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.design.sender.NoticeSender;
import io.github.atengk.design.sender.NullNoticeSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 通知发送器上下文
 *
 * @author Ateng
 * @since 2026-05-01
 */
@Slf4j
@Component
public class NoticeSenderContext {

    private final Map<String, NoticeSender> senderMap;
    private final NullNoticeSender nullNoticeSender;

    /**
     * 创建通知发送器上下文
     *
     * @param senders          通知发送器列表
     * @param nullNoticeSender 空通知发送器
     */
    public NoticeSenderContext(List<NoticeSender> senders, NullNoticeSender nullNoticeSender) {
        this.nullNoticeSender = nullNoticeSender;

        if (CollUtil.isEmpty(senders)) {
            log.warn("通知发送器列表为空");
            this.senderMap = Map.of();
            return;
        }

        this.senderMap = senders.stream()
                .filter(sender -> !sender.isNull())
                .collect(Collectors.toUnmodifiableMap(
                        sender -> StrUtil.trim(sender.supportChannel()).toLowerCase(),
                        Function.identity()
                ));

        log.info("初始化通知发送器上下文完成，支持渠道：{}", senderMap.keySet());
    }

    /**
     * 根据渠道获取通知发送器
     *
     * @param channel 通知渠道
     * @return 通知发送器，找不到时返回空发送器
     */
    public NoticeSender getSender(String channel) {
        if (StrUtil.isBlank(channel)) {
            log.warn("获取通知发送器失败，渠道为空，返回空通知发送器");
            return nullNoticeSender;
        }

        NoticeSender sender = senderMap.get(StrUtil.trim(channel).toLowerCase());
        if (sender == null) {
            log.warn("获取通知发送器失败，不支持的渠道：{}，返回空通知发送器", channel);
            return nullNoticeSender;
        }

        return sender;
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/service/NoticeService.java`

下面是通知服务接口。

```java
package io.github.atengk.design.service;

import io.github.atengk.design.dto.NoticeSendRequest;
import io.github.atengk.design.dto.NoticeSendResponse;

/**
 * 通知服务
 *
 * @author Ateng
 * @since 2026-05-01
 */
public interface NoticeService {

    /**
     * 发送通知
     *
     * @param request 通知发送请求
     * @return 通知发送响应
     */
    NoticeSendResponse send(NoticeSendRequest request);
}
```

文件位置：`src/main/java/io/github/atengk/design/service/impl/NoticeServiceImpl.java`

下面是通知服务实现。它不再判断发送器是否为 `null`，而是统一调用发送器接口。

```java
package io.github.atengk.design.service.impl;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.design.context.NoticeSenderContext;
import io.github.atengk.design.dto.NoticeSendRequest;
import io.github.atengk.design.dto.NoticeSendResponse;
import io.github.atengk.design.sender.NoticeSender;
import io.github.atengk.design.service.NoticeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 通知服务实现
 *
 * @author Ateng
 * @since 2026-05-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NoticeServiceImpl implements NoticeService {

    private final NoticeSenderContext noticeSenderContext;

    /**
     * 发送通知
     *
     * @param request 通知发送请求
     * @return 通知发送响应
     */
    @Override
    public NoticeSendResponse send(NoticeSendRequest request) {
        validateRequest(request);

        NoticeSender sender = noticeSenderContext.getSender(request.channel());
        NoticeSendResponse response = sender.send(request);

        log.info("通知发送流程完成，渠道：{}，接收人：{}，是否空发送器：{}，结果：{}",
                request.channel(), request.receiver(), sender.isNull(), response.success());

        return response;
    }

    /**
     * 校验通知发送请求
     *
     * @param request 通知发送请求
     */
    private void validateRequest(NoticeSendRequest request) {
        if (request == null) {
            log.warn("发送通知失败，请求参数为空");
            throw new IllegalArgumentException("请求参数不能为空");
        }

        if (StrUtil.hasBlank(request.receiver(), request.content())) {
            log.warn("发送通知失败，接收人或内容为空");
            throw new IllegalArgumentException("接收人和内容不能为空");
        }
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/controller/NoticeController.java`

下面是通知接口，用于验证空对象模式效果。

```java
package io.github.atengk.design.controller;

import io.github.atengk.design.dto.NoticeSendRequest;
import io.github.atengk.design.dto.NoticeSendResponse;
import io.github.atengk.design.service.NoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 通知控制器
 *
 * @author Ateng
 * @since 2026-05-01
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/null-object/notice")
public class NoticeController {

    private final NoticeService noticeService;

    /**
     * 发送通知
     *
     * @param channel  通知渠道
     * @param receiver 接收人
     * @param title    标题
     * @param content  内容
     * @return 通知发送响应
     */
    @PostMapping("/send")
    public NoticeSendResponse send(@RequestParam String channel,
                                   @RequestParam String receiver,
                                   @RequestParam(required = false) String title,
                                   @RequestParam String content) {
        NoticeSendRequest request = new NoticeSendRequest(channel, receiver, title, content);
        return noticeService.send(request);
    }
}
```

## 使用方式

启动 Spring Boot 项目：

```bash
mvn spring-boot:run
```

发送短信通知：

```bash
curl -X POST "http://localhost:8080/null-object/notice/send?channel=sms&receiver=13800138000&title=注册成功&content=欢迎注册"
```

可能返回：

```json
{
  "channel": "sms",
  "receiver": "13800138000",
  "success": true,
  "message": "短信发送成功",
  "bizId": "SMS2020123456789017600"
}
```

发送未知渠道通知：

```bash
curl -X POST "http://localhost:8080/null-object/notice/send?channel=dingding&receiver=ateng&title=系统通知&content=这是一条测试通知"
```

可能返回：

```json
{
  "channel": "dingding",
  "receiver": "ateng",
  "success": false,
  "message": "通知渠道不支持，已安全跳过",
  "bizId": ""
}
```

如果空对象模式正常，可以看到类似日志：

```text
初始化通知发送器上下文完成，支持渠道：[sms, email, site]
获取通知发送器失败，不支持的渠道：dingding，返回空通知发送器
通知渠道未匹配，使用空通知发送器降级处理，渠道：dingding，接收人：ateng
通知发送流程完成，渠道：dingding，接收人：ateng，是否空发送器：true，结果：false
```

重点观察未知渠道请求。它不会出现空指针异常，也不需要调用方写 `if (sender == null)`。

## 默认策略场景

空对象模式经常和策略模式一起使用。策略不存在时，不返回 `null`，而是返回默认策略或空策略。

下面以折扣计算为例。未知折扣类型时返回 `NoDiscountPolicy`，表示不打折。

### 文件结构

```text
src/main/java/io/github/atengk/design/discount/
├── DiscountPolicy.java
├── FullReductionDiscountPolicy.java
├── PercentDiscountPolicy.java
├── NoDiscountPolicy.java
└── DiscountPolicyContext.java
```

文件位置：`src/main/java/io/github/atengk/design/discount/DiscountPolicy.java`

下面是折扣策略接口。

```java
package io.github.atengk.design.discount;

import java.math.BigDecimal;

/**
 * 折扣策略
 *
 * @author Ateng
 * @since 2026-05-01
 */
public interface DiscountPolicy {

    /**
     * 获取折扣类型
     *
     * @return 折扣类型
     */
    String discountType();

    /**
     * 计算折后金额
     *
     * @param originAmount 原始金额
     * @return 折后金额
     */
    BigDecimal calculate(BigDecimal originAmount);

    /**
     * 判断是否为空策略
     *
     * @return true 表示空策略
     */
    default boolean isNull() {
        return false;
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/discount/FullReductionDiscountPolicy.java`

下面是满减折扣策略。

```java
package io.github.atengk.design.discount;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 满减折扣策略
 *
 * @author Ateng
 * @since 2026-05-01
 */
@Slf4j
@Component
public class FullReductionDiscountPolicy implements DiscountPolicy {

    /**
     * 获取折扣类型
     *
     * @return 折扣类型
     */
    @Override
    public String discountType() {
        return "full_reduction";
    }

    /**
     * 计算满减后金额
     *
     * @param originAmount 原始金额
     * @return 折后金额
     */
    @Override
    public BigDecimal calculate(BigDecimal originAmount) {
        if (originAmount == null || originAmount.compareTo(BigDecimal.ZERO) < 0) {
            log.warn("满减折扣计算失败，原始金额不合法，金额：{}", originAmount);
            throw new IllegalArgumentException("原始金额不能小于0");
        }

        BigDecimal result = originAmount.compareTo(BigDecimal.valueOf(100)) >= 0
                ? originAmount.subtract(BigDecimal.TEN)
                : originAmount;

        log.info("满减折扣计算完成，原始金额：{}，折后金额：{}", originAmount, result);
        return result;
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/discount/PercentDiscountPolicy.java`

下面是百分比折扣策略。

```java
package io.github.atengk.design.discount;

import cn.hutool.core.util.NumberUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 百分比折扣策略
 *
 * @author Ateng
 * @since 2026-05-01
 */
@Slf4j
@Component
public class PercentDiscountPolicy implements DiscountPolicy {

    /**
     * 获取折扣类型
     *
     * @return 折扣类型
     */
    @Override
    public String discountType() {
        return "percent";
    }

    /**
     * 计算百分比折扣后金额
     *
     * @param originAmount 原始金额
     * @return 折后金额
     */
    @Override
    public BigDecimal calculate(BigDecimal originAmount) {
        if (originAmount == null || originAmount.compareTo(BigDecimal.ZERO) < 0) {
            log.warn("百分比折扣计算失败，原始金额不合法，金额：{}", originAmount);
            throw new IllegalArgumentException("原始金额不能小于0");
        }

        BigDecimal result = NumberUtil.mul(originAmount, BigDecimal.valueOf(0.9));
        log.info("百分比折扣计算完成，原始金额：{}，折后金额：{}", originAmount, result);
        return result;
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/discount/NoDiscountPolicy.java`

下面是空折扣策略。它不做任何优惠，直接返回原始金额。

```java
package io.github.atengk.design.discount;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 无折扣空策略
 *
 * @author Ateng
 * @since 2026-05-01
 */
@Slf4j
@Component
public class NoDiscountPolicy implements DiscountPolicy {

    /**
     * 获取折扣类型
     *
     * @return 折扣类型
     */
    @Override
    public String discountType() {
        return "none";
    }

    /**
     * 计算无折扣金额
     *
     * @param originAmount 原始金额
     * @return 原始金额
     */
    @Override
    public BigDecimal calculate(BigDecimal originAmount) {
        if (originAmount == null || originAmount.compareTo(BigDecimal.ZERO) < 0) {
            log.warn("无折扣计算失败，原始金额不合法，金额：{}", originAmount);
            throw new IllegalArgumentException("原始金额不能小于0");
        }

        log.info("使用无折扣空策略，原始金额：{}", originAmount);
        return originAmount;
    }

    /**
     * 判断是否为空策略
     *
     * @return true 表示空策略
     */
    @Override
    public boolean isNull() {
        return true;
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/discount/DiscountPolicyContext.java`

下面是折扣策略上下文。找不到策略时返回无折扣空策略。

```java
package io.github.atengk.design.discount;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 折扣策略上下文
 *
 * @author Ateng
 * @since 2026-05-01
 */
@Slf4j
@Component
public class DiscountPolicyContext {

    private final Map<String, DiscountPolicy> policyMap;
    private final NoDiscountPolicy noDiscountPolicy;

    /**
     * 创建折扣策略上下文
     *
     * @param policies         折扣策略列表
     * @param noDiscountPolicy 无折扣空策略
     */
    public DiscountPolicyContext(List<DiscountPolicy> policies, NoDiscountPolicy noDiscountPolicy) {
        this.noDiscountPolicy = noDiscountPolicy;

        if (CollUtil.isEmpty(policies)) {
            log.warn("折扣策略列表为空");
            this.policyMap = Map.of();
            return;
        }

        this.policyMap = policies.stream()
                .filter(policy -> !policy.isNull())
                .collect(Collectors.toUnmodifiableMap(
                        policy -> StrUtil.trim(policy.discountType()).toLowerCase(),
                        Function.identity()
                ));

        log.info("初始化折扣策略上下文完成，支持折扣类型：{}", policyMap.keySet());
    }

    /**
     * 获取折扣策略
     *
     * @param discountType 折扣类型
     * @return 折扣策略
     */
    public DiscountPolicy getPolicy(String discountType) {
        if (StrUtil.isBlank(discountType)) {
            log.warn("折扣类型为空，返回无折扣空策略");
            return noDiscountPolicy;
        }

        DiscountPolicy policy = policyMap.get(StrUtil.trim(discountType).toLowerCase());
        if (policy == null) {
            log.warn("折扣类型不支持，返回无折扣空策略，折扣类型：{}", discountType);
            return noDiscountPolicy;
        }

        return policy;
    }
}
```

这种写法适合“没有匹配策略时可以安全使用默认行为”的场景。对折扣来说，未知折扣类型按无折扣处理是可接受的。但对支付、退款、库存扣减这类核心业务，未知类型通常应该抛异常，而不是静默返回空对象。

## 空对象模式和 Optional 的区别

空对象模式和 `Optional` 都能减少空指针问题，但语义不同。

| 对比项           | 空对象模式                   | Optional               |
| ---------------- | ---------------------------- | ---------------------- |
| 核心目的         | 提供安全默认行为             | 显式表达可能不存在     |
| 调用方式         | 继续调用统一接口             | 调用方处理存在或不存在 |
| 适合场景         | 默认处理器、默认策略、空用户 | 查询结果可能不存在     |
| 是否有行为       | 有行为，只是行为为空或默认   | 通常只是容器           |
| 是否可能掩盖错误 | 可能                         | 较少                   |

简单理解：

```text
空对象模式：给你一个不会炸的默认对象。
Optional：明确告诉你对象可能不存在。
```

如果调用方确实需要区分存在和不存在，`Optional` 更清晰。
如果调用方只需要统一调用行为，空对象模式更简洁。

示例：

```java
Optional<UserProfile> profile = userRepository.findById(userId);
```

适合表达“用户可能不存在”。

```java
NoticeSender sender = noticeSenderContext.getSender(channel);
sender.send(request);
```

适合表达“找不到发送器时走空发送器安全跳过”。

## 空对象模式和默认策略的区别

空对象模式经常表现为默认策略。默认策略不一定是空对象，但空对象通常可以作为一种默认策略。

| 对比项         | 空对象                 | 默认策略                      |
| -------------- | ---------------------- | ----------------------------- |
| 核心语义       | 什么也不做或返回默认值 | 使用默认业务算法              |
| 是否表示不存在 | 通常表示不存在         | 不一定                        |
| 示例           | `NullNoticeSender`     | `DefaultPriceCalculatePolicy` |
| 结果           | 安全跳过、默认空值     | 正常业务结果                  |
| 风险           | 可能静默吞掉错误       | 可能业务语义不准确            |

如果默认行为是“无操作”，通常更接近空对象。
如果默认行为是“标准算法”，通常更接近默认策略。

## 空对象模式和异常处理的边界

空对象模式不能替代异常处理。对象不存在时到底返回空对象还是抛异常，取决于业务语义。

适合返回空对象：

```text
游客用户资料
默认通知发送器
默认折扣策略
空权限集合
空购物车
空报表数据
无操作审计器
```

适合抛异常：

```text
订单不存在
支付单不存在
库存记录不存在
用户登录态不存在
退款记录不存在
核心配置不存在
不支持的支付渠道
```

简单判断原则：

```text
不存在是正常业务分支：可以使用空对象。
不存在是业务错误：应该抛异常。
```

例如查询用户头像时，用户没有头像可以返回默认头像对象。
但支付时找不到支付渠道，通常不应该返回空支付渠道并继续成功，而应该明确失败。

## 验证方式

启动 Spring Boot 项目：

```bash
mvn spring-boot:run
```

发送支持的短信通知：

```bash
curl -X POST "http://localhost:8080/null-object/notice/send?channel=sms&receiver=13800138000&title=注册成功&content=欢迎注册"
```

发送不支持的通知渠道：

```bash
curl -X POST "http://localhost:8080/null-object/notice/send?channel=dingding&receiver=ateng&title=系统通知&content=这是一条测试通知"
```

如果空对象模式正常，可以看到类似日志：

```text
初始化通知发送器上下文完成，支持渠道：[sms, email, site]
短信通知发送成功，接收人：13800138000，标题：注册成功，业务ID：SMS2020123456789017600
通知发送流程完成，渠道：sms，接收人：13800138000，是否空发送器：false，结果：true
获取通知发送器失败，不支持的渠道：dingding，返回空通知发送器
通知渠道未匹配，使用空通知发送器降级处理，渠道：dingding，接收人：ateng
通知发送流程完成，渠道：dingding，接收人：ateng，是否空发送器：true，结果：false
```

重点验证两个结果：

```text
支持渠道：执行真实发送器
未知渠道：执行空发送器，不出现 NullPointerException
```

## 注意事项

空对象模式适合提供安全默认行为，但不要滥用。过度使用空对象可能掩盖真实错误，让问题延迟暴露。

适合使用空对象模式的场景：

```text
默认通知发送器
默认折扣策略
空用户资料
空购物车
空权限集合
空报表结果
无操作审计器
无操作回调处理器
```

不太适合使用空对象模式的场景：

```text
核心业务对象必须存在
不存在应该阻断流程
调用方必须知道失败原因
空对象会导致数据被误认为成功
后续流程依赖真实对象状态
```

不要让空对象返回看似成功的结果。空对象可以安全跳过，但最好明确标记 `success=false` 或 `isNull=true`。

不推荐：

```java
return new NoticeSendResponse(channel, receiver, true, "发送成功", "");
```

推荐：

```java
return new NoticeSendResponse(channel, receiver, false, "通知渠道不支持，已安全跳过", "");
```

空对象最好设计为不可变、无状态、线程安全。Spring Bean 默认单例，空对象中不要保存请求级状态。

错误示例：

```java
private String currentReceiver;
private String currentChannel;
```

推荐使用方法参数：

```java
public NoticeSendResponse send(NoticeSendRequest request) {
    String channel = request.channel();
    return new NoticeSendResponse(channel, request.receiver(), false, "已跳过", "");
}
```

空对象需要有明显命名，建议使用 `Null`、`Noop`、`Empty` 前缀。

常见命名：

```text
NullNoticeSender
NoopAuditLogger
EmptyCart
NullUserProfile
NoDiscountPolicy
EmptyPermissionSet
```

不同命名语义略有区别：

```text
Null：强调对象不存在
Noop：强调什么也不做
Empty：强调空集合或空结果
Default：强调默认业务行为
```

如果空对象的处理需要被监控，建议记录日志或指标。特别是未知渠道、未知策略、未知处理器这类情况，不能完全静默。

推荐日志：

```java
log.warn("通知渠道未匹配，使用空通知发送器降级处理，渠道：{}", channel);
```

如果未知类型频繁出现，说明调用方参数、配置或路由可能有问题，需要尽早排查。

## 总结

在 JDK21 和 Spring Boot 3 项目中，空对象模式的实践重点是用一个行为安全的对象代替 `null`，让调用方统一面向接口编程，减少空判断和空指针异常。

普通 Java 空对象模式适合理解真实对象和空对象实现同一接口。Spring Boot 项目中更常见的是“接口 + 多个真实实现 + 空实现 + 上下文选择器”的结构。对于通知发送器、折扣策略、默认处理器、空用户资料、空权限集合等场景，空对象模式可以让代码更稳定、更简洁。

空对象模式不是异常处理的替代品。它最适合处理“不存在是正常业务分支，并且有明确安全默认行为”的场景。实际落地时，需要重点关注是否会掩盖业务错误、空对象返回值是否清晰、是否记录降级日志、是否保存请求级状态，以及调用方是否需要感知对象不存在。
