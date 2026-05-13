# 外观模式 + 适配器模式

外观模式和适配器模式组合使用时，通常用于处理 **对外提供统一业务入口，但内部需要兼容多个接口不一致的子系统或第三方服务** 的场景。

外观模式负责把复杂调用流程封装成一个简单入口，调用方只需要面向这个入口编程；适配器模式负责把不同第三方接口转换成系统内部统一接口。这个组合在 Spring Boot 后端项目中非常常见，例如第三方支付、短信发送、对象存储、物流查询、电子签章、地图服务、AI 平台调用等场景。

```text
外观模式：对业务层提供统一入口，隐藏复杂调用流程
适配器模式：把不同第三方接口适配成系统内部统一接口
```

## 适用场景

本示例以“第三方支付”为业务场景。系统需要支持支付宝、微信支付、银联支付，但它们的接口参数、调用方式、响应结构都不一样。

| 支付渠道      | 第三方接口差异             | 系统内部期望                      |
| --------- | ------------------- | --------------------------- |
| ALIPAY    | 使用 appId、金额字符串、订单标题 | 统一调用 `PaymentAdapter.pay()` |
| WECHAT    | 使用参数对象、商户号、订单描述     | 统一调用 `PaymentAdapter.pay()` |
| UNION_PAY | 使用银联订单对象、交易类型、通知地址  | 统一调用 `PaymentAdapter.pay()` |

如果业务 Service 直接调用各个第三方 SDK，代码通常会变成：

```text
Service 判断支付渠道
  -> 拼接支付宝参数
  -> 调用支付宝 SDK
  -> 解析支付宝响应
  -> 拼接微信支付参数
  -> 调用微信支付 SDK
  -> 解析微信响应
  -> 拼接银联参数
  -> 调用银联接口
  -> 解析银联响应
```

这种写法会导致业务层直接依赖第三方接口细节。第三方接口越多，Service 越复杂。

使用“外观模式 + 适配器模式”后，结构变成：

```text
Controller
  -> Service
      -> PaymentFacade：统一支付外观入口
          -> PaymentAdapterRegistry：获取支付渠道适配器
              -> AlipayPaymentAdapter
              -> WechatPaymentAdapter
              -> UnionPayPaymentAdapter
                  -> 各自调用不同第三方客户端
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

    <!-- Hutool：提供字符串、对象、集合、数字、ID 等常用工具 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.8.36</version>
    </dependency>

    <!-- Lombok：减少 Getter、Setter、构造器、日志等样板代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

建议目录结构如下：

```text
src/main/java/io/github/atengk/pattern/combination/facadeadapter
├── adapter
│   ├── PaymentAdapter.java
│   └── impl
│       ├── AlipayPaymentAdapter.java
│       ├── UnionPayPaymentAdapter.java
│       └── WechatPaymentAdapter.java
├── client
│   ├── AlipayClient.java
│   ├── UnionPayClient.java
│   ├── WechatPayClient.java
│   └── dto
│       ├── ExternalPaymentResult.java
│       ├── UnionPayParam.java
│       └── WechatPayParam.java
├── controller
│   └── PaymentController.java
├── dto
│   ├── PaymentRequest.java
│   └── PaymentResponse.java
├── enums
│   └── PayChannelEnum.java
├── facade
│   └── PaymentFacade.java
├── handler
│   └── GlobalExceptionHandler.java
├── registry
│   └── PaymentAdapterRegistry.java
├── result
│   └── Result.java
└── service
    ├── PaymentService.java
    └── impl
        └── PaymentServiceImpl.java
```

## 核心代码

这一部分给出核心实现。重点看两个位置：

```text
PaymentFacade：外观模式，对业务层提供统一支付入口
PaymentAdapter：适配器模式，把不同第三方支付接口统一成系统内部接口
```

业务层只调用 `PaymentFacade.pay()`，不直接关心支付宝、微信支付、银联支付的参数差异和响应差异。

### 支付渠道枚举

支付渠道枚举用于统一维护支付渠道编码。

文件位置：`src/main/java/io/github/atengk/pattern/combination/facadeadapter/enums/PayChannelEnum.java`

```java
package io.github.atengk.pattern.combination.facadeadapter.enums;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 支付渠道枚举
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Getter
@RequiredArgsConstructor
public enum PayChannelEnum {

    ALIPAY("ALIPAY", "支付宝支付"),

    WECHAT("WECHAT", "微信支付"),

    UNION_PAY("UNION_PAY", "银联支付");

    private final String code;

    private final String description;

    /**
     * 根据编码获取支付渠道
     *
     * @param code 支付渠道编码
     * @return 支付渠道枚举
     */
    public static PayChannelEnum of(String code) {
        String actualCode = StrUtil.trimToEmpty(code);
        for (PayChannelEnum item : values()) {
            if (StrUtil.equalsIgnoreCase(item.getCode(), actualCode)) {
                return item;
            }
        }
        throw new IllegalArgumentException("不支持的支付渠道：" + code);
    }
}
```

### 支付请求对象

支付请求对象用于接收系统内部统一的支付参数。外部调用方不需要关心不同第三方支付接口的参数格式。

文件位置：`src/main/java/io/github/atengk/pattern/combination/facadeadapter/dto/PaymentRequest.java`

```java
package io.github.atengk.pattern.combination.facadeadapter.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 支付请求
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
public class PaymentRequest {

