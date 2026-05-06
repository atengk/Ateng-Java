# 接口加密解密

接口加密解密用于在 Spring Boot 3 项目中对客户端与服务端之间传输的接口数据进行加密保护，降低请求参数、响应数据在传输链路、网关日志、代理节点或抓包场景中泄露的风险。该方案不替代 HTTPS，而是在 HTTPS 基础上增加应用层数据保护能力，适用于对敏感字段、业务请求体和接口响应内容有更高安全要求的系统。

## 功能概述

本章节说明接口加密解密能力的建设目标、适用范围以及系统处理边界，用于明确该能力解决什么问题、不解决什么问题，以及在 Spring Boot 3 项目中的集成位置。

### 建设目标

接口加密解密的建设目标是为系统提供一套统一、可配置、可扩展的应用层数据保护机制，避免各业务接口自行实现加密逻辑导致规则不一致、维护成本高和安全漏洞难以控制。

建设目标主要包括以下几个方面：

| 目标               | 说明                                                         |
| ------------------ | ------------------------------------------------------------ |
| 统一加密规范       | 统一请求加密、响应加密、签名校验、时间戳校验和随机字符串校验规则 |
| 降低业务侵入       | 通过 `RequestBodyAdvice`、`ResponseBodyAdvice`、拦截器和注解完成加解密处理，业务 Controller 尽量感知不到密文处理逻辑 |
| 支持灵活开关       | 支持全局开关、接口级开关、类级别开关和方法级别开关，便于灰度上线和分接口接入 |
| 防止数据泄露       | 对请求体和响应体中的敏感业务数据进行加密，降低链路日志、代理转发、抓包分析中的泄露风险 |
| 防止参数篡改       | 通过签名机制校验请求参数完整性，避免密文、时间戳、随机字符串等关键参数被篡改 |
| 防止重放攻击       | 通过时间戳、随机字符串和缓存记录机制限制同一请求被重复提交   |
| 兼容 Spring Boot 3 | 基于 Jakarta Servlet、Spring MVC 扩展点和 Spring Boot 3 配置方式实现，适配当前主流后端项目结构 |

该能力建设完成后，业务接口可以继续使用普通 DTO 接收请求参数、返回标准响应对象，由基础组件在进入 Controller 前完成请求解密，在响应写出前完成响应加密。

### 适用场景

接口加密解密适用于对传输数据安全性、参数完整性和接口防重放有明确要求的业务场景，尤其是移动端、Web 前端、小程序、开放平台和第三方系统对接场景。

常见适用场景如下：

| 场景               | 说明                                                         |
| ------------------ | ------------------------------------------------------------ |
| 登录认证接口       | 例如账号、手机号、验证码、密码、Token 等敏感信息传输         |
| 用户隐私数据接口   | 例如身份证号、银行卡号、手机号、邮箱、地址等隐私字段         |
| 支付与资金类接口   | 例如订单支付、余额变更、提现申请、账户流水查询               |
| 第三方开放接口     | 面向外部系统提供 API 时，要求调用方按照统一规则加密、签名和验签 |
| App 与服务端通信   | 移动端容易被抓包分析，适合对关键接口增加应用层加密           |
| 管理后台敏感操作   | 例如用户权限变更、配置变更、数据导出、账号冻结等高风险操作   |
| 内部系统跨网络调用 | 跨机房、跨网络区域或经过代理网关时，对敏感业务数据增加额外保护 |

不建议所有接口无差别强制加密。对于健康检查、静态资源、文件下载、公开字典数据、无敏感信息的查询接口，可以根据业务安全等级选择不加密或仅签名校验。

### 加密解密边界

接口加密解密边界用于明确哪些内容参与加密，哪些内容只参与签名或校验，避免实现过程中出现职责不清、重复加密或业务处理异常。

推荐边界如下：

| 数据位置    | 是否加密   | 是否参与签名 | 说明                                                         |
| ----------- | ---------- | ------------ | ------------------------------------------------------------ |
| 请求 Body   | 是         | 是           | 主要加密对象，客户端提交密文，服务端解密后映射为 DTO         |
| 响应 Body   | 是         | 可选         | 服务端返回密文，客户端解密后读取真实业务数据                 |
| 请求 Header | 否         | 是           | 存放时间戳、随机字符串、签名、密钥标识、加密版本等控制参数   |
| URL Path    | 否         | 是           | 例如 `/api/user/detail`，通常不加密，但可纳入签名防篡改      |
| Query 参数  | 可选       | 是           | GET 请求参数不建议放敏感数据；如必须传输，可参与签名或整体转为 POST Body |
| 文件上传    | 默认不加密 | 可选         | `multipart/form-data` 不建议直接纳入通用加解密组件，可单独设计文件加密方案 |
| 文件下载    | 默认不加密 | 可选         | 大文件响应不建议通过 `ResponseBodyAdvice` 整体加密，避免内存压力 |
| 错误响应    | 可配置     | 可选         | 对外接口建议加密错误响应，内部调试环境可关闭                 |

在 Spring Boot 3 中，推荐的处理边界如下：

请求进入系统后，拦截器先校验请求头中的时间戳、随机字符串、签名和加密开关标识；校验通过后，由 `RequestBodyAdvice` 读取密文请求体并完成解密；解密后的 JSON 再交给 Spring MVC 进行参数反序列化，最终进入 Controller。

响应返回时，Controller 仍然返回普通业务对象；`ResponseBodyAdvice` 根据接口注解、全局配置和响应类型判断是否需要加密；需要加密时，将响应对象序列化为 JSON 后加密，并包装为统一密文响应结构。

需要注意的是，接口加密解密只保护应用层业务数据，不负责解决以下问题：

| 非目标能力         | 说明                                                         |
| ------------------ | ------------------------------------------------------------ |
| 不替代 HTTPS       | 仍然必须使用 HTTPS 保证传输层安全                            |
| 不替代登录认证     | 用户身份认证、Token 校验、权限控制仍由认证授权模块负责       |
| 不替代接口权限     | 接口是否可访问仍需要 Spring Security、Sa-Token 或网关权限体系控制 |
| 不处理数据库加密   | 数据库存储加密、字段脱敏、日志脱敏属于独立安全能力           |
| 不直接保护前端密钥 | 前端密钥存在被逆向风险，需要结合密钥轮换、非对称加密和接口风控降低风险 |

## 技术方案

本章节说明接口加密解密的核心技术实现方案，包括加密算法、密钥管理、请求加密流程和响应加密流程。整体方案采用“对称加密处理业务数据，非对称加密保护对称密钥，签名机制保证完整性”的混合加密模型。

### 加密算法选择

接口加密解密建议采用混合加密方案：业务数据使用对称加密算法处理，密钥交换使用非对称加密算法保护，请求完整性使用签名算法校验。

推荐算法组合如下：

| 用途         | 推荐算法          | 说明                                               |
| ------------ | ----------------- | -------------------------------------------------- |
| 请求体加密   | AES-256-GCM       | 性能较好，同时提供机密性和完整性校验               |
| 响应体加密   | AES-256-GCM       | 可复用请求级会话密钥，也可由服务端生成新的响应密钥 |
| 会话密钥保护 | RSA-OAEP / SM2    | 客户端生成 AES 密钥后，使用服务端公钥加密传输      |
| 请求签名     | HMAC-SHA256 / SM3 | 对请求关键字段生成签名，防止参数篡改               |
| 摘要处理     | SHA-256 / SM3     | 对请求体、业务参数或原文生成摘要                   |
| 随机数生成   | SecureRandom      | 用于生成 AES Key、IV、Nonce 等随机数据             |

普通国际算法场景建议使用：

```text
AES-256-GCM + RSA-OAEP + HMAC-SHA256
```

国密合规场景建议使用：

```text
SM4 + SM2 + SM3
```

在 Spring Boot 3 常规企业项目中，优先推荐 `AES-256-GCM` 作为请求体和响应体加密算法。相比 AES-CBC，AES-GCM 可以同时提供加密和认证能力，能够发现密文被篡改的情况，安全性更适合接口数据加密场景。

推荐密文包装结构如下：

```json
{
  "keyId": "server-key-202605",
  "version": "v1",
  "algorithm": "AES-256-GCM",
  "encryptedKey": "使用服务端公钥加密后的AES密钥",
  "iv": "AES-GCM初始化向量",
  "timestamp": 1777996800000,
  "nonce": "随机字符串",
  "sign": "请求签名",
  "data": "加密后的业务数据"
}
```

其中：

| 字段           | 说明                                 |
| -------------- | ------------------------------------ |
| `keyId`        | 服务端密钥标识，用于支持密钥轮换     |
| `version`      | 加密协议版本，用于兼容后续升级       |
| `algorithm`    | 当前使用的加密算法                   |
| `encryptedKey` | 使用服务端公钥加密后的 AES 密钥      |
| `iv`           | AES-GCM 初始化向量，每次请求必须不同 |
| `timestamp`    | 客户端请求时间戳，用于过期校验       |
| `nonce`        | 请求随机字符串，用于防重放           |
| `sign`         | 请求签名，用于完整性校验             |
| `data`         | 加密后的业务 JSON 数据               |

### 密钥管理方式

密钥管理用于解决密钥如何生成、保存、读取、轮换和废弃的问题，是接口加密解密方案中最关键的安全边界之一。

推荐采用“长期非对称密钥 + 短期对称会话密钥”的管理方式：

| 密钥类型     | 生成方     | 生命周期         | 用途                                |
| ------------ | ---------- | ---------------- | ----------------------------------- |
| 服务端公钥   | 服务端     | 较长，支持轮换   | 下发给客户端，用于加密 AES 会话密钥 |
| 服务端私钥   | 服务端     | 较长，严格保护   | 解密客户端上传的 AES 会话密钥       |
| AES 会话密钥 | 客户端     | 单次请求或短会话 | 加密请求体和解密响应体              |
| 签名密钥     | 双方协商   | 中短期，支持轮换 | 生成和校验请求签名                  |
| 密钥 ID      | 服务端分配 | 与密钥版本一致   | 标识当前使用的密钥版本              |

服务端私钥不能写死在代码中，也不建议直接提交到 Git 仓库。生产环境推荐通过以下方式管理：

| 方式         | 适用场景       | 说明                                               |
| ------------ | -------------- | -------------------------------------------------- |
| 环境变量     | 中小型项目     | 启动时从环境变量读取私钥内容或私钥文件路径         |
| 配置中心     | 微服务项目     | 使用 Nacos、Apollo、Spring Cloud Config 等统一管理 |
| 密钥管理服务 | 高安全项目     | 使用 KMS、Vault 或云厂商密钥管理服务               |
| 本地密钥文件 | 开发或测试环境 | 仅用于非生产环境，必须限制文件权限                 |

推荐密钥配置示例：

```yaml
interface-crypto:
  # 是否启用接口加密解密
  enabled: true

  # 默认加密协议版本，便于后续平滑升级
  version: v1

  # 请求时间戳允许偏差，单位秒
  timestamp-expire-seconds: 300

  # 服务端当前密钥标识
  key-id: server-key-202605

  # 服务端私钥文件路径，生产环境建议通过环境变量或KMS管理
  private-key-location: ${CRYPTO_PRIVATE_KEY_LOCATION:/data/keys/server_private.pem}

  # 服务端公钥文件路径，用于本地调试或提供给客户端
  public-key-location: ${CRYPTO_PUBLIC_KEY_LOCATION:/data/keys/server_public.pem}

  # 是否加密错误响应
  encrypt-error-response: true

  # 不参与加解密的接口路径
  exclude-paths:
    - /actuator/**
    - /api/auth/public-key
    - /swagger-ui/**
    - /v3/api-docs/**
```

密钥轮换建议遵循以下规则：

| 规则                      | 说明                                                      |
| ------------------------- | --------------------------------------------------------- |
| 使用 `keyId` 标识密钥版本 | 客户端请求中携带 `keyId`，服务端根据 `keyId` 查找对应私钥 |
| 新旧密钥并行一段时间      | 避免客户端升级不一致导致请求失败                          |
| 私钥只在服务端保存        | 前端、小程序、App 不得保存服务端私钥                      |
| 公钥可通过接口下发        | 可提供 `/api/auth/public-key` 接口给客户端获取当前公钥    |
| 会话密钥每次请求生成      | 不建议长期复用同一个 AES Key                              |
| 记录密钥启停状态          | 密钥应包含启用、停用、废弃状态，便于审计和回滚            |

### 请求加密流程

请求加密流程描述客户端如何将明文业务参数转换为密文请求，以及服务端如何在进入 Controller 之前完成解密和校验。

推荐客户端请求加密步骤如下：

| 步骤 | 处理方 | 说明                                                 |
| ---- | ------ | ---------------------------------------------------- |
| 1    | 客户端 | 构造原始业务 JSON，例如登录参数、查询参数或提交数据  |
| 2    | 客户端 | 生成随机 AES 会话密钥                                |
| 3    | 客户端 | 生成随机 IV，AES-GCM 推荐 12 字节                    |
| 4    | 客户端 | 使用 AES 会话密钥加密业务 JSON，得到 `data`          |
| 5    | 客户端 | 使用服务端公钥加密 AES 会话密钥，得到 `encryptedKey` |
| 6    | 客户端 | 生成 `timestamp` 和 `nonce`                          |
| 7    | 客户端 | 使用约定规则生成签名 `sign`                          |
| 8    | 客户端 | 将密文数据、密钥信息和校验字段组装为统一请求结构     |
| 9    | 服务端 | 拦截器校验时间戳、随机字符串和签名                   |
| 10   | 服务端 | 使用服务端私钥解密 `encryptedKey`，得到 AES 会话密钥 |
| 11   | 服务端 | 使用 AES 会话密钥和 IV 解密 `data`，得到明文 JSON    |
| 12   | 服务端 | 将明文 JSON 交给 Spring MVC 反序列化为业务 DTO       |

推荐签名原文规则如下：

```text
METHOD + "\n" +
PATH + "\n" +
TIMESTAMP + "\n" +
NONCE + "\n" +
KEY_ID + "\n" +
ENCRYPTED_KEY + "\n" +
IV + "\n" +
DATA
```

示例：

```text
POST
/api/user/login
1777996800000
3f7f2c2d9d874cbf
server-key-202605
Base64(RSA加密后的AES密钥)
Base64(IV)
Base64(AES加密后的业务数据)
```

签名生成后放入请求头或请求体中。推荐控制字段放在请求头，密文数据放在请求体：

```http
POST /api/user/login HTTP/1.1
Content-Type: application/json
X-Crypto-Version: v1
X-Crypto-Key-Id: server-key-202605
X-Crypto-Timestamp: 1777996800000
X-Crypto-Nonce: 3f7f2c2d9d874cbf
X-Crypto-Sign: 生成后的签名
```

请求体示例：

```json
{
  "encryptedKey": "Base64(RSA-OAEP加密后的AES密钥)",
  "iv": "Base64(AES-GCM IV)",
  "data": "Base64(AES-GCM加密后的业务JSON)"
}
```

服务端处理时，建议将请求解密拆分为三个组件：

| 组件                       | 职责                                               |
| -------------------------- | -------------------------------------------------- |
| `CryptoProperties`         | 读取加密开关、密钥路径、过期时间、排除路径等配置   |
| `CryptoRequestInterceptor` | 校验时间戳、随机字符串、签名和接口是否需要加密     |
| `DecryptRequestBodyAdvice` | 解密请求体，并将明文 JSON 重新交给 Spring MVC 处理 |

请求流程可以概括为：

```text
客户端明文参数
  -> JSON序列化
  -> AES加密业务数据
  -> RSA加密AES密钥
  -> 生成时间戳、随机字符串和签名
  -> 发送密文请求
  -> 服务端校验请求头
  -> 服务端解密AES密钥
  -> 服务端解密业务数据
  -> Controller接收普通DTO
```

对于请求异常，建议返回统一错误码：

| 错误场景       | 建议错误码               | 说明                               |
| -------------- | ------------------------ | ---------------------------------- |
| 缺少加密请求头 | `CRYPTO_HEADER_MISSING`  | 必要的加密控制字段不存在           |
| 请求已过期     | `CRYPTO_REQUEST_EXPIRED` | 时间戳超过允许偏差                 |
| 重复请求       | `CRYPTO_REPLAY_REQUEST`  | `nonce` 已经被使用                 |
| 签名错误       | `CRYPTO_SIGN_INVALID`    | 请求字段被篡改或签名密钥不一致     |
| 密钥不存在     | `CRYPTO_KEY_NOT_FOUND`   | `keyId` 无法匹配服务端密钥         |
| 解密失败       | `CRYPTO_DECRYPT_FAILED`  | 密文格式错误、密钥错误或数据被篡改 |

### 响应加密流程

响应加密流程描述服务端如何将 Controller 返回的普通业务对象转换为密文响应，以及客户端如何解密得到真实业务数据。

推荐服务端响应加密步骤如下：

| 步骤 | 处理方     | 说明                                                     |
| ---- | ---------- | -------------------------------------------------------- |
| 1    | Controller | 返回普通业务响应对象，例如 `Result<UserLoginVO>`         |
| 2    | 服务端     | `ResponseBodyAdvice` 判断当前接口是否需要响应加密        |
| 3    | 服务端     | 将响应对象序列化为 JSON 字符串                           |
| 4    | 服务端     | 获取请求上下文中的 AES 会话密钥，或生成新的响应 AES 密钥 |
| 5    | 服务端     | 生成新的响应 IV                                          |
| 6    | 服务端     | 使用 AES-GCM 加密响应 JSON                               |
| 7    | 服务端     | 生成响应签名，可选                                       |
| 8    | 服务端     | 返回统一密文响应结构                                     |
| 9    | 客户端     | 使用约定 AES 密钥和 IV 解密响应 `data`                   |
| 10   | 客户端     | 反序列化明文 JSON 并进入业务处理                         |

推荐响应体结构如下：

```json
{
  "version": "v1",
  "algorithm": "AES-256-GCM",
  "keyId": "server-key-202605",
  "iv": "Base64(响应IV)",
  "timestamp": 1777996800000,
  "nonce": "响应随机字符串",
  "sign": "响应签名",
  "data": "Base64(AES-GCM加密后的响应JSON)"
}
```

响应加密可以采用两种密钥策略：

