# Sa-Token

本文档用于说明 Spring Boot 3 项目中集成 Sa-Token 的基础开发方案，重点覆盖登录认证、登录态校验、角色权限控制、Token 管理、Redis 持久化以及前后端分离交互等常见场景。

## 项目概述

本章用于说明 Sa-Token 在 Spring Boot 3 项目中的功能定位、适用场景和技术选型。Sa-Token 在项目中主要承担认证鉴权职责，业务系统仍然负责用户、角色、权限、菜单等基础数据的维护。

### 功能定位

Sa-Token 是一个 Java 权限认证框架，在 Spring Boot 3 项目中主要用于处理登录认证、Token 生成、登录态维护、权限校验、角色校验、Token 注销、多端登录控制等认证鉴权能力。

在本项目中，Sa-Token 的核心定位是：业务系统完成账号密码校验后，将登录状态交给 Sa-Token 管理；后续请求进入系统时，由 Sa-Token 根据 Token 判断用户是否已登录，并根据角色或权限码判断用户是否允许访问指定接口。

常见功能职责如下：

| 功能         | 说明                                               |
| ------------ | -------------------------------------------------- |
| 登录认证     | 用户账号密码校验通过后，调用 Sa-Token 写入登录状态 |
| Token 管理   | 生成、获取、续期、注销和校验 Token                 |
| 登录态校验   | 判断当前请求是否携带有效 Token                     |
| 用户上下文   | 在接口中获取当前登录用户 ID                        |
| 角色校验     | 判断当前用户是否拥有指定角色                       |
| 权限校验     | 判断当前用户是否拥有指定权限码                     |
| 注解鉴权     | 使用注解对 Controller 或接口方法进行权限控制       |
| 路由鉴权     | 通过拦截器统一控制接口访问权限                     |
| Redis 持久化 | 将登录态数据存储到 Redis，支持分布式部署           |

Sa-Token 不负责替代用户系统本身。用户表、角色表、权限表、菜单表等仍然由业务系统维护，Sa-Token 只负责在认证成功后管理登录状态，并在请求访问时完成鉴权判断。

### 适用场景

Sa-Token 适合用于前后端分离的 Spring Boot 3 后端服务，尤其适合需要快速实现登录认证、接口鉴权、角色权限控制和 Token 管理的系统。

典型适用场景如下：

| 场景           | 说明                                                |
| -------------- | --------------------------------------------------- |
| 后台管理系统   | 管理员登录后访问用户、角色、菜单、配置等管理接口    |
| 前后端分离项目 | 前端通过请求头携带 Token，后端根据 Token 判断登录态 |
| RBAC 权限系统  | 基于用户、角色、权限码实现接口级权限控制            |
| 多端登录控制   | 控制同一账号在 PC、APP、小程序等多个终端的登录策略  |
| 分布式部署     | 多个后端实例共享 Redis 中的登录态                   |
| 微服务系统     | 网关或业务服务统一校验 Token 和权限                 |
| 内部管理平台   | 对菜单权限、按钮权限和接口权限进行统一控制          |

以下场景需要结合实际情况调整方案：

| 场景            | 建议                                                         |
| --------------- | ------------------------------------------------------------ |
| 简单演示项目    | 可以先使用默认内存模式，暂不接入 Redis                       |
| 单体后台系统    | 可以直接在业务服务中完成登录认证和权限校验                   |
| 微服务系统      | 建议在网关层统一处理 Token 校验，业务服务按需二次鉴权        |
| 高安全等级系统  | 需要额外补充登录失败限制、操作审计、二次认证、风控策略等能力 |
| OAuth2 授权中心 | 需要单独设计客户端、授权码、Scope、回调地址等授权模型        |

### 技术选型

本项目采用 Spring Boot 3 + Sa-Token + Redis 的认证鉴权技术路线。Spring Boot 3 负责 Web 服务、配置管理和依赖管理；Sa-Token 负责登录认证和权限校验；Redis 负责登录态持久化；Hutool 用于简化字符串、集合、日期、加密等常见工具处理。