    /**
     * 商户订单号
     */
    @NotBlank(message = "商户订单号不能为空")
    private String orderNo;

    /**
     * 用户ID
     */
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /**
     * 支付渠道：ALIPAY、WECHAT、UNION_PAY
     */
    @NotBlank(message = "支付渠道不能为空")
    private String payChannel;

    /**
     * 支付金额
     */
    @NotNull(message = "支付金额不能为空")
    @DecimalMin(value = "0.01", message = "支付金额必须大于0")
    private BigDecimal amount;

    /**
     * 订单标题
     */
    @NotBlank(message = "订单标题不能为空")
    private String subject;

    /**
     * 支付完成后的通知地址
     */
    private String notifyUrl;
}
```

### 支付响应对象

支付响应对象用于返回系统内部统一的支付结果。

文件位置：`src/main/java/io/github/atengk/pattern/combination/facadeadapter/dto/PaymentResponse.java`

```java
package io.github.atengk.pattern.combination.facadeadapter.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 支付响应
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
@Builder
public class PaymentResponse {

    /**
     * 商户订单号
     */
    private String orderNo;

    /**
     * 支付渠道
     */
    private String payChannel;

    /**
     * 第三方交易号
     */
    private String thirdTradeNo;

    /**
     * 支付跳转地址
     */
    private String payUrl;

    /**
     * 是否成功
     */
    private Boolean success;

    /**
     * 支付状态
     */
    private String status;

    /**
     * 响应消息
     */
    private String message;
}
```

### 第三方统一返回对象

第三方统一返回对象用于在模拟第三方客户端和适配器之间传递结果。真实项目中这里通常是第三方 SDK 的原始响应对象。

文件位置：`src/main/java/io/github/atengk/pattern/combination/facadeadapter/client/dto/ExternalPaymentResult.java`

```java
package io.github.atengk.pattern.combination.facadeadapter.client.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 第三方支付返回结果
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
@Builder
public class ExternalPaymentResult {

    /**
     * 是否成功
     */
    private Boolean success;

    /**
     * 第三方交易号
     */
    private String thirdTradeNo;

    /**
     * 支付跳转地址
     */
    private String payUrl;

    /**
     * 第三方响应消息
     */
    private String message;
}
```

### 微信支付参数对象

微信支付模拟接口使用独立参数对象，体现第三方接口差异。

文件位置：`src/main/java/io/github/atengk/pattern/combination/facadeadapter/client/dto/WechatPayParam.java`

```java
package io.github.atengk.pattern.combination.facadeadapter.client.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 微信支付参数
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
@Builder
public class WechatPayParam {

    /**
     * 商户号
     */
    private String merchantId;

    /**
     * 商户订单号
     */
    private String outTradeNo;

    /**
     * 支付金额，单位：分
     */
    private Integer amountCent;

    /**
     * 订单描述
     */
    private String description;

    /**
     * 支付通知地址
     */
    private String notifyUrl;
}
```

### 银联支付参数对象

银联支付模拟接口使用自己的订单参数结构。

文件位置：`src/main/java/io/github/atengk/pattern/combination/facadeadapter/client/dto/UnionPayParam.java`

```java
package io.github.atengk.pattern.combination.facadeadapter.client.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 银联支付参数
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
@Builder
public class UnionPayParam {

    /**
     * 银联商户号
     */
    private String unionMerchantNo;

    /**
     * 交易类型
     */
    private String tradeType;

    /**
     * 商户订单号
     */
    private String orderNo;

    /**
     * 支付金额，单位：元
     */
    private String amount;

