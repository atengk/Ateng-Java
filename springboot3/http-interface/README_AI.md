# Spring Boot HTTP 接口

本文档用于说明 Spring Boot 3 项目中 HTTP 接口的开发方式，重点覆盖接口分层、依赖配置、参数接收、统一响应、参数校验、异常处理、接口文档和接口测试等内容。通过统一的工程结构和开发规范，可以降低接口维护成本，提高前后端联调效率。

## 接口开发概述

本章节用于明确 HTTP 接口开发的目标、适用场景和基础技术栈。接口开发不是简单地编写 Controller 方法，而是围绕请求接收、参数校验、业务处理、响应封装、异常处理、接口文档和测试验证形成一套完整的开发流程。

### 开发目标

Spring Boot HTTP 接口开发的核心目标是提供稳定、清晰、易维护的后端服务入口。接口需要能够准确接收客户端请求，完成业务处理，并以统一、可预期的格式返回处理结果。

主要目标如下：

| 目标           | 说明                                                         |
| -------------- | ------------------------------------------------------------ |
| 统一接口风格   | 统一 URL 命名、请求方法、参数接收方式和响应结构              |
| 降低联调成本   | 前后端按照统一规范开发，减少字段、状态码、异常格式不一致的问题 |
| 提高可维护性   | Controller 只负责接口入口，业务逻辑下沉到 Service 层         |
| 增强健壮性     | 通过参数校验、全局异常处理和统一日志提高接口稳定性           |
| 方便接口管理   | 通过 OpenAPI 自动生成接口文档，便于测试和交付                |
| 支持自动化测试 | 通过单元测试、MockMvc 或接口测试工具验证接口行为             |

在实际项目中，HTTP 接口通常需要做到以下几点：

1. 请求路径语义清晰，例如 `/api/users/{id}` 表示用户资源。
2. 请求方法符合 REST 风格，例如查询使用 `GET`，新增使用 `POST`，修改使用 `PUT`，删除使用 `DELETE`。
3. 入参对象和出参对象分离，避免直接暴露数据库实体。
4. 接口返回结构统一，便于前端统一处理成功、失败和异常场景。
5. 接口异常统一拦截，避免将 Java 异常堆栈直接返回给客户端。

### 适用场景

Spring Boot HTTP 接口适用于大多数 Web 后端服务开发场景，尤其是前后端分离、微服务、管理后台、移动端接口和第三方系统对接等业务。

常见适用场景如下：

| 场景           | 示例                                         |
| -------------- | -------------------------------------------- |
| 前后端分离系统 | Vue、React、UniApp 调用 Spring Boot 后端接口 |
| 管理后台接口   | 用户管理、角色管理、菜单管理、系统配置       |
| 移动端接口     | App 登录、订单查询、消息通知、个人中心       |
| 第三方系统对接 | 支付回调、物流回调、开放平台接口             |
| 微服务内部调用 | 服务之间通过 HTTP 或 OpenFeign 调用          |
| 数据查询服务   | 报表查询、分页列表、详情接口、导出任务触发   |

不建议将所有业务逻辑直接写在 Controller 中。Controller 应保持轻量，主要处理 HTTP 协议相关内容，例如请求参数、请求头、响应结果和状态码。核心业务规则应放在 Service 层，数据访问逻辑应放在 Mapper、Repository 或 DAO 层。

### 技术栈说明

本文档默认基于 Spring Boot 3 进行 HTTP 接口开发，使用 JDK 17 及以上版本。Spring Boot 3 底层使用 Jakarta EE 规范，因此部分包名从 `javax.*` 迁移到了 `jakarta.*`。

推荐技术栈如下：

| 技术              | 版本建议         | 用途                                         |
| ----------------- | ---------------- | -------------------------------------------- |
| JDK               | 17+              | Spring Boot 3 最低要求 JDK 17                |
| Spring Boot       | 3.x              | 后端基础框架                                 |
| Spring Web        | 3.x              | 提供 HTTP 接口、Controller、JSON 序列化能力  |
| Spring Validation | 3.x              | 请求参数校验                                 |
| Jackson           | Spring Boot 内置 | JSON 序列化和反序列化                        |
| Lombok            | 1.18.x           | 简化 DTO、VO、实体类代码                     |
| Hutool            | 5.8.x            | 常用工具类，例如字符串、集合、日期、对象判断 |
| springdoc-openapi | 2.x              | 生成 OpenAPI / Swagger 接口文档              |
| JUnit 5           | Spring Boot 内置 | 单元测试和集成测试                           |
| MockMvc           | Spring Test 内置 | Controller 接口测试                          |

推荐的基础开发约定如下：

| 类型            | 约定                               |
| --------------- | ---------------------------------- |
| 基础包名        | `io.github.atengk`                 |
| 接口统一前缀    | `/api`                             |
| Controller 命名 | `XxxController`                    |
| 请求对象命名    | `XxxRequest` 或 `XxxCreateRequest` |
| 响应对象命名    | `XxxVO` 或 `XxxResponse`           |
| 统一响应类      | `Result<T>`                        |
| 异常处理类      | `GlobalExceptionHandler`           |
| 配置文件        | `application.yml`                  |

## 基础工程准备

本章节用于说明 HTTP 接口开发前需要准备的工程结构、依赖配置和基础配置文件。良好的工程结构可以让 Controller、Service、DTO、VO、异常处理和配置类职责清晰，后续扩展参数校验、接口文档和统一异常处理时也更容易维护。

### 项目结构规划

建议按照“接口层、业务层、数据层、模型对象、通用模块、配置模块”进行分层。即使是小型项目，也建议保持基础分层，避免后期业务增长后代码难以拆分。

推荐目录结构如下：

```text
spring-boot-http-api
├── pom.xml
└── src
    ├── main
    │   ├── java
    │   │   └── io
    │   │       └── github
    │   │           └── atengk
    │   │               ├── HttpApiApplication.java
    │   │               ├── common
    │   │               │   ├── exception
    │   │               │   │   ├── BizException.java
    │   │               │   │   └── GlobalExceptionHandler.java
    │   │               │   └── result
    │   │               │       ├── Result.java
    │   │               │       └── ResultCode.java
    │   │               ├── config
    │   │               │   └── OpenApiConfig.java
    │   │               └── user
    │   │                   ├── controller
    │   │                   │   └── UserController.java
    │   │                   ├── service
    │   │                   │   ├── UserService.java
    │   │                   │   └── impl
    │   │                   │       └── UserServiceImpl.java
    │   │                   ├── dto
    │   │                   │   ├── UserCreateRequest.java
    │   │                   │   └── UserUpdateRequest.java
    │   │                   └── vo
    │   │                       └── UserVO.java
    │   └── resources
    │       ├── application.yml
    │       └── logback-spring.xml
    └── test
        └── java
            └── io
                └── github
                    └── atengk
                        └── user
                            └── controller
                                └── UserControllerTest.java
```

各目录职责如下：

| 目录               | 职责                                                 |
| ------------------ | ---------------------------------------------------- |
| `controller`       | 接收 HTTP 请求，处理路径、参数、请求体、请求头和响应 |
| `service`          | 定义业务接口，承载业务编排逻辑                       |
| `service.impl`     | 实现业务逻辑，处理校验、转换、事务和外部调用         |
| `dto`              | 接收客户端请求参数，不建议直接使用实体类接收入参     |
| `vo`               | 返回给客户端的数据对象，不建议直接返回实体类         |
| `common.result`    | 存放统一响应结构、响应码枚举                         |
| `common.exception` | 存放业务异常、全局异常处理                           |
| `config`           | 存放 Spring 配置类，例如 OpenAPI、跨域、序列化配置   |
| `resources`        | 存放配置文件、日志配置、静态资源等                   |
| `test`             | 存放单元测试和接口测试代码                           |

模块较小时，可以按照业务聚合，例如 `user`、`order`、`system`。模块较大时，可以进一步拆成多模块 Maven 工程，例如 `api`、`service`、`common`、`domain`、`infrastructure`。

### 依赖配置

依赖配置用于引入 HTTP 接口开发所需的基础能力，包括 Web 接口、参数校验、接口文档、工具类和测试框架。Spring Boot 3 项目建议使用 Maven 管理依赖，并通过 `spring-boot-starter-parent` 统一依赖版本。

文件位置：`pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="
            http://maven.apache.org/POM/4.0.0
            https://maven.apache.org/xsd/maven-4.0.0.xsd">

    <modelVersion>4.0.0</modelVersion>

    <!-- Spring Boot 3 父工程：统一管理 Spring 相关依赖版本 -->
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.3.5</version>
        <relativePath/>
    </parent>

    <groupId>io.github.atengk</groupId>
    <artifactId>spring-boot-http-api</artifactId>
    <version>1.0.0</version>
    <name>spring-boot-http-api</name>
    <description>Spring Boot 3 HTTP 接口开发示例工程</description>

    <properties>
        <!-- Spring Boot 3 要求 JDK 17 及以上版本 -->
        <java.version>17</java.version>
        <hutool.version>5.8.32</hutool.version>
        <springdoc.version>2.6.0</springdoc.version>
    </properties>

    <dependencies>
        <!-- Web 接口开发：提供 Controller、REST、JSON、内置 Tomcat 等能力 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- 参数校验：支持 @Valid、@NotBlank、@NotNull、@Size 等校验注解 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <!-- OpenAPI 文档：自动生成 Swagger UI 和 OpenAPI 规范 -->
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
            <version>${springdoc.version}</version>
        </dependency>

        <!-- Hutool 工具类：字符串、集合、日期、对象、JSON 等常用工具 -->
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
            <version>${hutool.version}</version>
        </dependency>

        <!-- Lombok：简化 Getter、Setter、构造方法、日志对象等代码 -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- 测试依赖：JUnit 5、Spring Test、MockMvc 等测试能力 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <!-- Spring Boot Maven 插件：支持打包、运行 Spring Boot 应用 -->
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <!-- 打包时排除 Lombok，避免运行环境引入无用依赖 -->
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

依赖说明如下：

| 依赖                                  | 必要性   | 说明                         |
| ------------------------------------- | -------- | ---------------------------- |
| `spring-boot-starter-web`             | 必选     | HTTP 接口开发核心依赖        |
| `spring-boot-starter-validation`      | 推荐必选 | 接口参数校验依赖             |
| `springdoc-openapi-starter-webmvc-ui` | 推荐     | 自动生成接口文档             |
| `hutool-all`                          | 推荐     | 简化常用工具类处理           |
| `lombok`                              | 推荐     | 减少 DTO、VO、配置类样板代码 |
| `spring-boot-starter-test`            | 推荐必选 | 接口测试和单元测试依赖       |

项目创建完成后，可以使用以下命令检查依赖是否正常：

```bash
# 编译项目，验证 Maven 依赖、Java 版本和代码结构是否正常
mvn clean compile

# 启动项目，验证 Spring Boot 应用是否可以正常运行
mvn spring-boot:run
```

### 配置文件说明

配置文件用于集中管理服务端口、应用名称、JSON 序列化、接口文档、日志和其他运行参数。Spring Boot 项目推荐使用 `application.yml`，层级结构比 `application.properties` 更清晰。

文件位置：`src/main/resources/application.yml`

```yaml
server:
  # HTTP 服务端口
  port: 8080
  servlet:
    # 统一接口上下文路径；为空表示直接使用根路径
    context-path: /

spring:
  application:
    # 应用名称，通常用于日志、注册中心、链路追踪等场景
    name: spring-boot-http-api

  jackson:
    # JSON 日期时间格式
    date-format: yyyy-MM-dd HH:mm:ss
    # 默认时区
    time-zone: Asia/Shanghai
    # null 字段不返回，减少响应体冗余
    default-property-inclusion: non_null

  mvc:
    format:
      # GET 请求中 LocalDateTime 参数的默认格式
      date-time: yyyy-MM-dd HH:mm:ss
      date: yyyy-MM-dd
      time: HH:mm:ss

springdoc:
  api-docs:
    # OpenAPI JSON 文档访问路径
    path: /v3/api-docs
  swagger-ui:
    # Swagger UI 页面访问路径
    path: /swagger-ui.html
    # 按 HTTP 方法排序接口
    operations-sorter: method
    # 按标签排序接口分组
    tags-sorter: alpha
  default-flat-param-object: true

logging:
  level:
    # 项目包日志级别
    io.github.atengk: debug
    # Spring Web 请求处理日志级别
    org.springframework.web: info
```

常用配置项说明如下：

| 配置项                                      | 说明                                                     |
| ------------------------------------------- | -------------------------------------------------------- |
| `server.port`                               | HTTP 服务启动端口                                        |
| `server.servlet.context-path`               | 应用上下文路径，通常保持 `/`，接口前缀由 Controller 控制 |
| `spring.application.name`                   | 应用名称                                                 |
| `spring.jackson.date-format`                | JSON 日期时间序列化格式                                  |
| `spring.jackson.time-zone`                  | JSON 时间序列化时区                                      |
| `spring.jackson.default-property-inclusion` | 控制 null 字段是否返回                                   |
| `springdoc.api-docs.path`                   | OpenAPI JSON 文档地址                                    |
| `springdoc.swagger-ui.path`                 | Swagger UI 页面地址                                      |
| `logging.level.io.github.atengk`            | 当前项目包的日志级别                                     |

启动后可以通过以下地址验证基础配置是否生效：

```bash
# 访问 Swagger UI 接口文档页面
http://localhost:8080/swagger-ui.html

# 访问 OpenAPI JSON 文档
http://localhost:8080/v3/api-docs
```

如果项目后续需要区分开发、测试、生产环境，建议使用多环境配置文件：

```text
src/main/resources
├── application.yml
├── application-dev.yml
├── application-test.yml
└── application-prod.yml
```

主配置文件中指定默认环境：

```yaml
spring:
  profiles:
    # 默认启用开发环境配置
    active: dev
```

启动时也可以通过命令切换环境：

```bash
# 使用测试环境配置启动
java -jar spring-boot-http-api-1.0.0.jar --spring.profiles.active=test

