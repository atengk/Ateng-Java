# 抽象工厂模式

抽象工厂模式属于创建型模式，用于创建一组相关或相互依赖的对象。它关注的不是“创建一个对象”，而是“创建一个产品族”。在当前 29 个设计模式文档体系中，抽象工厂模式属于 GoF 创建型模式，模块名为 `abstractfactory`。

## 基础配置

本示例基于 JDK 21、Spring Boot 3、Maven、Hutool、Lombok 编写。示例场景是“多支付平台接入”。系统支持支付宝和微信支付，每个平台都包含一组相关能力：创建支付订单、发起退款。抽象工厂负责根据平台创建对应的一组客户端。

文件位置：`pom.xml`

```xml
<dependencies>
    <!-- Spring Boot Web，用于提供支付和退款接口 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Spring Boot Validation，用于请求参数校验 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- Hutool 工具类，用于字符串、日期、ID、JSON 等常用处理 -->
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

    <!-- Spring Boot 测试依赖，用于验证抽象工厂产品族创建逻辑 -->
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
  abstract-factory:
    alipay:
      # 支付宝商户号，示例值
      merchant-id: ALIPAY_MERCHANT_10001
      # 支付宝网关地址，示例值
      gateway-url: https://openapi.alipay.com/gateway.do
    wechat:
      # 微信支付商户号，示例值
      merchant-id: WECHAT_MERCHANT_20001
      # 微信支付网关地址，示例值
      gateway-url: https://api.mch.weixin.qq.com
```

## 模式说明

抽象工厂模式解决的是“同一产品族的一组对象创建问题”。它适合多个产品之间存在平台、厂商、渠道、环境等维度上的一致性约束。

在本示例中，支付宝和微信支付是两个产品族：

```text
支付宝产品族：
- AlipayPaymentClient
- AlipayRefundClient

微信支付产品族：
- WechatPaymentClient
- WechatRefundClient
```

抽象工厂提供统一创建入口：

```text
PaymentPlatformFactory
- createPaymentClient()
- createRefundClient()
```

它和工厂方法模式的区别在于：工厂方法通常创建一个产品对象；抽象工厂创建一组相关产品对象。例如支付场景中，一个平台不仅要创建支付客户端，还要创建退款客户端、查询客户端、关闭订单客户端、回调验签器等，这类场景更适合抽象工厂模式。

## 项目结构

本示例按照 Spring Boot 常规分层组织。`factory` 包中定义抽象工厂和具体工厂，`client` 包中定义不同产品接口和具体产品。

```text
src/main/java/io/github/atengk/pattern/abstractfactory
├── AbstractFactoryApplication.java
├── client
│   ├── payment
│   │   ├── PaymentClient.java
│   │   ├── AlipayPaymentClient.java
│   │   └── WechatPaymentClient.java
│   └── refund
│       ├── RefundClient.java
│       ├── AlipayRefundClient.java
│       └── WechatRefundClient.java
├── config
│   └── AbstractFactoryDemoProperties.java
├── controller
│   └── PayPlatformController.java
├── dto
│   ├── PaymentCreateDTO.java
│   └── RefundCreateDTO.java
├── enums
│   └── PayPlatformEnum.java
├── factory
│   ├── PaymentPlatformFactory.java
│   ├── AlipayPlatformFactory.java
│   └── WechatPlatformFactory.java
├── registry
│   └── PaymentPlatformFactoryRegistry.java
├── service
│   ├── PayPlatformService.java
│   └── PayPlatformServiceImpl.java
└── vo
    ├── PaymentCreateVO.java
    └── RefundCreateVO.java
```

## 核心代码

这一部分给出抽象工厂模式的完整核心代码。重点是 `PaymentPlatformFactory`，它不是只创建一个对象，而是创建同一支付平台下的一组相关客户端。

文件位置：`src/main/java/io/github/atengk/pattern/abstractfactory/AbstractFactoryApplication.java`

```java
package io.github.atengk.pattern.abstractfactory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 抽象工厂模式示例启动类
 *
 * @author Ateng
 * @since 2026-05-13
 */
@SpringBootApplication
public class AbstractFactoryApplication {

    /**
     * 启动抽象工厂模式示例应用
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(AbstractFactoryApplication.class, args);
    }

}
```

文件位置：`src/main/java/io/github/atengk/pattern/abstractfactory/config/AbstractFactoryDemoProperties.java`

```java
package io.github.atengk.pattern.abstractfactory.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 抽象工厂模式示例配置
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
@Component
@ConfigurationProperties(prefix = "demo.abstract-factory")
public class AbstractFactoryDemoProperties {

    /**
     * 支付宝配置
     */
    private Alipay alipay = new Alipay();

    /**
     * 微信支付配置
     */
    private Wechat wechat = new Wechat();

    /**
     * 支付宝配置项
     *
     * @author Ateng
     * @since 2026-05-13
     */
    @Data
    public static class Alipay {

        /**
         * 商户号
         */
        private String merchantId;

        /**
         * 网关地址
         */
        private String gatewayUrl;

    }

    /**
     * 微信支付配置项
     *
     * @author Ateng
     * @since 2026-05-13
     */
    @Data
    public static class Wechat {

        /**
         * 商户号
         */
        private String merchantId;

        /**
         * 网关地址
         */
        private String gatewayUrl;

    }

}
```