    /**
     * 后台通知地址
     */
    private String backNotifyUrl;
}
```

### 支付宝客户端

支付宝客户端用于模拟支付宝 SDK。它的接口使用 `appId`、金额字符串和订单标题。

文件位置：`src/main/java/io/github/atengk/pattern/combination/facadeadapter/client/AlipayClient.java`

```java
package io.github.atengk.pattern.combination.facadeadapter.client;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.pattern.combination.facadeadapter.client.dto.ExternalPaymentResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 支付宝客户端
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class AlipayClient {

    /**
     * 创建支付宝交易
     *
     * @param appId      支付宝应用ID
     * @param outTradeNo 商户订单号
     * @param amount     支付金额
     * @param subject    订单标题
     * @return 第三方支付返回结果
     */
    public ExternalPaymentResult createTrade(String appId, String outTradeNo, String amount, String subject) {
        log.info("调用支付宝支付接口，appId：{}，订单号：{}，金额：{}，标题：{}",
                appId, outTradeNo, amount, subject);

        return ExternalPaymentResult.builder()
                .success(true)
                .thirdTradeNo("ALI" + IdUtil.fastSimpleUUID())
                .payUrl(StrUtil.format("https://pay.example.com/alipay/{}", outTradeNo))
                .message("支付宝预下单成功")
                .build();
    }
}
```

### 微信支付客户端

微信支付客户端用于模拟微信支付 SDK。它的接口使用参数对象。

文件位置：`src/main/java/io/github/atengk/pattern/combination/facadeadapter/client/WechatPayClient.java`

```java
package io.github.atengk.pattern.combination.facadeadapter.client;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.pattern.combination.facadeadapter.client.dto.ExternalPaymentResult;
import io.github.atengk.pattern.combination.facadeadapter.client.dto.WechatPayParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 微信支付客户端
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class WechatPayClient {

    /**
     * 创建微信支付预支付订单
     *
     * @param param 微信支付参数
     * @return 第三方支付返回结果
     */
    public ExternalPaymentResult prepay(WechatPayParam param) {
        log.info("调用微信支付接口，商户号：{}，订单号：{}，金额分：{}，描述：{}",
                param.getMerchantId(), param.getOutTradeNo(), param.getAmountCent(), param.getDescription());

        return ExternalPaymentResult.builder()
                .success(true)
                .thirdTradeNo("WX" + IdUtil.fastSimpleUUID())
                .payUrl(StrUtil.format("https://pay.example.com/wechat/{}", param.getOutTradeNo()))
                .message("微信支付预下单成功")
                .build();
    }
}
```

### 银联支付客户端

银联支付客户端用于模拟银联接口。它的接口使用银联自己的参数对象。

文件位置：`src/main/java/io/github/atengk/pattern/combination/facadeadapter/client/UnionPayClient.java`

```java
package io.github.atengk.pattern.combination.facadeadapter.client;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.pattern.combination.facadeadapter.client.dto.ExternalPaymentResult;
import io.github.atengk.pattern.combination.facadeadapter.client.dto.UnionPayParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 银联支付客户端
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class UnionPayClient {

    /**
     * 提交银联支付订单
     *
     * @param param 银联支付参数
     * @return 第三方支付返回结果
     */
    public ExternalPaymentResult submitPay(UnionPayParam param) {
        log.info("调用银联支付接口，商户号：{}，订单号：{}，交易类型：{}，金额：{}",
                param.getUnionMerchantNo(), param.getOrderNo(), param.getTradeType(), param.getAmount());

        return ExternalPaymentResult.builder()
                .success(true)
                .thirdTradeNo("UP" + IdUtil.fastSimpleUUID())
                .payUrl(StrUtil.format("https://pay.example.com/union/{}", param.getOrderNo()))
                .message("银联支付下单成功")
                .build();
    }
}
```

### 支付适配器接口

支付适配器接口定义系统内部统一的支付调用方式。不同第三方支付渠道都要适配成这个接口。

文件位置：`src/main/java/io/github/atengk/pattern/combination/facadeadapter/adapter/PaymentAdapter.java`

```java
package io.github.atengk.pattern.combination.facadeadapter.adapter;

import io.github.atengk.pattern.combination.facadeadapter.dto.PaymentRequest;
import io.github.atengk.pattern.combination.facadeadapter.dto.PaymentResponse;
import io.github.atengk.pattern.combination.facadeadapter.enums.PayChannelEnum;

/**
 * 支付适配器接口
 *
 * @author Ateng
 * @since 2026-05-13
 */
public interface PaymentAdapter {

    /**
     * 当前适配器支持的支付渠道
     *
     * @return 支付渠道
     */
    PayChannelEnum payChannel();

    /**
     * 发起支付
     *
     * @param request 支付请求
     * @return 支付响应
     */
    PaymentResponse pay(PaymentRequest request);
}
```

### 支付宝支付适配器

支付宝适配器负责把系统内部统一请求转换成支付宝接口需要的参数，并把支付宝响应转换成系统统一响应。

文件位置：`src/main/java/io/github/atengk/pattern/combination/facadeadapter/adapter/impl/AlipayPaymentAdapter.java`

```java
package io.github.atengk.pattern.combination.facadeadapter.adapter.impl;

