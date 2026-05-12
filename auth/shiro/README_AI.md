# Apache Shiro 2

Apache Shiro 2 是一个面向 JVM 应用的安全框架，适合在 Spring Boot 3 项目中实现认证、授权、密码加密、会话管理和权限拦截。本文示例以 Spring Boot 3.x、JDK 17+、Apache Shiro 2.1.0 为基础，后续章节会围绕登录认证、角色权限、接口拦截和前后端分离 Token 认证展开。Apache Shiro 官方文档显示，Shiro v1 已于 2024 年 2 月 28 日被 v2 取代，当前 Shiro 稳定版本为 2.1.0。([Apache Shiro](https://shiro.apache.org/spring-boot.html))

## 技术概述

本节说明 Apache Shiro 2 在 Spring Boot 3 项目中的定位、适配重点和核心安全模型。Shiro 的设计重点是把认证、授权、会话和加密能力抽象为统一的安全组件，业务系统只需要实现自己的用户、角色、权限加载逻辑即可。

### Apache Shiro 2 简介

Apache Shiro 2 是 Apache Shiro 的主线版本，提供认证、授权、加密和会话管理等能力，可用于普通 Java 应用、Web 应用和企业级后端服务。Shiro 的 API 相对轻量，核心概念清晰，适合在已有业务系统中快速接入权限控制。Apache Shiro 官方对其定位包括 Authentication、Authorization、Cryptography 和 Session Management。([Apache Shiro](https://shiro.apache.org/?utm_source=chatgpt.com))

在 Spring Boot 3 后端项目中，Shiro 通常承担以下职责：

| 能力     | 说明                                                 | 常见落地点                                |
| -------- | ---------------------------------------------------- | ----------------------------------------- |
| 认证     | 判断用户身份是否合法，例如用户名密码登录、Token 登录 | 登录接口、Realm、AuthenticationToken      |
| 授权     | 判断用户是否拥有角色或权限                           | 接口注解、FilterChain、权限缓存           |
| 加密     | 对密码、摘要、密钥等进行安全处理                     | 密码存储、密码校验、RememberMe            |
| 会话     | 管理登录状态和用户上下文                             | Session、Subject、SecurityManager         |
| Web 拦截 | 对 URL 请求进行安全过滤                              | ShiroFilterChainDefinition、自定义 Filter |

Shiro 2 在工程实践中一般不会直接把权限逻辑写在 Controller 中，而是通过 `Realm` 从数据库加载用户身份、角色和权限，再通过注解或过滤器完成统一校验。这样可以让业务接口保持清晰，也便于后续接入 Redis 缓存、前后端分离 Token 或多端登录控制。

### Spring Boot 3 适配说明

Spring Boot 3 的底层基线是 Java 17 和 Spring Framework 6，依赖体系从 Java EE 的 `javax.*` 迁移到了 Jakarta EE 的 `jakarta.*`。Spring Boot 3 迁移指南明确要求避免继续使用旧的 Java EE 依赖，例如应使用 `jakarta.servlet:jakarta.servlet-api`，而不是 `javax.servlet:javax.servlet-api`。([GitHub](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-3.0-Migration-Guide?utm_source=chatgpt.com))

在 Spring Boot 3 中集成 Shiro 2 时，需要重点关注以下几点：

| 适配点       | 说明                                                         |
| ------------ | ------------------------------------------------------------ |
| JDK 版本     | Spring Boot 3 要求 JDK 17+，建议生产环境统一使用 JDK 17 或 JDK 21 |
| Servlet 包名 | 项目代码和依赖应使用 `jakarta.servlet.*`，不要混用 `javax.servlet.*` |
| Web Starter  | Web 项目优先使用 `shiro-spring-boot-web-starter`，不是普通命令行项目的 `shiro-spring-boot-starter` |
| FilterChain  | Web 请求必须经过 Shiro 主过滤器，并通过 URL 规则配置不同访问级别 |
| 注解控制     | `@RequiresRoles`、`@RequiresPermissions` 等注解在 Shiro Spring Boot Starter 中默认启用 |
| 默认放行策略 | Shiro 2.x 中 `shiro.allowAccessByDefault` 默认为 `true`，实际项目建议显式配置拦截规则，避免遗漏接口 |

Apache Shiro 官方 Spring Boot 集成文档中，Web 应用使用的依赖是 `org.apache.shiro:shiro-spring-boot-web-starter:2.1.0`，并要求提供 `Realm` 和 `ShiroFilterChainDefinition` Bean；文档也说明 Web 请求会经过 Shiro 主 Filter，通过 URL Path 配置不同的访问控制链。([Apache Shiro](https://shiro.apache.org/spring-boot.html))

### 认证与授权核心概念

Shiro 的认证与授权围绕 `Subject`、`SecurityManager`、`Realm`、`AuthenticationToken`、`AuthenticationInfo` 和 `AuthorizationInfo` 展开。开发时要把这些概念和业务表结构对应起来，否则后续登录、权限加载和接口拦截会比较混乱。

| 概念                  | 作用                                           | 项目中的常见实现                                        |
| --------------------- | ---------------------------------------------- | ------------------------------------------------------- |
| `Subject`             | 当前访问主体，可以是用户、程序或外部系统       | 当前登录用户上下文                                      |
| `SecurityManager`     | Shiro 的核心调度器，负责认证、授权、会话等流程 | 由 Shiro Starter 自动装配或通过配置扩展                 |
| `Realm`               | Shiro 与业务数据源之间的桥梁                   | 查询用户、角色、权限                                    |
| `AuthenticationToken` | 用户提交的认证凭证                             | 用户名密码 Token、自定义 JWT Token                      |
| `AuthenticationInfo`  | 认证阶段返回的用户身份与凭证信息               | 用户 ID、用户名、密码摘要、盐值                         |
| `AuthorizationInfo`   | 授权阶段返回的角色与权限信息                   | 角色编码、权限标识                                      |
| `CredentialsMatcher`  | 密码或凭证匹配器                               | BCrypt、SHA256、Argon2 等校验逻辑                       |
| `FilterChain`         | URL 级别的访问控制规则                         | `/auth/login = anon`、`/admin/** = authc, roles[admin]` |

认证流程通常是：客户端提交登录参数，Controller 构造 `AuthenticationToken`，调用 `Subject.login(token)`，Shiro 委托 `Realm#doGetAuthenticationInfo` 查询用户信息并校验密码，认证成功后保存登录状态或返回 Token。

授权流程通常是：请求进入受保护接口，Shiro 根据注解或 URL 规则判断是否需要角色或权限，随后调用 `Realm#doGetAuthorizationInfo` 加载当前用户的角色和权限集合，再完成访问判定。

## 环境准备

本节给出开发 Apache Shiro 2 + Spring Boot 3 项目前需要准备的 JDK、Maven 依赖和基础目录结构。示例采用 Maven、Spring Boot Web、MyBatis-Plus、MySQL、Lombok、Hutool 和 Shiro Web Starter，便于后续直接扩展数据库认证和接口权限控制。

### JDK 与 Spring Boot 版本要求

Spring Boot 3.x 的最低运行要求是 Java 17。以 Spring Boot 3.0.x 官方文档为基线，Spring Boot 3.0.5 要求 Java 17，并需要 Spring Framework 6.0.7 或以上版本；Maven 构建要求为 3.5+。较新的 Spring Boot 3.5.x 文档中，Maven 要求提升为 3.6.3 或以上。([Home](https://docs.spring.io/spring-boot/docs/3.0.5/reference/html/getting-started.html?utm_source=chatgpt.com))

推荐版本如下：

| 工具         | 推荐版本 | 说明                                                |
| ------------ | -------- | --------------------------------------------------- |
| JDK          | 17 或 21 | Spring Boot 3 最低要求 JDK 17，生产环境建议统一版本 |
| Spring Boot  | 3.5.13   | 3.5.13 于 2026-03-26 发布，可作为 3.x 示例版本      |
| Apache Shiro | 2.1.0    | 当前 Shiro 2 稳定版本                               |
| Maven        | 3.8+     | 满足 Shiro 源码构建和 Spring Boot 3.5.x 构建要求    |
| MySQL        | 8.0+     | 用于存储用户、角色、权限数据                        |
| MyBatis-Plus | 3.5.x    | 简化用户和权限表 CRUD                               |

开发环境可以先检查 JDK 和 Maven 版本：

```bash
java -version
mvn -version
```

`java -version` 用于确认当前终端使用的 JDK 版本，`mvn -version` 用于确认 Maven 版本以及 Maven 运行时绑定的 Java 版本。若 IDE 和终端版本不一致，优先检查 `JAVA_HOME`、IDE Project SDK 和 Maven Runner JRE。

### Maven 依赖配置

下面的 `pom.xml` 定义 Spring Boot 3、Apache Shiro 2、MyBatis-Plus、MySQL、Hutool 和 Lombok 等基础依赖，可作为项目初始化配置使用。Shiro Web 项目使用 `shiro-spring-boot-web-starter`，该依赖是 Shiro 官方 Spring Boot Web 集成文档中推荐的 Web Starter。([Apache Shiro](https://shiro.apache.org/spring-boot.html))

文件位置：`pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="
             http://maven.apache.org/POM/4.0.0
             https://maven.apache.org/xsd/maven-4.0.0.xsd">

    <modelVersion>4.0.0</modelVersion>

    <!-- Spring Boot 3.x 父工程，统一管理 Spring 生态依赖版本 -->
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.5.13</version>
        <relativePath/>
    </parent>

    <groupId>io.github.atengk</groupId>
    <artifactId>springboot3-shiro2-demo</artifactId>
    <version>1.0.0</version>
    <name>springboot3-shiro2-demo</name>
    <description>Spring Boot 3 集成 Apache Shiro 2 示例项目</description>

    <properties>
        <!-- Spring Boot 3 最低要求 JDK 17 -->
        <java.version>17</java.version>

        <!-- Apache Shiro 2 当前稳定版本 -->
        <shiro.version>2.1.0</shiro.version>

        <!-- MyBatis-Plus Spring Boot 3 适配版本 -->
        <mybatis-plus.version>3.5.12</mybatis-plus.version>

        <!-- Hutool 工具包，后续用于字符串、集合、加密、日期等工具处理 -->
        <hutool.version>5.8.44</hutool.version>
    </properties>

    <dependencies>
        <!-- Spring MVC、内置 Tomcat、JSON 序列化等 Web 基础能力 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Apache Shiro 2 Web Starter：用于 Spring Boot Web 应用集成 Shiro Filter、Realm、注解权限等 -->
        <dependency>
            <groupId>org.apache.shiro</groupId>
            <artifactId>shiro-spring-boot-web-starter</artifactId>
            <version>${shiro.version}</version>
        </dependency>

        <!-- MyBatis-Plus：简化用户、角色、权限等表的 CRUD 操作 -->
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
            <version>${mybatis-plus.version}</version>
        </dependency>

        <!-- MySQL JDBC 驱动：连接 MySQL 8.x 数据库 -->
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- Hutool：常用 Java 工具类库，后续用于参数校验、字符串处理、集合处理和加密辅助 -->
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
            <version>${hutool.version}</version>
        </dependency>

        <!-- Lombok：减少 Entity、DTO、VO 中的 getter/setter 样板代码 -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Spring Boot Test：用于接口、Realm、权限校验等单元测试和集成测试 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <!-- Spring Boot 打包插件：支持生成可执行 jar -->
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

Hutool 当前 5.8.x 分支在 Maven Central 上可用版本包含 `5.8.44`，本文使用该版本作为工具类依赖示例。([Maven Repository](https://repo1.maven.org/maven2/cn/hutool/hutool-all/?utm_source=chatgpt.com))

基础配置文件可以先保留数据库、MyBatis-Plus 和 Shiro 常用配置，后续在认证、授权、前后端分离章节中继续扩展。

文件位置：`src/main/resources/application.yml`

```yaml
server:
  port: 8080

spring:
  application:
    name: springboot3-shiro2-demo

  datasource:
    # MySQL 8.x 连接地址，根据实际环境修改数据库名、地址和时区
    url: jdbc:mysql://127.0.0.1:3306/shiro_demo?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false
    username: root
    password: root
    driver-class-name: com.mysql.cj.jdbc.Driver

mybatis-plus:
  configuration:
    # 开发阶段打开 SQL 日志，生产环境建议关闭或切换为规范日志方案
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  global-config:
    db-config:
      # 主键策略，示例使用数据库自增
      id-type: auto
      # 逻辑删除字段，后续用户、角色、权限表可统一使用
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0

shiro:
  # 是否启用 Shiro Spring 模块
  enabled: true

  web:
    # 是否启用 Shiro Web 模块
    enabled: true

  annotations:
    # 是否启用 @RequiresRoles、@RequiresPermissions 等注解
    enabled: true

  # 未登录时的默认登录地址。前后端分离项目通常会通过自定义 Filter 返回 JSON，而不是跳转页面
  loginUrl: /api/auth/unauthorized

  # 无权限时的默认地址。前后端分离项目通常会统一返回 403 JSON
  unauthorizedUrl: /api/auth/forbidden

  # Shiro 2.x 默认为 true，实际项目建议通过明确的 FilterChain 控制访问规则
  allowAccessByDefault: false
```

### 项目基础结构

项目采用常见的 Spring Boot 分层结构，先预留认证、授权、配置、通用响应和数据库访问相关包。后续章节可以在该结构上继续补充 `Realm`、登录接口、自定义 Token、自定义 Filter、权限注解和数据库表设计。

下面目录结构展示项目初始化后的核心文件分布：

```text
springboot3-shiro2-demo
├── pom.xml
├── src
│   ├── main
│   │   ├── java
│   │   │   └── io
│   │   │       └── github
│   │   │           └── atengk
│   │   │               └── shiro
│   │   │                   ├── ShiroDemoApplication.java
│   │   │                   ├── common
│   │   │                   │   ├── response
│   │   │                   │   │   └── Result.java
│   │   │                   │   └── exception
│   │   │                   │       └── GlobalExceptionHandler.java
│   │   │                   ├── config
│   │   │                   │   └── ShiroConfig.java
│   │   │                   ├── module
│   │   │                   │   ├── auth
│   │   │                   │   │   ├── controller
│   │   │                   │   │   ├── dto
│   │   │                   │   │   ├── service
│   │   │                   │   │   └── realm
│   │   │                   │   ├── user
│   │   │                   │   │   ├── entity
│   │   │                   │   │   ├── mapper
│   │   │                   │   │   └── service
│   │   │                   │   ├── role
│   │   │                   │   │   ├── entity
│   │   │                   │   │   ├── mapper
│   │   │                   │   │   └── service
│   │   │                   │   └── permission
│   │   │                   │       ├── entity
│   │   │                   │       ├── mapper
│   │   │                   │       └── service
│   │   │                   └── security
│   │   │                       ├── filter
│   │   │                       ├── token
│   │   │                       └── util
│   │   └── resources
│   │       ├── application.yml
│   │       └── mapper
│   │           ├── user
│   │           ├── role
│   │           └── permission
│   └── test
│       └── java
│           └── io
│               └── github
│                   └── atengk
│                       └── shiro
│                           └── ShiroDemoApplicationTests.java
```

基础启动类放在根包 `io.github.atengk.shiro` 下，保证 Spring Boot 能扫描到 `config`、`module`、`security` 等子包。

文件位置：`src/main/java/io/github/atengk/shiro/ShiroDemoApplication.java`

```java
package io.github.atengk.shiro;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot 3 集成 Apache Shiro 2 示例项目启动类。
 *
 * @author Ateng
 * @since 2026-05-12
 */
@SpringBootApplication
public class ShiroDemoApplication {

    /**
     * 应用启动入口。
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(ShiroDemoApplication.class, args);
    }

}
```

项目启动后，可以先执行以下命令验证依赖和基础工程是否正常：

```bash
mvn clean package -DskipTests
mvn spring-boot:run
```

`mvn clean package -DskipTests` 用于验证依赖解析、编译和打包流程是否正常；`mvn spring-boot:run` 用于启动本地开发服务。若启动失败，优先检查 JDK 版本、Maven 版本、MySQL 连接配置，以及依赖树中是否混入旧的 `javax.servlet` 相关依赖。



## Shiro 核心配置

本节配置 Shiro 在 Spring Boot 3 Web 项目中的核心运行组件，包括 `SecurityManager`、`Realm`、`ShiroFilterChainDefinition`、Session 和 RememberMe。Shiro 官方 Spring Boot 文档要求 Web 请求通过 Shiro 主过滤器，并通过 `Realm` 和 `ShiroFilterChainDefinition` 完成业务身份认证与 URL 访问控制；Shiro 注解如 `@RequiresRoles`、`@RequiresPermissions` 在 Spring Boot Starter 中默认启用。([Apache Shiro](https://shiro.apache.org/spring-boot.html))

### SecurityManager 配置

在 Spring Boot 3 + Shiro 2 Web 项目中，通常不需要手动 `new DefaultWebSecurityManager`。推荐做法是交给 `shiro-spring-boot-web-starter` 自动装配 `SecurityManager`，业务侧提供 `Realm`、缓存、会话参数和过滤链即可。Shiro 官方文档也明确说明 Web 应用不应使用 standalone 示例中的静态 `SecurityUtils.setSecurityManager(...)` 方式。([Apache Shiro](https://shiro.apache.org/spring-boot.html))

核心配置类放在 `config` 包下，负责声明密码匹配器、Realm、缓存和 URL 过滤链。

文件位置：`src/main/java/io/github/atengk/shiro/config/ShiroConfig.java`

```java
package io.github.atengk.shiro.config;

import io.github.atengk.shiro.module.auth.realm.UserPasswordRealm;
import io.github.atengk.shiro.module.auth.service.SecurityUserService;
import org.apache.shiro.authc.credential.HashedCredentialsMatcher;
import org.apache.shiro.cache.CacheManager;
import org.apache.shiro.cache.MemoryConstrainedCacheManager;
import org.apache.shiro.crypto.hash.Sha256Hash;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.spring.web.config.DefaultShiroFilterChainDefinition;
import org.apache.shiro.spring.web.config.ShiroFilterChainDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Apache Shiro 2 核心配置。
 *
 * @author Ateng
 * @since 2026-05-12
 */
@Configuration
public class ShiroConfig {

    private static final int HASH_ITERATIONS = 1024;

    @Bean
    public HashedCredentialsMatcher hashedCredentialsMatcher() {
        HashedCredentialsMatcher matcher = new HashedCredentialsMatcher(Sha256Hash.ALGORITHM_NAME);
        matcher.setHashIterations(HASH_ITERATIONS);
        matcher.setStoredCredentialsHexEncoded(true);
        return matcher;
    }

    @Bean
    public Realm userPasswordRealm(SecurityUserService securityUserService,
                                   HashedCredentialsMatcher hashedCredentialsMatcher) {
        UserPasswordRealm realm = new UserPasswordRealm(securityUserService);
        realm.setCredentialsMatcher(hashedCredentialsMatcher);

        // 开启 Realm 缓存，后续可替换为 Redis、Caffeine 等缓存实现
        realm.setCachingEnabled(true);
        realm.setAuthenticationCachingEnabled(true);
        realm.setAuthorizationCachingEnabled(true);
        return realm;
    }

    @Bean
    public CacheManager shiroCacheManager() {
        // 示例使用内存缓存，生产环境建议接入集中式缓存
        return new MemoryConstrainedCacheManager();
    }

    @Bean
    public ShiroFilterChainDefinition shiroFilterChainDefinition() {
        DefaultShiroFilterChainDefinition chainDefinition = new DefaultShiroFilterChainDefinition();

        // 登录、登出、未登录、无权限接口放行
        chainDefinition.addPathDefinition("/api/auth/login", "anon");
        chainDefinition.addPathDefinition("/api/auth/logout", "anon");
        chainDefinition.addPathDefinition("/api/auth/unauthorized", "anon");
        chainDefinition.addPathDefinition("/api/auth/forbidden", "anon");

        // 静态资源、接口文档按需放行
        chainDefinition.addPathDefinition("/favicon.ico", "anon");
        chainDefinition.addPathDefinition("/doc.html", "anon");
        chainDefinition.addPathDefinition("/swagger-ui/**", "anon");
        chainDefinition.addPathDefinition("/v3/api-docs/**", "anon");

        // 管理端接口示例：必须登录并具备 admin 角色
        chainDefinition.addPathDefinition("/api/admin/**", "authc, roles[admin]");

        // 用户模块示例：必须登录并具备 user:query 权限
        chainDefinition.addPathDefinition("/api/users/**", "authc, perms[user:query]");

        // 其他接口默认要求登录
        chainDefinition.addPathDefinition("/api/**", "authc");

        return chainDefinition;
    }

}
```

这类配置的关键点是：`SecurityManager` 由 Starter 创建，`Realm` 决定用户、角色、权限从哪里加载，`ShiroFilterChainDefinition` 决定 URL 是否匿名访问、是否登录访问、是否需要角色或权限访问。官方示例中的过滤链也采用 `/admin/** = authc, roles[admin]`、`/docs/** = authc, perms[document:read]` 这种写法。([Apache Shiro](https://shiro.apache.org/spring-boot.html))

### Realm 配置

`Realm` 是 Shiro 连接业务数据源的核心组件。认证阶段通过 `doGetAuthenticationInfo` 查询用户身份和密码摘要；授权阶段通过 `doGetAuthorizationInfo` 查询用户角色和权限。Shiro 官方文档也说明，最终由 `Realm` 与数据源通信，并决定角色和权限是否存在。([Apache Shiro](https://shiro.apache.org/java-authorization-guide.html))

先定义登录主体对象。该对象会放入 Shiro `Subject`，所以不要放密码、盐值等敏感字段。

文件位置：`src/main/java/io/github/atengk/shiro/module/auth/model/LoginPrincipal.java`

```java
package io.github.atengk.shiro.module.auth.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;

/**
 * 当前登录用户主体信息。
 *
 * @author Ateng
 * @since 2026-05-12
 */
@Getter
@AllArgsConstructor
public class LoginPrincipal implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long userId;

    private String username;

    private String nickname;

}
```

定义 Realm 查询用户时使用的用户安全模型。该模型只在服务端认证流程中使用，不直接返回给前端。

文件位置：`src/main/java/io/github/atengk/shiro/module/auth/model/SecurityUserProfile.java`

```java
package io.github.atengk.shiro.module.auth.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户认证安全信息。
 *
 * @author Ateng
 * @since 2026-05-12
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecurityUserProfile {

    private Long userId;

    private String username;

    private String nickname;

    /**
     * 数据库存储的密码摘要。
     */
    private String password;

    /**
     * 密码盐值。
     */
    private String passwordSalt;

    /**
     * 是否启用。
     */
    private Boolean enabled;

    /**
     * 是否锁定。
     */
    private Boolean locked;

}
```

定义授权信息模型，后续可以从用户角色表、角色权限表和用户直接权限表组合加载。

文件位置：`src/main/java/io/github/atengk/shiro/module/auth/model/SecurityPermissionProfile.java`

```java
package io.github.atengk.shiro.module.auth.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 用户角色与权限信息。
 *
 * @author Ateng
 * @since 2026-05-12
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecurityPermissionProfile {

    private List<String> roleCodes;

    private List<String> permissionCodes;

}
```

定义 Realm 使用的业务查询接口。具体实现可以通过 MyBatis-Plus 查询用户表、角色表和权限表。

文件位置：`src/main/java/io/github/atengk/shiro/module/auth/service/SecurityUserService.java`

```java
package io.github.atengk.shiro.module.auth.service;

import io.github.atengk.shiro.module.auth.model.SecurityPermissionProfile;
import io.github.atengk.shiro.module.auth.model.SecurityUserProfile;

/**
 * Shiro 用户安全信息查询服务。
 *
 * @author Ateng
 * @since 2026-05-12
 */
public interface SecurityUserService {

    SecurityUserProfile loadUserByUsername(String username);

    SecurityPermissionProfile loadPermissionByUserId(Long userId);

}
```

下面的 Realm 完成用户名密码认证和角色权限加载，是本节的核心类。

文件位置：`src/main/java/io/github/atengk/shiro/module/auth/realm/UserPasswordRealm.java`

```java
package io.github.atengk.shiro.module.auth.realm;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.shiro.module.auth.model.LoginPrincipal;
import io.github.atengk.shiro.module.auth.model.SecurityPermissionProfile;
import io.github.atengk.shiro.module.auth.model.SecurityUserProfile;
import io.github.atengk.shiro.module.auth.service.SecurityUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.DisabledAccountException;
import org.apache.shiro.authc.LockedAccountException;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authc.UnknownAccountException;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.lang.util.SimpleByteSource;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;

import java.util.HashSet;

/**
 * 用户名密码认证 Realm。
 *
 * @author Ateng
 * @since 2026-05-12
 */
@Slf4j
@RequiredArgsConstructor
public class UserPasswordRealm extends AuthorizingRealm {

    private static final String REALM_NAME = "userPasswordRealm";

    private final SecurityUserService securityUserService;

    @Override
    public String getName() {
        return REALM_NAME;
    }

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
        String username = Convert.toStr(token.getPrincipal());

        if (StrUtil.isBlank(username)) {
            throw new UnknownAccountException("用户名不能为空");
        }

        SecurityUserProfile userProfile = securityUserService.loadUserByUsername(username);
        if (ObjUtil.isNull(userProfile)) {
            log.warn("登录失败，用户不存在：{}", username);
            throw new UnknownAccountException("用户不存在");
        }

        if (BooleanUtil.isFalse(userProfile.getEnabled())) {
            log.warn("登录失败，用户已禁用：{}", username);
            throw new DisabledAccountException("用户已禁用");
        }

        if (BooleanUtil.isTrue(userProfile.getLocked())) {
            log.warn("登录失败，用户已锁定：{}", username);
            throw new LockedAccountException("用户已锁定");
        }

        LoginPrincipal principal = new LoginPrincipal(
                userProfile.getUserId(),
                userProfile.getUsername(),
                userProfile.getNickname()
        );

        return new SimpleAuthenticationInfo(
                principal,
                userProfile.getPassword(),
                new SimpleByteSource(userProfile.getPasswordSalt()),
                getName()
        );
    }

    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
        LoginPrincipal principal = (LoginPrincipal) principals.getPrimaryPrincipal();

        SecurityPermissionProfile permissionProfile = securityUserService.loadPermissionByUserId(principal.getUserId());
        SimpleAuthorizationInfo authorizationInfo = new SimpleAuthorizationInfo();

        if (ObjUtil.isNull(permissionProfile)) {
            log.warn("授权信息为空，用户ID：{}", principal.getUserId());
            return authorizationInfo;
        }

        if (CollUtil.isNotEmpty(permissionProfile.getRoleCodes())) {
            authorizationInfo.setRoles(new HashSet<>(permissionProfile.getRoleCodes()));
        }

        if (CollUtil.isNotEmpty(permissionProfile.getPermissionCodes())) {
            authorizationInfo.setStringPermissions(new HashSet<>(permissionProfile.getPermissionCodes()));
        }

        log.debug("加载用户权限完成，用户ID：{}，角色：{}，权限：{}",
                principal.getUserId(),
                permissionProfile.getRoleCodes(),
                permissionProfile.getPermissionCodes());

        return authorizationInfo;
    }

    public void clearAuthorizationCache(Long userId, String username, String nickname) {
        LoginPrincipal principal = new LoginPrincipal(userId, username, nickname);
        SimplePrincipalCollection principalCollection = new SimplePrincipalCollection(principal, getName());
        clearCachedAuthorizationInfo(principalCollection);
        log.info("清理用户授权缓存完成，用户ID：{}", userId);
    }

}
```

`SimpleAuthenticationInfo` 在 Shiro 2 中支持传入 principal、已加密凭证、盐值和 Realm 名称；盐值类型来自 `org.apache.shiro.lang.util.ByteSource`，示例中使用 `SimpleByteSource` 包装数据库保存的盐值。([Apache Shiro](https://shiro.apache.org/static/2.0.0/shiro-core/xref/org/apache/shiro/authc/SimpleAuthenticationInfo.html))

### ShiroFilterChain 配置

`ShiroFilterChainDefinition` 用于配置不同 URL 的访问规则。规则匹配顺序非常重要，应按照“公开接口优先、特殊接口其次、兜底规则最后”的顺序配置。

常用过滤器规则如下：

| 规则                | 说明                 | 示例                                       |
| ------------------- | -------------------- | ------------------------------------------ |
| `anon`              | 匿名访问，不要求登录 | `/api/auth/login = anon`                   |
| `authc`             | 必须完成认证登录     | `/api/** = authc`                          |
| `roles[admin]`      | 必须拥有指定角色     | `/api/admin/** = authc, roles[admin]`      |
| `perms[user:query]` | 必须拥有指定权限     | `/api/users/** = authc, perms[user:query]` |
| `logout`            | Shiro 内置登出过滤器 | `/logout = logout`                         |

建议项目初期采用“少量 URL 过滤链 + 方法注解”的组合方式：URL 过滤链负责是否需要登录，方法注解负责细粒度角色和权限。若接口量较大，也可以将菜单权限、按钮权限和 API 权限统一收敛到权限编码中。

示例配置已经在 `ShiroConfig#shiroFilterChainDefinition` 中给出。需要注意的是，Shiro 2.x 中 `shiro.allowAccessByDefault` 默认值为 `true`，即没有匹配到过滤链时默认允许访问；实际后端项目建议在 `application.yml` 中显式设置为 `false`，并通过 `/api/** = authc` 做兜底。Shiro 官方配置表说明该属性在 2.x 默认是 `true`，3.x 默认会变成 `false`。([Apache Shiro](https://shiro.apache.org/spring-boot.html))

### Session 与 RememberMe 配置

Shiro 默认支持有状态会话。用户登录成功后，Shiro 会通过 Session 保存 `PrincipalCollection` 和认证状态；RememberMe 则用于记住用户身份，但不等价于当前会话已认证。Shiro 官方认证文档明确区分 remembered 和 authenticated：`isRemembered()` 表示身份来自历史记忆，`isAuthenticated()` 才表示当前会话已经通过凭证认证。敏感接口应依赖 `isAuthenticated()`，不要仅依赖 RememberMe。([Apache Shiro](https://shiro.apache.org/authentication.html?utm_source=chatgpt.com))

文件位置：`src/main/resources/application.yml`

```yaml
shiro:
  enabled: true

  web:
    enabled: true

  annotations:
    enabled: true

  # 生产环境建议关闭默认放行，避免新增接口忘记配置过滤链
  allowAccessByDefault: false

  # 未登录和无权限地址。前后端分离项目后续可改为自定义 Filter 直接返回 JSON
  loginUrl: /api/auth/unauthorized
  unauthorizedUrl: /api/auth/forbidden

  sessionManager:
    # 删除无效 Session
    deleteInvalidSessions: true

    # 是否通过 Cookie 传递 Session ID
    sessionIdCookieEnabled: true

    # 禁止 URL 重写携带 Session ID，避免 URL 泄露会话标识
    sessionIdUrlRewritingEnabled: false

    cookie:
      # Session Cookie 名称，避免直接使用默认 JSESSIONID 暴露技术栈
      name: SHIRO_SID

      # -1 表示浏览器会话级 Cookie
      maxAge: -1

      # Cookie 路径
      path: /

      # 本地开发可为 false，生产 HTTPS 环境建议设置为 true
      secure: false

  rememberMeManager:
    cookie:
      # RememberMe Cookie 名称
      name: SHIRO_REMEMBER_ME

      # 示例设置 7 天，单位为秒
      maxAge: 604800

      path: /

      # 本地开发可为 false，生产 HTTPS 环境建议设置为 true
      secure: false
```

Shiro Spring Boot 配置表中提供了 `shiro.sessionManager.*` 与 `shiro.rememberMeManager.cookie.*` 相关配置项，包括 Session Cookie 名称、最大存活时间、是否启用 URL 重写、RememberMe Cookie 名称和存活时间等。([Apache Shiro](https://shiro.apache.org/spring-boot.html))

## 用户认证开发

本节实现用户名密码登录认证。认证流程由 Controller 接收登录参数，Service 构造 `UsernamePasswordToken` 并调用 `Subject.login(token)`，最终进入 `Realm#doGetAuthenticationInfo` 查询用户和校验密码。

### 登录接口设计

登录接口建议只负责参数接收、调用认证服务和返回登录结果，不在 Controller 中直接写 Shiro 认证细节。

请求模型定义如下。

文件位置：`src/main/java/io/github/atengk/shiro/module/auth/dto/LoginRequest.java`

```java
package io.github.atengk.shiro.module.auth.dto;

import lombok.Data;

/**
 * 登录请求参数。
 *
 * @author Ateng
 * @since 2026-05-12
 */
@Data
public class LoginRequest {

    private String username;

    private String password;

    private Boolean rememberMe;

}
```

登录响应模型定义如下。

文件位置：`src/main/java/io/github/atengk/shiro/module/auth/vo/LoginResponse.java`

```java
package io.github.atengk.shiro.module.auth.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 登录响应信息。
 *
 * @author Ateng
 * @since 2026-05-12
 */
@Data
@Builder
public class LoginResponse {

    private Long userId;

    private String username;

    private String nickname;

    private String sessionId;

    private List<String> roles;

    private List<String> permissions;

}
```

认证接口定义如下，示例中的 `Result` 为前文项目基础结构中预留的统一响应对象。

文件位置：`src/main/java/io/github/atengk/shiro/module/auth/controller/AuthController.java`

```java
package io.github.atengk.shiro.module.auth.controller;

import io.github.atengk.shiro.common.response.Result;
import io.github.atengk.shiro.module.auth.dto.LoginRequest;
import io.github.atengk.shiro.module.auth.service.AuthService;
import io.github.atengk.shiro.module.auth.vo.LoginResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 认证接口。
 *
 * @author Ateng
 * @since 2026-05-12
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody LoginRequest request) {
        return Result.success(authService.login(request));
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        authService.logout();
        return Result.success();
    }

    @GetMapping("/unauthorized")
    public Result<Void> unauthorized() {
        return Result.fail(401, "用户未登录");
    }

    @GetMapping("/forbidden")
    public Result<Void> forbidden() {
        return Result.fail(403, "权限不足");
    }

}
```

接口服务定义如下。

文件位置：`src/main/java/io/github/atengk/shiro/module/auth/service/AuthService.java`

```java
package io.github.atengk.shiro.module.auth.service;

import io.github.atengk.shiro.module.auth.dto.LoginRequest;
import io.github.atengk.shiro.module.auth.vo.LoginResponse;

/**
 * 认证业务服务。
 *
 * @author Ateng
 * @since 2026-05-12
 */
public interface AuthService {

    LoginResponse login(LoginRequest request);

    void logout();

}
```

认证服务实现通过 `SecurityUtils.getSubject()` 获取当前主体，然后调用 `login` 完成认证。

文件位置：`src/main/java/io/github/atengk/shiro/module/auth/service/impl/AuthServiceImpl.java`

```java
package io.github.atengk.shiro.module.auth.service.impl;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.shiro.module.auth.dto.LoginRequest;
import io.github.atengk.shiro.module.auth.model.LoginPrincipal;
import io.github.atengk.shiro.module.auth.model.SecurityPermissionProfile;
import io.github.atengk.shiro.module.auth.service.AuthService;
import io.github.atengk.shiro.module.auth.service.SecurityUserService;
import io.github.atengk.shiro.module.auth.vo.LoginResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.DisabledAccountException;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.LockedAccountException;
import org.apache.shiro.authc.UnknownAccountException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 认证业务服务实现。
 *
 * @author Ateng
 * @since 2026-05-12
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final SecurityUserService securityUserService;

    @Override
    public LoginResponse login(LoginRequest request) {
        if (StrUtil.hasBlank(request.getUsername(), request.getPassword())) {
            throw new IllegalArgumentException("用户名或密码不能为空");
        }

        Subject subject = SecurityUtils.getSubject();
        UsernamePasswordToken token = new UsernamePasswordToken(
                request.getUsername(),
                request.getPassword(),
                BooleanUtil.isTrue(request.getRememberMe())
        );

        try {
            subject.login(token);

            LoginPrincipal principal = (LoginPrincipal) subject.getPrincipal();
            SecurityPermissionProfile permissionProfile = securityUserService.loadPermissionByUserId(principal.getUserId());

            log.info("用户登录成功：{}", principal.getUsername());

            return LoginResponse.builder()
                    .userId(principal.getUserId())
                    .username(principal.getUsername())
                    .nickname(principal.getNickname())
                    .sessionId(Convert.toStr(subject.getSession().getId()))
                    .roles(permissionProfile == null ? List.of() : permissionProfile.getRoleCodes())
                    .permissions(permissionProfile == null ? List.of() : permissionProfile.getPermissionCodes())
                    .build();
        } catch (UnknownAccountException | IncorrectCredentialsException e) {
            log.warn("用户登录失败，用户名或密码错误：{}", request.getUsername());
            throw new AuthenticationException("用户名或密码错误");
        } catch (DisabledAccountException e) {
            log.warn("用户登录失败，账号已禁用：{}", request.getUsername());
            throw new AuthenticationException("账号已禁用");
        } catch (LockedAccountException e) {
            log.warn("用户登录失败，账号已锁定：{}", request.getUsername());
            throw new AuthenticationException("账号已锁定");
        } catch (AuthenticationException e) {
            log.warn("用户登录失败，认证异常：{}", request.getUsername());
            throw new AuthenticationException("登录认证失败");
        }
    }

    @Override
    public void logout() {
        Subject subject = SecurityUtils.getSubject();
        LoginPrincipal principal = (LoginPrincipal) subject.getPrincipal();

        subject.logout();

        if (principal != null) {
            log.info("用户退出登录：{}", principal.getUsername());
        }
    }

}
```

调用示例：

```bash
curl -X POST 'http://127.0.0.1:8080/api/auth/login' \
  -H 'Content-Type: application/json' \
  -d '{
    "username": "admin",
    "password": "123456",
    "rememberMe": true
  }'
```

### 用户身份校验流程

用户身份校验流程如下：

| 步骤 | 说明                                                         |
| ---- | ------------------------------------------------------------ |
| 1    | 前端提交用户名、密码和 RememberMe 标识                       |
| 2    | 后端创建 `UsernamePasswordToken`                             |
| 3    | 调用 `Subject.login(token)`                                  |
| 4    | Shiro 调用 `UserPasswordRealm#doGetAuthenticationInfo`       |
| 5    | Realm 查询用户状态、密码摘要和盐值                           |
| 6    | `HashedCredentialsMatcher` 对用户输入密码进行同算法加密并比较 |
| 7    | 比较成功则登录成功，失败则抛出认证异常                       |

Shiro 的 `HashedCredentialsMatcher` 会先对登录请求中的凭证进行散列，然后与数据源中已经散列存储的凭证进行比较；其文档也强调，账号盐值应来自已存储的账号信息，而不是用户提交数据。([Apache Shiro](https://shiro.apache.org/static/2.0.0/shiro-core/xref/org/apache/shiro/authc/credential/HashedCredentialsMatcher.html?utm_source=chatgpt.com))

### 密码加密与匹配

示例采用 `SHA-256 + 用户独立盐值 + 1024 次迭代`。Shiro Realm 文档提供了使用 `Sha256Hash`、随机盐和 1024 次迭代的示例，并说明需要将对应的 `HashedCredentialsMatcher` 配置到 Realm 上。([Apache Shiro](https://shiro.apache.org/realm.html?utm_source=chatgpt.com))

密码工具类用于注册用户、重置密码和测试初始化数据。

文件位置：`src/main/java/io/github/atengk/shiro/security/util/PasswordHashUtil.java`

```java
package io.github.atengk.shiro.security.util;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import org.apache.shiro.crypto.hash.Sha256Hash;
import org.apache.shiro.lang.util.SimpleByteSource;

/**
 * Shiro 密码摘要工具。
 *
 * @author Ateng
 * @since 2026-05-12
 */
public final class PasswordHashUtil {

    private static final int HASH_ITERATIONS = 1024;

    private PasswordHashUtil() {
    }

    public static String generateSalt() {
        return IdUtil.fastSimpleUUID();
    }

    public static String encryptPassword(String rawPassword, String salt) {
        if (StrUtil.hasBlank(rawPassword, salt)) {
            throw new IllegalArgumentException("密码或盐值不能为空");
        }

        return new Sha256Hash(rawPassword, new SimpleByteSource(salt), HASH_ITERATIONS).toHex();
    }

    public static PasswordPair encryptPassword(String rawPassword) {
        String salt = generateSalt();
        String password = encryptPassword(rawPassword, salt);
        return new PasswordPair(password, salt);
    }

    /**
     * 密码摘要和盐值。
     *
     * @author Ateng
     * @since 2026-05-12
     */
    public record PasswordPair(String password, String salt) {
    }

}
```

初始化用户时可以这样生成密码摘要：

```java
PasswordHashUtil.PasswordPair pair = PasswordHashUtil.encryptPassword("123456");

String password = pair.password();
String salt = pair.salt();
```

数据库中应保存 `password` 和 `salt` 两个字段。登录时，用户输入原始密码，Shiro 使用 Realm 返回的盐值和 `HashedCredentialsMatcher` 配置进行同算法比较，不需要业务代码手动比较密码。

### 登录成功与失败处理

登录成功后建议返回用户基础信息、角色、权限和 Session ID。前端如果使用 Cookie 会话，通常由浏览器自动携带 Cookie；如果后续改为前后端分离 Token 模式，则不再返回 Shiro Session ID，而是返回自定义 Token。

登录失败时建议统一转换为业务响应，不直接把 Shiro 异常栈返回给前端。

| 异常                            | 建议返回         |
| ------------------------------- | ---------------- |
| `UnknownAccountException`       | 用户名或密码错误 |
| `IncorrectCredentialsException` | 用户名或密码错误 |
| `DisabledAccountException`      | 账号已禁用       |
| `LockedAccountException`        | 账号已锁定       |
| `AuthenticationException`       | 登录认证失败     |

生产环境中，用户名不存在和密码错误建议统一返回“用户名或密码错误”，避免攻击者通过响应差异枚举账号。

## 用户授权开发

本节实现角色与权限模型设计，并通过 Realm 加载角色权限，最后使用 Shiro 注解进行接口级权限控制。Shiro 授权模型包含用户、角色和权限三个核心元素，权限是最小安全策略单元，角色则是权限集合，用于简化权限管理。([Apache Shiro](https://shiro.apache.org/java-authorization-guide.html))

### 角色模型设计

角色用于承载一组权限，通常不建议在代码里大量写死角色判断。更合理的方式是：角色作为权限集合的管理单位，接口最终尽量使用权限标识进行控制。

角色编码建议保持稳定、简短、语义明确：

| 角色编码         | 角色名称   | 说明                 |
| ---------------- | ---------- | -------------------- |
| `admin`          | 超级管理员 | 拥有系统管理能力     |
| `security_admin` | 安全管理员 | 管理用户、角色、权限 |
| `user_admin`     | 用户管理员 | 管理用户资料和状态   |
| `auditor`        | 审计员     | 查看日志、审计记录   |
| `normal_user`    | 普通用户   | 访问基础业务功能     |

角色表建议至少包含以下字段：

| 字段          | 类型     | 说明           |
| ------------- | -------- | -------------- |
| `id`          | bigint   | 主键           |
| `role_code`   | varchar  | 角色编码，唯一 |
| `role_name`   | varchar  | 角色名称       |
| `role_desc`   | varchar  | 角色说明       |
| `enabled`     | tinyint  | 是否启用       |
| `sort_order`  | int      | 排序           |
| `deleted`     | tinyint  | 逻辑删除       |
| `create_time` | datetime | 创建时间       |
| `update_time` | datetime | 更新时间       |

角色编码可用于 `@RequiresRoles("admin")`，但复杂业务中更推荐主要使用 `@RequiresPermissions`，让角色变更不影响代码结构。

### 权限模型设计

权限表示“能对什么资源做什么操作”。Shiro 官方文档说明，权限可以按资源、动作、实例等粒度设计，常见数据操作动作包括 create、read、update、delete。([Apache Shiro](https://shiro.apache.org/java-authorization-guide.html))

推荐权限编码格式：

```text
资源:操作
资源:操作:范围
```

示例：

| 权限编码               | 说明         |
| ---------------------- | ------------ |
| `user:query`           | 查询用户     |
| `user:create`          | 创建用户     |
| `user:update`          | 修改用户     |
| `user:delete`          | 删除用户     |
| `role:query`           | 查询角色     |
| `role:assign`          | 分配角色     |
| `permission:query`     | 查询权限     |
| `permission:assign`    | 分配权限     |
| `system:config:update` | 修改系统配置 |
| `audit:log:query`      | 查询审计日志 |

权限表建议至少包含以下字段：

| 字段              | 类型     | 说明                             |
| ----------------- | -------- | -------------------------------- |
| `id`              | bigint   | 主键                             |
| `permission_code` | varchar  | 权限编码，唯一                   |
| `permission_name` | varchar  | 权限名称                         |
| `permission_type` | varchar  | 权限类型，例如 menu、button、api |
| `parent_id`       | bigint   | 父权限 ID                        |
| `path`            | varchar  | 前端路由或后端接口路径           |
| `enabled`         | tinyint  | 是否启用                         |
| `sort_order`      | int      | 排序                             |
| `deleted`         | tinyint  | 逻辑删除                         |
| `create_time`     | datetime | 创建时间                         |
| `update_time`     | datetime | 更新时间                         |

权限粒度建议按后端接口能力设计，而不是完全按前端按钮设计。前端按钮权限可以复用后端权限编码，例如“新增用户”按钮和 `POST /api/users` 接口都绑定 `user:create`。

### 权限加载流程

权限加载发生在以下场景：

| 场景                            | 说明                                               |
| ------------------------------- | -------------------------------------------------- |
| 首次访问带权限注解的接口        | Shiro 调用 `doGetAuthorizationInfo` 加载角色和权限 |
| 调用 `subject.hasRole(...)`     | 触发角色判断                                       |
| 调用 `subject.isPermitted(...)` | 触发权限判断                                       |
| 权限缓存失效后再次访问          | 重新加载授权信息                                   |

标准加载流程如下：

```text
请求接口
  ↓
Shiro Filter 判断是否需要登录
  ↓
进入 Controller 方法前触发注解校验
  ↓
调用 Realm#doGetAuthorizationInfo
  ↓
根据 userId 查询角色编码和权限编码
  ↓
构造 SimpleAuthorizationInfo
  ↓
Shiro 判断角色或权限是否满足
```

`UserPasswordRealm#doGetAuthorizationInfo` 已经给出角色和权限加载逻辑。实际数据库实现中，`SecurityUserService#loadPermissionByUserId` 可以按以下逻辑查询：

```sql
-- 查询用户角色编码
SELECT r.role_code
FROM sys_user_role ur
JOIN sys_role r ON r.id = ur.role_id
WHERE ur.user_id = #{userId}
  AND r.enabled = 1
  AND r.deleted = 0;

-- 查询用户权限编码
SELECT DISTINCT p.permission_code
FROM sys_user_role ur
JOIN sys_role_permission rp ON rp.role_id = ur.role_id
JOIN sys_permission p ON p.id = rp.permission_id
WHERE ur.user_id = #{userId}
  AND p.enabled = 1
  AND p.deleted = 0;
```

当管理员修改某个用户的角色或权限后，应清理该用户的授权缓存。前面的 `UserPasswordRealm#clearAuthorizationCache` 已经提供了清理入口，可以在“分配角色”“分配权限”“禁用角色”“禁用权限”之后调用。

### 注解式权限控制

Shiro 注解适合做方法级权限控制。官方 Spring Boot 文档中说明，Starter 默认启用 `@RequiresRoles`、`@RequiresPermissions` 等注解，并且这些注解可用于 Controller 方法。([Apache Shiro](https://shiro.apache.org/spring-boot.html))

下面示例展示角色校验、权限校验和 OR 权限校验三种常用方式。

文件位置：`src/main/java/io/github/atengk/shiro/module/user/controller/UserManageController.java`

```java
package io.github.atengk.shiro.module.user.controller;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.shiro.common.response.Result;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户管理接口。
 *
 * @author Ateng
 * @since 2026-05-12
 */
@Slf4j
@RestController
@RequestMapping("/api/users")
public class UserManageController {

    @GetMapping
    @RequiresPermissions("user:query")
    public Result<List<String>> listUsers() {
        log.info("查询用户列表");
        return Result.success(List.of("admin", "test"));
    }

    @GetMapping("/{id}")
    @RequiresPermissions(value = {"user:query", "user:detail"}, logical = Logical.OR)
    public Result<String> getUser(@PathVariable Long id) {
        log.info("查询用户详情，用户ID：{}", id);
        return Result.success("用户ID：" + id);
    }

    @PostMapping
    @RequiresPermissions("user:create")
    public Result<String> createUser(@RequestParam String username) {
        if (StrUtil.isBlank(username)) {
            return Result.fail(400, "用户名不能为空");
        }

        log.info("创建用户：{}", username);
        return Result.success("创建成功");
    }

    @PutMapping("/{id}")
    @RequiresPermissions("user:update")
    public Result<String> updateUser(@PathVariable Long id, @RequestParam String nickname) {
        log.info("修改用户信息，用户ID：{}，昵称：{}", id, nickname);
        return Result.success("修改成功");
    }

    @DeleteMapping("/{id}")
    @RequiresRoles("admin")
    @RequiresPermissions("user:delete")
    public Result<String> deleteUser(@PathVariable Long id) {
        log.info("删除用户，用户ID：{}", id);
        return Result.success("删除成功");
    }

    @GetMapping("/profile")
    @RequiresAuthentication
    public Result<String> profile() {
        return Result.success("当前用户资料");
    }

}
```

常用注解说明：

| 注解                      | 说明                                        | 示例                                  |
| ------------------------- | ------------------------------------------- | ------------------------------------- |
| `@RequiresAuthentication` | 要求当前会话已认证，不只是 RememberMe       | 查看当前登录用户资料                  |
| `@RequiresUser`           | 要求当前主体存在，可以是已登录或 RememberMe | 普通个性化页面                        |
| `@RequiresRoles`          | 要求拥有指定角色                            | `@RequiresRoles("admin")`             |
| `@RequiresPermissions`    | 要求拥有指定权限                            | `@RequiresPermissions("user:create")` |
| `logical = Logical.OR`    | 多个角色或权限满足一个即可                  | 查询或详情权限满足任一即可            |

验证示例：

```bash
# 未登录访问，预期返回 401 或跳转到 shiro.loginUrl
curl -i 'http://127.0.0.1:8080/api/users'

# 登录后访问，若具备 user:query 权限，预期返回用户列表
curl -i 'http://127.0.0.1:8080/api/users' \
  -H 'Cookie: SHIRO_SID=实际登录后的Cookie值'

# 登录后创建用户，若缺少 user:create 权限，预期返回 403
curl -X POST 'http://127.0.0.1:8080/api/users?username=test01' \
  -H 'Cookie: SHIRO_SID=实际登录后的Cookie值'
```

这一阶段完成后，项目已经具备基本的 Shiro 配置、用户名密码登录、密码摘要匹配、角色权限加载和注解式接口授权能力。下一步通常继续补充异常统一处理、JSON 响应适配、数据库表结构和前后端分离 Token 认证。



## 与 Spring Boot 3 集成

本节将 Shiro 2 接入 Spring Boot 3 的 Web 请求链路，重点处理 Filter 注册、接口权限注解、异常统一转换和 JSON 响应适配。Shiro Web 应用的请求需要经过主 Shiro Filter，再根据 `ShiroFilterChainDefinition` 中的 URL 规则执行不同过滤链；Shiro Spring Boot Web Starter 也要求提供 `Realm` 和至少一个过滤链定义。([Apache Shiro](https://shiro.apache.org/spring-boot.html?utm_source=chatgpt.com))

### Filter 注册与请求拦截

在前后端分离项目中，推荐让 Filter 层只处理“是否登录”，细粒度角色和权限交给 Controller 方法上的 `@RequiresRoles`、`@RequiresPermissions` 注解处理。这样未登录可以由自定义 Filter 直接返回 JSON，权限不足则由统一异常处理器返回 JSON。

下面定义一个 JSON 认证过滤器。它继承 Shiro 的 `AccessControlFilter`，当用户未认证时直接返回 `401` JSON，不再跳转登录页面。

文件位置：`src/main/java/io/github/atengk/shiro/security/filter/JsonAuthenticationFilter.java`

```java
package io.github.atengk.shiro.security.filter;

import io.github.atengk.shiro.common.response.Result;
import io.github.atengk.shiro.security.util.JsonResponseUtil;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.web.filter.AccessControlFilter;

/**
 * JSON 认证过滤器。
 *
 * @author Ateng
 * @since 2026-05-12
 */
@Slf4j
public class JsonAuthenticationFilter extends AccessControlFilter {

    @Override
    protected boolean isAccessAllowed(ServletRequest request, ServletResponse response, Object mappedValue) {
        return getSubject(request, response).isAuthenticated();
    }

    @Override
    protected boolean onAccessDenied(ServletRequest request, ServletResponse response) {
        log.warn("请求未认证，已拦截访问：{}", getPathWithinApplication(request));
        JsonResponseUtil.writeJson(response, Result.fail(401, "用户未登录"));
        return false;
    }

}
```

下面的工具类用于在 Filter 中写出统一 JSON 响应，避免每个过滤器重复设置响应头和序列化逻辑。

文件位置：`src/main/java/io/github/atengk/shiro/security/util/JsonResponseUtil.java`

```java
package io.github.atengk.shiro.security.util;

import cn.hutool.json.JSONUtil;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * JSON 响应写出工具。
 *
 * @author Ateng
 * @since 2026-05-12
 */
@Slf4j
public final class JsonResponseUtil {

    private JsonResponseUtil() {
    }

    public static void writeJson(ServletResponse response, Object body) {
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        httpResponse.setStatus(HttpServletResponse.SC_OK);
        httpResponse.setCharacterEncoding(StandardCharsets.UTF_8.name());
        httpResponse.setContentType("application/json;charset=UTF-8");

        try {
            httpResponse.getWriter().write(JSONUtil.toJsonStr(body));
        } catch (IOException e) {
            log.error("写出 JSON 响应失败", e);
        }
    }

}
```

在 `ShiroConfig` 中注册自定义 Filter，并在过滤链中使用 `jsonAuthc`。Shiro 的 `ShiroFilterFactoryBean` 支持发现 Spring 容器中的 `Filter` Bean，并通过 Bean 名称在过滤链中引用；同时在 Spring Boot 中应禁用该自定义 Filter 的 Servlet 容器级自动注册，避免它绕过 Shiro 过滤链直接执行。([svn.apache.org](https://svn.apache.org/repos/asf/shiro/site/publish/static/1.2.2/apidocs/org/apache/shiro/spring/web/ShiroFilterFactoryBean.html?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/shiro/config/ShiroConfig.java`

```java
package io.github.atengk.shiro.config;

import io.github.atengk.shiro.security.filter.JsonAuthenticationFilter;
import jakarta.servlet.Filter;
import org.apache.shiro.spring.web.config.DefaultShiroFilterChainDefinition;
import org.apache.shiro.spring.web.config.ShiroFilterChainDefinition;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Apache Shiro 2 核心配置。
 *
 * @author Ateng
 * @since 2026-05-12
 */
@Configuration
public class ShiroConfig {

    @Bean(name = "jsonAuthc")
    public Filter jsonAuthenticationFilter() {
        return new JsonAuthenticationFilter();
    }

    @Bean
    public FilterRegistrationBean<Filter> jsonAuthcFilterRegistration(@Qualifier("jsonAuthc") Filter filter) {
        FilterRegistrationBean<Filter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(filter);

        // 只交给 Shiro 过滤链管理，不让 Servlet 容器直接注册执行
        registrationBean.setEnabled(false);
        return registrationBean;
    }

    @Bean
    public ShiroFilterChainDefinition shiroFilterChainDefinition() {
        DefaultShiroFilterChainDefinition chainDefinition = new DefaultShiroFilterChainDefinition();

        // 登录、登出、未登录、无权限接口放行
        chainDefinition.addPathDefinition("/api/auth/login", "anon");
        chainDefinition.addPathDefinition("/api/auth/logout", "anon");
        chainDefinition.addPathDefinition("/api/auth/unauthorized", "anon");
        chainDefinition.addPathDefinition("/api/auth/forbidden", "anon");

        // 接口文档和静态资源放行
        chainDefinition.addPathDefinition("/favicon.ico", "anon");
        chainDefinition.addPathDefinition("/doc.html", "anon");
        chainDefinition.addPathDefinition("/swagger-ui/**", "anon");
        chainDefinition.addPathDefinition("/v3/api-docs/**", "anon");

        // API 接口统一要求登录，细粒度权限交给 Controller 注解控制
        chainDefinition.addPathDefinition("/api/**", "jsonAuthc");

        return chainDefinition;
    }

}
```

这种写法下，请求拦截顺序如下：

| 顺序 | 处理点          | 说明                                               |
| ---- | --------------- | -------------------------------------------------- |
| 1    | Servlet 容器    | 请求进入 Spring Boot Web 应用                      |
| 2    | Shiro 主过滤器  | 根据 URL 匹配过滤链                                |
| 3    | `jsonAuthc`     | 判断当前 `Subject` 是否已认证                      |
| 4    | Spring MVC      | 进入 Controller                                    |
| 5    | Shiro 注解 AOP  | 执行 `@RequiresPermissions`、`@RequiresRoles` 校验 |
| 6    | Controller 方法 | 执行业务逻辑                                       |

### Controller 权限控制

Controller 层负责细粒度权限控制。Shiro Spring Boot Starter 默认启用 `@RequiresRoles`、`@RequiresPermissions` 等注解，并支持在 `@Controller` 或 `@RestController` 类中使用这些注解。([Apache Shiro](https://shiro.apache.org/spring-boot.html?utm_source=chatgpt.com))

下面示例给出系统管理接口的权限控制方式。

文件位置：`src/main/java/io/github/atengk/shiro/module/system/controller/SystemConfigController.java`

```java
package io.github.atengk.shiro.module.system.controller;

import io.github.atengk.shiro.common.response.Result;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.springframework.web.bind.annotation.*;

/**
 * 系统配置接口。
 *
 * @author Ateng
 * @since 2026-05-12
 */
@Slf4j
@RestController
@RequestMapping("/api/system/config")
public class SystemConfigController {

    @GetMapping
    @RequiresAuthentication
    @RequiresPermissions("system:config:query")
    public Result<String> getConfig() {
        log.info("查询系统配置");
        return Result.success("系统配置内容");
    }

    @PutMapping
    @RequiresRoles("admin")
    @RequiresPermissions("system:config:update")
    public Result<Void> updateConfig(@RequestParam String configValue) {
        log.info("修改系统配置，配置值：{}", configValue);
        return Result.success();
    }

}
```

权限控制建议采用以下原则：

| 场景           | 推荐方式                                                    |
| -------------- | ----------------------------------------------------------- |
| 只要求登录     | `@RequiresAuthentication`                                   |
| 要求指定权限   | `@RequiresPermissions("user:query")`                        |
| 要求管理员角色 | `@RequiresRoles("admin")`                                   |
| 多权限任一满足 | `@RequiresPermissions(value = {...}, logical = Logical.OR)` |
| 高风险接口     | 同时使用角色和权限                                          |

Shiro 的授权模型中，权限是安全策略的最小行为描述，角色通常是一组权限的集合；因此业务接口优先使用权限编码控制，角色更适合用于后台管理入口或高风险接口。([Apache Shiro](https://shiro.apache.org/authorization.html?utm_source=chatgpt.com))

### 异常统一处理

Shiro 注解校验失败、登录失败、参数错误和系统异常都应转换成统一 JSON 响应。Spring MVC 的 `@RestControllerAdvice` 是 `@ControllerAdvice` 与 `@ResponseBody` 的组合，可以让异常处理方法直接返回响应体，并应用到多个 Controller。([Home](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-advice.html?utm_source=chatgpt.com))

先补充统一响应对象。

文件位置：`src/main/java/io/github/atengk/shiro/common/response/Result.java`

```java
package io.github.atengk.shiro.common.response;

import cn.hutool.core.date.DateUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一接口响应对象。
 *
 * @author Ateng
 * @since 2026-05-12
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

    private Integer code;

    private String message;

    private T data;

    private Long timestamp;

    public static Result<Void> success() {
        return Result.<Void>builder()
                .code(200)
                .message("操作成功")
                .timestamp(DateUtil.current())
                .build();
    }

    public static <T> Result<T> success(T data) {
        return Result.<T>builder()
                .code(200)
                .message("操作成功")
                .data(data)
                .timestamp(DateUtil.current())
                .build();
    }

    public static <T> Result<T> fail(Integer code, String message) {
        return Result.<T>builder()
                .code(code)
                .message(message)
                .timestamp(DateUtil.current())
                .build();
    }

}
```

下面定义全局异常处理器，统一处理未登录、权限不足、认证失败和普通业务异常。

文件位置：`src/main/java/io/github/atengk/shiro/common/exception/GlobalExceptionHandler.java`

```java
package io.github.atengk.shiro.common.exception;

import io.github.atengk.shiro.common.response.Result;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器。
 *
 * @author Ateng
 * @since 2026-05-12
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UnauthenticatedException.class)
    public Result<Void> handleUnauthenticatedException(UnauthenticatedException e) {
        log.warn("用户未登录：{}", e.getMessage());
        return Result.fail(401, "用户未登录");
    }

    @ExceptionHandler(UnauthorizedException.class)
    public Result<Void> handleUnauthorizedException(UnauthorizedException e) {
        log.warn("权限不足：{}", e.getMessage());
        return Result.fail(403, "权限不足");
    }

    @ExceptionHandler(AuthorizationException.class)
    public Result<Void> handleAuthorizationException(AuthorizationException e) {
        log.warn("授权失败：{}", e.getMessage());
        return Result.fail(403, "权限不足");
    }

    @ExceptionHandler(AuthenticationException.class)
    public Result<Void> handleAuthenticationException(AuthenticationException e) {
        log.warn("认证失败：{}", e.getMessage());
        return Result.fail(401, e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Result<Void> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("请求参数错误：{}", e.getMessage());
        return Result.fail(400, e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return Result.fail(500, "系统异常");
    }

}
```

### JSON 响应适配

Shiro 默认更偏向传统 Web 应用，未登录时常见行为是跳转到登录地址；前后端分离项目应统一返回 JSON。JSON 响应适配主要覆盖三类场景：

| 场景            | 处理方式                                                |
| --------------- | ------------------------------------------------------- |
| Filter 层未登录 | `JsonAuthenticationFilter` 直接返回 `401` JSON          |
| 注解层权限不足  | `GlobalExceptionHandler` 捕获 `AuthorizationException`  |
| 业务登录失败    | `GlobalExceptionHandler` 捕获 `AuthenticationException` |

建议统一响应格式如下：

```json
{
  "code": 401,
  "message": "用户未登录",
  "data": null,
  "timestamp": 1778572800000
}
```

前端可按 `code` 做统一处理：

| code | 含义             | 前端处理                     |
| ---- | ---------------- | ---------------------------- |
| 200  | 请求成功         | 正常渲染                     |
| 400  | 参数错误         | 提示用户修正参数             |
| 401  | 未登录或登录失效 | 清理本地用户状态并跳转登录页 |
| 403  | 权限不足         | 展示无权限提示               |
| 500  | 系统异常         | 展示系统错误提示             |

## 数据库设计

本节设计用户、角色、权限以及关联表。Shiro 本身不限制数据库模型，权限如何授予用户由业务系统决定；常见做法是用户关联角色，角色关联权限，最终由 Realm 查询当前用户的角色编码和权限编码。([Apache Shiro](https://shiro.apache.org/authorization.html?utm_source=chatgpt.com))

### 用户表设计

用户表保存账号基础信息、密码摘要、密码盐值和账号状态。密码字段不保存明文，只保存前文 `PasswordHashUtil` 生成的摘要。

```sql
CREATE TABLE `sys_user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` varchar(64) NOT NULL COMMENT '用户名',
  `nickname` varchar(64) DEFAULT NULL COMMENT '昵称',
  `password` varchar(128) NOT NULL COMMENT '密码摘要',
  `password_salt` varchar(64) NOT NULL COMMENT '密码盐值',
  `email` varchar(128) DEFAULT NULL COMMENT '邮箱',
  `mobile` varchar(32) DEFAULT NULL COMMENT '手机号',
  `enabled` tinyint NOT NULL DEFAULT 1 COMMENT '是否启用：0否，1是',
  `locked` tinyint NOT NULL DEFAULT 0 COMMENT '是否锁定：0否，1是',
  `last_login_time` datetime DEFAULT NULL COMMENT '最后登录时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常，1删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_user_username` (`username`),
  KEY `idx_sys_user_enabled` (`enabled`),
  KEY `idx_sys_user_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';
```

字段说明：

| 字段            | 说明               |
| --------------- | ------------------ |
| `username`      | 登录账号，必须唯一 |
| `password`      | 加密后的密码摘要   |
| `password_salt` | 用户独立盐值       |
| `enabled`       | 是否允许登录       |
| `locked`        | 是否锁定账号       |
| `deleted`       | 逻辑删除标识       |

### 角色表设计

角色表保存角色编码和角色名称。角色编码会加载到 Shiro 的 `SimpleAuthorizationInfo#setRoles` 中，可用于 `@RequiresRoles` 校验。

```sql
CREATE TABLE `sys_role` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `role_code` varchar(64) NOT NULL COMMENT '角色编码',
  `role_name` varchar(64) NOT NULL COMMENT '角色名称',
  `role_desc` varchar(255) DEFAULT NULL COMMENT '角色描述',
  `enabled` tinyint NOT NULL DEFAULT 1 COMMENT '是否启用：0否，1是',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序号',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常，1删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_role_code` (`role_code`),
  KEY `idx_sys_role_enabled` (`enabled`),
  KEY `idx_sys_role_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统角色表';
```

推荐角色编码：

| 角色编码         | 说明       |
| ---------------- | ---------- |
| `admin`          | 超级管理员 |
| `security_admin` | 安全管理员 |
| `user_admin`     | 用户管理员 |
| `auditor`        | 审计员     |
| `normal_user`    | 普通用户   |

### 权限表设计

权限表保存权限编码、权限类型、接口路径和前端路由等信息。Shiro 权限字符串通常按“资源:操作”设计，例如 `user:query`、`user:create`、`system:config:update`。Shiro 官方也将权限描述为安全策略中最原子的行为声明，通常由资源和动作组成。([Apache Shiro](https://shiro.apache.org/authorization.html?utm_source=chatgpt.com))

```sql
CREATE TABLE `sys_permission` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `permission_code` varchar(128) NOT NULL COMMENT '权限编码',
  `permission_name` varchar(64) NOT NULL COMMENT '权限名称',
  `permission_type` varchar(32) NOT NULL COMMENT '权限类型：menu菜单，button按钮，api接口',
  `parent_id` bigint NOT NULL DEFAULT 0 COMMENT '父权限ID',
  `path` varchar(255) DEFAULT NULL COMMENT '前端路由或后端接口路径',
  `http_method` varchar(16) DEFAULT NULL COMMENT '请求方法：GET、POST、PUT、DELETE',
  `enabled` tinyint NOT NULL DEFAULT 1 COMMENT '是否启用：0否，1是',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序号',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常，1删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_permission_code` (`permission_code`),
  KEY `idx_sys_permission_parent_id` (`parent_id`),
  KEY `idx_sys_permission_type` (`permission_type`),
  KEY `idx_sys_permission_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统权限表';
```

推荐权限编码：

| 权限编码               | 说明         |
| ---------------------- | ------------ |
| `user:query`           | 查询用户     |
| `user:create`          | 创建用户     |
| `user:update`          | 修改用户     |
| `user:delete`          | 删除用户     |
| `role:query`           | 查询角色     |
| `role:assign`          | 分配角色     |
| `permission:query`     | 查询权限     |
| `permission:assign`    | 分配权限     |
| `system:config:query`  | 查询系统配置 |
| `system:config:update` | 修改系统配置 |

### 用户角色权限关联表设计

用户、角色、权限之间建议采用两张关联表：`sys_user_role` 和 `sys_role_permission`。如果系统需要给用户直接分配特殊权限，可以额外增加 `sys_user_permission`，但常规 RBAC 模型中不建议一开始就引入，避免权限来源过多。

用户角色关联表：

```sql
CREATE TABLE `sys_user_role` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_user_role` (`user_id`, `role_id`),
  KEY `idx_sys_user_role_user_id` (`user_id`),
  KEY `idx_sys_user_role_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';
```

角色权限关联表：

```sql
CREATE TABLE `sys_role_permission` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `permission_id` bigint NOT NULL COMMENT '权限ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_role_permission` (`role_id`, `permission_id`),
  KEY `idx_sys_role_permission_role_id` (`role_id`),
  KEY `idx_sys_role_permission_permission_id` (`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限关联表';
```

Realm 加载角色和权限时可以使用以下 SQL：

```sql
-- 查询用户角色编码
SELECT r.role_code
FROM sys_user_role ur
JOIN sys_role r ON r.id = ur.role_id
WHERE ur.user_id = #{userId}
  AND r.enabled = 1
  AND r.deleted = 0;

-- 查询用户权限编码
SELECT DISTINCT p.permission_code
FROM sys_user_role ur
JOIN sys_role r ON r.id = ur.role_id
JOIN sys_role_permission rp ON rp.role_id = r.id
JOIN sys_permission p ON p.id = rp.permission_id
WHERE ur.user_id = #{userId}
  AND r.enabled = 1
  AND r.deleted = 0
  AND p.enabled = 1
  AND p.deleted = 0;
```

## 接口开发

本节补充登录、登出、用户信息和权限校验接口。接口设计保持 REST 风格，统一使用 `/api/auth` 前缀，响应结构统一使用 `Result<T>`。

### 登录接口

登录接口用于提交用户名、密码和 RememberMe 标识。认证成功后返回用户基础信息、角色、权限和 Session ID；认证失败时由全局异常处理器返回 `401`。

接口说明：

| 项目         | 内容               |
| ------------ | ------------------ |
| 请求路径     | `/api/auth/login`  |
| 请求方法     | `POST`             |
| 是否登录     | 否                 |
| 权限要求     | 无                 |
| Content-Type | `application/json` |

请求示例：

```json
{
  "username": "admin",
  "password": "123456",
  "rememberMe": true
}
```

响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "userId": 1,
    "username": "admin",
    "nickname": "管理员",
    "sessionId": "8f1bb0f0-6bc7-4a8d-9af3-c5b7f1a2a9d1",
    "roles": ["admin"],
    "permissions": ["user:query", "user:create", "system:config:update"]
  },
  "timestamp": 1778572800000
}
```

调用示例：

```bash
curl -X POST 'http://127.0.0.1:8080/api/auth/login' \
  -H 'Content-Type: application/json' \
  -d '{
    "username": "admin",
    "password": "123456",
    "rememberMe": true
  }'
```

### 登出接口

登出接口用于清理当前用户的 Shiro 会话。Cookie 会话模式下，服务端调用 `Subject.logout()` 后，当前会话中的认证状态会被清除。

接口说明：

| 项目     | 内容                       |
| -------- | -------------------------- |
| 请求路径 | `/api/auth/logout`         |
| 请求方法 | `POST`                     |
| 是否登录 | 建议否，接口内部兼容未登录 |
| 权限要求 | 无                         |

调用示例：

```bash
curl -X POST 'http://127.0.0.1:8080/api/auth/logout' \
  -H 'Cookie: SHIRO_SID=实际登录后的Cookie值'
```

响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": null,
  "timestamp": 1778572800000
}
```

### 用户信息接口

用户信息接口用于返回当前登录用户的基础资料、角色和权限。前端通常在刷新页面、进入后台首页或恢复登录态时调用该接口。

先定义响应对象。

文件位置：`src/main/java/io/github/atengk/shiro/module/auth/vo/UserInfoResponse.java`

```java
package io.github.atengk.shiro.module.auth.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 当前用户信息响应。
 *
 * @author Ateng
 * @since 2026-05-12
 */
@Data
@Builder
public class UserInfoResponse {

    private Long userId;

    private String username;

    private String nickname;

    private List<String> roles;

    private List<String> permissions;

}
```

扩展认证服务接口。

文件位置：`src/main/java/io/github/atengk/shiro/module/auth/service/AuthService.java`

```java
package io.github.atengk.shiro.module.auth.service;

import io.github.atengk.shiro.module.auth.dto.LoginRequest;
import io.github.atengk.shiro.module.auth.vo.LoginResponse;
import io.github.atengk.shiro.module.auth.vo.UserInfoResponse;

/**
 * 认证业务服务。
 *
 * @author Ateng
 * @since 2026-05-12
 */
public interface AuthService {

    LoginResponse login(LoginRequest request);

    void logout();

    UserInfoResponse currentUser();

    Boolean checkPermission(String permissionCode);

}
```

在认证服务实现类中增加当前用户信息和权限校验逻辑。

文件位置：`src/main/java/io/github/atengk/shiro/module/auth/service/impl/AuthServiceImpl.java`

```java
package io.github.atengk.shiro.module.auth.service.impl;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.shiro.module.auth.model.LoginPrincipal;
import io.github.atengk.shiro.module.auth.model.SecurityPermissionProfile;
import io.github.atengk.shiro.module.auth.service.AuthService;
import io.github.atengk.shiro.module.auth.service.SecurityUserService;
import io.github.atengk.shiro.module.auth.vo.UserInfoResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.subject.Subject;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 认证业务服务实现。
 *
 * @author Ateng
 * @since 2026-05-12
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final SecurityUserService securityUserService;

    @Override
    public UserInfoResponse currentUser() {
        Subject subject = SecurityUtils.getSubject();
        if (!subject.isAuthenticated()) {
            throw new UnauthenticatedException("用户未登录");
        }

        LoginPrincipal principal = (LoginPrincipal) subject.getPrincipal();
        SecurityPermissionProfile permissionProfile = securityUserService.loadPermissionByUserId(principal.getUserId());

        log.info("查询当前用户信息，用户ID：{}", principal.getUserId());

        return UserInfoResponse.builder()
                .userId(principal.getUserId())
                .username(principal.getUsername())
                .nickname(principal.getNickname())
                .roles(permissionProfile == null ? List.of() : permissionProfile.getRoleCodes())
                .permissions(permissionProfile == null ? List.of() : permissionProfile.getPermissionCodes())
                .build();
    }

    @Override
    public Boolean checkPermission(String permissionCode) {
        if (StrUtil.isBlank(permissionCode)) {
            throw new IllegalArgumentException("权限编码不能为空");
        }

        Subject subject = SecurityUtils.getSubject();
        if (!subject.isAuthenticated()) {
            throw new UnauthenticatedException("用户未登录");
        }

        boolean permitted = subject.isPermitted(permissionCode);
        log.info("校验用户权限，权限编码：{}，结果：{}", permissionCode, permitted);
        return permitted;
    }

}
```

接口说明：

| 项目     | 内容           |
| -------- | -------------- |
| 请求路径 | `/api/auth/me` |
| 请求方法 | `GET`          |
| 是否登录 | 是             |
| 权限要求 | 已认证         |

调用示例：

```bash
curl -X GET 'http://127.0.0.1:8080/api/auth/me' \
  -H 'Cookie: SHIRO_SID=实际登录后的Cookie值'
```

### 权限校验接口

权限校验接口用于前端在渲染按钮、菜单或动态操作入口时判断当前用户是否具备某个权限。后端接口自身仍必须使用注解或过滤链做权限控制，不能只依赖前端校验。

定义请求对象。

文件位置：`src/main/java/io/github/atengk/shiro/module/auth/dto/PermissionCheckRequest.java`

```java
package io.github.atengk.shiro.module.auth.dto;

import lombok.Data;

/**
 * 权限校验请求参数。
 *
 * @author Ateng
 * @since 2026-05-12
 */
@Data
public class PermissionCheckRequest {

    private String permissionCode;

}
```

在 `AuthController` 中补充用户信息和权限校验接口。

文件位置：`src/main/java/io/github/atengk/shiro/module/auth/controller/AuthController.java`

```java
package io.github.atengk.shiro.module.auth.controller;

import io.github.atengk.shiro.common.response.Result;
import io.github.atengk.shiro.module.auth.dto.LoginRequest;
import io.github.atengk.shiro.module.auth.dto.PermissionCheckRequest;
import io.github.atengk.shiro.module.auth.service.AuthService;
import io.github.atengk.shiro.module.auth.vo.LoginResponse;
import io.github.atengk.shiro.module.auth.vo.UserInfoResponse;
import lombok.RequiredArgsConstructor;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.springframework.web.bind.annotation.*;

/**
 * 认证接口。
 *
 * @author Ateng
 * @since 2026-05-12
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody LoginRequest request) {
        return Result.success(authService.login(request));
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        authService.logout();
        return Result.success();
    }

    @GetMapping("/me")
    @RequiresAuthentication
    public Result<UserInfoResponse> currentUser() {
        return Result.success(authService.currentUser());
    }

    @PostMapping("/permission/check")
    @RequiresAuthentication
    public Result<Boolean> checkPermission(@RequestBody PermissionCheckRequest request) {
        return Result.success(authService.checkPermission(request.getPermissionCode()));
    }

    @GetMapping("/unauthorized")
    public Result<Void> unauthorized() {
        return Result.fail(401, "用户未登录");
    }

    @GetMapping("/forbidden")
    public Result<Void> forbidden() {
        return Result.fail(403, "权限不足");
    }

}
```

权限校验接口说明：

| 项目     | 内容                         |
| -------- | ---------------------------- |
| 请求路径 | `/api/auth/permission/check` |
| 请求方法 | `POST`                       |
| 是否登录 | 是                           |
| 权限要求 | 已认证                       |

请求示例：

```json
{
  "permissionCode": "user:create"
}
```

响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": true,
  "timestamp": 1778572800000
}
```

调用示例：

```bash
curl -X POST 'http://127.0.0.1:8080/api/auth/permission/check' \
  -H 'Content-Type: application/json' \
  -H 'Cookie: SHIRO_SID=实际登录后的Cookie值' \
  -d '{
    "permissionCode": "user:create"
  }'
```

完成本节后，项目已经具备 Spring Boot 3 Web 请求拦截、Controller 注解权限控制、统一 JSON 异常响应、RBAC 数据库结构，以及认证相关基础接口。下一步可以继续展开“前后端分离适配”，将 Cookie Session 模式替换为 Token 模式，并实现自定义 `AuthenticationToken` 与无状态 Filter。



## 前后端分离适配

前后端分离项目通常不依赖浏览器 Cookie Session 保存登录态，而是由后端签发 Token，前端在后续请求中通过 `Authorization` 请求头携带 Token。Shiro 仍然负责认证、授权和注解权限控制，但认证凭证从用户名密码切换为 Token。

本节采用“不使用 Shiro Session、后端保存 Token、请求头携带 Token”的方案。示例使用内存 Token Store，适合开发和演示；生产环境建议替换为 Redis，并设置过期时间、续期策略和踢人逻辑。

### Token 认证方案

Token 认证流程如下：

```text
用户提交用户名密码
  ↓
后端校验账号、密码、账号状态
  ↓
生成 accessToken
  ↓
将 accessToken 与用户信息写入 TokenStore
  ↓
前端保存 accessToken
  ↓
后续请求通过 Authorization: Bearer <token> 携带凭证
  ↓
Shiro 自定义 Filter 解析 Token
  ↓
构造自定义 AuthenticationToken
  ↓
TokenRealm 校验 Token 并加载当前用户
  ↓
Controller 注解继续做角色和权限校验
```

推荐请求头格式如下：

```http
Authorization: Bearer 8f1bb0f06bc74a8d9af3c5b7f1a2a9d1
```

Token 登录响应建议返回以下字段：

| 字段          | 说明                 |
| ------------- | -------------------- |
| `accessToken` | 访问令牌             |
| `tokenType`   | 固定为 `Bearer`      |
| `expiresIn`   | Token 有效期，单位秒 |
| `userId`      | 用户 ID              |
| `username`    | 用户名               |
| `nickname`    | 昵称                 |
| `roles`       | 当前用户角色编码     |
| `permissions` | 当前用户权限编码     |

下面定义 Token 会话信息，用于保存 Token 和用户的绑定关系。

文件位置：`src/main/java/io/github/atengk/shiro/security/token/TokenSessionInfo.java`

```java
package io.github.atengk.shiro.security.token;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Token 会话信息。
 *
 * @author Ateng
 * @since 2026-05-12
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenSessionInfo {

    private Long userId;

    private String username;

    private String nickname;

    /**
     * 过期时间戳，单位毫秒。
     */
    private Long expireAt;

}
```

下面定义 Token 存储接口，生产环境可以使用 Redis 实现该接口。

文件位置：`src/main/java/io/github/atengk/shiro/security/token/TokenStore.java`

```java
package io.github.atengk.shiro.security.token;

/**
 * Token 存储接口。
 *
 * @author Ateng
 * @since 2026-05-12
 */
public interface TokenStore {

    String createToken(TokenSessionInfo sessionInfo, long expireSeconds);

    TokenSessionInfo getToken(String token);

    void removeToken(String token);

    void refreshToken(String token, long expireSeconds);

}
```

下面是内存版 Token 存储实现，仅用于开发环境和单机验证。

文件位置：`src/main/java/io/github/atengk/shiro/security/token/MemoryTokenStore.java`

```java
package io.github.atengk.shiro.security.token;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存 Token 存储实现。
 *
 * @author Ateng
 * @since 2026-05-12
 */
@Slf4j
@Component
public class MemoryTokenStore implements TokenStore {

    private final Map<String, TokenSessionInfo> tokenMap = new ConcurrentHashMap<>();

    @Override
    public String createToken(TokenSessionInfo sessionInfo, long expireSeconds) {
        String token = IdUtil.fastSimpleUUID();
        sessionInfo.setExpireAt(DateUtil.current() + expireSeconds * 1000);
        tokenMap.put(token, sessionInfo);
        log.info("创建用户 Token，用户ID：{}，过期秒数：{}", sessionInfo.getUserId(), expireSeconds);
        return token;
    }

    @Override
    public TokenSessionInfo getToken(String token) {
        TokenSessionInfo sessionInfo = tokenMap.get(token);
        if (ObjUtil.isNull(sessionInfo)) {
            return null;
        }

        if (sessionInfo.getExpireAt() < DateUtil.current()) {
            tokenMap.remove(token);
            log.warn("Token 已过期，用户ID：{}", sessionInfo.getUserId());
            return null;
        }

        return sessionInfo;
    }

    @Override
    public void removeToken(String token) {
        TokenSessionInfo sessionInfo = tokenMap.remove(token);
        if (ObjUtil.isNotNull(sessionInfo)) {
            log.info("移除用户 Token，用户ID：{}", sessionInfo.getUserId());
        }
    }

    @Override
    public void refreshToken(String token, long expireSeconds) {
        TokenSessionInfo sessionInfo = tokenMap.get(token);
        if (ObjUtil.isNull(sessionInfo)) {
            return;
        }

        sessionInfo.setExpireAt(DateUtil.current() + expireSeconds * 1000);
        log.debug("刷新用户 Token 有效期，用户ID：{}，过期秒数：{}", sessionInfo.getUserId(), expireSeconds);
    }

}
```

登录响应对象调整为 Token 模式。

文件位置：`src/main/java/io/github/atengk/shiro/module/auth/vo/TokenLoginResponse.java`

```java
package io.github.atengk.shiro.module.auth.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Token 登录响应。
 *
 * @author Ateng
 * @since 2026-05-12
 */
@Data
@Builder
public class TokenLoginResponse {

    private String accessToken;

    private String tokenType;

    private Long expiresIn;

    private Long userId;

    private String username;

    private String nickname;

    private List<String> roles;

    private List<String> permissions;

}
```

### 自定义 AuthenticationToken

Shiro 的 `AuthenticationToken` 表示一次认证请求提交的身份和凭证。用户名密码登录时可以使用 `UsernamePasswordToken`，Token 模式则需要自定义一个 `AuthenticationToken`，让 Shiro 能识别请求头中的访问令牌。

下面定义 Token 认证凭证。

文件位置：`src/main/java/io/github/atengk/shiro/security/token/BearerAuthenticationToken.java`

```java
package io.github.atengk.shiro.security.token;

import org.apache.shiro.authc.AuthenticationToken;

import java.io.Serial;

/**
 * Bearer Token 认证凭证。
 *
 * @author Ateng
 * @since 2026-05-12
 */
public class BearerAuthenticationToken implements AuthenticationToken {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String token;

    public BearerAuthenticationToken(String token) {
        this.token = token;
    }

    @Override
    public Object getPrincipal() {
        return token;
    }

    @Override
    public Object getCredentials() {
        return token;
    }

}
```

下面定义 Token 登录主体，后续可直接从 `Subject#getPrincipal()` 获取当前用户。

文件位置：`src/main/java/io/github/atengk/shiro/security/token/TokenPrincipal.java`

```java
package io.github.atengk.shiro.security.token;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;

/**
 * Token 登录用户主体。
 *
 * @author Ateng
 * @since 2026-05-12
 */
@Getter
@AllArgsConstructor
public class TokenPrincipal implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long userId;

    private String username;

    private String nickname;

    private String token;

}
```

下面定义 Token Realm。它只处理 `BearerAuthenticationToken`，不处理用户名密码登录。

文件位置：`src/main/java/io/github/atengk/shiro/security/realm/BearerTokenRealm.java`

```java
package io.github.atengk.shiro.security.realm;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import io.github.atengk.shiro.module.auth.model.SecurityPermissionProfile;
import io.github.atengk.shiro.module.auth.service.SecurityUserService;
import io.github.atengk.shiro.security.token.BearerAuthenticationToken;
import io.github.atengk.shiro.security.token.TokenPrincipal;
import io.github.atengk.shiro.security.token.TokenSessionInfo;
import io.github.atengk.shiro.security.token.TokenStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;

import java.util.HashSet;

/**
 * Bearer Token 认证 Realm。
 *
 * @author Ateng
 * @since 2026-05-12
 */
@Slf4j
@RequiredArgsConstructor
public class BearerTokenRealm extends AuthorizingRealm {

    private static final String REALM_NAME = "bearerTokenRealm";

    private final TokenStore tokenStore;

    private final SecurityUserService securityUserService;

    @Override
    public String getName() {
        return REALM_NAME;
    }

    @Override
    public boolean supports(AuthenticationToken token) {
        return token instanceof BearerAuthenticationToken;
    }

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken authenticationToken) throws AuthenticationException {
        String token = (String) authenticationToken.getCredentials();
        TokenSessionInfo sessionInfo = tokenStore.getToken(token);

        if (ObjUtil.isNull(sessionInfo)) {
            log.warn("Token 认证失败，Token 无效或已过期");
            throw new AuthenticationException("登录状态已失效");
        }

        TokenPrincipal principal = new TokenPrincipal(
                sessionInfo.getUserId(),
                sessionInfo.getUsername(),
                sessionInfo.getNickname(),
                token
        );

        return new SimpleAuthenticationInfo(principal, token, getName());
    }

    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
        TokenPrincipal principal = (TokenPrincipal) principals.getPrimaryPrincipal();
        SecurityPermissionProfile permissionProfile = securityUserService.loadPermissionByUserId(principal.getUserId());

        SimpleAuthorizationInfo authorizationInfo = new SimpleAuthorizationInfo();
        if (ObjUtil.isNull(permissionProfile)) {
            log.warn("Token 授权信息为空，用户ID：{}", principal.getUserId());
            return authorizationInfo;
        }

        if (CollUtil.isNotEmpty(permissionProfile.getRoleCodes())) {
            authorizationInfo.setRoles(new HashSet<>(permissionProfile.getRoleCodes()));
        }

        if (CollUtil.isNotEmpty(permissionProfile.getPermissionCodes())) {
            authorizationInfo.setStringPermissions(new HashSet<>(permissionProfile.getPermissionCodes()));
        }

        return authorizationInfo;
    }

}
```

下面定义 Token 认证过滤器。它会从 `Authorization` 或 `X-Access-Token` 中读取 Token，并调用 Shiro 登录流程。

文件位置：`src/main/java/io/github/atengk/shiro/security/filter/BearerTokenAuthenticationFilter.java`

```java
package io.github.atengk.shiro.security.filter;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.shiro.common.response.Result;
import io.github.atengk.shiro.security.token.BearerAuthenticationToken;
import io.github.atengk.shiro.security.util.JsonResponseUtil;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.web.filter.AccessControlFilter;

/**
 * Bearer Token 认证过滤器。
 *
 * @author Ateng
 * @since 2026-05-12
 */
@Slf4j
public class BearerTokenAuthenticationFilter extends AccessControlFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";

    private static final String TOKEN_PREFIX = "Bearer ";

    private static final String ACCESS_TOKEN_HEADER = "X-Access-Token";

    @Override
    protected boolean isAccessAllowed(ServletRequest request, ServletResponse response, Object mappedValue) {
        HttpServletRequest httpRequest = (HttpServletRequest) request;

        if (StrUtil.equalsIgnoreCase(httpRequest.getMethod(), "OPTIONS")) {
            return true;
        }

        String token = resolveToken(httpRequest);
        if (StrUtil.isBlank(token)) {
            return false;
        }

        try {
            Subject subject = getSubject(request, response);
            subject.login(new BearerAuthenticationToken(token));
            return true;
        } catch (AuthenticationException e) {
            log.warn("Token 认证失败：{}", e.getMessage());
            return false;
        }
    }

    @Override
    protected boolean onAccessDenied(ServletRequest request, ServletResponse response) {
        JsonResponseUtil.writeJson(response, Result.fail(401, "用户未登录或登录状态已失效"));
        return false;
    }

    private String resolveToken(HttpServletRequest request) {
        String authorization = request.getHeader(AUTHORIZATION_HEADER);
        if (StrUtil.startWithIgnoreCase(authorization, TOKEN_PREFIX)) {
            return StrUtil.subAfter(authorization, TOKEN_PREFIX, false);
        }

        return request.getHeader(ACCESS_TOKEN_HEADER);
    }

}
```

在 Shiro 配置中注册 Token Realm 和 Token Filter。

文件位置：`src/main/java/io/github/atengk/shiro/config/ShiroConfig.java`

```java
package io.github.atengk.shiro.config;

import io.github.atengk.shiro.module.auth.realm.UserPasswordRealm;
import io.github.atengk.shiro.module.auth.service.SecurityUserService;
import io.github.atengk.shiro.security.filter.BearerTokenAuthenticationFilter;
import io.github.atengk.shiro.security.realm.BearerTokenRealm;
import io.github.atengk.shiro.security.token.TokenStore;
import jakarta.servlet.Filter;
import org.apache.shiro.authc.credential.HashedCredentialsMatcher;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.spring.web.config.DefaultShiroFilterChainDefinition;
import org.apache.shiro.spring.web.config.ShiroFilterChainDefinition;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Apache Shiro 2 核心配置。
 *
 * @author Ateng
 * @since 2026-05-12
 */
@Configuration
public class ShiroConfig {

    @Bean
    public Realm userPasswordRealm(SecurityUserService securityUserService,
                                   HashedCredentialsMatcher hashedCredentialsMatcher) {
        UserPasswordRealm realm = new UserPasswordRealm(securityUserService);
        realm.setCredentialsMatcher(hashedCredentialsMatcher);
        return realm;
    }

    @Bean
    public Realm bearerTokenRealm(TokenStore tokenStore, SecurityUserService securityUserService) {
        return new BearerTokenRealm(tokenStore, securityUserService);
    }

    @Bean(name = "tokenAuthc")
    public Filter bearerTokenAuthenticationFilter() {
        return new BearerTokenAuthenticationFilter();
    }

    @Bean
    public FilterRegistrationBean<Filter> tokenAuthcFilterRegistration(@Qualifier("tokenAuthc") Filter filter) {
        FilterRegistrationBean<Filter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(filter);

        // 只允许 ShiroFilter 调用该 Filter，避免被 Servlet 容器重复注册
        registrationBean.setEnabled(false);
        return registrationBean;
    }

    @Bean
    public ShiroFilterChainDefinition shiroFilterChainDefinition() {
        DefaultShiroFilterChainDefinition chainDefinition = new DefaultShiroFilterChainDefinition();

        // 登录接口必须放行，但不创建 Shiro Session
        chainDefinition.addPathDefinition("/api/auth/login", "anon, noSessionCreation");

        // 预检请求放行
        chainDefinition.addPathDefinition("/**", "noSessionCreation");

        // 静态资源和接口文档放行
        chainDefinition.addPathDefinition("/favicon.ico", "anon");
        chainDefinition.addPathDefinition("/doc.html", "anon");
        chainDefinition.addPathDefinition("/swagger-ui/**", "anon");
        chainDefinition.addPathDefinition("/v3/api-docs/**", "anon");

        // 所有 API 接口使用 Token 认证
        chainDefinition.addPathDefinition("/api/**", "noSessionCreation, tokenAuthc");

        return chainDefinition;
    }

}
```

注意：上面的配置片段用于说明 Token 模式的关键改造。实际项目中不要同时让 `/api/**` 走 Cookie Session 过滤器和 Token 过滤器，否则认证状态来源会混乱。

### 无状态会话处理

Token 模式下，不建议让 Shiro 创建服务端 Session，也不建议向浏览器写入 Session Cookie。核心原则是：每次请求都通过 Token 独立认证，认证结果只在当前请求上下文中有效。

`application.yml` 建议调整如下：

文件位置：`src/main/resources/application.yml`

```yaml
shiro:
  enabled: true

  web:
    enabled: true

  annotations:
    enabled: true

  # 前后端分离项目必须显式关闭默认放行
  allowAccessByDefault: false

  sessionManager:
    # Token 模式不通过 Cookie 传递 Session ID
    sessionIdCookieEnabled: false

    # 禁止通过 URL 重写传递 Session ID
    sessionIdUrlRewritingEnabled: false

  # Token 模式不使用 RememberMe
  rememberMeManager:
    cookie:
      enabled: false
```

登录接口需要改为生成 Token，而不是返回 Session ID。下面展示 Token 登录的核心实现。

文件位置：`src/main/java/io/github/atengk/shiro/module/auth/service/impl/AuthServiceImpl.java`

```java
package io.github.atengk.shiro.module.auth.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.shiro.module.auth.dto.LoginRequest;
import io.github.atengk.shiro.module.auth.model.LoginPrincipal;
import io.github.atengk.shiro.module.auth.model.SecurityPermissionProfile;
import io.github.atengk.shiro.module.auth.service.AuthService;
import io.github.atengk.shiro.module.auth.service.SecurityUserService;
import io.github.atengk.shiro.module.auth.vo.TokenLoginResponse;
import io.github.atengk.shiro.security.token.TokenPrincipal;
import io.github.atengk.shiro.security.token.TokenSessionInfo;
import io.github.atengk.shiro.security.token.TokenStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.subject.Subject;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Token 认证业务服务实现。
 *
 * @author Ateng
 * @since 2026-05-12
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final long TOKEN_EXPIRE_SECONDS = 7200L;

    private final TokenStore tokenStore;

    private final SecurityUserService securityUserService;

    @Override
    public TokenLoginResponse login(LoginRequest request) {
        if (StrUtil.hasBlank(request.getUsername(), request.getPassword())) {
            throw new IllegalArgumentException("用户名或密码不能为空");
        }

        Subject subject = SecurityUtils.getSubject();
        UsernamePasswordToken usernamePasswordToken = new UsernamePasswordToken(
                request.getUsername(),
                request.getPassword(),
                BooleanUtil.isTrue(request.getRememberMe())
        );

        try {
            subject.login(usernamePasswordToken);
            LoginPrincipal principal = (LoginPrincipal) subject.getPrincipal();

            TokenSessionInfo sessionInfo = TokenSessionInfo.builder()
                    .userId(principal.getUserId())
                    .username(principal.getUsername())
                    .nickname(principal.getNickname())
                    .build();

            String accessToken = tokenStore.createToken(sessionInfo, TOKEN_EXPIRE_SECONDS);
            SecurityPermissionProfile permissionProfile = securityUserService.loadPermissionByUserId(principal.getUserId());

            // Token 模式不依赖 Shiro Session，用户名密码校验完成后清理本次登录状态
            subject.logout();

            log.info("Token 登录成功，用户ID：{}", principal.getUserId());

            return TokenLoginResponse.builder()
                    .accessToken(accessToken)
                    .tokenType("Bearer")
                    .expiresIn(TOKEN_EXPIRE_SECONDS)
                    .userId(principal.getUserId())
                    .username(principal.getUsername())
                    .nickname(principal.getNickname())
                    .roles(permissionProfile == null ? List.of() : permissionProfile.getRoleCodes())
                    .permissions(permissionProfile == null ? List.of() : permissionProfile.getPermissionCodes())
                    .build();
        } catch (AuthenticationException e) {
            log.warn("Token 登录失败，用户名：{}，原因：{}", request.getUsername(), e.getMessage());
            throw new AuthenticationException("用户名或密码错误");
        }
    }

    @Override
    public void logout() {
        Subject subject = SecurityUtils.getSubject();
        Object principal = subject.getPrincipal();

        if (principal instanceof TokenPrincipal tokenPrincipal) {
            tokenStore.removeToken(tokenPrincipal.getToken());
            log.info("Token 退出登录，用户ID：{}", tokenPrincipal.getUserId());
        }
    }

    @Override
    public Boolean checkPermission(String permissionCode) {
        if (StrUtil.isBlank(permissionCode)) {
            throw new IllegalArgumentException("权限编码不能为空");
        }

        Subject subject = SecurityUtils.getSubject();
        if (!subject.isAuthenticated()) {
            throw new UnauthenticatedException("用户未登录");
        }

        boolean permitted = subject.isPermitted(permissionCode);
        log.info("校验用户权限，权限编码：{}，结果：{}", permissionCode, permitted);
        return permitted;
    }

}
```

无状态模式下需要注意以下事项：

| 项目       | 建议                                                    |
| ---------- | ------------------------------------------------------- |
| Session    | 禁止通过 Cookie 或 URL 保存 Session ID                  |
| RememberMe | Token 模式下通常关闭                                    |
| Token 存储 | 开发环境可内存存储，生产环境使用 Redis                  |
| Token 过期 | 必须设置过期时间                                        |
| Token 续期 | 可在 Filter 认证成功后按需刷新                          |
| Token 退出 | 登出时删除服务端 Token                                  |
| 权限变更   | 修改用户角色或权限后清理授权缓存，必要时强制 Token 失效 |

### 跨域与未登录响应处理

前后端分离项目通常存在跨域请求。跨域配置建议在 Spring MVC 中统一处理，同时在 Shiro Token Filter 中放行 `OPTIONS` 预检请求。

下面配置跨域规则。

文件位置：`src/main/java/io/github/atengk/shiro/config/CorsConfig.java`

```java
package io.github.atengk.shiro.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 跨域配置。
 *
 * @author Ateng
 * @since 2026-05-12
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                // 生产环境应替换为具体前端域名，例如 https://admin.example.com
                .allowedOriginPatterns("http://localhost:*", "http://127.0.0.1:*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Authorization", "X-Access-Token")
                .allowCredentials(false)
                .maxAge(3600);
    }

}
```

未登录和无权限响应建议保持统一：

```json
{
  "code": 401,
  "message": "用户未登录或登录状态已失效",
  "data": null,
  "timestamp": 1778572800000
}
{
  "code": 403,
  "message": "权限不足",
  "data": null,
  "timestamp": 1778572800000
}
```

前端请求示例：

```javascript
const token = localStorage.getItem('accessToken')

fetch('http://127.0.0.1:8080/api/auth/me', {
  method: 'GET',
  headers: {
    Authorization: `Bearer ${token}`
  }
})
```

## 功能验证

本节验证登录认证、角色权限、接口拦截和异常场景。建议先准备一个管理员账号和一个普通账号，分别赋予不同角色和权限，再通过 curl 或 Postman 执行接口测试。

### 登录认证测试

登录认证测试用于验证用户名密码校验、Token 签发和 Token 访问是否正常。

测试登录接口：

```bash
curl -X POST 'http://127.0.0.1:8080/api/auth/login' \
  -H 'Content-Type: application/json' \
  -d '{
    "username": "admin",
    "password": "123456",
    "rememberMe": false
  }'
```

预期响应：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "accessToken": "实际返回的Token",
    "tokenType": "Bearer",
    "expiresIn": 7200,
    "userId": 1,
    "username": "admin",
    "nickname": "管理员",
    "roles": ["admin"],
    "permissions": ["user:query", "user:create", "user:update", "user:delete"]
  },
  "timestamp": 1778572800000
}
```

提取 Token 后访问当前用户接口：

```bash
TOKEN='实际返回的Token'

curl -X GET 'http://127.0.0.1:8080/api/auth/me' \
  -H "Authorization: Bearer ${TOKEN}"
```

验证点：

| 验证项         | 预期结果                    |
| -------------- | --------------------------- |
| 用户名密码正确 | 返回 Token                  |
| 用户名不存在   | 返回 `401`                  |
| 密码错误       | 返回 `401`                  |
| 账号禁用       | 返回 `401` 或业务定义状态码 |
| 携带有效 Token | 可访问受保护接口            |
| 不携带 Token   | 返回 `401`                  |

### 角色权限测试

角色权限测试用于验证 `@RequiresRoles` 和 `@RequiresPermissions` 是否生效。

假设接口权限如下：

| 接口                     | 注解                                                         | 说明     |
| ------------------------ | ------------------------------------------------------------ | -------- |
| `GET /api/users`         | `@RequiresPermissions("user:query")`                         | 查询用户 |
| `POST /api/users`        | `@RequiresPermissions("user:create")`                        | 创建用户 |
| `DELETE /api/users/{id}` | `@RequiresRoles("admin")` + `@RequiresPermissions("user:delete")` | 删除用户 |

管理员测试：

```bash
ADMIN_TOKEN='管理员Token'

curl -X GET 'http://127.0.0.1:8080/api/users' \
  -H "Authorization: Bearer ${ADMIN_TOKEN}"

curl -X DELETE 'http://127.0.0.1:8080/api/users/1001' \
  -H "Authorization: Bearer ${ADMIN_TOKEN}"
```

普通用户测试：

```bash
USER_TOKEN='普通用户Token'

curl -X GET 'http://127.0.0.1:8080/api/users' \
  -H "Authorization: Bearer ${USER_TOKEN}"

curl -X DELETE 'http://127.0.0.1:8080/api/users/1001' \
  -H "Authorization: Bearer ${USER_TOKEN}"
```

预期结果：

| 用户     | 操作     | 预期                      |
| -------- | -------- | ------------------------- |
| 管理员   | 查询用户 | 成功                      |
| 管理员   | 删除用户 | 成功                      |
| 普通用户 | 查询用户 | 取决于是否有 `user:query` |
| 普通用户 | 删除用户 | 返回 `403`                |

### 接口拦截测试

接口拦截测试用于验证 Shiro FilterChain 是否正确拦截 `/api/**`。

不携带 Token 访问接口：

```bash
curl -i -X GET 'http://127.0.0.1:8080/api/auth/me'
```

预期返回：

```json
{
  "code": 401,
  "message": "用户未登录或登录状态已失效",
  "data": null,
  "timestamp": 1778572800000
}
```

携带错误 Token 访问接口：

```bash
curl -i -X GET 'http://127.0.0.1:8080/api/auth/me' \
  -H 'Authorization: Bearer error-token'
```

预期返回 `401`。

预检请求测试：

```bash
curl -i -X OPTIONS 'http://127.0.0.1:8080/api/auth/me' \
  -H 'Origin: http://localhost:5173' \
  -H 'Access-Control-Request-Method: GET' \
  -H 'Access-Control-Request-Headers: Authorization'
```

预期结果：

| 验证项                   | 预期                           |
| ------------------------ | ------------------------------ |
| `OPTIONS` 请求           | 不被 Token Filter 拦截         |
| 响应头包含跨域配置       | 前端浏览器允许继续发起真实请求 |
| `/api/**` 未携带 Token   | 返回 `401`                     |
| `/api/**` 携带有效 Token | 进入 Controller                |

### 异常场景测试

异常场景测试用于验证系统在错误输入、无权限、登录失效等情况下是否返回统一 JSON，而不是 HTML 错误页或重定向页面。

建议覆盖以下场景：

| 场景       | 请求方式                       | 预期  |
| ---------- | ------------------------------ | ----- |
| 用户名为空 | 登录接口传空用户名             | `400` |
| 密码错误   | 登录接口传错误密码             | `401` |
| Token 缺失 | 访问 `/api/auth/me` 不带 Token | `401` |
| Token 过期 | 使用过期 Token 访问接口        | `401` |
| 权限不足   | 普通用户删除用户               | `403` |
| 参数错误   | 缺少必要业务参数               | `400` |
| 系统异常   | 人为触发运行时异常             | `500` |

错误登录测试：

```bash
curl -X POST 'http://127.0.0.1:8080/api/auth/login' \
  -H 'Content-Type: application/json' \
  -d '{
    "username": "admin",
    "password": "error-password",
    "rememberMe": false
  }'
```

权限不足测试：

```bash
USER_TOKEN='普通用户Token'

curl -X DELETE 'http://127.0.0.1:8080/api/users/1001' \
  -H "Authorization: Bearer ${USER_TOKEN}"
```

预期无权限响应：

```json
{
  "code": 403,
  "message": "权限不足",
  "data": null,
  "timestamp": 1778572800000
}
```

最终验收标准如下：

| 模块       | 验收标准                                     |
| ---------- | -------------------------------------------- |
| 登录认证   | 正确账号返回 Token，错误账号返回统一异常     |
| Token 认证 | 有效 Token 可访问接口，无效 Token 返回 `401` |
| 角色权限   | 注解权限正常生效，权限不足返回 `403`         |
| 无状态会话 | 请求不依赖 Cookie Session                    |
| 跨域请求   | 浏览器前端可正常携带 `Authorization` 请求头  |
| 异常响应   | 所有错误场景返回统一 JSON                    |