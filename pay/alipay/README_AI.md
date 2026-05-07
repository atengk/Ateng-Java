# SpringBoot 支付宝支付开发

本文档用于说明在 SpringBoot3 项目中接入支付宝支付的基础方案，主要覆盖支付模块定位、支付场景选择、接入方式、开放平台配置、证书密钥配置以及项目依赖准备。后续章节可以在此基础上继续展开支付配置设计、业务流程设计、核心功能实现、接口设计、安全幂等和联调验证等内容。

## 模块概述

支付宝支付模块用于封装业务系统与支付宝开放平台之间的支付交互能力，避免订单、会员、商品等业务模块直接依赖支付宝 SDK。该模块重点处理支付请求构建、支付发起、支付结果通知、主动查询、退款申请、退款查询、签名验证和幂等控制。

在 SpringBoot3 项目中，支付宝支付模块通常作为独立业务模块存在，对外提供统一的支付接口，对内封装支付宝 SDK 调用细节。业务系统只需要关心支付订单状态变化，不需要直接处理支付宝网关、签名算法、证书路径和回调验签等底层细节。

### 功能定位

支付宝支付模块的核心定位是支付能力适配层，负责将本系统的业务订单转换为支付宝可识别的交易请求，并将支付宝返回的交易结果同步回本系统。

该模块主要承担以下职责：

| 功能         | 说明                                                         |
| ------------ | ------------------------------------------------------------ |
| 支付订单创建 | 根据业务订单生成本地支付订单，保存商户订单号、金额、支付状态等信息 |
| 支付发起     | 根据不同支付场景调用支付宝接口，例如电脑网站支付、手机网站支付、App 支付 |
| 支付回调处理 | 接收支付宝异步通知，完成验签、参数校验和订单状态更新         |
| 主动查询     | 在回调丢失、状态不明确或定时补偿时主动查询支付宝交易状态     |
| 退款处理     | 根据业务退款申请调用支付宝退款接口，并保存退款记录           |
| 退款查询     | 查询支付宝退款结果，补偿本地退款状态                         |
| 安全校验     | 校验支付宝签名、应用 ID、商户订单号、支付金额和收款方信息    |
| 幂等控制     | 防止重复通知、重复支付、重复退款和订单状态回退               |

支付模块不建议直接处理商品库存、会员权益、优惠券核销等业务逻辑。较合理的方式是：支付模块只负责输出支付成功、支付失败、退款成功等支付事件，具体业务模块根据事件完成后续业务处理。

### 支付场景

支付宝支付在 SpringBoot3 项目中常见的接入场景包括电脑网站支付、手机网站支付、App 支付、支付查询、支付退款和退款查询。不同场景对应不同的支付宝接口，后端返回的数据形式也不同。

| 支付场景     | 支付宝接口                          | 适用终端          | 后端返回结果   | 说明                                                |
| ------------ | ----------------------------------- | ----------------- | -------------- | --------------------------------------------------- |
| 电脑网站支付 | `alipay.trade.page.pay`             | PC 浏览器         | HTML 表单      | 后端生成支付表单，浏览器跳转支付宝收银台            |
| 手机网站支付 | `alipay.trade.wap.pay`              | 手机浏览器 / H5   | HTML 表单      | 后端生成 H5 支付表单，移动端跳转支付宝完成付款      |
| App 支付     | `alipay.trade.app.pay`              | Android / iOS App | 支付订单字符串 | 后端生成签名后的订单字符串，App 调用支付宝 SDK 支付 |
| 支付订单查询 | `alipay.trade.query`                | 服务端            | JSON 响应      | 用于主动查询交易状态，适合回调补偿场景              |
| 支付退款     | `alipay.trade.refund`               | 服务端            | JSON 响应      | 用于整单退款或部分退款                              |
| 退款结果查询 | `alipay.trade.fastpay.refund.query` | 服务端            | JSON 响应      | 用于查询退款结果和补偿退款状态                      |

电脑网站支付和手机网站支付都属于页面跳转类支付，后端通常生成一段 HTML 表单内容，前端页面接收后直接渲染或跳转到支付宝收银台。

App 支付与网页支付不同，后端不直接返回 HTML 页面，而是返回签名后的订单字符串。移动端拿到订单字符串后，调用支付宝移动端 SDK 完成支付。

支付结果不能只依赖同步回跳。同步回跳主要用于用户页面展示，最终订单状态应以支付宝异步通知或服务端主动查询结果为准。

### 接入方式

SpringBoot3 接入支付宝支付时，建议使用支付宝 Java SDK，并将 `AlipayClient` 注册为 Spring Bean。业务代码通过注入支付服务完成支付、查询和退款，不直接在 Controller 中调用支付宝 SDK。

推荐接入方式如下：

| 接入项       | 推荐方案          | 说明                                                   |
| ------------ | ----------------- | ------------------------------------------------------ |
| SDK          | 支付宝 Java SDK   | 封装支付宝 OpenAPI 调用、加签、验签和请求处理          |
| 签名方式     | RSA2              | 推荐使用 RSA2，安全性高于普通 RSA                      |
| 密钥模式     | 证书模式          | 通过应用公钥证书、支付宝公钥证书和支付宝根证书完成验签 |
| 配置管理     | `application.yml` | 将 AppId、网关、证书路径、回调地址等配置外置           |
| 客户端初始化 | Spring Bean       | 项目启动时初始化 `AlipayClient`，业务服务中直接注入    |
| 回调处理     | 异步通知为准      | 支付状态最终以异步通知或主动查询为准                   |
| 状态补偿     | 定时查询          | 对长时间未收到回调的订单进行主动查询补偿               |

推荐的模块调用链路如下：

```text
业务订单模块
    ↓
支付订单创建
    ↓
支付宝支付模块
    ↓
AlipayClient
    ↓
支付宝开放平台
    ↓
异步通知 / 主动查询
    ↓
更新支付订单状态
    ↓
通知业务订单模块
```

这种接入方式可以降低业务模块与支付宝 SDK 的耦合度。后续如果需要扩展微信支付、银联支付或聚合支付，也可以在支付模块内继续增加支付渠道适配层。

## 环境准备

环境准备主要包括支付宝开放平台应用配置、支付产品开通、应用证书与密钥配置、项目依赖引入和本地基础配置。该部分完成后，SpringBoot3 项目才具备初始化支付宝客户端和发起支付请求的基础条件。

### 支付宝开放平台配置

接入支付宝支付前，需要先在支付宝开放平台创建应用，并开通对应的支付产品。开发阶段可以优先使用沙箱环境完成本地联调，正式上线前再切换到生产环境。

基础配置流程如下：

1. 登录支付宝开放平台。
2. 创建网页应用、移动应用或小程序应用，具体类型根据业务终端选择。
3. 获取应用的 `app_id`。
4. 开通需要使用的支付产品，例如电脑网站支付、手机网站支付或 App 支付。
5. 配置接口加签方式，推荐使用 RSA2 证书模式。
6. 上传应用公钥，下载支付宝相关证书。
7. 配置异步通知地址 `notify_url`。
8. 配置同步回跳地址 `return_url`。
9. 使用沙箱账号完成支付联调。
10. 应用审核通过后，切换正式环境网关和正式应用配置。

常用配置项如下：

| 配置项              | 示例                                                  | 说明                     |
| ------------------- | ----------------------------------------------------- | ------------------------ |
| `appId`             | `2021000000000000`                                    | 支付宝应用 ID            |
| `gatewayUrl`        | `https://openapi.alipay.com/gateway.do`               | 支付宝正式环境网关       |
| `sandboxGatewayUrl` | `https://openapi-sandbox.dl.alipaydev.com/gateway.do` | 支付宝沙箱环境网关       |
| `notifyUrl`         | `https://api.example.com/pay/alipay/notify`           | 支付宝异步通知地址       |
| `returnUrl`         | `https://www.example.com/pay/result`                  | 支付完成后的同步回跳地址 |
| `signType`          | `RSA2`                                                | 签名类型                 |
| `charset`           | `UTF-8`                                               | 字符集                   |
| `format`            | `json`                                                | 响应格式                 |

异步通知地址必须是公网可访问的服务端接口。本地开发时，如果需要接收支付宝回调，可以使用内网穿透工具将本地服务暴露为公网地址。

需要注意的是，同步回跳地址只适合用于支付结果页面展示，不适合直接作为订单支付成功依据。正式订单状态应通过异步通知或主动查询确认。

### 应用证书与密钥配置

支付宝支付推荐使用证书模式进行加签和验签。证书模式通常需要准备应用私钥、应用公钥证书、支付宝公钥证书和支付宝根证书。

常见证书文件如下：

| 文件           | 说明                                       |
| -------------- | ------------------------------------------ |
| 应用私钥       | 商户系统自己保存，用于请求支付宝接口时加签 |
| 应用公钥证书   | 上传应用公钥后由支付宝开放平台生成         |
| 支付宝公钥证书 | 用于校验支付宝响应或通知签名               |
| 支付宝根证书   | 用于 SDK 证书模式下的证书链校验            |

推荐的开发环境证书目录如下：

```text
src/main/resources/cert/alipay/
├── appCertPublicKey.crt
├── alipayCertPublicKey_RSA2.crt
└── alipayRootCert.crt
```

推荐的生产环境证书目录如下：

```text
/data/app/pay/cert/alipay/
├── appCertPublicKey.crt
├── alipayCertPublicKey_RSA2.crt
└── alipayRootCert.crt
```

生产环境不建议将应用私钥和证书文件直接提交到 Git 仓库。应用私钥应通过环境变量、配置中心、Kubernetes Secret 或密钥管理服务注入。证书文件可以通过服务器挂载目录或容器挂载目录提供。

支付宝配置示例放在 `application.yml` 中：

```yaml
# 支付配置
pay:
  alipay:
    # 是否启用支付宝支付
    enabled: true

    # 支付宝应用 ID
    app-id: ${ALIPAY_APP_ID}

    # 支付宝网关地址
    gateway-url: ${ALIPAY_GATEWAY_URL:https://openapi.alipay.com/gateway.do}

    # 应用私钥，建议通过环境变量或配置中心注入
    private-key: ${ALIPAY_PRIVATE_KEY}

    # 请求格式
    format: json

    # 字符集
    charset: UTF-8

    # 签名类型，推荐 RSA2
    sign-type: RSA2

    # 应用公钥证书路径
    app-cert-path: ${ALIPAY_APP_CERT_PATH:/data/app/pay/cert/alipay/appCertPublicKey.crt}

    # 支付宝公钥证书路径
    alipay-cert-path: ${ALIPAY_PUBLIC_CERT_PATH:/data/app/pay/cert/alipay/alipayCertPublicKey_RSA2.crt}

    # 支付宝根证书路径
    alipay-root-cert-path: ${ALIPAY_ROOT_CERT_PATH:/data/app/pay/cert/alipay/alipayRootCert.crt}

    # 支付宝异步通知地址
    notify-url: ${ALIPAY_NOTIFY_URL:https://api.example.com/pay/alipay/notify}

    # 支付宝同步回跳地址
    return-url: ${ALIPAY_RETURN_URL:https://www.example.com/pay/result}
```

本地开发可以通过环境变量注入关键配置：

```bash
export ALIPAY_APP_ID="2021000000000000"
export ALIPAY_GATEWAY_URL="https://openapi-sandbox.dl.alipaydev.com/gateway.do"
export ALIPAY_PRIVATE_KEY="你的应用私钥"
export ALIPAY_APP_CERT_PATH="/Users/ateng/cert/alipay/appCertPublicKey.crt"
export ALIPAY_PUBLIC_CERT_PATH="/Users/ateng/cert/alipay/alipayCertPublicKey_RSA2.crt"
export ALIPAY_ROOT_CERT_PATH="/Users/ateng/cert/alipay/alipayRootCert.crt"
export ALIPAY_NOTIFY_URL="https://你的公网域名/pay/alipay/notify"
export ALIPAY_RETURN_URL="https://你的前端域名/pay/result"
```

以上配置用于启动 SpringBoot3 项目前注入支付宝支付参数。实际生产环境中，不建议直接在启动脚本中明文写入应用私钥。

### SpringBoot3 项目依赖

SpringBoot3 项目需要引入 Web、参数校验、支付宝 SDK、Hutool、Lombok 和配置提示相关依赖。其中支付宝 SDK 用于调用支付宝开放平台接口，Hutool 用于字符串、集合、JSON、日期和对象判断等通用处理。

文件位置：`pom.xml`

```xml
<properties>
    <!-- SpringBoot3 推荐使用 Java 17 或更高版本 -->
    <java.version>17</java.version>

    <!-- 支付宝 Java SDK 版本，可根据 Maven 仓库版本调整 -->
    <alipay-sdk.version>4.40.133.ALL</alipay-sdk.version>

    <!-- Hutool 工具类版本 -->
    <hutool.version>5.8.38</hutool.version>
</properties>

<dependencies>
    <!-- Web 模块：提供 Controller、回调接口和 HTTP 请求处理能力 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- 参数校验：用于校验支付请求参数 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- 支付宝 Java SDK：用于支付、查询、退款、验签等操作 -->
    <dependency>
        <groupId>com.alipay.sdk</groupId>
        <artifactId>alipay-sdk-java</artifactId>
        <version>${alipay-sdk.version}</version>
    </dependency>

    <!-- Hutool：提供常用工具类，简化字符串、集合、JSON、日期等处理 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>${hutool.version}</version>
    </dependency>

    <!-- Lombok：简化实体类、配置类和日志对象等样板代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- 配置提示：支持自定义配置项在 IDE 中提示 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-configuration-processor</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- 测试依赖：用于服务测试、参数校验测试和回调验签测试 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

支付宝配置建议映射为独立配置类，后续初始化 `AlipayClient` 时直接注入该配置类。

文件位置：`src/main/java/io/github/atengk/pay/config/AlipayProperties.java`

该类用于接收 `application.yml` 中的支付宝支付配置，并提供基础环境判断方法。

```java
package io.github.atengk.pay.config;

import cn.hutool.core.util.StrUtil;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 支付宝支付配置属性
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Data
@Validated
@ConfigurationProperties(prefix = "pay.alipay")
public class AlipayProperties {

    /**
     * 是否启用支付宝支付
     */
    private Boolean enabled = true;

    /**
     * 支付宝应用 ID
     */
    @NotBlank(message = "支付宝 appId 不能为空")
    private String appId;

    /**
     * 支付宝网关地址
     */
    @NotBlank(message = "支付宝网关地址不能为空")
    private String gatewayUrl;

    /**
     * 应用私钥
     */
    @NotBlank(message = "支付宝应用私钥不能为空")
    private String privateKey;

    /**
     * 请求格式
     */
    private String format = "json";

    /**
     * 字符集
     */
    private String charset = "UTF-8";

    /**
     * 签名类型
     */
    private String signType = "RSA2";

    /**
     * 应用公钥证书路径
     */
    @NotBlank(message = "支付宝应用公钥证书路径不能为空")
    private String appCertPath;

    /**
     * 支付宝公钥证书路径
     */
    @NotBlank(message = "支付宝公钥证书路径不能为空")
    private String alipayCertPath;

    /**
     * 支付宝根证书路径
     */
    @NotBlank(message = "支付宝根证书路径不能为空")
    private String alipayRootCertPath;

    /**
     * 异步通知地址
     */
    @NotBlank(message = "支付宝异步通知地址不能为空")
    private String notifyUrl;

    /**
     * 同步回跳地址
     */
    @NotBlank(message = "支付宝同步回跳地址不能为空")
    private String returnUrl;

    /**
     * 判断是否为沙箱环境
     *
     * @return 是否为沙箱环境
     */
    public boolean isSandbox() {
        return StrUtil.containsIgnoreCase(this.gatewayUrl, "sandbox");
    }
}
```

在启动类或配置类中启用该配置属性：

文件位置：`src/main/java/io/github/atengk/PayApplication.java`

该启动类用于启动 SpringBoot3 项目，并启用支付宝配置属性绑定。

```java
package io.github.atengk;

import io.github.atengk.pay.config.AlipayProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * 支付服务启动类
 *
 * @author Ateng
 * @since 2026-05-07
 */
@SpringBootApplication
@EnableConfigurationProperties(AlipayProperties.class)
public class PayApplication {

    /**
     * 项目启动入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(PayApplication.class, args);
    }
}
```

完成以上依赖和配置后，项目已经具备支付宝支付接入的基础条件。下一步可以继续编写 `AlipayClient` 初始化配置，并在支付服务中封装电脑网站支付、手机网站支付、App 支付、支付查询和退款等核心能力。



## 支付配置设计

支付配置设计用于统一管理支付宝接入参数、客户端初始化方式和多环境差异。该部分的目标是让业务代码不直接感知支付宝网关、证书路径、签名方式、回调地址等底层配置，后续支付、查询、退款和回调验签都通过统一配置完成。

### 支付宝参数配置

支付宝参数配置建议统一放在 `application.yml` 或配置中心中，并通过 `@ConfigurationProperties` 映射为 Java 配置类。这样可以避免在业务代码中硬编码 `appId`、网关地址、私钥、证书路径和回调地址。

支付宝核心配置项如下：

| 配置项                  | 必填 | 说明                    |
| ----------------------- | ---- | ----------------------- |
| `enabled`               | 否   | 是否启用支付宝支付      |
| `app-id`                | 是   | 支付宝开放平台应用 ID   |
| `gateway-url`           | 是   | 支付宝开放平台网关地址  |
| `private-key`           | 是   | 应用私钥，用于请求加签  |
| `format`                | 否   | 请求格式，通常为 `json` |
| `charset`               | 否   | 字符集，通常为 `UTF-8`  |
| `sign-type`             | 是   | 签名类型，推荐 `RSA2`   |
| `app-cert-path`         | 是   | 应用公钥证书路径        |
| `alipay-cert-path`      | 是   | 支付宝公钥证书路径      |
| `alipay-root-cert-path` | 是   | 支付宝根证书路径        |
| `notify-url`            | 是   | 支付宝异步通知地址      |
| `return-url`            | 是   | 支付宝同步回跳地址      |

推荐配置如下：

文件位置：`src/main/resources/application.yml`

```yaml
server:
  port: 8080

spring:
  application:
    name: springboot-alipay-demo
  profiles:
    active: dev

pay:
  alipay:
    # 是否启用支付宝支付
    enabled: true

    # 支付宝应用 ID
    app-id: ${ALIPAY_APP_ID}

    # 支付宝网关地址
    gateway-url: ${ALIPAY_GATEWAY_URL}

    # 应用私钥，生产环境建议从环境变量、配置中心或密钥服务读取
    private-key: ${ALIPAY_PRIVATE_KEY}

    # 请求格式
    format: json

    # 字符集
    charset: UTF-8

    # 签名类型，推荐 RSA2
    sign-type: RSA2

    # 应用公钥证书路径
    app-cert-path: ${ALIPAY_APP_CERT_PATH}

    # 支付宝公钥证书路径
    alipay-cert-path: ${ALIPAY_PUBLIC_CERT_PATH}

    # 支付宝根证书路径
    alipay-root-cert-path: ${ALIPAY_ROOT_CERT_PATH}

    # 支付宝异步通知地址
    notify-url: ${ALIPAY_NOTIFY_URL}

    # 支付宝同步回跳地址，仅用于页面展示，不作为最终支付成功依据
    return-url: ${ALIPAY_RETURN_URL}
```

支付宝配置类用于承接上述配置项，并提供基础参数校验和环境判断能力。

文件位置：`src/main/java/io/github/atengk/pay/config/AlipayProperties.java`

该类用于绑定 `pay.alipay` 配置，并在项目启动时校验关键配置是否完整。

```java
package io.github.atengk.pay.config;

import cn.hutool.core.util.StrUtil;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 支付宝支付配置属性
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Data
@Validated
@ConfigurationProperties(prefix = "pay.alipay")
public class AlipayProperties {

    /**
     * 是否启用支付宝支付
     */
    private Boolean enabled = true;

    /**
     * 支付宝应用 ID
     */
    @NotBlank(message = "支付宝 appId 不能为空")
    private String appId;

    /**
     * 支付宝网关地址
     */
    @NotBlank(message = "支付宝网关地址不能为空")
    private String gatewayUrl;

    /**
     * 应用私钥
     */
    @NotBlank(message = "支付宝应用私钥不能为空")
    private String privateKey;

    /**
     * 请求格式
     */
    private String format = "json";

    /**
     * 字符集
     */
    private String charset = "UTF-8";

    /**
     * 签名类型
     */
    private String signType = "RSA2";

    /**
     * 应用公钥证书路径
     */
    @NotBlank(message = "支付宝应用公钥证书路径不能为空")
    private String appCertPath;