下面的枚举用于定义支付平台，避免业务代码直接使用字符串判断。

文件位置：`src/main/java/io/github/atengk/pattern/abstractfactory/enums/PayPlatformEnum.java`

```java
package io.github.atengk.pattern.abstractfactory.enums;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;

import java.util.Arrays;

/**
 * 支付平台枚举
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Getter
public enum PayPlatformEnum {

    ALIPAY("ALIPAY", "支付宝"),

    WECHAT("WECHAT", "微信支付");

    private final String code;

    private final String description;

    PayPlatformEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根据编码解析支付平台
     *
     * @param code 支付平台编码
     * @return 支付平台
     */
    public static PayPlatformEnum parse(String code) {
        return Arrays.stream(values())
                .filter(item -> StrUtil.equalsIgnoreCase(item.getCode(), code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(StrUtil.format("不支持的支付平台：{}", code)));
    }

}
```

## DTO 和 VO

这一部分定义支付和退款接口的数据对象。支付和退款属于同一个产品族下的两类业务能力。

文件位置：`src/main/java/io/github/atengk/pattern/abstractfactory/dto/PaymentCreateDTO.java`

```java
package io.github.atengk.pattern.abstractfactory.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 支付创建请求对象
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
public class PaymentCreateDTO {

    /**
     * 支付平台：ALIPAY、WECHAT
     */
    @NotBlank(message = "支付平台不能为空")
    private String platform;

    /**
     * 业务订单号
     */
    @NotBlank(message = "业务订单号不能为空")
    private String orderNo;

    /**
     * 用户编号
     */
    @NotBlank(message = "用户编号不能为空")
    private String userId;

    /**
     * 支付金额
     */
    @NotNull(message = "支付金额不能为空")
    @DecimalMin(value = "0.01", message = "支付金额必须大于0")
    private BigDecimal amount;

}
```

文件位置：`src/main/java/io/github/atengk/pattern/abstractfactory/dto/RefundCreateDTO.java`

```java
package io.github.atengk.pattern.abstractfactory.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 退款创建请求对象
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
public class RefundCreateDTO {

    /**
     * 支付平台：ALIPAY、WECHAT
     */
    @NotBlank(message = "支付平台不能为空")
    private String platform;

    /**
     * 原支付交易号
     */
    @NotBlank(message = "原支付交易号不能为空")
    private String payTradeNo;

    /**
     * 退款单号
     */
    @NotBlank(message = "退款单号不能为空")
    private String refundNo;

    /**
     * 退款金额
     */
    @NotNull(message = "退款金额不能为空")
    @DecimalMin(value = "0.01", message = "退款金额必须大于0")
    private BigDecimal refundAmount;

}
```

文件位置：`src/main/java/io/github/atengk/pattern/abstractfactory/vo/PaymentCreateVO.java`

```java
package io.github.atengk.pattern.abstractfactory.vo;

import java.math.BigDecimal;

/**
 * 支付创建响应对象
 *
 * @author Ateng
 * @since 2026-05-13
 */
public record PaymentCreateVO(
        String platform,
        String orderNo,
        String payTradeNo,
        BigDecimal amount,
        Boolean success,
        String message,
        String createTime
) {
}
```

文件位置：`src/main/java/io/github/atengk/pattern/abstractfactory/vo/RefundCreateVO.java`

```java
package io.github.atengk.pattern.abstractfactory.vo;

import java.math.BigDecimal;

/**
 * 退款创建响应对象
 *
 * @author Ateng
 * @since 2026-05-13
 */
public record RefundCreateVO(
        String platform,
        String refundNo,
        String refundTradeNo,
        BigDecimal refundAmount,
        Boolean success,
        String message,
        String createTime
) {
}
```

## 产品接口和具体产品

这一部分定义两个产品接口：支付客户端和退款客户端。每个支付平台都要提供这两个产品的具体实现。

文件位置：`src/main/java/io/github/atengk/pattern/abstractfactory/client/payment/PaymentClient.java`

```java
package io.github.atengk.pattern.abstractfactory.client.payment;

import io.github.atengk.pattern.abstractfactory.dto.PaymentCreateDTO;
import io.github.atengk.pattern.abstractfactory.vo.PaymentCreateVO;

/**
 * 支付客户端接口
 *
 * @author Ateng
 * @since 2026-05-13
 */
public interface PaymentClient {

    /**
     * 创建支付订单
     *
     * @param createDTO 支付创建请求
     * @return 支付创建结果
     */
    PaymentCreateVO createPayment(PaymentCreateDTO createDTO);

}
```

文件位置：`src/main/java/io/github/atengk/pattern/abstractfactory/client/refund/RefundClient.java`

```java
package io.github.atengk.pattern.abstractfactory.client.refund;

import io.github.atengk.pattern.abstractfactory.dto.RefundCreateDTO;
import io.github.atengk.pattern.abstractfactory.vo.RefundCreateVO;

/**
 * 退款客户端接口
 *
 * @author Ateng
 * @since 2026-05-13
 */
public interface RefundClient {

    /**
     * 创建退款订单
     *
     * @param createDTO 退款创建请求
     * @return 退款创建结果
     */
    RefundCreateVO createRefund(RefundCreateDTO createDTO);

}
```

