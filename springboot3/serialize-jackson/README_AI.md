# Spring Boot 序列化与反序列化

Spring Boot 序列化与反序列化主要用于处理 Java 对象与外部数据格式之间的转换。在 Web 接口开发中，最常见的是 Java 对象与 JSON 数据之间的互相转换。Spring Boot 3 默认集成 Jackson，可以自动完成请求参数反序列化和响应结果序列化。

## 模块概述

本模块用于说明序列化与反序列化的基本概念、Spring Boot 3 中的典型使用场景，以及实际开发中常见的数据格式。理解这些内容有助于后续掌握 Jackson 配置、注解使用、自定义转换器和接口数据处理。

### 序列化与反序列化定义

序列化是指将 Java 对象转换为可传输、可存储的数据格式的过程。例如，将 Java 对象转换为 JSON 字符串、XML 文本、字节数组或其他结构化数据。序列化后的数据可以通过 HTTP 接口返回给前端，也可以写入 Redis、消息队列、数据库或本地文件。

反序列化是指将外部数据转换为 Java 对象的过程。例如，前端提交 JSON 请求体后，Spring MVC 可以自动将 JSON 数据转换为 Controller 方法中的 DTO 对象。

在 Spring Boot Web 开发中，最常见的转换关系如下：

```text
Java 对象  ->  JSON 字符串：序列化
JSON 字符串 -> Java 对象：反序列化
```

典型接口处理流程如下：

```text
前端提交 JSON 请求
        ↓
Spring MVC 接收请求体
        ↓
Jackson 反序列化为 Java DTO
        ↓
Controller / Service 处理业务逻辑
        ↓
返回 Java VO / Result 对象
        ↓
Jackson 序列化为 JSON 响应
```

在大多数普通接口中，开发者不需要手动调用 `ObjectMapper`。只要引入 Spring Web 相关依赖，Spring Boot 会自动配置 JSON 消息转换器。

### Spring Boot 3 中的应用场景

Spring Boot 3 中，序列化与反序列化广泛应用于接口开发、缓存处理、消息通信、日志输出和第三方系统对接等场景。

常见应用场景如下：

| 应用场景       | 说明                                                    |
| -------------- | ------------------------------------------------------- |
| 请求体参数接收 | 使用 `@RequestBody` 将 JSON 请求体反序列化为 Java DTO   |
| 接口响应返回   | 使用 `@RestController` 将 Java 对象序列化为 JSON        |
| Redis 缓存     | 将对象序列化后写入 Redis，读取时再反序列化              |
| 消息队列       | 将业务对象转换为 JSON 后发送到 RabbitMQ、Kafka 等中间件 |
| 第三方接口调用 | 将请求对象序列化为 JSON 后调用外部 HTTP 接口            |
| 日志排查       | 将对象转换为 JSON 字符串输出，便于定位问题              |
| 配置绑定       | 将 `application.yml` 中的配置绑定为 Java 配置对象       |
| 文件导入导出   | 将对象数据转换为 JSON、CSV、Excel 等格式                |

在 Controller 中，常见写法如下：

```java
@PostMapping("/users")
public Result<UserVO> createUser(@RequestBody UserCreateDTO dto) {
    return userService.createUser(dto);
}
```

其中，`@RequestBody` 负责触发请求体反序列化，`Result<UserVO>` 返回值会被 Spring MVC 自动序列化为 JSON 响应。

### 常见数据格式

实际开发中，序列化与反序列化并不只针对 JSON。不同系统、协议和中间件可能使用不同的数据格式。

常见数据格式如下：

| 数据格式          | 常见用途                              | 特点                                           |
| ----------------- | ------------------------------------- | ---------------------------------------------- |
| JSON              | Web 接口、前后端交互、日志、Redis、MQ | 可读性强，使用最广泛，Spring Boot 默认支持良好 |
| XML               | 老系统接口、部分开放平台、配置文件    | 结构严格，但标签较多，数据体积偏大             |
| YAML              | Spring Boot 配置、Kubernetes 配置     | 层级清晰，适合配置文件                         |
| Properties        | Spring Boot 配置                      | 简单直接，适合扁平配置                         |
| CSV               | 数据导入导出、报表文件                | 适合表格型数据，结构简单                       |
| Binary            | RPC、文件存储、高性能通信             | 体积小，性能较高，但可读性差                   |
| Protocol Buffers  | 微服务通信、跨语言传输                | 性能高，结构强约束，需要维护 schema            |
| Java Serializable | Java 原生对象序列化                   | 与 Java 绑定较强，跨语言能力较弱               |

对于 Spring Boot 后端接口开发，JSON 是最常用的数据格式。默认情况下，只要引入 `spring-boot-starter-web`，Spring Boot 就会自动引入 Jackson，并使用 Jackson 作为默认 JSON 序列化与反序列化工具。

## 基础环境准备

本节用于准备 Spring Boot 3 序列化与反序列化示例所需的基础项目结构、依赖配置和 Jackson 默认环境。后续章节中的 JSON 转换、注解使用、自定义序列化器和全局配置都可以基于该环境继续扩展。

### Spring Boot 3 项目结构

Spring Boot 3 推荐使用 Java 17 或更高版本。下面给出一个用于演示序列化与反序列化的基础项目结构。

```text
spring-boot-serialize-demo
├── pom.xml
├── src
│   ├── main
│   │   ├── java
│   │   │   └── io
│   │   │       └── github
│   │   │           └── atengk
│   │   │               ├── SerializeApplication.java
│   │   │               ├── controller
│   │   │               │   └── UserController.java
│   │   │               ├── dto
│   │   │               │   └── UserCreateDTO.java
│   │   │               ├── vo
│   │   │               │   └── UserVO.java
│   │   │               └── common
│   │   │                   └── Result.java
│   │   └── resources
│   │       └── application.yml
│   └── test
│       └── java
│           └── io
│               └── github
│                   └── atengk
│                       └── SerializeApplicationTests.java
```

各目录职责如下：

| 路径                        | 说明                                           |
| --------------------------- | ---------------------------------------------- |
| `controller`                | 提供接口入口，用于验证请求反序列化和响应序列化 |
| `dto`                       | 定义请求数据传输对象，用于接收前端提交的数据   |
| `vo`                        | 定义响应视图对象，用于返回给前端               |
| `common`                    | 定义通用返回结果、异常结构或工具类             |
| `resources/application.yml` | 定义 Spring Boot 和 Jackson 基础配置           |
| `pom.xml`                   | 定义项目依赖和构建配置                         |

基础启动类如下。

文件位置：`src/main/java/io/github/atengk/SerializeApplication.java`

```java
package io.github.atengk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 序列化与反序列化示例项目启动类
 *
 * @author Ateng
 * @since 2026-05-06
 */
@SpringBootApplication
public class SerializeApplication {

    /**
     * 项目启动入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(SerializeApplication.class, args);
    }

}
```

### Jackson 默认集成

Spring Boot 3 引入 `spring-boot-starter-web` 后，会默认集成 Jackson。Jackson 是 Spring Boot Web 场景下默认的 JSON 序列化与反序列化框架，主要负责 Java 对象和 JSON 数据之间的转换。

默认依赖关系如下：

```text
spring-boot-starter-web
    └── spring-boot-starter-json
        ├── jackson-databind
        ├── jackson-core
        ├── jackson-annotations
        └── jackson-datatype-jsr310
```

核心依赖说明如下：

| 依赖                      | 说明                                                     |
| ------------------------- | -------------------------------------------------------- |
| `jackson-databind`        | 提供 Java 对象和 JSON 数据之间的绑定转换能力             |
| `jackson-core`            | 提供 JSON 解析和生成的底层能力                           |
| `jackson-annotations`     | 提供 Jackson 注解支持                                    |
| `jackson-datatype-jsr310` | 支持 `LocalDate`、`LocalDateTime` 等 Java 8 日期时间类型 |

在 Spring MVC 中，Jackson 通常通过 `MappingJackson2HttpMessageConverter` 生效。该转换器会在以下场景自动工作：

| 场景                 | 触发方式                              |
| -------------------- | ------------------------------------- |
| 请求体转 Java 对象   | Controller 参数使用 `@RequestBody`    |
| Java 对象转响应 JSON | Controller 使用 `@RestController`     |
| 方法返回值转 JSON    | 方法使用 `@ResponseBody`              |
| JSON 媒体类型匹配    | 请求头或响应头使用 `application/json` |

下面示例用于验证 Jackson 默认反序列化与序列化能力。

文件位置：`src/main/java/io/github/atengk/controller/UserController.java`

```java
package io.github.atengk.controller;

import cn.hutool.core.date.LocalDateTimeUtil;
import io.github.atengk.common.Result;
import io.github.atengk.dto.UserCreateDTO;
import io.github.atengk.vo.UserVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 用户接口示例
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@RestController
@RequestMapping("/api/users")
public class UserController {

    /**
     * 创建用户
     *
     * @param dto 用户创建请求参数
     * @return 用户响应结果
     */
    @PostMapping
    public Result<UserVO> createUser(@RequestBody UserCreateDTO dto) {
        log.info("接收到创建用户请求，用户名：{}", dto.getUsername());

        UserVO userVO = new UserVO();
        userVO.setId(10001L);
        userVO.setUsername(dto.getUsername());
        userVO.setNickname(dto.getNickname());
        userVO.setCreateTime(LocalDateTime.now());
        userVO.setCreateTimeText(LocalDateTimeUtil.formatNormal(userVO.getCreateTime()));

        return Result.success(userVO);
    }

    /**
     * 查询用户
     *
     * @param id 用户ID
     * @return 用户响应结果
     */
    @GetMapping("/{id}")
    public Result<UserVO> getUser(@PathVariable Long id) {
        log.info("查询用户信息，用户ID：{}", id);

        UserVO userVO = new UserVO();
        userVO.setId(id);
        userVO.setUsername("admin");
        userVO.setNickname("管理员");
        userVO.setCreateTime(LocalDateTime.now());
        userVO.setCreateTimeText(LocalDateTimeUtil.formatNormal(userVO.getCreateTime()));

        return Result.success(userVO);
    }

}
```

请求 DTO 示例。

文件位置：`src/main/java/io/github/atengk/dto/UserCreateDTO.java`

```java
package io.github.atengk.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户创建请求参数
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
public class UserCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户名
     */
    private String username;

    /**
     * 昵称
     */
    private String nickname;

}
```

响应 VO 示例。

文件位置：`src/main/java/io/github/atengk/vo/UserVO.java`

```java
package io.github.atengk.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户响应结果
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
public class UserVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 创建时间文本
     */
    private String createTimeText;

}
```

通用返回结果示例。

文件位置：`src/main/java/io/github/atengk/common/Result.java`

```java
package io.github.atengk.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 通用接口响应结果
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 状态码
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
     * @param <T>  数据类型
     * @return 通用响应结果
     */
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage("操作成功");
        result.setData(data);
        return result;
    }

    /**
     * 失败响应
     *
     * @param code    状态码
     * @param message 响应消息
     * @param <T>     数据类型
     * @return 通用响应结果
     */
    public static <T> Result<T> fail(Integer code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }

}
```

### 常用依赖配置

Spring Boot 3 Web 项目中，如果只使用 JSON 序列化与反序列化，通常引入 `spring-boot-starter-web` 即可。该依赖会自动引入 Jackson 相关组件。实际项目中通常还会配合 Lombok、Hutool 和测试依赖使用。

文件位置：`pom.xml`

```xml
<dependencies>
    <!-- Spring Web：提供 Spring MVC、内置 Web 容器和 JSON 数据转换能力 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Jackson JSR310：支持 LocalDate、LocalDateTime 等 Java 8 日期时间类型 -->
    <dependency>
        <groupId>com.fasterxml.jackson.datatype</groupId>
        <artifactId>jackson-datatype-jsr310</artifactId>
    </dependency>

    <!-- Hutool：常用 Java 工具类库，用于日期、字符串、集合等辅助处理 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.8.35</version>
    </dependency>

    <!-- Lombok：减少 Getter、Setter、构造方法等样板代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- Spring Boot Test：提供接口测试、JSON 断言和 MockMvc 支持 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

如果需要完整 Maven 配置，可以使用以下结构。

文件位置：`pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">

    <modelVersion>4.0.0</modelVersion>

    <!-- Spring Boot 父工程：统一管理 Spring Boot 相关依赖版本 -->
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.3.5</version>
        <relativePath/>
    </parent>

    <groupId>io.github.atengk</groupId>
    <artifactId>spring-boot-serialize-demo</artifactId>
    <version>1.0.0</version>
    <name>spring-boot-serialize-demo</name>
    <description>Spring Boot 序列化与反序列化示例项目</description>

    <properties>
        <!-- Spring Boot 3 推荐使用 Java 17 或更高版本 -->
        <java.version>17</java.version>

        <!-- Hutool 工具类版本 -->
        <hutool.version>5.8.35</hutool.version>
    </properties>

    <dependencies>
        <!-- Web 开发基础依赖，默认集成 Jackson JSON 转换能力 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- 支持 LocalDate、LocalDateTime 等 Java 8 日期时间类型 -->
        <dependency>
            <groupId>com.fasterxml.jackson.datatype</groupId>
            <artifactId>jackson-datatype-jsr310</artifactId>
        </dependency>

        <!-- 常用工具类库，用于日期、字符串、集合等辅助处理 -->
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
            <version>${hutool.version}</version>
        </dependency>

        <!-- 简化 Java Bean、DTO、VO 的样板代码 -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- 测试依赖，用于接口测试和 JSON 结果断言 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <!-- Spring Boot Maven 插件：用于项目打包和运行 -->
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

Jackson 基础配置可以先放在 `application.yml` 中，后续如果需要更精细的日期格式、枚举转换、Long 精度处理，可以再通过全局 `ObjectMapper` 配置类扩展。

文件位置：`src/main/resources/application.yml`

```yaml
server:
  # 服务端口
  port: 8080

spring:
  application:
    # 应用名称
    name: spring-boot-serialize-demo

  jackson:
    # 默认时区，避免日期时间序列化后出现时区偏移
    time-zone: GMT+8

    # 日期格式，主要影响 java.util.Date 类型
    date-format: yyyy-MM-dd HH:mm:ss

    serialization:
      # 禁止将日期序列化为时间戳
      write-dates-as-timestamps: false

    deserialization:
      # 反序列化时遇到未知字段不报错，便于接口兼容
      fail-on-unknown-properties: false
```