    /**
     * 支付宝公钥证书路径
     */
    @NotBlank(message = "支付宝公钥证书路径不能为空")
    private String alipayCertPath;

    /**
     * 支付宝根证书路径
     */
    @NotBlank(message = "支付宝根证书路径不能为空")
    private String alipayRootCertPath;

    /**
     * 异步通知地址
     */
    @NotBlank(message = "支付宝异步通知地址不能为空")
    private String notifyUrl;

    /**
     * 同步回跳地址
     */
    @NotBlank(message = "支付宝同步回跳地址不能为空")
    private String returnUrl;

    /**
     * 判断是否为沙箱环境
     *
     * @return 是否为沙箱环境
     */
    public boolean isSandbox() {
        return StrUtil.containsIgnoreCase(this.gatewayUrl, "sandbox");
    }
}
```

### 支付客户端初始化

支付客户端初始化用于创建支付宝 SDK 的 `AlipayClient` 实例。项目中建议只初始化一个支付宝客户端 Bean，支付、查询、退款和验签服务统一注入使用，不要在每个业务方法中重复创建客户端对象。

证书模式下，客户端初始化需要读取应用私钥、应用公钥证书、支付宝公钥证书和支付宝根证书。启动时建议检查证书文件是否存在，避免运行到支付请求时才暴露配置错误。

文件位置：`src/main/java/io/github/atengk/pay/config/AlipayClientConfig.java`

该配置类用于根据支付宝配置初始化 `AlipayClient`，并在启动时检查证书路径是否有效。

```java
package io.github.atengk.pay.config;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.alipay.api.AlipayClient;
import com.alipay.api.CertAlipayRequest;
import com.alipay.api.DefaultAlipayClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 支付宝客户端配置
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class AlipayClientConfig {

    private final AlipayProperties alipayProperties;

    /**
     * 初始化支付宝客户端
     *
     * @return 支付宝客户端
     */
    @Bean
    @ConditionalOnProperty(prefix = "pay.alipay", name = "enabled", havingValue = "true", matchIfMissing = true)
    public AlipayClient alipayClient() {
        checkCertPath();

        CertAlipayRequest request = new CertAlipayRequest();
        request.setServerUrl(alipayProperties.getGatewayUrl());
        request.setAppId(alipayProperties.getAppId());
        request.setPrivateKey(alipayProperties.getPrivateKey());
        request.setFormat(StrUtil.blankToDefault(alipayProperties.getFormat(), "json"));
        request.setCharset(StrUtil.blankToDefault(alipayProperties.getCharset(), "UTF-8"));
        request.setSignType(StrUtil.blankToDefault(alipayProperties.getSignType(), "RSA2"));
        request.setCertPath(alipayProperties.getAppCertPath());
        request.setAlipayPublicCertPath(alipayProperties.getAlipayCertPath());
        request.setRootCertPath(alipayProperties.getAlipayRootCertPath());

        log.info("支付宝客户端初始化完成，当前环境：{}", alipayProperties.isSandbox() ? "沙箱环境" : "正式环境");
        return new DefaultAlipayClient(request);
    }

    /**
     * 检查支付宝证书路径
     */
    private void checkCertPath() {
        checkFileExists(alipayProperties.getAppCertPath(), "应用公钥证书");
        checkFileExists(alipayProperties.getAlipayCertPath(), "支付宝公钥证书");
        checkFileExists(alipayProperties.getAlipayRootCertPath(), "支付宝根证书");
    }

    /**
     * 检查文件是否存在
     *
     * @param path 文件路径
     * @param name 文件名称
     */
    private void checkFileExists(String path, String name) {
        if (StrUtil.isBlank(path)) {
            throw new IllegalArgumentException(name + "路径不能为空");
        }
        if (!FileUtil.exist(path)) {
            throw new IllegalArgumentException(name + "不存在，路径：" + path);
        }
    }
}
```

完成客户端初始化后，后续支付服务中只需要注入 `AlipayClient`：

```java
private final AlipayClient alipayClient;
```

不建议在支付方法中通过 `new DefaultAlipayClient(...)` 重复创建客户端。重复创建会导致配置分散、测试困难，也容易出现不同业务方法使用不同支付宝参数的问题。

### 多环境配置区分

支付宝支付通常需要区分本地开发环境、测试环境、沙箱环境和生产环境。多环境配置的核心目标是确保不同环境使用不同的支付宝应用、网关地址、证书路径和回调地址，避免测试交易误发到正式环境。

推荐使用 Spring Profile 区分配置文件：

```text
src/main/resources/
├── application.yml
├── application-dev.yml
├── application-test.yml
└── application-prod.yml
```

开发环境配置示例：

文件位置：`src/main/resources/application-dev.yml`

```yaml
pay:
  alipay:
    # 开发环境启用沙箱支付
    enabled: true

    # 沙箱应用 ID
    app-id: ${ALIPAY_APP_ID}

    # 沙箱网关地址
    gateway-url: https://openapi-sandbox.dl.alipaydev.com/gateway.do

    # 沙箱应用私钥
    private-key: ${ALIPAY_PRIVATE_KEY}

    # 本地或开发服务器上的沙箱证书路径
    app-cert-path: ${ALIPAY_APP_CERT_PATH:/Users/ateng/cert/alipay/appCertPublicKey.crt}
    alipay-cert-path: ${ALIPAY_PUBLIC_CERT_PATH:/Users/ateng/cert/alipay/alipayCertPublicKey_RSA2.crt}
    alipay-root-cert-path: ${ALIPAY_ROOT_CERT_PATH:/Users/ateng/cert/alipay/alipayRootCert.crt}

    # 内网穿透后的异步通知地址
    notify-url: ${ALIPAY_NOTIFY_URL:https://dev.example.com/pay/alipay/notify}

    # 开发环境前端回跳地址
    return-url: ${ALIPAY_RETURN_URL:http://localhost:5173/pay/result}
```

生产环境配置示例：

文件位置：`src/main/resources/application-prod.yml`

```yaml
pay:
  alipay:
    # 生产环境启用支付宝支付
    enabled: true

    # 正式应用 ID
    app-id: ${ALIPAY_APP_ID}

    # 正式环境网关
    gateway-url: https://openapi.alipay.com/gateway.do

    # 正式应用私钥，必须通过安全方式注入
    private-key: ${ALIPAY_PRIVATE_KEY}

    # 生产服务器证书挂载路径
    app-cert-path: ${ALIPAY_APP_CERT_PATH:/data/app/pay/cert/alipay/appCertPublicKey.crt}
    alipay-cert-path: ${ALIPAY_PUBLIC_CERT_PATH:/data/app/pay/cert/alipay/alipayCertPublicKey_RSA2.crt}
    alipay-root-cert-path: ${ALIPAY_ROOT_CERT_PATH:/data/app/pay/cert/alipay/alipayRootCert.crt}

    # 生产环境异步通知地址，必须公网可访问
    notify-url: ${ALIPAY_NOTIFY_URL:https://api.example.com/pay/alipay/notify}

    # 生产环境同步回跳地址
    return-url: ${ALIPAY_RETURN_URL:https://www.example.com/pay/result}
```

环境切换可以通过启动参数完成：

```bash
java -jar springboot-alipay-demo.jar --spring.profiles.active=prod
```

该命令用于以生产环境配置启动服务。`--spring.profiles.active=prod` 表示启用 `application-prod.yml`，实际部署时需要保证生产环境变量和证书文件已经准备完成。

多环境配置需要重点注意以下问题：

| 问题                     | 说明                                               |
| ------------------------ | -------------------------------------------------- |
| 沙箱和正式应用不能混用   | 沙箱 AppId、私钥、证书、网关必须成套使用           |
| 回调地址必须公网可访问   | 支付宝服务器无法访问本地 `localhost` 地址          |
| 私钥不能提交到仓库       | 应用私钥属于高敏感信息，应由环境变量或密钥服务注入 |
| 证书路径要随环境变化     | 本地、测试、生产服务器上的证书路径通常不同         |
| 生产环境禁止使用沙箱网关 | 上线前需要检查 `gateway-url` 是否为正式网关        |

## 业务流程设计

业务流程设计用于定义支付订单从创建、发起支付、接收通知、主动查询到退款处理的完整链路。支付宝支付不是单次接口调用即可完成的功能，而是一个包含本地订单、支付宝交易、异步通知、状态补偿和退款处理的完整状态流转过程。

建议先定义本地支付订单状态：

| 状态             | 说明                                   |
| ---------------- | -------------------------------------- |
| `WAIT_PAY`       | 待支付，支付订单已创建但用户未完成付款 |
| `PAYING`         | 支付中，已向支付宝发起支付请求         |
| `PAY_SUCCESS`    | 支付成功，支付宝确认交易成功           |
| `PAY_CLOSED`     | 交易关闭，用户未支付或订单超时关闭     |
| `PAY_FAILED`     | 支付失败，支付请求或交易处理失败       |
| `REFUNDING`      | 退款中，已发起退款但未确认结果         |
| `REFUND_SUCCESS` | 退款成功                               |
| `REFUND_FAILED`  | 退款失败                               |

订单状态只能按业务允许的方向流转，不能随意覆盖。例如，已支付成功的订单不能因为重复收到旧通知而回退到待支付状态。

### 订单创建流程

订单创建流程用于在用户提交业务订单后，生成本地支付订单。此阶段还没有真正请求支付宝，只是在本系统中建立支付记录，为后续支付发起、回调验签和状态查询提供依据。

推荐流程如下：

```text
用户提交订单
    ↓
业务订单模块创建业务订单
    ↓
支付模块创建支付订单
    ↓
生成商户订单号 out_trade_no
    ↓
保存支付金额、支付场景、订单状态
    ↓
返回支付订单号给前端
```

订单创建时需要重点处理以下数据：

| 字段       | 说明                                      |
| ---------- | ----------------------------------------- |
| 业务订单号 | 业务系统自身订单号，例如商品订单号        |
| 商户订单号 | 传给支付宝的 `out_trade_no`，必须全局唯一 |
| 支付金额   | 实际需要支付的金额                        |
| 支付标题   | 展示给用户的订单标题                      |
| 支付场景   | PC、H5、App 等                            |
| 支付状态   | 初始状态为 `WAIT_PAY`                     |
| 过期时间   | 用于控制订单支付有效期                    |
| 用户标识   | 记录当前支付用户，便于后续查询和风控      |

订单创建阶段不应直接调用支付宝支付接口。这样可以保证本地系统先拥有完整的支付订单记录，后续即使支付请求失败，也可以根据本地支付订单号追踪问题。

### 支付发起流程

支付发起流程用于根据本地支付订单调用支付宝接口，并返回前端可使用的支付参数。不同支付场景返回结果不同，电脑网站支付和手机网站支付通常返回 HTML 表单，App 支付返回订单字符串。

推荐流程如下：

```text
前端请求发起支付
    ↓
查询本地支付订单
    ↓
校验订单是否存在、是否待支付、是否过期
    ↓
根据支付场景选择支付宝接口
    ↓
调用支付宝 SDK
    ↓
更新本地支付订单为 PAYING
    ↓
返回支付表单或订单字符串
```

不同支付场景的处理方式如下：

| 支付场景     | 服务端处理                                  | 前端处理                         |
| ------------ | ------------------------------------------- | -------------------------------- |
| 电脑网站支付 | 调用 `alipay.trade.page.pay` 生成 HTML 表单 | 浏览器渲染表单并跳转支付宝收银台 |
| 手机网站支付 | 调用 `alipay.trade.wap.pay` 生成 HTML 表单  | H5 页面跳转支付宝                |
| App 支付     | 调用 `alipay.trade.app.pay` 生成订单字符串  | App 调用支付宝 SDK 完成支付      |

支付发起前必须校验订单状态。如果本地支付订单已经是 `PAY_SUCCESS`，则不应重复发起支付。如果订单已过期，应先关闭本地订单或提示用户重新下单。

推荐校验规则如下：

| 校验项         | 处理方式                     |
| -------------- | ---------------------------- |
| 支付订单不存在 | 返回订单不存在               |
| 订单已支付     | 返回已支付状态，不重复发起   |
| 订单已关闭     | 返回订单已关闭               |
| 订单已过期     | 更新为关闭状态，提示重新下单 |
| 金额异常       | 拒绝发起支付，并记录异常日志 |
| 支付场景不支持 | 返回不支持的支付方式         |

### 支付结果通知流程

支付结果通知流程用于处理支付宝服务器主动推送的异步通知。异步通知是服务端确认支付结果的核心依据，不能直接相信前端同步回跳结果。

推荐流程如下：

```text
支付宝发送异步通知
    ↓
接收通知参数
    ↓
支付宝签名验证
    ↓
校验 app_id、out_trade_no、total_amount、seller_id
    ↓
查询本地支付订单
    ↓
判断通知是否已处理
    ↓
根据 trade_status 更新支付订单状态
    ↓
触发业务订单支付成功逻辑
    ↓
返回 success
```

支付宝异步通知处理必须先验签，验签通过后再处理业务逻辑。不能在验签前修改订单状态。

常见交易状态处理如下：

| 支付宝交易状态   | 本地处理                                                     |
| ---------------- | ------------------------------------------------------------ |
| `TRADE_SUCCESS`  | 更新本地支付订单为 `PAY_SUCCESS`                             |
| `TRADE_FINISHED` | 更新本地支付订单为 `PAY_SUCCESS`，通常表示交易完成且不可退款 |
| `WAIT_BUYER_PAY` | 保持待支付或支付中状态                                       |
| `TRADE_CLOSED`   | 更新本地支付订单为 `PAY_CLOSED`                              |

异步通知需要保证幂等。支付宝可能因为网络超时、响应失败或系统重试多次推送同一笔交易通知。本地处理时应通过商户订单号、支付宝交易号和通知状态判断是否已经处理过。

推荐幂等策略如下：

| 场景                           | 处理方式                             |
| ------------------------------ | ------------------------------------ |
| 已处理成功订单再次收到成功通知 | 直接返回 `success`                   |
| 已关闭订单收到成功通知         | 主动查询支付宝状态后再决定是否修正   |
| 金额不一致                     | 拒绝处理，记录安全告警日志           |
| 应用 ID 不一致                 | 拒绝处理，记录安全告警日志           |
| 验签失败                       | 拒绝处理，返回失败响应               |
| 业务处理异常                   | 不返回 `success`，允许支付宝后续重试 |

### 主动查询流程

主动查询流程用于补偿异步通知丢失、通知处理失败、用户支付后页面中断或本地状态长时间未更新等问题。支付系统不能完全依赖异步通知，需要具备主动查询支付宝交易状态的能力。

推荐触发主动查询的场景如下：

| 触发场景                       | 说明                                             |
| ------------------------------ | ------------------------------------------------ |
| 用户支付完成后前端主动刷新状态 | 前端调用查询接口，本地状态不明确时触发支付宝查询 |
| 异步通知长时间未收到           | 定时任务扫描 `PAYING` 状态订单并主动查询         |
| 支付订单状态异常               | 本地状态与业务订单状态不一致时主动查询           |
| 客服或后台人工查询             | 管理后台查询订单时触发状态同步                   |
| 退款前校验                     | 发起退款前确认原交易是否支付成功                 |

主动查询流程如下：

```text
触发支付状态查询
    ↓
查询本地支付订单
    ↓
判断本地状态是否已终态
    ↓
未终态则调用 alipay.trade.query
    ↓
解析支付宝交易状态
    ↓
更新本地支付订单
    ↓
同步业务订单状态
    ↓
返回最新支付状态
```

主动查询不应无条件频繁调用支付宝接口。可以结合订单状态和时间间隔控制查询频率，例如只查询 `WAIT_PAY`、`PAYING` 状态，并且距离上次查询超过一定时间后再请求支付宝。

推荐查询策略如下：

| 本地状态      | 是否需要查询支付宝 | 说明                         |
| ------------- | ------------------ | ---------------------------- |
| `WAIT_PAY`    | 可以查询           | 用户可能已经支付但回调未到达 |
| `PAYING`      | 需要查询           | 支付中状态需要补偿           |
| `PAY_SUCCESS` | 通常不需要         | 已经是成功终态               |
| `PAY_CLOSED`  | 通常不需要         | 已关闭订单一般不再查询       |
| `PAY_FAILED`  | 按需查询           | 需要根据失败原因决定是否补偿 |
| `REFUNDING`   | 查询退款接口       | 应走退款查询流程             |

主动查询更新状态时，也要遵守状态流转规则。不能因为支付宝返回短暂中间态而覆盖本地已成功状态。

### 退款处理流程

退款处理流程用于在订单支付成功后，根据业务售后或取消订单需求向支付宝发起退款。退款可以是全额退款，也可以是部分退款。退款处理需要同时维护本地退款记录和原支付订单状态。

推荐流程如下：

```text
业务系统发起退款申请
    ↓
校验原支付订单是否支付成功
    ↓
校验退款金额是否合法
    ↓
创建本地退款记录
    ↓
生成退款请求号 out_request_no
    ↓
调用 alipay.trade.refund
    ↓
根据支付宝返回结果更新退款状态
    ↓
必要时主动查询退款结果
    ↓
同步业务退款状态
```

退款处理需要重点校验以下内容：

| 校验项             | 说明                                   |
| ------------------ | -------------------------------------- |
| 原支付订单是否存在 | 不存在则不能退款                       |
| 原订单是否支付成功 | 只有支付成功订单才能退款               |
| 是否超过可退款金额 | 部分退款时累计退款金额不能超过实付金额 |
| 是否重复退款       | 同一业务退款单不能重复发起             |
| 退款请求号是否唯一 | `out_request_no` 必须唯一              |
| 退款原因是否完整   | 便于后续财务、客服和审计查询           |

退款状态建议独立维护，不要只依赖支付订单状态。一个支付订单可能发生多次部分退款，因此应设计单独的退款记录表保存每次退款请求。

推荐退款状态如下：

| 状态             | 说明                           |
| ---------------- | ------------------------------ |
| `REFUND_INIT`    | 退款记录已创建，尚未请求支付宝 |
| `REFUNDING`      | 退款处理中                     |
| `REFUND_SUCCESS` | 退款成功                       |
| `REFUND_FAILED`  | 退款失败                       |
| `REFUND_CLOSED`  | 退款关闭或取消                 |

退款成功后，需要根据累计退款金额判断原支付订单状态：

| 退款结果     | 支付订单处理                                              |
| ------------ | --------------------------------------------------------- |
| 未退款       | 保持 `PAY_SUCCESS`                                        |
| 部分退款成功 | 可标记为 `PARTIAL_REFUND`，或在支付订单中记录累计退款金额 |
| 全额退款成功 | 标记为 `REFUND_SUCCESS` 或 `FULL_REFUND`                  |
| 退款失败     | 保持原支付成功状态，并记录失败原因                        |

退款接口调用成功不一定代表整个业务链路已经完成。系统仍需保存支付宝退款返回结果，并在必要时通过退款查询接口补偿退款状态。对于金额敏感业务，建议退款成功后再触发业务侧售后完成、库存回补或权益回收逻辑。



## 数据库设计

数据库设计用于承载支付宝支付过程中的本地状态。支付系统不能只依赖支付宝交易状态，还需要在本地维护支付订单、支付流水和退款记录，用于幂等控制、状态补偿、对账、问题排查和业务状态同步。

本章节默认使用 MySQL 8.x，金额字段统一使用 `decimal(18,2)`，订单号类字段统一使用 `varchar(64)`，状态类字段统一使用 `varchar(32)`。生产环境建议根据实际业务量对订单号、状态、创建时间等字段建立索引。

### 支付订单表

支付订单表用于保存一笔业务订单对应的支付主记录。该表是支付模块的核心表，负责记录商户订单号、支付宝交易号、支付金额、支付状态、支付时间、退款金额和通知状态等信息。

一笔业务订单通常对应一笔支付订单。如果业务允许重新发起支付，也可以设计为一笔业务订单对应多笔支付订单，但必须保证每次传给支付宝的 `out_trade_no` 全局唯一。

文件位置：`sql/pay_order.sql`

```sql
CREATE TABLE pay_order (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',

    pay_order_no VARCHAR(64) NOT NULL COMMENT '本地支付订单号',
    biz_order_no VARCHAR(64) NOT NULL COMMENT '业务订单号',
    out_trade_no VARCHAR(64) NOT NULL COMMENT '支付宝商户订单号',
    trade_no VARCHAR(64) DEFAULT NULL COMMENT '支付宝交易号',

    app_id VARCHAR(64) NOT NULL COMMENT '支付宝应用ID',
    pay_channel VARCHAR(32) NOT NULL DEFAULT 'ALIPAY' COMMENT '支付渠道：ALIPAY',
    pay_scene VARCHAR(32) NOT NULL COMMENT '支付场景：PC、WAP、APP',

    subject VARCHAR(256) NOT NULL COMMENT '订单标题',
    body VARCHAR(512) DEFAULT NULL COMMENT '订单描述',

    total_amount DECIMAL(18,2) NOT NULL COMMENT '订单支付金额',
    paid_amount DECIMAL(18,2) DEFAULT NULL COMMENT '支付宝实收金额',
    refund_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '累计退款金额',
    currency VARCHAR(16) NOT NULL DEFAULT 'CNY' COMMENT '币种',

    buyer_id VARCHAR(64) DEFAULT NULL COMMENT '买家支付宝用户ID',
    buyer_logon_id VARCHAR(128) DEFAULT NULL COMMENT '买家支付宝账号',

    status VARCHAR(32) NOT NULL COMMENT '本地支付状态：WAIT_PAY、PAYING、PAY_SUCCESS、PAY_CLOSED、PAY_FAILED、REFUNDING、REFUND_SUCCESS',
    trade_status VARCHAR(32) DEFAULT NULL COMMENT '支付宝交易状态：WAIT_BUYER_PAY、TRADE_SUCCESS、TRADE_FINISHED、TRADE_CLOSED',

    expire_time DATETIME DEFAULT NULL COMMENT '支付过期时间',
    pay_time DATETIME DEFAULT NULL COMMENT '支付成功时间',
    close_time DATETIME DEFAULT NULL COMMENT '交易关闭时间',
    notify_time DATETIME DEFAULT NULL COMMENT '最近一次异步通知时间',
    last_query_time DATETIME DEFAULT NULL COMMENT '最近一次主动查询时间',

    notify_count INT NOT NULL DEFAULT 0 COMMENT '异步通知次数',
    query_count INT NOT NULL DEFAULT 0 COMMENT '主动查询次数',

    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    remark VARCHAR(512) DEFAULT NULL COMMENT '备注',

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否，1是',

    PRIMARY KEY (id),
    UNIQUE KEY uk_pay_order_no (pay_order_no),
    UNIQUE KEY uk_out_trade_no (out_trade_no),
    KEY idx_biz_order_no (biz_order_no),
    KEY idx_trade_no (trade_no),
    KEY idx_status_expire_time (status, expire_time),
    KEY idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付订单表';
```

字段设计说明：

| 字段           | 说明                                               |
| -------------- | -------------------------------------------------- |
| `pay_order_no` | 本系统内部支付订单号，供接口和业务模块使用         |
| `biz_order_no` | 业务订单号，例如商品订单号、会员订单号             |
| `out_trade_no` | 传给支付宝的商户订单号，必须全局唯一               |
| `trade_no`     | 支付宝交易号，支付成功或查询成功后回填             |
| `pay_scene`    | 支付场景，区分电脑网站支付、手机网站支付、App 支付 |
| `status`       | 本地支付状态，用于业务判断                         |
| `trade_status` | 支付宝交易状态，用于记录支付宝原始状态             |
| `notify_count` | 记录支付宝异步通知次数，便于排查重复回调           |
| `query_count`  | 记录主动查询次数，便于控制补偿频率                 |
| `version`      | 用于订单状态更新时的乐观锁控制                     |

支付订单表中最关键的是 `out_trade_no`。支付宝支付、查询、退款和异步通知都会使用该字段关联本地订单。支付宝电脑网站支付 API 列表中包含 `alipay.trade.page.pay`、`alipay.trade.query`、`alipay.trade.refund`、`alipay.trade.fastpay.refund.query` 等接口，后续核心实现会围绕这些接口展开。([open.fliggy.com](https://open.fliggy.com/docs/doc.htm?articleId=105900&docType=1&treeId=270&utm_source=chatgpt.com))

### 支付流水表

支付流水表用于记录每次与支付宝交互的请求和响应，包括支付发起、异步通知、主动查询、退款申请和退款查询。该表不直接决定订单状态，但对排查支付问题、审计接口调用和追踪异常非常重要。

支付流水建议只追加、不覆盖。每次调用支付宝接口或接收支付宝通知，都写入一条流水记录。

文件位置：`sql/pay_flow.sql`

```sql
CREATE TABLE pay_flow (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',

    flow_no VARCHAR(64) NOT NULL COMMENT '支付流水号',
    pay_order_no VARCHAR(64) DEFAULT NULL COMMENT '本地支付订单号',
    refund_no VARCHAR(64) DEFAULT NULL COMMENT '本地退款单号',

    out_trade_no VARCHAR(64) DEFAULT NULL COMMENT '支付宝商户订单号',
    trade_no VARCHAR(64) DEFAULT NULL COMMENT '支付宝交易号',
    out_request_no VARCHAR(64) DEFAULT NULL COMMENT '退款请求号',

    flow_type VARCHAR(32) NOT NULL COMMENT '流水类型：PAY、NOTIFY、RETURN、QUERY、REFUND、REFUND_QUERY',
    request_body LONGTEXT DEFAULT NULL COMMENT '请求参数或通知参数',
    response_body LONGTEXT DEFAULT NULL COMMENT '响应内容',

    success TINYINT NOT NULL DEFAULT 0 COMMENT '是否成功：0否，1是',
    error_code VARCHAR(64) DEFAULT NULL COMMENT '错误码',
    error_message VARCHAR(512) DEFAULT NULL COMMENT '错误信息',

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    PRIMARY KEY (id),
    UNIQUE KEY uk_flow_no (flow_no),
    KEY idx_pay_order_no (pay_order_no),
    KEY idx_refund_no (refund_no),
    KEY idx_out_trade_no (out_trade_no),
    KEY idx_trade_no (trade_no),
    KEY idx_flow_type_created_at (flow_type, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付流水表';
```

流水类型建议如下：

| 类型           | 说明             |
| -------------- | ---------------- |
| `PAY`          | 发起支付请求     |
| `NOTIFY`       | 支付宝异步通知   |
| `RETURN`       | 支付宝同步回跳   |
| `QUERY`        | 主动查询支付状态 |
| `REFUND`       | 发起退款请求     |
| `REFUND_QUERY` | 查询退款结果     |

支付流水表中的 `request_body` 和 `response_body` 可能包含较多内容，建议使用 `LONGTEXT` 保存。生产环境中应避免保存敏感私钥、完整签名原文或用户隐私信息；如果存在敏感字段，写入前需要脱敏。

### 退款记录表

退款记录表用于保存每次退款申请。由于支付宝支持一笔交易多次退款，所以退款记录必须独立于支付订单表设计。每次退款都需要唯一的 `out_request_no`，用于标识一次退款请求。

支付宝退款接口要求同一笔交易多次退款时提交不同的退款请求号；如果一笔退款失败后重新提交，重试时退款请求号不能变更，避免重复退款；同一笔交易累计退款金额不能超过原始交易总金额。([Postman](https://www.postman.com/alipay-dev/apiv3/request/nfm74kj/?utm_source=chatgpt.com))

文件位置：`sql/pay_refund.sql`

```sql
CREATE TABLE pay_refund (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',

    refund_no VARCHAR(64) NOT NULL COMMENT '本地退款单号',
    biz_refund_no VARCHAR(64) NOT NULL COMMENT '业务退款单号',

    pay_order_no VARCHAR(64) NOT NULL COMMENT '本地支付订单号',
    biz_order_no VARCHAR(64) NOT NULL COMMENT '业务订单号',
    out_trade_no VARCHAR(64) NOT NULL COMMENT '支付宝商户订单号',
    trade_no VARCHAR(64) DEFAULT NULL COMMENT '支付宝交易号',

    out_request_no VARCHAR(64) NOT NULL COMMENT '支付宝退款请求号',
    refund_amount DECIMAL(18,2) NOT NULL COMMENT '本次退款金额',
    refund_reason VARCHAR(256) DEFAULT NULL COMMENT '退款原因',

    status VARCHAR(32) NOT NULL COMMENT '退款状态：REFUND_INIT、REFUNDING、REFUND_SUCCESS、REFUND_FAILED、REFUND_CLOSED',
    alipay_refund_amount DECIMAL(18,2) DEFAULT NULL COMMENT '支付宝返回退款金额',
    refund_time DATETIME DEFAULT NULL COMMENT '退款成功时间',

    query_count INT NOT NULL DEFAULT 0 COMMENT '退款查询次数',
    last_query_time DATETIME DEFAULT NULL COMMENT '最近一次退款查询时间',

    fail_code VARCHAR(64) DEFAULT NULL COMMENT '失败编码',
    fail_message VARCHAR(512) DEFAULT NULL COMMENT '失败原因',
    raw_response LONGTEXT DEFAULT NULL COMMENT '支付宝退款响应原文',

    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    remark VARCHAR(512) DEFAULT NULL COMMENT '备注',

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否，1是',

    PRIMARY KEY (id),
    UNIQUE KEY uk_refund_no (refund_no),
    UNIQUE KEY uk_biz_refund_no (biz_refund_no),
    UNIQUE KEY uk_out_request_no (out_request_no),
    KEY idx_pay_order_no (pay_order_no),
    KEY idx_out_trade_no (out_trade_no),
    KEY idx_status_created_at (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='退款记录表';
```

字段设计说明：

| 字段                   | 说明                             |
| ---------------------- | -------------------------------- |
| `refund_no`            | 本地退款单号                     |
| `biz_refund_no`        | 业务退款单号，防止业务侧重复申请 |
| `out_request_no`       | 传给支付宝的退款请求号，必须唯一 |
| `refund_amount`        | 本次退款金额                     |
| `status`               | 本地退款状态                     |
| `alipay_refund_amount` | 支付宝返回的退款金额             |
| `raw_response`         | 支付宝退款响应原文，便于排查问题 |

退款状态建议独立维护，不要只在支付订单表中记录一个退款状态。支付订单表只需要保存累计退款金额和聚合后的支付状态，退款明细由退款记录表承载。

## 核心功能实现

核心功能实现用于封装支付宝支付、查询、退款、回调验签和状态同步逻辑。支付宝开放平台网页与移动应用能力支持在网页、移动端等场景唤起支付宝完成支付，并提供开发配置和开放 API 接入能力。([open.alipay.com](https://open.alipay.com/module/webApp?utm_source=chatgpt.com))

下面示例默认你已经完成前面章节中的 `AlipayProperties` 和 `AlipayClient` 初始化，并使用 MyBatis-Plus 操作 `pay_order`、`pay_flow`、`pay_refund` 三张表。

推荐文件结构如下：

```text
src/main/java/io/github/atengk/pay/
├── constant/
│   └── AlipayConstant.java
├── dto/
│   ├── AlipayPayRequest.java
│   ├── AlipayPayResult.java
│   ├── AlipayRefundRequest.java
│   └── AlipayRefundQueryRequest.java
├── enums/
│   ├── PaySceneEnum.java
│   ├── PayStatusEnum.java
│   └── RefundStatusEnum.java
├── service/
│   └── AlipayTradeService.java
└── service/impl/
    └── AlipayTradeServiceImpl.java
```

### 电脑网站支付

电脑网站支付适用于 PC 浏览器。服务端调用 `alipay.trade.page.pay` 后返回一段 HTML 表单，前端将该表单写入页面并自动跳转到支付宝收银台。支付宝电脑网站支付 API 列表中将 `alipay.trade.page.pay` 描述为统一收单下单并支付页面接口。([open.fliggy.com](https://open.fliggy.com/docs/doc.htm?articleId=105900&docType=1&treeId=270&utm_source=chatgpt.com))

电脑网站支付的核心处理步骤如下：

```text
查询本地支付订单
    ↓
校验订单状态是否允许支付
    ↓
构造 AlipayTradePagePayRequest
    ↓
设置 notifyUrl 和 returnUrl
    ↓
设置 productCode = FAST_INSTANT_TRADE_PAY
    ↓
调用 alipayClient.pageExecute(request)
    ↓
保存支付流水
    ↓
返回 HTML 表单
```

支付场景枚举如下。

文件位置：`src/main/java/io/github/atengk/pay/enums/PaySceneEnum.java`

```java
package io.github.atengk.pay.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 支付场景枚举
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Getter
@AllArgsConstructor
public enum PaySceneEnum {

    /**
     * 电脑网站支付
     */
    PC("PC", "电脑网站支付"),

    /**
     * 手机网站支付
     */
    WAP("WAP", "手机网站支付"),

    /**
     * App 支付
     */
    APP("APP", "App 支付");

    private final String code;
    private final String desc;
}
```

支付状态枚举如下。

文件位置：`src/main/java/io/github/atengk/pay/enums/PayStatusEnum.java`

```java
package io.github.atengk.pay.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 支付状态枚举
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Getter
@AllArgsConstructor
public enum PayStatusEnum {

    /**
     * 待支付
     */
    WAIT_PAY("WAIT_PAY", "待支付"),

    /**
     * 支付中
     */
    PAYING("PAYING", "支付中"),

    /**
     * 支付成功
     */
    PAY_SUCCESS("PAY_SUCCESS", "支付成功"),

    /**
     * 交易关闭
     */
    PAY_CLOSED("PAY_CLOSED", "交易关闭"),

    /**
     * 支付失败
     */
    PAY_FAILED("PAY_FAILED", "支付失败"),

    /**
     * 退款中
     */
    REFUNDING("REFUNDING", "退款中"),

    /**
     * 退款成功
     */
    REFUND_SUCCESS("REFUND_SUCCESS", "退款成功");

    private final String code;
    private final String desc;
}
```

支付请求 DTO 如下。

文件位置：`src/main/java/io/github/atengk/pay/dto/AlipayPayRequest.java`

```java
package io.github.atengk.pay.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 支付宝支付请求
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Data
public class AlipayPayRequest {

    /**
     * 本地支付订单号
     */
    @NotBlank(message = "支付订单号不能为空")
    private String payOrderNo;

    /**
     * 支付场景：PC、WAP、APP
     */
    @NotBlank(message = "支付场景不能为空")
    private String payScene;

    /**
     * 手机网站支付退出地址
     */
    private String quitUrl;
}
```

支付返回 DTO 如下。

文件位置：`src/main/java/io/github/atengk/pay/dto/AlipayPayResult.java`

```java
package io.github.atengk.pay.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 支付宝支付返回结果
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Data
@Builder
public class AlipayPayResult {

    /**
     * 本地支付订单号
     */
    private String payOrderNo;

    /**
     * 支付宝商户订单号
     */
    private String outTradeNo;

    /**
     * 支付场景
     */
    private String payScene;

    /**
     * 返回内容：PC/WAP 为 HTML 表单，APP 为订单字符串
     */
    private String body;
}
```

电脑网站支付核心方法在后面的 `AlipayTradeServiceImpl` 中统一给出，方法名为 `pagePay`。

### 手机网站支付

手机网站支付适用于 H5、移动浏览器和需要从移动网页唤起支付宝的场景。服务端调用 `alipay.trade.wap.pay` 后返回 HTML 表单，移动端浏览器跳转到支付宝收银台或唤起支付宝 App。公开文档中 `alipay.trade.wap.pay` 的接口名称为手机网站支付接口，公共参数包含 `return_url`、`notify_url`、`sign_type`、`biz_content` 等。([飞猪](https://open.alitrip.com/docs/api.htm?apiId=1056&docType=4&utm_source=chatgpt.com))

手机网站支付的核心处理步骤如下：

```text
查询本地支付订单
    ↓
校验订单状态是否允许支付
    ↓
构造 AlipayTradeWapPayRequest
    ↓
设置 notifyUrl、returnUrl、quitUrl
    ↓
设置 productCode = QUICK_WAP_WAY
    ↓
调用 alipayClient.pageExecute(request)
    ↓
保存支付流水
    ↓
返回 HTML 表单
```

手机网站支付与电脑网站支付的主要差异是 `productCode` 和终端使用方式不同。电脑网站支付使用 `FAST_INSTANT_TRADE_PAY`，手机网站支付使用 `QUICK_WAP_WAY`。

### App 支付

App 支付适用于 Android 和 iOS 原生应用。服务端调用 `alipay.trade.app.pay` 后返回签名后的订单字符串，App 端拿到订单字符串后调用支付宝移动端 SDK 发起支付。

App 支付的核心处理步骤如下：

```text
App 请求后端发起支付
    ↓
后端查询本地支付订单
    ↓
校验订单状态
    ↓
构造 AlipayTradeAppPayRequest
    ↓
设置 productCode = QUICK_MSECURITY_PAY
    ↓
调用 alipayClient.sdkExecute(request)
    ↓
保存支付流水
    ↓
返回订单字符串给 App
```

App 支付不能把应用私钥下发到客户端。签名必须在服务端完成，客户端只负责使用后端返回的订单字符串调用支付宝 SDK。

### 支付宝异步通知

支付宝异步通知是服务端确认支付结果的核心依据。处理异步通知时必须先验签，再校验 `app_id`、`out_trade_no`、`total_amount`、交易状态等关键参数，最后更新本地支付订单。

异步通知处理流程如下：

```text
接收支付宝 POST 通知
    ↓
提取全部通知参数
    ↓
使用支付宝公钥证书验签
    ↓
校验 app_id
    ↓
查询本地支付订单
    ↓
校验金额
    ↓
判断是否已处理
    ↓
根据 trade_status 更新订单
    ↓
写入支付流水
    ↓
返回 success
```

异步通知接口必须返回纯文本 `success`，表示商户系统已经成功处理通知。业务处理失败时不要返回 `success`，这样支付宝后续可以继续重试通知。

### 支付宝同步回跳

支付宝同步回跳用于用户支付完成后的页面跳转，不建议作为最终支付成功依据。同步回跳可以展示“支付处理中”或“支付成功，请稍后刷新”的页面，也可以触发一次主动查询，但订单最终状态仍应以异步通知或主动查询为准。

同步回跳处理流程如下：

```text
用户完成支付
    ↓
支付宝跳转 returnUrl
    ↓
后端接收同步参数
    ↓
记录同步回跳流水
    ↓
按需验签
    ↓
跳转前端支付结果页
```

同步回跳一般用于改善用户体验，不应在未校验支付状态的情况下直接发放权益、扣减库存或确认订单完成。

### 支付订单查询

支付订单查询用于补偿异步通知丢失、用户支付后页面中断、本地状态长时间处于支付中等情况。服务端通过 `alipay.trade.query` 查询支付宝交易状态，再同步更新本地支付订单。

查询处理流程如下：

```text
查询本地支付订单
    ↓
如果已经是终态，直接返回本地状态
    ↓
未终态则调用 alipay.trade.query
    ↓
解析 trade_status
    ↓
更新本地支付订单
    ↓
写入查询流水
    ↓
返回最新状态
```

支付查询不应无限频繁调用。生产系统建议对 `WAIT_PAY` 和 `PAYING` 状态订单设置查询间隔，例如 30 秒或 1 分钟以上。

### 支付退款

支付退款用于已支付订单的全部或部分退款。退款前必须校验原支付订单是否存在、是否支付成功、退款金额是否合法、是否重复退款、累计退款金额是否超过支付金额。

退款处理流程如下：

```text
业务系统提交退款申请
    ↓
查询原支付订单
    ↓
校验订单已支付
    ↓
校验退款金额和累计退款金额
    ↓
创建本地退款记录
    ↓
调用 alipay.trade.refund
    ↓
更新退款记录状态
    ↓
更新支付订单累计退款金额
    ↓
写入退款流水
```

退款请求号 `out_request_no` 必须稳定。相同业务退款单重试时应使用同一个 `out_request_no`，不能每次重试都生成新请求号。

退款请求 DTO 如下。

文件位置：`src/main/java/io/github/atengk/pay/dto/AlipayRefundRequest.java`

```java
package io.github.atengk.pay.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 支付宝退款请求
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Data
public class AlipayRefundRequest {

    /**
     * 本地支付订单号
     */
    @NotBlank(message = "支付订单号不能为空")
    private String payOrderNo;

    /**
     * 业务退款单号
     */
    @NotBlank(message = "业务退款单号不能为空")
    private String bizRefundNo;

    /**
     * 退款金额
     */
    @NotNull(message = "退款金额不能为空")
    @DecimalMin(value = "0.01", message = "退款金额必须大于0")
    private BigDecimal refundAmount;

    /**
     * 退款原因
     */
    private String refundReason;
}
```

退款状态枚举如下。

文件位置：`src/main/java/io/github/atengk/pay/enums/RefundStatusEnum.java`

```java
package io.github.atengk.pay.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 退款状态枚举
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Getter
@AllArgsConstructor
public enum RefundStatusEnum {

    /**
     * 退款初始化
     */
    REFUND_INIT("REFUND_INIT", "退款初始化"),

    /**
     * 退款处理中
     */
    REFUNDING("REFUNDING", "退款处理中"),

    /**
     * 退款成功
     */
    REFUND_SUCCESS("REFUND_SUCCESS", "退款成功"),

    /**
     * 退款失败
     */
    REFUND_FAILED("REFUND_FAILED", "退款失败"),

    /**
     * 退款关闭
     */
    REFUND_CLOSED("REFUND_CLOSED", "退款关闭");

    private final String code;
    private final String desc;
}
```

### 退款结果查询

退款结果查询用于确认退款是否执行成功。支付宝退款查询接口用于查询通过 `alipay.trade.refund` 提交的退款请求是否执行成功。([Postman](https://www.postman.com/alipay-dev/apiv3/request/y87u527/?utm_source=chatgpt.com))

退款查询流程如下：

```text
查询本地退款记录
    ↓
如果退款已经成功，直接返回
    ↓
未成功则调用 alipay.trade.fastpay.refund.query
    ↓
解析支付宝退款结果
    ↓
更新本地退款记录
    ↓
写入退款查询流水
    ↓
返回最新退款状态
```

退款查询请求 DTO 如下。

文件位置：`src/main/java/io/github/atengk/pay/dto/AlipayRefundQueryRequest.java`

```java
package io.github.atengk.pay.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 支付宝退款查询请求
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Data
public class AlipayRefundQueryRequest {

    /**
     * 本地退款单号
     */
    @NotBlank(message = "退款单号不能为空")
    private String refundNo;
}
```

下面给出核心 Service 接口和实现。示例默认项目中已经存在 `PayOrder`、`PayRefund`、`PayFlow` 实体类，以及对应的 MyBatis-Plus Mapper：`PayOrderMapper`、`PayRefundMapper`、`PayFlowMapper`。实体字段与前文 SQL 字段保持一致。

文件位置：`src/main/java/io/github/atengk/pay/service/AlipayTradeService.java`

该接口封装支付宝支付、回调、查询和退款能力，Controller 层可以直接调用。

```java
package io.github.atengk.pay.service;

import io.github.atengk.pay.dto.AlipayPayRequest;
import io.github.atengk.pay.dto.AlipayPayResult;
import io.github.atengk.pay.dto.AlipayRefundQueryRequest;
import io.github.atengk.pay.dto.AlipayRefundRequest;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 支付宝交易服务
 *
 * @author Ateng
 * @since 2026-05-07
 */
public interface AlipayTradeService {

    /**
     * 发起电脑网站支付
     *
     * @param request 支付请求
     * @return 支付表单
     */
    AlipayPayResult pagePay(AlipayPayRequest request);

    /**
     * 发起手机网站支付
     *
     * @param request 支付请求
     * @return 支付表单
     */
    AlipayPayResult wapPay(AlipayPayRequest request);

    /**
     * 发起 App 支付
     *
     * @param request 支付请求
     * @return 订单字符串
     */
    AlipayPayResult appPay(AlipayPayRequest request);

    /**
     * 处理支付宝异步通知
     *
     * @param request HTTP 请求
     * @return 支付宝通知响应
     */
    String handleNotify(HttpServletRequest request);

    /**
     * 处理支付宝同步回跳
     *
     * @param request HTTP 请求
     * @return 前端跳转地址
     */
    String handleReturn(HttpServletRequest request);

    /**
     * 查询支付订单状态
     *
     * @param payOrderNo 本地支付订单号
     * @return 最新支付状态
     */
    String queryPayStatus(String payOrderNo);

    /**
     * 发起退款
     *
     * @param request 退款请求
     * @return 退款状态
     */
    String refund(AlipayRefundRequest request);

    /**
     * 查询退款结果
     *
     * @param request 退款查询请求
     * @return 退款状态
     */
    String queryRefund(AlipayRefundQueryRequest request);
}
```

文件位置：`src/main/java/io/github/atengk/pay/service/impl/AlipayTradeServiceImpl.java`

该实现类封装支付宝核心能力，包括 PC 支付、WAP 支付、App 支付、异步通知、同步回跳、支付查询、退款和退款查询。

```java
package io.github.atengk.pay.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.JakartaServletUtil;
import cn.hutool.json.JSONUtil;
import com.alipay.api.AlipayClient;
import com.alipay.api.AlipayResponse;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradeAppPayRequest;
import com.alipay.api.request.AlipayTradeFastpayRefundQueryRequest;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.request.AlipayTradeWapPayRequest;
import com.alipay.api.response.AlipayTradeFastpayRefundQueryResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import io.github.atengk.pay.config.AlipayProperties;
import io.github.atengk.pay.dto.AlipayPayRequest;
import io.github.atengk.pay.dto.AlipayPayResult;
import io.github.atengk.pay.dto.AlipayRefundQueryRequest;
import io.github.atengk.pay.dto.AlipayRefundRequest;
import io.github.atengk.pay.entity.PayFlow;
import io.github.atengk.pay.entity.PayOrder;
import io.github.atengk.pay.entity.PayRefund;
import io.github.atengk.pay.enums.PaySceneEnum;
import io.github.atengk.pay.enums.PayStatusEnum;
import io.github.atengk.pay.enums.RefundStatusEnum;
import io.github.atengk.pay.mapper.PayFlowMapper;
import io.github.atengk.pay.mapper.PayOrderMapper;
import io.github.atengk.pay.mapper.PayRefundMapper;
import io.github.atengk.pay.service.AlipayTradeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;
import java.util.Objects;

/**
 * 支付宝交易服务实现
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlipayTradeServiceImpl implements AlipayTradeService {

    private static final String ALIPAY_SUCCESS = "success";
    private static final String ALIPAY_FAIL = "fail";

    private static final String TRADE_SUCCESS = "TRADE_SUCCESS";
    private static final String TRADE_FINISHED = "TRADE_FINISHED";
    private static final String TRADE_CLOSED = "TRADE_CLOSED";
    private static final String WAIT_BUYER_PAY = "WAIT_BUYER_PAY";

    private final AlipayClient alipayClient;
    private final AlipayProperties alipayProperties;
    private final PayOrderMapper payOrderMapper;
    private final PayRefundMapper payRefundMapper;
    private final PayFlowMapper payFlowMapper;

    /**
     * 发起电脑网站支付
     *
     * @param request 支付请求
     * @return 支付表单
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AlipayPayResult pagePay(AlipayPayRequest request) {
        PayOrder payOrder = getPayOrderForPay(request.getPayOrderNo());

        try {
            AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();
            alipayRequest.setNotifyUrl(alipayProperties.getNotifyUrl());
            alipayRequest.setReturnUrl(alipayProperties.getReturnUrl());

            Map<String, Object> bizContent = MapUtil.<String, Object>builder()
                    .put("out_trade_no", payOrder.getOutTradeNo())
                    .put("total_amount", formatAmount(payOrder.getTotalAmount()))
                    .put("subject", payOrder.getSubject())
                    .put("body", payOrder.getBody())
                    .put("product_code", "FAST_INSTANT_TRADE_PAY")
                    .put("timeout_express", "30m")
                    .build();

            alipayRequest.setBizContent(JSONUtil.toJsonStr(bizContent));
            String body = alipayClient.pageExecute(alipayRequest).getBody();

            updatePaying(payOrder, PaySceneEnum.PC.getCode());
            saveFlow(payOrder, null, "PAY", bizContent, body, true, null, null);

            log.info("电脑网站支付发起成功，支付订单号：{}，商户订单号：{}", payOrder.getPayOrderNo(), payOrder.getOutTradeNo());
            return buildPayResult(payOrder, PaySceneEnum.PC.getCode(), body);
        } catch (Exception e) {
            saveFlow(payOrder, null, "PAY", request, null, false, null, e.getMessage());
            log.error("电脑网站支付发起失败，支付订单号：{}", request.getPayOrderNo(), e);
            throw new IllegalStateException("电脑网站支付发起失败");
        }
    }

    /**
     * 发起手机网站支付
     *
     * @param request 支付请求
     * @return 支付表单
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AlipayPayResult wapPay(AlipayPayRequest request) {
        PayOrder payOrder = getPayOrderForPay(request.getPayOrderNo());

        try {
            AlipayTradeWapPayRequest alipayRequest = new AlipayTradeWapPayRequest();
            alipayRequest.setNotifyUrl(alipayProperties.getNotifyUrl());
            alipayRequest.setReturnUrl(alipayProperties.getReturnUrl());

            Map<String, Object> bizContent = MapUtil.<String, Object>builder()
                    .put("out_trade_no", payOrder.getOutTradeNo())
                    .put("total_amount", formatAmount(payOrder.getTotalAmount()))
                    .put("subject", payOrder.getSubject())
                    .put("body", payOrder.getBody())
                    .put("product_code", "QUICK_WAP_WAY")
                    .put("quit_url", StrUtil.blankToDefault(request.getQuitUrl(), alipayProperties.getReturnUrl()))
                    .put("timeout_express", "30m")
                    .build();

            alipayRequest.setBizContent(JSONUtil.toJsonStr(bizContent));
            String body = alipayClient.pageExecute(alipayRequest).getBody();

            updatePaying(payOrder, PaySceneEnum.WAP.getCode());
            saveFlow(payOrder, null, "PAY", bizContent, body, true, null, null);

            log.info("手机网站支付发起成功，支付订单号：{}，商户订单号：{}", payOrder.getPayOrderNo(), payOrder.getOutTradeNo());
            return buildPayResult(payOrder, PaySceneEnum.WAP.getCode(), body);
        } catch (Exception e) {
            saveFlow(payOrder, null, "PAY", request, null, false, null, e.getMessage());
            log.error("手机网站支付发起失败，支付订单号：{}", request.getPayOrderNo(), e);
            throw new IllegalStateException("手机网站支付发起失败");
        }
    }

    /**
     * 发起 App 支付
     *
     * @param request 支付请求
     * @return 订单字符串
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AlipayPayResult appPay(AlipayPayRequest request) {
        PayOrder payOrder = getPayOrderForPay(request.getPayOrderNo());

        try {
            AlipayTradeAppPayRequest alipayRequest = new AlipayTradeAppPayRequest();
            alipayRequest.setNotifyUrl(alipayProperties.getNotifyUrl());

            Map<String, Object> bizContent = MapUtil.<String, Object>builder()
                    .put("out_trade_no", payOrder.getOutTradeNo())
                    .put("total_amount", formatAmount(payOrder.getTotalAmount()))
                    .put("subject", payOrder.getSubject())
                    .put("body", payOrder.getBody())
                    .put("product_code", "QUICK_MSECURITY_PAY")
                    .put("timeout_express", "30m")
                    .build();

            alipayRequest.setBizContent(JSONUtil.toJsonStr(bizContent));
            String orderString = alipayClient.sdkExecute(alipayRequest).getBody();

            updatePaying(payOrder, PaySceneEnum.APP.getCode());
            saveFlow(payOrder, null, "PAY", bizContent, orderString, true, null, null);

            log.info("App 支付发起成功，支付订单号：{}，商户订单号：{}", payOrder.getPayOrderNo(), payOrder.getOutTradeNo());
            return buildPayResult(payOrder, PaySceneEnum.APP.getCode(), orderString);
        } catch (Exception e) {
            saveFlow(payOrder, null, "PAY", request, null, false, null, e.getMessage());
            log.error("App 支付发起失败，支付订单号：{}", request.getPayOrderNo(), e);
            throw new IllegalStateException("App 支付发起失败");
        }
    }

    /**
     * 处理支付宝异步通知
     *
     * @param request HTTP 请求
     * @return 支付宝通知响应
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String handleNotify(HttpServletRequest request) {
        Map<String, String> params = JakartaServletUtil.getParamMap(request);

        try {
            boolean signVerified = AlipaySignature.rsaCertCheckV1(
                    params,
                    alipayProperties.getAlipayCertPath(),
                    alipayProperties.getCharset(),
                    alipayProperties.getSignType()
            );

            if (!signVerified) {
                log.warn("支付宝异步通知验签失败，通知参数：{}", JSONUtil.toJsonStr(params));
                saveFlow(null, null, "NOTIFY", params, null, false, "SIGN_ERROR", "支付宝异步通知验签失败");
                return ALIPAY_FAIL;
            }

            String appId = params.get("app_id");
            String outTradeNo = params.get("out_trade_no");
            String tradeNo = params.get("trade_no");
            String tradeStatus = params.get("trade_status");
            String totalAmount = params.get("total_amount");

            if (!StrUtil.equals(appId, alipayProperties.getAppId())) {
                log.warn("支付宝异步通知 appId 不匹配，通知 appId：{}，系统 appId：{}", appId, alipayProperties.getAppId());
                return ALIPAY_FAIL;
            }

            PayOrder payOrder = payOrderMapper.selectByOutTradeNo(outTradeNo);
            if (Objects.isNull(payOrder)) {
                log.warn("支付宝异步通知未找到本地支付订单，商户订单号：{}", outTradeNo);
                return ALIPAY_FAIL;
            }

            if (!amountEquals(payOrder.getTotalAmount(), totalAmount)) {
                log.warn("支付宝异步通知金额不一致，支付订单号：{}，本地金额：{}，通知金额：{}",
                        payOrder.getPayOrderNo(), payOrder.getTotalAmount(), totalAmount);
                saveFlow(payOrder, null, "NOTIFY", params, null, false, "AMOUNT_ERROR", "支付宝异步通知金额不一致");
                return ALIPAY_FAIL;
            }

            saveFlow(payOrder, null, "NOTIFY", params, null, true, null, null);

            if (PayStatusEnum.PAY_SUCCESS.getCode().equals(payOrder.getStatus())) {
                log.info("支付宝异步通知重复处理，支付订单号：{}", payOrder.getPayOrderNo());
                return ALIPAY_SUCCESS;
            }

            if (TRADE_SUCCESS.equals(tradeStatus) || TRADE_FINISHED.equals(tradeStatus)) {
                payOrder.setTradeNo(tradeNo);
                payOrder.setTradeStatus(tradeStatus);
                payOrder.setPaidAmount(new BigDecimal(totalAmount));
                payOrder.setBuyerId(params.get("buyer_id"));
                payOrder.setBuyerLogonId(params.get("buyer_logon_id"));
                payOrder.setPayTime(parseDate(params.get("gmt_payment")));
                payOrder.setNotifyTime(new Date());
                payOrder.setNotifyCount(payOrder.getNotifyCount() + 1);
                payOrder.setStatus(PayStatusEnum.PAY_SUCCESS.getCode());
                payOrderMapper.updateById(payOrder);

                log.info("支付宝异步通知支付成功，支付订单号：{}，支付宝交易号：{}", payOrder.getPayOrderNo(), tradeNo);

                // 这里建议发布支付成功事件，由业务订单模块消费
                // applicationEventPublisher.publishEvent(new PaySuccessEvent(payOrder.getBizOrderNo()));
                return ALIPAY_SUCCESS;
            }

            if (TRADE_CLOSED.equals(tradeStatus)) {
                payOrder.setTradeNo(tradeNo);
                payOrder.setTradeStatus(tradeStatus);
                payOrder.setCloseTime(new Date());
                payOrder.setNotifyTime(new Date());
                payOrder.setNotifyCount(payOrder.getNotifyCount() + 1);
                payOrder.setStatus(PayStatusEnum.PAY_CLOSED.getCode());
                payOrderMapper.updateById(payOrder);

                log.info("支付宝异步通知交易关闭，支付订单号：{}", payOrder.getPayOrderNo());
                return ALIPAY_SUCCESS;
            }

            if (WAIT_BUYER_PAY.equals(tradeStatus)) {
                payOrder.setTradeNo(tradeNo);
                payOrder.setTradeStatus(tradeStatus);
                payOrder.setNotifyTime(new Date());
                payOrder.setNotifyCount(payOrder.getNotifyCount() + 1);
                payOrderMapper.updateById(payOrder);

                log.info("支付宝异步通知等待买家付款，支付订单号：{}", payOrder.getPayOrderNo());
                return ALIPAY_SUCCESS;
            }

            log.info("支付宝异步通知收到未处理状态，支付订单号：{}，交易状态：{}", payOrder.getPayOrderNo(), tradeStatus);
            return ALIPAY_SUCCESS;
        } catch (Exception e) {
            log.error("支付宝异步通知处理异常，通知参数：{}", JSONUtil.toJsonStr(params), e);
            return ALIPAY_FAIL;
        }
    }

    /**
     * 处理支付宝同步回跳
     *
     * @param request HTTP 请求
     * @return 前端跳转地址
     */
    @Override
    public String handleReturn(HttpServletRequest request) {
        Map<String, String> params = JakartaServletUtil.getParamMap(request);
        String outTradeNo = params.get("out_trade_no");

        PayOrder payOrder = StrUtil.isBlank(outTradeNo) ? null : payOrderMapper.selectByOutTradeNo(outTradeNo);
        saveFlow(payOrder, null, "RETURN", params, null, true, null, null);

        log.info("支付宝同步回跳处理完成，商户订单号：{}", outTradeNo);

        if (Objects.isNull(payOrder)) {
            return alipayProperties.getReturnUrl();
        }

        return StrUtil.format("{}?payOrderNo={}&status={}",
                alipayProperties.getReturnUrl(),
                payOrder.getPayOrderNo(),
                payOrder.getStatus());
    }

    /**
     * 查询支付订单状态
     *
     * @param payOrderNo 本地支付订单号
     * @return 最新支付状态
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String queryPayStatus(String payOrderNo) {
        PayOrder payOrder = payOrderMapper.selectByPayOrderNo(payOrderNo);
        if (Objects.isNull(payOrder)) {
            throw new IllegalArgumentException("支付订单不存在");
        }

        if (PayStatusEnum.PAY_SUCCESS.getCode().equals(payOrder.getStatus())
                || PayStatusEnum.PAY_CLOSED.getCode().equals(payOrder.getStatus())) {
            return payOrder.getStatus();
        }

        try {
            AlipayTradeQueryRequest alipayRequest = new AlipayTradeQueryRequest();

            Map<String, Object> bizContent = MapUtil.<String, Object>builder()
                    .put("out_trade_no", payOrder.getOutTradeNo())
                    .build();

            alipayRequest.setBizContent(JSONUtil.toJsonStr(bizContent));

            AlipayTradeQueryResponse response = alipayClient.certificateExecute(alipayRequest);
            saveFlow(payOrder, null, "QUERY", bizContent, response.getBody(), response.isSuccess(), response.getCode(), response.getSubMsg());

            payOrder.setLastQueryTime(new Date());
            payOrder.setQueryCount(payOrder.getQueryCount() + 1);

            if (response.isSuccess()) {
                syncPayStatusByQuery(payOrder, response);
            } else {
                payOrderMapper.updateById(payOrder);
                log.warn("支付宝支付查询失败，支付订单号：{}，错误信息：{}", payOrderNo, response.getSubMsg());
            }

            return payOrder.getStatus();
        } catch (Exception e) {
            saveFlow(payOrder, null, "QUERY", payOrder.getOutTradeNo(), null, false, null, e.getMessage());
            log.error("支付宝支付查询异常，支付订单号：{}", payOrderNo, e);
            throw new IllegalStateException("支付宝支付查询异常");
        }
    }

    /**
     * 发起退款
     *
     * @param request 退款请求
     * @return 退款状态
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String refund(AlipayRefundRequest request) {
        PayOrder payOrder = payOrderMapper.selectByPayOrderNo(request.getPayOrderNo());
        if (Objects.isNull(payOrder)) {
            throw new IllegalArgumentException("支付订单不存在");
        }
        if (!PayStatusEnum.PAY_SUCCESS.getCode().equals(payOrder.getStatus())
                && !PayStatusEnum.REFUNDING.getCode().equals(payOrder.getStatus())) {
            throw new IllegalStateException("当前订单状态不允许退款");
        }

        BigDecimal refundedAmount = NumberUtil.nullToZero(payOrder.getRefundAmount());
        BigDecimal availableAmount = payOrder.getTotalAmount().subtract(refundedAmount);
        if (request.getRefundAmount().compareTo(availableAmount) > 0) {
            throw new IllegalArgumentException("退款金额不能大于可退金额");
        }

        PayRefund existRefund = payRefundMapper.selectByBizRefundNo(request.getBizRefundNo());
        if (Objects.nonNull(existRefund)) {
            log.info("业务退款单已存在，直接返回当前状态，业务退款单号：{}", request.getBizRefundNo());
            return existRefund.getStatus();
        }

        PayRefund payRefund = buildRefund(payOrder, request);
        payRefundMapper.insert(payRefund);

        try {
            AlipayTradeRefundRequest alipayRequest = new AlipayTradeRefundRequest();

            Map<String, Object> bizContent = MapUtil.<String, Object>builder()
                    .put("out_trade_no", payOrder.getOutTradeNo())
                    .put("refund_amount", formatAmount(request.getRefundAmount()))
                    .put("refund_reason", StrUtil.blankToDefault(request.getRefundReason(), "用户申请退款"))
                    .put("out_request_no", payRefund.getOutRequestNo())
                    .build();

            alipayRequest.setBizContent(JSONUtil.toJsonStr(bizContent));

            AlipayTradeRefundResponse response = alipayClient.certificateExecute(alipayRequest);
            saveFlow(payOrder, payRefund, "REFUND", bizContent, response.getBody(), response.isSuccess(), response.getCode(), response.getSubMsg());

            if (response.isSuccess()) {
                payRefund.setStatus(RefundStatusEnum.REFUND_SUCCESS.getCode());
                payRefund.setAlipayRefundAmount(new BigDecimal(response.getRefundFee()));
                payRefund.setRefundTime(parseDate(response.getGmtRefundPay()));
                payRefund.setRawResponse(response.getBody());
                payRefundMapper.updateById(payRefund);

                payOrder.setRefundAmount(refundedAmount.add(request.getRefundAmount()));
                payOrder.setStatus(resolvePayStatusAfterRefund(payOrder));
                payOrderMapper.updateById(payOrder);

                log.info("支付宝退款成功，支付订单号：{}，退款单号：{}", payOrder.getPayOrderNo(), payRefund.getRefundNo());
                return RefundStatusEnum.REFUND_SUCCESS.getCode();
            }

            payRefund.setStatus(RefundStatusEnum.REFUND_FAILED.getCode());
            payRefund.setFailCode(response.getSubCode());
            payRefund.setFailMessage(response.getSubMsg());
            payRefund.setRawResponse(response.getBody());
            payRefundMapper.updateById(payRefund);

            log.warn("支付宝退款失败，支付订单号：{}，退款单号：{}，错误信息：{}",
                    payOrder.getPayOrderNo(), payRefund.getRefundNo(), response.getSubMsg());
            return RefundStatusEnum.REFUND_FAILED.getCode();
        } catch (Exception e) {
            payRefund.setStatus(RefundStatusEnum.REFUND_FAILED.getCode());
            payRefund.setFailMessage(e.getMessage());
            payRefundMapper.updateById(payRefund);

            saveFlow(payOrder, payRefund, "REFUND", request, null, false, null, e.getMessage());
            log.error("支付宝退款异常，支付订单号：{}，退款单号：{}", payOrder.getPayOrderNo(), payRefund.getRefundNo(), e);
            throw new IllegalStateException("支付宝退款异常");
        }
    }

    /**
     * 查询退款结果
     *
     * @param request 退款查询请求
     * @return 退款状态
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String queryRefund(AlipayRefundQueryRequest request) {
        PayRefund payRefund = payRefundMapper.selectByRefundNo(request.getRefundNo());
        if (Objects.isNull(payRefund)) {
            throw new IllegalArgumentException("退款记录不存在");
        }

        if (RefundStatusEnum.REFUND_SUCCESS.getCode().equals(payRefund.getStatus())) {
            return payRefund.getStatus();
        }

        PayOrder payOrder = payOrderMapper.selectByPayOrderNo(payRefund.getPayOrderNo());

        try {
            AlipayTradeFastpayRefundQueryRequest alipayRequest = new AlipayTradeFastpayRefundQueryRequest();

            Map<String, Object> bizContent = MapUtil.<String, Object>builder()
                    .put("out_trade_no", payRefund.getOutTradeNo())
                    .put("out_request_no", payRefund.getOutRequestNo())
                    .build();

            alipayRequest.setBizContent(JSONUtil.toJsonStr(bizContent));

            AlipayTradeFastpayRefundQueryResponse response = alipayClient.certificateExecute(alipayRequest);
            saveFlow(payOrder, payRefund, "REFUND_QUERY", bizContent, response.getBody(), response.isSuccess(), response.getCode(), response.getSubMsg());

            payRefund.setQueryCount(payRefund.getQueryCount() + 1);
            payRefund.setLastQueryTime(new Date());
            payRefund.setRawResponse(response.getBody());

            if (response.isSuccess()) {
                payRefund.setStatus(RefundStatusEnum.REFUND_SUCCESS.getCode());
                payRefund.setAlipayRefundAmount(new BigDecimal(response.getRefundAmount()));
                payRefundMapper.updateById(payRefund);

                log.info("支付宝退款查询成功，退款单号：{}", payRefund.getRefundNo());
                return RefundStatusEnum.REFUND_SUCCESS.getCode();
            }

            payRefund.setStatus(RefundStatusEnum.REFUND_FAILED.getCode());
            payRefund.setFailCode(response.getSubCode());
            payRefund.setFailMessage(response.getSubMsg());
            payRefundMapper.updateById(payRefund);

            log.warn("支付宝退款查询失败，退款单号：{}，错误信息：{}", payRefund.getRefundNo(), response.getSubMsg());
            return payRefund.getStatus();
        } catch (Exception e) {
            saveFlow(payOrder, payRefund, "REFUND_QUERY", request, null, false, null, e.getMessage());
            log.error("支付宝退款查询异常，退款单号：{}", request.getRefundNo(), e);
            throw new IllegalStateException("支付宝退款查询异常");
        }
    }

    /**
     * 获取可支付订单
     *
     * @param payOrderNo 支付订单号
     * @return 支付订单
     */
    private PayOrder getPayOrderForPay(String payOrderNo) {
        PayOrder payOrder = payOrderMapper.selectByPayOrderNo(payOrderNo);
        if (Objects.isNull(payOrder)) {
            throw new IllegalArgumentException("支付订单不存在");
        }
        if (PayStatusEnum.PAY_SUCCESS.getCode().equals(payOrder.getStatus())) {
            throw new IllegalStateException("订单已支付，请勿重复支付");
        }
        if (PayStatusEnum.PAY_CLOSED.getCode().equals(payOrder.getStatus())) {
            throw new IllegalStateException("订单已关闭，不能发起支付");
        }
        if (Objects.nonNull(payOrder.getExpireTime()) && payOrder.getExpireTime().before(new Date())) {
            payOrder.setStatus(PayStatusEnum.PAY_CLOSED.getCode());
            payOrder.setCloseTime(new Date());
            payOrderMapper.updateById(payOrder);
            throw new IllegalStateException("订单已过期，请重新下单");
        }
        return payOrder;
    }

    /**
     * 更新支付中状态
     *
     * @param payOrder 支付订单
     * @param payScene 支付场景
     */
    private void updatePaying(PayOrder payOrder, String payScene) {
        payOrder.setPayScene(payScene);
        payOrder.setStatus(PayStatusEnum.PAYING.getCode());
        payOrderMapper.updateById(payOrder);
    }

    /**
     * 根据查询结果同步支付状态
     *
     * @param payOrder 支付订单
     * @param response 支付宝查询响应
     */
    private void syncPayStatusByQuery(PayOrder payOrder, AlipayTradeQueryResponse response) {
        String tradeStatus = response.getTradeStatus();

        payOrder.setTradeNo(response.getTradeNo());
        payOrder.setTradeStatus(tradeStatus);
        payOrder.setBuyerLogonId(response.getBuyerLogonId());

        if (TRADE_SUCCESS.equals(tradeStatus) || TRADE_FINISHED.equals(tradeStatus)) {
            payOrder.setStatus(PayStatusEnum.PAY_SUCCESS.getCode());
            payOrder.setPaidAmount(new BigDecimal(response.getTotalAmount()));
            payOrder.setPayTime(parseDate(response.getSendPayDate()));
        } else if (TRADE_CLOSED.equals(tradeStatus)) {
            payOrder.setStatus(PayStatusEnum.PAY_CLOSED.getCode());
            payOrder.setCloseTime(new Date());
        } else if (WAIT_BUYER_PAY.equals(tradeStatus)) {
            payOrder.setStatus(PayStatusEnum.PAYING.getCode());
        }

        payOrderMapper.updateById(payOrder);
        log.info("支付宝支付查询状态同步完成，支付订单号：{}，交易状态：{}", payOrder.getPayOrderNo(), tradeStatus);
    }

    /**
     * 构建退款记录
     *
     * @param payOrder 支付订单
     * @param request 退款请求
     * @return 退款记录
     */
    private PayRefund buildRefund(PayOrder payOrder, AlipayRefundRequest request) {
        PayRefund payRefund = new PayRefund();
        payRefund.setRefundNo(IdUtil.getSnowflakeNextIdStr());
        payRefund.setBizRefundNo(request.getBizRefundNo());
        payRefund.setPayOrderNo(payOrder.getPayOrderNo());
        payRefund.setBizOrderNo(payOrder.getBizOrderNo());
        payRefund.setOutTradeNo(payOrder.getOutTradeNo());
        payRefund.setTradeNo(payOrder.getTradeNo());
        payRefund.setOutRequestNo(request.getBizRefundNo());
        payRefund.setRefundAmount(request.getRefundAmount());
        payRefund.setRefundReason(StrUtil.blankToDefault(request.getRefundReason(), "用户申请退款"));
        payRefund.setStatus(RefundStatusEnum.REFUNDING.getCode());
        return payRefund;
    }

    /**
     * 根据退款金额计算支付订单状态
     *
     * @param payOrder 支付订单
     * @return 支付订单状态
     */
    private String resolvePayStatusAfterRefund(PayOrder payOrder) {
        if (payOrder.getRefundAmount().compareTo(payOrder.getTotalAmount()) >= 0) {
            return PayStatusEnum.REFUND_SUCCESS.getCode();
        }
        return PayStatusEnum.REFUNDING.getCode();
    }

    /**
     * 保存支付流水
     *
     * @param payOrder 支付订单
     * @param payRefund 退款记录
     * @param flowType 流水类型
     * @param requestBody 请求内容
     * @param responseBody 响应内容
     * @param success 是否成功
     * @param errorCode 错误码
     * @param errorMessage 错误信息
     */
    private void saveFlow(PayOrder payOrder, PayRefund payRefund, String flowType, Object requestBody,
                          Object responseBody, boolean success, String errorCode, String errorMessage) {
        PayFlow payFlow = new PayFlow();
        payFlow.setFlowNo(IdUtil.getSnowflakeNextIdStr());
        payFlow.setFlowType(flowType);
        payFlow.setSuccess(success ? 1 : 0);
        payFlow.setRequestBody(JSONUtil.toJsonStr(requestBody));
        payFlow.setResponseBody(toResponseBody(responseBody));
        payFlow.setErrorCode(errorCode);
        payFlow.setErrorMessage(errorMessage);

        if (Objects.nonNull(payOrder)) {
            payFlow.setPayOrderNo(payOrder.getPayOrderNo());
            payFlow.setOutTradeNo(payOrder.getOutTradeNo());
            payFlow.setTradeNo(payOrder.getTradeNo());
        }

        if (Objects.nonNull(payRefund)) {
            payFlow.setRefundNo(payRefund.getRefundNo());
            payFlow.setOutRequestNo(payRefund.getOutRequestNo());
        }

        payFlowMapper.insert(payFlow);
    }

    /**
     * 构建支付结果
     *
     * @param payOrder 支付订单
     * @param payScene 支付场景
     * @param body 返回内容
     * @return 支付结果
     */
    private AlipayPayResult buildPayResult(PayOrder payOrder, String payScene, String body) {
        return AlipayPayResult.builder()
                .payOrderNo(payOrder.getPayOrderNo())
                .outTradeNo(payOrder.getOutTradeNo())
                .payScene(payScene)
                .body(body)
                .build();
    }

    /**
     * 格式化金额
     *
     * @param amount 金额
     * @return 支付宝金额字符串
     */
    private String formatAmount(BigDecimal amount) {
        return NumberUtil.roundStr(amount, 2);
    }

    /**
     * 判断金额是否一致
     *
     * @param localAmount 本地金额
     * @param alipayAmount 支付宝金额
     * @return 是否一致
     */
    private boolean amountEquals(BigDecimal localAmount, String alipayAmount) {
        if (Objects.isNull(localAmount) || StrUtil.isBlank(alipayAmount)) {
            return false;
        }
        return localAmount.compareTo(new BigDecimal(alipayAmount)) == 0;
    }

    /**
     * 解析支付宝日期
     *
     * @param dateTime 日期字符串
     * @return 日期
     */
    private Date parseDate(String dateTime) {
        if (StrUtil.isBlank(dateTime)) {
            return null;
        }
        return DateUtil.parse(dateTime);
    }

    /**
     * 转换响应内容
     *
     * @param responseBody 响应内容
     * @return 字符串响应
     */
    private String toResponseBody(Object responseBody) {
        if (Objects.isNull(responseBody)) {
            return null;
        }
        if (responseBody instanceof String value) {
            return value;
        }
        if (responseBody instanceof AlipayResponse response) {
            return response.getBody();
        }
        return JSONUtil.toJsonStr(responseBody);
    }
}
```

上面的实现中，PC 支付和 WAP 支付都返回 HTML 表单，App 支付返回订单字符串。Controller 层不要直接解析支付宝 SDK 响应，只需要根据 `payScene` 将 `body` 返回给前端即可。前端接收到 PC/WAP 的 HTML 表单后渲染跳转，App 接收到订单字符串后调用移动端支付宝 SDK。

需要补充的 Mapper 方法可以按如下方式定义，实际项目中也可以直接使用 MyBatis-Plus 的 `LambdaQueryWrapper` 实现。

文件位置：`src/main/java/io/github/atengk/pay/mapper/PayOrderMapper.java`

该 Mapper 用于按支付订单号和支付宝商户订单号查询支付订单。

```java
package io.github.atengk.pay.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.atengk.pay.entity.PayOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 支付订单 Mapper
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Mapper
public interface PayOrderMapper extends BaseMapper<PayOrder> {

    /**
     * 根据本地支付订单号查询
     *
     * @param payOrderNo 本地支付订单号
     * @return 支付订单
     */
    @Select("SELECT * FROM pay_order WHERE pay_order_no = #{payOrderNo} AND deleted = 0 LIMIT 1")
    PayOrder selectByPayOrderNo(String payOrderNo);

    /**
     * 根据支付宝商户订单号查询
     *
     * @param outTradeNo 支付宝商户订单号
     * @return 支付订单
     */
    @Select("SELECT * FROM pay_order WHERE out_trade_no = #{outTradeNo} AND deleted = 0 LIMIT 1")
    PayOrder selectByOutTradeNo(String outTradeNo);
}
```

文件位置：`src/main/java/io/github/atengk/pay/mapper/PayRefundMapper.java`

该 Mapper 用于按本地退款单号和业务退款单号查询退款记录。

```java
package io.github.atengk.pay.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.atengk.pay.entity.PayRefund;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 退款记录 Mapper
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Mapper
public interface PayRefundMapper extends BaseMapper<PayRefund> {

    /**
     * 根据本地退款单号查询
     *
     * @param refundNo 本地退款单号
     * @return 退款记录
     */
    @Select("SELECT * FROM pay_refund WHERE refund_no = #{refundNo} AND deleted = 0 LIMIT 1")
    PayRefund selectByRefundNo(String refundNo);

    /**
     * 根据业务退款单号查询
     *
     * @param bizRefundNo 业务退款单号
     * @return 退款记录
     */
    @Select("SELECT * FROM pay_refund WHERE biz_refund_no = #{bizRefundNo} AND deleted = 0 LIMIT 1")
    PayRefund selectByBizRefundNo(String bizRefundNo);
}
```

文件位置：`src/main/java/io/github/atengk/pay/mapper/PayFlowMapper.java`

该 Mapper 用于写入支付流水。

```java
package io.github.atengk.pay.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.atengk.pay.entity.PayFlow;
import org.apache.ibatis.annotations.Mapper;

/**
 * 支付流水 Mapper
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Mapper
public interface PayFlowMapper extends BaseMapper<PayFlow> {
}
```

核心功能验证时，可以按以下顺序进行：

```text
1. 创建本地支付订单，状态为 WAIT_PAY
2. 调用电脑网站支付，确认返回 HTML 表单
3. 浏览器跳转支付宝收银台完成付款
4. 检查异步通知是否进入 handleNotify
5. 检查 pay_order 是否更新为 PAY_SUCCESS
6. 调用支付查询接口，确认本地状态和支付宝状态一致
7. 发起部分退款，检查 pay_refund 是否生成退款记录
8. 调用退款查询接口，确认退款状态是否同步
```

核心注意事项如下：

| 注意项                          | 说明                                     |
| ------------------------------- | ---------------------------------------- |
| 异步通知必须验签                | 验签失败不能更新订单状态                 |
| 同步回跳不能作为支付成功依据    | 同步回跳只用于页面展示或触发查询         |
| `out_trade_no` 必须唯一         | 支付、查询、通知都依赖该字段关联订单     |
| `out_request_no` 必须唯一且稳定 | 退款重试不能更换退款请求号               |
| 金额必须校验                    | 回调金额、查询金额和本地订单金额必须一致 |
| 状态更新必须幂等                | 重复通知不能导致订单状态异常变更         |
| 支付和退款流水要保留            | 便于排查、对账和审计                     |



## 接口设计

接口设计用于向前端、App 端、支付宝开放平台和业务系统暴露统一的支付能力。支付接口应尽量保持职责单一：创建支付订单只负责生成本地支付记录，发起支付只负责调用支付宝接口，回调接口只负责处理支付宝通知，查询接口只负责同步或返回支付状态，退款接口只负责发起退款申请。

推荐接口路径如下：

| 接口             | 请求方式 | 路径                                  | 说明                                   |
| ---------------- | -------- | ------------------------------------- | -------------------------------------- |
| 创建支付订单接口 | `POST`   | `/api/pay/orders`                     | 根据业务订单创建本地支付订单           |
| 发起支付接口     | `POST`   | `/api/pay/alipay/pay`                 | 根据支付场景发起 PC、H5 或 App 支付    |
| 支付回调接口     | `POST`   | `/api/pay/alipay/notify`              | 接收支付宝异步通知                     |
| 支付同步回跳接口 | `GET`    | `/api/pay/alipay/return`              | 处理支付宝同步回跳                     |
| 查询支付状态接口 | `GET`    | `/api/pay/orders/{payOrderNo}/status` | 查询本地支付状态，必要时主动查询支付宝 |
| 申请退款接口     | `POST`   | `/api/pay/refunds`                    | 对已支付订单发起退款                   |

统一响应对象建议放在公共包中，便于 Controller 返回标准结构。

文件位置：`src/main/java/io/github/atengk/common/ApiResult.java`

该类用于封装接口统一响应结果。

```java
package io.github.atengk.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 接口统一响应结果
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResult<T> {

    /**
     * 是否成功
     */
    private Boolean success;

    /**
     * 响应编码
     */
    private String code;

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
     * @param <T> 数据类型
     * @return 响应结果
     */
    public static <T> ApiResult<T> ok(T data) {
        return new ApiResult<>(true, "0", "操作成功", data);
    }

    /**
     * 失败响应
     *
     * @param message 错误消息
     * @param <T> 数据类型
     * @return 响应结果
     */
    public static <T> ApiResult<T> fail(String message) {
        return new ApiResult<>(false, "500", message, null);
    }
}
```

### 创建支付订单接口

创建支付订单接口用于在用户提交业务订单后，生成本地支付订单。该接口只创建本地支付记录，不直接跳转支付宝，也不调用支付宝支付接口。

接口信息如下：

| 项目           | 内容               |
| -------------- | ------------------ |
| 请求方式       | `POST`             |
| 接口路径       | `/api/pay/orders`  |
| Content-Type   | `application/json` |
| 主要用途       | 创建本地支付订单   |
| 是否调用支付宝 | 否                 |

请求参数如下：

| 参数            | 类型         | 必填 | 说明                         |
| --------------- | ------------ | ---- | ---------------------------- |
| `bizOrderNo`    | `String`     | 是   | 业务订单号                   |
| `subject`       | `String`     | 是   | 订单标题                     |
| `body`          | `String`     | 否   | 订单描述                     |
| `totalAmount`   | `BigDecimal` | 是   | 支付金额                     |
| `payScene`      | `String`     | 是   | 支付场景：`PC`、`WAP`、`APP` |
| `expireMinutes` | `Integer`    | 否   | 支付过期分钟数，默认 30 分钟 |

文件位置：`src/main/java/io/github/atengk/pay/dto/PayOrderCreateRequest.java`

该 DTO 用于接收创建支付订单的请求参数。

```java
package io.github.atengk.pay.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 创建支付订单请求
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Data
public class PayOrderCreateRequest {

    /**
     * 业务订单号
     */
    @NotBlank(message = "业务订单号不能为空")
    private String bizOrderNo;

    /**
     * 订单标题
     */
    @NotBlank(message = "订单标题不能为空")
    private String subject;

    /**
     * 订单描述
     */
    private String body;

    /**
     * 支付金额
     */
    @NotNull(message = "支付金额不能为空")
    @DecimalMin(value = "0.01", message = "支付金额必须大于0")
    private BigDecimal totalAmount;

    /**
     * 支付场景：PC、WAP、APP
     */
    @NotBlank(message = "支付场景不能为空")
    private String payScene;

    /**
     * 支付过期分钟数
     */
    private Integer expireMinutes;
}
```

文件位置：`src/main/java/io/github/atengk/pay/dto/PayOrderCreateResult.java`

该 DTO 用于返回创建后的本地支付订单信息。

```java
package io.github.atengk.pay.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 创建支付订单结果
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Data
@Builder
public class PayOrderCreateResult {

    /**
     * 本地支付订单号
     */
    private String payOrderNo;

    /**
     * 业务订单号
     */
    private String bizOrderNo;

    /**
     * 支付宝商户订单号
     */
    private String outTradeNo;

    /**
     * 支付金额
     */
    private BigDecimal totalAmount;

    /**
     * 支付状态
     */
    private String status;

    /**
     * 支付过期时间
     */
    private Date expireTime;
}
```

Controller 接口示例如下。

文件位置：`src/main/java/io/github/atengk/pay/controller/PayController.java`

该 Controller 提供支付订单创建、发起支付、支付通知、同步回跳、状态查询和退款申请接口。

```java
package io.github.atengk.pay.controller;

import io.github.atengk.common.ApiResult;
import io.github.atengk.pay.dto.AlipayPayRequest;
import io.github.atengk.pay.dto.AlipayPayResult;
import io.github.atengk.pay.dto.AlipayRefundRequest;
import io.github.atengk.pay.dto.PayOrderCreateRequest;
import io.github.atengk.pay.dto.PayOrderCreateResult;
import io.github.atengk.pay.service.AlipayTradeService;
import io.github.atengk.pay.service.PayOrderApplicationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

/**
 * 支付接口控制器
 *
 * @author Ateng
 * @since 2026-05-07
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/pay")
public class PayController {

    private final PayOrderApplicationService payOrderApplicationService;
    private final AlipayTradeService alipayTradeService;

    /**
     * 创建支付订单
     *
     * @param request 创建支付订单请求
     * @return 创建结果
     */
    @PostMapping("/orders")
    public ApiResult<PayOrderCreateResult> createPayOrder(@Valid @RequestBody PayOrderCreateRequest request) {
        return ApiResult.ok(payOrderApplicationService.createPayOrder(request));
    }

    /**
     * 发起支付宝支付
     *
     * @param request 支付请求
     * @return 支付结果
     */
    @PostMapping("/alipay/pay")
    public ApiResult<AlipayPayResult> alipayPay(@Valid @RequestBody AlipayPayRequest request) {
        return ApiResult.ok(payOrderApplicationService.alipayPay(request));
    }

    /**
     * 处理支付宝异步通知
     *
     * @param request HTTP 请求
     * @return 支付宝通知响应
     */
    @PostMapping(value = "/alipay/notify", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public String alipayNotify(HttpServletRequest request) {
        return alipayTradeService.handleNotify(request);
    }

    /**
     * 处理支付宝同步回跳
     *
     * @param request HTTP 请求
     * @return 前端跳转地址
     */
    @GetMapping("/alipay/return")
    public String alipayReturn(HttpServletRequest request) {
        return alipayTradeService.handleReturn(request);
    }

    /**
     * 查询支付状态
     *
     * @param payOrderNo 本地支付订单号
     * @return 支付状态
     */
    @GetMapping("/orders/{payOrderNo}/status")
    public ApiResult<String> queryPayStatus(@PathVariable String payOrderNo) {
        return ApiResult.ok(alipayTradeService.queryPayStatus(payOrderNo));
    }

    /**
     * 申请退款
     *
     * @param request 退款请求
     * @return 退款状态
     */
    @PostMapping("/refunds")
    public ApiResult<String> refund(@Valid @RequestBody AlipayRefundRequest request) {
        return ApiResult.ok(alipayTradeService.refund(request));
    }
}
```

调用示例：

```bash
curl -X POST 'http://localhost:8080/api/pay/orders' \
  -H 'Content-Type: application/json' \
  -d '{
    "bizOrderNo": "BIZ202605070001",
    "subject": "会员月卡",
    "body": "会员月卡支付订单",
    "totalAmount": 9.90,
    "payScene": "PC",
    "expireMinutes": 30
  }'
```

响应示例：

```json
{
  "success": true,
  "code": "0",
  "message": "操作成功",
  "data": {
    "payOrderNo": "1900000000000000001",
    "bizOrderNo": "BIZ202605070001",
    "outTradeNo": "ALI202605070001000001",
    "totalAmount": 9.90,
    "status": "WAIT_PAY",
    "expireTime": "2026-05-07 15:30:00"
  }
}
```

### 发起支付接口

发起支付接口用于根据本地支付订单号调用支付宝支付接口。该接口需要根据支付场景分别调用电脑网站支付、手机网站支付或 App 支付。

接口信息如下：

| 项目           | 内容                  |
| -------------- | --------------------- |
| 请求方式       | `POST`                |
| 接口路径       | `/api/pay/alipay/pay` |
| Content-Type   | `application/json`    |
| 主要用途       | 发起支付宝支付        |
| 是否调用支付宝 | 是                    |

请求参数如下：

| 参数         | 类型     | 必填 | 说明                         |
| ------------ | -------- | ---- | ---------------------------- |
| `payOrderNo` | `String` | 是   | 本地支付订单号               |
| `payScene`   | `String` | 是   | 支付场景：`PC`、`WAP`、`APP` |
| `quitUrl`    | `String` | 否   | 手机网站支付退出地址         |

应用服务层根据支付场景分发到对应支付方法。

文件位置：`src/main/java/io/github/atengk/pay/service/PayOrderApplicationService.java`

该接口用于封装支付订单创建和支付发起编排逻辑。

```java
package io.github.atengk.pay.service;

import io.github.atengk.pay.dto.AlipayPayRequest;
import io.github.atengk.pay.dto.AlipayPayResult;
import io.github.atengk.pay.dto.PayOrderCreateRequest;
import io.github.atengk.pay.dto.PayOrderCreateResult;

/**
 * 支付订单应用服务
 *
 * @author Ateng
 * @since 2026-05-07
 */
public interface PayOrderApplicationService {

    /**
     * 创建支付订单
     *
     * @param request 创建支付订单请求
     * @return 创建结果
     */
    PayOrderCreateResult createPayOrder(PayOrderCreateRequest request);

    /**
     * 发起支付宝支付
     *
     * @param request 支付请求
     * @return 支付结果
     */
    AlipayPayResult alipayPay(AlipayPayRequest request);
}
```

文件位置：`src/main/java/io/github/atengk/pay/service/impl/PayOrderApplicationServiceImpl.java`

该实现类用于创建本地支付订单，并根据支付场景调用不同支付宝支付能力。

```java
package io.github.atengk.pay.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.pay.dto.AlipayPayRequest;
import io.github.atengk.pay.dto.AlipayPayResult;
import io.github.atengk.pay.dto.PayOrderCreateRequest;
import io.github.atengk.pay.dto.PayOrderCreateResult;
import io.github.atengk.pay.entity.PayOrder;
import io.github.atengk.pay.enums.PaySceneEnum;
import io.github.atengk.pay.enums.PayStatusEnum;
import io.github.atengk.pay.mapper.PayOrderMapper;
import io.github.atengk.pay.service.AlipayTradeService;
import io.github.atengk.pay.service.PayOrderApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Objects;

/**
 * 支付订单应用服务实现
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PayOrderApplicationServiceImpl implements PayOrderApplicationService {

    private final PayOrderMapper payOrderMapper;
    private final AlipayTradeService alipayTradeService;

    /**
     * 创建支付订单
     *
     * @param request 创建支付订单请求
     * @return 创建结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PayOrderCreateResult createPayOrder(PayOrderCreateRequest request) {
        PayOrder existOrder = payOrderMapper.selectByBizOrderNo(request.getBizOrderNo());
        if (Objects.nonNull(existOrder)) {
            log.info("业务订单已存在支付记录，业务订单号：{}", request.getBizOrderNo());
            return buildCreateResult(existOrder);
        }

        int expireMinutes = Objects.isNull(request.getExpireMinutes()) ? 30 : request.getExpireMinutes();
        Date expireTime = DateUtil.offsetMinute(new Date(), expireMinutes);

        PayOrder payOrder = new PayOrder();
        payOrder.setPayOrderNo(IdUtil.getSnowflakeNextIdStr());
        payOrder.setBizOrderNo(request.getBizOrderNo());
        payOrder.setOutTradeNo("ALI" + IdUtil.getSnowflakeNextIdStr());
        payOrder.setAppId("");
        payOrder.setPayChannel("ALIPAY");
        payOrder.setPayScene(request.getPayScene());
        payOrder.setSubject(request.getSubject());
        payOrder.setBody(request.getBody());
        payOrder.setTotalAmount(request.getTotalAmount());
        payOrder.setRefundAmount(java.math.BigDecimal.ZERO);
        payOrder.setCurrency("CNY");
        payOrder.setStatus(PayStatusEnum.WAIT_PAY.getCode());
        payOrder.setExpireTime(expireTime);

        payOrderMapper.insert(payOrder);

        log.info("支付订单创建成功，业务订单号：{}，支付订单号：{}", request.getBizOrderNo(), payOrder.getPayOrderNo());
        return buildCreateResult(payOrder);
    }

    /**
     * 发起支付宝支付
     *
     * @param request 支付请求
     * @return 支付结果
     */
    @Override
    public AlipayPayResult alipayPay(AlipayPayRequest request) {
        if (StrUtil.equals(request.getPayScene(), PaySceneEnum.PC.getCode())) {
            return alipayTradeService.pagePay(request);
        }
        if (StrUtil.equals(request.getPayScene(), PaySceneEnum.WAP.getCode())) {
            return alipayTradeService.wapPay(request);
        }
        if (StrUtil.equals(request.getPayScene(), PaySceneEnum.APP.getCode())) {
            return alipayTradeService.appPay(request);
        }
        throw new IllegalArgumentException("不支持的支付宝支付场景");
    }

    /**
     * 构建创建支付订单结果
     *
     * @param payOrder 支付订单
     * @return 创建结果
     */
    private PayOrderCreateResult buildCreateResult(PayOrder payOrder) {
        return PayOrderCreateResult.builder()
                .payOrderNo(payOrder.getPayOrderNo())
                .bizOrderNo(payOrder.getBizOrderNo())
                .outTradeNo(payOrder.getOutTradeNo())
                .totalAmount(payOrder.getTotalAmount())
                .status(payOrder.getStatus())
                .expireTime(payOrder.getExpireTime())
                .build();
    }
}
```

上述实现中 `appId` 可以在创建订单时从 `AlipayProperties` 注入赋值。示例中为了突出接口流程，保留为空字符串，实际项目中应写入当前支付宝应用 ID。

调用示例：

```bash
curl -X POST 'http://localhost:8080/api/pay/alipay/pay' \
  -H 'Content-Type: application/json' \
  -d '{
    "payOrderNo": "1900000000000000001",
    "payScene": "PC"
  }'
```

PC 和 WAP 支付返回的 `data.body` 是 HTML 表单，前端需要将其写入页面完成跳转。App 支付返回的 `data.body` 是订单字符串，移动端应调用支付宝 App SDK 发起支付。

### 支付回调接口

支付回调接口用于接收支付宝异步通知。该接口由支付宝服务器调用，不由前端调用。接口必须支持 `application/x-www-form-urlencoded` 表单参数。

接口信息如下：

| 项目         | 内容                                    |
| ------------ | --------------------------------------- |
| 请求方式     | `POST`                                  |
| 接口路径     | `/api/pay/alipay/notify`                |
| Content-Type | `application/x-www-form-urlencoded`     |
| 主要用途     | 接收支付宝异步通知                      |
| 返回值       | 处理成功返回 `success`，失败返回 `fail` |

异步通知接口不返回 JSON，必须直接返回纯文本。支付宝收到 `success` 后才会认为商户系统处理成功；如果返回其他内容或请求超时，支付宝可能会重试通知。

回调接口处理要求如下：

| 步骤     | 说明                                 |
| -------- | ------------------------------------ |
| 接收参数 | 获取支付宝通知的全部表单参数         |
| 验签     | 使用支付宝公钥证书校验签名           |
| 校验订单 | 根据 `out_trade_no` 查询本地支付订单 |
| 校验金额 | 比较通知金额和本地订单金额           |
| 校验应用 | 比较通知中的 `app_id` 和当前应用 ID  |
| 幂等处理 | 已支付订单重复通知直接返回 `success` |
| 更新状态 | 根据 `trade_status` 更新本地订单     |
| 返回结果 | 成功返回 `success`，失败返回 `fail`  |

### 查询支付状态接口

查询支付状态接口用于前端、App 或后台系统获取当前支付订单状态。该接口可以先查询本地状态，如果本地状态仍是 `WAIT_PAY` 或 `PAYING`，可以主动调用支付宝查询接口进行补偿。

接口信息如下：

| 项目           | 内容                                  |
| -------------- | ------------------------------------- |
| 请求方式       | `GET`                                 |
| 接口路径       | `/api/pay/orders/{payOrderNo}/status` |
| 主要用途       | 查询支付状态                          |
| 是否调用支付宝 | 视本地订单状态决定                    |

调用示例：

```bash
curl -X GET 'http://localhost:8080/api/pay/orders/1900000000000000001/status'
```

响应示例：

```json
{
  "success": true,
  "code": "0",
  "message": "操作成功",
  "data": "PAY_SUCCESS"
}
```

查询支付状态时建议遵循以下规则：

| 本地状态      | 处理方式                   |
| ------------- | -------------------------- |
| `PAY_SUCCESS` | 直接返回成功               |
| `PAY_CLOSED`  | 直接返回关闭               |
| `WAIT_PAY`    | 可以主动查询支付宝         |
| `PAYING`      | 可以主动查询支付宝         |
| `PAY_FAILED`  | 按业务需要决定是否查询     |
| `REFUNDING`   | 不走支付查询，应走退款查询 |

### 申请退款接口

申请退款接口用于对已支付订单发起退款。退款接口应由业务系统、运营后台或售后系统调用，不建议直接暴露给普通前端用户绕过业务审核。

接口信息如下：

| 项目           | 内容               |
| -------------- | ------------------ |
| 请求方式       | `POST`             |
| 接口路径       | `/api/pay/refunds` |
| Content-Type   | `application/json` |
| 主要用途       | 发起支付宝退款     |
| 是否调用支付宝 | 是                 |

请求参数如下：

| 参数           | 类型         | 必填 | 说明           |
| -------------- | ------------ | ---- | -------------- |
| `payOrderNo`   | `String`     | 是   | 本地支付订单号 |
| `bizRefundNo`  | `String`     | 是   | 业务退款单号   |
| `refundAmount` | `BigDecimal` | 是   | 退款金额       |
| `refundReason` | `String`     | 否   | 退款原因       |

调用示例：

```bash
curl -X POST 'http://localhost:8080/api/pay/refunds' \
  -H 'Content-Type: application/json' \
  -d '{
    "payOrderNo": "1900000000000000001",
    "bizRefundNo": "RF202605070001",
    "refundAmount": 9.90,
    "refundReason": "用户申请退款"
  }'
```

响应示例：

```json
{
  "success": true,
  "code": "0",
  "message": "操作成功",
  "data": "REFUND_SUCCESS"
}
```

退款接口必须校验以下内容：

| 校验项               | 说明                                      |
| -------------------- | ----------------------------------------- |
| 支付订单是否存在     | 不存在不能退款                            |
| 支付订单是否成功     | 只有支付成功订单允许退款                  |
| 退款金额是否合法     | 退款金额必须大于 0                        |
| 累计退款金额是否合法 | 累计退款金额不能超过支付金额              |
| 业务退款单是否重复   | 同一个 `bizRefundNo` 只能发起一次         |
| 退款请求号是否稳定   | 同一笔退款重试应使用相同 `out_request_no` |

## 安全与幂等设计

安全与幂等设计是支付系统的核心。支付宝支付涉及资金交易，不能只保证接口可用，还必须保证请求可信、通知可信、金额可信、状态更新可信，并且能正确处理重复请求、重复通知、并发支付和重复退款。

### 签名验证

支付宝支付中的签名主要分为两类：请求加签和响应验签。

请求加签由支付宝 SDK 根据应用私钥完成。系统调用支付宝支付、查询、退款等接口时，不需要业务代码手动拼接签名，但必须保证应用私钥、签名类型、证书路径和字符集配置正确。

响应验签用于确认支付宝返回或通知的内容确实来自支付宝，且中途未被篡改。异步通知属于最关键的验签场景，必须在任何业务处理之前完成验签。

签名相关安全要求如下：

| 要求             | 说明                                               |
| ---------------- | -------------------------------------------------- |
| 使用 RSA2        | 不建议使用普通 RSA                                 |
| 私钥不入库       | 应用私钥不应保存到数据库                           |
| 私钥不进 Git     | 不允许提交到代码仓库                               |
| 使用证书模式     | 推荐使用应用公钥证书、支付宝公钥证书和支付宝根证书 |
| 回调先验签       | 验签失败不能更新任何订单状态                       |
| 金额必须二次校验 | 验签成功后仍需校验订单金额                         |
| 应用 ID 必须校验 | 防止其他应用通知误入本系统                         |

文件位置：`src/main/java/io/github/atengk/pay/security/AlipayNotifyVerifier.java`

该类用于统一处理支付宝异步通知验签，业务服务只需要调用 `verify` 方法。

```java
package io.github.atengk.pay.security;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.alipay.api.internal.util.AlipaySignature;
import io.github.atengk.pay.config.AlipayProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 支付宝通知验签器
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlipayNotifyVerifier {

    private final AlipayProperties alipayProperties;

    /**
     * 验证支付宝通知签名
     *
     * @param params 通知参数
     * @return 是否验签通过
     */
    public boolean verify(Map<String, String> params) {
        try {
            if (params == null || params.isEmpty()) {
                log.warn("支付宝通知验签失败，通知参数为空");
                return false;
            }

            boolean verified = AlipaySignature.rsaCertCheckV1(
                    params,
                    alipayProperties.getAlipayCertPath(),
                    alipayProperties.getCharset(),
                    alipayProperties.getSignType()
            );

            if (!verified) {
                log.warn("支付宝通知验签失败，通知参数：{}", JSONUtil.toJsonStr(params));
                return false;
            }

            String appId = params.get("app_id");
            if (!StrUtil.equals(appId, alipayProperties.getAppId())) {
                log.warn("支付宝通知 appId 不匹配，通知 appId：{}，系统 appId：{}", appId, alipayProperties.getAppId());
                return false;
            }

            return true;
        } catch (Exception e) {
            log.error("支付宝通知验签异常，通知参数：{}", JSONUtil.toJsonStr(params), e);
            return false;
        }
    }
}
```

### 回调验签

回调验签是支付宝异步通知处理中的第一道安全边界。异步通知接口必须先验签，再查询本地订单，再校验金额，最后才允许更新订单状态。

推荐回调处理顺序如下：

```text
接收支付宝通知
    ↓
提取全部表单参数
    ↓
支付宝证书验签
    ↓
校验 app_id
    ↓
查询本地支付订单
    ↓
校验 total_amount
    ↓
校验 trade_status
    ↓
执行幂等状态更新
    ↓
返回 success
```

金额校验工具方法如下。

文件位置：`src/main/java/io/github/atengk/pay/security/AlipayAmountValidator.java`

该类用于校验支付宝通知金额和本地订单金额是否一致。

```java
package io.github.atengk.pay.security;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * 支付宝金额校验器
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Component
public class AlipayAmountValidator {

    /**
     * 校验支付金额是否一致
     *
     * @param localAmount 本地订单金额
     * @param notifyAmount 支付宝通知金额
     * @return 是否一致
     */
    public boolean validate(BigDecimal localAmount, String notifyAmount) {
        if (Objects.isNull(localAmount) || StrUtil.isBlank(notifyAmount)) {
            log.warn("支付宝金额校验失败，金额参数为空，本地金额：{}，通知金额：{}", localAmount, notifyAmount);
            return false;
        }

        BigDecimal alipayAmount = NumberUtil.toBigDecimal(notifyAmount);
        boolean matched = localAmount.compareTo(alipayAmount) == 0;
        if (!matched) {
            log.warn("支付宝金额校验失败，本地金额：{}，通知金额：{}", localAmount, notifyAmount);
        }
        return matched;
    }
}
```

回调验签需要注意以下问题：

| 问题            | 处理方式                         |
| --------------- | -------------------------------- |
| 验签失败        | 返回 `fail`，不处理订单          |
| `app_id` 不一致 | 返回 `fail`，记录安全日志        |
| 订单不存在      | 返回 `fail`，等待人工排查        |
| 金额不一致      | 返回 `fail`，记录告警日志        |
| 交易状态未知    | 记录流水，不更新成功状态         |
| 业务处理失败    | 不返回 `success`，允许支付宝重试 |

### 通知幂等处理

支付宝异步通知可能重复推送，同一笔交易成功通知可能被系统接收多次。通知幂等的目标是：重复通知不会重复执行业务逻辑，不会重复发放权益，不会重复修改订单，不会导致状态回退。

通知幂等建议从三层实现：

| 层级           | 策略                | 说明                                                         |
| -------------- | ------------------- | ------------------------------------------------------------ |
| 数据库唯一约束 | `out_trade_no` 唯一 | 保证一笔支付宝商户订单只对应一笔本地支付订单                 |
| 状态判断       | 成功状态直接返回    | 已经 `PAY_SUCCESS` 的订单再次收到成功通知，直接返回 `success` |
| 条件更新       | 只允许特定状态更新  | 只允许 `WAIT_PAY`、`PAYING` 更新为 `PAY_SUCCESS`             |
| 业务事件幂等   | 业务订单防重        | 支付成功事件消费方也必须做幂等                               |

推荐在支付订单状态更新时使用条件更新，不要先查询再无条件覆盖。条件更新可以避免并发通知导致重复处理。

文件位置：`src/main/java/io/github/atengk/pay/service/PayOrderStatusService.java`

该接口用于封装支付订单状态变更能力。

```java
package io.github.atengk.pay.service;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 支付订单状态服务
 *
 * @author Ateng
 * @since 2026-05-07
 */
public interface PayOrderStatusService {

    /**
     * 标记支付成功
     *
     * @param payOrderNo 支付订单号
     * @param tradeNo 支付宝交易号
     * @param paidAmount 实收金额
     * @param payTime 支付时间
     * @param tradeStatus 支付宝交易状态
     * @return 是否更新成功
     */
    boolean markPaySuccess(String payOrderNo, String tradeNo, BigDecimal paidAmount, Date payTime, String tradeStatus);

    /**
     * 标记交易关闭
     *
     * @param payOrderNo 支付订单号
     * @param tradeNo 支付宝交易号
     * @param tradeStatus 支付宝交易状态
     * @return 是否更新成功
     */
    boolean markPayClosed(String payOrderNo, String tradeNo, String tradeStatus);
}
```

文件位置：`src/main/java/io/github/atengk/pay/service/impl/PayOrderStatusServiceImpl.java`

该实现类通过条件更新保证支付状态变更幂等。

```java
package io.github.atengk.pay.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import io.github.atengk.pay.entity.PayOrder;
import io.github.atengk.pay.enums.PayStatusEnum;
import io.github.atengk.pay.mapper.PayOrderMapper;
import io.github.atengk.pay.service.PayOrderStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 支付订单状态服务实现
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PayOrderStatusServiceImpl implements PayOrderStatusService {

    private final PayOrderMapper payOrderMapper;

    /**
     * 标记支付成功
     *
     * @param payOrderNo 支付订单号
     * @param tradeNo 支付宝交易号
     * @param paidAmount 实收金额
     * @param payTime 支付时间
     * @param tradeStatus 支付宝交易状态
     * @return 是否更新成功
     */
    @Override
    public boolean markPaySuccess(String payOrderNo, String tradeNo, BigDecimal paidAmount, Date payTime, String tradeStatus) {
        LambdaUpdateWrapper<PayOrder> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(PayOrder::getPayOrderNo, payOrderNo)
                .in(PayOrder::getStatus, CollUtil.newArrayList(
                        PayStatusEnum.WAIT_PAY.getCode(),
                        PayStatusEnum.PAYING.getCode()
                ))
                .set(PayOrder::getTradeNo, tradeNo)
                .set(PayOrder::getPaidAmount, paidAmount)
                .set(PayOrder::getPayTime, payTime)
                .set(PayOrder::getTradeStatus, tradeStatus)
                .set(PayOrder::getNotifyTime, new Date())
                .set(PayOrder::getStatus, PayStatusEnum.PAY_SUCCESS.getCode());

        int rows = payOrderMapper.update(null, wrapper);
        boolean updated = rows > 0;

        if (updated) {
            log.info("支付订单状态更新为支付成功，支付订单号：{}，支付宝交易号：{}", payOrderNo, tradeNo);
        } else {
            log.info("支付订单支付成功状态无需重复更新，支付订单号：{}", payOrderNo);
        }

        return updated;
    }

    /**
     * 标记交易关闭
     *
     * @param payOrderNo 支付订单号
     * @param tradeNo 支付宝交易号
     * @param tradeStatus 支付宝交易状态
     * @return 是否更新成功
     */
    @Override
    public boolean markPayClosed(String payOrderNo, String tradeNo, String tradeStatus) {
        LambdaUpdateWrapper<PayOrder> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(PayOrder::getPayOrderNo, payOrderNo)
                .in(PayOrder::getStatus, CollUtil.newArrayList(
                        PayStatusEnum.WAIT_PAY.getCode(),
                        PayStatusEnum.PAYING.getCode()
                ))
                .set(PayOrder::getTradeNo, tradeNo)
                .set(PayOrder::getTradeStatus, tradeStatus)
                .set(PayOrder::getCloseTime, new Date())
                .set(PayOrder::getStatus, PayStatusEnum.PAY_CLOSED.getCode());

        int rows = payOrderMapper.update(null, wrapper);
        boolean updated = rows > 0;

        if (updated) {
            log.info("支付订单状态更新为交易关闭，支付订单号：{}，支付宝交易号：{}", payOrderNo, tradeNo);
        } else {
            log.info("支付订单关闭状态无需重复更新，支付订单号：{}", payOrderNo);
        }

        return updated;
    }
}
```

通知幂等处理的关键点是：只有第一次从 `WAIT_PAY` 或 `PAYING` 成功更新到 `PAY_SUCCESS` 的线程，才允许触发业务订单支付成功逻辑。后续重复通知只能记录流水并返回 `success`。

业务事件消费方也必须做幂等。例如订单模块收到支付成功事件后，应检查业务订单是否已经支付成功，不能重复发货、重复开通会员或重复发放权益。

### 订单状态防重处理

订单状态防重处理用于防止重复支付、重复退款、状态覆盖和状态回退。支付订单状态必须按照明确的状态机流转，不能任意修改。

推荐状态流转如下：

```text
WAIT_PAY
    ├── PAYING
    │     ├── PAY_SUCCESS
    │     ├── PAY_CLOSED
    │     └── PAY_FAILED
    └── PAY_CLOSED

PAY_SUCCESS
    ├── REFUNDING
    └── REFUND_SUCCESS
```

禁止的状态流转如下：

| 当前状态         | 禁止变更为    | 原因                                                         |
| ---------------- | ------------- | ------------------------------------------------------------ |
| `PAY_SUCCESS`    | `WAIT_PAY`    | 已成功订单不能回退为待支付                                   |
| `PAY_SUCCESS`    | `PAYING`      | 已成功订单不能重新进入支付中                                 |
| `REFUND_SUCCESS` | `PAY_SUCCESS` | 已全额退款订单不能恢复为支付成功                             |
| `PAY_CLOSED`     | `PAY_SUCCESS` | 已关闭订单不能直接改为成功，除非主动查询支付宝确认后人工修正 |
| `PAY_FAILED`     | `PAY_SUCCESS` | 失败订单不能直接覆盖成功，应查询支付宝确认                   |

订单状态防重建议使用以下策略：

| 策略       | 说明                                                     |
| ---------- | -------------------------------------------------------- |
| 唯一索引   | `out_trade_no`、`biz_order_no`、`biz_refund_no` 保持唯一 |
| 条件更新   | 更新状态时带上当前状态条件                               |
| 乐观锁     | 使用 `version` 字段防止并发覆盖                          |
| 分布式锁   | 高并发场景下按 `payOrderNo` 加锁                         |
| 业务幂等   | 支付成功事件、退款成功事件消费方也必须防重               |
| 状态机校验 | 所有状态变更必须经过统一状态服务                         |

数据库唯一索引建议如下：

```sql
-- 支付订单号唯一，防止本地支付订单重复
ALTER TABLE pay_order ADD UNIQUE KEY uk_pay_order_no (pay_order_no);

-- 支付宝商户订单号唯一，防止重复向支付宝提交同一商户订单
ALTER TABLE pay_order ADD UNIQUE KEY uk_out_trade_no (out_trade_no);

-- 业务订单号唯一，适用于一笔业务订单只允许一笔支付订单的场景
ALTER TABLE pay_order ADD UNIQUE KEY uk_biz_order_no (biz_order_no);

-- 业务退款单号唯一，防止业务侧重复退款
ALTER TABLE pay_refund ADD UNIQUE KEY uk_biz_refund_no (biz_refund_no);

-- 支付宝退款请求号唯一，防止重复退款
ALTER TABLE pay_refund ADD UNIQUE KEY uk_out_request_no (out_request_no);
```

如果系统允许一笔业务订单多次发起支付，则不要给 `biz_order_no` 单独加唯一索引，可以改成联合唯一索引，例如 `biz_order_no + pay_order_no`，或者通过业务状态控制当前可用支付订单。

退款防重处理建议如下：

```text
收到退款请求
    ↓
根据 bizRefundNo 查询退款记录
    ↓
如果已存在，直接返回已有退款状态
    ↓
如果不存在，校验可退金额
    ↓
创建退款记录
    ↓
调用支付宝退款接口
    ↓
更新退款状态
```

退款防重的核心是 `bizRefundNo` 和 `out_request_no`。同一笔业务退款单重试时，必须使用同一个 `out_request_no`，不能每次重试生成新的退款请求号，否则可能造成重复退款风险。

在高并发场景中，可以增加分布式锁。锁粒度建议使用支付订单号或业务退款单号，避免锁范围过大。

```text
支付通知锁：pay:notify:{payOrderNo}
支付查询锁：pay:query:{payOrderNo}
退款申请锁：pay:refund:{bizRefundNo}
退款查询锁：pay:refund-query:{refundNo}
```

最终建议采用“数据库唯一约束 + 条件更新 + 状态机校验 + 业务事件幂等”的组合方案。单独依赖分布式锁并不可靠，因为锁可能因为超时、网络抖动或服务重启失效；数据库约束和条件更新才是支付幂等的最终防线。



## 异常处理

异常处理用于统一支付模块中的错误返回、日志记录、状态保护和异常补偿。支付宝支付涉及外部接口调用、异步通知、金额校验、状态流转和退款处理，不能只依赖普通的 `RuntimeException`。建议定义支付专用异常、错误码和全局异常处理器，保证接口返回统一、日志可追踪、状态不被错误覆盖。

支付模块常见异常类型如下：

| 异常类型     | 说明                                                     |
| ------------ | -------------------------------------------------------- |
| 支付创建异常 | 本地支付订单创建失败、重复订单、金额非法、支付场景不支持 |
| 支付发起异常 | 调用支付宝支付接口失败、SDK 参数错误、证书配置错误       |
| 回调处理异常 | 验签失败、金额不一致、订单不存在、重复通知处理异常       |
| 订单状态异常 | 当前订单状态不允许支付、查询、关闭、退款或状态回退       |
| 退款异常     | 原订单未支付、退款金额超限、重复退款、支付宝退款失败     |

建议先定义支付错误码。

文件位置：`src/main/java/io/github/atengk/pay/exception/PayErrorCode.java`

该枚举用于统一支付模块错误编码和错误描述。

```java
package io.github.atengk.pay.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 支付错误码
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Getter
@AllArgsConstructor
public enum PayErrorCode {

    /**
     * 支付订单不存在
     */
    PAY_ORDER_NOT_FOUND("PAY_ORDER_NOT_FOUND", "支付订单不存在"),

    /**
     * 支付订单已存在
     */
    PAY_ORDER_EXISTS("PAY_ORDER_EXISTS", "支付订单已存在"),

    /**
     * 支付金额非法
     */
    PAY_AMOUNT_INVALID("PAY_AMOUNT_INVALID", "支付金额非法"),

    /**
     * 支付场景不支持
     */
    PAY_SCENE_NOT_SUPPORTED("PAY_SCENE_NOT_SUPPORTED", "支付场景不支持"),

    /**
     * 支付订单状态异常
     */
    PAY_STATUS_INVALID("PAY_STATUS_INVALID", "支付订单状态异常"),

    /**
     * 支付宝接口调用失败
     */
    ALIPAY_API_ERROR("ALIPAY_API_ERROR", "支付宝接口调用失败"),

    /**
     * 支付宝通知验签失败
     */
    ALIPAY_NOTIFY_SIGN_ERROR("ALIPAY_NOTIFY_SIGN_ERROR", "支付宝通知验签失败"),

    /**
     * 支付宝通知金额不一致
     */
    ALIPAY_NOTIFY_AMOUNT_ERROR("ALIPAY_NOTIFY_AMOUNT_ERROR", "支付宝通知金额不一致"),

    /**
     * 退款金额非法
     */
    REFUND_AMOUNT_INVALID("REFUND_AMOUNT_INVALID", "退款金额非法"),

    /**
     * 重复退款
     */
    REFUND_DUPLICATE("REFUND_DUPLICATE", "重复退款"),

    /**
     * 退款处理失败
     */
    REFUND_PROCESS_ERROR("REFUND_PROCESS_ERROR", "退款处理失败");

    private final String code;

    private final String message;
}
```

文件位置：`src/main/java/io/github/atengk/pay/exception/PayException.java`

该异常类用于在支付模块中抛出业务异常，并携带支付错误码。

```java
package io.github.atengk.pay.exception;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;

/**
 * 支付业务异常
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Getter
public class PayException extends RuntimeException {

    private final String code;

    /**
     * 创建支付业务异常
     *
     * @param errorCode 支付错误码
     */
    public PayException(PayErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    /**
     * 创建支付业务异常
     *
     * @param errorCode 支付错误码
     * @param message 错误消息
     */
    public PayException(PayErrorCode errorCode, String message) {
        super(StrUtil.blankToDefault(message, errorCode.getMessage()));
        this.code = errorCode.getCode();
    }
}
```

文件位置：`src/main/java/io/github/atengk/common/GlobalExceptionHandler.java`

该全局异常处理器用于统一处理参数校验异常、支付业务异常和系统异常。

```java
package io.github.atengk.common;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import io.github.atengk.pay.exception.PayException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理支付业务异常
     *
     * @param e 支付异常
     * @param request HTTP 请求
     * @return 响应结果
     */
    @ExceptionHandler(PayException.class)
    public ApiResult<Void> handlePayException(PayException e, HttpServletRequest request) {
        log.warn("支付业务异常，请求地址：{}，错误码：{}，错误信息：{}",
                request.getRequestURI(), e.getCode(), e.getMessage());
        return new ApiResult<>(false, e.getCode(), e.getMessage(), null);
    }

    /**
     * 处理请求参数校验异常
     *
     * @param e 参数校验异常
     * @param request HTTP 请求
     * @return 响应结果
     */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ApiResult<Void> handleValidException(Exception e, HttpServletRequest request) {
        String message = "请求参数校验失败";

        if (e instanceof MethodArgumentNotValidException validException
                && validException.getBindingResult().hasErrors()) {
            message = validException.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        }

        if (e instanceof BindException bindException
                && bindException.getBindingResult().hasErrors()) {
            message = bindException.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        }

        log.warn("请求参数异常，请求地址：{}，错误信息：{}", request.getRequestURI(), message);
        return ApiResult.fail(StrUtil.blankToDefault(message, "请求参数错误"));
    }

    /**
     * 处理系统异常
     *
     * @param e 异常
     * @param request HTTP 请求
     * @return 响应结果
     */
    @ExceptionHandler(Exception.class)
    public ApiResult<Void> handleException(Exception e, HttpServletRequest request) {
        log.error("系统异常，请求地址：{}，请求参数：{}",
                request.getRequestURI(), JSONUtil.toJsonStr(request.getParameterMap()), e);
        return ApiResult.fail("系统繁忙，请稍后重试");
    }
}
```

### 支付创建异常

支付创建异常主要发生在创建本地支付订单阶段。此阶段还没有调用支付宝接口，因此重点是校验业务订单、支付金额、支付场景和重复创建问题。

常见异常场景如下：

| 场景                         | 处理方式                             |
| ---------------------------- | ------------------------------------ |
| 业务订单不存在               | 拒绝创建支付订单                     |
| 业务订单已取消               | 拒绝创建支付订单                     |
| 业务订单已支付               | 直接返回已支付状态或拒绝重复创建     |
| 支付金额小于等于 0           | 返回金额非法                         |
| 支付场景不支持               | 返回支付场景不支持                   |
| 同一业务订单重复创建支付订单 | 返回已有支付订单或按业务规则重新创建 |
| 数据库唯一索引冲突           | 捕获异常后查询已有支付订单返回       |

支付创建时建议采用“先查已有订单，再依赖唯一索引兜底”的方式。不能只依赖代码判断，因为并发请求下可能出现多个线程同时创建支付订单。

处理逻辑建议如下：

```text
接收创建支付订单请求
    ↓
校验业务订单状态
    ↓
校验支付金额
    ↓
校验支付场景
    ↓
查询是否已有支付订单
    ↓
不存在则创建支付订单
    ↓
如果唯一索引冲突，则查询已有订单返回
```

支付创建异常不应写入支付宝流水表，因为此阶段尚未与支付宝交互。但可以记录业务日志，便于排查重复创建、金额异常和订单状态异常。

### 回调处理异常

回调处理异常主要发生在支付宝异步通知阶段。异步通知接口与普通业务接口不同，返回值不是统一 JSON，而是支付宝要求的纯文本 `success` 或 `fail`。

回调异常处理原则如下：

| 异常场景             | 是否返回 `success` | 说明                                                 |
| -------------------- | ------------------ | ---------------------------------------------------- |
| 验签失败             | 否                 | 返回 `fail`，不能处理订单                            |
| `app_id` 不一致      | 否                 | 返回 `fail`，记录安全日志                            |
| 本地订单不存在       | 否                 | 返回 `fail`，等待排查                                |
| 通知金额不一致       | 否                 | 返回 `fail`，记录告警                                |
| 订单已支付且金额一致 | 是                 | 重复通知，直接返回 `success`                         |
| 交易状态为未知值     | 是                 | 记录流水，不更新成功状态                             |
| 业务订单更新失败     | 否                 | 不返回 `success`，允许支付宝重试                     |
| 支付流水写入失败     | 视情况             | 核心订单状态成功后可返回 `success`，但需记录系统异常 |

回调异常处理要特别注意：不能因为写支付流水失败就导致支付成功订单反复处理。如果订单状态已经成功更新，业务订单也已经处理成功，即使流水记录失败，也可以返回 `success`，同时记录错误日志，后续通过补偿任务修复流水。

推荐回调处理顺序如下：

```text
验签
    ↓
校验 app_id
    ↓
查询本地订单
    ↓
校验金额
    ↓
写入通知流水
    ↓
幂等更新支付订单
    ↓
触发业务订单支付成功事件
    ↓
返回 success
```

回调处理中禁止以下操作：

| 禁止操作                       | 原因                                 |
| ------------------------------ | ------------------------------------ |
| 验签前更新订单                 | 存在伪造通知风险                     |
| 只根据同步回跳更新支付成功     | 同步回跳不可靠                       |
| 忽略金额校验                   | 存在错单、串单风险                   |
| 重复发放权益                   | 重复通知会导致资损                   |
| 捕获异常后无条件返回 `success` | 可能导致支付宝停止重试，订单状态丢失 |

### 订单状态异常

订单状态异常主要发生在支付订单状态流转过程中。支付订单状态不能任意覆盖，必须按照状态机规则处理，避免出现已支付订单重新变为待支付、已退款订单重新变为支付成功等问题。

常见状态异常如下：

| 场景                         | 处理方式                               |
| ---------------------------- | -------------------------------------- |
| 已支付订单再次发起支付       | 拒绝发起，返回已支付                   |
| 已关闭订单发起支付           | 拒绝发起，提示重新下单                 |
| 待支付订单申请退款           | 拒绝退款                               |
| 已全额退款订单再次退款       | 拒绝退款                               |
| 支付成功通知晚于关闭通知到达 | 主动查询支付宝状态后处理               |
| 本地状态与支付宝状态不一致   | 以主动查询结果为准，并记录状态修正日志 |

可以通过统一的状态校验器集中控制状态流转。

文件位置：`src/main/java/io/github/atengk/pay/state/PayStatusValidator.java`

该状态校验器用于判断支付订单是否允许支付、退款和状态变更。

```java
package io.github.atengk.pay.state;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.pay.enums.PayStatusEnum;
import io.github.atengk.pay.exception.PayErrorCode;
import io.github.atengk.pay.exception.PayException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 支付状态校验器
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Component
public class PayStatusValidator {

    private static final Set<String> PAYABLE_STATUS = CollUtil.newHashSet(
            PayStatusEnum.WAIT_PAY.getCode(),
            PayStatusEnum.PAYING.getCode()
    );

    private static final Set<String> REFUNDABLE_STATUS = CollUtil.newHashSet(
            PayStatusEnum.PAY_SUCCESS.getCode(),
            PayStatusEnum.REFUNDING.getCode()
    );

    /**
     * 校验是否允许发起支付
     *
     * @param status 当前支付状态
     */
    public void checkPayable(String status) {
        if (!PAYABLE_STATUS.contains(status)) {
            log.warn("当前支付状态不允许发起支付，状态：{}", status);
            throw new PayException(PayErrorCode.PAY_STATUS_INVALID, "当前订单状态不允许发起支付");
        }
    }

    /**
     * 校验是否允许退款
     *
     * @param status 当前支付状态
     */
    public void checkRefundable(String status) {
        if (!REFUNDABLE_STATUS.contains(status)) {
            log.warn("当前支付状态不允许退款，状态：{}", status);
            throw new PayException(PayErrorCode.PAY_STATUS_INVALID, "当前订单状态不允许退款");
        }
    }

    /**
     * 校验是否允许状态变更
     *
     * @param currentStatus 当前状态
     * @param targetStatus 目标状态
     */
    public void checkStatusChange(String currentStatus, String targetStatus) {
        if (StrUtil.equals(currentStatus, targetStatus)) {
            return;
        }

        if (StrUtil.equals(currentStatus, PayStatusEnum.PAY_SUCCESS.getCode())
                && StrUtil.equals(targetStatus, PayStatusEnum.PAYING.getCode())) {
            throw new PayException(PayErrorCode.PAY_STATUS_INVALID, "已支付订单不能回退到支付中");
        }

        if (StrUtil.equals(currentStatus, PayStatusEnum.REFUND_SUCCESS.getCode())
                && StrUtil.equals(targetStatus, PayStatusEnum.PAY_SUCCESS.getCode())) {
            throw new PayException(PayErrorCode.PAY_STATUS_INVALID, "已退款订单不能回退到支付成功");
        }
    }
}
```

### 退款异常

退款异常主要发生在退款申请、支付宝退款接口调用和退款结果查询阶段。退款涉及资金退回，必须保证退款单号唯一、金额合法、状态幂等。

常见退款异常如下：

| 场景                     | 处理方式                               |
| ------------------------ | -------------------------------------- |
| 原支付订单不存在         | 返回支付订单不存在                     |
| 原订单未支付成功         | 拒绝退款                               |
| 退款金额小于等于 0       | 返回退款金额非法                       |
| 累计退款金额超过支付金额 | 拒绝退款                               |
| 业务退款单重复提交       | 返回已有退款单状态                     |
| 支付宝退款接口失败       | 记录失败原因，退款状态改为失败或处理中 |
| 退款结果不明确           | 保持退款中，通过退款查询补偿           |

退款异常处理建议：

```text
先创建本地退款记录
    ↓
再调用支付宝退款接口
    ↓
支付宝返回成功则更新退款成功
    ↓
支付宝返回失败则记录失败原因
    ↓
网络异常或响应不明确则保持退款中
    ↓
后续通过退款查询接口补偿
```

退款处理中不要轻易删除本地退款记录。即使支付宝接口调用失败，也要保留退款记录、请求参数、响应内容和失败原因，便于后续重试或人工处理。

## 本地调试与联调

本地调试与联调用于在正式上线前验证支付链路是否正确。支付宝支付联调至少需要完成沙箱配置、内网穿透、支付跳转、异步通知、主动查询和退款测试。

本地调试建议准备以下内容：

| 项目           | 说明                           |
| -------------- | ------------------------------ |
| 沙箱应用       | 用于本地支付测试               |
| 沙箱买家账号   | 用于模拟用户付款               |
| 沙箱商家账号   | 用于查看收款和交易记录         |
| 应用私钥       | 本地服务请求加签使用           |
| 应用公钥证书   | 证书模式必需                   |
| 支付宝公钥证书 | 回调验签必需                   |
| 支付宝根证书   | 证书链校验必需                 |
| 内网穿透地址   | 用于支付宝异步通知访问本地服务 |

### 沙箱环境配置

沙箱环境用于在不产生真实资金交易的情况下调试支付宝支付流程。本地开发时建议单独使用 `application-dev.yml` 配置沙箱参数，避免误连正式环境。

文件位置：`src/main/resources/application-dev.yml`

该配置用于本地沙箱联调，证书路径和回调地址需要根据本机环境调整。

```yaml
spring:
  profiles:
    active: dev

pay:
  alipay:
    # 本地开发启用支付宝沙箱支付
    enabled: true

    # 沙箱应用 ID
    app-id: ${ALIPAY_APP_ID}

    # 沙箱网关地址
    gateway-url: https://openapi-sandbox.dl.alipaydev.com/gateway.do

    # 沙箱应用私钥
    private-key: ${ALIPAY_PRIVATE_KEY}

    # 请求格式
    format: json

    # 字符集
    charset: UTF-8

    # 签名类型
    sign-type: RSA2

    # 本地沙箱证书路径
    app-cert-path: ${ALIPAY_APP_CERT_PATH:/Users/ateng/cert/alipay/appCertPublicKey.crt}
    alipay-cert-path: ${ALIPAY_PUBLIC_CERT_PATH:/Users/ateng/cert/alipay/alipayCertPublicKey_RSA2.crt}
    alipay-root-cert-path: ${ALIPAY_ROOT_CERT_PATH:/Users/ateng/cert/alipay/alipayRootCert.crt}

    # 内网穿透后的异步通知地址
    notify-url: ${ALIPAY_NOTIFY_URL:https://你的公网域名/api/pay/alipay/notify}

    # 前端支付结果页地址
    return-url: ${ALIPAY_RETURN_URL:http://localhost:5173/pay/result}
```

本地启动前可以设置环境变量：

```bash
export ALIPAY_APP_ID="你的沙箱应用ID"
export ALIPAY_PRIVATE_KEY="你的沙箱应用私钥"
export ALIPAY_APP_CERT_PATH="/Users/ateng/cert/alipay/appCertPublicKey.crt"
export ALIPAY_PUBLIC_CERT_PATH="/Users/ateng/cert/alipay/alipayCertPublicKey_RSA2.crt"
export ALIPAY_ROOT_CERT_PATH="/Users/ateng/cert/alipay/alipayRootCert.crt"
export ALIPAY_NOTIFY_URL="https://你的公网域名/api/pay/alipay/notify"
export ALIPAY_RETURN_URL="http://localhost:5173/pay/result"
```

启动服务：

```bash
mvn clean package -DskipTests
java -jar target/springboot-alipay-demo.jar --spring.profiles.active=dev
```

上述命令用于打包并以开发环境启动项目。`--spring.profiles.active=dev` 会加载 `application-dev.yml`，需要确保沙箱证书文件和环境变量已经准备完成。

### 内网穿透配置

支付宝异步通知是支付宝服务器主动请求本地服务，因此 `notify_url` 必须是公网可访问地址。本地 `localhost` 或局域网 IP 无法被支付宝服务器访问，需要使用内网穿透工具将本地端口映射为公网 HTTPS 地址。

内网穿透配置目标如下：

```text
公网 HTTPS 地址
    ↓
内网穿透服务
    ↓
本地 SpringBoot 服务 8080 端口
    ↓
/api/pay/alipay/notify
```

假设内网穿透工具生成的公网地址为：

```text
https://pay-demo.example.com
```

则支付宝异步通知地址应配置为：

```text
https://pay-demo.example.com/api/pay/alipay/notify
```

本地验证回调接口是否可访问：

```bash
curl -X POST 'https://pay-demo.example.com/api/pay/alipay/notify'
```

如果接口能访问，但因为缺少支付宝通知参数返回 `fail`，说明公网地址已经能访问到本地服务。真正的验签和业务处理需要由支付宝沙箱支付完成后触发。

内网穿透联调需要注意：

| 注意项                   | 说明                                      |
| ------------------------ | ----------------------------------------- |
| 必须使用公网地址         | 支付宝无法访问 `localhost`                |
| 建议使用 HTTPS           | 避免浏览器或网关拦截                      |
| 路径必须与配置一致       | `notify-url` 要和 Controller 路径完全一致 |
| 本地服务不能频繁重启     | 支付过程中服务重启会导致回调失败          |
| 穿透域名变化后要更新配置 | 免费穿透地址可能每次启动变化              |
| 回调接口不能要求登录     | 支付宝服务器无法携带业务系统登录态        |

### 支付回调测试

支付回调测试用于验证支付宝异步通知能否被本地服务接收、验签和处理。回调测试不能只用手写 curl 参数模拟，因为支付宝通知包含签名，手写参数通常无法通过验签。

推荐测试方式如下：

```text
启动本地服务
    ↓
启动内网穿透
    ↓
配置 notify-url 为公网地址
    ↓
创建支付订单
    ↓
发起沙箱支付
    ↓
使用沙箱买家完成付款
    ↓
观察本地日志是否收到异步通知
    ↓
检查 pay_order 状态是否更新为 PAY_SUCCESS
    ↓
检查 pay_flow 是否记录 NOTIFY 流水
```

创建支付订单：

```bash
curl -X POST 'http://localhost:8080/api/pay/orders' \
  -H 'Content-Type: application/json' \
  -d '{
    "bizOrderNo": "BIZ202605070001",
    "subject": "支付宝沙箱测试订单",
    "body": "本地联调订单",
    "totalAmount": 0.01,
    "payScene": "PC",
    "expireMinutes": 30
  }'
```

发起电脑网站支付：

```bash
curl -X POST 'http://localhost:8080/api/pay/alipay/pay' \
  -H 'Content-Type: application/json' \
  -d '{
    "payOrderNo": "替换为创建支付订单返回的 payOrderNo",
    "payScene": "PC"
  }'
```

如果返回的是 HTML 表单，可以将返回内容保存为本地 HTML 文件并用浏览器打开，也可以由前端页面直接渲染。

支付完成后检查数据库：

```sql
SELECT pay_order_no, out_trade_no, trade_no, status, trade_status, total_amount, paid_amount, pay_time, notify_time
FROM pay_order
WHERE pay_order_no = '替换为支付订单号';

SELECT flow_no, pay_order_no, flow_type, success, error_code, error_message, created_at
FROM pay_flow
WHERE pay_order_no = '替换为支付订单号'
ORDER BY id DESC;
```

预期结果：

| 检查项                  | 预期               |
| ----------------------- | ------------------ |
| `pay_order.status`      | `PAY_SUCCESS`      |
| `pay_order.trade_no`    | 不为空             |
| `pay_order.paid_amount` | 与本地订单金额一致 |
| `pay_order.notify_time` | 不为空             |
| `pay_flow.flow_type`    | 存在 `NOTIFY` 流水 |
| 接口日志                | 打印支付成功日志   |

## 功能验证

功能验证用于确认支付模块在正常场景、重复通知、失败场景和退款场景下都能正确运行。支付系统测试不能只验证“能支付成功”，还要验证异常路径和幂等路径。

建议准备以下测试数据：

| 数据         | 示例               |
| ------------ | ------------------ |
| 业务订单号   | `BIZ202605070001`  |
| 支付金额     | `0.01`             |
| 支付场景     | `PC`、`WAP`、`APP` |
| 业务退款单号 | `RF202605070001`   |
| 支付过期时间 | 30 分钟            |
| 回调地址     | 内网穿透公网地址   |

### 正常支付验证

正常支付验证用于确认从创建支付订单到支付成功的完整链路。

验证步骤如下：

```text
创建业务订单
    ↓
创建支付订单
    ↓
发起支付宝支付
    ↓
用户在沙箱收银台完成付款
    ↓
支付宝发送异步通知
    ↓
系统验签并更新支付订单
    ↓
系统触发业务订单支付成功
    ↓
前端查询支付状态
```

验证接口调用顺序：

```bash
curl -X POST 'http://localhost:8080/api/pay/orders' \
  -H 'Content-Type: application/json' \
  -d '{
    "bizOrderNo": "BIZ202605070001",
    "subject": "正常支付验证订单",
    "body": "支付宝正常支付验证",
    "totalAmount": 0.01,
    "payScene": "PC",
    "expireMinutes": 30
  }'
curl -X POST 'http://localhost:8080/api/pay/alipay/pay' \
  -H 'Content-Type: application/json' \
  -d '{
    "payOrderNo": "替换为支付订单号",
    "payScene": "PC"
  }'
