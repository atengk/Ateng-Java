# Spring Boot 微信支付开发

## 功能概述

本章节用于说明 Spring Boot 3 项目接入微信支付的整体目标、适用场景、支付能力范围和开发边界。微信支付模块通常不单独存在，而是与订单、库存、会员、营销、退款、对账和消息通知等业务模块协同工作。

在后端系统中，微信支付的核心职责是完成支付订单创建、支付参数生成、支付状态同步、异步回调处理、订单状态变更和异常补偿。前端只负责发起支付动作和展示支付结果，最终支付结果应以后端接收到的微信支付回调或主动查询结果为准。

### 业务场景

微信支付适用于电商交易、会员充值、内容付费、课程购买、服务预约、扫码收款、小程序商城、公众号商城、App 交易等在线支付场景。不同业务入口对应不同的支付产品，但后端核心流程基本一致。

典型业务流程如下：

| 流程节点     | 说明                                                         |
| ------------ | ------------------------------------------------------------ |
| 创建业务订单 | 用户提交商品、金额、支付方式等信息，系统生成本地业务订单     |
| 创建支付订单 | 后端根据业务订单调用微信支付下单接口，生成微信支付预支付信息 |
| 返回支付参数 | 后端将 JSAPI、小程序、APP、H5 或 Native 所需支付参数返回前端 |
| 用户完成支付 | 用户在微信、浏览器、App 或扫码页面完成付款                   |
| 接收支付回调 | 微信支付通过异步通知地址向后端推送支付结果                   |
| 验签与解密   | 后端校验回调签名，并解密回调资源数据                         |
| 更新业务状态 | 后端更新订单状态、支付流水和业务权益                         |
| 异常补偿     | 回调失败或状态不一致时，通过主动查询、关闭订单等方式补偿     |

支付结果不能只依赖前端页面跳转或前端支付成功回调。前端支付成功只能说明用户侧支付流程完成，不能作为后端发货、开通会员、发放权益的最终依据。后端必须基于微信支付回调或订单查询结果更新业务状态。

### 支付能力范围

本文档面向 Spring Boot 3 后端服务，主要覆盖微信支付 API v3 的常见接入能力。根据业务入口不同，可以选择不同支付方式。

| 支付方式    | 适用场景                      | 后端主要返回内容           |
| ----------- | ----------------------------- | -------------------------- |
| JSAPI 支付  | 微信公众号、微信内网页        | 前端调起支付所需参数       |
| 小程序支付  | 微信小程序商城、预约服务      | 小程序调起支付所需参数     |
| Native 支付 | PC 网站、后台收银台、扫码支付 | 二维码链接 `code_url`      |
| H5 支付     | 手机浏览器、非微信内网页      | H5 支付跳转链接 `mweb_url` |
| APP 支付    | Android / iOS 原生 App        | App 调起微信支付所需参数   |

本文档覆盖以下能力：

| 能力         | 说明                                             |
| ------------ | ------------------------------------------------ |
| 创建支付订单 | 根据本地订单调用微信支付下单接口                 |
| 生成支付参数 | 根据支付方式生成前端调起支付所需参数             |
| 查询支付订单 | 主动查询微信支付订单状态，用于页面刷新或异常补偿 |
| 关闭支付订单 | 对超时未支付订单发起关闭                         |
| 接收支付回调 | 接收微信支付异步通知                             |
| 回调验签     | 校验回调来源是否合法                             |
| 回调解密     | 解密微信支付回调中的资源数据                     |
| 幂等处理     | 防止重复回调导致订单重复处理                     |
| 支付流水记录 | 记录每一笔支付请求和支付结果                     |
| 退款申请     | 对已支付订单发起退款                             |
| 退款查询     | 查询退款单状态                                   |
| 退款回调处理 | 接收并处理退款结果通知                           |

支付模块的重点不只是“调起微信支付”，还包括订单状态一致性、支付流水完整性、回调安全性和异常补偿能力。生产环境中，支付模块应具备可追踪、可重试、可审计和可告警能力。

### 开发边界

微信支付接入需要明确后端系统、前端系统、微信支付平台和运维配置之间的职责边界。边界清晰后，支付流程更容易维护，也能降低订单状态错乱和重复处理风险。

后端系统负责：

1. 生成本地业务订单和商户订单号。
2. 校验订单金额、用户身份、订单状态和支付方式。
3. 调用微信支付下单、查询、关闭、退款等接口。
4. 生成前端调起支付所需参数。
5. 接收微信支付异步回调。
6. 完成回调验签、解密和参数校验。
7. 幂等更新订单状态和支付流水。
8. 记录回调日志、异常日志和支付操作日志。
9. 对异常订单执行主动查询、关闭或人工补偿。

前端系统负责：

1. 展示商品、订单和支付页面。
2. 调用后端创建支付订单接口。
3. 根据后端返回参数调起微信支付。
4. 展示支付中、支付成功或支付失败页面。
5. 支付完成后调用后端订单查询接口刷新真实订单状态。

微信支付平台负责：

1. 接收商户下单请求。
2. 创建微信支付交易。
3. 完成用户扣款。
4. 向商户后端发送支付结果通知。
5. 提供订单查询、关闭订单、退款和退款查询能力。

本文档不覆盖以下内容：

1. 微信支付商户开户注册流程。
2. 公众号、小程序、App 前端完整工程实现。
3. 多商户服务商模式下的进件、分账和特约商户管理。
4. 财务对账平台、清结算系统和资金报表系统建设。
5. 支付风控、反欺诈和交易风险识别策略。
6. 用户银行卡、支付密码、身份证等敏感金融数据处理。

支付模块应避免与具体业务过度耦合。推荐将支付订单、支付流水、回调日志、退款流水作为独立数据模型维护，再通过业务订单号与订单模块关联。

## 环境准备

本章节用于说明接入微信支付前需要准备的商户配置、应用配置、证书文件和密钥信息。环境准备是否完整，直接影响后续下单、验签、回调解密和退款功能是否能够正常运行。

在正式开发前，需要确认商户号、AppID、商户 API 私钥、商户证书序列号、API v3 密钥、支付通知地址和退款通知地址均已准备完成。

### 微信支付商户配置

微信支付商户配置主要在微信支付商户平台完成。后端开发需要从商户平台获取必要参数，并确认当前商户已经开通对应支付产品。

需要准备的商户参数如下：

| 参数                | 示例                                               | 说明                                  |
| ------------------- | -------------------------------------------------- | ------------------------------------- |
| 商户号              | `1900000000`                                       | 微信支付分配的商户编号                |
| AppID               | `wx1234567890abcdef`                               | 公众号、小程序、App 或开放平台应用 ID |
| 商户 API 证书序列号 | `5157F09EFD******`                                 | 商户 API 证书对应的序列号             |
| 商户 API 私钥       | `apiclient_key.pem`                                | 请求微信支付接口时用于签名            |
| API v3 密钥         | `32 位字符串`                                      | 用于回调解密、平台证书下载等场景      |
| 支付通知地址        | `https://api.example.com/api/pay/wx/notify`        | 支付结果异步通知地址                  |
| 退款通知地址        | `https://api.example.com/api/pay/wx/refund/notify` | 退款结果异步通知地址                  |

商户平台需要重点检查以下配置：

1. 已开通需要使用的支付产品，例如 JSAPI 支付、Native 支付、H5 支付、APP 支付。
2. 商户号与 AppID 已完成绑定。
3. 已申请并下载商户 API 证书。
4. 已获取商户 API 证书序列号。
5. 已设置 API v3 密钥。
6. 已配置支付回调地址和退款回调地址。
7. 回调域名支持公网访问，并启用有效 HTTPS 证书。
8. H5 支付场景下，已配置 H5 支付域名。
9. JSAPI 和小程序支付场景下，用户 `openid` 来源与 AppID 保持一致。

回调地址必须是公网可访问的 HTTPS 地址。本地开发阶段可以使用内网穿透工具进行联调，但生产环境不建议依赖临时穿透域名。

### 应用参数配置

Spring Boot 3 项目建议将微信支付参数集中维护在配置文件中，并通过配置类注入业务代码。商户号、AppID、证书路径、回调地址等可以写入环境配置；API v3 密钥、私钥文件等敏感信息不应硬编码在 Java 代码中。

文件位置：`src/main/resources/application.yml`

```yaml
wechat:
  pay:
    # 微信支付商户号
    mch-id: "1900000000"

    # 公众号、小程序或 App 对应的 AppID
    app-id: "wx1234567890abcdef"

    # 商户 API 证书序列号
    merchant-serial-number: "5157F09EFD******"

    # 商户 API 私钥文件路径
    private-key-path: "/data/cert/wechat/apiclient_key.pem"

    # API v3 密钥，生产环境建议使用环境变量注入
    api-v3-key: "${WECHAT_PAY_API_V3_KEY}"

    # 支付结果通知地址
    pay-notify-url: "https://api.example.com/api/pay/wx/notify"

    # 退款结果通知地址
    refund-notify-url: "https://api.example.com/api/pay/wx/refund/notify"

    # 支付订单默认过期时间，单位：分钟
    order-expire-minutes: 30
```

不同环境建议使用独立配置文件隔离：

| 环境     | 配置文件               | 说明                       |
| -------- | ---------------------- | -------------------------- |
| 开发环境 | `application-dev.yml`  | 用于本地开发和接口调试     |
| 测试环境 | `application-test.yml` | 用于测试商户或测试域名联调 |
| 预发环境 | `application-pre.yml`  | 用于上线前验证             |
| 生产环境 | `application-prod.yml` | 用于正式支付交易           |

生产环境推荐通过环境变量注入 API v3 密钥：

```bash
# 设置微信支付 API v3 密钥
export WECHAT_PAY_API_V3_KEY="xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"

# 启动 Spring Boot 服务并指定生产环境配置
java -jar app.jar --spring.profiles.active=prod
```

`WECHAT_PAY_API_V3_KEY` 用于注入微信支付 API v3 密钥，避免密钥明文出现在配置文件和代码仓库中。实际部署时，也可以通过 Kubernetes Secret、Docker 环境变量、配置中心或密钥管理系统进行注入。

应用配置需要遵循以下原则：

1. 商户号、AppID、回调地址按环境隔离。
2. API v3 密钥不提交到代码仓库。
3. 商户 API 私钥使用文件挂载，不写入源码。
4. 回调地址使用 HTTPS，并保证公网可访问。
5. 支付超时时间应与业务订单超时时间保持一致。
6. 测试环境和生产环境参数必须明确区分，避免误调真实支付。

### 证书与密钥准备

微信支付 API v3 接入过程中，证书和密钥主要用于请求签名、响应验签、回调验签和回调解密。证书与密钥属于高敏感配置，必须独立于代码进行管理。

需要准备的证书和密钥如下：

| 名称                   | 用途                       | 管理方式                    |
| ---------------------- | -------------------------- | --------------------------- |
| 商户 API 私钥          | 请求微信支付接口时生成签名 | 以文件方式挂载到服务器      |
| 商户 API 证书序列号    | 标识当前商户证书           | 写入环境配置或配置中心      |
| API v3 密钥            | 解密回调资源数据           | 使用环境变量或密钥系统注入  |
| 微信支付平台证书或公钥 | 验证微信支付响应和回调签名 | 由 SDK 自动管理或配置化管理 |

推荐服务器目录结构如下：

```bash
# 创建微信支付证书目录
mkdir -p /data/cert/wechat

# 将商户 API 私钥文件上传到该目录
# 示例路径：/data/cert/wechat/apiclient_key.pem

# 设置证书目录权限
chmod 700 /data/cert/wechat

# 设置商户 API 私钥文件权限
chmod 600 /data/cert/wechat/apiclient_key.pem
```

`/data/cert/wechat` 是微信支付证书目录；`apiclient_key.pem` 是商户 API 私钥文件；`chmod 700` 用于限制目录访问权限；`chmod 600` 用于限制私钥文件读取权限。运行 Spring Boot 服务的系统用户必须具备读取该私钥文件的权限。

证书与密钥管理需要注意以下事项：

1. 不要将 `apiclient_key.pem` 提交到 Git、SVN 或制品仓库。
2. 不要在 Controller、Service、常量类或前端代码中硬编码 API v3 密钥。
3. 不要在日志中打印私钥内容、API v3 密钥或完整敏感回调报文。
4. 商户 API 证书轮换后，需要同步更新私钥文件和证书序列号。
5. 生产环境建议使用独立账号管理证书文件权限。
6. 多环境部署时，测试商户证书和生产商户证书必须隔离。
7. 支付客户端建议作为 Spring Bean 统一初始化并复用，避免重复加载证书和重复创建客户端对象。

本地或测试环境可以通过以下命令检查证书文件和回调地址：

```bash
# 检查商户 API 私钥文件是否存在
ls -l /data/cert/wechat/apiclient_key.pem

# 检查当前用户是否具备读取权限
head -n 5 /data/cert/wechat/apiclient_key.pem

# 检查支付回调地址是否可以访问
curl -I https://api.example.com/api/pay/wx/notify
```

`ls -l` 用于确认私钥文件存在且权限正确；`head -n 5` 仅建议在本地或测试环境用于权限检查，生产环境排查时不要输出私钥内容；`curl -I` 用于确认回调地址的 HTTPS 访问状态。



## 项目依赖与基础配置

本章节用于完成 Spring Boot 3 项目接入微信支付前的基础工程配置，包括 Maven 依赖、支付参数配置、配置属性绑定和微信支付客户端初始化。建议将微信支付 SDK 客户端作为 Spring Bean 统一管理，避免在业务代码中重复创建客户端对象。

