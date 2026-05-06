# Spring Boot 全局异常处理

## 功能概述

Spring Boot 全局异常处理用于统一接管接口请求过程中产生的异常，将不同类型的异常转换为统一、稳定、可识别的响应结构。通过集中处理业务异常、参数校验异常、请求参数异常、系统运行异常等问题，可以减少 Controller 层重复的 `try-catch` 代码，提升接口返回的一致性和问题排查效率。

在实际项目中，全局异常处理通常会与统一响应对象、错误码枚举、业务异常类、日志规范和参数校验机制配合使用，形成完整的接口异常处理体系。

### 全局异常处理目标

全局异常处理的主要目标是将系统内部异常转换为对调用方友好的接口响应，同时保留服务端排查问题所需的日志信息。

具体目标如下：

| 目标         | 说明                                                         |
| ------------ | ------------------------------------------------------------ |
| 统一响应格式 | 所有异常都返回统一 JSON 结构，便于前端或调用方解析           |
| 规范错误码   | 使用错误码区分不同失败原因，避免仅依赖错误文本判断           |
| 屏蔽敏感信息 | 避免将异常堆栈、SQL、服务器路径、内部类名等信息直接返回给客户端 |
| 降低重复代码 | 避免在每个 Controller 方法中重复编写异常捕获逻辑             |
| 提升可维护性 | 异常处理逻辑集中管理，便于统一调整返回格式和日志策略         |
| 便于问题定位 | 结合请求路径、请求参数、traceId、异常日志快速定位问题        |

推荐将全局异常处理作为 Web 项目的基础能力建设，而不是在业务功能开发完成后再零散补充。这样可以保证项目从一开始就具备稳定的错误响应规范。

### 适用场景

全局异常处理适用于所有基于 Spring Boot Web 构建的 HTTP 接口项目，例如后台管理系统、前后端分离接口、移动端接口、开放平台接口、微服务接口等。

常见适用场景如下：

| 场景           | 示例                                                     |
| -------------- | -------------------------------------------------------- |
| 业务异常       | 用户不存在、订单状态不允许修改、余额不足、数据已被删除   |
| 参数校验异常   | 必填字段为空、字段长度超限、手机号格式错误、枚举值不合法 |
| 请求参数异常   | 缺少请求参数、参数类型转换失败、请求方法不支持           |
| 请求体异常     | JSON 格式错误、请求体为空、字段类型与 DTO 不匹配         |
| 系统运行异常   | 空指针异常、数组越界、数据库异常、文件读写异常           |
| 第三方服务异常 | 短信服务调用失败、支付服务异常、对象存储上传失败         |
| 未知异常兜底   | 未被业务显式处理的异常统一返回系统错误                   |

对于前后端分离项目，全局异常处理尤其重要。前端通常依赖固定字段判断接口是否成功，例如 `code`、`message`、`data`。如果异常响应格式不统一，前端需要针对不同异常单独适配，会增加接口联调和错误提示的复杂度。

### 异常处理边界

全局异常处理只负责异常响应转换和必要日志记录，不应替代业务判断、权限认证、事务控制或参数修正。边界清晰可以避免全局异常处理类承担过多职责，降低后期维护成本。

建议边界如下：

| 边界类型       | 处理原则                                                     |
| -------------- | ------------------------------------------------------------ |
| 业务异常       | 由业务代码主动抛出，再由全局异常处理统一转换为业务错误响应   |
| 参数校验异常   | 由 Bean Validation 或 Spring MVC 触发，再统一返回参数错误信息 |
| 请求格式异常   | 由 Spring MVC 捕获后统一转换为请求错误响应                   |
| 系统未知异常   | 记录完整异常堆栈，返回通用系统错误提示                       |
| 第三方服务异常 | 可先封装为业务异常或外部服务异常，再交给全局异常处理         |
| 权限认证异常   | 优先由 Spring Security、Sa-Token 等安全框架的异常入口处理    |
| 事务回滚       | 由 Spring 事务机制处理，全局异常处理只负责响应结果           |
| 参数修正       | 不建议在异常处理器中修正参数，应在 DTO、转换器或业务逻辑中完成 |

需要注意的是，全局异常处理不是为了吞掉所有异常。对于系统异常，应记录完整堆栈；对于业务异常，应保留明确错误码；对于参数异常，应尽量返回具体字段和失败原因。

## 基础环境

本节说明 Spring Boot 3 全局异常处理所需的基础环境。后续实现中会使用 `@RestControllerAdvice`、`@ExceptionHandler`、`@Valid`、`@Validated`、`jakarta.validation` 等能力，因此需要先确认项目版本和依赖配置。

### Spring Boot 3 版本说明

本文以 Spring Boot 3.x 为基础进行说明。Spring Boot 3 要求使用 Java 17 或更高版本，并基于 Spring Framework 6。与 Spring Boot 2 相比，Spring Boot 3 的一个重要变化是从 Java EE 迁移到 Jakarta EE，因此参数校验、Servlet 等相关包名需要使用 `jakarta.*`，而不是旧版本中的 `javax.*`。

推荐基础环境如下：

| 组件             | 推荐版本           | 说明                                      |
| ---------------- | ------------------ | ----------------------------------------- |
| JDK              | 17 或以上          | Spring Boot 3 的基础运行要求              |
| Spring Boot      | 3.x                | 本文示例按 Spring Boot 3 风格编写         |
| Spring Framework | 6.x                | Spring Boot 3 底层依赖 Spring Framework 6 |
| Web 技术栈       | Spring MVC         | 适用于传统 Servlet Web 应用               |
| Validation API   | Jakarta Validation | 使用 `jakarta.validation` 包名            |

开发 Spring Boot 3 项目时需要注意以下几点：

1. `@Valid` 应导入 `jakarta.validation.Valid`。
2. 参数校验注解应导入 `jakarta.validation.constraints.*`。
3. 旧项目升级时，需要将 `javax.validation.*` 替换为 `jakarta.validation.*`。
4. 如果项目使用第三方依赖，需要确认依赖版本是否兼容 Spring Boot 3。
5. 全局异常处理类仍然使用 Spring MVC 提供的 `@RestControllerAdvice` 和 `@ExceptionHandler`。

可以通过以下命令查看当前 JDK 版本：

```bash
java -version
```

如果本地 JDK 版本低于 17，需要先升级 JDK，否则 Spring Boot 3 项目可能无法正常编译或启动。

### Web 模块依赖

全局异常处理主要作用于 Web 请求链路，因此项目需要引入 Spring Boot Web 模块。该模块提供 Controller、REST 接口、JSON 序列化、Spring MVC 异常处理机制以及内置 Web 容器等能力。

Maven 项目可以在 `pom.xml` 中添加以下依赖：

```xml
<dependencies>
    <!-- Spring MVC Web 支持：提供 Controller、REST 接口、JSON 序列化和内置 Web 容器 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Hutool 工具类：用于字符串、集合、日期、唯一 ID 等通用处理 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.8.36</version>
    </dependency>

    <!-- Lombok：减少实体类、DTO、VO、异常类中的样板代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

Gradle 项目可以添加以下依赖：

```gradle
dependencies {
    // Spring MVC Web 支持：用于构建 REST 接口和统一异常处理
    implementation 'org.springframework.boot:spring-boot-starter-web'

    // Hutool 工具类：用于常见字符串、集合、日期、ID 等处理
    implementation 'cn.hutool:hutool-all:5.8.36'

    // Lombok：减少实体类、DTO、VO、异常类中的样板代码
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
}
```

引入 Web 模块后，项目即可使用以下全局异常处理相关能力：

| 能力               | 常用类或注解                                                 |
| ------------------ | ------------------------------------------------------------ |
| 声明 REST 接口     | `@RestController`                                            |
| 声明全局异常处理类 | `@RestControllerAdvice`                                      |
| 声明异常处理方法   | `@ExceptionHandler`                                          |
| 获取请求上下文     | `HttpServletRequest`                                         |
| 返回 HTTP 状态码   | `ResponseEntity`、`HttpStatus`                               |
| 处理请求参数异常   | `MethodArgumentTypeMismatchException`、`MissingServletRequestParameterException` |
| 处理请求体异常     | `HttpMessageNotReadableException`                            |

如果项目没有引入 Web 模块，则无法完整使用 Spring MVC 请求链路中的异常处理能力。

### 参数校验依赖

参数校验用于在请求进入业务逻辑前完成基础合法性检查，例如非空、长度、数值范围、邮箱格式、正则格式、集合大小等。全局异常处理通常会统一接管参数校验失败后抛出的异常，并转换为标准错误响应。

Maven 项目可以添加以下依赖：

```xml
<dependencies>
    <!-- 参数校验支持：提供 @Valid、@Validated、@NotNull、@NotBlank、@Size 等校验能力 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