curl -X GET 'http://localhost:8080/api/pay/orders/替换为支付订单号/status'
```

数据库验证：

```sql
SELECT pay_order_no, biz_order_no, out_trade_no, trade_no, status, trade_status, total_amount, paid_amount, pay_time
FROM pay_order
WHERE pay_order_no = '替换为支付订单号';
```

预期结果：

| 验证项       | 预期                                      |
| ------------ | ----------------------------------------- |
| 支付订单创建 | 成功返回 `payOrderNo`                     |
| 支付发起     | PC/WAP 返回 HTML 表单，App 返回订单字符串 |
| 支付完成     | 支付宝沙箱显示支付成功                    |
| 异步通知     | 本地服务收到通知                          |
| 本地支付状态 | `PAY_SUCCESS`                             |
| 支付宝交易号 | `trade_no` 不为空                         |
| 支付流水     | 存在 `PAY`、`NOTIFY`、可选 `QUERY` 流水   |

### 重复回调验证

重复回调验证用于确认支付宝多次通知同一笔订单时，本地系统不会重复更新订单、重复发放权益或重复触发业务逻辑。

重复回调测试方式有两种：

| 方式             | 说明                                            |
| ---------------- | ----------------------------------------------- |
| 支付宝自动重试   | 故意让第一次回调返回 `fail`，观察支付宝后续重试 |
| 复用真实通知参数 | 保存一次真实通知参数，再重复请求回调接口        |

推荐使用真实通知参数测试重复回调。第一次支付成功后，可以从日志或 `pay_flow.request_body` 中复制支付宝通知参数，再重复请求回调接口。

重复回调验证重点如下：

| 验证项       | 预期                                 |
| ------------ | ------------------------------------ |
| 第一次回调   | 订单从 `PAYING` 更新为 `PAY_SUCCESS` |
| 第二次回调   | 订单保持 `PAY_SUCCESS`               |
| 支付成功事件 | 只触发一次                           |
| 业务订单状态 | 不重复更新                           |
| 权益发放     | 不重复发放                           |
| 回调返回     | 重复成功通知仍返回 `success`         |
| 通知流水     | 可以记录多条 `NOTIFY` 流水           |

数据库验证：

```sql
SELECT pay_order_no, status, notify_count, pay_time, updated_at
FROM pay_order
WHERE pay_order_no = '替换为支付订单号';

