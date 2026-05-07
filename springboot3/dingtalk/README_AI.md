# Spring Boot 钉钉开发

本文档用于说明如何基于 Spring Boot 3 集成钉钉开放平台能力，覆盖应用创建、权限配置、回调地址配置、项目环境准备等前置工作，为后续实现 AccessToken 获取、用户体系集成、消息通知、审批流程、事件回调等功能提供基础。

## 项目概述

本章节用于说明 Spring Boot 3 集成钉钉开放平台的整体目标、功能边界和适用业务场景。通过本项目，可以将企业内部系统与钉钉的组织架构、用户身份、消息通知、审批流程和事件回调能力进行集成，形成统一的钉钉业务接入模块。

### 功能定位

本项目定位为企业内部系统与钉钉开放平台之间的后端集成服务，主要负责钉钉接口调用、应用鉴权、用户数据同步、消息发送、审批流程对接和事件回调处理。

在系统架构中，Spring Boot 3 服务作为业务系统与钉钉平台之间的适配层，对外为业务模块提供统一的服务接口，对内封装钉钉开放平台的 AccessToken 获取、OpenAPI 调用、请求签名、事件解密、异常处理和日志记录等通用能力。

主要功能包括：

| 功能模块     | 功能说明                                                   |
| ------------ | ---------------------------------------------------------- |
| 应用鉴权     | 根据钉钉应用参数获取 AccessToken，并进行缓存和自动刷新     |
| 用户体系集成 | 获取钉钉用户信息、部门信息，并与本地系统账号建立绑定关系   |
| 消息通知     | 支持发送工作通知、群机器人消息、审批提醒等业务消息         |
| 审批流程集成 | 支持创建审批实例、查询审批状态、处理审批结果回调           |
| 事件回调处理 | 接收并处理钉钉推送的用户、部门、审批、机器人等事件         |
| 接口统一封装 | 对钉钉 API 请求、响应、异常、重试、日志进行统一处理        |
| 数据落库记录 | 保存用户绑定关系、消息发送记录、审批关联关系和回调事件日志 |

该模块不直接承载具体业务规则，而是为业务系统提供稳定、统一、可复用的钉钉能力接入基础。

### 适用场景

本项目适用于企业内部系统需要接入钉钉生态的场景，尤其适合 OA、ERP、CRM、工单系统、审批系统、项目管理系统、运维告警系统等业务平台。

常见适用场景包括：

| 场景         | 说明                                                         |
| ------------ | ------------------------------------------------------------ |
| 钉钉免登接入 | 用户从钉钉工作台进入业务系统时，使用钉钉身份完成登录         |
| 组织架构同步 | 将钉钉部门、用户信息同步到本地系统，用于权限、流程和人员管理 |
| 业务消息通知 | 将订单、工单、审批、告警等业务消息发送到钉钉                 |
| 审批流程对接 | 业务系统发起钉钉审批，并根据审批状态推进本地业务流程         |
| 事件驱动处理 | 监听钉钉用户变更、部门变更、审批状态变更等事件               |
| 群机器人通知 | 通过钉钉群机器人向群聊推送业务提醒或系统告警                 |
| 统一平台入口 | 将企业内部系统作为钉钉工作台应用入口，提升使用便利性         |

在实际项目中，可以根据业务复杂度选择部分能力接入。例如，轻量级系统可能只需要消息通知能力；审批类系统通常需要同时接入用户体系、审批实例创建、审批状态查询和审批回调处理。

## 开发准备

本章节用于说明正式开发前需要完成的准备工作，包括钉钉开放平台应用创建、接口权限配置、回调地址配置和 Spring Boot 3 项目环境准备。这些配置是后续获取 AccessToken、调用钉钉接口、接收回调事件的基础。

### 钉钉开放平台应用创建

钉钉开放平台应用是后端服务调用钉钉 OpenAPI 的身份主体。创建应用后，系统才能获取应用凭证，并基于该应用申请接口权限、配置回调地址和发布到企业工作台。

创建应用的一般流程如下：

1. 使用企业管理员或具备开发权限的账号登录钉钉开放平台。
2. 进入开发者后台，选择企业内部应用。
3. 创建新的企业内部应用。
4. 填写应用名称、应用描述、应用图标等基础信息。
5. 创建完成后，进入应用详情页面。
6. 在应用基础信息页面获取应用凭证。
7. 保存后端服务需要使用的应用参数。

需要重点记录的参数如下：

| 参数            | 说明                                          | 后端配置建议             |
| --------------- | --------------------------------------------- | ------------------------ |
| `Client ID`     | 钉钉应用的身份标识，通常用于新版 OpenAPI 调用 | `dingtalk.client-id`     |
| `Client Secret` | 钉钉应用密钥，用于获取访问凭证                | `dingtalk.client-secret` |
| `AgentId`       | 企业内部应用 ID，发送工作通知时通常需要       | `dingtalk.agent-id`      |
| `CorpId`        | 企业 ID，部分企业级接口或回调场景可能需要     | `dingtalk.corp-id`       |
| `AppKey`        | 旧版应用标识，部分历史接口或 SDK 中可能使用   | `dingtalk.app-key`       |
| `AppSecret`     | 旧版应用密钥，部分历史接口或 SDK 中可能使用   | `dingtalk.app-secret`    |

应用参数属于敏感信息，禁止硬编码在 Java 类中。开发环境可以放在本地配置文件或环境变量中，测试环境和生产环境建议通过配置中心、密钥管理服务或容器环境变量注入。

推荐配置方式如下：

| 环境         | 配置方式                             |
| ------------ | ------------------------------------ |
| 本地开发环境 | `application-dev.yml` 或本地环境变量 |
| 测试环境     | 配置中心或服务器环境变量             |
| 生产环境     | 配置中心、密钥管理服务或容器 Secret  |
| CI/CD 环境   | 构建平台密钥变量或部署平台 Secret    |

### 应用权限配置

钉钉应用默认不具备调用所有开放接口的权限。开发前需要根据业务功能申请对应权限，并由企业管理员完成授权。否则即使应用凭证正确，接口调用时也可能返回无权限错误。

权限配置的一般流程如下：

1. 进入钉钉开放平台应用详情。
2. 打开权限管理页面。
3. 根据业务模块选择需要的权限点。
4. 提交权限申请。
5. 由企业管理员审核并授权。
6. 授权完成后进行接口联调验证。

常见业务能力与权限方向如下：

| 业务能力       | 权限方向                         |
| -------------- | -------------------------------- |
| 获取用户信息   | 用户基础信息读取权限             |
| 获取部门信息   | 通讯录部门读取权限               |
| 获取部门用户   | 通讯录成员读取权限               |
| 发送工作通知   | 工作通知消息发送权限             |
| 发起审批       | 审批实例创建权限                 |
| 查询审批状态   | 审批实例读取权限                 |
| 接收审批回调   | 审批事件订阅权限                 |
| 接收通讯录变更 | 用户、部门、组织架构事件订阅权限 |
| 钉钉免登       | 登录授权、用户身份识别相关权限   |

权限配置时需要注意应用调用和用户授权调用的区别。应用调用通常使用应用身份访问企业资源，适合后端定时同步、消息通知、审批发起等场景；用户授权调用通常代表当前登录用户访问资源，适合免登、获取当前用户信息等场景。

建议在项目中维护一份权限清单，记录每个业务功能依赖的钉钉权限，便于上线前检查和问题排查。

示例权限清单如下：

| 本系统功能   | 依赖钉钉能力     | 是否必需 | 说明               |
| ------------ | ---------------- | -------- | ------------------ |
| 钉钉用户登录 | 获取用户基础信息 | 是       | 用于识别当前登录人 |
| 通讯录同步   | 读取部门和成员   | 是       | 用于同步组织架构   |
| 工作通知发送 | 发送工作通知     | 是       | 用于业务消息触达   |
| 审批发起     | 创建审批实例     | 按需     | 仅审批业务需要     |
| 审批状态同步 | 查询审批实例     | 按需     | 用于同步审批进度   |
| 审批回调处理 | 订阅审批事件     | 按需     | 用于接收审批结果   |
| 群机器人通知 | 群机器人消息     | 按需     | 用于群聊通知和告警 |

### 回调地址配置

回调地址用于接收钉钉平台主动推送的事件数据，例如用户变更、部门变更、审批状态变更、机器人消息、互动卡片事件等。后端服务需要提供稳定的接口地址，并根据钉钉规则完成签名校验、消息解密和事件分发。

如果使用 HTTP 回调模式，服务地址必须能够被钉钉服务器访问。生产环境建议使用 HTTPS 域名，开发环境可以通过内网穿透工具将本地服务临时暴露到公网。

推荐回调地址格式如下：

```text
https://api.example.com/api/dingtalk/callback/event
```

后端接口路径建议按回调类型拆分，便于后续维护：

| 回调类型       | 建议接口路径                      | 说明                           |
| -------------- | --------------------------------- | ------------------------------ |
| 通用事件回调   | `/api/dingtalk/callback/event`    | 接收用户、部门、组织等通用事件 |
| 审批事件回调   | `/api/dingtalk/callback/approval` | 接收审批实例状态变更事件       |
| 机器人消息回调 | `/api/dingtalk/callback/robot`    | 接收群机器人消息               |
| 互动卡片回调   | `/api/dingtalk/callback/card`     | 接收卡片按钮点击等交互事件     |
| 回调健康检查   | `/api/dingtalk/callback/health`   | 用于服务探活和部署验证         |

回调配置中通常需要填写以下参数：

| 参数     | 说明                       | 配置建议                 |
| -------- | -------------------------- | ------------------------ |
| 回调 URL | 钉钉推送事件的后端接口地址 | 使用 HTTPS 公网地址      |
| Token    | 用于回调签名校验           | 使用随机字符串，避免泄露 |
| AES Key  | 用于回调消息加解密         | 按钉钉要求生成并妥善保存 |
| 事件类型 | 需要订阅的事件范围         | 只订阅业务实际需要的事件 |
| 推送方式 | HTTP 回调或 Stream 模式    | 根据部署环境选择         |

回调地址配置完成后，需要重点检查以下内容：

1. 回调地址可以被公网访问。
2. HTTPS 证书有效。
3. 网关、负载均衡、防火墙没有拦截钉钉请求。
4. 后端接口支持 `POST` 请求。
5. 请求参数、请求头和请求体可以被完整读取。
6. Token、AES Key、CorpId 等配置与钉钉后台保持一致。
7. 接口处理成功后能够按钉钉要求返回成功响应。
8. 回调处理逻辑具备幂等能力，避免重复事件导致业务重复执行。

本地开发阶段如果不方便提供公网地址，可以优先使用内网穿透工具调试 HTTP 回调；如果项目采用 Stream 模式，则可以减少公网回调地址配置和网络暴露成本。

### Spring Boot 3 项目环境

Spring Boot 3 项目是钉钉集成服务的后端基础。开发前需要准备 JDK、构建工具、基础依赖、配置文件、日志配置和统一包结构，确保后续模块能够按清晰分层扩展。

推荐开发环境如下：

| 环境项       | 推荐版本                   | 说明                                      |
| ------------ | -------------------------- | ----------------------------------------- |
| JDK          | 17 或 21                   | Spring Boot 3 要求使用 Java 17 及以上版本 |
| Spring Boot  | 3.x                        | 建议使用项目统一版本                      |
| Maven        | 3.8+                       | 用于依赖管理、编译和打包                  |
| 数据库       | MySQL 8.x / PostgreSQL 15+ | 用于保存业务关联数据和日志                |
| Redis        | 6.x / 7.x                  | 用于缓存 AccessToken、幂等标识和分布式锁  |
| IDE          | IntelliJ IDEA              | 推荐用于 Spring Boot 开发                 |
| 接口调试工具 | Apifox / Postman / curl    | 用于调试后端接口和钉钉回调                |
| 内网穿透工具 | frp / ngrok / cpolar       | 本地调试 HTTP 回调时使用                  |

推荐项目结构如下：

```text
dingtalk-springboot3-demo
├── pom.xml
├── src
│   ├── main
│   │   ├── java
│   │   │   └── io/github/atengk/dingtalk
│   │   │       ├── DingtalkApplication.java
│   │   │       ├── config
│   │   │       ├── controller
│   │   │       ├── service
│   │   │       ├── service/impl
│   │   │       ├── client
│   │   │       ├── callback
│   │   │       ├── domain
│   │   │       └── common
│   │   └── resources
│   │       ├── application.yml
│   │       └── logback-spring.xml
│   └── test
│       └── java
└── README.md
```

基础依赖建议包括以下类型：

| 依赖类型                            | 作用                                          |
| ----------------------------------- | --------------------------------------------- |
| `spring-boot-starter-web`           | 提供 REST 接口和回调接口能力                  |
| `spring-boot-starter-validation`    | 提供请求参数校验能力                          |
| `spring-boot-starter-aop`           | 支持日志、耗时统计、异常增强等横切逻辑        |
| `spring-boot-starter-cache`         | 支持 AccessToken 等数据缓存                   |
| `spring-boot-starter-data-redis`    | 用于 Redis 缓存、幂等和分布式锁               |
| `hutool-all`                        | 简化字符串、JSON、日期、加密、HTTP 等工具处理 |
| `lombok`                            | 减少实体类、配置类、DTO 的样板代码            |
| `mybatis-plus-spring-boot3-starter` | 用于业务数据表的 CRUD 操作                    |
| `spring-boot-starter-test`          | 用于单元测试和接口测试                        |

基础配置文件建议提前规划以下配置项：

```yaml
server:
  port: 8080

spring:
  application:
    name: dingtalk-springboot3-demo

dingtalk:
  corp-id: ${DINGTALK_CORP_ID:}
  client-id: ${DINGTALK_CLIENT_ID:}
  client-secret: ${DINGTALK_CLIENT_SECRET:}
  agent-id: ${DINGTALK_AGENT_ID:}
  callback:
    token: ${DINGTALK_CALLBACK_TOKEN:}
    aes-key: ${DINGTALK_CALLBACK_AES_KEY:}
    url: ${DINGTALK_CALLBACK_URL:}
```

其中，`client-secret`、`callback.token`、`callback.aes-key` 等参数属于敏感配置，生产环境必须通过环境变量、配置中心或密钥管理服务注入，不应提交到代码仓库。

开发准备完成后，建议按以下顺序进行验证：

1. Spring Boot 项目可以正常启动。
2. 配置文件能够正确读取钉钉应用参数。
3. 后端日志中不打印明文密钥。
4. 本地接口可以正常访问。
5. 测试环境具备公网回调地址或 Stream 连接能力。
6. 钉钉开放平台应用权限已完成授权。
7. 后续可以继续开发 AccessToken 获取、用户信息读取、消息发送和回调事件处理。



## 基础配置

本章节用于完成 Spring Boot 3 项目接入钉钉开放平台前的基础配置，包括 Maven 依赖、应用参数、配置类和核心 Bean 设计。后续 AccessToken 获取、消息发送、审批集成和事件回调都会基于本章节中的配置进行扩展。

### Maven 依赖配置

