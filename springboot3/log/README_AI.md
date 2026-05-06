# Spring Boot 日志

Spring Boot 3 默认提供开箱即用的日志能力，项目启动后无需额外配置即可在控制台输出启动日志、请求日志、异常日志和框架运行日志。实际开发中，日志通常用于记录系统运行状态、定位异常问题、追踪业务流程、辅助排查线上故障，并为监控告警、链路分析和审计留存提供基础数据。

## 日志体系概述

本节用于说明 Spring Boot 3 中日志体系的基本组成，包括默认日志框架、日志门面与日志实现的关系，以及日志在项目开发和生产运维中的实际作用。

### Spring Boot 3 默认日志框架

Spring Boot 3 默认使用 `spring-boot-starter-logging` 作为日志启动器。只要项目引入了 `spring-boot-starter-web`、`spring-boot-starter` 等常用 Starter，日志依赖通常会被自动引入，不需要手动添加 Logback 依赖。

Spring Boot 3 默认日志组合如下：

| 组成     | 默认实现                     | 说明                                         |
| -------- | ---------------------------- | -------------------------------------------- |
| 日志门面 | SLF4J                        | Java 日志抽象层，业务代码通常面向 SLF4J 编程 |
| 日志实现 | Logback                      | Spring Boot 默认日志实现，负责真正输出日志   |
| 日志桥接 | Log4j-to-SLF4J、Jul-to-SLF4J | 将其他日志体系统一转发到 SLF4J               |
| 默认配置 | Spring Boot 内置配置         | 未提供自定义配置时，自动输出控制台日志       |

默认情况下，Spring Boot 3 启动后会在控制台输出类似以下内容：

```text
2026-05-06T10:20:30.123+09:00  INFO 12345 --- [main] i.g.a.Application : Starting Application using Java 17
2026-05-06T10:20:31.456+09:00  INFO 12345 --- [main] o.s.b.w.embedded.tomcat.TomcatWebServer : Tomcat started on port 8080
2026-05-06T10:20:31.789+09:00  INFO 12345 --- [main] i.g.a.Application : Started Application in 2.345 seconds
```

其中包含时间、日志级别、进程号、线程名、日志输出类和日志内容。生产环境中通常会进一步配置日志级别、日志文件路径、归档策略和日志格式。

### SLF4J 与 Logback 的关系

SLF4J 是日志门面，Logback 是日志实现。开发代码中一般只直接使用 SLF4J API，而不直接依赖 Logback API。

推荐写法如下：

```java
private static final Logger log = LoggerFactory.getLogger(UserService.class);
```

或者在项目中使用 Lombok 简化日志对象声明：

```java
@Slf4j
@Service
public class UserService {
}
```

二者关系可以理解为：

| 名称        | 角色     | 作用                                               |
| ----------- | -------- | -------------------------------------------------- |
| SLF4J       | 日志门面 | 提供统一日志 API，例如 `log.info()`、`log.error()` |
| Logback     | 日志实现 | 决定日志如何输出、输出到哪里、如何切分文件         |
| Spring Boot | 自动配置 | 默认整合 SLF4J 和 Logback，并提供配置入口          |

业务代码面向 SLF4J 编程的好处是后续可以更换日志实现。例如从 Logback 切换到 Log4j2 时，大多数业务代码无需修改，只需要调整依赖和配置文件。

### 日志在项目中的作用

日志不是简单的打印文本，而是系统可观测性的重要组成部分。合理的日志可以帮助开发人员快速判断系统是否正常运行，并在出现问题时定位原因。

日志在项目中的常见作用包括：

| 场景     | 作用                                               |
| -------- | -------------------------------------------------- |
| 系统启动 | 记录应用端口、环境、配置加载情况                   |
| 业务流程 | 记录关键业务节点，例如创建订单、支付回调、状态变更 |
| 异常排查 | 记录异常堆栈、请求参数、业务标识                   |
| 性能分析 | 记录接口耗时、SQL 耗时、外部接口调用耗时           |
| 安全审计 | 记录登录、权限变更、敏感操作                       |
| 运维监控 | 配合 ELK、Loki、Prometheus 等系统分析日志          |

示例：在业务代码中记录关键操作日志。

以下代码演示在用户服务中记录正常业务日志和异常日志，适合放在业务 Service 层。

文件位置：`src/main/java/io/github/atengk/user/service/UserService.java`

```java
package io.github.atengk.user.service;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 用户业务服务
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Service
public class UserService {

    /**
     * 创建用户
     *
     * @param username 用户名
     * @return 创建结果
     */
    public boolean createUser(String username) {
        if (StrUtil.isBlank(username)) {
            log.warn("创建用户失败，用户名为空");
            return false;
        }

        try {
            log.info("开始创建用户，username={}", username);

            // 这里模拟用户创建逻辑，例如写入数据库
            boolean result = true;

            log.info("创建用户完成，username={}, result={}", username, result);
            return result;
        } catch (Exception e) {
            log.error("创建用户异常，username={}", username, e);
            return false;
        }
    }
}
```

日志输出时建议保留关键业务标识，例如用户 ID、订单号、请求流水号、任务 ID 等。不要在日志中输出密码、身份证号、银行卡号、完整手机号、Token 等敏感信息。

## 日志基础配置

本节用于说明 Spring Boot 3 项目中日志相关的基础配置，包括 Maven 依赖、`application.yml` 配置、日志级别、控制台输出和文件输出。

### Maven 依赖说明

Spring Boot 3 默认通过 Starter 自动引入日志依赖。一般情况下，Web 项目只需要引入 `spring-boot-starter-web`，不需要单独引入 Logback。

文件位置：`pom.xml`

```xml
<dependencies>
    <!-- Spring Boot Web 启动器，默认包含 spring-boot-starter-logging -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Lombok：用于简化日志对象声明，例如 @Slf4j -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- Hutool：常用工具类库，可用于字符串、集合、日期等工具处理 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.8.36</version>
    </dependency>
</dependencies>
```

如果项目没有特殊要求，推荐继续使用 Spring Boot 默认的 Logback。只有在团队已有统一规范或明确需要 Log4j2 特性时，才建议替换默认日志实现。

如果需要切换为 Log4j2，需要排除默认日志依赖并引入 Log4j2 Starter。

```xml
<dependencies>
    <!-- 排除默认 Logback 日志实现 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
        <exclusions>
            <exclusion>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-logging</artifactId>
            </exclusion>
        </exclusions>
    </dependency>

    <!-- 使用 Log4j2 作为日志实现 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-log4j2</artifactId>
    </dependency>
</dependencies>
```

除非项目明确要求 Log4j2，否则不建议随意替换默认日志实现。Spring Boot 默认的 Logback 已经能满足大多数业务系统的控制台输出、文件输出、日志级别控制和日志归档需求。

### application.yml 日志配置

Spring Boot 支持直接在 `application.yml` 中配置常用日志参数。对于中小型项目，优先使用 `application.yml` 配置日志级别、日志文件名、日志目录和日志格式。

文件位置：`src/main/resources/application.yml`

```yaml
server:
  port: 8080

spring:
  application:
    # 应用名称，会出现在部分日志上下文或监控系统中
    name: springboot-log-demo

logging:
  level:
    # 根日志级别，影响所有未单独配置的包
    root: info

    # 当前项目包日志级别，开发环境可设置为 debug
    io.github.atengk: debug

    # Spring Web 相关日志，排查请求映射时可临时开启 debug
    org.springframework.web: info

    # MyBatis 或 SQL 执行相关日志，根据项目实际持久层框架调整
    org.mybatis: info

  file:
    # 日志文件输出路径，Spring Boot 会自动创建日志文件
    name: logs/springboot-log-demo.log

  pattern:
    # 控制台日志格式
    console: "%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%thread] %logger{36} - %msg%n"

    # 文件日志格式
    file: "%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%thread] %logger{50} - %msg%n"
```

该配置适合本地开发和基础测试环境。生产环境中通常还会配合 `logback-spring.xml` 配置日志滚动、按日期归档、按大小切分、保留天数等策略。

### 日志级别配置

日志级别用于控制哪些日志可以输出。日志级别从低到高通常为：

```text
trace < debug < info < warn < error
```

配置为某个级别后，只会输出该级别及更高级别的日志。例如配置为 `info` 时，会输出 `info`、`warn`、`error`，不会输出 `debug` 和 `trace`。

常用日志级别说明如下：

| 级别  | 使用场景                                           |
| ----- | -------------------------------------------------- |
| trace | 极细粒度调试信息，生产环境基本不使用               |
| debug | 开发调试信息，例如参数、分支、临时排查数据         |
| info  | 正常业务流程日志，例如启动成功、任务完成、状态变化 |
| warn  | 可恢复或需要关注的问题，例如参数异常、重试、降级   |
| error | 系统异常或业务失败，例如数据库异常、接口调用失败   |

推荐配置策略如下：

| 环境     | 推荐级别      | 说明                 |
| -------- | ------------- | -------------------- |
| 本地开发 | debug         | 便于查看详细业务流程 |
| 测试环境 | debug 或 info | 根据排查需要调整     |
| 预发环境 | info          | 尽量接近生产环境     |
| 生产环境 | info 或 warn  | 避免日志量过大       |

示例配置：

```yaml
logging:
  level:
    # 全局默认输出 info 及以上日志
    root: info

    # 项目业务包输出 debug 及以上日志
    io.github.atengk: debug

    # Spring 框架日志保持 info，避免控制台过多框架细节
    org.springframework: info

    # 数据库连接池日志保持 warn，只关注潜在问题
    com.zaxxer.hikari: warn
```

在开发过程中，如果只想临时排查某个类，可以把日志级别配置到具体类名。

```yaml
logging:
  level:
    # 只打开 UserService 的 debug 日志
    io.github.atengk.user.service.UserService: debug
```

这种方式比直接把整个项目包设置为 `debug` 更精确，适合定位单个模块问题。

### 控制台日志输出

控制台日志主要用于本地开发、容器标准输出和快速排查启动问题。Spring Boot 默认会输出控制台日志，不需要额外配置。

如果需要自定义控制台日志格式，可以配置 `logging.pattern.console`。

文件位置：`src/main/resources/application.yml`

```yaml
logging:
  pattern:
    # 控制台日志格式：时间 级别 线程 类名 - 消息
    console: "%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%thread] %logger{36} - %msg%n"
```

常见格式参数说明如下：

| 参数                          | 说明                             |
| ----------------------------- | -------------------------------- |
| `%d{yyyy-MM-dd HH:mm:ss.SSS}` | 日志时间                         |
| `%-5level`                    | 日志级别，左对齐，占 5 位        |
| `%thread`                     | 当前线程名                       |
| `%logger{36}`                 | 日志输出类名，最多显示 36 个字符 |
| `%msg`                        | 日志内容                         |
| `%n`                          | 换行                             |

控制台输出示例：

```text
2026-05-06 10:30:15.123 INFO  [main] io.github.atengk.Application - 项目启动成功
2026-05-06 10:30:20.456 DEBUG [http-nio-8080-exec-1] i.g.a.user.service.UserService - 开始创建用户，username=admin
2026-05-06 10:30:20.789 INFO  [http-nio-8080-exec-1] i.g.a.user.service.UserService - 创建用户完成，username=admin, result=true
```

在 Docker、Kubernetes 等容器环境中，控制台日志非常重要。容器平台通常会采集标准输出，因此生产环境即使配置了文件日志，也建议保留控制台日志。

### 文件日志输出

文件日志用于持久化保存系统运行记录，便于线上问题回溯。Spring Boot 可以通过 `logging.file.name` 或 `logging.file.path` 快速启用文件日志。

使用 `logging.file.name` 可以指定具体日志文件名。

文件位置：`src/main/resources/application.yml`

```yaml
logging:
  file:
    # 指定日志文件完整路径
    name: logs/springboot-log-demo.log
```

启动后会生成如下文件：

```text
logs/springboot-log-demo.log
```

也可以使用 `logging.file.path` 指定日志目录，由 Spring Boot 使用默认文件名。

```yaml
logging:
  file:
    # 指定日志目录，默认文件名通常为 spring.log
    path: logs
```

两种配置方式建议二选一，不要同时配置。一般项目中更推荐使用 `logging.file.name`，因为日志文件名更清晰，便于部署和采集。

基础文件日志配置示例：

```yaml
logging:
  level:
    root: info
    io.github.atengk: info

  file:
    # 生产环境建议使用绝对路径，例如 /data/logs/app/app.log
    name: /data/logs/springboot-log-demo/app.log

  pattern:
    # 文件日志建议保留更完整的 logger 名称，便于定位代码位置
    file: "%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%thread] %logger{50} - %msg%n"
```

生产环境注意事项：

| 配置项   | 建议                                           |
| -------- | ---------------------------------------------- |
| 日志路径 | 使用固定目录，例如 `/data/logs/应用名/app.log` |
| 文件权限 | 确保应用进程有写入权限                         |
| 日志级别 | 默认使用 `info`，问题排查时临时调整            |
| 敏感信息 | 不记录密码、Token、身份证、银行卡等敏感数据    |
| 日志采集 | 配合 Filebeat、Fluent Bit、Promtail 等工具采集 |

如果需要更完整的文件滚动策略，建议新增 `logback-spring.xml`，在其中配置按日期和文件大小滚动、最大保留天数、总日志大小上限等规则。`application.yml` 适合基础配置，`logback-spring.xml` 更适合生产级日志治理。

## Logback 配置

本节用于说明 Spring Boot 3 中如何通过 `logback-spring.xml` 配置 Logback。相比只使用 `application.yml`，`logback-spring.xml` 更适合生产环境，可以精细控制控制台输出、文件输出、日志滚动、日志保留时间、日志文件大小和不同环境的日志策略。

### logback-spring.xml 文件位置

Spring Boot 3 推荐将 Logback 配置文件放在 `src/main/resources` 目录下，并命名为 `logback-spring.xml`。

文件位置：

```text
src/main/resources/logback-spring.xml
```

推荐使用 `logback-spring.xml`，不要优先使用 `logback.xml`。原因是 `logback-spring.xml` 可以使用 Spring Boot 提供的扩展能力，例如 `<springProfile>` 和 `<springProperty>`，可以根据不同环境加载不同日志配置。

常见文件结构如下：

```text
springboot-log-demo
├── src
│   └── main
│       ├── java
│       │   └── io
│       │       └── github
│       │           └── atengk
│       │               └── Application.java
│       └── resources
│           ├── application.yml
│           ├── application-dev.yml
│           ├── application-prod.yml
│           └── logback-spring.xml
└── pom.xml
```

`logback-spring.xml` 通常用于定义日志格式、Appender、滚动策略和不同环境的日志级别。`application.yml` 则用于配置应用名称、激活环境和少量通用日志参数。

示例 `application.yml`：

```yaml
spring:
  application:
    # 应用名称，可在 logback-spring.xml 中通过 springProperty 读取
    name: springboot-log-demo

  profiles:
    # 当前激活环境，实际项目中可通过启动参数覆盖
    active: dev

logging:
  file:
    # 日志输出目录，logback-spring.xml 可以读取该配置
    path: /data/logs/springboot-log-demo
```

