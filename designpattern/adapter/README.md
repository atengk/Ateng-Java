# 适配器模式

适配器模式属于结构型模式，核心作用是把“不兼容的接口”转换成系统内部期望的统一接口。在当前设计模式文档体系中，适配器模式位于结构型模式分类下，适合处理第三方接口适配、老系统接口兼容、SDK 调用封装等 Spring Boot 项目场景。

本文以 **JDK21 + Spring Boot 3** 后端项目为背景，通过“统一支付接口适配支付宝旧接口和微信支付接口”的示例，说明适配器模式在真实项目中的落地方式。

## 基础配置

本示例模拟一个支付模块。业务系统内部只认一个统一的 `PaymentAdapter` 接口，但外部支付渠道的接口形式不一致：支付宝旧接口是普通方法调用，微信支付接口是 Map 参数调用。适配器模式负责把这些差异封装起来，让业务层不直接依赖第三方接口细节。

如果项目已经有统一父工程，只需要补充以下依赖。Spring Boot 版本建议由父工程统一管理，Hutool 版本也建议放到公司统一依赖管理中。

```xml
<dependencies>
    <!-- Spring Web：提供 REST API 能力 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- 参数校验：用于校验支付请求参数 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- Hutool：常用工具类，减少重复工具代码 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>${hutool.version}</version>
    </dependency>

    <!-- Lombok：减少 DTO、VO、构造器样板代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- 配置元数据提示：让 IDE 能识别 @ConfigurationProperties -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-configuration-processor</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- 测试依赖：用于单元测试和接口测试 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

支付渠道配置放在 `application.yml` 中，真实项目中应替换为配置中心、环境变量或密钥管理系统。

```yaml
server:
  port: 8080 # 示例服务端口

pay:
  alipay:
    partner-id: demo-alipay-partner # 模拟支付宝合作方 ID
  wechat:
    mch-id: demo-wechat-mch # 模拟微信商户号
```

本示例的核心文件结构如下。

```text
src/main/java/io/github/atengk/designpattern/adapter
├── AdapterApplication.java
├── config
│   └── PayProperties.java
├── controller
│   └── PaymentController.java
├── dto
│   └── PayCreateRequest.java
├── enums
│   └── PayChannel.java
├── external
│   ├── AlipayLegacyClient.java
│   ├── AlipayTradeResult.java
│   └── WechatPayApiClient.java
├── adapter
│   ├── PaymentAdapter.java
│   ├── AlipayPaymentAdapter.java
│   ├── WechatPaymentAdapter.java
│   └── PaymentAdapterRegistry.java
├── service
│   ├── PaymentService.java
│   └── PaymentServiceImpl.java
├── vo
│   ├── ApiResult.java
│   └── PayResultVO.java
└── web
    └── GlobalExceptionHandler.java
```

## 模式设计

适配器模式在本示例中的角色分工比较清晰：系统内部定义统一目标接口，第三方接口作为被适配对象，每个渠道提供一个适配器类，把第三方返回值转换成系统内部统一返回值。

| 角色       | 示例类                                         | 说明                                     |
| ---------- | ---------------------------------------------- | ---------------------------------------- |
| 目标接口   | `PaymentAdapter`                               | 系统内部希望调用的统一支付接口           |
| 被适配对象 | `AlipayLegacyClient`、`WechatPayApiClient`     | 外部系统、旧接口或第三方 SDK             |
| 适配器     | `AlipayPaymentAdapter`、`WechatPaymentAdapter` | 把第三方接口转换成统一接口               |
| 调用方     | `PaymentServiceImpl`                           | 业务服务，只依赖统一接口，不关心渠道差异 |
| 注册器     | `PaymentAdapterRegistry`                       | 根据支付渠道选择对应适配器               |

核心调用流程如下。

```text
Controller
    ↓
PaymentService
    ↓
PaymentAdapterRegistry 根据 channel 获取适配器
    ↓
PaymentAdapter.createPay()
    ↓
AlipayLegacyClient / WechatPayApiClient
    ↓