</dependencies>
```

Gradle 项目可以添加以下依赖：

```gradle
dependencies {
    // 参数校验支持：用于 Controller 入参、RequestBody、PathVariable、RequestParam 等参数校验
    implementation 'org.springframework.boot:spring-boot-starter-validation'
}
```

Spring Boot 3 中常用的参数校验导入如下：

```java
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
```

常用校验注解如下：

| 注解            | 适用类型           | 说明                                      |
| --------------- | ------------------ | ----------------------------------------- |
| `@NotNull`      | 任意对象           | 值不能为 `null`                           |
| `@NotBlank`     | 字符串             | 字符串不能为 `null`，且去除空白后不能为空 |
| `@NotEmpty`     | 字符串、集合、数组 | 不能为 `null`，且长度或大小不能为 0       |
| `@Size`         | 字符串、集合、数组 | 限制长度或元素数量                        |
| `@Min` / `@Max` | 数字               | 限制最小值和最大值                        |
| `@Email`        | 字符串             | 校验邮箱格式                              |
| `@Pattern`      | 字符串             | 使用正则表达式校验                        |
| `@Valid`        | 对象、嵌套对象     | 触发 Bean Validation 校验                 |
| `@Validated`    | 类、方法、参数     | 支持 Spring 校验和分组校验                |

参数校验失败后，常见异常类型如下：

| 异常类型                                  | 常见触发场景                                        |
| ----------------------------------------- | --------------------------------------------------- |
| `MethodArgumentNotValidException`         | `@RequestBody` 对象字段校验失败                     |
| `ConstraintViolationException`            | `@RequestParam`、`@PathVariable` 或方法参数校验失败 |
| `BindException`                           | 表单对象或普通对象参数绑定校验失败                  |
| `MissingServletRequestParameterException` | 必填请求参数缺失                                    |
| `MethodArgumentTypeMismatchException`     | 参数类型转换失败                                    |
| `HttpMessageNotReadableException`         | JSON 格式错误或请求体不可读                         |

在实际项目中，建议将参数校验依赖作为 Web 项目的基础依赖引入。Controller 层通过注解声明参数规则，全局异常处理类负责统一返回错误信息，可以避免业务代码中重复编写大量参数判断逻辑。



## 响应结构设计

响应结构设计用于规范接口成功和失败时的返回格式。全局异常处理最终会将不同异常转换为统一响应对象，因此需要先定义响应对象、错误码枚举和异常响应字段，保证前端或调用方可以按照固定规则解析接口结果。

建议统一响应结构包含 `code`、`message`、`data`、`timestamp`、`path`、`traceId` 等字段。其中，业务成功时主要关注 `code`、`message`、`data`；异常失败时则需要额外关注请求路径、错误时间和链路追踪编号。

### 统一响应对象

统一响应对象用于封装所有接口返回结果。无论接口执行成功、业务失败、参数校验失败还是系统异常，都建议通过统一对象返回，避免不同接口返回结构不一致。

推荐响应结构如下：

| 字段        | 类型     | 说明                             |
| ----------- | -------- | -------------------------------- |
| `code`      | `String` | 响应码，成功和失败都使用固定编码 |
| `message`   | `String` | 响应消息，用于展示或问题说明     |
| `data`      | `T`      | 响应数据，异常场景通常为空       |
| `timestamp` | `Long`   | 响应时间戳，便于排查请求时间     |
| `path`      | `String` | 请求路径，异常场景用于定位接口   |
| `traceId`   | `String` | 请求追踪编号，便于日志链路查询   |

文件位置：`src/main/java/io/github/atengk/common/core/domain/Result.java`

统一响应对象用于封装接口成功和失败的标准返回结构。

```java
package io.github.atengk.common.core.domain;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.atengk.common.core.enums.ResultCode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一接口响应对象
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Result<T> {

    /**
     * 响应码
     */
    private String code;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 响应时间戳
     */
    private Long timestamp;

    /**
     * 请求路径
     */
    private String path;

    /**
     * 链路追踪编号
     */
    private String traceId;

    /**
     * 返回成功响应
     *
     * @param data 响应数据
     * @return 统一响应对象
     */
    public static <T> Result<T> success(T data) {
        return build(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data, null);
    }

    /**
     * 返回成功响应
     *
     * @return 统一响应对象
     */
    public static <T> Result<T> success() {
        return build(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), null, null);
    }

    /**
     * 返回失败响应
     *
     * @param resultCode 响应码枚举
     * @return 统一响应对象
     */
    public static <T> Result<T> fail(ResultCode resultCode) {
        return build(resultCode.getCode(), resultCode.getMessage(), null, null);
    }

    /**
     * 返回失败响应
     *
     * @param code    响应码
     * @param message 响应消息
     * @param path    请求路径
     * @return 统一响应对象
     */
    public static <T> Result<T> fail(String code, String message, String path) {
        return build(code, message, null, path);
    }

    /**
     * 构建统一响应对象
     *
     * @param code    响应码
     * @param message 响应消息
     * @param data    响应数据
     * @param path    请求路径
     * @return 统一响应对象
     */
    private static <T> Result<T> build(String code, String message, T data, String path) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        result.setData(data);
        result.setPath(path);
        result.setTimestamp(DateUtil.current());
        result.setTraceId(IdUtil.fastSimpleUUID());
        return result;
    }
}
```

成功响应示例：

```json
{
  "code": "00000",
  "message": "操作成功",
  "data": {
    "id": 1001,
    "username": "admin"
  },
  "timestamp": 1778054400000,
  "traceId": "7c24e9fcf1a64dbb8b3d7f5c5c2a6e11"
}
```

失败响应示例：

```json
{
  "code": "A0400",
  "message": "请求参数错误",
  "timestamp": 1778054400000,
  "path": "/api/users",
  "traceId": "e1dc2d9378e445ca85f3b2ff4471cce8"
}
```

### 错误码枚举

错误码枚举用于统一管理系统中的错误编码和错误描述。相比直接在代码中返回字符串，错误码枚举更利于维护，也便于前端、网关、监控系统和接口文档统一识别错误类型。

推荐错误码按类别划分：

| 错误码段 | 类型           | 示例                             |
| -------- | -------------- | -------------------------------- |
| `00000`  | 成功           | 操作成功                         |
| `Axxxx`  | 客户端错误     | 参数错误、未登录、权限不足       |
| `Bxxxx`  | 业务错误       | 用户不存在、订单状态异常         |
| `Cxxxx`  | 系统错误       | 系统异常、数据库异常             |
| `Dxxxx`  | 第三方服务错误 | 短信失败、支付失败、对象存储失败 |

文件位置：`src/main/java/io/github/atengk/common/core/enums/ResultCode.java`

错误码枚举用于集中维护接口响应码和默认响应消息。

```java
package io.github.atengk.common.core.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 接口响应码枚举
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    /**
     * 操作成功
     */
    SUCCESS("00000", "操作成功"),

    /**
     * 请求参数错误
     */
    PARAM_ERROR("A0400", "请求参数错误"),

    /**
     * 参数校验失败
     */
    VALIDATION_ERROR("A0401", "参数校验失败"),

    /**
     * 请求资源不存在
     */
    NOT_FOUND("A0404", "请求资源不存在"),

    /**
     * 请求方法不支持
     */
    METHOD_NOT_ALLOWED("A0405", "请求方法不支持"),

    /**
     * 业务处理失败
     */
    BUSINESS_ERROR("B0001", "业务处理失败"),

    /**
     * 用户不存在
     */
    USER_NOT_FOUND("B0101", "用户不存在"),

    /**
     * 数据状态异常
     */
    DATA_STATUS_ERROR("B0201", "数据状态异常"),

    /**
     * 系统内部异常
     */
    SYSTEM_ERROR("C0001", "系统内部异常"),

    /**
     * 数据库访问异常
     */
    DATABASE_ERROR("C0300", "数据库访问异常"),

    /**
     * 第三方服务异常
     */
    THIRD_PARTY_ERROR("D0001", "第三方服务异常"),

    /**
     * 远程接口调用失败
     */
    REMOTE_CALL_ERROR("D0101", "远程接口调用失败");

    /**
     * 响应码
     */
    private final String code;

    /**
     * 响应消息
     */
    private final String message;
}
```

错误码设计建议保持稳定，不要频繁修改已经对外使用的错误码。错误消息可以根据业务语义适当调整，但错误码一旦被前端、第三方调用方或监控系统依赖，就应作为接口契约的一部分维护。

### 异常响应字段

异常响应字段用于描述接口失败时需要返回给调用方的信息。与成功响应相比，异常响应更关注失败原因、请求位置和排查线索，因此建议保留 `path`、`timestamp`、`traceId` 等字段。

推荐异常响应字段如下：

| 字段        | 是否必填 | 说明                                               |
| ----------- | -------- | -------------------------------------------------- |
| `code`      | 是       | 错误码，用于标识异常类型                           |
| `message`   | 是       | 错误信息，用于提示失败原因                         |
| `data`      | 否       | 异常场景通常为空，参数校验失败时可返回字段错误详情 |
| `timestamp` | 是       | 异常发生时间戳                                     |
| `path`      | 是       | 当前请求路径                                       |
| `traceId`   | 是       | 链路追踪编号                                       |
| `errors`    | 否       | 字段级错误详情，适用于参数校验失败                 |

如果希望参数校验失败时返回更清晰的字段错误信息，可以定义字段错误对象。

文件位置：`src/main/java/io/github/atengk/common/core/domain/FieldErrorItem.java`

字段错误对象用于描述参数校验失败时的字段名称和错误原因。

```java
package io.github.atengk.common.core.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 字段错误信息
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FieldErrorItem {

    /**
     * 字段名称
     */
    private String field;

    /**
     * 错误信息
     */
    private String message;

    /**
     * 被拒绝的值
     */
    private Object rejectedValue;
}
```

参数校验异常响应示例：

```json
{
  "code": "A0401",
  "message": "参数校验失败",
  "data": [
    {
      "field": "username",
      "message": "用户名不能为空",
      "rejectedValue": ""
    },
    {
      "field": "email",
      "message": "邮箱格式不正确",
      "rejectedValue": "test"
    }
  ],
  "timestamp": 1778054400000,
  "path": "/api/users",
  "traceId": "e85b84cf5f244c12bb89b4d4933df087"
}
```

异常响应字段不建议包含完整异常堆栈、SQL 语句、服务器文件路径、内部类全限定名等敏感信息。这些内容应记录在服务端日志中，接口响应只返回必要的错误摘要和排查编号。

## 异常体系设计

异常体系设计用于规范项目中不同异常的分类、抛出位置和处理方式。合理的异常体系可以让业务代码只关注业务判断，让全局异常处理类负责统一转换响应结果。

建议将异常划分为业务异常、参数校验异常、系统运行异常和第三方服务异常。业务异常通常由开发人员主动抛出；参数校验异常通常由 Spring MVC 和 Bean Validation 自动抛出；系统运行异常通常属于非预期异常；第三方服务异常则用于描述外部依赖调用失败。

### 业务异常

业务异常表示请求参数格式正确，但不满足当前业务规则。此类异常通常由 Service 层主动抛出，再由全局异常处理统一转换为业务错误响应。

常见业务异常包括：

| 场景         | 示例                                   |
| ------------ | -------------------------------------- |
| 数据不存在   | 用户不存在、订单不存在、文件不存在     |
| 状态不允许   | 订单已关闭不能支付、账号已禁用不能登录 |
| 业务额度不足 | 库存不足、余额不足、积分不足           |
| 重复操作     | 用户名已存在、订单已提交、任务已执行   |
| 数据冲突     | 乐观锁失败、数据版本不一致             |

文件位置：`src/main/java/io/github/atengk/common/core/exception/BusinessException.java`

业务异常用于在业务逻辑中主动表达可预期的业务失败。

```java
package io.github.atengk.common.core.exception;