启动时也可以通过命令指定环境：

```bash
java -jar springboot-log-demo.jar --spring.profiles.active=prod
```

该命令通过 `--spring.profiles.active=prod` 指定生产环境配置，`logback-spring.xml` 中的 `<springProfile name="prod">` 会生效。

### 控制台 Appender 配置

控制台 Appender 用于将日志输出到控制台。它适合本地开发、容器标准输出、Kubernetes 日志采集和启动问题排查。

文件位置：`src/main/resources/logback-spring.xml`

下面配置定义了控制台日志格式，并将日志输出到标准输出。

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration scan="false">

    <!-- 读取 Spring Boot 应用名称 -->
    <springProperty scope="context" name="APP_NAME" source="spring.application.name" defaultValue="springboot-log-demo"/>

    <!-- 控制台日志格式 -->
    <property name="CONSOLE_LOG_PATTERN"
              value="%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%thread] %logger{36} - %msg%n"/>

    <!-- 控制台输出 Appender -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <!-- 输出到控制台标准输出 -->
        <target>System.out</target>

        <encoder>
            <!-- 控制台日志编码 -->
            <charset>UTF-8</charset>

            <!-- 控制台日志格式 -->
            <pattern>${CONSOLE_LOG_PATTERN}</pattern>
        </encoder>
    </appender>

    <!-- 默认日志级别 -->
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
    </root>

</configuration>
```

控制台日志输出示例：

```text
2026-05-06 10:30:15.123 INFO  [main] io.github.atengk.Application - 项目启动成功
2026-05-06 10:31:20.456 WARN  [http-nio-8080-exec-1] i.g.a.user.service.UserService - 查询用户为空，userId=10001
```

控制台日志建议保留以下信息：日志时间、日志级别、线程名、Logger 名称和日志内容。对于生产环境，控制台日志可以配合容器日志采集工具使用，例如 Fluent Bit、Filebeat、Promtail 等。

### 文件 Appender 配置

文件 Appender 用于将日志写入本地文件，适合生产环境中进行问题追溯和日志采集。相比控制台日志，文件日志通常需要更完整的类名、更稳定的路径和明确的编码配置。

文件位置：`src/main/resources/logback-spring.xml`

下面配置定义了普通文件日志输出，不包含滚动策略，适合理解基础写法。

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration scan="false">

    <!-- 读取应用名称 -->
    <springProperty scope="context" name="APP_NAME" source="spring.application.name" defaultValue="springboot-log-demo"/>

    <!-- 读取日志目录，未配置时使用 logs 目录 -->
    <springProperty scope="context" name="LOG_PATH" source="logging.file.path" defaultValue="logs"/>

    <!-- 文件日志格式 -->
    <property name="FILE_LOG_PATTERN"
              value="%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%thread] %logger{50} - %msg%n"/>

    <!-- 文件输出 Appender -->
    <appender name="FILE" class="ch.qos.logback.core.FileAppender">
        <!-- 日志文件路径 -->
        <file>${LOG_PATH}/${APP_NAME}.log</file>

        <!-- 追加写入，避免应用重启后覆盖历史日志 -->
        <append>true</append>

        <encoder>
            <!-- 文件日志编码 -->
            <charset>UTF-8</charset>

            <!-- 文件日志格式 -->
            <pattern>${FILE_LOG_PATTERN}</pattern>
        </encoder>
    </appender>

    <!-- 默认日志级别 -->
    <root level="INFO">
        <appender-ref ref="FILE"/>
    </root>

</configuration>
```

生成的日志文件示例：

```text
/data/logs/springboot-log-demo/springboot-log-demo.log
```

生产环境不建议长期只使用 `FileAppender`，因为日志文件会持续增长。更推荐使用 `RollingFileAppender` 配置滚动策略，避免单个日志文件过大。

### 滚动日志配置

滚动日志用于控制日志文件按照时间、大小进行切分，并设置历史日志保留时间。生产环境建议使用 `RollingFileAppender`，避免日志文件无限增长导致磁盘空间耗尽。

常见滚动策略包括：

| 策略             | 说明                                     |
| ---------------- | ---------------------------------------- |
| 按日期滚动       | 每天生成一个日志文件                     |
| 按大小滚动       | 单个日志文件超过指定大小后切分           |
| 按日期和大小滚动 | 每天一个目录或文件前缀，超过大小继续编号 |
| 保留天数         | 自动删除超过指定天数的历史日志           |
| 总大小限制       | 控制历史日志文件总占用空间               |

文件位置：`src/main/resources/logback-spring.xml`

下面配置适合生产环境使用，同时输出控制台日志和滚动文件日志。

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration scan="false">

    <!-- 读取 Spring Boot 应用名称 -->
    <springProperty scope="context" name="APP_NAME" source="spring.application.name" defaultValue="springboot-log-demo"/>

    <!-- 读取日志目录，生产环境建议配置为 /data/logs/应用名 -->
    <springProperty scope="context" name="LOG_PATH" source="logging.file.path" defaultValue="logs"/>

    <!-- 控制台日志格式 -->
    <property name="CONSOLE_LOG_PATTERN"
              value="%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%thread] %logger{36} - %msg%n"/>

    <!-- 文件日志格式 -->
    <property name="FILE_LOG_PATTERN"
              value="%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%thread] %logger{50} - %msg%n"/>

    <!-- 控制台输出 -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <target>System.out</target>
        <encoder>
            <charset>UTF-8</charset>
            <pattern>${CONSOLE_LOG_PATTERN}</pattern>
        </encoder>
    </appender>

    <!-- 滚动文件输出 -->
    <appender name="ROLLING_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <!-- 当前正在写入的日志文件 -->
        <file>${LOG_PATH}/${APP_NAME}.log</file>

        <encoder>
            <charset>UTF-8</charset>
            <pattern>${FILE_LOG_PATTERN}</pattern>
        </encoder>

        <!-- 按日期和大小滚动 -->
        <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
            <!-- 历史日志文件命名规则，%d 表示日期，%i 表示同一天内的文件序号 -->
            <fileNamePattern>${LOG_PATH}/archive/${APP_NAME}.%d{yyyy-MM-dd}.%i.log.gz</fileNamePattern>

            <!-- 单个日志文件最大大小，超过后按序号切分 -->
            <maxFileSize>100MB</maxFileSize>

            <!-- 历史日志最多保留 30 天 -->
            <maxHistory>30</maxHistory>

            <!-- 历史日志总大小上限，超过后自动清理旧文件 -->
            <totalSizeCap>10GB</totalSizeCap>

            <!-- 应用启动时清理过期历史日志 -->
            <cleanHistoryOnStart>true</cleanHistoryOnStart>
        </rollingPolicy>
    </appender>

    <!-- 项目业务包日志级别 -->
    <logger name="io.github.atengk" level="INFO"/>

    <!-- 默认日志级别 -->
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="ROLLING_FILE"/>
    </root>

</configuration>
```

滚动后日志文件示例：

```text
/data/logs/springboot-log-demo/springboot-log-demo.log
/data/logs/springboot-log-demo/archive/springboot-log-demo.2026-05-06.0.log.gz
/data/logs/springboot-log-demo/archive/springboot-log-demo.2026-05-06.1.log.gz
/data/logs/springboot-log-demo/archive/springboot-log-demo.2026-05-07.0.log.gz
```

关键配置说明：

| 配置项                | 说明                       |
| --------------------- | -------------------------- |
| `file`                | 当前正在写入的日志文件     |
| `fileNamePattern`     | 历史日志文件命名规则       |
| `%d{yyyy-MM-dd}`      | 按日期切分日志             |
| `%i`                  | 同一天内按大小切分后的序号 |
| `maxFileSize`         | 单个日志文件最大大小       |
| `maxHistory`          | 历史日志保留天数           |
| `totalSizeCap`        | 历史日志总大小限制         |
| `cleanHistoryOnStart` | 应用启动时是否清理过期日志 |

生产环境建议至少配置 `maxHistory` 和 `totalSizeCap`，避免日志长期累积导致磁盘占满。

### 不同环境日志配置

不同环境对日志的要求不同。本地开发环境需要更多调试信息，生产环境更关注稳定性、性能和磁盘占用。因此可以使用 `<springProfile>` 为不同环境配置不同日志策略。

文件位置：`src/main/resources/logback-spring.xml`

下面配置根据 `dev`、`test`、`prod` 环境分别设置日志级别和输出方式。

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration scan="false">

    <springProperty scope="context" name="APP_NAME" source="spring.application.name" defaultValue="springboot-log-demo"/>
    <springProperty scope="context" name="LOG_PATH" source="logging.file.path" defaultValue="logs"/>

    <property name="CONSOLE_LOG_PATTERN"
              value="%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%thread] %logger{36} - %msg%n"/>

    <property name="FILE_LOG_PATTERN"
              value="%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%thread] %logger{50} - %msg%n"/>

    <!-- 控制台日志 -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <target>System.out</target>
        <encoder>
            <charset>UTF-8</charset>
            <pattern>${CONSOLE_LOG_PATTERN}</pattern>
        </encoder>
    </appender>

    <!-- 滚动文件日志 -->
    <appender name="ROLLING_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${LOG_PATH}/${APP_NAME}.log</file>

        <encoder>
            <charset>UTF-8</charset>
            <pattern>${FILE_LOG_PATTERN}</pattern>
        </encoder>

        <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
            <fileNamePattern>${LOG_PATH}/archive/${APP_NAME}.%d{yyyy-MM-dd}.%i.log.gz</fileNamePattern>
            <maxFileSize>100MB</maxFileSize>
            <maxHistory>30</maxHistory>
            <totalSizeCap>10GB</totalSizeCap>
            <cleanHistoryOnStart>true</cleanHistoryOnStart>
        </rollingPolicy>
    </appender>

    <!-- 开发环境：输出更详细的业务日志，只输出到控制台 -->
    <springProfile name="dev">
        <logger name="io.github.atengk" level="DEBUG"/>
        <logger name="org.springframework.web" level="INFO"/>

        <root level="INFO">
            <appender-ref ref="CONSOLE"/>
        </root>
    </springProfile>

    <!-- 测试环境：输出控制台和文件日志，业务包保持 DEBUG 便于排查 -->
    <springProfile name="test">
        <logger name="io.github.atengk" level="DEBUG"/>
        <logger name="org.springframework.web" level="INFO"/>

        <root level="INFO">
            <appender-ref ref="CONSOLE"/>
            <appender-ref ref="ROLLING_FILE"/>
        </root>
    </springProfile>

    <!-- 生产环境：控制日志量，保留控制台和滚动文件日志 -->
    <springProfile name="prod">
        <logger name="io.github.atengk" level="INFO"/>
        <logger name="org.springframework" level="WARN"/>
        <logger name="com.zaxxer.hikari" level="WARN"/>

        <root level="INFO">
            <appender-ref ref="CONSOLE"/>
            <appender-ref ref="ROLLING_FILE"/>
        </root>
    </springProfile>

</configuration>
```

推荐环境策略如下：

| 环境 | 日志级别          | 输出位置      | 说明                       |
| ---- | ----------------- | ------------- | -------------------------- |
| dev  | `DEBUG`           | 控制台        | 便于本地调试               |
| test | `DEBUG` 或 `INFO` | 控制台 + 文件 | 便于测试问题复现           |
| prod | `INFO` 或 `WARN`  | 控制台 + 文件 | 控制日志量，便于采集和追溯 |

启动时指定环境：

```bash
# 开发环境
java -jar springboot-log-demo.jar --spring.profiles.active=dev

# 测试环境
java -jar springboot-log-demo.jar --spring.profiles.active=test

# 生产环境
java -jar springboot-log-demo.jar --spring.profiles.active=prod
```

参数 `--spring.profiles.active` 用于指定当前运行环境。Logback 会根据当前环境匹配对应的 `<springProfile>` 配置。

## 日志使用规范

本节用于说明业务代码中如何规范使用日志。日志规范的目标是让日志内容可读、可检索、可定位、可追溯，同时避免无意义日志、敏感信息泄露和异常堆栈丢失。

### Logger 定义方式

在 Spring Boot 项目中，推荐使用 SLF4J 作为日志门面。实际项目中常见两种定义方式：手动定义 Logger，或者使用 Lombok 的 `@Slf4j` 注解。

推荐方式一：使用 Lombok `@Slf4j`。

文件位置：`src/main/java/io/github/atengk/user/service/UserService.java`

```java
package io.github.atengk.user.service;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 用户业务服务
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Service
public class UserService {

    /**
     * 创建用户
     *
     * @param username 用户名
     * @return 创建结果
     */
    public boolean createUser(String username) {
        if (StrUtil.isBlank(username)) {
            log.warn("创建用户失败，用户名为空");
            return false;
        }

        log.info("创建用户成功，username={}", username);
        return true;
    }
}
```

推荐方式二：手动定义 Logger。

文件位置：`src/main/java/io/github/atengk/order/service/OrderService.java`

```java
package io.github.atengk.order.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 订单业务服务
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    /**
     * 取消订单
     *
     * @param orderNo 订单号
     */
    public void cancelOrder(String orderNo) {
        log.info("取消订单，orderNo={}", orderNo);
    }
}
```

项目中建议统一一种写法。若项目已经使用 Lombok，推荐使用 `@Slf4j`，代码更简洁；若团队不希望引入 Lombok，则使用 `LoggerFactory.getLogger()`。

不推荐写法：

```java
// 不推荐：使用 System.out 输出日志
System.out.println("创建用户成功");

// 不推荐：直接使用具体日志实现，降低可替换性
ch.qos.logback.classic.Logger logger;
```

业务代码应避免使用 `System.out.println()`、`e.printStackTrace()` 输出日志。这类写法无法统一日志格式、日志级别、文件输出和日志采集。

### 日志级别使用规范

日志级别要根据业务含义选择，不能所有日志都使用 `info`，也不能随意使用 `error`。级别使用不当会影响问题判断和告警准确性。

推荐规范如下：

| 级别    | 使用规范                                       | 示例                         |
| ------- | ---------------------------------------------- | ---------------------------- |
| `trace` | 极细粒度流程日志，一般不在业务代码中使用       | 框架内部细节                 |
| `debug` | 开发和排查问题时使用，记录参数、分支、临时状态 | 查询条件、状态判断           |
| `info`  | 关键业务流程正常完成时使用                     | 创建订单、支付成功           |
| `warn`  | 出现异常倾向但系统可继续运行时使用             | 参数为空、重复请求、降级处理 |
| `error` | 系统异常、业务失败、外部依赖不可用时使用       | 数据库异常、接口调用失败     |

示例代码：

文件位置：`src/main/java/io/github/atengk/order/service/OrderProcessService.java`