下面是支付宝产品族中的支付客户端。

文件位置：`src/main/java/io/github/atengk/pattern/abstractfactory/client/payment/AlipayPaymentClient.java`

```java
package io.github.atengk.pattern.abstractfactory.client.payment;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.pattern.abstractfactory.dto.PaymentCreateDTO;
import io.github.atengk.pattern.abstractfactory.enums.PayPlatformEnum;
import io.github.atengk.pattern.abstractfactory.vo.PaymentCreateVO;
import lombok.extern.slf4j.Slf4j;

/**
 * 支付宝支付客户端
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
public class AlipayPaymentClient implements PaymentClient {

    private final String merchantId;

    private final String gatewayUrl;

    public AlipayPaymentClient(String merchantId, String gatewayUrl) {
        this.merchantId = merchantId;
        this.gatewayUrl = gatewayUrl;
    }

    /**
     * 创建支付宝支付订单
     *
     * @param createDTO 支付创建请求
     * @return 支付创建结果
     */
    @Override
    public PaymentCreateVO createPayment(PaymentCreateDTO createDTO) {
        String tradeNo = StrUtil.format("ALI_PAY_{}", IdUtil.fastSimpleUUID());

        log.info("创建支付宝支付订单，merchantId：{}，gatewayUrl：{}，orderNo：{}，amount：{}",
                merchantId, gatewayUrl, createDTO.getOrderNo(), createDTO.getAmount());

        return new PaymentCreateVO(
                PayPlatformEnum.ALIPAY.getCode(),
                createDTO.getOrderNo(),
                tradeNo,
                createDTO.getAmount(),
                true,
                "支付宝支付订单创建成功",
                DateUtil.now()
        );
    }

}
```

下面是支付宝产品族中的退款客户端。

文件位置：`src/main/java/io/github/atengk/pattern/abstractfactory/client/refund/AlipayRefundClient.java`

```java
package io.github.atengk.pattern.abstractfactory.client.refund;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.pattern.abstractfactory.dto.RefundCreateDTO;
import io.github.atengk.pattern.abstractfactory.enums.PayPlatformEnum;
import io.github.atengk.pattern.abstractfactory.vo.RefundCreateVO;
import lombok.extern.slf4j.Slf4j;

/**
 * 支付宝退款客户端
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
public class AlipayRefundClient implements RefundClient {

    private final String merchantId;

    private final String gatewayUrl;

    public AlipayRefundClient(String merchantId, String gatewayUrl) {
        this.merchantId = merchantId;
        this.gatewayUrl = gatewayUrl;
    }

    /**
     * 创建支付宝退款订单
     *
     * @param createDTO 退款创建请求
     * @return 退款创建结果
     */
    @Override
    public RefundCreateVO createRefund(RefundCreateDTO createDTO) {
        String refundTradeNo = StrUtil.format("ALI_REFUND_{}", IdUtil.fastSimpleUUID());

        log.info("创建支付宝退款订单，merchantId：{}，gatewayUrl：{}，refundNo：{}，refundAmount：{}",
                merchantId, gatewayUrl, createDTO.getRefundNo(), createDTO.getRefundAmount());

        return new RefundCreateVO(
                PayPlatformEnum.ALIPAY.getCode(),
                createDTO.getRefundNo(),
                refundTradeNo,
                createDTO.getRefundAmount(),
                true,
                "支付宝退款订单创建成功",
                DateUtil.now()
        );
    }

}
```

下面是微信支付产品族中的支付客户端。

文件位置：`src/main/java/io/github/atengk/pattern/abstractfactory/client/payment/WechatPaymentClient.java`

```java
package io.github.atengk.pattern.abstractfactory.client.payment;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.pattern.abstractfactory.dto.PaymentCreateDTO;
import io.github.atengk.pattern.abstractfactory.enums.PayPlatformEnum;
import io.github.atengk.pattern.abstractfactory.vo.PaymentCreateVO;
import lombok.extern.slf4j.Slf4j;

/**
 * 微信支付客户端
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
public class WechatPaymentClient implements PaymentClient {

    private final String merchantId;

    private final String gatewayUrl;

    public WechatPaymentClient(String merchantId, String gatewayUrl) {
        this.merchantId = merchantId;
        this.gatewayUrl = gatewayUrl;
    }

    /**
     * 创建微信支付订单
     *
     * @param createDTO 支付创建请求
     * @return 支付创建结果
     */
    @Override
    public PaymentCreateVO createPayment(PaymentCreateDTO createDTO) {
        String tradeNo = StrUtil.format("WX_PAY_{}", IdUtil.fastSimpleUUID());

        log.info("创建微信支付订单，merchantId：{}，gatewayUrl：{}，orderNo：{}，amount：{}",
                merchantId, gatewayUrl, createDTO.getOrderNo(), createDTO.getAmount());

        return new PaymentCreateVO(
                PayPlatformEnum.WECHAT.getCode(),
                createDTO.getOrderNo(),
                tradeNo,
                createDTO.getAmount(),
                true,
                "微信支付订单创建成功",
                DateUtil.now()
        );
    }

}
```