# 使用生产环境配置启动
java -jar spring-boot-http-api-1.0.0.jar --spring.profiles.active=prod
```

多环境配置建议遵循以下原则：

| 环境     | 配置建议                                              |
| -------- | ----------------------------------------------------- |
| `dev`    | 日志级别可设置为 `debug`，方便本地调试                |
| `test`   | 尽量接近生产配置，用于接口联调和测试验证              |
| `prod`   | 日志级别建议为 `info`，关闭调试接口和不必要的详细日志 |
| 敏感配置 | 数据库密码、密钥、Token 等不建议直接提交到代码仓库    |



## Controller 接口设计

本章节用于说明 Controller 层的职责边界、URL 设计方式、HTTP 请求方法选择、参数接收方式和响应结果返回规范。Controller 是 HTTP 请求进入后端系统的第一层，应保持轻量，主要负责协议适配和参数接收，不应承载复杂业务逻辑。

### RestController 职责划分

`@RestController` 用于声明 REST 风格接口类，本质上是 `@Controller` 和 `@ResponseBody` 的组合。使用该注解后，方法返回值会直接写入 HTTP 响应体，通常以 JSON 格式返回给客户端。

Controller 层建议只负责以下内容：

| 职责           | 说明                                                         |
| -------------- | ------------------------------------------------------------ |
| 接收 HTTP 请求 | 通过 `@GetMapping`、`@PostMapping` 等注解声明接口            |
| 接收请求参数   | 使用 `@PathVariable`、`@RequestParam`、`@RequestBody`、`@RequestHeader` |
| 调用业务服务   | 调用 Service 层完成业务处理                                  |
| 返回统一结果   | 使用 `Result<T>` 统一封装响应数据                            |
| 简单日志记录   | 记录关键接口调用信息，不输出敏感数据                         |

Controller 层不建议处理以下内容：

| 不建议内容             | 原因                             |
| ---------------------- | -------------------------------- |
| 编写复杂业务逻辑       | 会导致接口层膨胀，难以复用和测试 |
| 直接访问数据库         | 破坏分层结构，后期维护困难       |
| 返回数据库实体类       | 容易暴露内部字段，影响接口稳定性 |
| 捕获所有异常并手动返回 | 应交给全局异常处理器统一处理     |
| 拼接复杂响应结构       | 应通过 VO、Response 对象统一组织 |

推荐的调用链路如下：

```text
客户端请求
   ↓
Controller 接收 HTTP 请求
   ↓
DTO 接收和校验参数
   ↓
Service 处理业务逻辑
   ↓
VO 组织响应数据
   ↓
Result<T> 统一返回
```

下面示例展示一个较完整的 Controller 层写法，包含查询、新增、修改、删除和请求头接收等常见接口。

文件位置：`src/main/java/io/github/atengk/user/controller/UserController.java`

```java
package io.github.atengk.user.controller;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.common.result.Result;
import io.github.atengk.user.dto.UserCreateRequest;
import io.github.atengk.user.dto.UserQueryRequest;
import io.github.atengk.user.dto.UserUpdateRequest;
import io.github.atengk.user.service.UserService;
import io.github.atengk.user.vo.UserVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户 HTTP 接口
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    @GetMapping("/{id}")
    public Result<UserVO> getUser(@PathVariable Long id) {
        log.info("查询用户详情，用户ID：{}", id);
        return Result.success(userService.getUser(id));
    }

    @GetMapping
    public Result<List<UserVO>> listUsers(@Valid UserQueryRequest request) {
        log.info("查询用户列表，查询条件：{}", request);
        return Result.success(userService.listUsers(request));
    }

    @PostMapping
    public Result<UserVO> createUser(@Valid @RequestBody UserCreateRequest request,
                                     @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        String traceId = StrUtil.blankToDefault(requestId, "unknown");
        log.info("新增用户，请求ID：{}，请求参数：{}", traceId, request);
        return Result.success(userService.createUser(request));
    }

    @PutMapping("/{id}")
    public Result<UserVO> updateUser(@PathVariable Long id,
                                     @Valid @RequestBody UserUpdateRequest request) {
        log.info("修改用户，用户ID：{}，请求参数：{}", id, request);
        return Result.success(userService.updateUser(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> deleteUser(@PathVariable Long id) {
        log.info("删除用户，用户ID：{}", id);
        userService.deleteUser(id);
        return Result.success(Boolean.TRUE);
    }
}
```

该 Controller 的职责边界比较清晰：路径和请求方法由 Controller 声明，参数校验由 Validation 完成，核心业务由 `UserService` 完成，最终统一返回 `Result<T>`。

### 请求路径设计

请求路径用于标识后端资源。路径设计应稳定、清晰、语义化，避免将具体动作随意写入 URL 中。一般建议使用名词表示资源，使用 HTTP 方法表示动作。

推荐格式如下：

```text
/api/{资源名称}
/api/{资源名称}/{资源ID}
/api/{资源名称}/{资源ID}/{子资源名称}
```

用户接口路径示例：

| 业务动作     | 请求方法 | 请求路径                | 说明             |
| ------------ | -------- | ----------------------- | ---------------- |
| 查询用户列表 | `GET`    | `/api/users`            | 查询用户资源集合 |
| 查询用户详情 | `GET`    | `/api/users/{id}`       | 查询单个用户资源 |
| 新增用户     | `POST`   | `/api/users`            | 创建用户资源     |
| 修改用户     | `PUT`    | `/api/users/{id}`       | 修改指定用户资源 |
| 删除用户     | `DELETE` | `/api/users/{id}`       | 删除指定用户资源 |
| 查询用户角色 | `GET`    | `/api/users/{id}/roles` | 查询用户子资源   |

路径设计建议如下：

| 规则                    | 推荐写法                 | 不推荐写法               |
| ----------------------- | ------------------------ | ------------------------ |
| 使用复数名词            | `/api/users`             | `/api/user`              |
| 使用短横线分隔单词      | `/api/order-items`       | `/api/orderItems`        |
| 使用路径参数表示资源 ID | `/api/users/{id}`        | `/api/users?id=1`        |
| 避免在路径中写动作      | `DELETE /api/users/{id}` | `/api/users/delete/{id}` |
| 接口统一加业务前缀      | `/api/users`             | `/users`                 |

在管理后台项目中，如果需要区分端侧，也可以增加路径前缀：

```text
/api/admin/users
/api/app/users
/api/open/users
```

其中：

| 前缀         | 说明                         |
| ------------ | ---------------------------- |
| `/api/admin` | 管理后台接口                 |
| `/api/app`   | 移动端或用户端接口           |
| `/api/open`  | 对外开放接口或第三方对接接口 |

### 请求方法选择

HTTP 请求方法用于表达接口动作。Spring Boot 中常用 `@GetMapping`、`@PostMapping`、`@PutMapping`、`@DeleteMapping` 对不同方法进行映射。

常用请求方法如下：

| 请求方法 | Spring 注解      | 使用场景             | 是否通常携带请求体 |
| -------- | ---------------- | -------------------- | ------------------ |
| `GET`    | `@GetMapping`    | 查询列表、查询详情   | 否                 |
| `POST`   | `@PostMapping`   | 新增、提交、复杂查询 | 是                 |
| `PUT`    | `@PutMapping`    | 全量修改资源         | 是                 |
| `PATCH`  | `@PatchMapping`  | 局部修改资源         | 是                 |
| `DELETE` | `@DeleteMapping` | 删除资源             | 一般否             |

选择建议如下：

| 场景         | 推荐方法 | 示例                                   |
| ------------ | -------- | -------------------------------------- |
| 查询详情     | `GET`    | `GET /api/users/1`                     |
| 分页查询     | `GET`    | `GET /api/users?pageNum=1&pageSize=10` |
| 复杂条件查询 | `POST`   | `POST /api/users/search`               |
| 新增数据     | `POST`   | `POST /api/users`                      |
| 修改数据     | `PUT`    | `PUT /api/users/1`                     |
| 局部修改状态 | `PATCH`  | `PATCH /api/users/1/status`            |
| 删除数据     | `DELETE` | `DELETE /api/users/1`                  |

对于查询条件较少的接口，优先使用 `GET + RequestParam`。对于查询条件复杂、字段较多、存在数组或嵌套对象的接口，可以使用 `POST + RequestBody`。

### 请求参数接收

Spring Boot Controller 常见参数接收方式包括路径参数、查询参数、请求体参数和请求头参数。不同参数来源应选择不同注解，避免混用导致接口语义不清。

| 参数来源   | 注解             | 常见场景                            |
| ---------- | ---------------- | ----------------------------------- |
| 路径参数   | `@PathVariable`  | 资源 ID、层级资源标识               |
| 查询参数   | `@RequestParam`  | 分页参数、筛选条件、关键字          |
| 请求体参数 | `@RequestBody`   | 新增、修改、复杂查询                |
| 请求头参数 | `@RequestHeader` | Token、请求 ID、客户端版本、租户 ID |

请求参数 DTO 示例：

文件位置：`src/main/java/io/github/atengk/user/dto/UserQueryRequest.java`

```java
package io.github.atengk.user.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 用户查询请求参数
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
public class UserQueryRequest {

    private String keyword;

    private Integer status;

    @Min(value = 1, message = "页码不能小于1")
    private Integer pageNum = 1;

    @Min(value = 1, message = "每页条数不能小于1")
    @Max(value = 100, message = "每页条数不能大于100")
    private Integer pageSize = 10;
}
```

新增用户请求体示例：

文件位置：`src/main/java/io/github/atengk/user/dto/UserCreateRequest.java`

```java
package io.github.atengk.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户新增请求参数
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
public class UserCreateRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(max = 30, message = "用户名长度不能超过30个字符")
    private String username;

    @NotBlank(message = "昵称不能为空")
    @Size(max = 50, message = "昵称长度不能超过50个字符")
    private String nickname;

    @Email(message = "邮箱格式不正确")
    private String email;
}
```

修改用户请求体示例：

文件位置：`src/main/java/io/github/atengk/user/dto/UserUpdateRequest.java`

```java
package io.github.atengk.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户修改请求参数
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
public class UserUpdateRequest {

    @NotBlank(message = "昵称不能为空")
    @Size(max = 50, message = "昵称长度不能超过50个字符")
    private String nickname;

    @Email(message = "邮箱格式不正确")
    private String email;

    private Integer status;
}
```

### 响应结果返回

接口响应建议统一使用固定结构返回，避免不同接口返回格式不一致。常见统一响应字段包括 `code`、`message`、`data` 和 `timestamp`。

推荐响应格式如下：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 1,
    "username": "ateng",
    "nickname": "阿腾"
  },
  "timestamp": 1710000000000
}
```

统一响应类示例：

文件位置：`src/main/java/io/github/atengk/common/result/Result.java`

```java
package io.github.atengk.common.result;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一响应结果
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
public class Result<T> implements Serializable {

    private Integer code;

    private String message;

    private T data;

    private Long timestamp;

    private Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    public static <T> Result<T> success() {
        return new Result<>(200, "操作成功", null);
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(200, "操作成功", data);
    }

    public static <T> Result<T> fail(Integer code, String message) {
        return new Result<>(code, message, null);
    }
}
```

响应码枚举示例：

文件位置：`src/main/java/io/github/atengk/common/result/ResultCode.java`

```java
package io.github.atengk.common.result;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 响应码枚举
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Getter
@RequiredArgsConstructor
public enum ResultCode {

    SUCCESS(200, "操作成功"),

    BAD_REQUEST(400, "请求参数错误"),

    UNAUTHORIZED(401, "未认证或登录已过期"),

    FORBIDDEN(403, "无访问权限"),

    NOT_FOUND(404, "资源不存在"),

    INTERNAL_ERROR(500, "系统异常");

    private final Integer code;

    private final String message;
}
```

用户响应对象示例：

文件位置：`src/main/java/io/github/atengk/user/vo/UserVO.java`

```java
package io.github.atengk.user.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户响应对象
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
public class UserVO {

    private Long id;

    private String username;

    private String nickname;

    private String email;

    private Integer status;

    private LocalDateTime createTime;
}
```

Controller 中返回结果时，应统一使用：

```java
return Result.success(data);
```

不建议不同接口混合返回以下格式：

```java
return data;
return Map.of("data", data);
return "success";
return ResponseEntity.ok(data);
```

除非接口有明确的特殊要求，例如文件下载、图片流、第三方回调响应或需要精确控制 HTTP 状态码。

## 请求参数处理

本章节用于说明 Spring Boot HTTP 接口中常见的四类参数处理方式。参数接收方式应与接口语义保持一致，路径参数用于标识资源，查询参数用于筛选，请求体用于提交复杂对象，请求头用于传递协议级或上下文信息。

### PathVariable 路径参数

`@PathVariable` 用于接收 URL 路径中的变量，常用于资源 ID、编码、类型等直接参与路径定位的参数。

示例接口：

```text
GET /api/users/1001
```

Controller 写法如下：

```java
@GetMapping("/{id}")
public Result<UserVO> getUser(@PathVariable Long id) {
    log.info("查询用户详情，用户ID：{}", id);
    return Result.success(userService.getUser(id));
}
```

如果路径变量名和方法参数名不一致，需要显式指定变量名：

```java
@GetMapping("/{userId}")
public Result<UserVO> getUser(@PathVariable("userId") Long id) {
    log.info("查询用户详情，用户ID：{}", id);
    return Result.success(userService.getUser(id));
}
```

多级路径参数示例：

```text
GET /api/users/1001/orders/9001
@GetMapping("/{userId}/orders/{orderId}")
public Result<String> getUserOrder(@PathVariable Long userId,
                                   @PathVariable Long orderId) {
    log.info("查询用户订单，用户ID：{}，订单ID：{}", userId, orderId);
    return Result.success("userId=" + userId + ", orderId=" + orderId);
}
```

路径参数使用建议：

| 建议                 | 说明                                                  |
| -------------------- | ----------------------------------------------------- |
| 资源 ID 使用路径参数 | `/api/users/{id}` 比 `/api/users?id=1` 更符合资源语义 |
| 参数类型尽量明确     | 常用 `Long`、`String`、`Integer`                      |
| 不要传复杂对象       | 路径参数只适合简单标识                                |
| 不要传敏感信息       | URL 可能被日志、浏览器历史、代理服务器记录            |

### RequestParam 查询参数

`@RequestParam` 用于接收 URL 查询字符串中的参数，常用于分页、关键字搜索、状态筛选等查询场景。

示例接口：

```text
GET /api/users?keyword=ateng&status=1&pageNum=1&pageSize=10
```

简单参数接收方式如下：

```java
@GetMapping("/simple")
public Result<List<UserVO>> listUsersSimple(@RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) Integer status,
                                            @RequestParam(defaultValue = "1") Integer pageNum,
                                            @RequestParam(defaultValue = "10") Integer pageSize) {
    log.info("查询用户列表，关键字：{}，状态：{}，页码：{}，每页条数：{}", keyword, status, pageNum, pageSize);
    return Result.success(userService.listUsersSimple(keyword, status, pageNum, pageSize));
}
```

当查询参数较多时，推荐使用 DTO 接收，不需要给 DTO 参数添加 `@RequestBody`：

```java
@GetMapping
public Result<List<UserVO>> listUsers(@Valid UserQueryRequest request) {
    log.info("查询用户列表，查询条件：{}", request);
    return Result.success(userService.listUsers(request));
}
```

查询参数 DTO 接收方式适合以下场景：

| 场景     | 说明                        |
| -------- | --------------------------- |
| 分页查询 | `pageNum`、`pageSize`       |
| 条件筛选 | `keyword`、`status`、`type` |
| 时间范围 | `startTime`、`endTime`      |
| 排序字段 | `sortField`、`sortOrder`    |

如果需要接收数组参数，可以使用以下方式：

```text
GET /api/users/batch?ids=1&ids=2&ids=3
@GetMapping("/batch")
public Result<List<UserVO>> listUsersByIds(@RequestParam List<Long> ids) {
    log.info("批量查询用户，用户ID列表：{}", ids);
    return Result.success(userService.listUsersByIds(ids));
}
```

也可以使用逗号分隔参数，但需要自行处理：

```text
GET /api/users/batch-string?ids=1,2,3
@GetMapping("/batch-string")
public Result<List<UserVO>> listUsersByIdString(@RequestParam String ids) {
    List<Long> idList = StrUtil.split(ids, ',')
            .stream()
            .map(Long::valueOf)
            .toList();

    log.info("批量查询用户，用户ID列表：{}", idList);
    return Result.success(userService.listUsersByIds(idList));
}
```

这里使用 Hutool 的 `StrUtil.split` 处理逗号分隔字符串，适合兼容前端传递 `ids=1,2,3` 的场景。

### RequestBody 请求体参数

`@RequestBody` 用于接收 HTTP 请求体中的 JSON 数据，常用于新增、修改、复杂查询等场景。Spring Boot 会通过 Jackson 将 JSON 自动反序列化为 Java 对象。

示例接口：

```text
POST /api/users
Content-Type: application/json
```

请求体：

```json
{
  "username": "ateng",
  "nickname": "阿腾",
  "email": "ateng@example.com"
}
```

Controller 写法如下：

```java
@PostMapping
public Result<UserVO> createUser(@Valid @RequestBody UserCreateRequest request) {
    log.info("新增用户，请求参数：{}", request);
    return Result.success(userService.createUser(request));
}
```

修改接口示例：

```text
PUT /api/users/1001
Content-Type: application/json
```

请求体：

```json
{
  "nickname": "阿腾",
  "email": "ateng@example.com",
  "status": 1
}
@PutMapping("/{id}")
public Result<UserVO> updateUser(@PathVariable Long id,
                                 @Valid @RequestBody UserUpdateRequest request) {
    log.info("修改用户，用户ID：{}，请求参数：{}", id, request);
    return Result.success(userService.updateUser(id, request));
}
```

`@RequestBody` 使用建议：

| 建议                        | 说明                                    |
| --------------------------- | --------------------------------------- |
| 新增、修改优先使用请求体    | 字段较多时比查询参数更清晰              |
| 请求体对象添加 `@Valid`     | 配合 Validation 注解完成参数校验        |
| 不建议使用 Map 接收业务参数 | 类型不明确，后期维护困难                |
| 不建议直接使用实体类接收    | 避免暴露数据库字段                      |
| 请求头必须指定 JSON         | 通常为 `Content-Type: application/json` |

不推荐写法：

```java
@PostMapping
public Result<Boolean> createUser(@RequestBody Map<String, Object> body) {
    return Result.success(Boolean.TRUE);
}
```

推荐写法：

```java
@PostMapping
public Result<UserVO> createUser(@Valid @RequestBody UserCreateRequest request) {
    return Result.success(userService.createUser(request));
}
```

复杂查询如果字段较多，也可以使用 `POST + RequestBody`：

```java
@PostMapping("/search")
public Result<List<UserVO>> searchUsers(@Valid @RequestBody UserQueryRequest request) {
    log.info("复杂条件查询用户，请求参数：{}", request);
    return Result.success(userService.listUsers(request));
}
```

### RequestHeader 请求头参数

`@RequestHeader` 用于接收 HTTP 请求头中的参数，常用于认证信息、请求链路 ID、客户端版本、租户标识、语言标识等。

常见请求头如下：

| 请求头             | 用途                       |
| ------------------ | -------------------------- |
| `Authorization`    | 登录 Token 或认证信息      |
| `X-Request-Id`     | 请求唯一标识，便于链路追踪 |
| `X-Tenant-Id`      | 租户 ID                    |
| `X-Client-Version` | 客户端版本                 |
| `Accept-Language`  | 客户端语言                 |

示例接口：

```text
GET /api/users/1001
Authorization: Bearer token-value
X-Request-Id: 202605061000001
X-Client-Version: 1.0.0
```

Controller 写法如下：

```java
@GetMapping("/{id}/header-demo")
public Result<UserVO> getUserWithHeader(@PathVariable Long id,
                                        @RequestHeader(value = "Authorization", required = false) String authorization,
                                        @RequestHeader(value = "X-Request-Id", required = false) String requestId,
                                        @RequestHeader(value = "X-Client-Version", required = false) String clientVersion) {
    String traceId = StrUtil.blankToDefault(requestId, "unknown");
    String version = StrUtil.blankToDefault(clientVersion, "unknown");

    log.info("查询用户详情，请求ID：{}，客户端版本：{}，用户ID：{}", traceId, version, id);
    return Result.success(userService.getUser(id));
}
```

请求头参数使用建议：

| 建议                                | 说明                                 |
| ----------------------------------- | ------------------------------------ |
| 非必传请求头设置 `required = false` | 避免缺失请求头直接报错               |
| 认证信息交给拦截器或安全框架处理    | 不建议每个 Controller 手动解析 Token |
| 请求链路 ID 可以记录日志            | 便于排查线上问题                     |
| 不要在日志中输出完整 Token          | 避免敏感信息泄露                     |
| 多租户参数建议统一拦截处理          | 避免业务接口重复接收租户 ID          |

如果请求头是全局通用参数，例如 Token、租户 ID、请求 ID，建议后续通过过滤器、拦截器或 Spring Security、Sa-Token 等认证框架统一处理，而不是在每个 Controller 方法中重复声明。



## 响应结果封装

本章节用于说明 Spring Boot HTTP 接口的统一响应格式、HTTP 状态码使用原则，以及 DTO、VO 等数据传输对象的设计方式。统一响应结构可以让前端、移动端和第三方调用方更容易处理接口结果，也便于后端统一管理错误码、异常信息和响应数据。

### 统一响应结构

统一响应结构用于规范所有 JSON 接口的返回格式。接口不建议有的返回对象，有的返回字符串，有的返回 `Map`，否则前端需要针对不同接口做额外兼容。

推荐统一响应格式如下：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 1,
    "username": "ateng",
    "nickname": "阿腾"
  },
  "timestamp": 1710000000000
}
```

字段说明如下：

| 字段        | 类型      | 说明                                   |
| ----------- | --------- | -------------------------------------- |
| `code`      | `Integer` | 业务响应码，通常与 HTTP 状态码保持接近 |
| `message`   | `String`  | 响应提示信息                           |
| `data`      | `T`       | 实际响应数据                           |
| `timestamp` | `Long`    | 响应时间戳，便于问题排查               |

文件位置：`src/main/java/io/github/atengk/common/result/Result.java`

以下代码定义接口统一响应对象，所有 Controller 可以统一返回 `Result<T>`。

```java
package io.github.atengk.common.result;