完成依赖和配置后，可以启动项目并使用以下命令验证接口。

```bash
# 创建用户：验证 JSON 请求体反序列化为 UserCreateDTO
curl -X POST "http://localhost:8080/api/users" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","nickname":"管理员"}'

# 查询用户：验证 UserVO 序列化为 JSON 响应
curl -X GET "http://localhost:8080/api/users/10001"
```

预期响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 10001,
    "username": "admin",
    "nickname": "管理员",
    "createTime": "2026-05-06T10:30:00",
    "createTimeText": "2026-05-06 10:30:00"
  }
}
```

这里需要注意，`spring.jackson.date-format` 对 `java.util.Date` 类型更直接；对于 `LocalDateTime`、`LocalDate` 等 Java 8 日期时间类型，建议在后续章节中通过 `@JsonFormat` 或全局 `ObjectMapper` 配置统一处理，避免不同接口返回的日期格式不一致。



## JSON 序列化与反序列化

本节主要说明 Java 对象、JSON 字符串、集合、泛型和日期时间类型之间的转换方式。在 Spring Boot 接口开发中，大部分 JSON 转换由 Spring MVC 自动完成；在缓存、日志、消息队列、第三方接口调用等场景中，则通常需要手动使用 `ObjectMapper` 完成转换。

### 对象转 JSON

对象转 JSON 是指将 Java 对象序列化为 JSON 字符串。常见场景包括接口响应、日志输出、Redis 缓存、MQ 消息发送和第三方接口请求参数构造。

在 Spring Boot 项目中，建议优先注入 Spring 容器中的 `ObjectMapper`，不要在业务代码中频繁使用 `new ObjectMapper()`。Spring Boot 自动配置的 `ObjectMapper` 会继承全局 Jackson 配置，例如日期格式、空值处理、字段命名策略等。

示例文件结构如下：

```text
src/main/java/io/github/atengk/dto/UserJsonDTO.java
src/main/java/io/github/atengk/service/JsonSerializeService.java
```

用户 JSON 示例对象如下。

文件位置：`src/main/java/io/github/atengk/dto/UserJsonDTO.java`

```java
package io.github.atengk.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户 JSON 转换示例对象
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
public class UserJsonDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 启用状态
     */
    private Boolean enabled;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

}
```

下面的服务类演示对象转 JSON、对象转格式化 JSON，以及异常处理方式。

文件位置：`src/main/java/io/github/atengk/service/JsonSerializeService.java`

```java
package io.github.atengk.service;

import cn.hutool.core.date.LocalDateTimeUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.atengk.dto.UserJsonDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * JSON 序列化示例服务
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JsonSerializeService {

    private final ObjectMapper objectMapper;

    /**
     * 将用户对象转换为 JSON 字符串
     *
     * @return JSON 字符串
     */
    public String objectToJson() {
        UserJsonDTO user = buildUser();

        try {
            String json = objectMapper.writeValueAsString(user);
            log.info("用户对象序列化成功，用户ID：{}", user.getId());
            return json;
        } catch (JsonProcessingException e) {
            log.error("用户对象序列化失败，用户ID：{}", user.getId(), e);
            throw new IllegalArgumentException("用户对象序列化失败", e);
        }
    }

    /**
     * 将用户对象转换为格式化 JSON 字符串
     *
     * @return 格式化 JSON 字符串
     */
    public String objectToPrettyJson() {
        UserJsonDTO user = buildUser();

        try {
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(user);
            log.info("用户对象格式化序列化成功，用户ID：{}", user.getId());
            return json;
        } catch (JsonProcessingException e) {
            log.error("用户对象格式化序列化失败，用户ID：{}", user.getId(), e);
            throw new IllegalArgumentException("用户对象格式化序列化失败", e);
        }
    }

    /**
     * 构造用户示例对象
     *
     * @return 用户对象
     */
    private UserJsonDTO buildUser() {
        UserJsonDTO user = new UserJsonDTO();
        user.setId(10001L);
        user.setUsername("admin");
        user.setNickname("管理员");
        user.setPhone("13800000000");
        user.setEnabled(Boolean.TRUE);
        user.setCreateTime(LocalDateTimeUtil.parse("2026-05-06 10:30:00", "yyyy-MM-dd HH:mm:ss"));
        return user;
    }

}
```

对象转 JSON 的输出示例如下：

```json
{
  "id": 10001,
  "username": "admin",
  "nickname": "管理员",
  "phone": "13800000000",
  "enabled": true,
  "createTime": "2026-05-06 10:30:00"
}
```

在 Controller 返回对象时，Spring Boot 会自动执行同样的序列化过程。例如：

```java
@GetMapping("/users/{id}")
public UserJsonDTO getUser(@PathVariable Long id) {
    UserJsonDTO user = new UserJsonDTO();
    user.setId(id);
    user.setUsername("admin");
    user.setNickname("管理员");
    user.setEnabled(Boolean.TRUE);
    user.setCreateTime(LocalDateTime.now());
    return user;
}
```

该接口返回时，`UserJsonDTO` 会自动被 Jackson 转换为 JSON 响应，不需要开发者手动调用 `writeValueAsString()`。

### JSON 转对象

JSON 转对象是指将 JSON 字符串反序列化为 Java 对象。常见场景包括读取 Redis 缓存、消费 MQ 消息、解析第三方接口响应、处理本地 JSON 文件等。

在 Controller 中，`@RequestBody` 会自动完成 JSON 到 Java 对象的转换；在普通业务代码中，可以使用 `ObjectMapper#readValue()` 手动转换。

下面的代码演示 JSON 字符串转 Java 对象，并在转换前使用 Hutool 的 `StrUtil` 做基础校验。

文件位置：`src/main/java/io/github/atengk/service/JsonDeserializeService.java`

```java
package io.github.atengk.service;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.atengk.dto.UserJsonDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * JSON 反序列化示例服务
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JsonDeserializeService {

    private final ObjectMapper objectMapper;

    /**
     * 将 JSON 字符串转换为用户对象
     *
     * @param json JSON 字符串
     * @return 用户对象
     */
    public UserJsonDTO jsonToObject(String json) {
        if (StrUtil.isBlank(json)) {
            log.warn("JSON 反序列化失败，请求内容为空");
            throw new IllegalArgumentException("JSON 字符串不能为空");
        }

        try {
            UserJsonDTO user = objectMapper.readValue(json, UserJsonDTO.class);
            log.info("JSON 反序列化成功，用户名：{}", user.getUsername());
            return user;
        } catch (JsonProcessingException e) {
            log.error("JSON 反序列化失败，原始内容：{}", json, e);
            throw new IllegalArgumentException("JSON 反序列化失败", e);
        }
    }

}
```

测试输入示例：

```json
{
  "id": 10001,
  "username": "admin",
  "nickname": "管理员",
  "phone": "13800000000",
  "enabled": true,
  "createTime": "2026-05-06 10:30:00"
}
```

在接口请求中，反序列化通常由 Spring MVC 自动完成：

```java
@PostMapping("/users")
public UserJsonDTO createUser(@RequestBody UserJsonDTO dto) {
    log.info("接收到用户创建请求，用户名：{}", dto.getUsername());
    return dto;
}
```

需要注意，JSON 字段名称默认需要与 Java 属性名称一致。如果前端传入字段名称与后端字段不一致，可以通过字段命名策略或 `@JsonProperty` 注解处理。

### 集合与泛型处理

集合和泛型反序列化是 Jackson 使用中比较容易出错的场景。由于 Java 泛型存在类型擦除，不能直接使用 `List.class`、`Map.class` 获取准确的元素类型，否则转换后可能得到 `LinkedHashMap`，而不是目标 DTO 对象。

对于集合类型，推荐使用 `TypeReference` 或 `JavaType`。

下面示例演示 `List<UserJsonDTO>` 和 `Map<String, UserJsonDTO>` 的序列化与反序列化。

文件位置：`src/main/java/io/github/atengk/service/JsonGenericService.java`

```java
package io.github.atengk.service;

import cn.hutool.core.collection.CollUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.atengk.dto.UserJsonDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JSON 集合与泛型转换示例服务
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JsonGenericService {

    private final ObjectMapper objectMapper;

    /**
     * 将用户集合转换为 JSON 字符串
     *
     * @return JSON 字符串
     */
    public String listToJson() {
        List<UserJsonDTO> userList = buildUserList();

        try {
            String json = objectMapper.writeValueAsString(userList);
            log.info("用户集合序列化成功，数量：{}", CollUtil.size(userList));
            return json;
        } catch (JsonProcessingException e) {
            log.error("用户集合序列化失败", e);
            throw new IllegalArgumentException("用户集合序列化失败", e);
        }
    }

    /**
     * 将 JSON 字符串转换为用户集合
     *
     * @param json JSON 字符串
     * @return 用户集合
     */
    public List<UserJsonDTO> jsonToList(String json) {
        try {
            List<UserJsonDTO> userList = objectMapper.readValue(json, new TypeReference<List<UserJsonDTO>>() {
            });
            log.info("用户集合反序列化成功，数量：{}", CollUtil.size(userList));
            return userList;
        } catch (JsonProcessingException e) {
            log.error("用户集合反序列化失败，原始内容：{}", json, e);
            throw new IllegalArgumentException("用户集合反序列化失败", e);
        }
    }

    /**
     * 将 JSON 字符串转换为用户 Map
     *
     * @param json JSON 字符串
     * @return 用户 Map
     */
    public Map<String, UserJsonDTO> jsonToMap(String json) {
        try {
            Map<String, UserJsonDTO> userMap = objectMapper.readValue(json, new TypeReference<Map<String, UserJsonDTO>>() {
            });
            log.info("用户 Map 反序列化成功，数量：{}", CollUtil.size(userMap));
            return userMap;
        } catch (JsonProcessingException e) {
            log.error("用户 Map 反序列化失败，原始内容：{}", json, e);
            throw new IllegalArgumentException("用户 Map 反序列化失败", e);
        }
    }

    /**
     * 构造用户集合
     *
     * @return 用户集合
     */
    private List<UserJsonDTO> buildUserList() {
        UserJsonDTO admin = new UserJsonDTO();
        admin.setId(10001L);
        admin.setUsername("admin");
        admin.setNickname("管理员");
        admin.setEnabled(Boolean.TRUE);
        admin.setCreateTime(LocalDateTime.now());

        UserJsonDTO test = new UserJsonDTO();
        test.setId(10002L);
        test.setUsername("test");
        test.setNickname("测试用户");
        test.setEnabled(Boolean.TRUE);
        test.setCreateTime(LocalDateTime.now());

        return List.of(admin, test);
    }

    /**
     * 构造用户 Map
     *
     * @return 用户 Map
     */
    private Map<String, UserJsonDTO> buildUserMap() {
        List<UserJsonDTO> userList = buildUserList();
        Map<String, UserJsonDTO> userMap = new LinkedHashMap<>();
        userMap.put("admin", userList.get(0));
        userMap.put("test", userList.get(1));
        return userMap;
    }

}
```

集合 JSON 示例：

```json
[
  {
    "id": 10001,
    "username": "admin",
    "nickname": "管理员",
    "enabled": true,
    "createTime": "2026-05-06 10:30:00"
  },
  {
    "id": 10002,
    "username": "test",
    "nickname": "测试用户",
    "enabled": true,
    "createTime": "2026-05-06 10:35:00"
  }
]
```

泛型处理时需要注意以下几点：

| 写法                                                         | 是否推荐 | 说明                                             |
| ------------------------------------------------------------ | -------- | ------------------------------------------------ |
| `objectMapper.readValue(json, List.class)`                   | 不推荐   | 会丢失元素类型，集合元素可能变成 `LinkedHashMap` |
| `new TypeReference<List<UserJsonDTO>>() {}`                  | 推荐     | 能保留完整泛型信息                               |
| `objectMapper.getTypeFactory().constructCollectionType(...)` | 推荐     | 适合动态构造类型                                 |
| `new TypeReference<Map<String, UserJsonDTO>>() {}`           | 推荐     | 适合 Map 泛型转换                                |

当泛型类型需要动态传入时，可以使用 `JavaType`：

```java
JavaType javaType = objectMapper.getTypeFactory()
        .constructCollectionType(List.class, UserJsonDTO.class);

List<UserJsonDTO> userList = objectMapper.readValue(json, javaType);
```

### 日期时间类型处理

日期时间类型是 JSON 转换中最常见的问题之一。Spring Boot 3 默认支持 `LocalDate`、`LocalDateTime` 等 Java 8 日期时间类型，但如果不做统一配置，不同接口可能出现格式不一致的问题。

常见日期时间类型如下：

| Java 类型       | 常见 JSON 格式         |
| --------------- | ---------------------- |
| `LocalDateTime` | `2026-05-06 10:30:00`  |
| `LocalDate`     | `2026-05-06`           |
| `LocalTime`     | `10:30:00`             |
| `Date`          | `2026-05-06 10:30:00`  |
| `Instant`       | `2026-05-06T01:30:00Z` |

单个字段可以使用 `@JsonFormat` 指定格式。

文件位置：`src/main/java/io/github/atengk/dto/UserTimeDTO.java`

```java
package io.github.atengk.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户日期时间示例对象
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
public class UserTimeDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 生日
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthday;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

}
```

如果项目中所有接口都需要统一日期格式，推荐通过全局 Jackson 配置实现，而不是在每个字段上重复添加 `@JsonFormat`。

## Jackson 常用配置

本节主要说明 Spring Boot 3 中 Jackson 的常用配置方式，包括 `ObjectMapper` 全局配置、字段命名策略、空值处理和未知字段处理。项目中建议优先使用 Spring Boot 提供的自动配置和扩展点，避免直接覆盖默认 `ObjectMapper` 导致部分自动配置失效。

### ObjectMapper 配置

`ObjectMapper` 是 Jackson 的核心对象，负责 Java 对象和 JSON 之间的序列化与反序列化。Spring Boot 会自动创建并管理 `ObjectMapper`，开发者可以通过配置文件或 `Jackson2ObjectMapperBuilderCustomizer` 进行扩展。

推荐做法是使用 `Jackson2ObjectMapperBuilderCustomizer` 统一配置日期格式、时区、空值策略和反序列化行为。

