# 工厂方法模式

工厂方法模式属于创建型模式，用于把对象创建逻辑延迟到具体工厂类中。它和简单工厂模式的区别在于：简单工厂通常由一个工厂类根据类型返回对象；工厂方法模式则为每一种产品提供一个对应的工厂实现，由具体工厂决定创建哪一种对象。当前设计模式总览中，工厂方法模式属于 GoF 创建型模式，模块名为 `factorymethod`。

## 基础配置

本示例基于 JDK 21、Spring Boot 3、Maven、Hutool、Lombok 编写。示例场景是“多支付渠道下单”，系统支持支付宝、微信支付、余额支付。每一种支付渠道都有自己的客户端创建逻辑，因此使用工厂方法模式将不同客户端的创建过程放到不同工厂中。

文件位置：`pom.xml`

```xml
<dependencies>
    <!-- Spring Boot Web，用于提供支付下单接口 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Spring Boot Validation，用于请求参数校验 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- Hutool 工具类，用于字符串、日期、金额、JSON 等常用处理 -->
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

    <!-- Spring Boot 测试依赖，用于验证工厂方法选择和客户端创建逻辑 -->
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
  factory-method:
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
    balance:
      # 余额支付内部账户编码，示例值
      account-code: BALANCE_ACCOUNT_30001
```

## 模式说明

工厂方法模式的核心是定义一个创建对象的工厂接口，把具体对象的创建交给子类工厂完成。调用方依赖抽象工厂和抽象产品，不直接依赖具体产品类。

在本示例中：

```text
PayClient：支付客户端产品接口
AlipayPayClient：支付宝客户端产品
WechatPayClient：微信支付客户端产品
BalancePayClient：余额支付客户端产品

PayClientFactory：支付客户端工厂接口
AlipayPayClientFactory：支付宝客户端工厂
WechatPayClientFactory：微信支付客户端工厂
BalancePayClientFactory：余额支付客户端工厂
```

工厂方法模式适合以下场景：

```text
对象创建逻辑比较复杂
不同产品有不同初始化参数
不同产品创建前需要不同校验
新增产品时希望新增一个工厂类，而不是修改统一工厂的大量分支
调用方只关心产品接口，不关心产品创建细节
```

在 Spring Boot 项目中，工厂方法模式常见于支付客户端、短信客户端、存储客户端、文件解析器、导出器、消息生产者、第三方平台适配客户端等场景。

## 项目结构

本示例使用 Spring Bean 管理具体工厂。业务层根据支付渠道找到对应工厂，再调用工厂方法创建支付客户端。

```text
src/main/java/io/github/atengk/pattern/factorymethod
├── FactoryMethodApplication.java
├── client
│   ├── PayClient.java
│   ├── AlipayPayClient.java
│   ├── WechatPayClient.java
│   └── BalancePayClient.java
├── config
│   └── FactoryMethodDemoProperties.java
├── controller
│   └── PayOrderController.java
├── dto
│   └── PayOrderCreateDTO.java
├── enums
│   └── PayChannelEnum.java
├── factory
│   ├── PayClientFactory.java
│   ├── AlipayPayClientFactory.java
│   ├── WechatPayClientFactory.java
│   └── BalancePayClientFactory.java
├── registry
│   └── PayClientFactoryRegistry.java
├── service
│   ├── PayOrderService.java
│   └── PayOrderServiceImpl.java
└── vo
    └── PayOrderCreateVO.java
```

## 核心代码

这一部分给出工厂方法模式的完整核心代码。重点是 `PayClientFactory` 接口和多个具体工厂类，每个具体工厂类都通过 `createClient()` 创建自己的支付客户端。

文件位置：`src/main/java/io/github/atengk/pattern/factorymethod/FactoryMethodApplication.java`

```java
package io.github.atengk.pattern.factorymethod;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 工厂方法模式示例启动类
 *
 * @author Ateng
 * @since 2026-05-13
 */
@SpringBootApplication
public class FactoryMethodApplication {

    /**
     * 启动工厂方法模式示例应用
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(FactoryMethodApplication.class, args);
    }

}
```

文件位置：`src/main/java/io/github/atengk/pattern/factorymethod/config/FactoryMethodDemoProperties.java`