import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;

/**
 * 统一响应结果
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Getter
public class Result<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final Integer code;

    private final String message;

    private final T data;

    private final Long timestamp;

    private Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    public static <T> Result<T> success() {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), null);
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
    }

    public static <T> Result<T> success(String message, T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), message, data);
    }

    public static <T> Result<T> fail(ResultCode resultCode) {
        return new Result<>(resultCode.getCode(), resultCode.getMessage(), null);
    }

    public static <T> Result<T> fail(Integer code, String message) {
        return new Result<>(code, message, null);
    }

    public static <T> Result<T> fail(Integer code, String message, T data) {
        return new Result<>(code, message, data);
    }
}
```

文件位置：`src/main/java/io/github/atengk/common/result/ResultCode.java`

以下代码定义通用响应码，业务模块可以在此基础上扩展更细的错误码。

```java
package io.github.atengk.common.result;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 响应码枚举
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Getter
@RequiredArgsConstructor
public enum ResultCode {

    SUCCESS(200, "操作成功"),

    BAD_REQUEST(400, "请求参数错误"),

    UNAUTHORIZED(401, "未认证或登录已过期"),

    FORBIDDEN(403, "无访问权限"),

    NOT_FOUND(404, "资源不存在"),

    METHOD_NOT_ALLOWED(405, "请求方法不支持"),

    CONFLICT(409, "资源状态冲突"),

    UNSUPPORTED_MEDIA_TYPE(415, "请求媒体类型不支持"),

    INTERNAL_ERROR(500, "系统异常"),

    BIZ_ERROR(10001, "业务处理失败");

    private final Integer code;

    private final String message;
}
```

Controller 中统一返回示例：

```java
@GetMapping("/{id}")
public Result<UserVO> getUser(@PathVariable Long id) {
    log.info("查询用户详情，用户ID：{}", id);
    return Result.success(userService.getUser(id));
}

@PostMapping
public Result<UserVO> createUser(@Valid @RequestBody UserCreateRequest request) {
    log.info("新增用户，请求参数：{}", request);
    return Result.success(userService.createUser(request));
}
```

不推荐以下写法混用：

```java
return userService.getUser(id);
return Map.of("data", data);
return "success";
return ResponseEntity.ok(data);
```

如果是普通 JSON 业务接口，优先统一返回 `Result<T>`。如果是文件下载、图片流、第三方回调、重定向、SSE、WebSocket 等特殊场景，可以按协议要求单独处理。

### HTTP 状态码使用

HTTP 状态码用于描述请求在 HTTP 协议层面的处理结果，`Result.code` 用于描述业务层面的处理结果。两者不要完全混为一谈。

推荐原则如下：

| 场景         | HTTP 状态码    | Result.code | 说明                                   |
| ------------ | -------------- | ----------- | -------------------------------------- |
| 请求成功     | `200`          | `200`       | 查询、修改、删除成功                   |
| 新增成功     | `200` 或 `201` | `200`       | 内部系统常用 `200`，开放接口可用 `201` |
| 参数错误     | `400`          | `400`       | 参数格式错误、校验失败                 |
| 未认证       | `401`          | `401`       | 未登录、Token 无效                     |
| 无权限       | `403`          | `403`       | 已认证但无操作权限                     |
| 资源不存在   | `404`          | `404`       | 数据、路径或资源不存在                 |
| 请求方法错误 | `405`          | `405`       | 例如 POST 接口使用 GET 调用            |
| 资源冲突     | `409`          | `409`       | 重复提交、状态冲突                     |
| 系统异常     | `500`          | `500`       | 未预期异常                             |

在实际项目中有两种常见策略：

第一种是所有业务响应 HTTP 状态码都返回 `200`，通过 `Result.code` 区分成功或失败。这种方式对前端统一拦截较简单，但不够符合 HTTP 语义。

第二种是 HTTP 状态码和业务错误码保持一致，例如参数错误返回 HTTP `400`，系统异常返回 HTTP `500`。这种方式更符合 REST 接口规范，也更利于网关、监控和调用方识别错误类型。

推荐在企业内部系统中使用折中方式：

| 类型           | 建议                                                         |
| -------------- | ------------------------------------------------------------ |
| 正常业务成功   | HTTP `200`                                                   |
| 参数错误       | HTTP `400`                                                   |
| 未认证、无权限 | HTTP `401`、`403`                                            |
| 资源不存在     | HTTP `404`                                                   |
| 系统异常       | HTTP `500`                                                   |
| 可预期业务失败 | 可返回 HTTP `200` + 业务错误码，也可返回 HTTP `400`，团队内保持一致即可 |

异常处理中可以使用 `ResponseEntity<Result<?>>` 明确控制 HTTP 状态码：

```java
@ExceptionHandler(BizException.class)
public ResponseEntity<Result<Void>> handleBizException(BizException exception) {
    log.warn("业务处理失败：{}", exception.getMessage());
    Result<Void> result = Result.fail(exception.getCode(), exception.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
}
```

如果项目已经约定所有业务异常 HTTP 状态码统一返回 `200`，也可以直接返回：

```java
@ExceptionHandler(BizException.class)
public Result<Void> handleBizException(BizException exception) {
    log.warn("业务处理失败：{}", exception.getMessage());
    return Result.fail(exception.getCode(), exception.getMessage());
}
```

两种方式不要在同一个项目中随意混用。

### 数据传输对象设计

数据传输对象用于隔离接口层和数据库实体层。常见对象包括 DTO、Request、Response、VO、Entity 等，它们的职责不同。

| 类型      | 用途                 | 示例                |
| --------- | -------------------- | ------------------- |
| `Request` | 接收前端请求参数     | `UserCreateRequest` |
| `DTO`     | 层间传输数据         | `UserDTO`           |
| `VO`      | 返回给前端的展示对象 | `UserVO`            |
| `Entity`  | 数据库表映射对象     | `UserEntity`        |
| `BO`      | 复杂业务处理对象     | `UserRegisterBO`    |

接口开发中建议遵循以下原则：

| 原则               | 说明                                                 |
| ------------------ | ---------------------------------------------------- |
| 入参不用 Entity    | 避免前端传入数据库字段，例如 `deleted`、`createTime` |
| 出参不用 Entity    | 避免暴露内部字段，降低字段变更影响                   |
| 新增和修改对象分离 | 新增、修改字段通常不完全一致                         |
| 查询对象单独设计   | 查询条件通常包含分页、关键字、状态、时间范围         |
| VO 面向前端展示    | 可组合多个实体字段或脱敏字段                         |

文件位置：`src/main/java/io/github/atengk/user/dto/UserCreateRequest.java`

以下代码定义新增用户请求对象，用于接收 `POST /api/users` 的请求体。

```java
package io.github.atengk.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户新增请求参数
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
public class UserCreateRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(max = 30, message = "用户名长度不能超过30个字符")
    private String username;

    @NotBlank(message = "昵称不能为空")
    @Size(max = 50, message = "昵称长度不能超过50个字符")
    private String nickname;

    @Email(message = "邮箱格式不正确")
    private String email;
}
```

文件位置：`src/main/java/io/github/atengk/user/dto/UserUpdateRequest.java`

以下代码定义修改用户请求对象，修改接口通常不允许修改用户名等唯一标识字段。

```java
package io.github.atengk.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户修改请求参数
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
public class UserUpdateRequest {

    @NotBlank(message = "昵称不能为空")
    @Size(max = 50, message = "昵称长度不能超过50个字符")
    private String nickname;

    @Email(message = "邮箱格式不正确")
    private String email;

    private Integer status;
}
```

文件位置：`src/main/java/io/github/atengk/user/vo/UserVO.java`

以下代码定义用户响应对象，只返回前端需要展示的字段。

```java
package io.github.atengk.user.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户响应对象
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@Builder
public class UserVO {

    private Long id;

    private String username;

    private String nickname;

    private String email;

    private Integer status;

    private LocalDateTime createTime;
}
```

对象转换可以放在 Service 层、转换器类或 MapStruct 中。简单项目可以直接在 Service 中转换，复杂项目建议抽取 `Converter`。

文件位置：`src/main/java/io/github/atengk/user/converter/UserConverter.java`

以下代码定义用户对象转换器，使用 Hutool 判断对象是否为空。

```java
package io.github.atengk.user.converter;

import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.user.entity.UserEntity;
import io.github.atengk.user.vo.UserVO;

/**
 * 用户对象转换器
 *
 * @author Ateng
 * @since 2026-05-06
 */