| 策略                    | 说明                                                      | 适用场景                                       |
| ----------------------- | --------------------------------------------------------- | ---------------------------------------------- |
| 复用请求 AES 会话密钥   | 请求解密成功后，将 AES Key 放入请求上下文，响应加密时复用 | 简单、高效，适合大多数业务接口                 |
| 服务端生成响应 AES 密钥 | 服务端重新生成 AES Key，再用客户端公钥加密后返回          | 安全边界更清晰，但客户端需要提供公钥或预置公钥 |

多数 Spring Boot 3 后端接口推荐使用第一种方式，即复用请求级 AES 会话密钥。该方式可以减少响应报文复杂度，也避免每次响应额外处理客户端公钥。

响应流程可以概括为：

```text
Controller返回普通对象
  -> ResponseBodyAdvice拦截响应
  -> 判断是否需要加密
  -> JSON序列化
  -> 获取请求级AES密钥
  -> AES加密响应JSON
  -> 包装密文响应结构
  -> 客户端解密响应数据
```

以下响应类型建议跳过通用响应加密：

| 响应类型                | 原因                                 |
| ----------------------- | ------------------------------------ |
| 文件下载响应            | 文件流不适合整体读入内存后加密       |
| 图片、音视频响应        | 二进制大对象不适合通用 JSON 加密结构 |
| `StreamingResponseBody` | 流式响应不适合统一响应包装           |
| Swagger / OpenAPI 文档  | 调试和文档接口应保持明文             |
| Actuator 健康检查       | 监控系统通常需要直接识别响应         |
| 已经加密的响应对象      | 避免重复加密                         |

对于异常响应，建议通过配置控制是否加密。生产环境面向外部调用方时，可以加密错误响应，避免错误信息中包含内部字段、栈信息或业务敏感数据。开发环境可以关闭异常响应加密，方便联调排查。

推荐异常响应处理策略如下：

| 环境     | 建议策略                                 |
| -------- | ---------------------------------------- |
| 开发环境 | 可关闭异常响应加密，便于调试             |
| 测试环境 | 根据联调需要开启或关闭                   |
| 预发环境 | 开启异常响应加密，模拟生产行为           |
| 生产环境 | 开启异常响应加密，并禁止返回详细堆栈信息 |

最终对业务代码而言，加密解密流程应保持透明。Controller 只处理明文 DTO 和普通响应对象，加解密、签名、防重放和密文包装均由基础安全组件统一完成。



## 项目基础配置

本章节用于说明接口加密解密能力接入 Spring Boot 3 项目前需要准备的依赖、配置项和密钥读取方式。基础配置完成后，后续请求解密、响应加密、签名校验和防重放处理都基于这些配置统一执行。

### Maven 依赖配置

接口加密解密功能依赖 Spring MVC 扩展点、Jackson JSON 序列化、Hutool 工具类和 Lombok。若需要防重放能力，建议接入 Redis，用于缓存已使用过的 `nonce`。

文件位置：`pom.xml`

```xml
<dependencies>
    <!-- Spring Boot Web：提供 RequestBodyAdvice、ResponseBodyAdvice、HandlerInterceptor 等能力 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- 参数校验：用于请求对象字段校验 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- Redis：用于 nonce 防重放缓存，可根据项目情况选择是否启用 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>

    <!-- Hutool：用于字符串、集合、Base64、文件读取等通用工具处理 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.8.36</version>
    </dependency>

    <!-- Lombok：简化 DTO、配置类、日志对象代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- 配置元数据生成：为 application.yml 提供配置提示 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-configuration-processor</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

如果项目没有使用 Redis，可以先去掉 `spring-boot-starter-data-redis`，后续防重放能力可先使用本地缓存实现。但生产环境不建议使用本地缓存，因为多实例部署时无法保证 `nonce` 全局唯一。

### 加密参数配置

加密参数建议统一放在 `application.yml` 中，通过 `@ConfigurationProperties` 读取。配置项应覆盖全局开关、默认加密策略、时间戳有效期、排除路径、密钥版本和密钥文件路径。

文件位置：`src/main/resources/application.yml`

```yaml
interface-crypto:
  # 是否启用接口加密解密总开关
  enabled: true

  # 是否默认开启请求体解密
  default-request-decrypt: false

  # 是否默认开启响应体加密
  default-response-encrypt: false

  # 是否加密异常响应
  encrypt-error-response: true

  # 当前默认密钥标识，用于密钥轮换
  default-key-id: server-key-202605

  # 请求时间戳允许偏差，单位：秒
  timestamp-expire-seconds: 300

  # AES-GCM 初始化向量长度，推荐 12 字节
  aes-gcm-iv-length: 12

  # AES-GCM 认证标签长度，推荐 128 bit
  aes-gcm-tag-length: 128

  # 不参与接口加解密处理的路径
  exclude-paths:
    - /actuator/**
    - /swagger-ui/**
    - /v3/api-docs/**
    - /api/crypto/public-key

  # 多版本密钥配置，key 为密钥标识
  keys:
    server-key-202605:
      # 服务端公钥路径，可用于提供给客户端
      public-key-location: classpath:keys/server_public.pem

      # 服务端私钥路径，生产环境建议改为文件路径或 KMS
      private-key-location: classpath:keys/server_private.pem

      # 签名密钥，生产环境建议从环境变量或配置中心读取
      sign-secret: ${CRYPTO_SIGN_SECRET:change-me-sign-secret}
```

该配置建议按环境拆分管理。开发环境可以使用 `classpath:keys/*.pem`；测试、预发和生产环境建议使用环境变量、挂载文件、配置中心或 KMS 读取密钥，避免私钥进入代码仓库。

生产环境配置示例：

```yaml
interface-crypto:
  enabled: true
  default-request-decrypt: false
  default-response-encrypt: false
  default-key-id: server-key-202605
  timestamp-expire-seconds: 300
  keys:
    server-key-202605:
      public-key-location: ${CRYPTO_PUBLIC_KEY_LOCATION:/data/keys/server_public.pem}
      private-key-location: ${CRYPTO_PRIVATE_KEY_LOCATION:/data/keys/server_private.pem}
      sign-secret: ${CRYPTO_SIGN_SECRET}
```

推荐约束如下：

| 配置项                     | 建议                                     |
| -------------------------- | ---------------------------------------- |
| `enabled`                  | 生产环境开启，开发环境可按需关闭         |
| `default-request-decrypt`  | 建议默认关闭，通过注解按接口开启         |
| `default-response-encrypt` | 建议默认关闭，通过注解按接口开启         |
| `timestamp-expire-seconds` | 建议 300 秒以内                          |
| `exclude-paths`            | 必须排除健康检查、接口文档、公钥获取接口 |
| `sign-secret`              | 生产环境不得使用默认值，不得提交到 Git   |

### 密钥配置读取

密钥配置读取组件负责从配置文件中读取密钥路径，并将 PEM 格式的 RSA 私钥解析为 `PrivateKey`。后续请求解密时，服务端根据请求头中的 `keyId` 查找对应密钥，再使用私钥解密 AES 会话密钥。

推荐文件结构如下：

```text
src/main/java/io/github/atengk/crypto/config/CryptoProperties.java
src/main/java/io/github/atengk/crypto/core/CryptoKeyProvider.java
src/main/java/io/github/atengk/crypto/exception/CryptoException.java
```

以下配置类用于承载 `application.yml` 中的 `interface-crypto` 配置。

文件位置：`src/main/java/io/github/atengk/crypto/config/CryptoProperties.java`

```java
package io.github.atengk.crypto.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 接口加密配置属性
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@ConfigurationProperties(prefix = "interface-crypto")
public class CryptoProperties {

    /**
     * 是否启用接口加密解密
     */
    private Boolean enabled = Boolean.TRUE;

    /**
     * 是否默认开启请求解密
     */
    private Boolean defaultRequestDecrypt = Boolean.FALSE;

    /**
     * 是否默认开启响应加密
     */
    private Boolean defaultResponseEncrypt = Boolean.FALSE;

    /**
     * 是否加密异常响应
     */
    private Boolean encryptErrorResponse = Boolean.TRUE;

    /**
     * 默认密钥标识
     */
    private String defaultKeyId;

    /**
     * 请求时间戳有效期，单位秒
     */
    private Long timestampExpireSeconds = 300L;

    /**
     * AES-GCM IV 长度
     */
    private Integer aesGcmIvLength = 12;

    /**
     * AES-GCM Tag 长度
     */
    private Integer aesGcmTagLength = 128;

    /**
     * 排除路径
     */
    private List<String> excludePaths = new ArrayList<>();

    /**
     * 多版本密钥配置
     */
    private Map<String, KeyConfig> keys = new LinkedHashMap<>();

    /**
     * 密钥配置
     *
     * @author Ateng
     * @since 2026-05-06
     */
    @Data
    public static class KeyConfig {

        /**
         * 公钥路径
         */
        private String publicKeyLocation;

        /**
         * 私钥路径
         */
        private String privateKeyLocation;

        /**
         * 签名密钥
         */
        private String signSecret;
    }
}
```

以下异常类用于统一抛出接口加解密异常，便于全局异常处理器识别。

文件位置：`src/main/java/io/github/atengk/crypto/exception/CryptoException.java`

```java
package io.github.atengk.crypto.exception;

/**
 * 接口加密解密异常
 *
 * @author Ateng
 * @since 2026-05-06
 */
public class CryptoException extends RuntimeException {

    public CryptoException(String message) {
        super(message);
    }

