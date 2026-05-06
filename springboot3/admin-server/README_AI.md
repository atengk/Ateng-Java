# Spring Boot Admin



## 项目概述

Spring Boot Admin 是用于集中管理和监控 Spring Boot 应用的可视化平台。它通过客户端主动注册或服务发现的方式接入应用实例，并基于 Spring Boot Actuator 暴露的端点采集健康状态、指标、日志、环境变量、线程、JVM 等运行信息。官方文档说明，Spring Boot Admin Server 会轮询已注册应用的健康和指标数据，并在 Web Dashboard 中聚合展示，同时支持通过 Eureka、Consul 等服务发现方式接入应用。([Spring Boot Admin](https://docs.spring-boot-admin.com/3.5.5/docs/installation-and-setup/))

### 功能定位

Spring Boot Admin 的核心定位是“应用级运维管理控制台”，主要解决多应用、多实例场景下运行状态分散、Actuator 端点访问不统一、日志级别调整不便、健康检查缺少集中入口等问题。

在 SpringBoot3 项目中，Spring Boot Admin 通常由两部分组成：`Admin Server` 作为监控服务端，负责提供 UI 页面、实例注册中心、状态轮询和通知能力；`Admin Client` 集成在业务服务中，负责将当前应用的访问地址、Actuator 地址、应用名称、实例元数据等信息注册到 Admin Server。

它不是 Prometheus、Grafana、SkyWalking、Datadog 这类完整可观测性平台的替代品。更合理的定位是轻量级应用运行管理入口，适合快速查看应用健康状态、端点信息、日志级别和基础运行指标。官方文档也明确说明，Spring Boot Admin 更适合提供轻量、应用中心化视图，生产级复杂监控仍建议与 Grafana、Datadog、Instana 等平台配合使用。([Spring Boot Admin](https://docs.spring-boot-admin.com/3.5.5/docs/installation-and-setup/))

常见功能包括：

| 功能              | 说明                                                         |
| ----------------- | ------------------------------------------------------------ |
| 应用实例注册      | 业务服务通过 Admin Client 或服务发现注册到 Admin Server      |
| 健康状态监控      | 查看 `UP`、`DOWN`、`OUT_OF_SERVICE` 等实例状态               |
| Actuator 端点聚合 | 集中访问 `health`、`info`、`metrics`、`env`、`beans`、`loggers` 等端点 |
| JVM 运行信息      | 查看内存、线程、GC、类加载等基础运行指标                     |
| 日志级别管理      | 在线查看和调整应用 Logger 级别                               |
| 应用上下线通知    | 支持服务状态变化后的通知扩展                                 |
| 多环境管理        | 可按 dev、test、prod 等环境区分服务实例                      |

### 应用场景

Spring Boot Admin 适用于 SpringBoot3 后端服务较多、需要集中查看应用运行状态的场景。对于单体应用，它可以作为 Actuator 的可视化入口；对于微服务系统，它可以作为研发和运维人员的应用状态总览页面。

典型使用场景如下：

| 场景           | 说明                                                         |
| -------------- | ------------------------------------------------------------ |
| 开发环境调试   | 开发人员快速查看本地或测试环境服务是否启动、Actuator 是否正常暴露 |
| 测试环境联调   | 测试人员查看多个服务实例状态，确认依赖服务是否在线           |
| 微服务运行监控 | 聚合展示订单、用户、认证、网关等多个服务的健康状态           |
| 应用故障排查   | 通过 `health`、`metrics`、`loggers`、`env` 等端点辅助定位问题 |
| 临时日志调整   | 在不重启服务的情况下临时调整指定包或类的日志级别             |
| 服务上下线感知 | 服务状态变化后，通过邮件、Webhook 或自定义通知方式提醒相关人员 |
| 多环境运维入口 | 分别部署 dev、test、prod Admin Server，避免不同环境实例混淆  |

在生产环境中，Spring Boot Admin 不建议裸奔暴露，应配合 Spring Security、反向代理访问控制、Actuator 端点最小化暴露、网络白名单等方式使用。

## 环境准备

环境准备用于统一项目运行版本、模块职责和依赖管理方式。Spring Boot Admin 依赖 Spring Boot 与 Actuator 体系，版本不匹配容易导致启动失败、端点不可见、客户端注册异常或 Spring Security 配置不兼容。

### 基础版本要求

本文示例面向 Spring Boot 3.x 项目，推荐使用 Java 17 及以上版本。Spring Boot 3.5.x 官方文档显示，Spring Boot 3.5.14 至少要求 Java 17，并支持 Maven 3.6.3+；Gradle 支持 7.x 的 7.6.4+ 或 8.x 的 8.4+。([Home](https://docs.spring.io/spring-boot/3.5/system-requirements.html))

推荐版本如下：

| 组件              | 推荐版本              | 说明                                              |
| ----------------- | --------------------- | ------------------------------------------------- |
| JDK               | 17+                   | Spring Boot 3.x 最低要求 Java 17                  |
| Spring Boot       | 3.5.x                 | 示例基于 SpringBoot3 体系                         |
| Spring Boot Admin | 3.5.x                 | 适配 Spring Boot 3.x 项目，3.5.x 当前仍有维护版本 |
| Maven             | 3.6.3+                | Spring Boot 3.5.x 官方支持版本                    |
| Gradle            | 7.6.4+ / 8.4+         | 使用 Gradle 时选择官方支持范围                    |
| Servlet 容器      | Tomcat 10.1+          | Spring Boot 3 使用 Jakarta EE 命名空间            |
| Actuator          | 跟随 Spring Boot 版本 | 由 `spring-boot-starter-actuator` 提供            |

Spring Boot Admin 的 3.5.x 线适合 Spring Boot 3 项目；截至当前 Maven 仓库信息，Spring Boot Admin 已存在 4.x 和 3.5.x 两条版本线，其中 3.5.x 可用于继续维护 Spring Boot 3 项目，4.x 应结合 Spring Boot 4 体系单独评估。([Maven Repository](https://mvnrepository.com/artifact/de.codecentric/spring-boot-admin-starter-server?utm_source=chatgpt.com))

### 项目模块结构

建议使用多模块结构，将 Admin Server 与业务 Client 拆分，避免监控平台与业务应用强耦合。Admin Server 独立部署，业务服务只接入 Admin Client 和 Actuator。

推荐项目结构如下：

```text
spring-boot-admin-demo
├── pom.xml
├── admin-server
│   ├── pom.xml
│   └── src
│       └── main
│           ├── java
│           │   └── io
│           │       └── github
│           │           └── atengk
│           │               └── admin
│           │                   └── AdminServerApplication.java
│           └── resources
│               └── application.yml
├── admin-client
│   ├── pom.xml
│   └── src
│       └── main
│           ├── java
│           │   └── io
│           │       └── github
│           │           └── atengk
│           │               └── client
│           │                   └── AdminClientApplication.java
│           └── resources
│               └── application.yml
└── common-core
    ├── pom.xml
    └── src
        └── main
            └── java
                └── io
                    └── github
                        └── atengk
                            └── common
```

模块职责说明：

| 模块                     | 职责                                                         |
| ------------------------ | ------------------------------------------------------------ |
| `spring-boot-admin-demo` | 父工程，统一 Spring Boot、Spring Boot Admin、插件和 Java 版本 |
| `admin-server`           | Spring Boot Admin 服务端，提供监控 UI、实例注册、状态轮询和通知能力 |
| `admin-client`           | 示例业务服务，接入 Actuator 和 Admin Client，将自身注册到 Admin Server |
| `common-core`            | 公共模块，可放置通用工具、常量、响应模型等内容，非必需模块   |

对于正式项目，`admin-client` 通常不是单独模块，而是接入到已有业务服务中，例如 `user-service`、`order-service`、`gateway-service`。本文使用 `admin-client` 作为示例模块，便于说明接入方式。

### 依赖配置

Spring Boot Admin Server 官方快速开始示例要求服务端引入 `spring-boot-admin-starter-server` 和对应 Web Starter，并在启动类上添加 `@EnableAdminServer`；客户端可通过 `spring-boot-admin-starter-client` 注册到服务端，同时需要配置 Admin Server 地址和 Actuator 端点暴露策略。([Spring Boot Admin](https://docs.spring-boot-admin.com/3.5.5/docs/installation-and-setup/))

父工程用于统一版本和模块管理。

文件位置：`pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">

    <modelVersion>4.0.0</modelVersion>

    <groupId>io.github.atengk</groupId>
    <artifactId>spring-boot-admin-demo</artifactId>
    <version>1.0.0</version>
    <packaging>pom</packaging>

    <name>spring-boot-admin-demo</name>
    <description>SpringBoot3 Spring Boot Admin 开发示例工程</description>

    <modules>
        <module>common-core</module>
        <module>admin-server</module>
        <module>admin-client</module>
    </modules>

    <properties>
        <!-- Java 版本：Spring Boot 3.x 最低要求 Java 17 -->
        <java.version>17</java.version>

        <!-- Spring Boot 版本：统一管理 Spring Boot 相关 Starter 版本 -->
        <spring-boot.version>3.5.14</spring-boot.version>

        <!-- Spring Boot Admin 版本：Spring Boot 3 项目建议使用 3.5.x 版本线 -->
        <spring-boot-admin.version>3.5.8</spring-boot-admin.version>

        <!-- Hutool 工具库版本：业务模块可用于字符串、集合、日期等通用处理 -->
        <hutool.version>5.8.40</hutool.version>

        <!-- Lombok 版本：减少实体类、配置类中的样板代码 -->
        <lombok.version>1.18.38</lombok.version>

        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>
    </properties>

    <dependencyManagement>
        <dependencies>
            <!-- Spring Boot 依赖版本管理 -->
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-dependencies</artifactId>
                <version>${spring-boot.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>

            <!-- Spring Boot Admin 依赖版本管理 -->
            <dependency>
                <groupId>de.codecentric</groupId>
                <artifactId>spring-boot-admin-dependencies</artifactId>
                <version>${spring-boot-admin.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>

            <!-- Hutool 工具库版本管理 -->
            <dependency>
                <groupId>cn.hutool</groupId>
                <artifactId>hutool-all</artifactId>
                <version>${hutool.version}</version>
            </dependency>

            <!-- Lombok 版本管理 -->
            <dependency>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
                <version>${lombok.version}</version>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <build>
        <pluginManagement>
            <plugins>
                <!-- Spring Boot Maven 插件：用于打包可执行 Jar -->
                <plugin>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-maven-plugin</artifactId>
                    <version>${spring-boot.version}</version>
                </plugin>

                <!-- Maven 编译插件：统一 Java 编译版本 -->
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-compiler-plugin</artifactId>
                    <version>3.14.1</version>
                    <configuration>
                        <source>${java.version}</source>
                        <target>${java.version}</target>
                        <encoding>UTF-8</encoding>
                    </configuration>
                </plugin>
            </plugins>
        </pluginManagement>
    </build>

</project>
```

`admin-server` 模块用于搭建 Spring Boot Admin 服务端，需要引入 Admin Server Starter 和 Web Starter；如果后续需要登录认证，再增加 Spring Security。

文件位置：`admin-server/pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">

    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>io.github.atengk</groupId>
        <artifactId>spring-boot-admin-demo</artifactId>
        <version>1.0.0</version>
    </parent>

    <artifactId>admin-server</artifactId>
    <packaging>jar</packaging>

    <dependencies>
        <!-- Spring Boot Admin 服务端：提供监控 UI、实例注册、状态轮询等能力 -->
        <dependency>
            <groupId>de.codecentric</groupId>
            <artifactId>spring-boot-admin-starter-server</artifactId>
        </dependency>

        <!-- Web 支持：Admin Server 示例使用 Servlet Web 应用方式运行 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- 安全认证：后续配置登录认证、接口保护时使用 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>

        <!-- Actuator：用于监控 Admin Server 自身运行状态，可选但推荐保留 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>

        <!-- Lombok：减少配置类、实体类样板代码 -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <!-- 打包 admin-server 为可执行 Jar -->
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>

</project>
```

`admin-client` 模块用于模拟业务服务接入 Admin Server。实际项目中，这部分依赖应添加到每个需要被监控的业务服务中。

文件位置：`admin-client/pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">

    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>io.github.atengk</groupId>
        <artifactId>spring-boot-admin-demo</artifactId>
        <version>1.0.0</version>
    </parent>

    <artifactId>admin-client</artifactId>
    <packaging>jar</packaging>

    <dependencies>
        <!-- Web 支持：模拟一个普通 Spring Boot 业务服务 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Actuator：暴露 health、info、metrics、loggers 等监控端点 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>

        <!-- Spring Boot Admin 客户端：将当前应用注册到 Admin Server -->
        <dependency>
            <groupId>de.codecentric</groupId>
            <artifactId>spring-boot-admin-starter-client</artifactId>
        </dependency>

        <!-- Hutool 工具库：业务代码中可用于字符串、集合、日期等通用处理 -->
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
        </dependency>

        <!-- Lombok：减少实体类、配置类样板代码 -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <!-- 打包 admin-client 为可执行 Jar -->
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>

</project>
```

`common-core` 模块不是 Spring Boot Admin 必需模块。如果项目已有公共基础包，可以放置统一响应模型、常量、异常、工具类等内容。

文件位置：`common-core/pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">

    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>io.github.atengk</groupId>
        <artifactId>spring-boot-admin-demo</artifactId>
        <version>1.0.0</version>
    </parent>

    <artifactId>common-core</artifactId>
    <packaging>jar</packaging>

    <dependencies>
        <!-- Hutool 工具库：公共模块提供通用工具能力 -->
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
        </dependency>

        <!-- Lombok：减少公共模型类样板代码 -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
    </dependencies>

</project>
```

依赖配置完成后，可以先执行 Maven 编译，确认父子模块、版本管理和依赖解析正常。

```bash
# 在父工程根目录执行，检查所有模块是否可以正常编译
mvn clean package -DskipTests
```

该命令会清理旧构建产物并编译所有子模块。`-DskipTests` 表示跳过测试执行，适合在依赖和模块结构初始化阶段快速验证工程是否可构建。

## Admin Server 搭建

Admin Server 是 Spring Boot Admin 的服务端模块，负责接收客户端注册、轮询客户端 Actuator 端点、展示监控 UI 和维护应用实例状态。官方示例中，服务端需要引入 `spring-boot-admin-starter-server` 与 Web Starter，并在启动类上添加 `@EnableAdminServer` 启用 Admin Server。([Spring Boot Admin](https://docs.spring-boot-admin.com/3.5.5/docs/installation-and-setup/?utm_source=chatgpt.com))

### 服务端依赖引入

服务端依赖放在 `admin-server` 模块中。由于父工程已经通过 `dependencyManagement` 统一管理 Spring Boot 和 Spring Boot Admin 版本，子模块中不需要再单独声明版本号。

文件位置：`admin-server/pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">

    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>io.github.atengk</groupId>
        <artifactId>spring-boot-admin-demo</artifactId>
        <version>1.0.0</version>
    </parent>

    <artifactId>admin-server</artifactId>
    <packaging>jar</packaging>

    <dependencies>
        <!-- Spring Boot Admin 服务端：提供监控 UI、实例注册、状态轮询等能力 -->
        <dependency>
            <groupId>de.codecentric</groupId>
            <artifactId>spring-boot-admin-starter-server</artifactId>
        </dependency>

        <!-- Web 支持：Admin Server 基于 Servlet Web 应用方式运行 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Actuator：用于监控 Admin Server 自身运行状态 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>

        <!-- Security：后续安全认证章节使用，开发期可先放行访问 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>

        <!-- Hutool：提供常用工具能力，例如网络地址、字符串处理等 -->
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
        </dependency>

        <!-- Lombok：减少样板代码 -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <!-- 打包为可执行 Jar -->
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>

</project>
```

当前依赖中提前加入了 `spring-boot-starter-security`，主要是为了后续“安全认证配置”章节扩展登录认证。如果当前阶段只需要无认证访问，也可以临时移除该依赖，或者按下面的基础访问配置放行所有请求。

### 启动类配置

Admin Server 启动类需要添加 `@EnableAdminServer` 注解。该注解会启用 Spring Boot Admin Server 的自动配置，使当前应用具备实例注册、监控 UI 和客户端状态管理能力。([Spring Boot Admin](https://docs.spring-boot-admin.com/3.5.5/docs/installation-and-setup/?utm_source=chatgpt.com))

文件位置：`admin-server/src/main/java/io/github/atengk/admin/AdminServerApplication.java`

```java
package io.github.atengk.admin;

import de.codecentric.boot.admin.server.config.EnableAdminServer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot Admin 服务端启动类
 *
 * @author Ateng
 * @since 2026-05-06
 */
@EnableAdminServer
@SpringBootApplication
public class AdminServerApplication {

    /**
     * 启动 Admin Server 服务
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(AdminServerApplication.class, args);
    }

}
```

为了便于启动后查看访问地址，可以增加一个启动完成日志组件。该组件不是 Spring Boot Admin 必需代码，只用于开发阶段输出 Admin Server 访问地址。

文件位置：`admin-server/src/main/java/io/github/atengk/admin/runner/AdminServerStartedRunner.java`

```java
package io.github.atengk.admin.runner;

import cn.hutool.core.net.NetUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Admin Server 启动完成日志
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Component
public class AdminServerStartedRunner implements CommandLineRunner {

    @Value("${server.port:8080}")
    private Integer serverPort;

    @Value("${spring.boot.admin.context-path:}")
    private String adminContextPath;

    /**
     * 输出 Admin Server 访问地址
     *
     * @param args 启动参数
     */
    @Override
    public void run(String... args) {
        String host = NetUtil.getLocalhostStr();
        String contextPath = StrUtil.blankToDefault(adminContextPath, "");
        log.info("Spring Boot Admin 服务端启动完成，本机访问地址：http://localhost:{}{}", serverPort, contextPath);
        log.info("Spring Boot Admin 服务端启动完成，局域网访问地址：http://{}:{}{}", host, serverPort, contextPath);
    }

}
```

### 基础访问配置

基础访问配置用于指定 Admin Server 的端口、访问路径、页面标题和自身 Actuator 暴露策略。Spring Boot Admin 支持通过 `spring.boot.admin.context-path` 设置 Admin Server UI 和 API 的上下文路径；服务端也有状态轮询、信息轮询等配置项，可按实际环境调整。([Spring Boot Admin](https://docs.spring-boot-admin.com/3.5.5/docs/customize/ui-properties/?utm_source=chatgpt.com))

文件位置：`admin-server/src/main/resources/application.yml`

```yaml
server:
  # Admin Server 服务端口
  port: 9000

spring:
  application:
    # Admin Server 应用名称
    name: admin-server

  boot:
    admin:
      # Admin Server UI 和 API 访问前缀，最终访问地址为 http://localhost:9000/admin
      context-path: /admin

      ui:
        # 页面标题
        title: Spring Boot Admin
        # 顶部导航栏品牌文案
        brand: Spring Boot Admin 管理平台

management:
  endpoints:
    web:
      exposure:
        # 暴露 Admin Server 自身基础监控端点
        include: health,info,metrics,env,loggers
  endpoint:
    health:
      # 开发环境显示健康检查详情，生产环境建议改为 when_authorized
      show-details: always
  info:
    env:
      # 启用 info 端点读取 info.* 配置
      enabled: true

info:
  app:
    name: ${spring.application.name}
    description: Spring Boot Admin 服务端
    version: 1.0.0
```

由于当前 `admin-server` 已引入 `spring-boot-starter-security`，Spring Security 会默认保护所有请求。开发阶段如果希望先无认证访问 Admin UI，可以增加一个临时放行配置。正式环境不建议使用该配置，后续“安全认证配置”章节应替换为用户名密码登录、CSRF 处理和客户端访问认证。

文件位置：`admin-server/src/main/java/io/github/atengk/admin/config/AdminServerSecurityConfig.java`

```java
package io.github.atengk.admin.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Admin Server 开发期安全配置
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Configuration
public class AdminServerSecurityConfig {

    /**
     * 开发期放行所有请求
     *
     * @param http HTTP 安全配置
     * @return 安全过滤器链
     * @throws Exception 配置异常
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        log.info("加载 Admin Server 开发期安全配置，当前模式允许无认证访问");

        http
                .authorizeHttpRequests(authorize -> authorize
                        .anyRequest().permitAll()
                )
                .csrf(csrf -> csrf.disable());

        return http.build();
    }

}
```

启动 Admin Server：

```bash
# 在父工程根目录执行
mvn -pl admin-server spring-boot:run
```

命令说明：`-pl admin-server` 表示只启动 `admin-server` 子模块；`spring-boot:run` 表示使用 Spring Boot Maven 插件运行当前模块。启动完成后访问 `http://localhost:9000/admin`，如果页面正常打开，说明 Admin Server 基础搭建完成。

## Admin Client 接入

Admin Client 是被监控的业务服务端。它通过 `spring-boot-admin-starter-client` 将自身注册到 Admin Server，同时依赖 Spring Boot Actuator 暴露监控端点。官方文档说明，客户端接入需要配置 Admin Server 地址，并暴露必要的 Actuator Web 端点；应用启动后会自动向 Admin Server 注册。([Spring Boot Admin](https://docs.spring-boot-admin.com/3.5.5/docs/installation-and-setup/?utm_source=chatgpt.com))

### 客户端依赖引入

客户端依赖应添加到每个需要被监控的业务服务中。本文使用 `admin-client` 模块模拟一个普通业务服务，实际项目中可以替换为 `user-service`、`order-service`、`gateway-service` 等业务模块。

文件位置：`admin-client/pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">

    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>io.github.atengk</groupId>
        <artifactId>spring-boot-admin-demo</artifactId>
        <version>1.0.0</version>
    </parent>

    <artifactId>admin-client</artifactId>
    <packaging>jar</packaging>

    <dependencies>
        <!-- Web 支持：模拟普通业务服务 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Actuator：暴露 health、info、metrics、loggers 等监控端点 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>

        <!-- Spring Boot Admin 客户端：将当前应用注册到 Admin Server -->
        <dependency>
            <groupId>de.codecentric</groupId>
            <artifactId>spring-boot-admin-starter-client</artifactId>
        </dependency>

        <!-- Hutool：提供常用工具能力 -->
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
        </dependency>

        <!-- Lombok：减少样板代码 -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <!-- 打包为可执行 Jar -->
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <executions>
                    <execution>
                        <goals>
                            <!-- 生成 build-info.properties，便于 Admin UI 显示应用构建信息 -->
                            <goal>build-info</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>

</project>
```

`build-info` 不是客户端注册的必需配置，但它可以生成 `META-INF/build-info.properties`，便于 Spring Boot Admin 在应用列表和详情中展示构建版本信息。官方文档也推荐通过 Spring Boot Maven 插件的 `build-info` 目标展示应用版本。([Spring Boot Admin](https://docs.spring-boot-admin.com/3.5.5/docs/client/client-features/?utm_source=chatgpt.com))

客户端启动类保持普通 Spring Boot 应用写法即可，不需要额外启用注解。只要引入 Admin Client Starter 并配置 `spring.boot.admin.client.url`，客户端自动配置就会生效。

文件位置：`admin-client/src/main/java/io/github/atengk/client/AdminClientApplication.java`

```java
package io.github.atengk.client;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot Admin 客户端启动类
 *
 * @author Ateng
 * @since 2026-05-06
 */
@SpringBootApplication
public class AdminClientApplication {

    /**
     * 启动 Admin Client 示例服务
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(AdminClientApplication.class, args);
    }

}
```

为了提供一个简单业务访问入口，可以增加一个测试接口。该接口不是 Admin Client 必需代码，只用于确认客户端服务本身可正常访问。

文件位置：`admin-client/src/main/java/io/github/atengk/client/controller/ClientIndexController.java`

```java
package io.github.atengk.client.controller;

import cn.hutool.core.util.StrUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 客户端示例接口
 *
 * @author Ateng
 * @since 2026-05-06
 */
@RestController
public class ClientIndexController {

    @Value("${spring.application.name:admin-client}")
    private String applicationName;

    /**
     * 获取客户端首页信息
     *
     * @return 首页信息
     */
    @GetMapping("/")
    public String index() {
        return StrUtil.format("{} 服务运行正常", applicationName);
    }

}
```

### 客户端注册配置

客户端注册配置的核心是 `spring.boot.admin.client.url`。该配置指定 Admin Server 地址，客户端启动后会周期性向该地址注册应用实例。Spring Boot Admin Client 还支持注册周期、连接超时、读取超时、自动注册、自动注销、多 Admin Server 地址等属性。([Spring Boot Admin](https://docs.spring-boot-admin.com/3.5.0/docs/client/client-properties/?utm_source=chatgpt.com))

文件位置：`admin-client/src/main/resources/application.yml`

```yaml
server:
  # 客户端业务服务端口
  port: 9101

spring:
  application:
    # 客户端注册到 Admin Server 后展示的默认应用名称
    name: admin-client

  boot:
    admin:
      client:
        # 是否启用 Admin Client
        enabled: true

        # Admin Server 地址；由于服务端配置了 context-path=/admin，这里需要带上 /admin
        url: http://localhost:9000/admin

        # 客户端启动完成后自动注册
        auto-registration: true

        # 客户端关闭时自动从 Admin Server 注销
        auto-deregistration: true

        # 注册间隔，默认约 10 秒；开发环境可以保持默认
        period: 10s

        # 注册请求连接超时时间
        connect-timeout: 5s

        # 注册请求读取超时时间
        read-timeout: 5s

management:
  endpoints:
    web:
      exposure:
        # 开发环境可暴露较多端点，生产环境应按需缩小范围
        include: health,info,metrics,env,loggers,beans,threaddump,heapdump
  endpoint:
    health:
      # 开发环境显示健康检查详情
      show-details: always
  info:
    env:
      # 启用 info 端点读取 info.* 配置
      enabled: true

info:
  app:
    name: ${spring.application.name}
    description: Spring Boot Admin 客户端示例服务
    version: 1.0.0
```

如果 Admin Server 没有配置 `spring.boot.admin.context-path`，客户端地址可以写成：

```yaml
spring:
  boot:
    admin:
      client:
        # Admin Server 未配置 context-path 时使用根路径
        url: http://localhost:9000
```

如果后续 Admin Server 增加了 HTTP Basic 登录认证，需要在客户端注册配置中增加服务端用户名和密码。该部分会在“安全认证配置”章节详细说明，这里只给出配置位置。

```yaml
spring:
  boot:
    admin:
      client:
        # Admin Server 登录用户名
        username: admin
        # Admin Server 登录密码
        password: admin123
```

### 应用实例信息配置

应用实例信息用于控制客户端在 Admin Server 中展示的名称、访问地址、管理端点地址、分组、标签和元数据。Spring Boot Admin Client 支持通过 `spring.boot.admin.client.instance.*` 配置实例名称、服务地址、管理地址、健康检查地址和 metadata；其中 metadata 还支持 `tags.*`、`group`、`user.name`、`user.password` 等字段。([Spring Boot Admin](https://docs.spring-boot-admin.com/3.5.0/docs/client/client-properties/?utm_source=chatgpt.com))

文件位置：`admin-client/src/main/resources/application.yml`

```yaml
spring:
  boot:
    admin:
      client:
        instance:
          # 注册到 Admin Server 的实例名称，默认使用 spring.application.name
          name: ${spring.application.name}

          # 服务访问地址；本地开发可省略，Docker、Nginx、K8s 场景建议显式配置
          service-url: http://localhost:${server.port}

          # Actuator 管理端点基础地址；如果 management.server.port 单独配置，需要显式指定
          management-url: http://localhost:${server.port}/actuator

          # 健康检查地址；必须保证 Admin Server 可以访问
          health-url: http://localhost:${server.port}/actuator/health

          metadata:
            # 实例分组，可用于 Admin UI 聚合展示
            group: demo

            # 当前部署环境
            environment: dev

            # 应用负责人
            owner: Ateng

            # 应用描述
            description: Spring Boot Admin 客户端示例服务

            # 标签信息，可在 Admin UI 中作为实例标记展示
            tags:
              environment: dev
              module: demo
              type: springboot3
```

如果客户端 Actuator 后续启用了 Spring Security 或其他认证机制，需要将访问客户端 Actuator 端点的账号密码通过 metadata 提供给 Admin Server。官方文档中 `user.name` 和 `user.password` 是用于访问客户端端点的实例元数据字段。([Spring Boot Admin](https://docs.spring-boot-admin.com/3.5.5/docs/client/configuration/?utm_source=chatgpt.com))

```yaml
spring:
  boot:
    admin:
      client:
        instance:
          metadata:
            # Admin Server 访问当前客户端 Actuator 端点时使用的用户名
            user.name: actuator

            # Admin Server 访问当前客户端 Actuator 端点时使用的密码
            user.password: actuator123
```

如果客户端运行在容器、虚拟机、Kubernetes 或反向代理后面，不能简单依赖本机 `localhost` 推断地址。此时建议显式配置 `service-url`、`management-url` 和 `health-url`，保证 Admin Server 所在机器能够访问这些地址。

示例：Docker Compose 网络下的客户端实例配置。

```yaml
server:
  port: 9101

spring:
  boot:
    admin:
      client:
        url: http://admin-server:9000/admin
        instance:
          # Docker 网络中的服务名，必须能被 Admin Server 容器解析
          service-url: http://admin-client:9101
          management-url: http://admin-client:9101/actuator
          health-url: http://admin-client:9101/actuator/health
          metadata:
            group: docker
            environment: dev
            tags:
              runtime: docker
```

客户端启动命令：

```bash
# 先启动 Admin Server
mvn -pl admin-server spring-boot:run

# 再启动 Admin Client
mvn -pl admin-client spring-boot:run
```

命令说明：需要先启动 `admin-server`，再启动 `admin-client`，否则客户端第一次注册可能失败。由于客户端默认会周期性重新注册，只要 Admin Server 后续恢复，客户端也会再次尝试注册。

启动后访问：

```bash
# 查看客户端业务接口
curl http://localhost:9101/

# 查看客户端健康检查
curl http://localhost:9101/actuator/health

# 查看客户端应用信息
curl http://localhost:9101/actuator/info
```

预期结果如下：

```json
{
  "status": "UP"
}
```

此时打开 `http://localhost:9000/admin`，应用列表中应能看到 `admin-client` 实例。如果实例状态为 `UP`，说明客户端依赖、注册配置、Actuator 暴露配置和实例信息配置已经生效。

## Actuator 监控配置

Actuator 是 Spring Boot Admin 获取客户端监控数据的基础。客户端应用需要通过 `spring-boot-starter-actuator` 暴露监控端点，Admin Server 才能读取健康状态、运行指标、日志级别、环境变量、线程信息等内容。Spring Boot 官方文档说明，Actuator 端点默认以 `/actuator/{id}` 形式暴露，其中 HTTP 默认只暴露 `health` 端点，其他端点需要通过 `management.endpoints.web.exposure.include` 显式开放。([Home](https://docs.spring.io/spring-boot/3.5/reference/actuator/endpoints.html?utm_source=chatgpt.com)) 本章对应大纲中的 Actuator 配置部分。

### 端点暴露配置

端点暴露配置用于控制哪些 Actuator 端点可以通过 HTTP 被访问。开发环境可以暴露较多端点，便于 Spring Boot Admin 展示完整监控信息；生产环境应最小化暴露范围，并配合 Spring Security、内网访问控制或网关白名单使用。Spring Boot 官方文档也提示，暴露 Actuator 端点前应确认端点不包含敏感信息，或者使用防火墙、Spring Security 等方式进行保护。([Home](https://docs.spring.io/spring-boot/3.5/reference/actuator/endpoints.html?utm_source=chatgpt.com))

文件位置：`admin-client/src/main/resources/application.yml`

```yaml
management:
  endpoints:
    web:
      # Actuator Web 端点基础路径，默认就是 /actuator
      base-path: /actuator
      exposure:
        # 开发环境暴露常用监控端点，生产环境建议按需缩小范围
        include: health,info,metrics,env,loggers,beans,threaddump,heapdump,caches,scheduledtasks

  endpoint:
    health:
      # 开发环境显示健康详情，生产环境建议改为 when_authorized
      show-details: always
    env:
      # 开发环境可查看 env 详情，生产环境建议开启脱敏或关闭
      show-values: when_authorized
    configprops:
      # 配置属性端点包含敏感信息，生产环境应谨慎暴露
      show-values: when_authorized

  info:
    env:
      # 允许 info 端点读取 info.* 配置
      enabled: true
    java:
      # 展示 Java 运行时信息
      enabled: true
    os:
      # 展示操作系统信息
      enabled: true
```

常用端点说明：

| 端点             | 路径                       | 说明               | 生产建议           |
| ---------------- | -------------------------- | ------------------ | ------------------ |
| `health`         | `/actuator/health`         | 应用健康状态       | 建议暴露           |
| `info`           | `/actuator/info`           | 应用基础信息       | 建议暴露           |
| `metrics`        | `/actuator/metrics`        | 应用指标入口       | 按需暴露           |
| `loggers`        | `/actuator/loggers`        | 查看和调整日志级别 | 需认证保护         |
| `env`            | `/actuator/env`            | 环境变量和配置属性 | 谨慎暴露           |
| `beans`          | `/actuator/beans`          | Spring Bean 信息   | 内网或开发环境使用 |
| `threaddump`     | `/actuator/threaddump`     | 线程快照           | 故障排查使用       |
| `heapdump`       | `/actuator/heapdump`       | 堆转储文件         | 生产环境慎用       |
| `caches`         | `/actuator/caches`         | 缓存信息           | 按需暴露           |
| `scheduledtasks` | `/actuator/scheduledtasks` | 定时任务信息       | 按需暴露           |

如果只希望生产环境保留最小监控能力，可以使用下面的配置。

文件位置：`admin-client/src/main/resources/application-prod.yml`

```yaml
management:
  endpoints:
    web:
      exposure:
        # 生产环境建议只暴露必要端点
        include: health,info,metrics,loggers
  endpoint:
    health:
      # 只有认证用户可以查看健康详情
      show-details: when_authorized
    env:
      # 敏感配置仅认证后查看，仍建议生产环境不暴露 env
      show-values: when_authorized
```

启动后可以通过以下命令验证端点暴露结果。

```bash
# 查看 Actuator 暴露端点列表
curl http://localhost:9101/actuator

# 查看健康状态
curl http://localhost:9101/actuator/health

# 查看应用信息
curl http://localhost:9101/actuator/info

# 查看指标入口
curl http://localhost:9101/actuator/metrics

# 查看日志级别入口
curl http://localhost:9101/actuator/loggers
```

如果 `/actuator` 中没有出现对应端点，优先检查 `management.endpoints.web.exposure.include` 是否包含该端点，以及项目中是否存在支持该端点的依赖或自动配置。

### 健康检查配置

健康检查配置用于控制 `/actuator/health` 的返回内容。Spring Boot 的 `health` 端点会聚合应用上下文中的 `HealthIndicator`，例如数据库、Redis、RabbitMQ、磁盘空间等组件的状态，并根据状态聚合结果给出最终健康状态。`management.endpoint.health.show-details` 支持 `never`、`when-authorized` 和 `always` 三种模式，默认不展示详情。([Home](https://docs.spring.io/spring-boot/3.5/reference/actuator/endpoints.html?utm_source=chatgpt.com))

文件位置：`admin-client/src/main/resources/application.yml`

```yaml
management:
  endpoint:
    health:
      # 开发环境直接展示健康详情
      show-details: always

      # 展示组件级健康信息
      show-components: always

      # 开启 Kubernetes 探针端点：/actuator/health/liveness 和 /actuator/health/readiness
      probes:
        enabled: true

      group:
        # 存活探针：用于判断应用进程是否需要重启
        liveness:
          include: livenessState,ping

        # 就绪探针：用于判断应用是否可以接收流量
        readiness:
          include: readinessState,ping,diskSpace
```

Spring Boot 支持通过 health group 暴露 Kubernetes 常用的 liveness 和 readiness 探针，启用后可访问 `/actuator/health/liveness` 与 `/actuator/health/readiness`。官方文档说明，Kubernetes 可使用这些端点判断容器是否存活以及是否可以接收流量。([Home](https://docs.spring.io/spring-boot/3.5/reference/actuator/endpoints.html?utm_source=chatgpt.com))

如果项目需要自定义业务健康检查，可以实现 `HealthIndicator`。下面示例用于检查当前服务是否具备基础业务运行条件，适合放置轻量级检查逻辑，不建议在健康检查中执行耗时 SQL、大批量远程调用或复杂业务逻辑。

文件位置：`admin-client/src/main/java/io/github/atengk/client/actuator/BusinessHealthIndicator.java`

```java
package io.github.atengk.client.actuator;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * 业务健康检查指示器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Component("business")
public class BusinessHealthIndicator implements HealthIndicator {

    @Value("${spring.application.name:admin-client}")
    private String applicationName;

    @Value("${app.business.enabled:true}")
    private Boolean businessEnabled;

    /**
     * 返回业务健康检查结果
     *
     * @return 健康检查结果
     */
    @Override
    public Health health() {
        if (Boolean.FALSE.equals(businessEnabled)) {
            log.warn("业务健康检查未通过，应用：{}，原因：业务开关已关闭", applicationName);
            return Health.down()
                    .withDetail("application", applicationName)
                    .withDetail("reason", "业务开关已关闭")
                    .build();
        }

        String message = StrUtil.format("{} 业务健康检查正常", applicationName);
        log.debug("业务健康检查通过，应用：{}", applicationName);
        return Health.up()
                .withDetail("application", applicationName)
                .withDetail("message", message)
                .build();
    }

}
```

如果希望该自定义健康检查参与 readiness 探针，可以把 `business` 加入 readiness 分组。

文件位置：`admin-client/src/main/resources/application.yml`

```yaml
app:
  business:
    # 模拟业务健康开关，设置为 false 后 /actuator/health 会变为 DOWN
    enabled: true

management:
  endpoint:
    health:
      group:
        readiness:
          # 将自定义 business 健康检查加入就绪探针
          include: readinessState,ping,diskSpace,business
```

验证命令如下：

```bash
# 查看整体健康状态
curl http://localhost:9101/actuator/health

# 查看存活探针
curl http://localhost:9101/actuator/health/liveness

# 查看就绪探针
curl http://localhost:9101/actuator/health/readiness
```

预期结果中可以看到 `business` 组件。如果 `app.business.enabled=false`，整体状态会变为 `DOWN`，Admin Server 中对应实例也会显示异常状态。

### 应用信息配置

应用信息配置用于控制 `/actuator/info` 返回内容。Spring Boot 官方文档说明，`info` 端点会收集所有 `InfoContributor` 提供的信息，常见来源包括 `info.*` 环境属性、构建信息、Git 信息、Java 信息和操作系统信息。([Home](https://docs.spring.io/spring-boot/3.5/reference/actuator/endpoints.html?utm_source=chatgpt.com))

基础应用信息可以直接写在 `application.yml` 中。

文件位置：`admin-client/src/main/resources/application.yml`

```yaml
info:
  app:
    # 应用名称
    name: ${spring.application.name}
    # 应用描述
    description: Spring Boot Admin 客户端示例服务
    # 应用版本
    version: 1.0.0
    # 所属团队
    team: backend
    # 负责人
    owner: Ateng

  build:
    # 当前运行环境
    environment: dev

  tags:
    # Spring Boot Admin UI 中可展示标签信息
    environment: dev
    module: demo
    type: springboot3
```

为了让 Admin UI 显示构建版本信息，可以在客户端模块中启用 Spring Boot Maven 插件的 `build-info` 目标。

文件位置：`admin-client/pom.xml`

```xml
<build>
    <plugins>
        <!-- 生成 META-INF/build-info.properties，供 /actuator/info 使用 -->
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
            <executions>
                <execution>
                    <goals>
                        <goal>build-info</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

如果需要动态补充运行时信息，可以实现 `InfoContributor`。

文件位置：`admin-client/src/main/java/io/github/atengk/client/actuator/ApplicationInfoContributor.java`

```java
package io.github.atengk.client.actuator;

import cn.hutool.system.SystemUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 应用信息贡献器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Component
public class ApplicationInfoContributor implements InfoContributor {

    @Value("${spring.application.name:admin-client}")
    private String applicationName;

    /**
     * 补充应用运行时信息
     *
     * @param builder 应用信息构建器
     */
    @Override
    public void contribute(Info.Builder builder) {
        Map<String, Object> runtime = new LinkedHashMap<>();
        runtime.put("applicationName", applicationName);
        runtime.put("javaVersion", SystemUtil.getJavaInfo().getVersion());
        runtime.put("osName", SystemUtil.getOsInfo().getName());
        runtime.put("userName", SystemUtil.getUserInfo().getName());
        runtime.put("startupCheckTime", LocalDateTime.now().toString());

        builder.withDetail("runtime", runtime);
    }

}
```

验证命令如下：

```bash
# 查看应用信息
curl http://localhost:9101/actuator/info
```

正常情况下，返回结果中会包含 `app`、`build`、`tags`、`runtime` 等信息。Spring Boot Admin 会读取这些信息并在实例详情页中展示部分内容。

## 安全认证配置

安全认证配置用于保护 Admin Server 控制台、Client 注册接口和 Client Actuator 端点。Spring Boot 官方文档说明，如果 Spring Security 在 classpath 中且没有自定义 `SecurityFilterChain`，除 `/health` 外的 Actuator 端点会被自动保护；一旦用户自定义 `SecurityFilterChain`，Spring Boot 的默认安全自动配置会退让，由用户完整控制访问规则。([Home](https://docs.spring.io/spring-boot/3.5/reference/actuator/endpoints.html?utm_source=chatgpt.com))

### Admin Server 登录认证

Admin Server 登录认证用于保护监控控制台，避免未授权用户访问应用实例、环境变量、日志级别、线程信息等敏感数据。当前章节会替换前文开发期的“放行所有请求”配置，正式使用时应删除或停用 `permitAll` 的开发配置。

先在 `admin-server` 模块中保留 Spring Security 依赖。

文件位置：`admin-server/pom.xml`

```xml
<dependencies>
    <!-- Spring Boot Admin 服务端 -->
    <dependency>
        <groupId>de.codecentric</groupId>
        <artifactId>spring-boot-admin-starter-server</artifactId>
    </dependency>

    <!-- Web 支持 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Spring Security：保护 Admin Server 控制台和注册接口 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
</dependencies>
```

配置 Admin Server 登录账号。

文件位置：`admin-server/src/main/resources/application.yml`

```yaml
server:
  port: 9000

spring:
  application:
    name: admin-server

  boot:
    admin:
      context-path: /admin

  security:
    user:
      # Admin Server 控制台登录用户名
      name: admin
      # Admin Server 控制台登录密码，生产环境必须使用环境变量或密钥系统注入
      password: ${ADMIN_SERVER_PASSWORD:admin123}
      # 登录用户角色
      roles: ADMIN

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
```

Admin Server 安全配置需要同时处理页面登录、客户端注册接口、静态资源、退出登录和 CSRF。Spring Boot Admin 官方安全文档说明，部分 Actuator 端点如 `/loggers` 会使用 POST 请求，在使用 Spring Security 时需要忽略相应端点的 CSRF；客户端注册和注销接口也需要合理处理 CSRF。([Spring Boot Admin](https://docs.spring-boot-admin.com/3.4.5/docs/server/security/?utm_source=chatgpt.com))

文件位置：`admin-server/src/main/java/io/github/atengk/admin/config/AdminServerSecurityConfig.java`

```java
package io.github.atengk.admin.config;

import de.codecentric.boot.admin.server.config.AdminServerProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * Admin Server 安全认证配置
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class AdminServerSecurityConfig {

    private final AdminServerProperties adminServerProperties;

    /**
     * 配置 Admin Server 控制台登录认证和客户端注册认证
     *
     * @param http HTTP 安全配置
     * @return 安全过滤器链
     * @throws Exception 配置异常
     */
    @Bean
    public SecurityFilterChain adminServerSecurityFilterChain(HttpSecurity http) throws Exception {
        String adminContextPath = adminServerProperties.getContextPath();
        SavedRequestAwareAuthenticationSuccessHandler successHandler = new SavedRequestAwareAuthenticationSuccessHandler();
        successHandler.setTargetUrlParameter("redirectTo");
        successHandler.setDefaultTargetUrl(adminServerProperties.path("/"));

        log.info("加载 Admin Server 安全认证配置，访问前缀：{}", adminContextPath);

        http
                .authorizeHttpRequests(authorize -> authorize
                        // 静态资源放行
                        .requestMatchers(adminServerProperties.path("/assets/**")).permitAll()
                        // 登录页放行
                        .requestMatchers(adminServerProperties.path("/login")).permitAll()
                        // 健康检查端点放行，便于外部探活
                        .requestMatchers("/actuator/health").permitAll()
                        // 其他请求必须认证
                        .anyRequest().authenticated()
                )
                // 表单登录，用于浏览器访问 Admin UI
                .formLogin(formLogin -> formLogin
                        .loginPage(adminServerProperties.path("/login"))
                        .successHandler(successHandler)
                )
                // Basic 认证，用于 Admin Client 注册到 Admin Server
                .httpBasic(Customizer.withDefaults())
                .logout(logout -> logout
                        .logoutUrl(adminServerProperties.path("/logout"))
                )
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .ignoringRequestMatchers(
                                // 客户端注册实例
                                new AntPathRequestMatcher(adminServerProperties.path("/instances"), HttpMethod.POST.name()),
                                // 客户端注销实例
                                new AntPathRequestMatcher(adminServerProperties.path("/instances/*"), HttpMethod.DELETE.name()),
                                // Admin Server 自身 Actuator 端点
                                new AntPathRequestMatcher("/actuator/**")
                        )
                );

        return http.build();
    }

}
```

启动后访问 `http://localhost:9000/admin`，页面会跳转到登录页。输入 `admin/admin123` 后进入 Admin Server 控制台。

### Client 访问认证

Client 访问认证分为两个方向：一是 Admin Client 注册到 Admin Server 时需要通过 Basic 认证；二是 Admin Server 访问 Client Actuator 端点时需要使用客户端提供的访问凭据。Spring Boot Admin Client 官方属性说明中，`spring.boot.admin.client.username` 和 `spring.boot.admin.client.password` 用于 Admin Server API 受 HTTP Basic 保护时的客户端注册认证。([Spring Boot Admin](https://docs.spring-boot-admin.com/3.5.0/docs/client/client-properties/?utm_source=chatgpt.com))

先配置客户端注册 Admin Server 时使用的账号密码。

文件位置：`admin-client/src/main/resources/application.yml`

```yaml
spring:
  boot:
    admin:
      client:
        # Admin Server 地址
        url: http://localhost:9000/admin

        # 注册到 Admin Server 时使用的 Basic 认证用户名
        username: admin

        # 注册到 Admin Server 时使用的 Basic 认证密码，应与 Admin Server 登录密码一致
        password: ${ADMIN_SERVER_PASSWORD:admin123}

        instance:
          # 当前客户端服务地址
          service-url: http://localhost:${server.port}
          # 当前客户端 Actuator 管理地址
          management-url: http://localhost:${server.port}/actuator
          # 当前客户端健康检查地址
          health-url: http://localhost:${server.port}/actuator/health
```

如果客户端 Actuator 端点也启用了认证，Admin Server 访问客户端端点时还需要知道客户端端点账号密码。Spring Boot Admin 官方文档说明，实例 metadata 中的 `user.name` 和 `user.password` 可作为访问客户端端点的凭据。([Spring Boot Admin](https://docs.spring-boot-admin.com/3.5.5/docs/client/configuration/?utm_source=chatgpt.com))

文件位置：`admin-client/src/main/resources/application.yml`

```yaml
spring:
  boot:
    admin:
      client:
        instance:
          metadata:
            # Admin Server 访问当前客户端 Actuator 端点时使用的用户名
            user.name: actuator

            # Admin Server 访问当前客户端 Actuator 端点时使用的密码
            user.password: ${ACTUATOR_PASSWORD:actuator123}

            # 实例分组
            group: demo

            # 实例标签
            tags:
              environment: dev
              module: demo
```

也可以在 Admin Server 侧配置默认客户端端点访问凭据，适合所有客户端使用统一 Actuator 账号密码的场景。Spring Boot Admin Server 支持 `spring.boot.admin.instance-auth.default-user-name`、`spring.boot.admin.instance-auth.default-password`，也支持按服务名配置不同凭据；如果客户端 metadata 中提供了凭据，metadata 会优先使用。([Spring Boot Admin](https://docs.spring-boot-admin.com/3.4.5/docs/server/security/?utm_source=chatgpt.com))

文件位置：`admin-server/src/main/resources/application.yml`

```yaml
spring:
  boot:
    admin:
      instance-auth:
        # 启用 Admin Server 访问客户端 Actuator 的默认认证配置
        enabled: true

        # 默认 Actuator 用户名
        default-user-name: actuator

        # 默认 Actuator 密码
        default-password: ${ACTUATOR_PASSWORD:actuator123}

        service-map:
          # 针对指定服务配置独立访问凭据，服务名对应 spring.application.name
          admin-client:
            user-name: actuator
            user-password: ${ACTUATOR_PASSWORD:actuator123}
```

配置建议：如果不同服务团队独立维护 Actuator 密码，优先使用客户端 metadata；如果平台统一托管监控凭据，可以使用 Admin Server 的 `instance-auth` 统一配置。

### Actuator 端点保护

Actuator 端点保护用于防止敏感监控数据被未授权访问。生产环境中，`health` 和 `info` 可以按需放行，`env`、`loggers`、`heapdump`、`threaddump`、`beans` 等端点必须认证后访问。Spring Boot 官方文档提供了 `EndpointRequest.toAnyEndpoint()` 这类匹配器，用于在 Spring Security 中专门匹配 Actuator 端点。([Home](https://docs.spring.io/spring-boot/3.5/reference/actuator/endpoints.html?utm_source=chatgpt.com))

先在客户端加入 Spring Security 依赖。

文件位置：`admin-client/pom.xml`

```xml
<dependencies>
    <!-- Web 支持 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Actuator 监控端点 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>

    <!-- Spring Boot Admin 客户端 -->
    <dependency>
        <groupId>de.codecentric</groupId>
        <artifactId>spring-boot-admin-starter-client</artifactId>
    </dependency>

    <!-- Spring Security：保护 Actuator 端点 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
</dependencies>
```

配置客户端 Actuator 访问账号。

文件位置：`admin-client/src/main/resources/application.yml`

```yaml
spring:
  security:
    user:
      # Actuator 访问用户名
      name: actuator
      # Actuator 访问密码，生产环境必须通过环境变量或密钥系统注入
      password: ${ACTUATOR_PASSWORD:actuator123}
      # Actuator 管理角色
      roles: ACTUATOR_ADMIN
```

配置客户端安全过滤器链，放行 `health` 和 `info`，保护其他 Actuator 端点。由于 `/actuator/loggers` 存在 POST 操作，Spring Security 开启 CSRF 后可能导致日志级别调整失败，因此这里对 Actuator 端点忽略 CSRF。Spring Boot 官方文档也说明，默认 CSRF 保护会导致需要 POST、PUT、DELETE 的 Actuator 端点出现 403，若服务用于非浏览器客户端，可以按需禁用或忽略对应端点。([Home](https://docs.spring.io/spring-boot/3.5/reference/actuator/endpoints.html?utm_source=chatgpt.com))

文件位置：`admin-client/src/main/java/io/github/atengk/client/config/ActuatorSecurityConfig.java`

```java
package io.github.atengk.client.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Actuator 端点安全配置
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Configuration
public class ActuatorSecurityConfig {

    /**
     * 配置客户端 Actuator 端点访问权限
     *
     * @param http HTTP 安全配置
     * @return 安全过滤器链
     * @throws Exception 配置异常
     */
    @Bean
    public SecurityFilterChain actuatorSecurityFilterChain(HttpSecurity http) throws Exception {
        log.info("加载客户端 Actuator 端点安全配置");

        http
                .authorizeHttpRequests(authorize -> authorize
                        // 健康检查和应用信息允许匿名访问
                        .requestMatchers(EndpointRequest.to("health", "info")).permitAll()
                        // 其他 Actuator 端点需要 ACTUATOR_ADMIN 角色
                        .requestMatchers(EndpointRequest.toAnyEndpoint()).hasRole("ACTUATOR_ADMIN")
                        // 普通业务接口按当前示例放行，正式业务系统应接入自身认证体系
                        .anyRequest().permitAll()
                )
                // Admin Server 访问客户端 Actuator 时使用 Basic 认证
                .httpBasic(Customizer.withDefaults())
                .csrf(csrf -> csrf
                        // Actuator 端点主要由 Admin Server 调用，忽略 CSRF
                        .ignoringRequestMatchers(EndpointRequest.toAnyEndpoint())
                );

        return http.build();
    }

}
```

验证命令如下：

```bash
# health 放行
curl http://localhost:9101/actuator/health

# metrics 未认证时应返回 401 或 403
curl -i http://localhost:9101/actuator/metrics

# 使用 Actuator 账号访问 metrics
curl -u actuator:actuator123 http://localhost:9101/actuator/metrics

# 使用 Actuator 账号访问 loggers
curl -u actuator:actuator123 http://localhost:9101/actuator/loggers
```

如果 Admin Server 中实例显示 `DOWN` 或 Actuator 页面访问失败，重点检查三项：客户端 `metadata.user.name/user.password` 是否正确；Admin Server 是否能访问客户端的 `management-url`；客户端安全配置是否允许该账号访问对应端点。

## 通知与告警

通知与告警用于在应用状态变化时主动提醒研发或运维人员。Spring Boot Admin 支持多种通知方式，包括 Mail、Telegram、Webex、Discord、Slack 等，也支持通过实现 `Notifier` 或继承 `AbstractEventNotifier`、`AbstractStatusChangeNotifier` 扩展自定义通知。官方文档说明，自定义通知可以通过添加 Spring Bean 的方式接入。([Spring Boot Admin](https://docs.spring-boot-admin.com/3.5.2/docs/server/notifications/?utm_source=chatgpt.com))

### 邮件通知配置

邮件通知适合在服务 `DOWN`、`OFFLINE`、恢复 `UP` 时发送邮件给研发、测试或运维负责人。Spring Boot Admin 官方文档说明，邮件通知基于 `spring-boot-starter-mail` 提供的 `JavaMailSender`，邮件内容会以 HTML 形式发送，并使用 Thymeleaf 模板渲染；启用邮件通知需要配置邮件发送器和收件人。([Spring Boot Admin](https://docs.spring-boot-admin.com/3.5.2/docs/server/notifications/?utm_source=chatgpt.com))

先在 `admin-server` 模块中加入邮件依赖。

文件位置：`admin-server/pom.xml`

```xml
<dependencies>
    <!-- Spring Boot Admin 服务端 -->
    <dependency>
        <groupId>de.codecentric</groupId>
        <artifactId>spring-boot-admin-starter-server</artifactId>
    </dependency>

    <!-- 邮件发送：用于 Spring Boot Admin 状态变更邮件通知 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-mail</artifactId>
    </dependency>
</dependencies>
```

配置 SMTP 和 Spring Boot Admin 邮件通知参数。

文件位置：`admin-server/src/main/resources/application.yml`

```yaml
spring:
  mail:
    # SMTP 服务器地址
    host: smtp.example.com
    # SMTP 服务器端口，常见端口：25、465、587
    port: 587
    # 发件邮箱账号
    username: ${MAIL_USERNAME:admin@example.com}
    # 发件邮箱密码或授权码
    password: ${MAIL_PASSWORD:change-me}
    properties:
      mail:
        smtp:
          # 开启 SMTP 认证
          auth: true
          # 开启 STARTTLS
          starttls:
            enable: true
            required: true

  boot:
    admin:
      notify:
        mail:
          # 是否启用邮件通知
          enabled: true
          # 收件人，多个收件人使用逗号分隔
          to: ${ADMIN_NOTIFY_MAIL_TO:ops@example.com}
          # 抄送人，可选
          cc: ${ADMIN_NOTIFY_MAIL_CC:}
          # 发件人
          from: Spring Boot Admin <${MAIL_USERNAME:admin@example.com}>
          # 忽略 UNKNOWN 到 UP 的变化，避免客户端首次启动时频繁发送恢复通知
          ignore-changes: UNKNOWN:UP
```

常用邮件通知参数如下：

| 配置项                                         | 说明                 |
| ---------------------------------------------- | -------------------- |
| `spring.mail.host`                             | SMTP 服务器地址      |
| `spring.mail.port`                             | SMTP 服务器端口      |
| `spring.mail.username`                         | 发件邮箱账号         |
| `spring.mail.password`                         | 发件邮箱密码或授权码 |
| `spring.boot.admin.notify.mail.enabled`        | 是否启用邮件通知     |
| `spring.boot.admin.notify.mail.to`             | 收件人               |
| `spring.boot.admin.notify.mail.cc`             | 抄送人               |
| `spring.boot.admin.notify.mail.from`           | 发件人               |
| `spring.boot.admin.notify.mail.ignore-changes` | 忽略指定状态变化     |

Spring Boot Admin 邮件通知默认不会在模板中展示实例 metadata，以避免泄露敏感信息；如需展示自定义 metadata，应使用自定义模板并谨慎处理敏感字段。([Spring Boot Admin](https://docs.spring-boot-admin.com/3.5.2/docs/server/notifications/?utm_source=chatgpt.com))

验证邮件通知时，可以先启动 Admin Server 和 Admin Client，然后手动停止 Admin Client。等待 Admin Server 轮询到实例状态变化后，应收到服务下线邮件。

```bash
# 启动 Admin Server
mvn -pl admin-server spring-boot:run

# 启动 Admin Client
mvn -pl admin-client spring-boot:run

# 停止 admin-client 后观察 Admin Server 控制台状态和邮件通知
```

### 服务上下线通知

服务上下线通知主要关注实例状态变化，例如 `UP`、`DOWN`、`OFFLINE`、`UNKNOWN`。Admin Server 会轮询客户端健康状态，并在状态变化时触发通知。Spring Boot Admin 的通知配置支持 `ignore-changes`，可以忽略指定状态流转，例如常见的 `UNKNOWN:UP`，用于减少客户端首次注册时的噪声通知。([Spring Boot Admin](https://docs.spring-boot-admin.com/3.5.2/docs/server/notifications/?utm_source=chatgpt.com))

基础配置如下。

文件位置：`admin-server/src/main/resources/application.yml`

```yaml
spring:
  boot:
    admin:
      monitor:
        # 状态轮询间隔，开发环境可适当调小，生产环境不建议过低
        status-interval: 10s
        # 信息轮询间隔
        info-interval: 1m

      notify:
        mail:
          # 启用邮件通知
          enabled: true
          # 忽略首次注册恢复通知
          ignore-changes: UNKNOWN:UP
```

如果希望应用持续处于 `DOWN` 或 `OFFLINE` 时重复提醒，可以配置 `RemindingNotifier`。官方文档说明，`RemindingNotifier` 会对处于异常状态的应用发送重复提醒，默认示例中需要通过 Bean 的 `start` 和 `stop` 方法启动和停止提醒任务。([Spring Boot Admin](https://docs.spring-boot-admin.com/3.5.2/docs/server/notifications/?utm_source=chatgpt.com))

文件位置：`admin-server/src/main/java/io/github/atengk/admin/config/NotifierConfig.java`

```java
package io.github.atengk.admin.config;

import de.codecentric.boot.admin.server.domain.entities.InstanceRepository;
import de.codecentric.boot.admin.server.notify.CompositeNotifier;
import de.codecentric.boot.admin.server.notify.FilteringNotifier;
import de.codecentric.boot.admin.server.notify.Notifier;
import de.codecentric.boot.admin.server.notify.RemindingNotifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

/**
 * Spring Boot Admin 通知增强配置
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class NotifierConfig {

    private final InstanceRepository instanceRepository;

    private final ObjectProvider<List<Notifier>> otherNotifiers;

    /**
     * 配置通知过滤器
     *
     * @return 过滤通知器
     */
    @Bean
    public FilteringNotifier filteringNotifier() {
        log.info("加载 Spring Boot Admin 通知过滤器配置");
        CompositeNotifier delegate = new CompositeNotifier(otherNotifiers.getIfAvailable(Collections::emptyList));
        return new FilteringNotifier(delegate, instanceRepository);
    }

    /**
     * 配置异常状态重复提醒
     *
     * @return 重复提醒通知器
     */
    @Primary
    @Bean(initMethod = "start", destroyMethod = "stop")
    public RemindingNotifier remindingNotifier() {
        log.info("加载 Spring Boot Admin 异常状态重复提醒配置");
        RemindingNotifier notifier = new RemindingNotifier(filteringNotifier(), instanceRepository);
        notifier.setReminderPeriod(Duration.ofMinutes(10));
        notifier.setCheckReminderInverval(Duration.ofSeconds(10));
        return notifier;
    }

}
```

这段配置会在通知链路中加入过滤和重复提醒能力。`setReminderPeriod(Duration.ofMinutes(10))` 表示异常状态持续存在时每 10 分钟提醒一次；`setCheckReminderInverval(Duration.ofSeconds(10))` 表示每 10 秒检查一次是否存在需要提醒的实例。

如果不需要重复提醒，只保留邮件通知配置即可，避免告警过密。

### 自定义通知扩展

自定义通知扩展适合对接企业微信、钉钉、飞书、内部告警平台或自研事件中心。Spring Boot Admin 官方文档说明，可以通过实现 `Notifier` 接口，或继承 `AbstractEventNotifier`、`AbstractStatusChangeNotifier` 来实现自定义通知逻辑。([Spring Boot Admin](https://docs.spring-boot-admin.com/3.5.2/docs/server/notifications/?utm_source=chatgpt.com))

下面示例实现一个通用 Webhook 通知器。它会在实例事件发生时向指定 Webhook 地址发送 JSON 数据，外部系统可以根据 `application`、`status`、`eventType` 等字段做二次处理。

先增加自定义 Webhook 配置。

文件位置：`admin-server/src/main/resources/application.yml`

```yaml
admin:
  notify:
    webhook:
      # 是否启用自定义 Webhook 通知
      enabled: true
      # 自定义告警平台地址，例如企业内部告警网关
      url: ${ADMIN_NOTIFY_WEBHOOK_URL:http://localhost:18080/webhook/spring-boot-admin}
      # 自定义密钥，可用于下游平台验签
      secret: ${ADMIN_NOTIFY_WEBHOOK_SECRET:change-me}
```

定义配置属性类。

文件位置：`admin-server/src/main/java/io/github/atengk/admin/notify/WebhookNotifyProperties.java`

```java
package io.github.atengk.admin.notify;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Webhook 通知配置属性
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@Component
@ConfigurationProperties(prefix = "admin.notify.webhook")
public class WebhookNotifyProperties {

    /**
     * 是否启用 Webhook 通知
     */
    private Boolean enabled = false;

    /**
     * Webhook 请求地址
     */
    private String url;

    /**
     * Webhook 密钥
     */
    private String secret;

}
```

实现自定义事件通知器。

文件位置：`admin-server/src/main/java/io/github/atengk/admin/notify/WebhookEventNotifier.java`

```java
package io.github.atengk.admin.notify;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import de.codecentric.boot.admin.server.domain.entities.Instance;
import de.codecentric.boot.admin.server.domain.entities.InstanceRepository;
import de.codecentric.boot.admin.server.domain.events.InstanceEvent;
import de.codecentric.boot.admin.server.domain.events.InstanceStatusChangedEvent;
import de.codecentric.boot.admin.server.notify.AbstractEventNotifier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Webhook 自定义事件通知器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Component
public class WebhookEventNotifier extends AbstractEventNotifier {

    private final WebhookNotifyProperties properties;

    private final RestClient restClient;

    /**
     * 创建 Webhook 自定义事件通知器
     *
     * @param repository        实例仓库
     * @param properties        Webhook 通知配置
     * @param restClientBuilder REST 客户端构建器
     */
    public WebhookEventNotifier(InstanceRepository repository,
                                WebhookNotifyProperties properties,
                                RestClient.Builder restClientBuilder) {
        super(repository);
        this.properties = properties;
        this.restClient = restClientBuilder.build();
    }

    /**
     * 执行事件通知
     *
     * @param event    实例事件
     * @param instance 应用实例
     * @return 通知执行结果
     */
    @Override
    protected Mono<Void> doNotify(InstanceEvent event, Instance instance) {
        if (Boolean.FALSE.equals(properties.getEnabled()) || StrUtil.isBlank(properties.getUrl())) {
            log.debug("Webhook 通知未启用，跳过事件通知，事件类型：{}", event.getType());
            return Mono.empty();
        }

        return Mono.fromRunnable(() -> sendWebhook(event, instance))
                .onErrorResume(ex -> {
                    log.error("Webhook 通知发送失败，实例ID：{}，事件类型：{}", event.getInstance(), event.getType(), ex);
                    return Mono.empty();
                });
    }

    /**
     * 发送 Webhook 请求
     *
     * @param event    实例事件
     * @param instance 应用实例
     */
    private void sendWebhook(InstanceEvent event, Instance instance) {
        String applicationName = instance.getRegistration().getName();
        String status = getStatus(event);

        Map<String, Object> body = MapUtil.newHashMap();
        body.put("application", applicationName);
        body.put("instanceId", event.getInstance().getValue());
        body.put("eventType", event.getType());
        body.put("status", status);
        body.put("serviceUrl", instance.getRegistration().getServiceUrl());
        body.put("managementUrl", instance.getRegistration().getManagementUrl());
        body.put("secret", properties.getSecret());

        restClient.post()
                .uri(properties.getUrl())
                .contentType(MediaType.APPLICATION_JSON)
                .body(JSONUtil.toJsonStr(body))
                .retrieve()
                .toBodilessEntity();

        log.info("Webhook 通知发送成功，应用：{}，状态：{}，事件类型：{}", applicationName, status, event.getType());
    }

    /**
     * 获取事件状态
     *
     * @param event 实例事件
     * @return 事件状态
     */
    private String getStatus(InstanceEvent event) {
        if (event instanceof InstanceStatusChangedEvent statusChangedEvent) {
            return statusChangedEvent.getStatusInfo().getStatus();
        }
        return event.getType();
    }

}
```

下游 Webhook 服务会收到类似下面的 JSON 数据。

```json
{
  "application": "admin-client",
  "instanceId": "8f7c3b9a4c2d",
  "eventType": "STATUS_CHANGED",
  "status": "DOWN",
  "serviceUrl": "http://localhost:9101",
  "managementUrl": "http://localhost:9101/actuator",
  "secret": "change-me"
}
```

验证方式可以先启动一个简单的 Webhook 接收服务，再停止 `admin-client`，观察 Admin Server 日志是否输出“Webhook 通知发送成功”。

```bash
# 启动 Admin Server
mvn -pl admin-server spring-boot:run

# 启动 Admin Client
mvn -pl admin-client spring-boot:run

# 停止 Admin Client，触发 DOWN 或 OFFLINE 通知
```

如果没有触发通知，按以下顺序排查：确认 Admin Client 已成功注册到 Admin Server；确认 Admin Server 中实例状态发生变化；确认 `admin.notify.webhook.enabled=true`；确认 Webhook 地址能被 Admin Server 访问；确认通知器 Bean 已被 Spring 容器加载。

## 功能验证

功能验证用于确认 Admin Server 是否正常启动、Admin Client 是否成功注册、Actuator 端点是否可以被 Admin Server 访问。Spring Boot Admin 的基础工作方式是：客户端应用暴露 Actuator 端点，然后通过 Admin Client 或服务发现注册到 Admin Server，Admin Server 再轮询健康状态和指标数据并在控制台展示。([Spring Boot Admin](https://docs.spring-boot-admin.com/3.5.5/docs/installation-and-setup/?utm_source=chatgpt.com)) 本章对应大纲中的“功能验证”部分。

### 服务端启动验证

服务端启动验证用于确认 `admin-server` 模块依赖、启动类、端口、访问路径和登录认证是否配置正确。按照前文配置，Admin Server 访问地址为 `http://localhost:9000/admin`。

在父工程根目录执行以下命令启动 Admin Server。

```bash
# 启动 Admin Server 模块
mvn -pl admin-server spring-boot:run
```

如果项目需要先打包再运行，可以使用下面的命令。

```bash
# 打包所有模块，跳过测试
mvn clean package -DskipTests

# 运行 Admin Server 可执行 Jar
java -jar admin-server/target/admin-server-1.0.0.jar
```

命令说明：`mvn clean package -DskipTests` 会清理并打包所有 Maven 模块；`java -jar` 用于运行 Spring Boot 可执行 Jar。如果控制台出现 `Started AdminServerApplication`，说明 Spring Boot 应用已经启动完成。

验证 Admin Server 健康检查。

```bash
# Admin Server 自身健康检查
curl http://localhost:9000/actuator/health
```

预期返回：

```json
{
  "status": "UP"
}
```

如果已经启用 Admin Server 登录认证，浏览器访问 `http://localhost:9000/admin` 后应进入登录页。输入前文配置的账号密码，例如 `admin/admin123`，登录后应进入 Spring Boot Admin 控制台。

也可以通过命令验证登录保护是否生效。

```bash
# 未登录访问 Admin Server，通常会返回登录页或重定向
curl -i http://localhost:9000/admin

# 使用 Basic 认证访问 Admin Server API
curl -u admin:admin123 http://localhost:9000/admin/instances
```

如果 `/admin` 无法访问，优先检查以下配置项。

| 检查项                           | 期望值                                    |
| -------------------------------- | ----------------------------------------- |
| `server.port`                    | `9000`                                    |
| `spring.boot.admin.context-path` | `/admin`                                  |
| 启动类注解                       | 已添加 `@EnableAdminServer`               |
| 依赖                             | 已引入 `spring-boot-admin-starter-server` |
| 安全配置                         | 已放行登录页和静态资源                    |

Spring Boot Admin 官方示例要求服务端引入 `spring-boot-admin-starter-server`，并在启动类添加 `@EnableAdminServer`，启动后即可访问 Admin Server Web 页面。([Spring Boot Admin](https://docs.spring-boot-admin.com/3.5.5/docs/installation-and-setup/?utm_source=chatgpt.com))

### 客户端注册验证

客户端注册验证用于确认 `admin-client` 是否能够把自身注册到 Admin Server。按照前文配置，客户端服务端口为 `9101`，Admin Server 地址为 `http://localhost:9000/admin`。

先启动 Admin Server，再启动 Admin Client。

```bash
# 终端 1：启动 Admin Server
mvn -pl admin-server spring-boot:run

# 终端 2：启动 Admin Client
mvn -pl admin-client spring-boot:run
```

如果使用 Jar 方式运行，可以执行以下命令。

```bash
# 启动 Admin Server
java -jar admin-server/target/admin-server-1.0.0.jar

# 启动 Admin Client
java -jar admin-client/target/admin-client-1.0.0.jar
```

启动后验证客户端服务是否可访问。

```bash
# 验证客户端业务接口
curl http://localhost:9101/

# 验证客户端健康检查
curl http://localhost:9101/actuator/health

# 验证客户端应用信息
curl http://localhost:9101/actuator/info
```

如果 Actuator 端点已经启用 Basic 认证，则除 `health`、`info` 外的端点需要带上账号密码。

```bash
# 使用 Actuator 账号访问 metrics
curl -u actuator:actuator123 http://localhost:9101/actuator/metrics

# 使用 Actuator 账号访问 loggers
curl -u actuator:actuator123 http://localhost:9101/actuator/loggers
```

客户端注册成功后，浏览器打开 `http://localhost:9000/admin`，应用列表中应出现 `admin-client`，状态应为 `UP`。Spring Boot Admin Client 的 `spring.boot.admin.client.url` 是必填配置，用于指定客户端注册到哪个 Admin Server；客户端默认会周期性注册，默认注册周期为 10 秒。([Spring Boot Admin](https://docs.spring-boot-admin.com/3.5.0/docs/client/client-properties/?utm_source=chatgpt.com))

如果 Admin Server 开启了登录认证，客户端必须配置注册认证账号。

文件位置：`admin-client/src/main/resources/application.yml`

```yaml
spring:
  boot:
    admin:
      client:
        # Admin Server 地址，必须与服务端 context-path 保持一致
        url: http://localhost:9000/admin

        # Admin Server 注册接口认证账号
        username: admin

        # Admin Server 注册接口认证密码
        password: ${ADMIN_SERVER_PASSWORD:admin123}
```

如果 Admin Server 页面没有显示客户端，按下面顺序排查。

| 问题         | 排查方式                                                     |
| ------------ | ------------------------------------------------------------ |
| 客户端未启动 | 检查 `admin-client` 控制台是否启动成功                       |
| 注册地址错误 | 检查 `spring.boot.admin.client.url` 是否包含 `/admin`        |
| 认证失败     | 检查客户端 `username/password` 是否与 Admin Server 一致      |
| 端点不可达   | 在 Admin Server 所在机器执行 `curl http://客户端地址/actuator/health` |
| 实例地址错误 | 检查 `service-url`、`management-url`、`health-url` 是否可被服务端访问 |
| 网络隔离     | 检查 Docker、Kubernetes、Nginx、服务器防火墙和安全组配置     |

### 监控端点验证

监控端点验证用于确认 Admin Server 能否读取客户端 Actuator 数据。Spring Boot Actuator 默认只有 `health` 端点通过 HTTP 暴露，其他端点必须通过 `management.endpoints.web.exposure.include` 显式配置；官方文档也提醒，暴露端点前要评估敏感信息风险并配合安全保护。([Home](https://docs.spring.io/spring-boot/3.5/reference/actuator/endpoints.html?utm_source=chatgpt.com))

先检查客户端 Actuator 端点列表。

```bash
# 查看客户端已暴露的 Actuator 端点
curl http://localhost:9101/actuator
```

如果启用了认证，可以使用：

```bash
# 带认证查看 Actuator 端点列表
curl -u actuator:actuator123 http://localhost:9101/actuator
```

常用验证命令如下。

```bash
# 健康检查
curl http://localhost:9101/actuator/health

# 应用信息
curl http://localhost:9101/actuator/info

# 指标入口
curl -u actuator:actuator123 http://localhost:9101/actuator/metrics

# JVM 内存指标
curl -u actuator:actuator123 http://localhost:9101/actuator/metrics/jvm.memory.used

# 日志级别
curl -u actuator:actuator123 http://localhost:9101/actuator/loggers

# 线程快照
curl -u actuator:actuator123 http://localhost:9101/actuator/threaddump
```

如果需要验证在线调整日志级别，可以调用 `/actuator/loggers/{loggerName}`。Spring Boot Admin UI 内部也会调用该端点完成日志级别变更。

下面命令用于将指定包日志级别调整为 `DEBUG`。

```bash
# 调整 io.github.atengk 包日志级别为 DEBUG
curl -u actuator:actuator123 \
  -H "Content-Type: application/json" \
  -X POST \
  -d '{"configuredLevel":"DEBUG"}' \
  http://localhost:9101/actuator/loggers/io.github.atengk
```

恢复默认日志级别。

```bash
# 清空配置级别，恢复继承上级 Logger
curl -u actuator:actuator123 \
  -H "Content-Type: application/json" \
  -X POST \
  -d '{"configuredLevel":null}' \
  http://localhost:9101/actuator/loggers/io.github.atengk
```

如果端点返回 `404`，说明端点没有暴露或端点不存在；如果返回 `401`，说明没有认证；如果返回 `403`，通常是权限或 CSRF 配置问题。对于由 Admin Server 调用的 Actuator POST 端点，例如 `loggers`，需要在客户端安全配置中对 Actuator 端点忽略 CSRF，否则可能无法在线调整日志级别。Spring Boot 官方文档说明，自定义 `SecurityFilterChain` 后，Actuator 端点安全规则由用户完全控制。([Home](https://docs.spring.io/spring-boot/3.5/reference/actuator/endpoints.html?utm_source=chatgpt.com))

## 部署说明

部署说明用于将前文的开发环境配置迁移到实际运行环境。Spring Boot Admin 官方文档说明，项目没有维护官方生产 Docker 镜像，建议根据自身环境创建镜像；如果 Admin Server 运行在反向代理后面，可能需要配置 `spring.boot.admin.ui.public-url` 和 `server.forward-headers-strategy`，确保页面生成的链接正确。([Spring Boot Admin](https://docs.spring-boot-admin.com/3.5.5/docs/installation-and-setup/?utm_source=chatgpt.com))

### 单机部署

单机部署适合开发环境、测试环境、小规模内部系统或轻量级运维平台。推荐将 Admin Server 独立部署，业务服务作为 Client 接入。不要把 Admin Server 与核心业务模块强耦合在同一个进程中，避免监控平台故障影响业务服务。

先在服务器上准备目录。

```bash
# 创建部署目录
sudo mkdir -p /opt/spring-boot-admin/admin-server
sudo mkdir -p /opt/spring-boot-admin/admin-client
sudo mkdir -p /opt/spring-boot-admin/logs

# 授权当前用户写入，按实际部署用户调整
sudo chown -R $USER:$USER /opt/spring-boot-admin
```

命令说明：`/opt/spring-boot-admin/admin-server` 存放 Admin Server Jar 和配置；`/opt/spring-boot-admin/admin-client` 存放示例 Client Jar；`/opt/spring-boot-admin/logs` 存放运行日志。生产环境建议使用专门的低权限用户运行服务。

打包并上传 Jar。

```bash
# 本地打包
mvn clean package -DskipTests

# 上传 Admin Server Jar
scp admin-server/target/admin-server-1.0.0.jar user@server:/opt/spring-boot-admin/admin-server/

# 上传 Admin Client Jar，实际生产中通常上传业务服务 Jar
scp admin-client/target/admin-client-1.0.0.jar user@server:/opt/spring-boot-admin/admin-client/
```

创建 Admin Server 启动脚本。

文件位置：`/opt/spring-boot-admin/admin-server/start.sh`

```bash
#!/usr/bin/env bash

# Admin Server 单机启动脚本
set -e

APP_NAME="admin-server"
APP_HOME="/opt/spring-boot-admin/admin-server"
LOG_DIR="/opt/spring-boot-admin/logs"
JAR_FILE="${APP_HOME}/admin-server-1.0.0.jar"

export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-prod}"
export ADMIN_SERVER_PASSWORD="${ADMIN_SERVER_PASSWORD:-admin123}"
export ACTUATOR_PASSWORD="${ACTUATOR_PASSWORD:-actuator123}"
export JAVA_OPTS="${JAVA_OPTS:--Xms256m -Xmx512m -XX:+UseG1GC}"

mkdir -p "${LOG_DIR}"

cd "${APP_HOME}"

if pgrep -f "${JAR_FILE}" > /dev/null; then
  echo "${APP_NAME} 已在运行"
  exit 0
fi

nohup java ${JAVA_OPTS} -jar "${JAR_FILE}" \
  > "${LOG_DIR}/${APP_NAME}.log" 2>&1 &

echo "${APP_NAME} 启动完成，PID：$!"
echo "日志文件：${LOG_DIR}/${APP_NAME}.log"
```

创建 Admin Client 启动脚本。

文件位置：`/opt/spring-boot-admin/admin-client/start.sh`

```bash
#!/usr/bin/env bash

# Admin Client 单机启动脚本
set -e

APP_NAME="admin-client"
APP_HOME="/opt/spring-boot-admin/admin-client"
LOG_DIR="/opt/spring-boot-admin/logs"
JAR_FILE="${APP_HOME}/admin-client-1.0.0.jar"

export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-prod}"
export ADMIN_SERVER_PASSWORD="${ADMIN_SERVER_PASSWORD:-admin123}"
export ACTUATOR_PASSWORD="${ACTUATOR_PASSWORD:-actuator123}"
export JAVA_OPTS="${JAVA_OPTS:--Xms256m -Xmx512m -XX:+UseG1GC}"

mkdir -p "${LOG_DIR}"

cd "${APP_HOME}"

if pgrep -f "${JAR_FILE}" > /dev/null; then
  echo "${APP_NAME} 已在运行"
  exit 0
fi

nohup java ${JAVA_OPTS} -jar "${JAR_FILE}" \
  > "${LOG_DIR}/${APP_NAME}.log" 2>&1 &

echo "${APP_NAME} 启动完成，PID：$!"
echo "日志文件：${LOG_DIR}/${APP_NAME}.log"
```

添加执行权限并启动。

```bash
# 添加执行权限
chmod +x /opt/spring-boot-admin/admin-server/start.sh
chmod +x /opt/spring-boot-admin/admin-client/start.sh

# 启动服务端
/opt/spring-boot-admin/admin-server/start.sh

# 启动客户端
/opt/spring-boot-admin/admin-client/start.sh
```

如果需要使用 systemd 托管 Admin Server，可以创建服务文件。

文件位置：`/etc/systemd/system/admin-server.service`

```ini
[Unit]
Description=Spring Boot Admin Server
After=network.target

[Service]
Type=simple
User=app
Group=app
WorkingDirectory=/opt/spring-boot-admin/admin-server
Environment="SPRING_PROFILES_ACTIVE=prod"
Environment="ADMIN_SERVER_PASSWORD=admin123"
Environment="ACTUATOR_PASSWORD=actuator123"
Environment="JAVA_OPTS=-Xms256m -Xmx512m -XX:+UseG1GC"
ExecStart=/usr/bin/java $JAVA_OPTS -jar /opt/spring-boot-admin/admin-server/admin-server-1.0.0.jar
Restart=always
RestartSec=10
SuccessExitStatus=143

[Install]
WantedBy=multi-user.target
```

启用 systemd 服务。

```bash
# 重新加载 systemd 配置
sudo systemctl daemon-reload

# 设置开机自启
sudo systemctl enable admin-server

# 启动 Admin Server
sudo systemctl start admin-server

# 查看服务状态
sudo systemctl status admin-server

# 查看运行日志
journalctl -u admin-server -f
```

如果 Admin Server 前面有 Nginx 反向代理，需要配置转发头，并在 Spring Boot Admin 中设置公开访问地址。官方文档说明，反向代理场景可配置 `spring.boot.admin.ui.public-url`，HTTPS 在代理层终止时可能还需要设置 `server.forward-headers-strategy=native`。([Spring Boot Admin](https://docs.spring-boot-admin.com/3.5.7/docs/server/server/?utm_source=chatgpt.com))

文件位置：`admin-server/src/main/resources/application-prod.yml`

```yaml
server:
  # 识别反向代理转发头
  forward-headers-strategy: native

spring:
  boot:
    admin:
      ui:
        # 外部访问地址，按实际域名调整
        public-url: https://admin.example.com/admin
```

Nginx 反向代理配置如下。

文件位置：`/etc/nginx/conf.d/admin-server.conf`

```nginx
server {
    listen 80;
    server_name admin.example.com;

    # 建议生产环境跳转 HTTPS
    return 301 https://$host$request_uri;
}

server {
    listen 443 ssl;
    server_name admin.example.com;

    # SSL 证书配置，替换为实际证书路径
    ssl_certificate /etc/nginx/certs/admin.example.com.pem;
    ssl_certificate_key /etc/nginx/certs/admin.example.com.key;

    location /admin/ {
        proxy_pass http://127.0.0.1:9000/admin/;

        # 透传真实客户端信息
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;

        # 透传协议和端口，便于 Spring Boot Admin 生成正确链接
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header X-Forwarded-Port $server_port;

        # 支持 Server-Sent Events
        proxy_buffering off;
        proxy_read_timeout 3600s;
    }
}
```

应用 Nginx 配置。

```bash
# 检查 Nginx 配置
sudo nginx -t

# 重新加载 Nginx
sudo systemctl reload nginx
```

### 多环境配置

多环境配置用于隔离开发、测试和生产环境。Spring Boot Admin 不建议混用不同环境实例，例如 dev、test、prod 服务全部注册到同一个 Admin Server，否则容易误判服务状态，也可能暴露生产敏感配置。

推荐使用 Spring Profile 区分环境。

```text
admin-server/src/main/resources
├── application.yml
├── application-dev.yml
├── application-test.yml
└── application-prod.yml

admin-client/src/main/resources
├── application.yml
├── application-dev.yml
├── application-test.yml
└── application-prod.yml
```

Admin Server 通用配置如下。

文件位置：`admin-server/src/main/resources/application.yml`

```yaml
server:
  port: 9000

spring:
  application:
    name: admin-server

  boot:
    admin:
      context-path: /admin
      ui:
        title: Spring Boot Admin
        brand: Spring Boot Admin 管理平台

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: when_authorized
```

Admin Server 开发环境配置如下。

文件位置：`admin-server/src/main/resources/application-dev.yml`

```yaml
spring:
  security:
    user:
      # 开发环境账号
      name: admin
      password: admin123
      roles: ADMIN

  boot:
    admin:
      ui:
        # 开发环境访问地址
        public-url: http://localhost:9000/admin
```

Admin Server 测试环境配置如下。

文件位置：`admin-server/src/main/resources/application-test.yml`

```yaml
spring:
  security:
    user:
      # 测试环境账号
      name: admin
      password: ${ADMIN_SERVER_PASSWORD}
      roles: ADMIN

  boot:
    admin:
      ui:
        # 测试环境访问地址
        public-url: https://admin-test.example.com/admin
```

Admin Server 生产环境配置如下。

文件位置：`admin-server/src/main/resources/application-prod.yml`

```yaml
server:
  # 生产环境通常通过 Nginx 或网关转发
  forward-headers-strategy: native

spring:
  security:
    user:
      # 生产环境账号
      name: ${ADMIN_SERVER_USERNAME:admin}
      password: ${ADMIN_SERVER_PASSWORD}
      roles: ADMIN

  boot:
    admin:
      ui:
        # 生产环境外部访问地址
        public-url: https://admin.example.com/admin

      instance-auth:
        # Admin Server 访问客户端 Actuator 端点的默认凭据
        enabled: true
        default-user-name: ${ACTUATOR_USERNAME:actuator}
        default-password: ${ACTUATOR_PASSWORD}

      notify:
        mail:
          # 生产环境开启邮件通知
          enabled: true
          to: ${ADMIN_NOTIFY_MAIL_TO}
          from: Spring Boot Admin <${MAIL_USERNAME}>
          ignore-changes: UNKNOWN:UP

management:
  endpoints:
    web:
      exposure:
        # Admin Server 自身只暴露必要端点
        include: health,info,metrics
  endpoint:
    health:
      show-details: when_authorized
```

Admin Client 通用配置如下。

文件位置：`admin-client/src/main/resources/application.yml`

```yaml
server:
  port: 9101

spring:
  application:
    name: admin-client

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,loggers
  endpoint:
    health:
      show-details: when_authorized
  info:
    env:
      enabled: true

info:
  app:
    name: ${spring.application.name}
    version: 1.0.0
```

Admin Client 开发环境配置如下。

文件位置：`admin-client/src/main/resources/application-dev.yml`

```yaml
spring:
  boot:
    admin:
      client:
        # 开发环境 Admin Server
        url: http://localhost:9000/admin
        username: admin
        password: admin123
        instance:
          service-url: http://localhost:${server.port}
          management-url: http://localhost:${server.port}/actuator
          health-url: http://localhost:${server.port}/actuator/health
          metadata:
            group: dev
            environment: dev
            tags:
              environment: dev
              module: demo
```

Admin Client 测试环境配置如下。

文件位置：`admin-client/src/main/resources/application-test.yml`

```yaml
spring:
  boot:
    admin:
      client:
        # 测试环境 Admin Server
        url: https://admin-test.example.com/admin
        username: ${ADMIN_SERVER_USERNAME:admin}
        password: ${ADMIN_SERVER_PASSWORD}
        instance:
          service-url: https://admin-client-test.example.com
          management-url: https://admin-client-test.example.com/actuator
          health-url: https://admin-client-test.example.com/actuator/health
          metadata:
            group: test
            environment: test
            user.name: ${ACTUATOR_USERNAME:actuator}
            user.password: ${ACTUATOR_PASSWORD}
            tags:
              environment: test
              module: demo
```

Admin Client 生产环境配置如下。

文件位置：`admin-client/src/main/resources/application-prod.yml`

```yaml
spring:
  boot:
    admin:
      client:
        # 生产环境 Admin Server
        url: https://admin.example.com/admin
        username: ${ADMIN_SERVER_USERNAME:admin}
        password: ${ADMIN_SERVER_PASSWORD}
        instance:
          # 生产环境必须配置为 Admin Server 可访问的真实地址
          service-url: https://admin-client.example.com
          management-url: https://admin-client.example.com/actuator
          health-url: https://admin-client.example.com/actuator/health
          metadata:
            group: prod
            environment: prod
            user.name: ${ACTUATOR_USERNAME:actuator}
            user.password: ${ACTUATOR_PASSWORD}
            tags:
              environment: prod
              module: demo

management:
  endpoints:
    web:
      exposure:
        # 生产环境只暴露必要端点
        include: health,info,metrics,loggers
  endpoint:
    health:
      show-details: when_authorized
```

不同环境启动命令如下。

```bash
# 开发环境
java -jar admin-server-1.0.0.jar --spring.profiles.active=dev

# 测试环境
java -jar admin-server-1.0.0.jar --spring.profiles.active=test

# 生产环境
java -jar admin-server-1.0.0.jar --spring.profiles.active=prod
```

也可以使用环境变量方式启动。

```bash
# 生产环境启动示例
export SPRING_PROFILES_ACTIVE=prod
export ADMIN_SERVER_USERNAME=admin
export ADMIN_SERVER_PASSWORD='replace-with-strong-password'
export ACTUATOR_USERNAME=actuator
export ACTUATOR_PASSWORD='replace-with-strong-actuator-password'

java -jar admin-server-1.0.0.jar
```

多环境配置建议如下。

| 环境 | Admin Server       | Client 注册地址                        | Actuator 暴露策略 | 告警策略       |
| ---- | ------------------ | -------------------------------------- | ----------------- | -------------- |
| dev  | 本地或开发服务器   | `http://localhost:9000/admin`          | 可暴露较多端点    | 可关闭         |
| test | 测试域名           | `https://admin-test.example.com/admin` | 暴露常用排查端点  | 可开启测试通知 |
| prod | 生产内网或受控域名 | `https://admin.example.com/admin`      | 最小化暴露        | 必须开启       |

### 常见问题处理

常见问题处理用于快速定位启动、注册、端点访问、认证和部署过程中的异常。排查时建议先看客户端日志，再看 Admin Server 页面状态，最后从 Admin Server 所在机器直接访问客户端 Actuator 端点。

| 问题                       | 现象                 | 原因                                        | 处理方式                                                     |
| -------------------------- | -------------------- | ------------------------------------------- | ------------------------------------------------------------ |
| Admin Server 页面打不开    | 浏览器访问失败       | 端口错误、服务未启动、context-path 错误     | 检查 `server.port`、`spring.boot.admin.context-path` 和启动日志 |
| 客户端没有出现在页面       | 应用列表为空         | Client 未注册成功                           | 检查 `spring.boot.admin.client.url` 和客户端日志             |
| 客户端状态为 `DOWN`        | 页面显示红色异常     | `/actuator/health` 返回非 UP                | 直接访问客户端 `/actuator/health` 查看具体组件               |
| 客户端状态为 `OFFLINE`     | 实例离线             | Admin Server 无法访问客户端                 | 检查网络、防火墙、容器服务名和 `health-url`                  |
| Actuator 页面 401          | 未认证               | Actuator 启用了 Basic 认证                  | 配置 `metadata.user.name/user.password` 或 `instance-auth`   |
| Actuator 页面 403          | 权限或 CSRF 问题     | Security 规则不允许访问或 POST 被 CSRF 拦截 | 调整 `SecurityFilterChain`，对 Actuator 端点忽略 CSRF        |
| `/actuator/loggers` 不显示 | UI 日志页面不可用    | 未暴露 `loggers` 端点                       | 增加 `management.endpoints.web.exposure.include=loggers`     |
| `/actuator/info` 为空      | 信息页无内容         | 未启用 env info 或没有 build-info           | 配置 `management.info.env.enabled=true` 并启用 `build-info`  |
| Docker 中地址错误          | 注册的是容器内部地址 | 自动推断的地址不可被 Admin Server 访问      | 显式配置 `service-url`、`management-url`、`health-url`       |
| 反向代理后链接错误         | 页面资源或跳转异常   | 缺少公开地址和转发头配置                    | 配置 `spring.boot.admin.ui.public-url` 和 `server.forward-headers-strategy` |

下面给出几个典型故障的处理示例。

场景一：客户端无法注册到 Admin Server。

```bash
# 在客户端服务器上测试 Admin Server 是否可达
curl -i http://localhost:9000/admin

# 如果开启了认证，测试 Basic 认证是否正确
curl -u admin:admin123 http://localhost:9000/admin/instances

# 查看客户端启动日志中的注册失败原因
grep -i "spring boot admin" /opt/spring-boot-admin/logs/admin-client.log
```

重点检查客户端配置。

文件位置：`admin-client/src/main/resources/application.yml`

```yaml
spring:
  boot:
    admin:
      client:
        # 必须是客户端能够访问到的 Admin Server 地址
        url: http://localhost:9000/admin

        # 如果 Admin Server 开启 Basic 认证，必须配置账号密码
        username: admin
        password: ${ADMIN_SERVER_PASSWORD:admin123}
```

场景二：Admin Server 页面能看到客户端，但状态为 `OFFLINE`。

```bash
# 在 Admin Server 所在机器执行，确认能否访问客户端健康检查
curl -i http://localhost:9101/actuator/health

# 如果客户端 Actuator 启用了认证
curl -u actuator:actuator123 http://localhost:9101/actuator/health
```

如果命令在客户端本机可以访问，但在 Admin Server 所在机器不能访问，说明是网络或地址配置问题。需要显式配置客户端实例地址。

文件位置：`admin-client/src/main/resources/application-prod.yml`

```yaml
spring:
  boot:
    admin:
      client:
        instance:
          # 必须配置为 Admin Server 可以访问的地址
          service-url: https://admin-client.example.com
          management-url: https://admin-client.example.com/actuator
          health-url: https://admin-client.example.com/actuator/health
```

场景三：Admin Server 无法读取客户端日志、指标或环境信息。

```bash
# 查看暴露端点列表
curl -u actuator:actuator123 http://localhost:9101/actuator

# 查看指标端点
curl -u actuator:actuator123 http://localhost:9101/actuator/metrics

# 查看日志端点
curl -u actuator:actuator123 http://localhost:9101/actuator/loggers
```

检查端点暴露配置。

文件位置：`admin-client/src/main/resources/application.yml`

```yaml
management:
  endpoints:
    web:
      exposure:
        # 至少暴露 Admin Server 需要读取的端点
        include: health,info,metrics,loggers
```

场景四：在线调整日志级别失败。

```bash
# 测试 loggers POST 是否可用
curl -u actuator:actuator123 \
  -H "Content-Type: application/json" \
  -X POST \
  -d '{"configuredLevel":"DEBUG"}' \
  http://localhost:9101/actuator/loggers/io.github.atengk
```

如果返回 `403`，检查客户端 Spring Security 配置中是否对 Actuator 忽略 CSRF。

文件位置：`admin-client/src/main/java/io/github/atengk/client/config/ActuatorSecurityConfig.java`

```java
package io.github.atengk.client.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Actuator 端点安全配置
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Configuration
public class ActuatorSecurityConfig {

    /**
     * 配置 Actuator 端点认证和 CSRF 规则
     *
     * @param http HTTP 安全配置
     * @return 安全过滤器链
     * @throws Exception 配置异常
     */
    @Bean
    public SecurityFilterChain actuatorSecurityFilterChain(HttpSecurity http) throws Exception {
        log.info("加载 Actuator 端点安全配置，允许 Admin Server 调用监控端点");

        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(EndpointRequest.to("health", "info")).permitAll()
                        .requestMatchers(EndpointRequest.toAnyEndpoint()).hasRole("ACTUATOR_ADMIN")
                        .anyRequest().permitAll()
                )
                .httpBasic(Customizer.withDefaults())
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers(EndpointRequest.toAnyEndpoint())
                );

        return http.build();
    }

}
```

场景五：反向代理后页面跳转或静态资源路径异常。

检查 Admin Server 生产配置。

文件位置：`admin-server/src/main/resources/application-prod.yml`

```yaml
server:
  # 识别 Nginx 或网关传入的 X-Forwarded-* 请求头
  forward-headers-strategy: native

spring:
  boot:
    admin:
      ui:
        # 必须配置为浏览器实际访问的外部地址
        public-url: https://admin.example.com/admin
```

检查 Nginx 是否透传转发头。

```nginx
location /admin/ {
    proxy_pass http://127.0.0.1:9000/admin/;

    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_set_header X-Forwarded-Port $server_port;

    proxy_buffering off;
    proxy_read_timeout 3600s;
}
```

生产环境最终检查清单如下。

| 检查项            | 建议                                                         |
| ----------------- | ------------------------------------------------------------ |
| Admin Server 登录 | 必须启用认证                                                 |
| Client 注册认证   | 必须配置 `username/password`                                 |
| Actuator 端点认证 | 除必要公开端点外都应认证                                     |
| Actuator 暴露范围 | 生产环境最小化暴露                                           |
| 密码管理          | 使用环境变量、密钥系统或配置中心，不写死在 Git               |
| 网络访问          | Admin Server 能访问 Client 的 `health-url` 和 `management-url` |
| 反向代理          | 配置 `public-url` 和 `forward-headers-strategy`              |
| 通知告警          | 生产环境开启邮件或 Webhook 告警                              |
| 日志保留          | 使用 systemd、日志平台或文件滚动策略管理日志                 |
| 环境隔离          | dev、test、prod 使用不同 Admin Server 或明确分组             |