文件位置：`src/main/java/io/github/atengk/config/JacksonConfig.java`

```java
package io.github.atengk.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Jackson 全局配置
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Configuration
public class JacksonConfig {

    /**
     * 日期时间格式
     */
    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    /**
     * 日期格式
     */
    private static final String DATE_PATTERN = "yyyy-MM-dd";

    /**
     * 时间格式
     */
    private static final String TIME_PATTERN = "HH:mm:ss";

    /**
     * 自定义 Jackson 配置
     *
     * @return Jackson 配置定制器
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jackson2ObjectMapperBuilderCustomizer() {
        return builder -> {
            // 序列化时不输出 null 字段
            builder.serializationInclusion(JsonInclude.Include.NON_NULL);

            // 禁止将日期时间序列化为时间戳
            builder.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

            // 反序列化时忽略未知字段
            builder.featuresToDisable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

            // Java 8 日期时间类型序列化格式
            builder.serializerByType(LocalDateTime.class,
                    new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(DATE_TIME_PATTERN)));
            builder.serializerByType(LocalDate.class,
                    new LocalDateSerializer(DateTimeFormatter.ofPattern(DATE_PATTERN)));
            builder.serializerByType(LocalTime.class,
                    new LocalTimeSerializer(DateTimeFormatter.ofPattern(TIME_PATTERN)));
        };
    }

}
```

也可以通过 `application.yml` 做基础配置。

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  jackson:
    # 默认时区
    time-zone: GMT+8

    # java.util.Date 类型默认格式
    date-format: yyyy-MM-dd HH:mm:ss

    serialization:
      # 日期不输出为时间戳
      write-dates-as-timestamps: false

    deserialization:
      # 遇到未知字段不报错
      fail-on-unknown-properties: false

    default-property-inclusion: non_null
```

两种配置方式的使用建议如下：

| 配置方式                                | 适用场景                                                     |
| --------------------------------------- | ------------------------------------------------------------ |
| `application.yml`                       | 简单配置，例如时区、日期格式、未知字段处理                   |
| `Jackson2ObjectMapperBuilderCustomizer` | 复杂配置，例如 Java 8 日期格式、自定义序列化器、统一空值策略 |
| 自定义 `ObjectMapper` Bean              | 不优先推荐，容易覆盖 Spring Boot 默认自动配置                |

一般项目中，推荐使用 `application.yml` 配合 `Jackson2ObjectMapperBuilderCustomizer`。配置文件负责基础项，配置类负责复杂项。

### 字段命名策略

字段命名策略用于处理 Java 属性名和 JSON 字段名之间的转换关系。Java 中通常使用小驼峰命名，例如 `createTime`；前端或数据库接口中可能使用下划线命名，例如 `create_time`。

默认情况下，Jackson 使用 Java 字段原名：

```json
{
  "userId": 10001,
  "userName": "admin",
  "createTime": "2026-05-06 10:30:00"
}
```

如果配置为下划线命名策略，输出结果会变为：

```json
{
  "user_id": 10001,
  "user_name": "admin",
  "create_time": "2026-05-06 10:30:00"
}
```

可以通过配置文件统一设置字段命名策略。

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  jackson:
    # SNAKE_CASE 表示 Java 小驼峰字段与 JSON 下划线字段互相转换
    property-naming-strategy: SNAKE_CASE
```

示例对象如下。

文件位置：`src/main/java/io/github/atengk/dto/UserNamingDTO.java`

```java
package io.github.atengk.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 字段命名策略示例对象
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
public class UserNamingDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名称
     */
    private String userName;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

}
```

如果只需要对单个字段指定 JSON 名称，可以使用 `@JsonProperty`，不需要开启全局命名策略。

```java
@JsonProperty("user_name")
private String userName;
```

字段命名策略使用建议如下：

| 场景                             | 推荐方式                                    |
| -------------------------------- | ------------------------------------------- |
| 整个项目统一使用下划线 JSON 字段 | 使用 `property-naming-strategy: SNAKE_CASE` |
| 只有个别字段需要特殊名称         | 使用 `@JsonProperty`                        |
| 对接第三方接口字段不规范         | 优先使用独立 DTO + `@JsonProperty`          |
| 内部接口前后端统一小驼峰         | 不配置命名策略，使用默认行为                |

全局命名策略会影响所有接口的请求和响应字段，配置前需要与前端明确字段规范，避免上线后出现接口兼容问题。

### 空值与默认值处理

空值处理用于控制对象中的 `null` 字段是否输出到 JSON 中。默认情况下，Jackson 会输出 `null` 字段。

默认输出示例：

```json
{
  "id": 10001,
  "username": "admin",
  "nickname": null,
  "phone": null
}
```

如果配置为忽略 `null` 字段，输出结果会变为：

```json
{
  "id": 10001,
  "username": "admin"
}
```

可以通过配置文件设置全局空值策略。

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  jackson:
    # non_null 表示序列化时不输出 null 字段
    default-property-inclusion: non_null
```

也可以在类或字段上使用 `@JsonInclude` 局部控制。

文件位置：`src/main/java/io/github/atengk/dto/UserNullDTO.java`

```java
package io.github.atengk.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serializable;

/**
 * 空值处理示例对象
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserNullDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 手机号
     */
    private String phone;

}
```

`JsonInclude.Include` 常见选项如下：

| 配置项        | 说明                                    |
| ------------- | --------------------------------------- |
| `ALWAYS`      | 始终输出字段，包括 `null`               |
| `NON_NULL`    | 不输出 `null` 字段                      |
| `NON_EMPTY`   | 不输出 `null`、空字符串、空集合、空数组 |
| `NON_DEFAULT` | 不输出默认值字段，例如 `0`、`false` 等  |
| `NON_ABSENT`  | 不输出空的 `Optional` 等缺失值          |

默认值处理需要和业务语义区分清楚。例如，`enabled = false` 可能是明确的业务值，不应该被当作“无效值”忽略。因此，实际项目中更推荐使用 `NON_NULL`，谨慎使用 `NON_DEFAULT`。

空值策略使用建议如下：

| 场景                           | 推荐配置               |
| ------------------------------ | ---------------------- |
| 普通业务接口                   | `NON_NULL`             |
| 前端需要明确知道字段为空       | `ALWAYS`               |
| 字段较多且空集合不希望返回     | `NON_EMPTY`            |
| 涉及 `false`、`0` 等有效业务值 | 避免使用 `NON_DEFAULT` |
| 第三方接口要求字段完整         | 根据接口协议固定输出   |

如果需要给字段设置默认值，建议在 Java 对象初始化或业务组装阶段处理，而不是依赖 Jackson 自动补默认值。例如：

```java
UserNullDTO user = new UserNullDTO();
user.setId(10001L);
user.setUsername("admin");
user.setNickname("默认昵称");
```

### 未知字段处理

未知字段处理是指 JSON 中存在 Java 对象没有定义的字段时，Jackson 应该忽略还是抛出异常。

例如，后端 DTO 只有 `id` 和 `username`：

```java
private Long id;
private String username;
```

但前端传入了额外字段：

```json
{
  "id": 10001,
  "username": "admin",
  "extraField": "额外字段"
}
```

如果开启未知字段失败策略，反序列化会抛出异常；如果关闭该策略，Jackson 会忽略 `extraField`，只转换 Java 对象中存在的字段。

在实际接口开发中，为了提高前后端接口兼容性，通常建议忽略未知字段。

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  jackson:
    deserialization:
      # false 表示反序列化时遇到未知字段不抛异常
      fail-on-unknown-properties: false
```

也可以在单个 DTO 类上使用 `@JsonIgnoreProperties`。

文件位置：`src/main/java/io/github/atengk/dto/UserUnknownFieldDTO.java`

```java
package io.github.atengk.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.io.Serializable;

/**
 * 未知字段处理示例对象
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserUnknownFieldDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 用户名
     */
    private String username;

}
```

如果需要验证未知字段处理效果，可以使用以下测试 JSON：

```json
{
  "id": 10001,
  "username": "admin",
  "extraField": "该字段在 Java DTO 中不存在"
}
```

当 `ignoreUnknown = true` 或 `fail-on-unknown-properties: false` 生效时，转换后的对象只会保留 `id` 和 `username`。

未知字段处理建议如下：

| 场景                   | 推荐方式                                           |
| ---------------------- | -------------------------------------------------- |
| 普通前后端接口         | 全局配置 `fail-on-unknown-properties: false`       |
| 第三方回调参数可能扩展 | 使用 `@JsonIgnoreProperties(ignoreUnknown = true)` |
| 金融、支付、签名类接口 | 谨慎忽略未知字段，必要时严格校验                   |
| 内部管理接口           | 可忽略未知字段，提高兼容性                         |
| 安全敏感接口           | 建议结合参数校验和白名单字段控制                   |

对于安全敏感场景，例如支付回调、权限变更、用户身份信息修改等，不建议仅依赖 Jackson 忽略未知字段。应结合参数校验、签名校验和业务字段白名单，避免前端或第三方传入未预期字段造成风险。



## 常用注解使用

本节主要说明 Jackson 在 Spring Boot 3 项目中常用的字段控制注解。通过注解可以在不修改全局 `ObjectMapper` 配置的情况下，对单个类或字段进行精细化控制，例如字段格式化、字段忽略、字段重命名和日期格式处理。

### 字段格式化注解

字段格式化注解主要用于控制字段序列化或反序列化时的表现形式。常用注解是 `@JsonFormat`，它可以控制日期格式、数字格式、枚举格式以及字段输出形态。

常见使用场景如下：

| 注解                    | 作用                           |
| ----------------------- | ------------------------------ |
| `@JsonFormat`           | 控制字段序列化和反序列化格式   |
| `@JsonInclude`          | 控制字段为空时是否输出         |
| `@JsonProperty`         | 指定 JSON 字段名称             |
| `@JsonAlias`            | 指定反序列化时可兼容的别名字段 |
| `@JsonIgnore`           | 忽略指定字段                   |
| `@JsonIgnoreProperties` | 忽略类中的指定字段或未知字段   |

下面示例演示字段格式化的常见写法，包括 Long 转字符串、金额转字符串、状态枚举按字符串输出。

文件位置：`src/main/java/io/github/atengk/dto/ProductFormatDTO.java`

```java
package io.github.atengk.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品字段格式化示例对象
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
public class ProductFormatDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 商品ID
     *
     * Long 类型序列化为字符串，避免前端 JavaScript 精度丢失
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    /**
     * 商品名称
     */
    private String productName;

    /**
     * 商品价格
     *
     * BigDecimal 类型序列化为字符串，避免前端浮点精度问题
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal price;

    /**
     * 商品状态
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private ProductStatus status;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 商品状态枚举
     *
     * @author Ateng
     * @since 2026-05-06
     */
    public enum ProductStatus {

        /**
         * 上架
         */
        ENABLED,

        /**
         * 下架
         */
        DISABLED

    }

}
```

序列化结果示例：

```json
{
  "id": "9007199254740993",
  "productName": "机械键盘",
  "price": "399.99",
  "status": "ENABLED",
  "createTime": "2026-05-06 10:30:00"
}
```

字段格式化需要注意以下几点：

| 场景                | 建议                                               |
| ------------------- | -------------------------------------------------- |
| Long 类型返回前端   | 建议序列化为字符串，避免 JS 精度丢失               |
| BigDecimal 金额字段 | 建议序列化为字符串或统一金额格式                   |
| 枚举字段            | 简单场景可使用字符串，复杂场景建议自定义序列化     |
| 日期字段            | 单字段可使用 `@JsonFormat`，全局统一建议使用配置类 |

### 字段忽略注解

字段忽略注解用于控制某些字段不参与 JSON 序列化或反序列化。常见场景包括密码、密钥、身份证号、手机号、内部标识、逻辑删除字段等敏感信息。

常用忽略注解如下：

| 注解                    | 作用                                           |
| ----------------------- | ---------------------------------------------- |
| `@JsonIgnore`           | 忽略单个字段                                   |
| `@JsonIgnoreProperties` | 忽略类中的一个或多个字段                       |
| `@JsonInclude`          | 根据空值、默认值、空集合等条件控制字段是否输出 |

下面示例演示用户对象中密码、密钥和内部字段的忽略处理。

文件位置：`src/main/java/io/github/atengk/dto/UserIgnoreDTO.java`

```java
package io.github.atengk.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 字段忽略示例对象
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(value = {"deleted", "internalRemark"})
public class UserIgnoreDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 登录密码
     */
    @JsonIgnore
    private String password;

    /**
     * 用户密钥
     */
    @JsonIgnore
    private String secretKey;

    /**
     * 逻辑删除状态
     */
    private Boolean deleted;

    /**
     * 内部备注
     */
    private String internalRemark;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

}
```

序列化前 Java 对象中可能包含以下字段：

```json
{
  "id": 10001,
  "username": "admin",
  "password": "123456",
  "secretKey": "AK-TEST-SECRET",
  "deleted": false,
  "internalRemark": "内部测试账号",
  "createTime": "2026-05-06T10:30:00"
}
```

序列化后返回给前端的 JSON 示例：

```json
{
  "id": 10001,
  "username": "admin",
  "createTime": "2026-05-06T10:30:00"
}
```

字段忽略使用建议如下：

| 场景                  | 推荐方式                                          |
| --------------------- | ------------------------------------------------- |
| 单个敏感字段          | 使用 `@JsonIgnore`                                |
| 多个固定字段          | 使用 `@JsonIgnoreProperties`                      |
| 空字段不返回          | 使用 `@JsonInclude(JsonInclude.Include.NON_NULL)` |
| 登录密码、密钥、Token | 必须避免直接序列化返回                            |
| 不同接口返回字段不同  | 建议使用不同 VO，不建议过度依赖忽略注解           |

对于正式项目，敏感字段不建议出现在接口响应 VO 中。更推荐将数据库实体、业务 DTO、接口 VO 分开定义，避免因为复用对象导致敏感字段误返回。

### 自定义字段名称注解

自定义字段名称注解用于处理 Java 字段名和 JSON 字段名不一致的情况。常用注解是 `@JsonProperty` 和 `@JsonAlias`。

`@JsonProperty` 可以同时影响序列化和反序列化。`@JsonAlias` 只影响反序列化，常用于兼容多个前端字段名或第三方接口字段名。

下面示例演示字段重命名和字段别名兼容。