    public CryptoException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

以下组件用于读取 PEM 私钥，并根据 `keyId` 获取当前密钥配置。

文件位置：`src/main/java/io/github/atengk/crypto/core/CryptoKeyProvider.java`

```java
package io.github.atengk.crypto.core;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.crypto.config.CryptoProperties;
import io.github.atengk.crypto.exception.CryptoException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

/**
 * 接口加密密钥读取组件
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CryptoKeyProvider {

    private static final String PRIVATE_KEY_BEGIN = "-----BEGIN PRIVATE KEY-----";
    private static final String PRIVATE_KEY_END = "-----END PRIVATE KEY-----";

    private final CryptoProperties cryptoProperties;
    private final ResourceLoader resourceLoader;

    /**
     * 获取密钥配置
     *
     * @param keyId 密钥标识
     * @return 密钥配置
     */
    public CryptoProperties.KeyConfig getKeyConfig(String keyId) {
        String resolvedKeyId = StrUtil.blankToDefault(keyId, cryptoProperties.getDefaultKeyId());
        CryptoProperties.KeyConfig keyConfig = cryptoProperties.getKeys().get(resolvedKeyId);
        if (keyConfig == null) {
            throw new CryptoException("加密密钥不存在，keyId=" + resolvedKeyId);
        }
        return keyConfig;
    }

    /**
     * 读取 RSA 私钥
     *
     * @param keyId 密钥标识
     * @return RSA 私钥
     */
    public PrivateKey getPrivateKey(String keyId) {
        CryptoProperties.KeyConfig keyConfig = getKeyConfig(keyId);
        String location = keyConfig.getPrivateKeyLocation();
        if (StrUtil.isBlank(location)) {
            throw new CryptoException("私钥路径不能为空，keyId=" + keyId);
        }

        try {
            String pem = readText(location);
            String privateKeyText = pem
                    .replace(PRIVATE_KEY_BEGIN, StrUtil.EMPTY)
                    .replace(PRIVATE_KEY_END, StrUtil.EMPTY)
                    .replaceAll("\\s+", StrUtil.EMPTY);

            byte[] keyBytes = Base64.getDecoder().decode(privateKeyText);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePrivate(keySpec);
        } catch (Exception e) {
            log.error("读取 RSA 私钥失败，keyId={}，location={}", keyId, location, e);
            throw new CryptoException("读取 RSA 私钥失败", e);
        }
    }

    /**
     * 获取签名密钥
     *
     * @param keyId 密钥标识
     * @return 签名密钥
     */
    public String getSignSecret(String keyId) {
        CryptoProperties.KeyConfig keyConfig = getKeyConfig(keyId);
        if (StrUtil.isBlank(keyConfig.getSignSecret())) {
            throw new CryptoException("签名密钥不能为空，keyId=" + keyId);
        }
        return keyConfig.getSignSecret();
    }

    /**
     * 读取文本内容
     *
     * @param location 文件路径
     * @return 文本内容
     */
    private String readText(String location) throws Exception {
        if (StrUtil.startWith(location, "classpath:")) {
            Resource resource = resourceLoader.getResource(location);
            return IoUtil.read(resource.getInputStream(), StandardCharsets.UTF_8);
        }
        return FileUtil.readUtf8String(location);
    }
}
```

同时需要注册配置属性类。

文件位置：`src/main/java/io/github/atengk/crypto/config/CryptoAutoConfiguration.java`

```java
package io.github.atengk.crypto.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 接口加密自动配置
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Configuration
@EnableConfigurationProperties(CryptoProperties.class)
public class CryptoAutoConfiguration {
}
```

## 核心组件设计

本章节说明接口加密解密的核心组件职责和关键代码结构。整体设计围绕四类组件展开：加密解密工具类、请求体解密组件、响应体加密组件和加密开关控制组件。

推荐核心文件结构如下：

```text
src/main/java/io/github/atengk/crypto/annotation/CryptoIgnore.java
src/main/java/io/github/atengk/crypto/annotation/DecryptRequest.java
src/main/java/io/github/atengk/crypto/annotation/EncryptResponse.java
src/main/java/io/github/atengk/crypto/core/CryptoBody.java
src/main/java/io/github/atengk/crypto/core/CryptoContextHolder.java
src/main/java/io/github/atengk/crypto/core/CryptoUtils.java
src/main/java/io/github/atengk/crypto/core/CryptoSwitchSupport.java
src/main/java/io/github/atengk/crypto/advice/DecryptRequestBodyAdvice.java
src/main/java/io/github/atengk/crypto/advice/EncryptResponseBodyAdvice.java
```

### 加密解密工具类

加密解密工具类负责提供 AES-GCM 加密、AES-GCM 解密、RSA 私钥解密和 HMAC-SHA256 签名能力。业务组件不应直接拼装底层 JCA 代码，而应统一调用工具类，避免算法参数、编码格式和异常处理不一致。

以下 DTO 用于承载请求和响应中的密文结构。

文件位置：`src/main/java/io/github/atengk/crypto/core/CryptoBody.java`

```java
package io.github.atengk.crypto.core;

import lombok.Data;

/**
 * 接口加密密文载体
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
public class CryptoBody {

    /**
     * 加密协议版本
     */
    private String version;

    /**
     * 加密算法
     */
    private String algorithm;

    /**
     * 密钥标识
     */
    private String keyId;

    /**
     * RSA 加密后的 AES 密钥
     */
    private String encryptedKey;

    /**
     * AES-GCM IV
     */
    private String iv;

    /**
     * 请求或响应时间戳
     */
    private Long timestamp;

    /**
     * 随机字符串
     */
    private String nonce;

    /**
     * 签名
     */
    private String sign;

    /**
     * AES 加密后的业务数据
     */
    private String data;
}
```

以下上下文对象用于在一次请求内保存 AES 会话密钥，响应加密时可直接复用。

文件位置：`src/main/java/io/github/atengk/crypto/core/CryptoContextHolder.java`

```java
package io.github.atengk.crypto.core;

/**
 * 接口加密请求上下文
 *
 * @author Ateng
 * @since 2026-05-06
 */
public final class CryptoContextHolder {

    private static final ThreadLocal<byte[]> AES_KEY_HOLDER = new ThreadLocal<>();

    private CryptoContextHolder() {
    }

    /**
     * 保存 AES 密钥
     *
     * @param aesKey AES 密钥
     */
    public static void setAesKey(byte[] aesKey) {
        AES_KEY_HOLDER.set(aesKey);
    }

    /**
     * 获取 AES 密钥
     *
     * @return AES 密钥
     */
    public static byte[] getAesKey() {
        return AES_KEY_HOLDER.get();
    }

    /**
     * 清理上下文
     */
    public static void clear() {
        AES_KEY_HOLDER.remove();
    }
}
```

以下工具类提供核心加解密能力。AES-GCM 使用 `AES/GCM/NoPadding`，RSA 使用 `RSA/ECB/OAEPWithSHA-256AndMGF1Padding`。

文件位置：`src/main/java/io/github/atengk/crypto/core/CryptoUtils.java`

```java
package io.github.atengk.crypto.core;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.crypto.exception.CryptoException;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;

/**
 * 接口加密解密工具类
 *
 * @author Ateng
 * @since 2026-05-06
 */
public final class CryptoUtils {

    private static final String AES_ALGORITHM = "AES";
    private static final String AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String RSA_OAEP_TRANSFORMATION = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";
    private static final String HMAC_SHA256 = "HmacSHA256";

    private CryptoUtils() {
    }

    /**
     * AES-GCM 加密
     *
     * @param plainText 明文
     * @param aesKey AES 密钥
     * @param iv IV
     * @param tagLength Tag 长度
     * @return Base64 密文
     */
    public static String aesGcmEncryptToBase64(String plainText, byte[] aesKey, byte[] iv, int tagLength) {
        try {
            Cipher cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION);
            SecretKeySpec keySpec = new SecretKeySpec(aesKey, AES_ALGORITHM);
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(tagLength, iv);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmParameterSpec);
            byte[] encryptedBytes = cipher.doFinal(StrUtil.bytes(plainText, StandardCharsets.UTF_8));
            return Base64.encode(encryptedBytes);
        } catch (Exception e) {
            throw new CryptoException("AES-GCM 加密失败", e);
        }
    }

    /**
     * AES-GCM 解密
     *
     * @param cipherTextBase64 Base64 密文
     * @param aesKey AES 密钥
     * @param ivBase64 Base64 IV
     * @param tagLength Tag 长度
     * @return 明文
     */
    public static String aesGcmDecryptFromBase64(String cipherTextBase64, byte[] aesKey, String ivBase64, int tagLength) {
        try {
            byte[] iv = Base64.decode(ivBase64);
            byte[] cipherBytes = Base64.decode(cipherTextBase64);

            Cipher cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION);
            SecretKeySpec keySpec = new SecretKeySpec(aesKey, AES_ALGORITHM);
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(tagLength, iv);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmParameterSpec);

            byte[] plainBytes = cipher.doFinal(cipherBytes);
            return StrUtil.str(plainBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new CryptoException("AES-GCM 解密失败", e);
        }
    }

    /**
     * RSA 私钥解密 AES 密钥
     *
     * @param encryptedKeyBase64 Base64 RSA 密文
     * @param privateKey RSA 私钥
     * @return AES 密钥
     */
    public static byte[] rsaDecryptAesKey(String encryptedKeyBase64, PrivateKey privateKey) {
        try {
            Cipher cipher = Cipher.getInstance(RSA_OAEP_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            return cipher.doFinal(Base64.decode(encryptedKeyBase64));
        } catch (Exception e) {
            throw new CryptoException("RSA 解密 AES 密钥失败", e);
        }
    }

    /**
     * HMAC-SHA256 签名
     *
     * @param text 签名原文
     * @param secret 签名密钥
     * @return Base64 签名
     */
    public static String hmacSha256Base64(String text, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKeySpec = new SecretKeySpec(StrUtil.bytes(secret, StandardCharsets.UTF_8), HMAC_SHA256);
            mac.init(secretKeySpec);
            byte[] digest = mac.doFinal(StrUtil.bytes(text, StandardCharsets.UTF_8));
            return Base64.encode(digest);
        } catch (Exception e) {
            throw new CryptoException("HMAC-SHA256 签名失败", e);
        }
    }
}
```

### 请求体解密组件

请求体解密组件建议基于 `RequestBodyAdvice` 实现。该组件在 Spring MVC 将请求体反序列化为 DTO 之前执行，可以先读取密文请求体，完成 AES 密钥解密和业务数据解密，再将明文 JSON 交还给 Spring MVC。

请求体解密的核心职责包括：

| 职责                 | 说明                                                    |
| -------------------- | ------------------------------------------------------- |
| 判断接口是否需要解密 | 根据全局开关、排除路径、类注解和方法注解决定            |
| 读取密文请求体       | 将请求体反序列化为 `CryptoBody`                         |
| 解密 AES 密钥        | 使用服务端 RSA 私钥解密 `encryptedKey`                  |
| 解密业务数据         | 使用 AES-GCM 解密 `data` 字段                           |
| 保存请求上下文       | 将 AES Key 保存到 `CryptoContextHolder`，供响应加密复用 |
| 替换请求输入流       | 将明文 JSON 包装为新的 `HttpInputMessage`               |

以下代码实现请求体解密组件。

文件位置：`src/main/java/io/github/atengk/crypto/advice/DecryptRequestBodyAdvice.java`

```java
package io.github.atengk.crypto.advice;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.atengk.crypto.config.CryptoProperties;
import io.github.atengk.crypto.core.CryptoBody;
import io.github.atengk.crypto.core.CryptoContextHolder;
import io.github.atengk.crypto.core.CryptoKeyProvider;
import io.github.atengk.crypto.core.CryptoSwitchSupport;
import io.github.atengk.crypto.core.CryptoUtils;
import io.github.atengk.crypto.exception.CryptoException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;

/**
 * 请求体解密处理组件
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@ControllerAdvice(annotations = Controller.class)
@RequiredArgsConstructor
public class DecryptRequestBodyAdvice extends RequestBodyAdviceAdapter {

    private final ObjectMapper objectMapper;
    private final CryptoProperties cryptoProperties;
    private final CryptoKeyProvider cryptoKeyProvider;
    private final CryptoSwitchSupport cryptoSwitchSupport;

    /**
     * 判断是否支持请求体解密
     *
     * @param methodParameter 方法参数
     * @param targetType 目标类型
     * @param converterType 消息转换器类型
     * @return 是否支持
     */
    @Override
    public boolean supports(MethodParameter methodParameter, Type targetType, Class converterType) {
        return cryptoSwitchSupport.needDecrypt(methodParameter);
    }

    /**
     * 在请求体读取前完成密文解密
     *
     * @param inputMessage 原始请求体
     * @param parameter 方法参数
     * @param targetType 目标类型
     * @param converterType 消息转换器类型
     * @return 明文请求体
     */
    @Override
    public HttpInputMessage beforeBodyRead(HttpInputMessage inputMessage,
                                           MethodParameter parameter,
                                           Type targetType,
                                           Class converterType) {
        try {
            String body = StrUtil.str(inputMessage.getBody().readAllBytes(), StandardCharsets.UTF_8);
            if (StrUtil.isBlank(body)) {
                throw new CryptoException("加密请求体不能为空");
            }

            CryptoBody cryptoBody = objectMapper.readValue(body, CryptoBody.class);
            validateCryptoBody(cryptoBody);

            PrivateKey privateKey = cryptoKeyProvider.getPrivateKey(cryptoBody.getKeyId());
            byte[] aesKey = CryptoUtils.rsaDecryptAesKey(cryptoBody.getEncryptedKey(), privateKey);
            String plainJson = CryptoUtils.aesGcmDecryptFromBase64(
                    cryptoBody.getData(),
                    aesKey,
                    cryptoBody.getIv(),
                    cryptoProperties.getAesGcmTagLength()
            );

            CryptoContextHolder.setAesKey(aesKey);
            log.debug("请求体解密完成，keyId={}，targetType={}", cryptoBody.getKeyId(), targetType.getTypeName());

            return new DecryptedHttpInputMessage(inputMessage.getHeaders(), plainJson);
        } catch (CryptoException e) {
            throw e;
        } catch (Exception e) {
            log.error("请求体解密处理失败", e);
            throw new CryptoException("请求体解密处理失败", e);
        }
    }

    /**
     * 校验密文请求体
     *
     * @param cryptoBody 密文请求体
     */
    private void validateCryptoBody(CryptoBody cryptoBody) {
        if (cryptoBody == null) {
            throw new CryptoException("加密请求体格式错误");
        }
        if (StrUtil.hasBlank(cryptoBody.getEncryptedKey(), cryptoBody.getIv(), cryptoBody.getData())) {
            throw new CryptoException("加密请求体缺少必要字段");
        }
    }

    /**
     * 解密后的请求输入消息
     *
     * @author Ateng
     * @since 2026-05-06
     */
    private static class DecryptedHttpInputMessage implements HttpInputMessage {

        private final HttpHeaders headers;
        private final String plainJson;

        private DecryptedHttpInputMessage(HttpHeaders headers, String plainJson) {
            this.headers = headers;
            this.plainJson = plainJson;
        }

        /**
         * 获取明文请求体输入流
         *
         * @return 输入流
         */
        @Override
        public InputStream getBody() {
            return new ByteArrayInputStream(plainJson.getBytes(StandardCharsets.UTF_8));
        }

        /**
         * 获取请求头
         *
         * @return 请求头
         */
        @Override
        public HttpHeaders getHeaders() {
            return headers;
        }
    }
}
```

该组件只负责“把密文请求体转换为明文请求体”。时间戳、随机字符串、防重放和签名校验建议放到 `HandlerInterceptor` 中处理，避免请求体解密组件职责过重。

### 响应体加密组件

响应体加密组件建议基于 `ResponseBodyAdvice` 实现。该组件在 Controller 返回对象之后、HTTP 响应写出之前执行，可以将普通业务对象序列化为 JSON，再使用 AES-GCM 加密后包装为统一密文响应结构。

响应体加密的核心职责包括：

| 职责                 | 说明                                         |
| -------------------- | -------------------------------------------- |
| 判断接口是否需要加密 | 根据全局开关、排除路径、类注解和方法注解决定 |
| 跳过特殊响应类型     | 文件、流、字节数组等响应不走通用加密         |
| 序列化业务对象       | 将 Controller 返回对象转换为 JSON            |
| 加密响应数据         | 使用 AES-GCM 加密响应 JSON                   |
| 组装密文响应         | 返回 `CryptoBody` 结构                       |
| 清理请求上下文       | 响应处理结束后清理 ThreadLocal               |

以下代码实现响应体加密组件。

文件位置：`src/main/java/io/github/atengk/crypto/advice/EncryptResponseBodyAdvice.java`

```java
package io.github.atengk.crypto.advice;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.atengk.crypto.config.CryptoProperties;
import io.github.atengk.crypto.core.CryptoBody;
import io.github.atengk.crypto.core.CryptoContextHolder;
import io.github.atengk.crypto.core.CryptoSwitchSupport;
import io.github.atengk.crypto.core.CryptoUtils;
import io.github.atengk.crypto.exception.CryptoException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.security.SecureRandom;

/**
 * 响应体加密处理组件
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@ControllerAdvice(annotations = Controller.class)
@RequiredArgsConstructor
public class EncryptResponseBodyAdvice implements ResponseBodyAdvice<Object> {

    private final ObjectMapper objectMapper;
    private final CryptoProperties cryptoProperties;
    private final CryptoSwitchSupport cryptoSwitchSupport;
    private final HttpServletRequest request;

    /**
     * 判断是否支持响应体加密
     *
     * @param returnType 返回类型
     * @param converterType 消息转换器类型
     * @return 是否支持
     */
    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return cryptoSwitchSupport.needEncrypt(returnType);
    }

    /**
     * 在响应写出前完成加密
     *
     * @param body 原始响应体
     * @param returnType 返回类型
     * @param selectedContentType 响应类型
     * @param selectedConverterType 消息转换器类型
     * @param serverHttpRequest 请求
     * @param serverHttpResponse 响应
     * @return 加密后的响应体
     */
    @Override
    public Object beforeBodyWrite(Object body,
                                  MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  org.springframework.http.server.ServerHttpRequest serverHttpRequest,
                                  org.springframework.http.server.ServerHttpResponse serverHttpResponse) {
        try {
            if (shouldSkipBody(body)) {
                return body;
            }

            byte[] aesKey = CryptoContextHolder.getAesKey();
            if (aesKey == null || aesKey.length == 0) {
                log.warn("响应加密缺少请求级 AES 密钥，uri={}", request.getRequestURI());
                throw new CryptoException("响应加密缺少 AES 密钥");
            }

            byte[] iv = new byte[cryptoProperties.getAesGcmIvLength()];
            new SecureRandom().nextBytes(iv);

            String plainJson = objectMapper.writeValueAsString(body);
            String encryptedData = CryptoUtils.aesGcmEncryptToBase64(
                    plainJson,
                    aesKey,
                    iv,
                    cryptoProperties.getAesGcmTagLength()
            );

            CryptoBody cryptoBody = new CryptoBody();
            cryptoBody.setVersion("v1");
            cryptoBody.setAlgorithm("AES-256-GCM");
            cryptoBody.setKeyId(cryptoProperties.getDefaultKeyId());
            cryptoBody.setIv(Base64.encode(iv));
            cryptoBody.setTimestamp(System.currentTimeMillis());
            cryptoBody.setNonce(RandomUtil.randomString(16));
            cryptoBody.setData(encryptedData);

            log.debug("响应体加密完成，uri={}", request.getRequestURI());

            if (body instanceof String) {
                return objectMapper.writeValueAsString(cryptoBody);
            }
            return cryptoBody;
        } catch (CryptoException e) {
            throw e;
        } catch (Exception e) {
            log.error("响应体加密失败，uri={}", request.getRequestURI(), e);
            throw new CryptoException("响应体加密失败", e);
        } finally {
            CryptoContextHolder.clear();
        }
    }

    /**
     * 判断是否跳过响应加密
     *
     * @param body 响应体
     * @return 是否跳过
     */
    private boolean shouldSkipBody(Object body) {
        if (body == null) {
            return true;
        }
        if (body instanceof CryptoBody) {
            return true;
        }
        if (body instanceof byte[]) {
            return true;
        }
        if (body instanceof Resource) {
            return true;
        }
        if (body instanceof StreamingResponseBody) {
            return true;
        }
        return StrUtil.equalsAnyIgnoreCase(body.getClass().getSimpleName(), "ResponseEntity");
    }
}
```

响应加密默认复用请求解密阶段得到的 AES 会话密钥。因此，如果某个接口只开启响应加密、没有开启请求解密，需要额外设计“客户端公钥上传”或“服务端响应密钥加密返回”机制。推荐第一阶段先约束为“请求解密与响应加密成对使用”。

### 加密开关控制

加密开关控制用于统一判断某个接口是否需要请求解密或响应加密。推荐同时支持全局开关、排除路径、类级别注解、方法级别注解。方法级别优先级高于类级别，显式忽略注解优先级最高。

建议控制优先级如下：

```text
全局 enabled=false
  -> 直接不加解密

路径命中 exclude-paths
  -> 直接不加解密

方法或类存在 @CryptoIgnore
  -> 直接不加解密

方法存在 @DecryptRequest / @EncryptResponse
  -> 按方法注解决定

类存在 @DecryptRequest / @EncryptResponse
  -> 按类注解决定

没有注解
  -> 使用 default-request-decrypt / default-response-encrypt
```

以下是基础注解定义。详细注解设计可在后续 `注解设计` 章节进一步展开。

文件位置：`src/main/java/io/github/atengk/crypto/annotation/DecryptRequest.java`

```java
package io.github.atengk.crypto.annotation;

import java.lang.annotation.*;

/**
 * 开启请求体解密
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DecryptRequest {
}
```

文件位置：`src/main/java/io/github/atengk/crypto/annotation/EncryptResponse.java`

```java
package io.github.atengk.crypto.annotation;

import java.lang.annotation.*;

/**
 * 开启响应体加密
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EncryptResponse {
}
```

文件位置：`src/main/java/io/github/atengk/crypto/annotation/CryptoIgnore.java`

```java
package io.github.atengk.crypto.annotation;

import java.lang.annotation.*;

/**
 * 忽略接口加密解密
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CryptoIgnore {
}
```

以下组件用于统一判断接口是否需要加密或解密。

文件位置：`src/main/java/io/github/atengk/crypto/core/CryptoSwitchSupport.java`

```java
package io.github.atengk.crypto.core;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.crypto.annotation.CryptoIgnore;
import io.github.atengk.crypto.annotation.DecryptRequest;
import io.github.atengk.crypto.annotation.EncryptResponse;
import io.github.atengk.crypto.config.CryptoProperties;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

/**
 * 接口加密开关判断组件
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Component
@RequiredArgsConstructor
public class CryptoSwitchSupport {

    private final CryptoProperties cryptoProperties;

    private final AntPathMatcher antPathMatcher = new AntPathMatcher();

    /**
     * 判断是否需要请求解密
     *
     * @param methodParameter 方法参数
     * @return 是否需要解密
     */
    public boolean needDecrypt(MethodParameter methodParameter) {
        if (isDisabledOrExcluded()) {
            return false;
        }
        if (hasIgnore(methodParameter)) {
            return false;
        }
        if (hasMethodAnnotation(methodParameter, DecryptRequest.class)) {
            return true;
        }
        if (hasClassAnnotation(methodParameter, DecryptRequest.class)) {
            return true;
        }
        return Boolean.TRUE.equals(cryptoProperties.getDefaultRequestDecrypt());
    }

    /**
     * 判断是否需要响应加密
     *
     * @param methodParameter 方法参数
     * @return 是否需要加密
     */
    public boolean needEncrypt(MethodParameter methodParameter) {
        if (isDisabledOrExcluded()) {
            return false;
        }
        if (hasIgnore(methodParameter)) {
            return false;
        }
        if (hasMethodAnnotation(methodParameter, EncryptResponse.class)) {
            return true;
        }
        if (hasClassAnnotation(methodParameter, EncryptResponse.class)) {
            return true;
        }
        return Boolean.TRUE.equals(cryptoProperties.getDefaultResponseEncrypt());
    }

    /**
     * 判断全局禁用或路径排除
     *
     * @return 是否禁用或排除
     */
    private boolean isDisabledOrExcluded() {
        if (!Boolean.TRUE.equals(cryptoProperties.getEnabled())) {
            return true;
        }

        HttpServletRequest request = getCurrentRequest();
        if (request == null) {
            return false;
        }

        String uri = request.getRequestURI();
        if (StrUtil.isBlank(uri) || CollUtil.isEmpty(cryptoProperties.getExcludePaths())) {
            return false;
        }

        return cryptoProperties.getExcludePaths()
                .stream()
                .anyMatch(pattern -> antPathMatcher.match(pattern, uri));
    }

    /**
     * 判断是否存在忽略注解
     *
     * @param methodParameter 方法参数
     * @return 是否忽略
     */
    private boolean hasIgnore(MethodParameter methodParameter) {
        return hasMethodAnnotation(methodParameter, CryptoIgnore.class)
                || hasClassAnnotation(methodParameter, CryptoIgnore.class);
    }

    /**
     * 判断方法是否存在指定注解
     *
     * @param methodParameter 方法参数
     * @param annotationClass 注解类型
     * @return 是否存在
     */
    private boolean hasMethodAnnotation(MethodParameter methodParameter, Class annotationClass) {
        Method method = methodParameter.getMethod();
        return method != null && method.isAnnotationPresent(annotationClass);
    }

    /**
     * 判断类是否存在指定注解
     *
     * @param methodParameter 方法参数
     * @param annotationClass 注解类型
     * @return 是否存在
     */
    private boolean hasClassAnnotation(MethodParameter methodParameter, Class annotationClass) {
        Class<?> containingClass = methodParameter.getContainingClass();
        return containingClass.isAnnotationPresent(annotationClass);
    }

    /**
     * 获取当前请求
     *
     * @return 当前请求
     */
    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes == null ? null : attributes.getRequest();
    }
}
```

为了让 `AntPathMatcher` 作为 Bean 管理，也可以在配置类中补充：

文件位置：`src/main/java/io/github/atengk/crypto/config/CryptoWebConfiguration.java`

```java
package io.github.atengk.crypto.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.AntPathMatcher;

/**
 * 接口加密 Web 配置
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Configuration
public class CryptoWebConfiguration {

    /**
     * 路径匹配器
     *
     * @return Ant 风格路径匹配器
     */
    @Bean
    public AntPathMatcher antPathMatcher() {
        return new AntPathMatcher();
    }
}
```

当前章节完成后，接口加密解密能力已经具备基础配置、密钥读取、请求体解密、响应体加密和开关判断的核心骨架。后续章节可以继续补充接口处理流程、`HandlerInterceptor` 签名校验、防重放校验、全局异常处理和接口示例。



## 接口处理流程

本章节说明客户端、服务端在一次完整接口调用中的加密、验签、解密、响应加密和客户端响应解密规则。接口处理流程应保持前后端一致，否则会出现签名不一致、密钥无法解密、响应无法解析等问题。

### 客户端请求加密规则

客户端请求加密规则用于约束前端、App、小程序或第三方调用方如何组装加密请求。所有需要加密的接口必须按照统一协议生成请求头、请求体和签名。

推荐请求头如下：

```http
POST /api/user/login HTTP/1.1
Content-Type: application/json
X-Crypto-Version: v1
X-Crypto-Key-Id: server-key-202605
X-Crypto-Timestamp: 1777996800000
X-Crypto-Nonce: 3f7f2c2d9d874cbf
X-Crypto-Sign: Base64(HMAC-SHA256签名)
```

推荐请求体如下：

```json
{
  "encryptedKey": "Base64(RSA-OAEP加密后的AES密钥)",
  "iv": "Base64(AES-GCM初始化向量)",
  "data": "Base64(AES-GCM加密后的业务JSON)"
}
```

客户端加密步骤如下：

| 步骤 | 说明                                                 |
| ---- | ---------------------------------------------------- |
| 1    | 将业务参数序列化为 JSON 字符串                       |
| 2    | 生成一次性 AES 会话密钥，推荐 256 bit                |
| 3    | 生成 AES-GCM IV，推荐 12 字节，每次请求必须不同      |
| 4    | 使用 AES-GCM 加密业务 JSON，得到 `data`              |
| 5    | 使用服务端公钥加密 AES 会话密钥，得到 `encryptedKey` |
| 6    | 生成毫秒时间戳 `timestamp`                           |
| 7    | 生成随机字符串 `nonce`，建议长度不少于 16 位         |
| 8    | 按服务端约定拼接签名原文                             |
| 9    | 使用签名密钥生成 `X-Crypto-Sign`                     |
| 10   | 发送请求头和密文请求体                               |

推荐签名原文规则如下：

```text
HTTP_METHOD + "\n" +
REQUEST_PATH + "\n" +
TIMESTAMP + "\n" +
NONCE + "\n" +
KEY_ID + "\n" +
ENCRYPTED_KEY + "\n" +
IV + "\n" +
DATA
```

示例签名原文：

```text
POST
/api/user/login
1777996800000
3f7f2c2d9d874cbf
server-key-202605
Base64(RSA-OAEP加密后的AES密钥)
Base64(AES-GCM初始化向量)
Base64(AES-GCM加密后的业务JSON)
```

客户端需要注意以下规则：

| 规则                        | 说明                                        |
| --------------------------- | ------------------------------------------- |
| AES Key 不得长期复用        | 建议每次请求重新生成                        |
| IV 不得重复                 | AES-GCM 下同一 Key 不应复用 IV              |
| 时间戳使用毫秒值            | 服务端按毫秒时间戳进行有效期校验            |
| `nonce` 全局尽量唯一        | 服务端会基于 `nonce` 做防重放处理           |
| 签名前后字段必须一致        | 参与签名的字段发送后不得再被修改            |
| 不要对密文字段二次 URL 编码 | 否则服务端验签可能失败                      |
| 请求体字段名固定            | `encryptedKey`、`iv`、`data` 不建议随意改名 |

### 服务端请求解密流程

服务端请求解密流程用于描述请求进入 Spring Boot 3 应用后，在进入 Controller 之前如何完成缓存请求体、参数校验、签名校验、防重放和请求体解密。

推荐服务端处理顺序如下：

```text
客户端密文请求
  -> CachedBodyFilter 缓存请求体
  -> HandlerInterceptor 校验请求头、时间戳、nonce、签名
  -> RequestBodyAdvice 解密请求体
  -> Spring MVC 反序列化明文 JSON
  -> Controller 接收普通 DTO
```

各组件职责如下：

| 组件                     | 执行时机         | 职责                                                       |
| ------------------------ | ---------------- | ---------------------------------------------------------- |
| `CachedBodyFilter`       | 最先执行         | 缓存请求体，避免拦截器读取 Body 后 Controller 无法再次读取 |
| `HandlerInterceptor`     | Controller 前    | 校验加密请求头、时间戳、随机字符串、签名和防重放           |
| `RequestBodyAdvice`      | 请求体反序列化前 | 解密 AES Key，解密业务数据，替换为明文 JSON                |
| `Controller`             | 业务处理         | 接收普通 DTO，不感知密文结构                               |
| `GlobalExceptionHandler` | 异常处理         | 统一返回加解密异常、签名异常和参数异常                     |

服务端请求解密需要遵循以下校验顺序：

| 顺序 | 校验项               | 说明                                                         |
| ---- | -------------------- | ------------------------------------------------------------ |
| 1    | 判断接口是否需要解密 | 全局开关、排除路径、注解开关共同决定                         |
| 2    | 校验请求头是否完整   | 缺少 `version`、`keyId`、`timestamp`、`nonce`、`sign` 时直接拒绝 |
| 3    | 校验请求体是否完整   | 缺少 `encryptedKey`、`iv`、`data` 时直接拒绝                 |
| 4    | 校验时间戳           | 超过允许偏差时拒绝，防止历史请求长期有效                     |
| 5    | 校验 `nonce`         | 已使用过的 `nonce` 直接拒绝，防止重放攻击                    |
| 6    | 校验签名             | 使用服务端签名密钥重新计算签名并比较                         |
| 7    | 解密 AES Key         | 使用服务端私钥解密 `encryptedKey`                            |
| 8    | 解密业务数据         | 使用 AES-GCM 解密 `data`                                     |
| 9    | 替换请求体           | 将明文 JSON 交给 Spring MVC 继续处理                         |

请求校验失败时，建议直接抛出业务异常，不再进入 Controller。

### 服务端响应加密流程

服务端响应加密流程用于描述 Controller 返回普通对象后，如何在响应写出前加密为统一密文结构。

推荐响应处理顺序如下：

```text
Controller 返回普通对象
  -> ResponseBodyAdvice 判断是否需要加密
  -> ObjectMapper 序列化响应对象
  -> 获取请求上下文中的 AES Key
  -> 生成新的响应 IV
  -> AES-GCM 加密响应 JSON
  -> 包装 CryptoBody
  -> 客户端接收密文响应
```

推荐响应体如下：

```json
{
  "version": "v1",
  "algorithm": "AES-256-GCM",
  "keyId": "server-key-202605",
  "iv": "Base64(响应IV)",
  "timestamp": 1777996800000,
  "nonce": "e75c2a8d9f014a3b",
  "data": "Base64(AES-GCM加密后的响应JSON)"
}
```

响应加密建议遵循以下规则：

| 规则                   | 说明                                                         |
| ---------------------- | ------------------------------------------------------------ |
| 默认复用请求 AES Key   | 请求解密阶段保存 AES Key，响应加密阶段直接复用               |
| 响应 IV 每次重新生成   | 不复用请求 IV                                                |
| 响应 Body 统一包装     | 避免客户端需要兼容多种密文格式                               |
| 文件和流式响应跳过加密 | 避免大文件读入内存导致性能和稳定性问题                       |
| 异常响应按配置处理     | 解密前失败通常无法复用 AES Key，只能返回明文错误或采用额外公钥加密策略 |
| 响应完成后清理上下文   | 必须清理 `ThreadLocal`，避免线程复用造成数据串扰             |

不建议对以下响应做统一加密：

| 响应类型     | 说明                              |
| ------------ | --------------------------------- |
| 文件下载     | 例如 Excel、PDF、图片、压缩包     |
| 流式响应     | 例如 `StreamingResponseBody`、SSE |
| 字节数组     | 可能是图片、文件或二进制协议      |
| 接口文档     | Swagger、OpenAPI 文档需要明文     |
| 健康检查     | 监控系统需要直接识别状态          |
| 公钥获取接口 | 客户端需要明文获取服务端公钥      |

### 客户端响应解密规则

客户端响应解密规则用于约束调用方如何处理服务端返回的密文响应。客户端需要先判断接口是否约定加密响应，再按统一结构读取 `iv` 和 `data`。

客户端响应解密步骤如下：

| 步骤 | 说明                                       |
| ---- | ------------------------------------------ |
| 1    | 判断 HTTP 状态码是否成功                   |
| 2    | 判断响应体是否为密文结构                   |
| 3    | 读取响应中的 `iv` 和 `data`                |
| 4    | 使用请求阶段生成的 AES 会话密钥解密 `data` |
| 5    | 将明文 JSON 反序列化为业务响应对象         |
| 6    | 进入正常业务处理                           |

客户端需要注意：

| 规则                       | 说明                                                         |
| -------------------------- | ------------------------------------------------------------ |
| 响应解密使用请求 AES Key   | 如果服务端约定复用请求 AES Key，客户端必须保存本次请求生成的 AES Key |
| 响应 IV 来自服务端         | 不得复用请求 IV 解密响应                                     |
| 解密失败应进入统一异常处理 | 不应继续按普通 JSON 解析                                     |
| 错误响应可能是明文         | 当请求在解密前失败时，服务端可能无法返回加密错误             |
| 加密接口和明文接口要区分   | 客户端请求封装层应根据接口配置决定是否解密                   |

## Spring Boot 3 集成实现

本章节说明接口加密解密能力在 Spring Boot 3 中的具体集成方式。核心实现基于 `OncePerRequestFilter`、`HandlerInterceptor`、`RequestBodyAdvice`、`ResponseBodyAdvice` 和 `@RestControllerAdvice`。

推荐文件结构如下：

```text
src/main/java/io/github/atengk/crypto/constant/CryptoHeaderConstants.java
src/main/java/io/github/atengk/crypto/core/CryptoSignUtils.java
src/main/java/io/github/atengk/crypto/web/CachedBodyHttpServletRequest.java
src/main/java/io/github/atengk/crypto/web/CachedBodyFilter.java
src/main/java/io/github/atengk/crypto/interceptor/CryptoValidateInterceptor.java
src/main/java/io/github/atengk/crypto/config/CryptoMvcConfiguration.java
src/main/java/io/github/atengk/crypto/model/ApiResult.java
src/main/java/io/github/atengk/crypto/advice/GlobalExceptionHandler.java
```

### RequestBodyAdvice 请求解密

`RequestBodyAdvice` 用于在 Spring MVC 读取 `@RequestBody` 参数之前处理请求体。对于加密接口，该组件读取密文请求体，解析 `CryptoBody`，使用服务端私钥解密 AES Key，再使用 AES-GCM 解密业务 JSON，最后将明文 JSON 交给 Spring MVC 反序列化。

该组件应只负责请求体解密，不建议把时间戳、随机字符串、签名和防重放全部放在这里处理。参数校验应优先放在 `HandlerInterceptor` 中，这样职责更清晰，也便于在解密前拒绝非法请求。

关键处理逻辑如下：

```text
supports()
  -> 判断当前接口是否需要请求解密

beforeBodyRead()
  -> 读取缓存后的密文请求体
  -> 反序列化为 CryptoBody
  -> 根据 keyId 获取服务端私钥
  -> RSA 解密 encryptedKey
  -> AES-GCM 解密 data
  -> 保存 AES Key 到 CryptoContextHolder
  -> 返回新的明文 HttpInputMessage
```

请求体解密需要依赖前置请求体缓存。如果没有缓存请求体，拦截器读取 Body 做签名校验后，`RequestBodyAdvice` 将无法再次读取请求体。因此必须先注册 `CachedBodyFilter`。

以下请求体包装类用于支持请求体重复读取。

文件位置：`src/main/java/io/github/atengk/crypto/web/CachedBodyHttpServletRequest.java`

```java
package io.github.atengk.crypto.web;

import cn.hutool.core.util.StrUtil;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.springframework.util.StreamUtils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * 可重复读取请求体的 HttpServletRequest 包装类
 *
 * @author Ateng
 * @since 2026-05-06
 */
public class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {

    private final byte[] cachedBody;

    public CachedBodyHttpServletRequest(HttpServletRequest request) throws IOException {
        super(request);
        this.cachedBody = StreamUtils.copyToByteArray(request.getInputStream());
    }

    /**
     * 获取缓存后的请求体字节数组
     *
     * @return 请求体字节数组
     */
    public byte[] getCachedBody() {
        return cachedBody;
    }

    /**
     * 获取缓存后的请求体字符串
     *
     * @return 请求体字符串
     */
    public String getCachedBodyAsString() {
        return StrUtil.str(cachedBody, StandardCharsets.UTF_8);
    }

    /**
     * 获取请求体输入流
     *
     * @return 可重复读取的输入流
     */
    @Override
    public ServletInputStream getInputStream() {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(cachedBody);
        return new ServletInputStream() {

            /**
             * 判断是否读取完成
             *
             * @return 是否读取完成
             */
            @Override
            public boolean isFinished() {
                return byteArrayInputStream.available() == 0;
            }

            /**
             * 判断是否可读取
             *
             * @return 是否可读取
             */
            @Override
            public boolean isReady() {
                return true;
            }

            /**
             * 设置读取监听器
             *
             * @param readListener 读取监听器
             */
            @Override
            public void setReadListener(ReadListener readListener) {
                // 同步读取场景无需处理
            }

            /**
             * 读取字节
             *
             * @return 字节数据
             */
            @Override
            public int read() {
                return byteArrayInputStream.read();
            }
        };
    }

    /**
     * 获取请求体字符流
     *
     * @return 字符流
     */
    @Override
    public BufferedReader getReader() {
        return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
    }
}
```

以下过滤器用于在拦截器执行前缓存 JSON 请求体。

文件位置：`src/main/java/io/github/atengk/crypto/web/CachedBodyFilter.java`

```java
package io.github.atengk.crypto.web;

import cn.hutool.core.util.StrUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 请求体缓存过滤器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CachedBodyFilter extends OncePerRequestFilter {

    /**
     * 缓存 JSON 请求体
     *
     * @param request 请求对象
     * @param response 响应对象
     * @param filterChain 过滤器链
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (shouldWrap(request)) {
            filterChain.doFilter(new CachedBodyHttpServletRequest(request), response);
            return;
        }
        filterChain.doFilter(request, response);
    }

    /**
     * 判断是否需要包装请求体
     *
     * @param request 请求对象
     * @return 是否需要包装
     */
    private boolean shouldWrap(HttpServletRequest request) {
        String method = request.getMethod();
        String contentType = request.getContentType();
        return StrUtil.equalsAnyIgnoreCase(method, "POST", "PUT", "PATCH")
                && StrUtil.containsIgnoreCase(contentType, MediaType.APPLICATION_JSON_VALUE);
    }
}
```

### ResponseBodyAdvice 响应加密

`ResponseBodyAdvice` 用于在响应写出前处理 Controller 返回值。对于需要响应加密的接口，该组件将业务响应对象序列化为 JSON，使用请求上下文中的 AES Key 加密，然后包装为统一 `CryptoBody` 返回。

关键处理逻辑如下：

```text
supports()
  -> 判断当前接口是否需要响应加密

beforeBodyWrite()
  -> 跳过文件、流、字节数组等特殊响应
  -> 从 CryptoContextHolder 获取 AES Key
  -> 生成新的响应 IV
  -> 序列化原始响应对象
  -> AES-GCM 加密响应 JSON
  -> 组装 CryptoBody
  -> 清理 CryptoContextHolder
```

响应加密实现需要注意：

| 注意事项                      | 说明                                                         |
| ----------------------------- | ------------------------------------------------------------ |
| `String` 返回值要特殊处理     | `StringHttpMessageConverter` 要求返回字符串，因此需要手动序列化密文对象 |
| `ResponseEntity` 建议谨慎处理 | 可能包含文件、状态码、Header 等复杂语义                      |
| 异常响应不一定能加密          | 如果请求未解密成功，则没有 AES Key 可用于响应加密            |
| 必须清理 `ThreadLocal`        | 可放在响应加密完成后或请求完成拦截器中兜底清理               |
| 不要加密 `CryptoBody`         | 避免重复加密                                                 |

推荐策略是：业务接口返回普通对象，响应加密组件统一包装密文结构。业务 Controller 不直接返回 `CryptoBody`。

### HandlerInterceptor 参数校验

`HandlerInterceptor` 用于在请求进入 Controller 前完成加密参数校验。它适合处理请求头完整性、时间戳有效期、随机字符串防重放和签名校验。

由于签名通常需要请求体中的 `encryptedKey`、`iv`、`data` 参与计算，因此拦截器必须读取缓存后的请求体，而不是直接读取原始 `request.getInputStream()`。

以下常量类统一管理请求头名称。

文件位置：`src/main/java/io/github/atengk/crypto/constant/CryptoHeaderConstants.java`

```java
package io.github.atengk.crypto.constant;

/**
 * 接口加密请求头常量
 *
 * @author Ateng
 * @since 2026-05-06
 */
public final class CryptoHeaderConstants {

    public static final String VERSION = "X-Crypto-Version";
    public static final String KEY_ID = "X-Crypto-Key-Id";
    public static final String TIMESTAMP = "X-Crypto-Timestamp";
    public static final String NONCE = "X-Crypto-Nonce";
    public static final String SIGN = "X-Crypto-Sign";

    private CryptoHeaderConstants() {
    }
}
```

以下工具类用于生成签名原文和进行安全字符串比较。

文件位置：`src/main/java/io/github/atengk/crypto/core/CryptoSignUtils.java`

```java
package io.github.atengk.crypto.core;

import cn.hutool.core.util.StrUtil;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * 接口加密签名工具类
 *
 * @author Ateng
 * @since 2026-05-06
 */
public final class CryptoSignUtils {

    private CryptoSignUtils() {
    }

    /**
     * 构造请求签名原文
     *
     * @param method 请求方法
     * @param path 请求路径
     * @param timestamp 时间戳
     * @param nonce 随机字符串
     * @param keyId 密钥标识
     * @param encryptedKey 加密后的 AES 密钥
     * @param iv AES-GCM IV
     * @param data 加密后的业务数据
     * @return 签名原文
     */
    public static String buildRequestSignText(String method,
                                              String path,
                                              String timestamp,
                                              String nonce,
                                              String keyId,
                                              String encryptedKey,
                                              String iv,
                                              String data) {
        return StrUtil.join("\n", method, path, timestamp, nonce, keyId, encryptedKey, iv, data);
    }

    /**
     * 安全比较字符串
     *
     * @param source 原字符串
     * @param target 目标字符串
     * @return 是否相等
     */
    public static boolean secureEquals(String source, String target) {
        if (source == null || target == null) {
            return false;
        }
        byte[] sourceBytes = source.getBytes(StandardCharsets.UTF_8);
        byte[] targetBytes = target.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(sourceBytes, targetBytes);
    }
}
```

以下拦截器实现请求头、时间戳、随机字符串和签名校验。

文件位置：`src/main/java/io/github/atengk/crypto/interceptor/CryptoValidateInterceptor.java`

```java
package io.github.atengk.crypto.interceptor;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.atengk.crypto.annotation.CryptoIgnore;
import io.github.atengk.crypto.annotation.DecryptRequest;
import io.github.atengk.crypto.config.CryptoProperties;
import io.github.atengk.crypto.constant.CryptoHeaderConstants;
import io.github.atengk.crypto.core.CryptoBody;
import io.github.atengk.crypto.core.CryptoKeyProvider;
import io.github.atengk.crypto.core.CryptoSignUtils;
import io.github.atengk.crypto.core.CryptoUtils;
import io.github.atengk.crypto.exception.CryptoException;
import io.github.atengk.crypto.web.CachedBodyHttpServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.lang.annotation.Annotation;
import java.time.Duration;

/**
 * 接口加密参数校验拦截器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CryptoValidateInterceptor implements HandlerInterceptor {

    private static final String NONCE_CACHE_PREFIX = "interface-crypto:nonce:";

    private final CryptoProperties cryptoProperties;
    private final CryptoKeyProvider cryptoKeyProvider;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate stringRedisTemplate;

    private final AntPathMatcher antPathMatcher = new AntPathMatcher();

    /**
     * Controller 执行前校验加密参数
     *
     * @param request 请求对象
     * @param response 响应对象
     * @param handler 处理器
     * @return 是否放行
     */
    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        if (!needValidate(request, handlerMethod)) {
            return true;
        }

        String version = request.getHeader(CryptoHeaderConstants.VERSION);
        String keyId = request.getHeader(CryptoHeaderConstants.KEY_ID);
        String timestamp = request.getHeader(CryptoHeaderConstants.TIMESTAMP);
        String nonce = request.getHeader(CryptoHeaderConstants.NONCE);
        String sign = request.getHeader(CryptoHeaderConstants.SIGN);

        validateHeaders(version, keyId, timestamp, nonce, sign);
        validateTimestamp(timestamp);
        validateNonce(keyId, nonce);

        CryptoBody cryptoBody = readCryptoBody(request);
        validateBody(cryptoBody);

        String signText = CryptoSignUtils.buildRequestSignText(
                request.getMethod(),
                request.getRequestURI(),
                timestamp,
                nonce,
                keyId,
                cryptoBody.getEncryptedKey(),
                cryptoBody.getIv(),
                cryptoBody.getData()
        );

        String expectedSign = CryptoUtils.hmacSha256Base64(signText, cryptoKeyProvider.getSignSecret(keyId));
        if (!CryptoSignUtils.secureEquals(expectedSign, sign)) {
            log.warn("接口加密签名校验失败，uri={}，keyId={}，nonce={}", request.getRequestURI(), keyId, nonce);
            throw new CryptoException("请求签名校验失败");
        }

        log.debug("接口加密参数校验通过，uri={}，keyId={}", request.getRequestURI(), keyId);
        return true;
    }

    /**
     * 判断是否需要校验
     *
     * @param request 请求对象
     * @param handlerMethod 处理方法
     * @return 是否需要校验
     */
    private boolean needValidate(HttpServletRequest request, HandlerMethod handlerMethod) {
        if (!Boolean.TRUE.equals(cryptoProperties.getEnabled())) {
            return false;
        }

        String uri = request.getRequestURI();
        boolean excluded = cryptoProperties.getExcludePaths()
                .stream()
                .anyMatch(pattern -> antPathMatcher.match(pattern, uri));
        if (excluded) {
            return false;
        }

        if (hasAnnotation(handlerMethod, CryptoIgnore.class)) {
            return false;
        }

        if (hasAnnotation(handlerMethod, DecryptRequest.class)) {
            return true;
        }

        return Boolean.TRUE.equals(cryptoProperties.getDefaultRequestDecrypt());
    }

    /**
     * 校验请求头
     *
     * @param version 协议版本
     * @param keyId 密钥标识
     * @param timestamp 时间戳
     * @param nonce 随机字符串
     * @param sign 签名
     */
    private void validateHeaders(String version, String keyId, String timestamp, String nonce, String sign) {
        if (StrUtil.hasBlank(version, keyId, timestamp, nonce, sign)) {
            throw new CryptoException("加密请求头缺少必要字段");
        }
    }

    /**
     * 校验请求时间戳
     *
     * @param timestamp 时间戳
     */
    private void validateTimestamp(String timestamp) {
        if (!NumberUtil.isLong(timestamp)) {
            throw new CryptoException("请求时间戳格式错误");
        }

        long requestTime = NumberUtil.parseLong(timestamp);
        long currentTime = System.currentTimeMillis();
        long expireMillis = cryptoProperties.getTimestampExpireSeconds() * 1000L;

        if (Math.abs(currentTime - requestTime) > expireMillis) {
            throw new CryptoException("请求已过期");
        }
    }

    /**
     * 校验随机字符串，防止请求重放
     *
     * @param keyId 密钥标识
     * @param nonce 随机字符串
     */
    private void validateNonce(String keyId, String nonce) {
        String cacheKey = NONCE_CACHE_PREFIX + keyId + ":" + nonce;
        Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(
                cacheKey,
                "1",
                Duration.ofSeconds(cryptoProperties.getTimestampExpireSeconds())
        );

        if (!Boolean.TRUE.equals(success)) {
            log.warn("检测到重复加密请求，keyId={}，nonce={}", keyId, nonce);
            throw new CryptoException("请求重复提交");
        }
    }

    /**
     * 读取密文请求体
     *
     * @param request 请求对象
     * @return 密文请求体
     */
    private CryptoBody readCryptoBody(HttpServletRequest request) throws Exception {
        if (!(request instanceof CachedBodyHttpServletRequest cachedRequest)) {
            throw new CryptoException("请求体未缓存，无法进行签名校验");
        }

        String body = cachedRequest.getCachedBodyAsString();
        if (StrUtil.isBlank(body)) {
            throw new CryptoException("加密请求体不能为空");
        }

        return objectMapper.readValue(body, CryptoBody.class);
    }

    /**
     * 校验密文请求体
     *
     * @param cryptoBody 密文请求体
     */
    private void validateBody(CryptoBody cryptoBody) {
        if (cryptoBody == null) {
            throw new CryptoException("加密请求体格式错误");
        }
        if (StrUtil.hasBlank(cryptoBody.getEncryptedKey(), cryptoBody.getIv(), cryptoBody.getData())) {
            throw new CryptoException("加密请求体缺少必要字段");
        }
    }

    /**
     * 判断处理方法或类上是否存在注解
     *
     * @param handlerMethod 处理方法
     * @param annotationClass 注解类型
     * @return 是否存在
     */
    private boolean hasAnnotation(HandlerMethod handlerMethod, Class<? extends Annotation> annotationClass) {
        return handlerMethod.hasMethodAnnotation(annotationClass)
                || handlerMethod.getBeanType().isAnnotationPresent(annotationClass);
    }
}
```

以下配置类注册拦截器。

文件位置：`src/main/java/io/github/atengk/crypto/config/CryptoMvcConfiguration.java`

```java
package io.github.atengk.crypto.config;

import cn.hutool.core.collection.CollUtil;
import io.github.atengk.crypto.interceptor.CryptoValidateInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 接口加密 MVC 配置
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class CryptoMvcConfiguration implements WebMvcConfigurer {

    private final CryptoProperties cryptoProperties;
    private final CryptoValidateInterceptor cryptoValidateInterceptor;

    /**
     * 注册接口加密校验拦截器
     *
     * @param registry 拦截器注册器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        var registration = registry.addInterceptor(cryptoValidateInterceptor)
                .addPathPatterns("/**");

        if (CollUtil.isNotEmpty(cryptoProperties.getExcludePaths())) {
            registration.excludePathPatterns(cryptoProperties.getExcludePaths());
        }

        log.info("接口加密校验拦截器注册完成");
    }
}
```

### 全局异常处理

全局异常处理用于统一捕获加密解密异常、签名异常、防重放异常和参数校验异常，并返回标准错误结构。对于解密前失败的请求，服务端通常无法获取 AES Key，因此这类错误响应一般返回明文错误结构；如果业务要求错误响应也必须加密，需要额外设计客户端公钥机制。

以下是统一响应对象示例。如果项目已有统一返回结构，可以直接替换为项目内的 `Result`、`R` 或 `CommonResult`。

文件位置：`src/main/java/io/github/atengk/crypto/model/ApiResult.java`

```java
package io.github.atengk.crypto.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 接口统一响应对象
 *
 * @author Ateng
 * @since 2026-05-06
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
     * 响应码
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
     * 时间戳
     */
    private Long timestamp;

    /**
     * 成功响应
     *
     * @param data 响应数据
     * @return 响应对象
     * @param <T> 数据类型
     */
    public static <T> ApiResult<T> success(T data) {
        return new ApiResult<>(Boolean.TRUE, "SUCCESS", "操作成功", data, System.currentTimeMillis());
    }

    /**
     * 失败响应
     *
     * @param code 响应码
     * @param message 响应消息
     * @return 响应对象
     * @param <T> 数据类型
     */
    public static <T> ApiResult<T> fail(String code, String message) {
        return new ApiResult<>(Boolean.FALSE, code, message, null, System.currentTimeMillis());
    }
}
```

以下全局异常处理器用于统一处理接口加密相关异常。

文件位置：`src/main/java/io/github/atengk/crypto/advice/GlobalExceptionHandler.java`

```java
package io.github.atengk.crypto.advice;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.crypto.exception.CryptoException;
import io.github.atengk.crypto.model.ApiResult;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理接口加密异常
     *
     * @param exception 加密异常
     * @param request 请求对象
     * @return 统一错误响应
     */
    @ExceptionHandler(CryptoException.class)
    public ApiResult<Void> handleCryptoException(CryptoException exception, HttpServletRequest request) {
        log.warn("接口加密处理失败，uri={}，原因={}", request.getRequestURI(), exception.getMessage());
        return ApiResult.fail("CRYPTO_ERROR", exception.getMessage());
    }

    /**
     * 处理请求体参数校验异常
     *
     * @param exception 参数校验异常
     * @return 统一错误响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResult<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(error -> StrUtil.format("{} {}", error.getField(), error.getDefaultMessage()))
                .orElse("请求参数校验失败");
        return ApiResult.fail("PARAM_INVALID", message);
    }

    /**
     * 处理表单参数绑定异常
     *
     * @param exception 参数绑定异常
     * @return 统一错误响应
     */
    @ExceptionHandler(BindException.class)
    public ApiResult<Void> handleBindException(BindException exception) {
        String message = CollUtil.isEmpty(exception.getFieldErrors())
                ? "请求参数绑定失败"
                : StrUtil.format("{} {}", exception.getFieldError().getField(), exception.getFieldError().getDefaultMessage());
        return ApiResult.fail("PARAM_BIND_ERROR", message);
    }

    /**
     * 处理系统未知异常
     *
     * @param exception 系统异常
     * @param request 请求对象
     * @return 统一错误响应
     */
    @ExceptionHandler(Exception.class)
    public ApiResult<Void> handleException(Exception exception, HttpServletRequest request) {
        log.error("系统异常，uri={}", request.getRequestURI(), exception);
        return ApiResult.fail(String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value()), "系统异常，请稍后重试");
    }
}
```

建议错误码定义如下：

| 错误码                   | 触发场景               |
| ------------------------ | ---------------------- |
| `CRYPTO_ERROR`           | 通用接口加密解密异常   |
| `CRYPTO_HEADER_MISSING`  | 加密请求头缺失         |
| `CRYPTO_BODY_INVALID`    | 密文请求体格式错误     |
| `CRYPTO_SIGN_INVALID`    | 签名校验失败           |
| `CRYPTO_REQUEST_EXPIRED` | 请求时间戳过期         |
| `CRYPTO_REPLAY_REQUEST`  | 重放请求               |
| `CRYPTO_KEY_NOT_FOUND`   | `keyId` 对应密钥不存在 |
| `CRYPTO_DECRYPT_FAILED`  | 请求体解密失败         |
| `PARAM_INVALID`          | 明文 DTO 参数校验失败  |

实际项目中可以将 `CryptoException` 扩展为携带错误码的异常类，例如 `new CryptoException("CRYPTO_SIGN_INVALID", "请求签名校验失败")`，这样全局异常处理可以返回更精确的错误码。



## 注解设计

本章节用于说明接口加密解密能力如何通过注解控制。注解设计的目标是减少业务代码侵入，让业务 Controller 只关注明文 DTO 和普通响应对象，加解密开关由类级别或方法级别注解决定。

推荐提供三个核心注解：

| 注解               | 作用                 |
| ------------------ | -------------------- |
| `@DecryptRequest`  | 开启请求体解密       |
| `@EncryptResponse` | 开启响应体加密       |
| `@CryptoIgnore`    | 忽略接口加密解密处理 |

### 接口加密注解

接口加密注解用于标记某个 Controller 或某个方法的响应结果需要加密。该注解对应服务端的 `ResponseBodyAdvice` 处理逻辑。

推荐注解名称为 `@EncryptResponse`，语义明确，表示“服务端响应需要加密”。

该注解用于声明响应结果需要加密，可放在 Controller 类或具体接口方法上。

文件位置：`src/main/java/io/github/atengk/crypto/annotation/EncryptResponse.java`

```java
package io.github.atengk.crypto.annotation;

import java.lang.annotation.*;

/**
 * 开启响应体加密
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EncryptResponse {

    /**
     * 是否启用响应加密
     *
     * @return 是否启用
     */
    boolean enabled() default true;
}
```

使用示例：

该示例展示在方法级别开启响应加密，Controller 返回普通对象即可。

文件位置：`src/main/java/io/github/atengk/demo/controller/UserController.java`

```java
package io.github.atengk.demo.controller;

import io.github.atengk.crypto.annotation.EncryptResponse;
import io.github.atengk.crypto.model.ApiResult;
import lombok.Data;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户接口示例
 *
 * @author Ateng
 * @since 2026-05-06
 */
@RestController
public class UserController {

    /**
     * 获取用户信息，响应体加密返回
     *
     * @return 用户信息
     */
    @EncryptResponse
    @GetMapping("/api/user/profile")
    public ApiResult<UserProfileVO> profile() {
        UserProfileVO vo = new UserProfileVO();
        vo.setUserId(10001L);
        vo.setNickname("Ateng");
        vo.setMobile("138****8000");
        return ApiResult.success(vo);
    }

    /**
     * 用户信息响应对象
     *
     * @author Ateng
     * @since 2026-05-06
     */
    @Data
    public static class UserProfileVO {

        /**
         * 用户 ID
         */
        private Long userId;

        /**
         * 昵称
         */
        private String nickname;

        /**
         * 手机号
         */
        private String mobile;
    }
}
```

响应加密注解的处理规则如下：

| 场景                                       | 处理规则                               |
| ------------------------------------------ | -------------------------------------- |
| 方法上存在 `@EncryptResponse`              | 当前方法响应加密                       |
| 类上存在 `@EncryptResponse`                | 当前 Controller 下所有方法默认响应加密 |
| 方法上 `@EncryptResponse(enabled = false)` | 当前方法关闭响应加密                   |
| 同时存在 `@CryptoIgnore`                   | 优先忽略，不执行响应加密               |
| 返回文件、流、字节数组                     | 即使存在注解，也建议跳过响应加密       |

### 接口解密注解

接口解密注解用于标记某个 Controller 或某个方法的请求体需要解密。该注解对应服务端的 `RequestBodyAdvice` 和 `HandlerInterceptor` 处理逻辑。

推荐注解名称为 `@DecryptRequest`，语义明确，表示“客户端请求体是密文，服务端需要解密后再交给业务接口”。

该注解用于声明请求体需要解密，可放在 Controller 类或具体接口方法上。

文件位置：`src/main/java/io/github/atengk/crypto/annotation/DecryptRequest.java`

```java
package io.github.atengk.crypto.annotation;

import java.lang.annotation.*;

/**
 * 开启请求体解密
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DecryptRequest {

    /**
     * 是否启用请求解密
     *
     * @return 是否启用
     */
    boolean enabled() default true;
}
```

使用示例：

该示例展示登录接口开启请求解密和响应加密，业务方法仍然接收普通明文 DTO。

文件位置：`src/main/java/io/github/atengk/demo/controller/AuthController.java`

```java
package io.github.atengk.demo.controller;

import io.github.atengk.crypto.annotation.DecryptRequest;
import io.github.atengk.crypto.annotation.EncryptResponse;
import io.github.atengk.crypto.model.ApiResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 认证接口示例
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    /**
     * 登录接口，请求体解密，响应体加密
     *
     * @param dto 登录请求
     * @return 登录结果
     */
    @DecryptRequest
    @EncryptResponse
    @PostMapping("/login")
    public ApiResult<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        log.info("用户登录请求，username={}", dto.getUsername());

        LoginVO vo = new LoginVO();
        vo.setAccessToken("mock-access-token");
        vo.setTokenType("Bearer");
        vo.setExpireSeconds(7200L);
        return ApiResult.success(vo);
    }

    /**
     * 登录请求对象
     *
     * @author Ateng
     * @since 2026-05-06
     */
    @Data
    public static class LoginDTO {

        /**
         * 用户名
         */
        @NotBlank(message = "用户名不能为空")
        private String username;

        /**
         * 密码
         */
        @NotBlank(message = "密码不能为空")
        private String password;
    }

    /**
     * 登录响应对象
     *
     * @author Ateng
     * @since 2026-05-06
     */
    @Data
    public static class LoginVO {

        /**
         * 访问令牌
         */
        private String accessToken;

        /**
         * 令牌类型
         */
        private String tokenType;

        /**
         * 过期时间，单位秒
         */
        private Long expireSeconds;
    }
}
```

请求解密注解的处理规则如下：

| 场景                                      | 处理规则                                    |
| ----------------------------------------- | ------------------------------------------- |
| 方法上存在 `@DecryptRequest`              | 当前方法请求体解密                          |
| 类上存在 `@DecryptRequest`                | 当前 Controller 下所有方法默认请求体解密    |
| 方法上 `@DecryptRequest(enabled = false)` | 当前方法关闭请求解密                        |
| 同时存在 `@CryptoIgnore`                  | 优先忽略，不执行请求解密                    |
| GET 请求无请求体                          | 不建议使用请求体解密，敏感查询建议改为 POST |

### 类级别与方法级别控制

类级别与方法级别控制用于解决同一个 Controller 下多数接口需要加密、少数接口不需要加密的场景。推荐使用“类级别默认控制，方法级别局部覆盖”的方式。

忽略注解用于跳过请求解密、响应加密、签名校验和防重放处理。适合公钥获取、健康检查、文档接口、文件下载等场景。

文件位置：`src/main/java/io/github/atengk/crypto/annotation/CryptoIgnore.java`

```java
package io.github.atengk.crypto.annotation;

import java.lang.annotation.*;

/**
 * 忽略接口加密解密
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CryptoIgnore {

    /**
     * 忽略原因，便于代码审查
     *
     * @return 忽略原因
     */
    String reason() default "";
}
```

推荐优先级如下：

```text
全局 enabled=false
  -> 不执行加密解密

路径命中 exclude-paths
  -> 不执行加密解密

方法存在 @CryptoIgnore
  -> 不执行加密解密

类存在 @CryptoIgnore
  -> 不执行加密解密

方法存在 @DecryptRequest(enabled = true)
  -> 请求解密

方法存在 @DecryptRequest(enabled = false)
  -> 请求不解密

类存在 @DecryptRequest(enabled = true)
  -> 请求解密

方法存在 @EncryptResponse(enabled = true)
  -> 响应加密

方法存在 @EncryptResponse(enabled = false)
  -> 响应不加密

类存在 @EncryptResponse(enabled = true)
  -> 响应加密

没有任何注解
  -> 使用 application.yml 中的默认配置
```

以下示例展示类级别默认加解密，方法级别忽略部分接口。

文件位置：`src/main/java/io/github/atengk/demo/controller/OrderController.java`

```java
package io.github.atengk.demo.controller;

import io.github.atengk.crypto.annotation.CryptoIgnore;
import io.github.atengk.crypto.annotation.DecryptRequest;
import io.github.atengk.crypto.annotation.EncryptResponse;
import io.github.atengk.crypto.model.ApiResult;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 订单接口示例
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@DecryptRequest
@EncryptResponse
@RestController
@RequestMapping("/api/order")
public class OrderController {

    /**
     * 创建订单，继承类级别请求解密和响应加密
     *
     * @param dto 创建订单请求
     * @return 创建结果
     */
    @PostMapping("/create")
    public ApiResult<OrderCreateVO> create(@RequestBody OrderCreateDTO dto) {
        log.info("创建订单请求，productId={}，quantity={}", dto.getProductId(), dto.getQuantity());

        OrderCreateVO vo = new OrderCreateVO();
        vo.setOrderNo("ORDER202605060001");
        return ApiResult.success(vo);
    }

    /**
     * 订单状态公开查询，不执行接口加解密
     *
     * @param orderNo 订单号
     * @return 订单状态
     */
    @CryptoIgnore(reason = "公开查询接口，无敏感数据")
    @GetMapping("/public/status")
    public ApiResult<String> publicStatus(@RequestParam String orderNo) {
        return ApiResult.success("PAID");
    }

    /**
     * 创建订单请求对象
     *
     * @author Ateng
     * @since 2026-05-06
     */
    @Data
    public static class OrderCreateDTO {

        /**
         * 商品 ID
         */
        private Long productId;

        /**
         * 购买数量
         */
        private Integer quantity;
    }

    /**
     * 创建订单响应对象
     *
     * @author Ateng
     * @since 2026-05-06
     */
    @Data
    public static class OrderCreateVO {

        /**
         * 订单号
         */
        private String orderNo;
    }
}
```

为了支持 `enabled = false` 的细粒度覆盖，需要调整前文的 `CryptoSwitchSupport`。以下实现支持注解启停值、类方法优先级和忽略注解。

文件位置：`src/main/java/io/github/atengk/crypto/core/CryptoSwitchSupport.java`

```java
package io.github.atengk.crypto.core;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.crypto.annotation.CryptoIgnore;
import io.github.atengk.crypto.annotation.DecryptRequest;
import io.github.atengk.crypto.annotation.EncryptResponse;
import io.github.atengk.crypto.config.CryptoProperties;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

/**
 * 接口加密开关判断组件
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Component
@RequiredArgsConstructor
public class CryptoSwitchSupport {

    private final CryptoProperties cryptoProperties;

    private final AntPathMatcher antPathMatcher = new AntPathMatcher();

    /**
     * 判断是否需要请求解密
     *
     * @param methodParameter 方法参数
     * @return 是否需要请求解密
     */
    public boolean needDecrypt(MethodParameter methodParameter) {
        if (isDisabledOrExcluded()) {
            return false;
        }
        if (hasIgnore(methodParameter)) {
            return false;
        }

        Method method = methodParameter.getMethod();
        if (method != null) {
            DecryptRequest methodAnnotation = method.getAnnotation(DecryptRequest.class);
            if (methodAnnotation != null) {
                return methodAnnotation.enabled();
            }
        }

        DecryptRequest classAnnotation = methodParameter.getContainingClass().getAnnotation(DecryptRequest.class);
        if (classAnnotation != null) {
            return classAnnotation.enabled();
        }

        return Boolean.TRUE.equals(cryptoProperties.getDefaultRequestDecrypt());
    }

    /**
     * 判断是否需要响应加密
     *
     * @param methodParameter 方法参数
     * @return 是否需要响应加密
     */
    public boolean needEncrypt(MethodParameter methodParameter) {
        if (isDisabledOrExcluded()) {
            return false;
        }
        if (hasIgnore(methodParameter)) {
            return false;
        }

        Method method = methodParameter.getMethod();
        if (method != null) {
            EncryptResponse methodAnnotation = method.getAnnotation(EncryptResponse.class);
            if (methodAnnotation != null) {
                return methodAnnotation.enabled();
            }
        }

        EncryptResponse classAnnotation = methodParameter.getContainingClass().getAnnotation(EncryptResponse.class);
        if (classAnnotation != null) {
            return classAnnotation.enabled();
        }

        return Boolean.TRUE.equals(cryptoProperties.getDefaultResponseEncrypt());
    }

    /**
     * 判断是否全局关闭或路径排除
     *
     * @return 是否跳过
     */
    private boolean isDisabledOrExcluded() {
        if (!Boolean.TRUE.equals(cryptoProperties.getEnabled())) {
            return true;
        }

        HttpServletRequest request = getCurrentRequest();
        if (request == null) {
            return false;
        }

        String uri = request.getRequestURI();
        if (StrUtil.isBlank(uri) || CollUtil.isEmpty(cryptoProperties.getExcludePaths())) {
            return false;
        }

        return cryptoProperties.getExcludePaths()
                .stream()
                .anyMatch(pattern -> antPathMatcher.match(pattern, uri));
    }

    /**
     * 判断是否存在忽略注解
     *
     * @param methodParameter 方法参数
     * @return 是否忽略
     */
    private boolean hasIgnore(MethodParameter methodParameter) {
        Method method = methodParameter.getMethod();
        boolean methodIgnored = method != null && method.isAnnotationPresent(CryptoIgnore.class);
        boolean classIgnored = methodParameter.getContainingClass().isAnnotationPresent(CryptoIgnore.class);
        return methodIgnored || classIgnored;
    }

    /**
     * 获取当前请求
     *
     * @return 当前请求
     */
    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes == null ? null : attributes.getRequest();
    }
}
```

## 安全校验

本章节说明加密接口在解密前必须完成的安全校验，包括时间戳校验、随机字符串校验、签名校验和防重放处理。安全校验建议统一放在 `HandlerInterceptor` 中执行，保证非法请求不会进入 Controller。

整体校验顺序建议如下：

```text
判断接口是否需要加密校验
  -> 校验请求头完整性
  -> 校验时间戳是否过期
  -> 校验 nonce 格式
  -> 校验 nonce 是否重复
  -> 读取密文请求体
  -> 校验密文请求体完整性
  -> 构造签名原文
  -> 计算服务端签名
  -> 安全比较签名
  -> 校验通过后放行
```

### 时间戳校验

时间戳校验用于限制请求有效期，避免攻击者拿到历史请求后长期重复使用。客户端需要在请求头中传入毫秒时间戳，服务端根据当前系统时间判断是否超过允许偏差。

请求头示例：

```http
X-Crypto-Timestamp: 1777996800000
```

推荐配置：

```yaml
interface-crypto:
  # 请求时间戳允许偏差，单位秒
  timestamp-expire-seconds: 300
```

校验规则如下：

| 校验项           | 规则                                                   |
| ---------------- | ------------------------------------------------------ |
| 是否为空         | 为空直接拒绝                                           |
| 是否为数字       | 非数字直接拒绝                                         |
| 是否为毫秒时间戳 | 建议长度为 13 位                                       |
| 是否超过有效期   | `abs(服务端当前时间 - 客户端时间戳) > 允许偏差` 时拒绝 |
| 是否允许未来时间 | 允许小范围时钟偏差，但不允许超过配置窗口               |

以下工具方法用于校验请求时间戳。

文件位置：`src/main/java/io/github/atengk/crypto/core/CryptoSecurityValidator.java`

```java
package io.github.atengk.crypto.core;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.crypto.config.CryptoProperties;
import io.github.atengk.crypto.exception.CryptoException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 接口加密安全校验组件
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Component
@RequiredArgsConstructor
public class CryptoSecurityValidator {

    private final CryptoProperties cryptoProperties;

    /**
     * 校验请求时间戳
     *
     * @param timestamp 时间戳字符串
     */
    public void validateTimestamp(String timestamp) {
        if (StrUtil.isBlank(timestamp)) {
            throw new CryptoException("CRYPTO_HEADER_MISSING", "请求时间戳不能为空");
        }
        if (!NumberUtil.isLong(timestamp)) {
            throw new CryptoException("CRYPTO_TIMESTAMP_INVALID", "请求时间戳格式错误");
        }

        long requestTime = NumberUtil.parseLong(timestamp);
        long currentTime = System.currentTimeMillis();
        long expireMillis = cryptoProperties.getTimestampExpireSeconds() * 1000L;

        if (Math.abs(currentTime - requestTime) > expireMillis) {
            throw new CryptoException("CRYPTO_REQUEST_EXPIRED", "请求已过期");
        }
    }

    /**
     * 校验随机字符串格式
     *
     * @param nonce 随机字符串
     */
    public void validateNonceFormat(String nonce) {
        if (StrUtil.isBlank(nonce)) {
            throw new CryptoException("CRYPTO_HEADER_MISSING", "请求随机字符串不能为空");
        }
        if (nonce.length() < 16 || nonce.length() > 64) {
            throw new CryptoException("CRYPTO_NONCE_INVALID", "请求随机字符串长度不合法");
        }
        if (!nonce.matches("^[A-Za-z0-9_-]+$")) {
            throw new CryptoException("CRYPTO_NONCE_INVALID", "请求随机字符串格式不合法");
        }
    }
}
```

为了支持上面的错误码形式，建议将前文的 `CryptoException` 调整为携带错误码。

文件位置：`src/main/java/io/github/atengk/crypto/exception/CryptoException.java`

```java
package io.github.atengk.crypto.exception;

import lombok.Getter;

/**
 * 接口加密解密异常
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Getter
public class CryptoException extends RuntimeException {

    /**
     * 错误码
     */
    private final String code;

    public CryptoException(String message) {
        super(message);
        this.code = "CRYPTO_ERROR";
    }

    public CryptoException(String message, Throwable cause) {
        super(message, cause);
        this.code = "CRYPTO_ERROR";
    }

    public CryptoException(String code, String message) {
        super(message);
        this.code = code;
    }

    public CryptoException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }
}
```

### 随机字符串校验

随机字符串校验用于保证每次请求都有唯一标识。客户端每次请求都应生成新的 `nonce`，服务端对 `nonce` 做格式校验和缓存记录。

请求头示例：

```http
X-Crypto-Nonce: 3f7f2c2d9d874cbf
```

推荐规则如下：

| 规则     | 建议                           |
| -------- | ------------------------------ |
| 长度     | 16 到 64 位                    |
| 字符     | 字母、数字、下划线、短横线     |
| 生成方式 | 使用安全随机数，不使用自增序号 |
| 生命周期 | 与时间戳有效期一致             |
| 唯一维度 | 建议按 `keyId + nonce` 唯一    |
| 存储位置 | 生产环境使用 Redis             |

不建议只校验 `nonce` 格式而不做缓存记录。只做格式校验无法防止攻击者在有效期内重复提交同一请求。

### 签名校验

签名校验用于保证请求头和密文请求体未被篡改。客户端和服务端必须使用完全一致的签名原文拼接规则、字符编码和签名算法。

推荐参与签名的字段如下：

| 字段            | 来源     | 说明                           |
| --------------- | -------- | ------------------------------ |
| `HTTP_METHOD`   | 请求方法 | 例如 `POST`                    |
| `REQUEST_PATH`  | 请求路径 | 不包含域名，不包含 QueryString |
| `TIMESTAMP`     | 请求头   | `X-Crypto-Timestamp`           |
| `NONCE`         | 请求头   | `X-Crypto-Nonce`               |
| `KEY_ID`        | 请求头   | `X-Crypto-Key-Id`              |
| `ENCRYPTED_KEY` | 请求体   | RSA 加密后的 AES Key           |
| `IV`            | 请求体   | AES-GCM IV                     |
| `DATA`          | 请求体   | AES-GCM 加密后的业务数据       |

签名原文示例：

```text
POST
/api/auth/login
1777996800000
3f7f2c2d9d874cbf
server-key-202605
Base64(RSA-OAEP加密后的AES密钥)
Base64(AES-GCM初始化向量)
Base64(AES-GCM加密后的业务JSON)
```

签名算法推荐：

```text
Base64(HMAC-SHA256(signText, signSecret))
```

签名校验注意事项如下：

| 注意事项               | 说明                                                         |
| ---------------------- | ------------------------------------------------------------ |
| 字段顺序必须固定       | 客户端和服务端必须完全一致                                   |
| 换行符必须固定         | 建议统一使用 `\n`                                            |
| 编码必须固定           | 建议统一使用 UTF-8                                           |
| Base64 形式必须固定    | 不要一端使用 URL Safe Base64，另一端使用普通 Base64          |
| 比较签名要使用安全比较 | 避免普通字符串比较带来的时序攻击风险                         |
| 签名密钥不能前端硬编码 | Web 场景签名密钥暴露风险较高，强安全场景应使用设备密钥或后端换取临时密钥 |

以下签名校验方法可以放入 `CryptoSecurityValidator` 中。

文件位置：`src/main/java/io/github/atengk/crypto/core/CryptoSecurityValidator.java`

```java
package io.github.atengk.crypto.core;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.crypto.config.CryptoProperties;
import io.github.atengk.crypto.exception.CryptoException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 接口加密安全校验组件
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Component
@RequiredArgsConstructor
public class CryptoSecurityValidator {

    private final CryptoProperties cryptoProperties;
    private final CryptoKeyProvider cryptoKeyProvider;

    /**
     * 校验请求时间戳
     *
     * @param timestamp 时间戳字符串
     */
    public void validateTimestamp(String timestamp) {
        if (StrUtil.isBlank(timestamp)) {
            throw new CryptoException("CRYPTO_HEADER_MISSING", "请求时间戳不能为空");
        }
        if (!NumberUtil.isLong(timestamp)) {
            throw new CryptoException("CRYPTO_TIMESTAMP_INVALID", "请求时间戳格式错误");
        }

        long requestTime = NumberUtil.parseLong(timestamp);
        long currentTime = System.currentTimeMillis();
        long expireMillis = cryptoProperties.getTimestampExpireSeconds() * 1000L;

        if (Math.abs(currentTime - requestTime) > expireMillis) {
            throw new CryptoException("CRYPTO_REQUEST_EXPIRED", "请求已过期");
        }
    }

    /**
     * 校验随机字符串格式
     *
     * @param nonce 随机字符串
     */
    public void validateNonceFormat(String nonce) {
        if (StrUtil.isBlank(nonce)) {
            throw new CryptoException("CRYPTO_HEADER_MISSING", "请求随机字符串不能为空");
        }
        if (nonce.length() < 16 || nonce.length() > 64) {
            throw new CryptoException("CRYPTO_NONCE_INVALID", "请求随机字符串长度不合法");
        }
        if (!nonce.matches("^[A-Za-z0-9_-]+$")) {
            throw new CryptoException("CRYPTO_NONCE_INVALID", "请求随机字符串格式不合法");
        }
    }

    /**
     * 校验请求签名
     *
     * @param keyId 密钥标识
     * @param signText 签名原文
     * @param requestSign 客户端签名
     */
    public void validateSign(String keyId, String signText, String requestSign) {
        if (StrUtil.isBlank(requestSign)) {
            throw new CryptoException("CRYPTO_HEADER_MISSING", "请求签名不能为空");
        }

        String signSecret = cryptoKeyProvider.getSignSecret(keyId);
        String expectedSign = CryptoUtils.hmacSha256Base64(signText, signSecret);

        if (!CryptoSignUtils.secureEquals(expectedSign, requestSign)) {
            throw new CryptoException("CRYPTO_SIGN_INVALID", "请求签名校验失败");
        }
    }
}
```

这里给出的是整合后的 `CryptoSecurityValidator` 完整版本。如果前面已经创建过同名类，直接用该版本替换即可。

### 防重放处理

防重放处理用于防止攻击者在有效期内重复提交相同密文请求。仅依赖时间戳无法阻止有效期窗口内的重复请求，因此必须结合 `nonce` 缓存。

推荐 Redis 缓存 Key 设计：

```text
interface-crypto:nonce:{keyId}:{nonce}
```

示例：

```text
interface-crypto:nonce:server-key-202605:3f7f2c2d9d874cbf
```

缓存过期时间建议与 `timestamp-expire-seconds` 保持一致。例如请求有效期是 300 秒，则 `nonce` 缓存也设置 300 秒。

防重放校验规则如下：

| 步骤 | 说明                                  |
| ---- | ------------------------------------- |
| 1    | 校验 `nonce` 格式                     |
| 2    | 使用 `SETNX` 写入 Redis               |
| 3    | 写入成功表示首次请求，允许继续        |
| 4    | 写入失败表示 `nonce` 已存在，拒绝请求 |
| 5    | Redis Key 自动过期，避免长期占用内存  |

以下组件用于封装 Redis 防重放处理。

文件位置：`src/main/java/io/github/atengk/crypto/core/CryptoReplayService.java`

```java
package io.github.atengk.crypto.core;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.crypto.config.CryptoProperties;
import io.github.atengk.crypto.exception.CryptoException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 接口加密防重放服务
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CryptoReplayService {

    private static final String NONCE_CACHE_PREFIX = "interface-crypto:nonce:";

    private final CryptoProperties cryptoProperties;
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 校验并记录随机字符串
     *
     * @param keyId 密钥标识
     * @param nonce 随机字符串
     */
    public void checkAndSaveNonce(String keyId, String nonce) {
        String cacheKey = buildNonceCacheKey(keyId, nonce);
        Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(
                cacheKey,
                "1",
                Duration.ofSeconds(cryptoProperties.getTimestampExpireSeconds())
        );

        if (!Boolean.TRUE.equals(success)) {
            log.warn("检测到重放请求，keyId={}，nonce={}", keyId, nonce);
            throw new CryptoException("CRYPTO_REPLAY_REQUEST", "请求重复提交");
        }
    }

    /**
     * 构造 nonce 缓存 Key
     *
     * @param keyId 密钥标识
     * @param nonce 随机字符串
     * @return Redis Key
     */
    private String buildNonceCacheKey(String keyId, String nonce) {
        return NONCE_CACHE_PREFIX + StrUtil.blankToDefault(keyId, "default") + ":" + nonce;
    }
}
```

生产环境建议使用 Redis 完成防重放处理。如果当前项目不方便接入 Redis，可在开发环境使用 Caffeine、本地 Map 或 Hutool 缓存作为临时方案，但多实例部署时不能依赖本地缓存。

以下是整合安全校验后的拦截器核心版本，用于替换前文中直接写在拦截器里的时间戳、nonce 和签名校验逻辑。

文件位置：`src/main/java/io/github/atengk/crypto/interceptor/CryptoValidateInterceptor.java`

```java
package io.github.atengk.crypto.interceptor;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.atengk.crypto.annotation.CryptoIgnore;
import io.github.atengk.crypto.annotation.DecryptRequest;
import io.github.atengk.crypto.config.CryptoProperties;
import io.github.atengk.crypto.constant.CryptoHeaderConstants;
import io.github.atengk.crypto.core.CryptoBody;
import io.github.atengk.crypto.core.CryptoReplayService;
import io.github.atengk.crypto.core.CryptoSecurityValidator;
import io.github.atengk.crypto.core.CryptoSignUtils;
import io.github.atengk.crypto.exception.CryptoException;
import io.github.atengk.crypto.web.CachedBodyHttpServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.lang.annotation.Annotation;

/**
 * 接口加密参数校验拦截器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CryptoValidateInterceptor implements HandlerInterceptor {

    private final CryptoProperties cryptoProperties;
    private final ObjectMapper objectMapper;
    private final CryptoSecurityValidator cryptoSecurityValidator;
    private final CryptoReplayService cryptoReplayService;

    private final AntPathMatcher antPathMatcher = new AntPathMatcher();

    /**
     * Controller 执行前校验加密请求参数
     *
     * @param request 请求对象
     * @param response 响应对象
     * @param handler 处理器
     * @return 是否放行
     */
    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        if (!needValidate(request, handlerMethod)) {
            return true;
        }

        String version = request.getHeader(CryptoHeaderConstants.VERSION);
        String keyId = request.getHeader(CryptoHeaderConstants.KEY_ID);
        String timestamp = request.getHeader(CryptoHeaderConstants.TIMESTAMP);
        String nonce = request.getHeader(CryptoHeaderConstants.NONCE);
        String sign = request.getHeader(CryptoHeaderConstants.SIGN);

        validateRequiredHeaders(version, keyId, timestamp, nonce, sign);

        cryptoSecurityValidator.validateTimestamp(timestamp);
        cryptoSecurityValidator.validateNonceFormat(nonce);
        cryptoReplayService.checkAndSaveNonce(keyId, nonce);

        CryptoBody cryptoBody = readCryptoBody(request);
        validateCryptoBody(cryptoBody);

        String signText = CryptoSignUtils.buildRequestSignText(
                request.getMethod(),
                request.getRequestURI(),
                timestamp,
                nonce,
                keyId,
                cryptoBody.getEncryptedKey(),
                cryptoBody.getIv(),
                cryptoBody.getData()
        );

        cryptoSecurityValidator.validateSign(keyId, signText, sign);

        log.debug("接口加密安全校验通过，uri={}，version={}，keyId={}", request.getRequestURI(), version, keyId);
        return true;
    }

    /**
     * 判断是否需要校验
     *
     * @param request 请求对象
     * @param handlerMethod 处理方法
     * @return 是否需要校验
     */
    private boolean needValidate(HttpServletRequest request, HandlerMethod handlerMethod) {
        if (!Boolean.TRUE.equals(cryptoProperties.getEnabled())) {
            return false;
        }

        String uri = request.getRequestURI();
        boolean excluded = cryptoProperties.getExcludePaths()
                .stream()
                .anyMatch(pattern -> antPathMatcher.match(pattern, uri));
        if (excluded) {
            return false;
        }

        if (hasAnnotation(handlerMethod, CryptoIgnore.class)) {
            return false;
        }

        DecryptRequest methodAnnotation = handlerMethod.getMethodAnnotation(DecryptRequest.class);
        if (methodAnnotation != null) {
            return methodAnnotation.enabled();
        }

        DecryptRequest classAnnotation = handlerMethod.getBeanType().getAnnotation(DecryptRequest.class);
        if (classAnnotation != null) {
            return classAnnotation.enabled();
        }

        return Boolean.TRUE.equals(cryptoProperties.getDefaultRequestDecrypt());
    }

    /**
     * 校验必要请求头
     *
     * @param version 协议版本
     * @param keyId 密钥标识
     * @param timestamp 时间戳
     * @param nonce 随机字符串
     * @param sign 签名
     */
    private void validateRequiredHeaders(String version, String keyId, String timestamp, String nonce, String sign) {
        if (StrUtil.hasBlank(version, keyId, timestamp, nonce, sign)) {
            throw new CryptoException("CRYPTO_HEADER_MISSING", "加密请求头缺少必要字段");
        }
    }

    /**
     * 读取密文请求体
     *
     * @param request 请求对象
     * @return 密文请求体
     */
    private CryptoBody readCryptoBody(HttpServletRequest request) throws Exception {
        if (!(request instanceof CachedBodyHttpServletRequest cachedRequest)) {
            throw new CryptoException("CRYPTO_BODY_NOT_CACHED", "请求体未缓存，无法进行签名校验");
        }

        String body = cachedRequest.getCachedBodyAsString();
        if (StrUtil.isBlank(body)) {
            throw new CryptoException("CRYPTO_BODY_INVALID", "加密请求体不能为空");
        }

        return objectMapper.readValue(body, CryptoBody.class);
    }

    /**
     * 校验密文请求体
     *
     * @param cryptoBody 密文请求体
     */
    private void validateCryptoBody(CryptoBody cryptoBody) {
        if (cryptoBody == null) {
            throw new CryptoException("CRYPTO_BODY_INVALID", "加密请求体格式错误");
        }
        if (StrUtil.hasBlank(cryptoBody.getEncryptedKey(), cryptoBody.getIv(), cryptoBody.getData())) {
            throw new CryptoException("CRYPTO_BODY_INVALID", "加密请求体缺少必要字段");
        }
    }

    /**
     * 判断处理方法或类上是否存在注解
     *
     * @param handlerMethod 处理方法
     * @param annotationClass 注解类型
     * @return 是否存在
     */
    private boolean hasAnnotation(HandlerMethod handlerMethod, Class<? extends Annotation> annotationClass) {
        return handlerMethod.hasMethodAnnotation(annotationClass)
                || handlerMethod.getBeanType().isAnnotationPresent(annotationClass);
    }
}
```

全局异常处理器中建议使用 `CryptoException#getCode()` 返回精确错误码。

文件位置：`src/main/java/io/github/atengk/crypto/advice/GlobalExceptionHandler.java`

```java
package io.github.atengk.crypto.advice;

import io.github.atengk.crypto.exception.CryptoException;
import io.github.atengk.crypto.model.ApiResult;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理接口加密异常
     *
     * @param exception 加密异常
     * @param request 请求对象
     * @return 统一错误响应
     */
    @ExceptionHandler(CryptoException.class)
    public ApiResult<Void> handleCryptoException(CryptoException exception, HttpServletRequest request) {
        log.warn("接口加密校验失败，uri={}，code={}，message={}",
                request.getRequestURI(),
                exception.getCode(),
                exception.getMessage());
        return ApiResult.fail(exception.getCode(), exception.getMessage());
    }
}
```

安全校验相关错误码建议如下：

| 错误码                     | 说明               |
| -------------------------- | ------------------ |
| `CRYPTO_HEADER_MISSING`    | 加密请求头缺失     |
| `CRYPTO_TIMESTAMP_INVALID` | 时间戳格式错误     |
| `CRYPTO_REQUEST_EXPIRED`   | 请求已过期         |
| `CRYPTO_NONCE_INVALID`     | 随机字符串格式错误 |
| `CRYPTO_REPLAY_REQUEST`    | 重放请求           |
| `CRYPTO_BODY_NOT_CACHED`   | 请求体未缓存       |
| `CRYPTO_BODY_INVALID`      | 密文请求体格式错误 |
| `CRYPTO_SIGN_INVALID`      | 请求签名校验失败   |
| `CRYPTO_KEY_NOT_FOUND`     | 密钥不存在         |
| `CRYPTO_DECRYPT_FAILED`    | 请求体解密失败     |

这一部分完成后，接口加密解密能力已经具备注解控制、安全校验、防重放处理和精确错误码返回能力。后续可以继续补充 `接口示例`、`测试验证` 和 `使用说明`。



## 接口示例

本章节通过具体接口展示加密请求、解密响应和不加密接口的使用方式。示例默认沿用前文约定的 `@DecryptRequest`、`@EncryptResponse`、`@CryptoIgnore` 注解，以及统一密文结构 `CryptoBody`。

### 加密请求示例

加密请求示例用于说明客户端调用需要请求体解密的接口时，应该如何组装请求头、请求体和签名。服务端接口仍然使用普通 DTO 接收参数，不直接接收密文对象。

服务端登录接口示例：

文件位置：`src/main/java/io/github/atengk/demo/controller/AuthController.java`

```java
package io.github.atengk.demo.controller;

import io.github.atengk.crypto.annotation.DecryptRequest;
import io.github.atengk.crypto.annotation.EncryptResponse;
import io.github.atengk.crypto.model.ApiResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 认证接口示例
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    /**
     * 用户登录，请求体解密，响应体加密
     *
     * @param dto 登录请求
     * @return 登录结果
     */
    @DecryptRequest
    @EncryptResponse
    @PostMapping("/login")
    public ApiResult<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        log.info("用户登录请求，username={}", dto.getUsername());

        LoginVO vo = new LoginVO();
        vo.setAccessToken("mock-access-token");
        vo.setTokenType("Bearer");
        vo.setExpireSeconds(7200L);
        return ApiResult.success(vo);
    }

    /**
     * 登录请求对象
     *
     * @author Ateng
     * @since 2026-05-06
     */
    @Data
    public static class LoginDTO {

        /**
         * 用户名
         */
        @NotBlank(message = "用户名不能为空")
        private String username;

        /**
         * 密码
         */
        @NotBlank(message = "密码不能为空")
        private String password;
    }

    /**
     * 登录响应对象
     *
     * @author Ateng
     * @since 2026-05-06
     */
    @Data
    public static class LoginVO {

        /**
         * 访问令牌
         */
        private String accessToken;

        /**
         * 令牌类型
         */
        private String tokenType;

        /**
         * 过期时间，单位秒
         */
        private Long expireSeconds;
    }
}
```

客户端原始业务参数如下：

```json
{
  "username": "ateng",
  "password": "123456"
}
```

客户端加密后发送的请求头示例：

```http
POST /api/auth/login HTTP/1.1
Host: localhost:8080
Content-Type: application/json
X-Crypto-Version: v1
X-Crypto-Key-Id: server-key-202605
X-Crypto-Timestamp: 1777996800000
X-Crypto-Nonce: 3f7f2c2d9d874cbf
X-Crypto-Sign: 3cFJ5L/9Rk8b4qx8qY6S6tJH4yW1dLq4fY2Vq7xJ3qM=
```

客户端加密后发送的请求体示例：

```json
{
  "encryptedKey": "RsaOaepEncryptedAesKeyBase64Value",
  "iv": "AesGcmIvBase64Value",
  "data": "AesGcmEncryptedRequestBodyBase64Value"
}
```

签名原文示例：

```text
POST
/api/auth/login
1777996800000
3f7f2c2d9d874cbf
server-key-202605
RsaOaepEncryptedAesKeyBase64Value
AesGcmIvBase64Value
AesGcmEncryptedRequestBodyBase64Value
```

签名结果生成规则：

```text
Base64(HMAC-SHA256(签名原文, signSecret))
```

调用示例：

```bash
curl -X POST "http://localhost:8080/api/auth/login" \
  -H "Content-Type: application/json" \
  -H "X-Crypto-Version: v1" \
  -H "X-Crypto-Key-Id: server-key-202605" \
  -H "X-Crypto-Timestamp: 1777996800000" \
  -H "X-Crypto-Nonce: 3f7f2c2d9d874cbf" \
  -H "X-Crypto-Sign: 3cFJ5L/9Rk8b4qx8qY6S6tJH4yW1dLq4fY2Vq7xJ3qM=" \
  -d '{
    "encryptedKey": "RsaOaepEncryptedAesKeyBase64Value",
    "iv": "AesGcmIvBase64Value",
    "data": "AesGcmEncryptedRequestBodyBase64Value"
  }'
```

以上 `encryptedKey`、`iv`、`data`、`X-Crypto-Sign` 均为示例占位值。实际调用时必须由客户端加密 SDK 或前端请求封装层动态生成。

### 解密响应示例

解密响应示例用于说明客户端收到服务端密文响应后，如何根据响应中的 `iv` 和 `data` 还原业务数据。默认方案中，服务端响应加密复用本次请求生成的 AES 会话密钥。

服务端原始业务响应对象如下：

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "操作成功",
  "data": {
    "accessToken": "mock-access-token",
    "tokenType": "Bearer",
    "expireSeconds": 7200
  },
  "timestamp": 1777996801000
}
```

服务端加密后实际返回给客户端的响应体如下：

```json
{
  "version": "v1",
  "algorithm": "AES-256-GCM",
  "keyId": "server-key-202605",
  "iv": "ResponseAesGcmIvBase64Value",
  "timestamp": 1777996801000,
  "nonce": "f8e7d6c5b4a32109",
  "data": "AesGcmEncryptedResponseBodyBase64Value"
}
```

客户端解密流程如下：

| 步骤 | 说明                                                    |
| ---- | ------------------------------------------------------- |
| 1    | 从响应体读取 `iv`                                       |
| 2    | 从响应体读取 `data`                                     |
| 3    | 使用本次请求生成的 AES Key 解密响应 `data`              |
| 4    | 将解密后的 JSON 反序列化为业务响应对象                  |
| 5    | 按普通响应结构处理 `success`、`code`、`message`、`data` |

客户端解密后的明文结果如下：

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "操作成功",
  "data": {
    "accessToken": "mock-access-token",
    "tokenType": "Bearer",
    "expireSeconds": 7200
  },
  "timestamp": 1777996801000
}
```