```java
package io.github.atengk.order.service;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 订单处理服务
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Service
public class OrderProcessService {

    /**
     * 处理订单
     *
     * @param orderNo 订单号
     * @return 处理结果
     */
    public boolean processOrder(String orderNo) {
        log.debug("开始校验订单参数，orderNo={}", orderNo);

        if (StrUtil.isBlank(orderNo)) {
            log.warn("处理订单失败，订单号为空");
            return false;
        }

        try {
            log.info("开始处理订单，orderNo={}", orderNo);

            // 模拟订单处理逻辑
            boolean success = true;

            log.info("订单处理完成，orderNo={}, success={}", orderNo, success);
            return success;
        } catch (Exception e) {
            log.error("订单处理异常，orderNo={}", orderNo, e);
            return false;
        }
    }
}
```

使用建议：

| 场景               | 推荐级别 |
| ------------------ | -------- |
| 方法入参调试       | `debug`  |
| 核心业务成功       | `info`   |
| 参数不合法但已处理 | `warn`   |
| 重复提交、幂等拦截 | `warn`   |
| 外部接口调用失败   | `error`  |
| 数据库写入失败     | `error`  |
| 定时任务开始和结束 | `info`   |
| 定时任务执行异常   | `error`  |

不要把可预期的业务校验失败全部记录为 `error`。例如用户名为空、订单不存在、重复提交等，如果系统已经正常处理，通常使用 `warn` 更合适。

### 占位符日志写法

SLF4J 推荐使用 `{}` 占位符输出日志参数，而不是使用字符串拼接。占位符写法可以避免不必要的字符串拼接开销，并且日志结构更清晰。

推荐写法：

```java
log.info("创建用户成功，userId={}, username={}", userId, username);
```

不推荐写法：

```java
log.info("创建用户成功，userId=" + userId + ", username=" + username);
```

在日志级别未启用时，字符串拼接仍然可能提前执行。占位符写法则由日志框架控制参数处理，更适合生产环境。

完整示例：

文件位置：`src/main/java/io/github/atengk/user/service/UserQueryService.java`

```java
package io.github.atengk.user.service;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 用户查询服务
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Service
public class UserQueryService {

    /**
     * 查询用户昵称
     *
     * @param userId 用户ID
     * @return 用户昵称
     */
    public String getNickname(Long userId) {
        log.debug("开始查询用户昵称，userId={}", userId);

        if (userId == null) {
            log.warn("查询用户昵称失败，userId为空");
            return null;
        }

        String nickname = "Ateng";

        if (StrUtil.isBlank(nickname)) {
            log.warn("查询用户昵称为空，userId={}", userId);
            return null;
        }

        log.info("查询用户昵称成功，userId={}, nickname={}", userId, nickname);
        return nickname;
    }
}
```

多个参数建议按业务主键、核心参数、结果状态的顺序输出：

```java
log.info("支付回调处理完成，orderNo={}, payStatus={}, transactionId={}", orderNo, payStatus, transactionId);
```

对于耗时日志，建议记录接口名、业务标识和耗时：

```java
log.info("调用库存服务完成，skuId={}, cost={}ms", skuId, cost);
```

不建议输出大对象、完整集合或超长 JSON。必要时可以只输出数量、主键或摘要信息：

```java
log.info("批量导入用户完成，total={}, success={}, fail={}", totalCount, successCount, failCount);
```

### 异常日志记录方式

异常日志必须保留异常堆栈，否则线上问题很难定位。记录异常时，应将异常对象作为日志方法的最后一个参数传入。

推荐写法：

```java
log.error("创建订单异常，orderNo={}", orderNo, e);
```

不推荐写法：

```java
// 不推荐：只记录异常消息，丢失堆栈
log.error("创建订单异常：" + e.getMessage());

// 不推荐：无法进入统一日志体系
e.printStackTrace();
```

完整示例：

文件位置：`src/main/java/io/github/atengk/payment/service/PaymentService.java`

```java
package io.github.atengk.payment.service;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 支付业务服务
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Service
public class PaymentService {

    /**
     * 处理支付回调
     *
     * @param orderNo       订单号
     * @param transactionId 第三方交易号
     * @return 处理结果
     */
    public boolean handlePayCallback(String orderNo, String transactionId) {
        if (StrUtil.hasBlank(orderNo, transactionId)) {
            log.warn("支付回调参数不完整，orderNo={}, transactionId={}", orderNo, transactionId);
            return false;
        }

        try {
            log.info("开始处理支付回调，orderNo={}, transactionId={}", orderNo, transactionId);

            // 模拟支付回调处理逻辑，例如校验金额、更新订单状态、记录流水
            boolean success = true;

            log.info("支付回调处理完成，orderNo={}, transactionId={}, success={}", orderNo, transactionId, success);
            return success;
        } catch (Exception e) {
            log.error("支付回调处理异常，orderNo={}, transactionId={}", orderNo, transactionId, e);
            return false;
        }
    }
}
```

异常日志记录建议：

| 场景                 | 推荐写法                                    |
| -------------------- | ------------------------------------------- |
| 捕获异常并处理       | 使用 `log.error("xxx，id={}", id, e)`       |
| 需要继续向上抛出异常 | 记录关键上下文后抛出业务异常                |
| 全局异常处理器       | 统一记录请求路径、请求方法、异常堆栈        |
| 外部接口异常         | 记录接口名称、请求标识、响应状态、耗时      |
| 数据库异常           | 记录业务主键，不直接输出完整 SQL 参数敏感值 |

全局异常处理器示例：

文件位置：`src/main/java/io/github/atengk/common/handler/GlobalExceptionHandler.java`

```java
package io.github.atengk.common.handler;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * 全局异常处理器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理系统异常
     *
     * @param request 请求对象
     * @param e       异常对象
     * @return 响应结果
     */
    @ExceptionHandler(Exception.class)
    public Map<String, Object> handleException(HttpServletRequest request, Exception e) {
        log.error("系统异常，method={}, uri={}", request.getMethod(), request.getRequestURI(), e);

        return Map.of(
                "code", 500,
                "message", "系统繁忙，请稍后重试"
        );
    }
}
```

全局异常处理器中不要把完整异常信息直接返回给前端。前端响应应保持简洁，详细异常信息只记录到服务端日志。

### 中文日志内容规范

项目日志建议统一使用中文描述，变量名使用英文 key。这样既便于国内团队排查问题，也便于通过日志平台按字段检索。

推荐格式：

```text
动作 + 业务对象 + 结果，key1={}, key2={}
```

示例：

```java
log.info("创建订单成功，orderNo={}, userId={}", orderNo, userId);
log.warn("取消订单失败，订单状态不允许取消，orderNo={}, status={}", orderNo, status);
log.error("同步库存异常，skuId={}, warehouseId={}", skuId, warehouseId, e);
```

中文日志编写建议：

| 规范                 | 说明                       | 示例                                 |
| -------------------- | -------------------------- | ------------------------------------ |
| 使用中文描述业务动作 | 便于直接理解日志含义       | `创建订单成功`                       |
| 变量 key 使用英文    | 便于日志平台检索           | `orderNo={}`                         |
| 保留核心业务标识     | 便于串联上下文             | `userId`、`orderNo`、`requestId`     |
| 避免口语化           | 保持专业简洁               | 使用“查询用户失败”，避免“查用户挂了” |
| 避免敏感信息         | 不输出密码、Token、证件号  | 使用脱敏值或摘要                     |
| 避免无意义日志       | 不输出“进来了”“走到这里了” | 使用明确业务动作                     |

推荐日志内容：

```java
log.info("用户登录成功，userId={}, username={}", userId, username);
log.warn("用户登录失败，账号不存在，username={}", username);
log.warn("用户登录失败，密码错误，userId={}", userId);
log.info("订单支付成功，orderNo={}, payAmount={}, transactionId={}", orderNo, payAmount, transactionId);
log.error("订单支付回调异常，orderNo={}, transactionId={}", orderNo, transactionId, e);
```

不推荐日志内容：

```java
log.info("进来了");
log.info("111111");
log.info("执行完了");
log.error("报错了");
log.info("用户密码是：{}", password);
log.info("请求Token：{}", token);
```

对于手机号、身份证号、银行卡号等敏感信息，应先脱敏再输出。可以使用 Hutool 工具类处理。

文件位置：`src/main/java/io/github/atengk/common/util/LogMaskUtil.java`

```java
package io.github.atengk.common.util;

import cn.hutool.core.util.DesensitizedUtil;
import cn.hutool.core.util.StrUtil;

/**
 * 日志脱敏工具类
 *
 * @author Ateng
 * @since 2026-05-06
 */
public class LogMaskUtil {

    /**
     * 手机号脱敏
     *
     * @param mobile 手机号
     * @return 脱敏后的手机号
     */
    public static String mobile(String mobile) {
        if (StrUtil.isBlank(mobile)) {
            return "";
        }
        return DesensitizedUtil.mobilePhone(mobile);
    }

    /**
     * 身份证号脱敏
     *
     * @param idCard 身份证号
     * @return 脱敏后的身份证号
     */
    public static String idCard(String idCard) {
        if (StrUtil.isBlank(idCard)) {
            return "";
        }
        return DesensitizedUtil.idCardNum(idCard, 4, 4);
    }
}
```

使用示例：

文件位置：`src/main/java/io/github/atengk/user/service/UserLoginService.java`

```java
package io.github.atengk.user.service;

import io.github.atengk.common.util.LogMaskUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 用户登录服务
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Service
public class UserLoginService {

    /**
     * 记录登录日志
     *
     * @param userId 用户ID
     * @param mobile 手机号
     */
    public void recordLoginLog(Long userId, String mobile) {
        log.info("用户登录成功，userId={}, mobile={}", userId, LogMaskUtil.mobile(mobile));
    }
}
```

最终建议形成统一规范：正常业务流程使用 `info`，可恢复问题使用 `warn`，系统异常使用 `error`；日志内容使用中文描述，参数使用英文 key；异常日志必须保留堆栈；敏感信息必须脱敏；禁止使用 `System.out.println()` 和 `e.printStackTrace()`。

## 业务日志开发

业务日志用于记录系统中与业务行为相关的关键过程，例如接口调用、参数校验、业务状态变更、数据库操作结果、外部服务调用和异常处理。开发业务日志时，应优先记录可定位问题的业务标识，例如 `userId`、`orderNo`、`requestId`、`traceId`，避免记录无意义文本和敏感数据。

### Controller 层日志

Controller 层日志主要用于记录接口入口、请求参数摘要、参数校验结果和接口调用结果。Controller 不建议记录过多业务细节，核心业务过程应放在 Service 层记录。

Controller 层适合记录以下内容：

| 内容         | 建议                       |
| ------------ | -------------------------- |
| 接口入口     | 记录请求动作和核心参数     |
| 参数校验失败 | 使用 `warn` 级别           |
| 调用成功     | 简要记录业务主键和结果     |
| 调用异常     | 交给全局异常处理器统一记录 |

示例文件结构：

```text
src/main/java/io/github/atengk/order/controller/OrderController.java
src/main/java/io/github/atengk/order/dto/CreateOrderRequest.java
src/main/java/io/github/atengk/order/vo/OrderCreateVO.java
src/main/java/io/github/atengk/order/service/OrderService.java
```

文件位置：`src/main/java/io/github/atengk/order/dto/CreateOrderRequest.java`

```java
package io.github.atengk.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 创建订单请求参数
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
public class CreateOrderRequest {

    /**
     * 用户ID
     */
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /**
     * 商品编码
     */
    @NotBlank(message = "商品编码不能为空")
    private String productCode;

    /**
     * 购买数量
     */
    @NotNull(message = "购买数量不能为空")
    private Integer quantity;

    /**
     * 订单金额
     */
    @NotNull(message = "订单金额不能为空")
    private BigDecimal amount;
}
```

文件位置：`src/main/java/io/github/atengk/order/vo/OrderCreateVO.java`

```java
package io.github.atengk.order.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 订单创建响应结果
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreateVO {

    /**
     * 订单号
     */
    private String orderNo;

    /**
     * 创建状态
     */
    private String status;
}
```

Controller 中只记录接口入口和接口返回摘要，具体创建逻辑交给 Service 层处理。

文件位置：`src/main/java/io/github/atengk/order/controller/OrderController.java`

```java
package io.github.atengk.order.controller;

import io.github.atengk.order.dto.CreateOrderRequest;
import io.github.atengk.order.service.OrderService;
import io.github.atengk.order.vo.OrderCreateVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 订单接口
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    /**
     * 创建订单
     *
     * @param request 创建订单请求参数
     * @return 订单创建结果
     */
    @PostMapping
    public OrderCreateVO createOrder(@Valid @RequestBody CreateOrderRequest request) {
        log.info("接收创建订单请求，userId={}, productCode={}, quantity={}",
                request.getUserId(), request.getProductCode(), request.getQuantity());

        OrderCreateVO result = orderService.createOrder(request);

        log.info("创建订单接口处理完成，userId={}, orderNo={}, status={}",
                request.getUserId(), result.getOrderNo(), result.getStatus());

        return result;
    }

    /**
     * 查询订单
     *
     * @param orderNo 订单号
     * @return 订单信息
     */
    @GetMapping("/{orderNo}")
    public OrderCreateVO getOrder(@PathVariable String orderNo) {
        log.info("接收查询订单请求，orderNo={}", orderNo);
        return orderService.getOrder(orderNo);
    }
}
```

Controller 层日志不建议输出完整请求体，尤其是登录、支付、用户实名、文件上传等接口。完整请求日志可由统一请求日志 Filter 处理，并配合脱敏规则。

### Service 层日志

Service 层是业务日志的重点位置，适合记录业务判断、状态变化、外部系统调用、事务处理结果和异常上下文。Service 层日志应围绕业务流程展开，而不是简单记录“进入方法”“方法结束”。

Service 层适合记录以下内容：

| 内容                         | 推荐级别 |
| ---------------------------- | -------- |
| 关键业务开始                 | `info`   |
| 业务状态变更                 | `info`   |
| 参数异常、重复提交、幂等拦截 | `warn`   |
| 外部接口调用失败             | `error`  |
| 数据库写入异常               | `error`  |
| 调试分支和中间变量           | `debug`  |

文件位置：`src/main/java/io/github/atengk/order/service/OrderService.java`

```java
package io.github.atengk.order.service;

import io.github.atengk.order.dto.CreateOrderRequest;
import io.github.atengk.order.vo.OrderCreateVO;

/**
 * 订单业务服务
 *
 * @author Ateng
 * @since 2026-05-06
 */
public interface OrderService {

    /**
     * 创建订单
     *
     * @param request 创建订单请求参数
     * @return 订单创建结果
     */
    OrderCreateVO createOrder(CreateOrderRequest request);

    /**
     * 查询订单
     *
     * @param orderNo 订单号
     * @return 订单信息
     */
    OrderCreateVO getOrder(String orderNo);
}
```

文件位置：`src/main/java/io/github/atengk/order/service/impl/OrderServiceImpl.java`