Spring Boot 3.x 官方系统要求中，Spring Boot 3.3.16 至少需要 Java 17，并明确支持 Maven 3.6.3 或更高版本；Sa-Token Spring Boot 3 Starter 在 Maven 仓库中可查到 1.45.0 版本。([Spring Docs](https://docs.enterprise.spring.io/spring-boot/system-requirements.html?utm_source=chatgpt.com))

| 技术        | 推荐版本            | 作用                           |
| ----------- | ------------------- | ------------------------------ |
| JDK         | 17 或 21            | Spring Boot 3 运行基础环境     |
| Spring Boot | 3.x                 | 项目基础框架                   |
| Sa-Token    | 1.45.0              | 登录认证、权限认证、Token 管理 |
| Maven       | 3.6.3+              | 项目构建和依赖管理             |
| Redis       | 6.x / 7.x           | 登录态和权限缓存持久化         |
| Hutool      | 5.8.x               | 常用 Java 工具类库             |
| Lombok      | 由 Spring Boot 管理 | 简化 DTO、VO、实体类等样板代码 |
| Jackson     | 由 Spring Boot 管理 | JSON 序列化和反序列化          |

整体技术组合如下：

| 模块       | 技术                      |
| ---------- | ------------------------- |
| Web 框架   | Spring Boot 3、Spring MVC |
| 认证鉴权   | Sa-Token                  |
| Token 存储 | Redis                     |
| 参数校验   | Spring Validation         |
| 工具类     | Hutool                    |
| 日志       | SLF4J + Logback           |
| 构建工具   | Maven                     |

该技术选型适合常规后台接口项目。若项目使用 Spring Cloud Gateway 或 WebFlux，应选择 Sa-Token 的 Reactor 版本，而不是普通 Spring MVC Starter。

## 环境准备

本章用于说明 Spring Boot 3 集成 Sa-Token 前需要准备的基础环境、版本选择原则和 Maven 依赖配置。后续登录接口、权限校验、Token 管理和 Redis 持久化都会基于本章配置继续展开。

### Spring Boot 3 基础环境

Spring Boot 3 项目建议统一使用 JDK 17 或更高版本，并使用 Maven 3.6.3 以上版本构建项目。由于 Spring Boot 3 已切换到 Jakarta EE 命名空间，项目中应避免继续使用 `javax.servlet`、`javax.validation` 等旧包名，相关代码和依赖应统一使用 `jakarta.*` 体系。

基础环境建议如下：

| 环境项     | 推荐配置           | 说明                             |
| ---------- | ------------------ | -------------------------------- |
| JDK        | 17 或 21           | Spring Boot 3 最低要求 Java 17   |
| Maven      | 3.6.3+             | 用于依赖管理、编译、测试和打包   |
| IDE        | IntelliJ IDEA      | 推荐开启 Lombok 插件和注解处理   |
| Redis      | 6.x / 7.x          | 用于后续 Sa-Token 登录态持久化   |
| 编码       | UTF-8              | 避免中文日志、配置和返回信息乱码 |
| 基础包路径 | `io.github.atengk` | 示例代码默认使用该包路径         |

验证本地 Java 和 Maven 环境：

```bash
java -version
mvn -v
```

`java -version` 用于确认当前 JDK 版本，应显示 17 或更高版本。`mvn -v` 用于确认 Maven 版本和 Maven 使用的 JDK 版本，建议 Maven 使用的 Java 版本与项目编译版本保持一致。

项目基础目录建议如下：

```text
springboot3-sa-token-demo
├── pom.xml
├── src
│   ├── main
│   │   ├── java
│   │   │   └── io
│   │   │       └── github
│   │   │           └── atengk
│   │   │               ├── SaTokenApplication.java
│   │   │               ├── common
│   │   │               ├── config
│   │   │               ├── controller
│   │   │               ├── service
│   │   │               └── security
│   │   └── resources
│   │       ├── application.yml
│   │       └── application-dev.yml
│   └── test
│       └── java
```

目录说明如下：

| 目录         | 说明                                                     |
| ------------ | -------------------------------------------------------- |
| `common`     | 存放统一返回结果、异常枚举、通用工具类                   |
| `config`     | 存放 Sa-Token 配置、拦截器配置、跨域配置                 |
| `controller` | 存放登录接口、鉴权测试接口和业务接口                     |
| `service`    | 存放用户认证、角色权限查询等业务逻辑                     |
| `security`   | 存放 Sa-Token 权限接口实现、登录用户上下文等安全相关代码 |
| `resources`  | 存放 `application.yml`、环境配置和其他资源文件           |

### Sa-Token 版本选择

Spring Boot 3 项目应使用 `sa-token-spring-boot3-starter`，不要使用面向 Spring Boot 2 的 `sa-token-spring-boot-starter`。如果项目使用的是常规 Spring MVC，也就是 `spring-boot-starter-web`，选择 `sa-token-spring-boot3-starter`；如果项目使用 WebFlux 或 Spring Cloud Gateway，则选择 `sa-token-reactor-spring-boot3-starter`。

Sa-Token Spring Boot 3 Starter 在 Maven 仓库中显示 1.45.0 版本，发布日期为 2026 年 3 月 8 日。([Maven Repository](https://mvnrepository.com/artifact/cn.dev33/sa-token-spring-boot3-starter/versions?utm_source=chatgpt.com))

版本选择建议如下：

| 项目类型                   | 推荐依赖                                | 说明                     |
| -------------------------- | --------------------------------------- | ------------------------ |
| Spring Boot 3 + Spring MVC | `sa-token-spring-boot3-starter`         | 常规后台接口项目推荐使用 |
| Spring Boot 3 + WebFlux    | `sa-token-reactor-spring-boot3-starter` | 响应式 Web 项目使用      |
| Spring Cloud Gateway       | `sa-token-reactor-spring-boot3-starter` | 网关鉴权场景使用         |
| Spring Boot 2              | `sa-token-spring-boot-starter`          | 不属于本文档范围         |
| Spring Boot 4              | `sa-token-spring-boot4-starter`         | 不属于本文档范围         |

本文档示例版本如下：

| 依赖        | 版本                   |
| ----------- | ---------------------- |
| Spring Boot | `3.3.x` 或项目统一版本 |
| Sa-Token    | `1.45.0`               |
| JDK         | `17`                   |
| Maven       | `3.6.3+`               |

版本管理建议如下：

1. 不建议使用 `LATEST`、`RELEASE` 或动态版本号，避免构建结果不可控。
2. Sa-Token 主依赖、Redis DAO 依赖、JWT 依赖等扩展依赖建议保持同一个版本号。
3. Spring Boot 相关依赖建议由 `spring-boot-starter-parent` 或 Spring Boot BOM 统一管理。
4. Hutool、Sa-Token 这类非 Spring Boot BOM 管理的依赖建议显式声明版本。
5. 生产项目升级 Sa-Token 前，应重点回归登录、鉴权、Token 续期、Token 注销和 Redis 持久化逻辑。

### Maven 依赖配置

下面给出 Spring Boot 3 + Spring MVC + Sa-Token 的基础 Maven 配置。该配置适合普通后台管理系统、前后端分离接口服务和 RBAC 权限系统。

文件位置：`pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">

    <modelVersion>4.0.0</modelVersion>

    <!-- Spring Boot 3 父工程：统一管理 Spring 相关依赖版本和 Maven 插件版本 -->
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.3.16</version>
        <relativePath/>
    </parent>

    <groupId>io.github.atengk</groupId>
    <artifactId>springboot3-sa-token-demo</artifactId>
    <version>1.0.0</version>
    <name>springboot3-sa-token-demo</name>
    <description>Spring Boot 3 集成 Sa-Token 示例项目</description>

    <properties>
        <!-- Spring Boot 3 最低要求 Java 17 -->
        <java.version>17</java.version>

        <!-- Sa-Token 版本：核心依赖、Redis DAO、JWT 等扩展依赖建议保持一致 -->
        <sa-token.version>1.45.0</sa-token.version>

        <!-- Hutool 工具类库版本：用于字符串、集合、日期、加密等常用处理 -->
        <hutool.version>5.8.44</hutool.version>
    </properties>

    <dependencies>
        <!-- Spring Web MVC：提供 Controller、REST 接口、内置 Tomcat 等能力 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- 参数校验：提供 jakarta.validation 注解支持，例如 @NotBlank、@NotNull -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <!-- Sa-Token Spring Boot 3 集成包：提供登录认证、权限认证、Token 管理等能力 -->
        <dependency>
            <groupId>cn.dev33</groupId>
            <artifactId>sa-token-spring-boot3-starter</artifactId>
            <version>${sa-token.version}</version>
        </dependency>

        <!-- Hutool 工具类库：用于简化字符串、集合、日期、加密等常见处理 -->
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
            <version>${hutool.version}</version>
        </dependency>

        <!-- Lombok：简化 DTO、VO、实体类、日志对象等样板代码 -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- 测试依赖：用于单元测试、接口测试和 Spring 上下文测试 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <!-- Spring Boot Maven 插件：用于打包可执行 Jar -->
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <!-- 打包时排除 Lombok，避免无意义依赖进入运行包 -->
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>

</project>
```

如果后续需要接入 Redis 持久化登录态，可以在上述依赖基础上增加 Redis 相关依赖。该部分也可以放到后续 `Redis 持久化集成` 章节中展开。

```xml
<!-- Spring Data Redis：提供 RedisTemplate、连接池自动配置等能力 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>

<!-- Sa-Token Redis DAO：将登录态、Token 数据等持久化到 Redis，使用 Jackson 序列化 -->
<dependency>
    <groupId>cn.dev33</groupId>
    <artifactId>sa-token-dao-redis-jackson</artifactId>
    <version>${sa-token.version}</version>
</dependency>
```

如果项目是 WebFlux 或 Spring Cloud Gateway，不要同时引入 `sa-token-spring-boot3-starter` 和 `sa-token-reactor-spring-boot3-starter`，应替换为 Reactor 版本。

```xml
<!-- Sa-Token Spring Boot 3 Reactor 集成包：适用于 WebFlux 和 Gateway 场景 -->
<dependency>
    <groupId>cn.dev33</groupId>
    <artifactId>sa-token-reactor-spring-boot3-starter</artifactId>
    <version>${sa-token.version}</version>
</dependency>
```

依赖添加完成后，可以执行以下命令验证依赖是否正常解析：

```bash
mvn clean compile
mvn dependency:tree -Dincludes=cn.dev33
```

`mvn clean compile` 用于清理并编译项目，确认 Maven 依赖、Java 版本和项目代码是否正常。`mvn dependency:tree -Dincludes=cn.dev33` 用于查看 Sa-Token 相关依赖是否正确引入，重点确认是否加载了 `sa-token-spring-boot3-starter` 以及版本是否符合预期。



## 基础配置

本章用于完成 Sa-Token 在 Spring Boot 3 项目中的基础接入配置，包括核心参数、Token 有效期、登录认证策略、跨域配置和接口拦截规则。Sa-Token 的核心登录 API 主要通过 `StpUtil` 使用，例如 `StpUtil.login()`、`StpUtil.checkLogin()`、`StpUtil.getTokenValue()`、`StpUtil.getTokenInfo()` 等。([Gitee](https://gitee.com/dromara/sa-token/blob/40b01aba00112b76caba1989f42919f5fbe49c38/sa-token-doc/api/stp-util.md?utm_source=chatgpt.com))

### Sa-Token 核心配置

Sa-Token 的核心配置建议统一放在 `application.yml` 中管理。常见配置包括 Token 名称、有效期、活跃超时时间、是否允许并发登录、是否共享 Token、Token 风格、是否从请求头读取 Token 等。

文件位置：`src/main/resources/application.yml`

```yaml
server:
  port: 8080

spring:
  application:
    name: springboot3-sa-token-demo

############## Sa-Token 配置 ##############
sa-token:
  # Token 名称，同时也是前端请求头名称
  token-name: Authorization

  # Token 有效期，单位：秒
  # 这里设置为 7 天，-1 代表永久有效
  timeout: 604800

  # Token 最低活跃时间，单位：秒
  # 如果超过该时间没有访问系统，则 Token 会被冻结
  # -1 代表不限制活跃时间
  active-timeout: 1800

  # 是否启用动态 active-timeout
  # 开启后可以在登录时通过 SaLoginModel 单独指定活跃时间
  dynamic-active-timeout: true

  # 是否自动续签 active-timeout
  # 用户访问系统时，自动刷新最后活跃时间
  auto-renew: true

  # 是否允许同一账号并发登录
  # true：允许多个端同时在线
  # false：新登录会挤掉旧登录
  is-concurrent: true

  # 多人登录同一账号时，是否共用一个 Token
  # true：同一账号多次登录共用一个 Token
  # false：每次登录生成新的 Token
  is-share: false

  # Token 风格
  # 常用值：uuid、simple-uuid、random-32、random-64、random-128、tik
  token-style: random-64

  # 是否从请求头读取 Token
  # 前后端分离项目通常开启
  is-read-header: true

  # 是否从 Cookie 读取 Token
  # 前后端分离项目通常关闭，避免受浏览器 Cookie 策略影响
  is-read-cookie: false

  # 是否从请求参数读取 Token
  # 一般不建议开启，避免 Token 出现在 URL 中
  is-read-body: false

  # Token 前缀
  # 前端请求头格式：Authorization: Bearer xxxxxx
  token-prefix: Bearer

  # 是否在初始化时打印 Sa-Token 标识
  is-print: true

  # 是否输出操作日志
  is-log: true
```

常用配置说明如下：

| 配置项                   | 说明                                               |
| ------------------------ | -------------------------------------------------- |
| `token-name`             | Token 名称，前后端分离项目通常使用 `Authorization` |
| `timeout`                | Token 总有效期，单位为秒                           |
| `active-timeout`         | Token 活跃有效期，超过该时间未访问会被冻结         |
| `dynamic-active-timeout` | 是否允许登录时动态指定活跃时间                     |
| `auto-renew`             | 是否自动续签活跃时间                               |
| `is-concurrent`          | 是否允许同一账号多端同时登录                       |
| `is-share`               | 同一账号多次登录是否共享同一个 Token               |
| `token-style`            | Token 生成风格                                     |
| `is-read-header`         | 是否从请求头读取 Token                             |
| `is-read-cookie`         | 是否从 Cookie 读取 Token                           |
| `is-read-body`           | 是否从请求参数读取 Token                           |
| `token-prefix`           | Token 前缀，常用 `Bearer`                          |
| `is-log`                 | 是否输出 Sa-Token 操作日志                         |

Sa-Token 官方配置示例中也包含 `token-name`、`timeout`、`active-timeout`、`is-concurrent`、`is-share`、`token-style`、`is-log` 等常见配置项。([Docfork](https://docfork.com/dromara/sa-token?utm_source=chatgpt.com))

### Token 生成与有效期配置

Token 生成和有效期控制主要由 `timeout`、`active-timeout`、`token-style`、`is-concurrent`、`is-share` 几个参数共同决定。固定有效期用于控制 Token 最长可用时间，活跃有效期用于控制用户长时间无操作后的会话冻结。

推荐配置策略如下：

| 场景         | `timeout` | `active-timeout` | 说明                                |
| ------------ | --------- | ---------------- | ----------------------------------- |
| 后台管理系统 | `604800`  | `1800`           | Token 7 天有效，30 分钟无操作冻结   |
| 内部系统     | `86400`   | `3600`           | Token 1 天有效，1 小时无操作冻结    |
| 移动端 App   | `2592000` | `-1`             | Token 30 天有效，不限制活跃时间     |
| 临时登录     | `7200`    | `1800`           | Token 2 小时有效，30 分钟无操作冻结 |
| 高安全系统   | `28800`   | `900`            | Token 8 小时有效，15 分钟无操作冻结 |

在登录时，也可以通过 `SaLoginModel` 对单次登录指定 Token 有效期、设备类型、是否持久 Cookie、活跃时间等参数。`StpUtil.login(10001, loginModel)` 是 Sa-Token 支持的登录方式之一。([Gitee](https://gitee.com/dromara/sa-token/blob/40b01aba00112b76caba1989f42919f5fbe49c38/sa-token-doc/api/stp-util.md?utm_source=chatgpt.com))

示例配置：

```java
SaLoginModel loginModel = new SaLoginModel()
        // 指定登录设备，用于多端登录控制
        .setDevice("PC")
        // 指定本次登录 Token 有效期，单位：秒
        .setTimeout(60 * 60 * 24 * 7)
        // 指定本次登录活跃有效期，单位：秒
        .setActiveTimeout(60 * 30);

StpUtil.login(userId, loginModel);
```

实际开发中建议遵循以下规则：

1. 管理端系统不要设置永久 Token。
2. 前后端分离项目建议从请求头读取 Token。
3. `token-prefix` 使用 `Bearer` 时，前端请求头需要按 `Bearer token值` 传递。
4. 如果启用 `active-timeout`，建议同时开启 `auto-renew`。
5. 如果需要同端互斥登录，登录时必须明确指定设备类型，例如 `PC`、`APP`、`MINI`。

### 登录认证配置

登录认证配置主要分为两部分：一是 Sa-Token 的全局参数配置，二是登录接口中的认证逻辑。Sa-Token 不负责校验账号密码，账号密码应由业务系统自行校验；校验通过后，再调用 `StpUtil.login()` 写入登录态。

登录认证流程如下：

| 步骤 | 说明                                                         |
| ---- | ------------------------------------------------------------ |
| 1    | 前端提交账号、密码、设备类型等登录参数                       |
| 2    | 后端校验参数格式                                             |
| 3    | 根据账号查询用户信息                                         |
| 4    | 校验密码是否正确                                             |
| 5    | 校验用户状态是否正常                                         |
| 6    | 调用 `StpUtil.login()` 写入登录状态                          |
| 7    | 调用 `StpUtil.getTokenValue()` 或 `StpUtil.getTokenInfo()` 获取 Token 信息 |
| 8    | 返回 Token、用户信息和过期时间给前端                         |

登录认证时建议区分以下错误：

| 异常场景       | 返回说明               |
| -------------- | ---------------------- |
| 账号为空       | `账号不能为空`         |
| 密码为空       | `密码不能为空`         |
| 账号不存在     | `账号或密码错误`       |
| 密码错误       | `账号或密码错误`       |
| 用户被禁用     | `账号已被禁用`         |
| 登录态写入失败 | `登录失败，请稍后重试` |

为了避免暴露账号是否存在，生产环境中账号不存在和密码错误建议统一返回 `账号或密码错误`。

### 跨域与拦截器配置

前后端分离项目通常需要同时配置跨域和 Sa-Token 拦截器。跨域配置负责允许浏览器访问后端接口，Sa-Token 拦截器负责校验登录态和注解鉴权。

Sa-Token 从 1.31.0 开始提供 `SaInterceptor` 综合拦截器，可同时支持注解鉴权和路由鉴权；如果使用带认证函数的 `SaInterceptor`，需要手动补充登录校验规则。([电脑学习网](https://www.yuucn.com/a/1557811.html?utm_source=chatgpt.com))

当前章节涉及的文件结构如下：

```text
src/main/java/io/github/atengk
├── config
│   └── SaTokenConfig.java
├── common
│   └── result
│       └── Result.java
├── controller
│   └── AuthController.java
├── service
│   └── AuthService.java
├── service
│   └── impl
│       └── AuthServiceImpl.java
├── dto
│   └── LoginRequest.java
└── vo
    └── LoginResponse.java
```

下面的配置类用于注册跨域规则和 Sa-Token 拦截器，登录接口、错误接口、静态资源接口和接口文档接口默认放行。

文件位置：`src/main/java/io/github/atengk/config/SaTokenConfig.java`

```java
package io.github.atengk.config;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaHttpMethod;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.collection.CollUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Sa-Token 基础配置
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    /**
     * 白名单接口
     */
    private static final List<String> WHITE_LIST = CollUtil.newArrayList(
            "/auth/login",
            "/auth/logout",
            "/error",
            "/favicon.ico",
            "/doc.html",
            "/webjars/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**"
    );

    /**
     * 注册 Sa-Token 拦截器
     *
     * @param registry 拦截器注册器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handler -> {
                    // 放行浏览器跨域预检请求
                    SaRouter.match(SaHttpMethod.OPTIONS)
                            .free(r -> log.debug("放行跨域预检请求：{}", SaHolder.getRequest().getRequestPath()))
                            .back();

                    // 除白名单外，其他接口均需要登录
                    SaRouter.match("/**")
                            .notMatch(WHITE_LIST)
                            .check(r -> StpUtil.checkLogin());
                }))
                .addPathPatterns("/**");
    }

    /**
     * 配置跨域规则
     *
     * @param registry 跨域注册器
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                // 生产环境建议替换为前端实际域名，例如 https://admin.example.com
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Authorization")
                .allowCredentials(false)
                .maxAge(3600);
    }
}
```

配置说明如下：

| 配置                                         | 说明                                   |
| -------------------------------------------- | -------------------------------------- |
| `WHITE_LIST`                                 | 不需要登录即可访问的接口               |
| `SaRouter.match(SaHttpMethod.OPTIONS)`       | 放行跨域预检请求                       |
| `SaRouter.match("/**").notMatch(WHITE_LIST)` | 拦截除白名单外的所有接口               |
| `StpUtil.checkLogin()`                       | 校验当前请求是否已登录                 |
| `allowedOriginPatterns("*")`                 | 允许跨域来源，生产环境建议改为具体域名 |
| `exposedHeaders("Authorization")`            | 允许前端读取响应头中的 `Authorization` |

注意：如果项目使用了 `server.servlet.context-path`，拦截器中的路径不需要包含 context path。例如上下文路径为 `/api` 时，登录接口仍然写 `/auth/login`，不要写 `/api/auth/login`。

## 用户登录实现

本章用于实现一个完整的登录接口，包括登录接口设计、请求参数校验、账号密码认证、登录状态写入和登录结果返回。示例使用内存用户模拟数据库查询，实际项目中应替换为 MyBatis-Plus、JPA 或其他持久层查询逻辑。

### 登录接口设计

登录接口用于接收前端提交的账号、密码、是否记住我和设备类型。后端认证成功后，返回 Token 信息、Token 名称、Token 前缀、用户基础信息和过期时间。

接口设计如下：

| 项目         | 内容                    |
| ------------ | ----------------------- |
| 请求路径     | `/auth/login`           |
| 请求方法     | `POST`                  |
| 请求类型     | `application/json`      |
| 是否需要登录 | 否                      |
| 返回类型     | `Result<LoginResponse>` |

请求参数如下：

| 参数         | 类型      | 必填 | 说明                               |
| ------------ | --------- | ---- | ---------------------------------- |
| `username`   | `String`  | 是   | 登录账号                           |
| `password`   | `String`  | 是   | 登录密码                           |
| `rememberMe` | `Boolean` | 否   | 是否记住登录状态                   |
| `device`     | `String`  | 否   | 登录设备，例如 `PC`、`APP`、`MINI` |

请求示例：

```json
{
  "username": "admin",
  "password": "123456",
  "rememberMe": true,
  "device": "PC"
}
```

返回示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "userId": 1,
    "username": "admin",
    "nickname": "系统管理员",
    "tokenName": "Authorization",
    "tokenPrefix": "Bearer",
    "tokenValue": "0f985e9f3d0b4a7d9b75e7a6d7f3d1c2",
    "authorization": "Bearer 0f985e9f3d0b4a7d9b75e7a6d7f3d1c2",
    "tokenTimeout": 604800,
    "device": "PC"
  }
}
```

### 登录参数校验

登录参数校验建议使用 Spring Validation 完成基础格式校验。Controller 层使用 `@Valid` 触发校验，DTO 中使用 `@NotBlank`、`@Size` 等注解约束字段。

下面的请求 DTO 用于接收登录参数。

文件位置：`src/main/java/io/github/atengk/dto/LoginRequest.java`

```java
package io.github.atengk.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 登录请求参数
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Data
public class LoginRequest {

    /**
     * 登录账号
     */
    @NotBlank(message = "账号不能为空")
    @Size(max = 50, message = "账号长度不能超过50个字符")
    private String username;

    /**
     * 登录密码
     */
    @NotBlank(message = "密码不能为空")
    @Size(max = 100, message = "密码长度不能超过100个字符")
    private String password;

    /**
     * 是否记住我
     */
    private Boolean rememberMe = false;

    /**
     * 登录设备
     */
    private String device = "PC";
}
```

校验规则说明如下：

| 字段         | 校验规则                | 说明                     |
| ------------ | ----------------------- | ------------------------ |
| `username`   | 不能为空，最大 50 字符  | 防止空账号和异常长字符串 |
| `password`   | 不能为空，最大 100 字符 | 防止空密码和异常长字符串 |
| `rememberMe` | 可为空                  | 为空时按 `false` 处理    |
| `device`     | 可为空                  | 为空时按 `PC` 处理       |

### 账号密码认证

账号密码认证属于业务系统职责。Sa-Token 不直接处理账号密码，只在认证成功后维护登录态。示例中使用 Hutool 的 `BCrypt` 进行密码校验，实际项目建议从数据库读取用户密码哈希后再校验。

下面的 `LoginResponse` 用于封装登录成功后的返回数据。

文件位置：`src/main/java/io/github/atengk/vo/LoginResponse.java`

```java
package io.github.atengk.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 登录响应结果
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Data
@Builder
public class LoginResponse {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 登录账号
     */
    private String username;

    /**
     * 用户昵称
     */
    private String nickname;

    /**
     * Token 请求头名称
     */
    private String tokenName;

    /**
     * Token 前缀
     */
    private String tokenPrefix;

    /**
     * Token 值
     */
    private String tokenValue;

    /**
     * 完整认证值
     */
    private String authorization;

    /**
     * Token 剩余有效期，单位：秒
     */
    private Long tokenTimeout;

    /**
     * 登录设备
     */
    private String device;
}
```

下面的统一返回结果用于封装接口响应。

文件位置：`src/main/java/io/github/atengk/common/result/Result.java`

```java
package io.github.atengk.common.result;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一接口返回结果
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

    /**
     * 状态码
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
     * 操作成功
     *
     * @param data 返回数据
     * @return 统一结果
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "操作成功", data);
    }

    /**
     * 操作失败
     *
     * @param message 失败消息
     * @return 统一结果
     */
    public static <T> Result<T> fail(String message) {
        return new Result<>(500, message, null);
    }
}
```

下面定义登录服务接口。

文件位置：`src/main/java/io/github/atengk/service/AuthService.java`

```java
package io.github.atengk.service;

import io.github.atengk.dto.LoginRequest;
import io.github.atengk.vo.LoginResponse;

/**
 * 登录认证服务
 *
 * @author Ateng
 * @since 2026-05-05
 */
public interface AuthService {

    /**
     * 用户登录
     *
     * @param request 登录请求参数
     * @return 登录响应结果
     */
    LoginResponse login(LoginRequest request);

    /**
     * 用户退出登录
     */
    void logout();
}
```

下面的服务实现类完成账号查询、密码校验、登录态写入和 Token 信息返回。示例中的用户数据使用内存模拟，后续可以替换为数据库查询。

文件位置：`src/main/java/io/github/atengk/service/impl/AuthServiceImpl.java`

```java
package io.github.atengk.service.impl;

import cn.dev33.satoken.stp.SaLoginModel;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.BCrypt;
import io.github.atengk.dto.LoginRequest;
import io.github.atengk.service.AuthService;
import io.github.atengk.vo.LoginResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 登录认证服务实现
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    /**
     * 示例用户数据，实际项目中应替换为数据库查询
     */
    private static final Map<String, DemoUser> USER_MAP = new HashMap<>();

    static {
        USER_MAP.put("admin", new DemoUser(
                1L,
                "admin",
                "系统管理员",
                BCrypt.hashpw("123456", BCrypt.gensalt()),
                true
        ));
    }

    /**
     * 用户登录
     *
     * @param request 登录请求参数
     * @return 登录响应结果
     */
    @Override
    public LoginResponse login(LoginRequest request) {
        String username = StrUtil.trim(request.getUsername());
        String password = request.getPassword();
        String device = StrUtil.blankToDefault(request.getDevice(), "PC");

        DemoUser user = USER_MAP.get(username);
        if (Objects.isNull(user)) {
            log.warn("用户登录失败，账号不存在：{}", username);
            throw new IllegalArgumentException("账号或密码错误");
        }

        if (!user.getEnabled()) {
            log.warn("用户登录失败，账号已禁用：{}", username);
            throw new IllegalArgumentException("账号已被禁用");
        }

        if (!BCrypt.checkpw(password, user.getPasswordHash())) {
            log.warn("用户登录失败，密码错误：{}", username);
            throw new IllegalArgumentException("账号或密码错误");
        }

        SaLoginModel loginModel = buildLoginModel(request, device);

        // 写入 Sa-Token 登录态
        StpUtil.login(user.getUserId(), loginModel);

        String tokenName = StpUtil.getTokenName();
        String tokenValue = StpUtil.getTokenValue();
        long tokenTimeout = StpUtil.getTokenTimeout();

        log.info("用户登录成功，用户ID：{}，账号：{}，设备：{}", user.getUserId(), username, device);

        return LoginResponse.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .tokenName(tokenName)
                .tokenPrefix("Bearer")
                .tokenValue(tokenValue)
                .authorization(StrUtil.format("Bearer {}", tokenValue))
                .tokenTimeout(tokenTimeout)
                .device(device)
                .build();
    }

    /**
     * 用户退出登录
     */
    @Override
    public void logout() {
        Object loginId = StpUtil.getLoginId();
        StpUtil.logout();
        log.info("用户退出登录成功，用户ID：{}", loginId);
    }

    /**
     * 构建登录参数
     *
     * @param request 登录请求参数
     * @param device  登录设备
     * @return Sa-Token 登录参数
     */
    private SaLoginModel buildLoginModel(LoginRequest request, String device) {
        SaLoginModel loginModel = new SaLoginModel()
                .setDevice(device)
                .setActiveTimeout(60 * 30);

        if (BooleanUtil.isTrue(request.getRememberMe())) {
            // 记住我：Token 7 天有效
            loginModel.setTimeout(60 * 60 * 24 * 7);
        } else {
            // 非记住我：Token 2 小时有效
            loginModel.setTimeout(60 * 60 * 2);
        }

        return loginModel;
    }

    /**
     * 示例用户对象
     *
     * @author Ateng
     * @since 2026-05-05
     */
    @Data
    @AllArgsConstructor
    private static class DemoUser {

        /**
         * 用户ID
         */
        private Long userId;

        /**
         * 登录账号
         */
        private String username;

        /**
         * 用户昵称
         */
        private String nickname;

        /**
         * 密码哈希
         */
        private String passwordHash;

        /**
         * 是否启用
         */
        private Boolean enabled;
    }
}
```

### 登录状态写入

登录状态写入的核心代码是 `StpUtil.login(userId, loginModel)`。执行成功后，Sa-Token 会为当前会话生成 Token，并将登录态写入当前 Sa-Token 存储中。如果后续接入 Redis DAO，登录态会持久化到 Redis。

核心逻辑如下：

```java
SaLoginModel loginModel = new SaLoginModel()
        .setDevice("PC")
        .setTimeout(60 * 60 * 24 * 7)
        .setActiveTimeout(60 * 30);

StpUtil.login(userId, loginModel);

String tokenName = StpUtil.getTokenName();
String tokenValue = StpUtil.getTokenValue();
long tokenTimeout = StpUtil.getTokenTimeout();
```

相关方法说明如下：

| 方法                                | 说明                            |
| ----------------------------------- | ------------------------------- |
| `StpUtil.login(userId)`             | 使用默认参数登录                |
| `StpUtil.login(userId, loginModel)` | 使用自定义登录参数登录          |
| `StpUtil.getTokenName()`            | 获取 Token 名称                 |
| `StpUtil.getTokenValue()`           | 获取当前请求上下文中的 Token 值 |
| `StpUtil.getTokenInfo()`            | 获取当前 Token 的详细信息       |
| `StpUtil.getTokenTimeout()`         | 获取当前 Token 剩余有效时间     |

Sa-Token 文档中列出的 `StpUtil` 登录、注销、会话查询、Token 有效期、权限认证等方法，可以作为后续章节继续扩展 Token 管理和权限控制的基础。([Gitee](https://gitee.com/dromara/sa-token/blob/40b01aba00112b76caba1989f42919f5fbe49c38/sa-token-doc/api/stp-util.md?utm_source=chatgpt.com))

### 登录结果返回

Controller 层负责接收登录请求、触发参数校验、调用登录服务并返回统一结果。

文件位置：`src/main/java/io/github/atengk/controller/AuthController.java`

```java
package io.github.atengk.controller;

import io.github.atengk.common.result.Result;
import io.github.atengk.dto.LoginRequest;
import io.github.atengk.service.AuthService;
import io.github.atengk.vo.LoginResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 登录认证接口
 *
 * @author Ateng
 * @since 2026-05-05
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    /**
     * 用户登录
     *
     * @param request 登录请求参数
     * @return 登录响应结果
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(authService.login(request));
    }

    /**
     * 用户退出登录
     *
     * @return 退出结果
     */
    @PostMapping("/logout")
    public Result<Void> logout() {
        authService.logout();
        return Result.success(null);
    }
}
```

接口调用示例：

```bash
curl -X POST "http://localhost:8080/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "123456",
    "rememberMe": true,
    "device": "PC"
  }'
```

登录成功后，前端需要保存响应中的 `authorization` 或 `tokenValue`。如果后端配置了 `token-prefix: Bearer`，后续请求建议按以下方式传递：

```bash
curl -X GET "http://localhost:8080/user/current" \
  -H "Authorization: Bearer 登录后返回的tokenValue"
```

前端处理建议如下：

| 字段            | 用途                                     |
| --------------- | ---------------------------------------- |
| `tokenName`     | 请求头名称，通常是 `Authorization`       |
| `tokenPrefix`   | Token 前缀，通常是 `Bearer`              |
| `tokenValue`    | 原始 Token 值                            |
| `authorization` | 可直接放入请求头的完整认证值             |
| `tokenTimeout`  | Token 剩余有效期，可用于前端登录状态判断 |
| `device`        | 当前登录设备                             |

至此，登录接口已经具备完整的参数接收、参数校验、账号密码认证、登录态写入和登录结果返回能力。后续章节可以继续在此基础上实现当前登录用户获取、登录状态判断、注解鉴权、路由鉴权、角色权限控制和 Redis 持久化。



## 登录态校验

本章用于说明用户登录成功后，后端如何获取当前登录用户、判断登录状态、使用注解完成接口鉴权，以及通过路由拦截器统一保护接口。登录态校验是后续角色权限控制、Token 管理和前后端交互的基础。

### 获取当前登录用户

获取当前登录用户时，通常不建议前端直接传递用户 ID，而是由后端根据当前请求中的 Token 解析登录态，再通过 `StpUtil.getLoginIdAsLong()` 获取当前登录用户 ID。这样可以避免用户伪造请求参数访问其他用户数据。

当前章节新增文件结构如下：

```text
src/main/java/io/github/atengk
├── controller
│   └── UserController.java
├── security
│   ├── LoginUserContext.java
│   └── SecurityConstant.java
└── vo
    ├── CurrentUserVO.java
    └── LoginStatusVO.java
```

下面的安全常量类用于统一管理角色编码、权限编码和设备类型，避免在代码中散落硬编码字符串。

文件位置：`src/main/java/io/github/atengk/security/SecurityConstant.java`

```java
package io.github.atengk.security;

/**
 * 安全权限常量
 *
 * @author Ateng
 * @since 2026-05-05
 */
public final class SecurityConstant {

    /**
     * 管理员角色
     */
    public static final String ROLE_ADMIN = "admin";

    /**
     * 普通用户角色
     */
    public static final String ROLE_USER = "user";

    /**
     * 用户查询权限
     */
    public static final String PERMISSION_USER_QUERY = "system:user:query";

    /**
     * 用户新增权限
     */
    public static final String PERMISSION_USER_ADD = "system:user:add";

    /**
     * 用户修改权限
     */
    public static final String PERMISSION_USER_UPDATE = "system:user:update";

    /**
     * 用户删除权限
     */
    public static final String PERMISSION_USER_DELETE = "system:user:delete";

    /**
     * 管理端设备
     */
    public static final String DEVICE_PC = "PC";

    private SecurityConstant() {
    }
}
```

下面的上下文工具类用于从 Sa-Token 登录态中获取当前登录用户 ID，并提供登录状态判断方法。

文件位置：`src/main/java/io/github/atengk/security/LoginUserContext.java`

```java
package io.github.atengk.security;

import cn.dev33.satoken.stp.StpUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 当前登录用户上下文
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
public final class LoginUserContext {

    private LoginUserContext() {
    }

    /**
     * 获取当前登录用户ID
     *
     * @return 当前登录用户ID
     */
    public static Long getUserId() {
        Long userId = StpUtil.getLoginIdAsLong();
        log.debug("获取当前登录用户ID：{}", userId);
        return userId;
    }

    /**
     * 获取当前登录用户ID，未登录时返回空
     *
     * @return 当前登录用户ID
     */
    public static Long getUserIdOrNull() {
        if (!StpUtil.isLogin()) {
            return null;
        }
        return StpUtil.getLoginIdAsLong();
    }

    /**
     * 判断当前请求是否已登录
     *
     * @return true：已登录，false：未登录
     */
    public static boolean isLogin() {
        return StpUtil.isLogin();
    }

    /**
     * 校验当前请求必须已登录
     */
    public static void checkLogin() {
        StpUtil.checkLogin();
    }
}
```

下面的 VO 用于返回当前登录用户信息。

文件位置：`src/main/java/io/github/atengk/vo/CurrentUserVO.java`

```java
package io.github.atengk.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 当前登录用户信息
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Data
@Builder
public class CurrentUserVO {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 登录账号
     */
    private String username;

    /**
     * 用户昵称
     */
    private String nickname;

    /**
     * 角色编码列表
     */
    private List<String> roles;

    /**
     * 权限编码列表
     */
    private List<String> permissions;
}
```

下面的 Controller 用于演示如何获取当前登录用户。实际项目中，用户基础信息应根据 `userId` 查询数据库，这里为了保持示例独立，使用模拟数据返回。

文件位置：`src/main/java/io/github/atengk/controller/UserController.java`

```java
package io.github.atengk.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.collection.CollUtil;
import io.github.atengk.common.result.Result;
import io.github.atengk.security.LoginUserContext;
import io.github.atengk.security.SecurityConstant;
import io.github.atengk.vo.CurrentUserVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户信息接口
 *
 * @author Ateng
 * @since 2026-05-05
 */
@RestController
@RequestMapping("/user")
public class UserController {

    /**
     * 获取当前登录用户
     *
     * @return 当前登录用户信息
     */
    @SaCheckLogin
    @GetMapping("/current")
    public Result<CurrentUserVO> currentUser() {
        Long userId = LoginUserContext.getUserId();

        CurrentUserVO currentUser = CurrentUserVO.builder()
                .userId(userId)
                .username("admin")
                .nickname("系统管理员")
                .roles(StpUtil.getRoleList())
                .permissions(StpUtil.getPermissionList())
                .build();

        return Result.success(currentUser);
    }

    /**
     * 获取当前用户简要信息
     *
     * @return 当前用户简要信息
     */
    @SaCheckLogin
    @GetMapping("/profile")
    public Result<CurrentUserVO> profile() {
        Long userId = LoginUserContext.getUserId();

        CurrentUserVO currentUser = CurrentUserVO.builder()
                .userId(userId)
                .username("admin")
                .nickname("系统管理员")
                .roles(CollUtil.newArrayList(SecurityConstant.ROLE_ADMIN))
                .permissions(CollUtil.newArrayList(SecurityConstant.PERMISSION_USER_QUERY))
                .build();

        return Result.success(currentUser);
    }
}
```

调用示例：

```bash
curl -X GET "http://localhost:8080/user/current" \
  -H "Authorization: Bearer 登录后返回的tokenValue"
```

如果 Token 有效，接口返回当前用户信息；如果 Token 无效或未传递 Token，Sa-Token 会抛出未登录异常。后续可在 `全局异常处理` 章节统一处理未登录、无权限和 Token 异常。

### 判断登录状态

判断登录状态可以分为两种方式：一种是 `StpUtil.isLogin()`，用于返回布尔值；另一种是 `StpUtil.checkLogin()`，用于强制校验登录状态，未登录时直接抛出异常。前者适合状态查询接口，后者适合业务接口访问控制。

下面的 VO 用于返回登录状态信息。

文件位置：`src/main/java/io/github/atengk/vo/LoginStatusVO.java`

```java
package io.github.atengk.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 登录状态信息
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Data
@Builder
public class LoginStatusVO {

    /**
     * 是否已登录
     */
    private Boolean login;

    /**
     * 当前登录用户ID
     */
    private Long userId;

    /**
     * Token 名称
     */
    private String tokenName;

    /**
     * Token 值
     */
    private String tokenValue;

    /**
     * Token 剩余有效期，单位：秒
     */
    private Long tokenTimeout;

    /**
     * 当前登录设备
     */
    private String loginDevice;
}
```

可以在登录认证 Controller 中增加登录状态查询接口。

文件位置：`src/main/java/io/github/atengk/controller/AuthController.java`

```java
package io.github.atengk.controller;

import cn.dev33.satoken.stp.StpUtil;
import io.github.atengk.common.result.Result;
import io.github.atengk.dto.LoginRequest;
import io.github.atengk.service.AuthService;
import io.github.atengk.vo.LoginResponse;
import io.github.atengk.vo.LoginStatusVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 登录认证接口
 *
 * @author Ateng
 * @since 2026-05-05
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    /**
     * 用户登录
     *
     * @param request 登录请求参数
     * @return 登录响应结果
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(authService.login(request));
    }

    /**
     * 用户退出登录
     *
     * @return 退出结果
     */
    @PostMapping("/logout")
    public Result<Void> logout() {
        authService.logout();
        return Result.success(null);
    }

    /**
     * 获取登录状态
     *
     * @return 登录状态信息
     */
    @GetMapping("/status")
    public Result<LoginStatusVO> status() {
        boolean login = StpUtil.isLogin();

        LoginStatusVO status = LoginStatusVO.builder()
                .login(login)
                .userId(login ? StpUtil.getLoginIdAsLong() : null)
                .tokenName(StpUtil.getTokenName())
                .tokenValue(login ? StpUtil.getTokenValue() : null)
                .tokenTimeout(login ? StpUtil.getTokenTimeout() : null)
                .loginDevice(login ? StpUtil.getLoginDevice() : null)
                .build();

        return Result.success(status);
    }

    /**
     * 校验当前请求必须已登录
     *
     * @return 校验结果
     */
    @GetMapping("/check-login")
    public Result<String> checkLogin() {
        StpUtil.checkLogin();
        return Result.success("当前请求已登录");
    }
}
```

接口调用示例：

```bash
curl -X GET "http://localhost:8080/auth/status" \
  -H "Authorization: Bearer 登录后返回的tokenValue"
```

`/auth/status` 适合前端初始化时判断本地 Token 是否仍然有效；`/auth/check-login` 适合调试登录态校验是否生效。Sa-Token 的常用注解包括 `@SaCheckLogin`、`@SaCheckPermission`、`@SaCheckRole` 等，代码中也可以通过 `StpUtil` 直接进行登录、权限和角色校验。([Jeecg 文档中心](https://help.jeecg.com/java/system/auth/satoken-annotations?utm_source=chatgpt.com))

### 注解鉴权使用

注解鉴权适合用于 Controller 层接口权限控制，可以把鉴权逻辑从业务代码中剥离出来。常用注解包括 `@SaCheckLogin`、`@SaCheckRole`、`@SaCheckPermission`，其中 `@SaCheckLogin` 用于登录校验，`@SaCheckRole` 用于角色校验，`@SaCheckPermission` 用于权限码校验。([Jeecg 文档中心](https://help.jeecg.com/java/system/auth/satoken-annotations?utm_source=chatgpt.com))

下面的接口用于演示注解鉴权。

文件位置：`src/main/java/io/github/atengk/controller/PermissionTestController.java`

```java
package io.github.atengk.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import io.github.atengk.common.result.Result;
import io.github.atengk.security.SecurityConstant;
import org.springframework.web.bind.annotation.*;

/**
 * 权限测试接口
 *
 * @author Ateng
 * @since 2026-05-05
 */
@RestController
@RequestMapping("/permission-test")
public class PermissionTestController {

    /**
     * 仅要求登录
     *
     * @return 测试结果
     */
    @SaCheckLogin
    @GetMapping("/login")
    public Result<String> checkLogin() {
        return Result.success("已登录用户可以访问");
    }

    /**
     * 要求管理员角色
     *
     * @return 测试结果
     */
    @SaCheckRole(SecurityConstant.ROLE_ADMIN)
    @GetMapping("/admin")
    public Result<String> checkAdminRole() {
        return Result.success("管理员角色可以访问");
    }

    /**
     * 要求用户查询权限
     *
     * @return 测试结果
     */
    @SaCheckPermission(SecurityConstant.PERMISSION_USER_QUERY)
    @GetMapping("/user-query")
    public Result<String> checkUserQueryPermission() {
        return Result.success("拥有用户查询权限可以访问");
    }

    /**
     * 要求同时拥有用户新增和用户修改权限
     *
     * @return 测试结果
     */
    @SaCheckPermission(
            value = {
                    SecurityConstant.PERMISSION_USER_ADD,
                    SecurityConstant.PERMISSION_USER_UPDATE
            },
            mode = SaMode.AND
    )
    @PostMapping("/user-save")
    public Result<String> checkUserSavePermission() {
        return Result.success("同时拥有用户新增和用户修改权限可以访问");
    }

    /**
     * 拥有用户新增或用户修改任一权限即可访问
     *
     * @return 测试结果
     */
    @SaCheckPermission(
            value = {
                    SecurityConstant.PERMISSION_USER_ADD,
                    SecurityConstant.PERMISSION_USER_UPDATE
            },
            mode = SaMode.OR
    )
    @PostMapping("/user-edit")
    public Result<String> checkUserEditPermission() {
        return Result.success("拥有用户新增或用户修改任一权限即可访问");
    }
}
```

注解使用说明如下：

| 注解                                      | 说明                          |
| ----------------------------------------- | ----------------------------- |
| `@SaCheckLogin`                           | 当前请求必须已登录            |
| `@SaCheckRole("admin")`                   | 当前用户必须拥有 `admin` 角色 |
| `@SaCheckPermission("system:user:query")` | 当前用户必须拥有指定权限码    |
| `mode = SaMode.AND`                       | 必须同时满足多个角色或权限    |
| `mode = SaMode.OR`                        | 满足任意一个角色或权限即可    |

调用示例：

```bash
curl -X GET "http://localhost:8080/permission-test/admin" \
  -H "Authorization: Bearer 登录后返回的tokenValue"

curl -X GET "http://localhost:8080/permission-test/user-query" \
  -H "Authorization: Bearer 登录后返回的tokenValue"
```

注解鉴权需要配合前文配置的 `SaInterceptor` 使用，否则注解可能不会被拦截器解析。实际项目中建议以注解鉴权作为主要接口权限控制方式，路由拦截鉴权作为全局基础保护方式。

### 路由拦截鉴权

路由拦截鉴权适合处理全局规则，例如所有接口默认要求登录、管理端接口要求管理员角色、系统配置接口要求特定权限。它的优势是可以集中维护访问规则，避免每个接口都重复添加基础登录校验。

下面给出增强后的 Sa-Token 拦截器配置，可以替换前文基础配置中的 `SaTokenConfig`。

文件位置：`src/main/java/io/github/atengk/config/SaTokenConfig.java`

```java
package io.github.atengk.config;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaHttpMethod;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.collection.CollUtil;
import io.github.atengk.security.SecurityConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Sa-Token 基础配置
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    /**
     * 白名单接口
     */
    private static final List<String> WHITE_LIST = CollUtil.newArrayList(
            "/auth/login",
            "/auth/status",
            "/error",
            "/favicon.ico",
            "/doc.html",
            "/webjars/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**"
    );

    /**
     * 注册 Sa-Token 拦截器
     *
     * @param registry 拦截器注册器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handler -> {
                    // 放行浏览器跨域预检请求
                    SaRouter.match(SaHttpMethod.OPTIONS)
                            .free(r -> log.debug("放行跨域预检请求：{}", SaHolder.getRequest().getRequestPath()))
                            .back();

                    // 除白名单外，所有接口都要求登录
                    SaRouter.match("/**")
                            .notMatch(WHITE_LIST)
                            .check(r -> StpUtil.checkLogin());

                    // 管理端接口要求管理员角色
                    SaRouter.match("/admin/**")
                            .check(r -> StpUtil.checkRole(SecurityConstant.ROLE_ADMIN));

                    // 用户管理接口要求用户查询权限
                    SaRouter.match("/user/**")
                            .notMatch("/user/current", "/user/profile")
                            .check(r -> StpUtil.checkPermission(SecurityConstant.PERMISSION_USER_QUERY));

                    // 系统接口要求管理员角色
                    SaRouter.match("/system/**")
                            .check(r -> StpUtil.checkRole(SecurityConstant.ROLE_ADMIN));
                }))
                .addPathPatterns("/**");
    }

    /**
     * 配置跨域规则
     *
     * @param registry 跨域注册器
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                // 生产环境建议替换为前端实际域名
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Authorization")
                .allowCredentials(false)
                .maxAge(3600);
    }
}
```

路由规则说明如下：

| 路由                  | 规则                                            |
| --------------------- | ----------------------------------------------- |
| `/auth/login`         | 放行                                            |
| `/auth/status`        | 放行，用于前端检查登录状态                      |
| `/user/current`       | 需要登录                                        |
| `/permission-test/**` | 需要登录，具体权限由注解控制                    |
| `/admin/**`           | 需要 `admin` 角色                               |
| `/system/**`          | 需要 `admin` 角色                               |
| `/user/**`            | 除当前用户接口外，需要 `system:user:query` 权限 |

路由拦截鉴权适合做全局底线控制，注解鉴权适合做接口级精确控制。实际项目中可以两者结合：所有非白名单接口统一要求登录，具体业务接口再通过注解判断角色和权限。

## 权限与角色控制

本章用于说明如何设计角色模型和权限码模型，并通过实现 `StpInterface` 向 Sa-Token 提供当前用户拥有的角色集合和权限集合。Sa-Token 的角色校验和权限校验本质上都是判断当前登录用户是否拥有指定字符串标识，因此角色编码和权限码设计必须稳定、清晰、可维护。

### 角色模型设计

角色用于描述用户在系统中的身份或职责，例如超级管理员、系统管理员、普通用户、审计员等。角色通常用于控制模块级访问权限，例如后台管理入口、系统配置入口、审计日志入口等。

推荐角色模型如下：

| 字段       | 类型      | 说明                     |
| ---------- | --------- | ------------------------ |
| `id`       | `Long`    | 角色 ID                  |
| `roleName` | `String`  | 角色名称，例如系统管理员 |
| `roleCode` | `String`  | 角色编码，例如 `admin`   |
| `sort`     | `Integer` | 排序值                   |
| `enabled`  | `Boolean` | 是否启用                 |
| `remark`   | `String`  | 备注                     |

推荐角色编码如下：

| 角色名称   | 角色编码      | 说明                             |
| ---------- | ------------- | -------------------------------- |
| 超级管理员 | `super_admin` | 拥有系统全部权限                 |
| 系统管理员 | `admin`       | 拥有大部分后台管理权限           |
| 普通用户   | `user`        | 仅拥有基础业务权限               |
| 审计员     | `auditor`     | 只读查看日志、报表、审计数据     |
| 运维人员   | `ops`         | 负责系统配置、任务、监控相关功能 |

角色设计建议如下：

1. 角色编码使用小写英文和下划线，例如 `super_admin`。
2. 角色用于粗粒度身份控制，权限码用于细粒度接口控制。
3. 不建议把角色编码设计成中文名称。
4. 不建议在接口上大量只使用角色校验，应优先使用权限码校验。
5. 超级管理员可以在权限查询时直接返回 `*`，表示拥有全部权限。

如果使用数据库维护角色，常见建表可以参考以下结构：

```sql
CREATE TABLE sys_role (
    id BIGINT PRIMARY KEY COMMENT '角色ID',
    role_name VARCHAR(64) NOT NULL COMMENT '角色名称',
    role_code VARCHAR(64) NOT NULL COMMENT '角色编码',
    sort INT DEFAULT 0 COMMENT '排序值',
    enabled TINYINT DEFAULT 1 COMMENT '是否启用：1启用，0禁用',
    remark VARCHAR(255) COMMENT '备注'
) COMMENT '系统角色表';

CREATE TABLE sys_user_role (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_id BIGINT NOT NULL COMMENT '角色ID'
) COMMENT '用户角色关联表';
```

在本示例中，为了不引入数据库，先使用内存模拟用户角色关系。后续接入 MyBatis-Plus 后，只需要将模拟数据替换为数据库查询即可。

### 权限码模型设计

权限码用于描述用户可以执行的具体操作，一般与菜单、按钮、接口动作绑定。权限码建议采用 `系统:模块:动作` 的格式，例如 `system:user:query`、`system:user:add`、`system:user:update`、`system:user:delete`。

推荐权限码格式如下：

```text
业务域:资源:动作
```

示例权限码如下：

| 权限名称 | 权限码               | 说明                   |
| -------- | -------------------- | ---------------------- |
| 用户查询 | `system:user:query`  | 查看用户列表和用户详情 |
| 用户新增 | `system:user:add`    | 新增用户               |
| 用户修改 | `system:user:update` | 修改用户信息           |
| 用户删除 | `system:user:delete` | 删除用户               |
| 角色查询 | `system:role:query`  | 查看角色列表和角色详情 |
| 角色授权 | `system:role:grant`  | 给角色分配权限         |
| 菜单查询 | `system:menu:query`  | 查看菜单列表           |
| 日志查询 | `system:log:query`   | 查看操作日志           |

权限码设计建议如下：

1. 查询类权限统一使用 `query`。
2. 新增类权限统一使用 `add`。
3. 修改类权限统一使用 `update`。
4. 删除类权限统一使用 `delete`。
5. 导入导出可以使用 `import`、`export`。
6. 授权类权限可以使用 `grant`。
7. 不建议使用过于随意的权限码，例如 `user1`、`admin_test`。
8. 菜单权限和按钮权限可以共用同一套权限码模型。

如果使用数据库维护权限，常见建表可以参考以下结构：

```sql
CREATE TABLE sys_permission (
    id BIGINT PRIMARY KEY COMMENT '权限ID',
    permission_name VARCHAR(64) NOT NULL COMMENT '权限名称',
    permission_code VARCHAR(128) NOT NULL COMMENT '权限编码',
    permission_type VARCHAR(32) NOT NULL COMMENT '权限类型：MENU菜单，BUTTON按钮，API接口',
    parent_id BIGINT DEFAULT 0 COMMENT '父级权限ID',
    sort INT DEFAULT 0 COMMENT '排序值',
    enabled TINYINT DEFAULT 1 COMMENT '是否启用：1启用，0禁用',
    remark VARCHAR(255) COMMENT '备注'
) COMMENT '系统权限表';

CREATE TABLE sys_role_permission (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    permission_id BIGINT NOT NULL COMMENT '权限ID'
) COMMENT '角色权限关联表';
```

权限码与接口的关系建议如下：

| 接口                       | 权限码               |
| -------------------------- | -------------------- |
| `GET /system/user/list`    | `system:user:query`  |
| `GET /system/user/{id}`    | `system:user:query`  |
| `POST /system/user`        | `system:user:add`    |
| `PUT /system/user/{id}`    | `system:user:update` |
| `DELETE /system/user/{id}` | `system:user:delete` |

这样可以让后端接口权限、前端按钮权限、菜单权限保持一致，便于统一授权和排查问题。

### StpInterface 实现

Sa-Token 通过 `StpInterface` 获取当前账号拥有的权限码集合和角色集合。实现该接口后，`@SaCheckPermission`、`@SaCheckRole`、`StpUtil.checkPermission()`、`StpUtil.checkRole()` 等校验方法就会基于这里返回的数据进行判断。常见实践也是通过实现 `getPermissionList` 和 `getRoleList` 分别返回权限列表和角色列表。([Hexo](https://xinshengttt.github.io/2024/05/17/鉴权注解@SaCheckPermission和@SaCheckRole/?utm_source=chatgpt.com))

下面先定义权限服务接口，模拟从业务系统中查询用户角色和权限。

文件位置：`src/main/java/io/github/atengk/service/PermissionService.java`

```java
package io.github.atengk.service;

import java.util.List;

/**
 * 权限查询服务
 *
 * @author Ateng
 * @since 2026-05-05
 */
public interface PermissionService {

    /**
     * 根据用户ID查询角色编码列表
     *
     * @param userId 用户ID
     * @return 角色编码列表
     */
    List<String> listRoleCodeByUserId(Long userId);

    /**
     * 根据用户ID查询权限编码列表
     *
     * @param userId 用户ID
     * @return 权限编码列表
     */
    List<String> listPermissionCodeByUserId(Long userId);
}
```

下面的实现类使用内存数据模拟数据库查询。实际项目中可替换为 Mapper 查询用户角色表、角色权限表和权限表。

文件位置：`src/main/java/io/github/atengk/service/impl/PermissionServiceImpl.java`

```java
package io.github.atengk.service.impl;

import cn.hutool.core.collection.CollUtil;
import io.github.atengk.security.SecurityConstant;
import io.github.atengk.service.PermissionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 权限查询服务实现
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Service
public class PermissionServiceImpl implements PermissionService {

    /**
     * 根据用户ID查询角色编码列表
     *
     * @param userId 用户ID
     * @return 角色编码列表
     */
    @Override
    public List<String> listRoleCodeByUserId(Long userId) {
        if (1L == userId) {
            log.debug("查询用户角色成功，用户ID：{}，角色：admin", userId);
            return CollUtil.newArrayList(SecurityConstant.ROLE_ADMIN, SecurityConstant.ROLE_USER);
        }

        log.debug("查询用户角色成功，用户ID：{}，角色：user", userId);
        return CollUtil.newArrayList(SecurityConstant.ROLE_USER);
    }

    /**
     * 根据用户ID查询权限编码列表
     *
     * @param userId 用户ID
     * @return 权限编码列表
     */
    @Override
    public List<String> listPermissionCodeByUserId(Long userId) {
        if (1L == userId) {
            log.debug("查询用户权限成功，用户ID：{}，权限：用户管理全权限", userId);
            return CollUtil.newArrayList(
                    SecurityConstant.PERMISSION_USER_QUERY,
                    SecurityConstant.PERMISSION_USER_ADD,
                    SecurityConstant.PERMISSION_USER_UPDATE,
                    SecurityConstant.PERMISSION_USER_DELETE
            );
        }

        log.debug("查询用户权限成功，用户ID：{}，权限：用户查询", userId);
        return CollUtil.newArrayList(SecurityConstant.PERMISSION_USER_QUERY);
    }
}
```

下面实现 `StpInterface`，把业务系统中的角色和权限提供给 Sa-Token。

文件位置：`src/main/java/io/github/atengk/security/SaPermissionConfig.java`

```java
package io.github.atengk.security;

import cn.dev33.satoken.stp.StpInterface;
import cn.hutool.core.convert.Convert;
import io.github.atengk.service.PermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Sa-Token 权限数据加载配置
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SaPermissionConfig implements StpInterface {

    private final PermissionService permissionService;

    /**
     * 获取当前账号拥有的权限码集合
     *
     * @param loginId   登录用户ID
     * @param loginType 登录类型
     * @return 权限码集合
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        Long userId = Convert.toLong(loginId);
        List<String> permissions = permissionService.listPermissionCodeByUserId(userId);
        log.debug("加载用户权限成功，用户ID：{}，登录类型：{}，权限数量：{}", userId, loginType, permissions.size());
        return permissions;
    }

    /**
     * 获取当前账号拥有的角色编码集合
     *
     * @param loginId   登录用户ID
     * @param loginType 登录类型
     * @return 角色编码集合
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        Long userId = Convert.toLong(loginId);
        List<String> roles = permissionService.listRoleCodeByUserId(userId);
        log.debug("加载用户角色成功，用户ID：{}，登录类型：{}，角色数量：{}", userId, loginType, roles.size());
        return roles;
    }
}
```

实现说明如下：

| 方法                | 作用                                                         |
| ------------------- | ------------------------------------------------------------ |
| `getPermissionList` | 返回当前用户拥有的权限码集合，对应 `@SaCheckPermission`      |
| `getRoleList`       | 返回当前用户拥有的角色编码集合，对应 `@SaCheckRole`          |
| `loginId`           | 登录时传入的用户 ID，即 `StpUtil.login(userId)` 中的 `userId` |
| `loginType`         | 登录类型，多账号体系时用于区分不同账号体系                   |

实际项目中建议在 `PermissionServiceImpl` 中查询数据库，并结合 Redis 缓存用户权限。用户角色或权限发生变化时，需要清理用户权限缓存，避免权限变更后仍然使用旧数据。

### 接口权限控制

接口权限控制建议分为三层：第一层是路由拦截器统一要求登录，第二层是注解控制接口级角色和权限，第三层是在复杂业务方法中使用 `StpUtil` 进行动态权限判断。

下面给出一个系统用户管理接口示例，演示如何使用权限注解控制接口访问。

文件位置：`src/main/java/io/github/atengk/controller/SystemUserController.java`

```java
package io.github.atengk.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.collection.CollUtil;
import io.github.atengk.common.result.Result;
import io.github.atengk.security.SecurityConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 系统用户管理接口
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@RestController
@RequestMapping("/system/user")
public class SystemUserController {

    /**
     * 查询用户列表
     *
     * @return 用户列表
     */
    @SaCheckPermission(SecurityConstant.PERMISSION_USER_QUERY)
    @GetMapping("/list")
    public Result<List<String>> list() {
        List<String> users = CollUtil.newArrayList("admin", "test", "guest");
        return Result.success(users);
    }

    /**
     * 新增用户
     *
     * @param username 用户名
     * @return 操作结果
     */
    @SaCheckPermission(SecurityConstant.PERMISSION_USER_ADD)
    @PostMapping
    public Result<String> add(@RequestParam String username) {
        log.info("新增用户成功，操作人：{}，新用户：{}", StpUtil.getLoginIdAsLong(), username);
        return Result.success("新增用户成功：" + username);
    }

    /**
     * 修改用户
     *
     * @param userId   用户ID
     * @param username 用户名
     * @return 操作结果
     */
    @SaCheckPermission(SecurityConstant.PERMISSION_USER_UPDATE)
    @PutMapping("/{userId}")
    public Result<String> update(@PathVariable Long userId, @RequestParam String username) {
        log.info("修改用户成功，操作人：{}，用户ID：{}，用户名：{}", StpUtil.getLoginIdAsLong(), userId, username);
        return Result.success("修改用户成功：" + username);
    }

    /**
     * 删除用户
     *
     * @param userId 用户ID
     * @return 操作结果
     */
    @SaCheckPermission(SecurityConstant.PERMISSION_USER_DELETE)
    @DeleteMapping("/{userId}")
    public Result<String> delete(@PathVariable Long userId) {
        log.info("删除用户成功，操作人：{}，用户ID：{}", StpUtil.getLoginIdAsLong(), userId);
        return Result.success("删除用户成功：" + userId);
    }

    /**
     * 管理员专属接口
     *
     * @return 操作结果
     */
    @SaCheckRole(SecurityConstant.ROLE_ADMIN)
    @GetMapping("/admin-only")
    public Result<String> adminOnly() {
        return Result.success("管理员角色可以访问该接口");
    }

    /**
     * 复杂业务中的动态权限判断
     *
     * @param userId 用户ID
     * @return 操作结果
     */
    @PostMapping("/{userId}/reset-password")
    public Result<String> resetPassword(@PathVariable Long userId) {
        StpUtil.checkPermission(SecurityConstant.PERMISSION_USER_UPDATE);

        if (1L == userId && !StpUtil.hasRole(SecurityConstant.ROLE_ADMIN)) {
            log.warn("重置管理员密码失败，当前用户不是管理员，操作人：{}", StpUtil.getLoginIdAsLong());
            return Result.fail("只有管理员可以重置管理员密码");
        }

        log.info("重置用户密码成功，操作人：{}，目标用户：{}", StpUtil.getLoginIdAsLong(), userId);
        return Result.success("重置密码成功");
    }
}
```

接口权限控制示例：

| 接口                                        | 权限要求                                     |
| ------------------------------------------- | -------------------------------------------- |
| `GET /system/user/list`                     | `system:user:query`                          |
| `POST /system/user`                         | `system:user:add`                            |
| `PUT /system/user/{userId}`                 | `system:user:update`                         |
| `DELETE /system/user/{userId}`              | `system:user:delete`                         |
| `GET /system/user/admin-only`               | `admin` 角色                                 |
| `POST /system/user/{userId}/reset-password` | `system:user:update`，并在业务中动态判断角色 |

测试命令如下：

```bash
curl -X GET "http://localhost:8080/system/user/list" \
  -H "Authorization: Bearer 登录后返回的tokenValue"

curl -X POST "http://localhost:8080/system/user?username=test01" \
  -H "Authorization: Bearer 登录后返回的tokenValue"

curl -X PUT "http://localhost:8080/system/user/2?username=test02" \
  -H "Authorization: Bearer 登录后返回的tokenValue"

curl -X DELETE "http://localhost:8080/system/user/2" \
  -H "Authorization: Bearer 登录后返回的tokenValue"
```

接口权限控制建议如下：

1. 所有需要登录的接口都应经过拦截器保护。
2. 普通业务接口优先使用 `@SaCheckPermission`。
3. 管理员专属接口可以使用 `@SaCheckRole`。
4. 涉及复杂业务条件时，可以在方法内部使用 `StpUtil.checkPermission()` 和 `StpUtil.hasRole()`。
5. 权限码应与前端菜单、按钮权限保持一致。
6. 权限不足异常不要在 Controller 中零散处理，应放到后续全局异常处理章节统一返回。



## Token 管理

本章用于说明 Sa-Token 中 Token 的获取、续期、注销和多端登录控制。Token 管理主要围绕 `StpUtil` 展开，常用方法包括 `getTokenValue()`、`getTokenInfo()`、`getTokenTimeout()`、`renewTimeout()`、`updateLastActiveToNow()`、`logout()`、`logoutByTokenValue()`、`kickout()`、`getTokenValueListByLoginId()` 等。Sa-Token 官方 `StpUtil` 文档明确列出了 Token 获取、登录、注销、会话查询、Token 有效期、角色权限、ID 反查 Token 等常用 API。([Gitee](https://gitee.com/dromara/sa-token/blob/40b01aba00112b76caba1989f42919f5fbe49c38/sa-token-doc/api/stp-util.md?utm_source=chatgpt.com))

### Token 获取方式

Token 获取方式分为两类：登录成功后获取 Token，以及请求过程中获取当前 Token。登录成功后通常返回 `tokenName`、`tokenValue`、`authorization`、`tokenTimeout` 等字段给前端；后续业务接口中，后端可以通过 `StpUtil.getTokenValue()` 获取当前请求携带的 Token。

常见 Token 获取 API 如下：

| 方法                                          | 说明                                  |
| --------------------------------------------- | ------------------------------------- |
| `StpUtil.getTokenName()`                      | 获取 Token 名称，例如 `Authorization` |
| `StpUtil.getTokenValue()`                     | 获取当前请求提交的 Token 值           |
| `StpUtil.getTokenValueNotCut()`               | 获取未裁剪前缀的 Token 值             |
| `StpUtil.getTokenInfo()`                      | 获取当前 Token 详细信息               |
| `StpUtil.getTokenTimeout()`                   | 获取当前 Token 剩余有效期             |
| `StpUtil.getTokenActiveTimeout()`             | 获取当前 Token 剩余活跃有效期         |
| `StpUtil.getLoginDevice()`                    | 获取当前 Token 的登录设备             |
| `StpUtil.getTokenValueListByLoginId(loginId)` | 根据用户 ID 查询该用户所有 Token      |

下面新增 Token 管理相关文件：

```text
src/main/java/io/github/atengk
├── controller
│   └── TokenController.java
├── dto
│   ├── TokenRenewRequest.java
│   └── TokenLogoutRequest.java
├── service
│   └── TokenService.java
├── service
│   └── impl
│       └── TokenServiceImpl.java
└── vo
    └── TokenInfoVO.java
```

下面的 VO 用于返回当前 Token 信息。

文件位置：`src/main/java/io/github/atengk/vo/TokenInfoVO.java`

```java
package io.github.atengk.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Token 信息
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Data
@Builder
public class TokenInfoVO {

    /**
     * 是否已登录
     */
    private Boolean login;

    /**
     * 登录用户ID
     */
    private Long userId;

    /**
     * Token 名称
     */
    private String tokenName;

    /**
     * Token 值
     */
    private String tokenValue;

    /**
     * 完整认证值
     */
    private String authorization;

    /**
     * Token 剩余有效期，单位：秒
     */
    private Long tokenTimeout;

    /**
     * Token 剩余活跃有效期，单位：秒
     */
    private Long tokenActiveTimeout;

    /**
     * 登录设备
     */
    private String loginDevice;

    /**
     * 当前用户全部 Token
     */
    private List<String> tokenValueList;
}
```

下面的 Service 用于封装 Token 查询、续期、注销和多端控制逻辑。

文件位置：`src/main/java/io/github/atengk/service/TokenService.java`

```java
package io.github.atengk.service;

import io.github.atengk.dto.TokenLogoutRequest;
import io.github.atengk.dto.TokenRenewRequest;
import io.github.atengk.vo.TokenInfoVO;

/**
 * Token 管理服务
 *
 * @author Ateng
 * @since 2026-05-05
 */
public interface TokenService {

    /**
     * 获取当前 Token 信息
     *
     * @return Token 信息
     */
    TokenInfoVO getCurrentTokenInfo();

    /**
     * 续期当前 Token
     *
     * @param request Token 续期参数
     * @return Token 信息
     */
    TokenInfoVO renewCurrentToken(TokenRenewRequest request);

    /**
     * 刷新当前 Token 活跃时间
     *
     * @return Token 信息
     */
    TokenInfoVO refreshActiveTimeout();

    /**
     * 注销指定 Token
     *
     * @param request Token 注销参数
     */
    void logoutByToken(TokenLogoutRequest request);

    /**
     * 注销指定用户指定设备
     *
     * @param userId 用户ID
     * @param device 设备类型
     */
    void logoutByUserAndDevice(Long userId, String device);

    /**
     * 踢下线指定用户指定设备
     *
     * @param userId 用户ID
     * @param device 设备类型
     */
    void kickoutByUserAndDevice(Long userId, String device);
}
```

下面的实现类封装 Sa-Token 的 Token 管理 API。

文件位置：`src/main/java/io/github/atengk/service/impl/TokenServiceImpl.java`

```java
package io.github.atengk.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.dto.TokenLogoutRequest;
import io.github.atengk.dto.TokenRenewRequest;
import io.github.atengk.service.TokenService;
import io.github.atengk.vo.TokenInfoVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Token 管理服务实现
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Service
public class TokenServiceImpl implements TokenService {

    /**
     * 获取当前 Token 信息
     *
     * @return Token 信息
     */
    @Override
    public TokenInfoVO getCurrentTokenInfo() {
        boolean login = StpUtil.isLogin();

        if (!login) {
            return TokenInfoVO.builder()
                    .login(false)
                    .tokenName(StpUtil.getTokenName())
                    .build();
        }

        Long userId = StpUtil.getLoginIdAsLong();
        List<String> tokenValueList = StpUtil.getTokenValueListByLoginId(userId);

        return TokenInfoVO.builder()
                .login(true)
                .userId(userId)
                .tokenName(StpUtil.getTokenName())
                .tokenValue(StpUtil.getTokenValue())
                .authorization(StrUtil.format("Bearer {}", StpUtil.getTokenValue()))
                .tokenTimeout(StpUtil.getTokenTimeout())
                .tokenActiveTimeout(StpUtil.getTokenActiveTimeout())
                .loginDevice(StpUtil.getLoginDevice())
                .tokenValueList(CollUtil.newArrayList(tokenValueList))
                .build();
    }

    /**
     * 续期当前 Token
     *
     * @param request Token 续期参数
     * @return Token 信息
     */
    @Override
    public TokenInfoVO renewCurrentToken(TokenRenewRequest request) {
        StpUtil.checkLogin();

        long timeout = request.getTimeout();
        StpUtil.renewTimeout(timeout);

        log.info("Token 续期成功，用户ID：{}，续期时间：{}秒", StpUtil.getLoginIdAsLong(), timeout);
        return getCurrentTokenInfo();
    }

    /**
     * 刷新当前 Token 活跃时间
     *
     * @return Token 信息
     */
    @Override
    public TokenInfoVO refreshActiveTimeout() {
        StpUtil.checkLogin();
        StpUtil.updateLastActiveToNow();

        log.info("Token 活跃时间刷新成功，用户ID：{}", StpUtil.getLoginIdAsLong());
        return getCurrentTokenInfo();
    }

    /**
     * 注销指定 Token
     *
     * @param request Token 注销参数
     */
    @Override
    public void logoutByToken(TokenLogoutRequest request) {
        String tokenValue = StrUtil.trim(request.getTokenValue());
        StpUtil.logoutByTokenValue(tokenValue);
        log.info("指定 Token 注销成功，Token：{}", tokenValue);
    }

    /**
     * 注销指定用户指定设备
     *
     * @param userId 用户ID
     * @param device 设备类型
     */
    @Override
    public void logoutByUserAndDevice(Long userId, String device) {
        String loginDevice = StrUtil.blankToDefault(device, "PC");
        StpUtil.logout(userId, loginDevice);
        log.info("指定用户设备注销成功，用户ID：{}，设备：{}", userId, loginDevice);
    }

    /**
     * 踢下线指定用户指定设备
     *
     * @param userId 用户ID
     * @param device 设备类型
     */
    @Override
    public void kickoutByUserAndDevice(Long userId, String device) {
        String loginDevice = StrUtil.blankToDefault(device, "PC");
        StpUtil.kickout(userId, loginDevice);
        log.info("指定用户设备踢下线成功，用户ID：{}，设备：{}", userId, loginDevice);
    }
}
```

### Token 续期机制

Sa-Token 中需要区分两个有效期概念：`timeout` 和 `active-timeout`。`timeout` 是 Token 总有效期，代表 Token 最长可以存活多久；`active-timeout` 是 Token 活跃有效期，代表用户超过多长时间无操作后会被冻结。Sa-Token 文档说明，`timeout` 代表 Token 长久有效期，`activity-timeout` 代表临时有效期；并且 1.29.0 之后支持使用 `StpUtil.renewTimeout(timeout)` 对 Token 总有效期进行续期。([Gitee](https://gitee.com/dromara/sa-token/blob/4ef67ee282d293d3876ba5718f8ef1b2c19d29df/sa-token-doc/doc/fun/token-timeout.md?utm_source=chatgpt.com))

下面的 DTO 用于接收 Token 续期参数。

文件位置：`src/main/java/io/github/atengk/dto/TokenRenewRequest.java`

```java
package io.github.atengk.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * Token 续期请求参数
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Data
public class TokenRenewRequest {

    /**
     * 续期后的 Token 有效期，单位：秒
     */
    @Min(value = 60, message = "Token 有效期不能小于60秒")
    @Max(value = 2592000, message = "Token 有效期不能大于30天")
    private Long timeout;
}
```

Token 续期方式建议如下：

| 续期类型         | 方法                                   | 说明                                      |
| ---------------- | -------------------------------------- | ----------------------------------------- |
| 刷新活跃时间     | `StpUtil.updateLastActiveToNow()`      | 更新最后活跃时间，适用于 `active-timeout` |
| 续期总有效期     | `StpUtil.renewTimeout(timeout)`        | 修改当前 Token 的 `timeout` 剩余有效期    |
| 续期指定 Token   | `StpUtil.renewTimeout(token, timeout)` | 修改指定 Token 的剩余有效期               |
| 自动续签活跃时间 | `auto-renew: true`                     | 请求访问时自动刷新活跃时间                |

建议策略如下：

1. 后台管理系统优先使用 `active-timeout` 控制无操作冻结。
2. 不建议每次请求都手动调用 `renewTimeout()`，否则 Token 可能长期有效。
3. `rememberMe = true` 可以在登录时设置较长 `timeout`。
4. 普通登录可以使用较短 `timeout`，例如 2 小时或 8 小时。
5. 管理员敏感操作可以结合二级认证，而不是简单延长 Token 有效期。

### Token 注销机制

Token 注销用于退出登录、强制下线、注销指定 Token、注销指定设备等场景。Sa-Token 提供了 `logout()`、`logout(loginId)`、`logout(loginId, device)`、`logoutByTokenValue(token)`、`kickout(loginId)`、`kickout(loginId, device)`、`kickoutByTokenValue(token)` 等方法。([Gitee](https://gitee.com/dromara/sa-token/blob/40b01aba00112b76caba1989f42919f5fbe49c38/sa-token-doc/api/stp-util.md?utm_source=chatgpt.com))

下面的 DTO 用于接收需要注销的 Token。

文件位置：`src/main/java/io/github/atengk/dto/TokenLogoutRequest.java`

```java
package io.github.atengk.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Token 注销请求参数
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Data
public class TokenLogoutRequest {

    /**
     * Token 值
     */
    @NotBlank(message = "Token不能为空")
    private String tokenValue;
}
```

下面的 Controller 对外提供 Token 管理接口。

文件位置：`src/main/java/io/github/atengk/controller/TokenController.java`

```java
package io.github.atengk.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import io.github.atengk.common.result.Result;
import io.github.atengk.dto.TokenLogoutRequest;
import io.github.atengk.dto.TokenRenewRequest;
import io.github.atengk.security.SecurityConstant;
import io.github.atengk.service.TokenService;
import io.github.atengk.vo.TokenInfoVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Token 管理接口
 *
 * @author Ateng
 * @since 2026-05-05
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/token")
public class TokenController {

    private final TokenService tokenService;

    /**
     * 获取当前 Token 信息
     *
     * @return Token 信息
     */
    @SaCheckLogin
    @GetMapping("/info")
    public Result<TokenInfoVO> info() {
        return Result.success(tokenService.getCurrentTokenInfo());
    }

    /**
     * 续期当前 Token 总有效期
     *
     * @param request Token 续期参数
     * @return Token 信息
     */
    @SaCheckLogin
    @PostMapping("/renew")
    public Result<TokenInfoVO> renew(@Valid @RequestBody TokenRenewRequest request) {
        return Result.success(tokenService.renewCurrentToken(request));
    }

    /**
     * 刷新当前 Token 活跃时间
     *
     * @return Token 信息
     */
    @SaCheckLogin
    @PostMapping("/refresh-active")
    public Result<TokenInfoVO> refreshActive() {
        return Result.success(tokenService.refreshActiveTimeout());
    }

    /**
     * 注销当前 Token
     *
     * @return 操作结果
     */
    @SaCheckLogin
    @PostMapping("/logout-current")
    public Result<Void> logoutCurrent() {
        StpUtil.logout();
        return Result.success(null);
    }

    /**
     * 注销指定 Token
     *
     * @param request Token 注销参数
     * @return 操作结果
     */
    @SaCheckRole(SecurityConstant.ROLE_ADMIN)
    @PostMapping("/logout-token")
    public Result<Void> logoutByToken(@Valid @RequestBody TokenLogoutRequest request) {
        tokenService.logoutByToken(request);
        return Result.success(null);
    }

    /**
     * 注销指定用户指定设备
     *
     * @param userId 用户ID
     * @param device 设备类型
     * @return 操作结果
     */
    @SaCheckRole(SecurityConstant.ROLE_ADMIN)
    @PostMapping("/logout-user-device")
    public Result<Void> logoutByUserAndDevice(@RequestParam Long userId,
                                               @RequestParam(defaultValue = "PC") String device) {
        tokenService.logoutByUserAndDevice(userId, device);
        return Result.success(null);
    }

    /**
     * 踢下线指定用户指定设备
     *
     * @param userId 用户ID
     * @param device 设备类型
     * @return 操作结果
     */
    @SaCheckRole(SecurityConstant.ROLE_ADMIN)
    @PostMapping("/kickout-user-device")
    public Result<Void> kickoutByUserAndDevice(@RequestParam Long userId,
                                                @RequestParam(defaultValue = "PC") String device) {
        tokenService.kickoutByUserAndDevice(userId, device);
        return Result.success(null);
    }
}
```

Token 注销方式建议如下：

| 场景                     | 推荐方法                             |
| ------------------------ | ------------------------------------ |
| 当前用户退出登录         | `StpUtil.logout()`                   |
| 管理员强制注销某个 Token | `StpUtil.logoutByTokenValue(token)`  |
| 管理员注销某个用户全部端 | `StpUtil.logout(loginId)`            |
| 管理员注销某个用户指定端 | `StpUtil.logout(loginId, device)`    |
| 踢人下线                 | `StpUtil.kickout(loginId)`           |
| 踢掉指定 Token           | `StpUtil.kickoutByTokenValue(token)` |

`logout` 和 `kickout` 的语义建议区分使用：用户主动退出登录时使用 `logout`；管理员或系统策略强制用户重新登录时使用 `kickout`。

### 多端登录控制

多端登录控制主要依赖两个配置项和一个登录参数：`is-concurrent`、`is-share` 和 `device`。`is-concurrent` 控制同一账号是否允许并发登录，`is-share` 控制同一账号多次登录是否共享同一个 Token，`device` 用于区分 PC、APP、小程序等登录端。

推荐配置如下：

```yaml
sa-token:
  # 是否允许同一账号并发登录
  is-concurrent: true

  # 多人登录同一账号时，是否共用一个 Token
  is-share: false

  # 是否启用动态活跃时间
  dynamic-active-timeout: true
```

登录时指定设备类型：

```java
SaLoginModel loginModel = new SaLoginModel()
        .setDevice("PC")
        .setTimeout(60 * 60 * 24 * 7)
        .setActiveTimeout(60 * 30);

StpUtil.login(userId, loginModel);
```

多端登录策略建议如下：

| 策略                     | 配置建议                                 | 说明                                     |
| ------------------------ | ---------------------------------------- | ---------------------------------------- |
| 允许多端同时在线         | `is-concurrent: true`、`is-share: false` | 每次登录生成不同 Token                   |
| 同一账号仅保留一个 Token | `is-concurrent: false`                   | 新登录会影响旧登录                       |
| 同一账号共享 Token       | `is-share: true`                         | 多次登录复用 Token                       |
| PC 和 APP 分端控制       | 登录时设置不同 `device`                  | 通过 `logout(userId, device)` 控制指定端 |
| 管理员强制下线           | 使用 `kickout(userId, device)`           | 适合后台运维管理场景                     |

测试示例：

```bash
# 查看当前 Token 信息
curl -X GET "http://localhost:8080/token/info" \
  -H "Authorization: Bearer 登录后返回的tokenValue"

# 续期当前 Token
curl -X POST "http://localhost:8080/token/renew" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer 登录后返回的tokenValue" \
  -d '{
    "timeout": 604800
  }'

# 刷新当前 Token 活跃时间
curl -X POST "http://localhost:8080/token/refresh-active" \
  -H "Authorization: Bearer 登录后返回的tokenValue"

# 注销当前 Token
curl -X POST "http://localhost:8080/token/logout-current" \
  -H "Authorization: Bearer 登录后返回的tokenValue"
```

实际项目中，普通用户通常只允许注销自己的 Token；指定 Token 注销、指定用户踢下线、指定设备下线等接口应限制为管理员角色访问。

## Redis 持久化集成

本章用于说明如何将 Sa-Token 的登录态、Token 信息和会话数据持久化到 Redis。默认情况下，Sa-Token 的数据保存在内存中，服务重启后登录态会丢失，并且多实例部署时无法共享登录状态；集成 Redis 后，可以解决服务重启登录态丢失和分布式实例会话不一致的问题。Sa-Token Redis 集成文档说明：集成 Redis 后框架会自动保存数据，上层 API 保持不变，不需要额外手动保存登录态。([Gitee](https://gitee.com/dromara/sa-token/blob/adbc1003b475939e858fe4705056d76d68b1424f/sa-token-doc/doc/up/integ-redis.md?utm_source=chatgpt.com))

### Redis 依赖配置

Spring Boot 3 集成 Redis 时，建议使用 `spring-boot-starter-data-redis` 提供 Redis 自动配置，使用 `commons-pool2` 支持 Lettuce 连接池，并引入 Sa-Token Redis Jackson 集成包。

需要注意版本命名：旧文档中常见的是 `sa-token-dao-redis-jackson`，而 Maven Central 当前可查到 Sa-Token 1.45.0 的 Redis Jackson 集成包为 `sa-token-redis-jackson`，描述为 `sa-token integrate redis (to jackson)`；旧的 `sa-token-dao-redis-jackson` 在 Central 上可见较早版本，例如 1.11.0。([Maven Central](https://central.sonatype.com/artifact/cn.dev33/sa-token-redis-jackson/1.45.0?utm_source=chatgpt.com))

文件位置：`pom.xml`

```xml
<dependencies>
    <!-- Spring Data Redis：提供 RedisTemplate、连接工厂、Lettuce 客户端等能力 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>

    <!-- Sa-Token Redis Jackson 集成包：将 Sa-Token 登录态和会话数据持久化到 Redis -->
    <dependency>
        <groupId>cn.dev33</groupId>
        <artifactId>sa-token-redis-jackson</artifactId>
        <version>${sa-token.version}</version>
    </dependency>

    <!-- Redis 连接池：启用 spring.data.redis.lettuce.pool 配置时需要 -->
    <dependency>
        <groupId>org.apache.commons</groupId>
        <artifactId>commons-pool2</artifactId>
    </dependency>
</dependencies>
```

如果你的项目仍然使用旧版 Sa-Token，并且官方依赖文档要求使用 `sa-token-dao-redis-jackson`，则必须保证该依赖版本与 Sa-Token Starter 版本一致。不要混用不同大版本的 Starter 和 Redis 集成包，否则可能出现启动失败、方法不存在或登录态读取异常。

依赖选择建议如下：

| 场景                            | 推荐依赖                         |
| ------------------------------- | -------------------------------- |
| Sa-Token 1.45.0 + Spring Boot 3 | `sa-token-redis-jackson`         |
| 旧版 Sa-Token 项目              | 按当前项目版本对应的官方依赖选择 |
| Session 可读性优先              | Jackson 序列化版本               |
| 兼容性优先                      | JDK 序列化版本                   |
| Sa-Token 与业务 Redis 隔离      | 可考虑 `sa-token-alone-redis`    |

Maven 验证命令：

```bash
mvn dependency:tree -Dincludes=cn.dev33
mvn dependency:tree -Dincludes=org.springframework.boot:spring-boot-starter-data-redis
```

第一条命令用于确认 Sa-Token 相关依赖版本是否一致；第二条命令用于确认 Spring Data Redis 是否正确引入。

### Sa-Token DAO 配置

Sa-Token 的 Redis 持久化不需要业务代码手动保存 Token。只要 Redis 依赖和 Redis 连接配置正确，Sa-Token 会通过 Redis 集成包自动把登录态写入 Redis。旧文档中将这一层称为 DAO 配置，本质上是替换 Sa-Token 底层数据读写实现。官方 Redis 集成文档也说明，集成 Redis 后框架自动保存数据，`StpUtil` 等上层 API 保持不变。([Gitee](https://gitee.com/dromara/sa-token/blob/adbc1003b475939e858fe4705056d76d68b1424f/sa-token-doc/doc/up/integ-redis.md?utm_source=chatgpt.com))

Spring Boot 3 的 Redis 配置建议放在 `spring.data.redis` 下。

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  data:
    redis:
      # Redis 数据库索引
      database: 1

      # Redis 服务地址
      host: 127.0.0.1

      # Redis 服务端口
      port: 6379

      # Redis 密码，没有密码时可以不配置
      # password: 123456

      # Redis 连接超时时间
      timeout: 3000ms

      lettuce:
        pool:
          # 连接池最大连接数
          max-active: 20

          # 连接池最大阻塞等待时间，-1ms 表示不限制
          max-wait: -1ms

          # 连接池最大空闲连接
          max-idle: 10

          # 连接池最小空闲连接
          min-idle: 2

sa-token:
  # Token 名称，前端请求头也使用该名称
  token-name: Authorization

  # Token 总有效期，单位：秒
  timeout: 604800

  # Token 活跃有效期，单位：秒
  active-timeout: 1800

  # 启用动态活跃时间，允许登录时通过 SaLoginModel 单独设置 activeTimeout
  dynamic-active-timeout: true

  # 自动续签活跃时间
  auto-renew: true

  # 是否允许同一账号并发登录
  is-concurrent: true

  # 多次登录同一账号是否共享 Token
  is-share: false

  # Token 风格
  token-style: random-64

  # 前后端分离项目从请求头读取 Token
  is-read-header: true

  # 前后端分离项目通常关闭 Cookie 读取
  is-read-cookie: false

  # 不建议从请求参数读取 Token
  is-read-body: false

  # Token 前缀，前端传参格式：Authorization: Bearer xxxxxx
  token-prefix: Bearer

  # 开启日志，便于开发环境排查问题
  is-log: true
```

如果希望本地快速启动 Redis，可以使用 Docker。

```bash
docker run -d \
  --name redis-sa-token \
  -p 6379:6379 \
  redis:7.2
```

该命令会启动一个 Redis 7.2 容器，并将本机 `6379` 端口映射到容器 Redis 服务。开发环境可以直接使用该方式；生产环境建议使用独立 Redis、Redis Sentinel 或 Redis Cluster，并配置密码、网络访问控制和监控告警。

如果需要为 Sa-Token 使用独立 Redis，避免与业务缓存混用，可以额外引入 `sa-token-alone-redis`。Maven Central 当前可查到 `sa-token-alone-redis` 1.45.0，用于 Sa-Token 独立 Redis 场景。([Maven Central](https://central.sonatype.com/artifact/cn.dev33/sa-token-alone-redis?utm_source=chatgpt.com))

示例依赖如下：

```xml
<!-- Sa-Token 独立 Redis：用于将权限缓存与业务缓存分离 -->
<dependency>
    <groupId>cn.dev33</groupId>
    <artifactId>sa-token-alone-redis</artifactId>
    <version>${sa-token.version}</version>
</dependency>
```

是否使用独立 Redis 取决于项目复杂度。单体项目或中小型后台系统，通常直接复用业务 Redis 即可；多业务线、多租户或高安全系统，可以考虑给 Sa-Token 单独配置 Redis。

### 登录状态持久化验证

Redis 持久化验证的目标是确认登录成功后，Sa-Token 登录态已经写入 Redis，并且服务重启后 Token 仍然有效。验证时应重点检查三个结果：登录接口正常返回 Token，Redis 中出现 Sa-Token 相关 Key，服务重启后旧 Token 仍然可以访问受保护接口。

验证流程如下：

| 步骤 | 操作                  | 预期结果                        |
| ---- | --------------------- | ------------------------------- |
| 1    | 启动 Redis            | Redis 连接正常                  |
| 2    | 启动 Spring Boot 项目 | 项目启动成功，无 Redis 连接异常 |
| 3    | 调用登录接口          | 返回 Token                      |
| 4    | 查看 Redis Key        | 出现 Sa-Token 相关 Key          |
| 5    | 重启后端服务          | 服务正常重启                    |
| 6    | 使用旧 Token 调用接口 | 仍然可以访问，说明登录态未丢失  |
| 7    | 注销 Token 后再次访问 | 访问失败，说明 Token 注销生效   |

登录获取 Token：

```bash
curl -X POST "http://localhost:8080/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "123456",
    "rememberMe": true,
    "device": "PC"
  }'
```

查看 Redis 中的 Sa-Token 相关 Key：

```bash
docker exec -it redis-sa-token redis-cli

SELECT 1
KEYS "*satoken*"
```

`SELECT 1` 需要与 `spring.data.redis.database: 1` 保持一致。`KEYS "*satoken*"` 只建议在开发环境使用，生产环境不要直接执行 `KEYS` 扫描大库，避免阻塞 Redis。

重启后端服务后，使用旧 Token 调用接口：

```bash
curl -X GET "http://localhost:8080/token/info" \
  -H "Authorization: Bearer 登录后返回的tokenValue"
```

如果返回结果中 `login` 为 `true`，并且能看到 `userId`、`tokenTimeout`、`loginDevice` 等字段，说明登录态已经由 Redis 持久化保存。返回示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "login": true,
    "userId": 1,
    "tokenName": "Authorization",
    "tokenValue": "登录后返回的tokenValue",
    "authorization": "Bearer 登录后返回的tokenValue",
    "tokenTimeout": 604700,
    "tokenActiveTimeout": 1700,
    "loginDevice": "PC",
    "tokenValueList": [
      "登录后返回的tokenValue"
    ]
  }
}
```

注销当前 Token 后再次验证：

```bash
curl -X POST "http://localhost:8080/token/logout-current" \
  -H "Authorization: Bearer 登录后返回的tokenValue"

curl -X GET "http://localhost:8080/token/info" \
  -H "Authorization: Bearer 登录后返回的tokenValue"
```

如果第二次调用返回未登录或 Token 无效，说明 Token 注销已经生效，Redis 中对应登录态也已被清理。

常见问题排查如下：

| 问题                    | 可能原因                   | 处理方式                                            |
| ----------------------- | -------------------------- | --------------------------------------------------- |
| 项目启动失败            | Redis 地址、端口、密码错误 | 检查 `spring.data.redis` 配置                       |
| 登录成功但 Redis 无 Key | Redis 集成依赖未生效       | 检查是否引入 `sa-token-redis-jackson`               |
| 服务重启后 Token 失效   | 实际仍使用内存存储         | 检查 Sa-Token Redis 依赖版本是否与 Starter 版本一致 |
| Redis Key 不可读        | 使用了 JDK 序列化版本      | 改用 Jackson 序列化集成包                           |
| 多实例登录态不一致      | 实例未连接同一个 Redis     | 检查各实例 Redis 配置                               |
| 前端传 Token 后仍未登录 | 请求头格式错误             | 确认格式为 `Authorization: Bearer token值`          |

Redis 持久化集成完成后，原有登录、鉴权、Token 续期、Token 注销和多端登录控制代码不需要改造，仍然继续使用 `StpUtil` API。后续章节可以继续补充前后端 Token 传递、全局异常处理、未登录响应、无权限响应和接口测试。



## 前后端交互

本章用于说明前端如何携带 Token 请求后端，后端登录接口应返回哪些字段，以及前端如何处理未登录和无权限响应。前后端交互的关键点是统一 Token 请求头、统一登录响应结构、统一业务状态码和统一异常响应格式。

### 请求头 Token 传递

前后端分离项目中，前端登录成功后应保存后端返回的 Token，并在后续请求中通过请求头传递。本文档前面配置了 `token-name: Authorization` 和 `token-prefix: Bearer`，因此前端请求头格式应保持为：

```text
Authorization: Bearer token值
```

Bearer Token 的常见传递方式就是把访问令牌放在 `Authorization` 请求头中，并使用 `Bearer <token>` 格式。([Yaak](https://yaak.app/docs/authentication/bearer-token?utm_source=chatgpt.com))

后端 Sa-Token 配置需要保持如下约定：

```yaml
sa-token:
  # 前端请求头名称
  token-name: Authorization

  # 前端请求头前缀
  token-prefix: Bearer

  # 从请求头读取 Token
  is-read-header: true

  # 前后端分离项目通常关闭 Cookie 读取
  is-read-cookie: false
```

前端保存 Token 时，建议保存完整认证值 `authorization`，这样后续请求可以直接放入请求头，避免每次手动拼接 `Bearer`。

登录成功后保存 Token 的示例：

```typescript
// 保存登录响应中的认证信息
localStorage.setItem('authorization', loginResult.authorization)
localStorage.setItem('tokenValue', loginResult.tokenValue)
localStorage.setItem('tokenName', loginResult.tokenName)
```

Vue 3 + Axios 项目中，可以通过请求拦截器统一追加 Token。

文件位置：`src/utils/request.ts`

```typescript
import axios, { AxiosError, AxiosInstance, InternalAxiosRequestConfig } from 'axios'

interface Result<T = unknown> {
  code: number
  message: string
  data: T
}

const request: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
  timeout: 15000
})

request.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const authorization = localStorage.getItem('authorization')

    if (authorization) {
      config.headers.Authorization = authorization
    }

    return config
  },
  (error: AxiosError) => {
    return Promise.reject(error)
  }
)