统一返回 PayResultVO
```

## 核心代码

下面给出适配器模式在 Spring Boot 项目中的关键实现。非核心的启动脚本、构建脚本和测试类可按现有项目规范补充。

项目启动类开启配置属性扫描，保证 `PayProperties` 可以从 `application.yml` 读取配置。

文件位置：`src/main/java/io/github/atengk/designpattern/adapter/AdapterApplication.java`

```java
package io.github.atengk.designpattern.adapter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * 适配器模式示例应用启动类
 *
 * @author Ateng
 * @since 2026-05-13
 */
@ConfigurationPropertiesScan
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

支付配置类用于承接不同渠道的基础配置，避免适配器中写死商户信息。

文件位置：`src/main/java/io/github/atengk/designpattern/adapter/config/PayProperties.java`

```java
package io.github.atengk.designpattern.adapter.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 支付渠道配置
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
@ConfigurationProperties(prefix = "pay")
public class PayProperties {

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
         * 合作方 ID
         */
        private String partnerId;
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
        private String mchId;
    }
}
```

支付渠道枚举提供字符串到枚举的转换能力，避免业务层直接散落字符串判断。

文件位置：`src/main/java/io/github/atengk/designpattern/adapter/enums/PayChannel.java`

```java
package io.github.atengk.designpattern.adapter.enums;

import cn.hutool.core.util.StrUtil;

import java.util.Arrays;

/**
 * 支付渠道枚举
 *
 * @author Ateng
 * @since 2026-05-13
 */
public enum PayChannel {

    /**
     * 支付宝
     */
    ALIPAY,

    /**
     * 微信支付
     */
    WECHAT;

    /**
     * 根据渠道编码解析支付渠道
     *
     * @param channel 渠道编码
     * @return 支付渠道
     */
    public static PayChannel parse(String channel) {
        if (StrUtil.isBlank(channel)) {
            throw new IllegalArgumentException("支付渠道不能为空");
        }

        return Arrays.stream(values())
                .filter(item -> StrUtil.equalsIgnoreCase(item.name(), channel))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(StrUtil.format("不支持的支付渠道：{}", channel)));
    }
}
```

支付请求 DTO 表示系统内部的统一支付请求，不暴露第三方渠道的参数差异。

文件位置：`src/main/java/io/github/atengk/designpattern/adapter/dto/PayCreateRequest.java`

```java
package io.github.atengk.designpattern.adapter.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 创建支付请求
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
public class PayCreateRequest {

    /**
     * 支付渠道：ALIPAY、WECHAT
     */
    @NotBlank(message = "支付渠道不能为空")
    private String channel;

    /**
     * 业务订单号
     */
    @NotBlank(message = "订单号不能为空")
    private String orderNo;

    /**
     * 支付标题
     */
    @NotBlank(message = "支付标题不能为空")
    private String subject;

    /**
     * 支付金额
     */
    @NotNull(message = "支付金额不能为空")
    @DecimalMin(value = "0.01", message = "支付金额必须大于 0")
    private BigDecimal amount;
}
```

统一支付返回 VO 屏蔽不同渠道返回字段差异，方便 Controller 直接返回给调用方。

文件位置：`src/main/java/io/github/atengk/designpattern/adapter/vo/PayResultVO.java`

```java
package io.github.atengk.designpattern.adapter.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 支付结果返回对象
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
@Builder
public class PayResultVO {

    /**
     * 支付渠道
     */
    private String channel;

    /**
     * 业务订单号
     */
    private String orderNo;

    /**
     * 第三方支付流水号或预支付单号
     */
    private String tradeNo;

    /**
     * 支付状态
     */
    private String status;

    /**
     * 支付提示信息
     */
    private String message;
}
```

统一 API 返回结构用于包装接口返回值，便于前端和调用方统一处理成功与失败响应。

文件位置：`src/main/java/io/github/atengk/designpattern/adapter/vo/ApiResult.java`

```java
package io.github.atengk.designpattern.adapter.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API 统一返回对象
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResult<T> {

    /**
     * 业务状态码
     */
    private Integer code;

    /**
     * 返回消息
     */
    private String message;

    /**
     * 返回数据
     */
    private T data;

    /**
     * 成功返回
     *
     * @param data 返回数据
     * @return API 返回对象
     */
    public static <T> ApiResult<T> success(T data) {
        return ApiResult.<T>builder()
                .code(200)
                .message("操作成功")
                .data(data)
                .build();
    }

    /**
     * 失败返回
     *
     * @param message 失败消息
     * @return API 返回对象
     */
    public static ApiResult<Void> fail(String message) {
        return ApiResult.<Void>builder()
                .code(500)
                .message(message)
                .build();
    }
}
```