```java
package io.github.atengk.order.service.impl;

import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.order.dto.CreateOrderRequest;
import io.github.atengk.order.service.OrderService;
import io.github.atengk.order.vo.OrderCreateVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 订单业务服务实现
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Service
public class OrderServiceImpl implements OrderService {

    /**
     * 创建订单
     *
     * @param request 创建订单请求参数
     * @return 订单创建结果
     */
    @Override
    public OrderCreateVO createOrder(CreateOrderRequest request) {
        if (ObjectUtil.isNull(request)) {
            log.warn("创建订单失败，请求参数为空");
            throw new IllegalArgumentException("请求参数不能为空");
        }

        if (ObjectUtil.isNull(request.getUserId()) || StrUtil.isBlank(request.getProductCode())) {
            log.warn("创建订单失败，核心参数为空，userId={}, productCode={}",
                    request.getUserId(), request.getProductCode());
            throw new IllegalArgumentException("用户ID和商品编码不能为空");
        }

        if (ObjectUtil.isNull(request.getAmount()) || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("创建订单失败，订单金额不合法，userId={}, amount={}",
                    request.getUserId(), request.getAmount());
            throw new IllegalArgumentException("订单金额不合法");
        }

        String orderNo = "ORD" + UUID.fastUUID().toString(true).substring(0, 16).toUpperCase();

        try {
            log.info("开始创建订单，orderNo={}, userId={}, productCode={}, quantity={}, amount={}",
                    orderNo, request.getUserId(), request.getProductCode(), request.getQuantity(), request.getAmount());

            // 这里模拟库存校验、价格校验、订单入库、发送订单事件等业务逻辑
            boolean saved = true;

            if (!saved) {
                log.warn("创建订单失败，订单保存未成功，orderNo={}, userId={}", orderNo, request.getUserId());
                throw new IllegalStateException("订单保存失败");
            }

            log.info("创建订单成功，orderNo={}, userId={}, amount={}",
                    orderNo, request.getUserId(), request.getAmount());

            return new OrderCreateVO(orderNo, "CREATED");
        } catch (Exception e) {
            log.error("创建订单异常，orderNo={}, userId={}, productCode={}",
                    orderNo, request.getUserId(), request.getProductCode(), e);
            throw e;
        }
    }

    /**
     * 查询订单
     *
     * @param orderNo 订单号
     * @return 订单信息
     */
    @Override
    public OrderCreateVO getOrder(String orderNo) {
        if (StrUtil.isBlank(orderNo)) {
            log.warn("查询订单失败，订单号为空");
            throw new IllegalArgumentException("订单号不能为空");
        }

        log.info("开始查询订单，orderNo={}", orderNo);

        // 这里模拟数据库查询
        OrderCreateVO result = new OrderCreateVO(orderNo, "CREATED");

        log.info("查询订单完成，orderNo={}, status={}", result.getOrderNo(), result.getStatus());
        return result;
    }
}
```

Service 层日志要突出业务语义。推荐使用“动作 + 业务对象 + 结果 + 核心字段”的格式，例如“创建订单成功，orderNo={}, userId={}”。

### Mapper 层日志

Mapper 层一般不建议手动写大量业务日志。Mapper 的主要职责是数据访问，SQL 执行情况更适合通过 MyBatis、MyBatis-Plus 或数据库连接池日志统一输出。

如果项目使用 MyBatis-Plus，推荐通过 SLF4J 输出 SQL 日志，并按环境控制日志级别。

文件位置：`src/main/resources/application-dev.yml`

```yaml
mybatis-plus:
  configuration:
    # 使用 SLF4J 输出 SQL 日志，便于统一进入 Logback 日志体系
    log-impl: org.apache.ibatis.logging.slf4j.Slf4jImpl

logging:
  level:
    # Mapper 包开启 debug 后可以看到 SQL 执行相关日志
    io.github.atengk.order.mapper: debug
```

生产环境通常不建议长期打开完整 SQL 日志，尤其是高并发系统或包含敏感参数的业务系统。生产环境可以只在临时排查时打开指定 Mapper 的 `debug` 级别。

文件位置：`src/main/resources/application-prod.yml`

```yaml
mybatis-plus:
  configuration:
    # 生产环境仍使用 SLF4J，但默认不打开 Mapper debug
    log-impl: org.apache.ibatis.logging.slf4j.Slf4jImpl

logging:
  level:
    # 生产环境 Mapper 日志默认保持 info 或 warn，避免 SQL 日志量过大
    io.github.atengk.order.mapper: info
```

Mapper 示例：

文件位置：`src/main/java/io/github/atengk/order/mapper/OrderMapper.java`

```java
package io.github.atengk.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.atengk.order.entity.OrderEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单数据访问接口
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Mapper
public interface OrderMapper extends BaseMapper<OrderEntity> {
}
```

文件位置：`src/main/java/io/github/atengk/order/entity/OrderEntity.java`

```java
package io.github.atengk.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单实体
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@TableName("t_order")
public class OrderEntity {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 订单号
     */
    private String orderNo;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 商品编码
     */
    private String productCode;

    /**
     * 订单金额
     */
    private BigDecimal amount;

    /**
     * 订单状态
     */
    private String status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
```

如果确实需要记录 Mapper 调用结果，建议在 Service 层记录，而不是在 Mapper 接口中记录。例如记录“订单写入成功，orderNo={}, rows={}”，这样业务语义更清晰。

### 关键业务节点日志

关键业务节点日志用于串联业务流程，适合记录状态变化、核心操作完成情况、外部接口调用结果和异步任务处理结果。

以订单创建为例，关键节点可以设计为：

| 节点     | 日志内容               |
| -------- | ---------------------- |
| 接收请求 | 用户、商品、数量       |
| 参数校验 | 校验失败原因           |
| 库存校验 | 商品编码、库存是否充足 |
| 订单创建 | 订单号、用户、金额     |
| 支付发起 | 支付单号、支付渠道     |
| 消息发送 | Topic、消息 ID         |
| 处理完成 | 订单号、最终状态       |

关键业务节点示例：

文件位置：`src/main/java/io/github/atengk/order/service/impl/OrderWorkflowService.java`

```java
package io.github.atengk.order.service.impl;

import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 订单流程服务
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Service
public class OrderWorkflowService {

    /**
     * 执行订单流程
     *
     * @param userId      用户ID
     * @param productCode 商品编码
     * @param amount      订单金额
     * @return 订单号
     */
    public String executeOrderFlow(Long userId, String productCode, BigDecimal amount) {
        if (userId == null || StrUtil.isBlank(productCode)) {
            log.warn("订单流程终止，核心参数为空，userId={}, productCode={}", userId, productCode);
            throw new IllegalArgumentException("订单参数不完整");
        }

        String orderNo = "ORD" + UUID.fastUUID().toString(true).substring(0, 16).toUpperCase();

        log.info("订单流程开始，orderNo={}, userId={}, productCode={}", orderNo, userId, productCode);

        boolean stockEnough = checkStock(productCode);
        if (!stockEnough) {
            log.warn("订单流程终止，库存不足，orderNo={}, productCode={}", orderNo, productCode);
            throw new IllegalStateException("库存不足");
        }
        log.info("库存校验通过，orderNo={}, productCode={}", orderNo, productCode);

        boolean orderSaved = saveOrder(orderNo, userId, productCode, amount);
        if (!orderSaved) {
            log.error("订单保存失败，orderNo={}, userId={}, productCode={}", orderNo, userId, productCode);
            throw new IllegalStateException("订单保存失败");
        }
        log.info("订单保存成功，orderNo={}, userId={}, amount={}", orderNo, userId, amount);

        boolean messageSent = sendOrderMessage(orderNo);
        if (!messageSent) {
            log.warn("订单消息发送失败，等待补偿任务处理，orderNo={}", orderNo);
        } else {
            log.info("订单消息发送成功，orderNo={}", orderNo);
        }

        log.info("订单流程完成，orderNo={}, userId={}", orderNo, userId);
        return orderNo;
    }

    /**
     * 校验库存
     *
     * @param productCode 商品编码
     * @return 是否库存充足
     */
    private boolean checkStock(String productCode) {
        return StrUtil.isNotBlank(productCode);
    }

    /**
     * 保存订单
     *
     * @param orderNo     订单号
     * @param userId      用户ID
     * @param productCode 商品编码
     * @param amount      订单金额
     * @return 是否保存成功
     */
    private boolean saveOrder(String orderNo, Long userId, String productCode, BigDecimal amount) {
        return StrUtil.isNotBlank(orderNo) && userId != null && amount != null;
    }

    /**
     * 发送订单消息
     *
     * @param orderNo 订单号
     * @return 是否发送成功
     */
    private boolean sendOrderMessage(String orderNo) {
        return StrUtil.isNotBlank(orderNo);
    }
}
```

关键业务节点日志不要求每一行代码都记录，而是记录“能还原业务过程”的节点。日志过少会导致无法排查，日志过多会增加存储成本和阅读噪音。

### 参数与返回值日志

参数和返回值日志用于辅助定位接口输入输出问题，但需要控制日志大小和敏感信息。通常建议记录参数摘要，而不是完整对象。

推荐记录：

| 类型                        | 建议                                 |
| --------------------------- | ------------------------------------ |
| 业务主键                    | 可以记录，例如 `userId`、`orderNo`   |
| 状态值                      | 可以记录，例如 `status`、`payStatus` |
| 数量金额                    | 可以记录，但金额要注意精度           |
| 手机号、身份证号            | 脱敏后记录                           |
| 密码、Token、密钥           | 禁止记录                             |
| 大对象、文件、图片、长 JSON | 不建议完整记录                       |

可以封装一个日志脱敏工具，用于统一处理敏感字段。

文件位置：`src/main/java/io/github/atengk/common/util/LogValueUtil.java`

```java
package io.github.atengk.common.util;

import cn.hutool.core.util.DesensitizedUtil;
import cn.hutool.core.util.StrUtil;

/**
 * 日志值处理工具类
 *
 * @author Ateng
 * @since 2026-05-06
 */
public class LogValueUtil {

    /**
     * 手机号脱敏
     *
     * @param mobile 手机号
     * @return 脱敏手机号
     */
    public static String mobile(String mobile) {
        if (StrUtil.isBlank(mobile)) {
            return "";
        }
        return DesensitizedUtil.mobilePhone(mobile);
    }

    /**
     * Token 脱敏
     *
     * @param token Token
     * @return 脱敏 Token
     */
    public static String token(String token) {
        if (StrUtil.isBlank(token)) {
            return "";
        }
        return StrUtil.hide(token, 6, Math.max(6, token.length() - 4));
    }

    /**
     * 长文本摘要
     *
     * @param value  原始文本
     * @param length 最大长度
     * @return 文本摘要
     */
    public static String summary(String value, int length) {
        if (StrUtil.isBlank(value)) {
            return "";
        }
        return StrUtil.maxLength(value, length);
    }
}
```

使用示例：

文件位置：`src/main/java/io/github/atengk/user/service/UserProfileService.java`

```java
package io.github.atengk.user.service;

import io.github.atengk.common.util.LogValueUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 用户资料服务
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Service
public class UserProfileService {

    /**
     * 更新用户手机号
     *
     * @param userId 用户ID
     * @param mobile 手机号
     */
    public void updateMobile(Long userId, String mobile) {
        log.info("开始更新用户手机号，userId={}, mobile={}", userId, LogValueUtil.mobile(mobile));

        // 这里模拟更新手机号逻辑

        log.info("用户手机号更新完成，userId={}, mobile={}", userId, LogValueUtil.mobile(mobile));
    }

    /**
     * 保存用户备注
     *
     * @param userId 用户ID
     * @param remark 用户备注
     */
    public void saveRemark(Long userId, String remark) {
        log.info("保存用户备注，userId={}, remarkSummary={}", userId, LogValueUtil.summary(remark, 50));

        // 这里模拟保存备注逻辑
    }
}
```

返回值日志应记录业务结果摘要，例如 `success`、`status`、`count`、`orderNo`，不建议完整输出响应对象。

```java
log.info("批量导入用户完成，total={}, success={}, fail={}", totalCount, successCount, failCount);
log.info("订单查询完成，orderNo={}, status={}", orderNo, status);
log.info("库存扣减完成，skuId={}, quantity={}, result={}", skuId, quantity, result);
```

## 请求日志记录

请求日志用于统一记录 HTTP 请求的入口和出口信息，包括请求路径、请求参数、请求耗时、响应状态和 TraceId。相比在每个 Controller 中重复记录，请求日志更适合通过 Filter 或 Interceptor 统一实现。

### 请求路径记录

请求路径记录用于识别接口访问情况，通常需要记录请求方法、URI、客户端 IP、User-Agent 和 TraceId。Spring Boot 3 推荐使用 `OncePerRequestFilter` 实现统一请求日志。

请求日志实现文件结构如下：

```text
src/main/java/io/github/atengk/common/filter/TraceIdFilter.java
src/main/java/io/github/atengk/common/filter/RequestLogFilter.java
src/main/java/io/github/atengk/common/util/RequestLogUtil.java
src/main/resources/logback-spring.xml
```

TraceId 需要优先放入 MDC，后续日志格式中通过 `%X{traceId}` 输出。

### 请求参数记录

请求参数包括 Query 参数、表单参数和 JSON 请求体。记录请求参数时需要注意两点：第一，请求体默认只能读取一次；第二，敏感字段必须脱敏。

在 Filter 中读取请求体时，可以使用 Spring 提供的 `ContentCachingRequestWrapper` 包装请求对象。

文件位置：`src/main/java/io/github/atengk/common/util/RequestLogUtil.java`

```java
package io.github.atengk.common.util;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * 请求日志工具类
 *
 * @author Ateng
 * @since 2026-05-06
 */
public class RequestLogUtil {

    private static final Set<String> SKIP_URI_SET = Set.of(
            "/actuator/health",
            "/actuator/prometheus"
    );

    /**
     * 判断是否跳过请求日志
     *
     * @param request 请求对象
     * @return 是否跳过
     */
    public static boolean shouldSkip(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return SKIP_URI_SET.contains(uri);
    }

    /**
     * 判断是否为可记录的文本请求
     *
     * @param request 请求对象
     * @return 是否为文本请求
     */
    public static boolean isTextRequest(HttpServletRequest request) {
        String contentType = request.getContentType();
        if (StrUtil.isBlank(contentType)) {
            return true;
        }
        return StrUtil.containsAnyIgnoreCase(contentType,
                MediaType.APPLICATION_JSON_VALUE,
                MediaType.APPLICATION_FORM_URLENCODED_VALUE,
                MediaType.TEXT_PLAIN_VALUE,
                MediaType.TEXT_XML_VALUE,
                MediaType.APPLICATION_XML_VALUE);
    }

    /**
     * 获取请求体摘要
     *
     * @param request 请求包装对象
     * @return 请求体摘要
     */
    public static String getRequestBody(ContentCachingRequestWrapper request) {
        byte[] content = request.getContentAsByteArray();
        if (ArrayUtil.isEmpty(content)) {
            return "";
        }

        String body = new String(content, StandardCharsets.UTF_8);
        return maskSensitiveValue(StrUtil.maxLength(body, 2000));
    }

    /**
     * 脱敏敏感字段
     *
     * @param value 原始内容
     * @return 脱敏后内容
     */
    public static String maskSensitiveValue(String value) {
        if (StrUtil.isBlank(value)) {
            return "";
        }

        String result = value;
        result = result.replaceAll("(?i)(\"password\"\\s*:\\s*\")([^\"]+)(\")", "$1******$3");
        result = result.replaceAll("(?i)(\"token\"\\s*:\\s*\")([^\"]+)(\")", "$1******$3");
        result = result.replaceAll("(?i)(\"authorization\"\\s*:\\s*\")([^\"]+)(\")", "$1******$3");
        result = result.replaceAll("(?i)(\"idCard\"\\s*:\\s*\")([^\"]+)(\")", "$1******$3");
        result = result.replaceAll("(?i)(\"mobile\"\\s*:\\s*\")([^\"]+)(\")", "$1******$3");
        return result;
    }

    /**
     * 获取客户端 IP
     *
     * @param request 请求对象
     * @return 客户端 IP
     */
    public static String getClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StrUtil.isNotBlank(forwardedFor)) {
            return StrUtil.splitTrim(forwardedFor, ",").get(0);
        }

        String realIp = request.getHeader("X-Real-IP");
        if (StrUtil.isNotBlank(realIp)) {
            return realIp;
        }

        return request.getRemoteAddr();
    }
}
```