request.interceptors.response.use(
  (response) => {
    const result = response.data as Result

    if (result.code === 200) {
      return result.data
    }

    if (result.code === 401) {
      localStorage.removeItem('authorization')
      localStorage.removeItem('tokenValue')
      localStorage.removeItem('tokenName')
      window.location.href = '/login'
      return Promise.reject(new Error(result.message || '登录状态已失效'))
    }

    if (result.code === 403) {
      return Promise.reject(new Error(result.message || '暂无权限访问'))
    }

    return Promise.reject(new Error(result.message || '请求处理失败'))
  },
  (error: AxiosError) => {
    return Promise.reject(error)
  }
)

export default request
```

请求头传递建议如下：

| 项目            | 建议                     |
| --------------- | ------------------------ |
| 请求头名称      | `Authorization`          |
| 请求头值        | `Bearer token值`         |
| 前端保存字段    | 优先保存 `authorization` |
| 后端读取方式    | `is-read-header: true`   |
| 是否放在 URL    | 不建议                   |
| 是否放在 Cookie | 前后端分离项目通常不建议 |

后端接口测试示例：

```bash
curl -X GET "http://localhost:8080/user/current" \
  -H "Authorization: Bearer 登录后返回的tokenValue"
```

如果后端配置了 `token-prefix: Bearer`，前端必须传递 `Bearer token值`，只传递裸 Token 可能会被 Sa-Token 判断为未按指定前缀提交 Token。

### 登录响应结构

登录响应结构应同时满足前端存储、请求头传递、登录态展示和过期时间判断的需要。建议登录接口返回用户基础信息、Token 名称、Token 前缀、Token 值、完整认证值和 Token 剩余有效期。

推荐登录响应结构如下：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "userId": 1,
    "username": "admin",
    "nickname": "系统管理员",
    "tokenName": "Authorization",
    "tokenPrefix": "Bearer",
    "tokenValue": "token-value",
    "authorization": "Bearer token-value",
    "tokenTimeout": 604800,
    "device": "PC"
  }
}
```