```java
package io.github.atengk.pattern.factorymethod.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 工厂方法模式示例配置
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
@Component
@ConfigurationProperties(prefix = "demo.factory-method")
public class FactoryMethodDemoProperties {

    /**
     * 支付宝配置
     */
    private Alipay alipay = new Alipay();

    /**
     * 微信支付配置
     */
    private Wechat wechat = new Wechat();

    /**
     * 余额支付配置
     */
    private Balance balance = new Balance();

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

    /**
     * 余额支付配置项
     *
     * @author Ateng
     * @since 2026-05-13
     */
    @Data
    public static class Balance {

        /**
         * 内部账户编码
         */
        private String accountCode;

    }

}
```

下面的枚举用于定义支付渠道，避免在业务代码中直接使用字符串。

文件位置：`src/main/java/io/github/atengk/pattern/factorymethod/enums/PayChannelEnum.java`

```java
package io.github.atengk.pattern.factorymethod.enums;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;

import java.util.Arrays;

/**
 * 支付渠道枚举
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Getter
public enum PayChannelEnum {

    ALIPAY("ALIPAY", "支付宝"),

    WECHAT("WECHAT", "微信支付"),

    BALANCE("BALANCE", "余额支付");

    private final String code;

    private final String description;

    PayChannelEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根据编码解析支付渠道
     *
     * @param code 支付渠道编码
     * @return 支付渠道
     */
    public static PayChannelEnum parse(String code) {
        return Arrays.stream(values())
                .filter(item -> StrUtil.equalsIgnoreCase(item.getCode(), code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(StrUtil.format("不支持的支付渠道：{}", code)));
    }

}
```

文件位置：`src/main/java/io/github/atengk/pattern/factorymethod/dto/PayOrderCreateDTO.java`

```java
package io.github.atengk.pattern.factorymethod.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 支付订单创建请求对象
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
public class PayOrderCreateDTO {

    /**
     * 支付渠道：ALIPAY、WECHAT、BALANCE
     */
    @NotBlank(message = "支付渠道不能为空")
    private String payChannel;

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

文件位置：`src/main/java/io/github/atengk/pattern/factorymethod/vo/PayOrderCreateVO.java`

```java
package io.github.atengk.pattern.factorymethod.vo;

import java.math.BigDecimal;

/**
 * 支付订单创建响应对象
 *
 * @author Ateng
 * @since 2026-05-13
 */
public record PayOrderCreateVO(
        String payChannel,
        String orderNo,
        String payTradeNo,
        BigDecimal amount,
        Boolean success,
        String message,
        String createTime
) {
}
```

## 产品接口和具体产品

这一部分定义支付客户端产品。业务层不直接依赖支付宝、微信或余额支付客户端，而是统一依赖 `PayClient` 接口。

文件位置：`src/main/java/io/github/atengk/pattern/factorymethod/client/PayClient.java`

```java
package io.github.atengk.pattern.factorymethod.client;

import io.github.atengk.pattern.factorymethod.dto.PayOrderCreateDTO;
import io.github.atengk.pattern.factorymethod.vo.PayOrderCreateVO;

/**
 * 支付客户端接口
 *
 * @author Ateng
 * @since 2026-05-13
 */
public interface PayClient {

    /**
     * 创建支付订单
     *
     * @param createDTO 支付订单创建请求
     * @return 支付订单创建结果
     */
    PayOrderCreateVO createPayOrder(PayOrderCreateDTO createDTO);

}
```

下面是支付宝支付客户端。示例中没有真实调用支付宝网关，只模拟创建支付交易号。

文件位置：`src/main/java/io/github/atengk/pattern/factorymethod/client/AlipayPayClient.java`

```java
package io.github.atengk.pattern.factorymethod.client;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.pattern.factorymethod.dto.PayOrderCreateDTO;
import io.github.atengk.pattern.factorymethod.enums.PayChannelEnum;
import io.github.atengk.pattern.factorymethod.vo.PayOrderCreateVO;
import lombok.extern.slf4j.Slf4j;