下面模拟一个支付宝旧接口。它的入参形式和系统内部的 `PayCreateRequest` 不一致，因此不能直接暴露给业务层使用。

文件位置：`src/main/java/io/github/atengk/designpattern/adapter/external/AlipayTradeResult.java`

```java
package io.github.atengk.designpattern.adapter.external;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 支付宝旧接口返回结果
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlipayTradeResult {

    /**
     * 是否成功
     */
    private Boolean success;

    /**
     * 业务订单号
     */
    private String outTradeNo;

    /**
     * 支付宝交易号
     */
    private String tradeNo;

    /**
     * 返回消息
     */
    private String message;
}
```

支付宝旧客户端模拟第三方 SDK 或老系统接口，业务代码不应该直接依赖它。

文件位置：`src/main/java/io/github/atengk/designpattern/adapter/external/AlipayLegacyClient.java`

```java
package io.github.atengk.designpattern.adapter.external;

import cn.hutool.core.util.IdUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 支付宝旧版支付客户端
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class AlipayLegacyClient {

    /**
     * 创建支付宝交易
     *
     * @param partnerId   合作方 ID
     * @param outTradeNo  业务订单号
     * @param subject     支付标题
     * @param totalAmount 支付金额
     * @return 支付宝交易结果
     */
    public AlipayTradeResult createTrade(String partnerId, String outTradeNo, String subject, BigDecimal totalAmount) {
        log.info("调用支付宝旧版接口，partnerId={}，orderNo={}，amount={}", partnerId, outTradeNo, totalAmount);

        String tradeNo = "ALI_" + IdUtil.fastSimpleUUID();
        return new AlipayTradeResult(Boolean.TRUE, outTradeNo, tradeNo, "支付宝交易创建成功");
    }
}
```

下面模拟一个微信支付接口。它使用 Map 作为入参和出参，这种接口在旧 SDK、HTTP 网关、动态协议中比较常见。

文件位置：`src/main/java/io/github/atengk/designpattern/adapter/external/WechatPayApiClient.java`

```java
package io.github.atengk.designpattern.adapter.external;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.IdUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 微信支付接口客户端
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class WechatPayApiClient {

    /**
     * 创建微信预支付订单
     *
     * @param payload 微信支付请求参数
     * @return 微信支付响应参数
     */
    public Map<String, Object> unifiedOrder(Map<String, Object> payload) {
        String mchId = Convert.toStr(payload.get("mchId"));
        String outTradeNo = Convert.toStr(payload.get("outTradeNo"));
        BigDecimal amount = Convert.toBigDecimal(payload.get("amount"));

        log.info("调用微信支付接口，mchId={}，orderNo={}，amount={}", mchId, outTradeNo, amount);

        return MapUtil.<String, Object>builder(new HashMap<>())
                .put("code", "SUCCESS")
                .put("outTradeNo", outTradeNo)
                .put("prepayId", "WX_" + IdUtil.fastSimpleUUID())
                .put("message", "微信预支付订单创建成功")
                .build();
    }
}
```

统一支付适配器接口是适配器模式的目标接口。业务层只依赖这个接口，而不是依赖具体支付渠道 SDK。

文件位置：`src/main/java/io/github/atengk/designpattern/adapter/adapter/PaymentAdapter.java`

```java
package io.github.atengk.designpattern.adapter.adapter;

import io.github.atengk.designpattern.adapter.dto.PayCreateRequest;
import io.github.atengk.designpattern.adapter.enums.PayChannel;
import io.github.atengk.designpattern.adapter.vo.PayResultVO;

/**
 * 支付适配器接口
 *
 * @author Ateng
 * @since 2026-05-13
 */
public interface PaymentAdapter {

    /**
     * 获取当前适配器支持的支付渠道
     *
     * @return 支付渠道
     */
    PayChannel getChannel();

    /**
     * 创建支付单
     *
     * @param request 创建支付请求
     * @return 支付结果
     */
    PayResultVO createPay(PayCreateRequest request);
}
```

支付宝适配器负责把系统内部统一请求转换成支付宝旧接口参数，并把支付宝返回值转换成统一返回对象。