客户端需要注意：响应解密使用请求阶段保存的 AES Key，但响应 IV 必须使用服务端返回的 `iv`，不能复用请求 IV。

### 不加密接口示例

不加密接口用于健康检查、接口文档、公钥获取、公开查询、文件下载等不适合或不需要加密的场景。可以通过配置排除路径，也可以通过 `@CryptoIgnore` 注解跳过加解密处理。

示例一：公钥获取接口。

文件位置：`src/main/java/io/github/atengk/demo/controller/CryptoPublicController.java`

```java
package io.github.atengk.demo.controller;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.crypto.annotation.CryptoIgnore;
import io.github.atengk.crypto.config.CryptoProperties;
import io.github.atengk.crypto.model.ApiResult;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

/**
 * 加密公钥接口
 *
 * @author Ateng
 * @since 2026-05-06
 */
@CryptoIgnore(reason = "客户端需要明文获取服务端公钥")
@RestController
@RequiredArgsConstructor
public class CryptoPublicController {

    private final CryptoProperties cryptoProperties;
    private final ResourceLoader resourceLoader;

    /**
     * 获取服务端公钥
     *
     * @return 服务端公钥 PEM 文本
     */
    @GetMapping("/api/crypto/public-key")
    public ApiResult<String> publicKey() throws Exception {
        CryptoProperties.KeyConfig keyConfig = cryptoProperties.getKeys().get(cryptoProperties.getDefaultKeyId());
        String location = keyConfig.getPublicKeyLocation();
        String publicKeyText;

        if (StrUtil.startWith(location, "classpath:")) {
            Resource resource = resourceLoader.getResource(location);
            publicKeyText = IoUtil.read(resource.getInputStream(), StandardCharsets.UTF_8);
        } else {
            publicKeyText = FileUtil.readUtf8String(location);
        }

        return ApiResult.success(publicKeyText);
    }
}
```