import io.github.atengk.common.core.enums.ResultCode;
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
     * 错误码
     */
    private final String code;

    /**
     * 创建业务异常
     *
     * @param message 错误信息
     */
    public BusinessException(String message) {
        super(message);
        this.code = ResultCode.BUSINESS_ERROR.getCode();
    }

    /**
     * 创建业务异常
     *
     * @param resultCode 错误码枚举
     */
    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    /**
     * 创建业务异常
     *
     * @param resultCode 错误码枚举
     * @param message    自定义错误信息
     */
    public BusinessException(ResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
    }
}
```

业务异常使用示例：

```java
package io.github.atengk.user.service.impl;

import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.common.core.enums.ResultCode;
import io.github.atengk.common.core.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 用户服务实现
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Service
public class UserServiceImpl {

    /**
     * 查询用户详情
     *
     * @param userId 用户ID
     * @return 用户名称
     */
    public String getUserName(Long userId) {
        String username = null;

        if (ObjectUtil.isNull(username)) {
            log.warn("查询用户失败，用户不存在，userId={}", userId);
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        return username;
    }
}
```

业务异常属于可预期异常，日志级别一般使用 `warn`。如果业务异常频繁出现，通常说明调用方参数、页面操作流程或业务规则提示需要优化。

### 参数校验异常

参数校验异常表示请求参数不满足接口入参约束。此类异常通常不需要手动抛出，而是由 Spring MVC、Bean Validation 或参数绑定流程自动触发。

常见参数校验异常包括：

| 异常类型                                  | 触发场景                                      |
| ----------------------------------------- | --------------------------------------------- |
| `MethodArgumentNotValidException`         | `@RequestBody` 对象字段校验失败               |
| `ConstraintViolationException`            | `@RequestParam`、`@PathVariable` 参数校验失败 |
| `BindException`                           | 表单对象或普通对象绑定校验失败                |
| `MissingServletRequestParameterException` | 必填请求参数缺失                              |
| `MethodArgumentTypeMismatchException`     | 参数类型转换失败                              |
| `HttpMessageNotReadableException`         | 请求体 JSON 格式错误或不可读                  |

参数校验 DTO 示例：

文件位置：`src/main/java/io/github/atengk/user/dto/UserCreateDTO.java`

用户创建请求对象用于演示 RequestBody 参数校验规则。

```java
package io.github.atengk.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户创建请求对象
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
public class UserCreateDTO {

    /**
     * 用户名
     */
    @NotBlank(message = "用户名不能为空")
    @Size(max = 30, message = "用户名长度不能超过30个字符")
    private String username;

    /**
     * 邮箱
     */
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;
}
```

Controller 参数校验示例：

文件位置：`src/main/java/io/github/atengk/user/controller/UserController.java`

用户接口示例用于演示 `@RequestBody`、`@PathVariable` 和 `@RequestParam` 的参数校验。

```java
package io.github.atengk.user.controller;

import io.github.atengk.common.core.domain.Result;
import io.github.atengk.user.dto.UserCreateDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 用户接口
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/users")
public class UserController {

    /**
     * 创建用户
     *
     * @param request 创建用户请求对象
     * @return 创建结果
     */
    @PostMapping
    public Result<Boolean> createUser(@Valid @RequestBody UserCreateDTO request) {
        log.info("创建用户，username={}", request.getUsername());
        return Result.success(Boolean.TRUE);
    }

    /**
     * 查询用户详情
     *
     * @param id 用户ID
     * @return 用户详情
     */
    @GetMapping("/{id}")
    public Result<String> getUser(@PathVariable @Min(value = 1, message = "用户ID必须大于0") Long id) {
        log.info("查询用户详情，id={}", id);
        return Result.success("admin");
    }

    /**
     * 根据用户名搜索用户
     *
     * @param username 用户名
     * @return 用户名称
     */
    @GetMapping("/search")
    public Result<String> searchUser(@RequestParam @NotBlank(message = "用户名不能为空") String username) {
        log.info("搜索用户，username={}", username);
        return Result.success(username);
    }
}
```

参数校验异常属于客户端输入错误，通常不需要记录完整异常堆栈。建议在全局异常处理中提取字段、错误信息和被拒绝的值，返回给调用方进行修正。

### 系统运行异常

系统运行异常表示程序运行过程中出现非预期错误，例如空指针异常、数据库连接异常、文件读取失败、类型转换异常等。此类异常通常不应该直接暴露给客户端，而应统一返回系统错误提示，并在服务端记录完整堆栈。

常见系统运行异常包括：

| 异常类型                | 示例                       |
| ----------------------- | -------------------------- |
| `NullPointerException`  | 未做空值判断导致空指针     |
| `IllegalStateException` | 当前状态不符合程序执行要求 |
| `DataAccessException`   | 数据库访问异常             |
| `IOException`           | 文件或网络 IO 异常         |
| `RuntimeException`      | 未分类运行时异常           |
| `Exception`             | 兜底异常                   |

系统异常处理原则如下：

| 原则         | 说明                                                 |
| ------------ | ---------------------------------------------------- |
| 对外提示简洁 | 返回“系统内部异常”或统一错误提示                     |
| 对内日志完整 | 服务端日志保留异常类型、请求路径、请求参数、堆栈信息 |
| 避免暴露细节 | 不向客户端返回 SQL、堆栈、服务器路径等敏感信息       |
| 及时修复根因 | 系统异常通常代表代码缺陷、配置错误或基础设施异常     |

系统运行异常不建议在业务代码中随意捕获后忽略。如果必须捕获，应明确处理策略，例如重试、补偿、转换为业务异常或继续向上抛出。

示例：

```java
try {
    // 执行系统资源访问、文件处理、数据库操作等逻辑
} catch (Exception e) {
    log.error("处理系统资源失败", e);
    throw e;
}
```

以上写法保留了异常堆栈，并继续交给全局异常处理进行统一响应转换。不要只打印错误消息后返回 `null`，否则容易造成后续空指针或数据不一致问题。

### 第三方服务异常

第三方服务异常表示当前系统调用外部依赖失败，例如支付服务、短信服务、对象存储、消息推送、远程 HTTP 接口、RPC 服务等。此类异常需要区分是外部服务不可用、响应超时、返回业务失败，还是当前系统请求参数不正确。

常见第三方服务异常场景如下：

| 场景           | 示例                                 |
| -------------- | ------------------------------------ |
| 网络异常       | 连接超时、读取超时、DNS 解析失败     |
| 外部服务错误   | 第三方接口返回 500、服务不可用       |
| 业务调用失败   | 支付失败、短信发送失败、文件上传失败 |
| 鉴权失败       | AppKey 错误、签名错误、Token 过期    |
| 限流或配额不足 | 请求频率过高、余额不足、额度耗尽     |

文件位置：`src/main/java/io/github/atengk/common/core/exception/ThirdPartyException.java`

第三方服务异常用于封装外部接口、远程服务和中间件调用失败场景。

```java
package io.github.atengk.common.core.exception;

import io.github.atengk.common.core.enums.ResultCode;
import lombok.Getter;

/**
 * 第三方服务异常
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Getter
public class ThirdPartyException extends RuntimeException {

    /**
     * 错误码
     */
    private final String code;

    /**
     * 第三方服务名称
     */
    private final String serviceName;

    /**
     * 创建第三方服务异常
     *
     * @param serviceName 第三方服务名称
     * @param message     错误信息
     */
    public ThirdPartyException(String serviceName, String message) {
        super(message);
        this.serviceName = serviceName;
        this.code = ResultCode.THIRD_PARTY_ERROR.getCode();
    }