文件位置：`src/main/java/io/github/atengk/designpattern/adapter/adapter/AlipayPaymentAdapter.java`

```java
package io.github.atengk.designpattern.adapter.adapter;

import cn.hutool.core.util.BooleanUtil;
import io.github.atengk.designpattern.adapter.config.PayProperties;
import io.github.atengk.designpattern.adapter.dto.PayCreateRequest;
import io.github.atengk.designpattern.adapter.enums.PayChannel;
import io.github.atengk.designpattern.adapter.external.AlipayLegacyClient;
import io.github.atengk.designpattern.adapter.external.AlipayTradeResult;
import io.github.atengk.designpattern.adapter.vo.PayResultVO;
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

    private final AlipayLegacyClient alipayLegacyClient;
    private final PayProperties payProperties;

    /**
     * 获取当前适配器支持的支付渠道
     *
     * @return 支付渠道
     */
    @Override
    public PayChannel getChannel() {
        return PayChannel.ALIPAY;
    }

    /**
     * 创建支付宝支付单
     *
     * @param request 创建支付请求
     * @return 支付结果
     */
    @Override
    public PayResultVO createPay(PayCreateRequest request) {
        AlipayTradeResult result = alipayLegacyClient.createTrade(
                payProperties.getAlipay().getPartnerId(),
                request.getOrderNo(),
                request.getSubject(),
                request.getAmount()
        );

        if (!BooleanUtil.isTrue(result.getSuccess())) {
            log.warn("支付宝交易创建失败，orderNo={}，message={}", request.getOrderNo(), result.getMessage());
            throw new IllegalStateException(result.getMessage());
        }

        log.info("支付宝交易创建成功，orderNo={}，tradeNo={}", result.getOutTradeNo(), result.getTradeNo());

        return PayResultVO.builder()
                .channel(getChannel().name())
                .orderNo(result.getOutTradeNo())
                .tradeNo(result.getTradeNo())
                .status("CREATED")
                .message(result.getMessage())
                .build();
    }
}
```

微信支付适配器负责把系统内部统一请求转换成微信接口 Map 参数，并把微信 Map 响应转换成统一返回对象。

文件位置：`src/main/java/io/github/atengk/designpattern/adapter/adapter/WechatPaymentAdapter.java`

```java
package io.github.atengk.designpattern.adapter.adapter;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.designpattern.adapter.config.PayProperties;
import io.github.atengk.designpattern.adapter.dto.PayCreateRequest;
import io.github.atengk.designpattern.adapter.enums.PayChannel;
import io.github.atengk.designpattern.adapter.external.WechatPayApiClient;
import io.github.atengk.designpattern.adapter.vo.PayResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

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

    private final WechatPayApiClient wechatPayApiClient;
    private final PayProperties payProperties;

    /**
     * 获取当前适配器支持的支付渠道
     *
     * @return 支付渠道
     */
    @Override
    public PayChannel getChannel() {
        return PayChannel.WECHAT;
    }

    /**
     * 创建微信支付单
     *
     * @param request 创建支付请求
     * @return 支付结果
     */
    @Override
    public PayResultVO createPay(PayCreateRequest request) {
        Map<String, Object> payload = MapUtil.<String, Object>builder(new HashMap<>())
                .put("mchId", payProperties.getWechat().getMchId())
                .put("outTradeNo", request.getOrderNo())
                .put("description", request.getSubject())
                .put("amount", request.getAmount())
                .build();

        Map<String, Object> response = wechatPayApiClient.unifiedOrder(payload);
        String code = Convert.toStr(response.get("code"));

        if (!StrUtil.equals("SUCCESS", code)) {
            String message = Convert.toStr(response.get("message"), "微信支付单创建失败");
            log.warn("微信支付单创建失败，orderNo={}，message={}", request.getOrderNo(), message);
            throw new IllegalStateException(message);
        }

        String prepayId = Convert.toStr(response.get("prepayId"));
        log.info("微信支付单创建成功，orderNo={}，prepayId={}", request.getOrderNo(), prepayId);

        return PayResultVO.builder()
                .channel(getChannel().name())
                .orderNo(Convert.toStr(response.get("outTradeNo")))
                .tradeNo(prepayId)
                .status("CREATED")
                .message(Convert.toStr(response.get("message")))
                .build();
    }
}
```