字段说明如下：

| 字段            | 说明                               |
| --------------- | ---------------------------------- |
| `userId`        | 当前登录用户 ID                    |
| `username`      | 登录账号                           |
| `nickname`      | 用户昵称                           |
| `tokenName`     | 请求头名称，通常为 `Authorization` |
| `tokenPrefix`   | Token 前缀，通常为 `Bearer`        |
| `tokenValue`    | 原始 Token 值                      |
| `authorization` | 可直接放入请求头的完整认证值       |
| `tokenTimeout`  | Token 剩余有效期，单位为秒         |
| `device`        | 当前登录设备，例如 `PC`、`APP`     |

前端登录方法示例：

文件位置：`src/api/auth.ts`

```typescript
import request from '@/utils/request'

export interface LoginRequest {
  username: string
  password: string
  rememberMe?: boolean
  device?: string
}

export interface LoginResponse {
  userId: number
  username: string
  nickname: string
  tokenName: string
  tokenPrefix: string
  tokenValue: string
  authorization: string
  tokenTimeout: number
  device: string
}

export function login(data: LoginRequest): Promise<LoginResponse> {
  return request.post('/auth/login', data)
}

export function logout(): Promise<void> {
  return request.post('/auth/logout')
}

export function getLoginStatus() {
  return request.get('/auth/status')
}
```

