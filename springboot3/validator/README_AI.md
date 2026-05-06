# Spring Boot Validation 校验

Spring Boot Validation 用于对请求参数、业务对象、方法入参和返回值进行声明式校验。它基于 Jakarta Validation 规范，常见实现是 Hibernate Validator，在 Spring Boot 3 项目中主要通过 `spring-boot-starter-validation` 引入。

## 功能概述

本节说明 Validation 在后端应用中的定位、适用场景，以及 Spring Boot 3 相比 Spring Boot 2 在依赖和包名上的主要变化。

### Validation 校验定位

Validation 的核心定位是“声明式参数校验”。开发者通过注解描述字段、对象、方法参数或返回值的约束规则，例如不能为空、长度范围、数值范围、邮箱格式、日期范围等，由框架在运行时统一执行校验逻辑。

在 Spring Boot Web 项目中，Validation 通常位于 Controller 入参处理阶段，用于在业务逻辑执行之前拦截非法参数。这样可以避免在 Service 中重复编写大量 `if` 判断，让参数合法性校验和业务规则处理分离。

Jakarta Validation 的约束通常通过 Java 注解声明，支持字段约束、属性约束、容器元素约束和类级别约束；Hibernate Validator 文档也明确说明约束可以作用于字段、属性、容器元素和类等不同层级。([JBoss 文档](https://docs.jboss.org/hibernate/validator/8.0/reference/en-US/html_single/))

在 Spring Boot 中，Validation 主要解决以下问题：

| 定位         | 说明                                                         |
| ------------ | ------------------------------------------------------------ |
| 请求参数校验 | 校验 `@RequestBody`、`@RequestParam`、`@PathVariable` 等入参 |
| 对象字段校验 | 校验 DTO、Query、Command、Form 等对象字段                    |
| 方法参数校验 | 校验 Service 或组件方法的入参和返回值                        |
| 统一异常处理 | 将校验失败信息转换为统一响应结构                             |
| 降低重复代码 | 用注解替代分散的手工参数判断                                 |

### 常见使用场景

Validation 适合放在“参数进入系统边界”的位置，尤其是 Controller 层和对外接口层。对于新增、修改、查询、批量提交等接口，应优先通过 Validation 完成基础合法性校验，再进入业务逻辑处理。

常见使用场景如下：

| 场景       | 示例                               | 常用注解                           |
| ---------- | ---------------------------------- | ---------------------------------- |
| 新增数据   | 新增用户、创建订单、提交表单       | `@NotBlank`、`@NotNull`、`@Size`   |
| 修改数据   | 修改用户资料、更新状态             | `@NotNull`、`@Positive`、分组校验  |
| 查询参数   | 分页查询、条件筛选                 | `@Min`、`@Max`、`@Pattern`         |
| 路径变量   | `/users/{id}`、`/orders/{orderNo}` | `@Positive`、`@NotBlank`           |
| 批量提交   | 批量删除、批量导入                 | `@NotEmpty`、`List<@NotNull Long>` |
| 嵌套对象   | 用户地址、订单明细、商品规格       | `@Valid`                           |
| 方法级校验 | Service 方法入参、返回值约束       | `@Validated`、约束注解             |

对于方法级校验，Spring Boot 在 classpath 中存在 Bean Validation 实现时会自动启用相关能力；目标类需要使用 `@Validated`，这样 Spring 才会扫描方法参数和返回值上的 `jakarta.validation` 约束。([Home](https://docs.spring.io/spring-boot/3.5/reference/io/validation.html))

### Spring Boot 3 中的依赖变化

Spring Boot 3 基于 Spring Framework 6，并迁移到 Jakarta EE 体系。与 Spring Boot 2.x 中大量使用 `javax.*` 包名不同，Spring Boot 3 中的 Validation 注解应使用 `jakarta.validation.*` 包名，例如 `jakarta.validation.Valid`、`jakarta.validation.constraints.NotBlank`。Spring Boot 3 迁移指南明确指出，Jakarta EE 现在使用 `jakarta` 包而不是 `javax` 包，升级后需要同步调整 import 语句。([GitHub](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-3.0-Migration-Guide))

需要注意的是，`spring-boot-starter-web` 和 `spring-boot-starter-webflux` 从 Spring Boot 2.3 开始不再默认依赖 Validation starter。也就是说，即使是 Web 项目，只要使用参数校验，也建议显式引入 `spring-boot-starter-validation`。官方 2.3 Release Notes 明确说明 Web 和 WebFlux starters 不再默认依赖 validation starter，并给出了 Maven 与 Gradle 的手动引入方式。([GitHub](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-2.3-Release-Notes))

Spring Boot 3 项目中推荐使用以下方式：

```java
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
```

不推荐继续使用以下旧包名：

```java
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
```

## 环境与依赖配置

本节给出 Spring Boot 3 项目中启用 Validation 的基础依赖配置。一般情况下，只需要引入 `spring-boot-starter-validation`，由 Spring Boot 统一管理 Hibernate Validator、Jakarta Validation API 等相关依赖版本。

### Maven 依赖配置

Maven 项目建议使用 `spring-boot-starter-parent` 管理依赖版本。业务模块中只需要声明 starter，不需要手动指定 Hibernate Validator 的版本。

以下配置放在项目根目录的 `pom.xml` 中：

```xml
<project>
    <parent>
        <!-- Spring Boot 父工程，统一管理 starter 和三方依赖版本 -->
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.5.14</version>
        <relativePath/>
    </parent>

    <dependencies>
        <!-- Web 项目常用依赖，提供 Spring MVC、内嵌容器、JSON 转换等能力 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Validation 校验依赖，提供 Jakarta Validation API 和 Hibernate Validator 实现 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <!-- Lombok 用于简化 DTO、VO、实体类代码，非 Validation 必需 -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
    </dependencies>
</project>
```

如果项目没有使用 `spring-boot-starter-parent`，而是使用公司统一父工程，可以通过 Spring Boot BOM 方式管理版本：

```xml
<dependencyManagement>
    <dependencies>
        <!-- 使用 Spring Boot BOM 统一管理依赖版本 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-dependencies</artifactId>
            <version>3.5.14</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <!-- Validation 校验依赖，版本由 Spring Boot BOM 管理 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
</dependencies>
```

Spring Boot starter 本身就是一组便捷依赖描述，用于一次性引入相关技术栈所需依赖，并保持版本一致性；因此一般不建议在业务项目中单独覆盖 `hibernate-validator` 版本。([Home](https://docs.spring.io/spring-boot/reference/using/build-systems.html))

### Gradle 依赖配置

Gradle 项目推荐使用 Spring Boot Gradle Plugin，由插件和依赖管理机制统一处理 starter 版本。

如果项目使用 Kotlin DSL，配置文件为 `build.gradle.kts`：

```kotlin
plugins {
    // Spring Boot Gradle 插件，负责打包、运行和依赖版本管理
    id("org.springframework.boot") version "3.5.14"

    // Spring 依赖管理插件，配合 Spring Boot 管理依赖版本
    id("io.spring.dependency-management") version "1.1.7"

    // Java 项目插件
    java
}

dependencies {
    // Web 项目常用依赖，提供 Spring MVC、内嵌容器、JSON 转换等能力
    implementation("org.springframework.boot:spring-boot-starter-web")

    // Validation 校验依赖，提供 Jakarta Validation API 和 Hibernate Validator 实现
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // Lombok 用于简化 DTO、VO、实体类代码，非 Validation 必需
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
}
```

如果项目使用 Groovy DSL，配置文件为 `build.gradle`：

```groovy
plugins {
    // Spring Boot Gradle 插件，负责打包、运行和依赖版本管理
    id 'org.springframework.boot' version '3.5.14'

    // Spring 依赖管理插件，配合 Spring Boot 管理依赖版本
    id 'io.spring.dependency-management' version '1.1.7'

    // Java 项目插件
    id 'java'
}

dependencies {
    // Web 项目常用依赖，提供 Spring MVC、内嵌容器、JSON 转换等能力
    implementation 'org.springframework.boot:spring-boot-starter-web'

    // Validation 校验依赖，提供 Jakarta Validation API 和 Hibernate Validator 实现
    implementation 'org.springframework.boot:spring-boot-starter-validation'

    // Lombok 用于简化 DTO、VO、实体类代码，非 Validation 必需
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
}
```

依赖添加完成后，刷新 Gradle 项目即可使用 `jakarta.validation` 下的校验注解。

### Jakarta Validation 说明

Jakarta Validation 是 Java 生态中用于声明和执行 Bean 校验的标准规范。它定义的是 API 和约束模型，具体执行通常由 Hibernate Validator 这类实现完成。Hibernate Validator 文档中也说明，校验失败时会返回 `ConstraintViolation` 集合，校验成功时集合为空。([JBoss 文档](https://docs.jboss.org/hibernate/validator/8.0/reference/en-US/html_single/))

在 Spring Boot 3 中需要重点区分以下概念：

| 概念                             | 说明                                                         |
| -------------------------------- | ------------------------------------------------------------ |
| Jakarta Validation               | 校验规范，定义注解、校验接口和约束模型                       |
| Hibernate Validator              | Jakarta Validation 的常用实现                                |
| Spring Validation                | Spring 对校验体系的集成，例如 `@Validated`、异常绑定、消息解析 |
| `spring-boot-starter-validation` | Spring Boot 提供的校验 starter，用于快速引入校验能力         |

常用包名如下：

```java
// 对象级联校验
import jakarta.validation.Valid;

// 约束注解
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;

// Spring 提供的分组校验和方法级校验支持
import org.springframework.validation.annotation.Validated;
```

实际开发中可以按以下规则使用：

| 注解            | 主要用途                                 |
| --------------- | ---------------------------------------- |
| `@Valid`        | 触发对象字段校验和嵌套对象校验           |
| `@Validated`    | 支持 Spring 方法级校验和分组校验         |
| `@NotNull`      | 校验对象不能为 `null`                    |
| `@NotBlank`     | 校验字符串不能为 `null`、空串或纯空白    |
| `@NotEmpty`     | 校验字符串、集合、数组不能为 `null` 或空 |
| `@Size`         | 校验字符串、集合、数组长度范围           |
| `@Min` / `@Max` | 校验数值范围                             |
| `@Email`        | 校验邮箱格式                             |
| `@Pattern`      | 使用正则表达式校验字符串                 |

Spring Boot 会使用应用中的 `MessageSource` 解析校验消息中的参数，因此可以结合 `messages.properties` 维护统一的中文校验提示；如需自定义 `ValidatorFactory` 构建过程，可以定义 `ValidationConfigurationCustomizer` Bean。([Home](https://docs.spring.io/spring-boot/3.5/reference/io/validation.html))



## 基础参数校验

本节介绍 Spring Boot 3 中最常见的参数校验方式，包括请求体对象、URL 查询参数、路径变量和集合参数校验。示例基于前文已引入的 `spring-boot-starter-validation`，校验注解统一使用 `jakarta.validation` 包。

### 请求体对象校验

请求体对象校验主要用于 `POST`、`PUT`、`PATCH` 等接口，通常配合 `@RequestBody` 和 `@Valid` 使用。客户端提交 JSON 数据后，Spring 会将 JSON 反序列化为 Java 对象，并根据对象字段上的校验注解执行校验。

文件位置：`src/main/java/io/github/atengk/validation/dto/UserCreateRequest.java`

以下 DTO 用于演示新增用户时的请求体字段校验。

```java
package io.github.atengk.validation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

/**
 * 用户新增请求参数
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
public class UserCreateRequest {

    /**
     * 用户名不能为空，长度限制为 4 到 20 位
     */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 4, max = 20, message = "用户名长度必须在 4 到 20 位之间")
    private String username;

    /**
     * 昵称不能为空
     */
    @NotBlank(message = "昵称不能为空")
    @Size(max = 30, message = "昵称长度不能超过 30 位")
    private String nickname;

    /**
     * 密码不能为空，长度限制为 8 到 32 位
     */
    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 32, message = "密码长度必须在 8 到 32 位之间")
    private String password;

    /**
     * 邮箱格式校验
     */
    @Email(message = "邮箱格式不正确")
    private String email;

    /**
     * 年龄范围校验
     */
    @NotNull(message = "年龄不能为空")
    @Min(value = 1, message = "年龄不能小于 1")
    @Max(value = 120, message = "年龄不能大于 120")
    private Integer age;

    /**
     * 地址对象嵌套校验
     */
    @Valid
    @NotNull(message = "地址信息不能为空")
    private AddressRequest address;

    /**
     * 联系方式集合嵌套校验
     */
    @Valid
    @Size(max = 5, message = "联系方式最多填写 5 个")
    private List<ContactRequest> contacts;
}
```

文件位置：`src/main/java/io/github/atengk/validation/dto/AddressRequest.java`

以下 DTO 用于演示嵌套地址对象校验。

```java
package io.github.atengk.validation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 地址请求参数
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
public class AddressRequest {

    @NotBlank(message = "省份不能为空")
    private String province;

    @NotBlank(message = "城市不能为空")
    private String city;

    @NotBlank(message = "详细地址不能为空")
    @Size(max = 100, message = "详细地址长度不能超过 100 位")
    private String detail;
}
```

文件位置：`src/main/java/io/github/atengk/validation/dto/ContactRequest.java`

以下 DTO 用于演示集合元素中的嵌套对象校验。

```java
package io.github.atengk.validation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 联系方式请求参数
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
public class ContactRequest {

    @NotBlank(message = "联系人姓名不能为空")
    private String name;

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;
}
```

文件位置：`src/main/java/io/github/atengk/validation/controller/UserValidationController.java`

以下 Controller 演示请求体对象校验的入口写法。

```java
package io.github.atengk.validation.controller;

import io.github.atengk.validation.dto.UserCreateRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 用户参数校验示例接口
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@RestController
@RequestMapping("/api/users")
public class UserValidationController {

    /**
     * 新增用户
     *
     * @param request 用户新增请求参数
     * @return 处理结果
     */
    @PostMapping
    public String createUser(@Valid @RequestBody UserCreateRequest request) {
        log.info("新增用户参数校验通过，username={}", request.getUsername());
        return "新增用户成功";
    }
}
```

请求示例：

```bash
curl -X POST "http://localhost:8080/api/users" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "ateng",
    "nickname": "阿腾",
    "password": "12345678",
    "email": "ateng@example.com",
    "age": 25,
    "address": {
      "province": "浙江省",
      "city": "杭州市",
      "detail": "西湖区示例路 100 号"
    },
    "contacts": [
      {
        "name": "张三",
        "phone": "13800138000"
      }
    ]
  }'
```

请求体校验的核心点是：`@RequestBody` 负责接收 JSON，`@Valid` 负责触发对象字段校验。如果缺少 `@Valid`，DTO 字段上的校验注解通常不会生效。

### URL 参数校验

URL 参数校验主要用于 `GET` 查询接口，例如分页查询、条件筛选、状态筛选等。对于简单参数，可以直接在 `@RequestParam` 参数上添加校验注解。

使用 `@RequestParam`、`@PathVariable` 这类方法参数校验时，Controller 类上建议添加 `@Validated`，用于启用方法参数级别的校验。

文件位置：`src/main/java/io/github/atengk/validation/controller/UserQueryController.java`

以下 Controller 演示 URL 查询参数校验。

```java
package io.github.atengk.validation.controller;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 用户查询参数校验示例接口
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
     * 分页查询用户
     *
     * @param pageNum  当前页码
     * @param pageSize 每页条数
     * @param status   用户状态
     * @return 查询结果
     */
    @GetMapping("/page")
    public String pageUsers(
            @RequestParam(defaultValue = "1")
            @Min(value = 1, message = "当前页码不能小于 1")
            Integer pageNum,

            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "每页条数不能小于 1")
            @Max(value = 100, message = "每页条数不能大于 100")
            Integer pageSize,

            @RequestParam(required = false)
            @Pattern(regexp = "^(ENABLE|DISABLE)$", message = "用户状态只能是 ENABLE 或 DISABLE")
            String status) {

        log.info("分页查询用户参数校验通过，pageNum={}，pageSize={}，status={}", pageNum, pageSize, status);
        return "分页查询用户成功";
    }
}
```

请求示例：

```bash
curl "http://localhost:8080/api/users/page?pageNum=1&pageSize=10&status=ENABLE"
```

错误请求示例：

```bash
curl "http://localhost:8080/api/users/page?pageNum=0&pageSize=200&status=UNKNOWN"
```

对于查询参数较多的接口，也可以将参数封装成查询对象，并使用 `@Valid` 或 `@Validated` 触发校验。

文件位置：`src/main/java/io/github/atengk/validation/dto/UserPageQuery.java`

以下查询对象用于封装分页查询参数。

```java
package io.github.atengk.validation.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 用户分页查询参数
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
public class UserPageQuery {

    @Min(value = 1, message = "当前页码不能小于 1")
    private Integer pageNum = 1;

    @Min(value = 1, message = "每页条数不能小于 1")
    @Max(value = 100, message = "每页条数不能大于 100")
    private Integer pageSize = 10;

    @Pattern(regexp = "^(ENABLE|DISABLE)$", message = "用户状态只能是 ENABLE 或 DISABLE")
    private String status;
}
```

Controller 中使用查询对象接收 URL 参数：

```java
@GetMapping("/list")
public String listUsers(@Valid UserPageQuery query) {
    log.info("列表查询用户参数校验通过，pageNum={}，pageSize={}，status={}",
            query.getPageNum(), query.getPageSize(), query.getStatus());
    return "列表查询用户成功";
}
```

对象接收 URL 参数时，不需要添加 `@RequestBody`。Spring MVC 会根据 URL 查询参数自动绑定到对象字段中。

### 路径变量校验

路径变量校验用于校验 RESTful 风格接口中的路径参数，例如用户 ID、订单 ID、资源编码等。路径变量通常配合 `@PathVariable` 使用。

路径变量属于方法参数校验，因此 Controller 类上应添加 `@Validated`。

文件位置：`src/main/java/io/github/atengk/validation/controller/UserPathController.java`

以下 Controller 演示路径变量校验。

```java
package io.github.atengk.validation.controller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 用户路径变量校验示例接口
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/users")
public class UserPathController {

    /**
     * 根据用户 ID 查询用户
     *
     * @param id 用户 ID
     * @return 查询结果
     */
    @GetMapping("/{id}")
    public String getUser(
            @PathVariable
            @Positive(message = "用户 ID 必须是正整数")
            Long id) {

        log.info("根据用户 ID 查询用户，id={}", id);
        return "查询用户成功";
    }

    /**
     * 根据用户编码查询用户
     *
     * @param userCode 用户编码
     * @return 查询结果
     */
    @GetMapping("/code/{userCode}")
    public String getUserByCode(
            @PathVariable
            @NotBlank(message = "用户编码不能为空")
            @Pattern(regexp = "^U\\d{6}$", message = "用户编码格式必须为 U 加 6 位数字")
            String userCode) {

        log.info("根据用户编码查询用户，userCode={}", userCode);
        return "根据用户编码查询用户成功";
    }
}
```

请求示例：

```bash
curl "http://localhost:8080/api/users/1001"
curl "http://localhost:8080/api/users/code/U000001"
```

错误请求示例：

```bash
curl "http://localhost:8080/api/users/0"
curl "http://localhost:8080/api/users/code/ABC001"
```

路径变量常用校验包括 `@Positive`、`@NotBlank`、`@Pattern`。如果路径变量是数值 ID，优先使用 `@Positive`；如果是业务编码，优先使用 `@NotBlank` 和 `@Pattern`。

### 集合参数校验

集合参数校验主要用于批量删除、批量修改、批量新增等场景。集合校验需要区分两类规则：集合本身的规则和集合元素的规则。

集合本身的规则通常使用 `@NotEmpty`、`@Size`；集合元素的规则可以写在泛型位置，例如 `List<@Positive Long>`、`List<@Valid UserCreateRequest>`。

文件位置：`src/main/java/io/github/atengk/validation/controller/UserBatchController.java`

以下 Controller 演示 URL 集合参数和请求体集合参数校验。

```java
package io.github.atengk.validation.controller;

import io.github.atengk.validation.dto.UserCreateRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户批量参数校验示例接口
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/users")
public class UserBatchController {

    /**
     * 批量删除用户
     *
     * @param ids 用户 ID 集合
     * @return 处理结果
     */
    @DeleteMapping("/batch")
    public String batchDelete(
            @RequestParam
            @NotEmpty(message = "用户 ID 集合不能为空")
            @Size(max = 100, message = "单次最多删除 100 个用户")
            List<@Positive(message = "用户 ID 必须是正整数") Long> ids) {

        log.info("批量删除用户参数校验通过，数量={}", ids.size());
        return "批量删除用户成功";
    }

    /**
     * 批量新增用户
     *
     * @param requests 用户新增请求集合
     * @return 处理结果
     */
    @PostMapping("/batch")
    public String batchCreate(
            @RequestBody
            @NotEmpty(message = "用户集合不能为空")
            @Size(max = 50, message = "单次最多新增 50 个用户")
            List<@Valid UserCreateRequest> requests) {

        log.info("批量新增用户参数校验通过，数量={}", requests.size());
        return "批量新增用户成功";
    }
}
```

批量删除请求示例：

```bash
curl -X DELETE "http://localhost:8080/api/users/batch?ids=1,2,3"
```

批量新增请求示例：

```bash
curl -X POST "http://localhost:8080/api/users/batch" \
  -H "Content-Type: application/json" \
  -d '[
    {
      "username": "ateng001",
      "nickname": "阿腾001",
      "password": "12345678",
      "email": "ateng001@example.com",
      "age": 25,
      "address": {
        "province": "浙江省",
        "city": "杭州市",
        "detail": "西湖区示例路 100 号"
      }
    },
    {
      "username": "ateng002",
      "nickname": "阿腾002",
      "password": "12345678",
      "email": "ateng002@example.com",
      "age": 26,
      "address": {
        "province": "浙江省",
        "city": "宁波市",
        "detail": "鄞州区示例路 200 号"
      }
    }
  ]'
```

集合参数校验时需要注意：`List<@Positive Long>` 用于校验集合中的每一个 ID；`List<@Valid UserCreateRequest>` 用于校验集合中的每一个对象。如果只写 `@Valid List<UserCreateRequest>`，在部分场景下可能无法准确表达“校验每一个元素”的意图，推荐将 `@Valid` 写在泛型元素位置。

## 校验注解使用

本节介绍 Jakarta Validation 中常用的内置注解，并按字符串、数值、日期时间和嵌套对象进行分类说明。实际开发中应优先使用内置注解，只有内置注解无法满足业务规则时，再考虑自定义校验注解。

### 常用内置注解

常用内置注解可以覆盖大部分基础参数校验场景，包括空值校验、长度校验、范围校验、格式校验和时间校验。

| 注解               | 适用类型                  | 说明                                  |
| ------------------ | ------------------------- | ------------------------------------- |
| `@NotNull`         | 任意类型                  | 不能为 `null`                         |
| `@NotBlank`        | `String`                  | 不能为 `null`、空字符串或纯空白字符串 |
| `@NotEmpty`        | `String`、集合、数组、Map | 不能为 `null`，且长度或大小不能为 0   |
| `@Size`            | `String`、集合、数组、Map | 校验长度或大小范围                    |
| `@Min`             | 整数数值类型              | 最小值校验                            |
| `@Max`             | 整数数值类型              | 最大值校验                            |
| `@DecimalMin`      | 数值类型、字符串数值      | 最小值校验，适合金额、小数            |
| `@DecimalMax`      | 数值类型、字符串数值      | 最大值校验，适合金额、小数            |
| `@Positive`        | 数值类型                  | 必须为正数                            |
| `@PositiveOrZero`  | 数值类型                  | 必须为正数或 0                        |
| `@Negative`        | 数值类型                  | 必须为负数                            |
| `@NegativeOrZero`  | 数值类型                  | 必须为负数或 0                        |
| `@Email`           | `String`                  | 邮箱格式校验                          |
| `@Pattern`         | `String`                  | 正则表达式校验                        |
| `@Past`            | 日期时间类型              | 必须是过去时间                        |
| `@PastOrPresent`   | 日期时间类型              | 必须是过去或当前时间                  |
| `@Future`          | 日期时间类型              | 必须是未来时间                        |
| `@FutureOrPresent` | 日期时间类型              | 必须是未来或当前时间                  |
| `@Valid`           | 对象、集合元素            | 触发嵌套对象校验                      |

常用注解选择建议如下：

| 校验目标                 | 推荐注解                     |
| ------------------------ | ---------------------------- |
| 字符串必填               | `@NotBlank`                  |
| 集合必填                 | `@NotEmpty`                  |
| 对象必填                 | `@NotNull`                   |
| 字符串长度               | `@Size`                      |
| 整数范围                 | `@Min`、`@Max`               |
| 小数范围                 | `@DecimalMin`、`@DecimalMax` |
| ID 必须大于 0            | `@Positive`                  |
| 邮箱                     | `@Email`                     |
| 手机号、编码、枚举字符串 | `@Pattern`                   |
| 嵌套对象                 | `@Valid`                     |

### 字符串校验注解

字符串校验主要用于用户名、密码、手机号、邮箱、编码、名称、备注等字段。最常用的组合是 `@NotBlank`、`@Size`、`@Pattern` 和 `@Email`。

文件位置：`src/main/java/io/github/atengk/validation/dto/UserStringRuleRequest.java`

以下 DTO 演示常见字符串字段校验写法。

```java
package io.github.atengk.validation.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户字符串校验请求参数
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
public class UserStringRuleRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 4, max = 20, message = "用户名长度必须在 4 到 20 位之间")
    @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_]*$", message = "用户名必须以字母开头，只能包含字母、数字和下划线")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 32, message = "密码长度必须在 8 到 32 位之间")
    private String password;

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Email(message = "邮箱格式不正确")
    private String email;

    @Size(max = 200, message = "备注长度不能超过 200 位")
    private String remark;
}
```

字符串校验中，`@NotNull`、`@NotEmpty`、`@NotBlank` 容易混淆：

| 注解        | `null` | `""`   | `" "`  | 推荐场景                       |
| ----------- | ------ | ------ | ------ | ------------------------------ |
| `@NotNull`  | 不通过 | 通过   | 通过   | 对象、包装类型、日期           |
| `@NotEmpty` | 不通过 | 不通过 | 通过   | 集合、数组、Map、字符串非空    |
| `@NotBlank` | 不通过 | 不通过 | 不通过 | 用户名、名称、标题等字符串必填 |

对于大部分必填字符串字段，优先使用 `@NotBlank`，不要只使用 `@NotNull`。

### 数值校验注解

数值校验主要用于 ID、年龄、价格、库存、排序号、分页参数、比例、金额等字段。整数范围通常使用 `@Min`、`@Max`；金额和小数建议使用 `@DecimalMin`、`@DecimalMax`。

文件位置：`src/main/java/io/github/atengk/validation/dto/ProductRuleRequest.java`

以下 DTO 演示数值字段校验写法。

```java
package io.github.atengk.validation.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 商品数值校验请求参数
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
public class ProductRuleRequest {

    @NotNull(message = "商品 ID 不能为空")
    @Positive(message = "商品 ID 必须是正整数")
    private Long productId;

    @NotNull(message = "库存不能为空")
    @Min(value = 0, message = "库存不能小于 0")
    @Max(value = 999999, message = "库存不能大于 999999")
    private Integer stock;

    @NotNull(message = "销售价格不能为空")
    @DecimalMin(value = "0.01", message = "销售价格不能小于 0.01")
    @DecimalMax(value = "999999.99", message = "销售价格不能大于 999999.99")
    private BigDecimal salePrice;

    @NotNull(message = "排序号不能为空")
    @PositiveOrZero(message = "排序号不能小于 0")
    private Integer sort;
}
```

数值校验建议：

| 场景          | 推荐注解                                      |
| ------------- | --------------------------------------------- |
| 数据库主键 ID | `@NotNull` + `@Positive`                      |
| 分页页码      | `@Min(1)`                                     |
| 每页条数      | `@Min(1)` + `@Max(100)`                       |
| 库存          | `@Min(0)`                                     |
| 金额          | `@DecimalMin` + `@DecimalMax`                 |
| 排序号        | `@PositiveOrZero`                             |
| 折扣比例      | `@DecimalMin("0.00")` + `@DecimalMax("1.00")` |

金额字段建议使用 `BigDecimal`，不要使用 `double` 或 `float`。`@DecimalMin`、`@DecimalMax` 的 `value` 是字符串形式，可以避免小数精度表达问题。

### 日期时间校验注解

日期时间校验主要用于生日、开始时间、结束时间、预约时间、过期时间、发布时间等字段。Jakarta Validation 提供了 `@Past`、`@PastOrPresent`、`@Future`、`@FutureOrPresent` 等注解。

文件位置：`src/main/java/io/github/atengk/validation/dto/ActivityCreateRequest.java`

以下 DTO 演示日期时间字段校验写法。

```java
package io.github.atengk.validation.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 活动日期时间校验请求参数
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
public class ActivityCreateRequest {

    @NotNull(message = "用户生日不能为空")
    @Past(message = "用户生日必须是过去日期")
    private LocalDate birthday;

    @NotNull(message = "活动开始时间不能为空")
    @FutureOrPresent(message = "活动开始时间不能早于当前时间")
    private LocalDateTime startTime;

    @NotNull(message = "活动结束时间不能为空")
    @Future(message = "活动结束时间必须是未来时间")
    private LocalDateTime endTime;
}
```

日期时间校验注解说明：

| 注解               | 说明                   | 示例场景               |
| ------------------ | ---------------------- | ---------------------- |
| `@Past`            | 必须早于当前时间       | 生日、历史日期         |
| `@PastOrPresent`   | 必须早于或等于当前时间 | 创建日期、统计截止日期 |
| `@Future`          | 必须晚于当前时间       | 预约时间、过期时间     |
| `@FutureOrPresent` | 必须晚于或等于当前时间 | 开始时间、生效时间     |

需要注意，`@Future` 和 `@FutureOrPresent` 只能校验单个字段与当前时间的关系。如果要校验“结束时间必须大于开始时间”，通常需要使用自定义校验注解，或者在业务层进行组合规则校验。

### 嵌套对象校验

嵌套对象校验用于对象内部包含另一个对象或集合对象的场景。例如订单中包含收货地址、订单明细；用户中包含地址信息、联系方式列表。

触发嵌套校验的关键是 `@Valid`。如果父对象字段上没有添加 `@Valid`，子对象内部字段上的校验注解不会被递归执行。

文件位置：`src/main/java/io/github/atengk/validation/dto/OrderCreateRequest.java`

以下 DTO 演示订单创建时的嵌套对象校验。

```java
package io.github.atengk.validation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.util.List;

/**
 * 订单创建请求参数
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
public class OrderCreateRequest {

    @NotNull(message = "用户 ID 不能为空")
    @Positive(message = "用户 ID 必须是正整数")
    private Long userId;

    @Valid
    @NotNull(message = "收货地址不能为空")
    private OrderAddressRequest address;

    @Valid
    @NotEmpty(message = "订单明细不能为空")
    private List<OrderItemRequest> items;
}
```

文件位置：`src/main/java/io/github/atengk/validation/dto/OrderAddressRequest.java`

以下 DTO 用于订单收货地址校验。

```java
package io.github.atengk.validation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 订单收货地址请求参数
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
public class OrderAddressRequest {

    @NotBlank(message = "收货人姓名不能为空")
    private String receiverName;

    @NotBlank(message = "收货人手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "收货人手机号格式不正确")
    private String receiverPhone;

    @NotBlank(message = "详细地址不能为空")
    private String detailAddress;
}
```

文件位置：`src/main/java/io/github/atengk/validation/dto/OrderItemRequest.java`

以下 DTO 用于订单明细校验。

```java
package io.github.atengk.validation.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * 订单明细请求参数
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
public class OrderItemRequest {

    @NotNull(message = "商品 ID 不能为空")
    @Positive(message = "商品 ID 必须是正整数")
    private Long productId;

    @NotNull(message = "购买数量不能为空")
    @Min(value = 1, message = "购买数量不能小于 1")
    private Integer quantity;
}
```

文件位置：`src/main/java/io/github/atengk/validation/controller/OrderController.java`

以下 Controller 演示提交订单时触发嵌套对象校验。

```java
package io.github.atengk.validation.controller;

import io.github.atengk.validation.dto.OrderCreateRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 订单参数校验示例接口
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    /**
     * 创建订单
     *
     * @param request 订单创建请求参数
     * @return 处理结果
     */
    @PostMapping
    public String createOrder(@Valid @RequestBody OrderCreateRequest request) {
        log.info("创建订单参数校验通过，userId={}，商品明细数量={}",
                request.getUserId(), request.getItems().size());
        return "创建订单成功";
    }
}
```

请求示例：

```bash
curl -X POST "http://localhost:8080/api/orders" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1001,
    "address": {
      "receiverName": "张三",
      "receiverPhone": "13800138000",
      "detailAddress": "浙江省杭州市西湖区示例路 100 号"
    },
    "items": [
      {
        "productId": 2001,
        "quantity": 2
      },
      {
        "productId": 2002,
        "quantity": 1
      }
    ]
  }'
```

嵌套对象校验常见写法如下：

```java
// 单个嵌套对象校验
@Valid
@NotNull(message = "地址信息不能为空")
private OrderAddressRequest address;

// 集合中的嵌套对象校验
@Valid
@NotEmpty(message = "订单明细不能为空")
private List<OrderItemRequest> items;

// 更明确的集合元素校验写法
@NotEmpty(message = "订单明细不能为空")
private List<@Valid OrderItemRequest> items;
```

实际开发中，推荐对复杂对象统一使用 `@Valid` 触发级联校验；对集合参数同时校验集合本身和集合元素，避免出现“集合不为空，但集合内对象字段非法”的问题。



## 分组校验

分组校验用于解决“同一个 DTO 在不同业务场景下校验规则不同”的问题。例如新增用户时不需要传 `id`，修改用户时必须传 `id`；新增时密码必填，修改基础资料时密码可以不传。

### 分组接口定义

分组接口本身不需要定义方法，只作为校验规则的标识。通常会将分组接口放在统一包下，便于 DTO 和 Controller 复用。

文件位置：`src/main/java/io/github/atengk/validation/group/CreateGroup.java`

```java
package io.github.atengk.validation.group;

/**
 * 新增场景校验分组
 *
 * @author Ateng
 * @since 2026-05-06
 */
public interface CreateGroup {
}
```

文件位置：`src/main/java/io/github/atengk/validation/group/UpdateGroup.java`

```java
package io.github.atengk.validation.group;

/**
 * 修改场景校验分组
 *
 * @author Ateng
 * @since 2026-05-06
 */
public interface UpdateGroup {
}
```

也可以将多个分组接口集中放在一个类中，但在中大型项目中，更推荐独立接口文件，命名清晰，便于复用和维护。

### 新增与修改场景区分

同一个请求对象可以通过 `groups` 属性声明不同场景下的校验规则。下面示例中，`id` 在新增时必须为空，在修改时必须存在；`password` 在新增时必填，修改时不强制传入。

文件位置：`src/main/java/io/github/atengk/validation/dto/UserSaveRequest.java`

以下 DTO 演示新增和修改共用一个请求对象时的分组校验写法。

```java
package io.github.atengk.validation.dto;

import io.github.atengk.validation.group.CreateGroup;
import io.github.atengk.validation.group.UpdateGroup;
import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * 用户保存请求参数
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
public class UserSaveRequest {

    @Null(message = "新增用户时 ID 必须为空", groups = CreateGroup.class)
    @NotNull(message = "修改用户时 ID 不能为空", groups = UpdateGroup.class)
    @Positive(message = "用户 ID 必须是正整数", groups = UpdateGroup.class)
    private Long id;

    @NotBlank(message = "用户名不能为空", groups = CreateGroup.class)
    @Size(min = 4, max = 20, message = "用户名长度必须在 4 到 20 位之间",
            groups = {CreateGroup.class, UpdateGroup.class})
    private String username;

    @NotBlank(message = "昵称不能为空", groups = CreateGroup.class)
    @Size(max = 30, message = "昵称长度不能超过 30 位",
            groups = {CreateGroup.class, UpdateGroup.class})
    private String nickname;

    @NotBlank(message = "密码不能为空", groups = CreateGroup.class)
    @Size(min = 8, max = 32, message = "密码长度必须在 8 到 32 位之间",
            groups = {CreateGroup.class, UpdateGroup.class})
    private String password;

    @Email(message = "邮箱格式不正确", groups = {CreateGroup.class, UpdateGroup.class})
    private String email;

    @NotNull(message = "年龄不能为空", groups = CreateGroup.class)
    @Min(value = 1, message = "年龄不能小于 1", groups = {CreateGroup.class, UpdateGroup.class})
    @Max(value = 120, message = "年龄不能大于 120", groups = {CreateGroup.class, UpdateGroup.class})
    private Integer age;
}
```

分组校验有一个容易忽略的点：如果注解声明了 `groups`，但 Controller 中没有指定对应分组，那么该注解不会在默认校验中生效。因此使用分组校验时，Controller 入参应使用 `@Validated(分组.class)`，而不是只使用 `@Valid`。

### Controller 中使用分组校验

Controller 中通过 `@Validated(CreateGroup.class)` 或 `@Validated(UpdateGroup.class)` 指定当前接口执行哪个校验分组。

文件位置：`src/main/java/io/github/atengk/validation/controller/UserGroupController.java`

以下 Controller 演示新增和修改接口分别触发不同分组。

```java
package io.github.atengk.validation.controller;

import io.github.atengk.validation.dto.UserSaveRequest;
import io.github.atengk.validation.group.CreateGroup;
import io.github.atengk.validation.group.UpdateGroup;
import jakarta.validation.constraints.Positive;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 用户分组校验示例接口
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/group/users")
public class UserGroupController {

    /**
     * 新增用户
     *
     * @param request 用户保存请求参数
     * @return 处理结果
     */
    @PostMapping
    public String createUser(@Validated(CreateGroup.class) @RequestBody UserSaveRequest request) {
        log.info("新增用户分组校验通过，username={}", request.getUsername());
        return "新增用户成功";
    }

    /**
     * 修改用户
     *
     * @param request 用户保存请求参数
     * @return 处理结果
     */
    @PutMapping
    public String updateUser(@Validated(UpdateGroup.class) @RequestBody UserSaveRequest request) {
        log.info("修改用户分组校验通过，id={}", request.getId());
        return "修改用户成功";
    }

    /**
     * 删除用户
     *
     * @param id 用户 ID
     * @return 处理结果
     */
    @DeleteMapping("/{id}")
    public String deleteUser(@PathVariable @Positive(message = "用户 ID 必须是正整数") Long id) {
        log.info("删除用户参数校验通过，id={}", id);
        return "删除用户成功";
    }
}
```

新增请求示例：

```bash
curl -X POST "http://localhost:8080/api/group/users" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "ateng",
    "nickname": "阿腾",
    "password": "12345678",
    "email": "ateng@example.com",
    "age": 25
  }'
```

修改请求示例：

```bash
curl -X PUT "http://localhost:8080/api/group/users" \
  -H "Content-Type: application/json" \
  -d '{
    "id": 1001,
    "username": "ateng",
    "nickname": "阿腾",
    "email": "ateng@example.com",
    "age": 26
  }'
```

错误请求示例：

```bash
curl -X POST "http://localhost:8080/api/group/users" \
  -H "Content-Type: application/json" \
  -d '{
    "id": 1001,
    "username": "at",
    "nickname": "",
    "password": "123",
    "age": 0
  }'
```

分组校验的使用建议如下：

| 场景                       | 推荐方式                                |
| -------------------------- | --------------------------------------- |
| 新增和修改字段差异较少     | 同一个 DTO + 分组校验                   |
| 新增和修改字段差异较大     | 拆分 `CreateRequest` 和 `UpdateRequest` |
| 查询参数和提交参数差异明显 | 拆分不同 DTO                            |
| 分组规则很多且复杂         | 优先拆 DTO，避免分组过度复杂            |

## 自定义校验

自定义校验用于处理内置注解无法覆盖的业务规则，例如枚举值校验、手机号归属校验、用户名唯一性校验、用户 ID 是否存在等。

如果示例中使用 Hutool 工具类，需要补充依赖。

Maven 依赖：

```xml
<!-- Hutool 工具类，示例中用于字符串、数组、集合等判断 -->
<dependency>
    <groupId>cn.hutool</groupId>
    <artifactId>hutool-core</artifactId>
    <version>5.8.35</version>
</dependency>
```

Gradle 依赖：

```kotlin
// Hutool 工具类，示例中用于字符串、数组、集合等判断
implementation("cn.hutool:hutool-core:5.8.35")
```

### 自定义校验注解

自定义校验注解需要使用 `@Constraint` 指定校验器实现类，并定义 `message`、`groups`、`payload` 三个标准属性。

下面以“枚举值校验”为例，限制字段只能取指定值。

文件位置：`src/main/java/io/github/atengk/validation/annotation/EnumValue.java`

以下注解用于校验字符串字段是否属于指定枚举值集合。

```java
package io.github.atengk.validation.annotation;

import io.github.atengk.validation.validator.EnumValueValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * 枚举值校验注解
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Documented
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = EnumValueValidator.class)
public @interface EnumValue {

    /**
     * 默认错误提示
     *
     * @return 错误提示
     */
    String message() default "参数值不在允许范围内";

    /**
     * 允许的值
     *
     * @return 枚举值数组
     */
    String[] values();

    /**
     * 是否允许为空
     *
     * @return true 表示允许为空
     */
    boolean allowBlank() default true;

    /**
     * 校验分组
     *
     * @return 分组类型
     */
    Class<?>[] groups() default {};

    /**
     * 负载信息
     *
     * @return 负载类型
     */
    Class<? extends Payload>[] payload() default {};
}
```

### 自定义校验器实现

自定义校验器需要实现 `ConstraintValidator<A, T>` 接口。`A` 是自定义注解类型，`T` 是被校验字段的类型。

文件位置：`src/main/java/io/github/atengk/validation/validator/EnumValueValidator.java`

以下校验器用于判断字符串是否在注解声明的允许值范围内。

```java
package io.github.atengk.validation.validator;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.validation.annotation.EnumValue;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 枚举值校验器
 *
 * @author Ateng
 * @since 2026-05-06
 */
public class EnumValueValidator implements ConstraintValidator<EnumValue, String> {

    private Set<String> allowedValues;

    private boolean allowBlank;

    /**
     * 初始化校验参数
     *
     * @param constraintAnnotation 枚举值校验注解
     */
    @Override
    public void initialize(EnumValue constraintAnnotation) {
        this.allowBlank = constraintAnnotation.allowBlank();

        String[] values = constraintAnnotation.values();
        if (ArrayUtil.isEmpty(values)) {
            this.allowedValues = Set.of();
            return;
        }

        this.allowedValues = Arrays.stream(values)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toSet());
    }

    /**
     * 执行校验
     *
     * @param value   待校验值
     * @param context 校验上下文
     * @return true 表示校验通过
     */
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (StrUtil.isBlank(value)) {
            return allowBlank;
        }

        if (CollUtil.isEmpty(allowedValues)) {
            return false;
        }

        return allowedValues.contains(value);
    }
}
```

在 DTO 中使用自定义注解：

文件位置：`src/main/java/io/github/atengk/validation/dto/UserStatusUpdateRequest.java`

```java
package io.github.atengk.validation.dto;

import io.github.atengk.validation.annotation.EnumValue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * 用户状态修改请求参数
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
public class UserStatusUpdateRequest {

    @NotNull(message = "用户 ID 不能为空")
    @Positive(message = "用户 ID 必须是正整数")
    private Long userId;

    @EnumValue(values = {"ENABLE", "DISABLE"}, allowBlank = false, message = "用户状态只能是 ENABLE 或 DISABLE")
    private String status;
}
```

Controller 使用示例：

```java
/**
 * 修改用户状态
 *
 * @param request 用户状态修改请求参数
 * @return 处理结果
 */
@PatchMapping("/status")
public String updateStatus(@Validated @RequestBody UserStatusUpdateRequest request) {
    log.info("修改用户状态参数校验通过，userId={}，status={}", request.getUserId(), request.getStatus());
    return "修改用户状态成功";
}
```

### 注入 Spring Bean 进行校验

某些校验规则需要查询数据库或调用业务组件，例如判断用户 ID 是否存在、编码是否重复、手机号是否已注册等。自定义校验器可以注入 Spring Bean 完成业务校验。

下面以“用户 ID 是否存在”为例。

文件位置：`src/main/java/io/github/atengk/validation/annotation/UserExists.java`

以下注解用于校验用户 ID 是否存在。

```java
package io.github.atengk.validation.annotation;

import io.github.atengk.validation.validator.UserExistsValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * 用户存在校验注解
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Documented
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = UserExistsValidator.class)
public @interface UserExists {

    /**
     * 默认错误提示
     *
     * @return 错误提示
     */
    String message() default "用户不存在";

    /**
     * 是否允许为空
     *
     * @return true 表示允许为空
     */
    boolean allowNull() default false;

    /**
     * 校验分组
     *
     * @return 分组类型
     */
    Class<?>[] groups() default {};

    /**
     * 负载信息
     *
     * @return 负载类型
     */
    Class<? extends Payload>[] payload() default {};
}
```

文件位置：`src/main/java/io/github/atengk/validation/service/UserCheckService.java`

以下 Service 模拟业务查询，真实项目中可以替换为 Mapper、Repository 或远程服务调用。

```java
package io.github.atengk.validation.service;

import cn.hutool.core.collection.CollUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * 用户校验业务服务
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Service
public class UserCheckService {

    private static final Set<Long> EXIST_USER_IDS = Set.of(1001L, 1002L, 1003L);

    /**
     * 判断用户是否存在
     *
     * @param userId 用户 ID
     * @return true 表示用户存在
     */
    public boolean existsById(Long userId) {
        boolean exists = CollUtil.contains(EXIST_USER_IDS, userId);
        log.info("校验用户是否存在，userId={}，exists={}", userId, exists);
        return exists;
    }
}
```

文件位置：`src/main/java/io/github/atengk/validation/validator/UserExistsValidator.java`

以下校验器通过构造方法注入 Spring Bean，并调用业务方法执行校验。

```java
package io.github.atengk.validation.validator;

import io.github.atengk.validation.annotation.UserExists;
import io.github.atengk.validation.service.UserCheckService;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;

/**
 * 用户存在校验器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@RequiredArgsConstructor
public class UserExistsValidator implements ConstraintValidator<UserExists, Long> {

    private final UserCheckService userCheckService;

    private boolean allowNull;

    /**
     * 初始化校验参数
     *
     * @param constraintAnnotation 用户存在校验注解
     */
    @Override
    public void initialize(UserExists constraintAnnotation) {
        this.allowNull = constraintAnnotation.allowNull();
    }

    /**
     * 执行用户存在性校验
     *
     * @param value   用户 ID
     * @param context 校验上下文
     * @return true 表示校验通过
     */
    @Override
    public boolean isValid(Long value, ConstraintValidatorContext context) {
        if (value == null) {
            return allowNull;
        }

        return userCheckService.existsById(value);
    }
}
```

文件位置：`src/main/java/io/github/atengk/validation/dto/UserDeleteRequest.java`

以下 DTO 演示使用 `@UserExists` 校验用户 ID 是否存在。

```java
package io.github.atengk.validation.dto;

import io.github.atengk.validation.annotation.UserExists;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * 用户删除请求参数
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
public class UserDeleteRequest {

    @NotNull(message = "用户 ID 不能为空")
    @Positive(message = "用户 ID 必须是正整数")
    @UserExists(message = "要删除的用户不存在")
    private Long userId;
}
```

Controller 使用示例：

```java
/**
 * 删除用户
 *
 * @param request 用户删除请求参数
 * @return 处理结果
 */
@PostMapping("/delete")
public String deleteUser(@Validated @RequestBody UserDeleteRequest request) {
    log.info("删除用户参数校验通过，userId={}", request.getUserId());
    return "删除用户成功";
}
```

如果校验器中需要访问数据库，建议只做轻量查询，例如 `exists` 判断，不要在校验器中编写复杂业务流程。复杂业务规则应放在 Service 层处理，避免校验器职责过重。

## 全局异常处理

全局异常处理用于将 Validation 抛出的不同异常转换为统一响应结构。这样前端无论遇到请求体字段错误、URL 参数错误、路径变量错误，还是自定义校验失败，都可以按统一格式处理。

### 参数校验异常类型

Spring Boot 3 中常见的参数校验异常如下：

| 异常类型                              | 常见触发场景                                                 |
| ------------------------------------- | ------------------------------------------------------------ |
| `MethodArgumentNotValidException`     | `@RequestBody` 对象字段校验失败                              |
| `BindException`                       | 表单对象、查询对象绑定和校验失败                             |
| `ConstraintViolationException`        | `@RequestParam`、`@PathVariable`、方法参数校验失败           |
| `HandlerMethodValidationException`    | Spring MVC 方法参数或返回值校验失败，常见于较新的 Spring Framework 6.x |
| `MethodArgumentTypeMismatchException` | 参数类型转换失败，例如 `id=abc` 转 `Long`                    |
| `HttpMessageNotReadableException`     | JSON 格式错误、请求体无法解析                                |

实际项目中建议至少处理前三类 Validation 异常，同时补充类型转换异常和 JSON 解析异常。这样接口返回会更稳定，不会把框架默认错误响应直接暴露给前端。

### 统一错误响应结构

统一响应结构建议至少包含业务状态码、提示信息、请求路径、时间和字段错误列表。字段错误列表用于向前端展示具体字段的错误原因。

文件位置：`src/main/java/io/github/atengk/validation/response/ApiResult.java`

以下类用于封装接口统一响应结果。

```java
package io.github.atengk.validation.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 接口统一响应结果
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
public class ApiResult<T> {

    private Integer code;

    private String message;

    private T data;

    private String path;

    private LocalDateTime timestamp;

    /**
     * 构建成功响应
     *
     * @param data 响应数据
     * @param <T>  数据类型
     * @return 统一响应结果
     */
    public static <T> ApiResult<T> success(T data) {
        ApiResult<T> result = new ApiResult<>();
        result.setCode(200);
        result.setMessage("操作成功");
        result.setData(data);
        result.setTimestamp(LocalDateTime.now());
        return result;
    }

    /**
     * 构建失败响应
     *
     * @param code    状态码
     * @param message 提示信息
     * @param data    响应数据
     * @param path    请求路径
     * @param <T>     数据类型
     * @return 统一响应结果
     */
    public static <T> ApiResult<T> fail(Integer code, String message, T data, String path) {
        ApiResult<T> result = new ApiResult<>();
        result.setCode(code);
        result.setMessage(message);
        result.setData(data);
        result.setPath(path);
        result.setTimestamp(LocalDateTime.now());
        return result;
    }
}
```

文件位置：`src/main/java/io/github/atengk/validation/response/ValidationErrorItem.java`

以下类用于封装单个字段的校验错误信息。

```java
package io.github.atengk.validation.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 字段校验错误信息
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValidationErrorItem {

    private String field;

    private Object rejectedValue;

    private String message;
}
```

统一响应示例：

```json
{
  "code": 400,
  "message": "参数校验失败",
  "data": [
    {
      "field": "username",
      "rejectedValue": "at",
      "message": "用户名长度必须在 4 到 20 位之间"
    },
    {
      "field": "age",
      "rejectedValue": 0,
      "message": "年龄不能小于 1"
    }
  ],
  "path": "/api/group/users",
  "timestamp": "2026-05-06T10:30:00"
}
```

### 字段级错误信息封装

全局异常处理类中需要分别提取不同异常中的字段错误信息。请求体对象通常从 `BindingResult` 中读取；方法参数校验通常从 `ConstraintViolation` 中读取；类型转换和 JSON 解析错误则返回普通错误提示。

文件位置：`src/main/java/io/github/atengk/validation/handler/GlobalExceptionHandler.java`

以下全局异常处理器用于统一封装 Validation 校验失败响应。

```java
package io.github.atengk.validation.handler;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.validation.response.ApiResult;
import io.github.atengk.validation.response.ValidationErrorItem;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;
import java.util.Objects;

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
     * 处理请求体对象校验异常
     *
     * @param exception MethodArgumentNotValidException
     * @param request   HTTP 请求
     * @return 统一响应结果
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResult<List<ValidationErrorItem>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {

        List<ValidationErrorItem> errors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::buildFieldErrorItem)
                .toList();

        log.warn("请求体参数校验失败，path={}，errors={}", request.getRequestURI(), errors);
        return ApiResult.fail(400, "参数校验失败", errors, request.getRequestURI());
    }

    /**
     * 处理表单对象或查询对象绑定校验异常
     *
     * @param exception BindException
     * @param request   HTTP 请求
     * @return 统一响应结果
     */
    @ExceptionHandler(BindException.class)
    public ApiResult<List<ValidationErrorItem>> handleBindException(
            BindException exception,
            HttpServletRequest request) {

        List<ValidationErrorItem> errors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::buildFieldErrorItem)
                .toList();

        log.warn("参数绑定或校验失败，path={}，errors={}", request.getRequestURI(), errors);
        return ApiResult.fail(400, "参数校验失败", errors, request.getRequestURI());
    }

    /**
     * 处理方法参数校验异常
     *
     * @param exception ConstraintViolationException
     * @param request   HTTP 请求
     * @return 统一响应结果
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ApiResult<List<ValidationErrorItem>> handleConstraintViolationException(
            ConstraintViolationException exception,
            HttpServletRequest request) {

        List<ValidationErrorItem> errors = exception.getConstraintViolations()
                .stream()
                .map(this::buildConstraintViolationErrorItem)
                .toList();

        log.warn("方法参数校验失败，path={}，errors={}", request.getRequestURI(), errors);
        return ApiResult.fail(400, "参数校验失败", errors, request.getRequestURI());
    }

    /**
     * 处理 Spring MVC 方法校验异常
     *
     * @param exception HandlerMethodValidationException
     * @param request   HTTP 请求
     * @return 统一响应结果
     */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ApiResult<List<ValidationErrorItem>> handleHandlerMethodValidationException(
            HandlerMethodValidationException exception,
            HttpServletRequest request) {

        List<ValidationErrorItem> errors = exception.getAllErrors()
                .stream()
                .map(error -> new ValidationErrorItem(
                        getFieldName(error),
                        null,
                        error.getDefaultMessage()))
                .toList();

        log.warn("Handler 方法参数校验失败，path={}，errors={}", request.getRequestURI(), errors);
        return ApiResult.fail(400, "参数校验失败", errors, request.getRequestURI());
    }

    /**
     * 处理参数类型转换异常
     *
     * @param exception MethodArgumentTypeMismatchException
     * @param request   HTTP 请求
     * @return 统一响应结果
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ApiResult<Void> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request) {

        String message = StrUtil.format("参数 {} 类型不正确", exception.getName());
        log.warn("参数类型转换失败，path={}，name={}，value={}",
                request.getRequestURI(), exception.getName(), exception.getValue());

        return ApiResult.fail(400, message, null, request.getRequestURI());
    }

    /**
     * 处理 JSON 请求体解析异常
     *
     * @param exception HttpMessageNotReadableException
     * @param request   HTTP 请求
     * @return 统一响应结果
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ApiResult<Void> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException exception,
            HttpServletRequest request) {

        log.warn("请求体解析失败，path={}，message={}", request.getRequestURI(), exception.getMessage());
        return ApiResult.fail(400, "请求体格式不正确", null, request.getRequestURI());
    }

    /**
     * 处理未知异常
     *
     * @param exception Exception
     * @param request   HTTP 请求
     * @return 统一响应结果
     */
    @ExceptionHandler(Exception.class)
    public ApiResult<Void> handleException(Exception exception, HttpServletRequest request) {
        log.error("系统异常，path={}", request.getRequestURI(), exception);
        return ApiResult.fail(500, "系统异常，请稍后重试", null, request.getRequestURI());
    }

    /**
     * 构建字段错误信息
     *
     * @param fieldError 字段错误
     * @return 字段校验错误信息
     */
    private ValidationErrorItem buildFieldErrorItem(FieldError fieldError) {
        return new ValidationErrorItem(
                fieldError.getField(),
                fieldError.getRejectedValue(),
                fieldError.getDefaultMessage()
        );
    }

    /**
     * 构建方法参数错误信息
     *
     * @param violation 约束错误
     * @return 字段校验错误信息
     */
    private ValidationErrorItem buildConstraintViolationErrorItem(ConstraintViolation<?> violation) {
        String propertyPath = Objects.toString(violation.getPropertyPath(), "");
        String field = getLastPathNode(propertyPath);

        return new ValidationErrorItem(
                field,
                violation.getInvalidValue(),
                violation.getMessage()
        );
    }

    /**
     * 获取最后一级字段名
     *
     * @param propertyPath 属性路径
     * @return 字段名
     */
    private String getLastPathNode(String propertyPath) {
        if (StrUtil.isBlank(propertyPath)) {
            return "";
        }

        List<String> nodes = StrUtil.split(propertyPath, '.');
        if (CollUtil.isEmpty(nodes)) {
            return propertyPath;
        }

        return CollUtil.getLast(nodes);
    }

    /**
     * 获取字段名称
     *
     * @param resolvable 错误信息
     * @return 字段名称
     */
    private String getFieldName(DefaultMessageSourceResolvable resolvable) {
        String[] codes = resolvable.getCodes();
        if (codes == null || codes.length == 0) {
            return "";
        }

        String code = codes[0];
        return getLastPathNode(code);
    }
}
```

验证请求体字段校验：

```bash
curl -X POST "http://localhost:8080/api/group/users" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "at",
    "nickname": "",
    "password": "123",
    "email": "error-email",
    "age": 0
  }'
```

验证路径变量校验：

```bash
curl -X DELETE "http://localhost:8080/api/group/users/0"
```

验证自定义枚举值校验：

```bash
curl -X PATCH "http://localhost:8080/api/group/users/status" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1001,
    "status": "LOCKED"
  }'
```

验证用户是否存在校验：

```bash
curl -X POST "http://localhost:8080/api/group/users/delete" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 9999
  }'
```

全局异常处理建议：

| 建议                           | 说明                                 |
| ------------------------------ | ------------------------------------ |
| 不要直接返回框架默认异常       | 默认错误结构不利于前端统一处理       |
| 字段错误使用数组返回           | 一个请求可能同时存在多个字段错误     |
| 日志使用 `warn` 记录参数错误   | 参数错误通常不是系统异常             |
| 未知异常使用 `error` 记录堆栈  | 便于排查服务端问题                   |
| 不建议把完整异常堆栈返回给前端 | 避免泄露内部实现细节                 |
| 参数错误统一使用 400           | 符合 HTTP 语义，也便于网关和前端处理 |



## 实战示例

本节将前面介绍的请求体校验、URL 参数校验、路径变量校验、集合参数校验、分组校验和自定义校验组合到一个用户管理示例中。示例重点放在参数校验链路，不涉及数据库持久化，业务层使用模拟逻辑返回结果。

示例文件结构如下：

```text
src/main/java/io/github/atengk/validation/
├── controller
│   └── UserPracticeController.java
├── dto
│   ├── UserPracticeSaveRequest.java
│   ├── UserPracticePageQuery.java
│   └── UserPracticeBatchRequest.java
├── service
│   ├── UserPracticeService.java
│   └── impl
│       └── UserPracticeServiceImpl.java
├── group
│   ├── CreateGroup.java
│   └── UpdateGroup.java
├── annotation
│   ├── EnumValue.java
│   └── UserExists.java
└── response
    └── ApiResult.java
```

本节默认已经存在前文定义过的 `CreateGroup`、`UpdateGroup`、`EnumValue`、`UserExists`、`ApiResult` 和 `GlobalExceptionHandler`。如果单独复制本节代码，需要先补齐这些基础类。

### 用户新增参数校验

用户新增接口主要校验用户名、昵称、密码、邮箱、手机号、年龄和状态字段。新增场景下 `id` 必须为空，避免客户端主动传入主键。

文件位置：`src/main/java/io/github/atengk/validation/dto/UserPracticeSaveRequest.java`

以下请求对象同时支持新增和修改，通过分组区分不同字段的校验规则。

```java
package io.github.atengk.validation.dto;

import io.github.atengk.validation.annotation.EnumValue;
import io.github.atengk.validation.annotation.UserExists;
import io.github.atengk.validation.group.CreateGroup;
import io.github.atengk.validation.group.UpdateGroup;
import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * 用户实战保存请求参数
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
public class UserPracticeSaveRequest {

    @Null(message = "新增用户时 ID 必须为空", groups = CreateGroup.class)
    @NotNull(message = "修改用户时 ID 不能为空", groups = UpdateGroup.class)
    @Positive(message = "用户 ID 必须是正整数", groups = UpdateGroup.class)
    @UserExists(message = "要修改的用户不存在", groups = UpdateGroup.class)
    private Long id;

    @NotBlank(message = "用户名不能为空", groups = CreateGroup.class)
    @Size(min = 4, max = 20, message = "用户名长度必须在 4 到 20 位之间",
            groups = {CreateGroup.class, UpdateGroup.class})
    @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_]*$", message = "用户名必须以字母开头，只能包含字母、数字和下划线",
            groups = {CreateGroup.class, UpdateGroup.class})
    private String username;

    @NotBlank(message = "昵称不能为空", groups = CreateGroup.class)
    @Size(max = 30, message = "昵称长度不能超过 30 位",
            groups = {CreateGroup.class, UpdateGroup.class})
    private String nickname;

    @NotBlank(message = "密码不能为空", groups = CreateGroup.class)
    @Size(min = 8, max = 32, message = "密码长度必须在 8 到 32 位之间",
            groups = CreateGroup.class)
    private String password;

    @Email(message = "邮箱格式不正确", groups = {CreateGroup.class, UpdateGroup.class})
    private String email;

    @NotBlank(message = "手机号不能为空", groups = CreateGroup.class)
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确",
            groups = {CreateGroup.class, UpdateGroup.class})
    private String phone;

    @NotNull(message = "年龄不能为空", groups = CreateGroup.class)
    @Min(value = 1, message = "年龄不能小于 1", groups = {CreateGroup.class, UpdateGroup.class})
    @Max(value = 120, message = "年龄不能大于 120", groups = {CreateGroup.class, UpdateGroup.class})
    private Integer age;

    @EnumValue(values = {"ENABLE", "DISABLE"}, allowBlank = false, message = "用户状态只能是 ENABLE 或 DISABLE",
            groups = {CreateGroup.class, UpdateGroup.class})
    private String status;
}
```

文件位置：`src/main/java/io/github/atengk/validation/service/UserPracticeService.java`

以下 Service 接口定义用户新增、修改、列表查询和批量提交的业务入口。

```java
package io.github.atengk.validation.service;

import io.github.atengk.validation.dto.UserPracticeBatchRequest;
import io.github.atengk.validation.dto.UserPracticePageQuery;
import io.github.atengk.validation.dto.UserPracticeSaveRequest;

/**
 * 用户实战业务服务
 *
 * @author Ateng
 * @since 2026-05-06
 */
public interface UserPracticeService {

    Long createUser(UserPracticeSaveRequest request);

    void updateUser(UserPracticeSaveRequest request);

    String listUsers(UserPracticePageQuery query);

    Integer batchSubmit(UserPracticeBatchRequest request);
}
```

文件位置：`src/main/java/io/github/atengk/validation/service/impl/UserPracticeServiceImpl.java`

以下实现类使用 Hutool 做字符串处理和集合判断，真实项目中可以替换为 MyBatis-Plus、JPA 或远程服务调用。

```java
package io.github.atengk.validation.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.validation.dto.UserPracticeBatchRequest;
import io.github.atengk.validation.dto.UserPracticePageQuery;
import io.github.atengk.validation.dto.UserPracticeSaveRequest;
import io.github.atengk.validation.service.UserPracticeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 用户实战业务服务实现
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Service
public class UserPracticeServiceImpl implements UserPracticeService {

    @Override
    public Long createUser(UserPracticeSaveRequest request) {
        String username = StrUtil.trim(request.getUsername());
        Long userId = RandomUtil.randomLong(10000L, 99999L);
        log.info("模拟新增用户成功，userId={}，username={}，status={}", userId, username, request.getStatus());
        return userId;
    }

    @Override
    public void updateUser(UserPracticeSaveRequest request) {
        String nickname = StrUtil.trim(request.getNickname());
        log.info("模拟修改用户成功，userId={}，nickname={}，status={}", request.getId(), nickname, request.getStatus());
    }

    @Override
    public String listUsers(UserPracticePageQuery query) {
        String keyword = StrUtil.blankToDefault(query.getKeyword(), "全部");
        String status = StrUtil.blankToDefault(query.getStatus(), "全部");
        log.info("模拟查询用户列表，pageNum={}，pageSize={}，keyword={}，status={}",
                query.getPageNum(), query.getPageSize(), keyword, status);

        return StrUtil.format("查询成功，页码={}，条数={}，关键词={}，状态={}",
                query.getPageNum(), query.getPageSize(), keyword, status);
    }

    @Override
    public Integer batchSubmit(UserPracticeBatchRequest request) {
        int size = CollUtil.size(request.getUserIds());
        log.info("模拟批量提交用户操作，action={}，数量={}", request.getAction(), size);
        return size;
    }
}
```

文件位置：`src/main/java/io/github/atengk/validation/controller/UserPracticeController.java`

以下 Controller 组合演示新增、修改、列表查询和批量提交接口的参数校验写法。

```java
package io.github.atengk.validation.controller;

import io.github.atengk.validation.dto.UserPracticeBatchRequest;
import io.github.atengk.validation.dto.UserPracticePageQuery;
import io.github.atengk.validation.dto.UserPracticeSaveRequest;
import io.github.atengk.validation.group.CreateGroup;
import io.github.atengk.validation.group.UpdateGroup;
import io.github.atengk.validation.response.ApiResult;
import io.github.atengk.validation.service.UserPracticeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 用户实战参数校验接口
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/practice/users")
public class UserPracticeController {

    private final UserPracticeService userPracticeService;

    @PostMapping
    public ApiResult<Long> createUser(@Validated(CreateGroup.class) @RequestBody UserPracticeSaveRequest request) {
        Long userId = userPracticeService.createUser(request);
        log.info("新增用户接口执行完成，userId={}", userId);
        return ApiResult.success(userId);
    }

    @PutMapping
    public ApiResult<Void> updateUser(@Validated(UpdateGroup.class) @RequestBody UserPracticeSaveRequest request) {
        userPracticeService.updateUser(request);
        log.info("修改用户接口执行完成，userId={}", request.getId());
        return ApiResult.success(null);
    }

    @GetMapping
    public ApiResult<String> listUsers(@Validated UserPracticePageQuery query) {
        String result = userPracticeService.listUsers(query);
        log.info("列表查询用户接口执行完成");
        return ApiResult.success(result);
    }

    @PostMapping("/batch")
    public ApiResult<Integer> batchSubmit(@Validated @RequestBody UserPracticeBatchRequest request) {
        Integer count = userPracticeService.batchSubmit(request);
        log.info("批量提交用户接口执行完成，数量={}", count);
        return ApiResult.success(count);
    }
}
```

新增用户请求示例：

```bash
curl -X POST "http://localhost:8080/api/practice/users" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "ateng001",
    "nickname": "阿腾001",
    "password": "12345678",
    "email": "ateng001@example.com",
    "phone": "13800138000",
    "age": 25,
    "status": "ENABLE"
  }'
```

成功响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": 58321,
  "path": null,
  "timestamp": "2026-05-06T10:30:00"
}
```

### 用户修改参数校验

用户修改接口复用 `UserPracticeSaveRequest`，但使用 `UpdateGroup` 分组。修改时 `id` 必填，并通过自定义 `@UserExists` 校验用户是否存在；密码不再强制必填，适合“修改基础资料”的场景。

修改用户请求示例：

```bash
curl -X PUT "http://localhost:8080/api/practice/users" \
  -H "Content-Type: application/json" \
  -d '{
    "id": 1001,
    "username": "ateng001",
    "nickname": "阿腾修改",
    "email": "new-ateng@example.com",
    "phone": "13900139000",
    "age": 26,
    "status": "ENABLE"
  }'
```

修改接口的关键点是 Controller 入参使用：

```java
@Validated(UpdateGroup.class) @RequestBody UserPracticeSaveRequest request
```

如果这里改成 `@Valid`，`UpdateGroup` 中声明的校验规则不会按预期执行。

### 列表查询参数校验

列表查询一般使用 URL 查询参数，例如页码、每页条数、关键词和状态。查询参数较多时，建议封装为 Query 对象，避免 Controller 方法参数过长。

文件位置：`src/main/java/io/github/atengk/validation/dto/UserPracticePageQuery.java`

以下查询对象用于列表接口的分页和条件校验。

```java
package io.github.atengk.validation.dto;

import io.github.atengk.validation.annotation.EnumValue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户实战分页查询参数
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
public class UserPracticePageQuery {

    @Min(value = 1, message = "当前页码不能小于 1")
    private Integer pageNum = 1;

    @Min(value = 1, message = "每页条数不能小于 1")
    @Max(value = 100, message = "每页条数不能大于 100")
    private Integer pageSize = 10;

    @Size(max = 30, message = "关键词长度不能超过 30 位")
    private String keyword;

    @EnumValue(values = {"ENABLE", "DISABLE"}, message = "用户状态只能是 ENABLE 或 DISABLE")
    private String status;
}
```

列表查询请求示例：

```bash
curl "http://localhost:8080/api/practice/users?pageNum=1&pageSize=10&keyword=ateng&status=ENABLE"
```

异常请求示例：

```bash
curl "http://localhost:8080/api/practice/users?pageNum=0&pageSize=200&status=LOCKED"
```

该接口会触发查询对象字段校验，常见异常由全局异常处理器中的 `BindException` 或 Spring MVC 方法校验异常处理逻辑统一封装。

### 批量提交参数校验

批量提交常见于批量删除、批量启用、批量禁用等场景。校验时需要同时限制集合本身和集合元素。

文件位置：`src/main/java/io/github/atengk/validation/dto/UserPracticeBatchRequest.java`

以下请求对象用于批量提交用户操作。

```java
package io.github.atengk.validation.dto;

import io.github.atengk.validation.annotation.EnumValue;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 用户实战批量提交请求参数
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
public class UserPracticeBatchRequest {

    @NotEmpty(message = "用户 ID 集合不能为空")
    @Size(max = 100, message = "单次最多提交 100 个用户")
    private List<@NotNull(message = "用户 ID 不能为空") @Positive(message = "用户 ID 必须是正整数") Long> userIds;

    @EnumValue(values = {"ENABLE", "DISABLE", "DELETE"}, allowBlank = false,
            message = "批量操作类型只能是 ENABLE、DISABLE 或 DELETE")
    private String action;
}
```

批量提交请求示例：

```bash
curl -X POST "http://localhost:8080/api/practice/users/batch" \
  -H "Content-Type: application/json" \
  -d '{
    "userIds": [1001, 1002, 1003],
    "action": "DISABLE"
  }'
```

异常请求示例：

```bash
curl -X POST "http://localhost:8080/api/practice/users/batch" \
  -H "Content-Type: application/json" \
  -d '{
    "userIds": [1001, 0, -1],
    "action": "UNKNOWN"
  }'
```

批量参数校验建议：

| 校验目标              | 推荐写法                           |
| --------------------- | ---------------------------------- |
| 集合不能为空          | `@NotEmpty`                        |
| 限制批量数量          | `@Size(max = 100)`                 |
| 集合元素不能为 `null` | `List<@NotNull Long>`              |
| 集合元素必须大于 0    | `List<@Positive Long>`             |
| 操作类型固定          | 自定义 `@EnumValue` 或业务枚举校验 |

## 测试与验证

本节使用 curl 和 MockMvc 两种方式验证参数校验效果。curl 适合手工调试接口，MockMvc 适合纳入自动化测试，避免后续改动破坏校验规则。

如果项目中还没有测试依赖，需要添加 Spring Boot 测试 starter。

Maven 依赖：

```xml
<!-- Spring Boot 测试依赖，包含 JUnit Jupiter、MockMvc、AssertJ 等常用测试组件 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

Gradle 依赖：

```kotlin
// Spring Boot 测试依赖，包含 JUnit Jupiter、MockMvc、AssertJ 等常用测试组件
testImplementation("org.springframework.boot:spring-boot-starter-test")
```

### 正常参数测试

正常参数测试用于确认合法请求可以通过校验，并且接口返回成功响应。

手工验证新增用户：

```bash
curl -X POST "http://localhost:8080/api/practice/users" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "ateng001",
    "nickname": "阿腾001",
    "password": "12345678",
    "email": "ateng001@example.com",
    "phone": "13800138000",
    "age": 25,
    "status": "ENABLE"
  }'
```

预期响应：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": 58321,
  "path": null,
  "timestamp": "2026-05-06T10:30:00"
}
```

手工验证列表查询：

```bash
curl "http://localhost:8080/api/practice/users?pageNum=1&pageSize=10&keyword=ateng&status=ENABLE"
```

预期结果是 `code=200`，并返回查询成功信息。

### 异常参数测试

异常参数测试用于确认非法请求会被全局异常处理器拦截，并返回统一错误结构。

手工验证新增用户参数异常：

```bash
curl -X POST "http://localhost:8080/api/practice/users" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "at",
    "nickname": "",
    "password": "123",
    "email": "error-email",
    "phone": "123456",
    "age": 0,
    "status": "LOCKED"
  }'
```

预期响应示例：

```json
{
  "code": 400,
  "message": "参数校验失败",
  "data": [
    {
      "field": "username",
      "rejectedValue": "at",
      "message": "用户名长度必须在 4 到 20 位之间"
    },
    {
      "field": "nickname",
      "rejectedValue": "",
      "message": "昵称不能为空"
    },
    {
      "field": "status",
      "rejectedValue": "LOCKED",
      "message": "用户状态只能是 ENABLE 或 DISABLE"
    }
  ],
  "path": "/api/practice/users",
  "timestamp": "2026-05-06T10:30:00"
}
```

手工验证列表查询异常：

```bash
curl "http://localhost:8080/api/practice/users?pageNum=0&pageSize=200&status=LOCKED"
```

预期结果是返回 `code=400`，错误信息中包含页码、每页条数和状态字段的校验失败原因。

### 分组校验测试

分组校验测试用于确认新增和修改接口触发的是不同校验规则。新增接口不能传 `id`，修改接口必须传 `id`。

手工验证新增时错误传入 ID：

```bash
curl -X POST "http://localhost:8080/api/practice/users" \
  -H "Content-Type: application/json" \
  -d '{
    "id": 1001,
    "username": "ateng001",
    "nickname": "阿腾001",
    "password": "12345678",
    "email": "ateng001@example.com",
    "phone": "13800138000",
    "age": 25,
    "status": "ENABLE"
  }'
```

预期错误信息：

```json
{
  "code": 400,
  "message": "参数校验失败",
  "data": [
    {
      "field": "id",
      "rejectedValue": 1001,
      "message": "新增用户时 ID 必须为空"
    }
  ]
}
```

手工验证修改时缺少 ID：

```bash
curl -X PUT "http://localhost:8080/api/practice/users" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "ateng001",
    "nickname": "阿腾修改",
    "email": "new-ateng@example.com",
    "phone": "13900139000",
    "age": 26,
    "status": "ENABLE"
  }'
```

预期错误信息中包含：

```text
修改用户时 ID 不能为空
```

### 自定义校验测试

自定义校验测试用于确认 `@EnumValue` 和 `@UserExists` 可以正常拦截非法业务参数。

验证枚举值非法：

```bash
curl -X PUT "http://localhost:8080/api/practice/users" \
  -H "Content-Type: application/json" \
  -d '{
    "id": 1001,
    "username": "ateng001",
    "nickname": "阿腾修改",
    "email": "new-ateng@example.com",
    "phone": "13900139000",
    "age": 26,
    "status": "LOCKED"
  }'
```

预期错误信息：

```text
用户状态只能是 ENABLE 或 DISABLE
```

验证用户不存在：

```bash
curl -X PUT "http://localhost:8080/api/practice/users" \
  -H "Content-Type: application/json" \
  -d '{
    "id": 9999,
    "username": "ateng001",
    "nickname": "阿腾修改",
    "email": "new-ateng@example.com",
    "phone": "13900139000",
    "age": 26,
    "status": "ENABLE"
  }'
