# Spring Boot 配置

Spring Boot 配置用于管理应用运行过程中的各类参数，例如服务端口、应用名称、日志级别、数据库连接、Redis 连接、JSON 序列化规则、自定义业务开关等。通过配置文件和外部化配置机制，可以将运行参数从代码中解耦出来，使同一套代码适配开发、测试、预发、生产等不同环境。

## 配置概述

本节主要说明 Spring Boot 配置体系的基本组成、常见配置来源以及配置使用建议。实际开发中，应优先将环境相关、部署相关、业务可变参数放入配置文件或外部配置中，避免在 Java 代码中硬编码。

### 配置体系说明

Spring Boot 的配置体系可以理解为三部分：

| 组成部分 | 说明                                                         |
| -------- | ------------------------------------------------------------ |
| 配置来源 | 配置文件、环境变量、JVM 参数、命令行参数等                   |
| 配置加载 | 应用启动时由 Spring Boot 自动加载并合并配置                  |
| 配置使用 | 通过 `@Value`、`Environment`、`@ConfigurationProperties` 等方式读取配置 |

Spring Boot 启动时，会将不同来源的配置统一加载到 Spring 的 `Environment` 中。业务代码不需要关心配置来自哪里，只需要按照配置 Key 读取对应的值即可。

常见读取方式如下：

| 读取方式                   | 适用场景         | 示例                                                 |
| -------------------------- | ---------------- | ---------------------------------------------------- |
| `@Value`                   | 读取少量简单配置 | `@Value("${server.port}")`                           |
| `Environment`              | 动态读取配置     | `environment.getProperty("spring.application.name")` |
| `@ConfigurationProperties` | 绑定一组配置项   | `app.upload.path`、`app.upload.max-size`             |

推荐做法是：少量配置可以使用 `@Value`，成组业务配置优先使用 `@ConfigurationProperties`。后者结构更清晰，也更适合嵌套对象、集合、配置校验等场景。

### 常见配置来源

Spring Boot 支持多种配置来源。实际项目中最常用的是 `application.yml`、`application.properties`、环境变量、JVM 参数和命令行参数。

| 配置来源                 | 示例                  | 适用场景                       |
| ------------------------ | --------------------- | ------------------------------ |
| `application.yml`        | `server.port: 8080`   | 推荐用于结构化配置             |
| `application.properties` | `server.port=8080`    | 适合简单配置或老项目迁移       |
| Profile 配置文件         | `application-dev.yml` | 多环境配置拆分                 |
| 环境变量                 | `SERVER_PORT=8081`    | Docker、Kubernetes、CI/CD 部署 |
| JVM 参数                 | `-Dserver.port=8082`  | 启动时覆盖部分配置             |
| 命令行参数               | `--server.port=8083`  | 临时覆盖配置                   |

示例：

```bash
# 使用命令行参数指定端口
java -jar app.jar --server.port=9000

# 使用 JVM 参数指定端口
java -Dserver.port=9001 -jar app.jar

# 使用环境变量指定端口
SERVER_PORT=9002 java -jar app.jar
```

一般情况下，项目默认配置放在 `application.yml` 中，环境差异配置放在 `application-dev.yml`、`application-test.yml`、`application-prod.yml` 中。生产环境的密码、密钥、Token 等敏感配置不建议写入代码仓库，应通过环境变量、配置中心或密钥管理服务注入。

## 基础配置文件

Spring Boot 默认会自动加载 `src/main/resources` 目录下的配置文件。常用配置文件包括 `application.yml` 和 `application.properties`。两者都可以完成相同的配置功能，区别主要在于书写格式不同。

### application.yml

`application.yml` 使用 YAML 格式编写配置，通过缩进表达层级关系。它适合配置项较多、层级较明显的 Spring Boot 项目。

文件位置：`src/main/resources/application.yml`

```yaml
# 服务配置
server:
  # 应用启动端口
  port: 8080
  servlet:
    # 应用访问上下文路径
    context-path: /api

# Spring 基础配置
spring:
  application:
    # 应用名称，常用于日志、注册中心、链路追踪和监控标识
    name: springboot3-config-demo

  profiles:
    # 默认激活环境
    active: dev

  jackson:
    # JSON 日期时间格式
    date-format: yyyy-MM-dd HH:mm:ss
    # JSON 时区
    time-zone: Asia/Shanghai

# 日志配置
logging:
  level:
    # 全局日志级别
    root: info
    # 指定项目包日志级别
    io.github.atengk: debug
  file:
    # 日志文件输出路径
    name: logs/app.log

# 自定义业务配置
app:
  upload:
    # 文件上传目录
    path: /data/files
    # 单个文件最大大小
    max-size: 100MB
  security:
    # 是否开启接口签名校验
    enable-sign: true
```

YAML 格式的优点是层级清晰、重复前缀少，适合中大型项目使用。缺点是对缩进敏感，缩进错误可能导致配置解析失败或配置层级不符合预期。

编写 `application.yml` 时建议遵守以下规则：

| 建议                   | 说明                                      |
| ---------------------- | ----------------------------------------- |
| 使用两个空格缩进       | 不要使用 Tab                              |
| 同类配置集中管理       | 例如 `server`、`spring`、`logging`、`app` |
| 自定义配置使用统一前缀 | 例如 `app`、`biz`、`custom`               |
| 字符串按需加引号       | 包含特殊字符、冒号、前导零时建议加引号    |
| 敏感信息不要提交仓库   | 密码、密钥、Token 应使用外部化配置        |

### application.properties

`application.properties` 使用 `key=value` 格式编写配置。它结构简单，适合配置项较少的项目，也适合从传统 Spring 项目迁移到 Spring Boot 项目。

文件位置：`src/main/resources/application.properties`

```properties
# 服务端口
server.port=8080

# 应用访问上下文路径
server.servlet.context-path=/api

# 应用名称
spring.application.name=springboot3-config-demo

# 默认激活环境
spring.profiles.active=dev

# JSON 日期时间格式
spring.jackson.date-format=yyyy-MM-dd HH:mm:ss

# JSON 时区
spring.jackson.time-zone=Asia/Shanghai

# 全局日志级别
logging.level.root=info

# 指定项目包日志级别
logging.level.io.github.atengk=debug

# 日志文件输出路径
logging.file.name=logs/app.log

# 文件上传目录
app.upload.path=/data/files

# 单个文件最大大小
app.upload.max-size=100MB

# 是否开启接口签名校验
app.security.enable-sign=true
```

`application.properties` 的优点是简单直接，便于搜索和复制单个配置项。缺点是配置项较多时会出现大量重复前缀，可读性不如 YAML。

同一个项目中建议统一使用一种配置格式，避免同时维护 `application.yml` 和 `application.properties` 导致配置来源混乱。如果两种文件同时存在，需要特别注意配置覆盖关系，避免出现实际生效值和预期不一致的问题。

### 配置优先级

配置优先级用于解决同一个配置项在多个位置同时存在时，最终使用哪个值的问题。Spring Boot 会按照一定顺序加载配置，优先级高的配置会覆盖优先级低的配置。

例如，配置文件中指定端口为 `8080`：

```yaml
server:
  port: 8080
```

启动时通过命令行参数指定端口为 `9000`：

```bash
java -jar app.jar --server.port=9000
```

最终应用启动端口为 `9000`，因为命令行参数的优先级高于配置文件。

常见配置优先级可以简化理解如下：

| 优先级 | 配置来源                  | 示例                              |
| ------ | ------------------------- | --------------------------------- |
| 高     | 命令行参数                | `--server.port=9000`              |
| 高     | JVM 参数                  | `-Dserver.port=9001`              |
| 高     | 环境变量                  | `SERVER_PORT=9002`                |
| 中     | 外部配置文件              | `./config/application.yml`        |
| 中     | 外部 Profile 配置文件     | `./config/application-prod.yml`   |
| 低     | 项目内部 Profile 配置文件 | `classpath:/application-prod.yml` |
| 低     | 项目内部通用配置文件      | `classpath:/application.yml`      |

示例配置如下：

文件位置：`src/main/resources/application.yml`

```yaml
# 默认配置
server:
  port: 8080

spring:
  application:
    name: config-demo
```

文件位置：`src/main/resources/application-prod.yml`

```yaml
# 生产环境配置
server:
  port: 8081
```

启动命令：

```bash
java -jar config-demo.jar --spring.profiles.active=prod --server.port=9000
```

最终生效结果：

| 配置项                    | 最终值        | 来源              |
| ------------------------- | ------------- | ----------------- |
| `spring.profiles.active`  | `prod`        | 命令行参数        |
| `spring.application.name` | `config-demo` | `application.yml` |
| `server.port`             | `9000`        | 命令行参数        |

配置优先级排查建议如下：

| 问题                     | 排查方向                                 |
| ------------------------ | ---------------------------------------- |
| 修改配置文件后端口不生效 | 检查启动命令是否包含 `--server.port`     |
| 本地正常，服务器不生效   | 检查环境变量、启动脚本和外部配置目录     |
| Profile 配置没有加载     | 检查 `spring.profiles.active` 是否正确   |
| YAML 配置读取不到        | 检查缩进、冒号后空格、文件名和文件位置   |
| 配置值和预期不一致       | 检查是否存在多个配置来源同时设置同一 Key |

实际开发建议遵循以下原则：

1. 通用默认配置放在 `application.yml`。
2. 环境差异配置放在 `application-dev.yml`、`application-test.yml`、`application-prod.yml`。
3. 生产环境敏感配置不要写入 Git 仓库。
4. Docker、Kubernetes 部署时优先通过环境变量或配置中心覆盖配置。
5. 临时调试可以使用命令行参数，但不要长期依赖手工参数覆盖关键配置。

## 核心配置项

核心配置项用于定义 Spring Boot 应用最基础的运行行为，包括服务端口、应用名称、日志输出和 JSON 序列化规则。这些配置通常在项目初始化时就需要明确，并且会被后续的接口访问、日志排查、服务注册、链路追踪和前后端交互共同依赖。以下内容承接你上传的开发大纲继续展开。

### 服务端口配置