前端登录页调用示例：

```typescript
import { login } from '@/api/auth'

async function handleLogin() {
  const result = await login({
    username: 'admin',
    password: '123456',
    rememberMe: true,
    device: 'PC'
  })

  localStorage.setItem('authorization', result.authorization)
  localStorage.setItem('tokenValue', result.tokenValue)
  localStorage.setItem('tokenName', result.tokenName)
}
```

登录响应结构建议保持稳定，不建议频繁变更字段名称。前端只需要关心 `authorization` 和用户基础信息，后端仍然以 `tokenValue` 作为真实 Token 值进行认证。

### 未登录响应处理

未登录通常发生在以下场景：请求未携带 Token、Token 无效、Token 已过期、Token 被踢下线、Token 被顶下线、Token 前缀错误等。Sa-Token 中 `NotLoginException` 表示会话未能通过登录认证校验。([JavaDoc](https://javadoc.io/static/cn.dev33/sa-token-core/1.44.0/cn/dev33/satoken/exception/package-summary.html?utm_source=chatgpt.com))

推荐未登录响应结构如下：

```json
{
  "code": 401,
  "message": "登录状态已失效，请重新登录",
  "data": null
}
```

后端处理建议：

| 场景           | 响应码 | 响应信息                           |
| -------------- | ------ | ---------------------------------- |
| 未携带 Token   | `401`  | `请先登录`                         |
| Token 无效     | `401`  | `登录状态无效，请重新登录`         |
| Token 已过期   | `401`  | `登录状态已过期，请重新登录`       |
| Token 被踢下线 | `401`  | `账号已在其他位置下线，请重新登录` |
| Token 被顶下线 | `401`  | `账号已在其他位置登录，请重新登录` |
| Token 前缀错误 | `401`  | `Token格式错误，请重新登录`        |

前端收到 `401` 后建议执行以下操作：

1. 清理本地 Token。
2. 清理用户信息、菜单、按钮权限等缓存。
3. 跳转登录页。
4. 携带当前页面地址作为重定向参数。
5. 避免重复弹出多个登录失效提示。

前端处理示例：

```typescript
function handleUnauthorized(message: string) {
  localStorage.removeItem('authorization')
  localStorage.removeItem('tokenValue')
  localStorage.removeItem('tokenName')

  const redirect = encodeURIComponent(window.location.pathname + window.location.search)
  window.location.href = `/login?redirect=${redirect}`

  console.warn(message || '登录状态已失效，请重新登录')
}
```

未登录响应不建议返回 HTTP 500，也不建议返回模糊的 `系统异常`。前端需要根据明确的 `401` 业务码判断是否跳转登录页。

### 无权限响应处理

无权限通常发生在以下场景：用户已登录，但没有访问接口所需的角色或权限码。Sa-Token 中 `NotPermissionException` 表示没有指定权限码，`NotRoleException` 表示没有指定角色标识。([JavaDoc](https://javadoc.io/static/cn.dev33/sa-token-core/1.15.2/cn/dev33/satoken/exception/package-summary.html?utm_source=chatgpt.com))

推荐无权限响应结构如下：

```json
{
  "code": 403,
  "message": "暂无权限访问该资源",
  "data": null
}
```

无权限处理建议如下：

| 场景         | 响应码 | 前端处理                   |
| ------------ | ------ | -------------------------- |
| 缺少角色     | `403`  | 提示暂无权限，不跳转登录页 |
| 缺少权限码   | `403`  | 提示暂无权限，不跳转登录页 |
| 菜单权限不足 | `403`  | 跳转 403 页面              |
| 按钮权限不足 | `403`  | 前端隐藏按钮，后端仍需拦截 |
| 接口权限不足 | `403`  | 提示暂无权限               |

前端收到 `403` 后不应清理 Token，也不应强制跳转登录页。`401` 表示认证失败，`403` 表示认证成功但授权失败，二者需要区分处理。

前端处理示例：

```typescript
function handleForbidden(message: string) {
  console.warn(message || '暂无权限访问该资源')

  if (window.location.pathname !== '/403') {
    window.location.href = '/403'
  }
}
```

对于管理后台系统，前端可以根据后端返回的菜单权限控制页面可见性，根据按钮权限控制按钮显示；但这些只属于前端体验优化，后端接口仍必须使用 `@SaCheckPermission`、`@SaCheckRole` 或路由拦截器进行权限校验。

## 全局异常处理

本章用于统一处理 Sa-Token 异常、参数校验异常、业务异常和系统异常。统一异常处理可以避免 Controller 中重复编写 `try-catch`，并保证前端始终接收到结构一致的响应结果。

### 未登录异常处理

未登录异常主要处理 `NotLoginException`。该异常通常由 `StpUtil.checkLogin()`、`@SaCheckLogin`、路由拦截器登录校验等触发。Sa-Token 的 `StpUtil.checkLogin()` 用于检验当前会话是否已经登录，未登录时会抛出异常。([JavaDoc](https://javadoc.io/static/cn.dev33/sa-token-core/1.44.0/cn/dev33/satoken/stp/StpUtil.html?utm_source=chatgpt.com))

本章节涉及文件如下：

```text
src/main/java/io/github/atengk
├── common
│   ├── exception
│   │   └── GlobalExceptionHandler.java
│   └── result
│       ├── Result.java
│       └── ResultCode.java
```

下面先定义统一响应码。

文件位置：`src/main/java/io/github/atengk/common/result/ResultCode.java`

```java
package io.github.atengk.common.result;

import lombok.Getter;

/**
 * 统一响应码
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Getter
public enum ResultCode {

    /**
     * 操作成功
     */
    SUCCESS(200, "操作成功"),

    /**
     * 请求参数错误
     */
    BAD_REQUEST(400, "请求参数错误"),

    /**
     * 未登录或登录状态失效
     */
    UNAUTHORIZED(401, "登录状态已失效，请重新登录"),

    /**
     * 无权限访问
     */
    FORBIDDEN(403, "暂无权限访问该资源"),

    /**
     * 资源不存在
     */
    NOT_FOUND(404, "资源不存在"),

    /**
     * 系统异常
     */
    ERROR(500, "系统异常，请稍后重试");

    private final Integer code;

    private final String message;

    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
```

下面是增强后的统一返回结果。该类可以替换前文中较简化的 `Result` 类。

文件位置：`src/main/java/io/github/atengk/common/result/Result.java`

```java
package io.github.atengk.common.result;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一接口返回结果
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

    /**
     * 状态码
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
     * 操作成功
     *
     * @return 统一结果
     */
    public static <T> Result<T> success() {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), null);
    }

    /**
     * 操作成功
     *
     * @param data 返回数据
     * @return 统一结果
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
    }

    /**
     * 操作失败
     *
     * @param message 失败消息
     * @return 统一结果
     */
    public static <T> Result<T> fail(String message) {
        return new Result<>(ResultCode.ERROR.getCode(), message, null);
    }

    /**
     * 操作失败
     *
     * @param code    状态码
     * @param message 失败消息
     * @return 统一结果
     */
    public static <T> Result<T> fail(Integer code, String message) {
        return new Result<>(code, message, null);
    }

    /**
     * 操作失败
     *
     * @param resultCode 响应码
     * @return 统一结果
     */
    public static <T> Result<T> fail(ResultCode resultCode) {
        return new Result<>(resultCode.getCode(), resultCode.getMessage(), null);
    }

    /**
     * 未登录
     *
     * @param message 提示信息
     * @return 统一结果
     */
    public static <T> Result<T> unauthorized(String message) {
        return new Result<>(ResultCode.UNAUTHORIZED.getCode(), message, null);
    }

    /**
     * 无权限
     *
     * @param message 提示信息
     * @return 统一结果
     */
    public static <T> Result<T> forbidden(String message) {
        return new Result<>(ResultCode.FORBIDDEN.getCode(), message, null);
    }
}
```

下面是全局异常处理类，统一处理未登录、权限不足、Token 异常、参数校验异常和系统异常。

文件位置：`src/main/java/io/github/atengk/common/exception/GlobalExceptionHandler.java`

```java
package io.github.atengk.common.exception;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import cn.dev33.satoken.exception.SaTokenException;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.common.result.Result;
import io.github.atengk.common.result.ResultCode;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理 Sa-Token 未登录异常
     *
     * @param exception 未登录异常
     * @return 统一结果
     */
    @ExceptionHandler(NotLoginException.class)
    public Result<Void> handleNotLoginException(NotLoginException exception) {
        String message = buildNotLoginMessage(exception);
        log.warn("登录状态校验失败，类型：{}，信息：{}", exception.getType(), exception.getMessage());
        return Result.unauthorized(message);
    }

    /**
     * 处理 Sa-Token 权限码不足异常
     *
     * @param exception 权限不足异常
     * @return 统一结果
     */
    @ExceptionHandler(NotPermissionException.class)
    public Result<Void> handleNotPermissionException(NotPermissionException exception) {
        log.warn("权限码校验失败：{}", exception.getMessage());
        return Result.forbidden("暂无权限访问该资源");
    }

    /**
     * 处理 Sa-Token 角色不足异常
     *
     * @param exception 角色不足异常
     * @return 统一结果
     */
    @ExceptionHandler(NotRoleException.class)
    public Result<Void> handleNotRoleException(NotRoleException exception) {
        log.warn("角色校验失败：{}", exception.getMessage());
        return Result.forbidden("暂无权限访问该资源");
    }

    /**
     * 处理 Sa-Token 通用异常
     *
     * @param exception Sa-Token 异常
     * @return 统一结果
     */
    @ExceptionHandler(SaTokenException.class)
    public Result<Void> handleSaTokenException(SaTokenException exception) {
        log.warn("Sa-Token 认证异常，错误码：{}，信息：{}", exception.getCode(), exception.getMessage());
        return Result.fail(ResultCode.UNAUTHORIZED.getCode(), "认证状态异常，请重新登录");
    }

    /**
     * 处理请求体参数校验异常
     *
     * @param exception 参数校验异常
     * @return 统一结果
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult()
                .getAllErrors()
                .stream()
                .map(ObjectError::getDefaultMessage)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.joining("；"));

        log.warn("请求体参数校验失败：{}", message);
        return Result.fail(ResultCode.BAD_REQUEST.getCode(), StrUtil.blankToDefault(message, "请求参数错误"));
    }

    /**
     * 处理请求参数约束异常
     *
     * @param exception 参数约束异常
     * @return 统一结果
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraintViolationException(ConstraintViolationException exception) {
        String message = exception.getConstraintViolations()
                .stream()
                .map(ConstraintViolation::getMessage)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.joining("；"));

        log.warn("请求参数约束校验失败：{}", message);
        return Result.fail(ResultCode.BAD_REQUEST.getCode(), StrUtil.blankToDefault(message, "请求参数错误"));
    }

    /**
     * 处理缺少请求参数异常
     *
     * @param exception 缺少请求参数异常
     * @return 统一结果
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<Void> handleMissingServletRequestParameterException(MissingServletRequestParameterException exception) {
        String message = StrUtil.format("缺少必要请求参数：{}", exception.getParameterName());
        log.warn(message);
        return Result.fail(ResultCode.BAD_REQUEST.getCode(), message);
    }

    /**
     * 处理业务参数异常
     *
     * @param exception 业务参数异常
     * @return 统一结果
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public Result<Void> handleIllegalArgumentException(IllegalArgumentException exception) {
        log.warn("业务参数异常：{}", exception.getMessage());
        return Result.fail(ResultCode.BAD_REQUEST.getCode(), StrUtil.blankToDefault(exception.getMessage(), "请求参数错误"));
    }

    /**
     * 处理系统未知异常
     *
     * @param exception 未知异常
     * @return 统一结果
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception exception) {
        log.error("系统异常", exception);
        return Result.fail(ResultCode.ERROR);
    }

    /**
     * 构建未登录提示信息
     *
     * @param exception 未登录异常
     * @return 提示信息
     */
    private String buildNotLoginMessage(NotLoginException exception) {
        String type = exception.getType();

        if (StrUtil.equals(type, NotLoginException.NOT_TOKEN)) {
            return "请先登录";
        }

        if (StrUtil.equals(type, NotLoginException.INVALID_TOKEN)) {
            return "登录状态无效，请重新登录";
        }

        if (StrUtil.equals(type, NotLoginException.TOKEN_TIMEOUT)) {
            return "登录状态已过期，请重新登录";
        }

        if (StrUtil.equals(type, NotLoginException.BE_REPLACED)) {
            return "账号已在其他位置登录，请重新登录";
        }

        if (StrUtil.equals(type, NotLoginException.KICK_OUT)) {
            return "账号已被下线，请重新登录";
        }

        if (StrUtil.equals(type, NotLoginException.NO_PREFIX)) {
            return "Token格式错误，请重新登录";
        }

        return "登录状态已失效，请重新登录";
    }
}
```

`NotLoginException` 在 Sa-Token 的索引中包含 `NOT_TOKEN`、`KICK_OUT`、`NO_PREFIX` 等异常类型常量，适合在全局异常处理中细分未登录原因。([JavaDoc](https://javadoc.io/static/cn.dev33/sa-token-core/1.44.0/index-all.html?utm_source=chatgpt.com))

### 权限不足异常处理

权限不足异常主要包括 `NotPermissionException` 和 `NotRoleException`。这类异常说明用户已经登录，但没有访问指定资源的权限，因此应返回 `403`，不应清理前端 Token，也不应跳转登录页。

推荐处理方式如下：

```java
@ExceptionHandler(NotPermissionException.class)
public Result<Void> handleNotPermissionException(NotPermissionException exception) {
    log.warn("权限码校验失败：{}", exception.getMessage());
    return Result.forbidden("暂无权限访问该资源");
}

@ExceptionHandler(NotRoleException.class)
public Result<Void> handleNotRoleException(NotRoleException exception) {
    log.warn("角色校验失败：{}", exception.getMessage());
    return Result.forbidden("暂无权限访问该资源");
}
```

响应示例：

```json
{
  "code": 403,
  "message": "暂无权限访问该资源",
  "data": null
}
```

前端处理逻辑应与未登录区分开：

| 响应码 | 含义                 | 前端处理                              |
| ------ | -------------------- | ------------------------------------- |
| `401`  | 未登录或登录状态失效 | 清理 Token，跳转登录页                |
| `403`  | 已登录但权限不足     | 保留 Token，提示无权限或跳转 403 页面 |
| `500`  | 系统异常             | 提示系统异常                          |

权限不足处理建议不要把详细权限码直接暴露给普通用户，例如不要返回 `缺少 system:user:delete 权限`。这类详细信息可以写入后端日志，前端只显示通用提示。

### Token 异常处理

Token 异常可以分为未携带 Token、Token 无效、Token 过期、Token 被踢下线、Token 被顶下线、Token 前缀错误等。大部分 Token 状态异常会进入 `NotLoginException`，部分 Sa-Token 框架层异常会进入 `SaTokenException`。`SaTokenException` 是 Sa-Token 框架异常父类，Javadoc 中列出了多个直接子类，包括 `NotLoginException`、`NotPermissionException`、`NotRoleException`、`NotSafeException`、`SameTokenInvalidException` 等。([JavaDoc](https://javadoc.io/static/cn.dev33/sa-token-core/1.44.0/cn/dev33/satoken/exception/SaTokenException.html?utm_source=chatgpt.com))

Token 异常处理策略如下：

| 异常类型          | 处理方式                             |
| ----------------- | ------------------------------------ |
| 未携带 Token      | 返回 `401`，提示请先登录             |
| Token 无效        | 返回 `401`，提示重新登录             |
| Token 过期        | 返回 `401`，提示登录过期             |
| Token 被踢下线    | 返回 `401`，提示账号已被下线         |
| Token 被顶下线    | 返回 `401`，提示账号已在其他位置登录 |
| Token 前缀错误    | 返回 `401`，提示 Token 格式错误      |
| Sa-Token 其他异常 | 返回 `401` 或统一认证异常            |

推荐响应示例：

```json
{
  "code": 401,
  "message": "登录状态已过期，请重新登录",
  "data": null
}
```

如果系统希望同时返回异常类型，建议只在开发环境返回，不建议生产环境直接暴露：

```json
{
  "code": 401,
  "message": "登录状态已过期，请重新登录",
  "data": null
}
```

生产环境不建议返回如下信息：

```json
{
  "code": 401,
  "message": "TOKEN_TIMEOUT: token已过期",
  "data": null
}
```

原因是前端只需要知道是否需要重新登录，具体 Token 异常细节应保留在后端日志中，便于排查问题即可。

### 统一返回结果封装

统一返回结果封装的目标是让所有接口，无论成功、失败、未登录、无权限、参数错误还是系统异常，都返回相同的数据结构。这样前端只需要基于 `code`、`message`、`data` 三个字段进行统一处理。

推荐统一结构如下：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {}
}
```

响应码建议如下：

| code  | 含义         | 使用场景                         |
| ----- | ------------ | -------------------------------- |
| `200` | 操作成功     | 查询、新增、修改、删除成功       |
| `400` | 请求参数错误 | 参数为空、格式错误、校验失败     |
| `401` | 未登录       | Token 缺失、无效、过期、被踢下线 |
| `403` | 无权限       | 缺少角色或权限码                 |
| `404` | 资源不存在   | 用户、角色、菜单等资源不存在     |
| `500` | 系统异常     | 未知异常、服务内部异常           |

接口响应示例：

成功响应：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 1,
    "username": "admin"
  }
}
```

参数错误响应：

```json
{
  "code": 400,
  "message": "账号不能为空",
  "data": null
}
```

未登录响应：

```json
{
  "code": 401,
  "message": "登录状态已失效，请重新登录",
  "data": null
}
```

无权限响应：

```json
{
  "code": 403,
  "message": "暂无权限访问该资源",
  "data": null
}
```

系统异常响应：

```json
{
  "code": 500,
  "message": "系统异常，请稍后重试",
  "data": null
}
```

建议在前端 Axios 响应拦截器中统一判断 `code`，不要在每个页面单独处理登录失效和无权限逻辑：

```typescript
if (result.code === 401) {
  localStorage.removeItem('authorization')
  window.location.href = '/login'
  return Promise.reject(new Error(result.message))
}

if (result.code === 403) {
  window.location.href = '/403'
  return Promise.reject(new Error(result.message))
}
```

至此，前后端 Token 交互、登录响应结构、未登录处理、无权限处理和全局异常处理已经形成闭环。后续接口测试章节可以基于这些统一响应结构，分别测试登录接口、鉴权接口、角色权限、Token 失效和 Redis 持久化效果。



## 接口测试

本章用于验证 Sa-Token 登录认证、登录态校验、角色权限控制、Token 注销、Token 失效和 Redis 持久化是否正常。测试建议按照“先登录获取 Token，再携带 Token 访问受保护接口，最后验证异常场景”的顺序进行。

### 登录接口测试

登录接口测试用于确认账号密码认证、登录态写入、Token 返回结构是否正常。测试前需要保证后端服务已启动，Redis 已正常连接，且示例用户 `admin / 123456` 可以通过认证。

测试接口信息如下：

| 项目           | 内容               |
| -------------- | ------------------ |
| 请求路径       | `/auth/login`      |
| 请求方法       | `POST`             |
| 是否需要 Token | 否                 |
| 请求类型       | `application/json` |
| 测试账号       | `admin`            |
| 测试密码       | `123456`           |

登录成功测试命令如下：

```bash
curl -X POST "http://localhost:8080/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "123456",
    "rememberMe": true,
    "device": "PC"
  }'
```

预期响应如下：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "userId": 1,
    "username": "admin",
    "nickname": "系统管理员",
    "tokenName": "Authorization",
    "tokenPrefix": "Bearer",
    "tokenValue": "登录后返回的tokenValue",
    "authorization": "Bearer 登录后返回的tokenValue",
    "tokenTimeout": 604800,
    "device": "PC"
  }
}
```

登录失败测试命令如下：

```bash
curl -X POST "http://localhost:8080/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "wrong-password",
    "rememberMe": false,
    "device": "PC"
  }'
```

预期响应如下：

```json
{
  "code": 400,
  "message": "账号或密码错误",
  "data": null
}
```

参数校验测试命令如下：

```bash
curl -X POST "http://localhost:8080/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "",
    "password": "",
    "rememberMe": false,
    "device": "PC"
  }'
```

预期响应如下：

```json
{
  "code": 400,
  "message": "账号不能为空；密码不能为空",
  "data": null
}
```

登录接口测试通过的判断标准如下：

| 检查项           | 预期结果                                     |
| ---------------- | -------------------------------------------- |
| 正确账号密码登录 | 返回 `code = 200`                            |
| 返回 Token       | `data.tokenValue` 不为空                     |
| 返回完整认证值   | `data.authorization` 格式为 `Bearer token值` |
| 错误密码登录     | 返回 `code = 400`                            |
| 空参数登录       | 返回参数校验错误                             |
| Redis 持久化     | Redis 中出现 Sa-Token 相关 Key               |

如果登录接口返回成功，但后续接口仍提示未登录，需要优先检查前端或 curl 是否正确传递请求头：`Authorization: Bearer token值`。

### 鉴权接口测试

鉴权接口测试用于确认后端是否能够正确识别当前请求的登录态。测试时需要先调用登录接口获取 Token，然后把返回的 `authorization` 字段放入请求头中访问受保护接口。

假设登录接口返回：

```text
Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.mock-token
```

可以在终端中先保存变量，方便后续测试：

```bash
TOKEN="Bearer 登录后返回的tokenValue"
```

测试当前登录用户接口：

```bash
curl -X GET "http://localhost:8080/user/current" \
  -H "Authorization: ${TOKEN}"
```

预期响应如下：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "userId": 1,
    "username": "admin",
    "nickname": "系统管理员",
    "roles": [
      "admin",
      "user"
    ],
    "permissions": [
      "system:user:query",
      "system:user:add",
      "system:user:update",
      "system:user:delete"
    ]
  }
}
```

测试登录状态接口：

```bash
curl -X GET "http://localhost:8080/auth/status" \
  -H "Authorization: ${TOKEN}"
```

预期响应如下：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "login": true,
    "userId": 1,
    "tokenName": "Authorization",
    "tokenValue": "登录后返回的tokenValue",
    "tokenTimeout": 604800,
    "loginDevice": "PC"
  }
}
```

测试不携带 Token 访问受保护接口：

```bash
curl -X GET "http://localhost:8080/user/current"
```

预期响应如下：

```json
{
  "code": 401,
  "message": "请先登录",
  "data": null
}
```

测试 Token 前缀错误：

```bash
curl -X GET "http://localhost:8080/user/current" \
  -H "Authorization: 登录后返回的tokenValue"
```

如果后端配置了 `token-prefix: Bearer`，此时预期返回未登录或 Token 格式错误。

鉴权接口测试通过的判断标准如下：

| 检查项         | 预期结果                        |
| -------------- | ------------------------------- |
| 携带正确 Token | 可以访问受保护接口              |
| 不携带 Token   | 返回 `401`                      |
| Token 前缀错误 | 返回 `401`                      |
| 登录状态接口   | 返回 `login = true`             |
| 当前用户接口   | 可以获取当前用户 ID、角色和权限 |

### 角色权限测试

角色权限测试用于确认 `StpInterface` 是否正确返回当前用户的角色和权限码，并验证 `@SaCheckRole`、`@SaCheckPermission`、`StpUtil.checkRole()`、`StpUtil.checkPermission()` 是否生效。

测试管理员角色接口：

```bash
curl -X GET "http://localhost:8080/permission-test/admin" \
  -H "Authorization: ${TOKEN}"
```

预期响应如下：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": "管理员角色可以访问"
}
```

测试用户查询权限接口：

```bash
curl -X GET "http://localhost:8080/permission-test/user-query" \
  -H "Authorization: ${TOKEN}"
```

预期响应如下：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": "拥有用户查询权限可以访问"
}
```

测试系统用户列表接口：

```bash
curl -X GET "http://localhost:8080/system/user/list" \
  -H "Authorization: ${TOKEN}"
```

预期响应如下：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    "admin",
    "test",
    "guest"
  ]
}
```

测试新增用户权限：

```bash
curl -X POST "http://localhost:8080/system/user?username=test01" \
  -H "Authorization: ${TOKEN}"
```

预期响应如下：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": "新增用户成功：test01"
}
```

测试修改用户权限：

```bash
curl -X PUT "http://localhost:8080/system/user/2?username=test02" \
  -H "Authorization: ${TOKEN}"
```

预期响应如下：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": "修改用户成功：test02"
}
```

测试删除用户权限：

```bash
curl -X DELETE "http://localhost:8080/system/user/2" \
  -H "Authorization: ${TOKEN}"
```

预期响应如下：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": "删除用户成功：2"
}
```

如果需要测试无权限场景，可以把 `PermissionServiceImpl` 中用户 `1L` 返回的某个权限临时移除，例如移除 `system:user:delete`，然后重新登录并调用删除接口。预期响应如下：

```json
{
  "code": 403,
  "message": "暂无权限访问该资源",
  "data": null
}
```

角色权限测试通过的判断标准如下：

| 检查项           | 预期结果                           |
| ---------------- | ---------------------------------- |
| `admin` 角色接口 | 管理员用户可访问                   |
| 权限码接口       | 拥有对应权限码时可访问             |
| 缺少权限码       | 返回 `403`                         |
| 缺少角色         | 返回 `403`                         |
| 全局异常处理     | 不返回默认异常堆栈                 |
| 后端日志         | 能看到权限校验失败或成功的关键日志 |

权限测试时不要只依赖前端菜单或按钮是否隐藏。前端权限控制只用于改善体验，真正的接口权限必须由后端注解或拦截器校验。

### Token 失效测试

Token 失效测试用于确认 Token 注销、Token 过期、Token 被踢下线、Token 活跃超时等场景是否能够正确返回 `401`，并验证前端是否可以清理登录状态并跳转登录页。

测试当前 Token 信息：

```bash
curl -X GET "http://localhost:8080/token/info" \
  -H "Authorization: ${TOKEN}"