文件位置：`src/main/java/io/github/atengk/dto/UserPropertyDTO.java`

```java
package io.github.atengk.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 自定义字段名称示例对象
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
public class UserPropertyDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    @JsonProperty("user_id")
    private Long userId;

    /**
     * 用户名
     */
    @JsonProperty("user_name")
    @JsonAlias({"username", "account", "login_name"})
    private String userName;

    /**
     * 手机号
     */
    @JsonProperty("phone_number")
    @JsonAlias({"phone", "mobile"})
    private String phoneNumber;

}
```

序列化时，Java 对象会输出为以下 JSON：

```json
{
  "user_id": 10001,
  "user_name": "admin",
  "phone_number": "13800000000"
}
```

反序列化时，下面几种 JSON 都可以正常映射到 `userName` 字段：

```json
{
  "user_id": 10001,
  "user_name": "admin"
}
{
  "user_id": 10001,
  "username": "admin"
}
{
  "user_id": 10001,
  "account": "admin"
}
```

字段名称注解使用建议如下：

| 场景                 | 推荐方式                                 |
| -------------------- | ---------------------------------------- |
| 单个字段名不一致     | 使用 `@JsonProperty`                     |
| 需要兼容历史字段名   | 使用 `@JsonAlias`                        |
| 全项目统一下划线命名 | 使用全局 `SNAKE_CASE` 命名策略           |
| 对接第三方接口       | 使用独立 DTO，并明确标注 `@JsonProperty` |
| 字段命名不稳定       | 不建议直接复用内部业务对象               |

如果项目已经配置了全局字段命名策略，例如 `SNAKE_CASE`，则应谨慎再叠加大量 `@JsonProperty`，避免字段转换规则混乱。

### 日期格式注解

日期格式注解主要用于处理 `LocalDateTime`、`LocalDate`、`LocalTime`、`Date` 等时间字段。常用注解是 `@JsonFormat`。

对于单个字段的特殊日期格式，可以直接使用 `@JsonFormat`；对于全项目统一日期格式，推荐通过全局 `ObjectMapper` 配置处理。

下面示例演示常见日期类型的注解配置。

文件位置：`src/main/java/io/github/atengk/dto/OrderTimeDTO.java`

```java
package io.github.atengk.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;

/**
 * 订单日期格式示例对象
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
public class OrderTimeDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 订单ID
     */
    private Long orderId;

    /**
     * 下单日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate orderDate;

    /**
     * 下单时间
     */
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime orderTime;

    /**
     * 支付时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime payTime;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

}
```

序列化结果示例：

```json
{
  "orderId": 10001,
  "orderDate": "2026-05-06",
  "orderTime": "10:30:00",
  "payTime": "2026-05-06 10:35:00",
  "createTime": "2026-05-06 10:40:00"
}
```

日期格式注解使用建议如下：

| 场景                | 推荐方式                                           |
| ------------------- | -------------------------------------------------- |
| 单个字段格式特殊    | 使用 `@JsonFormat`                                 |
| 全项目统一日期格式  | 使用 Jackson 全局配置                              |
| `Date` 类型涉及时区 | 明确指定 `timezone`                                |
| Java 8 日期时间类型 | 推荐使用 `LocalDateTime`、`LocalDate`、`LocalTime` |
| 接口层时间展示      | 建议统一格式，避免 ISO 格式和普通格式混用          |

需要注意，`@JsonFormat` 同时影响序列化和反序列化。当前端提交的时间字符串格式与注解格式不一致时，反序列化可能失败。

## 自定义序列化与反序列化

本节主要说明如何通过自定义 `JsonSerializer` 和 `JsonDeserializer` 处理特殊字段转换逻辑。常见场景包括金额格式化、枚举编码转换、脱敏输出、Long 精度处理、字符串清洗、第三方接口字段兼容等。

### 自定义 JsonSerializer

自定义 `JsonSerializer` 用于控制 Java 对象字段如何输出为 JSON。相比 `@JsonFormat`，自定义序列化器可以实现更复杂的业务格式处理。

下面示例将 `BigDecimal` 金额序列化为带人民币符号和千分位的字符串，例如 `¥1,234.56`。

文件位置：`src/main/java/io/github/atengk/jackson/serializer/AmountJsonSerializer.java`

```java
package io.github.atengk.jackson.serializer;

import cn.hutool.core.util.NumberUtil;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.math.BigDecimal;

/**
 * 金额 JSON 序列化器
 *
 * @author Ateng
 * @since 2026-05-06
 */
public class AmountJsonSerializer extends JsonSerializer<BigDecimal> {

    /**
     * 将 BigDecimal 金额序列化为人民币金额字符串
     *
     * @param value       金额
     * @param gen         JSON 生成器
     * @param serializers 序列化上下文
     * @throws IOException IO 异常
     */
    @Override
    public void serialize(BigDecimal value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value == null) {
            gen.writeNull();
            return;
        }

        String amountText = NumberUtil.decimalFormat("¥#,##0.00", value);
        gen.writeString(amountText);
    }

}
```

示例字段绑定方式如下：

```java
@JsonSerialize(using = AmountJsonSerializer.class)
private BigDecimal amount;
```

序列化前：

```json
{
  "amount": 1234.56
}
```

序列化后：

```json
{
  "amount": "¥1,234.56"
}
```

自定义序列化器适合处理输出格式复杂的字段。如果只是简单的日期格式或字段名称转换，优先使用 `@JsonFormat`、`@JsonProperty` 等注解即可。

### 自定义 JsonDeserializer

自定义 `JsonDeserializer` 用于控制 JSON 字段如何转换为 Java 对象字段。常见场景包括金额字符串转 `BigDecimal`、枚举编码转枚举对象、日期字符串兼容多种格式、字符串去空格等。

下面示例将 `¥1,234.56`、`1,234.56`、`1234.56` 等金额字符串统一反序列化为 `BigDecimal`。

文件位置：`src/main/java/io/github/atengk/jackson/deserializer/AmountJsonDeserializer.java`

```java
package io.github.atengk.jackson.deserializer;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.math.BigDecimal;

/**
 * 金额 JSON 反序列化器
 *
 * @author Ateng
 * @since 2026-05-06
 */
public class AmountJsonDeserializer extends JsonDeserializer<BigDecimal> {

    /**
     * 将金额字符串反序列化为 BigDecimal
     *
     * @param parser  JSON 解析器
     * @param context 反序列化上下文
     * @return BigDecimal 金额
     * @throws IOException IO 异常
     */
    @Override
    public BigDecimal deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        String value = parser.getValueAsString();

        if (StrUtil.isBlank(value)) {
            return null;
        }

        String cleanValue = StrUtil.removeAll(value, "¥", ",", " ");
        if (!NumberUtil.isNumber(cleanValue)) {
            throw new IllegalArgumentException("金额格式不正确：" + value);
        }

        return NumberUtil.toBigDecimal(cleanValue);
    }

}
```

示例字段绑定方式如下：

```java
@JsonDeserialize(using = AmountJsonDeserializer.class)
private BigDecimal amount;
```

支持的 JSON 输入示例：

```json
{
  "amount": "¥1,234.56"
}
{
  "amount": "1,234.56"
}
{
  "amount": "1234.56"
}
```

反序列化后，`amount` 字段都会转换为：

```text
1234.56
```

自定义反序列化器需要重点处理异常输入。对于金额、枚举、状态码等业务字段，不建议静默吞掉非法值，应在格式异常时抛出明确异常，便于接口调用方及时修正参数。

### 注解方式绑定

注解方式绑定是指在字段上使用 `@JsonSerialize` 和 `@JsonDeserialize` 指定自定义序列化器和反序列化器。该方式适合只对某个字段、某个 DTO 或某类接口生效的场景。

下面示例定义订单金额 DTO，对 `amount` 字段使用自定义金额序列化和反序列化逻辑。

文件位置：`src/main/java/io/github/atengk/dto/OrderAmountDTO.java`

```java
package io.github.atengk.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.github.atengk.jackson.deserializer.AmountJsonDeserializer;
import io.github.atengk.jackson.serializer.AmountJsonSerializer;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 订单金额示例对象
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
public class OrderAmountDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 订单ID
     */
    private Long orderId;

    /**
     * 订单金额
     */
    @JsonSerialize(using = AmountJsonSerializer.class)
    @JsonDeserialize(using = AmountJsonDeserializer.class)
    private BigDecimal amount;

}
```

为了验证注解绑定效果，可以提供一个简单接口。

文件位置：`src/main/java/io/github/atengk/controller/OrderAmountController.java`

```java
package io.github.atengk.controller;

import io.github.atengk.dto.OrderAmountDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 订单金额接口示例
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@RestController
@RequestMapping("/api/orders")
public class OrderAmountController {

    /**
     * 提交订单金额
     *
     * @param dto 订单金额请求参数
     * @return 订单金额响应结果
     */
    @PostMapping("/amount")
    public OrderAmountDTO submitAmount(@RequestBody OrderAmountDTO dto) {
        log.info("接收到订单金额请求，订单ID：{}，金额：{}", dto.getOrderId(), dto.getAmount());
        return dto;
    }

}
```

请求示例：

```bash
curl -X POST "http://localhost:8080/api/orders/amount" \
  -H "Content-Type: application/json" \
  -d '{"orderId":10001,"amount":"¥1,234.56"}'
```

响应示例：

```json
{
  "orderId": 10001,
  "amount": "¥1,234.56"
}
```

注解方式绑定的特点如下：

| 特点           | 说明                                                   |
| -------------- | ------------------------------------------------------ |
| 生效范围小     | 只影响标注了注解的字段                                 |
| 可读性强       | 字段转换规则直接写在 DTO 上                            |
| 适合特殊字段   | 适合金额、手机号脱敏、枚举编码等局部场景               |
| 不适合全局规则 | 如果所有 BigDecimal 都要统一格式，建议使用模块方式注册 |

### 模块方式统一注册

模块方式统一注册是指通过 Jackson 的 `SimpleModule` 将自定义序列化器或反序列化器注册到全局 `ObjectMapper`。该方式适合全项目统一处理某一种类型，例如统一处理 `BigDecimal`、`Long`、枚举、日期时间等。

下面示例将 `BigDecimal` 类型统一注册为金额序列化和反序列化规则。

文件位置：`src/main/java/io/github/atengk/config/JacksonModuleConfig.java`

```java
package io.github.atengk.config;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.github.atengk.jackson.deserializer.AmountJsonDeserializer;
import io.github.atengk.jackson.serializer.AmountJsonSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

/**
 * Jackson 模块统一注册配置
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Configuration
public class JacksonModuleConfig {

    /**
     * 注册金额序列化与反序列化模块
     *
     * @return Jackson 模块
     */
    @Bean
    public Module amountJacksonModule() {
        SimpleModule simpleModule = new SimpleModule();

        // 全局注册 BigDecimal 序列化规则
        simpleModule.addSerializer(BigDecimal.class, new AmountJsonSerializer());

        // 全局注册 BigDecimal 反序列化规则
        simpleModule.addDeserializer(BigDecimal.class, new AmountJsonDeserializer());

        return simpleModule;
    }

}
```

注册后，只要字段类型是 `BigDecimal`，都会使用该转换规则。

示例 DTO 不再需要添加 `@JsonSerialize` 和 `@JsonDeserialize`。

文件位置：`src/main/java/io/github/atengk/dto/OrderAmountGlobalDTO.java`

```java
package io.github.atengk.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 全局金额转换示例对象
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
public class OrderAmountGlobalDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 订单ID
     */
    private Long orderId;

    /**
     * 订单金额
     */
    private BigDecimal amount;

}
```

模块方式统一注册的特点如下：

| 特点         | 说明                                  |
| ------------ | ------------------------------------- |
| 生效范围大   | 对所有指定类型统一生效                |
| 配置集中     | 转换规则集中在 Jackson 配置类中       |
| 适合统一规范 | 适合全局日期、金额、枚举、Long 等处理 |
| 风险更高     | 配置后会影响所有接口，需要评估兼容性  |

注解方式和模块方式的选择建议如下：

| 场景                             | 推荐方式         |
| -------------------------------- | ---------------- |
| 只有某个字段需要特殊转换         | 注解方式绑定     |
| 某个 DTO 需要特殊处理            | 注解方式绑定     |
| 全项目统一处理某种类型           | 模块方式统一注册 |
| 对接第三方接口，规则只在局部生效 | 注解方式绑定     |
| 公司内部接口规范要求统一格式     | 模块方式统一注册 |

如果项目中同时存在后台管理接口、开放平台接口和内部服务接口，不建议轻易对基础类型做全局转换。更稳妥的方式是为不同接口定义独立 DTO，并在需要的字段上使用注解绑定，避免全局规则影响非目标接口。



## Spring MVC 数据转换

Spring MVC 数据转换主要发生在请求进入 Controller 和 Controller 返回响应结果的过程中。请求路径参数、查询参数、表单参数通常由 Spring 的类型转换体系处理；请求体和响应体通常由 `HttpMessageConverter` 处理，JSON 场景下默认使用 Jackson 完成转换。

### 请求参数反序列化

请求参数反序列化主要指将 URL 路径参数、查询参数、表单参数转换为 Java 方法参数。常见注解包括 `@PathVariable`、`@RequestParam`、`@ModelAttribute`，这些参数通常不是由 Jackson 处理，而是由 Spring MVC 的 `ConversionService` 处理。

常见请求参数来源如下：

| 参数来源   | 常用注解          | 示例                                |
| ---------- | ----------------- | ----------------------------------- |
| 路径参数   | `@PathVariable`   | `/api/users/10001`                  |
| 查询参数   | `@RequestParam`   | `/api/users?pageNum=1&pageSize=10`  |
| 表单参数   | `@ModelAttribute` | `application/x-www-form-urlencoded` |
| 请求头参数 | `@RequestHeader`  | `Authorization`、`Content-Type`     |

下面示例演示路径参数、查询参数和日期参数的转换。

文件位置：`src/main/java/io/github/atengk/controller/UserQueryController.java`