适配器注册器在项目启动时收集所有 `PaymentAdapter` 实现类，并根据支付渠道完成路由。

文件位置：`src/main/java/io/github/atengk/designpattern/adapter/adapter/PaymentAdapterRegistry.java`

```java
package io.github.atengk.designpattern.adapter.adapter;

import cn.hutool.core.collection.CollUtil;
import io.github.atengk.designpattern.adapter.enums.PayChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

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

    private final Map<PayChannel, PaymentAdapter> adapterMap;

    /**
     * 初始化支付适配器注册器
     *
     * @param adapters 支付适配器列表
     */
    public PaymentAdapterRegistry(List<PaymentAdapter> adapters) {
        if (CollUtil.isEmpty(adapters)) {
            throw new IllegalStateException("未找到任何支付适配器");
        }

        Map<PayChannel, PaymentAdapter> tempMap = new EnumMap<>(PayChannel.class);
        for (PaymentAdapter adapter : adapters) {
            PaymentAdapter oldAdapter = tempMap.put(adapter.getChannel(), adapter);
            if (oldAdapter != null) {
                throw new IllegalStateException("支付适配器重复注册：" + adapter.getChannel());
            }
            log.info("支付适配器注册成功，channel={}，adapter={}", adapter.getChannel(), adapter.getClass().getSimpleName());
        }

        this.adapterMap = Map.copyOf(tempMap);
    }

    /**
     * 根据支付渠道获取适配器
     *
     * @param channel 支付渠道编码
     * @return 支付适配器
     */
    public PaymentAdapter getAdapter(String channel) {
        PayChannel payChannel = PayChannel.parse(channel);
        PaymentAdapter adapter = adapterMap.get(payChannel);

        if (adapter == null) {
            throw new IllegalArgumentException("未找到支付适配器：" + channel);
        }

        return adapter;
    }
}
```

支付服务只依赖适配器注册器和统一适配器接口，不需要知道支付宝和微信的具体调用细节。

文件位置：`src/main/java/io/github/atengk/designpattern/adapter/service/PaymentService.java`

```java
package io.github.atengk.designpattern.adapter.service;

import io.github.atengk.designpattern.adapter.dto.PayCreateRequest;
import io.github.atengk.designpattern.adapter.vo.PayResultVO;

/**
 * 支付服务接口
 *
 * @author Ateng
 * @since 2026-05-13
 */
public interface PaymentService {

    /**
     * 创建支付单
     *
     * @param request 创建支付请求
     * @return 支付结果
     */
    PayResultVO createPay(PayCreateRequest request);
}
```

支付服务实现类负责业务编排，渠道差异通过适配器完成隔离。

文件位置：`src/main/java/io/github/atengk/designpattern/adapter/service/PaymentServiceImpl.java`

```java
package io.github.atengk.designpattern.adapter.service;

import io.github.atengk.designpattern.adapter.adapter.PaymentAdapter;
import io.github.atengk.designpattern.adapter.adapter.PaymentAdapterRegistry;
import io.github.atengk.designpattern.adapter.dto.PayCreateRequest;
import io.github.atengk.designpattern.adapter.vo.PayResultVO;
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

    private final PaymentAdapterRegistry paymentAdapterRegistry;

    /**
     * 创建支付单
     *
     * @param request 创建支付请求
     * @return 支付结果
     */
    @Override
    public PayResultVO createPay(PayCreateRequest request) {
        log.info("开始创建支付单，channel={}，orderNo={}，amount={}",
                request.getChannel(), request.getOrderNo(), request.getAmount());

        PaymentAdapter adapter = paymentAdapterRegistry.getAdapter(request.getChannel());
        PayResultVO result = adapter.createPay(request);

        log.info("支付单创建完成，channel={}，orderNo={}，tradeNo={}",
                result.getChannel(), result.getOrderNo(), result.getTradeNo());

        return result;
    }
}
```

Controller 对外提供统一支付接口，调用方只需要传入渠道，不需要关心底层渠道 SDK 差异。

文件位置：`src/main/java/io/github/atengk/designpattern/adapter/controller/PaymentController.java`