/**
 * 支付宝支付客户端
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
public class AlipayPayClient implements PayClient {

    private final String merchantId;

    private final String gatewayUrl;

    public AlipayPayClient(String merchantId, String gatewayUrl) {
        this.merchantId = merchantId;
        this.gatewayUrl = gatewayUrl;
    }

    /**
     * 创建支付宝支付订单
     *
     * @param createDTO 支付订单创建请求
     * @return 支付订单创建结果
     */
    @Override
    public PayOrderCreateVO createPayOrder(PayOrderCreateDTO createDTO) {
        String tradeNo = StrUtil.format("ALI_{}", IdUtil.fastSimpleUUID());

        log.info("创建支付宝支付订单，merchantId：{}，gatewayUrl：{}，orderNo：{}，amount：{}",
                merchantId, gatewayUrl, createDTO.getOrderNo(), createDTO.getAmount());

        return new PayOrderCreateVO(
                PayChannelEnum.ALIPAY.getCode(),
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

下面是微信支付客户端。

文件位置：`src/main/java/io/github/atengk/pattern/factorymethod/client/WechatPayClient.java`

```java
package io.github.atengk.pattern.factorymethod.client;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.pattern.factorymethod.dto.PayOrderCreateDTO;
import io.github.atengk.pattern.factorymethod.enums.PayChannelEnum;
import io.github.atengk.pattern.factorymethod.vo.PayOrderCreateVO;
import lombok.extern.slf4j.Slf4j;

/**
 * 微信支付客户端
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
public class WechatPayClient implements PayClient {

    private final String merchantId;

    private final String gatewayUrl;

    public WechatPayClient(String merchantId, String gatewayUrl) {
        this.merchantId = merchantId;
        this.gatewayUrl = gatewayUrl;
    }

    /**
     * 创建微信支付订单
     *
     * @param createDTO 支付订单创建请求
     * @return 支付订单创建结果
     */
    @Override
    public PayOrderCreateVO createPayOrder(PayOrderCreateDTO createDTO) {
        String tradeNo = StrUtil.format("WX_{}", IdUtil.fastSimpleUUID());

        log.info("创建微信支付订单，merchantId：{}，gatewayUrl：{}，orderNo：{}，amount：{}",
                merchantId, gatewayUrl, createDTO.getOrderNo(), createDTO.getAmount());

        return new PayOrderCreateVO(
                PayChannelEnum.WECHAT.getCode(),
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

下面是余额支付客户端。

文件位置：`src/main/java/io/github/atengk/pattern/factorymethod/client/BalancePayClient.java`

```java
package io.github.atengk.pattern.factorymethod.client;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.pattern.factorymethod.dto.PayOrderCreateDTO;
import io.github.atengk.pattern.factorymethod.enums.PayChannelEnum;
import io.github.atengk.pattern.factorymethod.vo.PayOrderCreateVO;
import lombok.extern.slf4j.Slf4j;

/**
 * 余额支付客户端
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
public class BalancePayClient implements PayClient {

    private final String accountCode;

    public BalancePayClient(String accountCode) {
        this.accountCode = accountCode;
    }

    /**
     * 创建余额支付订单
     *
     * @param createDTO 支付订单创建请求
     * @return 支付订单创建结果
     */
    @Override
    public PayOrderCreateVO createPayOrder(PayOrderCreateDTO createDTO) {
        String tradeNo = StrUtil.format("BAL_{}", IdUtil.fastSimpleUUID());

        log.info("创建余额支付订单，accountCode：{}，userId：{}，orderNo：{}，amount：{}",
                accountCode, createDTO.getUserId(), createDTO.getOrderNo(), createDTO.getAmount());

        return new PayOrderCreateVO(
                PayChannelEnum.BALANCE.getCode(),
                createDTO.getOrderNo(),
                tradeNo,
                createDTO.getAmount(),
                true,
                "余额支付订单创建成功",
                DateUtil.now()
        );
    }

}
```

## 工厂接口和具体工厂

这一部分是工厂方法模式的核心。`PayClientFactory` 定义工厂方法 `createClient()`，具体工厂类负责创建具体支付客户端。

文件位置：`src/main/java/io/github/atengk/pattern/factorymethod/factory/PayClientFactory.java`

```java
package io.github.atengk.pattern.factorymethod.factory;

import io.github.atengk.pattern.factorymethod.client.PayClient;
import io.github.atengk.pattern.factorymethod.enums.PayChannelEnum;

/**
 * 支付客户端工厂接口
 *
 * @author Ateng
 * @since 2026-05-13
 */
public interface PayClientFactory {

    /**
     * 获取当前工厂支持的支付渠道
     *
     * @return 支付渠道
     */
    PayChannelEnum supportChannel();

    /**
     * 创建支付客户端
     *
     * @return 支付客户端
     */
    PayClient createClient();

}
```

下面是支付宝客户端工厂。它只负责创建支付宝客户端，并处理支付宝客户端的配置校验。

文件位置：`src/main/java/io/github/atengk/pattern/factorymethod/factory/AlipayPayClientFactory.java`

```java
package io.github.atengk.pattern.factorymethod.factory;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.pattern.factorymethod.client.AlipayPayClient;
import io.github.atengk.pattern.factorymethod.client.PayClient;
import io.github.atengk.pattern.factorymethod.config.FactoryMethodDemoProperties;
import io.github.atengk.pattern.factorymethod.enums.PayChannelEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 支付宝支付客户端工厂
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlipayPayClientFactory implements PayClientFactory {

    private final FactoryMethodDemoProperties properties;

    /**
     * 获取当前工厂支持的支付渠道
     *
     * @return 支付宝支付渠道
     */
    @Override
    public PayChannelEnum supportChannel() {
        return PayChannelEnum.ALIPAY;
    }

    /**
     * 创建支付宝支付客户端
     *
     * @return 支付宝支付客户端
     */
    @Override
    public PayClient createClient() {
        FactoryMethodDemoProperties.Alipay alipay = properties.getAlipay();

        if (StrUtil.hasBlank(alipay.getMerchantId(), alipay.getGatewayUrl())) {
            throw new IllegalStateException("支付宝配置不完整，请检查 merchant-id 和 gateway-url");
        }

        log.info("创建支付宝支付客户端，merchantId：{}", alipay.getMerchantId());
        return new AlipayPayClient(alipay.getMerchantId(), alipay.getGatewayUrl());
    }

}
```

下面是微信支付客户端工厂。

文件位置：`src/main/java/io/github/atengk/pattern/factorymethod/factory/WechatPayClientFactory.java`

```java
package io.github.atengk.pattern.factorymethod.factory;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.pattern.factorymethod.client.PayClient;
import io.github.atengk.pattern.factorymethod.client.WechatPayClient;
import io.github.atengk.pattern.factorymethod.config.FactoryMethodDemoProperties;
import io.github.atengk.pattern.factorymethod.enums.PayChannelEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 微信支付客户端工厂
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WechatPayClientFactory implements PayClientFactory {

    private final FactoryMethodDemoProperties properties;

    /**
     * 获取当前工厂支持的支付渠道
     *
     * @return 微信支付渠道
     */
    @Override
    public PayChannelEnum supportChannel() {
        return PayChannelEnum.WECHAT;
    }

    /**
     * 创建微信支付客户端
     *
     * @return 微信支付客户端
     */
    @Override
    public PayClient createClient() {
        FactoryMethodDemoProperties.Wechat wechat = properties.getWechat();

        if (StrUtil.hasBlank(wechat.getMerchantId(), wechat.getGatewayUrl())) {
            throw new IllegalStateException("微信支付配置不完整，请检查 merchant-id 和 gateway-url");
        }

        log.info("创建微信支付客户端，merchantId：{}", wechat.getMerchantId());
        return new WechatPayClient(wechat.getMerchantId(), wechat.getGatewayUrl());
    }

}
```

下面是余额支付客户端工厂。

文件位置：`src/main/java/io/github/atengk/pattern/factorymethod/factory/BalancePayClientFactory.java`

```java
package io.github.atengk.pattern.factorymethod.factory;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.pattern.factorymethod.client.BalancePayClient;
import io.github.atengk.pattern.factorymethod.client.PayClient;
import io.github.atengk.pattern.factorymethod.config.FactoryMethodDemoProperties;
import io.github.atengk.pattern.factorymethod.enums.PayChannelEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 余额支付客户端工厂
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BalancePayClientFactory implements PayClientFactory {

    private final FactoryMethodDemoProperties properties;

    /**
     * 获取当前工厂支持的支付渠道
     *
     * @return 余额支付渠道
     */
    @Override
    public PayChannelEnum supportChannel() {
        return PayChannelEnum.BALANCE;
    }

    /**
     * 创建余额支付客户端
     *
     * @return 余额支付客户端
     */
    @Override
    public PayClient createClient() {
        FactoryMethodDemoProperties.Balance balance = properties.getBalance();

        if (StrUtil.isBlank(balance.getAccountCode())) {
            throw new IllegalStateException("余额支付配置不完整，请检查 account-code");
        }

        log.info("创建余额支付客户端，accountCode：{}", balance.getAccountCode());
        return new BalancePayClient(balance.getAccountCode());
    }

}
```

## 工厂注册表

工厂方法模式本身不要求必须有注册表。这里增加 `PayClientFactoryRegistry`，是为了让 Spring Boot 项目可以根据请求渠道快速找到对应工厂，同时避免在 Service 中写大量 `if else`。

文件位置：`src/main/java/io/github/atengk/pattern/factorymethod/registry/PayClientFactoryRegistry.java`

```java
package io.github.atengk.pattern.factorymethod.registry;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.pattern.factorymethod.enums.PayChannelEnum;
import io.github.atengk.pattern.factorymethod.factory.PayClientFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 支付客户端工厂注册表
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class PayClientFactoryRegistry {

    private final Map<PayChannelEnum, PayClientFactory> factoryMap;

    /**
     * 初始化支付客户端工厂注册表
     *
     * @param payClientFactories 支付客户端工厂列表
     */
    public PayClientFactoryRegistry(List<PayClientFactory> payClientFactories) {
        if (CollUtil.isEmpty(payClientFactories)) {
            this.factoryMap = Collections.emptyMap();
            log.warn("未发现任何支付客户端工厂实现");
            return;
        }

        Map<PayChannelEnum, PayClientFactory> tempFactoryMap = new EnumMap<>(PayChannelEnum.class);
        for (PayClientFactory factory : payClientFactories) {
            PayChannelEnum channel = factory.supportChannel();
            if (tempFactoryMap.containsKey(channel)) {
                throw new IllegalStateException(StrUtil.format("支付渠道存在重复工厂实现：{}", channel.getCode()));
            }

            tempFactoryMap.put(channel, factory);
            log.info("注册支付客户端工厂，channel：{}，factory：{}", channel.getCode(), factory.getClass().getSimpleName());
        }

        this.factoryMap = Collections.unmodifiableMap(tempFactoryMap);
    }

    /**
     * 根据支付渠道获取工厂
     *
     * @param channelCode 支付渠道编码
     * @return 支付客户端工厂
     */
    public PayClientFactory getFactory(String channelCode) {
        PayChannelEnum channel = PayChannelEnum.parse(channelCode);
        PayClientFactory factory = factoryMap.get(channel);

        if (factory == null) {
            throw new IllegalArgumentException(StrUtil.format("支付渠道未注册工厂：{}", channel.getCode()));
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

业务层不直接创建支付客户端，而是先通过注册表找到对应工厂，再调用工厂方法创建客户端，最后调用客户端执行业务。

文件位置：`src/main/java/io/github/atengk/pattern/factorymethod/service/PayOrderService.java`

```java
package io.github.atengk.pattern.factorymethod.service;

import io.github.atengk.pattern.factorymethod.dto.PayOrderCreateDTO;
import io.github.atengk.pattern.factorymethod.vo.PayOrderCreateVO;

/**
 * 支付订单服务接口
 *
 * @author Ateng
 * @since 2026-05-13
 */
public interface PayOrderService {

    /**
     * 创建支付订单
     *
     * @param createDTO 支付订单创建请求
     * @return 支付订单创建结果
     */
    PayOrderCreateVO createPayOrder(PayOrderCreateDTO createDTO);

}
```

文件位置：`src/main/java/io/github/atengk/pattern/factorymethod/service/PayOrderServiceImpl.java`

```java
package io.github.atengk.pattern.factorymethod.service;

import cn.hutool.json.JSONUtil;
import io.github.atengk.pattern.factorymethod.client.PayClient;
import io.github.atengk.pattern.factorymethod.dto.PayOrderCreateDTO;
import io.github.atengk.pattern.factorymethod.factory.PayClientFactory;
import io.github.atengk.pattern.factorymethod.registry.PayClientFactoryRegistry;
import io.github.atengk.pattern.factorymethod.vo.PayOrderCreateVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 支付订单服务实现
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PayOrderServiceImpl implements PayOrderService {

    private final PayClientFactoryRegistry payClientFactoryRegistry;

    /**
     * 创建支付订单
     *
     * @param createDTO 支付订单创建请求
     * @return 支付订单创建结果
     */
    @Override
    public PayOrderCreateVO createPayOrder(PayOrderCreateDTO createDTO) {
        log.info("准备创建支付订单，请求参数：{}", JSONUtil.toJsonStr(createDTO));

        PayClientFactory factory = payClientFactoryRegistry.getFactory(createDTO.getPayChannel());
        PayClient payClient = factory.createClient();
        PayOrderCreateVO result = payClient.createPayOrder(createDTO);

        log.info("支付订单创建完成，响应结果：{}", JSONUtil.toJsonStr(result));
        return result;
    }

}
```

文件位置：`src/main/java/io/github/atengk/pattern/factorymethod/controller/PayOrderController.java`

```java
package io.github.atengk.pattern.factorymethod.controller;

import io.github.atengk.pattern.factorymethod.dto.PayOrderCreateDTO;
import io.github.atengk.pattern.factorymethod.service.PayOrderService;
import io.github.atengk.pattern.factorymethod.vo.PayOrderCreateVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 支付订单接口
 *
 * @author Ateng
 * @since 2026-05-13
 */
@RestController
@RequestMapping("/api/patterns/factory-method/pay-orders")
@RequiredArgsConstructor
public class PayOrderController {

    private final PayOrderService payOrderService;

    /**
     * 创建支付订单
     *
     * @param createDTO 支付订单创建请求
     * @return 支付订单创建结果
     */
    @PostMapping
    public PayOrderCreateVO createPayOrder(@Valid @RequestBody PayOrderCreateDTO createDTO) {
        return payOrderService.createPayOrder(createDTO);
    }

}
```

## 使用方式

启动项目后，通过不同的 `payChannel` 创建不同渠道的支付订单。业务层不需要知道具体客户端如何创建，只需要通过工厂方法拿到 `PayClient`。

接口地址：

```text
POST /api/patterns/factory-method/pay-orders
```

支付宝支付请求：

```bash
curl -X POST "http://localhost:8080/api/patterns/factory-method/pay-orders" \
  -H "Content-Type: application/json" \
  -d '{
    "payChannel": "ALIPAY",
    "orderNo": "ORDER_10001",
    "userId": "USER_001",
    "amount": 99.90
  }'
```

响应示例：

```json
{
  "payChannel": "ALIPAY",
  "orderNo": "ORDER_10001",
  "payTradeNo": "ALI_8dff930bbfed4238a4c2ba8fa7f4a7bc",
  "amount": 99.90,
  "success": true,
  "message": "支付宝支付订单创建成功",
  "createTime": "2026-05-13 12:10:30"
}
```

微信支付请求：

```bash
curl -X POST "http://localhost:8080/api/patterns/factory-method/pay-orders" \
  -H "Content-Type: application/json" \
  -d '{
    "payChannel": "WECHAT",
    "orderNo": "ORDER_10002",
    "userId": "USER_002",
    "amount": 128.50
  }'
```

响应示例：

```json
{
  "payChannel": "WECHAT",
  "orderNo": "ORDER_10002",
  "payTradeNo": "WX_a813fd1fbb674f4cba3111ae54bbf435",
  "amount": 128.50,
  "success": true,
  "message": "微信支付订单创建成功",
  "createTime": "2026-05-13 12:11:45"
}
```

余额支付请求：

```bash
curl -X POST "http://localhost:8080/api/patterns/factory-method/pay-orders" \
  -H "Content-Type: application/json" \
  -d '{
    "payChannel": "BALANCE",
    "orderNo": "ORDER_10003",
    "userId": "USER_003",
    "amount": 36.80
  }'
```

响应示例：

```json
{
  "payChannel": "BALANCE",
  "orderNo": "ORDER_10003",
  "payTradeNo": "BAL_eb3f6e501fd34f048a5b90ad73cd2539",
  "amount": 36.80,
  "success": true,
  "message": "余额支付订单创建成功",
  "createTime": "2026-05-13 12:12:16"
}
```

## 新增支付渠道

如果需要新增一种支付渠道，例如银联支付 `UNION_PAY`，工厂方法模式的扩展步骤比较清晰。

先在枚举中新增渠道：

```java
UNION_PAY("UNION_PAY", "银联支付");
```

然后新增银联支付客户端：

```java
package io.github.atengk.pattern.factorymethod.client;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.pattern.factorymethod.dto.PayOrderCreateDTO;
import io.github.atengk.pattern.factorymethod.enums.PayChannelEnum;
import io.github.atengk.pattern.factorymethod.vo.PayOrderCreateVO;
import lombok.extern.slf4j.Slf4j;

/**
 * 银联支付客户端
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
public class UnionPayClient implements PayClient {

    /**
     * 创建银联支付订单
     *
     * @param createDTO 支付订单创建请求
     * @return 支付订单创建结果
     */
    @Override
    public PayOrderCreateVO createPayOrder(PayOrderCreateDTO createDTO) {
        String tradeNo = StrUtil.format("UNION_{}", IdUtil.fastSimpleUUID());

        log.info("创建银联支付订单，orderNo：{}，amount：{}", createDTO.getOrderNo(), createDTO.getAmount());

        return new PayOrderCreateVO(
                PayChannelEnum.UNION_PAY.getCode(),
                createDTO.getOrderNo(),
                tradeNo,
                createDTO.getAmount(),
                true,
                "银联支付订单创建成功",
                DateUtil.now()
        );
    }

}
```

再新增银联支付客户端工厂：

```java
package io.github.atengk.pattern.factorymethod.factory;

import io.github.atengk.pattern.factorymethod.client.PayClient;
import io.github.atengk.pattern.factorymethod.client.UnionPayClient;
import io.github.atengk.pattern.factorymethod.enums.PayChannelEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 银联支付客户端工厂
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class UnionPayClientFactory implements PayClientFactory {

    /**
     * 获取当前工厂支持的支付渠道
     *
     * @return 银联支付渠道
     */
    @Override
    public PayChannelEnum supportChannel() {
        return PayChannelEnum.UNION_PAY;
    }

    /**
     * 创建银联支付客户端
     *
     * @return 银联支付客户端
     */
    @Override
    public PayClient createClient() {
        log.info("创建银联支付客户端");
        return new UnionPayClient();
    }

}
```

由于 `PayClientFactoryRegistry` 会自动收集所有 `PayClientFactory` Bean，新增工厂类后，不需要修改注册表和业务 Service。

## 验证方式

可以通过单元测试验证不同渠道是否能获取不同工厂，并创建不同客户端。

文件位置：`src/test/java/io/github/atengk/pattern/factorymethod/PayClientFactoryRegistryTest.java`

```java
package io.github.atengk.pattern.factorymethod;

import io.github.atengk.pattern.factorymethod.client.AlipayPayClient;
import io.github.atengk.pattern.factorymethod.client.BalancePayClient;
import io.github.atengk.pattern.factorymethod.client.PayClient;
import io.github.atengk.pattern.factorymethod.client.WechatPayClient;
import io.github.atengk.pattern.factorymethod.factory.PayClientFactory;
import io.github.atengk.pattern.factorymethod.registry.PayClientFactoryRegistry;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 支付客户端工厂注册表测试
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@SpringBootTest
class PayClientFactoryRegistryTest {

    private final PayClientFactoryRegistry payClientFactoryRegistry;

    PayClientFactoryRegistryTest(PayClientFactoryRegistry payClientFactoryRegistry) {
        this.payClientFactoryRegistry = payClientFactoryRegistry;
    }

    /**
     * 验证不同支付渠道能够创建不同支付客户端
     */
    @Test
    void shouldCreateDifferentPayClientByChannel() {
        PayClientFactory alipayFactory = payClientFactoryRegistry.getFactory("ALIPAY");
        PayClientFactory wechatFactory = payClientFactoryRegistry.getFactory("WECHAT");
        PayClientFactory balanceFactory = payClientFactoryRegistry.getFactory("BALANCE");

        PayClient alipayClient = alipayFactory.createClient();
        PayClient wechatClient = wechatFactory.createClient();
        PayClient balanceClient = balanceFactory.createClient();

        log.info("支付宝客户端：{}", alipayClient.getClass().getSimpleName());
        log.info("微信支付客户端：{}", wechatClient.getClass().getSimpleName());
        log.info("余额支付客户端：{}", balanceClient.getClass().getSimpleName());

        Assertions.assertInstanceOf(AlipayPayClient.class, alipayClient);
        Assertions.assertInstanceOf(WechatPayClient.class, wechatClient);
        Assertions.assertInstanceOf(BalancePayClient.class, balanceClient);
    }

    /**
     * 验证不支持的支付渠道会抛出异常
     */
    @Test
    void shouldThrowExceptionWhenChannelUnsupported() {
        IllegalArgumentException exception = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> payClientFactoryRegistry.getFactory("UNKNOWN")
        );

        log.info("不支持支付渠道异常信息：{}", exception.getMessage());
        Assertions.assertTrue(exception.getMessage().contains("不支持的支付渠道"));
    }

}
```

执行测试：

```bash
mvn test -Dtest=PayClientFactoryRegistryTest
```

命令说明：`-Dtest=PayClientFactoryRegistryTest` 表示只运行当前测试类，用于快速验证工厂方法模式下的工厂注册和客户端创建逻辑。

## 和简单工厂模式的区别

简单工厂模式通常是一个工厂类负责创建多种对象。调用方传入类型，工厂内部通过 `if else`、`switch` 或 Map 选择对象。它适合对象类型少、创建逻辑简单的场景。

工厂方法模式是多个工厂类分别负责创建自己的产品。每个产品有一个对应的工厂实现，创建逻辑分散在具体工厂中。它适合对象创建逻辑复杂、不同产品初始化差异明显、希望新增产品时减少修改已有工厂代码的场景。

在 Spring Boot 项目中，可以这样选择：

```text
对象只是按类型分发，创建逻辑很简单：优先简单工厂模式
每种对象创建过程不同，配置校验不同，初始化流程不同：优先工厂方法模式
需要创建一组相关对象：考虑抽象工厂模式
只是根据类型执行业务算法：更接近策略模式
```

## 开发建议

在 Spring Boot 项目中使用工厂方法模式时，建议遵循以下原则：

```text
产品定义统一接口，业务层只面向接口编程
工厂定义统一接口，每个具体工厂只创建一种产品
复杂初始化逻辑放到具体工厂中，不要堆在业务 Service 中
需要依赖配置或外部组件时，让工厂类交给 Spring 容器管理
工厂注册和路由可以用 Map 承接，避免 Service 中出现大量 if else
产品对象如果无状态，可以考虑由工厂返回单例 Bean
产品对象如果有请求级状态，应每次通过工厂创建新对象
```

需要注意的是，工厂方法模式会增加类数量。如果对象创建逻辑很简单，只有两三个 `new`，没有复杂配置、校验、缓存、初始化流程，就不一定需要使用工厂方法模式。

## 总结

工厂方法模式用于把具体对象的创建过程交给具体工厂类处理。它比简单工厂更强调扩展性和创建职责拆分，适合多种产品创建逻辑差异较大的业务场景。

本示例中的核心流程可以概括为：

```text
定义支付客户端产品接口 PayClient
为不同支付渠道创建具体客户端
定义支付客户端工厂接口 PayClientFactory
为不同支付渠道创建具体工厂
Spring 启动时注册所有工厂
业务层根据支付渠道获取对应工厂
通过工厂方法 createClient 创建客户端
调用客户端完成支付下单
```

在真实 Spring Boot 项目中，工厂方法模式常用于支付、存储、消息、文件解析、第三方平台客户端等模块。它的价值在于把“怎么创建对象”从业务流程中拆出来，让业务层只关注“使用对象完成业务”。