SELECT flow_type, success, COUNT(*) AS total
FROM pay_flow
WHERE pay_order_no = '替换为支付订单号'
GROUP BY flow_type, success;
```

预期结果：

```text
pay_order.status = PAY_SUCCESS
pay_order.notify_count >= 1
pay_flow 中允许存在多条 NOTIFY 流水
业务订单支付成功处理只执行一次
```

重复回调测试时，重点不是阻止支付宝重复通知，而是保证重复通知不会造成重复业务效果。

### 支付失败验证

支付失败验证用于确认支付模块在异常场景下不会错误更新订单状态，也不会误判支付成功。

建议验证以下失败场景：

| 场景           | 操作方式                   | 预期结果                               |
| -------------- | -------------------------- | -------------------------------------- |
| 用户取消支付   | 打开收银台后取消付款       | 本地订单保持 `PAYING` 或最终查询为关闭 |
| 订单过期       | 设置较短过期时间后再支付   | 本地订单更新为 `PAY_CLOSED`            |
| 金额不一致     | 修改本地订单金额后模拟通知 | 回调返回 `fail`，订单不更新成功        |
| 验签失败       | 使用伪造参数请求回调       | 回调返回 `fail`，记录验签失败日志      |
| AppId 不一致   | 修改通知中的 `app_id`      | 回调返回 `fail`                        |
| 支付宝接口异常 | 配置错误网关或证书路径     | 支付发起失败，返回统一异常             |

伪造回调请求示例：

```bash
curl -X POST 'http://localhost:8080/api/pay/alipay/notify' \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'app_id=fake_app_id&out_trade_no=fake_out_trade_no&trade_status=TRADE_SUCCESS&total_amount=0.01'
```

预期结果：

```text
接口返回 fail
日志出现验签失败
pay_order 不更新为 PAY_SUCCESS
pay_flow 可以记录失败通知流水
```

支付失败后可以通过主动查询确认支付宝最终状态：

```bash
curl -X GET 'http://localhost:8080/api/pay/orders/替换为支付订单号/status'
```

如果支付宝返回 `TRADE_CLOSED`，本地订单应更新为 `PAY_CLOSED`。如果仍为 `WAIT_BUYER_PAY`，本地可以保持 `PAYING`，后续由定时任务继续补偿。

### 退款流程验证

退款流程验证用于确认支付成功订单可以正常发起退款，并正确更新退款记录和支付订单累计退款金额。

验证步骤如下：

```text
完成一笔正常支付
    ↓