下面是微信支付产品族中的退款客户端。

文件位置：`src/main/java/io/github/atengk/pattern/abstractfactory/client/refund/WechatRefundClient.java`

```java
package io.github.atengk.pattern.abstractfactory.client.refund;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.pattern.abstractfactory.dto.RefundCreateDTO;
import io.github.atengk.pattern.abstractfactory.enums.PayPlatformEnum;
import io.github.atengk.pattern.abstractfactory.vo.RefundCreateVO;
import lombok.extern.slf4j.Slf4j;

/**
 * 微信退款客户端
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
public class WechatRefundClient implements RefundClient {

    private final String merchantId;

    private final String gatewayUrl;

    public WechatRefundClient(String merchantId, String gatewayUrl) {
        this.merchantId = merchantId;
        this.gatewayUrl = gatewayUrl;
    }

    /**
     * 创建微信退款订单
     *
     * @param createDTO 退款创建请求
     * @return 退款创建结果
     */
    @Override
    public RefundCreateVO createRefund(RefundCreateDTO createDTO) {
        String refundTradeNo = StrUtil.format("WX_REFUND_{}", IdUtil.fastSimpleUUID());

        log.info("创建微信退款订单，merchantId：{}，gatewayUrl：{}，refundNo：{}，refundAmount：{}",
                merchantId, gatewayUrl, createDTO.getRefundNo(), createDTO.getRefundAmount());

        return new RefundCreateVO(
                PayPlatformEnum.WECHAT.getCode(),
                createDTO.getRefundNo(),
                refundTradeNo,
                createDTO.getRefundAmount(),
                true,
                "微信退款订单创建成功",
                DateUtil.now()
        );
    }

}
```

## 抽象工厂和具体工厂

这一部分是抽象工厂模式的核心。`PaymentPlatformFactory` 定义一个产品族的创建规范，具体工厂负责创建同一平台下的支付客户端和退款客户端。

文件位置：`src/main/java/io/github/atengk/pattern/abstractfactory/factory/PaymentPlatformFactory.java`

```java
package io.github.atengk.pattern.abstractfactory.factory;

import io.github.atengk.pattern.abstractfactory.client.payment.PaymentClient;
import io.github.atengk.pattern.abstractfactory.client.refund.RefundClient;
import io.github.atengk.pattern.abstractfactory.enums.PayPlatformEnum;

/**
 * 支付平台抽象工厂
 *
 * @author Ateng
 * @since 2026-05-13
 */
public interface PaymentPlatformFactory {

    /**
     * 获取当前工厂支持的支付平台
     *
     * @return 支付平台
     */
    PayPlatformEnum supportPlatform();

    /**
     * 创建支付客户端
     *
     * @return 支付客户端
     */
    PaymentClient createPaymentClient();

    /**
     * 创建退款客户端
     *
     * @return 退款客户端
     */
    RefundClient createRefundClient();

}
```

下面是支付宝产品族工厂。

文件位置：`src/main/java/io/github/atengk/pattern/abstractfactory/factory/AlipayPlatformFactory.java`

```java
package io.github.atengk.pattern.abstractfactory.factory;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.pattern.abstractfactory.client.payment.AlipayPaymentClient;
import io.github.atengk.pattern.abstractfactory.client.payment.PaymentClient;
import io.github.atengk.pattern.abstractfactory.client.refund.AlipayRefundClient;
import io.github.atengk.pattern.abstractfactory.client.refund.RefundClient;
import io.github.atengk.pattern.abstractfactory.config.AbstractFactoryDemoProperties;
import io.github.atengk.pattern.abstractfactory.enums.PayPlatformEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 支付宝产品族工厂
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlipayPlatformFactory implements PaymentPlatformFactory {

    private final AbstractFactoryDemoProperties properties;

    /**
     * 获取当前工厂支持的支付平台
     *
     * @return 支付宝平台
     */
    @Override
    public PayPlatformEnum supportPlatform() {
        return PayPlatformEnum.ALIPAY;
    }

    /**
     * 创建支付宝支付客户端
     *
     * @return 支付宝支付客户端
     */
    @Override
    public PaymentClient createPaymentClient() {
        AbstractFactoryDemoProperties.Alipay alipay = properties.getAlipay();
        checkConfig(alipay);

        log.info("创建支付宝支付客户端，merchantId：{}", alipay.getMerchantId());
        return new AlipayPaymentClient(alipay.getMerchantId(), alipay.getGatewayUrl());
    }

    /**
     * 创建支付宝退款客户端
     *
     * @return 支付宝退款客户端
     */
    @Override
    public RefundClient createRefundClient() {
        AbstractFactoryDemoProperties.Alipay alipay = properties.getAlipay();
        checkConfig(alipay);

        log.info("创建支付宝退款客户端，merchantId：{}", alipay.getMerchantId());
        return new AlipayRefundClient(alipay.getMerchantId(), alipay.getGatewayUrl());
    }

    /**
     * 校验支付宝配置
     *
     * @param alipay 支付宝配置
     */
    private void checkConfig(AbstractFactoryDemoProperties.Alipay alipay) {
        if (StrUtil.hasBlank(alipay.getMerchantId(), alipay.getGatewayUrl())) {
            throw new IllegalStateException("支付宝配置不完整，请检查 merchant-id 和 gateway-url");
        }
    }

}
```