import cn.hutool.core.util.NumberUtil;
import io.github.atengk.pattern.combination.facadeadapter.adapter.PaymentAdapter;
import io.github.atengk.pattern.combination.facadeadapter.client.AlipayClient;
import io.github.atengk.pattern.combination.facadeadapter.client.dto.ExternalPaymentResult;
import io.github.atengk.pattern.combination.facadeadapter.dto.PaymentRequest;
import io.github.atengk.pattern.combination.facadeadapter.dto.PaymentResponse;
import io.github.atengk.pattern.combination.facadeadapter.enums.PayChannelEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 支付宝支付适配器
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlipayPaymentAdapter implements PaymentAdapter {

    private static final String APP_ID = "alipay-app-10001";

    private final AlipayClient alipayClient;

    /**
     * 当前适配器支持的支付渠道
     *
     * @return 支付渠道
     */
    @Override
    public PayChannelEnum payChannel() {
        return PayChannelEnum.ALIPAY;
    }

    /**
     * 发起支付宝支付
     *
     * @param request 支付请求
     * @return 支付响应
     */
    @Override
    public PaymentResponse pay(PaymentRequest request) {
        String amountText = NumberUtil.round(request.getAmount(), 2).toPlainString();

        ExternalPaymentResult result = alipayClient.createTrade(
                APP_ID,
                request.getOrderNo(),
                amountText,
                request.getSubject()
        );

        log.info("支付宝支付适配完成，订单号：{}，第三方交易号：{}",
                request.getOrderNo(), result.getThirdTradeNo());

        return PaymentResponse.builder()
                .orderNo(request.getOrderNo())
                .payChannel(payChannel().getCode())
                .thirdTradeNo(result.getThirdTradeNo())
                .payUrl(result.getPayUrl())
                .success(result.getSuccess())
                .status(Boolean.TRUE.equals(result.getSuccess()) ? "PAYING" : "FAILED")
                .message(result.getMessage())
                .build();
    }
}
```

### 微信支付适配器

微信支付适配器负责把系统请求转换成微信支付参数对象，并转换响应结果。

文件位置：`src/main/java/io/github/atengk/pattern/combination/facadeadapter/adapter/impl/WechatPaymentAdapter.java`

```java
package io.github.atengk.pattern.combination.facadeadapter.adapter.impl;