请求参数日志建议限制长度，例如最多 2000 个字符，避免大 JSON、Base64、文件内容撑爆日志文件。

### 请求耗时记录

请求耗时用于判断接口性能和定位慢请求。通常在请求进入时记录开始时间，在请求完成后计算耗时。

文件位置：`src/main/java/io/github/atengk/common/filter/RequestLogFilter.java`

```java
package io.github.atengk.common.filter;

import cn.hutool.core.date.StopWatch;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.common.util.RequestLogUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 请求日志过滤器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class RequestLogFilter extends OncePerRequestFilter {

    /**
     * 记录请求日志
     *
     * @param request     请求对象
     * @param response    响应对象
     * @param filterChain 过滤器链
     * @throws ServletException Servlet异常
     * @throws IOException      IO异常
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (RequestLogUtil.shouldSkip(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        String method = request.getMethod();
        String uri = request.getRequestURI();
        String queryString = StrUtil.blankToDefault(request.getQueryString(), "");
        String clientIp = RequestLogUtil.getClientIp(request);
        String userAgent = StrUtil.maxLength(StrUtil.blankToDefault(request.getHeader("User-Agent"), ""), 200);

        try {
            log.info("请求开始，method={}, uri={}, query={}, clientIp={}, userAgent={}",
                    method, uri, queryString, clientIp, userAgent);

            filterChain.doFilter(requestWrapper, responseWrapper);
        } finally {
            stopWatch.stop();

            String requestBody = "";
            if (RequestLogUtil.isTextRequest(requestWrapper)) {
                requestBody = RequestLogUtil.getRequestBody(requestWrapper);
            }

            int status = responseWrapper.getStatus();
            long cost = stopWatch.getTotalTimeMillis();

            String responseBody = getResponseBody(responseWrapper);

            if (cost >= 3000) {
                log.warn("慢请求完成，method={}, uri={}, status={}, cost={}ms, requestBody={}, responseBody={}",
                        method, uri, status, cost, requestBody, responseBody);
            } else {
                log.info("请求完成，method={}, uri={}, status={}, cost={}ms, requestBody={}, responseBody={}",
                        method, uri, status, cost, requestBody, responseBody);
            }

            responseWrapper.copyBodyToResponse();
        }
    }

    /**
     * 获取响应体摘要
     *
     * @param responseWrapper 响应包装对象
     * @return 响应体摘要
     */
    private String getResponseBody(ContentCachingResponseWrapper responseWrapper) {
        String contentType = responseWrapper.getContentType();
        if (StrUtil.isBlank(contentType)) {
            return "";
        }

        if (!StrUtil.containsAnyIgnoreCase(contentType,
                MediaType.APPLICATION_JSON_VALUE,
                MediaType.TEXT_PLAIN_VALUE,
                MediaType.TEXT_XML_VALUE,
                MediaType.APPLICATION_XML_VALUE)) {
            return "";
        }

        byte[] content = responseWrapper.getContentAsByteArray();
        if (content.length == 0) {
            return "";
        }

        String body = new String(content, StandardCharsets.UTF_8);
        return RequestLogUtil.maskSensitiveValue(StrUtil.maxLength(body, 2000));
    }
}
```

该 Filter 会记录请求开始和请求完成日志，并在耗时超过 3000ms 时使用 `warn` 级别标记为慢请求。实际项目中慢请求阈值可以根据接口类型调整，例如普通接口 1000ms，报表接口 5000ms。

### 响应结果记录

响应结果日志用于辅助判断接口是否成功返回。一般建议记录响应状态码、业务状态码、响应摘要和耗时，不建议完整记录所有响应数据。

响应结果记录建议如下：

| 响应类型             | 建议                         |
| -------------------- | ---------------------------- |
| JSON 小对象          | 可记录摘要                   |
| 列表数据             | 记录数量，不记录完整列表     |
| 文件下载             | 不记录响应体                 |
| 图片、视频、二进制流 | 不记录响应体                 |
| 包含敏感字段         | 脱敏后记录                   |
| 异常响应             | 记录状态码、错误码、错误摘要 |

如果项目有统一响应结构，可以只记录 `code`、`message`、`success`，避免记录完整 `data`。

统一响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "orderNo": "ORD123456"
  }
}
```

推荐日志：

```text
请求完成，method=POST, uri=/api/orders, status=200, cost=68ms, responseBody={"code":200,"message":"操作成功","data":{"orderNo":"ORD123456"}}
```

如果响应结果较大，建议截断：

```text
请求完成，method=GET, uri=/api/orders, status=200, cost=120ms, responseBody={"code":200,"message":"操作成功","data":[...]}
```

在高并发生产环境中，响应体日志可以只在测试环境开启。生产环境建议只记录请求路径、状态码、耗时、业务主键和异常摘要。

### TraceId 链路标识

TraceId 用于串联一次请求中的所有日志。只要在请求入口将 TraceId 放入 MDC，当前线程中的日志都可以自动输出 TraceId。

推荐规则：

| 来源                    | 处理方式                |
| ----------------------- | ----------------------- |
| 请求头已有 `X-Trace-Id` | 直接使用                |
| 请求头没有 TraceId      | 服务端生成              |
| 响应头                  | 返回 `X-Trace-Id`       |
| 日志格式                | 使用 `%X{traceId}` 输出 |
| 异步线程                | 需要额外传递 MDC        |

文件位置：`src/main/java/io/github/atengk/common/filter/TraceIdFilter.java`

```java
package io.github.atengk.common.filter;

import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.StrUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * TraceId 过滤器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    public static final String TRACE_ID = "traceId";
    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    /**
     * 生成并绑定 TraceId
     *
     * @param request     请求对象
     * @param response    响应对象
     * @param filterChain 过滤器链
     * @throws ServletException Servlet异常
     * @throws IOException      IO异常
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String traceId = request.getHeader(TRACE_ID_HEADER);
        if (StrUtil.isBlank(traceId)) {
            traceId = UUID.fastUUID().toString(true);
        }

        try {
            MDC.put(TRACE_ID, traceId);
            response.setHeader(TRACE_ID_HEADER, traceId);
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(TRACE_ID);
        }
    }
}
```

需要在 `logback-spring.xml` 中增加 TraceId 输出。

文件位置：`src/main/resources/logback-spring.xml`

```xml
<!-- 控制台日志格式：增加 traceId -->
<property name="CONSOLE_LOG_PATTERN"
          value="%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%thread] [%X{traceId}] %logger{36} - %msg%n"/>

<!-- 文件日志格式：增加 traceId -->
<property name="FILE_LOG_PATTERN"
          value="%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%thread] [%X{traceId}] %logger{50} - %msg%n"/>
```

接口调用时可以传入 TraceId：

```bash
curl -X POST "http://localhost:8080/api/orders" \
  -H "Content-Type: application/json" \
  -H "X-Trace-Id: test-trace-001" \
  -d '{
    "userId": 10001,
    "productCode": "SKU-10001",
    "quantity": 2,
    "amount": 99.90
  }'
```

日志输出示例：

```text
2026-05-06 11:20:30.123 INFO  [http-nio-8080-exec-1] [test-trace-001] i.g.a.common.filter.RequestLogFilter - 请求开始，method=POST, uri=/api/orders, query=, clientIp=127.0.0.1, userAgent=curl/8.0.1
2026-05-06 11:20:30.145 INFO  [http-nio-8080-exec-1] [test-trace-001] i.g.a.order.controller.OrderController - 接收创建订单请求，userId=10001, productCode=SKU-10001, quantity=2
2026-05-06 11:20:30.180 INFO  [http-nio-8080-exec-1] [test-trace-001] i.g.a.order.service.impl.OrderServiceImpl - 创建订单成功，orderNo=ORD2F4D9A1B8C7E6F5A, userId=10001, amount=99.90
2026-05-06 11:20:30.201 INFO  [http-nio-8080-exec-1] [test-trace-001] i.g.a.common.filter.RequestLogFilter - 请求完成，method=POST, uri=/api/orders, status=200, cost=78ms, requestBody={"userId":10001,"productCode":"SKU-10001","quantity":2,"amount":99.90}, responseBody={"orderNo":"ORD2F4D9A1B8C7E6F5A","status":"CREATED"}
```

如果业务中使用 `@Async`、线程池、CompletableFuture 或 MQ 消费，MDC 不会自动跨线程传递，需要在线程池装饰器或消息上下文中手动传递 TraceId。普通同步 Web 请求中，以上 Filter 配置已经可以满足基本链路日志需求。

## 异常日志记录

异常日志用于记录系统运行过程中出现的错误、失败和不可预期分支。异常日志的核心目标不是“把错误打印出来”，而是保留足够的上下文，帮助开发人员在没有复现环境的情况下定位问题。

### 全局异常处理日志

全局异常处理器用于统一处理 Controller 层向外抛出的异常，避免异常堆栈直接返回给前端，同时确保服务端能够记录完整异常信息。Spring Boot 3 推荐使用 `@RestControllerAdvice` 和 `@ExceptionHandler` 实现统一异常处理。

推荐文件结构如下：

```text
src/main/java/io/github/atengk/common/exception/BusinessException.java
src/main/java/io/github/atengk/common/handler/GlobalExceptionHandler.java
src/main/java/io/github/atengk/common/model/ApiResult.java
```

文件位置：`src/main/java/io/github/atengk/common/model/ApiResult.java`

```java
package io.github.atengk.common.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 接口统一响应结果
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResult<T> {

    /**
     * 响应码
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
     * 返回成功结果
     *
     * @param data 响应数据
     * @return 统一响应结果
     */
    public static <T> ApiResult<T> success(T data) {
        return new ApiResult<>(200, "操作成功", data);
    }

    /**
     * 返回失败结果
     *
     * @param code    响应码
     * @param message 响应消息
     * @return 统一响应结果
     */
    public static <T> ApiResult<T> fail(Integer code, String message) {
        return new ApiResult<>(code, message, null);
    }
}
```

文件位置：`src/main/java/io/github/atengk/common/exception/BusinessException.java`

```java
package io.github.atengk.common.exception;

import lombok.Getter;

/**
 * 业务异常
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Getter
public class BusinessException extends RuntimeException {

    /**
     * 业务错误码
     */
    private final Integer code;

    /**
     * 创建业务异常
     *
     * @param message 异常消息
     */
    public BusinessException(String message) {
        super(message);
        this.code = 400;
    }

    /**
     * 创建业务异常
     *
     * @param code    错误码
     * @param message 异常消息
     */
    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}
```

全局异常处理器统一记录业务异常、参数校验异常和系统异常。

文件位置：`src/main/java/io/github/atengk/common/handler/GlobalExceptionHandler.java`

```java
package io.github.atengk.common.handler;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.common.exception.BusinessException;
import io.github.atengk.common.model.ApiResult;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常
     *
     * @param request 请求对象
     * @param e       业务异常
     * @return 统一响应结果
     */
    @ExceptionHandler(BusinessException.class)
    public ApiResult<Void> handleBusinessException(HttpServletRequest request, BusinessException e) {
        log.warn("业务异常，method={}, uri={}, code={}, message={}",
                request.getMethod(), request.getRequestURI(), e.getCode(), e.getMessage());
        return ApiResult.fail(e.getCode(), e.getMessage());
    }

    /**
     * 处理请求体参数校验异常
     *
     * @param request 请求对象
     * @param e       参数校验异常
     * @return 统一响应结果
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResult<Void> handleMethodArgumentNotValidException(HttpServletRequest request,
                                                                 MethodArgumentNotValidException e) {
        String message = getFieldErrorMessage(e.getBindingResult().getFieldError());
        log.warn("请求参数校验失败，method={}, uri={}, message={}",
                request.getMethod(), request.getRequestURI(), message);
        return ApiResult.fail(400, message);
    }

    /**
     * 处理表单参数绑定异常
     *
     * @param request 请求对象
     * @param e       参数绑定异常
     * @return 统一响应结果
     */
    @ExceptionHandler(BindException.class)
    public ApiResult<Void> handleBindException(HttpServletRequest request, BindException e) {
        String message = getFieldErrorMessage(e.getBindingResult().getFieldError());
        log.warn("请求参数绑定失败，method={}, uri={}, message={}",
                request.getMethod(), request.getRequestURI(), message);
        return ApiResult.fail(400, message);
    }

    /**
     * 处理系统异常
     *
     * @param request 请求对象
     * @param e       系统异常
     * @return 统一响应结果
     */
    @ExceptionHandler(Exception.class)
    public ApiResult<Void> handleException(HttpServletRequest request, Exception e) {
        log.error("系统异常，method={}, uri={}", request.getMethod(), request.getRequestURI(), e);
        return ApiResult.fail(500, "系统繁忙，请稍后重试");
    }

    /**
     * 获取字段错误消息
     *
     * @param fieldError 字段错误
     * @return 错误消息
     */
    private String getFieldErrorMessage(FieldError fieldError) {
        if (fieldError == null || StrUtil.isBlank(fieldError.getDefaultMessage())) {
            return "请求参数不合法";
        }
        return fieldError.getDefaultMessage();
    }
}
```

全局异常日志建议遵循以下规则：

| 异常类型       | 日志级别 | 是否打印堆栈 | 说明                                         |
| -------------- | -------- | ------------ | -------------------------------------------- |
| 业务异常       | `warn`   | 否           | 可预期异常，例如库存不足、订单状态不允许操作 |
| 参数校验异常   | `warn`   | 否           | 请求参数错误，不属于系统故障                 |
| 系统异常       | `error`  | 是           | 非预期异常，必须保留堆栈                     |
| 第三方接口异常 | `error`  | 是           | 需要保留接口名、业务标识、耗时和异常堆栈     |

### 业务异常日志

业务异常是系统可预期的异常，例如订单不存在、余额不足、重复提交、状态不允许变更等。业务异常通常不需要记录完整堆栈，否则会造成日志噪音。

业务异常建议在业务判断处记录关键上下文，然后抛出 `BusinessException`，由全局异常处理器统一返回响应。

文件位置：`src/main/java/io/github/atengk/order/service/impl/OrderCancelService.java`

```java
package io.github.atengk.order.service.impl;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 订单取消服务
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Service
public class OrderCancelService {