下面是微信支付产品族工厂。

文件位置：`src/main/java/io/github/atengk/pattern/abstractfactory/factory/WechatPlatformFactory.java`

```java
package io.github.atengk.pattern.abstractfactory.factory;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.pattern.abstractfactory.client.payment.PaymentClient;
import io.github.atengk.pattern.abstractfactory.client.payment.WechatPaymentClient;
import io.github.atengk.pattern.abstractfactory.client.refund.RefundClient;
import io.github.atengk.pattern.abstractfactory.client.refund.WechatRefundClient;
import io.github.atengk.pattern.abstractfactory.config.AbstractFactoryDemoProperties;
import io.github.atengk.pattern.abstractfactory.enums.PayPlatformEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 微信支付产品族工厂
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WechatPlatformFactory implements PaymentPlatformFactory {

    private final AbstractFactoryDemoProperties properties;

    /**
     * 获取当前工厂支持的支付平台
     *
     * @return 微信支付平台
     */
    @Override
    public PayPlatformEnum supportPlatform() {
        return PayPlatformEnum.WECHAT;
    }

    /**
     * 创建微信支付客户端
     *
     * @return 微信支付客户端
     */
    @Override
    public PaymentClient createPaymentClient() {
        AbstractFactoryDemoProperties.Wechat wechat = properties.getWechat();
        checkConfig(wechat);

        log.info("创建微信支付客户端，merchantId：{}", wechat.getMerchantId());
        return new WechatPaymentClient(wechat.getMerchantId(), wechat.getGatewayUrl());
    }

    /**
     * 创建微信退款客户端
     *
     * @return 微信退款客户端
     */
    @Override
    public RefundClient createRefundClient() {
        AbstractFactoryDemoProperties.Wechat wechat = properties.getWechat();
        checkConfig(wechat);

        log.info("创建微信退款客户端，merchantId：{}", wechat.getMerchantId());
        return new WechatRefundClient(wechat.getMerchantId(), wechat.getGatewayUrl());
    }

    /**
     * 校验微信支付配置
     *
     * @param wechat 微信支付配置
     */
    private void checkConfig(AbstractFactoryDemoProperties.Wechat wechat) {
        if (StrUtil.hasBlank(wechat.getMerchantId(), wechat.getGatewayUrl())) {
            throw new IllegalStateException("微信支付配置不完整，请检查 merchant-id 和 gateway-url");
        }
    }

}
```

## 工厂注册表

注册表用于根据平台编码找到对应的产品族工厂。这样业务层不用写 `if else` 判断支付宝或微信支付。

文件位置：`src/main/java/io/github/atengk/pattern/abstractfactory/registry/PaymentPlatformFactoryRegistry.java`

```java
package io.github.atengk.pattern.abstractfactory.registry;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.pattern.abstractfactory.enums.PayPlatformEnum;
import io.github.atengk.pattern.abstractfactory.factory.PaymentPlatformFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 支付平台工厂注册表
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class PaymentPlatformFactoryRegistry {

    private final Map<PayPlatformEnum, PaymentPlatformFactory> factoryMap;

    /**
     * 初始化支付平台工厂注册表
     *
     * @param factories 支付平台工厂列表
     */
    public PaymentPlatformFactoryRegistry(List<PaymentPlatformFactory> factories) {
        if (CollUtil.isEmpty(factories)) {
            this.factoryMap = Collections.emptyMap();
            log.warn("未发现任何支付平台工厂实现");
            return;
        }

        Map<PayPlatformEnum, PaymentPlatformFactory> tempFactoryMap = new EnumMap<>(PayPlatformEnum.class);
        for (PaymentPlatformFactory factory : factories) {
            PayPlatformEnum platform = factory.supportPlatform();
            if (tempFactoryMap.containsKey(platform)) {
                throw new IllegalStateException(StrUtil.format("支付平台存在重复工厂实现：{}", platform.getCode()));
            }

            tempFactoryMap.put(platform, factory);
            log.info("注册支付平台工厂，platform：{}，factory：{}", platform.getCode(), factory.getClass().getSimpleName());
        }

        this.factoryMap = Collections.unmodifiableMap(tempFactoryMap);
    }

    /**
     * 根据支付平台获取工厂
     *
     * @param platformCode 支付平台编码
     * @return 支付平台工厂
     */
    public PaymentPlatformFactory getFactory(String platformCode) {
        PayPlatformEnum platform = PayPlatformEnum.parse(platformCode);
        PaymentPlatformFactory factory = factoryMap.get(platform);

        if (factory == null) {
            throw new IllegalArgumentException(StrUtil.format("支付平台未注册工厂：{}", platform.getCode()));
        }

        return factory;
    }

    /**
     * 获取已注册工厂数量
     *
     * @return 已注册工厂数量
     */
    public int registeredCount() {
        return MapUtil.size(factoryMap);
    }

}
```

