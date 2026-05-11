# Spring Boot 集成 Hutool 开发

本文档用于说明在 Spring Boot 3 项目中集成 Hutool 的基础开发规范，重点覆盖项目定位、技术选型、适用场景、开发目标以及环境依赖配置。Spring Boot 3 系列要求以 Java 17 作为最低运行基线，当前 Spring Boot 文档稳定版本列表中仍包含 Spring Boot 3.5.14，可作为 Spring Boot 3 新项目的优先选择之一。([Spring Enterprise Documentation](https://docs.enterprise.spring.io/spring-boot/system-requirements.html))

## 项目概述

本项目基于 Spring Boot 3 构建后端服务，通过集成 Hutool 提供常用工具能力，减少重复工具类编写，提高字符串、集合、日期、JSON、文件、加密、HTTP 调用等基础能力的开发效率。

Hutool 在项目中的定位不是替代 Spring Framework 或 JDK 标准能力，而是作为业务开发中的通用工具补充。对于 Spring 已经提供成熟抽象的能力，例如配置绑定、Bean 管理、事务控制、参数校验、HTTP 服务暴露等，应优先使用 Spring 原生机制；对于日常数据处理、格式转换、工具方法封装等场景，可以合理使用 Hutool。

### 技术选型

本项目建议采用以下技术栈作为基础开发组合。Spring Boot 3 负责应用启动、自动配置、Web 服务、参数校验、配置管理和测试能力；Hutool 负责常用工具能力补充；Maven 负责依赖管理和构建。Spring Boot 3.4.16 官方文档要求至少 Java 17，并明确支持 Maven 3.6.3 及以上版本，这一要求适合作为 Spring Boot 3 项目的环境基线。([Spring Enterprise Documentation](https://docs.enterprise.spring.io/spring-boot/system-requirements.html))

| 技术              | 推荐版本                   | 用途                         |
| ----------------- | -------------------------- | ---------------------------- |
| JDK               | 17 或 21 LTS               | Spring Boot 3 运行基础       |
| Spring Boot       | 3.5.14 或团队指定 3.x 版本 | 应用框架、自动配置、Web 能力 |
| Hutool            | 5.8.44                     | 常用 Java 工具类库           |
| Maven             | 3.6.3+                     | 项目构建与依赖管理           |
| Lombok            | 与 Spring Boot 兼容版本    | 简化 DTO、VO、配置类代码     |
| Spring Validation | Spring Boot Starter 管理   | 参数校验                     |

Hutool 当前 5.x 线的 `hutool-all` 最新版本为 `5.8.44`，Maven Repository 显示其发布日期为 2026 年 3 月 12 日；同时 Hutool v7 仍存在里程碑版本，不建议在稳定业务项目中直接使用 v7 预发布版本。([Maven Repository](https://mvnrepository.com/artifact/cn.hutool/hutool-all))

### 使用场景

Hutool 适合用于业务开发中高频、重复、非框架核心能力的工具处理。典型场景包括字符串判空、集合判空、日期格式化、对象判空、JSON 转换、文件读取、摘要加密、HTTP 客户端调用、脱敏处理等。

在 Spring Boot 3 项目中，Hutool 常见使用方式如下：

| 场景       | 推荐 Hutool 模块 | 示例能力                                              |
| ---------- | ---------------- | ----------------------------------------------------- |
| 字符串处理 | `hutool-core`    | `StrUtil.isBlank`、`StrUtil.format`                   |
| 集合处理   | `hutool-core`    | `CollUtil.isEmpty`、`CollUtil.newArrayList`           |
| 日期时间   | `hutool-core`    | `DateUtil.format`、`DateUtil.parse`                   |
| JSON 转换  | `hutool-json`    | `JSONUtil.toJsonStr`、`JSONUtil.toBean`               |
| 文件处理   | `hutool-core`    | `FileUtil.readUtf8String`、`FileUtil.writeUtf8String` |
| 加密摘要   | `hutool-crypto`  | `SecureUtil.md5`、`SecureUtil.sha256`                 |
| HTTP 调用  | `hutool-http`    | `HttpUtil.get`、`HttpRequest.post`                    |
| 数据脱敏   | `hutool-core`    | `DesensitizedUtil.mobilePhone`                        |

使用时需要控制边界。接口入参校验优先使用 `jakarta.validation`；JSON 序列化优先使用 Spring Boot 默认集成的 Jackson；HTTP 调用在简单内部工具或低复杂度场景可使用 Hutool，复杂场景建议使用 Spring `RestClient`、`WebClient` 或 OpenFeign。

### 开发目标

本项目集成 Hutool 的主要目标是建立统一、可维护、可复用的工具使用规范，而不是在项目中无边界地引入工具方法。

开发目标包括：

1. 减少重复工具类代码，例如字符串判空、集合判空、日期格式化、文件读取等。
2. 统一常用数据处理方式，避免不同开发人员在同一项目中使用多套工具风格。
3. 提升业务代码可读性，让业务逻辑聚焦在规则本身，而不是底层工具实现。
4. 保持 Spring Boot 3 原生能力优先，避免 Hutool 与 Spring、Jackson、Validation 等框架能力冲突。
5. 为后续章节中的统一返回、全局异常、文件上传、远程调用、数据脱敏等示例提供基础依赖。

## 环境准备

环境准备主要确认 JDK、Spring Boot 版本、构建工具和 Hutool 依赖版本。建议先统一团队开发环境，再进行工程搭建，避免后续出现编译版本、依赖版本或 Jakarta 包迁移相关问题。

### JDK 版本要求

Spring Boot 3 项目必须使用 JDK 17 或更高版本。Spring Boot 3.4.16 官方系统要求说明其最低要求为 Java 17，且兼容到 Java 24；因此实际项目中建议使用 JDK 17 或 JDK 21 LTS。([Spring Enterprise Documentation](https://docs.enterprise.spring.io/spring-boot/system-requirements.html))

推荐策略如下：

| JDK 版本 | 是否推荐 | 说明                                          |
| -------- | -------- | --------------------------------------------- |
| JDK 8    | 不支持   | Spring Boot 3 不支持                          |
| JDK 11   | 不支持   | Spring Boot 3 不支持                          |
| JDK 17   | 推荐     | Spring Boot 3 最低基线，兼容性好              |
| JDK 21   | 推荐     | LTS 版本，适合新项目                          |
| JDK 22+  | 谨慎     | 需结合 Spring Boot 具体版本和生产环境支持策略 |

检查本地 JDK 版本：

```bash
java -version
javac -version
```

Maven 编译版本建议统一配置为 `17` 或 `21`。如果团队没有明确升级计划，建议先使用 `17`，降低运行环境差异。

### Spring Boot 3 版本选择

新项目建议优先选择当前稳定的 Spring Boot 3.5.x 版本；截至当前检索结果，Spring Boot 官方文档稳定版本列表中包含 `3.5.14`，Spring 官方博客也说明 `3.5.14` 已发布并可从 Maven Central 获取。([Home](https://docs.spring.io/spring-boot/index.html?utm_source=chatgpt.com))

版本选择建议如下：

| 项目类型                  | 推荐版本策略                                        |
| ------------------------- | --------------------------------------------------- |
| 新建项目                  | 使用 Spring Boot 3.5.14 或团队统一的 3.5.x 稳定版本 |
| 存量 Spring Boot 3.4 项目 | 可继续维护，结合安全修复节奏评估是否升级            |
| 存量 Spring Boot 2.x 项目 | 迁移前需评估 `javax.*` 到 `jakarta.*` 的包名变化    |
| 企业长期维护项目          | 优先选择团队统一认证过的 Spring Boot 3.x 小版本     |

Maven 父工程建议使用 Spring Boot 官方 parent 管理依赖版本：

```xml
<!-- Spring Boot 统一管理依赖版本、插件版本和默认构建配置 -->
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.5.14</version>
    <relativePath/>
</parent>
```

如果公司已有统一 BOM 或父工程，应优先使用公司内部父工程，避免多个模块之间 Spring Boot、Spring Framework、Jackson、Validation 等依赖版本不一致。

### Hutool 依赖配置

Hutool 可以按模块引入，也可以使用 `hutool-all` 一次性引入全部常用模块。业务系统早期或中小型项目可以使用 `hutool-all` 提升开发效率；对依赖体积、依赖审计、模块边界要求较高的项目，建议按需引入 `hutool-core`、`hutool-json`、`hutool-crypto`、`hutool-http` 等模块。

Maven 依赖配置如下，建议先通过 properties 统一维护 Hutool 版本。

```xml
<properties>
    <!-- Java 编译版本，Spring Boot 3 最低要求 Java 17 -->
    <java.version>17</java.version>

    <!-- Hutool 版本统一管理，避免多模块版本不一致 -->
    <hutool.version>5.8.44</hutool.version>
</properties>

<dependencies>
    <!-- Spring Web：提供 REST 接口、JSON 处理、Servlet Web 能力 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- 参数校验：Spring Boot 3 使用 jakarta.validation 体系 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- Hutool 全量工具包：包含 core、json、crypto、http 等常用模块 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>${hutool.version}</version>
    </dependency>

    <!-- Lombok：简化 DTO、VO、配置类等样板代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- 测试依赖：提供 JUnit、Spring Test、MockMvc 等测试能力 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

如果项目更强调依赖精简，可以改为按需引入：

```xml
<dependencies>
    <!-- Hutool 核心工具：字符串、集合、日期、文件、对象等基础能力 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-core</artifactId>
        <version>${hutool.version}</version>
    </dependency>

    <!-- Hutool JSON：轻量 JSON 转换能力，复杂接口序列化仍建议优先使用 Jackson -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-json</artifactId>
        <version>${hutool.version}</version>
    </dependency>

    <!-- Hutool 加密：MD5、SHA、AES、RSA 等常用加密摘要能力 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-crypto</artifactId>
        <version>${hutool.version}</version>
    </dependency>

    <!-- Hutool HTTP：适合简单 HTTP 请求，复杂远程调用建议使用 RestClient、WebClient 或 OpenFeign -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-http</artifactId>
        <version>${hutool.version}</version>
    </dependency>
</dependencies>
```

依赖配置完成后，可以执行以下命令检查依赖是否正常解析：

```bash
mvn clean compile
mvn dependency:tree -Dincludes=cn.hutool
```

第一条命令用于验证项目是否可以正常编译；第二条命令用于查看 Hutool 实际引入的依赖版本，确认是否存在多版本冲突。对于多模块项目，建议在父工程中统一声明 `hutool.version`，业务模块只声明依赖，不单独写版本号。



## 基础工程搭建

基础工程搭建用于统一项目目录、依赖管理方式和配置文件组织规则。Spring Boot 3 项目应保持清晰的分层结构，避免工具类、业务类、配置类混放，后续集成 Hutool、统一异常、统一返回、日志处理和业务模块开发都会基于该工程结构展开。

### 项目结构设计

项目结构建议采用标准 Maven + Spring Boot 分层结构。通用代码放在 `common` 包下，业务代码按模块划分，配置类统一放在 `config` 包下，接口层、业务层、实体对象、请求对象和响应对象保持职责清晰。

推荐项目结构如下：

```text
springboot3-hutool-demo
├── pom.xml
├── README.md
├── src
│   ├── main
│   │   ├── java
│   │   │   └── io
│   │   │       └── github
│   │   │           └── atengk
│   │   │               ├── SpringBootHutoolApplication.java
│   │   │               ├── common
│   │   │               │   ├── constant
│   │   │               │   │   └── CommonConstant.java
│   │   │               │   ├── exception
│   │   │               │   │   └── BizException.java
│   │   │               │   ├── result
│   │   │               │   │   └── Result.java
│   │   │               │   └── util
│   │   │               │       └── HutoolDemoUtil.java
│   │   │               ├── config
│   │   │               │   └── WebConfig.java
│   │   │               └── module
│   │   │                   └── user
│   │   │                       ├── controller
│   │   │                       │   └── UserController.java
│   │   │                       ├── service
│   │   │                       │   └── UserService.java
│   │   │                       ├── service
│   │   │                       │   └── impl
│   │   │                       │       └── UserServiceImpl.java
│   │   │                       ├── dto
│   │   │                       │   └── UserCreateDTO.java
│   │   │                       └── vo
│   │   │                           └── UserVO.java
│   │   └── resources
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       ├── application-prod.yml
│   │       └── logback-spring.xml
│   └── test
│       └── java
│           └── io
│               └── github
│                   └── atengk
│                       └── HutoolModuleTests.java
```

核心目录说明如下：

| 目录               | 说明                                                         |
| ------------------ | ------------------------------------------------------------ |
| `common.constant`  | 存放公共常量，避免业务代码中出现大量硬编码                   |
| `common.exception` | 存放业务异常、系统异常等异常类型                             |
| `common.result`    | 存放统一返回结构                                             |
| `common.util`      | 存放通用工具封装，允许对 Hutool 做轻量二次封装               |
| `config`           | 存放 Spring Boot 配置类，例如 Web、Jackson、拦截器配置       |
| `module`           | 存放业务模块，每个模块内部再按 Controller、Service、DTO、VO 分层 |
| `resources`        | 存放配置文件、日志配置、静态资源等                           |

启动类放在根包 `io.github.atengk` 下，保证 Spring Boot 可以扫描到所有子包。

文件位置：`src/main/java/io/github/atengk/SpringBootHutoolApplication.java`

下面的启动类用于启动 Spring Boot 3 项目，包路径需要位于业务代码的最外层根包。

```java
package io.github.atengk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot 集成 Hutool 示例项目启动类
 *
 * @author Ateng
 * @since 2026-05-09
 */
@SpringBootApplication
public class SpringBootHutoolApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringBootHutoolApplication.class, args);
    }

}
```

### Maven 依赖管理

Maven 依赖管理建议遵循“版本集中管理、模块按需引入、业务模块不重复声明版本”的原则。Spring Boot 相关依赖优先交给 `spring-boot-starter-parent` 管理，Hutool 版本通过 `properties` 统一声明。

文件位置：`pom.xml`

下面的 Maven 配置用于定义 Spring Boot 3 项目基础依赖，并集成 Hutool、参数校验、Lombok 和测试能力。

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">

    <modelVersion>4.0.0</modelVersion>

    <!-- Spring Boot 父工程：统一管理 Spring、Jackson、Validation、日志等依赖版本 -->
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.5.14</version>
        <relativePath/>
    </parent>

    <groupId>io.github.atengk</groupId>
    <artifactId>springboot3-hutool-demo</artifactId>
    <version>1.0.0</version>
    <name>springboot3-hutool-demo</name>
    <description>Spring Boot 3 集成 Hutool 开发示例</description>

    <properties>
        <!-- Spring Boot 3 最低要求 Java 17，生产项目建议统一团队 JDK 版本 -->
        <java.version>17</java.version>

        <!-- Hutool 版本集中管理，避免不同模块版本不一致 -->
        <hutool.version>5.8.44</hutool.version>
    </properties>

    <dependencies>
        <!-- Web 开发基础依赖：提供 REST API、Spring MVC、内置 Tomcat、Jackson 等能力 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- 参数校验依赖：Spring Boot 3 使用 jakarta.validation 包体系 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <!-- Hutool 全量工具包：包含 core、json、crypto、http 等常用模块 -->
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
            <version>${hutool.version}</version>
        </dependency>

        <!-- Lombok：减少 DTO、VO、配置类中的样板代码 -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- 单元测试依赖：包含 JUnit Jupiter、Spring Test、MockMvc 等测试能力 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <!-- Spring Boot 打包插件：支持生成可执行 Jar -->
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>

            <!-- Maven 编译插件：统一 Java 编译版本 -->
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

</project>
```

如果项目规模较大，可以在父工程中通过 `dependencyManagement` 统一管理 Hutool 版本，子模块只声明依赖，不声明版本。

```xml
<dependencyManagement>
    <dependencies>
        <!-- 统一管理 Hutool 版本，业务模块只负责按需引入 -->
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
            <version>${hutool.version}</version>
        </dependency>
    </dependencies>
</dependencyManagement>
```

依赖检查命令如下：

```bash
mvn clean compile
mvn dependency:tree -Dincludes=cn.hutool
```

`mvn clean compile` 用于确认项目是否可以正常编译，`mvn dependency:tree` 用于检查 Hutool 实际依赖版本，避免多模块项目中出现版本不一致问题。

### 配置文件规划

配置文件建议按环境拆分，公共配置放在 `application.yml`，环境差异配置放在 `application-dev.yml`、`application-prod.yml`。Hutool 本身通常不需要额外配置，但与 Hutool 使用相关的文件路径、远程接口地址、超时时间、加密盐值等业务参数应统一放入配置文件。

文件位置：`src/main/resources/application.yml`

下面的配置用于定义项目基础信息、激活环境、接口路径和业务参数。

```yaml
server:
  # 服务端口，开发环境可根据本地占用情况调整
  port: 8080
  servlet:
    # 接口统一前缀，便于网关或前端统一代理
    context-path: /api

spring:
  application:
    # 应用名称，用于日志、链路追踪和服务标识
    name: springboot3-hutool-demo
  profiles:
    # 默认激活开发环境
    active: dev

logging:
  level:
    # 项目根包日志级别
    io.github.atengk: info

app:
  file:
    # 文件读取和写入的基础目录，生产环境建议使用独立挂载目录
    base-path: /tmp/springboot3-hutool-demo
  http:
    # Hutool HTTP 调用默认超时时间，单位毫秒
    timeout: 5000
  security:
    # 示例盐值，生产环境必须通过环境变量、配置中心或密钥系统维护
    salt: ateng-demo-salt
```

文件位置：`src/main/resources/application-dev.yml`

```yaml
logging:
  level:
    # 开发环境可以开启 debug，便于排查问题
    io.github.atengk: debug

app:
  file:
    # 开发环境文件目录
    base-path: ./data
```

文件位置：`src/main/resources/application-prod.yml`

```yaml
logging:
  level:
    # 生产环境保持 info，避免输出过多日志
    io.github.atengk: info

app:
  file:
    # 生产环境建议使用固定挂载目录
    base-path: /data/springboot3-hutool-demo
```

如果需要在代码中读取这些配置，可以使用 `@ConfigurationProperties` 进行类型安全绑定，而不是在业务代码中到处写 `@Value`。

文件位置：`src/main/java/io/github/atengk/config/AppProperties.java`

下面的配置类用于读取 `app.*` 业务配置，供文件处理、HTTP 调用和加密逻辑复用。

```java
package io.github.atengk.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 应用业务配置属性
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private FileConfig file = new FileConfig();

    private HttpConfig http = new HttpConfig();

    private SecurityConfig security = new SecurityConfig();

    /**
     * 文件配置
     */
    @Data
    public static class FileConfig {
        private String basePath;
    }

    /**
     * HTTP 配置
     */
    @Data
    public static class HttpConfig {
        private Integer timeout;
    }

    /**
     * 安全配置
     */
    @Data
    public static class SecurityConfig {
        private String salt;
    }

}
```

## Hutool 常用模块

Hutool 常用模块主要用于提升日常业务开发效率。本章节重点介绍字符串、集合、日期时间、对象、JSON、文件、加密和 HTTP 工具的基础用法，并说明在 Spring Boot 3 项目中的推荐使用边界。

示例代码统一放在工具演示类中，实际项目中可以按业务需要拆分到 Service、Util 或测试类中。

文件位置：`src/main/java/io/github/atengk/common/util/HutoolDemoUtil.java`

下面的工具类集中演示 Hutool 常用模块的基础用法，可用于快速验证 Hutool 集成是否正常。

```java
package io.github.atengk.common.util;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Hutool 常用模块演示工具类
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
public class HutoolDemoUtil {

    public static String buildUsername(String prefix, Long userId) {
        if (StrUtil.isBlank(prefix) || ObjectUtil.isNull(userId)) {
            log.warn("构建用户名失败，prefix 或 userId 为空");
            return StrUtil.EMPTY;
        }
        return StrUtil.format("{}_{}", prefix, userId);
    }

    public static boolean hasValidData(List<String> values) {
        return CollUtil.isNotEmpty(values);
    }

    public static String formatNow() {
        return DateUtil.format(new Date(), DatePattern.NORM_DATETIME_PATTERN);
    }

    public static boolean isEmptyObject(Object value) {
        return ObjectUtil.isEmpty(value);
    }

    public static String toJson(Object value) {
        if (ObjectUtil.isNull(value)) {
            return "{}";
        }
        return JSONUtil.toJsonStr(value);
    }

    public static void writeText(String path, String content) {
        if (StrUtil.isBlank(path)) {
            throw new IllegalArgumentException("文件路径不能为空");
        }
        FileUtil.writeString(content, new File(path), StandardCharsets.UTF_8);
        log.info("文件写入成功，path={}", path);
    }

    public static String md5WithSalt(String rawText, String salt) {
        if (StrUtil.hasBlank(rawText, salt)) {
            throw new IllegalArgumentException("加密原文和盐值不能为空");
        }
        return SecureUtil.md5(rawText + salt);
    }

    public static String doGet(String url, Integer timeout) {
        if (StrUtil.isBlank(url)) {
            throw new IllegalArgumentException("请求地址不能为空");
        }
        int requestTimeout = ObjectUtil.defaultIfNull(timeout, 5000);
        return HttpRequest.get(url)
                .timeout(requestTimeout)
                .execute()
                .body();
    }

    public static Map<String, Object> buildMap() {
        return Map.of(
                "name", "Ateng",
                "time", DateTime.now().toString(),
                "enabled", true
        );
    }

}
```

### 字符串工具

字符串工具主要用于判空、格式化、截取、分割、去除前后缀等操作。项目中建议优先使用 `StrUtil` 替代手写 `null` 判断和空字符串判断。

常用方法如下：

| 方法                                | 说明                                              |
| ----------------------------------- | ------------------------------------------------- |
| `StrUtil.isBlank(value)`            | 判断字符串是否为空白，包含 `null`、空字符串、空格 |
| `StrUtil.isNotBlank(value)`         | 判断字符串是否非空白                              |
| `StrUtil.hasBlank(values...)`       | 判断多个字符串中是否存在空白值                    |
| `StrUtil.format(template, args...)` | 按 `{}` 占位符格式化字符串                        |
| `StrUtil.sub(value, from, to)`      | 截取字符串                                        |

示例代码如下：

```java
String username = "";
boolean blank = StrUtil.isBlank(username);

String message = StrUtil.format("用户 {} 登录成功，用户ID：{}", "Ateng", 10001L);

if (StrUtil.hasBlank(username, message)) {
    log.warn("字符串参数存在空值");
}
```

推荐使用场景：

| 场景                   | 推荐写法                               |
| ---------------------- | -------------------------------------- |
| 接口参数空字符串判断   | `StrUtil.isBlank(name)`                |
| 日志文本拼接前构造内容 | `StrUtil.format("用户ID：{}", userId)` |
| 多个字符串同时校验     | `StrUtil.hasBlank(username, phone)`    |

需要注意的是，参数校验不要完全依赖 `StrUtil`。Controller 入参仍然建议使用 `@NotBlank`、`@NotNull`、`@Valid` 等 Jakarta Validation 注解完成第一层校验。

### 集合工具

集合工具主要用于集合判空、集合创建、交集、并集、过滤、分组等处理。业务代码中不建议直接写复杂的集合空判断，可以使用 `CollUtil` 提高可读性。

常用方法如下：

| 方法                                   | 说明                     |
| -------------------------------------- | ------------------------ |
| `CollUtil.isEmpty(collection)`         | 判断集合是否为空         |
| `CollUtil.isNotEmpty(collection)`      | 判断集合是否非空         |
| `CollUtil.newArrayList(values...)`     | 快速创建 `ArrayList`     |
| `CollUtil.join(collection, separator)` | 使用指定分隔符拼接集合   |
| `CollUtil.contains(collection, value)` | 判断集合是否包含指定元素 |

示例代码如下：

```java
List<String> roles = CollUtil.newArrayList("admin", "user", "guest");

if (CollUtil.isEmpty(roles)) {
    log.warn("角色列表为空");
    return;
}

String roleText = CollUtil.join(roles, ",");
log.info("角色列表：{}", roleText);
```

在业务开发中，集合工具常用于批量参数校验、查询结果判空、批量 ID 拼接、导入数据过滤等场景。对于复杂集合流式处理，可以结合 Java Stream 使用；简单集合判空和集合构造则优先使用 Hutool，代码更直接。

### 日期时间工具

日期时间工具主要用于日期格式化、日期解析、时间偏移、开始结束时间计算等操作。Hutool 的 `DateUtil` 适合处理 `Date` 类型和常见字符串日期格式。

常用方法如下：

| 方法                               | 说明             |
| ---------------------------------- | ---------------- |
| `DateUtil.date()`                  | 获取当前时间     |
| `DateUtil.format(date, pattern)`   | 日期格式化       |
| `DateUtil.parse(value)`            | 字符串解析为日期 |
| `DateUtil.beginOfDay(date)`        | 获取一天开始时间 |
| `DateUtil.endOfDay(date)`          | 获取一天结束时间 |
| `DateUtil.offsetDay(date, offset)` | 按天偏移日期     |

示例代码如下：

```java
DateTime now = DateUtil.date();

String nowText = DateUtil.format(now, DatePattern.NORM_DATETIME_PATTERN);

DateTime beginOfDay = DateUtil.beginOfDay(now);
DateTime endOfDay = DateUtil.endOfDay(now);

DateTime tomorrow = DateUtil.offsetDay(now, 1);

log.info("当前时间：{}，当天开始：{}，当天结束：{}，明天：{}",
        nowText, beginOfDay, endOfDay, tomorrow);
```

日期时间处理需要注意类型边界。新业务代码中，如果主要使用 Java 8+ 时间类型，可以优先使用 `LocalDateTime`、`LocalDate`、`Instant`；如果涉及旧系统、数据库字段、第三方接口返回 `Date` 或字符串日期，使用 Hutool `DateUtil` 会更方便。

### 对象工具

对象工具主要用于对象判空、默认值处理、相等判断、克隆等操作。业务代码中常用的是 `ObjectUtil.isNull`、`ObjectUtil.isNotNull`、`ObjectUtil.defaultIfNull` 和 `ObjectUtil.isEmpty`。

常用方法如下：

| 方法                                            | 说明                                       |
| ----------------------------------------------- | ------------------------------------------ |
| `ObjectUtil.isNull(value)`                      | 判断对象是否为 `null`                      |
| `ObjectUtil.isNotNull(value)`                   | 判断对象是否不为 `null`                    |
| `ObjectUtil.isEmpty(value)`                     | 判断对象是否为空，支持字符串、集合、Map 等 |
| `ObjectUtil.defaultIfNull(value, defaultValue)` | 如果对象为 `null`，返回默认值              |
| `ObjectUtil.equal(a, b)`                        | 安全比较两个对象是否相等                   |

示例代码如下：

```java
Integer timeout = null;

int requestTimeout = ObjectUtil.defaultIfNull(timeout, 5000);

Long userId = 10001L;
if (ObjectUtil.isNotNull(userId)) {
    log.info("用户ID有效，userId={}", userId);
}

boolean same = ObjectUtil.equal(userId, 10001L);
```

对象工具适合用于普通业务判断，但不建议滥用。对于强业务约束，例如“用户不存在”“订单状态非法”“金额不能为空”，应结合业务异常和参数校验机制处理，而不是只返回默认值掩盖问题。

### JSON 工具

JSON 工具主要用于对象与 JSON 字符串之间的轻量转换。Hutool 的 `JSONUtil` 使用简单，适合工具类、日志输出、简单数据转换等场景。

常用方法如下：

| 方法                           | 说明                       |
| ------------------------------ | -------------------------- |
| `JSONUtil.toJsonStr(value)`    | 对象转 JSON 字符串         |
| `JSONUtil.toBean(json, clazz)` | JSON 字符串转对象          |
| `JSONUtil.parseObj(value)`     | 转为 `JSONObject`          |
| `JSONUtil.parseArray(value)`   | 转为 `JSONArray`           |
| `JSONUtil.isTypeJSON(value)`   | 判断字符串是否为 JSON 格式 |

示例 DTO 如下。

文件位置：`src/main/java/io/github/atengk/module/user/dto/UserCreateDTO.java`

```java
package io.github.atengk.module.user.dto;

import lombok.Data;

/**
 * 用户创建请求参数
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
public class UserCreateDTO {

    private String username;

    private String phone;

    private Integer age;

}
```

JSON 转换示例：

```java
UserCreateDTO dto = new UserCreateDTO();
dto.setUsername("Ateng");
dto.setPhone("13800138000");
dto.setAge(18);

String json = JSONUtil.toJsonStr(dto);
log.info("用户 JSON 数据：{}", json);

UserCreateDTO parsed = JSONUtil.toBean(json, UserCreateDTO.class);
log.info("解析后的用户名：{}", parsed.getUsername());
```

在 Spring Boot Web 接口中，默认 JSON 序列化和反序列化建议继续使用 Jackson，也就是由 Spring MVC 自动处理 `@RequestBody` 和响应对象。Hutool `JSONUtil` 更适合业务内部转换、日志输出、简单配置解析、第三方接口临时数据处理等场景。

### 文件工具

文件工具主要用于文件创建、读取、写入、目录判断、文件扩展名获取等操作。Hutool 的 `FileUtil` 可以减少大量重复的文件流代码。

常用方法如下：

| 方法                                      | 说明                   |
| ----------------------------------------- | ---------------------- |
| `FileUtil.exist(path)`                    | 判断文件或目录是否存在 |
| `FileUtil.mkdir(path)`                    | 创建目录               |
| `FileUtil.writeUtf8String(content, path)` | 写入 UTF-8 文本        |
| `FileUtil.readUtf8String(path)`           | 读取 UTF-8 文本        |
| `FileUtil.extName(file)`                  | 获取文件扩展名         |
| `FileUtil.del(path)`                      | 删除文件或目录         |

示例代码如下：

```java
String basePath = "./data";
String filePath = basePath + "/demo.txt";

if (!FileUtil.exist(basePath)) {
    FileUtil.mkdir(basePath);
    log.info("目录创建成功，basePath={}", basePath);
}

FileUtil.writeUtf8String("Spring Boot 3 集成 Hutool", filePath);

String content = FileUtil.readUtf8String(filePath);
log.info("文件读取内容：{}", content);

String extName = FileUtil.extName(filePath);
log.info("文件扩展名：{}", extName);
```

文件处理需要注意生产环境路径权限。开发环境可以使用项目相对路径，例如 `./data`；生产环境建议使用独立挂载目录，例如 `/data/app-name`，并通过配置文件维护，不要在代码中硬编码绝对路径。

### 加密工具

加密工具主要用于摘要计算、对称加密、非对称加密等场景。常见业务中使用较多的是 MD5、SHA-256、AES 等。Hutool 的 `SecureUtil` 可以快速完成摘要和加密操作。

常用方法如下：

| 方法                       | 说明              |
| -------------------------- | ----------------- |
| `SecureUtil.md5(value)`    | 计算 MD5 摘要     |
| `SecureUtil.sha256(value)` | 计算 SHA-256 摘要 |
| `SecureUtil.aes(key)`      | 创建 AES 加密工具 |
| `SecureUtil.rsa()`         | 创建 RSA 加密工具 |

摘要示例：

```java
String rawPassword = "123456";
String salt = "ateng-demo-salt";

String md5Text = SecureUtil.md5(rawPassword + salt);
String sha256Text = SecureUtil.sha256(rawPassword + salt);

log.info("MD5摘要：{}", md5Text);
log.info("SHA256摘要：{}", sha256Text);
```

AES 示例：

```java
byte[] key = "1234567890123456".getBytes(StandardCharsets.UTF_8);

String content = "需要加密的业务数据";
String encrypted = SecureUtil.aes(key).encryptHex(content);
String decrypted = SecureUtil.aes(key).decryptStr(encrypted);

log.info("AES加密结果：{}", encrypted);
log.info("AES解密结果：{}", decrypted);
```

加密处理需要注意安全边界。MD5 不适合用于高安全级别的密码存储；密码场景建议使用 Spring Security 提供的 `BCryptPasswordEncoder`。Hutool 加密工具更适合签名摘要、简单数据加密、接口验签、兼容历史系统等场景。

### HTTP 工具

HTTP 工具主要用于发起简单的 GET、POST 请求。Hutool 的 `HttpRequest` 和 `HttpUtil` 使用成本低，适合轻量级远程调用、第三方接口验证、内部工具调用等场景。

常用方法如下：

| 方法                         | 说明                                          |
| ---------------------------- | --------------------------------------------- |
| `HttpUtil.get(url)`          | 发起简单 GET 请求                             |
| `HttpUtil.post(url, params)` | 发起简单 POST 请求                            |
| `HttpRequest.get(url)`       | 构造 GET 请求，支持更多配置                   |
| `HttpRequest.post(url)`      | 构造 POST 请求，支持 Header、Body、超时等配置 |
| `timeout(milliseconds)`      | 设置请求超时时间                              |

GET 请求示例：

```java
String url = "https://example.com/api/user?id=10001";

String body = HttpRequest.get(url)
        .timeout(5000)
        .execute()
        .body();

log.info("远程接口响应：{}", body);
```

POST JSON 请求示例：

```java
String url = "https://example.com/api/user";

String requestBody = JSONUtil.toJsonStr(Map.of(
        "username", "Ateng",
        "phone", "13800138000"
));

String responseBody = HttpRequest.post(url)
        .header("Content-Type", "application/json")
        .body(requestBody)
        .timeout(5000)
        .execute()
        .body();

log.info("远程接口响应：{}", responseBody);
```

在正式业务系统中，Hutool HTTP 更适合低复杂度调用。如果远程调用涉及连接池、熔断、重试、负载均衡、服务发现、统一拦截、链路追踪等能力，建议使用 Spring `RestClient`、`WebClient` 或 OpenFeign。Hutool HTTP 可以作为工具类或简单外部接口调用的补充，不建议承担复杂微服务通信职责。



## Spring Boot 3 集成实践

本章节用于说明 Hutool 在 Spring Boot 3 项目中的实际集成方式，重点覆盖参数校验、统一返回、全局异常、日志记录和配置属性读取。这些内容属于项目基础能力，建议在业务模块开发前先统一封装。

本章节涉及的主要文件结构如下：

```text
src/main/java/io/github/atengk
├── common
│   ├── exception
│   │   └── BizException.java
│   ├── result
│   │   ├── Result.java
│   │   └── ResultCode.java
│   └── util
│       ├── UserConvertUtil.java
│       └── UserDesensitizeUtil.java
├── config
│   └── AppProperties.java
└── module
    └── user
        ├── controller
        │   └── UserController.java
        ├── dto
        │   └── UserCreateDTO.java
        ├── service
        │   ├── UserService.java
        │   └── impl
        │       └── UserServiceImpl.java
        └── vo
            └── UserVO.java

src/main/resources
├── application.yml
└── logback-spring.xml
```

### 参数校验处理

参数校验用于在请求进入业务逻辑之前完成基础数据检查，避免业务层出现大量重复的空值判断。Spring Boot 3 使用 `jakarta.validation` 包体系，Controller 层建议通过 `@Validated`、`@Valid`、`@NotBlank`、`@NotNull`、`@Pattern` 等注解完成第一层校验。

Hutool 在参数校验中的定位是补充校验，例如业务层二次判断、字符串工具判断、对象判空、集合判空等，不建议完全替代 Validation 注解。

文件位置：`src/main/java/io/github/atengk/module/user/dto/UserCreateDTO.java`

下面的 DTO 用于接收用户创建请求，并通过 Jakarta Validation 完成基础参数校验。

```java
package io.github.atengk.module.user.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

/**
 * 用户创建请求参数
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
public class UserCreateDTO {

    @NotBlank(message = "用户名不能为空")
    @Length(min = 2, max = 20, message = "用户名长度必须在 2 到 20 个字符之间")
    private String username;

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Min(value = 1, message = "年龄不能小于 1")
    @Max(value = 120, message = "年龄不能大于 120")
    private Integer age;

}
```

Controller 中需要配合 `@Validated` 或 `@Valid` 使用。

```java
@PostMapping("/users")
public Result<UserVO> create(@Validated @RequestBody UserCreateDTO dto) {
    return Result.success(userService.create(dto));
}
```

对于业务层补充校验，可以使用 Hutool 的 `StrUtil`、`ObjectUtil`、`CollUtil`。

```java
if (ObjectUtil.isNull(dto) || StrUtil.isBlank(dto.getUsername())) {
    throw new BizException("用户参数不能为空");
}
```

### 统一返回结果

统一返回结果用于规范接口响应结构，避免不同接口返回格式不一致。建议返回结构至少包含 `code`、`message`、`data`、`timestamp`，业务接口统一返回 `Result<T>`。

文件位置：`src/main/java/io/github/atengk/common/result/ResultCode.java`

下面的枚举用于维护常见响应状态码和提示信息。

```java
package io.github.atengk.common.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 接口响应状态码
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    SUCCESS(200, "操作成功"),

    PARAM_ERROR(400, "参数错误"),

    BIZ_ERROR(500, "业务异常"),

    SYSTEM_ERROR(5000, "系统异常");

    private final Integer code;

    private final String message;

}
```

文件位置：`src/main/java/io/github/atengk/common/result/Result.java`

下面的统一返回类用于封装所有 REST 接口响应结果，时间字段通过 Hutool `DateUtil` 生成。

```java
package io.github.atengk.common.result;

import cn.hutool.core.date.DateUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一接口返回结果
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

    private Integer code;

    private String message;

    private T data;

    private String timestamp;

    /**
     * 返回成功结果
     *
     * @param data 响应数据
     * @return 成功结果
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(
                ResultCode.SUCCESS.getCode(),
                ResultCode.SUCCESS.getMessage(),
                data,
                DateUtil.now()
        );
    }

    /**
     * 返回成功结果
     *
     * @return 成功结果
     */
    public static <T> Result<T> success() {
        return success(null);
    }

    /**
     * 返回失败结果
     *
     * @param message 错误信息
     * @return 失败结果
     */
    public static <T> Result<T> fail(String message) {
        return fail(ResultCode.BIZ_ERROR.getCode(), message);
    }

    /**
     * 返回失败结果
     *
     * @param code    错误码
     * @param message 错误信息
     * @return 失败结果
     */
    public static <T> Result<T> fail(Integer code, String message) {
        return new Result<>(code, message, null, DateUtil.now());
    }

}
```

统一响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 10001,
    "username": "Ateng",
    "phone": "138****8000"
  },
  "timestamp": "2026-05-09 14:30:00"
}
```

### 全局异常处理

全局异常处理用于统一处理参数校验异常、业务异常和系统异常。业务代码中不建议直接返回错误对象，而是抛出业务异常，由全局异常处理器统一转换为接口响应。

文件位置：`src/main/java/io/github/atengk/common/exception/BizException.java`

下面的业务异常类用于表达可预期的业务错误，例如用户不存在、参数非法、文件不存在等。

```java
package io.github.atengk.common.exception;

import io.github.atengk.common.result.ResultCode;
import lombok.Getter;

/**
 * 业务异常
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Getter
public class BizException extends RuntimeException {

    private final Integer code;

    /**
     * 创建业务异常
     *
     * @param message 异常信息
     */
    public BizException(String message) {
        super(message);
        this.code = ResultCode.BIZ_ERROR.getCode();
    }

    /**
     * 创建业务异常
     *
     * @param code    错误码
     * @param message 异常信息
     */
    public BizException(Integer code, String message) {
        super(message);
        this.code = code;
    }

}
```

文件位置：`src/main/java/io/github/atengk/common/exception/GlobalExceptionHandler.java`

下面的全局异常处理器用于统一处理校验异常、业务异常和未捕获异常，并使用 Hutool 简化字符串和集合判断。

```java
package io.github.atengk.common.exception;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.common.result.Result;
import io.github.atengk.common.result.ResultCode;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.Set;

/**
 * 全局异常处理器
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理请求体参数校验异常
     *
     * @param exception 参数校验异常
     * @return 统一返回结果
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException exception) {
        List<FieldError> fieldErrors = exception.getBindingResult().getFieldErrors();
        String message = "参数校验失败";

        if (CollUtil.isNotEmpty(fieldErrors)) {
            message = fieldErrors.stream()
                    .map(FieldError::getDefaultMessage)
                    .filter(StrUtil::isNotBlank)
                    .findFirst()
                    .orElse(message);
        }

        log.warn("请求体参数校验失败，message={}", message);
        return Result.fail(ResultCode.PARAM_ERROR.getCode(), message);
    }

    /**
     * 处理请求参数校验异常
     *
     * @param exception 参数校验异常
     * @return 统一返回结果
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraintViolationException(ConstraintViolationException exception) {
        Set<ConstraintViolation<?>> violations = exception.getConstraintViolations();
        String message = violations.stream()
                .map(ConstraintViolation::getMessage)
                .filter(StrUtil::isNotBlank)
                .findFirst()
                .orElse("参数校验失败");

        log.warn("请求参数校验失败，message={}", message);
        return Result.fail(ResultCode.PARAM_ERROR.getCode(), message);
    }

    /**
     * 处理缺少请求参数异常
     *
     * @param exception 缺少请求参数异常
     * @return 统一返回结果
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<Void> handleMissingServletRequestParameterException(MissingServletRequestParameterException exception) {
        String message = StrUtil.format("缺少必要请求参数：{}", exception.getParameterName());
        log.warn("请求参数缺失，parameter={}", exception.getParameterName());
        return Result.fail(ResultCode.PARAM_ERROR.getCode(), message);
    }

    /**
     * 处理请求体不可读异常
     *
     * @param exception 请求体不可读异常
     * @return 统一返回结果
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleHttpMessageNotReadableException(HttpMessageNotReadableException exception) {
        log.warn("请求体格式错误，message={}", exception.getMessage());
        return Result.fail(ResultCode.PARAM_ERROR.getCode(), "请求体格式错误");
    }

    /**
     * 处理业务异常
     *
     * @param exception 业务异常
     * @return 统一返回结果
     */
    @ExceptionHandler(BizException.class)
    public Result<Void> handleBizException(BizException exception) {
        log.warn("业务异常，code={}，message={}", exception.getCode(), exception.getMessage());
        return Result.fail(exception.getCode(), exception.getMessage());
    }

    /**
     * 处理系统异常
     *
     * @param exception 系统异常
     * @return 统一返回结果
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception exception) {
        log.error("系统异常", exception);
        return Result.fail(ResultCode.SYSTEM_ERROR.getCode(), "系统繁忙，请稍后再试");
    }

}
```

### 日志记录

日志记录用于记录关键业务流程、异常信息、远程调用结果和文件操作结果。Spring Boot 默认使用 Logback，项目中建议通过 `logback-spring.xml` 统一日志格式。

文件位置：`src/main/resources/logback-spring.xml`

下面的日志配置用于输出控制台日志和文件日志，适合开发和基础生产场景。

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>

    <!-- 从 Spring 配置中读取应用名称 -->
    <springProperty scope="context" name="APP_NAME" source="spring.application.name" defaultValue="springboot3-hutool-demo"/>

    <!-- 日志输出目录，生产环境建议挂载到独立磁盘 -->
    <property name="LOG_PATH" value="./logs"/>

    <!-- 控制台日志格式 -->
    <property name="CONSOLE_LOG_PATTERN"
              value="%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"/>

    <!-- 控制台输出 -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>${CONSOLE_LOG_PATTERN}</pattern>
            <charset>UTF-8</charset>
        </encoder>
    </appender>

    <!-- 文件输出，按天滚动 -->
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${LOG_PATH}/${APP_NAME}.log</file>
        <encoder>
            <pattern>${CONSOLE_LOG_PATTERN}</pattern>
            <charset>UTF-8</charset>
        </encoder>

        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <!-- 每天生成一个日志文件 -->
            <fileNamePattern>${LOG_PATH}/${APP_NAME}.%d{yyyy-MM-dd}.log</fileNamePattern>
            <!-- 保留 30 天日志 -->
            <maxHistory>30</maxHistory>
        </rollingPolicy>
    </appender>

    <!-- 项目包日志级别 -->
    <logger name="io.github.atengk" level="INFO"/>

    <!-- 根日志配置 -->
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="FILE"/>
    </root>

</configuration>
```

业务代码中建议记录以下内容：

| 场景                                 | 日志级别 | 示例                                           |
| ------------------------------------ | -------- | ---------------------------------------------- |
| 用户创建、文件上传、远程调用成功     | `info`   | `log.info("用户创建成功，userId={}", userId);` |
| 参数异常、业务异常、可预期失败       | `warn`   | `log.warn("用户不存在，userId={}", userId);`   |
| 系统异常、远程调用异常、文件读写异常 | `error`  | `log.error("文件读取失败", exception);`        |
| 本地调试细节                         | `debug`  | `log.debug("转换前数据：{}", json);`           |

不建议在日志中输出密码、身份证完整号码、完整手机号、Token、密钥、银行卡号等敏感信息。确实需要输出时，应先进行脱敏处理。

### 配置属性读取

配置属性读取用于统一管理业务配置，例如文件目录、HTTP 超时时间、远程接口地址、加密盐值等。建议使用 `@ConfigurationProperties` 绑定配置，避免业务代码中大量散落 `@Value`。

文件位置：`src/main/resources/application.yml`

下面的配置用于支持后续文件上传、接口远程调用和敏感数据处理示例。

```yaml
server:
  # 服务端口
  port: 8080
  servlet:
    # 接口统一前缀
    context-path: /api

spring:
  application:
    # 应用名称
    name: springboot3-hutool-demo
  servlet:
    multipart:
      # 单个上传文件最大大小
      max-file-size: 10MB
      # 单次请求最大大小
      max-request-size: 20MB

app:
  file:
    # 文件上传基础目录
    base-path: ./data/upload
  http:
    # Hutool HTTP 调用默认超时时间，单位毫秒
    timeout: 5000
  remote:
    # 示例远程用户接口，实际项目替换为业务系统地址
    user-url: https://httpbin.org/post
  security:
    # 示例盐值，生产环境建议通过环境变量、配置中心或密钥系统维护
    salt: ateng-demo-salt
```

文件位置：`src/main/java/io/github/atengk/config/AppProperties.java`

下面的配置属性类用于读取 `app.*` 配置，供文件处理、HTTP 调用和加密处理复用。

```java
package io.github.atengk.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 应用业务配置属性
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private FileConfig file = new FileConfig();

    private HttpConfig http = new HttpConfig();

    private RemoteConfig remote = new RemoteConfig();

    private SecurityConfig security = new SecurityConfig();

    /**
     * 文件配置
     *
     * @author Ateng
     * @since 2026-05-09
     */
    @Data
    public static class FileConfig {
        private String basePath;
    }

    /**
     * HTTP 配置
     *
     * @author Ateng
     * @since 2026-05-09
     */
    @Data
    public static class HttpConfig {
        private Integer timeout;
    }

    /**
     * 远程接口配置
     *
     * @author Ateng
     * @since 2026-05-09
     */
    @Data
    public static class RemoteConfig {
        private String userUrl;
    }

    /**
     * 安全配置
     *
     * @author Ateng
     * @since 2026-05-09
     */
    @Data
    public static class SecurityConfig {
        private String salt;
    }

}
```

## 业务开发示例

本章节基于前面的统一返回、参数校验、异常处理、配置属性和 Hutool 常用模块，提供一组常见业务开发示例。示例不依赖数据库，使用内存 Map 模拟用户数据，便于直接复制到基础工程中验证。

### 用户信息处理

用户信息处理示例用于演示 Controller、Service、DTO、VO 的基础分层方式，同时结合 Hutool 完成对象判空、字符串处理、ID 生成、日期格式化和手机号脱敏。

文件位置：`src/main/java/io/github/atengk/module/user/vo/UserVO.java`

下面的 VO 用于返回用户信息，手机号字段使用脱敏后的结果。

```java
package io.github.atengk.module.user.vo;

import lombok.Data;

/**
 * 用户信息响应对象
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
public class UserVO {

    private Long id;

    private String username;

    private String phone;

    private Integer age;

    private String createTime;

}
```

文件位置：`src/main/java/io/github/atengk/module/user/service/UserService.java`

下面的接口定义用户创建、详情查询和列表查询能力。

```java
package io.github.atengk.module.user.service;

import io.github.atengk.module.user.dto.UserCreateDTO;
import io.github.atengk.module.user.vo.UserVO;

import java.util.List;

/**
 * 用户业务服务
 *
 * @author Ateng
 * @since 2026-05-09
 */
public interface UserService {

    /**
     * 创建用户
     *
     * @param dto 用户创建参数
     * @return 用户信息
     */
    UserVO create(UserCreateDTO dto);

    /**
     * 查询用户详情
     *
     * @param id 用户ID
     * @return 用户信息
     */
    UserVO detail(Long id);

    /**
     * 查询用户列表
     *
     * @return 用户列表
     */
    List<UserVO> list();

}
```

文件位置：`src/main/java/io/github/atengk/module/user/service/impl/UserServiceImpl.java`

下面的业务实现类使用 Hutool 完成参数补充校验、ID 生成、日期处理、手机号脱敏和集合处理。

```java
package io.github.atengk.module.user.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.DesensitizedUtil;
import io.github.atengk.common.exception.BizException;
import io.github.atengk.module.user.dto.UserCreateDTO;
import io.github.atengk.module.user.service.UserService;
import io.github.atengk.module.user.vo.UserVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用户业务服务实现
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Service
public class UserServiceImpl implements UserService {

    private final Map<Long, UserVO> userStorage = new ConcurrentHashMap<>();

    /**
     * 创建用户
     *
     * @param dto 用户创建参数
     * @return 用户信息
     */
    @Override
    public UserVO create(UserCreateDTO dto) {
        if (ObjectUtil.isNull(dto) || StrUtil.isBlank(dto.getUsername())) {
            throw new BizException("用户参数不能为空");
        }

        Long userId = IdUtil.getSnowflakeNextId();

        UserVO userVO = new UserVO();
        userVO.setId(userId);
        userVO.setUsername(StrUtil.trim(dto.getUsername()));
        userVO.setPhone(DesensitizedUtil.mobilePhone(dto.getPhone()));
        userVO.setAge(dto.getAge());
        userVO.setCreateTime(DateUtil.now());

        userStorage.put(userId, userVO);
        log.info("用户创建成功，userId={}，username={}", userId, userVO.getUsername());
        return userVO;
    }

    /**
     * 查询用户详情
     *
     * @param id 用户ID
     * @return 用户信息
     */
    @Override
    public UserVO detail(Long id) {
        if (ObjectUtil.isNull(id)) {
            throw new BizException("用户ID不能为空");
        }

        UserVO userVO = userStorage.get(id);
        if (ObjectUtil.isNull(userVO)) {
            log.warn("用户不存在，userId={}", id);
            throw new BizException("用户不存在");
        }

        return userVO;
    }

    /**
     * 查询用户列表
     *
     * @return 用户列表
     */
    @Override
    public List<UserVO> list() {
        List<UserVO> users = new ArrayList<>(userStorage.values());
        if (CollUtil.isEmpty(users)) {
            log.info("当前用户列表为空");
            return CollUtil.newArrayList();
        }
        return users;
    }

}
```

文件位置：`src/main/java/io/github/atengk/module/user/controller/UserController.java`

下面的 Controller 提供用户创建、详情查询和列表查询接口。

```java
package io.github.atengk.module.user.controller;

import io.github.atengk.common.result.Result;
import io.github.atengk.module.user.dto.UserCreateDTO;
import io.github.atengk.module.user.service.UserService;
import io.github.atengk.module.user.vo.UserVO;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户接口
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    /**
     * 创建用户
     *
     * @param dto 用户创建参数
     * @return 用户信息
     */
    @PostMapping
    public Result<UserVO> create(@Validated @RequestBody UserCreateDTO dto) {
        return Result.success(userService.create(dto));
    }

    /**
     * 查询用户详情
     *
     * @param id 用户ID
     * @return 用户信息
     */
    @GetMapping("/{id}")
    public Result<UserVO> detail(@NotNull(message = "用户ID不能为空") @PathVariable Long id) {
        return Result.success(userService.detail(id));
    }

    /**
     * 查询用户列表
     *
     * @return 用户列表
     */
    @GetMapping
    public Result<List<UserVO>> list() {
        return Result.success(userService.list());
    }

}
```

接口调用示例：

```bash
curl -X POST "http://localhost:8080/api/users" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "Ateng",
    "phone": "13800138000",
    "age": 18
  }'
```

响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 1000000000000000000,
    "username": "Ateng",
    "phone": "138****8000",
    "age": 18,
    "createTime": "2026-05-09 15:00:00"
  },
  "timestamp": "2026-05-09 15:00:00"
}
```

### 文件上传与读取

文件上传与读取示例用于演示 Hutool `FileUtil` 在 Spring Boot 文件处理中的使用。上传文件时需要注意文件名安全、目录创建、文件大小限制和路径穿越问题。

文件位置：`src/main/java/io/github/atengk/module/file/controller/FileController.java`

下面的文件接口支持上传文件和读取文本文件内容，文件目录从 `AppProperties` 中读取。

```java
package io.github.atengk.module.file.controller;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.common.exception.BizException;
import io.github.atengk.common.result.Result;
import io.github.atengk.config.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * 文件接口
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/files")
public class FileController {

    private final AppProperties appProperties;

    /**
     * 上传文件
     *
     * @param file 上传文件
     * @return 文件信息
     * @throws IOException 文件读取异常
     */
    @PostMapping
    public Result<Map<String, Object>> upload(@RequestParam("file") MultipartFile file) throws IOException {
        if (ObjectUtil.isNull(file) || file.isEmpty()) {
            throw new BizException("上传文件不能为空");
        }

        String basePath = appProperties.getFile().getBasePath();
        FileUtil.mkdir(basePath);

        String originalName = ObjectUtil.defaultIfNull(file.getOriginalFilename(), "unknown");
        String cleanName = FileUtil.getName(originalName);
        String extName = FileUtil.extName(cleanName);

        String filePrefix = StrUtil.format("{}-{}", DateUtil.format(DateUtil.date(), "yyyyMMddHHmmss"), IdUtil.fastSimpleUUID());
        String storageName = StrUtil.isBlank(extName) ? filePrefix : StrUtil.format("{}.{}", filePrefix, extName);

        File targetFile = FileUtil.file(basePath, storageName);
        FileUtil.writeBytes(file.getBytes(), targetFile);

        log.info("文件上传成功，originalName={}，storageName={}，size={}", cleanName, storageName, file.getSize());

        return Result.success(Map.of(
                "originalName", cleanName,
                "storageName", storageName,
                "size", file.getSize(),
                "path", targetFile.getAbsolutePath()
        ));
    }

    /**
     * 读取文本文件
     *
     * @param storageName 存储文件名
     * @return 文件内容
     */
    @GetMapping("/{storageName}")
    public Result<String> readText(@PathVariable String storageName) {
        if (StrUtil.isBlank(storageName)
                || StrUtil.contains(storageName, "..")
                || StrUtil.contains(storageName, "/")
                || StrUtil.contains(storageName, "\\")) {
            throw new BizException("文件名不合法");
        }

        File file = FileUtil.file(appProperties.getFile().getBasePath(), storageName);
        if (!FileUtil.exist(file)) {
            log.warn("文件不存在，storageName={}", storageName);
            throw new BizException("文件不存在");
        }

        String content = FileUtil.readUtf8String(file);
        log.info("文件读取成功，storageName={}", storageName);
        return Result.success(content);
    }

}
```

上传文件示例：

```bash
curl -X POST "http://localhost:8080/api/files" \
  -F "file=@./demo.txt"
```

读取文本文件示例：

```bash
curl -X GET "http://localhost:8080/api/files/20260509150000-xxxx.txt"
```

文件上传时不建议直接使用原始文件名作为存储文件名。示例中通过时间戳和 UUID 生成存储文件名，可以降低重名覆盖风险。

### 接口远程调用

接口远程调用示例用于演示 Hutool `HttpRequest` 发送 HTTP 请求。适合简单第三方接口调用、内部工具接口调用、临时数据同步等场景。复杂微服务通信仍建议使用 OpenFeign、RestClient 或 WebClient。

文件位置：`src/main/java/io/github/atengk/module/remote/controller/RemoteCallController.java`

下面的接口将用户数据转换为 JSON 后，通过 Hutool HTTP 发送到远程地址。

```java
package io.github.atengk.module.remote.controller;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import io.github.atengk.common.exception.BizException;
import io.github.atengk.common.result.Result;
import io.github.atengk.config.AppProperties;
import io.github.atengk.module.user.dto.UserCreateDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 远程调用接口
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/remote")
public class RemoteCallController {

    private final AppProperties appProperties;

    /**
     * 推送用户数据
     *
     * @param dto 用户创建参数
     * @return 远程接口响应
     */
    @PostMapping("/users")
    public Result<String> pushUser(@Validated @RequestBody UserCreateDTO dto) {
        String url = appProperties.getRemote().getUserUrl();
        Integer timeout = ObjectUtil.defaultIfNull(appProperties.getHttp().getTimeout(), 5000);
        String requestBody = JSONUtil.toJsonStr(dto);

        log.info("开始调用远程用户接口，url={}", url);

        try (HttpResponse response = HttpRequest.post(url)
                .header("Content-Type", "application/json")
                .body(requestBody)
                .timeout(timeout)
                .execute()) {

            if (!response.isOk()) {
                log.warn("远程用户接口调用失败，status={}，body={}", response.getStatus(), response.body());
                throw new BizException("远程接口调用失败");
            }

            log.info("远程用户接口调用成功，status={}", response.getStatus());
            return Result.success(response.body());
        } catch (BizException exception) {
            throw exception;
        } catch (Exception exception) {
            log.error("远程用户接口调用异常", exception);
            throw new BizException("远程接口调用异常");
        }
    }

}
```

调用示例：

```bash
curl -X POST "http://localhost:8080/api/remote/users" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "Ateng",
    "phone": "13800138000",
    "age": 18
  }'
```

使用 Hutool HTTP 时建议统一设置超时时间，避免远程接口无响应导致请求线程长时间阻塞。生产项目中还应结合业务情况处理重试、熔断、限流、日志脱敏和链路追踪。

### 数据格式转换

数据格式转换用于处理 DTO、VO、JSON、Map、List 等数据结构之间的转换。简单对象复制可以使用 Hutool `BeanUtil`，JSON 字符串转换可以使用 `JSONUtil`，集合转换可以结合 `CollUtil` 和 Stream。

文件位置：`src/main/java/io/github/atengk/common/util/UserConvertUtil.java`

下面的工具类演示用户 DTO 到 VO、JSON 到 DTO、DTO 列表到 VO 列表的转换。

```java
package io.github.atengk.common.util;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.DesensitizedUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import io.github.atengk.module.user.dto.UserCreateDTO;
import io.github.atengk.module.user.vo.UserVO;
import lombok.experimental.UtilityClass;

import java.util.List;

/**
 * 用户数据转换工具类
 *
 * @author Ateng
 * @since 2026-05-09
 */
@UtilityClass
public class UserConvertUtil {

    /**
     * DTO 转 VO
     *
     * @param dto 用户创建参数
     * @return 用户响应对象
     */
    public UserVO toVO(UserCreateDTO dto) {
        if (ObjectUtil.isNull(dto)) {
            return null;
        }

        UserVO userVO = BeanUtil.copyProperties(dto, UserVO.class);
        userVO.setId(IdUtil.getSnowflakeNextId());
        userVO.setPhone(DesensitizedUtil.mobilePhone(dto.getPhone()));
        userVO.setCreateTime(DateUtil.now());
        return userVO;
    }

    /**
     * JSON 转 DTO
     *
     * @param json JSON 字符串
     * @return 用户创建参数
     */
    public UserCreateDTO jsonToDTO(String json) {
        if (!JSONUtil.isTypeJSON(json)) {
            return null;
        }
        return JSONUtil.toBean(json, UserCreateDTO.class);
    }

    /**
     * DTO 列表转 VO 列表
     *
     * @param dtoList DTO 列表
     * @return VO 列表
     */
    public List<UserVO> toVOList(List<UserCreateDTO> dtoList) {
        if (CollUtil.isEmpty(dtoList)) {
            return CollUtil.newArrayList();
        }

        return dtoList.stream()
                .map(UserConvertUtil::toVO)
                .toList();
    }

}
```

使用示例：

```java
String json = """
        {
          "username": "Ateng",
          "phone": "13800138000",
          "age": 18
        }
        """;

UserCreateDTO dto = UserConvertUtil.jsonToDTO(json);
UserVO userVO = UserConvertUtil.toVO(dto);
```

数据转换时需要注意：简单字段复制可以使用 `BeanUtil.copyProperties`，但复杂业务转换不建议完全依赖自动复制。例如金额单位转换、枚举转换、状态翻译、权限字段过滤等，应显式编写转换逻辑。

### 敏感信息脱敏

敏感信息脱敏用于保护手机号、邮箱、身份证号、银行卡号、地址等隐私数据。日志输出、接口响应、导出文件、远程调用前都应根据业务场景决定是否脱敏。

文件位置：`src/main/java/io/github/atengk/common/util/UserDesensitizeUtil.java`

下面的脱敏工具类基于 Hutool `DesensitizedUtil` 封装常见敏感字段处理。

```java
package io.github.atengk.common.util;

import cn.hutool.core.util.DesensitizedUtil;
import cn.hutool.core.util.StrUtil;
import lombok.experimental.UtilityClass;

/**
 * 用户敏感信息脱敏工具类
 *
 * @author Ateng
 * @since 2026-05-09
 */
@UtilityClass
public class UserDesensitizeUtil {

    /**
     * 手机号脱敏
     *
     * @param phone 手机号
     * @return 脱敏手机号
     */
    public String mobilePhone(String phone) {
        if (StrUtil.isBlank(phone)) {
            return StrUtil.EMPTY;
        }
        return DesensitizedUtil.mobilePhone(phone);
    }

    /**
     * 邮箱脱敏
     *
     * @param email 邮箱
     * @return 脱敏邮箱
     */
    public String email(String email) {
        if (StrUtil.isBlank(email)) {
            return StrUtil.EMPTY;
        }
        return DesensitizedUtil.email(email);
    }

    /**
     * 身份证号脱敏
     *
     * @param idCard 身份证号
     * @return 脱敏身份证号
     */
    public String idCard(String idCard) {
        if (StrUtil.isBlank(idCard)) {
            return StrUtil.EMPTY;
        }
        return DesensitizedUtil.idCardNum(idCard, 3, 4);
    }

    /**
     * 银行卡号脱敏
     *
     * @param bankCard 银行卡号
     * @return 脱敏银行卡号
     */
    public String bankCard(String bankCard) {
        if (StrUtil.isBlank(bankCard)) {
            return StrUtil.EMPTY;
        }
        return DesensitizedUtil.bankCard(bankCard);
    }

}
```

脱敏使用示例：

```java
String phone = UserDesensitizeUtil.mobilePhone("13800138000");
String email = UserDesensitizeUtil.email("ateng@example.com");
String idCard = UserDesensitizeUtil.idCard("110101199001011234");

log.info("脱敏手机号：{}，脱敏邮箱：{}，脱敏身份证：{}", phone, email, idCard);
```

接口响应中建议返回脱敏后的手机号、邮箱等信息；业务内部计算、权限校验、数据落库仍应使用原始值。日志中原则上不输出原始敏感数据，尤其是密码、Token、密钥、身份证号、银行卡号等字段。



## 工具类封装规范

工具类封装用于统一项目中通用能力的调用方式，减少业务代码中的重复判断、重复转换和重复异常处理。Hutool 已经提供了大量工具类，项目中不应为了“封装而封装”，只有当业务规则、异常处理、日志记录或配置读取需要统一时，才建议在 Hutool 基础上做二次封装。

### 通用工具类设计

通用工具类应只处理无状态、可复用、低业务耦合的逻辑，例如字符串清洗、文件名处理、脱敏、摘要生成、对象转换、时间格式化等。强业务规则不建议放入通用工具类，应放在 Service 或 Domain 层处理。

推荐工具类目录如下：

```text
src/main/java/io/github/atengk/common
├── util
│   ├── SafeFileUtil.java
│   ├── DigestUtil.java
│   ├── DesensitizeHelper.java
│   └── ConvertHelper.java
└── exception
    └── BizException.java
```

工具类设计建议遵循以下原则：

| 原则     | 说明                                           |
| -------- | ---------------------------------------------- |
| 无状态   | 不保存请求上下文、用户状态、临时数据           |
| 单一职责 | 一个工具类只处理一类问题，例如文件、加密、脱敏 |
| 入参明确 | 不依赖隐藏上下文，方法参数表达完整意图         |
| 返回稳定 | 对空值、异常值有明确处理策略                   |
| 异常清晰 | 不吞异常，必要时转换为业务异常                 |
| 日志克制 | 工具类只记录关键失败，不输出敏感信息           |

文件位置：`src/main/java/io/github/atengk/common/util/SafeFileUtil.java`

下面的工具类用于安全处理文件名和文件路径，基于 Hutool `FileUtil`、`StrUtil`、`IdUtil` 做轻量封装。

```java
package io.github.atengk.common.util;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.common.exception.BizException;
import lombok.experimental.UtilityClass;

import java.io.File;

/**
 * 安全文件工具类
 *
 * @author Ateng
 * @since 2026-05-09
 */
@UtilityClass
public class SafeFileUtil {

    /**
     * 清理原始文件名
     *
     * @param originalName 原始文件名
     * @return 安全文件名
     */
    public String cleanFileName(String originalName) {
        if (StrUtil.isBlank(originalName)) {
            return "unknown";
        }
        return FileUtil.getName(originalName);
    }

    /**
     * 生成存储文件名
     *
     * @param originalName 原始文件名
     * @return 存储文件名
     */
    public String generateStorageName(String originalName) {
        String cleanName = cleanFileName(originalName);
        String extName = FileUtil.extName(cleanName);
        String prefix = StrUtil.format("{}-{}", DateUtil.format(DateUtil.date(), "yyyyMMddHHmmss"), IdUtil.fastSimpleUUID());

        if (StrUtil.isBlank(extName)) {
            return prefix;
        }
        return StrUtil.format("{}.{}", prefix, extName);
    }

    /**
     * 校验文件名是否合法
     *
     * @param fileName 文件名
     */
    public void checkFileName(String fileName) {
        if (StrUtil.isBlank(fileName)
                || StrUtil.contains(fileName, "..")
                || StrUtil.contains(fileName, "/")
                || StrUtil.contains(fileName, "\\")) {
            throw new BizException("文件名不合法");
        }
    }

    /**
     * 构建安全文件对象
     *
     * @param basePath 基础目录
     * @param fileName 文件名
     * @return 文件对象
     */
    public File buildFile(String basePath, String fileName) {
        if (StrUtil.isBlank(basePath)) {
            throw new BizException("文件基础目录不能为空");
        }
        checkFileName(fileName);
        FileUtil.mkdir(basePath);
        return FileUtil.file(basePath, fileName);
    }

}
```

文件位置：`src/main/java/io/github/atengk/common/util/DigestUtil.java`

下面的摘要工具类用于统一摘要生成方式，避免业务代码中散落盐值拼接和摘要算法选择逻辑。

```java
package io.github.atengk.common.util;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import io.github.atengk.common.exception.BizException;
import lombok.experimental.UtilityClass;

/**
 * 摘要工具类
 *
 * @author Ateng
 * @since 2026-05-09
 */
@UtilityClass
public class DigestUtil {

    /**
     * 生成 SHA-256 摘要
     *
     * @param rawText 原始文本
     * @param salt    盐值
     * @return 摘要字符串
     */
    public String sha256WithSalt(String rawText, String salt) {
        if (StrUtil.hasBlank(rawText, salt)) {
            throw new BizException("摘要原文和盐值不能为空");
        }
        return SecureUtil.sha256(rawText + salt);
    }

    /**
     * 生成 MD5 摘要
     *
     * @param rawText 原始文本
     * @param salt    盐值
     * @return 摘要字符串
     */
    public String md5WithSalt(String rawText, String salt) {
        if (StrUtil.hasBlank(rawText, salt)) {
            throw new BizException("摘要原文和盐值不能为空");
        }
        return SecureUtil.md5(rawText + salt);
    }

}
```

工具类封装后，业务代码应调用项目内部工具类，而不是在每个业务类中重复拼接文件名、重复写摘要算法、重复处理路径校验。

```java
String storageName = SafeFileUtil.generateStorageName(file.getOriginalFilename());

String sign = DigestUtil.sha256WithSalt("业务原文", appProperties.getSecurity().getSalt());
```

### Hutool 二次封装原则

Hutool 二次封装不是简单地把 `StrUtil.isBlank` 包一层 `StringUtils.isBlank`。只有当项目需要统一业务规则、异常处理、默认值、日志、安全限制或配置读取时，才应该二次封装。

建议二次封装的场景如下：

| 场景           | 是否建议封装 | 原因                                 |
| -------------- | ------------ | ------------------------------------ |
| 字符串判空     | 不建议       | `StrUtil.isBlank` 已足够清晰         |
| 集合判空       | 不建议       | `CollUtil.isEmpty` 已足够清晰        |
| 文件名安全处理 | 建议         | 涉及路径穿越、重名、扩展名处理       |
| 敏感信息脱敏   | 建议         | 需要统一手机号、邮箱、身份证脱敏规则 |
| 摘要签名       | 建议         | 涉及盐值、算法、验签规则             |
| HTTP 远程调用  | 视情况       | 简单调用可直接使用，复杂调用建议封装 |
| JSON 转换      | 视情况       | 简单转换直接使用，复杂转换应显式处理 |

不推荐的封装方式如下：

```java
public static boolean isBlank(String value) {
    return StrUtil.isBlank(value);
}
```

这种封装没有增加业务语义，只会增加维护成本。

推荐的封装方式如下：

```java
public static String cleanUsername(String username) {
    if (StrUtil.isBlank(username)) {
        throw new BizException("用户名不能为空");
    }
    return StrUtil.trim(username);
}
```

这种封装加入了业务语义、异常策略和数据清洗逻辑，适合沉淀到项目工具类或领域服务中。

### 异常与日志处理规范

工具类中的异常处理应保持清晰，不应吞掉异常后返回空值，也不应在工具类中过度捕获所有异常。对调用方可恢复或可预期的问题，可以抛出业务异常；对系统异常，应保留原始异常信息并在上层统一处理。

推荐处理方式如下：

| 场景          | 处理方式                                 |
| ------------- | ---------------------------------------- |
| 参数为空      | 抛出 `BizException` 或由调用方提前校验   |
| 文件不存在    | 抛出 `BizException("文件不存在")`        |
| 文件读写失败  | 记录错误日志后抛出 `BizException`        |
| 远程接口异常  | 记录状态码和简要响应，避免输出敏感响应体 |
| JSON 格式错误 | 返回明确错误，避免直接暴露底层异常堆栈   |

文件位置：`src/main/java/io/github/atengk/common/util/TextFileUtil.java`

下面的工具类用于演示文件读写中的异常与日志处理方式。

```java
package io.github.atengk.common.util;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.common.exception.BizException;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.charset.StandardCharsets;

/**
 * 文本文件工具类
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@UtilityClass
public class TextFileUtil {

    /**
     * 写入 UTF-8 文本
     *
     * @param file    文件对象
     * @param content 文件内容
     */
    public void writeUtf8(File file, String content) {
        if (file == null) {
            throw new BizException("文件对象不能为空");
        }

        try {
            FileUtil.writeString(StrUtil.nullToEmpty(content), file, StandardCharsets.UTF_8);
            log.info("文本文件写入成功，path={}", file.getAbsolutePath());
        } catch (Exception exception) {
            log.error("文本文件写入失败，path={}", file.getAbsolutePath(), exception);
            throw new BizException("文本文件写入失败");
        }
    }

    /**
     * 读取 UTF-8 文本
     *
     * @param file 文件对象
     * @return 文件内容
     */
    public String readUtf8(File file) {
        if (file == null || !FileUtil.exist(file)) {
            throw new BizException("文件不存在");
        }

        try {
            String content = FileUtil.readString(file, StandardCharsets.UTF_8);
            log.info("文本文件读取成功，path={}", file.getAbsolutePath());
            return content;
        } catch (Exception exception) {
            log.error("文本文件读取失败，path={}", file.getAbsolutePath(), exception);
            throw new BizException("文本文件读取失败");
        }
    }

}
```

日志处理注意事项：

1. 日志中不要输出密码、Token、密钥、身份证号、银行卡号等敏感信息。
2. 业务异常使用 `warn`，系统异常使用 `error`。
3. 成功操作只记录关键字段，不记录完整大对象。
4. 工具类日志应克制，避免高频方法产生大量日志。
5. 异常日志应保留异常对象，便于排查堆栈。

## 测试与验证

测试与验证用于确认 Hutool 集成、工具封装、接口返回、异常处理和业务流程是否符合预期。建议至少覆盖工具方法单元测试、Controller 接口测试和核心业务流程测试。

测试依赖通常由 `spring-boot-starter-test` 提供，不需要额外引入 JUnit 依赖。

```xml
<!-- 测试依赖：包含 JUnit Jupiter、Spring Test、MockMvc、AssertJ 等能力 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

### 单元测试

单元测试用于验证工具类或单个业务类的行为，不依赖 Web 容器。对于 Hutool 二次封装工具类，应重点测试正常输入、空值输入、非法输入和边界输入。

文件位置：`src/test/java/io/github/atengk/common/util/SafeFileUtilTest.java`

下面的测试类用于验证文件名清理、存储文件名生成和非法文件名校验。

```java
package io.github.atengk.common.util;

import io.github.atengk.common.exception.BizException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * 安全文件工具类测试
 *
 * @author Ateng
 * @since 2026-05-09
 */
class SafeFileUtilTest {

    @Test
    void cleanFileNameShouldReturnFileName() {
        String fileName = SafeFileUtil.cleanFileName("../demo.txt");
        Assertions.assertEquals("demo.txt", fileName);
    }

    @Test
    void generateStorageNameShouldKeepExtension() {
        String storageName = SafeFileUtil.generateStorageName("demo.txt");
        Assertions.assertTrue(storageName.endsWith(".txt"));
    }

    @Test
    void checkFileNameShouldThrowExceptionWhenPathTraversal() {
        BizException exception = Assertions.assertThrows(
                BizException.class,
                () -> SafeFileUtil.checkFileName("../demo.txt")
        );
        Assertions.assertEquals("文件名不合法", exception.getMessage());
    }

}
```

文件位置：`src/test/java/io/github/atengk/common/util/DigestUtilTest.java`

下面的测试类用于验证摘要工具类的正常处理和异常处理。

```java
package io.github.atengk.common.util;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.common.exception.BizException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * 摘要工具类测试
 *
 * @author Ateng
 * @since 2026-05-09
 */
class DigestUtilTest {

    @Test
    void sha256WithSaltShouldReturnDigest() {
        String digest = DigestUtil.sha256WithSalt("Ateng", "salt");
        Assertions.assertTrue(StrUtil.isNotBlank(digest));
        Assertions.assertEquals(64, digest.length());
    }

    @Test
    void md5WithSaltShouldReturnDigest() {
        String digest = DigestUtil.md5WithSalt("Ateng", "salt");
        Assertions.assertTrue(StrUtil.isNotBlank(digest));
        Assertions.assertEquals(32, digest.length());
    }

    @Test
    void digestShouldThrowExceptionWhenRawTextIsBlank() {
        BizException exception = Assertions.assertThrows(
                BizException.class,
                () -> DigestUtil.sha256WithSalt("", "salt")
        );
        Assertions.assertEquals("摘要原文和盐值不能为空", exception.getMessage());
    }

}
```

执行单元测试：

```bash
mvn test -Dtest=SafeFileUtilTest,DigestUtilTest
```

该命令只执行指定测试类，适合在开发阶段快速验证工具类行为。

### 接口测试

接口测试用于验证 Controller、参数校验、统一返回、全局异常处理和业务流程是否能正常协作。Spring Boot 项目中可以使用 `MockMvc` 完成轻量级接口测试。

文件位置：`src/test/java/io/github/atengk/module/user/controller/UserControllerTest.java`

下面的接口测试用于验证用户创建成功、参数校验失败和用户列表查询。

```java
package io.github.atengk.module.user.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 用户接口测试
 *
 * @author Ateng
 * @since 2026-05-09
 */
@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createShouldReturnSuccessWhenParamValid() throws Exception {
        String requestBody = """
                {
                  "username": "Ateng",
                  "phone": "13800138000",
                  "age": 18
                }
                """;

        mockMvc.perform(post("/api/users")
                        .contextPath("/api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andExpect(jsonPath("$.data.username").value("Ateng"))
                .andExpect(jsonPath("$.data.phone").value("138****8000"));
    }

    @Test
    void createShouldReturnParamErrorWhenPhoneInvalid() throws Exception {
        String requestBody = """
                {
                  "username": "Ateng",
                  "phone": "123",
                  "age": 18
                }
                """;

        mockMvc.perform(post("/api/users")
                        .contextPath("/api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("手机号格式不正确"));
    }

    @Test
    void listShouldReturnSuccess() throws Exception {
        mockMvc.perform(get("/api/users").contextPath("/api"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

}
```

执行接口测试：

```bash
mvn test -Dtest=UserControllerTest
```

如果项目未配置 `server.servlet.context-path=/api`，测试中的请求地址可以改为 `/users`，并移除 `.contextPath("/api")`。

### 工具方法验证

工具方法验证可以通过单元测试、临时测试类或 Spring Boot 测试完成。对于不依赖 Spring 容器的 Hutool 工具方法，建议使用普通 JUnit 测试；对于依赖配置属性、Bean 注入、环境变量的工具方法，建议使用 `@SpringBootTest`。

文件位置：`src/test/java/io/github/atengk/HutoolModuleTests.java`

下面的测试类用于验证 Hutool 字符串、集合、日期、JSON、文件、加密等常用能力是否可用。

```java
package io.github.atengk;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.json.JSONUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Hutool 常用模块验证测试
 *
 * @author Ateng
 * @since 2026-05-09
 */
class HutoolModuleTests {

    @Test
    void stringModuleShouldWork() {
        Assertions.assertTrue(StrUtil.isBlank(" "));
        Assertions.assertEquals("Hello Ateng", StrUtil.format("Hello {}", "Ateng"));
    }

    @Test
    void collectionModuleShouldWork() {
        List<String> names = CollUtil.newArrayList("Ateng", "Spring Boot", "Hutool");

        Assertions.assertTrue(CollUtil.isNotEmpty(names));
        Assertions.assertEquals("Ateng,Spring Boot,Hutool", CollUtil.join(names, ","));
    }

    @Test
    void dateModuleShouldWork() {
        String now = DateUtil.now();

        Assertions.assertTrue(StrUtil.isNotBlank(now));
        Assertions.assertTrue(now.length() >= 19);
    }

    @Test
    void jsonModuleShouldWork() {
        Map<String, Object> user = Map.of(
                "username", "Ateng",
                "age", 18
        );

        String json = JSONUtil.toJsonStr(user);

        Assertions.assertTrue(JSONUtil.isTypeJSON(json));
        Assertions.assertTrue(StrUtil.contains(json, "Ateng"));
    }

    @Test
    void fileModuleShouldWork() {
        File file = FileUtil.file("./target/hutool-test/demo.txt");

        FileUtil.writeUtf8String("Spring Boot 3 集成 Hutool", file);
        String content = FileUtil.readUtf8String(file);

        Assertions.assertEquals("Spring Boot 3 集成 Hutool", content);
    }

    @Test
    void cryptoModuleShouldWork() {
        String md5 = SecureUtil.md5("Ateng");

        Assertions.assertTrue(StrUtil.isNotBlank(md5));
        Assertions.assertEquals(32, md5.length());
    }

}
```

执行全部测试：

```bash
mvn clean test
```

验证通过后，说明 Spring Boot 3 基础工程、Hutool 依赖、工具类封装、接口校验和核心业务示例已经可以正常运行。

## 开发注意事项

开发注意事项用于约束 Hutool 在项目中的使用范围，避免出现工具类滥用、框架能力冲突、版本不兼容和代码可读性下降等问题。Hutool 应作为开发效率工具，而不是替代 Spring Boot 3 体系的基础框架。

### Hutool 使用边界

Hutool 的优势是工具方法丰富、调用简单、覆盖面广，但它不应该替代 Spring Boot 的核心框架能力。项目中应明确哪些场景适合使用 Hutool，哪些场景应优先使用 Spring、JDK 或专用组件。

适合使用 Hutool 的场景：

| 场景                           | 推荐工具                  |
| ------------------------------ | ------------------------- |
| 字符串判空、格式化、截取       | `StrUtil`                 |
| 集合判空、快速构造、拼接       | `CollUtil`                |
| 日期格式化、日期偏移、时间范围 | `DateUtil`                |
| 文件读写、文件名处理、目录创建 | `FileUtil`                |
| 简单 JSON 转换                 | `JSONUtil`                |
| 摘要、简单加密、签名           | `SecureUtil`              |
| 简单 HTTP 调用                 | `HttpRequest`、`HttpUtil` |
| 手机号、邮箱、身份证脱敏       | `DesensitizedUtil`        |

不建议优先使用 Hutool 的场景：

| 场景                | 推荐方案                                    |
| ------------------- | ------------------------------------------- |
| Controller 参数校验 | `jakarta.validation`                        |
| 接口 JSON 序列化    | Spring Boot 默认 Jackson                    |
| 复杂 HTTP 客户端    | `RestClient`、`WebClient`、OpenFeign        |
| 密码存储            | Spring Security `BCryptPasswordEncoder`     |
| Bean 生命周期管理   | Spring IoC                                  |
| 事务控制            | Spring Transaction                          |
| 数据访问            | MyBatis-Plus、Spring Data JPA、JdbcTemplate |
| 配置绑定            | `@ConfigurationProperties`                  |

判断是否使用 Hutool，可以按以下规则处理：

1. 如果是 Spring Boot 核心能力，优先使用 Spring。
2. 如果是 JDK 已经清晰表达的简单能力，可以直接使用 JDK。
3. 如果 Hutool 能显著减少重复代码，且不会影响框架边界，可以使用 Hutool。
4. 如果需要统一业务规则，应在 Hutool 基础上做轻量封装。
5. 如果封装后没有增加业务语义，不建议封装。

### Spring 原生能力优先级

Spring Boot 3 项目应优先使用 Spring 原生能力处理框架级问题。Hutool 适合解决局部工具问题，不适合接管应用架构。

推荐优先级如下：

| 能力类型  | 优先方案                           | Hutool 定位                 |
| --------- | ---------------------------------- | --------------------------- |
| 参数校验  | `jakarta.validation`               | 业务层补充判断              |
| 配置读取  | `@ConfigurationProperties`         | 不建议替代                  |
| JSON 响应 | Jackson                            | 辅助内部转换                |
| Web 接口  | Spring MVC                         | 不替代                      |
| 异常处理  | `@RestControllerAdvice`            | 辅助格式化文本              |
| 日志      | SLF4J + Logback                    | 不替代                      |
| 文件上传  | `MultipartFile`                    | 辅助文件保存                |
| 远程调用  | RestClient / WebClient / OpenFeign | 简单调用可用                |
| 密码处理  | Spring Security                    | 不建议使用 MD5/SHA 直接存储 |

示例：参数校验应优先这样写。

```java
@NotBlank(message = "用户名不能为空")
private String username;
```

不建议在 Controller 中只依赖 Hutool 手写校验。

```java
if (StrUtil.isBlank(dto.getUsername())) {
    throw new BizException("用户名不能为空");
}
```

后者可以作为业务层补充校验，但不应替代标准参数校验注解。

### 依赖版本兼容性

Spring Boot 3 使用 Jakarta EE 体系，很多依赖包从 `javax.*` 迁移到了 `jakarta.*`。引入第三方依赖时，需要确认依赖版本是否支持 Spring Boot 3，尤其是 Web、Validation、Servlet、JPA、Security 等相关组件。

依赖管理建议如下：

| 规则                             | 说明                                                |
| -------------------------------- | --------------------------------------------------- |
| Spring 依赖交给 Spring Boot 管理 | 不手动覆盖 Spring Framework、Jackson、Tomcat 等版本 |
| Hutool 版本集中管理              | 在 `properties` 中维护 `hutool.version`             |
| 避免重复版本                     | 多模块项目通过父工程统一版本                        |
| 谨慎升级大版本                   | Hutool、Spring Boot 大版本升级前先回归测试          |
| 定期检查依赖树                   | 使用 `mvn dependency:tree` 排查冲突                 |

检查 Hutool 依赖：

```bash
mvn dependency:tree -Dincludes=cn.hutool
```

检查 Spring 相关依赖：

```bash
mvn dependency:tree -Dincludes=org.springframework
```

检查是否存在旧 `javax` 依赖：

```bash
mvn dependency:tree | grep javax
```

如果项目使用 Windows PowerShell，可以使用：

```powershell
mvn dependency:tree | Select-String javax
```

依赖兼容性问题常见表现包括启动失败、类找不到、方法找不到、参数校验不生效、Servlet 相关类冲突等。遇到这类问题时，应优先检查依赖树，而不是直接修改业务代码。

### 代码可读性与维护性

Hutool 可以减少样板代码，但过度使用会降低代码可读性。项目中应保持“业务语义优先”，工具类调用只是实现手段，不能让业务逻辑变成难以理解的工具方法堆叠。

推荐写法：

```java
if (StrUtil.isBlank(username)) {
    throw new BizException("用户名不能为空");
}

String cleanUsername = StrUtil.trim(username);
```

不推荐写法：

```java
String cleanUsername = ObjectUtil.defaultIfNull(StrUtil.trim(username), "default");
```

后者虽然短，但业务含义不清晰，默认值来源也不明确。

代码维护建议如下：

1. 业务规则显式表达，不要过度压缩成一行工具调用。
2. Hutool 工具方法用于降低重复代码，不用于隐藏复杂业务逻辑。
3. 公共工具类应保持稳定，修改前评估影响范围。
4. 涉及安全、金额、权限、状态流转的逻辑不要放入普通工具类。
5. 日志内容应简洁、可排查、不过度暴露敏感信息。
6. 对 Hutool 二次封装的方法必须有明确业务语义。
7. 单元测试应覆盖工具类的正常、异常和边界输入。

最终建议是：Spring Boot 3 负责应用架构和框架能力，Hutool 负责日常开发工具能力。两者职责清晰时，项目代码会更简洁，也更容易维护。