确认 pay_order.status = PAY_SUCCESS
    ↓
提交退款申请
    ↓
调用支付宝退款接口
    ↓
更新 pay_refund 状态
    ↓
更新 pay_order 累计退款金额
    ↓
按需查询退款结果
```

申请退款：

```bash
curl -X POST 'http://localhost:8080/api/pay/refunds' \
  -H 'Content-Type: application/json' \
  -d '{
    "payOrderNo": "替换为支付订单号",
    "bizRefundNo": "RF202605070001",
    "refundAmount": 0.01,
    "refundReason": "退款流程验证"
  }'
```

查询退款记录：

```sql
SELECT refund_no, biz_refund_no, pay_order_no, out_trade_no, out_request_no, refund_amount, status, refund_time, fail_code, fail_message
FROM pay_refund
WHERE biz_refund_no = 'RF202605070001';

SELECT pay_order_no, status, total_amount, refund_amount
FROM pay_order
WHERE pay_order_no = '替换为支付订单号';
```

预期结果：

| 验证项               | 预期                                                |
| -------------------- | --------------------------------------------------- |
| 退款接口返回         | `REFUND_SUCCESS` 或 `REFUNDING`                     |
| 退款记录             | `pay_refund` 生成一条记录                           |
| 退款请求号           | `out_request_no` 不为空且唯一                       |
| 支付订单累计退款金额 | `refund_amount` 增加                                |
| 全额退款             | 支付订单可更新为 `REFUND_SUCCESS`                   |
| 部分退款             | 支付订单可保持 `REFUNDING` 或业务定义的部分退款状态 |
| 支付流水             | 存在 `REFUND` 流水                                  |

重复退款验证：

```bash
curl -X POST 'http://localhost:8080/api/pay/refunds' \
  -H 'Content-Type: application/json' \
  -d '{
    "payOrderNo": "替换为支付订单号",
    "bizRefundNo": "RF202605070001",
    "refundAmount": 0.01,
    "refundReason": "重复退款验证"
  }'
```

预期结果：

```text
不会重新创建退款记录
不会重复调用支付宝产生新退款
返回已有退款单状态
pay_refund 中 biz_refund_no 仍只有一条记录
```

退款异常验证：

| 场景                 | 预期                       |
| -------------------- | -------------------------- |
| 未支付订单退款       | 返回当前订单状态不允许退款 |
| 退款金额大于可退金额 | 返回退款金额非法           |
| 重复业务退款单号     | 返回已有退款状态           |
| 伪造支付订单号       | 返回支付订单不存在         |
| 支付宝退款失败       | 退款记录保存失败原因       |

最终功能验证通过标准如下：

```text
正常支付可以完成
异步通知可以验签并更新订单
重复通知不会重复处理业务
伪造通知不会更新订单
支付查询可以补偿订单状态
退款可以生成退款记录
重复退款不会造成重复资金退回
异常场景有明确日志和统一错误响应
```