## 业务层调用

业务层通过注册表获取平台工厂，再由平台工厂创建当前平台下的支付客户端或退款客户端。这样可以保证支付和退款能力来自同一个平台产品族。

文件位置：`src/main/java/io/github/atengk/pattern/abstractfactory/service/PayPlatformService.java`

```java
package io.github.atengk.pattern.abstractfactory.service;

import io.github.atengk.pattern.abstractfactory.dto.PaymentCreateDTO;
import io.github.atengk.pattern.abstractfactory.dto.RefundCreateDTO;
import io.github.atengk.pattern.abstractfactory.vo.PaymentCreateVO;
import io.github.atengk.pattern.abstractfactory.vo.RefundCreateVO;

/**
 * 支付平台服务接口
 *
 * @author Ateng
 * @since 2026-05-13
 */
public interface PayPlatformService {

    /**
     * 创建支付订单
     *
     * @param createDTO 支付创建请求
     * @return 支付创建结果
     */
    PaymentCreateVO createPayment(PaymentCreateDTO createDTO);

    /**
     * 创建退款订单
     *
     * @param createDTO 退款创建请求
     * @return 退款创建结果
     */
    RefundCreateVO createRefund(RefundCreateDTO createDTO);

}
```

文件位置：`src/main/java/io/github/atengk/pattern/abstractfactory/service/PayPlatformServiceImpl.java`

```java
package io.github.atengk.pattern.abstractfactory.service;

import cn.hutool.json.JSONUtil;
import io.github.atengk.pattern.abstractfactory.client.payment.PaymentClient;
import io.github.atengk.pattern.abstractfactory.client.refund.RefundClient;
import io.github.atengk.pattern.abstractfactory.dto.PaymentCreateDTO;
import io.github.atengk.pattern.abstractfactory.dto.RefundCreateDTO;
import io.github.atengk.pattern.abstractfactory.factory.PaymentPlatformFactory;
import io.github.atengk.pattern.abstractfactory.registry.PaymentPlatformFactoryRegistry;
import io.github.atengk.pattern.abstractfactory.vo.PaymentCreateVO;
import io.github.atengk.pattern.abstractfactory.vo.RefundCreateVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 支付平台服务实现
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PayPlatformServiceImpl implements PayPlatformService {

    private final PaymentPlatformFactoryRegistry factoryRegistry;

    /**
     * 创建支付订单
     *
     * @param createDTO 支付创建请求
     * @return 支付创建结果
     */
    @Override
    public PaymentCreateVO createPayment(PaymentCreateDTO createDTO) {
        log.info("准备创建支付订单，请求参数：{}", JSONUtil.toJsonStr(createDTO));

        PaymentPlatformFactory factory = factoryRegistry.getFactory(createDTO.getPlatform());
        PaymentClient paymentClient = factory.createPaymentClient();
        PaymentCreateVO result = paymentClient.createPayment(createDTO);

        log.info("支付订单创建完成，响应结果：{}", JSONUtil.toJsonStr(result));
        return result;
    }

    /**
     * 创建退款订单
     *
     * @param createDTO 退款创建请求
     * @return 退款创建结果
     */
    @Override
    public RefundCreateVO createRefund(RefundCreateDTO createDTO) {
        log.info("准备创建退款订单，请求参数：{}", JSONUtil.toJsonStr(createDTO));

        PaymentPlatformFactory factory = factoryRegistry.getFactory(createDTO.getPlatform());
        RefundClient refundClient = factory.createRefundClient();
        RefundCreateVO result = refundClient.createRefund(createDTO);

        log.info("退款订单创建完成，响应结果：{}", JSONUtil.toJsonStr(result));
        return result;
    }

}
```

文件位置：`src/main/java/io/github/atengk/pattern/abstractfactory/controller/PayPlatformController.java`

```java
package io.github.atengk.pattern.abstractfactory.controller;

import io.github.atengk.pattern.abstractfactory.dto.PaymentCreateDTO;
import io.github.atengk.pattern.abstractfactory.dto.RefundCreateDTO;
import io.github.atengk.pattern.abstractfactory.service.PayPlatformService;
import io.github.atengk.pattern.abstractfactory.vo.PaymentCreateVO;
import io.github.atengk.pattern.abstractfactory.vo.RefundCreateVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 支付平台接口
 *
 * @author Ateng
 * @since 2026-05-13
 */
@RestController
@RequestMapping("/api/patterns/abstract-factory/pay-platforms")
@RequiredArgsConstructor
public class PayPlatformController {

    private final PayPlatformService payPlatformService;

    /**
     * 创建支付订单
     *
     * @param createDTO 支付创建请求
     * @return 支付创建结果
     */
    @PostMapping("/payments")
    public PaymentCreateVO createPayment(@Valid @RequestBody PaymentCreateDTO createDTO) {
        return payPlatformService.createPayment(createDTO);
    }

    /**
     * 创建退款订单
     *
     * @param createDTO 退款创建请求
     * @return 退款创建结果
     */
    @PostMapping("/refunds")
    public RefundCreateVO createRefund(@Valid @RequestBody RefundCreateDTO createDTO) {
        return payPlatformService.createRefund(createDTO);
    }

}
```