    /**
     * 创建第三方服务异常
     *
     * @param serviceName 第三方服务名称
     * @param message     错误信息
     * @param cause       原始异常
     */
    public ThirdPartyException(String serviceName, String message, Throwable cause) {
        super(message, cause);
        this.serviceName = serviceName;
        this.code = ResultCode.THIRD_PARTY_ERROR.getCode();
    }
}
```

第三方服务异常使用示例：

```java
package io.github.atengk.message.service;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.common.core.exception.ThirdPartyException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 短信服务
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Service
public class SmsService {

    /**
     * 发送验证码
     *
     * @param phone 手机号
     * @param code  验证码
     */
    public void sendCode(String phone, String code) {
        try {
            if (StrUtil.isBlank(phone) || StrUtil.isBlank(code)) {
                throw new ThirdPartyException("短信服务", "手机号或验证码不能为空");
            }

            log.info("调用短信服务发送验证码，phone={}", phone);
            // 此处调用真实短信服务 SDK 或远程接口

        } catch (ThirdPartyException e) {
            throw e;
        } catch (Exception e) {
            log.error("短信服务调用失败，phone={}", phone, e);
            throw new ThirdPartyException("短信服务", "短信发送失败，请稍后重试", e);
        }
    }
}
```

第三方服务异常建议保留原始异常 `cause`，方便服务端排查真实失败原因。对外响应时只返回统一错误提示，避免暴露第三方接口地址、签名参数、密钥、请求头等敏感信息。



## 全局异常处理实现

全局异常处理实现用于集中接管 Controller 请求链路中的异常，并将异常转换为统一响应对象。Spring Boot 3 项目中通常使用 `@RestControllerAdvice` 声明全局异常处理类，使用 `@ExceptionHandler` 标识具体异常处理方法。

本节基于前文定义的 `Result`、`ResultCode`、`BusinessException`、`ThirdPartyException` 和 `FieldErrorItem` 进行实现。异常处理顺序建议从具体异常到通用异常，避免业务异常、参数异常被兜底异常提前拦截。

### 全局异常处理类

全局异常处理类建议放在 `common` 模块或基础包路径下，保证可以被 Spring Boot 启动类扫描到。如果启动类位于 `io.github.atengk` 包下，则异常处理类可以放在 `io.github.atengk.common.web.handler` 包中。

文件位置：`src/main/java/io/github/atengk/common/web/handler/GlobalExceptionHandler.java`

全局异常处理类用于统一处理业务异常、参数校验异常、请求参数异常、第三方服务异常和未知异常。

```java
package io.github.atengk.common.web.handler;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.common.core.domain.FieldErrorItem;
import io.github.atengk.common.core.domain.Result;
import io.github.atengk.common.core.enums.ResultCode;
import io.github.atengk.common.core.exception.BusinessException;
import io.github.atengk.common.core.exception.ThirdPartyException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;
import java.util.Set;

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
     * @param exception 业务异常
     * @param request   HTTP 请求对象
     * @return 统一响应对象
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException exception, HttpServletRequest request) {
        String path = request.getRequestURI();
        log.warn("业务处理失败，path={}，code={}，message={}", path, exception.getCode(), exception.getMessage());
        return Result.fail(exception.getCode(), exception.getMessage(), path);
    }

    /**
     * 处理第三方服务异常
     *
     * @param exception 第三方服务异常
     * @param request   HTTP 请求对象
     * @return 统一响应对象
     */
    @ExceptionHandler(ThirdPartyException.class)
    public Result<Void> handleThirdPartyException(ThirdPartyException exception, HttpServletRequest request) {
        String path = request.getRequestURI();
        log.error("第三方服务调用失败，path={}，serviceName={}，message={}",
                path, exception.getServiceName(), exception.getMessage(), exception);
        return Result.fail(exception.getCode(), exception.getMessage(), path);
    }

    /**
     * 处理 RequestBody 参数校验异常
     *
     * @param exception RequestBody 参数校验异常
     * @param request   HTTP 请求对象
     * @return 统一响应对象
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<List<FieldErrorItem>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {
        String path = request.getRequestURI();
        List<FieldErrorItem> errors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::buildFieldErrorItem)
                .toList();

        log.warn("RequestBody 参数校验失败，path={}，errors={}", path, errors);
        return Result.fail(ResultCode.VALIDATION_ERROR.getCode(), ResultCode.VALIDATION_ERROR.getMessage(), errors, path);
    }

    /**
     * 处理表单对象参数绑定异常
     *
     * @param exception 参数绑定异常
     * @param request   HTTP 请求对象
     * @return 统一响应对象
     */
    @ExceptionHandler(BindException.class)
    public Result<List<FieldErrorItem>> handleBindException(BindException exception, HttpServletRequest request) {
        String path = request.getRequestURI();
        List<FieldErrorItem> errors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::buildFieldErrorItem)
                .toList();

        log.warn("参数绑定校验失败，path={}，errors={}", path, errors);
        return Result.fail(ResultCode.VALIDATION_ERROR.getCode(), ResultCode.VALIDATION_ERROR.getMessage(), errors, path);
    }

    /**
     * 处理 PathVariable 和 RequestParam 参数校验异常
     *
     * @param exception 约束校验异常
     * @param request   HTTP 请求对象
     * @return 统一响应对象
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public Result<List<FieldErrorItem>> handleConstraintViolationException(
            ConstraintViolationException exception,
            HttpServletRequest request) {
        String path = request.getRequestURI();
        Set<ConstraintViolation<?>> violations = exception.getConstraintViolations();

        List<FieldErrorItem> errors = violations.stream()
                .map(violation -> new FieldErrorItem(
                        getSimplePropertyName(violation.getPropertyPath().toString()),
                        violation.getMessage(),
                        violation.getInvalidValue()
                ))
                .toList();

        log.warn("请求参数约束校验失败，path={}，errors={}", path, errors);
        return Result.fail(ResultCode.VALIDATION_ERROR.getCode(), ResultCode.VALIDATION_ERROR.getMessage(), errors, path);
    }

    /**
     * 处理请求参数缺失异常
     *
     * @param exception 请求参数缺失异常
     * @param request   HTTP 请求对象
     * @return 统一响应对象
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<Void> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException exception,
            HttpServletRequest request) {
        String path = request.getRequestURI();
        String message = "缺少必要请求参数：" + exception.getParameterName();

        log.warn("请求参数缺失，path={}，parameterName={}，parameterType={}",
                path, exception.getParameterName(), exception.getParameterType());
        return Result.fail(ResultCode.PARAM_ERROR.getCode(), message, path);
    }

    /**
     * 处理路径变量缺失异常
     *
     * @param exception 路径变量缺失异常
     * @param request   HTTP 请求对象
     * @return 统一响应对象
     */
    @ExceptionHandler(MissingPathVariableException.class)
    public Result<Void> handleMissingPathVariableException(
            MissingPathVariableException exception,
            HttpServletRequest request) {
        String path = request.getRequestURI();
        String message = "缺少路径参数：" + exception.getVariableName();

        log.warn("路径参数缺失，path={}，variableName={}", path, exception.getVariableName());
        return Result.fail(ResultCode.PARAM_ERROR.getCode(), message, path);
    }

    /**
     * 处理参数类型转换异常
     *
     * @param exception 参数类型转换异常
     * @param request   HTTP 请求对象
     * @return 统一响应对象
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result<Void> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request) {
        String path = request.getRequestURI();
        String requiredType = ObjectUtil.isNotNull(exception.getRequiredType())
                ? exception.getRequiredType().getSimpleName()
                : "未知类型";
        String message = "请求参数类型错误：" + exception.getName();

        log.warn("请求参数类型转换失败，path={}，name={}，value={}，requiredType={}",
                path, exception.getName(), exception.getValue(), requiredType);
        return Result.fail(ResultCode.PARAM_ERROR.getCode(), message, path);
    }

    /**
     * 处理请求体不可读异常
     *
     * @param exception 请求体不可读异常
     * @param request   HTTP 请求对象
     * @return 统一响应对象
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException exception,
            HttpServletRequest request) {
        String path = request.getRequestURI();

        log.warn("请求体读取失败，path={}，message={}", path, exception.getMessage());
        return Result.fail(ResultCode.PARAM_ERROR.getCode(), "请求体格式错误或不可读取", path);
    }

    /**
     * 处理请求方法不支持异常
     *
     * @param exception 请求方法不支持异常
     * @param request   HTTP 请求对象
     * @return 统一响应对象
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Result<Void> handleHttpRequestMethodNotSupportedException(
            HttpRequestMethodNotSupportedException exception,
            HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = exception.getMethod();
        String[] supportedMethods = exception.getSupportedMethods();

        String message = CollUtil.isNotEmpty(List.of(ObjectUtil.defaultIfNull(supportedMethods, new String[0])))
                ? "请求方法不支持：" + method
                : "请求方法不支持";

        log.warn("请求方法不支持，path={}，method={}，supportedMethods={}", path, method, supportedMethods);
        return Result.fail(ResultCode.METHOD_NOT_ALLOWED.getCode(), message, path);
    }

    /**
     * 处理未知异常
     *
     * @param exception 未知异常
     * @param request   HTTP 请求对象
     * @return 统一响应对象
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception exception, HttpServletRequest request) {
        String path = request.getRequestURI();

        log.error("系统未知异常，path={}，message={}", path, exception.getMessage(), exception);
        return Result.fail(ResultCode.SYSTEM_ERROR.getCode(), ResultCode.SYSTEM_ERROR.getMessage(), path);
    }

    /**
     * 构建字段错误对象
     *
     * @param fieldError Spring 字段错误对象
     * @return 字段错误对象
     */
    private FieldErrorItem buildFieldErrorItem(FieldError fieldError) {
        return new FieldErrorItem(
                fieldError.getField(),
                fieldError.getDefaultMessage(),
                fieldError.getRejectedValue()
        );
    }

    /**
     * 获取简单属性名称
     *
     * @param propertyPath 参数路径
     * @return 简单属性名称
     */
    private String getSimplePropertyName(String propertyPath) {
        if (ObjectUtil.isEmpty(propertyPath)) {
            return propertyPath;
        }
        int index = propertyPath.lastIndexOf(".");
        return index >= 0 ? propertyPath.substring(index + 1) : propertyPath;
    }
}
```

上面的异常处理类中，业务异常和参数异常使用 `warn` 级别日志，因为这类异常通常是可预期的；第三方服务异常和未知异常使用 `error` 级别日志，因为这类异常通常需要服务端排查。未知异常处理方法必须放在最后，避免提前拦截更具体的异常类型。

如果前文的 `Result` 类还没有支持携带 `data` 的失败响应，需要补充下面这个重载方法。

文件位置：`src/main/java/io/github/atengk/common/core/domain/Result.java`

该方法用于支持参数校验失败时返回字段错误详情。

```java
/**
 * 返回失败响应
 *
 * @param code    响应码
 * @param message 响应消息
 * @param data    响应数据
 * @param path    请求路径
 * @return 统一响应对象
 */