```java
package io.github.atengk.controller;

import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.util.StrUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户查询参数转换接口
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@RestController
@RequestMapping("/api/query-users")
public class UserQueryController {

    /**
     * 查询用户详情
     *
     * @param id 用户ID
     * @return 用户详情
     */
    @GetMapping("/{id}")
    public UserQueryVO getUser(@PathVariable Long id) {
        log.info("根据路径参数查询用户，用户ID：{}", id);

        UserQueryVO vo = new UserQueryVO();
        vo.setId(id);
        vo.setUsername("admin");
        vo.setQueryTime(LocalDateTimeUtil.formatNormal(LocalDateTime.now()));
        return vo;
    }

    /**
     * 分页查询用户
     *
     * @param keyword  关键字
     * @param pageNum  页码
     * @param pageSize 每页条数
     * @param beginDate 开始日期
     * @return 查询条件
     */
    @GetMapping
    public UserQueryCondition queryUsers(@RequestParam(required = false) String keyword,
                                         @RequestParam(defaultValue = "1") Integer pageNum,
                                         @RequestParam(defaultValue = "10") Integer pageSize,
                                         @RequestParam(required = false)
                                         @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate beginDate) {
        log.info("分页查询用户，关键字：{}，页码：{}，每页条数：{}", keyword, pageNum, pageSize);

        UserQueryCondition condition = new UserQueryCondition();
        condition.setKeyword(StrUtil.blankToDefault(keyword, ""));
        condition.setPageNum(pageNum);
        condition.setPageSize(pageSize);
        condition.setBeginDate(beginDate);
        return condition;
    }

    /**
     * 用户查询响应对象
     *
     * @author Ateng
     * @since 2026-05-06
     */
    @Data
    public static class UserQueryVO implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * 用户ID
         */
        private Long id;

        /**
         * 用户名
         */
        private String username;

        /**
         * 查询时间
         */
        private String queryTime;

    }

    /**
     * 用户查询条件
     *
     * @author Ateng
     * @since 2026-05-06
     */
    @Data
    public static class UserQueryCondition implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * 查询关键字
         */
        private String keyword;

        /**
         * 页码
         */
        private Integer pageNum;

        /**
         * 每页条数
         */
        private Integer pageSize;

        /**
         * 开始日期
         */
        private LocalDate beginDate;

    }

}
```

请求示例：

```bash
# 路径参数反序列化：字符串 10001 转换为 Long
curl -X GET "http://localhost:8080/api/query-users/10001"

# 查询参数反序列化：pageNum、pageSize 转换为 Integer，beginDate 转换为 LocalDate
curl -X GET "http://localhost:8080/api/query-users?keyword=admin&pageNum=1&pageSize=10&beginDate=2026-05-06"
```

需要注意，`@RequestParam` 中的日期参数通常使用 `@DateTimeFormat`，而不是 `@JsonFormat`。`@JsonFormat` 主要作用于 JSON 请求体和 JSON 响应体；`@DateTimeFormat` 主要作用于 URL 参数、表单参数等非 JSON 参数。

### 请求体反序列化

请求体反序列化主要指将 HTTP 请求体中的 JSON 数据转换为 Java 对象。Spring MVC 中通常使用 `@RequestBody` 触发该过程，底层由 `HttpMessageConverter` 完成，JSON 场景下默认使用 `MappingJackson2HttpMessageConverter`。

请求体反序列化适用于新增、修改、批量提交、复杂查询等请求参数较多的场景。

下面示例演示单个对象请求体和批量对象请求体的反序列化。

文件位置：`src/main/java/io/github/atengk/controller/UserBodyController.java`

```java
package io.github.atengk.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户请求体反序列化接口
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@RestController
@RequestMapping("/api/body-users")
public class UserBodyController {

    /**
     * 创建用户
     *
     * @param dto 用户创建请求
     * @return 用户响应
     */
    @PostMapping
    public UserBodyVO createUser(@RequestBody UserCreateBodyDTO dto) {
        if (StrUtil.isBlank(dto.getUsername())) {
            log.warn("创建用户失败，用户名为空");
            throw new IllegalArgumentException("用户名不能为空");
        }

        log.info("创建用户，请求用户名：{}", dto.getUsername());

        UserBodyVO vo = new UserBodyVO();
        vo.setId(10001L);
        vo.setUsername(dto.getUsername());
        vo.setNickname(dto.getNickname());
        vo.setEnabled(Boolean.TRUE);
        vo.setCreateTime(LocalDateTime.now());
        return vo;
    }

    /**
     * 批量创建用户
     *
     * @param dtoList 用户创建请求集合
     * @return 创建数量
     */
    @PostMapping("/batch")
    public Integer batchCreateUsers(@RequestBody List<UserCreateBodyDTO> dtoList) {
        if (CollUtil.isEmpty(dtoList)) {
            log.warn("批量创建用户失败，请求集合为空");
            throw new IllegalArgumentException("用户集合不能为空");
        }

        log.info("批量创建用户，请求数量：{}", dtoList.size());
        return dtoList.size();
    }

    /**
     * 用户创建请求体
     *
     * @author Ateng
     * @since 2026-05-06
     */
    @Data
    public static class UserCreateBodyDTO implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * 用户名
         */
        private String username;

        /**
         * 昵称
         */
        private String nickname;

        /**
         * 手机号
         */
        private String phone;

    }

    /**
     * 用户响应对象
     *
     * @author Ateng
     * @since 2026-05-06
     */
    @Data
    public static class UserBodyVO implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * 用户ID
         */
        private Long id;

        /**
         * 用户名
         */
        private String username;

        /**
         * 昵称
         */
        private String nickname;

        /**
         * 启用状态
         */
        private Boolean enabled;

        /**
         * 创建时间
         */
        private LocalDateTime createTime;

    }

}
```

单个对象请求示例：

```bash
curl -X POST "http://localhost:8080/api/body-users" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","nickname":"管理员","phone":"13800000000"}'
```

批量对象请求示例：

```bash
curl -X POST "http://localhost:8080/api/body-users/batch" \
  -H "Content-Type: application/json" \
  -d '[{"username":"admin","nickname":"管理员"},{"username":"test","nickname":"测试用户"}]'
```

请求体反序列化需要注意以下几点：

| 场景           | 说明                                                         |
| -------------- | ------------------------------------------------------------ |
| `Content-Type` | JSON 请求体需要设置为 `application/json`                     |
| 字段名称       | 默认需要与 Java 字段名称一致，或通过命名策略、`@JsonProperty` 处理 |
| 日期格式       | JSON 中日期格式需要与 Jackson 配置或 `@JsonFormat` 一致      |
| 泛型集合       | Controller 中 `List<UserDTO>` 可以被 Spring MVC 正确识别     |
| 异常处理       | 参数格式错误通常会抛出 `HttpMessageNotReadableException`     |

### 响应结果序列化

响应结果序列化是指 Controller 返回 Java 对象后，Spring MVC 自动将其转换为 JSON 响应。使用 `@RestController` 时，类中的方法默认相当于添加了 `@ResponseBody`，返回对象会直接写入 HTTP 响应体。

下面示例演示普通对象、集合对象和统一响应结构的序列化。

文件位置：`src/main/java/io/github/atengk/controller/UserResponseController.java`

```java
package io.github.atengk.controller;

import cn.hutool.core.date.LocalDateTimeUtil;
import io.github.atengk.common.Result;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户响应结果序列化接口
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@RestController
@RequestMapping("/api/response-users")
public class UserResponseController {

    /**
     * 查询单个用户
     *
     * @param id 用户ID
     * @return 用户响应对象
     */
    @GetMapping("/{id}")
    public Result<UserResponseVO> getUser(@PathVariable Long id) {
        log.info("查询单个用户响应数据，用户ID：{}", id);
        return Result.success(buildUser(id, "admin", "管理员"));
    }

    /**
     * 查询用户列表
     *
     * @return 用户响应集合
     */
    @GetMapping
    public Result<List<UserResponseVO>> listUsers() {
        log.info("查询用户列表响应数据");

        List<UserResponseVO> userList = List.of(
                buildUser(10001L, "admin", "管理员"),
                buildUser(10002L, "test", "测试用户")
        );
        return Result.success(userList);
    }

    /**
     * 构造用户响应对象
     *
     * @param id       用户ID
     * @param username 用户名
     * @param nickname 昵称
     * @return 用户响应对象
     */
    private UserResponseVO buildUser(Long id, String username, String nickname) {
        LocalDateTime now = LocalDateTime.now();

        UserResponseVO vo = new UserResponseVO();
        vo.setId(id);
        vo.setUsername(username);
        vo.setNickname(nickname);
        vo.setEnabled(Boolean.TRUE);
        vo.setCreateTime(now);
        vo.setCreateTimeText(LocalDateTimeUtil.formatNormal(now));
        return vo;
    }

    /**
     * 用户响应对象
     *
     * @author Ateng
     * @since 2026-05-06
     */
    @Data
    public static class UserResponseVO implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * 用户ID
         */
        private Long id;

        /**
         * 用户名
         */
        private String username;

        /**
         * 昵称
         */
        private String nickname;

        /**
         * 启用状态
         */
        private Boolean enabled;

        /**
         * 创建时间
         */
        private LocalDateTime createTime;

        /**
         * 创建时间文本
         */
        private String createTimeText;

    }

}
```

请求示例：

```bash
curl -X GET "http://localhost:8080/api/response-users/10001"
```

响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 10001,
    "username": "admin",
    "nickname": "管理员",
    "enabled": true,
    "createTime": "2026-05-06 10:30:00",
    "createTimeText": "2026-05-06 10:30:00"
  }
}
```

响应结果序列化建议如下：

| 场景      | 建议                                        |
| --------- | ------------------------------------------- |
| 普通接口  | 使用统一响应结构，例如 `Result<T>`          |
| 敏感字段  | 不要返回密码、密钥、Token、身份证等敏感数据 |
| 日期字段  | 使用全局日期格式配置，避免格式不一致        |
| Long 类型 | 建议序列化为字符串，避免前端精度丢失        |
| 金额字段  | 建议统一格式，避免前端浮点精度问题          |

### HttpMessageConverter 工作机制

`HttpMessageConverter` 是 Spring MVC 中负责 HTTP 请求体和响应体转换的核心组件。它根据请求头、响应头、目标 Java 类型和媒体类型，选择合适的转换器完成数据读写。

JSON 场景下，核心转换器是 `MappingJackson2HttpMessageConverter`。它内部使用 Spring 容器中的 `ObjectMapper` 完成 JSON 与 Java 对象之间的转换。

典型工作流程如下：

```text
请求进入 DispatcherServlet
        ↓
HandlerMapping 找到 Controller 方法
        ↓
HandlerAdapter 准备方法参数
        ↓
@RequestBody 触发 HttpMessageConverter 读取请求体
        ↓
MappingJackson2HttpMessageConverter 使用 ObjectMapper 反序列化 JSON
        ↓
Controller 方法执行业务逻辑
        ↓
返回 Java 对象
        ↓
@ResponseBody 触发 HttpMessageConverter 写出响应体
        ↓
MappingJackson2HttpMessageConverter 使用 ObjectMapper 序列化 JSON
        ↓
返回 HTTP JSON 响应
```

常见转换器如下：

| 转换器                                   | 作用                             |
| ---------------------------------------- | -------------------------------- |
| `MappingJackson2HttpMessageConverter`    | 处理 JSON 请求体和响应体         |
| `StringHttpMessageConverter`             | 处理字符串响应                   |
| `ByteArrayHttpMessageConverter`          | 处理字节数组                     |
| `ResourceHttpMessageConverter`           | 处理文件资源                     |
| `FormHttpMessageConverter`               | 处理表单数据                     |
| `MappingJackson2XmlHttpMessageConverter` | 处理 XML 数据，需要额外 XML 依赖 |

如果需要扩展 Spring MVC 转换器，可以实现 `WebMvcConfigurer`。

文件位置：`src/main/java/io/github/atengk/config/WebMvcConfig.java`

```java
package io.github.atengk.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Spring MVC 扩展配置
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * 扩展 HTTP 消息转换器
     *
     * @param converters HTTP 消息转换器集合
     */
    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        log.info("当前 HTTP 消息转换器数量：{}", converters.size());

        for (HttpMessageConverter<?> converter : converters) {
            log.info("已加载 HTTP 消息转换器：{}", converter.getClass().getName());
        }
    }

}
```

该配置不会覆盖 Spring Boot 默认转换器，只是在默认配置基础上进行扩展。实际项目中，如果只是配置 JSON 格式，优先配置 `ObjectMapper` 或 `Jackson2ObjectMapperBuilderCustomizer`，不要轻易重写全部 `HttpMessageConverter`。

## 实际开发场景

本节整理 Spring Boot 3 接口开发中最常见的 JSON 转换问题，包括枚举类型序列化、Long 类型精度处理、BigDecimal 金额格式处理和 LocalDateTime 格式处理。这些问题通常直接影响前后端接口联调和生产环境数据准确性。

### 枚举类型序列化

枚举类型序列化常见需求是返回给前端的不只是枚举名称，而是业务编码和描述。例如订单状态既需要返回 `code`，也需要返回 `desc`，便于前端展示和判断。

常见枚举返回方式如下：

| 方式         | 输出示例                     | 适用场景                     |
| ------------ | ---------------------------- | ---------------------------- |
| 默认枚举名称 | `"PAID"`                     | 内部接口、简单状态           |
| 枚举编码     | `1`                          | 前后端约定明确的状态码       |
| 枚举对象     | `{"code":1,"desc":"已支付"}` | 前端需要展示描述             |
| 自定义序列化 | 根据业务输出                 | 复杂枚举、多语言、兼容旧接口 |

下面示例使用 `@JsonValue` 和 `@JsonCreator` 实现枚举编码的序列化与反序列化。

文件位置：`src/main/java/io/github/atengk/enums/OrderStatusEnum.java`

```java
package io.github.atengk.enums;

import cn.hutool.core.util.ObjectUtil;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

import java.util.Arrays;

/**
 * 订单状态枚举
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Getter
public enum OrderStatusEnum {

    /**
     * 待支付
     */
    WAIT_PAY(0, "待支付"),

    /**
     * 已支付
     */
    PAID(1, "已支付"),

    /**
     * 已取消
     */
    CANCELED(2, "已取消"),

    /**
     * 已完成
     */
    FINISHED(3, "已完成");

    /**
     * 状态编码
     */
    private final Integer code;

    /**
     * 状态描述
     */
    private final String desc;

    OrderStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 序列化时输出状态编码
     *
     * @return 状态编码
     */
    @JsonValue
    public Integer getCode() {
        return code;
    }

    /**
     * 根据状态编码反序列化为枚举
     *
     * @param code 状态编码
     * @return 订单状态枚举
     */
    @JsonCreator
    public static OrderStatusEnum of(Integer code) {
        if (ObjectUtil.isNull(code)) {
            return null;
        }

        return Arrays.stream(values())
                .filter(item -> ObjectUtil.equal(item.getCode(), code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("订单状态编码不正确：" + code));
    }

}
```

订单 DTO 示例。

文件位置：`src/main/java/io/github/atengk/dto/OrderStatusDTO.java`

```java
package io.github.atengk.dto;