微信支付 API v3 官方 Java SDK 当前 Maven Central 可用版本为 `0.2.17`；该 SDK 由 `core` 和 `service` 组成，`core` 提供自动签名、验签、回调处理和加解密能力，`service` 提供业务接口和示例代码。SDK 示例也说明，使用 SDK 后通常不需要业务代码手动计算请求签名和验证应答签名。([GitHub](https://github.com/wechatpay-apiv3/wechatpay-java?utm_source=chatgpt.com))

### Maven 依赖配置

Maven 依赖用于引入 Spring Boot Web 能力、参数校验能力、微信支付官方 SDK、Hutool 工具类和 Lombok。支付模块建议保持依赖清晰，不要将订单、库存、会员等业务依赖混入支付基础配置中。

文件位置：`pom.xml`

```xml
<dependencies>
    <!-- Spring Boot Web：提供 Controller、JSON 序列化、Servlet 请求处理能力 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Spring Boot 参数校验：用于校验支付请求 DTO 和配置参数 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- 微信支付 API v3 官方 Java SDK：用于下单、查单、关单、退款、签名、验签和回调解密 -->
    <dependency>
        <groupId>com.github.wechatpay-apiv3</groupId>
        <artifactId>wechatpay-java</artifactId>
        <version>0.2.17</version>
    </dependency>

    <!-- Hutool：用于字符串、对象、日期、JSON 等常用工具处理 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.8.35</version>
    </dependency>

    <!-- Lombok：简化 Getter、Setter、日志对象等样板代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- Spring Boot 测试依赖：用于支付参数、回调处理和业务服务单元测试 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

如果项目已经统一管理依赖版本，可以将微信支付 SDK 版本放入 `<properties>` 中，便于后续升级。

```xml
<properties>
    <!-- 微信支付 API v3 官方 Java SDK 版本 -->
    <wechatpay-java.version>0.2.17</wechatpay-java.version>

    <!-- Hutool 工具类版本 -->
    <hutool.version>5.8.35</hutool.version>
</properties>

<dependencies>
    <!-- 微信支付 API v3 官方 Java SDK -->
    <dependency>
        <groupId>com.github.wechatpay-apiv3</groupId>
        <artifactId>wechatpay-java</artifactId>
        <version>${wechatpay-java.version}</version>
    </dependency>

    <!-- Hutool 工具类 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>${hutool.version}</version>
    </dependency>
</dependencies>
```

微信支付 SDK 版本升级时，需要重点回归下单、查单、关单、支付回调、退款和退款回调流程。支付相关依赖不建议自动使用快照版本或不受控升级版本。

### 支付参数配置

支付参数配置用于承载商户号、AppID、证书序列号、商户私钥路径、API v3 密钥和回调地址等信息。该配置应按环境隔离，敏感信息不应提交到代码仓库。

文件位置：`src/main/resources/application.yml`

```yaml
wechat:
  pay:
    # 微信支付商户号
    mch-id: "1900000000"

    # 公众号、小程序或 App 对应的 AppID
    app-id: "wx1234567890abcdef"

    # 商户 API 证书序列号
    merchant-serial-number: "5157F09EFD******"

    # 商户 API 私钥文件路径
    private-key-path: "/data/cert/wechat/apiclient_key.pem"

    # API v3 密钥，生产环境建议通过环境变量注入
    api-v3-key: "${WECHAT_PAY_API_V3_KEY}"

    # 支付结果通知地址，必须是公网 HTTPS 地址
    pay-notify-url: "https://api.example.com/api/pay/wx/notify"

    # 退款结果通知地址，必须是公网 HTTPS 地址
    refund-notify-url: "https://api.example.com/api/pay/wx/refund/notify"

    # 本地订单默认过期时间，单位：分钟
    order-expire-minutes: 30
```

配置项说明如下：

| 配置项                   | 是否必填 | 说明                            |
| ------------------------ | -------- | ------------------------------- |
| `mch-id`                 | 是       | 微信支付商户号                  |
| `app-id`                 | 是       | 公众号、小程序或 App 的 AppID   |
| `merchant-serial-number` | 是       | 商户 API 证书序列号             |
| `private-key-path`       | 是       | 商户 API 私钥文件路径           |
| `api-v3-key`             | 是       | API v3 密钥，用于回调解密等场景 |
| `pay-notify-url`         | 是       | 支付结果通知地址                |
| `refund-notify-url`      | 是       | 退款结果通知地址                |
| `order-expire-minutes`   | 是       | 本地订单超时时间                |

推荐为微信支付参数创建独立配置属性类，统一完成参数绑定和基础校验。

文件位置：`src/main/java/io/github/atengk/payment/config/WechatPayProperties.java`

下面的配置类用于绑定 `wechat.pay` 配置，并在项目启动时校验关键参数是否完整。

```java
package io.github.atengk.payment.config;

import cn.hutool.core.util.StrUtil;
import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 微信支付配置属性
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Data
@Validated
@ConfigurationProperties(prefix = "wechat.pay")
public class WechatPayProperties {

    /**
     * 微信支付商户号
     */
    @NotBlank(message = "微信支付商户号不能为空")
    private String mchId;

    /**
     * 微信应用 AppID
     */
    @NotBlank(message = "微信支付 AppID 不能为空")
    private String appId;

    /**
     * 商户 API 证书序列号
     */
    @NotBlank(message = "商户 API 证书序列号不能为空")
    private String merchantSerialNumber;

    /**
     * 商户 API 私钥路径
     */
    @NotBlank(message = "商户 API 私钥路径不能为空")
    private String privateKeyPath;

    /**
     * API v3 密钥
     */
    @NotBlank(message = "微信支付 API v3 密钥不能为空")
    private String apiV3Key;

    /**
     * 支付通知地址
     */
    @NotBlank(message = "微信支付通知地址不能为空")
    private String payNotifyUrl;

    /**
     * 退款通知地址
     */
    @NotBlank(message = "微信退款通知地址不能为空")
    private String refundNotifyUrl;

    /**
     * 订单过期分钟数
     */
    @Min(value = 1, message = "订单过期时间不能小于 1 分钟")
    private Integer orderExpireMinutes = 30;

    /**
     * 校验微信支付核心配置
     */
    @PostConstruct
    public void validateConfig() {
        if (StrUtil.length(apiV3Key) != 32) {
            throw new IllegalArgumentException("微信支付 API v3 密钥长度必须为 32 位");
        }
    }
}
```

生产环境中，`api-v3-key` 建议通过环境变量、Kubernetes Secret、Docker Secret、配置中心或密钥管理系统注入。商户私钥文件建议通过挂载方式提供给应用读取。

### 支付客户端初始化

支付客户端初始化用于创建微信支付 SDK 的基础 `Config` 和各支付服务对象。官方文档建议将 `RSAAutoCertificateConfig` 作为单例或全局对象复用，避免重复下载平台证书和浪费系统资源；该配置类可自动下载并定时更新微信支付平台证书。([pay.wechatpay.cn](https://pay.wechatpay.cn/doc/v3/partner/4012083111?utm_source=chatgpt.com))

推荐的基础文件结构如下：

```text
src/main/java/io/github/atengk/payment
├── config
│   ├── WechatPayProperties.java
│   └── WechatPayClientConfig.java
├── controller
│   └── WxPayController.java
├── service
│   └── WxPayService.java
└── service/impl
    └── WxPayServiceImpl.java
```

文件位置：`src/main/java/io/github/atengk/payment/config/WechatPayClientConfig.java`

下面的配置类用于初始化微信支付基础配置，并将 Native、JSAPI、JSAPI 扩展服务注册为 Spring Bean。后续如果需要 H5、APP、退款能力，可以在该配置类中继续添加对应 Service Bean。

```java
package io.github.atengk.payment.config;

import com.wechat.pay.java.core.Config;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.service.payments.jsapi.JsapiService;
import com.wechat.pay.java.service.payments.jsapi.JsapiServiceExtension;
import com.wechat.pay.java.service.payments.nativepay.NativePayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 微信支付客户端配置
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(WechatPayProperties.class)
public class WechatPayClientConfig {

    private final WechatPayProperties properties;

    /**
     * 创建微信支付基础配置
     *
     * @return 微信支付配置对象
     */
    @Bean
    public Config wechatPayConfig() {
        log.info("初始化微信支付配置，商户号：{}，AppID：{}", properties.getMchId(), properties.getAppId());

        return new RSAAutoCertificateConfig.Builder()
                .merchantId(properties.getMchId())
                .privateKeyFromPath(properties.getPrivateKeyPath())
                .merchantSerialNumber(properties.getMerchantSerialNumber())
                .apiV3Key(properties.getApiV3Key())
                .build();
    }

    /**
     * 创建 Native 支付服务
     *
     * @param wechatPayConfig 微信支付配置对象
     * @return Native 支付服务
     */
    @Bean
    public NativePayService nativePayService(Config wechatPayConfig) {
        return new NativePayService.Builder()
                .config(wechatPayConfig)
                .build();
    }

    /**
     * 创建 JSAPI 支付服务
     *
     * @param wechatPayConfig 微信支付配置对象
     * @return JSAPI 支付服务
     */
    @Bean
    public JsapiService jsapiService(Config wechatPayConfig) {
        return new JsapiService.Builder()
                .config(wechatPayConfig)
                .build();
    }

    /**
     * 创建 JSAPI 支付扩展服务
     *
     * @param wechatPayConfig 微信支付配置对象
     * @return JSAPI 支付扩展服务
     */
    @Bean
    public JsapiServiceExtension jsapiServiceExtension(Config wechatPayConfig) {
        return new JsapiServiceExtension.Builder()
                .config(wechatPayConfig)
                .build();
    }
}
```

如果业务同时支持 H5、APP、退款，可以继续扩展以下 Bean。实际项目中只需要注册当前业务使用的支付服务，避免无意义初始化。

```java
// H5 支付服务
// import com.wechat.pay.java.service.payments.h5.H5Service;

// APP 支付服务
// import com.wechat.pay.java.service.payments.app.AppService;
// import com.wechat.pay.java.service.payments.app.AppServiceExtension;

// 退款服务
// import com.wechat.pay.java.service.refund.RefundService;
```

客户端初始化完成后，可以在 Service 层直接注入对应支付服务：

```java
private final NativePayService nativePayService;
private final JsapiServiceExtension jsapiServiceExtension;
private final WechatPayProperties wechatPayProperties;
```

支付客户端初始化注意事项：

1. `Config` 建议作为全局 Bean 复用。
2. 不要在每次下单、查单、回调时重复创建 `RSAAutoCertificateConfig`。
3. 商户私钥路径必须保证应用运行用户可读取。
4. API v3 密钥必须为 32 位字符串。
5. 初始化失败通常与私钥路径错误、证书序列号错误、API v3 密钥错误或网络无法访问微信支付平台有关。
6. 多商户模式下，不应使用单一全局配置，需要按商户号维护不同的支付客户端配置。

## 支付流程设计

本章节用于说明微信支付在后端系统中的核心业务流程，包括下单流程、支付状态流转、支付回调流程和订单关闭流程。支付流程设计的重点是保证订单状态可靠、支付结果可信、重复通知可幂等、异常场景可补偿。

支付模块建议至少维护三类数据：业务订单、支付流水和回调日志。业务订单用于承载商品和权益状态；支付流水用于记录支付渠道交易信息；回调日志用于记录微信支付通知内容和处理结果。

### 下单流程

下单流程用于根据本地业务订单创建微信支付预支付订单，并将前端调起支付所需参数返回给调用方。下单接口需要保证商户订单号唯一、支付金额可信、订单状态可控。

推荐下单流程如下：

```text
用户提交支付请求
        |
        v
校验用户身份、订单归属、订单金额、订单状态
        |
        v
创建或读取本地业务订单
        |
        v
创建本地支付流水，生成 out_trade_no
        |
        v
调用微信支付下单接口
        |
        v
保存微信支付预支付结果
        |
        v
返回前端调起支付参数
```

下单时需要重点处理以下规则：

| 规则               | 说明                                                         |
| ------------------ | ------------------------------------------------------------ |
| 金额以后端为准     | 前端只能传订单号或商品信息，最终支付金额必须由后端计算       |
| 商户订单号唯一     | `out_trade_no` 必须全局唯一，建议包含业务前缀、时间和序列号  |
| 避免重复下单       | 同一业务订单未关闭、未失败、未过期时，不应重复创建多笔有效支付流水 |
| 控制订单有效期     | 本地订单过期时间应与微信支付订单关闭策略保持一致             |
| 外部调用不放长事务 | 调用微信支付接口不建议包裹在长数据库事务中                   |
| 支付参数按端返回   | JSAPI、小程序、APP、H5、Native 返回参数不同，需要按支付方式封装 |

本地商户订单号示例：

```text
业务订单号：ORDER202605070001
支付流水号：PAY202605070001
微信商户订单号：WX202605070001
```

推荐下单接口返回结构：

```json
{
  "orderNo": "ORDER202605070001",
  "payNo": "PAY202605070001",
  "outTradeNo": "WX202605070001",
  "payType": "NATIVE",
  "payStatus": "WAIT_PAY",
  "codeUrl": "weixin://wxpay/bizpayurl?pr=xxxxxx",
  "expireTime": "2026-05-07 15:30:00"
}
```

不同支付方式的返回内容应保持统一外层结构，差异参数放到扩展字段中。例如 Native 返回 `codeUrl`，H5 返回 `mwebUrl`，JSAPI 和小程序返回调起支付参数，APP 返回 App 端调起参数。

### 支付状态流转

支付状态流转用于描述本地订单状态和微信支付交易状态之间的映射关系。业务系统不能直接照搬微信支付状态，应根据自身业务建立稳定的本地支付状态模型。

推荐本地支付状态如下：

| 本地状态           | 状态说明 | 触发场景                               |
| ------------------ | -------- | -------------------------------------- |
| `WAIT_PAY`         | 待支付   | 本地订单创建成功，尚未完成支付         |
| `PAYING`           | 支付中   | 已创建微信预支付订单，等待用户付款     |
| `PAID`             | 已支付   | 微信支付回调或主动查询确认支付成功     |
| `CLOSED`           | 已关闭   | 超时未支付或主动关闭成功               |
| `PAY_FAILED`       | 支付失败 | 微信明确返回支付失败，或业务侧判定失败 |
| `REFUNDING`        | 退款中   | 已发起退款，等待退款结果               |
| `PARTIAL_REFUNDED` | 部分退款 | 已完成部分金额退款                     |
| `REFUNDED`         | 已退款   | 已完成全额退款                         |

常见状态流转如下：

```text
WAIT_PAY
   |
   v
PAYING -----> CLOSED
   |
   v
PAID -----> REFUNDING -----> PARTIAL_REFUNDED
   |                              |
   |                              v
   └-------------------------> REFUNDED
```

本地状态与微信支付状态可以按以下方式映射：

| 微信支付状态 | 本地建议状态          | 处理方式                         |
| ------------ | --------------------- | -------------------------------- |
| `SUCCESS`    | `PAID`                | 支付成功，更新订单和支付流水     |
| `NOTPAY`     | `PAYING`              | 用户尚未支付，继续等待或后续关闭 |
| `USERPAYING` | `PAYING`              | 用户支付中，建议稍后查询         |
| `CLOSED`     | `CLOSED`              | 订单已关闭，本地同步关闭         |
| `REVOKED`    | `CLOSED`              | 订单已撤销，按关闭处理           |
| `PAYERROR`   | `PAY_FAILED`          | 支付失败，记录异常原因           |
| `REFUND`     | `PAID` 或 `REFUNDING` | 结合退款单状态判断最终状态       |

状态流转需要遵循以下原则：

1. 已支付订单不能被支付回调覆盖为未支付。
2. 已关闭订单如果收到支付成功回调，需要进入异常处理或主动查单确认。
3. 回调和主动查询都可能更新订单状态，但必须共用同一套状态变更规则。
4. 状态更新必须带条件，例如只允许 `PAYING -> PAID`，避免重复回调导致脏写。
5. 订单状态和支付流水状态需要同时维护，不能只更新业务订单。
6. 支付成功后发放权益、扣减库存、发送消息应保证幂等。

### 支付回调流程

支付回调流程用于接收微信支付异步通知，并根据通知结果更新本地订单状态。回调处理必须保证安全性、幂等性和可追踪性。

推荐支付回调流程如下：

```text
微信支付发送回调通知
        |
        v
接收请求头和请求体
        |
        v
记录原始回调日志
        |
        v
校验请求签名
        |
        v
解密 resource 资源数据
        |
        v
解析微信支付订单结果
        |
        v
校验商户订单号、商户号、金额
        |
        v
判断本地订单状态
        |
        v
幂等更新支付流水和业务订单
        |
        v
返回成功应答给微信支付
```

回调处理需要重点校验以下内容：

| 校验项               | 说明                                   |
| -------------------- | -------------------------------------- |
| 签名是否合法         | 防止伪造回调                           |
| 商户号是否一致       | 校验回调中的商户号是否为当前系统商户号 |
| 商户订单号是否存在   | 根据 `out_trade_no` 查询本地支付流水   |
| 支付金额是否一致     | 微信支付金额必须与本地订单金额一致     |
| 订单状态是否允许变更 | 已支付、已退款等终态不能重复处理       |
| 微信交易号是否为空   | 支付成功时应记录 `transaction_id`      |
| 回调事件是否重复     | 同一通知或同一支付流水只能成功处理一次 |

回调接口成功时，应返回微信支付要求的成功响应。业务处理失败时，不应返回成功，否则微信支付可能认为通知已处理完成，导致后续不再重试。

推荐成功响应结构：

```json
{
  "code": "SUCCESS",
  "message": "成功"
}
```

推荐失败响应结构：

```json
{
  "code": "FAIL",
  "message": "处理失败"
}
```

支付回调处理注意事项：

1. 回调接口不需要登录鉴权，但必须进行微信支付签名校验。
2. 回调接口必须允许公网访问。
3. 回调处理应尽量短，避免在回调线程中执行复杂耗时业务。
4. 支付成功后的权益发放、消息发送可以通过本地事务事件或 MQ 异步处理。
5. 回调日志应记录请求头、请求体、解密结果、处理状态和异常信息。
6. 重复回调属于正常现象，必须通过支付流水状态和唯一索引保证幂等。
7. 回调验签失败、解密失败、金额不一致必须记录安全日志，不应继续更新订单。

### 订单关闭流程

订单关闭流程用于处理用户长时间未支付、主动取消订单、支付页面超时或库存释放等场景。关闭订单需要同时处理本地订单状态和微信支付侧交易状态。

推荐订单关闭流程如下：

```text
触发订单关闭
        |
        v
查询本地订单和支付流水
        |
        v
判断订单是否已支付
        |
        v
未支付则调用微信支付关闭订单接口
        |
        v
根据关闭结果更新本地状态
        |
        v
释放库存、优惠券或其他占用资源
        |
        v
记录关闭日志
```

订单关闭通常有以下触发方式：

| 触发方式         | 说明                               |
| ---------------- | ---------------------------------- |
| 用户主动取消     | 用户在订单页点击取消支付或取消订单 |
| 定时任务关闭     | 后台定时扫描超时未支付订单         |
| 支付前置校验失败 | 下单后业务校验失败，需要关闭支付单 |
| 库存释放         | 订单超时后释放库存和优惠券         |
| 管理端操作       | 客服或运营手动关闭异常订单         |

订单关闭需要注意以下规则：

1. 已支付订单不能关闭。
2. 已关闭订单重复关闭时应直接返回成功或忽略。
3. 调用微信关闭订单前，建议先确认本地状态仍为 `WAIT_PAY` 或 `PAYING`。
4. 微信侧返回订单不存在时，需要结合本地状态判断是否标记为关闭或异常。
5. 关闭订单后如果又收到支付成功回调，应主动查询微信订单状态并进入异常处理。
6. 定时关闭任务应分批处理，避免一次扫描过多订单造成数据库和微信支付接口压力。
7. 关闭本地订单、释放库存、退还优惠券等操作需要保证幂等。

定时关闭策略示例：

| 策略项   | 建议值                     |
| -------- | -------------------------- |
| 扫描频率 | 每 1 分钟或每 5 分钟       |
| 单批数量 | 100 到 500 条              |
| 扫描状态 | `WAIT_PAY`、`PAYING`       |
| 扫描条件 | 当前时间大于订单过期时间   |
| 处理方式 | 分页扫描，逐条或批量关闭   |
| 异常处理 | 记录失败原因，后续继续补偿 |

订单关闭后的本地状态建议如下：

```text
WAIT_PAY -> CLOSED
PAYING   -> CLOSED
PAID     -> 不允许关闭
CLOSED   -> 幂等返回
REFUNDING / REFUNDED -> 不允许关闭
```

订单关闭不是简单地修改本地状态。只要已经向微信支付创建过预支付订单，就应尽量同步关闭微信支付侧订单，避免用户在本地订单已取消后仍然完成付款。

## 核心功能实现

本章节用于实现微信支付的核心下单能力，包括 JSAPI 支付、Native 支付、APP 支付、H5 支付和小程序支付。不同支付方式的下单参数略有差异，但后端处理流程基本一致：校验本地订单、创建支付流水、调用微信支付下单接口、保存预支付结果、返回前端调起支付参数。

微信支付官方 Java SDK 的 `service` 模块提供了 Native、JSAPI、APP 等支付服务；其中 JSAPI 和 APP 推荐使用扩展服务类生成前端调起支付参数，Native 支付下单后返回二维码链接 `code_url`。SDK 的 `core` 模块负责自动签名和验签，业务代码通常不需要手动计算请求签名。([GitHub](https://github.com/wechatpay-apiv3/wechatpay-java?utm_source=chatgpt.com))

推荐的核心文件结构如下：

```text
src/main/java/io/github/atengk/payment
├── config
│   ├── WechatPayProperties.java
│   └── WechatPayClientConfig.java
├── controller
│   └── WxPayController.java
├── service
│   └── WxPayService.java
├── service/impl
│   └── WxPayServiceImpl.java
├── dto
│   └── WxPayCreateRequest.java
└── vo
    └── WxPayCreateResponse.java
```

文件位置：`src/main/java/io/github/atengk/payment/dto/WxPayCreateRequest.java`

下面的请求对象用于接收业务侧创建支付订单时传入的核心参数，实际项目中金额应以后端订单金额为准，不能直接信任前端传入金额。

```java
package io.github.atengk.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 微信支付创建订单请求
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Data
public class WxPayCreateRequest {

    /**
     * 本地业务订单号
     */
    @NotBlank(message = "业务订单号不能为空")
    private String orderNo;

    /**
     * 商品描述
     */
    @NotBlank(message = "商品描述不能为空")
    private String description;

    /**
     * 支付金额，单位：元
     */
    @NotNull(message = "支付金额不能为空")
    private BigDecimal amount;

    /**
     * 用户 openid，JSAPI 和小程序支付必填
     */
    private String openid;

    /**
     * 客户端 IP，H5 支付建议传入
     */
    private String clientIp;
}
```

文件位置：`src/main/java/io/github/atengk/payment/vo/WxPayCreateResponse.java`

下面的响应对象用于统一封装不同支付方式的返回结果，前端根据 `payType` 读取对应参数。

```java
package io.github.atengk.payment.vo;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * 微信支付创建订单响应
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Data
@Builder
public class WxPayCreateResponse {

    /**
     * 本地业务订单号
     */
    private String orderNo;

    /**
     * 微信商户订单号
     */
    private String outTradeNo;

    /**
     * 支付方式：JSAPI、NATIVE、APP、H5、MINI_PROGRAM
     */
    private String payType;

    /**
     * Native 支付二维码链接
     */
    private String codeUrl;

    /**
     * H5 支付跳转链接
     */
    private String h5Url;

    /**
     * 前端调起支付参数
     */
    private Map<String, Object> payParams;
}
```

文件位置：`src/main/java/io/github/atengk/payment/service/WxPayService.java`

下面的接口定义微信支付核心能力，后续 Controller 只依赖该接口，不直接调用微信支付 SDK。

```java
package io.github.atengk.payment.service;

import io.github.atengk.payment.dto.WxPayCreateRequest;
import io.github.atengk.payment.vo.WxPayCreateResponse;

/**
 * 微信支付服务
 *
 * @author Ateng
 * @since 2026-05-07
 */
public interface WxPayService {

    /**
     * 创建 JSAPI 支付订单
     *
     * @param request 支付请求
     * @return 支付参数
     */
    WxPayCreateResponse createJsapiPay(WxPayCreateRequest request);

    /**
     * 创建 Native 支付订单
     *
     * @param request 支付请求
     * @return 支付参数
     */
    WxPayCreateResponse createNativePay(WxPayCreateRequest request);

    /**
     * 创建 APP 支付订单
     *
     * @param request 支付请求
     * @return 支付参数
     */
    WxPayCreateResponse createAppPay(WxPayCreateRequest request);

    /**
     * 创建 H5 支付订单
     *
     * @param request 支付请求
     * @return 支付参数
     */
    WxPayCreateResponse createH5Pay(WxPayCreateRequest request);

    /**
     * 创建小程序支付订单
     *
     * @param request 支付请求
     * @return 支付参数
     */
    WxPayCreateResponse createMiniProgramPay(WxPayCreateRequest request);
}
```

文件位置：`src/main/java/io/github/atengk/payment/service/impl/WxPayServiceImpl.java`

下面的实现类演示五种支付方式的核心下单写法。示例中的订单校验、支付流水落库、状态更新和库存处理以注释形式标明，完整数据表实现放到后续“订单与支付数据设计”章节展开。

```java
package io.github.atengk.payment.service.impl;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import com.wechat.pay.java.service.payments.app.AppServiceExtension;
import com.wechat.pay.java.service.payments.h5.H5Service;
import com.wechat.pay.java.service.payments.jsapi.JsapiServiceExtension;
import com.wechat.pay.java.service.payments.nativepay.NativePayService;
import io.github.atengk.payment.config.WechatPayProperties;
import io.github.atengk.payment.dto.WxPayCreateRequest;
import io.github.atengk.payment.service.WxPayService;
import io.github.atengk.payment.vo.WxPayCreateResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 微信支付服务实现
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WxPayServiceImpl implements WxPayService {

    private final WechatPayProperties properties;
    private final NativePayService nativePayService;
    private final JsapiServiceExtension jsapiServiceExtension;
    private final AppServiceExtension appServiceExtension;
    private final H5Service h5Service;

    /**
     * 创建 JSAPI 支付订单
     *
     * @param request 支付请求
     * @return 支付参数
     */
    @Override
    public WxPayCreateResponse createJsapiPay(WxPayCreateRequest request) {
        validateBasicPayRequest(request);
        Assert.isTrue(StrUtil.isNotBlank(request.getOpenid()), "JSAPI 支付 openid 不能为空");

        String outTradeNo = buildOutTradeNo(request.getOrderNo());
        Integer totalAmount = convertAmountToCent(request.getAmount());

        com.wechat.pay.java.service.payments.jsapi.model.PrepayRequest prepayRequest =
                new com.wechat.pay.java.service.payments.jsapi.model.PrepayRequest();

        com.wechat.pay.java.service.payments.jsapi.model.Amount amount =
                new com.wechat.pay.java.service.payments.jsapi.model.Amount();
        amount.setTotal(totalAmount);

        com.wechat.pay.java.service.payments.jsapi.model.Payer payer =
                new com.wechat.pay.java.service.payments.jsapi.model.Payer();
        payer.setOpenid(request.getOpenid());

        prepayRequest.setAppid(properties.getAppId());
        prepayRequest.setMchid(properties.getMchId());
        prepayRequest.setDescription(request.getDescription());
        prepayRequest.setOutTradeNo(outTradeNo);
        prepayRequest.setNotifyUrl(properties.getPayNotifyUrl());
        prepayRequest.setAmount(amount);
        prepayRequest.setPayer(payer);

        log.info("创建 JSAPI 支付订单，订单号：{}，商户订单号：{}，金额：{} 分", request.getOrderNo(), outTradeNo, totalAmount);

        com.wechat.pay.java.service.payments.jsapi.model.PrepayWithRequestPaymentResponse response =
                jsapiServiceExtension.prepayWithRequestPayment(prepayRequest);

        Map<String, Object> payParams = MapUtil.<String, Object>builder()
                .put("appId", response.getAppId())
                .put("timeStamp", response.getTimeStamp())
                .put("nonceStr", response.getNonceStr())
                .put("packageVal", response.getPackageVal())
                .put("signType", response.getSignType())
                .put("paySign", response.getPaySign())
                .build();

        return WxPayCreateResponse.builder()
                .orderNo(request.getOrderNo())
                .outTradeNo(outTradeNo)
                .payType("JSAPI")
                .payParams(payParams)
                .build();
    }

    /**
     * 创建 Native 支付订单
     *
     * @param request 支付请求
     * @return 支付参数
     */
    @Override
    public WxPayCreateResponse createNativePay(WxPayCreateRequest request) {
        validateBasicPayRequest(request);

        String outTradeNo = buildOutTradeNo(request.getOrderNo());
        Integer totalAmount = convertAmountToCent(request.getAmount());

        com.wechat.pay.java.service.payments.nativepay.model.PrepayRequest prepayRequest =
                new com.wechat.pay.java.service.payments.nativepay.model.PrepayRequest();

        com.wechat.pay.java.service.payments.nativepay.model.Amount amount =
                new com.wechat.pay.java.service.payments.nativepay.model.Amount();
        amount.setTotal(totalAmount);

        prepayRequest.setAppid(properties.getAppId());
        prepayRequest.setMchid(properties.getMchId());
        prepayRequest.setDescription(request.getDescription());
        prepayRequest.setOutTradeNo(outTradeNo);
        prepayRequest.setNotifyUrl(properties.getPayNotifyUrl());
        prepayRequest.setAmount(amount);

        log.info("创建 Native 支付订单，订单号：{}，商户订单号：{}，金额：{} 分", request.getOrderNo(), outTradeNo, totalAmount);

        com.wechat.pay.java.service.payments.nativepay.model.PrepayResponse response =
                nativePayService.prepay(prepayRequest);

        return WxPayCreateResponse.builder()
                .orderNo(request.getOrderNo())
                .outTradeNo(outTradeNo)
                .payType("NATIVE")
                .codeUrl(response.getCodeUrl())
                .build();
    }

    /**
     * 创建 APP 支付订单
     *
     * @param request 支付请求
     * @return 支付参数
     */
    @Override
    public WxPayCreateResponse createAppPay(WxPayCreateRequest request) {
        validateBasicPayRequest(request);

        String outTradeNo = buildOutTradeNo(request.getOrderNo());
        Integer totalAmount = convertAmountToCent(request.getAmount());

        com.wechat.pay.java.service.payments.app.model.PrepayRequest prepayRequest =
                new com.wechat.pay.java.service.payments.app.model.PrepayRequest();

        com.wechat.pay.java.service.payments.app.model.Amount amount =
                new com.wechat.pay.java.service.payments.app.model.Amount();
        amount.setTotal(totalAmount);

        prepayRequest.setAppid(properties.getAppId());
        prepayRequest.setMchid(properties.getMchId());
        prepayRequest.setDescription(request.getDescription());
        prepayRequest.setOutTradeNo(outTradeNo);
        prepayRequest.setNotifyUrl(properties.getPayNotifyUrl());
        prepayRequest.setAmount(amount);

        log.info("创建 APP 支付订单，订单号：{}，商户订单号：{}，金额：{} 分", request.getOrderNo(), outTradeNo, totalAmount);

        com.wechat.pay.java.service.payments.app.model.PrepayWithRequestPaymentResponse response =
                appServiceExtension.prepayWithRequestPayment(prepayRequest);

        Map<String, Object> payParams = MapUtil.<String, Object>builder()
                .put("appid", response.getAppid())
                .put("partnerid", response.getPartnerid())
                .put("prepayid", response.getPrepayid())
                .put("packageVal", response.getPackageVal())
                .put("noncestr", response.getNoncestr())
                .put("timestamp", response.getTimestamp())
                .put("sign", response.getSign())
                .build();

        return WxPayCreateResponse.builder()
                .orderNo(request.getOrderNo())
                .outTradeNo(outTradeNo)
                .payType("APP")
                .payParams(payParams)
                .build();
    }

    /**
     * 创建 H5 支付订单
     *
     * @param request 支付请求
     * @return 支付参数
     */
    @Override
    public WxPayCreateResponse createH5Pay(WxPayCreateRequest request) {
        validateBasicPayRequest(request);
        Assert.isTrue(StrUtil.isNotBlank(request.getClientIp()), "H5 支付客户端 IP 不能为空");

        String outTradeNo = buildOutTradeNo(request.getOrderNo());
        Integer totalAmount = convertAmountToCent(request.getAmount());

        com.wechat.pay.java.service.payments.h5.model.PrepayRequest prepayRequest =
                new com.wechat.pay.java.service.payments.h5.model.PrepayRequest();

        com.wechat.pay.java.service.payments.h5.model.Amount amount =
                new com.wechat.pay.java.service.payments.h5.model.Amount();
        amount.setTotal(totalAmount);

        com.wechat.pay.java.service.payments.h5.model.SceneInfo sceneInfo =
                new com.wechat.pay.java.service.payments.h5.model.SceneInfo();
        sceneInfo.setPayerClientIp(request.getClientIp());

        com.wechat.pay.java.service.payments.h5.model.H5Info h5Info =
                new com.wechat.pay.java.service.payments.h5.model.H5Info();
        h5Info.setType("Wap");
        sceneInfo.setH5Info(h5Info);

        prepayRequest.setAppid(properties.getAppId());
        prepayRequest.setMchid(properties.getMchId());
        prepayRequest.setDescription(request.getDescription());
        prepayRequest.setOutTradeNo(outTradeNo);
        prepayRequest.setNotifyUrl(properties.getPayNotifyUrl());
        prepayRequest.setAmount(amount);
        prepayRequest.setSceneInfo(sceneInfo);

        log.info("创建 H5 支付订单，订单号：{}，商户订单号：{}，金额：{} 分", request.getOrderNo(), outTradeNo, totalAmount);

        com.wechat.pay.java.service.payments.h5.model.PrepayResponse response =
                h5Service.prepay(prepayRequest);

        return WxPayCreateResponse.builder()
                .orderNo(request.getOrderNo())
                .outTradeNo(outTradeNo)
                .payType("H5")
                .h5Url(response.getH5Url())
                .build();
    }

    /**
     * 创建小程序支付订单
     *
     * @param request 支付请求
     * @return 支付参数
     */
    @Override
    public WxPayCreateResponse createMiniProgramPay(WxPayCreateRequest request) {
        // 小程序支付后端接口与 JSAPI 支付基本一致，核心差异在于 appId 和 openid 来源于小程序
        return createJsapiPay(request);
    }

    /**
     * 校验基础支付请求
     *
     * @param request 支付请求
     */
    private void validateBasicPayRequest(WxPayCreateRequest request) {
        Assert.notNull(request, "支付请求不能为空");
        Assert.isTrue(StrUtil.isNotBlank(request.getOrderNo()), "业务订单号不能为空");
        Assert.isTrue(StrUtil.isNotBlank(request.getDescription()), "商品描述不能为空");
        Assert.notNull(request.getAmount(), "支付金额不能为空");
        Assert.isTrue(NumberUtil.isGreater(request.getAmount(), BigDecimal.ZERO), "支付金额必须大于 0");
    }

    /**
     * 构建微信商户订单号
     *
     * @param orderNo 本地业务订单号
     * @return 微信商户订单号
     */
    private String buildOutTradeNo(String orderNo) {
        return StrUtil.format("WX{}{}", LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss")), orderNo);
    }

    /**
     * 将元转换为分
     *
     * @param amount 金额，单位：元
     * @return 金额，单位：分
     */
    private Integer convertAmountToCent(BigDecimal amount) {
        return NumberUtil.mul(amount, BigDecimal.valueOf(100)).intValue();
    }
}
```

上述代码中的 `buildOutTradeNo` 仅用于文档示例。生产环境建议使用独立的订单号生成器，例如雪花算法、数据库号段、Redis 自增序列或统一 ID 服务，避免并发场景下商户订单号冲突。

### JSAPI 支付

JSAPI 支付适用于微信公众号、微信内 H5 页面等场景。该模式下，用户需要在微信内打开页面，后端下单时需要传入当前 AppID 下的用户 `openid`。

JSAPI 支付的关键参数如下：

| 参数           | 说明                      |
| -------------- | ------------------------- |
| `appid`        | 公众号 AppID              |
| `mchid`        | 微信支付商户号            |
| `description`  | 商品描述                  |
| `out_trade_no` | 商户订单号                |
| `notify_url`   | 支付结果通知地址          |
| `amount.total` | 支付金额，单位：分        |
| `payer.openid` | 当前公众号下的用户 openid |

JSAPI 支付后端返回前端调起支付所需参数，前端通常使用 `WeixinJSBridge` 或微信 JS-SDK 发起支付。官方 SDK 提供 `JsapiServiceExtension.prepayWithRequestPayment()`，可在下单后直接生成调起支付参数。([GitHub](https://github.com/wechatpay-apiv3/wechatpay-java?utm_source=chatgpt.com))

推荐返回示例：

```json
{
  "orderNo": "ORDER202605070001",
  "outTradeNo": "WX202605071530000001",
  "payType": "JSAPI",
  "payParams": {
    "appId": "wx1234567890abcdef",
    "timeStamp": "1778123400",
    "nonceStr": "a8f2d9e7c1",
    "packageVal": "prepay_id=wx07153000123456789",
    "signType": "RSA",
    "paySign": "MEUCIQD..."
  }
}
```

JSAPI 支付注意事项：

1. `openid` 必须属于当前 `appid`。
2. 前端不能自行拼接支付签名，建议直接使用后端返回参数。
3. 支付成功页面不能作为后端发货依据。
4. 用户取消支付时，微信不一定立即回调后端，前端应引导用户刷新订单状态。
5. 订单超时后，应通过定时任务关闭未支付订单。

### Native 支付

Native 支付适用于 PC 网站、扫码收银台、后台管理系统生成二维码收款等场景。后端调用 Native 下单接口后，微信支付返回 `code_url`，业务系统将该链接生成二维码展示给用户扫码支付。

Native 支付的关键参数如下：

| 参数           | 说明                       |
| -------------- | -------------------------- |
| `appid`        | 公众号、小程序或应用 AppID |
| `mchid`        | 微信支付商户号             |
| `description`  | 商品描述                   |
| `out_trade_no` | 商户订单号                 |
| `notify_url`   | 支付结果通知地址           |
| `amount.total` | 支付金额，单位：分         |

Native 支付返回示例：

```json
{
  "orderNo": "ORDER202605070002",
  "outTradeNo": "WX202605071531000002",
  "payType": "NATIVE",
  "codeUrl": "weixin://wxpay/bizpayurl?pr=xxxxxx"
}
```

前端拿到 `codeUrl` 后，可以使用二维码组件生成扫码支付二维码。后端不需要轮询微信支付接口来判断用户是否付款，正常情况下应优先等待微信支付回调；前端订单页可以定时调用后端订单查询接口刷新支付状态。

Native 支付注意事项：

1. 二维码内容必须使用微信返回的 `code_url`。
2. 二维码过期后，应重新创建支付订单或提示用户刷新。
3. PC 页面轮询查询订单状态时，应查询本地订单状态，不建议每次都直接查询微信支付。
4. 支付成功后应由微信回调驱动订单状态更新。
5. 如果回调延迟，可以由后端补偿任务主动查询微信支付订单。

### APP 支付

APP 支付适用于 Android 和 iOS 原生应用。后端创建 APP 支付订单后，返回 App 端调起微信支付 SDK 所需参数，由 App 客户端调用微信 SDK 完成支付。

APP 支付的关键参数如下：

| 参数           | 说明                       |
| -------------- | -------------------------- |
| `appid`        | 微信开放平台移动应用 AppID |
| `mchid`        | 微信支付商户号             |
| `description`  | 商品描述                   |
| `out_trade_no` | 商户订单号                 |
| `notify_url`   | 支付结果通知地址           |
| `amount.total` | 支付金额，单位：分         |

APP 支付返回示例：

```json
{
  "orderNo": "ORDER202605070003",
  "outTradeNo": "WX202605071532000003",
  "payType": "APP",
  "payParams": {
    "appid": "wx1234567890abcdef",
    "partnerid": "1900000000",
    "prepayid": "wx07153200123456789",
    "packageVal": "Sign=WXPay",
    "noncestr": "d9f1a2b3c4",
    "timestamp": "1778123520",
    "sign": "MEUCIQD..."
  }
}
```

APP 支付注意事项：

1. App 使用的 AppID 应与微信开放平台移动应用一致。
2. 商户号需要与移动应用完成绑定。
3. Android 和 iOS 客户端只负责调起微信支付，不负责判定最终支付结果。
4. App 支付完成后，客户端应调用后端订单查询接口刷新状态。
5. 后端仍以微信支付回调或主动查询结果作为最终支付依据。

### H5 支付

H5 支付适用于手机浏览器中的支付场景，尤其是非微信内置浏览器访问的移动 Web 页面。后端调用 H5 下单接口后，微信支付返回 H5 支付跳转链接，前端跳转该链接完成支付。

H5 支付的关键参数如下：

| 参数                         | 说明                     |
| ---------------------------- | ------------------------ |
| `appid`                      | 应用 AppID               |
| `mchid`                      | 微信支付商户号           |
| `description`                | 商品描述                 |
| `out_trade_no`               | 商户订单号               |
| `notify_url`                 | 支付结果通知地址         |
| `amount.total`               | 支付金额，单位：分       |
| `scene_info.payer_client_ip` | 用户客户端 IP            |
| `scene_info.h5_info.type`    | 场景类型，常用值为 `Wap` |

H5 支付返回示例：

```json
{
  "orderNo": "ORDER202605070004",
  "outTradeNo": "WX202605071533000004",
  "payType": "H5",
  "h5Url": "https://wx.tenpay.com/cgi-bin/mmpayweb-bin/checkmweb?prepay_id=..."
}
```

H5 支付注意事项：

1. H5 支付需要正确传入用户客户端 IP。
2. 商户平台需要配置 H5 支付域名。
3. 浏览器跳转完成后，前端应回到业务系统支付结果页。
4. 支付结果页应调用后端查询订单状态，不应直接信任浏览器跳转结果。
5. 微信内置浏览器一般不使用 H5 支付，应优先使用 JSAPI 支付。

### 小程序支付

小程序支付适用于微信小程序内的商品购买、服务预约、会员充值等场景。小程序支付后端接口与 JSAPI 支付高度一致，本质上也是通过 `openid`、`appid`、`mchid`、`prepay_id` 和前端支付签名完成调起支付。

小程序支付的关键参数如下：

| 参数           | 说明                  |
| -------------- | --------------------- |
| `appid`        | 小程序 AppID          |
| `mchid`        | 微信支付商户号        |
| `description`  | 商品描述              |
| `out_trade_no` | 商户订单号            |
| `notify_url`   | 支付结果通知地址      |
| `amount.total` | 支付金额，单位：分    |
| `payer.openid` | 当前小程序用户 openid |

小程序支付返回示例：

```json
{
  "orderNo": "ORDER202605070005",
  "outTradeNo": "WX202605071534000005",
  "payType": "MINI_PROGRAM",
  "payParams": {
    "appId": "wx1234567890abcdef",
    "timeStamp": "1778123640",
    "nonceStr": "a1b2c3d4e5",
    "packageVal": "prepay_id=wx07153400123456789",
    "signType": "RSA",
    "paySign": "MEUCIQD..."
  }
}
```

小程序支付注意事项：

1. `openid` 必须是当前小程序 AppID 下的用户标识。
2. 小程序端调用支付 API 时，应使用后端返回的完整支付参数。
3. 小程序支付成功回调只用于页面交互，不作为后端订单完成依据。
4. 小程序订单详情页应主动查询后端订单状态。
5. 后端需要通过支付回调或主动查单确认最终支付结果。

## 支付回调处理

本章节用于实现微信支付回调处理，包括回调验签、回调解密、订单状态更新和幂等处理。支付回调是微信支付通知商户支付结果的核心入口，必须保证原始报文完整、签名校验可靠、业务处理幂等。

微信支付 Java SDK 提供 `NotificationParser` 处理回调通知。官方文档强调，构造 `RequestParam` 时必须使用 HTTP 原始请求体，不能使用反序列化后再序列化的 JSON 字符串，否则容易导致验签失败。`NotificationParser.parse()` 会完成验签、解密和通知对象转换；验签失败会抛出 `ValidationException`。([GitHub](https://github.com/wechatpay-apiv3/wechatpay-java?utm_source=chatgpt.com))

推荐回调处理文件结构如下：

```text
src/main/java/io/github/atengk/payment
├── controller
│   └── WxPayNotifyController.java
├── service
│   └── WxPayNotifyService.java
└── service/impl
    └── WxPayNotifyServiceImpl.java
```

文件位置：`src/main/java/io/github/atengk/payment/controller/WxPayNotifyController.java`

下面的 Controller 用于接收微信支付回调。回调接口不需要登录鉴权，但必须依赖微信支付签名校验保证请求可信。

```java
package io.github.atengk.payment.controller;

import cn.hutool.core.io.IoUtil;
import io.github.atengk.payment.service.WxPayNotifyService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 微信支付回调控制器
 *
 * @author Ateng
 * @since 2026-05-07
 */
@RestController
@RequiredArgsConstructor
public class WxPayNotifyController {

    private final WxPayNotifyService wxPayNotifyService;

    /**
     * 微信支付结果通知
     *
     * @param request HTTP 请求
     * @return 微信支付回调响应
     */
    @PostMapping("/api/pay/wx/notify")
    public ResponseEntity<Map<String, String>> payNotify(HttpServletRequest request) {
        String body = IoUtil.read(request.getInputStream(), StandardCharsets.UTF_8);
        wxPayNotifyService.handlePayNotify(request, body);
        return ResponseEntity.ok(Map.of("code", "SUCCESS", "message", "成功"));
    }
}
```

如果业务处理失败，可以返回非 2xx 状态码，让微信支付后续重试通知。官方文档也说明，处理成功应返回 `200 OK`，处理失败可返回 `4xx` 或 `5xx`。([GitHub](https://github.com/wechatpay-apiv3/wechatpay-java?utm_source=chatgpt.com))

### 回调验签

回调验签用于确认当前通知确实来自微信支付，并且通知内容没有被篡改。验签时必须使用微信支付请求头中的签名信息、平台证书序列号、随机串、时间戳和原始请求体。

微信支付回调常用请求头如下：

| 请求头                     | 说明                   |
| -------------------------- | ---------------------- |
| `Wechatpay-Signature`      | 微信支付签名           |
| `Wechatpay-Serial`         | 微信支付平台证书序列号 |
| `Wechatpay-Nonce`          | 签名随机串             |
| `Wechatpay-Timestamp`      | 签名时间戳             |
| `Wechatpay-Signature-Type` | 签名类型               |

文件位置：`src/main/java/io/github/atengk/payment/service/WxPayNotifyService.java`

```java
package io.github.atengk.payment.service;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 微信支付回调服务
 *
 * @author Ateng
 * @since 2026-05-07
 */
public interface WxPayNotifyService {

    /**
     * 处理支付结果通知
     *
     * @param request HTTP 请求
     * @param body    原始请求体
     */
    void handlePayNotify(HttpServletRequest request, String body);
}
```

文件位置：`src/main/java/io/github/atengk/payment/service/impl/WxPayNotifyServiceImpl.java`

下面的实现类使用 `RequestParam` 和 `NotificationParser` 处理微信支付回调通知。为了兼容不同 SDK 模型变化，示例使用 `Map.class` 接收解密后的通知数据。

```java
package io.github.atengk.payment.service.impl;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import com.wechat.pay.java.core.Config;
import com.wechat.pay.java.core.exception.ValidationException;
import com.wechat.pay.java.core.notification.NotificationParser;
import com.wechat.pay.java.core.notification.RequestParam;
import io.github.atengk.payment.config.WechatPayProperties;
import io.github.atengk.payment.service.WxPayNotifyService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 微信支付回调服务实现
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WxPayNotifyServiceImpl implements WxPayNotifyService {

    private final Config wechatPayConfig;
    private final WechatPayProperties properties;

    /**
     * 处理支付结果通知
     *
     * @param request HTTP 请求
     * @param body    原始请求体
     */
    @Override
    public void handlePayNotify(HttpServletRequest request, String body) {
        RequestParam requestParam = buildRequestParam(request, body);
        Map<String, Object> notifyData = parseNotifyData(requestParam);

        String outTradeNo = Convert.toStr(notifyData.get("out_trade_no"));
        String transactionId = Convert.toStr(notifyData.get("transaction_id"));
        String tradeState = Convert.toStr(notifyData.get("trade_state"));
        String mchId = Convert.toStr(notifyData.get("mchid"));

        Map<String, Object> amountMap = Convert.toMap(String.class, Object.class, notifyData.get("amount"));
        Integer totalAmount = Convert.toInt(amountMap.get("total"));

        log.info("收到微信支付回调，商户订单号：{}，微信交易号：{}，交易状态：{}", outTradeNo, transactionId, tradeState);

        validateNotifyBusinessData(outTradeNo, mchId, totalAmount);
        updateOrderStatusByNotify(outTradeNo, transactionId, tradeState, totalAmount);
    }

    /**
     * 构建微信支付回调请求参数
     *
     * @param request HTTP 请求
     * @param body    原始请求体
     * @return 回调请求参数
     */
    private RequestParam buildRequestParam(HttpServletRequest request, String body) {
        return new RequestParam.Builder()
                .serialNumber(request.getHeader("Wechatpay-Serial"))
                .nonce(request.getHeader("Wechatpay-Nonce"))
                .signature(request.getHeader("Wechatpay-Signature"))
                .timestamp(request.getHeader("Wechatpay-Timestamp"))
                .signType(request.getHeader("Wechatpay-Signature-Type"))
                .body(body)
                .build();
    }

    /**
     * 验签并解析回调数据
     *
     * @param requestParam 回调请求参数
     * @return 解密后的通知数据
     */
    private Map<String, Object> parseNotifyData(RequestParam requestParam) {
        try {
            NotificationParser parser = new NotificationParser(wechatPayConfig);
            Map<String, Object> notifyData = parser.parse(requestParam, Map.class);
            if (MapUtil.isEmpty(notifyData)) {
                throw new IllegalArgumentException("微信支付回调数据为空");
            }
            return notifyData;
        } catch (ValidationException e) {
            log.warn("微信支付回调验签失败", e);
            throw e;
        } catch (Exception e) {
            log.error("微信支付回调解析失败", e);
            throw e;
        }
    }

    /**
     * 校验回调业务数据
     *
     * @param outTradeNo  商户订单号
     * @param mchId       商户号
     * @param totalAmount 支付金额，单位：分
     */
    private void validateNotifyBusinessData(String outTradeNo, String mchId, Integer totalAmount) {
        if (StrUtil.isBlank(outTradeNo)) {
            throw new IllegalArgumentException("微信支付回调商户订单号为空");
        }
        if (!StrUtil.equals(properties.getMchId(), mchId)) {
            throw new IllegalArgumentException("微信支付回调商户号不一致");
        }
        if (totalAmount == null || totalAmount <= 0) {
            throw new IllegalArgumentException("微信支付回调金额异常");
        }

        // 实际项目中需要根据 outTradeNo 查询本地支付流水，并校验金额是否一致。
        // 示例：
        // PayRecord payRecord = payRecordMapper.selectByOutTradeNo(outTradeNo);
        // Assert.notNull(payRecord, "支付流水不存在");
        // Assert.isTrue(payRecord.getAmount().equals(totalAmount), "支付金额不一致");
    }

    /**
     * 根据回调更新订单状态
     *
     * @param outTradeNo    商户订单号
     * @param transactionId 微信交易号
     * @param tradeState    微信支付交易状态
     * @param totalAmount   支付金额，单位：分
     */
    private void updateOrderStatusByNotify(String outTradeNo, String transactionId, String tradeState, Integer totalAmount) {
        if (!StrUtil.equals("SUCCESS", tradeState)) {
            log.info("微信支付回调非成功状态，商户订单号：{}，交易状态：{}", outTradeNo, tradeState);
            return;
        }

        // 实际项目中应在事务中完成以下操作：
        // 1. 根据 outTradeNo 查询支付流水。
        // 2. 判断支付流水是否已经是 PAID。
        // 3. 校验本地订单金额和微信回调金额是否一致。
        // 4. 使用条件更新将支付流水从 PAYING 更新为 PAID。
        // 5. 使用条件更新将业务订单从 WAIT_PAY 或 PAYING 更新为 PAID。
        // 6. 记录 transactionId、支付完成时间和回调处理状态。
        // 7. 发布订单支付成功事件，用于发放权益、扣减库存或发送消息。

        log.info("微信支付成功，准备更新订单状态，商户订单号：{}，微信交易号：{}，金额：{} 分", outTradeNo, transactionId, totalAmount);
    }
}
```

回调验签注意事项：

1. 必须使用原始请求体 `body` 参与验签。
2. 不能先把请求体反序列化为对象，再重新序列化后参与验签。
3. 请求头名称需要与微信支付实际请求头保持一致。
4. 验签失败必须拒绝处理订单状态。
5. 验签失败应记录安全日志，但不要打印敏感密钥信息。

### 回调解密

回调解密用于读取微信支付通知中的真实业务数据，例如商户订单号、微信交易号、交易状态、支付金额和支付完成时间。使用 `NotificationParser.parse()` 时，SDK 会在验签成功后完成资源数据解密，并转换为指定对象或 `Map`。官方文档说明，该解析器会完成验签、解密和通知对象转换。([GitHub](https://github.com/wechatpay-apiv3/wechatpay-java?utm_source=chatgpt.com))

解密后的支付通知中，常用字段如下：

| 字段                 | 说明                       |
| -------------------- | -------------------------- |
| `mchid`              | 商户号                     |
| `appid`              | 应用 AppID                 |
| `out_trade_no`       | 商户订单号                 |
| `transaction_id`     | 微信支付交易号             |
| `trade_type`         | 支付类型                   |
| `trade_state`        | 交易状态                   |
| `trade_state_desc`   | 交易状态描述               |
| `bank_type`          | 付款银行                   |
| `success_time`       | 支付完成时间               |
| `payer.openid`       | 支付用户 openid            |
| `amount.total`       | 订单总金额，单位：分       |
| `amount.payer_total` | 用户实际支付金额，单位：分 |

推荐在解密后立即进行业务校验：

```java
String outTradeNo = Convert.toStr(notifyData.get("out_trade_no"));
String transactionId = Convert.toStr(notifyData.get("transaction_id"));
String tradeState = Convert.toStr(notifyData.get("trade_state"));
String mchId = Convert.toStr(notifyData.get("mchid"));

Map<String, Object> amountMap = Convert.toMap(String.class, Object.class, notifyData.get("amount"));
Integer totalAmount = Convert.toInt(amountMap.get("total"));
```

回调解密注意事项：

1. API v3 密钥错误会导致解密失败。
2. 解密成功不代表业务可以直接更新状态，还需要校验商户号、订单号和金额。
3. 支付成功必须记录微信交易号 `transaction_id`。
4. 解密后的原始字段建议保存到回调日志，便于问题追踪。
5. 回调日志中应避免输出 API v3 密钥、私钥等敏感信息。

### 订单状态更新

订单状态更新用于将微信支付结果同步到本地业务订单和支付流水。支付成功回调通常只处理 `SUCCESS` 状态，其他状态可以记录日志，并由主动查询或关闭订单流程补偿处理。

推荐状态更新规则如下：

| 当前本地状态 | 微信状态  | 目标状态    | 处理方式                     |
| ------------ | --------- | ----------- | ---------------------------- |
| `WAIT_PAY`   | `SUCCESS` | `PAID`      | 更新支付流水和业务订单       |
| `PAYING`     | `SUCCESS` | `PAID`      | 更新支付流水和业务订单       |
| `PAID`       | `SUCCESS` | `PAID`      | 幂等返回                     |
| `CLOSED`     | `SUCCESS` | `异常状态`  | 主动查单并进入人工或补偿流程 |
| `REFUNDING`  | `SUCCESS` | `REFUNDING` | 不覆盖退款中状态             |
| `REFUNDED`   | `SUCCESS` | `REFUNDED`  | 不覆盖已退款状态             |

订单状态更新建议放在数据库事务中处理：

```text
开启事务
  |
  |-- 查询支付流水并加锁
  |-- 判断支付流水是否已处理
  |-- 校验支付金额
  |-- 更新支付流水状态为 PAID
  |-- 更新业务订单状态为 PAID
  |-- 写入回调处理结果
  |-- 发布支付成功事件
提交事务
```

推荐 SQL 条件更新思路如下：

```sql
-- 更新支付流水，只有支付中状态才允许变更为已支付
UPDATE pay_record
SET pay_status = 'PAID',
    transaction_id = #{transactionId},
    pay_success_time = NOW(),
    update_time = NOW()
WHERE out_trade_no = #{outTradeNo}
  AND pay_status IN ('WAIT_PAY', 'PAYING');

-- 更新业务订单，只有待支付或支付中订单才允许变更为已支付
UPDATE trade_order
SET order_status = 'PAID',
    pay_time = NOW(),
    update_time = NOW()
WHERE order_no = #{orderNo}
  AND order_status IN ('WAIT_PAY', 'PAYING');
```

订单状态更新注意事项：

1. 支付流水和业务订单必须同时更新。
2. 支付成功后不能被后续非成功状态覆盖。
3. 金额不一致时不能更新订单为已支付。
4. 微信交易号必须保存，后续退款、对账和问题排查都需要使用。
5. 支付成功后的权益发放建议通过事件或消息队列异步处理。
6. 如果订单状态已经是 `PAID`，重复回调应直接返回成功。

### 幂等处理

幂等处理用于保证微信支付重复回调、多次主动查询、接口重试或网络超时重放时，不会导致订单重复支付处理、重复发货、重复开通会员或重复发放权益。

微信支付回调重复通知属于正常情况，后端必须按商户订单号、支付流水号或微信交易号进行幂等控制。幂等设计不应只依赖内存锁，因为支付服务可能多实例部署。

推荐幂等控制方式如下：

| 方式         | 说明                                          |
| ------------ | --------------------------------------------- |
| 唯一索引     | `out_trade_no`、`transaction_id` 建立唯一约束 |
| 状态机控制   | 只允许指定状态向下一个状态变更                |
| 条件更新     | SQL 中增加当前状态条件                        |
| 分布式锁     | 对同一商户订单号加短时间锁                    |
| 回调日志     | 按通知 ID 或商户订单号记录处理状态            |
| 业务事件幂等 | 权益发放、库存扣减、消息通知独立做幂等        |

推荐数据库约束如下：

```sql
-- 支付流水商户订单号唯一
ALTER TABLE pay_record
ADD UNIQUE KEY uk_pay_record_out_trade_no (out_trade_no);

-- 微信交易号唯一，允许未支付时为空
ALTER TABLE pay_record
ADD UNIQUE KEY uk_pay_record_transaction_id (transaction_id);

-- 回调日志按通知 ID 去重，如果保存了微信回调 id，可建立唯一索引
ALTER TABLE pay_notify_log
ADD UNIQUE KEY uk_pay_notify_log_notify_id (notify_id);
```

如果项目使用 Redis 或 Redisson，可以在处理回调时对 `out_trade_no` 加短时间分布式锁：

```text
lock:wxpay:notify:{out_trade_no}
```

幂等处理推荐流程如下：

```text
收到支付回调
   |
   v
验签和解密
   |
   v
根据 out_trade_no 加分布式锁
   |
   v
查询支付流水
   |
   v
已支付则直接返回成功
   |
   v
未支付则执行条件更新
   |
   v
更新成功后发布支付成功事件
   |
   v
释放分布式锁
```

幂等处理注意事项：

1. 分布式锁只能作为并发保护，不能替代数据库状态条件。
2. 订单状态更新必须使用条件更新。
3. 支付成功事件消费者也必须幂等。
4. 回调日志应记录每次通知，但业务状态只能成功变更一次。
5. 如果支付流水已是 `PAID`，应直接返回成功，避免微信持续重试。
6. 如果处理过程中数据库异常，应返回失败状态码，让微信支付后续重试。



## 订单与支付数据设计

本章节用于设计微信支付接入过程中需要落库的核心数据，包括业务订单、支付流水和回调日志。业务订单用于承载业务状态，支付流水用于承载支付渠道状态，回调日志用于记录微信支付通知内容和处理结果。三类数据需要职责分离，避免把微信支付字段全部堆到业务订单表中。

支付模块建议遵循以下数据设计原则：

1. 业务订单表记录用户购买了什么、订单金额是多少、订单当前处于什么业务状态。
2. 支付流水表记录通过哪个支付渠道、哪个商户订单号、哪个微信交易号完成支付。
3. 回调日志表记录微信支付每次通知的原始数据、解密数据、处理状态和异常原因。
4. 订单状态更新必须以支付流水为核心依据，不能只看前端返回结果。
5. 支付成功、退款成功、回调处理成功等关键事件必须可追踪。

### 订单表设计

订单表用于保存业务订单信息，例如用户、商品、订单金额、支付状态、订单状态、支付时间和关闭时间。订单表不应承担过多支付渠道细节，微信交易号、商户订单号、回调内容等应放到支付流水和回调日志中。

推荐表名：`trade_order`

```sql
CREATE TABLE trade_order (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    order_no VARCHAR(64) NOT NULL COMMENT '业务订单号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    order_title VARCHAR(128) NOT NULL COMMENT '订单标题',
    order_amount INT NOT NULL COMMENT '订单金额，单位：分',
    pay_amount INT NOT NULL COMMENT '应支付金额，单位：分',
    paid_amount INT DEFAULT 0 COMMENT '已支付金额，单位：分',
    order_status VARCHAR(32) NOT NULL DEFAULT 'WAIT_PAY' COMMENT '订单状态：WAIT_PAY待支付，PAID已支付，CLOSED已关闭，REFUNDING退款中，REFUNDED已退款',
    pay_status VARCHAR(32) NOT NULL DEFAULT 'WAIT_PAY' COMMENT '支付状态：WAIT_PAY待支付，PAYING支付中，PAID已支付，PAY_FAILED支付失败',
    pay_type VARCHAR(32) DEFAULT NULL COMMENT '支付方式：JSAPI，NATIVE，APP，H5，MINI_PROGRAM',
    pay_time DATETIME DEFAULT NULL COMMENT '支付完成时间',
    close_time DATETIME DEFAULT NULL COMMENT '订单关闭时间',
    expire_time DATETIME NOT NULL COMMENT '订单过期时间',
    remark VARCHAR(255) DEFAULT NULL COMMENT '订单备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_trade_order_order_no (order_no),
    KEY idx_trade_order_user_id (user_id),
    KEY idx_trade_order_status_expire_time (order_status, expire_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='业务订单表';
```

字段说明如下：

| 字段           | 说明                         |
| -------------- | ---------------------------- |
| `order_no`     | 本地业务订单号，必须全局唯一 |
| `order_amount` | 原始订单金额，单位为分       |
| `pay_amount`   | 实际应支付金额，单位为分     |
| `paid_amount`  | 已支付金额，支付成功后更新   |
| `order_status` | 业务订单状态                 |
| `pay_status`   | 支付状态                     |
| `pay_type`     | 用户选择的支付方式           |
| `expire_time`  | 订单过期时间，用于超时关闭   |
| `pay_time`     | 支付成功时间                 |
| `close_time`   | 订单关闭时间                 |

订单状态建议控制如下：

| 状态               | 说明       | 是否终态 |
| ------------------ | ---------- | -------- |
| `WAIT_PAY`         | 待支付     | 否       |
| `PAYING`           | 支付中     | 否       |
| `PAID`             | 已支付     | 是       |
| `CLOSED`           | 已关闭     | 是       |
| `REFUNDING`        | 退款中     | 否       |
| `PARTIAL_REFUNDED` | 部分退款   | 否       |
| `REFUNDED`         | 已全额退款 | 是       |

订单表更新时必须使用状态条件，避免重复回调或并发请求覆盖终态数据。

```sql
UPDATE trade_order
SET order_status = 'PAID',
    pay_status = 'PAID',
    paid_amount = #{paidAmount},
    pay_time = NOW(),
    update_time = NOW()
WHERE order_no = #{orderNo}
  AND order_status IN ('WAIT_PAY', 'PAYING')
  AND pay_status IN ('WAIT_PAY', 'PAYING');
```

该 SQL 只允许待支付或支付中的订单变更为已支付。如果订单已经是 `PAID`，重复执行不会影响数据，可作为幂等控制的一部分。

### 支付流水表设计

支付流水表用于保存每一次支付请求和微信支付交易结果。业务订单和支付流水是一对多关系，同一个业务订单可能因为超时、取消、重新支付等原因产生多条支付流水，但同一时间只应存在一条有效待支付流水。

推荐表名：`pay_record`

```sql
CREATE TABLE pay_record (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    pay_no VARCHAR(64) NOT NULL COMMENT '本地支付流水号',
    order_no VARCHAR(64) NOT NULL COMMENT '业务订单号',
    out_trade_no VARCHAR(64) NOT NULL COMMENT '微信商户订单号',
    transaction_id VARCHAR(64) DEFAULT NULL COMMENT '微信支付交易号',
    mch_id VARCHAR(32) NOT NULL COMMENT '微信支付商户号',
    app_id VARCHAR(64) NOT NULL COMMENT '微信应用AppID',
    pay_type VARCHAR(32) NOT NULL COMMENT '支付方式：JSAPI，NATIVE，APP，H5，MINI_PROGRAM',
    pay_status VARCHAR(32) NOT NULL DEFAULT 'PAYING' COMMENT '支付状态：PAYING支付中，PAID已支付，CLOSED已关闭，PAY_FAILED支付失败',
    total_amount INT NOT NULL COMMENT '支付金额，单位：分',
    payer_openid VARCHAR(128) DEFAULT NULL COMMENT '支付用户openid',
    code_url VARCHAR(512) DEFAULT NULL COMMENT 'Native支付二维码链接',
    h5_url VARCHAR(1024) DEFAULT NULL COMMENT 'H5支付跳转链接',
    prepay_id VARCHAR(128) DEFAULT NULL COMMENT '微信预支付交易会话标识',
    trade_type VARCHAR(32) DEFAULT NULL COMMENT '微信支付交易类型',
    trade_state VARCHAR(32) DEFAULT NULL COMMENT '微信支付交易状态',
    trade_state_desc VARCHAR(255) DEFAULT NULL COMMENT '微信支付交易状态描述',
    success_time DATETIME DEFAULT NULL COMMENT '微信支付成功时间',
    close_time DATETIME DEFAULT NULL COMMENT '支付关闭时间',
    fail_reason VARCHAR(255) DEFAULT NULL COMMENT '支付失败原因',
    request_content TEXT DEFAULT NULL COMMENT '下单请求参数',
    response_content TEXT DEFAULT NULL COMMENT '下单响应参数',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_pay_record_pay_no (pay_no),
    UNIQUE KEY uk_pay_record_out_trade_no (out_trade_no),
    UNIQUE KEY uk_pay_record_transaction_id (transaction_id),
    KEY idx_pay_record_order_no (order_no),
    KEY idx_pay_record_status_create_time (pay_status, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付流水表';
```

字段说明如下：

| 字段               | 说明                                 |
| ------------------ | ------------------------------------ |
| `pay_no`           | 本地支付流水号，用于系统内部追踪     |
| `order_no`         | 业务订单号                           |
| `out_trade_no`     | 微信支付商户订单号，提交给微信支付   |
| `transaction_id`   | 微信支付交易号，支付成功后由微信返回 |
| `pay_type`         | 支付方式                             |
| `pay_status`       | 本地支付流水状态                     |
| `total_amount`     | 支付金额，单位为分                   |
| `prepay_id`        | 预支付交易会话标识                   |
| `code_url`         | Native 支付二维码链接                |
| `h5_url`           | H5 支付跳转链接                      |
| `request_content`  | 调用微信支付下单接口的请求参数       |
| `response_content` | 微信支付下单接口返回参数             |

支付流水状态建议如下：

| 状态               | 说明                         |
| ------------------ | ---------------------------- |
| `PAYING`           | 已创建支付流水，等待用户付款 |
| `PAID`             | 支付成功                     |
| `CLOSED`           | 支付单已关闭                 |
| `PAY_FAILED`       | 支付失败                     |
| `REFUNDING`        | 退款中                       |
| `PARTIAL_REFUNDED` | 部分退款                     |
| `REFUNDED`         | 已全额退款                   |

支付流水更新示例：

```sql
UPDATE pay_record
SET pay_status = 'PAID',
    transaction_id = #{transactionId},
    trade_type = #{tradeType},
    trade_state = #{tradeState},
    trade_state_desc = #{tradeStateDesc},
    success_time = #{successTime},
    update_time = NOW()
WHERE out_trade_no = #{outTradeNo}
  AND pay_status = 'PAYING';
```

该 SQL 只允许 `PAYING` 状态更新为 `PAID`。如果微信支付重复通知，第二次执行时不会再次更新成功，从而避免重复触发业务处理。

### 回调日志表设计

回调日志表用于保存微信支付通知的原始内容、请求头、解密结果、处理状态和异常原因。支付回调日志是排查支付异常、验签失败、金额不一致、重复通知和订单状态不一致的重要依据。

推荐表名：`pay_notify_log`

```sql
CREATE TABLE pay_notify_log (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    notify_id VARCHAR(128) DEFAULT NULL COMMENT '微信支付通知ID',
    notify_type VARCHAR(32) NOT NULL COMMENT '通知类型：PAY支付通知，REFUND退款通知',
    event_type VARCHAR(64) DEFAULT NULL COMMENT '微信支付事件类型',
    out_trade_no VARCHAR(64) DEFAULT NULL COMMENT '微信商户订单号',
    transaction_id VARCHAR(64) DEFAULT NULL COMMENT '微信支付交易号',
    out_refund_no VARCHAR(64) DEFAULT NULL COMMENT '商户退款单号',
    refund_id VARCHAR(64) DEFAULT NULL COMMENT '微信退款单号',
    headers TEXT DEFAULT NULL COMMENT '回调请求头',
    raw_body TEXT NOT NULL COMMENT '回调原始请求体',
    decrypt_body TEXT DEFAULT NULL COMMENT '解密后的回调内容',
    handle_status VARCHAR(32) NOT NULL DEFAULT 'INIT' COMMENT '处理状态：INIT初始，SUCCESS成功，FAILED失败',
    fail_reason VARCHAR(1024) DEFAULT NULL COMMENT '失败原因',
    notify_time DATETIME DEFAULT NULL COMMENT '微信通知时间',
    handle_time DATETIME DEFAULT NULL COMMENT '处理时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_pay_notify_log_notify_id (notify_id),
    KEY idx_pay_notify_log_out_trade_no (out_trade_no),
    KEY idx_pay_notify_log_transaction_id (transaction_id),
    KEY idx_pay_notify_log_out_refund_no (out_refund_no),
    KEY idx_pay_notify_log_status_create_time (handle_status, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付回调日志表';
```

字段说明如下：

| 字段            | 说明                                                    |
| --------------- | ------------------------------------------------------- |
| `notify_id`     | 微信支付通知 ID，如果回调报文中存在该字段，建议唯一保存 |
| `notify_type`   | 通知类型，区分支付回调和退款回调                        |
| `event_type`    | 微信支付事件类型                                        |
| `raw_body`      | 原始请求体，必须完整保存                                |
| `decrypt_body`  | 验签解密后的业务数据                                    |
| `handle_status` | 当前回调处理状态                                        |
| `fail_reason`   | 处理失败原因                                            |
| `handle_time`   | 回调处理完成时间                                        |

回调日志写入建议：

1. 收到回调后先保存原始请求体和请求头。
2. 验签失败也要记录日志，但不能更新订单状态。
3. 解密成功后更新 `decrypt_body`。
4. 业务处理成功后更新 `handle_status = SUCCESS`。
5. 业务处理失败后更新 `handle_status = FAILED` 并记录失败原因。
6. 重复回调可以记录多次，但业务状态只能变更一次。

## 接口设计

本章节用于定义微信支付模块对前端、业务系统和微信支付平台暴露的接口。对前端暴露的接口主要包括创建支付订单、查询支付订单和关闭支付订单；对微信支付平台暴露的接口主要是支付回调接口。

接口设计建议遵循以下原则：

1. 前端只传业务订单号、支付方式和必要终端参数。
2. 支付金额必须以后端订单金额为准。
3. 创建支付订单接口返回不同支付方式所需的调起参数。
4. 查询接口返回本地订单状态，不建议前端直接轮询微信支付。
5. 关闭接口需要同时关闭本地订单和微信支付订单。
6. 回调接口不做登录鉴权，但必须做微信支付验签。

### 创建支付订单接口

创建支付订单接口用于根据业务订单创建微信支付预支付订单，并返回前端调起支付所需参数。

接口定义如下：

| 项目         | 说明                 |
| ------------ | -------------------- |
| 请求方式     | `POST`               |
| 接口路径     | `/api/pay/wx/orders` |
| 是否登录     | 是                   |
| Content-Type | `application/json`   |

请求参数如下：

```json
{
  "orderNo": "ORDER202605070001",
  "payType": "NATIVE",
  "openid": "oUpF8uMuAJO_M2pxb1Q9zNjWeS6o",
  "clientIp": "127.0.0.1"
}
```

参数说明如下：

| 参数       | 是否必填           | 说明                                           |
| ---------- | ------------------ | ---------------------------------------------- |
| `orderNo`  | 是                 | 本地业务订单号                                 |
| `payType`  | 是                 | 支付方式：JSAPI、NATIVE、APP、H5、MINI_PROGRAM |
| `openid`   | JSAPI 和小程序必填 | 微信用户 openid                                |
| `clientIp` | H5 建议必填        | 用户客户端 IP                                  |

响应示例如下：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "orderNo": "ORDER202605070001",
    "payNo": "PAY202605070001",
    "outTradeNo": "WX202605071530000001",
    "payType": "NATIVE",
    "payStatus": "PAYING",
    "codeUrl": "weixin://wxpay/bizpayurl?pr=xxxxxx",
    "expireTime": "2026-05-07 15:30:00"
  }
}
```

调用示例：

```bash
curl -X POST "https://api.example.com/api/pay/wx/orders" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${TOKEN}" \
  -d '{
    "orderNo": "ORDER202605070001",
    "payType": "NATIVE"
  }'
```

创建支付订单接口处理流程如下：

```text
接收创建支付请求
  |
  |-- 校验用户身份
  |-- 查询业务订单
  |-- 校验订单归属、金额、状态
  |-- 判断是否已有有效支付流水
  |-- 创建新的支付流水
  |-- 调用微信支付下单接口
  |-- 保存微信预支付结果
  |-- 返回前端调起支付参数
```

接口注意事项：

1. 前端不应传入最终支付金额，金额以后端订单为准。
2. 同一个订单重复创建支付时，应先判断是否已有有效支付流水。
3. 如果已有未过期的 Native 支付二维码，可以直接返回原二维码链接。
4. 如果支付流水已关闭，可以重新生成新的商户订单号。
5. 创建支付订单失败时，应记录失败原因，方便后续排查。

### 查询支付订单接口

查询支付订单接口用于查询本地订单和支付流水状态。前端支付完成后、支付结果页刷新时、Native 支付二维码轮询时，都可以调用该接口获取最终状态。

接口定义如下：

| 项目     | 说明                           |
| -------- | ------------------------------ |
| 请求方式 | `GET`                          |
| 接口路径 | `/api/pay/wx/orders/{orderNo}` |
| 是否登录 | 是                             |

响应示例如下：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "orderNo": "ORDER202605070001",
    "payNo": "PAY202605070001",
    "outTradeNo": "WX202605071530000001",
    "transactionId": "4200000000202605071234567890",
    "payType": "NATIVE",
    "orderStatus": "PAID",
    "payStatus": "PAID",
    "payAmount": 100,
    "payTime": "2026-05-07 15:20:10"
  }
}
```

调用示例：

```bash
curl -X GET "https://api.example.com/api/pay/wx/orders/ORDER202605070001" \
  -H "Authorization: Bearer ${TOKEN}"
```

查询接口处理流程如下：

```text
接收查询请求
  |
  |-- 校验用户身份
  |-- 查询业务订单
  |-- 校验订单归属
  |-- 查询最新支付流水
  |-- 返回订单状态和支付状态
```

查询接口注意事项：

1. 前端查询接口优先返回本地状态。
2. 不建议前端每次查询都触发微信支付查单接口。
3. 如果本地状态长时间为 `PAYING`，可以由后端补偿任务主动查单。
4. 查询结果应同时返回业务订单状态和支付流水状态。
5. 已支付订单应返回支付时间和微信交易号，便于问题追踪。

### 关闭支付订单接口

关闭支付订单接口用于用户主动取消支付、订单超时关闭或管理端关闭异常订单。关闭时应先判断本地订单状态，再调用微信支付关闭订单接口。

接口定义如下：

| 项目     | 说明                                 |
| -------- | ------------------------------------ |
| 请求方式 | `POST`                               |
| 接口路径 | `/api/pay/wx/orders/{orderNo}/close` |
| 是否登录 | 是                                   |

请求示例：

```json
{
  "reason": "用户主动取消订单"
}
```

响应示例如下：

```json
{
  "code": 200,
  "message": "订单已关闭",
  "data": {
    "orderNo": "ORDER202605070001",
    "payStatus": "CLOSED",
    "closeTime": "2026-05-07 15:30:00"
  }
}
```

调用示例：

```bash
curl -X POST "https://api.example.com/api/pay/wx/orders/ORDER202605070001/close" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${TOKEN}" \
  -d '{
    "reason": "用户主动取消订单"
  }'
```

关闭接口处理流程如下：

```text
接收关闭请求
  |
  |-- 校验用户身份
  |-- 查询业务订单和支付流水
  |-- 判断订单是否已支付
  |-- 未支付则调用微信支付关闭订单接口
  |-- 更新支付流水为 CLOSED
  |-- 更新业务订单为 CLOSED
  |-- 释放库存、优惠券等占用资源
```

关闭接口注意事项：

1. 已支付订单不能关闭。
2. 已关闭订单重复关闭时应幂等返回成功。
3. 关闭微信支付订单失败时，不应直接修改本地订单为关闭状态。
4. 如果微信支付返回订单不存在，需要结合本地流水状态判断是否进入异常补偿。
5. 关闭后如果收到支付成功回调，需要主动查单并进入异常处理流程。

### 支付回调接口

支付回调接口由微信支付平台调用，用于通知商户支付结果。该接口不面向前端，不需要登录态，但必须进行微信支付签名校验和回调解密。

接口定义如下：

| 项目         | 说明                 |
| ------------ | -------------------- |
| 请求方式     | `POST`               |
| 接口路径     | `/api/pay/wx/notify` |
| 是否登录     | 否                   |
| Content-Type | `application/json`   |
| 调用方       | 微信支付平台         |

微信支付回调请求中通常包含以下请求头：

| 请求头                     | 说明                   |
| -------------------------- | ---------------------- |
| `Wechatpay-Signature`      | 微信支付签名           |
| `Wechatpay-Serial`         | 微信支付平台证书序列号 |
| `Wechatpay-Nonce`          | 签名随机串             |
| `Wechatpay-Timestamp`      | 签名时间戳             |
| `Wechatpay-Signature-Type` | 签名类型               |

成功响应：

```json
{
  "code": "SUCCESS",
  "message": "成功"
}
```

失败响应：

```json
{
  "code": "FAIL",
  "message": "处理失败"
}
```

回调接口处理流程如下：

```text
接收微信支付回调
  |
  |-- 读取请求头和原始请求体
  |-- 保存回调日志
  |-- 验证微信支付签名
  |-- 解密 resource 数据
  |-- 校验商户号、订单号、金额
  |-- 查询支付流水
  |-- 幂等更新支付流水和业务订单
  |-- 更新回调日志处理结果
  |-- 返回 SUCCESS
```

回调接口注意事项：

1. 必须使用原始请求体进行验签。
2. 验签失败不能更新订单状态。
3. 金额不一致不能更新订单为已支付。
4. 重复回调必须幂等返回。
5. 业务处理失败时不要返回成功，否则微信支付可能不再重试。
6. 支付成功后的业务动作，例如发货、开通会员、发放权益，建议通过事件或消息队列异步处理。

## 退款功能

本章节用于说明微信支付退款能力，包括申请退款、查询退款和退款回调处理。退款功能应基于已支付订单和支付流水执行，不能只根据前端请求直接发起退款。退款金额、退款原因、退款权限和退款次数都应由后端进行严格校验。

退款功能建议单独维护退款流水表。如果当前项目暂时没有独立退款表，也至少需要在支付流水和订单表中记录退款状态、退款金额和退款结果。

推荐退款流水表：`pay_refund_record`

```sql
CREATE TABLE pay_refund_record (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    refund_no VARCHAR(64) NOT NULL COMMENT '本地退款流水号',
    order_no VARCHAR(64) NOT NULL COMMENT '业务订单号',
    pay_no VARCHAR(64) NOT NULL COMMENT '本地支付流水号',
    out_trade_no VARCHAR(64) NOT NULL COMMENT '微信商户订单号',
    transaction_id VARCHAR(64) DEFAULT NULL COMMENT '微信支付交易号',
    out_refund_no VARCHAR(64) NOT NULL COMMENT '商户退款单号',
    refund_id VARCHAR(64) DEFAULT NULL COMMENT '微信退款单号',
    refund_status VARCHAR(32) NOT NULL DEFAULT 'PROCESSING' COMMENT '退款状态：PROCESSING退款中，SUCCESS退款成功，CLOSED退款关闭，ABNORMAL退款异常',
    refund_amount INT NOT NULL COMMENT '退款金额，单位：分',
    total_amount INT NOT NULL COMMENT '原订单金额，单位：分',
    refund_reason VARCHAR(255) DEFAULT NULL COMMENT '退款原因',
    notify_url VARCHAR(512) DEFAULT NULL COMMENT '退款结果通知地址',
    success_time DATETIME DEFAULT NULL COMMENT '退款成功时间',
    request_content TEXT DEFAULT NULL COMMENT '退款请求参数',
    response_content TEXT DEFAULT NULL COMMENT '退款响应参数',
    fail_reason VARCHAR(1024) DEFAULT NULL COMMENT '退款失败原因',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_pay_refund_record_refund_no (refund_no),
    UNIQUE KEY uk_pay_refund_record_out_refund_no (out_refund_no),
    KEY idx_pay_refund_record_order_no (order_no),
    KEY idx_pay_refund_record_out_trade_no (out_trade_no),
    KEY idx_pay_refund_record_status_create_time (refund_status, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付退款流水表';
```

退款状态建议如下：

| 状态         | 说明       |
| ------------ | ---------- |
| `PROCESSING` | 退款处理中 |
| `SUCCESS`    | 退款成功   |
| `CLOSED`     | 退款关闭   |
| `ABNORMAL`   | 退款异常   |

### 申请退款

申请退款用于对已支付订单发起退款请求。退款前必须校验订单状态、支付流水状态、可退金额、退款次数和业务权限。

申请退款接口定义如下：

| 项目         | 说明                         |
| ------------ | ---------------------------- |
| 请求方式     | `POST`                       |
| 接口路径     | `/api/pay/wx/refunds`        |
| 是否登录     | 是，通常需要管理端或业务权限 |
| Content-Type | `application/json`           |

请求示例：

```json
{
  "orderNo": "ORDER202605070001",
  "refundAmount": 100,
  "reason": "用户申请退款"
}
```

参数说明如下：

| 参数           | 是否必填 | 说明               |
| -------------- | -------- | ------------------ |
| `orderNo`      | 是       | 本地业务订单号     |
| `refundAmount` | 是       | 退款金额，单位：分 |
| `reason`       | 否       | 退款原因           |

响应示例：

```json
{
  "code": 200,
  "message": "退款申请已提交",
  "data": {
    "orderNo": "ORDER202605070001",
    "refundNo": "RF202605070001",
    "outRefundNo": "WXR202605070001",
    "refundStatus": "PROCESSING",
    "refundAmount": 100
  }
}
```

申请退款流程如下：

```text
接收退款申请
  |
  |-- 校验操作权限
  |-- 查询业务订单
  |-- 查询支付流水
  |-- 校验订单是否已支付
  |-- 校验可退款金额
  |-- 创建退款流水
  |-- 调用微信支付退款接口
  |-- 保存微信退款响应
  |-- 更新订单为 REFUNDING
  |-- 返回退款申请结果
```

退款请求中的关键字段如下：

| 字段              | 说明                                       |
| ----------------- | ------------------------------------------ |
| `transaction_id`  | 微信支付交易号，与 `out_trade_no` 二选一   |
| `out_trade_no`    | 微信商户订单号，与 `transaction_id` 二选一 |
| `out_refund_no`   | 商户退款单号，必须唯一                     |
| `reason`          | 退款原因                                   |
| `notify_url`      | 退款结果通知地址                           |
| `amount.refund`   | 退款金额，单位：分                         |
| `amount.total`    | 原订单金额，单位：分                       |
| `amount.currency` | 货币类型，通常为 `CNY`                     |

申请退款注意事项：

1. 只有已支付订单允许发起退款。
2. 退款金额不能大于订单可退金额。
3. 多次部分退款时，需要累计计算已退款金额。
4. `out_refund_no` 必须全局唯一。
5. 退款申请成功不等于退款到账成功，最终结果应以退款查询或退款回调为准。
6. 退款接口失败时，应记录失败原因，不应直接把订单更新为已退款。
7. 退款成功后的业务回滚，例如关闭权益、回滚库存、退还积分，应保证幂等。

### 查询退款

查询退款用于获取退款单的最新状态。该接口可用于管理端查看退款进度，也可用于退款回调异常时的补偿任务。

查询退款接口定义如下：

| 项目     | 说明                                |
| -------- | ----------------------------------- |
| 请求方式 | `GET`                               |
| 接口路径 | `/api/pay/wx/refunds/{outRefundNo}` |
| 是否登录 | 是                                  |

响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "orderNo": "ORDER202605070001",
    "outTradeNo": "WX202605071530000001",
    "outRefundNo": "WXR202605070001",
    "refundId": "5030200001202605071234567890",
    "refundStatus": "SUCCESS",
    "refundAmount": 100,
    "successTime": "2026-05-07 16:00:00"
  }
}
```

查询退款流程如下：

```text
接收退款查询请求
  |
  |-- 查询本地退款流水
  |-- 调用微信支付退款查询接口
  |-- 对比微信退款状态和本地退款状态
  |-- 必要时更新本地退款流水
  |-- 返回退款状态
```

退款状态同步规则如下：

| 微信退款状态 | 本地退款状态 | 处理方式                   |
| ------------ | ------------ | -------------------------- |
| `SUCCESS`    | `SUCCESS`    | 更新退款成功时间和退款金额 |
| `CLOSED`     | `CLOSED`     | 标记退款关闭               |
| `PROCESSING` | `PROCESSING` | 保持退款中                 |
| `ABNORMAL`   | `ABNORMAL`   | 标记异常，等待人工处理     |

查询退款注意事项：

1. 查询接口优先根据 `out_refund_no` 查询。
2. 本地不存在退款流水时，不应直接调用微信接口盲查。
3. 查询到退款成功后，需要同步更新订单累计退款金额。
4. 全额退款成功后，订单状态更新为 `REFUNDED`。
5. 部分退款成功后，订单状态更新为 `PARTIAL_REFUNDED`。
6. 退款异常状态应进入人工处理或定时补偿流程。

### 退款回调处理

退款回调用于接收微信支付平台推送的退款结果通知。退款回调与支付回调类似，同样需要验签、解密、记录日志和幂等处理。

退款回调接口定义如下：

| 项目         | 说明                        |
| ------------ | --------------------------- |
| 请求方式     | `POST`                      |
| 接口路径     | `/api/pay/wx/refund/notify` |
| 是否登录     | 否                          |
| Content-Type | `application/json`          |
| 调用方       | 微信支付平台                |

成功响应：

```json
{
  "code": "SUCCESS",
  "message": "成功"
}
```

失败响应：

```json
{
  "code": "FAIL",
  "message": "处理失败"
}
```

退款回调处理流程如下：

```text
接收微信退款回调
  |
  |-- 读取请求头和原始请求体
  |-- 保存退款回调日志
  |-- 验证微信支付签名
  |-- 解密 resource 数据
  |-- 解析退款结果
  |-- 校验商户订单号、退款单号、金额
  |-- 查询退款流水
  |-- 幂等更新退款流水
  |-- 更新订单退款状态
  |-- 触发退款成功后的业务回滚
  |-- 返回 SUCCESS
```

退款回调解密后常用字段如下：

| 字段                  | 说明                       |
| --------------------- | -------------------------- |
| `mchid`               | 商户号                     |
| `out_trade_no`        | 微信商户订单号             |
| `transaction_id`      | 微信支付交易号             |
| `out_refund_no`       | 商户退款单号               |
| `refund_id`           | 微信退款单号               |
| `refund_status`       | 退款状态                   |
| `success_time`        | 退款成功时间               |
| `amount.total`        | 原订单金额，单位：分       |
| `amount.refund`       | 退款金额，单位：分         |
| `amount.payer_total`  | 用户实际支付金额，单位：分 |
| `amount.payer_refund` | 用户实际退款金额，单位：分 |

退款回调状态更新建议：

```sql
UPDATE pay_refund_record
SET refund_status = 'SUCCESS',
    refund_id = #{refundId},
    success_time = #{successTime},
    update_time = NOW()
WHERE out_refund_no = #{outRefundNo}
  AND refund_status = 'PROCESSING';
```

订单退款状态更新建议：

```sql
-- 部分退款成功
UPDATE trade_order
SET order_status = 'PARTIAL_REFUNDED',
    update_time = NOW()
WHERE order_no = #{orderNo}
  AND order_status IN ('PAID', 'REFUNDING', 'PARTIAL_REFUNDED')
  AND #{totalRefundAmount} < pay_amount;

-- 全额退款成功
UPDATE trade_order
SET order_status = 'REFUNDED',
    update_time = NOW()
WHERE order_no = #{orderNo}
  AND order_status IN ('PAID', 'REFUNDING', 'PARTIAL_REFUNDED')
  AND #{totalRefundAmount} >= pay_amount;
```

退款回调注意事项：

1. 退款回调接口不需要登录鉴权，但必须进行微信支付验签。
2. 必须使用原始请求体参与验签。
3. 退款金额必须与本地退款流水金额一致。
4. `out_refund_no` 必须能匹配本地退款流水。
5. 重复退款回调应幂等返回成功。
6. 退款成功后的业务回滚必须幂等，例如关闭会员权益、撤销发货、退还积分。
7. 退款异常状态应记录日志并进入人工处理或补偿任务。
8. 支付回调日志表可以通过 `notify_type = REFUND` 复用，也可以单独建立退款回调日志表。



## 异常处理

本章节用于说明微信支付接入过程中的异常处理策略，包括支付失败、回调异常和订单状态不一致。支付模块属于资金相关模块，异常处理不能只依赖日志记录，还需要具备状态补偿、幂等控制、告警通知和人工介入能力。

微信支付官方 Java SDK 会在调用微信支付服务或处理回调时抛出不同异常，例如 HTTP 请求异常、签名验证失败、微信支付服务返回错误、回调报文解析或解密失败等。对回调通知，官方 SDK 要求使用原始请求体构建 `RequestParam`，并通过 `NotificationParser.parse()` 完成验签、解密和对象转换；处理成功返回 `200 OK`，处理失败返回 `4xx` 或 `5xx`。([GitHub](https://github.com/wechatpay-apiv3/wechatpay-java?utm_source=chatgpt.com))

### 支付失败处理

支付失败处理用于应对下单失败、用户取消支付、支付超时、微信支付返回失败状态、网络异常和本地数据库异常等场景。支付失败不应直接等同于订单失败，需要根据失败发生的位置和状态进行分类处理。

常见支付失败场景如下：

| 场景             | 典型原因                               | 处理方式                                                   |
| ---------------- | -------------------------------------- | ---------------------------------------------------------- |
| 下单接口调用失败 | 参数错误、签名错误、证书错误、网络异常 | 记录失败原因，支付流水标记为 `PAY_FAILED` 或保持可重试状态 |
| 用户取消支付     | 用户主动关闭支付页面                   | 前端提示取消，后端保持 `WAIT_PAY` 或 `PAYING`              |
| 支付超时         | 用户长时间未付款                       | 定时任务关闭本地订单和微信支付订单                         |
| 微信返回支付失败 | 余额不足、风控拦截、支付受限           | 记录微信错误码和错误描述，提示用户更换支付方式             |
| 本地更新失败     | 数据库异常、锁等待超时                 | 返回失败，等待回调重试或补偿任务处理                       |
| 前端显示失败     | 前端调起支付失败或网络断开             | 前端重新查询订单状态，不直接修改支付状态                   |

支付失败状态建议如下：

| 本地支付状态 | 说明                               | 是否允许重新支付             |
| ------------ | ---------------------------------- | ---------------------------- |
| `WAIT_PAY`   | 尚未创建微信预支付订单             | 允许                         |
| `PAYING`     | 已创建微信预支付订单，等待用户支付 | 允许继续等待，不建议重复创建 |
| `PAY_FAILED` | 支付流程明确失败                   | 允许重新创建支付流水         |
| `CLOSED`     | 支付订单已关闭                     | 允许重新创建新的支付流水     |
| `PAID`       | 已支付成功                         | 不允许重新支付               |

支付失败处理流程如下：

```text
支付请求失败或支付状态异常
        |
        v
判断失败发生阶段
        |
        |-- 下单前失败：直接返回错误，不创建支付流水
        |-- 下单中失败：记录请求和异常，支付流水标记为失败或待补偿
        |-- 用户支付中失败：前端提示用户重试或查询订单状态
        |-- 支付后本地失败：依赖回调重试或主动查询补偿
        |
        v
记录异常日志和支付流水状态
        |
        v
必要时触发告警或补偿任务
```

支付失败处理建议：

1. 微信支付接口调用失败时，需要记录请求参数、响应错误码、错误信息和商户订单号。
2. 用户取消支付不应立即关闭订单，可以等待订单过期或用户重新发起支付。
3. 支付超时应由定时任务扫描关闭，避免订单长期处于 `PAYING`。
4. 下单失败时，如果微信支付未创建订单，本地支付流水可以标记为 `PAY_FAILED`。
5. 如果微信支付返回结果不明确，例如网络超时，应主动查询微信支付订单状态后再决定本地状态。
6. 支付失败后允许重新支付时，必须生成新的商户订单号 `out_trade_no`。
7. 对于证书错误、签名错误、API v3 密钥错误等系统性异常，应立即告警。

建议记录的异常信息如下：

| 字段               | 说明                                           |
| ------------------ | ---------------------------------------------- |
| `order_no`         | 本地业务订单号                                 |
| `out_trade_no`     | 微信商户订单号                                 |
| `pay_type`         | 支付方式                                       |
| `error_stage`      | 异常阶段：CREATE、QUERY、CLOSE、NOTIFY、REFUND |
| `error_code`       | 微信支付或系统错误码                           |
| `error_message`    | 错误描述                                       |
| `request_content`  | 请求参数                                       |
| `response_content` | 响应内容                                       |
| `trace_id`         | 请求链路 ID                                    |

### 回调异常处理

回调异常处理用于应对微信支付异步通知中的验签失败、解密失败、金额不一致、订单不存在、重复通知、数据库更新失败等场景。回调异常必须谨慎处理，不能在未确认通知真实性和金额正确性的情况下更新订单状态。

常见回调异常如下：

| 异常类型     | 可能原因                                    | 处理方式                     |
| ------------ | ------------------------------------------- | ---------------------------- |
| 验签失败     | 请求体被改动、平台证书错误、未使用原始 body | 返回失败，记录安全日志       |
| 解密失败     | API v3 密钥错误、回调资源数据异常           | 返回失败，记录异常日志并告警 |
| 订单不存在   | 本地没有对应 `out_trade_no`                 | 返回失败或进入异常队列       |
| 金额不一致   | 本地金额与微信回调金额不一致                | 拒绝更新订单，触发告警       |
| 商户号不一致 | 回调不属于当前商户                          | 拒绝处理，记录安全日志       |
| 重复通知     | 微信支付重试或网络重放                      | 幂等返回成功                 |
| 数据库异常   | 更新订单或流水失败                          | 返回失败，让微信支付后续重试 |

回调异常处理流程如下：

```text
接收微信支付回调
        |
        v
保存原始回调日志
        |
        v
验签和解密
        |
        |-- 失败：记录失败原因，返回 4xx 或 5xx
        |
        v
校验商户号、订单号、金额
        |
        |-- 不一致：标记异常，触发告警，返回失败
        |
        v
执行业务状态更新
        |
        |-- 成功：更新回调日志，返回 SUCCESS
        |-- 失败：记录异常，返回失败，等待重试
```

回调异常响应策略如下：

| 处理结果     | HTTP 状态      | 响应体                                 | 说明                 |
| ------------ | -------------- | -------------------------------------- | -------------------- |
| 处理成功     | `200`          | `{"code":"SUCCESS","message":"成功"}`  | 微信支付认为通知成功 |
| 验签失败     | `401` 或 `400` | `{"code":"FAIL","message":"验签失败"}` | 拒绝伪造或异常通知   |
| 业务处理失败 | `500`          | `{"code":"FAIL","message":"处理失败"}` | 允许微信支付后续重试 |
| 重复回调     | `200`          | `{"code":"SUCCESS","message":"成功"}`  | 已处理过，幂等成功   |

官方 Java SDK 对回调通知处理的建议是：验签失败会抛出 `ValidationException`，业务处理成功返回 `200 OK`，业务处理失败返回 `4xx` 或 `5xx`，例如数据库操作失败返回 `500 Internal Server Error`。([GitHub](https://github.com/wechatpay-apiv3/wechatpay-java?utm_source=chatgpt.com))

回调异常处理建议：

1. 验签失败不能继续解密和更新订单。
2. 解密失败通常与 API v3 密钥、回调报文或配置错误有关，需要告警。
3. 金额不一致属于高危异常，必须阻断订单状态更新。
4. 订单不存在时，建议记录异常并进入人工排查或补偿队列。
5. 数据库更新失败时，不要返回成功，应让微信支付后续重试。
6. 已成功处理过的回调再次到达时，应直接返回成功。
7. 回调日志必须记录原始请求体、请求头、解密结果、处理状态和失败原因。

### 订单状态不一致处理

订单状态不一致处理用于解决本地订单状态、支付流水状态和微信支付交易状态之间不一致的问题。这类问题通常由回调延迟、网络超时、数据库异常、定时任务并发、人工操作或重复支付导致。

常见状态不一致场景如下：

| 本地订单状态 | 支付流水状态 | 微信支付状态 | 处理建议                           |
| ------------ | ------------ | ------------ | ---------------------------------- |
| `PAYING`     | `PAYING`     | `SUCCESS`    | 更新本地订单和支付流水为已支付     |
| `CLOSED`     | `CLOSED`     | `SUCCESS`    | 标记异常，主动查单，人工确认后处理 |
| `PAID`       | `PAYING`     | `SUCCESS`    | 修正支付流水为已支付               |
| `WAIT_PAY`   | 无支付流水   | `SUCCESS`    | 高危异常，人工确认来源             |
| `PAID`       | `PAID`       | `CLOSED`     | 以支付成功记录为准，保留异常日志   |
| `REFUNDED`   | `PAID`       | `SUCCESS`    | 结合退款流水判断，不回退订单状态   |

状态不一致补偿流程如下：

```text
扫描异常订单或收到异常回调
        |
        v
查询本地订单和支付流水
        |
        v
主动查询微信支付订单状态
        |
        v
按状态映射规则修正本地状态
        |
        v
记录补偿日志
        |
        v
必要时触发人工审核
```

补偿任务建议如下：

| 任务           | 扫描条件                                    | 执行频率         |
| -------------- | ------------------------------------------- | ---------------- |
| 支付中订单补偿 | `PAYING` 超过 3 到 5 分钟                   | 每 1 到 5 分钟   |
| 超时订单关闭   | `WAIT_PAY` 或 `PAYING` 且超过 `expire_time` | 每 1 到 5 分钟   |
| 回调失败补偿   | 回调日志 `FAILED`                           | 每 5 到 10 分钟  |
| 退款中补偿     | 退款状态 `PROCESSING` 超过一定时间          | 每 10 到 30 分钟 |
| 状态不一致检查 | 订单和流水状态不一致                        | 每 10 到 30 分钟 |

状态修复建议使用条件更新：

```sql
-- 将支付流水修正为已支付，仅允许支付中状态变更
UPDATE pay_record
SET pay_status = 'PAID',
    transaction_id = #{transactionId},
    success_time = #{successTime},
    update_time = NOW()
WHERE out_trade_no = #{outTradeNo}
  AND pay_status = 'PAYING';

-- 将业务订单修正为已支付，仅允许待支付或支付中状态变更
UPDATE trade_order
SET order_status = 'PAID',
    pay_status = 'PAID',
    pay_time = #{successTime},
    update_time = NOW()
WHERE order_no = #{orderNo}
  AND order_status IN ('WAIT_PAY', 'PAYING');
```

订单状态不一致处理建议：

1. 修正状态前应优先查询微信支付订单状态。
2. 已支付、已退款等终态不能被低优先级状态覆盖。
3. 修正操作必须记录补偿日志，包括修正前状态、修正后状态和依据。
4. 金额不一致、订单不存在、已关闭后支付成功等场景必须进入人工审核。
5. 补偿任务应分批执行，避免对数据库和微信支付接口造成压力。
6. 生产环境应提供管理端查询异常订单和手动重试能力。

## 测试与验证

本章节用于说明微信支付功能在开发、测试、预发和上线前的验证方式。支付测试不能只验证“能调起支付”，还需要验证下单、回调、查单、关单、退款、重复回调、异常补偿和日志告警。

测试阶段建议至少覆盖以下内容：

1. 支付参数是否正确。
2. 商户私钥和证书序列号是否可用。
3. 微信支付客户端是否能正常初始化。
4. 各支付方式是否能正常下单。
5. 支付回调是否能完成验签和解密。
6. 订单状态是否能正确更新。
7. 重复回调是否幂等。
8. 超时订单是否能关闭。
9. 退款申请、查询和回调是否正常。
10. 异常日志和告警是否能触发。

### 本地回调调试

本地回调调试用于验证支付回调接口是否能接收微信支付通知，以及回调验签、解密和业务处理逻辑是否正确。由于微信支付需要访问公网地址，本地服务通常需要通过内网穿透、测试环境部署或网关转发暴露 HTTPS 地址。

本地调试流程如下：

```text
启动本地 Spring Boot 服务
        |
        v
通过内网穿透暴露 HTTPS 地址
        |
        v
将通知地址配置为公网 HTTPS 地址
        |
        v
发起测试支付
        |
        v
微信支付回调本地接口
        |
        v
检查回调日志、订单状态和支付流水
```

本地启动命令如下：

```bash
# 启动本地开发环境
java -jar app.jar --spring.profiles.active=dev
```

本地回调地址示例：

```text
https://pay-dev.example.com/api/pay/wx/notify
https://pay-dev.example.com/api/pay/wx/refund/notify
```

如果需要先验证接口可达性，可以使用普通请求测试接口路径是否可访问，但该方式不能替代微信支付真实回调验签。

```bash
# 仅用于验证接口是否可访问，不能用于验证微信支付签名
curl -X POST "https://pay-dev.example.com/api/pay/wx/notify" \
  -H "Content-Type: application/json" \
  -d '{"mock":"ping"}'
```

本地回调调试检查项如下：

| 检查项             | 预期结果                                               |
| ------------------ | ------------------------------------------------------ |
| 回调地址公网可访问 | 外网请求能到达后端服务                                 |
| HTTPS 证书有效     | 微信支付能正常请求回调地址                             |
| 原始请求体完整     | 后端能读取完整 body                                    |
| 请求头完整         | 能获取 `Wechatpay-Signature` 等请求头                  |
| 验签成功           | SDK 不抛出验签异常                                     |
| 解密成功           | 能解析 `out_trade_no`、`transaction_id`、`trade_state` |
| 状态更新成功       | 订单和支付流水变更为 `PAID`                            |
| 回调响应成功       | 返回 `200` 和 `SUCCESS` 响应                           |

本地回调调试注意事项：

1. 不要使用反向代理修改回调请求体。
2. 不要在 Controller 中提前消费请求流后又重复读取 body。
3. 网关、Nginx、过滤器不能丢失微信支付请求头。
4. 回调接口不需要登录鉴权，但必须保留验签逻辑。
5. 内网穿透地址变化后，需要同步更新支付通知地址。
6. 验签失败时，优先检查是否使用原始 body。

### 沙箱与联调验证

沙箱与联调验证用于在不影响生产交易的前提下验证支付链路。不同商户、不同支付产品和不同接入模式可用的测试能力可能不同，因此联调方案应以商户平台当前可用能力为准。

建议采用以下联调策略：

| 联调方式       | 适用场景 | 说明                                               |
| -------------- | -------- | -------------------------------------------------- |
| Mock 回调      | 开发早期 | 验证本地状态机、幂等和异常处理，但不能验证真实验签 |
| 测试商户联调   | 测试环境 | 使用测试商户参数验证完整支付链路                   |
| 小额真实支付   | 预发验证 | 使用低金额订单验证真实扣款、回调和退款             |
| 管理端补偿验证 | 异常场景 | 验证查单、关单、退款查询和状态修复能力             |
| 灰度商户验证   | 生产灰度 | 小范围真实用户或内部用户验证                       |

联调环境建议配置如下：

```yaml
wechat:
  pay:
    # 测试环境商户号
    mch-id: "1900000000"

    # 测试环境 AppID
    app-id: "wx1234567890abcdef"

    # 测试环境商户 API 证书序列号
    merchant-serial-number: "TEST_SERIAL_NUMBER"

    # 测试环境私钥路径
    private-key-path: "/data/cert/wechat-test/apiclient_key.pem"

    # 测试环境 API v3 密钥
    api-v3-key: "${WECHAT_PAY_TEST_API_V3_KEY}"

    # 测试环境支付回调地址
    pay-notify-url: "https://pay-test.example.com/api/pay/wx/notify"

    # 测试环境退款回调地址
    refund-notify-url: "https://pay-test.example.com/api/pay/wx/refund/notify"
```

联调验证顺序建议如下：

```text
验证配置加载
  |
  |-- 验证证书文件读取
  |-- 验证微信支付客户端初始化
  |-- 验证 Native 下单
  |-- 验证扫码支付
  |-- 验证支付回调
  |-- 验证订单查询
  |-- 验证关闭订单
  |-- 验证申请退款
  |-- 验证退款回调
```

联调验证注意事项：

1. 测试环境和生产环境必须使用不同配置文件或配置命名空间。
2. 测试商户参数不能混入生产环境。
3. 小额真实支付完成后，需要同步验证退款流程。
4. Mock 回调只能验证业务流程，不能证明微信支付验签配置正确。
5. 如果支付产品需要商户平台开通或域名配置，必须提前完成。
6. H5 支付、JSAPI 支付、小程序支付、APP 支付应分别验证，不要只验证 Native 支付。

### 支付流程验证

支付流程验证用于确认从创建订单到支付成功的完整链路可用。不同支付方式应分别验证下单参数、前端调起参数、回调处理和本地状态更新。

支付流程验证用例如下：

| 用例            | 操作                             | 预期结果                                  |
| --------------- | -------------------------------- | ----------------------------------------- |
| Native 支付成功 | 创建 Native 支付订单并扫码付款   | 返回 `codeUrl`，支付成功后订单变为 `PAID` |
| JSAPI 支付成功  | 公众号内创建支付订单并付款       | 返回调起支付参数，回调成功                |
| 小程序支付成功  | 小程序创建支付订单并付款         | 小程序调起支付成功，订单变为 `PAID`       |
| H5 支付成功     | 手机浏览器创建支付订单并跳转支付 | 返回 `h5Url`，支付完成后状态正确          |
| APP 支付成功    | App 创建支付订单并调起微信       | 返回 App 支付参数，回调成功               |
| 用户取消支付    | 用户关闭支付页面                 | 本地订单保持 `WAIT_PAY` 或 `PAYING`       |
| 支付超时关闭    | 超过订单过期时间未支付           | 定时任务关闭订单和支付流水                |
| 重复回调        | 多次发送同一支付成功通知         | 订单只更新一次，重复回调返回成功          |
| 金额不一致      | 模拟回调金额异常                 | 拒绝更新订单并触发告警                    |

创建支付订单测试命令如下：

```bash
# 创建 Native 支付订单
curl -X POST "https://pay-test.example.com/api/pay/wx/orders" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${TOKEN}" \
  -d '{
    "orderNo": "ORDER202605070001",
    "payType": "NATIVE"
  }'
```

查询支付状态测试命令如下：

```bash
# 查询订单支付状态
curl -X GET "https://pay-test.example.com/api/pay/wx/orders/ORDER202605070001" \
  -H "Authorization: Bearer ${TOKEN}"
```

关闭支付订单测试命令如下：

```bash
# 关闭未支付订单
curl -X POST "https://pay-test.example.com/api/pay/wx/orders/ORDER202605070001/close" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${TOKEN}" \
  -d '{
    "reason": "测试关闭未支付订单"
  }'
```

支付流程验证通过标准如下：

1. 下单接口返回支付方式对应参数。
2. 本地支付流水生成成功。
3. 微信支付回调能正常验签和解密。
4. 支付成功后订单状态为 `PAID`。
5. 支付成功后支付流水状态为 `PAID`。
6. 重复回调不会重复发放权益。
7. 超时订单能自动关闭。
8. 支付异常能记录日志并触发告警。

### 退款流程验证

退款流程验证用于确认已支付订单可以发起退款、查询退款状态，并通过退款回调同步本地订单状态。退款流程必须覆盖全额退款、部分退款、重复退款请求和退款异常场景。

退款流程验证用例如下：

| 用例         | 操作                     | 预期结果                                |
| ------------ | ------------------------ | --------------------------------------- |
| 全额退款     | 对已支付订单发起全额退款 | 退款成功后订单状态为 `REFUNDED`         |
| 部分退款     | 对已支付订单发起部分退款 | 退款成功后订单状态为 `PARTIAL_REFUNDED` |
| 重复退款     | 重复提交同一退款单号     | 幂等返回，不重复退款                    |
| 超额退款     | 退款金额大于可退金额     | 请求被拒绝                              |
| 未支付退款   | 对未支付订单申请退款     | 请求被拒绝                              |
| 退款回调     | 微信支付推送退款结果     | 退款流水和订单状态正确更新              |
| 退款查询补偿 | 回调未到达时主动查询退款 | 本地状态能被修正                        |

申请退款测试命令如下：

```bash
# 对已支付订单发起退款
curl -X POST "https://pay-test.example.com/api/pay/wx/refunds" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${TOKEN}" \
  -d '{
    "orderNo": "ORDER202605070001",
    "refundAmount": 100,
    "reason": "测试退款"
  }'
```

查询退款测试命令如下：

```bash
# 查询退款状态
curl -X GET "https://pay-test.example.com/api/pay/wx/refunds/WXR202605070001" \
  -H "Authorization: Bearer ${TOKEN}"
```

退款流程验证通过标准如下：

1. 只有已支付订单允许退款。
2. 退款金额不能大于可退金额。
3. 退款流水能正确生成。
4. 微信退款单号能正确保存。
5. 退款回调能正常验签和解密。
6. 全额退款后订单状态为 `REFUNDED`。
7. 部分退款后订单状态为 `PARTIAL_REFUNDED`。
8. 重复退款回调不会重复更新订单。
9. 退款异常能进入补偿或人工处理流程。

## 上线准备

本章节用于说明微信支付模块上线前的检查项，包括生产参数、证书安全、日志告警和灰度验证。支付功能上线前必须完成完整链路验证，不能只验证接口启动成功。

上线前建议按以下顺序检查：

```text
生产参数检查
        |
        v
证书和密钥检查
        |
        v
支付链路验证
        |
        v
退款链路验证
        |
        v
日志和告警检查
        |
        v
灰度发布
        |
        v
生产监控观察
```

### 生产参数检查

生产参数检查用于确认正式环境使用的商户号、AppID、证书、密钥、回调地址和支付产品权限均正确。生产参数错误会直接导致下单失败、验签失败、回调失败或资金进入错误商户。

生产参数检查表如下：

| 检查项            | 检查内容                                  | 预期结果                    |
| ----------------- | ----------------------------------------- | --------------------------- |
| 商户号            | `mch-id` 是否为生产商户号                 | 与商户平台一致              |
| AppID             | `app-id` 是否为生产应用 AppID             | 与公众号、小程序或 App 一致 |
| 商户证书序列号    | `merchant-serial-number` 是否正确         | 与当前商户 API 证书一致     |
| 私钥路径          | `private-key-path` 是否存在               | 应用用户可读取              |
| API v3 密钥       | 是否为生产 API v3 密钥                    | 长度 32 位，来源可信        |
| 支付回调地址      | `pay-notify-url` 是否为生产 HTTPS 地址    | 公网可访问                  |
| 退款回调地址      | `refund-notify-url` 是否为生产 HTTPS 地址 | 公网可访问                  |
| 支付产品权限      | JSAPI、Native、H5、APP 是否开通           | 与业务支付方式一致          |
| H5 支付域名       | H5 场景是否已配置域名                     | 商户平台配置正确            |
| 小程序/公众号绑定 | AppID 与商户号是否绑定                    | 支付权限正常                |

生产配置示例：

```yaml
wechat:
  pay:
    # 生产商户号
    mch-id: "${WECHAT_PAY_MCH_ID}"

    # 生产应用 AppID
    app-id: "${WECHAT_PAY_APP_ID}"

    # 生产商户 API 证书序列号
    merchant-serial-number: "${WECHAT_PAY_MERCHANT_SERIAL_NUMBER}"

    # 生产商户 API 私钥路径
    private-key-path: "/data/cert/wechat-prod/apiclient_key.pem"

    # 生产 API v3 密钥
    api-v3-key: "${WECHAT_PAY_API_V3_KEY}"

    # 生产支付回调地址
    pay-notify-url: "https://api.example.com/api/pay/wx/notify"

    # 生产退款回调地址
    refund-notify-url: "https://api.example.com/api/pay/wx/refund/notify"

    # 生产订单过期时间
    order-expire-minutes: 30
```

生产参数检查建议：

1. 测试环境配置不能出现在生产配置中。
2. 生产 API v3 密钥不能写入代码仓库。
3. 生产回调地址必须使用 HTTPS。
4. 生产回调域名不能使用临时内网穿透地址。
5. 每个支付方式都需要单独验证权限和参数。
6. 上线前应完成一次小额真实支付和退款验证。

### 证书安全检查

证书安全检查用于确认商户 API 私钥、API v3 密钥、平台证书或微信支付公钥的存储方式和访问权限满足生产安全要求。支付证书泄露可能导致严重资金风险，因此不能以普通配置文件方式随意分发。

证书安全检查表如下：

| 检查项       | 预期要求                              |
| ------------ | ------------------------------------- |
| 私钥文件     | 不提交代码仓库，不打入镜像            |
| 私钥权限     | 文件权限建议为 `600`                  |
| 证书目录权限 | 目录权限建议为 `700`                  |
| API v3 密钥  | 使用环境变量、Secret 或密钥系统注入   |
| 日志脱敏     | 不打印私钥、API v3 密钥和完整敏感回调 |
| 访问账号     | 仅应用运行用户可读取私钥              |
| 证书备份     | 安全备份，避免丢失                    |
| 证书轮换     | 有明确轮换流程和回滚方案              |

证书权限检查命令如下：

```bash
# 检查证书目录权限
ls -ld /data/cert/wechat-prod

# 检查商户 API 私钥权限
ls -l /data/cert/wechat-prod/apiclient_key.pem

# 检查当前应用用户是否可读取私钥
sudo -u appuser test -r /data/cert/wechat-prod/apiclient_key.pem && echo "private key readable"
```

证书目录和私钥权限设置命令如下：

```bash
# 限制证书目录权限
chmod 700 /data/cert/wechat-prod

# 限制私钥文件权限
chmod 600 /data/cert/wechat-prod/apiclient_key.pem

# 设置证书目录属主为应用运行用户
chown -R appuser:appuser /data/cert/wechat-prod
```

证书安全建议：

1. 不要将 `apiclient_key.pem` 放入 Git、Docker 镜像或普通制品包。
2. 不要在启动日志中打印 API v3 密钥。
3. 不要在异常日志中输出完整私钥内容。
4. 证书轮换时应先在预发环境验证。
5. 多实例部署时，应确保所有实例使用一致的证书配置。
6. 出现证书泄露风险时，应立即在商户平台更换证书和密钥。

### 日志与告警检查

日志与告警检查用于确保支付链路出现异常时能够及时发现、定位和处理。支付日志应围绕订单号、支付流水号、商户订单号、微信交易号和退款单号建立完整链路。

建议记录的关键日志如下：

| 日志类型 | 关键字段                                         | 说明                  |
| -------- | ------------------------------------------------ | --------------------- |
| 下单日志 | `order_no`、`out_trade_no`、`pay_type`、`amount` | 记录支付订单创建过程  |
| 回调日志 | `out_trade_no`、`transaction_id`、`trade_state`  | 记录支付回调处理过程  |
| 退款日志 | `out_refund_no`、`refund_id`、`refund_status`    | 记录退款处理过程      |
| 关闭日志 | `order_no`、`out_trade_no`、`close_result`       | 记录订单关闭结果      |
| 补偿日志 | `order_no`、`before_status`、`after_status`      | 记录状态修复过程      |
| 异常日志 | `error_code`、`error_message`、`trace_id`        | 记录异常原因和链路 ID |

建议配置的告警规则如下：

| 告警项                 | 触发条件                     | 告警等级 |
| ---------------------- | ---------------------------- | -------- |
| 下单失败率高           | 5 分钟内下单失败率超过阈值   | 高       |
| 回调验签失败           | 出现连续验签失败             | 高       |
| 回调解密失败           | 出现 API v3 解密失败         | 高       |
| 金额不一致             | 任意一笔金额不一致           | 严重     |
| 支付成功但本地更新失败 | 回调处理返回失败             | 高       |
| 订单长时间 `PAYING`    | 超过设定时间未完成           | 中       |
| 退款异常               | 退款状态为 `ABNORMAL`        | 高       |
| 证书加载失败           | 应用启动或运行中证书读取失败 | 严重     |

日志脱敏建议如下：

```text
API v3 密钥：不打印
商户 API 私钥：不打印
支付用户 openid：可部分脱敏
手机号：必须脱敏
身份证号：必须脱敏
银行卡号：必须脱敏
完整回调 body：生产环境按需保存，展示时应做权限控制
```

日志示例格式如下：

```text
[微信支付] 创建支付订单成功，orderNo=ORDER202605070001，outTradeNo=WX202605071530000001，payType=NATIVE，amount=100

[微信支付] 收到支付回调，outTradeNo=WX202605071530000001，transactionId=4200000000202605071234567890，tradeState=SUCCESS

[微信支付] 支付回调处理成功，orderNo=ORDER202605070001，outTradeNo=WX202605071530000001，payStatus=PAID

[微信支付] 支付回调金额不一致，orderNo=ORDER202605070001，localAmount=100，wechatAmount=1
```

日志与告警检查建议：

1. 所有支付日志必须包含 `order_no` 或 `out_trade_no`。
2. 回调异常必须记录请求头、原始 body、异常类型和处理结果。
3. 金额不一致、商户号不一致、验签失败必须触发告警。
4. 告警通知应接入企业微信、短信、电话或值班平台。
5. 生产环境日志查询应设置权限，避免敏感支付信息泄露。
6. 支付链路应支持按订单号快速检索完整调用过程。

### 灰度验证

灰度验证用于在正式全量开放前，以较小范围验证生产支付链路是否稳定。支付功能建议先由内部账号、测试商品、低金额订单或指定用户群进行灰度验证，再逐步扩大流量。

灰度验证阶段建议如下：

| 阶段       | 范围               | 验证内容                           |
| ---------- | ------------------ | ---------------------------------- |
| 内部灰度   | 内部测试账号       | 下单、支付、回调、退款、查单       |
| 小流量灰度 | 指定用户或指定商品 | 支付成功率、回调成功率、订单一致性 |
| 区域灰度   | 指定渠道或业务线   | 业务链路和财务数据                 |
| 全量发布   | 所有用户           | 监控稳定后逐步放开                 |

灰度验证流程如下：

```text
发布生产版本
        |
        v
启用内部账号或测试商品支付
        |
        v
完成小额支付和退款
        |
        v
检查订单、支付流水、回调日志、退款流水
        |
        v
观察日志和告警
        |
        v
扩大灰度范围
        |
        v
全量开放
```

灰度验证检查项如下：

| 检查项   | 预期结果                  |
| -------- | ------------------------- |
| 生产下单 | 微信支付下单成功          |
| 支付调起 | 前端能正常调起支付        |
| 支付回调 | 回调能正常验签和解密      |
| 订单状态 | 支付成功后订单变为 `PAID` |
| 支付流水 | 微信交易号正确保存        |
| 退款申请 | 已支付订单能发起退款      |
| 退款回调 | 退款状态能正确同步        |
| 补偿任务 | 超时和异常订单能自动处理  |
| 告警通知 | 异常能及时触发告警        |
| 日志检索 | 能按订单号追踪完整链路    |

灰度期间建议重点观察以下指标：

| 指标               | 说明                                          |
| ------------------ | --------------------------------------------- |
| 支付下单成功率     | 微信支付下单成功数 / 下单请求数               |
| 支付回调成功率     | 回调处理成功数 / 回调总数                     |
| 支付成功订单一致率 | 本地 `PAID` 订单与微信 `SUCCESS` 订单是否一致 |
| 平均支付耗时       | 从创建支付订单到收到支付成功回调的耗时        |
| 退款成功率         | 退款成功数 / 退款申请数                       |
| 异常订单数量       | 状态不一致、金额不一致、回调失败订单数量      |
| 告警数量           | 支付异常告警数量和处理时长                    |

灰度验证注意事项：

1. 灰度期间不要关闭支付异常告警。
2. 小额真实支付后必须验证退款。
3. 灰度订单需要和正常生产订单一样进入对账和日志体系。
4. 如果出现金额不一致、验签异常、订单状态错乱，应暂停扩大灰度。
5. 灰度完成后，需要保留验证记录，包括订单号、支付流水号、退款单号和处理结果。
6. 全量发布后至少观察一个完整业务高峰周期，再将支付链路视为稳定。