public class UserConverter {

    private UserConverter() {
    }

    public static UserVO toVO(UserEntity entity) {
        if (ObjectUtil.isNull(entity)) {
            return null;
        }

        return UserVO.builder()
                .id(entity.getId())
                .username(entity.getUsername())
                .nickname(entity.getNickname())
                .email(entity.getEmail())
                .status(entity.getStatus())
                .createTime(entity.getCreateTime())
                .build();
    }
}
```

## 参数校验

本章节用于说明 Spring Boot 3 中接口参数校验的使用方式。参数校验应尽量在 Controller 入参阶段完成，避免无效数据进入业务层。Spring Boot 3 使用 `jakarta.validation` 相关注解，不再使用旧版 `javax.validation` 包。

### Validation 依赖配置

参数校验需要引入 `spring-boot-starter-validation` 依赖。该依赖会引入 Hibernate Validator，并支持 `@Valid`、`@Validated`、`@NotBlank`、`@NotNull` 等常用校验能力。

文件位置：`pom.xml`

```xml
<dependencies>
    <!-- 参数校验：支持 @Valid、@Validated 和 Jakarta Validation 校验注解 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
</dependencies>
```

Controller 中常见启用方式如下：

```java
@PostMapping
public Result<UserVO> createUser(@Valid @RequestBody UserCreateRequest request) {
    log.info("新增用户，请求参数：{}", request);
    return Result.success(userService.createUser(request));
}
```

如果需要对 `@RequestParam`、`@PathVariable` 这类简单参数进行校验，需要在 Controller 类上添加 `@Validated`。

```java
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    @GetMapping("/{id}")
    public Result<UserVO> getUser(@PathVariable @Min(value = 1, message = "用户ID必须大于0") Long id) {
        log.info("查询用户详情，用户ID：{}", id);
        return Result.success(userService.getUser(id));
    }
}
```

### 常用校验注解

常用校验注解可以覆盖非空、长度、数值范围、邮箱格式、正则表达式、时间范围等场景。

| 注解              | 适用类型           | 说明                                  |
| ----------------- | ------------------ | ------------------------------------- |
| `@NotNull`        | 任意类型           | 不能为 `null`                         |
| `@NotBlank`       | `String`           | 不能为 `null`，且去除空白后长度大于 0 |
| `@NotEmpty`       | 字符串、集合、数组 | 不能为 `null`，且长度或数量大于 0     |
| `@Size`           | 字符串、集合、数组 | 限制长度或数量                        |
| `@Min`            | 数值               | 最小值                                |
| `@Max`            | 数值               | 最大值                                |
| `@DecimalMin`     | 数值               | 小数最小值                            |
| `@DecimalMax`     | 数值               | 小数最大值                            |
| `@Email`          | 字符串             | 邮箱格式                              |
| `@Pattern`        | 字符串             | 正则表达式                            |
| `@Past`           | 日期时间           | 必须是过去时间                        |
| `@Future`         | 日期时间           | 必须是未来时间                        |
| `@Positive`       | 数值               | 必须为正数                            |
| `@PositiveOrZero` | 数值               | 必须为正数或 0                        |

文件位置：`src/main/java/io/github/atengk/user/dto/UserRegisterRequest.java`

以下代码定义用户注册请求参数，覆盖非空、长度、邮箱、手机号正则和年龄范围校验。

```java
package io.github.atengk.user.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

/**
 * 用户注册请求参数
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
public class UserRegisterRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 4, max = 30, message = "用户名长度必须在4到30个字符之间")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 64, message = "密码长度必须在8到64个字符之间")
    private String password;

    @NotBlank(message = "昵称不能为空")
    @Size(max = 50, message = "昵称长度不能超过50个字符")
    private String nickname;

    @Email(message = "邮箱格式不正确")
    private String email;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String mobile;

    @Min(value = 1, message = "年龄不能小于1")
    @Max(value = 120, message = "年龄不能大于120")
    private Integer age;

    @Past(message = "生日必须是过去日期")
    private LocalDate birthday;
}
```

查询参数校验示例：

```java
@GetMapping("/page")
public Result<List<UserVO>> pageUsers(@RequestParam(defaultValue = "1")
                                      @Min(value = 1, message = "页码不能小于1") Integer pageNum,
                                      @RequestParam(defaultValue = "10")
                                      @Min(value = 1, message = "每页条数不能小于1")
                                      @Max(value = 100, message = "每页条数不能大于100") Integer pageSize) {
    log.info("分页查询用户，页码：{}，每页条数：{}", pageNum, pageSize);
    return Result.success(userService.pageUsers(pageNum, pageSize));
}
```

### 嵌套对象校验

当请求对象内部包含另一个对象或集合对象时，需要在嵌套字段上添加 `@Valid`，否则内部对象的校验注解不会生效。

例如新增订单时，请求体中包含收货地址和商品明细列表：

```json
{
  "userId": 1,
  "address": {
    "receiverName": "阿腾",
    "receiverMobile": "13800138000",
    "detailAddress": "北京市朝阳区xxx街道"
  },
  "items": [
    {
      "skuId": 1001,
      "quantity": 2
    }
  ]
}
```

文件位置：`src/main/java/io/github/atengk/order/dto/OrderCreateRequest.java`

以下代码定义订单新增请求参数，其中 `address` 和 `items` 都是嵌套校验对象。

```java
package io.github.atengk.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 订单新增请求参数
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
public class OrderCreateRequest {

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @Valid
    @NotNull(message = "收货地址不能为空")
    private OrderAddressRequest address;

    @Valid
    @NotEmpty(message = "订单商品不能为空")
    private List<OrderItemRequest> items;
}
```

文件位置：`src/main/java/io/github/atengk/order/dto/OrderAddressRequest.java`

以下代码定义订单收货地址请求参数。

```java
package io.github.atengk.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 订单收货地址请求参数
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
public class OrderAddressRequest {

    @NotBlank(message = "收货人不能为空")
    @Size(max = 30, message = "收货人长度不能超过30个字符")
    private String receiverName;

    @NotBlank(message = "收货人手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "收货人手机号格式不正确")
    private String receiverMobile;

    @NotBlank(message = "详细地址不能为空")
    @Size(max = 200, message = "详细地址长度不能超过200个字符")
    private String detailAddress;
}
```

文件位置：`src/main/java/io/github/atengk/order/dto/OrderItemRequest.java`

以下代码定义订单商品明细请求参数。

```java
package io.github.atengk.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 订单商品明细请求参数
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
public class OrderItemRequest {

    @NotNull(message = "商品SKU不能为空")
    private Long skuId;

    @NotNull(message = "购买数量不能为空")
    @Min(value = 1, message = "购买数量不能小于1")
    private Integer quantity;
}
```

Controller 使用方式如下：

```java
@PostMapping("/orders")
public Result<Long> createOrder(@Valid @RequestBody OrderCreateRequest request) {
    log.info("创建订单，请求参数：{}", request);
    return Result.success(orderService.createOrder(request));
}
```

如果缺少 `@Valid`，例如：

```java
@NotNull(message = "收货地址不能为空")
private OrderAddressRequest address;
```

此时只能校验 `address` 是否为空，无法继续校验 `receiverName`、`receiverMobile` 和 `detailAddress`。

### 分组校验

分组校验用于同一个请求对象在不同接口场景下使用不同校验规则。例如新增用户时 `username` 必填，修改用户时 `id` 必填，但两者可以复用同一个请求对象。

文件位置：`src/main/java/io/github/atengk/common/validation/ValidationGroup.java`

以下代码定义通用校验分组，按新增、修改、删除、查询场景区分。

```java
package io.github.atengk.common.validation;

/**
 * 参数校验分组
 *
 * @author Ateng
 * @since 2026-05-06
 */
public interface ValidationGroup {

    interface Create {
    }

    interface Update {
    }

    interface Delete {
    }

    interface Query {
    }
}
```

文件位置：`src/main/java/io/github/atengk/user/dto/UserSaveRequest.java`

以下代码定义用户保存请求对象，通过 `groups` 指定不同接口场景下的校验规则。

```java
package io.github.atengk.user.dto;

import io.github.atengk.common.validation.ValidationGroup;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户保存请求参数
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
public class UserSaveRequest {

    @NotNull(message = "用户ID不能为空", groups = ValidationGroup.Update.class)
    private Long id;

    @NotBlank(message = "用户名不能为空", groups = ValidationGroup.Create.class)
    @Size(max = 30, message = "用户名长度不能超过30个字符", groups = {
            ValidationGroup.Create.class,
            ValidationGroup.Update.class
    })
    private String username;

    @NotBlank(message = "昵称不能为空", groups = {
            ValidationGroup.Create.class,
            ValidationGroup.Update.class
    })
    @Size(max = 50, message = "昵称长度不能超过50个字符", groups = {
            ValidationGroup.Create.class,
            ValidationGroup.Update.class
    })
    private String nickname;

    @Email(message = "邮箱格式不正确", groups = {
            ValidationGroup.Create.class,
            ValidationGroup.Update.class
    })
    private String email;
}
```

Controller 中使用 `@Validated` 指定校验分组：

```java
@PostMapping
public Result<UserVO> createUser(@Validated(ValidationGroup.Create.class)
                                 @RequestBody UserSaveRequest request) {
    log.info("新增用户，请求参数：{}", request);
    return Result.success(userService.createUser(request));
}