import io.github.atengk.enums.OrderStatusEnum;
import lombok.Data;

import java.io.Serializable;

/**
 * 订单状态示例对象
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
public class OrderStatusDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 订单ID
     */
    private Long orderId;

    /**
     * 订单状态
     */
    private OrderStatusEnum status;

}
```

接口示例。

文件位置：`src/main/java/io/github/atengk/controller/OrderStatusController.java`

```java
package io.github.atengk.controller;

import io.github.atengk.dto.OrderStatusDTO;
import io.github.atengk.enums.OrderStatusEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 订单状态接口示例
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@RestController
@RequestMapping("/api/order-status")
public class OrderStatusController {

    /**
     * 查询订单状态
     *
     * @param orderId 订单ID
     * @return 订单状态
     */
    @GetMapping("/{orderId}")
    public OrderStatusDTO getOrderStatus(@PathVariable Long orderId) {
        log.info("查询订单状态，订单ID：{}", orderId);

        OrderStatusDTO dto = new OrderStatusDTO();
        dto.setOrderId(orderId);
        dto.setStatus(OrderStatusEnum.PAID);
        return dto;
    }

    /**
     * 修改订单状态
     *
     * @param dto 订单状态请求
     * @return 订单状态响应
     */
    @PostMapping
    public OrderStatusDTO updateOrderStatus(@RequestBody OrderStatusDTO dto) {
        log.info("修改订单状态，订单ID：{}，状态：{}", dto.getOrderId(), dto.getStatus());
        return dto;
    }

}
```

请求示例：

```bash
curl -X POST "http://localhost:8080/api/order-status" \
  -H "Content-Type: application/json" \
  -d '{"orderId":10001,"status":1}'
```

响应示例：

```json
{
  "orderId": 10001,
  "status": 1
}
```

如果前端需要同时展示状态描述，可以在 VO 中额外提供 `statusDesc` 字段，而不是强行让枚举字段输出复杂对象。

```java
private Integer status;
private String statusDesc;
```

这种方式对前端更稳定，也便于接口文档表达。

### Long 类型精度处理

Java 的 `Long` 类型最大值可能超过 JavaScript 的安全整数范围。当前端使用 JavaScript 接收较大的 Long 值时，可能出现精度丢失。因此，后端接口返回 ID 类 Long 字段时，通常建议序列化为字符串。

典型问题如下：

```json
{
  "id": 9007199254740993
}
```

前端 JavaScript 可能读取为：

```text
9007199254740992
```

局部字段可以使用 `@JsonFormat(shape = JsonFormat.Shape.STRING)`。

文件位置：`src/main/java/io/github/atengk/dto/LongIdDTO.java`

```java
package io.github.atengk.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;

/**
 * Long 精度处理示例对象
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
public class LongIdDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long userId;

    /**
     * 订单ID
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long orderId;

    /**
     * 用户名
     */
    private String username;

}
```

序列化结果如下：

```json
{
  "userId": "9007199254740993",
  "orderId": "9007199254740995",
  "username": "admin"
}
```

如果项目中所有 Long 类型都需要返回字符串，可以通过 Jackson 全局配置处理。

文件位置：`src/main/java/io/github/atengk/config/JacksonLongConfig.java`

```java
package io.github.atengk.config;

import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jackson Long 类型序列化配置
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Configuration
public class JacksonLongConfig {

    /**
     * 将 Long 类型统一序列化为字符串
     *
     * @return Jackson 配置定制器
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer longToStringCustomizer() {
        return builder -> {
            // 包装类型 Long
            builder.serializerByType(Long.class, ToStringSerializer.instance);

            // 基本类型 long
            builder.serializerByType(Long.TYPE, ToStringSerializer.instance);
        };
    }

}
```

Long 类型处理建议如下：

| 场景                   | 推荐方式                                            |
| ---------------------- | --------------------------------------------------- |
| 只有 ID 字段需要处理   | 使用 `@JsonFormat(shape = JsonFormat.Shape.STRING)` |
| 全项目 ID 都是 Long    | 使用全局 Jackson 配置                               |
| 内部服务接口不经过前端 | 可按实际协议决定                                    |
| 开放接口返回给第三方   | 建议明确字段类型为字符串                            |
| 数据库雪花 ID          | 强烈建议返回字符串                                  |

需要注意，全局 Long 转字符串会影响所有接口中的 Long 字段。如果某些计算类字段需要保持数字类型，应谨慎使用全局配置，或改为在 ID 字段上局部添加注解。

### BigDecimal 金额格式处理

金额字段通常使用 `BigDecimal`，不建议使用 `double` 或 `float`。JSON 返回时，如果直接返回数字类型，前端在计算或展示时可能遇到精度和格式问题。因此，金额字段常见处理方式是统一保留两位小数，并以字符串形式返回。

常见金额处理方式如下：

| 方式       | 示例          | 说明                     |
| ---------- | ------------- | ------------------------ |
| 数字输出   | `1234.5`      | 简单，但展示格式不稳定   |
| 字符串输出 | `"1234.50"`   | 推荐，适合金额展示和传输 |
| 带币种输出 | `"¥1,234.50"` | 适合展示，不适合计算     |
| 分单位整数 | `123450`      | 适合支付系统和精确计算   |

下面示例实现一个金额序列化器，将 `BigDecimal` 统一输出为保留两位小数的字符串。

文件位置：`src/main/java/io/github/atengk/jackson/serializer/MoneyJsonSerializer.java`

```java
package io.github.atengk.jackson.serializer;

import cn.hutool.core.util.NumberUtil;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.math.BigDecimal;

/**
 * 金额 JSON 序列化器
 *
 * @author Ateng
 * @since 2026-05-06
 */
public class MoneyJsonSerializer extends JsonSerializer<BigDecimal> {

    /**
     * 将金额序列化为两位小数字符串
     *
     * @param value       金额
     * @param gen         JSON 生成器
     * @param serializers 序列化上下文
     * @throws IOException IO 异常
     */
    @Override
    public void serialize(BigDecimal value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value == null) {
            gen.writeNull();
            return;
        }

        String moneyText = NumberUtil.decimalFormat("0.00", value);
        gen.writeString(moneyText);
    }

}
```

订单金额 DTO 示例。

文件位置：`src/main/java/io/github/atengk/dto/OrderMoneyDTO.java`

```java
package io.github.atengk.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.github.atengk.jackson.serializer.MoneyJsonSerializer;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 订单金额示例对象
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
public class OrderMoneyDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 订单ID
     */
    private Long orderId;

    /**
     * 原始金额
     */
    @JsonSerialize(using = MoneyJsonSerializer.class)
    private BigDecimal originalAmount;

    /**
     * 实付金额
     */
    @JsonSerialize(using = MoneyJsonSerializer.class)
    private BigDecimal payAmount;

}
```

接口示例。

文件位置：`src/main/java/io/github/atengk/controller/OrderMoneyController.java`

```java
package io.github.atengk.controller;

import io.github.atengk.dto.OrderMoneyDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * 订单金额接口示例
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@RestController
@RequestMapping("/api/order-money")
public class OrderMoneyController {

    /**
     * 查询订单金额
     *
     * @param orderId 订单ID
     * @return 订单金额
     */
    @GetMapping("/{orderId}")
    public OrderMoneyDTO getOrderMoney(@PathVariable Long orderId) {
        log.info("查询订单金额，订单ID：{}", orderId);

        OrderMoneyDTO dto = new OrderMoneyDTO();
        dto.setOrderId(orderId);
        dto.setOriginalAmount(new BigDecimal("199.9"));
        dto.setPayAmount(new BigDecimal("168"));
        return dto;
    }

}
```

请求示例：

```bash
curl -X GET "http://localhost:8080/api/order-money/10001"
```

响应示例：

```json
{
  "orderId": 10001,
  "originalAmount": "199.90",
  "payAmount": "168.00"
}
```

金额字段处理建议如下：

| 场景     | 建议                                      |
| -------- | ----------------------------------------- |
| 金额存储 | 使用 `BigDecimal` 或分单位整数            |
| 金额计算 | 使用 `BigDecimal`，避免 `double`、`float` |
| 接口返回 | 推荐字符串，并统一小数位                  |
| 支付系统 | 推荐使用分单位整数传输                    |
| 前端展示 | 可返回格式化字符串或由前端统一格式化      |

金额字段不要随意进行全局格式化。如果系统中既有金额字段，也有普通高精度数值字段，全局处理 `BigDecimal` 可能误伤非金额数据。更推荐对金额字段使用注解方式绑定自定义序列化器。

### LocalDateTime 格式处理

`LocalDateTime` 是 Spring Boot 3 项目中最常用的日期时间类型之一。默认情况下，如果没有统一配置，接口可能返回 ISO-8601 格式，例如 `2026-05-06T10:30:00`。实际前后端项目中，通常希望统一返回 `yyyy-MM-dd HH:mm:ss` 格式。

局部字段可以使用 `@JsonFormat`。

文件位置：`src/main/java/io/github/atengk/dto/LocalDateTimeDTO.java`

```java
package io.github.atengk.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * LocalDateTime 格式处理示例对象
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
public class LocalDateTimeDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 业务ID
     */
    private Long id;

    /**
     * 业务日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate businessDate;

    /**
     * 业务时间
     */
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime businessTime;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

}
```

如果项目中需要统一日期时间格式，推荐使用全局配置。

文件位置：`src/main/java/io/github/atengk/config/JacksonDateTimeConfig.java`

```java
package io.github.atengk.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Jackson 日期时间格式配置
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Configuration
public class JacksonDateTimeConfig {

    /**
     * 日期时间格式
     */
    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    /**
     * 日期格式
     */
    private static final String DATE_PATTERN = "yyyy-MM-dd";

    /**
     * 时间格式
     */
    private static final String TIME_PATTERN = "HH:mm:ss";

    /**
     * 统一配置 Java 8 日期时间类型
     *
     * @return Jackson 配置定制器
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer dateTimeCustomizer() {
        return builder -> {
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN);
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(DATE_PATTERN);
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern(TIME_PATTERN);

            // 禁止日期时间输出为时间戳
            builder.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

            // 反序列化时忽略未知字段，提升接口兼容性
            builder.featuresToDisable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

            // LocalDateTime 序列化与反序列化格式
            builder.serializerByType(LocalDateTime.class, new LocalDateTimeSerializer(dateTimeFormatter));
            builder.deserializerByType(LocalDateTime.class, new LocalDateTimeDeserializer(dateTimeFormatter));

            // LocalDate 序列化与反序列化格式
            builder.serializerByType(LocalDate.class, new LocalDateSerializer(dateFormatter));
            builder.deserializerByType(LocalDate.class, new LocalDateDeserializer(dateFormatter));

            // LocalTime 序列化与反序列化格式
            builder.serializerByType(LocalTime.class, new LocalTimeSerializer(timeFormatter));
            builder.deserializerByType(LocalTime.class, new LocalTimeDeserializer(timeFormatter));
        };
    }

}
```

接口示例。

文件位置：`src/main/java/io/github/atengk/controller/LocalDateTimeController.java`

```java
package io.github.atengk.controller;

import cn.hutool.core.date.LocalDateTimeUtil;
import io.github.atengk.dto.LocalDateTimeDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * LocalDateTime 格式接口示例
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@RestController
@RequestMapping("/api/date-time")
public class LocalDateTimeController {

    /**
     * 查询日期时间
     *
     * @param id 业务ID
     * @return 日期时间响应
     */
    @GetMapping("/{id}")
    public LocalDateTimeDTO getDateTime(@PathVariable Long id) {
        log.info("查询日期时间示例，业务ID：{}", id);

        LocalDateTime now = LocalDateTime.now();

        LocalDateTimeDTO dto = new LocalDateTimeDTO();
        dto.setId(id);
        dto.setBusinessDate(LocalDate.now());
        dto.setBusinessTime(LocalTime.now().withNano(0));
        dto.setCreateTime(now);

        log.info("当前时间：{}", LocalDateTimeUtil.formatNormal(now));
        return dto;
    }

    /**
     * 提交日期时间
     *
     * @param dto 日期时间请求
     * @return 日期时间响应
     */
    @PostMapping
    public LocalDateTimeDTO submitDateTime(@RequestBody LocalDateTimeDTO dto) {
        log.info("提交日期时间示例，业务ID：{}，创建时间：{}", dto.getId(), dto.getCreateTime());
        return dto;
    }

}
```

请求示例：

```bash
curl -X POST "http://localhost:8080/api/date-time" \
  -H "Content-Type: application/json" \
  -d '{"id":10001,"businessDate":"2026-05-06","businessTime":"10:30:00","createTime":"2026-05-06 10:30:00"}'