import cn.hutool.core.util.NumberUtil;
import io.github.atengk.pattern.combination.facadeadapter.adapter.PaymentAdapter;
import io.github.atengk.pattern.combination.facadeadapter.client.WechatPayClient;
import io.github.atengk.pattern.combination.facadeadapter.client.dto.ExternalPaymentResult;
import io.github.atengk.pattern.combination.facadeadapter.client.dto.WechatPayParam;
import io.github.atengk.pattern.combination.facadeadapter.dto.PaymentRequest;
import io.github.atengk.pattern.combination.facadeadapter.dto.PaymentResponse;
import io.github.atengk.pattern.combination.facadeadapter.enums.PayChannelEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 微信支付适配器
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WechatPaymentAdapter implements PaymentAdapter {

    private static final String MERCHANT_ID = "wechat-merchant-20001";

    private final WechatPayClient wechatPayClient;

    /**
     * 当前适配器支持的支付渠道
     *
     * @return 支付渠道
     */
    @Override
    public PayChannelEnum payChannel() {
        return PayChannelEnum.WECHAT;
    }

    /**
     * 发起微信支付
     *
     * @param request 支付请求
     * @return 支付响应
     */
    @Override
    public PaymentResponse pay(PaymentRequest request) {
        Integer amountCent = NumberUtil.mul(request.getAmount(), new BigDecimal("100")).intValue();

        WechatPayParam param = WechatPayParam.builder()
                .merchantId(MERCHANT_ID)
                .outTradeNo(request.getOrderNo())
                .amountCent(amountCent)
                .description(request.getSubject())
                .notifyUrl(request.getNotifyUrl())
                .build();

        ExternalPaymentResult result = wechatPayClient.prepay(param);

        log.info("微信支付适配完成，订单号：{}，第三方交易号：{}",
                request.getOrderNo(), result.getThirdTradeNo());

        return PaymentResponse.builder()
                .orderNo(request.getOrderNo())
                .payChannel(payChannel().getCode())
                .thirdTradeNo(result.getThirdTradeNo())
                .payUrl(result.getPayUrl())
                .success(result.getSuccess())
                .status(Boolean.TRUE.equals(result.getSuccess()) ? "PAYING" : "FAILED")
                .message(result.getMessage())
                .build();
    }
}
```

### 银联支付适配器

银联支付适配器负责把系统请求转换成银联支付参数对象，并转换响应结果。

文件位置：`src/main/java/io/github/atengk/pattern/combination/facadeadapter/adapter/impl/UnionPayPaymentAdapter.java`

```java
package io.github.atengk.pattern.combination.facadeadapter.adapter.impl;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.pattern.combination.facadeadapter.adapter.PaymentAdapter;
import io.github.atengk.pattern.combination.facadeadapter.client.UnionPayClient;
import io.github.atengk.pattern.combination.facadeadapter.client.dto.ExternalPaymentResult;
import io.github.atengk.pattern.combination.facadeadapter.client.dto.UnionPayParam;
import io.github.atengk.pattern.combination.facadeadapter.dto.PaymentRequest;
import io.github.atengk.pattern.combination.facadeadapter.dto.PaymentResponse;
import io.github.atengk.pattern.combination.facadeadapter.enums.PayChannelEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 银联支付适配器
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UnionPayPaymentAdapter implements PaymentAdapter {

    private static final String UNION_MERCHANT_NO = "union-merchant-30001";

    private final UnionPayClient unionPayClient;

    /**
     * 当前适配器支持的支付渠道
     *
     * @return 支付渠道
     */
    @Override
    public PayChannelEnum payChannel() {
        return PayChannelEnum.UNION_PAY;
    }

    /**
     * 发起银联支付
     *
     * @param request 支付请求
     * @return 支付响应
     */
    @Override
    public PaymentResponse pay(PaymentRequest request) {
        String amountText = NumberUtil.round(request.getAmount(), 2).toPlainString();
        String notifyUrl = StrUtil.blankToDefault(request.getNotifyUrl(), "https://api.example.com/pay/notify/union");

        UnionPayParam param = UnionPayParam.builder()
                .unionMerchantNo(UNION_MERCHANT_NO)
                .tradeType("WEB_PAY")
                .orderNo(request.getOrderNo())
                .amount(amountText)
                .backNotifyUrl(notifyUrl)
                .build();

        ExternalPaymentResult result = unionPayClient.submitPay(param);

        log.info("银联支付适配完成，订单号：{}，第三方交易号：{}",
                request.getOrderNo(), result.getThirdTradeNo());

        return PaymentResponse.builder()
                .orderNo(request.getOrderNo())
                .payChannel(payChannel().getCode())
                .thirdTradeNo(result.getThirdTradeNo())
                .payUrl(result.getPayUrl())
                .success(result.getSuccess())
                .status(Boolean.TRUE.equals(result.getSuccess()) ? "PAYING" : "FAILED")
                .message(result.getMessage())
                .build();
    }
}
```

### 支付适配器注册器

适配器注册器负责收集所有支付适配器，并根据支付渠道找到对应适配器。它不处理第三方接口差异，只负责适配器定位。

文件位置：`src/main/java/io/github/atengk/pattern/combination/facadeadapter/registry/PaymentAdapterRegistry.java`

```java
package io.github.atengk.pattern.combination.facadeadapter.registry;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.pattern.combination.facadeadapter.adapter.PaymentAdapter;
import io.github.atengk.pattern.combination.facadeadapter.enums.PayChannelEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 支付适配器注册器
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class PaymentAdapterRegistry {

    private final Map<PayChannelEnum, PaymentAdapter> adapterMap;

    /**
     * 初始化支付适配器注册器
     *
     * @param adapterList 支付适配器集合
     */
    public PaymentAdapterRegistry(List<PaymentAdapter> adapterList) {
        if (CollUtil.isEmpty(adapterList)) {
            throw new IllegalStateException("支付适配器不能为空");
        }

        Map<PayChannelEnum, PaymentAdapter> tempAdapterMap = new EnumMap<>(PayChannelEnum.class);
        for (PaymentAdapter adapter : adapterList) {
            PaymentAdapter oldAdapter = tempAdapterMap.put(adapter.payChannel(), adapter);
            if (ObjectUtil.isNotNull(oldAdapter)) {
                throw new IllegalStateException("支付适配器重复注册：" + adapter.payChannel().getCode());
            }
        }

        this.adapterMap = Collections.unmodifiableMap(tempAdapterMap);
        log.info("支付适配器注册器初始化完成，适配器数量：{}", this.adapterMap.size());
    }

    /**
     * 根据支付渠道获取适配器
     *
     * @param payChannelCode 支付渠道编码
     * @return 支付适配器
     */
    public PaymentAdapter getAdapter(String payChannelCode) {
        PayChannelEnum payChannel = PayChannelEnum.of(payChannelCode);
        PaymentAdapter adapter = adapterMap.get(payChannel);
        if (ObjectUtil.isNull(adapter)) {
            throw new IllegalArgumentException("未找到支付适配器：" + payChannelCode);
        }

        log.info("命中支付适配器，支付渠道：{}，适配器类：{}",
                payChannel.getCode(), adapter.getClass().getSimpleName());
        return adapter;
    }
}
```

### 支付外观类

支付外观类是外观模式的核心。业务层只需要调用 `PaymentFacade.pay()`，不用直接关心适配器选择、支付前检查、第三方调用和支付后处理。

文件位置：`src/main/java/io/github/atengk/pattern/combination/facadeadapter/facade/PaymentFacade.java`

```java
package io.github.atengk.pattern.combination.facadeadapter.facade;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.pattern.combination.facadeadapter.adapter.PaymentAdapter;
import io.github.atengk.pattern.combination.facadeadapter.dto.PaymentRequest;
import io.github.atengk.pattern.combination.facadeadapter.dto.PaymentResponse;
import io.github.atengk.pattern.combination.facadeadapter.registry.PaymentAdapterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 支付外观
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentFacade {

    private final PaymentAdapterRegistry paymentAdapterRegistry;

    /**
     * 发起支付
     *
     * @param request 支付请求
     * @return 支付响应
     */
    public PaymentResponse pay(PaymentRequest request) {
        beforePay(request);

        PaymentAdapter adapter = paymentAdapterRegistry.getAdapter(request.getPayChannel());

        PaymentResponse response = adapter.pay(request);

        afterPay(request, response);

        return response;
    }

    /**
     * 支付前处理
     *
     * @param request 支付请求
     */
    private void beforePay(PaymentRequest request) {
        if (StrUtil.isBlank(request.getOrderNo())) {
            throw new IllegalArgumentException("商户订单号不能为空");
        }
        if (ObjectUtil.isNull(request.getUserId())) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        if (StrUtil.isBlank(request.getPayChannel())) {
            throw new IllegalArgumentException("支付渠道不能为空");
        }
        if (ObjectUtil.isNull(request.getAmount()) || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("支付金额必须大于0");
        }

        log.info("开始发起支付，订单号：{}，用户ID：{}，支付渠道：{}，金额：{}",
                request.getOrderNo(), request.getUserId(), request.getPayChannel(), request.getAmount());
    }

    /**
     * 支付后处理
     *
     * @param request  支付请求
     * @param response 支付响应
     */
    private void afterPay(PaymentRequest request, PaymentResponse response) {
        log.info("支付调用完成，订单号：{}，支付渠道：{}，状态：{}，第三方交易号：{}",
                request.getOrderNo(), response.getPayChannel(), response.getStatus(), response.getThirdTradeNo());
    }
}
```

### Service 接口

Service 接口对外暴露支付能力。

文件位置：`src/main/java/io/github/atengk/pattern/combination/facadeadapter/service/PaymentService.java`

```java
package io.github.atengk.pattern.combination.facadeadapter.service;