public static <T> Result<T> fail(String code, String message, T data, String path) {
    return build(code, message, data, path);
}
```

### 业务异常处理

业务异常处理用于接管业务代码主动抛出的 `BusinessException`。这类异常通常代表请求格式正确，但不满足业务规则，例如用户不存在、订单状态异常、余额不足、数据重复等。

业务异常处理方法如下：

```java
/**
 * 处理业务异常
 *
 * @param exception 业务异常
 * @param request   HTTP 请求对象
 * @return 统一响应对象
 */
@ExceptionHandler(BusinessException.class)
public Result<Void> handleBusinessException(BusinessException exception, HttpServletRequest request) {
    String path = request.getRequestURI();
    log.warn("业务处理失败，path={}，code={}，message={}", path, exception.getCode(), exception.getMessage());
    return Result.fail(exception.getCode(), exception.getMessage(), path);
}
```

业务代码中不建议直接返回失败响应对象，而是抛出业务异常交给全局异常处理器统一处理。

示例：

```java
if (ObjectUtil.isNull(user)) {
    log.warn("用户不存在，userId={}", userId);
    throw new BusinessException(ResultCode.USER_NOT_FOUND);
}
```

这样可以让 Service 层保持业务语义清晰，Controller 层只负责接口入参和结果返回，全局异常处理器统一负责异常响应结构。

### 参数校验异常处理

参数校验异常处理主要接管 `@RequestBody`、表单对象、普通对象绑定时产生的字段校验错误。常见异常包括 `MethodArgumentNotValidException` 和 `BindException`。

`@RequestBody` 参数校验失败时，通常会抛出 `MethodArgumentNotValidException`。

```java
/**
 * 处理 RequestBody 参数校验异常
 *
 * @param exception RequestBody 参数校验异常
 * @param request   HTTP 请求对象
 * @return 统一响应对象
 */
@ExceptionHandler(MethodArgumentNotValidException.class)
public Result<List<FieldErrorItem>> handleMethodArgumentNotValidException(
        MethodArgumentNotValidException exception,
        HttpServletRequest request) {
    String path = request.getRequestURI();
    List<FieldErrorItem> errors = exception.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(this::buildFieldErrorItem)
            .toList();

    log.warn("RequestBody 参数校验失败，path={}，errors={}", path, errors);
    return Result.fail(ResultCode.VALIDATION_ERROR.getCode(), ResultCode.VALIDATION_ERROR.getMessage(), errors, path);
}
```

表单对象或普通对象参数绑定失败时，通常会抛出 `BindException`。

```java
/**
 * 处理表单对象参数绑定异常
 *
 * @param exception 参数绑定异常
 * @param request   HTTP 请求对象
 * @return 统一响应对象
 */
@ExceptionHandler(BindException.class)
public Result<List<FieldErrorItem>> handleBindException(BindException exception, HttpServletRequest request) {
    String path = request.getRequestURI();
    List<FieldErrorItem> errors = exception.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(this::buildFieldErrorItem)
            .toList();

    log.warn("参数绑定校验失败，path={}，errors={}", path, errors);
    return Result.fail(ResultCode.VALIDATION_ERROR.getCode(), ResultCode.VALIDATION_ERROR.getMessage(), errors, path);
}
```

参数校验异常建议返回字段级错误详情，便于前端准确定位哪个字段不合法。例如用户名为空、邮箱格式错误、密码长度不足等问题，都可以通过 `field` 和 `message` 字段明确表达。

### 请求参数异常处理

请求参数异常处理用于接管请求参数缺失、参数类型错误、请求体格式错误、请求方法不支持等问题。这类异常不一定来自 Bean Validation，也可能来自 Spring MVC 的参数绑定和请求解析流程。

请求参数缺失处理：

```java
/**
 * 处理请求参数缺失异常
 *
 * @param exception 请求参数缺失异常
 * @param request   HTTP 请求对象
 * @return 统一响应对象
 */
@ExceptionHandler(MissingServletRequestParameterException.class)
public Result<Void> handleMissingServletRequestParameterException(
        MissingServletRequestParameterException exception,
        HttpServletRequest request) {
    String path = request.getRequestURI();
    String message = "缺少必要请求参数：" + exception.getParameterName();

    log.warn("请求参数缺失，path={}，parameterName={}，parameterType={}",
            path, exception.getParameterName(), exception.getParameterType());
    return Result.fail(ResultCode.PARAM_ERROR.getCode(), message, path);
}
```

参数类型转换失败处理：

```java
/**
 * 处理参数类型转换异常
 *
 * @param exception 参数类型转换异常
 * @param request   HTTP 请求对象
 * @return 统一响应对象
 */
@ExceptionHandler(MethodArgumentTypeMismatchException.class)
public Result<Void> handleMethodArgumentTypeMismatchException(
        MethodArgumentTypeMismatchException exception,
        HttpServletRequest request) {
    String path = request.getRequestURI();
    String message = "请求参数类型错误：" + exception.getName();

    log.warn("请求参数类型转换失败，path={}，name={}，value={}",
            path, exception.getName(), exception.getValue());
    return Result.fail(ResultCode.PARAM_ERROR.getCode(), message, path);
}
```

请求体格式错误处理：

```java
/**
 * 处理请求体不可读异常
 *
 * @param exception 请求体不可读异常
 * @param request   HTTP 请求对象
 * @return 统一响应对象
 */
@ExceptionHandler(HttpMessageNotReadableException.class)
public Result<Void> handleHttpMessageNotReadableException(
        HttpMessageNotReadableException exception,
        HttpServletRequest request) {
    String path = request.getRequestURI();

    log.warn("请求体读取失败，path={}，message={}", path, exception.getMessage());
    return Result.fail(ResultCode.PARAM_ERROR.getCode(), "请求体格式错误或不可读取", path);
}
```

请求参数异常通常属于客户端输入问题，响应消息可以相对明确，但不建议返回底层异常完整信息。例如 JSON 解析失败时，只需要提示“请求体格式错误或不可读取”，完整解析异常应保留在服务端日志中。

### 未知异常兜底处理

未知异常兜底处理用于接管系统中未被其他 `@ExceptionHandler` 明确处理的异常。该方法是全局异常处理的最后一道防线，可以避免异常堆栈直接返回给客户端。

未知异常处理方法如下：

```java
/**
 * 处理未知异常
 *
 * @param exception 未知异常
 * @param request   HTTP 请求对象
 * @return 统一响应对象
 */
@ExceptionHandler(Exception.class)
public Result<Void> handleException(Exception exception, HttpServletRequest request) {
    String path = request.getRequestURI();

    log.error("系统未知异常，path={}，message={}", path, exception.getMessage(), exception);
    return Result.fail(ResultCode.SYSTEM_ERROR.getCode(), ResultCode.SYSTEM_ERROR.getMessage(), path);
}
```

兜底异常处理需要注意以下几点：

| 注意项         | 说明                                                  |
| -------------- | ----------------------------------------------------- |
| 不返回异常堆栈 | 堆栈信息只能写入服务端日志，不能暴露给客户端          |
| 日志必须完整   | 使用 `log.error("xxx", exception)` 保留完整堆栈       |
| 返回通用提示   | 对外统一返回“系统内部异常”或类似提示                  |
| 放在最后处理   | `Exception.class` 范围最大，避免覆盖具体异常处理      |
| 不吞异常根因   | 不能只记录 `exception.getMessage()`，否则排查信息不足 |

未知异常通常代表代码缺陷、配置问题、数据异常或外部资源不可用。线上出现未知异常后，应通过日志中的请求路径、traceId、异常堆栈和请求参数定位根因，而不是在接口响应中暴露内部细节。

## 参数校验集成

参数校验集成用于在 Controller 层声明参数合法性规则，并将校验失败结果交给全局异常处理器统一返回。Spring Boot 3 使用 `jakarta.validation` 相关注解，常见组合包括 `@Valid`、`@Validated`、`@RequestBody`、`@PathVariable` 和 `@RequestParam`。

参数校验建议遵循以下原则：

| 原则         | 说明                                                         |
| ------------ | ------------------------------------------------------------ |
| 简单规则前置 | 非空、长度、格式、范围等基础校验放在 DTO 或 Controller 参数上 |
| 业务规则后置 | 用户是否存在、订单状态是否允许修改等规则放在 Service 层      |
| 错误提示明确 | 校验注解的 `message` 应直接描述失败原因                      |
| 不重复判断   | 已经由注解校验的规则，不建议在业务代码中重复判断             |
| 分层处理     | 参数格式错误由校验处理，业务规则错误由业务异常处理           |

### Controller 参数校验

Controller 参数校验需要在类上添加 `@Validated`，这样 `@PathVariable`、`@RequestParam` 等方法参数上的约束注解才能生效。对于 `@RequestBody` 对象参数，通常在参数前添加 `@Valid`。

文件位置：`src/main/java/io/github/atengk/user/controller/UserController.java`

用户接口示例用于演示 Controller 层常见参数校验方式。

```java
package io.github.atengk.user.controller;