```

响应示例：

```json
{
  "id": 10001,
  "businessDate": "2026-05-06",
  "businessTime": "10:30:00",
  "createTime": "2026-05-06 10:30:00"
}
```

`LocalDateTime` 格式处理建议如下：

| 场景               | 推荐方式                                     |
| ------------------ | -------------------------------------------- |
| 单个字段格式特殊   | 使用 `@JsonFormat`                           |
| 全项目统一格式     | 使用 `Jackson2ObjectMapperBuilderCustomizer` |
| URL 查询参数日期   | 使用 `@DateTimeFormat`                       |
| JSON 请求体日期    | 使用 Jackson 配置或 `@JsonFormat`            |
| 涉及时区的绝对时间 | 考虑使用 `Instant`、`OffsetDateTime`         |
| 业务本地时间       | 使用 `LocalDateTime`                         |

需要区分 `@JsonFormat` 和 `@DateTimeFormat` 的使用范围。`@JsonFormat` 主要用于 JSON 请求体和响应体，`@DateTimeFormat` 主要用于 URL 查询参数、路径参数和表单参数。



## 全局统一配置

全局统一配置用于集中管理 Spring Boot 项目中的 JSON 序列化、反序列化、日期时间格式、Long 精度、空值策略和 Spring MVC 参数转换规则。实际项目中建议将全局规则集中到配置类和配置文件中，避免在多个 DTO、Controller 或 Service 中重复处理。

### Jackson 全局配置类

Jackson 全局配置类适合处理需要在整个项目中统一生效的 JSON 转换规则，例如 `LocalDateTime` 格式、`Long` 转字符串、未知字段忽略、空值不输出等。

推荐使用 `Jackson2ObjectMapperBuilderCustomizer` 扩展 Spring Boot 默认的 Jackson 配置，不建议直接重新声明一个新的 `ObjectMapper` Bean。直接覆盖 `ObjectMapper` 容易导致 Spring Boot 默认自动配置、模块注册、第三方组件配置失效。

下面示例配置了以下规则：

| 配置项                 | 说明                           |
| ---------------------- | ------------------------------ |
| `Long` 转字符串        | 避免前端 JavaScript 精度丢失   |
| `LocalDateTime` 格式化 | 统一输出 `yyyy-MM-dd HH:mm:ss` |
| `LocalDate` 格式化     | 统一输出 `yyyy-MM-dd`          |
| `LocalTime` 格式化     | 统一输出 `HH:mm:ss`            |
| 忽略未知字段           | 提升接口兼容性                 |
| 不输出空字段           | 减少响应中的无效字段           |
| 日期不输出时间戳       | 避免返回数字时间戳             |

文件位置：`src/main/java/io/github/atengk/config/JacksonGlobalConfig.java`

```java
package io.github.atengk.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
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
 * Jackson 全局序列化与反序列化配置
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Configuration
public class JacksonGlobalConfig {

    /**
     * 日期时间格式
     */
    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    /**
     * 日期格式
     */
    private static final String DATE_PATTERN = "yyyy-MM-dd";

    /**
     * 时间格式
     */
    private static final String TIME_PATTERN = "HH:mm:ss";

    /**
     * 自定义 Jackson 全局配置
     *
     * @return Jackson 配置定制器
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonGlobalCustomizer() {
        log.info("初始化 Jackson 全局配置");

        return builder -> {
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN);
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(DATE_PATTERN);
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern(TIME_PATTERN);

            // 序列化时忽略 null 字段
            builder.serializationInclusion(JsonInclude.Include.NON_NULL);

            // 禁止日期时间序列化为时间戳
            builder.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

            // 反序列化时忽略未知字段
            builder.featuresToDisable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

            // Long 类型统一序列化为字符串，避免前端精度丢失
            builder.serializerByType(Long.class, ToStringSerializer.instance);
            builder.serializerByType(Long.TYPE, ToStringSerializer.instance);

            // LocalDateTime 序列化与反序列化
            builder.serializerByType(LocalDateTime.class, new LocalDateTimeSerializer(dateTimeFormatter));
            builder.deserializerByType(LocalDateTime.class, new LocalDateTimeDeserializer(dateTimeFormatter));

            // LocalDate 序列化与反序列化
            builder.serializerByType(LocalDate.class, new LocalDateSerializer(dateFormatter));
            builder.deserializerByType(LocalDate.class, new LocalDateDeserializer(dateFormatter));

            // LocalTime 序列化与反序列化
            builder.serializerByType(LocalTime.class, new LocalTimeSerializer(timeFormatter));
            builder.deserializerByType(LocalTime.class, new LocalTimeDeserializer(timeFormatter));
        };
    }

}
```

配置生效后，接口返回中的 `Long`、`LocalDateTime` 等字段会按统一规则输出。例如：

```json
{
  "id": "9007199254740993",
  "username": "admin",
  "createTime": "2026-05-06 10:30:00"
}
```

使用全局配置时需要注意，如果项目中某些接口不希望 `Long` 转字符串，或者某些 `BigDecimal` 字段不是金额字段，就不应轻易做基础类型的全局转换。全局规则会影响所有接口，需要提前与前端和接口调用方确认。

### WebMvcConfigurer 扩展配置

`WebMvcConfigurer` 主要用于扩展 Spring MVC 的 Web 层行为，例如参数转换器、跨域、拦截器、静态资源、消息转换器等。在序列化与反序列化场景中，它常用于扩展 URL 参数、表单参数的转换规则。

需要区分两类转换：

| 转换场景                         | 主要处理组件                                         |
| -------------------------------- | ---------------------------------------------------- |
| JSON 请求体、JSON 响应体         | Jackson、`HttpMessageConverter`                      |
| URL 查询参数、路径参数、表单参数 | Spring `Converter`、`Formatter`、`ConversionService` |

下面示例扩展 `String` 到 `LocalDateTime` 的转换规则，用于处理查询参数或表单参数中的日期时间。

文件位置：`src/main/java/io/github/atengk/converter/StringToLocalDateTimeConverter.java`

```java
package io.github.atengk.converter;

import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;

import java.time.LocalDateTime;

/**
 * 字符串转 LocalDateTime 转换器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
public class StringToLocalDateTimeConverter implements Converter<String, LocalDateTime> {

    /**
     * 日期时间格式
     */
    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    /**
     * 将字符串转换为 LocalDateTime
     *
     * @param source 原始字符串
     * @return LocalDateTime
     */
    @Override
    public LocalDateTime convert(String source) {
        if (StrUtil.isBlank(source)) {
            return null;
        }

        try {
            return LocalDateTimeUtil.parse(source, DATE_TIME_PATTERN);
        } catch (Exception e) {
            log.warn("日期时间参数转换失败，原始值：{}", source);
            throw new IllegalArgumentException("日期时间格式不正确，请使用 yyyy-MM-dd HH:mm:ss");
        }
    }

}
```

将转换器注册到 Spring MVC 中。

文件位置：`src/main/java/io/github/atengk/config/WebMvcSerializeConfig.java`

```java
package io.github.atengk.config;

import io.github.atengk.converter.StringToLocalDateTimeConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Spring MVC 数据转换扩展配置
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Configuration
public class WebMvcSerializeConfig implements WebMvcConfigurer {

    /**
     * 注册 Spring MVC 参数转换器
     *
     * @param registry 转换器注册器
     */
    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(new StringToLocalDateTimeConverter());
        log.info("注册 String 转 LocalDateTime 参数转换器");
    }

    /**
     * 扩展 HTTP 消息转换器
     *
     * @param converters HTTP 消息转换器集合
     */
    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        log.info("当前 HTTP 消息转换器数量：{}", converters.size());
    }

}
```

示例接口：

```java
@GetMapping("/records")
public String queryRecords(@RequestParam LocalDateTime beginTime) {
    return beginTime.toString();
}
```

请求示例：

```bash
curl -X GET "http://localhost:8080/api/records?beginTime=2026-05-06%2010:30:00"
```

`WebMvcConfigurer` 扩展建议如下：

| 场景               | 建议                                    |
| ------------------ | --------------------------------------- |
| URL 参数日期转换   | 使用 `Converter` 或 `@DateTimeFormat`   |
| JSON 日期转换      | 使用 Jackson 全局配置或 `@JsonFormat`   |
| 扩展默认消息转换器 | 使用 `extendMessageConverters`          |
| 完全替换默认转换器 | 谨慎使用，容易影响 Spring Boot 默认行为 |
| 普通 JSON 规则     | 优先配置 `ObjectMapper`                 |

### 配置文件方式配置

配置文件方式适合处理 Jackson 的基础配置，例如时区、日期格式、未知字段处理、空值处理、字段命名策略等。对于简单项目，`application.yml` 已经可以满足大部分需求。

文件位置：`src/main/resources/application.yml`

```yaml
server:
  # 服务端口
  port: 8080

spring:
  application:
    # 应用名称
    name: spring-boot-serialize-demo

  jackson:
    # 默认时区，避免 java.util.Date 序列化后出现时区偏移
    time-zone: GMT+8

    # java.util.Date 默认格式
    date-format: yyyy-MM-dd HH:mm:ss

    # 字段命名策略：SNAKE_CASE 会将 userName 转换为 user_name
    # property-naming-strategy: SNAKE_CASE

    # 默认字段输出策略：non_null 表示不输出 null 字段
    default-property-inclusion: non_null

    serialization:
      # 日期时间不输出为时间戳
      write-dates-as-timestamps: false

    deserialization:
      # 反序列化时遇到未知字段不报错
      fail-on-unknown-properties: false
```

配置文件方式和配置类方式的选择建议如下：

| 配置方式                                | 适用场景                                      |
| --------------------------------------- | --------------------------------------------- |
| `application.yml`                       | 简单、标准、Spring Boot 已支持的 Jackson 配置 |
| `Jackson2ObjectMapperBuilderCustomizer` | 日期类型、Long 类型、自定义序列化器等复杂配置 |
| `WebMvcConfigurer`                      | URL 参数、表单参数、Web 层转换扩展            |
| 注解配置                                | 某个字段、某个 DTO 的局部特殊规则             |

实际项目中推荐组合使用：配置文件处理基础规则，Jackson 配置类处理复杂 JSON 规则，`WebMvcConfigurer` 处理 URL 参数和表单参数转换。

## 接口验证

接口验证用于确认序列化与反序列化配置是否符合预期。验证内容包括请求参数转换、请求体反序列化、响应结果序列化、日期格式、Long 精度、空值处理和异常场景。

### Controller 示例接口

下面提供一个完整的验证接口，覆盖查询参数、请求体、响应体、Long 字符串输出、LocalDateTime 格式化和异常输入处理。

如果使用参数校验，需要添加校验依赖。

文件位置：`pom.xml`

```xml
<!-- 参数校验：支持 @Valid、@NotBlank、@NotNull 等注解 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

请求对象如下。

文件位置：`src/main/java/io/github/atengk/dto/SerializeVerifyRequestDTO.java`

```java
package io.github.atengk.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 序列化验证请求对象
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
public class SerializeVerifyRequestDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /**
     * 用户名
     */
    @NotBlank(message = "用户名不能为空")
    private String username;

    /**
     * 订单金额
     */
    @NotNull(message = "订单金额不能为空")
    private BigDecimal amount;

    /**
     * 创建时间
     */
    @NotNull(message = "创建时间不能为空")
    private LocalDateTime createTime;

}
```

响应对象如下。

文件位置：`src/main/java/io/github/atengk/vo/SerializeVerifyResponseVO.java`

```java
package io.github.atengk.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 序列化验证响应对象
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
public class SerializeVerifyResponseVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 订单金额
     */
    private BigDecimal amount;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 处理结果
     */
    private String result;

}
```

Controller 示例接口如下。

文件位置：`src/main/java/io/github/atengk/controller/SerializeVerifyController.java`

```java
package io.github.atengk.controller;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.common.Result;
import io.github.atengk.dto.SerializeVerifyRequestDTO;
import io.github.atengk.vo.SerializeVerifyResponseVO;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 序列化与反序列化验证接口
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@RestController
@RequestMapping("/api/serialize/verify")
public class SerializeVerifyController {

    /**
     * 验证请求参数转换
     *
     * @param userId   用户ID
     * @param username 用户名
     * @param beginTime 开始时间
     * @return 响应结果
     */
    @GetMapping("/params")
    public Result<SerializeVerifyResponseVO> verifyParams(@RequestParam Long userId,
                                                          @RequestParam String username,
                                                          @RequestParam LocalDateTime beginTime) {
        log.info("验证请求参数转换，用户ID：{}，用户名：{}，开始时间：{}", userId, username, beginTime);

        SerializeVerifyResponseVO vo = new SerializeVerifyResponseVO();
        vo.setUserId(userId);
        vo.setUsername(StrUtil.trim(username));
        vo.setAmount(new BigDecimal("99.90"));
        vo.setCreateTime(beginTime);
        vo.setResult("请求参数转换成功");

        return Result.success(vo);
    }

    /**
     * 验证请求体反序列化和响应体序列化
     *
     * @param request 请求参数
     * @return 响应结果
     */
    @PostMapping("/body")
    public Result<SerializeVerifyResponseVO> verifyBody(@Valid @RequestBody SerializeVerifyRequestDTO request) {
        log.info("验证请求体转换，用户ID：{}，用户名：{}", request.getUserId(), request.getUsername());

        SerializeVerifyResponseVO vo = new SerializeVerifyResponseVO();
        vo.setUserId(request.getUserId());
        vo.setUsername(StrUtil.trim(request.getUsername()));
        vo.setAmount(request.getAmount());
        vo.setCreateTime(request.getCreateTime());
        vo.setResult("请求体反序列化和响应体序列化成功");

        return Result.success(vo);
    }

}
```

### 请求参数测试

请求参数测试主要验证 `@RequestParam`、`@PathVariable` 等非 JSON 参数是否可以正确转换为 Java 类型。日期时间参数如果通过 URL 传递，需要注意空格需要进行 URL 编码。

请求参数测试命令如下：

```bash
curl -X GET "http://localhost:8080/api/serialize/verify/params?userId=9007199254740993&username=admin&beginTime=2026-05-06%2010:30:00"
```

预期响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "userId": "9007199254740993",
    "username": "admin",
    "amount": 99.90,
    "createTime": "2026-05-06 10:30:00",
    "result": "请求参数转换成功"
  }
}
```

该测试重点验证以下内容：

| 验证项                         | 预期结果                                 |
| ------------------------------ | ---------------------------------------- |
| `userId` 转 Long               | Controller 可以正常接收                  |
| `userId` 响应序列化            | 如果配置 Long 转字符串，响应中应为字符串 |
| `beginTime` 转 `LocalDateTime` | 格式正确时转换成功                       |
| `createTime` 响应格式          | 返回 `yyyy-MM-dd HH:mm:ss`               |
| `username` 字符串处理          | 可以在业务中进行 trim 等清洗             |

### 响应结果测试

响应结果测试主要验证 Java 对象返回给前端时是否按统一 JSON 规则输出，包括字段名称、日期格式、Long 精度、空值处理等。

请求体测试命令如下：

```bash
curl -X POST "http://localhost:8080/api/serialize/verify/body" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 9007199254740993,
    "username": " admin ",
    "amount": 199.90,
    "createTime": "2026-05-06 10:30:00"
  }'
