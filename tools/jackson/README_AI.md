# SpringBoot 的 Jackson 开发

SpringBoot3 中的 JSON 处理默认以 Jackson 为核心，主要用于完成 Java 对象与 JSON 数据之间的序列化、反序列化，以及 Controller 请求体和响应体的数据转换。Spring Boot 官方文档说明，Spring Boot 支持 Gson、Jackson 和 JSON-B，其中 Jackson 是首选且默认的 JSON 库；当 Jackson 存在于 classpath 中时，Spring Boot 会自动配置 `ObjectMapper`。([Spring 企业文档](https://docs.enterprise.spring.io/spring-boot/reference/features/json.html?utm_source=chatgpt.com))

## Jackson 概述

本章主要说明 Jackson 在 SpringBoot3 Web 应用中的定位。理解 Jackson 与 Spring MVC、`ObjectMapper`、HTTP 消息转换器之间的关系后，后续处理日期格式、字段命名、枚举输出、Long 精度、null 字段策略和自定义序列化时会更清晰。

### Jackson 在 SpringBoot3 中的作用

Jackson 是 SpringBoot3 中默认使用的 JSON 序列化与反序列化工具。它负责把 Java 对象转换成 JSON 字符串，也负责把客户端提交的 JSON 字符串转换成 Java 对象。Spring Boot 的 JSON 文档明确说明，Jackson 是默认 JSON 库，并且属于 `spring-boot-starter-json` 的组成部分。([Spring 企业文档](https://docs.enterprise.spring.io/spring-boot/reference/features/json.html?utm_source=chatgpt.com))

在实际开发中，开发者通常不会直接在 Controller 中手动调用 `ObjectMapper`。当接口方法使用 `@RequestBody` 接收 JSON 请求体时，Spring MVC 会自动调用 Jackson 将请求体转换成 Java 对象；当接口返回 Java 对象时，Spring MVC 会自动调用 Jackson 将返回值转换成 JSON 响应体。

常见作用如下：

| 场景             | Jackson 的作用                                               |
| ---------------- | ------------------------------------------------------------ |
| 接收 JSON 请求体 | 将 JSON 字符串反序列化为 DTO、VO、Map 或集合                 |
| 返回 JSON 响应   | 将 Java 对象序列化为 JSON 字符串                             |
| 日期时间处理     | 处理 `LocalDate`、`LocalDateTime`、`Instant` 等类型          |
| 字段输出控制     | 控制字段命名、忽略字段、null 字段、默认值、枚举输出          |
| 自定义转换       | 通过 `JsonSerializer`、`JsonDeserializer`、`Module` 扩展转换逻辑 |

在项目分层中，Jackson 通常属于接口数据转换层。业务层不应该依赖 JSON 字符串处理逻辑，而应该通过 DTO、VO、统一响应结构和全局 JSON 配置来保证接口输出的一致性。

### SpringBoot3 默认 JSON 处理机制

SpringBoot3 Web 项目通常通过 `spring-boot-starter-web` 引入 Web 开发能力。该 Starter 会间接引入 JSON 处理相关能力，因此大多数 REST API 项目不需要单独声明 Jackson 核心依赖。Spring Boot 官方文档说明，Jackson 自动配置由 Spring Boot 提供，且当 Jackson 在 classpath 中时会自动配置 `ObjectMapper`。([Spring 企业文档](https://docs.enterprise.spring.io/spring-boot/reference/features/json.html?utm_source=chatgpt.com))

默认处理流程如下：

1. 客户端发送 `Content-Type: application/json` 的 HTTP 请求。
2. Spring MVC 根据请求头、接口参数类型和消息转换器列表选择 JSON 消息转换器。
3. Jackson 将 JSON 请求体反序列化为 Controller 方法参数。
4. Controller 执行业务逻辑并返回 Java 对象。
5. Spring MVC 根据返回值类型和响应内容类型选择消息转换器。
6. Jackson 将 Java 返回值序列化为 JSON 响应体。

例如下面的接口中，`UserCreateRequest` 的 JSON 解析和 `UserVO` 的 JSON 输出都由 Jackson 自动完成。

```java
@PostMapping("/users")
public UserVO createUser(@RequestBody UserCreateRequest request) {
    return userService.createUser(request);
}
```

上面代码中，开发者只需要关注请求 DTO 和响应 VO 的字段设计，不需要手动处理 JSON 字符串。Jackson 会在 Spring MVC 的请求处理链路中自动完成对象转换。

### Jackson 与 HTTP 消息转换器的关系

Jackson 本身只负责 JSON 与 Java 对象之间的转换，而 Spring MVC 通过 `HttpMessageConverter` 将 Jackson 接入 HTTP 请求和响应流程。Spring Framework 6.x 中常用的 JSON 消息转换器是 `MappingJackson2HttpMessageConverter`，官方 API 文档说明该转换器使用 Jackson 2.x 的 `ObjectMapper` 读写 JSON，默认支持 `application/json` 和 `application/*+json`。([Home](https://docs.spring.io/spring-framework/docs/6.2.14/javadoc-api/org/springframework/http/converter/json/MappingJackson2HttpMessageConverter.html?utm_source=chatgpt.com))

三者关系可以理解为：

| 组件                                  | 作用                                                   |
| ------------------------------------- | ------------------------------------------------------ |
| `ObjectMapper`                        | Jackson 核心对象，负责 JSON 序列化和反序列化           |
| `MappingJackson2HttpMessageConverter` | Spring MVC 的 JSON 消息转换器，内部使用 `ObjectMapper` |
| `HttpMessageConverters`               | Spring Boot 管理的一组 HTTP 消息转换器                 |

一般情况下，项目中只需要配置 `ObjectMapper` 或 `Jackson2ObjectMapperBuilderCustomizer`，不建议直接替换 `MappingJackson2HttpMessageConverter`。只有在需要修改支持的媒体类型、定制特殊响应格式，或者需要为某类数据单独指定转换规则时，才需要直接配置 HTTP 消息转换器。

## 环境与依赖

本章说明 SpringBoot3 项目中使用 Jackson 所需的基础依赖。对于普通 REST API 项目，推荐优先使用 `spring-boot-starter-web`，让 Spring Boot 统一管理 Jackson 相关模块和版本。

### SpringBoot3 Web 依赖

SpringBoot3 Web 项目通常只需要引入 `spring-boot-starter-web`。Starter 会提供 Spring MVC、嵌入式 Web 容器、JSON 处理等 Web 开发常用能力。Spring Boot 文档说明，Starter 是一组便捷依赖描述，用于帮助开发者一次性引入某类场景所需的依赖，并由 Spring Boot 统一管理版本。([Spring 企业文档](https://docs.enterprise.spring.io/spring-boot/reference/features/json.html?utm_source=chatgpt.com))

文件位置：`pom.xml`

```xml
<dependencies>
    <!-- SpringBoot Web 开发依赖：提供 Spring MVC、嵌入式容器、JSON 消息转换等能力 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- 测试依赖：用于单元测试、SpringBoot 测试和接口层测试 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

如果项目使用 `spring-boot-starter-parent` 或 Spring Boot BOM 管理依赖版本，通常不需要为 Jackson 相关依赖手动指定版本。这样可以避免 `jackson-databind`、`jackson-core`、`jackson-annotations` 等模块版本不一致导致运行时异常。

对于非 Web 模块，例如工具类模块、消息消费模块、定时任务模块，如果只需要 JSON 转换能力，可以单独引入 `spring-boot-starter-json`。

文件位置：`pom.xml`

```xml
<dependencies>
    <!-- 非 Web 场景下单独引入 JSON 处理能力，不包含 Spring MVC 和嵌入式 Web 容器 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-json</artifactId>
    </dependency>
</dependencies>
```

### Jackson 自动装配依赖

Jackson 的自动装配主要由 Spring Boot 完成。Spring Boot 官方文档说明，Jackson 是 `spring-boot-starter-json` 的组成部分，当 Jackson 在 classpath 中时，Spring Boot 会自动配置一个 `ObjectMapper` Bean。([Spring 企业文档](https://docs.enterprise.spring.io/spring-boot/reference/features/json.html?utm_source=chatgpt.com))

在 Web 项目中，常见依赖链路如下：

```text
spring-boot-starter-web
└── spring-boot-starter-json
    ├── jackson-databind
    ├── jackson-core
    ├── jackson-annotations
    ├── jackson-datatype-jdk8
    ├── jackson-datatype-jsr310
    └── jackson-module-parameter-names
```

核心模块说明如下：

| 依赖                             | 作用                                                       |
| -------------------------------- | ---------------------------------------------------------- |
| `jackson-databind`               | 提供 `ObjectMapper`、对象绑定、序列化、反序列化等核心能力  |
| `jackson-core`                   | 提供底层 JSON 解析和生成能力                               |
| `jackson-annotations`            | 提供 Jackson 注解能力，例如 `@JsonProperty`、`@JsonIgnore` |
| `jackson-datatype-jsr310`        | 支持 Java 8 日期时间 API，例如 `LocalDateTime`             |
| `jackson-datatype-jdk8`          | 支持 JDK 8 类型，例如 `Optional`                           |
| `jackson-module-parameter-names` | 支持基于构造方法参数名的反序列化                           |

可以在项目根目录执行下面命令查看 Jackson 依赖来源。

```bash
mvn dependency:tree -Dincludes=com.fasterxml.jackson
```

该命令用于输出当前 Maven 项目中 `com.fasterxml.jackson` 相关依赖的版本和传递来源。重点检查是否存在多个不同版本的 Jackson 模块。如果发现版本不一致，优先删除手动声明的 Jackson 版本，让 Spring Boot 依赖管理统一控制。

### 常用 Jackson 模块

Jackson 采用模块化设计，不同数据类型和数据格式由不同模块支持。Spring Boot 默认集成 Jackson 后，常用 JSON 处理模块一般已经可用；只有在处理 XML、YAML、CSV 等特殊格式时，才需要额外引入对应的数据格式模块。Spring Framework 的 JSON 消息转换器默认基于 Jackson 2.x 的 `ObjectMapper` 工作。([Home](https://docs.spring.io/spring-framework/docs/6.2.14/javadoc-api/org/springframework/http/converter/json/MappingJackson2HttpMessageConverter.html?utm_source=chatgpt.com))

常用模块如下：

| 模块                             | 主要作用                  | 常见场景                                |
| -------------------------------- | ------------------------- | --------------------------------------- |
| `jackson-databind`               | Java 对象与 JSON 数据绑定 | DTO、VO、Map、List 与 JSON 互转         |
| `jackson-core`                   | JSON 底层读写能力         | 流式 JSON 解析、生成                    |
| `jackson-annotations`            | Jackson 注解支持          | 字段命名、忽略字段、日期格式化          |
| `jackson-datatype-jsr310`        | Java 8 日期时间支持       | `LocalDate`、`LocalDateTime`、`Instant` |
| `jackson-datatype-jdk8`          | JDK 8 类型支持            | `Optional`、`OptionalLong`              |
| `jackson-module-parameter-names` | 构造参数名识别            | 不可变对象、构造方法反序列化            |
| `jackson-dataformat-xml`         | XML 格式支持              | 对接 XML 接口、JSON/XML 双格式输出      |

如果项目需要支持 XML，可以额外添加 `jackson-dataformat-xml`。

文件位置：`pom.xml`

```xml
<dependencies>
    <!-- Jackson XML 支持：用于 Java 对象与 XML 内容互相转换 -->
    <dependency>
        <groupId>com.fasterxml.jackson.dataformat</groupId>
        <artifactId>jackson-dataformat-xml</artifactId>
    </dependency>
</dependencies>
```

实际项目中不建议随意单独指定 Jackson 模块版本。SpringBoot3 已经通过依赖管理提供了一组兼容版本，手动覆盖版本可能导致 `NoSuchMethodError`、`ClassNotFoundException`、日期时间序列化异常或行为不一致。只有在明确需要安全修复、兼容第三方 SDK 或升级特定 Jackson 功能时，才建议单独调整版本，并通过完整回归测试验证。



## 基础使用

本章主要说明 Jackson 在业务代码中的基础调用方式。虽然在 SpringBoot3 的 Controller 请求和响应处理中，Jackson 通常由 Spring MVC 自动调用，但在日志处理、消息消费、缓存转换、第三方接口适配、单元测试和工具类封装中，仍然经常需要直接使用 `ObjectMapper`。

Jackson 的核心入口是 `ObjectMapper`。官方 Javadoc 说明，`ObjectMapper` 可用于将 Java 对象写出为 JSON，也可以通过 `readValue` 将 JSON 读取为 Java 类型；对于复杂泛型类型，可以结合 `TypeReference` 或 `JavaType` 使用。([Javadoc](https://javadoc.io/static/com.fasterxml.jackson.core/jackson-databind/2.17.3/com/fasterxml/jackson/databind/ObjectMapper.html?utm_source=chatgpt.com))

### 对象序列化

对象序列化是指将 Java 对象转换为 JSON 字符串。常见场景包括接口响应输出、日志记录、消息发送、缓存写入和第三方接口请求参数构造。

在 SpringBoot3 项目中，推荐直接注入 Spring Boot 自动配置好的 `ObjectMapper`，不要在业务代码中频繁 `new ObjectMapper()`。这样可以复用项目中的全局日期格式、字段命名策略、模块注册和序列化特性配置。

文件位置：`src/main/java/io/github/atengk/jackson/dto/UserInfoDTO.java`

```java
package io.github.atengk.jackson.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户信息 DTO
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoDTO {

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
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime createTime;
}
```

文件位置：`src/main/java/io/github/atengk/jackson/service/JacksonSerializeService.java`

```java
package io.github.atengk.jackson.service;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.atengk.jackson.dto.UserInfoDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Jackson 对象序列化服务
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JacksonSerializeService {

    private final ObjectMapper objectMapper;

    /**
     * 将用户对象序列化为 JSON 字符串
     *
     * @param userInfo 用户信息
     * @return JSON 字符串
     */
    public String serializeUser(UserInfoDTO userInfo) {
        try {
            String json = objectMapper.writeValueAsString(userInfo);
            log.info("用户对象序列化成功，用户ID：{}", userInfo.getId());
            return json;
        } catch (JsonProcessingException e) {
            log.error("用户对象序列化失败，用户ID：{}", userInfo == null ? null : userInfo.getId(), e);
            throw new IllegalArgumentException(StrUtil.format("用户对象序列化失败：{}", e.getMessage()), e);
        }
    }
}
```

上面代码使用 `writeValueAsString` 将 Java 对象转换为 JSON 字符串。该方法适合生成接口日志、消息体、缓存值或第三方接口请求体。对于文件输出，也可以使用 `writeValue(File, Object)`，但在 Web 项目中更常见的是直接生成字符串或字节数组。([Javadoc](https://javadoc.io/static/com.fasterxml.jackson.core/jackson-databind/2.17.3/com/fasterxml/jackson/databind/ObjectMapper.html?utm_source=chatgpt.com))

示例调用结果如下：

```json
{
  "id": 10001,
  "username": "ateng",
  "nickname": "阿腾",
  "createTime": "2026-05-09 10:30:00"
}
```

### 对象反序列化

对象反序列化是指将 JSON 字符串转换为 Java 对象。常见场景包括读取 MQ 消息、解析 Redis 缓存、处理第三方接口回调、读取配置文件和编写单元测试。

在 Controller 中，`@RequestBody` 的反序列化通常由 Spring MVC 自动完成；在普通 Service 或工具类中，可以直接使用 `ObjectMapper#readValue`。Spring MVC 的 JSON 消息转换器底层也会使用 Jackson 的 `ObjectMapper` 读写 JSON。([Home](https://docs.spring.io/spring-framework/docs/6.2.14/javadoc-api/org/springframework/http/converter/json/MappingJackson2HttpMessageConverter.html?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/jackson/service/JacksonDeserializeService.java`

```java
package io.github.atengk.jackson.service;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.atengk.jackson.dto.UserInfoDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Jackson 对象反序列化服务
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JacksonDeserializeService {

    private final ObjectMapper objectMapper;

    /**
     * 将 JSON 字符串反序列化为用户对象
     *
     * @param json JSON 字符串
     * @return 用户信息
     */
    public UserInfoDTO deserializeUser(String json) {
        if (StrUtil.isBlank(json)) {
            throw new IllegalArgumentException("JSON 字符串不能为空");
        }

        try {
            UserInfoDTO userInfo = objectMapper.readValue(json, UserInfoDTO.class);
            log.info("用户 JSON 反序列化成功，用户ID：{}", userInfo.getId());
            return userInfo;
        } catch (JsonProcessingException e) {
            log.error("用户 JSON 反序列化失败，原始内容：{}", json, e);
            throw new IllegalArgumentException(StrUtil.format("用户 JSON 反序列化失败：{}", e.getMessage()), e);
        }
    }
}
```

示例 JSON 如下：

```json
{
  "id": 10001,
  "username": "ateng",
  "nickname": "阿腾",
  "createTime": "2026-05-09 10:30:00"
}
```

需要注意，反序列化时 JSON 字段名需要能够匹配 Java 对象字段名，或者通过 `@JsonProperty` 指定映射关系。如果 JSON 中存在 Java 对象没有定义的字段，是否报错取决于 `ObjectMapper` 的配置。Spring Boot 默认会关闭 `FAIL_ON_UNKNOWN_PROPERTIES`，因此一般不会因为未知字段直接失败。

### JSON 字符串与 Java 对象转换

在实际业务中，除了明确的序列化和反序列化，还经常需要在 Java 对象、`Map`、DTO、VO 之间进行转换。例如读取第三方接口返回的 JSON 后，先转成 `Map` 做字段判断，再转成业务 DTO；或者将 Entity 转成 VO。

`ObjectMapper#convertValue` 可以用于 Java 对象之间的结构转换。官方 Javadoc 说明，该方法类似于先将源对象写成 JSON，再绑定到目标类型，但实现上会避免完整 JSON 字符串序列化过程，并复用当前 `ObjectMapper` 的配置。([Javadoc](https://javadoc.io/static/com.fasterxml.jackson.core/jackson-databind/2.17.3/com/fasterxml/jackson/databind/ObjectMapper.html?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/jackson/vo/UserInfoVO.java`

```java
package io.github.atengk.jackson.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户信息 VO
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
public class UserInfoVO {

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 显示名称
     */
    private String nickname;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
```

文件位置：`src/main/java/io/github/atengk/jackson/service/JacksonConvertService.java`

```java
package io.github.atengk.jackson.service;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.atengk.jackson.dto.UserInfoDTO;
import io.github.atengk.jackson.vo.UserInfoVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Jackson 数据转换服务
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JacksonConvertService {

    private final ObjectMapper objectMapper;

    /**
     * JSON 字符串转 Map
     *
     * @param json JSON 字符串
     * @return Map 数据
     */
    public Map<String, Object> jsonToMap(String json) {
        if (StrUtil.isBlank(json)) {
            return MapUtil.empty();
        }

        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
            });
        } catch (JsonProcessingException e) {
            log.error("JSON 转 Map 失败，原始内容：{}", json, e);
            throw new IllegalArgumentException("JSON 转 Map 失败", e);
        }
    }

    /**
     * DTO 转 VO
     *
     * @param userInfo 用户 DTO
     * @return 用户 VO
     */
    public UserInfoVO dtoToVo(UserInfoDTO userInfo) {
        if (userInfo == null) {
            return null;
        }

        UserInfoVO userInfoVO = objectMapper.convertValue(userInfo, UserInfoVO.class);
        log.info("用户 DTO 转 VO 成功，用户ID：{}", userInfo.getId());
        return userInfoVO;
    }
}
```

使用 `convertValue` 时，字段名称和字段类型仍然需要能够匹配。它适合结构相近的对象转换，不适合复杂业务转换。如果字段存在合并、拆分、字典翻译、权限过滤等逻辑，建议使用 MapStruct、手写转换方法或专门的 assembler 类。

### 集合与泛型类型转换

集合和泛型类型转换是 Jackson 使用中最容易出错的部分。由于 Java 泛型存在类型擦除，不能简单地使用 `List.class` 表示 `List<UserInfoDTO>`，否则反序列化后集合元素可能变成 `LinkedHashMap`。

对于泛型类型，推荐使用 `TypeReference`。`ObjectMapper#readValue` 支持通过 `TypeReference` 指定目标类型，适合处理 `List<T>`、`Map<String, T>`、分页对象、统一响应对象等泛型结构。([Javadoc](https://javadoc.io/static/com.fasterxml.jackson.core/jackson-databind/2.17.3/com/fasterxml/jackson/databind/ObjectMapper.html?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/jackson/service/JacksonGenericService.java`

```java
package io.github.atengk.jackson.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.atengk.jackson.dto.UserInfoDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Jackson 泛型转换服务
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JacksonGenericService {

    private final ObjectMapper objectMapper;

    /**
     * JSON 数组转用户列表
     *
     * @param json JSON 数组字符串
     * @return 用户列表
     */
    public List<UserInfoDTO> jsonToUserList(String json) {
        if (StrUtil.isBlank(json)) {
            return Collections.emptyList();
        }

        try {
            List<UserInfoDTO> userList = objectMapper.readValue(json, new TypeReference<List<UserInfoDTO>>() {
            });
            log.info("JSON 数组转用户列表成功，数量：{}", CollUtil.size(userList));
            return userList;
        } catch (JsonProcessingException e) {
            log.error("JSON 数组转用户列表失败，原始内容：{}", json, e);
            throw new IllegalArgumentException("JSON 数组转用户列表失败", e);
        }
    }

    /**
     * JSON 对象转用户 Map
     *
     * @param json JSON 对象字符串
     * @return 用户 Map
     */
    public Map<String, UserInfoDTO> jsonToUserMap(String json) {
        if (StrUtil.isBlank(json)) {
            return Collections.emptyMap();
        }

        try {
            Map<String, UserInfoDTO> userMap = objectMapper.readValue(json, new TypeReference<Map<String, UserInfoDTO>>() {
            });
            log.info("JSON 对象转用户 Map 成功，数量：{}", CollUtil.size(userMap));
            return userMap;
        } catch (JsonProcessingException e) {
            log.error("JSON 对象转用户 Map 失败，原始内容：{}", json, e);
            throw new IllegalArgumentException("JSON 对象转用户 Map 失败", e);
        }
    }
}
```

示例 JSON 数组如下：

```json
[
  {
    "id": 10001,
    "username": "ateng",
    "nickname": "阿腾",
    "createTime": "2026-05-09 10:30:00"
  },
  {
    "id": 10002,
    "username": "jack",
    "nickname": "杰克",
    "createTime": "2026-05-09 11:00:00"
  }
]
```

示例 JSON 对象如下：

```json
{
  "10001": {
    "id": 10001,
    "username": "ateng",
    "nickname": "阿腾",
    "createTime": "2026-05-09 10:30:00"
  },
  "10002": {
    "id": 10002,
    "username": "jack",
    "nickname": "杰克",
    "createTime": "2026-05-09 11:00:00"
  }
}
```

泛型转换的核心原则是：只要目标类型中包含泛型参数，就不要直接使用 `List.class`、`Map.class` 这类原始类型。简单对象使用 `UserInfoDTO.class`，泛型对象使用 `new TypeReference<具体泛型>() {}`。

## 常用注解

本章说明 Jackson 在 DTO、VO、枚举和字段方法上常用的注解。注解适合处理局部字段规则，例如字段改名、字段忽略、日期格式、null 输出、枚举值输出和派生字段输出。全局统一规则仍然建议放在 `ObjectMapper` 配置中。

常用注解包括 `@JsonProperty`、`@JsonIgnore`、`@JsonFormat`、`@JsonInclude`、`@JsonValue` 和 `@JsonGetter`。这些注解分别用于字段命名、忽略字段、格式化、输出条件控制、枚举值输出和自定义 getter 输出。([Adobe开发者官网](https://developer.adobe.com/experience-manager/reference-materials/6-5-lts/javadoc/com/fasterxml/jackson/annotation/JsonProperty.html?utm_source=chatgpt.com))

### 字段命名与忽略

字段命名通常使用 `@JsonProperty`。该注解可以为 Java 字段指定 JSON 中使用的属性名，常用于对接下划线字段、第三方接口字段或历史接口字段。`@JsonProperty` 的官方说明指出，非空 `value` 可以指定外部 JSON 对象中使用的属性名。([Adobe开发者官网](https://developer.adobe.com/experience-manager/reference-materials/6-5-lts/javadoc/com/fasterxml/jackson/annotation/JsonProperty.html?utm_source=chatgpt.com))

字段忽略通常使用 `@JsonIgnore`。该注解用于让 Jackson 在序列化和反序列化时忽略某个逻辑属性，适合密码、密钥、内部标记、临时字段等不应该暴露到 JSON 的数据。([Javadoc](https://www.javadoc.io/static/com.fasterxml.jackson.core/jackson-annotations/2.9.0/com/fasterxml/jackson/annotation/JsonIgnore.html?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/jackson/dto/UserAccountDTO.java`

```java
package io.github.atengk.jackson.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 用户账号 DTO
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
public class UserAccountDTO {

    /**
     * 用户ID，JSON 字段使用 user_id
     */
    @JsonProperty("user_id")
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码字段不参与 JSON 输入输出
     */
    @JsonIgnore
    private String password;

    /**
     * 手机号，JSON 字段使用 mobile_phone
     */
    @JsonProperty("mobile_phone")
    private String mobilePhone;
}
```

序列化结果示例：

```json
{
  "user_id": 10001,
  "username": "ateng",
  "mobile_phone": "13800000000"
}
```

在接口 DTO 中，建议优先保持 Java 字段命名符合 Java 规范，例如 `userId`、`mobilePhone`；如果外部 JSON 字段必须使用 `user_id`、`mobile_phone`，再通过 `@JsonProperty` 进行适配。不要为了 JSON 字段名直接把 Java 字段写成下划线风格。

### 日期时间格式化

日期时间格式化通常使用 `@JsonFormat`。该注解可以指定日期时间的输出格式、时区和形态。Jackson 官方说明中，`@JsonFormat` 是通用格式化注解，可用于配置值序列化时的表示方式，其中常见用途包括日期以字符串或数字输出，以及通过 `pattern()` 指定具体格式。([Javadoc](https://www.javadoc.io/static/com.fasterxml.jackson.core/jackson-annotations/2.9.0/com/fasterxml/jackson/annotation/JsonFormat.html?utm_source=chatgpt.com))

SpringBoot3 项目中常见日期时间类型包括 `LocalDate`、`LocalDateTime`、`Instant`、`OffsetDateTime` 等。Jackson 的 `JavaTimeModule` 用于支持 Java 8 日期时间类型。该模块用于注册 `java.time` 类型的序列化和反序列化能力。([Javadoc](https://javadoc.io/static/com.fasterxml.jackson.datatype/jackson-datatype-jsr310/2.17.3/com/fasterxml/jackson/datatype/jsr310/JavaTimeModule.html?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/jackson/dto/OrderDTO.java`

```java
package io.github.atengk.jackson.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 订单 DTO
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
public class OrderDTO {

    /**
     * 订单ID
     */
    private Long orderId;

    /**
     * 下单日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Asia/Shanghai")
    private LocalDate orderDate;

    /**
     * 支付时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime payTime;
}
```

序列化结果示例：

```json
{
  "orderId": 90001,
  "orderDate": "2026-05-09",
  "payTime": "2026-05-09 12:30:00"
}
```

如果项目中所有接口都要求统一日期格式，建议优先使用全局 `ObjectMapper` 配置或 `application.yml` 配置，而不是在每个字段上重复添加 `@JsonFormat`。字段级注解更适合处理少量特殊格式字段。

### 空值与默认值处理

空值输出通常使用 `@JsonInclude`。该注解用于控制字段在什么条件下参与序列化，例如排除 `null`、空字符串、空集合或默认值。Jackson 官方说明中，`@JsonInclude` 可用于指定属性值何时被序列化，默认情况下属性总是会被包含。([Javadoc](https://www.javadoc.io/static/com.fasterxml.jackson.core/jackson-annotations/2.8.0/com/fasterxml/jackson/annotation/JsonInclude.html?utm_source=chatgpt.com))

常用策略如下：

| 策略                              | 含义                                                 |
| --------------------------------- | ---------------------------------------------------- |
| `JsonInclude.Include.ALWAYS`      | 总是输出字段                                         |
| `JsonInclude.Include.NON_NULL`    | 字段值不为 `null` 时输出                             |
| `JsonInclude.Include.NON_EMPTY`   | 字段值非空时输出，通常会排除空字符串、空集合、空数组 |
| `JsonInclude.Include.NON_DEFAULT` | 字段值不是默认值时输出                               |

文件位置：`src/main/java/io/github/atengk/jackson/vo/ProductVO.java`

```java
package io.github.atengk.jackson.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商品 VO
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductVO {

    /**
     * 商品ID
     */
    private Long productId;

    /**
     * 商品名称
     */
    private String productName;

    /**
     * 商品价格
     */
    private BigDecimal price;

    /**
     * 商品标签，空集合不输出
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<String> tags;

    /**
     * 备注，null 时不输出
     */
    private String remark;
}
```

当 `remark` 为 `null`，`tags` 为空集合时，序列化结果可能如下：

```json
{
  "productId": 30001,
  "productName": "机械键盘",
  "price": 299.00
}
```

默认值处理不建议完全依赖 Jackson 注解完成。对于接口返回值，如果字段有明确业务默认值，例如库存默认为 `0`、状态默认为 `0`、标签默认为空集合，建议在业务组装 VO 时就赋值，保证对象本身语义完整，而不是只在 JSON 输出阶段临时处理。

### 枚举序列化

枚举序列化常见需求是输出枚举名称、编码、描述，或者输出一个包含多个字段的对象。Jackson 默认通常会按枚举名称输出，例如 `ENABLE`、`DISABLE`。如果需要输出业务 code，可以使用 `@JsonValue`。

`@JsonValue` 的官方说明指出，该注解表示使用被标注的字段或访问器方法作为对象序列化时的单一值；对于 Java 枚举，返回值还会被视为可用于反序列化的值。([Javadoc](https://javadoc.io/static/com.fasterxml.jackson.core/jackson-annotations/2.15.4/com/fasterxml/jackson/annotation/JsonValue.html?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/jackson/enums/UserStatusEnum.java`

```java
package io.github.atengk.jackson.enums;

import cn.hutool.core.util.ObjectUtil;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 用户状态枚举
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Getter
@AllArgsConstructor
public enum UserStatusEnum {

    /**
     * 启用
     */
    ENABLE(1, "启用"),

    /**
     * 禁用
     */
    DISABLE(0, "禁用");

    /**
     * 状态编码
     */
    @JsonValue
    private final Integer code;

    /**
     * 状态描述
     */
    private final String desc;

    /**
     * 根据编码获取枚举
     *
     * @param code 状态编码
     * @return 用户状态枚举
     */
    @JsonCreator
    public static UserStatusEnum of(Integer code) {
        for (UserStatusEnum statusEnum : values()) {
            if (ObjectUtil.equals(statusEnum.getCode(), code)) {
                return statusEnum;
            }
        }
        throw new IllegalArgumentException("用户状态编码不合法：" + code);
    }
}
```

文件位置：`src/main/java/io/github/atengk/jackson/vo/UserStatusVO.java`

```java
package io.github.atengk.jackson.vo;

import io.github.atengk.jackson.enums.UserStatusEnum;
import lombok.Data;

/**
 * 用户状态 VO
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
public class UserStatusVO {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户状态
     */
    private UserStatusEnum status;
}
```

序列化结果示例：

```json
{
  "userId": 10001,
  "status": 1
}
```

如果前端需要同时展示 `code` 和 `desc`，不建议直接让枚举输出复杂对象。更清晰的方式是在 VO 中显式提供 `statusCode` 和 `statusDesc` 字段，避免枚举序列化规则影响全局接口行为。

例如：

```json
{
  "userId": 10001,
  "statusCode": 1,
  "statusDesc": "启用"
}
```

### 自定义字段输出

自定义字段输出是指某些字段不直接来自对象属性，而是通过方法动态计算得到。常见场景包括脱敏字段、展示字段、组合字段、状态描述、金额展示值等。

Jackson 可以通过 `@JsonGetter` 将一个无参、有返回值的方法声明为 JSON 输出字段。官方说明中，`@JsonGetter` 可用于把非静态、无参数、有返回值的方法定义为逻辑属性的 getter；序列化对象时会调用该方法，并将返回值作为属性值输出。([Javadoc](https://www.javadoc.io/static/com.fasterxml.jackson.core/jackson-annotations/2.11.1/com/fasterxml/jackson/annotation/JsonGetter.html?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/jackson/vo/UserProfileVO.java`

```java
package io.github.atengk.jackson.vo;

import cn.hutool.core.util.DesensitizedUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

/**
 * 用户资料 VO
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
public class UserProfileVO {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 姓名
     */
    private String realName;

    /**
     * 手机号原始值，不直接输出
     */
    @JsonIgnore
    private String mobilePhone;

    /**
     * 邮箱原始值，不直接输出
     */
    @JsonIgnore
    private String email;

    /**
     * 输出脱敏手机号
     *
     * @return 脱敏手机号
     */
    @JsonGetter("mobilePhone")
    public String getMaskedMobilePhone() {
        if (StrUtil.isBlank(mobilePhone)) {
            return null;
        }
        return DesensitizedUtil.mobilePhone(mobilePhone);
    }

    /**
     * 输出脱敏邮箱
     *
     * @return 脱敏邮箱
     */
    @JsonGetter("email")
    public String getMaskedEmail() {
        if (StrUtil.isBlank(email)) {
            return null;
        }
        return DesensitizedUtil.email(email);
    }

    /**
     * 输出展示名称
     *
     * @return 展示名称
     */
    @JsonGetter("displayName")
    public String getDisplayName() {
        return StrUtil.blankToDefault(realName, "未命名用户");
    }
}
```

序列化结果示例：

```json
{
  "userId": 10001,
  "realName": "张三",
  "mobilePhone": "138****0000",
  "email": "z********@example.com",
  "displayName": "张三"
}
```

自定义字段输出适合轻量派生字段。如果逻辑涉及数据库查询、远程接口调用、复杂权限判断或上下文信息，不建议放在 getter 中执行。更推荐在 Service 层或 VO 组装层提前计算好字段，保证序列化过程只做数据输出，不承载复杂业务逻辑。



## 全局配置

全局配置用于统一控制整个 SpringBoot3 应用中的 JSON 行为，例如日期格式、时区、空值输出、未知字段处理、枚举处理和自定义模块注册。Spring Boot 官方文档说明，如果 Jackson 在 classpath 中，Spring MVC 会获得由 `Jackson2ObjectMapperBuilder` 提供的默认 JSON 转换器，并且 Spring Boot 会自动配置相关的 JSON 映射能力。([Home](https://docs.spring.io/spring-boot/3.4.0/how-to/spring-mvc.html?utm_source=chatgpt.com))

### ObjectMapper 配置

`ObjectMapper` 是 Jackson 的核心对象，负责 Java 对象与 JSON 数据之间的序列化和反序列化。在 SpringBoot3 项目中，不建议在业务代码中频繁使用 `new ObjectMapper()`，因为手动创建的对象不会自动继承 Spring Boot 中已经配置好的日期格式、模块注册、序列化策略和反序列化策略。

推荐做法是直接注入 Spring 容器中自动配置好的 `ObjectMapper`。Spring Boot 文档说明，环境配置会应用到自动配置的 `Jackson2ObjectMapperBuilder`，并影响由该 builder 创建的 mapper，包括自动配置的 `ObjectMapper`。([Home](https://docs.spring.io/spring-boot/3.4.0/how-to/spring-mvc.html?utm_source=chatgpt.com))

下面示例用于封装项目中的 JSON 转换工具，统一复用 Spring 容器中的 `ObjectMapper`。

文件位置：`src/main/java/io/github/atengk/jackson/util/JacksonUtil.java`

```java
package io.github.atengk.jackson.util;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Jackson JSON 工具类
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JacksonUtil {

    private final ObjectMapper objectMapper;

    /**
     * Java 对象转 JSON 字符串
     *
     * @param object Java 对象
     * @return JSON 字符串
     */
    public String toJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.error("对象转 JSON 失败，对象类型：{}", object == null ? null : object.getClass().getName(), e);
            throw new IllegalArgumentException("对象转 JSON 失败", e);
        }
    }

    /**
     * JSON 字符串转 Java 对象
     *
     * @param json JSON 字符串
     * @param clazz 目标类型
     * @return Java 对象
     */
    public <T> T parseObject(String json, Class<T> clazz) {
        if (StrUtil.isBlank(json)) {
            return null;
        }

        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            log.error("JSON 转对象失败，目标类型：{}，原始内容：{}", clazz.getName(), json, e);
            throw new IllegalArgumentException("JSON 转对象失败", e);
        }
    }

    /**
     * JSON 字符串转泛型对象
     *
     * @param json JSON 字符串
     * @param typeReference 泛型类型引用
     * @return 泛型对象
     */
    public <T> T parseObject(String json, TypeReference<T> typeReference) {
        if (StrUtil.isBlank(json)) {
            return null;
        }

        try {
            return objectMapper.readValue(json, typeReference);
        } catch (JsonProcessingException e) {
            log.error("JSON 转泛型对象失败，原始内容：{}", json, e);
            throw new IllegalArgumentException("JSON 转泛型对象失败", e);
        }
    }
}
```

如果确实需要完全替换 Spring Boot 默认的 `ObjectMapper`，可以声明自己的 `ObjectMapper` Bean。但 Spring Boot 文档明确说明，定义 `ObjectMapper` Bean 或 `Jackson2ObjectMapperBuilder` Bean 会禁用 `ObjectMapper` 的自动配置；如果只是增加日期格式、空值策略或模块注册，更推荐使用 `application.yml` 或 `Jackson2ObjectMapperBuilderCustomizer`。([Home](https://docs.spring.io/spring-boot/3.4.0/how-to/spring-mvc.html?utm_source=chatgpt.com))

### application.yml 配置

`application.yml` 适合配置简单、稳定、全局统一的 Jackson 行为，例如是否缩进输出、是否忽略未知字段、是否输出 null 字段、时区和普通日期格式。Spring Boot 的配置属性文档列出了 `spring.jackson.date-format`、`spring.jackson.default-property-inclusion`、`spring.jackson.deserialization.*`、`spring.jackson.serialization.*` 等配置项。([Home](https://docs.spring.io/spring-boot/appendix/application-properties/index.html?utm_source=chatgpt.com))

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  jackson:
    # 全局时区，影响 Jackson 中涉及时区的日期时间处理
    time-zone: Asia/Shanghai

    # Date、Calendar 等传统日期类型的格式
    date-format: yyyy-MM-dd HH:mm:ss

    # null 字段不参与 JSON 序列化
    default-property-inclusion: non_null

    serialization:
      # 日期不输出为时间戳
      write-dates-as-timestamps: false

      # 空 Bean 序列化时不直接失败
      fail-on-empty-beans: false

      # 开发调试阶段可以开启，生产环境一般关闭
      indent-output: false

    deserialization:
      # JSON 中出现 Java 对象不存在的字段时不报错
      fail-on-unknown-properties: false

      # 空字符串不自动转换成 null，按项目规范决定是否开启
      accept-empty-string-as-null-object: false
```

需要注意，`spring.jackson.date-format` 主要用于传统日期类型，例如 `java.util.Date` 和 `java.util.Calendar`。对于 `LocalDate`、`LocalDateTime` 等 Java 8 日期时间类型，建议通过 `JavaTimeModule`、字段上的 `@JsonFormat`，或者统一的 `Jackson2ObjectMapperBuilderCustomizer` 配置处理。

`application.yml` 的优点是简单直观，缺点是无法处理复杂类型注册、特殊序列化器和反序列化器。如果项目需要统一 `LocalDateTime` 格式、Long 转字符串、枚举 code 输出或字段脱敏，建议使用 Java 配置类。

### Jackson2ObjectMapperBuilderCustomizer 配置

`Jackson2ObjectMapperBuilderCustomizer` 是 SpringBoot3 中推荐的 Jackson 扩展方式之一。官方 API 文档说明，该接口用于通过 `Jackson2ObjectMapperBuilder` 进一步定制 `ObjectMapper`，同时保留默认自动配置。([Home](https://docs.spring.io/spring-boot/docs/3.0.7/api/org/springframework/boot/autoconfigure/jackson/Jackson2ObjectMapperBuilderCustomizer.html?utm_source=chatgpt.com))

下面示例统一配置空值策略、未知字段处理、日期时间格式和 Java 8 时间模块。Spring Boot 文档还说明，容器中 `Module` 类型的 Bean 会自动注册到自动配置的 `Jackson2ObjectMapperBuilder`，并应用到它创建的 `ObjectMapper`。([Home](https://docs.spring.io/spring-boot/3.4.0/how-to/spring-mvc.html?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/jackson/config/JacksonConfig.java`

```java
package io.github.atengk.jackson.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Jackson 全局配置
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Configuration
public class JacksonConfig {

    private static final String DATE_PATTERN = "yyyy-MM-dd";

    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    private static final String TIME_ZONE = "Asia/Shanghai";

    /**
     * 定制 SpringBoot 自动配置的 ObjectMapper 构建过程
     *
     * @return Jackson 构建器定制器
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jackson2ObjectMapperBuilderCustomizer() {
        return builder -> builder
                // 设置区域信息
                .locale(Locale.CHINA)
                // 设置默认时区
                .timeZone(TimeZone.getTimeZone(TIME_ZONE))
                // null 字段不参与序列化
                .serializationInclusion(JsonInclude.Include.NON_NULL)
                // 日期不输出为时间戳
                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                // 未知字段不导致反序列化失败
                .featuresToDisable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    /**
     * 注册 Java 8 日期时间模块
     *
     * @return Jackson 模块
     */
    @Bean
    public Module javaTimeModule() {
        JavaTimeModule module = new JavaTimeModule();

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(DATE_PATTERN);
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN);

        module.addSerializer(LocalDate.class, new LocalDateSerializer(dateFormatter));
        module.addDeserializer(LocalDate.class, new LocalDateDeserializer(dateFormatter));

        module.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(dateTimeFormatter));
        module.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(dateTimeFormatter));

        return module;
    }
}
```

这种配置方式适合大多数 SpringBoot3 Web 项目。它不会直接替换 Spring Boot 的默认 `ObjectMapper`，而是在自动配置基础上增加项目自己的 JSON 规则。

### 日期格式与时区配置

日期格式和时区建议在项目中统一规范，否则容易出现接口响应格式不一致、前端解析失败、跨时区时间偏移、数据库时间与接口时间不一致等问题。Spring Boot 默认会禁用 `SerializationFeature.WRITE_DATES_AS_TIMESTAMPS`，也就是默认不优先把日期写成时间戳形式。([Home](https://docs.spring.io/spring-boot/3.4.0/how-to/spring-mvc.html?utm_source=chatgpt.com))

推荐规范如下：

| 类型            | 推荐格式              | 示例                   |
| --------------- | --------------------- | ---------------------- |
| `LocalDate`     | `yyyy-MM-dd`          | `2026-05-09`           |
| `LocalDateTime` | `yyyy-MM-dd HH:mm:ss` | `2026-05-09 10:30:00`  |
| `Instant`       | ISO-8601 UTC 时间     | `2026-05-09T01:30:00Z` |
| `Date`          | `yyyy-MM-dd HH:mm:ss` | `2026-05-09 10:30:00`  |

如果系统只面向单一国内业务场景，可以统一使用 `Asia/Shanghai`。如果系统涉及跨国家、跨地区或开放平台接口，建议内部存储使用 UTC 或带时区类型，接口层明确说明输出时区。

下面是一个日期时间 DTO 示例，用于验证全局日期格式是否生效。

文件位置：`src/main/java/io/github/atengk/jackson/dto/DateTimeDemoDTO.java`

```java
package io.github.atengk.jackson.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 日期时间演示 DTO
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DateTimeDemoDTO {

    /**
     * 业务日期
     */
    private LocalDate businessDate;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
```

期望输出如下：

```json
{
  "businessDate": "2026-05-09",
  "createTime": "2026-05-09 10:30:00"
}
```

## 请求与响应处理

本章说明 Jackson 在 Controller 请求体、响应体、参数校验和统一响应结构中的实际应用。Spring Framework 文档说明，`@RequestBody` 会通过 `HttpMessageConverter` 读取请求体并反序列化为对象；`@RestController` 本身组合了 `@Controller` 和 `@ResponseBody`，因此其请求处理方法默认具有响应体输出语义。([Spring 企业文档](https://docs.enterprise.spring.io/spring-framework/reference/6.0/web/webmvc/mvc-controller/ann-methods/requestbody.html?utm_source=chatgpt.com))

如果使用 `@Valid`、`@NotBlank`、`@NotNull` 等校验注解，需要确保项目中存在 Bean Validation 实现。Spring Boot 文档说明，只要 JSR-303 实现，例如 Hibernate Validator，在 classpath 中，Bean Validation 相关能力即可启用。([Home](https://docs.spring.io/spring-boot/reference/io/validation.html?utm_source=chatgpt.com))

如果项目尚未引入校验依赖，可以添加以下配置。

文件位置：`pom.xml`

```xml
<dependencies>
    <!-- 参数校验依赖：提供 jakarta.validation 注解和 Hibernate Validator 实现 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- Hutool 工具类：用于字符串、集合、脱敏、ID 等常用工具处理 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.8.36</version>
    </dependency>
</dependencies>
```

### Controller 请求体反序列化

Controller 请求体反序列化是指客户端提交 JSON 请求体后，Spring MVC 使用 Jackson 将 JSON 内容转换为 Java DTO。`@RequestBody` 的官方文档说明，请求体会通过 `HttpMessageConverter` 按请求内容类型解析为方法参数；同时可以结合 `jakarta.validation.Valid` 或 Spring 的 `@Validated` 触发标准 Bean Validation。([Spring 企业文档](https://docs.enterprise.spring.io/spring-framework/reference/6.0/web/webmvc/mvc-controller/ann-methods/requestbody.html?utm_source=chatgpt.com))

下面示例定义用户创建请求 DTO。

文件位置：`src/main/java/io/github/atengk/jackson/dto/UserCreateRequest.java`

```java
package io.github.atengk.jackson.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/**
 * 用户创建请求 DTO
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
public class UserCreateRequest {

    /**
     * 用户名
     */
    @NotBlank(message = "用户名不能为空")
    @Size(max = 30, message = "用户名长度不能超过30个字符")
    private String username;

    /**
     * 昵称
     */
    @NotBlank(message = "昵称不能为空")
    @Size(max = 50, message = "昵称长度不能超过50个字符")
    private String nickname;

    /**
     * 手机号
     */
    @NotBlank(message = "手机号不能为空")
    private String mobilePhone;

    /**
     * 出生日期
     */
    @NotNull(message = "出生日期不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Asia/Shanghai")
    private LocalDate birthday;
}
```

下面 Controller 通过 `@Valid @RequestBody` 接收 JSON 请求体。请求进入接口时，Jackson 先把 JSON 转成 `UserCreateRequest`，然后 Bean Validation 对 DTO 字段执行校验。

文件位置：`src/main/java/io/github/atengk/jackson/controller/UserController.java`

```java
package io.github.atengk.jackson.controller;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.DesensitizedUtil;
import io.github.atengk.jackson.common.ApiResult;
import io.github.atengk.jackson.dto.UserCreateRequest;
import io.github.atengk.jackson.vo.UserInfoResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 用户接口
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@RestController
@RequestMapping("/api/jackson/users")
public class UserController {

    /**
     * 创建用户
     *
     * @param request 用户创建请求
     * @return 用户信息
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<UserInfoResponse> createUser(@Valid @RequestBody UserCreateRequest request) {
        long userId = IdUtil.getSnowflakeNextId();

        UserInfoResponse response = new UserInfoResponse();
        response.setUserId(userId);
        response.setUsername(request.getUsername());
        response.setNickname(request.getNickname());
        response.setMobilePhone(DesensitizedUtil.mobilePhone(request.getMobilePhone()));
        response.setCreateTime(LocalDateTime.now());

        log.info("创建用户成功，用户ID：{}，用户名：{}", userId, request.getUsername());
        return ApiResult.success(response);
    }

    /**
     * 查询用户详情
     *
     * @param userId 用户ID
     * @return 用户信息
     */
    @GetMapping(value = "/{userId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<UserInfoResponse> getUser(@PathVariable Long userId) {
        UserInfoResponse response = new UserInfoResponse();
        response.setUserId(userId);
        response.setUsername("ateng");
        response.setNickname("阿腾");
        response.setMobilePhone("138****0000");
        response.setCreateTime(LocalDateTime.now());

        log.info("查询用户详情成功，用户ID：{}", userId);
        return ApiResult.success(response);
    }
}
```

请求示例：

```bash
curl -X POST 'http://localhost:8080/api/jackson/users' \
  -H 'Content-Type: application/json' \
  -d '{
    "username": "ateng",
    "nickname": "阿腾",
    "mobilePhone": "13800000000",
    "birthday": "1998-05-09"
  }'
```

请求体中的 `"birthday": "1998-05-09"` 会由 Jackson 反序列化为 `LocalDate`。如果日期格式不符合配置，例如传入 `"1998/05/09"`，通常会触发 JSON 解析异常或类型转换异常。

### Controller 响应体序列化

Controller 响应体序列化是指 Controller 返回 Java 对象后，Spring MVC 使用 Jackson 将对象转换为 JSON 响应体。`MappingJackson2HttpMessageConverter` 的官方 API 文档说明，该转换器使用 Jackson 2.x 的 `ObjectMapper` 读写 JSON，默认支持 `application/json` 和 `application/*+json`。([Home](https://docs.spring.io/spring-framework/docs/6.2.14/javadoc-api/org/springframework/http/converter/json/MappingJackson2HttpMessageConverter.html?utm_source=chatgpt.com))

下面定义用户响应 VO。

文件位置：`src/main/java/io/github/atengk/jackson/vo/UserInfoResponse.java`

```java
package io.github.atengk.jackson.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户信息响应 VO
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
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
     * 昵称
     */
    private String nickname;

    /**
     * 脱敏手机号
     */
    private String mobilePhone;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime createTime;
}
```

响应结果示例：

```json
{
  "code": 0,
  "message": "操作成功",
  "data": {
    "userId": 10001,
    "username": "ateng",
    "nickname": "阿腾",
    "mobilePhone": "138****0000",
    "createTime": "2026-05-09 10:30:00"
  },
  "success": true
}
```

响应体序列化通常不需要开发者手动调用 `ObjectMapper`。只要返回对象不是 `String`、`byte[]`、文件流等特殊类型，Spring MVC 会根据响应内容类型和消息转换器自动完成 JSON 输出。

### 参数校验与 JSON 解析错误处理

参数校验失败和 JSON 解析失败是接口开发中最常见的两类错误。Spring Framework 文档说明，`@RequestBody` 结合 `@Valid` 时会触发 Bean Validation，默认情况下校验错误会导致 `MethodArgumentNotValidException`，并转换成 400 响应；`HttpMessageNotReadableException` 则是 `HttpMessageConverter` 读取请求体失败时抛出的异常。([Spring 企业文档](https://docs.enterprise.spring.io/spring-framework/reference/6.0/web/webmvc/mvc-controller/ann-methods/requestbody.html?utm_source=chatgpt.com))

为了让接口错误响应保持一致，建议使用 `@RestControllerAdvice` 统一处理异常。`@RestControllerAdvice` 官方文档说明，它组合了 `@ControllerAdvice` 和 `@ResponseBody`，其中 `@ExceptionHandler` 方法默认具有响应体语义。([Spring 企业文档](https://docs.enterprise.spring.io/spring-framework/docs/6.0.25/javadoc-api/org/springframework/web/bind/annotation/RestControllerAdvice.html?utm_source=chatgpt.com))

下面定义统一响应对象。

文件位置：`src/main/java/io/github/atengk/jackson/common/ApiResult.java`

```java
package io.github.atengk.jackson.common;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 接口统一响应结构
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
public class ApiResult<T> {

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
     * 是否成功
     */
    private Boolean success;

    /**
     * 响应时间
     */
    private LocalDateTime timestamp;

    /**
     * 成功响应
     *
     * @param data 响应数据
     * @return 统一响应
     */
    public static <T> ApiResult<T> success(T data) {
        ApiResult<T> result = new ApiResult<>();
        result.setCode(0);
        result.setMessage("操作成功");
        result.setData(data);
        result.setSuccess(true);
        result.setTimestamp(LocalDateTime.now());
        return result;
    }

    /**
     * 失败响应
     *
     * @param code 状态码
     * @param message 响应消息
     * @return 统一响应
     */
    public static <T> ApiResult<T> fail(Integer code, String message) {
        ApiResult<T> result = new ApiResult<>();
        result.setCode(code);
        result.setMessage(message);
        result.setData(null);
        result.setSuccess(false);
        result.setTimestamp(LocalDateTime.now());
        return result;
    }
}
```

下面定义全局异常处理器。

文件位置：`src/main/java/io/github/atengk/jackson/handler/GlobalExceptionHandler.java`

```java
package io.github.atengk.jackson.handler;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.jackson.common.ApiResult;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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
     * @param ex 参数校验异常
     * @return 错误响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResult<Void>> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        var messages = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::buildFieldErrorMessage)
                .toList();

        String message = CollUtil.isEmpty(messages) ? "请求参数校验失败" : CollUtil.join(messages, "；");

        log.warn("请求参数校验失败：{}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResult.fail(400, message));
    }

    /**
     * 处理 JSON 解析异常
     *
     * @param ex JSON 解析异常
     * @return 错误响应
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResult<Void>> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        log.warn("JSON 请求体解析失败：{}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResult.fail(400, "JSON 请求体格式错误，请检查字段类型、日期格式和 JSON 语法"));
    }

    /**
     * 处理普通参数校验异常
     *
     * @param ex 约束校验异常
     * @return 错误响应
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResult<Void>> handleConstraintViolationException(ConstraintViolationException ex) {
        log.warn("请求参数约束校验失败：{}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResult.fail(400, ex.getMessage()));
    }

    /**
     * 处理非法参数异常
     *
     * @param ex 非法参数异常
     * @return 错误响应
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResult<Void>> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("非法参数异常：{}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResult.fail(400, ex.getMessage()));
    }

    /**
     * 处理系统异常
     *
     * @param ex 系统异常
     * @return 错误响应
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResult<Void>> handleException(Exception ex) {
        log.error("系统异常", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResult.fail(500, "系统异常，请稍后重试"));
    }

    /**
     * 构建字段错误消息
     *
     * @param fieldError 字段错误
     * @return 错误消息
     */
    private String buildFieldErrorMessage(FieldError fieldError) {
        String message = StrUtil.blankToDefault(fieldError.getDefaultMessage(), "参数不合法");
        return StrUtil.format("{}：{}", fieldError.getField(), message);
    }
}
```

测试参数校验失败：

```bash
curl -X POST 'http://localhost:8080/api/jackson/users' \
  -H 'Content-Type: application/json' \
  -d '{
    "username": "",
    "nickname": "",
    "mobilePhone": "",
    "birthday": null
  }'
```

可能返回：

```json
{
  "code": 400,
  "message": "username：用户名不能为空；nickname：昵称不能为空；mobilePhone：手机号不能为空；birthday：出生日期不能为空",
  "data": null,
  "success": false,
  "timestamp": "2026-05-09 10:30:00"
}
```

测试 JSON 解析失败：

```bash
curl -X POST 'http://localhost:8080/api/jackson/users' \
  -H 'Content-Type: application/json' \
  -d '{
    "username": "ateng",
    "nickname": "阿腾",
    "mobilePhone": "13800000000",
    "birthday": "1998/05/09"
  }'
```

可能返回：

```json
{
  "code": 400,
  "message": "JSON 请求体格式错误，请检查字段类型、日期格式和 JSON 语法",
  "data": null,
  "success": false,
  "timestamp": "2026-05-09 10:30:00"
}
```

### 统一响应结构中的 JSON 输出

统一响应结构可以让所有接口保持稳定的 JSON 外层格式，便于前端统一处理成功、失败、消息提示和业务数据。常见结构包括 `code`、`message`、`data`、`success`、`timestamp` 等字段。

推荐响应结构如下：

```json
{
  "code": 0,
  "message": "操作成功",
  "data": {},
  "success": true,
  "timestamp": "2026-05-09 10:30:00"
}
```

统一响应结构中的 JSON 输出同样由 Jackson 完成。也就是说，`ApiResult<UserInfoResponse>` 会先作为普通 Java 对象返回给 Spring MVC，然后由 `MappingJackson2HttpMessageConverter` 调用 `ObjectMapper` 输出为 JSON。Spring Framework 文档说明，`MappingJackson2HttpMessageConverter` 可以读写 typed bean，也可以读写未类型化的 `HashMap` 实例。([Home](https://docs.spring.io/spring-framework/docs/6.2.14/javadoc-api/org/springframework/http/converter/json/MappingJackson2HttpMessageConverter.html?utm_source=chatgpt.com))

实际项目中建议遵循以下规则：

| 规则                   | 说明                                              |
| ---------------------- | ------------------------------------------------- |
| 外层响应字段保持稳定   | `code`、`message`、`data`、`success` 不要频繁变更 |
| 业务数据放入 `data`    | 不要把业务字段直接平铺到外层结构                  |
| 错误响应不暴露异常堆栈 | 日志记录详细异常，接口只返回可读错误消息          |
| 日期格式统一           | 外层 `timestamp` 和业务日期字段使用一致格式       |
| null 输出策略统一      | 根据项目规范决定是否输出 null 字段                |

如果项目中所有接口都使用统一响应结构，Controller 返回值建议统一为 `ApiResult<T>`。对于文件下载、图片验证码、SSE、WebSocket、导出流等特殊接口，可以绕过统一 JSON 响应结构，避免影响非 JSON 响应。



## 自定义序列化与反序列化

自定义序列化与反序列化用于处理 Jackson 默认行为无法满足业务规范的场景，例如 Long 类型转字符串、枚举输出 code 和 desc、字段脱敏、特殊日期格式、第三方接口字段兼容等。一般情况下，优先使用 Jackson 注解和全局配置；只有当规则具备明确复用价值时，才建议编写自定义 `JsonSerializer`、`JsonDeserializer` 或 Jackson 模块。

### JsonSerializer 实现

`JsonSerializer` 用于控制 Java 对象如何输出为 JSON。常见应用场景包括 Long 转字符串、枚举输出对象、金额格式化、字段脱敏、状态翻译等。

下面示例实现一个 Long 类型转字符串的序列化器，用于避免前端 JavaScript 处理大整数时出现精度丢失。

文件位置：`src/main/java/io/github/atengk/jackson/serializer/LongToStringJsonSerializer.java`

下面的代码用于将 Java 中的 `Long` 类型序列化为 JSON 字符串，适合用户 ID、订单 ID、雪花 ID 等大整数场景。

```java
package io.github.atengk.jackson.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

/**
 * Long 转字符串序列化器
 *
 * @author Ateng
 * @since 2026-05-09
 */
public class LongToStringJsonSerializer extends JsonSerializer<Long> {

    /**
     * 将 Long 值输出为字符串
     *
     * @param value Long 值
     * @param gen JSON 生成器
     * @param serializers 序列化上下文
     * @throws IOException IO 异常
     */
    @Override
    public void serialize(Long value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value == null) {
            gen.writeNull();
            return;
        }
        gen.writeString(String.valueOf(value));
    }
}
```

在字段上使用该序列化器。

文件位置：`src/main/java/io/github/atengk/jackson/vo/OrderInfoVO.java`

```java
package io.github.atengk.jackson.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.github.atengk.jackson.serializer.LongToStringJsonSerializer;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单信息 VO
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
public class OrderInfoVO {

    /**
     * 订单ID，序列化为字符串
     */
    @JsonSerialize(using = LongToStringJsonSerializer.class)
    private Long orderId;

    /**
     * 用户ID，序列化为字符串
     */
    @JsonSerialize(using = LongToStringJsonSerializer.class)
    private Long userId;

    /**
     * 订单金额
     */
    private BigDecimal amount;
}
```

输出结果示例：

```json
{
  "orderId": "1987654321098765432",
  "userId": "1987654321098765001",
  "amount": 99.90
}
```

字段级 `@JsonSerialize` 适合局部处理。如果整个项目都要求 Long 类型统一输出为字符串，建议通过 Jackson 模块全局注册，避免在每个字段上重复添加注解。

### JsonDeserializer 实现

`JsonDeserializer` 用于控制 JSON 如何解析为 Java 对象。常见应用场景包括枚举 code 转枚举对象、特殊日期字符串解析、字符串清洗、第三方接口兼容多种字段格式等。

下面示例实现用户状态枚举的反序列化，支持前端传入数字、字符串数字，或者对象结构中的 `code` 字段。

文件位置：`src/main/java/io/github/atengk/jackson/enums/UserStatusEnum.java`

```java
package io.github.atengk.jackson.enums;

import cn.hutool.core.util.ObjectUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 用户状态枚举
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Getter
@AllArgsConstructor
public enum UserStatusEnum {

    /**
     * 启用
     */
    ENABLE(1, "启用"),

    /**
     * 禁用
     */
    DISABLE(0, "禁用");

    /**
     * 状态编码
     */
    private final Integer code;

    /**
     * 状态描述
     */
    private final String desc;

    /**
     * 根据状态编码获取枚举
     *
     * @param code 状态编码
     * @return 用户状态枚举
     */
    public static UserStatusEnum of(Integer code) {
        for (UserStatusEnum statusEnum : values()) {
            if (ObjectUtil.equals(statusEnum.getCode(), code)) {
                return statusEnum;
            }
        }
        throw new IllegalArgumentException("用户状态编码不合法：" + code);
    }
}
```

文件位置：`src/main/java/io/github/atengk/jackson/deserializer/UserStatusJsonDeserializer.java`

下面的代码用于把 JSON 中的状态编码解析为 `UserStatusEnum`，兼容 `1`、`"1"` 和 `{ "code": 1 }` 三种输入格式。

```java
package io.github.atengk.jackson.deserializer;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import io.github.atengk.jackson.enums.UserStatusEnum;

import java.io.IOException;

/**
 * 用户状态枚举反序列化器
 *
 * @author Ateng
 * @since 2026-05-09
 */
public class UserStatusJsonDeserializer extends JsonDeserializer<UserStatusEnum> {

    /**
     * 将 JSON 状态值转换为用户状态枚举
     *
     * @param parser JSON 解析器
     * @param context 反序列化上下文
     * @return 用户状态枚举
     * @throws IOException IO 异常
     */
    @Override
    public UserStatusEnum deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        JsonNode node = parser.getCodec().readTree(parser);
        Integer code = parseCode(parser, node);
        return UserStatusEnum.of(code);
    }

    /**
     * 解析状态编码
     *
     * @param parser JSON 解析器
     * @param node JSON 节点
     * @return 状态编码
     * @throws JsonMappingException JSON 映射异常
     */
    private Integer parseCode(JsonParser parser, JsonNode node) throws JsonMappingException {
        if (node == null || node.isNull()) {
            return null;
        }

        if (node.isInt()) {
            return node.asInt();
        }

        if (node.isTextual()) {
            String text = node.asText();
            if (StrUtil.isBlank(text)) {
                return null;
            }
            try {
                return Integer.valueOf(text);
            } catch (NumberFormatException e) {
                throw JsonMappingException.from(parser, "用户状态编码不是合法整数：" + text);
            }
        }

        if (node.isObject() && node.has("code")) {
            return node.get("code").asInt();
        }

        throw JsonMappingException.from(parser, "用户状态格式不合法，支持数字、字符串数字或包含 code 的对象");
    }
}
```

字段级使用方式如下。

文件位置：`src/main/java/io/github/atengk/jackson/dto/UserStatusUpdateRequest.java`

```java
package io.github.atengk.jackson.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.github.atengk.jackson.deserializer.UserStatusJsonDeserializer;
import io.github.atengk.jackson.enums.UserStatusEnum;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 用户状态更新请求 DTO
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
public class UserStatusUpdateRequest {

    /**
     * 用户ID
     */
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /**
     * 用户状态
     */
    @NotNull(message = "用户状态不能为空")
    @JsonDeserialize(using = UserStatusJsonDeserializer.class)
    private UserStatusEnum status;
}
```

支持的请求示例如下：

```json
{
  "userId": 10001,
  "status": 1
}
{
  "userId": 10001,
  "status": "1"
}
{
  "userId": 10001,
  "status": {
    "code": 1
  }
}
```

### 自定义注解绑定

自定义注解绑定适合处理字段级通用规则，例如手机号脱敏、邮箱脱敏、身份证脱敏、银行卡脱敏等。相比直接在字段上写 `@JsonSerialize(using = XxxSerializer.class)`，自定义注解可以表达更明确的业务语义。

下面示例实现一个 `@Desensitize` 注解，通过 `@JacksonAnnotationsInside` 和 `@JsonSerialize` 绑定自定义序列化器。

文件位置：`src/main/java/io/github/atengk/jackson/enums/DesensitizeTypeEnum.java`

```java
package io.github.atengk.jackson.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 脱敏类型枚举
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Getter
@AllArgsConstructor
public enum DesensitizeTypeEnum {

    /**
     * 中文姓名
     */
    CHINESE_NAME,

    /**
     * 手机号
     */
    MOBILE_PHONE,

    /**
     * 邮箱
     */
    EMAIL,

    /**
     * 身份证号
     */
    ID_CARD,

    /**
     * 银行卡号
     */
    BANK_CARD
}
```

文件位置：`src/main/java/io/github/atengk/jackson/annotation/Desensitize.java`

下面的代码定义脱敏注解，字段标注后会自动启用脱敏序列化器。

```java
package io.github.atengk.jackson.annotation;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.github.atengk.jackson.enums.DesensitizeTypeEnum;
import io.github.atengk.jackson.serializer.DesensitizeJsonSerializer;

import java.lang.annotation.*;

/**
 * 字段脱敏注解
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@JacksonAnnotationsInside
@JsonSerialize(using = DesensitizeJsonSerializer.class)
public @interface Desensitize {

    /**
     * 脱敏类型
     *
     * @return 脱敏类型
     */
    DesensitizeTypeEnum type();
}
```

文件位置：`src/main/java/io/github/atengk/jackson/serializer/DesensitizeJsonSerializer.java`

下面的代码根据字段上的 `@Desensitize` 注解类型，对字符串字段执行不同脱敏规则。

```java
package io.github.atengk.jackson.serializer;

import cn.hutool.core.util.DesensitizedUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import io.github.atengk.jackson.annotation.Desensitize;
import io.github.atengk.jackson.enums.DesensitizeTypeEnum;

import java.io.IOException;

/**
 * 字段脱敏序列化器
 *
 * @author Ateng
 * @since 2026-05-09
 */
public class DesensitizeJsonSerializer extends JsonSerializer<String> implements ContextualSerializer {

    private final DesensitizeTypeEnum type;

    public DesensitizeJsonSerializer() {
        this.type = null;
    }

    public DesensitizeJsonSerializer(DesensitizeTypeEnum type) {
        this.type = type;
    }

    /**
     * 序列化脱敏后的字符串
     *
     * @param value 原始字符串
     * @param gen JSON 生成器
     * @param serializers 序列化上下文
     * @throws IOException IO 异常
     */
    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (StrUtil.isBlank(value)) {
            gen.writeString(value);
            return;
        }

        gen.writeString(desensitize(value));
    }

    /**
     * 根据字段注解创建上下文序列化器
     *
     * @param provider 序列化提供者
     * @param property Bean 字段属性
     * @return 当前字段对应的序列化器
     * @throws JsonMappingException JSON 映射异常
     */
    @Override
    public JsonSerializer<?> createContextual(SerializerProvider provider, BeanProperty property) throws JsonMappingException {
        if (property == null) {
            return this;
        }

        Desensitize annotation = property.getAnnotation(Desensitize.class);
        if (annotation == null) {
            annotation = property.getContextAnnotation(Desensitize.class);
        }

        if (annotation == null) {
            return this;
        }

        return new DesensitizeJsonSerializer(annotation.type());
    }

    /**
     * 根据脱敏类型执行脱敏
     *
     * @param value 原始值
     * @return 脱敏后的值
     */
    private String desensitize(String value) {
        if (type == null) {
            return value;
        }

        return switch (type) {
            case CHINESE_NAME -> DesensitizedUtil.chineseName(value);
            case MOBILE_PHONE -> DesensitizedUtil.mobilePhone(value);
            case EMAIL -> DesensitizedUtil.email(value);
            case ID_CARD -> DesensitizedUtil.idCardNum(value, 1, 2);
            case BANK_CARD -> DesensitizedUtil.bankCard(value);
        };
    }
}
```

使用示例：

文件位置：`src/main/java/io/github/atengk/jackson/vo/UserPrivacyVO.java`

```java
package io.github.atengk.jackson.vo;

import io.github.atengk.jackson.annotation.Desensitize;
import io.github.atengk.jackson.enums.DesensitizeTypeEnum;
import lombok.Data;

/**
 * 用户隐私信息 VO
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
public class UserPrivacyVO {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 姓名
     */
    @Desensitize(type = DesensitizeTypeEnum.CHINESE_NAME)
    private String realName;

    /**
     * 手机号
     */
    @Desensitize(type = DesensitizeTypeEnum.MOBILE_PHONE)
    private String mobilePhone;

    /**
     * 邮箱
     */
    @Desensitize(type = DesensitizeTypeEnum.EMAIL)
    private String email;

    /**
     * 身份证号
     */
    @Desensitize(type = DesensitizeTypeEnum.ID_CARD)
    private String idCard;
}
```

输出结果示例：

```json
{
  "userId": 10001,
  "realName": "张*",
  "mobilePhone": "138****0000",
  "email": "a****@example.com",
  "idCard": "1****************2"
}
```

### 模块注册与生效方式

如果自定义规则需要全局生效，推荐使用 `SimpleModule` 注册序列化器和反序列化器。Spring Boot 会自动将容器中的 Jackson `Module` Bean 注册到自动配置的 `ObjectMapper` 中，因此这种方式适合项目级统一规范。

下面示例统一注册 Long 转字符串、用户状态枚举序列化和用户状态枚举反序列化。

文件位置：`src/main/java/io/github/atengk/jackson/serializer/UserStatusJsonSerializer.java`

```java
package io.github.atengk.jackson.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import io.github.atengk.jackson.enums.UserStatusEnum;

import java.io.IOException;

/**
 * 用户状态枚举序列化器
 *
 * @author Ateng
 * @since 2026-05-09
 */
public class UserStatusJsonSerializer extends JsonSerializer<UserStatusEnum> {

    /**
     * 将用户状态枚举输出为 code 和 desc 对象
     *
     * @param value 用户状态枚举
     * @param gen JSON 生成器
     * @param serializers 序列化上下文
     * @throws IOException IO 异常
     */
    @Override
    public void serialize(UserStatusEnum value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value == null) {
            gen.writeNull();
            return;
        }

        gen.writeStartObject();
        gen.writeNumberField("code", value.getCode());
        gen.writeStringField("desc", value.getDesc());
        gen.writeEndObject();
    }
}
```

文件位置：`src/main/java/io/github/atengk/jackson/config/JacksonModuleConfig.java`

下面的配置类用于把自定义序列化器和反序列化器注册为全局 Jackson 模块。

```java
package io.github.atengk.jackson.config;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.github.atengk.jackson.deserializer.UserStatusJsonDeserializer;
import io.github.atengk.jackson.enums.UserStatusEnum;
import io.github.atengk.jackson.serializer.LongToStringJsonSerializer;
import io.github.atengk.jackson.serializer.UserStatusJsonSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jackson 自定义模块配置
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Configuration
public class JacksonModuleConfig {

    /**
     * 注册项目自定义 Jackson 模块
     *
     * @return Jackson 模块
     */
    @Bean
    public Module projectJacksonModule() {
        SimpleModule module = new SimpleModule("projectJacksonModule");

        // Long 类型统一序列化为字符串，避免前端大整数精度丢失
        module.addSerializer(Long.class, new LongToStringJsonSerializer());
        module.addSerializer(Long.TYPE, new LongToStringJsonSerializer());

        // 用户状态枚举统一输出为 {code, desc}
        module.addSerializer(UserStatusEnum.class, new UserStatusJsonSerializer());

        // 用户状态枚举支持 code 反序列化
        module.addDeserializer(UserStatusEnum.class, new UserStatusJsonDeserializer());

        return module;
    }
}
```

模块注册后，凡是由 Spring Boot 自动配置的 `ObjectMapper`、Spring MVC JSON 消息转换器使用的 `ObjectMapper`，都会应用这些规则。需要注意，如果项目中手动 `new ObjectMapper()`，这些模块不会自动生效。

建议优先级如下：

| 场景                   | 推荐方式                                                     |
| ---------------------- | ------------------------------------------------------------ |
| 单个字段特殊处理       | 字段上使用 `@JsonSerialize`、`@JsonDeserialize`、`@JsonFormat` |
| 一类字段通用处理       | 自定义注解绑定序列化器                                       |
| 全项目统一处理         | 注册 Jackson `Module` Bean                                   |
| 简单开关配置           | 使用 `application.yml` 中的 `spring.jackson.*`               |
| 复杂 ObjectMapper 定制 | 使用 `Jackson2ObjectMapperBuilderCustomizer`                 |

## 常见场景

本章整理 SpringBoot3 + Jackson 开发中最常见的 JSON 处理问题，包括 `LocalDateTime` 格式、Long 精度丢失、null 字段输出、枚举输出和字段脱敏。这些问题通常不建议分散在业务代码中临时处理，而应该形成统一配置或统一注解规范。

### LocalDateTime 格式处理

`LocalDateTime` 是后端接口中非常常见的时间类型。默认情况下，如果项目没有正确注册 Java 8 时间模块或没有统一格式规范，可能会出现数组格式、时间戳格式、ISO 格式和自定义字符串格式混用的问题。

推荐做法是：项目全局统一 `LocalDateTime` 输出格式，个别字段有特殊要求时再使用 `@JsonFormat` 单独覆盖。

文件位置：`src/main/java/io/github/atengk/jackson/config/JacksonDateTimeConfig.java`

下面的配置类用于统一 `LocalDate` 和 `LocalDateTime` 的 JSON 输入输出格式。

```java
package io.github.atengk.jackson.config;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Jackson 日期时间格式配置
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Configuration
public class JacksonDateTimeConfig {

    private static final String DATE_PATTERN = "yyyy-MM-dd";

    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    /**
     * 注册 Java 8 日期时间模块
     *
     * @return Jackson 模块
     */
    @Bean
    public Module javaTimeModule() {
        JavaTimeModule module = new JavaTimeModule();

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(DATE_PATTERN);
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN);

        module.addSerializer(LocalDate.class, new LocalDateSerializer(dateFormatter));
        module.addDeserializer(LocalDate.class, new LocalDateDeserializer(dateFormatter));

        module.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(dateTimeFormatter));
        module.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(dateTimeFormatter));

        return module;
    }
}
```

示例 DTO：

文件位置：`src/main/java/io/github/atengk/jackson/vo/DateTimeSceneVO.java`

```java
package io.github.atengk.jackson.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 日期时间场景 VO
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
public class DateTimeSceneVO {

    /**
     * 业务日期
     */
    private LocalDate businessDate;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
```

期望输出：

```json
{
  "businessDate": "2026-05-09",
  "createTime": "2026-05-09 10:30:00"
}
```

如果某个字段需要特殊格式，可以在字段上使用 `@JsonFormat` 单独覆盖：

```java
@JsonFormat(pattern = "yyyy/MM/dd HH:mm", timezone = "Asia/Shanghai")
private LocalDateTime publishTime;
```

### Long 类型精度丢失处理

Java 的 `Long` 能表示 64 位整数，而 JavaScript 的 `Number` 对大整数存在安全精度限制。后端如果直接返回雪花 ID、订单 ID、流水号等 Long 类型大整数，前端可能出现尾数被截断或四舍五入的问题。

推荐做法是：后端接口中 ID 类 Long 字段统一以字符串输出。这样不会影响后端存储和计算，也能保证前端展示、传参和路由跳转时不丢失精度。

局部处理方式：

```java
@JsonSerialize(using = LongToStringJsonSerializer.class)
private Long orderId;
```

全局处理方式：

```java
@Bean
public Module projectJacksonModule() {
    SimpleModule module = new SimpleModule("projectJacksonModule");
    module.addSerializer(Long.class, new LongToStringJsonSerializer());
    module.addSerializer(Long.TYPE, new LongToStringJsonSerializer());
    return module;
}
```

示例输出：

```json
{
  "orderId": "1987654321098765432",
  "userId": "1987654321098765001"
}
```

需要注意，Long 转字符串主要用于 JSON 输出。数据库字段仍然可以使用 `BIGINT`，Java 实体类仍然可以使用 `Long`。不要为了前端精度问题把数据库主键或 Java 实体 ID 全部改成字符串，除非项目从设计上就采用字符串 ID。

### null 字段输出控制

null 字段输出控制用于决定响应 JSON 中是否包含值为 `null` 的字段。不同项目规范可能不同：有些项目希望字段稳定，即使为 null 也输出；有些项目希望响应更简洁，不输出 null 字段。

常见控制方式有三种。

第一种是全局配置：

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  jackson:
    # null 字段不参与 JSON 序列化
    default-property-inclusion: non_null
```

第二种是类级别配置：

```java
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserVO {
}
```

第三种是字段级别配置：

```java
@JsonInclude(JsonInclude.Include.NON_EMPTY)
private List<String> tags;
```

建议优先制定项目统一规范。如果项目接口契约要求字段稳定，建议不要全局排除 null，而是在具体字段或具体 VO 上控制。如果项目更关注响应简洁，可以全局设置 `non_null`，但要注意前端不能依赖 null 字段判断业务状态。

示例 VO：

文件位置：`src/main/java/io/github/atengk/jackson/vo/NullFieldSceneVO.java`

```java
package io.github.atengk.jackson.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

/**
 * 空字段输出场景 VO
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NullFieldSceneVO {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 昵称，null 时不输出
     */
    private String nickname;

    /**
     * 标签，空集合或 null 时不输出
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<String> tags;
}
```

当 `nickname = null` 且 `tags = []` 时，输出结果如下：

```json
{
  "userId": 10001
}
```

### 枚举 code 与 desc 输出

枚举输出是接口设计中很常见的问题。后端业务代码通常使用枚举表达状态，但前端一般需要状态编码和状态描述。如果直接输出枚举名称，例如 `ENABLE`、`DISABLE`，前端还需要额外维护一份映射关系，不利于接口自描述。

推荐方式一：VO 中显式输出 `statusCode` 和 `statusDesc`。

文件位置：`src/main/java/io/github/atengk/jackson/vo/UserStatusSceneVO.java`

```java
package io.github.atengk.jackson.vo;

import io.github.atengk.jackson.enums.UserStatusEnum;
import lombok.Data;

/**
 * 用户状态场景 VO
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
public class UserStatusSceneVO {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 状态编码
     */
    private Integer statusCode;

    /**
     * 状态描述
     */
    private String statusDesc;

    /**
     * 根据枚举填充状态字段
     *
     * @param statusEnum 用户状态枚举
     */
    public void fillStatus(UserStatusEnum statusEnum) {
        if (statusEnum == null) {
            return;
        }
        this.statusCode = statusEnum.getCode();
        this.statusDesc = statusEnum.getDesc();
    }
}
```

输出结果：

```json
{
  "userId": 10001,
  "statusCode": 1,
  "statusDesc": "启用"
}
```

推荐方式二：通过自定义 `JsonSerializer` 将枚举统一输出为对象。

```json
{
  "userId": 10001,
  "status": {
    "code": 1,
    "desc": "启用"
  }
}
```

两种方式的选择建议如下：

| 方式                            | 优点                   | 适用场景                         |
| ------------------------------- | ---------------------- | -------------------------------- |
| 显式 `statusCode`、`statusDesc` | 字段清晰，接口契约稳定 | 对外接口、前端表格、开放平台     |
| 枚举序列化为对象                | 复用性高，代码少       | 内部系统、统一枚举规范明确的项目 |
| 直接输出枚举名称                | 实现简单               | 仅限后端内部调试或临时接口       |

如果接口已经对外发布，建议优先使用显式字段方式，避免后续修改枚举序列化规则影响已有接口。

### 字段脱敏处理

字段脱敏用于防止敏感信息直接暴露到接口响应中，常见字段包括手机号、邮箱、身份证号、银行卡号、姓名、地址等。脱敏应该在响应输出层统一处理，避免不同接口脱敏规则不一致。

推荐方式是使用前面定义的 `@Desensitize` 注解。这样 VO 字段上可以明确表达脱敏规则，业务代码只需要赋原始值，JSON 输出时自动脱敏。

文件位置：`src/main/java/io/github/atengk/jackson/vo/UserDesensitizeSceneVO.java`

```java
package io.github.atengk.jackson.vo;

import io.github.atengk.jackson.annotation.Desensitize;
import io.github.atengk.jackson.enums.DesensitizeTypeEnum;
import lombok.Data;

/**
 * 用户脱敏场景 VO
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
public class UserDesensitizeSceneVO {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 真实姓名
     */
    @Desensitize(type = DesensitizeTypeEnum.CHINESE_NAME)
    private String realName;

    /**
     * 手机号
     */
    @Desensitize(type = DesensitizeTypeEnum.MOBILE_PHONE)
    private String mobilePhone;

    /**
     * 邮箱
     */
    @Desensitize(type = DesensitizeTypeEnum.EMAIL)
    private String email;

    /**
     * 身份证号
     */
    @Desensitize(type = DesensitizeTypeEnum.ID_CARD)
    private String idCard;
}
```

输出示例：

```json
{
  "userId": 10001,
  "realName": "张*",
  "mobilePhone": "138****0000",
  "email": "a****@example.com",
  "idCard": "1****************2"
}
```

脱敏处理需要注意以下几点：

| 注意事项               | 说明                                               |
| ---------------------- | -------------------------------------------------- |
| 不要只依赖前端脱敏     | 敏感数据不应该完整返回给浏览器后再由前端处理       |
| 不建议实体类上直接脱敏 | 实体类通常对应数据库完整数据，建议在 VO 层控制输出 |
| 内部接口也要区分权限   | 管理端、审计端、普通用户端可能需要不同脱敏策略     |
| 日志也要脱敏           | 接口返回脱敏不代表日志中可以输出原始敏感字段       |
| 导出场景单独设计       | Excel 导出、审计报表可能需要独立权限和脱敏规则     |

在正式项目中，字段脱敏通常还需要结合权限控制。例如普通用户只能看到脱敏手机号，客服角色可以查看部分明文，管理员查看完整明文时需要记录审计日志。Jackson 注解适合解决“输出格式”问题，权限判断仍然建议放在业务层或响应组装层完成。



## 异常与排查

Jackson 相关异常通常出现在请求体反序列化、响应体序列化、字段类型转换、日期时间解析和未知字段处理阶段。在 SpringBoot3 Web 项目中，这些异常大多会被 Spring MVC 包装成 `HttpMessageNotReadableException`、`HttpMessageConversionException`、`MethodArgumentTypeMismatchException` 等异常，因此排查时不能只看接口返回信息，还需要结合后端日志中的根异常。

### JSON 解析异常

JSON 解析异常通常表示请求体不是合法 JSON，或者 JSON 结构与接口参数不匹配。常见原因包括缺少引号、逗号多写、对象和数组结构传错、请求体为空、`Content-Type` 不正确等。

常见错误请求如下：

```json
{
  "username": "ateng",
  "age": 18,
}
```

上面的 JSON 在 `age` 字段后多了一个逗号，属于非法 JSON。接口接收时通常会触发 `HttpMessageNotReadableException`。

建议在全局异常处理器中统一处理 JSON 解析异常，并返回清晰的错误信息。

文件位置：`src/main/java/io/github/atengk/jackson/handler/GlobalExceptionHandler.java`

下面的代码用于统一处理 JSON 请求体解析失败、参数校验失败和系统异常。

```java
package io.github.atengk.jackson.handler;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.jackson.common.ApiResult;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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
     * 处理 JSON 请求体解析异常
     *
     * @param ex JSON 解析异常
     * @return 统一错误响应
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResult<Void>> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        log.warn("JSON 请求体解析失败：{}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResult.fail(400, "JSON 请求体格式错误，请检查 JSON 语法、字段类型和日期格式"));
    }

    /**
     * 处理请求体参数校验异常
     *
     * @param ex 参数校验异常
     * @return 统一错误响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResult<Void>> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        var messages = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::buildFieldErrorMessage)
                .toList();

        String message = CollUtil.isEmpty(messages) ? "请求参数校验失败" : CollUtil.join(messages, "；");

        log.warn("请求参数校验失败：{}", message);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResult.fail(400, message));
    }

    /**
     * 处理普通参数约束异常
     *
     * @param ex 参数约束异常
     * @return 统一错误响应
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResult<Void>> handleConstraintViolationException(ConstraintViolationException ex) {
        log.warn("请求参数约束校验失败：{}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResult.fail(400, ex.getMessage()));
    }

    /**
     * 处理系统异常
     *
     * @param ex 系统异常
     * @return 统一错误响应
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResult<Void>> handleException(Exception ex) {
        log.error("系统异常", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResult.fail(500, "系统异常，请稍后重试"));
    }

    /**
     * 构建字段校验错误消息
     *
     * @param fieldError 字段错误
     * @return 错误消息
     */
    private String buildFieldErrorMessage(FieldError fieldError) {
        String message = StrUtil.blankToDefault(fieldError.getDefaultMessage(), "参数不合法");
        return StrUtil.format("{}：{}", fieldError.getField(), message);
    }
}
```

排查 JSON 解析异常时，建议按以下顺序检查：

| 检查项     | 说明                                                |
| ---------- | --------------------------------------------------- |
| 请求头     | `Content-Type` 是否为 `application/json`            |
| 请求体语法 | JSON 是否缺少引号、逗号、括号或存在多余逗号         |
| 接口参数   | Controller 是否使用了 `@RequestBody`                |
| 字段结构   | 前端传的是对象还是数组，是否和 DTO 匹配             |
| 后端日志   | 查看 `HttpMessageNotReadableException` 的根异常信息 |

### 类型转换异常

类型转换异常通常表示 JSON 字段值无法转换成 Java 字段类型。常见情况包括字符串传给数字字段、对象传给字符串字段、数组传给对象字段、枚举值不合法等。

错误示例：

```json
{
  "userId": "abc",
  "age": "十八"
}
```

如果 DTO 中 `userId` 是 `Long`，`age` 是 `Integer`，上面的请求会导致类型转换失败。

示例 DTO：

文件位置：`src/main/java/io/github/atengk/jackson/dto/UserTypeRequest.java`

```java
package io.github.atengk.jackson.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 用户类型转换请求 DTO
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
public class UserTypeRequest {

    /**
     * 用户ID
     */
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /**
     * 年龄
     */
    @NotNull(message = "年龄不能为空")
    private Integer age;
}
```

正确请求：

```json
{
  "userId": 10001,
  "age": 18
}
```

错误请求：

```json
{
  "userId": "abc",
  "age": "十八"
}
```

类型转换异常的排查重点如下：

| 问题                     | 处理方式                                        |
| ------------------------ | ----------------------------------------------- |
| 数字字段传入非数字字符串 | 前端按字段类型传值，后端增加校验和错误提示      |
| 枚举字段传入非法 code    | 自定义枚举反序列化器，并返回明确错误信息        |
| 对象和数组结构传反       | 核对 DTO 字段类型和接口文档                     |
| Long 大整数前端传参异常  | 前后端约定 ID 字段使用字符串传输，后端再转 Long |
| 空字符串转数值失败       | 前端不要传空字符串，后端可按需统一清洗          |

如果前端无法保证类型稳定，可以在请求 DTO 中使用字符串接收，然后在 Service 层显式转换并校验。但对于内部系统和规范化接口，更推荐前端按接口契约正确传参，后端通过 Jackson 和 Bean Validation 保持严格约束。

### 日期格式异常

日期格式异常是 Jackson 使用中最常见的问题之一。常见原因是前端传入的日期格式与后端配置不一致，例如后端要求 `yyyy-MM-dd HH:mm:ss`，前端传入 `2026/05/09 10:30:00` 或 ISO 字符串。

示例 DTO：

文件位置：`src/main/java/io/github/atengk/jackson/dto/DateTimeRequest.java`

```java
package io.github.atengk.jackson.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 日期时间请求 DTO
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
public class DateTimeRequest {

    /**
     * 业务日期
     */
    @NotNull(message = "业务日期不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Asia/Shanghai")
    private LocalDate businessDate;

    /**
     * 开始时间
     */
    @NotNull(message = "开始时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime startTime;
}
```

正确请求：

```json
{
  "businessDate": "2026-05-09",
  "startTime": "2026-05-09 10:30:00"
}
```

错误请求：

```json
{
  "businessDate": "2026/05/09",
  "startTime": "2026-05-09T10:30:00"
}
```

日期格式异常的处理建议如下：

| 场景                   | 建议                                                |
| ---------------------- | --------------------------------------------------- |
| 全项目统一日期格式     | 使用 `JavaTimeModule` 和全局 Jackson 配置           |
| 单个字段格式特殊       | 使用 `@JsonFormat` 单独声明                         |
| 第三方接口格式不固定   | 自定义 `JsonDeserializer` 兼容多个格式              |
| 跨时区系统             | 优先使用 `Instant`、`OffsetDateTime` 或明确约定时区 |
| 前端日期组件输出不一致 | 前端提交前统一格式化                                |

不建议让后端无限兼容各种日期格式。对于内部系统，接口文档应明确日期格式；对于外部接口，可以在网关层或适配层做兼容转换，业务 DTO 保持稳定格式。

### 未知字段处理

未知字段是指 JSON 请求体中存在 DTO 没有定义的字段。例如 DTO 只有 `username` 和 `nickname`，但请求体额外传了 `extraField`。

示例请求：

```json
{
  "username": "ateng",
  "nickname": "阿腾",
  "extraField": "unknown"
}
```

是否报错取决于 Jackson 的 `FAIL_ON_UNKNOWN_PROPERTIES` 配置。SpringBoot3 项目中通常建议关闭未知字段失败，让接口具备一定兼容能力。

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  jackson:
    deserialization:
      # JSON 中存在 DTO 未定义字段时不报错
      fail-on-unknown-properties: false
```

如果只想在某个 DTO 上忽略未知字段，可以使用 `@JsonIgnoreProperties(ignoreUnknown = true)`。

文件位置：`src/main/java/io/github/atengk/jackson/dto/ThirdPartyUserRequest.java`

```java
package io.github.atengk.jackson.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * 第三方用户请求 DTO
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ThirdPartyUserRequest {

    /**
     * 第三方用户ID
     */
    private String openId;

    /**
     * 昵称
     */
    private String nickname;
}
```

未知字段处理的选择建议如下：

| 场景          | 建议                                             |
| ------------- | ------------------------------------------------ |
| 对内 REST API | 通常关闭未知字段失败，提高兼容性                 |
| 对外开放接口  | 可关闭未知字段失败，但接口文档必须明确有效字段   |
| 配置文件解析  | 可以开启未知字段失败，避免配置拼错不生效         |
| 安全敏感场景  | 谨慎忽略未知字段，避免调用方误传危险字段         |
| 第三方回调    | 建议忽略未知字段，防止第三方新增字段导致接口失败 |

## 测试与验证

Jackson 配置一旦进入项目全局配置，就会影响所有 Controller 请求和响应。因此，建议对常见 JSON 行为编写测试用例，包括对象转换、日期格式、Long 精度、null 字段、枚举输出、异常响应等。

### 单元测试验证

单元测试适合验证 `ObjectMapper` 的基础行为，例如序列化、反序列化、泛型转换、自定义序列化器和日期格式是否生效。测试时建议注入 Spring 容器中的 `ObjectMapper`，而不是手动创建新的 `ObjectMapper`。

文件位置：`src/test/java/io/github/atengk/jackson/JacksonObjectMapperTest.java`

下面的测试用于验证日期格式、Long 转字符串、null 字段控制和对象反序列化行为。

```java
package io.github.atengk.jackson;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.atengk.jackson.vo.OrderInfoVO;
import lombok.Data;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Jackson ObjectMapper 单元测试
 *
 * @author Ateng
 * @since 2026-05-09
 */
@SpringBootTest
class JacksonObjectMapperTest {

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 验证 Long 类型序列化为字符串
     *
     * @throws Exception JSON 处理异常
     */
    @Test
    void shouldSerializeLongToString() throws Exception {
        OrderInfoVO vo = new OrderInfoVO();
        vo.setOrderId(1987654321098765432L);
        vo.setUserId(1987654321098765001L);
        vo.setAmount(new BigDecimal("99.90"));

        String json = objectMapper.writeValueAsString(vo);

        Assertions.assertTrue(StrUtil.contains(json, "\"orderId\":\"1987654321098765432\""));
        Assertions.assertTrue(StrUtil.contains(json, "\"userId\":\"1987654321098765001\""));
    }

    /**
     * 验证 LocalDateTime 序列化格式
     *
     * @throws Exception JSON 处理异常
     */
    @Test
    void shouldSerializeLocalDateTimeWithPattern() throws Exception {
        DateTimeTestVO vo = new DateTimeTestVO();
        vo.setCreateTime(LocalDateTime.of(2026, 5, 9, 10, 30, 0));

        String json = objectMapper.writeValueAsString(vo);

        Assertions.assertTrue(StrUtil.contains(json, "\"createTime\":\"2026-05-09 10:30:00\""));
    }

    /**
     * 日期时间测试 VO
     *
     * @author Ateng
     * @since 2026-05-09
     */
    @Data
    static class DateTimeTestVO {

        /**
         * 创建时间
         */
        private LocalDateTime createTime;
    }
}
```

单元测试适合快速验证 JSON 规则是否生效，但不能完全替代接口测试。因为 Controller 层还涉及 `HttpMessageConverter`、参数校验、异常处理器和响应结构。

### 接口测试验证

接口测试用于验证 Spring MVC 请求和响应链路是否完整生效，包括 `@RequestBody` 反序列化、`@Valid` 校验、Controller 返回值序列化和全局异常处理。

文件位置：`src/test/java/io/github/atengk/jackson/UserControllerTest.java`

下面的测试使用 `MockMvc` 验证用户创建接口的成功响应、参数校验失败和 JSON 解析失败。

```java
package io.github.atengk.jackson;

import cn.hutool.core.util.StrUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

    /**
     * 验证创建用户成功
     *
     * @throws Exception 请求异常
     */
    @Test
    void shouldCreateUserSuccess() throws Exception {
        String requestBody = """
                {
                  "username": "ateng",
                  "nickname": "阿腾",
                  "mobilePhone": "13800000000",
                  "birthday": "1998-05-09"
                }
                """;

        String responseBody = mockMvc.perform(post("/api/jackson/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Assertions.assertTrue(StrUtil.contains(responseBody, "\"code\":0"));
        Assertions.assertTrue(StrUtil.contains(responseBody, "\"success\":true"));
        Assertions.assertTrue(StrUtil.contains(responseBody, "\"username\":\"ateng\""));
    }

    /**
     * 验证参数校验失败
     *
     * @throws Exception 请求异常
     */
    @Test
    void shouldReturnBadRequestWhenValidationFailed() throws Exception {
        String requestBody = """
                {
                  "username": "",
                  "nickname": "",
                  "mobilePhone": "",
                  "birthday": null
                }
                """;

        String responseBody = mockMvc.perform(post("/api/jackson/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Assertions.assertTrue(StrUtil.contains(responseBody, "\"code\":400"));
        Assertions.assertTrue(StrUtil.contains(responseBody, "\"success\":false"));
    }

    /**
     * 验证 JSON 日期格式错误
     *
     * @throws Exception 请求异常
     */
    @Test
    void shouldReturnBadRequestWhenJsonDateFormatInvalid() throws Exception {
        String requestBody = """
                {
                  "username": "ateng",
                  "nickname": "阿腾",
                  "mobilePhone": "13800000000",
                  "birthday": "1998/05/09"
                }
                """;

        String responseBody = mockMvc.perform(post("/api/jackson/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Assertions.assertTrue(StrUtil.contains(responseBody, "JSON 请求体格式错误"));
    }
}
```

接口测试重点验证的是完整 HTTP 链路，而不是单个序列化器本身。对于全局 Jackson 配置，建议至少覆盖以下场景：

| 场景         | 验证点                             |
| ------------ | ---------------------------------- |
| 正常请求     | JSON 请求体能正确转换为 DTO        |
| 正常响应     | VO 能按预期格式输出                |
| 参数为空     | Bean Validation 能返回统一错误响应 |
| 日期格式错误 | 能返回明确 JSON 解析错误           |
| 类型传错     | 能返回 400，而不是 500             |
| Long ID 输出 | 响应中的大整数 ID 是否为字符串     |
| null 字段    | 是否符合项目输出规范               |

### 边界数据验证

边界数据验证用于确保 Jackson 配置在极端或特殊数据下仍然表现稳定。很多 JSON 问题不会在正常数据下暴露，而是在空值、大整数、超长字符串、特殊字符、嵌套结构和未知字段中暴露。

建议重点验证以下边界数据：

| 边界类型   | 示例                  | 验证目标                        |
| ---------- | --------------------- | ------------------------------- |
| 空请求体   | 空字符串、`{}`        | 是否返回清晰错误                |
| null 字段  | `"nickname": null`    | null 输出和参数校验是否符合规范 |
| 空字符串   | `"username": ""`      | 是否触发 `@NotBlank`            |
| 大整数     | `1987654321098765432` | Long 输出是否丢失精度           |
| 日期边界   | `2026-02-29`          | 非法日期是否被拒绝              |
| 特殊字符   | emoji、换行、引号     | JSON 是否正常转义               |
| 未知字段   | `extraField`          | 是否按项目规范忽略或报错        |
| 枚举非法值 | `status: 99`          | 是否返回明确错误                |
| 空集合     | `[]`                  | 是否按 `NON_EMPTY` 输出         |
| 嵌套对象   | 多层 DTO              | 是否能正确反序列化              |

可以针对边界数据编写参数化测试。

文件位置：`src/test/java/io/github/atengk/jackson/JacksonBoundaryTest.java`

下面的测试用于验证多个非法日期输入是否都会被 Jackson 拒绝。

```java
package io.github.atengk.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.atengk.jackson.dto.DateTimeRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Jackson 边界数据测试
 *
 * @author Ateng
 * @since 2026-05-09
 */
@SpringBootTest
class JacksonBoundaryTest {

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 验证非法日期格式不能反序列化成功
     *
     * @param businessDate 业务日期
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "2026/05/09",
            "2026-02-29",
            "2026-13-01",
            "invalid-date"
    })
    void shouldRejectInvalidDateFormat(String businessDate) {
        String json = """
                {
                  "businessDate": "%s",
                  "startTime": "2026-05-09 10:30:00"
                }
                """.formatted(businessDate);

        Assertions.assertThrows(Exception.class, () -> objectMapper.readValue(json, DateTimeRequest.class));
    }
}
```

边界测试的目标不是覆盖所有数据组合，而是覆盖最容易破坏接口稳定性的输入。对于公共 API、开放平台接口、支付回调、订单接口和用户隐私接口，应当增加更多边界测试。

## 最佳实践

Jackson 最佳实践的核心目标是减少隐式行为、保证接口契约稳定、避免 JSON 规则分散在业务代码中。对于 SpringBoot3 项目，建议把 JSON 配置作为基础设施能力统一管理，而不是让每个 Controller 或 Service 自行处理 JSON。

### 配置集中管理

Jackson 配置应集中放在 `config` 包下，例如 `JacksonConfig`、`JacksonModuleConfig`、`JacksonDateTimeConfig`。不要在多个业务模块中分别声明不同的 `ObjectMapper`，否则会导致同一个字段在不同接口中输出格式不一致。

推荐目录结构如下：

```text
src/main/java/io/github/atengk/jackson
├── annotation
│   └── Desensitize.java
├── common
│   └── ApiResult.java
├── config
│   ├── JacksonConfig.java
│   ├── JacksonDateTimeConfig.java
│   └── JacksonModuleConfig.java
├── deserializer
│   └── UserStatusJsonDeserializer.java
├── dto
│   └── UserCreateRequest.java
├── enums
│   ├── DesensitizeTypeEnum.java
│   └── UserStatusEnum.java
├── handler
│   └── GlobalExceptionHandler.java
├── serializer
│   ├── DesensitizeJsonSerializer.java
│   ├── LongToStringJsonSerializer.java
│   └── UserStatusJsonSerializer.java
└── vo
    └── UserInfoResponse.java
```

集中管理建议如下：

| 配置类型      | 推荐位置                             |
| ------------- | ------------------------------------ |
| 日期时间格式  | `JacksonDateTimeConfig`              |
| Long 转字符串 | `JacksonModuleConfig`                |
| null 字段策略 | `application.yml` 或 `JacksonConfig` |
| 未知字段策略  | `application.yml` 或 `JacksonConfig` |
| 字段脱敏      | `annotation` + `serializer`          |
| 枚举转换      | `serializer` + `deserializer`        |
| 异常响应      | `GlobalExceptionHandler`             |

配置集中管理后，业务代码只需要关注 DTO、VO 和业务逻辑。接口输出规则由基础设施层统一保证。

### DTO 与实体对象隔离

Controller 不建议直接使用数据库实体对象作为请求体或响应体。实体对象通常对应数据库表结构，而接口 DTO/VO 对应外部契约，两者职责不同。如果直接暴露实体对象，会导致数据库字段变化影响接口响应，也容易泄露内部字段。

推荐分层如下：

| 对象类型            | 职责             |
| ------------------- | ---------------- |
| Entity              | 数据库表映射对象 |
| DTO / Request       | 接收前端请求参数 |
| VO / Response       | 返回前端响应数据 |
| Convert / Assembler | 负责对象转换     |
| Service             | 处理业务逻辑     |

示例实体对象：

文件位置：`src/main/java/io/github/atengk/jackson/entity/UserEntity.java`

```java
package io.github.atengk.jackson.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体对象
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
public class UserEntity {

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
     * 密码哈希
     */
    private String passwordHash;

    /**
     * 手机号
     */
    private String mobilePhone;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 删除标记
     */
    private Integer deleted;
}
```

示例响应对象：

文件位置：`src/main/java/io/github/atengk/jackson/vo/UserSafeResponse.java`

```java
package io.github.atengk.jackson.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.github.atengk.jackson.annotation.Desensitize;
import io.github.atengk.jackson.enums.DesensitizeTypeEnum;
import io.github.atengk.jackson.serializer.LongToStringJsonSerializer;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户安全响应对象
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
public class UserSafeResponse {

    /**
     * 用户ID
     */
    @JsonSerialize(using = LongToStringJsonSerializer.class)
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 脱敏手机号
     */
    @Desensitize(type = DesensitizeTypeEnum.MOBILE_PHONE)
    private String mobilePhone;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
```

示例转换器：

文件位置：`src/main/java/io/github/atengk/jackson/convert/UserConvert.java`

```java
package io.github.atengk.jackson.convert;

import io.github.atengk.jackson.entity.UserEntity;
import io.github.atengk.jackson.vo.UserSafeResponse;
import lombok.experimental.UtilityClass;

/**
 * 用户对象转换器
 *
 * @author Ateng
 * @since 2026-05-09
 */
@UtilityClass
public class UserConvert {

    /**
     * 实体对象转安全响应对象
     *
     * @param entity 用户实体
     * @return 用户安全响应对象
     */
    public UserSafeResponse toSafeResponse(UserEntity entity) {
        if (entity == null) {
            return null;
        }

        UserSafeResponse response = new UserSafeResponse();
        response.setUserId(entity.getId());
        response.setUsername(entity.getUsername());
        response.setNickname(entity.getNickname());
        response.setMobilePhone(entity.getMobilePhone());
        response.setCreateTime(entity.getCreateTime());
        return response;
    }
}
```

实体对象中可能存在 `passwordHash`、`deleted`、`createBy`、`updateBy`、内部状态字段等，这些字段不应该直接参与接口输出。即使可以通过 `@JsonIgnore` 隐藏字段，也不建议把实体对象直接作为响应体。

### 日期时间统一规范

日期时间规范应在项目初期明确，否则后期接口会出现多种格式混用。常见混乱包括 `yyyy-MM-dd HH:mm:ss`、ISO-8601、时间戳、数组格式和带时区字符串同时存在。

推荐统一规范如下：

| 类型             | 用途           | 推荐格式              |
| ---------------- | -------------- | --------------------- |
| `LocalDate`      | 日期，不含时间 | `yyyy-MM-dd`          |
| `LocalTime`      | 时间，不含日期 | `HH:mm:ss`            |
| `LocalDateTime`  | 本地日期时间   | `yyyy-MM-dd HH:mm:ss` |
| `Instant`        | 绝对时间点     | ISO-8601 UTC          |
| `OffsetDateTime` | 带偏移量时间   | ISO-8601 带偏移量     |

项目中建议遵循以下原则：

1. 请求和响应使用同一套日期格式。
2. 单一地区业务可以统一使用 `Asia/Shanghai`。
3. 跨时区业务应明确存储时间和展示时间的转换规则。
4. 不要在不同 Controller 中写不同的日期格式。
5. 不要让前端猜测日期格式，接口文档必须明确说明。
6. 对外接口尽量使用标准化格式，并保持版本稳定。

如果已经有历史接口存在多种日期格式，应通过接口版本或兼容反序列化器平滑迁移，不建议直接修改旧接口输出格式。

### 响应字段稳定性控制

响应字段稳定性是接口可维护性的核心。Jackson 配置会直接影响响应字段是否存在、字段名是否变化、日期格式是否变化、枚举输出是否变化和 null 字段是否输出。因此，所有全局配置都应视为接口契约的一部分。

建议遵循以下规则：

| 规则                   | 说明                                       |
| ---------------------- | ------------------------------------------ |
| 字段名不要随意变更     | 前端、第三方系统和自动化测试都依赖字段名   |
| 不要直接删除字段       | 可先标记废弃，保留一段兼容期               |
| null 输出策略要统一    | 不要有的接口输出 null，有的接口不输出 null |
| 枚举输出要稳定         | 不要在字符串、数字、对象之间频繁切换       |
| 日期格式要稳定         | 日期格式变更属于破坏性变更                 |
| Long ID 输出要统一     | ID 字段建议统一字符串化                    |
| 敏感字段默认不输出明文 | 手机号、邮箱、证件号、银行卡号需要脱敏     |

对于已经对外发布的接口，响应字段变更建议通过接口版本控制处理。例如：

```text
/api/v1/users/{userId}
/api/v2/users/{userId}
```

如果只是新增字段，一般兼容性较好；如果是删除字段、修改字段名、修改字段类型、修改枚举格式、修改日期格式，都属于高风险变更，需要评估调用方影响。

在 SpringBoot3 + Jackson 项目中，推荐把响应字段稳定性作为接口评审的一部分。尤其是统一响应结构、DTO/VO 字段、日期格式、Long 精度策略和枚举输出方式，应在项目规范中固定下来，避免随着业务迭代不断漂移。