## 使用方式

启动项目后，可以通过不同平台调用支付和退款接口。抽象工厂会确保同一个平台下的支付客户端和退款客户端来自同一产品族。

支付宝支付请求：

```bash
curl -X POST "http://localhost:8080/api/patterns/abstract-factory/pay-platforms/payments" \
  -H "Content-Type: application/json" \
  -d '{
    "platform": "ALIPAY",
    "orderNo": "ORDER_10001",
    "userId": "USER_001",
    "amount": 99.90
  }'
```

响应示例：

```json
{
  "platform": "ALIPAY",
  "orderNo": "ORDER_10001",
  "payTradeNo": "ALI_PAY_9f86d081884c7d659a2feaa0c55ad015",
  "amount": 99.90,
  "success": true,
  "message": "支付宝支付订单创建成功",
  "createTime": "2026-05-13 13:10:30"
}
```

支付宝退款请求：

```bash
curl -X POST "http://localhost:8080/api/patterns/abstract-factory/pay-platforms/refunds" \
  -H "Content-Type: application/json" \
  -d '{
    "platform": "ALIPAY",
    "payTradeNo": "ALI_PAY_9f86d081884c7d659a2feaa0c55ad015",
    "refundNo": "REFUND_10001",
    "refundAmount": 99.90
  }'
```

响应示例：

```json
{
  "platform": "ALIPAY",
  "refundNo": "REFUND_10001",
  "refundTradeNo": "ALI_REFUND_c4ca4238a0b923820dcc509a6f75849b",
  "refundAmount": 99.90,
  "success": true,
  "message": "支付宝退款订单创建成功",
  "createTime": "2026-05-13 13:11:05"
}
```

微信支付请求：

```bash
curl -X POST "http://localhost:8080/api/patterns/abstract-factory/pay-platforms/payments" \
  -H "Content-Type: application/json" \
  -d '{
    "platform": "WECHAT",
    "orderNo": "ORDER_10002",
    "userId": "USER_002",
    "amount": 128.50
  }'
```

响应示例：

```json
{
  "platform": "WECHAT",
  "orderNo": "ORDER_10002",
  "payTradeNo": "WX_PAY_eccbc87e4b5ce2fe28308fd9f2a7baf3",
  "amount": 128.50,
  "success": true,
  "message": "微信支付订单创建成功",
  "createTime": "2026-05-13 13:12:16"
}
```

## 新增产品族

如果新增一个支付平台，例如银联支付 `UNION_PAY`，抽象工厂模式的扩展路径是新增一个完整产品族。

首先在枚举中新增平台：

```java
UNION_PAY("UNION_PAY", "银联支付");
```

然后新增银联支付客户端、银联退款客户端：

```text
UnionPayPaymentClient
UnionPayRefundClient
```

最后新增银联产品族工厂：

```java
package io.github.atengk.pattern.abstractfactory.factory;

import io.github.atengk.pattern.abstractfactory.client.payment.PaymentClient;
import io.github.atengk.pattern.abstractfactory.client.refund.RefundClient;
import io.github.atengk.pattern.abstractfactory.enums.PayPlatformEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 银联支付产品族工厂
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class UnionPayPlatformFactory implements PaymentPlatformFactory {

    /**
     * 获取当前工厂支持的支付平台
     *
     * @return 银联支付平台
     */
    @Override
    public PayPlatformEnum supportPlatform() {
        return PayPlatformEnum.UNION_PAY;
    }

    /**
     * 创建银联支付客户端
     *
     * @return 银联支付客户端
     */
    @Override
    public PaymentClient createPaymentClient() {
        log.info("创建银联支付客户端");
        return new UnionPayPaymentClient();
    }

    /**
     * 创建银联退款客户端
     *
     * @return 银联退款客户端
     */
    @Override
    public RefundClient createRefundClient() {
        log.info("创建银联退款客户端");
        return new UnionPayRefundClient();
    }

}
```

由于 `PaymentPlatformFactoryRegistry` 会自动收集所有 `PaymentPlatformFactory` Bean，新增平台工厂后，业务层不需要修改。

## 验证方式

可以通过单元测试验证不同平台工厂创建出来的产品族是否一致。

文件位置：`src/test/java/io/github/atengk/pattern/abstractfactory/PaymentPlatformFactoryRegistryTest.java`

