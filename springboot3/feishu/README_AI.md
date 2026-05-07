# Spring Boot 飞书开发

## 项目概述

本章节用于说明 Spring Boot 3 集成飞书开放平台的建设背景、功能边界和适用场景。通过该集成，业务系统可以统一调用飞书的消息、通讯录、事件订阅、审批等开放能力，减少业务模块直接对接飞书接口造成的重复开发和维护成本。

### 开发背景

在企业内部系统开发中，业务系统通常需要与办公协同平台进行集成。例如，审批系统需要将审批结果同步到飞书，任务系统需要向负责人推送待办提醒，运维系统需要将异常告警发送到飞书群，组织权限系统需要同步飞书用户和部门信息。

飞书开放平台提供了应用凭证、访问令牌、消息发送、通讯录、审批、事件回调等能力，可以满足企业内部系统与飞书之间的数据交互需求。Spring Boot 3 具备成熟的 Web 开发、配置管理、依赖管理、日志记录和部署能力，适合作为飞书集成服务的后端基础框架。

直接在业务代码中调用飞书接口会带来以下问题：

| 问题           | 说明                                                       |
| -------------- | ---------------------------------------------------------- |
| 鉴权逻辑重复   | 不同接口需要不同类型的访问令牌，重复处理容易出错           |
| Token 管理复杂 | 需要处理 Token 获取、缓存、过期刷新和失效重试              |
| 接口调用分散   | 消息、用户、部门、审批、事件回调等能力分布在不同业务模块中 |
| 异常处理不统一 | 飞书接口错误码、网络异常、权限异常需要统一封装             |
| 回调安全要求高 | 事件回调需要处理地址校验、签名校验、消息解密和幂等处理     |
| 敏感配置易泄露 | `app_id`、`app_secret`、`encrypt_key` 等配置需要安全管理   |

因此，本项目将飞书开放能力封装为统一的 Spring Boot 集成模块，对外提供标准 Service 或 REST API，业务系统只需要调用内部接口，不需要关心飞书接口细节。

### 功能目标

本项目的核心目标是构建一套可复用、可扩展、可维护的飞书集成方案，方便后续业务系统快速接入飞书能力。

主要功能目标如下：

| 功能目标         | 说明                                                         |
| ---------------- | ------------------------------------------------------------ |
| 应用凭证统一配置 | 统一维护飞书应用的 `app_id`、`app_secret`、回调 Token、加密密钥等配置 |
| Token 自动管理   | 封装 `app_access_token`、`tenant_access_token`、`user_access_token` 的获取、缓存和刷新逻辑 |
| 消息发送能力     | 支持发送文本消息、富文本消息、卡片消息和群机器人消息         |
| 用户信息查询     | 支持根据用户 ID、手机号、邮箱等信息查询飞书用户              |
| 组织架构同步     | 支持查询部门、用户列表、用户与部门关系等组织数据             |
| 事件订阅处理     | 支持飞书事件回调地址校验、消息解密、事件分发和业务处理       |
| 审批业务集成     | 支持创建审批实例、查询审批状态、处理审批回调                 |
| 异常统一封装     | 统一处理飞书接口调用失败、Token 失效、权限不足、验签失败等异常 |
| 日志与调试支持   | 记录关键请求、响应、错误码和业务处理结果，便于排查问题       |
| 多环境配置隔离   | 支持开发、测试、生产环境使用不同飞书应用配置                 |

集成完成后，业务模块可以通过内部接口完成飞书能力调用。例如，订单模块只需要调用消息发送服务推送订单通知，审批模块只需要调用审批服务创建飞书审批实例，不需要重复处理飞书鉴权、HTTP 请求和异常解析。

### 应用场景

Spring Boot 集成飞书适用于企业内部系统与飞书协同平台打通的场景，常见应用场景如下：

| 应用场景         | 说明                                                         |
| ---------------- | ------------------------------------------------------------ |
| 系统通知推送     | 将订单变更、任务提醒、审批提醒、异常告警等消息推送到飞书     |
| 审批流程集成     | 业务系统发起飞书审批，并将审批结果同步回本地业务数据         |
| 组织架构同步     | 定时同步飞书部门、用户、上下级关系，用于权限控制和人员管理   |
| 用户账号绑定     | 将业务系统账号与飞书用户 ID、手机号、邮箱进行绑定            |
| 群机器人通知     | 将业务提醒、监控告警、定时任务结果发送到指定飞书群           |
| 事件回调处理     | 接收飞书消息事件、卡片交互事件、审批事件，并触发后端业务逻辑 |
| 内部管理系统集成 | OA、ERP、CRM、工单系统、人事系统、项目管理系统等接入飞书     |
| 运维监控告警     | 将服务异常、接口失败、任务失败、资源告警等信息推送到飞书     |

在实际项目中，建议将飞书相关代码单独放在一个模块或包中，例如：

```text
src/main/java/io/github/atengk/feishu
├── config        # 飞书配置类
├── client        # 飞书接口客户端
├── service       # 飞书业务封装
├── controller    # 飞书回调接口或内部调用接口
├── model         # 请求、响应、事件对象
└── exception     # 飞书异常处理
```

这种组织方式可以降低飞书集成代码与业务代码之间的耦合度，便于后续扩展消息、审批、通讯录、事件订阅等能力。

## 环境准备

本章节用于说明 Spring Boot 3 集成飞书开发前需要准备的基础环境，包括后端开发环境、飞书开放平台应用、应用凭证、权限配置和本地运行配置。环境准备完成后，后续才能继续进行 Maven 依赖配置、客户端初始化、Token 管理和接口开发。

### Spring Boot 版本要求

本项目建议基于 Spring Boot 3.x 稳定版本开发。Spring Boot 3 最低要求使用 JDK 17，因此项目开发、编译、运行环境都需要使用 JDK 17 或更高版本。

推荐环境如下：

| 环境               | 推荐版本     | 说明                                                     |
| ------------------ | ------------ | -------------------------------------------------------- |
| JDK                | 17 或 21     | Spring Boot 3 最低要求 JDK 17，生产环境推荐使用 LTS 版本 |
| Spring Boot        | 3.x 稳定版本 | 建议使用 3.2.x、3.3.x 或更高稳定版本                     |
| Maven              | 3.6.3+       | 用于依赖管理、项目构建和打包                             |
| Redis              | 6.x / 7.x    | 可选，用于缓存飞书 Token、回调幂等数据                   |
| MySQL / PostgreSQL | 按项目要求   | 可选，用于保存用户绑定、审批记录、事件日志等数据         |
| Lombok             | 最新稳定版   | 简化 DTO、配置类和日志代码                               |
| Hutool             | 5.8.x        | 用于字符串、JSON、HTTP、加解密、日期等工具处理           |

需要注意，Spring Boot 3 已经从 `javax.*` 迁移到 `jakarta.*` 命名空间。如果项目中存在旧版依赖，例如基于 Spring Boot 2 的 Servlet、Validation、JPA、过滤器或第三方 SDK，需要确认其是否兼容 Spring Boot 3。

基础 Maven 依赖可以按以下方式准备。

```xml
<properties>
    <!-- Spring Boot 3 最低要求 JDK 17 -->
    <java.version>17</java.version>
    <!-- 统一项目编码 -->
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
</properties>

<dependencies>
    <!-- Web 能力：用于提供飞书回调接口和业务调用接口 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- 参数校验：用于校验消息发送、用户查询、审批请求等接口参数 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- Redis 缓存：用于缓存飞书访问令牌，避免频繁请求飞书鉴权接口 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>

    <!-- Hutool 工具类：用于 JSON、HTTP、字符串、日期、加解密等通用处理 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.8.35</version>
    </dependency>

    <!-- Lombok：用于简化实体类、配置类、DTO 和日志声明 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

如果项目使用飞书官方 Java SDK，可以在后续“基础配置 / Maven 依赖配置”章节中单独补充 SDK 依赖。如果希望减少对 SDK 的绑定，也可以使用 Hutool 的 `HttpUtil`、Spring `RestClient` 或 `WebClient` 自行封装飞书 OpenAPI 调用。

### 飞书开放平台准备

在开始编码前，需要先在飞书开放平台创建企业自建应用。该应用是后端服务调用飞书接口的身份载体，后续获取 Token、发送消息、查询用户、订阅事件、创建审批实例等操作都依赖该应用。

飞书开放平台准备步骤如下：

| 步骤 | 操作             | 说明                                                     |
| ---- | ---------------- | -------------------------------------------------------- |
| 1    | 登录飞书开放平台 | 使用企业管理员或具备开发权限的账号登录                   |
| 2    | 创建企业自建应用 | 适用于企业内部业务系统集成                               |
| 3    | 获取应用凭证     | 在应用后台获取 `app_id` 和 `app_secret`                  |
| 4    | 添加应用能力     | 按业务需要开启机器人、通讯录、审批、事件订阅等能力       |
| 5    | 申请接口权限     | 根据接口调用范围申请消息、通讯录、审批等权限             |
| 6    | 配置事件订阅     | 设置回调地址、Verification Token、Encrypt Key 和订阅事件 |
| 7    | 发布应用版本     | 权限或能力变更后，需要发布新版本                         |
| 8    | 安装应用到企业   | 应用安装后，后端服务才能在授权范围内调用接口             |

权限申请时建议遵循最小权限原则。只需要发送消息时，不要申请通讯录全量读取权限；只需要处理审批回调时，不要申请文档、日历等无关权限。生产环境中可以按照业务边界拆分多个飞书应用，例如通知应用、审批应用、机器人应用，降低单个应用权限过大的风险。

常见能力和权限关系如下：

| 集成能力     | 需要准备的能力         | 说明                                     |
| ------------ | ---------------------- | ---------------------------------------- |
| 发送用户消息 | 机器人能力、消息权限   | 向指定用户发送业务通知                   |
| 发送群消息   | 机器人能力、群消息权限 | 向指定群推送通知或告警                   |
| 查询用户信息 | 通讯录权限             | 根据手机号、邮箱、open_id 等查询用户     |
| 查询部门信息 | 通讯录权限             | 同步部门、用户和组织关系                 |
| 接收事件回调 | 事件订阅能力           | 接收消息、卡片交互、审批状态等事件       |
| 审批集成     | 审批权限               | 创建审批实例、查询审批状态、处理审批回调 |

本地开发事件回调功能时，飞书开放平台需要访问后端回调地址。如果本地服务没有公网地址，可以使用内网穿透工具将本地端口映射为 HTTPS 地址。生产环境必须使用稳定的 HTTPS 域名，并确保该域名可以被飞书开放平台正常访问。

### 应用凭证配置

应用凭证配置用于将飞书开放平台中的应用信息注入到 Spring Boot 项目中。常见配置包括 `app_id`、`app_secret`、回调校验 Token、事件加密 Key、飞书 OpenAPI 地址、Token 缓存时间和回调路径。

敏感信息不要写死在 Java 代码中，也不要提交真实值到 Git 仓库。开发环境可以使用本地环境变量或本地配置文件，测试和生产环境建议使用配置中心、Kubernetes Secret、环境变量或密钥管理服务。

推荐配置项如下：

| 配置项                       | 说明                                       |
| ---------------------------- | ------------------------------------------ |
| `feishu.base-url`            | 飞书 OpenAPI 基础地址                      |
| `feishu.app-id`              | 飞书应用 ID                                |
| `feishu.app-secret`          | 飞书应用密钥                               |
| `feishu.verification-token`  | 事件订阅回调校验 Token                     |
| `feishu.encrypt-key`         | 事件订阅消息加密 Key                       |
| `feishu.token-cache-seconds` | Token 缓存时间，建议小于飞书返回的过期时间 |
| `feishu.callback-path`       | 飞书事件回调接口路径                       |

`application.yml` 可以按以下方式配置。

```yaml
server:
  port: 8080

spring:
  application:
    # 当前服务名称，用于日志追踪和配置区分
    name: springboot-feishu-demo
  data:
    redis:
      # Redis 用于缓存飞书访问令牌，生产环境建议配置密码和连接池
      host: ${REDIS_HOST:127.0.0.1}
      port: ${REDIS_PORT:6379}
      database: ${REDIS_DATABASE:0}

feishu:
  # 飞书 OpenAPI 基础地址
  base-url: https://open.feishu.cn

  # 飞书应用 ID，建议通过环境变量注入
  app-id: ${FEISHU_APP_ID:cli_xxxxxxxxxxxxx}

  # 飞书应用密钥，禁止提交真实值到 Git 仓库
  app-secret: ${FEISHU_APP_SECRET:replace-with-your-secret}

  # 事件订阅 Verification Token，用于回调校验
  verification-token: ${FEISHU_VERIFICATION_TOKEN:replace-with-your-token}

  # 事件订阅 Encrypt Key，用于解密飞书推送的加密事件
  encrypt-key: ${FEISHU_ENCRYPT_KEY:replace-with-your-encrypt-key}

  # Token 缓存时间，单位秒；建议小于飞书接口返回的过期时间
  token-cache-seconds: 6600

  # 飞书事件回调接口路径，需要与开放平台后台配置保持一致
  callback-path: /api/feishu/event/callback
```

本地启动时，可以使用环境变量覆盖敏感配置。

Linux 或 macOS 环境：

```bash
export FEISHU_APP_ID="cli_xxxxxxxxxxxxx"
export FEISHU_APP_SECRET="your-app-secret"
export FEISHU_VERIFICATION_TOKEN="your-verification-token"
export FEISHU_ENCRYPT_KEY="your-encrypt-key"

mvn spring-boot:run
```

Windows PowerShell 环境：

```powershell
$env:FEISHU_APP_ID="cli_xxxxxxxxxxxxx"
$env:FEISHU_APP_SECRET="your-app-secret"
$env:FEISHU_VERIFICATION_TOKEN="your-verification-token"
$env:FEISHU_ENCRYPT_KEY="your-encrypt-key"

mvn spring-boot:run
```

应用凭证配置完成后，需要检查以下内容：

| 检查项             | 说明                                             |
| ------------------ | ------------------------------------------------ |
| 应用凭证是否匹配   | `app_id` 和 `app_secret` 必须来自同一个飞书应用  |
| 权限是否申请完成   | 调用消息、通讯录、审批接口前，需要先申请对应权限 |
| 应用是否已发布     | 权限变更后，需要发布应用版本                     |
| 应用是否已安装     | 应用需要安装到企业或指定用户范围                 |
| 回调地址是否可访问 | 事件订阅回调地址需要能被飞书开放平台访问         |
| 配置是否环境隔离   | 开发、测试、生产环境建议使用不同配置             |
| 敏感信息是否安全   | 不要将真实密钥提交到代码仓库、日志或接口响应中   |
| Token 是否缓存     | 不建议每次业务请求都实时调用飞书 Token 接口      |

完成以上准备后，可以继续进入“基础配置”章节，开始编写 Maven 依赖、配置属性映射类、飞书客户端初始化和 Token 管理逻辑。

## 接入方式设计

本章节用于说明 Spring Boot 后端服务接入飞书开放平台时的几种常见方式。不同接入方式对应不同的鉴权模型、Token 类型、权限范围和业务场景，设计时需要先明确当前项目是企业内部系统集成、第三方应用接入，还是需要用户授权的 OAuth 登录场景。本文档继续基于你给出的开发大纲展开。

### 自建应用接入

自建应用接入是企业内部系统集成飞书时最常用的方式。开发者在飞书开放平台创建企业自建应用，获取 `app_id` 和 `app_secret`，后端服务通过应用凭证获取访问令牌，然后调用飞书开放接口。

自建应用适用于以下场景：

| 场景         | 说明                                       |
| ------------ | ------------------------------------------ |
| 内部系统通知 | 业务系统向员工、群聊推送消息               |
| 审批集成     | 后端系统发起飞书审批或接收审批回调         |
| 通讯录同步   | 同步飞书用户、部门、组织架构               |
| 机器人交互   | 飞书机器人接收用户消息并触发后端业务       |
| 运维告警     | 将服务异常、任务失败、监控告警推送到飞书群 |

自建应用的核心特点是：应用由企业自己创建和管理，只服务于当前企业内部。后端服务通常使用 `tenant_access_token` 调用大部分企业级开放接口，例如消息发送、通讯录查询、审批实例创建等。飞书接口文档中，自建应用可通过 `/open-apis/auth/v3/tenant_access_token/internal` 使用 `app_id` 和 `app_secret` 获取 `tenant_access_token`。([CSDN博客](https://blog.csdn.net/ymxk2876721452/article/details/143469577?utm_source=chatgpt.com))

接入流程如下：

| 步骤 | 说明                                                 |
| ---- | ---------------------------------------------------- |
| 1    | 在飞书开放平台创建企业自建应用                       |
| 2    | 获取应用的 `app_id` 和 `app_secret`                  |
| 3    | 根据业务需要开启机器人、通讯录、审批、事件订阅等能力 |
| 4    | 申请对应接口权限，并发布应用版本                     |
| 5    | 在 Spring Boot 中配置应用凭证                        |
| 6    | 后端通过应用凭证获取 `tenant_access_token`           |
| 7    | 使用 `tenant_access_token` 调用飞书业务接口          |
| 8    | 处理接口响应、异常、Token 缓存和回调事件             |

自建应用接入建议作为企业内部系统的默认接入方式。它的权限边界清晰，部署和维护成本较低，也便于后端统一封装消息、通讯录、审批和事件能力。

### 企业内部应用接入

企业内部应用接入可以理解为自建应用在企业内部业务系统中的落地方式。与面向外部客户的商店应用不同，企业内部应用通常只安装在当前企业中，服务对象是本企业员工、部门、群组和内部业务流程。

企业内部应用更关注以下几个方面：

| 设计点   | 说明                                           |
| -------- | ---------------------------------------------- |
| 权限范围 | 只申请当前业务需要的最小权限                   |
| 安装范围 | 可以限制为全企业、指定部门或指定用户           |
| 数据边界 | 只能处理当前企业授权范围内的数据               |
| 回调地址 | 需要配置企业可访问的 HTTPS 回调地址            |
| 配置隔离 | 开发、测试、生产环境建议使用不同应用或不同凭证 |
| 安全审计 | 需要记录关键接口调用、审批回调、异常响应等日志 |

企业内部应用接入时，建议后端服务按照以下分层设计：

```text
src/main/java/io/github/atengk/feishu
├── config
│   ├── FeishuProperties.java
│   └── FeishuClientConfig.java
├── client
│   └── FeishuApiClient.java
├── service
│   ├── FeishuTokenService.java
│   ├── FeishuMessageService.java
│   └── FeishuUserService.java
├── model
│   ├── request
│   └── response
└── exception
    └── FeishuApiException.java
```

这种结构将配置、HTTP 调用、Token 管理、业务封装和异常处理分离，后续扩展消息发送、用户查询、事件回调、审批集成时不会破坏已有代码。

企业内部应用常用 Token 选择如下：

| Token 类型            | 使用场景                                                     |
| --------------------- | ------------------------------------------------------------ |
| `app_access_token`    | 应用级接口、部分 OAuth 相关接口、商店应用换取租户 Token      |
| `tenant_access_token` | 企业内部应用调用通讯录、消息、审批等企业级接口               |
| `user_access_token`   | 需要代表某个用户访问数据的接口，例如用户授权后的个人数据访问 |

对于大多数企业内部业务系统，优先使用 `tenant_access_token`。只有在接口明确要求用户身份，或者需要实现飞书免登录、扫码登录、用户授权访问时，才需要引入 OAuth 流程和 `user_access_token`。

### OAuth 授权接入

OAuth 授权接入适用于需要用户主动授权的场景，例如飞书免登录、扫码登录、以用户身份访问飞书资源、获取当前登录用户信息等。该模式下，后端不能只依赖应用凭证，还需要接收飞书返回的授权码 `code`，再使用授权码换取 `user_access_token`。

OAuth 授权适用于以下场景：

| 场景         | 说明                                                   |
| ------------ | ------------------------------------------------------ |
| 飞书免登录   | 用户从飞书工作台进入业务系统，后端根据授权信息识别用户 |
| 扫码登录     | 用户通过飞书扫码登录企业系统                           |
| 用户身份绑定 | 将业务系统账号与飞书用户身份绑定                       |
| 用户数据访问 | 代表用户访问其授权范围内的数据                         |
| 多租户授权   | 第三方应用被不同企业安装后，需要按租户和用户维度授权   |

OAuth 授权流程如下：

| 步骤 | 说明                                           |
| ---- | ---------------------------------------------- |
| 1    | 前端跳转到飞书授权地址                         |
| 2    | 用户确认授权                                   |
| 3    | 飞书回调业务系统配置的 `redirect_uri`          |
| 4    | 后端从回调参数中获取授权码 `code`              |
| 5    | 后端调用飞书接口换取 `user_access_token`       |
| 6    | 后端根据返回的用户信息完成登录、绑定或业务授权 |
| 7    | 缓存 `user_access_token` 和 `refresh_token`    |
| 8    | Token 过期前使用刷新接口续期                   |

飞书获取 `user_access_token` 的接口为 `https://open.feishu.cn/open-apis/authen/v1/access_token`，请求体中使用 `grant_type=authorization_code` 和授权码 `code` 换取用户访问令牌；返回数据中包含 `access_token`、`refresh_token`、`expires_in`、用户 ID、邮箱等字段，部分敏感字段需要额外权限。([Apifox](https://s.apifox.cn/apidoc/docs-site/532425/api-58160393?utm_source=chatgpt.com))

OAuth 接入需要额外注意以下事项：

| 注意事项                  | 说明                                                   |
| ------------------------- | ------------------------------------------------------ |
| `redirect_uri` 必须一致   | 授权地址中的回调地址需要与飞书开放平台后台配置保持一致 |
| `state` 参数必须校验      | 防止 CSRF 攻击，建议后端生成并缓存一次性 state         |
| 授权码只能使用一次        | `code` 通常只能短时间内使用一次，重复使用会失败        |
| 用户 Token 需要按用户缓存 | `user_access_token` 不能作为全局 Token 使用            |
| 刷新 Token 需要安全保存   | `refresh_token` 可以换取新的用户 Token，应加密存储     |
| 权限按需申请              | 不需要用户手机号、邮箱时，不建议申请对应敏感权限       |

## 基础配置

本章节用于给出 Spring Boot 3 集成飞书的基础配置，包括 Maven 依赖、`application.yml` 配置项、配置属性映射类和飞书客户端初始化。后续 Token 获取、消息发送、用户查询和事件回调都会基于这些配置继续开发。

### Maven 依赖配置

本项目使用 Spring Boot 3、Hutool、Lombok、Redis 和 Validation 作为基础依赖。Hutool 用于封装 HTTP 请求、JSON 解析、字符串判断等通用逻辑；Redis 用于缓存飞书 Token，避免频繁请求飞书鉴权接口。

如果项目使用 Spring Boot Parent 管理版本，`spring-boot-starter-*` 依赖不需要单独指定版本。

```xml
<properties>
    <!-- Spring Boot 3 最低要求 JDK 17 -->
    <java.version>17</java.version>
    <!-- 统一项目编码 -->
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
</properties>

<dependencies>
    <!-- Web 能力：提供飞书回调接口、OAuth 回调接口和内部业务接口 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- 参数校验：用于校验消息发送、OAuth 回调、Token 请求等参数 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- Redis：用于缓存 app_access_token、tenant_access_token、user_access_token 等访问令牌 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>

    <!-- Hutool：用于 HTTP 请求、JSON 处理、字符串处理、日期处理和加解密等工具能力 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.8.35</version>
    </dependency>

    <!-- Lombok：用于简化配置类、DTO、日志对象和构造器代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

如果团队希望使用飞书官方 Java SDK，也可以额外引入 `com.larksuite.oapi:larksuite-oapi`。飞书开放接口 SDK 的 GitHub 仓库说明其用于调用飞书开放 API、处理事件订阅和卡片回调；如果项目希望减少 SDK 绑定，则可以像本文档一样基于 Hutool 自行封装 HTTP 客户端。([GitHub](https://github.com/larksuite/oapi-sdk-java?utm_source=chatgpt.com))

### application 配置项

`application.yml` 用于统一维护飞书开放平台的基础配置。真实的 `app_secret`、`verification_token` 和 `encrypt_key` 不建议直接写死在配置文件中，生产环境应通过环境变量、配置中心、Kubernetes Secret 或密钥管理服务注入。

```yaml
server:
  port: 8080

spring:
  application:
    # 当前服务名称，用于日志追踪和服务识别
    name: springboot-feishu-demo
  data:
    redis:
      # Redis 用于缓存飞书访问令牌，生产环境建议配置密码和连接池
      host: ${REDIS_HOST:127.0.0.1}
      port: ${REDIS_PORT:6379}
      database: ${REDIS_DATABASE:0}

