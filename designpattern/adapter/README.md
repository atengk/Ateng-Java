# 设计模式：适配器模式

适配器模式用于把一个类的接口转换成调用方期望的另一个接口，让原本接口不兼容的类可以协同工作。在 JDK21 和 Spring Boot 3 项目中，适配器模式常用于第三方 SDK 接入、旧系统接口兼容、统一支付渠道、统一短信发送、统一文件存储、统一物流查询、数据格式转换等场景。

需要注意：适配器模式关注的是“接口不兼容时做转换”。如果是给对象叠加增强能力，更适合装饰器模式；如果是控制对象访问，更适合代理模式；如果是根据类型选择不同算法，更适合策略模式。

## 基础配置

本示例基于 JDK21、Spring Boot 3、Maven 项目。示例包路径统一使用 `io.github.atengk`。

文件位置：`pom.xml`

```xml
<dependencies>
    <!-- Spring Boot Web，用于提供接口验证适配器模式行为 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Hutool 工具类，用于字符串、ID、金额转换等通用处理 -->
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

适配器模式的核心目标是让调用方依赖统一接口，适配器负责把统一接口转换成旧系统、第三方 SDK 或特殊组件需要的接口格式。

常见角色如下：

| 角色    | 说明                                              |
| ------- | ------------------------------------------------- |
| Target  | 目标接口，也就是调用方期望使用的统一接口          |
| Adaptee | 被适配对象，通常是旧系统接口、第三方 SDK 或已有类 |
| Adapter | 适配器，负责实现目标接口，并在内部调用被适配对象  |
| Client  | 调用方，只依赖目标接口，不关心底层真实接口        |

常见实现方式如下：

| 实现方式           | 是否推荐 | 适用场景                                  |
| ------------------ | -------- | ----------------------------------------- |
| 对象适配器         | 推荐     | 通过组合持有被适配对象，Java 项目最常用   |
| 类适配器           | 较少使用 | 通过继承适配，被 Java 单继承限制影响      |
| Spring Bean 适配器 | 强烈推荐 | Spring Boot 项目中统一第三方 SDK 或旧接口 |
| 双向适配器         | 谨慎使用 | 两套接口需要互相兼容，复杂度较高          |
| 直接修改旧接口     | 不推荐   | 容易破坏已有调用方和历史逻辑              |

在 Spring Boot 项目中，常见优先级通常是：

```text
Spring Bean 适配器 > 对象适配器 > 类适配器
```

对象适配器使用组合方式，不要求继承被适配对象，更符合 Java 项目的常见设计习惯。

## 普通 Java 对象适配器

普通 Java 对象适配器适合不依赖 Spring 容器的接口兼容场景。下面以短信发送为例，新系统希望使用统一的 `MessageSender` 接口，但旧短信客户端只提供 `sendSms` 方法。

整体转换关系如下：

```text
新系统统一接口 MessageSender
    -> SmsClientAdapter
        -> 旧短信客户端 LegacySmsClient
```

### 文件结构

```text
src/main/java/io/github/atengk/design/adapter/simple/
├── MessageRequest.java
├── MessageSender.java
├── LegacySmsClient.java
├── LegacySmsResult.java
└── SmsClientAdapter.java
```

文件位置：`src/main/java/io/github/atengk/design/adapter/simple/MessageRequest.java`

下面是新系统期望使用的统一消息请求对象。

```java
package io.github.atengk.design.adapter.simple;

/**
 * 消息发送请求
 *
 * @param receiver 接收人
 * @param content  消息内容
 * @author Ateng
 * @since 2026-04-30
 */
public record MessageRequest(String receiver, String content) {
}
```

文件位置：`src/main/java/io/github/atengk/design/adapter/simple/MessageSender.java`

下面是新系统定义的统一消息发送接口。

```java
package io.github.atengk.design.adapter.simple;

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
     * @param request 消息发送请求
     * @return 发送结果
     */
    String send(MessageRequest request);
}
```

文件位置：`src/main/java/io/github/atengk/design/adapter/simple/LegacySmsResult.java`

下面是旧短信客户端返回的结果对象。

```java
package io.github.atengk.design.adapter.simple;

/**
 * 旧短信发送结果
 *
 * @param success 是否成功
 * @param code    响应编码
 * @param message 响应消息
 * @author Ateng
 * @since 2026-04-30
 */
public record LegacySmsResult(Boolean success, String code, String message) {
}
```

文件位置：`src/main/java/io/github/atengk/design/adapter/simple/LegacySmsClient.java`

下面是旧短信客户端，接口风格和新系统不一致。

```java
package io.github.atengk.design.adapter.simple;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 旧短信客户端
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
public class LegacySmsClient {