    /**
     * 取消订单
     *
     * @param orderNo 订单号
     * @param userId  用户ID
     */
    public void cancelOrder(String orderNo, Long userId) {
        if (StrUtil.isBlank(orderNo)) {
            log.warn("取消订单失败，订单号为空，userId={}", userId);
            throw new BusinessException("订单号不能为空");
        }

        String status = getOrderStatus(orderNo);
        if (StrUtil.equals(status, "PAID")) {
            log.warn("取消订单失败，订单已支付不允许取消，orderNo={}, userId={}, status={}",
                    orderNo, userId, status);
            throw new BusinessException("订单已支付，不允许取消");
        }

        if (StrUtil.equals(status, "CANCELED")) {
            log.warn("取消订单忽略，订单已取消，orderNo={}, userId={}, status={}",
                    orderNo, userId, status);
            throw new BusinessException("订单已取消，请勿重复操作");
        }

        log.info("开始取消订单，orderNo={}, userId={}, status={}", orderNo, userId, status);

        // 这里模拟订单取消逻辑，例如更新订单状态、释放库存、发送取消消息

        log.info("取消订单成功，orderNo={}, userId={}", orderNo, userId);
    }

    /**
     * 获取订单状态
     *
     * @param orderNo 订单号
     * @return 订单状态
     */
    private String getOrderStatus(String orderNo) {
        return "CREATED";
    }
}
```

业务异常日志不要滥用 `error`。例如“用户余额不足”“订单状态不允许取消”是业务规则命中，不是系统故障，使用 `warn` 更合理。

### 系统异常日志

系统异常是不可预期异常，例如空指针、数据库连接失败、Redis 连接失败、序列化失败、文件读写失败等。这类异常通常需要使用 `error` 级别，并记录完整异常堆栈。

系统异常日志必须包含三类信息：业务上下文、失败位置、异常对象。

推荐写法：

```java
log.error("保存订单异常，orderNo={}, userId={}", orderNo, userId, e);
```

不推荐写法：

```java
log.error("保存订单异常：" + e.getMessage());
e.printStackTrace();
```

系统异常示例：

文件位置：`src/main/java/io/github/atengk/order/service/impl/OrderSaveService.java`

```java
package io.github.atengk.order.service.impl;

import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 订单保存服务
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Service
public class OrderSaveService {

    /**
     * 保存订单
     *
     * @param userId 用户ID
     * @param amount 订单金额
     * @return 订单号
     */
    public String saveOrder(Long userId, BigDecimal amount) {
        if (ObjectUtil.hasEmpty(userId, amount)) {
            log.warn("保存订单失败，核心参数为空，userId={}, amount={}", userId, amount);
            throw new BusinessException("订单参数不完整");
        }

        String orderNo = "ORD" + UUID.fastUUID().toString(true).substring(0, 16).toUpperCase();

        try {
            log.info("开始保存订单，orderNo={}, userId={}, amount={}", orderNo, userId, amount);

            // 这里模拟数据库保存逻辑
            boolean saved = true;

            if (!saved) {
                log.error("保存订单失败，数据库返回失败结果，orderNo={}, userId={}", orderNo, userId);
                throw new IllegalStateException("订单保存失败");
            }

            log.info("保存订单成功，orderNo={}, userId={}", orderNo, userId);
            return orderNo;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("保存订单系统异常，orderNo={}, userId={}, amount={}", orderNo, userId, amount, e);
            throw new IllegalStateException("保存订单失败", e);
        }
    }
}
```

系统异常日志中不要只记录 `e.getMessage()`。`e.getMessage()` 只能说明异常摘要，不能提供完整调用栈，线上排查价值很低。

### 第三方接口异常日志

第三方接口异常通常发生在支付、短信、物流、认证、对象存储、消息推送等场景。记录这类日志时，应包含接口名称、请求业务标识、第三方响应码、耗时、异常堆栈和降级处理结果。

第三方接口日志建议记录以下字段：

| 字段         | 说明                      |
| ------------ | ------------------------- |
| `apiName`    | 第三方接口名称            |
| `requestId`  | 本地请求流水号            |
| `businessNo` | 业务单号，例如订单号      |
| `statusCode` | HTTP 状态码或第三方业务码 |
| `cost`       | 接口耗时                  |
| `result`     | 调用结果摘要              |
| `error`      | 异常堆栈                  |

文件位置：`src/main/java/io/github/atengk/payment/service/PaymentRemoteService.java`

```java
package io.github.atengk.payment.service;

import cn.hutool.core.date.StopWatch;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 支付远程接口服务
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Service
public class PaymentRemoteService {

    /**
     * 查询支付结果
     *
     * @param orderNo 订单号
     * @return 支付状态
     */
    public String queryPayResult(String orderNo) {
        if (StrUtil.isBlank(orderNo)) {
            log.warn("查询支付结果失败，订单号为空");
            throw new BusinessException("订单号不能为空");
        }

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        String apiName = "payment.queryPayResult";

        try {
            log.info("开始调用支付查询接口，apiName={}, orderNo={}", apiName, orderNo);

            // 这里模拟第三方支付接口调用
            String payStatus = mockQueryPayResult(orderNo);

            stopWatch.stop();
            log.info("支付查询接口调用成功，apiName={}, orderNo={}, payStatus={}, cost={}ms",
                    apiName, orderNo, payStatus, stopWatch.getTotalTimeMillis());

            return payStatus;
        } catch (Exception e) {
            stopWatch.stop();
            log.error("支付查询接口调用异常，apiName={}, orderNo={}, cost={}ms",
                    apiName, orderNo, stopWatch.getTotalTimeMillis(), e);
            throw new IllegalStateException("支付接口调用失败", e);
        }
    }

    /**
     * 模拟查询支付结果
     *
     * @param orderNo 订单号
     * @return 支付状态
     */
    private String mockQueryPayResult(String orderNo) {
        return "SUCCESS";
    }
}
```

第三方接口返回失败但没有抛出异常时，也需要记录日志。

```java
log.warn("短信发送失败，apiName={}, mobile={}, responseCode={}, responseMessage={}, cost={}ms",
        apiName, maskedMobile, responseCode, responseMessage, cost);
```

如果第三方返回内容包含手机号、身份证号、银行卡号、Token、签名、密钥等敏感信息，必须先脱敏或只记录响应摘要。

## AOP 日志切面

AOP 日志切面用于统一记录方法调用日志，适合记录管理后台操作日志、重要业务方法调用日志、接口调用耗时和异常日志。相比在每个方法中手动写日志，AOP 更适合横切逻辑，但不应替代关键业务节点日志。

### 日志注解设计

日志注解用于标记哪些方法需要被切面记录。推荐将注解放在 Controller 方法或 Service 关键业务方法上，避免对所有方法无差别拦截。

文件结构如下：

```text
src/main/java/io/github/atengk/common/annotation/OperationLog.java
src/main/java/io/github/atengk/common/aspect/OperationLogAspect.java
src/main/java/io/github/atengk/common/util/OperationLogUtil.java
```

文件位置：`src/main/java/io/github/atengk/common/annotation/OperationLog.java`

```java
package io.github.atengk.common.annotation;

import java.lang.annotation.*;

/**
 * 操作日志注解
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLog {

    /**
     * 操作模块
     *
     * @return 模块名称
     */
    String module();

    /**
     * 操作名称
     *
     * @return 操作名称
     */
    String operation();

    /**
     * 是否记录入参
     *
     * @return 是否记录入参
     */
    boolean recordArgs() default true;

    /**
     * 是否记录返回值
     *
     * @return 是否记录返回值
     */
    boolean recordResult() default false;
}
```

注解使用示例：

```java
@OperationLog(module = "订单管理", operation = "创建订单", recordArgs = true, recordResult = true)
@PostMapping
public ApiResult<OrderCreateVO> createOrder(@Valid @RequestBody CreateOrderRequest request) {
    return ApiResult.success(orderService.createOrder(request));
}
```

通常建议默认不记录完整返回值，尤其是列表查询、导出下载、详情查询等接口。对于创建、修改、删除类接口，可以记录返回摘要。

### 切面拦截逻辑

切面负责拦截带有 `@OperationLog` 的方法，记录模块、操作名称、方法签名、入参摘要、返回值摘要、执行耗时和异常信息。

文件位置：`src/main/java/io/github/atengk/common/util/OperationLogUtil.java`

```java
package io.github.atengk.common.util;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;

/**
 * 操作日志工具类
 *
 * @author Ateng
 * @since 2026-05-06
 */
public class OperationLogUtil {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * 参数转日志文本
     *
     * @param args 方法参数
     * @return 参数摘要
     */
    public static String argsToLogText(Object[] args) {
        if (ArrayUtil.isEmpty(args)) {
            return "";
        }

        Object[] filteredArgs = Arrays.stream(args)
                .filter(OperationLogUtil::isRecordableArg)
                .toArray();

        if (ArrayUtil.isEmpty(filteredArgs)) {
            return "";
        }

        return toJsonSummary(filteredArgs);
    }

    /**
     * 返回值转日志文本
     *
     * @param result 返回值
     * @return 返回值摘要
     */
    public static String resultToLogText(Object result) {
        if (result == null) {
            return "";
        }
        return toJsonSummary(result);
    }

    /**
     * 转 JSON 摘要
     *
     * @param value 对象
     * @return JSON 摘要
     */
    public static String toJsonSummary(Object value) {
        try {
            String json = OBJECT_MAPPER.writeValueAsString(value);
            String masked = maskSensitiveValue(json);
            return StrUtil.maxLength(masked, 2000);
        } catch (Exception e) {
            return StrUtil.maxLength(String.valueOf(value), 500);
        }
    }

    /**
     * 判断参数是否可以记录
     *
     * @param arg 参数
     * @return 是否可以记录
     */
    private static boolean isRecordableArg(Object arg) {
        if (arg == null) {
            return false;
        }

        return !(arg instanceof ServletRequest)
                && !(arg instanceof ServletResponse)
                && !(arg instanceof MultipartFile);
    }

    /**
     * 脱敏敏感字段
     *
     * @param value 原始内容
     * @return 脱敏内容
     */
    public static String maskSensitiveValue(String value) {
        if (StrUtil.isBlank(value)) {
            return "";
        }

        String result = value;
        result = result.replaceAll("(?i)(\"password\"\\s*:\\s*\")([^\"]+)(\")", "$1******$3");
        result = result.replaceAll("(?i)(\"token\"\\s*:\\s*\")([^\"]+)(\")", "$1******$3");
        result = result.replaceAll("(?i)(\"authorization\"\\s*:\\s*\")([^\"]+)(\")", "$1******$3");
        result = result.replaceAll("(?i)(\"mobile\"\\s*:\\s*\")([^\"]+)(\")", "$1******$3");
        result = result.replaceAll("(?i)(\"idCard\"\\s*:\\s*\")([^\"]+)(\")", "$1******$3");
        return result;
    }
}
```

切面核心逻辑如下：

文件位置：`src/main/java/io/github/atengk/common/aspect/OperationLogAspect.java`

```java
package io.github.atengk.common.aspect;

import cn.hutool.core.date.StopWatch;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.common.annotation.OperationLog;
import io.github.atengk.common.util.OperationLogUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

/**
 * 操作日志切面
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Aspect
@Component
public class OperationLogAspect {

    /**
     * 拦截操作日志注解
     *
     * @param joinPoint    切点
     * @param operationLog 操作日志注解
     * @return 方法返回值
     * @throws Throwable 方法执行异常
     */
    @Around("@annotation(operationLog)")
    public Object around(ProceedingJoinPoint joinPoint, OperationLog operationLog) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className = signature.getDeclaringTypeName();
        String methodName = signature.getName();
        String methodFullName = StrUtil.format("{}.{}", className, methodName);

        String argsText = "";
        if (operationLog.recordArgs()) {
            argsText = OperationLogUtil.argsToLogText(joinPoint.getArgs());
        }

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        try {
            log.info("操作开始，module={}, operation={}, method={}, args={}",
                    operationLog.module(), operationLog.operation(), methodFullName, argsText);

            Object result = joinPoint.proceed();

            stopWatch.stop();

            if (operationLog.recordResult()) {
                String resultText = OperationLogUtil.resultToLogText(result);
                log.info("操作完成，module={}, operation={}, method={}, cost={}ms, result={}",
                        operationLog.module(), operationLog.operation(), methodFullName,
                        stopWatch.getTotalTimeMillis(), resultText);
            } else {
                log.info("操作完成，module={}, operation={}, method={}, cost={}ms",
                        operationLog.module(), operationLog.operation(), methodFullName,
                        stopWatch.getTotalTimeMillis());
            }

            return result;
        } catch (Throwable e) {
            stopWatch.stop();
            log.error("操作异常，module={}, operation={}, method={}, cost={}ms, args={}",
                    operationLog.module(), operationLog.operation(), methodFullName,
                    stopWatch.getTotalTimeMillis(), argsText, e);
            throw e;
        }
    }
}
```

使用 AOP 日志时需要引入 AOP 依赖。

文件位置：`pom.xml`

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>
```

### 方法入参记录

方法入参日志用于记录关键请求参数，便于后续排查“用户传了什么”。但入参日志必须控制范围，不能无差别记录所有参数。

推荐记录：

| 参数类型              | 是否建议记录                       |
| --------------------- | ---------------------------------- |
| 普通 DTO              | 可以记录，但需脱敏和截断           |
| `HttpServletRequest`  | 不建议直接记录                     |
| `HttpServletResponse` | 不建议记录                         |
| `MultipartFile`       | 不记录文件内容，只记录文件名、大小 |
| 密码、Token、密钥     | 禁止明文记录                       |
| 大集合、大 JSON       | 只记录数量或摘要                   |

示例 Controller：

文件位置：`src/main/java/io/github/atengk/order/controller/OrderOperationController.java`

```java
package io.github.atengk.order.controller;

import io.github.atengk.common.annotation.OperationLog;
import io.github.atengk.common.model.ApiResult;
import io.github.atengk.order.dto.CreateOrderRequest;
import io.github.atengk.order.service.OrderService;
import io.github.atengk.order.vo.OrderCreateVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 订单操作接口
 *
 * @author Ateng
 * @since 2026-05-06
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/order-operations")
public class OrderOperationController {

    private final OrderService orderService;

    /**
     * 创建订单
     *
     * @param request 创建订单请求参数
     * @return 订单创建结果
     */
    @OperationLog(module = "订单管理", operation = "创建订单", recordArgs = true, recordResult = true)
    @PostMapping
    public ApiResult<OrderCreateVO> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        return ApiResult.success(orderService.createOrder(request));
    }

    /**
     * 查询订单
     *
     * @param orderNo 订单号
     * @return 订单信息
     */
    @OperationLog(module = "订单管理", operation = "查询订单", recordArgs = true, recordResult = false)
    @GetMapping("/{orderNo}")
    public ApiResult<OrderCreateVO> getOrder(@PathVariable String orderNo) {
        return ApiResult.success(orderService.getOrder(orderNo));
    }
}
```

