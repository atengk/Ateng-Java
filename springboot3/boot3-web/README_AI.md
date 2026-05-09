# Spring Boot 3 开发

Spring Boot 3 是基于 Spring Framework 6 构建的现代 Java 应用开发框架，要求使用 Java 17 及以上版本。它通过自动配置、约定优于配置、内嵌 Web 容器和统一生态集成，降低企业级后端服务的初始化成本，适合快速构建 REST API、业务系统、微服务、后台管理服务和中间件集成应用。

## Spring Boot 3 项目概述

本章节用于说明 Spring Boot 3 在后端项目中的技术定位、适用范围、开发目标和核心建设内容，帮助开发者在编码前明确项目边界和工程结构。

### 技术定位

Spring Boot 3 的核心定位是简化 Spring 应用开发，提供一套开箱即用的企业级应用开发基础设施。它不是替代 Spring Framework，而是在 Spring Framework 之上封装了自动配置、依赖管理、运行监控、配置加载、内嵌容器、测试支持等能力。

在实际项目中，Spring Boot 3 通常作为后端服务的基础框架，负责完成以下工作：

- 提供 Web 接口开发能力，例如 REST API、文件上传下载、统一异常处理。
- 管理 Bean、依赖注入、配置属性和应用生命周期。
- 集成数据库、缓存、消息队列、对象存储、定时任务等基础设施。
- 提供多环境配置、日志、健康检查、指标监控和应用打包运行能力。
- 支持单体应用、分层架构应用和微服务应用开发。

Spring Boot 3 默认基于 Jakarta EE 命名空间，原来的 `javax.*` 相关包在多数场景下需要迁移为 `jakarta.*`。因此在升级旧项目时，需要特别关注 Servlet、Validation、Persistence 等依赖包路径的变化。

### 适用场景

Spring Boot 3 适合用于构建中大型 Java 后端系统，也适合快速开发轻量级业务服务。它对常见企业应用场景支持较完整，能够满足从本地开发到生产部署的完整工程需求。

常见适用场景包括：

- 后台管理系统后端服务，例如用户管理、角色权限、菜单管理、系统配置。
- RESTful API 服务，例如 App 接口、小程序接口、第三方开放接口。
- 微服务业务模块，例如订单服务、支付服务、库存服务、消息服务。
- 数据处理服务，例如数据同步、定时统计、报表生成、异步任务处理。
- 中间件集成服务，例如 Redis、RabbitMQ、Kafka、MinIO、Elasticsearch 集成。
- 内部工具服务，例如自动化运维接口、配置管理接口、任务调度接口。

不适合的场景主要包括极低资源占用的嵌入式程序、纯前端应用、强实时系统，以及不希望引入 JVM 运行环境的轻量脚本类任务。

### 项目开发目标

Spring Boot 3 项目开发的目标不是简单完成接口功能，而是形成一套稳定、清晰、可维护、可扩展的后端工程基础。一个规范的项目应当同时关注功能实现、代码结构、配置管理、异常处理、日志规范和运行维护。

项目开发目标主要包括：

- 建立清晰的分层结构，区分 Controller、Service、Repository、Model、Config、Common 等职责。
- 提供统一的接口响应结构，减少前后端联调成本。
- 提供统一的异常处理机制，避免异常信息散落在业务代码中。
- 提供统一的参数校验方式，提高接口输入数据的可靠性。
- 提供可维护的配置文件管理方式，支持开发、测试、生产多环境切换。
- 提供基础日志、运行监控和健康检查能力，方便问题排查和生产运维。
- 提供规范的包命名、类命名、方法命名和接口路径设计规则。
- 支持后续平滑扩展数据库访问、缓存、消息队列、认证授权、文件存储等能力。

### 核心开发内容

Spring Boot 3 项目的核心开发内容通常围绕接口、业务、数据、配置、异常、日志和运行维护展开。实际开发时，应优先搭建项目基础骨架，再逐步补充业务模块。

核心开发内容包括：

1. 工程初始化  
   创建 Maven 或 Gradle 项目，选择 Spring Web、Validation、Lombok、Actuator、数据库访问等基础依赖。

2. 分层结构设计  
   按照 Controller、Service、Repository、Model、Config、Common 等层次组织代码，避免所有类堆放在同一个包中。

3. 接口开发  
   使用 `@RestController`、`@RequestMapping`、`@GetMapping`、`@PostMapping` 等注解开发 REST API。

4. 参数处理与校验  
   使用 `@RequestParam`、`@PathVariable`、`@RequestBody` 接收参数，使用 Jakarta Validation 进行参数校验。

5. 统一响应与异常处理  
   封装统一响应对象，使用 `@RestControllerAdvice` 处理业务异常、参数异常和系统异常。

6. 配置管理  
   使用 `application.yml` 管理端口、应用名称、日志、数据源、多环境配置等内容。

7. 数据访问  
   根据项目复杂度选择 JdbcClient、JdbcTemplate、Spring Data JPA、MyBatis 或 MyBatis-Plus。

8. 日志与监控  
   使用 SLF4J + Logback 输出日志，使用 Spring Boot Actuator 暴露健康检查和基础监控端点。

9. 打包与运行  
   使用 Maven 构建 Jar 包，通过命令行、Profile、JVM 参数等方式运行应用。

## 项目初始化

本章节用于说明 Spring Boot 3 项目的创建方式、基础依赖选择、命名规范和启动类配置。项目初始化阶段的规范会直接影响后续代码组织、模块扩展和部署维护。

### 项目创建方式

Spring Boot 3 项目常见创建方式包括 Spring Initializr、IDEA 图形化创建、Maven 手工创建和企业脚手架创建。个人学习或新项目验证时推荐使用 Spring Initializr；企业项目推荐基于统一脚手架创建，保证依赖版本、包结构和基础能力一致。

推荐方式一：使用 Spring Initializr 创建项目。

访问 Spring Initializr 后，建议选择以下配置：

| 配置项       | 推荐值                  | 说明                                  |
| ------------ | ----------------------- | ------------------------------------- |
| Project      | Maven                   | 企业 Java 后端项目中 Maven 使用更普遍 |
| Language     | Java                    | Spring Boot 主流开发语言              |
| Spring Boot  | 3.x 稳定版本            | 优先选择当前稳定版本                  |
| Packaging    | Jar                     | 适合内嵌 Tomcat 独立运行              |
| Java         | 17 或 21                | Spring Boot 3 最低要求 Java 17        |
| Group        | `io.github.atengk`      | 组织或个人域名反写                    |
| Artifact     | `spring-boot3-demo`     | 项目构件名称                          |
| Package name | `io.github.atengk.demo` | Java 根包路径                         |

推荐方式二：使用 IDEA 创建项目。

在 IDEA 中选择 `New Project`，然后选择 `Spring Initializr`，填写 Group、Artifact、Name、Package name、JDK 和 Spring Boot 版本，最后勾选基础依赖即可。

推荐方式三：使用 Maven 手工创建项目。

适合已经有公司父工程、统一依赖管理或多模块结构的场景。手工创建时应先确定父工程、JDK 版本、编码格式、依赖版本管理方式和打包插件。

项目创建完成后，建议先执行以下命令确认工程可以正常构建：

```bash
# 在项目根目录执行，验证 Maven 工程是否可以正常编译
mvn clean package
```

该命令会清理旧构建产物并重新编译打包项目。如果执行成功，说明基础依赖、JDK 版本和 Maven 配置基本可用。

### 基础依赖选择

基础依赖应围绕项目实际功能选择，避免一次性引入大量暂时不用的中间件依赖。Spring Boot 3 项目初始阶段通常只需要 Web、Validation、Lombok、Actuator、配置处理器和工具类依赖。

以下 Maven 配置用于 Spring Boot 3 基础项目初始化，放在项目根目录的 `pom.xml` 中。

```xml
<dependencies>
    <!-- Spring Web：提供 REST API、MVC、JSON 序列化、内嵌 Tomcat 等能力 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- 参数校验：Spring Boot 3 使用 jakarta.validation 相关规范 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- 运行监控：提供健康检查、应用信息、指标端点 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>

    <!-- 配置元数据生成：便于 IDEA 对 @ConfigurationProperties 提供提示 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-configuration-processor</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- Lombok：减少 Getter、Setter、构造器、日志对象等样板代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- Hutool：常用工具类集合，简化字符串、集合、日期、文件等处理 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.8.36</version>
    </dependency>

    <!-- 测试依赖：提供 JUnit Jupiter、Spring Test、MockMvc 等测试能力 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

如果项目需要数据库访问，可以根据技术选型额外添加依赖：

| 场景              | 推荐依赖                                   |
| ----------------- | ------------------------------------------ |
| 简单 SQL 操作     | `spring-boot-starter-jdbc`                 |
| MyBatis 开发      | `mybatis-spring-boot-starter`              |
| MyBatis-Plus CRUD | `mybatis-plus-spring-boot3-starter`        |
| JPA ORM           | `spring-boot-starter-data-jpa`             |
| Redis 缓存        | `spring-boot-starter-data-redis`           |
| 安全认证          | `spring-boot-starter-security` 或 Sa-Token |
| 接口文档          | Springdoc OpenAPI                          |
| 对象存储          | MinIO SDK                                  |

依赖选择原则是按需引入、统一版本、避免重复能力。例如已经使用 MyBatis-Plus 时，通常不需要同时引入 Spring Data JPA。

### 项目命名规范

项目命名应清晰表达业务含义，避免使用无意义缩写。命名规范应覆盖项目名、模块名、包名、类名和接口路径。

项目名称建议使用小写字母和中划线：

```text
spring-boot3-demo
system-service
order-service
payment-service
file-center
```

Maven 坐标建议保持组织统一：

```text
groupId: io.github.atengk
artifactId: spring-boot3-demo
version: 1.0.0
```

Java 包名建议使用小写字母，不使用中划线和下划线：

```text
io.github.atengk.demo
io.github.atengk.system
io.github.atengk.order
```

类命名建议遵循职责后缀：

| 类型        | 命名示例          | 说明           |
| ----------- | ----------------- | -------------- |
| 启动类      | `DemoApplication` | 应用启动入口   |
| Controller  | `UserController`  | 接口控制层     |
| Service     | `UserService`     | 业务接口       |
| ServiceImpl | `UserServiceImpl` | 业务实现       |
| Repository  | `UserRepository`  | 数据访问层     |
| DTO         | `UserCreateDTO`   | 请求入参对象   |
| VO          | `UserVO`          | 响应视图对象   |
| Entity      | `UserEntity`      | 数据库实体对象 |
| Config      | `WebMvcConfig`    | 配置类         |
| Exception   | `BizException`    | 自定义异常     |
| Constants   | `CommonConstants` | 常量类         |

接口路径建议使用小写字母和中划线，使用名词表达资源：

```text
GET    /api/users
GET    /api/users/{id}
POST   /api/users
PUT    /api/users/{id}
DELETE /api/users/{id}
```

不推荐以下命名：

```text
/api/getUser
/api/add_user
/api/UserList
```

接口路径应避免暴露 Java 方法名，优先围绕资源和 HTTP 方法表达语义。

### 启动类配置

启动类是 Spring Boot 应用的入口类，通常放在根包路径下。启动类所在包应当是其他业务包的上级包，确保 Spring Boot 默认组件扫描可以扫描到 Controller、Service、Config 等 Bean。

文件位置：`src/main/java/io/github/atengk/demo/DemoApplication.java`

以下启动类用于启动 Spring Boot 3 应用，并在启动完成后输出应用启动日志。

```java
package io.github.atengk.demo;

import cn.hutool.core.date.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot 3 项目启动类。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@SpringBootApplication
public class DemoApplication {

    /**
     * 应用启动入口。
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
        log.info("Spring Boot 3 应用启动完成，启动时间：{}", DateUtil.now());
    }

}
```

启动类配置要点如下：

- `@SpringBootApplication` 是组合注解，包含自动配置、组件扫描和配置类能力。
- 启动类应放在根包下，例如 `io.github.atengk.demo`。
- Controller、Service、Repository、Config 等包应放在根包下级路径中。
- 不建议将启动类放在 `controller`、`service` 等子包中，否则可能导致组件扫描范围不完整。
- 如需扫描额外包路径，可以使用 `scanBasePackages`，但一般不建议滥用。

示例：

```java
@SpringBootApplication(scanBasePackages = "io.github.atengk")
public class DemoApplication {
}
```

只有当项目确实需要扫描多个平级包或外部公共模块时，才建议显式配置 `scanBasePackages`。

## 项目目录结构

本章节用于说明 Spring Boot 3 项目的推荐目录结构。合理的目录结构可以降低代码维护成本，使接口、业务、数据、配置和公共能力边界更加清晰。

推荐基础目录结构如下：

```text
spring-boot3-demo
├── pom.xml
├── README.md
├── src
│   ├── main
│   │   ├── java
│   │   │   └── io
│   │   │       └── github
│   │   │           └── atengk
│   │   │               └── demo
│   │   │                   ├── DemoApplication.java
│   │   │                   ├── common
│   │   │                   ├── config
│   │   │                   ├── controller
│   │   │                   ├── model
│   │   │                   ├── repository
│   │   │                   └── service
│   │   └── resources
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       ├── application-test.yml
│   │       ├── application-prod.yml
│   │       ├── mapper
│   │       └── static
│   └── test
│       └── java
│           └── io
│               └── github
│                   └── atengk
│                       └── demo
```

目录设计原则如下：

- 启动类放在根包下。
- 业务代码按职责分层，不按技术随意堆叠。
- 请求对象、响应对象、实体对象放入 `model` 下的不同子包。
- 公共响应、异常、常量、工具类放入 `common`。
- 配置类统一放入 `config`。
- 数据访问类统一放入 `repository`。
- Mapper XML、配置文件、静态资源统一放入 `resources`。

### Controller 层目录

Controller 层用于接收 HTTP 请求、校验基础参数、调用 Service 业务方法并返回响应结果。Controller 不应直接编写复杂业务逻辑，也不应直接访问数据库。

推荐目录：

```text
src/main/java/io/github/atengk/demo/controller
├── UserController.java
├── FileController.java
└── SystemController.java
```

Controller 层职责：

- 定义接口路径和请求方法。
- 接收路径参数、查询参数、请求体和请求头。
- 使用 `@Validated` 或 `@Valid` 触发参数校验。
- 调用 Service 完成业务处理。
- 返回统一响应对象。

Controller 层不建议做的事情：

- 不直接编写 SQL。
- 不直接操作 Repository。
- 不编写复杂业务规则。
- 不处理大量数据转换逻辑。
- 不吞掉异常，应交给全局异常处理。

文件位置：`src/main/java/io/github/atengk/demo/controller/UserController.java`

以下 Controller 示例提供用户查询和创建接口，并通过统一响应对象返回结果。

```java
package io.github.atengk.demo.controller;

import io.github.atengk.demo.common.response.ApiResult;
import io.github.atengk.demo.model.dto.UserCreateDTO;
import io.github.atengk.demo.model.vo.UserVO;
import io.github.atengk.demo.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 用户接口控制器。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    /**
     * 根据用户 ID 查询用户信息。
     *
     * @param id 用户 ID
     * @return 用户信息
     */
    @GetMapping("/{id}")
    public ApiResult<UserVO> getById(@PathVariable Long id) {
        return ApiResult.success(userService.getById(id));
    }

    /**
     * 创建用户。
     *
     * @param dto 用户创建参数
     * @return 用户信息
     */
    @PostMapping
    public ApiResult<UserVO> create(@Valid @RequestBody UserCreateDTO dto) {
        return ApiResult.success(userService.create(dto));
    }

}
```

### Service 层目录

Service 层用于承载业务逻辑，是项目中最核心的业务处理层。Controller 只负责请求入口，Repository 只负责数据访问，真正的业务规则应集中在 Service 层。

推荐目录：

```text
src/main/java/io/github/atengk/demo/service
├── UserService.java
└── impl
    └── UserServiceImpl.java
```

Service 层职责：

- 编排业务流程。
- 校验业务规则。
- 调用 Repository 或外部服务。
- 控制事务边界。
- 处理业务异常。
- 完成 DTO、Entity、VO 之间的数据转换。

接口文件位置：`src/main/java/io/github/atengk/demo/service/UserService.java`

```java
package io.github.atengk.demo.service;

import io.github.atengk.demo.model.dto.UserCreateDTO;
import io.github.atengk.demo.model.vo.UserVO;

/**
 * 用户业务接口。
 *
 * @author Ateng
 * @since 2026-05-09
 */
public interface UserService {

    /**
     * 根据用户 ID 查询用户信息。
     *
     * @param id 用户 ID
     * @return 用户信息
     */
    UserVO getById(Long id);

    /**
     * 创建用户。
     *
     * @param dto 用户创建参数
     * @return 用户信息
     */
    UserVO create(UserCreateDTO dto);

}
```

实现类文件位置：`src/main/java/io/github/atengk/demo/service/impl/UserServiceImpl.java`

```java
package io.github.atengk.demo.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.demo.common.exception.BizException;
import io.github.atengk.demo.model.dto.UserCreateDTO;
import io.github.atengk.demo.model.entity.UserEntity;
import io.github.atengk.demo.model.vo.UserVO;
import io.github.atengk.demo.repository.UserRepository;
import io.github.atengk.demo.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 用户业务实现。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    /**
     * 根据用户 ID 查询用户信息。
     *
     * @param id 用户 ID
     * @return 用户信息
     */
    @Override
    public UserVO getById(Long id) {
        UserEntity entity = userRepository.getById(id);
        if (entity == null) {
            log.warn("用户不存在，用户ID：{}", id);
            throw new BizException("用户不存在");
        }
        return BeanUtil.copyProperties(entity, UserVO.class);
    }

    /**
     * 创建用户。
     *
     * @param dto 用户创建参数
     * @return 用户信息
     */
    @Override
    public UserVO create(UserCreateDTO dto) {
        if (StrUtil.isBlank(dto.getUsername())) {
            throw new BizException("用户名不能为空");
        }

        UserEntity entity = BeanUtil.copyProperties(dto, UserEntity.class);
        UserEntity savedEntity = userRepository.save(entity);
        log.info("用户创建成功，用户ID：{}，用户名：{}", savedEntity.getId(), savedEntity.getUsername());

        return BeanUtil.copyProperties(savedEntity, UserVO.class);
    }

}
```

### Repository 层目录

Repository 层用于封装数据访问逻辑，对上层 Service 屏蔽具体的数据读取和写入方式。Repository 可以基于 JdbcClient、JdbcTemplate、MyBatis、MyBatis-Plus、JPA 等技术实现。

推荐目录：

```text
src/main/java/io/github/atengk/demo/repository
├── UserRepository.java
└── impl
    └── UserRepositoryImpl.java
```

Repository 层职责：

- 封装数据库查询、新增、修改、删除操作。
- 屏蔽底层数据访问技术细节。
- 返回 Entity 或基础数据对象。
- 不处理复杂业务规则。
- 不直接接收 Controller 请求参数。

接口文件位置：`src/main/java/io/github/atengk/demo/repository/UserRepository.java`

```java
package io.github.atengk.demo.repository;

import io.github.atengk.demo.model.entity.UserEntity;

/**
 * 用户数据访问接口。
 *
 * @author Ateng
 * @since 2026-05-09
 */
public interface UserRepository {

    /**
     * 根据用户 ID 查询用户。
     *
     * @param id 用户 ID
     * @return 用户实体
     */
    UserEntity getById(Long id);

    /**
     * 保存用户。
     *
     * @param entity 用户实体
     * @return 保存后的用户实体
     */
    UserEntity save(UserEntity entity);

}
```

在实际项目中，如果使用 MyBatis-Plus，可以将 Repository 替换为 Mapper 或者在 Repository 内部调用 Mapper。关键原则是不要让 Controller 直接访问数据库层。

### Model 层目录

Model 层用于存放项目中的数据模型对象。为了避免请求对象、响应对象和数据库实体混用，建议在 Model 下继续拆分 DTO、VO、Entity、Query 等子目录。

推荐目录：

```text
src/main/java/io/github/atengk/demo/model
├── dto
│   └── UserCreateDTO.java
├── entity
│   └── UserEntity.java
├── query
│   └── UserPageQuery.java
└── vo
    └── UserVO.java
```

常见模型类型说明：

| 类型   | 作用             | 示例            |
| ------ | ---------------- | --------------- |
| DTO    | 接收请求参数     | `UserCreateDTO` |
| VO     | 返回前端展示数据 | `UserVO`        |
| Entity | 映射数据库表     | `UserEntity`    |
| Query  | 查询条件对象     | `UserPageQuery` |
| BO     | 业务内部对象     | `UserBO`        |

DTO 文件位置：`src/main/java/io/github/atengk/demo/model/dto/UserCreateDTO.java`

```java
package io.github.atengk.demo.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户创建请求参数。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
public class UserCreateDTO {

    /**
     * 用户名。
     */
    @NotBlank(message = "用户名不能为空")
    @Size(max = 30, message = "用户名长度不能超过30个字符")
    private String username;

    /**
     * 昵称。
     */
    @Size(max = 50, message = "昵称长度不能超过50个字符")
    private String nickname;

}
```

VO 文件位置：`src/main/java/io/github/atengk/demo/model/vo/UserVO.java`

```java
package io.github.atengk.demo.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户响应对象。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
public class UserVO {

    /**
     * 用户 ID。
     */
    private Long id;

    /**
     * 用户名。
     */
    private String username;

    /**
     * 昵称。
     */
    private String nickname;

    /**
     * 创建时间。
     */
    private LocalDateTime createTime;

}
```

Entity 文件位置：`src/main/java/io/github/atengk/demo/model/entity/UserEntity.java`

```java
package io.github.atengk.demo.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体对象。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
public class UserEntity {

    /**
     * 用户 ID。
     */
    private Long id;

    /**
     * 用户名。
     */
    private String username;

    /**
     * 昵称。
     */
    private String nickname;

    /**
     * 创建时间。
     */
    private LocalDateTime createTime;

}
```

Model 层使用建议：

- DTO 不直接作为数据库实体使用。
- Entity 不直接返回给前端。
- VO 不参与数据库写入。
- 查询参数复杂时单独定义 Query 对象。
- 对象转换可以使用 Hutool `BeanUtil`，也可以使用 MapStruct。

### Config 配置目录

Config 目录用于存放 Spring Boot 项目的配置类。配置类应按功能拆分，避免将所有配置集中到一个大类中。

推荐目录：

```text
src/main/java/io/github/atengk/demo/config
├── WebMvcConfig.java
├── JacksonConfig.java
├── AsyncConfig.java
└── CorsConfig.java
```

Config 层职责：

- 注册 WebMvc 配置。
- 配置跨域、拦截器、类型转换器。
- 配置 Jackson 序列化规则。
- 配置线程池、异步任务、定时任务。
- 配置第三方组件 Bean。

文件位置：`src/main/java/io/github/atengk/demo/config/WebMvcConfig.java`

以下配置类用于注册 Spring MVC 相关扩展，后续可以在其中加入拦截器、跨域、格式化器等配置。

```java
package io.github.atengk.demo.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC 配置。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * 创建 Spring MVC 配置对象。
     */
    public WebMvcConfig() {
        log.info("初始化 Spring MVC 配置");
    }

}
```

配置类编写建议：

- 配置类使用 `@Configuration` 标注。
- 与业务强相关的 Bean 不建议放入通用 Config。
- 配置项较多时使用 `@ConfigurationProperties` 绑定。
- 配置类中避免编写复杂业务逻辑。
- 不同能力使用不同配置类拆分，例如 Web、Jackson、Redis、ThreadPool。

### Common 公共目录

Common 目录用于存放跨模块复用的公共能力，例如统一响应、异常定义、常量、工具类、枚举、上下文对象等。Common 中的代码应保持稳定、通用，不应放入具体业务逻辑。

推荐目录：

```text
src/main/java/io/github/atengk/demo/common
├── constant
│   └── CommonConstants.java
├── exception
│   └── BizException.java
├── response
│   └── ApiResult.java
├── enums
│   └── ResultCodeEnum.java
└── util
    └── TraceIdUtil.java
```

Common 目录常见内容：

| 子目录       | 说明                   |
| ------------ | ---------------------- |
| `constant`   | 全局常量               |
| `exception`  | 自定义异常             |
| `response`   | 统一响应对象           |
| `enums`      | 通用枚举               |
| `util`       | 工具类                 |
| `context`    | 请求上下文、用户上下文 |
| `handler`    | 全局处理器             |
| `annotation` | 自定义注解             |

文件位置：`src/main/java/io/github/atengk/demo/common/response/ApiResult.java`

以下统一响应对象用于规范接口返回结构，便于前端统一处理成功、失败和错误信息。

```java
package io.github.atengk.demo.common.response;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一接口响应对象。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
public class ApiResult<T> implements Serializable {

    /**
     * 响应码。
     */
    private Integer code;

    /**
     * 响应消息。
     */
    private String message;

    /**
     * 响应数据。
     */
    private T data;

    /**
     * 创建成功响应。
     *
     * @param data 响应数据
     * @param <T> 数据类型
     * @return 统一响应对象
     */
    public static <T> ApiResult<T> success(T data) {
        ApiResult<T> result = new ApiResult<>();
        result.setCode(200);
        result.setMessage("操作成功");
        result.setData(data);
        return result;
    }

    /**
     * 创建失败响应。
     *
     * @param code 响应码
     * @param message 响应消息
     * @param <T> 数据类型
     * @return 统一响应对象
     */
    public static <T> ApiResult<T> fail(Integer code, String message) {
        ApiResult<T> result = new ApiResult<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }

}
```

文件位置：`src/main/java/io/github/atengk/demo/common/exception/BizException.java`

以下业务异常用于主动中断不符合业务规则的流程，并交由全局异常处理器统一返回错误结果。

```java
package io.github.atengk.demo.common.exception;

import lombok.Getter;

/**
 * 业务异常。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Getter
public class BizException extends RuntimeException {

    /**
     * 响应码。
     */
    private final Integer code;

    /**
     * 创建业务异常。
     *
     * @param message 异常消息
     */
    public BizException(String message) {
        super(message);
        this.code = 500;
    }

    /**
     * 创建业务异常。
     *
     * @param code 响应码
     * @param message 异常消息
     */
    public BizException(Integer code, String message) {
        super(message);
        this.code = code;
    }

}
```

Common 目录使用建议：

- 公共类必须具备复用价值，不能把具体业务代码放入 Common。
- 异常、响应、枚举、常量应统一维护，避免每个模块重复定义。
- 工具类优先使用成熟工具库，例如 Hutool；只有项目确实需要时再自定义工具类。
- 公共能力要保持稳定，修改 Common 代码时要评估对所有业务模块的影响。





## Maven 工程配置

Maven 工程配置用于统一项目构建方式、依赖版本、插件行为和模块组织。Spring Boot 3 项目推荐使用 Maven 3.6.3 及以上版本，并使用 Java 17 及以上版本作为基础运行环境。Spring Boot 3.x 官方文档中明确要求 Java 17，并对 Maven、Gradle 等构建工具版本有明确支持范围。([Home](https://docs.spring.io/spring-boot/docs/3.2.5/reference/htmlsingle/index.html?utm_source=chatgpt.com))

### 父工程配置

父工程配置用于统一管理 Spring Boot 版本、Java 编译版本、依赖版本和构建插件。单模块项目可以直接继承 `spring-boot-starter-parent`；多模块项目可以在父工程中统一继承 Spring Boot 父工程，再由各业务模块继承公司内部父工程。

单模块项目推荐使用以下 `pom.xml` 结构。

文件位置：`pom.xml`

以下配置定义了 Spring Boot 3 项目的基础父工程、Java 版本、项目坐标和常用依赖。

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">

    <modelVersion>4.0.0</modelVersion>

    <!-- Spring Boot 父工程：统一管理 Spring Boot 依赖版本和插件默认配置 -->
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.5.14</version>
        <relativePath/>
    </parent>

    <groupId>io.github.atengk</groupId>
    <artifactId>spring-boot3-demo</artifactId>
    <version>1.0.0</version>
    <name>spring-boot3-demo</name>
    <description>Spring Boot 3 开发示例项目</description>
    <packaging>jar</packaging>

    <properties>
        <!-- Spring Boot 3 最低要求 Java 17，生产项目建议统一使用 17 或 21 -->
        <java.version>17</java.version>

        <!-- 项目源码编码 -->
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>

        <!-- 第三方依赖版本集中管理 -->
        <hutool.version>5.8.36</hutool.version>
    </properties>

    <dependencies>
        <!-- Web 开发基础依赖，包含 Spring MVC、JSON、内嵌 Tomcat -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- 参数校验依赖，Spring Boot 3 使用 jakarta.validation -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <!-- Actuator 运行监控依赖，提供健康检查、指标、应用信息等端点 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>

        <!-- Hutool 工具类，简化字符串、集合、日期、Bean、文件等常见处理 -->
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
            <version>${hutool.version}</version>
        </dependency>

        <!-- Lombok 简化实体类、构造器、日志对象等样板代码 -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- 测试依赖，包含 JUnit Jupiter、Spring Test、MockMvc 等 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

</project>
```

如果公司已有统一父工程，不方便直接继承 `spring-boot-starter-parent`，可以使用 `dependencyManagement` 导入 Spring Boot 依赖版本清单。该方式适合企业内部已有父 POM、需要统一插件、仓库、代码规范和发布流程的场景。

文件位置：`pom.xml`

以下配置不继承 Spring Boot 父工程，而是通过 BOM 管理 Spring Boot 依赖版本。

```xml
<properties>
    <!-- Spring Boot 版本统一由属性控制，便于后续升级 -->
    <spring-boot.version>3.5.14</spring-boot.version>
    <java.version>17</java.version>
</properties>

<dependencyManagement>
    <dependencies>
        <!-- 导入 Spring Boot 依赖版本清单，不需要逐个声明 Spring 相关依赖版本 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-dependencies</artifactId>
            <version>${spring-boot.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

使用 BOM 方式时，需要自己配置 Maven 编译插件和 Spring Boot 打包插件；而继承 `spring-boot-starter-parent` 时，Spring Boot 已经提供了一批合理的默认插件配置。

### 依赖版本管理

依赖版本管理用于解决版本冲突、重复声明和升级困难的问题。Spring Boot 已经统一管理了大量 Spring 生态和常见第三方库版本，因此项目中引入 Spring Boot 官方 Starter 时，一般不需要手动声明版本号。

推荐原则如下：

- Spring Boot 官方 Starter 不写版本号。
- 公司内部组件统一放在父工程或 BOM 中管理。
- 非 Spring Boot 管理的第三方依赖，统一放入 `<properties>` 或 `<dependencyManagement>`。
- 业务模块只声明需要什么依赖，不负责决定依赖版本。
- 避免在不同模块中声明同一个依赖的不同版本。

文件位置：`pom.xml`

以下配置演示如何集中管理常见第三方依赖版本。

```xml
<properties>
    <!-- 工具类版本 -->
    <hutool.version>5.8.36</hutool.version>

    <!-- MyBatis-Plus Spring Boot 3 Starter 版本 -->
    <mybatis-plus.version>3.5.12</mybatis-plus.version>

    <!-- Knife4j 接口文档版本，根据项目实际情况选择是否引入 -->
    <knife4j.version>4.5.0</knife4j.version>
</properties>

<dependencyManagement>
    <dependencies>
        <!-- MyBatis-Plus：用于简化常见 CRUD 和分页查询 -->
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
            <version>${mybatis-plus.version}</version>
        </dependency>

        <!-- Hutool：常用 Java 工具类集合 -->
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
            <version>${hutool.version}</version>
        </dependency>

        <!-- Knife4j：接口文档增强工具 -->
        <dependency>
            <groupId>com.github.xiaoymin</groupId>
            <artifactId>knife4j-openapi3-jakarta-spring-boot-starter</artifactId>
            <version>${knife4j.version}</version>
        </dependency>
    </dependencies>
</dependencyManagement>
```

业务模块中只需要声明依赖坐标，不再声明版本。

```xml
<dependencies>
    <!-- 版本由 dependencyManagement 统一管理 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
    </dependency>

    <!-- 版本由 dependencyManagement 统一管理 -->
    <dependency>
        <groupId>com.baomidou</groupId>
        <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
    </dependency>
</dependencies>
```

依赖冲突排查可以使用以下命令。

```bash
# 查看完整依赖树，排查重复依赖和版本冲突
mvn dependency:tree

# 只查看指定关键字相关依赖，例如 jackson
mvn dependency:tree -Dincludes=com.fasterxml.jackson.core
```

`dependency:tree` 用于输出 Maven 实际解析后的依赖树。`-Dincludes` 可以过滤指定 groupId 或 artifactId，适合排查 Jackson、Netty、Guava、MyBatis 等依赖冲突问题。

### 打包插件配置

Spring Boot Maven Plugin 用于将普通 Jar 重新打包为可执行 Jar，使应用可以通过 `java -jar` 直接启动。官方 Maven 插件文档说明，`repackage` 目标会在 Maven `package` 阶段生成包含应用依赖的可执行归档文件，继承 `spring-boot-starter-parent` 时该行为已经有默认配置。([Home](https://docs.spring.io/spring-boot/maven-plugin/packaging.html?utm_source=chatgpt.com))

文件位置：`pom.xml`

以下配置用于生成可执行 Jar，并排除 Lombok 这类仅编译期使用的依赖。

```xml
<build>
    <finalName>spring-boot3-demo</finalName>

    <plugins>
        <!-- Spring Boot 打包插件：生成可通过 java -jar 运行的可执行 Jar -->
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
            <configuration>
                <!-- 启动类，通常可以自动识别；多启动类项目建议显式指定 -->
                <mainClass>io.github.atengk.demo.DemoApplication</mainClass>

                <!-- 排除编译期工具依赖，避免进入最终运行包 -->
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
```

常用打包命令如下。

```bash
# 清理并打包项目
mvn clean package

# 跳过测试执行，但仍编译测试代码
mvn clean package -DskipTests

# 跳过测试编译和测试执行
mvn clean package -Dmaven.test.skip=true

# 启动打包后的应用
java -jar target/spring-boot3-demo.jar
```

`-DskipTests` 只跳过测试运行，测试源码仍会编译；`-Dmaven.test.skip=true` 会跳过测试源码编译和测试执行。生产构建中不建议长期跳过测试，除非构建流水线已经在前置阶段完成了测试验证。

如果需要在 Jar 中写入构建信息，可以添加 `build-info` 目标，配合 Actuator 的 `/actuator/info` 端点查看版本信息。

文件位置：`pom.xml`

以下配置用于在构建产物中写入构建时间、版本等信息。

```xml
<build>
    <plugins>
        <!-- Spring Boot 打包插件：生成可执行 Jar，并写入构建信息 -->
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
            <executions>
                <execution>
                    <goals>
                        <!-- 生成 META-INF/build-info.properties -->
                        <goal>build-info</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

### 多模块工程结构

多模块工程适合中大型项目，用于拆分公共能力、业务模块、接口模块和启动模块。多模块不是越多越好，只有当项目存在明确边界、复用诉求或团队协作需要时才建议拆分。

推荐多模块结构如下：

```text
spring-boot3-parent
├── pom.xml
├── spring-boot3-common
│   └── pom.xml
├── spring-boot3-domain
│   └── pom.xml
├── spring-boot3-repository
│   └── pom.xml
├── spring-boot3-service
│   └── pom.xml
└── spring-boot3-web
    └── pom.xml
```

模块职责建议如下：

| 模块                      | 职责                                   |
| ------------------------- | -------------------------------------- |
| `spring-boot3-common`     | 公共响应、异常、常量、工具类、基础枚举 |
| `spring-boot3-domain`     | Entity、DTO、VO、Query 等模型对象      |
| `spring-boot3-repository` | Mapper、Repository、数据访问实现       |
| `spring-boot3-service`    | 业务接口和业务实现                     |
| `spring-boot3-web`        | 启动类、Controller、Web 配置、接口入口 |

父工程只负责版本管理和模块聚合，不放业务代码。

文件位置：`spring-boot3-parent/pom.xml`

以下父工程配置用于聚合多个子模块，并统一管理 Java、Spring Boot 和第三方依赖版本。

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">

    <modelVersion>4.0.0</modelVersion>

    <!-- 统一继承 Spring Boot 父工程，简化依赖和插件配置 -->
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.5.14</version>
        <relativePath/>
    </parent>

    <groupId>io.github.atengk</groupId>
    <artifactId>spring-boot3-parent</artifactId>
    <version>1.0.0</version>
    <packaging>pom</packaging>

    <modules>
        <module>spring-boot3-common</module>
        <module>spring-boot3-domain</module>
        <module>spring-boot3-repository</module>
        <module>spring-boot3-service</module>
        <module>spring-boot3-web</module>
    </modules>

    <properties>
        <java.version>17</java.version>
        <hutool.version>5.8.36</hutool.version>
        <mybatis-plus.version>3.5.12</mybatis-plus.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <!-- 公共模块版本由父工程统一管理 -->
            <dependency>
                <groupId>io.github.atengk</groupId>
                <artifactId>spring-boot3-common</artifactId>
                <version>${project.version}</version>
            </dependency>

            <dependency>
                <groupId>io.github.atengk</groupId>
                <artifactId>spring-boot3-domain</artifactId>
                <version>${project.version}</version>
            </dependency>

            <dependency>
                <groupId>io.github.atengk</groupId>
                <artifactId>spring-boot3-repository</artifactId>
                <version>${project.version}</version>
            </dependency>

            <dependency>
                <groupId>io.github.atengk</groupId>
                <artifactId>spring-boot3-service</artifactId>
                <version>${project.version}</version>
            </dependency>

            <!-- 第三方依赖版本统一管理 -->
            <dependency>
                <groupId>cn.hutool</groupId>
                <artifactId>hutool-all</artifactId>
                <version>${hutool.version}</version>
            </dependency>

            <dependency>
                <groupId>com.baomidou</groupId>
                <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
                <version>${mybatis-plus.version}</version>
            </dependency>
        </dependencies>
    </dependencyManagement>

</project>
```

启动模块负责引入 Controller、Service 和基础运行依赖。

文件位置：`spring-boot3-web/pom.xml`

以下配置用于定义最终可启动的 Web 模块。

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">

    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>io.github.atengk</groupId>
        <artifactId>spring-boot3-parent</artifactId>
        <version>1.0.0</version>
    </parent>

    <artifactId>spring-boot3-web</artifactId>
    <packaging>jar</packaging>

    <dependencies>
        <!-- Web 接口开发依赖 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- 参数校验依赖 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <!-- 业务模块依赖 -->
        <dependency>
            <groupId>io.github.atengk</groupId>
            <artifactId>spring-boot3-service</artifactId>
        </dependency>

        <!-- 公共模块依赖 -->
        <dependency>
            <groupId>io.github.atengk</groupId>
            <artifactId>spring-boot3-common</artifactId>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <!-- 只有启动模块需要配置 Spring Boot 打包插件 -->
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <mainClass>io.github.atengk.demo.DemoApplication</mainClass>
                </configuration>
            </plugin>
        </plugins>
    </build>

</project>
```

多模块依赖方向建议保持单向依赖，避免循环引用。

```text
web -> service -> repository -> domain
web -> common
service -> common
repository -> common
domain -> common
```

不建议出现以下依赖关系：

```text
common -> service
domain -> repository
repository -> web
service -> web
```

公共模块应保持轻量，不应依赖业务模块；业务模块可以依赖公共模块，但不能反向依赖 Controller 或启动模块。

## Spring Boot 启动流程

Spring Boot 启动流程用于说明应用从 `main` 方法执行到容器完成初始化的核心过程。理解启动流程有助于排查配置不生效、Bean 未注册、自动配置未加载、启动参数未读取等问题。

### 启动类入口

启动类是 Spring Boot 应用的主入口，通常只保留最少的启动逻辑。启动类应放在根包路径下，使组件扫描可以覆盖 Controller、Service、Repository、Config 等子包。

文件位置：`src/main/java/io/github/atengk/demo/DemoApplication.java`

以下启动类用于启动 Spring Boot 3 应用，并输出启动完成日志。

```java
package io.github.atengk.demo;

import cn.hutool.core.date.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot 3 应用启动类。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@SpringBootApplication
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
        log.info("应用启动完成，当前时间：{}", DateUtil.now());
    }

}
```

`@SpringBootApplication` 是启动类上最常用的组合注解，包含组件扫描、自动配置和配置类能力。官方文档建议通常只在主配置类上添加一个 `@SpringBootApplication` 或 `@EnableAutoConfiguration`，避免多个入口重复启用自动配置。([Home](https://docs.spring.io/spring-boot/reference/using/auto-configuration.html?utm_source=chatgpt.com))

启动类放置建议：

- 推荐位置：`io.github.atengk.demo.DemoApplication`
- Controller 位置：`io.github.atengk.demo.controller`
- Service 位置：`io.github.atengk.demo.service`
- Config 位置：`io.github.atengk.demo.config`
- Common 位置：`io.github.atengk.demo.common`

这样 Spring Boot 默认组件扫描可以覆盖所有下级包。

### SpringApplication 启动过程

`SpringApplication.run()` 是 Spring Boot 启动的核心入口。它会创建应用运行上下文、准备环境变量、加载配置、创建 Spring 容器、刷新上下文、启动内嵌 Web 服务器，并在启动完成后执行 Runner 回调。

典型启动过程可以理解为以下步骤：

1. 创建 `SpringApplication` 对象。
2. 推断应用类型，例如 Servlet Web 应用、Reactive Web 应用或普通非 Web 应用。
3. 加载初始化器和监听器。
4. 解析命令行参数。
5. 准备 `Environment`，加载配置文件、环境变量、系统属性和命令行参数。
6. 打印 Banner。
7. 创建 `ApplicationContext`。
8. 加载 BeanDefinition，包括业务 Bean 和自动配置 Bean。
9. 刷新 Spring 容器。
10. 启动内嵌 Web 容器。
11. 执行 `CommandLineRunner` 和 `ApplicationRunner`。
12. 发布启动完成事件。

如果需要更精细地控制启动行为，可以使用 `SpringApplication` 对象进行配置。

文件位置：`src/main/java/io/github/atengk/demo/DemoApplication.java`

以下启动方式显式创建 `SpringApplication`，用于配置 Banner、默认属性、启动参数处理等行为。

```java
package io.github.atengk.demo;

import cn.hutool.core.map.MapUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Map;

/**
 * Spring Boot 3 应用启动类。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@SpringBootApplication
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(DemoApplication.class);

        // 控制台关闭 Banner，生产环境可根据团队规范选择是否关闭
        application.setBannerMode(Banner.Mode.OFF);

        // 设置默认属性，优先级低于 application.yml、环境变量和命令行参数
        Map<String, Object> defaultProperties = MapUtil.<String, Object>builder()
                .put("server.port", 8080)
                .put("spring.application.name", "spring-boot3-demo")
                .build();
        application.setDefaultProperties(defaultProperties);

        application.run(args);
        log.info("SpringApplication 启动完成");
    }

}
```

默认属性适合放置兜底配置，不适合放置生产环境敏感配置。数据库密码、密钥、Token 等敏感信息应通过环境变量、配置中心或部署平台注入。

### 应用上下文初始化

应用上下文初始化是 Spring Boot 启动流程中的核心阶段。`ApplicationContext` 负责管理 Bean 的定义、创建、依赖注入、生命周期回调、事件发布和资源加载。

在 Web 项目中，Spring Boot 会创建适合 Servlet 环境的应用上下文，并在刷新容器时完成以下关键动作：

- 扫描 `@Component`、`@Service`、`@Repository`、`@Controller` 等组件。
- 处理 `@Configuration` 配置类。
- 加载自动配置类。
- 注册 BeanDefinition。
- 实例化单例 Bean。
- 执行依赖注入。
- 调用初始化方法。
- 启动内嵌 Tomcat。
- 发布容器刷新完成事件。

如果需要在应用启动完成后执行初始化逻辑，可以使用 `ApplicationRunner` 或 `CommandLineRunner`。两者都会在 Spring 容器初始化完成后执行，适合做缓存预热、字典加载、启动检查等任务。

文件位置：`src/main/java/io/github/atengk/demo/runner/ApplicationStartupRunner.java`

以下 Runner 用于在应用启动完成后输出启动参数和初始化日志。

```java
package io.github.atengk.demo.runner;

import cn.hutool.core.collection.CollUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * 应用启动完成后的初始化任务。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApplicationStartupRunner implements ApplicationRunner {

    private final Environment environment;

    @Override
    public void run(ApplicationArguments args) {
        String applicationName = environment.getProperty("spring.application.name", "unknown");
        String port = environment.getProperty("server.port", "8080");

        log.info("应用初始化完成，应用名称：{}，端口：{}", applicationName, port);

        if (CollUtil.isNotEmpty(args.getOptionNames())) {
            log.info("启动参数名称：{}", String.join(",", args.getOptionNames()));
        }
    }

}
```

Runner 中不建议执行耗时过长的阻塞任务。如果启动任务必须访问外部系统，例如数据库、Redis、对象存储或第三方接口，应增加超时控制和异常日志，避免应用启动过程长时间卡住。

### 启动参数处理

Spring Boot 支持通过命令行参数、系统属性、环境变量、配置文件等方式传入配置。官方文档说明，默认情况下，`SpringApplication` 会将 `--server.port=9000` 这类命令行选项参数转换为属性并加入 `Environment`，且命令行参数优先级高于文件配置。([Home](https://docs.spring.io/spring-boot/3.5-SNAPSHOT/reference/features/external-config.html?utm_source=chatgpt.com))

常见启动参数示例如下。

```bash
# 指定服务端口
java -jar spring-boot3-demo.jar --server.port=9000

# 指定运行环境
java -jar spring-boot3-demo.jar --spring.profiles.active=prod

# 同时指定应用名称、端口和环境
java -jar spring-boot3-demo.jar \
  --spring.application.name=spring-boot3-demo \
  --server.port=9000 \
  --spring.profiles.active=prod
```

启动参数可以通过 `ApplicationArguments` 读取。

文件位置：`src/main/java/io/github/atengk/demo/runner/StartupArgsRunner.java`

以下代码用于读取命令行启动参数，并输出关键参数值。

```java
package io.github.atengk.demo.runner;

import cn.hutool.core.collection.CollUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 启动参数读取任务。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Component
public class StartupArgsRunner implements ApplicationRunner {

    @Override
    public void run(ApplicationArguments args) {
        if (args.containsOption("server.port")) {
            List<String> ports = args.getOptionValues("server.port");
            if (CollUtil.isNotEmpty(ports)) {
                log.info("命令行指定服务端口：{}", ports.get(0));
            }
        }

        if (args.containsOption("spring.profiles.active")) {
            List<String> profiles = args.getOptionValues("spring.profiles.active");
            if (CollUtil.isNotEmpty(profiles)) {
                log.info("命令行指定运行环境：{}", profiles.get(0));
            }
        }

        if (CollUtil.isEmpty(args.getOptionNames())) {
            log.info("未读取到命令行启动参数");
        }
    }

}
```

如果项目不希望命令行参数覆盖配置文件，可以关闭命令行属性添加。

文件位置：`src/main/java/io/github/atengk/demo/DemoApplication.java`

以下配置关闭命令行参数加入 `Environment` 的行为。

```java
package io.github.atengk.demo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot 3 应用启动类。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@SpringBootApplication
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(DemoApplication.class);

        // 禁止命令行参数覆盖配置文件
        application.setAddCommandLineProperties(false);

        application.run(args);
        log.info("应用启动完成，已关闭命令行参数属性注入");
    }

}
```

关闭命令行参数覆盖能力后，`--server.port=9000` 这类参数不会再作为配置属性加入 Spring 环境。生产环境中是否关闭该能力，应结合部署平台和配置管理规范决定。

## 自动配置机制

自动配置是 Spring Boot 的核心能力之一。它会根据当前项目的 classpath、已经存在的 Bean、配置属性和运行环境，自动装配合适的组件。Spring Boot 官方文档说明，自动配置会根据项目添加的 Jar 依赖尝试配置 Spring 应用，并且可以通过 `@SpringBootApplication` 或 `@EnableAutoConfiguration` 启用。([Home](https://docs.spring.io/spring-boot/reference/using/auto-configuration.html?utm_source=chatgpt.com))

### 自动配置加载流程

自动配置加载流程可以理解为“先识别依赖，再判断条件，最后注册 Bean”。例如项目引入 `spring-boot-starter-web` 后，Spring Boot 会识别到 Web MVC、Tomcat、Jackson 等相关类存在，然后根据条件装配 Web 容器、JSON 序列化、DispatcherServlet 等基础组件。

核心流程如下：

1. 启动类上的 `@SpringBootApplication` 间接启用自动配置。
2. Spring Boot 读取自动配置候选类。
3. 根据条件注解判断自动配置是否生效。
4. 如果条件满足，注册对应配置类中的 Bean。
5. 如果用户已经自定义同类型 Bean，部分自动配置会自动退让。
6. 应用上下文刷新时完成 Bean 创建和依赖注入。

自动配置不是强制覆盖用户配置。官方文档指出，自动配置是非侵入式的，用户可以通过定义自己的配置替换自动配置中的特定部分；例如用户定义了自己的 `DataSource` Bean 后，默认数据源自动配置会退让。([Home](https://docs.spring.io/spring-boot/reference/using/auto-configuration.html?utm_source=chatgpt.com))

启动时可以增加 `--debug` 参数查看自动配置条件报告。

```bash
# 启动时输出自动配置条件匹配报告
java -jar spring-boot3-demo.jar --debug
```

条件报告中通常包含三类信息：

| 类型             | 说明                         |
| ---------------- | ---------------------------- |
| Positive matches | 条件匹配成功，自动配置已生效 |
| Negative matches | 条件不满足，自动配置未生效   |
| Exclusions       | 被手动排除的自动配置         |

该命令适合排查“为什么某个 Bean 没有自动创建”“为什么某个自动配置生效了”“为什么配置文件修改后没有达到预期效果”等问题。

### 条件装配规则

条件装配是自动配置能否生效的判断基础。Spring Boot 自动配置类通常会使用条件注解限制生效范围，避免在依赖不存在、配置未开启或用户已自定义 Bean 时错误装配。Spring Boot 自动配置类通常使用 `@ConditionalOnClass`、`@ConditionalOnMissingBean` 等条件注解来约束配置是否应用。([Home](https://docs.spring.io/spring-boot/reference/features/developing-auto-configuration.html?utm_source=chatgpt.com))

常见条件注解如下：

| 注解                              | 作用                           |
| --------------------------------- | ------------------------------ |
| `@ConditionalOnClass`             | classpath 中存在指定类时生效   |
| `@ConditionalOnMissingClass`      | classpath 中不存在指定类时生效 |
| `@ConditionalOnBean`              | 容器中存在指定 Bean 时生效     |
| `@ConditionalOnMissingBean`       | 容器中不存在指定 Bean 时生效   |
| `@ConditionalOnProperty`          | 指定配置属性满足条件时生效     |
| `@ConditionalOnWebApplication`    | 当前是 Web 应用时生效          |
| `@ConditionalOnNotWebApplication` | 当前不是 Web 应用时生效        |

实际项目中也可以编写自己的条件配置类。例如只有当配置项 `demo.feature.enabled=true` 时，才注册某个业务组件。

文件位置：`src/main/java/io/github/atengk/demo/config/DemoFeatureConfig.java`

以下配置类演示基于配置属性的条件装配。

```java
package io.github.atengk.demo.config;

import io.github.atengk.demo.service.DemoFeatureService;
import io.github.atengk.demo.service.impl.DefaultDemoFeatureServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 示例功能条件装配配置。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Configuration
public class DemoFeatureConfig {

    @Bean
    @ConditionalOnProperty(prefix = "demo.feature", name = "enabled", havingValue = "true")
    public DemoFeatureService demoFeatureService() {
        log.info("示例功能已开启，注册 DemoFeatureService");
        return new DefaultDemoFeatureServiceImpl();
    }

}
```

文件位置：`src/main/resources/application.yml`

以下配置用于控制示例功能是否开启。

```yaml
demo:
  feature:
    # true 表示注册 DemoFeatureService，false 或未配置表示不注册
    enabled: true
```

文件位置：`src/main/java/io/github/atengk/demo/service/DemoFeatureService.java`

以下接口定义示例功能的业务能力。

```java
package io.github.atengk.demo.service;

/**
 * 示例功能业务接口。
 *
 * @author Ateng
 * @since 2026-05-09
 */
public interface DemoFeatureService {

    String getFeatureName();

}
```

文件位置：`src/main/java/io/github/atengk/demo/service/impl/DefaultDemoFeatureServiceImpl.java`

以下实现类用于返回示例功能名称。

```java
package io.github.atengk.demo.service.impl;

import io.github.atengk.demo.service.DemoFeatureService;

/**
 * 默认示例功能业务实现。
 *
 * @author Ateng
 * @since 2026-05-09
 */
public class DefaultDemoFeatureServiceImpl implements DemoFeatureService {

    @Override
    public String getFeatureName() {
        return "default-demo-feature";
    }

}
```

条件装配常用于以下场景：

- 某个功能需要通过配置开关控制。
- 某个 Bean 只有在依赖存在时才注册。
- 默认实现需要在用户未自定义实现时才注册。
- Web 环境和非 Web 环境需要使用不同配置。
- 本地环境、测试环境、生产环境需要差异化装配。

### 自动配置覆盖方式

自动配置覆盖不是修改 Spring Boot 源码，而是在项目中定义自己的 Bean、配置属性或配置类，让 Spring Boot 默认配置退让或使用新的配置。多数 Spring Boot 自动配置都会遵守“用户优先”的原则，即用户已经提供 Bean 时，自动配置不再重复创建默认 Bean。

常见覆盖方式包括：

1. 自定义同类型 Bean。
2. 修改配置文件中的属性。
3. 使用 `@Configuration` 提供自定义配置。
4. 使用 `WebMvcConfigurer`、`Jackson2ObjectMapperBuilderCustomizer` 等扩展点。
5. 使用 Starter 暴露的配置属性覆盖默认行为。

例如，自定义 Jackson 日期格式和时区。

文件位置：`src/main/java/io/github/atengk/demo/config/JacksonConfig.java`

以下配置类用于覆盖 Jackson 默认序列化行为，统一处理日期时间格式。

```java
package io.github.atengk.demo.config;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Jackson JSON 序列化配置。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jackson2ObjectMapperBuilderCustomizer() {
        return builder -> {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            builder.serializerByType(LocalDateTime.class, new LocalDateTimeSerializer(formatter));
            builder.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

            log.info("初始化 Jackson 日期时间序列化配置");
        };
    }

}
```

也可以通过配置文件覆盖部分默认行为。

文件位置：`src/main/resources/application.yml`

以下配置用于调整 Jackson 默认行为。

```yaml
spring:
  jackson:
    # 全局日期时间格式，主要影响 Date 类型；Java Time 类型建议结合配置类处理
    date-format: yyyy-MM-dd HH:mm:ss
    # 设置默认时区
    time-zone: Asia/Shanghai
    serialization:
      # 日期不输出为时间戳
      write-dates-as-timestamps: false
```

再例如，自定义线程池 Bean，替换默认异步任务执行器。

文件位置：`src/main/java/io/github/atengk/demo/config/AsyncConfig.java`

以下配置类提供自定义异步线程池，用于覆盖默认异步执行行为。

```java
package io.github.atengk.demo.config;

import cn.hutool.core.thread.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步任务线程池配置。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Configuration
public class AsyncConfig {

    @Bean("applicationTaskExecutor")
    public ThreadPoolTaskExecutor applicationTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(8);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(200);
        executor.setKeepAliveSeconds(60);
        executor.setThreadFactory(ThreadFactoryBuilder.create().setNamePrefix("app-async-").build());
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();

        log.info("初始化应用异步线程池，核心线程数：{}，最大线程数：{}", 8, 16);
        return executor;
    }

}
```

自动配置覆盖注意事项：

- 优先使用配置属性覆盖默认行为。
- 配置属性无法满足时，再使用自定义 Bean。
- 不建议直接引用自动配置类内部的非公开细节。
- 覆盖默认 Bean 后，需要验证原有自动配置是否仍然符合预期。
- 对外部中间件相关配置，例如 Redis、数据库、MQ，应优先使用官方 Starter 暴露的配置项。

### 自动配置排除方式

当某个自动配置不适合当前项目时，可以显式排除。常见场景包括：项目暂时引入了数据库相关依赖但不希望启动时初始化数据源，或者某个 Starter 自动创建的 Bean 与项目自定义框架冲突。Spring Boot 支持通过注解和配置属性排除自动配置类。([Home](https://docs.spring.io/spring-boot/reference/using/auto-configuration.html?utm_source=chatgpt.com))

方式一：在启动类上使用 `exclude`。

文件位置：`src/main/java/io/github/atengk/demo/DemoApplication.java`

以下配置用于排除数据源自动配置，适合暂时不连接数据库的 Web 项目。

```java
package io.github.atengk.demo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

/**
 * Spring Boot 3 应用启动类。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
        log.info("应用启动完成，已排除数据源自动配置");
    }

}
```

方式二：在配置文件中使用 `spring.autoconfigure.exclude`。

文件位置：`src/main/resources/application.yml`

以下配置通过配置文件排除数据源自动配置。

```yaml
spring:
  autoconfigure:
    # 排除数据源自动配置，多个自动配置类可以使用逗号分隔或 YAML 数组
    exclude:
      - org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
```

方式三：使用 `excludeName` 排除类名。

当项目中无法直接引用某个自动配置类时，可以使用类全名字符串排除。

```java
@SpringBootApplication(excludeName = "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration")
public class DemoApplication {
}
```

排除自动配置时需要注意：

- 能通过配置属性解决的问题，不优先使用排除。
- 排除自动配置会影响相关 Bean 的创建，需要确认没有其他组件依赖这些 Bean。
- 数据源、Redis、Security、WebMvc 等核心自动配置不应随意排除。
- 排除后建议使用 `--debug` 查看条件报告，确认排除结果符合预期。
- 多模块项目中，排除配置通常放在启动模块，而不是公共模块。

自动配置排除更适合作为明确的工程决策，而不是临时绕过报错。遇到启动失败时，应先查看异常原因、依赖树和配置文件，再判断是否真的需要排除自动配置。



## Bean 管理

Bean 管理是 Spring 框架的核心能力之一。Spring Boot 3 项目中的 Controller、Service、Repository、Config、工具组件、第三方客户端等对象，通常都交由 Spring 容器统一创建、装配和管理。合理使用 Bean 管理机制，可以减少对象创建代码，提高模块解耦能力，并让事务、异步、缓存、校验等 Spring 能力正常生效。

### Bean 注册方式

Bean 注册是指将对象交给 Spring 容器管理。Spring Boot 项目中常见的注册方式包括注解扫描、配置类 `@Bean` 方法、`@Import` 导入、条件装配和配置属性绑定等。业务代码中最常用的是注解扫描和 `@Bean` 方法。

常见 Bean 注册方式如下：

| 注册方式     | 常用注解                                                     | 适用场景                             |
| ------------ | ------------------------------------------------------------ | ------------------------------------ |
| 组件扫描     | `@Component`、`@Service`、`@Repository`、`@Controller`、`@RestController` | 注册项目内部业务类                   |
| 配置方法     | `@Configuration` + `@Bean`                                   | 注册第三方对象、SDK 客户端、工具组件 |
| 条件注册     | `@ConditionalOnProperty`、`@ConditionalOnMissingBean`        | 根据配置或已有 Bean 决定是否注册     |
| 导入注册     | `@Import`                                                    | 显式导入配置类或组件                 |
| 配置绑定注册 | `@ConfigurationProperties`                                   | 注册配置属性对象                     |

注解扫描适合注册业务类。只要类位于启动类所在包或其子包下，并标注了 Spring 组件注解，就会被 Spring 自动扫描并注册为 Bean。

文件位置：`src/main/java/io/github/atengk/demo/service/SystemClockService.java`

以下 Service 通过 `@Service` 注册为 Bean，用于返回当前系统时间。

```java
package io.github.atengk.demo.service;

import cn.hutool.core.date.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 系统时间业务组件。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Service
public class SystemClockService {

    /**
     * 获取当前时间字符串。
     *
     * @return 当前时间字符串
     */
    public String getCurrentTime() {
        String currentTime = DateUtil.now();
        log.info("获取当前系统时间：{}", currentTime);
        return currentTime;
    }

}
```

`@Bean` 适合注册第三方类、无法直接修改源码的类，或者需要在创建时传入参数的对象。

文件位置：`src/main/java/io/github/atengk/demo/config/AppBeanConfig.java`

以下配置类通过 `@Bean` 注册一个应用级组件。

```java
package io.github.atengk.demo.config;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 应用 Bean 配置。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Configuration
public class AppBeanConfig {

    /**
     * 注册雪花算法 ID 生成器。
     *
     * @return 雪花算法 ID 生成器
     */
    @Bean
    public Snowflake snowflake() {
        log.info("初始化雪花算法 ID 生成器");
        return IdUtil.getSnowflake(1, 1);
    }

}
```

使用该 Bean 时，可以直接通过构造器注入。

文件位置：`src/main/java/io/github/atengk/demo/service/OrderNoService.java`

以下 Service 注入 `Snowflake` Bean，用于生成订单编号。

```java
package io.github.atengk.demo.service;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 订单编号业务组件。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderNoService {

    private final Snowflake snowflake;

    /**
     * 生成订单编号。
     *
     * @return 订单编号
     */
    public String generateOrderNo() {
        String orderNo = StrUtil.format("ORD{}", snowflake.nextId());
        log.info("生成订单编号：{}", orderNo);
        return orderNo;
    }

}
```

Bean 注册建议：

- 项目内部业务类优先使用 `@Service`、`@Component` 等注解注册。
- 第三方 SDK、工具组件、连接客户端优先使用 `@Configuration` + `@Bean` 注册。
- 有多个同类型 Bean 时，应使用 `@Primary` 或 `@Qualifier` 明确注入目标。
- 不建议在业务代码中频繁使用 `new` 创建应由 Spring 管理的对象。
- 需要事务、异步、缓存等 AOP 能力的类，必须交给 Spring 容器管理。

### 依赖注入方式

依赖注入用于让一个 Bean 使用另一个 Bean。Spring 支持构造器注入、Setter 注入和字段注入。实际项目中推荐使用构造器注入，因为它可以明确依赖关系、支持不可变字段、便于单元测试，并且能更早发现缺失依赖问题。

常见注入方式对比如下：

| 注入方式    | 示例                                         | 推荐程度 | 说明                   |
| ----------- | -------------------------------------------- | -------- | ---------------------- |
| 构造器注入  | `private final UserService userService`      | 推荐     | 依赖明确，适合必需依赖 |
| Setter 注入 | `setUserService()`                           | 可选     | 适合可选依赖           |
| 字段注入    | `@Autowired private UserService userService` | 不推荐   | 不利于测试，依赖不清晰 |

推荐写法是 `final` 字段配合 Lombok 的 `@RequiredArgsConstructor`。

文件位置：`src/main/java/io/github/atengk/demo/controller/ClockController.java`

以下 Controller 使用构造器注入 Service，并提供一个测试接口。

```java
package io.github.atengk.demo.controller;

import io.github.atengk.demo.common.response.ApiResult;
import io.github.atengk.demo.service.SystemClockService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 系统时间接口。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@RestController
@RequiredArgsConstructor
public class ClockController {

    private final SystemClockService systemClockService;

    /**
     * 获取当前系统时间。
     *
     * @return 当前系统时间
     */
    @GetMapping("/api/system/current-time")
    public ApiResult<String> currentTime() {
        return ApiResult.success(systemClockService.getCurrentTime());
    }

}
```

当存在多个同类型 Bean 时，需要使用 `@Qualifier` 指定注入名称。

文件位置：`src/main/java/io/github/atengk/demo/config/MessageConfig.java`

以下配置类注册两个不同的消息发送器 Bean。

```java
package io.github.atengk.demo.config;

import io.github.atengk.demo.service.MessageSender;
import io.github.atengk.demo.service.impl.EmailMessageSender;
import io.github.atengk.demo.service.impl.SmsMessageSender;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 消息发送器配置。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Configuration
public class MessageConfig {

    /**
     * 注册短信消息发送器。
     *
     * @return 消息发送器
     */
    @Bean
    public MessageSender smsMessageSender() {
        return new SmsMessageSender();
    }

    /**
     * 注册邮件消息发送器。
     *
     * @return 消息发送器
     */
    @Bean
    public MessageSender emailMessageSender() {
        return new EmailMessageSender();
    }

}
```

文件位置：`src/main/java/io/github/atengk/demo/service/MessageSender.java`

以下接口定义统一的消息发送能力。

```java
package io.github.atengk.demo.service;

/**
 * 消息发送器。
 *
 * @author Ateng
 * @since 2026-05-09
 */
public interface MessageSender {

    /**
     * 发送消息。
     *
     * @param receiver 接收人
     * @param content 消息内容
     */
    void send(String receiver, String content);

}
```

文件位置：`src/main/java/io/github/atengk/demo/service/impl/SmsMessageSender.java`

以下实现类用于发送短信消息。

```java
package io.github.atengk.demo.service.impl;

import io.github.atengk.demo.service.MessageSender;
import lombok.extern.slf4j.Slf4j;

/**
 * 短信消息发送器。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
public class SmsMessageSender implements MessageSender {

    @Override
    public void send(String receiver, String content) {
        log.info("发送短信消息，接收人：{}，内容：{}", receiver, content);
    }

}
```

文件位置：`src/main/java/io/github/atengk/demo/service/impl/EmailMessageSender.java`

以下实现类用于发送邮件消息。

```java
package io.github.atengk.demo.service.impl;

import io.github.atengk.demo.service.MessageSender;
import lombok.extern.slf4j.Slf4j;

/**
 * 邮件消息发送器。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
public class EmailMessageSender implements MessageSender {

    @Override
    public void send(String receiver, String content) {
        log.info("发送邮件消息，接收人：{}，内容：{}", receiver, content);
    }

}
```

文件位置：`src/main/java/io/github/atengk/demo/service/NotifyService.java`

以下 Service 通过 `@Qualifier` 指定注入短信发送器。

```java
package io.github.atengk.demo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * 通知业务组件。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Service
public class NotifyService {

    private final MessageSender messageSender;

    /**
     * 创建通知业务组件。
     *
     * @param messageSender 消息发送器
     */
    public NotifyService(@Qualifier("smsMessageSender") MessageSender messageSender) {
        this.messageSender = messageSender;
    }

    /**
     * 发送通知。
     *
     * @param receiver 接收人
     * @param content 通知内容
     */
    public void notify(String receiver, String content) {
        messageSender.send(receiver, content);
    }

}
```

依赖注入建议：

- 必需依赖使用构造器注入。
- 可选依赖可以使用 `ObjectProvider<T>` 或 Setter 注入。
- 同类型多个 Bean 时使用 `@Qualifier` 或 `@Primary`。
- 避免字段注入，尤其是核心业务类中。
- 避免在构造方法中执行复杂业务逻辑，构造方法只负责接收依赖。

### Bean 生命周期

Bean 生命周期是指 Bean 从定义、实例化、依赖注入、初始化、使用到销毁的完整过程。理解生命周期有助于正确处理资源初始化、连接关闭、缓存预热、启动检查和优雅停机。

单例 Bean 的典型生命周期如下：

1. 读取 Bean 定义。
2. 实例化 Bean。
3. 填充属性和依赖注入。
4. 执行 `BeanNameAware`、`ApplicationContextAware` 等感知接口。
5. 执行 Bean 后置处理器前置逻辑。
6. 执行初始化方法，例如 `@PostConstruct`、`InitializingBean`、`initMethod`。
7. 执行 Bean 后置处理器后置逻辑。
8. Bean 进入可使用状态。
9. 容器关闭时执行销毁方法，例如 `@PreDestroy`、`DisposableBean`、`destroyMethod`。

实际开发中，最常用的是 `@PostConstruct` 和 `@PreDestroy`。它们适合处理轻量初始化和资源释放。

文件位置：`src/main/java/io/github/atengk/demo/service/LocalCacheService.java`

以下 Service 在 Bean 初始化时加载本地缓存，在应用关闭时清理缓存。

```java
package io.github.atengk.demo.service;

import cn.hutool.core.map.MapUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 本地缓存业务组件。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Service
public class LocalCacheService {

    private final Map<String, String> localCache = new ConcurrentHashMap<>();

    /**
     * 初始化本地缓存。
     */
    @PostConstruct
    public void init() {
        localCache.put("systemName", "spring-boot3-demo");
        localCache.put("version", "1.0.0");
        log.info("本地缓存初始化完成，缓存数量：{}", localCache.size());
    }

    /**
     * 根据缓存键获取缓存值。
     *
     * @param key 缓存键
     * @return 缓存值
     */
    public String get(String key) {
        return MapUtil.getStr(localCache, key);
    }

    /**
     * 清理本地缓存。
     */
    @PreDestroy
    public void destroy() {
        localCache.clear();
        log.info("本地缓存已清理");
    }

}
```

也可以在 `@Bean` 中通过 `initMethod` 和 `destroyMethod` 指定初始化与销毁方法。该方式适合第三方类或不方便修改源码的类。

文件位置：`src/main/java/io/github/atengk/demo/config/ResourceConfig.java`

以下配置类注册一个自定义资源对象，并指定初始化和销毁方法。

```java
package io.github.atengk.demo.config;

import io.github.atengk.demo.support.DemoResource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 资源对象配置。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Configuration
public class ResourceConfig {

    /**
     * 注册示例资源对象。
     *
     * @return 示例资源对象
     */
    @Bean(initMethod = "open", destroyMethod = "close")
    public DemoResource demoResource() {
        return new DemoResource("default-resource");
    }

}
```

文件位置：`src/main/java/io/github/atengk/demo/support/DemoResource.java`

以下类模拟一个需要初始化和释放的资源对象。

```java
package io.github.atengk.demo.support;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 示例资源对象。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@RequiredArgsConstructor
public class DemoResource {

    private final String resourceName;

    /**
     * 打开资源。
     */
    public void open() {
        log.info("打开资源：{}", resourceName);
    }

    /**
     * 关闭资源。
     */
    public void close() {
        log.info("关闭资源：{}", resourceName);
    }

}
```

生命周期使用建议：

- 缓存预热、字典加载等轻量逻辑可以放在 `@PostConstruct`。
- 复杂启动任务建议使用 `ApplicationRunner`，避免 Bean 初始化阶段逻辑过重。
- 文件句柄、线程池、网络连接等资源应在销毁阶段释放。
- 不建议在初始化方法中调用远程接口做长时间阻塞操作。
- 初始化失败会影响应用启动，应保留清晰日志。

### Bean 初始化与销毁

Bean 初始化和销毁是生命周期中的两个重要阶段。初始化阶段适合检查必要配置、构建内存数据、加载轻量资源；销毁阶段适合关闭连接、清理缓存、停止线程池、释放临时文件。

初始化方式常见有三种：

| 方式                                    | 适用场景                         |
| --------------------------------------- | -------------------------------- |
| `@PostConstruct`                        | 项目内部 Bean 的初始化逻辑       |
| `InitializingBean.afterPropertiesSet()` | 需要实现 Spring 接口的初始化逻辑 |
| `@Bean(initMethod = "...")`             | 第三方对象或外部资源初始化       |

销毁方式常见有三种：

| 方式                           | 适用场景                       |
| ------------------------------ | ------------------------------ |
| `@PreDestroy`                  | 项目内部 Bean 的资源释放       |
| `DisposableBean.destroy()`     | 需要实现 Spring 接口的销毁逻辑 |
| `@Bean(destroyMethod = "...")` | 第三方对象或外部资源销毁       |

文件位置：`src/main/java/io/github/atengk/demo/service/StartupCheckService.java`

以下 Service 使用 `InitializingBean` 和 `DisposableBean` 处理初始化检查与销毁日志。

```java
package io.github.atengk.demo.service;

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

/**
 * 应用启动检查组件。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StartupCheckService implements InitializingBean, DisposableBean {

    private final Environment environment;

    /**
     * Bean 属性注入完成后执行。
     */
    @Override
    public void afterPropertiesSet() {
        String applicationName = environment.getProperty("spring.application.name");
        if (StrUtil.isBlank(applicationName)) {
            log.warn("未配置 spring.application.name，建议补充应用名称");
            return;
        }
        log.info("启动配置检查完成，应用名称：{}", applicationName);
    }

    /**
     * Bean 销毁时执行。
     */
    @Override
    public void destroy() {
        log.info("应用启动检查组件已销毁");
    }

}
```

在实际项目中，更推荐 `@PostConstruct` 和 `@PreDestroy`，因为它们不会让业务类直接依赖 Spring 生命周期接口。但如果你需要明确接入 Spring 生命周期扩展点，也可以使用 `InitializingBean` 和 `DisposableBean`。

初始化与销毁注意事项：

- 初始化逻辑应短小、明确、可失败感知。
- 销毁逻辑应具备幂等性，重复执行也不应产生严重问题。
- 初始化阶段不要依赖尚未完成初始化的复杂业务链路。
- 多个初始化任务有顺序要求时，可以使用 `@DependsOn` 或事件机制。
- 生产项目中资源释放应配合优雅停机一起验证。

### Bean 循环依赖处理

Bean 循环依赖是指两个或多个 Bean 互相依赖，导致容器创建 Bean 时出现依赖闭环。最常见的是 Service 之间互相注入。

典型错误示例：

```text
UserService -> OrderService -> UserService
```

循环依赖会造成代码职责混乱，也容易导致应用启动失败。Spring 对部分单例 Bean 的 Setter 循环依赖有处理能力，但构造器循环依赖通常无法解决。Spring Boot 项目中不建议依赖容器自动处理循环依赖，而应从设计上拆分职责。

不推荐的写法如下。

```java
@Service
@RequiredArgsConstructor
public class UserService {

    private final OrderService orderService;

}

@Service
@RequiredArgsConstructor
public class OrderService {

    private final UserService userService;

}
```

推荐处理方式一：抽取第三个协调服务，打破双向依赖。

文件位置：`src/main/java/io/github/atengk/demo/service/UserOrderFacadeService.java`

以下 Facade Service 负责协调用户和订单逻辑，避免 UserService 与 OrderService 互相依赖。

```java
package io.github.atengk.demo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 用户订单门面业务组件。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserOrderFacadeService {

    private final UserBaseService userBaseService;
    private final OrderBaseService orderBaseService;

    /**
     * 处理用户订单统计。
     *
     * @param userId 用户 ID
     * @return 订单数量
     */
    public Integer countUserOrders(Long userId) {
        userBaseService.checkUserExists(userId);
        Integer orderCount = orderBaseService.countByUserId(userId);
        log.info("统计用户订单数量完成，用户ID：{}，订单数量：{}", userId, orderCount);
        return orderCount;
    }

}
```

推荐处理方式二：使用事件解耦。一个业务完成后发布事件，另一个业务监听事件，不直接互相调用。

文件位置：`src/main/java/io/github/atengk/demo/event/UserCreatedEvent.java`

以下事件对象表示用户创建完成事件。

```java
package io.github.atengk.demo.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 用户创建完成事件。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Getter
public class UserCreatedEvent extends ApplicationEvent {

    private final Long userId;

    /**
     * 创建用户事件。
     *
     * @param source 事件源
     * @param userId 用户 ID
     */
    public UserCreatedEvent(Object source, Long userId) {
        super(source);
        this.userId = userId;
    }

}
```

文件位置：`src/main/java/io/github/atengk/demo/listener/UserCreatedListener.java`

以下监听器在用户创建完成后处理后续业务。

```java
package io.github.atengk.demo.listener;

import io.github.atengk.demo.event.UserCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 用户创建事件监听器。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Component
public class UserCreatedListener {

    /**
     * 处理用户创建完成事件。
     *
     * @param event 用户创建完成事件
     */
    @EventListener
    public void handleUserCreated(UserCreatedEvent event) {
        log.info("监听到用户创建事件，用户ID：{}", event.getUserId());
    }

}
```

临时处理方式三：使用 `@Lazy` 延迟注入。该方式只能作为过渡方案，不应作为长期设计。

```java
package io.github.atengk.demo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

/**
 * 用户业务组件。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Service
public class LazyUserService {

    private final LazyOrderService lazyOrderService;

    /**
     * 创建用户业务组件。
     *
     * @param lazyOrderService 订单业务组件
     */
    public LazyUserService(@Lazy LazyOrderService lazyOrderService) {
        this.lazyOrderService = lazyOrderService;
    }

}
```

循环依赖处理建议：

- 优先通过职责拆分解决。
- 使用 Facade、Domain Service 或事件机制降低模块耦合。
- 构造器循环依赖应视为设计问题。
- `@Lazy` 只适合作为临时兼容方案。
- 不建议通过全局开启循环依赖来掩盖设计问题。

如确实需要临时开启循环依赖，可以配置：

```yaml
spring:
  main:
    # 不推荐长期使用，只建议迁移旧项目时临时开启
    allow-circular-references: true
```

## 配置文件管理

配置文件管理用于维护应用名称、端口、数据库连接、日志级别、第三方接口、线程池参数、开关配置等内容。Spring Boot 支持 `application.yml`、`application.properties`、Profile 配置、环境变量、命令行参数、外部配置文件等多种配置来源。

### application.yml 配置

`application.yml` 是 Spring Boot 项目中最常用的配置文件格式。YAML 使用缩进表达层级结构，可读性较好，适合维护复杂配置。

文件位置：`src/main/resources/application.yml`

以下配置定义了应用名称、服务端口、运行环境、日志级别和 Actuator 暴露端点。

```yaml
spring:
  application:
    # 应用名称，建议与服务名保持一致
    name: spring-boot3-demo

  profiles:
    # 默认激活开发环境
    active: dev

server:
  # 应用启动端口
  port: 8080

  servlet:
    # 应用上下文路径
    context-path: /

logging:
  level:
    # 项目包日志级别
    io.github.atengk: info
    # Spring Web 日志级别
    org.springframework.web: info

management:
  endpoints:
    web:
      exposure:
        # 开发环境可暴露 health、info、metrics，生产环境应收敛
        include: health,info,metrics
  endpoint:
    health:
      # 展示健康检查详情，生产环境建议按需控制
      show-details: when_authorized
```

YAML 配置注意事项：

- 使用空格缩进，不使用 Tab。
- 同级配置缩进必须一致。
- 字符串一般不需要加引号，但包含特殊字符时建议加引号。
- 密码、密钥、Token 不建议直接写入仓库中的 YAML。
- 布尔值、数字、数组要注意类型识别。

数组配置可以使用以下写法：

```yaml
demo:
  security:
    # 白名单路径
    whitelist:
      - /actuator/health
      - /api/login
      - /api/captcha
```

也可以使用行内数组写法：

```yaml
demo:
  security:
    # 白名单路径
    whitelist: [/actuator/health, /api/login, /api/captcha]
```

推荐使用多行数组写法，便于后续增加注释和维护。

### application.properties 配置

`application.properties` 是键值对格式的配置文件。它比 YAML 更简单，适合配置项较少、层级不复杂的项目，也适合某些中间件或脚本场景。

文件位置：`src/main/resources/application.properties`

以下配置与常见 `application.yml` 配置等价。

```properties
# 应用名称
spring.application.name=spring-boot3-demo

# 默认激活开发环境
spring.profiles.active=dev

# 应用端口
server.port=8080

# 应用上下文路径
server.servlet.context-path=/

# 项目日志级别
logging.level.io.github.atengk=info

# Spring Web 日志级别
logging.level.org.springframework.web=info

# Actuator 暴露端点
management.endpoints.web.exposure.include=health,info,metrics

# 健康检查详情展示方式
management.endpoint.health.show-details=when_authorized
```

`application.yml` 和 `application.properties` 不建议在同一个项目中混用同一批配置。虽然 Spring Boot 支持两种格式，但混用容易造成配置来源不清晰、覆盖关系难排查。

推荐原则：

- 新项目优先使用 `application.yml`。
- 配置简单的小工具项目可以使用 `application.properties`。
- 团队内部应统一一种主配置格式。
- 不要在两个文件中维护相同配置项。
- 如果两种文件同时存在，要明确配置覆盖关系并做好说明。

### 多环境配置

多环境配置用于区分开发、测试、预发、生产等不同运行环境。常见差异包括端口、数据库地址、Redis 地址、日志级别、文件路径、第三方接口地址等。

推荐配置结构如下：

```text
src/main/resources
├── application.yml
├── application-dev.yml
├── application-test.yml
└── application-prod.yml
```

主配置文件只放通用配置和默认激活环境。

文件位置：`src/main/resources/application.yml`

以下配置用于声明应用通用配置和默认环境。

```yaml
spring:
  application:
    # 应用名称
    name: spring-boot3-demo

  profiles:
    # 默认使用开发环境，生产启动时应通过命令行或环境变量覆盖
    active: dev

server:
  servlet:
    # 通用上下文路径
    context-path: /
```

开发环境配置用于本地调试。

文件位置：`src/main/resources/application-dev.yml`

以下配置用于本地开发环境。

```yaml
server:
  # 本地开发端口
  port: 8080

logging:
  level:
    # 开发环境可以输出更详细的项目日志
    io.github.atengk: debug

demo:
  file:
    # 本地文件上传目录
    upload-path: ./data/upload
```

测试环境配置用于测试服务器或集成测试环境。

文件位置：`src/main/resources/application-test.yml`

以下配置用于测试环境。

```yaml
server:
  # 测试环境端口
  port: 18080

logging:
  level:
    # 测试环境保持普通信息日志
    io.github.atengk: info

demo:
  file:
    # 测试服务器文件上传目录
    upload-path: /data/spring-boot3-demo/upload
```

生产环境配置用于正式运行环境。

文件位置：`src/main/resources/application-prod.yml`

以下配置用于生产环境。

```yaml
server:
  # 生产环境端口，通常由部署平台统一规划
  port: 8080

logging:
  level:
    # 生产环境不建议开启 debug
    io.github.atengk: info

management:
  endpoints:
    web:
      exposure:
        # 生产环境只暴露必要端点
        include: health,info

demo:
  file:
    # 生产文件上传目录
    upload-path: /data/spring-boot3-demo/upload
```

启动时可以通过参数指定环境：

```bash
# 使用开发环境
java -jar spring-boot3-demo.jar --spring.profiles.active=dev

# 使用测试环境
java -jar spring-boot3-demo.jar --spring.profiles.active=test

# 使用生产环境
java -jar spring-boot3-demo.jar --spring.profiles.active=prod
```

也可以通过环境变量指定：

```bash
# Linux 环境变量方式指定 Profile
export SPRING_PROFILES_ACTIVE=prod
java -jar spring-boot3-demo.jar
```

多环境配置建议：

- `application.yml` 只放通用配置。
- 环境差异放入 `application-{profile}.yml`。
- 生产敏感配置不提交到代码仓库。
- 生产启动时使用外部配置、环境变量或配置中心覆盖。
- 禁止依赖默认 `dev` 环境启动生产服务。

### 外部配置加载

外部配置加载用于在不修改 Jar 包的情况下调整配置。生产环境通常不建议把数据库密码、密钥、第三方 Token 等敏感配置打入 Jar，而应通过外部配置文件、环境变量、启动参数或配置中心注入。

常见外部配置方式如下：

| 方式         | 示例                                      | 适用场景                   |
| ------------ | ----------------------------------------- | -------------------------- |
| 命令行参数   | `--server.port=9000`                      | 临时覆盖少量配置           |
| 环境变量     | `SPRING_PROFILES_ACTIVE=prod`             | 容器、Linux 服务、CI/CD    |
| 外部配置文件 | `--spring.config.location=...`            | 生产配置独立维护           |
| 额外配置路径 | `--spring.config.additional-location=...` | 保留默认配置并追加外部配置 |
| 配置中心     | Nacos、Apollo、Spring Cloud Config        | 多服务统一配置管理         |

使用外部配置文件启动：

```bash
# 使用指定外部配置文件启动
java -jar spring-boot3-demo.jar \
  --spring.config.location=file:/data/config/application-prod.yml
```

使用额外配置路径启动：

```bash
# 加载 Jar 内默认配置，同时追加外部配置目录
java -jar spring-boot3-demo.jar \
  --spring.config.additional-location=file:/data/config/
```

`spring.config.location` 更偏向直接指定配置位置，使用不当可能导致默认配置不再按预期加载；`spring.config.additional-location` 更适合在保留 Jar 内配置的基础上追加外部配置。

生产部署中常见目录结构如下：

```text
/data/spring-boot3-demo
├── app
│   └── spring-boot3-demo.jar
├── config
│   └── application-prod.yml
├── logs
└── upload
```

启动命令示例：

```bash
cd /data/spring-boot3-demo/app

java -jar spring-boot3-demo.jar \
  --spring.profiles.active=prod \
  --spring.config.additional-location=file:/data/spring-boot3-demo/config/
```

外部配置建议：

- 敏感配置不写入 Git 仓库。
- 外部配置目录权限应限制为应用用户可读。
- 生产配置变更应纳入发布审批或配置审计。
- 配置文件路径应使用绝对路径。
- 配置变更后应重启或通过配置中心刷新，不能假设自动生效。

### 配置优先级

配置优先级决定同一个配置项在多个来源同时存在时，最终哪个值生效。Spring Boot 支持多种配置来源，常见优先级可以按“越靠近启动命令和运行环境，优先级越高”理解。

常见配置来源优先级由高到低可以简化理解为：

1. 命令行参数，例如 `--server.port=9000`。
2. Java 系统属性，例如 `-Dserver.port=9000`。
3. 操作系统环境变量，例如 `SERVER_PORT=9000`。
4. 外部配置文件，例如 `/data/config/application-prod.yml`。
5. Jar 包内部 Profile 配置，例如 `application-prod.yml`。
6. Jar 包内部默认配置，例如 `application.yml`。
7. `SpringApplication#setDefaultProperties` 设置的默认属性。

示例：如果同时存在以下配置：

文件位置：`src/main/resources/application.yml`

```yaml
server:
  # Jar 内默认端口
  port: 8080
```

文件位置：`/data/config/application-prod.yml`

```yaml
server:
  # 外部配置端口
  port: 18080
```

启动命令：

```bash
java -jar spring-boot3-demo.jar \
  --spring.profiles.active=prod \
  --spring.config.additional-location=file:/data/config/ \
  --server.port=9000
```

最终生效端口通常是命令行参数中的 `9000`。

可以通过以下接口验证配置实际值。

文件位置：`src/main/java/io/github/atengk/demo/controller/ConfigCheckController.java`

以下接口用于读取当前生效端口和应用名称，方便排查配置覆盖问题。

```java
package io.github.atengk.demo.controller;

import cn.hutool.core.map.MapUtil;
import io.github.atengk.demo.common.response.ApiResult;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 配置检查接口。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@RestController
@RequiredArgsConstructor
public class ConfigCheckController {

    private final Environment environment;

    /**
     * 查看当前生效配置。
     *
     * @return 当前配置值
     */
    @GetMapping("/api/config/check")
    public ApiResult<Map<String, Object>> check() {
        Map<String, Object> configMap = MapUtil.<String, Object>builder()
                .put("applicationName", environment.getProperty("spring.application.name"))
                .put("serverPort", environment.getProperty("server.port"))
                .put("activeProfile", String.join(",", environment.getActiveProfiles()))
                .build();

        return ApiResult.success(configMap);
    }

}
```

验证命令如下：

```bash
# 启动应用后查看当前生效配置
curl http://localhost:9000/api/config/check
```

配置优先级排查建议：

- 先确认当前激活的 Profile。
- 再确认配置是否在正确的文件中。
- 然后检查命令行参数和环境变量是否覆盖了文件配置。
- 最后通过日志或接口输出实际生效值。
- 不要只看配置文件内容判断最终结果，应以运行时 Environment 为准。

## 配置属性绑定

配置属性绑定用于将配置文件中的属性绑定到 Java 对象中。相比在代码中零散使用 `@Value`，使用 `@ConfigurationProperties` 可以更好地组织复杂配置，支持嵌套对象、集合、校验和默认值，更适合企业项目。

### @Value 属性注入

`@Value` 适合注入少量、简单、分散的配置项，例如应用名称、开关配置、单个超时时间等。不适合注入大量同前缀配置，也不适合复杂嵌套结构。

文件位置：`src/main/resources/application.yml`

以下配置定义应用展示名称和功能开关。

```yaml
demo:
  app:
    # 应用展示名称
    display-name: Spring Boot 3 示例项目
    # 是否启用欢迎语
    welcome-enabled: true
```

文件位置：`src/main/java/io/github/atengk/demo/service/AppInfoService.java`

以下 Service 使用 `@Value` 注入简单配置项。

```java
package io.github.atengk.demo.service;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 应用信息业务组件。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Service
public class AppInfoService {

    @Value("${demo.app.display-name:未命名应用}")
    private String displayName;

    @Value("${demo.app.welcome-enabled:false}")
    private Boolean welcomeEnabled;

    /**
     * 获取应用欢迎语。
     *
     * @return 应用欢迎语
     */
    public String getWelcomeText() {
        if (Boolean.FALSE.equals(welcomeEnabled)) {
            log.info("欢迎语功能未开启");
            return StrUtil.EMPTY;
        }

        String welcomeText = StrUtil.format("欢迎使用 {}", displayName);
        log.info("生成应用欢迎语：{}", welcomeText);
        return welcomeText;
    }

}
```

`@Value` 使用建议：

- 适合简单配置，不适合复杂配置。
- 建议提供默认值，例如 `${demo.app.name:default}`。
- 配置项较多时应改用 `@ConfigurationProperties`。
- 不建议在大量业务类中散落相同配置项。
- 不建议用 `@Value` 注入列表、Map、复杂嵌套对象。

### @ConfigurationProperties 绑定

`@ConfigurationProperties` 适合绑定一组同前缀配置。它可以把 YAML 或 Properties 中的层级配置映射为 Java 对象，使配置结构更清晰，也方便后续校验和复用。

推荐先在启动类上开启配置属性扫描。

文件位置：`src/main/java/io/github/atengk/demo/DemoApplication.java`

以下启动类通过 `@ConfigurationPropertiesScan` 开启配置属性类扫描。

```java
package io.github.atengk.demo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Spring Boot 3 应用启动类。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@SpringBootApplication
@ConfigurationPropertiesScan
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
        log.info("Spring Boot 3 应用启动完成");
    }

}
```

文件位置：`src/main/resources/application.yml`

以下配置定义文件上传相关参数。

```yaml
demo:
  file:
    # 文件上传根目录
    upload-path: /data/spring-boot3-demo/upload
    # 单个文件最大大小，单位 MB
    max-size-mb: 20
    # 是否按日期创建目录
    date-dir-enabled: true
    # 允许上传的文件扩展名
    allowed-extensions:
      - jpg
      - png
      - pdf
      - xlsx
```

文件位置：`src/main/java/io/github/atengk/demo/config/properties/FileProperties.java`

以下配置属性类用于绑定 `demo.file` 前缀下的配置。

```java
package io.github.atengk.demo.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 文件上传配置属性。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
@ConfigurationProperties(prefix = "demo.file")
public class FileProperties {

    /**
     * 文件上传根目录。
     */
    private String uploadPath = "./data/upload";

    /**
     * 单个文件最大大小，单位 MB。
     */
    private Integer maxSizeMb = 10;

    /**
     * 是否按日期创建目录。
     */
    private Boolean dateDirEnabled = true;

    /**
     * 允许上传的文件扩展名。
     */
    private List<String> allowedExtensions = new ArrayList<>();

}
```

文件位置：`src/main/java/io/github/atengk/demo/service/FileConfigService.java`

以下 Service 注入配置属性类，并使用 Hutool 处理文件路径和集合判断。

```java
package io.github.atengk.demo.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import io.github.atengk.demo.config.properties.FileProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 文件配置业务组件。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileConfigService {

    private final FileProperties fileProperties;

    /**
     * 检查文件上传目录。
     *
     * @return 文件上传目录是否存在
     */
    public Boolean checkUploadPath() {
        boolean exists = FileUtil.exist(fileProperties.getUploadPath());
        log.info("检查文件上传目录，路径：{}，是否存在：{}", fileProperties.getUploadPath(), exists);
        return exists;
    }

    /**
     * 判断文件扩展名是否允许。
     *
     * @param extension 文件扩展名
     * @return 是否允许
     */
    public Boolean isAllowedExtension(String extension) {
        boolean allowed = CollUtil.contains(fileProperties.getAllowedExtensions(), extension);
        log.info("检查文件扩展名，扩展名：{}，是否允许：{}", extension, allowed);
        return allowed;
    }

}
```

`@ConfigurationProperties` 使用建议：

- 同一类配置使用统一前缀，例如 `demo.file`、`demo.security`。
- 配置项较多时优先使用配置属性类，不要散落 `@Value`。
- 配置类建议放在 `config.properties` 包下。
- 配置类字段应提供合理默认值。
- 需要校验时配合 `@Validated` 和 Jakarta Validation 注解。

### 嵌套属性绑定

嵌套属性绑定适合表达复杂配置，例如第三方接口配置、线程池配置、安全白名单、文件存储配置等。嵌套对象可以让配置结构更清晰，避免大量平铺字段。

文件位置：`src/main/resources/application.yml`

以下配置定义第三方接口客户端参数。

```yaml
demo:
  client:
    # 是否启用第三方客户端
    enabled: true
    # 默认连接超时时间，单位毫秒
    connect-timeout: 3000
    # 默认读取超时时间，单位毫秒
    read-timeout: 5000
    auth:
      # 认证地址
      token-url: https://api.example.com/oauth/token
      # 客户端 ID
      client-id: demo-client
      # 客户端密钥，生产环境应使用环境变量或外部配置注入
      client-secret: change-me
    endpoints:
      # 用户查询接口
      user-url: https://api.example.com/users
      # 订单查询接口
      order-url: https://api.example.com/orders
```

文件位置：`src/main/java/io/github/atengk/demo/config/properties/ClientProperties.java`

以下配置属性类使用嵌套对象绑定第三方客户端配置。

```java
package io.github.atengk.demo.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 第三方客户端配置属性。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
@ConfigurationProperties(prefix = "demo.client")
public class ClientProperties {

    /**
     * 是否启用第三方客户端。
     */
    private Boolean enabled = false;

    /**
     * 连接超时时间，单位毫秒。
     */
    private Integer connectTimeout = 3000;

    /**
     * 读取超时时间，单位毫秒。
     */
    private Integer readTimeout = 5000;

    /**
     * 认证配置。
     */
    private Auth auth = new Auth();

    /**
     * 接口地址配置。
     */
    private Endpoints endpoints = new Endpoints();

    /**
     * 认证配置。
     *
     * @author Ateng
     * @since 2026-05-09
     */
    @Data
    public static class Auth {

        /**
         * Token 地址。
         */
        private String tokenUrl;

        /**
         * 客户端 ID。
         */
        private String clientId;

        /**
         * 客户端密钥。
         */
        private String clientSecret;

    }

    /**
     * 接口地址配置。
     *
     * @author Ateng
     * @since 2026-05-09
     */
    @Data
    public static class Endpoints {

        /**
         * 用户接口地址。
         */
        private String userUrl;

        /**
         * 订单接口地址。
         */
        private String orderUrl;

    }

}
```

文件位置：`src/main/java/io/github/atengk/demo/service/ClientConfigService.java`

以下 Service 读取嵌套配置，并输出当前客户端配置信息。

```java
package io.github.atengk.demo.service;

import cn.hutool.core.util.DesensitizedUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.demo.config.properties.ClientProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 第三方客户端配置业务组件。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClientConfigService {

    private final ClientProperties clientProperties;

    /**
     * 打印客户端配置摘要。
     */
    public void printClientSummary() {
        if (Boolean.FALSE.equals(clientProperties.getEnabled())) {
            log.info("第三方客户端未启用");
            return;
        }

        String clientId = clientProperties.getAuth().getClientId();
        String clientSecret = clientProperties.getAuth().getClientSecret();
        String safeSecret = StrUtil.isBlank(clientSecret)
                ? ""
                : DesensitizedUtil.password(clientSecret);

        log.info("第三方客户端已启用，clientId：{}，clientSecret：{}，用户接口：{}",
                clientId,
                safeSecret,
                clientProperties.getEndpoints().getUserUrl());
    }

}
```

嵌套配置建议：

- 按功能域拆分嵌套对象，例如 `auth`、`endpoints`、`pool`、`security`。
- 嵌套对象字段应初始化，避免空指针。
- 敏感字段日志输出时必须脱敏。
- 外部接口地址、超时时间、开关状态应配置化。
- 嵌套层级不要过深，一般不超过三层。

### 配置校验

配置校验用于在应用启动阶段发现错误配置，避免系统运行后才暴露问题。对于端口、路径、URL、线程池大小、密钥、开关组合等关键配置，建议使用 Jakarta Validation 进行校验。

文件位置：`src/main/resources/application.yml`

以下配置定义短信服务参数。

```yaml
demo:
  sms:
    # 是否启用短信服务
    enabled: true
    # 短信服务地址
    endpoint: https://sms.example.com/send
    # 访问密钥
    access-key: demo-access-key
    # 密钥，生产环境应通过外部配置注入
    secret-key: change-me
    # 请求超时时间，单位毫秒
    timeout: 5000
```

文件位置：`src/main/java/io/github/atengk/demo/config/properties/SmsProperties.java`

以下配置属性类使用校验注解约束短信配置。

```java
package io.github.atengk.demo.config.properties;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.hibernate.validator.constraints.URL;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 短信服务配置属性。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
@Validated
@ConfigurationProperties(prefix = "demo.sms")
public class SmsProperties {

    /**
     * 是否启用短信服务。
     */
    private Boolean enabled = false;

    /**
     * 短信服务地址。
     */
    @NotBlank(message = "短信服务地址不能为空")
    @URL(message = "短信服务地址格式不正确")
    private String endpoint;

    /**
     * 访问密钥。
     */
    @NotBlank(message = "短信 access-key 不能为空")
    private String accessKey;

    /**
     * 访问密钥 Secret。
     */
    @NotBlank(message = "短信 secret-key 不能为空")
    private String secretKey;

    /**
     * 请求超时时间，单位毫秒。
     */
    @Min(value = 1000, message = "短信请求超时时间不能小于 1000 毫秒")
    @Max(value = 60000, message = "短信请求超时时间不能大于 60000 毫秒")
    private Integer timeout = 5000;

}
```

如果配置不满足约束，应用会在启动阶段失败，并输出绑定或校验异常。这样可以避免错误配置进入运行阶段。

配置校验建议：

- 必填配置使用 `@NotBlank`、`@NotNull`。
- 数值范围使用 `@Min`、`@Max`。
- URL 地址使用 `@URL`。
- 嵌套配置需要配合 `@Valid`。
- 生产关键配置必须校验，例如数据库、对象存储、短信、支付、认证配置。

嵌套对象校验示例：

```java
package io.github.atengk.demo.config.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 线程池配置属性。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
@Validated
@ConfigurationProperties(prefix = "demo.thread")
public class ThreadPoolProperties {

    /**
     * 业务线程池配置。
     */
    @Valid
    private Pool business = new Pool();

    /**
     * 线程池参数。
     *
     * @author Ateng
     * @since 2026-05-09
     */
    @Data
    public static class Pool {

        /**
         * 线程名前缀。
         */
        @NotBlank(message = "线程名前缀不能为空")
        private String threadNamePrefix = "business-";

        /**
         * 核心线程数。
         */
        @Min(value = 1, message = "核心线程数不能小于 1")
        private Integer coreSize = 4;

        /**
         * 最大线程数。
         */
        @Min(value = 1, message = "最大线程数不能小于 1")
        private Integer maxSize = 8;

        /**
         * 队列容量。
         */
        @Min(value = 1, message = "队列容量不能小于 1")
        private Integer queueCapacity = 200;

    }

}
```

### 配置默认值处理

配置默认值用于保证配置缺失时应用仍有合理行为。默认值可以放在 `@Value` 表达式中，也可以直接放在 `@ConfigurationProperties` 字段初始化中。复杂项目更推荐后者，因为默认值集中在配置属性类中，更容易维护。

`@Value` 默认值写法如下：

```java
@Value("${demo.app.display-name:未命名应用}")
private String displayName;
```

`@ConfigurationProperties` 默认值写法如下：

```java
private Integer timeout = 5000;
private Boolean enabled = false;
```

推荐示例：

文件位置：`src/main/resources/application.yml`

以下配置只声明部分限流参数，未声明的字段由 Java 默认值补充。

```yaml
demo:
  rate-limit:
    # 是否开启限流
    enabled: true
    # 每分钟最大请求数
    max-requests-per-minute: 120
```

文件位置：`src/main/java/io/github/atengk/demo/config/properties/RateLimitProperties.java`

以下配置属性类为限流配置提供默认值。

```java
package io.github.atengk.demo.config.properties;

import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 接口限流配置属性。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
@Validated
@ConfigurationProperties(prefix = "demo.rate-limit")
public class RateLimitProperties {

    /**
     * 是否开启限流。
     */
    private Boolean enabled = false;

    /**
     * 每分钟最大请求数。
     */
    @Min(value = 1, message = "每分钟最大请求数不能小于 1")
    private Integer maxRequestsPerMinute = 60;

    /**
     * 限流窗口秒数。
     */
    @Min(value = 1, message = "限流窗口秒数不能小于 1")
    private Integer windowSeconds = 60;

    /**
     * 超限提示信息。
     */
    private String message = "请求过于频繁，请稍后再试";

}
```

文件位置：`src/main/java/io/github/atengk/demo/service/RateLimitConfigService.java`

以下 Service 读取限流配置，并输出当前限流规则摘要。

```java
package io.github.atengk.demo.service;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.demo.config.properties.RateLimitProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 限流配置业务组件。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitConfigService {

    private final RateLimitProperties rateLimitProperties;

    /**
     * 获取限流配置摘要。
     *
     * @return 限流配置摘要
     */
    public String getSummary() {
        if (Boolean.FALSE.equals(rateLimitProperties.getEnabled())) {
            log.info("接口限流未开启");
            return "接口限流未开启";
        }

        String summary = StrUtil.format("接口限流已开启，每 {} 秒最多允许 {} 次请求",
                rateLimitProperties.getWindowSeconds(),
                rateLimitProperties.getMaxRequestsPerMinute());

        log.info("限流配置摘要：{}", summary);
        return summary;
    }

}
```

默认值处理建议：

- 简单单项配置可以使用 `@Value` 默认值。
- 一组配置应使用 `@ConfigurationProperties` 字段默认值。
- 默认值必须是安全值，例如限流默认关闭或使用保守阈值。
- 密码、密钥、Token 不应设置真实默认值。
- 生产必填配置不建议依赖默认值，应通过校验强制提供。

配置属性绑定整体建议：

- 少量简单配置使用 `@Value`。
- 成组配置使用 `@ConfigurationProperties`。
- 复杂配置使用嵌套对象。
- 关键配置配合 `@Validated` 校验。
- 默认值写在配置属性类字段上，集中维护。
- 配置类命名建议使用 `XxxProperties`，统一放在 `config.properties` 包下。



## Profile 环境隔离

Profile 用于隔离不同运行环境的配置和 Bean 装配逻辑。Spring Boot 项目通常至少包含开发、测试、生产三个环境，不同环境在端口、数据库、Redis、日志级别、文件路径、第三方接口地址、监控端点暴露范围等方面会存在差异。

### 开发环境配置

开发环境用于本地编码、接口调试和功能验证。开发环境配置应以方便调试为主，可以开启更详细的日志，使用本地或开发服务器的中间件地址，但不应连接生产数据库或生产外部接口。

文件位置：`src/main/resources/application-dev.yml`

开发环境配置通常包含本地端口、日志级别、文件目录和调试开关。

```yaml
server:
  # 本地开发端口
  port: 8080

spring:
  datasource:
    # 开发数据库地址，禁止连接生产库
    url: jdbc:mysql://127.0.0.1:3306/spring_boot3_demo?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root
    password: root

logging:
  level:
    # 开发环境输出更详细的业务日志
    io.github.atengk: debug
    # SQL 或 Web 调试时可临时调整
    org.springframework.web: info

management:
  endpoints:
    web:
      exposure:
        # 开发环境可以暴露更多端点，便于排查问题
        include: health,info,metrics,env,beans

demo:
  file:
    # 本地文件上传目录
    upload-path: ./data/upload
  debug:
    # 开发调试开关
    enabled: true
```

开发环境建议：

- 可以开启 `debug` 级别日志，但不要长期输出过多框架日志。
- 文件上传目录可以使用项目相对路径，便于本地清理。
- 数据库、Redis、MQ 使用本地或开发环境资源。
- 可以暴露较多 Actuator 端点，但仅限本地或内网。
- 不要在开发配置中写入生产密钥、生产 Token 或生产数据库密码。

### 测试环境配置

测试环境用于联调、自动化测试、验收测试和预发布验证。测试环境应尽量接近生产环境，但允许保留必要的调试能力。测试环境的数据可以重置，但配置结构应与生产环境保持一致。

文件位置：`src/main/resources/application-test.yml`

测试环境配置通常使用独立端口、测试数据库和相对收敛的日志级别。

```yaml
server:
  # 测试环境端口
  port: 18080

spring:
  datasource:
    # 测试数据库地址
    url: jdbc:mysql://192.168.10.20:3306/spring_boot3_demo_test?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: test_user
    password: test_password

logging:
  level:
    # 测试环境建议保留业务 info 日志
    io.github.atengk: info
    org.springframework.web: info

management:
  endpoints:
    web:
      exposure:
        # 测试环境暴露必要排查端点
        include: health,info,metrics

demo:
  file:
    # 测试服务器文件上传目录
    upload-path: /data/spring-boot3-demo-test/upload
  debug:
    # 测试环境默认关闭调试开关
    enabled: false
```

测试环境建议：

- 测试环境与生产环境保持相同的配置结构。
- 测试数据库和生产数据库必须物理隔离。
- 测试环境可以保留部分监控端点，便于定位问题。
- 自动化测试数据应可重复初始化。
- 第三方接口建议使用沙箱地址或 Mock 服务。

### 生产环境配置

生产环境用于正式对外提供服务。生产环境配置应以安全、稳定、可观测、可回滚为核心目标。生产配置中不建议直接写明文密码，应优先通过环境变量、部署平台、密钥管理系统或配置中心注入。

文件位置：`src/main/resources/application-prod.yml`

生产环境配置应控制日志级别、端点暴露范围和敏感配置来源。

```yaml
server:
  # 生产服务端口，通常由部署平台统一规划
  port: 8080

spring:
  datasource:
    # 生产数据库地址建议通过外部配置或环境变量注入
    url: ${MYSQL_URL}
    username: ${MYSQL_USERNAME}
    password: ${MYSQL_PASSWORD}

logging:
  level:
    # 生产环境不建议开启 debug
    io.github.atengk: info
    org.springframework.web: warn

management:
  endpoints:
    web:
      exposure:
        # 生产环境只暴露必要端点
        include: health,info
  endpoint:
    health:
      # 生产环境健康详情按权限展示
      show-details: when_authorized

demo:
  file:
    # 生产文件上传目录
    upload-path: /data/spring-boot3-demo/upload
  debug:
    # 生产环境必须关闭调试开关
    enabled: false
```

生产环境建议：

- 密码、密钥、Token 使用环境变量或外部配置注入。
- 日志级别以 `info`、`warn` 为主，避免长期 `debug`。
- Actuator 端点只暴露必要接口。
- 文件路径使用绝对路径，避免依赖启动目录。
- 配置变更应纳入发布流程，不建议人工临时修改线上配置。
- 生产环境启动必须显式指定 `prod` Profile，不能依赖默认值。

### 环境切换方式

环境切换用于指定当前应用加载哪个 Profile 配置。Spring Boot 支持通过配置文件、命令行参数、环境变量、JVM 参数等方式切换环境。生产部署中推荐使用命令行参数或环境变量，而不是把生产环境写死在 `application.yml` 中。

方式一：在主配置中设置默认环境。

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  profiles:
    # 默认开发环境，只适合本地开发；生产启动时必须覆盖
    active: dev
```

方式二：使用命令行参数切换。

```bash
# 启动开发环境
java -jar spring-boot3-demo.jar --spring.profiles.active=dev

# 启动测试环境
java -jar spring-boot3-demo.jar --spring.profiles.active=test

# 启动生产环境
java -jar spring-boot3-demo.jar --spring.profiles.active=prod
```

方式三：使用环境变量切换。

```bash
# Linux 环境变量方式
export SPRING_PROFILES_ACTIVE=prod

java -jar spring-boot3-demo.jar
```

方式四：使用 JVM 参数切换。

```bash
java -Dspring.profiles.active=prod -jar spring-boot3-demo.jar
```

可以通过接口查看当前激活环境。

文件位置：`src/main/java/io/github/atengk/demo/controller/ProfileController.java`

该接口用于返回当前激活的 Profile，便于确认环境切换是否生效。

```java
package io.github.atengk.demo.controller;

import cn.hutool.core.util.ArrayUtil;
import io.github.atengk.demo.common.response.ApiResult;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 环境配置接口。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@RestController
@RequiredArgsConstructor
public class ProfileController {

    private final Environment environment;

    @GetMapping("/api/profile/active")
    public ApiResult<String> activeProfile() {
        String[] activeProfiles = environment.getActiveProfiles();
        if (ArrayUtil.isEmpty(activeProfiles)) {
            return ApiResult.success("default");
        }
        return ApiResult.success(String.join(",", activeProfiles));
    }

}
```

验证命令如下：

```bash
curl http://localhost:8080/api/profile/active
```

环境切换建议：

- 本地开发可以在 `application.yml` 中默认指定 `dev`。
- 测试和生产环境应由启动命令、环境变量或部署平台指定。
- 不建议在代码中硬编码环境判断。
- 环境名称应统一，例如 `dev`、`test`、`prod`。
- 启动日志中建议输出当前激活 Profile，便于排查部署问题。

### 环境差异管理

环境差异管理用于控制不同环境之间的配置变化范围。良好的环境差异管理应保证“结构一致、值不同”，即不同环境的配置项名称和层级尽量一致，只有具体值不同。

推荐配置结构：

```text
src/main/resources
├── application.yml
├── application-dev.yml
├── application-test.yml
└── application-prod.yml
```

通用配置放在 `application.yml`，环境差异放在对应 Profile 文件中。

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  application:
    # 所有环境共用应用名称
    name: spring-boot3-demo

server:
  servlet:
    # 所有环境共用上下文路径
    context-path: /

demo:
  api:
    # 所有环境共用接口版本
    version: v1
```

文件位置：`src/main/resources/application-dev.yml`

```yaml
demo:
  api:
    # 开发环境第三方接口地址
    base-url: https://dev-api.example.com
```

文件位置：`src/main/resources/application-test.yml`

```yaml
demo:
  api:
    # 测试环境第三方接口地址
    base-url: https://test-api.example.com
```

文件位置：`src/main/resources/application-prod.yml`

```yaml
demo:
  api:
    # 生产环境第三方接口地址
    base-url: https://api.example.com
```

环境差异管理建议：

- 通用配置集中放在 `application.yml`。
- 差异配置只放在对应 Profile 文件中。
- 各环境配置项结构保持一致，避免某个环境缺少关键配置。
- 生产敏感配置通过外部方式注入，不提交到代码仓库。
- 配置项应有明确命名，避免 `flag1`、`url2` 这类含义不清的配置。
- 重要配置启动时应校验，避免应用启动成功但运行时报错。

## Controller 接口开发

Controller 是 Web 请求入口，负责接收 HTTP 请求、解析参数、调用 Service、返回响应数据。Controller 层应保持轻量，不应直接编写复杂业务逻辑，也不应直接访问数据库。

### REST 接口设计

REST 接口设计应围绕资源建模，通过 HTTP 方法表达操作语义。路径使用名词，动作使用请求方法表达，避免在路径中使用 `get`、`add`、`delete` 这类动词。

推荐接口设计：

| 操作         | 请求方法 | 路径              | 说明               |
| ------------ | -------- | ----------------- | ------------------ |
| 查询用户列表 | `GET`    | `/api/users`      | 查询集合资源       |
| 查询用户详情 | `GET`    | `/api/users/{id}` | 查询单个资源       |
| 创建用户     | `POST`   | `/api/users`      | 创建资源           |
| 修改用户     | `PUT`    | `/api/users/{id}` | 全量或主要字段更新 |
| 删除用户     | `DELETE` | `/api/users/{id}` | 删除资源           |

不推荐设计：

```text
/api/getUser
/api/addUser
/api/deleteUser
/api/user/list
```

推荐设计：

```text
GET    /api/users
GET    /api/users/{id}
POST   /api/users
PUT    /api/users/{id}
DELETE /api/users/{id}
```

REST 接口设计建议：

- 路径使用名词，不使用动词。
- 集合资源使用复数，例如 `/api/users`。
- 使用 HTTP 方法表达操作类型。
- 路径层级不要过深，一般控制在三到四层以内。
- 查询条件放在 Query 参数中。
- 创建和修改数据使用 JSON 请求体。
- 返回结构保持统一，避免不同接口返回风格不一致。

### 请求路径设计

请求路径用于表达资源位置和接口边界。项目中建议统一加上 `/api` 前缀，必要时增加版本号，例如 `/api/v1`。后台管理接口、开放接口、内部接口也可以通过路径前缀区分。

常见路径设计：

```text
/api/users
/api/users/{id}
/api/orders
/api/orders/{id}
/api/files/upload
/api/files/{id}/download
/api/system/configs
```

如果需要区分接口版本，可以使用：

```text
/api/v1/users
/api/v2/users
```

如果需要区分业务入口，可以使用：

```text
/api/admin/users
/api/open/orders
/api/internal/tasks
```

文件位置：`src/main/java/io/github/atengk/demo/controller/UserController.java`

该 Controller 使用 `/api/users` 作为统一资源路径。

```java
package io.github.atengk.demo.controller;

import io.github.atengk.demo.common.response.ApiResult;
import io.github.atengk.demo.model.dto.UserCreateDTO;
import io.github.atengk.demo.model.dto.UserUpdateDTO;
import io.github.atengk.demo.model.vo.UserVO;
import io.github.atengk.demo.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户接口控制器。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    @GetMapping("/{id}")
    public ApiResult<UserVO> getById(@PathVariable Long id) {
        return ApiResult.success(userService.getById(id));
    }

    @PostMapping
    public ApiResult<UserVO> create(@Valid @RequestBody UserCreateDTO dto) {
        return ApiResult.success(userService.create(dto));
    }

    @PutMapping("/{id}")
    public ApiResult<UserVO> update(@PathVariable Long id, @Valid @RequestBody UserUpdateDTO dto) {
        return ApiResult.success(userService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ApiResult<Boolean> delete(@PathVariable Long id) {
        userService.delete(id);
        return ApiResult.success(true);
    }

}
```

请求路径设计建议：

- Controller 类上使用资源根路径。
- 方法上只写相对路径。
- 路径参数使用 `{id}`、`{orderId}` 等清晰命名。
- 不同业务域不要共用含糊路径。
- 文件上传、导入、导出等非标准资源操作可使用动作名，但应保持简洁。

### 请求方法选择

HTTP 请求方法应与操作语义一致。选择合适的请求方法可以提高接口可读性，也便于网关、浏览器、缓存、接口文档和前后端联调工具正确理解接口行为。

常见请求方法说明：

| 请求方法 | 适用场景               | 示例               |
| -------- | ---------------------- | ------------------ |
| `GET`    | 查询数据               | 查询列表、查询详情 |
| `POST`   | 创建数据或提交复杂操作 | 创建用户、提交订单 |
| `PUT`    | 更新资源               | 修改用户信息       |
| `PATCH`  | 局部更新资源           | 修改用户状态       |
| `DELETE` | 删除资源               | 删除用户           |

示例接口：

```text
GET    /api/users?pageNum=1&pageSize=10
GET    /api/users/1001
POST   /api/users
PUT    /api/users/1001
PATCH  /api/users/1001/status
DELETE /api/users/1001
```

文件位置：`src/main/java/io/github/atengk/demo/controller/UserStatusController.java`

该 Controller 使用 `PATCH` 处理用户状态局部更新。

```java
package io.github.atengk.demo.controller;

import io.github.atengk.demo.common.response.ApiResult;
import io.github.atengk.demo.model.dto.UserStatusUpdateDTO;
import io.github.atengk.demo.service.UserStatusService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户状态接口控制器。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserStatusController {

    private final UserStatusService userStatusService;

    @PatchMapping("/{id}/status")
    public ApiResult<Boolean> updateStatus(@PathVariable Long id, @Valid @RequestBody UserStatusUpdateDTO dto) {
        userStatusService.updateStatus(id, dto);
        return ApiResult.success(true);
    }

}
```

请求方法选择建议：

- 查询接口使用 `GET`。
- 创建接口使用 `POST`。
- 更新接口优先使用 `PUT`，局部更新可以使用 `PATCH`。
- 删除接口使用 `DELETE`。
- 不要使用 `GET` 执行新增、修改、删除操作。
- 复杂查询如果参数过多，可以使用 `POST /api/users/search`，但应在文档中说明原因。

### 请求参数接收

Controller 可以接收路径参数、查询参数、请求头、Cookie、JSON 请求体、表单参数和文件参数。不同参数来源应选择合适的注解。

常见参数注解：

| 注解              | 参数来源               | 示例                      |
| ----------------- | ---------------------- | ------------------------- |
| `@PathVariable`   | URL 路径               | `/api/users/{id}`         |
| `@RequestParam`   | Query 参数或表单字段   | `?pageNum=1`              |
| `@RequestHeader`  | 请求头                 | `X-Trace-Id`              |
| `@RequestBody`    | JSON 请求体            | `{ "username": "admin" }` |
| `@ModelAttribute` | 表单对象               | 表单提交                  |
| `@RequestPart`    | multipart 请求的一部分 | 文件和 JSON 混合上传      |

文件位置：`src/main/java/io/github/atengk/demo/controller/ParamDemoController.java`

该 Controller 展示路径参数、查询参数和请求头参数的接收方式。

```java
package io.github.atengk.demo.controller;

import cn.hutool.core.map.MapUtil;
import io.github.atengk.demo.common.response.ApiResult;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 请求参数示例接口。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@RestController
@RequestMapping("/api/param-demo")
public class ParamDemoController {

    @GetMapping("/users/{id}")
    public ApiResult<Map<String, Object>> getUser(
            @PathVariable Long id,
            @RequestParam(required = false) String keyword,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId) {

        Map<String, Object> data = MapUtil.<String, Object>builder()
                .put("id", id)
                .put("keyword", keyword)
                .put("traceId", traceId)
                .build();

        return ApiResult.success(data);
    }

}
```

调用示例：

```bash
curl -X GET "http://localhost:8080/api/param-demo/users/1001?keyword=ateng" \
  -H "X-Trace-Id: trace-001"
```

参数接收建议：

- 必填简单参数使用明确的注解声明。
- 查询条件较多时封装为 Query 对象。
- 新增、修改接口使用 `@RequestBody` 接收 JSON 对象。
- 请求头参数只放链路、认证、客户端信息等元数据。
- 不要把大量业务字段塞入 Header。

### 请求体接收

请求体通常用于接收 JSON 数据，常见于新增、修改、提交业务操作等场景。Spring Boot 默认使用 Jackson 进行 JSON 反序列化，将请求体转换为 Java 对象。

文件位置：`src/main/java/io/github/atengk/demo/model/dto/UserCreateDTO.java`

该 DTO 用于接收创建用户的 JSON 请求体。

```java
package io.github.atengk.demo.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户创建请求参数。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
public class UserCreateDTO {

    /**
     * 用户名。
     */
    @NotBlank(message = "用户名不能为空")
    @Size(max = 30, message = "用户名长度不能超过30个字符")
    private String username;

    /**
     * 昵称。
     */
    @Size(max = 50, message = "昵称长度不能超过50个字符")
    private String nickname;

    /**
     * 邮箱。
     */
    @Email(message = "邮箱格式不正确")
    private String email;

}
```

文件位置：`src/main/java/io/github/atengk/demo/controller/UserBodyController.java`

该 Controller 使用 `@RequestBody` 接收 JSON 请求体。

```java
package io.github.atengk.demo.controller;

import io.github.atengk.demo.common.response.ApiResult;
import io.github.atengk.demo.model.dto.UserCreateDTO;
import io.github.atengk.demo.model.vo.UserVO;
import io.github.atengk.demo.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户请求体示例接口。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/body/users")
public class UserBodyController {

    private final UserService userService;

    @PostMapping
    public ApiResult<UserVO> create(@Valid @RequestBody UserCreateDTO dto) {
        return ApiResult.success(userService.create(dto));
    }

}
```

请求示例：

```bash
curl -X POST "http://localhost:8080/api/body/users" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "ateng",
    "nickname": "Ateng",
    "email": "ateng@example.com"
  }'
```

请求体接收建议：

- JSON 请求必须指定 `Content-Type: application/json`。
- 请求 DTO 不要复用 Entity。
- DTO 字段应增加参数校验注解。
- Controller 中使用 `@Valid` 或 `@Validated` 触发校验。
- 大对象或复杂对象应拆分清楚，避免一个 DTO 承载过多业务语义。

### 响应数据返回

响应数据返回应保持统一结构，便于前端统一处理成功、失败、错误码、提示消息和响应数据。Controller 不建议直接返回裸对象，推荐封装统一响应对象。

文件位置：`src/main/java/io/github/atengk/demo/common/response/ApiResult.java`

该统一响应对象用于封装接口返回结果。

```java
package io.github.atengk.demo.common.response;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一接口响应对象。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
public class ApiResult<T> implements Serializable {

    /**
     * 响应码。
     */
    private Integer code;

    /**
     * 响应消息。
     */
    private String message;

    /**
     * 响应数据。
     */
    private T data;

    public static <T> ApiResult<T> success(T data) {
        ApiResult<T> result = new ApiResult<>();
        result.setCode(200);
        result.setMessage("操作成功");
        result.setData(data);
        return result;
    }

    public static <T> ApiResult<T> success() {
        ApiResult<T> result = new ApiResult<>();
        result.setCode(200);
        result.setMessage("操作成功");
        return result;
    }

    public static <T> ApiResult<T> fail(Integer code, String message) {
        ApiResult<T> result = new ApiResult<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }

}
```

成功响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 1001,
    "username": "ateng",
    "nickname": "Ateng"
  }
}
```

失败响应示例：

```json
{
  "code": 400,
  "message": "用户名不能为空",
  "data": null
}
```

响应数据返回建议：

- 所有接口返回统一结构。
- 成功和失败响应格式保持一致。
- 列表分页数据建议使用统一分页对象。
- 不直接返回 Entity，优先返回 VO。
- 异常响应交给全局异常处理统一封装。
- 删除、启用、禁用等操作可以返回 `Boolean` 或空成功响应。

## 请求参数处理

请求参数处理用于规范不同来源参数的接收、校验和转换方式。Spring Boot Controller 层应根据参数来源选择不同注解，并通过 DTO、Query 对象、校验注解和统一异常处理提高接口稳定性。

### 路径参数

路径参数是 URL 路径的一部分，通常用于标识具体资源，例如用户 ID、订单 ID、文件 ID。路径参数使用 `@PathVariable` 接收。

文件位置：`src/main/java/io/github/atengk/demo/controller/PathParamController.java`

该 Controller 展示单个路径参数和多个路径参数的接收方式。

```java
package io.github.atengk.demo.controller;

import cn.hutool.core.map.MapUtil;
import io.github.atengk.demo.common.response.ApiResult;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 路径参数示例接口。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@RestController
@RequestMapping("/api/path")
public class PathParamController {

    @GetMapping("/users/{userId}")
    public ApiResult<Map<String, Object>> getUser(@PathVariable Long userId) {
        return ApiResult.success(MapUtil.<String, Object>builder()
                .put("userId", userId)
                .build());
    }

    @GetMapping("/users/{userId}/orders/{orderId}")
    public ApiResult<Map<String, Object>> getUserOrder(@PathVariable Long userId, @PathVariable Long orderId) {
        return ApiResult.success(MapUtil.<String, Object>builder()
                .put("userId", userId)
                .put("orderId", orderId)
                .build());
    }

}
```

调用示例：

```bash
curl http://localhost:8080/api/path/users/1001

curl http://localhost:8080/api/path/users/1001/orders/9001
```

路径参数建议：

- 用于标识唯一资源。
- 参数名应表达业务含义，例如 `userId`、`orderId`。
- 不要把复杂查询条件放入路径。
- 路径层级不要过深。
- 路径参数通常应为必填参数。

### 查询参数

查询参数通常用于列表查询、分页查询、筛选条件和排序条件。简单查询参数可以使用 `@RequestParam`，复杂查询条件建议封装为 Query 对象。

文件位置：`src/main/java/io/github/atengk/demo/model/query/UserPageQuery.java`

该 Query 对象用于接收用户分页查询条件。

```java
package io.github.atengk.demo.model.query;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 用户分页查询参数。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
public class UserPageQuery {

    /**
     * 页码。
     */
    @Min(value = 1, message = "页码不能小于1")
    private Integer pageNum = 1;

    /**
     * 每页条数。
     */
    @Min(value = 1, message = "每页条数不能小于1")
    @Max(value = 100, message = "每页条数不能大于100")
    private Integer pageSize = 10;

    /**
     * 关键字。
     */
    private String keyword;

    /**
     * 用户状态。
     */
    private Integer status;

}
```

文件位置：`src/main/java/io/github/atengk/demo/controller/QueryParamController.java`

该 Controller 展示简单查询参数和对象查询参数的接收方式。

```java
package io.github.atengk.demo.controller;

import cn.hutool.core.map.MapUtil;
import io.github.atengk.demo.common.response.ApiResult;
import io.github.atengk.demo.model.query.UserPageQuery;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 查询参数示例接口。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Validated
@RestController
@RequestMapping("/api/query")
public class QueryParamController {

    @GetMapping("/simple")
    public ApiResult<Map<String, Object>> simpleQuery(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword) {

        return ApiResult.success(MapUtil.<String, Object>builder()
                .put("pageNum", pageNum)
                .put("pageSize", pageSize)
                .put("keyword", keyword)
                .build());
    }

    @GetMapping("/users")
    public ApiResult<UserPageQuery> queryUsers(@Valid UserPageQuery query) {
        return ApiResult.success(query);
    }

}
```

调用示例：

```bash
curl "http://localhost:8080/api/query/simple?pageNum=1&pageSize=10&keyword=ateng"

curl "http://localhost:8080/api/query/users?pageNum=1&pageSize=20&keyword=ateng&status=1"
```

查询参数建议：

- 少量参数可以使用 `@RequestParam`。
- 参数较多时封装为 Query 对象。
- 分页参数提供默认值。
- 分页大小应设置最大限制，避免一次查询过多数据。
- 查询接口尽量使用 `GET`，复杂查询可以使用 `POST` 请求体。

### 请求头参数

请求头参数用于传递请求元信息，例如链路追踪 ID、认证 Token、客户端类型、语言、版本号等。请求头不适合承载复杂业务数据。

常见请求头：

| 请求头            | 说明        |
| ----------------- | ----------- |
| `Authorization`   | 认证信息    |
| `X-Trace-Id`      | 请求链路 ID |
| `X-Client-Type`   | 客户端类型  |
| `X-App-Version`   | App 版本    |
| `Accept-Language` | 语言设置    |

文件位置：`src/main/java/io/github/atengk/demo/controller/HeaderParamController.java`

该 Controller 展示请求头参数接收方式。

```java
package io.github.atengk.demo.controller;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.demo.common.response.ApiResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 请求头参数示例接口。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@RestController
@RequestMapping("/api/header")
public class HeaderParamController {

    @GetMapping("/client")
    public ApiResult<Map<String, Object>> clientInfo(
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId,
            @RequestHeader(value = "X-Client-Type", required = false) String clientType,
            @RequestHeader(value = "X-App-Version", required = false) String appVersion) {

        String finalTraceId = StrUtil.blankToDefault(traceId, "unknown");
        log.info("接收客户端请求头，traceId：{}，clientType：{}，appVersion：{}", finalTraceId, clientType, appVersion);

        return ApiResult.success(MapUtil.<String, Object>builder()
                .put("traceId", finalTraceId)
                .put("clientType", clientType)
                .put("appVersion", appVersion)
                .build());
    }

}
```

调用示例：

```bash
curl http://localhost:8080/api/header/client \
  -H "X-Trace-Id: trace-001" \
  -H "X-Client-Type: web" \
  -H "X-App-Version: 1.0.0"
```

请求头参数建议：

- 认证信息放在 `Authorization`。
- 链路 ID 使用统一请求头名称，例如 `X-Trace-Id`。
- Header 中不要放大量业务字段。
- 敏感 Header 日志输出时应脱敏。
- 可选 Header 应提供默认值或空值处理。

### JSON 请求体

JSON 请求体是新增、修改、提交业务数据时最常用的参数形式。Controller 使用 `@RequestBody` 接收 JSON，配合 `@Valid` 触发参数校验。

文件位置：`src/main/java/io/github/atengk/demo/model/dto/OrderCreateDTO.java`

该 DTO 用于接收创建订单的 JSON 请求体。

```java
package io.github.atengk.demo.model.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 订单创建请求参数。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
public class OrderCreateDTO {

    /**
     * 用户 ID。
     */
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /**
     * 订单商品列表。
     */
    @Valid
    @NotEmpty(message = "订单商品不能为空")
    private List<OrderItemDTO> items;

    /**
     * 订单商品请求参数。
     *
     * @author Ateng
     * @since 2026-05-09
     */
    @Data
    public static class OrderItemDTO {

        /**
         * 商品 ID。
         */
        @NotNull(message = "商品ID不能为空")
        private Long productId;

        /**
         * 购买数量。
         */
        @NotNull(message = "购买数量不能为空")
        @Min(value = 1, message = "购买数量不能小于1")
        private Integer quantity;

    }

}
```

文件位置：`src/main/java/io/github/atengk/demo/controller/JsonBodyController.java`

该 Controller 接收嵌套 JSON 请求体。

```java
package io.github.atengk.demo.controller;

import io.github.atengk.demo.common.response.ApiResult;
import io.github.atengk.demo.model.dto.OrderCreateDTO;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * JSON 请求体示例接口。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@RestController
@RequestMapping("/api/json")
public class JsonBodyController {

    @PostMapping("/orders")
    public ApiResult<Boolean> createOrder(@Valid @RequestBody OrderCreateDTO dto) {
        log.info("接收创建订单请求，用户ID：{}，商品数量：{}", dto.getUserId(), dto.getItems().size());
        return ApiResult.success(true);
    }

}
```

请求示例：

```bash
curl -X POST "http://localhost:8080/api/json/orders" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1001,
    "items": [
      {
        "productId": 2001,
        "quantity": 2
      },
      {
        "productId": 2002,
        "quantity": 1
      }
    ]
  }'
```

JSON 请求体建议：

- 使用 DTO 接收，不直接使用 Map 或 Entity。
- 嵌套对象使用 `@Valid` 触发级联校验。
- 请求体字段命名保持清晰，不要使用缩写。
- 金额字段建议使用 `BigDecimal`。
- 日期时间字段建议使用 `LocalDateTime`，并统一 JSON 格式。
- 请求体过大时应设置请求体大小限制。

### 表单参数

表单参数通常来自 `application/x-www-form-urlencoded` 或 `multipart/form-data` 请求。普通表单字段可以使用 `@RequestParam` 或对象接收。文件上传场景一般使用 `multipart/form-data`。

文件位置：`src/main/java/io/github/atengk/demo/model/dto/LoginFormDTO.java`

该 DTO 用于接收登录表单参数。

```java
package io.github.atengk.demo.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录表单请求参数。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
public class LoginFormDTO {

    /**
     * 用户名。
     */
    @NotBlank(message = "用户名不能为空")
    private String username;

    /**
     * 密码。
     */
    @NotBlank(message = "密码不能为空")
    private String password;

}
```

文件位置：`src/main/java/io/github/atengk/demo/controller/FormParamController.java`

该 Controller 使用表单对象接收 `application/x-www-form-urlencoded` 参数。

```java
package io.github.atengk.demo.controller;

import cn.hutool.core.util.DesensitizedUtil;
import io.github.atengk.demo.common.response.ApiResult;
import io.github.atengk.demo.model.dto.LoginFormDTO;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 表单参数示例接口。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@RestController
@RequestMapping("/api/form")
public class FormParamController {

    @PostMapping("/login")
    public ApiResult<Boolean> login(@Valid LoginFormDTO dto) {
        log.info("接收表单登录请求，用户名：{}，密码：{}", dto.getUsername(), DesensitizedUtil.password(dto.getPassword()));
        return ApiResult.success(true);
    }

}
```

请求示例：

```bash
curl -X POST "http://localhost:8080/api/form/login" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=ateng&password=123456"
```

表单参数建议：

- 普通表单可以使用对象接收。
- 表单对象同样可以使用校验注解。
- 密码、Token 等敏感字段日志必须脱敏。
- 复杂业务数据优先使用 JSON 请求体。
- 文件和字段混合上传时使用 `multipart/form-data`。

### 文件参数

文件参数用于处理上传文件，Spring Boot 中通常使用 `MultipartFile` 接收。文件上传接口必须关注文件大小、文件类型、文件名安全、存储路径和访问权限。

文件位置：`src/main/resources/application.yml`

以下配置用于限制上传文件大小。

```yaml
spring:
  servlet:
    multipart:
      # 单个文件最大大小
      max-file-size: 20MB
      # 单次请求最大大小
      max-request-size: 50MB

demo:
  file:
    # 文件上传目录
    upload-path: ./data/upload
    # 允许上传的扩展名
    allowed-extensions:
      - jpg
      - png
      - pdf
      - xlsx
```

文件位置：`src/main/java/io/github/atengk/demo/config/properties/FileUploadProperties.java`

该配置属性类用于绑定文件上传配置。

```java
package io.github.atengk.demo.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 文件上传配置属性。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
@ConfigurationProperties(prefix = "demo.file")
public class FileUploadProperties {

    /**
     * 文件上传目录。
     */
    private String uploadPath = "./data/upload";

    /**
     * 允许上传的扩展名。
     */
    private List<String> allowedExtensions = new ArrayList<>();

}
```

文件位置：`src/main/java/io/github/atengk/demo/controller/FileUploadController.java`

该 Controller 使用 `MultipartFile` 接收上传文件，并使用 Hutool 处理文件名、扩展名和存储路径。

```java
package io.github.atengk.demo.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.demo.common.exception.BizException;
import io.github.atengk.demo.common.response.ApiResult;
import io.github.atengk.demo.config.properties.FileUploadProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

/**
 * 文件上传接口。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/files")
@EnableConfigurationProperties(FileUploadProperties.class)
public class FileUploadController {

    private final FileUploadProperties fileUploadProperties;

    @PostMapping("/upload")
    public ApiResult<String> upload(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException("上传文件不能为空");
        }

        String originalFilename = file.getOriginalFilename();
        String suffix = FileUtil.extName(originalFilename);
        if (StrUtil.isBlank(suffix)) {
            throw new BizException("文件扩展名不能为空");
        }

        if (CollUtil.isNotEmpty(fileUploadProperties.getAllowedExtensions())
                && !fileUploadProperties.getAllowedExtensions().contains(suffix.toLowerCase())) {
            throw new BizException("不支持的文件类型");
        }

        String newFilename = StrUtil.format("{}.{}", IdUtil.fastSimpleUUID(), suffix);
        File targetFile = FileUtil.file(fileUploadProperties.getUploadPath(), newFilename);
        FileUtil.mkParentDirs(targetFile);

        try {
            file.transferTo(targetFile);
            log.info("文件上传成功，原文件名：{}，保存路径：{}", originalFilename, targetFile.getAbsolutePath());
            return ApiResult.success(targetFile.getAbsolutePath());
        } catch (Exception e) {
            log.error("文件上传失败，原文件名：{}", originalFilename, e);
            throw new BizException("文件上传失败");
        }
    }

}
```

请求示例：

```bash
curl -X POST "http://localhost:8080/api/files/upload" \
  -F "file=@/Users/ateng/Desktop/test.pdf"
```

文件参数处理建议：

- 上传接口使用 `POST`。
- 文件字段使用 `MultipartFile` 接收。
- 必须限制单文件大小和请求总大小。
- 必须校验文件扩展名，必要时校验 MIME 类型和文件内容。
- 不要直接使用原始文件名保存，避免覆盖和路径风险。
- 文件保存路径应配置化。
- 生产环境建议接入对象存储，例如 MinIO、S3 或云厂商 OSS。
- 文件访问权限应单独控制，不要默认公开所有上传文件。



## 统一响应设计

统一响应设计用于规范接口返回结构，使前端、网关、日志系统和调用方可以用一致方式处理成功结果、失败结果、分页数据和空数据。Spring Boot 项目不建议不同接口各自返回不同格式，否则会增加前后端联调、异常处理和接口维护成本。

### 响应对象结构

统一响应对象通常包含响应码、响应消息、响应数据、请求追踪 ID、响应时间等字段。基础项目可以先保留 `code`、`message`、`data` 三个核心字段，后续再根据链路追踪、国际化、错误详情等需求扩展。

推荐响应结构如下：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {}
}
```

常见字段说明：

| 字段      | 类型      | 说明                                              |
| --------- | --------- | ------------------------------------------------- |
| `code`    | `Integer` | 业务响应码，例如 `200`、`400`、`500`              |
| `message` | `String`  | 响应提示信息                                      |
| `data`    | `T`       | 具体响应数据，可以是对象、数组、分页对象或 `null` |

文件位置：`src/main/java/io/github/atengk/demo/common/response/ApiResult.java`

下面的统一响应对象用于封装所有接口返回结果，支持成功响应、失败响应和无数据成功响应。

```java
package io.github.atengk.demo.common.response;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一接口响应对象。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
public class ApiResult<T> implements Serializable {

    private Integer code;

    private String message;

    private T data;

    public static <T> ApiResult<T> success() {
        ApiResult<T> result = new ApiResult<>();
        result.setCode(200);
        result.setMessage("操作成功");
        return result;
    }

    public static <T> ApiResult<T> success(T data) {
        ApiResult<T> result = new ApiResult<>();
        result.setCode(200);
        result.setMessage("操作成功");
        result.setData(data);
        return result;
    }

    public static <T> ApiResult<T> success(String message, T data) {
        ApiResult<T> result = new ApiResult<>();
        result.setCode(200);
        result.setMessage(message);
        result.setData(data);
        return result;
    }

    public static <T> ApiResult<T> fail(Integer code, String message) {
        ApiResult<T> result = new ApiResult<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }

    public static <T> ApiResult<T> fail(ResultCodeEnum resultCodeEnum) {
        ApiResult<T> result = new ApiResult<>();
        result.setCode(resultCodeEnum.getCode());
        result.setMessage(resultCodeEnum.getMessage());
        return result;
    }

}
```

文件位置：`src/main/java/io/github/atengk/demo/common/response/ResultCodeEnum.java`

下面的枚举用于集中维护常见响应码，避免业务代码中散落魔法数字。

```java
package io.github.atengk.demo.common.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 统一响应码枚举。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Getter
@RequiredArgsConstructor
public enum ResultCodeEnum {

    SUCCESS(200, "操作成功"),

    BAD_REQUEST(400, "请求参数错误"),

    UNAUTHORIZED(401, "未认证"),

    FORBIDDEN(403, "无访问权限"),

    NOT_FOUND(404, "资源不存在"),

    METHOD_NOT_ALLOWED(405, "请求方法不支持"),

    INTERNAL_ERROR(500, "系统异常"),

    BIZ_ERROR(1000, "业务处理失败");

    private final Integer code;

    private final String message;

}
```

统一响应对象建议：

- 所有 Controller 返回 `ApiResult<T>`。
- 成功响应和失败响应字段结构保持一致。
- 业务响应码统一维护，不在代码中随意写数字。
- `message` 面向调用方和前端展示，应简洁明确。
- `data` 只承载业务数据，不承载异常堆栈。
- 系统异常详情只写入日志，不直接返回给前端。

### 成功响应封装

成功响应用于表示接口处理成功。对于查询接口，`data` 通常返回业务对象、列表或分页对象；对于新增、修改、删除接口，可以返回新增后的对象、操作结果或空成功响应。

文件位置：`src/main/java/io/github/atengk/demo/controller/UserResultController.java`

下面的 Controller 展示详情查询、创建成功和删除成功三种响应形式。

```java
package io.github.atengk.demo.controller;

import io.github.atengk.demo.common.response.ApiResult;
import io.github.atengk.demo.model.dto.UserCreateDTO;
import io.github.atengk.demo.model.vo.UserVO;
import io.github.atengk.demo.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户统一响应示例接口。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/result/users")
public class UserResultController {

    private final UserService userService;

    @GetMapping("/{id}")
    public ApiResult<UserVO> getById(@PathVariable Long id) {
        return ApiResult.success(userService.getById(id));
    }

    @PostMapping
    public ApiResult<UserVO> create(@Valid @RequestBody UserCreateDTO dto) {
        UserVO userVO = userService.create(dto);
        return ApiResult.success("用户创建成功", userVO);
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ApiResult.success();
    }

}
```

成功响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 1001,
    "username": "ateng",
    "nickname": "Ateng"
  }
}
```

无数据成功响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": null
}
```

成功响应封装建议：

- 查询详情返回 VO 对象。
- 查询列表返回集合或分页对象。
- 创建接口可以返回创建后的资源对象。
- 修改、删除接口可以返回空成功响应。
- 不建议直接返回字符串作为最终接口结构。
- 不建议在 Controller 中手动拼接 JSON 字符串。

### 失败响应封装

失败响应用于表示请求参数错误、业务规则不满足、权限不足、资源不存在、系统异常等情况。失败响应应由全局异常处理统一生成，业务代码只需要抛出明确异常。

文件位置：`src/main/java/io/github/atengk/demo/common/exception/BizException.java`

下面的业务异常用于主动表达业务失败，例如用户不存在、库存不足、状态不允许修改等。

```java
package io.github.atengk.demo.common.exception;

import io.github.atengk.demo.common.response.ResultCodeEnum;
import lombok.Getter;

/**
 * 业务异常。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Getter
public class BizException extends RuntimeException {

    private final Integer code;

    public BizException(String message) {
        super(message);
        this.code = ResultCodeEnum.BIZ_ERROR.getCode();
    }

    public BizException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public BizException(ResultCodeEnum resultCodeEnum) {
        super(resultCodeEnum.getMessage());
        this.code = resultCodeEnum.getCode();
    }

}
```

业务代码中抛出异常：

```java
package io.github.atengk.demo.service;

import io.github.atengk.demo.common.exception.BizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 用户状态业务组件。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Service
public class UserStatusService {

    public void disable(Long userId) {
        if (userId == null) {
            throw new BizException(400, "用户ID不能为空");
        }

        log.info("禁用用户，用户ID：{}", userId);
    }

}
```

失败响应示例：

```json
{
  "code": 1000,
  "message": "用户不存在",
  "data": null
}
```

失败响应封装建议：

- 业务失败抛出 `BizException`。
- 参数错误由参数校验统一处理。
- 系统异常统一返回通用提示。
- 日志中保留异常堆栈，响应中不返回堆栈。
- 响应码和响应消息统一维护。
- 不要在每个 Controller 方法中手动 `try-catch` 组装失败响应。

### 分页响应封装

分页响应用于统一列表查询接口的数据结构。常见分页字段包括当前页、每页数量、总条数、总页数和当前页数据列表。分页对象应独立封装，避免不同接口返回不同分页格式。

文件位置：`src/main/java/io/github/atengk/demo/common/response/PageResult.java`

下面的分页响应对象用于封装分页查询结果。

```java
package io.github.atengk.demo.common.response;

import cn.hutool.core.collection.CollUtil;
import lombok.Data;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 分页响应对象。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
public class PageResult<T> implements Serializable {

    private Long pageNum;

    private Long pageSize;

    private Long total;

    private Long pages;

    private List<T> records;

    public static <T> PageResult<T> of(Long pageNum, Long pageSize, Long total, List<T> records) {
        PageResult<T> result = new PageResult<>();
        result.setPageNum(pageNum);
        result.setPageSize(pageSize);
        result.setTotal(total);
        result.setPages(calculatePages(total, pageSize));
        result.setRecords(CollUtil.isEmpty(records) ? Collections.emptyList() : records);
        return result;
    }

    public static <T> PageResult<T> empty(Long pageNum, Long pageSize) {
        return of(pageNum, pageSize, 0L, Collections.emptyList());
    }

    private static Long calculatePages(Long total, Long pageSize) {
        if (total == null || total <= 0 || pageSize == null || pageSize <= 0) {
            return 0L;
        }
        return (total + pageSize - 1) / pageSize;
    }

}
```

文件位置：`src/main/java/io/github/atengk/demo/model/query/UserPageQuery.java`

下面的查询对象用于接收分页查询参数。

```java
package io.github.atengk.demo.model.query;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 用户分页查询参数。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
public class UserPageQuery {

    @Min(value = 1, message = "页码不能小于1")
    private Long pageNum = 1L;

    @Min(value = 1, message = "每页条数不能小于1")
    @Max(value = 100, message = "每页条数不能大于100")
    private Long pageSize = 10L;

    private String keyword;

}
```

文件位置：`src/main/java/io/github/atengk/demo/controller/UserPageController.java`

下面的 Controller 返回统一分页响应。

```java
package io.github.atengk.demo.controller;

import io.github.atengk.demo.common.response.ApiResult;
import io.github.atengk.demo.common.response.PageResult;
import io.github.atengk.demo.model.query.UserPageQuery;
import io.github.atengk.demo.model.vo.UserVO;
import io.github.atengk.demo.service.UserQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

 /**
 * 用户分页查询接口。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/page/users")
public class UserPageController {

    private final UserQueryService userQueryService;

    @GetMapping
    public ApiResult<PageResult<UserVO>> page(@Valid UserPageQuery query) {
        return ApiResult.success(userQueryService.page(query));
    }

}
```

分页响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "pageNum": 1,
    "pageSize": 10,
    "total": 25,
    "pages": 3,
    "records": [
      {
        "id": 1001,
        "username": "ateng"
      }
    ]
  }
}
```

分页响应封装建议：

- 分页字段名称全项目统一。
- 当前页无数据时 `records` 返回空数组，不返回 `null`。
- `pageSize` 必须设置最大限制。
- 总页数由后端计算，不交给前端推断。
- MyBatis-Plus、JPA、手写 SQL 的分页结果最终都转换为统一 `PageResult<T>`。

### 空数据响应处理

空数据响应用于处理查询结果为空、列表无数据、资源不存在等情况。不同场景应区分处理，不能简单地全部返回 `null` 或全部抛异常。

常见处理建议：

| 场景           | 推荐处理                             |
| -------------- | ------------------------------------ |
| 查询列表无数据 | 返回空数组                           |
| 分页查询无数据 | 返回 `records: []`，`total: 0`       |
| 查询详情不存在 | 抛出业务异常或返回 404 业务码        |
| 删除不存在资源 | 根据业务决定幂等成功或提示资源不存在 |
| 可选配置为空   | 返回默认值或空对象                   |

文件位置：`src/main/java/io/github/atengk/demo/service/UserQueryService.java`

下面的 Service 对列表空数据和详情不存在做不同处理。

```java
package io.github.atengk.demo.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.demo.common.exception.BizException;
import io.github.atengk.demo.common.response.PageResult;
import io.github.atengk.demo.model.query.UserPageQuery;
import io.github.atengk.demo.model.vo.UserVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * 用户查询业务组件。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Service
public class UserQueryService {

    public UserVO getById(Long id) {
        UserVO userVO = mockGetById(id);
        if (ObjectUtil.isNull(userVO)) {
            log.warn("用户详情不存在，用户ID：{}", id);
            throw new BizException(404, "用户不存在");
        }
        return userVO;
    }

    public List<UserVO> list(String keyword) {
        List<UserVO> list = mockList(keyword);
        if (CollUtil.isEmpty(list)) {
            return Collections.emptyList();
        }
        return list;
    }

    public PageResult<UserVO> page(UserPageQuery query) {
        List<UserVO> list = mockList(query.getKeyword());
        if (CollUtil.isEmpty(list)) {
            return PageResult.empty(query.getPageNum(), query.getPageSize());
        }
        return PageResult.of(query.getPageNum(), query.getPageSize(), (long) list.size(), list);
    }

    private UserVO mockGetById(Long id) {
        return null;
    }

    private List<UserVO> mockList(String keyword) {
        return Collections.emptyList();
    }

}
```

空列表响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": []
}
```

空分页响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "pageNum": 1,
    "pageSize": 10,
    "total": 0,
    "pages": 0,
    "records": []
  }
}
```

空数据处理建议：

- 列表为空返回 `[]`，不返回 `null`。
- 分页为空返回完整分页结构。
- 详情不存在应明确提示，避免返回空对象误导调用方。
- 删除接口是否幂等，需要在接口设计阶段确定。
- 统一响应层不要隐藏业务错误。

## 参数校验

参数校验用于在业务执行前拦截非法输入，减少无效请求进入 Service 层。Spring Boot 3 使用 Jakarta Validation 体系，常见注解包括 `@NotBlank`、`@NotNull`、`@Size`、`@Min`、`@Max`、`@Email`、`@Pattern` 等。

### 基础参数校验

基础参数校验适合校验路径参数、查询参数、请求头等简单参数。Controller 类上需要添加 `@Validated`，方法参数上直接添加校验注解。

文件位置：`src/main/java/io/github/atengk/demo/controller/BasicValidateController.java`

下面的 Controller 校验路径参数和查询参数。

```java
package io.github.atengk.demo.controller;

import io.github.atengk.demo.common.response.ApiResult;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 基础参数校验接口。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/validate/basic")
public class BasicValidateController {

    @GetMapping("/users/{id}")
    public ApiResult<String> getById(@PathVariable @Min(value = 1, message = "用户ID必须大于0") Long id) {
        log.info("基础参数校验通过，用户ID：{}", id);
        return ApiResult.success("校验通过");
    }

    @GetMapping("/search")
    public ApiResult<String> search(
            @RequestParam @NotBlank(message = "关键字不能为空") String keyword,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码不能小于1") Integer pageNum,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "每页条数不能小于1") @Max(value = 100, message = "每页条数不能大于100") Integer pageSize) {

        log.info("查询参数校验通过，keyword：{}，pageNum：{}，pageSize：{}", keyword, pageNum, pageSize);
        return ApiResult.success("校验通过");
    }

}
```

请求示例：

```bash
curl "http://localhost:8080/api/validate/basic/search?keyword=ateng&pageNum=1&pageSize=10"
```

基础参数校验建议：

- Controller 类上添加 `@Validated`。
- 路径 ID 使用 `@Min(1)` 或 `@Positive`。
- 查询参数分页大小设置上限。
- 必填字符串使用 `@NotBlank`。
- 必填对象使用 `@NotNull`。
- 校验失败交给全局异常处理统一返回。

### 对象参数校验

对象参数校验适合 JSON 请求体和复杂查询对象。请求体参数使用 `@RequestBody` 接收，并在参数前添加 `@Valid` 或 `@Validated` 触发校验。

文件位置：`src/main/java/io/github/atengk/demo/model/dto/UserCreateValidateDTO.java`

下面的 DTO 用于校验用户创建请求。

```java
package io.github.atengk.demo.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户创建校验请求参数。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
public class UserCreateValidateDTO {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 4, max = 30, message = "用户名长度必须在4到30个字符之间")
    private String username;

    @NotBlank(message = "昵称不能为空")
    @Size(max = 50, message = "昵称长度不能超过50个字符")
    private String nickname;

    @Email(message = "邮箱格式不正确")
    private String email;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String mobile;

}
```

文件位置：`src/main/java/io/github/atengk/demo/controller/ObjectValidateController.java`

下面的 Controller 使用 `@Valid` 触发请求体校验。

```java
package io.github.atengk.demo.controller;

import io.github.atengk.demo.common.response.ApiResult;
import io.github.atengk.demo.model.dto.UserCreateValidateDTO;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 对象参数校验接口。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@RestController
@RequestMapping("/api/validate/object")
public class ObjectValidateController {

    @PostMapping("/users")
    public ApiResult<Boolean> create(@Valid @RequestBody UserCreateValidateDTO dto) {
        log.info("用户创建参数校验通过，用户名：{}", dto.getUsername());
        return ApiResult.success(true);
    }

}
```

请求示例：

```bash
curl -X POST "http://localhost:8080/api/validate/object/users" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "ateng",
    "nickname": "Ateng",
    "email": "ateng@example.com",
    "mobile": "13800138000"
  }'
```

对象参数校验建议：

- 新增、修改接口分别定义 DTO。
- DTO 不直接复用 Entity。
- 字符串必填使用 `@NotBlank`。
- 集合必填使用 `@NotEmpty`。
- 对象必填使用 `@NotNull`。
- Controller 参数前必须添加 `@Valid` 或 `@Validated`。
- 校验提示应面向前端和用户可理解。

### 分组校验

分组校验用于在同一个 DTO 中根据不同业务场景启用不同校验规则。例如创建用户时不需要传 ID，修改用户时必须传 ID。

文件位置：`src/main/java/io/github/atengk/demo/common/validation/ValidationGroups.java`

下面的接口定义参数校验分组。

```java
package io.github.atengk.demo.common.validation;

/**
 * 参数校验分组。
 *
 * @author Ateng
 * @since 2026-05-09
 */
public interface ValidationGroups {

    interface Create {
    }

    interface Update {
    }

    interface Delete {
    }

}
```

文件位置：`src/main/java/io/github/atengk/demo/model/dto/UserSaveDTO.java`

下面的 DTO 根据创建和修改场景定义不同校验规则。

```java
package io.github.atengk.demo.model.dto;

import io.github.atengk.demo.common.validation.ValidationGroups;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户保存请求参数。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
public class UserSaveDTO {

    @NotNull(message = "用户ID不能为空", groups = ValidationGroups.Update.class)
    private Long id;

    @NotBlank(message = "用户名不能为空", groups = {ValidationGroups.Create.class, ValidationGroups.Update.class})
    @Size(max = 30, message = "用户名长度不能超过30个字符", groups = {ValidationGroups.Create.class, ValidationGroups.Update.class})
    private String username;

    @NotBlank(message = "昵称不能为空", groups = {ValidationGroups.Create.class, ValidationGroups.Update.class})
    @Size(max = 50, message = "昵称长度不能超过50个字符", groups = {ValidationGroups.Create.class, ValidationGroups.Update.class})
    private String nickname;

    @Email(message = "邮箱格式不正确", groups = {ValidationGroups.Create.class, ValidationGroups.Update.class})
    private String email;

}
```

文件位置：`src/main/java/io/github/atengk/demo/controller/GroupValidateController.java`

下面的 Controller 在创建和修改接口中启用不同校验分组。

```java
package io.github.atengk.demo.controller;

import io.github.atengk.demo.common.response.ApiResult;
import io.github.atengk.demo.common.validation.ValidationGroups;
import io.github.atengk.demo.model.dto.UserSaveDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 分组校验接口。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@RestController
@RequestMapping("/api/validate/group/users")
public class GroupValidateController {

    @PostMapping
    public ApiResult<Boolean> create(@Validated(ValidationGroups.Create.class) @RequestBody UserSaveDTO dto) {
        log.info("创建用户分组校验通过，用户名：{}", dto.getUsername());
        return ApiResult.success(true);
    }

    @PutMapping("/{id}")
    public ApiResult<Boolean> update(@PathVariable Long id,
                                     @Validated(ValidationGroups.Update.class) @RequestBody UserSaveDTO dto) {
        dto.setId(id);
        log.info("修改用户分组校验通过，用户ID：{}，用户名：{}", dto.getId(), dto.getUsername());
        return ApiResult.success(true);
    }

}
```

分组校验建议：

- 创建、修改、删除等场景规则不同时使用分组。
- 分组接口统一放在 `common.validation` 包下。
- 简单 DTO 不需要为了形式强行使用分组。
- 分组过多时建议拆分 DTO，而不是让一个 DTO 过度复杂。
- 修改接口中的路径 ID 和请求体 ID 应统一处理，避免冲突。

### 嵌套对象校验

嵌套对象校验用于处理对象内部包含对象或集合的情况，例如订单包含多个商品、角色包含多个权限、表单包含多个子项。嵌套字段必须添加 `@Valid`，否则内部对象的校验不会生效。

文件位置：`src/main/java/io/github/atengk/demo/model/dto/OrderSubmitDTO.java`

下面的 DTO 展示订单提交时的嵌套对象校验。

```java
package io.github.atengk.demo.model.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 订单提交请求参数。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
public class OrderSubmitDTO {

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @Valid
    @NotEmpty(message = "订单商品不能为空")
    private List<OrderItemDTO> items;

    /**
     * 订单商品请求参数。
     *
     * @author Ateng
     * @since 2026-05-09
     */
    @Data
    public static class OrderItemDTO {

        @NotNull(message = "商品ID不能为空")
        private Long productId;

        @NotNull(message = "购买数量不能为空")
        @Min(value = 1, message = "购买数量不能小于1")
        private Integer quantity;

        @NotNull(message = "商品单价不能为空")
        private BigDecimal price;

    }

}
```

文件位置：`src/main/java/io/github/atengk/demo/controller/NestedValidateController.java`

下面的 Controller 使用 `@Valid` 触发订单对象及其商品列表校验。

```java
package io.github.atengk.demo.controller;

import io.github.atengk.demo.common.response.ApiResult;
import io.github.atengk.demo.model.dto.OrderSubmitDTO;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 嵌套对象校验接口。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@RestController
@RequestMapping("/api/validate/nested")
public class NestedValidateController {

    @PostMapping("/orders")
    public ApiResult<Boolean> submit(@Valid @RequestBody OrderSubmitDTO dto) {
        log.info("订单提交参数校验通过，用户ID：{}，商品数量：{}", dto.getUserId(), dto.getItems().size());
        return ApiResult.success(true);
    }

}
```

请求示例：

```bash
curl -X POST "http://localhost:8080/api/validate/nested/orders" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1001,
    "items": [
      {
        "productId": 2001,
        "quantity": 2,
        "price": 99.00
      }
    ]
  }'
```

嵌套对象校验建议：

- 嵌套对象字段必须添加 `@Valid`。
- 集合字段使用 `@NotEmpty` 限制不能为空集合。
- 子对象字段继续使用常规校验注解。
- 金额字段使用 `BigDecimal`，不要使用 `Double`。
- 嵌套层级过深时应重新审视请求结构。

### 自定义校验注解

自定义校验注解用于处理内置注解无法表达的规则，例如手机号、身份证号、业务编码、枚举值、文件扩展名等。自定义校验由注解和校验器两部分组成。

文件位置：`src/main/java/io/github/atengk/demo/common/validation/Mobile.java`

下面的注解用于校验手机号格式。

```java
package io.github.atengk.demo.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * 手机号校验注解。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Documented
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = MobileValidator.class)
public @interface Mobile {

    String message() default "手机号格式不正确";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
```

文件位置：`src/main/java/io/github/atengk/demo/common/validation/MobileValidator.java`

下面的校验器使用 Hutool 校验手机号格式，空值交给 `@NotBlank` 控制。

```java
package io.github.atengk.demo.common.validation;

import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.StrUtil;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * 手机号校验器。
 *
 * @author Ateng
 * @since 2026-05-09
 */
public class MobileValidator implements ConstraintValidator<Mobile, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (StrUtil.isBlank(value)) {
            return true;
        }
        return Validator.isMobile(value);
    }

}
```

文件位置：`src/main/java/io/github/atengk/demo/model/dto/UserMobileDTO.java`

下面的 DTO 使用自定义手机号校验注解。

```java
package io.github.atengk.demo.model.dto;

import io.github.atengk.demo.common.validation.Mobile;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 用户手机号请求参数。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
public class UserMobileDTO {

    @NotBlank(message = "手机号不能为空")
    @Mobile
    private String mobile;

}
```

文件位置：`src/main/java/io/github/atengk/demo/controller/CustomValidateController.java`

下面的 Controller 触发自定义注解校验。

```java
package io.github.atengk.demo.controller;

import io.github.atengk.demo.common.response.ApiResult;
import io.github.atengk.demo.model.dto.UserMobileDTO;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 自定义校验接口。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@RestController
@RequestMapping("/api/validate/custom")
public class CustomValidateController {

    @PostMapping("/mobile")
    public ApiResult<Boolean> validateMobile(@Valid @RequestBody UserMobileDTO dto) {
        log.info("手机号校验通过，手机号：{}", dto.getMobile());
        return ApiResult.success(true);
    }

}
```

自定义校验建议：

- 通用格式校验可以封装为自定义注解。
- 空值是否允许由业务决定，通常由 `@NotBlank` 单独控制。
- 校验器中不要访问复杂业务服务，避免校验逻辑过重。
- 业务唯一性校验可以放在 Service 层，不建议全部塞进参数校验器。
- 自定义校验注解放在 `common.validation` 包下统一管理。

## 全局异常处理

全局异常处理用于集中处理 Controller 层抛出的异常，将业务异常、参数校验异常、请求方法异常和系统异常统一转换为标准响应。这样可以避免在每个接口中重复编写 `try-catch`，同时保证错误响应格式一致。

### 业务异常设计

业务异常用于表达可预期的业务失败，例如用户不存在、余额不足、订单状态不允许修改、文件类型不支持等。业务异常应包含业务响应码和业务消息。

文件位置：`src/main/java/io/github/atengk/demo/common/exception/BizException.java`

下面的异常类用于表示业务处理失败。

```java
package io.github.atengk.demo.common.exception;

import io.github.atengk.demo.common.response.ResultCodeEnum;
import lombok.Getter;

/**
 * 业务异常。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Getter
public class BizException extends RuntimeException {

    private final Integer code;

    public BizException(String message) {
        super(message);
        this.code = ResultCodeEnum.BIZ_ERROR.getCode();
    }

    public BizException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public BizException(ResultCodeEnum resultCodeEnum) {
        super(resultCodeEnum.getMessage());
        this.code = resultCodeEnum.getCode();
    }

}
```

业务异常使用示例：

```java
package io.github.atengk.demo.service;

import io.github.atengk.demo.common.exception.BizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 订单业务组件。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Service
public class OrderService {

    public void cancel(Long orderId) {
        if (orderId == null) {
            throw new BizException(400, "订单ID不能为空");
        }

        boolean paid = true;
        if (paid) {
            log.warn("订单已支付，不允许取消，订单ID：{}", orderId);
            throw new BizException("订单已支付，不允许取消");
        }

        log.info("订单取消成功，订单ID：{}", orderId);
    }

}
```

业务异常设计建议：

- 业务失败使用 `BizException`，不要直接抛 `RuntimeException`。
- 异常消息应明确表达失败原因。
- 可恢复、可提示、可预期的问题归类为业务异常。
- 不要把系统异常伪装成业务异常。
- 不要在异常消息中返回敏感信息。

### 系统异常处理

系统异常是指未被业务主动捕获的非预期异常，例如空指针、类型转换错误、数据库连接异常、第三方服务异常等。系统异常应记录完整错误日志，但返回给前端时只给出通用提示。

文件位置：`src/main/java/io/github/atengk/demo/common/handler/GlobalExceptionHandler.java`

下面的全局异常处理器处理业务异常和系统异常。

```java
package io.github.atengk.demo.common.handler;

import io.github.atengk.demo.common.exception.BizException;
import io.github.atengk.demo.common.response.ApiResult;
import io.github.atengk.demo.common.response.ResultCodeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public ApiResult<Void> handleBizException(BizException e) {
        log.warn("业务异常：{}", e.getMessage());
        return ApiResult.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ApiResult<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return ApiResult.fail(ResultCodeEnum.INTERNAL_ERROR);
    }

}
```

系统异常响应示例：

```json
{
  "code": 500,
  "message": "系统异常",
  "data": null
}
```

系统异常处理建议：

- 系统异常必须打印完整堆栈。
- 响应中不要返回 Java 异常类名和堆栈信息。
- 外部接口调用异常应记录请求标识、接口名称、必要参数和耗时。
- 数据库异常应记录业务上下文，不要记录明文密码。
- 生产环境错误响应应克制，避免泄露实现细节。

### 参数校验异常处理

参数校验异常通常来自 `@Valid`、`@Validated`、`@RequestParam` 类型转换失败、请求体格式错误等场景。全局异常处理器应统一提取校验失败消息并返回标准错误响应。

常见参数异常类型：

| 异常类型                              | 常见场景                          |
| ------------------------------------- | --------------------------------- |
| `MethodArgumentNotValidException`     | `@RequestBody` 对象校验失败       |
| `ConstraintViolationException`        | 路径参数、查询参数校验失败        |
| `BindException`                       | 表单对象或 Query 对象绑定校验失败 |
| `MethodArgumentTypeMismatchException` | 参数类型转换失败                  |
| `HttpMessageNotReadableException`     | JSON 格式错误或请求体不可读       |

文件位置：`src/main/java/io/github/atengk/demo/common/handler/GlobalExceptionHandler.java`

下面的异常处理器增加了常见参数校验异常处理。

```java
package io.github.atengk.demo.common.handler;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.demo.common.exception.BizException;
import io.github.atengk.demo.common.response.ApiResult;
import io.github.atengk.demo.common.response.ResultCodeEnum;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * 全局异常处理器。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public ApiResult<Void> handleBizException(BizException e) {
        log.warn("业务异常：{}", e.getMessage());
        return ApiResult.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResult<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .filter(StrUtil::isNotBlank)
                .findFirst()
                .orElse(ResultCodeEnum.BAD_REQUEST.getMessage());

        log.warn("请求体参数校验失败：{}", message);
        return ApiResult.fail(ResultCodeEnum.BAD_REQUEST.getCode(), message);
    }

    @ExceptionHandler(BindException.class)
    public ApiResult<Void> handleBindException(BindException e) {
        String message = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .filter(StrUtil::isNotBlank)
                .findFirst()
                .orElse(ResultCodeEnum.BAD_REQUEST.getMessage());

        log.warn("参数绑定校验失败：{}", message);
        return ApiResult.fail(ResultCodeEnum.BAD_REQUEST.getCode(), message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ApiResult<Void> handleConstraintViolationException(ConstraintViolationException e) {
        Set<ConstraintViolation<?>> violations = e.getConstraintViolations();
        String message = CollUtil.isEmpty(violations)
                ? ResultCodeEnum.BAD_REQUEST.getMessage()
                : violations.stream()
                        .map(ConstraintViolation::getMessage)
                        .filter(StrUtil::isNotBlank)
                        .collect(Collectors.joining("; "));

        log.warn("基础参数校验失败：{}", message);
        return ApiResult.fail(ResultCodeEnum.BAD_REQUEST.getCode(), message);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ApiResult<Void> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        String message = StrUtil.format("参数类型错误：{}", e.getName());
        log.warn("参数类型转换失败，参数名：{}，参数值：{}", e.getName(), e.getValue());
        return ApiResult.fail(ResultCodeEnum.BAD_REQUEST.getCode(), message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ApiResult<Void> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.warn("请求体不可读或JSON格式错误：{}", e.getMessage());
        return ApiResult.fail(ResultCodeEnum.BAD_REQUEST.getCode(), "请求体格式错误");
    }

    @ExceptionHandler(Exception.class)
    public ApiResult<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return ApiResult.fail(ResultCodeEnum.INTERNAL_ERROR);
    }

}
```

参数校验错误响应示例：

```json
{
  "code": 400,
  "message": "用户名不能为空",
  "data": null
}
```

参数校验异常处理建议：

- 优先返回第一个校验错误，提示更清晰。
- 表单对象、JSON 对象、基础参数分别处理。
- JSON 格式错误返回统一提示，不暴露底层解析细节。
- 类型转换错误应提示具体参数名。
- 参数异常通常记录 `warn` 日志，不需要打印完整堆栈。

### 请求方法异常处理

请求方法异常通常发生在接口路径存在，但请求方法不匹配时。例如接口只支持 `POST /api/users`，调用方却使用 `GET /api/users`。这种异常应返回清晰的“请求方法不支持”。

文件位置：`src/main/java/io/github/atengk/demo/common/handler/GlobalExceptionHandler.java`

下面的处理方法用于处理请求方法不支持异常，可追加到前面的全局异常处理器中。

```java
@ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
public ApiResult<Void> handleHttpRequestMethodNotSupportedException(
        org.springframework.web.HttpRequestMethodNotSupportedException e) {

    String method = e.getMethod();
    String[] supportedMethods = e.getSupportedMethods();
    String supportedText = supportedMethods == null ? "" : String.join(",", supportedMethods);

    log.warn("请求方法不支持，请求方法：{}，支持方法：{}", method, supportedText);
    return ApiResult.fail(ResultCodeEnum.METHOD_NOT_ALLOWED.getCode(), "请求方法不支持");
}
```

如果希望保持完整类文件，可以使用下面版本。

文件位置：`src/main/java/io/github/atengk/demo/common/handler/GlobalExceptionHandler.java`

下面的全局异常处理器包含业务异常、参数异常、请求方法异常和系统异常处理。

```java
package io.github.atengk.demo.common.handler;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.demo.common.exception.BizException;
import io.github.atengk.demo.common.response.ApiResult;
import io.github.atengk.demo.common.response.ResultCodeEnum;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * 全局异常处理器。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public ApiResult<Void> handleBizException(BizException e) {
        log.warn("业务异常：{}", e.getMessage());
        return ApiResult.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResult<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .filter(StrUtil::isNotBlank)
                .findFirst()
                .orElse(ResultCodeEnum.BAD_REQUEST.getMessage());

        log.warn("请求体参数校验失败：{}", message);
        return ApiResult.fail(ResultCodeEnum.BAD_REQUEST.getCode(), message);
    }

    @ExceptionHandler(BindException.class)
    public ApiResult<Void> handleBindException(BindException e) {
        String message = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .filter(StrUtil::isNotBlank)
                .findFirst()
                .orElse(ResultCodeEnum.BAD_REQUEST.getMessage());

        log.warn("参数绑定校验失败：{}", message);
        return ApiResult.fail(ResultCodeEnum.BAD_REQUEST.getCode(), message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ApiResult<Void> handleConstraintViolationException(ConstraintViolationException e) {
        Set<ConstraintViolation<?>> violations = e.getConstraintViolations();
        String message = CollUtil.isEmpty(violations)
                ? ResultCodeEnum.BAD_REQUEST.getMessage()
                : violations.stream()
                        .map(ConstraintViolation::getMessage)
                        .filter(StrUtil::isNotBlank)
                        .collect(Collectors.joining("; "));

        log.warn("基础参数校验失败：{}", message);
        return ApiResult.fail(ResultCodeEnum.BAD_REQUEST.getCode(), message);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ApiResult<Void> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        String message = StrUtil.format("参数类型错误：{}", e.getName());
        log.warn("参数类型转换失败，参数名：{}，参数值：{}", e.getName(), e.getValue());
        return ApiResult.fail(ResultCodeEnum.BAD_REQUEST.getCode(), message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ApiResult<Void> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.warn("请求体不可读或JSON格式错误：{}", e.getMessage());
        return ApiResult.fail(ResultCodeEnum.BAD_REQUEST.getCode(), "请求体格式错误");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ApiResult<Void> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        String method = e.getMethod();
        String[] supportedMethods = e.getSupportedMethods();
        String supportedText = supportedMethods == null ? "" : String.join(",", supportedMethods);

        log.warn("请求方法不支持，请求方法：{}，支持方法：{}", method, supportedText);
        return ApiResult.fail(ResultCodeEnum.METHOD_NOT_ALLOWED.getCode(), "请求方法不支持");
    }

    @ExceptionHandler(Exception.class)
    public ApiResult<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return ApiResult.fail(ResultCodeEnum.INTERNAL_ERROR);
    }

}
```

请求方法异常响应示例：

```json
{
  "code": 405,
  "message": "请求方法不支持",
  "data": null
}
```

请求方法异常处理建议：

- `GET`、`POST`、`PUT`、`DELETE` 使用语义要清晰。
- 接口文档中标明请求方法。
- 前端联调时优先确认路径和方法是否同时正确。
- 不建议为了兼容错误调用而让一个接口同时支持所有方法。
- 请求方法异常通常记录 `warn` 日志。

### 统一错误响应

统一错误响应是全局异常处理的最终输出结果。无论是业务异常、参数异常、系统异常，返回结构都应保持一致，只是响应码和消息不同。

推荐错误响应结构：

```json
{
  "code": 400,
  "message": "用户名不能为空",
  "data": null
}
```

常见错误响应示例：

参数错误：

```json
{
  "code": 400,
  "message": "页码不能小于1",
  "data": null
}
```

业务错误：

```json
{
  "code": 1000,
  "message": "订单已支付，不允许取消",
  "data": null
}
```

请求方法错误：

```json
{
  "code": 405,
  "message": "请求方法不支持",
  "data": null
}
```

系统错误：

```json
{
  "code": 500,
  "message": "系统异常",
  "data": null
}
```

统一错误响应建议：

- 错误响应不要返回堆栈。
- 错误消息保持简短、明确。
- 业务错误和系统错误分开处理。
- 参数错误返回可修正的提示。
- 生产环境不暴露数据库、Redis、文件路径、服务器路径等内部信息。
- 日志中保留完整上下文，响应中只保留必要信息。
- 后续接入 TraceId 后，可以在响应中增加 `traceId` 字段，方便前后端联合排查。



## 业务分层开发

业务分层开发用于明确不同代码层的职责边界。Controller 负责请求入口，Service 负责业务规则，Repository 负责数据访问，DTO 负责接收请求，VO 负责返回响应，Entity 负责映射数据库数据。分层清晰后，代码更容易维护、测试和扩展。

### Controller 层职责

Controller 层是 HTTP 请求入口，负责接收请求参数、触发参数校验、调用 Service，并返回统一响应。Controller 不应直接编写复杂业务逻辑，不应直接访问数据库，也不应进行大量对象转换。

Controller 层主要职责如下：

| 职责         | 说明                                                         |
| ------------ | ------------------------------------------------------------ |
| 定义接口路径 | 使用 `@RequestMapping`、`@GetMapping`、`@PostMapping` 等注解 |
| 接收请求参数 | 接收路径参数、查询参数、请求体、请求头、文件参数             |
| 触发参数校验 | 使用 `@Valid`、`@Validated` 配合 DTO 校验                    |
| 调用业务服务 | 调用 Service 层完成业务处理                                  |
| 返回统一响应 | 使用 `ApiResult<T>` 统一返回结构                             |

Controller 层不建议做的事情：

- 不直接写 SQL。
- 不直接操作数据库对象。
- 不直接处理复杂业务规则。
- 不直接调用多个 Repository 组装复杂业务。
- 不捕获所有异常并手动拼接错误响应。
- 不返回 Entity 给前端。

文件位置：`src/main/java/io/github/atengk/demo/controller/UserLayerController.java`

下面的 Controller 只负责接收请求、调用 Service 和返回响应，不直接处理业务细节。

```java
package io.github.atengk.demo.controller;

import io.github.atengk.demo.common.response.ApiResult;
import io.github.atengk.demo.model.dto.UserCreateDTO;
import io.github.atengk.demo.model.query.UserPageQuery;
import io.github.atengk.demo.model.vo.UserVO;
import io.github.atengk.demo.common.response.PageResult;
import io.github.atengk.demo.service.UserLayerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户分层示例接口。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/layer/users")
public class UserLayerController {

    private final UserLayerService userLayerService;

    @GetMapping("/{id}")
    public ApiResult<UserVO> getById(@PathVariable Long id) {
        return ApiResult.success(userLayerService.getById(id));
    }

    @GetMapping
    public ApiResult<PageResult<UserVO>> page(@Valid UserPageQuery query) {
        return ApiResult.success(userLayerService.page(query));
    }

    @PostMapping
    public ApiResult<UserVO> create(@Valid @RequestBody UserCreateDTO dto) {
        return ApiResult.success(userLayerService.create(dto));
    }

}
```

Controller 层建议保持“薄控制器”风格。一个 Controller 方法通常只做四件事：接收参数、触发校验、调用 Service、返回结果。

### Service 层职责

Service 层是业务逻辑核心，负责组织业务流程、校验业务规则、控制事务边界、调用 Repository、处理数据转换和抛出业务异常。Service 层应表达业务语义，而不是简单转发 Controller 请求。

Service 层主要职责如下：

| 职责         | 说明                                       |
| ------------ | ------------------------------------------ |
| 业务规则校验 | 例如用户是否存在、状态是否允许修改         |
| 业务流程编排 | 例如创建订单时校验商品、扣减库存、生成订单 |
| 事务控制     | 在业务方法上使用 `@Transactional`          |
| 数据访问调用 | 调用 Repository 获取或修改数据             |
| 对象转换     | DTO 转 Entity，Entity 转 VO                |
| 业务异常处理 | 不满足业务规则时抛出 `BizException`        |

文件位置：`src/main/java/io/github/atengk/demo/service/UserLayerService.java`

下面的接口定义用户业务能力。

```java
package io.github.atengk.demo.service;

import io.github.atengk.demo.common.response.PageResult;
import io.github.atengk.demo.model.dto.UserCreateDTO;
import io.github.atengk.demo.model.query.UserPageQuery;
import io.github.atengk.demo.model.vo.UserVO;

/**
 * 用户分层业务接口。
 *
 * @author Ateng
 * @since 2026-05-09
 */
public interface UserLayerService {

    UserVO getById(Long id);

    PageResult<UserVO> page(UserPageQuery query);

    UserVO create(UserCreateDTO dto);

}
```

文件位置：`src/main/java/io/github/atengk/demo/service/impl/UserLayerServiceImpl.java`

下面的 Service 实现业务规则校验、数据访问调用和对象转换。

```java
package io.github.atengk.demo.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import io.github.atengk.demo.common.exception.BizException;
import io.github.atengk.demo.common.response.PageResult;
import io.github.atengk.demo.model.dto.UserCreateDTO;
import io.github.atengk.demo.model.entity.UserEntity;
import io.github.atengk.demo.model.query.UserPageQuery;
import io.github.atengk.demo.model.vo.UserVO;
import io.github.atengk.demo.repository.UserLayerRepository;
import io.github.atengk.demo.service.UserLayerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * 用户分层业务实现。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserLayerServiceImpl implements UserLayerService {

    private final UserLayerRepository userLayerRepository;

    @Override
    public UserVO getById(Long id) {
        UserEntity entity = userLayerRepository.getById(id);
        if (entity == null) {
            log.warn("用户不存在，用户ID：{}", id);
            throw new BizException(404, "用户不存在");
        }
        return BeanUtil.copyProperties(entity, UserVO.class);
    }

    @Override
    public PageResult<UserVO> page(UserPageQuery query) {
        Long total = userLayerRepository.count(query);
        if (total == null || total <= 0) {
            return PageResult.empty(query.getPageNum(), query.getPageSize());
        }

        List<UserEntity> entities = userLayerRepository.page(query);
        if (CollUtil.isEmpty(entities)) {
            return PageResult.empty(query.getPageNum(), query.getPageSize());
        }

        List<UserVO> records = BeanUtil.copyToList(entities, UserVO.class);
        return PageResult.of(query.getPageNum(), query.getPageSize(), total, records);
    }

    @Override
    public UserVO create(UserCreateDTO dto) {
        UserEntity entity = BeanUtil.copyProperties(dto, UserEntity.class);
        UserEntity savedEntity = userLayerRepository.save(entity);
        log.info("创建用户成功，用户ID：{}，用户名：{}", savedEntity.getId(), savedEntity.getUsername());
        return BeanUtil.copyProperties(savedEntity, UserVO.class);
    }

}
```

Service 层建议：

- 业务规则放在 Service，不放在 Controller。
- 事务注解通常加在 Service 方法上。
- Service 方法命名应体现业务含义。
- 复杂业务可以拆分多个 Service 或引入 Facade。
- Service 可以调用多个 Repository，但 Repository 不应反向调用 Service。
- Service 对外返回 VO、BO 或业务结果对象，不直接暴露数据库细节。

### Repository 层职责

Repository 层负责数据访问，对 Service 层屏蔽底层数据库操作细节。Repository 可以基于 `JdbcClient`、`JdbcTemplate`、MyBatis、MyBatis-Plus、JPA 等技术实现。无论底层技术如何变化，Service 层都应尽量不受影响。

Repository 层主要职责如下：

| 职责         | 说明                                 |
| ------------ | ------------------------------------ |
| 查询数据     | 根据 ID、条件、分页参数查询数据库    |
| 写入数据     | 新增、修改、删除数据库记录           |
| 封装 SQL     | 维护 SQL 语句和参数绑定              |
| 返回 Entity  | 将数据库结果转换为 Entity            |
| 屏蔽实现细节 | Service 不关心底层使用 JDBC 还是 ORM |

文件位置：`src/main/java/io/github/atengk/demo/repository/UserLayerRepository.java`

下面的 Repository 接口定义用户数据访问能力。

```java
package io.github.atengk.demo.repository;

import io.github.atengk.demo.model.entity.UserEntity;
import io.github.atengk.demo.model.query.UserPageQuery;

import java.util.List;

/**
 * 用户数据访问接口。
 *
 * @author Ateng
 * @since 2026-05-09
 */
public interface UserLayerRepository {

    UserEntity getById(Long id);

    Long count(UserPageQuery query);

    List<UserEntity> page(UserPageQuery query);

    UserEntity save(UserEntity entity);

}
```

文件位置：`src/main/java/io/github/atengk/demo/repository/impl/UserLayerRepositoryImpl.java`

下面的 Repository 使用 `JdbcClient` 封装用户查询和新增操作。

```java
package io.github.atengk.demo.repository.impl;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.demo.model.entity.UserEntity;
import io.github.atengk.demo.model.query.UserPageQuery;
import io.github.atengk.demo.repository.UserLayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户数据访问实现。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Repository
@RequiredArgsConstructor
public class UserLayerRepositoryImpl implements UserLayerRepository {

    private final JdbcClient jdbcClient;

    @Override
    public UserEntity getById(Long id) {
        return jdbcClient.sql("""
                        select id, username, nickname, email, create_time
                        from sys_user
                        where id = :id
                        """)
                .param("id", id)
                .query(UserEntity.class)
                .optional()
                .orElse(null);
    }

    @Override
    public Long count(UserPageQuery query) {
        StringBuilder sql = new StringBuilder("""
                select count(1)
                from sys_user
                where deleted = 0
                """);

        Map<String, Object> params = new HashMap<>();
        if (StrUtil.isNotBlank(query.getKeyword())) {
            sql.append(" and (username like :keyword or nickname like :keyword)");
            params.put("keyword", StrUtil.format("%{}%", query.getKeyword()));
        }

        return jdbcClient.sql(sql.toString())
                .params(params)
                .query(Long.class)
                .single();
    }

    @Override
    public List<UserEntity> page(UserPageQuery query) {
        StringBuilder sql = new StringBuilder("""
                select id, username, nickname, email, create_time
                from sys_user
                where deleted = 0
                """);

        Map<String, Object> params = new HashMap<>();
        if (StrUtil.isNotBlank(query.getKeyword())) {
            sql.append(" and (username like :keyword or nickname like :keyword)");
            params.put("keyword", StrUtil.format("%{}%", query.getKeyword()));
        }

        sql.append(" order by id desc limit :offset, :pageSize");
        params.put("offset", (query.getPageNum() - 1) * query.getPageSize());
        params.put("pageSize", query.getPageSize());

        return jdbcClient.sql(sql.toString())
                .params(params)
                .query(UserEntity.class)
                .list();
    }

    @Override
    public UserEntity save(UserEntity entity) {
        jdbcClient.sql("""
                        insert into sys_user(username, nickname, email, deleted, create_time)
                        values (:username, :nickname, :email, 0, now())
                        """)
                .param("username", entity.getUsername())
                .param("nickname", entity.getNickname())
                .param("email", entity.getEmail())
                .update();

        return entity;
    }

}
```

Repository 层建议：

- Repository 只处理数据访问，不处理业务规则。
- Repository 返回 Entity 或基础数据对象。
- SQL 参数必须使用参数绑定，避免字符串拼接用户输入。
- 分页、排序、条件查询应封装清楚。
- 数据库异常不建议在 Repository 中吞掉，应交给上层统一处理。
- 多表复杂查询可以返回专用数据对象，但不要直接返回 Controller VO。

### DTO 对象设计

DTO 用于接收客户端请求参数，主要服务于 Controller 入参。DTO 应表达请求场景，例如创建用户、修改用户、提交订单、分页查询等。DTO 不应直接复用 Entity，因为请求字段和数据库字段通常不完全一致。

DTO 设计原则：

- 按场景设计 DTO，例如 `UserCreateDTO`、`UserUpdateDTO`。
- DTO 字段只包含请求需要的内容。
- DTO 中添加参数校验注解。
- DTO 不包含数据库审计字段，例如 `createTime`、`updateTime`、`deleted`。
- DTO 不写复杂业务逻辑。
- DTO 不直接返回给前端，响应使用 VO。

文件位置：`src/main/java/io/github/atengk/demo/model/dto/UserCreateDTO.java`

下面的 DTO 用于接收用户创建请求参数。

```java
package io.github.atengk.demo.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户创建请求参数。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
public class UserCreateDTO {

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

文件位置：`src/main/java/io/github/atengk/demo/model/dto/UserUpdateDTO.java`

下面的 DTO 用于接收用户修改请求参数。

```java
package io.github.atengk.demo.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户修改请求参数。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
public class UserUpdateDTO {

    @NotBlank(message = "昵称不能为空")
    @Size(max = 50, message = "昵称长度不能超过50个字符")
    private String nickname;

    @Email(message = "邮箱格式不正确")
    private String email;

}
```

DTO 设计建议：

- 创建和修改 DTO 分开，避免校验规则冲突。
- 查询条件使用 Query 对象，不和创建 DTO 混用。
- DTO 不要继承 Entity。
- DTO 不要包含响应展示字段。
- DTO 中可以使用嵌套对象，但需要配合 `@Valid`。

### VO 对象设计

VO 用于返回给前端或调用方，主要服务于接口响应。VO 应面向展示结果设计，只包含调用方需要看到的数据，不应暴露数据库内部字段和敏感字段。

VO 设计原则：

- VO 字段应满足前端展示需求。
- VO 不返回密码、密钥、逻辑删除字段等敏感或内部字段。
- VO 可以根据场景拆分，例如 `UserVO`、`UserDetailVO`、`UserSimpleVO`。
- VO 不直接参与数据库写入。
- Entity 转 VO 通常在 Service 层完成。

文件位置：`src/main/java/io/github/atengk/demo/model/vo/UserVO.java`

下面的 VO 用于返回用户基础信息。

```java
package io.github.atengk.demo.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户响应对象。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
public class UserVO {

    private Long id;

    private String username;

    private String nickname;

    private String email;

    private LocalDateTime createTime;

}
```

文件位置：`src/main/java/io/github/atengk/demo/model/vo/UserSimpleVO.java`

下面的 VO 用于下拉框、选择器等轻量场景。

```java
package io.github.atengk.demo.model.vo;

import lombok.Data;

/**
 * 用户简要响应对象。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
public class UserSimpleVO {

    private Long id;

    private String nickname;

}
```

VO 设计建议：

- 列表接口返回轻量 VO。
- 详情接口返回详情 VO。
- 敏感字段必须脱敏或不返回。
- 时间字段格式应由 Jackson 统一处理。
- 不要让前端依赖 Entity 字段结构。

### Entity 对象设计

Entity 用于表示数据库表结构，是数据访问层和数据库之间的数据载体。Entity 应尽量与数据库表字段对应，但不应承载接口展示逻辑和复杂业务流程。

Entity 设计原则：

- Entity 字段对应数据库表字段。
- Entity 不直接作为请求参数。
- Entity 不直接返回给前端。
- Entity 中可以包含审计字段，例如 `createTime`、`updateTime`、`deleted`。
- Entity 应放在 `model.entity` 包下。

文件位置：`src/main/java/io/github/atengk/demo/model/entity/UserEntity.java`

下面的 Entity 对应 `sys_user` 表。

```java
package io.github.atengk.demo.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体对象。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
public class UserEntity {

    private Long id;

    private String username;

    private String nickname;

    private String email;

    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

}
```

数据库表结构示例：

```sql
-- 用户表，用于保存系统用户基础信息
create table sys_user (
    id bigint primary key auto_increment comment '用户ID',
    username varchar(30) not null comment '用户名',
    nickname varchar(50) not null comment '昵称',
    email varchar(100) null comment '邮箱',
    deleted tinyint not null default 0 comment '逻辑删除标识：0未删除，1已删除',
    create_time datetime not null default current_timestamp comment '创建时间',
    update_time datetime null on update current_timestamp comment '更新时间'
) comment '系统用户表';
```

Entity 设计建议：

- Entity 和表结构保持一致。
- 不在 Entity 中写接口校验注解，校验应放在 DTO。
- 不在 Entity 中放前端展示专用字段。
- 敏感字段如果必须存在于 Entity，应避免转换到 VO。
- 数据库字段命名建议使用下划线，Java 字段命名使用小驼峰。

## 数据访问基础

数据访问基础用于说明 Spring Boot 3 项目如何配置数据源、使用 `JdbcClient` 和 `JdbcTemplate` 操作数据库，以及如何封装查询和更新逻辑。对于简单项目，`JdbcClient` 和 `JdbcTemplate` 足够完成常见 SQL 操作；对于复杂 CRUD，可以选择 MyBatis、MyBatis-Plus 或 JPA。

### 数据源配置

数据源配置用于建立应用与数据库之间的连接。Spring Boot 支持通过 `spring.datasource.*` 配置数据源，并且在引入 `spring-boot-starter-jdbc` 或 `spring-boot-starter-data-jpa` 后通常会自动配置连接池；官方文档说明，如果使用这些 Starter，会自动获得 HikariCP 依赖，并且 Spring Boot 在可用时优先选择 HikariCP。([Home](https://docs.spring.io/spring-boot/reference/data/sql.html?utm_source=chatgpt.com))

文件位置：`pom.xml`

下面的 Maven 依赖用于启用 JDBC 数据访问和 MySQL 驱动。

```xml
<dependencies>
    <!-- JDBC 数据访问基础依赖，提供 JdbcTemplate、JdbcClient 和 DataSource 自动配置 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-jdbc</artifactId>
    </dependency>

    <!-- MySQL JDBC 驱动，运行时连接 MySQL 数据库 -->
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
        <scope>runtime</scope>
    </dependency>
</dependencies>
```

文件位置：`src/main/resources/application.yml`

下面的配置用于配置 MySQL 数据源和 HikariCP 连接池。

```yaml
spring:
  datasource:
    # 数据库连接地址
    url: jdbc:mysql://127.0.0.1:3306/spring_boot3_demo?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    # 数据库用户名
    username: root
    # 数据库密码，生产环境应通过环境变量或外部配置注入
    password: root
    # MySQL 驱动类，通常可以由 Spring Boot 根据 URL 推断
    driver-class-name: com.mysql.cj.jdbc.Driver
    hikari:
      # 连接池名称
      pool-name: SpringBoot3HikariCP
      # 最小空闲连接数
      minimum-idle: 5
      # 最大连接数
      maximum-pool-size: 20
      # 连接超时时间，单位毫秒
      connection-timeout: 30000
      # 空闲连接最大存活时间，单位毫秒
      idle-timeout: 600000
      # 连接最大生命周期，单位毫秒
      max-lifetime: 1800000
```

数据源验证接口：

文件位置：`src/main/java/io/github/atengk/demo/controller/DataSourceCheckController.java`

下面的接口通过 `DataSource` 获取数据库连接，验证数据源是否可用。

```java
package io.github.atengk.demo.controller;

import io.github.atengk.demo.common.response.ApiResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * 数据源检查接口。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/datasource")
public class DataSourceCheckController {

    private final DataSource dataSource;

    @GetMapping("/check")
    public ApiResult<Boolean> check() {
        try (Connection connection = dataSource.getConnection()) {
            boolean valid = connection.isValid(3);
            log.info("数据源连接检查完成，是否可用：{}", valid);
            return ApiResult.success(valid);
        } catch (Exception e) {
            log.error("数据源连接检查失败", e);
            return ApiResult.fail(500, "数据源连接失败");
        }
    }

}
```

验证命令：

```bash
curl http://localhost:8080/api/datasource/check
```

数据源配置建议：

- 生产环境数据库密码使用环境变量或外部配置注入。
- 连接池参数应结合数据库规格和应用并发量设置。
- 不建议本地、测试、生产共用同一数据库。
- 数据源连接失败时优先检查 URL、用户名、密码、网络、防火墙和驱动依赖。
- 多数据源场景应明确主数据源和事务管理器。

### JdbcClient 使用

`JdbcClient` 是 Spring Framework 6.1 开始提供的 JDBC 访问客户端，提供更流式的 API，支持位置参数和命名参数。它适合在 Spring Boot 3.2 及以上项目中编写简洁 SQL 操作；复杂批处理、存储过程等场景仍可继续使用较底层的 `JdbcTemplate`、`NamedParameterJdbcTemplate`、`SimpleJdbcInsert` 或 `SimpleJdbcCall`。([Home](https://docs.spring.io/spring-framework/docs/6.2.6/javadoc-api/org/springframework/jdbc/core/simple/JdbcClient.html?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/demo/repository/UserJdbcClientRepository.java`

下面的 Repository 使用 `JdbcClient` 完成用户查询、新增和更新操作。

```java
package io.github.atengk.demo.repository;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.demo.model.entity.UserEntity;
import io.github.atengk.demo.model.query.UserPageQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * JdbcClient 用户数据访问组件。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Repository
@RequiredArgsConstructor
public class UserJdbcClientRepository {

    private final JdbcClient jdbcClient;

    public UserEntity getById(Long id) {
        return jdbcClient.sql("""
                        select id, username, nickname, email, deleted, create_time, update_time
                        from sys_user
                        where id = :id and deleted = 0
                        """)
                .param("id", id)
                .query(UserEntity.class)
                .optional()
                .orElse(null);
    }

    public List<UserEntity> page(UserPageQuery query) {
        StringBuilder sql = new StringBuilder("""
                select id, username, nickname, email, deleted, create_time, update_time
                from sys_user
                where deleted = 0
                """);

        Map<String, Object> params = new HashMap<>();
        if (StrUtil.isNotBlank(query.getKeyword())) {
            sql.append(" and (username like :keyword or nickname like :keyword)");
            params.put("keyword", StrUtil.format("%{}%", query.getKeyword()));
        }

        sql.append(" order by id desc limit :offset, :pageSize");
        params.put("offset", (query.getPageNum() - 1) * query.getPageSize());
        params.put("pageSize", query.getPageSize());

        return jdbcClient.sql(sql.toString())
                .params(params)
                .query(UserEntity.class)
                .list();
    }

    public int insert(UserEntity entity) {
        return jdbcClient.sql("""
                        insert into sys_user(username, nickname, email, deleted, create_time)
                        values (:username, :nickname, :email, 0, now())
                        """)
                .param("username", entity.getUsername())
                .param("nickname", entity.getNickname())
                .param("email", entity.getEmail())
                .update();
    }

    public int updateNickname(Long id, String nickname) {
        return jdbcClient.sql("""
                        update sys_user
                        set nickname = :nickname, update_time = now()
                        where id = :id and deleted = 0
                        """)
                .param("id", id)
                .param("nickname", nickname)
                .update();
    }

}
```

`JdbcClient` 使用建议：

- 优先使用命名参数，例如 `:id`、`:username`。
- 查询单条数据使用 `optional()`，避免无数据时报错。
- 查询列表使用 `list()`。
- 新增、修改、删除使用 `update()`。
- 用户输入参数必须绑定，不能直接拼接到 SQL。
- 动态 SQL 可以用 `StringBuilder` 组合固定片段，参数仍使用绑定方式传入。
- 复杂批量写入或存储过程可以使用 `JdbcTemplate` 或其他更专门的组件。

### JdbcTemplate 使用

`JdbcTemplate` 是 Spring JDBC 中更传统的数据访问工具，适合执行 SQL 查询、更新、批处理和自定义 RowMapper。相比 `JdbcClient`，`JdbcTemplate` API 更底层，但在老项目、批量操作和复杂映射场景中仍然常用。

文件位置：`src/main/java/io/github/atengk/demo/repository/UserJdbcTemplateRepository.java`

下面的 Repository 使用 `JdbcTemplate` 完成用户查询和批量新增。

```java
package io.github.atengk.demo.repository;

import io.github.atengk.demo.model.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;

/**
 * JdbcTemplate 用户数据访问组件。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Repository
@RequiredArgsConstructor
public class UserJdbcTemplateRepository {

    private final JdbcTemplate jdbcTemplate;

    public UserEntity getById(Long id) {
        String sql = """
                select id, username, nickname, email, deleted, create_time, update_time
                from sys_user
                where id = ? and deleted = 0
                """;

        List<UserEntity> list = jdbcTemplate.query(sql, (rs, rowNum) -> {
            UserEntity entity = new UserEntity();
            entity.setId(rs.getLong("id"));
            entity.setUsername(rs.getString("username"));
            entity.setNickname(rs.getString("nickname"));
            entity.setEmail(rs.getString("email"));
            entity.setDeleted(rs.getInt("deleted"));

            Timestamp createTime = rs.getTimestamp("create_time");
            if (createTime != null) {
                entity.setCreateTime(createTime.toLocalDateTime());
            }

            Timestamp updateTime = rs.getTimestamp("update_time");
            if (updateTime != null) {
                entity.setUpdateTime(updateTime.toLocalDateTime());
            }

            return entity;
        }, id);

        return list.isEmpty() ? null : list.get(0);
    }

    public int batchInsert(List<UserEntity> users) {
        String sql = """
                insert into sys_user(username, nickname, email, deleted, create_time)
                values (?, ?, ?, 0, now())
                """;

        int[] results = jdbcTemplate.batchUpdate(sql, users, 500, (ps, user) -> {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getNickname());
            ps.setString(3, user.getEmail());
        });

        int total = 0;
        for (int result : results) {
            total += result;
        }
        return total;
    }

}
```

`JdbcTemplate` 使用建议：

- 简单查询可以使用 `queryForObject`，但要注意无数据异常。
- 查询列表使用 `query` 配合 RowMapper。
- 批量操作使用 `batchUpdate`。
- SQL 参数使用 `?` 占位符，不拼接用户输入。
- 重复 RowMapper 可以提取为私有方法或独立类。
- 新项目简单 SQL 优先考虑 `JdbcClient`，复杂批处理可继续使用 `JdbcTemplate`。

### 数据查询封装

数据查询封装用于统一处理分页、条件查询、排序和空数据返回。Repository 层负责 SQL 查询，Service 层负责业务规则和返回结构转换。

文件位置：`src/main/java/io/github/atengk/demo/model/query/UserSearchQuery.java`

下面的查询对象用于封装用户搜索条件。

```java
package io.github.atengk.demo.model.query;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 用户搜索查询参数。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
public class UserSearchQuery {

    @Min(value = 1, message = "页码不能小于1")
    private Long pageNum = 1L;

    @Min(value = 1, message = "每页条数不能小于1")
    @Max(value = 100, message = "每页条数不能大于100")
    private Long pageSize = 10L;

    private String keyword;

    private String email;

}
```

文件位置：`src/main/java/io/github/atengk/demo/repository/UserSearchRepository.java`

下面的 Repository 封装用户分页查询和总数查询。

```java
package io.github.atengk.demo.repository;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.demo.model.entity.UserEntity;
import io.github.atengk.demo.model.query.UserSearchQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户搜索数据访问组件。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Repository
@RequiredArgsConstructor
public class UserSearchRepository {

    private final JdbcClient jdbcClient;

    public Long count(UserSearchQuery query) {
        SqlBuildResult sqlBuildResult = buildWhereSql(query, "select count(1) from sys_user where deleted = 0");

        return jdbcClient.sql(sqlBuildResult.sql())
                .params(sqlBuildResult.params())
                .query(Long.class)
                .single();
    }

    public List<UserEntity> page(UserSearchQuery query) {
        SqlBuildResult sqlBuildResult = buildWhereSql(query, """
                select id, username, nickname, email, deleted, create_time, update_time
                from sys_user
                where deleted = 0
                """);

        String pageSql = sqlBuildResult.sql() + " order by id desc limit :offset, :pageSize";
        Map<String, Object> params = sqlBuildResult.params();
        params.put("offset", (query.getPageNum() - 1) * query.getPageSize());
        params.put("pageSize", query.getPageSize());

        return jdbcClient.sql(pageSql)
                .params(params)
                .query(UserEntity.class)
                .list();
    }

    private SqlBuildResult buildWhereSql(UserSearchQuery query, String baseSql) {
        StringBuilder sql = new StringBuilder(baseSql);
        Map<String, Object> params = new HashMap<>();

        if (StrUtil.isNotBlank(query.getKeyword())) {
            sql.append(" and (username like :keyword or nickname like :keyword)");
            params.put("keyword", StrUtil.format("%{}%", query.getKeyword()));
        }

        if (StrUtil.isNotBlank(query.getEmail())) {
            sql.append(" and email = :email");
            params.put("email", query.getEmail());
        }

        return new SqlBuildResult(sql.toString(), params);
    }

    private record SqlBuildResult(String sql, Map<String, Object> params) {
    }

}
```

数据查询封装建议：

- 查询参数封装为 Query 对象。
- 查询总数和查询列表分开。
- 动态条件只拼接固定 SQL 片段。
- 用户输入必须通过参数绑定传入。
- 分页参数必须限制最大值。
- 排序字段如果来自前端，必须使用白名单校验。
- Repository 返回 Entity，Service 转换为 VO 或分页响应。

### 数据更新处理

数据更新包括新增、修改、逻辑删除和状态变更。更新操作通常需要在 Service 层进行业务校验，并在 Repository 层执行 SQL。涉及多条 SQL 的业务更新应放在事务中。

文件位置：`src/main/java/io/github/atengk/demo/repository/UserUpdateRepository.java`

下面的 Repository 封装用户新增、修改和逻辑删除操作。

```java
package io.github.atengk.demo.repository;

import io.github.atengk.demo.model.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * 用户更新数据访问组件。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Repository
@RequiredArgsConstructor
public class UserUpdateRepository {

    private final JdbcClient jdbcClient;

    public int insert(UserEntity entity) {
        return jdbcClient.sql("""
                        insert into sys_user(username, nickname, email, deleted, create_time)
                        values (:username, :nickname, :email, 0, now())
                        """)
                .param("username", entity.getUsername())
                .param("nickname", entity.getNickname())
                .param("email", entity.getEmail())
                .update();
    }

    public int updateById(Long id, UserEntity entity) {
        return jdbcClient.sql("""
                        update sys_user
                        set nickname = :nickname,
                            email = :email,
                            update_time = now()
                        where id = :id and deleted = 0
                        """)
                .param("id", id)
                .param("nickname", entity.getNickname())
                .param("email", entity.getEmail())
                .update();
    }

    public int deleteById(Long id) {
        return jdbcClient.sql("""
                        update sys_user
                        set deleted = 1,
                            update_time = now()
                        where id = :id and deleted = 0
                        """)
                .param("id", id)
                .update();
    }

}
```

文件位置：`src/main/java/io/github/atengk/demo/service/UserUpdateService.java`

下面的 Service 对更新结果进行业务判断，避免静默失败。

```java
package io.github.atengk.demo.service;

import cn.hutool.core.bean.BeanUtil;
import io.github.atengk.demo.common.exception.BizException;
import io.github.atengk.demo.model.dto.UserCreateDTO;
import io.github.atengk.demo.model.dto.UserUpdateDTO;
import io.github.atengk.demo.model.entity.UserEntity;
import io.github.atengk.demo.repository.UserUpdateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 用户更新业务组件。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserUpdateService {

    private final UserUpdateRepository userUpdateRepository;

    public void create(UserCreateDTO dto) {
        UserEntity entity = BeanUtil.copyProperties(dto, UserEntity.class);
        int rows = userUpdateRepository.insert(entity);
        if (rows != 1) {
            throw new BizException("用户创建失败");
        }
        log.info("用户创建成功，用户名：{}", dto.getUsername());
    }

    public void update(Long id, UserUpdateDTO dto) {
        UserEntity entity = BeanUtil.copyProperties(dto, UserEntity.class);
        int rows = userUpdateRepository.updateById(id, entity);
        if (rows != 1) {
            throw new BizException("用户不存在或已被删除");
        }
        log.info("用户修改成功，用户ID：{}", id);
    }

    public void delete(Long id) {
        int rows = userUpdateRepository.deleteById(id);
        if (rows != 1) {
            throw new BizException("用户不存在或已被删除");
        }
        log.info("用户删除成功，用户ID：{}", id);
    }

}
```

数据更新处理建议：

- 新增、修改、删除操作必须检查影响行数。
- 删除优先使用逻辑删除，除非明确需要物理删除。
- 更新条件应包含主键和必要状态条件。
- 涉及多表更新时必须使用事务。
- 不要忽略更新结果，否则容易出现接口成功但数据未变更。
- 更新操作日志应记录关键业务标识，不记录敏感字段。

## 事务管理

事务管理用于保证一组数据库操作要么全部成功，要么全部回滚。Spring 事务常用于创建订单、扣减库存、账户扣款、批量导入、状态流转等涉及多次数据写入的业务场景。Spring 的声明式事务通过 AOP 代理实现，`@Transactional` 的默认传播行为是 `REQUIRED`，默认隔离级别是 `DEFAULT`，默认读写事务，并且 `RuntimeException` 或 `Error` 会触发回滚，受检异常默认不回滚。([Home](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html?utm_source=chatgpt.com))

### 声明式事务

声明式事务通过 `@Transactional` 注解实现，通常加在 Service 层的 public 方法上。它可以让开发者专注业务逻辑，而不需要手动获取连接、提交事务或回滚事务。

文件位置：`src/main/java/io/github/atengk/demo/service/OrderCreateService.java`

下面的 Service 在创建订单时同时写入订单主表和订单明细表，任一操作失败都会回滚。

```java
package io.github.atengk.demo.service;

import io.github.atengk.demo.common.exception.BizException;
import io.github.atengk.demo.model.dto.OrderCreateDTO;
import io.github.atengk.demo.repository.OrderItemRepository;
import io.github.atengk.demo.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 订单创建业务组件。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderCreateService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    @Transactional(rollbackFor = Exception.class)
    public Long createOrder(OrderCreateDTO dto) {
        Long orderId = orderRepository.insertOrder(dto);
        if (orderId == null) {
            throw new BizException("订单创建失败");
        }

        int itemRows = orderItemRepository.batchInsert(orderId, dto.getItems());
        if (itemRows != dto.getItems().size()) {
            throw new BizException("订单明细创建失败");
        }

        log.info("订单创建成功，订单ID：{}，商品数量：{}", orderId, dto.getItems().size());
        return orderId;
    }

}
```

声明式事务建议：

- `@Transactional` 通常加在 Service 层。
- 涉及多次写库操作的方法应加事务。
- 查询方法不随意加写事务。
- 需要回滚受检异常时配置 `rollbackFor = Exception.class`。
- 不建议把事务加在 Controller 层。
- 长事务中避免调用耗时外部接口。

### 事务传播行为

事务传播行为用于定义当前方法被另一个事务方法调用时，应该加入已有事务、创建新事务，还是以非事务方式执行。最常用的是 `REQUIRED` 和 `REQUIRES_NEW`。

常见传播行为：

| 传播行为        | 说明                               | 常见场景                               |
| --------------- | ---------------------------------- | -------------------------------------- |
| `REQUIRED`      | 有事务就加入，没有事务就新建       | 默认选择，普通业务写操作               |
| `REQUIRES_NEW`  | 挂起当前事务，创建新事务           | 操作日志、审计日志独立提交             |
| `SUPPORTS`      | 有事务就加入，没有事务就非事务执行 | 查询方法                               |
| `MANDATORY`     | 必须存在事务，否则报错             | 必须依赖外层事务的方法                 |
| `NOT_SUPPORTED` | 以非事务方式执行，挂起当前事务     | 不需要事务的耗时操作                   |
| `NEVER`         | 必须没有事务，否则报错             | 禁止事务的操作                         |
| `NESTED`        | 嵌套事务，依赖保存点               | 局部回滚场景，需数据库和事务管理器支持 |

文件位置：`src/main/java/io/github/atengk/demo/service/OrderAuditService.java`

下面的审计日志使用 `REQUIRES_NEW`，即使外层订单事务回滚，审计日志也可以独立提交。

```java
package io.github.atengk.demo.service;

import io.github.atengk.demo.repository.OrderAuditRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 订单审计业务组件。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderAuditService {

    private final OrderAuditRepository orderAuditRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void recordCreateLog(Long orderId, String content) {
        orderAuditRepository.insert(orderId, content);
        log.info("订单审计日志写入成功，订单ID：{}", orderId);
    }

}
```

文件位置：`src/main/java/io/github/atengk/demo/service/OrderBusinessService.java`

下面的业务方法在主事务中创建订单，并调用独立事务写审计日志。

```java
package io.github.atengk.demo.service;

import io.github.atengk.demo.model.dto.OrderCreateDTO;
import io.github.atengk.demo.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 订单业务处理组件。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderBusinessService {

    private final OrderRepository orderRepository;
    private final OrderAuditService orderAuditService;

    @Transactional(rollbackFor = Exception.class)
    public Long create(OrderCreateDTO dto) {
        Long orderId = orderRepository.insertOrder(dto);
        orderAuditService.recordCreateLog(orderId, "创建订单");
        log.info("订单业务处理完成，订单ID：{}", orderId);
        return orderId;
    }

}
```

事务传播行为建议：

- 默认使用 `REQUIRED`。
- 独立日志、审计记录可以使用 `REQUIRES_NEW`。
- 查询方法一般不需要强制事务，可使用只读事务。
- 不要滥用 `REQUIRES_NEW`，否则会增加事务复杂度。
- 使用 `NESTED` 前必须确认数据库和事务管理器支持保存点。
- 传播行为必须结合业务一致性要求设计。

### 事务回滚规则

Spring 默认只对 `RuntimeException` 和 `Error` 回滚，对受检异常默认不回滚。如果业务中可能抛出受检异常并希望回滚，需要显式配置 `rollbackFor = Exception.class`。([Home](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/demo/service/PaymentService.java`

下面的支付业务配置 `rollbackFor = Exception.class`，确保受检异常也能触发回滚。

```java
package io.github.atengk.demo.service;

import io.github.atengk.demo.common.exception.BizException;
import io.github.atengk.demo.repository.AccountRepository;
import io.github.atengk.demo.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;

/**
 * 支付业务组件。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final AccountRepository accountRepository;
    private final PaymentRepository paymentRepository;

    @Transactional(rollbackFor = Exception.class)
    public void pay(Long userId, BigDecimal amount) throws IOException {
        int accountRows = accountRepository.decreaseBalance(userId, amount);
        if (accountRows != 1) {
            throw new BizException("账户余额扣减失败");
        }

        int paymentRows = paymentRepository.insertPaymentRecord(userId, amount);
        if (paymentRows != 1) {
            throw new BizException("支付记录创建失败");
        }

        boolean remoteFailed = false;
        if (remoteFailed) {
            throw new IOException("远程支付确认失败");
        }

        log.info("支付处理成功，用户ID：{}，金额：{}", userId, amount);
    }

}
```

也可以配置某些异常不回滚。

```java
@Transactional(
        rollbackFor = Exception.class,
        noRollbackFor = IllegalArgumentException.class
)
public void updateWithRule() {
    // 示例：IllegalArgumentException 不触发回滚，其他 Exception 触发回滚
}
```

事务回滚建议：

- 业务写操作建议显式配置 `rollbackFor = Exception.class`。
- 业务异常继承 `RuntimeException`，默认可回滚。
- 不要捕获异常后不抛出，否则事务无法感知失败。
- 如果必须捕获异常，应在处理后继续抛出，或手动标记回滚。
- 不要把所有异常都转换成成功响应。
- 回滚规则应结合业务一致性要求，不要机械配置。

捕获异常后需要继续回滚时，可以这样处理：

```java
try {
    // 执行业务写操作
} catch (Exception e) {
    log.error("业务处理失败，准备回滚事务", e);
    throw e;
}
```

如果确实不能继续抛出异常，可以手动标记回滚：

```java
import org.springframework.transaction.interceptor.TransactionAspectSupport;

TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
```

手动标记回滚只适合特殊场景，常规业务仍建议通过抛出异常触发回滚。

### 只读事务

只读事务用于查询场景，声明当前方法不会修改数据。它可以帮助数据库驱动、连接池或 ORM 框架做一定优化，也能表达方法语义。只读事务不等于强制禁止写入，不能依赖它作为安全控制手段。

文件位置：`src/main/java/io/github/atengk/demo/service/UserReadService.java`

下面的 Service 使用只读事务处理查询操作。

```java
package io.github.atengk.demo.service;

import cn.hutool.core.bean.BeanUtil;
import io.github.atengk.demo.common.exception.BizException;
import io.github.atengk.demo.model.entity.UserEntity;
import io.github.atengk.demo.model.vo.UserVO;
import io.github.atengk.demo.repository.UserJdbcClientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户只读查询业务组件。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserReadService {

    private final UserJdbcClientRepository userJdbcClientRepository;

    @Transactional(readOnly = true)
    public UserVO getById(Long id) {
        UserEntity entity = userJdbcClientRepository.getById(id);
        if (entity == null) {
            log.warn("用户不存在，用户ID：{}", id);
            throw new BizException(404, "用户不存在");
        }
        return BeanUtil.copyProperties(entity, UserVO.class);
    }

}
```

只读事务建议：

- 查询详情、查询列表、统计查询可以使用 `readOnly = true`。
- 写操作不能放在只读事务方法中。
- 只读事务用于表达语义和优化，不作为权限控制。
- 高并发只读接口仍需关注索引、SQL 性能和缓存。
- 如果查询不需要事务一致性，也可以不加事务，视项目规范决定。

### 事务失效场景

事务失效是 Spring 项目中常见问题。声明了 `@Transactional` 不代表一定生效，事务依赖 Spring AOP 代理，只有通过代理对象调用符合条件的方法时才会被拦截。官方文档说明，在默认代理模式下，只有通过代理进入的外部方法调用会被拦截，同类内部自调用不会触发事务。([Home](https://docs.spring.io/spring-framework/docs/3.1.x/spring-framework-reference/html/transaction.html?utm_source=chatgpt.com))

常见事务失效场景如下：

| 场景                   | 原因                     | 处理方式                             |
| ---------------------- | ------------------------ | ------------------------------------ |
| 方法不是 `public`      | 代理通常拦截 public 方法 | 事务方法使用 public                  |
| 同类内部方法调用       | 没有经过 Spring 代理     | 拆分到另一个 Service                 |
| 异常被捕获未抛出       | 事务无法感知异常         | 捕获后继续抛出或标记回滚             |
| 抛出受检异常           | 默认不回滚               | 配置 `rollbackFor = Exception.class` |
| 类没有交给 Spring 管理 | 没有代理对象             | 使用 `@Service` 等注册 Bean          |
| 数据库表不支持事务     | 例如 MySQL MyISAM        | 使用 InnoDB                          |
| 方法被 `final` 修饰    | 代理可能无法增强         | 避免 final 事务方法                  |
| 多线程内执行数据库操作 | 事务绑定当前线程         | 不要期望事务跨线程传播               |

错误示例：同类内部调用导致事务不生效。

```java
package io.github.atengk.demo.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 事务失效示例业务组件。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Service
public class TransactionInvalidService {

    public void outerMethod() {
        // 同类内部调用，不经过 Spring 代理，innerMethod 上的事务可能不生效
        innerMethod();
    }

    @Transactional(rollbackFor = Exception.class)
    public void innerMethod() {
        log.info("执行内部事务方法");
    }

}
```

推荐处理：将事务方法拆分到另一个 Spring Bean。

文件位置：`src/main/java/io/github/atengk/demo/service/TransactionInnerService.java`

下面的内部事务 Service 独立注册为 Spring Bean，外部调用时可以经过代理对象。

```java
package io.github.atengk.demo.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 内部事务业务组件。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Service
public class TransactionInnerService {

    @Transactional(rollbackFor = Exception.class)
    public void innerMethod() {
        log.info("执行独立 Bean 中的事务方法");
    }

}
```

文件位置：`src/main/java/io/github/atengk/demo/service/TransactionOuterService.java`

下面的外部 Service 注入内部事务 Service，调用时可以触发事务代理。

```java
package io.github.atengk.demo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 外部事务调用业务组件。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Service
@RequiredArgsConstructor
public class TransactionOuterService {

    private final TransactionInnerService transactionInnerService;

    public void outerMethod() {
        transactionInnerService.innerMethod();
    }

}
```

错误示例：异常被捕获后未继续抛出。

```java
@Transactional(rollbackFor = Exception.class)
public void updateUser() {
    try {
        // 执行数据库更新
    } catch (Exception e) {
        log.error("更新用户失败，但异常被吞掉，事务可能不会回滚", e);
    }
}
```

推荐写法：记录日志后继续抛出异常。

```java
@Transactional(rollbackFor = Exception.class)
public void updateUser() {
    try {
        // 执行数据库更新
    } catch (Exception e) {
        log.error("更新用户失败，准备回滚事务", e);
        throw e;
    }
}
```

事务失效排查建议：

- 确认类是否由 Spring 容器管理。
- 确认事务方法是否为 public。
- 确认调用是否经过代理对象。
- 确认异常是否被吞掉。
- 确认异常类型是否满足回滚规则。
- 确认数据库表是否支持事务。
- 确认多数据源场景是否使用了正确的事务管理器。
- 开启事务相关日志辅助排查：

```yaml
logging:
  level:
    # 查看 Spring 事务拦截和提交回滚日志
    org.springframework.transaction: debug
```

事务管理整体建议：

- 查询使用只读事务或不加事务，按项目规范统一。
- 写操作涉及多条 SQL 时必须加事务。
- 事务边界放在 Service 层。
- 避免长事务，事务中不要执行耗时外部接口。
- 回滚规则显式配置，避免受检异常不回滚。
- 事务失效问题优先检查代理调用、异常处理和数据库引擎。



## 文件上传下载

文件上传下载是后台系统、内容管理系统、报表系统、导入导出功能中常见的基础能力。Spring Boot 3 可以通过 `MultipartFile` 接收上传文件，通过 `HttpServletResponse` 或 `ResponseEntity<Resource>` 输出下载文件。文件功能开发时不仅要关注接口实现，还要关注文件大小、文件类型、文件名安全、目录穿越、访问权限和下载响应头。

### 文件上传接口

文件上传接口通常使用 `POST` 请求和 `multipart/form-data` 格式提交。后端使用 `MultipartFile` 接收文件，并将文件保存到本地目录、对象存储或分布式文件系统中。基础项目可以先实现本地文件上传，后续再替换为 MinIO、S3、OSS 等对象存储。

文件位置：`src/main/resources/application.yml`

以下配置定义上传目录、允许的扩展名和访问前缀。

```yaml
spring:
  servlet:
    multipart:
      # 单个文件最大大小
      max-file-size: 20MB
      # 单次请求最大大小
      max-request-size: 50MB

demo:
  file:
    # 文件上传根目录，生产环境建议使用绝对路径
    upload-path: ./data/upload
    # 允许上传的文件扩展名
    allowed-extensions:
      - jpg
      - jpeg
      - png
      - pdf
      - xlsx
      - docx
    # 文件下载接口前缀
    download-prefix: /api/files/download
```

文件位置：`src/main/java/io/github/atengk/demo/config/properties/FileStorageProperties.java`

以下配置属性类用于绑定文件存储相关配置。

```java
package io.github.atengk.demo.config.properties;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

/**
 * 文件存储配置属性。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
@Validated
@ConfigurationProperties(prefix = "demo.file")
public class FileStorageProperties {

    /**
     * 文件上传根目录。
     */
    @NotBlank(message = "文件上传根目录不能为空")
    private String uploadPath = "./data/upload";

    /**
     * 允许上传的文件扩展名。
     */
    private List<String> allowedExtensions = new ArrayList<>();

    /**
     * 文件下载接口前缀。
     */
    private String downloadPrefix = "/api/files/download";

}
```

如果项目启动类尚未开启配置属性扫描，需要补充 `@ConfigurationPropertiesScan`。

文件位置：`src/main/java/io/github/atengk/demo/DemoApplication.java`

```java
package io.github.atengk.demo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Spring Boot 3 应用启动类。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@SpringBootApplication
@ConfigurationPropertiesScan
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
        log.info("Spring Boot 3 应用启动完成");
    }

}
```

文件位置：`src/main/java/io/github/atengk/demo/model/vo/FileUploadVO.java`

以下 VO 用于返回上传后的文件信息。

```java
package io.github.atengk.demo.model.vo;

import lombok.Data;

/**
 * 文件上传响应对象。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
public class FileUploadVO {

    /**
     * 原始文件名。
     */
    private String originalFilename;

    /**
     * 存储文件名。
     */
    private String storageFilename;

    /**
     * 文件大小，单位字节。
     */
    private Long size;

    /**
     * 文件扩展名。
     */
    private String extension;

    /**
     * 下载地址。
     */
    private String downloadUrl;

}
```

文件位置：`src/main/java/io/github/atengk/demo/service/FileStorageService.java`

以下 Service 负责保存上传文件，并返回文件下载信息。

```java
package io.github.atengk.demo.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.demo.common.exception.BizException;
import io.github.atengk.demo.config.properties.FileStorageProperties;
import io.github.atengk.demo.model.vo.FileUploadVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

/**
 * 文件存储业务组件。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageService {

    private final FileStorageProperties fileStorageProperties;

    /**
     * 上传文件。
     *
     * @param file 上传文件
     * @return 文件上传响应
     */
    public FileUploadVO upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException("上传文件不能为空");
        }

        String originalFilename = StrUtil.blankToDefault(file.getOriginalFilename(), "unknown");
        String extension = FileUtil.extName(originalFilename);
        if (StrUtil.isBlank(extension)) {
            throw new BizException("文件扩展名不能为空");
        }

        String lowerExtension = extension.toLowerCase();
        if (CollUtil.isNotEmpty(fileStorageProperties.getAllowedExtensions())
                && !fileStorageProperties.getAllowedExtensions().contains(lowerExtension)) {
            log.warn("文件类型不允许，原文件名：{}，扩展名：{}", originalFilename, lowerExtension);
            throw new BizException("不支持的文件类型");
        }

        String storageFilename = StrUtil.format("{}.{}", IdUtil.fastSimpleUUID(), lowerExtension);
        File targetFile = FileUtil.file(fileStorageProperties.getUploadPath(), storageFilename);
        FileUtil.mkParentDirs(targetFile);

        try {
            file.transferTo(targetFile);
        } catch (Exception e) {
            log.error("文件保存失败，原文件名：{}", originalFilename, e);
            throw new BizException("文件上传失败");
        }

        FileUploadVO vo = new FileUploadVO();
        vo.setOriginalFilename(originalFilename);
        vo.setStorageFilename(storageFilename);
        vo.setSize(file.getSize());
        vo.setExtension(lowerExtension);
        vo.setDownloadUrl(fileStorageProperties.getDownloadPrefix() + "/" + storageFilename);

        log.info("文件上传成功，原文件名：{}，存储文件名：{}，大小：{}", originalFilename, storageFilename, file.getSize());
        return vo;
    }

}
```

文件位置：`src/main/java/io/github/atengk/demo/controller/FileController.java`

以下 Controller 提供文件上传接口。

```java
package io.github.atengk.demo.controller;

import io.github.atengk.demo.common.response.ApiResult;
import io.github.atengk.demo.model.vo.FileUploadVO;
import io.github.atengk.demo.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件接口控制器。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/files")
public class FileController {

    private final FileStorageService fileStorageService;

    /**
     * 上传文件。
     *
     * @param file 上传文件
     * @return 上传结果
     */
    @PostMapping("/upload")
    public ApiResult<FileUploadVO> upload(@RequestParam("file") MultipartFile file) {
        return ApiResult.success(fileStorageService.upload(file));
    }

}
```

上传调用示例：

```bash
curl -X POST "http://localhost:8080/api/files/upload" \
  -F "file=@/Users/ateng/Desktop/test.pdf"
```

文件上传接口建议：

- 上传接口使用 `POST`。
- 请求格式使用 `multipart/form-data`。
- 文件参数名统一，例如 `file`。
- 不直接使用原始文件名作为存储文件名。
- 上传成功后返回文件 ID、存储文件名或下载地址。
- 文件元数据建议落库保存，便于权限控制、审计和后续清理。

### 文件大小限制

文件大小限制分为两层：Spring Boot multipart 限制和业务层限制。multipart 限制用于防止请求体过大，业务层限制用于针对不同业务类型做更细粒度控制。

文件位置：`src/main/resources/application.yml`

以下配置限制单个文件最大 20MB，单次请求最大 50MB。

```yaml
spring:
  servlet:
    multipart:
      # 单个文件最大大小
      max-file-size: 20MB
      # 单次 multipart 请求最大大小
      max-request-size: 50MB
```

如果上传文件超过限制，通常会抛出 `MaxUploadSizeExceededException` 或 multipart 解析异常。可以在全局异常处理器中统一返回提示。

文件位置：`src/main/java/io/github/atengk/demo/common/handler/GlobalExceptionHandler.java`

以下方法用于处理文件过大异常，可追加到已有全局异常处理器中。

```java
@ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
public ApiResult<Void> handleMaxUploadSizeExceededException(
        org.springframework.web.multipart.MaxUploadSizeExceededException e) {

    log.warn("上传文件超过大小限制：{}", e.getMessage());
    return ApiResult.fail(400, "上传文件超过大小限制");
}
```

如果希望在业务层按接口控制文件大小，可以在 Service 中增加判断。

文件位置：`src/main/java/io/github/atengk/demo/service/FileSizeCheckService.java`

以下 Service 按业务规则校验文件大小。

```java
package io.github.atengk.demo.service;

import io.github.atengk.demo.common.exception.BizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件大小校验业务组件。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Service
public class FileSizeCheckService {

    private static final long AVATAR_MAX_SIZE = 2 * 1024 * 1024L;

    private static final long DOCUMENT_MAX_SIZE = 20 * 1024 * 1024L;

    /**
     * 校验头像文件大小。
     *
     * @param file 上传文件
     */
    public void checkAvatarSize(MultipartFile file) {
        if (file.getSize() > AVATAR_MAX_SIZE) {
            log.warn("头像文件超过大小限制，文件大小：{}", file.getSize());
            throw new BizException("头像文件不能超过2MB");
        }
    }

    /**
     * 校验文档文件大小。
     *
     * @param file 上传文件
     */
    public void checkDocumentSize(MultipartFile file) {
        if (file.getSize() > DOCUMENT_MAX_SIZE) {
            log.warn("文档文件超过大小限制，文件大小：{}", file.getSize());
            throw new BizException("文档文件不能超过20MB");
        }
    }

}
```

文件大小限制建议：

- 使用 `spring.servlet.multipart.max-file-size` 限制单文件大小。
- 使用 `spring.servlet.multipart.max-request-size` 限制单次请求总大小。
- 不同业务类型可以在 Service 中设置不同大小限制。
- 超限异常应统一处理，避免返回框架默认错误页。
- 大文件上传建议使用分片上传或对象存储直传。
- 不建议让应用服务器承载无限制文件上传。

### 文件类型校验

文件类型校验不能只依赖前端限制，后端必须校验。基础项目可以先校验扩展名；安全要求较高时，应进一步校验 MIME 类型和文件头，防止伪造扩展名上传脚本或可执行文件。

常见校验层次：

| 校验方式      | 说明                       | 安全性 |
| ------------- | -------------------------- | ------ |
| 扩展名校验    | 校验 `.jpg`、`.pdf` 等后缀 | 基础   |
| MIME 类型校验 | 校验 `Content-Type`        | 中等   |
| 文件头校验    | 检查文件魔数               | 较高   |
| 内容扫描      | 病毒扫描、敏感内容扫描     | 高     |

文件位置：`src/main/java/io/github/atengk/demo/service/FileTypeCheckService.java`

以下 Service 使用 Hutool 校验扩展名，并结合 MIME 类型做基础判断。

```java
package io.github.atengk.demo.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.demo.common.exception.BizException;
import io.github.atengk.demo.config.properties.FileStorageProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件类型校验业务组件。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileTypeCheckService {

    private final FileStorageProperties fileStorageProperties;

    /**
     * 校验文件类型。
     *
     * @param file 上传文件
     * @return 文件扩展名
     */
    public String checkFileType(MultipartFile file) {
        String originalFilename = StrUtil.blankToDefault(file.getOriginalFilename(), "");
        String extension = FileUtil.extName(originalFilename);
        if (StrUtil.isBlank(extension)) {
            throw new BizException("文件扩展名不能为空");
        }

        String lowerExtension = extension.toLowerCase();
        if (CollUtil.isNotEmpty(fileStorageProperties.getAllowedExtensions())
                && !fileStorageProperties.getAllowedExtensions().contains(lowerExtension)) {
            log.warn("文件扩展名不允许，文件名：{}，扩展名：{}", originalFilename, lowerExtension);
            throw new BizException("不支持的文件类型");
        }

        String contentType = StrUtil.blankToDefault(file.getContentType(), "");
        if (StrUtil.isNotBlank(contentType)) {
            log.info("上传文件 MIME 类型，文件名：{}，contentType：{}", originalFilename, contentType);
        }

        return lowerExtension;
    }

}
```

图片文件可以进一步读取文件头做校验。以下示例只允许常见图片文件头。

文件位置：`src/main/java/io/github/atengk/demo/service/ImageFileCheckService.java`

```java
package io.github.atengk.demo.service;

import cn.hutool.core.util.HexUtil;
import io.github.atengk.demo.common.exception.BizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

/**
 * 图片文件头校验业务组件。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Service
public class ImageFileCheckService {

    private static final List<String> IMAGE_HEADERS = List.of(
            "FFD8FF",
            "89504E47",
            "47494638"
    );

    /**
     * 校验图片文件头。
     *
     * @param file 上传文件
     */
    public void checkImageHeader(MultipartFile file) {
        byte[] header = new byte[4];
        try (InputStream inputStream = file.getInputStream()) {
            int readSize = inputStream.read(header);
            if (readSize <= 0) {
                throw new BizException("文件内容为空");
            }
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("读取文件头失败", e);
            throw new BizException("文件类型校验失败");
        }

        String headerHex = HexUtil.encodeHexStr(header).toUpperCase();
        boolean matched = IMAGE_HEADERS.stream().anyMatch(headerHex::startsWith);
        if (!matched) {
            log.warn("图片文件头不匹配，header：{}", headerHex);
            throw new BizException("图片文件格式不正确");
        }
    }

}
```

文件类型校验建议：

- 后端必须校验扩展名。
- 不信任前端传入的文件名和 `Content-Type`。
- 图片、PDF 等重要类型建议校验文件头。
- 禁止上传脚本、可执行文件、压缩炸弹等高风险文件。
- 文件名入库和日志输出前应做长度控制和安全处理。
- 对外可访问文件应尽量存储到隔离目录或对象存储。

### 文件下载接口

文件下载接口用于将服务器文件返回给客户端。下载时需要设置 `Content-Disposition` 响应头，避免中文文件名乱码，并防止浏览器直接打开某些敏感类型文件。

文件位置：`src/main/java/io/github/atengk/demo/service/FileDownloadService.java`

以下 Service 负责根据存储文件名定位文件，并校验文件是否存在。

```java
package io.github.atengk.demo.service;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.demo.common.exception.BizException;
import io.github.atengk.demo.config.properties.FileStorageProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;

/**
 * 文件下载业务组件。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileDownloadService {

    private final FileStorageProperties fileStorageProperties;

    /**
     * 根据文件名获取下载文件。
     *
     * @param filename 文件名
     * @return 文件对象
     */
    public File getDownloadFile(String filename) {
        if (StrUtil.isBlank(filename)) {
            throw new BizException("文件名不能为空");
        }

        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            log.warn("非法文件名访问，filename：{}", filename);
            throw new BizException("文件名非法");
        }

        File file = FileUtil.file(fileStorageProperties.getUploadPath(), filename);
        if (!FileUtil.exist(file) || !FileUtil.isFile(file)) {
            log.warn("下载文件不存在，filename：{}", filename);
            throw new BizException(404, "文件不存在");
        }

        return file;
    }

}
```

文件位置：`src/main/java/io/github/atengk/demo/controller/FileDownloadController.java`

以下 Controller 使用 `ResponseEntity<Resource>` 返回下载文件。

```java
package io.github.atengk.demo.controller;

import cn.hutool.core.io.FileUtil;
import io.github.atengk.demo.service.FileDownloadService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.nio.charset.StandardCharsets;

/**
 * 文件下载接口控制器。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/files")
public class FileDownloadController {

    private final FileDownloadService fileDownloadService;

    /**
     * 下载文件。
     *
     * @param filename 文件名
     * @return 文件资源
     */
    @GetMapping("/download/{filename}")
    public ResponseEntity<Resource> download(@PathVariable String filename) {
        File file = fileDownloadService.getDownloadFile(filename);
        Resource resource = new FileSystemResource(file);

        ContentDisposition contentDisposition = ContentDisposition.attachment()
                .filename(file.getName(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(FileUtil.size(file))
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .body(resource);
    }

}
```

下载调用示例：

```bash
curl -O "http://localhost:8080/api/files/download/2f8e5d9a0d614b07a53f4f64cb8d0a91.pdf"
```

如果希望下载时使用原始文件名，建议把文件元数据保存到数据库中，下载时根据文件 ID 查询原始文件名和存储文件名。

推荐文件元数据表：

```sql
-- 文件元数据表，用于记录上传文件的基础信息
create table sys_file (
    id bigint primary key auto_increment comment '文件ID',
    original_filename varchar(255) not null comment '原始文件名',
    storage_filename varchar(255) not null comment '存储文件名',
    extension varchar(20) not null comment '文件扩展名',
    size bigint not null comment '文件大小',
    content_type varchar(100) null comment 'MIME类型',
    storage_path varchar(500) not null comment '存储路径',
    create_time datetime not null default current_timestamp comment '创建时间'
) comment '文件元数据表';
```

文件下载接口建议：

- 下载接口使用 `GET`。
- 不建议直接暴露服务器真实路径。
- 推荐通过文件 ID 下载，而不是通过完整路径下载。
- 响应头设置 `Content-Disposition`。
- 文件名需要防止目录穿越。
- 大文件下载应考虑分片、断点续传或对象存储临时链接。

### 文件访问安全控制

文件访问安全控制用于防止越权访问、路径穿越、恶意文件上传和敏感文件泄露。文件上传下载不应仅依赖路径可访问，还应结合用户身份、业务归属和文件权限判断。

常见风险：

| 风险     | 示例                    | 处理方式                           |
| -------- | ----------------------- | ---------------------------------- |
| 目录穿越 | `../../application.yml` | 禁止文件名包含路径符和 `..`        |
| 越权下载 | 下载他人文件            | 文件元数据绑定用户或业务 ID        |
| 恶意文件 | 上传脚本、木马          | 校验扩展名、MIME、文件头、病毒扫描 |
| 敏感泄露 | 直接暴露存储路径        | 下载接口代理输出                   |
| 文件覆盖 | 原始文件名重复          | 使用 UUID 或雪花 ID 存储文件       |

文件访问权限可以在 Service 中统一判断。

文件位置：`src/main/java/io/github/atengk/demo/service/FileAccessService.java`

以下 Service 演示按用户 ID 判断文件访问权限。

```java
package io.github.atengk.demo.service;

import io.github.atengk.demo.common.exception.BizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 文件访问权限业务组件。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Service
public class FileAccessService {

    /**
     * 校验文件访问权限。
     *
     * @param currentUserId 当前用户 ID
     * @param fileOwnerId 文件归属用户 ID
     */
    public void checkDownloadPermission(Long currentUserId, Long fileOwnerId) {
        if (currentUserId == null) {
            throw new BizException(401, "用户未登录");
        }

        if (!currentUserId.equals(fileOwnerId)) {
            log.warn("文件下载越权，当前用户ID：{}，文件归属用户ID：{}", currentUserId, fileOwnerId);
            throw new BizException(403, "无文件访问权限");
        }
    }

}
```

文件访问安全建议：

- 文件上传后保存元数据，包括上传人、业务 ID、文件类型、大小、存储名。
- 下载时根据文件 ID 查询元数据，不直接使用路径参数定位任意文件。
- 文件权限校验放在 Service 层。
- 不把上传目录配置到静态资源目录下直接公开。
- 图片预览和文件下载也要区分权限。
- 生产环境优先使用对象存储，并通过临时签名链接或后端代理控制访问。
- 对敏感文件增加审计日志，记录下载人、文件 ID、业务 ID 和时间。

## 拦截器与过滤器

拦截器和过滤器都可以处理请求链路，但所在层次不同。Filter 属于 Servlet 规范，执行时机更靠前；HandlerInterceptor 属于 Spring MVC，主要围绕 Controller 请求处理。实际项目中，通用链路处理、编码、跨域、安全框架常使用 Filter；登录校验、接口权限、请求日志、业务上下文通常可以使用 Interceptor。

### HandlerInterceptor 使用

`HandlerInterceptor` 用于在请求进入 Controller 前后执行逻辑，适合处理登录校验、请求上下文、接口权限、操作日志等 MVC 层相关能力。

执行顺序：

1. `preHandle`：Controller 执行前。
2. Controller 方法执行。
3. `postHandle`：Controller 执行后、视图渲染前。
4. `afterCompletion`：请求完成后，无论是否异常通常都会执行。

文件位置：`src/main/java/io/github/atengk/demo/common/context/UserContext.java`

以下上下文类使用 `ThreadLocal` 保存当前请求用户信息。

```java
package io.github.atengk.demo.common.context;

/**
 * 用户上下文。
 *
 * @author Ateng
 * @since 2026-05-09
 */
public class UserContext {

    private static final ThreadLocal<Long> USER_ID_HOLDER = new ThreadLocal<>();

    /**
     * 设置当前用户 ID。
     *
     * @param userId 用户 ID
     */
    public static void setUserId(Long userId) {
        USER_ID_HOLDER.set(userId);
    }

    /**
     * 获取当前用户 ID。
     *
     * @return 用户 ID
     */
    public static Long getUserId() {
        return USER_ID_HOLDER.get();
    }

    /**
     * 清理当前用户 ID。
     */
    public static void clear() {
        USER_ID_HOLDER.remove();
    }

}
```

文件位置：`src/main/java/io/github/atengk/demo/interceptor/UserContextInterceptor.java`

以下拦截器从请求头读取用户 ID，并放入用户上下文。

```java
package io.github.atengk.demo.interceptor;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.demo.common.context.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 用户上下文拦截器。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Component
public class UserContextInterceptor implements HandlerInterceptor {

    /**
     * 请求进入 Controller 前执行。
     *
     * @param request 请求对象
     * @param response 响应对象
     * @param handler 处理器
     * @return 是否继续执行
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String userIdText = request.getHeader("X-User-Id");
        if (StrUtil.isNotBlank(userIdText) && NumberUtil.isLong(userIdText)) {
            Long userId = Long.valueOf(userIdText);
            UserContext.setUserId(userId);
            log.info("设置用户上下文，用户ID：{}", userId);
        }
        return true;
    }

    /**
     * 请求完成后执行。
     *
     * @param request 请求对象
     * @param response 响应对象
     * @param handler 处理器
     * @param ex 异常对象
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
        log.info("清理用户上下文");
    }

}
```

文件位置：`src/main/java/io/github/atengk/demo/config/WebMvcConfig.java`

以下配置类注册用户上下文拦截器。

```java
package io.github.atengk.demo.config;

import io.github.atengk.demo.interceptor.UserContextInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC 配置。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final UserContextInterceptor userContextInterceptor;

    /**
     * 注册拦截器。
     *
     * @param registry 拦截器注册器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(userContextInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/login", "/api/files/download/**");
    }

}
```

拦截器使用建议：

- 适合处理 Controller 前后的业务链路逻辑。
- `preHandle` 返回 `false` 会中断请求。
- 使用 `ThreadLocal` 后必须在 `afterCompletion` 清理。
- 静态资源、登录接口、健康检查接口通常需要排除。
- 不建议在拦截器中写复杂业务逻辑。

### Filter 使用

Filter 是 Servlet 层过滤器，执行时机早于 Spring MVC 拦截器。它适合处理请求编码、请求包装、响应包装、链路 ID、基础安全头、跨域、安全框架入口等能力。

文件位置：`src/main/java/io/github/atengk/demo/filter/TraceIdFilter.java`

以下 Filter 为每个请求设置 TraceId，并写入响应头。

```java
package io.github.atengk.demo.filter;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 请求链路 ID 过滤器。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Component
public class TraceIdFilter extends OncePerRequestFilter {

    private static final String TRACE_ID = "traceId";

    private static final String TRACE_HEADER = "X-Trace-Id";

    /**
     * 处理每次请求。
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
        String traceId = request.getHeader(TRACE_HEADER);
        if (StrUtil.isBlank(traceId)) {
            traceId = IdUtil.fastSimpleUUID();
        }

        MDC.put(TRACE_ID, traceId);
        response.setHeader(TRACE_HEADER, traceId);

        try {
            log.info("请求开始，traceId：{}，uri：{}", traceId, request.getRequestURI());
            filterChain.doFilter(request, response);
        } finally {
            log.info("请求结束，traceId：{}，uri：{}", traceId, request.getRequestURI());
            MDC.remove(TRACE_ID);
        }
    }

}
```

如果需要指定 Filter 顺序，可以使用 `FilterRegistrationBean`。

文件位置：`src/main/java/io/github/atengk/demo/config/FilterConfig.java`

以下配置类注册 TraceIdFilter 并指定执行顺序。

```java
package io.github.atengk.demo.config;

import io.github.atengk.demo.filter.TraceIdFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Filter 注册配置。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Configuration
@RequiredArgsConstructor
public class FilterConfig {

    private final TraceIdFilter traceIdFilter;

    /**
     * 注册 TraceId 过滤器。
     *
     * @return 过滤器注册对象
     */
    @Bean
    public FilterRegistrationBean<TraceIdFilter> traceIdFilterRegistrationBean() {
        FilterRegistrationBean<TraceIdFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(traceIdFilter);
        registrationBean.addUrlPatterns("/*");
        registrationBean.setName("traceIdFilter");
        registrationBean.setOrder(1);
        return registrationBean;
    }

}
```

Filter 使用建议：

- 通用底层处理使用 Filter。
- 每个请求只执行一次的过滤器优先继承 `OncePerRequestFilter`。
- Filter 中应调用 `filterChain.doFilter(request, response)` 放行请求。
- 使用 MDC 后必须清理，避免线程复用导致日志串号。
- Filter 顺序需要明确，安全、跨域、日志等过滤器顺序会影响行为。
- 不建议在 Filter 中读取请求体后不重写包装，否则 Controller 可能无法再次读取。

### 请求前置处理

请求前置处理通常在 Filter 或 Interceptor 的前置阶段完成。常见内容包括 TraceId 设置、用户上下文初始化、接口权限校验、参数预处理、请求开始时间记录等。

使用 Interceptor 记录请求开始时间：

文件位置：`src/main/java/io/github/atengk/demo/interceptor/RequestTimeInterceptor.java`

以下拦截器在请求开始时记录时间戳，请求结束后计算耗时。

```java
package io.github.atengk.demo.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 请求耗时拦截器。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Component
public class RequestTimeInterceptor implements HandlerInterceptor {

    private static final String START_TIME = "requestStartTime";

    /**
     * 请求前置处理。
     *
     * @param request 请求对象
     * @param response 响应对象
     * @param handler 处理器
     * @return 是否继续执行
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(START_TIME, System.currentTimeMillis());
        log.info("请求前置处理，method：{}，uri：{}", request.getMethod(), request.getRequestURI());
        return true;
    }

    /**
     * 请求完成处理。
     *
     * @param request 请求对象
     * @param response 响应对象
     * @param handler 处理器
     * @param ex 异常对象
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        Object startTime = request.getAttribute(START_TIME);
        if (startTime instanceof Long start) {
            long cost = System.currentTimeMillis() - start;
            log.info("请求完成，method：{}，uri：{}，status：{}，耗时：{}ms",
                    request.getMethod(), request.getRequestURI(), response.getStatus(), cost);
        }
    }

}
```

注册拦截器：

```java
@Override
public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(requestTimeInterceptor)
            .addPathPatterns("/api/**");
}
```

请求前置处理建议：

- 只放轻量逻辑，避免阻塞请求。
- 权限校验失败应快速返回。
- 前置处理中的上下文数据必须在请求结束后清理。
- 不建议在前置处理中执行复杂查询。
- 请求开始日志应避免记录敏感参数。

### 请求后置处理

请求后置处理用于在 Controller 执行后记录状态、清理上下文、统计耗时、处理审计日志等。后置处理可以放在 `postHandle` 或 `afterCompletion` 中。一般资源清理和耗时统计建议使用 `afterCompletion`，因为它在异常场景下也会执行。

`postHandle` 示例：

```java
@Override
public void postHandle(HttpServletRequest request,
                       HttpServletResponse response,
                       Object handler,
                       ModelAndView modelAndView) {
    log.info("Controller 执行完成，uri：{}，status：{}", request.getRequestURI(), response.getStatus());
}
```

`afterCompletion` 示例：

```java
@Override
public void afterCompletion(HttpServletRequest request,
                            HttpServletResponse response,
                            Object handler,
                            Exception ex) {
    if (ex != null) {
        log.warn("请求处理出现异常，uri：{}，异常：{}", request.getRequestURI(), ex.getMessage());
    }
    UserContext.clear();
}
```

请求后置处理建议：

- 清理 `ThreadLocal` 必须放在 `afterCompletion`。
- 统计耗时建议在 `afterCompletion` 中完成。
- 异常信息记录要简洁，完整堆栈交给全局异常处理器。
- 后置处理不要修改已经提交的响应。
- 审计日志如果需要独立落库，应考虑异步或独立事务。

### 接口访问控制

接口访问控制用于判断当前请求是否允许访问目标接口。简单项目可以在拦截器中基于 Token、用户 ID、角色、白名单路径做基础控制；复杂项目建议接入 Spring Security、Sa-Token 或网关统一鉴权。

文件位置：`src/main/resources/application.yml`

以下配置定义白名单路径。

```yaml
demo:
  security:
    # 不需要登录的接口路径
    whitelist:
      - /api/login
      - /api/captcha
      - /actuator/health
      - /api/files/download/**
```

文件位置：`src/main/java/io/github/atengk/demo/config/properties/SecurityProperties.java`

以下配置属性类绑定安全白名单。

```java
package io.github.atengk.demo.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 安全配置属性。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
@ConfigurationProperties(prefix = "demo.security")
public class SecurityProperties {

    /**
     * 白名单路径。
     */
    private List<String> whitelist = new ArrayList<>();

}
```

文件位置：`src/main/java/io/github/atengk/demo/interceptor/AuthInterceptor.java`

以下拦截器通过请求头 `Authorization` 做基础访问控制。

```java
package io.github.atengk.demo.interceptor;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.demo.common.response.ApiResult;
import io.github.atengk.demo.common.response.ResultCodeEnum;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;

/**
 * 接口认证拦截器。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final ObjectMapper objectMapper;

    /**
     * 创建接口认证拦截器。
     *
     * @param objectMapper JSON 序列化对象
     */
    public AuthInterceptor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 请求认证前置处理。
     *
     * @param request 请求对象
     * @param response 响应对象
     * @param handler 处理器
     * @return 是否继续执行
     * @throws Exception 处理异常
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String authorization = request.getHeader("Authorization");
        if (StrUtil.isBlank(authorization)) {
            log.warn("请求未携带认证信息，uri：{}", request.getRequestURI());

            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write(objectMapper.writeValueAsString(ApiResult.fail(ResultCodeEnum.UNAUTHORIZED)));
            return false;
        }

        return true;
    }

}
```

注册认证拦截器时配置白名单：

文件位置：`src/main/java/io/github/atengk/demo/config/WebMvcConfig.java`

```java
package io.github.atengk.demo.config;

import io.github.atengk.demo.config.properties.SecurityProperties;
import io.github.atengk.demo.interceptor.AuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC 配置。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    private final SecurityProperties securityProperties;

    /**
     * 注册接口认证拦截器。
     *
     * @param registry 拦截器注册器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(securityProperties.getWhitelist());
    }

}
```

接口访问控制建议：

- 登录、验证码、健康检查等接口加入白名单。
- 认证失败返回 `401`。
- 权限不足返回 `403`。
- 不要只在前端控制菜单权限，后端必须做接口权限校验。
- 简单项目可以使用拦截器，复杂项目建议使用成熟安全框架。
- Token 校验、用户上下文、权限判断应明确分层，避免全部堆在拦截器中。

## 跨域处理

跨域处理用于解决浏览器同源策略导致的前端访问限制问题。当前端页面和后端 API 不在同一个协议、域名或端口下时，浏览器会触发跨域限制。Spring Boot 可以通过全局 CORS 配置、局部 `@CrossOrigin` 注解或网关/Nginx 统一处理跨域。

### 全局跨域配置

全局跨域配置适合前后端分离项目，可以统一设置允许的前端域名、请求方法、请求头和凭证策略。生产环境不建议使用 `*` 放开所有来源，应明确配置允许访问的前端域名。

文件位置：`src/main/resources/application.yml`

以下配置定义允许跨域访问的前端来源。

```yaml
demo:
  cors:
    # 允许跨域访问的前端域名
    allowed-origins:
      - http://localhost:5173
      - http://localhost:3000
      - https://admin.example.com
    # 是否允许携带 Cookie 或认证信息
    allow-credentials: true
```

文件位置：`src/main/java/io/github/atengk/demo/config/properties/CorsProperties.java`

以下配置属性类用于绑定跨域配置。

```java
package io.github.atengk.demo.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 跨域配置属性。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
@ConfigurationProperties(prefix = "demo.cors")
public class CorsProperties {

    /**
     * 允许跨域访问的来源。
     */
    private List<String> allowedOrigins = new ArrayList<>();

    /**
     * 是否允许携带凭证。
     */
    private Boolean allowCredentials = true;

}
```

文件位置：`src/main/java/io/github/atengk/demo/config/CorsConfig.java`

以下配置类通过 `WebMvcConfigurer` 设置全局跨域规则。

```java
package io.github.atengk.demo.config;

import io.github.atengk.demo.config.properties.CorsProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 全局跨域配置。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Configuration
@RequiredArgsConstructor
public class CorsConfig implements WebMvcConfigurer {

    private final CorsProperties corsProperties;

    /**
     * 配置跨域规则。
     *
     * @param registry 跨域注册器
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(corsProperties.getAllowedOrigins().toArray(String[]::new))
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("X-Trace-Id")
                .allowCredentials(Boolean.TRUE.equals(corsProperties.getAllowCredentials()))
                .maxAge(3600);
    }

}
```

全局跨域配置建议：

- 只对 `/api/**` 等接口路径开放跨域。
- 生产环境明确写允许的域名。
- 允许携带 Cookie 或认证信息时，不要使用 `allowedOrigins("*")`。
- 常用请求方法包含 `GET`、`POST`、`PUT`、`PATCH`、`DELETE`、`OPTIONS`。
- 如果前端需要读取自定义响应头，应配置 `exposedHeaders`。
- 跨域问题优先在网关或 Nginx 统一处理，应用内配置作为补充。

### 局部跨域配置

局部跨域配置适合只开放少量接口给指定前端来源。可以在 Controller 类或方法上使用 `@CrossOrigin`。相比全局配置，局部配置更细，但如果大量接口都需要跨域，会导致配置分散。

文件位置：`src/main/java/io/github/atengk/demo/controller/OpenApiController.java`

以下 Controller 只允许指定来源访问当前开放接口。

```java
package io.github.atengk.demo.controller;

import cn.hutool.core.map.MapUtil;
import io.github.atengk.demo.common.response.ApiResult;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 开放接口控制器。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@RestController
@RequestMapping("/api/open")
@CrossOrigin(
        origins = {"https://admin.example.com"},
        methods = {RequestMethod.GET, RequestMethod.POST},
        allowedHeaders = "*",
        exposedHeaders = {"X-Trace-Id"},
        allowCredentials = "true",
        maxAge = 3600
)
public class OpenApiController {

    /**
     * 获取公开配置。
     *
     * @return 公开配置
     */
    @GetMapping("/config")
    public ApiResult<Map<String, Object>> config() {
        return ApiResult.success(MapUtil.<String, Object>builder()
                .put("name", "spring-boot3-demo")
                .put("version", "1.0.0")
                .build());
    }

}
```

也可以只在单个接口方法上配置：

```java
@CrossOrigin(origins = "https://admin.example.com")
@GetMapping("/public-info")
public ApiResult<String> publicInfo() {
    return ApiResult.success("public-info");
}
```

局部跨域配置建议：

- 少量开放接口可以使用 `@CrossOrigin`。
- 大量接口需要跨域时使用全局配置。
- 不建议全局和局部规则混乱叠加。
- 对外开放接口应单独评估认证、限流和审计。
- 局部跨域也不能代替接口权限控制。

### 预检请求处理

预检请求是浏览器在正式跨域请求前发送的 `OPTIONS` 请求，用于确认服务端是否允许后续请求。当前端发送自定义请求头、非简单请求方法或携带凭证时，浏览器通常会先发起预检请求。

常见预检请求示例：

```bash
curl -X OPTIONS "http://localhost:8080/api/users" \
  -H "Origin: http://localhost:5173" \
  -H "Access-Control-Request-Method: POST" \
  -H "Access-Control-Request-Headers: Content-Type,Authorization"
```

如果跨域配置正确，响应头中应包含类似信息：

```text
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE,OPTIONS
Access-Control-Allow-Headers: Content-Type,Authorization
Access-Control-Allow-Credentials: true
Access-Control-Max-Age: 3600
```

如果项目使用自定义认证拦截器，需要注意不要拦截预检请求。否则浏览器会在正式请求前就失败。

文件位置：`src/main/java/io/github/atengk/demo/interceptor/AuthInterceptor.java`

以下认证拦截器对 `OPTIONS` 预检请求直接放行。

```java
package io.github.atengk.demo.interceptor;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.atengk.demo.common.response.ApiResult;
import io.github.atengk.demo.common.response.ResultCodeEnum;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;

/**
 * 接口认证拦截器。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final ObjectMapper objectMapper;

    /**
     * 创建接口认证拦截器。
     *
     * @param objectMapper JSON 序列化对象
     */
    public AuthInterceptor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 请求认证前置处理。
     *
     * @param request 请求对象
     * @param response 响应对象
     * @param handler 处理器
     * @return 是否继续执行
     * @throws Exception 处理异常
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }

        String authorization = request.getHeader("Authorization");
        if (StrUtil.isBlank(authorization)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write(objectMapper.writeValueAsString(ApiResult.fail(ResultCodeEnum.UNAUTHORIZED)));
            return false;
        }

        return true;
    }

}
```

预检请求处理建议：

- 跨域配置中必须允许 `OPTIONS` 方法。
- 自定义认证拦截器应放行预检请求。
- 网关、Nginx、后端不要重复返回冲突的 CORS 响应头。
- 前端携带 `Authorization` 时，后端需要允许该请求头。
- `maxAge` 可以减少浏览器预检请求频率。

### 跨域安全边界

跨域配置只解决浏览器是否允许前端页面调用后端接口的问题，不等于接口安全。即使限制了 CORS，非浏览器客户端仍然可以直接调用接口。因此接口认证、权限校验、签名校验、限流和审计仍然必须在后端完成。

跨域安全边界建议：

- CORS 不是认证机制，不能代替 Token 校验。
- 生产环境不要使用任意来源跨域。
- 允许携带凭证时，必须明确指定可信来源。
- 不要为了方便联调长期开放 `*`。
- 内部接口不应因为配置了 CORS 就暴露到公网。
- 对开放接口增加认证、限流、审计和异常监控。
- 网关层、Nginx 层、应用层的 CORS 配置保持一致，避免重复或冲突。

不推荐的生产配置：

```java
registry.addMapping("/**")
        .allowedOrigins("*")
        .allowedMethods("*")
        .allowedHeaders("*");
```

推荐的生产配置：

```java
registry.addMapping("/api/**")
        .allowedOrigins("https://admin.example.com")
        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        .allowedHeaders("Content-Type", "Authorization", "X-Trace-Id")
        .exposedHeaders("X-Trace-Id")
        .allowCredentials(true)
        .maxAge(3600);
```

跨域排查顺序：

1. 确认浏览器控制台中的错误信息。
2. 确认请求是否发送了 `Origin`。
3. 确认后端是否返回 `Access-Control-Allow-Origin`。
4. 确认预检请求是否被认证拦截器拦截。
5. 确认 `Authorization`、`Content-Type` 等请求头是否被允许。
6. 确认是否同时在 Nginx 和应用中重复配置 CORS。
7. 确认前端请求地址、协议、域名、端口是否与允许来源一致。



## 请求上下文处理

请求上下文处理用于在一次 HTTP 请求链路中传递用户 ID、TraceId、客户端信息、请求来源、语言标识等数据。合理的上下文设计可以减少方法参数层层传递，提高日志排查效率，也方便后续做认证、审计、限流、数据权限和链路追踪。

### HttpServletRequest 使用

`HttpServletRequest` 是 Servlet 规范中的请求对象，可以获取请求路径、请求方法、请求参数、请求头、客户端地址、Cookie、Session 等信息。在 Spring Boot 3 中，Servlet 相关包使用 `jakarta.servlet.*` 命名空间。

Controller 中可以直接注入 `HttpServletRequest`，但不建议在业务 Service 中大量传递它。Service 层应优先接收业务参数或使用专门的上下文对象。

文件位置：`src/main/java/io/github/atengk/demo/controller/RequestInfoController.java`

以下接口用于演示如何通过 `HttpServletRequest` 获取基础请求信息。

```java
package io.github.atengk.demo.controller;

import cn.hutool.core.map.MapUtil;
import io.github.atengk.demo.common.response.ApiResult;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 请求信息接口。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@RestController
public class RequestInfoController {

    @GetMapping("/api/request/info")
    public ApiResult<Map<String, Object>> info(HttpServletRequest request) {
        Map<String, Object> data = MapUtil.<String, Object>builder()
                .put("method", request.getMethod())
                .put("requestUri", request.getRequestURI())
                .put("queryString", request.getQueryString())
                .put("contextPath", request.getContextPath())
                .put("remoteAddr", request.getRemoteAddr())
                .put("userAgent", request.getHeader("User-Agent"))
                .build();

        return ApiResult.success(data);
    }

}
```

调用示例：

```bash
curl "http://localhost:8080/api/request/info?keyword=ateng" \
  -H "User-Agent: Mozilla/5.0"
```

`HttpServletRequest` 使用建议：

- Controller 层可以直接使用 `HttpServletRequest`。
- Service 层不建议依赖 Servlet API，避免业务层和 Web 层强耦合。
- 常用请求信息应提取到上下文对象中。
- 不要在多线程异步任务中直接使用当前请求对象。
- 获取请求体时要谨慎，普通请求体默认只能读取一次。

### 请求头信息获取

请求头用于传递请求元数据，例如认证 Token、TraceId、客户端类型、语言、版本号、租户 ID 等。请求头适合放元信息，不适合放复杂业务数据。

常见请求头约定：

| 请求头            | 说明                                  |
| ----------------- | ------------------------------------- |
| `Authorization`   | 登录认证信息                          |
| `X-Trace-Id`      | 请求链路 ID                           |
| `X-User-Id`       | 当前用户 ID，通常由网关或认证服务写入 |
| `X-Tenant-Id`     | 租户 ID                               |
| `X-Client-Type`   | 客户端类型，例如 web、ios、android    |
| `X-App-Version`   | 应用版本                              |
| `Accept-Language` | 语言标识                              |

文件位置：`src/main/java/io/github/atengk/demo/controller/HeaderInfoController.java`

以下接口演示通过 `@RequestHeader` 获取请求头。

```java
package io.github.atengk.demo.controller;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.demo.common.response.ApiResult;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 请求头信息接口。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@RestController
@RequestMapping("/api/header-info")
public class HeaderInfoController {

    @GetMapping
    public ApiResult<Map<String, Object>> headerInfo(
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId,
            @RequestHeader(value = "X-Client-Type", required = false) String clientType,
            @RequestHeader(value = "X-App-Version", required = false) String appVersion,
            @RequestHeader(value = "Accept-Language", required = false) String language) {

        Map<String, Object> data = MapUtil.<String, Object>builder()
                .put("traceId", StrUtil.blankToDefault(traceId, "unknown"))
                .put("clientType", StrUtil.blankToDefault(clientType, "unknown"))
                .put("appVersion", StrUtil.blankToDefault(appVersion, "unknown"))
                .put("language", StrUtil.blankToDefault(language, "zh-CN"))
                .build();

        return ApiResult.success(data);
    }

}
```

也可以封装请求头工具类，减少业务代码重复读取。

文件位置：`src/main/java/io/github/atengk/demo/common/util/RequestHeaderUtil.java`

以下工具类用于从请求中安全获取常用请求头。

```java
package io.github.atengk.demo.common.util;

import cn.hutool.core.util.StrUtil;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 请求头工具类。
 *
 * @author Ateng
 * @since 2026-05-09
 */
public class RequestHeaderUtil {

    private RequestHeaderUtil() {
    }

    public static String getTraceId(HttpServletRequest request) {
        return StrUtil.blankToDefault(request.getHeader("X-Trace-Id"), "unknown");
    }

    public static String getClientType(HttpServletRequest request) {
        return StrUtil.blankToDefault(request.getHeader("X-Client-Type"), "unknown");
    }

    public static String getAppVersion(HttpServletRequest request) {
        return StrUtil.blankToDefault(request.getHeader("X-App-Version"), "unknown");
    }

    public static String getLanguage(HttpServletRequest request) {
        return StrUtil.blankToDefault(request.getHeader("Accept-Language"), "zh-CN");
    }

}
```

请求头处理建议：

- 认证信息统一使用 `Authorization`。
- 链路 ID 使用统一请求头，例如 `X-Trace-Id`。
- 客户端类型和版本号由前端或网关注入。
- 敏感请求头不要完整输出到日志。
- 不要信任普通客户端直接传入的用户 ID，正式项目应由认证服务解析 Token 后写入。

### 客户端信息获取

客户端信息通常包括 IP 地址、User-Agent、客户端类型、App 版本、语言、设备信息等。它们常用于日志审计、风控、灰度发布、问题定位和统计分析。

在经过 Nginx、网关或负载均衡后，`request.getRemoteAddr()` 可能只能获取到代理服务器地址。因此获取真实客户端 IP 时，需要按顺序读取常见代理请求头，并结合可信代理策略处理。

文件位置：`src/main/java/io/github/atengk/demo/common/util/ClientInfoUtil.java`

以下工具类用于获取客户端 IP 和 User-Agent。

```java
package io.github.atengk.demo.common.util;

import cn.hutool.core.util.StrUtil;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
 * 客户端信息工具类。
 *
 * @author Ateng
 * @since 2026-05-09
 */
public class ClientInfoUtil {

    private static final List<String> IP_HEADERS = List.of(
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP"
    );

    private ClientInfoUtil() {
    }

    public static String getClientIp(HttpServletRequest request) {
        for (String header : IP_HEADERS) {
            String value = request.getHeader(header);
            if (StrUtil.isNotBlank(value) && !"unknown".equalsIgnoreCase(value)) {
                return StrUtil.split(value, ',').get(0).trim();
            }
        }
        return request.getRemoteAddr();
    }

    public static String getUserAgent(HttpServletRequest request) {
        return StrUtil.blankToDefault(request.getHeader("User-Agent"), "unknown");
    }

    public static String getClientType(HttpServletRequest request) {
        return StrUtil.blankToDefault(request.getHeader("X-Client-Type"), "unknown");
    }

    public static String getAppVersion(HttpServletRequest request) {
        return StrUtil.blankToDefault(request.getHeader("X-App-Version"), "unknown");
    }

}
```

文件位置：`src/main/java/io/github/atengk/demo/controller/ClientInfoController.java`

以下接口返回当前请求的客户端信息。

```java
package io.github.atengk.demo.controller;

import cn.hutool.core.map.MapUtil;
import io.github.atengk.demo.common.response.ApiResult;
import io.github.atengk.demo.common.util.ClientInfoUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 客户端信息接口。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@RestController
public class ClientInfoController {

    @GetMapping("/api/client/info")
    public ApiResult<Map<String, Object>> clientInfo(HttpServletRequest request) {
        Map<String, Object> data = MapUtil.<String, Object>builder()
                .put("clientIp", ClientInfoUtil.getClientIp(request))
                .put("userAgent", ClientInfoUtil.getUserAgent(request))
                .put("clientType", ClientInfoUtil.getClientType(request))
                .put("appVersion", ClientInfoUtil.getAppVersion(request))
                .build();

        return ApiResult.success(data);
    }

}
```

客户端信息获取建议：

- 经过代理时优先读取 `X-Forwarded-For`、`X-Real-IP`。
- 只有可信网关写入的 IP 请求头才可作为安全依据。
- 普通客户端可伪造请求头，不能仅凭 Header 做安全判断。
- User-Agent 只适合做辅助分析，不适合作为强校验依据。
- 日志记录 IP 时注意合规要求，必要时做脱敏或访问控制。

### ThreadLocal 上下文

`ThreadLocal` 可以在一次请求线程内保存上下文数据，例如当前用户 ID、TraceId、租户 ID、客户端 IP 等。它适合解决“Controller、Service、Repository 多层调用都需要当前请求信息”的问题，避免方法参数层层传递。

但 `ThreadLocal` 必须在请求结束后清理。Web 容器线程会复用，如果不清理，可能出现用户信息串号、日志串号、租户数据污染等严重问题。

文件位置：`src/main/java/io/github/atengk/demo/common/context/RequestContext.java`

以下上下文对象保存当前请求的基础链路数据。

```java
package io.github.atengk.demo.common.context;

import lombok.Data;

/**
 * 请求上下文对象。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
public class RequestContext {

    private String traceId;

    private Long userId;

    private String tenantId;

    private String clientIp;

    private String clientType;

    private String appVersion;

}
```

文件位置：`src/main/java/io/github/atengk/demo/common/context/RequestContextHolder.java`

以下上下文持有器使用 `ThreadLocal` 保存和清理请求上下文。

```java
package io.github.atengk.demo.common.context;

/**
 * 请求上下文持有器。
 *
 * @author Ateng
 * @since 2026-05-09
 */
public class RequestContextHolder {

    private static final ThreadLocal<RequestContext> CONTEXT_HOLDER = new ThreadLocal<>();

    private RequestContextHolder() {
    }

    public static void set(RequestContext context) {
        CONTEXT_HOLDER.set(context);
    }

    public static RequestContext get() {
        return CONTEXT_HOLDER.get();
    }

    public static String getTraceId() {
        RequestContext context = get();
        return context == null ? null : context.getTraceId();
    }

    public static Long getUserId() {
        RequestContext context = get();
        return context == null ? null : context.getUserId();
    }

    public static String getTenantId() {
        RequestContext context = get();
        return context == null ? null : context.getTenantId();
    }

    public static void clear() {
        CONTEXT_HOLDER.remove();
    }

}
```

文件位置：`src/main/java/io/github/atengk/demo/filter/RequestContextFilter.java`

以下 Filter 在请求开始时创建上下文，在请求结束时清理上下文。

```java
package io.github.atengk.demo.filter;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.demo.common.context.RequestContext;
import io.github.atengk.demo.common.context.RequestContextHolder;
import io.github.atengk.demo.common.util.ClientInfoUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 请求上下文过滤器。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Component
public class RequestContextFilter extends OncePerRequestFilter {

    private static final String TRACE_ID = "traceId";

    private static final String TRACE_HEADER = "X-Trace-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String traceId = StrUtil.blankToDefault(request.getHeader(TRACE_HEADER), IdUtil.fastSimpleUUID());

        RequestContext context = new RequestContext();
        context.setTraceId(traceId);
        context.setTenantId(request.getHeader("X-Tenant-Id"));
        context.setClientIp(ClientInfoUtil.getClientIp(request));
        context.setClientType(ClientInfoUtil.getClientType(request));
        context.setAppVersion(ClientInfoUtil.getAppVersion(request));

        String userIdText = request.getHeader("X-User-Id");
        if (NumberUtil.isLong(userIdText)) {
            context.setUserId(Long.valueOf(userIdText));
        }

        RequestContextHolder.set(context);
        MDC.put(TRACE_ID, traceId);
        response.setHeader(TRACE_HEADER, traceId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            RequestContextHolder.clear();
            MDC.remove(TRACE_ID);
        }
    }

}
```

ThreadLocal 上下文建议：

- 只保存请求级别的轻量数据。
- 不保存大对象、请求体、响应体、数据库连接等资源。
- 必须在 `finally` 或 `afterCompletion` 中清理。
- 异步线程无法自动继承普通 `ThreadLocal`。
- 多租户、用户身份等关键上下文应来自可信认证链路，不直接信任客户端。

### 请求链路数据传递

请求链路数据传递用于在接口调用链路中保持 TraceId、用户信息、租户信息等上下文一致。常见链路包括：浏览器到后端、后端到后端、后端到第三方服务、业务日志、异常日志、审计日志。

最常用的是 TraceId 传递。入口请求没有 TraceId 时由后端生成，后续日志和响应头都带上该 TraceId。调用下游服务时，也应继续传递该 TraceId。

文件位置：`src/main/java/io/github/atengk/demo/config/RestClientConfig.java`

以下配置类创建 `RestClient`，并在请求下游接口时自动传递 TraceId。

```java
package io.github.atengk.demo.config;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.demo.common.context.RequestContextHolder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * RestClient 配置。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Configuration
public class RestClientConfig {

    @Bean
    public RestClient restClient() {
        return RestClient.builder()
                .requestInterceptor((request, body, execution) -> {
                    String traceId = RequestContextHolder.getTraceId();
                    if (StrUtil.isNotBlank(traceId)) {
                        request.getHeaders().add("X-Trace-Id", traceId);
                    }
                    return execution.execute(request, body);
                })
                .build();
    }

}
```

文件位置：`src/main/java/io/github/atengk/demo/service/RemoteUserService.java`

以下 Service 使用 `RestClient` 调用下游服务，请求头会自动携带 TraceId。

```java
package io.github.atengk.demo.service;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.demo.common.context.RequestContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * 远程用户业务组件。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RemoteUserService {

    private final RestClient restClient;

    public String getRemoteUser(Long userId) {
        String traceId = StrUtil.blankToDefault(RequestContextHolder.getTraceId(), "unknown");
        log.info("调用远程用户接口，traceId：{}，用户ID：{}", traceId, userId);

        return restClient.get()
                .uri("https://api.example.com/users/{userId}", userId)
                .retrieve()
                .body(String.class);
    }

}
```

请求链路数据传递建议：

- TraceId 应在入口生成或接收，并写入 MDC。
- 响应头中返回 TraceId，方便前端反馈问题。
- 调用下游服务时继续传递 TraceId。
- 日志格式中输出 TraceId。
- 业务审计日志中记录 TraceId、用户 ID、客户端 IP。
- 异步任务中如需链路信息，应显式复制上下文，不要假设自动传递。

## 日志管理

日志管理用于记录系统运行状态、业务操作、异常信息、接口耗时和关键链路数据。良好的日志可以显著降低生产问题排查成本。Spring Boot 默认使用 Logback 作为日志实现，业务代码中推荐通过 SLF4J 门面输出日志。

### 日志级别配置

日志级别用于控制不同包、不同组件输出日志的详细程度。常见级别从低到高包括 `trace`、`debug`、`info`、`warn`、`error`。生产环境通常使用 `info` 或 `warn`，本地开发可以对项目包开启 `debug`。

文件位置：`src/main/resources/application.yml`

以下配置为不同包设置日志级别。

```yaml
logging:
  level:
    # 项目业务包日志级别
    io.github.atengk: info
    # Spring Web 日志级别
    org.springframework.web: info
    # Spring 事务日志，排查事务问题时可临时开启 debug
    org.springframework.transaction: info
    # JDBC 日志，排查 SQL 问题时可临时调整
    org.springframework.jdbc.core: info
```

开发环境可以单独开启更详细日志。

文件位置：`src/main/resources/application-dev.yml`

```yaml
logging:
  level:
    # 开发环境输出更详细的业务日志
    io.github.atengk: debug
    # 开发排查事务时可开启
    org.springframework.transaction: debug
```

生产环境建议收敛日志级别。

文件位置：`src/main/resources/application-prod.yml`

```yaml
logging:
  level:
    # 生产环境保持 info
    io.github.atengk: info
    # 生产环境减少框架日志噪声
    org.springframework.web: warn
    org.springframework.transaction: warn
```

日志级别使用建议：

- `debug`：本地开发、问题排查、临时诊断。
- `info`：正常业务流程、关键状态变化。
- `warn`：可恢复异常、业务异常、异常分支。
- `error`：系统异常、外部依赖失败、不可恢复错误。
- 生产环境不建议长期开启大量 `debug` 日志。
- 日志级别配置应按包名精确控制，避免全局过度输出。

### 日志输出格式

日志输出格式应包含时间、日志级别、进程 ID、线程名、TraceId、Logger 名称和日志内容。加入 TraceId 后，可以快速串联同一次请求产生的多行日志。

文件位置：`src/main/resources/logback-spring.xml`

以下 Logback 配置定义统一日志输出格式，并从 MDC 中读取 `traceId`。

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>

    <!-- 应用名称，默认取 spring.application.name -->
    <springProperty scope="context" name="APP_NAME" source="spring.application.name" defaultValue="spring-boot3-demo"/>

    <!-- 日志目录 -->
    <springProperty scope="context" name="LOG_PATH" source="logging.file.path" defaultValue="./logs"/>

    <!-- 控制台日志格式 -->
    <property name="CONSOLE_LOG_PATTERN"
              value="%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level ${APP_NAME} [%thread] [%X{traceId:-}] %logger{36} - %msg%n"/>

    <!-- 文件日志格式 -->
    <property name="FILE_LOG_PATTERN"
              value="%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level ${APP_NAME} [%thread] [%X{traceId:-}] %logger{60} - %msg%n"/>

</configuration>
```

完整日志示例：

```text
2026-05-09 10:30:12.123 INFO  spring-boot3-demo [http-nio-8080-exec-1] [5f9b6e2c4c6b4a94a8f7d1b28d7f9f11] i.g.a.demo.service.UserService - 查询用户成功，用户ID：1001
```

日志格式建议：

- 必须包含时间和日志级别。
- 建议包含 TraceId。
- 建议包含线程名，便于排查异步和并发问题。
- Logger 名称长度可以限制，避免日志过宽。
- 文件日志和控制台日志可以使用不同格式。
- 不建议在日志格式中输出过多固定字段，避免影响可读性。

### 控制台日志配置

控制台日志适合本地开发、容器标准输出和临时排查。容器化部署时，应用通常将日志输出到 stdout/stderr，由平台采集到日志系统。

文件位置：`src/main/resources/logback-spring.xml`

以下配置定义控制台日志输出。

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>

    <springProperty scope="context" name="APP_NAME" source="spring.application.name" defaultValue="spring-boot3-demo"/>

    <property name="CONSOLE_LOG_PATTERN"
              value="%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level ${APP_NAME} [%thread] [%X{traceId:-}] %logger{36} - %msg%n"/>

    <!-- 控制台输出 -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>${CONSOLE_LOG_PATTERN}</pattern>
            <charset>UTF-8</charset>
        </encoder>
    </appender>

    <!-- 根日志级别 -->
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
    </root>

</configuration>
```

本地开发中，如果日志过多，可以只调整项目包级别。

```yaml
logging:
  level:
    io.github.atengk: debug
    org.springframework: info
```

控制台日志建议：

- 本地开发优先输出到控制台。
- Docker 和 Kubernetes 环境建议输出到标准输出。
- 控制台日志保持简洁，避免超长 JSON 全量输出。
- 生产容器日志由平台采集时，不一定需要应用写本地文件。
- 控制台日志中也应包含 TraceId。

### 文件日志配置

文件日志适合传统服务器部署场景，可以按照日期和大小滚动保存。需要注意日志文件占用磁盘空间，必须配置滚动策略和保留天数。

文件位置：`src/main/resources/application.yml`

以下配置定义日志文件目录。

```yaml
logging:
  file:
    # 日志文件输出目录
    path: ./logs
```

文件位置：`src/main/resources/logback-spring.xml`

以下配置定义按日期和大小滚动的文件日志。

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>

    <springProperty scope="context" name="APP_NAME" source="spring.application.name" defaultValue="spring-boot3-demo"/>
    <springProperty scope="context" name="LOG_PATH" source="logging.file.path" defaultValue="./logs"/>

    <property name="FILE_LOG_PATTERN"
              value="%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level ${APP_NAME} [%thread] [%X{traceId:-}] %logger{60} - %msg%n"/>

    <!-- 普通业务日志文件 -->
    <appender name="FILE_INFO" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${LOG_PATH}/${APP_NAME}.log</file>
        <encoder>
            <pattern>${FILE_LOG_PATTERN}</pattern>
            <charset>UTF-8</charset>
        </encoder>
        <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
            <!-- 按日期和序号滚动 -->
            <fileNamePattern>${LOG_PATH}/archive/${APP_NAME}.%d{yyyy-MM-dd}.%i.log.gz</fileNamePattern>
            <!-- 单个文件最大大小 -->
            <maxFileSize>100MB</maxFileSize>
            <!-- 保留天数 -->
            <maxHistory>30</maxHistory>
            <!-- 日志总大小上限 -->
            <totalSizeCap>10GB</totalSizeCap>
        </rollingPolicy>
    </appender>

    <!-- 错误日志文件 -->
    <appender name="FILE_ERROR" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${LOG_PATH}/${APP_NAME}-error.log</file>
        <filter class="ch.qos.logback.classic.filter.ThresholdFilter">
            <level>ERROR</level>
        </filter>
        <encoder>
            <pattern>${FILE_LOG_PATTERN}</pattern>
            <charset>UTF-8</charset>
        </encoder>
        <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
            <fileNamePattern>${LOG_PATH}/archive/${APP_NAME}-error.%d{yyyy-MM-dd}.%i.log.gz</fileNamePattern>
            <maxFileSize>100MB</maxFileSize>
            <maxHistory>60</maxHistory>
            <totalSizeCap>5GB</totalSizeCap>
        </rollingPolicy>
    </appender>

    <root level="INFO">
        <appender-ref ref="FILE_INFO"/>
        <appender-ref ref="FILE_ERROR"/>
    </root>

</configuration>
```

文件日志建议：

- 配置滚动策略，避免单文件过大。
- 配置保留天数，避免磁盘被打满。
- 错误日志可单独输出，便于快速排查。
- 日志目录应使用部署用户有权限的路径。
- 容器环境优先使用标准输出，是否写文件由平台规范决定。
- 生产日志必须接入集中化日志系统，例如 ELK、Loki、云日志服务等。

### 业务日志规范

业务日志用于记录关键业务动作和状态变化。业务日志应服务于问题排查和审计，不是越多越好。日志应包含业务标识、操作结果、关键状态、耗时、TraceId 等信息，避免输出密码、Token、身份证号、银行卡号等敏感数据。

推荐记录的业务日志：

| 场景         | 日志级别 | 示例                   |
| ------------ | -------- | ---------------------- |
| 创建订单成功 | `info`   | 订单 ID、用户 ID、金额 |
| 状态变更     | `info`   | 原状态、新状态、操作人 |
| 业务校验失败 | `warn`   | 用户 ID、失败原因      |
| 外部接口失败 | `error`  | 接口名、状态码、耗时   |
| 系统异常     | `error`  | 完整异常堆栈           |

文件位置：`src/main/java/io/github/atengk/demo/service/OrderLogService.java`

以下 Service 演示业务日志输出规范。

```java
package io.github.atengk.demo.service;

import cn.hutool.core.util.DesensitizedUtil;
import io.github.atengk.demo.common.exception.BizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 订单日志业务组件。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Service
public class OrderLogService {

    public void createOrder(Long userId, String mobile, BigDecimal amount) {
        if (userId == null) {
            log.warn("创建订单失败，用户ID为空");
            throw new BizException("用户ID不能为空");
        }

        log.info("开始创建订单，用户ID：{}，手机号：{}，金额：{}",
                userId,
                DesensitizedUtil.mobilePhone(mobile),
                amount);

        Long orderId = 10001L;

        log.info("创建订单成功，订单ID：{}，用户ID：{}，金额：{}", orderId, userId, amount);
    }

    public void cancelOrder(Long orderId, Long userId) {
        if (orderId == null) {
            log.warn("取消订单失败，订单ID为空，用户ID：{}", userId);
            throw new BizException("订单ID不能为空");
        }

        log.info("取消订单成功，订单ID：{}，用户ID：{}", orderId, userId);
    }

}
```

业务日志建议：

- 成功日志记录关键业务 ID。
- 失败日志记录失败原因和业务标识。
- 异常日志使用 `log.error("xxx", e)` 输出堆栈。
- 参数日志避免输出完整请求体。
- 敏感字段必须脱敏。
- 高频接口不要输出过多 info 日志。
- 不要使用 `System.out.println` 输出业务日志。

## 接口调用日志

接口调用日志用于记录 HTTP 请求和响应的基本信息，包括请求方法、URI、客户端 IP、请求参数、响应状态、耗时、异常信息等。它可以帮助定位接口慢请求、异常请求、参数错误和调用方问题。接口日志通常通过 Filter 或 Interceptor 统一实现，不建议每个 Controller 手动记录。

### 请求日志记录

请求日志记录应包含请求方法、URI、客户端 IP、TraceId、请求头摘要和必要参数。对于 JSON 请求体，如果需要记录，需要使用 `ContentCachingRequestWrapper` 包装请求，否则请求体被读取后 Controller 可能无法再次读取。

文件位置：`src/main/java/io/github/atengk/demo/filter/ApiAccessLogFilter.java`

以下 Filter 使用 Spring 的缓存包装器记录请求基础信息。

```java
package io.github.atengk.demo.filter;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.demo.common.util.ClientInfoUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;

/**
 * 接口访问日志过滤器。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Component
public class ApiAccessLogFilter extends OncePerRequestFilter {

    private static final String START_TIME = "apiAccessStartTime";

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return !StrUtil.startWith(uri, "/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long startTime = System.currentTimeMillis();
        request.setAttribute(START_TIME, startTime);

        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

        log.info("接口请求开始，method：{}，uri：{}，clientIp：{}",
                request.getMethod(),
                request.getRequestURI(),
                ClientInfoUtil.getClientIp(request));

        try {
            filterChain.doFilter(requestWrapper, responseWrapper);
        } finally {
            responseWrapper.copyBodyToResponse();
        }
    }

}
```

请求日志建议：

- 记录请求方法、URI、客户端 IP、TraceId。
- 查询参数可以记录，敏感字段要脱敏。
- 请求体按需记录，不建议默认完整输出。
- 文件上传接口不要记录文件内容。
- 健康检查接口和静态资源接口可以排除。
- 高频接口请求日志应控制级别和采样策略。

### 响应日志记录

响应日志用于记录接口返回状态、响应体摘要和业务响应码。响应体日志需要谨慎开启，尤其是列表、大对象、文件下载、敏感数据场景。生产环境一般不建议完整记录响应体。

可以在前面的 `ApiAccessLogFilter` 中补充响应日志。以下代码展示完整版本。

文件位置：`src/main/java/io/github/atengk/demo/filter/ApiAccessLogFilter.java`

以下 Filter 在请求完成后记录响应状态、耗时、请求体摘要和响应体摘要。

```java
package io.github.atengk.demo.filter;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.demo.common.util.ClientInfoUtil;
import io.github.atengk.demo.common.util.SensitiveLogUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 接口访问日志过滤器。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Component
public class ApiAccessLogFilter extends OncePerRequestFilter {

    private static final int MAX_LOG_BODY_LENGTH = 2000;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return !StrUtil.startWith(uri, "/api/") || StrUtil.contains(uri, "/api/files/download");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long startTime = System.currentTimeMillis();

        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

        try {
            filterChain.doFilter(requestWrapper, responseWrapper);
        } finally {
            long cost = System.currentTimeMillis() - startTime;

            String requestBody = getRequestBody(requestWrapper);
            String responseBody = getResponseBody(responseWrapper);

            log.info("接口调用完成，method：{}，uri：{}，status：{}，clientIp：{}，耗时：{}ms，请求体：{}，响应体：{}",
                    request.getMethod(),
                    request.getRequestURI(),
                    responseWrapper.getStatus(),
                    ClientInfoUtil.getClientIp(request),
                    cost,
                    SensitiveLogUtil.desensitize(limitLength(requestBody)),
                    SensitiveLogUtil.desensitize(limitLength(responseBody)));

            responseWrapper.copyBodyToResponse();
        }
    }

    private String getRequestBody(ContentCachingRequestWrapper requestWrapper) {
        String contentType = StrUtil.blankToDefault(requestWrapper.getContentType(), "");
        if (!StrUtil.containsIgnoreCase(contentType, MediaType.APPLICATION_JSON_VALUE)) {
            return "";
        }

        byte[] content = requestWrapper.getContentAsByteArray();
        if (content.length == 0) {
            return "";
        }
        return new String(content, StandardCharsets.UTF_8);
    }

    private String getResponseBody(ContentCachingResponseWrapper responseWrapper) {
        String contentType = StrUtil.blankToDefault(responseWrapper.getContentType(), "");
        if (!StrUtil.containsIgnoreCase(contentType, MediaType.APPLICATION_JSON_VALUE)) {
            return "";
        }

        byte[] content = responseWrapper.getContentAsByteArray();
        if (content.length == 0) {
            return "";
        }
        return new String(content, StandardCharsets.UTF_8);
    }

    private String limitLength(String content) {
        if (StrUtil.isBlank(content)) {
            return "";
        }
        return StrUtil.length(content) > MAX_LOG_BODY_LENGTH
                ? StrUtil.sub(content, 0, MAX_LOG_BODY_LENGTH) + "..."
                : content;
    }

}
```

响应日志建议：

- 记录 HTTP 状态码和耗时。
- 响应体只在必要时记录摘要。
- 大响应、文件下载、图片预览接口应排除响应体日志。
- 响应日志必须在 `copyBodyToResponse()` 前读取缓存体。
- 日志记录失败不能影响正常接口响应。
- 敏感字段必须脱敏。

### 异常日志记录

异常日志应记录异常类型、异常消息、请求路径、请求方法、TraceId、用户 ID、客户端 IP 和堆栈信息。业务异常通常记录 `warn`，系统异常记录 `error` 并输出堆栈。

全局异常处理器是记录异常日志的主要位置。

文件位置：`src/main/java/io/github/atengk/demo/common/handler/GlobalExceptionHandler.java`

以下异常处理器记录业务异常和系统异常。

```java
package io.github.atengk.demo.common.handler;

import io.github.atengk.demo.common.context.RequestContextHolder;
import io.github.atengk.demo.common.exception.BizException;
import io.github.atengk.demo.common.response.ApiResult;
import io.github.atengk.demo.common.response.ResultCodeEnum;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public ApiResult<Void> handleBizException(BizException e, HttpServletRequest request) {
        log.warn("业务异常，traceId：{}，userId：{}，method：{}，uri：{}，message：{}",
                RequestContextHolder.getTraceId(),
                RequestContextHolder.getUserId(),
                request.getMethod(),
                request.getRequestURI(),
                e.getMessage());

        return ApiResult.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ApiResult<Void> handleException(Exception e, HttpServletRequest request) {
        log.error("系统异常，traceId：{}，userId：{}，method：{}，uri：{}",
                RequestContextHolder.getTraceId(),
                RequestContextHolder.getUserId(),
                request.getMethod(),
                request.getRequestURI(),
                e);

        return ApiResult.fail(ResultCodeEnum.INTERNAL_ERROR);
    }

}
```

异常日志建议：

- 业务异常用 `warn`，系统异常用 `error`。
- 系统异常必须输出完整堆栈。
- 异常日志中必须包含 URI 和 TraceId。
- 参数校验异常通常不需要打印完整堆栈。
- 不要把异常堆栈返回给前端。
- 不要重复记录同一个异常，避免日志噪声。

### 耗时日志记录

耗时日志用于发现慢接口、慢数据库操作、慢外部接口调用等性能问题。接口层耗时可以通过 Filter 或 Interceptor 统一记录，方法级耗时可以通过 AOP 记录。

文件位置：`src/main/java/io/github/atengk/demo/common/annotation/CostLog.java`

以下注解用于标记需要记录方法耗时的业务方法。

```java
package io.github.atengk.demo.common.annotation;

import java.lang.annotation.*;

/**
 * 方法耗时日志注解。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CostLog {

    String value() default "";

}
```

文件位置：`src/main/java/io/github/atengk/demo/common/aspect/CostLogAspect.java`

以下切面用于记录标记了 `@CostLog` 的方法耗时。

```java
package io.github.atengk.demo.common.aspect;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.demo.common.annotation.CostLog;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

/**
 * 方法耗时日志切面。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Aspect
@Component
public class CostLogAspect {

    @Around("@annotation(costLog)")
    public Object around(ProceedingJoinPoint joinPoint, CostLog costLog) throws Throwable {
        long startTime = System.currentTimeMillis();

        try {
            return joinPoint.proceed();
        } finally {
            long cost = System.currentTimeMillis() - startTime;
            String name = StrUtil.blankToDefault(costLog.value(), joinPoint.getSignature().toShortString());
            log.info("方法执行完成，名称：{}，耗时：{}ms", name, cost);
        }
    }

}
```

使用耗时注解：

文件位置：`src/main/java/io/github/atengk/demo/service/ReportService.java`

以下 Service 使用 `@CostLog` 记录报表生成耗时。

```java
package io.github.atengk.demo.service;

import io.github.atengk.demo.common.annotation.CostLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 报表业务组件。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Service
public class ReportService {

    @CostLog("生成销售报表")
    public void generateSalesReport() {
        log.info("开始生成销售报表");
    }

}
```

如果项目没有引入 AOP，需要添加依赖。

文件位置：`pom.xml`

```xml
<!-- Spring AOP：用于实现方法耗时日志、权限切面、审计切面等能力 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>
```

耗时日志建议：

- 接口耗时通过 Filter 或 Interceptor 统一记录。
- 关键业务方法耗时可以通过 AOP 记录。
- 慢接口阈值应结合业务设置。
- 耗时日志应包含 TraceId 和业务标识。
- 高频短方法不建议全部加耗时日志。
- 外部接口调用和数据库慢查询应单独监控。

### 敏感字段脱敏

敏感字段脱敏用于防止日志泄露密码、手机号、身份证号、银行卡号、Token、密钥等数据。接口日志、业务日志、异常日志中都必须避免输出敏感明文。

常见敏感字段：

| 字段        | 处理方式                         |
| ----------- | -------------------------------- |
| `password`  | 替换为 `******`                  |
| `token`     | 替换为 `******` 或只保留前后几位 |
| `mobile`    | 手机号脱敏                       |
| `email`     | 邮箱脱敏                         |
| `idCard`    | 身份证脱敏                       |
| `bankCard`  | 银行卡脱敏                       |
| `secretKey` | 替换为 `******`                  |

文件位置：`src/main/java/io/github/atengk/demo/common/util/SensitiveLogUtil.java`

以下工具类用于对日志内容中的常见敏感字段做基础脱敏。

```java
package io.github.atengk.demo.common.util;

import cn.hutool.core.util.DesensitizedUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;

/**
 * 敏感日志脱敏工具类。
 *
 * @author Ateng
 * @since 2026-05-09
 */
public class SensitiveLogUtil {

    private SensitiveLogUtil() {
    }

    public static String desensitize(String content) {
        if (StrUtil.isBlank(content)) {
            return content;
        }

        String result = content;

        result = ReUtil.replaceAll(result, "(?i)(\"password\"\\s*:\\s*\")([^\"]+)(\")", "$1******$3");
        result = ReUtil.replaceAll(result, "(?i)(\"token\"\\s*:\\s*\")([^\"]+)(\")", "$1******$3");
        result = ReUtil.replaceAll(result, "(?i)(\"accessToken\"\\s*:\\s*\")([^\"]+)(\")", "$1******$3");
        result = ReUtil.replaceAll(result, "(?i)(\"refreshToken\"\\s*:\\s*\")([^\"]+)(\")", "$1******$3");
        result = ReUtil.replaceAll(result, "(?i)(\"secretKey\"\\s*:\\s*\")([^\"]+)(\")", "$1******$3");

        result = ReUtil.replaceAll(result, "(1[3-9]\\d{9})", matcher -> DesensitizedUtil.mobilePhone(matcher.group(1)));

        return result;
    }

}
```

业务代码中也应主动脱敏。

文件位置：`src/main/java/io/github/atengk/demo/service/LoginLogService.java`

以下 Service 输出登录日志时对手机号和 Token 做脱敏处理。

```java
package io.github.atengk.demo.service;

import cn.hutool.core.util.DesensitizedUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 登录日志业务组件。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Service
public class LoginLogService {

    public void recordLoginSuccess(Long userId, String mobile, String token) {
        String safeMobile = DesensitizedUtil.mobilePhone(mobile);
        String safeToken = StrUtil.isBlank(token) ? "" : StrUtil.subPre(token, 6) + "******";

        log.info("用户登录成功，用户ID：{}，手机号：{}，token：{}", userId, safeMobile, safeToken);
    }

}
```

敏感字段脱敏建议：

- 密码、Token、密钥永远不要明文输出。
- 手机号、身份证、银行卡、邮箱按规则脱敏。
- 请求体和响应体日志统一经过脱敏工具处理。
- 异常日志中也可能包含敏感参数，需要控制输出内容。
- 文件上传日志不要输出完整本地路径给外部响应。
- 生产环境日志访问权限应严格控制。
- 脱敏规则应随着业务字段变化持续维护。



## JSON 序列化

JSON 序列化用于控制 Java 对象与 JSON 数据之间的转换规则。Spring Boot Web 项目默认使用 Jackson 作为 JSON 处理组件，常见配置包括日期时间格式、空值字段、枚举输出、字段命名策略和自定义序列化规则。统一 JSON 序列化规则可以减少前后端字段格式不一致、时间格式混乱、枚举值不可控等问题。

### Jackson 默认配置

Spring Boot 引入 `spring-boot-starter-web` 后，会自动集成 Jackson，并使用 `ObjectMapper` 处理 `@RequestBody` 和接口响应对象。一般情况下，不需要手动创建 `ObjectMapper`，而是通过配置文件或 `Jackson2ObjectMapperBuilderCustomizer` 扩展默认行为。

文件位置：`pom.xml`

以下依赖会间接引入 Jackson 和 Spring MVC JSON 转换能力。

```xml
<dependencies>
    <!-- Spring Web：默认集成 Jackson，用于 JSON 请求体解析和响应序列化 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
</dependencies>
```

文件位置：`src/main/java/io/github/atengk/demo/controller/JacksonDemoController.java`

以下接口返回一个普通 Java 对象，Spring MVC 会自动使用 Jackson 转换为 JSON。

```java
package io.github.atengk.demo.controller;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.map.MapUtil;
import io.github.atengk.demo.common.response.ApiResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Jackson JSON 示例接口。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@RestController
public class JacksonDemoController {

    /**
     * 获取 JSON 示例数据。
     *
     * @return JSON 示例数据
     */
    @GetMapping("/api/json/demo")
    public ApiResult<Map<String, Object>> demo() {
        Map<String, Object> data = MapUtil.<String, Object>builder()
                .put("name", "spring-boot3-demo")
                .put("version", "1.0.0")
                .put("currentTime", DateUtil.now())
                .build();

        return ApiResult.success(data);
    }

}
```

Jackson 默认使用建议：

- 不建议在业务代码中频繁手动创建 `ObjectMapper`。
- 全局 JSON 行为优先通过配置文件或配置类统一处理。
- 请求 DTO 和响应 VO 字段命名应稳定，避免频繁变更。
- 日期时间、枚举、空值字段应有统一规则。
- 对外接口一旦发布，JSON 字段格式应尽量保持兼容。

### 日期时间格式化

日期时间格式化用于统一 `LocalDateTime`、`LocalDate`、`LocalTime`、`Date` 等类型的 JSON 输入输出格式。后端应避免同一个系统中同时出现时间戳、ISO 字符串、自定义字符串等多种格式。

推荐格式：

| Java 类型       | JSON 格式             |
| --------------- | --------------------- |
| `LocalDateTime` | `yyyy-MM-dd HH:mm:ss` |
| `LocalDate`     | `yyyy-MM-dd`          |
| `LocalTime`     | `HH:mm:ss`            |

文件位置：`src/main/resources/application.yml`

以下配置用于设置 Jackson 的基础日期格式和时区。

```yaml
spring:
  jackson:
    # Date 类型默认格式
    date-format: yyyy-MM-dd HH:mm:ss
    # 默认时区
    time-zone: Asia/Shanghai
    serialization:
      # 日期不输出为时间戳
      write-dates-as-timestamps: false
```

对于 Java 8 时间类型，建议使用配置类进行统一处理。

文件位置：`src/main/java/io/github/atengk/demo/config/JacksonConfig.java`

以下配置类统一设置 `LocalDateTime`、`LocalDate`、`LocalTime` 的序列化和反序列化格式。

```java
package io.github.atengk.demo.config;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Jackson JSON 配置。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Configuration
public class JacksonConfig {

    public static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    public static final String DATE_PATTERN = "yyyy-MM-dd";

    public static final String TIME_PATTERN = "HH:mm:ss";

    /**
     * 自定义 Jackson 构建器。
     *
     * @return Jackson 构建器自定义器
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jackson2ObjectMapperBuilderCustomizer() {
        return builder -> {
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN);
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(DATE_PATTERN);
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern(TIME_PATTERN);

            builder.serializerByType(LocalDateTime.class, new LocalDateTimeSerializer(dateTimeFormatter));
            builder.deserializerByType(LocalDateTime.class, new LocalDateTimeDeserializer(dateTimeFormatter));

            builder.serializerByType(LocalDate.class, new LocalDateSerializer(dateFormatter));
            builder.deserializerByType(LocalDate.class, new LocalDateDeserializer(dateFormatter));

            builder.serializerByType(LocalTime.class, new LocalTimeSerializer(timeFormatter));
            builder.deserializerByType(LocalTime.class, new LocalTimeDeserializer(timeFormatter));

            builder.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

            log.info("初始化 Jackson 日期时间格式配置");
        };
    }

}
```

文件位置：`src/main/java/io/github/atengk/demo/model/vo/JsonTimeVO.java`

以下 VO 用于验证日期时间序列化效果。

```java
package io.github.atengk.demo.model.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * JSON 时间响应对象。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
public class JsonTimeVO {

    private LocalDateTime createTime;

    private LocalDate birthday;

    private LocalTime startTime;

}
```

文件位置：`src/main/java/io/github/atengk/demo/controller/JsonTimeController.java`

以下接口返回日期时间对象，用于验证全局格式化配置。

```java
package io.github.atengk.demo.controller;

import io.github.atengk.demo.common.response.ApiResult;
import io.github.atengk.demo.model.vo.JsonTimeVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * JSON 时间格式接口。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@RestController
public class JsonTimeController {

    /**
     * 获取时间格式示例。
     *
     * @return 时间格式示例
     */
    @GetMapping("/api/json/time")
    public ApiResult<JsonTimeVO> time() {
        JsonTimeVO vo = new JsonTimeVO();
        vo.setCreateTime(LocalDateTime.now());
        vo.setBirthday(LocalDate.now());
        vo.setStartTime(LocalTime.of(9, 30, 0));
        return ApiResult.success(vo);
    }

}
```

响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "createTime": "2026-05-09 10:30:12",
    "birthday": "2026-05-09",
    "startTime": "09:30:00"
  }
}
```

日期时间格式化建议：

- 全项目统一日期时间格式。
- 响应时间字段优先使用 `LocalDateTime`、`LocalDate`、`LocalTime`。
- 不同接口不要混用时间戳和字符串格式。
- 涉及时区的国际化系统应明确使用 UTC 或业务时区。
- 数据库存储、后端对象、JSON 输出的时间规则应统一说明。

### 空值字段处理

空值字段处理用于控制响应 JSON 中是否输出 `null` 字段。默认情况下，Jackson 会输出空值字段。接口响应是否保留空值，需要结合前端约定和接口兼容性决定。

常见策略：

| 策略       | 说明                                 |
| ---------- | ------------------------------------ |
| 输出空值   | 字段完整，前端结构稳定               |
| 忽略空值   | 响应更简洁，但前端需要处理字段不存在 |
| 忽略空集合 | 空列表字段不输出，不推荐默认使用     |
| 局部忽略   | 只对某些 VO 生效                     |

方式一：全局忽略空值。

文件位置：`src/main/resources/application.yml`

以下配置让 Jackson 全局不输出 `null` 字段。

```yaml
spring:
  jackson:
    # non_null 表示 JSON 响应中不输出 null 字段
    default-property-inclusion: non_null
```

方式二：在指定类上局部忽略空值。

文件位置：`src/main/java/io/github/atengk/demo/model/vo/UserJsonVO.java`

以下 VO 只在当前类中忽略空值字段。

```java
package io.github.atengk.demo.model.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

/**
 * 用户 JSON 响应对象。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserJsonVO {

    private Long id;

    private String username;

    private String nickname;

    private String email;

}
```

空值字段响应示例：

```json
{
  "id": 1001,
  "username": "ateng",
  "nickname": "Ateng"
}
```

如果 `email` 为 `null`，配置 `NON_NULL` 后不会出现在 JSON 中。

空值字段处理建议：

- 内部管理系统可以保留空值字段，前端结构更稳定。
- 对外开放接口可以按约定忽略空值，减少响应体大小。
- 分页 `records` 为空时应返回空数组，不应返回 `null`。
- 空值策略一旦确定，不建议频繁修改。
- 全局忽略空值前，应确认前端是否依赖字段存在。

### 枚举序列化

枚举序列化用于控制枚举在 JSON 中输出什么内容。默认情况下，Jackson 通常输出枚举名称，例如 `"ENABLE"`。实际业务中更常见的是输出编码、描述，或同时输出编码和描述。

推荐方案一：枚举字段输出编码，前端通过字典或接口映射展示文本。

文件位置：`src/main/java/io/github/atengk/demo/common/enums/UserStatusEnum.java`

以下枚举使用 `@JsonValue` 指定 JSON 输出编码，并使用 `@JsonCreator` 支持从编码反序列化。

```java
package io.github.atengk.demo.common.enums;

import cn.hutool.core.util.ObjectUtil;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 用户状态枚举。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Getter
@RequiredArgsConstructor
public enum UserStatusEnum {

    ENABLE(1, "启用"),

    DISABLE(0, "禁用");

    private final Integer code;

    private final String description;

    /**
     * JSON 序列化时输出编码。
     *
     * @return 状态编码
     */
    @JsonValue
    public Integer getCode() {
        return code;
    }

    /**
     * 根据编码创建枚举。
     *
     * @param code 状态编码
     * @return 用户状态枚举
     */
    @JsonCreator
    public static UserStatusEnum of(Integer code) {
        if (code == null) {
            return null;
        }

        for (UserStatusEnum value : values()) {
            if (ObjectUtil.equals(value.getCode(), code)) {
                return value;
            }
        }
        return null;
    }

}
```

文件位置：`src/main/java/io/github/atengk/demo/model/vo/UserStatusVO.java`

以下 VO 包含枚举字段。

```java
package io.github.atengk.demo.model.vo;

import io.github.atengk.demo.common.enums.UserStatusEnum;
import lombok.Data;

/**
 * 用户状态响应对象。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
public class UserStatusVO {

    private Long id;

    private String username;

    private UserStatusEnum status;

}
```

响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 1001,
    "username": "ateng",
    "status": 1
  }
}
```

推荐方案二：响应中同时返回编码和描述。

文件位置：`src/main/java/io/github/atengk/demo/model/vo/UserStatusDetailVO.java`

以下 VO 显式返回状态编码和状态描述，适合前端不想再查字典的场景。

```java
package io.github.atengk.demo.model.vo;

import lombok.Data;

/**
 * 用户状态详情响应对象。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
public class UserStatusDetailVO {

    private Long id;

    private String username;

    private Integer status;

    private String statusName;

}
```

枚举序列化建议：

- 数据库存储建议使用稳定编码，不使用枚举名称。
- JSON 对外输出编码时，前端需要有字典映射。
- 如果前端需要直接展示，可同时返回编码和描述。
- 不建议直接输出枚举 ordinal 序号，因为枚举顺序变化会产生兼容风险。
- `@JsonCreator` 中无法识别的编码应根据业务决定返回 `null` 或抛出异常。

### 字段命名策略

字段命名策略用于控制 Java 小驼峰字段与 JSON 字段之间的转换方式。Java 推荐使用小驼峰，例如 `createTime`；部分前端或外部接口可能要求下划线，例如 `create_time`。

常见策略：

| Java 字段    | JSON 字段     | 策略           |
| ------------ | ------------- | -------------- |
| `createTime` | `createTime`  | 默认小驼峰     |
| `createTime` | `create_time` | 下划线         |
| `createTime` | `create-time` | 中划线，不常用 |

方式一：全局配置下划线命名。

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  jackson:
    # 全局 JSON 字段使用下划线命名策略
    property-naming-strategy: SNAKE_CASE
```

方式二：局部类使用下划线命名。

文件位置：`src/main/java/io/github/atengk/demo/model/vo/SnakeUserVO.java`

以下 VO 只对当前类启用下划线命名。

```java
package io.github.atengk.demo.model.vo;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 下划线用户响应对象。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class SnakeUserVO {

    private Long userId;

    private String userName;

    private LocalDateTime createTime;

}
```

响应示例：

```json
{
  "user_id": 1001,
  "user_name": "ateng",
  "create_time": "2026-05-09 10:30:12"
}
```

方式三：单个字段指定 JSON 名称。

```java
@JsonProperty("user_name")
private String username;
```

字段命名策略建议：

- 内部前后端项目建议统一使用小驼峰或下划线，不要混用。
- 对外开放接口应优先遵守接口协议。
- 全局命名策略变更影响范围很大，应谨慎修改。
- 局部兼容外部接口时，可以使用 `@JsonProperty` 或 `@JsonNaming`。
- Java 字段仍保持小驼峰，不建议为了 JSON 改成下划线字段名。

## 类型转换

类型转换用于将请求中的字符串参数转换为 Java 类型，例如数字、日期时间、枚举、自定义对象等。Spring MVC 内置了大量常用转换器，但实际项目中经常需要补充日期时间转换、枚举编码转换和自定义格式转换。

### 请求参数类型转换

请求参数默认都是字符串，Spring MVC 会根据 Controller 方法参数类型自动进行转换。例如 `?id=1001` 可以转换为 `Long`，`?enabled=true` 可以转换为 `Boolean`。

文件位置：`src/main/java/io/github/atengk/demo/controller/TypeConvertController.java`

以下接口演示基础请求参数类型转换。

```java
package io.github.atengk.demo.controller;

import cn.hutool.core.map.MapUtil;
import io.github.atengk.demo.common.response.ApiResult;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 请求参数类型转换接口。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@RestController
@RequestMapping("/api/convert")
public class TypeConvertController {

    /**
     * 基础类型转换。
     *
     * @param id 用户 ID
     * @param enabled 是否启用
     * @param score 分数
     * @return 转换结果
     */
    @GetMapping("/basic")
    public ApiResult<Map<String, Object>> basic(@RequestParam Long id,
                                                @RequestParam Boolean enabled,
                                                @RequestParam Integer score) {
        Map<String, Object> data = MapUtil.<String, Object>builder()
                .put("id", id)
                .put("enabled", enabled)
                .put("score", score)
                .build();

        return ApiResult.success(data);
    }

}
```

调用示例：

```bash
curl "http://localhost:8080/api/convert/basic?id=1001&enabled=true&score=90"
```

请求参数转换建议：

- 基础类型转换由 Spring MVC 自动完成。
- 类型转换失败应由全局异常处理器统一返回参数错误。
- 必填参数使用包装类型配合校验注解，不建议使用基本类型。
- 复杂对象建议使用 Query DTO 接收。
- 日期时间、枚举等类型建议定义统一转换规则。

### 日期时间类型转换

日期时间类型转换用于处理查询参数或路径参数中的日期时间字符串。注意：这里处理的是请求参数转换，不是 JSON 请求体。JSON 请求体的日期格式由 Jackson 配置处理。

推荐格式：

```text
LocalDateTime: yyyy-MM-dd HH:mm:ss
LocalDate: yyyy-MM-dd
LocalTime: HH:mm:ss
```

文件位置：`src/main/java/io/github/atengk/demo/controller/DateTimeConvertController.java`

以下接口通过 `@DateTimeFormat` 指定单个参数的日期时间格式。

```java
package io.github.atengk.demo.controller;

import cn.hutool.core.map.MapUtil;
import io.github.atengk.demo.common.response.ApiResult;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 日期时间参数转换接口。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@RestController
@RequestMapping("/api/convert/time")
public class DateTimeConvertController {

    /**
     * 日期时间参数转换。
     *
     * @param startTime 开始时间
     * @param endDate 结束日期
     * @return 转换结果
     */
    @GetMapping
    public ApiResult<Map<String, Object>> time(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {

        Map<String, Object> data = MapUtil.<String, Object>builder()
                .put("startTime", startTime)
                .put("endDate", endDate)
                .build();

        return ApiResult.success(data);
    }

}
```

调用示例：

```bash
curl "http://localhost:8080/api/convert/time?startTime=2026-05-09%2010:30:00&endDate=2026-05-09"
```

如果希望全局处理日期时间参数转换，可以注册 Formatter。

文件位置：`src/main/java/io/github/atengk/demo/config/WebMvcConfig.java`

以下配置注册全局日期时间 Formatter。

```java
package io.github.atengk.demo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.format.datetime.standard.DateTimeFormatterRegistrar;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.format.DateTimeFormatter;

/**
 * Spring MVC 类型转换配置。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * 注册类型转换器和格式化器。
     *
     * @param registry 格式化器注册器
     */
    @Override
    public void addFormatters(FormatterRegistry registry) {
        DateTimeFormatterRegistrar registrar = new DateTimeFormatterRegistrar();
        registrar.setDateFormatter(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        registrar.setTimeFormatter(DateTimeFormatter.ofPattern("HH:mm:ss"));
        registrar.setDateTimeFormatter(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        registrar.registerFormatters(registry);
    }

}
```

日期时间转换建议：

- Query 参数日期格式使用 `@DateTimeFormat` 或全局 Formatter。
- JSON 请求体日期格式由 Jackson 配置处理。
- 同一项目中日期格式应统一。
- URL 中空格要编码为 `%20`。
- 时间范围查询建议使用 `startTime`、`endTime` 命名。

### 枚举类型转换

枚举类型转换用于将请求参数中的编码或名称转换为枚举对象。Spring 默认可以把枚举名称转换为枚举，例如 `status=ENABLE`，但实际业务更常使用编码，例如 `status=1`。此时需要自定义 Converter。

先定义一个通用编码枚举接口。

文件位置：`src/main/java/io/github/atengk/demo/common/enums/CodeEnum.java`

以下接口用于约束枚举必须提供编码。

```java
package io.github.atengk.demo.common.enums;

/**
 * 编码枚举接口。
 *
 * @author Ateng
 * @since 2026-05-09
 */
public interface CodeEnum<T> {

    /**
     * 获取枚举编码。
     *
     * @return 枚举编码
     */
    T getCode();

}
```

文件位置：`src/main/java/io/github/atengk/demo/common/enums/UserStatusEnum.java`

以下枚举实现编码枚举接口。

```java
package io.github.atengk.demo.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 用户状态枚举。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Getter
@RequiredArgsConstructor
public enum UserStatusEnum implements CodeEnum<Integer> {

    ENABLE(1, "启用"),

    DISABLE(0, "禁用");

    private final Integer code;

    private final String description;

}
```

文件位置：`src/main/java/io/github/atengk/demo/common/converter/StringToUserStatusEnumConverter.java`

以下转换器将请求参数中的字符串编码转换为 `UserStatusEnum`。

```java
package io.github.atengk.demo.common.converter;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.demo.common.enums.UserStatusEnum;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * 字符串转用户状态枚举转换器。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Component
public class StringToUserStatusEnumConverter implements Converter<String, UserStatusEnum> {

    /**
     * 转换用户状态枚举。
     *
     * @param source 请求参数值
     * @return 用户状态枚举
     */
    @Override
    public UserStatusEnum convert(String source) {
        if (StrUtil.isBlank(source)) {
            return null;
        }

        Integer code = Convert.toInt(source);
        for (UserStatusEnum value : UserStatusEnum.values()) {
            if (ObjectUtil.equals(value.getCode(), code)) {
                return value;
            }
        }

        throw new IllegalArgumentException("用户状态不合法");
    }

}
```

文件位置：`src/main/java/io/github/atengk/demo/controller/EnumConvertController.java`

以下接口直接接收枚举类型参数。

```java
package io.github.atengk.demo.controller;

import cn.hutool.core.map.MapUtil;
import io.github.atengk.demo.common.enums.UserStatusEnum;
import io.github.atengk.demo.common.response.ApiResult;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 枚举参数转换接口。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@RestController
@RequestMapping("/api/convert/enum")
public class EnumConvertController {

    /**
     * 枚举参数转换。
     *
     * @param status 用户状态
     * @return 转换结果
     */
    @GetMapping
    public ApiResult<Map<String, Object>> enumConvert(@RequestParam UserStatusEnum status) {
        Map<String, Object> data = MapUtil.<String, Object>builder()
                .put("code", status.getCode())
                .put("description", status.getDescription())
                .build();

        return ApiResult.success(data);
    }

}
```

调用示例：

```bash
curl "http://localhost:8080/api/convert/enum?status=1"
```

枚举转换建议：

- 请求参数建议使用稳定编码，不直接使用枚举名称。
- 数据库存储也建议使用编码。
- 对外响应可以返回编码和描述。
- 枚举编码转换失败应由全局异常处理器返回参数错误。
- 通用枚举较多时可以实现通用 ConverterFactory，而不是为每个枚举写一个 Converter。

### 自定义 Converter

自定义 `Converter` 用于处理字符串到特定类型的转换。它适合无格式化展示需求的简单转换，例如字符串转业务 ID、字符串转枚举、字符串转自定义值对象。

示例：将请求参数中的 `tenant:1001` 转换为租户标识对象。

文件位置：`src/main/java/io/github/atengk/demo/model/value/TenantKey.java`

以下值对象用于表示租户标识。

```java
package io.github.atengk.demo.model.value;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 租户标识值对象。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TenantKey {

    private String type;

    private Long id;

}
```

文件位置：`src/main/java/io/github/atengk/demo/common/converter/StringToTenantKeyConverter.java`

以下转换器将 `tenant:1001` 转换为 `TenantKey`。

```java
package io.github.atengk.demo.common.converter;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.demo.model.value.TenantKey;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * 字符串转租户标识转换器。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Component
public class StringToTenantKeyConverter implements Converter<String, TenantKey> {

    /**
     * 转换租户标识。
     *
     * @param source 请求参数值
     * @return 租户标识
     */
    @Override
    public TenantKey convert(String source) {
        if (StrUtil.isBlank(source)) {
            return null;
        }

        String[] parts = CharSequenceUtil.splitToArray(source, ":");
        if (parts.length != 2) {
            throw new IllegalArgumentException("租户标识格式不正确");
        }

        return new TenantKey(parts[0], Convert.toLong(parts[1]));
    }

}
```

文件位置：`src/main/java/io/github/atengk/demo/controller/CustomConverterController.java`

以下接口直接接收自定义值对象参数。

```java
package io.github.atengk.demo.controller;

import io.github.atengk.demo.common.response.ApiResult;
import io.github.atengk.demo.model.value.TenantKey;
import org.springframework.web.bind.annotation.*;

/**
 * 自定义 Converter 示例接口。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@RestController
@RequestMapping("/api/convert/custom")
public class CustomConverterController {

    /**
     * 转换租户标识。
     *
     * @param tenantKey 租户标识
     * @return 租户标识
     */
    @GetMapping("/tenant")
    public ApiResult<TenantKey> tenant(@RequestParam TenantKey tenantKey) {
        return ApiResult.success(tenantKey);
    }

}
```

调用示例：

```bash
curl "http://localhost:8080/api/convert/custom/tenant?tenantKey=tenant:1001"
```

自定义 Converter 建议：

- 适合简单、单向、无区域格式要求的转换。
- 转换失败应抛出明确异常。
- Converter 类交给 Spring 管理即可自动注册。
- 不要在 Converter 中访问数据库或复杂业务服务。
- 复杂解析逻辑建议放到专用解析器中，再由 Converter 调用。

### 自定义 Formatter

`Formatter` 同时支持字符串解析和格式化输出，适合日期、金额、编号等具有文本格式表达的类型。相比 Converter，Formatter 更关注“文本格式”。

示例：将 `20260509-1001` 转换为业务单号对象，并支持格式化输出。

文件位置：`src/main/java/io/github/atengk/demo/model/value/BizNo.java`

以下值对象表示业务单号。

```java
package io.github.atengk.demo.model.value;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 业务单号值对象。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BizNo {

    private String datePart;

    private Long sequence;

}
```

文件位置：`src/main/java/io/github/atengk/demo/common/formatter/BizNoFormatter.java`

以下 Formatter 支持业务单号的解析和格式化。

```java
package io.github.atengk.demo.common.formatter;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.demo.model.value.BizNo;
import org.springframework.format.Formatter;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.util.Locale;

/**
 * 业务单号格式化器。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Component
public class BizNoFormatter implements Formatter<BizNo> {

    /**
     * 解析业务单号。
     *
     * @param text 文本
     * @param locale 区域信息
     * @return 业务单号
     * @throws ParseException 解析异常
     */
    @Override
    public BizNo parse(String text, Locale locale) throws ParseException {
        if (StrUtil.isBlank(text)) {
            return null;
        }

        String[] parts = StrUtil.splitToArray(text, "-");
        if (parts.length != 2) {
            throw new ParseException("业务单号格式不正确", 0);
        }

        return new BizNo(parts[0], Convert.toLong(parts[1]));
    }

    /**
     * 格式化业务单号。
     *
     * @param object 业务单号
     * @param locale 区域信息
     * @return 文本
     */
    @Override
    public String print(BizNo object, Locale locale) {
        if (object == null) {
            return "";
        }
        return StrUtil.format("{}-{}", object.getDatePart(), object.getSequence());
    }

}
```

如果 Formatter 没有自动注册，可以在 Web MVC 配置中显式注册。

文件位置：`src/main/java/io/github/atengk/demo/config/WebMvcFormatConfig.java`

```java
package io.github.atengk.demo.config;

import io.github.atengk.demo.common.formatter.BizNoFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC 格式化器配置。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcFormatConfig implements WebMvcConfigurer {

    private final BizNoFormatter bizNoFormatter;

    /**
     * 注册格式化器。
     *
     * @param registry 格式化器注册器
     */
    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addFormatter(bizNoFormatter);
    }

}
```

文件位置：`src/main/java/io/github/atengk/demo/controller/FormatterDemoController.java`

以下接口接收业务单号参数。

```java
package io.github.atengk.demo.controller;

import io.github.atengk.demo.common.response.ApiResult;
import io.github.atengk.demo.model.value.BizNo;
import org.springframework.web.bind.annotation.*;

/**
 * Formatter 示例接口。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@RestController
@RequestMapping("/api/formatter")
public class FormatterDemoController {

    /**
     * 解析业务单号。
     *
     * @param bizNo 业务单号
     * @return 业务单号
     */
    @GetMapping("/biz-no")
    public ApiResult<BizNo> bizNo(@RequestParam BizNo bizNo) {
        return ApiResult.success(bizNo);
    }

}
```

调用示例：

```bash
curl "http://localhost:8080/api/formatter/biz-no?bizNo=20260509-1001"
```

Formatter 使用建议：

- 适合具有明确文本格式的对象。
- 既需要解析又需要格式化输出时使用 Formatter。
- 只需要单向转换时使用 Converter。
- 解析失败应抛出明确异常。
- 不要在 Formatter 中写复杂业务逻辑。

## 定时任务

定时任务用于按固定时间、固定频率或固定延迟执行后台任务，例如数据同步、缓存刷新、报表生成、临时文件清理、超时订单关闭、统计任务等。Spring Boot 可以通过 `@EnableScheduling` 和 `@Scheduled` 快速启用定时任务。

### 定时任务开启

Spring 定时任务需要在配置类或启动类上添加 `@EnableScheduling`。添加后，Spring 会扫描容器中带有 `@Scheduled` 的方法，并按配置执行。

文件位置：`src/main/java/io/github/atengk/demo/config/SchedulingConfig.java`

以下配置类启用定时任务。

```java
package io.github.atengk.demo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 定时任务配置。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
```

文件位置：`src/main/java/io/github/atengk/demo/task/SystemStatusTask.java`

以下任务每隔一段时间输出系统状态日志。

```java
package io.github.atengk.demo.task;

import cn.hutool.core.date.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 系统状态定时任务。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Component
public class SystemStatusTask {

    /**
     * 打印系统状态。
     */
    @Scheduled(fixedRate = 60000)
    public void printStatus() {
        log.info("系统运行状态正常，当前时间：{}", DateUtil.now());
    }

}
```

定时任务开启建议：

- 定时任务类使用 `@Component` 注册为 Bean。
- 定时任务方法通常使用 `public void`，不依赖外部入参。
- 定时任务中必须做好异常处理，避免异常影响后续执行。
- 多实例部署时要注意任务重复执行问题。
- 复杂分布式任务建议使用 XXL-JOB、Quartz、ElasticJob 或调度平台。

### Cron 表达式配置

Cron 表达式用于指定任务在某些固定时间点执行，适合每天凌晨统计、每小时同步、每周生成报表等任务。Spring 的 `@Scheduled(cron = "...")` 支持 6 位表达式，通常格式为：

```text
秒 分 时 日 月 周
```

常见 Cron 示例：

| 表达式           | 含义                |
| ---------------- | ------------------- |
| `0 0 2 * * ?`    | 每天凌晨 2 点执行   |
| `0 */5 * * * ?`  | 每 5 分钟执行一次   |
| `0 0/30 * * * ?` | 每 30 分钟执行一次  |
| `0 0 1 * * ?`    | 每天凌晨 1 点执行   |
| `0 0 9 ? * MON`  | 每周一上午 9 点执行 |

文件位置：`src/main/resources/application.yml`

以下配置将 Cron 表达式放入配置文件，方便不同环境调整。

```yaml
demo:
  task:
    cleanup:
      # 每天凌晨 2 点清理临时文件
      cron: "0 0 2 * * ?"
```

文件位置：`src/main/java/io/github/atengk/demo/task/CleanupTask.java`

以下任务从配置文件读取 Cron 表达式，定时清理临时文件。

```java
package io.github.atengk.demo.task;

import cn.hutool.core.date.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 临时文件清理定时任务。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Component
public class CleanupTask {

    /**
     * 清理临时文件。
     */
    @Scheduled(cron = "${demo.task.cleanup.cron}")
    public void cleanupTempFiles() {
        log.info("开始执行临时文件清理任务，时间：{}", DateUtil.now());

        // 示例中省略具体文件清理逻辑，实际项目应按目录、文件时间和业务状态清理

        log.info("临时文件清理任务执行完成，时间：{}", DateUtil.now());
    }

}
```

Cron 表达式建议：

- 表达式放入配置文件，不要硬编码在代码中。
- 生产环境任务时间避开业务高峰。
- 任务执行时间较长时，应评估是否会与下一次调度重叠。
- 多实例部署时需要防止重复执行。
- 关键任务开始和结束都要记录日志。
- Cron 表达式修改后通常需要重启应用，除非使用动态调度方案。

### 固定频率任务

固定频率任务使用 `fixedRate`，表示按照固定间隔触发任务。它的计时基准是上一次任务开始时间。适合心跳上报、状态刷新、短周期监控等任务。

文件位置：`src/main/java/io/github/atengk/demo/task/HeartbeatTask.java`

以下任务每 30 秒执行一次心跳日志。

```java
package io.github.atengk.demo.task;

import cn.hutool.core.date.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 心跳定时任务。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Component
public class HeartbeatTask {

    /**
     * 上报心跳。
     */
    @Scheduled(fixedRate = 30000)
    public void heartbeat() {
        log.info("执行应用心跳任务，当前时间：{}", DateUtil.now());
    }

}
```

也可以将固定频率放入配置文件。

文件位置：`src/main/resources/application.yml`

```yaml
demo:
  task:
    heartbeat:
      # 心跳任务执行间隔，单位毫秒
      fixed-rate: 30000
```

文件位置：`src/main/java/io/github/atengk/demo/task/ConfigHeartbeatTask.java`

```java
package io.github.atengk.demo.task;

import cn.hutool.core.date.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 配置化心跳定时任务。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Component
public class ConfigHeartbeatTask {

    /**
     * 配置化心跳任务。
     */
    @Scheduled(fixedRateString = "${demo.task.heartbeat.fixed-rate}")
    public void heartbeat() {
        log.info("执行配置化心跳任务，当前时间：{}", DateUtil.now());
    }

}
```

固定频率任务建议：

- 适合执行时间短、周期稳定的任务。
- 如果任务执行时间可能超过间隔，需要评估线程池和重叠风险。
- 固定频率参数建议配置化。
- 高频任务日志要克制，避免刷屏。
- 任务内部调用外部接口时必须设置超时。

### 固定延迟任务

固定延迟任务使用 `fixedDelay`，表示上一次任务执行完成后，等待指定时间再执行下一次。它的计时基准是上一次任务结束时间。适合轮询、补偿、清理、同步等不希望重叠执行的任务。

文件位置：`src/main/java/io/github/atengk/demo/task/RetryCompensationTask.java`

以下任务在每次执行完成后延迟 60 秒再次执行。

```java
package io.github.atengk.demo.task;

import cn.hutool.core.date.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 补偿重试定时任务。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Component
public class RetryCompensationTask {

    /**
     * 执行补偿重试。
     */
    @Scheduled(fixedDelay = 60000)
    public void retry() {
        log.info("开始执行补偿重试任务，时间：{}", DateUtil.now());

        // 示例中省略具体补偿逻辑，实际项目应查询待补偿数据并逐条处理

        log.info("补偿重试任务执行完成，时间：{}", DateUtil.now());
    }

}
```

支持首次延迟执行：

```java
@Scheduled(initialDelay = 10000, fixedDelay = 60000)
public void retryAfterStartup() {
    log.info("应用启动 10 秒后开始执行，之后每次完成后延迟 60 秒执行");
}
```

固定延迟任务建议：

- 适合不希望任务重叠的场景。
- 补偿任务、清理任务、轮询任务优先考虑 `fixedDelay`。
- 任务执行完成后才开始计算下一次延迟。
- 任务异常应捕获并记录，避免中断后续调度。
- 任务处理数据量应分批，避免单次执行时间过长。

### 定时任务异常处理

定时任务异常处理非常重要。任务方法中如果抛出未处理异常，可能导致本次执行失败，并影响后续调度表现。业务任务应在方法内部捕获异常、记录日志，并对单条数据失败和整体任务失败做区分。

方式一：任务内部捕获异常。

文件位置：`src/main/java/io/github/atengk/demo/task/SafeCleanupTask.java`

以下任务在方法内部捕获异常，避免异常扩散。

```java
package io.github.atengk.demo.task;

import cn.hutool.core.date.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 安全清理定时任务。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Component
public class SafeCleanupTask {

    /**
     * 安全执行清理任务。
     */
    @Scheduled(cron = "0 */10 * * * ?")
    public void cleanup() {
        log.info("安全清理任务开始，时间：{}", DateUtil.now());

        try {
            // 示例中省略具体清理逻辑
            log.info("安全清理任务完成，时间：{}", DateUtil.now());
        } catch (Exception e) {
            log.error("安全清理任务执行失败", e);
        }
    }

}
```

方式二：配置统一调度线程池和异常处理器。

默认定时任务执行器较简单。生产项目建议配置专用调度线程池，避免多个任务互相阻塞。

文件位置：`src/main/java/io/github/atengk/demo/config/TaskSchedulerConfig.java`

以下配置类提供定时任务线程池，并统一处理未捕获异常。

```java
package io.github.atengk.demo.config;

import cn.hutool.core.thread.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * 定时任务线程池配置。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Configuration
public class TaskSchedulerConfig {

    /**
     * 创建定时任务线程池。
     *
     * @return 定时任务线程池
     */
    @Bean
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(4);
        scheduler.setThreadFactory(ThreadFactoryBuilder.create().setNamePrefix("schedule-task-").build());
        scheduler.setErrorHandler(e -> log.error("定时任务执行异常", e));
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(30);
        scheduler.initialize();

        log.info("初始化定时任务线程池，线程数：{}", 4);
        return scheduler;
    }

}
```

方式三：单条数据异常不影响整个批次。

文件位置：`src/main/java/io/github/atengk/demo/task/BatchSyncTask.java`

以下任务对每条数据单独捕获异常，避免一条失败导致整批中断。

```java
package io.github.atengk.demo.task;

import cn.hutool.core.collection.CollUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 批量同步定时任务。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Component
public class BatchSyncTask {

    /**
     * 批量同步数据。
     */
    @Scheduled(cron = "0 */5 * * * ?")
    public void sync() {
        List<Long> ids = loadPendingIds();
        if (CollUtil.isEmpty(ids)) {
            log.info("暂无待同步数据");
            return;
        }

        log.info("开始批量同步数据，数量：{}", ids.size());

        for (Long id : ids) {
            try {
                syncOne(id);
                log.info("单条数据同步成功，ID：{}", id);
            } catch (Exception e) {
                log.error("单条数据同步失败，ID：{}", id, e);
            }
        }

        log.info("批量同步数据完成，数量：{}", ids.size());
    }

    private List<Long> loadPendingIds() {
        return List.of(1001L, 1002L, 1003L);
    }

    private void syncOne(Long id) {
        // 示例中省略具体同步逻辑
    }

}
```

定时任务异常处理建议：

- 任务开始和结束都记录日志。
- 任务整体异常必须捕获并记录。
- 批量任务中单条失败不应影响整批，除非业务要求强一致。
- 任务调用外部系统时必须设置超时和重试上限。
- 多实例部署时要防止任务重复执行，可以使用分布式锁或调度平台。
- 长时间任务应分页处理，避免一次加载过多数据。
- 关键任务建议记录执行结果表，便于追踪成功、失败、耗时和错误原因。



## 异步任务

异步任务用于将耗时操作从主请求线程中拆分出去执行，例如发送通知、写操作日志、生成报表、同步第三方系统、处理文件、发送邮件等。Spring Boot 可以通过 `@EnableAsync` 和 `@Async` 快速启用异步执行能力。异步任务能降低接口响应时间，但不能替代消息队列，也不能用于强一致业务流程。

### 异步任务开启

启用异步任务需要在配置类或启动类上添加 `@EnableAsync`。添加后，Spring 会为标注了 `@Async` 的 Bean 方法创建异步执行代理。

文件位置：`src/main/java/io/github/atengk/demo/config/AsyncConfig.java`

以下配置类启用异步任务，并配置默认异步异常处理。

```java
package io.github.atengk.demo.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 异步任务配置。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    /**
     * 配置无返回值异步方法的异常处理器。
     *
     * @return 异步异常处理器
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (throwable, method, objects) -> log.error("异步任务执行异常，方法：{}", method.getName(), throwable);
    }

}
```

异步任务开启建议：

- 配置类上添加 `@EnableAsync`。
- 异步方法所在类必须交给 Spring 容器管理。
- `@Async` 方法必须通过 Spring 代理调用才会生效。
- 不建议在 Controller 中直接编写异步业务逻辑。
- 关键异步任务建议使用自定义线程池，不使用默认执行器。
- 需要可靠投递、失败重试、削峰填谷时，应优先使用 MQ 或任务调度平台。

### @Async 使用

`@Async` 用于标记某个方法异步执行。调用方调用该方法后，不会等待方法执行完成，而是立即继续向下执行。适合发送通知、记录审计日志、刷新缓存等不影响主流程结果的操作。

文件位置：`src/main/java/io/github/atengk/demo/service/NotifyAsyncService.java`

以下 Service 使用 `@Async` 异步发送通知。

```java
package io.github.atengk.demo.service;

import cn.hutool.core.date.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 异步通知业务组件。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Service
public class NotifyAsyncService {

    /**
     * 异步发送通知。
     *
     * @param userId 用户 ID
     * @param content 通知内容
     */
    @Async
    public void sendNotify(Long userId, String content) {
        log.info("开始异步发送通知，用户ID：{}，内容：{}，时间：{}", userId, content, DateUtil.now());

        // 示例中省略短信、邮件或站内信发送逻辑

        log.info("异步通知发送完成，用户ID：{}", userId);
    }

}
```

文件位置：`src/main/java/io/github/atengk/demo/service/UserRegisterService.java`

以下业务方法在用户注册成功后异步发送通知。

```java
package io.github.atengk.demo.service;

import io.github.atengk.demo.model.dto.UserCreateDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 用户注册业务组件。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserRegisterService {

    private final NotifyAsyncService notifyAsyncService;

    /**
     * 注册用户。
     *
     * @param dto 用户创建参数
     */
    public void register(UserCreateDTO dto) {
        Long userId = 1001L;
        log.info("用户注册成功，用户ID：{}，用户名：{}", userId, dto.getUsername());

        notifyAsyncService.sendNotify(userId, "欢迎注册系统");
        log.info("注册主流程已完成，通知任务已提交");
    }

}
```

`@Async` 使用建议：

- 异步方法必须放在 Spring Bean 中。
- 不能在同一个类内部直接调用自己的 `@Async` 方法，否则可能不生效。
- 异步方法不应依赖当前请求线程中的 `ThreadLocal`，除非显式传递上下文。
- 异步任务不适合处理必须立即成功的强一致业务。
- 异步任务中的异常不会直接抛给调用方，需要单独处理。
- 方法可返回 `void`、`Future`、`CompletableFuture` 等类型。

错误示例：同类内部调用导致异步不生效。

```java
@Service
public class WrongAsyncService {

    public void submit() {
        // 同类内部调用，不经过 Spring 代理，@Async 可能不生效
        this.doAsync();
    }

    @Async
    public void doAsync() {
    }

}
```

推荐做法是拆分到另一个 Service 中，通过注入的 Bean 调用异步方法。

### 线程池配置

生产项目不建议直接使用默认异步执行器。应根据业务类型配置独立线程池，控制核心线程数、最大线程数、队列容量、线程名前缀和拒绝策略。否则在高并发或任务堆积时，异步任务可能影响应用稳定性。

文件位置：`src/main/java/io/github/atengk/demo/config/AsyncThreadPoolConfig.java`

以下配置类定义应用异步线程池，并使用 Hutool 创建线程工厂。

```java
package io.github.atengk.demo.config;

import cn.hutool.core.thread.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步线程池配置。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Configuration
public class AsyncThreadPoolConfig {

    /**
     * 应用异步任务线程池。
     *
     * @return 异步任务线程池
     */
    @Bean("applicationTaskExecutor")
    public Executor applicationTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(8);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(500);
        executor.setKeepAliveSeconds(60);
        executor.setThreadFactory(ThreadFactoryBuilder.create().setNamePrefix("app-async-").build());
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();

        log.info("初始化应用异步线程池，核心线程数：{}，最大线程数：{}，队列容量：{}", 8, 16, 500);
        return executor;
    }

}
```

使用指定线程池：

```java
@Async("applicationTaskExecutor")
public void sendNotify(Long userId, String content) {
    log.info("使用指定线程池执行异步通知，用户ID：{}", userId);
}
```

线程池配置建议：

- 不同类型任务可以使用不同线程池，例如通知线程池、报表线程池、文件处理线程池。
- 线程名前缀要清晰，便于日志和线程栈排查。
- 队列容量不能无限大，否则任务堆积会导致内存压力。
- 拒绝策略要结合业务选择，常见选择是 `CallerRunsPolicy` 或自定义记录失败。
- 线程池参数应根据 CPU、任务耗时、并发量和外部依赖能力评估。
- 异步任务访问外部接口时必须设置超时时间。

### 异步异常处理

异步异常分两种：无返回值异步方法异常和有返回值异步方法异常。`void` 异步方法中的异常不会抛回调用方，需要通过 `AsyncUncaughtExceptionHandler` 统一处理。有返回值的 `CompletableFuture` 异常，需要调用方通过 `exceptionally`、`handle` 或 `get` 处理。

文件位置：`src/main/java/io/github/atengk/demo/config/AsyncExceptionConfig.java`

以下配置类统一处理无返回值异步方法异常。

```java
package io.github.atengk.demo.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.Arrays;

/**
 * 异步异常处理配置。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncExceptionConfig implements AsyncConfigurer {

    /**
     * 获取异步未捕获异常处理器。
     *
     * @return 异步异常处理器
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (throwable, method, params) -> log.error(
                "无返回值异步任务执行异常，方法：{}，参数：{}",
                method.getName(),
                Arrays.toString(params),
                throwable
        );
    }

}
```

文件位置：`src/main/java/io/github/atengk/demo/service/AsyncErrorDemoService.java`

以下 Service 演示异步方法内部异常处理。

```java
package io.github.atengk.demo.service;

import io.github.atengk.demo.common.exception.BizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 异步异常示例业务组件。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Service
public class AsyncErrorDemoService {

    /**
     * 无返回值异步任务。
     *
     * @param taskId 任务 ID
     */
    @Async("applicationTaskExecutor")
    public void executeVoidTask(Long taskId) {
        log.info("开始执行无返回值异步任务，任务ID：{}", taskId);
        throw new BizException("异步任务执行失败");
    }

}
```

异步异常处理建议：

- `void` 异步方法异常使用 `AsyncUncaughtExceptionHandler` 统一记录。
- `CompletableFuture` 异步方法异常由调用方处理。
- 异步任务内部可以对单条数据单独捕获异常，避免整批中断。
- 重要异步任务失败应落库或投递失败队列，便于补偿。
- 不要认为异步方法抛异常会自动影响主请求结果。
- 异步任务异常日志中应记录任务 ID、业务 ID 和 TraceId。

### 异步返回结果处理

异步方法可以返回 `CompletableFuture<T>`，调用方可以并行发起多个异步任务，再统一等待结果。这适合多个互不依赖的查询、远程调用和数据聚合场景。

文件位置：`src/main/java/io/github/atengk/demo/service/AsyncQueryService.java`

以下 Service 提供两个异步查询方法。

```java
package io.github.atengk.demo.service;

import cn.hutool.core.map.MapUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 异步查询业务组件。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Service
public class AsyncQueryService {

    /**
     * 异步查询用户信息。
     *
     * @param userId 用户 ID
     * @return 用户信息
     */
    @Async("applicationTaskExecutor")
    public CompletableFuture<Map<String, Object>> queryUser(Long userId) {
        log.info("异步查询用户信息，用户ID：{}", userId);

        Map<String, Object> data = MapUtil.<String, Object>builder()
                .put("userId", userId)
                .put("username", "ateng")
                .build();

        return CompletableFuture.completedFuture(data);
    }

    /**
     * 异步查询订单统计。
     *
     * @param userId 用户 ID
     * @return 订单统计
     */
    @Async("applicationTaskExecutor")
    public CompletableFuture<Map<String, Object>> queryOrderSummary(Long userId) {
        log.info("异步查询订单统计，用户ID：{}", userId);

        Map<String, Object> data = MapUtil.<String, Object>builder()
                .put("orderCount", 10)
                .put("paidAmount", "199.00")
                .build();

        return CompletableFuture.completedFuture(data);
    }

}
```

文件位置：`src/main/java/io/github/atengk/demo/service/UserDashboardService.java`

以下 Service 并行执行两个异步查询并合并结果。

```java
package io.github.atengk.demo.service;

import cn.hutool.core.map.MapUtil;
import io.github.atengk.demo.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 用户看板业务组件。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserDashboardService {

    private final AsyncQueryService asyncQueryService;

    /**
     * 获取用户看板数据。
     *
     * @param userId 用户 ID
     * @return 用户看板数据
     */
    public Map<String, Object> getDashboard(Long userId) {
        CompletableFuture<Map<String, Object>> userFuture = asyncQueryService.queryUser(userId);
        CompletableFuture<Map<String, Object>> orderFuture = asyncQueryService.queryOrderSummary(userId);

        try {
            CompletableFuture.allOf(userFuture, orderFuture).join();

            return MapUtil.<String, Object>builder()
                    .put("user", userFuture.join())
                    .put("orderSummary", orderFuture.join())
                    .build();
        } catch (Exception e) {
            log.error("获取用户看板数据失败，用户ID：{}", userId, e);
            throw new BizException("获取用户看板数据失败");
        }
    }

}
```

异步返回结果处理建议：

- 多个无依赖查询可以并行执行。
- 使用 `CompletableFuture.allOf()` 等待多个任务完成。
- 调用 `join()` 或 `get()` 时要处理异常。
- 异步任务返回值不要过大，避免占用过多内存。
- 需要超时控制时，应结合 `orTimeout`、`completeOnTimeout` 或外部调用超时。
- 对强一致写操作，不建议简单使用异步聚合替代事务。

## 应用事件

应用事件是 Spring 提供的一种轻量级解耦机制。业务代码可以发布事件，其他组件监听事件并执行后续动作。事件适合处理用户注册后发送通知、订单创建后写审计日志、文件上传后生成缩略图、配置变更后刷新缓存等场景。

### 事件对象设计

事件对象用于承载业务发生的事实。事件命名应表达已经发生的动作，例如 `UserCreatedEvent`、`OrderPaidEvent`、`FileUploadedEvent`。事件对象中只放监听方需要的数据，不放复杂业务服务对象。

文件位置：`src/main/java/io/github/atengk/demo/event/UserCreatedEvent.java`

以下事件表示用户创建完成。

```java
package io.github.atengk.demo.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 用户创建完成事件。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Getter
public class UserCreatedEvent extends ApplicationEvent {

    private final Long userId;

    private final String username;

    /**
     * 创建用户创建完成事件。
     *
     * @param source 事件源
     * @param userId 用户 ID
     * @param username 用户名
     */
    public UserCreatedEvent(Object source, Long userId, String username) {
        super(source);
        this.userId = userId;
        this.username = username;
    }

}
```

也可以使用普通对象作为事件，不继承 `ApplicationEvent`。

文件位置：`src/main/java/io/github/atengk/demo/event/OrderCreatedEvent.java`

以下事件对象表示订单创建完成。

```java
package io.github.atengk.demo.event;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单创建完成事件。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
@AllArgsConstructor
public class OrderCreatedEvent {

    private Long orderId;

    private Long userId;

    private BigDecimal amount;

}
```

事件对象设计建议：

- 使用过去式命名，表达“某事已经发生”。
- 事件字段保持简单，只携带必要业务标识。
- 不要在事件对象中注入 Service、Repository 等 Bean。
- 事件对象可以继承 `ApplicationEvent`，也可以是普通 POJO。
- 事件不应承担命令职责，命令是“要求做什么”，事件是“已经发生什么”。

### 事件发布

事件发布通过 `ApplicationEventPublisher` 完成。业务代码在关键动作完成后发布事件，监听器接收到事件后执行扩展逻辑。

文件位置：`src/main/java/io/github/atengk/demo/service/UserEventService.java`

以下 Service 在用户创建成功后发布用户创建事件。

```java
package io.github.atengk.demo.service;

import io.github.atengk.demo.event.UserCreatedEvent;
import io.github.atengk.demo.model.dto.UserCreateDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

/**
 * 用户事件业务组件。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserEventService {

    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * 创建用户并发布事件。
     *
     * @param dto 用户创建参数
     * @return 用户 ID
     */
    public Long createUser(UserCreateDTO dto) {
        Long userId = 1001L;
        log.info("用户创建成功，用户ID：{}，用户名：{}", userId, dto.getUsername());

        applicationEventPublisher.publishEvent(new UserCreatedEvent(this, userId, dto.getUsername()));
        log.info("用户创建事件已发布，用户ID：{}", userId);

        return userId;
    }

}
```

发布普通 POJO 事件：

```java
applicationEventPublisher.publishEvent(new OrderCreatedEvent(orderId, userId, amount));
```

事件发布建议：

- 在业务状态真正完成后再发布事件。
- 事件发布点应清晰，避免同一业务重复发布。
- 如果事件监听逻辑依赖事务提交结果，应使用事务事件监听。
- 发布事件不应替代核心业务流程。
- 事件发布失败或监听失败的处理策略需要结合业务重要性设计。

### 事件监听

事件监听通过 `@EventListener` 实现。监听器可以接收事件并执行后续逻辑，例如发送通知、写日志、刷新缓存等。默认情况下，事件监听是同步执行的，即发布事件的方法会等待监听器执行完成。

文件位置：`src/main/java/io/github/atengk/demo/listener/UserCreatedListener.java`

以下监听器监听用户创建完成事件，并执行通知逻辑。

```java
package io.github.atengk.demo.listener;

import io.github.atengk.demo.event.UserCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 用户创建事件监听器。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Component
public class UserCreatedListener {

    /**
     * 处理用户创建完成事件。
     *
     * @param event 用户创建完成事件
     */
    @EventListener
    public void handleUserCreated(UserCreatedEvent event) {
        log.info("监听到用户创建事件，用户ID：{}，用户名：{}", event.getUserId(), event.getUsername());

        // 示例中省略发送欢迎消息、初始化用户配置等逻辑

        log.info("用户创建事件处理完成，用户ID：{}", event.getUserId());
    }

}
```

监听普通 POJO 事件：

文件位置：`src/main/java/io/github/atengk/demo/listener/OrderCreatedListener.java`

```java
package io.github.atengk.demo.listener;

import io.github.atengk.demo.event.OrderCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 订单创建事件监听器。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Component
public class OrderCreatedListener {

    /**
     * 处理订单创建事件。
     *
     * @param event 订单创建事件
     */
    @EventListener
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("监听到订单创建事件，订单ID：{}，用户ID：{}，金额：{}",
                event.getOrderId(),
                event.getUserId(),
                event.getAmount());
    }

}
```

事件监听建议：

- 监听器类使用 `@Component` 注册。
- 监听方法使用 `@EventListener`。
- 默认同步监听器中的异常会影响事件发布流程。
- 监听器中不要写过重逻辑，耗时任务建议异步处理。
- 多个监听器之间不应依赖执行顺序，除非明确使用排序机制。
- 监听器应关注单一职责，例如通知、审计、缓存刷新分开写。

### 异步事件监听

异步事件监听适合处理不需要阻塞主流程的后续动作，例如发送通知、生成日志、刷新缓存等。可以在监听方法上同时使用 `@Async` 和 `@EventListener`。使用前需要启用异步任务。

文件位置：`src/main/java/io/github/atengk/demo/listener/AsyncUserCreatedListener.java`

以下监听器异步处理用户创建事件。

```java
package io.github.atengk.demo.listener;

import io.github.atengk.demo.event.UserCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 异步用户创建事件监听器。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Component
public class AsyncUserCreatedListener {

    /**
     * 异步处理用户创建事件。
     *
     * @param event 用户创建事件
     */
    @Async("applicationTaskExecutor")
    @EventListener
    public void handleUserCreated(UserCreatedEvent event) {
        log.info("开始异步处理用户创建事件，用户ID：{}", event.getUserId());

        // 示例中省略异步发送消息、初始化资源等逻辑

        log.info("异步处理用户创建事件完成，用户ID：{}", event.getUserId());
    }

}
```

如果事件发布发生在事务方法中，并且监听器必须等事务提交后再执行，可以使用 `@TransactionalEventListener`。

文件位置：`src/main/java/io/github/atengk/demo/listener/UserCreatedTransactionListener.java`

以下监听器在事务提交后处理事件。

```java
package io.github.atengk.demo.listener;

import io.github.atengk.demo.event.UserCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 用户创建事务事件监听器。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Component
public class UserCreatedTransactionListener {

    /**
     * 事务提交后异步处理用户创建事件。
     *
     * @param event 用户创建事件
     */
    @Async("applicationTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void afterCommit(UserCreatedEvent event) {
        log.info("事务提交后处理用户创建事件，用户ID：{}", event.getUserId());
    }

}
```

异步事件监听建议：

- 异步事件需要启用 `@EnableAsync`。
- 监听器异常不会直接返回给事件发布方，应记录日志并做好补偿。
- 依赖数据库提交结果的事件，优先使用 `@TransactionalEventListener(phase = AFTER_COMMIT)`。
- 异步事件不能保证强一致，关键业务应设计补偿机制。
- 多实例部署下，应用事件只在当前 JVM 内传播，不是分布式事件。
- 跨服务事件应使用 MQ、事件总线或消息中间件。

### 业务解耦场景

应用事件适合处理同一个应用内的轻量解耦。它可以让主业务流程只关注核心动作，将扩展动作交给监听器处理，降低 Service 之间的直接依赖。

适合使用应用事件的场景：

| 场景     | 主流程   | 监听器                   |
| -------- | -------- | ------------------------ |
| 用户注册 | 创建用户 | 发送欢迎消息、初始化配置 |
| 订单创建 | 保存订单 | 写审计日志、发送通知     |
| 文件上传 | 保存文件 | 生成缩略图、扫描文件     |
| 配置变更 | 更新配置 | 刷新本地缓存             |
| 登录成功 | 创建会话 | 记录登录日志             |

不适合使用应用事件的场景：

- 跨服务可靠事件通知。
- 必须保证投递成功的关键业务。
- 大批量异步削峰任务。
- 需要失败重试、死信队列、消费确认的任务。
- 多实例之间需要广播的事件。

文件位置：`src/main/java/io/github/atengk/demo/service/OrderEventService.java`

以下 Service 在订单创建后发布事件，把通知和审计逻辑从主流程中拆出去。

```java
package io.github.atengk.demo.service;

import io.github.atengk.demo.event.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 订单事件业务组件。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderEventService {

    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * 创建订单。
     *
     * @param userId 用户 ID
     * @param amount 订单金额
     * @return 订单 ID
     */
    public Long createOrder(Long userId, BigDecimal amount) {
        Long orderId = 9001L;
        log.info("订单创建成功，订单ID：{}，用户ID：{}，金额：{}", orderId, userId, amount);

        applicationEventPublisher.publishEvent(new OrderCreatedEvent(orderId, userId, amount));
        log.info("订单创建事件已发布，订单ID：{}", orderId);

        return orderId;
    }

}
```

业务解耦建议：

- 主流程只处理核心状态变更。
- 扩展动作放入监听器。
- 事件命名表达业务事实。
- 事件监听失败不能影响核心链路时，应使用异步监听。
- 事件要求事务提交后执行时，使用事务事件监听。
- 超出单体应用范围时，升级为 MQ 或事件总线。

## 应用启动任务

应用启动任务用于在 Spring Boot 应用启动完成后执行初始化逻辑，例如读取启动参数、初始化字典缓存、检查依赖服务、创建默认数据、预热本地缓存等。Spring Boot 常用 `CommandLineRunner` 和 `ApplicationRunner` 实现启动任务。

### CommandLineRunner 使用

`CommandLineRunner` 会在 Spring Boot 应用启动完成后执行，参数是原始字符串数组 `String[] args`。它适合处理简单启动参数，或者只需要在启动后执行一段初始化逻辑的场景。

文件位置：`src/main/java/io/github/atengk/demo/runner/SystemCommandLineRunner.java`

以下 Runner 在应用启动完成后输出原始启动参数。

```java
package io.github.atengk.demo.runner;

import cn.hutool.core.util.ArrayUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 命令行启动任务。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Component
public class SystemCommandLineRunner implements CommandLineRunner {

    /**
     * 应用启动完成后执行。
     *
     * @param args 原始启动参数
     */
    @Override
    public void run(String... args) {
        if (ArrayUtil.isEmpty(args)) {
            log.info("CommandLineRunner 未接收到启动参数");
            return;
        }

        log.info("CommandLineRunner 接收到启动参数：{}", String.join(",", args));
    }

}
```

启动示例：

```bash
java -jar spring-boot3-demo.jar --server.port=8080 --spring.profiles.active=dev customArg
```

`CommandLineRunner` 使用建议：

- 适合简单启动任务。
- 参数是原始字符串数组，需要自己解析。
- 多个 Runner 可以通过 `@Order` 控制执行顺序。
- 不建议在 Runner 中执行长时间阻塞任务。
- 初始化失败会影响应用启动，应明确处理异常。

### ApplicationRunner 使用

`ApplicationRunner` 与 `CommandLineRunner` 类似，也是在应用启动完成后执行，但它接收的是 `ApplicationArguments`，可以更方便地读取选项参数和非选项参数。

文件位置：`src/main/java/io/github/atengk/demo/runner/SystemApplicationRunner.java`

以下 Runner 使用 `ApplicationArguments` 读取启动参数。

```java
package io.github.atengk.demo.runner;

import cn.hutool.core.collection.CollUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * 应用启动参数任务。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Component
public class SystemApplicationRunner implements ApplicationRunner {

    /**
     * 应用启动完成后执行。
     *
     * @param args 应用启动参数
     */
    @Override
    public void run(ApplicationArguments args) {
        Set<String> optionNames = args.getOptionNames();
        if (CollUtil.isEmpty(optionNames)) {
            log.info("ApplicationRunner 未接收到选项参数");
            return;
        }

        for (String optionName : optionNames) {
            List<String> values = args.getOptionValues(optionName);
            log.info("启动选项参数，name：{}，values：{}", optionName, values);
        }

        if (CollUtil.isNotEmpty(args.getNonOptionArgs())) {
            log.info("非选项启动参数：{}", args.getNonOptionArgs());
        }
    }

}
```

启动示例：

```bash
java -jar spring-boot3-demo.jar --init-cache=true --tenant=default file1 file2
```

`ApplicationRunner` 使用建议：

- 需要解析 `--key=value` 参数时优先使用 `ApplicationRunner`。
- `getOptionNames()` 获取选项参数名称。
- `getOptionValues()` 获取选项参数值。
- `getNonOptionArgs()` 获取非选项参数。
- 复杂启动参数应封装为配置属性，不建议长期依赖命令行解析。

### 启动初始化数据

启动初始化数据用于在应用启动后加载必要基础数据，例如字典缓存、系统配置、默认租户、默认管理员、地区数据等。初始化任务应具备幂等性，避免应用每次启动都重复插入数据。

文件位置：`src/main/java/io/github/atengk/demo/service/SystemDictCacheService.java`

以下 Service 模拟加载系统字典缓存。

```java
package io.github.atengk.demo.service;

import cn.hutool.core.map.MapUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 系统字典缓存业务组件。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Service
public class SystemDictCacheService {

    private final Map<String, String> dictCache = new ConcurrentHashMap<>();

    /**
     * 初始化字典缓存。
     */
    public void initCache() {
        dictCache.put("user_status:1", "启用");
        dictCache.put("user_status:0", "禁用");
        dictCache.put("order_status:paid", "已支付");

        log.info("系统字典缓存初始化完成，缓存数量：{}", dictCache.size());
    }

    /**
     * 获取字典值。
     *
     * @param key 字典键
     * @return 字典值
     */
    public String get(String key) {
        return MapUtil.getStr(dictCache, key);
    }

}
```

文件位置：`src/main/java/io/github/atengk/demo/runner/DataInitRunner.java`

以下 Runner 在应用启动后初始化字典缓存。

```java
package io.github.atengk.demo.runner;

import io.github.atengk.demo.service.SystemDictCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 启动数据初始化任务。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class DataInitRunner implements ApplicationRunner {

    private final SystemDictCacheService systemDictCacheService;

    /**
     * 应用启动完成后初始化数据。
     *
     * @param args 启动参数
     */
    @Override
    public void run(ApplicationArguments args) {
        log.info("开始执行启动数据初始化任务");
        systemDictCacheService.initCache();
        log.info("启动数据初始化任务执行完成");
    }

}
```

如果初始化数据库默认数据，需要先检查是否存在，再决定是否插入。

文件位置：`src/main/java/io/github/atengk/demo/service/DefaultAdminInitService.java`

以下 Service 演示默认管理员初始化的幂等处理。

```java
package io.github.atengk.demo.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 默认管理员初始化业务组件。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Service
public class DefaultAdminInitService {

    /**
     * 初始化默认管理员。
     */
    public void initDefaultAdmin() {
        boolean exists = checkAdminExists();
        if (exists) {
            log.info("默认管理员已存在，跳过初始化");
            return;
        }

        // 示例中省略数据库插入逻辑
        log.info("默认管理员初始化完成");
    }

    private boolean checkAdminExists() {
        return true;
    }

}
```

启动初始化数据建议：

- 初始化逻辑必须幂等。
- 初始化任务应记录开始、结束和失败日志。
- 关键初始化失败时应阻止应用启动。
- 非关键初始化失败可以降级，但必须记录告警日志。
- 大批量初始化不建议放在应用启动阶段。
- 数据库结构初始化建议使用 Flyway、Liquibase 或专门发布脚本。

### 启动参数读取

启动参数读取用于根据启动命令控制初始化行为，例如是否刷新缓存、是否执行修复任务、是否跳过某些初始化。`ApplicationArguments` 更适合读取这类参数。

文件位置：`src/main/java/io/github/atengk/demo/runner/StartupArgumentRunner.java`

以下 Runner 读取 `--init-cache=true` 和 `--repair-mode=true` 参数。

```java
package io.github.atengk.demo.runner;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.collection.CollUtil;
import io.github.atengk.demo.service.SystemDictCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 启动参数处理任务。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StartupArgumentRunner implements ApplicationRunner {

    private final SystemDictCacheService systemDictCacheService;

    /**
     * 根据启动参数执行初始化逻辑。
     *
     * @param args 启动参数
     */
    @Override
    public void run(ApplicationArguments args) {
        boolean initCache = getBooleanOption(args, "init-cache", false);
        boolean repairMode = getBooleanOption(args, "repair-mode", false);

        log.info("启动参数读取完成，init-cache：{}，repair-mode：{}", initCache, repairMode);

        if (initCache) {
            systemDictCacheService.initCache();
            log.info("根据启动参数完成缓存初始化");
        }

        if (repairMode) {
            log.warn("当前应用以修复模式启动，请确认是否符合预期");
        }
    }

    private boolean getBooleanOption(ApplicationArguments args, String name, boolean defaultValue) {
        if (!args.containsOption(name)) {
            return defaultValue;
        }

        List<String> values = args.getOptionValues(name);
        if (CollUtil.isEmpty(values)) {
            return true;
        }

        return Convert.toBool(values.get(0), defaultValue);
    }

}
```

启动命令示例：

```bash
java -jar spring-boot3-demo.jar \
  --spring.profiles.active=prod \
  --init-cache=true \
  --repair-mode=false
```

启动参数读取建议：

- 临时开关可以使用启动参数。
- 长期配置应放入配置文件或配置中心。
- 启动参数应在日志中输出摘要，便于排查。
- 敏感参数不要通过命令行传递，因为可能被进程列表看到。
- 参数解析要提供默认值，避免空值导致启动异常。
- 高风险参数，例如修复模式、清理模式，应输出 `warn` 日志。

### 初始化失败处理

初始化失败处理用于决定启动任务失败后应用是否继续启动。不同初始化任务的重要性不同：核心配置、数据库连接、基础字典等失败可能应阻止启动；非核心缓存预热、通知模板加载等失败可以允许应用继续运行，但必须记录告警日志。

常见处理策略：

| 初始化类型       | 失败处理               |
| ---------------- | ---------------------- |
| 核心配置校验     | 阻止启动               |
| 数据库必要数据   | 阻止启动或进入只读模式 |
| 本地缓存预热     | 可继续启动，后续懒加载 |
| 第三方接口检查   | 可告警，不一定阻止启动 |
| 临时文件清理     | 可继续启动             |
| 默认管理员初始化 | 视业务要求决定         |

方式一：关键初始化失败直接抛出异常，阻止应用启动。

文件位置：`src/main/java/io/github/atengk/demo/runner/CriticalInitRunner.java`

以下 Runner 在关键初始化失败时抛出异常。

```java
package io.github.atengk.demo.runner;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * 关键初始化任务。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Component
public class CriticalInitRunner implements ApplicationRunner {

    private final Environment environment;

    /**
     * 创建关键初始化任务。
     *
     * @param environment 环境对象
     */
    public CriticalInitRunner(Environment environment) {
        this.environment = environment;
    }

    /**
     * 执行关键初始化检查。
     *
     * @param args 启动参数
     */
    @Override
    public void run(ApplicationArguments args) {
        String applicationName = environment.getProperty("spring.application.name");
        if (StrUtil.isBlank(applicationName)) {
            log.error("关键配置缺失，spring.application.name 不能为空");
            throw new IllegalStateException("关键配置缺失：spring.application.name");
        }

        log.info("关键初始化检查通过，应用名称：{}", applicationName);
    }

}
```

方式二：非关键初始化失败记录错误，但不阻止应用启动。

文件位置：`src/main/java/io/github/atengk/demo/runner/NonCriticalInitRunner.java`

以下 Runner 在非关键初始化失败时只记录日志。

```java
package io.github.atengk.demo.runner;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 非关键初始化任务。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Component
public class NonCriticalInitRunner implements ApplicationRunner {

    /**
     * 执行非关键初始化任务。
     *
     * @param args 启动参数
     */
    @Override
    public void run(ApplicationArguments args) {
        try {
            warmLocalCache();
            log.info("非关键初始化任务执行完成");
        } catch (Exception e) {
            log.error("非关键初始化任务执行失败，应用继续启动", e);
        }
    }

    private void warmLocalCache() {
        // 示例中省略本地缓存预热逻辑
    }

}
```

方式三：统一封装初始化任务执行结果。

文件位置：`src/main/java/io/github/atengk/demo/service/StartupInitService.java`

以下 Service 将多个初始化步骤拆分执行，并清晰记录结果。

```java
package io.github.atengk.demo.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 启动初始化业务组件。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Service
public class StartupInitService {

    /**
     * 执行核心初始化。
     */
    public void initCriticalResources() {
        log.info("开始初始化核心资源");

        // 示例中省略数据库连接检查、必要配置校验等逻辑

        log.info("核心资源初始化完成");
    }

    /**
     * 执行非核心初始化。
     */
    public void initOptionalResources() {
        try {
            log.info("开始初始化非核心资源");

            // 示例中省略缓存预热、模板加载等逻辑

            log.info("非核心资源初始化完成");
        } catch (Exception e) {
            log.error("非核心资源初始化失败，应用继续运行", e);
        }
    }

}
```

文件位置：`src/main/java/io/github/atengk/demo/runner/StartupInitRunner.java`

以下 Runner 统一调用启动初始化服务。

```java
package io.github.atengk.demo.runner;

import io.github.atengk.demo.service.StartupInitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 应用启动初始化任务。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Component
@Order(10)
@RequiredArgsConstructor
public class StartupInitRunner implements ApplicationRunner {

    private final StartupInitService startupInitService;

    /**
     * 执行启动初始化。
     *
     * @param args 启动参数
     */
    @Override
    public void run(ApplicationArguments args) {
        log.info("开始执行应用启动初始化任务");

        startupInitService.initCriticalResources();
        startupInitService.initOptionalResources();

        log.info("应用启动初始化任务执行完成");
    }

}
```

初始化失败处理建议：

- 区分关键初始化和非关键初始化。
- 关键初始化失败应抛出异常阻止启动。
- 非关键初始化失败可以继续启动，但必须记录 `error` 日志。
- 初始化任务必须具备幂等性。
- 初始化顺序明确时使用 `@Order`。
- 不要在启动阶段执行超长任务。
- 重要初始化结果可以暴露到健康检查或应用信息接口中。



## Actuator 运行监控

Actuator 是 Spring Boot 提供的运行监控和应用管理能力，可以用于健康检查、应用信息查看、指标采集、线程信息、Bean 信息、环境信息等场景。生产环境中应谨慎暴露端点，只开放必要接口，并结合认证、网关、防火墙或内网访问控制。

### 健康检查接口

健康检查接口用于判断应用当前是否可用，常用于负载均衡、Kubernetes 探针、监控告警和发布验证。Actuator 默认提供 `/actuator/health` 端点，用于返回应用健康状态。Spring Boot 的 `health` 端点会汇总应用上下文中的 `HealthIndicator`，例如数据源、磁盘空间、Redis 等组件的健康状态；健康详情是否展示由 `management.endpoint.health.show-details` 控制。([Home](https://docs.spring.io/spring-boot/3.5/reference/actuator/endpoints.html?utm_source=chatgpt.com))

文件位置：`pom.xml`

以下依赖用于启用 Actuator 运行监控能力。

```xml
<dependencies>
    <!-- Actuator：提供健康检查、应用信息、指标监控等运行端点 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
</dependencies>
```

文件位置：`src/main/resources/application.yml`

以下配置暴露 `health` 和 `info` 端点，并控制健康检查详情展示方式。

```yaml
management:
  endpoints:
    web:
      exposure:
        # 暴露健康检查和应用信息端点
        include: health,info
  endpoint:
    health:
      # never：不展示详情；when_authorized：授权后展示；always：总是展示
      show-details: when_authorized
```

健康检查访问示例：

```bash
curl http://localhost:8080/actuator/health
```

响应示例：

```json
{
  "status": "UP"
}
```

如果需要自定义健康检查，可以实现 `HealthIndicator`。例如检查本地上传目录是否存在。

文件位置：`src/main/java/io/github/atengk/demo/actuator/FileStorageHealthIndicator.java`

以下健康检查组件用于检查文件上传目录是否可用。

```java
package io.github.atengk.demo.actuator;

import cn.hutool.core.io.FileUtil;
import io.github.atengk.demo.config.properties.FileStorageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * 文件存储健康检查。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Component
@RequiredArgsConstructor
public class FileStorageHealthIndicator implements HealthIndicator {

    private final FileStorageProperties fileStorageProperties;

    @Override
    public Health health() {
        boolean exists = FileUtil.exist(fileStorageProperties.getUploadPath());
        if (!exists) {
            return Health.down()
                    .withDetail("uploadPath", fileStorageProperties.getUploadPath())
                    .withDetail("reason", "文件上传目录不存在")
                    .build();
        }

        return Health.up()
                .withDetail("uploadPath", fileStorageProperties.getUploadPath())
                .build();
    }

}
```

健康检查建议：

- 生产环境至少暴露 `/actuator/health`。
- 健康检查不要执行耗时很长的远程调用。
- Kubernetes 场景可以使用 liveness 和 readiness 探针。
- 依赖外部系统的健康检查要谨慎，避免外部故障导致应用被错误重启。
- 生产环境不建议对未授权用户展示健康详情。
- 自定义健康检查应简洁、快速、可解释。

### 应用信息接口

应用信息接口用于展示应用名称、版本、描述、构建信息、Git 信息、Java 信息等。Actuator 的 `/actuator/info` 端点会收集 `InfoContributor` 提供的信息。Spring Boot 可以通过 `info.*` 配置、构建插件生成的 `build-info.properties`、Git 信息等方式提供应用信息。([Home](https://docs.spring.io/spring-boot/3.5/reference/actuator/endpoints.html?utm_source=chatgpt.com))

文件位置：`src/main/resources/application.yml`

以下配置定义应用基础信息。

```yaml
info:
  app:
    # 应用名称
    name: spring-boot3-demo
    # 应用描述
    description: Spring Boot 3 开发示例项目
    # 应用版本
    version: 1.0.0
    # 维护人
    owner: Ateng

management:
  endpoints:
    web:
      exposure:
        # 暴露 info 端点
        include: health,info
```

访问示例：

```bash
curl http://localhost:8080/actuator/info
```

如果需要写入构建信息，可以配置 Spring Boot Maven Plugin 的 `build-info` 目标。

文件位置：`pom.xml`

以下配置会在构建时生成 `META-INF/build-info.properties`，供 `/actuator/info` 展示构建信息。

```xml
<build>
    <plugins>
        <!-- Spring Boot 打包插件：生成构建信息，供 Actuator info 端点使用 -->
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
            <executions>
                <execution>
                    <goals>
                        <!-- 生成 META-INF/build-info.properties -->
                        <goal>build-info</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

也可以自定义 `InfoContributor`，补充运行环境信息。

文件位置：`src/main/java/io/github/atengk/demo/actuator/AppInfoContributor.java`

以下组件向 `/actuator/info` 追加自定义应用信息。

```java
package io.github.atengk.demo.actuator;

import cn.hutool.core.date.DateUtil;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;

/**
 * 自定义应用信息贡献器。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Component
public class AppInfoContributor implements InfoContributor {

    @Override
    public void contribute(Info.Builder builder) {
        builder.withDetail("runtime", java.util.Map.of(
                "startupCheckTime", DateUtil.now(),
                "document", "Spring Boot 3 开发文档"
        ));
    }

}
```

应用信息接口建议：

- 展示应用名称、版本、构建时间、Git 提交信息。
- 生产环境不要在 `/actuator/info` 中暴露密钥、数据库地址、Token。
- 构建信息建议通过 Maven 插件自动生成。
- 多实例部署时，版本信息有助于确认发布是否完成。
- 发布验证脚本可以读取 `/actuator/info` 判断当前运行版本。

### 指标接口

指标接口用于暴露 JVM、HTTP、线程、内存、GC、数据源、连接池等运行指标。Actuator 的 `/actuator/metrics` 端点可以查看可用指标名称，也可以通过 `/actuator/metrics/{metric.name}` 查看具体指标。Actuator 端点通过 HTTP 暴露时，默认基础路径为 `/actuator`。([Home](https://docs.spring.io/spring-boot/3.5/reference/actuator/endpoints.html?utm_source=chatgpt.com))

文件位置：`src/main/resources/application.yml`

以下配置暴露 `metrics` 端点。

```yaml
management:
  endpoints:
    web:
      exposure:
        # 暴露健康检查、应用信息和指标端点
        include: health,info,metrics
```

查看全部指标名称：

```bash
curl http://localhost:8080/actuator/metrics
```

查看 JVM 内存指标：

```bash
curl http://localhost:8080/actuator/metrics/jvm.memory.used
```

查看 HTTP 请求指标：

```bash
curl http://localhost:8080/actuator/metrics/http.server.requests
```

如果项目需要接入 Prometheus，需要增加 Prometheus Registry 依赖并暴露 `prometheus` 端点。

文件位置：`pom.xml`

```xml
<dependencies>
    <!-- Prometheus 指标注册器：将 Micrometer 指标导出为 Prometheus 格式 -->
    <dependency>
        <groupId>io.micrometer</groupId>
        <artifactId>micrometer-registry-prometheus</artifactId>
    </dependency>
</dependencies>
```

文件位置：`src/main/resources/application.yml`

```yaml
management:
  endpoints:
    web:
      exposure:
        # 暴露 Prometheus 指标端点
        include: health,info,metrics,prometheus
```

访问 Prometheus 指标：

```bash
curl http://localhost:8080/actuator/prometheus
```

指标接口建议：

- 本地和测试环境可以暴露 `metrics` 便于排查。
- 生产环境建议通过 Prometheus、Grafana 或云监控平台采集指标。
- 生产环境不要直接公网暴露 `/actuator/metrics`。
- HTTP 请求耗时、JVM 内存、GC、线程、连接池是重点关注指标。
- 自定义业务指标可以通过 Micrometer 注册 Counter、Timer、Gauge。

### 端点暴露配置

Actuator 端点是否可访问由“端点启用”和“端点暴露”共同决定。启用表示端点在应用中可用，暴露表示该端点可以通过 HTTP 或 JMX 访问。Spring Boot 文档明确说明，可以通过 `management.endpoints.web.exposure.include` 和 `exclude` 控制 HTTP 暴露范围。([Home](https://docs.spring.io/spring-boot/3.5/reference/actuator/endpoints.html?utm_source=chatgpt.com))

常见端点：

| 端点         | 路径                   | 说明      |
| ------------ | ---------------------- | --------- |
| `health`     | `/actuator/health`     | 健康检查  |
| `info`       | `/actuator/info`       | 应用信息  |
| `metrics`    | `/actuator/metrics`    | 指标入口  |
| `env`        | `/actuator/env`        | 环境属性  |
| `beans`      | `/actuator/beans`      | Bean 信息 |
| `loggers`    | `/actuator/loggers`    | 日志级别  |
| `threaddump` | `/actuator/threaddump` | 线程转储  |
| `heapdump`   | `/actuator/heapdump`   | 堆转储    |

开发环境配置示例：

文件位置：`src/main/resources/application-dev.yml`

```yaml
management:
  endpoints:
    web:
      exposure:
        # 开发环境可适当暴露更多端点
        include: health,info,metrics,env,beans,loggers
  endpoint:
    health:
      # 开发环境可以直接展示健康详情
      show-details: always
```

测试环境配置示例：

文件位置：`src/main/resources/application-test.yml`

```yaml
management:
  endpoints:
    web:
      exposure:
        # 测试环境暴露排查所需端点
        include: health,info,metrics,loggers
  endpoint:
    health:
      show-details: when_authorized
```

生产环境配置示例：

文件位置：`src/main/resources/application-prod.yml`

```yaml
management:
  endpoints:
    web:
      exposure:
        # 生产环境只暴露必要端点
        include: health,info,prometheus
  endpoint:
    health:
      # 生产环境不对未授权用户展示健康详情
      show-details: when_authorized
```

如果需要修改 Actuator 访问基础路径，可以配置：

```yaml
management:
  endpoints:
    web:
      # 将 /actuator 改为 /manage
      base-path: /manage
```

端点暴露建议：

- 开发环境可以暴露更多排查端点。
- 生产环境只暴露 `health`、`info`、`prometheus` 等必要端点。
- `env`、`beans`、`heapdump`、`threaddump` 等端点可能包含敏感信息，生产环境应谨慎开放。
- 如果必须开放敏感端点，应放在内网并加认证授权。
- 端点暴露策略应按 Profile 区分。

### 生产环境端点控制

生产环境端点控制的核心目标是“最小暴露、权限保护、内网访问、可审计”。Actuator 文档也提醒，在暴露端点前应确认端点不包含敏感信息，或者通过防火墙、Spring Security 等方式保护。([Home](https://docs.spring.io/spring-boot/3.5/reference/actuator/endpoints.html?utm_source=chatgpt.com))

推荐生产策略：

| 端点         | 生产建议                 |
| ------------ | ------------------------ |
| `health`     | 可以开放给负载均衡或探针 |
| `info`       | 可开放有限信息           |
| `prometheus` | 仅允许监控系统访问       |
| `metrics`    | 不直接公网开放           |
| `env`        | 禁止公网开放             |
| `heapdump`   | 禁止公网开放             |
| `threaddump` | 仅故障排查时临时开放     |
| `loggers`    | 需要认证和审计           |

文件位置：`src/main/resources/application-prod.yml`

以下配置将管理端口独立到 `9001`，便于通过网络策略控制访问。

```yaml
management:
  server:
    # 管理端口与业务端口分离，便于内网访问控制
    port: 9001
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
  endpoint:
    health:
      show-details: when_authorized
```

如果项目引入 Spring Security，可以为 Actuator 配置单独访问规则。

文件位置：`src/main/java/io/github/atengk/demo/config/ActuatorSecurityConfig.java`

以下配置允许健康检查访问，其他 Actuator 端点需要 `ACTUATOR_ADMIN` 权限。

```java
package io.github.atengk.demo.config;

import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.info.InfoEndpoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Actuator 安全配置。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Configuration
public class ActuatorSecurityConfig {

    @Bean
    @Order(1)
    public SecurityFilterChain actuatorSecurityFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher(EndpointRequest.toAnyEndpoint())
                .authorizeHttpRequests(registry -> registry
                        .requestMatchers(EndpointRequest.to(HealthEndpoint.class, InfoEndpoint.class)).permitAll()
                        .anyRequest().hasRole("ACTUATOR_ADMIN")
                )
                .httpBasic(Customizer.withDefaults());

        return http.build();
    }

}
```

生产环境端点控制建议：

- 管理端口和业务端口可以分离。
- 使用内网、防火墙、网关或安全组限制访问来源。
- `heapdump`、`env` 等敏感端点默认不要暴露。
- 监控采集端点只允许监控系统访问。
- 动态修改日志级别等操作应有权限控制和审计记录。
- 生产事故排查临时开放端点后，应及时关闭。

## 测试开发

测试开发用于保证代码逻辑、接口行为、配置加载和应用集成能力符合预期。Spring Boot 提供 `spring-boot-starter-test`，会引入 JUnit Jupiter、Spring Test、AssertJ、Mockito 等常用测试能力。Spring Boot 官方测试文档说明，`@SpringBootTest` 会通过 `SpringApplication` 创建测试用 `ApplicationContext`，而 `@WebMvcTest` 可以测试 Spring MVC Controller，并自动配置 MockMvc。([Home](https://docs.spring.io/spring-boot/reference/testing/spring-boot-applications.html?utm_source=chatgpt.com))

### 单元测试

单元测试用于验证单个类或单个方法的逻辑，不依赖完整 Spring 容器。Service 中的纯业务逻辑、工具类、转换器、校验器等都适合写单元测试。单元测试应运行快、依赖少、结果稳定。

文件位置：`pom.xml`

以下依赖用于测试开发。

```xml
<dependencies>
    <!-- Spring Boot 测试依赖：包含 JUnit Jupiter、Mockito、AssertJ、Spring Test 等 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

文件位置：`src/main/java/io/github/atengk/demo/service/AmountService.java`

以下业务类用于计算订单金额。

```java
package io.github.atengk.demo.service;

import cn.hutool.core.collection.CollUtil;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * 金额计算业务组件。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Service
public class AmountService {

    public BigDecimal sum(List<BigDecimal> amounts) {
        if (CollUtil.isEmpty(amounts)) {
            return BigDecimal.ZERO;
        }

        return amounts.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

}
```

文件位置：`src/test/java/io/github/atengk/demo/service/AmountServiceTest.java`

以下单元测试不启动 Spring 容器，直接测试金额计算逻辑。

```java
package io.github.atengk.demo.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 金额计算业务组件测试。
 *
 * @author Ateng
 * @since 2026-05-09
 */
class AmountServiceTest {

    private final AmountService amountService = new AmountService();

    @Test
    void shouldReturnZeroWhenAmountListIsEmpty() {
        BigDecimal result = amountService.sum(List.of());

        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void shouldSumAmountList() {
        BigDecimal result = amountService.sum(List.of(
                new BigDecimal("10.50"),
                new BigDecimal("20.30")
        ));

        assertThat(result).isEqualByComparingTo(new BigDecimal("30.80"));
    }

}
```

单元测试建议：

- 不需要 Spring 容器时，不使用 `@SpringBootTest`。
- 纯逻辑类直接 `new` 对象测试。
- 断言使用 AssertJ 可读性更好。
- 测试方法名表达业务场景。
- 单元测试不连接真实数据库、Redis、MQ。
- 对异常分支、边界值、空数据都要覆盖。

### Controller 测试

Controller 测试用于验证接口路径、请求方法、参数校验、响应结构、状态码等 Web 层行为。`@WebMvcTest` 会加载 MVC 相关组件，并自动配置 MockMvc，适合只测试 Controller，不启动完整应用。Spring Boot 文档说明，`@WebMvcTest` 会限制扫描范围，普通 `@Component` 和 `@ConfigurationProperties` 不会默认扫描，依赖的 Service 通常需要使用 Mockito 替身。([Home](https://docs.spring.io/spring-boot/reference/testing/spring-boot-applications.html?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/demo/controller/UserTestController.java`

以下 Controller 用于测试用户详情接口。

```java
package io.github.atengk.demo.controller;

import io.github.atengk.demo.common.response.ApiResult;
import io.github.atengk.demo.model.vo.UserVO;
import io.github.atengk.demo.service.UserTestService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户测试接口。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/test/users")
public class UserTestController {

    private final UserTestService userTestService;

    @GetMapping("/{id}")
    public ApiResult<UserVO> getById(@PathVariable Long id) {
        return ApiResult.success(userTestService.getById(id));
    }

}
```

文件位置：`src/test/java/io/github/atengk/demo/controller/UserTestControllerTest.java`

以下 Controller 测试使用 MockMvc 调用接口，并模拟 Service 返回值。

```java
package io.github.atengk.demo.controller;

import io.github.atengk.demo.model.vo.UserVO;
import io.github.atengk.demo.service.UserTestService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 用户测试接口测试。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@WebMvcTest(UserTestController.class)
class UserTestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserTestService userTestService;

    @Test
    void shouldGetUserById() throws Exception {
        UserVO userVO = new UserVO();
        userVO.setId(1001L);
        userVO.setUsername("ateng");
        userVO.setNickname("Ateng");

        given(userTestService.getById(1001L)).willReturn(userVO);

        mockMvc.perform(get("/api/test/users/{id}", 1001L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1001))
                .andExpect(jsonPath("$.data.username").value("ateng"));
    }

}
```

如果项目使用较早的 Spring Boot 3 版本，不支持 `@MockitoBean`，可以使用 `@MockBean` 替代；新版本文档中的 MVC 测试示例推荐使用 Mockito 替身配合 `@WebMvcTest`。([Home](https://docs.spring.io/spring-boot/reference/testing/spring-boot-applications.html?utm_source=chatgpt.com))

Controller 测试建议：

- 使用 `@WebMvcTest` 测试单个 Controller。
- 使用 MockMvc 验证状态码、JSON 字段和响应结构。
- Service 使用 Mockito 模拟，不连接真实数据库。
- 参数校验失败场景也要测试。
- 全局异常处理器需要时可以通过 `@Import` 引入。
- 不建议 Controller 测试启动完整应用上下文。

### Service 测试

Service 测试用于验证业务规则、异常分支、对象转换、事务边界和 Repository 调用逻辑。Service 测试可以是纯单元测试，也可以配合 Spring 容器进行集成测试。多数业务规则适合使用 Mockito 模拟 Repository 依赖。

文件位置：`src/main/java/io/github/atengk/demo/service/UserTestService.java`

以下 Service 根据用户 ID 查询用户，不存在时抛出业务异常。

```java
package io.github.atengk.demo.service;

import cn.hutool.core.bean.BeanUtil;
import io.github.atengk.demo.common.exception.BizException;
import io.github.atengk.demo.model.entity.UserEntity;
import io.github.atengk.demo.model.vo.UserVO;
import io.github.atengk.demo.repository.UserTestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 用户测试业务组件。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Service
@RequiredArgsConstructor
public class UserTestService {

    private final UserTestRepository userTestRepository;

    public UserVO getById(Long id) {
        UserEntity entity = userTestRepository.getById(id);
        if (entity == null) {
            throw new BizException(404, "用户不存在");
        }
        return BeanUtil.copyProperties(entity, UserVO.class);
    }

}
```

文件位置：`src/test/java/io/github/atengk/demo/service/UserTestServiceTest.java`

以下 Service 单元测试使用 Mockito 模拟 Repository。

```java
package io.github.atengk.demo.service;

import io.github.atengk.demo.common.exception.BizException;
import io.github.atengk.demo.model.entity.UserEntity;
import io.github.atengk.demo.model.vo.UserVO;
import io.github.atengk.demo.repository.UserTestRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;

/**
 * 用户测试业务组件测试。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@ExtendWith(MockitoExtension.class)
class UserTestServiceTest {

    @Mock
    private UserTestRepository userTestRepository;

    @InjectMocks
    private UserTestService userTestService;

    @Test
    void shouldReturnUserWhenUserExists() {
        UserEntity entity = new UserEntity();
        entity.setId(1001L);
        entity.setUsername("ateng");
        entity.setNickname("Ateng");

        given(userTestRepository.getById(1001L)).willReturn(entity);

        UserVO result = userTestService.getById(1001L);

        assertThat(result.getId()).isEqualTo(1001L);
        assertThat(result.getUsername()).isEqualTo("ateng");
    }

    @Test
    void shouldThrowExceptionWhenUserNotExists() {
        given(userTestRepository.getById(1001L)).willReturn(null);

        assertThatThrownBy(() -> userTestService.getById(1001L))
                .isInstanceOf(BizException.class)
                .hasMessage("用户不存在");
    }

}
```

Service 测试建议：

- 核心业务规则必须覆盖成功和失败场景。
- Repository、外部接口、消息组件使用 Mock 替代。
- 断言不仅验证非空，还要验证关键字段。
- 异常分支使用 `assertThatThrownBy`。
- 事务、多数据源、真实数据库相关逻辑可放到集成测试。
- 不要为了测试方便把业务方法改成不合理的可见性。

### 配置加载测试

配置加载测试用于验证 `application.yml`、Profile 配置、`@ConfigurationProperties` 绑定、默认值和校验规则是否符合预期。配置类是项目基础能力的一部分，应针对关键配置写测试。

文件位置：`src/main/java/io/github/atengk/demo/config/properties/FileStorageProperties.java`

以下配置属性类用于文件存储配置。

```java
package io.github.atengk.demo.config.properties;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

/**
 * 文件存储配置属性。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
@Validated
@ConfigurationProperties(prefix = "demo.file")
public class FileStorageProperties {

    @NotBlank(message = "文件上传根目录不能为空")
    private String uploadPath = "./data/upload";

    private List<String> allowedExtensions = new ArrayList<>();

    private String downloadPrefix = "/api/files/download";

}
```

文件位置：`src/test/java/io/github/atengk/demo/config/FileStoragePropertiesTest.java`

以下测试加载配置属性，并验证字段绑定结果。

```java
package io.github.atengk.demo.config;

import io.github.atengk.demo.config.properties.FileStorageProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 文件存储配置属性测试。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@SpringBootTest(properties = {
        "demo.file.upload-path=/tmp/upload",
        "demo.file.allowed-extensions[0]=jpg",
        "demo.file.allowed-extensions[1]=pdf",
        "demo.file.download-prefix=/api/files/download"
})
@EnableConfigurationProperties(FileStorageProperties.class)
class FileStoragePropertiesTest {

    @Autowired
    private FileStorageProperties fileStorageProperties;

    @Test
    void shouldBindFileStorageProperties() {
        assertThat(fileStorageProperties.getUploadPath()).isEqualTo("/tmp/upload");
        assertThat(fileStorageProperties.getAllowedExtensions()).containsExactly("jpg", "pdf");
        assertThat(fileStorageProperties.getDownloadPrefix()).isEqualTo("/api/files/download");
    }

}
```

如果只想测试配置绑定，不启动完整应用，也可以使用 `ApplicationContextRunner`。

文件位置：`src/test/java/io/github/atengk/demo/config/FileStoragePropertiesContextRunnerTest.java`

```java
package io.github.atengk.demo.config;

import io.github.atengk.demo.config.properties.FileStorageProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 文件存储配置上下文测试。
 *
 * @author Ateng
 * @since 2026-05-09
 */
class FileStoragePropertiesContextRunnerTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfig.class)
            .withPropertyValues(
                    "demo.file.upload-path=/tmp/upload",
                    "demo.file.allowed-extensions[0]=png"
            );

    @Test
    void shouldLoadProperties() {
        contextRunner.run(context -> {
            FileStorageProperties properties = context.getBean(FileStorageProperties.class);
            assertThat(properties.getUploadPath()).isEqualTo("/tmp/upload");
            assertThat(properties.getAllowedExtensions()).containsExactly("png");
        });
    }

    @Configuration
    @EnableConfigurationProperties(FileStorageProperties.class)
    static class TestConfig {
    }

}
```

配置加载测试建议：

- 核心配置属性类应测试绑定结果。
- 默认值和校验规则都应覆盖。
- Profile 差异配置可以分别测试。
- 配置测试不应依赖真实外部服务。
- `ApplicationContextRunner` 适合轻量测试自动配置和配置属性。
- 配置类字段改名时，测试可以及时发现配置失效。

### 集成测试

集成测试用于验证多个组件组合后的行为，例如 Controller、Service、Repository、配置、数据库访问等完整链路。`@SpringBootTest` 会创建完整应用上下文，适合验证应用能否启动、接口能否跑通、配置是否完整。Spring Boot 文档说明，`@SpringBootTest` 默认不会启动真实服务器；可以通过 `webEnvironment` 指定 `MOCK`、`RANDOM_PORT`、`DEFINED_PORT` 或 `NONE`。([Home](https://docs.spring.io/spring-boot/reference/testing/spring-boot-applications.html?utm_source=chatgpt.com))

文件位置：`src/test/java/io/github/atengk/demo/DemoApplicationTests.java`

以下测试验证 Spring Boot 应用上下文可以正常加载。

```java
package io.github.atengk.demo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Spring Boot 应用上下文测试。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@SpringBootTest
class DemoApplicationTests {

    @Test
    void contextLoads() {
    }

}
```

使用随机端口启动真实 Web 环境：

文件位置：`src/test/java/io/github/atengk/demo/integration/HealthEndpointIntegrationTest.java`

以下集成测试启动随机端口，并访问健康检查接口。

```java
package io.github.atengk.demo.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 健康检查集成测试。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class HealthEndpointIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Test
    void shouldVisitHealthEndpoint() {
        String response = testRestTemplate.getForObject(
                "http://localhost:" + port + "/actuator/health",
                String.class
        );

        assertThat(response).contains("UP");
    }

}
```

如果需要测试数据库访问，建议使用测试数据库、Testcontainers 或内存数据库，避免连接开发或生产数据库。

集成测试建议：

- 用于验证关键链路，不替代单元测试。
- 集成测试数量应控制，避免构建过慢。
- 不连接生产数据库。
- 测试数据应可重复初始化和清理。
- 随机端口测试适合验证真实 HTTP 行为。
- `@SpringBootTest` 启动较慢，不应滥用于所有测试。

## 接口文档与调试

接口文档与调试用于规范接口说明、请求示例、响应示例、错误码和本地联调方式。Spring Boot 3 项目可以使用 springdoc-openapi 生成 OpenAPI 3 文档，并通过 Swagger UI 页面进行接口调试。springdoc-openapi 官方文档说明，Spring Boot 3.x 项目应使用 `springdoc-openapi` v2 系列 starter；Swagger UI 默认可通过 `/swagger-ui.html` 访问，OpenAPI JSON 默认可通过 `/v3/api-docs` 访问。([OpenAPI 3 Library for spring-boot](https://springdoc.org/?utm_source=chatgpt.com))

### 接口说明规范

接口说明应覆盖接口用途、请求路径、请求方法、请求参数、请求体、响应字段、错误码、权限要求和调用示例。对于前后端分离项目，接口文档不应只依赖口头说明或代码猜测。

推荐接口说明包含以下内容：

| 内容     | 说明                           |
| -------- | ------------------------------ |
| 接口名称 | 简短表达接口功能               |
| 请求方法 | `GET`、`POST`、`PUT`、`DELETE` |
| 请求路径 | 例如 `/api/users/{id}`         |
| 权限要求 | 是否登录、需要什么角色         |
| 请求参数 | Path、Query、Header、Body      |
| 响应结构 | 成功响应和失败响应             |
| 错误码   | 业务错误码和含义               |
| 示例     | curl、JSON 请求体、JSON 响应   |

文件位置：`pom.xml`

以下依赖用于集成 springdoc-openapi 和 Swagger UI。具体版本应与项目 Spring Boot 版本保持兼容；Spring Boot 3 项目通常使用 `springdoc-openapi` v2 starter。([OpenAPI 3 Library for spring-boot](https://springdoc.org/?utm_source=chatgpt.com))

```xml
<properties>
    <!-- Spring Boot 3 项目常用 springdoc-openapi v2 starter，版本按项目兼容性选择 -->
    <springdoc.version>2.8.17</springdoc.version>
</properties>

<dependencies>
    <!-- OpenAPI 3 + Swagger UI：用于生成接口文档和本地调试页面 -->
    <dependency>
        <groupId>org.springdoc</groupId>
        <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
        <version>${springdoc.version}</version>
    </dependency>
</dependencies>
```

文件位置：`src/main/java/io/github/atengk/demo/config/OpenApiConfig.java`

以下配置类定义接口文档基础信息。

```java
package io.github.atengk.demo.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 接口文档配置。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Spring Boot 3 开发接口文档")
                        .description("用于说明 Spring Boot 3 项目接口、参数、响应和错误码")
                        .version("1.0.0"));
    }

}
```

文件位置：`src/main/resources/application.yml`

以下配置调整 Swagger UI 和 OpenAPI 文档路径。

```yaml
springdoc:
  api-docs:
    # OpenAPI JSON 文档路径
    path: /v3/api-docs
  swagger-ui:
    # Swagger UI 页面路径
    path: /swagger-ui.html
    # 按接口路径排序
    operations-sorter: alpha
    # 按标签排序
    tags-sorter: alpha
```

访问地址：

```text
Swagger UI: http://localhost:8080/swagger-ui.html
OpenAPI JSON: http://localhost:8080/v3/api-docs
```

接口说明规范建议：

- 每个接口必须有清晰用途说明。
- 请求参数必须标注必填、类型、示例和含义。
- 响应字段必须说明含义。
- 错误码必须集中维护。
- 接口变更需要同步更新文档。
- 对外接口应注明认证方式、签名规则、频率限制和版本策略。

### 请求示例整理

请求示例用于帮助前端、测试人员和第三方调用方快速理解接口调用方式。每个重要接口至少应提供 curl 示例，复杂请求应提供完整 JSON 请求体。

文件位置：`src/main/java/io/github/atengk/demo/model/dto/DocUserCreateDTO.java`

以下 DTO 使用 OpenAPI 注解补充请求字段说明。

```java
package io.github.atengk.demo.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 文档用户创建请求参数。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
@Schema(description = "用户创建请求参数")
public class DocUserCreateDTO {

    @Schema(description = "用户名", example = "ateng", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "用户名不能为空")
    private String username;

    @Schema(description = "昵称", example = "Ateng", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "昵称不能为空")
    private String nickname;

    @Schema(description = "邮箱", example = "ateng@example.com")
    @Email(message = "邮箱格式不正确")
    private String email;

}
```

文件位置：`src/main/java/io/github/atengk/demo/controller/DocUserController.java`

以下 Controller 使用 OpenAPI 注解描述接口用途和响应信息。

```java
package io.github.atengk.demo.controller;

import io.github.atengk.demo.common.response.ApiResult;
import io.github.atengk.demo.model.dto.DocUserCreateDTO;
import io.github.atengk.demo.model.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 文档用户接口。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Tag(name = "用户接口", description = "用户创建、查询、修改和删除接口")
@RestController
@RequestMapping("/api/doc/users")
public class DocUserController {

    @Operation(summary = "创建用户", description = "根据用户名、昵称和邮箱创建用户")
    @PostMapping
    public ApiResult<UserVO> create(@Valid @RequestBody DocUserCreateDTO dto) {
        UserVO vo = new UserVO();
        vo.setId(1001L);
        vo.setUsername(dto.getUsername());
        vo.setNickname(dto.getNickname());
        vo.setEmail(dto.getEmail());
        return ApiResult.success(vo);
    }

    @Operation(summary = "查询用户详情", description = "根据用户 ID 查询用户基础信息")
    @GetMapping("/{id}")
    public ApiResult<UserVO> getById(@PathVariable Long id) {
        UserVO vo = new UserVO();
        vo.setId(id);
        vo.setUsername("ateng");
        vo.setNickname("Ateng");
        return ApiResult.success(vo);
    }

}
```

创建用户请求示例：

```bash
curl -X POST "http://localhost:8080/api/doc/users" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "ateng",
    "nickname": "Ateng",
    "email": "ateng@example.com"
  }'
```

查询用户详情请求示例：

```bash
curl -X GET "http://localhost:8080/api/doc/users/1001"
```

请求示例整理建议：

- 每个新增、修改接口提供 JSON 请求体示例。
- 每个查询接口提供 Query 参数示例。
- Header 中需要 Token、TraceId、租户 ID 时必须写清楚。
- curl 示例应可直接复制执行。
- 文件上传接口应提供 `multipart/form-data` 示例。
- 示例数据应贴近真实业务，不使用无意义字段。

### 响应示例整理

响应示例用于明确接口返回结构，减少前后端对字段含义和格式的理解偏差。统一响应结构下，每个接口都应说明 `code`、`message`、`data` 的具体内容。

成功响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 1001,
    "username": "ateng",
    "nickname": "Ateng",
    "email": "ateng@example.com",
    "createTime": "2026-05-09 10:30:00"
  }
}
```

参数错误响应示例：

```json
{
  "code": 400,
  "message": "用户名不能为空",
  "data": null
}
```

业务错误响应示例：

```json
{
  "code": 1000,
  "message": "用户不存在",
  "data": null
}
```

分页响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "pageNum": 1,
    "pageSize": 10,
    "total": 25,
    "pages": 3,
    "records": [
      {
        "id": 1001,
        "username": "ateng",
        "nickname": "Ateng"
      }
    ]
  }
}
```

响应示例整理建议：

- 成功响应和失败响应都要提供。
- 分页接口必须提供完整分页结构。
- 时间字段格式要和 Jackson 配置保持一致。
- 枚举字段要说明返回编码还是文本。
- 空列表返回 `[]`，空分页返回完整分页对象。
- 不应在响应示例中出现密码、Token、密钥等敏感字段。

### 错误码说明

错误码用于让前端和调用方根据错误类型做不同处理。错误码应集中定义、稳定维护，不应在业务代码中随意编写魔法数字。

推荐错误码分段：

| 错误码范围  | 说明           |
| ----------- | -------------- |
| `200`       | 成功           |
| `400`       | 请求参数错误   |
| `401`       | 未认证         |
| `403`       | 无权限         |
| `404`       | 资源不存在     |
| `405`       | 请求方法不支持 |
| `500`       | 系统异常       |
| `1000-1999` | 通用业务错误   |
| `2000-2999` | 用户业务错误   |
| `3000-3999` | 订单业务错误   |
| `4000-4999` | 文件业务错误   |

文件位置：`src/main/java/io/github/atengk/demo/common/response/ResultCodeEnum.java`

以下枚举集中维护接口错误码。

```java
package io.github.atengk.demo.common.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 统一响应码枚举。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Getter
@RequiredArgsConstructor
public enum ResultCodeEnum {

    SUCCESS(200, "操作成功"),

    BAD_REQUEST(400, "请求参数错误"),

    UNAUTHORIZED(401, "未认证"),

    FORBIDDEN(403, "无访问权限"),

    NOT_FOUND(404, "资源不存在"),

    METHOD_NOT_ALLOWED(405, "请求方法不支持"),

    INTERNAL_ERROR(500, "系统异常"),

    BIZ_ERROR(1000, "业务处理失败"),

    USER_NOT_FOUND(2001, "用户不存在"),

    ORDER_STATUS_NOT_ALLOWED(3001, "订单状态不允许操作"),

    FILE_TYPE_NOT_ALLOWED(4001, "不支持的文件类型"),

    FILE_SIZE_EXCEEDED(4002, "文件大小超过限制");

    private final Integer code;

    private final String message;

}
```

错误码文档示例：

| 错误码 | 错误消息         | 处理建议                    |
| ------ | ---------------- | --------------------------- |
| `400`  | 请求参数错误     | 检查必填参数、格式、类型    |
| `401`  | 未认证           | 重新登录或刷新 Token        |
| `403`  | 无访问权限       | 联系管理员分配权限          |
| `404`  | 资源不存在       | 检查资源 ID 是否正确        |
| `500`  | 系统异常         | 记录 TraceId 并联系后端排查 |
| `2001` | 用户不存在       | 检查用户是否已删除          |
| `4001` | 不支持的文件类型 | 更换允许上传的文件格式      |

错误码说明建议：

- 错误码集中定义，不散落在业务代码中。
- 错误消息面向调用方，保持清晰可理解。
- 不同业务域预留不同错误码段。
- 错误码一旦发布，不要随意改变含义。
- 系统异常返回通用提示，详细原因写日志。
- 文档中应说明错误码、错误消息和处理建议。

### 本地接口调试

本地接口调试用于验证接口是否能按预期接收参数、处理业务、返回响应。常用方式包括 curl、Postman、Apifox、Swagger UI、IDE HTTP Client 等。springdoc-openapi 集成后，可以通过 Swagger UI 页面直接调试接口，默认文档路径通常是 `/swagger-ui.html` 和 `/v3/api-docs`。([OpenAPI 3 Library for spring-boot](https://springdoc.org/?utm_source=chatgpt.com))

常用本地启动命令：

```bash
# 使用开发环境启动
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 或者打包后启动
mvn clean package -DskipTests
java -jar target/spring-boot3-demo.jar --spring.profiles.active=dev
```

常用调试地址：

```text
应用接口: http://localhost:8080/api
健康检查: http://localhost:8080/actuator/health
应用信息: http://localhost:8080/actuator/info
Swagger UI: http://localhost:8080/swagger-ui.html
OpenAPI JSON: http://localhost:8080/v3/api-docs
```

curl 调试示例：

```bash
# 查询健康检查
curl http://localhost:8080/actuator/health

# 查询用户详情
curl http://localhost:8080/api/doc/users/1001

# 创建用户
curl -X POST "http://localhost:8080/api/doc/users" \
  -H "Content-Type: application/json" \
  -H "X-Trace-Id: trace-local-001" \
  -d '{
    "username": "ateng",
    "nickname": "Ateng",
    "email": "ateng@example.com"
  }'
```

IDEA HTTP Client 调试文件示例：

文件位置：`docs/http/user-api.http`

以下 HTTP 文件可在 IntelliJ IDEA 中直接执行接口请求。

```http
### 健康检查
GET http://localhost:8080/actuator/health

### 查询用户详情
GET http://localhost:8080/api/doc/users/1001
X-Trace-Id: trace-local-001

### 创建用户
POST http://localhost:8080/api/doc/users
Content-Type: application/json
X-Trace-Id: trace-local-002

{
  "username": "ateng",
  "nickname": "Ateng",
  "email": "ateng@example.com"
}
```

本地接口调试建议：

- 优先确认应用是否启动成功。
- 先访问 `/actuator/health` 判断应用状态。
- 再访问 `/v3/api-docs` 判断接口文档是否生成。
- 使用 Swagger UI 调试接口时，注意认证 Header 是否配置。
- curl 示例应保存到文档中，方便复现问题。
- 本地调试数据应与测试环境隔离。
- 调试失败时优先检查请求方法、路径、Header、Content-Type 和请求体格式。



## 打包构建

打包构建用于将 Spring Boot 项目编译、测试、打包为可部署产物。常见产物是可执行 Jar 包，内部包含应用代码、资源文件、依赖包和 Spring Boot 启动加载器。规范的构建过程应包含环境检查、依赖解析、测试执行、产物生成、版本信息写入和产物验证。

### Jar 包构建

Spring Boot 项目通常使用 Maven 构建可执行 Jar。打包时，`spring-boot-maven-plugin` 会将普通 Jar 重新打包为可执行 Jar，使其能够通过 `java -jar` 直接运行。Spring Boot Maven Plugin 文档说明，`repackage` 目标会处理 Maven `package` 阶段生成的 Jar 或 War，并创建可执行归档文件。([Home](https://docs.spring.io/spring-boot/maven-plugin/packaging.html?utm_source=chatgpt.com))

文件位置：`pom.xml`

以下配置用于生成 Spring Boot 可执行 Jar。

```xml
<build>
    <!-- 最终构建产物名称：target/spring-boot3-demo.jar -->
    <finalName>spring-boot3-demo</finalName>

    <plugins>
        <!-- Spring Boot 打包插件：将普通 Jar 重新打包为可执行 Jar -->
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
            <configuration>
                <!-- 多启动类项目建议显式指定主启动类 -->
                <mainClass>io.github.atengk.demo.DemoApplication</mainClass>

                <!-- 排除 Lombok，Lombok 只在编译期使用，不需要进入运行包 -->
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
```

常用构建命令：

```bash
# 清理旧构建产物并重新打包
mvn clean package

# 查看 target 目录中的构建产物
ls -lh target/

# 启动构建后的 Jar 包
java -jar target/spring-boot3-demo.jar
```

构建成功后，通常会生成如下文件：

```text
target/
├── spring-boot3-demo.jar
├── spring-boot3-demo.jar.original
├── classes
├── generated-sources
├── generated-test-sources
├── maven-archiver
├── maven-status
├── surefire-reports
└── test-classes
```

其中：

| 文件                             | 说明                                        |
| -------------------------------- | ------------------------------------------- |
| `spring-boot3-demo.jar`          | 可执行 Jar，可通过 `java -jar` 运行         |
| `spring-boot3-demo.jar.original` | Maven 原始 Jar，未经过 Spring Boot 重新打包 |
| `classes`                        | 编译后的主代码和资源                        |
| `test-classes`                   | 编译后的测试代码                            |
| `surefire-reports`               | 单元测试报告                                |

Jar 包构建建议：

- 正式构建前执行 `mvn clean package`，避免旧产物影响结果。
- 构建产物名称通过 `<finalName>` 固定，便于部署脚本引用。
- 多模块项目只在启动模块配置 Spring Boot 打包插件。
- 不要将 `target` 目录提交到 Git。
- 构建失败时优先查看编译错误、测试错误和依赖解析错误。

### 构建环境配置

构建环境配置用于保证不同开发者、CI/CD 服务器和生产构建环境使用一致的 JDK、Maven、编码和依赖仓库。Spring Boot 3 最低要求 Java 17，因此构建环境必须使用 Java 17 或更高版本。

推荐构建环境：

| 项目     | 推荐值                     |
| -------- | -------------------------- |
| JDK      | Java 17 或 Java 21         |
| Maven    | 3.9.x                      |
| 编码     | UTF-8                      |
| 操作系统 | Linux、macOS、Windows 均可 |
| 构建命令 | `mvn clean package`        |
| 产物类型 | 可执行 Jar                 |

检查本地构建环境：

```bash
# 查看 Java 版本
java -version

# 查看 Maven 版本
mvn -version

# 查看当前项目 Maven 配置
mvn help:effective-pom
```

文件位置：`pom.xml`

以下配置统一 Java 编译版本和编码格式。

```xml
<properties>
    <!-- Spring Boot 3 最低要求 Java 17 -->
    <java.version>17</java.version>

    <!-- 源码编码 -->
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>

    <!-- 报告输出编码 -->
    <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>

    <!-- Maven 编译参数 -->
    <maven.compiler.source>${java.version}</maven.compiler.source>
    <maven.compiler.target>${java.version}</maven.compiler.target>
</properties>
```

如果没有继承 `spring-boot-starter-parent`，建议显式配置 Maven 编译插件。

```xml
<build>
    <plugins>
        <!-- Maven 编译插件：指定 Java 源码和目标字节码版本 -->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <configuration>
                <source>${java.version}</source>
                <target>${java.version}</target>
                <encoding>UTF-8</encoding>
            </configuration>
        </plugin>
    </plugins>
</build>
```

CI 构建脚本示例：

文件位置：`scripts/build.sh`

以下脚本用于在 Linux 环境执行标准构建。

```bash
#!/usr/bin/env bash
set -e

# 进入脚本所在目录的上级项目根目录
cd "$(dirname "$0")/.."

# 输出构建环境信息
java -version
mvn -version

# 执行 Maven 构建
mvn clean package -DskipTests=false

# 输出构建产物
ls -lh target/*.jar
```

赋权并执行：

```bash
chmod +x scripts/build.sh
./scripts/build.sh
```

构建环境建议：

- 团队统一 JDK 主版本。
- CI/CD 使用固定 Maven 镜像或固定构建机环境。
- Maven 私服地址、镜像和凭证统一管理。
- 不在开发者本机生成生产包，生产包应由 CI/CD 生成。
- 构建脚本应输出 Java、Maven 版本，便于排查环境差异。
- 构建产物应带版本号或构建号，便于回滚。

### 跳过测试打包

跳过测试打包用于在特定场景下快速生成构建产物，例如本地临时验证、测试代码正在调整、CI 已在前置阶段完成测试等。生产构建不建议长期跳过测试，否则容易把明显问题带到部署阶段。

常见命令：

```bash
# 跳过测试执行，但仍编译测试代码
mvn clean package -DskipTests

# 跳过测试编译和测试执行
mvn clean package -Dmaven.test.skip=true
```

两者区别：

| 命令                     | 是否编译测试代码 | 是否执行测试 |
| ------------------------ | ---------------- | ------------ |
| `-DskipTests`            | 是               | 否           |
| `-Dmaven.test.skip=true` | 否               | 否           |

建议优先使用 `-DskipTests`，因为它仍会编译测试代码，可以发现测试源码中的编译问题。`-Dmaven.test.skip=true` 更彻底，适合极少数临时场景。

可以在 Maven Surefire 插件中配置默认行为，但不建议默认跳过测试。

文件位置：`pom.xml`

```xml
<build>
    <plugins>
        <!-- Maven 单元测试插件：控制测试执行行为 -->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-surefire-plugin</artifactId>
            <configuration>
                <!-- 不建议生产项目默认跳过测试 -->
                <skipTests>false</skipTests>
            </configuration>
        </plugin>
    </plugins>
</build>
```

跳过测试建议：

- 本地临时构建可以使用 `-DskipTests`。
- 生产流水线不建议跳过测试。
- 如果 CI 分阶段执行测试和打包，可以在打包阶段跳过测试，但前置测试阶段必须通过。
- 测试失败时不应直接长期跳过，而应修复测试或隔离不稳定测试。
- 跳过测试构建的产物应避免直接进入生产环境。

### 构建产物检查

构建产物检查用于确认 Jar 包是否存在、大小是否合理、主类是否正确、配置文件是否打入、依赖是否完整、版本信息是否写入。产物检查可以降低部署后才发现包不可运行的风险。

常见检查命令：

```bash
# 查看 Jar 包大小
ls -lh target/spring-boot3-demo.jar

# 查看 Jar 包内容
jar tf target/spring-boot3-demo.jar | head -n 50

# 检查启动类和 BOOT-INF 结构
jar tf target/spring-boot3-demo.jar | grep -E "BOOT-INF|DemoApplication|application.yml" | head -n 50

# 查看 Jar 包清单
unzip -p target/spring-boot3-demo.jar META-INF/MANIFEST.MF
```

Spring Boot 可执行 Jar 通常包含以下结构：

```text
BOOT-INF/
├── classes/
│   ├── application.yml
│   └── io/github/atengk/demo/DemoApplication.class
└── lib/
    ├── spring-boot-*.jar
    ├── spring-context-*.jar
    └── ...
META-INF/
org/springframework/boot/loader/
```

可执行验证：

```bash
# 查看帮助或尝试启动
java -jar target/spring-boot3-demo.jar --spring.profiles.active=dev --server.port=18080

# 健康检查验证
curl http://localhost:18080/actuator/health
```

构建产物检查脚本示例：

文件位置：`scripts/check-artifact.sh`

以下脚本用于检查 Jar 产物是否存在、是否包含启动类和配置文件。

```bash
#!/usr/bin/env bash
set -e

APP_JAR="target/spring-boot3-demo.jar"

if [ ! -f "${APP_JAR}" ]; then
  echo "构建产物不存在：${APP_JAR}"
  exit 1
fi

echo "构建产物存在：${APP_JAR}"
ls -lh "${APP_JAR}"

echo "检查启动类..."
jar tf "${APP_JAR}" | grep "DemoApplication.class"

echo "检查配置文件..."
jar tf "${APP_JAR}" | grep "application.yml"

echo "检查 Jar 清单..."
unzip -p "${APP_JAR}" META-INF/MANIFEST.MF

echo "构建产物检查完成"
```

执行：

```bash
chmod +x scripts/check-artifact.sh
./scripts/check-artifact.sh
```

构建产物检查建议：

- 检查 Jar 是否生成。
- 检查 Jar 大小是否异常。
- 检查 `BOOT-INF/classes` 是否包含配置文件。
- 检查 `BOOT-INF/lib` 是否包含运行依赖。
- 检查 `MANIFEST.MF` 是否包含 Spring Boot 启动信息。
- 构建后至少做一次启动验证和健康检查验证。

### 版本信息写入

版本信息写入用于在运行时确认当前应用版本、构建时间、构建用户、Git 提交号等信息。它对发布验证、问题排查和版本回滚很重要。Spring Boot Maven Plugin 支持生成构建信息，Actuator 的 `/actuator/info` 可以读取这些信息。

文件位置：`pom.xml`

以下配置用于写入构建信息。

```xml
<build>
    <plugins>
        <!-- Spring Boot 打包插件：生成 build-info.properties -->
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
            <executions>
                <execution>
                    <goals>
                        <!-- 将项目名称、版本、构建时间等写入 META-INF/build-info.properties -->
                        <goal>build-info</goal>
                    </goals>
                    <configuration>
                        <additionalProperties>
                            <!-- 自定义构建信息 -->
                            <java.version>${java.version}</java.version>
                            <encoding>${project.build.sourceEncoding}</encoding>
                        </additionalProperties>
                    </configuration>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

文件位置：`src/main/resources/application.yml`

以下配置暴露应用信息端点。

```yaml
management:
  endpoints:
    web:
      exposure:
        # 暴露应用信息端点
        include: health,info
```

构建后访问：

```bash
mvn clean package
java -jar target/spring-boot3-demo.jar
curl http://localhost:8080/actuator/info
```

也可以在配置文件中写入静态版本信息。

文件位置：`src/main/resources/application.yml`

```yaml
info:
  app:
    # 应用名称
    name: spring-boot3-demo
    # 应用版本
    version: 1.0.0
    # 应用描述
    description: Spring Boot 3 开发示例项目
```

版本信息建议：

- 生产发布包必须能查询版本。
- 推荐通过 Maven 插件生成构建信息。
- CI/CD 中建议写入 Git commit、构建号、构建时间。
- `/actuator/info` 不应暴露敏感信息。
- 发布后通过版本接口确认所有实例是否更新完成。
- 回滚时通过版本信息确认回滚结果。

## 应用运行

应用运行用于说明 Jar 包如何启动、如何指定环境、端口和 JVM 参数，以及如何在服务器后台运行。Spring Boot 支持通过配置文件、环境变量、Java 系统属性和命令行参数传入配置；命令行参数如 `--server.port=9000` 默认会加入 Spring `Environment`，并且优先级高于文件配置。([Home](https://docs.spring.io/spring-boot/3.5-SNAPSHOT/reference/features/external-config.html?utm_source=chatgpt.com))

### 命令行启动

命令行启动是最基础的运行方式。构建完成后，可以使用 `java -jar` 启动 Spring Boot 可执行 Jar。

基础启动命令：

```bash
java -jar target/spring-boot3-demo.jar
```

指定当前目录下 Jar 启动：

```bash
cd /data/spring-boot3-demo/app

java -jar spring-boot3-demo.jar
```

启动后验证：

```bash
# 查看进程
ps -ef | grep spring-boot3-demo | grep -v grep

# 健康检查
curl http://localhost:8080/actuator/health

# 查看应用信息
curl http://localhost:8080/actuator/info
```

如果需要查看启动参数和系统属性，可以使用：

```bash
# 查看 Java 进程
jps -l

# 查看指定 Java 进程启动参数
jcmd <pid> VM.command_line
```

命令行启动建议：

- 本地调试可以直接使用 `java -jar`。
- 服务器部署建议配合启动脚本、systemd 或容器平台。
- 启动前确认 JDK 版本。
- 启动后必须检查健康检查接口。
- 生产环境不要直接在交互终端中前台运行，除非用于临时排查。

### 指定 Profile 启动

指定 Profile 启动用于加载不同环境配置，例如 `dev`、`test`、`prod`。生产环境必须显式指定 Profile，不能依赖默认 `dev` 配置。

命令行方式：

```bash
# 开发环境
java -jar spring-boot3-demo.jar --spring.profiles.active=dev

# 测试环境
java -jar spring-boot3-demo.jar --spring.profiles.active=test

# 生产环境
java -jar spring-boot3-demo.jar --spring.profiles.active=prod
```

环境变量方式：

```bash
export SPRING_PROFILES_ACTIVE=prod

java -jar spring-boot3-demo.jar
```

JVM 系统属性方式：

```bash
java -Dspring.profiles.active=prod -jar spring-boot3-demo.jar
```

如果需要同时加载多个 Profile：

```bash
java -jar spring-boot3-demo.jar --spring.profiles.active=prod,local-cache
```

Profile 验证接口可以复用前文中的 `/api/profile/active`，也可以直接查看启动日志中的 active profile。

指定 Profile 建议：

- 本地默认可以使用 `dev`。
- 测试环境使用 `test`。
- 生产环境使用 `prod`。
- 生产启动命令中必须显式指定 `prod`。
- 不同环境配置项结构应保持一致。
- 敏感配置不要写在 Jar 内部生产配置中，应通过外部配置或环境变量注入。

### 指定端口启动

指定端口启动用于在同一台机器运行多个实例，或适配服务器端口规划。端口可以通过配置文件、命令行参数、环境变量或 JVM 系统属性指定。

命令行参数方式：

```bash
java -jar spring-boot3-demo.jar --server.port=9000
```

JVM 系统属性方式：

```bash
java -Dserver.port=9000 -jar spring-boot3-demo.jar
```

环境变量方式：

```bash
export SERVER_PORT=9000

java -jar spring-boot3-demo.jar
```

同时指定 Profile 和端口：

```bash
java -jar spring-boot3-demo.jar \
  --spring.profiles.active=prod \
  --server.port=9000
```

端口占用排查：

```bash
# 查看 9000 端口占用
lsof -i :9000

# 或使用 ss
ss -lntp | grep 9000
```

指定端口建议：

- 本地开发使用默认端口或临时端口。
- 测试和生产端口由部署规范统一管理。
- 同机多实例部署时，每个实例必须使用不同端口。
- 管理端口和业务端口可以分离。
- 启动失败时优先检查端口占用和防火墙策略。

### JVM 参数配置

JVM 参数用于控制内存、GC、编码、时区、诊断输出等运行行为。生产环境应根据服务器资源、应用负载和容器限制合理配置 JVM 参数。

常见 JVM 参数：

| 参数                              | 说明               |
| --------------------------------- | ------------------ |
| `-Xms`                            | 初始堆内存         |
| `-Xmx`                            | 最大堆内存         |
| `-XX:+UseG1GC`                    | 使用 G1 垃圾收集器 |
| `-Dfile.encoding=UTF-8`           | 文件编码           |
| `-Duser.timezone=Asia/Shanghai`   | JVM 默认时区       |
| `-XX:+HeapDumpOnOutOfMemoryError` | OOM 时导出堆转储   |
| `-XX:HeapDumpPath=...`            | 堆转储路径         |

生产启动示例：

```bash
java \
  -Xms512m \
  -Xmx512m \
  -XX:+UseG1GC \
  -XX:+HeapDumpOnOutOfMemoryError \
  -XX:HeapDumpPath=/data/spring-boot3-demo/dump \
  -Dfile.encoding=UTF-8 \
  -Duser.timezone=Asia/Shanghai \
  -jar spring-boot3-demo.jar \
  --spring.profiles.active=prod
```

如果使用容器部署，JDK 17 对容器内存支持较好，可以使用百分比参数：

```bash
java \
  -XX:InitialRAMPercentage=50.0 \
  -XX:MaxRAMPercentage=75.0 \
  -XX:+UseG1GC \
  -Dfile.encoding=UTF-8 \
  -Duser.timezone=Asia/Shanghai \
  -jar spring-boot3-demo.jar \
  --spring.profiles.active=prod
```

JVM 参数建议：

- 传统服务器部署可以固定 `-Xms` 和 `-Xmx`。
- 容器部署建议结合容器内存限制使用百分比参数。
- 生产环境建议开启 OOM dump。
- Dump 路径必须有写权限，并预留磁盘空间。
- 时区和编码应显式配置。
- 不同环境 JVM 参数应统一由启动脚本或部署平台管理。

### 后台运行配置

后台运行用于让应用在服务器上脱离当前终端持续运行。常见方式包括 `nohup`、systemd、Docker、Kubernetes 等。简单服务器部署可以使用 `nohup`，生产环境更推荐 systemd 或容器平台。

方式一：使用 `nohup` 后台运行。

```bash
cd /data/spring-boot3-demo/app

nohup java \
  -Xms512m \
  -Xmx512m \
  -Dfile.encoding=UTF-8 \
  -Duser.timezone=Asia/Shanghai \
  -jar spring-boot3-demo.jar \
  --spring.profiles.active=prod \
  > /data/spring-boot3-demo/logs/startup.log 2>&1 &
```

查看进程：

```bash
ps -ef | grep spring-boot3-demo | grep -v grep
```

停止进程：

```bash
kill -15 <pid>
```

方式二：使用 systemd 管理应用。

文件位置：`/etc/systemd/system/spring-boot3-demo.service`

以下 systemd 配置用于管理 Spring Boot 应用。

```ini
[Unit]
Description=Spring Boot 3 Demo Service
After=network.target

[Service]
Type=simple
User=app
Group=app
WorkingDirectory=/data/spring-boot3-demo/app
ExecStart=/usr/bin/java -Xms512m -Xmx512m -Dfile.encoding=UTF-8 -Duser.timezone=Asia/Shanghai -jar /data/spring-boot3-demo/app/spring-boot3-demo.jar --spring.profiles.active=prod
ExecStop=/bin/kill -15 $MAINPID
Restart=on-failure
RestartSec=10
SuccessExitStatus=143

[Install]
WantedBy=multi-user.target
```

加载并启动：

```bash
# 重新加载 systemd 配置
sudo systemctl daemon-reload

# 设置开机自启
sudo systemctl enable spring-boot3-demo

# 启动服务
sudo systemctl start spring-boot3-demo

# 查看状态
sudo systemctl status spring-boot3-demo

# 查看日志
journalctl -u spring-boot3-demo -f
```

后台运行建议：

- 临时部署可以使用 `nohup`。
- 生产服务器建议使用 systemd 或容器平台。
- 停止应用使用 `SIGTERM`，不要优先使用 `kill -9`。
- 启动日志和业务日志分开管理。
- systemd 中指定运行用户，不建议使用 root 运行应用。
- 部署目录、日志目录、上传目录应清晰分离。

## 优雅停机

优雅停机用于在应用停止时尽量完成正在处理的请求、停止接收新请求、关闭线程池、释放资源，避免请求中断、数据不一致和资源泄漏。Spring Boot 支持内嵌 Web 服务器的优雅停机，并通过生命周期超时时间控制等待周期。Spring Boot 3.5 文档说明，优雅停机默认对 Jetty、Reactor Netty、Tomcat 等嵌入式 Web 服务器启用，可以通过 `spring.lifecycle.timeout-per-shutdown-phase` 配置每个关闭阶段的超时时间；如果需要禁用，可设置 `server.shutdown=immediate`。([Home](https://docs.spring.io/spring-boot/reference/web/graceful-shutdown.html?utm_source=chatgpt.com))

### 优雅停机配置

优雅停机配置用于控制应用收到停止信号后如何关闭。生产环境应使用 `SIGTERM` 停止应用，让 Spring 容器有机会执行关闭流程。不要优先使用 `kill -9`，因为它会强制杀死进程，不会触发资源释放逻辑。

文件位置：`src/main/resources/application.yml`

以下配置设置关闭阶段等待时间。

```yaml
spring:
  lifecycle:
    # 每个关闭阶段最多等待 30 秒
    timeout-per-shutdown-phase: 30s
```

如果使用较旧的 Spring Boot 版本，可以显式配置：

```yaml
server:
  # 启用优雅停机；Spring Boot 3.5 默认已启用，旧版本建议显式配置
  shutdown: graceful

spring:
  lifecycle:
    # 停机阶段等待时间
    timeout-per-shutdown-phase: 30s
```

停止命令：

```bash
# 推荐：发送 SIGTERM，触发优雅停机
kill -15 <pid>

# 不推荐：强制杀死进程，不触发优雅停机
kill -9 <pid>
```

systemd 停止时也应发送 SIGTERM：

```ini
[Service]
ExecStop=/bin/kill -15 $MAINPID
TimeoutStopSec=45
```

优雅停机配置建议：

- 生产环境使用 `SIGTERM` 停止应用。
- 不要用 `kill -9` 作为常规停机方式。
- `timeout-per-shutdown-phase` 应略大于常见请求耗时。
- 停机等待时间不宜过长，否则发布会变慢。
- Kubernetes、systemd、脚本中的停止超时应大于应用优雅停机超时。
- 停机日志中应能看到资源释放和线程池关闭过程。

### 请求处理等待

请求处理等待用于在停机时让正在处理的请求尽量完成，同时拒绝新请求。Spring Boot 的优雅停机会在关闭应用上下文时对 Web 服务器执行优雅关闭，给已有请求一个完成窗口。不同 Web 服务器拒绝新请求的方式不同，Tomcat、Jetty、Reactor Netty 通常会在网络层停止接收新请求。([Home](https://docs.spring.io/spring-boot/reference/web/graceful-shutdown.html?utm_source=chatgpt.com))

为了验证请求处理等待，可以提供一个慢接口。

文件位置：`src/main/java/io/github/atengk/demo/controller/ShutdownTestController.java`

以下接口模拟一个耗时请求，用于验证优雅停机是否等待当前请求完成。

```java
package io.github.atengk.demo.controller;

import cn.hutool.core.thread.ThreadUtil;
import io.github.atengk.demo.common.response.ApiResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 停机验证接口。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@RestController
@RequestMapping("/api/shutdown")
public class ShutdownTestController {

    /**
     * 模拟慢请求。
     *
     * @param seconds 等待秒数
     * @return 处理结果
     */
    @GetMapping("/sleep")
    public ApiResult<String> sleep(@RequestParam(defaultValue = "10") Integer seconds) {
        log.info("慢请求开始执行，等待秒数：{}", seconds);
        ThreadUtil.sleep(seconds * 1000L);
        log.info("慢请求执行完成，等待秒数：{}", seconds);
        return ApiResult.success("慢请求执行完成");
    }

}
```

验证步骤：

```bash
# 终端 1：发起一个 20 秒慢请求
curl "http://localhost:8080/api/shutdown/sleep?seconds=20"

# 终端 2：在请求执行期间发送 SIGTERM
ps -ef | grep spring-boot3-demo | grep -v grep
kill -15 <pid>
```

观察结果：

```text
慢请求开始执行
应用收到停止信号
慢请求执行完成
应用完成关闭
```

请求处理等待建议：

- 慢请求耗时应小于优雅停机等待时间。
- 停机前负载均衡应先摘除实例，再停止应用。
- 文件上传、导出、长轮询接口应单独评估停机影响。
- 停机期间不应继续接收新流量。
- 发布系统应设置“摘流量 -> 等待 -> 停进程”的顺序。

### 线程池关闭

线程池关闭用于在应用停止时等待异步任务、定时任务、业务线程池完成或中断。Spring 管理的 `ThreadPoolTaskExecutor`、`ThreadPoolTaskScheduler` 可以配置关闭等待策略。自定义线程池必须显式释放，否则可能导致进程无法退出或任务被强制中断。

文件位置：`src/main/java/io/github/atengk/demo/config/AsyncThreadPoolConfig.java`

以下配置让异步线程池在停机时等待任务完成。

```java
package io.github.atengk.demo.config;

import cn.hutool.core.thread.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步线程池配置。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Configuration
public class AsyncThreadPoolConfig {

    /**
     * 应用异步线程池。
     *
     * @return 异步线程池
     */
    @Bean("applicationTaskExecutor")
    public Executor applicationTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(8);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(500);
        executor.setThreadFactory(ThreadFactoryBuilder.create().setNamePrefix("app-async-").build());
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // 停机时等待已提交任务完成
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        executor.initialize();
        log.info("初始化应用异步线程池");
        return executor;
    }

}
```

定时任务线程池关闭配置：

文件位置：`src/main/java/io/github/atengk/demo/config/TaskSchedulerConfig.java`

```java
package io.github.atengk.demo.config;

import cn.hutool.core.thread.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * 定时任务线程池配置。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Configuration
public class TaskSchedulerConfig {

    /**
     * 定时任务线程池。
     *
     * @return 定时任务线程池
     */
    @Bean
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(4);
        scheduler.setThreadFactory(ThreadFactoryBuilder.create().setNamePrefix("schedule-task-").build());
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(30);
        scheduler.setErrorHandler(e -> log.error("定时任务执行异常", e));
        scheduler.initialize();

        log.info("初始化定时任务线程池");
        return scheduler;
    }

}
```

如果使用原生 `ExecutorService`，需要手动关闭。

文件位置：`src/main/java/io/github/atengk/demo/config/NativeExecutorConfig.java`

以下配置通过 `destroyMethod = "shutdown"` 关闭原生线程池。

```java
package io.github.atengk.demo.config;

import cn.hutool.core.thread.ThreadFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 原生线程池配置。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Configuration
public class NativeExecutorConfig {

    /**
     * 原生业务线程池。
     *
     * @return 原生线程池
     */
    @Bean(destroyMethod = "shutdown")
    public ExecutorService nativeBusinessExecutor() {
        return Executors.newFixedThreadPool(
                4,
                ThreadFactoryBuilder.create().setNamePrefix("native-business-").build()
        );
    }

}
```

线程池关闭建议：

- Spring 管理的线程池配置关闭等待。
- 原生线程池必须配置 `destroyMethod` 或在 `@PreDestroy` 中关闭。
- 异步任务应支持中断或超时，不要无限阻塞。
- 停机等待时间应覆盖常见任务耗时。
- 长耗时任务建议拆分、持久化进度或交给任务平台。
- 线程池关闭日志应清晰，便于确认资源释放。

### 资源释放

资源释放用于在应用关闭时清理本地缓存、临时文件、连接、客户端、锁、文件句柄等资源。Spring Bean 可以使用 `@PreDestroy`、`DisposableBean` 或 `@Bean(destroyMethod = "...")` 处理资源释放。

文件位置：`src/main/java/io/github/atengk/demo/service/LocalResourceService.java`

以下 Service 在应用关闭时清理本地资源。

```java
package io.github.atengk.demo.service;

import cn.hutool.core.map.MapUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 本地资源业务组件。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Service
public class LocalResourceService {

    private final Map<String, Object> localCache = new ConcurrentHashMap<>();

    /**
     * 初始化本地资源。
     */
    @PostConstruct
    public void init() {
        localCache.put("startup", true);
        log.info("本地资源初始化完成，资源数量：{}", localCache.size());
    }

    /**
     * 获取资源。
     *
     * @param key 资源键
     * @return 资源值
     */
    public Object get(String key) {
        return MapUtil.get(localCache, key, Object.class);
    }

    /**
     * 释放本地资源。
     */
    @PreDestroy
    public void destroy() {
        localCache.clear();
        log.info("本地资源释放完成");
    }

}
```

第三方客户端资源释放示例：

文件位置：`src/main/java/io/github/atengk/demo/support/DemoClient.java`

以下类模拟一个需要连接和关闭的第三方客户端。

```java
package io.github.atengk.demo.support;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 示例第三方客户端。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@RequiredArgsConstructor
public class DemoClient {

    private final String endpoint;

    /**
     * 打开客户端连接。
     */
    public void open() {
        log.info("打开示例客户端连接，endpoint：{}", endpoint);
    }

    /**
     * 关闭客户端连接。
     */
    public void close() {
        log.info("关闭示例客户端连接，endpoint：{}", endpoint);
    }

}
```

文件位置：`src/main/java/io/github/atengk/demo/config/DemoClientConfig.java`

以下配置通过 `initMethod` 和 `destroyMethod` 管理客户端生命周期。

```java
package io.github.atengk.demo.config;

import io.github.atengk.demo.support.DemoClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 示例客户端配置。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Configuration
public class DemoClientConfig {

    /**
     * 注册示例客户端。
     *
     * @return 示例客户端
     */
    @Bean(initMethod = "open", destroyMethod = "close")
    public DemoClient demoClient() {
        return new DemoClient("https://api.example.com");
    }

}
```

资源释放建议：

- 本地缓存、临时资源、客户端连接都应有释放逻辑。
- `@PreDestroy` 适合 Spring 管理的业务 Bean。
- 第三方对象可以使用 `@Bean(destroyMethod = "...")`。
- 资源释放方法应具备幂等性。
- 释放失败应记录日志，但通常不应阻塞整个关闭流程太久。
- 关闭过程中不要再提交新的异步任务。

### 停机验证方式

停机验证用于确认优雅停机配置是否生效。验证内容包括：应用是否接收 SIGTERM、是否等待慢请求完成、是否停止接收新请求、线程池是否关闭、资源是否释放、日志是否完整。

验证准备：

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  lifecycle:
    # 停机等待时间
    timeout-per-shutdown-phase: 30s

logging:
  level:
    # 输出项目日志，便于观察停机过程
    io.github.atengk: info
```

启动应用：

```bash
java -jar target/spring-boot3-demo.jar \
  --spring.profiles.active=dev \
  --server.port=18080
```

验证慢请求等待：

```bash
# 终端 1：发起慢请求
curl "http://localhost:18080/api/shutdown/sleep?seconds=20"

# 终端 2：查找进程并发送 SIGTERM
ps -ef | grep spring-boot3-demo | grep -v grep
kill -15 <pid>
```

验证健康检查变化：

```bash
# 停机前检查
curl http://localhost:18080/actuator/health

# 停机期间或停机后检查
curl http://localhost:18080/actuator/health
```

验证日志重点：

```text
慢请求开始执行
收到停止信号或应用开始关闭
停止接收新请求
慢请求执行完成
线程池关闭完成
本地资源释放完成
应用进程退出
```

systemd 验证：

```bash
# 启动服务
sudo systemctl start spring-boot3-demo

# 查看状态
sudo systemctl status spring-boot3-demo

# 停止服务
sudo systemctl stop spring-boot3-demo

# 查看停机日志
journalctl -u spring-boot3-demo -n 200
```

停机验证建议：

- 使用 `kill -15` 或 `systemctl stop` 验证，不使用 `kill -9`。
- 用慢请求验证是否等待已有请求完成。
- 验证停机时间是否小于发布平台允许的停止超时。
- 检查线程池和资源释放日志。
- 验证停机后端口不再监听。
- 在测试环境验证通过后，再应用到生产发布流程。
- Kubernetes 场景应同时配置 `terminationGracePeriodSeconds` 和就绪探针摘流量策略。



## 项目开发规范

项目开发规范用于统一团队编码风格、接口风格、包结构、日志输出和基础命名规则。规范的目标不是增加约束，而是降低协作成本，使不同开发者写出的代码结构一致、语义清晰、易于维护。

### 接口命名规范

接口命名应围绕资源设计，使用 HTTP 方法表达动作，使用路径表达资源。接口路径不建议出现 `get`、`add`、`update`、`delete` 等动词，因为这些动作已经可以通过请求方法表达。

推荐接口命名方式：

| 操作         | 请求方法 | 推荐路径                 | 说明         |
| ------------ | -------- | ------------------------ | ------------ |
| 查询用户列表 | `GET`    | `/api/users`             | 查询集合资源 |
| 查询用户详情 | `GET`    | `/api/users/{id}`        | 查询单个资源 |
| 创建用户     | `POST`   | `/api/users`             | 创建资源     |
| 修改用户     | `PUT`    | `/api/users/{id}`        | 更新资源     |
| 修改用户状态 | `PATCH`  | `/api/users/{id}/status` | 局部更新     |
| 删除用户     | `DELETE` | `/api/users/{id}`        | 删除资源     |

不推荐写法：

```text
/api/getUser
/api/addUser
/api/updateUser
/api/deleteUser
/api/user/list
/api/user/getById
```

推荐写法：

```text
GET    /api/users
GET    /api/users/{id}
POST   /api/users
PUT    /api/users/{id}
PATCH  /api/users/{id}/status
DELETE /api/users/{id}
```

业务动作无法完全用标准资源表达时，可以使用简洁动作名，但应控制使用范围。例如文件上传、导出、导入、审核、发布等接口：

```text
POST /api/files/upload
GET  /api/files/{id}/download
POST /api/users/import
GET  /api/users/export
POST /api/articles/{id}/publish
POST /api/orders/{id}/approve
```

接口命名建议：

- 路径统一使用小写字母和中划线。
- 资源名使用复数，例如 `/api/users`、`/api/orders`。
- 路径中不要出现 Java 方法名。
- 版本号可以放在路径中，例如 `/api/v1/users`。
- 后台管理、开放接口、内部接口可以通过前缀区分，例如 `/api/admin`、`/api/open`、`/api/internal`。
- 查询条件放在 Query 参数中，不放在路径中。
- 复杂查询可以使用 `POST /api/users/search`，但应在接口文档中说明原因。

### 类命名规范

类命名应根据职责使用固定后缀，使开发者通过类名即可判断该类用途。类名使用大驼峰命名，避免无意义缩写。

常见类命名规范：

| 类型         | 命名示例                 | 说明               |
| ------------ | ------------------------ | ------------------ |
| 启动类       | `DemoApplication`        | 应用启动入口       |
| Controller   | `UserController`         | HTTP 接口入口      |
| Service 接口 | `UserService`            | 业务接口           |
| Service 实现 | `UserServiceImpl`        | 业务实现           |
| Repository   | `UserRepository`         | 数据访问接口或组件 |
| DTO          | `UserCreateDTO`          | 请求参数对象       |
| Query        | `UserPageQuery`          | 查询参数对象       |
| VO           | `UserVO`                 | 响应对象           |
| Entity       | `UserEntity`             | 数据库实体对象     |
| Config       | `WebMvcConfig`           | 配置类             |
| Properties   | `FileStorageProperties`  | 配置属性绑定类     |
| Exception    | `BizException`           | 异常类             |
| Enum         | `UserStatusEnum`         | 枚举类             |
| Constants    | `CommonConstants`        | 常量类             |
| Util         | `TraceIdUtil`            | 工具类             |
| Handler      | `GlobalExceptionHandler` | 处理器类           |
| Interceptor  | `AuthInterceptor`        | 拦截器             |
| Filter       | `TraceIdFilter`          | 过滤器             |
| Listener     | `UserCreatedListener`    | 事件监听器         |
| Task         | `CleanupTask`            | 定时任务           |

推荐类命名：

```text
UserController
UserService
UserServiceImpl
UserRepository
UserCreateDTO
UserUpdateDTO
UserPageQuery
UserVO
UserDetailVO
UserEntity
UserStatusEnum
GlobalExceptionHandler
RequestContextFilter
```

不推荐类命名：

```text
UserCtl
UserBiz
UserManager
UserData
UserObj
UserParam
UserResult
UserUtil2
```

类命名建议：

- 类名必须表达清楚职责。
- 不使用拼音命名。
- 不使用无意义缩写。
- 不使用 `Data`、`Info`、`Object` 这类含义模糊的后缀，除非上下文非常明确。
- DTO、VO、Entity 不混用。
- 工具类必须以 `Util` 结尾，并且构造方法私有化。
- 配置属性类统一以 `Properties` 结尾。

工具类示例：

```java
package io.github.atengk.demo.common.util;

/**
 * 字符串掩码工具类。
 *
 * @author Ateng
 * @since 2026-05-09
 */
public class MaskUtil {

    private MaskUtil() {
    }

}
```

### 方法命名规范

方法命名应使用小驼峰，动词开头，表达清晰的业务语义。Controller 方法可以简洁，Service 方法应体现业务动作，Repository 方法应体现数据访问意图。

常见方法命名：

| 场景         | 推荐命名           | 说明         |
| ------------ | ------------------ | ------------ |
| 根据 ID 查询 | `getById`          | 查询单个对象 |
| 分页查询     | `page`             | 返回分页结果 |
| 列表查询     | `list`             | 返回集合     |
| 创建         | `create`           | 新增资源     |
| 修改         | `update`           | 更新资源     |
| 删除         | `delete`           | 删除资源     |
| 启用         | `enable`           | 状态变更     |
| 禁用         | `disable`          | 状态变更     |
| 校验         | `checkUserExists`  | 校验业务规则 |
| 统计         | `countByUserId`    | 返回数量     |
| 生成编号     | `generateOrderNo`  | 生成业务编号 |
| 初始化       | `initCache`        | 初始化数据   |
| 清理         | `cleanupTempFiles` | 清理资源     |

推荐写法：

```java
public UserVO getById(Long id)

public PageResult<UserVO> page(UserPageQuery query)

public UserVO create(UserCreateDTO dto)

public void update(Long id, UserUpdateDTO dto)

public void delete(Long id)

public void checkUserExists(Long userId)

public String generateOrderNo()
```

不推荐写法：

```java
public UserVO user(Long id)

public Object doSomething(Object param)

public void handle(UserDTO dto)

public void test()

public void process()
```

方法命名建议：

- 查询单个对象使用 `getById`、`getDetail`。
- 查询集合使用 `list`，分页查询使用 `page`。
- 新增使用 `create`，修改使用 `update`，删除使用 `delete`。
- 布尔判断方法使用 `is`、`has`、`can` 开头，例如 `isEnabled`、`hasPermission`、`canApprove`。
- 校验方法使用 `check` 或 `validate` 开头。
- 方法名不要过度简写。
- Service 方法名应体现业务含义，不要只体现技术动作。

### 包结构规范

包结构应按照职责分层组织，避免所有类堆放在同一个包中。推荐以启动类所在包作为根包，其他包放在根包下级，保证组件扫描范围完整。

推荐包结构：

```text
io.github.atengk.demo
├── DemoApplication.java
├── common
│   ├── annotation
│   ├── aspect
│   ├── constant
│   ├── context
│   ├── converter
│   ├── enums
│   ├── exception
│   ├── handler
│   ├── response
│   ├── util
│   └── validation
├── config
│   └── properties
├── controller
├── event
├── filter
├── interceptor
├── listener
├── model
│   ├── dto
│   ├── entity
│   ├── query
│   └── vo
├── repository
│   └── impl
├── runner
├── service
│   └── impl
└── task
```

各包职责说明：

| 包名                | 职责              |
| ------------------- | ----------------- |
| `controller`        | HTTP 接口入口     |
| `service`           | 业务接口          |
| `service.impl`      | 业务实现          |
| `repository`        | 数据访问          |
| `model.dto`         | 请求参数对象      |
| `model.query`       | 查询参数对象      |
| `model.vo`          | 响应对象          |
| `model.entity`      | 数据库实体        |
| `config`            | 配置类            |
| `config.properties` | 配置属性绑定      |
| `common.response`   | 统一响应          |
| `common.exception`  | 自定义异常        |
| `common.handler`    | 全局处理器        |
| `common.enums`      | 通用枚举          |
| `common.util`       | 工具类            |
| `filter`            | Servlet Filter    |
| `interceptor`       | Spring MVC 拦截器 |
| `listener`          | 事件监听器        |
| `task`              | 定时任务          |
| `runner`            | 启动任务          |

包结构建议：

- 启动类放在根包下。
- Controller、Service、Repository 明确分层。
- DTO、VO、Entity 分开放置。
- 公共能力放入 `common`，但不要把业务逻辑放入 `common`。
- 配置属性类放在 `config.properties`。
- 多模块项目中，公共模块不应依赖业务模块。
- 包名全部小写，不使用下划线和中划线。

### 日志输出规范

日志输出应服务于问题排查、业务审计和系统监控。日志不是越多越好，而是要在关键节点输出正确的信息。业务日志应包含业务 ID、用户 ID、TraceId、操作结果、异常原因和耗时等关键信息。

日志级别建议：

| 级别    | 使用场景                             |
| ------- | ------------------------------------ |
| `debug` | 本地调试、详细过程、临时排查         |
| `info`  | 正常业务流程、关键状态变化           |
| `warn`  | 业务失败、参数异常、可恢复问题       |
| `error` | 系统异常、外部依赖失败、不可恢复错误 |

推荐日志：

```java
log.info("创建用户成功，用户ID：{}，用户名：{}", userId, username);

log.warn("用户不存在，用户ID：{}", userId);

log.error("调用短信服务失败，用户ID：{}，手机号：{}", userId, safeMobile, e);
```

不推荐日志：

```java
log.info("进来了");

log.info("成功");

log.error(e.getMessage());

System.out.println("创建用户成功");
```

文件位置：`src/main/java/io/github/atengk/demo/service/UserLogStandardService.java`

下面的 Service 展示业务日志推荐写法。

```java
package io.github.atengk.demo.service;

import cn.hutool.core.util.DesensitizedUtil;
import io.github.atengk.demo.common.exception.BizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 用户日志规范业务组件。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Service
public class UserLogStandardService {

    public void createUser(Long userId, String username, String mobile) {
        if (userId == null) {
            log.warn("创建用户失败，用户ID为空，用户名：{}", username);
            throw new BizException("用户ID不能为空");
        }

        String safeMobile = DesensitizedUtil.mobilePhone(mobile);
        log.info("开始创建用户，用户ID：{}，用户名：{}，手机号：{}", userId, username, safeMobile);

        // 示例中省略具体创建逻辑

        log.info("创建用户成功，用户ID：{}，用户名：{}", userId, username);
    }

}
```

日志输出建议：

- 不使用 `System.out.println`。
- 日志内容使用中文，表达清楚业务含义。
- 日志必须使用占位符，不使用字符串拼接。
- 异常日志使用 `log.error("描述", e)`，保留堆栈。
- 敏感字段必须脱敏。
- 高频循环内谨慎输出 `info` 日志。
- 关键业务动作记录业务 ID。
- 外部接口调用失败记录接口名、业务 ID、状态码和耗时。

## 常见业务基础能力

常见业务基础能力是多个业务模块都会复用的基础组件，例如用户上下文、分页查询、枚举处理、错误码管理、请求追踪等。这些能力应统一设计、统一维护，避免每个模块重复实现。

### 统一用户上下文

统一用户上下文用于在一次请求中保存当前登录用户信息，例如用户 ID、用户名、租户 ID、角色编码等。它可以减少方法参数层层传递，但必须在请求结束后清理。

文件位置：`src/main/java/io/github/atengk/demo/common/context/LoginUser.java`

下面的对象用于保存当前登录用户信息。

```java
package io.github.atengk.demo.common.context;

import lombok.Data;

import java.util.List;

/**
 * 当前登录用户。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
public class LoginUser {

    /**
     * 用户 ID。
     */
    private Long userId;

    /**
     * 用户名。
     */
    private String username;

    /**
     * 租户 ID。
     */
    private String tenantId;

    /**
     * 角色编码列表。
     */
    private List<String> roles;

}
```

文件位置：`src/main/java/io/github/atengk/demo/common/context/LoginUserContext.java`

下面的上下文持有器使用 `ThreadLocal` 保存当前登录用户。

```java
package io.github.atengk.demo.common.context;

/**
 * 登录用户上下文。
 *
 * @author Ateng
 * @since 2026-05-09
 */
public class LoginUserContext {

    private static final ThreadLocal<LoginUser> LOGIN_USER_HOLDER = new ThreadLocal<>();

    private LoginUserContext() {
    }

    public static void set(LoginUser loginUser) {
        LOGIN_USER_HOLDER.set(loginUser);
    }

    public static LoginUser get() {
        return LOGIN_USER_HOLDER.get();
    }

    public static Long getUserId() {
        LoginUser loginUser = get();
        return loginUser == null ? null : loginUser.getUserId();
    }

    public static String getTenantId() {
        LoginUser loginUser = get();
        return loginUser == null ? null : loginUser.getTenantId();
    }

    public static void clear() {
        LOGIN_USER_HOLDER.remove();
    }

}
```

文件位置：`src/main/java/io/github/atengk/demo/filter/LoginUserContextFilter.java`

下面的 Filter 从请求头中读取用户信息并写入上下文。实际项目中应从 Token 解析用户信息，这里只做基础示例。

```java
package io.github.atengk.demo.filter;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.demo.common.context.LoginUser;
import io.github.atengk.demo.common.context.LoginUserContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * 登录用户上下文过滤器。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Component
public class LoginUserContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            LoginUser loginUser = buildLoginUser(request);
            if (loginUser != null) {
                LoginUserContext.set(loginUser);
            }
            filterChain.doFilter(request, response);
        } finally {
            LoginUserContext.clear();
        }
    }

    private LoginUser buildLoginUser(HttpServletRequest request) {
        String userIdText = request.getHeader("X-User-Id");
        if (!NumberUtil.isLong(userIdText)) {
            return null;
        }

        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(Long.valueOf(userIdText));
        loginUser.setUsername(StrUtil.blankToDefault(request.getHeader("X-Username"), "unknown"));
        loginUser.setTenantId(request.getHeader("X-Tenant-Id"));
        loginUser.setRoles(List.of());
        return loginUser;
    }

}
```

使用示例：

```java
Long currentUserId = LoginUserContext.getUserId();
String tenantId = LoginUserContext.getTenantId();
```

统一用户上下文建议：

- 上下文只保存请求级轻量信息。
- 用户信息应来自可信认证链路，不直接信任普通客户端 Header。
- 请求结束必须清理 `ThreadLocal`。
- 异步任务中不会自动继承普通 `ThreadLocal`，需要显式传递。
- Service 层可以读取上下文，但不要在 Entity 中依赖上下文。

### 统一分页查询

统一分页查询用于规范所有列表接口的分页参数和分页响应结构。前端只需要适配一套分页格式，后端也可以统一分页校验、默认值和最大分页大小。

文件位置：`src/main/java/io/github/atengk/demo/common/query/PageQuery.java`

下面的分页查询基类定义页码和每页条数。

```java
package io.github.atengk.demo.common.query;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 分页查询参数。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
public class PageQuery {

    /**
     * 页码。
     */
    @Min(value = 1, message = "页码不能小于1")
    private Long pageNum = 1L;

    /**
     * 每页条数。
     */
    @Min(value = 1, message = "每页条数不能小于1")
    @Max(value = 100, message = "每页条数不能大于100")
    private Long pageSize = 10L;

    public Long offset() {
        return (pageNum - 1) * pageSize;
    }

}
```

文件位置：`src/main/java/io/github/atengk/demo/model/query/UserListQuery.java`

下面的用户查询对象继承通用分页参数。

```java
package io.github.atengk.demo.model.query;

import io.github.atengk.demo.common.query.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户列表查询参数。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserListQuery extends PageQuery {

    /**
     * 查询关键字。
     */
    private String keyword;

    /**
     * 用户状态。
     */
    private Integer status;

}
```

文件位置：`src/main/java/io/github/atengk/demo/common/response/PageResult.java`

下面的分页响应对象用于统一返回分页数据。

```java
package io.github.atengk.demo.common.response;

import cn.hutool.core.collection.CollUtil;
import lombok.Data;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 分页响应对象。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
public class PageResult<T> implements Serializable {

    private Long pageNum;

    private Long pageSize;

    private Long total;

    private Long pages;

    private List<T> records;

    public static <T> PageResult<T> of(Long pageNum, Long pageSize, Long total, List<T> records) {
        PageResult<T> result = new PageResult<>();
        result.setPageNum(pageNum);
        result.setPageSize(pageSize);
        result.setTotal(total);
        result.setPages(calculatePages(total, pageSize));
        result.setRecords(CollUtil.isEmpty(records) ? Collections.emptyList() : records);
        return result;
    }

    public static <T> PageResult<T> empty(Long pageNum, Long pageSize) {
        return of(pageNum, pageSize, 0L, Collections.emptyList());
    }

    private static Long calculatePages(Long total, Long pageSize) {
        if (total == null || total <= 0 || pageSize == null || pageSize <= 0) {
            return 0L;
        }
        return (total + pageSize - 1) / pageSize;
    }

}
```

统一分页建议：

- 所有分页接口使用 `pageNum`、`pageSize`。
- `pageNum` 默认 `1`，`pageSize` 默认 `10`。
- `pageSize` 必须设置最大值。
- 空分页返回 `records: []`，不返回 `null`。
- 不同数据访问技术最终都转换成统一 `PageResult<T>`。
- 排序字段如果允许前端传入，必须使用白名单校验。

### 统一枚举处理

统一枚举处理用于规范枚举编码、描述、数据库存储和接口返回。业务枚举应有稳定编码，不建议使用枚举下标 `ordinal`，也不建议数据库直接存储枚举名称。

文件位置：`src/main/java/io/github/atengk/demo/common/enums/CodeEnum.java`

下面的接口定义统一编码枚举能力。

```java
package io.github.atengk.demo.common.enums;

/**
 * 编码枚举接口。
 *
 * @author Ateng
 * @since 2026-05-09
 */
public interface CodeEnum<T> {

    /**
     * 获取编码。
     *
     * @return 编码
     */
    T getCode();

    /**
     * 获取描述。
     *
     * @return 描述
     */
    String getDescription();

}
```

文件位置：`src/main/java/io/github/atengk/demo/common/enums/UserStatusEnum.java`

下面的用户状态枚举实现统一编码枚举接口。

```java
package io.github.atengk.demo.common.enums;

import cn.hutool.core.util.ObjectUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 用户状态枚举。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Getter
@RequiredArgsConstructor
public enum UserStatusEnum implements CodeEnum<Integer> {

    ENABLE(1, "启用"),

    DISABLE(0, "禁用");

    private final Integer code;

    private final String description;

    public static UserStatusEnum of(Integer code) {
        if (code == null) {
            return null;
        }

        for (UserStatusEnum value : values()) {
            if (ObjectUtil.equals(value.getCode(), code)) {
                return value;
            }
        }

        return null;
    }

}
```

文件位置：`src/main/java/io/github/atengk/demo/common/util/EnumUtil.java`

下面的工具类用于根据编码查找枚举。

```java
package io.github.atengk.demo.common.util;

import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.demo.common.enums.CodeEnum;

/**
 * 枚举工具类。
 *
 * @author Ateng
 * @since 2026-05-09
 */
public class EnumUtil {

    private EnumUtil() {
    }

    public static <E extends Enum<E> & CodeEnum<T>, T> E getByCode(Class<E> enumClass, T code) {
        if (code == null || enumClass == null) {
            return null;
        }

        for (E enumValue : enumClass.getEnumConstants()) {
            if (ObjectUtil.equals(enumValue.getCode(), code)) {
                return enumValue;
            }
        }

        return null;
    }

}
```

使用示例：

```java
UserStatusEnum status = EnumUtil.getByCode(UserStatusEnum.class, 1);
```

统一枚举建议：

- 枚举必须包含编码和描述。
- 数据库存储编码，不存储 `ordinal`。
- 接口响应可以返回编码，也可以同时返回描述。
- 请求参数传入编码时，应统一转换为枚举。
- 枚举编码一旦发布，不随意变更。
- 删除枚举值要考虑历史数据兼容。

### 统一错误码管理

统一错误码管理用于规范业务失败和系统失败的返回结果。错误码应集中维护，避免不同模块重复定义、含义冲突或返回格式不一致。

文件位置：`src/main/java/io/github/atengk/demo/common/response/ResultCodeEnum.java`

下面的响应码枚举按通用、用户、订单、文件等业务域划分错误码。

```java
package io.github.atengk.demo.common.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 统一响应码枚举。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Getter
@RequiredArgsConstructor
public enum ResultCodeEnum {

    SUCCESS(200, "操作成功"),

    BAD_REQUEST(400, "请求参数错误"),

    UNAUTHORIZED(401, "未认证"),

    FORBIDDEN(403, "无访问权限"),

    NOT_FOUND(404, "资源不存在"),

    METHOD_NOT_ALLOWED(405, "请求方法不支持"),

    INTERNAL_ERROR(500, "系统异常"),

    BIZ_ERROR(1000, "业务处理失败"),

    USER_NOT_FOUND(2001, "用户不存在"),

    USER_DISABLED(2002, "用户已被禁用"),

    ORDER_NOT_FOUND(3001, "订单不存在"),

    ORDER_STATUS_NOT_ALLOWED(3002, "订单状态不允许操作"),

    FILE_NOT_FOUND(4001, "文件不存在"),

    FILE_TYPE_NOT_ALLOWED(4002, "不支持的文件类型"),

    FILE_SIZE_EXCEEDED(4003, "文件大小超过限制");

    private final Integer code;

    private final String message;

}
```

文件位置：`src/main/java/io/github/atengk/demo/common/exception/BizException.java`

下面的业务异常支持直接使用错误码枚举。

```java
package io.github.atengk.demo.common.exception;

import io.github.atengk.demo.common.response.ResultCodeEnum;
import lombok.Getter;

/**
 * 业务异常。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Getter
public class BizException extends RuntimeException {

    private final Integer code;

    public BizException(String message) {
        super(message);
        this.code = ResultCodeEnum.BIZ_ERROR.getCode();
    }

    public BizException(ResultCodeEnum resultCodeEnum) {
        super(resultCodeEnum.getMessage());
        this.code = resultCodeEnum.getCode();
    }

    public BizException(Integer code, String message) {
        super(message);
        this.code = code;
    }

}
```

使用示例：

```java
throw new BizException(ResultCodeEnum.USER_NOT_FOUND);
```

统一错误码建议：

- 错误码集中定义在枚举中。
- 不同业务域预留不同号段。
- 前端依赖的错误码不能随意变更。
- 错误消息应简洁明确。
- 系统异常返回通用错误码，不暴露内部细节。
- 文档中同步维护错误码说明和处理建议。

### 统一请求追踪

统一请求追踪用于给每个请求分配 TraceId，并在日志、响应头、下游请求中持续传递。它是排查生产问题的重要基础能力。

文件位置：`src/main/java/io/github/atengk/demo/filter/TraceIdFilter.java`

下面的 Filter 为每个请求生成或接收 TraceId，并写入 MDC 和响应头。

```java
package io.github.atengk.demo.filter;

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
 * 请求追踪过滤器。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Component
public class TraceIdFilter extends OncePerRequestFilter {

    private static final String TRACE_ID = "traceId";

    private static final String TRACE_HEADER = "X-Trace-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String traceId = StrUtil.blankToDefault(request.getHeader(TRACE_HEADER), IdUtil.fastSimpleUUID());

        MDC.put(TRACE_ID, traceId);
        response.setHeader(TRACE_HEADER, traceId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(TRACE_ID);
        }
    }

}
```

文件位置：`src/main/resources/logback-spring.xml`

日志格式中加入 TraceId。

```xml
<property name="CONSOLE_LOG_PATTERN"
          value="%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%thread] [%X{traceId:-}] %logger{36} - %msg%n"/>
```

下游调用传递 TraceId：

```java
package io.github.atengk.demo.config;

import cn.hutool.core.util.StrUtil;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * RestClient 请求追踪配置。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Configuration
public class TraceRestClientConfig {

    @Bean
    public RestClient restClient() {
        return RestClient.builder()
                .requestInterceptor((request, body, execution) -> {
                    String traceId = MDC.get("traceId");
                    if (StrUtil.isNotBlank(traceId)) {
                        request.getHeaders().add("X-Trace-Id", traceId);
                    }
                    return execution.execute(request, body);
                })
                .build();
    }

}
```

统一请求追踪建议：

- 所有请求都应有 TraceId。
- 如果上游传入 TraceId，则沿用；否则后端生成。
- TraceId 写入响应头，方便前端反馈问题。
- 日志格式中必须包含 TraceId。
- 调用下游服务时继续传递 TraceId。
- 异步任务中如果需要 TraceId，应显式传递或封装上下文。

## 常见问题处理

常见问题处理用于整理 Spring Boot 3 开发中高频问题的排查思路。排查问题时建议先看启动日志、异常堆栈、配置文件、依赖树，再定位代码逻辑，不要直接猜测。

### 配置不生效

配置不生效通常表现为端口没有变化、Profile 没有切换、配置属性为 `null`、外部配置没有覆盖内部配置、生产配置没有加载等。

常见原因：

| 原因           | 说明                                      |
| -------------- | ----------------------------------------- |
| Profile 未激活 | 没有指定 `spring.profiles.active`         |
| 文件名错误     | 应为 `application-{profile}.yml`          |
| YAML 缩进错误  | 缩进不正确导致配置未绑定                  |
| 配置路径错误   | 外部配置路径没有被加载                    |
| 配置项名称错误 | 属性名和绑定类字段不匹配                  |
| 优先级被覆盖   | 命令行参数或环境变量覆盖了文件配置        |
| 配置类未注册   | `@ConfigurationProperties` 未被扫描或启用 |

排查命令：

```bash
# 查看启动时激活的 Profile
java -jar spring-boot3-demo.jar --spring.profiles.active=prod

# 指定外部配置目录
java -jar spring-boot3-demo.jar \
  --spring.profiles.active=prod \
  --spring.config.additional-location=file:/data/spring-boot3-demo/config/

# 查看进程启动参数
jcmd <pid> VM.command_line
```

配置属性类检查：

```java
package io.github.atengk.demo.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 示例配置属性。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
@ConfigurationProperties(prefix = "demo.app")
public class DemoAppProperties {

    private String name;

    private String version;

}
```

启动类需要开启扫描：

```java
package io.github.atengk.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Spring Boot 3 应用启动类。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

}
```

配置文件示例：

```yaml
demo:
  app:
    name: spring-boot3-demo
    version: 1.0.0
```

排查建议：

- 查看启动日志中的 active profiles。
- 确认配置文件名称和路径。
- 检查 YAML 缩进。
- 检查配置项前缀和字段名。
- 检查是否开启 `@ConfigurationPropertiesScan`。
- 检查命令行参数、环境变量是否覆盖配置文件。
- 通过 Actuator `/actuator/env` 在安全环境下确认配置来源。

### Bean 注入失败

Bean 注入失败通常表现为应用启动失败，提示 `No qualifying bean`、`required a bean of type`、`expected single matching bean but found` 等。

常见原因：

| 原因                | 说明                                            |
| ------------------- | ----------------------------------------------- |
| 类未加组件注解      | 没有 `@Service`、`@Component`、`@Repository` 等 |
| 不在扫描路径下      | 类不在启动类所在包或子包下                      |
| 存在多个同类型 Bean | 未使用 `@Qualifier` 或 `@Primary`               |
| 条件装配未满足      | `@ConditionalOnProperty` 等条件不成立           |
| 配置类未生效        | `@Configuration` 未被扫描                       |
| 循环依赖            | Bean 之间互相依赖                               |
| 构造器参数缺失      | 必需依赖无法注入                                |

典型错误：

```text
Parameter 0 of constructor in io.github.atengk.demo.service.UserService required a bean of type
'io.github.atengk.demo.repository.UserRepository' that could not be found.
```

错误示例：

```java
package io.github.atengk.demo.repository;

/**
 * 用户数据访问组件。
 *
 * @author Ateng
 * @since 2026-05-09
 */
public class UserRepository {
}
```

修复方式：添加组件注解。

```java
package io.github.atengk.demo.repository;

import org.springframework.stereotype.Repository;

/**
 * 用户数据访问组件。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Repository
public class UserRepository {
}
```

多个同类型 Bean 注入失败时，使用 `@Qualifier`：

```java
package io.github.atengk.demo.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * 消息通知业务组件。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Service
public class MessageNotifyService {

    private final MessageSender messageSender;

    public MessageNotifyService(@Qualifier("smsMessageSender") MessageSender messageSender) {
        this.messageSender = messageSender;
    }

}
```

排查建议：

- 确认类是否加了 Spring 组件注解。
- 确认类是否在启动类扫描范围内。
- 多个实现时使用 `@Qualifier` 或 `@Primary`。
- 检查条件装配配置是否满足。
- 查看完整异常堆栈第一处 `Caused by`。
- 避免 Service 之间互相构造器注入形成循环依赖。
- 不要手动 `new` 需要 Spring 管理的业务对象。

### 接口参数接收失败

接口参数接收失败通常表现为参数为 `null`、JSON 请求体无法解析、类型转换失败、表单参数接收不到、文件上传失败等。

常见原因：

| 场景               | 原因                                                       |
| ------------------ | ---------------------------------------------------------- |
| JSON 请求体为空    | 缺少 `@RequestBody` 或 `Content-Type` 错误                 |
| Query 参数接收不到 | 参数名不一致                                               |
| 路径参数接收失败   | `@PathVariable` 名称不一致                                 |
| 日期参数转换失败   | 日期格式不匹配                                             |
| 枚举参数转换失败   | 传入值和枚举转换规则不一致                                 |
| 文件参数为空       | 表单字段名不是 `file` 或请求类型不是 `multipart/form-data` |
| 表单参数接收失败   | 使用了错误注解或 Content-Type 不匹配                       |

JSON 请求体正确写法：

```java
package io.github.atengk.demo.controller;

import io.github.atengk.demo.common.response.ApiResult;
import io.github.atengk.demo.model.dto.UserCreateDTO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 参数接收示例接口。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@RestController
@RequestMapping("/api/param-check/users")
public class ParamCheckController {

    @PostMapping
    public ApiResult<Boolean> create(@Valid @RequestBody UserCreateDTO dto) {
        return ApiResult.success(true);
    }

}
```

请求示例：

```bash
curl -X POST "http://localhost:8080/api/param-check/users" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "ateng",
    "nickname": "Ateng"
  }'
```

Query 参数正确写法：

```java
@GetMapping("/search")
public ApiResult<String> search(@RequestParam String keyword) {
    return ApiResult.success(keyword);
}
```

文件上传正确写法：

```java
@PostMapping("/upload")
public ApiResult<String> upload(@RequestParam("file") MultipartFile file) {
    return ApiResult.success(file.getOriginalFilename());
}
```

请求示例：

```bash
curl -X POST "http://localhost:8080/api/files/upload" \
  -F "file=@/Users/ateng/Desktop/test.pdf"
```

排查建议：

- JSON 请求确认 `Content-Type: application/json`。
- 表单请求确认 `Content-Type: application/x-www-form-urlencoded` 或 `multipart/form-data`。
- 文件上传确认字段名和 `@RequestParam("file")` 一致。
- 路径参数名称保持一致。
- 日期时间参数格式和后端格式化规则一致。
- 查看全局异常处理器中的参数异常日志。
- 使用 curl 复现问题，排除前端封装影响。

### 事务不回滚

事务不回滚通常是因为事务没有生效、异常没有触发回滚规则、异常被吞掉或数据库本身不支持事务。

常见原因：

| 原因                   | 说明                            |
| ---------------------- | ------------------------------- |
| 方法不是 `public`      | Spring 代理通常拦截 public 方法 |
| 同类内部调用           | 没有经过代理对象                |
| 异常被捕获未抛出       | 事务感知不到异常                |
| 抛出受检异常           | 默认不回滚受检异常              |
| 类未交给 Spring 管理   | 没有事务代理                    |
| 数据库表不支持事务     | MyISAM 不支持事务               |
| 多数据源事务管理器错误 | 使用了错误事务管理器            |
| 异步线程执行写库       | 事务不跨线程传播                |

推荐写法：

```java
package io.github.atengk.demo.service;

import io.github.atengk.demo.common.exception.BizException;
import io.github.atengk.demo.repository.OrderRepository;
import io.github.atengk.demo.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 订单事务业务组件。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderTransactionService {

    private final OrderRepository orderRepository;

    private final StockRepository stockRepository;

    @Transactional(rollbackFor = Exception.class)
    public void createOrder(Long userId, Long productId) {
        int orderRows = orderRepository.insertOrder(userId, productId);
        if (orderRows != 1) {
            throw new BizException("订单创建失败");
        }

        int stockRows = stockRepository.decreaseStock(productId);
        if (stockRows != 1) {
            throw new BizException("库存扣减失败");
        }

        log.info("订单创建成功，用户ID：{}，商品ID：{}", userId, productId);
    }

}
```

错误写法：异常被吞掉。

```java
@Transactional(rollbackFor = Exception.class)
public void createOrder() {
    try {
        // 执行数据库操作
    } catch (Exception e) {
        log.error("创建订单失败，但异常被吞掉", e);
    }
}
```

正确写法：记录日志后继续抛出。

```java
@Transactional(rollbackFor = Exception.class)
public void createOrder() {
    try {
        // 执行数据库操作
    } catch (Exception e) {
        log.error("创建订单失败，准备回滚事务", e);
        throw e;
    }
}
```

排查建议：

- 确认方法是 `public`。
- 确认类由 Spring 管理。
- 确认是通过代理 Bean 调用，不是同类内部调用。
- 确认异常没有被吞掉。
- 对写操作配置 `rollbackFor = Exception.class`。
- 检查数据库表是否为 InnoDB。
- 开启事务日志：

```yaml
logging:
  level:
    org.springframework.transaction: debug
```

### 文件上传失败

文件上传失败通常表现为接口返回 400、文件参数为空、超过大小限制、类型不允许、保存失败、目录无权限等。

常见原因：

| 原因           | 说明                                        |
| -------------- | ------------------------------------------- |
| 请求类型错误   | 没有使用 `multipart/form-data`              |
| 字段名错误     | 前端字段名和 `@RequestParam("file")` 不一致 |
| 文件超过限制   | `max-file-size` 或 `max-request-size` 太小  |
| 上传目录不存在 | 目录未创建                                  |
| 目录无权限     | 应用用户没有写权限                          |
| 文件类型不允许 | 扩展名不在白名单                            |
| 文件名非法     | 包含路径穿越字符                            |
| 临时目录不可用 | 系统临时目录没有空间或权限                  |

配置检查：

```yaml
spring:
  servlet:
    multipart:
      # 单个文件最大大小
      max-file-size: 20MB
      # 单次请求最大大小
      max-request-size: 50MB

demo:
  file:
    upload-path: /data/spring-boot3-demo/upload
    allowed-extensions:
      - jpg
      - png
      - pdf
      - xlsx
```

上传接口示例：

```java
package io.github.atengk.demo.controller;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.demo.common.exception.BizException;
import io.github.atengk.demo.common.response.ApiResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

/**
 * 文件上传排查接口。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@RestController
@RequestMapping("/api/upload-check")
public class UploadCheckController {

    @PostMapping
    public ApiResult<String> upload(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException("上传文件不能为空");
        }

        String originalFilename = StrUtil.blankToDefault(file.getOriginalFilename(), "unknown");
        String extension = FileUtil.extName(originalFilename);
        String storageFilename = IdUtil.fastSimpleUUID() + "." + extension;
        File targetFile = FileUtil.file("./data/upload", storageFilename);
        FileUtil.mkParentDirs(targetFile);

        try {
            file.transferTo(targetFile);
            log.info("文件上传成功，原文件名：{}，保存路径：{}", originalFilename, targetFile.getAbsolutePath());
            return ApiResult.success(targetFile.getAbsolutePath());
        } catch (Exception e) {
            log.error("文件上传失败，原文件名：{}", originalFilename, e);
            throw new BizException("文件上传失败");
        }
    }

}
```

curl 验证：

```bash
curl -X POST "http://localhost:8080/api/upload-check" \
  -F "file=@/Users/ateng/Desktop/test.pdf"
```

服务器目录检查：

```bash
# 检查目录是否存在
ls -ld /data/spring-boot3-demo/upload

# 创建目录
mkdir -p /data/spring-boot3-demo/upload

# 修改目录归属
chown -R app:app /data/spring-boot3-demo/upload

# 检查磁盘空间
df -h
```

排查建议：

- 确认前端使用 `multipart/form-data`。
- 确认字段名是 `file`。
- 确认文件大小没有超过配置限制。
- 确认上传目录存在且应用用户有写权限。
- 确认磁盘空间充足。
- 确认文件扩展名在白名单内。
- 生产环境不建议把上传目录放在项目 `target` 或 Jar 所在目录中。
- 大文件上传建议使用对象存储或分片上传。