    /**
     * 发送短信
     *
     * @param mobile  手机号
     * @param text    短信内容
     * @param traceNo 请求流水号
     * @return 旧短信发送结果
     */
    public LegacySmsResult sendSms(String mobile, String text, String traceNo) {
        if (StrUtil.hasBlank(mobile, text, traceNo)) {
            log.warn("旧短信客户端发送失败，手机号、内容或流水号为空");
            return new LegacySmsResult(false, "400", "参数不完整");
        }

        log.info("旧短信客户端发送短信，手机号：{}，内容：{}，流水号：{}", mobile, text, traceNo);
        return new LegacySmsResult(true, "200", "发送成功，bizId=" + IdUtil.fastSimpleUUID());
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/adapter/simple/SmsClientAdapter.java`

下面是短信客户端适配器。它实现新系统的 `MessageSender` 接口，并在内部调用旧短信客户端。

```java
package io.github.atengk.design.adapter.simple;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 短信客户端适配器
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
public class SmsClientAdapter implements MessageSender {

    private final LegacySmsClient legacySmsClient;

    /**
     * 创建短信客户端适配器
     *
     * @param legacySmsClient 旧短信客户端
     */
    public SmsClientAdapter(LegacySmsClient legacySmsClient) {
        if (legacySmsClient == null) {
            throw new IllegalArgumentException("旧短信客户端不能为空");
        }
        this.legacySmsClient = legacySmsClient;
    }

    /**
     * 发送消息
     *
     * @param request 消息发送请求
     * @return 发送结果
     */
    @Override
    public String send(MessageRequest request) {
        validateRequest(request);

        String traceNo = IdUtil.fastSimpleUUID();
        LegacySmsResult result = legacySmsClient.sendSms(request.receiver(), request.content(), traceNo);

        if (!Boolean.TRUE.equals(result.success())) {
            log.warn("短信适配器发送失败，接收人：{}，响应编码：{}，响应消息：{}",
                    request.receiver(), result.code(), result.message());
            throw new IllegalStateException("短信发送失败：" + result.message());
        }

        log.info("短信适配器发送成功，接收人：{}，流水号：{}", request.receiver(), traceNo);
        return result.message();
    }

    /**
     * 校验消息请求
     *
     * @param request 消息发送请求
     */
    private void validateRequest(MessageRequest request) {
        if (request == null) {
            log.warn("短信适配器发送失败，请求参数为空");
            throw new IllegalArgumentException("请求参数不能为空");
        }

        if (StrUtil.hasBlank(request.receiver(), request.content())) {
            log.warn("短信适配器发送失败，接收人或内容为空");
            throw new IllegalArgumentException("接收人和内容不能为空");
        }
    }
}
```

使用方式：

```java
MessageSender messageSender = new SmsClientAdapter(new LegacySmsClient());

String result = messageSender.send(new MessageRequest(
        "13800138000",
        "你的订单已支付成功"
));
```

调用方只依赖 `MessageSender`，不需要关心旧短信客户端的 `sendSms` 参数结构，也不需要处理旧接口返回格式。

## Spring Boot 适配器

Spring Boot 项目中更常见的写法，是将第三方 SDK 或旧系统客户端注册为 Spring Bean，然后为每个外部系统编写一个适配器 Bean。业务层只依赖统一接口。

下面以支付场景为例，系统内部希望使用统一的支付接口 `PaymentAdapter`，但支付宝旧接口和微信新接口的参数格式完全不同。

整体关系如下：

```text
PaymentController
    -> PaymentContext
        -> PaymentAdapter
            -> AlipayLegacyClient
            -> WechatV3Client
```

示例支持两个支付渠道：

```text
alipay  支付宝旧接口
wechat  微信V3接口
```

### 文件结构

```text
src/main/java/io/github/atengk/design/
├── AdapterApplication.java
├── config/
│   └── ThirdPayClientConfig.java
├── controller/
│   └── PaymentController.java
├── context/
│   └── PaymentContext.java
├── dto/
│   ├── PaymentRequest.java
│   ├── PaymentResponse.java
│   └── WechatPayCommand.java
├── external/
│   ├── AlipayLegacyClient.java
│   ├── AlipayLegacyResult.java
│   ├── WechatV3Client.java
│   └── WechatV3Result.java
└── adapter/
    ├── PaymentAdapter.java
    ├── AlipayPaymentAdapter.java
    └── WechatPaymentAdapter.java
```

文件位置：`src/main/java/io/github/atengk/design/AdapterApplication.java`

下面是 Spring Boot 启动类。

```java
package io.github.atengk.design;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 适配器模式示例启动类
 *
 * @author Ateng
 * @since 2026-04-30
 */
@SpringBootApplication
public class AdapterApplication {

    /**
     * 应用启动入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(AdapterApplication.class, args);
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/dto/PaymentRequest.java`

下面是系统内部统一的支付请求对象。

```java
package io.github.atengk.design.dto;

import java.math.BigDecimal;

/**
 * 支付请求
 *
 * @param channel 支付渠道
 * @param orderNo 订单号
 * @param userId  用户ID
 * @param amount  支付金额，单位：元
 * @param subject 订单标题
 * @author Ateng
 * @since 2026-04-30
 */
public record PaymentRequest(
        String channel,
        String orderNo,
        Long userId,
        BigDecimal amount,
        String subject
) {
}
```

文件位置：`src/main/java/io/github/atengk/design/dto/PaymentResponse.java`

下面是系统内部统一的支付响应对象。

```java
package io.github.atengk.design.dto;

/**
 * 支付响应
 *
 * @param channel 支付渠道
 * @param orderNo 订单号
 * @param tradeNo 第三方交易号
 * @param success 是否成功
 * @param message 响应消息
 * @author Ateng
 * @since 2026-04-30
 */
public record PaymentResponse(
        String channel,
        String orderNo,
        String tradeNo,
        Boolean success,
        String message
) {
}
```

文件位置：`src/main/java/io/github/atengk/design/dto/WechatPayCommand.java`

下面是微信 V3 客户端需要的支付命令对象，它和系统内部统一请求对象不一致。

```java
package io.github.atengk.design.dto;

/**
 * 微信支付命令
 *
 * @param outTradeNo 商户订单号
 * @param payerId    支付用户ID
 * @param amountFen  支付金额，单位：分
 * @param description 商品描述
 * @author Ateng
 * @since 2026-04-30
 */
public record WechatPayCommand(
        String outTradeNo,
        String payerId,
        Long amountFen,
        String description
) {
}
```

文件位置：`src/main/java/io/github/atengk/design/external/AlipayLegacyResult.java`

下面是支付宝旧接口返回结果。

```java
package io.github.atengk.design.external;

/**
 * 支付宝旧接口结果
 *
 * @param code    响应编码
 * @param tradeNo 支付宝交易号
 * @param msg     响应消息
 * @author Ateng
 * @since 2026-04-30
 */
public record AlipayLegacyResult(String code, String tradeNo, String msg) {
}
```

文件位置：`src/main/java/io/github/atengk/design/external/AlipayLegacyClient.java`

下面模拟支付宝旧客户端。它使用字符串订单号和分为单位的金额参数。

```java
package io.github.atengk.design.external;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 支付宝旧客户端
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
public class AlipayLegacyClient {

    /**
     * 发起支付宝支付
     *
     * @param outTradeNo 商户订单号
     * @param amountFen  支付金额，单位：分
     * @param title      订单标题
     * @return 支付宝旧接口结果
     */
    public AlipayLegacyResult executePay(String outTradeNo, Long amountFen, String title) {
        if (StrUtil.isBlank(outTradeNo) || amountFen == null || amountFen <= 0) {
            log.warn("支付宝旧客户端支付失败，订单号或金额不合法");
            return new AlipayLegacyResult("400", null, "参数不合法");
        }

        String tradeNo = "ALI" + IdUtil.getSnowflakeNextId();
        log.info("调用支付宝旧客户端，订单号：{}，金额分：{}，标题：{}，交易号：{}",
                outTradeNo, amountFen, title, tradeNo);

        return new AlipayLegacyResult("10000", tradeNo, "支付成功");
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/external/WechatV3Result.java`

下面是微信 V3 客户端返回结果。

```java
package io.github.atengk.design.external;

/**
 * 微信V3支付结果
 *
 * @param success       是否成功
 * @param transactionId 微信交易号
 * @param message       响应消息
 * @author Ateng
 * @since 2026-04-30
 */
public record WechatV3Result(Boolean success, String transactionId, String message) {
}
```

文件位置：`src/main/java/io/github/atengk/design/external/WechatV3Client.java`

下面模拟微信 V3 客户端。它接收的是 `WechatPayCommand` 对象。

```java
package io.github.atengk.design.external;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.design.dto.WechatPayCommand;
import lombok.extern.slf4j.Slf4j;

/**
 * 微信V3客户端
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
public class WechatV3Client {

    /**
     * 创建微信支付交易
     *
     * @param command 微信支付命令
     * @return 微信V3支付结果
     */
    public WechatV3Result createTransaction(WechatPayCommand command) {
        if (command == null || StrUtil.isBlank(command.outTradeNo()) || command.amountFen() == null || command.amountFen() <= 0) {
            log.warn("微信V3客户端支付失败，支付命令不合法");
            return new WechatV3Result(false, null, "参数不合法");
        }

        String transactionId = "WX" + IdUtil.getSnowflakeNextId();
        log.info("调用微信V3客户端，订单号：{}，支付用户：{}，金额分：{}，交易号：{}",
                command.outTradeNo(), command.payerId(), command.amountFen(), transactionId);

        return new WechatV3Result(true, transactionId, "支付成功");
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/config/ThirdPayClientConfig.java`

下面是第三方支付客户端配置，将模拟的外部客户端注册到 Spring 容器中。

```java
package io.github.atengk.design.config;

import io.github.atengk.design.external.AlipayLegacyClient;
import io.github.atengk.design.external.WechatV3Client;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 第三方支付客户端配置
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Configuration
public class ThirdPayClientConfig {

    /**
     * 创建支付宝旧客户端
     *
     * @return 支付宝旧客户端
     */
    @Bean
    public AlipayLegacyClient alipayLegacyClient() {
        log.info("初始化支付宝旧客户端");
        return new AlipayLegacyClient();
    }

    /**
     * 创建微信V3客户端
     *
     * @return 微信V3客户端
     */
    @Bean
    public WechatV3Client wechatV3Client() {
        log.info("初始化微信V3客户端");
        return new WechatV3Client();
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/adapter/PaymentAdapter.java`

下面是系统内部统一支付适配器接口。业务层只依赖该接口，不直接依赖第三方客户端。

```java
package io.github.atengk.design.adapter;

import io.github.atengk.design.dto.PaymentRequest;
import io.github.atengk.design.dto.PaymentResponse;

/**
 * 支付适配器
 *
 * @author Ateng
 * @since 2026-04-30
 */
public interface PaymentAdapter {

    /**
     * 获取支持的支付渠道
     *
     * @return 支付渠道
     */
    String supportChannel();

    /**
     * 执行支付
     *
     * @param request 支付请求
     * @return 支付响应
     */
    PaymentResponse pay(PaymentRequest request);
}
```

文件位置：`src/main/java/io/github/atengk/design/adapter/AlipayPaymentAdapter.java`

下面是支付宝适配器，负责把系统内部支付请求转换成支付宝旧客户端需要的参数。

```java
package io.github.atengk.design.adapter;

import cn.hutool.core.util.NumberUtil;
import io.github.atengk.design.dto.PaymentRequest;
import io.github.atengk.design.dto.PaymentResponse;
import io.github.atengk.design.external.AlipayLegacyClient;
import io.github.atengk.design.external.AlipayLegacyResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 支付宝支付适配器
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlipayPaymentAdapter implements PaymentAdapter {

    private static final String SUCCESS_CODE = "10000";

    private final AlipayLegacyClient alipayLegacyClient;

    /**
     * 获取支持的支付渠道
     *
     * @return 支付渠道
     */
    @Override
    public String supportChannel() {
        return "alipay";
    }

    /**
     * 执行支付
     *
     * @param request 支付请求
     * @return 支付响应
     */
    @Override
    public PaymentResponse pay(PaymentRequest request) {
        Long amountFen = NumberUtil.mul(request.amount(), BigDecimal.valueOf(100)).longValue();

        AlipayLegacyResult result = alipayLegacyClient.executePay(
                request.orderNo(),
                amountFen,
                request.subject()
        );

        boolean success = SUCCESS_CODE.equals(result.code());

        log.info("支付宝支付适配完成，订单号：{}，金额分：{}，结果：{}",
                request.orderNo(), amountFen, result.msg());

        return new PaymentResponse(
                supportChannel(),
                request.orderNo(),
                result.tradeNo(),
                success,
                result.msg()
        );
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/adapter/WechatPaymentAdapter.java`

下面是微信支付适配器，负责把系统内部支付请求转换成微信 V3 客户端需要的命令对象。

```java
package io.github.atengk.design.adapter;

import cn.hutool.core.util.NumberUtil;
import io.github.atengk.design.dto.PaymentRequest;
import io.github.atengk.design.dto.PaymentResponse;
import io.github.atengk.design.dto.WechatPayCommand;
import io.github.atengk.design.external.WechatV3Client;
import io.github.atengk.design.external.WechatV3Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 微信支付适配器
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WechatPaymentAdapter implements PaymentAdapter {

    private final WechatV3Client wechatV3Client;

    /**
     * 获取支持的支付渠道
     *
     * @return 支付渠道
     */
    @Override
    public String supportChannel() {
        return "wechat";
    }

    /**
     * 执行支付
     *
     * @param request 支付请求
     * @return 支付响应
     */
    @Override
    public PaymentResponse pay(PaymentRequest request) {
        Long amountFen = NumberUtil.mul(request.amount(), BigDecimal.valueOf(100)).longValue();

        WechatPayCommand command = new WechatPayCommand(
                request.orderNo(),
                String.valueOf(request.userId()),
                amountFen,
                request.subject()
        );

        WechatV3Result result = wechatV3Client.createTransaction(command);

        log.info("微信支付适配完成，订单号：{}，金额分：{}，结果：{}",
                request.orderNo(), amountFen, result.message());

        return new PaymentResponse(
                supportChannel(),
                request.orderNo(),
                result.transactionId(),
                result.success(),
                result.message()
        );
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/context/PaymentContext.java`

下面是支付上下文，负责根据支付渠道选择对应适配器，并对统一请求做基础校验。

```java
package io.github.atengk.design.context;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.design.adapter.PaymentAdapter;
import io.github.atengk.design.dto.PaymentRequest;
import io.github.atengk.design.dto.PaymentResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 支付上下文
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
public class PaymentContext {

    private final Map<String, PaymentAdapter> adapterMap;

    /**
     * 创建支付上下文
     *
     * @param paymentAdapters 支付适配器列表
     */
    public PaymentContext(List<PaymentAdapter> paymentAdapters) {
        if (CollUtil.isEmpty(paymentAdapters)) {
            log.warn("支付适配器列表为空");
            this.adapterMap = Map.of();
            return;
        }

        this.adapterMap = paymentAdapters.stream()
                .collect(Collectors.toUnmodifiableMap(
                        adapter -> StrUtil.trim(adapter.supportChannel()).toLowerCase(),
                        Function.identity()
                ));

        log.info("初始化支付上下文，支持支付渠道：{}", adapterMap.keySet());
    }

    /**
     * 执行支付
     *
     * @param request 支付请求
     * @return 支付响应
     */
    public PaymentResponse pay(PaymentRequest request) {
        validateRequest(request);

        String channel = StrUtil.trim(request.channel()).toLowerCase();
        PaymentAdapter adapter = adapterMap.get(channel);

        if (adapter == null) {
            log.warn("支付失败，不支持的支付渠道：{}", request.channel());
            throw new IllegalArgumentException("不支持的支付渠道：" + request.channel());
        }

        log.info("匹配支付适配器成功，支付渠道：{}，订单号：{}", channel, request.orderNo());
        return adapter.pay(request);
    }

    /**
     * 校验支付请求
     *
     * @param request 支付请求
     */
    private void validateRequest(PaymentRequest request) {
        if (request == null) {
            log.warn("支付失败，请求参数为空");
            throw new IllegalArgumentException("请求参数不能为空");
        }

        if (StrUtil.isBlank(request.channel())) {
            log.warn("支付失败，支付渠道为空");
            throw new IllegalArgumentException("支付渠道不能为空");
        }

        if (StrUtil.isBlank(request.orderNo())) {
            log.warn("支付失败，订单号为空");
            throw new IllegalArgumentException("订单号不能为空");
        }

        if (request.userId() == null || request.userId() <= 0) {
            log.warn("支付失败，用户ID不合法，用户ID：{}", request.userId());
            throw new IllegalArgumentException("用户ID必须大于0");
        }

        if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("支付失败，金额不合法，金额：{}", request.amount());
            throw new IllegalArgumentException("支付金额必须大于0");
        }

        if (StrUtil.isBlank(request.subject())) {
            log.warn("支付失败，订单标题为空");
            throw new IllegalArgumentException("订单标题不能为空");
        }
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/controller/PaymentController.java`

下面是支付接口，用于验证适配器模式效果。

```java
package io.github.atengk.design.controller;

import io.github.atengk.design.context.PaymentContext;
import io.github.atengk.design.dto.PaymentRequest;
import io.github.atengk.design.dto.PaymentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * 支付控制器
 *
 * @author Ateng
 * @since 2026-04-30
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/adapter/payment")
public class PaymentController {

    private final PaymentContext paymentContext;

    /**
     * 执行支付
     *
     * @param channel 支付渠道
     * @param orderNo 订单号
     * @param userId  用户ID
     * @param amount  支付金额
     * @param subject 订单标题
     * @return 支付响应
     */
    @PostMapping("/pay")
    public PaymentResponse pay(@RequestParam String channel,
                               @RequestParam String orderNo,
                               @RequestParam Long userId,
                               @RequestParam BigDecimal amount,
                               @RequestParam String subject) {
        PaymentRequest request = new PaymentRequest(
                channel,
                orderNo,
                userId,
                amount,
                subject
        );
        return paymentContext.pay(request);
    }
}
```

接口调用示例：

```bash
curl -X POST "http://localhost:8080/adapter/payment/pay?channel=alipay&orderNo=ORDER10001&userId=10001&amount=99.90&subject=键盘订单"

curl -X POST "http://localhost:8080/adapter/payment/pay?channel=wechat&orderNo=ORDER10002&userId=10002&amount=66.60&subject=鼠标订单"
```

支付宝可能返回：

```json
{
  "channel": "alipay",
  "orderNo": "ORDER10001",
  "tradeNo": "ALI2019776866538487808",
  "success": true,
  "message": "支付成功"
}
```

微信可能返回：

```json
{
  "channel": "wechat",
  "orderNo": "ORDER10002",
  "tradeNo": "WX2019776866538487809",
  "success": true,
  "message": "支付成功"
}
```

这种方式的优点是业务层完全不需要关心支付宝和微信的接口差异。第三方接口参数、返回码、金额单位、字段名称都由适配器内部完成转换。

## 扩展一个新适配器

在 Spring Boot 适配器模式中，新增外部系统接入通常只需要新增一个适配器类和对应客户端。下面以银联支付为例。

### 文件结构

```text
src/main/java/io/github/atengk/design/
├── external/
│   ├── UnionPayClient.java
│   └── UnionPayResult.java
└── adapter/
    └── UnionPayPaymentAdapter.java
```

文件位置：`src/main/java/io/github/atengk/design/external/UnionPayResult.java`

下面是银联客户端返回结果。

```java
package io.github.atengk.design.external;

/**
 * 银联支付结果
 *
 * @param respCode 响应编码
 * @param queryId  银联交易查询ID
 * @param respMsg  响应消息
 * @author Ateng
 * @since 2026-04-30
 */
public record UnionPayResult(String respCode, String queryId, String respMsg) {
}
```

文件位置：`src/main/java/io/github/atengk/design/external/UnionPayClient.java`

下面是银联支付客户端示例。

```java
package io.github.atengk.design.external;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 银联支付客户端
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
public class UnionPayClient {

    /**
     * 消费交易
     *
     * @param merOrderId 商户订单号
     * @param txnAmt     交易金额，单位：分
     * @return 银联支付结果
     */
    public UnionPayResult consume(String merOrderId, String txnAmt) {
        if (StrUtil.hasBlank(merOrderId, txnAmt)) {
            log.warn("银联支付失败，商户订单号或交易金额为空");
            return new UnionPayResult("99", null, "参数不合法");
        }

        String queryId = "UP" + IdUtil.getSnowflakeNextId();
        log.info("调用银联支付客户端，商户订单号：{}，交易金额：{}，查询ID：{}",
                merOrderId, txnAmt, queryId);

        return new UnionPayResult("00", queryId, "支付成功");
    }
}
```

如果要交给 Spring 管理，需要在配置类中新增 Bean。

文件位置：`src/main/java/io/github/atengk/design/config/ThirdPayClientConfig.java`

```java
@Bean
public UnionPayClient unionPayClient() {
    log.info("初始化银联支付客户端");
    return new UnionPayClient();
}
```

文件位置：`src/main/java/io/github/atengk/design/adapter/UnionPayPaymentAdapter.java`

下面是银联支付适配器。新增后会被 Spring 自动扫描，并自动加入 `PaymentContext` 的适配器列表。

```java
package io.github.atengk.design.adapter;

import cn.hutool.core.util.NumberUtil;
import io.github.atengk.design.dto.PaymentRequest;
import io.github.atengk.design.dto.PaymentResponse;
import io.github.atengk.design.external.UnionPayClient;
import io.github.atengk.design.external.UnionPayResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 银联支付适配器
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UnionPayPaymentAdapter implements PaymentAdapter {

    private static final String SUCCESS_CODE = "00";

    private final UnionPayClient unionPayClient;

    /**
     * 获取支持的支付渠道
     *
     * @return 支付渠道
     */
    @Override
    public String supportChannel() {
        return "unionpay";
    }

    /**
     * 执行支付
     *
     * @param request 支付请求
     * @return 支付响应
     */
    @Override
    public PaymentResponse pay(PaymentRequest request) {
        Long amountFen = NumberUtil.mul(request.amount(), BigDecimal.valueOf(100)).longValue();
        UnionPayResult result = unionPayClient.consume(request.orderNo(), String.valueOf(amountFen));

        boolean success = SUCCESS_CODE.equals(result.respCode());

        log.info("银联支付适配完成，订单号：{}，金额分：{}，结果：{}",
                request.orderNo(), amountFen, result.respMsg());

        return new PaymentResponse(
                supportChannel(),
                request.orderNo(),
                result.queryId(),
                success,
                result.respMsg()
        );
    }
}
```

调用示例：

```bash
curl -X POST "http://localhost:8080/adapter/payment/pay?channel=unionpay&orderNo=ORDER10003&userId=10003&amount=188.00&subject=显示器订单"
```

新增银联适配器后，`PaymentController` 和 `PaymentContext` 不需要修改。业务入口仍然使用统一支付请求，差异转换由银联适配器承担。

## 类适配器

类适配器通过继承被适配类并实现目标接口来完成适配。由于 Java 只支持单继承，类适配器在实际项目中不如对象适配器灵活。

下面是一个简化示例。

文件位置：`src/main/java/io/github/atengk/design/adapter/classadapter/ClassSmsClientAdapter.java`

下面的类适配器继承旧短信客户端，并实现新系统的消息发送接口。

```java
package io.github.atengk.design.adapter.classadapter;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.design.adapter.simple.LegacySmsClient;
import io.github.atengk.design.adapter.simple.LegacySmsResult;
import io.github.atengk.design.adapter.simple.MessageRequest;
import io.github.atengk.design.adapter.simple.MessageSender;
import lombok.extern.slf4j.Slf4j;

/**
 * 类短信客户端适配器
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
public class ClassSmsClientAdapter extends LegacySmsClient implements MessageSender {

    /**
     * 发送消息
     *
     * @param request 消息发送请求
     * @return 发送结果
     */
    @Override
    public String send(MessageRequest request) {
        if (request == null || StrUtil.hasBlank(request.receiver(), request.content())) {
            log.warn("类适配器发送短信失败，接收人或内容为空");
            throw new IllegalArgumentException("接收人和内容不能为空");
        }

        String traceNo = IdUtil.fastSimpleUUID();
        LegacySmsResult result = sendSms(request.receiver(), request.content(), traceNo);

        if (!Boolean.TRUE.equals(result.success())) {
            log.warn("类适配器发送短信失败，响应编码：{}，响应消息：{}", result.code(), result.message());
            throw new IllegalStateException("短信发送失败：" + result.message());
        }

        log.info("类适配器发送短信成功，接收人：{}，流水号：{}", request.receiver(), traceNo);
        return result.message();
    }
}
```

使用方式：

```java
MessageSender sender = new ClassSmsClientAdapter();
String result = sender.send(new MessageRequest("13800138000", "验证码：123456"));
```

类适配器的缺点是适配器已经继承了被适配类，后续无法再继承其他父类。因此在 Spring Boot 后端项目中，优先使用对象适配器。

## 适配器模式和装饰器模式的区别

适配器模式和装饰器模式都可能表现为“包装一个对象”，但二者的目的不同。

| 对比项     | 适配器模式                     | 装饰器模式                     |
| ---------- | ------------------------------ | ------------------------------ |
| 核心目的   | 转换接口，让不兼容接口可以协作 | 增强能力，不改变原接口         |
| 关注点     | 接口兼容、参数转换、结果转换   | 日志、缓存、重试、限流、审计   |
| 调用方接口 | 通常变成调用方期望的新接口     | 通常仍然保持原接口             |
| 被包装对象 | 接口往往不兼容                 | 接口通常兼容                   |
| 典型场景   | 第三方 SDK 接入、旧系统兼容    | 服务增强链、缓存增强、审计增强 |

简单理解：

```text
适配器模式：接口不一样，我帮你转换成一样。
装饰器模式：接口本来一样，我帮你额外增强能力。
```

例如支付宝旧接口、微信 V3 接口、银联接口都不一样，但系统内部希望统一为 `PaymentAdapter`，这就是适配器模式。订单创建服务本来已经可用，只是在外层增加审计和幂等，这更接近装饰器模式。

## 适配器模式和门面模式的区别

适配器模式和门面模式都可以隐藏复杂性，但二者关注点不同。

| 对比项           | 适配器模式                 | 门面模式                             |
| ---------------- | -------------------------- | ------------------------------------ |
| 核心目的         | 解决接口不兼容             | 简化复杂子系统调用                   |
| 是否强调接口转换 | 强调                       | 不一定                               |
| 主要对象         | 单个旧接口或第三方接口     | 一组子系统接口                       |
| 调用方收益       | 用统一接口接入不兼容对象   | 用简单入口调用复杂流程               |
| 典型场景         | 统一支付适配、统一短信适配 | 下单门面、文件处理门面、用户中心门面 |

简单理解：

```text
适配器模式：旧接口不能直接用，转换后才能用。
门面模式：接口太多太复杂，包成一个简单入口用。
```

如果是把不同第三方支付渠道统一成一个支付接口，通常是适配器模式。如果是把创建订单、扣库存、支付、发通知包装成一个下单入口，更接近门面模式。

## 验证方式

启动 Spring Boot 项目：

```bash
mvn spring-boot:run
```

执行支付宝支付：

```bash
curl -X POST "http://localhost:8080/adapter/payment/pay?channel=alipay&orderNo=ORDER10001&userId=10001&amount=99.90&subject=键盘订单"
```

执行微信支付：

```bash
curl -X POST "http://localhost:8080/adapter/payment/pay?channel=wechat&orderNo=ORDER10002&userId=10002&amount=66.60&subject=鼠标订单"
```

执行银联支付：

```bash
curl -X POST "http://localhost:8080/adapter/payment/pay?channel=unionpay&orderNo=ORDER10003&userId=10003&amount=188.00&subject=显示器订单"
```

如果适配器分发正常，可以看到类似日志：

```text
初始化支付上下文，支持支付渠道：[alipay, wechat, unionpay]
匹配支付适配器成功，支付渠道：alipay，订单号：ORDER10001
调用支付宝旧客户端，订单号：ORDER10001，金额分：9990，标题：键盘订单，交易号：ALI2019776866538487808
支付宝支付适配完成，订单号：ORDER10001，金额分：9990，结果：支付成功
```

如果传入不支持的支付渠道：

```bash
curl -X POST "http://localhost:8080/adapter/payment/pay?channel=paypal&orderNo=ORDER10004&userId=10004&amount=20.00&subject=测试订单"
```

会抛出异常：

```text
不支持的支付渠道：paypal
```

实际项目中建议结合全局异常处理器，将该异常转换成统一响应结构。

## 注意事项

适配器模式适合处理接口不兼容问题，但不要在适配器中塞入过多核心业务逻辑。适配器应该主要做参数转换、单位转换、字段映射、状态码转换和异常包装。

推荐适配器承担这些职责：

```text
内部 DTO -> 第三方请求对象
第三方响应对象 -> 内部响应 DTO
金额元 -> 金额分
第三方状态码 -> 内部成功失败状态
第三方异常 -> 内部业务异常
```

不推荐适配器承担这些职责：

```text
订单核心状态流转
库存扣减
会员权益计算
复杂营销规则
核心业务事务控制
```

错误示例：

```java
@Override
public PaymentResponse pay(PaymentRequest request) {
    // 创建订单
    // 扣减库存
    // 计算优惠
    // 调用第三方支付
    // 修改订单状态
    // 发送通知
    return null;
}
```

推荐将适配器控制在接口转换范围内：

```java
@Override
public PaymentResponse pay(PaymentRequest request) {
    Long amountFen = NumberUtil.mul(request.amount(), BigDecimal.valueOf(100)).longValue();
    AlipayLegacyResult result = alipayLegacyClient.executePay(request.orderNo(), amountFen, request.subject());
    return convertResult(request, result);
}
```

适配器需要重点处理字段语义差异。例如不同第三方系统对金额单位、成功状态、订单号字段、交易号字段的定义可能不同。

常见差异如下：

```text
金额单位：元、分、厘
订单字段：orderNo、outTradeNo、merchantOrderId
交易字段：tradeNo、transactionId、queryId
成功状态：true、SUCCESS、10000、00
异常格式：错误码、异常对象、HTTP 状态码
```

生产环境中，不建议把第三方返回结果原样暴露给前端或上游系统。适配器应转换成内部统一响应，避免第三方接口变动直接影响业务接口。

Spring Bean 默认是单例，适配器中不要保存请求级状态。

错误示例：

```java
private String currentOrderNo;
private BigDecimal currentAmount;
private String currentTradeNo;
```

推荐使用方法参数和局部变量：

```java
public PaymentResponse pay(PaymentRequest request) {
    String orderNo = request.orderNo();
    Long amountFen = NumberUtil.mul(request.amount(), BigDecimal.valueOf(100)).longValue();
    return doPay(orderNo, amountFen);
}
```

如果适配器调用外部 HTTP、RPC 或 SDK，生产环境中需要考虑超时、重试、幂等、限流、熔断、签名、验签、日志脱敏和异常补偿。适配器只解决接口兼容问题，不自动保证分布式一致性。

## 总结

在 JDK21 和 Spring Boot 3 项目中，适配器模式的实践重点是把外部不兼容接口转换成系统内部统一接口。

普通 Java 对象适配器适合理解原理和本地兼容。Spring Boot 适配器适合第三方 SDK、旧系统接口、多渠道支付、多渠道短信、多云存储等业务场景。对于这些场景，推荐使用“统一接口 + 多个适配器实现 + Spring 上下文分发”的结构。

适配器模式不是为了增强功能，也不是为了封装全部业务流程。它最适合解决接口不一致、参数结构不同、返回格式不同、字段语义不同的问题，让业务层面对稳定统一的内部接口编程。