服务端口用于指定 Spring Boot 内置 Web 容器监听的端口。对于 Web 项目，默认端口通常为 `8080`，实际开发中可以根据本地调试、测试部署、容器暴露端口或网关转发规则进行调整。Spring Boot 的 `server.port` 属于常见应用配置项，可写入 `application.yml`、`application.properties`，也可以通过环境变量、JVM 参数或命令行参数覆盖。([Home](https://docs.spring.io/spring-boot/appendix/application-properties/?utm_source=chatgpt.com))

文件位置：`src/main/resources/application.yml`

```yaml
# 服务配置
server:
  # 应用启动端口
  port: 8080
  servlet:
    # 应用上下文路径，接口访问前缀为 /api
    context-path: /api
```

使用 `application.properties` 时可以写成如下形式：

文件位置：`src/main/resources/application.properties`

```properties
# 应用启动端口
server.port=8080

# 应用上下文路径
server.servlet.context-path=/api
```

配置完成后，接口访问地址会受到 `server.port` 和 `server.servlet.context-path` 两个配置共同影响。例如端口为 `8080`、上下文路径为 `/api` 时，本地接口访问地址通常为：

```text
http://localhost:8080/api
```

开发中常见端口约定如下：

| 环境     | 推荐端口         | 说明                                           |
| -------- | ---------------- | ---------------------------------------------- |
| 本地开发 | `8080`、`8081`   | 单服务调试或避免端口冲突                       |
| 测试环境 | `18080`、`18081` | 便于和本地默认端口区分                         |
| 生产环境 | 按部署规范指定   | 通常由网关、Nginx、Kubernetes Service 统一暴露 |

启动时也可以临时覆盖端口：

```bash
# 命令行参数覆盖配置文件中的 server.port
java -jar app.jar --server.port=9000

# JVM 参数覆盖配置文件中的 server.port
java -Dserver.port=9001 -jar app.jar

# 环境变量覆盖配置文件中的 server.port
SERVER_PORT=9002 java -jar app.jar
```

端口配置排查时，优先检查启动命令、环境变量和外部配置文件。Spring Boot 会将命令行参数加入 `Environment`，并且命令行参数优先级高于基于文件的配置来源，因此配置文件中的端口可能会被启动参数覆盖。([Home](https://docs.spring.io/spring-boot/3.5-SNAPSHOT/reference/features/external-config.html?utm_source=chatgpt.com))

### 应用名称配置

应用名称用于标识当前 Spring Boot 应用，常用于日志输出、服务注册、链路追踪、监控告警、配置中心和容器部署标识。Spring Boot 提供 `spring.application.name` 配置项用于指定应用名称。([Home](https://docs.spring.io/spring-boot/docs/3.1.11/reference/html/application-properties.html?utm_source=chatgpt.com))

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  application:
    # 应用名称，建议使用小写字母、中横线分隔
    name: order-service
```

使用 `application.properties` 时可以写成如下形式：

文件位置：`src/main/resources/application.properties`

```properties
# 应用名称
spring.application.name=order-service
```

应用名称建议遵守统一命名规范：

| 命名规则             | 示例            | 说明                                   |
| -------------------- | --------------- | -------------------------------------- |
| 使用业务域 + service | `order-service` | 适合微服务后端应用                     |
| 使用业务域 + job     | `order-job`     | 适合定时任务应用                       |
| 使用业务域 + admin   | `system-admin`  | 适合管理后台应用                       |
| 使用小写和中横线     | `user-center`   | 避免大小写混用导致部署和监控标识不统一 |

在多模块或微服务项目中，不建议多个应用使用相同的 `spring.application.name`。如果名称重复，可能导致日志检索、注册中心实例识别、链路追踪服务名和监控指标维度混乱。

### 日志配置

日志配置用于控制应用运行时的日志级别、日志输出位置、日志格式和日志滚动策略。Spring Boot 默认集成日志系统；使用常见 starter 时，默认日志实现通常为 Logback，并预配置控制台输出，也支持文件输出。([Home](https://docs.spring.io/spring-boot/3.5-SNAPSHOT/reference/features/logging.html?utm_source=chatgpt.com))

文件位置：`src/main/resources/application.yml`

```yaml
logging:
  level:
    # 全局日志级别
    root: info
    # Spring Web 相关日志级别
    org.springframework.web: info
    # 项目业务包日志级别
    io.github.atengk: debug

  file:
    # 日志文件输出路径，可以是相对路径或绝对路径
    name: logs/order-service.log

  pattern:
    # 控制台日志格式
    console: "%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%thread] %logger{36} - %msg%n"
    # 文件日志格式
    file: "%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%thread] %logger{50} - %msg%n"

  logback:
    rollingpolicy:
      # 单个日志文件最大大小
      max-file-size: 100MB
      # 日志保留天数
      max-history: 30
      # 所有归档日志总大小上限
      total-size-cap: 10GB
```

常见日志级别如下：

| 日志级别 | 说明         | 使用场景                         |
| -------- | ------------ | -------------------------------- |
| `trace`  | 最详细日志   | 极少使用，适合框架级问题追踪     |
| `debug`  | 调试日志     | 本地开发、测试环境排查问题       |
| `info`   | 普通运行日志 | 生产环境常用级别                 |
| `warn`   | 警告日志     | 可恢复异常、配置风险、降级处理   |
| `error`  | 错误日志     | 业务异常、系统异常、外部调用失败 |

生产环境建议将全局日志级别设置为 `info`，并针对具体业务包或排查对象临时开启 `debug`。不建议在生产环境长期将 `root` 设置为 `debug`，否则可能造成日志量过大、磁盘占用升高和敏感信息泄露风险。

如果项目需要更复杂的日志策略，例如按环境区分日志路径、按模块拆分日志文件、输出 JSON 格式日志，可以额外创建 Logback 配置文件：

文件位置：`src/main/resources/logback-spring.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <!-- 从 Spring 配置中读取应用名称 -->
    <springProperty scope="context" name="APP_NAME" source="spring.application.name" defaultValue="springboot-app"/>

    <!-- 日志文件路径 -->
    <property name="LOG_PATH" value="logs"/>

    <!-- 控制台输出 -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level [${APP_NAME}] [%thread] %logger{36} - %msg%n</pattern>
            <charset>UTF-8</charset>
        </encoder>
    </appender>

    <!-- 文件输出 -->
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${LOG_PATH}/${APP_NAME}.log</file>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level [${APP_NAME}] [%thread] %logger{50} - %msg%n</pattern>
            <charset>UTF-8</charset>
        </encoder>

        <!-- 日志滚动策略 -->
        <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
            <fileNamePattern>${LOG_PATH}/${APP_NAME}.%d{yyyy-MM-dd}.%i.log.gz</fileNamePattern>
            <maxFileSize>100MB</maxFileSize>
            <maxHistory>30</maxHistory>
            <totalSizeCap>10GB</totalSizeCap>
        </rollingPolicy>
    </appender>

    <!-- 项目业务包日志级别 -->
    <logger name="io.github.atengk" level="DEBUG"/>

    <!-- 根日志配置 -->
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="FILE"/>
    </root>
</configuration>
```

如果使用外部 Logback 配置文件，可以通过 `logging.config` 指定文件位置。Spring Boot 常见应用属性中包含 `logging.config`、`logging.file.name`、`logging.file.path`、`logging.level.*`、`logging.pattern.*` 等日志配置项。([Home](https://docs.spring.io/spring-boot/appendix/application-properties/?utm_source=chatgpt.com))

```yaml
logging:
  # 指定日志配置文件
  config: classpath:logback-spring.xml
```

### Jackson 配置

Jackson 配置用于控制 Spring Boot 中 JSON 序列化和反序列化行为，例如日期格式、时区、空值输出、枚举处理和未知字段处理。Spring Boot 提供 `spring.jackson.*` 系列配置项，用于调整自动配置的 Jackson 行为。常见属性包括 `spring.jackson.date-format`、`spring.jackson.time-zone`、`spring.jackson.default-property-inclusion`、`spring.jackson.serialization.*`、`spring.jackson.deserialization.*` 等。([Home](https://docs.spring.io/spring-boot/appendix/application-properties/?utm_source=chatgpt.com))

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  jackson:
    # 日期时间格式
    date-format: yyyy-MM-dd HH:mm:ss
    # 时区配置
    time-zone: Asia/Shanghai
    # 序列化时不输出 null 字段
    default-property-inclusion: non_null
    serialization:
      # 日期不输出为时间戳
      write-dates-as-timestamps: false
    deserialization:
      # JSON 中存在 Java 对象没有的字段时不报错
      fail-on-unknown-properties: false
```

对应的 `application.properties` 写法如下：

```properties
# 日期时间格式
spring.jackson.date-format=yyyy-MM-dd HH:mm:ss

# 时区配置
spring.jackson.time-zone=Asia/Shanghai

# 序列化时不输出 null 字段
spring.jackson.default-property-inclusion=non_null

# 日期不输出为时间戳
spring.jackson.serialization.write-dates-as-timestamps=false

# JSON 中存在 Java 对象没有的字段时不报错
spring.jackson.deserialization.fail-on-unknown-properties=false
```

常见配置项说明如下：

| 配置项                                                      | 说明                     | 推荐值                |
| ----------------------------------------------------------- | ------------------------ | --------------------- |
| `spring.jackson.date-format`                                | 全局日期时间格式         | `yyyy-MM-dd HH:mm:ss` |
| `spring.jackson.time-zone`                                  | JSON 日期时区            | `Asia/Shanghai`       |
| `spring.jackson.default-property-inclusion`                 | 控制字段输出规则         | `non_null`            |
| `spring.jackson.serialization.write-dates-as-timestamps`    | 日期是否输出为时间戳     | `false`               |
| `spring.jackson.deserialization.fail-on-unknown-properties` | 未知字段是否反序列化失败 | `false`               |

需要注意，`spring.jackson.date-format` 对传统 `java.util.Date`、`java.util.Calendar` 类型更直接；如果项目大量使用 Java 8 时间类型，例如 `LocalDateTime`、`LocalDate`、`LocalTime`，通常还需要结合字段级注解或统一的 Jackson 自定义配置进行处理。

字段级示例：

```java
package io.github.atengk.order.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 订单响应对象
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
public class OrderVO {

    /**
     * 订单编号
     */
    private String orderNo;

    /**
     * 下单时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime createTime;
}
```

字段级注解适合少量特殊字段。如果项目中所有接口都需要统一日期格式，优先使用全局配置或统一的 Jackson 配置类，避免每个字段重复添加注解。

## 多环境配置

多环境配置用于解决不同运行环境之间的配置差异，例如本地开发连接本地数据库，测试环境连接测试数据库，生产环境连接生产数据库。Spring Boot 通过 Profile 机制支持按环境加载不同配置文件，常见环境包括 `dev`、`test`、`pre`、`prod`。Profile 专用配置文件遵循 `application-{profile}` 命名规则，例如激活 `prod` 环境时，会同时考虑 `application.yml` 和 `application-prod.yml`。([Home](https://docs.spring.io/spring-boot/reference/features/external-config.html?utm_source=chatgpt.com))

### Profile 基本用法

Profile 用于标识当前应用运行环境。通过激活不同 Profile，可以让同一套代码读取不同环境的配置。常见 Profile 命名如下：

| Profile | 环境     | 说明           |
| ------- | -------- | -------------- |
| `dev`   | 开发环境 | 本地开发、联调 |
| `test`  | 测试环境 | 测试人员验证   |
| `pre`   | 预发环境 | 生产前验证     |
| `prod`  | 生产环境 | 正式线上环境   |

在默认配置文件中可以指定当前激活环境：

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  profiles:
    # 当前激活环境
    active: dev
```

也可以在单个 YAML 文件中使用多文档配置，根据 Profile 条件激活不同配置段。Spring Boot 支持通过 `spring.config.activate.on-profile` 控制某个配置文档是否参与最终配置合并。([Home](https://docs.spring.io/spring-boot/3.5/how-to/properties-and-configuration.html?utm_source=chatgpt.com))

文件位置：`src/main/resources/application.yml`

```yaml
# 默认配置
server:
  port: 8080

spring:
  application:
    name: profile-demo

---
# 开发环境配置
spring:
  config:
    activate:
      on-profile: dev

server:
  port: 8081

logging:
  level:
    io.github.atengk: debug

---
# 生产环境配置
spring:
  config:
    activate:
      on-profile: prod

server:
  port: 8082

logging:
  level:
    io.github.atengk: info
```

上面的配置中，如果激活 `dev`，应用端口为 `8081`；如果激活 `prod`，应用端口为 `8082`。多文档配置会按照文档出现顺序合并，后面的值可以覆盖前面的值。([Home](https://docs.spring.io/spring-boot/3.5/how-to/properties-and-configuration.html?utm_source=chatgpt.com))

### 环境配置文件拆分

在实际项目中，更推荐将不同环境配置拆分为多个文件。这样可以让公共配置和环境差异配置边界更清晰，也便于生产环境部署时替换或外置配置文件。Spring Boot 会加载普通配置文件和对应的 Profile 配置文件，并且 Profile 专用配置会覆盖非 Profile 配置；如果同时激活多个 Profile，则后面的 Profile 配置可以覆盖前面的 Profile 配置。([Home](https://docs.spring.io/spring-boot/reference/features/external-config.html?utm_source=chatgpt.com))

推荐文件结构如下：

```text
src/main/resources
├── application.yml
├── application-dev.yml
├── application-test.yml
├── application-pre.yml
└── application-prod.yml
```

通用配置放在 `application.yml` 中：

文件位置：`src/main/resources/application.yml`

```yaml
server:
  servlet:
    # 所有环境统一接口前缀
    context-path: /api

spring:
  application:
    # 应用名称
    name: order-service

  jackson:
    # 统一 JSON 日期格式
    date-format: yyyy-MM-dd HH:mm:ss
    # 统一 JSON 时区
    time-zone: Asia/Shanghai

logging:
  file:
    # 默认日志输出文件
    name: logs/order-service.log
```

开发环境配置放在 `application-dev.yml` 中：

文件位置：`src/main/resources/application-dev.yml`

```yaml
server:
  # 开发环境端口
  port: 8080

logging:
  level:
    # 开发环境输出更多业务调试日志
    io.github.atengk: debug
    org.springframework.web: debug

app:
  upload:
    # 开发环境文件目录
    path: ./data/files
```

测试环境配置放在 `application-test.yml` 中：

文件位置：`src/main/resources/application-test.yml`

```yaml
server:
  # 测试环境端口
  port: 18080

logging:
  level:
    # 测试环境保留业务调试日志
    io.github.atengk: debug
    org.springframework.web: info

app:
  upload:
    # 测试环境文件目录
    path: /data/test/order-service/files
```

生产环境配置放在 `application-prod.yml` 中：

文件位置：`src/main/resources/application-prod.yml`

```yaml
server:
  # 生产环境端口，实际也可以由环境变量或启动参数覆盖
  port: 8080

logging:
  level:
    # 生产环境建议使用 info，避免日志量过大
    root: info
    io.github.atengk: info

app:
  upload:
    # 生产环境文件目录
    path: /data/prod/order-service/files
```

拆分配置文件时建议遵守以下原则：

| 原则                         | 说明                                      |
| ---------------------------- | ----------------------------------------- |
| 公共配置放 `application.yml` | 应用名称、上下文路径、JSON 格式等         |
| 环境差异配置放 Profile 文件  | 端口、日志级别、数据库地址、Redis 地址等  |
| 敏感配置不写入仓库           | 密码、密钥、Token 使用环境变量或配置中心  |
| 生产环境配置保持克制         | 不开启过多 debug 日志，不输出敏感信息     |
| Profile 名称统一             | 推荐统一使用 `dev`、`test`、`pre`、`prod` |

### 启动时指定环境

启动时指定环境是多环境配置最常用的使用方式。开发阶段可以在配置文件中指定 `spring.profiles.active=dev`，但测试、预发、生产环境更推荐通过启动命令、环境变量、JVM 参数或部署平台注入，避免将生产环境选择写死在 Jar 包内部。Spring Boot 支持通过外部配置来源覆盖配置文件中的 Profile 设置。([Home](https://docs.spring.io/spring-boot/3.5-SNAPSHOT/reference/features/external-config.html?utm_source=chatgpt.com))

使用命令行参数指定环境：

```bash
# 启动开发环境
java -jar order-service.jar --spring.profiles.active=dev

# 启动测试环境
java -jar order-service.jar --spring.profiles.active=test

# 启动生产环境
java -jar order-service.jar --spring.profiles.active=prod
```

使用 JVM 参数指定环境：

```bash
# 使用 JVM 参数激活生产环境
java -Dspring.profiles.active=prod -jar order-service.jar
```

使用环境变量指定环境：

```bash
# Linux / macOS
SPRING_PROFILES_ACTIVE=prod java -jar order-service.jar
```

Docker 启动示例：

```bash
docker run -d \
  --name order-service \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e SERVER_PORT=8080 \
  order-service:1.0.0
```

Kubernetes 环境变量示例：

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: order-service
spec:
  replicas: 2
  selector:
    matchLabels:
      app: order-service
  template:
    metadata:
      labels:
        app: order-service
    spec:
      containers:
        - name: order-service
          image: order-service:1.0.0
          ports:
            - containerPort: 8080
          env:
            # 激活生产环境配置
            - name: SPRING_PROFILES_ACTIVE
              value: prod
            # 覆盖服务端口
            - name: SERVER_PORT
              value: "8080"
```

启动后可以从日志中确认激活环境。Spring Boot 启动日志通常会输出当前 active profile 信息，例如：

```text
The following 1 profile is active: "prod"
```

多环境启动排查建议如下：

| 问题                      | 排查方式                                                     |
| ------------------------- | ------------------------------------------------------------ |
| 启动后仍然加载开发配置    | 检查 `SPRING_PROFILES_ACTIVE`、`--spring.profiles.active` 是否正确 |
| 配置文件存在但没有生效    | 检查文件名是否为 `application-{profile}.yml`                 |
| 多个 Profile 配置覆盖异常 | 检查 Profile 顺序，例如 `prod,live` 中后面的 `live` 优先     |
| Docker 环境变量不生效     | 检查变量名是否使用下划线格式，例如 `SPRING_PROFILES_ACTIVE`  |
| 生产环境端口不符合预期    | 检查是否被 `SERVER_PORT` 或 `--server.port` 覆盖             |

生产环境推荐通过启动参数或环境变量显式指定 Profile，不建议依赖 `application.yml` 中的默认 `spring.profiles.active`。这样可以让同一个构建产物在不同环境中复用，并减少因打包配置错误导致的环境串用问题。

## 配置属性绑定

配置属性绑定用于将配置文件中的参数注入到 Java 对象中。Spring Boot 支持通过 `@Value` 读取单个配置项，也支持通过 `@ConfigurationProperties` 将一组配置绑定为类型安全的对象。官方文档将这类能力归入外部化配置，配置值可以通过 `@Value` 注入，也可以通过 `Environment` 访问，或通过 `@ConfigurationProperties` 绑定为结构化对象。 ([Home](https://docs.spring.io/spring-boot/docs/3.0.x/reference/html/features.html?utm_source=chatgpt.com))

### @Value 使用方式

`@Value` 适合读取少量、简单、分散的配置项，例如某个开关、单个路径、单个超时时间等。它的写法直接，但当配置项较多或存在嵌套结构时，可维护性不如 `@ConfigurationProperties`。

文件位置：`src/main/resources/application.yml`

```yaml
app:
  upload:
    # 文件上传目录
    path: /data/files
    # 单个文件最大大小
    max-size: 100MB
  security:
    # 是否开启接口签名校验
    enable-sign: true
```

下面的组件演示通过 `@Value` 读取单个配置项，并在应用启动后输出关键配置。

文件位置：`src/main/java/io/github/atengk/config/runner/ValueConfigRunner.java`

```java
package io.github.atengk.config.runner;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Value 配置读取示例
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Component
public class ValueConfigRunner implements CommandLineRunner {

    /**
     * 文件上传目录
     */
    @Value("${app.upload.path}")
    private String uploadPath;

    /**
     * 单个文件最大大小
     */
    @Value("${app.upload.max-size:50MB}")
    private String maxSize;

    /**
     * 是否开启接口签名校验
     */
    @Value("${app.security.enable-sign:false}")
    private Boolean enableSign;

    /**
     * 应用启动后检查基础配置
     *
     * @param args 启动参数
     */
    @Override
    public void run(String... args) {
        if (StrUtil.isBlank(uploadPath)) {
            log.warn("文件上传目录未配置，请检查 app.upload.path");
            return;
        }

        log.info("配置读取完成，上传目录：{}，最大文件大小：{}，接口签名：{}", uploadPath, maxSize, enableSign);
    }
}
```

`@Value` 支持默认值写法，例如 `${app.upload.max-size:50MB}` 表示配置不存在时使用 `50MB`。这种方式适合简单配置兜底，但不适合承载复杂配置结构。Spring Boot 官方文档也说明，`@ConfigurationProperties` 相比 `@Value` 支持更完整的类型安全配置能力、宽松绑定和配置元数据。([Home](https://docs.spring.io/spring-boot/docs/3.0.x/reference/html/features.html?utm_source=chatgpt.com))

常见写法如下：

| 写法                                          | 说明                           |
| --------------------------------------------- | ------------------------------ |
| `@Value("${app.name}")`                       | 必须存在配置，不存在会启动失败 |
| `@Value("${app.name:demo}")`                  | 配置不存在时使用默认值         |
| `@Value("${app.security.enable-sign:false}")` | 布尔值配置读取                 |
| `@Value("${server.port}")`                    | 读取 Spring Boot 内置配置项    |

使用 `@Value` 时建议遵守以下原则：

| 建议             | 说明                                             |
| ---------------- | ------------------------------------------------ |
| 只用于少量配置   | 配置项多时使用 `@ConfigurationProperties`        |
| 设置合理默认值   | 对非关键配置可以使用冒号设置默认值               |
| 不读取复杂对象   | 嵌套对象、集合、Map 不建议使用 `@Value`          |
| Key 使用规范命名 | 推荐使用小写和中横线，例如 `app.upload.max-size` |

### @ConfigurationProperties 使用方式

`@ConfigurationProperties` 适合绑定一组配置项，可以将配置文件中的层级结构映射为 Java 对象。它比 `@Value` 更适合业务配置、第三方服务配置、文件上传配置、安全配置、限流配置等成组配置场景。`@ConfigurationProperties` 可以标注在类或 `@Bean` 方法上，用于绑定和校验外部化配置。([Home](https://docs.spring.vmware.com/spring-boot/docs/3.1.15.2/api/org/springframework/boot/context/properties/ConfigurationProperties.html?utm_source=chatgpt.com))

配置文件示例：

文件位置：`src/main/resources/application.yml`

```yaml
app:
  upload:
    # 文件上传目录
    path: /data/files
    # 单个文件最大大小
    max-size: 100MB
    # 允许上传的文件后缀
    allow-extensions:
      - jpg
      - png
      - pdf
  security:
    # 是否开启接口签名校验
    enable-sign: true
    # 请求签名有效期，单位秒
    sign-expire-seconds: 300
```

下面的配置类用于绑定 `app` 前缀下的业务配置。

文件位置：`src/main/java/io/github/atengk/config/properties/AppProperties.java`

```java
package io.github.atengk.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 应用业务配置属性
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    /**
     * 文件上传配置
     */
    private Upload upload = new Upload();

    /**
     * 安全配置
     */
    private Security security = new Security();

    /**
     * 文件上传配置
     *
     * @author Ateng
     * @since 2026-05-06
     */
    @Data
    public static class Upload {

        /**
         * 文件上传目录
         */
        private String path;

        /**
         * 单个文件最大大小
         */
        private String maxSize = "50MB";

        /**
         * 允许上传的文件后缀
         */
        private List<String> allowExtensions = new ArrayList<>();
    }

    /**
     * 安全配置
     *
     * @author Ateng
     * @since 2026-05-06
     */
    @Data
    public static class Security {

        /**
         * 是否开启接口签名校验
         */
        private Boolean enableSign = false;

        /**
         * 请求签名有效期，单位秒
         */
        private Integer signExpireSeconds = 300;
    }
}
```

下面的组件演示如何注入并使用配置对象。

文件位置：`src/main/java/io/github/atengk/config/runner/PropertiesConfigRunner.java`

```java
package io.github.atengk.config.runner;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.config.properties.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * ConfigurationProperties 配置读取示例
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PropertiesConfigRunner implements CommandLineRunner {

    private final AppProperties appProperties;

    /**
     * 应用启动后检查业务配置
     *
     * @param args 启动参数
     */
    @Override
    public void run(String... args) {
        AppProperties.Upload upload = appProperties.getUpload();

        if (StrUtil.isBlank(upload.getPath())) {
            log.warn("文件上传目录未配置，请检查 app.upload.path");
            return;
        }

        if (CollUtil.isEmpty(upload.getAllowExtensions())) {
            log.warn("文件上传后缀白名单为空，请检查 app.upload.allow-extensions");
        }

        log.info("业务配置加载完成，上传目录：{}，最大文件大小：{}，允许后缀：{}",
                upload.getPath(),
                upload.getMaxSize(),
                upload.getAllowExtensions());
    }
}
```

`@ConfigurationProperties` 的常见优势如下：

| 能力       | 说明                                                         |
| ---------- | ------------------------------------------------------------ |
| 类型安全   | 配置可以绑定为 `String`、`Integer`、`Boolean`、`Duration`、`DataSize`、集合等类型 |
| 结构清晰   | 一组相关配置集中维护在一个配置类中                           |
| 支持嵌套   | 可以绑定嵌套对象                                             |
| 支持集合   | 可以绑定 `List`、`Set`、`Map`                                |
| 支持校验   | 可结合 `@Validated` 和 Jakarta Validation 做启动时校验       |
| 支持元数据 | 配合 `spring-boot-configuration-processor` 可生成 IDE 配置提示 |

### 配置类注册方式

`@ConfigurationProperties` 配置类需要注册到 Spring 容器后才能被其他 Bean 注入使用。Spring Boot 支持多种注册方式，常见方式包括 `@Component`、`@EnableConfigurationProperties` 和 `@ConfigurationPropertiesScan`。官方 API 文档说明，`@EnableConfigurationProperties` 用于启用 `@ConfigurationProperties` Bean 支持，也可以直接指定需要注册的配置类。([Home](https://docs.spring.io/spring-boot/3.3.7/api/java/org/springframework/boot/context/properties/EnableConfigurationProperties.html?utm_source=chatgpt.com))

方式一：在配置类上添加 `@Component`。

文件位置：`src/main/java/io/github/atengk/config/properties/AppProperties.java`

```java
package io.github.atengk.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 应用业务配置属性
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    /**
     * 应用名称
     */
    private String name;
}
```

这种方式简单直接，但会让配置类同时承担“配置绑定类”和“Spring 组件”的角色。小型项目可以使用，复杂项目更推荐使用扫描或显式启用方式。

方式二：在启动类上使用 `@EnableConfigurationProperties`。

文件位置：`src/main/java/io/github/atengk/ConfigApplication.java`

```java
package io.github.atengk;

import io.github.atengk.config.properties.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * 配置示例应用启动类
 *
 * @author Ateng
 * @since 2026-05-06
 */
@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
public class ConfigApplication {

    /**
     * 应用启动入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(ConfigApplication.class, args);
    }
}
```

方式三：在启动类上使用 `@ConfigurationPropertiesScan`。

文件位置：`src/main/java/io/github/atengk/ConfigApplication.java`

```java
package io.github.atengk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * 配置示例应用启动类
 *
 * @author Ateng
 * @since 2026-05-06
 */
@SpringBootApplication
@ConfigurationPropertiesScan(basePackages = "io.github.atengk.config.properties")
public class ConfigApplication {

    /**
     * 应用启动入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(ConfigApplication.class, args);
    }
}
```

三种方式对比如下：

| 注册方式                         | 适用场景             | 推荐程度 |
| -------------------------------- | -------------------- | -------- |
| `@Component`                     | 简单项目、配置类较少 | 可用     |
| `@EnableConfigurationProperties` | 显式注册少量配置类   | 推荐     |
| `@ConfigurationPropertiesScan`   | 配置类较多、统一扫描 | 推荐     |

项目中更推荐将配置类集中放在 `config.properties` 包下，再通过 `@ConfigurationPropertiesScan` 统一扫描，这样结构清晰，也方便后续新增配置类。

## 自定义配置开发

自定义配置开发用于管理业务系统自己的参数，例如上传目录、接口签名、短信服务、对象存储、任务开关、限流阈值、默认分页大小等。自定义配置建议统一前缀，避免和 Spring Boot 内置配置项冲突。

### 自定义业务配置项

自定义业务配置项应放在统一命名空间下，例如 `app`、`biz`、`custom`。其中 `app` 更适合表达当前应用级别的业务配置。

文件位置：`src/main/resources/application.yml`

```yaml
app:
  # 系统标题
  title: Spring Boot 配置示例
  # 默认分页大小
  default-page-size: 20
  # 是否开启演示模式
  demo-mode: false
  # 接口文档地址
  api-doc-url: http://localhost:8080/doc.html
```

配置类如下：

文件位置：`src/main/java/io/github/atengk/config/properties/BusinessProperties.java`

```java
package io.github.atengk.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 业务基础配置属性
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@ConfigurationProperties(prefix = "app")
public class BusinessProperties {

    /**
     * 系统标题
     */
    private String title = "Spring Boot 应用";

    /**
     * 默认分页大小
     */
    private Integer defaultPageSize = 20;

    /**
     * 是否开启演示模式
     */
    private Boolean demoMode = false;

    /**
     * 接口文档地址
     */
    private String apiDocUrl;
}
```

下面的服务类演示在业务逻辑中使用自定义配置。

文件位置：`src/main/java/io/github/atengk/config/service/SystemInfoService.java`

```java
package io.github.atengk.config.service;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.config.properties.BusinessProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 系统信息服务
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemInfoService {

    private final BusinessProperties businessProperties;

    /**
     * 获取系统基础信息
     *
     * @return 系统基础信息
     */
    public Map<String, Object> getSystemInfo() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("title", businessProperties.getTitle());
        result.put("defaultPageSize", businessProperties.getDefaultPageSize());
        result.put("demoMode", businessProperties.getDemoMode());

        if (StrUtil.isNotBlank(businessProperties.getApiDocUrl())) {
            result.put("apiDocUrl", businessProperties.getApiDocUrl());
        }

        log.info("读取系统基础配置完成，系统标题：{}", businessProperties.getTitle());
        return result;
    }
}
```

自定义配置命名建议如下：

| 配置项       | 推荐命名                   | 说明                                  |
| ------------ | -------------------------- | ------------------------------------- |
| 上传目录     | `app.upload.path`          | 使用清晰业务分组                      |
| 默认分页大小 | `app.default-page-size`    | 使用小写和中横线                      |
| 接口签名开关 | `app.security.enable-sign` | 布尔值建议以 `enable` 开头            |
| 接口超时时间 | `app.remote.timeout`       | 时间配置建议明确单位或使用 `Duration` |

### 嵌套配置对象

嵌套配置对象适合表达有层级关系的业务配置，例如上传配置、安全配置、远程接口配置、对象存储配置等。使用嵌套对象可以避免配置类字段过多，也能让配置结构更接近业务语义。Spring Boot 配置元数据支持嵌套属性，注解处理器会根据 `@ConfigurationProperties` 自动生成多数配置元数据。([Home](https://docs.spring.io/spring-boot/specification/configuration-metadata/index.html?utm_source=chatgpt.com))

文件位置：`src/main/resources/application.yml`

```yaml
app:
  storage:
    # 存储类型：local、minio、oss
    type: local
    local:
      # 本地存储目录
      base-path: /data/storage
      # 是否按日期分目录
      date-path-enabled: true
    minio:
      # MinIO 服务地址
      endpoint: http://localhost:9000
      # 访问密钥
      access-key: minioadmin
      # 访问密钥对应的 Secret
      secret-key: minioadmin
      # 默认桶名称
      bucket-name: app-files
```

配置类如下：

文件位置：`src/main/java/io/github/atengk/config/properties/StorageProperties.java`

```java
package io.github.atengk.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 存储配置属性
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@ConfigurationProperties(prefix = "app.storage")
public class StorageProperties {

    /**
     * 存储类型：local、minio、oss
     */
    private String type = "local";

    /**
     * 本地存储配置
     */
    private Local local = new Local();

    /**
     * MinIO 存储配置
     */
    private Minio minio = new Minio();

    /**
     * 本地存储配置
     *
     * @author Ateng
     * @since 2026-05-06
     */
    @Data
    public static class Local {

        /**
         * 本地存储目录
         */
        private String basePath = "/data/storage";

        /**
         * 是否按日期分目录
         */
        private Boolean datePathEnabled = true;
    }

    /**
     * MinIO 存储配置
     *
     * @author Ateng
     * @since 2026-05-06
     */
    @Data
    public static class Minio {

        /**
         * MinIO 服务地址
         */
        private String endpoint;

        /**
         * 访问密钥
         */
        private String accessKey;

        /**
         * 访问密钥对应的 Secret
         */
        private String secretKey;

        /**
         * 默认桶名称
         */
        private String bucketName;
    }
}
```

嵌套配置对象使用建议如下：

| 建议                     | 说明                                            |
| ------------------------ | ----------------------------------------------- |
| 按业务分组               | 例如 `storage.local`、`storage.minio`           |
| 给嵌套对象设置默认实例   | 避免业务代码调用时出现空指针                    |
| 敏感字段谨慎输出         | `secretKey`、`password`、`token` 不要打印到日志 |
| 多种实现使用类型字段区分 | 例如 `type: local`、`type: minio`               |

### 集合类型配置

集合类型配置适合白名单、黑名单、多服务端点、多租户配置、多规则配置等场景。Spring Boot 可以将 YAML 中的数组绑定到 `List`，也可以将对象结构绑定到 `Map` 或对象列表。

文件位置：`src/main/resources/application.yml`

```yaml
app:
  access:
    # IP 白名单
    white-ips:
      - 127.0.0.1
      - 192.168.1.10

    # 不需要鉴权的接口路径
    ignore-paths:
      - /actuator/health
      - /login
      - /doc.html

    # 第三方系统配置
    clients:
      - code: user-center
        name: 用户中心
        endpoint: http://localhost:9001
        timeout-seconds: 5
      - code: order-center
        name: 订单中心
        endpoint: http://localhost:9002
        timeout-seconds: 10
```

配置类如下：

文件位置：`src/main/java/io/github/atengk/config/properties/AccessProperties.java`

```java
package io.github.atengk.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 访问控制配置属性
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@ConfigurationProperties(prefix = "app.access")
public class AccessProperties {

    /**
     * IP 白名单
     */
    private List<String> whiteIps = new ArrayList<>();

    /**
     * 忽略鉴权的接口路径
     */
    private List<String> ignorePaths = new ArrayList<>();

    /**
     * 第三方系统配置
     */
    private List<Client> clients = new ArrayList<>();

    /**
     * 第三方系统配置
     *
     * @author Ateng
     * @since 2026-05-06
     */
    @Data
    public static class Client {

        /**
         * 系统编码
         */
        private String code;

        /**
         * 系统名称
         */
        private String name;

        /**
         * 服务地址
         */
        private String endpoint;

        /**
         * 超时时间，单位秒
         */
        private Integer timeoutSeconds = 5;
    }
}
```

下面的服务类演示如何查询第三方系统配置，并处理配置不存在的情况。

文件位置：`src/main/java/io/github/atengk/config/service/ClientConfigService.java`

```java
package io.github.atengk.config.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.config.properties.AccessProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 第三方系统配置服务
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClientConfigService {

    private final AccessProperties accessProperties;

    /**
     * 根据系统编码查询第三方系统配置
     *
     * @param code 系统编码
     * @return 第三方系统配置
     */
    public Optional<AccessProperties.Client> findClientByCode(String code) {
        if (StrUtil.isBlank(code) || CollUtil.isEmpty(accessProperties.getClients())) {
            log.warn("第三方系统配置查询参数为空或配置列表为空，code：{}", code);
            return Optional.empty();
        }

        return accessProperties.getClients()
                .stream()
                .filter(item -> StrUtil.equals(item.getCode(), code))
                .findFirst();
    }
}
```

集合配置建议遵守以下规则：

| 建议                              | 说明                                   |
| --------------------------------- | -------------------------------------- |
| 简单字符串列表使用 `List<String>` | 例如白名单、忽略路径                   |
| 复杂结构使用 `List<对象>`         | 例如多个第三方系统配置                 |
| 集合字段设置默认空集合            | 避免空指针                             |
| 对关键集合做配置校验              | 例如生产环境白名单、客户端地址不能为空 |

## 配置校验

配置校验用于在应用启动阶段检查关键配置是否正确。相比运行时才暴露问题，启动时失败可以更早发现配置缺失、格式错误、范围不合法、嵌套配置不完整等问题。Spring Boot 在 `@ConfigurationProperties` 类添加 `@Validated` 后，会尝试执行配置校验；校验注解来自 Jakarta Validation。([Home](https://docs.spring.io/spring-boot/docs/3.0.x/reference/html/features.html?utm_source=chatgpt.com))

### 参数校验依赖

Spring Boot 3 使用 Jakarta Validation。如果项目中需要对配置类使用 `@NotBlank`、`@NotNull`、`@Min`、`@Max`、`@Valid` 等校验注解，需要引入 `spring-boot-starter-validation`。

文件位置：`pom.xml`

```xml
<dependencies>
    <!-- Spring Boot Web 基础依赖，提供 Web 开发能力 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- 配置校验依赖，提供 Jakarta Validation 支持 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- 配置元数据生成器，提供 application.yml 配置提示 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-configuration-processor</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- Lombok，简化 Getter、Setter、构造方法等样板代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- Hutool 工具包，提供字符串、集合等常用工具类 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.8.40</version>
    </dependency>
</dependencies>
```

`spring-boot-configuration-processor` 不是配置绑定必需依赖，但建议在开发中引入。Spring Boot 官方说明，该处理器会在编译期处理 `@ConfigurationProperties` 并生成配置元数据，用于 IDE 在 `application.properties` 或 `application.yaml` 中提供配置提示和补全。([Home](https://docs.spring.io/spring-boot/specification/configuration-metadata/index.html?utm_source=chatgpt.com))

### 配置项校验规则

配置项校验通常写在 `@ConfigurationProperties` 配置类中。对类添加 `@Validated`，再对字段添加 Jakarta Validation 约束注解即可。对于嵌套对象，如果希望触发嵌套字段校验，需要在嵌套对象字段上添加 `@Valid`。官方文档明确说明，嵌套属性要始终触发校验时，需要在对应字段上添加 `@Valid`。([Home](https://docs.spring.io/spring-boot/docs/3.0.x/reference/html/features.html?utm_source=chatgpt.com))

文件位置：`src/main/resources/application.yml`

```yaml
app:
  remote:
    # 是否启用远程调用
    enabled: true
    # 远程服务地址
    endpoint: http://localhost:9000
    # 连接超时时间
    connect-timeout-seconds: 3
    # 读取超时时间
    read-timeout-seconds: 10
    auth:
      # 认证客户端 ID
      client-id: config-demo
      # 认证密钥，生产环境建议通过环境变量注入
      client-secret: local-secret
```

下面的配置类对远程调用配置进行启动校验。

文件位置：`src/main/java/io/github/atengk/config/properties/RemoteProperties.java`

```java
package io.github.atengk.config.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.URL;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 远程服务配置属性
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@Validated
@ConfigurationProperties(prefix = "app.remote")
public class RemoteProperties {

    /**
     * 是否启用远程调用
     */
    @NotNull(message = "远程调用启用状态不能为空")
    private Boolean enabled = false;

    /**
     * 远程服务地址
     */
    @NotBlank(message = "远程服务地址不能为空")
    @URL(message = "远程服务地址格式不正确")
    private String endpoint;

    /**
     * 连接超时时间，单位秒
     */
    @NotNull(message = "连接超时时间不能为空")
    @Min(value = 1, message = "连接超时时间不能小于 1 秒")
    @Max(value = 60, message = "连接超时时间不能大于 60 秒")
    private Integer connectTimeoutSeconds = 3;

    /**
     * 读取超时时间，单位秒
     */
    @NotNull(message = "读取超时时间不能为空")
    @Min(value = 1, message = "读取超时时间不能小于 1 秒")
    @Max(value = 300, message = "读取超时时间不能大于 300 秒")
    private Integer readTimeoutSeconds = 10;

    /**
     * 认证配置
     */
    @Valid
    @NotNull(message = "认证配置不能为空")
    private Auth auth = new Auth();

    /**
     * 认证配置
     *
     * @author Ateng
     * @since 2026-05-06
     */
    @Data
    public static class Auth {

        /**
         * 认证客户端 ID
         */
        @NotBlank(message = "认证客户端 ID 不能为空")
        private String clientId;

        /**
         * 认证密钥
         */
        @NotBlank(message = "认证密钥不能为空")
        private String clientSecret;
    }
}
```

常用校验注解如下：

| 注解        | 说明                         | 适用字段                   |
| ----------- | ---------------------------- | -------------------------- |
| `@NotNull`  | 不能为 `null`                | `Integer`、`Boolean`、对象 |
| `@NotBlank` | 字符串不能为空且不能全是空白 | `String`                   |
| `@NotEmpty` | 集合或字符串不能为空         | `List`、`Map`、`String`    |
| `@Min`      | 数值不能小于指定值           | `Integer`、`Long`          |
| `@Max`      | 数值不能大于指定值           | `Integer`、`Long`          |
| `@Pattern`  | 必须匹配正则表达式           | `String`                   |
| `@Valid`    | 触发嵌套对象校验             | 嵌套对象、对象集合         |
| `@URL`      | 必须是 URL 格式              | `String`                   |

配置校验建议如下：

| 场景     | 建议                                   |
| -------- | -------------------------------------- |
| 服务地址 | 使用 `@NotBlank`、`@URL`               |
| 超时时间 | 使用 `@Min`、`@Max`                    |
| 开关配置 | 使用 `@NotNull`，避免布尔值缺失        |
| 密钥配置 | 使用 `@NotBlank`，生产环境通过外部注入 |
| 嵌套配置 | 嵌套字段添加 `@Valid`                  |
| 集合配置 | 使用 `@NotEmpty`，对象元素内部继续校验 |

### 启动失败处理

配置校验失败时，Spring Boot 会在应用启动阶段抛出异常并终止启动。这样可以避免应用带着错误配置继续运行，导致接口调用失败、文件写入异常、第三方服务认证失败等运行时问题。

例如配置文件缺失 `client-secret`：

文件位置：`src/main/resources/application.yml`

```yaml
app:
  remote:
    enabled: true
    endpoint: http://localhost:9000
    connect-timeout-seconds: 3
    read-timeout-seconds: 10
    auth:
      client-id: config-demo
      # client-secret 未配置，启动时会触发校验失败
```

启动时可能出现类似错误信息：

```text
Binding to target org.springframework.boot.context.properties.bind.BindException failed:

Property: app.remote.auth.clientSecret
Value: null
Reason: 认证密钥不能为空
```

处理配置校验失败时，应优先根据错误中的 `Property`、`Value`、`Reason` 三部分定位问题：

| 错误信息                    | 排查重点                       |
| --------------------------- | ------------------------------ |
| `Property`                  | 哪个配置项绑定失败             |
| `Value`                     | 当前绑定到的值是什么           |
| `Reason`                    | 违反了哪条校验规则             |
| `BindException`             | 配置绑定或校验失败             |
| `Failed to bind properties` | 配置前缀、字段类型或配置值异常 |

常见失败原因如下：

| 失败原因       | 示例                           | 处理方式                                                     |
| -------------- | ------------------------------ | ------------------------------------------------------------ |
| 必填配置缺失   | `app.remote.endpoint` 未配置   | 补充配置项                                                   |
| 配置类型错误   | `connect-timeout-seconds: abc` | 改为合法数字                                                 |
| 数值范围非法   | `connect-timeout-seconds: 0`   | 调整到允许范围                                               |
| URL 格式错误   | `endpoint: localhost:9000`     | 改为 `http://localhost:9000`                                 |
| 嵌套对象未校验 | `auth` 内字段未触发校验        | 在嵌套字段上添加 `@Valid`                                    |
| 配置类未注册   | 配置没有绑定到对象             | 检查 `@ConfigurationPropertiesScan` 或 `@EnableConfigurationProperties` |

建议在项目中增加一个配置检查组件，用于输出非敏感关键配置，辅助确认配置加载结果。注意不要输出密码、密钥、Token 等敏感字段。

文件位置：`src/main/java/io/github/atengk/config/runner/RemoteConfigCheckRunner.java`

```java
package io.github.atengk.config.runner;

import cn.hutool.core.util.DesensitizedUtil;
import io.github.atengk.config.properties.RemoteProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 远程服务配置检查
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RemoteConfigCheckRunner implements CommandLineRunner {

    private final RemoteProperties remoteProperties;

    /**
     * 应用启动后输出远程服务关键配置
     *
     * @param args 启动参数
     */
    @Override
    public void run(String... args) {
        String clientSecret = remoteProperties.getAuth().getClientSecret();
        String desensitizedSecret = DesensitizedUtil.password(clientSecret);

        log.info("远程服务配置检查完成，启用状态：{}，服务地址：{}，连接超时：{} 秒，读取超时：{} 秒，客户端 ID：{}，客户端密钥：{}",
                remoteProperties.getEnabled(),
                remoteProperties.getEndpoint(),
                remoteProperties.getConnectTimeoutSeconds(),
                remoteProperties.getReadTimeoutSeconds(),
                remoteProperties.getAuth().getClientId(),
                desensitizedSecret);
    }
}
```

启动验证命令如下：

```bash
# 使用 dev 环境启动应用
java -jar config-demo.jar --spring.profiles.active=dev

# 临时覆盖远程服务地址
java -jar config-demo.jar \
  --spring.profiles.active=dev \
  --app.remote.endpoint=http://localhost:9000
```

如果启动成功，说明配置文件可以正常绑定并通过校验。如果启动失败，应优先修复配置，而不是删除校验规则。关键配置越早失败，线上运行风险越低。

## 外部化配置

外部化配置用于将应用运行参数从代码和 Jar 包中解耦出来，使同一套 Spring Boot 应用可以在不同环境中通过不同配置启动。Spring Boot 支持配置文件、环境变量、JVM System properties、命令行参数、`SPRING_APPLICATION_JSON` 等多种配置来源，并会按照固定优先级合并到 Spring `Environment` 中。 ([Home](https://docs.spring.io/spring-boot/3.5-SNAPSHOT/reference/features/external-config.html?utm_source=chatgpt.com))

### 命令行参数配置

命令行参数适合在应用启动时临时覆盖配置文件中的值，例如临时修改端口、指定运行环境、调整日志级别、指定外部配置文件位置等。Spring Boot 默认会将以 `--` 开头的命令行参数转换为配置属性并加入到 `Environment`，并且命令行参数优先级高于基于文件的配置来源。([Home](https://docs.spring.io/spring-boot/3.5-SNAPSHOT/reference/features/external-config.html?utm_source=chatgpt.com))

常见启动方式如下：

```bash
# 指定服务端口
java -jar config-demo.jar --server.port=9000

# 指定运行环境
java -jar config-demo.jar --spring.profiles.active=prod

# 同时指定运行环境、服务端口和日志级别
java -jar config-demo.jar \
  --spring.profiles.active=prod \
  --server.port=9000 \
  --logging.level.io.github.atengk=info

# 指定外部配置文件名称，默认名称 application 会变为 config-demo
java -jar config-demo.jar --spring.config.name=config-demo

# 指定外部配置文件位置
java -jar config-demo.jar --spring.config.location=optional:file:/data/config/application-prod.yml
```

上面的命令中，`--server.port=9000` 会覆盖 `application.yml` 中的 `server.port`。`--spring.profiles.active=prod` 会激活生产环境配置，应用会尝试加载 `application-prod.yml` 或 `application-prod.properties`。

如果项目希望支持更短的命令行参数，例如使用 `--port=9000` 代替 `--server.port=9000`，可以在配置文件中使用占位符进行映射。Spring Boot 支持在配置文件中使用 `${name:default}` 形式的属性占位符。([Home](https://docs.spring.io/spring-boot/3.5/how-to/properties-and-configuration.html?utm_source=chatgpt.com))

文件位置：`src/main/resources/application.yml`

```yaml
server:
  # 支持通过 --port=9000 覆盖端口，未指定时使用 8080
  port: ${port:8080}
```

启动命令如下：

```bash
# 使用短参数覆盖 server.port
java -jar config-demo.jar --port=9000
```

命令行参数适合临时覆盖，但不建议长期在手工命令中维护大量业务配置。对于数据库密码、密钥、Token 等敏感信息，更推荐使用环境变量、配置中心或密钥管理服务。

### 环境变量配置

环境变量适合容器化部署、Kubernetes 部署、CI/CD 注入配置和生产环境敏感配置管理。大多数操作系统对环境变量名称有限制，Spring Boot 的宽松绑定规则支持将配置项转换为环境变量形式：点号替换为下划线、移除中横线、转为大写。例如 `spring.main.log-startup-info` 可以转换为 `SPRING_MAIN_LOGSTARTUPINFO`。([docs.enterprise.spring.io](https://docs.enterprise.spring.io/spring-boot/reference/features/external-config.html?utm_source=chatgpt.com))

常见配置项与环境变量映射如下：

| 配置项                     | 环境变量                  | 说明           |
| -------------------------- | ------------------------- | -------------- |
| `server.port`              | `SERVER_PORT`             | 服务端口       |
| `spring.profiles.active`   | `SPRING_PROFILES_ACTIVE`  | 当前运行环境   |
| `spring.application.name`  | `SPRING_APPLICATION_NAME` | 应用名称       |
| `logging.level.root`       | `LOGGING_LEVEL_ROOT`      | 根日志级别     |
| `app.upload.path`          | `APP_UPLOAD_PATH`         | 自定义上传目录 |
| `app.security.enable-sign` | `APP_SECURITY_ENABLESIGN` | 自定义签名开关 |

Linux 或 macOS 启动示例：

```bash
# 设置环境变量后启动应用
export SPRING_PROFILES_ACTIVE=prod
export SERVER_PORT=9000
export APP_UPLOAD_PATH=/data/files

java -jar config-demo.jar
```

也可以在单条命令中指定环境变量：

```bash
# 单次启动生效，不影响当前终端后续环境
SPRING_PROFILES_ACTIVE=prod SERVER_PORT=9000 java -jar config-demo.jar
```

Docker 启动示例：

```bash
docker run -d \
  --name config-demo \
  -p 9000:9000 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e SERVER_PORT=9000 \
  -e APP_UPLOAD_PATH=/data/files \
  config-demo:1.0.0
```

Docker Compose 示例：

```yaml
services:
  config-demo:
    image: config-demo:1.0.0
    container_name: config-demo
    ports:
      - "9000:9000"
    environment:
      # 激活生产环境
      SPRING_PROFILES_ACTIVE: prod
      # 覆盖服务端口
      SERVER_PORT: "9000"
      # 覆盖上传目录
      APP_UPLOAD_PATH: /data/files
    volumes:
      # 挂载业务文件目录
      - /data/files:/data/files
```

Kubernetes 示例：

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: config-demo
spec:
  replicas: 2
  selector:
    matchLabels:
      app: config-demo
  template:
    metadata:
      labels:
        app: config-demo
    spec:
      containers:
        - name: config-demo
          image: config-demo:1.0.0
          ports:
            - containerPort: 9000
          env:
            # 激活生产环境配置
            - name: SPRING_PROFILES_ACTIVE
              value: prod
            # 覆盖服务端口
            - name: SERVER_PORT
              value: "9000"
            # 覆盖上传目录
            - name: APP_UPLOAD_PATH
              value: /data/files
```

环境变量的优点是和部署平台集成方便，不需要修改 Jar 包，也不需要为不同环境重新打包。缺点是复杂嵌套配置和集合配置可读性不如 YAML，配置项较多时应考虑配置中心、挂载外部配置文件或 ConfigMap。

### JVM 参数配置

JVM 参数配置通过 `-Dkey=value` 写入 Java System properties。Spring Boot 会将 Java System properties 作为配置来源之一加入 `Environment`，其优先级高于操作系统环境变量，但低于命令行参数。([Home](https://docs.spring.io/spring-boot/3.5-SNAPSHOT/reference/features/external-config.html?utm_source=chatgpt.com))

常见启动方式如下：

```bash
# 使用 JVM 参数指定服务端口
java -Dserver.port=9000 -jar config-demo.jar

# 使用 JVM 参数指定运行环境
java -Dspring.profiles.active=prod -jar config-demo.jar

# 同时指定 JVM 运行参数和 Spring Boot 配置参数
java \
  -Xms512m \
  -Xmx1024m \
  -Dspring.profiles.active=prod \
  -Dserver.port=9000 \
  -jar config-demo.jar
```

`-D` 参数必须放在 `-jar` 前面，否则可能不会被 JVM 正确识别为 System property。下面是错误示例：

```bash
# 不推荐：-D 参数放在 -jar 后面，容易被当成应用参数而不是 JVM System property
java -jar config-demo.jar -Dserver.port=9000
```

如果要通过 JVM 参数传入 JSON 配置，可以使用 `spring.application.json`。Spring Boot 支持通过系统属性、环境变量或命令行参数传入 JSON 配置块，并解析到 `Environment` 中。([Home](https://docs.spring.io/spring-boot/3.5-SNAPSHOT/reference/features/external-config.html?utm_source=chatgpt.com))

```bash
# 使用 JVM 参数传入 JSON 配置
java -Dspring.application.json='{"app":{"upload":{"path":"/data/files"}}}' -jar config-demo.jar
```

JVM 参数适合 JVM 运行时配置和少量 Spring 配置覆盖，例如内存、GC、Profile、端口、日志级别等。大量业务配置不建议全部写在 JVM 参数中，否则启动命令会变得难以维护。

## 敏感配置处理

敏感配置包括数据库密码、Redis 密码、对象存储密钥、JWT 密钥、接口签名密钥、第三方平台 Token、OAuth Secret 等。这类配置不应硬编码在 Java 代码中，也不应直接提交到 Git 仓库。Spring Boot 的外部化配置机制允许通过环境变量、外部配置文件、配置中心或密钥挂载方式注入敏感配置。([Home](https://docs.spring.io/spring-boot/3.5-SNAPSHOT/reference/features/external-config.html?utm_source=chatgpt.com))

### 密码与密钥配置

密码与密钥配置建议只在本地开发环境使用示例值，在测试和生产环境通过外部方式注入。配置文件中可以使用占位符读取环境变量，并提供本地默认值。

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  datasource:
    # 数据库连接地址
    url: ${DB_URL:jdbc:mysql://localhost:3306/config_demo?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai}
    # 数据库用户名
    username: ${DB_USERNAME:root}
    # 数据库密码，生产环境必须通过环境变量注入
    password: ${DB_PASSWORD:root}

app:
  security:
    # JWT 签名密钥，生产环境必须通过环境变量注入
    jwt-secret: ${APP_SECURITY_JWT_SECRET:local-dev-secret}
    # 接口签名密钥，生产环境必须通过环境变量注入
    sign-secret: ${APP_SECURITY_SIGN_SECRET:local-sign-secret}

  minio:
    # MinIO 服务地址
    endpoint: ${MINIO_ENDPOINT:http://localhost:9000}
    # MinIO 访问密钥
    access-key: ${MINIO_ACCESS_KEY:minioadmin}
    # MinIO Secret，生产环境必须通过环境变量注入
    secret-key: ${MINIO_SECRET_KEY:minioadmin}
```

生产环境启动示例：

```bash
export SPRING_PROFILES_ACTIVE=prod
export DB_URL=jdbc:mysql://mysql:3306/config_demo?useUnicode=true\&characterEncoding=utf8\&serverTimezone=Asia/Shanghai
export DB_USERNAME=config_user
export DB_PASSWORD='replace-with-real-password'
export APP_SECURITY_JWT_SECRET='replace-with-real-jwt-secret'
export APP_SECURITY_SIGN_SECRET='replace-with-real-sign-secret'
export MINIO_ENDPOINT=http://minio:9000
export MINIO_ACCESS_KEY=config-demo
export MINIO_SECRET_KEY='replace-with-real-minio-secret'

java -jar config-demo.jar
```

Docker Secret 或文件挂载场景下，可以将密钥以文件形式挂载到容器中，再通过配置树导入。Spring Boot 支持 `configtree:` 方式从目录中导入配置，Docker secrets 挂载到容器后可以通过该方式加入 Spring 环境。([Home](https://docs.spring.io/spring-boot/4.1-SNAPSHOT/reference/features/external-config.html?utm_source=chatgpt.com))

文件位置：`src/main/resources/application-prod.yml`

```yaml
spring:
  config:
    # 从 /run/secrets/ 目录导入密钥文件，optional 表示目录不存在时不强制失败
    import: optional:configtree:/run/secrets/
```

如果 `/run/secrets/` 目录下存在名为 `db.password` 的文件，Spring Boot 可以将其作为配置项 `db.password` 读取。该方式适合容器编排平台或密钥管理系统将敏感信息挂载为文件的场景。

敏感配置管理建议如下：

| 场景     | 推荐方式                                       | 说明                               |
| -------- | ---------------------------------------------- | ---------------------------------- |
| 本地开发 | `.env`、本地环境变量、本地配置文件             | 示例值可以简单，但不要复用生产密钥 |
| 测试环境 | 环境变量、配置中心                             | 避免写入代码仓库                   |
| 生产环境 | 密钥管理服务、Kubernetes Secret、Docker Secret | 权限隔离，减少明文暴露             |
| CI/CD    | 平台 Secret 变量                               | 不在流水线日志中输出               |
| 临时调试 | 命令行或环境变量                               | 调试后及时清理                     |

### 配置脱敏建议

配置脱敏用于避免敏感配置在日志、异常、监控端点、接口响应中明文输出。Spring Boot Actuator 的 `/env`、`/configprops` 等端点返回的信息可能包含敏感内容，Spring Boot 默认会对这些端点中的值进行脱敏处理，以 `******` 替代原始值。([Home](https://docs.spring.io/spring-boot/3.4/reference/actuator/endpoints.html?utm_source=chatgpt.com))

如果项目引入 Actuator，建议只暴露必要端点，并避免在生产环境展示未脱敏配置值。

文件位置：`src/main/resources/application-prod.yml`

```yaml
management:
  endpoints:
    web:
      exposure:
        # 生产环境只暴露必要端点
        include: health,info
  endpoint:
    env:
      # env 端点不展示原始配置值
      show-values: never
    configprops:
      # configprops 端点不展示原始配置值
      show-values: never
```

业务日志中也应避免直接打印密码、密钥和 Token。下面的配置检查类演示如何对敏感字段脱敏后再输出。

文件位置：`src/main/java/io/github/atengk/config/properties/SecurityProperties.java`

```java
package io.github.atengk.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 安全配置属性
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@ConfigurationProperties(prefix = "app.security")
public class SecurityProperties {

    /**
     * JWT 签名密钥
     */
    private String jwtSecret;

    /**
     * 接口签名密钥
     */
    private String signSecret;
}
```

下面的启动检查类会输出脱敏后的配置摘要，不输出原始密钥。

文件位置：`src/main/java/io/github/atengk/config/runner/SensitiveConfigCheckRunner.java`

```java
package io.github.atengk.config.runner;

import cn.hutool.core.util.DesensitizedUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.config.properties.SecurityProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 敏感配置检查
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SensitiveConfigCheckRunner implements CommandLineRunner {

    private final SecurityProperties securityProperties;

    /**
     * 应用启动后检查敏感配置是否存在，并输出脱敏摘要
     *
     * @param args 启动参数
     */
    @Override
    public void run(String... args) {
        if (StrUtil.isBlank(securityProperties.getJwtSecret())) {
            log.warn("JWT 签名密钥未配置，请检查 app.security.jwt-secret");
            return;
        }

        if (StrUtil.isBlank(securityProperties.getSignSecret())) {
            log.warn("接口签名密钥未配置，请检查 app.security.sign-secret");
            return;
        }

        log.info("敏感配置检查完成，jwtSecret：{}，signSecret：{}",
                DesensitizedUtil.password(securityProperties.getJwtSecret()),
                DesensitizedUtil.password(securityProperties.getSignSecret()));
    }
}
```

配置脱敏建议如下：

| 位置          | 建议                               |
| ------------- | ---------------------------------- |
| 启动日志      | 只输出配置是否存在，不输出明文值   |
| 异常信息      | 不拼接密码、Token、Secret          |
| Actuator 端点 | 生产环境限制暴露范围，并保持脱敏   |
| 接口响应      | 不返回内部密钥、连接串、认证配置   |
| CI/CD 日志    | 使用平台 Secret Mask 能力          |
| 配置文件      | 示例值和真实值分离，真实值外部注入 |

## 配置测试与验证

配置测试与验证用于确认配置文件是否能够被正确加载、配置属性是否能够正确绑定、Profile 是否按预期生效、启动日志是否符合预期。Spring Boot 测试支持通过 `@SpringBootTest(properties = "...")`、`@DynamicPropertySource`、`@TestPropertySource` 等方式提供测试配置来源。([Home](https://docs.spring.io/spring-boot/3.5-SNAPSHOT/reference/features/external-config.html?utm_source=chatgpt.com))

### 单元测试验证配置

单元测试验证配置适合在开发阶段检查 `@ConfigurationProperties` 是否绑定正确，避免应用启动后才发现配置缺失或类型不匹配。测试中可以通过 `@SpringBootTest(properties = {...})` 覆盖配置项。

文件位置：`src/test/java/io/github/atengk/config/properties/SecurityPropertiesTest.java`

```java
package io.github.atengk.config.properties;

import cn.hutool.core.util.StrUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 安全配置属性测试
 *
 * @author Ateng
 * @since 2026-05-06
 */
@SpringBootTest(properties = {
        "app.security.jwt-secret=test-jwt-secret",
        "app.security.sign-secret=test-sign-secret"
})
class SecurityPropertiesTest {

    @Autowired
    private SecurityProperties securityProperties;

    /**
     * 验证安全配置可以正常绑定
     */
    @Test
    void shouldBindSecurityProperties() {
        Assertions.assertTrue(StrUtil.isNotBlank(securityProperties.getJwtSecret()));
        Assertions.assertTrue(StrUtil.isNotBlank(securityProperties.getSignSecret()));
        Assertions.assertEquals("test-jwt-secret", securityProperties.getJwtSecret());
        Assertions.assertEquals("test-sign-secret", securityProperties.getSignSecret());
    }
}
```

如果需要测试 Profile 配置是否生效，可以使用 `@ActiveProfiles` 指定测试环境。

文件位置：`src/test/resources/application-test.yml`

```yaml
app:
  upload:
    # 测试环境上传目录
    path: /tmp/config-demo/files
    # 测试环境文件大小
    max-size: 10MB
```

文件位置：`src/test/java/io/github/atengk/config/properties/UploadPropertiesTest.java`

```java
package io.github.atengk.config.properties;

import cn.hutool.core.util.StrUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 上传配置属性测试
 *
 * @author Ateng
 * @since 2026-05-06
 */
@ActiveProfiles("test")
@SpringBootTest
class UploadPropertiesTest {

    @Autowired
    private AppProperties appProperties;

    /**
     * 验证 test Profile 下上传配置可以正常绑定
     */
    @Test
    void shouldBindUploadPropertiesInTestProfile() {
        AppProperties.Upload upload = appProperties.getUpload();

        Assertions.assertTrue(StrUtil.isNotBlank(upload.getPath()));
        Assertions.assertEquals("/tmp/config-demo/files", upload.getPath());
        Assertions.assertEquals("10MB", upload.getMaxSize());
    }
}
```

如果测试需要动态生成配置，例如随机端口、Testcontainers 容器地址、临时 Redis 地址，可以使用 `@DynamicPropertySource`。Spring Boot 的配置优先级中，`@DynamicPropertySource` 属于测试阶段的配置来源之一。([Home](https://docs.spring.io/spring-boot/3.5-SNAPSHOT/reference/features/external-config.html?utm_source=chatgpt.com))

文件位置：`src/test/java/io/github/atengk/config/properties/DynamicConfigTest.java`

```java
package io.github.atengk.config.properties;

import cn.hutool.core.util.StrUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * 动态配置属性测试
 *
 * @author Ateng
 * @since 2026-05-06
 */
@SpringBootTest
class DynamicConfigTest {

    @Autowired
    private SecurityProperties securityProperties;

    /**
     * 动态注册测试配置
     *
     * @param registry 动态配置注册器
     */
    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("app.security.jwt-secret", () -> "dynamic-jwt-secret");
        registry.add("app.security.sign-secret", () -> "dynamic-sign-secret");
    }

    /**
     * 验证动态配置可以正常注入
     */
    @Test
    void shouldBindDynamicProperties() {
        Assertions.assertTrue(StrUtil.isNotBlank(securityProperties.getJwtSecret()));
        Assertions.assertEquals("dynamic-jwt-secret", securityProperties.getJwtSecret());
        Assertions.assertEquals("dynamic-sign-secret", securityProperties.getSignSecret());
    }
}
```

配置测试建议如下：

| 测试目标               | 推荐方式                              |
| ---------------------- | ------------------------------------- |
| 验证单个配置类绑定     | `@SpringBootTest(properties = {...})` |
| 验证环境配置文件       | `@ActiveProfiles("test")`             |
| 验证动态端口或容器地址 | `@DynamicPropertySource`              |
| 验证配置校验失败       | 使用错误配置启动上下文并断言异常      |
| 验证默认值             | 不传入配置，直接断言默认字段值        |

### 启动日志检查

启动日志检查用于确认应用实际加载的 Profile、端口、日志级别、配置文件和关键业务配置。Spring Boot 启动时通常会输出 active profile 信息，并且当 Web 容器启动后，会输出端口信息。对于配置文件加载细节，可以将 `org.springframework.boot.context.config` 日志级别设置为 `trace`，用于查看配置文件加载过程。([Home](https://docs.spring.io/spring-boot/3.5/how-to/properties-and-configuration.html?utm_source=chatgpt.com))

开发环境可以临时开启配置加载追踪日志：

```bash
java -jar config-demo.jar \
  --spring.profiles.active=dev \
  --logging.level.org.springframework.boot.context.config=trace
```

常见启动日志检查项如下：

| 检查项            | 关注内容                                       |
| ----------------- | ---------------------------------------------- |
| Active profiles   | 是否加载了正确环境，例如 `dev`、`test`、`prod` |
| Web server port   | 实际启动端口是否符合预期                       |
| Application name  | `spring.application.name` 是否正确             |
| Logging level     | 指定包日志级别是否生效                         |
| Config data       | 外部配置文件是否被加载                         |
| Business config   | 关键业务配置是否加载成功                       |
| Validation result | 是否出现配置校验失败                           |

建议在应用启动后输出非敏感关键配置摘要，用于快速确认配置是否生效。

文件位置：`src/main/java/io/github/atengk/config/runner/ApplicationConfigSummaryRunner.java`

```java
package io.github.atengk.config.runner;

import cn.hutool.core.collection.CollUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * 应用配置摘要输出
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApplicationConfigSummaryRunner implements ApplicationRunner {

    private final Environment environment;

    /**
     * 应用启动完成后输出非敏感配置摘要
     *
     * @param args 应用启动参数
     */
    @Override
    public void run(ApplicationArguments args) {
        String applicationName = environment.getProperty("spring.application.name", "unknown");
        String serverPort = environment.getProperty("server.port", "8080");
        String contextPath = environment.getProperty("server.servlet.context-path", "/");
        String[] activeProfiles = environment.getActiveProfiles();

        if (CollUtil.isEmpty(Arrays.asList(activeProfiles))) {
            log.warn("当前未显式激活 Profile，将使用默认配置");
        }

        log.info("应用配置摘要：应用名称：{}，端口：{}，上下文路径：{}，激活环境：{}",
                applicationName,
                serverPort,
                contextPath,
                Arrays.toString(activeProfiles));
    }
}
```

日志中不要输出以下内容：

| 不应输出内容    | 示例                            |
| --------------- | ------------------------------- |
| 数据库密码      | `spring.datasource.password`    |
| Redis 密码      | `spring.data.redis.password`    |
| JWT 密钥        | `app.security.jwt-secret`       |
| 接口签名密钥    | `app.security.sign-secret`      |
| 第三方 Token    | `access-token`、`refresh-token` |
| 对象存储 Secret | `secret-key`                    |

启动日志检查不是配置测试的替代品。测试用于自动化验证，启动日志用于运行时确认，两者应配合使用。

### 常见配置问题排查

常见配置问题通常集中在配置未加载、配置被覆盖、Profile 未生效、YAML 格式错误、环境变量命名错误、配置类型不匹配和敏感配置缺失等方面。Spring Boot 官方文档也说明，Actuator 的 `env` 和 `configprops` 端点可用于诊断属性值来源和绑定情况；但生产环境使用时需要注意端点暴露和敏感值脱敏。([Home](https://docs.spring.io/spring-boot/3.5-SNAPSHOT/reference/features/external-config.html?utm_source=chatgpt.com))

常见问题排查表如下：

| 问题现象                        | 可能原因                                  | 处理方式                                       |
| ------------------------------- | ----------------------------------------- | ---------------------------------------------- |
| 修改 `application.yml` 后不生效 | 被命令行参数、JVM 参数或环境变量覆盖      | 检查启动脚本和部署平台环境变量                 |
| Profile 配置未加载              | `spring.profiles.active` 未指定或拼写错误 | 检查启动参数和配置文件名                       |
| 端口不是配置文件中的值          | 存在 `SERVER_PORT` 或 `--server.port`     | 检查环境变量和启动命令                         |
| YAML 启动解析失败               | 缩进错误、冒号后缺少空格、Tab 缩进        | 使用两个空格缩进并检查 YAML 格式               |
| 配置绑定为 `null`               | 配置 Key 和 Java 字段不匹配               | 检查前缀、字段名和宽松绑定规则                 |
| 布尔值读取异常                  | 配置值不是合法布尔值                      | 使用 `true` 或 `false`                         |
| 数字配置绑定失败                | 配置值包含非法字符                        | 改为合法数字或明确单位                         |
| 环境变量不生效                  | 变量名转换错误                            | 点号改下划线、中横线移除、转大写               |
| 敏感配置缺失                    | 生产环境未注入 Secret                     | 检查部署平台 Secret 或环境变量                 |
| 测试配置污染正式配置            | 测试 Profile 或测试属性配置不当           | 使用 `src/test/resources` 和 `@ActiveProfiles` |

配置优先级排查建议按照从高到低顺序检查：

```text
命令行参数
JVM System properties
环境变量
SPRING_APPLICATION_JSON
外部 application-{profile}.yml
外部 application.yml
Jar 包内部 application-{profile}.yml
Jar 包内部 application.yml
默认属性
```

如果需要查看配置文件加载过程，可以临时开启 trace 日志：

```bash
java -jar config-demo.jar \
  --spring.profiles.active=prod \
  --logging.level.org.springframework.boot.context.config=trace
```

如果项目引入了 Actuator，可以在非生产环境临时暴露 `env` 和 `configprops` 端点辅助排查。生产环境不建议直接开放这些端点。

文件位置：`src/main/resources/application-dev.yml`

```yaml
management:
  endpoints:
    web:
      exposure:
        # 仅开发环境暴露配置排查端点
        include: health,info,env,configprops
  endpoint:
    env:
      # 开发环境按需查看配置值，生产环境不要开启
      show-values: when_authorized
    configprops:
      # 开发环境按需查看配置绑定值，生产环境不要开启
      show-values: when_authorized
```

排查配置问题时建议遵循以下流程：

1. 先确认应用实际激活的 Profile。
2. 再确认配置文件路径和文件名是否正确。
3. 检查是否存在命令行参数、JVM 参数、环境变量覆盖。
4. 检查 YAML 缩进、配置 Key 拼写和数据类型。
5. 对 `@ConfigurationProperties` 配置类检查前缀、字段名、注册方式。
6. 对敏感配置检查是否通过部署平台正确注入。
7. 必要时开启 `org.springframework.boot.context.config=trace` 查看配置加载过程。
8. 非生产环境可以借助 Actuator `env`、`configprops` 端点辅助定位。