import io.github.atengk.pattern.combination.facadeadapter.dto.PaymentRequest;
import io.github.atengk.pattern.combination.facadeadapter.dto.PaymentResponse;

/**
 * 支付服务接口
 *
 * @author Ateng
 * @since 2026-05-13
 */
public interface PaymentService {

    /**
     * 发起支付
     *
     * @param request 支付请求
     * @return 支付响应
     */
    PaymentResponse pay(PaymentRequest request);
}
```

### Service 实现类

Service 实现类只调用支付外观，不直接调用具体第三方支付客户端。

文件位置：`src/main/java/io/github/atengk/pattern/combination/facadeadapter/service/impl/PaymentServiceImpl.java`

```java
package io.github.atengk.pattern.combination.facadeadapter.service.impl;

import io.github.atengk.pattern.combination.facadeadapter.dto.PaymentRequest;
import io.github.atengk.pattern.combination.facadeadapter.dto.PaymentResponse;
import io.github.atengk.pattern.combination.facadeadapter.facade.PaymentFacade;
import io.github.atengk.pattern.combination.facadeadapter.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 支付服务实现类
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentFacade paymentFacade;

    /**
     * 发起支付
     *
     * @param request 支付请求
     * @return 支付响应
     */
    @Override
    public PaymentResponse pay(PaymentRequest request) {
        PaymentResponse response = paymentFacade.pay(request);
        log.info("支付服务处理完成，订单号：{}，支付渠道：{}", response.getOrderNo(), response.getPayChannel());
        return response;
    }
}
```

### 统一响应对象

统一响应对象用于包装接口返回结果。

文件位置：`src/main/java/io/github/atengk/pattern/combination/facadeadapter/result/Result.java`

```java
package io.github.atengk.pattern.combination.facadeadapter.result;

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

文件位置：`src/main/java/io/github/atengk/pattern/combination/facadeadapter/handler/GlobalExceptionHandler.java`

```java
package io.github.atengk.pattern.combination.facadeadapter.handler;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.pattern.combination.facadeadapter.result.Result;
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
        log.warn("支付业务异常：{}", exception.getMessage());
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

Controller 提供统一支付接口，调用方只需要传入支付渠道，不需要关心内部适配细节。

文件位置：`src/main/java/io/github/atengk/pattern/combination/facadeadapter/controller/PaymentController.java`

```java
package io.github.atengk.pattern.combination.facadeadapter.controller;

import io.github.atengk.pattern.combination.facadeadapter.dto.PaymentRequest;
import io.github.atengk.pattern.combination.facadeadapter.dto.PaymentResponse;
import io.github.atengk.pattern.combination.facadeadapter.result.Result;
import io.github.atengk.pattern.combination.facadeadapter.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 支付控制器
 *
 * @author Ateng
 * @since 2026-05-13
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * 发起支付
     *
     * @param request 支付请求
     * @return 支付响应
     */
    @PostMapping
    public Result<PaymentResponse> pay(@Valid @RequestBody PaymentRequest request) {
        return Result.success(paymentService.pay(request));
    }
}
```

## 使用方式

启动 Spring Boot 项目后，可以通过统一支付接口验证外观模式和适配器模式是否同时生效。

接口信息如下：

```text
请求地址：POST /api/payments
Content-Type：application/json
```

支付宝支付请求示例：

```bash
curl -X POST "http://localhost:8080/api/payments" \
  -H "Content-Type: application/json" \
  -d '{
    "orderNo": "PAY202605130001",
    "userId": 1001,
    "payChannel": "ALIPAY",
    "amount": 199.99,
    "subject": "订单支付-支付宝",
    "notifyUrl": "https://api.example.com/pay/notify/alipay"
  }'
