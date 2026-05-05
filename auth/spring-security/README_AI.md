# Spring Security

Spring Security 是 Spring 体系中用于处理认证、授权和常见 Web 安全防护的核心安全框架。本文以 Spring Boot 3.x、Spring Security 6.x、Servlet Web 应用为基础，说明后续开发所需的技术背景、版本要求、依赖配置和项目结构。Spring Security 官方文档将其核心能力概括为认证、授权和常见攻击防护，并提供与 Spring 应用生态的集成能力。([Home](https://docs.spring.io/spring-security/reference/6.5/features/index.html))

## 技术概述

本节用于建立 Spring Security 开发前的基础认知，重点说明它解决什么问题、Spring Boot 3 中有哪些适配变化，以及实际项目中常见的认证授权场景。

### Spring Security 核心能力

Spring Security 的核心职责是保护应用接口，控制“谁能访问系统”和“访问后能做什么”。在 Spring MVC 项目中，它主要通过 Servlet Filter 机制介入请求链路，在 Controller 执行业务逻辑之前完成安全校验。Spring Security 的 Servlet 支持基于标准 Servlet Filter，Spring MVC 应用中的请求会先经过 FilterChain，再进入 DispatcherServlet。([Home](https://docs.spring.io/spring-security/reference/6.5/servlet/architecture.html))

核心能力主要包括以下几类。

| 能力                | 说明                                                         | 常见组件                                                     |
| ------------------- | ------------------------------------------------------------ | ------------------------------------------------------------ |
| 认证 Authentication | 判断当前请求是谁发起的，例如用户名密码登录、Token 登录、OAuth2 登录 | `AuthenticationManager`、`AuthenticationProvider`、`UserDetailsService`、`PasswordEncoder` |
| 授权 Authorization  | 判断当前用户是否有权限访问某个资源或执行某个方法             | `AuthorizationFilter`、`AuthorizationManager`、`@PreAuthorize` |
| 安全上下文          | 保存当前请求中的认证信息，供业务代码获取当前登录用户         | `SecurityContext`、`SecurityContextHolder`                   |
| Web 安全防护        | 提供 CSRF、防安全响应头、请求防火墙等能力                    | `CsrfFilter`、`HeaderWriterFilter`、`HttpFirewall`           |
| 会话与退出          | 管理 Session、Remember-Me、Logout、匿名访问等                | `SessionManagementFilter`、`LogoutFilter`、`AnonymousAuthenticationFilter` |
| 第三方协议支持      | 支持 OAuth2 Login、OAuth2 Resource Server、SAML2 等安全协议  | `oauth2-client`、`oauth2-resource-server`                    |

在请求处理链路中，Spring Security 会通过 `FilterChainProxy` 委托多个安全过滤器执行认证、授权和安全防护逻辑；`SecurityFilterChain` 用于判断当前请求应该触发哪些安全过滤器。官方文档也明确说明，安全过滤器可以用于攻击防护、认证、授权等场景，并且过滤器顺序很重要。([Home](https://docs.spring.io/spring-security/reference/6.5/servlet/architecture.html))

一个典型请求的大致流程如下：

```text
客户端请求
  -> Servlet FilterChain
  -> DelegatingFilterProxy
  -> FilterChainProxy
  -> SecurityFilterChain
  -> CSRF / CORS / 登录认证 / Token 认证 / 权限校验
  -> DispatcherServlet
  -> Controller
  -> Service
  -> 返回响应
```

后续开发时，重点关注三个配置入口：

| 配置入口              | 作用                                                         |
| --------------------- | ------------------------------------------------------------ |
| `SecurityFilterChain` | 配置 URL 权限、登录方式、登出、CSRF、异常处理、Session 策略等 |
| `UserDetailsService`  | 加载用户信息，通常对接数据库用户表                           |
| `PasswordEncoder`     | 处理密码加密与密码匹配，生产环境不应使用明文密码             |

### Spring Boot 3 适配变化

Spring Boot 3 与 Spring Security 6 配合使用时，和 Spring Boot 2.x / Spring Security 5.x 相比有明显变化。Spring Boot 3.0 升级到了 Spring Security 6.0，并要求 Java 17 及以上版本；同时 Spring Boot 3 基于 Spring Framework 6，并迁移到 Jakarta EE API。([GitHub](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-3.0-Migration-Guide?utm_source=chatgpt.com))

需要重点关注以下变化。

| 变化点           | Spring Boot 2.x / Security 5.x               | Spring Boot 3.x / Security 6.x                           |
| ---------------- | -------------------------------------------- | -------------------------------------------------------- |
| JDK 要求         | 可使用 Java 8、11、17，取决于具体 Boot 版本  | Java 17 起步                                             |
| Java EE 包名     | 常见 `javax.servlet.*`、`javax.validation.*` | 使用 `jakarta.servlet.*`、`jakarta.validation.*`         |
| 安全配置方式     | 早期常见继承 `WebSecurityConfigurerAdapter`  | 使用 `SecurityFilterChain` Bean                          |
| 方法级安全       | 常见 `@EnableGlobalMethodSecurity`           | 推荐 `@EnableMethodSecurity`                             |
| URL 授权配置     | 旧代码常见 `authorizeRequests()`             | 推荐 `authorizeHttpRequests()`                           |
| 认证授权底层模型 | 旧式投票器、元数据源较常见                   | 更强调 `AuthorizationManager` API                        |
| 默认安全行为     | 引入 security 后默认保护应用                 | 引入 security 后仍默认保护应用，并可通过自定义 Bean 覆盖 |

Spring Boot 3 中只要 `spring-boot-starter-security` 在 classpath 中，Web 应用默认就会被保护，包括 `/error` 端点；默认情况下会生成一个用户名为 `user` 的内存用户，密码会在应用启动日志中以 WARN 级别打印，仅适合开发调试。([Home](https://docs.spring.io/spring-boot/3.5/reference/web/spring-security.html))

实际项目中一般不会依赖默认用户，而是通过以下方式覆盖默认行为：

```text
定义 SecurityFilterChain Bean
  -> 覆盖默认 Web 安全规则

定义 UserDetailsService / AuthenticationProvider / AuthenticationManager Bean
  -> 覆盖默认用户认证逻辑
```

Spring Boot 官方文档说明，添加 `SecurityFilterChain` Bean 后可以覆盖访问规则；如果要关闭默认 `UserDetailsService` 配置，则需要提供自己的 `UserDetailsService`、`AuthenticationProvider` 或 `AuthenticationManager` Bean。([Home](https://docs.spring.io/spring-boot/3.5/reference/web/spring-security.html))

### 典型认证与授权场景

Spring Security 可以覆盖传统服务端页面系统、前后端分离系统、后台管理系统、开放 API 和微服务资源服务等场景。本文后续章节建议以前后端分离接口系统为主线，重点实现数据库用户认证、权限控制、JWT 无状态认证和统一异常响应。

常见场景如下。

| 场景           | 认证方式                                      | 授权方式                   | 适用项目                 |
| -------------- | --------------------------------------------- | -------------------------- | ------------------------ |
| 服务端页面登录 | Form Login + Session                          | URL 规则 + 方法注解        | Thymeleaf、JSP、传统后台 |
| 前后端分离登录 | JSON 登录接口 + JWT                           | URL 规则 + `@PreAuthorize` | Vue、React、移动端 API   |
| 管理后台 RBAC  | 用户、角色、权限表                            | 角色权限模型               | 后台管理系统             |
| 开放接口访问   | API Key、Bearer Token、OAuth2 Resource Server | Scope、Authority           | 开放平台、网关 API       |
| 微服务资源保护 | OAuth2 Resource Server + JWT                  | Token 中的权限声明         | 微服务内部资源服务       |
| 简单内部系统   | HTTP Basic 或内存用户                         | 简单角色控制               | 内部工具、临时管理端     |

对于前后端分离项目，推荐采用以下模式：

```text
用户提交用户名和密码
  -> 后端认证成功
  -> 后端生成 Access Token
  -> 前端保存 Token
  -> 后续请求在 Authorization 请求头中携带 Bearer Token
  -> 后端 JWT 过滤器解析 Token
  -> 写入 SecurityContext
  -> Spring Security 执行 URL 或方法级权限判断
```

方法级授权适合放在 Service 或 Controller 方法上，用于细粒度权限控制。Spring Security 支持通过 `@EnableMethodSecurity` 启用方法级安全，并可使用 `@PreAuthorize`、`@PostAuthorize`、`@PreFilter`、`@PostFilter` 等注解控制方法调用权限；Spring Boot Starter Security 默认不会自动启用方法级授权。([Home](https://docs.spring.io/spring-security/reference/6.5/servlet/authorization/method-security.html))

## 环境准备

本节用于准备 Spring Boot 3 + Spring Security 开发所需的基础环境，包括 JDK、Maven、依赖配置和推荐项目结构。后续认证、授权、JWT 和异常处理章节都基于这里的基础工程继续扩展。

### JDK 与 Spring Boot 版本要求

建议使用 Spring Boot 3.5.x 作为 Spring Boot 3 系列的稳定开发基线。以 Spring Boot 3.5.14 为例，官方要求至少 Java 17，并兼容到 Java 25；Maven 需要 3.6.3 或更高版本。([Home](https://docs.spring.io/spring-boot/3.5/system-requirements.html))

| 项目            | 推荐版本     | 说明                                            |
| --------------- | ------------ | ----------------------------------------------- |
| JDK             | 17+          | Spring Boot 3 起步要求 Java 17                  |
| Spring Boot     | 3.5.x        | 本文以 Boot 3.5.x / Security 6.x 为主           |
| Spring Security | 6.x          | 由 Spring Boot Starter 统一管理版本             |
| Maven           | 3.6.3+       | 推荐使用 3.9.x                                  |
| Servlet 容器    | Tomcat 10.1+ | Boot 3.5.x 默认内嵌 Tomcat 10.1，Servlet 6.0    |
| 包名规范        | `jakarta.*`  | 不再使用旧的 `javax.*` Servlet / Validation API |

可以通过以下命令检查本地环境。

```bash
# 查看 JDK 版本，要求 17 或更高
java -version

# 查看 Maven 版本，要求 3.6.3 或更高
mvn -v
```

命令输出中重点检查 `java version`、`Java home` 和 `Apache Maven` 版本。如果项目使用 IntelliJ IDEA，还需要确认 Project SDK、Maven Runner JRE 与命令行 JDK 保持一致，避免出现本地启动正常但 Maven 编译失败的问题。

Spring Boot 3 的迁移还要求依赖和代码改用 Jakarta EE 包名。例如 Servlet API 应使用 `jakarta.servlet.*`，而不是旧的 `javax.servlet.*`；如果从老项目升级，需要统一检查 Controller、Filter、Interceptor、Validation、JPA 等代码中的 import。([GitHub](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-3.0-Migration-Guide?utm_source=chatgpt.com))

### Maven 依赖配置

这里给出一个适合后续 Spring Security 教程继续扩展的 `pom.xml` 基础配置。依赖分为基础 Web、安全测试、工具类和可选扩展几类；数据库认证、JWT 认证等章节可以在此基础上继续补充。

文件位置：`pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">

    <modelVersion>4.0.0</modelVersion>

    <!-- 使用 Spring Boot Parent 统一管理 Spring、Jackson、Tomcat、Security 等依赖版本 -->
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.5.14</version>
        <relativePath/>
    </parent>

    <groupId>io.github.atengk</groupId>
    <artifactId>spring-security-demo</artifactId>
    <version>1.0.0</version>
    <name>spring-security-demo</name>
    <description>Spring Boot 3 Spring Security 示例项目</description>

    <properties>
        <!-- Spring Boot 3 要求 Java 17 起步 -->
        <java.version>17</java.version>

        <!-- Hutool 用于字符串、集合、日期、JSON 等常用工具处理 -->
        <hutool.version>5.8.40</hutool.version>

        <!-- 后续 JWT 章节如使用 JJWT，可在此统一维护版本 -->
        <jjwt.version>0.12.6</jjwt.version>
    </properties>

    <dependencies>
        <!-- Web 开发基础依赖，包含 Spring MVC、内嵌 Tomcat、Jackson -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Spring Security 核心依赖，提供认证、授权、FilterChain、安全防护等能力 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>

        <!-- 参数校验依赖，Spring Boot 3 使用 jakarta.validation 包 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <!-- Hutool 工具包，便于处理字符串、集合、日期、JSON、加密摘要等通用逻辑 -->
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
            <version>${hutool.version}</version>
        </dependency>

        <!-- Lombok 简化实体类、DTO、日志对象等样板代码 -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Spring Boot 测试基础依赖，包含 JUnit Jupiter、Mockito、AssertJ 等 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>

        <!-- Spring Security 测试支持，提供 @WithMockUser、SecurityMockMvcRequestPostProcessors 等 -->
        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-test</artifactId>
            <scope>test</scope>
        </dependency>

        <!-- 后续 JWT 认证章节可启用：JWT API -->
        <!--
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>${jjwt.version}</version>
        </dependency>
        -->

        <!-- 后续 JWT 认证章节可启用：JWT 运行时实现 -->
        <!--
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <version>${jjwt.version}</version>
            <scope>runtime</scope>
        </dependency>
        -->

        <!-- 后续 JWT 认证章节可启用：JWT Jackson 序列化支持 -->
        <!--
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
            <version>${jjwt.version}</version>
            <scope>runtime</scope>
        </dependency>
        -->
    </dependencies>

    <build>
        <plugins>
            <!-- Spring Boot Maven 插件，用于打包和运行 Spring Boot 应用 -->
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <!-- Lombok 只参与编译，不打入最终运行包 -->
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

添加 `spring-boot-starter-security` 后，项目启动即会进入 Spring Security 默认保护模式。默认情况下，访问接口需要登录，用户名为 `user`，随机密码会打印在启动日志中；这只是开发调试行为，生产环境必须替换为自定义认证逻辑。([Home](https://docs.spring.io/spring-boot/3.5/reference/web/spring-security.html))

可以使用以下命令编译并启动项目。

```bash
# 清理并编译项目
mvn clean package

# 以 Spring Boot Maven 插件方式启动项目
mvn spring-boot:run
```

如果启动后看到类似以下日志，说明 Spring Security 已经生效：

```text
Using generated security password: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
```

后续一旦定义自己的 `SecurityFilterChain`、`UserDetailsService`、`PasswordEncoder` 等 Bean，就会逐步替换默认安全行为。

### 项目基础结构

建议采用清晰的分层结构，将安全配置、认证逻辑、用户模型、统一响应和异常处理拆分到独立包中。这样后续扩展数据库用户认证、JWT 过滤器、权限注解和异常响应时，不会把安全代码堆叠在 Controller 中。

推荐结构如下：

```text
spring-security-demo
├── pom.xml
└── src
    ├── main
    │   ├── java
    │   │   └── io
    │   │       └── github
    │   │           └── atengk
    │   │               └── security
    │   │                   ├── SpringSecurityDemoApplication.java
    │   │                   ├── common
    │   │                   │   ├── result
    │   │                   │   │   ├── Result.java
    │   │                   │   │   └── ResultCode.java
    │   │                   │   └── exception
    │   │                   │       └── GlobalExceptionHandler.java
    │   │                   ├── config
    │   │                   │   └── SecurityConfig.java
    │   │                   ├── security
    │   │                   │   ├── handler
    │   │                   │   │   ├── CustomAccessDeniedHandler.java
    │   │                   │   │   └── CustomAuthenticationEntryPoint.java
    │   │                   │   ├── jwt
    │   │                   │   │   ├── JwtAuthenticationFilter.java
    │   │                   │   │   └── JwtTokenProvider.java
    │   │                   │   └── service
    │   │                   │       └── CustomUserDetailsService.java
    │   │                   ├── module
    │   │                   │   ├── auth
    │   │                   │   │   ├── controller
    │   │                   │   │   │   └── AuthController.java
    │   │                   │   │   ├── dto
    │   │                   │   │   │   └── LoginRequest.java
    │   │                   │   │   └── vo
    │   │                   │   │       └── LoginResponse.java
    │   │                   │   └── user
    │   │                   │       ├── controller
    │   │                   │       │   └── UserController.java
    │   │                   │       ├── entity
    │   │                   │       │   └── SysUser.java
    │   │                   │       └── service
    │   │                   │           └── SysUserService.java
    │   │                   └── test
    │   │                       └── controller
    │   │                           └── TestController.java
    │   └── resources
    │       ├── application.yml
    │       └── logback-spring.xml
    └── test
        └── java
            └── io
                └── github
                    └── atengk
                        └── security
                            └── SpringSecurityDemoApplicationTests.java
```

各目录职责如下：

| 路径                        | 职责                                                         |
| --------------------------- | ------------------------------------------------------------ |
| `config`                    | 放置 Spring Security、跨域、Web MVC 等配置                   |
| `security.handler`          | 处理未登录、无权限、认证失败、登录成功等安全响应             |
| `security.jwt`              | 放置 JWT 生成、解析、认证过滤器等代码                        |
| `security.service`          | 对接 Spring Security 的用户加载逻辑，例如 `UserDetailsService` |
| `module.auth`               | 登录、登出、刷新 Token 等认证接口                            |
| `module.user`               | 用户信息、角色权限、用户资料等业务接口                       |
| `common.result`             | 统一响应结构                                                 |
| `common.exception`          | 全局异常处理                                                 |
| `resources/application.yml` | 应用端口、日志、安全参数、数据库参数等配置                   |

基础配置文件可以先保持简洁，后续章节再逐步补充数据库、JWT、跨域和日志配置。

文件位置：`src/main/resources/application.yml`

```yaml
server:
  # 应用服务端口
  port: 8080

spring:
  application:
    # 应用名称
    name: spring-security-demo

logging:
  level:
    # 开发阶段可开启 DEBUG 观察 Spring Security 过滤器和授权过程
    org.springframework.security: DEBUG
```

开发初期建议先保持最小工程可启动，再逐步添加安全配置。验证顺序建议如下：

```text
第一步：只添加 spring-boot-starter-security，确认默认登录保护生效
第二步：添加 SecurityFilterChain，放行登录接口和静态资源
第三步：添加 UserDetailsService 和 PasswordEncoder，实现自定义用户认证
第四步：添加 JWT 过滤器，切换为无状态认证
第五步：添加 @EnableMethodSecurity 和权限注解，实现方法级授权
```

这种顺序可以降低排错成本。Spring Security 的过滤器链较长，如果一开始同时加入数据库认证、JWT、跨域、异常处理和权限注解，出现 401 或 403 时不容易定位问题。



## 基础安全配置

本节用于配置 Spring Security 的基础安全链路，包括过滤器链、请求路径授权、登录登出和 CSRF 策略。Spring Boot 项目中只要引入 `spring-boot-starter-security`，Web 应用默认会被保护；自定义 `SecurityFilterChain` Bean 后，可以覆盖默认 Web 安全规则。([Home](https://docs.spring.io/spring-boot/3.5-SNAPSHOT/reference/web/spring-security.html?utm_source=chatgpt.com))

### SecurityFilterChain 配置

Spring Boot 3 / Spring Security 6 推荐通过 `SecurityFilterChain` Bean 配置安全规则，不再使用旧版常见的 `WebSecurityConfigurerAdapter` 继承方式。`SecurityFilterChain` 的核心作用是定义当前应用的请求授权、登录方式、登出方式、异常处理、CSRF、Session 策略等内容。Spring Security 官方示例也使用 `SecurityFilterChain` 配合 `authorizeHttpRequests`、`formLogin`、`httpBasic` 进行配置。([Home](https://docs.spring.io/spring-security/reference/servlet/authentication/passwords/index.html?utm_source=chatgpt.com))

本示例先采用 Session + Form Login 的方式，适合传统后台页面或初期验证 Spring Security 基础链路。后续如果切换为 JWT 无状态认证，需要关闭 `formLogin`、`httpBasic`，并将 Session 策略改为无状态。

文件位置：`src/main/java/io/github/atengk/security/config/SecurityConfig.java`

```java
package io.github.atengk.security.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security 基础安全配置
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /**
     * 配置 Spring Security 过滤器链
     *
     * @param http HTTP 安全配置对象
     * @return SecurityFilterChain
     * @throws Exception 配置异常
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        log.info("初始化 Spring Security 基础安全配置");

        http
                // 请求路径权限配置
                .authorizeHttpRequests(authorize -> authorize
                        // 登录页、错误页、公开接口直接放行
                        .requestMatchers("/login", "/error", "/public/**").permitAll()

                        // 静态资源直接放行
                        .requestMatchers(HttpMethod.GET,
                                "/favicon.ico",
                                "/static/**",
                                "/css/**",
                                "/js/**",
                                "/images/**"
                        ).permitAll()

                        // Knife4j / Swagger 文档资源，开发环境可放行，生产环境建议关闭或加权限
                        .requestMatchers(
                                "/doc.html",
                                "/webjars/**",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()

                        // 管理端接口要求 ADMIN 角色
                        .requestMatchers("/admin/**").hasRole("ADMIN")

                        // 用户端接口允许 USER 或 ADMIN 角色访问
                        .requestMatchers("/user/**").hasAnyRole("USER", "ADMIN")

                        // 其他请求必须登录
                        .anyRequest().authenticated()
                )

                // 表单登录配置
                .formLogin(form -> form
                        // 自定义登录页面地址
                        .loginPage("/login")
                        // Spring Security 处理登录提交的地址，默认也是 /login
                        .loginProcessingUrl("/login")
                        // 登录成功后跳转地址
                        .defaultSuccessUrl("/index", true)
                        // 登录失败后跳转地址
                        .failureUrl("/login?error")
                        .permitAll()
                )

                // 登出配置
                .logout(logout -> logout
                        // 登出请求地址，默认也是 /logout
                        .logoutUrl("/logout")
                        // 登出成功后跳转地址
                        .logoutSuccessUrl("/login?logout")
                        // 清理认证信息
                        .clearAuthentication(true)
                        // 让 Session 失效
                        .invalidateHttpSession(true)
                        // 删除浏览器中的 JSESSIONID
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )

                // 开启 HTTP Basic，便于 Postman 或 curl 调试
                .httpBasic(Customizer.withDefaults())

                // 默认开启 CSRF，适合 Session + 表单登录场景
                .csrf(Customizer.withDefaults());

        return http.build();
    }

}
```

这段配置包含几个关键点：

| 配置                    | 作用                                     |
| ----------------------- | ---------------------------------------- |
| `@EnableWebSecurity`    | 启用 Web 安全配置                        |
| `@EnableMethodSecurity` | 启用方法级权限控制，例如 `@PreAuthorize` |
| `authorizeHttpRequests` | 配置 URL 级访问规则                      |
| `formLogin`             | 配置表单登录                             |
| `logout`                | 配置登出处理                             |
| `csrf`                  | 配置 CSRF 防护策略                       |
| `httpBasic`             | 开启 Basic Auth，便于接口调试            |

启动项目后，可以先访问任意受保护接口，例如 `/user/info`。未登录时会被重定向到 `/login`，说明安全过滤器链已经生效。

### 请求路径权限配置

请求路径权限配置用于声明哪些接口可以匿名访问，哪些接口必须登录，哪些接口必须具备指定角色或权限。Spring Security 6 中推荐使用 `authorizeHttpRequests`，并按“公开资源、业务资源、兜底规则”的顺序配置。

常见权限规则如下：

```java
.authorizeHttpRequests(authorize -> authorize
        // 公开接口
        .requestMatchers("/login", "/error", "/public/**").permitAll()

        // 静态资源
        .requestMatchers(HttpMethod.GET, "/css/**", "/js/**", "/images/**").permitAll()

        // 管理员接口
        .requestMatchers("/admin/**").hasRole("ADMIN")

        // 普通用户接口
        .requestMatchers("/user/**").hasAnyRole("USER", "ADMIN")

        // 其他接口必须登录
        .anyRequest().authenticated()
)
```

路径权限配置建议遵循以下规则：

| 规则                 | 说明                                          |
| -------------------- | --------------------------------------------- |
| 放行规则放前面       | 例如登录页、静态资源、验证码、公开接口        |
| 业务规则放中间       | 例如 `/admin/**`、`/user/**`、`/api/order/**` |
| 兜底规则放最后       | 一般使用 `.anyRequest().authenticated()`      |
| 生产环境谨慎放行文档 | Swagger、Knife4j 建议只在开发环境放行         |
| 角色判断注意前缀     | `hasRole("ADMIN")` 实际匹配的是 `ROLE_ADMIN`  |

示例 Controller 可用于验证路径规则。

文件位置：`src/main/java/io/github/atengk/security/module/test/controller/TestController.java`

```java
package io.github.atengk.security.module.test.controller;

import cn.hutool.core.map.MapUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 权限测试接口
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@RestController
public class TestController {

    /**
     * 公开接口
     *
     * @return 响应数据
     */
    @GetMapping("/public/hello")
    public Map<String, Object> publicHello() {
        log.info("访问公开接口");
        return MapUtil.builder("message", (Object) "公开接口访问成功").build();
    }

    /**
     * 登录后可访问接口
     *
     * @return 响应数据
     */
    @GetMapping("/user/info")
    public Map<String, Object> userInfo() {
        log.info("访问用户接口");
        return MapUtil.builder("message", (Object) "用户接口访问成功").build();
    }

    /**
     * 管理员接口
     *
     * @return 响应数据
     */
    @GetMapping("/admin/info")
    public Map<String, Object> adminInfo() {
        log.info("访问管理员接口");
        return MapUtil.builder("message", (Object) "管理员接口访问成功").build();
    }

    /**
     * 方法级权限接口
     *
     * @return 响应数据
     */
    @PreAuthorize("hasAuthority('system:user:query')")
    @GetMapping("/user/permission")
    public Map<String, Object> permissionInfo() {
        log.info("访问方法级权限接口");
        return MapUtil.builder("message", (Object) "方法级权限访问成功").build();
    }

}
```

验证命令如下：

```bash
# 公开接口，不需要登录
curl http://localhost:8080/public/hello

# 受保护接口，未登录时会被拦截
curl -i http://localhost:8080/user/info

# 使用 HTTP Basic 调试内存用户
curl -u admin:123456 http://localhost:8080/admin/info
```

`@PreAuthorize` 属于方法级权限控制，需要配合 `@EnableMethodSecurity` 使用。Spring Security 文档说明，方法级安全可以通过 `@EnableMethodSecurity` 启用，并支持 `@PreAuthorize` 等注解；Spring Boot Starter Security 不会自动启用方法级授权。([Home](https://docs.spring.io/spring-boot/3.5-SNAPSHOT/reference/web/spring-security.html?utm_source=chatgpt.com))

### 登录与登出配置

登录配置用于指定用户如何进入系统，登出配置用于清理登录状态。Spring Security 默认提供 `/logout` 端点；当执行 `POST /logout` 时，默认会让 Session 失效、清理 `SecurityContext`、清理 Remember-Me 信息、清理 CSRF Token，并在成功后重定向到 `/login?logout`。([Home](https://docs.spring.io/spring-security/reference/servlet/authentication/logout.html?utm_source=chatgpt.com))

传统页面项目可使用以下配置：

```java
.formLogin(form -> form
        // 登录页面
        .loginPage("/login")
        // 登录提交地址
        .loginProcessingUrl("/login")
        // 登录成功跳转
        .defaultSuccessUrl("/index", true)
        // 登录失败跳转
        .failureUrl("/login?error")
        .permitAll()
)
.logout(logout -> logout
        // 登出地址
        .logoutUrl("/logout")
        // 登出成功跳转
        .logoutSuccessUrl("/login?logout")
        // 清理认证状态
        .clearAuthentication(true)
        // Session 失效
        .invalidateHttpSession(true)
        // 删除 Cookie
        .deleteCookies("JSESSIONID")
        .permitAll()
)
```

如果是前后端分离项目，通常不需要页面跳转，而是返回 JSON。此处先给出基础配置思路，具体 JSON 登录成功、失败、未登录、无权限处理可以放到后续“异常处理”和“前后端分离适配”章节展开。

```java
.formLogin(form -> form
        .loginProcessingUrl("/auth/login")
        .successHandler((request, response, authentication) -> {
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":200,\"message\":\"登录成功\"}");
        })
        .failureHandler((request, response, exception) -> {
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(401);
            response.getWriter().write("{\"code\":401,\"message\":\"用户名或密码错误\"}");
        })
)
.logout(logout -> logout
        .logoutUrl("/auth/logout")
        .logoutSuccessHandler((request, response, authentication) -> {
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":200,\"message\":\"退出成功\"}");
        })
)
```

需要注意，启用 CSRF 时，Spring Security 默认要求登出使用 `POST /logout`，并携带合法 CSRF Token；如果直接使用 GET 链接触发登出，并不推荐。官方文档明确指出，启用 CSRF 时 `LogoutFilter` 默认只处理 POST，以避免恶意站点强制用户退出登录。([Spring 企业文档](https://docs.enterprise.spring.io/spring-security/reference/6.0/servlet/exploits/csrf.html?utm_source=chatgpt.com))

### CSRF 配置策略

CSRF 是跨站请求伪造防护，主要用于防止用户在已登录状态下被第三方站点诱导提交非预期请求。Spring Security 在 Servlet 环境中默认启用 CSRF，并针对 POST 等不安全 HTTP 方法进行保护。([Home](https://docs.spring.io/spring-security/reference/6.5-SNAPSHOT/servlet/exploits/csrf.html?utm_source=chatgpt.com))

不同项目形态下，CSRF 策略不同：

| 项目类型                      | 推荐策略         | 说明                                                |
| ----------------------------- | ---------------- | --------------------------------------------------- |
| 服务端页面 + Session          | 保持开启         | 表单提交、登录、登出都应携带 CSRF Token             |
| 前后端分离 + Session Cookie   | 通常保持开启     | 可使用 `CookieCsrfTokenRepository` 给前端读取 Token |
| 前后端分离 + JWT Bearer Token | 通常关闭         | Token 放在 `Authorization` 请求头中，后端无 Session |
| 纯开放 API                    | 通常关闭         | 需使用 Token、签名、网关鉴权等其他机制              |
| 内部管理后台                  | 根据认证方式决定 | 如果仍使用 Cookie Session，建议开启                 |

Session + 表单登录场景可以显式保持默认配置：

```java
.csrf(Customizer.withDefaults())
```

前后端分离但仍使用 Cookie Session 时，可以将 CSRF Token 写入 Cookie，供前端读取后放入请求头。Spring Security 文档说明，SPA 场景可使用 `CookieCsrfTokenRepository.withHttpOnlyFalse()`，使 JavaScript 能读取 Cookie 中的 CSRF Token。([Home](https://docs.spring.io/spring-security/reference/6.5-SNAPSHOT/servlet/exploits/csrf.html?utm_source=chatgpt.com))

```java
.csrf(csrf -> csrf
        // 前端可读取 XSRF-TOKEN Cookie，并通过 X-XSRF-TOKEN 请求头提交
        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
)
```

如果是后续 JWT 无状态认证场景，可以关闭 CSRF：

```java
.csrf(csrf -> csrf.disable())
```

关闭 CSRF 前需要确认接口不依赖 Cookie Session 作为主要认证凭证。如果浏览器会自动携带登录态 Cookie，直接关闭 CSRF 可能扩大攻击面；如果认证完全依赖 `Authorization: Bearer xxx` 请求头，则通常不需要 CSRF 防护。

## 用户认证实现

本节用于实现用户认证，也就是验证用户名和密码是否正确，并把认证后的用户信息放入 Spring Security 上下文。Spring Security 的用户名密码认证通常通过 `UserDetailsService` 加载用户信息，再由 `PasswordEncoder` 完成密码匹配。官方文档说明，`UserDetailsService` 会被 `DaoAuthenticationProvider` 用于获取用户名、密码和其他认证属性。([Home](https://docs.spring.io/spring-security/reference/servlet/authentication/passwords/user-details-service.html?utm_source=chatgpt.com))

### 内存用户认证

内存用户认证适合快速验证 Spring Security 配置是否正确，不依赖数据库，启动后即可使用。它不适合生产环境，因为用户信息写死在代码或配置中，无法进行账号管理、密码重置、权限动态调整等操作。

为了避免和数据库认证冲突，建议使用 Spring Profile 区分内存认证和数据库认证。例如开发初期使用 `memory`，后续切换到 `db`。

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  profiles:
    # 开发初期使用 memory，切换数据库认证时改为 db
    active: memory
```

内存用户配置如下。

文件位置：`src/main/java/io/github/atengk/security/config/InMemoryUserConfig.java`

```java
package io.github.atengk.security.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 内存用户认证配置
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Configuration
@Profile("memory")
public class InMemoryUserConfig {

    /**
     * 配置内存用户
     *
     * @param passwordEncoder 密码加密器
     * @return UserDetailsService
     */
    @Bean
    public UserDetailsService inMemoryUserDetailsService(PasswordEncoder passwordEncoder) {
        log.info("启用内存用户认证模式");

        UserDetails admin = User.builder()
                .username("admin")
                .password(passwordEncoder.encode("123456"))
                .roles("ADMIN", "USER")
                .authorities("system:user:query", "system:user:add", "system:user:update", "system:user:delete")
                .build();

        UserDetails user = User.builder()
                .username("user")
                .password(passwordEncoder.encode("123456"))
                .roles("USER")
                .authorities("system:user:query")
                .build();

        return new InMemoryUserDetailsManager(admin, user);
    }

}
```

需要注意，`roles("ADMIN")` 会自动添加 `ROLE_` 前缀，最终权限为 `ROLE_ADMIN`；而 `authorities("system:user:query")` 不会自动添加前缀，适合用于按钮级、接口级、方法级权限。

验证方式：

```bash
# admin 具备 ROLE_ADMIN，可以访问管理接口
curl -u admin:123456 http://localhost:8080/admin/info

# user 只有 ROLE_USER，访问管理接口会返回 403
curl -i -u user:123456 http://localhost:8080/admin/info

# user 具备 system:user:query 权限，可以访问方法级权限接口
curl -u user:123456 http://localhost:8080/user/permission
```

官方示例中也提供了 `InMemoryUserDetailsManager` 用法，但 `User.withDefaultPasswordEncoder()` 只适合示例程序，不应在生产环境中使用；生产环境应显式配置安全的 `PasswordEncoder`。([Home](https://docs.spring.io/spring-security/reference/servlet/authentication/passwords/index.html?utm_source=chatgpt.com))

### 数据库用户认证

数据库用户认证适合真实业务系统。用户、角色、权限存储在数据库中，登录时根据用户名查询用户记录，再将数据库中的密码密文交给 Spring Security 校验。

本示例采用 MyBatis-Plus + MySQL。`mybatis-plus-spring-boot3-starter` 当前 Maven Central 最新版本为 `3.5.16`，适配 Spring Boot 3 项目；MySQL 驱动版本由 Spring Boot Parent 管理即可。([Maven Central](https://central.sonatype.com/artifact/com.baomidou/mybatis-plus-spring-boot3-starter?utm_source=chatgpt.com))

需要在 `pom.xml` 中补充以下依赖。

```xml
<properties>
    <!-- MyBatis-Plus Spring Boot 3 Starter 版本 -->
    <mybatis-plus.version>3.5.16</mybatis-plus.version>
</properties>

<dependencies>
    <!-- MyBatis-Plus Spring Boot 3 启动器，用于数据库用户认证 -->
    <dependency>
        <groupId>com.baomidou</groupId>
        <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
        <version>${mybatis-plus.version}</version>
    </dependency>

    <!-- MySQL JDBC 驱动，版本由 Spring Boot Parent 管理 -->
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
        <scope>runtime</scope>
    </dependency>
</dependencies>
```

数据库连接配置如下。

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  profiles:
    # 切换为数据库认证模式
    active: db

  datasource:
    # MySQL 连接地址
    url: jdbc:mysql://localhost:3306/security_demo?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false
    # 数据库用户名
    username: root
    # 数据库密码
    password: root
    # MySQL 8+ 驱动类
    driver-class-name: com.mysql.cj.jdbc.Driver

mybatis-plus:
  configuration:
    # 开发阶段打印 SQL，生产环境建议关闭
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  global-config:
    db-config:
      # 主键自增策略
      id-type: auto
```

示例用户表如下。

```sql
-- 用户表：保存登录账号、密码密文和账号状态
CREATE TABLE sys_user (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(64) NOT NULL COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码密文',
    nickname VARCHAR(64) DEFAULT NULL COMMENT '用户昵称',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用，0禁用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_user_username (username)
) COMMENT='系统用户表';

-- 注意：这里的密码需要替换为 PasswordEncoder 生成的密文，不建议直接使用明文
INSERT INTO sys_user (username, password, nickname, status)
VALUES ('admin', '{bcrypt}$2a$10$Qv6G3Ykz3uq1FRQl83tQ7uvF89mXxbZ1oH8dyZ9kBLTQ2vF5XoTj2', '管理员', 1);
```

上面的密码字段使用 `{bcrypt}` 前缀，是为了兼容 `DelegatingPasswordEncoder` 的密码格式。Spring Security 的 `DelegatingPasswordEncoder` 密码存储格式为 `{id}encodedPassword`，其中 `id` 用于选择具体的密码匹配器；例如 `{bcrypt}` 表示使用 `BCryptPasswordEncoder` 匹配。([Home](https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html?utm_source=chatgpt.com))

数据库实体类如下。

文件位置：`src/main/java/io/github/atengk/security/module/user/entity/SysUser.java`

```java
package io.github.atengk.security.module.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统用户实体
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Data
@TableName("sys_user")
public class SysUser {

    /**
     * 用户ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码密文
     */
    private String password;

    /**
     * 用户昵称
     */
    private String nickname;

    /**
     * 状态：1启用，0禁用
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

}
```

Mapper 接口如下。

文件位置：`src/main/java/io/github/atengk/security/module/user/mapper/SysUserMapper.java`

```java
package io.github.atengk.security.module.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.atengk.security.module.user.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统用户 Mapper
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
}
```

启动类需要扫描 Mapper。

文件位置：`src/main/java/io/github/atengk/security/SpringSecurityDemoApplication.java`

```java
package io.github.atengk.security;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Security 示例应用
 *
 * @author Ateng
 * @since 2026-05-05
 */
@MapperScan("io.github.atengk.security.module.**.mapper")
@SpringBootApplication
public class SpringSecurityDemoApplication {

    /**
     * 应用启动入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(SpringSecurityDemoApplication.class, args);
    }

}
```

### UserDetailsService 实现

`UserDetailsService` 是 Spring Security 对接用户数据源的核心接口。数据库认证时，开发者需要实现 `loadUserByUsername` 方法，根据用户名查询用户，并返回 Spring Security 能识别的 `UserDetails` 对象。官方文档说明，自定义认证可以通过暴露自定义 `UserDetailsService` Bean 实现。([Home](https://docs.spring.io/spring-security/reference/servlet/authentication/passwords/user-details-service.html?utm_source=chatgpt.com))

为了让后续权限控制更清晰，建议不要直接返回 Spring Security 内置的 `User`，而是封装一个自己的登录用户对象。

文件位置：`src/main/java/io/github/atengk/security/security/model/SecurityUser.java`

```java
package io.github.atengk.security.security.model;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.io.Serial;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Spring Security 登录用户
 *
 * @author Ateng
 * @since 2026-05-05
 */
@RequiredArgsConstructor
public class SecurityUser implements UserDetails {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    private final Long userId;

    /**
     * 用户名
     */
    private final String username;

    /**
     * 密码密文
     */
    private final String password;

    /**
     * 用户状态：1启用，0禁用
     */
    private final Integer status;

    /**
     * 权限标识集合
     */
    private final List<String> permissions;

    /**
     * 获取用户ID
     *
     * @return 用户ID
     */
    public Long getUserId() {
        return userId;
    }

    /**
     * 获取权限集合
     *
     * @return 权限集合
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (CollUtil.isEmpty(permissions)) {
            return Collections.emptyList();
        }
        return permissions.stream()
                .filter(StrUtil::isNotBlank)
                .map(SimpleGrantedAuthority::new)
                .toList();
    }

    /**
     * 获取密码
     *
     * @return 密码密文
     */
    @Override
    public String getPassword() {
        return password;
    }

    /**
     * 获取用户名
     *
     * @return 用户名
     */
    @Override
    public String getUsername() {
        return username;
    }

    /**
     * 账号是否未过期
     *
     * @return true 未过期
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * 账号是否未锁定
     *
     * @return true 未锁定
     */
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    /**
     * 凭证是否未过期
     *
     * @return true 未过期
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * 账号是否启用
     *
     * @return true 启用
     */
    @Override
    public boolean isEnabled() {
        return Integer.valueOf(1).equals(status);
    }

}
```

数据库用户加载逻辑如下。这里为了聚焦认证流程，权限先写为固定集合；真实 RBAC 项目中应从用户、角色、菜单、权限表查询。

文件位置：`src/main/java/io/github/atengk/security/security/service/CustomUserDetailsService.java`

```java
package io.github.atengk.security.security.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.github.atengk.security.module.user.entity.SysUser;
import io.github.atengk.security.module.user.mapper.SysUserMapper;
import io.github.atengk.security.security.model.SecurityUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 数据库用户认证服务
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Service
@Profile("db")
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final SysUserMapper sysUserMapper;

    /**
     * 根据用户名加载用户信息
     *
     * @param username 用户名
     * @return Spring Security 用户详情
     */
    @Override
    public UserDetails loadUserByUsername(String username) {
        if (StrUtil.isBlank(username)) {
            log.warn("登录认证失败，用户名为空");
            throw new UsernameNotFoundException("用户名不能为空");
        }

        SysUser sysUser = sysUserMapper.selectOne(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getUsername, username)
                .last("limit 1"));

        if (sysUser == null) {
            log.warn("登录认证失败，用户不存在：{}", username);
            throw new UsernameNotFoundException("用户不存在");
        }

        if (!Integer.valueOf(1).equals(sysUser.getStatus())) {
            log.warn("登录认证失败，账号已被禁用：{}", username);
            throw new DisabledException("账号已被禁用");
        }

        log.info("加载登录用户成功：{}", username);

        // 示例权限：后续 RBAC 章节应从角色权限表查询
        List<String> permissions = List.of(
                "ROLE_USER",
                "system:user:query"
        );

        return new SecurityUser(
                sysUser.getId(),
                sysUser.getUsername(),
                sysUser.getPassword(),
                sysUser.getStatus(),
                permissions
        );
    }

}
```

数据库认证流程如下：

```text
用户提交用户名和密码
  -> Spring Security 根据用户名调用 UserDetailsService
  -> CustomUserDetailsService 查询 sys_user 表
  -> 返回 SecurityUser，包含用户名、密码密文、账号状态、权限集合
  -> DaoAuthenticationProvider 使用 PasswordEncoder 校验密码
  -> 校验成功后写入 SecurityContext
  -> 后续接口通过 URL 规则或 @PreAuthorize 判断权限
```

使用数据库认证时，只保留一个 `UserDetailsService` Bean。上面的内存认证和数据库认证通过 `@Profile("memory")`、`@Profile("db")` 隔离，避免同时存在多个认证数据源导致配置不清晰。

### 密码加密与 PasswordEncoder

`PasswordEncoder` 用于密码加密和密码匹配。Spring Security 官方文档说明，`PasswordEncoder` 是单向转换，通常用于将用户输入的密码与数据库中保存的密码密文进行比较，而不是用于可逆解密。([Home](https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html?utm_source=chatgpt.com))

推荐使用 `PasswordEncoderFactories.createDelegatingPasswordEncoder()`，它默认生成 `DelegatingPasswordEncoder`，可以通过 `{bcrypt}`、`{noop}`、`{pbkdf2}` 等前缀识别不同密码算法，便于旧密码迁移和未来算法升级。([Home](https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/security/config/PasswordConfig.java`

```java
package io.github.atengk.security.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 密码加密配置
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Configuration
public class PasswordConfig {

    /**
     * 配置密码加密器
     *
     * @return PasswordEncoder
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        log.info("初始化 PasswordEncoder 密码加密器");
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

}
```

为了生成数据库初始化密码，可以增加一个临时测试接口或单元测试。生产环境不要暴露密码生成接口。

文件位置：`src/test/java/io/github/atengk/security/PasswordEncoderTests.java`

```java
package io.github.atengk.security;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 密码加密测试
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
class PasswordEncoderTests {

    /**
     * 生成 BCrypt 密码密文
     */
    @Test
    void encodePassword() {
        PasswordEncoder passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
        String rawPassword = "123456";
        String encodedPassword = passwordEncoder.encode(rawPassword);

        log.info("原始密码：{}", rawPassword);
        log.info("密码密文：{}", encodedPassword);
    }

    /**
     * 验证密码是否匹配
     */
    @Test
    void matchesPassword() {
        PasswordEncoder passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
        String rawPassword = "123456";
        String encodedPassword = passwordEncoder.encode(rawPassword);

        boolean matches = passwordEncoder.matches(rawPassword, encodedPassword);
        log.info("密码匹配结果：{}", matches);
    }

}
```

运行测试后，将生成的密文写入 `sys_user.password` 字段，例如：

```sql
-- 将 admin 用户密码更新为 PasswordEncoder 生成的密文
UPDATE sys_user
SET password = '{bcrypt}$2a$10$xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx'
WHERE username = 'admin';
```

注意事项：

| 注意点                   | 说明                                                         |
| ------------------------ | ------------------------------------------------------------ |
| 不要保存明文密码         | 数据库只保存加密后的密码密文                                 |
| 不要使用 `{noop}`        | `{noop}` 表示明文匹配，只适合极短期本地验证                  |
| 推荐使用 `{bcrypt}`      | `DelegatingPasswordEncoder` 默认编码通常使用 bcrypt          |
| 密码字段长度要足够       | 建议 `VARCHAR(255)`                                          |
| 登录时不需要手动比对密码 | Spring Security 会调用 `PasswordEncoder.matches()`           |
| 密码密文要保留算法前缀   | 例如 `{bcrypt}xxxxx`，否则可能出现找不到 PasswordEncoder 的异常 |

如果出现以下异常：

```text
java.lang.IllegalArgumentException: There is no PasswordEncoder mapped for the id "null"
```

通常表示数据库中的密码没有 `{bcrypt}`、`{noop}` 等算法前缀。处理方式是重新用 `PasswordEncoder` 生成密码，或者在旧密码迁移阶段明确配置兼容策略。Spring Security 文档也说明，`DelegatingPasswordEncoder` 默认根据密码前缀中的 `{id}` 查找对应的密码匹配器。([Home](https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html?utm_source=chatgpt.com))



## 权限授权实现

本节用于实现 Spring Security 的授权能力。认证解决“用户是谁”，授权解决“用户能访问什么”。在实际后台系统中，通常使用 RBAC 模型维护用户、角色、权限之间的关系，然后把角色和权限转换为 Spring Security 能识别的 `GrantedAuthority`，最终供 URL 级权限和方法级权限统一使用。

Spring Security 支持在请求级别建模授权规则，例如 `/admin/**` 需要管理员权限，其他请求只需要登录；官方文档也说明，`authorizeHttpRequests` 中的规则会按声明顺序匹配，常用规则包括 `permitAll`、`hasAuthority`、`hasRole`、`hasAnyAuthority`、`hasAnyRole` 等。([Home](https://docs.spring.io/spring-security/reference/6.5/servlet/authorization/authorize-http-requests.html?utm_source=chatgpt.com))

### 角色与权限模型

角色与权限模型建议采用 RBAC 设计。用户不直接绑定每一个接口权限，而是通过角色间接获得权限；特殊场景下也可以扩展用户直接绑定权限，但基础版本先使用“用户-角色-权限”三层结构。

推荐模型如下：

```text
sys_user
  -> sys_user_role
  -> sys_role
  -> sys_role_permission
  -> sys_permission
```

角色和权限在 Spring Security 中最终都会转换为 `GrantedAuthority`。需要注意：

| 类型 | 数据库示例          | Spring Security 中的权限值 | 使用方式                                           |
| ---- | ------------------- | -------------------------- | -------------------------------------------------- |
| 角色 | `ADMIN`             | `ROLE_ADMIN`               | `hasRole("ADMIN")` 或 `hasAuthority("ROLE_ADMIN")` |
| 角色 | `USER`              | `ROLE_USER`                | `hasRole("USER")`                                  |
| 权限 | `system:user:query` | `system:user:query`        | `hasAuthority("system:user:query")`                |
| 权限 | `system:user:add`   | `system:user:add`          | `@PreAuthorize("hasAuthority('system:user:add')")` |

`hasRole("ADMIN")` 会自动匹配 `ROLE_ADMIN`，因此数据库中角色编码建议保存为 `ADMIN`、`USER`，加载到 Spring Security 时再统一添加 `ROLE_` 前缀。`hasAuthority("system:user:query")` 不会自动添加前缀，适合保存按钮级、接口级、菜单级权限标识。([Home](https://docs.spring.io/spring-security/reference/6.5/servlet/authorization/authorize-http-requests.html?utm_source=chatgpt.com))

下面是基础 RBAC 表结构。

```sql
-- 角色表：保存系统角色，例如 ADMIN、USER
CREATE TABLE sys_role (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '角色ID',
    role_code VARCHAR(64) NOT NULL COMMENT '角色编码，例如 ADMIN、USER',
    role_name VARCHAR(64) NOT NULL COMMENT '角色名称',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用，0禁用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_role_code (role_code)
) COMMENT='系统角色表';

-- 权限表：保存接口、菜单、按钮等权限标识
CREATE TABLE sys_permission (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '权限ID',
    permission_code VARCHAR(128) NOT NULL COMMENT '权限标识，例如 system:user:query',
    permission_name VARCHAR(64) NOT NULL COMMENT '权限名称',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用，0禁用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_permission_code (permission_code)
) COMMENT='系统权限表';

-- 用户角色关联表：一个用户可以拥有多个角色
CREATE TABLE sys_user_role (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_user_role (user_id, role_id)
) COMMENT='用户角色关联表';

-- 角色权限关联表：一个角色可以拥有多个权限
CREATE TABLE sys_role_permission (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    permission_id BIGINT NOT NULL COMMENT '权限ID',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_role_permission (role_id, permission_id)
) COMMENT='角色权限关联表';

-- 初始化角色
INSERT INTO sys_role (id, role_code, role_name, status)
VALUES
    (1, 'ADMIN', '管理员', 1),
    (2, 'USER', '普通用户', 1);

-- 初始化权限
INSERT INTO sys_permission (id, permission_code, permission_name, status)
VALUES
    (1, 'system:user:query', '用户查询', 1),
    (2, 'system:user:add', '用户新增', 1),
    (3, 'system:user:update', '用户修改', 1),
    (4, 'system:user:delete', '用户删除', 1);

-- admin 用户绑定管理员角色，假设 admin 用户ID为 1
INSERT INTO sys_user_role (user_id, role_id)
VALUES (1, 1);

-- ADMIN 角色绑定全部权限
INSERT INTO sys_role_permission (role_id, permission_id)
VALUES
    (1, 1),
    (1, 2),
    (1, 3),
    (1, 4);

-- USER 角色只绑定查询权限
INSERT INTO sys_role_permission (role_id, permission_id)
VALUES (2, 1);
```

为了让 `UserDetailsService` 加载用户时同时查询角色和权限，可以增加一个认证授权 Mapper。

文件位置：`src/main/java/io/github/atengk/security/security/mapper/SysAuthMapper.java`

```java
package io.github.atengk.security.security.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 认证授权 Mapper
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Mapper
public interface SysAuthMapper {

    /**
     * 查询用户角色编码
     *
     * @param userId 用户ID
     * @return 角色编码集合
     */
    @Select("""
            SELECT DISTINCT r.role_code
            FROM sys_user_role ur
            INNER JOIN sys_role r ON ur.role_id = r.id
            WHERE ur.user_id = #{userId}
              AND r.status = 1
            """)
    List<String> selectRoleCodesByUserId(Long userId);

    /**
     * 查询用户权限标识
     *
     * @param userId 用户ID
     * @return 权限标识集合
     */
    @Select("""
            SELECT DISTINCT p.permission_code
            FROM sys_user_role ur
            INNER JOIN sys_role r ON ur.role_id = r.id
            INNER JOIN sys_role_permission rp ON r.id = rp.role_id
            INNER JOIN sys_permission p ON rp.permission_id = p.id
            WHERE ur.user_id = #{userId}
              AND r.status = 1
              AND p.status = 1
            """)
    List<String> selectPermissionCodesByUserId(Long userId);

}
```

然后修改数据库用户认证服务，把角色和权限都加载到 `SecurityUser` 中。

文件位置：`src/main/java/io/github/atengk/security/security/service/CustomUserDetailsService.java`

```java
package io.github.atengk.security.security.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.github.atengk.security.module.user.entity.SysUser;
import io.github.atengk.security.module.user.mapper.SysUserMapper;
import io.github.atengk.security.security.mapper.SysAuthMapper;
import io.github.atengk.security.security.model.SecurityUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 数据库用户认证服务
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Service
@Profile("db")
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final SysUserMapper sysUserMapper;
    private final SysAuthMapper sysAuthMapper;

    /**
     * 根据用户名加载用户信息
     *
     * @param username 用户名
     * @return Spring Security 用户详情
     */
    @Override
    public UserDetails loadUserByUsername(String username) {
        if (StrUtil.isBlank(username)) {
            log.warn("登录认证失败，用户名为空");
            throw new UsernameNotFoundException("用户名不能为空");
        }

        SysUser sysUser = sysUserMapper.selectOne(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getUsername, username)
                .last("limit 1"));

        if (sysUser == null) {
            log.warn("登录认证失败，用户不存在：{}", username);
            throw new UsernameNotFoundException("用户不存在");
        }

        if (!Integer.valueOf(1).equals(sysUser.getStatus())) {
            log.warn("登录认证失败，账号已被禁用：{}", username);
            throw new DisabledException("账号已被禁用");
        }

        List<String> roleAuthorities = sysAuthMapper.selectRoleCodesByUserId(sysUser.getId())
                .stream()
                .filter(StrUtil::isNotBlank)
                .map(roleCode -> StrUtil.addPrefixIfNot(roleCode, "ROLE_"))
                .toList();

        List<String> permissionAuthorities = sysAuthMapper.selectPermissionCodesByUserId(sysUser.getId())
                .stream()
                .filter(StrUtil::isNotBlank)
                .toList();

        List<String> authorities = CollUtil.newArrayList();
        authorities.addAll(roleAuthorities);
        authorities.addAll(permissionAuthorities);

        log.info("加载登录用户成功：{}，权限数量：{}", username, authorities.size());

        return new SecurityUser(
                sysUser.getId(),
                sysUser.getUsername(),
                sysUser.getPassword(),
                sysUser.getStatus(),
                authorities
        );
    }

}
```

这样处理后，登录用户的权限集合中会同时包含：

```text
ROLE_ADMIN
system:user:query
system:user:add
system:user:update
system:user:delete
```

后续 URL 级权限、方法级权限和 JWT 认证过滤器都可以复用这套权限集合。

### URL 级权限控制

URL 级权限控制适合处理粗粒度接口访问，例如公开接口、登录接口、用户接口、管理员接口、文档接口等。它的优势是集中、直观，适合全局路径规则；缺点是不适合表达复杂业务条件，例如“只能修改自己创建的数据”。

推荐配置方式如下。

文件位置：`src/main/java/io/github/atengk/security/config/SecurityConfig.java`

```java
package io.github.atengk.security.config;

import io.github.atengk.security.security.handler.CustomAccessDeniedHandler;
import io.github.atengk.security.security.handler.CustomAuthenticationEntryPoint;
import io.github.atengk.security.security.jwt.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 安全配置
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;

    /**
     * 配置安全过滤器链
     *
     * @param http HTTP 安全配置对象
     * @return SecurityFilterChain
     * @throws Exception 配置异常
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        log.info("初始化 JWT 模式 Spring Security 配置");

        http
                // JWT 前后端分离模式通常关闭 CSRF
                .csrf(AbstractHttpConfigurer::disable)

                // JWT 模式不使用服务端 Session
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // 关闭默认表单登录
                .formLogin(AbstractHttpConfigurer::disable)

                // 关闭 HTTP Basic
                .httpBasic(AbstractHttpConfigurer::disable)

                // 请求路径授权配置
                .authorizeHttpRequests(authorize -> authorize
                        // 登录、验证码、公开接口放行
                        .requestMatchers("/auth/login", "/auth/captcha", "/public/**", "/error").permitAll()

                        // 静态资源放行
                        .requestMatchers(HttpMethod.GET,
                                "/favicon.ico",
                                "/static/**",
                                "/css/**",
                                "/js/**",
                                "/images/**"
                        ).permitAll()

                        // 接口文档资源，生产环境建议移除或增加权限
                        .requestMatchers(
                                "/doc.html",
                                "/webjars/**",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()

                        // 管理员接口
                        .requestMatchers("/admin/**").hasRole("ADMIN")

                        // 用户接口
                        .requestMatchers("/user/**").hasAnyRole("USER", "ADMIN")

                        // 其他请求必须登录
                        .anyRequest().authenticated()
                )

                // 统一处理未登录和无权限异常
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(customAuthenticationEntryPoint)
                        .accessDeniedHandler(customAccessDeniedHandler)
                )

                // JWT 认证过滤器放在用户名密码过滤器之前
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * 暴露认证管理器，用于登录接口执行用户名密码认证
     *
     * @param authenticationConfiguration 认证配置
     * @return AuthenticationManager
     * @throws Exception 获取异常
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

}
```

URL 级权限控制建议遵循以下规则：

| 建议                              | 说明                                     |
| --------------------------------- | ---------------------------------------- |
| 公开接口显式放行                  | 登录、验证码、健康检查、公开资源         |
| 管理端接口使用角色控制            | 例如 `/admin/**` 使用 `hasRole("ADMIN")` |
| 业务接口再叠加方法权限            | URL 控制大类，方法注解控制具体动作       |
| 兜底使用 `authenticated()`        | 防止新增接口忘记配置后被匿名访问         |
| 高安全系统可使用 `denyAll()` 兜底 | 明确列出的接口才允许访问                 |

如果项目希望采用“白名单之外全部拒绝”，可以将兜底规则改为：

```java
.anyRequest().denyAll()
```

这种方式更安全，但每新增一个接口都必须同步维护安全规则，否则接口会默认不可访问。

### 方法级权限控制

方法级权限控制适合细粒度授权，通常写在 Controller 或 Service 方法上。它可以根据角色、权限标识、方法参数、当前登录用户、返回值等进行判断。Spring Security 支持通过 `@EnableMethodSecurity` 启用方法级授权，并可使用 `@PreAuthorize`、`@PostAuthorize`、`@PreFilter`、`@PostFilter` 等注解；Spring Boot Starter Security 默认不会自动开启方法级授权。([Home](https://docs.spring.io/spring-security/reference/6.5-SNAPSHOT/servlet/authorization/method-security.html?utm_source=chatgpt.com))

常见写法如下：

```java
@PreAuthorize("hasRole('ADMIN')")
@PreAuthorize("hasAuthority('system:user:query')")
@PreAuthorize("hasAnyAuthority('system:user:add', 'system:user:update')")
@PreAuthorize("#userId == authentication.principal.userId or hasRole('ADMIN')")
```

下面给出一个用户管理接口示例。

文件位置：`src/main/java/io/github/atengk/security/module/user/controller/UserController.java`

```java
package io.github.atengk.security.module.user.controller;

import cn.hutool.core.map.MapUtil;
import io.github.atengk.security.security.model.SecurityUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 用户管理接口
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {

    /**
     * 查询当前登录用户信息
     *
     * @param securityUser 当前登录用户
     * @return 用户信息
     */
    @GetMapping("/info")
    @PreAuthorize("hasAuthority('system:user:query')")
    public Map<String, Object> getUserInfo(@AuthenticationPrincipal SecurityUser securityUser) {
        log.info("查询当前登录用户信息：{}", securityUser.getUsername());
        return MapUtil.builder("userId", (Object) securityUser.getUserId())
                .put("username", securityUser.getUsername())
                .put("message", "查询成功")
                .build();
    }

    /**
     * 新增用户
     *
     * @param username 用户名
     * @return 操作结果
     */
    @PostMapping
    @PreAuthorize("hasAuthority('system:user:add')")
    public Map<String, Object> addUser(@RequestParam String username) {
        log.info("新增用户：{}", username);
        return MapUtil.builder("message", (Object) "新增用户成功")
                .put("username", username)
                .build();
    }

    /**
     * 修改用户
     *
     * @param userId 用户ID
     * @param nickname 用户昵称
     * @return 操作结果
     */
    @PutMapping("/{userId}")
    @PreAuthorize("hasAuthority('system:user:update')")
    public Map<String, Object> updateUser(@PathVariable Long userId, @RequestParam String nickname) {
        log.info("修改用户信息，用户ID：{}，昵称：{}", userId, nickname);
        return MapUtil.builder("message", (Object) "修改用户成功")
                .put("userId", userId)
                .put("nickname", nickname)
                .build();
    }

    /**
     * 删除用户
     *
     * @param userId 用户ID
     * @return 操作结果
     */
    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('system:user:delete')")
    public Map<String, Object> deleteUser(@PathVariable Long userId) {
        log.info("删除用户，用户ID：{}", userId);
        return MapUtil.builder("message", (Object) "删除用户成功")
                .put("userId", userId)
                .build();
    }

    /**
     * 查询本人资料或管理员查询任意用户资料
     *
     * @param userId 用户ID
     * @param securityUser 当前登录用户
     * @return 用户资料
     */
    @GetMapping("/{userId}/profile")
    @PreAuthorize("#userId == authentication.principal.userId or hasRole('ADMIN')")
    public Map<String, Object> getUserProfile(@PathVariable Long userId,
                                              @AuthenticationPrincipal SecurityUser securityUser) {
        log.info("查询用户资料，请求用户ID：{}，当前登录用户：{}", userId, securityUser.getUsername());
        return MapUtil.builder("message", (Object) "查询用户资料成功")
                .put("userId", userId)
                .build();
    }

}
```

方法级权限更推荐写在 Service 层，因为 Controller 层只是入口，业务方法才是真正的权限边界。对于简单教程，可以先写在 Controller；对于正式项目，建议在 Service 层做最终兜底。

验证命令如下：

```bash
# 查询当前用户信息，需要 system:user:query 权限
curl -H "Authorization: Bearer <access_token>" \
  http://localhost:8080/user/info

# 新增用户，需要 system:user:add 权限
curl -X POST \
  -H "Authorization: Bearer <access_token>" \
  "http://localhost:8080/user?username=test01"

# 删除用户，需要 ROLE_ADMIN 和 system:user:delete 权限
curl -X DELETE \
  -H "Authorization: Bearer <access_token>" \
  http://localhost:8080/user/100
```

### 权限不足处理

权限不足一般分为两类：未登录和无权限。未登录表示当前请求没有有效认证信息，通常返回 401；无权限表示已经登录，但权限不足，通常返回 403。Spring Security 中 `AccessDeniedHandler` 用于处理 `AccessDeniedException`，通常由 `ExceptionTranslationFilter` 调用。([Home](https://docs.spring.io/spring-security/reference/6.5-SNAPSHOT/api/java/org/springframework/security/web/access/AccessDeniedHandler.html?utm_source=chatgpt.com))

先定义统一响应结构。

文件位置：`src/main/java/io/github/atengk/security/common/result/Result.java`

```java
package io.github.atengk.security.common.result;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

/**
 * 统一响应结果
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Data
@Accessors(chain = true)
public class Result<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

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
     * @param <T> 数据类型
     * @return 响应结果
     */
    public static <T> Result<T> success(T data) {
        return new Result<T>()
                .setCode(200)
                .setMessage("操作成功")
                .setData(data);
    }

    /**
     * 失败响应
     *
     * @param code 响应编码
     * @param message 响应消息
     * @param <T> 数据类型
     * @return 响应结果
     */
    public static <T> Result<T> fail(Integer code, String message) {
        return new Result<T>()
                .setCode(code)
                .setMessage(message);
    }

}
```

未登录处理器如下。

文件位置：`src/main/java/io/github/atengk/security/security/handler/CustomAuthenticationEntryPoint.java`

```java
package io.github.atengk.security.security.handler;

import cn.hutool.json.JSONUtil;
import io.github.atengk.security.common.result.Result;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 未登录异常处理器
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    /**
     * 处理未登录异常
     *
     * @param request 请求对象
     * @param response 响应对象
     * @param authException 认证异常
     * @throws IOException IO异常
     * @throws ServletException Servlet异常
     */
    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {
        log.warn("请求未登录，路径：{}，异常：{}", request.getRequestURI(), authException.getMessage());
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(JSONUtil.toJsonStr(Result.fail(401, "请先登录")));
    }

}
```

无权限处理器如下。

文件位置：`src/main/java/io/github/atengk/security/security/handler/CustomAccessDeniedHandler.java`

```java
package io.github.atengk.security.security.handler;

import cn.hutool.json.JSONUtil;
import io.github.atengk.security.common.result.Result;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 权限不足异常处理器
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    /**
     * 处理权限不足异常
     *
     * @param request 请求对象
     * @param response 响应对象
     * @param accessDeniedException 权限异常
     * @throws IOException IO异常
     * @throws ServletException Servlet异常
     */
    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException, ServletException {
        log.warn("访问被拒绝，路径：{}，异常：{}", request.getRequestURI(), accessDeniedException.getMessage());
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(JSONUtil.toJsonStr(Result.fail(403, "权限不足")));
    }

}
```

在 `SecurityConfig` 中启用：

```java
.exceptionHandling(exception -> exception
        .authenticationEntryPoint(customAuthenticationEntryPoint)
        .accessDeniedHandler(customAccessDeniedHandler)
)
```

验证方式：

```bash
# 未携带 Token，预期返回 401
curl -i http://localhost:8080/user/info

# 携带普通用户 Token 访问管理员接口，预期返回 403
curl -i -H "Authorization: Bearer <user_access_token>" \
  http://localhost:8080/admin/info
```

## JWT 认证方案

本节用于实现前后端分离项目中常见的 JWT 认证方案。用户登录成功后，后端生成 Token；前端后续请求通过 `Authorization: Bearer <token>` 携带 Token；后端过滤器解析 Token 并写入 `SecurityContext`，让 Spring Security 后续授权逻辑继续生效。

Spring Security 官方的 OAuth2 Resource Server 支持使用 JWT 和 Opaque Token 保护接口；如果采用标准授权服务器签发 Token，可以使用 `spring-security-oauth2-resource-server` 和 `spring-security-oauth2-jose`。本文示例采用本地系统自签发 JWT，便于和自定义登录接口、用户表、权限表直接集成。([Home](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html?utm_source=chatgpt.com))

### JWT 登录流程

JWT 登录流程如下：

```text
1. 前端提交用户名和密码到 /auth/login
2. 后端使用 AuthenticationManager 执行用户名密码认证
3. Spring Security 调用 UserDetailsService 查询用户、角色、权限
4. PasswordEncoder 校验密码
5. 认证成功后生成 JWT
6. 前端保存 JWT
7. 后续请求携带 Authorization: Bearer <token>
8. JwtAuthenticationFilter 解析 Token
9. 解析成功后构造 Authentication 并写入 SecurityContext
10. URL 级权限和方法级权限继续生效
```

JWT 模式下，登录接口不应直接查询数据库并手动比对密码，而是交给 `AuthenticationManager` 处理。这样可以复用 Spring Security 的认证机制、密码加密器、账号状态判断和异常处理。

先补充 JJWT 依赖。Maven Central 当前列出的 `jjwt-api` 最新版本包括 `0.13.0`；这里使用 `0.13.0` 作为示例版本。([Maven Repository](https://repo.maven.apache.org/maven2/io/jsonwebtoken/jjwt-api/?utm_source=chatgpt.com))

```xml
<properties>
    <!-- JJWT 版本，用于生成和解析 JWT -->
    <jjwt.version>0.13.0</jjwt.version>
</properties>

<dependencies>
    <!-- JWT API -->
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-api</artifactId>
        <version>${jjwt.version}</version>
    </dependency>

    <!-- JWT 运行时实现 -->
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-impl</artifactId>
        <version>${jjwt.version}</version>
        <scope>runtime</scope>
    </dependency>

    <!-- JWT Jackson 序列化支持 -->
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-jackson</artifactId>
        <version>${jjwt.version}</version>
        <scope>runtime</scope>
    </dependency>
</dependencies>
```

JWT 配置项如下。

文件位置：`src/main/resources/application.yml`

```yaml
security:
  jwt:
    # JWT 签名密钥，生产环境建议使用环境变量或配置中心，不要硬编码在代码仓库
    secret: ${JWT_SECRET:ateng-security-jwt-secret-key-2026-must-be-at-least-32-bytes}
    # Access Token 有效期，单位秒
    access-token-expire-seconds: 7200
    # Token 签发者
    issuer: spring-security-demo
```

配置属性类如下。

文件位置：`src/main/java/io/github/atengk/security/security/jwt/JwtProperties.java`

```java
package io.github.atengk.security.security.jwt;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 配置属性
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Data
@Component
@ConfigurationProperties(prefix = "security.jwt")
public class JwtProperties {

    /**
     * JWT 签名密钥
     */
    private String secret;

    /**
     * Access Token 有效期，单位秒
     */
    private Long accessTokenExpireSeconds;

    /**
     * Token 签发者
     */
    private String issuer;

}
```

登录请求对象如下。

文件位置：`src/main/java/io/github/atengk/security/module/auth/dto/LoginRequest.java`

```java
package io.github.atengk.security.module.auth.dto;

import jakarta.validation.constraints.NotBlank;
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
```

登录响应对象如下。

文件位置：`src/main/java/io/github/atengk/security/module/auth/vo/LoginResponse.java`

```java
package io.github.atengk.security.module.auth.vo;

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
     * 访问令牌
     */
    private String accessToken;

    /**
     * Token 类型
     */
    private String tokenType;

    /**
     * 过期时间，单位秒
     */
    private Long expiresIn;

}
```

登录接口如下。

文件位置：`src/main/java/io/github/atengk/security/module/auth/controller/AuthController.java`

```java
package io.github.atengk.security.module.auth.controller;

import cn.hutool.core.collection.CollUtil;
import io.github.atengk.security.common.result.Result;
import io.github.atengk.security.module.auth.dto.LoginRequest;
import io.github.atengk.security.module.auth.vo.LoginResponse;
import io.github.atengk.security.security.jwt.JwtProperties;
import io.github.atengk.security.security.jwt.JwtTokenProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 认证接口
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;

    /**
     * 用户登录
     *
     * @param request 登录请求参数
     * @return Token 信息
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("用户登录请求：{}", request.getUsername());

        UsernamePasswordAuthenticationToken authenticationToken =
                UsernamePasswordAuthenticationToken.unauthenticated(request.getUsername(), request.getPassword());

        Authentication authentication = authenticationManager.authenticate(authenticationToken);

        List<String> authorities = authentication.getAuthorities()
                .stream()
                .map(Object::toString)
                .toList();

        String accessToken = jwtTokenProvider.generateToken(authentication.getName(), CollUtil.newArrayList(authorities));

        LoginResponse response = LoginResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresIn(jwtProperties.getAccessTokenExpireSeconds())
                .build();

        log.info("用户登录成功：{}，权限数量：{}", authentication.getName(), authorities.size());
        return Result.success(response);
    }

}
```

请求示例：

```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}'
```

响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9.xxx.xxx",
    "tokenType": "Bearer",
    "expiresIn": 7200
  }
}
```

### Token 生成与解析

Token 生成与解析建议单独封装，避免在 Controller、Filter、Service 中散落 JWT 细节。基础 Token 建议包含以下信息：

| Claim         | 说明                 |
| ------------- | -------------------- |
| `sub`         | 用户名或用户唯一标识 |
| `authorities` | 当前用户权限集合     |
| `iss`         | 签发者               |
| `iat`         | 签发时间             |
| `exp`         | 过期时间             |

Token 工具类如下。

文件位置：`src/main/java/io/github/atengk/security/security/jwt/JwtTokenProvider.java`

```java
package io.github.atengk.security.security.jwt;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;

/**
 * JWT Token 生成与解析工具
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private static final String AUTHORITIES_KEY = "authorities";

    private final JwtProperties jwtProperties;

    /**
     * 生成访问令牌
     *
     * @param username 用户名
     * @param authorities 权限集合
     * @return JWT Token
     */
    public String generateToken(String username, List<String> authorities) {
        Date issuedAt = DateUtil.date();
        Date expiration = DateUtil.offsetSecond(issuedAt, jwtProperties.getAccessTokenExpireSeconds().intValue());

        String token = Jwts.builder()
                .issuer(jwtProperties.getIssuer())
                .subject(username)
                .claim(AUTHORITIES_KEY, CollUtil.emptyIfNull(authorities))
                .issuedAt(issuedAt)
                .expiration(expiration)
                .signWith(getSecretKey())
                .compact();

        log.info("生成 JWT 成功，用户：{}，过期时间：{}", username, DateUtil.formatDateTime(expiration));
        return token;
    }

    /**
     * 解析 Token 中的用户名
     *
     * @param token JWT Token
     * @return 用户名
     */
    public String getUsername(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * 解析 Token 中的权限集合
     *
     * @param token JWT Token
     * @return 权限集合
     */
    @SuppressWarnings("unchecked")
    public List<String> getAuthorities(String token) {
        Object authorities = parseClaims(token).get(AUTHORITIES_KEY);
        if (authorities instanceof List<?> list) {
            return list.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .toList();
        }
        return List.of();
    }

    /**
     * 校验 Token 是否有效
     *
     * @param token JWT Token
     * @return true 有效
     */
    public boolean validateToken(String token) {
        if (StrUtil.isBlank(token)) {
            return false;
        }
        parseClaims(token);
        return true;
    }

    /**
     * 解析 Claims
     *
     * @param token JWT Token
     * @return Claims
     */
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSecretKey())
                .requireIssuer(jwtProperties.getIssuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 获取签名密钥
     *
     * @return SecretKey
     */
    private SecretKey getSecretKey() {
        byte[] keyBytes = StrUtil.bytes(jwtProperties.getSecret(), CharsetUtil.CHARSET_UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

}
```

注意事项：

| 注意点       | 说明                                                         |
| ------------ | ------------------------------------------------------------ |
| 密钥长度     | HS256 至少需要 256 bit，即 32 字节以上                       |
| 密钥存储     | 生产环境使用环境变量、配置中心或密钥管理系统                 |
| Token 有效期 | Access Token 建议较短，Refresh Token 可另行设计              |
| Token 撤销   | JWT 默认无状态，注销、踢人、改密后失效需要 Redis 黑名单或 Token 版本号 |
| 权限更新     | 如果权限放进 JWT，权限变更后旧 Token 不会自动更新            |

### JWT 认证过滤器

JWT 认证过滤器用于从请求头中提取 Token，解析用户名和权限，然后构造 `Authentication` 写入 `SecurityContext`。这样后续 `hasRole`、`hasAuthority`、`@PreAuthorize` 都可以正常工作。

Spring Security 官方 API 中的 Bearer Token 过滤器也是从请求中提取 Bearer Token 并尝试认证；本文为了贴合自定义登录接口和自签发 JWT，使用自定义 `OncePerRequestFilter` 实现。([Home](https://docs.spring.io/spring-security/site/docs/6.0.0-M6/api/org/springframework/security/oauth2/server/resource/web/BearerTokenAuthenticationFilter.html?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/security/security/jwt/JwtAuthenticationFilter.java`

```java
package io.github.atengk.security.security.jwt;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import io.github.atengk.security.common.result.Result;
import io.github.atengk.security.security.model.SecurityUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * JWT 认证过滤器
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String TOKEN_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;

    /**
     * 执行 JWT 认证过滤
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
        String token = resolveToken(request);

        if (StrUtil.isBlank(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            jwtTokenProvider.validateToken(token);
            String username = jwtTokenProvider.getUsername(token);

            if (StrUtil.isNotBlank(username) && SecurityContextHolder.getContext().getAuthentication() == null) {
                SecurityUser securityUser = (SecurityUser) userDetailsService.loadUserByUsername(username);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                securityUser,
                                null,
                                securityUser.getAuthorities()
                        );

                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.info("JWT 认证成功，用户：{}，路径：{}", username, request.getRequestURI());
            }

            filterChain.doFilter(request, response);
        } catch (Exception exception) {
            log.warn("JWT 认证失败，路径：{}，异常：{}", request.getRequestURI(), exception.getMessage());
            SecurityContextHolder.clearContext();
            writeUnauthorizedResponse(response);
        }
    }

    /**
     * 从请求头解析 Token
     *
     * @param request 请求对象
     * @return JWT Token
     */
    private String resolveToken(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StrUtil.isBlank(authorization) || !StrUtil.startWithIgnoreCase(authorization, TOKEN_PREFIX)) {
            return null;
        }
        return StrUtil.subAfter(authorization, TOKEN_PREFIX, false);
    }

    /**
     * 写出未认证响应
     *
     * @param response 响应对象
     * @throws IOException IO异常
     */
    private void writeUnauthorizedResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(JSONUtil.toJsonStr(Result.fail(401, "Token 无效或已过期")));
    }

}
```

需要在 `SecurityConfig` 中把过滤器加入 Spring Security 过滤器链：

```java
.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
```

验证方式：

```bash
# 1. 登录获取 Token
TOKEN=$(curl -s -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}' \
  | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p')

# 2. 携带 Token 访问受保护接口
curl -H "Authorization: Bearer ${TOKEN}" \
  http://localhost:8080/user/info

# 3. 携带 Token 访问管理员接口
curl -H "Authorization: Bearer ${TOKEN}" \
  http://localhost:8080/admin/info
```

如果返回 401，重点检查：

```text
1. Authorization 请求头是否为 Bearer <token>
2. Token 是否过期
3. JWT_SECRET 是否发生变化
4. issuer 是否与配置一致
5. 数据库中的用户是否仍然启用
```

如果返回 403，说明认证已经成功，但权限不足，需要检查用户角色、权限表数据，以及 `SecurityUser#getAuthorities()` 中是否包含目标权限。

### 无状态会话配置

JWT 模式下，服务端不保存登录状态，每次请求都通过 Token 重新识别用户。因此需要关闭服务端 Session 创建策略，并关闭默认表单登录、HTTP Basic 和 CSRF。Spring Security 文档说明，如果不希望创建 Session，可以使用 `SessionCreationPolicy.STATELESS`。([Home](https://docs.spring.io/spring-security/reference/6.5/servlet/authentication/session-management.html?utm_source=chatgpt.com))

无状态配置如下：

```java
http
        // JWT 不依赖 Cookie Session，通常关闭 CSRF
        .csrf(AbstractHttpConfigurer::disable)

        // 不创建、不使用 HttpSession 保存认证状态
        .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        )

        // 前后端分离项目不使用默认表单登录
        .formLogin(AbstractHttpConfigurer::disable)

        // 不使用 HTTP Basic
        .httpBasic(AbstractHttpConfigurer::disable);
```

JWT 无状态认证和 Session 认证的差异如下：

| 对比项               | Session 认证                  | JWT 认证                        |
| -------------------- | ----------------------------- | ------------------------------- |
| 登录状态保存位置     | 服务端 Session                | 客户端 Token                    |
| 服务端是否保存登录态 | 保存                          | 默认不保存                      |
| 横向扩展             | 需要共享 Session 或粘性会话   | 更容易扩展                      |
| 主动失效             | 服务端删除 Session 即可       | 需要黑名单、版本号或缩短有效期  |
| 前端传递方式         | Cookie 自动携带               | `Authorization: Bearer <token>` |
| CSRF 风险            | Cookie 自动携带时需要重点防护 | Header Token 通常不需要 CSRF    |
| 适用场景             | 服务端页面、传统后台          | 前后端分离、移动端、开放 API    |

JWT 模式的最终请求链路如下：

```text
客户端请求
  -> Authorization: Bearer <token>
  -> JwtAuthenticationFilter
  -> JwtTokenProvider 解析 Token
  -> UserDetailsService 加载用户最新状态
  -> SecurityContextHolder 写入 Authentication
  -> authorizeHttpRequests 执行 URL 权限判断
  -> @PreAuthorize 执行方法权限判断
  -> Controller / Service
```

推荐上线前检查以下事项：

| 检查项       | 建议                                         |
| ------------ | -------------------------------------------- |
| Token 密钥   | 不要硬编码到代码仓库，使用环境变量或配置中心 |
| Token 有效期 | Access Token 不宜过长                        |
| HTTPS        | 生产环境必须使用 HTTPS 传输 Token            |
| 注销机制     | 可使用 Redis 黑名单或用户 Token 版本号       |
| 改密失效     | 用户修改密码后，应让旧 Token 失效            |
| 权限变更     | 高安全场景不要长期依赖 Token 内旧权限        |
| 日志输出     | 不要在日志中打印完整 Token                   |
| 跨域配置     | 前后端分离时只允许可信域名访问               |



## 异常处理

本节用于统一处理 Spring Security 认证授权过程中的异常响应。前后端分离项目不适合使用默认重定向登录页，应统一返回 JSON，例如未登录返回 401、权限不足返回 403、认证失败返回 401、参数错误返回 400。Spring Security 中，`AuthenticationEntryPoint` 用于处理需要客户端提供认证凭证的场景，认证失败流程会调用 `AuthenticationFailureHandler`，认证成功流程会调用 `AuthenticationSuccessHandler`。([Home](https://www.springframework.org/spring-security/reference/6.5/servlet/authentication/architecture.html?utm_source=chatgpt.com))

### 未登录异常处理

未登录异常通常发生在访问受保护接口时没有携带 Token、Token 为空、Token 无效，或者 Spring Security 上下文中不存在有效认证信息。前后端分离项目建议返回 401 JSON，而不是跳转到登录页面。

前面已经定义过 `Result` 统一响应对象，这里先补充一个安全响应输出工具，避免每个 Handler 重复设置响应头和 JSON 序列化。

文件位置：`src/main/java/io/github/atengk/security/security/util/SecurityResponseUtil.java`

```java
package io.github.atengk.security.security.util;

import cn.hutool.json.JSONUtil;
import io.github.atengk.security.common.result.Result;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Spring Security 响应输出工具
 *
 * @author Ateng
 * @since 2026-05-05
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SecurityResponseUtil {

    /**
     * 写出 JSON 响应
     *
     * @param response 响应对象
     * @param status HTTP 状态码
     * @param result 统一响应结果
     * @throws IOException IO 异常
     */
    public static void writeJson(HttpServletResponse response, int status, Result<?> result) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(JSONUtil.toJsonStr(result));
    }

}
```

未登录处理器实现如下。

文件位置：`src/main/java/io/github/atengk/security/security/handler/CustomAuthenticationEntryPoint.java`

```java
package io.github.atengk.security.security.handler;

import io.github.atengk.security.common.result.Result;
import io.github.atengk.security.security.util.SecurityResponseUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 未登录异常处理器
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    /**
     * 处理未登录异常
     *
     * @param request 请求对象
     * @param response 响应对象
     * @param authException 认证异常
     * @throws IOException IO 异常
     * @throws ServletException Servlet 异常
     */
    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {
        log.warn("请求未登录，路径：{}，异常：{}", request.getRequestURI(), authException.getMessage());
        SecurityResponseUtil.writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, Result.fail(401, "请先登录"));
    }

}
```

该处理器需要注册到 `SecurityConfig` 中：

```java
.exceptionHandling(exception -> exception
        .authenticationEntryPoint(customAuthenticationEntryPoint)
        .accessDeniedHandler(customAccessDeniedHandler)
)
```

未登录响应示例：

```json
{
  "code": 401,
  "message": "请先登录",
  "data": null
}
```

### 无权限异常处理

无权限表示用户已经登录，但没有访问当前资源所需的角色或权限。例如普通用户访问 `/admin/**`，或者缺少 `system:user:delete` 权限时访问删除接口。Spring Security 的授权过滤流程会在授权拒绝时抛出 `AccessDeniedException`，然后交给 `ExceptionTranslationFilter` 处理；如果当前用户已认证，会委托给 `AccessDeniedHandler`。([Home](https://www.springframework.org/spring-security/reference/6.5/servlet/authorization/authorize-http-requests.html?utm_source=chatgpt.com))

无权限处理器实现如下。

文件位置：`src/main/java/io/github/atengk/security/security/handler/CustomAccessDeniedHandler.java`

```java
package io.github.atengk.security.security.handler;

import io.github.atengk.security.common.result.Result;
import io.github.atengk.security.security.util.SecurityResponseUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 权限不足异常处理器
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    /**
     * 处理权限不足异常
     *
     * @param request 请求对象
     * @param response 响应对象
     * @param accessDeniedException 权限异常
     * @throws IOException IO 异常
     * @throws ServletException Servlet 异常
     */
    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException, ServletException {
        log.warn("权限不足，路径：{}，异常：{}", request.getRequestURI(), accessDeniedException.getMessage());
        SecurityResponseUtil.writeJson(response, HttpServletResponse.SC_FORBIDDEN, Result.fail(403, "权限不足"));
    }

}
```

无权限响应示例：

```json
{
  "code": 403,
  "message": "权限不足",
  "data": null
}
```

401 和 403 的区别建议固定下来，避免前端判断混乱：

| 状态码 | 含义           | 常见原因                                   | 前端处理                  |
| ------ | -------------- | ------------------------------------------ | ------------------------- |
| 401    | 未认证         | 未登录、Token 缺失、Token 无效、Token 过期 | 清理登录态并跳转登录页    |
| 403    | 已认证但无权限 | 角色不足、权限标识不足、方法级权限校验失败 | 提示无权限或跳转 403 页面 |

### 认证失败处理

认证失败主要发生在登录阶段，例如用户名不存在、密码错误、账号禁用、账号锁定。JWT 登录接口如果使用 `AuthenticationManager#authenticate`，认证失败异常会从 Controller 抛出，因此可以通过全局异常处理器统一转换为 JSON。

文件位置：`src/main/java/io/github/atengk/security/common/exception/GlobalExceptionHandler.java`

```java
package io.github.atengk.security.common.exception;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.security.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AccountStatusException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

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
     * 处理用户名或密码错误
     *
     * @param exception 认证异常
     * @return 统一响应
     */
    @ExceptionHandler(BadCredentialsException.class)
    public Result<Void> handleBadCredentialsException(BadCredentialsException exception) {
        log.warn("登录认证失败，用户名或密码错误：{}", exception.getMessage());
        return Result.fail(401, "用户名或密码错误");
    }

    /**
     * 处理账号状态异常
     *
     * @param exception 账号状态异常
     * @return 统一响应
     */
    @ExceptionHandler(AccountStatusException.class)
    public Result<Void> handleAccountStatusException(AccountStatusException exception) {
        log.warn("登录认证失败，账号状态异常：{}", exception.getMessage());
        return Result.fail(401, exception.getMessage());
    }

    /**
     * 处理其他认证异常
     *
     * @param exception 认证异常
     * @return 统一响应
     */
    @ExceptionHandler(AuthenticationException.class)
    public Result<Void> handleAuthenticationException(AuthenticationException exception) {
        log.warn("登录认证失败：{}", exception.getMessage());
        return Result.fail(401, "认证失败");
    }

    /**
     * 处理参数校验异常
     *
     * @param exception 参数校验异常
     * @return 统一响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException exception) {
        List<FieldError> fieldErrors = exception.getBindingResult().getFieldErrors();
        String message = "请求参数错误";

        if (CollUtil.isNotEmpty(fieldErrors)) {
            FieldError fieldError = fieldErrors.get(0);
            message = StrUtil.blankToDefault(fieldError.getDefaultMessage(), message);
        }

        log.warn("请求参数校验失败：{}", message);
        return Result.fail(400, message);
    }

    /**
     * 处理未知系统异常
     *
     * @param exception 系统异常
     * @return 统一响应
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception exception) {
        log.error("系统异常", exception);
        return Result.fail(500, "系统异常，请联系管理员");
    }

}
```

如果使用 Spring Security 的 `formLogin` JSON 登录方式，而不是自定义 `/auth/login` Controller，则认证失败会进入 `AuthenticationFailureHandler`。认证过滤器在认证失败时会清理 `SecurityContextHolder`，然后调用 `AuthenticationFailureHandler`；认证成功时会设置认证信息并调用 `AuthenticationSuccessHandler`。([Home](https://www.springframework.org/spring-security/reference/6.5/servlet/authentication/architecture.html?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/security/security/handler/JsonAuthenticationFailureHandler.java`

```java
package io.github.atengk.security.security.handler;

import io.github.atengk.security.common.result.Result;
import io.github.atengk.security.security.util.SecurityResponseUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AccountStatusException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * JSON 登录失败处理器
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Component
public class JsonAuthenticationFailureHandler implements AuthenticationFailureHandler {

    /**
     * 处理登录失败
     *
     * @param request 请求对象
     * @param response 响应对象
     * @param exception 认证异常
     * @throws IOException IO 异常
     * @throws ServletException Servlet 异常
     */
    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        String message = "认证失败";

        if (exception instanceof BadCredentialsException) {
            message = "用户名或密码错误";
        } else if (exception instanceof AccountStatusException) {
            message = exception.getMessage();
        }

        log.warn("登录失败，路径：{}，原因：{}", request.getRequestURI(), message);
        SecurityResponseUtil.writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, Result.fail(401, message));
    }

}
```

### 登录成功与失败响应

JWT 前后端分离模式下，登录成功响应通常由 `/auth/login` 接口返回，登录失败由 `GlobalExceptionHandler` 统一处理。这样可以避免混用 Session 表单登录和 JWT 登录。

登录成功响应建议包含 Token、Token 类型、过期时间和用户基础信息。注意不要返回密码、盐值、身份证号、手机号完整值等敏感信息。

文件位置：`src/main/java/io/github/atengk/security/module/auth/vo/LoginResponse.java`

```java
package io.github.atengk.security.module.auth.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

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
     * 访问令牌
     */
    private String accessToken;

    /**
     * Token 类型
     */
    private String tokenType;

    /**
     * 过期时间，单位秒
     */
    private Long expiresIn;

    /**
     * 用户名
     */
    private String username;

    /**
     * 权限集合
     */
    private List<String> authorities;

}
```

登录成功响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9.xxx.xxx",
    "tokenType": "Bearer",
    "expiresIn": 7200,
    "username": "admin",
    "authorities": [
      "ROLE_ADMIN",
      "system:user:query",
      "system:user:add",
      "system:user:update",
      "system:user:delete"
    ]
  }
}
```

登录失败响应示例：

```json
{
  "code": 401,
  "message": "用户名或密码错误",
  "data": null
}
```

如果项目采用 `formLogin`，可以在安全配置中显式配置成功和失败处理器。需要注意，这种方式适合表单登录过滤器，不适合已经关闭 `formLogin` 的纯 JWT 登录接口。

```java
.formLogin(form -> form
        .loginProcessingUrl("/auth/login")
        .successHandler(jsonAuthenticationSuccessHandler)
        .failureHandler(jsonAuthenticationFailureHandler)
)
```

JWT 模式下推荐保持如下配置：

```java
.formLogin(AbstractHttpConfigurer::disable)
.httpBasic(AbstractHttpConfigurer::disable)
```

然后使用自定义 Controller 完成登录认证和 Token 签发。

## 接口开发与联调

本节用于补全前后端分离项目中最常用的联调接口，包括登录接口、用户信息接口、受保护资源接口和 Postman 调试方式。接口统一使用 JSON 响应，认证信息统一通过 `Authorization: Bearer <token>` 请求头传递。

### 登录接口

登录接口负责接收用户名和密码，调用 Spring Security 的 `AuthenticationManager` 执行认证，认证成功后生成 JWT 并返回给前端。认证失败时不在 Controller 中硬编码错误响应，而是交给前面的全局异常处理器处理。

文件位置：`src/main/java/io/github/atengk/security/module/auth/controller/AuthController.java`

```java
package io.github.atengk.security.module.auth.controller;

import cn.hutool.core.collection.CollUtil;
import io.github.atengk.security.common.result.Result;
import io.github.atengk.security.module.auth.dto.LoginRequest;
import io.github.atengk.security.module.auth.vo.LoginResponse;
import io.github.atengk.security.security.jwt.JwtProperties;
import io.github.atengk.security.security.jwt.JwtTokenProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 认证接口
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;

    /**
     * 用户登录
     *
     * @param request 登录请求参数
     * @return 登录响应
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("用户登录请求：{}", request.getUsername());

        UsernamePasswordAuthenticationToken authenticationToken =
                UsernamePasswordAuthenticationToken.unauthenticated(request.getUsername(), request.getPassword());

        Authentication authentication = authenticationManager.authenticate(authenticationToken);

        List<String> authorities = authentication.getAuthorities()
                .stream()
                .map(Object::toString)
                .toList();

        String accessToken = jwtTokenProvider.generateToken(authentication.getName(), CollUtil.newArrayList(authorities));

        LoginResponse response = LoginResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresIn(jwtProperties.getAccessTokenExpireSeconds())
                .username(authentication.getName())
                .authorities(authorities)
                .build();

        log.info("用户登录成功：{}，权限数量：{}", authentication.getName(), authorities.size());
        return Result.success(response);
    }

}
```

接口说明：

| 项目           | 内容               |
| -------------- | ------------------ |
| 请求路径       | `/auth/login`      |
| 请求方法       | `POST`             |
| Content-Type   | `application/json` |
| 是否需要 Token | 否                 |
| 成功状态码     | 200                |
| 失败状态码     | 401                |

请求示例：

```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}'
```

响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9.xxx.xxx",
    "tokenType": "Bearer",
    "expiresIn": 7200,
    "username": "admin",
    "authorities": [
      "ROLE_ADMIN",
      "system:user:query"
    ]
  }
}
```

### 用户信息接口

用户信息接口用于返回当前登录用户的基础信息和权限集合。该接口通常在前端登录成功后调用，用于初始化用户状态、菜单权限、按钮权限和路由权限。

文件位置：`src/main/java/io/github/atengk/security/module/auth/vo/UserInfoResponse.java`

```java
package io.github.atengk.security.module.auth.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 当前登录用户信息响应
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Data
@Builder
public class UserInfoResponse {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 权限集合
     */
    private List<String> authorities;

}
```

用户信息接口实现如下。

文件位置：`src/main/java/io/github/atengk/security/module/auth/controller/ProfileController.java`

```java
package io.github.atengk.security.module.auth.controller;

import io.github.atengk.security.common.result.Result;
import io.github.atengk.security.module.auth.vo.UserInfoResponse;
import io.github.atengk.security.security.model.SecurityUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 当前登录用户接口
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@RestController
public class ProfileController {

    /**
     * 查询当前登录用户信息
     *
     * @param securityUser 当前登录用户
     * @return 当前登录用户信息
     */
    @GetMapping("/auth/userinfo")
    public Result<UserInfoResponse> userInfo(@AuthenticationPrincipal SecurityUser securityUser) {
        List<String> authorities = securityUser.getAuthorities()
                .stream()
                .map(Object::toString)
                .toList();

        UserInfoResponse response = UserInfoResponse.builder()
                .userId(securityUser.getUserId())
                .username(securityUser.getUsername())
                .authorities(authorities)
                .build();

        log.info("查询当前登录用户信息：{}", securityUser.getUsername());
        return Result.success(response);
    }

}
```

接口说明：

| 项目           | 内容                                   |
| -------------- | -------------------------------------- |
| 请求路径       | `/auth/userinfo`                       |
| 请求方法       | `GET`                                  |
| 是否需要 Token | 是                                     |
| 请求头         | `Authorization: Bearer <access_token>` |
| 主要用途       | 前端初始化用户信息、权限、菜单、按钮   |

请求示例：

```bash
curl http://localhost:8080/auth/userinfo \
  -H "Authorization: Bearer <access_token>"
```

响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "userId": 1,
    "username": "admin",
    "authorities": [
      "ROLE_ADMIN",
      "system:user:query",
      "system:user:add"
    ]
  }
}
```

### 受保护资源接口

受保护资源接口用于验证 URL 级权限、方法级权限和 JWT 过滤器是否正常工作。建议至少准备公开接口、登录后接口、管理员接口、权限标识接口四类资源，便于快速定位 401 和 403 问题。

文件位置：`src/main/java/io/github/atengk/security/module/test/controller/ProtectedResourceController.java`

```java
package io.github.atengk.security.module.test.controller;

import cn.hutool.core.map.MapUtil;
import io.github.atengk.security.security.model.SecurityUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 受保护资源测试接口
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@RestController
@RequestMapping("/resource")
public class ProtectedResourceController {

    /**
     * 查询公开资源
     *
     * @return 公开资源
     */
    @GetMapping("/public")
    public Map<String, Object> publicResource() {
        log.info("访问公开资源");
        return MapUtil.builder("message", (Object) "公开资源访问成功").build();
    }

    /**
     * 查询登录后资源
     *
     * @param securityUser 当前登录用户
     * @return 登录后资源
     */
    @GetMapping("/profile")
    public Map<String, Object> profileResource(@AuthenticationPrincipal SecurityUser securityUser) {
        log.info("访问登录后资源：{}", securityUser.getUsername());
        return MapUtil.builder("message", (Object) "登录后资源访问成功")
                .put("username", securityUser.getUsername())
                .build();
    }

    /**
     * 查询管理员资源
     *
     * @return 管理员资源
     */
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> adminResource() {
        log.info("访问管理员资源");
        return MapUtil.builder("message", (Object) "管理员资源访问成功").build();
    }

    /**
     * 查询用户权限资源
     *
     * @return 用户权限资源
     */
    @GetMapping("/user/query")
    @PreAuthorize("hasAuthority('system:user:query')")
    public Map<String, Object> userQueryResource() {
        log.info("访问用户查询权限资源");
        return MapUtil.builder("message", (Object) "用户查询权限资源访问成功").build();
    }

    /**
     * 删除用户权限资源
     *
     * @param userId 用户ID
     * @return 删除结果
     */
    @DeleteMapping("/user/{userId}")
    @PreAuthorize("hasAuthority('system:user:delete')")
    public Map<String, Object> userDeleteResource(@PathVariable Long userId) {
        log.info("访问用户删除权限资源，用户ID：{}", userId);
        return MapUtil.builder("message", (Object) "用户删除权限资源访问成功")
                .put("userId", userId)
                .build();
    }

}
```

如果 `/resource/public` 也需要匿名访问，需要在 `SecurityConfig` 中补充放行规则：

```java
.requestMatchers("/resource/public").permitAll()
```

联调命令如下：

```bash
# 公开资源，不需要 Token
curl http://localhost:8080/resource/public

# 登录后资源，需要 Token
curl http://localhost:8080/resource/profile \
  -H "Authorization: Bearer <access_token>"

# 管理员资源，需要 ROLE_ADMIN
curl http://localhost:8080/resource/admin \
  -H "Authorization: Bearer <access_token>"

# 用户查询资源，需要 system:user:query
curl http://localhost:8080/resource/user/query \
  -H "Authorization: Bearer <access_token>"

# 用户删除资源，需要 system:user:delete
curl -X DELETE http://localhost:8080/resource/user/1 \
  -H "Authorization: Bearer <access_token>"
```

### Postman 调试方式

Postman 调试建议按“登录获取 Token、保存 Token、访问受保护接口、验证异常响应”的顺序进行。这样可以区分是登录失败、Token 解析失败、还是权限不足。

第一步，新建登录请求。

| 配置项  | 内容                               |
| ------- | ---------------------------------- |
| Method  | `POST`                             |
| URL     | `http://localhost:8080/auth/login` |
| Headers | `Content-Type: application/json`   |
| Body    | `raw` / `JSON`                     |

请求体：

```json
{
  "username": "admin",
  "password": "123456"
}
```

第二步，在登录请求的 Tests 中保存 Token 到环境变量。

```javascript
const response = pm.response.json();

if (response.code === 200 && response.data && response.data.accessToken) {
    pm.environment.set("access_token", response.data.accessToken);
    console.log("Token 已保存到环境变量 access_token");
} else {
    console.log("登录失败：", response);
}
```

第三步，新建受保护接口请求，在 Authorization 中配置：

| 配置项 | 内容               |
| ------ | ------------------ |
| Type   | `Bearer Token`     |
| Token  | `{{access_token}}` |

或者直接在 Headers 中添加：

```text
Authorization: Bearer {{access_token}}
```

第四步，验证典型场景。

| 场景         | 请求                                | 预期结果                          |
| ------------ | ----------------------------------- | --------------------------------- |
| 正确登录     | `/auth/login` 用户名密码正确        | 返回 200 和 `accessToken`         |
| 错误密码     | `/auth/login` 密码错误              | 返回 401，提示用户名或密码错误    |
| 未携带 Token | `/auth/userinfo` 不传 Authorization | 返回 401，提示请先登录            |
| Token 错误   | `/auth/userinfo` 传入伪造 Token     | 返回 401，提示 Token 无效或已过期 |
| 权限不足     | 普通用户访问管理员接口              | 返回 403，提示权限不足            |
| 权限正常     | 管理员访问管理员接口                | 返回 200                          |

常见问题排查：

| 问题                    | 排查方向                                                     |
| ----------------------- | ------------------------------------------------------------ |
| 登录接口返回 403        | 检查 `/auth/login` 是否在 `SecurityConfig` 中 `permitAll()`  |
| 所有接口都返回 401      | 检查 `Authorization` 是否为 `Bearer <token>` 格式            |
| Token 正确但仍返回 401  | 检查 JWT 密钥、过期时间、签发者、过滤器是否注册              |
| 登录成功但接口返回 403  | 检查数据库角色权限、`SecurityUser#getAuthorities()`、`@PreAuthorize` 表达式 |
| Postman 里 Token 未生效 | 检查环境变量名称是否为 `access_token`，当前请求是否选择了正确环境 |
| 浏览器跨域失败          | 检查 CORS 配置，跨域错误不等同于 Spring Security 鉴权失败    |

最终联调链路可以按下面顺序确认：

```text
1. /auth/login 能正常返回 accessToken
2. Authorization: Bearer <accessToken> 能进入 JwtAuthenticationFilter
3. JwtAuthenticationFilter 能写入 SecurityContext
4. /auth/userinfo 能返回当前登录用户
5. URL 级权限能区分 /admin/** 和普通接口
6. @PreAuthorize 能识别 ROLE_ADMIN 和 system:user:query
7. 401、403、登录失败都返回统一 JSON
```



## 前后端分离适配

本节用于处理 Vue、React、移动端等前端应用调用 Spring Boot 后端接口时的适配问题，重点包括跨域、JSON 响应、Token 传递和前端路由权限。前后端分离项目通常不使用服务端页面跳转，也不依赖服务端 Session 保存登录态，而是通过 JWT 或其他 Token 机制完成无状态认证。

### 跨域配置

跨域配置用于解决浏览器同源策略限制。例如前端运行在 `http://localhost:5173`，后端运行在 `http://localhost:8080`，浏览器会把它识别为跨域请求。Spring Security 官方文档说明，CORS 必须在 Spring Security 之前处理，因为预检请求通常不携带 Cookie；如果安全过滤器先处理，可能会把预检请求当作未认证请求拒绝。官方也说明，提供 `UrlBasedCorsConfigurationSource` 后，Spring Security 可以集成 CORS 配置。([Home](https://docs.spring.io/spring-security/reference/servlet/integrations/cors.html?utm_source=chatgpt.com))

推荐在 Spring Security 配置中统一启用 CORS，而不是只写 `WebMvcConfigurer`。这样预检请求、认证过滤器、异常处理链路更容易保持一致。

文件位置：`src/main/java/io/github/atengk/security/config/CorsConfig.java`

```java
package io.github.atengk.security.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * 跨域配置
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Configuration
public class CorsConfig {

    /**
     * 配置跨域规则
     *
     * @return 跨域配置源
     */
    @Bean
    public UrlBasedCorsConfigurationSource corsConfigurationSource() {
        log.info("初始化跨域配置");

        CorsConfiguration configuration = new CorsConfiguration();

        // 开发环境前端地址，生产环境应替换为真实域名
        configuration.setAllowedOriginPatterns(List.of(
                "http://localhost:5173",
                "http://localhost:3000",
                "https://*.example.com"
        ));

        // 允许的请求方法
        configuration.setAllowedMethods(List.of(
                "GET",
                "POST",
                "PUT",
                "DELETE",
                "PATCH",
                "OPTIONS"
        ));

        // 允许的请求头
        configuration.setAllowedHeaders(List.of(
                HttpHeaders.AUTHORIZATION,
                HttpHeaders.CONTENT_TYPE,
                HttpHeaders.ACCEPT,
                "X-Requested-With"
        ));

        // 暴露给前端读取的响应头
        configuration.setExposedHeaders(List.of(
                HttpHeaders.AUTHORIZATION
        ));

        // JWT 放在 Authorization 请求头中时通常不需要携带 Cookie
        configuration.setAllowCredentials(false);

        // 预检请求缓存时间，单位秒
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        // 所有接口应用同一套跨域规则
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

}
```

然后在 `SecurityConfig` 中启用 CORS。注意，这里只展示需要追加或确认的关键配置，其他 JWT、异常处理、权限配置沿用前文内容。

文件位置：`src/main/java/io/github/atengk/security/config/SecurityConfig.java`

```java
http
        // 启用 CORS，使用 CorsConfig 中的 UrlBasedCorsConfigurationSource
        .cors(cors -> cors.configurationSource(corsConfigurationSource))

        // JWT 前后端分离模式通常关闭 CSRF
        .csrf(AbstractHttpConfigurer::disable)

        // JWT 模式不使用服务端 Session
        .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        );
```

如果使用构造器注入，需要在 `SecurityConfig` 中加入字段：

```java
private final UrlBasedCorsConfigurationSource corsConfigurationSource;
```

跨域配置建议如下：

| 配置项                  | 建议                                     |
| ----------------------- | ---------------------------------------- |
| `allowedOriginPatterns` | 开发环境写 localhost，生产环境写真实域名 |
| `allowedMethods`        | 明确列出需要的方法，不建议无限制开放     |
| `allowedHeaders`        | 至少包含 `Authorization`、`Content-Type` |
| `allowCredentials`      | JWT Header 模式通常设为 `false`          |
| `maxAge`                | 可设置 1800 到 3600 秒，减少预检请求次数 |

如果 `allowCredentials(true)`，不要使用 `*` 作为允许来源。生产环境也不建议直接放开所有来源，否则会扩大接口暴露面。

### JSON 登录响应

前后端分离项目中，登录接口应返回 JSON，而不是服务端页面跳转。登录成功返回 Token、过期时间、用户基础信息和权限集合；登录失败返回统一错误结构。Spring Security 的表单登录认证成功与失败可以通过 `AuthenticationSuccessHandler` 和 `AuthenticationFailureHandler` 处理，但在 JWT 项目中，更推荐使用自定义 `/auth/login` Controller 调用 `AuthenticationManager` 后签发 Token。Spring Security 认证架构中，认证成功会设置认证结果，认证失败会清理安全上下文并交给失败处理器；自定义登录接口本质上是在 Controller 层主动调用同一套认证能力。([Home](https://docs.spring.io/spring-security/reference/6.5/servlet/index.html?utm_source=chatgpt.com))

登录成功响应建议统一为：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9.xxx.xxx",
    "tokenType": "Bearer",
    "expiresIn": 7200,
    "username": "admin",
    "authorities": [
      "ROLE_ADMIN",
      "system:user:query",
      "system:user:add"
    ]
  }
}
```

登录失败响应建议统一为：

```json
{
  "code": 401,
  "message": "用户名或密码错误",
  "data": null
}
```

如果需要将登录成功返回值进一步标准化，可以增加用户基础信息对象，避免前端从多个接口拼装数据。

文件位置：`src/main/java/io/github/atengk/security/module/auth/vo/LoginUserInfo.java`

```java
package io.github.atengk.security.module.auth.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 登录用户信息
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Data
@Builder
public class LoginUserInfo {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 权限集合
     */
    private List<String> authorities;

}
```

文件位置：`src/main/java/io/github/atengk/security/module/auth/vo/LoginResponse.java`

```java
package io.github.atengk.security.module.auth.vo;

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
     * 访问令牌
     */
    private String accessToken;

    /**
     * Token 类型
     */
    private String tokenType;

    /**
     * 过期时间，单位秒
     */
    private Long expiresIn;

    /**
     * 登录用户信息
     */
    private LoginUserInfo userInfo;

}
```

登录接口调整如下，主要变化是返回 `userInfo` 对象。

文件位置：`src/main/java/io/github/atengk/security/module/auth/controller/AuthController.java`

```java
package io.github.atengk.security.module.auth.controller;

import cn.hutool.core.collection.CollUtil;
import io.github.atengk.security.common.result.Result;
import io.github.atengk.security.module.auth.dto.LoginRequest;
import io.github.atengk.security.module.auth.vo.LoginResponse;
import io.github.atengk.security.module.auth.vo.LoginUserInfo;
import io.github.atengk.security.security.jwt.JwtProperties;
import io.github.atengk.security.security.jwt.JwtTokenProvider;
import io.github.atengk.security.security.model.SecurityUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 认证接口
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;

    /**
     * 用户登录
     *
     * @param request 登录请求参数
     * @return 登录响应
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("用户登录请求：{}", request.getUsername());

        UsernamePasswordAuthenticationToken authenticationToken =
                UsernamePasswordAuthenticationToken.unauthenticated(request.getUsername(), request.getPassword());

        Authentication authentication = authenticationManager.authenticate(authenticationToken);
        SecurityUser securityUser = (SecurityUser) authentication.getPrincipal();

        List<String> authorities = authentication.getAuthorities()
                .stream()
                .map(Object::toString)
                .toList();

        String accessToken = jwtTokenProvider.generateToken(authentication.getName(), CollUtil.newArrayList(authorities));

        LoginUserInfo userInfo = LoginUserInfo.builder()
                .userId(securityUser.getUserId())
                .username(securityUser.getUsername())
                .authorities(authorities)
                .build();

        LoginResponse response = LoginResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresIn(jwtProperties.getAccessTokenExpireSeconds())
                .userInfo(userInfo)
                .build();

        log.info("用户登录成功：{}，权限数量：{}", authentication.getName(), authorities.size());
        return Result.success(response);
    }

}
```

前端只需要判断 `code === 200` 后保存 `accessToken` 和用户信息。失败时直接读取 `message` 做提示即可。

### Token 传递方式

Token 传递方式需要前后端约定一致。本文推荐使用 `Authorization` 请求头，格式为 `Bearer <access_token>`。这种方式不会像 Cookie 一样被浏览器自动附加到跨站请求中，更适合 JWT 无状态接口认证。需要注意，Spring Security 默认会启用 CSRF 保护；如果使用 Cookie Session 作为认证凭证，应该谨慎处理 CSRF。如果使用前后端分离的 Bearer Token 请求头模式，通常会关闭 CSRF，并通过 Token、HTTPS、短有效期和服务端失效策略保证安全。Spring Security 文档说明 CSRF 默认保护不安全 HTTP 方法，也提供了 `CookieCsrfTokenRepository` 等 SPA 集成方式；具体策略应按认证凭证存放位置决定。([Home](https://docs.spring.io/spring-security/reference/6.5-SNAPSHOT/servlet/exploits/csrf.html?utm_source=chatgpt.com))

推荐请求头格式：

```text
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.xxx.xxx
```

前端建议封装统一 Axios 实例。

文件位置：`src/api/request.ts`

```typescript
import axios, { AxiosError, InternalAxiosRequestConfig } from 'axios'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
  timeout: 15000
})

request.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = localStorage.getItem('access_token')

  // 登录后统一携带 Bearer Token
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }

  return config
})

request.interceptors.response.use(
  response => {
    const data = response.data

    // 业务成功直接返回 data，具体项目可按统一响应结构调整
    if (data && data.code === 200) {
      return data
    }

    // 业务失败直接抛出，交给页面或全局错误处理
    return Promise.reject(data)
  },
  (error: AxiosError<any>) => {
    const status = error.response?.status
    const message = error.response?.data?.message || '请求失败'

    // Token 缺失、无效、过期时清理本地登录态
    if (status === 401) {
      localStorage.removeItem('access_token')
      localStorage.removeItem('user_info')
      window.location.href = '/login'
      return Promise.reject(new Error('登录已失效，请重新登录'))
    }

    // 权限不足时交给页面提示或跳转 403 页面
    if (status === 403) {
      return Promise.reject(new Error('权限不足'))
    }

    return Promise.reject(new Error(message))
  }
)

export default request
```

登录 API 封装如下。

文件位置：`src/api/auth.ts`

```typescript
import request from './request'

export interface LoginRequest {
  username: string
  password: string
}

export interface LoginUserInfo {
  userId: number
  username: string
  authorities: string[]
}

export interface LoginResponse {
  accessToken: string
  tokenType: string
  expiresIn: number
  userInfo: LoginUserInfo
}

export function loginApi(data: LoginRequest) {
  return request.post<any, { code: number; message: string; data: LoginResponse }>('/auth/login', data)
}

export function getUserInfoApi() {
  return request.get<any, { code: number; message: string; data: LoginUserInfo }>('/auth/userinfo')
}
```

登录页面调用示例：

```typescript
import { loginApi } from '@/api/auth'

async function handleLogin(username: string, password: string) {
  const response = await loginApi({ username, password })

  localStorage.setItem('access_token', response.data.accessToken)
  localStorage.setItem('user_info', JSON.stringify(response.data.userInfo))

  // 登录成功后跳转首页
  window.location.href = '/'
}
```

Token 存储方式对比：

| 存储方式         | 优点                     | 风险               | 建议                            |
| ---------------- | ------------------------ | ------------------ | ------------------------------- |
| `localStorage`   | 使用简单，刷新页面仍存在 | XSS 后可能被读取   | 内部系统可用，必须做好 XSS 防护 |
| `sessionStorage` | 关闭标签页后清理         | 仍可能受 XSS 影响  | 适合安全要求略高的后台          |
| HttpOnly Cookie  | JS 无法读取              | 需要重点处理 CSRF  | 适合服务端控制 Cookie 的系统    |
| 内存变量         | 页面关闭即失效           | 刷新页面丢失登录态 | 适合高安全、短会话场景          |

如果使用 `localStorage` 或 `sessionStorage`，前端必须避免将未转义的用户输入直接渲染为 HTML，并尽量开启 CSP、安全响应头和依赖安全扫描。

### 前端路由权限配合

前端路由权限用于控制页面是否可见、菜单是否展示、按钮是否可点击。它只能提升用户体验，不能替代后端权限校验。真正的权限边界必须在 Spring Security 的 URL 级权限和方法级权限中完成。

推荐前端权限判断基于后端返回的 `authorities` 集合，例如：

```text
ROLE_ADMIN
ROLE_USER
system:user:query
system:user:add
system:user:update
system:user:delete
```

Vue Router 路由元信息示例：

文件位置：`src/router/index.ts`

```typescript
import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/LoginView.vue'),
    meta: {
      public: true
    }
  },
  {
    path: '/',
    name: 'Home',
    component: () => import('@/views/home/HomeView.vue'),
    meta: {
      title: '首页'
    }
  },
  {
    path: '/system/user',
    name: 'SystemUser',
    component: () => import('@/views/system/user/UserView.vue'),
    meta: {
      title: '用户管理',
      permissions: ['system:user:query']
    }
  },
  {
    path: '/system/admin',
    name: 'SystemAdmin',
    component: () => import('@/views/system/admin/AdminView.vue'),
    meta: {
      title: '管理员页面',
      roles: ['ROLE_ADMIN']
    }
  },
  {
    path: '/403',
    name: 'Forbidden',
    component: () => import('@/views/error/ForbiddenView.vue'),
    meta: {
      public: true
    }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, _from, next) => {
  if (to.meta.public) {
    next()
    return
  }

  const token = localStorage.getItem('access_token')
  const userInfoText = localStorage.getItem('user_info')

  if (!token || !userInfoText) {
    next('/login')
    return
  }

  const userInfo = JSON.parse(userInfoText)
  const authorities: string[] = userInfo.authorities || []

  const requiredRoles = (to.meta.roles || []) as string[]
  const requiredPermissions = (to.meta.permissions || []) as string[]

  const hasRole = requiredRoles.length === 0 || requiredRoles.some(role => authorities.includes(role))
  const hasPermission = requiredPermissions.length === 0 || requiredPermissions.some(permission => authorities.includes(permission))

  if (hasRole && hasPermission) {
    next()
    return
  }

  next('/403')
})

export default router
```

按钮权限指令示例：

文件位置：`src/directives/permission.ts`

```typescript
import type { App, DirectiveBinding } from 'vue'

function hasPermission(requiredPermissions: string[]): boolean {
  const userInfoText = localStorage.getItem('user_info')

  if (!userInfoText) {
    return false
  }

  const userInfo = JSON.parse(userInfoText)
  const authorities: string[] = userInfo.authorities || []

  return requiredPermissions.some(permission => authorities.includes(permission))
}

export default {
  install(app: App) {
    app.directive('permission', {
      mounted(el: HTMLElement, binding: DirectiveBinding<string[]>) {
        const permissions = binding.value || []

        // 没有权限时移除按钮，避免误操作
        if (permissions.length > 0 && !hasPermission(permissions)) {
          el.parentNode?.removeChild(el)
        }
      }
    })
  }
}
```

页面使用示例：

```vue
<template>
  <div class="p-4">
    <button v-permission="['system:user:add']">
      新增用户
    </button>

    <button v-permission="['system:user:update']">
      修改用户
    </button>

    <button v-permission="['system:user:delete']">
      删除用户
    </button>
  </div>
</template>
```

前后端权限配合建议如下：

| 层级          | 控制内容               | 是否可信                    |
| ------------- | ---------------------- | --------------------------- |
| 前端路由权限  | 页面是否可进入         | 不可信，只做体验            |
| 前端菜单权限  | 菜单是否显示           | 不可信，只做体验            |
| 前端按钮权限  | 按钮是否展示           | 不可信，只做体验            |
| 后端 URL 权限 | 接口大类访问控制       | 可信                        |
| 后端方法权限  | 业务动作级控制         | 可信                        |
| 数据权限      | 当前用户能操作哪些数据 | 可信，通常在 Service 层实现 |

## 测试与验证

本节用于验证 Spring Security 的认证、授权、Token 失效和异常响应是否符合预期。建议同时使用接口联调和自动化测试：接口联调用于快速排查链路问题，自动化测试用于防止后续改动破坏安全规则。Spring Security 提供了 Spring MVC Test 集成能力，也支持在 MockMvc 测试中模拟用户和安全上下文；官方文档说明可以通过 `apply(springSecurity())` 把安全过滤器链集成进 MockMvc，也可以使用 `user()` 等 `RequestPostProcessor` 模拟认证用户。([Home](https://docs.spring.io/spring-security/reference/servlet/test/mockmvc/?utm_source=chatgpt.com))

### 认证流程验证

认证流程验证用于确认登录接口、密码校验、Token 生成、Token 解析和当前用户信息接口都正常工作。建议先用 curl 或 Postman 验证，再补充 MockMvc 自动化测试。

接口验证顺序如下：

```text
1. 调用 /auth/login，使用正确用户名和密码
2. 检查响应中是否存在 accessToken
3. 使用 Authorization: Bearer <accessToken> 调用 /auth/userinfo
4. 检查返回的 username、userId、authorities 是否正确
5. 使用错误密码登录，确认返回 401
6. 不携带 Token 调用 /auth/userinfo，确认返回 401
```

curl 验证命令如下：

```bash
# 1. 登录获取 Token
TOKEN=$(curl -s -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}' \
  | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p')

# 2. 输出 Token，确认不为空
echo "${TOKEN}"

# 3. 携带 Token 查询当前用户信息
curl http://localhost:8080/auth/userinfo \
  -H "Authorization: Bearer ${TOKEN}"

# 4. 错误密码验证，预期返回 401
curl -i -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"wrong-password"}'

# 5. 不携带 Token，预期返回 401
curl -i http://localhost:8080/auth/userinfo
```

MockMvc 测试类如下。该测试假设测试库中已经存在 `admin / 123456` 用户，并且该用户拥有查询权限和管理员角色。

文件位置：`src/test/java/io/github/atengk/security/AuthFlowTests.java`

```java
package io.github.atengk.security;

import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.ActiveProfiles;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.nio.charset.StandardCharsets;

/**
 * 认证流程测试
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("db")
class AuthFlowTests {

    @Autowired
    private MockMvc mockMvc;

    /**
     * 验证登录成功并返回 Token
     *
     * @throws Exception 测试异常
     */
    @Test
    void loginSuccessShouldReturnToken() throws Exception {
        String requestBody = """
                {
                  "username": "admin",
                  "password": "123456"
                }
                """;

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.accessToken", notNullValue()))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"));
    }

    /**
     * 验证错误密码返回 401
     *
     * @throws Exception 测试异常
     */
    @Test
    void loginFailShouldReturnUnauthorized() throws Exception {
        String requestBody = """
                {
                  "username": "admin",
                  "password": "wrong-password"
                }
                """;

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    /**
     * 验证未携带 Token 访问用户信息返回 401
     *
     * @throws Exception 测试异常
     */
    @Test
    void userInfoWithoutTokenShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/auth/userinfo"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    /**
     * 验证携带 Token 可以访问用户信息
     *
     * @throws Exception 测试异常
     */
    @Test
    void userInfoWithTokenShouldReturnSuccess() throws Exception {
        String token = loginAndGetToken("admin", "123456");

        mockMvc.perform(get("/auth/userinfo")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.username").value("admin"));
    }

    /**
     * 登录并提取 Token
     *
     * @param username 用户名
     * @param password 密码
     * @return Access Token
     * @throws Exception 测试异常
     */
    private String loginAndGetToken(String username, String password) throws Exception {
        String requestBody = JSONUtil.createObj()
                .set("username", username)
                .set("password", password)
                .toString();

        String responseBody = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        return JSONUtil.parseObj(responseBody)
                .getJSONObject("data")
                .getStr("accessToken");
    }

}
```

注意：如果 `GlobalExceptionHandler` 返回的是业务 JSON，但 HTTP 状态仍为 200，那么登录失败断言应检查 `$.code = 401`；如果你希望 HTTP 状态也返回 401，可以在异常处理方法上改用 `ResponseEntity<Result<?>>`。

### 权限控制验证

权限控制验证用于确认 URL 级权限、方法级权限、角色前缀和权限标识都符合预期。重点验证三类请求：有权限访问成功、无权限返回 403、未登录返回 401。

接口验证命令如下：

```bash
# 管理员登录
ADMIN_TOKEN=$(curl -s -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}' \
  | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p')

# 普通用户登录，前提是数据库中存在 user / 123456
USER_TOKEN=$(curl -s -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"user","password":"123456"}' \
  | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p')

# 管理员访问管理员资源，预期 200
curl -i http://localhost:8080/resource/admin \
  -H "Authorization: Bearer ${ADMIN_TOKEN}"

# 普通用户访问管理员资源，预期 403
curl -i http://localhost:8080/resource/admin \
  -H "Authorization: Bearer ${USER_TOKEN}"

# 普通用户访问查询权限资源，预期 200
curl -i http://localhost:8080/resource/user/query \
  -H "Authorization: Bearer ${USER_TOKEN}"

# 普通用户访问删除权限资源，预期 403
curl -i -X DELETE http://localhost:8080/resource/user/1 \
  -H "Authorization: Bearer ${USER_TOKEN}"
```

MockMvc 权限测试如下。Spring Security 测试支持可以用 `user("user").roles("USER")` 或 `user("admin").authorities(...)` 模拟当前请求的用户，这适合测试方法级权限和接口授权规则；官方文档说明 `user()` 会把用户关联到当前 `HttpServletRequest`，并需要将 Spring Security 集成到 MockMvc。([Home](https://docs.spring.io/spring-security/reference/servlet/test/mockmvc/authentication.html?utm_source=chatgpt.com))

文件位置：`src/test/java/io/github/atengk/security/PermissionTests.java`

```java
package io.github.atengk.security;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 权限控制测试
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("db")
class PermissionTests {

    @Autowired
    private MockMvc mockMvc;

    /**
     * 管理员访问管理员资源应成功
     *
     * @throws Exception 测试异常
     */
    @Test
    void adminAccessAdminResourceShouldSuccess() throws Exception {
        mockMvc.perform(get("/resource/admin")
                        .with(user("admin")
                                .roles("ADMIN")
                                .authorities(() -> "system:user:query")))
                .andExpect(status().isOk());
    }

    /**
     * 普通用户访问管理员资源应返回 403
     *
     * @throws Exception 测试异常
     */
    @Test
    void userAccessAdminResourceShouldForbidden() throws Exception {
        mockMvc.perform(get("/resource/admin")
                        .with(user("user")
                                .roles("USER")
                                .authorities(() -> "system:user:query")))
                .andExpect(status().isForbidden());
    }

    /**
     * 拥有查询权限应能访问查询接口
     *
     * @throws Exception 测试异常
     */
    @Test
    void userQueryPermissionShouldSuccess() throws Exception {
        mockMvc.perform(get("/resource/user/query")
                        .with(user("user")
                                .roles("USER")
                                .authorities(() -> "system:user:query")))
                .andExpect(status().isOk());
    }

    /**
     * 缺少删除权限应返回 403
     *
     * @throws Exception 测试异常
     */
    @Test
    void userDeleteWithoutPermissionShouldForbidden() throws Exception {
        mockMvc.perform(delete("/resource/user/1")
                        .with(user("user")
                                .roles("USER")
                                .authorities(() -> "system:user:query")))
                .andExpect(status().isForbidden());
    }

}
```

如果项目启用了 CSRF，并测试 POST、PUT、DELETE 等非安全方法，需要在 MockMvc 请求中添加 `csrf()`；Spring Security 文档明确说明，测试启用 CSRF 的非安全 HTTP 方法时必须包含合法 CSRF Token，也可以用 `csrf().useInvalidToken()` 测试非法 Token。([Home](https://docs.spring.io/spring-security/reference/7.0/servlet/test/mockmvc/csrf.html?utm_source=chatgpt.com))

```java
mockMvc.perform(delete("/resource/user/1")
                .with(csrf())
                .with(user("admin").roles("ADMIN").authorities(() -> "system:user:delete")))
        .andExpect(status().isOk());
```

JWT 无状态模式下前文已经关闭 CSRF，因此通常不需要 `csrf()`。

### Token 失效验证

Token 失效验证用于确认过期 Token、伪造 Token、错误签名 Token、空 Token 和注销后的 Token 都能被正确拒绝。JWT 是无状态令牌，服务端默认不保存登录状态，因此“过期失效”和“签名校验失败”天然支持；“主动注销失效”和“修改密码后失效”需要额外设计，例如 Redis 黑名单、Token 版本号或用户密码更新时间校验。

建议验证以下场景：

| 场景           | 操作                           | 预期结果                          |
| -------------- | ------------------------------ | --------------------------------- |
| Token 为空     | 不传 `Authorization`           | 返回 401                          |
| Token 格式错误 | 传 `Authorization: Bearer abc` | 返回 401                          |
| Token 被篡改   | 修改 Token 任意字符            | 返回 401                          |
| Token 过期     | 等待过期或缩短过期时间         | 返回 401                          |
| 用户被禁用     | 登录后禁用用户，再访问接口     | 返回 401 或 403，取决于过滤器处理 |
| 权限变更       | 登录后删除权限，再访问接口     | 如果过滤器重新查库，返回 403      |
| 注销失效       | 加入黑名单后访问接口           | 返回 401                          |

开发阶段可以把 Token 有效期临时调短，便于测试过期。

文件位置：`src/main/resources/application.yml`

```yaml
security:
  jwt:
    # 开发阶段临时设置为 10 秒，便于验证 Token 过期
    access-token-expire-seconds: 10
```

curl 验证命令如下：

```bash
# 1. 登录获取短有效期 Token
TOKEN=$(curl -s -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}' \
  | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p')

# 2. 立即访问，预期成功
curl -i http://localhost:8080/auth/userinfo \
  -H "Authorization: Bearer ${TOKEN}"

# 3. 等待 Token 过期
sleep 12

# 4. 再次访问，预期返回 401
curl -i http://localhost:8080/auth/userinfo \
  -H "Authorization: Bearer ${TOKEN}"

# 5. 使用伪造 Token，预期返回 401
curl -i http://localhost:8080/auth/userinfo \
  -H "Authorization: Bearer fake-token"
```

如果要实现注销后 Token 立即失效，可以增加 Redis 黑名单。这里给出设计思路，不展开完整 Redis 代码：

```text
用户退出登录
  -> 前端调用 /auth/logout
  -> 后端解析当前 Token 的 jti 或完整 Token 摘要
  -> 写入 Redis 黑名单，过期时间等于 Token 剩余有效期
  -> JwtAuthenticationFilter 每次认证前检查黑名单
  -> 命中黑名单则返回 401
```

生产项目中更推荐给 JWT 增加 `jti` 声明，然后只保存 `jti` 到 Redis，而不是保存完整 Token。也可以在用户表中维护 `token_version` 或 `password_update_time`，过滤器解析 Token 后和数据库当前值比对，发现版本不一致就拒绝访问。

### 常见问题排查

本节汇总前后端分离 Spring Security 开发中最常见的问题。排查时建议先看 HTTP 状态码，再看后端日志，最后看浏览器 Network 里的请求头、响应头和预检请求。

| 问题                      | 典型表现                       | 原因                                  | 处理方式                                        |
| ------------------------- | ------------------------------ | ------------------------------------- | ----------------------------------------------- |
| 登录接口返回 401          | `/auth/login` 被拦截           | 登录接口未 `permitAll()`              | 在 `SecurityConfig` 中放行 `/auth/login`        |
| 登录接口返回 403          | POST 登录被拒绝                | CSRF 未关闭或未带 CSRF Token          | JWT 模式关闭 CSRF，Session 模式带 CSRF          |
| 浏览器提示 CORS           | 控制台显示跨域错误             | CORS 未配置或预检请求被拦截           | 配置 `CorsConfigurationSource` 并启用 `.cors()` |
| Postman 正常，浏览器失败  | Postman 不受浏览器同源策略限制 | 跨域配置问题                          | 检查 OPTIONS 预检请求                           |
| 携带 Token 仍返回 401     | Token 未被识别                 | Header 格式错误、Token 过期、密钥变化 | 确认 `Authorization: Bearer <token>`            |
| 携带 Token 返回 403       | 认证成功但权限不足             | 缺少角色或权限标识                    | 检查 `authorities` 和 `@PreAuthorize`           |
| `hasRole` 不生效          | 明明有 ADMIN 仍 403            | 角色前缀不一致                        | `hasRole("ADMIN")` 对应 `ROLE_ADMIN`            |
| `hasAuthority` 不生效     | 权限注解返回 403               | 权限字符串不一致                      | 确认数据库权限与注解完全一致                    |
| 方法注解无效              | `@PreAuthorize` 不生效         | 未开启方法级安全                      | 增加 `@EnableMethodSecurity`                    |
| OPTIONS 请求 401          | 浏览器预检失败                 | 预检请求被安全链拦截                  | 启用 CORS，必要时放行 OPTIONS                   |
| Token 过期后前端死循环    | 页面不断跳登录                 | 响应拦截器重复跳转                    | 401 时清理 Token 并避免重复重定向               |
| 修改权限后旧 Token 仍有效 | 旧权限仍可访问                 | 权限写入 Token 且不过期               | 过滤器重新查库或缩短 Token 有效期               |

后端日志建议临时开启以下配置，便于定位 Spring Security 过滤器和授权过程：

文件位置：`src/main/resources/application.yml`

```yaml
logging:
  level:
    # 开发调试阶段查看 Spring Security 详细日志，生产环境建议改回 INFO
    org.springframework.security: DEBUG
    # 查看本项目安全相关日志
    io.github.atengk.security: DEBUG
```

排查顺序建议固定为：

```text
1. 看浏览器 Network，确认请求是否真正发到后端
2. 看是否存在 OPTIONS 预检请求失败
3. 看请求头是否包含 Authorization
4. 看 Authorization 是否为 Bearer <token>
5. 看后端 JwtAuthenticationFilter 是否打印认证成功日志
6. 看 SecurityContext 中是否存在 Authentication
7. 看用户 authorities 是否包含目标角色或权限
8. 看 URL 规则和 @PreAuthorize 表达式是否匹配
9. 看返回状态是 401 还是 403
10. 看前端响应拦截器是否错误处理了状态码
```

最终验收标准如下：

```text
1. 前端 localhost 域名可以正常调用后端接口
2. /auth/login 成功返回 JSON Token
3. 前端能自动携带 Authorization: Bearer <token>
4. /auth/userinfo 能返回当前用户和权限集合
5. 普通用户访问管理员接口返回 403
6. 未登录访问受保护接口返回 401
7. Token 过期、伪造、篡改后返回 401
8. 前端路由、菜单、按钮能按 authorities 控制展示
9. 后端 URL 权限和方法权限能兜底拦截非法访问
10. 所有认证授权异常都返回统一 JSON 响应
```