```

预期响应如下：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "login": true,
    "userId": 1,
    "tokenName": "Authorization",
    "tokenValue": "登录后返回的tokenValue",
    "authorization": "Bearer 登录后返回的tokenValue",
    "tokenTimeout": 604800,
    "tokenActiveTimeout": 1800,
    "loginDevice": "PC",
    "tokenValueList": [
      "登录后返回的tokenValue"
    ]
  }
}
```

测试注销当前 Token：

```bash
curl -X POST "http://localhost:8080/token/logout-current" \
  -H "Authorization: ${TOKEN}"
```

预期响应如下：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": null
}
```

注销后再次访问受保护接口：

```bash
curl -X GET "http://localhost:8080/user/current" \
  -H "Authorization: ${TOKEN}"
```

预期响应如下：

```json
{
  "code": 401,
  "message": "登录状态已失效，请重新登录",
  "data": null
}
```

测试 Token 续期：

```bash
curl -X POST "http://localhost:8080/token/renew" \
  -H "Content-Type: application/json" \
  -H "Authorization: ${TOKEN}" \
  -d '{
    "timeout": 604800
  }'
```

预期响应中 `tokenTimeout` 应被更新为接近 `604800` 的值。

测试活跃时间刷新：

```bash
curl -X POST "http://localhost:8080/token/refresh-active" \
  -H "Authorization: ${TOKEN}"