调用示例：

```bash
curl -X GET "http://localhost:8080/api/crypto/public-key"
```

响应示例：

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "操作成功",
  "data": "-----BEGIN PUBLIC KEY-----\nMIIBIjANBgkq...\n-----END PUBLIC KEY-----",
  "timestamp": 1777996800000
}
```

示例二：健康检查接口。

文件位置：`src/main/java/io/github/atengk/demo/controller/HealthController.java`

```java
package io.github.atengk.demo.controller;

import io.github.atengk.crypto.annotation.CryptoIgnore;
import io.github.atengk.crypto.model.ApiResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 健康检查接口
 *
 * @author Ateng
 * @since 2026-05-06
 */
@CryptoIgnore(reason = "健康检查接口供监控系统直接识别")
@RestController
public class HealthController {

    /**
     * 健康检查
     *
     * @return 检查结果
     */
    @GetMapping("/api/health")
    public ApiResult<String> health() {
        return ApiResult.success("ok");
    }
}
```

也可以通过配置统一排除：

```yaml
interface-crypto:
  exclude-paths:
    - /actuator/**
    - /api/health
    - /api/crypto/public-key
    - /swagger-ui/**
    - /v3/api-docs/**
```

## 测试验证

本章节用于说明接口加密解密能力上线前需要覆盖的测试场景。测试重点不是只验证接口能调通，而是要验证正常链路、篡改请求、过期请求和重放请求都能得到符合预期的结果。

建议测试前准备以下内容：

| 准备项       | 说明                                             |
| ------------ | ------------------------------------------------ |
| RSA 公私钥   | 服务端保存私钥，客户端使用公钥                   |
| 签名密钥     | 前后端保持一致                                   |
| Redis        | 用于验证 `nonce` 防重放                          |
| 测试接口     | 至少包含加密接口和不加密接口                     |
| 请求生成工具 | 可使用前端 SDK、Postman 预请求脚本或本地测试工具 |

### 正常加密解密测试

正常加密解密测试用于验证客户端加密请求后，服务端能够正确解密请求体并执行业务逻辑，同时服务端响应能被客户端正确解密。

测试目标：

| 校验点         | 预期结果                    |
| -------------- | --------------------------- |
| 请求头完整     | 服务端校验通过              |
| 时间戳有效     | 服务端校验通过              |
| `nonce` 未使用 | 服务端校验通过并写入 Redis  |
| 签名正确       | 服务端校验通过              |
| 请求体可解密   | Controller 能接收到明文 DTO |
| 响应可解密     | 客户端能还原业务响应 JSON   |

测试请求：

```bash
curl -X POST "http://localhost:8080/api/auth/login" \
  -H "Content-Type: application/json" \
  -H "X-Crypto-Version: v1" \
  -H "X-Crypto-Key-Id: server-key-202605" \
  -H "X-Crypto-Timestamp: 1777996800000" \
  -H "X-Crypto-Nonce: normal_nonce_20260506" \
  -H "X-Crypto-Sign: ValidSignBase64Value" \
  -d '{
    "encryptedKey": "ValidEncryptedKeyBase64Value",
    "iv": "ValidRequestIvBase64Value",
    "data": "ValidEncryptedDataBase64Value"
  }'
```

预期响应：

```json
{
  "version": "v1",
  "algorithm": "AES-256-GCM",
  "keyId": "server-key-202605",
  "iv": "ResponseIvBase64Value",
  "timestamp": 1777996801000,
  "nonce": "responseNonceValue",
  "data": "EncryptedResponseDataBase64Value"
}
```

服务端日志示例：

```text
接口加密安全校验通过，uri=/api/auth/login，version=v1，keyId=server-key-202605
请求体解密完成，keyId=server-key-202605，targetType=io.github.atengk.demo.controller.AuthController$LoginDTO
用户登录请求，username=ateng
响应体加密完成，uri=/api/auth/login
```

### 参数篡改测试

参数篡改测试用于验证请求头或请求体中的关键字段被修改后，服务端能够通过签名校验或 AES-GCM 完整性校验发现异常。

常见篡改场景如下：

| 篡改位置             | 篡改方式                   | 预期结果                        |
| -------------------- | -------------------------- | ------------------------------- |
| `X-Crypto-Timestamp` | 修改时间戳但不重新签名     | 签名校验失败                    |
| `X-Crypto-Nonce`     | 修改随机字符串但不重新签名 | 签名校验失败                    |
| `encryptedKey`       | 修改加密后的 AES Key       | 签名校验失败或 RSA 解密失败     |
| `iv`                 | 修改 IV                    | 签名校验失败或 AES 解密失败     |
| `data`               | 修改密文数据               | 签名校验失败或 AES-GCM 解密失败 |
| `X-Crypto-Sign`      | 随机替换签名               | 签名校验失败                    |

篡改签名测试示例：

```bash
curl -X POST "http://localhost:8080/api/auth/login" \
  -H "Content-Type: application/json" \
  -H "X-Crypto-Version: v1" \
  -H "X-Crypto-Key-Id: server-key-202605" \
  -H "X-Crypto-Timestamp: 1777996800000" \
  -H "X-Crypto-Nonce: tamper_nonce_20260506" \
  -H "X-Crypto-Sign: InvalidSignBase64Value" \
  -d '{
    "encryptedKey": "ValidEncryptedKeyBase64Value",
    "iv": "ValidRequestIvBase64Value",
    "data": "ValidEncryptedDataBase64Value"
  }'
```

预期响应：

```json
{
  "success": false,
  "code": "CRYPTO_SIGN_INVALID",
  "message": "请求签名校验失败",
  "data": null,
  "timestamp": 1777996800000
}
```

篡改密文测试示例：

```json
{
  "encryptedKey": "ValidEncryptedKeyBase64Value",
  "iv": "ValidRequestIvBase64Value",
  "data": "TamperedEncryptedDataBase64Value"
}
```

如果攻击者只改 `data` 且不重新签名，服务端应在签名校验阶段失败。如果攻击者重新计算了签名但无法正确生成合法 AES-GCM 密文，则会在请求体解密阶段失败。

### 过期请求测试

过期请求测试用于验证时间戳有效期控制是否生效。客户端请求时间戳超过服务端允许偏差时，服务端应拒绝请求。

配置示例：

```yaml
interface-crypto:
  timestamp-expire-seconds: 300
```

测试方式：

| 场景                          | 示例               | 预期结果       |
| ----------------------------- | ------------------ | -------------- |
| 时间戳早于当前时间超过 300 秒 | 当前时间减 10 分钟 | 请求过期       |
| 时间戳晚于当前时间超过 300 秒 | 当前时间加 10 分钟 | 请求过期       |
| 时间戳不是数字                | `abc`              | 时间戳格式错误 |
| 时间戳为空                    | 不传请求头         | 请求头缺失     |

过期请求示例：

```bash
curl -X POST "http://localhost:8080/api/auth/login" \
  -H "Content-Type: application/json" \
  -H "X-Crypto-Version: v1" \
  -H "X-Crypto-Key-Id: server-key-202605" \
  -H "X-Crypto-Timestamp: 1777990000000" \
  -H "X-Crypto-Nonce: expired_nonce_20260506" \
  -H "X-Crypto-Sign: ValidSignBase64Value" \
  -d '{
    "encryptedKey": "ValidEncryptedKeyBase64Value",
    "iv": "ValidRequestIvBase64Value",
    "data": "ValidEncryptedDataBase64Value"
  }'
```

预期响应：

```json
{
  "success": false,
  "code": "CRYPTO_REQUEST_EXPIRED",
  "message": "请求已过期",
  "data": null,
  "timestamp": 1777996800000
}
```

### 重放请求测试

重放请求测试用于验证同一个 `keyId + nonce` 在有效期内只能使用一次。首次请求应正常通过，第二次使用完全相同的请求头和请求体再次提交时，应被服务端拒绝。

测试步骤：

| 步骤 | 操作                            | 预期结果                 |
| ---- | ------------------------------- | ------------------------ |
| 1    | 使用合法请求调用接口            | 请求成功                 |
| 2    | 在 300 秒内重复发送完全相同请求 | 请求失败                 |
| 3    | 查看 Redis 中的 `nonce` Key     | Key 存在且有过期时间     |
| 4    | 等待 Key 过期后再次发送         | 通常会因时间戳过期而失败 |

首次请求：

```bash
curl -X POST "http://localhost:8080/api/auth/login" \
  -H "Content-Type: application/json" \
  -H "X-Crypto-Version: v1" \
  -H "X-Crypto-Key-Id: server-key-202605" \
  -H "X-Crypto-Timestamp: 1777996800000" \
  -H "X-Crypto-Nonce: replay_nonce_20260506" \
  -H "X-Crypto-Sign: ValidSignBase64Value" \
  -d '{
    "encryptedKey": "ValidEncryptedKeyBase64Value",
    "iv": "ValidRequestIvBase64Value",
    "data": "ValidEncryptedDataBase64Value"
  }'
```

第二次重复请求：

```bash
curl -X POST "http://localhost:8080/api/auth/login" \
  -H "Content-Type: application/json" \
  -H "X-Crypto-Version: v1" \
  -H "X-Crypto-Key-Id: server-key-202605" \
  -H "X-Crypto-Timestamp: 1777996800000" \
  -H "X-Crypto-Nonce: replay_nonce_20260506" \
  -H "X-Crypto-Sign: ValidSignBase64Value" \
  -d '{
    "encryptedKey": "ValidEncryptedKeyBase64Value",
    "iv": "ValidRequestIvBase64Value",
    "data": "ValidEncryptedDataBase64Value"
  }'
```

预期响应：

```json
{
  "success": false,
  "code": "CRYPTO_REPLAY_REQUEST",
  "message": "请求重复提交",
  "data": null,
  "timestamp": 1777996800000
}
```

Redis 验证命令：

```bash
redis-cli GET interface-crypto:nonce:server-key-202605:replay_nonce_20260506
redis-cli TTL interface-crypto:nonce:server-key-202605:replay_nonce_20260506
```

命令说明：第一条命令用于确认 `nonce` 是否已被记录，第二条命令用于确认该记录是否设置了过期时间。TTL 应小于或等于 `timestamp-expire-seconds` 配置值。

## 使用说明

本章节说明后端和前端如何接入接口加密解密能力，以及联调过程中需要重点关注的调试事项。实际项目中建议将加解密能力封装为公共 starter 或基础模块，由业务服务统一依赖。

### 后端接入方式

后端接入接口加密解密能力时，需要完成依赖引入、配置密钥、启用组件、标注接口和验证调用五个步骤。

接入步骤如下：

| 步骤 | 说明                                                         |
| ---- | ------------------------------------------------------------ |
| 1    | 引入接口加密模块或复制核心代码到项目                         |
| 2    | 在 `application.yml` 中配置加密开关、密钥路径和签名密钥      |
| 3    | 确认 `CachedBodyFilter`、`CryptoValidateInterceptor`、`RequestBodyAdvice`、`ResponseBodyAdvice` 已被 Spring 扫描 |
| 4    | 在需要加密的接口上添加 `@DecryptRequest` 和 `@EncryptResponse` |
| 5    | 在不需要加密的接口上添加 `@CryptoIgnore` 或加入 `exclude-paths` |
| 6    | 使用加密客户端或测试脚本完成联调验证                         |

基础配置示例：

```yaml
interface-crypto:
  enabled: true
  default-request-decrypt: false
  default-response-encrypt: false
  encrypt-error-response: true
  default-key-id: server-key-202605
  timestamp-expire-seconds: 300
  aes-gcm-iv-length: 12
  aes-gcm-tag-length: 128
  exclude-paths:
    - /actuator/**
    - /api/health
    - /api/crypto/public-key
    - /swagger-ui/**
    - /v3/api-docs/**
  keys:
    server-key-202605:
      public-key-location: classpath:keys/server_public.pem
      private-key-location: classpath:keys/server_private.pem
      sign-secret: ${CRYPTO_SIGN_SECRET:change-me-sign-secret}
```

接口接入示例：

```java
package io.github.atengk.demo.controller;

import io.github.atengk.crypto.annotation.DecryptRequest;
import io.github.atengk.crypto.annotation.EncryptResponse;
import io.github.atengk.crypto.model.ApiResult;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 支付接口示例
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@RestController
@RequestMapping("/api/pay")
public class PayController {

    /**
     * 创建支付单
     *
     * @param dto 创建支付请求
     * @return 支付单信息
     */
    @DecryptRequest
    @EncryptResponse
    @PostMapping("/create")
    public ApiResult<PayCreateVO> create(@RequestBody PayCreateDTO dto) {
        log.info("创建支付单，orderNo={}，amount={}", dto.getOrderNo(), dto.getAmount());

        PayCreateVO vo = new PayCreateVO();
        vo.setPayNo("PAY202605060001");
        vo.setStatus("WAIT_PAY");
        return ApiResult.success(vo);
    }

    /**
     * 创建支付请求
     *
     * @author Ateng
     * @since 2026-05-06
     */
    @Data
    public static class PayCreateDTO {

        /**
         * 订单号
         */
        private String orderNo;

        /**
         * 金额，单位分
         */
        private Long amount;
    }

    /**
     * 创建支付响应
     *
     * @author Ateng
     * @since 2026-05-06
     */
    @Data
    public static class PayCreateVO {

        /**
         * 支付单号
         */
        private String payNo;

        /**
         * 支付状态
         */
        private String status;
    }
}
```

后端接入注意事项：

| 注意事项                     | 说明                                           |
| ---------------------------- | ---------------------------------------------- |
| 私钥不得提交到 Git           | 生产环境使用环境变量、挂载文件、配置中心或 KMS |
| `sign-secret` 不得使用默认值 | 默认值只允许本地开发使用                       |
| 默认不建议全局加密           | 推荐通过注解逐个接口灰度接入                   |
| 文件接口不要统一加密         | 文件上传下载建议单独设计加密方案               |
| Redis 必须可用               | 防重放依赖 Redis，生产环境必须验证 Redis 连接  |
| 日志不要打印明文敏感数据     | 登录密码、证件号、银行卡号等不得打印           |

### 前端对接规则

前端对接接口加密解密能力时，建议封装统一请求客户端，例如 Axios 拦截器、App 网络层 SDK、小程序请求封装或第三方调用 SDK。业务页面不应直接处理加密细节。

前端请求封装需要完成以下事情：

| 步骤 | 说明                                  |
| ---- | ------------------------------------- |
| 1    | 判断当前接口是否需要加密              |
| 2    | 获取服务端公钥和 `keyId`              |
| 3    | 生成 AES 会话密钥                     |
| 4    | 生成请求 IV                           |
| 5    | 使用 AES-GCM 加密业务 JSON            |
| 6    | 使用 RSA-OAEP 加密 AES Key            |
| 7    | 生成时间戳和 `nonce`                  |
| 8    | 按固定规则生成签名                    |
| 9    | 发送密文请求体和加密请求头            |
| 10   | 收到响应后使用请求 AES Key 解密响应体 |

前端接口配置示例：

```javascript
const cryptoApiRules = {
  "/api/auth/login": {
    requestEncrypt: true,
    responseDecrypt: true
  },
  "/api/pay/create": {
    requestEncrypt: true,
    responseDecrypt: true
  },
  "/api/crypto/public-key": {
    requestEncrypt: false,
    responseDecrypt: false
  },
  "/api/health": {
    requestEncrypt: false,
    responseDecrypt: false
  }
};
```

前端发送请求时，应把本次请求生成的 AES Key 与请求上下文绑定，响应返回后再使用同一个 AES Key 解密响应 `data`。不要在多个请求之间复用同一个 AES Key。

前端签名原文必须与服务端保持一致：

```text
HTTP_METHOD + "\n" +
REQUEST_PATH + "\n" +
TIMESTAMP + "\n" +
NONCE + "\n" +
KEY_ID + "\n" +
ENCRYPTED_KEY + "\n" +
IV + "\n" +
DATA
```

前端对接注意事项：

| 注意事项                        | 说明                                    |
| ------------------------------- | --------------------------------------- |
| AES-GCM IV 每次请求必须重新生成 | 不允许固定 IV                           |
| AES Key 每次请求建议重新生成    | 降低密钥复用风险                        |
| 时间戳使用毫秒值                | 与服务端校验逻辑保持一致                |
| `nonce` 每次请求唯一            | 建议使用安全随机字符串                  |
| Base64 编码保持一致             | 普通 Base64 与 URL Safe Base64 不要混用 |
| 签名字段顺序不能变              | 顺序、换行符、编码都要一致              |
| 响应解密使用响应 IV             | 不使用请求 IV 解密响应                  |
| 加密失败统一进入异常处理        | 不要继续发送半加密请求                  |

### 调试注意事项

调试接口加密解密时，问题通常集中在签名不一致、Base64 编码不一致、时间戳过期、请求体被读取多次、AES-GCM 参数不一致和前后端密钥不匹配。

常见问题和排查方式如下：

| 问题                       | 常见原因                       | 排查方式                                 |
| -------------------------- | ------------------------------ | ---------------------------------------- |
| `CRYPTO_HEADER_MISSING`    | 请求头缺失或名称不一致         | 检查 `X-Crypto-*` 请求头是否完整         |
| `CRYPTO_REQUEST_EXPIRED`   | 客户端时间不准或时间戳单位错误 | 确认传的是毫秒时间戳，检查客户端系统时间 |
| `CRYPTO_REPLAY_REQUEST`    | 重复使用同一个 `nonce`         | 每次请求重新生成 `nonce`，检查 Redis Key |
| `CRYPTO_SIGN_INVALID`      | 签名原文不一致                 | 打印前后端签名原文，对比每一行           |
| `CRYPTO_BODY_INVALID`      | 请求体字段缺失或 JSON 格式错误 | 检查 `encryptedKey`、`iv`、`data`        |
| `CRYPTO_DECRYPT_FAILED`    | AES Key、IV、密文或算法不一致  | 检查 AES-GCM 参数、RSA 填充方式、Base64  |
| 响应无法解密               | 前端没有保存请求 AES Key       | 确认响应解密使用本次请求 AES Key         |
| Controller 收不到参数      | 请求体没有正确替换为明文 JSON  | 检查 `RequestBodyAdvice` 是否生效        |
| 拦截器读取 Body 后解密失败 | 请求体没有缓存                 | 检查 `CachedBodyFilter` 是否最先执行     |

建议联调时临时开启 debug 日志，但不要打印明文密码、Token、身份证号、银行卡号等敏感数据。

日志配置示例：

```yaml
logging:
  level:
    io.github.atengk.crypto: debug