日志输出示例：

```text
2026-05-06 14:20:30.123 INFO  [http-nio-8080-exec-1] [trace-001] i.g.a.common.aspect.OperationLogAspect - 操作开始，module=订单管理, operation=创建订单, method=io.github.atengk.order.controller.OrderOperationController.createOrder, args=[{"userId":10001,"productCode":"SKU-10001","quantity":2,"amount":99.90}]
2026-05-06 14:20:30.188 INFO  [http-nio-8080-exec-1] [trace-001] i.g.a.common.aspect.OperationLogAspect - 操作完成，module=订单管理, operation=创建订单, method=io.github.atengk.order.controller.OrderOperationController.createOrder, cost=65ms, result={"code":200,"message":"操作成功","data":{"orderNo":"ORD123456","status":"CREATED"}}
```

入参日志应优先服务于问题定位，不应作为数据备份。需要审计留痕的操作，建议单独设计操作日志表，而不是只依赖文件日志。

### 方法执行耗时记录

方法执行耗时日志用于定位慢接口、慢业务逻辑和第三方服务性能问题。AOP 切面中可以统一记录方法耗时，并对超过阈值的方法输出 `warn` 日志。

可以在注解中增加慢方法阈值。

文件位置：`src/main/java/io/github/atengk/common/annotation/OperationLog.java`

```java
package io.github.atengk.common.annotation;

import java.lang.annotation.*;

/**
 * 操作日志注解
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLog {

    /**
     * 操作模块
     *
     * @return 模块名称
     */
    String module();

    /**
     * 操作名称
     *
     * @return 操作名称
     */
    String operation();

    /**
     * 是否记录入参
     *
     * @return 是否记录入参
     */
    boolean recordArgs() default true;

    /**
     * 是否记录返回值
     *
     * @return 是否记录返回值
     */
    boolean recordResult() default false;

    /**
     * 慢方法阈值，单位毫秒
     *
     * @return 慢方法阈值
     */
    long slowThresholdMs() default 1000;
}
```

切面中根据耗时判断是否输出慢方法日志。

```java
if (cost >= operationLog.slowThresholdMs()) {
    log.warn("操作耗时较长，module={}, operation={}, method={}, cost={}ms, threshold={}ms",
            operationLog.module(), operationLog.operation(), methodFullName,
            cost, operationLog.slowThresholdMs());
}
```

推荐完整处理片段如下：

```java
long cost = stopWatch.getTotalTimeMillis();

if (cost >= operationLog.slowThresholdMs()) {
    log.warn("操作耗时较长，module={}, operation={}, method={}, cost={}ms, threshold={}ms",
            operationLog.module(), operationLog.operation(), methodFullName,
            cost, operationLog.slowThresholdMs());
}

log.info("操作完成，module={}, operation={}, method={}, cost={}ms",
        operationLog.module(), operationLog.operation(), methodFullName, cost);
```

使用示例：

```java
@OperationLog(module = "报表管理", operation = "导出订单报表", recordArgs = true, recordResult = false, slowThresholdMs = 5000)
@GetMapping("/export")
public ApiResult<String> exportOrderReport(String startDate, String endDate) {
    return ApiResult.success("导出任务已提交");
}
```

慢方法日志不是错误日志，不应使用 `error`。只有当方法执行失败并抛出异常时，才使用 `error` 记录异常堆栈。

### 异常捕获与记录

AOP 切面中的异常捕获用于记录被拦截方法的异常信息，但不能吞掉异常。正确做法是记录异常上下文后继续向外抛出，让全局异常处理器、事务管理器或上层调用方继续处理。

推荐写法：

```java
catch (Throwable e) {
    stopWatch.stop();
    log.error("操作异常，module={}, operation={}, method={}, cost={}ms, args={}",
            operationLog.module(), operationLog.operation(), methodFullName,
            stopWatch.getTotalTimeMillis(), argsText, e);
    throw e;
}
```

不推荐写法：

```java
catch (Exception e) {
    log.error("操作异常", e);
    return null;
}
```

不推荐在切面中返回 `null` 或自行构造失败响应，因为这会破坏原方法语义，也可能导致事务无法正常回滚。

如果切面和全局异常处理器都会记录异常，需要避免重复日志过多。一般建议：

| 位置           | 记录内容                                              |
| -------------- | ----------------------------------------------------- |
| AOP 切面       | 记录操作模块、操作名称、方法名、入参摘要、耗时        |
| 全局异常处理器 | 记录请求方法、请求路径、异常堆栈                      |
| Service 层     | 记录关键业务上下文，例如订单号、用户 ID、第三方接口名 |

对于业务异常，切面可以使用 `warn`，系统异常使用 `error`。可以在切面中区分异常类型：

```java
catch (BusinessException e) {
    stopWatch.stop();
    log.warn("操作业务异常，module={}, operation={}, method={}, cost={}ms, args={}, message={}",
            operationLog.module(), operationLog.operation(), methodFullName,
            stopWatch.getTotalTimeMillis(), argsText, e.getMessage());
    throw e;
} catch (Throwable e) {
    stopWatch.stop();
    log.error("操作系统异常，module={}, operation={}, method={}, cost={}ms, args={}",
            operationLog.module(), operationLog.operation(), methodFullName,
            stopWatch.getTotalTimeMillis(), argsText, e);
    throw e;
}
```

完整切面最终版本如下：

文件位置：`src/main/java/io/github/atengk/common/aspect/OperationLogAspect.java`

```java
package io.github.atengk.common.aspect;

import cn.hutool.core.date.StopWatch;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.common.annotation.OperationLog;
import io.github.atengk.common.exception.BusinessException;
import io.github.atengk.common.util.OperationLogUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

/**
 * 操作日志切面
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Aspect
@Component
public class OperationLogAspect {

    /**
     * 记录操作日志
     *
     * @param joinPoint    切点
     * @param operationLog 操作日志注解
     * @return 方法返回值
     * @throws Throwable 方法执行异常
     */
    @Around("@annotation(operationLog)")
    public Object around(ProceedingJoinPoint joinPoint, OperationLog operationLog) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String methodFullName = StrUtil.format("{}.{}",
                signature.getDeclaringTypeName(), signature.getName());

        String argsText = operationLog.recordArgs()
                ? OperationLogUtil.argsToLogText(joinPoint.getArgs())
                : "";

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        try {
            log.info("操作开始，module={}, operation={}, method={}, args={}",
                    operationLog.module(), operationLog.operation(), methodFullName, argsText);

            Object result = joinPoint.proceed();

            stopWatch.stop();
            long cost = stopWatch.getTotalTimeMillis();

            if (cost >= operationLog.slowThresholdMs()) {
                log.warn("操作耗时较长，module={}, operation={}, method={}, cost={}ms, threshold={}ms",
                        operationLog.module(), operationLog.operation(), methodFullName,
                        cost, operationLog.slowThresholdMs());
            }

            if (operationLog.recordResult()) {
                log.info("操作完成，module={}, operation={}, method={}, cost={}ms, result={}",
                        operationLog.module(), operationLog.operation(), methodFullName,
                        cost, OperationLogUtil.resultToLogText(result));
            } else {
                log.info("操作完成，module={}, operation={}, method={}, cost={}ms",
                        operationLog.module(), operationLog.operation(), methodFullName, cost);
            }

            return result;
        } catch (BusinessException e) {
            stopWatch.stop();
            log.warn("操作业务异常，module={}, operation={}, method={}, cost={}ms, args={}, message={}",
                    operationLog.module(), operationLog.operation(), methodFullName,
                    stopWatch.getTotalTimeMillis(), argsText, e.getMessage());
            throw e;
        } catch (Throwable e) {
            stopWatch.stop();
            log.error("操作系统异常，module={}, operation={}, method={}, cost={}ms, args={}",
                    operationLog.module(), operationLog.operation(), methodFullName,
                    stopWatch.getTotalTimeMillis(), argsText, e);
            throw e;
        }
    }
}
```

AOP 日志适合做统一方法级日志，但不能替代业务代码中的关键节点日志。核心业务流程仍然应在 Service 中记录明确的业务状态变化，例如“订单支付成功”“库存扣减完成”“消息发送失败等待补偿”。

## 日志脱敏处理

日志脱敏用于防止敏感信息进入日志文件、控制台、日志采集平台和告警系统。实际项目中，脱敏应优先在日志输出前完成，而不是依赖日志平台后置处理。后置处理只能作为补充，不能作为唯一安全边界。

### 敏感字段识别

敏感字段识别是日志脱敏的前置步骤。项目中应明确哪些字段禁止明文输出，并在工具类、请求日志 Filter、AOP 日志切面和业务日志中统一处理。

常见敏感字段如下：

| 类型     | 字段示例                                                | 日志处理建议                       |
| -------- | ------------------------------------------------------- | ---------------------------------- |
| 账号凭证 | `password`、`oldPassword`、`newPassword`                | 禁止明文输出，统一替换为 `******`  |
| Token    | `token`、`accessToken`、`refreshToken`、`Authorization` | 禁止完整输出，只允许摘要           |
| 手机号   | `mobile`、`phone`                                       | 中间四位脱敏                       |
| 身份证号 | `idCard`、`identityNo`                                  | 保留前后少量字符                   |
| 银行卡号 | `bankCard`、`cardNo`                                    | 保留后四位                         |
| 密钥     | `secret`、`privateKey`、`appSecret`                     | 禁止明文输出                       |
| 个人地址 | `address`、`detailAddress`                              | 按业务需要截断或不输出             |
| 支付信息 | `payNo`、`transactionId`、`bankAccount`                 | 支付流水可输出，账户类字段必须脱敏 |

推荐将敏感字段集中维护，避免每个业务类单独处理。

文件位置：`src/main/java/io/github/atengk/common/util/LogDesensitizeUtil.java`

```java
package io.github.atengk.common.util;

import cn.hutool.core.util.DesensitizedUtil;
import cn.hutool.core.util.StrUtil;

import java.util.Set;

/**
 * 日志脱敏工具类
 *
 * @author Ateng
 * @since 2026-05-06
 */
public class LogDesensitizeUtil {

    private static final Set<String> SENSITIVE_FIELD_SET = Set.of(
            "password",
            "oldPassword",
            "newPassword",
            "token",
            "accessToken",
            "refreshToken",
            "authorization",
            "secret",
            "appSecret",
            "privateKey",
            "mobile",
            "phone",
            "idCard",
            "identityNo",
            "bankCard",
            "cardNo"
    );

    /**
     * 判断是否为敏感字段
     *
     * @param fieldName 字段名
     * @return 是否敏感
     */
    public static boolean isSensitiveField(String fieldName) {
        if (StrUtil.isBlank(fieldName)) {
            return false;
        }
        return SENSITIVE_FIELD_SET.contains(StrUtil.lowerFirst(fieldName));
    }

    /**
     * 手机号脱敏
     *
     * @param mobile 手机号
     * @return 脱敏手机号
     */
    public static String mobile(String mobile) {
        if (StrUtil.isBlank(mobile)) {
            return "";
        }
        return DesensitizedUtil.mobilePhone(mobile);
    }

    /**
     * 身份证号脱敏
     *
     * @param idCard 身份证号
     * @return 脱敏身份证号
     */
    public static String idCard(String idCard) {
        if (StrUtil.isBlank(idCard)) {
            return "";
        }
        return DesensitizedUtil.idCardNum(idCard, 4, 4);
    }

    /**
     * Token 脱敏
     *
     * @param token Token
     * @return 脱敏 Token
     */
    public static String token(String token) {
        if (StrUtil.isBlank(token)) {
            return "";
        }

        int length = token.length();
        if (length <= 10) {
            return "******";
        }

        return StrUtil.subPre(token, 6) + "******" + StrUtil.subSuf(token, length - 4);
    }

    /**
     * 密码脱敏
     *
     * @param password 密码
     * @return 脱敏密码
     */
    public static String password(String password) {
        if (StrUtil.isBlank(password)) {
            return "";
        }
        return "******";
    }

    /**
     * 银行卡号脱敏
     *
     * @param bankCard 银行卡号
     * @return 脱敏银行卡号
     */
    public static String bankCard(String bankCard) {
        if (StrUtil.isBlank(bankCard)) {
            return "";
        }
        return DesensitizedUtil.bankCard(bankCard);
    }

    /**
     * JSON 文本敏感字段脱敏
     *
     * @param jsonText JSON 文本
     * @return 脱敏后的 JSON 文本
     */
    public static String maskJsonText(String jsonText) {
        if (StrUtil.isBlank(jsonText)) {
            return "";
        }

        String result = jsonText;
        result = maskJsonField(result, "password");
        result = maskJsonField(result, "oldPassword");
        result = maskJsonField(result, "newPassword");
        result = maskJsonField(result, "token");
        result = maskJsonField(result, "accessToken");
        result = maskJsonField(result, "refreshToken");
        result = maskJsonField(result, "authorization");
        result = maskJsonField(result, "secret");
        result = maskJsonField(result, "appSecret");
        result = maskJsonField(result, "privateKey");
        result = maskJsonField(result, "mobile");
        result = maskJsonField(result, "phone");
        result = maskJsonField(result, "idCard");
        result = maskJsonField(result, "identityNo");
        result = maskJsonField(result, "bankCard");
        result = maskJsonField(result, "cardNo");
        return result;
    }

    /**
     * QueryString 敏感字段脱敏
     *
     * @param queryString 请求查询参数
     * @return 脱敏后的查询参数
     */
    public static String maskQueryString(String queryString) {
        if (StrUtil.isBlank(queryString)) {
            return "";
        }

        String result = queryString;
        for (String fieldName : SENSITIVE_FIELD_SET) {
            result = result.replaceAll("(?i)(" + fieldName + "=)([^&\\s]+)", "$1******");
        }
        return result;
    }

    /**
     * 文本截断
     *
     * @param value     原始文本
     * @param maxLength 最大长度
     * @return 截断后的文本
     */
    public static String limit(String value, int maxLength) {
        if (StrUtil.isBlank(value)) {
            return "";
        }
        return StrUtil.maxLength(value, maxLength);
    }

    /**
     * JSON 字段脱敏
     *
     * @param jsonText  JSON 文本
     * @param fieldName 字段名
     * @return 脱敏后的文本
     */
    private static String maskJsonField(String jsonText, String fieldName) {
        return jsonText.replaceAll("(?i)(\"" + fieldName + "\"\\s*:\\s*\")([^\"]*)(\")", "$1******$3");
    }
}
```