@PutMapping("/{id}")
public Result<UserVO> updateUser(@PathVariable Long id,
                                 @Validated(ValidationGroup.Update.class)
                                 @RequestBody UserSaveRequest request) {
    request.setId(id);
    log.info("修改用户，用户ID：{}，请求参数：{}", id, request);
    return Result.success(userService.updateUser(request));
}
```

分组校验适合字段大部分相同的场景。如果新增和修改字段差异较大，建议直接拆分为 `UserCreateRequest` 和 `UserUpdateRequest`，可读性通常更好。

## 异常处理

本章节用于说明接口异常的统一处理方式。接口开发中不建议在每个 Controller 方法中重复 `try-catch`，而应通过 `@RestControllerAdvice` 统一拦截异常，并返回统一响应结构。

### 全局异常处理

全局异常处理类用于统一处理系统异常、业务异常、参数校验异常、请求方法异常、请求体格式错误等问题。

文件位置：`src/main/java/io/github/atengk/common/exception/GlobalExceptionHandler.java`

以下代码定义全局异常处理器，统一返回 `Result` 结构，并使用日志记录异常信息。

```java
package io.github.atengk.common.exception;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.common.result.Result;
import io.github.atengk.common.result.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public ResponseEntity<Result<Void>> handleBizException(BizException exception, HttpServletRequest request) {
        log.warn("业务异常，请求路径：{}，错误信息：{}", request.getRequestURI(), exception.getMessage());
        Result<Void> result = Result.fail(exception.getCode(), exception.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<List<String>>> handleMethodArgumentNotValidException(MethodArgumentNotValidException exception,
                                                                                      HttpServletRequest request) {
        List<String> messages = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::buildFieldErrorMessage)
                .toList();

        String message = CollUtil.isEmpty(messages) ? ResultCode.BAD_REQUEST.getMessage() : messages.getFirst();
        log.warn("请求体参数校验失败，请求路径：{}，错误信息：{}", request.getRequestURI(), messages);

        Result<List<String>> result = Result.fail(ResultCode.BAD_REQUEST.getCode(), message, messages);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<Result<List<String>>> handleBindException(BindException exception, HttpServletRequest request) {
        List<String> messages = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::buildFieldErrorMessage)
                .toList();

        String message = CollUtil.isEmpty(messages) ? ResultCode.BAD_REQUEST.getMessage() : messages.getFirst();
        log.warn("查询参数绑定失败，请求路径：{}，错误信息：{}", request.getRequestURI(), messages);

        Result<List<String>> result = Result.fail(ResultCode.BAD_REQUEST.getCode(), message, messages);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Result<List<String>>> handleConstraintViolationException(ConstraintViolationException exception,
                                                                                   HttpServletRequest request) {
        Set<ConstraintViolation<?>> violations = exception.getConstraintViolations();
        List<String> messages = violations.stream()
                .map(ConstraintViolation::getMessage)
                .filter(StrUtil::isNotBlank)
                .toList();

        String message = CollUtil.isEmpty(messages) ? ResultCode.BAD_REQUEST.getMessage() : messages.getFirst();
        log.warn("路径参数或查询参数校验失败，请求路径：{}，错误信息：{}", request.getRequestURI(), messages);

        Result<List<String>> result = Result.fail(ResultCode.BAD_REQUEST.getCode(), message, messages);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Result<Void>> handleHttpMessageNotReadableException(HttpMessageNotReadableException exception,
                                                                              HttpServletRequest request) {
        log.warn("请求体解析失败，请求路径：{}，错误信息：{}", request.getRequestURI(), exception.getMessage());
        Result<Void> result = Result.fail(ResultCode.BAD_REQUEST.getCode(), "请求体格式错误，请检查 JSON 格式");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Result<Void>> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException exception,
                                                                                     HttpServletRequest request) {
        log.warn("请求方法不支持，请求路径：{}，请求方法：{}", request.getRequestURI(), exception.getMethod());
        Result<Void> result = Result.fail(ResultCode.METHOD_NOT_ALLOWED);
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(result);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Result<Void>> handleNoResourceFoundException(NoResourceFoundException exception,
                                                                       HttpServletRequest request) {
        log.warn("资源不存在，请求路径：{}", request.getRequestURI());
        Result<Void> result = Result.fail(ResultCode.NOT_FOUND);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleException(Exception exception, HttpServletRequest request) {
        log.error("系统异常，请求路径：{}", request.getRequestURI(), exception);
        Result<Void> result = Result.fail(ResultCode.INTERNAL_ERROR);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
    }

    private String buildFieldErrorMessage(FieldError fieldError) {
        String field = fieldError.getField();
        String defaultMessage = fieldError.getDefaultMessage();
        if (StrUtil.isBlank(defaultMessage)) {
            return field + "参数不合法";
        }
        return field + "：" + defaultMessage;
    }
}
```

说明：

| 异常类型                                 | 常见触发场景                                      |
| ---------------------------------------- | ------------------------------------------------- |
| `BizException`                           | 主动抛出的业务异常                                |
| `MethodArgumentNotValidException`        | `@RequestBody` 对象参数校验失败                   |
| `BindException`                          | 普通对象绑定查询参数失败                          |
| `ConstraintViolationException`           | `@PathVariable`、`@RequestParam` 简单参数校验失败 |
| `HttpMessageNotReadableException`        | JSON 格式错误、类型不匹配                         |
| `HttpRequestMethodNotSupportedException` | 请求方法不支持                                    |
| `NoResourceFoundException`               | 静态资源或接口路径不存在                          |
| `Exception`                              | 未预期系统异常兜底                                |

### 参数校验异常处理

参数校验异常需要区分请求体对象校验、查询参数对象校验、路径参数校验和 JSON 解析失败。

常见错误示例：

请求体参数缺失：

```json
{
  "username": "",
  "email": "abc"
}
```

返回示例：

```json
{
  "code": 400,
  "message": "username：用户名不能为空",
  "data": [
    "username：用户名不能为空",
    "email：邮箱格式不正确"
  ],
  "timestamp": 1710000000000
}
```

路径参数错误：

```text
GET /api/users/0
```

返回示例：

```json
{
  "code": 400,
  "message": "用户ID必须大于0",
  "data": [
    "用户ID必须大于0"
  ],
  "timestamp": 1710000000000
}
```

JSON 格式错误：

```json
{
  "username": "ateng",
  "age": "abc"
}
```

返回示例：

```json
{
  "code": 400,
  "message": "请求体格式错误，请检查 JSON 格式",
  "data": null,
  "timestamp": 1710000000000
}
```

参数校验异常处理建议如下：

| 建议                         | 说明                                       |
| ---------------------------- | ------------------------------------------ |
| 不在 Controller 手动判断空值 | 使用 Validation 注解统一处理               |
| 错误信息直接写清楚           | 例如“用户名不能为空”，不要只返回“参数错误” |
| 可返回错误列表               | 前端表单可以一次性展示多个字段错误         |
| 日志使用 `warn`              | 参数错误属于客户端问题，不应记为系统错误   |
| 不返回 Java 异常类名         | 避免暴露后端实现细节                       |

如果希望返回更结构化的字段错误信息，可以定义专门的错误对象。

文件位置：`src/main/java/io/github/atengk/common/result/FieldErrorVO.java`

以下代码定义字段错误响应对象，适合前端表单按字段展示错误信息。

```java
package io.github.atengk.common.result;

import lombok.Builder;
import lombok.Data;

/**
 * 字段错误响应对象
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@Builder
public class FieldErrorVO {

    private String field;

    private String message;
}
```

异常处理器中可以改为返回 `List<FieldErrorVO>`：

```java
@ExceptionHandler(MethodArgumentNotValidException.class)
public ResponseEntity<Result<List<FieldErrorVO>>> handleMethodArgumentNotValidException(MethodArgumentNotValidException exception,
                                                                                        HttpServletRequest request) {
    List<FieldErrorVO> errors = exception.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(fieldError -> FieldErrorVO.builder()
                    .field(fieldError.getField())
                    .message(fieldError.getDefaultMessage())
                    .build())
            .toList();

    String message = CollUtil.isEmpty(errors) ? ResultCode.BAD_REQUEST.getMessage() : errors.getFirst().getMessage();
    log.warn("请求体参数校验失败，请求路径：{}，错误信息：{}", request.getRequestURI(), errors);

    Result<List<FieldErrorVO>> result = Result.fail(ResultCode.BAD_REQUEST.getCode(), message, errors);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
}
```

### 业务异常处理

业务异常用于处理可预期的业务失败，例如用户不存在、库存不足、订单状态不允许修改、用户名重复等。这类异常不是系统故障，应使用自定义异常主动抛出，并由全局异常处理器统一转换为接口响应。

文件位置：`src/main/java/io/github/atengk/common/exception/BizException.java`

以下代码定义业务异常类，支持自定义错误码和错误信息。

```java
package io.github.atengk.common.exception;

import io.github.atengk.common.result.ResultCode;
import lombok.Getter;

import java.io.Serial;

/**
 * 业务异常
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Getter
public class BizException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final Integer code;

    public BizException(String message) {
        super(message);
        this.code = ResultCode.BIZ_ERROR.getCode();
    }

    public BizException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public BizException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }
}
```

文件位置：`src/main/java/io/github/atengk/user/service/impl/UserServiceImpl.java`

以下代码演示在业务层抛出业务异常，Controller 不需要手动捕获。

```java
package io.github.atengk.user.service.impl;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.common.exception.BizException;
import io.github.atengk.common.result.ResultCode;
import io.github.atengk.user.converter.UserConverter;
import io.github.atengk.user.dto.UserCreateRequest;
import io.github.atengk.user.entity.UserEntity;
import io.github.atengk.user.service.UserService;
import io.github.atengk.user.vo.UserVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 用户业务实现
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Service
public class UserServiceImpl implements UserService {

    @Override
    public UserVO getUser(Long id) {
        UserEntity user = mockGetUser(id);
        if (ObjectUtil.isNull(user)) {
            log.warn("查询用户失败，用户不存在，用户ID：{}", id);
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "用户不存在");
        }
        return UserConverter.toVO(user);
    }

    @Override
    public UserVO createUser(UserCreateRequest request) {
        if (StrUtil.equalsIgnoreCase("admin", request.getUsername())) {
            log.warn("新增用户失败，用户名不允许使用，用户名：{}", request.getUsername());
            throw new BizException(10002, "用户名不允许使用");
        }

        UserEntity user = new UserEntity();
        user.setId(1001L);
        user.setUsername(request.getUsername());
        user.setNickname(request.getNickname());
        user.setEmail(request.getEmail());
        user.setStatus(1);
        user.setCreateTime(LocalDateTime.now());

        log.info("新增用户成功，用户ID：{}，用户名：{}", user.getId(), user.getUsername());
        return UserConverter.toVO(user);
    }

    private UserEntity mockGetUser(Long id) {
        if (id == null || id <= 0 || id == 9999L) {
            return null;
        }

        UserEntity user = new UserEntity();
        user.setId(id);
        user.setUsername("ateng");
        user.setNickname("阿腾");
        user.setEmail("ateng@example.com");
        user.setStatus(1);
        user.setCreateTime(LocalDateTime.now());
        return user;
    }
}
```

文件位置：`src/main/java/io/github/atengk/user/service/UserService.java`

以下代码定义用户业务接口。

```java
package io.github.atengk.user.service;

import io.github.atengk.user.dto.UserCreateRequest;
import io.github.atengk.user.vo.UserVO;

/**
 * 用户业务接口
 *
 * @author Ateng
 * @since 2026-05-06
 */
public interface UserService {

    UserVO getUser(Long id);

    UserVO createUser(UserCreateRequest request);
}
```

文件位置：`src/main/java/io/github/atengk/user/entity/UserEntity.java`

以下代码定义示例实体类，用于演示业务层和响应对象之间的转换。

```java
package io.github.atengk.user.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
public class UserEntity {

    private Long id;

    private String username;

    private String nickname;

    private String email;

    private Integer status;

    private LocalDateTime createTime;
}
```

业务异常返回示例：

```json
{
  "code": 10002,
  "message": "用户名不允许使用",
  "data": null,
  "timestamp": 1710000000000
}
```

业务异常使用建议如下：

| 建议                       | 说明                                   |
| -------------------------- | -------------------------------------- |
| 可预期失败使用业务异常     | 用户不存在、余额不足、状态不允许修改   |
| 不要用业务异常包裹所有异常 | 数据库连接失败、空指针等应视为系统异常 |
| 业务异常日志使用 `warn`    | 表示业务规则不满足，不是系统故障       |
| 系统异常日志使用 `error`   | 需要输出异常堆栈，便于排查             |
| 错误信息面向调用方         | 不返回 SQL、堆栈、服务器路径等内部信息 |

Controller 中保持简洁即可：

```java
@GetMapping("/{id}")
public Result<UserVO> getUser(@PathVariable @Min(value = 1, message = "用户ID必须大于0") Long id) {
    log.info("查询用户详情，用户ID：{}", id);
    return Result.success(userService.getUser(id));
}
```

当 `userService.getUser(id)` 抛出 `BizException` 时，全局异常处理器会自动捕获并返回统一 JSON 结果。



## 接口文档

本章节用于说明 Spring Boot 3 项目中接口文档的生成方式。接口文档建议使用 OpenAPI 规范统一维护，避免手写接口文档与实际代码不一致。Spring Boot 3 推荐使用 `springdoc-openapi`，它可以根据 Controller、DTO、VO 和注解自动生成 OpenAPI JSON 文档和 Swagger UI 页面。

### OpenAPI 依赖配置

OpenAPI 依赖用于生成接口文档页面和接口元数据。Spring Boot 3 需要使用 `springdoc-openapi` 2.x 版本，旧版 1.x 主要适配 Spring Boot 2，不建议在 Spring Boot 3 项目中使用。

文件位置：`pom.xml`

```xml
<properties>
    <!-- Spring Boot 3 推荐使用 springdoc-openapi 2.x -->
    <springdoc.version>2.6.0</springdoc.version>
</properties>

<dependencies>
    <!-- OpenAPI 文档：生成 Swagger UI 页面和 OpenAPI JSON 文档 -->
    <dependency>
        <groupId>org.springdoc</groupId>
        <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
        <version>${springdoc.version}</version>
    </dependency>

    <!-- 参数校验：OpenAPI 会读取部分 Validation 注解用于展示字段约束 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
</dependencies>
```

配置文件中可以指定文档访问路径、接口排序方式和分组展示方式。

文件位置：`src/main/resources/application.yml`

```yaml
springdoc:
  api-docs:
    # OpenAPI JSON 文档地址
    path: /v3/api-docs
  swagger-ui:
    # Swagger UI 页面地址
    path: /swagger-ui.html
    # 按 HTTP 方法排序接口
    operations-sorter: method
    # 按接口分组名称排序
    tags-sorter: alpha
    # 默认展开层级，none 表示不自动展开
    doc-expansion: none
  default-flat-param-object: true
```

如果需要统一设置接口文档标题、版本、描述和服务器地址，可以增加 OpenAPI 配置类。

文件位置：`src/main/java/io/github/atengk/config/OpenApiConfig.java`

以下代码用于配置 OpenAPI 基础信息，启动项目后会展示在 Swagger UI 页面顶部。

```java
package io.github.atengk.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI 文档配置
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Configuration
public class OpenApiConfig {

    /**
     * 配置 OpenAPI 文档基础信息
     *
     * @return OpenAPI 配置对象
     */
    @Bean
    public OpenAPI openApi() {
        Contact contact = new Contact()
                .name("Ateng")
                .email("ateng@example.com");

        Info info = new Info()
                .title("Spring Boot HTTP 接口文档")
                .version("1.0.0")
                .description("用于说明 Spring Boot 3 HTTP 接口的请求、响应、参数校验和异常处理规范。")
                .contact(contact);

        Server localServer = new Server()
                .url("http://localhost:8080")
                .description("本地开发环境");

        return new OpenAPI()
                .info(info)
                .servers(List.of(localServer));
    }
}
```

常用配置项说明如下：

| 配置项                                   | 说明                     |
| ---------------------------------------- | ------------------------ |
| `springdoc.api-docs.path`                | OpenAPI JSON 文档路径    |
| `springdoc.swagger-ui.path`              | Swagger UI 页面路径      |
| `springdoc.swagger-ui.operations-sorter` | 接口排序方式             |
| `springdoc.swagger-ui.tags-sorter`       | 标签排序方式             |
| `springdoc.default-flat-param-object`    | 查询参数对象是否平铺展示 |

### 接口注解说明

OpenAPI 注解用于补充接口说明、参数说明、响应说明和对象字段说明。虽然 springdoc 可以自动扫描 Controller，但只依赖自动扫描通常不够清晰，建议在核心接口、请求对象和响应对象上补充必要注解。

常用注解如下：

| 注解           | 使用位置        | 说明                                   |
| -------------- | --------------- | -------------------------------------- |
| `@Tag`         | Controller 类   | 定义接口分组                           |
| `@Operation`   | Controller 方法 | 定义接口名称和描述                     |
| `@Parameter`   | 方法参数        | 定义路径参数、查询参数、请求头参数说明 |
| `@Schema`      | DTO、VO、字段   | 定义对象和字段说明                     |
| `@ApiResponse` | Controller 方法 | 定义响应状态和响应说明                 |
| `@Hidden`      | 类、方法、字段  | 在接口文档中隐藏指定内容               |

文件位置：`src/main/java/io/github/atengk/user/dto/UserCreateRequest.java`

以下代码为新增用户请求对象增加 OpenAPI 字段说明，接口文档会展示字段名称、类型、示例值和校验规则。

```java
package io.github.atengk.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户新增请求参数
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@Schema(description = "用户新增请求参数")
public class UserCreateRequest {

    @Schema(description = "用户名", example = "ateng", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "用户名不能为空")
    @Size(max = 30, message = "用户名长度不能超过30个字符")
    private String username;

    @Schema(description = "昵称", example = "阿腾", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "昵称不能为空")
    @Size(max = 50, message = "昵称长度不能超过50个字符")
    private String nickname;

    @Schema(description = "邮箱", example = "ateng@example.com")
    @Email(message = "邮箱格式不正确")
    private String email;
}
```

文件位置：`src/main/java/io/github/atengk/user/dto/UserUpdateRequest.java`

以下代码为修改用户请求对象增加 OpenAPI 字段说明。

```java
package io.github.atengk.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户修改请求参数
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@Schema(description = "用户修改请求参数")
public class UserUpdateRequest {

    @Schema(description = "昵称", example = "阿腾", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "昵称不能为空")
    @Size(max = 50, message = "昵称长度不能超过50个字符")
    private String nickname;

    @Schema(description = "邮箱", example = "ateng@example.com")
    @Email(message = "邮箱格式不正确")
    private String email;

    @Schema(description = "状态：0-禁用，1-启用", example = "1")
    private Integer status;
}
```

文件位置：`src/main/java/io/github/atengk/user/vo/UserVO.java`

以下代码为用户响应对象增加字段说明，便于前端理解接口返回内容。

```java
package io.github.atengk.user.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户响应对象
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@Builder
@Schema(description = "用户响应对象")
public class UserVO {

    @Schema(description = "用户ID", example = "1001")
    private Long id;

    @Schema(description = "用户名", example = "ateng")
    private String username;

    @Schema(description = "昵称", example = "阿腾")
    private String nickname;

    @Schema(description = "邮箱", example = "ateng@example.com")
    private String email;

    @Schema(description = "状态：0-禁用，1-启用", example = "1")
    private Integer status;

    @Schema(description = "创建时间", example = "2026-05-06 10:00:00")
    private LocalDateTime createTime;
}
```

文件位置：`src/main/java/io/github/atengk/user/controller/UserController.java`

以下代码演示在 Controller 上增加 OpenAPI 注解，描述接口分组、接口用途、参数含义和响应结果。

```java
package io.github.atengk.user.controller;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.common.result.Result;
import io.github.atengk.user.dto.UserCreateRequest;
import io.github.atengk.user.dto.UserQueryRequest;
import io.github.atengk.user.dto.UserUpdateRequest;
import io.github.atengk.user.service.UserService;
import io.github.atengk.user.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户 HTTP 接口
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
@Tag(name = "用户接口", description = "用户查询、新增、修改、删除相关接口")
public class UserController {

    private final UserService userService;

    /**
     * 查询用户详情
     *
     * @param id 用户ID
     * @return 用户详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询用户详情", description = "根据用户ID查询单个用户的详细信息")
    public Result<UserVO> getUser(
            @Parameter(description = "用户ID", example = "1001", required = true)
            @PathVariable
            @Min(value = 1, message = "用户ID必须大于0") Long id) {
        log.info("查询用户详情，用户ID：{}", id);
        return Result.success(userService.getUser(id));
    }

    /**
     * 查询用户列表
     *
     * @param request 查询参数
     * @return 用户列表
     */
    @GetMapping
    @Operation(summary = "查询用户列表", description = "根据关键字、状态和分页参数查询用户列表")
    public Result<List<UserVO>> listUsers(@Valid UserQueryRequest request) {
        log.info("查询用户列表，查询条件：{}", request);
        return Result.success(userService.listUsers(request));
    }

    /**
     * 新增用户
     *
     * @param request 请求参数
     * @param requestId 请求ID
     * @return 新增后的用户信息
     */
    @PostMapping
    @Operation(summary = "新增用户", description = "创建一个新的用户")
    public Result<UserVO> createUser(
            @Valid @RequestBody UserCreateRequest request,
            @Parameter(description = "请求ID，用于链路追踪", example = "202605061000001")
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        String traceId = StrUtil.blankToDefault(requestId, "unknown");
        log.info("新增用户，请求ID：{}，请求参数：{}", traceId, request);
        return Result.success(userService.createUser(request));
    }

    /**
     * 修改用户
     *
     * @param id 用户ID
     * @param request 请求参数
     * @return 修改后的用户信息
     */
    @PutMapping("/{id}")
    @Operation(summary = "修改用户", description = "根据用户ID修改用户基础信息")
    public Result<UserVO> updateUser(
            @Parameter(description = "用户ID", example = "1001", required = true)
            @PathVariable
            @Min(value = 1, message = "用户ID必须大于0") Long id,
            @Valid @RequestBody UserUpdateRequest request) {
        log.info("修改用户，用户ID：{}，请求参数：{}", id, request);
        return Result.success(userService.updateUser(id, request));
    }

    /**
     * 删除用户
     *
     * @param id 用户ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除用户", description = "根据用户ID删除用户")
    public Result<Boolean> deleteUser(
            @Parameter(description = "用户ID", example = "1001", required = true)
            @PathVariable
            @Min(value = 1, message = "用户ID必须大于0") Long id) {
        log.info("删除用户，用户ID：{}", id);
        userService.deleteUser(id);
        return Result.success(Boolean.TRUE);
    }
}
```

注解使用建议如下：

| 建议                             | 说明                               |
| -------------------------------- | ---------------------------------- |
| Controller 类添加 `@Tag`         | 按业务模块聚合接口                 |
| Controller 方法添加 `@Operation` | 清楚说明接口用途                   |
| DTO、VO 添加 `@Schema`           | 明确字段含义和示例值               |
| 核心参数添加 `@Parameter`        | 说明路径参数、请求头、重要查询参数 |
| 不要过度注解                     | 字段含义清楚时，不必写冗长描述     |
| 隐藏内部接口                     | 内部调试接口可使用 `@Hidden`       |

### 文档访问方式

项目启动后，默认可以通过浏览器访问 Swagger UI 和 OpenAPI JSON 文档。

常用访问地址如下：

```text
Swagger UI 页面：
http://localhost:8080/swagger-ui.html

OpenAPI JSON 文档：
http://localhost:8080/v3/api-docs
```

如果配置了应用上下文路径，例如：

```yaml
server:
  servlet:
    # 应用上下文路径
    context-path: /http-api
```

则访问地址变为：

```text
Swagger UI 页面：
http://localhost:8080/http-api/swagger-ui.html

OpenAPI JSON 文档：
http://localhost:8080/http-api/v3/api-docs
```

本地验证命令如下：

```bash
# 启动 Spring Boot 应用
mvn spring-boot:run

# 查看 OpenAPI JSON 文档是否生成成功
curl http://localhost:8080/v3/api-docs
```

如果 Swagger UI 无法访问，可以按以下方向排查：

| 问题                 | 排查方向                                                |
| -------------------- | ------------------------------------------------------- |
| 页面 404             | 检查 `springdoc-openapi-starter-webmvc-ui` 依赖是否引入 |
| JSON 文档为空        | 检查 Controller 是否被 Spring Boot 扫描                 |
| 接口缺少描述         | 检查是否添加 `@Operation`、`@Schema`                    |
| 请求体字段不展示     | 检查 DTO 是否作为 `@RequestBody` 参数使用               |
| 参数校验不展示       | 检查是否引入 `spring-boot-starter-validation`           |
| 生产环境不想暴露文档 | 通过环境配置或安全框架限制访问                          |

生产环境如果不希望暴露接口文档，可以在 `application-prod.yml` 中关闭：

```yaml
springdoc:
  api-docs:
    # 生产环境关闭 OpenAPI JSON 文档
    enabled: false
  swagger-ui:
    # 生产环境关闭 Swagger UI 页面
    enabled: false
```

也可以只允许内网、测试账号或管理员角色访问文档页面，避免接口结构暴露给非授权用户。

## 接口调用示例

本章节用于给出常见 HTTP 接口的调用方式，包括 GET、POST、PUT、DELETE 四类请求。示例默认服务地址为 `http://localhost:8080`，接口统一前缀为 `/api`，响应结构统一使用 `Result<T>`。

### GET 接口示例

GET 接口通常用于查询数据，不建议携带请求体。常见形式包括查询详情、分页查询、条件查询和批量查询。

查询用户详情：

```http
GET /api/users/1001 HTTP/1.1
Host: localhost:8080
Accept: application/json
X-Request-Id: 202605061000001
```

curl 调用方式如下：

```bash
curl -X GET "http://localhost:8080/api/users/1001" \
  -H "Accept: application/json" \
  -H "X-Request-Id: 202605061000001"
```

响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 1001,
    "username": "ateng",
    "nickname": "阿腾",
    "email": "ateng@example.com",
    "status": 1,
    "createTime": "2026-05-06 10:00:00"
  },
  "timestamp": 1778023200000
}
```

查询用户列表：

```http
GET /api/users?keyword=ateng&status=1&pageNum=1&pageSize=10 HTTP/1.1
Host: localhost:8080
Accept: application/json
```

curl 调用方式如下：

```bash
curl -X GET "http://localhost:8080/api/users?keyword=ateng&status=1&pageNum=1&pageSize=10" \
  -H "Accept: application/json"
```

响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": 1001,
      "username": "ateng",
      "nickname": "阿腾",
      "email": "ateng@example.com",
      "status": 1,
      "createTime": "2026-05-06 10:00:00"
    }
  ],
  "timestamp": 1778023200000
}
```

GET 接口调用注意事项：

| 注意项                      | 说明                             |
| --------------------------- | -------------------------------- |
| 参数放在 URL 查询字符串中   | 例如 `?pageNum=1&pageSize=10`    |
| 不传复杂请求体              | GET 请求体兼容性较差             |
| 敏感信息不要放 URL          | URL 可能被浏览器、网关和日志记录 |
| 查询条件较复杂时可改用 POST | 例如高级搜索、多条件组合查询     |

### POST 接口示例

POST 接口通常用于新增数据、提交表单、复杂查询或触发业务动作。新增和复杂参数建议使用 JSON 请求体，并指定 `Content-Type: application/json`。

新增用户请求：

```http
POST /api/users HTTP/1.1
Host: localhost:8080
Content-Type: application/json
Accept: application/json
X-Request-Id: 202605061000002

{
  "username": "ateng",
  "nickname": "阿腾",
  "email": "ateng@example.com"
}
```

curl 调用方式如下：

```bash
curl -X POST "http://localhost:8080/api/users" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -H "X-Request-Id: 202605061000002" \
  -d '{
    "username": "ateng",
    "nickname": "阿腾",
    "email": "ateng@example.com"
  }'
```

响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 1001,
    "username": "ateng",
    "nickname": "阿腾",
    "email": "ateng@example.com",
    "status": 1,
    "createTime": "2026-05-06 10:00:00"
  },
  "timestamp": 1778023200000
}
```

参数校验失败响应示例：

```json
{
  "code": 400,
  "message": "username：用户名不能为空",
  "data": [
    "username：用户名不能为空",
    "email：邮箱格式不正确"
  ],
  "timestamp": 1778023200000
}
```

POST 接口调用注意事项：

| 注意项                     | 说明                                  |
| -------------------------- | ------------------------------------- |
| 必须声明请求体类型         | 使用 `Content-Type: application/json` |
| 请求体字段与 DTO 字段对应  | 字段名应保持一致                      |
| 使用 `@Valid` 校验请求体   | 避免非法数据进入业务层                |
| 新增接口不要由客户端传主键 | 主键通常由数据库或后端生成            |
| 不要在日志中打印敏感字段   | 例如密码、Token、身份证号             |

如果是复杂查询，也可以使用 POST 请求体：

```bash
curl -X POST "http://localhost:8080/api/users/search" \
  -H "Content-Type: application/json" \
  -d '{
    "keyword": "ateng",
    "status": 1,
    "pageNum": 1,
    "pageSize": 10
  }'
```

### PUT 接口示例

PUT 接口通常用于修改指定资源。请求路径中包含资源 ID，请求体中包含需要修改的数据。对于全量修改建议使用 PUT，对于只修改个别字段的场景可以使用 PATCH。

修改用户请求：

```http
PUT /api/users/1001 HTTP/1.1
Host: localhost:8080
Content-Type: application/json
Accept: application/json

{
  "nickname": "阿腾-修改",
  "email": "ateng.new@example.com",
  "status": 1
}
```

curl 调用方式如下：

```bash
curl -X PUT "http://localhost:8080/api/users/1001" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d '{
    "nickname": "阿腾-修改",
    "email": "ateng.new@example.com",
    "status": 1
  }'
```

响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 1001,
    "username": "ateng",
    "nickname": "阿腾-修改",
    "email": "ateng.new@example.com",
    "status": 1,
    "createTime": "2026-05-06 10:00:00"
  },
  "timestamp": 1778023200000
}
```

资源不存在响应示例：

```json
{
  "code": 404,
  "message": "用户不存在",
  "data": null,
  "timestamp": 1778023200000
}
```

PUT 接口调用注意事项：

| 注意项                  | 说明                           |
| ----------------------- | ------------------------------ |
| 资源 ID 放在路径中      | 例如 `/api/users/{id}`         |
| 修改内容放在请求体中    | 使用 `@RequestBody` 接收       |
| 路径 ID 优先于请求体 ID | 避免前端传入两个不一致的 ID    |
| 修改前检查资源是否存在  | 不存在时返回明确错误           |
| 需要校验状态流转        | 例如禁用、启用、删除等业务状态 |

推荐 Controller 写法：

```java
@PutMapping("/{id}")
public Result<UserVO> updateUser(@PathVariable @Min(value = 1, message = "用户ID必须大于0") Long id,
                                 @Valid @RequestBody UserUpdateRequest request) {
    log.info("修改用户，用户ID：{}，请求参数：{}", id, request);
    return Result.success(userService.updateUser(id, request));
}
```

### DELETE 接口示例

DELETE 接口通常用于删除指定资源。资源 ID 建议放在路径中，普通删除接口一般不需要请求体。

删除用户请求：

```http
DELETE /api/users/1001 HTTP/1.1
Host: localhost:8080
Accept: application/json
```

curl 调用方式如下：

```bash
curl -X DELETE "http://localhost:8080/api/users/1001" \
  -H "Accept: application/json"
```

响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": true,
  "timestamp": 1778023200000
}
```

如果删除资源不存在，可以返回：

```json
{
  "code": 404,
  "message": "用户不存在",
  "data": null,
  "timestamp": 1778023200000
}
```

如果业务上不允许删除，例如用户仍有关联订单，可以返回：

```json
{
  "code": 10001,
  "message": "当前用户存在关联订单，不允许删除",
  "data": null,
  "timestamp": 1778023200000
}
```

DELETE 接口调用注意事项：

| 注意项                   | 说明                         |
| ------------------------ | ---------------------------- |
| 删除 ID 放在路径中       | 例如 `/api/users/{id}`       |
| 普通删除不建议携带请求体 | 兼容性更好                   |
| 删除前检查业务约束       | 例如是否存在关联数据         |
| 谨慎执行物理删除         | 后台系统通常优先使用逻辑删除 |
| 重要删除操作记录日志     | 便于审计和问题追踪           |

推荐 Controller 写法：

```java
@DeleteMapping("/{id}")
public Result<Boolean> deleteUser(@PathVariable @Min(value = 1, message = "用户ID必须大于0") Long id) {
    log.info("删除用户，用户ID：{}", id);
    userService.deleteUser(id);
    return Result.success(Boolean.TRUE);
}
```

对于批量删除，可以使用 `DELETE + RequestBody` 或 `POST /batch-delete`，团队内保持统一即可。若考虑兼容性，推荐使用 POST：

```bash
curl -X POST "http://localhost:8080/api/users/batch-delete" \
  -H "Content-Type: application/json" \
  -d '{
    "ids": [1001, 1002, 1003]
  }'
```

批量删除请求对象示例：

文件位置：`src/main/java/io/github/atengk/user/dto/UserBatchDeleteRequest.java`

以下代码定义批量删除请求参数，适合接收多个用户 ID。

```java
package io.github.atengk.user.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 用户批量删除请求参数
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
public class UserBatchDeleteRequest {

    @NotEmpty(message = "用户ID列表不能为空")
    private List<@NotNull(message = "用户ID不能为空") Long> ids;
}
```

对应 Controller 示例：

```java
@PostMapping("/batch-delete")
public Result<Boolean> batchDeleteUsers(@Valid @RequestBody UserBatchDeleteRequest request) {
    log.info("批量删除用户，用户ID列表：{}", request.getIds());
    userService.batchDeleteUsers(request.getIds());
    return Result.success(Boolean.TRUE);
}
```

接口调用整体建议如下：

| 请求方法 | 主要用途                       | 参数位置           |
| -------- | ------------------------------ | ------------------ |
| `GET`    | 查询详情、查询列表             | 路径参数、查询参数 |
| `POST`   | 新增、提交、复杂查询、批量操作 | 请求体             |
| `PUT`    | 修改指定资源                   | 路径参数 + 请求体  |
| `DELETE` | 删除指定资源                   | 路径参数           |
| `PATCH`  | 局部修改字段或状态             | 路径参数 + 请求体  |



## 接口测试

本章节用于说明 Spring Boot HTTP 接口的常见测试方式。接口测试不应只依赖人工联调，建议同时覆盖 Service 单元测试、Controller 层 MockMvc 测试和 Postman 手工调试。单元测试用于验证业务逻辑，MockMvc 用于验证 HTTP 请求、参数校验和响应结构，Postman 用于前后端联调和接口交付验证。

### 单元测试

单元测试主要用于验证业务层逻辑，通常不启动完整 Spring 容器，执行速度快，适合覆盖正常流程、异常流程、边界条件和业务规则。对于 Service 层，如果依赖 Mapper、Repository 或外部服务，可以使用 Mockito 模拟依赖对象。

单元测试建议覆盖以下内容：

| 测试对象     | 测试重点                               |
| ------------ | -------------------------------------- |
| Service      | 业务规则、异常分支、数据转换           |
| Converter    | Entity、DTO、VO 转换是否正确           |
| 工具类       | 字符串、日期、金额、状态判断等工具逻辑 |
| 参数构造逻辑 | 默认值、边界值、空值处理               |
| 业务异常     | 是否在指定条件下抛出 `BizException`    |

文件位置：`src/test/java/io/github/atengk/user/service/UserServiceImplTest.java`

以下代码用于测试用户业务层的新增用户逻辑，覆盖成功场景和业务异常场景。

```java
package io.github.atengk.user.service;

import io.github.atengk.common.exception.BizException;
import io.github.atengk.user.dto.UserCreateRequest;
import io.github.atengk.user.service.impl.UserServiceImpl;
import io.github.atengk.user.vo.UserVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 用户业务单元测试
 *
 * @author Ateng
 * @since 2026-05-06
 */
class UserServiceImplTest {

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl();
    }

    /**
     * 测试新增用户成功
     */
    @Test
    void createUserSuccess() {
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("ateng");
        request.setNickname("阿腾");
        request.setEmail("ateng@example.com");

        UserVO userVO = userService.createUser(request);

        assertThat(userVO).isNotNull();
        assertThat(userVO.getId()).isEqualTo(1001L);
        assertThat(userVO.getUsername()).isEqualTo("ateng");
        assertThat(userVO.getNickname()).isEqualTo("阿腾");
        assertThat(userVO.getEmail()).isEqualTo("ateng@example.com");
    }

    /**
     * 测试用户名不允许使用时抛出业务异常
     */
    @Test
    void createUserWhenUsernameAdminThenThrowBizException() {
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("admin");
        request.setNickname("管理员");
        request.setEmail("admin@example.com");

        BizException exception = assertThrows(BizException.class, () -> userService.createUser(request));

        assertThat(exception.getCode()).isEqualTo(10002);
        assertThat(exception.getMessage()).isEqualTo("用户名不允许使用");
    }
}
```

运行测试命令如下：

```bash
# 运行全部测试
mvn test

# 只运行指定测试类
mvn -Dtest=UserServiceImplTest test
```

单元测试编写建议如下：

| 建议                         | 说明                                                    |
| ---------------------------- | ------------------------------------------------------- |
| 测试方法命名表达业务场景     | 例如 `createUserWhenUsernameAdminThenThrowBizException` |
| 正常和异常流程都要覆盖       | 不只测试成功路径                                        |
| 不依赖真实数据库             | 单元测试优先使用 Mock 或内存数据                        |
| 断言结果字段                 | 不只判断对象不为空                                      |
| 异常场景断言错误码和错误信息 | 保证接口返回可预期                                      |

如果 Service 依赖 Mapper，可以使用 Mockito 模拟数据库访问结果。

文件位置：`src/test/java/io/github/atengk/user/service/UserServiceWithMockTest.java`

以下代码用于演示 Service 依赖数据访问对象时的 Mock 测试方式。

```java
package io.github.atengk.user.service;

import io.github.atengk.common.exception.BizException;
import io.github.atengk.user.entity.UserEntity;
import io.github.atengk.user.mapper.UserMapper;
import io.github.atengk.user.service.impl.UserDbServiceImpl;
import io.github.atengk.user.vo.UserVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/**
 * 用户业务 Mock 单元测试
 *
 * @author Ateng
 * @since 2026-05-06
 */
@ExtendWith(MockitoExtension.class)
class UserServiceWithMockTest {

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserDbServiceImpl userService;

    /**
     * 测试根据用户ID查询成功
     */
    @Test
    void getUserSuccess() {
        UserEntity entity = new UserEntity();
        entity.setId(1001L);
        entity.setUsername("ateng");
        entity.setNickname("阿腾");
        entity.setEmail("ateng@example.com");
        entity.setStatus(1);
        entity.setCreateTime(LocalDateTime.now());

        when(userMapper.selectById(1001L)).thenReturn(entity);

        UserVO userVO = userService.getUser(1001L);

        assertThat(userVO).isNotNull();
        assertThat(userVO.getId()).isEqualTo(1001L);
        assertThat(userVO.getUsername()).isEqualTo("ateng");
    }

    /**
     * 测试用户不存在时抛出业务异常
     */
    @Test
    void getUserWhenNotFoundThenThrowBizException() {
        when(userMapper.selectById(9999L)).thenReturn(null);

        BizException exception = assertThrows(BizException.class, () -> userService.getUser(9999L));

        assertThat(exception.getMessage()).isEqualTo("用户不存在");
    }
}
```

### MockMvc 测试

MockMvc 用于测试 Controller 层接口行为，不需要真正启动 HTTP 端口。它可以验证请求路径、请求方法、请求参数、请求头、请求体、参数校验、响应状态码和响应 JSON 结构。

MockMvc 适合覆盖以下内容：

| 测试内容                        | 示例                           |
| ------------------------------- | ------------------------------ |
| 请求路径是否正确                | `/api/users/{id}`              |
| 请求方法是否正确                | `GET`、`POST`、`PUT`、`DELETE` |
| 参数校验是否生效                | 用户名为空时返回 `400`         |
| 响应结构是否正确                | `code`、`message`、`data`      |
| Controller 是否正确调用 Service | 查询、新增、删除等接口         |

文件位置：`src/test/java/io/github/atengk/user/controller/UserControllerTest.java`

以下代码用于测试用户 Controller 的查询、新增和参数校验行为。

```java
package io.github.atengk.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.atengk.common.exception.GlobalExceptionHandler;
import io.github.atengk.user.dto.UserCreateRequest;
import io.github.atengk.user.service.UserService;
import io.github.atengk.user.vo.UserVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 用户接口 MockMvc 测试
 *
 * @author Ateng
 * @since 2026-05-06
 */
@WebMvcTest(UserController.class)
@Import(GlobalExceptionHandler.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    /**
     * 测试查询用户详情成功
     *
     * @throws Exception 测试异常
     */
    @Test
    void getUserSuccess() throws Exception {
        UserVO userVO = UserVO.builder()
                .id(1001L)
                .username("ateng")
                .nickname("阿腾")
                .email("ateng@example.com")
                .status(1)
                .createTime(LocalDateTime.of(2026, 5, 6, 10, 0, 0))
                .build();

        when(userService.getUser(1001L)).thenReturn(userVO);

        mockMvc.perform(get("/api/users/{id}", 1001L)
                        .header("X-Request-Id", "202605061000001")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("操作成功"))
                .andExpect(jsonPath("$.data.id").value(1001))
                .andExpect(jsonPath("$.data.username").value("ateng"))
                .andExpect(jsonPath("$.data.nickname").value("阿腾"));
    }

    /**
     * 测试新增用户成功
     *
     * @throws Exception 测试异常
     */
    @Test
    void createUserSuccess() throws Exception {
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("ateng");
        request.setNickname("阿腾");
        request.setEmail("ateng@example.com");

        UserVO userVO = UserVO.builder()
                .id(1001L)
                .username("ateng")
                .nickname("阿腾")
                .email("ateng@example.com")
                .status(1)
                .createTime(LocalDateTime.of(2026, 5, 6, 10, 0, 0))
                .build();

        when(userService.createUser(any(UserCreateRequest.class))).thenReturn(userVO);

        mockMvc.perform(post("/api/users")
                        .header("X-Request-Id", "202605061000002")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1001))
                .andExpect(jsonPath("$.data.username").value("ateng"));
    }

    /**
     * 测试新增用户参数校验失败
     *
     * @throws Exception 测试异常
     */
    @Test
    void createUserWhenUsernameBlankThenReturnBadRequest() throws Exception {
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("");
        request.setNickname("阿腾");
        request.setEmail("error-email");

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message", containsString("用户名不能为空")));
    }

    /**
     * 测试删除用户成功
     *
     * @throws Exception 测试异常
     */
    @Test
    void deleteUserSuccess() throws Exception {
        mockMvc.perform(delete("/api/users/{id}", 1001L)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(true));
    }
}
```

如果使用 Spring Boot 3.4 及以上版本，`@MockBean` 可能会被标记为过时，可以改用 `@MockitoBean`。如果项目仍使用 Spring Boot 3.3.x，继续使用 `@MockBean` 即可。

MockMvc 常用断言如下：

| 断言                                | 说明                   |
| ----------------------------------- | ---------------------- |
| `status().isOk()`                   | 判断 HTTP 状态码为 200 |
| `status().isBadRequest()`           | 判断 HTTP 状态码为 400 |
| `jsonPath("$.code").value(200)`     | 判断响应业务码         |
| `jsonPath("$.data.id").value(1001)` | 判断响应数据字段       |
| `jsonPath("$.message").exists()`    | 判断字段存在           |
| `jsonPath("$.data").isArray()`      | 判断字段为数组         |

MockMvc 测试建议如下：

| 建议                        | 说明                           |
| --------------------------- | ------------------------------ |
| Controller 测试只关注接口层 | Service 使用 Mock 替代         |
| 覆盖参数校验失败场景        | 防止无效参数进入业务层         |
| 断言响应结构                | 确保统一响应格式没有被破坏     |
| 断言 HTTP 状态码            | 确保异常处理符合约定           |
| 不依赖真实端口              | MockMvc 不需要启动 `8080` 端口 |

### Postman 调试

Postman 适合接口联调、手工验证、环境切换和接口示例保存。它不能替代自动化测试，但适合作为开发、测试和前端联调阶段的辅助工具。

建议在 Postman 中创建以下环境变量：

| 变量名      | 示例值                  | 说明                          |
| ----------- | ----------------------- | ----------------------------- |
| `baseUrl`   | `http://localhost:8080` | 服务基础地址                  |
| `token`     | `Bearer xxx`            | 登录认证信息                  |
| `requestId` | `202605061000001`       | 请求链路 ID                   |
| `tenantId`  | `10001`                 | 租户 ID，非多租户系统可不配置 |

常用请求头建议统一配置在 Collection 级别：

```text
Accept: application/json
Content-Type: application/json
Authorization: {{token}}
X-Request-Id: {{requestId}}
```

GET 查询用户详情：

```text
GET {{baseUrl}}/api/users/1001
```

POST 新增用户：

```json
{
  "username": "ateng",
  "nickname": "阿腾",
  "email": "ateng@example.com"
}
```

PUT 修改用户：

```json
{
  "nickname": "阿腾-修改",
  "email": "ateng.new@example.com",
  "status": 1
}
```

DELETE 删除用户：

```text
DELETE {{baseUrl}}/api/users/1001
```

Postman 调试建议如下：

| 建议                  | 说明                                |
| --------------------- | ----------------------------------- |
| 使用环境变量管理地址  | 避免手动切换开发、测试、生产地址    |
| Collection 按模块分组 | 例如用户接口、订单接口、系统接口    |
| 保存成功和失败示例    | 便于前后端对齐接口行为              |
| 使用统一请求头        | Token、请求 ID、租户 ID 统一配置    |
| 不提交真实敏感信息    | Token、密码、密钥不要提交到公共仓库 |
| 导出前清理环境变量    | 避免泄露内部地址和认证信息          |

Postman 的测试脚本可以用于基础断言，例如判断响应码和统一响应结构：

```javascript
pm.test("HTTP 状态码为 200", function () {
    pm.response.to.have.status(200);
});

pm.test("响应业务码为 200", function () {
    const jsonData = pm.response.json();
    pm.expect(jsonData.code).to.eql(200);
});

pm.test("响应包含 data 字段", function () {
    const jsonData = pm.response.json();
    pm.expect(jsonData).to.have.property("data");
});
```

对于参数校验失败接口，可以增加以下断言：

```javascript
pm.test("HTTP 状态码为 400", function () {
    pm.response.to.have.status(400);
});

pm.test("响应包含错误信息", function () {
    const jsonData = pm.response.json();
    pm.expect(jsonData.code).to.eql(400);
    pm.expect(jsonData.message).to.not.be.empty;
});
```

## 开发规范

本章节用于统一 Spring Boot HTTP 接口开发中的命名、路径、请求响应对象和日志打印方式。开发规范的目标不是增加约束，而是减少协作成本，让接口在多人开发、长期维护和跨端联调时保持一致。

### Controller 命名规范

Controller 类负责 HTTP 接口入口，命名应清晰表达业务模块，不建议使用含糊或动作化命名。

推荐命名方式如下：

| 类型           | 推荐命名              | 说明             |
| -------------- | --------------------- | ---------------- |
| 用户接口       | `UserController`      | 用户资源接口     |
| 订单接口       | `OrderController`     | 订单资源接口     |
| 管理端用户接口 | `AdminUserController` | 管理后台用户接口 |
| 移动端用户接口 | `AppUserController`   | App 端用户接口   |
| 开放平台接口   | `OpenUserController`  | 第三方开放接口   |

不推荐命名如下：

| 不推荐命名             | 原因                                        |
| ---------------------- | ------------------------------------------- |
| `UserApi`              | 与 Controller 约定不统一                    |
| `UserAction`           | 偏旧式 MVC 风格                             |
| `UserRest`             | 表达不完整                                  |
| `UserManageController` | 如果只是用户资源接口，`Manage` 通常没有必要 |
| `CommonController`     | 职责过宽，容易堆积接口                      |

Controller 注解顺序建议保持一致：

```java
/**
 * 用户 HTTP 接口
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
@Tag(name = "用户接口", description = "用户查询、新增、修改、删除相关接口")
public class UserController {
}
```

Controller 方法命名建议使用业务动词加资源名：

| 方法名             | 场景     |
| ------------------ | -------- |
| `getUser`          | 查询详情 |
| `listUsers`        | 查询列表 |
| `pageUsers`        | 分页查询 |
| `createUser`       | 新增     |
| `updateUser`       | 修改     |
| `deleteUser`       | 删除     |
| `batchDeleteUsers` | 批量删除 |
| `updateUserStatus` | 修改状态 |

不建议使用以下方法名：

```java
public Result<?> query() {}
public Result<?> add() {}
public Result<?> edit() {}
public Result<?> remove() {}
public Result<?> doUser() {}
```

这些命名在类内部或接口文档中可读性较差，后期定位代码也不方便。

### URL 命名规范

URL 应以资源为中心设计，使用名词表达资源，使用 HTTP 方法表达动作。不要把新增、修改、删除等动作直接写进 URL，除非该接口确实是一个业务动作而不是资源 CRUD。

推荐规范如下：

| 规则                    | 推荐                     | 不推荐                        |
| ----------------------- | ------------------------ | ----------------------------- |
| 使用复数资源名          | `/api/users`             | `/api/user`                   |
| 使用短横线分隔单词      | `/api/order-items`       | `/api/orderItems`             |
| 使用路径参数表示资源 ID | `/api/users/{id}`        | `/api/users?id=1`             |
| 避免 URL 中出现动词     | `DELETE /api/users/{id}` | `POST /api/users/delete/{id}` |
| 统一接口前缀            | `/api/users`             | `/users`                      |

常见接口 URL 示例：

| 动作         | 请求方法 | URL                      |
| ------------ | -------- | ------------------------ |
| 查询用户列表 | `GET`    | `/api/users`             |
| 查询用户详情 | `GET`    | `/api/users/{id}`        |
| 新增用户     | `POST`   | `/api/users`             |
| 修改用户     | `PUT`    | `/api/users/{id}`        |
| 删除用户     | `DELETE` | `/api/users/{id}`        |
| 修改用户状态 | `PATCH`  | `/api/users/{id}/status` |
| 查询用户角色 | `GET`    | `/api/users/{id}/roles`  |

对于不能自然映射为 CRUD 的业务动作，可以使用动作型子路径，但要保持语义清晰：

| 业务动作 | 推荐 URL                              |
| -------- | ------------------------------------- |
| 用户登录 | `POST /api/auth/login`                |
| 用户退出 | `POST /api/auth/logout`               |
| 重置密码 | `POST /api/users/{id}/reset-password` |
| 导出用户 | `POST /api/users/export`              |
| 导入用户 | `POST /api/users/import`              |
| 批量删除 | `POST /api/users/batch-delete`        |

URL 命名注意事项：

| 注意项           | 说明                               |
| ---------------- | ---------------------------------- |
| URL 使用小写字母 | 避免大小写不一致                   |
| 多单词使用短横线 | 例如 `reset-password`              |
| 不使用下划线     | URL 中优先使用 `-`                 |
| 不暴露技术实现   | 不使用 `/api/userTable/list`       |
| 不包含文件后缀   | 不使用 `/api/users.json`           |
| 版本号按需使用   | 对外开放接口可使用 `/api/v1/users` |

### 请求响应对象规范

请求响应对象用于隔离接口层和数据库实体层。接口入参不建议直接使用 Entity，接口出参也不建议直接返回 Entity。这样可以避免数据库字段变化直接影响前端，也可以防止内部字段泄露。

推荐对象命名如下：

| 对象类型     | 命名示例                 | 用途             |
| ------------ | ------------------------ | ---------------- |
| 新增请求     | `UserCreateRequest`      | 接收新增参数     |
| 修改请求     | `UserUpdateRequest`      | 接收修改参数     |
| 查询请求     | `UserQueryRequest`       | 接收查询条件     |
| 批量删除请求 | `UserBatchDeleteRequest` | 接收批量操作参数 |
| 响应对象     | `UserVO`                 | 返回前端展示数据 |
| 分页响应     | `PageVO<T>`              | 返回分页结构     |
| 统一响应     | `Result<T>`              | 包装统一返回格式 |

请求对象设计建议：

| 建议                     | 说明                                       |
| ------------------------ | ------------------------------------------ |
| 按场景拆分对象           | 新增、修改、查询分开定义                   |
| 字段添加校验注解         | 使用 `@NotBlank`、`@NotNull`、`@Size`      |
| 字段名称与 JSON 保持一致 | 使用小驼峰命名                             |
| 不包含后端控制字段       | 例如 `deleted`、`createTime`、`updateTime` |
| 不直接使用 Entity        | 避免暴露数据库结构                         |

响应对象设计建议：

| 建议                         | 说明                           |
| ---------------------------- | ------------------------------ |
| 只返回前端需要的字段         | 不返回密码、删除标识、内部备注 |
| 敏感字段必须脱敏             | 手机号、身份证、邮箱等按需脱敏 |
| 时间格式统一                 | 例如 `yyyy-MM-dd HH:mm:ss`     |
| 枚举字段可同时返回编码和名称 | 例如 `status`、`statusName`    |
| 列表和详情可拆分 VO          | 字段差异较大时不要强行复用     |

示例请求对象：

文件位置：`src/main/java/io/github/atengk/user/dto/UserQueryRequest.java`

以下代码定义用户查询请求对象，适合 GET 查询参数自动绑定。

```java
package io.github.atengk.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 用户查询请求参数
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@Schema(description = "用户查询请求参数")
public class UserQueryRequest {

    @Schema(description = "关键字", example = "ateng")
    private String keyword;

    @Schema(description = "状态：0-禁用，1-启用", example = "1")
    private Integer status;

    @Schema(description = "页码", example = "1")
    @Min(value = 1, message = "页码不能小于1")
    private Integer pageNum = 1;

    @Schema(description = "每页条数", example = "10")
    @Min(value = 1, message = "每页条数不能小于1")
    @Max(value = 100, message = "每页条数不能大于100")
    private Integer pageSize = 10;
}
```

示例分页响应对象：

文件位置：`src/main/java/io/github/atengk/common/result/PageVO.java`

以下代码定义统一分页响应对象，适合列表接口返回分页数据。

```java
package io.github.atengk.common.result;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 分页响应对象
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@Builder
public class PageVO<T> {

    private Long total;

    private Integer pageNum;

    private Integer pageSize;

    private List<T> records;
}
```

分页接口响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "total": 1,
    "pageNum": 1,
    "pageSize": 10,
    "records": [
      {
        "id": 1001,
        "username": "ateng",
        "nickname": "阿腾",
        "email": "ateng@example.com",
        "status": 1,
        "statusName": "启用",
        "createTime": "2026-05-06 10:00:00"
      }
    ]
  },
  "timestamp": 1778023200000
}
```

敏感字段脱敏示例：

文件位置：`src/main/java/io/github/atengk/user/converter/UserConverter.java`

以下代码演示响应对象转换时使用 Hutool 对邮箱进行脱敏处理。

```java
package io.github.atengk.user.converter;

import cn.hutool.core.util.DesensitizedUtil;
import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.user.entity.UserEntity;
import io.github.atengk.user.vo.UserVO;

/**
 * 用户对象转换器
 *
 * @author Ateng
 * @since 2026-05-06
 */
public class UserConverter {

    private UserConverter() {
    }

    /**
     * 转换为用户响应对象
     *
     * @param entity 用户实体
     * @return 用户响应对象
     */
    public static UserVO toVO(UserEntity entity) {
        if (ObjectUtil.isNull(entity)) {
            return null;
        }

        return UserVO.builder()
                .id(entity.getId())
                .username(entity.getUsername())
                .nickname(entity.getNickname())
                .email(DesensitizedUtil.email(entity.getEmail()))
                .status(entity.getStatus())
                .createTime(entity.getCreateTime())
                .build();
    }
}
```

### 日志打印规范

日志用于记录系统运行状态、关键业务行为和异常信息。接口开发中应打印必要日志，但不能过度打印，也不能输出敏感信息。日志应能帮助定位问题，包括请求路径、业务主键、请求 ID、关键参数和异常堆栈。

日志级别建议如下：

| 级别    | 使用场景                             |
| ------- | ------------------------------------ |
| `debug` | 开发调试、详细变量、中间状态         |
| `info`  | 正常业务操作、接口调用、关键流程完成 |
| `warn`  | 可预期异常、参数错误、业务规则不满足 |
| `error` | 系统异常、外部服务异常、数据库异常   |

Controller 日志建议：

| 场景         | 建议                             |
| ------------ | -------------------------------- |
| 查询详情     | 记录资源 ID                      |
| 新增数据     | 记录业务关键字段，不记录敏感字段 |
| 修改数据     | 记录资源 ID 和关键变更字段       |
| 删除数据     | 记录资源 ID、操作人、请求 ID     |
| 参数校验失败 | 由全局异常处理器统一记录         |
| 系统异常     | 由全局异常处理器打印堆栈         |

推荐写法：

```java
log.info("查询用户详情，用户ID：{}", id);
log.info("新增用户，请求ID：{}，用户名：{}", requestId, request.getUsername());
log.warn("删除用户失败，用户不存在，用户ID：{}", id);
log.error("调用第三方接口失败，请求ID：{}，接口地址：{}", requestId, url, exception);
```

不推荐写法：

```java
log.info("开始");
log.info("请求参数：" + request);
log.error("异常：" + exception.getMessage());
log.info("用户密码：{}", password);
log.info("Authorization：{}", authorization);
```

主要问题如下：

| 不推荐写法      | 问题                               |
| --------------- | ---------------------------------- |
| `"开始"`        | 无上下文，无法定位业务             |
| 字符串拼接      | 即使日志级别不输出，也会先执行拼接 |
| 只打印异常消息  | 缺少堆栈，不利于排查               |
| 打印密码、Token | 存在安全风险                       |

推荐在全局异常处理中统一记录异常日志：

```java
@ExceptionHandler(BizException.class)
public ResponseEntity<Result<Void>> handleBizException(BizException exception, HttpServletRequest request) {
    log.warn("业务异常，请求路径：{}，错误信息：{}", request.getRequestURI(), exception.getMessage());
    Result<Void> result = Result.fail(exception.getCode(), exception.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
}

@ExceptionHandler(Exception.class)
public ResponseEntity<Result<Void>> handleException(Exception exception, HttpServletRequest request) {
    log.error("系统异常，请求路径：{}", request.getRequestURI(), exception);
    Result<Void> result = Result.fail(ResultCode.INTERNAL_ERROR);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
}
```

对于需要链路追踪的项目，建议使用 `X-Request-Id` 或网关生成的 TraceId，并通过过滤器写入 MDC。

文件位置：`src/main/java/io/github/atengk/common/filter/RequestIdFilter.java`

以下代码用于从请求头中读取请求 ID，如果没有传入则自动生成，并写入日志上下文。

```java
package io.github.atengk.common.filter;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 请求ID日志过滤器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Component
public class RequestIdFilter extends OncePerRequestFilter {

    private static final String REQUEST_ID_HEADER = "X-Request-Id";

    private static final String REQUEST_ID_MDC_KEY = "requestId";

    /**
     * 过滤请求并写入请求ID
     *
     * @param request 请求对象
     * @param response 响应对象
     * @param filterChain 过滤器链
     * @throws ServletException Servlet异常
     * @throws IOException IO异常
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String requestId = request.getHeader(REQUEST_ID_HEADER);
        if (StrUtil.isBlank(requestId)) {
            requestId = IdUtil.fastSimpleUUID();
        }

        try {
            MDC.put(REQUEST_ID_MDC_KEY, requestId);
            response.setHeader(REQUEST_ID_HEADER, requestId);
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(REQUEST_ID_MDC_KEY);
        }
    }
}
```

配合 `logback-spring.xml` 输出请求 ID。

文件位置：`src/main/resources/logback-spring.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <!-- 控制台日志格式，包含 requestId，便于接口链路排查 -->
    <property name="CONSOLE_LOG_PATTERN"
              value="%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%thread] [%X{requestId}] %logger{36} - %msg%n"/>

    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>${CONSOLE_LOG_PATTERN}</pattern>
            <charset>UTF-8</charset>
        </encoder>
    </appender>

    <logger name="io.github.atengk" level="debug"/>

    <root level="info">
        <appender-ref ref="CONSOLE"/>
    </root>
</configuration>
```

日志输出示例：

```text
2026-05-06 10:00:00.123 INFO  [http-nio-8080-exec-1] [202605061000001] i.g.a.user.controller.UserController - 查询用户详情，用户ID：1001
```

日志打印注意事项：

| 注意项                         | 说明                               |
| ------------------------------ | ---------------------------------- |
| 不打印密码                     | 包括明文密码、加密前密码           |
| 不打印完整 Token               | 认证信息只记录是否存在，不输出原文 |
| 不打印身份证、银行卡等敏感信息 | 必要时先脱敏                       |
| 不在循环中大量打印 info 日志   | 大批量处理使用 summary 日志        |
| 系统异常必须打印堆栈           | 使用 `log.error("xxx", exception)` |
| 业务异常使用 warn              | 避免误报系统故障                   |

整体开发规范建议如下：

| 规范项     | 推荐做法                                                     |
| ---------- | ------------------------------------------------------------ |
| Controller | 只做请求接收、参数校验、调用 Service 和返回结果              |
| URL        | 资源名复数、小写、短横线、统一 `/api` 前缀                   |
| 请求对象   | 使用 `CreateRequest`、`UpdateRequest`、`QueryRequest` 区分场景 |
| 响应对象   | 使用 `VO` 或 `Response`，不直接返回 Entity                   |
| 响应结构   | 使用 `Result<T>` 统一封装                                    |
| 参数校验   | 使用 Validation 注解，不手写大量空值判断                     |
| 异常处理   | 使用 `@RestControllerAdvice` 统一处理                        |
| 日志       | 记录关键业务信息，避免敏感数据泄露                           |