```

响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "orderNo": "PAY202605130001",
    "payChannel": "ALIPAY",
    "thirdTradeNo": "ALI7b4f4d72f7f74e9f94e31f4b5f1b0b68",
    "payUrl": "https://pay.example.com/alipay/PAY202605130001",
    "success": true,
    "status": "PAYING",
    "message": "支付宝预下单成功"
  }
}
```

微信支付请求示例：

```bash
curl -X POST "http://localhost:8080/api/payments" \
  -H "Content-Type: application/json" \
  -d '{
    "orderNo": "PAY202605130002",
    "userId": 1002,
    "payChannel": "WECHAT",
    "amount": 88.88,
    "subject": "订单支付-微信",
    "notifyUrl": "https://api.example.com/pay/notify/wechat"
  }'
```

响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "orderNo": "PAY202605130002",
    "payChannel": "WECHAT",
    "thirdTradeNo": "WXe0df86c936694b338580957b20d307cb",
    "payUrl": "https://pay.example.com/wechat/PAY202605130002",
    "success": true,
    "status": "PAYING",
    "message": "微信支付预下单成功"
  }
}
```

银联支付请求示例：

```bash
curl -X POST "http://localhost:8080/api/payments" \
  -H "Content-Type: application/json" \
  -d '{
    "orderNo": "PAY202605130003",
    "userId": 1003,
    "payChannel": "UNION_PAY",
    "amount": 299.99,
    "subject": "订单支付-银联",
    "notifyUrl": "https://api.example.com/pay/notify/union"
  }'
```

响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "orderNo": "PAY202605130003",
    "payChannel": "UNION_PAY",
    "thirdTradeNo": "UP4d66f6d9b7f84c49bba2c2d6d7b8396f",
    "payUrl": "https://pay.example.com/union/PAY202605130003",
    "success": true,
    "status": "PAYING",
    "message": "银联支付下单成功"
  }
}
```

不支持的支付渠道请求示例：

```bash
curl -X POST "http://localhost:8080/api/payments" \
  -H "Content-Type: application/json" \
  -d '{
    "orderNo": "PAY202605130004",
    "userId": 1004,
    "payChannel": "PAYPAL",
    "amount": 99.99,
    "subject": "订单支付-PayPal"
  }'
```

响应示例：

```json
{
  "code": 400,
  "message": "不支持的支付渠道：PAYPAL",
  "data": null
}
```

## 新增支付渠道

当新增一种支付渠道时，例如新增 PayPal 支付，不需要修改 `PaymentFacade`，也不需要修改已有支付宝、微信、银联适配器。

第一步，在 `PayChannelEnum` 中新增枚举：

```java
PAYPAL("PAYPAL", "PayPal支付");
```

第二步，新增 PayPal 客户端。真实项目中这里通常是 PayPal SDK 或 HTTP API 封装。

文件位置：`src/main/java/io/github/atengk/pattern/combination/facadeadapter/client/PaypalClient.java`

```java
package io.github.atengk.pattern.combination.facadeadapter.client;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.pattern.combination.facadeadapter.client.dto.ExternalPaymentResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * PayPal 客户端
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class PaypalClient {

    /**
     * 创建 PayPal 支付订单
     *
     * @param clientId 客户端ID
     * @param orderNo  商户订单号
     * @param amount   支付金额
     * @return 第三方支付返回结果
     */
    public ExternalPaymentResult createOrder(String clientId, String orderNo, String amount) {
        log.info("调用PayPal支付接口，clientId：{}，订单号：{}，金额：{}",
                clientId, orderNo, amount);

        return ExternalPaymentResult.builder()
                .success(true)
                .thirdTradeNo("PP" + IdUtil.fastSimpleUUID())
                .payUrl(StrUtil.format("https://pay.example.com/paypal/{}", orderNo))
                .message("PayPal支付订单创建成功")
                .build();
    }
}
```

第三步，新增 PayPal 支付适配器。

文件位置：`src/main/java/io/github/atengk/pattern/combination/facadeadapter/adapter/impl/PaypalPaymentAdapter.java`

