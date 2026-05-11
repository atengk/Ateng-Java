# Spring Boot 4 Jackson 3 开发

本文说明 Spring Boot 4 中 Jackson 3 的基础关系、依赖配置和自动配置机制，重点面向 REST API、统一 JSON 序列化、反序列化和后续迁移改造场景。当前 Spring Boot 4.0.6 要求至少 Java 17，并依赖 Spring Framework 7.0.7 或更高版本；Maven 需要 3.6.3 或更高版本，Gradle 支持 8.14+ 与 9.x。([Home](https://docs.spring.io/spring-boot/system-requirements.html))

## 基础概述

基础概述用于明确 Spring Boot 4 为什么切换到 Jackson 3、Jackson 2 到 Jackson 3 的主要差异，以及该变化会影响项目中的哪些模块。

### Spring Boot 4 与 Jackson 3 的关系

Spring Boot 4 将 Jackson 3 作为默认且首选的 JSON 处理库。Spring Boot 4 的 JSON 支持同时覆盖 Jackson 3、Jackson 2、Gson、JSON-B 和 Kotlin Serialization，但默认方向已经转向 Jackson 3；Jackson 2 支持在 Spring Boot 4 中处于废弃状态，主要用于降低迁移成本，不建议在新项目中长期依赖。([Home](https://docs.spring.io/spring-boot/reference/features/json.html))

在 Spring Boot 4 项目中，只要引入 Web 相关 Starter，通常会间接引入 `spring-boot-starter-json`。该 Starter 会带入 Jackson 3 相关依赖，并触发 Jackson 自动配置。Spring Boot 4 不再围绕 Jackson 2 的 `ObjectMapper` 作为首选入口，而是自动配置 Jackson 3 的 `tools.jackson.databind.json.JsonMapper` Bean。([Home](https://docs.spring.io/spring-boot/reference/features/json.html))

开发时可以把 Spring Boot 4 与 Jackson 3 的关系理解为：

| 层级                              | 作用                                                 |
| --------------------------------- | ---------------------------------------------------- |
| Spring Boot 4                     | 管理依赖版本、提供自动配置、暴露配置项               |
| Spring Framework 7                | 提供 MVC/WebFlux 消息转换器集成                      |
| Jackson 3                         | 执行 JSON 与 Java 对象之间的序列化、反序列化         |
| `JsonMapper`                      | Jackson 3 中 JSON 数据绑定的主要入口                 |
| `JacksonJsonHttpMessageConverter` | Spring MVC 中负责 HTTP JSON 请求体和响应体转换的组件 |

在业务代码中，大多数 Controller 不需要直接调用 `JsonMapper`。只要返回普通 Java 对象、DTO、VO、`Map` 或集合，Spring MVC 会通过 `HttpMessageConverter` 自动完成 JSON 输出。

### Jackson 2 到 Jackson 3 的核心变化

Jackson 3 是一个主版本升级，不与 Jackson 2 保持 API 兼容。最明显的变化是 Maven 坐标和 Java 包名从 `com.fasterxml.jackson` 迁移到 `tools.jackson`，但 `jackson-annotations` 是例外，Jackson 3 仍使用 2.x 版本的 annotations；同时，`jackson-databind` 中的部分注解，例如 `@JsonSerialize`、`@JsonDeserialize`，会迁移到新的 `tools.jackson.databind.annotation` 包。([GitHub](https://github.com/FasterXML/jackson/blob/main/jackson3/MIGRATING_TO_JACKSON_3.md?utm_source=chatgpt.com))

常见迁移点如下：

| 领域             | Jackson 2                               | Jackson 3                                           |
| ---------------- | --------------------------------------- | --------------------------------------------------- |
| Maven GroupId    | `com.fasterxml.jackson.core` 等         | `tools.jackson.core`、`tools.jackson.dataformat` 等 |
| 核心包名         | `com.fasterxml.jackson.*`               | `tools.jackson.*`                                   |
| JSON Mapper      | `ObjectMapper` 常作为默认入口           | `JsonMapper` 成为 Spring Boot 4 默认 JSON Bean      |
| 序列化器         | `JsonSerializer`                        | `ValueSerializer`                                   |
| 反序列化器       | `JsonDeserializer`                      | `ValueDeserializer`                                 |
| 上下文对象       | `SerializerProvider`                    | `SerializationContext`                              |
| Spring Boot 注解 | `@JsonComponent`                        | `@JacksonComponent`                                 |
| Boot 自定义入口  | `Jackson2ObjectMapperBuilderCustomizer` | `JsonMapperBuilderCustomizer`                       |

Jackson 3 还移除了 Jackson 2 中已废弃的方法、字段和类，并对部分核心实体进行了重命名。迁移时不能只改依赖版本，需要同步检查 import、序列化器、反序列化器、模块注册、测试用例和第三方组件兼容性。([GitHub](https://github.com/FasterXML/jackson/wiki/Jackson-Release-3.0))

另外，Jackson 3 将 Jackson 2 中常用的 Java 8 相关模块内置到 `jackson-databind`，包括构造参数名、`Optional` 类型和 `java.time` 日期时间类型支持。迁移后通常不再需要像 Jackson 2 一样显式注册 `jackson-module-parameter-names`、`jackson-datatype-jdk8`、`jackson-datatype-jsr310`。([GitHub](https://github.com/FasterXML/jackson/wiki/Jackson-Release-3.0?utm_source=chatgpt.com))

### 适用场景与影响范围

Spring Boot 4 Jackson 3 主要影响所有涉及 JSON 输入输出的位置，包括 REST 接口、HTTP 客户端、消息转换器、统一响应结构、异常响应、日志序列化、对象快照测试和第三方 SDK 适配。

主要适用场景如下：

| 场景            | 说明                                                         |
| --------------- | ------------------------------------------------------------ |
| REST API 响应   | Controller 返回 DTO、VO、集合、分页对象时自动序列化为 JSON   |
| 请求体解析      | `@RequestBody` 接收 JSON 并反序列化为 Java 对象              |
| 统一响应结构    | 将业务响应对象统一输出为固定 JSON 契约                       |
| 日期时间处理    | 对 `LocalDateTime`、`LocalDate`、`Instant` 等字段进行统一格式化 |
| 枚举处理        | 控制枚举输出 name、ordinal、自定义 code 或完整对象           |
| Long 精度控制   | 面向前端 JavaScript 场景，将大 Long 转字符串                 |
| 第三方 API 对接 | 对外部 JSON 数据进行反序列化、字段兼容、默认值处理           |
| 迁移改造        | 从 Spring Boot 3 + Jackson 2 升级到 Spring Boot 4 + Jackson 3 |

影响范围建议按以下顺序排查：

1. `pom.xml` 或 `build.gradle` 中是否显式声明了 Jackson 2 依赖。
2. Java 代码中是否存在 `com.fasterxml.jackson.databind.*`、`JsonSerializer`、`JsonDeserializer` 等 Jackson 2 类型。
3. 是否使用了 `Jackson2ObjectMapperBuilderCustomizer`、`@JsonComponent` 等 Jackson 2 时代的 Spring Boot 扩展点。
4. Web 层是否依赖自定义 `MappingJackson2HttpMessageConverter`。
5. 单元测试和接口快照测试是否依赖旧 JSON 格式。
6. 第三方依赖是否仍强依赖 Jackson 2。

## 环境与依赖

环境与依赖部分用于给出 Spring Boot 4 项目中 Jackson 3 的基础依赖写法，以及常见 JSON 处理依赖的职责边界。

### Spring Boot 4 项目依赖配置

Spring Boot 4 项目推荐使用 Spring Boot Parent 或 Spring Boot BOM 管理依赖版本，不建议手动指定 Jackson 3 的所有版本。这样可以减少 Jackson Core、Databind、Dataformat、Spring Framework 之间的版本不一致问题。

文件位置：`pom.xml`

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <!-- 使用 Spring Boot 统一管理依赖版本 -->
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>4.0.6</version>
        <relativePath/>
    </parent>

    <groupId>io.github.atengk</groupId>
    <artifactId>spring-boot4-jackson3-demo</artifactId>
    <version>1.0.0</version>
    <name>spring-boot4-jackson3-demo</name>

    <properties>
        <!-- Spring Boot 4 至少需要 Java 17 -->
        <java.version>17</java.version>
        <!-- Hutool 版本可按公司依赖规范统一管理 -->
        <hutool.version>5.8.40</hutool.version>
    </properties>

    <dependencies>
        <!-- Spring MVC Web 开发，间接包含 JSON 自动配置能力 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-webmvc</artifactId>
        </dependency>

        <!-- 参数校验，用于 @RequestBody DTO 校验 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <!-- Hutool 工具类，业务示例中用于集合、字符串、类型判断等常见处理 -->
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
            <version>${hutool.version}</version>
        </dependency>

        <!-- Lombok，简化 DTO、VO、日志对象代码 -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Spring Boot 测试，后续可用于 JSON 序列化和 WebMvcTest 验证 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

如果项目只做纯 JSON 工具封装，不引入 Web MVC，也可以单独引入 `spring-boot-starter-json`。但在常规 REST 服务中，`spring-boot-starter-webmvc` 更常见，因为它会同时提供 MVC、Servlet、参数绑定、消息转换器等能力。

### Jackson 3 模块依赖配置

Spring Boot 4 默认会通过 Starter 管理 Jackson 3 依赖。只有在使用额外数据格式时，才需要补充对应 Jackson 3 模块，例如 XML、YAML、CBOR、Smile 等。

常见模块如下：

| 依赖                                                | 用途                                 |
| --------------------------------------------------- | ------------------------------------ |
| `tools.jackson.core:jackson-databind`               | JSON 数据绑定核心，提供 `JsonMapper` |
| `tools.jackson.dataformat:jackson-dataformat-xml`   | XML 序列化与反序列化                 |
| `tools.jackson.dataformat:jackson-dataformat-yaml`  | YAML 序列化与反序列化                |
| `tools.jackson.dataformat:jackson-dataformat-cbor`  | CBOR 二进制 JSON 格式                |
| `tools.jackson.dataformat:jackson-dataformat-smile` | Smile 二进制 JSON 格式               |

Spring Framework 7 的 `JacksonJsonHttpMessageConverter` 使用 Jackson 3 的 `JsonMapper` 读写 JSON，默认支持 `application/json`，并要求存在 `tools.jackson.core:jackson-databind` 依赖。([Home](https://docs.spring.io/spring-framework/reference/web/webmvc/message-converters.html))

如果需要 XML 支持，可以增加以下依赖。

文件位置：`pom.xml`

```xml
<dependencies>
    <!-- Jackson 3 XML 数据格式支持，用于 application/xml 或 XML 文件处理 -->
    <dependency>
        <groupId>tools.jackson.dataformat</groupId>
        <artifactId>jackson-dataformat-xml</artifactId>
    </dependency>

    <!-- Jackson 3 YAML 数据格式支持，用于配置导入、配置导出或 YAML 文件处理 -->
    <dependency>
        <groupId>tools.jackson.dataformat</groupId>
        <artifactId>jackson-dataformat-yaml</artifactId>
    </dependency>
</dependencies>
```

注意不要在 Spring Boot 4 新项目中混用 Jackson 2 和 Jackson 3，除非是迁移期必须兼容。若两套依赖同时存在，需要明确 HTTP 消息转换优先级，否则可能出现运行时使用的 Mapper 与预期不一致的问题。

### 常用 JSON 处理依赖说明

Spring Boot 4 同时支持多种 JSON 映射库，但默认优先使用 Jackson 3。除非项目已有明确历史包袱，否则 REST API 服务建议统一使用 Jackson 3，避免同一服务中同时存在 Jackson、Gson、JSON-B 多套 JSON 契约。([Home](https://docs.spring.io/spring-boot/reference/features/json.html))

常用依赖选择建议如下：

| 场景                 | 推荐依赖                         | 说明                                 |
| -------------------- | -------------------------------- | ------------------------------------ |
| Spring MVC REST API  | `spring-boot-starter-webmvc`     | 默认集成 HTTP 消息转换器和 JSON 支持 |
| 纯 JSON 工具封装     | `spring-boot-starter-json`       | 不需要 Web 容器时使用                |
| JSON 字段校验        | `spring-boot-starter-validation` | 与 `@Valid`、`@Validated` 配合使用   |
| XML 数据格式         | `jackson-dataformat-xml`         | 额外支持 XML                         |
| YAML 数据格式        | `jackson-dataformat-yaml`        | 额外支持 YAML                        |
| Jackson 2 迁移期兼容 | `spring-boot-jackson2`           | 仅用于迁移过渡，不建议长期依赖       |

如果项目中存在以下依赖，需要重点确认是否为 Jackson 2 遗留依赖：

```xml
<!-- Jackson 2 旧依赖示例：Spring Boot 4 新项目不建议继续显式引入 -->
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
</dependency>
```

迁移到 Jackson 3 后，应优先使用 Spring Boot 4 的依赖管理，不要手动把 `com.fasterxml.jackson.*` 和 `tools.jackson.*` 拼接在同一个 JSON 处理链路中。

## 自动配置机制

自动配置机制用于说明 Spring Boot 4 如何创建 `JsonMapper`，如何把 Jackson 3 集成到 Spring MVC，以及业务代码如何验证当前项目使用的是 Jackson 3。

### JacksonAutoConfiguration

Spring Boot 4 的 Jackson 3 自动配置类是 `org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration`。该配置类在 classpath 中存在 `tools.jackson.databind.json.JsonMapper` 时生效，并负责为应用上下文提供 Jackson 3 相关 Bean。([Home](https://docs.spring.io/spring-boot/4.0/api/java/org/springframework/boot/jackson/autoconfigure/JacksonAutoConfiguration.html))

自动配置触发链路可以概括为：

```text
引入 spring-boot-starter-webmvc
        ↓
间接引入 spring-boot-starter-json
        ↓
classpath 中存在 Jackson 3
        ↓
触发 JacksonAutoConfiguration
        ↓
创建 JsonMapper Bean
        ↓
Spring MVC 注册 JacksonJsonHttpMessageConverter
        ↓
Controller 的 @RequestBody / @ResponseBody 自动使用 JSON 转换
```

在日常开发中，通常不需要直接引入 `JacksonAutoConfiguration`，也不需要手动创建 `JsonMapper`。只要依赖完整，Spring Boot 会自动完成 Bean 创建和 Web 层集成。

如果需要确认自动配置是否生效，可以在启动日志中打开条件评估报告，或者临时注入 `JsonMapper` 验证。

文件位置：`src/main/resources/application.yml`

```yaml
debug: true

spring:
  http:
    converters:
      # Spring MVC HTTP 消息转换优先使用 Jackson 3
      preferred-json-mapper: jackson
```

`spring.http.converters.preferred-json-mapper` 用于配置 HTTP 消息转换使用的首选 JSON Mapper，默认值是 `jackson`；可选项包含 `jackson`、废弃的 `jackson2`、`gson` 和 `jsonb`。当项目中存在多套 JSON 库时，应显式配置该值，避免消息转换器选择不符合预期。([Home](https://docs.spring.io/spring-boot/appendix/application-properties/index.html))

### ObjectMapper 自动装配

在 Spring Boot 4 + Jackson 3 中，自动装配的核心 Bean 是 `JsonMapper`，它是 Jackson 3 面向 JSON 数据绑定的 Mapper。与 Spring Boot 3 常见写法不同，新代码中不应再默认注入 Jackson 2 的 `com.fasterxml.jackson.databind.ObjectMapper`。

推荐写法如下。

文件位置：`src/main/java/io/github/atengk/jackson/controller/JsonDemoController.java`

```java
package io.github.atengk.jackson.controller;

import cn.hutool.core.map.MapUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Jackson 3 JSON 验证接口。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@RestController
public class JsonDemoController {

    private final JsonMapper jsonMapper;

    public JsonDemoController(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    /**
     * 验证 Controller 返回值序列化。
     *
     * @return JSON 响应数据
     */
    @GetMapping("/demo/jackson")
    public Map<String, Object> jackson() {
        Map<String, Object> data = MapUtil.<String, Object>builder()
                .put("id", 10001L)
                .put("name", "Spring Boot 4 Jackson 3")
                .put("createdAt", LocalDateTime.now())
                .put("mapperType", this.jsonMapper.getClass().getName())
                .build();

        log.info("返回 Jackson 3 验证数据，字段数量：{}", data.size());
        return data;
    }
}
```

启动项目后访问接口：

```bash
curl http://localhost:8080/demo/jackson
```

预期返回结构类似：

```json
{
  "id": 10001,
  "name": "Spring Boot 4 Jackson 3",
  "createdAt": "2026-05-09T12:00:00",
  "mapperType": "tools.jackson.databind.json.JsonMapper"
}
```

该接口主要用于验证两件事：第一，Spring 容器中已经存在 `JsonMapper` Bean；第二，Controller 返回的 Java 对象会被 Spring MVC 自动序列化为 JSON。

如果项目需要对 `JsonMapper` 做全局定制，Spring Boot 4 提供 `JsonMapperBuilderCustomizer`，用于基于 `JsonMapper.Builder` 微调自动配置结果。该接口从 Spring Boot 4.0.0 开始提供，适合放在后续“ObjectMapper 定制”章节展开。([Home](https://docs.spring.io/spring-boot/api/java/org/springframework/boot/jackson/autoconfigure/JsonMapperBuilderCustomizer.html))

### HttpMessageConverter 集成

`HttpMessageConverter` 是 Spring Web 中负责 HTTP 请求体和响应体转换的接口。Spring MVC REST Controller 中的 `@RequestBody`、`@ResponseBody`、`@RestController` 返回值，本质上都依赖消息转换器完成 Java 对象和 HTTP Body 之间的转换。Spring Framework 文档说明，`HttpMessageConverter` 用于通过输入流和输出流读写 HTTP 请求体与响应体，并会在 Spring MVC 服务端和 RestClient、RestTemplate 客户端中使用。([Home](https://docs.spring.io/spring-framework/reference/web/webmvc/message-converters.html))

在 Spring Boot 4 + Spring Framework 7 中，Jackson 3 对应的 JSON 消息转换器是：

```text
org.springframework.http.converter.json.JacksonJsonHttpMessageConverter
```

该转换器使用 Jackson 3 的 `JsonMapper` 读写 JSON，默认支持 `application/json` 和 `application/*+json`，并可通过 `supportedMediaTypes` 调整支持的媒体类型。([Home](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/http/converter/json/JacksonJsonHttpMessageConverter.html))

HTTP JSON 请求处理流程如下：

```text
客户端发送 JSON 请求
        ↓
请求头 Content-Type: application/json
        ↓
Spring MVC 查找可读 HttpMessageConverter
        ↓
JacksonJsonHttpMessageConverter 读取请求体
        ↓
JsonMapper 反序列化为 @RequestBody DTO
        ↓
Controller 执行业务逻辑
        ↓
返回 DTO / VO / Map / List
        ↓
Spring MVC 查找可写 HttpMessageConverter
        ↓
JacksonJsonHttpMessageConverter 序列化响应体
        ↓
客户端接收 JSON 响应
```

示例接口如下，用于同时验证请求体反序列化和响应体序列化。

文件位置：`src/main/java/io/github/atengk/jackson/controller/UserJsonController.java`

```java
package io.github.atengk.jackson.controller;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * 用户 JSON 请求与响应验证接口。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@RestController
public class UserJsonController {

    /**
     * 验证 @RequestBody 反序列化和响应体序列化。
     *
     * @param request 用户创建请求
     * @return 用户创建响应
     */
    @PostMapping("/demo/users")
    public UserCreateResponse createUser(@RequestBody UserCreateRequest request) {
        if (StrUtil.isBlank(request.username())) {
            log.info("用户创建失败，用户名为空");
            return new UserCreateResponse(false, "用户名不能为空", null, LocalDateTime.now());
        }

        log.info("创建用户成功，用户名：{}", request.username());
        return new UserCreateResponse(true, "创建成功", request.username(), LocalDateTime.now());
    }

    /**
     * 用户创建请求。
     *
     * @author Ateng
     * @since 2026-05-09
     */
    public record UserCreateRequest(String username, Integer age) {
    }

    /**
     * 用户创建响应。
     *
     * @author Ateng
     * @since 2026-05-09
     */
    public record UserCreateResponse(Boolean success, String message, String username, LocalDateTime createdAt) {
    }
}
```

调用验证：

```bash
curl -X POST http://localhost:8080/demo/users \
  -H "Content-Type: application/json" \
  -d '{"username":"ateng","age":18}'
```

预期响应：

```json
{
  "success": true,
  "message": "创建成功",
  "username": "ateng",
  "createdAt": "2026-05-09T12:00:00"
}
```

如果 `Content-Type` 不是 `application/json`，或者请求体不是合法 JSON，Spring MVC 可能无法选择正确的 `HttpMessageConverter`，最终抛出请求体不可读、媒体类型不支持或参数绑定异常。这部分异常处理建议放到后续“异常与兼容性处理”章节中集中说明。



## 常用配置

常用配置用于处理项目级 JSON 规则，例如字段命名、空值输出、日期时间格式、反序列化容错和枚举输出。Spring Boot 4 仍然通过 `spring.jackson.*` 暴露 Jackson 配置项，但底层类型已经切换为 Jackson 3 的 `tools.jackson.databind.*` 相关配置类型。([Home](https://docs.spring.io/spring-boot/api/java/org/springframework/boot/jackson/autoconfigure/JacksonProperties.html?utm_source=chatgpt.com))

### application 配置项

`application.yml` 适合配置全局、稳定、无业务侵入的 JSON 行为，例如字段命名策略、空值包含策略、时区、Locale、序列化特性和反序列化特性。对于跨接口统一规则，优先放在配置文件中；对于少数字段的特殊规则，再使用注解或自定义序列化器处理。

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  jackson:
    # 日期格式，主要影响传统 Date、Calendar 等日期类型
    date-format: yyyy-MM-dd HH:mm:ss

    # 全局时区，建议与服务器、数据库、业务地区保持一致
    time-zone: Asia/Shanghai

    # 本地化配置，影响部分区域相关格式化行为
    locale: zh_CN

    # 属性命名策略，常用于 Java camelCase 与 JSON snake_case 之间转换
    property-naming-strategy: SNAKE_CASE

    # 空值输出策略：non_null 表示 null 字段不输出
    default-property-inclusion: non_null

    # 是否自动查找并注册 Jackson Module，默认 true
    find-and-add-modules: true

    serialization:
      # 是否格式化输出 JSON，生产环境通常关闭
      indent-output: false

      # 空 Bean 是否抛出异常，关闭后空对象会输出 {}
      fail-on-empty-beans: false

    deserialization:
      # JSON 中存在 Java 对象没有的字段时是否报错，接口兼容场景建议关闭
      fail-on-unknown-properties: false

      # 浮点数传给整数类型时是否允许转换，严格契约场景建议关闭
      accept-float-as-int: false

    mapper:
      # 字段名大小写不敏感，适合兼容外部系统不稳定的 JSON 字段命名
      accept-case-insensitive-properties: true
```

常用配置项说明如下：

| 配置项                                      | 作用                             | 推荐场景                         |
| ------------------------------------------- | -------------------------------- | -------------------------------- |
| `spring.jackson.date-format`                | 配置日期格式字符串或日期格式类名 | 传统 `Date`、`Calendar` 输出     |
| `spring.jackson.time-zone`                  | 配置全局时区                     | 国内业务通常使用 `Asia/Shanghai` |
| `spring.jackson.locale`                     | 配置本地化区域                   | 涉及区域格式化时使用             |
| `spring.jackson.property-naming-strategy`   | 配置字段命名策略                 | 前端要求 `snake_case` 时使用     |
| `spring.jackson.default-property-inclusion` | 控制序列化时字段包含规则         | 空值字段是否输出                 |
| `spring.jackson.serialization.*`            | 控制序列化特性                   | 输出 JSON 时的全局行为           |
| `spring.jackson.deserialization.*`          | 控制反序列化特性                 | 解析 JSON 时的容错行为           |
| `spring.jackson.mapper.*`                   | 控制 Mapper 级别特性             | 字段发现、大小写兼容等行为       |

`spring.jackson.default-property-inclusion` 对应 Jackson `JsonInclude.Include` 枚举，`spring.jackson.serialization.*` 和 `spring.jackson.deserialization.*` 分别对应 Jackson 3 的 `SerializationFeature` 与 `DeserializationFeature`。([Home](https://docs.spring.io/spring-boot/api/java/org/springframework/boot/jackson/autoconfigure/JacksonProperties.html?utm_source=chatgpt.com))

### 日期时间格式配置

日期时间格式建议分层处理。全局配置负责传统日期类型和默认行为，字段注解负责接口契约精确控制，配置类负责复杂类型或公司级统一策略。对于 `LocalDateTime`、`LocalDate`、`Instant` 等 Java Time 类型，推荐在 DTO 层明确使用 `@JsonFormat` 或通过统一配置类处理，避免不同接口输出格式不一致。

文件位置：`src/main/java/io/github/atengk/jackson/vo/OrderTimeVO.java`

```java
package io.github.atengk.jackson.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * 订单时间响应对象。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderTimeVO {

    /**
     * 订单编号。
     */
    private Long orderId;

    /**
     * 创建时间，输出格式：yyyy-MM-dd HH:mm:ss。
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    /**
     * 业务日期，输出格式：yyyy-MM-dd。
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate businessDate;

    /**
     * 旧系统时间字段，Date 类型需要明确时区。
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date legacyTime;
}
```

文件位置：`src/main/java/io/github/atengk/jackson/controller/OrderTimeController.java`

```java
package io.github.atengk.jackson.controller;

import cn.hutool.core.date.DateUtil;
import io.github.atengk.jackson.vo.OrderTimeVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 订单时间格式验证接口。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@RestController
public class OrderTimeController {

    /**
     * 获取订单时间示例。
     *
     * @return 订单时间响应
     */
    @GetMapping("/demo/orders/time")
    public OrderTimeVO getOrderTime() {
        OrderTimeVO result = OrderTimeVO.builder()
                .orderId(10001L)
                .createdAt(LocalDateTime.of(2026, 5, 9, 10, 30, 15))
                .businessDate(LocalDate.of(2026, 5, 9))
                .legacyTime(DateUtil.parse("2026-05-09 10:30:15"))
                .build();

        log.info("返回订单时间格式示例，订单编号：{}", result.getOrderId());
        return result;
    }
}
```

验证接口：

```bash
curl http://localhost:8080/demo/orders/time
```

预期响应：

```json
{
  "order_id": 10001,
  "created_at": "2026-05-09 10:30:15",
  "business_date": "2026-05-09",
  "legacy_time": "2026-05-09 10:30:15"
}
```

这里的字段名变为 `order_id`、`created_at`、`business_date`，是因为前面的 `property-naming-strategy: SNAKE_CASE` 已经启用了全局下划线命名策略。

### 空值与默认值处理

空值处理分为两个方向：序列化时是否输出 `null` 字段，以及反序列化时客户端传入 `null` 后如何处理。前者通常使用 `spring.jackson.default-property-inclusion` 或 `@JsonInclude`，后者通常使用 `@JsonSetter`、业务默认值或参数校验处理。

`@JsonInclude` 用于声明属性序列化时的包含规则，常见值包括 `NON_NULL`、`NON_EMPTY`、`NON_DEFAULT` 等。该注解主要影响输出，不负责给入参自动补默认值。([Javadoc](https://www.javadoc.io/static/com.fasterxml.jackson.core/jackson-annotations/2.7.0/com/fasterxml/jackson/annotation/JsonInclude.html?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/jackson/dto/UserQueryRequest.java`

```java
package io.github.atengk.jackson.dto;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import lombok.Data;

/**
 * 用户查询请求。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
public class UserQueryRequest {

    /**
     * 页码，客户端传 null 时保持默认值。
     */
    @JsonSetter(nulls = Nulls.SKIP)
    private Integer pageNum = 1;

    /**
     * 每页数量，客户端传 null 时保持默认值。
     */
    @JsonSetter(nulls = Nulls.SKIP)
    private Integer pageSize = 10;

    /**
     * 用户名关键字。
     */
    private String username;
}
```

文件位置：`src/main/java/io/github/atengk/jackson/vo/UserQueryVO.java`

```java
package io.github.atengk.jackson.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 用户查询响应。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserQueryVO {

    /**
     * 当前页码。
     */
    private Integer pageNum;

    /**
     * 每页数量。
     */
    private Integer pageSize;

    /**
     * 用户名关键字，为 null 时不输出。
     */
    private String username;

    /**
     * 用户列表，为 null 时不输出。
     */
    private List<String> records;
}
```

文件位置：`src/main/java/io/github/atengk/jackson/controller/UserQueryController.java`

```java
package io.github.atengk.jackson.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.jackson.dto.UserQueryRequest;
import io.github.atengk.jackson.vo.UserQueryVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户查询 JSON 空值处理接口。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@RestController
public class UserQueryController {

    /**
     * 查询用户列表。
     *
     * @param request 查询请求
     * @return 查询响应
     */
    @PostMapping("/demo/users/query")
    public UserQueryVO queryUsers(@RequestBody UserQueryRequest request) {
        String username = StrUtil.blankToNull(request.getUsername());

        log.info("查询用户列表，页码：{}，每页数量：{}，用户名：{}",
                request.getPageNum(), request.getPageSize(), username);

        return UserQueryVO.builder()
                .pageNum(request.getPageNum())
                .pageSize(request.getPageSize())
                .username(username)
                .records(CollUtil.newArrayList("ateng", "jackson"))
                .build();
    }
}
```

验证请求：

```bash
curl -X POST http://localhost:8080/demo/users/query \
  -H "Content-Type: application/json" \
  -d '{"pageNum":null,"pageSize":null,"username":""}'
```

预期响应中 `page_num` 和 `page_size` 使用 DTO 默认值，`username` 因转换为 `null` 且响应类配置了 `NON_NULL`，不会输出。

```json
{
  "page_num": 1,
  "page_size": 10,
  "records": [
    "ateng",
    "jackson"
  ]
}
```

### 枚举序列化配置

枚举序列化需要先确定接口契约。内部系统可以直接使用枚举名称，但对外接口通常更推荐输出稳定的业务 code，避免 Java 枚举常量名变更影响前端或第三方系统。

常见枚举输出策略如下：

| 策略                   | JSON 示例                  | 适用场景             |
| ---------------------- | -------------------------- | -------------------- |
| 默认枚举名             | `"ENABLE"`                 | 内部接口、快速开发   |
| `@JsonValue` 输出 code | `1`                        | 前端只需要状态值     |
| 输出完整对象           | `{"code":1,"name":"启用"}` | 前端需要展示文案     |
| 自定义序列化器         | 按项目统一规范输出         | 多系统统一 JSON 契约 |

下面示例使用 `@JsonValue` 控制枚举输出，用 `@JsonCreator` 支持入参 code 反序列化。

文件位置：`src/main/java/io/github/atengk/jackson/enums/UserStatus.java`

```java
package io.github.atengk.jackson.enums;

import cn.hutool.core.util.ObjUtil;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 用户状态枚举。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Getter
public enum UserStatus {

    /**
     * 启用。
     */
    ENABLE(1, "启用"),

    /**
     * 禁用。
     */
    DISABLE(0, "禁用");

    /**
     * 状态编码。
     */
    @JsonValue
    private final Integer code;

    /**
     * 状态名称。
     */
    private final String name;

    UserStatus(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    /**
     * 根据状态编码获取枚举。
     *
     * @param code 状态编码
     * @return 用户状态枚举
     */
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static UserStatus of(Integer code) {
        for (UserStatus status : values()) {
            if (ObjUtil.equal(status.getCode(), code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("不支持的用户状态编码：" + code);
    }
}
```

文件位置：`src/main/java/io/github/atengk/jackson/vo/UserStatusVO.java`

```java
package io.github.atengk.jackson.vo;

import io.github.atengk.jackson.enums.UserStatus;
import lombok.Builder;
import lombok.Data;

/**
 * 用户状态响应。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
@Builder
public class UserStatusVO {

    /**
     * 用户编号。
     */
    private Long userId;

    /**
     * 用户状态，输出时使用枚举 code。
     */
    private UserStatus status;
}
```

输出示例：

```json
{
  "user_id": 10001,
  "status": 1
}
```

这种方式的优点是 JSON 契约稳定，缺点是前端如果需要展示文案，还需要额外维护 code 与 name 的映射。如果前端需要同时拿到 code 和 name，应改为输出完整状态对象或增加额外字段。

## ObjectMapper 定制

Spring Boot 4 中实际自动配置的是 Jackson 3 `JsonMapper`。定制全局 JSON 行为时，应优先使用 `JsonMapperBuilderCustomizer`，它可以在保留 Spring Boot 自动配置和 `application.yml` 配置的基础上继续追加配置。`JsonMapperBuilderCustomizer` 是 Spring Boot 4 提供的函数式接口，用于定制 `tools.jackson.databind.json.JsonMapper.Builder`。([Home](https://docs.spring.io/spring-boot/api/java/org/springframework/boot/jackson/autoconfigure/JsonMapperBuilderCustomizer.html?utm_source=chatgpt.com))

### Jackson2ObjectMapperBuilder 替代方案

Spring Boot 3 常见写法是提供 `Jackson2ObjectMapperBuilderCustomizer`，但在 Spring Boot 4 中，该接口位于 Jackson 2 兼容包下，并被标记为废弃且计划移除，官方方向是迁移到 Jackson 3。([Home](https://docs.spring.io/spring-boot/4.0-SNAPSHOT/api/java/org/springframework/boot/jackson2/autoconfigure/Jackson2ObjectMapperBuilderCustomizer.html?utm_source=chatgpt.com))

旧写法示例，不建议在 Spring Boot 4 新项目中继续使用：

```java
// Spring Boot 3 / Jackson 2 旧写法，Spring Boot 4 新项目不推荐
@Bean
public Jackson2ObjectMapperBuilderCustomizer jackson2Customizer() {
    return builder -> builder.indentOutput(true);
}
```

Spring Boot 4 推荐写法如下。

文件位置：`src/main/java/io/github/atengk/jackson/config/JacksonConfig.java`

```java
package io.github.atengk.jackson.config;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.SerializationFeature;

/**
 * Jackson 3 全局配置。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Configuration(proxyBeanMethods = false)
public class JacksonConfig {

    /**
     * 定制 Spring Boot 自动配置创建的 JsonMapper.Builder。
     *
     * @return JsonMapper 构建器定制器
     */
    @Bean
    public JsonMapperBuilderCustomizer jacksonJsonMapperCustomizer() {
        return builder -> builder
                // 生产环境通常关闭格式化输出，避免增加响应体积
                .disable(SerializationFeature.INDENT_OUTPUT)
                // 兼容外部系统多传字段
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                // 兼容 JSON 字段大小写差异
                .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES);
    }
}
```

这种方式不会直接替换 Spring Boot 自动创建的 `JsonMapper`，而是在自动配置链路上追加定制。相比手动声明一个新的 `JsonMapper` Bean，这种方式更适合项目级统一配置。

### JacksonCustomizer 使用方式

Spring Boot 4 官方自动配置包中没有一个名为 `JacksonCustomizer` 的通用接口；实际推荐使用的是 `JsonMapperBuilderCustomizer`、`XmlMapperBuilderCustomizer`、`CborMapperBuilderCustomizer` 等面向具体 Mapper 的定制器。这里可以把 `JacksonCustomizer` 作为项目内部对 Jackson 定制配置类或 Bean 方法的命名习惯，而不是独立官方 API。([Home](https://docs.spring.io/spring-boot/api/java/org/springframework/boot/jackson/autoconfigure/package-summary.html?utm_source=chatgpt.com))

项目中可以按以下方式组织自定义器。

文件位置：`src/main/java/io/github/atengk/jackson/config/JacksonCustomizerConfig.java`

```java
package io.github.atengk.jackson.config;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.SerializationFeature;

/**
 * Jackson 自定义器配置。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Configuration(proxyBeanMethods = false)
public class JacksonCustomizerConfig {

    /**
     * 基础序列化策略。
     *
     * @return JsonMapper 构建器定制器
     */
    @Bean
    public JsonMapperBuilderCustomizer baseJacksonCustomizer() {
        return builder -> builder
                .disable(SerializationFeature.INDENT_OUTPUT)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    /**
     * 严格反序列化策略。
     *
     * @return JsonMapper 构建器定制器
     */
    @Bean
    public JsonMapperBuilderCustomizer strictDeserializeCustomizer() {
        return builder -> builder
                // 禁止 1.2 自动截断为 1
                .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT);
    }
}
```

多个 `JsonMapperBuilderCustomizer` Bean 可以同时存在。需要控制执行顺序时，可以配合 `@Order` 使用；Spring Boot 自身的定制器也有顺序，项目定制器可以根据需要在其前后执行。([Home](https://docs.spring.io/spring-boot/4.1-SNAPSHOT/how-to/spring-mvc.html?utm_source=chatgpt.com))

### 全局序列化策略配置

全局序列化策略控制 Java 对象输出为 JSON 时的行为。常见策略包括是否格式化输出、空对象是否报错、字段是否排序、Map 是否按 Key 排序、空值字段是否输出等。

建议优先使用 `application.yml` 处理稳定策略：

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  jackson:
    # null 字段不输出
    default-property-inclusion: non_null

    serialization:
      # 关闭格式化输出
      indent-output: false

      # 空 Bean 输出 {}，不抛出异常
      fail-on-empty-beans: false

      # Map 按 Key 排序，适合快照测试或签名场景
      order-map-entries-by-keys: true
```

如果需要使用 Java 配置，可以使用 `JsonMapperBuilderCustomizer`。

文件位置：`src/main/java/io/github/atengk/jackson/config/JacksonSerializationConfig.java`

```java
package io.github.atengk.jackson.config;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.SerializationFeature;

/**
 * Jackson 全局序列化策略配置。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Configuration(proxyBeanMethods = false)
public class JacksonSerializationConfig {

    /**
     * 配置全局序列化特性。
     *
     * @return JsonMapper 构建器定制器
     */
    @Bean
    public JsonMapperBuilderCustomizer serializationCustomizer() {
        return builder -> builder
                // 关闭格式化输出
                .disable(SerializationFeature.INDENT_OUTPUT)
                // 空对象不抛出异常
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                // Map 按 Key 排序，方便快照测试和签名
                .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
    }
}
```

`SerializationFeature` 是 Jackson 3 中控制序列化开关的枚举，例如 `INDENT_OUTPUT` 控制缩进输出，`ORDER_MAP_ENTRIES_BY_KEYS` 控制 Map Key 排序，`FAIL_ON_EMPTY_BEANS` 控制空 Bean 是否抛出异常。([Javadoc](https://javadoc.io/static/tools.jackson.core/jackson-databind/3.0.0/tools.jackson.databind/tools/jackson/databind/SerializationFeature.html?utm_source=chatgpt.com))

### 全局反序列化策略配置

全局反序列化策略控制 JSON 转 Java 对象时的容错程度。接口对外暴露时，建议明确区分“兼容性”和“契约严格性”：业务入参可以适当兼容未知字段，但金额、数量、枚举、日期等关键字段应保持严格校验。

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  jackson:
    deserialization:
      # 兼容客户端多传字段
      fail-on-unknown-properties: false

      # 禁止浮点数自动转整数，避免金额、数量字段被截断
      accept-float-as-int: false

      # 不允许单值自动包装为数组，保持接口契约清晰
      accept-single-value-as-array: false
```

Java 配置方式如下。

文件位置：`src/main/java/io/github/atengk/jackson/config/JacksonDeserializationConfig.java`

```java
package io.github.atengk.jackson.config;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.DeserializationFeature;

/**
 * Jackson 全局反序列化策略配置。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Configuration(proxyBeanMethods = false)
public class JacksonDeserializationConfig {

    /**
     * 配置全局反序列化特性。
     *
     * @return JsonMapper 构建器定制器
     */
    @Bean
    public JsonMapperBuilderCustomizer deserializationCustomizer() {
        return builder -> builder
                // JSON 中存在 DTO 没有的字段时不报错
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                // 禁止浮点数自动转整数
                .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
                // 禁止单个值自动转数组
                .disable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
    }
}
```

`DeserializationFeature` 是 Jackson 3 中控制反序列化行为的枚举，例如 `FAIL_ON_UNKNOWN_PROPERTIES` 控制未知字段处理，`ACCEPT_FLOAT_AS_INT` 控制浮点数转整数，`ACCEPT_SINGLE_VALUE_AS_ARRAY` 控制单值是否可自动包装为数组。([Javadoc](https://javadoc.io/static/tools.jackson.core/jackson-databind/3.0.0/tools.jackson.databind/tools/jackson/databind/DeserializationFeature.html?utm_source=chatgpt.com))

## 注解使用

Jackson 注解适合处理字段级、类级或接口契约级的 JSON 规则。Spring Boot 4 使用 Jackson 3 时，大部分通用注解仍来自 `com.fasterxml.jackson.annotation`，例如 `@JsonProperty`、`@JsonIgnore`、`@JsonInclude`、`@JsonFormat`、`@JsonView`；但 Jackson Databind 相关注解，例如 `@JsonSerialize`、`@JsonDeserialize`，已经迁移到 `tools.jackson.databind.annotation` 包。([GitHub](https://github.com/FasterXML/jackson/blob/main/jackson3/MIGRATING_TO_JACKSON_3.md?utm_source=chatgpt.com))

### 常用序列化注解

序列化注解主要控制 Java 对象输出 JSON 时的字段名称、字段包含规则、时间格式、枚举输出和字段忽略策略。

常用注解如下：

| 注解             | 作用                   | 常见位置               |
| ---------------- | ---------------------- | ---------------------- |
| `@JsonProperty`  | 指定 JSON 字段名       | 字段、getter、构造参数 |
| `@JsonInclude`   | 控制字段是否输出       | 字段、类               |
| `@JsonFormat`    | 控制日期、枚举等格式   | 字段                   |
| `@JsonIgnore`    | 忽略字段输出           | 字段、getter           |
| `@JsonValue`     | 指定枚举或对象的输出值 | 方法、字段             |
| `@JsonView`      | 按视图输出部分字段     | 字段、方法、Controller |
| `@JsonSerialize` | 指定自定义序列化器     | 字段、类               |

文件位置：`src/main/java/io/github/atengk/jackson/vo/ProductVO.java`

```java
package io.github.atengk.jackson.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品响应对象。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductVO {

    /**
     * 商品编号，JSON 输出为 product_id。
     */
    @JsonProperty("product_id")
    private Long id;

    /**
     * 商品名称。
     */
    private String name;

    /**
     * 商品价格。
     */
    private BigDecimal price;

    /**
     * 创建时间。
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    /**
     * 内部备注，不输出到 JSON。
     */
    @JsonIgnore
    private String internalRemark;
}
```

输出示例：

```json
{
  "product_id": 10001,
  "name": "Jackson 3 实战文档",
  "price": 99.90,
  "created_at": "2026-05-09 10:30:15"
}
```

`@JsonProperty` 可指定外部 JSON 字段名，默认值为空时使用原字段名；`@JsonInclude` 可减少不必要的空字段输出。([Javadoc](https://www.javadoc.io/static/com.fasterxml.jackson.core/jackson-annotations/2.4.0/com/fasterxml/jackson/annotation/JsonProperty.html?utm_source=chatgpt.com))

### 常用反序列化注解

反序列化注解主要控制 JSON 入参如何绑定到 Java 对象，包括字段别名、必填字段、默认值处理、构造方法绑定和自定义反序列化。

常用注解如下：

| 注解                    | 作用                           | 常见位置           |
| ----------------------- | ------------------------------ | ------------------ |
| `@JsonProperty`         | 指定 JSON 字段与 Java 字段映射 | 字段、构造参数     |
| `@JsonAlias`            | 支持多个入参别名               | 字段、setter       |
| `@JsonSetter`           | 控制 null 处理                 | 字段、setter       |
| `@JsonIgnoreProperties` | 忽略未知字段                   | 类                 |
| `@JsonCreator`          | 指定构造方法或工厂方法         | 构造方法、静态方法 |
| `@JsonDeserialize`      | 指定自定义反序列化器           | 字段、类           |

文件位置：`src/main/java/io/github/atengk/jackson/dto/ProductCreateRequest.java`

```java
package io.github.atengk.jackson.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 商品创建请求。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductCreateRequest {

    /**
     * 商品名称，支持 name 和 product_name 两种入参。
     */
    @JsonAlias({"name", "product_name"})
    private String productName;

    /**
     * 商品价格。
     */
    @JsonProperty("price")
    private BigDecimal price;

    /**
     * 上架状态，客户端传 null 时保持默认值。
     */
    @JsonSetter(nulls = Nulls.SKIP)
    private Boolean enabled = true;
}
```

请求示例：

```bash
curl -X POST http://localhost:8080/demo/products \
  -H "Content-Type: application/json" \
  -d '{"product_name":"Jackson 3 文档","price":99.90,"enabled":null,"extra":"ignore"}'
```

在该示例中，`product_name` 可以正常绑定到 `productName`，`enabled` 传入 `null` 时保留默认值 `true`，`extra` 字段会被忽略。

### 字段命名与忽略策略

字段命名策略可以在全局、类级和字段级处理。全局命名适合统一规范，类级命名适合特殊 DTO，字段级命名适合少数字段兼容历史接口。

字段命名策略优先级建议如下：

| 层级     | 方式                                      | 适用场景                |
| -------- | ----------------------------------------- | ----------------------- |
| 全局     | `spring.jackson.property-naming-strategy` | 全项目统一 `snake_case` |
| 类级     | `@JsonNaming`                             | 单个 DTO 特殊命名       |
| 字段级   | `@JsonProperty`                           | 少数字段兼容历史名称    |
| 入参别名 | `@JsonAlias`                              | 接收多个历史字段名      |
| 忽略字段 | `@JsonIgnore`、`@JsonIgnoreProperties`    | 屏蔽敏感字段或未知字段  |

Jackson 3 中 `@JsonNaming` 属于 Databind 注解，应使用 `tools.jackson.databind.annotation.JsonNaming` 包。Jackson 3 迁移文档明确说明，`jackson-annotations` 中的通用注解仍保留 `com.fasterxml.jackson.annotation` 包，但 Databind 注解迁移到 `tools.jackson.databind.annotation`。([GitHub](https://github.com/FasterXML/jackson/blob/main/jackson3/MIGRATING_TO_JACKSON_3.md?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/jackson/vo/AccountVO.java`

```java
package io.github.atengk.jackson.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.time.LocalDateTime;

/**
 * 账户响应对象。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class AccountVO {

    /**
     * 账户编号，受 SnakeCaseStrategy 影响输出为 account_id。
     */
    private Long accountId;

    /**
     * 登录账号。
     */
    private String username;

    /**
     * 手机号，使用字段级名称覆盖默认命名策略。
     */
    @JsonProperty("mobile")
    private String phoneNumber;

    /**
     * 密码摘要，不允许输出。
     */
    @JsonIgnore
    private String passwordHash;

    /**
     * 创建时间，受 SnakeCaseStrategy 影响输出为 created_at。
     */
    private LocalDateTime createdAt;
}
```

输出示例：

```json
{
  "account_id": 10001,
  "username": "ateng",
  "mobile": "13800000000",
  "created_at": "2026-05-09T10:30:15"
}
```

注意不要在实体类上随意使用大量 JSON 注解。实体类通常服务于数据库映射，DTO/VO 服务于接口契约。接口字段命名、忽略策略、时间格式等规则建议优先放在 DTO/VO 中，避免数据库模型和接口模型耦合。

### JsonView 场景使用

`@JsonView` 用于同一个对象在不同接口场景下输出不同字段。例如列表接口只输出摘要字段，详情接口输出完整字段。Jackson 注解文档说明，`@JsonView` 通过视图类标识字段属于哪个视图，子视图可以包含父视图字段。([FasterXML](https://fasterxml.github.io/jackson-annotations/javadoc/2.10/com/fasterxml/jackson/annotation/JsonView.html?utm_source=chatgpt.com))

文件结构如下：

```text
src/main/java/io/github/atengk/jackson/view/UserViews.java
src/main/java/io/github/atengk/jackson/vo/UserViewVO.java
src/main/java/io/github/atengk/jackson/controller/UserViewController.java
```

文件位置：`src/main/java/io/github/atengk/jackson/view/UserViews.java`

```java
package io.github.atengk.jackson.view;

/**
 * 用户 JSON 视图定义。
 *
 * @author Ateng
 * @since 2026-05-09
 */
public class UserViews {

    /**
     * 摘要视图，用于列表接口。
     *
     * @author Ateng
     * @since 2026-05-09
     */
    public interface Summary {
    }

    /**
     * 详情视图，用于详情接口，包含摘要视图字段。
     *
     * @author Ateng
     * @since 2026-05-09
     */
    public interface Detail extends Summary {
    }
}
```

文件位置：`src/main/java/io/github/atengk/jackson/vo/UserViewVO.java`

```java
package io.github.atengk.jackson.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonView;
import io.github.atengk.jackson.view.UserViews;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户视图响应对象。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
@Builder
public class UserViewVO {

    /**
     * 用户编号，摘要和详情均输出。
     */
    @JsonView(UserViews.Summary.class)
    private Long userId;

    /**
     * 用户名，摘要和详情均输出。
     */
    @JsonView(UserViews.Summary.class)
    private String username;

    /**
     * 手机号，仅详情输出。
     */
    @JsonView(UserViews.Detail.class)
    private String phone;

    /**
     * 邮箱，仅详情输出。
     */
    @JsonView(UserViews.Detail.class)
    private String email;

    /**
     * 创建时间，仅详情输出。
     */
    @JsonView(UserViews.Detail.class)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
```

文件位置：`src/main/java/io/github/atengk/jackson/controller/UserViewController.java`

```java
package io.github.atengk.jackson.controller;

import com.fasterxml.jackson.annotation.JsonView;
import io.github.atengk.jackson.view.UserViews;
import io.github.atengk.jackson.vo.UserViewVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * 用户 JsonView 验证接口。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@RestController
public class UserViewController {

    /**
     * 获取用户摘要信息。
     *
     * @return 用户摘要信息
     */
    @JsonView(UserViews.Summary.class)
    @GetMapping("/demo/users/view/summary")
    public UserViewVO summary() {
        log.info("返回用户摘要视图");
        return buildUserView();
    }

    /**
     * 获取用户详情信息。
     *
     * @return 用户详情信息
     */
    @JsonView(UserViews.Detail.class)
    @GetMapping("/demo/users/view/detail")
    public UserViewVO detail() {
        log.info("返回用户详情视图");
        return buildUserView();
    }

    /**
     * 构建用户视图对象。
     *
     * @return 用户视图对象
     */
    private UserViewVO buildUserView() {
        return UserViewVO.builder()
                .userId(10001L)
                .username("ateng")
                .phone("13800000000")
                .email("ateng@example.com")
                .createdAt(LocalDateTime.of(2026, 5, 9, 10, 30, 15))
                .build();
    }
}
```

摘要接口验证：

```bash
curl http://localhost:8080/demo/users/view/summary
```

响应示例：

```json
{
  "user_id": 10001,
  "username": "ateng"
}
```

详情接口验证：

```bash
curl http://localhost:8080/demo/users/view/detail
```

响应示例：

```json
{
  "user_id": 10001,
  "username": "ateng",
  "phone": "13800000000",
  "email": "ateng@example.com",
  "created_at": "2026-05-09 10:30:15"
}
```

`JsonView` 适合少量视图差异场景。如果同一个对象在不同接口中字段差异较大，建议直接拆分为不同 VO，例如 `UserSummaryVO` 和 `UserDetailVO`，这样接口契约更清晰，测试和维护成本更低。



## 自定义序列化与反序列化

自定义序列化与反序列化用于处理默认 JSON 映射无法满足业务契约的场景，例如金额格式、手机号脱敏、枚举扩展对象、Long 精度、第三方接口特殊字段格式等。Spring Boot 4 使用 Jackson 3，原 Jackson 2 中常说的 `JsonSerializer` / `JsonDeserializer` 在 Jackson 3 中对应 `ValueSerializer` / `ValueDeserializer`；Spring Boot 4 也将 `@JsonComponent` 替换为 `@JacksonComponent`。([Javadoc](https://www.javadoc.io/static/tools.jackson.core/jackson-databind/3.0.2/tools.jackson.databind/tools/jackson/databind/ValueSerializer.html?utm_source=chatgpt.com))

### 自定义 JsonSerializer

自定义序列化器用于控制 Java 对象输出 JSON 时的格式。Jackson 3 中推荐继承 `ValueSerializer<T>`，并实现 `serialize(T value, JsonGenerator gen, SerializationContext context)` 方法；`SerializationContext` 是 Jackson 3 中替代 Jackson 2 `SerializerProvider` 的上下文类型。([Javadoc](https://www.javadoc.io/static/tools.jackson.core/jackson-databind/3.0.2/tools.jackson.databind/tools/jackson/databind/ValueSerializer.html?utm_source=chatgpt.com))

下面示例将金额对象 `MoneyAmount` 序列化为两位小数字符串，例如 `99.90`。

文件结构如下：

```text
src/main/java/io/github/atengk/jackson/model/MoneyAmount.java
src/main/java/io/github/atengk/jackson/serializer/MoneyAmountSerializer.java
src/main/java/io/github/atengk/jackson/vo/OrderAmountVO.java
src/main/java/io/github/atengk/jackson/controller/OrderAmountController.java
```

文件位置：`src/main/java/io/github/atengk/jackson/model/MoneyAmount.java`

```java
package io.github.atengk.jackson.model;

import io.github.atengk.jackson.deserializer.MoneyAmountDeserializer;
import io.github.atengk.jackson.serializer.MoneyAmountSerializer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonSerialize;

import java.math.BigDecimal;

/**
 * 金额对象。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonSerialize(using = MoneyAmountSerializer.class)
@JsonDeserialize(using = MoneyAmountDeserializer.class)
public class MoneyAmount {

    /**
     * 金额数值。
     */
    private BigDecimal value;
}
```

文件位置：`src/main/java/io/github/atengk/jackson/serializer/MoneyAmountSerializer.java`

```java
package io.github.atengk.jackson.serializer;

import cn.hutool.core.util.NumberUtil;
import io.github.atengk.jackson.model.MoneyAmount;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

import java.math.BigDecimal;

/**
 * 金额序列化器。
 *
 * @author Ateng
 * @since 2026-05-09
 */
public class MoneyAmountSerializer extends ValueSerializer<MoneyAmount> {

    @Override
    public void serialize(MoneyAmount value, JsonGenerator gen, SerializationContext context) throws JacksonException {
        BigDecimal amount = value.getValue() == null ? BigDecimal.ZERO : value.getValue();
        gen.writeString(NumberUtil.decimalFormat("#0.00", amount));
    }
}
```

文件位置：`src/main/java/io/github/atengk/jackson/vo/OrderAmountVO.java`

```java
package io.github.atengk.jackson.vo;

import io.github.atengk.jackson.model.MoneyAmount;
import lombok.Builder;
import lombok.Data;

/**
 * 订单金额响应对象。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
@Builder
public class OrderAmountVO {

    /**
     * 订单编号。
     */
    private Long orderId;

    /**
     * 实付金额。
     */
    private MoneyAmount payAmount;
}
```

文件位置：`src/main/java/io/github/atengk/jackson/controller/OrderAmountController.java`

```java
package io.github.atengk.jackson.controller;

import io.github.atengk.jackson.model.MoneyAmount;
import io.github.atengk.jackson.vo.OrderAmountVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

/**
 * 订单金额序列化验证接口。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@RestController
public class OrderAmountController {

    @GetMapping("/demo/orders/amount")
    public OrderAmountVO getOrderAmount() {
        OrderAmountVO result = OrderAmountVO.builder()
                .orderId(10001L)
                .payAmount(new MoneyAmount(new BigDecimal("99.9")))
                .build();

        log.info("返回订单金额序列化示例，订单编号：{}", result.getOrderId());
        return result;
    }
}
```

验证接口：

```bash
curl http://localhost:8080/demo/orders/amount
```

响应示例：

```json
{
  "order_id": 10001,
  "pay_amount": "99.90"
}
```

这里的 `pay_amount` 原本是一个 Java 对象，但通过自定义序列化器输出为字符串。该方式适合字段格式固定、多个接口复用同一格式的场景。

### 自定义 JsonDeserializer

自定义反序列化器用于控制 JSON 输入如何转换为 Java 对象。Jackson 3 中推荐继承 `ValueDeserializer<T>`，并实现 `deserialize(JsonParser parser, DeserializationContext context)` 方法。Jackson 3 官方 API 说明，`ValueDeserializer` 是 `ObjectMapper` 和 `ObjectReader` 从 JSON 读取对象时使用的基础抽象，常规自定义场景也可以继承 `StdDeserializer` 或其子类。([Javadoc](https://javadoc.io/static/tools.jackson.core/jackson-databind/3.0.0/tools.jackson.databind/tools/jackson/databind/ValueDeserializer.html?utm_source=chatgpt.com))

下面继续给 `MoneyAmount` 增加反序列化能力，使前端可以提交 `"99.90"`、`99.90` 这类金额值。

文件位置：`src/main/java/io/github/atengk/jackson/deserializer/MoneyAmountDeserializer.java`

```java
package io.github.atengk.jackson.deserializer;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.jackson.model.MoneyAmount;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

import java.math.BigDecimal;

/**
 * 金额反序列化器。
 *
 * @author Ateng
 * @since 2026-05-09
 */
public class MoneyAmountDeserializer extends ValueDeserializer<MoneyAmount> {

    @Override
    public MoneyAmount deserialize(JsonParser parser, DeserializationContext context) throws JacksonException {
        String text = StrUtil.trim(parser.getText());

        if (StrUtil.isBlank(text)) {
            return new MoneyAmount(BigDecimal.ZERO);
        }

        try {
            return new MoneyAmount(NumberUtil.toBigDecimal(text));
        } catch (Exception ex) {
            throw new IllegalArgumentException("金额格式不正确：" + text, ex);
        }
    }
}
```

文件位置：`src/main/java/io/github/atengk/jackson/dto/OrderPayRequest.java`

```java
package io.github.atengk.jackson.dto;

import io.github.atengk.jackson.model.MoneyAmount;
import lombok.Data;

/**
 * 订单支付请求。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
public class OrderPayRequest {

    /**
     * 订单编号。
     */
    private Long orderId;

    /**
     * 支付金额。
     */
    private MoneyAmount payAmount;
}
```

文件位置：`src/main/java/io/github/atengk/jackson/controller/OrderPayController.java`

```java
package io.github.atengk.jackson.controller;

import io.github.atengk.jackson.dto.OrderPayRequest;
import io.github.atengk.jackson.vo.OrderAmountVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 订单支付反序列化验证接口。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@RestController
public class OrderPayController {

    @PostMapping("/demo/orders/pay")
    public OrderAmountVO pay(@RequestBody OrderPayRequest request) {
        log.info("接收订单支付请求，订单编号：{}，支付金额：{}",
                request.getOrderId(), request.getPayAmount().getValue());

        return OrderAmountVO.builder()
                .orderId(request.getOrderId())
                .payAmount(request.getPayAmount())
                .build();
    }
}
```

验证请求：

```bash
curl -X POST http://localhost:8080/demo/orders/pay \
  -H "Content-Type: application/json" \
  -d '{"order_id":10001,"pay_amount":"199.8"}'
```

响应示例：

```json
{
  "order_id": 10001,
  "pay_amount": "199.80"
}
```

这个示例中，入参 `"199.8"` 会被反序列化为 `MoneyAmount`，响应时又通过序列化器输出为 `"199.80"`。这种方式可以把格式兼容逻辑集中到 Jackson 层，避免 Controller 中到处写转换代码。

### Module 注册方式

Module 注册适合项目级、模块级的统一扩展。Spring Boot 4 会自动注册应用上下文中的 `JacksonModule` Bean，并把它们应用到自动配置的 `JsonMapper.Builder` 以及由该 Builder 创建的 `JsonMapper` 实例中。官方文档也说明，`spring.jackson.find-and-add-modules` 默认会通过模块发现机制注册可用模块；如需禁用，可设置为 `false`。([Home](https://docs.spring.io/spring-boot/how-to/spring-mvc.html?utm_source=chatgpt.com))

如果使用 Module 注册方式，应去掉 `MoneyAmount` 类上的 `@JsonSerialize` 和 `@JsonDeserialize` 注解，避免同一类型同时存在多套注册来源。

文件位置：`src/main/java/io/github/atengk/jackson/config/JacksonModuleConfig.java`

```java
package io.github.atengk.jackson.config;

import io.github.atengk.jackson.deserializer.MoneyAmountDeserializer;
import io.github.atengk.jackson.model.MoneyAmount;
import io.github.atengk.jackson.serializer.MoneyAmountSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.module.SimpleModule;

/**
 * Jackson 模块注册配置。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Configuration(proxyBeanMethods = false)
public class JacksonModuleConfig {

    @Bean
    public JacksonModule moneyAmountJacksonModule() {
        SimpleModule module = new SimpleModule("money-amount-jackson-module");

        // 注册金额序列化器
        module.addSerializer(MoneyAmount.class, new MoneyAmountSerializer());

        // 注册金额反序列化器
        module.addDeserializer(MoneyAmount.class, new MoneyAmountDeserializer());

        return module;
    }
}
```

Module 注册方式和注解方式的选择建议如下：

| 方式                                  | 适用场景                                         |
| ------------------------------------- | ------------------------------------------------ |
| `@JsonSerialize` / `@JsonDeserialize` | 单个 DTO、VO、字段或类型的局部规则               |
| `@JacksonComponent`                   | 希望序列化器作为 Spring Bean，被组件扫描自动注册 |
| `JacksonModule` Bean                  | 项目级、模块级、Starter 级 JSON 扩展             |
| `JsonMapperBuilderCustomizer`         | 修改 Mapper 特性、注册模块、调整全局构建策略     |

Spring Boot 4 的 `@JacksonComponent` 可直接标注 `ValueSerializer`、`ValueDeserializer` 或 `KeyDeserializer`，也可以标注一个包含内部序列化器和反序列化器的类。所有 `@JacksonComponent` Bean 会被 `JacksonComponentModule` 注册到 Jackson。([Home](https://docs.spring.io/spring-boot/api/java/org/springframework/boot/jackson/JacksonComponent.html?utm_source=chatgpt.com))

使用 `@JacksonComponent` 的写法如下：

文件位置：`src/main/java/io/github/atengk/jackson/component/MoneyAmountJacksonComponent.java`

```java
package io.github.atengk.jackson.component;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.jackson.model.MoneyAmount;
import org.springframework.boot.jackson.JacksonComponent;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.ValueSerializer;

import java.math.BigDecimal;

/**
 * 金额 Jackson 组件。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@JacksonComponent
public class MoneyAmountJacksonComponent {

    /**
     * 金额序列化器。
     *
     * @author Ateng
     * @since 2026-05-09
     */
    public static class Serializer extends ValueSerializer<MoneyAmount> {

        @Override
        public void serialize(MoneyAmount value, JsonGenerator gen, SerializationContext context) throws JacksonException {
            BigDecimal amount = value.getValue() == null ? BigDecimal.ZERO : value.getValue();
            gen.writeString(NumberUtil.decimalFormat("#0.00", amount));
        }
    }

    /**
     * 金额反序列化器。
     *
     * @author Ateng
     * @since 2026-05-09
     */
    public static class Deserializer extends ValueDeserializer<MoneyAmount> {

        @Override
        public MoneyAmount deserialize(JsonParser parser, DeserializationContext context) throws JacksonException {
            String text = StrUtil.trim(parser.getText());
            if (StrUtil.isBlank(text)) {
                return new MoneyAmount(BigDecimal.ZERO);
            }
            return new MoneyAmount(NumberUtil.toBigDecimal(text));
        }
    }
}
```

实际项目中不要同时使用注解注册、Module 注册、`@JacksonComponent` 注册同一个类型，否则会增加排查难度。建议团队约定一种主要方式：业务项目常用 `@JacksonComponent`，公共 Starter 常用 `JacksonModule` Bean。

### 泛型类型处理

泛型类型处理主要出现在手动调用 `JsonMapper` 时，例如读取 `ApiResult<List<UserItemVO>>`、`Map<String, List<UserItemVO>>`、分页对象等。因为 Java 泛型存在类型擦除，不能只传 `ApiResult.class`，否则内部的 `List<UserItemVO>` 会丢失具体元素类型。Jackson 3 的 `ObjectMapper` / `JsonMapper` 支持通过 `TypeReference<T>` 或 `JavaType` 读取泛型类型。([Javadoc](https://javadoc.io/static/tools.jackson.core/jackson-databind/3.0.0/tools.jackson.databind/tools/jackson/databind/ObjectMapper.html?utm_source=chatgpt.com))

下面示例演示如何使用 `TypeReference` 处理统一响应泛型。

文件位置：`src/main/java/io/github/atengk/jackson/common/ApiResult.java`

```java
package io.github.atengk.jackson.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一接口响应结构。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResult<T> {

    /**
     * 是否成功。
     */
    private Boolean success;

    /**
     * 响应编码。
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
        return new ApiResult<>(true, 0, "操作成功", data);
    }

    public static <T> ApiResult<T> fail(Integer code, String message) {
        return new ApiResult<>(false, code, message, null);
    }
}
```

文件位置：`src/main/java/io/github/atengk/jackson/vo/UserItemVO.java`

```java
package io.github.atengk.jackson.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户列表项响应对象。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserItemVO {

    /**
     * 用户编号。
     */
    private Long userId;

    /**
     * 用户名。
     */
    private String username;
}
```

文件位置：`src/main/java/io/github/atengk/jackson/service/JsonGenericService.java`

```java
package io.github.atengk.jackson.service;

import cn.hutool.core.collection.CollUtil;
import io.github.atengk.jackson.common.ApiResult;
import io.github.atengk.jackson.vo.UserItemVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

/**
 * JSON 泛型处理服务。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JsonGenericService {

    private final JsonMapper jsonMapper;

    public String writeUserResultJson() throws JacksonException {
        List<UserItemVO> users = CollUtil.newArrayList(
                new UserItemVO(10001L, "ateng"),
                new UserItemVO(10002L, "jackson")
        );

        ApiResult<List<UserItemVO>> result = ApiResult.success(users);
        log.info("序列化用户泛型响应，用户数量：{}", users.size());
        return jsonMapper.writeValueAsString(result);
    }

    public ApiResult<List<UserItemVO>> readUserResultJson(String json) throws JacksonException {
        ApiResult<List<UserItemVO>> result = jsonMapper.readValue(
                json,
                new TypeReference<ApiResult<List<UserItemVO>>>() {
                }
        );

        log.info("反序列化用户泛型响应，响应编码：{}", result.getCode());
        return result;
    }
}
```

使用泛型时需要注意：Controller 正常返回 `ApiResult<List<UserItemVO>>` 时，Spring MVC 可以根据方法返回值签名推断类型；只有手动调用 `JsonMapper.readValue` 时，才需要显式提供 `TypeReference` 或 `JavaType`。

## REST 接口集成

REST 接口集成说明 Jackson 3 在 Controller 请求体、响应体和统一响应结构中的使用方式。Spring Framework 7 的 `JacksonJsonHttpMessageConverter` 负责通过 Jackson 3 `JsonMapper` 读写 JSON，默认支持 `application/json` 和 `application/*+json`。([Home](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/http/converter/json/JacksonJsonHttpMessageConverter.html?utm_source=chatgpt.com))

### Controller JSON 响应处理

Controller 返回普通 Java 对象时，Spring MVC 会通过合适的 `HttpMessageConverter` 写入响应体。对于 JSON 响应，在 Spring Boot 4 + Jackson 3 项目中，通常由 `JacksonJsonHttpMessageConverter` 使用 `JsonMapper` 完成序列化。([Home](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/http/converter/json/JacksonJsonHttpMessageConverter.html?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/jackson/controller/UserRestController.java`

```java
package io.github.atengk.jackson.controller;

import cn.hutool.core.collection.CollUtil;
import io.github.atengk.jackson.vo.UserItemVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户 REST JSON 响应接口。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@RestController
public class UserRestController {

    @GetMapping("/api/users")
    public List<UserItemVO> listUsers() {
        List<UserItemVO> users = CollUtil.newArrayList(
                new UserItemVO(10001L, "ateng"),
                new UserItemVO(10002L, "jackson")
        );

        log.info("查询用户列表，用户数量：{}", users.size());
        return users;
    }
}
```

验证接口：

```bash
curl http://localhost:8080/api/users
```

响应示例：

```json
[
  {
    "user_id": 10001,
    "username": "ateng"
  },
  {
    "user_id": 10002,
    "username": "jackson"
  }
]
```

这里字段输出为 `user_id`，是因为前文配置了 `spring.jackson.property-naming-strategy: SNAKE_CASE`。如果某个字段需要特殊命名，应优先在 DTO/VO 字段上使用 `@JsonProperty`，不要在 Controller 中手动拼接 `Map`。

### RequestBody 反序列化处理

`@RequestBody` 用于把 HTTP 请求体中的 JSON 反序列化为 Java 对象。实际处理流程是：Spring MVC 根据 `Content-Type` 找到可读的 `HttpMessageConverter`，再由 Jackson 3 的 `JsonMapper` 读取请求体并创建 DTO 对象。请求体字段校验建议配合 `spring-boot-starter-validation` 和 `@Valid` 使用。

文件位置：`src/main/java/io/github/atengk/jackson/dto/UserCreateRequest.java`

```java
package io.github.atengk.jackson.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 用户创建请求。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
public class UserCreateRequest {

    /**
     * 用户名。
     */
    @NotBlank(message = "用户名不能为空")
    private String username;

    /**
     * 年龄。
     */
    @Min(value = 1, message = "年龄不能小于 1")
    @Max(value = 120, message = "年龄不能大于 120")
    private Integer age;
}
```

文件位置：`src/main/java/io/github/atengk/jackson/vo/UserCreateVO.java`

```java
package io.github.atengk.jackson.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户创建响应。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
@Builder
public class UserCreateVO {

    /**
     * 用户编号。
     */
    private Long userId;

    /**
     * 用户名。
     */
    private String username;

    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;
}
```

文件位置：`src/main/java/io/github/atengk/jackson/controller/UserCreateController.java`

```java
package io.github.atengk.jackson.controller;

import cn.hutool.core.util.IdUtil;
import io.github.atengk.jackson.dto.UserCreateRequest;
import io.github.atengk.jackson.vo.UserCreateVO;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * 用户创建请求体反序列化接口。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@RestController
public class UserCreateController {

    @PostMapping("/api/users")
    public UserCreateVO createUser(@Valid @RequestBody UserCreateRequest request) {
        Long userId = IdUtil.getSnowflakeNextId();

        log.info("创建用户成功，用户编号：{}，用户名：{}", userId, request.getUsername());
        return UserCreateVO.builder()
                .userId(userId)
                .username(request.getUsername())
                .createdAt(LocalDateTime.now())
                .build();
    }
}
```

验证请求：

```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"username":"ateng","age":18}'
```

响应示例：

```json
{
  "user_id": 887654321012345678,
  "username": "ateng",
  "created_at": "2026-05-09T10:30:15"
}
```

如果请求体不是合法 JSON，Jackson 会在反序列化阶段抛出解析异常，通常由 Spring MVC 转换为请求体不可读异常。该类异常建议在后续“异常与兼容性处理”章节中通过 `@RestControllerAdvice` 统一处理。

### ResponseBody 序列化处理

`@ResponseBody` 用于把 Controller 方法返回值写入 HTTP 响应体。`@RestController` 等价于 `@Controller` + `@ResponseBody`，因此 REST 接口通常直接使用 `@RestController`。

文件位置：`src/main/java/io/github/atengk/jackson/controller/ProductResponseController.java`

```java
package io.github.atengk.jackson.controller;

import io.github.atengk.jackson.vo.ProductVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品响应体序列化接口。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@RestController
public class ProductResponseController {

    @GetMapping("/api/products/current")
    public ProductVO currentProduct() {
        ProductVO product = ProductVO.builder()
                .id(10001L)
                .name("Spring Boot 4 Jackson 3 开发文档")
                .price(new BigDecimal("99.90"))
                .createdAt(LocalDateTime.of(2026, 5, 9, 10, 30, 15))
                .internalRemark("内部备注不允许输出")
                .build();

        log.info("返回当前商品信息，商品编号：{}", product.getId());
        return product;
    }
}
```

响应示例：

```json
{
  "product_id": 10001,
  "name": "Spring Boot 4 Jackson 3 开发文档",
  "price": 99.90,
  "created_at": "2026-05-09 10:30:15"
}
```

响应体序列化建议遵循以下规则：

| 规则                   | 说明                                                     |
| ---------------------- | -------------------------------------------------------- |
| Controller 返回 DTO/VO | 不直接返回 Entity，避免数据库字段泄露                    |
| 时间字段明确格式       | 对外接口建议使用 `@JsonFormat` 或统一配置                |
| 敏感字段显式忽略       | 密码、密钥、内部备注等字段使用 `@JsonIgnore` 或不放入 VO |
| 避免返回裸 `Map`       | 复杂接口建议定义明确 VO，保持 JSON 契约稳定              |
| 大 Long 按需转字符串   | 面向前端 JavaScript 时避免精度丢失                       |

### 统一响应结构处理

统一响应结构用于让所有 REST API 返回固定格式，例如 `success`、`code`、`message`、`data`。最简单的方式是 Controller 手动返回 `ApiResult<T>`；更统一的方式是使用 `ResponseBodyAdvice` 自动包装响应体。

推荐先定义统一响应对象。

文件位置：`src/main/java/io/github/atengk/jackson/common/ApiResult.java`

```java
package io.github.atengk.jackson.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一接口响应结构。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResult<T> {

    /**
     * 是否成功。
     */
    private Boolean success;

    /**
     * 响应编码。
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
        return new ApiResult<>(true, 0, "操作成功", data);
    }

    public static ApiResult<Void> success() {
        return new ApiResult<>(true, 0, "操作成功", null);
    }

    public static <T> ApiResult<T> fail(Integer code, String message) {
        return new ApiResult<>(false, code, message, null);
    }
}
```

如果希望 Controller 显式返回统一结构，可以这样写：

文件位置：`src/main/java/io/github/atengk/jackson/controller/UserApiResultController.java`

```java
package io.github.atengk.jackson.controller;

import cn.hutool.core.collection.CollUtil;
import io.github.atengk.jackson.common.ApiResult;
import io.github.atengk.jackson.vo.UserItemVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户统一响应接口。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@RestController
public class UserApiResultController {

    @GetMapping("/api/result/users")
    public ApiResult<List<UserItemVO>> listUsers() {
        List<UserItemVO> users = CollUtil.newArrayList(
                new UserItemVO(10001L, "ateng"),
                new UserItemVO(10002L, "jackson")
        );

        log.info("返回统一响应用户列表，用户数量：{}", users.size());
        return ApiResult.success(users);
    }
}
```

响应示例：

```json
{
  "success": true,
  "code": 0,
  "message": "操作成功",
  "data": [
    {
      "user_id": 10001,
      "username": "ateng"
    },
    {
      "user_id": 10002,
      "username": "jackson"
    }
  ]
}
```

如果希望业务 Controller 仍然返回 DTO/VO，由框架层统一包装，可以增加 `ResponseBodyAdvice`。

文件位置：`src/main/java/io/github/atengk/jackson/advice/GlobalResponseAdvice.java`

```java
package io.github.atengk.jackson.advice;

import io.github.atengk.jackson.common.ApiResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * 全局响应包装处理器。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@RestControllerAdvice(basePackages = "io.github.atengk")
@RequiredArgsConstructor
public class GlobalResponseAdvice implements ResponseBodyAdvice<Object> {

    private final JsonMapper jsonMapper;

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body,
                                  MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request,
                                  ServerHttpResponse response) {
        if (body == null) {
            return ApiResult.success();
        }

        if (body instanceof ApiResult<?> || body instanceof ProblemDetail) {
            return body;
        }

        if (body instanceof String) {
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            try {
                return jsonMapper.writeValueAsString(ApiResult.success(body));
            } catch (JacksonException ex) {
                log.error("字符串响应包装失败", ex);
                throw new IllegalStateException("字符串响应包装失败", ex);
            }
        }

        return ApiResult.success(body);
    }
}
```

增加该处理器后，普通 Controller 返回 `UserItemVO`、`List<UserItemVO>`、`Map` 等对象时，最终会统一包装为：

```json
{
  "success": true,
  "code": 0,
  "message": "操作成功",
  "data": {
    "user_id": 10001,
    "username": "ateng"
  }
}
```

统一响应结构需要注意三个边界：

| 边界                       | 处理建议                                                     |
| -------------------------- | ------------------------------------------------------------ |
| 已经是 `ApiResult`         | 不重复包装                                                   |
| `ProblemDetail` / 异常响应 | 交给异常处理章节统一处理                                     |
| `String` 响应              | 需要手动转 JSON 字符串，否则可能被 `StringHttpMessageConverter` 当作纯文本处理 |
| 文件下载 / 流式响应        | 不建议进入统一包装逻辑                                       |
| 第三方回调接口             | 如果对方要求固定 JSON 契约，应排除自动包装                   |

对于中大型项目，推荐 Controller 显式返回 `ApiResult<T>`，而不是强依赖 `ResponseBodyAdvice` 自动包装。显式返回虽然代码略多，但接口契约更清晰，Swagger/OpenAPI 文档也更容易准确生成。



## 异常与兼容性处理

异常与兼容性处理用于统一 JSON 请求解析失败、字段类型不匹配、Jackson 2 迁移问题和第三方依赖不兼容问题。Spring MVC 在读取请求体失败时会抛出 `HttpMessageNotReadableException`，该异常由 `HttpMessageConverter` 读取请求体失败触发，因此 JSON 解析错误、类型转换错误、结构不匹配通常都可以从这个入口统一处理。([Home](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/http/converter/HttpMessageNotReadableException.html?utm_source=chatgpt.com))

### JSON 解析异常处理

JSON 解析异常通常发生在请求体不是合法 JSON、JSON 字符串没有闭合、对象和数组结构错误、字段值格式无法解析等场景。Spring MVC 会先通过 `JacksonJsonHttpMessageConverter` 读取请求体，再由 Jackson 3 的 `JsonMapper` 执行反序列化；该转换器默认支持 `application/json` 和 `application/*+json`。([Home](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/http/converter/json/JacksonJsonHttpMessageConverter.html?utm_source=chatgpt.com))

常见错误请求如下：

```json
{
  "username": "ateng",
  "age": 18
```

该请求缺少右花括号，属于 JSON 语法错误，应返回明确的 400 响应，而不是把 Jackson 原始异常堆栈暴露给前端。

下面给出全局异常处理器。该处理器沿用前文的 `ApiResult<T>` 统一响应结构，并针对 Jackson 3 的 `JacksonException`、`InvalidFormatException`、`MismatchedInputException` 做基础分类。

文件位置：`src/main/java/io/github/atengk/jackson/advice/GlobalJsonExceptionHandler.java`

```java
package io.github.atengk.jackson.advice;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.jackson.common.ApiResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.exc.InvalidFormatException;
import tools.jackson.databind.exc.MismatchedInputException;

/**
 * 全局 JSON 异常处理器。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@RestControllerAdvice(basePackages = "io.github.atengk")
public class GlobalJsonExceptionHandler {

    /**
     * 处理请求体不可读异常。
     *
     * @param exception 请求体不可读异常
     * @return 统一错误响应
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResult<Void>> handleHttpMessageNotReadable(HttpMessageNotReadableException exception) {
        Throwable cause = exception.getMostSpecificCause();
        String message = resolveJsonErrorMessage(cause);

        log.info("JSON 请求体解析失败，原因：{}", message);
        return ResponseEntity.badRequest().body(ApiResult.fail(40001, message));
    }

    /**
     * 解析 JSON 异常提示信息。
     *
     * @param cause 根异常
     * @return 前端可读的错误提示
     */
    private String resolveJsonErrorMessage(Throwable cause) {
        if (cause instanceof InvalidFormatException invalidFormatException) {
            String path = StrUtil.blankToDefault(invalidFormatException.getPathReference(), "-");
            String targetType = invalidFormatException.getTargetType() == null
                    ? "-"
                    : invalidFormatException.getTargetType().getSimpleName();

            return StrUtil.format(
                    "字段类型不匹配，路径：{}，输入值：{}，目标类型：{}",
                    path,
                    invalidFormatException.getValue(),
                    targetType
            );
        }

        if (cause instanceof MismatchedInputException mismatchedInputException) {
            String path = StrUtil.blankToDefault(mismatchedInputException.getPathReference(), "-");
            String targetType = mismatchedInputException.getTargetType() == null
                    ? "-"
                    : mismatchedInputException.getTargetType().getSimpleName();

            return StrUtil.format("JSON 结构不匹配，路径：{}，目标类型：{}", path, targetType);
        }

        if (cause instanceof JacksonException jacksonException) {
            return StrUtil.format("JSON 格式不正确：{}", jacksonException.getOriginalMessage());
        }

        return "请求体 JSON 格式不正确";
    }
}
```

`InvalidFormatException` 和 `MismatchedInputException` 属于 Jackson Databind 的输入不匹配类异常，Jackson 3 中这些异常位于 `tools.jackson.databind.exc` 包下；其中 `InvalidFormatException` 适合处理字段值格式错误，`MismatchedInputException` 适合处理 JSON 结构与目标 Java 类型不匹配的问题。([Javadoc](https://javadoc.io/static/tools.jackson.core/jackson-databind/3.0.0/tools.jackson.databind/tools/jackson/databind/exc/package-summary.html?utm_source=chatgpt.com))

验证请求：

```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"username":"ateng","age":18'
```

响应示例：

```json
{
  "success": false,
  "code": 40001,
  "message": "JSON 格式不正确：Unexpected end-of-input...",
  "data": null
}
```

实际生产环境中，`message` 不建议直接返回完整底层异常文本。可以把底层异常写入日志，对前端返回更稳定的业务提示，例如“请求体 JSON 格式不正确”。

### 字段类型不匹配处理

字段类型不匹配通常发生在前端传入的 JSON 值与 DTO 字段类型不一致时，例如 `Integer age` 收到 `"abc"`，`LocalDateTime createdAt` 收到非法日期字符串，枚举 code 不存在，数组字段收到对象等。

示例 DTO 如下。

文件位置：`src/main/java/io/github/atengk/jackson/dto/UserProfileRequest.java`

```java
package io.github.atengk.jackson.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户资料请求。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
public class UserProfileRequest {

    /**
     * 用户名。
     */
    private String username;

    /**
     * 年龄。
     */
    private Integer age;

    /**
     * 注册时间。
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime registeredAt;
}
```

文件位置：`src/main/java/io/github/atengk/jackson/controller/UserProfileController.java`

```java
package io.github.atengk.jackson.controller;

import io.github.atengk.jackson.dto.UserProfileRequest;
import io.github.atengk.jackson.vo.UserCreateVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * 用户资料接口。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@RestController
public class UserProfileController {

    /**
     * 保存用户资料。
     *
     * @param request 用户资料请求
     * @return 用户资料响应
     */
    @PostMapping("/api/users/profile")
    public UserCreateVO saveProfile(@RequestBody UserProfileRequest request) {
        log.info("保存用户资料，用户名：{}，年龄：{}", request.getUsername(), request.getAge());

        return UserCreateVO.builder()
                .userId(10001L)
                .username(request.getUsername())
                .createdAt(LocalDateTime.now())
                .build();
    }
}
```

错误请求示例：

```bash
curl -X POST http://localhost:8080/api/users/profile \
  -H "Content-Type: application/json" \
  -d '{"username":"ateng","age":"abc","registered_at":"2026-05-09 10:30:15"}'
```

响应示例：

```json
{
  "success": false,
  "code": 40001,
  "message": "字段类型不匹配，路径：io.github.atengk.jackson.dto.UserProfileRequest[\"age\"]，输入值：abc，目标类型：Integer",
  "data": null
}
```

字段类型不匹配处理建议如下：

| 场景               | 建议                                                         |
| ------------------ | ------------------------------------------------------------ |
| 数值字段传入字符串 | 对内部接口保持严格，直接返回 400；对外部兼容接口可单独定义兼容 DTO |
| 日期格式错误       | 用 `@JsonFormat` 或统一日期反序列化器明确格式                |
| 枚举值不存在       | 使用 `@JsonCreator` 或自定义反序列化器返回明确错误           |
| 前端多传字段       | 通过 `fail-on-unknown-properties: false` 或 `@JsonIgnoreProperties(ignoreUnknown = true)` 兼容 |
| 关键字段为 null    | 用 Bean Validation 处理，例如 `@NotNull`、`@NotBlank`        |

类型不匹配不建议在 Service 层兜底处理。JSON 入参无法正确转换为 DTO 时，业务方法不应该继续执行，应在 Web 层直接失败。

### Jackson 2 迁移兼容问题

Jackson 2 到 Jackson 3 不是简单升级版本号，而是包名、核心类型、异常类型、构建方式和部分默认行为都发生了变化。Jackson 3 的官方迁移说明明确指出，Jackson 3 的 Maven groupId 和 Java 包名迁移到 `tools.jackson`，但 `jackson-annotations` 仍然保留 `com.fasterxml.jackson.annotation`；同时 `@JsonSerialize`、`@JsonDeserialize` 这类 Databind 注解迁移到 `tools.jackson.databind.annotation`。([GitHub](https://github.com/FasterXML/jackson/blob/main/jackson3/MIGRATING_TO_JACKSON_3.md?utm_source=chatgpt.com))

常见替换关系如下：

| Jackson 2                                               | Jackson 3                                       | 说明                           |
| ------------------------------------------------------- | ----------------------------------------------- | ------------------------------ |
| `com.fasterxml.jackson.databind.ObjectMapper`           | `tools.jackson.databind.json.JsonMapper`        | Spring Boot 4 JSON 默认 Mapper |
| `com.fasterxml.jackson.databind.JsonSerializer`         | `tools.jackson.databind.ValueSerializer`        | 自定义值序列化器               |
| `com.fasterxml.jackson.databind.JsonDeserializer`       | `tools.jackson.databind.ValueDeserializer`      | 自定义值反序列化器             |
| `com.fasterxml.jackson.databind.SerializerProvider`     | `tools.jackson.databind.SerializationContext`   | 序列化上下文                   |
| `com.fasterxml.jackson.databind.DeserializationContext` | `tools.jackson.databind.DeserializationContext` | 包名迁移                       |
| `com.fasterxml.jackson.core.JsonProcessingException`    | `tools.jackson.core.JacksonException`           | Jackson 3 基础异常             |
| `com.fasterxml.jackson.databind.JsonMappingException`   | `tools.jackson.databind.DatabindException`      | Jackson 3 数据绑定异常         |
| `@JsonComponent`                                        | `@JacksonComponent`                             | Spring Boot 4 推荐组件注解     |
| `Jackson2ObjectMapperBuilderCustomizer`                 | `JsonMapperBuilderCustomizer`                   | Spring Boot 4 推荐构建器定制器 |

Spring Boot 4 的 `JsonMapperBuilderCustomizer` 是用于定制 `JsonMapper.Builder` 的函数式接口，适合替代 Spring Boot 3 常见的 `Jackson2ObjectMapperBuilderCustomizer`。([Home](https://docs.spring.io/spring-boot/api/java/org/springframework/boot/jackson/autoconfigure/JsonMapperBuilderCustomizer.html?utm_source=chatgpt.com))

迁移前旧代码示例：

```java
// Jackson 2 旧写法，不建议在 Spring Boot 4 新项目中继续使用
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

ObjectMapper objectMapper = new ObjectMapper();
objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
```

迁移后推荐写法：

文件位置：`src/main/java/io/github/atengk/jackson/config/JacksonMigrationConfig.java`

```java
package io.github.atengk.jackson.config;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.SerializationFeature;

/**
 * Jackson 迁移配置。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Configuration(proxyBeanMethods = false)
public class JacksonMigrationConfig {

    /**
     * 使用 Jackson 3 Builder 方式定制 JsonMapper。
     *
     * @return JsonMapper 构建器定制器
     */
    @Bean
    public JsonMapperBuilderCustomizer migrationJsonMapperCustomizer() {
        return builder -> builder
                // 日期时间不输出为时间戳
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
}
```

迁移时建议按以下顺序处理：

1. 先升级到 Spring Boot 4，并确认 Java 版本、构建插件和测试依赖满足要求。
2. 全局搜索 `com.fasterxml.jackson.databind`、`com.fasterxml.jackson.core`、`JsonSerializer`、`JsonDeserializer`。
3. 将核心 Databind 类型迁移到 `tools.jackson.*`。
4. 保留常用注解包 `com.fasterxml.jackson.annotation.*`，不要机械替换所有注解。
5. 替换 `@JsonComponent` 为 `@JacksonComponent`。
6. 替换 `Jackson2ObjectMapperBuilderCustomizer` 为 `JsonMapperBuilderCustomizer`。
7. 用快照测试验证迁移前后 JSON 契约是否发生变化。
8. 对三方依赖进行依赖树排查，避免同一链路混用 Jackson 2 和 Jackson 3。

可以用以下命令检查项目是否仍存在 Jackson 2 依赖：

```bash
mvn dependency:tree | grep -E "com.fasterxml.jackson|tools.jackson"
```

命令说明：`mvn dependency:tree` 输出完整 Maven 依赖树，`grep -E` 同时匹配 Jackson 2 的 `com.fasterxml.jackson` 和 Jackson 3 的 `tools.jackson`。如果输出中仍存在 `com.fasterxml.jackson.core:jackson-databind`，需要确认是否来自第三方依赖传递引入。

### 第三方依赖适配问题

第三方依赖适配问题通常出现在 SDK、Starter、日志组件、消息组件、OpenAPI 工具、搜索客户端、对象存储客户端等库仍然强依赖 Jackson 2 的场景。Jackson 3 迁移涉及包名和核心类变化，因此第三方库如果在公开 API 中暴露 `ObjectMapper`、`JsonSerializer`、`JsonDeserializer` 等 Jackson 2 类型，不能直接当作 Jackson 3 类型使用。([GitHub](https://github.com/FasterXML/jackson/blob/main/jackson3/MIGRATING_TO_JACKSON_3.md?utm_source=chatgpt.com))

排查依赖树：

```bash
mvn dependency:tree -Dincludes=com.fasterxml.jackson
mvn dependency:tree -Dincludes=tools.jackson
```

命令说明：第一条命令排查 Jackson 2 依赖来源，第二条命令排查 Jackson 3 依赖来源。迁移期可以同时看到两套依赖，但不要让两套 Mapper 混在同一个 HTTP 消息转换链路、统一响应序列化链路或公共 JSON 工具类中。

适配策略如下：

| 问题                                        | 处理方式                                                 |
| ------------------------------------------- | -------------------------------------------------------- |
| 第三方库已有 Spring Boot 4 版本             | 优先升级第三方库                                         |
| 第三方库只在内部使用 Jackson 2              | 可以暂时保留，但不要把 Jackson 2 类型暴露到业务代码      |
| 第三方库 API 暴露 `ObjectMapper`            | 用适配层隔离，不要在业务代码直接依赖该 API               |
| 第三方 Starter 自动注册 Jackson 2 Converter | 排除自动配置或降低优先级                                 |
| OpenAPI / Swagger 插件不兼容                | 升级到声明支持 Spring Boot 4 / Spring Framework 7 的版本 |
| 公共工具包写死 Jackson 2                    | 拆分 Jackson 2 与 Jackson 3 分支，或重构为接口抽象       |

可以用适配层隔离第三方 Jackson 2 SDK，避免污染主项目 JSON 契约。

文件位置：`src/main/java/io/github/atengk/jackson/adapter/ThirdPartyJsonAdapter.java`

```java
package io.github.atengk.jackson.adapter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * 第三方 JSON 适配器。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Component
public class ThirdPartyJsonAdapter {

    private final JsonMapper jsonMapper;

    public ThirdPartyJsonAdapter(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    /**
     * 将第三方返回的 JSON 字符串转换为业务对象。
     *
     * @param json  第三方 JSON 字符串
     * @param clazz 目标类型
     * @param <T>   目标类型
     * @return 业务对象
     */
    public <T> T readThirdPartyJson(String json, Class<T> clazz) {
        try {
            T result = jsonMapper.readValue(json, clazz);
            log.info("第三方 JSON 转换成功，目标类型：{}", clazz.getSimpleName());
            return result;
        } catch (JacksonException ex) {
            log.error("第三方 JSON 转换失败，目标类型：{}", clazz.getSimpleName(), ex);
            throw new IllegalArgumentException("第三方 JSON 数据格式不正确", ex);
        }
    }
}
```

该适配器只接收字符串和业务类型，不把第三方 SDK 的 Jackson 2 类型扩散到 Controller、Service、DTO 和测试代码中。迁移期可以允许某个三方 SDK 内部继续使用 Jackson 2，但项目对外 JSON 处理链路应统一使用 Jackson 3。

## 测试与验证

测试与验证用于保证 JSON 契约稳定，避免迁移到 Spring Boot 4 + Jackson 3 后出现字段名变化、日期格式变化、空值输出变化、枚举格式变化和 Long 精度变化。Spring Boot 测试模块提供 JSON 测试、MVC 切片测试、JSONAssert、JsonPath、AssertJ、JUnit Jupiter 等常用测试能力。([Home](https://docs.spring.io/spring-boot/reference/testing/index.html?utm_source=chatgpt.com))

Spring Boot 4 中，Spring MVC 测试支持位于 `spring-boot-starter-webmvc-test` 相关模块，`@WebMvcTest` 的包名为 `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest`。([Home](https://docs.spring.io/spring-boot/api/java/org/springframework/boot/webmvc/test/autoconfigure/WebMvcTest.html?utm_source=chatgpt.com))

测试依赖建议如下。

文件位置：`pom.xml`

```xml
<dependencies>
    <!-- Spring Boot 通用测试依赖：JUnit Jupiter、AssertJ、Mockito、JSONassert、JsonPath 等 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>

    <!-- Spring MVC 测试依赖：@WebMvcTest、MockMvc、MVC 测试自动配置 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-webmvc-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

### 单元测试 ObjectMapper

在 Spring Boot 4 中，JSON 单元测试推荐使用 `@JsonTest` 和 `JacksonTester`。`@JsonTest` 只启用 JSON 测试相关自动配置，默认会初始化 `JacksonTester`、`JsonbTester`、`GsonTester` 等测试辅助对象；在 Jackson 3 场景下，`JacksonTester` 是推荐入口。([Home](https://docs.spring.io/spring-boot/docs/current/api/org/springframework/boot/test/autoconfigure/json/JsonTest.html?utm_source=chatgpt.com))

下面测试 `ProductVO` 的序列化结果，验证字段名、日期格式和忽略字段是否符合预期。

文件位置：`src/test/java/io/github/atengk/jackson/ProductVOJsonTest.java`

```java
package io.github.atengk.jackson;

import io.github.atengk.jackson.vo.ProductVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 商品 VO JSON 单元测试。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@JsonTest(properties = {
        "spring.jackson.property-naming-strategy=SNAKE_CASE",
        "spring.jackson.default-property-inclusion=non_null"
})
class ProductVOJsonTest {

    @Autowired
    private JacksonTester<ProductVO> json;

    /**
     * 验证商品响应对象序列化结果。
     *
     * @throws Exception JSON 写入异常
     */
    @Test
    void shouldSerializeProductVO() throws Exception {
        ProductVO product = ProductVO.builder()
                .id(10001L)
                .name("Spring Boot 4 Jackson 3 开发文档")
                .price(new BigDecimal("99.90"))
                .createdAt(LocalDateTime.of(2026, 5, 9, 10, 30, 15))
                .internalRemark("内部备注不允许输出")
                .build();

        JsonContent<ProductVO> content = json.write(product);

        assertThat(content).isStrictlyEqualToJson("""
                {
                  "product_id": 10001,
                  "name": "Spring Boot 4 Jackson 3 开发文档",
                  "price": 99.90,
                  "created_at": "2026-05-09 10:30:15"
                }
                """);
    }
}
```

该测试适合验证单个 DTO/VO 的 JSON 契约。如果测试失败，通常说明字段命名策略、日期格式、空值策略或注解配置发生了变化。

### WebMvcTest JSON 验证

`@WebMvcTest` 用于专注测试 Spring MVC 层。它只启用 MVC 测试相关自动配置，并限制组件扫描范围；默认会自动配置 `MockMvc`，通常配合 `@Import` 或 Mockito 注入 Controller 所需依赖。([Home](https://docs.spring.io/spring-boot/api/java/org/springframework/boot/webmvc/test/autoconfigure/WebMvcTest.html?utm_source=chatgpt.com))

下面测试前文的 `UserCreateController`，同时验证成功请求和字段类型不匹配请求。

文件位置：`src/test/java/io/github/atengk/jackson/UserCreateControllerTest.java`

```java
package io.github.atengk.jackson;

import io.github.atengk.jackson.advice.GlobalJsonExceptionHandler;
import io.github.atengk.jackson.controller.UserCreateController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 用户创建接口 MVC 测试。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@WebMvcTest(
        controllers = UserCreateController.class,
        properties = {
                "spring.jackson.property-naming-strategy=SNAKE_CASE",
                "spring.jackson.default-property-inclusion=non_null"
        }
)
@Import(GlobalJsonExceptionHandler.class)
class UserCreateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * 验证合法 JSON 请求可以成功创建用户。
     *
     * @throws Exception MVC 调用异常
     */
    @Test
    void shouldCreateUserWhenRequestBodyValid() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "ateng",
                                  "age": 18
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user_id").exists())
                .andExpect(jsonPath("$.username").value("ateng"))
                .andExpect(jsonPath("$.created_at").exists());
    }

    /**
     * 验证字段类型不匹配时返回 400。
     *
     * @throws Exception MVC 调用异常
     */
    @Test
    void shouldReturnBadRequestWhenFieldTypeMismatch() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "ateng",
                                  "age": "abc"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(40001));
    }
}
```

如果项目引入了 Spring Security，`@WebMvcTest` 可能受到安全过滤器影响。需要保留安全测试时，应配置测试用户；只验证 JSON 序列化行为时，可以额外使用 `@AutoConfigureMockMvc(addFilters = false)`，该注解在 Spring Boot 4 中位于 `org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc`。([Home](https://docs.spring.io/spring-boot/api/java/org/springframework/boot/webmvc/test/autoconfigure/AutoConfigureMockMvc.html?utm_source=chatgpt.com))

### 接口序列化快照测试

接口序列化快照测试用于锁定 JSON 输出契约。迁移 Jackson、调整字段命名策略、修改 DTO 注解或升级 Spring Boot 后，只要 JSON 结构发生变化，测试就会失败，从而提醒开发者确认是否为预期变更。

快照文件建议放在 `src/test/resources/json` 目录下。

文件位置：`src/test/resources/json/user-list-snapshot.json`

```json
[
  {
    "user_id": 10001,
    "username": "ateng"
  },
  {
    "user_id": 10002,
    "username": "jackson"
  }
]
```

文件位置：`src/test/java/io/github/atengk/jackson/UserListSnapshotTest.java`

```java
package io.github.atengk.jackson;

import cn.hutool.core.collection.CollUtil;
import io.github.atengk.jackson.vo.UserItemVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.core.io.ClassPathResource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 用户列表 JSON 快照测试。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@JsonTest(properties = {
        "spring.jackson.property-naming-strategy=SNAKE_CASE",
        "spring.jackson.default-property-inclusion=non_null"
})
class UserListSnapshotTest {

    @Autowired
    private JacksonTester<List<UserItemVO>> json;

    /**
     * 验证用户列表 JSON 输出与快照一致。
     *
     * @throws Exception JSON 写入异常
     */
    @Test
    void shouldMatchUserListSnapshot() throws Exception {
        List<UserItemVO> users = CollUtil.newArrayList(
                new UserItemVO(10001L, "ateng"),
                new UserItemVO(10002L, "jackson")
        );

        assertThat(json.write(users))
                .isStrictlyEqualToJson(new ClassPathResource("json/user-list-snapshot.json"));
    }
}
```

快照测试建议使用严格比较。严格比较可以发现字段名变化、字段缺失、字段新增、数组顺序变化等问题；如果接口允许字段顺序变化但不允许字段内容变化，可以根据项目规范改为宽松比较。

### 迁移前后 JSON 差异对比

迁移前后 JSON 差异对比用于确认 Spring Boot 3 + Jackson 2 到 Spring Boot 4 + Jackson 3 后，外部接口契约是否保持稳定。Jackson 3 迁移涉及包名、异常类型、Mapper 构建方式、部分默认配置变化和 Java 8 模块内置等内容，因此迁移验证不能只依赖编译通过，还需要对核心接口做 JSON 契约对比。([GitHub](https://github.com/FasterXML/jackson/blob/main/jackson3/MIGRATING_TO_JACKSON_3.md?utm_source=chatgpt.com))

建议迁移前先在旧分支导出核心接口响应快照，例如：

```text
src/test/resources/migration/jackson2/user-detail.json
src/test/resources/migration/jackson2/order-detail.json
src/test/resources/migration/jackson2/page-user.json
```

示例旧快照：

文件位置：`src/test/resources/migration/jackson2/user-detail.json`

```json
{
  "user_id": 10001,
  "username": "ateng"
}
```

迁移后使用 Jackson 3 重新生成当前 JSON，并与旧快照进行严格对比。

文件位置：`src/test/java/io/github/atengk/jackson/JacksonMigrationDiffTest.java`

```java
package io.github.atengk.jackson;

import cn.hutool.core.io.resource.ResourceUtil;
import io.github.atengk.jackson.vo.UserItemVO;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import tools.jackson.databind.json.JsonMapper;

/**
 * Jackson 迁移前后 JSON 差异对比测试。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@SpringBootTest(properties = {
        "spring.jackson.property-naming-strategy=SNAKE_CASE",
        "spring.jackson.default-property-inclusion=non_null"
})
class JacksonMigrationDiffTest {

    @Autowired
    private JsonMapper jsonMapper;

    /**
     * 验证用户详情 JSON 契约迁移前后一致。
     *
     * @throws Exception JSON 序列化或断言异常
     */
    @Test
    void shouldKeepUserDetailJsonCompatibleAfterMigration() throws Exception {
        String jackson2Snapshot = ResourceUtil.readUtf8Str("migration/jackson2/user-detail.json");

        UserItemVO currentUser = new UserItemVO(10001L, "ateng");
        String jackson3Json = jsonMapper.writeValueAsString(currentUser);

        JSONAssert.assertEquals(jackson2Snapshot, jackson3Json, JSONCompareMode.STRICT);
    }
}
```

如果测试失败，应先判断差异是否为预期变更：

| 差异类型                         | 处理建议                                    |
| -------------------------------- | ------------------------------------------- |
| 字段名从 `userId` 变成 `user_id` | 检查命名策略是否一致                        |
| 日期从时间戳变成字符串           | 检查 `WRITE_DATES_AS_TIMESTAMPS` 和日期格式 |
| `null` 字段消失                  | 检查 `default-property-inclusion`           |
| 枚举从 `"ENABLE"` 变成 `1`       | 检查 `@JsonValue` 或枚举序列化策略          |
| Long 被转成字符串                | 检查 Long 精度处理策略                      |
| 字段顺序变化                     | 判断接口是否依赖字段顺序，通常不应依赖      |

迁移测试建议覆盖核心外部接口、前端强依赖接口、第三方回调接口、分页接口、枚举接口、日期时间接口和大 Long 字段接口。对于内部接口，可以采用宽松比较；对于对外开放接口，建议使用严格快照测试并纳入 CI。



## 实战案例

实战案例用于把前面分散的配置、注解、自定义序列化器和 REST 集成方式组合起来，解决真实项目中最常见的 JSON 契约问题。Spring Boot 4 中建议优先围绕 `JsonMapper`、`JsonMapperBuilderCustomizer`、`JacksonModule` 和 `JacksonJsonHttpMessageConverter` 组织 JSON 能力；其中 `JacksonModule` Bean 会被自动注册到自动配置的 `JsonMapper.Builder`，适合做应用级扩展。([Home](https://docs.spring.io/spring-boot/how-to/spring-mvc.html?utm_source=chatgpt.com))

### 日期时间格式统一处理

日期时间格式统一处理用于保证接口中 `LocalDateTime`、`LocalDate`、`LocalTime` 等类型输出格式一致。简单项目可以使用 `@JsonFormat` 标注字段；中大型项目更推荐通过统一 Jackson Module 或全局配置集中处理，避免不同 DTO 中格式不一致。

推荐统一格式如下：

| Java 类型       | JSON 格式             |
| --------------- | --------------------- |
| `LocalDateTime` | `yyyy-MM-dd HH:mm:ss` |
| `LocalDate`     | `yyyy-MM-dd`          |
| `LocalTime`     | `HH:mm:ss`            |

下面示例通过 `JacksonModule` 统一注册 `LocalDateTime` 的序列化器和反序列化器。

文件结构如下：

```text
src/main/java/io/github/atengk/jackson/serializer/LocalDateTimeFormatSerializer.java
src/main/java/io/github/atengk/jackson/deserializer/LocalDateTimeFormatDeserializer.java
src/main/java/io/github/atengk/jackson/config/JacksonDateTimeConfig.java
src/main/java/io/github/atengk/jackson/dto/EventCreateRequest.java
src/main/java/io/github/atengk/jackson/vo/EventVO.java
src/main/java/io/github/atengk/jackson/controller/EventController.java
```

该序列化器负责把 `LocalDateTime` 统一输出为 `yyyy-MM-dd HH:mm:ss`。

文件位置：`src/main/java/io/github/atengk/jackson/serializer/LocalDateTimeFormatSerializer.java`

```java
package io.github.atengk.jackson.serializer;

import cn.hutool.core.date.DatePattern;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

import java.time.LocalDateTime;

/**
 * LocalDateTime 日期时间序列化器。
 *
 * @author Ateng
 * @since 2026-05-09
 */
public class LocalDateTimeFormatSerializer extends ValueSerializer<LocalDateTime> {

    /**
     * 序列化 LocalDateTime。
     *
     * @param value   日期时间
     * @param gen     JSON 生成器
     * @param context 序列化上下文
     * @throws JacksonException JSON 写入异常
     */
    @Override
    public void serialize(LocalDateTime value, JsonGenerator gen, SerializationContext context) throws JacksonException {
        if (value == null) {
            gen.writeNull();
            return;
        }
        gen.writeString(DatePattern.NORM_DATETIME_FORMATTER.format(value));
    }
}
```

该反序列化器负责把 `yyyy-MM-dd HH:mm:ss` 字符串统一解析为 `LocalDateTime`。

文件位置：`src/main/java/io/github/atengk/jackson/deserializer/LocalDateTimeFormatDeserializer.java`

```java
package io.github.atengk.jackson.deserializer;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.util.StrUtil;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

import java.time.LocalDateTime;

/**
 * LocalDateTime 日期时间反序列化器。
 *
 * @author Ateng
 * @since 2026-05-09
 */
public class LocalDateTimeFormatDeserializer extends ValueDeserializer<LocalDateTime> {

    /**
     * 反序列化 LocalDateTime。
     *
     * @param parser  JSON 解析器
     * @param context 反序列化上下文
     * @return 日期时间
     * @throws JacksonException JSON 读取异常
     */
    @Override
    public LocalDateTime deserialize(JsonParser parser, DeserializationContext context) throws JacksonException {
        String text = StrUtil.trim(parser.getText());
        if (StrUtil.isBlank(text)) {
            return null;
        }
        return LocalDateTime.parse(text, DatePattern.NORM_DATETIME_FORMATTER);
    }
}
```

该配置类通过 `JacksonModule` 注册日期时间处理器。Spring Boot 4 会自动把 `JacksonModule` Bean 应用到自动配置的 `JsonMapper.Builder`。([Home](https://docs.spring.io/spring-boot/how-to/spring-mvc.html?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/jackson/config/JacksonDateTimeConfig.java`

```java
package io.github.atengk.jackson.config;

import io.github.atengk.jackson.deserializer.LocalDateTimeFormatDeserializer;
import io.github.atengk.jackson.serializer.LocalDateTimeFormatSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.module.SimpleModule;

import java.time.LocalDateTime;

/**
 * Jackson 日期时间统一配置。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Configuration(proxyBeanMethods = false)
public class JacksonDateTimeConfig {

    /**
     * 注册日期时间 Jackson 模块。
     *
     * @return Jackson 模块
     */
    @Bean
    public JacksonModule dateTimeJacksonModule() {
        SimpleModule module = new SimpleModule("date-time-jackson-module");
        module.addSerializer(LocalDateTime.class, new LocalDateTimeFormatSerializer());
        module.addDeserializer(LocalDateTime.class, new LocalDateTimeFormatDeserializer());
        return module;
    }
}
```

下面定义请求和响应对象，用于验证日期时间入参和出参格式。

文件位置：`src/main/java/io/github/atengk/jackson/dto/EventCreateRequest.java`

```java
package io.github.atengk.jackson.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 事件创建请求。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
public class EventCreateRequest {

    /**
     * 事件名称。
     */
    private String eventName;

    /**
     * 开始时间，格式：yyyy-MM-dd HH:mm:ss。
     */
    private LocalDateTime startTime;
}
```

文件位置：`src/main/java/io/github/atengk/jackson/vo/EventVO.java`

```java
package io.github.atengk.jackson.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 事件响应对象。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
@Builder
public class EventVO {

    /**
     * 事件编号。
     */
    private Long eventId;

    /**
     * 事件名称。
     */
    private String eventName;

    /**
     * 开始时间。
     */
    private LocalDateTime startTime;
}
```

该接口用于验证统一日期时间序列化和反序列化是否生效。

文件位置：`src/main/java/io/github/atengk/jackson/controller/EventController.java`

```java
package io.github.atengk.jackson.controller;

import cn.hutool.core.util.IdUtil;
import io.github.atengk.jackson.dto.EventCreateRequest;
import io.github.atengk.jackson.vo.EventVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 事件接口。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@RestController
public class EventController {

    /**
     * 创建事件。
     *
     * @param request 事件创建请求
     * @return 事件响应
     */
    @PostMapping("/api/events")
    public EventVO createEvent(@RequestBody EventCreateRequest request) {
        Long eventId = IdUtil.getSnowflakeNextId();

        log.info("创建事件成功，事件编号：{}，事件名称：{}", eventId, request.getEventName());
        return EventVO.builder()
                .eventId(eventId)
                .eventName(request.getEventName())
                .startTime(request.getStartTime())
                .build();
    }
}
```

验证请求：

```bash
curl -X POST http://localhost:8080/api/events \
  -H "Content-Type: application/json" \
  -d '{"event_name":"Jackson 3 分享会","start_time":"2026-05-09 10:30:15"}'
```

响应示例：

```json
{
  "event_id": 887654321012345678,
  "event_name": "Jackson 3 分享会",
  "start_time": "2026-05-09 10:30:15"
}
```

### 枚举值序列化处理

枚举值序列化处理用于保证枚举在 JSON 中输出稳定的业务值，而不是直接输出 Java 枚举常量名。对外接口推荐输出业务 `code`，入参也接收业务 `code`，这样 Java 枚举常量重命名时不会破坏接口契约。

下面示例使用 `@JsonValue` 控制枚举输出，使用 `@JsonCreator` 支持按 code 反序列化。Jackson 通用注解仍来自 `com.fasterxml.jackson.annotation`，这类注解在 Jackson 3 迁移中仍保留原包名。([Home](https://docs.spring.io/spring-boot/how-to/spring-mvc.html?utm_source=chatgpt.com))

该枚举用于统一用户状态的入参和出参。

文件位置：`src/main/java/io/github/atengk/jackson/enums/AccountStatus.java`

```java
package io.github.atengk.jackson.enums;

import cn.hutool.core.util.ObjUtil;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 账户状态枚举。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Getter
public enum AccountStatus {

    /**
     * 正常。
     */
    ENABLE(1, "正常"),

    /**
     * 禁用。
     */
    DISABLE(0, "禁用"),

    /**
     * 锁定。
     */
    LOCKED(2, "锁定");

    /**
     * 状态编码。
     */
    @JsonValue
    private final Integer code;

    /**
     * 状态名称。
     */
    private final String name;

    AccountStatus(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    /**
     * 根据状态编码获取枚举。
     *
     * @param code 状态编码
     * @return 账户状态
     */
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static AccountStatus of(Integer code) {
        for (AccountStatus status : values()) {
            if (ObjUtil.equal(status.getCode(), code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("不支持的账户状态编码：" + code);
    }
}
```

该请求对象中枚举字段接收 JSON 数值，例如 `"status": 1`。

文件位置：`src/main/java/io/github/atengk/jackson/dto/AccountUpdateRequest.java`

```java
package io.github.atengk.jackson.dto;

import io.github.atengk.jackson.enums.AccountStatus;
import lombok.Data;

/**
 * 账户更新请求。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
public class AccountUpdateRequest {

    /**
     * 账户编号。
     */
    private Long accountId;

    /**
     * 账户状态。
     */
    private AccountStatus status;
}
```

该响应对象中枚举字段也会输出 JSON 数值，例如 `"status": 1`。

文件位置：`src/main/java/io/github/atengk/jackson/vo/AccountStatusVO.java`

```java
package io.github.atengk.jackson.vo;

import io.github.atengk.jackson.enums.AccountStatus;
import lombok.Builder;
import lombok.Data;

/**
 * 账户状态响应对象。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
@Builder
public class AccountStatusVO {

    /**
     * 账户编号。
     */
    private Long accountId;

    /**
     * 账户状态。
     */
    private AccountStatus status;

    /**
     * 状态名称。
     */
    private String statusName;
}
```

该接口用于验证枚举 code 的反序列化和序列化。

文件位置：`src/main/java/io/github/atengk/jackson/controller/AccountStatusController.java`

```java
package io.github.atengk.jackson.controller;

import io.github.atengk.jackson.dto.AccountUpdateRequest;
import io.github.atengk.jackson.vo.AccountStatusVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 账户状态接口。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@RestController
public class AccountStatusController {

    /**
     * 更新账户状态。
     *
     * @param request 账户更新请求
     * @return 账户状态响应
     */
    @PutMapping("/api/accounts/status")
    public AccountStatusVO updateStatus(@RequestBody AccountUpdateRequest request) {
        log.info("更新账户状态，账户编号：{}，状态：{}", request.getAccountId(), request.getStatus());

        return AccountStatusVO.builder()
                .accountId(request.getAccountId())
                .status(request.getStatus())
                .statusName(request.getStatus().getName())
                .build();
    }
}
```

验证请求：

```bash
curl -X PUT http://localhost:8080/api/accounts/status \
  -H "Content-Type: application/json" \
  -d '{"account_id":10001,"status":1}'
```

响应示例：

```json
{
  "account_id": 10001,
  "status": 1,
  "status_name": "正常"
}
```

这种方式适合枚举数量较少、每个枚举自己维护 code 映射的场景。如果项目中有大量 code 枚举，可以抽象统一的 `CodeEnum` 接口，再使用公共枚举反序列化器集中处理。

### Long 类型精度处理

Long 类型精度处理主要面向前端 JavaScript 场景。Java 的 `Long` 可以表示 64 位整数，但 JavaScript `Number` 安全整数范围有限，过大的 Long 直接以 JSON 数字输出可能在前端丢失精度。实践中常见处理方式是将 ID 类 Long 字段输出为字符串。

推荐策略如下：

| 场景                | 推荐处理                                    |
| ------------------- | ------------------------------------------- |
| ID、雪花 ID、订单号 | 输出字符串                                  |
| 金额                | 不使用 Long 表示金额，优先使用 `BigDecimal` |
| 计数、页码、数量    | 可继续输出数字                              |
| 内部 Java 服务互调  | 可保持 Long 数字，但需确认消费方能力        |

下面示例通过 `JacksonModule` 将包装类型 `Long` 全局序列化为字符串。业务 DTO/VO 中建议使用 `Long` 包装类型，而不是 `long` 基本类型，便于空值处理和统一序列化。

该序列化器负责把 `Long` 输出为字符串。

文件位置：`src/main/java/io/github/atengk/jackson/serializer/LongToStringSerializer.java`

```java
package io.github.atengk.jackson.serializer;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

/**
 * Long 转字符串序列化器。
 *
 * @author Ateng
 * @since 2026-05-09
 */
public class LongToStringSerializer extends ValueSerializer<Long> {

    /**
     * 序列化 Long。
     *
     * @param value   Long 值
     * @param gen     JSON 生成器
     * @param context 序列化上下文
     * @throws JacksonException JSON 写入异常
     */
    @Override
    public void serialize(Long value, JsonGenerator gen, SerializationContext context) throws JacksonException {
        if (value == null) {
            gen.writeNull();
            return;
        }
        gen.writeString(String.valueOf(value));
    }
}
```

该配置类将所有包装类型 `Long` 注册为字符串输出。

文件位置：`src/main/java/io/github/atengk/jackson/config/JacksonLongConfig.java`

```java
package io.github.atengk.jackson.config;

import io.github.atengk.jackson.serializer.LongToStringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.module.SimpleModule;

/**
 * Jackson Long 精度配置。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Configuration(proxyBeanMethods = false)
public class JacksonLongConfig {

    /**
     * 注册 Long 转字符串模块。
     *
     * @return Jackson 模块
     */
    @Bean
    public JacksonModule longToStringJacksonModule() {
        SimpleModule module = new SimpleModule("long-to-string-jackson-module");

        // 将包装类型 Long 输出为字符串，避免前端 JavaScript 精度丢失
        module.addSerializer(Long.class, new LongToStringSerializer());

        return module;
    }
}
```

该响应对象用于验证大 Long 输出。

文件位置：`src/main/java/io/github/atengk/jackson/vo/SnowflakeIdVO.java`

```java
package io.github.atengk.jackson.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 雪花 ID 响应对象。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
@Builder
public class SnowflakeIdVO {

    /**
     * 用户编号。
     */
    private Long userId;

    /**
     * 订单编号。
     */
    private Long orderId;

    /**
     * 普通数量字段，示例中仍使用 Integer。
     */
    private Integer count;
}
```

该接口用于验证 Long 是否输出为字符串。

文件位置：`src/main/java/io/github/atengk/jackson/controller/SnowflakeIdController.java`

```java
package io.github.atengk.jackson.controller;

import cn.hutool.core.util.IdUtil;
import io.github.atengk.jackson.vo.SnowflakeIdVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 雪花 ID 接口。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@RestController
public class SnowflakeIdController {

    /**
     * 获取雪花 ID 示例。
     *
     * @return 雪花 ID 响应
     */
    @GetMapping("/api/ids/snowflake")
    public SnowflakeIdVO getSnowflakeId() {
        SnowflakeIdVO result = SnowflakeIdVO.builder()
                .userId(IdUtil.getSnowflakeNextId())
                .orderId(IdUtil.getSnowflakeNextId())
                .count(2)
                .build();

        log.info("返回雪花 ID 示例，用户编号：{}，订单编号：{}", result.getUserId(), result.getOrderId());
        return result;
    }
}
```

验证接口：

```bash
curl http://localhost:8080/api/ids/snowflake
```

响应示例：

```json
{
  "user_id": "887654321012345678",
  "order_id": "887654321012345679",
  "count": 2
}
```

如果不希望全局所有 `Long` 都转字符串，可以只在 ID 字段上使用 `@JsonSerialize(using = LongToStringSerializer.class)`。字段级方式侵入性更高，但影响范围更可控，适合只有少数字段需要保护精度的项目。

### Page 分页对象处理

Page 分页对象处理用于统一分页接口的 JSON 契约。Spring Data 的 `PageRequest` 是 `Pageable` 的基础实现，页码是从 0 开始的；`PageImpl<T>` 是 `Page<T>` 的基础实现，可通过内容列表、分页参数和总数构造分页结果。([Home](https://docs.spring.io/spring-data/commons/docs/current/api/org/springframework/data/domain/PageRequest.html?utm_source=chatgpt.com))

不建议直接把 `PageImpl<T>` 作为接口响应暴露给前端。原因是 `PageImpl` 的默认 JSON 结构包含较多 Spring Data 内部字段，例如 `pageable`、`sort`、`first`、`last` 等，前端通常只需要 `records`、`pageNum`、`pageSize`、`total`、`pages`。更稳定的方式是转换成自定义分页 VO。

文件结构如下：

```text
src/main/java/io/github/atengk/jackson/common/PageResult.java
src/main/java/io/github/atengk/jackson/dto/PageQuery.java
src/main/java/io/github/atengk/jackson/vo/UserItemVO.java
src/main/java/io/github/atengk/jackson/controller/UserPageController.java
```

该分页响应对象用于对外固定分页 JSON 契约。

文件位置：`src/main/java/io/github/atengk/jackson/common/PageResult.java`

```java
package io.github.atengk.jackson.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 分页响应结构。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {

    /**
     * 当前页码，从 1 开始。
     */
    private Integer pageNum;

    /**
     * 每页数量。
     */
    private Integer pageSize;

    /**
     * 总记录数。
     */
    private Long total;

    /**
     * 总页数。
     */
    private Long pages;

    /**
     * 当前页数据。
     */
    private List<T> records;

    /**
     * 构建分页响应。
     *
     * @param records  当前页数据
     * @param pageNum  当前页码
     * @param pageSize 每页数量
     * @param total    总记录数
     * @param <T>      数据类型
     * @return 分页响应
     */
    public static <T> PageResult<T> of(List<T> records, Integer pageNum, Integer pageSize, Long total) {
        long pages = pageSize == null || pageSize <= 0
                ? 0L
                : (total + pageSize - 1) / pageSize;

        return PageResult.<T>builder()
                .pageNum(pageNum)
                .pageSize(pageSize)
                .total(total)
                .pages(pages)
                .records(records)
                .build();
    }
}
```

该分页查询对象用于接收前端分页参数，并把页码限制在合理范围内。

文件位置：`src/main/java/io/github/atengk/jackson/dto/PageQuery.java`

```java
package io.github.atengk.jackson.dto;

import cn.hutool.core.util.NumberUtil;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
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
     * 当前页码，从 1 开始。
     */
    @JsonSetter(nulls = Nulls.SKIP)
    private Integer pageNum = 1;

    /**
     * 每页数量。
     */
    @JsonSetter(nulls = Nulls.SKIP)
    private Integer pageSize = 10;

    /**
     * 获取安全页码。
     *
     * @return 安全页码
     */
    public Integer safePageNum() {
        return NumberUtil.max(pageNum, 1);
    }

    /**
     * 获取安全每页数量。
     *
     * @return 安全每页数量
     */
    public Integer safePageSize() {
        return NumberUtil.min(NumberUtil.max(pageSize, 1), 100);
    }

    /**
     * 转为零基页码。
     *
     * @return 零基页码
     */
    public Integer zeroBasedPageNum() {
        return safePageNum() - 1;
    }
}
```

该接口模拟分页查询，并返回稳定的 `PageResult<T>`。

文件位置：`src/main/java/io/github/atengk/jackson/controller/UserPageController.java`

```java
package io.github.atengk.jackson.controller;

import cn.hutool.core.collection.CollUtil;
import io.github.atengk.jackson.common.PageResult;
import io.github.atengk.jackson.dto.PageQuery;
import io.github.atengk.jackson.vo.UserItemVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户分页接口。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@RestController
public class UserPageController {

    /**
     * 分页查询用户列表。
     *
     * @param query 分页查询参数
     * @return 用户分页结果
     */
    @GetMapping("/api/users/page")
    public PageResult<UserItemVO> pageUsers(PageQuery query) {
        Integer pageNum = query.safePageNum();
        Integer pageSize = query.safePageSize();

        List<UserItemVO> records = CollUtil.newArrayList(
                new UserItemVO(10001L, "ateng"),
                new UserItemVO(10002L, "jackson")
        );

        log.info("分页查询用户列表，页码：{}，每页数量：{}", pageNum, pageSize);
        return PageResult.of(records, pageNum, pageSize, 35L);
    }
}
```

验证接口：

```bash
curl "http://localhost:8080/api/users/page?pageNum=1&pageSize=2"
```

响应示例：

```json
{
  "page_num": 1,
  "page_size": 2,
  "total": "35",
  "pages": "18",
  "records": [
    {
      "user_id": "10001",
      "username": "ateng"
    },
    {
      "user_id": "10002",
      "username": "jackson"
    }
  ]
}
```

如果前文启用了 Long 转字符串模块，`total`、`pages` 和 `user_id` 会输出为字符串。若分页总数希望保持数字类型，可以将 `PageResult.total`、`PageResult.pages` 改为 `Integer` 或不启用全局 Long 转字符串，而是仅对 ID 字段做字段级序列化。

## 最佳实践

最佳实践用于约束团队在 Spring Boot 4 + Jackson 3 项目中的 JSON 使用方式。核心目标是减少配置分散、避免实体泄露、保持 JSON 契约稳定，并降低 Jackson 2 到 Jackson 3 迁移风险。

### 配置集中管理

配置集中管理要求项目中所有全局 JSON 策略统一放在固定位置，例如 `application.yml`、`JacksonConfig`、`JacksonModuleConfig`。不要在 Controller、Service 或工具类中临时 new `JsonMapper`，否则会绕过 Spring Boot 自动配置、模块注册、命名策略和统一日期格式。

推荐目录结构如下：

```text
src/main/resources/application.yml
src/main/java/io/github/atengk/jackson/config/JacksonConfig.java
src/main/java/io/github/atengk/jackson/config/JacksonDateTimeConfig.java
src/main/java/io/github/atengk/jackson/config/JacksonLongConfig.java
src/main/java/io/github/atengk/jackson/config/JacksonModuleConfig.java
src/main/java/io/github/atengk/jackson/serializer
src/main/java/io/github/atengk/jackson/deserializer
```

全局基础配置示例：

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  jackson:
    # 统一使用下划线命名，Java userId 对应 JSON user_id
    property-naming-strategy: SNAKE_CASE

    # null 字段不输出，减少响应体积
    default-property-inclusion: non_null

    # 自动发现并注册 Jackson 模块
    find-and-add-modules: true

    serialization:
      # 生产环境不格式化输出
      indent-output: false

      # 空对象不抛出异常
      fail-on-empty-beans: false

    deserialization:
      # 兼容客户端多传字段
      fail-on-unknown-properties: false

      # 禁止浮点数转整数，避免数据被截断
      accept-float-as-int: false
```

配置类建议只做全局策略，不写业务判断。

文件位置：`src/main/java/io/github/atengk/jackson/config/JacksonConfig.java`

```java
package io.github.atengk.jackson.config;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.SerializationFeature;

/**
 * Jackson 全局基础配置。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Configuration(proxyBeanMethods = false)
public class JacksonConfig {

    /**
     * 定制 JsonMapper 构建器。
     *
     * @return JsonMapper 构建器定制器
     */
    @Bean
    public JsonMapperBuilderCustomizer jsonMapperBuilderCustomizer() {
        return builder -> builder
                .disable(SerializationFeature.INDENT_OUTPUT)
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
                .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS);
    }
}
```

Spring Boot 4 推荐通过一个或多个 `JsonMapperBuilderCustomizer` Bean 定制上下文中的 `JsonMapper.Builder`，并且这些定制器可以通过顺序控制在 Boot 默认定制前后执行。([Home](https://docs.spring.io/spring-boot/api/java/org/springframework/boot/jackson/autoconfigure/JsonMapperBuilderCustomizer.html?utm_source=chatgpt.com))

### DTO 与实体隔离

DTO 与实体隔离要求接口层不要直接返回数据库实体，也不要直接用实体接收 `@RequestBody`。实体类服务于持久化，DTO/VO 服务于接口契约，两者职责不同。直接暴露实体会带来字段泄露、循环引用、懒加载异常、字段命名失控和接口契约不稳定等问题。

推荐分层如下：

| 类型             | 用途                           |
| ---------------- | ------------------------------ |
| Entity           | 数据库表映射                   |
| DTO              | 接收请求参数                   |
| VO               | 返回接口响应                   |
| Convert / Mapper | DTO、Entity、VO 转换           |
| Service          | 业务处理，不直接关心 JSON 注解 |

示例实体类不添加 JSON 注解，只保留数据结构。

文件位置：`src/main/java/io/github/atengk/jackson/entity/UserEntity.java`

```java
package io.github.atengk.jackson.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
public class UserEntity {

    /**
     * 主键编号。
     */
    private Long id;

    /**
     * 用户名。
     */
    private String username;

    /**
     * 密码摘要。
     */
    private String passwordHash;

    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;
}
```

响应 VO 只暴露接口需要的字段。

文件位置：`src/main/java/io/github/atengk/jackson/vo/UserDetailVO.java`

```java
package io.github.atengk.jackson.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户详情响应对象。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
@Builder
public class UserDetailVO {

    /**
     * 用户编号。
     */
    private Long userId;

    /**
     * 用户名。
     */
    private String username;

    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;
}
```

转换类集中处理字段映射，避免 Controller 中散落转换逻辑。

文件位置：`src/main/java/io/github/atengk/jackson/convert/UserConvert.java`

```java
package io.github.atengk.jackson.convert;

import io.github.atengk.jackson.entity.UserEntity;
import io.github.atengk.jackson.vo.UserDetailVO;

/**
 * 用户对象转换器。
 *
 * @author Ateng
 * @since 2026-05-09
 */
public class UserConvert {

    private UserConvert() {
    }

    /**
     * 实体转详情响应对象。
     *
     * @param entity 用户实体
     * @return 用户详情响应对象
     */
    public static UserDetailVO toDetailVO(UserEntity entity) {
        if (entity == null) {
            return null;
        }

        return UserDetailVO.builder()
                .userId(entity.getId())
                .username(entity.getUsername())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
```

隔离后的接口只返回 VO，不暴露 Entity。

文件位置：`src/main/java/io/github/atengk/jackson/controller/UserDetailController.java`

```java
package io.github.atengk.jackson.controller;

import io.github.atengk.jackson.convert.UserConvert;
import io.github.atengk.jackson.entity.UserEntity;
import io.github.atengk.jackson.vo.UserDetailVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * 用户详情接口。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@RestController
public class UserDetailController {

    /**
     * 获取用户详情。
     *
     * @return 用户详情
     */
    @GetMapping("/api/users/detail")
    public UserDetailVO getUserDetail() {
        UserEntity entity = new UserEntity();
        entity.setId(10001L);
        entity.setUsername("ateng");
        entity.setPasswordHash("不允许返回到前端");
        entity.setCreatedAt(LocalDateTime.of(2026, 5, 9, 10, 30, 15));

        log.info("查询用户详情，用户编号：{}", entity.getId());
        return UserConvert.toDetailVO(entity);
    }
}
```

该接口不会输出 `passwordHash`，因为响应对象中根本不存在该字段。相比依赖 `@JsonIgnore`，这种方式更稳定。

### JSON 契约稳定性

JSON 契约稳定性要求接口字段名、字段类型、日期格式、枚举值、空值策略、分页结构和错误结构在版本周期内保持稳定。Jackson 配置变更、DTO 字段重命名、枚举序列化策略调整都会影响外部消费者，因此必须通过测试和版本管理控制风险。

建议建立以下契约规则：

| 契约项   | 推荐规则                                               |
| -------- | ------------------------------------------------------ |
| 字段命名 | 全局统一 `snake_case` 或 `camelCase`，不要混用         |
| 日期时间 | 统一字符串格式，不同接口不要同时出现时间戳和字符串     |
| 枚举     | 对外输出稳定 code，不直接依赖枚举常量名                |
| Long ID  | 面向前端输出字符串，避免精度丢失                       |
| 空值     | 明确是否输出 null，不随意切换                          |
| 分页     | 使用自定义 `PageResult<T>`，不要直接暴露 `PageImpl<T>` |
| 错误响应 | 使用统一错误结构，不暴露底层异常堆栈                   |
| 快照测试 | 核心接口纳入 JSON 快照测试                             |

可以用接口快照文件约束核心 JSON 输出。

文件位置：`src/test/resources/json-contract/user-detail.json`

```json
{
  "user_id": "10001",
  "username": "ateng",
  "created_at": "2026-05-09 10:30:15"
}
```

该测试用于验证用户详情响应是否保持契约稳定。

文件位置：`src/test/java/io/github/atengk/jackson/UserDetailContractTest.java`

```java
package io.github.atengk.jackson;

import cn.hutool.core.io.resource.ResourceUtil;
import io.github.atengk.jackson.vo.UserDetailVO;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;

/**
 * 用户详情 JSON 契约测试。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@SpringBootTest(properties = {
        "spring.jackson.property-naming-strategy=SNAKE_CASE",
        "spring.jackson.default-property-inclusion=non_null"
})
class UserDetailContractTest {

    @Autowired
    private JsonMapper jsonMapper;

    /**
     * 验证用户详情 JSON 契约稳定。
     *
     * @throws Exception JSON 序列化或断言异常
     */
    @Test
    void shouldKeepUserDetailJsonContractStable() throws Exception {
        String expectedJson = ResourceUtil.readUtf8Str("json-contract/user-detail.json");

        UserDetailVO user = UserDetailVO.builder()
                .userId(10001L)
                .username("ateng")
                .createdAt(LocalDateTime.of(2026, 5, 9, 10, 30, 15))
                .build();

        String actualJson = jsonMapper.writeValueAsString(user);
        JSONAssert.assertEquals(expectedJson, actualJson, JSONCompareMode.STRICT);
    }
}
```

当快照测试失败时，不应直接更新快照。应先判断本次 JSON 变化是否是业务需求变更。如果是破坏性变更，应同步更新接口文档、前端代码、第三方对接文档和版本说明。

### 迁移风险控制

迁移风险控制用于降低 Spring Boot 3 + Jackson 2 升级到 Spring Boot 4 + Jackson 3 时的不可预期变化。Jackson 3 在包名、核心类型、异常类型和自定义扩展点上都有变化；Spring Framework 7 中 JSON HTTP 消息转换器使用 Jackson 3 的 `JsonMapper` 读写 JSON，默认支持 `application/json` 和 `application/*+json`。([Home](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/http/converter/json/JacksonJsonHttpMessageConverter.html?utm_source=chatgpt.com))

迁移前建议先建立清单：

| 检查项                                        | 处理方式                                                     |
| --------------------------------------------- | ------------------------------------------------------------ |
| `com.fasterxml.jackson.databind.ObjectMapper` | 迁移到 `tools.jackson.databind.json.JsonMapper`              |
| `JsonSerializer` / `JsonDeserializer`         | 迁移到 `ValueSerializer` / `ValueDeserializer`               |
| `@JsonComponent`                              | 迁移到 `@JacksonComponent`                                   |
| `Jackson2ObjectMapperBuilderCustomizer`       | 迁移到 `JsonMapperBuilderCustomizer`                         |
| 自定义 Module                                 | 调整为 Jackson 3 `JacksonModule`                             |
| DTO 注解                                      | 保留通用 `com.fasterxml.jackson.annotation`，调整 Databind 注解包 |
| 第三方 Starter                                | 确认是否支持 Spring Boot 4 和 Jackson 3                      |
| 快照测试                                      | 对比迁移前后 JSON 输出                                       |

迁移排查命令如下：

```bash
# 排查 Jackson 2 相关依赖
mvn dependency:tree -Dincludes=com.fasterxml.jackson

# 排查 Jackson 3 相关依赖
mvn dependency:tree -Dincludes=tools.jackson

# 搜索 Jackson 2 Databind 旧包名
grep -R "com.fasterxml.jackson.databind" -n src/main/java src/test/java

# 搜索旧版 Spring Boot Jackson 2 自定义器
grep -R "Jackson2ObjectMapperBuilderCustomizer" -n src/main/java src/test/java

# 搜索旧版 @JsonComponent
grep -R "@JsonComponent" -n src/main/java src/test/java
```

命令说明：`mvn dependency:tree` 用于确认依赖树中是否仍有 Jackson 2 或 Jackson 3；`grep -R` 用于在源码中定位旧包名和旧扩展点。迁移过程中允许短期存在 Jackson 2 依赖，但主项目的 REST JSON 链路应统一到 Jackson 3。

推荐迁移步骤如下：

1. 先补充核心接口 JSON 快照测试，锁定当前契约。
2. 升级 Spring Boot 4、Spring Framework 7 和构建插件。
3. 替换 Jackson 2 Databind 包名和自定义序列化器类型。
4. 替换 Spring Boot Jackson 2 扩展点。
5. 统一配置 `spring.jackson.*` 和 `JsonMapperBuilderCustomizer`。
6. 检查第三方依赖是否注册了 Jackson 2 消息转换器。
7. 运行单元测试、WebMvcTest、契约测试和核心接口回归测试。
8. 对 JSON 差异做变更评审，确认是否需要版本升级或兼容层。

如果必须兼容旧 JSON 契约，可以采用过渡策略：

| 风险点          | 过渡策略                                      |
| --------------- | --------------------------------------------- |
| 字段名变化      | 使用 `@JsonProperty` 固定字段名               |
| 入参历史字段名  | 使用 `@JsonAlias` 接收旧字段                  |
| 日期格式变化    | 统一注册日期时间序列化器和反序列化器          |
| 枚举格式变化    | 使用 `@JsonValue` 和 `@JsonCreator` 固定 code |
| Long 精度变化   | 对 ID 字段输出字符串                          |
| 分页结构变化    | 使用自定义 `PageResult<T>` 屏蔽底层分页实现   |
| 三方 SDK 不兼容 | 用适配层隔离，不向业务代码扩散 Jackson 2 类型 |

迁移完成后，建议把 JSON 契约测试纳入 CI。后续任何 Jackson 配置、DTO 字段、枚举、日期时间格式、分页结构或统一响应结构变更，都应先通过测试暴露差异，再由接口负责人确认是否允许变更。