```java
package io.github.atengk.pattern.abstractfactory;

import io.github.atengk.pattern.abstractfactory.client.payment.AlipayPaymentClient;
import io.github.atengk.pattern.abstractfactory.client.payment.PaymentClient;
import io.github.atengk.pattern.abstractfactory.client.payment.WechatPaymentClient;
import io.github.atengk.pattern.abstractfactory.client.refund.AlipayRefundClient;
import io.github.atengk.pattern.abstractfactory.client.refund.RefundClient;
import io.github.atengk.pattern.abstractfactory.client.refund.WechatRefundClient;
import io.github.atengk.pattern.abstractfactory.factory.PaymentPlatformFactory;
import io.github.atengk.pattern.abstractfactory.registry.PaymentPlatformFactoryRegistry;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 支付平台抽象工厂测试
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@SpringBootTest
class PaymentPlatformFactoryRegistryTest {

    private final PaymentPlatformFactoryRegistry factoryRegistry;

    PaymentPlatformFactoryRegistryTest(PaymentPlatformFactoryRegistry factoryRegistry) {
        this.factoryRegistry = factoryRegistry;
    }

    /**
     * 验证支付宝产品族创建结果
     */
    @Test
    void shouldCreateAlipayProductFamily() {
        PaymentPlatformFactory factory = factoryRegistry.getFactory("ALIPAY");

        PaymentClient paymentClient = factory.createPaymentClient();
        RefundClient refundClient = factory.createRefundClient();

        log.info("支付宝支付客户端：{}", paymentClient.getClass().getSimpleName());
        log.info("支付宝退款客户端：{}", refundClient.getClass().getSimpleName());

        Assertions.assertInstanceOf(AlipayPaymentClient.class, paymentClient);
        Assertions.assertInstanceOf(AlipayRefundClient.class, refundClient);
    }

    /**
     * 验证微信支付产品族创建结果
     */
    @Test
    void shouldCreateWechatProductFamily() {
        PaymentPlatformFactory factory = factoryRegistry.getFactory("WECHAT");

        PaymentClient paymentClient = factory.createPaymentClient();
        RefundClient refundClient = factory.createRefundClient();

        log.info("微信支付客户端：{}", paymentClient.getClass().getSimpleName());
        log.info("微信退款客户端：{}", refundClient.getClass().getSimpleName());

        Assertions.assertInstanceOf(WechatPaymentClient.class, paymentClient);
        Assertions.assertInstanceOf(WechatRefundClient.class, refundClient);
    }

    /**
     * 验证不支持的平台会抛出异常
     */
    @Test
    void shouldThrowExceptionWhenPlatformUnsupported() {
        IllegalArgumentException exception = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> factoryRegistry.getFactory("UNKNOWN")
        );

        log.info("不支持支付平台异常信息：{}", exception.getMessage());
        Assertions.assertTrue(exception.getMessage().contains("不支持的支付平台"));
    }

}
```

执行测试：

```bash
mvn test -Dtest=PaymentPlatformFactoryRegistryTest
```

命令说明：`-Dtest=PaymentPlatformFactoryRegistryTest` 表示只运行当前抽象工厂测试类，用于快速验证不同平台下产品族创建是否正确。

## 和工厂方法模式的区别

工厂方法模式通常面向单个产品对象，例如只创建一个 `PayClient`。抽象工厂模式面向一组相关产品对象，例如同一个支付平台下同时创建 `PaymentClient`、`RefundClient`、`QueryClient`、`CloseClient`。

可以按以下规则区分：

```text
只创建一个产品对象：优先考虑工厂方法模式
创建一组相关对象：优先考虑抽象工厂模式
对象只是简单按类型分发：优先考虑简单工厂模式
产品族内部对象必须保持平台一致：优先考虑抽象工厂模式
```

在支付系统中，支付宝支付、退款、查询、关单、回调验签都属于支付宝产品族；微信支付、退款、查询、关单、回调验签都属于微信产品族。如果这些对象混用，例如支付宝支付客户端搭配微信退款客户端，系统就会出现平台不一致的问题。抽象工厂模式正适合解决这种一致性问题。

## 开发建议

在 Spring Boot 项目中使用抽象工厂模式时，建议遵循以下原则：

```text
先确认是否真的存在产品族，而不是只有单个对象
产品族中的多个产品接口要相对稳定
具体工厂只负责创建同一产品族下的对象
业务层依赖抽象工厂和抽象产品，不直接依赖具体产品类
工厂注册可以交给 Spring 容器和 Map 完成
新增产品族时新增具体工厂和具体产品类
新增产品接口时需要修改所有具体工厂，要谨慎
```

抽象工厂模式的优势是产品族一致性强，扩展新产品族比较清晰。它的缺点是新增一个新的产品类型时成本较高。例如当前只有支付和退款，如果要新增“支付查询客户端”，则 `PaymentPlatformFactory` 接口和所有具体工厂都要增加 `createQueryClient()` 方法。

因此，抽象工厂更适合产品族结构稳定、平台扩展频繁的场景。

## 总结

抽象工厂模式用于创建一组相关或相互依赖的对象。它比工厂方法模式更强调“产品族”，适合多平台、多厂商、多环境、多协议等场景。

本示例的核心流程可以概括为：

```text
定义多个产品接口：PaymentClient、RefundClient
为每个平台实现一组具体产品：支付宝产品族、微信产品族
定义抽象工厂接口：PaymentPlatformFactory
每个具体工厂创建自己平台下的一组产品
Spring 启动时注册所有具体工厂
业务层根据平台获取工厂
通过工厂创建同一产品族下的客户端
调用客户端完成支付或退款业务
```

在真实 Spring Boot 项目中，抽象工厂常用于支付平台、云存储厂商、消息通道、第三方开放平台、多数据库适配、多环境 SDK 客户端等模块。它的核心价值是保证同一业务维度下的一组对象来自同一个产品族，从而减少对象混用和创建逻辑散落的问题。