import io.github.atengk.common.core.domain.Result;
import io.github.atengk.user.dto.UserCreateDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 用户接口
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/users")
public class UserController {

    /**
     * 创建用户
     *
     * @param request 创建用户请求对象
     * @return 创建结果
     */
    @PostMapping
    public Result<Boolean> createUser(@Valid @RequestBody UserCreateDTO request) {
        log.info("创建用户，username={}", request.getUsername());
        return Result.success(Boolean.TRUE);
    }

    /**
     * 查询用户详情
     *
     * @param id 用户ID
     * @return 用户详情
     */
    @GetMapping("/{id}")
    public Result<String> getUser(@PathVariable @Min(value = 1, message = "用户ID必须大于0") Long id) {
        log.info("查询用户详情，id={}", id);
        return Result.success("admin");
    }

    /**
     * 搜索用户
     *
     * @param username 用户名
     * @return 用户名称
     */
    @GetMapping("/search")
    public Result<String> searchUser(@RequestParam @NotBlank(message = "用户名不能为空") String username) {
        log.info("搜索用户，username={}", username);
        return Result.success(username);
    }
}
```

Controller 参数校验适合处理接口入参的基础合法性，例如 ID 是否大于 0、关键字是否为空、分页参数是否越界等。复杂业务判断不建议放在 Controller 参数注解中，应交给 Service 层处理。

### RequestBody 参数校验

`RequestBody` 参数校验适用于 JSON 请求体。通常做法是在 DTO 字段上添加校验注解，并在 Controller 方法参数上添加 `@Valid`。当请求体字段不满足约束时，Spring MVC 会抛出 `MethodArgumentNotValidException`，再由全局异常处理器统一返回字段错误详情。

文件位置：`src/main/java/io/github/atengk/user/dto/UserCreateDTO.java`

用户创建请求对象用于声明 JSON 请求体字段校验规则。

```java
package io.github.atengk.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户创建请求对象
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
public class UserCreateDTO {

    /**
     * 用户名
     */
    @NotBlank(message = "用户名不能为空")
    @Size(max = 30, message = "用户名长度不能超过30个字符")
    private String username;

    /**
     * 邮箱
     */
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度必须在6到20个字符之间")
    private String password;
}
```

文件位置：`src/main/java/io/github/atengk/user/controller/UserController.java`

该接口用于触发 `RequestBody` 参数校验。

```java
/**
 * 创建用户
 *
 * @param request 创建用户请求对象
 * @return 创建结果
 */
@PostMapping
public Result<Boolean> createUser(@Valid @RequestBody UserCreateDTO request) {
    log.info("创建用户，username={}，email={}", request.getUsername(), request.getEmail());
    return Result.success(Boolean.TRUE);
}
```

请求示例：

```bash
curl -X POST 'http://localhost:8080/api/users' \
  -H 'Content-Type: application/json' \
  -d '{
    "username": "",
    "email": "test",
    "password": "123"
  }'
```

可能返回：

```json
{
  "code": "A0401",
  "message": "参数校验失败",
  "data": [
    {
      "field": "username",
      "message": "用户名不能为空",
      "rejectedValue": ""
    },
    {
      "field": "email",
      "message": "邮箱格式不正确",
      "rejectedValue": "test"
    },
    {
      "field": "password",
      "message": "密码长度必须在6到20个字符之间",
      "rejectedValue": "123"
    }
  ],
  "timestamp": 1778054400000,
  "path": "/api/users",
  "traceId": "b9f3a7de67534fd19c148b6e35e15b8a"
}
```

`RequestBody` 参数校验适合处理新增、修改、批量提交等复杂请求体。对于嵌套对象，需要在嵌套字段上继续添加 `@Valid`，否则嵌套对象内部字段不会自动级联校验。

### PathVariable 与 RequestParam 校验

`PathVariable` 和 `RequestParam` 校验适用于路径参数和查询参数。此类校验需要在 Controller 类上添加 `@Validated`，否则方法参数上的约束注解可能不会生效。

文件位置：`src/main/java/io/github/atengk/user/controller/UserQueryController.java`

用户查询接口用于演示路径参数和查询参数校验。

```java
package io.github.atengk.user.controller;

import io.github.atengk.common.core.domain.Result;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 用户查询接口
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/users")
public class UserQueryController {

    /**
     * 根据用户ID查询详情
     *
     * @param id 用户ID
     * @return 用户详情
     */
    @GetMapping("/{id}")
    public Result<String> getUserById(@PathVariable @Min(value = 1, message = "用户ID必须大于0") Long id) {
        log.info("根据用户ID查询详情，id={}", id);
        return Result.success("admin");
    }

    /**
     * 分页搜索用户
     *
     * @param keyword  搜索关键字
     * @param pageNum  页码
     * @param pageSize 每页条数
     * @return 搜索结果
     */
    @GetMapping("/page")
    public Result<String> pageUser(
            @RequestParam @NotBlank(message = "搜索关键字不能为空") String keyword,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码必须大于0") Integer pageNum,
            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "每页条数必须大于0")
            @Max(value = 100, message = "每页条数不能超过100") Integer pageSize) {
        log.info("分页搜索用户，keyword={}，pageNum={}，pageSize={}", keyword, pageNum, pageSize);
        return Result.success("查询成功");
    }
}
```

路径参数校验请求示例：

```bash
curl -X GET 'http://localhost:8080/api/users/0'
```

可能返回：

```json
{
  "code": "A0401",
  "message": "参数校验失败",
  "data": [
    {
      "field": "id",
      "message": "用户ID必须大于0",
      "rejectedValue": 0
    }
  ],
  "timestamp": 1778054400000,
  "path": "/api/users/0",
  "traceId": "cd2e7d2040ad4f558f36df3bbcf5b7bb"
}
```

查询参数校验请求示例：

```bash
curl -X GET 'http://localhost:8080/api/users/page?keyword=&pageNum=0&pageSize=200'
```

可能返回：

```json
{
  "code": "A0401",
  "message": "参数校验失败",
  "data": [
    {
      "field": "keyword",
      "message": "搜索关键字不能为空",
      "rejectedValue": ""
    },
    {
      "field": "pageNum",
      "message": "页码必须大于0",
      "rejectedValue": 0
    },
    {
      "field": "pageSize",
      "message": "每页条数不能超过100",
      "rejectedValue": 200
    }
  ],
  "timestamp": 1778054400000,
  "path": "/api/users/page",
  "traceId": "1e45d4901195462285ccf31d30b8d6c4"
}
```

`PathVariable` 和 `RequestParam` 校验一般用于轻量参数约束。对于字段较多的查询接口，可以将查询条件封装成 DTO，并使用 `@Validated` 或 `@Valid` 触发表单对象校验，避免 Controller 方法参数过长。



## 日志记录设计

日志记录设计用于配合全局异常处理完成问题定位。接口异常响应面向调用方，只应返回必要的错误码、错误消息、请求路径和 traceId；完整异常堆栈、请求上下文、第三方调用细节等信息应保留在服务端日志中。

日志记录建议区分业务异常、系统异常和请求上下文日志。业务异常通常属于可预期异常，使用 `warn` 级别；系统异常通常属于非预期异常，使用 `error` 级别并记录完整堆栈；请求上下文日志用于串联一次请求的入口、出口和异常信息。

### 业务异常日志

业务异常表示请求已经进入业务处理流程，但不满足业务规则，例如用户不存在、订单状态不允许修改、库存不足、重复提交等。这类异常通常不是系统故障，因此不建议记录为 `error`，否则容易污染错误日志和告警。

业务异常日志建议使用 `warn` 级别，记录请求路径、错误码、错误消息和关键业务参数。

```java
/**
 * 处理业务异常
 *
 * @param exception 业务异常
 * @param request   HTTP 请求对象
 * @return 统一响应对象
 */