```

预期响应中 `tokenActiveTimeout` 应恢复为配置或登录时指定的活跃有效期附近。

测试 Token 过期可以在开发环境临时把有效期调短：

```yaml
sa-token:
  # 开发环境临时设置为 60 秒，便于测试过期
  timeout: 60

  # 开发环境临时设置为 30 秒，便于测试无操作冻结
  active-timeout: 30
```

修改配置后重新启动项目，登录获取新 Token，等待超过配置时间后再次访问接口。预期返回 `401`。

Token 失效测试通过的判断标准如下：

| 场景            | 预期结果                          |
| --------------- | --------------------------------- |
| 当前 Token 有效 | `/token/info` 返回 `login = true` |
| 当前 Token 注销 | 后续访问受保护接口返回 `401`      |
| Token 过期      | 返回 `401`                        |
| Token 活跃超时  | 返回 `401` 或登录状态失效         |
| Token 续期      | `tokenTimeout` 更新               |
| 刷新活跃时间    | `tokenActiveTimeout` 更新         |
| 前端收到 `401`  | 清理 Token 并跳转登录页           |

可以编写一个简单脚本串联主要测试流程。

文件位置：`scripts/test-sa-token.sh`

```bash
#!/usr/bin/env bash

set -e

BASE_URL="http://localhost:8080"