```java
package io.github.atengk.designpattern.adapter.controller;

import io.github.atengk.designpattern.adapter.dto.PayCreateRequest;
import io.github.atengk.designpattern.adapter.service.PaymentService;
import io.github.atengk.designpattern.adapter.vo.ApiResult;
import io.github.atengk.designpattern.adapter.vo.PayResultVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 支付接口控制器
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
     * 创建支付单
     *
     * @param request 创建支付请求
     * @return 支付结果
     */
    @PostMapping
    public ApiResult<PayResultVO> createPay(@Valid @RequestBody PayCreateRequest request) {
        return ApiResult.success(paymentService.createPay(request));
    }
}
```

全局异常处理器用于统一处理参数错误和业务异常，让接口返回结构保持一致。

文件位置：`src/main/java/io/github/atengk/designpattern/adapter/web/GlobalExceptionHandler.java`

```java
package io.github.atengk.designpattern.adapter.web;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.designpattern.adapter.vo.ApiResult;
import lombok.extern.slf4j.Slf4j;
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
     * @return API 返回对象
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResult<Void> handleValidException(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(error -> StrUtil.format("{} {}", error.getField(), error.getDefaultMessage()))
                .orElse("请求参数不合法");

        log.warn("请求参数校验失败，message={}", message);
        return ApiResult.fail(message);
    }

    /**
     * 处理非法参数异常
     *
     * @param exception 非法参数异常
     * @return API 返回对象
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ApiResult<Void> handleIllegalArgumentException(IllegalArgumentException exception) {
        log.warn("请求参数错误，message={}", exception.getMessage());
        return ApiResult.fail(exception.getMessage());
    }

    /**
     * 处理业务状态异常
     *
     * @param exception 业务状态异常
     * @return API 返回对象
     */
    @ExceptionHandler(IllegalStateException.class)
    public ApiResult<Void> handleIllegalStateException(IllegalStateException exception) {
        log.warn("业务处理失败，message={}", exception.getMessage());
        return ApiResult.fail(exception.getMessage());
    }
}
```

## 使用方式

启动项目后，可以通过统一接口创建不同渠道的支付单。调用方只改变 `channel`，后端自动选择对应适配器。

支付宝支付请求示例：

```bash
curl -X POST 'http://localhost:8080/api/payments' \
  -H 'Content-Type: application/json' \
  -d '{
    "channel": "ALIPAY",
    "orderNo": "ORDER202605130001",
    "subject": "设计模式课程订单",
    "amount": 99.90
  }'
```

支付宝返回示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "channel": "ALIPAY",
    "orderNo": "ORDER202605130001",
    "tradeNo": "ALI_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx",
    "status": "CREATED",
    "message": "支付宝交易创建成功"
  }
}
```

微信支付请求示例：

```bash
curl -X POST 'http://localhost:8080/api/payments' \
  -H 'Content-Type: application/json' \
  -d '{
    "channel": "WECHAT",
    "orderNo": "ORDER202605130002",
    "subject": "设计模式课程订单",
    "amount": 66.60
  }'
```

微信返回示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "channel": "WECHAT",
    "orderNo": "ORDER202605130002",
    "tradeNo": "WX_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx",
    "status": "CREATED",
    "message": "微信预支付订单创建成功"
  }
}
```

不支持的渠道请求示例：

```bash
curl -X POST 'http://localhost:8080/api/payments' \
  -H 'Content-Type: application/json' \
  -d '{
    "channel": "BANK",
    "orderNo": "ORDER202605130003",
    "subject": "设计模式课程订单",
    "amount": 88.80
  }'
```

返回示例：

```json
{
  "code": 500,
  "message": "不支持的支付渠道：BANK",
  "data": null
}
```

## 验证方式

可以从三个角度验证适配器模式是否落地成功。

第一，业务层没有直接依赖第三方客户端。检查 `PaymentServiceImpl`，它只依赖 `PaymentAdapterRegistry`，不依赖 `AlipayLegacyClient` 或 `WechatPayApiClient`。

第二，新增渠道不需要修改主流程。新增银联支付时，只需要新增 `UnionPayClient` 和 `UnionPayPaymentAdapter`，并让新适配器实现 `PaymentAdapter`。Spring 启动时会自动注入到 `PaymentAdapterRegistry` 中。

第三，不同渠道返回值已经被转换成统一结构。无论底层是对象返回、Map 返回、HTTP JSON 返回，Controller 最终都返回 `PayResultVO`。