Maven 依赖用于引入 Web、参数校验、Redis 缓存、配置元数据、Hutool 工具类和 Lombok 等基础能力。Spring Boot 3 要求使用 Java 17 及以上版本，Spring Boot 3.3.x 对 Maven 的明确构建支持为 3.6.3 或更高版本，因此项目环境建议统一使用 JDK 17+ 和 Maven 3.6.3+。([Spring Enterprise 文档](https://docs.enterprise.spring.io/spring-boot/system-requirements.html?utm_source=chatgpt.com))

文件位置：`pom.xml`

```xml
<properties>
    <!-- Spring Boot 3 最低要求 Java 17 -->
    <java.version>17</java.version>
    <hutool.version>5.8.35</hutool.version>
</properties>

<dependencies>
    <!-- Web 接口开发，用于提供钉钉回调接口和内部业务接口 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- 参数校验，用于校验钉钉配置、接口请求参数和业务 DTO -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- Redis 支持，用于缓存 AccessToken、幂等标识和分布式锁 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>

    <!-- Spring Cache 抽象，可用于统一封装缓存逻辑 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-cache</artifactId>
    </dependency>

    <!-- 配置元数据支持，便于 IDE 提示自定义配置项 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-configuration-processor</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- Hutool 工具类，简化字符串、JSON、HTTP、日期、加密等处理 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>${hutool.version}</version>
    </dependency>

    <!-- Lombok，减少实体类、配置类、DTO 的样板代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- 单元测试和接口测试基础依赖 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

如果项目后续需要将钉钉用户、消息发送记录、审批实例关联关系和回调事件日志保存到数据库，可以继续引入 MyBatis-Plus 和数据库驱动。

```xml
<!-- MyBatis-Plus Spring Boot 3 支持，用于业务表 CRUD -->
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
    <version>3.5.9</version>
</dependency>

<!-- MySQL 驱动，根据实际数据库类型选择 -->
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <scope>runtime</scope>
</dependency>
```

依赖配置完成后，执行以下命令验证依赖是否能够正常解析和编译。

```bash
mvn clean compile
```

该命令会清理历史编译产物并重新编译项目。如果 Maven 依赖版本冲突或 JDK 版本不符合 Spring Boot 3 要求，会在该阶段暴露问题。

### 钉钉应用参数配置

钉钉应用参数用于标识当前后端服务调用的是哪个钉钉应用。AccessToken 获取、工作通知发送、审批实例创建和事件回调解密都依赖这些参数。

钉钉新版应用凭证通常使用 `ClientId` 和 `ClientSecret` 表示应用身份，旧版文档或部分接口中也可能出现 `AppKey` 和 `AppSecret`。钉钉开发者百科说明，ClientID/ClientSecret 是应用在开放平台上的凭证，其中 ClientSecret 属于需要严格保密的凭证信息。([opensource.dingtalk.com](https://opensource.dingtalk.com/developerpedia/docs/learn/permission/intro/permission-glossary/?utm_source=chatgpt.com))

文件位置：`src/main/resources/application.yml`

```yaml
server:
  port: 8080

spring:
  application:
    name: dingtalk-springboot3-demo

  data:
    redis:
      # Redis 地址，用于缓存 AccessToken
      host: ${REDIS_HOST:127.0.0.1}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
      database: ${REDIS_DATABASE:0}
      timeout: 5s

dingtalk:
  # 是否启用钉钉集成
  enabled: ${DINGTALK_ENABLED:true}

  # 钉钉企业 ID，推荐在新版 AccessToken 获取、回调和多组织场景中配置
  corp-id: ${DINGTALK_CORP_ID:}

  # 钉钉应用 Client ID，可理解为新版应用标识
  client-id: ${DINGTALK_CLIENT_ID:}

  # 钉钉应用 Client Secret，生产环境禁止明文提交到代码仓库
  client-secret: ${DINGTALK_CLIENT_SECRET:}

  # 企业内部应用 AgentId，发送工作通知时通常需要
  agent-id: ${DINGTALK_AGENT_ID:}

  token:
    # AccessToken Redis 缓存 Key 前缀
    cache-prefix: dingtalk:access-token

    # AccessToken 提前刷新秒数，避免临近过期时接口调用失败
    refresh-before-seconds: 300

    # AccessToken 自动刷新间隔，单位毫秒
    refresh-fixed-delay: 300000

  callback:
    # 回调签名校验 Token
    token: ${DINGTALK_CALLBACK_TOKEN:}

    # 回调消息解密 AES Key
    aes-key: ${DINGTALK_CALLBACK_AES_KEY:}
```

配置项说明如下：

| 配置项                                  | 说明                                                |
| --------------------------------------- | --------------------------------------------------- |
| `dingtalk.enabled`                      | 是否启用钉钉集成，便于本地或特殊环境关闭            |
| `dingtalk.corp-id`                      | 企业 ID，推荐在新版应用访问凭证获取和回调场景中配置 |
| `dingtalk.client-id`                    | 应用 Client ID，用于标识钉钉应用                    |
| `dingtalk.client-secret`                | 应用 Client Secret，用于获取访问凭证                |
| `dingtalk.agent-id`                     | 企业内部应用 AgentId，发送工作通知时使用            |
| `dingtalk.token.cache-prefix`           | AccessToken 缓存 Key 前缀                           |
| `dingtalk.token.refresh-before-seconds` | 提前刷新时间，避免 token 临界过期                   |
| `dingtalk.token.refresh-fixed-delay`    | 定时刷新任务执行间隔                                |
| `dingtalk.callback.token`               | 回调签名校验参数                                    |
| `dingtalk.callback.aes-key`             | 回调消息解密密钥                                    |

生产环境建议通过环境变量、配置中心或容器 Secret 注入钉钉密钥，避免在 Git 仓库中出现真实密钥。

```bash
export DINGTALK_CORP_ID="dingxxxxxxxxxxxx"
export DINGTALK_CLIENT_ID="dingxxxxxxxxxxxx"
export DINGTALK_CLIENT_SECRET="xxxxxxxxxxxxxxxx"
export DINGTALK_AGENT_ID="123456789"
```

### 配置类设计

配置类用于将 `application.yml` 中的钉钉参数绑定为 Java 对象，便于后续 Service、Client、定时任务和回调处理组件统一读取。

建议将钉钉配置拆分为主配置、Token 配置和 Callback 配置。这样可以避免单个配置类过大，也便于后续扩展消息通知、审批流程等模块。

推荐文件结构如下：

```text
src/main/java/io/github/atengk/dingtalk
├── config
│   └── DingtalkProperties.java
├── token
│   ├── DingtalkTokenClient.java
│   ├── DingtalkTokenService.java
│   ├── DingtalkTokenServiceImpl.java
│   ├── DingtalkAccessTokenResponse.java
│   └── DingtalkTokenRefreshTask.java
```

下面的配置类用于绑定 `dingtalk` 前缀下的所有配置项，并提供基础参数校验。

文件位置：`src/main/java/io/github/atengk/dingtalk/config/DingtalkProperties.java`

```java
package io.github.atengk.dingtalk.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 钉钉应用配置属性
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Data
@Validated
@ConfigurationProperties(prefix = "dingtalk")
public class DingtalkProperties {

    /**
     * 是否启用钉钉集成
     */
    private Boolean enabled = Boolean.TRUE;

    /**
     * 钉钉企业 ID
     */
    @NotBlank(message = "钉钉企业 ID 不能为空")
    private String corpId;

    /**
     * 钉钉应用 Client ID
     */
    @NotBlank(message = "钉钉应用 Client ID 不能为空")
    private String clientId;

    /**
     * 钉钉应用 Client Secret
     */
    @NotBlank(message = "钉钉应用 Client Secret 不能为空")
    private String clientSecret;

    /**
     * 钉钉企业内部应用 AgentId
     */
    @NotBlank(message = "钉钉应用 AgentId 不能为空")
    private String agentId;

    /**
     * AccessToken 配置
     */
    @Valid
    private Token token = new Token();

    /**
     * 回调配置
     */
    @Valid
    private Callback callback = new Callback();

    /**
     * AccessToken 配置项
     *
     * @author Ateng
     * @since 2026-05-07
     */
    @Data
    public static class Token {

        /**
         * AccessToken 缓存 Key 前缀
         */
        @NotBlank(message = "AccessToken 缓存 Key 前缀不能为空")
        private String cachePrefix = "dingtalk:access-token";

        /**
         * 提前刷新秒数
         */
        @Min(value = 60, message = "AccessToken 提前刷新秒数不能小于 60 秒")
        private Integer refreshBeforeSeconds = 300;

        /**
         * 自动刷新间隔，单位毫秒
         */
        @Min(value = 60000, message = "AccessToken 自动刷新间隔不能小于 60000 毫秒")
        private Long refreshFixedDelay = 300000L;

    }

    /**
     * 钉钉回调配置项
     *
     * @author Ateng
     * @since 2026-05-07
     */
    @Data
    public static class Callback {

        /**
         * 回调签名校验 Token
         */
        private String token;

        /**
         * 回调消息解密 AES Key
         */
        private String aesKey;

    }

}
```

还需要在启动类或配置类中启用配置属性绑定。

文件位置：`src/main/java/io/github/atengk/dingtalk/DingtalkApplication.java`

```java
package io.github.atengk.dingtalk;

import io.github.atengk.dingtalk.config.DingtalkProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * 钉钉集成服务启动类
 *
 * @author Ateng
 * @since 2026-05-07
 */
@SpringBootApplication
@EnableConfigurationProperties(DingtalkProperties.class)
public class DingtalkApplication {

    /**
     * 启动 Spring Boot 应用
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(DingtalkApplication.class, args);
    }

}
```

配置类设计完成后，项目启动时会自动读取 `dingtalk` 配置。如果必填参数为空，应用会在启动阶段报错，避免运行时调用钉钉接口才发现配置缺失。

## 鉴权与访问凭证

本章节用于实现钉钉 AccessToken 的获取、缓存和自动刷新。AccessToken 是后端调用钉钉服务端 OpenAPI 的核心凭证，消息发送、用户查询、审批流程等接口都需要携带有效的 AccessToken。

钉钉官方文档说明，企业内部应用获取 AccessToken 后用于服务端 API 鉴权，AccessToken 有效期通常为 7200 秒，并且开发者需要缓存 AccessToken，避免频繁调用获取接口。([dingtalk.apifox.cn](https://dingtalk.apifox.cn/api-139698997?utm_source=chatgpt.com))

### AccessToken 获取

AccessToken 获取逻辑建议单独封装为 Client 层，专门负责调用钉钉开放平台接口。Service 层不直接拼接 HTTP 请求，而是调用 Client 层获取响应结果，便于后续统一处理异常、日志和重试。

钉钉当前常见的企业内部应用 AccessToken 获取接口为 `POST https://api.dingtalk.com/v1.0/oauth2/accessToken`，请求体包含应用标识和应用密钥，成功后返回 `accessToken` 和 `expireIn`。([dingtalk.apifox.cn](https://dingtalk.apifox.cn/api-139698997?utm_source=chatgpt.com))

下面的响应对象用于接收钉钉 AccessToken 接口返回结果。

文件位置：`src/main/java/io/github/atengk/dingtalk/token/DingtalkAccessTokenResponse.java`

```java
package io.github.atengk.dingtalk.token;

import lombok.Data;

/**
 * 钉钉 AccessToken 响应对象
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Data
public class DingtalkAccessTokenResponse {

    /**
     * 访问凭证
     */
    private String accessToken;

    /**
     * 有效期，单位秒
     */
    private Long expireIn;

}
```

下面的 Client 使用 Hutool `HttpRequest` 调用钉钉接口，并使用 Hutool `JSONUtil` 处理 JSON 请求和响应。

文件位置：`src/main/java/io/github/atengk/dingtalk/token/DingtalkTokenClient.java`

```java
package io.github.atengk.dingtalk.token;

import cn.hutool.core.lang.Assert;
import cn.hutool.http.ContentType;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import io.github.atengk.dingtalk.config.DingtalkProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 钉钉 AccessToken 客户端
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DingtalkTokenClient {

    private static final String ACCESS_TOKEN_URL = "https://api.dingtalk.com/v1.0/oauth2/accessToken";

    private final DingtalkProperties dingtalkProperties;

    /**
     * 从钉钉开放平台获取 AccessToken
     *
     * @return AccessToken 响应
     */
    public DingtalkAccessTokenResponse fetchAccessToken() {
        JSONObject requestBody = JSONUtil.createObj()
                .set("appKey", dingtalkProperties.getClientId())
                .set("appSecret", dingtalkProperties.getClientSecret());

        log.info("开始获取钉钉 AccessToken，clientId={}", dingtalkProperties.getClientId());

        String responseBody = HttpRequest.post(ACCESS_TOKEN_URL)
                .contentType(ContentType.JSON.getValue())
                .body(requestBody.toString())
                .timeout(10000)
                .execute()
                .body();

        log.info("钉钉 AccessToken 接口调用完成");

        JSONObject responseJson = JSONUtil.parseObj(responseBody);
        String accessToken = responseJson.getStr("accessToken");
        Long expireIn = responseJson.getLong("expireIn");

        Assert.notBlank(accessToken, "钉钉 AccessToken 不能为空");
        Assert.notNull(expireIn, "钉钉 AccessToken 过期时间不能为空");

        DingtalkAccessTokenResponse response = new DingtalkAccessTokenResponse();
        response.setAccessToken(accessToken);
        response.setExpireIn(expireIn);
        return response;
    }

}
```

如果项目采用新版“按组织获取应用 AccessToken”的方式，也可以将接口地址调整为以下格式：`POST https://api.dingtalk.com/v1.0/oauth2/{corpId}/token`，请求体使用 `client_id`、`client_secret` 和 `grant_type=client_credentials`。钉钉开发者百科标注该方式为推荐的新文档，用于支持应用从单组织到多组织的快速切换。([open-dingtalk.github.io](https://open-dingtalk.github.io/developerpedia/docs/develop/permission/single_to_multi/new_get_app_token?utm_source=chatgpt.com))

### AccessToken 缓存

AccessToken 不应该每次调用业务接口时都重新获取。正确做法是先从 Redis 读取缓存，缓存不存在时再调用钉钉接口获取，并将结果写入 Redis。

缓存 Key 建议包含应用标识或企业 ID，避免多应用、多企业场景下 AccessToken 混用。

缓存 Key 示例：

```text
dingtalk:access-token:dingxxxxxxxxxxxx:dingyyyyyyyyyyyy
```

下面的 Service 接口定义 AccessToken 的读取、刷新和缓存删除能力。

文件位置：`src/main/java/io/github/atengk/dingtalk/token/DingtalkTokenService.java`

```java
package io.github.atengk.dingtalk.token;

/**
 * 钉钉 AccessToken 服务
 *
 * @author Ateng
 * @since 2026-05-07
 */
public interface DingtalkTokenService {

    /**
     * 获取可用的 AccessToken
     *
     * @return AccessToken
     */
    String getAccessToken();

    /**
     * 强制刷新 AccessToken
     *
     * @return 新的 AccessToken
     */
    String refreshAccessToken();

    /**
     * 清理 AccessToken 缓存
     */
    void clearAccessToken();

}
```

下面的实现类使用 Redis 缓存 AccessToken，并在写入缓存时扣减提前刷新时间，避免缓存过期时间与钉钉真实过期时间完全重合。

文件位置：`src/main/java/io/github/atengk/dingtalk/token/DingtalkTokenServiceImpl.java`

```java
package io.github.atengk.dingtalk.token;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.dingtalk.config.DingtalkProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 钉钉 AccessToken 服务实现
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DingtalkTokenServiceImpl implements DingtalkTokenService {

    private final DingtalkProperties dingtalkProperties;
    private final DingtalkTokenClient dingtalkTokenClient;
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 获取可用的 AccessToken
     *
     * @return AccessToken
     */
    @Override
    public String getAccessToken() {
        String cacheKey = buildAccessTokenCacheKey();
        String accessToken = stringRedisTemplate.opsForValue().get(cacheKey);
        if (StrUtil.isNotBlank(accessToken)) {
            return accessToken;
        }

        log.info("钉钉 AccessToken 缓存不存在，开始重新获取");
        return refreshAccessToken();
    }

    /**
     * 强制刷新 AccessToken
     *
     * @return 新的 AccessToken
     */
    @Override
    public String refreshAccessToken() {
        DingtalkAccessTokenResponse response = dingtalkTokenClient.fetchAccessToken();

        Long expireIn = response.getExpireIn();
        Integer refreshBeforeSeconds = dingtalkProperties.getToken().getRefreshBeforeSeconds();
        long cacheSeconds = Math.max(expireIn - refreshBeforeSeconds, 60);

        String cacheKey = buildAccessTokenCacheKey();
        stringRedisTemplate.opsForValue().set(
                cacheKey,
                response.getAccessToken(),
                Duration.ofSeconds(cacheSeconds)
        );

        log.info("钉钉 AccessToken 已刷新，缓存有效期={}秒", cacheSeconds);
        return response.getAccessToken();
    }

    /**
     * 清理 AccessToken 缓存
     */
    @Override
    public void clearAccessToken() {
        String cacheKey = buildAccessTokenCacheKey();
        Boolean deleted = stringRedisTemplate.delete(cacheKey);
        log.info("钉钉 AccessToken 缓存清理完成，cacheKey={}，deleted={}", cacheKey, deleted);
    }

    /**
     * 构建 AccessToken 缓存 Key
     *
     * @return 缓存 Key
     */
    private String buildAccessTokenCacheKey() {
        return StrUtil.format(
                "{}:{}:{}",
                dingtalkProperties.getToken().getCachePrefix(),
                dingtalkProperties.getCorpId(),
                dingtalkProperties.getClientId()
        );
    }

}
```

这种缓存方式适合单实例和多实例部署。多个服务实例共享同一个 Redis 时，可以避免每个实例都频繁请求钉钉 AccessToken 接口。

### AccessToken 自动刷新

AccessToken 自动刷新用于降低业务请求首次触发刷新时的等待时间，同时避免 AccessToken 临近过期导致接口调用失败。定时任务可以周期性刷新 AccessToken，也可以结合 Redis 过期时间进行更精细的刷新控制。

在 Spring Boot 启动类中启用定时任务。

文件位置：`src/main/java/io/github/atengk/dingtalk/DingtalkApplication.java`

```java
package io.github.atengk.dingtalk;

import io.github.atengk.dingtalk.config.DingtalkProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 钉钉集成服务启动类
 *
 * @author Ateng
 * @since 2026-05-07
 */
@EnableScheduling
@SpringBootApplication
@EnableConfigurationProperties(DingtalkProperties.class)
public class DingtalkApplication {

    /**
     * 启动 Spring Boot 应用
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(DingtalkApplication.class, args);
    }

}
```

下面的定时任务会按照配置的固定间隔刷新 AccessToken。为避免本地开发环境或特殊环境不需要钉钉集成，可以通过 `dingtalk.enabled=false` 关闭。

文件位置：`src/main/java/io/github/atengk/dingtalk/token/DingtalkTokenRefreshTask.java`

```java
package io.github.atengk.dingtalk.token;

import io.github.atengk.dingtalk.config.DingtalkProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 钉钉 AccessToken 自动刷新任务
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DingtalkTokenRefreshTask {

    private final DingtalkProperties dingtalkProperties;
    private final DingtalkTokenService dingtalkTokenService;

    /**
     * 定时刷新钉钉 AccessToken
     */
    @Scheduled(fixedDelayString = "${dingtalk.token.refresh-fixed-delay:300000}")
    public void refreshAccessToken() {
        if (!Boolean.TRUE.equals(dingtalkProperties.getEnabled())) {
            log.info("钉钉集成未启用，跳过 AccessToken 自动刷新");
            return;
        }

        try {
            dingtalkTokenService.refreshAccessToken();
            log.info("钉钉 AccessToken 自动刷新成功");
        } catch (Exception e) {
            log.error("钉钉 AccessToken 自动刷新失败", e);
        }
    }

}
```

为了便于开发和联调，可以提供一个内部测试接口，验证 AccessToken 获取和缓存是否正常。生产环境不建议直接暴露该接口，或者必须增加管理员权限控制。

文件位置：`src/main/java/io/github/atengk/dingtalk/token/DingtalkTokenTestController.java`

```java
package io.github.atengk.dingtalk.token;

import cn.hutool.core.util.DesensitizedUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 钉钉 AccessToken 测试接口
 *
 * @author Ateng
 * @since 2026-05-07
 */
@RestController
@RequestMapping("/api/dingtalk/token")
@RequiredArgsConstructor
public class DingtalkTokenTestController {

    private final DingtalkTokenService dingtalkTokenService;

    /**
     * 获取当前 AccessToken
     *
     * @return 脱敏后的 AccessToken
     */
    @GetMapping
    public String getAccessToken() {
        String accessToken = dingtalkTokenService.getAccessToken();
        return DesensitizedUtil.password(accessToken);
    }

    /**
     * 强制刷新 AccessToken
     *
     * @return 脱敏后的 AccessToken
     */
    @PostMapping("/refresh")
    public String refreshAccessToken() {
        String accessToken = dingtalkTokenService.refreshAccessToken();
        return DesensitizedUtil.password(accessToken);
    }

    /**
     * 清理 AccessToken 缓存
     */
    @DeleteMapping
    public void clearAccessToken() {
        dingtalkTokenService.clearAccessToken();
    }

}
```

接口验证方式如下：

```bash
# 获取缓存中的 AccessToken，不存在时会自动请求钉钉接口
curl -X GET "http://127.0.0.1:8080/api/dingtalk/token"

# 强制刷新 AccessToken
curl -X POST "http://127.0.0.1:8080/api/dingtalk/token/refresh"

# 清理 AccessToken 缓存
curl -X DELETE "http://127.0.0.1:8080/api/dingtalk/token"
```

上述接口仅用于开发和测试阶段验证 AccessToken 获取、缓存、刷新是否正常。上线前应删除该测试接口，或增加认证、授权、IP 白名单等安全控制。

## 钉钉用户体系集成

本章节用于完成钉钉通讯录用户、部门与本地系统账号之间的集成。用户体系集成通常包含三类能力：通过钉钉接口读取用户信息，通过部门接口读取组织结构，以及将钉钉用户与本地系统用户建立绑定关系。

钉钉用户体系中常用的关键标识包括 `userid`、`unionid` 和本地系统用户 ID。一般建议使用 `userid` 作为企业内部应用中的主要钉钉用户标识，使用 `unionid` 作为跨应用或开放平台场景下的辅助标识。

### 获取用户信息

获取用户信息通常有两种场景。第一种是钉钉免登场景，通过免登 `code` 获取当前登录用户的 `userid`；第二种是后端管理场景，根据已知 `userid` 查询用户详情。钉钉提供了通过免登码获取用户信息的接口 `/topapi/v2/user/getuserinfo`，也提供了根据 `userid` 获取用户详情的接口 `/topapi/v2/user/get`。([Apifox](https://s.apifox.cn/apidoc/docs-site/467052/api-9093138?utm_source=chatgpt.com))

常见调用流程如下：

| 场景         | 调用方式                | 说明                                                     |
| ------------ | ----------------------- | -------------------------------------------------------- |
| 钉钉免登     | `code -> userid`        | 前端从钉钉获取免登 code，后端换取用户身份                |
| 查询用户详情 | `userid -> user detail` | 根据钉钉 userid 获取姓名、手机号、邮箱、头像、部门等信息 |
| 部门用户同步 | `dept_id -> user list`  | 根据部门 ID 分页获取部门下的用户列表                     |

下面的客户端封装依赖前文已经实现的 `DingtalkTokenService`，通过该服务获取可用的 `AccessToken`，再调用钉钉用户接口。

文件位置：`src/main/java/io/github/atengk/dingtalk/contact/DingtalkUserClient.java`

```java
package io.github.atengk.dingtalk.contact;

import cn.hutool.core.lang.Assert;
import cn.hutool.http.ContentType;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import io.github.atengk.dingtalk.token.DingtalkTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 钉钉用户客户端
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DingtalkUserClient {

    private static final String GET_USER_INFO_BY_CODE_URL = "https://oapi.dingtalk.com/topapi/v2/user/getuserinfo";
    private static final String GET_USER_DETAIL_URL = "https://oapi.dingtalk.com/topapi/v2/user/get";
    private static final String LIST_DEPARTMENT_USER_URL = "https://oapi.dingtalk.com/topapi/v2/user/list";

    private final DingtalkTokenService dingtalkTokenService;

    /**
     * 通过免登 code 获取用户身份信息
     *
     * @param code 钉钉免登 code
     * @return 用户身份信息
     */
    public JSONObject getUserInfoByCode(String code) {
        Assert.notBlank(code, "钉钉免登 code 不能为空");

        JSONObject requestBody = JSONUtil.createObj()
                .set("code", code);

        String responseBody = postWithAccessToken(GET_USER_INFO_BY_CODE_URL, requestBody);
        JSONObject responseJson = JSONUtil.parseObj(responseBody);
        checkDingtalkResponse(responseJson, "通过免登 code 获取用户信息失败");

        log.info("通过钉钉免登 code 获取用户信息成功");
        return responseJson.getJSONObject("result");
    }

    /**
     * 根据 userid 获取用户详情
     *
     * @param userId 钉钉用户 ID
     * @return 用户详情
     */
    public JSONObject getUserDetail(String userId) {
        Assert.notBlank(userId, "钉钉用户 ID 不能为空");

        JSONObject requestBody = JSONUtil.createObj()
                .set("userid", userId)
                .set("language", "zh_CN");

        String responseBody = postWithAccessToken(GET_USER_DETAIL_URL, requestBody);
        JSONObject responseJson = JSONUtil.parseObj(responseBody);
        checkDingtalkResponse(responseJson, "获取钉钉用户详情失败");

        log.info("获取钉钉用户详情成功，userId={}", userId);
        return responseJson.getJSONObject("result");
    }

    /**
     * 分页获取指定部门下的用户详情
     *
     * @param deptId 部门 ID
     * @param cursor 分页游标
     * @param size 每页数量
     * @return 部门用户分页结果
     */
    public JSONObject listDepartmentUsers(Long deptId, Long cursor, Integer size) {
        Assert.notNull(deptId, "钉钉部门 ID 不能为空");

        JSONObject requestBody = JSONUtil.createObj()
                .set("dept_id", deptId)
                .set("cursor", cursor == null ? 0 : cursor)
                .set("size", size == null ? 50 : size)
                .set("language", "zh_CN")
                .set("contain_access_limit", false)
                .set("order_field", "modify_desc");

        String responseBody = postWithAccessToken(LIST_DEPARTMENT_USER_URL, requestBody);
        JSONObject responseJson = JSONUtil.parseObj(responseBody);
        checkDingtalkResponse(responseJson, "获取部门用户详情失败");

        log.info("获取钉钉部门用户成功，deptId={}", deptId);
        return responseJson.getJSONObject("result");
    }

    /**
     * 使用 AccessToken 发起 POST 请求
     *
     * @param url 请求地址
     * @param requestBody 请求体
     * @return 响应内容
     */
    private String postWithAccessToken(String url, JSONObject requestBody) {
        String accessToken = dingtalkTokenService.getAccessToken();
        String requestUrl = url + "?access_token=" + accessToken;

        return HttpRequest.post(requestUrl)
                .contentType(ContentType.JSON.getValue())
                .body(requestBody.toString())
                .timeout(10000)
                .execute()
                .body();
    }

    /**
     * 校验钉钉接口响应
     *
     * @param responseJson 响应 JSON
     * @param message 错误提示
     */
    private void checkDingtalkResponse(JSONObject responseJson, String message) {
        Integer errcode = responseJson.getInt("errcode", -1);
        if (!Integer.valueOf(0).equals(errcode)) {
            String errmsg = responseJson.getStr("errmsg");
            log.error("{}，errcode={}，errmsg={}", message, errcode, errmsg);
            throw new IllegalStateException(message + "：" + errmsg);
        }
    }

}
```

接口使用示例：

```bash
# 通过免登 code 获取当前钉钉用户信息
curl -X POST "http://127.0.0.1:8080/api/dingtalk/users/login-info?code=xxxx"

# 根据 userid 查询钉钉用户详情
curl -X GET "http://127.0.0.1:8080/api/dingtalk/users/manager123"

# 查询部门下的用户列表
curl -X GET "http://127.0.0.1:8080/api/dingtalk/users/departments/1?cursor=0&size=50"
```

开发时需要注意：部门用户列表接口 `/topapi/v2/user/list` 只获取指定部门下的用户详情，不会自动递归获取子部门用户；如果需要同步完整组织用户，需要先递归读取部门树，再按部门分页获取用户。([Apifox](https://s.apifox.cn/apidoc/docs-site/467052/api-9093153?utm_source=chatgpt.com))

### 获取部门信息

部门信息用于构建企业组织架构、权限范围、审批节点和人员归属关系。钉钉常用的部门接口包括获取部门详情 `/topapi/v2/department/get`、获取部门列表 `/topapi/v2/department/listsub` 和获取子部门 ID 列表 `/topapi/v2/department/listsubid`。其中，获取部门列表接口通常用于读取某个部门下的直属子部门。([钉钉 API](https://dingtalk.apifox.cn/api-9093164?utm_source=chatgpt.com))

推荐本地系统保存以下部门字段：

| 字段                | 说明             |
| ------------------- | ---------------- |
| `dept_id`           | 钉钉部门 ID      |
| `parent_id`         | 父级部门 ID      |
| `name`              | 部门名称         |
| `order_num`         | 部门排序值       |
| `auto_add_user`     | 是否自动添加用户 |
| `create_dept_group` | 是否创建部门群   |
| `sync_time`         | 最近同步时间     |

下面的客户端用于获取部门详情和直属子部门列表。

文件位置：`src/main/java/io/github/atengk/dingtalk/contact/DingtalkDepartmentClient.java`

```java
package io.github.atengk.dingtalk.contact;

import cn.hutool.core.lang.Assert;
import cn.hutool.http.ContentType;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import io.github.atengk.dingtalk.token.DingtalkTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 钉钉部门客户端
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DingtalkDepartmentClient {

    private static final String GET_DEPARTMENT_DETAIL_URL = "https://oapi.dingtalk.com/topapi/v2/department/get";
    private static final String LIST_SUB_DEPARTMENT_URL = "https://oapi.dingtalk.com/topapi/v2/department/listsub";

    private final DingtalkTokenService dingtalkTokenService;

    /**
     * 获取部门详情
     *
     * @param deptId 部门 ID
     * @return 部门详情
     */
    public JSONObject getDepartmentDetail(Long deptId) {
        Assert.notNull(deptId, "钉钉部门 ID 不能为空");

        JSONObject requestBody = JSONUtil.createObj()
                .set("dept_id", deptId)
                .set("language", "zh_CN");

        String responseBody = postWithAccessToken(GET_DEPARTMENT_DETAIL_URL, requestBody);
        JSONObject responseJson = JSONUtil.parseObj(responseBody);
        checkDingtalkResponse(responseJson, "获取钉钉部门详情失败");

        log.info("获取钉钉部门详情成功，deptId={}", deptId);
        return responseJson.getJSONObject("result");
    }

    /**
     * 获取直属子部门列表
     *
     * @param deptId 父部门 ID
     * @return 子部门列表
     */
    public JSONObject listSubDepartments(Long deptId) {
        Assert.notNull(deptId, "钉钉父部门 ID 不能为空");

        JSONObject requestBody = JSONUtil.createObj()
                .set("dept_id", deptId)
                .set("language", "zh_CN");

        String responseBody = postWithAccessToken(LIST_SUB_DEPARTMENT_URL, requestBody);
        JSONObject responseJson = JSONUtil.parseObj(responseBody);
        checkDingtalkResponse(responseJson, "获取钉钉子部门列表失败");

        log.info("获取钉钉子部门列表成功，deptId={}", deptId);
        return responseJson;
    }

    /**
     * 使用 AccessToken 发起 POST 请求
     *
     * @param url 请求地址
     * @param requestBody 请求体
     * @return 响应内容
     */
    private String postWithAccessToken(String url, JSONObject requestBody) {
        String accessToken = dingtalkTokenService.getAccessToken();
        String requestUrl = url + "?access_token=" + accessToken;

        return HttpRequest.post(requestUrl)
                .contentType(ContentType.JSON.getValue())
                .body(requestBody.toString())
                .timeout(10000)
                .execute()
                .body();
    }

    /**
     * 校验钉钉接口响应
     *
     * @param responseJson 响应 JSON
     * @param message 错误提示
     */
    private void checkDingtalkResponse(JSONObject responseJson, String message) {
        Integer errcode = responseJson.getInt("errcode", -1);
        if (!Integer.valueOf(0).equals(errcode)) {
            String errmsg = responseJson.getStr("errmsg");
            log.error("{}，errcode={}，errmsg={}", message, errcode, errmsg);
            throw new IllegalStateException(message + "：" + errmsg);
        }
    }

}
```

部门同步建议采用“从根部门递归向下同步”的方式。钉钉根部门 ID 通常按接口约定使用 `1`，同步时先读取根部门下的子部门，再递归读取每个子部门的详情与下级部门。

部门同步需要注意以下事项：

| 注意事项                   | 说明                                 |
| -------------------------- | ------------------------------------ |
| 不要只同步一级部门         | 多级组织需要递归读取                 |
| 不要忽略停用或隐藏部门策略 | 本地权限系统需要明确是否保留         |
| 不要频繁全量同步           | 可使用定时任务或事件回调做增量更新   |
| 注意部门 ID 类型           | 建议本地使用 `Long` 保存             |
| 注意接口权限范围           | 应用权限不足时可能无法读取完整通讯录 |

### 用户与系统账号绑定

用户绑定用于建立钉钉用户和本地系统账号之间的对应关系。绑定完成后，系统可以通过钉钉身份识别本地用户，也可以根据本地用户找到对应的钉钉 `userid`，用于发送工作通知或发起审批。

推荐绑定关系如下：

| 本地字段            | 说明            |
| ------------------- | --------------- |
| `id`                | 主键 ID         |
| `system_user_id`    | 本地系统用户 ID |
| `dingtalk_user_id`  | 钉钉 userid     |
| `dingtalk_union_id` | 钉钉 unionid    |
| `dingtalk_name`     | 钉钉用户姓名    |
| `mobile`            | 手机号          |
| `avatar`            | 头像            |
| `bind_status`       | 绑定状态        |
| `bind_time`         | 绑定时间        |
| `last_sync_time`    | 最近同步时间    |

建表语句如下：

```sql
CREATE TABLE sys_user_dingtalk_bind (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID',
    system_user_id BIGINT NOT NULL COMMENT '本地系统用户 ID',
    dingtalk_user_id VARCHAR(128) NOT NULL COMMENT '钉钉 userid',
    dingtalk_union_id VARCHAR(128) DEFAULT NULL COMMENT '钉钉 unionid',
    dingtalk_name VARCHAR(128) DEFAULT NULL COMMENT '钉钉用户姓名',
    mobile VARCHAR(32) DEFAULT NULL COMMENT '手机号',
    avatar VARCHAR(512) DEFAULT NULL COMMENT '头像地址',
    bind_status TINYINT NOT NULL DEFAULT 1 COMMENT '绑定状态：1已绑定，0已解绑',
    bind_time DATETIME NOT NULL COMMENT '绑定时间',
    last_sync_time DATETIME DEFAULT NULL COMMENT '最近同步时间',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间',
    UNIQUE KEY uk_system_user_id (system_user_id),
    UNIQUE KEY uk_dingtalk_user_id (dingtalk_user_id),
    KEY idx_dingtalk_union_id (dingtalk_union_id)
) COMMENT='系统用户与钉钉用户绑定表';
```

绑定流程建议如下：

1. 前端从钉钉环境获取免登 `code`。
2. 后端调用钉钉接口获取当前用户 `userid`。
3. 后端根据 `userid` 查询钉钉用户详情。
4. 根据手机号、工号或用户主动确认逻辑匹配本地系统账号。
5. 保存本地用户 ID 与钉钉 `userid`、`unionid` 的绑定关系。
6. 后续登录、通知、审批等功能直接使用绑定关系。

绑定策略建议：

| 策略           | 说明                                       |
| -------------- | ------------------------------------------ |
| 手机号匹配     | 适合本地账号手机号可信且与钉钉一致的场景   |
| 工号匹配       | 适合企业内部 HR 数据规范的场景             |
| 用户确认绑定   | 适合存在重名、手机号不一致或外部用户的场景 |
| 管理员手动绑定 | 适合高安全要求系统                         |
| 批量导入绑定   | 适合已有统一身份源的企业                   |

用户绑定时不建议只依赖姓名匹配。姓名存在重复风险，手机号、工号、邮箱等字段也可能存在变更，因此需要保留 `userid` 和 `unionid` 作为稳定绑定字段。

## 消息通知开发

本章节用于实现钉钉消息通知能力，主要包括工作通知消息、群机器人消息和消息发送结果处理。工作通知适合向指定员工发送应用消息，群机器人适合向指定群聊推送提醒、告警或日报。

钉钉消息能力需要根据业务场景选择。工作通知以应用身份向员工推送消息，常用于生日祝福、入职提醒、流程提醒等场景；群自定义机器人通过 Webhook 向群聊发送消息，适合简单群通知，但钉钉开发者百科提示群自定义机器人不支持单聊，且仅支持向群内发送消息。([钉钉 API](https://dingtalk.apifox.cn/api-9093209?utm_source=chatgpt.com))

### 工作通知消息

工作通知消息用于以企业内部应用的名义向员工发送通知。钉钉发送工作通知接口为 `/topapi/message/corpconversation/asyncsend_v2`，调用成功后会返回任务 ID，后续可以根据任务 ID 查询发送进度或发送结果。([钉钉 API](https://dingtalk.apifox.cn/api-9093209?utm_source=chatgpt.com))

工作通知适合以下场景：

| 场景     | 说明                                 |
| -------- | ------------------------------------ |
| 审批提醒 | 审批待处理、审批完成、审批驳回       |
| 工单通知 | 工单分派、工单超时、工单关闭         |
| 业务提醒 | 合同到期、订单异常、库存预警         |
| 人事通知 | 入职提醒、转正提醒、生日祝福         |
| 系统通知 | 任务执行结果、接口异常、数据同步完成 |

下面的请求对象用于发送文本类工作通知。

文件位置：`src/main/java/io/github/atengk/dingtalk/message/WorkNoticeRequest.java`

```java
package io.github.atengk.dingtalk.message;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 工作通知请求对象
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Data
public class WorkNoticeRequest {

    /**
     * 接收人 userid，多个用户使用英文逗号分隔
     */
    @NotBlank(message = "接收人不能为空")
    private String userIdList;

    /**
     * 消息内容
     */
    @NotBlank(message = "消息内容不能为空")
    private String content;

}
```

下面的客户端用于发送文本工作通知。代码依赖前文 `DingtalkProperties` 中的 `agentId` 配置和 `DingtalkTokenService`。

文件位置：`src/main/java/io/github/atengk/dingtalk/message/DingtalkWorkNoticeClient.java`

```java
package io.github.atengk.dingtalk.message;

import cn.hutool.core.lang.Assert;
import cn.hutool.http.ContentType;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import io.github.atengk.dingtalk.config.DingtalkProperties;
import io.github.atengk.dingtalk.token.DingtalkTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 钉钉工作通知客户端
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DingtalkWorkNoticeClient {

    private static final String SEND_WORK_NOTICE_URL = "https://oapi.dingtalk.com/topapi/message/corpconversation/asyncsend_v2";

    private final DingtalkProperties dingtalkProperties;
    private final DingtalkTokenService dingtalkTokenService;

    /**
     * 发送文本工作通知
     *
     * @param userIdList 接收人 userid，多个用户使用英文逗号分隔
     * @param content 消息内容
     * @return 钉钉任务 ID
     */
    public Long sendTextNotice(String userIdList, String content) {
        Assert.notBlank(userIdList, "工作通知接收人不能为空");
        Assert.notBlank(content, "工作通知内容不能为空");

        JSONObject msg = JSONUtil.createObj()
                .set("msgtype", "text")
                .set("text", JSONUtil.createObj().set("content", content));

        JSONObject requestBody = JSONUtil.createObj()
                .set("agent_id", Long.valueOf(dingtalkProperties.getAgentId()))
                .set("userid_list", userIdList)
                .set("to_all_user", false)
                .set("msg", msg);

        String accessToken = dingtalkTokenService.getAccessToken();
        String requestUrl = SEND_WORK_NOTICE_URL + "?access_token=" + accessToken;

        String responseBody = HttpRequest.post(requestUrl)
                .contentType(ContentType.JSON.getValue())
                .body(requestBody.toString())
                .timeout(10000)
                .execute()
                .body();

        JSONObject responseJson = JSONUtil.parseObj(responseBody);
        Integer errcode = responseJson.getInt("errcode", -1);
        if (!Integer.valueOf(0).equals(errcode)) {
            String errmsg = responseJson.getStr("errmsg");
            log.error("发送钉钉工作通知失败，errcode={}，errmsg={}", errcode, errmsg);
            throw new IllegalStateException("发送钉钉工作通知失败：" + errmsg);
        }

        Long taskId = responseJson.getLong("task_id");
        log.info("发送钉钉工作通知成功，taskId={}，userIdList={}", taskId, userIdList);
        return taskId;
    }

}
```

接口示例：

```bash
curl -X POST "http://127.0.0.1:8080/api/dingtalk/messages/work-notice/text" \
  -H "Content-Type: application/json" \
  -d '{
    "userIdList": "manager123,user001",
    "content": "您有一条新的审批任务，请及时处理。"
  }'
```

工作通知发送成功只表示钉钉已经接受发送任务，不一定代表所有接收人已经收到消息。对可靠性要求较高的业务，需要记录 `task_id` 并查询发送进度或发送结果。

### 群机器人消息

群机器人消息适合向固定群聊推送通知，例如系统告警、日报、部署结果和接口异常提醒。群自定义机器人通过 Webhook 发送消息，支持文本、Markdown 等常见消息类型，但其能力边界明显弱于应用机器人。钉钉开发者百科将机器人分为应用机器人和群自定义机器人，并说明应用机器人具备更完整的收发消息能力；群自定义机器人主要用于简单群发送。([opensource.dingtalk.com](https://opensource.dingtalk.com/developerpedia/docs/learn/bot/overview?utm_source=chatgpt.com))

群机器人配置建议放入 `application.yml`。

文件位置：`src/main/resources/application.yml`

```yaml
dingtalk:
  robot:
    # 群自定义机器人 Webhook 地址
    webhook: ${DINGTALK_ROBOT_WEBHOOK:}

    # 群自定义机器人加签密钥
    secret: ${DINGTALK_ROBOT_SECRET:}

    # 是否启用群机器人消息
    enabled: ${DINGTALK_ROBOT_ENABLED:false}
```

如果启用了加签安全设置，需要在发送请求时根据时间戳和密钥计算签名，并将 `timestamp` 和 `sign` 参数拼接到 Webhook 地址中。

下面的配置类可以追加到前文 `DingtalkProperties` 中。

```java
/**
 * 群机器人配置项
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Data
public static class Robot {

    /**
     * 是否启用群机器人消息
     */
    private Boolean enabled = Boolean.FALSE;

    /**
     * 群机器人 Webhook 地址
     */
    private String webhook;

    /**
     * 群机器人加签密钥
     */
    private String secret;

}
```

同时在 `DingtalkProperties` 主类中增加字段：

```java
/**
 * 群机器人配置
 */
@Valid
private Robot robot = new Robot();
```

下面的客户端用于发送 Markdown 群机器人消息，并使用 Hutool 完成 HmacSHA256 签名和 URL 编码。

文件位置：`src/main/java/io/github/atengk/dingtalk/message/DingtalkRobotClient.java`

```java
package io.github.atengk.dingtalk.message;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.net.URLEncodeUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.HMac;
import cn.hutool.crypto.digest.HmacAlgorithm;
import cn.hutool.http.ContentType;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import io.github.atengk.dingtalk.config.DingtalkProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * 钉钉群机器人客户端
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DingtalkRobotClient {

    private final DingtalkProperties dingtalkProperties;

    /**
     * 发送 Markdown 群机器人消息
     *
     * @param title 消息标题
     * @param markdown Markdown 内容
     */
    public void sendMarkdown(String title, String markdown) {
        Assert.notBlank(title, "机器人消息标题不能为空");
        Assert.notBlank(markdown, "机器人消息内容不能为空");

        DingtalkProperties.Robot robot = dingtalkProperties.getRobot();
        if (!Boolean.TRUE.equals(robot.getEnabled())) {
            log.info("钉钉群机器人未启用，跳过消息发送");
            return;
        }

        Assert.notBlank(robot.getWebhook(), "钉钉群机器人 Webhook 不能为空");

        JSONObject requestBody = JSONUtil.createObj()
                .set("msgtype", "markdown")
                .set("markdown", JSONUtil.createObj()
                        .set("title", title)
                        .set("text", markdown));

        String requestUrl = buildSignedWebhook(robot.getWebhook(), robot.getSecret());

        String responseBody = HttpRequest.post(requestUrl)
                .contentType(ContentType.JSON.getValue())
                .body(requestBody.toString())
                .timeout(10000)
                .execute()
                .body();

        JSONObject responseJson = JSONUtil.parseObj(responseBody);
        Integer errcode = responseJson.getInt("errcode", -1);
        if (!Integer.valueOf(0).equals(errcode)) {
            String errmsg = responseJson.getStr("errmsg");
            log.error("发送钉钉群机器人消息失败，errcode={}，errmsg={}", errcode, errmsg);
            throw new IllegalStateException("发送钉钉群机器人消息失败：" + errmsg);
        }

        log.info("发送钉钉群机器人消息成功，title={}", title);
    }

    /**
     * 构建加签后的 Webhook 地址
     *
     * @param webhook 原始 Webhook 地址
     * @param secret 加签密钥
     * @return 加签后的 Webhook 地址
     */
    private String buildSignedWebhook(String webhook, String secret) {
        if (StrUtil.isBlank(secret)) {
            return webhook;
        }

        long timestamp = System.currentTimeMillis();
        String stringToSign = timestamp + "\n" + secret;

        HMac hMac = new HMac(HmacAlgorithm.HmacSHA256, secret.getBytes(StandardCharsets.UTF_8));
        String sign = URLEncodeUtil.encode(Base64.encode(hMac.digest(stringToSign)), StandardCharsets.UTF_8);

        String separator = webhook.contains("?") ? "&" : "?";
        return webhook + separator + "timestamp=" + timestamp + "&sign=" + sign;
    }

}
```

发送 Markdown 群消息示例：

```java
dingtalkRobotClient.sendMarkdown(
        "系统告警",
        "### 系统告警\n\n- 服务：订单服务\n- 状态：接口超时\n- 时间：2026-05-07 10:00:00"
);
```

群机器人消息适合低频、聚合类通知。对于监控告警类场景，不建议每次异常都单独发送一条消息，应在业务层做聚合、降噪和频率控制。

### 消息发送结果处理

消息发送结果处理用于记录消息发送任务、查询钉钉发送状态，并为业务系统提供可追踪的通知结果。工作通知发送接口返回 `task_id` 后，可以调用发送进度接口 `/topapi/message/corpconversation/getsendprogress` 或发送结果接口 `/topapi/message/corpconversation/getsendresult` 查询状态；钉钉文档说明这些结果通常只能查询 24 小时内的数据，并且接收人列表超过 100 人时不支持调用发送结果接口，否则可能返回超时。([钉钉 API](https://dingtalk.apifox.cn/api-9093212?utm_source=chatgpt.com))

推荐保存消息发送记录表：

```sql
CREATE TABLE dingtalk_message_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID',
    message_type VARCHAR(32) NOT NULL COMMENT '消息类型：WORK_NOTICE、ROBOT',
    receiver VARCHAR(1024) DEFAULT NULL COMMENT '接收人或接收群',
    title VARCHAR(255) DEFAULT NULL COMMENT '消息标题',
    content TEXT NOT NULL COMMENT '消息内容',
    task_id BIGINT DEFAULT NULL COMMENT '钉钉工作通知任务 ID',
    send_status VARCHAR(32) NOT NULL COMMENT '发送状态：PENDING、SUCCESS、FAILED、UNKNOWN',
    fail_reason VARCHAR(1024) DEFAULT NULL COMMENT '失败原因',
    send_time DATETIME DEFAULT NULL COMMENT '发送时间',
    result_query_time DATETIME DEFAULT NULL COMMENT '结果查询时间',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间',
    KEY idx_task_id (task_id),
    KEY idx_message_type (message_type),
    KEY idx_send_status (send_status)
) COMMENT='钉钉消息发送记录表';
```

消息状态建议按以下方式处理：

| 状态      | 说明                                     |
| --------- | ---------------------------------------- |
| `PENDING` | 已创建发送记录，但尚未完成发送           |
| `SUCCESS` | 钉钉接口调用成功，或查询结果显示发送成功 |
| `FAILED`  | 钉钉接口调用失败，或查询结果显示发送失败 |
| `UNKNOWN` | 接口超时、网络异常或结果无法确认         |

下面的客户端用于查询工作通知发送进度和发送结果。

文件位置：`src/main/java/io/github/atengk/dingtalk/message/DingtalkMessageResultClient.java`

```java
package io.github.atengk.dingtalk.message;

import cn.hutool.core.lang.Assert;
import cn.hutool.http.ContentType;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import io.github.atengk.dingtalk.config.DingtalkProperties;
import io.github.atengk.dingtalk.token.DingtalkTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 钉钉消息发送结果客户端
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DingtalkMessageResultClient {

    private static final String GET_SEND_PROGRESS_URL = "https://oapi.dingtalk.com/topapi/message/corpconversation/getsendprogress";
    private static final String GET_SEND_RESULT_URL = "https://oapi.dingtalk.com/topapi/message/corpconversation/getsendresult";

    private final DingtalkProperties dingtalkProperties;
    private final DingtalkTokenService dingtalkTokenService;

    /**
     * 查询工作通知发送进度
     *
     * @param taskId 钉钉任务 ID
     * @return 发送进度
     */
    public JSONObject getSendProgress(Long taskId) {
        Assert.notNull(taskId, "钉钉消息任务 ID 不能为空");

        JSONObject requestBody = JSONUtil.createObj()
                .set("agent_id", Long.valueOf(dingtalkProperties.getAgentId()))
                .set("task_id", taskId);

        JSONObject responseJson = postWithAccessToken(GET_SEND_PROGRESS_URL, requestBody);
        log.info("查询钉钉工作通知发送进度成功，taskId={}", taskId);
        return responseJson.getJSONObject("progress");
    }

    /**
     * 查询工作通知发送结果
     *
     * @param taskId 钉钉任务 ID
     * @return 发送结果
     */
    public JSONObject getSendResult(Long taskId) {
        Assert.notNull(taskId, "钉钉消息任务 ID 不能为空");

        JSONObject requestBody = JSONUtil.createObj()
                .set("agent_id", Long.valueOf(dingtalkProperties.getAgentId()))
                .set("task_id", taskId);

        JSONObject responseJson = postWithAccessToken(GET_SEND_RESULT_URL, requestBody);
        log.info("查询钉钉工作通知发送结果成功，taskId={}", taskId);
        return responseJson.getJSONObject("send_result");
    }

    /**
     * 使用 AccessToken 发起 POST 请求
     *
     * @param url 请求地址
     * @param requestBody 请求体
     * @return 响应 JSON
     */
    private JSONObject postWithAccessToken(String url, JSONObject requestBody) {
        String accessToken = dingtalkTokenService.getAccessToken();
        String requestUrl = url + "?access_token=" + accessToken;

        String responseBody = HttpRequest.post(requestUrl)
                .contentType(ContentType.JSON.getValue())
                .body(requestBody.toString())
                .timeout(10000)
                .execute()
                .body();

        JSONObject responseJson = JSONUtil.parseObj(responseBody);
        Integer errcode = responseJson.getInt("errcode", -1);
        if (!Integer.valueOf(0).equals(errcode)) {
            String errmsg = responseJson.getStr("errmsg");
            log.error("查询钉钉消息发送结果失败，errcode={}，errmsg={}", errcode, errmsg);
            throw new IllegalStateException("查询钉钉消息发送结果失败：" + errmsg);
        }

        return responseJson;
    }

}
```

推荐的发送结果处理流程如下：

1. 业务系统创建消息发送记录，状态为 `PENDING`。
2. 调用工作通知或机器人消息发送接口。
3. 如果接口调用成功，将状态更新为 `SUCCESS`，并保存 `task_id`。
4. 如果接口调用失败，将状态更新为 `FAILED`，并保存失败原因。
5. 对工作通知消息，可以通过定时任务查询 `task_id` 对应的发送进度或发送结果。
6. 查询结果异常时，不要立即覆盖为失败，可先标记为 `UNKNOWN`，等待下一次补偿查询。
7. 超过钉钉结果可查询时间范围后，仍无法确认的消息应保留为 `UNKNOWN` 或由人工排查。

消息通知模块上线前需要重点验证以下内容：

| 验证项                  | 说明                               |
| ----------------------- | ---------------------------------- |
| AccessToken 是否有效    | 工作通知依赖有效的 AccessToken     |
| AgentId 是否正确        | AgentId 错误会导致工作通知发送失败 |
| userid 是否正确         | 接收人不存在或无权限会导致发送失败 |
| 机器人 Webhook 是否正确 | Webhook 错误会导致群消息发送失败   |
| 加签密钥是否一致        | 启用加签后，签名错误会被钉钉拒绝   |
| 发送记录是否落库        | 便于排查消息是否真正发送           |
| 是否具备重试机制        | 网络抖动、接口超时需要补偿处理     |
| 是否做频率控制          | 避免告警风暴或机器人消息刷屏       |

## 审批流程集成

本章节用于实现业务系统与钉钉审批流程的集成，主要包括审批实例创建、审批状态查询和审批回调处理。业务系统通常负责保存本地业务单据，钉钉负责承载审批流转，二者通过 `process_instance_id` 建立关联关系。该章节继续基于你原始大纲中的审批流程集成部分展开。

### 审批实例创建

审批实例创建用于将本地业务单据提交到钉钉审批流程中。例如请假单、报销单、采购单、合同审批、工单审批等，都可以由业务系统组装表单字段后调用钉钉审批接口发起流程。

钉钉旧版服务端审批实例创建接口为 `/topapi/processinstance/create`，请求中通常需要传入 `process_code`、`originator_user_id`、`dept_id` 和 `form_component_values`。其中 `process_code` 是审批模板唯一标识，`originator_user_id` 是发起人的钉钉 `userid`，`dept_id` 是发起人所在部门，`form_component_values` 是审批表单控件值。([钉钉 API](https://dingtalk.apifox.cn/api-9093218?utm_source=chatgpt.com))

推荐的审批发起流程如下：

| 步骤             | 说明                                             |
| ---------------- | ------------------------------------------------ |
| 创建本地业务单据 | 先保存业务数据，例如请假单、报销单、采购单       |
| 查询钉钉绑定关系 | 根据本地用户 ID 获取钉钉 `userid`                |
| 组装审批表单     | 将本地字段转换为钉钉审批表单控件                 |
| 调用钉钉审批接口 | 调用审批实例创建接口，获取 `process_instance_id` |
| 保存审批关联关系 | 将本地业务 ID 与钉钉审批实例 ID 绑定             |
| 等待审批回调     | 后续通过回调或主动查询同步审批状态               |

审批实例创建请求对象如下。

文件位置：`src/main/java/io/github/atengk/dingtalk/approval/ApprovalCreateRequest.java`

```java
package io.github.atengk.dingtalk.approval;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 钉钉审批实例创建请求
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Data
public class ApprovalCreateRequest {

    /**
     * 审批流程模板编码
     */
    @NotBlank(message = "审批流程模板编码不能为空")
    private String processCode;

    /**
     * 审批发起人钉钉 userid
     */
    @NotBlank(message = "审批发起人不能为空")
    private String originatorUserId;

    /**
     * 发起人部门 ID，根部门可按钉钉接口要求传 -1
     */
    @NotBlank(message = "发起人部门 ID 不能为空")
    private String deptId;

    /**
     * 审批表单控件值
     */
    @Valid
    @NotEmpty(message = "审批表单控件值不能为空")
    private List<FormComponentValue> formComponentValues;

    /**
     * 审批人 userid，多个用户使用英文逗号分隔，按审批模板配置决定是否需要传入
     */
    private String approvers;

    /**
     * 抄送人 userid，多个用户使用英文逗号分隔
     */
    private String ccList;

    /**
     * 表单控件值
     *
     * @author Ateng
     * @since 2026-05-07
     */
    @Data
    public static class FormComponentValue {

        /**
         * 控件名称，必须与钉钉审批模板中的控件名称一致
         */
        @NotBlank(message = "表单控件名称不能为空")
        private String name;

        /**
         * 控件值
         */
        private String value;

        /**
         * 扩展值，复杂控件可按钉钉要求传入
         */
        private String extValue;

    }

}
```

下面的客户端用于调用钉钉审批实例创建接口，并返回审批实例 ID。

文件位置：`src/main/java/io/github/atengk/dingtalk/approval/DingtalkApprovalClient.java`

```java
package io.github.atengk.dingtalk.approval;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.ContentType;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import io.github.atengk.dingtalk.token.DingtalkTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 钉钉审批客户端
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DingtalkApprovalClient {

    private static final String CREATE_PROCESS_INSTANCE_URL = "https://oapi.dingtalk.com/topapi/processinstance/create";
    private static final String GET_PROCESS_INSTANCE_URL = "https://oapi.dingtalk.com/topapi/processinstance/get";

    private final DingtalkTokenService dingtalkTokenService;

    /**
     * 创建钉钉审批实例
     *
     * @param request 审批创建请求
     * @return 审批实例 ID
     */
    public String createProcessInstance(ApprovalCreateRequest request) {
        Assert.notNull(request, "审批创建请求不能为空");

        JSONObject requestBody = JSONUtil.createObj()
                .set("process_code", request.getProcessCode())
                .set("originator_user_id", request.getOriginatorUserId())
                .set("dept_id", request.getDeptId())
                .set("form_component_values", request.getFormComponentValues());

        if (StrUtil.isNotBlank(request.getApprovers())) {
            requestBody.set("approvers", request.getApprovers());
        }
        if (StrUtil.isNotBlank(request.getCcList())) {
            requestBody.set("cc_list", request.getCcList());
        }

        JSONObject responseJson = postWithAccessToken(CREATE_PROCESS_INSTANCE_URL, requestBody, "创建钉钉审批实例失败");
        String processInstanceId = responseJson.getStr("process_instance_id");
        Assert.notBlank(processInstanceId, "钉钉审批实例 ID 不能为空");

        log.info("创建钉钉审批实例成功，processCode={}，processInstanceId={}",
                request.getProcessCode(), processInstanceId);
        return processInstanceId;
    }

    /**
     * 获取钉钉审批实例详情
     *
     * @param processInstanceId 审批实例 ID
     * @return 审批实例详情
     */
    public JSONObject getProcessInstance(String processInstanceId) {
        Assert.notBlank(processInstanceId, "审批实例 ID 不能为空");

        JSONObject requestBody = JSONUtil.createObj()
                .set("process_instance_id", processInstanceId);

        JSONObject responseJson = postWithAccessToken(GET_PROCESS_INSTANCE_URL, requestBody, "获取钉钉审批实例详情失败");
        log.info("获取钉钉审批实例详情成功，processInstanceId={}", processInstanceId);
        return responseJson.getJSONObject("process_instance");
    }

    /**
     * 使用 AccessToken 发起 POST 请求
     *
     * @param url 请求地址
     * @param requestBody 请求体
     * @param errorMessage 错误提示
     * @return 响应 JSON
     */
    private JSONObject postWithAccessToken(String url, JSONObject requestBody, String errorMessage) {
        String accessToken = dingtalkTokenService.getAccessToken();
        String requestUrl = url + "?access_token=" + accessToken;

        String responseBody = HttpRequest.post(requestUrl)
                .contentType(ContentType.JSON.getValue())
                .body(requestBody.toString())
                .timeout(15000)
                .execute()
                .body();

        JSONObject responseJson = JSONUtil.parseObj(responseBody);
        Integer errcode = responseJson.getInt("errcode", -1);
        if (!Integer.valueOf(0).equals(errcode)) {
            String errmsg = responseJson.getStr("errmsg");
            log.error("{}，errcode={}，errmsg={}", errorMessage, errcode, errmsg);
            throw new IllegalStateException(errorMessage + "：" + errmsg);
        }

        return responseJson;
    }

}
```

业务系统发起审批时，建议先保存本地业务单据，再调用钉钉审批接口，最后保存本地业务单据与钉钉审批实例之间的关系。

示例请求：

```json
{
  "processCode": "PROC-XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX",
  "originatorUserId": "manager123",
  "deptId": "1",
  "formComponentValues": [
    {
      "name": "申请事由",
      "value": "采购办公设备"
    },
    {
      "name": "申请金额",
      "value": "3000"
    },
    {
      "name": "备注",
      "value": "用于研发部门新增工位"
    }
  ],
  "ccList": "user001,user002"
}
```

审批发起成功后，需要保存 `process_instance_id`。该字段是后续查询审批状态、处理审批回调和反查本地业务单据的关键字段。

### 审批状态查询

审批状态查询用于主动获取钉钉审批实例详情。它通常用于两类场景：第一类是用户在本地系统页面查看审批进度；第二类是回调异常、消息丢失或状态不一致时，通过定时任务进行补偿同步。

钉钉审批详情接口 `/topapi/processinstance/get` 通过 `process_instance_id` 获取审批实例详情，响应中会包含标题、发起人、审批人、抄送人、状态、结果、业务编号、操作记录等信息。([钉钉 API](https://dingtalk.apifox.cn/api-9093219?utm_source=chatgpt.com))

常见审批状态处理建议如下：

| 钉钉状态字段    | 本地状态建议       | 说明       |
| --------------- | ------------------ | ---------- |
| `RUNNING`       | `APPROVING`        | 审批中     |
| `COMPLETED`     | 根据 `result` 判断 | 审批已完成 |
| `TERMINATED`    | `CANCELED`         | 审批已终止 |
| `result=agree`  | `APPROVED`         | 审批通过   |
| `result=refuse` | `REJECTED`         | 审批拒绝   |

下面的响应对象用于承接本地系统关心的审批状态字段。

文件位置：`src/main/java/io/github/atengk/dingtalk/approval/ApprovalStatusResult.java`

```java
package io.github.atengk.dingtalk.approval;

import lombok.Data;

/**
 * 审批状态结果
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Data
public class ApprovalStatusResult {

    /**
     * 审批实例 ID
     */
    private String processInstanceId;

    /**
     * 审批标题
     */
    private String title;

    /**
     * 审批状态
     */
    private String status;

    /**
     * 审批结果
     */
    private String result;

    /**
     * 本地业务状态
     */
    private String businessStatus;

    /**
     * 审批发起人 userid
     */
    private String originatorUserId;

    /**
     * 审批创建时间
     */
    private String createTime;

    /**
     * 审批完成时间
     */
    private String finishTime;

}
```

下面的 Service 用于封装审批状态转换逻辑，避免业务代码直接依赖钉钉原始字段。

文件位置：`src/main/java/io/github/atengk/dingtalk/approval/DingtalkApprovalService.java`

```java
package io.github.atengk.dingtalk.approval;

/**
 * 钉钉审批服务
 *
 * @author Ateng
 * @since 2026-05-07
 */
public interface DingtalkApprovalService {

    /**
     * 查询审批状态
     *
     * @param processInstanceId 审批实例 ID
     * @return 审批状态结果
     */
    ApprovalStatusResult getApprovalStatus(String processInstanceId);

}
```

文件位置：`src/main/java/io/github/atengk/dingtalk/approval/DingtalkApprovalServiceImpl.java`

```java
package io.github.atengk.dingtalk.approval;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 钉钉审批服务实现
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DingtalkApprovalServiceImpl implements DingtalkApprovalService {

    private final DingtalkApprovalClient dingtalkApprovalClient;

    /**
     * 查询审批状态
     *
     * @param processInstanceId 审批实例 ID
     * @return 审批状态结果
     */
    @Override
    public ApprovalStatusResult getApprovalStatus(String processInstanceId) {
        JSONObject processInstance = dingtalkApprovalClient.getProcessInstance(processInstanceId);

        String status = processInstance.getStr("status");
        String result = processInstance.getStr("result");

        ApprovalStatusResult statusResult = new ApprovalStatusResult();
        statusResult.setProcessInstanceId(processInstanceId);
        statusResult.setTitle(processInstance.getStr("title"));
        statusResult.setStatus(status);
        statusResult.setResult(result);
        statusResult.setBusinessStatus(convertBusinessStatus(status, result));
        statusResult.setOriginatorUserId(processInstance.getStr("originator_userid"));
        statusResult.setCreateTime(processInstance.getStr("create_time"));
        statusResult.setFinishTime(processInstance.getStr("finish_time"));

        log.info("钉钉审批状态查询完成，processInstanceId={}，status={}，result={}",
                processInstanceId, status, result);
        return statusResult;
    }

    /**
     * 转换本地业务状态
     *
     * @param status 钉钉审批状态
     * @param result 钉钉审批结果
     * @return 本地业务状态
     */
    private String convertBusinessStatus(String status, String result) {
        if (StrUtil.equalsIgnoreCase("RUNNING", status)) {
            return "APPROVING";
        }
        if (StrUtil.equalsIgnoreCase("TERMINATED", status)) {
            return "CANCELED";
        }
        if (StrUtil.equalsIgnoreCase("COMPLETED", status) && StrUtil.equalsIgnoreCase("agree", result)) {
            return "APPROVED";
        }
        if (StrUtil.equalsIgnoreCase("COMPLETED", status) && StrUtil.equalsIgnoreCase("refuse", result)) {
            return "REJECTED";
        }
        return "UNKNOWN";
    }

}
```

接口验证示例：

```bash
curl -X GET "http://127.0.0.1:8080/api/dingtalk/approvals/1a2b-3c4d-xxxx/status"
```

审批状态查询不能完全替代审批回调。建议以回调为主、主动查询为辅：回调用于实时推进业务状态，主动查询用于页面展示和异常补偿。

### 审批回调处理

审批回调用于接收钉钉主动推送的审批实例变更事件。审批状态发生变化时，钉钉会将事件推送到配置的回调地址，后端服务解密事件内容后，根据 `EventType`、`processInstanceId`、`type`、`result` 等字段更新本地业务状态。

审批回调常见事件类型为 `bpms_instance_change`。钉钉回调示例中，审批实例变更事件通常包含 `EventType`、`processInstanceId`、`corpId`、`createTime`、`title`、`type`、`staffId`、`url`、`processCode` 等字段。([npm.io](https://npm.io/package/dingtalk-encrypt?utm_source=chatgpt.com))

推荐处理流程如下：

| 步骤         | 说明                                                         |
| ------------ | ------------------------------------------------------------ |
| 接收回调请求 | 获取请求参数 `signature`、`timestamp`、`nonce` 和请求体 `encrypt` |
| 签名校验     | 使用 Token、timestamp、nonce、encrypt 计算签名并比对         |
| 解密事件内容 | 使用 AES Key 和 CorpId 解密回调内容                          |
| 解析事件类型 | 根据 `EventType` 判断事件类型                                |
| 幂等校验     | 使用事件唯一字段或业务组合字段避免重复处理                   |
| 更新业务状态 | 根据审批实例 ID 查询本地关联单据并更新状态                   |
| 返回 success | 按钉钉要求返回加密后的 `success`                             |

审批回调建议只做快速处理，不要在回调接口中执行耗时业务。如果后续逻辑较复杂，可以先记录事件日志，再投递到 MQ 或异步任务中处理。

## 事件回调开发

本章节用于实现钉钉 HTTP 事件回调接入，包括回调接口设计、签名校验、事件解密和事件分发。事件回调不仅用于审批流程，也可以用于用户变更、部门变更、组织架构变更、机器人消息、互动卡片等场景。

钉钉 HTTP 回调会将签名、时间戳和随机字符串放在请求参数中，并将加密后的事件内容放在请求体的 `encrypt` 字段中；服务端处理完成后需要返回加密后的 `success`，钉钉才会认为事件推送成功。([钉钉 API](https://dingtalk.apifox.cn/doc-392955?utm_source=chatgpt.com))

### 回调接口设计

回调接口建议统一入口、按事件类型分发。这样可以减少钉钉后台配置项，也便于统一处理签名校验、解密、幂等、日志和异常。

推荐接口路径如下：

| 接口路径                        | 方法   | 说明                                     |
| ------------------------------- | ------ | ---------------------------------------- |
| `/api/dingtalk/callback/event`  | `POST` | 钉钉通用事件回调入口                     |
| `/api/dingtalk/callback/health` | `GET`  | 本地健康检查接口                         |
| `/api/dingtalk/callback/test`   | `POST` | 本地模拟回调测试接口，生产环境不建议保留 |

回调请求对象如下。

文件位置：`src/main/java/io/github/atengk/dingtalk/callback/DingtalkCallbackRequest.java`

```java
package io.github.atengk.dingtalk.callback;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 钉钉回调请求体
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Data
public class DingtalkCallbackRequest {

    /**
     * 加密后的事件内容
     */
    @NotBlank(message = "钉钉回调加密内容不能为空")
    private String encrypt;

}
```

回调控制器负责读取 URL 参数和请求体，不直接编写具体业务逻辑。

文件位置：`src/main/java/io/github/atengk/dingtalk/callback/DingtalkCallbackController.java`

```java
package io.github.atengk.dingtalk.callback;

import cn.hutool.core.util.StrUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 钉钉事件回调控制器
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Validated
@RestController
@RequestMapping("/api/dingtalk/callback")
@RequiredArgsConstructor
public class DingtalkCallbackController {

    private final DingtalkCallbackService dingtalkCallbackService;

    /**
     * 接收钉钉事件回调
     *
     * @param servletRequest HTTP 请求
     * @param callbackRequest 回调请求体
     * @return 加密后的 success 响应
     */
    @PostMapping("/event")
    public Map<String, String> receiveEvent(HttpServletRequest servletRequest,
                                            @RequestBody DingtalkCallbackRequest callbackRequest) {
        String signature = firstNotBlank(
                servletRequest.getParameter("msg_signature"),
                servletRequest.getParameter("signature")
        );
        String timestamp = firstNotBlank(
                servletRequest.getParameter("timeStamp"),
                servletRequest.getParameter("timestamp")
        );
        String nonce = servletRequest.getParameter("nonce");

        return dingtalkCallbackService.handleCallback(signature, timestamp, nonce, callbackRequest.getEncrypt());
    }

    /**
     * 回调服务健康检查
     *
     * @return 检查结果
     */
    @GetMapping("/health")
    public String health() {
        return "ok";
    }

    /**
     * 获取第一个非空字符串
     *
     * @param first 第一个值
     * @param second 第二个值
     * @return 非空字符串
     */
    private String firstNotBlank(String first, String second) {
        return StrUtil.isNotBlank(first) ? first : second;
    }

}
```

钉钉回调服务接口如下。

文件位置：`src/main/java/io/github/atengk/dingtalk/callback/DingtalkCallbackService.java`

```java
package io.github.atengk.dingtalk.callback;

import java.util.Map;

/**
 * 钉钉回调服务
 *
 * @author Ateng
 * @since 2026-05-07
 */
public interface DingtalkCallbackService {

    /**
     * 处理钉钉回调
     *
     * @param signature 签名
     * @param timestamp 时间戳
     * @param nonce 随机字符串
     * @param encrypt 加密内容
     * @return 加密后的 success 响应
     */
    Map<String, String> handleCallback(String signature, String timestamp, String nonce, String encrypt);

}
```

### 签名校验

签名校验用于确认回调请求确实来自钉钉。钉钉事件回调的签名规则为：将 `token`、`timestamp`、`nonce`、`encrypt` 按字典序排序后拼接，再计算 SHA1 摘要，与请求中的签名字段进行比较；钉钉文档同时说明该校验逻辑已经包含在钉钉提供的加解密库中，开发者通常不需要自行实现。([钉钉 API](https://dingtalk.apifox.cn/doc-392461?utm_source=chatgpt.com))

在 Java 项目中，推荐直接引入钉钉提供的 `DingCallbackCrypto` 加解密类。钉钉 HTTP 推送服务端文档提供了 Java 版 `DingCallbackCrypto` 示例类，依赖 Apache Commons Codec。([钉钉 API](https://dingtalk.apifox.cn/doc-3646244?utm_source=chatgpt.com))

需要补充依赖：

```xml
<!-- 钉钉回调加解密工具类依赖，用于 Base64、SHA1 等处理 -->
<dependency>
    <groupId>commons-codec</groupId>
    <artifactId>commons-codec</artifactId>
    <version>1.17.1</version>
</dependency>
```

建议将钉钉提供的 `DingCallbackCrypto` 类放入以下目录：

```text
src/main/java/io/github/atengk/dingtalk/common/crypto/DingCallbackCrypto.java
```

下面的工具服务负责创建 `DingCallbackCrypto` 实例，并封装解密和成功响应加密逻辑。

文件位置：`src/main/java/io/github/atengk/dingtalk/callback/DingtalkCallbackCryptoService.java`

```java
package io.github.atengk.dingtalk.callback;

import cn.hutool.core.lang.Assert;
import io.github.atengk.dingtalk.common.crypto.DingCallbackCrypto;
import io.github.atengk.dingtalk.config.DingtalkProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 钉钉回调加解密服务
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Component
@RequiredArgsConstructor
public class DingtalkCallbackCryptoService {

    private final DingtalkProperties dingtalkProperties;

    /**
     * 解密钉钉回调消息
     *
     * @param signature 签名
     * @param timestamp 时间戳
     * @param nonce 随机字符串
     * @param encrypt 加密内容
     * @return 解密后的明文 JSON
     */
    public String decryptCallback(String signature, String timestamp, String nonce, String encrypt) {
        Assert.notBlank(signature, "钉钉回调签名不能为空");
        Assert.notBlank(timestamp, "钉钉回调时间戳不能为空");
        Assert.notBlank(nonce, "钉钉回调随机字符串不能为空");
        Assert.notBlank(encrypt, "钉钉回调加密内容不能为空");

        try {
            DingCallbackCrypto callbackCrypto = buildCallbackCrypto();
            return callbackCrypto.getDecryptMsg(signature, timestamp, nonce, encrypt);
        } catch (Exception e) {
            throw new IllegalStateException("钉钉回调消息解密失败", e);
        }
    }

    /**
     * 构建加密后的 success 响应
     *
     * @return 加密响应
     */
    public Map<String, String> buildSuccessResponse() {
        try {
            DingCallbackCrypto callbackCrypto = buildCallbackCrypto();
            return callbackCrypto.getEncryptedMap("success");
        } catch (Exception e) {
            throw new IllegalStateException("构建钉钉回调 success 响应失败", e);
        }
    }

    /**
     * 构建钉钉回调加解密对象
     *
     * @return 加解密对象
     */
    private DingCallbackCrypto buildCallbackCrypto() {
        String token = dingtalkProperties.getCallback().getToken();
        String aesKey = dingtalkProperties.getCallback().getAesKey();
        String ownerKey = dingtalkProperties.getCorpId();

        Assert.notBlank(token, "钉钉回调 Token 不能为空");
        Assert.notBlank(aesKey, "钉钉回调 AES Key 不能为空");
        Assert.notBlank(ownerKey, "钉钉回调 ownerKey 不能为空");

        return new DingCallbackCrypto(token, aesKey, ownerKey);
    }

}
```

这里的 `ownerKey` 需要按应用类型选择。企业内部应用的常见配置为企业 `CorpId`，第三方企业应用通常使用 `SuiteKey`；具体以钉钉后台事件订阅方式和官方加解密类说明为准。钉钉回调加解密资料中也说明，`DingCallbackCrypto` 构造参数通常包括 `token`、`aesKey` 和 `ownerKey`。([cnblogs.com](https://www.cnblogs.com/ooo0/p/14498866.html?utm_source=chatgpt.com))

### 事件解密与分发

事件解密与分发用于将钉钉加密事件转换为业务系统可处理的 JSON，并根据 `EventType` 调用不同的处理器。常见事件包括回调地址校验、用户变更、部门变更、审批实例变更等。

推荐分发规则如下：

| EventType              | 说明             | 处理建议                         |
| ---------------------- | ---------------- | -------------------------------- |
| `check_url`            | 回调地址校验事件 | 只记录日志并返回 success         |
| `bpms_instance_change` | 审批实例变更事件 | 根据审批实例 ID 更新本地审批状态 |
| `user_add_org`         | 用户加入组织事件 | 触发用户同步                     |
| `user_modify_org`      | 用户信息修改事件 | 更新本地用户信息                 |
| `user_leave_org`       | 用户离职事件     | 禁用或解绑本地账号               |
| `org_dept_create`      | 部门创建事件     | 同步新增部门                     |
| `org_dept_modify`      | 部门修改事件     | 更新本地部门                     |
| `org_dept_remove`      | 部门删除事件     | 标记本地部门失效                 |

下面的事件处理器接口用于统一不同事件的处理方式。

文件位置：`src/main/java/io/github/atengk/dingtalk/callback/DingtalkEventHandler.java`

```java
package io.github.atengk.dingtalk.callback;

import cn.hutool.json.JSONObject;

/**
 * 钉钉事件处理器
 *
 * @author Ateng
 * @since 2026-05-07
 */
public interface DingtalkEventHandler {

    /**
     * 判断是否支持当前事件类型
     *
     * @param eventType 事件类型
     * @return 是否支持
     */
    boolean supports(String eventType);

    /**
     * 处理钉钉事件
     *
     * @param eventJson 事件 JSON
     */
    void handle(JSONObject eventJson);

}
```

下面的审批事件处理器专门处理 `bpms_instance_change` 事件。

文件位置：`src/main/java/io/github/atengk/dingtalk/callback/ApprovalInstanceChangeHandler.java`

```java
package io.github.atengk.dingtalk.callback;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 钉钉审批实例变更事件处理器
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Component
public class ApprovalInstanceChangeHandler implements DingtalkEventHandler {

    private static final String EVENT_TYPE = "bpms_instance_change";

    /**
     * 判断是否支持当前事件类型
     *
     * @param eventType 事件类型
     * @return 是否支持
     */
    @Override
    public boolean supports(String eventType) {
        return StrUtil.equals(EVENT_TYPE, eventType);
    }

    /**
     * 处理审批实例变更事件
     *
     * @param eventJson 事件 JSON
     */
    @Override
    public void handle(JSONObject eventJson) {
        String processInstanceId = eventJson.getStr("processInstanceId");
        String processCode = eventJson.getStr("processCode");
        String type = eventJson.getStr("type");
        String title = eventJson.getStr("title");
        String staffId = eventJson.getStr("staffId");

        log.info("接收到钉钉审批实例变更事件，processInstanceId={}，processCode={}，type={}，title={}，staffId={}",
                processInstanceId, processCode, type, title, staffId);

        // 这里建议执行以下业务逻辑：
        // 1. 根据 processInstanceId 查询本地审批关联表
        // 2. 做幂等校验，避免重复回调导致重复更新
        // 3. 调用审批详情接口查询最终状态
        // 4. 更新本地业务单据状态
        // 5. 记录回调事件日志
    }

}
```

下面的默认事件处理器用于兜底处理暂未实现的事件类型。

文件位置：`src/main/java/io/github/atengk/dingtalk/callback/DefaultDingtalkEventHandler.java`

```java
package io.github.atengk.dingtalk.callback;

import cn.hutool.json.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 钉钉默认事件处理器
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Component
public class DefaultDingtalkEventHandler implements DingtalkEventHandler {

    /**
     * 判断是否支持当前事件类型
     *
     * @param eventType 事件类型
     * @return 是否支持
     */
    @Override
    public boolean supports(String eventType) {
        return true;
    }

    /**
     * 处理默认事件
     *
     * @param eventJson 事件 JSON
     */
    @Override
    public void handle(JSONObject eventJson) {
        log.info("接收到未单独处理的钉钉事件，eventJson={}", eventJson);
    }

}
```

下面的回调服务实现完成解密、事件识别、分发和成功响应。

文件位置：`src/main/java/io/github/atengk/dingtalk/callback/DingtalkCallbackServiceImpl.java`

```java
package io.github.atengk.dingtalk.callback;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 钉钉回调服务实现
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DingtalkCallbackServiceImpl implements DingtalkCallbackService {

    private final DingtalkCallbackCryptoService dingtalkCallbackCryptoService;
    private final List<DingtalkEventHandler> dingtalkEventHandlers;

    /**
     * 处理钉钉回调
     *
     * @param signature 签名
     * @param timestamp 时间戳
     * @param nonce 随机字符串
     * @param encrypt 加密内容
     * @return 加密后的 success 响应
     */
    @Override
    public Map<String, String> handleCallback(String signature, String timestamp, String nonce, String encrypt) {
        String decryptMsg = dingtalkCallbackCryptoService.decryptCallback(signature, timestamp, nonce, encrypt);
        JSONObject eventJson = JSONUtil.parseObj(decryptMsg);
        String eventType = eventJson.getStr("EventType");

        log.info("钉钉回调事件解密成功，eventType={}", eventType);

        if (StrUtil.isBlank(eventType)) {
            log.warn("钉钉回调事件类型为空，eventJson={}", eventJson);
            return dingtalkCallbackCryptoService.buildSuccessResponse();
        }

        dispatchEvent(eventType, eventJson);
        return dingtalkCallbackCryptoService.buildSuccessResponse();
    }

    /**
     * 分发钉钉事件
     *
     * @param eventType 事件类型
     * @param eventJson 事件 JSON
     */
    private void dispatchEvent(String eventType, JSONObject eventJson) {
        if (CollUtil.isEmpty(dingtalkEventHandlers)) {
            log.warn("钉钉事件处理器为空，eventType={}", eventType);
            return;
        }

        dingtalkEventHandlers.stream()
                .filter(handler -> handler.supports(eventType))
                .findFirst()
                .ifPresent(handler -> handler.handle(eventJson));
    }

}
```

回调接口验证可以先使用钉钉开放平台的“测试回调 URL”能力。钉钉测试回调会推送 `check_url` 事件，服务端需要解密请求并返回加密后的 `success`，否则钉钉会认为回调地址不可用。([钉钉 API](https://dingtalk.apifox.cn/doc-392969?utm_source=chatgpt.com))

回调模块开发完成后，需要重点验证以下内容：

| 验证项                  | 说明                                       |
| ----------------------- | ------------------------------------------ |
| 回调 URL 是否公网可访问 | 钉钉 HTTP 回调必须能访问后端地址           |
| Token 是否一致          | Token 不一致会导致签名校验失败             |
| AES Key 是否一致        | AES Key 不一致会导致解密失败               |
| ownerKey 是否正确       | 企业内部应用、第三方应用的 ownerKey 不同   |
| 是否返回加密 success    | 未按要求返回会被钉钉判定推送失败           |
| 是否做幂等处理          | 钉钉可能重试推送，同一事件不能重复执行业务 |
| 是否记录原始事件        | 建议保存解密后的事件 JSON，便于排查        |
| 是否异步处理耗时逻辑    | 回调接口应快速返回，复杂业务建议异步处理   |



## 接口封装设计

本章节用于统一封装钉钉 OpenAPI 的调用方式，避免业务代码中直接散落 HTTP 地址、AccessToken 拼接、响应解析、错误码判断和异常处理逻辑。良好的接口封装可以降低后续用户、部门、消息、审批、回调等模块的重复代码量，也便于统一增加日志、重试、超时控制和问题排查能力。

钉钉接口调用失败时，排查信息通常需要包含应用标识、调用时间、接口 URL、脱敏后的请求参数、返回错误信息和 `RequestID` 等内容，因此在封装层应尽量保留响应原文和关键追踪字段，便于定位问题。([open-dingtalk.github.io](https://open-dingtalk.github.io/developerpedia/docs/develop/best-practices/openapi-exception?utm_source=chatgpt.com))

### 钉钉客户端封装

钉钉客户端封装用于屏蔽具体 HTTP 请求细节，对业务层暴露清晰的方法。例如用户模块只关心“获取用户详情”，消息模块只关心“发送工作通知”，不应该在业务 Service 中直接拼接 URL 和 AccessToken。

推荐按模块拆分客户端：

| 客户端类                        | 负责范围                                 |
| ------------------------------- | ---------------------------------------- |
| `DingtalkUserClient`            | 用户信息、部门用户、用户详情             |
| `DingtalkDepartmentClient`      | 部门详情、子部门列表、部门树             |
| `DingtalkMessageClient`         | 工作通知、发送结果查询                   |
| `DingtalkRobotClient`           | 群机器人消息                             |
| `DingtalkApprovalClient`        | 审批实例创建、审批状态查询               |
| `DingtalkCallbackCryptoService` | 回调签名校验、事件解密、success 加密响应 |

为了避免每个客户端都重复处理请求，建议抽取一个底层通用请求执行器。业务客户端只负责组装参数和解释业务字段，HTTP 调用、AccessToken 注入、日志、响应校验交给统一工具完成。

推荐目录结构如下：

```text
src/main/java/io/github/atengk/dingtalk
├── client
│   ├── DingtalkRequestExecutor.java
│   ├── DingtalkApiResponse.java
│   └── DingtalkApiException.java
├── contact
│   ├── DingtalkUserClient.java
│   └── DingtalkDepartmentClient.java
├── message
│   ├── DingtalkMessageClient.java
│   └── DingtalkRobotClient.java
├── approval
│   └── DingtalkApprovalClient.java
└── callback
    └── DingtalkCallbackCryptoService.java
```

下面的响应对象用于统一承接钉钉接口返回结果。旧版钉钉 OpenAPI 常见返回字段包括 `errcode`、`errmsg` 和业务字段；新版接口可能使用 HTTP 状态码、错误码或不同响应结构，因此统一响应对象中保留 `rawBody` 方便后续扩展。

文件位置：`src/main/java/io/github/atengk/dingtalk/client/DingtalkApiResponse.java`

```java
package io.github.atengk.dingtalk.client;

import cn.hutool.json.JSONObject;
import lombok.Data;

/**
 * 钉钉接口统一响应对象
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Data
public class DingtalkApiResponse {

    /**
     * 钉钉错误码，旧版接口通常为 errcode
     */
    private Integer errcode;

    /**
     * 钉钉错误消息，旧版接口通常为 errmsg
     */
    private String errmsg;

    /**
     * 请求追踪 ID
     */
    private String requestId;

    /**
     * 原始响应内容
     */
    private String rawBody;

    /**
     * 原始 JSON 响应
     */
    private JSONObject jsonObject;

    /**
     * 判断接口是否成功
     *
     * @return 是否成功
     */
    public boolean isSuccess() {
        return Integer.valueOf(0).equals(errcode);
    }

}
```

下面的异常类用于统一封装钉钉接口调用失败信息，便于全局异常处理器识别和输出。

文件位置：`src/main/java/io/github/atengk/dingtalk/client/DingtalkApiException.java`

```java
package io.github.atengk.dingtalk.client;

import lombok.Getter;

/**
 * 钉钉接口异常
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Getter
public class DingtalkApiException extends RuntimeException {

    /**
     * 钉钉错误码
     */
    private final Integer errcode;

    /**
     * 钉钉错误消息
     */
    private final String errmsg;

    /**
     * 请求追踪 ID
     */
    private final String requestId;

    /**
     * 创建钉钉接口异常
     *
     * @param message 异常消息
     * @param errcode 错误码
     * @param errmsg 错误消息
     * @param requestId 请求追踪 ID
     */
    public DingtalkApiException(String message, Integer errcode, String errmsg, String requestId) {
        super(message);
        this.errcode = errcode;
        this.errmsg = errmsg;
        this.requestId = requestId;
    }

}
```

业务客户端调用示例。该客户端只负责传入接口地址和请求参数，不再直接处理 HTTP 细节。

文件位置：`src/main/java/io/github/atengk/dingtalk/contact/DingtalkUserClient.java`

```java
package io.github.atengk.dingtalk.contact;

import cn.hutool.core.lang.Assert;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import io.github.atengk.dingtalk.client.DingtalkRequestExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 钉钉用户客户端
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DingtalkUserClient {

    private static final String GET_USER_DETAIL_URL = "https://oapi.dingtalk.com/topapi/v2/user/get";

    private final DingtalkRequestExecutor dingtalkRequestExecutor;

    /**
     * 获取钉钉用户详情
     *
     * @param userId 钉钉 userid
     * @return 用户详情
     */
    public JSONObject getUserDetail(String userId) {
        Assert.notBlank(userId, "钉钉 userid 不能为空");

        JSONObject requestBody = JSONUtil.createObj()
                .set("userid", userId)
                .set("language", "zh_CN");

        JSONObject result = dingtalkRequestExecutor.postJsonWithAccessToken(GET_USER_DETAIL_URL, requestBody)
                .getJsonObject()
                .getJSONObject("result");

        log.info("获取钉钉用户详情成功，userId={}", userId);
        return result;
    }

}
```

这种封装方式可以保证后续新增钉钉接口时，只需要新增对应业务客户端方法，不需要重复编写 AccessToken、HTTP 请求和错误处理逻辑。

### 通用请求工具封装

通用请求工具负责统一执行钉钉接口调用，包含 URL 拼接、AccessToken 注入、请求体序列化、响应体解析、耗时日志、错误码判断和异常抛出。

钉钉开发者百科说明，调用开放接口超时时，旧版 OpenAPI 可能返回 `15` 或特定 `88` 鉴权异常，新版 OpenAPI 超时可能表现为 HTTP `504`；对于网络抖动或临时性错误，可以设计超时重试机制，但要避免高并发场景下瞬时集中重试。([钉钉开发者百科学](https://opensource.dingtalk.com/developerpedia/docs/develop/best-practices/retry/?utm_source=chatgpt.com))

下面的请求执行器使用 Hutool `HttpRequest` 发起请求，使用 `JSONUtil` 解析响应，并将失败响应转换为统一异常。

文件位置：`src/main/java/io/github/atengk/dingtalk/client/DingtalkRequestExecutor.java`

```java
package io.github.atengk.dingtalk.client;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.ContentType;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import io.github.atengk.dingtalk.token.DingtalkTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 钉钉通用请求执行器
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DingtalkRequestExecutor {

    private static final int DEFAULT_TIMEOUT = 10000;

    private final DingtalkTokenService dingtalkTokenService;

    /**
     * 使用 AccessToken 发送 JSON POST 请求
     *
     * @param url 请求地址
     * @param requestBody 请求体
     * @return 钉钉统一响应
     */
    public DingtalkApiResponse postJsonWithAccessToken(String url, JSONObject requestBody) {
        Assert.notBlank(url, "钉钉接口地址不能为空");

        String accessToken = dingtalkTokenService.getAccessToken();
        String requestUrl = appendAccessToken(url, accessToken);

        return postJson(requestUrl, requestBody);
    }

    /**
     * 发送 JSON POST 请求
     *
     * @param url 请求地址
     * @param requestBody 请求体
     * @return 钉钉统一响应
     */
    public DingtalkApiResponse postJson(String url, JSONObject requestBody) {
        Assert.notBlank(url, "钉钉接口地址不能为空");

        long startTime = System.currentTimeMillis();
        String responseBody;

        try {
            responseBody = HttpRequest.post(url)
                    .contentType(ContentType.JSON.getValue())
                    .body(requestBody == null ? "{}" : requestBody.toString())
                    .timeout(DEFAULT_TIMEOUT)
                    .execute()
                    .body();
        } catch (Exception e) {
            log.error("钉钉接口请求异常，url={}，耗时={}ms", url, System.currentTimeMillis() - startTime, e);
            throw new IllegalStateException("钉钉接口请求异常", e);
        }

        long costTime = System.currentTimeMillis() - startTime;
        log.info("钉钉接口请求完成，url={}，耗时={}ms", url, costTime);

        return parseAndCheckResponse(responseBody);
    }

    /**
     * 拼接 AccessToken
     *
     * @param url 原始 URL
     * @param accessToken 访问凭证
     * @return 拼接后的 URL
     */
    private String appendAccessToken(String url, String accessToken) {
        Assert.notBlank(accessToken, "钉钉 AccessToken 不能为空");
        String separator = url.contains("?") ? "&" : "?";
        return url + separator + "access_token=" + accessToken;
    }

    /**
     * 解析并校验响应
     *
     * @param responseBody 响应内容
     * @return 钉钉统一响应
     */
    private DingtalkApiResponse parseAndCheckResponse(String responseBody) {
        if (StrUtil.isBlank(responseBody)) {
            throw new IllegalStateException("钉钉接口响应为空");
        }

        JSONObject responseJson = JSONUtil.parseObj(responseBody);

        DingtalkApiResponse response = new DingtalkApiResponse();
        response.setErrcode(responseJson.getInt("errcode", 0));
        response.setErrmsg(responseJson.getStr("errmsg"));
        response.setRequestId(firstNotBlank(
                responseJson.getStr("request_id"),
                responseJson.getStr("requestId")
        ));
        response.setRawBody(responseBody);
        response.setJsonObject(responseJson);

        if (!response.isSuccess()) {
            log.error("钉钉接口返回失败，errcode={}，errmsg={}，requestId={}",
                    response.getErrcode(), response.getErrmsg(), response.getRequestId());

            throw new DingtalkApiException(
                    "钉钉接口返回失败：" + response.getErrmsg(),
                    response.getErrcode(),
                    response.getErrmsg(),
                    response.getRequestId()
            );
        }

        return response;
    }

    /**
     * 获取第一个非空字符串
     *
     * @param first 第一个值
     * @param second 第二个值
     * @return 非空字符串
     */
    private String firstNotBlank(String first, String second) {
        return StrUtil.isNotBlank(first) ? first : second;
    }

}
```

该工具类适合旧版 `oapi.dingtalk.com` 接口。如果后续使用新版 `api.dingtalk.com` 接口，并通过请求头传递 `x-acs-dingtalk-access-token`，可以在该类中继续增加 `postJsonWithHeaderToken` 方法，而不是改动所有业务客户端。

### 统一响应结果处理

统一响应结果处理用于规范系统内部接口返回给前端或其他服务的格式，避免不同 Controller 返回结构不一致。钉钉接口的原始响应不建议直接透传给前端，应该由后端转换为业务系统统一响应对象。

推荐统一返回结构如下：

| 字段        | 说明             |
| ----------- | ---------------- |
| `code`      | 本系统业务状态码 |
| `message`   | 本系统提示消息   |
| `data`      | 响应数据         |
| `success`   | 是否成功         |
| `timestamp` | 响应时间戳       |

文件位置：`src/main/java/io/github/atengk/dingtalk/common/Result.java`

```java
package io.github.atengk.dingtalk.common;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 统一响应结果
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Data
public class Result<T> {

    /**
     * 是否成功
     */
    private Boolean success;

    /**
     * 响应状态码
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
     * 响应时间
     */
    private LocalDateTime timestamp;

    /**
     * 成功响应
     *
     * @param data 响应数据
     * @param <T> 数据类型
     * @return 统一响应
     */
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setSuccess(Boolean.TRUE);
        result.setCode("200");
        result.setMessage("操作成功");
        result.setData(data);
        result.setTimestamp(LocalDateTime.now());
        return result;
    }

    /**
     * 失败响应
     *
     * @param code 错误码
     * @param message 错误消息
     * @param <T> 数据类型
     * @return 统一响应
     */
    public static <T> Result<T> fail(String code, String message) {
        Result<T> result = new Result<>();
        result.setSuccess(Boolean.FALSE);
        result.setCode(code);
        result.setMessage(message);
        result.setTimestamp(LocalDateTime.now());
        return result;
    }

}
```

Controller 层示例：

```java
package io.github.atengk.dingtalk.contact;

import cn.hutool.json.JSONObject;
import io.github.atengk.dingtalk.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 钉钉用户接口
 *
 * @author Ateng
 * @since 2026-05-07
 */
@RestController
@RequestMapping("/api/dingtalk/users")
@RequiredArgsConstructor
public class DingtalkUserController {

    private final DingtalkUserClient dingtalkUserClient;

    /**
     * 查询钉钉用户详情
     *
     * @param userId 钉钉 userid
     * @return 用户详情
     */
    @GetMapping("/{userId}")
    public Result<JSONObject> getUserDetail(@PathVariable String userId) {
        return Result.success(dingtalkUserClient.getUserDetail(userId));
    }

}
```

统一响应对象应只用于本系统对外接口。钉钉回调接口不要强制套用该结构，因为钉钉要求回调接口按其协议返回加密后的 `success` 响应。

### 异常处理机制

异常处理机制用于统一捕获钉钉接口异常、参数校验异常、业务异常和系统异常，并转换为前端可识别的响应结构。这样可以避免堆栈信息直接暴露给调用方，同时保留服务端日志用于排查。

建议异常分类如下：

| 异常类型                          | 处理方式                            |
| --------------------------------- | ----------------------------------- |
| `DingtalkApiException`            | 记录钉钉错误码、错误消息、RequestID |
| `IllegalArgumentException`        | 返回参数错误                        |
| `MethodArgumentNotValidException` | 返回参数校验失败                    |
| `IllegalStateException`           | 返回业务状态异常                    |
| `Exception`                       | 返回系统异常，并记录完整日志        |

文件位置：`src/main/java/io/github/atengk/dingtalk/common/GlobalExceptionHandler.java`

```java
package io.github.atengk.dingtalk.common;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.dingtalk.client.DingtalkApiException;
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
     * 处理钉钉接口异常
     *
     * @param e 钉钉接口异常
     * @return 统一响应
     */
    @ExceptionHandler(DingtalkApiException.class)
    public Result<Void> handleDingtalkApiException(DingtalkApiException e) {
        log.error("钉钉接口调用失败，errcode={}，errmsg={}，requestId={}",
                e.getErrcode(), e.getErrmsg(), e.getRequestId(), e);

        String message = StrUtil.isBlank(e.getRequestId())
                ? e.getErrmsg()
                : e.getErrmsg() + "，requestId=" + e.getRequestId();

        return Result.fail("DINGTALK_API_ERROR", message);
    }

    /**
     * 处理方法参数校验异常
     *
     * @param e 参数校验异常
     * @return 统一响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + "：" + error.getDefaultMessage())
                .orElse("参数校验失败");

        log.warn("请求参数校验失败，message={}", message);
        return Result.fail("PARAM_VALID_ERROR", message);
    }

    /**
     * 处理绑定异常
     *
     * @param e 绑定异常
     * @return 统一响应
     */
    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + "：" + error.getDefaultMessage())
                .orElse("参数绑定失败");

        log.warn("请求参数绑定失败，message={}", message);
        return Result.fail("PARAM_BIND_ERROR", message);
    }

    /**
     * 处理业务状态异常
     *
     * @param e 业务状态异常
     * @return 统一响应
     */
    @ExceptionHandler(IllegalStateException.class)
    public Result<Void> handleIllegalStateException(IllegalStateException e) {
        log.warn("业务状态异常，message={}", e.getMessage());
        return Result.fail("BUSINESS_ERROR", e.getMessage());
    }

    /**
     * 处理系统异常
     *
     * @param e 系统异常
     * @return 统一响应
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return Result.fail("SYSTEM_ERROR", "系统异常，请联系管理员");
    }

}
```

异常处理时需要注意，日志中可以记录 `requestId`、接口名称、错误码和业务单据号，但不要输出 `ClientSecret`、AccessToken、手机号、消息正文等敏感信息。钉钉开发者百科的问题反馈建议也明确要求对敏感信息进行脱敏处理，避免泄露应用和用户数据。([open-dingtalk.github.io](https://open-dingtalk.github.io/developerpedia/docs/develop/best-practices/openapi-exception?utm_source=chatgpt.com))

## 数据存储设计

本章节用于设计钉钉集成模块需要落库的数据表，包括用户绑定表、消息发送记录表、审批业务关联表和回调事件日志表。数据表设计的目标不是替代钉钉平台数据，而是保存本地业务系统与钉钉之间的关联关系、调用记录和可追踪日志。

建议所有表统一包含 `create_time`、`update_time` 字段；涉及状态流转的数据表应包含状态字段；涉及外部接口调用的数据表应保存外部 ID、失败原因和重试信息。

### 用户绑定表

用户绑定表用于保存本地系统用户与钉钉用户之间的绑定关系。该表是钉钉免登、工作通知、审批发起和通讯录同步的基础表。

设计要点如下：

| 设计项       | 说明                                     |
| ------------ | ---------------------------------------- |
| 本地用户 ID  | 关联本系统用户主键                       |
| 钉钉 userid  | 企业内部应用中最常用的用户标识           |
| 钉钉 unionid | 跨应用或开放平台场景下的用户标识         |
| 手机号       | 可用于辅助匹配，但不建议作为唯一绑定依据 |
| 绑定状态     | 支持解绑、禁用、重新绑定                 |
| 最近同步时间 | 用于判断是否需要重新同步用户信息         |

建表语句如下：

```sql
CREATE TABLE sys_user_dingtalk_bind (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID',
    system_user_id BIGINT NOT NULL COMMENT '本地系统用户 ID',
    dingtalk_user_id VARCHAR(128) NOT NULL COMMENT '钉钉 userid',
    dingtalk_union_id VARCHAR(128) DEFAULT NULL COMMENT '钉钉 unionid',
    dingtalk_name VARCHAR(128) DEFAULT NULL COMMENT '钉钉用户姓名',
    mobile VARCHAR(32) DEFAULT NULL COMMENT '手机号',
    email VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
    avatar VARCHAR(512) DEFAULT NULL COMMENT '头像地址',
    dept_id BIGINT DEFAULT NULL COMMENT '钉钉主部门 ID',
    dept_name VARCHAR(128) DEFAULT NULL COMMENT '钉钉主部门名称',
    bind_status TINYINT NOT NULL DEFAULT 1 COMMENT '绑定状态：1已绑定，0已解绑，2已禁用',
    bind_time DATETIME NOT NULL COMMENT '绑定时间',
    unbind_time DATETIME DEFAULT NULL COMMENT '解绑时间',
    last_sync_time DATETIME DEFAULT NULL COMMENT '最近同步时间',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间',
    UNIQUE KEY uk_system_user_id (system_user_id),
    UNIQUE KEY uk_dingtalk_user_id (dingtalk_user_id),
    KEY idx_dingtalk_union_id (dingtalk_union_id),
    KEY idx_mobile (mobile),
    KEY idx_dept_id (dept_id)
) COMMENT='系统用户与钉钉用户绑定表';
```

字段说明如下：

| 字段                | 说明                                      |
| ------------------- | ----------------------------------------- |
| `system_user_id`    | 本地系统用户 ID，一般与系统用户表主键关联 |
| `dingtalk_user_id`  | 钉钉 `userid`，发送工作通知时常用         |
| `dingtalk_union_id` | 钉钉 `unionid`，用于跨应用身份识别        |
| `bind_status`       | 绑定状态，用于处理离职、解绑、禁用等场景  |
| `last_sync_time`    | 最近同步时间，用于增量同步判断            |

不建议仅依赖姓名进行绑定。企业中重名较常见，应优先使用 `userid`、`unionid`、手机号、工号等稳定字段组合判断。

### 消息发送记录表

消息发送记录表用于记录工作通知、群机器人消息和后续可能扩展的应用机器人消息。该表可以帮助排查消息是否发送、何时发送、发送给谁、钉钉是否返回任务 ID，以及失败原因是什么。

设计要点如下：

| 设计项   | 说明                                |
| -------- | ----------------------------------- |
| 消息类型 | 区分工作通知、群机器人、应用机器人  |
| 接收对象 | 保存 userid、群标识或业务接收人描述 |
| 消息内容 | 保存发送内容，敏感业务可只保存摘要  |
| 任务 ID  | 工作通知成功后返回的钉钉任务 ID     |
| 发送状态 | 保存待发送、成功、失败、未知等状态  |
| 失败原因 | 保存接口异常或钉钉错误信息          |
| 重试次数 | 支持后续补偿重试                    |

建表语句如下：

```sql
CREATE TABLE dingtalk_message_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID',
    business_type VARCHAR(64) DEFAULT NULL COMMENT '业务类型，例如 APPROVAL、ORDER、ALERT',
    business_id VARCHAR(128) DEFAULT NULL COMMENT '业务 ID',
    message_type VARCHAR(32) NOT NULL COMMENT '消息类型：WORK_NOTICE、ROBOT、APP_ROBOT',
    receiver VARCHAR(1024) DEFAULT NULL COMMENT '接收人或接收群',
    receiver_type VARCHAR(32) DEFAULT NULL COMMENT '接收类型：USER、DEPT、ALL、GROUP',
    title VARCHAR(255) DEFAULT NULL COMMENT '消息标题',
    content TEXT NOT NULL COMMENT '消息内容',
    content_digest VARCHAR(128) DEFAULT NULL COMMENT '消息摘要',
    task_id BIGINT DEFAULT NULL COMMENT '钉钉工作通知任务 ID',
    send_status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '发送状态：PENDING、SUCCESS、FAILED、UNKNOWN',
    errcode VARCHAR(64) DEFAULT NULL COMMENT '钉钉错误码',
    errmsg VARCHAR(1024) DEFAULT NULL COMMENT '钉钉错误消息',
    request_id VARCHAR(128) DEFAULT NULL COMMENT '钉钉请求追踪 ID',
    retry_count INT NOT NULL DEFAULT 0 COMMENT '重试次数',
    next_retry_time DATETIME DEFAULT NULL COMMENT '下次重试时间',
    send_time DATETIME DEFAULT NULL COMMENT '发送时间',
    result_query_time DATETIME DEFAULT NULL COMMENT '结果查询时间',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间',
    KEY idx_business (business_type, business_id),
    KEY idx_task_id (task_id),
    KEY idx_message_type (message_type),
    KEY idx_send_status (send_status),
    KEY idx_next_retry_time (next_retry_time)
) COMMENT='钉钉消息发送记录表';
```

状态流转建议如下：

| 状态      | 说明                                 |
| --------- | ------------------------------------ |
| `PENDING` | 已创建记录，尚未完成发送             |
| `SUCCESS` | 钉钉接口调用成功或结果查询显示成功   |
| `FAILED`  | 明确发送失败                         |
| `UNKNOWN` | 网络异常、接口超时或结果暂时不可确认 |

消息内容中如果包含个人信息、订单金额、审批详情等敏感数据，建议保存摘要字段 `content_digest`，完整内容按业务合规要求决定是否落库。

### 审批业务关联表

审批业务关联表用于保存本地业务单据与钉钉审批实例之间的关系。业务系统发起审批后，必须保存 `process_instance_id`，否则后续无法根据回调事件反查本地业务单据。

设计要点如下：

| 设计项       | 说明                                     |
| ------------ | ---------------------------------------- |
| 业务类型     | 区分请假、报销、采购、合同等不同审批业务 |
| 业务 ID      | 本地业务单据 ID                          |
| 流程编码     | 钉钉审批模板 `process_code`              |
| 审批实例 ID  | 钉钉 `process_instance_id`               |
| 发起人       | 保存本地用户 ID 和钉钉 userid            |
| 审批状态     | 保存本地转换后的审批状态                 |
| 审批结果     | 保存钉钉原始结果                         |
| 最近同步时间 | 用于查询补偿和状态同步                   |

建表语句如下：

```sql
CREATE TABLE dingtalk_approval_relation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID',
    business_type VARCHAR(64) NOT NULL COMMENT '业务类型，例如 LEAVE、EXPENSE、PURCHASE',
    business_id VARCHAR(128) NOT NULL COMMENT '本地业务 ID',
    process_code VARCHAR(128) NOT NULL COMMENT '钉钉审批流程模板编码',
    process_instance_id VARCHAR(128) NOT NULL COMMENT '钉钉审批实例 ID',
    originator_system_user_id BIGINT DEFAULT NULL COMMENT '本地发起人用户 ID',
    originator_dingtalk_user_id VARCHAR(128) NOT NULL COMMENT '钉钉发起人 userid',
    dept_id VARCHAR(64) DEFAULT NULL COMMENT '发起人部门 ID',
    approval_status VARCHAR(32) NOT NULL DEFAULT 'APPROVING' COMMENT '审批状态：APPROVING、APPROVED、REJECTED、CANCELED、UNKNOWN',
    dingtalk_status VARCHAR(64) DEFAULT NULL COMMENT '钉钉原始审批状态',
    dingtalk_result VARCHAR(64) DEFAULT NULL COMMENT '钉钉原始审批结果',
    title VARCHAR(255) DEFAULT NULL COMMENT '审批标题',
    form_snapshot JSON DEFAULT NULL COMMENT '审批表单快照',
    start_time DATETIME DEFAULT NULL COMMENT '审批发起时间',
    finish_time DATETIME DEFAULT NULL COMMENT '审批完成时间',
    last_sync_time DATETIME DEFAULT NULL COMMENT '最近同步时间',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间',
    UNIQUE KEY uk_business (business_type, business_id),
    UNIQUE KEY uk_process_instance_id (process_instance_id),
    KEY idx_process_code (process_code),
    KEY idx_approval_status (approval_status),
    KEY idx_originator_dingtalk_user_id (originator_dingtalk_user_id)
) COMMENT='钉钉审批业务关联表';
```

状态映射建议如下：

| 本地状态    | 含义                   |
| ----------- | ---------------------- |
| `APPROVING` | 审批中                 |
| `APPROVED`  | 审批通过               |
| `REJECTED`  | 审批拒绝               |
| `CANCELED`  | 审批撤销或终止         |
| `UNKNOWN`   | 状态未知，需要补偿查询 |

`form_snapshot` 建议保存发起审批时的表单快照，便于后续排查“本地单据内容”和“钉钉审批表单内容”是否一致。如果数据库不支持 JSON 类型，可以改为 `TEXT` 保存 JSON 字符串。

### 回调事件日志表

回调事件日志表用于保存钉钉推送到后端的事件记录，包括加密内容摘要、解密后的事件类型、事件业务 ID、处理状态、失败原因和重试次数。该表是排查审批状态不同步、通讯录同步异常、机器人回调失败的重要依据。

钉钉回调服务端处理完成后需要返回加密后的 `success`，如果未按协议返回，平台会认为推送失败并可能触发重试；因此本地需要对回调事件做幂等处理，避免重复事件导致业务重复执行。([cnblogs.com](https://www.cnblogs.com/ooo0/p/14498866.html?utm_source=chatgpt.com))

设计要点如下：

| 设计项      | 说明                             |
| ----------- | -------------------------------- |
| 事件类型    | 保存 `EventType`                 |
| 事件业务 ID | 审批实例 ID、用户 ID、部门 ID 等 |
| 原始内容    | 保存解密后的事件 JSON            |
| 处理状态    | 记录是否处理成功                 |
| 幂等键      | 防止重复处理                     |
| 失败原因    | 保存异常信息                     |
| 重试次数    | 支持异步补偿处理                 |

建表语句如下：

```sql
CREATE TABLE dingtalk_callback_event_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID',
    event_type VARCHAR(128) NOT NULL COMMENT '钉钉事件类型',
    event_biz_id VARCHAR(128) DEFAULT NULL COMMENT '事件业务 ID，例如审批实例 ID、用户 ID、部门 ID',
    idempotent_key VARCHAR(255) NOT NULL COMMENT '幂等键',
    corp_id VARCHAR(128) DEFAULT NULL COMMENT '企业 ID',
    process_instance_id VARCHAR(128) DEFAULT NULL COMMENT '审批实例 ID',
    dingtalk_user_id VARCHAR(128) DEFAULT NULL COMMENT '钉钉 userid',
    dept_id VARCHAR(64) DEFAULT NULL COMMENT '钉钉部门 ID',
    encrypted_digest VARCHAR(128) DEFAULT NULL COMMENT '加密内容摘要',
    event_content JSON DEFAULT NULL COMMENT '解密后的事件内容',
    handle_status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '处理状态：PENDING、SUCCESS、FAILED、IGNORED',
    fail_reason VARCHAR(1024) DEFAULT NULL COMMENT '失败原因',
    retry_count INT NOT NULL DEFAULT 0 COMMENT '重试次数',
    next_retry_time DATETIME DEFAULT NULL COMMENT '下次重试时间',
    receive_time DATETIME NOT NULL COMMENT '接收时间',
    handle_time DATETIME DEFAULT NULL COMMENT '处理时间',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间',
    UNIQUE KEY uk_idempotent_key (idempotent_key),
    KEY idx_event_type (event_type),
    KEY idx_event_biz_id (event_biz_id),
    KEY idx_process_instance_id (process_instance_id),
    KEY idx_handle_status (handle_status),
    KEY idx_next_retry_time (next_retry_time)
) COMMENT='钉钉回调事件日志表';
```

幂等键建议按事件类型和关键业务字段生成。例如审批事件可以使用以下格式：

```text
bpms_instance_change:{processInstanceId}:{type}:{createTime}
```

用户事件可以使用以下格式：

```text
user_modify_org:{userid}:{createTime}
```

如果事件中没有稳定唯一字段，可以使用解密后事件 JSON 的 SHA-256 摘要作为幂等键。Hutool 示例：

```java
package io.github.atengk.dingtalk.callback;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.json.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 钉钉回调幂等键生成器
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Component
public class DingtalkCallbackIdempotentKeyGenerator {

    /**
     * 生成回调事件幂等键
     *
     * @param eventJson 事件 JSON
     * @return 幂等键
     */
    public String generate(JSONObject eventJson) {
        String eventType = eventJson.getStr("EventType", "UNKNOWN");
        String digest = SecureUtil.sha256(eventJson.toString());
        String idempotentKey = eventType + ":" + digest;

        log.info("生成钉钉回调幂等键成功，eventType={}", eventType);
        return idempotentKey;
    }

}
```

回调事件处理状态建议如下：

| 状态      | 说明                     |
| --------- | ------------------------ |
| `PENDING` | 事件已接收，尚未处理     |
| `SUCCESS` | 事件处理成功             |
| `FAILED`  | 事件处理失败，可进入补偿 |
| `IGNORED` | 重复事件或业务无需处理   |

以上四张表覆盖了钉钉集成的核心数据关系：用户绑定解决“人”的映射，消息记录解决“通知”的追踪，审批关联解决“流程”的同步，回调日志解决“事件”的审计和补偿。



## 安全与稳定性

本章节用于说明钉钉集成模块在生产环境中需要重点关注的安全与稳定性设计，包括敏感参数保护、接口重试、回调幂等和日志记录。钉钉集成属于典型的外部平台对接场景，必须避免密钥泄露、重复处理、接口超时、消息丢失和日志不可追踪等问题。

### 参数加密存储

钉钉应用参数中包含 `ClientSecret`、`AppSecret`、回调 `Token`、回调 `AES Key`、群机器人 `Secret` 等敏感信息。这些参数不能硬编码在 Java 类中，也不应该以明文形式提交到 Git 仓库。

推荐按环境选择不同的参数存储方式：

| 环境         | 推荐方式                                    | 说明                           |
| ------------ | ------------------------------------------- | ------------------------------ |
| 本地开发环境 | 本地环境变量、`.env`、`application-dev.yml` | 仅用于个人开发，不提交真实密钥 |
| 测试环境     | 配置中心、服务器环境变量                    | 由测试环境统一维护             |
| 生产环境     | 配置中心、KMS、容器 Secret                  | 敏感参数集中管控，避免明文泄露 |
| CI/CD 环境   | 构建平台密钥变量                            | 构建和部署过程动态注入         |

不推荐的做法如下：

| 做法                                 | 风险                         |
| ------------------------------------ | ---------------------------- |
| 将 `ClientSecret` 写死在 Java 常量中 | 代码泄露后密钥直接暴露       |
| 将生产密钥提交到 Git 仓库            | 历史提交记录中仍可被追溯     |
| 日志中打印完整 AccessToken           | 日志系统泄露会导致接口被滥用 |
| 将机器人 Webhook 明文暴露给前端      | 任何人都可以向群里发送消息   |
| 多个环境共用同一套钉钉应用密钥       | 测试误操作可能影响生产       |

推荐配置方式如下：

文件位置：`src/main/resources/application.yml`

```yaml
dingtalk:
  enabled: ${DINGTALK_ENABLED:true}

  # 企业 ID
  corp-id: ${DINGTALK_CORP_ID:}

  # 应用 Client ID
  client-id: ${DINGTALK_CLIENT_ID:}

  # 应用 Client Secret，生产环境通过环境变量、配置中心或 Secret 注入
  client-secret: ${DINGTALK_CLIENT_SECRET:}

  # 企业内部应用 AgentId
  agent-id: ${DINGTALK_AGENT_ID:}

  callback:
    # 回调签名 Token
    token: ${DINGTALK_CALLBACK_TOKEN:}

    # 回调 AES Key
    aes-key: ${DINGTALK_CALLBACK_AES_KEY:}

  robot:
    # 群机器人 Webhook
    webhook: ${DINGTALK_ROBOT_WEBHOOK:}

    # 群机器人加签密钥
    secret: ${DINGTALK_ROBOT_SECRET:}
```

如果项目必须将部分配置加密后存储在配置文件中，可以设计统一的敏感参数解密工具。下面示例使用 Hutool AES 工具类实现基础解密能力，实际生产环境建议优先使用企业统一 KMS 或配置中心密钥能力。

文件位置：`src/main/java/io/github/atengk/dingtalk/security/DingtalkSecretCryptoService.java`

```java
package io.github.atengk.dingtalk.security;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.symmetric.AES;
import cn.hutool.crypto.symmetric.SymmetricCrypto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * 钉钉敏感参数加解密服务
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Component
public class DingtalkSecretCryptoService {

    private static final String ENCRYPT_PREFIX = "ENC(";
    private static final String ENCRYPT_SUFFIX = ")";

    /**
     * 解密配置值
     *
     * @param value 配置值
     * @param secretKey AES 密钥
     * @return 解密后的配置值
     */
    public String decryptIfNecessary(String value, String secretKey) {
        if (StrUtil.isBlank(value)) {
            return value;
        }
        if (!StrUtil.startWith(value, ENCRYPT_PREFIX) || !StrUtil.endWith(value, ENCRYPT_SUFFIX)) {
            return value;
        }

        Assert.notBlank(secretKey, "配置解密密钥不能为空");

        String encryptedValue = StrUtil.subBetween(value, ENCRYPT_PREFIX, ENCRYPT_SUFFIX);
        SymmetricCrypto aes = new AES(secretKey.getBytes(StandardCharsets.UTF_8));
        String plainText = aes.decryptStr(encryptedValue);

        log.info("钉钉敏感配置解密完成");
        return plainText;
    }

}
```

敏感参数还需要在日志中进行脱敏处理。下面的工具类用于统一脱敏 AccessToken、ClientSecret、Webhook 等敏感字符串。

文件位置：`src/main/java/io/github/atengk/dingtalk/security/DingtalkSensitiveMaskUtil.java`

```java
package io.github.atengk.dingtalk.security;

import cn.hutool.core.util.DesensitizedUtil;
import cn.hutool.core.util.StrUtil;

/**
 * 钉钉敏感信息脱敏工具
 *
 * @author Ateng
 * @since 2026-05-07
 */
public class DingtalkSensitiveMaskUtil {

    /**
     * 脱敏普通密钥
     *
     * @param value 原始值
     * @return 脱敏值
     */
    public static String maskSecret(String value) {
        if (StrUtil.isBlank(value)) {
            return value;
        }
        return DesensitizedUtil.password(value);
    }

    /**
     * 脱敏 URL
     *
     * @param value 原始 URL
     * @return 脱敏 URL
     */
    public static String maskUrl(String value) {
        if (StrUtil.isBlank(value)) {
            return value;
        }
        int index = value.indexOf("access_token=");
        if (index < 0) {
            return value;
        }
        return value.substring(0, index) + "access_token=******";
    }

}
```

生产环境安全要求如下：

1. 密钥参数必须通过环境变量、配置中心或 Secret 注入。
2. 禁止在代码仓库中提交真实 `ClientSecret`、`AppSecret`、`AES Key`。
3. 禁止在日志中打印完整 AccessToken、Webhook、手机号、审批正文。
4. 生产环境测试接口必须删除或增加鉴权。
5. 钉钉机器人 Webhook 不允许暴露到前端。
6. 定期轮换钉钉应用密钥和机器人加签密钥。
7. 离职人员应及时移除钉钉开放平台应用管理权限。

### 接口重试机制

钉钉 OpenAPI 调用可能因为网络抖动、服务端超时、临时限流等原因失败。对于临时性异常，可以通过重试机制提升稳定性；对于参数错误、权限错误、签名错误等确定性失败，不应该反复重试。

建议重试的场景：

| 场景                 | 是否重试 | 说明                     |
| -------------------- | -------- | ------------------------ |
| 网络连接超时         | 是       | 可能是临时网络抖动       |
| HTTP 502 / 503 / 504 | 是       | 可能是上游服务暂时不可用 |
| 读取响应超时         | 是       | 可短暂重试               |
| AccessToken 临界过期 | 是       | 刷新 Token 后重试        |
| 参数错误             | 否       | 重试无法解决             |
| 权限不足             | 否       | 需要调整应用权限         |
| 用户不存在           | 否       | 需要检查业务数据         |
| 回调解密失败         | 否       | 通常是配置错误           |

重试配置建议如下：

文件位置：`src/main/resources/application.yml`

```yaml
dingtalk:
  stability:
    retry:
      # 最大重试次数，包含首次调用
      max-attempts: ${DINGTALK_RETRY_MAX_ATTEMPTS:3}

      # 每次重试间隔，单位毫秒
      interval-millis: ${DINGTALK_RETRY_INTERVAL_MILLIS:1000}

      # 单次请求超时时间，单位毫秒
      timeout-millis: ${DINGTALK_REQUEST_TIMEOUT_MILLIS:10000}
```

下面的重试执行器用于包裹钉钉接口调用。它只负责通用重试逻辑，具体哪些异常可以重试，应在调用方或异常分类中进一步控制。

文件位置：`src/main/java/io/github/atengk/dingtalk/common/retry/DingtalkRetryExecutor.java`

```java
package io.github.atengk.dingtalk.common.retry;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.thread.ThreadUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.Callable;

/**
 * 钉钉接口重试执行器
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Component
public class DingtalkRetryExecutor {

    /**
     * 执行带重试的调用
     *
     * @param operationName 操作名称
     * @param maxAttempts 最大尝试次数
     * @param intervalMillis 重试间隔毫秒数
     * @param callable 执行逻辑
     * @param <T> 返回类型
     * @return 执行结果
     */
    public <T> T execute(String operationName,
                         int maxAttempts,
                         long intervalMillis,
                         Callable<T> callable) {
        Assert.notBlank(operationName, "操作名称不能为空");
        Assert.isTrue(maxAttempts >= 1, "最大尝试次数不能小于 1");
        Assert.notNull(callable, "重试执行逻辑不能为空");

        Exception lastException = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return callable.call();
            } catch (Exception e) {
                lastException = e;
                log.warn("钉钉接口调用失败，operation={}，attempt={}/{}，message={}",
                        operationName, attempt, maxAttempts, e.getMessage());

                if (attempt < maxAttempts) {
                    ThreadUtil.sleep(intervalMillis);
                }
            }
        }

        log.error("钉钉接口重试后仍失败，operation={}，maxAttempts={}", operationName, maxAttempts, lastException);
        throw new IllegalStateException("钉钉接口调用失败：" + operationName, lastException);
    }

}
```

使用示例：

```java
String accessToken = dingtalkRetryExecutor.execute(
        "刷新钉钉 AccessToken",
        3,
        1000,
        dingtalkTokenService::refreshAccessToken
);
```

重试机制需要配合限流和幂等处理。尤其是消息发送、审批创建这类写操作，不能盲目重试，否则可能造成重复通知或重复审批实例。

### 幂等处理

幂等处理用于避免同一业务动作被重复执行。钉钉集成中常见的重复风险包括：用户重复点击发起审批按钮、消息发送接口超时后业务系统重复发送、钉钉回调失败后平台重试推送、定时补偿任务重复处理同一条记录。

需要重点做幂等的场景：

| 场景             | 幂等依据                                            |
| ---------------- | --------------------------------------------------- |
| 创建审批实例     | `business_type + business_id`                       |
| 发送业务通知     | `business_type + business_id + message_type`        |
| 处理审批回调     | `EventType + processInstanceId + type + createTime` |
| 处理用户变更事件 | `EventType + userid + createTime`                   |
| 处理部门变更事件 | `EventType + deptId + createTime`                   |
| 查询补偿任务     | 数据库记录主键或任务 ID                             |

幂等推荐使用数据库唯一索引和 Redis 短期锁组合实现。数据库唯一索引用于保证最终一致性，Redis 用于减少短时间内重复请求进入业务逻辑。

下面的服务使用 Redis `setIfAbsent` 实现短期幂等锁。

文件位置：`src/main/java/io/github/atengk/dingtalk/common/idempotent/DingtalkIdempotentService.java`

```java
package io.github.atengk.dingtalk.common.idempotent;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 钉钉幂等处理服务
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DingtalkIdempotentService {

    private static final String IDEMPOTENT_PREFIX = "dingtalk:idempotent:";

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 尝试获取幂等锁
     *
     * @param key 幂等键
     * @param expireSeconds 过期秒数
     * @return 是否获取成功
     */
    public boolean tryAcquire(String key, long expireSeconds) {
        Assert.notBlank(key, "幂等键不能为空");

        String redisKey = buildRedisKey(key);
        Boolean success = stringRedisTemplate.opsForValue()
                .setIfAbsent(redisKey, "1", Duration.ofSeconds(expireSeconds));

        if (Boolean.TRUE.equals(success)) {
            log.info("钉钉幂等锁获取成功，key={}", key);
            return true;
        }

        log.warn("钉钉幂等锁已存在，忽略重复请求，key={}", key);
        return false;
    }

    /**
     * 释放幂等锁
     *
     * @param key 幂等键
     */
    public void release(String key) {
        if (StrUtil.isBlank(key)) {
            return;
        }

        stringRedisTemplate.delete(buildRedisKey(key));
        log.info("钉钉幂等锁已释放，key={}", key);
    }

    /**
     * 构建 Redis Key
     *
     * @param key 幂等键
     * @return Redis Key
     */
    private String buildRedisKey(String key) {
        return IDEMPOTENT_PREFIX + key;
    }

}
```

审批发起幂等示例：

```java
String idempotentKey = "approval:create:" + businessType + ":" + businessId;
boolean acquired = dingtalkIdempotentService.tryAcquire(idempotentKey, 300);

if (!acquired) {
    throw new IllegalStateException("审批正在处理中，请勿重复提交");
}

try {
    String processInstanceId = dingtalkApprovalClient.createProcessInstance(request);
    // 保存 businessType、businessId、processInstanceId 的关联关系
} finally {
    dingtalkIdempotentService.release(idempotentKey);
}
```

对于审批创建这类强一致场景，仅依赖 Redis 锁仍不够，必须在数据库中增加唯一索引。例如审批业务关联表应增加 `business_type + business_id` 唯一索引，避免服务重启、Redis 失效或并发边界场景下重复创建业务关联数据。

回调幂等处理建议：

1. 解密事件后生成幂等键。
2. 先尝试插入回调事件日志表。
3. 如果唯一键冲突，说明事件已接收过，直接返回 success。
4. 如果插入成功，再执行业务处理。
5. 业务处理成功后更新事件状态为 `SUCCESS`。
6. 处理失败时记录失败原因，后续通过补偿任务重试。

### 日志记录

日志记录用于支撑接口排障、调用追踪、回调审计和业务对账。钉钉集成模块建议统一记录关键操作日志，但日志中必须避免输出敏感信息。

推荐记录的日志内容：

| 日志类型         | 记录内容                                       |
| ---------------- | ---------------------------------------------- |
| AccessToken 日志 | 刷新开始、刷新成功、刷新失败，不打印完整 Token |
| OpenAPI 调用日志 | 接口名称、耗时、错误码、RequestID              |
| 消息通知日志     | 消息类型、接收对象、任务 ID、发送状态          |
| 审批流程日志     | 业务 ID、审批实例 ID、审批状态                 |
| 回调事件日志     | EventType、事件业务 ID、处理状态               |
| 异常日志         | 错误码、错误消息、堆栈、业务上下文             |

不应该记录的内容：

| 内容                     | 说明                 |
| ------------------------ | -------------------- |
| 完整 AccessToken         | 泄露后可调用钉钉接口 |
| ClientSecret / AppSecret | 应用核心密钥         |
| 回调 AES Key             | 影响回调解密安全     |
| 群机器人完整 Webhook     | 可被直接用于发消息   |
| 用户手机号全文           | 涉及个人信息         |
| 审批正文全文             | 可能包含业务敏感数据 |

推荐日志配置如下：

文件位置：`src/main/resources/logback-spring.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <!-- 日志目录，可通过环境变量覆盖 -->
    <property name="LOG_PATH" value="${LOG_PATH:-./logs}"/>

    <!-- 控制台日志格式 -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%thread] %logger{36} - %msg%n</pattern>
            <charset>UTF-8</charset>
        </encoder>
    </appender>

    <!-- 文件日志，按天滚动 -->
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${LOG_PATH}/dingtalk-service.log</file>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%thread] %logger{36} - %msg%n</pattern>
            <charset>UTF-8</charset>
        </encoder>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>${LOG_PATH}/dingtalk-service.%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
    </appender>

    <!-- 项目日志级别 -->
    <logger name="io.github.atengk.dingtalk" level="INFO"/>

    <!-- 根日志级别 -->
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="FILE"/>
    </root>
</configuration>
```

日志记录建议遵循以下规则：

1. 关键外部调用必须记录开始、结束、耗时和失败原因。
2. 异常日志必须包含业务 ID、钉钉实例 ID 或消息任务 ID。
3. 敏感字段必须脱敏后再输出。
4. 回调事件必须落库，不能只依赖文件日志。
5. 定时任务必须记录执行结果，避免静默失败。
6. 日志级别要区分清楚，正常业务用 `info`，可恢复异常用 `warn`，系统异常用 `error`。
7. 生产环境不要开启大量 `debug` 日志，避免性能和磁盘风险。

## 功能验证

本章节用于说明钉钉集成模块开发完成后的验证方式，包括本地调试、钉钉接口联调、回调事件测试和常见问题排查。功能验证应覆盖 AccessToken、用户接口、消息通知、审批流程、回调解密、幂等和日志记录等关键链路。

### 本地调试方式

本地调试的目标是先验证 Spring Boot 服务、配置读取、Redis 缓存、接口调用和日志输出是否正常，再进入钉钉开放平台进行联调。这样可以减少钉钉后台配置问题和本地代码问题混在一起排查。

本地调试前需要准备以下环境：

| 环境                    | 说明                       |
| ----------------------- | -------------------------- |
| JDK 17+                 | Spring Boot 3 基础运行环境 |
| Maven 3.6.3+            | 项目构建工具               |
| Redis                   | 缓存 AccessToken、幂等标识 |
| 钉钉测试应用            | 用于获取应用参数和接口权限 |
| 内网穿透工具            | HTTP 回调测试时需要        |
| Apifox / Postman / curl | 接口调试工具               |

本地环境变量示例：

```bash
export DINGTALK_ENABLED=true
export DINGTALK_CORP_ID="dingxxxxxxxxxxxx"
export DINGTALK_CLIENT_ID="dingxxxxxxxxxxxx"
export DINGTALK_CLIENT_SECRET="xxxxxxxxxxxxxxxx"
export DINGTALK_AGENT_ID="123456789"
export DINGTALK_CALLBACK_TOKEN="your-callback-token"
export DINGTALK_CALLBACK_AES_KEY="your-callback-aes-key"
export DINGTALK_ROBOT_WEBHOOK="https://oapi.dingtalk.com/robot/send?access_token=xxxx"
export DINGTALK_ROBOT_SECRET="SECxxxxxxxxxxxxxxxx"
export REDIS_HOST="127.0.0.1"
export REDIS_PORT="6379"
```

启动 Redis：

```bash
docker run -d \
  --name redis-dingtalk \
  -p 6379:6379 \
  redis:7
```

该命令使用 Docker 启动一个本地 Redis 容器，并将容器的 `6379` 端口映射到本机 `6379` 端口。开发环境可直接使用，生产环境应使用正式 Redis 服务或云 Redis。

编译并启动项目：

```bash
mvn clean package
java -jar target/dingtalk-springboot3-demo-1.0.0.jar
```

启动后需要检查以下内容：

| 检查项             | 预期结果                                  |
| ------------------ | ----------------------------------------- |
| 应用是否启动成功   | 控制台出现 Spring Boot 启动完成日志       |
| 配置是否读取成功   | 缺少必填配置时应启动失败或明确报错        |
| Redis 是否连接成功 | AccessToken 缓存可以写入 Redis            |
| 日志是否输出正常   | `logs/dingtalk-service.log` 正常生成      |
| 健康检查是否可访问 | `/api/dingtalk/callback/health` 返回 `ok` |

基础接口验证：

```bash
# 健康检查
curl -X GET "http://127.0.0.1:8080/api/dingtalk/callback/health"

# 获取 AccessToken，开发测试接口需要按实际项目路径调整
curl -X GET "http://127.0.0.1:8080/api/dingtalk/token"

# 强制刷新 AccessToken
curl -X POST "http://127.0.0.1:8080/api/dingtalk/token/refresh"
```

本地调试阶段建议先验证 AccessToken 获取，因为后续用户、部门、消息、审批接口都依赖有效的 AccessToken。

### 钉钉接口联调

钉钉接口联调用于验证后端服务是否可以正常调用钉钉开放平台接口。建议按照“凭证 -> 用户 -> 部门 -> 消息 -> 审批”的顺序逐步验证，不要一开始就直接调复杂审批流程。

推荐联调顺序如下：

| 顺序 | 验证内容         | 说明                            |
| ---- | ---------------- | ------------------------------- |
| 1    | AccessToken 获取 | 验证应用参数和应用权限基础配置  |
| 2    | 用户详情查询     | 验证通讯录用户权限              |
| 3    | 部门详情查询     | 验证通讯录部门权限              |
| 4    | 工作通知发送     | 验证 AgentId、userid 和消息权限 |
| 5    | 群机器人消息     | 验证 Webhook 和加签密钥         |
| 6    | 审批实例创建     | 验证审批模板、发起人、表单字段  |
| 7    | 审批状态查询     | 验证审批实例 ID 和审批权限      |

用户接口联调示例：

```bash
curl -X GET "http://127.0.0.1:8080/api/dingtalk/users/manager123"
```

部门接口联调示例：

```bash
curl -X GET "http://127.0.0.1:8080/api/dingtalk/departments/1"
```

工作通知联调示例：

```bash
curl -X POST "http://127.0.0.1:8080/api/dingtalk/messages/work-notice/text" \
  -H "Content-Type: application/json" \
  -d '{
    "userIdList": "manager123",
    "content": "钉钉工作通知联调测试。"
  }'
```

群机器人联调示例：

```bash
curl -X POST "http://127.0.0.1:8080/api/dingtalk/messages/robot/markdown" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "联调测试",
    "content": "### 钉钉群机器人联调测试\n\n- 服务：dingtalk-springboot3-demo\n- 结果：发送成功"
  }'
```

审批实例创建联调示例：

```bash
curl -X POST "http://127.0.0.1:8080/api/dingtalk/approvals" \
  -H "Content-Type: application/json" \
  -d '{
    "processCode": "PROC-XXXXXXXX",
    "originatorUserId": "manager123",
    "deptId": "1",
    "formComponentValues": [
      {
        "name": "申请事由",
        "value": "钉钉审批联调测试"
      },
      {
        "name": "申请金额",
        "value": "100"
      }
    ]
  }'
```

审批状态查询示例：

```bash
curl -X GET "http://127.0.0.1:8080/api/dingtalk/approvals/${PROCESS_INSTANCE_ID}/status"
```

联调过程中需要重点记录以下信息：

| 信息                 | 用途                       |
| -------------------- | -------------------------- |
| AccessToken 刷新日志 | 判断凭证是否正常           |
| 钉钉接口错误码       | 判断权限、参数或接口问题   |
| RequestID            | 提交钉钉问题反馈时使用     |
| userid               | 判断接收人或发起人是否正确 |
| AgentId              | 判断工作通知所属应用       |
| process_instance_id  | 审批状态查询和回调关联     |
| task_id              | 工作通知发送结果查询       |

联调建议一次只验证一个能力。出现问题时先确认应用参数和权限，再排查请求参数，最后检查代码封装和网络环境。

### 回调事件测试

回调事件测试用于验证钉钉是否可以成功推送事件到后端服务，以及后端是否能够完成签名校验、事件解密、事件分发、幂等处理和 success 响应。

如果使用 HTTP 回调模式，本地服务需要通过内网穿透暴露到公网。示例：

```bash
# 示例：使用 cpolar 将本地 8080 端口暴露到公网
cpolar http 8080
```

启动内网穿透后，将生成的 HTTPS 地址配置到钉钉开放平台事件回调地址中：

```text
https://xxxx.cpolar.top/api/dingtalk/callback/event
```

回调测试建议按以下顺序执行：

| 顺序 | 测试内容               | 预期结果                        |
| ---- | ---------------------- | ------------------------------- |
| 1    | 访问健康检查接口       | 返回 `ok`                       |
| 2    | 在钉钉后台测试回调 URL | 后台提示验证通过                |
| 3    | 触发审批实例变更事件   | 后端收到 `bpms_instance_change` |
| 4    | 触发用户变更事件       | 后端收到用户变更事件            |
| 5    | 重复发送同一事件       | 幂等逻辑生效，不重复处理        |
| 6    | 故意配置错误 AES Key   | 后端解密失败并记录错误日志      |

健康检查：

```bash
curl -X GET "https://xxxx.cpolar.top/api/dingtalk/callback/health"
```

回调事件测试重点检查以下日志：

```text
钉钉回调事件解密成功，eventType=bpms_instance_change
接收到钉钉审批实例变更事件，processInstanceId=xxx，processCode=xxx
生成钉钉回调幂等键成功，eventType=bpms_instance_change
钉钉回调事件处理成功，eventType=bpms_instance_change
```

回调测试时需要注意：

1. 钉钉后台配置的 `Token` 必须与后端配置一致。
2. 钉钉后台配置的 `AES Key` 必须与后端配置一致。
3. 企业内部应用通常使用 `CorpId` 作为加解密 `ownerKey`。
4. 回调接口不能返回本系统统一响应对象，必须返回钉钉要求的加密 `success`。
5. 回调接口应尽快返回，复杂业务应异步处理。
6. 钉钉可能重试推送事件，因此必须做幂等。
7. 本地内网穿透地址变化后，需要同步更新钉钉后台回调 URL。

### 常见问题排查

本章节用于整理钉钉集成开发中常见问题的排查思路。排查时建议先看后端日志，再看钉钉接口返回，再核对开放平台配置，最后检查业务数据。

| 问题                 | 常见原因                            | 排查方式                                      |
| -------------------- | ----------------------------------- | --------------------------------------------- |
| AccessToken 获取失败 | Client ID 或 Client Secret 错误     | 检查环境变量、应用凭证、是否使用正确应用      |
| AccessToken 频繁刷新 | Redis 未生效或缓存 Key 不一致       | 检查 Redis 连接、缓存 TTL、缓存 Key 组成      |
| 接口返回无权限       | 应用未申请对应权限                  | 检查钉钉开放平台权限管理和管理员授权状态      |
| 查询用户失败         | userid 错误或通讯录权限不足         | 检查用户是否在企业内，应用是否有通讯录权限    |
| 工作通知发送失败     | AgentId 错误、userid 错误、权限不足 | 检查 AgentId、接收人 userid、工作通知权限     |
| 群机器人发送失败     | Webhook 错误或签名错误              | 检查 Webhook、Secret、timestamp 和 sign       |
| 审批创建失败         | processCode 错误或表单字段不匹配    | 检查审批模板编码和控件名称                    |
| 审批状态不同步       | 回调未配置或处理失败                | 检查事件订阅、回调日志、审批关联表            |
| 回调 URL 验证失败    | Token、AES Key、ownerKey 不一致     | 检查钉钉后台配置和后端配置                    |
| 回调重复处理         | 未做幂等                            | 检查回调事件日志表唯一索引和 Redis 幂等锁     |
| 接口偶发超时         | 网络抖动或钉钉服务响应慢            | 检查超时设置、重试策略和调用日志              |
| 日志查不到问题       | 缺少 RequestID 或业务上下文         | 增加接口名称、业务 ID、错误码、RequestID 日志 |

AccessToken 问题排查步骤：

1. 检查 `DINGTALK_CLIENT_ID` 是否正确。
2. 检查 `DINGTALK_CLIENT_SECRET` 是否正确。
3. 检查是否调用了正确的 AccessToken 接口。
4. 检查后端日志中是否存在钉钉错误码。
5. 检查 Redis 是否正常写入缓存。
6. 检查 AccessToken 是否被错误清理或覆盖。

消息发送问题排查步骤：

1. 检查接收人 `userid` 是否正确。
2. 检查 `AgentId` 是否属于当前应用。
3. 检查应用是否具备工作通知权限。
4. 检查消息内容是否符合钉钉格式要求。
5. 检查发送接口是否返回 `task_id`。
6. 查询消息发送进度和发送结果。
7. 查看 `dingtalk_message_record` 表中的失败原因。

审批问题排查步骤：

1. 检查 `process_code` 是否为当前企业审批模板编码。
2. 检查发起人 `userid` 是否在该企业内。
3. 检查 `dept_id` 是否正确。
4. 检查表单控件名称是否与审批模板完全一致。
5. 检查本地是否保存了 `process_instance_id`。
6. 检查审批回调是否配置并验证通过。
7. 检查审批关联表状态和回调事件日志。

回调问题排查步骤：

1. 使用浏览器或 curl 访问回调健康检查地址。
2. 确认回调 URL 是公网 HTTPS 地址。
3. 检查钉钉后台配置的 Token 和 AES Key。
4. 检查后端使用的 `CorpId` 或 `ownerKey` 是否正确。
5. 查看是否进入 Controller。
6. 查看是否解密失败。
7. 查看是否生成并返回加密 success。
8. 查看回调事件日志表是否成功落库。

生产环境上线前检查清单：

| 检查项                           | 是否必须 |
| -------------------------------- | -------- |
| 钉钉应用参数通过安全方式注入     | 是       |
| AccessToken 已使用 Redis 缓存    | 是       |
| 钉钉接口调用已统一封装           | 是       |
| 消息发送记录已落库               | 是       |
| 审批业务关联关系已保存           | 是       |
| 回调事件日志已落库               | 是       |
| 回调处理已做幂等                 | 是       |
| 外部接口调用有超时配置           | 是       |
| 临时测试接口已删除或加权限       | 是       |
| 日志已做敏感信息脱敏             | 是       |
| 生产回调地址使用 HTTPS           | 是       |
| 异常日志包含业务 ID 和 RequestID | 建议     |
| 关键失败场景有补偿任务           | 建议     |
| 钉钉应用权限完成管理员授权       | 是       |

完成以上验证后，钉钉集成模块即可进入测试环境或生产环境灰度发布。建议优先灰度非核心业务通知，再逐步开放审批、组织同步和回调驱动类能力。