```java
package io.github.atengk.pattern.combination.facadeadapter.adapter.impl;

import cn.hutool.core.util.NumberUtil;
import io.github.atengk.pattern.combination.facadeadapter.adapter.PaymentAdapter;
import io.github.atengk.pattern.combination.facadeadapter.client.PaypalClient;
import io.github.atengk.pattern.combination.facadeadapter.client.dto.ExternalPaymentResult;
import io.github.atengk.pattern.combination.facadeadapter.dto.PaymentRequest;
import io.github.atengk.pattern.combination.facadeadapter.dto.PaymentResponse;
import io.github.atengk.pattern.combination.facadeadapter.enums.PayChannelEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * PayPal 支付适配器
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaypalPaymentAdapter implements PaymentAdapter {

    private static final String CLIENT_ID = "paypal-client-40001";

    private final PaypalClient paypalClient;

    /**
     * 当前适配器支持的支付渠道
     *
     * @return 支付渠道
     */
    @Override
    public PayChannelEnum payChannel() {
        return PayChannelEnum.PAYPAL;
    }

    /**
     * 发起 PayPal 支付
     *
     * @param request 支付请求
     * @return 支付响应
     */
    @Override
    public PaymentResponse pay(PaymentRequest request) {
        String amountText = NumberUtil.round(request.getAmount(), 2).toPlainString();
        ExternalPaymentResult result = paypalClient.createOrder(CLIENT_ID, request.getOrderNo(), amountText);

        log.info("PayPal支付适配完成，订单号：{}，第三方交易号：{}",
                request.getOrderNo(), result.getThirdTradeNo());

        return PaymentResponse.builder()
                .orderNo(request.getOrderNo())
                .payChannel(payChannel().getCode())
                .thirdTradeNo(result.getThirdTradeNo())
                .payUrl(result.getPayUrl())
                .success(result.getSuccess())
                .status(Boolean.TRUE.equals(result.getSuccess()) ? "PAYING" : "FAILED")
                .message(result.getMessage())
                .build();
    }
}
```

新增完成后，`PaymentAdapterRegistry` 会自动收集新的 `PaypalPaymentAdapter`。支付外观 `PaymentFacade` 不需要修改。

## 验证方式

可以从以下几个方面验证组合模式是否生效：

```text
1. 启动项目时，日志应输出支付适配器注册器初始化完成
2. 请求 ALIPAY 时，应命中 AlipayPaymentAdapter
3. 请求 WECHAT 时，应命中 WechatPaymentAdapter
4. 请求 UNION_PAY 时，应命中 UnionPayPaymentAdapter
5. Controller 和 Service 不直接依赖任何第三方支付客户端
6. 新增支付渠道时，不需要修改 PaymentFacade
7. 不同第三方接口差异只存在于各自适配器中
```

正常请求日志示例：

```text
支付适配器注册器初始化完成，适配器数量：3
开始发起支付，订单号：PAY202605130001，用户ID：1001，支付渠道：ALIPAY，金额：199.99
命中支付适配器，支付渠道：ALIPAY，适配器类：AlipayPaymentAdapter
调用支付宝支付接口，appId：alipay-app-10001，订单号：PAY202605130001，金额：199.99，标题：订单支付-支付宝
支付宝支付适配完成，订单号：PAY202605130001，第三方交易号：ALI7b4f4d72f7f74e9f94e31f4b5f1b0b68
支付调用完成，订单号：PAY202605130001，支付渠道：ALIPAY，状态：PAYING，第三方交易号：ALI7b4f4d72f7f74e9f94e31f4b5f1b0b68
支付服务处理完成，订单号：PAY202605130001，支付渠道：ALIPAY
```

## 组合效果

外观模式和适配器模式组合后，各自负责不同变化点：

| 模式    | 职责                  | 在示例中的体现                                                                                              |
| ----- | ------------------- | ---------------------------------------------------------------------------------------------------- |
| 外观模式  | 对业务层提供统一入口，隐藏复杂调用流程 | `PaymentFacade` 封装支付前检查、适配器选择、支付调用和支付后处理                                                             |
| 适配器模式 | 屏蔽不同第三方接口差异         | `AlipayPaymentAdapter`、`WechatPaymentAdapter`、`UnionPayPaymentAdapter` 把不同支付接口适配成统一 `PaymentAdapter` |

这种组合适合处理下面两类变化：

```text
第一类变化：外部调用方希望接口简单稳定
例如业务层只想调用统一的 pay() 方法，不想关心第三方 SDK 细节

第二类变化：内部第三方接口差异明显
例如不同支付渠道的参数结构、金额单位、响应字段、调用方法都不同
```

相比只使用外观模式，这个组合可以避免外观类内部堆积大量第三方接口转换逻辑。相比只使用适配器模式，这个组合可以给业务层提供更完整、更稳定的统一入口，而不是让业务层自己选择和调用适配器。

## 注意事项

外观模式和适配器模式组合使用时，要明确两者边界：

```text
外观模式负责统一入口和流程封装
适配器模式负责接口转换和差异屏蔽
```

不要把所有第三方接口转换逻辑都写进外观类。否则 `PaymentFacade` 会逐渐变成包含大量 `if else` 和参数拼接逻辑的复杂类。

也不要让业务层直接调用适配器。适配器解决的是接口兼容问题，不应该暴露太多第三方细节给业务层。业务层应该优先依赖外观类，由外观类统一完成前置校验、适配器选择、调用编排和后置处理。

这个组合最适合的判断标准是：**统一入口归外观，接口差异归适配器**。