该工具类可以用于 Controller、Service、AOP 切面、请求日志 Filter 和第三方接口日志。日志输出前先脱敏，避免敏感数据进入日志链路。

### 手机号脱敏

手机号属于常见个人敏感信息。业务日志中如果必须记录手机号，应统一脱敏，通常保留前三位和后四位。

推荐输出格式：

```text
138****5678
```

业务代码中不要直接输出手机号。

```java
log.info("用户登录成功，userId={}, mobile={}", userId, mobile);
```

应改为：

```java
log.info("用户登录成功，userId={}, mobile={}", userId, LogDesensitizeUtil.mobile(mobile));
```

完整示例：

文件位置：`src/main/java/io/github/atengk/user/service/UserLoginLogService.java`

```java
package io.github.atengk.user.service;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.common.util.LogDesensitizeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 用户登录日志服务
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Service
public class UserLoginLogService {

    /**
     * 记录用户登录成功日志
     *
     * @param userId 用户ID
     * @param mobile 手机号
     */
    public void recordLoginSuccess(Long userId, String mobile) {
        if (userId == null || StrUtil.isBlank(mobile)) {
            log.warn("记录登录日志失败，核心参数为空，userId={}, mobile={}",
                    userId, LogDesensitizeUtil.mobile(mobile));
            return;
        }

        log.info("用户登录成功，userId={}, mobile={}", userId, LogDesensitizeUtil.mobile(mobile));
    }
}
```

手机号脱敏适用于登录、注册、短信发送、用户资料修改、实名认证、客户信息查询等场景。

### 身份证号脱敏

身份证号属于高敏感个人信息，日志中原则上不建议输出。如果确实需要用于排查问题，应只输出脱敏值或哈希摘要，不得输出完整身份证号。

推荐输出格式：

```text
1101**********1234
```

示例代码：

文件位置：`src/main/java/io/github/atengk/user/service/UserRealNameLogService.java`

```java
package io.github.atengk.user.service;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.common.util.LogDesensitizeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 用户实名日志服务
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Service
public class UserRealNameLogService {

    /**
     * 记录实名认证日志
     *
     * @param userId 用户ID
     * @param idCard 身份证号
     */
    public void recordRealNameLog(Long userId, String idCard) {
        if (userId == null || StrUtil.isBlank(idCard)) {
            log.warn("实名认证日志记录失败，核心参数为空，userId={}, idCard={}",
                    userId, LogDesensitizeUtil.idCard(idCard));
            return;
        }

        log.info("实名认证提交成功，userId={}, idCard={}",
                userId, LogDesensitizeUtil.idCard(idCard));
    }
}
```

涉及实名、风控、支付、金融、政务、医疗等场景时，身份证号日志应默认关闭。只有在确有排障需求时，才输出脱敏后的身份证号。

### Token 与密码脱敏

Token、密码、密钥类字段禁止明文进入日志。即使是开发环境，也不建议打印完整 Token 和密码，因为开发环境日志也可能被采集、上传或共享。

密码统一输出为：

```text
******
```

Token 推荐只保留头尾摘要：

```text
eyJhbG******8s9A
```

示例代码：

文件位置：`src/main/java/io/github/atengk/auth/service/AuthLogService.java`

```java
package io.github.atengk.auth.service;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.common.util.LogDesensitizeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 认证日志服务
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Service
public class AuthLogService {

    /**
     * 记录登录请求日志
     *
     * @param username 用户名
     * @param password 密码
     */
    public void recordLoginRequest(String username, String password) {
        if (StrUtil.hasBlank(username, password)) {
            log.warn("登录请求参数不完整，username={}, password={}",
                    username, LogDesensitizeUtil.password(password));
            return;
        }

        log.info("接收登录请求，username={}, password={}",
                username, LogDesensitizeUtil.password(password));
    }

    /**
     * 记录 Token 创建日志
     *
     * @param userId 用户ID
     * @param token  访问 Token
     */
    public void recordTokenCreated(Long userId, String token) {
        log.info("用户 Token 创建成功，userId={}, token={}",
                userId, LogDesensitizeUtil.token(token));
    }

    /**
     * 记录 Token 校验失败日志
     *
     * @param token Token
     */
    public void recordTokenInvalid(String token) {
        log.warn("Token 校验失败，token={}", LogDesensitizeUtil.token(token));
    }
}
```

请求日志 Filter 和 AOP 切面中，如果会记录 JSON 请求体或方法入参，也必须对 `password`、`token`、`authorization` 等字段进行统一脱敏。

示例：

```java
String safeRequestBody = LogDesensitizeUtil.maskJsonText(requestBody);
String safeQueryString = LogDesensitizeUtil.maskQueryString(queryString);
```

### 日志输出安全边界

日志输出安全边界用于明确“哪些内容能输出，哪些内容不能输出”。安全边界应在编码规范中固化，并在 Code Review 中检查。

推荐规则如下：

| 内容                   | 是否允许输出   | 说明                     |
| ---------------------- | -------------- | ------------------------ |
| `userId`               | 允许           | 业务定位常用字段         |
| `orderNo`              | 允许           | 业务定位常用字段         |
| `traceId`              | 允许           | 链路追踪字段             |
| `mobile`               | 脱敏后允许     | 不允许明文               |
| `idCard`               | 脱敏后谨慎输出 | 高敏感字段               |
| `password`             | 禁止明文       | 只能输出 `******`        |
| `token`                | 禁止完整输出   | 只能输出摘要或不输出     |
| `Authorization` 请求头 | 禁止完整输出   | 只能输出摘要             |
| 文件内容               | 禁止输出       | 可输出文件名、大小、类型 |
| 大 JSON                | 谨慎输出       | 必须截断和脱敏           |
| SQL 参数               | 谨慎输出       | 生产环境避免输出敏感参数 |

日志输出前建议按以下顺序处理：

```text
原始数据 -> 敏感字段识别 -> 脱敏 -> 长度截断 -> 日志输出
```

不推荐顺序：

```text
原始数据 -> 日志输出 -> 日志平台脱敏
```

后者的问题是敏感数据已经进入应用日志文件、容器标准输出、采集 Agent、传输链路和日志平台原始索引，风险不可控。

## 日志验证

日志验证用于确认日志配置、日志格式、日志级别、文件输出、异常堆栈、TraceId 和脱敏规则是否符合预期。日志功能完成后，应至少覆盖本地启动、接口调用、文件检查和异常场景四类验证。

### 本地启动验证

本地启动验证用于确认应用可以正常启动，控制台日志格式正确，`logback-spring.xml` 或 `application.yml` 配置已经生效。

启动前确认配置文件存在：

```text
src/main/resources/application.yml
src/main/resources/logback-spring.xml
```

本地启动命令：

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

如果使用 Jar 包启动：

```bash
mvn clean package -DskipTests

java -jar target/springboot-log-demo.jar --spring.profiles.active=dev
```

上述命令分别用于源码方式启动和 Jar 包方式启动。`--spring.profiles.active=dev` 用于指定开发环境，使 `logback-spring.xml` 中的 `<springProfile name="dev">` 配置生效。

控制台应看到类似日志：

```text
2026-05-06 15:20:30.123 INFO  [main] [ ] io.github.atengk.Application - Started Application in 2.356 seconds
```

如果日志格式中配置了 TraceId，启动日志通常没有 TraceId，这是正常现象。TraceId 主要在 HTTP 请求线程中生成。

### 接口调用验证

接口调用验证用于确认 Controller、Service、请求日志 Filter、AOP 切面和脱敏工具是否正常工作。可以新增一个专门用于验证日志的 Controller。

文件位置：`src/main/java/io/github/atengk/log/controller/LogVerifyController.java`

```java
package io.github.atengk.log.controller;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.common.exception.BusinessException;
import io.github.atengk.common.model.ApiResult;
import io.github.atengk.common.util.LogDesensitizeUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 日志验证接口
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@RestController
@RequestMapping("/api/logs/verify")
public class LogVerifyController {

    /**
     * 验证普通业务日志
     *
     * @param orderNo 订单号
     * @return 验证结果
     */
    @GetMapping("/business")
    public ApiResult<String> verifyBusinessLog(@RequestParam String orderNo) {
        log.info("接收业务日志验证请求，orderNo={}", orderNo);

        if (StrUtil.isBlank(orderNo)) {
            log.warn("业务日志验证失败，订单号为空");
            throw new BusinessException("订单号不能为空");
        }

        log.info("业务日志验证完成，orderNo={}", orderNo);
        return ApiResult.success("业务日志验证完成");
    }

    /**
     * 验证脱敏日志
     *
     * @param request 登录验证请求
     * @return 验证结果
     */
    @PostMapping("/desensitize")
    public ApiResult<String> verifyDesensitizeLog(@RequestBody LoginVerifyRequest request) {
        log.info("接收登录日志验证请求，username={}, mobile={}, password={}, token={}",
                request.getUsername(),
                LogDesensitizeUtil.mobile(request.getMobile()),
                LogDesensitizeUtil.password(request.getPassword()),
                LogDesensitizeUtil.token(request.getToken()));

        return ApiResult.success("脱敏日志验证完成");
    }

    /**
     * 验证业务异常日志
     *
     * @return 验证结果
     */
    @GetMapping("/business-exception")
    public ApiResult<String> verifyBusinessException() {
        log.warn("准备触发业务异常验证");
        throw new BusinessException("模拟业务异常");
    }

    /**
     * 验证系统异常日志
     *
     * @return 验证结果
     */
    @GetMapping("/system-exception")
    public ApiResult<String> verifySystemException() {
        log.info("准备触发系统异常验证");
        throw new IllegalStateException("模拟系统异常");
    }

    /**
     * 登录验证请求
     *
     * @author Ateng
     * @since 2026-05-06
     */
    @Data
    public static class LoginVerifyRequest {

        /**
         * 用户名
         */
        private String username;

        /**
         * 手机号
         */
        private String mobile;

        /**
         * 密码
         */
        private String password;

        /**
         * Token
         */
        private String token;
    }
}
```

验证普通业务日志：

```bash
curl -X GET "http://localhost:8080/api/logs/verify/business?orderNo=ORD10001" \
  -H "X-Trace-Id: trace-log-verify-001"
```

验证脱敏日志：

```bash
curl -X POST "http://localhost:8080/api/logs/verify/desensitize" \
  -H "Content-Type: application/json" \
  -H "X-Trace-Id: trace-log-verify-002" \
  -d '{
    "username": "ateng",
    "mobile": "13812345678",
    "password": "123456",
    "token": "eyJhbGciOiJIUzI1NiJ9.demo.token.value"
  }'
```

期望日志中不能出现明文密码、完整手机号和完整 Token。

```text
接收登录日志验证请求，username=ateng, mobile=138****5678, password=******, token=eyJhbG******alue
```

### 日志文件检查

日志文件检查用于确认文件日志是否生成、滚动目录是否正确、日志内容是否符合格式、敏感信息是否已脱敏。

如果前文配置了日志目录：

```yaml
logging:
  file:
    path: /data/logs/springboot-log-demo
```

可以使用以下命令检查日志文件：

```bash
# 查看日志目录
ls -lh /data/logs/springboot-log-demo

# 实时查看应用日志
tail -f /data/logs/springboot-log-demo/springboot-log-demo.log

# 查看最近 100 行日志
tail -n 100 /data/logs/springboot-log-demo/springboot-log-demo.log

# 检查是否存在归档日志
ls -lh /data/logs/springboot-log-demo/archive
```

这些命令分别用于确认日志文件是否存在、实时观察日志输出、查看最近日志内容，以及检查滚动归档目录是否生成。

可以使用 `grep` 检查 TraceId：

```bash
grep "trace-log-verify-001" /data/logs/springboot-log-demo/springboot-log-demo.log
```

可以检查敏感信息是否泄露：

```bash
# 检查是否存在明文密码
grep "123456" /data/logs/springboot-log-demo/springboot-log-demo.log

# 检查是否存在完整手机号
grep "13812345678" /data/logs/springboot-log-demo/springboot-log-demo.log

# 检查是否存在完整 Token
grep "eyJhbGciOiJIUzI1NiJ9.demo.token.value" /data/logs/springboot-log-demo/springboot-log-demo.log
```

以上命令不应查询到结果。如果能查到明文敏感信息，说明请求日志、AOP 日志或业务日志中仍存在未脱敏输出，需要回到对应日志入口修复。

如果本地使用相对目录 `logs`，检查命令调整为：

```bash
tail -f logs/springboot-log-demo.log
grep "trace-log-verify-001" logs/springboot-log-demo.log
```

### 异常场景验证

异常场景验证用于确认业务异常、系统异常、全局异常处理器和异常堆栈记录是否符合预期。

验证业务异常：

```bash
curl -X GET "http://localhost:8080/api/logs/verify/business-exception" \
  -H "X-Trace-Id: trace-business-exception-001"
```

业务异常预期返回：

```json
{
  "code": 400,
  "message": "模拟业务异常",
  "data": null
}
```

业务异常预期日志：

```text
WARN  [http-nio-8080-exec-1] [trace-business-exception-001] ... - 业务异常，method=GET, uri=/api/logs/verify/business-exception, code=400, message=模拟业务异常
```

业务异常通常不需要打印完整堆栈，避免大量可预期异常污染日志。

验证系统异常：

```bash
curl -X GET "http://localhost:8080/api/logs/verify/system-exception" \
  -H "X-Trace-Id: trace-system-exception-001"
```

系统异常预期返回：

```json
{
  "code": 500,
  "message": "系统繁忙，请稍后重试",
  "data": null
}
```

系统异常预期日志：

```text
ERROR [http-nio-8080-exec-1] [trace-system-exception-001] ... - 系统异常，method=GET, uri=/api/logs/verify/system-exception
java.lang.IllegalStateException: 模拟系统异常
    at io.github.atengk.log.controller.LogVerifyController.verifySystemException(...)
```

系统异常必须保留异常堆栈。只记录 `e.getMessage()` 不满足线上排障要求。

建议最终验证清单如下：

| 验证项       | 预期结果                                                 |
| ------------ | -------------------------------------------------------- |
| 应用启动日志 | 控制台正常输出，日志格式正确                             |
| TraceId      | 请求日志、Controller 日志、Service 日志中的 TraceId 一致 |
| 普通业务日志 | 能看到关键业务字段，例如 `orderNo`、`userId`             |
| 手机号脱敏   | 日志中不出现完整手机号                                   |
| 密码脱敏     | 日志中不出现明文密码                                     |
| Token 脱敏   | 日志中不出现完整 Token                                   |
| 业务异常     | 使用 `warn`，返回明确业务提示                            |
| 系统异常     | 使用 `error`，服务端日志保留完整堆栈                     |
| 文件日志     | 日志文件正常生成，路径符合配置                           |
| 滚动日志     | 达到时间或大小条件后能够归档                             |
| 慢请求日志   | 超过阈值时使用 `warn` 标记                               |

日志验证完成后，建议将脱敏检查和异常验证纳入测试用例或发布检查清单，避免后续新增接口时再次引入敏感日志泄露问题。