```

预期错误信息：

```text
要修改的用户不存在
```

文件位置：`src/test/java/io/github/atengk/validation/PracticeValidationIntegrationTest.java`

以下测试类使用 MockMvc 覆盖正常参数、异常参数、分组校验和自定义校验场景。

```java
package io.github.atengk.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.atengk.validation.dto.UserPracticeBatchRequest;
import io.github.atengk.validation.dto.UserPracticeSaveRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 参数校验实战接口集成测试
 *
 * @author Ateng
 * @since 2026-05-06
 */
@SpringBootTest
@AutoConfigureMockMvc
class PracticeValidationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("正常参数测试：新增用户成功")
    void createUserSuccess() throws Exception {
        UserPracticeSaveRequest request = new UserPracticeSaveRequest();
        request.setUsername("ateng001");
        request.setNickname("阿腾001");
        request.setPassword("12345678");
        request.setEmail("ateng001@example.com");
        request.setPhone("13800138000");
        request.setAge(25);
        request.setStatus("ENABLE");

        mockMvc.perform(post("/api/practice/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("操作成功"))
                .andExpect(jsonPath("$.data", notNullValue()));
    }

    @Test
    @DisplayName("异常参数测试：新增用户字段非法")
    void createUserInvalidFields() throws Exception {
        UserPracticeSaveRequest request = new UserPracticeSaveRequest();
        request.setUsername("at");
        request.setNickname("");
        request.setPassword("123");
        request.setEmail("error-email");
        request.setPhone("123456");
        request.setAge(0);
        request.setStatus("LOCKED");

        mockMvc.perform(post("/api/practice/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("参数校验失败"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(content().string(containsString("用户名长度必须在 4 到 20 位之间")))
                .andExpect(content().string(containsString("用户状态只能是 ENABLE 或 DISABLE")));
    }

    @Test
    @DisplayName("分组校验测试：新增用户时 ID 必须为空")
    void createUserIdMustBeNull() throws Exception {
        UserPracticeSaveRequest request = new UserPracticeSaveRequest();
        request.setId(1001L);
        request.setUsername("ateng001");
        request.setNickname("阿腾001");
        request.setPassword("12345678");
        request.setEmail("ateng001@example.com");
        request.setPhone("13800138000");
        request.setAge(25);
        request.setStatus("ENABLE");

        mockMvc.perform(post("/api/practice/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(content().string(containsString("新增用户时 ID 必须为空")));
    }

    @Test
    @DisplayName("分组校验测试：修改用户时 ID 不能为空")
    void updateUserIdMustNotBeNull() throws Exception {
        UserPracticeSaveRequest request = new UserPracticeSaveRequest();
        request.setUsername("ateng001");
        request.setNickname("阿腾修改");
        request.setEmail("new-ateng@example.com");
        request.setPhone("13900139000");
        request.setAge(26);
        request.setStatus("ENABLE");

        mockMvc.perform(put("/api/practice/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(content().string(containsString("修改用户时 ID 不能为空")));
    }

    @Test
    @DisplayName("自定义校验测试：用户状态枚举非法")
    void updateUserStatusInvalid() throws Exception {
        UserPracticeSaveRequest request = new UserPracticeSaveRequest();
        request.setId(1001L);
        request.setUsername("ateng001");
        request.setNickname("阿腾修改");
        request.setEmail("new-ateng@example.com");
        request.setPhone("13900139000");
        request.setAge(26);
        request.setStatus("LOCKED");

        mockMvc.perform(put("/api/practice/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(content().string(containsString("用户状态只能是 ENABLE 或 DISABLE")));
    }

    @Test
    @DisplayName("自定义校验测试：修改的用户不存在")
    void updateUserNotExists() throws Exception {
        UserPracticeSaveRequest request = new UserPracticeSaveRequest();
        request.setId(9999L);
        request.setUsername("ateng001");
        request.setNickname("阿腾修改");
        request.setEmail("new-ateng@example.com");
        request.setPhone("13900139000");
        request.setAge(26);
        request.setStatus("ENABLE");

        mockMvc.perform(put("/api/practice/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(content().string(containsString("要修改的用户不存在")));
    }

    @Test
    @DisplayName("列表查询参数测试：分页参数非法")
    void listUsersInvalidPageParam() throws Exception {
        mockMvc.perform(get("/api/practice/users")
                        .param("pageNum", "0")
                        .param("pageSize", "200")
                        .param("status", "LOCKED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(content().string(containsString("当前页码不能小于 1")))
                .andExpect(content().string(containsString("每页条数不能大于 100")));
    }

    @Test
    @DisplayName("批量提交参数测试：批量用户 ID 非法")
    void batchSubmitInvalidUserIds() throws Exception {
        UserPracticeBatchRequest request = new UserPracticeBatchRequest();
        request.setUserIds(List.of(1001L, 0L, -1L));
        request.setAction("DISABLE");

        mockMvc.perform(post("/api/practice/users/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(content().string(containsString("用户 ID 必须是正整数")));
    }

    @Test
    @DisplayName("批量提交参数测试：批量操作类型非法")
    void batchSubmitInvalidAction() throws Exception {
        UserPracticeBatchRequest request = new UserPracticeBatchRequest();
        request.setUserIds(List.of(1001L, 1002L));
        request.setAction("LOCKED");

        mockMvc.perform(post("/api/practice/users/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(content().string(containsString("批量操作类型只能是 ENABLE、DISABLE 或 DELETE")));
    }
}
```

如果全局异常处理器使用 `ResponseEntity` 或 `@ResponseStatus(HttpStatus.BAD_REQUEST)` 返回真实 HTTP 400，那么测试中的 `status().isOk()` 应调整为 `status().isBadRequest()`。如果沿用前文直接返回 `ApiResult` 的写法，则 HTTP 状态通常是 200，业务状态通过响应体中的 `code=400` 表示。