可重点查看启动日志，确认适配器自动注册成功。

```text
支付适配器注册成功，channel=ALIPAY，adapter=AlipayPaymentAdapter
支付适配器注册成功，channel=WECHAT，adapter=WechatPaymentAdapter
```

## 适用场景

适配器模式适合解决“系统内部接口已经稳定，但外部接口不一致”的问题。在 Spring Boot 项目中，它常见于以下场景：

| 场景          | 说明                                              |
| ------------- | ------------------------------------------------- |
| 第三方支付    | 支付宝、微信、银联、Stripe 等接口参数和返回值不同 |
| 短信供应商    | 阿里云短信、腾讯云短信、华为云短信 SDK 不一致     |
| 文件存储      | MinIO、阿里云 OSS、腾讯云 COS、S3 接口不同        |
| 老系统改造    | 新系统需要兼容旧服务的 RPC、HTTP 或数据库接口     |
| 多渠道通知    | 邮件、短信、站内信、企业微信、钉钉发送接口不同    |
| 外部 API 聚合 | 外部平台字段命名、签名方式、错误码结构不一致      |

## 和其他模式的区别

适配器模式容易和策略模式、外观模式、代理模式混淆。区分时重点看“它解决的主要矛盾是什么”。

| 模式       | 关注点                 | 和适配器的区别                               |
| ---------- | ---------------------- | -------------------------------------------- |
| 适配器模式 | 接口不兼容             | 重点是把已有接口转换成系统需要的接口         |
| 策略模式   | 算法或业务规则可替换   | 重点是选择不同处理逻辑，不一定存在接口不兼容 |
| 外观模式   | 简化复杂子系统调用     | 重点是提供统一入口，不一定改变接口形态       |
| 代理模式   | 控制访问或增强访问过程 | 重点是权限、缓存、事务、远程调用等访问控制   |

在本示例中，`PaymentAdapter` 同时也具备一点策略选择的味道，因为它按支付渠道选择不同实现。但它的核心仍然是“适配第三方接口差异”，所以主要归类为适配器模式。

## 扩展新渠道

如果要新增银联支付，只需要按以下步骤扩展。

先新增渠道枚举：

```java
UNION_PAY
```

再新增第三方客户端，例如：

```text
src/main/java/io/github/atengk/designpattern/adapter/external/UnionPayClient.java
```

最后新增适配器：

```text
src/main/java/io/github/atengk/designpattern/adapter/adapter/UnionPayPaymentAdapter.java
```

只要 `UnionPayPaymentAdapter` 实现 `PaymentAdapter` 并交给 Spring 管理，`PaymentAdapterRegistry` 就会在启动时自动注册它。原有 `PaymentServiceImpl`、`PaymentController` 不需要修改。

这就是适配器模式在项目中的主要价值：把变化隔离在适配器层，把稳定接口留给业务层。

## 注意事项

适配器模式不应该被滥用。如果只是简单字段转换，普通转换方法或 MapStruct 可能更合适。只有当外部接口形态、调用方式、返回结构和错误处理明显不一致时，适配器模式才更有价值。

适配器内部不要堆积复杂业务规则。适配器主要负责协议转换、字段映射、返回值转换和异常转换。订单状态流转、库存扣减、优惠计算等业务规则应该留在业务服务或领域服务中。

适配器接口要保持稳定。比如本示例中的 `PaymentAdapter.createPay()` 应该表达系统内部真正需要的能力，而不是照搬某个第三方 SDK 的方法名和参数结构。

第三方异常要在适配器中转换。不要把 SDK 原始异常直接抛到业务层，否则业务层仍然会被第三方接口污染。

## 总结

适配器模式在 Spring Boot 项目中非常实用，尤其适合第三方接口、老系统接口、多供应商能力接入等场景。

本示例中，系统内部通过 `PaymentAdapter` 定义统一支付能力，支付宝和微信分别通过各自适配器完成接口转换。业务层只依赖统一接口，不直接关心第三方客户端的参数、返回值和调用方式。

最终效果是：

```text
第三方接口差异被限制在适配器层
业务服务只面对统一接口
新增渠道只新增适配器，不修改主流程
第三方 SDK 变化时影响范围更小
代码更清晰，也更容易测试和扩展
```