```

如果已经配置全局 `Long` 转字符串和 `LocalDateTime` 格式化，预期响应如下：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "userId": "9007199254740993",
    "username": "admin",
    "amount": 199.90,
    "createTime": "2026-05-06 10:30:00",
    "result": "请求体反序列化和响应体序列化成功"
  }
}
```

响应结果测试重点如下：

| 验证项    | 说明                                   |
| --------- | -------------------------------------- |
| Long 精度 | 大 Long 值是否以字符串返回             |
| 日期格式  | `LocalDateTime` 是否按统一格式返回     |
| 空值字段  | `null` 字段是否按配置隐藏              |
| 字段命名  | 是否符合前后端约定，例如小驼峰或下划线 |
| 金额字段  | 是否符合金额格式约定                   |

如果项目使用 MockMvc，也可以编写自动化测试。

文件位置：`src/test/java/io/github/atengk/SerializeVerifyControllerTests.java`

```java
package io.github.atengk;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 序列化与反序列化接口验证测试
 *
 * @author Ateng
 * @since 2026-05-06
 */
@SpringBootTest
@AutoConfigureMockMvc
public class SerializeVerifyControllerTests {

    @Autowired
    private MockMvc mockMvc;

    /**
     * 验证请求体反序列化和响应体序列化
     *
     * @throws Exception 测试异常
     */
    @Test
    void testVerifyBody() throws Exception {
        String requestBody = """
                {
                  "userId": 9007199254740993,
                  "username": "admin",
                  "amount": 199.90,
                  "createTime": "2026-05-06 10:30:00"
                }
                """;

        mockMvc.perform(post("/api/serialize/verify/body")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.data.userId", is("9007199254740993")))
                .andExpect(jsonPath("$.data.username", is("admin")))
                .andExpect(jsonPath("$.data.createTime", is("2026-05-06 10:30:00")))
                .andExpect(jsonPath("$.data.result", notNullValue()));
    }

}
```

### 异常场景验证

异常场景验证主要用于确认参数格式错误、JSON 格式错误、日期格式错误、字段校验失败时，接口是否能返回统一、清晰的错误响应。

常见异常如下：

| 异常类型                              | 常见原因                                          |
| ------------------------------------- | ------------------------------------------------- |
| `HttpMessageNotReadableException`     | JSON 格式错误、字段类型不匹配、请求体无法反序列化 |
| `MethodArgumentNotValidException`     | `@RequestBody` 参数校验失败                       |
| `BindException`                       | 表单对象绑定失败                                  |
| `ConstraintViolationException`        | 单个参数校验失败                                  |
| `MethodArgumentTypeMismatchException` | URL 参数类型转换失败                              |
| `IllegalArgumentException`            | 自定义转换器或业务参数校验失败                    |

建议提供统一异常处理器，将转换异常转换为稳定的接口响应。

文件位置：`src/main/java/io/github/atengk/handler/GlobalExceptionHandler.java`

```java
package io.github.atengk.handler;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.common.Result;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;
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

    /**
     * 处理请求体无法读取异常
     *
     * @param e 异常对象
     * @return 统一响应
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.warn("请求体反序列化失败：{}", e.getMessage());
        return Result.fail(400, "请求体格式不正确，请检查 JSON 格式、字段类型或日期格式");
    }

    /**
     * 处理请求体参数校验异常
     *
     * @param e 异常对象
     * @return 统一响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = buildFieldErrorMessage(e.getBindingResult().getFieldErrors());
        log.warn("请求体参数校验失败：{}", message);
        return Result.fail(400, message);
    }

    /**
     * 处理表单绑定异常
     *
     * @param e 异常对象
     * @return 统一响应
     */
    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException e) {
        String message = buildFieldErrorMessage(e.getBindingResult().getFieldErrors());
        log.warn("请求参数绑定失败：{}", message);
        return Result.fail(400, message);
    }

    /**
     * 处理单个参数校验异常
     *
     * @param e 异常对象
     * @return 统一响应
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraintViolationException(ConstraintViolationException e) {
        log.warn("请求参数校验失败：{}", e.getMessage());
        return Result.fail(400, e.getMessage());
    }

    /**
     * 处理 URL 参数类型转换异常
     *
     * @param e 异常对象
     * @return 统一响应
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result<Void> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        log.warn("请求参数类型转换失败，参数名：{}，参数值：{}", e.getName(), e.getValue());
        return Result.fail(400, "请求参数类型不正确：" + e.getName());
    }

    /**
     * 处理非法参数异常
     *
     * @param e 异常对象
     * @return 统一响应
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public Result<Void> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("请求参数不合法：{}", e.getMessage());
        return Result.fail(400, e.getMessage());
    }

    /**
     * 构造字段错误消息
     *
     * @param fieldErrors 字段错误集合
     * @return 错误消息
     */
    private String buildFieldErrorMessage(List<FieldError> fieldErrors) {
        if (CollUtil.isEmpty(fieldErrors)) {
            return "请求参数校验失败";
        }

        String message = fieldErrors.stream()
                .map(item -> StrUtil.format("{}：{}", item.getField(), item.getDefaultMessage()))
                .collect(Collectors.joining("；"));

        return StrUtil.blankToDefault(message, "请求参数校验失败");
    }

}
```

异常请求示例一：JSON 日期格式错误。

```bash
curl -X POST "http://localhost:8080/api/serialize/verify/body" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 10001,
    "username": "admin",
    "amount": 199.90,
    "createTime": "2026/05/06 10:30:00"
  }'
```

预期响应示例：

```json
{
  "code": 400,
  "message": "请求体格式不正确，请检查 JSON 格式、字段类型或日期格式",
  "data": null
}
```

异常请求示例二：必填字段为空。

```bash
curl -X POST "http://localhost:8080/api/serialize/verify/body" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 10001,
    "username": "",
    "amount": 199.90,
    "createTime": "2026-05-06 10:30:00"
  }'
```

预期响应示例：

```json
{
  "code": 400,
  "message": "username：用户名不能为空",
  "data": null
}
```

异常场景验证建议如下：

| 验证项        | 示例                         |
| ------------- | ---------------------------- |
| JSON 语法错误 | 少逗号、少括号、字符串未闭合 |
| 字段类型错误  | `userId` 传入 `"abc"`        |
| 日期格式错误  | `2026/05/06 10:30:00`        |
| 必填字段为空  | `username` 为空              |
| 未知字段      | 根据配置确认是否忽略         |
| Long 精度     | 大 ID 是否以字符串返回       |

## 开发注意事项

开发注意事项主要用于规避序列化与反序列化在生产项目中的常见问题，包括类型转换异常、字段命名不一致、敏感字段泄露、接口兼容性不足等。

### 类型转换异常处理

类型转换异常通常发生在请求参数、请求体字段类型与 Java 字段类型不一致时。例如前端传入字符串 `"abc"`，后端使用 `Long` 接收；前端传入 `2026/05/06`，后端按 `yyyy-MM-dd` 解析。

常见问题如下：

| 问题          | 示例                       | 处理方式                   |
| ------------- | -------------------------- | -------------------------- |
| 数字类型错误  | `id: "abc"`                | 返回明确参数错误           |
| 日期格式错误  | `createTime: "2026/05/06"` | 统一日期格式               |
| 枚举编码错误  | `status: 99`               | 枚举反序列化时抛出明确异常 |
| JSON 结构错误 | 数组传成对象               | 使用统一异常处理           |
| 泛型类型错误  | 集合元素字段不匹配         | 明确 DTO 类型和接口文档    |

建议在项目中统一处理转换异常，不要直接将底层异常堆栈返回给前端。前端只需要明确知道哪个参数错误、正确格式是什么。

类型转换异常处理建议如下：

```text
1. Controller 入参使用明确的 DTO，不直接使用 Map 接收复杂请求。
2. 日期时间格式在接口文档中固定，例如 yyyy-MM-dd HH:mm:ss。
3. 枚举字段明确传 code、name 还是对象。
4. Long 类型 ID 返回字符串，避免前端精度丢失。
5. 通过全局异常处理器统一返回错误信息。
6. 关键字段配合 Bean Validation 做必填和格式校验。
```

对于复杂业务字段，不建议依赖 Jackson 静默转换。例如金额、状态、权限标识等字段，如果格式非法，应抛出明确异常，而不是默认转为 `null`。

### 前后端字段格式约定

前后端字段格式约定是接口稳定性的基础。序列化和反序列化配置一旦上线，字段命名、日期格式、金额格式、枚举格式都应保持稳定，避免频繁变更导致前端兼容成本升高。

建议在项目初期明确以下规范：

| 规范项        | 推荐约定                                      |
| ------------- | --------------------------------------------- |
| JSON 字段命名 | 小驼峰 `userName` 或下划线 `user_name` 二选一 |
| 日期时间格式  | `yyyy-MM-dd HH:mm:ss`                         |
| 日期格式      | `yyyy-MM-dd`                                  |
| 时间格式      | `HH:mm:ss`                                    |
| Long 类型 ID  | 字符串返回                                    |
| 金额字段      | 字符串或分单位整数                            |
| 枚举字段      | 明确使用 code、name 或对象                    |
| 空字段        | 明确是否返回 `null`                           |
| 未知字段      | 普通接口可忽略，安全接口需谨慎                |

如果项目采用小驼峰字段，示例如下：

```json
{
  "userId": "10001",
  "userName": "admin",
  "createTime": "2026-05-06 10:30:00"
}
```

如果项目采用下划线字段，示例如下：

```json
{
  "user_id": "10001",
  "user_name": "admin",
  "create_time": "2026-05-06 10:30:00"
}
```

字段命名策略应尽量全项目统一。不要在同一个系统中部分接口使用小驼峰、部分接口使用下划线，除非是为了兼容第三方接口或历史接口。

### 安全与敏感字段处理

安全与敏感字段处理是序列化开发中的重点。很多敏感信息泄露并不是业务逻辑直接返回造成的，而是由于复用了数据库实体、DTO、缓存对象，导致 Jackson 自动将敏感字段序列化到响应中。

常见敏感字段包括：

| 字段类型   | 示例                                     |
| ---------- | ---------------------------------------- |
| 密码信息   | `password`、`oldPassword`、`newPassword` |
| 密钥信息   | `secretKey`、`accessKey`、`privateKey`   |
| Token 信息 | `accessToken`、`refreshToken`            |
| 身份信息   | `idCard`、`realName`、`phone`            |
| 内部字段   | `deleted`、`tenantId`、`internalRemark`  |
| 权限字段   | `roleCode`、`permissionList`、`isAdmin`  |

建议使用独立 VO 控制接口响应字段，不要直接返回数据库实体。

错误示例：

```java
@GetMapping("/{id}")
public UserEntity getUser(@PathVariable Long id) {
    return userService.getById(id);
}
```

推荐示例：

```java
@GetMapping("/{id}")
public UserVO getUser(@PathVariable Long id) {
    UserEntity entity = userService.getById(id);

    UserVO vo = new UserVO();
    vo.setId(entity.getId());
    vo.setUsername(entity.getUsername());
    vo.setNickname(entity.getNickname());
    return vo;
}
```

对于必须保留在对象中的敏感字段，可以使用 `@JsonIgnore` 做兜底处理。

```java
/**
 * 登录密码
 */
@JsonIgnore
private String password;
```

安全处理建议如下：

```text
1. 接口响应优先使用 VO，不直接返回 Entity。
2. 密码、密钥、Token 字段必须避免序列化返回。
3. 手机号、身份证号等字段按业务要求脱敏。
4. 管理端接口和用户端接口使用不同 VO。
5. 第三方开放接口使用独立 DTO，避免复用内部对象。
6. 日志中不要输出完整敏感字段。
7. 缓存对象和响应对象分开设计，避免缓存字段被误返回。
```

如果需要对手机号进行脱敏，可以在业务组装 VO 时处理：

```java
String safePhone = DesensitizedUtil.mobilePhone("13800000000");
```

返回结果示例：

```json
{
  "phone": "138****0000"
}
```

### 兼容性与扩展性设计

兼容性与扩展性设计用于保证接口在版本升级、字段新增、第三方对接和前后端迭代过程中保持稳定。序列化规则一旦影响外部接口，就应被视为接口协议的一部分。

常见兼容性设计如下：

| 设计项            | 建议                                         |
| ----------------- | -------------------------------------------- |
| 新增字段          | 可以新增，前端按需使用                       |
| 删除字段          | 谨慎删除，优先标记废弃                       |
| 字段改名          | 保留旧字段一段时间，或使用 `@JsonAlias` 兼容 |
| 字段类型变更      | 高风险，建议新增字段替代                     |
| 枚举新增值        | 前端需要有默认展示逻辑                       |
| 日期格式变更      | 高风险，不建议频繁调整                       |
| Long 输出策略变更 | 高风险，需前后端同步                         |
| 空值策略变更      | 可能影响前端字段判断                         |

字段改名时，可以使用 `@JsonAlias` 兼容历史请求字段。

```java
@JsonProperty("userName")
@JsonAlias({"username", "user_name"})
private String userName;
```

如果响应字段需要兼容旧版本，可以临时保留旧字段。

```java
/**
 * 用户名
 */
private String userName;

/**
 * 旧版用户名字段，兼容历史接口
 */
@Deprecated
private String username;
```

接口扩展建议如下：

```text
1. 请求 DTO 和响应 VO 分开设计，不共用同一个对象。
2. 内部对象和开放接口对象分开设计。
3. 字段新增优于字段修改，避免破坏旧客户端。
4. 字段废弃先标记，再逐步下线。
5. 枚举新增值时，前端需要默认兜底展示。
6. 第三方接口版本化，例如 /api/v1、/api/v2。
7. 全局 Jackson 配置变更前需要回归核心接口。
8. 重要字段格式写入接口文档，避免只依赖代码约定。
```

对于长期维护的项目，建议将序列化规则沉淀为团队级接口规范，例如：

```text
1. 所有 ID 类字段统一使用字符串返回。
2. 所有日期时间字段统一使用 yyyy-MM-dd HH:mm:ss。
3. 所有金额字段统一使用字符串，保留两位小数。
4. 所有枚举字段统一返回 code，同时必要时返回 desc。
5. 所有接口响应统一使用 Result<T> 包装。
6. 所有接口禁止直接返回 Entity。
7. 所有敏感字段必须在 VO 层排除或脱敏。
```

这些约定应在项目初始化阶段确定，并在代码评审和接口评审中持续检查。