@ExceptionHandler(BusinessException.class)
public Result<Void> handleBusinessException(BusinessException exception, HttpServletRequest request) {
    String path = request.getRequestURI();
    log.warn("业务处理失败，path={}，code={}，message={}", path, exception.getCode(), exception.getMessage());
    return Result.fail(exception.getCode(), exception.getMessage(), path);
}
```

Service 层抛出业务异常时，也可以记录关键业务参数，方便后续定位是哪一次业务操作触发了异常。

```java
if (ObjectUtil.isNull(user)) {
    log.warn("查询用户失败，用户不存在，userId={}", userId);
    throw new BusinessException(ResultCode.USER_NOT_FOUND);
}
```

业务异常日志建议记录以下内容：

| 日志字段     | 说明                                           |
| ------------ | ---------------------------------------------- |
| `traceId`    | 当前请求链路编号，通常通过 MDC 自动输出        |
| `path`       | 请求路径                                       |
| `code`       | 业务错误码                                     |
| `message`    | 业务错误描述                                   |
| 关键业务参数 | 例如 `userId`、`orderId`、`tenantId`、`taskId` |

业务异常日志不建议记录完整堆栈，除非该业务异常包含非预期原因。例如库存不足、用户不存在这类明确业务失败，只需要记录 `warn` 摘要；如果业务异常是由第三方服务异常或数据异常包装而来，则可以根据实际情况记录堆栈。

### 系统异常日志

系统异常表示程序运行过程中出现非预期错误，例如空指针异常、数据库异常、文件读写异常、JSON 解析异常、外部服务不可用等。此类异常需要使用 `error` 级别记录完整异常堆栈，避免线上问题无法定位。

系统异常日志处理示例：

```java
/**
 * 处理未知异常
 *
 * @param exception 未知异常
 * @param request   HTTP 请求对象
 * @return 统一响应对象
 */
@ExceptionHandler(Exception.class)
public Result<Void> handleException(Exception exception, HttpServletRequest request) {
    String path = request.getRequestURI();

    log.error("系统未知异常，path={}，message={}", path, exception.getMessage(), exception);
    return Result.fail(ResultCode.SYSTEM_ERROR.getCode(), ResultCode.SYSTEM_ERROR.getMessage(), path);
}
```

`log.error` 的最后一个参数必须传入异常对象本身，例如 `exception`，这样日志框架才能输出完整堆栈。不要只记录 `exception.getMessage()`，否则只能看到错误摘要，无法定位异常发生的类、方法和行号。

推荐写法：

```java
log.error("系统未知异常，path={}，message={}", path, exception.getMessage(), exception);
```

不推荐写法：

```java
log.error("系统未知异常，message={}", exception.getMessage());
```

系统异常日志建议记录以下内容：

| 日志字段           | 说明             |
| ------------------ | ---------------- |
| `traceId`          | 当前请求链路编号 |
| `path`             | 请求路径         |
| `method`           | 请求方法         |
| `queryString`      | URL 查询参数     |
| `exceptionClass`   | 异常类型         |
| `exceptionMessage` | 异常消息         |
| `stackTrace`       | 完整异常堆栈     |

对于系统异常，对外响应应保持简洁，例如“系统内部异常”；对内日志必须完整，便于后续根据 traceId 在日志平台中检索完整链路。

### 请求上下文日志

请求上下文日志用于记录每次 HTTP 请求的基础信息，并通过 traceId 将请求入口、业务日志、异常日志和响应结果串联起来。常见做法是在过滤器中生成 traceId，并写入 `MDC`，日志格式中再输出该字段。

文件位置：`src/main/java/io/github/atengk/common/web/filter/TraceIdFilter.java`

该过滤器用于为每次请求生成 traceId，并将其写入响应头和日志 MDC。

```java
package io.github.atengk.common.web.filter;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 请求链路追踪过滤器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
public class TraceIdFilter extends OncePerRequestFilter {

    /**
     * traceId 请求头名称
     */
    private static final String TRACE_ID_HEADER = "X-Trace-Id";

    /**
     * MDC 中 traceId 的键
     */
    private static final String TRACE_ID_KEY = "traceId";

    /**
     * 执行请求过滤
     *
     * @param request     HTTP 请求对象
     * @param response    HTTP 响应对象
     * @param filterChain 过滤器链
     * @throws ServletException Servlet 异常
     * @throws IOException      IO 异常
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long startTime = System.currentTimeMillis();
        String traceId = request.getHeader(TRACE_ID_HEADER);

        if (StrUtil.isBlank(traceId)) {
            traceId = IdUtil.fastSimpleUUID();
        }

        MDC.put(TRACE_ID_KEY, traceId);
        response.setHeader(TRACE_ID_HEADER, traceId);

        try {
            log.info("请求开始，method={}，uri={}，query={}",
                    request.getMethod(), request.getRequestURI(), request.getQueryString());

            filterChain.doFilter(request, response);
        } finally {
            long cost = System.currentTimeMillis() - startTime;
            log.info("请求结束，method={}，uri={}，status={}，cost={}ms",
                    request.getMethod(), request.getRequestURI(), response.getStatus(), cost);
            MDC.remove(TRACE_ID_KEY);
        }
    }
}
```

文件位置：`src/main/java/io/github/atengk/common/web/config/WebFilterConfig.java`

该配置类用于注册 traceId 过滤器。

```java
package io.github.atengk.common.web.config;

import io.github.atengk.common.web.filter.TraceIdFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Web 过滤器配置
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Configuration
public class WebFilterConfig {

    /**
     * 注册链路追踪过滤器
     *
     * @return 过滤器注册对象
     */
    @Bean
    public FilterRegistrationBean<TraceIdFilter> traceIdFilterRegistration() {
        FilterRegistrationBean<TraceIdFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new TraceIdFilter());
        registrationBean.addUrlPatterns("/*");
        registrationBean.setOrder(1);
        return registrationBean;
    }
}
```

文件位置：`src/main/resources/application.yml`

日志格式中需要输出 MDC 中的 traceId，便于按请求链路检索日志。

```yaml
logging:
  pattern:
    # 控制台日志格式：输出时间、级别、traceId、线程、日志名和消息
    console: "%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%X{traceId}] [%thread] %logger{36} - %msg%n"
```

请求上下文日志示例：

```text
2026-05-06 10:30:15.123 INFO  [9f2b7e0f8c6a4a46a6f5d3d2d8a6d7c1] [http-nio-8080-exec-1] i.g.a.common.web.filter.TraceIdFilter - 请求开始，method=POST，uri=/api/users，query=null
2026-05-06 10:30:15.145 WARN  [9f2b7e0f8c6a4a46a6f5d3d2d8a6d7c1] [http-nio-8080-exec-1] i.g.a.common.web.handler.GlobalExceptionHandler - RequestBody 参数校验失败，path=/api/users，errors=[...]
2026-05-06 10:30:15.150 INFO  [9f2b7e0f8c6a4a46a6f5d3d2d8a6d7c1] [http-nio-8080-exec-1] i.g.a.common.web.filter.TraceIdFilter - 请求结束，method=POST，uri=/api/users，status=200，cost=27ms
```

如果项目接入网关、Nginx 或微服务调用链，建议优先透传上游传入的 `X-Trace-Id`，没有传入时再由当前服务生成。这样可以保持跨系统调用链路一致。

## 接口返回示例

接口返回示例用于展示不同异常场景下的统一响应结果。前端或第三方调用方可以根据 `code` 判断错误类型，根据 `message` 展示错误信息，根据 `traceId` 向后端反馈问题排查线索。

以下示例基于前文定义的统一响应结构。

### 业务异常返回示例

业务异常通常由 Service 层主动抛出，例如查询用户时用户不存在。

请求示例：

```bash
curl -X GET 'http://localhost:8080/api/users/1001'
```

业务代码示例：

```java
if (ObjectUtil.isNull(user)) {
    log.warn("查询用户失败，用户不存在，userId={}", userId);
    throw new BusinessException(ResultCode.USER_NOT_FOUND);
}
```

返回示例：

```json
{
  "code": "B0101",
  "message": "用户不存在",
  "timestamp": 1778054400000,
  "path": "/api/users/1001",
  "traceId": "9f2b7e0f8c6a4a46a6f5d3d2d8a6d7c1"
}
```

该响应表示请求参数格式正确，但业务数据不满足规则。前端可以根据 `code` 做精确提示，例如提示用户刷新页面、重新选择数据或检查输入条件。

### 参数校验异常返回示例

参数校验异常通常由 `@Valid`、`@Validated` 和 Jakarta Validation 注解触发，例如用户名为空、邮箱格式错误、密码长度不足。

请求示例：

```bash
curl -X POST 'http://localhost:8080/api/users' \
  -H 'Content-Type: application/json' \
  -d '{
    "username": "",
    "email": "test",
    "password": "123"
  }'
```

DTO 校验规则示例：

```java
@NotBlank(message = "用户名不能为空")
@Size(max = 30, message = "用户名长度不能超过30个字符")
private String username;

@NotBlank(message = "邮箱不能为空")
@Email(message = "邮箱格式不正确")
private String email;

@NotBlank(message = "密码不能为空")
@Size(min = 6, max = 20, message = "密码长度必须在6到20个字符之间")
private String password;
```

返回示例：

```json
{
  "code": "A0401",
  "message": "参数校验失败",
  "data": [
    {
      "field": "username",
      "message": "用户名不能为空",
      "rejectedValue": ""
    },
    {
      "field": "email",
      "message": "邮箱格式不正确",
      "rejectedValue": "test"
    },
    {
      "field": "password",
      "message": "密码长度必须在6到20个字符之间",
      "rejectedValue": "123"
    }
  ],
  "timestamp": 1778054400000,
  "path": "/api/users",
  "traceId": "a84a2bb95c0643388c73f3bd0c1d89d4"
}
```

参数校验异常响应建议返回字段级错误详情。前端可以根据 `field` 将错误提示展示到对应表单项上，也可以将 `message` 汇总展示在页面顶部。

### 系统异常返回示例

系统异常通常是非预期异常，例如空指针、数据库访问失败、配置错误或第三方依赖不可用。此类异常对外不应返回内部细节，只返回统一系统错误提示。

请求示例：

```bash
curl -X GET 'http://localhost:8080/api/test/error'
```

Controller 示例：

```java
/**
 * 触发系统异常
 *
 * @return 响应结果
 */