```

调试签名不一致时，建议仅在开发环境打印签名原文摘要，不直接打印完整密文和敏感字段。

示例日志建议：

```text
接口加密安全校验开始，uri=/api/auth/login，keyId=server-key-202605
接口加密签名校验失败，uri=/api/auth/login，keyId=server-key-202605，nonce=3f7f2c2d9d874cbf
请求体解密完成，keyId=server-key-202605，targetType=io.github.atengk.demo.controller.AuthController$LoginDTO
响应体加密完成，uri=/api/auth/login
```

本地调试建议流程：

```text
先关闭响应加密，只验证请求解密
  -> 请求解密通过后，再开启响应加密
  -> 响应加密通过后，再开启签名校验
  -> 签名校验通过后，再开启 nonce 防重放
  -> 最后按生产配置完整验证
```

生产上线前检查清单：

| 检查项                       | 是否必须 |
| ---------------------------- | -------- |
| HTTPS 已开启                 | 必须     |
| 私钥未提交到代码仓库         | 必须     |
| `sign-secret` 已使用生产密钥 | 必须     |
| Redis 防重放可用             | 必须     |
| 健康检查和接口文档已排除     | 必须     |
| 文件上传下载接口未误加密     | 必须     |
| 错误码和前端异常处理已对齐   | 必须     |
| 前后端签名原文规则已固化     | 必须     |
| 密钥轮换方案已确认           | 建议     |
| 加密接口已完成压测           | 建议     |

接口加密解密能力上线时，建议按接口灰度接入：先选择登录、支付、用户隐私查询等高敏感接口启用，再逐步扩展到其他接口。不要一次性对所有接口开启全局加密，否则容易影响文档接口、文件接口、监控接口和第三方回调接口。