echo "1. 登录获取 Token"
LOGIN_RESULT=$(curl -s -X POST "${BASE_URL}/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "123456",
    "rememberMe": true,
    "device": "PC"
  }')

echo "${LOGIN_RESULT}"

TOKEN_VALUE=$(echo "${LOGIN_RESULT}" | sed -n 's/.*"tokenValue":"\([^"]*\)".*/\1/p')
AUTHORIZATION="Bearer ${TOKEN_VALUE}"

echo "2. 校验当前登录用户"
curl -s -X GET "${BASE_URL}/user/current" \
  -H "Authorization: ${AUTHORIZATION}"
echo

echo "3. 校验 Token 信息"
curl -s -X GET "${BASE_URL}/token/info" \
  -H "Authorization: ${AUTHORIZATION}"
echo

echo "4. 校验权限接口"
curl -s -X GET "${BASE_URL}/system/user/list" \
  -H "Authorization: ${AUTHORIZATION}"
echo

echo "5. 注销当前 Token"
curl -s -X POST "${BASE_URL}/token/logout-current" \
  -H "Authorization: ${AUTHORIZATION}"
echo

echo "6. 注销后再次访问受保护接口"
curl -s -X GET "${BASE_URL}/user/current" \
  -H "Authorization: ${AUTHORIZATION}"
echo
```

执行命令如下：

```bash
chmod +x scripts/test-sa-token.sh
./scripts/test-sa-token.sh
```

该脚本用于快速验证登录、获取当前用户、查询 Token、访问权限接口、注销 Token 和注销后访问失败等主流程。脚本中使用 `sed` 简单提取 `tokenValue`，适合本地开发测试；如果响应结构复杂，建议使用 `jq` 解析 JSON。

## 项目结构建议

本章用于给出 Spring Boot 3 + Sa-Token 项目的推荐分层结构。良好的目录结构可以让认证、鉴权、异常处理、统一响应、业务接口和权限实现保持边界清晰，避免 Sa-Token 相关代码散落在各个业务模块中。

### 配置类结构

配置类主要用于存放 Sa-Token 拦截器、跨域配置、Redis 配置、Web MVC 配置、JSON 配置、接口文档配置等。认证鉴权相关配置建议统一放在 `config` 包下。

推荐结构如下：

```text
src/main/java/io/github/atengk/config
├── SaTokenConfig.java
├── WebMvcConfig.java
├── CorsConfig.java
├── JacksonConfig.java
├── RedisConfig.java
└── OpenApiConfig.java
```

配置类职责建议如下：

| 类名            | 职责                                              |
| --------------- | ------------------------------------------------- |
| `SaTokenConfig` | 注册 Sa-Token 拦截器、路由鉴权规则、白名单        |
| `WebMvcConfig`  | Spring MVC 通用配置                               |
| `CorsConfig`    | 跨域配置，如果已在 `SaTokenConfig` 中处理，可省略 |
| `JacksonConfig` | JSON 序列化、日期格式、Long 类型处理              |
| `RedisConfig`   | RedisTemplate、序列化器、缓存配置                 |
| `OpenApiConfig` | Knife4j、Swagger、OpenAPI 接口文档配置            |

配置类设计建议如下：

1. Sa-Token 鉴权规则统一放在 `SaTokenConfig`。
2. 白名单路径建议集中维护，不要散落在多个拦截器中。
3. 跨域配置只保留一处，避免重复配置导致规则不一致。
4. 生产环境不要使用过宽的跨域来源，例如 `*`。
5. Redis 配置与 Sa-Token DAO 配置分开理解，业务代码不需要手动保存 Sa-Token 登录态。

推荐的核心配置文件如下：

```text
src/main/resources
├── application.yml
├── application-dev.yml
├── application-test.yml
└── application-prod.yml
```

环境配置建议如下：

| 文件                   | 用途         |
| ---------------------- | ------------ |
| `application.yml`      | 公共配置     |
| `application-dev.yml`  | 开发环境配置 |
| `application-test.yml` | 测试环境配置 |
| `application-prod.yml` | 生产环境配置 |

生产环境中，Redis 地址、密码、Token 有效期、跨域域名等配置应与开发环境隔离，不建议直接复用开发配置。

### Controller 结构

Controller 层负责接收 HTTP 请求、参数校验、调用 Service 和返回统一结果。Controller 不应直接写复杂业务逻辑，也不应直接拼接权限数据。登录认证、Token 管理、用户信息、系统用户管理、权限测试接口建议拆分为不同 Controller。

推荐结构如下：

```text
src/main/java/io/github/atengk/controller
├── AuthController.java
├── TokenController.java
├── UserController.java
├── SystemUserController.java
└── PermissionTestController.java
```

Controller 职责建议如下：

| 类名                       | 职责                           |
| -------------------------- | ------------------------------ |
| `AuthController`           | 登录、退出、登录状态查询       |
| `TokenController`          | Token 查询、续期、注销、踢下线 |
| `UserController`           | 当前登录用户信息、个人资料     |
| `SystemUserController`     | 系统用户管理接口               |
| `PermissionTestController` | 开发阶段验证角色权限控制       |

Controller 层编码建议如下：

1. 请求参数使用 DTO 接收，不建议直接使用 `Map`。
2. 请求体参数使用 `@Valid @RequestBody` 触发校验。
3. 路径参数使用 `@PathVariable`。
4. 查询参数使用 `@RequestParam`。
5. 返回值统一使用 `Result<T>`。
6. 接口权限优先使用 `@SaCheckPermission`。
7. 管理员专属接口可以使用 `@SaCheckRole`。
8. Controller 中不要吞异常，交给全局异常处理器统一处理。

推荐接口分层如下：

```text
/auth/**          登录认证相关接口
/token/**         Token 管理相关接口
/user/**          当前用户相关接口
/system/user/**   系统用户管理接口
/system/role/**   系统角色管理接口
/system/menu/**   系统菜单权限接口
```

这种路径设计可以配合 Sa-Token 路由拦截器做统一鉴权，例如 `/system/**` 统一要求后台管理权限，再在具体接口上使用权限码细分控制。

### Service 结构

Service 层负责业务逻辑编排，例如账号密码认证、用户状态校验、权限查询、Token 管理、角色权限缓存刷新等。Controller 只负责请求入口，真正的业务判断应放在 Service 层。

推荐结构如下：

```text
src/main/java/io/github/atengk/service
├── AuthService.java
├── TokenService.java
├── PermissionService.java
├── UserService.java
├── RoleService.java
└── MenuService.java

src/main/java/io/github/atengk/service/impl
├── AuthServiceImpl.java
├── TokenServiceImpl.java
├── PermissionServiceImpl.java
├── UserServiceImpl.java
├── RoleServiceImpl.java
└── MenuServiceImpl.java
```

Service 职责建议如下：

| 接口                | 职责                             |
| ------------------- | -------------------------------- |
| `AuthService`       | 登录认证、退出登录               |
| `TokenService`      | Token 查询、续期、注销、踢下线   |
| `PermissionService` | 查询用户角色和权限码             |
| `UserService`       | 用户基础信息、用户状态、账号密码 |
| `RoleService`       | 角色管理、用户角色分配           |
| `MenuService`       | 菜单权限、按钮权限、路由权限     |

Service 层编码建议如下：

1. 登录认证逻辑放在 `AuthService`，不要放在 Controller。
2. 密码校验、用户状态校验、登录日志记录应在 Service 层完成。
3. 角色权限查询统一通过 `PermissionService` 提供。
4. `StpInterface` 不建议直接写复杂 SQL，应调用 `PermissionService`。
5. Token 管理操作统一封装到 `TokenService`。
6. 用户权限变更后，应清理相关权限缓存。
7. 涉及重要操作时，日志应记录操作人、目标资源和操作结果。

如果项目接入数据库，推荐继续补充以下结构：

```text
src/main/java/io/github/atengk
├── entity
│   ├── SysUser.java
│   ├── SysRole.java
│   ├── SysPermission.java
│   ├── SysUserRole.java
│   └── SysRolePermission.java
├── mapper
│   ├── SysUserMapper.java
│   ├── SysRoleMapper.java
│   ├── SysPermissionMapper.java
│   ├── SysUserRoleMapper.java
│   └── SysRolePermissionMapper.java
├── dto
│   ├── LoginRequest.java
│   ├── TokenRenewRequest.java
│   └── TokenLogoutRequest.java
└── vo
    ├── LoginResponse.java
    ├── LoginStatusVO.java
    ├── TokenInfoVO.java
    └── CurrentUserVO.java
```

DTO、VO、Entity 的职责建议如下：

| 类型    | 说明             |
| ------- | ---------------- |
| DTO     | 接收前端请求参数 |
| VO      | 返回前端展示数据 |
| Entity  | 映射数据库表     |
| Mapper  | 执行数据库访问   |
| Service | 编排业务逻辑     |

这种分层可以避免接口请求对象、数据库对象和响应对象混用，后续权限字段扩展时也更容易维护。

### 权限实现类结构

权限实现类主要围绕 Sa-Token 的 `StpInterface` 展开。`StpInterface` 是 Sa-Token 获取用户角色和权限码的核心扩展点，建议单独放在 `security` 包下，并通过 `PermissionService` 查询业务权限数据。

推荐结构如下：

```text
src/main/java/io/github/atengk/security
├── SaPermissionConfig.java
├── LoginUserContext.java
├── SecurityConstant.java
├── SecurityUtils.java
└── PermissionCacheService.java
```

权限相关类职责建议如下：

| 类名                     | 职责                                              |
| ------------------------ | ------------------------------------------------- |
| `SaPermissionConfig`     | 实现 `StpInterface`，向 Sa-Token 提供角色和权限码 |
| `LoginUserContext`       | 获取当前登录用户 ID、判断登录状态                 |
| `SecurityConstant`       | 统一维护角色编码、权限码、设备类型                |
| `SecurityUtils`          | 封装常用权限判断方法                              |
| `PermissionCacheService` | 管理用户权限缓存，可选                            |

权限实现建议如下：

1. `SaPermissionConfig` 只负责适配 Sa-Token，不直接写复杂业务逻辑。
2. 用户角色和权限查询统一委托给 `PermissionService`。
3. 权限码常量集中放到 `SecurityConstant` 或枚举中。
4. 当前登录用户 ID 统一通过 `LoginUserContext` 获取。
5. 权限缓存可以按用户 ID 维度存储。
6. 用户角色或权限变更后，应主动清理权限缓存。
7. 超级管理员可以特殊处理为拥有全部权限，但要谨慎使用 `*`。

推荐的最终项目结构如下：

```text
springboot3-sa-token-demo
├── pom.xml
├── scripts
│   └── test-sa-token.sh
└── src
    ├── main
    │   ├── java
    │   │   └── io
    │   │       └── github
    │   │           └── atengk
    │   │               ├── SaTokenApplication.java
    │   │               ├── common
    │   │               │   ├── exception
    │   │               │   │   └── GlobalExceptionHandler.java
    │   │               │   └── result
    │   │               │       ├── Result.java
    │   │               │       └── ResultCode.java
    │   │               ├── config
    │   │               │   ├── SaTokenConfig.java
    │   │               │   ├── RedisConfig.java
    │   │               │   └── JacksonConfig.java
    │   │               ├── controller
    │   │               │   ├── AuthController.java
    │   │               │   ├── TokenController.java
    │   │               │   ├── UserController.java
    │   │               │   ├── SystemUserController.java
    │   │               │   └── PermissionTestController.java
    │   │               ├── dto
    │   │               │   ├── LoginRequest.java
    │   │               │   ├── TokenRenewRequest.java
    │   │               │   └── TokenLogoutRequest.java
    │   │               ├── entity
    │   │               │   ├── SysUser.java
    │   │               │   ├── SysRole.java
    │   │               │   ├── SysPermission.java
    │   │               │   ├── SysUserRole.java
    │   │               │   └── SysRolePermission.java
    │   │               ├── mapper
    │   │               │   ├── SysUserMapper.java
    │   │               │   ├── SysRoleMapper.java
    │   │               │   ├── SysPermissionMapper.java
    │   │               │   ├── SysUserRoleMapper.java
    │   │               │   └── SysRolePermissionMapper.java
    │   │               ├── security
    │   │               │   ├── SaPermissionConfig.java
    │   │               │   ├── LoginUserContext.java
    │   │               │   ├── SecurityConstant.java
    │   │               │   ├── SecurityUtils.java
    │   │               │   └── PermissionCacheService.java
    │   │               ├── service
    │   │               │   ├── AuthService.java
    │   │               │   ├── TokenService.java
    │   │               │   ├── PermissionService.java
    │   │               │   ├── UserService.java
    │   │               │   ├── RoleService.java
    │   │               │   └── MenuService.java
    │   │               ├── service
    │   │               │   └── impl
    │   │               │       ├── AuthServiceImpl.java
    │   │               │       ├── TokenServiceImpl.java
    │   │               │       ├── PermissionServiceImpl.java
    │   │               │       ├── UserServiceImpl.java
    │   │               │       ├── RoleServiceImpl.java
    │   │               │       └── MenuServiceImpl.java
    │   │               └── vo
    │   │                   ├── LoginResponse.java
    │   │                   ├── LoginStatusVO.java
    │   │                   ├── TokenInfoVO.java
    │   │                   └── CurrentUserVO.java
    │   └── resources
    │       ├── application.yml
    │       ├── application-dev.yml
    │       ├── application-test.yml
    │       └── application-prod.yml
    └── test
        └── java
            └── io
                └── github
                    └── atengk
```

项目结构落地建议如下：

| 模块          | 建议                                           |
| ------------- | ---------------------------------------------- |
| 认证登录      | 放在 `AuthController`、`AuthService`           |
| Token 管理    | 放在 `TokenController`、`TokenService`         |
| 权限加载      | 放在 `SaPermissionConfig`、`PermissionService` |
| 当前用户      | 放在 `LoginUserContext`                        |
| 统一响应      | 放在 `common.result`                           |
| 全局异常      | 放在 `common.exception`                        |
| Sa-Token 配置 | 放在 `config.SaTokenConfig`                    |
| 角色权限常量  | 放在 `security.SecurityConstant`               |
| 数据库访问    | 放在 `mapper` 和 `entity`                      |
| 请求响应对象  | 分别放在 `dto` 和 `vo`                         |

整体建议是：Controller 保持薄层，Service 负责业务编排，Security 负责 Sa-Token 适配，Common 负责通用能力，Config 负责框架配置。这样后续扩展菜单权限、按钮权限、操作日志、登录日志、权限缓存、租户隔离和微服务网关鉴权时，项目结构不会失控。