@GetMapping("/error")
public Result<Boolean> error() {
    log.info("准备触发系统异常");
    throw new NullPointerException("模拟空指针异常");
}
```

返回示例：

```json
{
  "code": "C0001",
  "message": "系统内部异常",
  "timestamp": 1778054400000,
  "path": "/api/test/error",
  "traceId": "c99fa3343de64389bb421466d7da1eb0"
}
```

服务端日志示例：

```text
2026-05-06 10:35:20.123 ERROR [c99fa3343de64389bb421466d7da1eb0] [http-nio-8080-exec-5] i.g.a.common.web.handler.GlobalExceptionHandler - 系统未知异常，path=/api/test/error，message=模拟空指针异常
java.lang.NullPointerException: 模拟空指针异常
    at io.github.atengk.test.controller.TestController.error(TestController.java:35)
    ...
```

客户端只需要拿到统一错误提示和 `traceId`；后端人员可以通过 `traceId` 在日志系统中检索完整堆栈。

## 测试与验证

测试与验证用于确认全局异常处理、参数校验、日志记录和统一响应结构是否符合预期。建议至少覆盖正常请求、业务异常、参数校验异常和未知异常四类场景。

测试前需要确认项目已经引入以下能力：

| 能力           | 说明                                        |
| -------------- | ------------------------------------------- |
| Web 模块       | 已引入 `spring-boot-starter-web`            |
| 参数校验模块   | 已引入 `spring-boot-starter-validation`     |
| 全局异常处理类 | 已创建 `GlobalExceptionHandler`             |
| 统一响应对象   | 已创建 `Result`                             |
| 错误码枚举     | 已创建 `ResultCode`                         |
| traceId 过滤器 | 已创建并注册 `TraceIdFilter`                |
| 日志格式       | 已在 `application.yml` 中配置 `%X{traceId}` |

可以先启动项目：

```bash
mvn spring-boot:run
```

如果是 Gradle 项目，可以使用：

```bash
./gradlew bootRun
```

启动成功后，使用 `curl`、Postman、Apifox 或 Swagger 接口文档进行验证。

### 正常请求验证

正常请求验证用于确认接口在无异常时能够返回统一成功结构。

测试接口示例：

```java
/**
 * 查询用户详情
 *
 * @param id 用户ID
 * @return 用户详情
 */
@GetMapping("/{id}")
public Result<String> getUser(@PathVariable @Min(value = 1, message = "用户ID必须大于0") Long id) {
    log.info("查询用户详情，id={}", id);
    return Result.success("admin");
}
```

请求命令：

```bash
curl -X GET 'http://localhost:8080/api/users/1'
```

预期响应：

```json
{
  "code": "00000",
  "message": "操作成功",
  "data": "admin",
  "timestamp": 1778054400000,
  "traceId": "4e5f6469a83e4f2b9f09dc66d2e2177c"
}
```

验证重点：

| 验证项    | 预期结果                     |
| --------- | ---------------------------- |
| `code`    | 返回 `00000`                 |
| `message` | 返回 `操作成功`              |
| `data`    | 返回接口业务数据             |
| `traceId` | 存在且可在日志中检索         |
| 日志      | 能看到请求开始和请求结束日志 |

### 业务异常验证

业务异常验证用于确认 Service 层主动抛出的 `BusinessException` 能够被全局异常处理器接管，并返回业务错误码。

测试接口示例：

```java
/**
 * 查询不存在的用户
 *
 * @param id 用户ID
 * @return 用户详情
 */
@GetMapping("/business-error/{id}")
public Result<String> businessError(@PathVariable Long id) {
    log.info("查询用户，id={}", id);
    throw new BusinessException(ResultCode.USER_NOT_FOUND);
}
```

请求命令：

```bash
curl -X GET 'http://localhost:8080/api/users/business-error/1001'
```

预期响应：

```json
{
  "code": "B0101",
  "message": "用户不存在",
  "timestamp": 1778054400000,
  "path": "/api/users/business-error/1001",
  "traceId": "c7f06cd37512410f98fcb4f8f7e9a2ac"
}
```

验证重点：

| 验证项    | 预期结果                            |
| --------- | ----------------------------------- |
| 异常类型  | 被 `BusinessException` 处理方法接管 |
| `code`    | 返回业务错误码，例如 `B0101`        |
| `message` | 返回业务错误信息                    |
| 日志级别  | 使用 `warn`                         |
| 堆栈输出  | 一般不输出完整堆栈                  |

业务异常验证通过后，说明业务代码可以通过抛出业务异常的方式统一表达业务失败，不需要在每个 Controller 中手动拼接失败响应。

### 参数校验异常验证

参数校验异常验证用于确认 `@Valid`、`@Validated`、`@RequestBody`、`@PathVariable` 和 `@RequestParam` 的校验规则能够生效，并由全局异常处理器返回字段级错误详情。

`RequestBody` 校验请求：

```bash
curl -X POST 'http://localhost:8080/api/users' \
  -H 'Content-Type: application/json' \
  -d '{
    "username": "",
    "email": "test",
    "password": "123"
  }'
```

预期响应：

```json
{
  "code": "A0401",
  "message": "参数校验失败",
  "data": [
    {
      "field": "username",
      "message": "用户名不能为空",
      "rejectedValue": ""
    },
    {
      "field": "email",
      "message": "邮箱格式不正确",
      "rejectedValue": "test"
    },
    {
      "field": "password",
      "message": "密码长度必须在6到20个字符之间",
      "rejectedValue": "123"
    }
  ],
  "timestamp": 1778054400000,
  "path": "/api/users",
  "traceId": "e43f46bb13a44af2830f7a32a02d1a8f"
}
```

`PathVariable` 校验请求：

```bash
curl -X GET 'http://localhost:8080/api/users/0'
```

预期响应：

```json
{
  "code": "A0401",
  "message": "参数校验失败",
  "data": [
    {
      "field": "id",
      "message": "用户ID必须大于0",
      "rejectedValue": 0
    }
  ],
  "timestamp": 1778054400000,
  "path": "/api/users/0",
  "traceId": "a504aacbd0d44b40b4da2246b3a40fd2"
}
```

`RequestParam` 校验请求：

```bash
curl -X GET 'http://localhost:8080/api/users/page?keyword=&pageNum=0&pageSize=200'
```

预期响应：

```json
{
  "code": "A0401",
  "message": "参数校验失败",
  "data": [
    {
      "field": "keyword",
      "message": "搜索关键字不能为空",
      "rejectedValue": ""
    },
    {
      "field": "pageNum",
      "message": "页码必须大于0",
      "rejectedValue": 0
    },
    {
      "field": "pageSize",
      "message": "每页条数不能超过100",
      "rejectedValue": 200
    }
  ],
  "timestamp": 1778054400000,
  "path": "/api/users/page",
  "traceId": "e3b9b1d761964684862f99b377f46f1e"
}
```

验证重点：

| 验证项               | 预期结果                               |
| -------------------- | -------------------------------------- |
| `@RequestBody` 校验  | 触发 `MethodArgumentNotValidException` |
| `@PathVariable` 校验 | 触发 `ConstraintViolationException`    |
| `@RequestParam` 校验 | 触发 `ConstraintViolationException`    |
| `data` 字段          | 返回字段级错误列表                     |
| `field` 字段         | 能定位具体参数名                       |
| `message` 字段       | 返回注解中配置的错误提示               |

如果 `PathVariable` 或 `RequestParam` 校验没有生效，优先检查 Controller 类上是否添加了 `@Validated`。

### 未知异常验证

未知异常验证用于确认系统未显式处理的异常能够被兜底处理方法接管，并返回统一系统错误响应，同时服务端日志记录完整堆栈。

测试接口示例：

```java
/**
 * 触发未知异常
 *
 * @return 响应结果
 */
@GetMapping("/unknown-error")
public Result<Boolean> unknownError() {
    log.info("准备触发未知异常");
    String value = null;
    value.length();
    return Result.success(Boolean.TRUE);
}
```

请求命令：

```bash
curl -X GET 'http://localhost:8080/api/test/unknown-error'
```

预期响应：

```json
{
  "code": "C0001",
  "message": "系统内部异常",
  "timestamp": 1778054400000,
  "path": "/api/test/unknown-error",
  "traceId": "f9b1fa4ff96e49b1ad374e79a2e2ac01"
}
```

预期日志：

```text
2026-05-06 10:42:18.331 ERROR [f9b1fa4ff96e49b1ad374e79a2e2ac01] [http-nio-8080-exec-7] i.g.a.common.web.handler.GlobalExceptionHandler - 系统未知异常，path=/api/test/unknown-error，message=Cannot invoke "String.length()" because "value" is null
java.lang.NullPointerException: Cannot invoke "String.length()" because "value" is null
    at io.github.atengk.test.controller.TestController.unknownError(TestController.java:42)
    ...
```

验证重点：

| 验证项       | 预期结果                                     |
| ------------ | -------------------------------------------- |
| 异常处理方法 | 被 `@ExceptionHandler(Exception.class)` 接管 |
| 响应错误码   | 返回 `C0001`                                 |
| 响应消息     | 不暴露空指针、SQL、堆栈等内部细节            |
| 日志级别     | 使用 `error`                                 |
| 日志内容     | 包含完整异常堆栈                             |
| traceId      | 响应和日志中的 traceId 一致                  |

未知异常验证通过后，说明系统具备基础兜底能力。线上出现未预期异常时，调用方可以提供 `traceId`，后端可以通过日志平台快速定位完整异常链路。