feishu:
  # 飞书 OpenAPI 基础地址
  base-url: https://open.feishu.cn

  # 飞书应用 ID，来自开放平台应用凭证
  app-id: ${FEISHU_APP_ID:cli_xxxxxxxxxxxxx}

  # 飞书应用密钥，禁止提交真实值到 Git 仓库
  app-secret: ${FEISHU_APP_SECRET:replace-with-your-secret}

  # 事件订阅 Verification Token，用于回调校验
  verification-token: ${FEISHU_VERIFICATION_TOKEN:replace-with-your-token}

  # 事件订阅 Encrypt Key，用于解密加密事件
  encrypt-key: ${FEISHU_ENCRYPT_KEY:replace-with-your-encrypt-key}

  # 默认租户标识，商店应用或多租户场景可使用；企业内部自建应用可不配置
  tenant-key: ${FEISHU_TENANT_KEY:}

  # Token 缓存提前过期秒数，避免临界过期导致接口调用失败
  token-expire-offset-seconds: 300

  # HTTP 请求连接超时时间，单位毫秒
  connect-timeout: 5000

  # HTTP 请求读取超时时间，单位毫秒
  read-timeout: 10000

  # 飞书事件回调路径，需要与开放平台后台配置保持一致
  callback-path: /api/feishu/event/callback

  # OAuth 授权回调地址，需要与开放平台配置保持一致
  oauth-redirect-uri: ${FEISHU_OAUTH_REDIRECT_URI:https://example.com/api/feishu/oauth/callback}
```

配置项建议说明如下：

| 配置项                               | 说明                                 |
| ------------------------------------ | ------------------------------------ |
| `feishu.base-url`                    | 飞书 OpenAPI 基础地址                |
| `feishu.app-id`                      | 飞书应用 ID                          |
| `feishu.app-secret`                  | 飞书应用密钥                         |
| `feishu.verification-token`          | 事件订阅回调校验 Token               |
| `feishu.encrypt-key`                 | 事件订阅消息加密 Key                 |
| `feishu.tenant-key`                  | 租户标识，多租户或商店应用场景使用   |
| `feishu.token-expire-offset-seconds` | Token 缓存提前失效时间，避免临界过期 |
| `feishu.connect-timeout`             | HTTP 连接超时时间                    |
| `feishu.read-timeout`                | HTTP 读取超时时间                    |
| `feishu.callback-path`               | 飞书事件回调接口路径                 |
| `feishu.oauth-redirect-uri`          | OAuth 授权回调地址                   |

下面配置类用于读取 `application.yml` 中的 `feishu` 配置。

文件位置：`src/main/java/io/github/atengk/feishu/config/FeishuProperties.java`

```java
package io.github.atengk.feishu.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 飞书开放平台配置属性
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Data
@ConfigurationProperties(prefix = "feishu")
public class FeishuProperties {

    /**
     * 飞书 OpenAPI 基础地址
     */
    private String baseUrl = "https://open.feishu.cn";

    /**
     * 飞书应用 ID
     */
    private String appId;

    /**
     * 飞书应用密钥
     */
    private String appSecret;

    /**
     * 事件订阅 Verification Token
     */
    private String verificationToken;

    /**
     * 事件订阅 Encrypt Key
     */
    private String encryptKey;

    /**
     * 租户标识，多租户或商店应用场景使用
     */
    private String tenantKey;

    /**
     * Token 缓存提前过期秒数
     */
    private Long tokenExpireOffsetSeconds = 300L;

    /**
     * HTTP 连接超时时间，单位毫秒
     */
    private Integer connectTimeout = 5000;

    /**
     * HTTP 读取超时时间，单位毫秒
     */
    private Integer readTimeout = 10000;

    /**
     * 飞书事件回调路径
     */
    private String callbackPath;

    /**
     * OAuth 授权回调地址
     */
    private String oauthRedirectUri;
}
```

### 飞书客户端初始化

飞书客户端用于统一处理 OpenAPI 请求，包括 URL 拼接、请求头设置、JSON 请求体发送、响应日志记录和异常解析。这里不直接在业务 Service 中拼接 HTTP 请求，而是先封装一个通用 `FeishuApiClient`，后续 Token、消息、用户、审批等服务都通过该客户端调用飞书接口。

下面配置类用于启用飞书配置属性，并注册飞书 API 客户端。

文件位置：`src/main/java/io/github/atengk/feishu/config/FeishuClientConfig.java`

```java
package io.github.atengk.feishu.config;

import io.github.atengk.feishu.client.FeishuApiClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 飞书客户端配置
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Configuration
@EnableConfigurationProperties(FeishuProperties.class)
public class FeishuClientConfig {

    /**
     * 初始化飞书 API 客户端
     *
     * @param feishuProperties 飞书配置属性
     * @return 飞书 API 客户端
     */
    @Bean
    public FeishuApiClient feishuApiClient(FeishuProperties feishuProperties) {
        return new FeishuApiClient(feishuProperties);
    }
}
```

下面客户端封装飞书接口的基础 POST 请求能力。

文件位置：`src/main/java/io/github/atengk/feishu/client/FeishuApiClient.java`

```java
package io.github.atengk.feishu.client;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONUtil;
import io.github.atengk.feishu.config.FeishuProperties;
import io.github.atengk.feishu.exception.FeishuApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * 飞书开放平台 API 客户端
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@RequiredArgsConstructor
public class FeishuApiClient {

    private final FeishuProperties feishuProperties;

    /**
     * 发送无鉴权 POST 请求
     *
     * @param path 接口路径
     * @param body 请求体
     * @return 响应 JSON 字符串
     */
    public String post(String path, Object body) {
        return post(path, body, null);
    }

    /**
     * 发送带 Bearer Token 的 POST 请求
     *
     * @param path 接口路径
     * @param body 请求体
     * @param bearerToken Bearer Token
     * @return 响应 JSON 字符串
     */
    public String post(String path, Object body, String bearerToken) {
        String url = buildUrl(path);
        String requestBody = JSONUtil.toJsonStr(body);

        try {
            HttpRequest request = HttpRequest.post(url)
                    .timeout(feishuProperties.getReadTimeout())
                    .connectionTimeout(feishuProperties.getConnectTimeout())
                    .header("Content-Type", "application/json; charset=utf-8")
                    .body(requestBody);

            if (StrUtil.isNotBlank(bearerToken)) {
                request.header("Authorization", "Bearer " + bearerToken);
            }

            String responseBody = request.execute().body();
            log.info("飞书接口请求完成，path={}，response={}", path, responseBody);
            return responseBody;
        } catch (Exception e) {
            log.error("飞书接口请求异常，path={}，body={}", path, requestBody, e);
            throw new FeishuApiException("飞书接口请求异常：" + e.getMessage(), e);
        }
    }

    /**
     * 构建完整请求地址
     *
     * @param path 接口路径
     * @return 完整请求地址
     */
    private String buildUrl(String path) {
        String baseUrl = StrUtil.removeSuffix(feishuProperties.getBaseUrl(), "/");
        String apiPath = StrUtil.addPrefixIfNot(path, "/");
        return baseUrl + apiPath;
    }

    /**
     * 构建基础请求体
     *
     * @return 包含应用凭证的请求体
     */
    public Map<String, Object> buildAppCredentialBody() {
        return Map.of(
                "app_id", feishuProperties.getAppId(),
                "app_secret", feishuProperties.getAppSecret()
        );
    }
}
```

下面异常类用于统一表示飞书接口调用异常。

文件位置：`src/main/java/io/github/atengk/feishu/exception/FeishuApiException.java`

```java
package io.github.atengk.feishu.exception;

/**
 * 飞书接口异常
 *
 * @author Ateng
 * @since 2026-05-07
 */
public class FeishuApiException extends RuntimeException {

    /**
     * 创建飞书接口异常
     *
     * @param message 异常信息
     */
    public FeishuApiException(String message) {
        super(message);
    }

    /**
     * 创建飞书接口异常
     *
     * @param message 异常信息
     * @param cause 原始异常
     */
    public FeishuApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

## 鉴权与令牌管理

本章节用于封装飞书访问令牌的获取、缓存和刷新逻辑。飞书接口调用前通常需要先获取访问令牌，不同业务场景需要的 Token 类型不同：应用级接口使用 `app_access_token`，企业内部应用常用 `tenant_access_token`，用户授权场景使用 `user_access_token`。

### App Access Token 获取

`app_access_token` 是应用级访问凭证，表示当前应用身份。自建应用可以通过 `app_id` 和 `app_secret` 获取 `app_access_token`。飞书文档示例中的接口路径为 `/open-apis/auth/v3/app_access_token/internal`，返回字段包含 `app_access_token`、`expire`、`code`、`msg` 等信息，并说明该 Token 最大有效期为 2 小时。([Apifox](https://s.apifox.cn/apidoc/project-532425/api-58156982?utm_source=chatgpt.com))

`app_access_token` 常用于以下场景：

| 场景                 | 说明                                           |
| -------------------- | ---------------------------------------------- |
| 应用级接口调用       | 部分接口要求应用级 Token                       |
| 商店应用换租户 Token | 商店应用通常先获取应用 Token，再换取租户 Token |
| OAuth 相关流程       | 部分用户授权流程可能需要应用级能力             |
| 多租户应用管理       | 用于区分应用身份和租户身份                     |

### Tenant Access Token 获取

`tenant_access_token` 是企业租户级访问凭证，表示当前应用在某个企业租户下的访问身份。企业内部自建应用调用消息、通讯录、审批等接口时，通常优先使用 `tenant_access_token`。

自建应用通常通过 `app_id` 和 `app_secret` 获取 `tenant_access_token`；商店应用则可能需要先获取 `app_access_token`，再结合 `tenant_key` 换取租户访问凭证。商店应用获取 `tenant_access_token` 的接口示例为 `/open-apis/auth/v3/tenant_access_token`，请求体包含 `app_access_token` 和 `tenant_key`，返回字段包含 `tenant_access_token` 和 `expire`。([Apifox](https://s.apifox.cn/apidoc/docs-site/532425/api-58158636?utm_source=chatgpt.com))

`tenant_access_token` 常用于以下场景：

| 场景           | 说明                         |
| -------------- | ---------------------------- |
| 发送消息       | 向用户、群聊发送通知         |
| 查询通讯录     | 获取用户、部门、组织关系     |
| 审批集成       | 创建审批实例、查询审批状态   |
| 事件处理       | 在事件回调中继续调用飞书接口 |
| 企业级数据访问 | 访问当前企业授权范围内的数据 |

### User Access Token 获取

`user_access_token` 是用户级访问凭证，表示某个用户授权后的访问身份。该 Token 不能作为全局系统 Token 使用，必须按用户维度存储和管理。

典型获取流程如下：

| 步骤 | 说明                                                     |
| ---- | -------------------------------------------------------- |
| 1    | 前端跳转飞书 OAuth 授权地址                              |
| 2    | 用户完成授权                                             |
| 3    | 飞书回调业务系统并携带 `code`                            |
| 4    | 后端使用 `code` 调用 `access_token` 接口                 |
| 5    | 后端获取 `user_access_token`、`refresh_token` 和用户信息 |
| 6    | 后端完成登录、绑定或用户授权业务                         |
| 7    | 将用户 Token 按用户维度缓存或加密保存                    |

飞书获取 `user_access_token` 的接口为 `/open-apis/authen/v1/access_token`，请求体使用 `grant_type=authorization_code` 和 `code`；刷新用户 Token 的接口为 `/open-apis/authen/v1/refresh_access_token`，请求体使用 `grant_type=refresh_token` 和 `refresh_token`。([Apifox](https://s.apifox.cn/apidoc/docs-site/532425/api-58160393?utm_source=chatgpt.com))

### Token 缓存与刷新

Token 缓存用于减少飞书鉴权接口调用次数，并避免高并发场景下频繁刷新 Token。建议将 `app_access_token` 和 `tenant_access_token` 缓存在 Redis 中，并设置略小于飞书返回过期时间的 TTL。例如飞书返回 `expire=7200` 秒时，可以缓存 `7200 - 300 = 6900` 秒。

下面给出完整的 Token 管理实现，包括应用 Token、租户 Token、用户 Token 获取，以及 Redis 缓存处理。

文件位置：`src/main/java/io/github/atengk/feishu/service/FeishuTokenService.java`

```java
package io.github.atengk.feishu.service;

/**
 * 飞书令牌服务
 *
 * @author Ateng
 * @since 2026-05-07
 */
public interface FeishuTokenService {

    /**
     * 获取应用访问令牌
     *
     * @return app_access_token
     */
    String getAppAccessToken();

    /**
     * 获取租户访问令牌
     *
     * @return tenant_access_token
     */
    String getTenantAccessToken();

    /**
     * 根据授权码获取用户访问令牌
     *
     * @param code OAuth 授权码
     * @return user_access_token
     */
    String getUserAccessToken(String code);

    /**
     * 根据刷新令牌刷新用户访问令牌
     *
     * @param refreshToken 用户刷新令牌
     * @return user_access_token
     */
    String refreshUserAccessToken(String refreshToken);
}
```

下面实现类完成 Token 请求、响应解析、错误处理和 Redis 缓存。

文件位置：`src/main/java/io/github/atengk/feishu/service/impl/FeishuTokenServiceImpl.java`

```java
package io.github.atengk.feishu.service.impl;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import io.github.atengk.feishu.client.FeishuApiClient;
import io.github.atengk.feishu.config.FeishuProperties;
import io.github.atengk.feishu.exception.FeishuApiException;
import io.github.atengk.feishu.service.FeishuTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 飞书令牌服务实现
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeishuTokenServiceImpl implements FeishuTokenService {

    private static final String APP_ACCESS_TOKEN_KEY = "feishu:token:app_access_token";

    private static final String TENANT_ACCESS_TOKEN_KEY = "feishu:token:tenant_access_token";

    private static final String APP_ACCESS_TOKEN_PATH = "/open-apis/auth/v3/app_access_token/internal";

    private static final String TENANT_ACCESS_TOKEN_PATH = "/open-apis/auth/v3/tenant_access_token/internal";

    private static final String USER_ACCESS_TOKEN_PATH = "/open-apis/authen/v1/access_token";

    private static final String REFRESH_USER_ACCESS_TOKEN_PATH = "/open-apis/authen/v1/refresh_access_token";

    private final FeishuApiClient feishuApiClient;

    private final FeishuProperties feishuProperties;

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 获取应用访问令牌
     *
     * @return app_access_token
     */
    @Override
    public String getAppAccessToken() {
        String cachedToken = stringRedisTemplate.opsForValue().get(APP_ACCESS_TOKEN_KEY);
        if (StrUtil.isNotBlank(cachedToken)) {
            return cachedToken;
        }

        log.info("飞书 app_access_token 缓存未命中，开始重新获取");
        String responseBody = feishuApiClient.post(APP_ACCESS_TOKEN_PATH, feishuApiClient.buildAppCredentialBody());
        JSONObject response = parseSuccessResponse(responseBody);

        String token = response.getStr("app_access_token");
        Integer expire = response.getInt("expire", 7200);
        cacheToken(APP_ACCESS_TOKEN_KEY, token, expire);

        log.info("飞书 app_access_token 获取成功，expire={}", expire);
        return token;
    }

    /**
     * 获取租户访问令牌
     *
     * @return tenant_access_token
     */
    @Override
    public String getTenantAccessToken() {
        String cachedToken = stringRedisTemplate.opsForValue().get(TENANT_ACCESS_TOKEN_KEY);
        if (StrUtil.isNotBlank(cachedToken)) {
            return cachedToken;
        }

        log.info("飞书 tenant_access_token 缓存未命中，开始重新获取");
        String responseBody = feishuApiClient.post(TENANT_ACCESS_TOKEN_PATH, feishuApiClient.buildAppCredentialBody());
        JSONObject response = parseSuccessResponse(responseBody);

        String token = response.getStr("tenant_access_token");
        Integer expire = response.getInt("expire", 7200);
        cacheToken(TENANT_ACCESS_TOKEN_KEY, token, expire);

        log.info("飞书 tenant_access_token 获取成功，expire={}", expire);
        return token;
    }

    /**
     * 根据授权码获取用户访问令牌
     *
     * @param code OAuth 授权码
     * @return user_access_token
     */
    @Override
    public String getUserAccessToken(String code) {
        if (StrUtil.isBlank(code)) {
            throw new FeishuApiException("飞书 OAuth 授权码不能为空");
        }

        Map<String, Object> requestBody = MapUtil.<String, Object>builder()
                .put("grant_type", "authorization_code")
                .put("code", code)
                .build();

        log.info("开始通过授权码获取飞书 user_access_token");
        String tenantAccessToken = getTenantAccessToken();
        String responseBody = feishuApiClient.post(USER_ACCESS_TOKEN_PATH, requestBody, tenantAccessToken);

        JSONObject data = parseSuccessData(responseBody);
        String userAccessToken = data.getStr("access_token");
        String refreshToken = data.getStr("refresh_token");
        Integer expiresIn = data.getInt("expires_in", 7200);
        String openId = data.getStr("open_id");

        if (StrUtil.isNotBlank(openId)) {
            cacheToken(buildUserAccessTokenKey(openId), userAccessToken, expiresIn);
            if (StrUtil.isNotBlank(refreshToken)) {
                cacheToken(buildUserRefreshTokenKey(openId), refreshToken, data.getInt("refresh_expires_in", 2592000));
            }
        }

        log.info("飞书 user_access_token 获取成功，openId={}，expiresIn={}", openId, expiresIn);
        return userAccessToken;
    }

    /**
     * 根据刷新令牌刷新用户访问令牌
     *
     * @param refreshToken 用户刷新令牌
     * @return user_access_token
     */
    @Override
    public String refreshUserAccessToken(String refreshToken) {
        if (StrUtil.isBlank(refreshToken)) {
            throw new FeishuApiException("飞书 refresh_token 不能为空");
        }

        Map<String, Object> requestBody = MapUtil.<String, Object>builder()
                .put("grant_type", "refresh_token")
                .put("refresh_token", refreshToken)
                .build();

        log.info("开始刷新飞书 user_access_token");
        String tenantAccessToken = getTenantAccessToken();
        String responseBody = feishuApiClient.post(REFRESH_USER_ACCESS_TOKEN_PATH, requestBody, tenantAccessToken);

        JSONObject data = parseSuccessData(responseBody);
        String userAccessToken = data.getStr("access_token");
        String newRefreshToken = data.getStr("refresh_token");
        Integer expiresIn = data.getInt("expires_in", 7200);
        String openId = data.getStr("open_id");

        if (StrUtil.isNotBlank(openId)) {
            cacheToken(buildUserAccessTokenKey(openId), userAccessToken, expiresIn);
            if (StrUtil.isNotBlank(newRefreshToken)) {
                cacheToken(buildUserRefreshTokenKey(openId), newRefreshToken, data.getInt("refresh_expires_in", 2592000));
            }
        }

        log.info("飞书 user_access_token 刷新成功，openId={}，expiresIn={}", openId, expiresIn);
        return userAccessToken;
    }

    /**
     * 解析飞书成功响应
     *
     * @param responseBody 响应内容
     * @return 响应 JSON
     */
    private JSONObject parseSuccessResponse(String responseBody) {
        if (!JSONUtil.isTypeJSON(responseBody)) {
            throw new FeishuApiException("飞书接口响应不是合法 JSON：" + responseBody);
        }

        JSONObject response = JSONUtil.parseObj(responseBody);
        Integer code = Convert.toInt(response.get("code"), -1);
        if (!Integer.valueOf(0).equals(code)) {
            String message = response.getStr("msg", "未知错误");
            throw new FeishuApiException("飞书接口调用失败，code=" + code + "，msg=" + message);
        }

        return response;
    }

    /**
     * 解析飞书成功响应中的 data 节点
     *
     * @param responseBody 响应内容
     * @return data JSON
     */
    private JSONObject parseSuccessData(String responseBody) {
        JSONObject response = parseSuccessResponse(responseBody);
        JSONObject data = response.getJSONObject("data");
        if (data == null) {
            throw new FeishuApiException("飞书接口响应缺少 data 节点");
        }
        return data;
    }

    /**
     * 缓存访问令牌
     *
     * @param key 缓存键
     * @param token 令牌
     * @param expireSeconds 原始过期时间，单位秒
     */
    private void cacheToken(String key, String token, Integer expireSeconds) {
        if (StrUtil.isBlank(token)) {
            throw new FeishuApiException("飞书 Token 为空，key=" + key);
        }

        long ttl = Math.max(60L, expireSeconds - feishuProperties.getTokenExpireOffsetSeconds());
        stringRedisTemplate.opsForValue().set(key, token, ttl, TimeUnit.SECONDS);
        log.info("飞书 Token 已写入缓存，key={}，ttl={}秒", key, ttl);
    }

    /**
     * 构建用户访问令牌缓存键
     *
     * @param openId 用户 open_id
     * @return 缓存键
     */
    private String buildUserAccessTokenKey(String openId) {
        return "feishu:token:user_access_token:" + openId;
    }

    /**
     * 构建用户刷新令牌缓存键
     *
     * @param openId 用户 open_id
     * @return 缓存键
     */
    private String buildUserRefreshTokenKey(String openId) {
        return "feishu:token:user_refresh_token:" + openId;
    }
}
```

为了验证 Token 服务是否正常，可以先提供一个内部测试接口。生产环境建议移除此类测试接口，或者使用 Sa-Token、Spring Security、网关鉴权等方式限制访问。

文件位置：`src/main/java/io/github/atengk/feishu/controller/FeishuTokenController.java`

```java
package io.github.atengk.feishu.controller;

import io.github.atengk.feishu.service.FeishuTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 飞书令牌测试接口
 *
 * @author Ateng
 * @since 2026-05-07
 */
@RestController
@RequiredArgsConstructor
public class FeishuTokenController {

    private final FeishuTokenService feishuTokenService;

    /**
     * 获取应用访问令牌
     *
     * @return app_access_token
     */
    @GetMapping("/api/feishu/token/app")
    public String getAppAccessToken() {
        return feishuTokenService.getAppAccessToken();
    }

    /**
     * 获取租户访问令牌
     *
     * @return tenant_access_token
     */
    @GetMapping("/api/feishu/token/tenant")
    public String getTenantAccessToken() {
        return feishuTokenService.getTenantAccessToken();
    }

    /**
     * 根据 OAuth 授权码获取用户访问令牌
     *
     * @param code OAuth 授权码
     * @return user_access_token
     */
    @GetMapping("/api/feishu/token/user")
    public String getUserAccessToken(@RequestParam String code) {
        return feishuTokenService.getUserAccessToken(code);
    }
}
```

接口验证方式如下。

```bash
# 获取 app_access_token
curl "http://localhost:8080/api/feishu/token/app"

# 获取 tenant_access_token
curl "http://localhost:8080/api/feishu/token/tenant"

# 使用 OAuth 授权码获取 user_access_token
curl "http://localhost:8080/api/feishu/token/user?code=replace-with-oauth-code"
```

Token 缓存策略建议如下：

| Token 类型            | 缓存键                                     | 缓存方式                 |
| --------------------- | ------------------------------------------ | ------------------------ |
| `app_access_token`    | `feishu:token:app_access_token`            | 全局缓存                 |
| `tenant_access_token` | `feishu:token:tenant_access_token`         | 企业内部应用全局缓存     |
| `user_access_token`   | `feishu:token:user_access_token:{openId}`  | 按用户缓存               |
| `refresh_token`       | `feishu:token:user_refresh_token:{openId}` | 按用户缓存，建议加密存储 |

令牌管理需要注意以下事项：

| 注意事项            | 说明                                                         |
| ------------------- | ------------------------------------------------------------ |
| 提前过期            | Redis TTL 应小于飞书返回的 `expire` 或 `expires_in`          |
| 防止并发刷新        | 高并发系统建议增加分布式锁，避免多个线程同时刷新 Token       |
| 不打印完整 Token    | 生产日志中不要打印完整访问令牌                               |
| 区分 Token 类型     | 不要用 `app_access_token` 代替 `tenant_access_token` 调用企业级接口 |
| 用户 Token 按人隔离 | `user_access_token` 必须按用户维度缓存或存储                 |
| 刷新失败需重新授权  | `refresh_token` 失效后，需要引导用户重新 OAuth 授权          |
| 生产接口需鉴权      | Token 测试接口不应直接暴露到公网                             |

## 消息能力开发

本章节用于封装飞书消息发送能力，包括文本消息、富文本消息、卡片消息和群机器人 Webhook 消息。飞书发送消息接口为 `POST /open-apis/im/v1/messages`，支持文本、富文本、可交互消息卡片等类型；调用前需要应用开启机器人能力，给用户发消息时用户需要在机器人可用范围内，给群组发消息时机器人需要在群组中。([Apifox](https://s.apifox.cn/apidoc/docs-site/532425/api-58348294?utm_source=chatgpt.com))

### 发送文本消息

文本消息适用于普通通知、任务提醒、异常告警等场景。飞书文本消息的 `msg_type` 为 `text`，`content` 字段需要是字符串化后的 JSON，例如 `{"text":"消息内容"}`，不能直接传 JSON 对象。([飞书 API](https://feishu.apifox.cn/doc-1945309?utm_source=chatgpt.com))

建议先定义消息服务接口，后续文本、富文本、卡片和群机器人消息统一放在该服务中。

文件位置：`src/main/java/io/github/atengk/feishu/service/FeishuMessageService.java`

```java
package io.github.atengk.feishu.service;

import java.util.Map;

/**
 * 飞书消息服务
 *
 * @author Ateng
 * @since 2026-05-07
 */
public interface FeishuMessageService {

    /**
     * 发送文本消息
     *
     * @param receiveId 接收者 ID
     * @param receiveIdType 接收者 ID 类型
     * @param text 文本内容
     * @return 消息 ID
     */
    String sendTextMessage(String receiveId, String receiveIdType, String text);

    /**
     * 发送富文本消息
     *
     * @param receiveId 接收者 ID
     * @param receiveIdType 接收者 ID 类型
     * @param title 标题
     * @param text 正文
     * @return 消息 ID
     */
    String sendRichTextMessage(String receiveId, String receiveIdType, String title, String text);

    /**
     * 发送卡片消息
     *
     * @param receiveId 接收者 ID
     * @param receiveIdType 接收者 ID 类型
     * @param title 标题
     * @param content 内容
     * @param buttonText 按钮文案
     * @param buttonUrl 按钮地址
     * @return 消息 ID
     */
    String sendCardMessage(String receiveId, String receiveIdType, String title, String content, String buttonText, String buttonUrl);

    /**
     * 群机器人发送文本消息
     *
     * @param webhookUrl 群机器人 Webhook 地址
     * @param secret 签名密钥，未开启签名时可为空
     * @param text 文本内容
     * @return 响应内容
     */
    String sendWebhookTextMessage(String webhookUrl, String secret, String text);

    /**
     * 群机器人发送卡片消息
     *
     * @param webhookUrl 群机器人 Webhook 地址
     * @param secret 签名密钥，未开启签名时可为空
     * @param card 卡片内容
     * @return 响应内容
     */
    String sendWebhookCardMessage(String webhookUrl, String secret, Map<String, Object> card);
}
```

下面工具类用于统一解析飞书接口响应，避免每个 Service 重复判断 `code` 和 `data`。

文件位置：`src/main/java/io/github/atengk/feishu/util/FeishuResponseUtil.java`

```java
package io.github.atengk.feishu.util;

import cn.hutool.core.convert.Convert;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import io.github.atengk.feishu.exception.FeishuApiException;

/**
 * 飞书接口响应工具类
 *
 * @author Ateng
 * @since 2026-05-07
 */
public class FeishuResponseUtil {

    private FeishuResponseUtil() {
    }

    /**
     * 解析成功响应
     *
     * @param responseBody 响应内容
     * @return 响应 JSON
     */
    public static JSONObject parseSuccessResponse(String responseBody) {
        if (!JSONUtil.isTypeJSON(responseBody)) {
            throw new FeishuApiException("飞书接口响应不是合法 JSON：" + responseBody);
        }

        JSONObject response = JSONUtil.parseObj(responseBody);
        Integer code = Convert.toInt(response.get("code"), -1);
        if (!Integer.valueOf(0).equals(code)) {
            String message = response.getStr("msg", "未知错误");
            throw new FeishuApiException("飞书接口调用失败，code=" + code + "，msg=" + message);
        }

        return response;
    }

    /**
     * 解析成功响应中的 data 节点
     *
     * @param responseBody 响应内容
     * @return data 节点
     */
    public static JSONObject parseSuccessData(String responseBody) {
        JSONObject response = parseSuccessResponse(responseBody);
        JSONObject data = response.getJSONObject("data");
        return data == null ? new JSONObject() : data;
    }
}
```

下面实现类封装文本消息、富文本消息、卡片消息和 Webhook 群机器人推送。

文件位置：`src/main/java/io/github/atengk/feishu/service/impl/FeishuMessageServiceImpl.java`

```java
package io.github.atengk.feishu.service.impl;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import io.github.atengk.feishu.client.FeishuApiClient;
import io.github.atengk.feishu.config.FeishuProperties;
import io.github.atengk.feishu.exception.FeishuApiException;
import io.github.atengk.feishu.service.FeishuMessageService;
import io.github.atengk.feishu.service.FeishuTokenService;
import io.github.atengk.feishu.util.FeishuResponseUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 飞书消息服务实现
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeishuMessageServiceImpl implements FeishuMessageService {

    private static final String SEND_MESSAGE_PATH = "/open-apis/im/v1/messages?receive_id_type={}";

    private final FeishuApiClient feishuApiClient;

    private final FeishuTokenService feishuTokenService;

    private final FeishuProperties feishuProperties;

    /**
     * 发送文本消息
     *
     * @param receiveId 接收者 ID
     * @param receiveIdType 接收者 ID 类型
     * @param text 文本内容
     * @return 消息 ID
     */
    @Override
    public String sendTextMessage(String receiveId, String receiveIdType, String text) {
        checkMessageParam(receiveId, receiveIdType, text);

        Map<String, Object> content = MapUtil.<String, Object>builder()
                .put("text", text)
                .build();

        Map<String, Object> requestBody = MapUtil.<String, Object>builder()
                .put("receive_id", receiveId)
                .put("msg_type", "text")
                .put("content", JSONUtil.toJsonStr(content))
                .build();

        return sendMessage(receiveIdType, requestBody);
    }

    /**
     * 发送富文本消息
     *
     * @param receiveId 接收者 ID
     * @param receiveIdType 接收者 ID 类型
     * @param title 标题
     * @param text 正文
     * @return 消息 ID
     */
    @Override
    public String sendRichTextMessage(String receiveId, String receiveIdType, String title, String text) {
        checkMessageParam(receiveId, receiveIdType, text);

        Map<String, Object> textElement = MapUtil.<String, Object>builder()
                .put("tag", "text")
                .put("text", text)
                .build();

        Map<String, Object> linkElement = MapUtil.<String, Object>builder()
                .put("tag", "a")
                .put("href", "https://open.feishu.cn")
                .put("text", "查看飞书开放平台")
                .build();

        Map<String, Object> zhCnContent = MapUtil.<String, Object>builder()
                .put("title", StrUtil.blankToDefault(title, "系统通知"))
                .put("content", List.of(
                        List.of(textElement),
                        List.of(linkElement)
                ))
                .build();

        Map<String, Object> content = MapUtil.<String, Object>builder()
                .put("zh_cn", zhCnContent)
                .build();

        Map<String, Object> requestBody = MapUtil.<String, Object>builder()
                .put("receive_id", receiveId)
                .put("msg_type", "post")
                .put("content", JSONUtil.toJsonStr(content))
                .build();

        return sendMessage(receiveIdType, requestBody);
    }

    /**
     * 发送卡片消息
     *
     * @param receiveId 接收者 ID
     * @param receiveIdType 接收者 ID 类型
     * @param title 标题
     * @param content 内容
     * @param buttonText 按钮文案
     * @param buttonUrl 按钮地址
     * @return 消息 ID
     */
    @Override
    public String sendCardMessage(String receiveId, String receiveIdType, String title, String content, String buttonText, String buttonUrl) {
        checkMessageParam(receiveId, receiveIdType, content);

        Map<String, Object> card = buildSimpleCard(title, content, buttonText, buttonUrl);

        Map<String, Object> requestBody = MapUtil.<String, Object>builder()
                .put("receive_id", receiveId)
                .put("msg_type", "interactive")
                .put("content", JSONUtil.toJsonStr(card))
                .build();

        return sendMessage(receiveIdType, requestBody);
    }

    /**
     * 群机器人发送文本消息
     *
     * @param webhookUrl 群机器人 Webhook 地址
     * @param secret 签名密钥，未开启签名时可为空
     * @param text 文本内容
     * @return 响应内容
     */
    @Override
    public String sendWebhookTextMessage(String webhookUrl, String secret, String text) {
        if (StrUtil.hasBlank(webhookUrl, text)) {
            throw new FeishuApiException("群机器人 Webhook 地址和文本内容不能为空");
        }

        Map<String, Object> payload = MapUtil.<String, Object>builder(new LinkedHashMap<>())
                .put("msg_type", "text")
                .put("content", MapUtil.<String, Object>builder()
                        .put("text", text)
                        .build())
                .build();

        return sendWebhookMessage(webhookUrl, secret, payload);
    }

    /**
     * 群机器人发送卡片消息
     *
     * @param webhookUrl 群机器人 Webhook 地址
     * @param secret 签名密钥，未开启签名时可为空
     * @param card 卡片内容
     * @return 响应内容
     */
    @Override
    public String sendWebhookCardMessage(String webhookUrl, String secret, Map<String, Object> card) {
        if (StrUtil.isBlank(webhookUrl) || MapUtil.isEmpty(card)) {
            throw new FeishuApiException("群机器人 Webhook 地址和卡片内容不能为空");
        }

        Map<String, Object> payload = MapUtil.<String, Object>builder(new LinkedHashMap<>())
                .put("msg_type", "interactive")
                .put("card", card)
                .build();

        return sendWebhookMessage(webhookUrl, secret, payload);
    }

    /**
     * 发送应用机器人消息
     *
     * @param receiveIdType 接收者 ID 类型
     * @param requestBody 请求体
     * @return 消息 ID
     */
    private String sendMessage(String receiveIdType, Map<String, Object> requestBody) {
        String path = StrUtil.format(SEND_MESSAGE_PATH, StrUtil.blankToDefault(receiveIdType, "open_id"));
        String tenantAccessToken = feishuTokenService.getTenantAccessToken();
        String responseBody = feishuApiClient.post(path, requestBody, tenantAccessToken);

        JSONObject data = FeishuResponseUtil.parseSuccessData(responseBody);
        String messageId = data.getStr("message_id");
        if (StrUtil.isBlank(messageId) && data.getJSONObject("message") != null) {
            messageId = data.getJSONObject("message").getStr("message_id");
        }

        log.info("飞书消息发送成功，receiveId={}，msgType={}，messageId={}",
                requestBody.get("receive_id"), requestBody.get("msg_type"), messageId);
        return messageId;
    }

    /**
     * 发送群机器人 Webhook 消息
     *
     * @param webhookUrl 群机器人 Webhook 地址
     * @param secret 签名密钥
     * @param payload 请求体
     * @return 响应内容
     */
    private String sendWebhookMessage(String webhookUrl, String secret, Map<String, Object> payload) {
        try {
            if (StrUtil.isNotBlank(secret)) {
                String timestamp = Convert.toStr(Instant.now().getEpochSecond());
                payload.put("timestamp", timestamp);
                payload.put("sign", buildWebhookSign(timestamp, secret));
            }

            String responseBody = HttpRequest.post(webhookUrl)
                    .timeout(feishuProperties.getReadTimeout())
                    .connectionTimeout(feishuProperties.getConnectTimeout())
                    .header("Content-Type", "application/json; charset=utf-8")
                    .body(JSONUtil.toJsonStr(payload))
                    .execute()
                    .body();

            FeishuResponseUtil.parseSuccessResponse(responseBody);
            log.info("飞书群机器人消息发送成功，msgType={}", payload.get("msg_type"));
            return responseBody;
        } catch (FeishuApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("飞书群机器人消息发送异常，webhookUrl={}", webhookUrl, e);
            throw new FeishuApiException("飞书群机器人消息发送异常：" + e.getMessage(), e);
        }
    }

    /**
     * 构建飞书群机器人签名
     *
     * @param timestamp 秒级时间戳
     * @param secret 签名密钥
     * @return 签名字符串
     */
    private String buildWebhookSign(String timestamp, String secret) {
        try {
            String stringToSign = timestamp + "\n" + secret;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(stringToSign.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] signData = mac.doFinal(new byte[]{});
            return Base64.getEncoder().encodeToString(signData);
        } catch (Exception e) {
            throw new FeishuApiException("飞书群机器人签名生成失败：" + e.getMessage(), e);
        }
    }

    /**
     * 构建简单卡片
     *
     * @param title 标题
     * @param content 内容
     * @param buttonText 按钮文案
     * @param buttonUrl 按钮地址
     * @return 卡片内容
     */
    private Map<String, Object> buildSimpleCard(String title, String content, String buttonText, String buttonUrl) {
        Map<String, Object> card = MapUtil.<String, Object>builder(new LinkedHashMap<>())
                .put("config", MapUtil.<String, Object>builder()
                        .put("wide_screen_mode", true)
                        .build())
                .put("header", MapUtil.<String, Object>builder()
                        .put("template", "blue")
                        .put("title", MapUtil.<String, Object>builder()
                                .put("tag", "plain_text")
                                .put("content", StrUtil.blankToDefault(title, "系统通知"))
                                .build())
                        .build())
                .put("elements", List.of(
                        MapUtil.<String, Object>builder()
                                .put("tag", "div")
                                .put("text", MapUtil.<String, Object>builder()
                                        .put("tag", "lark_md")
                                        .put("content", content)
                                        .build())
                                .build(),
                        MapUtil.<String, Object>builder()
                                .put("tag", "action")
                                .put("actions", List.of(
                                        MapUtil.<String, Object>builder()
                                                .put("tag", "button")
                                                .put("text", MapUtil.<String, Object>builder()
                                                        .put("tag", "plain_text")
                                                        .put("content", StrUtil.blankToDefault(buttonText, "查看详情"))
                                                        .build())
                                                .put("url", StrUtil.blankToDefault(buttonUrl, "https://open.feishu.cn"))
                                                .put("type", "primary")
                                                .build()
                                ))
                                .build()
                ))
                .build();

        return card;
    }

    /**
     * 校验消息参数
     *
     * @param receiveId 接收者 ID
     * @param receiveIdType 接收者 ID 类型
     * @param content 消息内容
     */
    private void checkMessageParam(String receiveId, String receiveIdType, String content) {
        if (StrUtil.hasBlank(receiveId, receiveIdType, content)) {
            throw new FeishuApiException("飞书消息参数不能为空");
        }
    }
}
```

### 发送富文本消息

富文本消息适用于结构化通知，例如审批提醒、任务通知、异常报告等。富文本可以包含标题、段落、文本、链接、At 用户、图片等元素；飞书富文本内容按语言组织，例如 `zh_cn`，每个段落由一个数组表示，每个段落中可以放多个元素。([Apifox](https://s.apifox.cn/apidoc/docs-site/532425/doc-1945306?utm_source=chatgpt.com))

上面的 `sendRichTextMessage` 方法已经给出一个基础实现：

| 字段                    | 说明                                 |
| ----------------------- | ------------------------------------ |
| `msg_type`              | 固定为 `post`                        |
| `content.zh_cn.title`   | 中文标题                             |
| `content.zh_cn.content` | 富文本正文段落                       |
| `tag=text`              | 普通文本                             |
| `tag=a`                 | 超链接                               |
| `tag=at`                | At 用户                              |
| `tag=img`               | 图片，需要先上传图片获取 `image_key` |

业务系统中可以根据不同场景扩展富文本内容。例如审批通知可以包含审批编号、申请人、金额、状态和详情链接；异常告警可以包含服务名、环境、异常类型、触发时间和日志地址。

### 发送卡片消息

卡片消息适用于需要突出展示内容并支持按钮交互的场景，例如审批待办、工单通知、告警详情、任务分配等。飞书发送消息接口支持可交互消息卡片，发送时 `msg_type` 使用 `interactive`，`content` 需要传入字符串化后的卡片 JSON。([Apifox](https://s.apifox.cn/apidoc/docs-site/532425/api-58348294?utm_source=chatgpt.com))

上面的 `sendCardMessage` 方法已经封装了一个简单卡片，包含标题、正文和按钮。实际业务中可以按场景扩展卡片结构，例如增加字段列表、多按钮、不同颜色模板、Markdown 内容等。

建议卡片消息优先用于以下场景：

| 场景     | 说明                                           |
| -------- | ---------------------------------------------- |
| 审批提醒 | 展示审批标题、申请人、金额、申请时间和详情按钮 |
| 工单分配 | 展示工单编号、优先级、负责人和处理入口         |
| 运维告警 | 展示服务名、环境、异常等级、触发时间和日志链接 |
| 任务通知 | 展示任务名称、截止时间、处理人和任务地址       |

### 群机器人消息推送

群机器人 Webhook 推送适用于简单的群通知场景，例如定时任务结果、部署通知、监控告警、CI/CD 构建结果等。群机器人通常在飞书群中添加自定义机器人后获取 Webhook 地址，然后服务端向该地址发送 HTTP POST 请求即可完成推送；如果开启签名校验，请求体需要携带 `timestamp` 和 `sign` 字段，签名算法通常使用 `timestamp + "\n" + secret` 作为签名字符串，再使用 HmacSHA256 和 Base64 计算。([DamoDev](https://damodev.csdn.net/6880a541bb9d8e0ecec2b0a3.html?utm_source=chatgpt.com))

应用机器人和群机器人 Webhook 的区别如下：

| 类型             | 适用场景                                 | 是否需要应用 Token | 是否支持接收事件 |
| ---------------- | ---------------------------------------- | ------------------ | ---------------- |
| 应用机器人       | 深度业务集成、用户消息、群消息、事件交互 | 需要               | 支持             |
| 群机器人 Webhook | 简单群通知、告警、流水线通知             | 不需要             | 不支持或能力有限 |

建议正式业务系统优先使用企业自建应用机器人，因为其权限、审计、消息能力和事件订阅更完整。群机器人 Webhook 更适合轻量通知或临时告警。

为了方便调试，可以提供一个消息测试接口。生产环境应加上鉴权，避免被外部随意调用。

文件位置：`src/main/java/io/github/atengk/feishu/controller/FeishuMessageController.java`

```java
package io.github.atengk.feishu.controller;

import io.github.atengk.feishu.service.FeishuMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 飞书消息测试接口
 *
 * @author Ateng
 * @since 2026-05-07
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/feishu/message")
public class FeishuMessageController {

    private final FeishuMessageService feishuMessageService;

    /**
     * 发送文本消息
     *
     * @param receiveId 接收者 ID
     * @param receiveIdType 接收者 ID 类型
     * @param text 文本内容
     * @return 消息 ID
     */
    @PostMapping("/text")
    public String sendTextMessage(@RequestParam String receiveId,
                                  @RequestParam(defaultValue = "open_id") String receiveIdType,
                                  @RequestParam String text) {
        return feishuMessageService.sendTextMessage(receiveId, receiveIdType, text);
    }

    /**
     * 发送富文本消息
     *
     * @param receiveId 接收者 ID
     * @param receiveIdType 接收者 ID 类型
     * @param title 标题
     * @param text 正文
     * @return 消息 ID
     */
    @PostMapping("/rich-text")
    public String sendRichTextMessage(@RequestParam String receiveId,
                                      @RequestParam(defaultValue = "open_id") String receiveIdType,
                                      @RequestParam String title,
                                      @RequestParam String text) {
        return feishuMessageService.sendRichTextMessage(receiveId, receiveIdType, title, text);
    }

    /**
     * 发送卡片消息
     *
     * @param receiveId 接收者 ID
     * @param receiveIdType 接收者 ID 类型
     * @param title 标题
     * @param content 内容
     * @param buttonText 按钮文案
     * @param buttonUrl 按钮地址
     * @return 消息 ID
     */
    @PostMapping("/card")
    public String sendCardMessage(@RequestParam String receiveId,
                                  @RequestParam(defaultValue = "open_id") String receiveIdType,
                                  @RequestParam String title,
                                  @RequestParam String content,
                                  @RequestParam(defaultValue = "查看详情") String buttonText,
                                  @RequestParam(defaultValue = "https://open.feishu.cn") String buttonUrl) {
        return feishuMessageService.sendCardMessage(receiveId, receiveIdType, title, content, buttonText, buttonUrl);
    }

    /**
     * 群机器人发送文本消息
     *
     * @param webhookUrl 群机器人 Webhook 地址
     * @param secret 签名密钥
     * @param text 文本内容
     * @return 响应内容
     */
    @PostMapping("/webhook/text")
    public String sendWebhookTextMessage(@RequestParam String webhookUrl,
                                         @RequestParam(required = false) String secret,
                                         @RequestParam String text) {
        return feishuMessageService.sendWebhookTextMessage(webhookUrl, secret, text);
    }

    /**
     * 群机器人发送卡片消息
     *
     * @param webhookUrl 群机器人 Webhook 地址
     * @param secret 签名密钥
     * @param card 卡片内容
     * @return 响应内容
     */
    @PostMapping("/webhook/card")
    public String sendWebhookCardMessage(@RequestParam String webhookUrl,
                                         @RequestParam(required = false) String secret,
                                         @RequestBody Map<String, Object> card) {
        return feishuMessageService.sendWebhookCardMessage(webhookUrl, secret, card);
    }
}
```

接口验证命令如下。

```bash
# 发送文本消息
curl -X POST "http://localhost:8080/api/feishu/message/text" \
  -d "receiveId=ou_xxxxxxxxxxxxx" \
  -d "receiveIdType=open_id" \
  -d "text=这是一条来自 Spring Boot 的飞书文本消息"

# 发送富文本消息
curl -X POST "http://localhost:8080/api/feishu/message/rich-text" \
  -d "receiveId=ou_xxxxxxxxxxxxx" \
  -d "receiveIdType=open_id" \
  -d "title=系统通知" \
  -d "text=审批任务已提交，请及时处理"

# 发送卡片消息
curl -X POST "http://localhost:8080/api/feishu/message/card" \
  -d "receiveId=ou_xxxxxxxxxxxxx" \
  -d "receiveIdType=open_id" \
  -d "title=工单提醒" \
  -d "content=你有一个新的待处理工单，请尽快查看。" \
  -d "buttonText=查看工单" \
  -d "buttonUrl=https://example.com/ticket/10001"

# 群机器人发送文本消息
curl -X POST "http://localhost:8080/api/feishu/message/webhook/text" \
  -d "webhookUrl=https://open.feishu.cn/open-apis/bot/v2/hook/xxxxxxxxxxxxx" \
  -d "secret=replace-with-secret" \
  -d "text=部署完成：springboot-feishu-demo"
```

## 用户与组织架构

本章节用于封装飞书通讯录相关能力，包括获取用户信息、获取部门信息，以及将飞书用户与业务系统账号进行绑定。用户与组织架构能力通常用于权限控制、审批流、消息接收人匹配、账号免登录和组织同步等场景。

### 获取用户信息

飞书获取单个用户信息接口为 `GET /open-apis/contact/v3/users/{user_id}`，可以通过 `user_id_type` 指定用户 ID 类型，例如 `open_id`；接口返回用户名称、邮箱、手机号、头像、部门等信息，但部分敏感字段需要额外字段权限才会返回。([Apifox](https://s.apifox.cn/apidoc/docs-site/532425/api-58266827?utm_source=chatgpt.com))

由于前文 `FeishuApiClient` 只封装了 POST 请求，这里先补充 GET 请求方法。

在 `FeishuApiClient` 中新增以下方法。

```java
/**
 * 发送带 Bearer Token 的 GET 请求
 *
 * @param path 接口路径
 * @param bearerToken Bearer Token
 * @return 响应 JSON 字符串
 */
public String get(String path, String bearerToken) {
    String url = buildUrl(path);

    try {
        HttpRequest request = HttpRequest.get(url)
                .timeout(feishuProperties.getReadTimeout())
                .connectionTimeout(feishuProperties.getConnectTimeout())
                .header("Content-Type", "application/json; charset=utf-8");

        if (StrUtil.isNotBlank(bearerToken)) {
            request.header("Authorization", "Bearer " + bearerToken);
        }

        String responseBody = request.execute().body();
        log.info("飞书接口请求完成，path={}，response={}", path, responseBody);
        return responseBody;
    } catch (Exception e) {
        log.error("飞书接口请求异常，path={}", path, e);
        throw new FeishuApiException("飞书接口请求异常：" + e.getMessage(), e);
    }
}
```

下面定义通讯录服务接口。

文件位置：`src/main/java/io/github/atengk/feishu/service/FeishuContactService.java`

```java
package io.github.atengk.feishu.service;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;

import java.util.List;

/**
 * 飞书通讯录服务
 *
 * @author Ateng
 * @since 2026-05-07
 */
public interface FeishuContactService {

    /**
     * 获取单个用户信息
     *
     * @param userId 用户 ID
     * @param userIdType 用户 ID 类型
     * @return 用户信息
     */
    JSONObject getUser(String userId, String userIdType);

    /**
     * 通过手机号或邮箱获取用户 ID
     *
     * @param mobiles 手机号列表
     * @param emails 邮箱列表
     * @param userIdType 返回的用户 ID 类型
     * @return 用户 ID 列表
     */
    JSONArray batchGetUserId(List<String> mobiles, List<String> emails, String userIdType);

    /**
     * 获取单个部门信息
     *
     * @param departmentId 部门 ID
     * @param departmentIdType 部门 ID 类型
     * @return 部门信息
     */
    JSONObject getDepartment(String departmentId, String departmentIdType);
}
```

下面实现用户信息、用户 ID 查询和部门信息查询。

文件位置：`src/main/java/io/github/atengk/feishu/service/impl/FeishuContactServiceImpl.java`

```java
package io.github.atengk.feishu.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import io.github.atengk.feishu.client.FeishuApiClient;
import io.github.atengk.feishu.exception.FeishuApiException;
import io.github.atengk.feishu.service.FeishuContactService;
import io.github.atengk.feishu.service.FeishuTokenService;
import io.github.atengk.feishu.util.FeishuResponseUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 飞书通讯录服务实现
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeishuContactServiceImpl implements FeishuContactService {

    private static final String GET_USER_PATH = "/open-apis/contact/v3/users/{}?user_id_type={}&department_id_type=open_department_id";

    private static final String BATCH_GET_USER_ID_PATH = "/open-apis/contact/v3/users/batch_get_id?user_id_type={}";

    private static final String GET_DEPARTMENT_PATH = "/open-apis/contact/v3/departments/{}?user_id_type=open_id&department_id_type={}";

    private final FeishuApiClient feishuApiClient;

    private final FeishuTokenService feishuTokenService;

    /**
     * 获取单个用户信息
     *
     * @param userId 用户 ID
     * @param userIdType 用户 ID 类型
     * @return 用户信息
     */
    @Override
    public JSONObject getUser(String userId, String userIdType) {
        if (StrUtil.isBlank(userId)) {
            throw new FeishuApiException("飞书用户 ID 不能为空");
        }

        String finalUserIdType = StrUtil.blankToDefault(userIdType, "open_id");
        String path = StrUtil.format(GET_USER_PATH, userId, finalUserIdType);
        String responseBody = feishuApiClient.get(path, feishuTokenService.getTenantAccessToken());

        JSONObject data = FeishuResponseUtil.parseSuccessData(responseBody);
        JSONObject user = data.getJSONObject("user");
        if (user == null) {
            throw new FeishuApiException("飞书用户信息不存在，userId=" + userId);
        }

        log.info("飞书用户信息查询成功，userId={}，userIdType={}", userId, finalUserIdType);
        return user;
    }

    /**
     * 通过手机号或邮箱获取用户 ID
     *
     * @param mobiles 手机号列表
     * @param emails 邮箱列表
     * @param userIdType 返回的用户 ID 类型
     * @return 用户 ID 列表
     */
    @Override
    public JSONArray batchGetUserId(List<String> mobiles, List<String> emails, String userIdType) {
        if (CollUtil.isEmpty(mobiles) && CollUtil.isEmpty(emails)) {
            throw new FeishuApiException("手机号和邮箱不能同时为空");
        }

        String finalUserIdType = StrUtil.blankToDefault(userIdType, "open_id");
        String path = StrUtil.format(BATCH_GET_USER_ID_PATH, finalUserIdType);

        Map<String, Object> requestBody = MapUtil.<String, Object>builder()
                .put("mobiles", CollUtil.emptyIfNull(mobiles))
                .put("emails", CollUtil.emptyIfNull(emails))
                .build();

        String responseBody = feishuApiClient.post(path, requestBody, feishuTokenService.getTenantAccessToken());
        JSONObject data = FeishuResponseUtil.parseSuccessData(responseBody);
        JSONArray userList = data.getJSONArray("user_list");

        log.info("飞书用户 ID 批量查询完成，mobileCount={}，emailCount={}，resultCount={}",
                CollUtil.size(mobiles), CollUtil.size(emails), userList == null ? 0 : userList.size());

        return userList == null ? new JSONArray() : userList;
    }

    /**
     * 获取单个部门信息
     *
     * @param departmentId 部门 ID
     * @param departmentIdType 部门 ID 类型
     * @return 部门信息
     */
    @Override
    public JSONObject getDepartment(String departmentId, String departmentIdType) {
        if (StrUtil.isBlank(departmentId)) {
            throw new FeishuApiException("飞书部门 ID 不能为空");
        }

        String finalDepartmentIdType = StrUtil.blankToDefault(departmentIdType, "open_department_id");
        String path = StrUtil.format(GET_DEPARTMENT_PATH, departmentId, finalDepartmentIdType);
        String responseBody = feishuApiClient.get(path, feishuTokenService.getTenantAccessToken());

        JSONObject data = FeishuResponseUtil.parseSuccessData(responseBody);
        JSONObject department = data.getJSONObject("department");
        if (department == null) {
            throw new FeishuApiException("飞书部门信息不存在，departmentId=" + departmentId);
        }

        log.info("飞书部门信息查询成功，departmentId={}，departmentIdType={}", departmentId, finalDepartmentIdType);
        return department;
    }
}
```

### 获取部门信息

飞书获取单个部门信息接口为 `GET /open-apis/contact/v3/departments/{department_id}`。使用 `tenant_access_token` 查询部门时，应用需要拥有待查询部门的通讯录授权；如果查询根部门，需要拥有全员权限。([Apifox](https://s.apifox.cn/apidoc/docs-site/532425/api-9021002?utm_source=chatgpt.com))

部门信息通常用于以下场景：

| 场景         | 说明                                 |
| ------------ | ------------------------------------ |
| 组织架构同步 | 定时同步飞书部门到本地组织表         |
| 权限控制     | 根据部门判断用户可访问的数据范围     |
| 审批流       | 根据部门负责人、部门层级设置审批节点 |
| 消息推送     | 按部门筛选消息接收人                 |
| 账号绑定     | 绑定用户时同步用户所属部门           |

下面提供通讯录测试接口，用于验证用户和部门查询能力。

文件位置：`src/main/java/io/github/atengk/feishu/controller/FeishuContactController.java`

```java
package io.github.atengk.feishu.controller;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import io.github.atengk.feishu.service.FeishuContactService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 飞书通讯录测试接口
 *
 * @author Ateng
 * @since 2026-05-07
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/feishu/contact")
public class FeishuContactController {

    private final FeishuContactService feishuContactService;

    /**
     * 获取用户信息
     *
     * @param userId 用户 ID
     * @param userIdType 用户 ID 类型
     * @return 用户信息
     */
    @GetMapping("/user/{userId}")
    public JSONObject getUser(@PathVariable String userId,
                              @RequestParam(defaultValue = "open_id") String userIdType) {
        return feishuContactService.getUser(userId, userIdType);
    }

    /**
     * 通过手机号或邮箱获取用户 ID
     *
     * @param mobiles 手机号列表
     * @param emails 邮箱列表
     * @param userIdType 返回用户 ID 类型
     * @return 用户 ID 列表
     */
    @PostMapping("/user/batch-get-id")
    public JSONArray batchGetUserId(@RequestParam(required = false) List<String> mobiles,
                                    @RequestParam(required = false) List<String> emails,
                                    @RequestParam(defaultValue = "open_id") String userIdType) {
        return feishuContactService.batchGetUserId(mobiles, emails, userIdType);
    }

    /**
     * 获取部门信息
     *
     * @param departmentId 部门 ID
     * @param departmentIdType 部门 ID 类型
     * @return 部门信息
     */
    @GetMapping("/department/{departmentId}")
    public JSONObject getDepartment(@PathVariable String departmentId,
                                    @RequestParam(defaultValue = "open_department_id") String departmentIdType) {
        return feishuContactService.getDepartment(departmentId, departmentIdType);
    }
}
```

接口验证命令如下。

```bash
# 获取单个用户信息
curl "http://localhost:8080/api/feishu/contact/user/ou_xxxxxxxxxxxxx?userIdType=open_id"

# 通过手机号查询用户 ID
curl -X POST "http://localhost:8080/api/feishu/contact/user/batch-get-id?mobiles=13812345678&userIdType=open_id"

# 通过邮箱查询用户 ID
curl -X POST "http://localhost:8080/api/feishu/contact/user/batch-get-id?emails=zhangsan@example.com&userIdType=open_id"

# 获取部门信息
curl "http://localhost:8080/api/feishu/contact/department/od-xxxxxxxxxxxxx?departmentIdType=open_department_id"
```

### 用户与系统账号绑定

用户与系统账号绑定用于将业务系统中的用户账号与飞书用户身份建立映射关系。绑定后，业务系统可以根据本地用户 ID 找到飞书 `open_id`，从而向用户发送消息、参与审批流、识别飞书 OAuth 登录用户，或者同步用户所属部门信息。

飞书支持通过手机号或邮箱获取用户 ID，接口为 `POST /open-apis/contact/v3/users/batch_get_id`，可以返回 `open_id`、`user_id`、`union_id` 等标识；如果手机号、邮箱不存在或应用无权限查看对应用户，则结果列表为空。([Apifox](https://s.apifox.cn/apidoc/docs-site/532425/api-9089109?utm_source=chatgpt.com))

建议绑定关系表如下。

```sql
CREATE TABLE sys_user_feishu_bind (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    sys_user_id BIGINT NOT NULL COMMENT '系统用户ID',
    username VARCHAR(100) NOT NULL COMMENT '系统用户名',
    open_id VARCHAR(128) NOT NULL COMMENT '飞书 open_id',
    user_id VARCHAR(128) DEFAULT NULL COMMENT '飞书 user_id',
    union_id VARCHAR(128) DEFAULT NULL COMMENT '飞书 union_id',
    email VARCHAR(200) DEFAULT NULL COMMENT '邮箱',
    mobile VARCHAR(50) DEFAULT NULL COMMENT '手机号',
    department_id VARCHAR(128) DEFAULT NULL COMMENT '飞书部门ID',
    department_name VARCHAR(200) DEFAULT NULL COMMENT '飞书部门名称',
    bind_source VARCHAR(50) NOT NULL DEFAULT 'manual' COMMENT '绑定来源：manual-手动 oauth-OAuth sync-同步',
    bind_time DATETIME NOT NULL COMMENT '绑定时间',
    update_time DATETIME NOT NULL COMMENT '更新时间',
    UNIQUE KEY uk_sys_user_id (sys_user_id),
    UNIQUE KEY uk_open_id (open_id),
    KEY idx_mobile (mobile),
    KEY idx_email (email),
    KEY idx_department_id (department_id)
) COMMENT='系统用户与飞书用户绑定表';
```

绑定流程建议如下：

| 步骤 | 说明                                                 |
| ---- | ---------------------------------------------------- |
| 1    | 系统用户提供手机号或邮箱                             |
| 2    | 后端调用飞书 `batch_get_id` 接口查询用户 ID          |
| 3    | 如果返回为空，提示用户不存在或应用无权限             |
| 4    | 使用返回的 `open_id` 调用用户详情接口                |
| 5    | 将飞书用户信息与系统用户 ID 保存到绑定表             |
| 6    | 后续发送消息时，通过系统用户 ID 查询飞书 `open_id`   |
| 7    | 用户离职、部门变更、手机号变更时定期同步更新绑定关系 |

下面给出绑定实体类示例，持久层可按项目已有 MyBatis-Plus、JPA 或 JDBC 规范实现。

文件位置：`src/main/java/io/github/atengk/feishu/model/entity/SysUserFeishuBind.java`

```java
package io.github.atengk.feishu.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统用户与飞书用户绑定关系
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Data
public class SysUserFeishuBind {

    /**
     * 主键 ID
     */
    private Long id;

    /**
     * 系统用户 ID
     */
    private Long sysUserId;

    /**
     * 系统用户名
     */
    private String username;

    /**
     * 飞书 open_id
     */
    private String openId;

    /**
     * 飞书 user_id
     */
    private String userId;

    /**
     * 飞书 union_id
     */
    private String unionId;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 手机号
     */
    private String mobile;

    /**
     * 飞书部门 ID
     */
    private String departmentId;

    /**
     * 飞书部门名称
     */
    private String departmentName;

    /**
     * 绑定来源
     */
    private String bindSource;

    /**
     * 绑定时间
     */
    private LocalDateTime bindTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
```

下面给出绑定服务接口，用于表达业务边界。具体数据库保存逻辑可接入项目现有用户表和持久层框架。

文件位置：`src/main/java/io/github/atengk/feishu/service/FeishuAccountBindService.java`

```java
package io.github.atengk.feishu.service;

import io.github.atengk.feishu.model.entity.SysUserFeishuBind;

/**
 * 飞书账号绑定服务
 *
 * @author Ateng
 * @since 2026-05-07
 */
public interface FeishuAccountBindService {

    /**
     * 通过手机号绑定飞书账号
     *
     * @param sysUserId 系统用户 ID
     * @param username 系统用户名
     * @param mobile 手机号
     * @return 绑定关系
     */
    SysUserFeishuBind bindByMobile(Long sysUserId, String username, String mobile);

    /**
     * 通过邮箱绑定飞书账号
     *
     * @param sysUserId 系统用户 ID
     * @param username 系统用户名
     * @param email 邮箱
     * @return 绑定关系
     */
    SysUserFeishuBind bindByEmail(Long sysUserId, String username, String email);

    /**
     * 根据系统用户 ID 获取飞书 open_id
     *
     * @param sysUserId 系统用户 ID
     * @return 飞书 open_id
     */
    String getOpenIdBySysUserId(Long sysUserId);
}
```

用户绑定实现时需要重点处理以下问题：

| 问题                         | 处理建议                                                 |
| ---------------------------- | -------------------------------------------------------- |
| 手机号格式不一致             | 入库前统一格式，例如去除空格、处理国家区号               |
| 邮箱大小写                   | 邮箱建议统一转小写后匹配                                 |
| 一个飞书用户绑定多个系统账号 | 通常不允许，使用 `open_id` 唯一索引限制                  |
| 一个系统账号重复绑定         | 使用 `sys_user_id` 唯一索引限制                          |
| 用户离职                     | 定时同步飞书用户状态，停用本地绑定关系                   |
| 部门变更                     | 定时更新 `department_id` 和 `department_name`            |
| 权限不足                     | 查询不到用户时，需要区分“用户不存在”和“应用无通讯录权限” |
| 敏感信息                     | 手机号、邮箱属于敏感字段，日志中不要完整打印             |



## 事件订阅

本章节用于处理飞书开放平台向 Spring Boot 服务推送的事件回调，包括事件订阅配置、回调地址校验、事件消息解密和事件分发处理。飞书事件推送通常需要先在应用后台配置事件订阅回调地址，并根据业务需要订阅消息、卡片交互、审批实例状态变更等事件；如果配置了 `Encrypt Key`，飞书会推送加密事件，服务端需要先解密再处理业务。本文档继续基于前文大纲展开。 飞书事件订阅配置流程包括配置回调 URL、开通权限、添加事件；审批事件还需要订阅对应的审批定义 `Approval Code` 才能收到审批事件。([Apifox](https://s.apifox.cn/apidoc/docs-site/532425/doc-1998391?utm_source=chatgpt.com))

### 事件订阅配置

事件订阅配置用于让飞书开放平台能够把应用相关事件推送到后端服务。配置时需要准备一个公网可访问的 HTTPS 地址，并在飞书开放平台应用后台的事件订阅中填写该地址。

常见配置项如下：

| 配置项             | 说明                                                         |
| ------------------ | ------------------------------------------------------------ |
| 回调地址           | Spring Boot 提供的事件接收接口，例如 `/api/feishu/event/callback` |
| Verification Token | 用于校验事件来源是否为当前飞书应用                           |
| Encrypt Key        | 用于解密飞书推送的加密事件，建议生产环境开启                 |
| 订阅事件           | 根据业务选择消息事件、卡片事件、审批事件等                   |
| 应用权限           | 不同事件依赖不同权限，需要先在权限管理中开通                 |
| 审批定义订阅       | 审批事件除了后台添加事件外，还需要订阅对应审批定义           |

推荐在前文 `application.yml` 中补充或确认以下配置。

```yaml
feishu:
  # 飞书事件回调路径，需要与开放平台后台配置保持一致
  callback-path: /api/feishu/event/callback

  # 事件订阅 Verification Token，用于校验事件来源
  verification-token: ${FEISHU_VERIFICATION_TOKEN:replace-with-your-token}

  # 事件订阅 Encrypt Key，用于解密加密事件
  encrypt-key: ${FEISHU_ENCRYPT_KEY:replace-with-your-encrypt-key}
```

事件订阅建议按业务场景拆分处理：

| 事件类型         | 适用场景                               |
| ---------------- | -------------------------------------- |
| 消息事件         | 机器人接收用户消息、群消息             |
| 卡片交互事件     | 用户点击卡片按钮后触发后端业务         |
| 审批实例状态事件 | 审批通过、拒绝、撤销后同步本地业务状态 |
| 审批任务状态事件 | 审批节点流转时同步任务状态             |
| 用户或部门事件   | 用户入职、离职、部门变更后同步组织架构 |

### 回调地址校验

回调地址校验是飞书事件订阅的第一步。配置回调地址时，飞书会向该地址发送校验请求，后端需要从请求体中取出 `challenge` 并原样返回。业务事件推送时，则需要校验请求中的 `token` 是否与配置的 `verification-token` 一致。

下面先定义事件处理服务接口。

文件位置：`src/main/java/io/github/atengk/feishu/service/FeishuEventService.java`

```java
package io.github.atengk.feishu.service;

import java.util.Map;

/**
 * 飞书事件服务
 *
 * @author Ateng
 * @since 2026-05-07
 */
public interface FeishuEventService {

    /**
     * 处理飞书事件回调
     *
     * @param requestBody 原始请求体
     * @return 回调响应
     */
    Map<String, Object> handleEvent(String requestBody);
}
```

下面控制器用于接收飞书事件回调。该接口路径使用配置项 `${feishu.callback-path}`，需要与飞书开放平台后台填写的回调地址路径保持一致。

文件位置：`src/main/java/io/github/atengk/feishu/controller/FeishuEventController.java`

```java
package io.github.atengk.feishu.controller;

import io.github.atengk.feishu.service.FeishuEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 飞书事件回调接口
 *
 * @author Ateng
 * @since 2026-05-07
 */
@RestController
@RequiredArgsConstructor
public class FeishuEventController {

    private final FeishuEventService feishuEventService;

    /**
     * 接收飞书事件回调
     *
     * @param requestBody 原始请求体
     * @return 回调响应
     */
    @PostMapping("${feishu.callback-path:/api/feishu/event/callback}")
    public Map<String, Object> callback(@RequestBody String requestBody) {
        return feishuEventService.handleEvent(requestBody);
    }
}
```

### 事件消息解密

如果在飞书开放平台配置了 `Encrypt Key`，飞书会推送形如 `{"encrypt":"..."}` 的加密事件。服务端需要使用配置的 `Encrypt Key` 进行解密，然后再读取解密后的事件内容。飞书文档说明配置 `Encrypt Key` 后，开放平台会向请求地址推送加密后的事件，服务端接收到加密事件后需要先解密处理。([飞书 API](https://feishu.apifox.cn/doc-1940224?utm_source=chatgpt.com))

下面工具类用于解密飞书事件。这里使用 `SHA-256(encrypt_key)` 作为 AES Key，取前 16 字节作为 IV，使用 `AES/CBC/PKCS5Padding` 解密密文。

文件位置：`src/main/java/io/github/atengk/feishu/util/FeishuEventCryptoUtil.java`

```java
package io.github.atengk.feishu.util;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.feishu.exception.FeishuApiException;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;

/**
 * 飞书事件加解密工具类
 *
 * @author Ateng
 * @since 2026-05-07
 */
public class FeishuEventCryptoUtil {

    private FeishuEventCryptoUtil() {
    }

    /**
     * 解密飞书加密事件
     *
     * @param encryptKey 事件订阅 Encrypt Key
     * @param encrypt 加密内容
     * @return 解密后的 JSON 字符串
     */
    public static String decrypt(String encryptKey, String encrypt) {
        if (StrUtil.hasBlank(encryptKey, encrypt)) {
            throw new FeishuApiException("飞书事件解密参数不能为空");
        }

        try {
            byte[] key = MessageDigest.getInstance("SHA-256")
                    .digest(encryptKey.getBytes(StandardCharsets.UTF_8));
            byte[] iv = Arrays.copyOfRange(key, 0, 16);
            byte[] encryptedBytes = Base64.getDecoder().decode(encrypt);

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, new IvParameterSpec(iv));

            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new FeishuApiException("飞书事件解密失败：" + e.getMessage(), e);
        }
    }
}
```

### 事件处理流程

事件处理流程建议分为五步：解析请求、解密事件、校验 Token、处理 URL 校验、按事件类型分发业务逻辑。飞书接收事件时通常需要进行安全校验和解密，飞书事件日志也可以在开发者后台排查推送问题。([飞书 API](https://feishu.apifox.cn/doc-1940243?utm_source=chatgpt.com))

下面实现类给出完整事件处理流程，并使用 Redis 做事件幂等，避免飞书重试导致业务重复执行。

文件位置：`src/main/java/io/github/atengk/feishu/service/impl/FeishuEventServiceImpl.java`

```java
package io.github.atengk.feishu.service.impl;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import io.github.atengk.feishu.config.FeishuProperties;
import io.github.atengk.feishu.exception.FeishuApiException;
import io.github.atengk.feishu.service.FeishuEventService;
import io.github.atengk.feishu.util.FeishuEventCryptoUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 飞书事件服务实现
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeishuEventServiceImpl implements FeishuEventService {

    private static final String EVENT_IDEMPOTENT_KEY_PREFIX = "feishu:event:idempotent:";

    private final FeishuProperties feishuProperties;

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 处理飞书事件回调
     *
     * @param requestBody 原始请求体
     * @return 回调响应
     */
    @Override
    public Map<String, Object> handleEvent(String requestBody) {
        if (!JSONUtil.isTypeJSON(requestBody)) {
            throw new FeishuApiException("飞书事件请求体不是合法 JSON");
        }

        JSONObject eventBody = JSONUtil.parseObj(requestBody);
        eventBody = decryptIfNecessary(eventBody);

        verifyToken(eventBody);

        if (isUrlVerification(eventBody)) {
            String challenge = eventBody.getStr("challenge");
            log.info("飞书事件回调地址校验成功");
            return MapUtil.<String, Object>builder()
                    .put("challenge", challenge)
                    .build();
        }

        if (!markEventIfAbsent(eventBody)) {
            log.info("飞书事件重复推送，已忽略，eventId={}", getEventId(eventBody));
            return success();
        }

        dispatchEvent(eventBody);
        return success();
    }

    /**
     * 解密加密事件
     *
     * @param eventBody 原始事件体
     * @return 明文事件体
     */
    private JSONObject decryptIfNecessary(JSONObject eventBody) {
        String encrypt = eventBody.getStr("encrypt");
        if (StrUtil.isBlank(encrypt)) {
            return eventBody;
        }

        String plainText = FeishuEventCryptoUtil.decrypt(feishuProperties.getEncryptKey(), encrypt);
        if (!JSONUtil.isTypeJSON(plainText)) {
            throw new FeishuApiException("飞书事件解密结果不是合法 JSON");
        }

        log.info("飞书加密事件解密成功");
        return JSONUtil.parseObj(plainText);
    }

    /**
     * 校验事件 Token
     *
     * @param eventBody 事件体
     */
    private void verifyToken(JSONObject eventBody) {
        String expectedToken = feishuProperties.getVerificationToken();
        if (StrUtil.isBlank(expectedToken)) {
            throw new FeishuApiException("飞书 verification-token 未配置");
        }

        String token = eventBody.getStr("token");
        JSONObject header = eventBody.getJSONObject("header");
        if (StrUtil.isBlank(token) && header != null) {
            token = header.getStr("token");
        }

        if (!StrUtil.equals(expectedToken, token)) {
            throw new FeishuApiException("飞书事件 Token 校验失败");
        }
    }

    /**
     * 判断是否为回调地址校验事件
     *
     * @param eventBody 事件体
     * @return 是否为地址校验事件
     */
    private boolean isUrlVerification(JSONObject eventBody) {
        String type = eventBody.getStr("type");
        return StrUtil.equals(type, "url_verification") && StrUtil.isNotBlank(eventBody.getStr("challenge"));
    }

    /**
     * 标记事件幂等
     *
     * @param eventBody 事件体
     * @return true 表示首次处理，false 表示重复事件
     */
    private boolean markEventIfAbsent(JSONObject eventBody) {
        String eventId = getEventId(eventBody);
        if (StrUtil.isBlank(eventId)) {
            return true;
        }

        String key = EVENT_IDEMPOTENT_KEY_PREFIX + eventId;
        Boolean success = stringRedisTemplate.opsForValue()
                .setIfAbsent(key, "1", 24, TimeUnit.HOURS);
        return Boolean.TRUE.equals(success);
    }

    /**
     * 分发事件
     *
     * @param eventBody 事件体
     */
    private void dispatchEvent(JSONObject eventBody) {
        String eventType = getEventType(eventBody);
        JSONObject event = eventBody.getJSONObject("event");

        log.info("开始处理飞书事件，eventType={}，eventId={}", eventType, getEventId(eventBody));

        if (StrUtil.startWith(eventType, "approval.")) {
            handleApprovalEvent(eventType, event);
            return;
        }

        if (StrUtil.startWith(eventType, "im.")) {
            handleMessageEvent(eventType, event);
            return;
        }

        log.info("未匹配到飞书事件处理器，eventType={}", eventType);
    }

    /**
     * 处理审批事件
     *
     * @param eventType 事件类型
     * @param event 事件内容
     */
    private void handleApprovalEvent(String eventType, JSONObject event) {
        log.info("接收到飞书审批事件，eventType={}，event={}", eventType, event);
        // 这里根据业务更新本地审批单状态，例如通过 instance_code 查询本地业务单据并更新状态
    }

    /**
     * 处理消息事件
     *
     * @param eventType 事件类型
     * @param event 事件内容
     */
    private void handleMessageEvent(String eventType, JSONObject event) {
        log.info("接收到飞书消息事件，eventType={}，event={}", eventType, event);
        // 这里根据机器人消息内容触发业务逻辑，例如关键字查询、工单创建、任务处理
    }

    /**
     * 获取事件 ID
     *
     * @param eventBody 事件体
     * @return 事件 ID
     */
    private String getEventId(JSONObject eventBody) {
        JSONObject header = eventBody.getJSONObject("header");
        return header == null ? eventBody.getStr("uuid") : header.getStr("event_id");
    }

    /**
     * 获取事件类型
     *
     * @param eventBody 事件体
     * @return 事件类型
     */
    private String getEventType(JSONObject eventBody) {
        JSONObject header = eventBody.getJSONObject("header");
        return header == null ? eventBody.getStr("event_type") : header.getStr("event_type");
    }

    /**
     * 构建成功响应
     *
     * @return 成功响应
     */
    private Map<String, Object> success() {
        return MapUtil.<String, Object>builder()
                .put("code", 0)
                .put("msg", "success")
                .build();
    }
}
```

事件处理时建议遵循以下规则：

| 处理点         | 建议                                        |
| -------------- | ------------------------------------------- |
| 先解密再处理   | 如果请求体包含 `encrypt`，必须先解密        |
| 必须校验 Token | 防止非飞书来源调用回调接口                  |
| 回调快速返回   | 耗时业务建议投递 MQ 或异步任务处理          |
| 做幂等控制     | 使用 `event_id` 或业务单号防止重复处理      |
| 记录关键日志   | 记录事件类型、事件 ID、业务单号、处理结果   |
| 不打印敏感数据 | 避免在日志中输出完整用户手机号、邮箱、Token |
| 失败可追踪     | 本地记录失败原因，结合飞书事件日志排查      |

## 审批与业务集成

本章节用于封装飞书审批能力，包括创建审批实例、查询审批状态、处理审批回调。飞书审批开放接口可以用于企业原有业务平台与飞书审批打通，审批实例创建接口为 `POST /open-apis/approval/v4/instances`，审批实例查询可通过实例 Code 获取详情。([飞书 API](https://feishu.apifox.cn/doc-1974107?utm_source=chatgpt.com))

### 审批实例创建

审批实例创建用于业务系统主动发起飞书审批，例如请假、报销、采购、合同、工单流转等场景。创建审批实例前，需要先在飞书审批后台创建审批定义，并获取对应的 `approval_code`。创建实例时，需要按照审批定义中的表单控件结构传入 `form` 数据。飞书创建审批实例接口要求调用方了解审批定义表单结构，并按表单控件传入对应值。([apifox](https://apifox.com/apidoc/docs-site/532425/api-9020735?utm_source=chatgpt.com))

先定义审批服务接口。

文件位置：`src/main/java/io/github/atengk/feishu/service/FeishuApprovalService.java`

```java
package io.github.atengk.feishu.service;

import cn.hutool.json.JSONObject;

import java.util.List;
import java.util.Map;

/**
 * 飞书审批服务
 *
 * @author Ateng
 * @since 2026-05-07
 */
public interface FeishuApprovalService {

    /**
     * 创建审批实例
     *
     * @param approvalCode 审批定义 Code
     * @param openId 发起人 open_id
     * @param formItems 表单控件值
     * @return 审批实例 Code
     */
    String createInstance(String approvalCode, String openId, List<Map<String, Object>> formItems);

    /**
     * 查询审批实例详情
     *
     * @param instanceCode 审批实例 Code
     * @param userId 用户 ID
     * @param userIdType 用户 ID 类型
     * @return 审批实例详情
     */
    JSONObject getInstance(String instanceCode, String userId, String userIdType);

    /**
     * 查询审批实例列表
     *
     * @param approvalCode 审批定义 Code
     * @param startTime 开始时间，毫秒时间戳
     * @param endTime 结束时间，毫秒时间戳
     * @param pageSize 分页大小
     * @param pageToken 分页 Token
     * @return 查询结果
     */
    JSONObject queryInstanceCodes(String approvalCode, Long startTime, Long endTime, Integer pageSize, String pageToken);
}
```

下面实现类封装审批实例创建、实例详情查询和实例列表查询。这里复用前文的 `FeishuApiClient`、`FeishuTokenService` 和 `FeishuResponseUtil`。

文件位置：`src/main/java/io/github/atengk/feishu/service/impl/FeishuApprovalServiceImpl.java`

```java
package io.github.atengk.feishu.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import io.github.atengk.feishu.client.FeishuApiClient;
import io.github.atengk.feishu.exception.FeishuApiException;
import io.github.atengk.feishu.service.FeishuApprovalService;
import io.github.atengk.feishu.service.FeishuTokenService;
import io.github.atengk.feishu.util.FeishuResponseUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * 飞书审批服务实现
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeishuApprovalServiceImpl implements FeishuApprovalService {

    private static final String CREATE_INSTANCE_PATH = "/open-apis/approval/v4/instances";

    private static final String GET_INSTANCE_PATH = "/open-apis/approval/v4/instances/{}?locale=zh-CN&user_id={}&user_id_type={}";

    private static final String QUERY_INSTANCE_CODES_PATH = "/open-apis/approval/v4/instances?approval_code={}&start_time={}&end_time={}&page_size={}{}";

    private final FeishuApiClient feishuApiClient;

    private final FeishuTokenService feishuTokenService;

    /**
     * 创建审批实例
     *
     * @param approvalCode 审批定义 Code
     * @param openId 发起人 open_id
     * @param formItems 表单控件值
     * @return 审批实例 Code
     */
    @Override
    public String createInstance(String approvalCode, String openId, List<Map<String, Object>> formItems) {
        if (StrUtil.hasBlank(approvalCode, openId) || CollUtil.isEmpty(formItems)) {
            throw new FeishuApiException("审批定义 Code、发起人 open_id 和表单内容不能为空");
        }

        Map<String, Object> requestBody = MapUtil.<String, Object>builder()
                .put("approval_code", approvalCode)
                .put("open_id", openId)
                .put("form", JSONUtil.toJsonStr(formItems))
                .build();

        String responseBody = feishuApiClient.post(
                CREATE_INSTANCE_PATH,
                requestBody,
                feishuTokenService.getTenantAccessToken()
        );

        JSONObject data = FeishuResponseUtil.parseSuccessData(responseBody);
        String instanceCode = data.getStr("instance_code");
        if (StrUtil.isBlank(instanceCode)) {
            throw new FeishuApiException("飞书审批实例创建成功但未返回 instance_code");
        }

        log.info("飞书审批实例创建成功，approvalCode={}，openId={}，instanceCode={}", approvalCode, openId, instanceCode);
        return instanceCode;
    }

    /**
     * 查询审批实例详情
     *
     * @param instanceCode 审批实例 Code
     * @param userId 用户 ID
     * @param userIdType 用户 ID 类型
     * @return 审批实例详情
     */
    @Override
    public JSONObject getInstance(String instanceCode, String userId, String userIdType) {
        if (StrUtil.hasBlank(instanceCode, userId)) {
            throw new FeishuApiException("审批实例 Code 和用户 ID 不能为空");
        }

        String finalUserIdType = StrUtil.blankToDefault(userIdType, "open_id");
        String path = StrUtil.format(
                GET_INSTANCE_PATH,
                URLUtil.encode(instanceCode, StandardCharsets.UTF_8),
                URLUtil.encode(userId, StandardCharsets.UTF_8),
                finalUserIdType
        );

        String responseBody = feishuApiClient.post(path, MapUtil.empty(), feishuTokenService.getTenantAccessToken());
        JSONObject data = FeishuResponseUtil.parseSuccessData(responseBody);

        log.info("飞书审批实例详情查询成功，instanceCode={}，status={}", instanceCode, data.getStr("status"));
        return data;
    }

    /**
     * 查询审批实例列表
     *
     * @param approvalCode 审批定义 Code
     * @param startTime 开始时间，毫秒时间戳
     * @param endTime 结束时间，毫秒时间戳
     * @param pageSize 分页大小
     * @param pageToken 分页 Token
     * @return 查询结果
     */
    @Override
    public JSONObject queryInstanceCodes(String approvalCode, Long startTime, Long endTime, Integer pageSize, String pageToken) {
        if (StrUtil.isBlank(approvalCode) || startTime == null || endTime == null) {
            throw new FeishuApiException("审批定义 Code、开始时间和结束时间不能为空");
        }

        int finalPageSize = pageSize == null ? 100 : pageSize;
        String pageTokenPart = StrUtil.isBlank(pageToken) ? "" : "&page_token=" + URLUtil.encode(pageToken, StandardCharsets.UTF_8);

        String path = StrUtil.format(
                QUERY_INSTANCE_CODES_PATH,
                URLUtil.encode(approvalCode, StandardCharsets.UTF_8),
                startTime,
                endTime,
                finalPageSize,
                pageTokenPart
        );

        String responseBody = feishuApiClient.post(path, MapUtil.empty(), feishuTokenService.getTenantAccessToken());
        JSONObject data = FeishuResponseUtil.parseSuccessData(responseBody);

        log.info("飞书审批实例列表查询成功，approvalCode={}，hasMore={}", approvalCode, data.getBool("has_more"));
        return data;
    }
}
```

审批表单控件值需要与飞书审批定义保持一致。以下是常见表单控件值示例。

```json
[
  {
    "id": "widget1",
    "type": "input",
    "value": "采购申请"
  },
  {
    "id": "widget2",
    "type": "textarea",
    "value": "申请采购一批研发测试设备"
  },
  {
    "id": "widget3",
    "type": "number",
    "value": 12000
  },
  {
    "id": "widget4",
    "type": "date",
    "value": "2026-05-07T09:00:00+08:00"
  }
]
```

### 审批状态查询

审批状态查询用于业务系统主动拉取飞书审批结果。常见场景是：本地业务单据发起审批后保存 `instance_code`，后续通过定时任务或人工刷新接口查询该审批实例的最新状态。飞书获取单个审批实例详情接口通过审批实例 `Instance Code` 获取详情，返回内容包含 `approval_code`、`approval_name`、`status`、`form`、`task_list`、`comment_list`、`timeline` 等字段。([飞书 API](https://feishu.apifox.cn/api-9020734?utm_source=chatgpt.com))

建议在本地业务表中至少保存以下字段：

| 字段                  | 说明                               |
| --------------------- | ---------------------------------- |
| `business_id`         | 本地业务单据 ID                    |
| `approval_code`       | 飞书审批定义 Code                  |
| `instance_code`       | 飞书审批实例 Code                  |
| `approval_status`     | 本地记录的审批状态                 |
| `approval_start_time` | 审批发起时间                       |
| `approval_end_time`   | 审批结束时间                       |
| `last_sync_time`      | 最近一次同步时间                   |
| `raw_response`        | 最近一次飞书审批详情响应，按需保存 |

下面控制器提供审批实例创建、查询和列表查询接口，用于联调和业务集成。生产环境应增加登录鉴权、权限控制和参数校验。

文件位置：`src/main/java/io/github/atengk/feishu/controller/FeishuApprovalController.java`

```java
package io.github.atengk.feishu.controller;

import cn.hutool.json.JSONObject;
import io.github.atengk.feishu.service.FeishuApprovalService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 飞书审批接口
 *
 * @author Ateng
 * @since 2026-05-07
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/feishu/approval")
public class FeishuApprovalController {

    private final FeishuApprovalService feishuApprovalService;

    /**
     * 创建审批实例
     *
     * @param approvalCode 审批定义 Code
     * @param openId 发起人 open_id
     * @param formItems 表单控件值
     * @return 审批实例 Code
     */
    @PostMapping("/instance")
    public String createInstance(@RequestParam String approvalCode,
                                 @RequestParam String openId,
                                 @RequestBody List<Map<String, Object>> formItems) {
        return feishuApprovalService.createInstance(approvalCode, openId, formItems);
    }

    /**
     * 查询审批实例详情
     *
     * @param instanceCode 审批实例 Code
     * @param userId 用户 ID
     * @param userIdType 用户 ID 类型
     * @return 审批实例详情
     */
    @GetMapping("/instance/{instanceCode}")
    public JSONObject getInstance(@PathVariable String instanceCode,
                                  @RequestParam String userId,
                                  @RequestParam(defaultValue = "open_id") String userIdType) {
        return feishuApprovalService.getInstance(instanceCode, userId, userIdType);
    }

    /**
     * 查询审批实例 Code 列表
     *
     * @param approvalCode 审批定义 Code
     * @param startTime 开始时间，毫秒时间戳
     * @param endTime 结束时间，毫秒时间戳
     * @param pageSize 分页大小
     * @param pageToken 分页 Token
     * @return 查询结果
     */
    @GetMapping("/instances")
    public JSONObject queryInstanceCodes(@RequestParam String approvalCode,
                                         @RequestParam Long startTime,
                                         @RequestParam Long endTime,
                                         @RequestParam(defaultValue = "100") Integer pageSize,
                                         @RequestParam(required = false) String pageToken) {
        return feishuApprovalService.queryInstanceCodes(approvalCode, startTime, endTime, pageSize, pageToken);
    }
}
```

接口验证命令如下。

```bash
# 创建审批实例
curl -X POST "http://localhost:8080/api/feishu/approval/instance?approvalCode=7C468A54-8745-2245-9675-08B7C63E7A85&openId=ou_xxxxxxxxxxxxx" \
  -H "Content-Type: application/json" \
  -d '[
    {
      "id": "widget1",
      "type": "input",
      "value": "采购申请"
    },
    {
      "id": "widget2",
      "type": "textarea",
      "value": "申请采购研发测试设备"
    }
  ]'

# 查询审批实例详情
curl "http://localhost:8080/api/feishu/approval/instance/357C21A0-2069-4F6B-955F-1DFBE6710C51?userId=ou_xxxxxxxxxxxxx&userIdType=open_id"

# 查询某个审批定义下的审批实例 Code 列表
curl "http://localhost:8080/api/feishu/approval/instances?approvalCode=7C468A54-8745-2245-9675-08B7C63E7A85&startTime=1714492800000&endTime=1717084800000&pageSize=100"
```

### 审批回调处理

审批回调处理用于在飞书审批状态变化时主动同步本地业务状态。与定时查询相比，回调方式实时性更好，也可以减少主动轮询次数。飞书审批事件配置时，需要在应用后台开通审批权限、添加审批事件，并订阅需要监听的审批定义 `Approval Code`；否则即使事件订阅已配置，也可能收不到对应审批事件。([Apifox](https://s.apifox.cn/apidoc/docs-site/532425/doc-1998391?utm_source=chatgpt.com))

审批回调建议处理流程如下：

| 步骤 | 说明                                                         |
| ---- | ------------------------------------------------------------ |
| 1    | 飞书推送审批事件到 `/api/feishu/event/callback`              |
| 2    | 后端解密事件并校验 Token                                     |
| 3    | 根据 `event_type` 判断是否为审批事件                         |
| 4    | 从事件体中提取 `approval_code`、`instance_code`、`status` 等信息 |
| 5    | 根据 `instance_code` 查询本地业务单据                        |
| 6    | 必要时调用审批详情接口补全状态和表单信息                     |
| 7    | 更新本地业务状态，例如待审批、已通过、已拒绝、已撤销         |
| 8    | 记录回调日志并返回成功响应                                   |

建议新增审批回调服务接口，把事件服务中的审批事件处理逻辑独立出来，避免 `FeishuEventServiceImpl` 过于臃肿。

文件位置：`src/main/java/io/github/atengk/feishu/service/FeishuApprovalCallbackService.java`

```java
package io.github.atengk.feishu.service;

import cn.hutool.json.JSONObject;

/**
 * 飞书审批回调服务
 *
 * @author Ateng
 * @since 2026-05-07
 */
public interface FeishuApprovalCallbackService {

    /**
     * 处理审批事件
     *
     * @param eventType 事件类型
     * @param event 事件内容
     */
    void handleApprovalEvent(String eventType, JSONObject event);
}
```

下面实现类演示如何提取审批事件中的关键字段，并根据 `instance_code` 同步本地业务状态。实际项目中需要替换为自己的 Mapper、Repository 或业务 Service。

文件位置：`src/main/java/io/github/atengk/feishu/service/impl/FeishuApprovalCallbackServiceImpl.java`

```java
package io.github.atengk.feishu.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import io.github.atengk.feishu.exception.FeishuApiException;
import io.github.atengk.feishu.service.FeishuApprovalCallbackService;
import io.github.atengk.feishu.service.FeishuApprovalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 飞书审批回调服务实现
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeishuApprovalCallbackServiceImpl implements FeishuApprovalCallbackService {

    private final FeishuApprovalService feishuApprovalService;

    /**
     * 处理审批事件
     *
     * @param eventType 事件类型
     * @param event 事件内容
     */
    @Override
    public void handleApprovalEvent(String eventType, JSONObject event) {
        if (event == null) {
            throw new FeishuApiException("飞书审批事件内容不能为空");
        }

        JSONObject object = event.getJSONObject("object");
        JSONObject approvalEvent = object == null ? event : object;

        String approvalCode = approvalEvent.getStr("approval_code");
        String instanceCode = approvalEvent.getStr("instance_code");
        String status = approvalEvent.getStr("status");
        String userId = approvalEvent.getStr("open_id");

        log.info("开始处理飞书审批回调，eventType={}，approvalCode={}，instanceCode={}，status={}",
                eventType, approvalCode, instanceCode, status);

        if (StrUtil.isBlank(instanceCode)) {
            log.info("飞书审批回调未包含 instance_code，跳过业务状态同步，event={}", event);
            return;
        }

        if (StrUtil.isNotBlank(userId)) {
            JSONObject instanceDetail = feishuApprovalService.getInstance(instanceCode, userId, "open_id");
            status = StrUtil.blankToDefault(instanceDetail.getStr("status"), status);
        }

        syncBusinessStatus(instanceCode, status);
    }

    /**
     * 同步本地业务状态
     *
     * @param instanceCode 审批实例 Code
     * @param status 审批状态
     */
    private void syncBusinessStatus(String instanceCode, String status) {
        String localStatus = convertApprovalStatus(status);

        log.info("同步本地业务审批状态，instanceCode={}，feishuStatus={}，localStatus={}",
                instanceCode, status, localStatus);

        // 示例：
        // 1. 根据 instanceCode 查询本地业务单据
        // 2. 更新业务单据审批状态
        // 3. 记录审批回调日志
        // 4. 必要时触发后续业务流程，例如发货、付款、归档、通知
    }

    /**
     * 转换飞书审批状态为本地业务状态
     *
     * @param status 飞书审批状态
     * @return 本地业务状态
     */
    private String convertApprovalStatus(String status) {
        if (StrUtil.equalsAnyIgnoreCase(status, "APPROVED", "PASS")) {
            return "APPROVED";
        }
        if (StrUtil.equalsAnyIgnoreCase(status, "REJECTED", "REJECT")) {
            return "REJECTED";
        }
        if (StrUtil.equalsAnyIgnoreCase(status, "CANCELED", "CANCELLED", "CANCEL")) {
            return "CANCELED";
        }
        if (StrUtil.equalsAnyIgnoreCase(status, "PENDING", "RUNNING")) {
            return "PENDING";
        }
        return "UNKNOWN";
    }
}
```

然后修改前文 `FeishuEventServiceImpl`，将原来的 `handleApprovalEvent` 方法替换为调用审批回调服务。

在 `FeishuEventServiceImpl` 中增加依赖：

```java
private final FeishuApprovalCallbackService feishuApprovalCallbackService;
```

将审批事件处理方法替换为以下内容：

```java
/**
 * 处理审批事件
 *
 * @param eventType 事件类型
 * @param event 事件内容
 */
private void handleApprovalEvent(String eventType, JSONObject event) {
    feishuApprovalCallbackService.handleApprovalEvent(eventType, event);
}
```

审批回调落库建议增加一张日志表，用于排查重复回调、异常回调和状态不一致问题。

```sql
CREATE TABLE feishu_approval_callback_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    event_id VARCHAR(128) DEFAULT NULL COMMENT '飞书事件ID',
    event_type VARCHAR(128) NOT NULL COMMENT '事件类型',
    approval_code VARCHAR(128) DEFAULT NULL COMMENT '审批定义Code',
    instance_code VARCHAR(128) DEFAULT NULL COMMENT '审批实例Code',
    approval_status VARCHAR(64) DEFAULT NULL COMMENT '审批状态',
    raw_event JSON DEFAULT NULL COMMENT '原始事件内容',
    handle_status VARCHAR(32) NOT NULL COMMENT '处理状态：SUCCESS/FAILED',
    error_message TEXT DEFAULT NULL COMMENT '错误信息',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间',
    UNIQUE KEY uk_event_id (event_id),
    KEY idx_instance_code (instance_code),
    KEY idx_approval_code (approval_code)
) COMMENT='飞书审批回调日志表';
```

审批与业务集成需要注意以下事项：

| 注意事项             | 说明                                                         |
| -------------------- | ------------------------------------------------------------ |
| 保存 `instance_code` | 创建审批实例成功后必须保存到本地业务表                       |
| 审批表单要匹配       | `form` 中的控件 `id`、`type`、`value` 必须与审批定义匹配     |
| 回调要做幂等         | 同一事件可能因网络或响应异常被重复推送                       |
| 状态以详情为准       | 回调事件字段不足时，建议再查询一次审批实例详情               |
| 不要阻塞回调         | 回调接口应快速返回，复杂业务建议异步处理                     |
| 记录回调日志         | 方便排查飞书事件、业务单据和状态同步问题                     |
| 权限要完整           | 审批实例创建、查询、回调事件均需要对应审批权限               |
| 订阅审批定义         | 审批事件不只是在后台添加事件，还需要订阅具体 `Approval Code` |

## 接口设计

本章节用于统一设计 Spring Boot 服务对外暴露的飞书集成接口，包括消息发送接口、用户查询接口、事件回调接口和审批业务接口。接口设计建议面向业务系统，而不是完全透传飞书开放平台参数，这样可以降低业务模块对飞书 API 细节的依赖。本文继续基于你上传的大纲展开。

飞书发送消息开放接口为 `POST /open-apis/im/v1/messages`，支持文本、富文本、可交互卡片等消息类型，调用时需要使用 Bearer Token，并通过 `receive_id_type` 指定接收者 ID 类型。([Apifox](https://s.apifox.cn/apidoc/docs-site/532425/api-58348294?utm_source=chatgpt.com)) 用户查询常用接口包括获取单个用户信息 `GET /contact/v3/users/{user_id}`，以及通过手机号或邮箱批量获取用户 ID `POST /contact/v3/users/batch_get_id`。([Apifox](https://s.apifox.cn/apidoc/docs-site/532425/api-9021015?utm_source=chatgpt.com)) 审批创建接口为 `POST /approval/v4/instances`，审批实例列表接口为 `POST /approval/v4/instances`，飞书要求创建审批时调用方按审批定义的表单结构传入表单值。([Apifox](https://s.apifox.cn/apidoc/docs-site/532425/api-9020735?utm_source=chatgpt.com))

### 消息发送接口

消息发送接口用于向飞书用户、群聊或机器人 Webhook 推送业务消息。建议业务系统对外只暴露文本、富文本、卡片三类高频接口，底层再由 `FeishuMessageService` 统一转换为飞书需要的 `msg_type` 和 `content` 格式。

接口设计如下：

| 接口名称         | 请求方式 | 接口路径                            | 说明                           |
| ---------------- | -------- | ----------------------------------- | ------------------------------ |
| 发送文本消息     | POST     | `/api/feishu/messages/text`         | 向用户或群聊发送普通文本       |
| 发送富文本消息   | POST     | `/api/feishu/messages/rich-text`    | 发送带标题、段落、链接的富文本 |
| 发送卡片消息     | POST     | `/api/feishu/messages/card`         | 发送可交互消息卡片             |
| 群机器人文本推送 | POST     | `/api/feishu/messages/webhook/text` | 通过群机器人 Webhook 推送文本  |
| 群机器人卡片推送 | POST     | `/api/feishu/messages/webhook/card` | 通过群机器人 Webhook 推送卡片  |

建议先定义统一响应对象，后续所有内部接口都返回该结构，避免直接返回字符串或飞书原始响应。

文件位置：`src/main/java/io/github/atengk/common/web/ApiResult.java`

```java
package io.github.atengk.common.web;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 统一接口响应结果
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Data
public class ApiResult<T> {

    /**
     * 业务状态码
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
     * 响应时间
     */
    private LocalDateTime timestamp;

    /**
     * 构建成功响应
     *
     * @param data 响应数据
     * @return 统一响应
     * @param <T> 数据类型
     */
    public static <T> ApiResult<T> success(T data) {
        ApiResult<T> result = new ApiResult<>();
        result.setCode(0);
        result.setMessage("success");
        result.setData(data);
        result.setTimestamp(LocalDateTime.now());
        return result;
    }

    /**
     * 构建失败响应
     *
     * @param code 错误码
     * @param message 错误消息
     * @return 统一响应
     * @param <T> 数据类型
     */
    public static <T> ApiResult<T> fail(Integer code, String message) {
        ApiResult<T> result = new ApiResult<>();
        result.setCode(code);
        result.setMessage(message);
        result.setTimestamp(LocalDateTime.now());
        return result;
    }
}
```

下面定义文本消息请求对象，用于接收业务系统发送文本消息的参数。

文件位置：`src/main/java/io/github/atengk/feishu/model/request/FeishuTextMessageRequest.java`

```java
package io.github.atengk.feishu.model.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 飞书文本消息请求
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Data
public class FeishuTextMessageRequest {

    /**
     * 接收者 ID，例如 open_id、user_id、email、chat_id
     */
    @NotBlank(message = "接收者 ID 不能为空")
    private String receiveId;

    /**
     * 接收者 ID 类型，常用值：open_id、user_id、union_id、email、chat_id
     */
    @NotBlank(message = "接收者 ID 类型不能为空")
    private String receiveIdType = "open_id";

    /**
     * 文本内容
     */
    @NotBlank(message = "文本内容不能为空")
    private String text;
}
```

下面定义卡片消息请求对象，用于业务系统发送简单卡片。

文件位置：`src/main/java/io/github/atengk/feishu/model/request/FeishuCardMessageRequest.java`

```java
package io.github.atengk.feishu.model.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 飞书卡片消息请求
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Data
public class FeishuCardMessageRequest {

    /**
     * 接收者 ID
     */
    @NotBlank(message = "接收者 ID 不能为空")
    private String receiveId;

    /**
     * 接收者 ID 类型
     */
    @NotBlank(message = "接收者 ID 类型不能为空")
    private String receiveIdType = "open_id";

    /**
     * 卡片标题
     */
    @NotBlank(message = "卡片标题不能为空")
    private String title;

    /**
     * 卡片内容
     */
    @NotBlank(message = "卡片内容不能为空")
    private String content;

    /**
     * 按钮文案
     */
    private String buttonText = "查看详情";

    /**
     * 按钮跳转地址
     */
    private String buttonUrl;
}
```

下面是标准化后的消息接口控制器，复用前文的 `FeishuMessageService`。

文件位置：`src/main/java/io/github/atengk/feishu/controller/FeishuMessageApiController.java`

```java
package io.github.atengk.feishu.controller;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.common.web.ApiResult;
import io.github.atengk.feishu.model.request.FeishuCardMessageRequest;
import io.github.atengk.feishu.model.request.FeishuTextMessageRequest;
import io.github.atengk.feishu.service.FeishuMessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 飞书消息业务接口
 *
 * @author Ateng
 * @since 2026-05-07
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/feishu/messages")
public class FeishuMessageApiController {

    private final FeishuMessageService feishuMessageService;

    /**
     * 发送文本消息
     *
     * @param request 请求参数
     * @return 消息 ID
     */
    @PostMapping("/text")
    public ApiResult<String> sendText(@Valid @RequestBody FeishuTextMessageRequest request) {
        String messageId = feishuMessageService.sendTextMessage(
                request.getReceiveId(),
                request.getReceiveIdType(),
                request.getText()
        );
        return ApiResult.success(messageId);
    }

    /**
     * 发送富文本消息
     *
     * @param request 请求参数
     * @return 消息 ID
     */
    @PostMapping("/rich-text")
    public ApiResult<String> sendRichText(@Valid @RequestBody FeishuTextMessageRequest request) {
        String messageId = feishuMessageService.sendRichTextMessage(
                request.getReceiveId(),
                request.getReceiveIdType(),
                "系统通知",
                request.getText()
        );
        return ApiResult.success(messageId);
    }

    /**
     * 发送卡片消息
     *
     * @param request 请求参数
     * @return 消息 ID
     */
    @PostMapping("/card")
    public ApiResult<String> sendCard(@Valid @RequestBody FeishuCardMessageRequest request) {
        String messageId = feishuMessageService.sendCardMessage(
                request.getReceiveId(),
                request.getReceiveIdType(),
                request.getTitle(),
                request.getContent(),
                StrUtil.blankToDefault(request.getButtonText(), "查看详情"),
                request.getButtonUrl()
        );
        return ApiResult.success(messageId);
    }

    /**
     * 群机器人发送文本消息
     *
     * @param webhookUrl 群机器人 Webhook 地址
     * @param secret 签名密钥
     * @param request 请求参数
     * @return 飞书响应内容
     */
    @PostMapping("/webhook/text")
    public ApiResult<String> sendWebhookText(@RequestParam String webhookUrl,
                                             @RequestParam(required = false) String secret,
                                             @Valid @RequestBody FeishuTextMessageRequest request) {
        String response = feishuMessageService.sendWebhookTextMessage(webhookUrl, secret, request.getText());
        return ApiResult.success(response);
    }

    /**
     * 群机器人发送卡片消息
     *
     * @param webhookUrl 群机器人 Webhook 地址
     * @param secret 签名密钥
     * @param card 卡片内容
     * @return 飞书响应内容
     */
    @PostMapping("/webhook/card")
    public ApiResult<String> sendWebhookCard(@RequestParam String webhookUrl,
                                             @RequestParam(required = false) String secret,
                                             @RequestBody Map<String, Object> card) {
        String response = feishuMessageService.sendWebhookCardMessage(webhookUrl, secret, card);
        return ApiResult.success(response);
    }
}
```

接口调用示例如下：

```bash
# 发送文本消息
curl -X POST "http://localhost:8080/api/feishu/messages/text" \
  -H "Content-Type: application/json" \
  -d '{
    "receiveId": "ou_xxxxxxxxxxxxx",
    "receiveIdType": "open_id",
    "text": "这是一条来自业务系统的飞书文本消息"
  }'

# 发送卡片消息
curl -X POST "http://localhost:8080/api/feishu/messages/card" \
  -H "Content-Type: application/json" \
  -d '{
    "receiveId": "ou_xxxxxxxxxxxxx",
    "receiveIdType": "open_id",
    "title": "工单提醒",
    "content": "你有一个新的待处理工单，请及时处理。",
    "buttonText": "查看工单",
    "buttonUrl": "https://example.com/ticket/10001"
  }'
```

### 用户查询接口

用户查询接口用于对外提供飞书用户和部门数据查询能力。业务系统不建议直接暴露飞书原始接口，而是按常见场景封装为“按用户 ID 查询”“按手机号或邮箱查询”“按部门 ID 查询”等接口。

接口设计如下：

| 接口名称        | 请求方式 | 接口路径                                 | 说明                                            |
| --------------- | -------- | ---------------------------------------- | ----------------------------------------------- |
| 获取用户信息    | GET      | `/api/feishu/users/{userId}`             | 根据飞书用户 ID 查询用户详情                    |
| 批量获取用户 ID | POST     | `/api/feishu/users/batch-get-id`         | 根据手机号或邮箱获取 open_id、user_id、union_id |
| 获取部门信息    | GET      | `/api/feishu/departments/{departmentId}` | 根据飞书部门 ID 查询部门详情                    |
| 绑定系统账号    | POST     | `/api/feishu/users/bind`                 | 将系统用户与飞书用户绑定                        |

下面定义批量获取用户 ID 请求对象。

文件位置：`src/main/java/io/github/atengk/feishu/model/request/FeishuUserIdQueryRequest.java`

```java
package io.github.atengk.feishu.model.request;

import lombok.Data;

import java.util.List;

/**
 * 飞书用户 ID 查询请求
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Data
public class FeishuUserIdQueryRequest {

    /**
     * 手机号列表
     */
    private List<String> mobiles;

    /**
     * 邮箱列表
     */
    private List<String> emails;

    /**
     * 返回的用户 ID 类型，常用值：open_id、user_id、union_id
     */
    private String userIdType = "open_id";
}
```

下面定义系统账号绑定请求对象。

文件位置：`src/main/java/io/github/atengk/feishu/model/request/FeishuUserBindRequest.java`

```java
package io.github.atengk.feishu.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 飞书用户绑定请求
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Data
public class FeishuUserBindRequest {

    /**
     * 系统用户 ID
     */
    @NotNull(message = "系统用户 ID 不能为空")
    private Long sysUserId;

    /**
     * 系统用户名
     */
    @NotBlank(message = "系统用户名不能为空")
    private String username;

    /**
     * 手机号
     */
    private String mobile;

    /**
     * 邮箱
     */
    private String email;
}
```

下面是用户查询接口控制器，复用前文的 `FeishuContactService` 和 `FeishuAccountBindService`。

文件位置：`src/main/java/io/github/atengk/feishu/controller/FeishuUserApiController.java`

```java
package io.github.atengk.feishu.controller;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import io.github.atengk.common.web.ApiResult;
import io.github.atengk.feishu.exception.FeishuApiException;
import io.github.atengk.feishu.model.entity.SysUserFeishuBind;
import io.github.atengk.feishu.model.request.FeishuUserBindRequest;
import io.github.atengk.feishu.model.request.FeishuUserIdQueryRequest;
import io.github.atengk.feishu.service.FeishuAccountBindService;
import io.github.atengk.feishu.service.FeishuContactService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 飞书用户业务接口
 *
 * @author Ateng
 * @since 2026-05-07
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/feishu")
public class FeishuUserApiController {

    private final FeishuContactService feishuContactService;

    private final FeishuAccountBindService feishuAccountBindService;

    /**
     * 获取飞书用户信息
     *
     * @param userId 用户 ID
     * @param userIdType 用户 ID 类型
     * @return 用户信息
     */
    @GetMapping("/users/{userId}")
    public ApiResult<JSONObject> getUser(@PathVariable String userId,
                                         @RequestParam(defaultValue = "open_id") String userIdType) {
        JSONObject user = feishuContactService.getUser(userId, userIdType);
        return ApiResult.success(user);
    }

    /**
     * 批量获取用户 ID
     *
     * @param request 请求参数
     * @return 用户 ID 列表
     */
    @PostMapping("/users/batch-get-id")
    public ApiResult<JSONArray> batchGetUserId(@RequestBody FeishuUserIdQueryRequest request) {
        JSONArray userList = feishuContactService.batchGetUserId(
                request.getMobiles(),
                request.getEmails(),
                request.getUserIdType()
        );
        return ApiResult.success(userList);
    }

    /**
     * 获取飞书部门信息
     *
     * @param departmentId 部门 ID
     * @param departmentIdType 部门 ID 类型
     * @return 部门信息
     */
    @GetMapping("/departments/{departmentId}")
    public ApiResult<JSONObject> getDepartment(@PathVariable String departmentId,
                                               @RequestParam(defaultValue = "open_department_id") String departmentIdType) {
        JSONObject department = feishuContactService.getDepartment(departmentId, departmentIdType);
        return ApiResult.success(department);
    }

    /**
     * 绑定系统账号与飞书账号
     *
     * @param request 请求参数
     * @return 绑定关系
     */
    @PostMapping("/users/bind")
    public ApiResult<SysUserFeishuBind> bindUser(@Valid @RequestBody FeishuUserBindRequest request) {
        if (StrUtil.isNotBlank(request.getMobile())) {
            SysUserFeishuBind bind = feishuAccountBindService.bindByMobile(
                    request.getSysUserId(),
                    request.getUsername(),
                    request.getMobile()
            );
            return ApiResult.success(bind);
        }

        if (StrUtil.isNotBlank(request.getEmail())) {
            SysUserFeishuBind bind = feishuAccountBindService.bindByEmail(
                    request.getSysUserId(),
                    request.getUsername(),
                    request.getEmail()
            );
            return ApiResult.success(bind);
        }

        throw new FeishuApiException("手机号和邮箱不能同时为空");
    }
}
```

接口调用示例如下：

```bash
# 查询用户详情
curl "http://localhost:8080/api/feishu/users/ou_xxxxxxxxxxxxx?userIdType=open_id"

# 通过手机号或邮箱获取用户 ID
curl -X POST "http://localhost:8080/api/feishu/users/batch-get-id" \
  -H "Content-Type: application/json" \
  -d '{
    "mobiles": ["13812345678"],
    "emails": ["zhangsan@example.com"],
    "userIdType": "open_id"
  }'

# 绑定系统账号与飞书账号
curl -X POST "http://localhost:8080/api/feishu/users/bind" \
  -H "Content-Type: application/json" \
  -d '{
    "sysUserId": 10001,
    "username": "zhangsan",
    "mobile": "13812345678"
  }'
```

### 事件回调接口

事件回调接口用于接收飞书事件推送，包括回调地址校验、消息事件、卡片交互事件、审批事件等。飞书事件回调支持 Verification Token 校验和签名校验；如果配置了加密策略，安全校验可通过请求头中的 `X-Lark-Request-Timestamp`、`X-Lark-Request-Nonce`、`X-Lark-Signature` 与 `encrypt_key` 计算 SHA-256 签名完成。([飞书 API](https://feishu.apifox.cn/doc-1940243?utm_source=chatgpt.com))

接口设计如下：

| 接口名称     | 请求方式 | 接口路径                     | 说明                         |
| ------------ | -------- | ---------------------------- | ---------------------------- |
| 飞书事件回调 | POST     | `/api/feishu/event/callback` | 接收飞书开放平台事件         |
| 事件健康检查 | GET      | `/api/feishu/event/health`   | 用于本地或平台侧检查服务状态 |

前文事件控制器只接收了 `requestBody`，如果需要支持签名校验，应同时接收请求头。下面给出增强后的事件回调控制器。

文件位置：`src/main/java/io/github/atengk/feishu/controller/FeishuEventApiController.java`

```java
package io.github.atengk.feishu.controller;

import io.github.atengk.common.web.ApiResult;
import io.github.atengk.feishu.service.FeishuEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 飞书事件回调业务接口
 *
 * @author Ateng
 * @since 2026-05-07
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/feishu/event")
public class FeishuEventApiController {

    private final FeishuEventService feishuEventService;

    /**
     * 飞书事件回调
     *
     * @param timestamp 请求时间戳
     * @param nonce 随机字符串
     * @param signature 请求签名
     * @param requestBody 原始请求体
     * @return 回调响应
     */
    @PostMapping("/callback")
    public Map<String, Object> callback(@RequestHeader(value = "X-Lark-Request-Timestamp", required = false) String timestamp,
                                        @RequestHeader(value = "X-Lark-Request-Nonce", required = false) String nonce,
                                        @RequestHeader(value = "X-Lark-Signature", required = false) String signature,
                                        @RequestBody String requestBody) {
        return feishuEventService.handleEvent(timestamp, nonce, signature, requestBody);
    }

    /**
     * 事件回调健康检查
     *
     * @return 检查结果
     */
    @GetMapping("/health")
    public ApiResult<String> health() {
        return ApiResult.success("feishu event callback is running");
    }
}
```

对应地，事件服务接口需要增加请求头参数。

文件位置：`src/main/java/io/github/atengk/feishu/service/FeishuEventService.java`

```java
package io.github.atengk.feishu.service;

import java.util.Map;

/**
 * 飞书事件服务
 *
 * @author Ateng
 * @since 2026-05-07
 */
public interface FeishuEventService {

    /**
     * 处理飞书事件回调
     *
     * @param timestamp 请求时间戳
     * @param nonce 随机字符串
     * @param signature 请求签名
     * @param requestBody 原始请求体
     * @return 回调响应
     */
    Map<String, Object> handleEvent(String timestamp, String nonce, String signature, String requestBody);
}
```

事件回调接口返回值需要注意：如果是飞书 URL 校验请求，需要返回 `{"challenge":"xxx"}`；如果是普通事件处理成功，可以返回 `{"code":0,"msg":"success"}`。不要统一包装成 `ApiResult`，否则可能导致飞书开放平台无法识别地址校验结果。

### 审批业务接口

审批业务接口用于对业务系统暴露审批创建、审批查询和审批状态同步能力。审批接口设计不建议只透传飞书原始参数，而是建议结合本地业务单据 ID、审批定义 Code、发起人、表单参数等信息封装为业务请求。

接口设计如下：

| 接口名称         | 请求方式 | 接口路径                                              | 说明                             |
| ---------------- | -------- | ----------------------------------------------------- | -------------------------------- |
| 创建审批实例     | POST     | `/api/feishu/approvals/instances`                     | 创建飞书审批实例                 |
| 查询审批实例详情 | GET      | `/api/feishu/approvals/instances/{instanceCode}`      | 查询单个审批实例详情             |
| 查询审批实例列表 | GET      | `/api/feishu/approvals/instances`                     | 按审批定义和时间范围查询实例列表 |
| 同步审批状态     | POST     | `/api/feishu/approvals/instances/{instanceCode}/sync` | 主动同步单个审批实例状态         |

下面定义审批创建请求对象。

文件位置：`src/main/java/io/github/atengk/feishu/model/request/FeishuApprovalCreateRequest.java`

```java
package io.github.atengk.feishu.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 飞书审批创建请求
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Data
public class FeishuApprovalCreateRequest {

    /**
     * 本地业务单据 ID
     */
    @NotBlank(message = "本地业务单据 ID 不能为空")
    private String businessId;

    /**
     * 审批定义 Code
     */
    @NotBlank(message = "审批定义 Code 不能为空")
    private String approvalCode;

    /**
     * 发起人 open_id
     */
    @NotBlank(message = "发起人 open_id 不能为空")
    private String openId;

    /**
     * 审批表单控件值
     */
    @NotEmpty(message = "审批表单不能为空")
    private List<Map<String, Object>> formItems;
}
```

下面是审批业务接口控制器。

文件位置：`src/main/java/io/github/atengk/feishu/controller/FeishuApprovalApiController.java`

```java
package io.github.atengk.feishu.controller;

import cn.hutool.json.JSONObject;
import io.github.atengk.common.web.ApiResult;
import io.github.atengk.feishu.model.request.FeishuApprovalCreateRequest;
import io.github.atengk.feishu.service.FeishuApprovalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 飞书审批业务接口
 *
 * @author Ateng
 * @since 2026-05-07
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/feishu/approvals")
public class FeishuApprovalApiController {

    private final FeishuApprovalService feishuApprovalService;

    /**
     * 创建审批实例
     *
     * @param request 请求参数
     * @return 审批实例 Code
     */
    @PostMapping("/instances")
    public ApiResult<String> createInstance(@Valid @RequestBody FeishuApprovalCreateRequest request) {
        String instanceCode = feishuApprovalService.createInstance(
                request.getApprovalCode(),
                request.getOpenId(),
                request.getFormItems()
        );

        // 实际项目中应在这里保存 businessId 与 instanceCode 的映射关系
        return ApiResult.success(instanceCode);
    }

    /**
     * 查询审批实例详情
     *
     * @param instanceCode 审批实例 Code
     * @param userId 查询用户 ID
     * @param userIdType 用户 ID 类型
     * @return 审批实例详情
     */
    @GetMapping("/instances/{instanceCode}")
    public ApiResult<JSONObject> getInstance(@PathVariable String instanceCode,
                                             @RequestParam String userId,
                                             @RequestParam(defaultValue = "open_id") String userIdType) {
        JSONObject detail = feishuApprovalService.getInstance(instanceCode, userId, userIdType);
        return ApiResult.success(detail);
    }

    /**
     * 查询审批实例列表
     *
     * @param approvalCode 审批定义 Code
     * @param startTime 开始时间，毫秒时间戳
     * @param endTime 结束时间，毫秒时间戳
     * @param pageSize 分页大小
     * @param pageToken 分页 Token
     * @return 审批实例列表
     */
    @GetMapping("/instances")
    public ApiResult<JSONObject> queryInstances(@RequestParam String approvalCode,
                                                @RequestParam Long startTime,
                                                @RequestParam Long endTime,
                                                @RequestParam(defaultValue = "100") Integer pageSize,
                                                @RequestParam(required = false) String pageToken) {
        JSONObject result = feishuApprovalService.queryInstanceCodes(
                approvalCode,
                startTime,
                endTime,
                pageSize,
                pageToken
        );
        return ApiResult.success(result);
    }

    /**
     * 主动同步审批实例状态
     *
     * @param instanceCode 审批实例 Code
     * @param userId 查询用户 ID
     * @param userIdType 用户 ID 类型
     * @return 审批实例详情
     */
    @PostMapping("/instances/{instanceCode}/sync")
    public ApiResult<JSONObject> syncInstance(@PathVariable String instanceCode,
                                              @RequestParam String userId,
                                              @RequestParam(defaultValue = "open_id") String userIdType) {
        JSONObject detail = feishuApprovalService.getInstance(instanceCode, userId, userIdType);

        // 实际项目中应在这里根据 instanceCode 更新本地业务审批状态
        return ApiResult.success(detail);
    }
}
```

接口调用示例如下：

```bash
# 创建审批实例
curl -X POST "http://localhost:8080/api/feishu/approvals/instances" \
  -H "Content-Type: application/json" \
  -d '{
    "businessId": "PO-20260507-001",
    "approvalCode": "7C468A54-8745-2245-9675-08B7C63E7A85",
    "openId": "ou_xxxxxxxxxxxxx",
    "formItems": [
      {
        "id": "widget1",
        "type": "input",
        "value": "采购申请"
      },
      {
        "id": "widget2",
        "type": "textarea",
        "value": "申请采购研发测试设备"
      }
    ]
  }'

# 查询审批实例详情
curl "http://localhost:8080/api/feishu/approvals/instances/357C21A0-2069-4F6B-955F-1DFBE6710C51?userId=ou_xxxxxxxxxxxxx&userIdType=open_id"

# 主动同步审批状态
curl -X POST "http://localhost:8080/api/feishu/approvals/instances/357C21A0-2069-4F6B-955F-1DFBE6710C51/sync?userId=ou_xxxxxxxxxxxxx&userIdType=open_id"
```

## 异常处理

本章节用于统一处理飞书接口异常、Token 失效和回调验签失败。飞书接口通常会返回 `code`、`msg`、`data` 等字段，业务系统应在客户端层统一解析错误码，不建议在每个业务 Service 中重复判断。消息、通讯录、审批等接口都使用 Bearer Token 鉴权，调用失败时应区分参数错误、权限不足、Token 失效、接口限流、网络异常和回调安全校验失败等情况。([Apifox](https://s.apifox.cn/apidoc/docs-site/532425/api-58348294?utm_source=chatgpt.com))

### 飞书接口异常处理

飞书接口异常处理建议分为三层：客户端层解析飞书响应，业务层抛出明确异常，全局异常处理器统一返回标准响应。这样 Controller 不需要捕获飞书异常，也不会把飞书原始错误直接暴露给前端。

建议异常类型如下：

| 异常类型                           | 说明                                       |
| ---------------------------------- | ------------------------------------------ |
| `FeishuApiException`               | 飞书接口调用失败、响应错误、业务错误码非 0 |
| `FeishuTokenExpiredException`      | 访问令牌失效或即将失效                     |
| `FeishuCallbackSignatureException` | 事件回调签名校验失败                       |
| `MethodArgumentNotValidException`  | Spring 参数校验失败                        |
| `Exception`                        | 兜底系统异常                               |

下面扩展飞书异常类，增加错误码字段，便于全局异常处理和日志排查。

文件位置：`src/main/java/io/github/atengk/feishu/exception/FeishuApiException.java`

```java
package io.github.atengk.feishu.exception;

import lombok.Getter;

/**
 * 飞书接口异常
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Getter
public class FeishuApiException extends RuntimeException {

    /**
     * 飞书错误码
     */
    private final Integer feishuCode;

    /**
     * 创建飞书接口异常
     *
     * @param message 异常信息
     */
    public FeishuApiException(String message) {
        super(message);
        this.feishuCode = null;
    }

    /**
     * 创建飞书接口异常
     *
     * @param message 异常信息
     * @param cause 原始异常
     */
    public FeishuApiException(String message, Throwable cause) {
        super(message, cause);
        this.feishuCode = null;
    }

    /**
     * 创建飞书接口异常
     *
     * @param feishuCode 飞书错误码
     * @param message 异常信息
     */
    public FeishuApiException(Integer feishuCode, String message) {
        super(message);
        this.feishuCode = feishuCode;
    }
}
```

下面定义 Token 失效异常。

文件位置：`src/main/java/io/github/atengk/feishu/exception/FeishuTokenExpiredException.java`

```java
package io.github.atengk.feishu.exception;

/**
 * 飞书 Token 失效异常
 *
 * @author Ateng
 * @since 2026-05-07
 */
public class FeishuTokenExpiredException extends FeishuApiException {

    /**
     * 创建飞书 Token 失效异常
     *
     * @param feishuCode 飞书错误码
     * @param message 异常信息
     */
    public FeishuTokenExpiredException(Integer feishuCode, String message) {
        super(feishuCode, message);
    }
}
```

下面定义回调签名异常。

文件位置：`src/main/java/io/github/atengk/feishu/exception/FeishuCallbackSignatureException.java`

```java
package io.github.atengk.feishu.exception;

/**
 * 飞书回调签名校验异常
 *
 * @author Ateng
 * @since 2026-05-07
 */
public class FeishuCallbackSignatureException extends RuntimeException {

    /**
     * 创建飞书回调签名校验异常
     *
     * @param message 异常信息
     */
    public FeishuCallbackSignatureException(String message) {
        super(message);
    }
}
```

下面是全局异常处理器，用于统一返回 `ApiResult`。

文件位置：`src/main/java/io/github/atengk/common/exception/GlobalExceptionHandler.java`

```java
package io.github.atengk.common.exception;

import cn.hutool.core.collection.CollUtil;
import io.github.atengk.common.web.ApiResult;
import io.github.atengk.feishu.exception.FeishuApiException;
import io.github.atengk.feishu.exception.FeishuCallbackSignatureException;
import io.github.atengk.feishu.exception.FeishuTokenExpiredException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

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
     * 处理飞书接口异常
     *
     * @param e 异常对象
     * @return 统一响应
     */
    @ExceptionHandler(FeishuApiException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResult<Void> handleFeishuApiException(FeishuApiException e) {
        log.warn("飞书接口调用失败，feishuCode={}，message={}", e.getFeishuCode(), e.getMessage());
        return ApiResult.fail(40001, e.getMessage());
    }

    /**
     * 处理飞书 Token 失效异常
     *
     * @param e 异常对象
     * @return 统一响应
     */
    @ExceptionHandler(FeishuTokenExpiredException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiResult<Void> handleFeishuTokenExpiredException(FeishuTokenExpiredException e) {
        log.warn("飞书 Token 已失效，feishuCode={}，message={}", e.getFeishuCode(), e.getMessage());
        return ApiResult.fail(40101, "飞书访问令牌已失效，请稍后重试");
    }

    /**
     * 处理飞书回调签名异常
     *
     * @param e 异常对象
     * @return 统一响应
     */
    @ExceptionHandler(FeishuCallbackSignatureException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiResult<Void> handleFeishuCallbackSignatureException(FeishuCallbackSignatureException e) {
        log.warn("飞书回调验签失败，message={}", e.getMessage());
        return ApiResult.fail(40102, "飞书回调验签失败");
    }

    /**
     * 处理参数校验异常
     *
     * @param e 异常对象
     * @return 统一响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResult<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        List<FieldError> fieldErrors = e.getBindingResult().getFieldErrors();
        String message = CollUtil.emptyIfNull(fieldErrors)
                .stream()
                .map(item -> item.getField() + "：" + item.getDefaultMessage())
                .collect(Collectors.joining("；"));

        log.warn("接口参数校验失败，message={}", message);
        return ApiResult.fail(40000, message);
    }

    /**
     * 处理系统异常
     *
     * @param e 异常对象
     * @return 统一响应
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResult<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return ApiResult.fail(50000, "系统异常，请联系管理员");
    }
}
```

异常处理建议如下：

| 异常场景     | HTTP 状态码 | 业务码  | 处理建议                 |
| ------------ | ----------- | ------- | ------------------------ |
| 参数校验失败 | 400         | `40000` | 返回字段级错误信息       |
| 飞书接口失败 | 400         | `40001` | 返回简化后的业务错误     |
| Token 失效   | 401         | `40101` | 清理缓存并重新获取 Token |
| 回调验签失败 | 401         | `40102` | 拒绝处理并记录安全日志   |
| 系统异常     | 500         | `50000` | 返回通用错误，不暴露堆栈 |

### Token 失效处理

Token 失效处理用于解决飞书访问令牌过期、缓存失效、权限变更或令牌被吊销导致的接口调用失败。推荐策略是：Redis 缓存 Token 时设置提前过期时间；如果飞书接口仍返回 Token 相关错误，则主动删除缓存，重新获取 Token 后重试一次，避免无限重试。

下面先定义 Token 缓存键常量，避免多个类中硬编码。

文件位置：`src/main/java/io/github/atengk/feishu/constant/FeishuCacheKeyConstant.java`

```java
package io.github.atengk.feishu.constant;

/**
 * 飞书缓存键常量
 *
 * @author Ateng
 * @since 2026-05-07
 */
public class FeishuCacheKeyConstant {

    private FeishuCacheKeyConstant() {
    }

    /**
     * 应用访问令牌缓存键
     */
    public static final String APP_ACCESS_TOKEN_KEY = "feishu:token:app_access_token";

    /**
     * 租户访问令牌缓存键
     */
    public static final String TENANT_ACCESS_TOKEN_KEY = "feishu:token:tenant_access_token";
}
```

下面定义飞书错误码判断工具。不同接口可能返回不同的 Token 错误码，项目中建议将错误码集合配置化，下面仅提供可扩展的代码结构。

文件位置：`src/main/java/io/github/atengk/feishu/util/FeishuErrorCodeUtil.java`

```java
package io.github.atengk.feishu.util;

import java.util.Set;

/**
 * 飞书错误码工具类
 *
 * @author Ateng
 * @since 2026-05-07
 */
public class FeishuErrorCodeUtil {

    private static final Set<Integer> TOKEN_INVALID_CODES = Set.of(
            99991661,
            99991663,
            99991664,
            99991668
    );

    private FeishuErrorCodeUtil() {
    }

    /**
     * 判断是否为 Token 失效错误码
     *
     * @param code 飞书错误码
     * @return 是否为 Token 失效
     */
    public static boolean isTokenInvalid(Integer code) {
        return code != null && TOKEN_INVALID_CODES.contains(code);
    }
}
```

下面调整 `FeishuResponseUtil`，在解析飞书响应时识别 Token 失效错误。

文件位置：`src/main/java/io/github/atengk/feishu/util/FeishuResponseUtil.java`

```java
package io.github.atengk.feishu.util;

import cn.hutool.core.convert.Convert;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import io.github.atengk.feishu.exception.FeishuApiException;
import io.github.atengk.feishu.exception.FeishuTokenExpiredException;

/**
 * 飞书接口响应工具类
 *
 * @author Ateng
 * @since 2026-05-07
 */
public class FeishuResponseUtil {

    private FeishuResponseUtil() {
    }

    /**
     * 解析成功响应
     *
     * @param responseBody 响应内容
     * @return 响应 JSON
     */
    public static JSONObject parseSuccessResponse(String responseBody) {
        if (!JSONUtil.isTypeJSON(responseBody)) {
            throw new FeishuApiException("飞书接口响应不是合法 JSON：" + responseBody);
        }

        JSONObject response = JSONUtil.parseObj(responseBody);
        Integer code = Convert.toInt(response.get("code"), -1);
        if (Integer.valueOf(0).equals(code)) {
            return response;
        }

        String message = response.getStr("msg", "未知错误");
        if (FeishuErrorCodeUtil.isTokenInvalid(code)) {
            throw new FeishuTokenExpiredException(code, "飞书 Token 失效，msg=" + message);
        }

        throw new FeishuApiException(code, "飞书接口调用失败，code=" + code + "，msg=" + message);
    }

    /**
     * 解析成功响应中的 data 节点
     *
     * @param responseBody 响应内容
     * @return data 节点
     */
    public static JSONObject parseSuccessData(String responseBody) {
        JSONObject response = parseSuccessResponse(responseBody);
        JSONObject data = response.getJSONObject("data");
        return data == null ? new JSONObject() : data;
    }
}
```

如果需要在 Token 失效后自动重试，可以在业务 Service 中针对关键接口做“一次重试”。下面给出一个通用模板，实际项目中可封装到 `FeishuApiClient` 或 AOP 中。

文件位置：`src/main/java/io/github/atengk/feishu/service/impl/FeishuRetrySupport.java`

```java
package io.github.atengk.feishu.service.impl;

import io.github.atengk.feishu.constant.FeishuCacheKeyConstant;
import io.github.atengk.feishu.exception.FeishuTokenExpiredException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/**
 * 飞书接口重试支持
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FeishuRetrySupport {

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * Token 失效时清理缓存并重试一次
     *
     * @param supplier 接口调用逻辑
     * @return 调用结果
     * @param <T> 返回类型
     */
    public <T> T retryOnceWhenTokenExpired(Supplier<T> supplier) {
        try {
            return supplier.get();
        } catch (FeishuTokenExpiredException e) {
            log.warn("飞书 Token 失效，清理缓存后准备重试一次，feishuCode={}", e.getFeishuCode());
            clearTokenCache();
            return supplier.get();
        }
    }

    /**
     * 清理飞书 Token 缓存
     */
    private void clearTokenCache() {
        stringRedisTemplate.delete(FeishuCacheKeyConstant.APP_ACCESS_TOKEN_KEY);
        stringRedisTemplate.delete(FeishuCacheKeyConstant.TENANT_ACCESS_TOKEN_KEY);
        log.info("飞书 Token 缓存已清理");
    }
}
```

使用示例：

```java
String messageId = feishuRetrySupport.retryOnceWhenTokenExpired(() ->
        feishuMessageService.sendTextMessage("ou_xxxxxxxxxxxxx", "open_id", "测试消息")
);
```

Token 失效处理建议如下：

| 处理点              | 建议                                                  |
| ------------------- | ----------------------------------------------------- |
| 提前过期            | Redis TTL 小于飞书返回的过期时间                      |
| 失败重试            | Token 失效后清理缓存并重试一次                        |
| 禁止无限重试        | 重试一次仍失败时直接抛出异常                          |
| 区分 Token 类型     | app、tenant、user Token 缓存键必须区分                |
| 用户 Token 单独处理 | `user_access_token` 失效后优先用 `refresh_token` 刷新 |
| 日志脱敏            | 不要在日志中输出完整 Token                            |
| 配置化错误码        | Token 失效错误码建议做成配置项，便于后续维护          |

### 回调验签失败处理

回调验签失败处理用于防止伪造请求调用事件回调接口。飞书事件订阅安全校验支持签名校验和 Verification Token 校验；签名校验使用请求头中的时间戳、随机字符串、`encrypt_key` 和原始请求体计算 SHA-256 摘要，再与 `X-Lark-Signature` 比对。([飞书 API](https://feishu.apifox.cn/doc-7518444?utm_source=chatgpt.com))

下面给出签名校验工具类。

文件位置：`src/main/java/io/github/atengk/feishu/util/FeishuCallbackSignatureUtil.java`

```java
package io.github.atengk.feishu.util;

import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.feishu.exception.FeishuCallbackSignatureException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * 飞书回调签名工具类
 *
 * @author Ateng
 * @since 2026-05-07
 */
public class FeishuCallbackSignatureUtil {

    private FeishuCallbackSignatureUtil() {
    }

    /**
     * 校验飞书回调签名
     *
     * @param timestamp 请求时间戳
     * @param nonce 随机字符串
     * @param encryptKey 加密密钥
     * @param requestBody 原始请求体
     * @param signature 请求签名
     */
    public static void verify(String timestamp, String nonce, String encryptKey, String requestBody, String signature) {
        if (StrUtil.hasBlank(timestamp, nonce, encryptKey, requestBody, signature)) {
            throw new FeishuCallbackSignatureException("飞书回调签名参数不完整");
        }

        String calculatedSignature = calculate(timestamp, nonce, encryptKey, requestBody);
        if (!StrUtil.equalsIgnoreCase(calculatedSignature, signature)) {
            throw new FeishuCallbackSignatureException("飞书回调签名不匹配");
        }
    }

    /**
     * 计算飞书回调签名
     *
     * @param timestamp 请求时间戳
     * @param nonce 随机字符串
     * @param encryptKey 加密密钥
     * @param requestBody 原始请求体
     * @return 签名
     */
    private static String calculate(String timestamp, String nonce, String encryptKey, String requestBody) {
        try {
            String content = timestamp + nonce + encryptKey + requestBody;
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] digest = messageDigest.digest(content.getBytes(StandardCharsets.UTF_8));
            return HexUtil.encodeHexStr(digest);
        } catch (Exception e) {
            throw new FeishuCallbackSignatureException("飞书回调签名计算失败：" + e.getMessage());
        }
    }
}
```

然后在 `FeishuEventServiceImpl` 的事件处理入口中加入签名校验。下面只展示需要替换的核心入口方法。

文件位置：`src/main/java/io/github/atengk/feishu/service/impl/FeishuEventServiceImpl.java`

```java
/**
 * 处理飞书事件回调
 *
 * @param timestamp 请求时间戳
 * @param nonce 随机字符串
 * @param signature 请求签名
 * @param requestBody 原始请求体
 * @return 回调响应
 */
@Override
public Map<String, Object> handleEvent(String timestamp, String nonce, String signature, String requestBody) {
    if (StrUtil.isNotBlank(signature)) {
        FeishuCallbackSignatureUtil.verify(
                timestamp,
                nonce,
                feishuProperties.getEncryptKey(),
                requestBody,
                signature
        );
        log.info("飞书事件回调签名校验成功");
    }

    if (!JSONUtil.isTypeJSON(requestBody)) {
        throw new FeishuApiException("飞书事件请求体不是合法 JSON");
    }

    JSONObject eventBody = JSONUtil.parseObj(requestBody);
    eventBody = decryptIfNecessary(eventBody);

    verifyToken(eventBody);

    if (isUrlVerification(eventBody)) {
        String challenge = eventBody.getStr("challenge");
        log.info("飞书事件回调地址校验成功");
        return MapUtil.<String, Object>builder()
                .put("challenge", challenge)
                .build();
    }

    if (!markEventIfAbsent(eventBody)) {
        log.info("飞书事件重复推送，已忽略，eventId={}", getEventId(eventBody));
        return success();
    }

    dispatchEvent(eventBody);
    return success();
}
```

回调验签失败时建议直接返回 HTTP 401，并记录安全日志。不要继续解密、分发或执行业务逻辑。

回调安全处理建议如下：

| 处理点           | 建议                                                |
| ---------------- | --------------------------------------------------- |
| 签名校验优先     | 有签名头时，先验签再解析事件体                      |
| 保留原始 Body    | 签名计算必须使用原始请求体，不要使用格式化后的 JSON |
| 验签失败立即拒绝 | 返回 401，不进入业务处理                            |
| Token 二次校验   | 解密后继续校验 `verification-token`                 |
| 控制日志内容     | 不记录完整回调明文，避免泄露用户信息                |
| 幂等处理         | 验签成功后仍需要按 `event_id` 做幂等                |
| 告警监控         | 验签失败次数异常增多时触发安全告警                  |

完成本章节后，项目的接口边界、统一响应、飞书异常处理、Token 失效处理和回调安全校验已经具备基础工程化能力。后续可以继续补充“日志与调试”“测试验证”“部署说明”等章节。

## 日志与调试

本章节用于规范 Spring Boot 集成飞书时的日志记录、接口响应日志和本地调试方式。飞书集成属于外部系统调用，排查问题时通常需要同时关注本地请求参数、飞书接口响应、Token 状态、事件回调原文、业务处理结果和异常堆栈。本文继续基于你上传的大纲补充后续章节。

### 请求日志记录

请求日志用于记录业务系统调用飞书集成接口的入口信息，包括请求路径、请求方法、请求耗时、调用来源、请求参数和响应状态。日志记录需要满足两个要求：一是能快速定位问题，二是不能泄露 `app_secret`、`access_token`、手机号、邮箱等敏感数据。

建议日志字段如下：

| 字段           | 说明                                    |
| -------------- | --------------------------------------- |
| `traceId`      | 请求链路 ID，用于串联一次请求的完整日志 |
| `method`       | HTTP 请求方法                           |
| `uri`          | 请求路径                                |
| `queryString`  | URL 查询参数                            |
| `clientIp`     | 客户端 IP                               |
| `cost`         | 请求耗时                                |
| `status`       | HTTP 响应状态                           |
| `requestBody`  | 请求体，需脱敏                          |
| `responseBody` | 响应体，需按需记录                      |
| `exception`    | 异常信息                                |

建议先配置日志输出格式，保证每行日志都带有 `traceId`。

文件位置：`src/main/resources/logback-spring.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <!-- 控制台日志格式，包含 traceId 便于定位链路 -->
    <property name="CONSOLE_LOG_PATTERN"
              value="%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%thread] [%X{traceId}] %logger{36} - %msg%n"/>

    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>${CONSOLE_LOG_PATTERN}</pattern>
            <charset>UTF-8</charset>
        </encoder>
    </appender>

    <!-- 生产环境建议写入文件，并接入 ELK、Loki 或云日志服务 -->
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
    </root>

    <!-- 飞书集成模块日志级别 -->
    <logger name="io.github.atengk.feishu" level="INFO"/>
</configuration>
```

下面过滤器用于记录业务接口请求日志，并对敏感字段做基础脱敏处理。

文件位置：`src/main/java/io/github/atengk/common/web/RequestLogFilter.java`

```java
package io.github.atengk.common.web;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * 请求日志过滤器
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Component
public class RequestLogFilter extends OncePerRequestFilter {

    private static final String TRACE_ID = "traceId";

    private static final Set<String> EXCLUDE_URIS = Set.of(
            "/actuator/health",
            "/favicon.ico"
    );

    /**
     * 记录请求日志
     *
     * @param request 请求对象
     * @param response 响应对象
     * @param filterChain 过滤器链
     * @throws ServletException Servlet 异常
     * @throws IOException IO 异常
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String uri = request.getRequestURI();
        if (EXCLUDE_URIS.contains(uri)) {
            filterChain.doFilter(request, response);
            return;
        }

        long startTime = System.currentTimeMillis();
        String traceId = IdUtil.fastSimpleUUID();
        MDC.put(TRACE_ID, traceId);

        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request);

        try {
            filterChain.doFilter(requestWrapper, response);
        } finally {
            long cost = System.currentTimeMillis() - startTime;
            String requestBody = getRequestBody(requestWrapper);

            log.info("接口请求完成，method={}，uri={}，query={}，clientIp={}，status={}，cost={}ms，requestBody={}",
                    request.getMethod(),
                    uri,
                    StrUtil.blankToDefault(request.getQueryString(), ""),
                    getClientIp(request),
                    response.getStatus(),
                    cost,
                    desensitize(requestBody));

            MDC.remove(TRACE_ID);
        }
    }

    /**
     * 获取请求体
     *
     * @param request 请求包装对象
     * @return 请求体
     */
    private String getRequestBody(ContentCachingRequestWrapper request) {
        String contentType = request.getContentType();
        if (StrUtil.isBlank(contentType) || !StrUtil.containsIgnoreCase(contentType, MediaType.APPLICATION_JSON_VALUE)) {
            return "";
        }

        byte[] content = request.getContentAsByteArray();
        if (content.length == 0) {
            return "";
        }

        return StrUtil.sub(new String(content, StandardCharsets.UTF_8), 0, 2000);
    }

    /**
     * 获取客户端 IP
     *
     * @param request 请求对象
     * @return 客户端 IP
     */
    private String getClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StrUtil.isNotBlank(forwardedFor)) {
            return StrUtil.split(forwardedFor, ',').get(0);
        }
        return request.getRemoteAddr();
    }

    /**
     * 脱敏日志内容
     *
     * @param content 原始内容
     * @return 脱敏内容
     */
    private String desensitize(String content) {
        if (StrUtil.isBlank(content)) {
            return "";
        }

        return content
                .replaceAll("(?i)(app_secret\"\\s*:\\s*\")[^\"]+", "$1******")
                .replaceAll("(?i)(access_token\"\\s*:\\s*\")[^\"]+", "$1******")
                .replaceAll("(?i)(refresh_token\"\\s*:\\s*\")[^\"]+", "$1******")
                .replaceAll("(?i)(encrypt_key\"\\s*:\\s*\")[^\"]+", "$1******")
                .replaceAll("(1[3-9]\\d)\\d{4}(\\d{4})", "$1****$2");
    }
}
```

如果项目通过网关统一接入，可以将 `traceId` 放在请求头中继续向下传递，例如 `X-Trace-Id`。这样可以把网关日志、应用日志、飞书请求日志和业务日志串联起来。

### 飞书响应日志记录

飞书响应日志用于记录调用飞书 OpenAPI 后的响应结果。建议记录接口路径、飞书错误码、错误消息、请求耗时和关键业务字段，不建议记录完整 Token、完整手机号、完整邮箱和大体积响应数据。

前文的 `FeishuApiClient` 已经封装了统一请求入口，可以在客户端层集中记录响应日志。建议将响应日志分为成功日志和失败日志：

| 日志类型 | 记录内容                                      |
| -------- | --------------------------------------------- |
| 成功日志 | 接口路径、耗时、飞书 `code`、关键 `data` 字段 |
| 失败日志 | 接口路径、耗时、飞书 `code`、`msg`、异常类型  |
| 调试日志 | 开发环境可开启完整响应，生产环境默认关闭      |
| 安全日志 | 回调验签失败、Token 校验失败、非法来源请求    |

下面给出一个可复用的飞书日志工具类，用于脱敏和截断响应内容。

文件位置：`src/main/java/io/github/atengk/feishu/util/FeishuLogUtil.java`

```java
package io.github.atengk.feishu.util;

import cn.hutool.core.util.StrUtil;

/**
 * 飞书日志工具类
 *
 * @author Ateng
 * @since 2026-05-07
 */
public class FeishuLogUtil {

    private static final int MAX_LENGTH = 3000;

    private FeishuLogUtil() {
    }

    /**
     * 脱敏并截断日志内容
     *
     * @param content 原始内容
     * @return 安全日志内容
     */
    public static String safeContent(String content) {
        if (StrUtil.isBlank(content)) {
            return "";
        }

        String value = content
                .replaceAll("(?i)(tenant_access_token\"\\s*:\\s*\")[^\"]+", "$1******")
                .replaceAll("(?i)(app_access_token\"\\s*:\\s*\")[^\"]+", "$1******")
                .replaceAll("(?i)(access_token\"\\s*:\\s*\")[^\"]+", "$1******")
                .replaceAll("(?i)(refresh_token\"\\s*:\\s*\")[^\"]+", "$1******")
                .replaceAll("(?i)(app_secret\"\\s*:\\s*\")[^\"]+", "$1******")
                .replaceAll("(1[3-9]\\d)\\d{4}(\\d{4})", "$1****$2");

        return StrUtil.sub(value, 0, MAX_LENGTH);
    }
}
```

在 `FeishuApiClient` 中建议按以下方式记录飞书响应日志。

```java
long startTime = System.currentTimeMillis();
String responseBody = request.execute().body();
long cost = System.currentTimeMillis() - startTime;

log.info("飞书接口响应完成，path={}，cost={}ms，response={}",
        path,
        cost,
        FeishuLogUtil.safeContent(responseBody));
```

日志记录需要注意以下事项：

| 注意事项         | 说明                                                         |
| ---------------- | ------------------------------------------------------------ |
| 不记录完整 Token | `tenant_access_token`、`app_access_token`、`user_access_token` 必须脱敏 |
| 不记录完整密钥   | `app_secret`、`encrypt_key`、Webhook `secret` 不允许明文输出 |
| 控制响应长度     | 飞书审批详情、通讯录列表等响应体可能较大，需要截断           |
| 区分环境         | 开发环境可适当详细，生产环境以关键信息为主                   |
| 记录飞书错误码   | 飞书接口失败时必须记录 `code` 和 `msg`                       |
| 记录业务主键     | 审批建议记录 `businessId`、`approvalCode`、`instanceCode`    |
| 记录事件 ID      | 事件回调建议记录 `eventId`、`eventType`、处理结果            |

### 本地调试方式

本地调试主要用于验证 Token 获取、消息发送、事件回调、审批创建和审批回调等功能。由于飞书事件回调需要公网 HTTPS 地址，本地开发时通常需要使用内网穿透工具将本机端口映射到公网。

本地调试流程如下：

| 步骤 | 说明                                     |
| ---- | ---------------------------------------- |
| 1    | 启动 Spring Boot 服务                    |
| 2    | 配置飞书应用凭证环境变量                 |
| 3    | 使用 curl 验证 Token 获取接口            |
| 4    | 使用 curl 验证消息发送接口               |
| 5    | 使用内网穿透生成 HTTPS 回调地址          |
| 6    | 在飞书开放平台配置事件回调地址           |
| 7    | 触发飞书事件并观察本地日志               |
| 8    | 使用飞书开放平台事件日志排查推送失败问题 |

本地启动命令如下：

```bash
export FEISHU_APP_ID="cli_xxxxxxxxxxxxx"
export FEISHU_APP_SECRET="your-app-secret"
export FEISHU_VERIFICATION_TOKEN="your-verification-token"
export FEISHU_ENCRYPT_KEY="your-encrypt-key"

mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

常用调试命令如下：

```bash
# 检查应用是否启动
curl "http://localhost:8080/actuator/health"

# 检查飞书事件回调服务是否可访问
curl "http://localhost:8080/api/feishu/event/health"

# 发送文本消息
curl -X POST "http://localhost:8080/api/feishu/messages/text" \
  -H "Content-Type: application/json" \
  -d '{
    "receiveId": "ou_xxxxxxxxxxxxx",
    "receiveIdType": "open_id",
    "text": "本地调试消息"
  }'
```

本地事件回调调试时，回调地址示例：

```text
https://your-domain.example.com/api/feishu/event/callback
```

本地调试常见问题如下：

| 问题             | 排查方式                                                |
| ---------------- | ------------------------------------------------------- |
| 回调地址校验失败 | 检查 HTTPS 地址是否可公网访问，接口是否返回 `challenge` |
| 收不到事件       | 检查应用权限、事件订阅、应用版本是否已发布              |
| Token 获取失败   | 检查 `app_id`、`app_secret` 是否属于同一个应用          |
| 消息发送失败     | 检查机器人能力、接收人 ID 类型、应用可用范围            |
| 审批创建失败     | 检查 `approval_code`、表单控件 ID、发起人 open_id       |
| 回调解密失败     | 检查 `encrypt_key` 是否与飞书后台一致                   |
| 验签失败         | 检查是否使用原始请求体计算签名                          |

## 测试验证

本章节用于说明飞书集成模块的测试方式，包括单元测试、接口联调和飞书开放平台验证。由于飞书接口依赖外部平台，测试时应区分本地单元测试和真实联调测试，避免在单元测试中直接调用外部飞书接口。

### 单元测试

单元测试主要验证本地代码逻辑，例如响应解析、Token 缓存、签名计算、消息体构建、异常处理等。单元测试不应该依赖真实飞书应用和真实网络调用。

建议补充测试依赖。

```xml
<dependencies>
    <!-- Spring Boot 测试：包含 JUnit 5、Mockito、AssertJ 等测试能力 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

下面测试飞书响应解析工具，验证成功响应、普通异常和 Token 失效异常。

文件位置：`src/test/java/io/github/atengk/feishu/util/FeishuResponseUtilTest.java`

```java
package io.github.atengk.feishu.util;

import cn.hutool.json.JSONObject;
import io.github.atengk.feishu.exception.FeishuApiException;
import io.github.atengk.feishu.exception.FeishuTokenExpiredException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * 飞书响应工具测试
 *
 * @author Ateng
 * @since 2026-05-07
 */
class FeishuResponseUtilTest {

    /**
     * 测试解析成功响应
     */
    @Test
    void parseSuccessResponseWhenCodeIsZero() {
        String responseBody = "{\"code\":0,\"msg\":\"success\",\"data\":{\"message_id\":\"om_xxx\"}}";

        JSONObject data = FeishuResponseUtil.parseSuccessData(responseBody);

        Assertions.assertEquals("om_xxx", data.getStr("message_id"));
    }

    /**
     * 测试解析普通失败响应
     */
    @Test
    void parseSuccessResponseWhenCodeIsError() {
        String responseBody = "{\"code\":999999,\"msg\":\"request error\"}";

        FeishuApiException exception = Assertions.assertThrows(
                FeishuApiException.class,
                () -> FeishuResponseUtil.parseSuccessResponse(responseBody)
        );

        Assertions.assertEquals(999999, exception.getFeishuCode());
    }

    /**
     * 测试解析 Token 失效响应
     */
    @Test
    void parseSuccessResponseWhenTokenExpired() {
        String responseBody = "{\"code\":99991663,\"msg\":\"token expired\"}";

        Assertions.assertThrows(
                FeishuTokenExpiredException.class,
                () -> FeishuResponseUtil.parseSuccessResponse(responseBody)
        );
    }
}
```

下面测试回调签名工具，用于保证验签逻辑稳定。

文件位置：`src/test/java/io/github/atengk/feishu/util/FeishuCallbackSignatureUtilTest.java`

```java
package io.github.atengk.feishu.util;

import io.github.atengk.feishu.exception.FeishuCallbackSignatureException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * 飞书回调签名工具测试
 *
 * @author Ateng
 * @since 2026-05-07
 */
class FeishuCallbackSignatureUtilTest {

    /**
     * 测试签名不匹配时抛出异常
     */
    @Test
    void verifyWhenSignatureMismatch() {
        Assertions.assertThrows(
                FeishuCallbackSignatureException.class,
                () -> FeishuCallbackSignatureUtil.verify(
                        "1715000000",
                        "nonce",
                        "encryptKey",
                        "{\"encrypt\":\"xxx\"}",
                        "invalid-signature"
                )
        );
    }

    /**
     * 测试签名参数不完整时抛出异常
     */
    @Test
    void verifyWhenParamBlank() {
        Assertions.assertThrows(
                FeishuCallbackSignatureException.class,
                () -> FeishuCallbackSignatureUtil.verify(
                        "",
                        "nonce",
                        "encryptKey",
                        "{}",
                        "signature"
                )
        );
    }
}
```

执行单元测试命令如下：

```bash
# 执行全部测试
mvn test

# 执行指定测试类
mvn -Dtest=FeishuResponseUtilTest test

# 执行指定测试方法
mvn -Dtest=FeishuResponseUtilTest#parseSuccessResponseWhenCodeIsZero test
```

### 接口联调

接口联调用于验证 Spring Boot 服务与飞书开放平台的真实交互，包括 Token 获取、消息发送、用户查询、审批创建、事件回调等。联调环境建议使用独立的飞书测试应用，避免影响生产应用。

接口联调清单如下：

| 联调项         | 验证目标                               |
| -------------- | -------------------------------------- |
| Token 获取     | 验证 `app_id`、`app_secret` 是否正确   |
| 文本消息发送   | 验证机器人能力、消息权限、接收人 ID    |
| 富文本消息发送 | 验证消息格式是否符合飞书要求           |
| 卡片消息发送   | 验证卡片 JSON 是否有效                 |
| 用户查询       | 验证通讯录权限和用户 ID 类型           |
| 部门查询       | 验证部门权限和部门 ID 类型             |
| 审批创建       | 验证 `approval_code`、表单字段、发起人 |
| 审批查询       | 验证 `instance_code` 是否保存正确      |
| 事件回调       | 验证公网地址、验签、解密、分发         |
| 审批回调       | 验证审批定义订阅和本地状态同步         |

建议准备一份联调脚本，便于开发、测试和运维重复执行。

文件位置：`scripts/feishu-api-test.sh`

```bash
#!/usr/bin/env bash

# 飞书接口联调脚本
# 使用前先修改 BASE_URL、OPEN_ID、APPROVAL_CODE 等变量

set -e

BASE_URL="${BASE_URL:-http://localhost:8080}"
OPEN_ID="${OPEN_ID:-ou_xxxxxxxxxxxxx}"
APPROVAL_CODE="${APPROVAL_CODE:-replace-with-approval-code}"

echo "1. 检查服务健康状态"
curl -s "${BASE_URL}/api/feishu/event/health"
echo ""

echo "2. 发送飞书文本消息"
curl -s -X POST "${BASE_URL}/api/feishu/messages/text" \
  -H "Content-Type: application/json" \
  -d "{
    \"receiveId\": \"${OPEN_ID}\",
    \"receiveIdType\": \"open_id\",
    \"text\": \"飞书接口联调测试消息\"
  }"
echo ""

echo "3. 查询飞书用户信息"
curl -s "${BASE_URL}/api/feishu/users/${OPEN_ID}?userIdType=open_id"
echo ""

echo "4. 创建飞书审批实例"
curl -s -X POST "${BASE_URL}/api/feishu/approvals/instances" \
  -H "Content-Type: application/json" \
  -d "{
    \"businessId\": \"TEST-$(date +%Y%m%d%H%M%S)\",
    \"approvalCode\": \"${APPROVAL_CODE}\",
    \"openId\": \"${OPEN_ID}\",
    \"formItems\": [
      {
        \"id\": \"widget1\",
        \"type\": \"input\",
        \"value\": \"飞书审批联调测试\"
      }
    ]
  }"
echo ""
```

执行脚本：

```bash
chmod +x scripts/feishu-api-test.sh

BASE_URL="http://localhost:8080" \
OPEN_ID="ou_xxxxxxxxxxxxx" \
APPROVAL_CODE="replace-with-approval-code" \
./scripts/feishu-api-test.sh
```

接口联调建议记录以下信息：

| 信息         | 说明                        |
| ------------ | --------------------------- |
| 联调环境     | dev、test、pre、prod        |
| 飞书应用     | 应用名称、`app_id` 后 6 位  |
| 调用接口     | 本地接口路径和飞书接口路径  |
| 请求时间     | 方便查询日志                |
| `traceId`    | 本地日志链路 ID             |
| 飞书错误码   | 飞书响应中的 `code`         |
| 飞书错误消息 | 飞书响应中的 `msg`          |
| 业务单号     | 审批或消息对应的本地业务 ID |

### 飞书开放平台验证

飞书开放平台验证用于确认应用后台配置是否正确，包括应用权限、事件订阅、回调地址、机器人能力、审批定义订阅和应用版本发布状态。很多联调失败不是代码问题，而是飞书后台配置不完整。

验证清单如下：

| 验证项             | 说明                                         |
| ------------------ | -------------------------------------------- |
| 应用凭证           | `app_id` 和 `app_secret` 是否来自当前应用    |
| 应用能力           | 是否开启机器人、通讯录、审批、事件订阅等能力 |
| 接口权限           | 消息、通讯录、审批权限是否已申请             |
| 权限版本           | 权限变更后是否发布应用版本                   |
| 应用安装           | 应用是否安装到当前企业或目标用户范围         |
| 机器人可用范围     | 接收人是否在机器人可用范围内                 |
| 群聊权限           | 机器人是否已加入目标群聊                     |
| 回调地址           | 是否为公网可访问 HTTPS 地址                  |
| Verification Token | 与后端配置是否一致                           |
| Encrypt Key        | 与后端配置是否一致                           |
| 事件订阅           | 是否订阅对应事件类型                         |
| 审批定义订阅       | 是否订阅目标 `approval_code`                 |
| 事件日志           | 推送失败时查看事件日志详情                   |

飞书开放平台验证时，可以按以下顺序排查：

| 排查顺序 | 操作                                                 |
| -------- | ---------------------------------------------------- |
| 1        | 先验证 Token 获取是否成功                            |
| 2        | 再验证最简单的文本消息发送                           |
| 3        | 再验证用户查询和部门查询                             |
| 4        | 再验证审批实例创建                                   |
| 5        | 最后验证事件回调和审批回调                           |
| 6        | 回调失败时优先查看飞书开放平台事件日志               |
| 7        | 本地日志按 `traceId`、`eventId`、`instanceCode` 检索 |

## 部署说明

本章节用于说明 Spring Boot 飞书集成服务部署时的配置隔离、回调地址配置和生产环境注意事项。飞书集成服务通常涉及应用密钥、访问令牌、事件回调和审批状态同步，生产部署时需要重点关注安全性、可用性、日志审计和配置隔离。

### 配置隔离

配置隔离用于确保开发、测试、预发和生产环境使用不同的飞书应用配置，避免测试消息发送到生产用户，或者生产审批回调进入测试服务。

推荐环境划分如下：

| 环境     | 配置文件               | 飞书应用 | 说明                             |
| -------- | ---------------------- | -------- | -------------------------------- |
| 开发环境 | `application-dev.yml`  | 开发应用 | 开发人员本地调试使用             |
| 测试环境 | `application-test.yml` | 测试应用 | QA 联调和自动化测试使用          |
| 预发环境 | `application-pre.yml`  | 预发应用 | 与生产配置接近，但不影响真实业务 |
| 生产环境 | `application-prod.yml` | 生产应用 | 正式业务使用                     |

基础配置文件只放通用配置，敏感配置全部使用环境变量注入。

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  profiles:
    # 默认使用 dev，生产环境通过 SPRING_PROFILES_ACTIVE 覆盖
    active: ${SPRING_PROFILES_ACTIVE:dev}

  application:
    # 应用名称
    name: springboot-feishu-demo

server:
  # 服务端口
  port: ${SERVER_PORT:8080}
```

生产配置示例：

文件位置：`src/main/resources/application-prod.yml`

```yaml
spring:
  data:
    redis:
      # 生产 Redis 地址，从环境变量注入
      host: ${REDIS_HOST}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD}
      database: ${REDIS_DATABASE:0}

feishu:
  # 飞书 OpenAPI 地址
  base-url: https://open.feishu.cn

  # 生产飞书应用凭证，必须通过环境变量注入
  app-id: ${FEISHU_APP_ID}
  app-secret: ${FEISHU_APP_SECRET}

  # 事件订阅安全配置，必须与飞书开放平台生产应用一致
  verification-token: ${FEISHU_VERIFICATION_TOKEN}
  encrypt-key: ${FEISHU_ENCRYPT_KEY}

  # Token 提前过期时间，避免临界失效
  token-expire-offset-seconds: ${FEISHU_TOKEN_EXPIRE_OFFSET_SECONDS:300}

  # 生产回调地址路径
  callback-path: /api/feishu/event/callback

logging:
  level:
    # 生产环境不建议使用 DEBUG
    io.github.atengk.feishu: INFO
```

生产环境启动命令示例：

```bash
export SPRING_PROFILES_ACTIVE="prod"
export REDIS_HOST="redis.example.com"
export REDIS_PORT="6379"
export REDIS_PASSWORD="replace-with-redis-password"

export FEISHU_APP_ID="cli_xxxxxxxxxxxxx"
export FEISHU_APP_SECRET="replace-with-prod-secret"
export FEISHU_VERIFICATION_TOKEN="replace-with-prod-token"
export FEISHU_ENCRYPT_KEY="replace-with-prod-encrypt-key"

java -jar springboot-feishu-demo.jar
```

配置隔离建议如下：

| 配置项               | 建议                                         |
| -------------------- | -------------------------------------------- |
| `app_id`             | 各环境使用不同飞书应用                       |
| `app_secret`         | 只通过环境变量或密钥服务注入                 |
| `encrypt_key`        | 各环境独立配置                               |
| `verification_token` | 各环境独立配置                               |
| 回调域名             | 各环境使用不同域名                           |
| Redis key            | 必要时加环境前缀，例如 `prod:feishu:token:*` |
| 日志级别             | 生产环境使用 `INFO`，排障时临时调整          |
| 审批定义             | 测试和生产使用不同审批定义 Code              |

### 回调地址配置

回调地址配置用于让飞书开放平台把事件推送到部署后的服务。生产环境必须使用公网可访问的 HTTPS 域名，且路径需要与后端接口一致。

回调地址示例：

```text
https://api.example.com/api/feishu/event/callback
```

如果服务部署在 Nginx 后面，需要确保 Nginx 正确转发请求体和请求头。飞书验签依赖原始请求体和请求头，因此代理层不能修改请求体内容。

文件位置：`/etc/nginx/conf.d/springboot-feishu.conf`

```nginx
server {
    listen 443 ssl;
    server_name api.example.com;

    # SSL 证书配置
    ssl_certificate /etc/nginx/certs/api.example.com.pem;
    ssl_certificate_key /etc/nginx/certs/api.example.com.key;

    # 飞书事件回调接口
    location /api/feishu/event/callback {
        proxy_pass http://127.0.0.1:8080/api/feishu/event/callback;

        # 保留真实客户端信息
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # 保留飞书回调签名相关请求头
        proxy_set_header X-Lark-Request-Timestamp $http_x_lark_request_timestamp;
        proxy_set_header X-Lark-Request-Nonce $http_x_lark_request_nonce;
        proxy_set_header X-Lark-Signature $http_x_lark_signature;

        # 回调请求体不应过小，避免卡片或事件内容较大时被拒绝
        client_max_body_size 10m;

        # 回调接口应快速返回，超时时间不建议过长
        proxy_connect_timeout 5s;
        proxy_read_timeout 30s;
        proxy_send_timeout 30s;
    }

    # 其他业务接口
    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

应用 Nginx 配置：

```bash
# 检查 Nginx 配置语法
nginx -t

# 重新加载 Nginx 配置
nginx -s reload

# 验证回调健康检查接口
curl "https://api.example.com/api/feishu/event/health"
```

回调地址配置建议如下：

| 配置项   | 建议                                           |
| -------- | ---------------------------------------------- |
| 协议     | 生产环境必须使用 HTTPS                         |
| 域名     | 使用稳定域名，不建议使用临时穿透地址           |
| 路径     | 与 `${feishu.callback-path}` 完全一致          |
| 请求头   | 保留飞书签名相关请求头                         |
| 请求体   | 不要在网关层修改请求体                         |
| 超时时间 | 回调接口应快速返回，耗时业务异步处理           |
| 白名单   | 如企业安全要求，可按飞书出口 IP 或安全策略限制 |
| 健康检查 | 提供 `/api/feishu/event/health` 用于运维检查   |

### 生产环境注意事项

生产环境需要重点关注密钥安全、Token 缓存、接口限流、回调幂等、日志审计、监控告警和故障恢复。飞书集成服务一旦异常，可能影响消息通知、审批流转和组织架构同步，因此应作为关键外部依赖进行治理。

生产环境检查清单如下：

| 检查项     | 说明                                                         |
| ---------- | ------------------------------------------------------------ |
| 密钥安全   | `app_secret`、`encrypt_key`、Webhook `secret` 不写入代码仓库 |
| Token 缓存 | `tenant_access_token`、`app_access_token` 使用 Redis 缓存    |
| 提前过期   | Token TTL 小于飞书返回过期时间                               |
| 幂等处理   | 事件回调、审批回调按 `event_id` 或 `instance_code` 做幂等    |
| 超时控制   | 调用飞书接口必须设置连接和读取超时                           |
| 重试策略   | Token 失效可重试一次，业务接口失败不无限重试                 |
| 限流保护   | 对内部消息发送接口加限流，防止误调用刷屏                     |
| 权限最小化 | 飞书应用只申请必要权限                                       |
| 日志脱敏   | Token、手机号、邮箱、密钥必须脱敏                            |
| 告警监控   | 监控飞书接口失败率、回调失败数、Token 获取失败数             |
| 灰度发布   | 生产变更前先在测试或预发环境验证                             |
| 应急预案   | 准备关闭消息推送或回调处理的开关                             |

建议增加飞书集成开关配置，生产故障时可以临时关闭非核心推送。

```yaml
feishu:
  feature:
    # 是否启用消息推送
    message-enabled: ${FEISHU_MESSAGE_ENABLED:true}

    # 是否启用事件回调处理
    event-enabled: ${FEISHU_EVENT_ENABLED:true}

    # 是否启用审批集成
    approval-enabled: ${FEISHU_APPROVAL_ENABLED:true}
```

对应配置类可以按前文 `FeishuProperties` 扩展：

```java
/**
 * 功能开关配置
 */
private Feature feature = new Feature();

/**
 * 飞书功能开关
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Data
public static class Feature {

    /**
     * 是否启用消息推送
     */
    private Boolean messageEnabled = true;

    /**
     * 是否启用事件回调处理
     */
    private Boolean eventEnabled = true;

    /**
     * 是否启用审批集成
     */
    private Boolean approvalEnabled = true;
}
```

生产监控指标建议如下：

| 指标                             | 说明               |
| -------------------------------- | ------------------ |
| `feishu.token.get.success.count` | Token 获取成功次数 |
| `feishu.token.get.fail.count`    | Token 获取失败次数 |
| `feishu.api.call.count`          | 飞书接口调用次数   |
| `feishu.api.call.fail.count`     | 飞书接口失败次数   |
| `feishu.message.send.count`      | 消息发送次数       |
| `feishu.message.send.fail.count` | 消息发送失败次数   |
| `feishu.event.receive.count`     | 事件回调接收次数   |
| `feishu.event.handle.fail.count` | 事件处理失败次数   |
| `feishu.approval.create.count`   | 审批创建次数       |
| `feishu.approval.callback.count` | 审批回调次数       |

生产故障排查顺序建议如下：

| 顺序 | 排查内容                                             |
| ---- | ---------------------------------------------------- |
| 1    | 检查服务是否正常运行                                 |
| 2    | 检查 Redis 是否可用，Token 缓存是否正常              |
| 3    | 检查飞书应用凭证是否正确                             |
| 4    | 检查飞书应用权限是否被修改                           |
| 5    | 检查回调域名和 Nginx 转发是否正常                    |
| 6    | 检查飞书开放平台事件日志                             |
| 7    | 按 `traceId`、`eventId`、`instanceCode` 检索应用日志 |
| 8    | 检查是否触发限流、超时或网络异常                     |
| 9    | 必要时临时关闭非核心消息推送                         |
| 10   | 修复后重新执行接口联调脚本验证                       |

部署完成后，建议至少完成以下验证：

```bash
# 检查服务健康状态
curl "https://api.example.com/actuator/health"

# 检查飞书事件回调健康状态
curl "https://api.example.com/api/feishu/event/health"

# 验证消息发送接口
curl -X POST "https://api.example.com/api/feishu/messages/text" \
  -H "Content-Type: application/json" \
  -d '{
    "receiveId": "ou_xxxxxxxxxxxxx",
    "receiveIdType": "open_id",
    "text": "生产环境飞书集成验证消息"
  }'
```

生产环境最终上线前，应确认飞书开放平台中的生产应用已经完成权限申请、应用发布、应用安装、事件订阅、审批定义订阅和回调地址校验。这样可以避免代码发布成功但飞书侧配置不完整导致功能不可用。