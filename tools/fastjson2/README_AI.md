# Spring Boot 集成 Fastjson2 开发

本文档用于说明在 Spring Boot 3 项目中集成 Fastjson2 的基础开发方式，内容包括技术概述、环境准备、Maven 依赖配置以及项目基础结构。后续章节可继续扩展对象序列化、对象反序列化、全局 JSON 转换器、日期格式配置、Web 接口开发和异常处理等内容。

## 技术概述

本节主要说明 Fastjson2 的技术定位、Spring Boot 3 适配要点，以及在实际后端项目中的典型使用场景。

### Fastjson2 简介

Fastjson2 是 Alibaba 提供的 Java JSON 处理库，用于完成 Java 对象与 JSON 字符串之间的序列化、反序列化，以及 `JSONObject`、`JSONArray`、JSONPath、JSONB 等 JSON 相关处理。Fastjson2 的核心包名为 `com.alibaba.fastjson2`，与 Fastjson 1.x 的 `com.alibaba.fastjson` 不同，因此在老项目迁移时需要特别注意导包差异。

在日常 Spring Boot 项目中，Fastjson2 通常有两种使用方式。第一种是在业务代码中直接调用 `JSON.toJSONString`、`JSON.parseObject`、`JSON.parseArray` 等 API。第二种是通过 Spring MVC 的 `HttpMessageConverter` 机制，将 Fastjson2 配置为 Web 请求和响应的 JSON 转换器。

Fastjson2 常用 API 示例：

```java
// 对象转 JSON 字符串
String json = JSON.toJSONString(user);

// JSON 字符串转 Java 对象
UserVO userVO = JSON.parseObject(json, UserVO.class);

// JSON 字符串转集合
List<UserVO> userList = JSON.parseArray(jsonArrayText, UserVO.class);
```

### Spring Boot 3 适配说明

Spring Boot 3 基于 Spring Framework 6 体系，运行环境要求至少 Java 17；Spring Boot 3.3.16 官方系统要求中也明确说明需要 Java 17，并支持 Maven 3.6.3 或更高版本。([Spring 企业文档](https://docs.enterprise.spring.io/spring-boot/system-requirements.html?utm_source=chatgpt.com))

因此，在 Spring Boot 3 项目中集成 Fastjson2 时，应优先使用适配 Spring 6 的扩展模块：

```xml
<dependency>
    <groupId>com.alibaba.fastjson2</groupId>
    <artifactId>fastjson2-extension-spring6</artifactId>
    <version>2.0.61</version>
</dependency>
```

`fastjson2-extension-spring6` 是 Fastjson2 面向 Spring 6 的扩展模块，Maven Central 当前展示的版本包含 `2.0.61`。([Maven Central](https://central.sonatype.com/artifact/com.alibaba.fastjson2/fastjson2-extension-spring6?utm_source=chatgpt.com))

Spring Boot 3 集成 Fastjson2 时需要关注以下适配点：

| 适配项         | 说明                                                         |
| -------------- | ------------------------------------------------------------ |
| JDK 版本       | Spring Boot 3 最低要求 Java 17，推荐使用 JDK 17 或 JDK 21    |
| Spring 版本    | Spring Boot 3 使用 Spring Framework 6，应使用 `fastjson2-extension-spring6` |
| 包名变化       | Fastjson2 使用 `com.alibaba.fastjson2`，不要继续使用 Fastjson 1.x 的旧包名 |
| Web 消息转换器 | 如需替换默认 JSON 转换行为，可配置 Fastjson2 的 HTTP 消息转换器 |
| 日期时间格式   | 建议统一配置 `LocalDateTime`、`LocalDate`、`Date` 等字段的 JSON 格式 |
| 安全控制       | 不建议开放不受控的 AutoType；外部入参应使用 DTO 和参数校验   |

### 典型使用场景

Fastjson2 在 Spring Boot 3 后端项目中主要用于 JSON 数据的输入、输出、转换和存储。

常见使用场景如下：

| 场景              | 说明                                                         |
| ----------------- | ------------------------------------------------------------ |
| HTTP 响应序列化   | Controller 返回 DTO、VO、统一响应对象时，将 Java 对象转换为 JSON |
| HTTP 请求反序列化 | 接收 JSON 请求体，并转换为请求 DTO                           |
| 第三方接口解析    | 调用外部接口后，将 JSON 响应转换为 Java 对象                 |
| MQ 消息处理       | Kafka、RabbitMQ、RocketMQ 等消息体的 JSON 编码和解码         |
| Redis 缓存处理    | 将对象序列化为 JSON 后写入 Redis，读取时再反序列化           |
| 日志记录          | 将请求参数、响应结果、业务上下文转换为 JSON 记录日志         |
| JSON 数组处理     | 处理批量数据、列表数据、集合类型转换                         |
| JSONPath 查询     | 从复杂 JSON 文档中读取局部字段                               |

在普通业务代码中，如果只是少量 JSON 转换，可以只使用 Fastjson2 核心依赖。如果需要让 Spring MVC 的请求体和响应体统一使用 Fastjson2，则需要额外引入 Spring 6 扩展依赖并配置消息转换器。

## 环境准备

本节给出 Spring Boot 3 集成 Fastjson2 的基础开发环境、Maven 依赖和推荐项目结构。后续章节中的配置类、DTO、VO、Controller 和测试代码都可以基于该结构继续扩展。

### JDK 与 Spring Boot 版本

推荐版本如下：

| 组件        | 推荐版本   | 说明                                               |
| ----------- | ---------- | -------------------------------------------------- |
| JDK         | 17 或 21   | Spring Boot 3 最低要求 Java 17                     |
| Spring Boot | 3.3.x      | 企业项目中建议优先选择稳定维护版本                 |
| Maven       | 3.6.3+     | Spring Boot 3.3.16 官方要求 Maven 3.6.3 或更高版本 |
| Fastjson2   | 2.0.61     | 当前 Maven Central 中可用的 Fastjson2 版本之一     |
| Hutool      | 5.8.x      | 用于字符串、集合、日期、Bean 等常用工具处理        |
| Lombok      | 随项目约定 | 用于减少 DTO、VO、配置类样板代码                   |

Spring Boot 3.3.16 的官方系统要求显示，其需要 Java 17，并要求 Maven 3.6.3 或更高版本。([Spring 企业文档](https://docs.enterprise.spring.io/spring-boot/system-requirements.html?utm_source=chatgpt.com)) Fastjson2 的 Spring 6 扩展模块当前可在 Maven Central 查询到 `2.0.61` 版本。([Maven Central](https://central.sonatype.com/artifact/com.alibaba.fastjson2/fastjson2-extension-spring6?utm_source=chatgpt.com))

### Maven 依赖配置

下面是一个可直接用于 Spring Boot 3 + Fastjson2 的 `pom.xml` 示例。该配置包含 Web、参数校验、Fastjson2、Fastjson2 Spring 6 扩展、Hutool、Lombok 和测试依赖。

文件位置：`pom.xml`

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <!-- Spring Boot 3 父工程，统一管理 Spring 生态依赖版本 -->
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.3.16</version>
        <relativePath/>
    </parent>

    <groupId>io.github.atengk</groupId>
    <artifactId>spring-boot-fastjson2-demo</artifactId>
    <version>1.0.0</version>
    <name>spring-boot-fastjson2-demo</name>
    <description>Spring Boot 3 集成 Fastjson2 示例项目</description>

    <properties>
        <!-- Spring Boot 3 最低要求 JDK 17 -->
        <java.version>17</java.version>

        <!-- Fastjson2 核心依赖与 Spring 6 扩展依赖建议保持同版本 -->
        <fastjson2.version>2.0.61</fastjson2.version>

        <!-- Hutool 工具类版本 -->
        <hutool.version>5.8.38</hutool.version>
    </properties>

    <dependencies>
        <!-- Spring MVC Web 开发依赖，提供 Controller、内嵌 Tomcat、HTTP 消息转换等能力 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- 参数校验依赖，用于 DTO 字段校验 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <!-- Fastjson2 核心依赖，提供 JSON 序列化、反序列化、JSONObject、JSONArray 等能力 -->
        <dependency>
            <groupId>com.alibaba.fastjson2</groupId>
            <artifactId>fastjson2</artifactId>
            <version>${fastjson2.version}</version>
        </dependency>

        <!-- Fastjson2 Spring 6 扩展，适配 Spring Boot 3 / Spring Framework 6 -->
        <dependency>
            <groupId>com.alibaba.fastjson2</groupId>
            <artifactId>fastjson2-extension-spring6</artifactId>
            <version>${fastjson2.version}</version>
        </dependency>

        <!-- Hutool 工具类库，用于字符串、集合、日期、Bean 等常用处理 -->
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
            <version>${hutool.version}</version>
        </dependency>

        <!-- Lombok 用于简化 DTO、VO、配置类等样板代码 -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Spring Boot 测试依赖，用于单元测试和接口测试 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <!-- Spring Boot Maven 插件，用于打包可执行 Jar -->
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>

            <!-- Maven 编译插件，确保源码和目标字节码版本一致 -->
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

如果项目只需要在业务代码中手动调用 Fastjson2 API，可以只引入 `fastjson2`。如果需要将 Fastjson2 接入 Spring MVC 的请求和响应转换流程，则需要同时引入 `fastjson2-extension-spring6`。

### 项目基础结构

项目建议按照 Spring Boot 常见分层方式组织，将配置、接口、请求对象、响应对象、通用响应封装和测试代码分开管理。这样后续扩展全局 JSON 转换器、统一异常处理和接口测试时会更加清晰。

推荐项目结构如下：

```text
spring-boot-fastjson2-demo
├── pom.xml
├── src
│   ├── main
│   │   ├── java
│   │   │   └── io
│   │   │       └── github
│   │   │           └── atengk
│   │   │               └── fastjson2
│   │   │                   ├── Fastjson2Application.java
│   │   │                   ├── config
│   │   │                   │   └── Fastjson2WebMvcConfig.java
│   │   │                   ├── controller
│   │   │                   │   └── UserController.java
│   │   │                   ├── dto
│   │   │                   │   └── UserCreateDTO.java
│   │   │                   ├── vo
│   │   │                   │   └── UserVO.java
│   │   │                   └── common
│   │   │                       └── R.java
│   │   └── resources
│   │       └── application.yml
│   └── test
│       └── java
│           └── io
│               └── github
│                   └── atengk
│                       └── fastjson2
│                           └── Fastjson2ApplicationTests.java
```

目录说明如下：

| 路径                        | 作用                                                   |
| --------------------------- | ------------------------------------------------------ |
| `config`                    | 存放 Fastjson2 全局配置、WebMvc 配置、消息转换器配置   |
| `controller`                | 存放 Web 接口，用于验证请求反序列化和响应序列化        |
| `dto`                       | 存放请求参数对象，配合 `jakarta.validation` 做参数校验 |
| `vo`                        | 存放响应视图对象，控制接口返回字段                     |
| `common`                    | 存放统一响应对象、常量、通用工具类                     |
| `resources/application.yml` | 存放应用名称、端口、日志级别等基础配置                 |
| `test`                      | 存放单元测试、MockMvc 接口测试和 JSON 转换测试         |

项目启动类如下，用于启动 Spring Boot 应用。

文件位置：`src/main/java/io/github/atengk/fastjson2/Fastjson2Application.java`

```java
package io.github.atengk.fastjson2;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot 集成 Fastjson2 示例应用
 *
 * @author Ateng
 * @since 2026-05-09
 */
@SpringBootApplication
public class Fastjson2Application {

    /**
     * 应用启动入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(Fastjson2Application.class, args);
    }

}
```

基础配置文件如下，用于声明应用端口、应用名称和日志级别。

文件位置：`src/main/resources/application.yml`

```yaml
server:
  # 应用启动端口
  port: 8080

spring:
  application:
    # 应用名称
    name: spring-boot-fastjson2-demo

logging:
  level:
    # 示例项目包日志级别
    io.github.atengk.fastjson2: info
```

项目结构和基础依赖准备完成后，可以执行以下命令验证项目是否能够正常编译和测试。

```bash
mvn clean test
```

该命令会清理历史编译产物并执行测试。如果 JDK 版本、Maven 配置或依赖版本存在问题，通常会在该阶段暴露出来。确认编译通过后，即可继续编写 Fastjson2 全局配置、DTO/VO、Controller 接口和序列化测试内容。



## 基础用法

本节主要说明 Fastjson2 在普通 Java 代码中的基础使用方式，包括对象序列化、对象反序列化、JSON 字符串与 Java 对象转换、JSON 数组处理等内容。Fastjson2 常用 API 包括 `JSON.toJSONString`、`JSON.parseObject`、`JSON.parseArray`、`JSONObject` 和 `JSONArray`，官方 README 也使用这些 API 展示 JavaBean、JSONObject、JSONArray 的转换方式。([GitHub](https://github.com/alibaba/fastjson2?utm_source=chatgpt.com))

### 对象序列化

对象序列化是指将 Java 对象转换为 JSON 字符串，常用于接口响应、日志记录、缓存存储、MQ 消息发送等场景。Fastjson2 可以通过 `JSON.toJSONString` 完成对象序列化，也可以通过 `JSONWriter.Feature` 控制序列化行为；官方 Features 文档说明，`JSONWriter.Feature` 用于配置序列化行为，`JSONReader.Feature` 用于配置反序列化行为。([Alibaba Open Source](https://alibaba.github.io/fastjson2/features_cn.html?utm_source=chatgpt.com))

下面的 VO 用于作为基础用法和接口返回示例对象。

文件位置：`src/main/java/io/github/atengk/fastjson2/vo/UserVO.java`

```java
package io.github.atengk.fastjson2.vo;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户响应对象
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserVO {

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 年龄
     */
    private Integer age;

    /**
     * 创建时间
     */
    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 角色列表
     */
    private List<String> roleList;

}
```

下面的测试类用于演示对象序列化、反序列化、泛型转换和 JSON 数组处理。

文件位置：`src/test/java/io/github/atengk/fastjson2/Fastjson2BasicUsageTest.java`

```java
package io.github.atengk.fastjson2;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.TypeReference;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;
import io.github.atengk.fastjson2.common.R;
import io.github.atengk.fastjson2.vo.UserVO;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Fastjson2 基础用法测试
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
class Fastjson2BasicUsageTest {

    /**
     * 构建测试用户对象
     *
     * @return 用户响应对象
     */
    private UserVO buildUser() {
        return new UserVO(
                1L,
                "ateng",
                18,
                LocalDateTime.of(2026, 5, 9, 10, 30, 0),
                CollUtil.newArrayList("admin", "user")
        );
    }

    /**
     * 对象序列化为 JSON 字符串
     */
    @Test
    void serializeObject() {
        UserVO user = buildUser();

        String json = JSON.toJSONString(user, JSONWriter.Feature.WriteNulls);

        log.info("对象序列化结果：{}", json);
        Assertions.assertTrue(StrUtil.isNotBlank(json));
        Assertions.assertTrue(JSON.isValid(json));
    }

    /**
     * JSON 字符串反序列化为 Java 对象
     */
    @Test
    void deserializeObject() {
        String json = """
                {
                  "id": 1,
                  "username": "ateng",
                  "age": 18,
                  "createTime": "2026-05-09 10:30:00",
                  "roleList": ["admin", "user"]
                }
                """;

        UserVO user = JSON.parseObject(json, UserVO.class, JSONReader.Feature.UseBigDecimalForDoubles);

        log.info("对象反序列化结果：{}", user);
        Assertions.assertEquals("ateng", user.getUsername());
        Assertions.assertEquals(2, user.getRoleList().size());
    }

    /**
     * JSON 字符串与泛型 Java 对象转换
     */
    @Test
    void convertGenericObject() {
        R<UserVO> result = R.ok(buildUser());
        String json = JSON.toJSONString(result);

        R<UserVO> parsedResult = JSON.parseObject(json, new TypeReference<R<UserVO>>() {
        }.getType());

        log.info("泛型对象转换结果：{}", parsedResult);
        Assertions.assertEquals(200, parsedResult.getCode());
        Assertions.assertEquals("ateng", parsedResult.getData().getUsername());
    }

    /**
     * JSON 数组转换为 Java 集合
     */
    @Test
    void parseJsonArray() {
        String jsonArrayText = """
                [
                  {
                    "id": 1,
                    "username": "ateng",
                    "age": 18,
                    "createTime": "2026-05-09 10:30:00",
                    "roleList": ["admin", "user"]
                  },
                  {
                    "id": 2,
                    "username": "tom",
                    "age": 20,
                    "createTime": "2026-05-09 11:00:00",
                    "roleList": ["user"]
                  }
                ]
                """;

        List<UserVO> userList = JSON.parseArray(jsonArrayText, UserVO.class);

        log.info("JSON 数组转换结果：{}", userList);
        Assertions.assertEquals(2, userList.size());
        Assertions.assertEquals("tom", userList.get(1).getUsername());
    }

    /**
     * JSONObject 与 Java 对象互相转换
     */
    @Test
    void convertJsonObject() {
        UserVO user = buildUser();

        JSONObject jsonObject = JSON.parseObject(JSON.toJSONString(user));
        UserVO parsedUser = jsonObject.toJavaObject(UserVO.class);

        log.info("JSONObject 转换结果：{}", parsedUser);
        Assertions.assertEquals(user.getUsername(), parsedUser.getUsername());
    }

    /**
     * JSONArray 与 Java 集合互相转换
     */
    @Test
    void convertJsonArray() {
        List<UserVO> userList = CollUtil.newArrayList(buildUser());
        JSONArray jsonArray = JSONArray.ofAll(userList);

        List<UserVO> parsedList = jsonArray.toJavaList(UserVO.class);

        log.info("JSONArray 转换结果：{}", parsedList);
        Assertions.assertFalse(parsedList.isEmpty());
        Assertions.assertEquals("ateng", parsedList.get(0).getUsername());
    }

}
```

### 对象反序列化

对象反序列化是指将 JSON 字符串转换为 Java 对象。接口请求体、第三方接口响应、缓存字符串、MQ 消息体等场景通常都需要反序列化。

常用写法如下：

```java
String json = """
        {
          "id": 1,
          "username": "ateng",
          "age": 18,
          "createTime": "2026-05-09 10:30:00",
          "roleList": ["admin", "user"]
        }
        """;

UserVO user = JSON.parseObject(json, UserVO.class);
```

如果 JSON 字符串来源于外部系统，建议在转换前做基础合法性校验：

```java
if (StrUtil.isBlank(json) || !JSON.isValid(json)) {
    log.warn("JSON 字符串格式非法，json={}", json);
    throw new IllegalArgumentException("JSON 字符串格式非法");
}

UserVO user = JSON.parseObject(json, UserVO.class);
```

这种方式适合在业务代码中手动解析外部 JSON 数据。如果是 Controller 接收 `@RequestBody`，则通常交给 Spring MVC 的消息转换器自动完成反序列化。

### JSON 字符串与 Java 对象转换

JSON 字符串与 Java 对象转换通常包括三类情况：普通对象转换、泛型对象转换和动态结构转换。

普通对象转换适用于字段结构固定的场景：

```java
UserVO user = JSON.parseObject(json, UserVO.class);
String jsonText = JSON.toJSONString(user);
```

泛型对象转换适用于统一返回结果、分页对象、集合包装对象等场景：

```java
R<UserVO> result = JSON.parseObject(json, new TypeReference<R<UserVO>>() {
}.getType());
```

动态结构转换适用于字段不固定、只需要读取部分字段或临时处理 JSON 的场景：

```java
JSONObject jsonObject = JSON.parseObject(json);

Long id = jsonObject.getLong("id");
String username = jsonObject.getString("username");

log.info("读取 JSON 动态字段，id={}，username={}", id, username);
```

如果业务字段明确，建议优先转换为 DTO、VO 或业务对象，减少大量 `JSONObject.getXxx` 带来的字段名散落问题。只有在字段结构不固定、透传 JSON 或处理第三方动态字段时，才建议使用 `JSONObject`。

### JSON 数组处理

JSON 数组处理常用于批量接口、列表响应、缓存集合、消息批量消费等场景。Fastjson2 可以通过 `JSON.parseArray` 将 JSON 数组字符串转换为 Java 集合。

```java
String jsonArrayText = """
        [
          {"id": 1, "username": "ateng"},
          {"id": 2, "username": "tom"}
        ]
        """;

List<UserVO> userList = JSON.parseArray(jsonArrayText, UserVO.class);
```

如果需要先以动态数组形式处理，再转换为 Java 集合，可以使用 `JSONArray`：

```java
JSONArray jsonArray = JSON.parseArray(jsonArrayText);

List<UserVO> userList = jsonArray.toJavaList(UserVO.class);

log.info("JSON 数组转换完成，数量={}", userList.size());
```

在实际开发中，如果数组元素结构固定，建议直接使用 `JSON.parseArray(jsonArrayText, UserVO.class)`。如果数组元素结构不固定，或者需要对数组元素做动态字段判断，可以先转换为 `JSONArray` 再逐项处理。

## Spring Boot 3 集成配置

本节说明如何在 Spring Boot 3 中将 Fastjson2 配置为全局 JSON 转换器，使 Controller 的 `@RequestBody` 和 `@ResponseBody` 自动使用 Fastjson2 进行 JSON 反序列化和序列化。Fastjson2 官方 Spring 集成文档说明，Spring Framework 支持被独立到 extension 包中，Spring 6 应使用 `fastjson2-extension-spring6`，并可通过 `FastJsonHttpMessageConverter` 和 `FastJsonConfig` 配置 Spring MVC 的 JSON 转换行为。([Alibaba Open Source](https://alibaba.github.io/fastjson2/Spring/spring_support_cn?utm_source=chatgpt.com))

### 全局 JSON 转换器配置

全局 JSON 转换器配置用于接管 Spring MVC 的 JSON 读写过程。配置完成后，Controller 中的 `@RequestBody` 请求参数会通过 Fastjson2 反序列化，接口返回对象会通过 Fastjson2 序列化为 JSON。

下面的配置类将 Fastjson2 消息转换器添加到 Spring MVC 转换器列表的前面，并统一配置字符集、日期格式、序列化特性和反序列化特性。Fastjson2 官方文档中 `FastJsonConfig` 支持配置 `charset`、`dateFormat`、`writerFeatures`、`readerFeatures` 等参数。([Alibaba Open Source](https://alibaba.github.io/fastjson2/Spring/spring_support_cn?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/fastjson2/config/Fastjson2WebMvcConfig.java`

```java
package io.github.atengk.fastjson2.config;

import cn.hutool.core.date.DatePattern;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.support.config.FastJsonConfig;
import com.alibaba.fastjson2.support.spring6.http.converter.FastJsonHttpMessageConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Fastjson2 Web MVC 全局配置
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Configuration
public class Fastjson2WebMvcConfig implements WebMvcConfigurer {

    /**
     * 扩展 Spring MVC 消息转换器
     *
     * @param converters 消息转换器列表
     */
    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        FastJsonHttpMessageConverter converter = new FastJsonHttpMessageConverter();

        FastJsonConfig fastJsonConfig = new FastJsonConfig();
        fastJsonConfig.setCharset(StandardCharsets.UTF_8);
        fastJsonConfig.setDateFormat(DatePattern.NORM_DATETIME_PATTERN);

        // 序列化特性：输出 null 字段，便于前端获得稳定字段结构
        fastJsonConfig.setWriterFeatures(
                JSONWriter.Feature.WriteNulls
        );

        // 反序列化特性：小数按 BigDecimal 读取，避免精度问题
        fastJsonConfig.setReaderFeatures(
                JSONReader.Feature.UseBigDecimalForDoubles
        );

        converter.setFastJsonConfig(fastJsonConfig);
        converter.setSupportedMediaTypes(List.of(MediaType.APPLICATION_JSON));

        // 放到最前面，优先使用 Fastjson2 处理 application/json
        converters.add(0, converter);

        log.info("Fastjson2 全局 JSON 转换器配置完成");
    }

}
```

这里使用 `extendMessageConverters` 而不是直接继承 `WebMvcConfigurationSupport`，可以保留 Spring Boot MVC 的自动配置能力，只是在已有转换器基础上扩展 Fastjson2 转换器。对于普通 Spring Boot 3 Web 项目，这种方式更容易与静态资源、参数解析、拦截器、默认转换器等配置共存。

### HTTP 请求参数反序列化

HTTP 请求参数反序列化是指 Spring MVC 将请求体中的 JSON 自动转换为 Java DTO 对象。配置 Fastjson2 全局转换器后，`@RequestBody` 标注的入参会通过 Fastjson2 转换。

下面的 DTO 用于接收创建用户的 JSON 请求体，并结合 `jakarta.validation` 完成参数校验。

文件位置：`src/main/java/io/github/atengk/fastjson2/dto/UserCreateDTO.java`

```java
package io.github.atengk.fastjson2.dto;

import com.alibaba.fastjson2.annotation.JSONField;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户创建请求参数
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
public class UserCreateDTO {

    /**
     * 用户名
     */
    @NotBlank(message = "用户名不能为空")
    private String username;

    /**
     * 年龄
     */
    @NotNull(message = "年龄不能为空")
    @Min(value = 0, message = "年龄不能小于0")
    @Max(value = 150, message = "年龄不能大于150")
    private Integer age;

    /**
     * 创建时间
     */
    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 角色列表
     */
    private List<String> roleList;

}
```

统一响应对象用于封装接口返回结果。

文件位置：`src/main/java/io/github/atengk/fastjson2/common/R.java`

```java
package io.github.atengk.fastjson2.common;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 统一响应结果
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class R<T> {

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
     * 响应时间
     */
    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;

    /**
     * 成功响应
     *
     * @param data 响应数据
     * @param <T>  数据类型
     * @return 统一响应结果
     */
    public static <T> R<T> ok(T data) {
        return new R<>(200, "操作成功", data, LocalDateTime.now());
    }

    /**
     * 失败响应
     *
     * @param code    状态码
     * @param message 响应消息
     * @param <T>     数据类型
     * @return 统一响应结果
     */
    public static <T> R<T> fail(Integer code, String message) {
        return new R<>(code, message, null, LocalDateTime.now());
    }

}
```

下面的 Controller 用于验证 Fastjson2 对 HTTP 请求体的反序列化效果。

文件位置：`src/main/java/io/github/atengk/fastjson2/controller/UserController.java`

```java
package io.github.atengk.fastjson2.controller;

import cn.hutool.core.collection.CollUtil;
import io.github.atengk.fastjson2.common.R;
import io.github.atengk.fastjson2.dto.UserCreateDTO;
import io.github.atengk.fastjson2.vo.UserVO;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
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
@RequestMapping("/users")
public class UserController {

    /**
     * 创建用户
     *
     * @param dto 用户创建请求参数
     * @return 用户响应结果
     */
    @PostMapping
    public R<UserVO> create(@Valid @RequestBody UserCreateDTO dto) {
        if (CollUtil.isEmpty(dto.getRoleList())) {
            dto.setRoleList(CollUtil.newArrayList("user"));
        }

        LocalDateTime createTime = dto.getCreateTime() == null ? LocalDateTime.now() : dto.getCreateTime();

        UserVO userVO = new UserVO(
                1L,
                dto.getUsername(),
                dto.getAge(),
                createTime,
                dto.getRoleList()
        );

        log.info("创建用户请求反序列化完成，username={}，roleList={}", dto.getUsername(), dto.getRoleList());
        return R.ok(userVO);
    }

    /**
     * 查询用户详情
     *
     * @param id 用户ID
     * @return 用户响应结果
     */
    @GetMapping("/{id}")
    public R<UserVO> detail(@PathVariable Long id) {
        UserVO userVO = new UserVO(
                id,
                "ateng",
                18,
                LocalDateTime.now(),
                CollUtil.newArrayList("admin", "user")
        );

        log.info("查询用户详情，id={}", id);
        return R.ok(userVO);
    }

}
```

请求示例：

```bash
curl -X POST "http://localhost:8080/users" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "ateng",
    "age": 18,
    "createTime": "2026-05-09 10:30:00",
    "roleList": ["admin", "user"]
  }'
```

如果接口能够正常接收请求体，并且日志输出了 `创建用户请求反序列化完成`，说明 Fastjson2 已参与 HTTP 请求体反序列化流程。

### HTTP 响应结果序列化

HTTP 响应结果序列化是指 Controller 返回 Java 对象后，Spring MVC 将返回值转换为 JSON 响应体。配置 Fastjson2 全局转换器后，`@RestController` 或 `@ResponseBody` 返回的对象会通过 Fastjson2 输出为 JSON。

查询接口示例：

```bash
curl -X GET "http://localhost:8080/users/1"
```

响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 1,
    "username": "ateng",
    "age": 18,
    "createTime": "2026-05-09 12:00:00",
    "roleList": [
      "admin",
      "user"
    ]
  },
  "timestamp": "2026-05-09 12:00:00"
}
```

这里的 `UserVO`、`R` 都是普通 Java 对象，Controller 不需要手动调用 `JSON.toJSONString`。Spring MVC 会根据响应类型和 `Content-Type` 自动选择消息转换器。如果 Fastjson2 转换器位于转换器列表前面，并且支持 `application/json`，响应结果就会由 Fastjson2 完成序列化。

### 日期时间格式配置

日期时间格式配置主要用于统一接口中的 `LocalDateTime`、`LocalDate`、`Date` 等字段展示格式，避免前后端因日期格式不一致产生解析问题。

在全局配置中，可以通过 `FastJsonConfig#setDateFormat` 设置默认日期时间格式。官方 Spring 集成文档中的示例也使用 `config.setDateFormat("yyyy-MM-dd HH:mm:ss")` 配置日期格式。([Alibaba Open Source](https://alibaba.github.io/fastjson2/Spring/spring_support_cn?utm_source=chatgpt.com))

全局配置示例：

```java
FastJsonConfig fastJsonConfig = new FastJsonConfig();
fastJsonConfig.setDateFormat(DatePattern.NORM_DATETIME_PATTERN);
```

对于关键字段，建议在 DTO 或 VO 字段上使用 `@JSONField(format = "yyyy-MM-dd HH:mm:ss")` 明确声明格式：

```java
@JSONField(format = "yyyy-MM-dd HH:mm:ss")
private LocalDateTime createTime;
```

推荐规范如下：

| 字段类型        | 推荐格式              | 示例                  |
| --------------- | --------------------- | --------------------- |
| `LocalDateTime` | `yyyy-MM-dd HH:mm:ss` | `2026-05-09 10:30:00` |
| `LocalDate`     | `yyyy-MM-dd`          | `2026-05-09`          |
| `LocalTime`     | `HH:mm:ss`            | `10:30:00`            |
| `Date`          | `yyyy-MM-dd HH:mm:ss` | `2026-05-09 10:30:00` |

实际项目中建议同时采用两层控制。第一层是在全局 Fastjson2 配置中设置统一默认格式；第二层是在关键 DTO、VO 字段上使用 `@JSONField(format = "...")` 显式约束字段格式。这样即使后续全局配置调整，关键接口字段也能保持稳定输出。



## 常用特性

本节主要说明 Fastjson2 在 Spring Boot 3 项目中常用的字段映射、空值处理、枚举处理、泛型转换和字段过滤能力。Fastjson2 的 `@JSONField` 可用于配置字段名称、日期格式、是否参与序列化或反序列化、反序列化别名、序列化特性等能力。([Javadoc](https://javadoc.io/static/com.alibaba.fastjson2/fastjson2/2.0.25/com/alibaba/fastjson2/annotation/JSONField.html?utm_source=chatgpt.com))

### 字段名称映射

字段名称映射用于解决 Java 字段名和 JSON 字段名不一致的问题。典型场景包括前端使用下划线命名、第三方接口字段不可控、老接口字段需要兼容等。

Fastjson2 可以通过 `@JSONField(name = "...")` 指定 JSON 字段名，也可以通过 `alternateNames` 指定反序列化时兼容的多个字段名。`JSONField.name` 用于配置序列化输出字段名和反序列化映射字段名，`alternateNames` 可用于反序列化时兼容多个不同字段名。([Javadoc](https://javadoc.io/static/com.alibaba.fastjson2/fastjson2/2.0.25/com/alibaba/fastjson2/annotation/JSONField.html?utm_source=chatgpt.com))

下面的 DTO 演示字段名称映射和多字段名兼容。

文件位置：`src/main/java/io/github/atengk/fastjson2/dto/UserFeatureDTO.java`

```java
package io.github.atengk.fastjson2.dto;

import com.alibaba.fastjson2.annotation.JSONField;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户特性请求参数
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
public class UserFeatureDTO {

    /**
     * 用户名
     */
    @NotBlank(message = "用户名不能为空")
    @JSONField(name = "user_name", alternateNames = {"username", "userName"})
    private String username;

    /**
     * 年龄
     */
    @NotNull(message = "年龄不能为空")
    private Integer age;

    /**
     * 创建时间
     */
    @JSONField(name = "create_time", format = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

}
```

请求 JSON 可以使用标准字段名：

```json
{
  "user_name": "ateng",
  "age": 18,
  "create_time": "2026-05-09 10:30:00"
}
```

也可以兼容历史字段名：

```json
{
  "userName": "ateng",
  "age": 18,
  "create_time": "2026-05-09 10:30:00"
}
```

字段名称映射建议集中在 DTO 和 VO 层处理，不建议在业务代码中大量手动读取 `JSONObject.getString("xxx")`。如果字段结构固定，使用强类型对象更利于参数校验、接口文档生成和后续重构。

### 空值字段处理

空值字段处理用于控制 `null` 字段在 JSON 输出中的表现。常见策略包括输出 `null`、字符串空值输出为空字符串、集合空值输出为空数组、数值空值输出为 `0`、布尔空值输出为 `false` 等。Fastjson2 的 `JSONWriter.Feature` 提供了 `WriteNulls`、`WriteNullListAsEmpty`、`WriteNullStringAsEmpty`、`WriteNullNumberAsZero`、`WriteNullBooleanAsFalse` 等特性。([Fossies](https://fossies.org/linux/misc/fastjson2-2.0.60.tar.gz/fastjson2-2.0.60/docs/features_en.md?utm_source=chatgpt.com))

如果希望全局统一空值输出规则，可以在前文的 Fastjson2 Web MVC 配置中调整 `writerFeatures`。

文件位置：`src/main/java/io/github/atengk/fastjson2/config/Fastjson2WebMvcConfig.java`

```java
fastJsonConfig.setWriterFeatures(
        // 输出 null 字段
        JSONWriter.Feature.WriteNulls,

        // String 类型 null 输出为空字符串
        JSONWriter.Feature.WriteNullStringAsEmpty,

        // List 类型 null 输出为空数组
        JSONWriter.Feature.WriteNullListAsEmpty,

        // Number 类型 null 输出为 0
        JSONWriter.Feature.WriteNullNumberAsZero,

        // Boolean 类型 null 输出为 false
        JSONWriter.Feature.WriteNullBooleanAsFalse
);
```

如果只希望局部字段生效，可以在字段上使用 `@JSONField(serializeFeatures = ...)`。

下面的 VO 演示局部空值处理。

文件位置：`src/main/java/io/github/atengk/fastjson2/vo/UserNullFeatureVO.java`

```java
package io.github.atengk.fastjson2.vo;

import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 用户空值特性响应对象
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserNullFeatureVO {

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 昵称，null 时输出为空字符串
     */
    @JSONField(serializeFeatures = JSONWriter.Feature.WriteNullStringAsEmpty)
    private String nickname;

    /**
     * 角色列表，null 时输出为空数组
     */
    @JSONField(serializeFeatures = JSONWriter.Feature.WriteNullListAsEmpty)
    private List<String> roleList;

}
```

示例代码如下：

```java
UserNullFeatureVO vo = new UserNullFeatureVO(1L, null, null);

String json = JSON.toJSONString(
        vo,
        JSONWriter.Feature.WriteNulls,
        JSONWriter.Feature.WriteNullStringAsEmpty,
        JSONWriter.Feature.WriteNullListAsEmpty
);

log.info("空值字段处理结果：{}", json);
```

输出示例：

```json
{
  "id": 1,
  "nickname": "",
  "roleList": []
}
```

接口响应字段是否输出 `null` 需要按团队规范统一。管理后台接口通常可以输出稳定字段结构；开放接口则应谨慎调整空值策略，避免影响调用方对字段是否存在的判断。

### 枚举类型处理

枚举类型处理常用于用户状态、订单状态、性别、启用禁用状态、业务类型等字段。Fastjson2 默认可以处理 Java 枚举类型，常规做法是请求中传入枚举名称，例如 `MALE`、`FEMALE`、`ENABLE`、`DISABLE`。如果需要改变枚举序列化行为，也可以使用 `JSONWriter.Feature.WriteEnumUsingToString` 等特性；Fastjson2 Features 文档说明该特性会使用枚举的 `toString` 方法进行序列化。([GitHub](https://github.com/alibaba/fastjson2/wiki/Features_cn?utm_source=chatgpt.com))

实际接口开发中，建议区分 Java 内部枚举值和前端展示字段。请求 DTO 中可以使用枚举类型接收固定值，响应 VO 中可以输出 `code` 和 `name`，避免前端直接依赖 Java 枚举名称。

下面定义用户性别枚举。

文件位置：`src/main/java/io/github/atengk/fastjson2/enums/GenderEnum.java`

```java
package io.github.atengk.fastjson2.enums;

import cn.hutool.core.util.StrUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 性别枚举
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Getter
@AllArgsConstructor
public enum GenderEnum {

    /**
     * 男
     */
    MALE("male", "男"),

    /**
     * 女
     */
    FEMALE("female", "女"),

    /**
     * 未知
     */
    UNKNOWN("unknown", "未知");

    /**
     * 编码
     */
    private final String code;

    /**
     * 名称
     */
    private final String name;

    /**
     * 根据编码获取枚举
     *
     * @param code 编码
     * @return 性别枚举
     */
    public static GenderEnum ofCode(String code) {
        return Arrays.stream(values())
                .filter(item -> StrUtil.equals(item.getCode(), code))
                .findFirst()
                .orElse(UNKNOWN);
    }

    /**
     * 输出枚举编码
     *
     * @return 枚举编码
     */
    @Override
    public String toString() {
        return this.code;
    }

}
```

在 DTO 中可以直接使用枚举字段。

```java
/**
 * 性别
 */
@NotNull(message = "性别不能为空")
private GenderEnum gender;
```

请求示例：

```json
{
  "username": "ateng",
  "age": 18,
  "gender": "MALE"
}
```

如果希望序列化时输出枚举 `toString()` 的结果，可以使用：

```java
String json = JSON.toJSONString(GenderEnum.MALE, JSONWriter.Feature.WriteEnumUsingToString);

log.info("枚举序列化结果：{}", json);
```

在对外接口中，更推荐响应 VO 明确输出 `genderCode` 和 `genderName`：

```java
vo.setGenderCode(gender.getCode());
vo.setGenderName(gender.getName());
```

这种方式比直接把枚举对象暴露给前端更稳定，也更适合接口长期兼容。

### 泛型对象转换

泛型对象转换常用于统一响应对象、分页对象、树结构对象、批量导入结果、包装集合等场景。由于 Java 泛型存在类型擦除，反序列化泛型对象时不能只传入 `R.class`，否则内部的 `data` 类型可能会被解析为 `JSONObject`。

Fastjson2 可以使用 `TypeReference` 保留泛型类型信息。

下面定义分页响应对象。

文件位置：`src/main/java/io/github/atengk/fastjson2/common/PageResult.java`

```java
package io.github.atengk.fastjson2.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 分页响应结果
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {

    /**
     * 当前页码
     */
    private Long current;

    /**
     * 每页条数
     */
    private Long size;

    /**
     * 总条数
     */
    private Long total;

    /**
     * 数据列表
     */
    private List<T> records;

}
```

泛型转换示例：

```java
PageResult<UserVO> pageResult = new PageResult<>(
        1L,
        10L,
        1L,
        CollUtil.newArrayList(userVO)
);

R<PageResult<UserVO>> result = R.ok(pageResult);

String json = JSON.toJSONString(result);

R<PageResult<UserVO>> parsedResult = JSON.parseObject(
        json,
        new TypeReference<R<PageResult<UserVO>>>() {
        }.getType()
);

log.info("泛型对象转换结果：{}", parsedResult);
```

泛型对象转换时建议遵循两个原则。第一，简单对象使用 `JSON.parseObject(json, UserVO.class)`；第二，带泛型的对象使用 `TypeReference`，不要直接使用原始类型反序列化。

### 忽略字段与字段过滤

忽略字段用于控制某些字段不参与序列化或不参与反序列化。典型场景包括密码、密钥、内部备注、逻辑删除标记、内部计算字段等。`@JSONField` 提供了 `serialize` 和 `deserialize` 属性，可分别控制字段是否参与序列化和反序列化。([Javadoc](https://javadoc.io/static/com.alibaba.fastjson2/fastjson2/2.0.25/com/alibaba/fastjson2/annotation/JSONField.html?utm_source=chatgpt.com))

下面的对象演示敏感字段忽略。

文件位置：`src/main/java/io/github/atengk/fastjson2/vo/UserSecureVO.java`

```java
package io.github.atengk.fastjson2.vo;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户安全响应对象
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSecureVO {

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码摘要，不允许输出到 JSON
     */
    @JSONField(serialize = false)
    private String passwordHash;

    /**
     * 内部备注，不允许从 JSON 入参反序列化
     */
    @JSONField(deserialize = false)
    private String internalRemark;

}
```

如果需要在某个特定场景下动态控制输出字段，可以使用字段过滤器。例如，列表接口只输出 `id` 和 `username`，详情接口再输出完整字段。

```java
UserSecureVO vo = new UserSecureVO(1L, "ateng", "hash-value", "内部备注");

SimplePropertyPreFilter filter = new SimplePropertyPreFilter(UserSecureVO.class, "id", "username");

String json = JSON.toJSONString(vo, filter);

log.info("字段过滤结果：{}", json);
```

字段忽略适合用于固定安全规则，例如密码永远不输出。字段过滤适合用于动态场景，例如列表、详情、导出、日志脱敏等不同输出需求。

## Web 接口开发

本节给出一个完整的 Web 接口开发示例，包括请求 DTO、响应 VO、Controller 接口和统一返回结果封装。示例基于前文的 Fastjson2 全局转换器配置，Controller 不需要手动调用 `JSON.toJSONString`，请求体和响应体会通过 Spring MVC 消息转换器自动完成 JSON 转换。

### 请求 DTO 定义

请求 DTO 用于接收前端或外部系统传入的 JSON 参数。DTO 应只包含接口需要的入参字段，并结合 `jakarta.validation` 完成参数校验。字段命名不一致时，可以使用 `@JSONField` 做字段映射。

文件位置：`src/main/java/io/github/atengk/fastjson2/dto/UserCreateDTO.java`

```java
package io.github.atengk.fastjson2.dto;

import com.alibaba.fastjson2.annotation.JSONField;
import io.github.atengk.fastjson2.enums.GenderEnum;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户创建请求参数
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
public class UserCreateDTO {

    /**
     * 用户名
     */
    @NotBlank(message = "用户名不能为空")
    @JSONField(name = "user_name", alternateNames = {"username", "userName"})
    private String username;

    /**
     * 年龄
     */
    @NotNull(message = "年龄不能为空")
    @Min(value = 0, message = "年龄不能小于0")
    @Max(value = 150, message = "年龄不能大于150")
    private Integer age;

    /**
     * 性别
     */
    @NotNull(message = "性别不能为空")
    private GenderEnum gender;

    /**
     * 创建时间
     */
    @JSONField(name = "create_time", format = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 角色列表
     */
    private List<String> roleList;

}
```

请求 DTO 建议只做参数承载和基础校验，不建议放业务逻辑。业务默认值可以在 Service 或 Controller 中处理，例如角色为空时默认设置为普通用户。

### 响应 VO 定义

响应 VO 用于控制接口返回给前端的字段。VO 不应直接复用数据库实体类，避免敏感字段、内部字段或无关字段被输出到接口响应中。

文件位置：`src/main/java/io/github/atengk/fastjson2/vo/UserVO.java`

```java
package io.github.atengk.fastjson2.vo;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户响应对象
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserVO {

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 用户名
     */
    @JSONField(name = "user_name")
    private String username;

    /**
     * 年龄
     */
    private Integer age;

    /**
     * 性别编码
     */
    private String genderCode;

    /**
     * 性别名称
     */
    private String genderName;

    /**
     * 创建时间
     */
    @JSONField(name = "create_time", format = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 角色列表
     */
    private List<String> roleList;

    /**
     * 内部备注，不输出到接口响应
     */
    @JSONField(serialize = false)
    private String internalRemark;

}
```

该 VO 中 `username` 会输出为 `user_name`，`createTime` 会输出为 `create_time`，`internalRemark` 不会输出到 JSON 响应中。这样可以在 Java 代码中继续使用驼峰命名，同时保证接口 JSON 字段符合前后端约定。

### Controller 接口示例

Controller 用于接收 HTTP 请求、触发 JSON 反序列化、返回统一 JSON 响应。下面的示例包含创建用户、查询详情和分页查询三个接口。

文件位置：`src/main/java/io/github/atengk/fastjson2/controller/UserController.java`

```java
package io.github.atengk.fastjson2.controller;

import cn.hutool.core.collection.CollUtil;
import io.github.atengk.fastjson2.common.PageResult;
import io.github.atengk.fastjson2.common.R;
import io.github.atengk.fastjson2.dto.UserCreateDTO;
import io.github.atengk.fastjson2.enums.GenderEnum;
import io.github.atengk.fastjson2.vo.UserVO;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户接口
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@RestController
@RequestMapping("/users")
public class UserController {

    /**
     * 创建用户
     *
     * @param dto 用户创建请求参数
     * @return 用户响应结果
     */
    @PostMapping
    public R<UserVO> create(@Valid @RequestBody UserCreateDTO dto) {
        List<String> roleList = CollUtil.isEmpty(dto.getRoleList())
                ? CollUtil.newArrayList("user")
                : dto.getRoleList();

        LocalDateTime createTime = dto.getCreateTime() == null
                ? LocalDateTime.now()
                : dto.getCreateTime();

        GenderEnum gender = dto.getGender();

        UserVO userVO = new UserVO(
                1L,
                dto.getUsername(),
                dto.getAge(),
                gender.getCode(),
                gender.getName(),
                createTime,
                roleList,
                "创建用户时生成的内部备注"
        );

        log.info("创建用户成功，username={}，gender={}，roleList={}", dto.getUsername(), gender.getCode(), roleList);
        return R.ok(userVO);
    }

    /**
     * 查询用户详情
     *
     * @param id 用户ID
     * @return 用户响应结果
     */
    @GetMapping("/{id}")
    public R<UserVO> detail(@PathVariable Long id) {
        GenderEnum gender = GenderEnum.MALE;

        UserVO userVO = new UserVO(
                id,
                "ateng",
                18,
                gender.getCode(),
                gender.getName(),
                LocalDateTime.now(),
                CollUtil.newArrayList("admin", "user"),
                "详情接口内部备注"
        );

        log.info("查询用户详情，id={}", id);
        return R.ok(userVO);
    }

    /**
     * 分页查询用户
     *
     * @param current 当前页码
     * @param size    每页条数
     * @return 用户分页响应结果
     */
    @GetMapping
    public R<PageResult<UserVO>> page(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size
    ) {
        GenderEnum gender = GenderEnum.MALE;

        UserVO userVO = new UserVO(
                1L,
                "ateng",
                18,
                gender.getCode(),
                gender.getName(),
                LocalDateTime.now(),
                CollUtil.newArrayList("admin", "user"),
                "分页接口内部备注"
        );

        PageResult<UserVO> pageResult = new PageResult<>(
                current,
                size,
                1L,
                CollUtil.newArrayList(userVO)
        );

        log.info("分页查询用户，current={}，size={}，total={}", current, size, pageResult.getTotal());
        return R.ok(pageResult);
    }

}
```

创建用户接口请求示例：

```bash
curl -X POST "http://localhost:8080/users" \
  -H "Content-Type: application/json" \
  -d '{
    "user_name": "ateng",
    "age": 18,
    "gender": "MALE",
    "create_time": "2026-05-09 10:30:00",
    "roleList": ["admin", "user"]
  }'
```

创建用户响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 1,
    "user_name": "ateng",
    "age": 18,
    "genderCode": "male",
    "genderName": "男",
    "create_time": "2026-05-09 10:30:00",
    "roleList": [
      "admin",
      "user"
    ]
  },
  "timestamp": "2026-05-09 12:00:00"
}
```

查询详情接口请求示例：

```bash
curl -X GET "http://localhost:8080/users/1"
```

分页查询接口请求示例：

```bash
curl -X GET "http://localhost:8080/users?current=1&size=10"
```

### 统一返回结果封装

统一返回结果用于规范接口响应结构，便于前端统一处理状态码、提示信息、业务数据和响应时间。常见字段包括 `code`、`message`、`data`、`timestamp`。如果项目需要链路追踪，也可以增加 `traceId`。

文件位置：`src/main/java/io/github/atengk/fastjson2/common/R.java`

```java
package io.github.atengk.fastjson2.common;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 统一响应结果
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class R<T> {

    /**
     * 成功状态码
     */
    public static final Integer SUCCESS_CODE = 200;

    /**
     * 失败状态码
     */
    public static final Integer FAIL_CODE = 500;

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
     * 响应时间
     */
    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;

    /**
     * 成功响应
     *
     * @param data 响应数据
     * @param <T>  数据类型
     * @return 统一响应结果
     */
    public static <T> R<T> ok(T data) {
        return new R<>(SUCCESS_CODE, "操作成功", data, LocalDateTime.now());
    }

    /**
     * 成功响应
     *
     * @param message 响应消息
     * @param data    响应数据
     * @param <T>     数据类型
     * @return 统一响应结果
     */
    public static <T> R<T> ok(String message, T data) {
        return new R<>(SUCCESS_CODE, message, data, LocalDateTime.now());
    }

    /**
     * 失败响应
     *
     * @param message 响应消息
     * @param <T>     数据类型
     * @return 统一响应结果
     */
    public static <T> R<T> fail(String message) {
        return new R<>(FAIL_CODE, message, null, LocalDateTime.now());
    }

    /**
     * 失败响应
     *
     * @param code    状态码
     * @param message 响应消息
     * @param <T>     数据类型
     * @return 统一响应结果
     */
    public static <T> R<T> fail(Integer code, String message) {
        return new R<>(code, message, null, LocalDateTime.now());
    }

}
```

统一返回结果的推荐响应结构如下：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {},
  "timestamp": "2026-05-09 12:00:00"
}
```

接口开发时建议遵循以下规则：

| 规则                              | 说明                                                         |
| --------------------------------- | ------------------------------------------------------------ |
| Controller 返回 `R<T>`            | 保持接口响应结构统一                                         |
| DTO 接收请求参数                  | 不直接使用实体类接收外部入参                                 |
| VO 返回响应数据                   | 不直接返回数据库实体类                                       |
| 枚举字段明确转换                  | 对外输出 `code`、`name`，减少前端对 Java 枚举名的依赖        |
| 敏感字段默认忽略                  | 密码、密钥、内部备注等字段使用 `@JSONField(serialize = false)` 或不放入 VO |
| 泛型响应使用 `TypeReference` 测试 | 测试反序列化时保留泛型类型信息                               |

到这里，常用特性和基础 Web 接口已经具备。后续可以继续扩展“高级配置”“数据校验与异常处理”和“测试验证”部分。



## 高级配置

本节主要说明 Fastjson2 在 Spring Boot 3 项目中的高级配置方式，包括序列化特性、反序列化特性、自定义 `ObjectWriter` 和自定义 `ObjectReader`。Fastjson2 使用 `JSONWriter.Feature` 配置序列化行为，使用 `JSONReader.Feature` 配置反序列化行为，这些特性既可以在 `JSON.toJSONString`、`JSON.parseObject` 中局部使用，也可以配置到 Spring MVC 的 `FastJsonConfig` 中作为全局规则。([GitHub](https://github.com/alibaba/fastjson2/wiki/Features_cn?utm_source=chatgpt.com))

### 序列化特性配置

序列化特性用于控制 Java 对象输出为 JSON 时的行为，例如是否输出 `null` 字段、空集合是否输出为 `[]`、枚举如何输出、大数字如何输出、字段顺序如何处理等。

Fastjson2 的序列化特性通过 `JSONWriter.Feature` 配置。常见使用方式有两种：一种是在业务代码中调用 `JSON.toJSONString` 时临时指定；另一种是在 Spring Boot 3 的全局 JSON 转换器中统一指定。

局部序列化配置示例：

```java
String json = JSON.toJSONString(
        userVO,
        JSONWriter.Feature.WriteNulls,
        JSONWriter.Feature.WriteNullStringAsEmpty,
        JSONWriter.Feature.WriteNullListAsEmpty
);
```

全局序列化配置建议写在前文的 `Fastjson2WebMvcConfig` 中。

文件位置：`src/main/java/io/github/atengk/fastjson2/config/Fastjson2WebMvcConfig.java`

```java
fastJsonConfig.setWriterFeatures(
        // 输出 null 字段，保证响应字段结构稳定
        JSONWriter.Feature.WriteNulls,

        // String 类型 null 输出为空字符串
        JSONWriter.Feature.WriteNullStringAsEmpty,

        // List 类型 null 输出为空数组
        JSONWriter.Feature.WriteNullListAsEmpty,

        // Number 类型 null 输出为 0
        JSONWriter.Feature.WriteNullNumberAsZero,

        // Boolean 类型 null 输出为 false
        JSONWriter.Feature.WriteNullBooleanAsFalse
);
```

常用序列化特性说明如下：

| 特性                      | 作用                       | 适用场景               |
| ------------------------- | -------------------------- | ---------------------- |
| `WriteNulls`              | 输出 `null` 字段           | 前端需要稳定字段结构   |
| `WriteNullStringAsEmpty`  | 字符串 `null` 输出为 `""`  | 表单展示、管理后台接口 |
| `WriteNullListAsEmpty`    | 集合 `null` 输出为 `[]`    | 列表字段避免前端空指针 |
| `WriteNullNumberAsZero`   | 数值 `null` 输出为 `0`     | 统计类字段、计数字段   |
| `WriteNullBooleanAsFalse` | 布尔 `null` 输出为 `false` | 开关类字段             |
| `WriteEnumUsingToString`  | 枚举使用 `toString()` 输出 | 需要输出枚举业务编码   |
| `PrettyFormat`            | 格式化输出 JSON            | 调试、日志、测试验证   |

实际业务接口中不建议无脑开启所有空值转换规则。例如金额、库存、状态等字段的 `null` 和 `0` 可能具有不同业务含义，是否启用 `WriteNullNumberAsZero` 需要根据接口规范决定。

下面给出一个独立测试示例，用于验证序列化特性。

文件位置：`src/test/java/io/github/atengk/fastjson2/Fastjson2AdvancedFeatureTest.java`

```java
package io.github.atengk.fastjson2;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;
import io.github.atengk.fastjson2.enums.GenderEnum;
import io.github.atengk.fastjson2.vo.UserNullFeatureVO;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Fastjson2 高级特性测试
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
class Fastjson2AdvancedFeatureTest {

    /**
     * 验证空值字段序列化特性
     */
    @Test
    void serializeNullFeature() {
        UserNullFeatureVO vo = new UserNullFeatureVO(1L, null, null);

        String json = JSON.toJSONString(
                vo,
                JSONWriter.Feature.WriteNulls,
                JSONWriter.Feature.WriteNullStringAsEmpty,
                JSONWriter.Feature.WriteNullListAsEmpty
        );

        log.info("空值字段序列化结果：{}", json);

        Assertions.assertTrue(json.contains("\"nickname\":\"\""));
        Assertions.assertTrue(json.contains("\"roleList\":[]"));
    }

    /**
     * 验证枚举使用 toString 进行序列化
     */
    @Test
    void serializeEnumUsingToString() {
        String json = JSON.toJSONString(GenderEnum.MALE, JSONWriter.Feature.WriteEnumUsingToString);

        log.info("枚举序列化结果：{}", json);

        Assertions.assertEquals("\"male\"", json);
    }

    /**
     * 验证格式化 JSON 输出
     */
    @Test
    void serializePrettyFormat() {
        String json = JSON.toJSONString(
                CollUtil.newArrayList("admin", "user"),
                JSONWriter.Feature.PrettyFormat
        );

        log.info("格式化 JSON 输出：\n{}", json);

        Assertions.assertTrue(json.contains("admin"));
    }

    /**
     * 验证智能字段匹配反序列化
     */
    @Test
    void deserializeSmartMatch() {
        String json = """
                {
                  "user_name": "ateng"
                }
                """;

        SmartMatchUser user = JSON.parseObject(json, SmartMatchUser.class, JSONReader.Feature.SupportSmartMatch);

        log.info("智能字段匹配结果：{}", user);

        Assertions.assertEquals("ateng", user.getUserName());
    }

    /**
     * 智能匹配测试对象
     *
     * @author Ateng
     * @since 2026-05-09
     */
    @lombok.Data
    static class SmartMatchUser {

        /**
         * 用户名
         */
        private String userName;

    }

}
```

### 反序列化特性配置

反序列化特性用于控制 JSON 字符串转换为 Java 对象时的行为，例如是否启用智能字段匹配、是否基于字段赋值、数值精度如何处理、是否允许特殊类型信息等。

Fastjson2 的反序列化特性通过 `JSONReader.Feature` 配置。官方 Features 文档说明，`JSONReader.Feature` 用于配置 JSON 解析和对象反序列化行为，`SupportSmartMatch` 可用于字段名称智能匹配，`UseNativeObject` 可让非强类型解析时使用 JDK 原生 `LinkedHashMap` 和 `ArrayList` 而不是 `JSONObject` 和 `JSONArray`。([GitHub](https://github.com/alibaba/fastjson2/wiki/Features_cn?utm_source=chatgpt.com))

局部反序列化配置示例：

```java
UserVO user = JSON.parseObject(
        json,
        UserVO.class,
        JSONReader.Feature.SupportSmartMatch,
        JSONReader.Feature.UseBigDecimalForDoubles
);
```

全局反序列化配置建议写在 `Fastjson2WebMvcConfig` 中。

文件位置：`src/main/java/io/github/atengk/fastjson2/config/Fastjson2WebMvcConfig.java`

```java
fastJsonConfig.setReaderFeatures(
        // 小数按 BigDecimal 读取，避免二进制浮点精度问题
        JSONReader.Feature.UseBigDecimalForDoubles,

        // 开启字段智能匹配，兼容 user_name、userName、UserName 等字段风格
        JSONReader.Feature.SupportSmartMatch
);
```

常用反序列化特性说明如下：

| 特性                      | 作用                     | 适用场景                               |
| ------------------------- | ------------------------ | -------------------------------------- |
| `SupportSmartMatch`       | 智能匹配字段名           | 兼容下划线、驼峰、大小写差异           |
| `UseBigDecimalForDoubles` | 小数读取为 `BigDecimal`  | 金额、费率、经纬度等精度敏感字段       |
| `FieldBased`              | 基于字段进行反序列化     | 没有标准 setter 的对象                 |
| `UseNativeObject`         | 使用 JDK 原生集合对象    | 动态 JSON 解析后希望得到 `Map`、`List` |
| `IgnoreNoneSerializable`  | 忽略非序列化字段         | 对象结构复杂且存在不可处理字段         |
| `SupportAutoType`         | 支持基于类型信息反序列化 | 仅限可信内部数据，外部入参不建议开启   |

`SupportAutoType` 不建议在对外 HTTP 入参中全局开启。外部 JSON 应通过 DTO 明确字段结构，并结合参数校验和白名单式业务处理，避免反序列化边界过宽。

下面给出一个金额字段反序列化示例。

文件位置：`src/main/java/io/github/atengk/fastjson2/dto/OrderAmountDTO.java`

```java
package io.github.atengk.fastjson2.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单金额请求参数
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
public class OrderAmountDTO {

    /**
     * 订单编号
     */
    @NotNull(message = "订单编号不能为空")
    private Long orderId;

    /**
     * 订单金额
     */
    @NotNull(message = "订单金额不能为空")
    @DecimalMin(value = "0.01", message = "订单金额必须大于0")
    private BigDecimal amount;

}
```

测试示例：

```java
String json = """
        {
          "orderId": 1001,
          "amount": 99.99
        }
        """;

OrderAmountDTO dto = JSON.parseObject(
        json,
        OrderAmountDTO.class,
        JSONReader.Feature.UseBigDecimalForDoubles
);

log.info("订单金额反序列化结果：{}", dto);
```

金额字段建议直接使用 `BigDecimal` 类型接收，不建议使用 `Double` 或 `Float` 表示金额。

### 自定义 ObjectWriter

自定义 `ObjectWriter` 用于控制某个类型如何被序列化为 JSON。Fastjson2 官方文档说明，自定义序列化可以实现 `com.alibaba.fastjson2.writer.ObjectWriter`，核心方法为 `write(JSONWriter jsonWriter, Object object, Object fieldName, Type fieldType, long features)`。([Alibaba Open Source](https://alibaba.github.io/fastjson2/register_custom_reader_writer_cn?utm_source=chatgpt.com))

下面以手机号对象为例。业务中不希望手机号完整输出，而是统一脱敏为 `138****8000` 这种形式。

先定义手机号值对象。

文件位置：`src/main/java/io/github/atengk/fastjson2/model/PhoneNumber.java`

```java
package io.github.atengk.fastjson2.model;

import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * 手机号值对象
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Getter
@EqualsAndHashCode
public class PhoneNumber {

    /**
     * 手机号正则
     */
    private static final String PHONE_REGEX = "^1[3-9]\\d{9}$";

    /**
     * 手机号
     */
    private final String value;

    /**
     * 创建手机号对象
     *
     * @param value 手机号
     */
    private PhoneNumber(String value) {
        this.value = value;
    }

    /**
     * 构建手机号对象
     *
     * @param value 手机号
     * @return 手机号对象
     */
    public static PhoneNumber of(String value) {
        if (StrUtil.isBlank(value)) {
            throw new IllegalArgumentException("手机号不能为空");
        }
        if (!ReUtil.isMatch(PHONE_REGEX, value)) {
            throw new IllegalArgumentException("手机号格式不正确");
        }
        return new PhoneNumber(value);
    }

    /**
     * 获取脱敏手机号
     *
     * @return 脱敏手机号
     */
    public String getMaskValue() {
        return StrUtil.hide(this.value, 3, 7);
    }

    /**
     * 输出手机号原始值
     *
     * @return 手机号
     */
    @Override
    public String toString() {
        return this.value;
    }

}
```

下面的 `ObjectWriter` 用于将 `PhoneNumber` 序列化为脱敏字符串。

文件位置：`src/main/java/io/github/atengk/fastjson2/json/PhoneNumberObjectWriter.java`

```java
package io.github.atengk.fastjson2.json;

import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.writer.ObjectWriter;
import io.github.atengk.fastjson2.model.PhoneNumber;

import java.lang.reflect.Type;

/**
 * 手机号自定义序列化器
 *
 * @author Ateng
 * @since 2026-05-09
 */
public class PhoneNumberObjectWriter implements ObjectWriter<PhoneNumber> {

    /**
     * 单例实例
     */
    public static final PhoneNumberObjectWriter INSTANCE = new PhoneNumberObjectWriter();

    /**
     * 写入手机号 JSON 值
     *
     * @param jsonWriter JSON 写入器
     * @param object     手机号对象
     * @param fieldName  字段名
     * @param fieldType  字段类型
     * @param features   序列化特性
     */
    @Override
    public void write(JSONWriter jsonWriter, Object object, Object fieldName, Type fieldType, long features) {
        if (object == null) {
            jsonWriter.writeNull();
            return;
        }

        PhoneNumber phoneNumber = (PhoneNumber) object;
        jsonWriter.writeString(phoneNumber.getMaskValue());
    }

}
```

注册自定义 `ObjectWriter`。

文件位置：`src/main/java/io/github/atengk/fastjson2/config/Fastjson2ObjectCodecConfig.java`

```java
package io.github.atengk.fastjson2.config;

import com.alibaba.fastjson2.JSONFactory;
import io.github.atengk.fastjson2.json.PhoneNumberObjectReader;
import io.github.atengk.fastjson2.json.PhoneNumberObjectWriter;
import io.github.atengk.fastjson2.model.PhoneNumber;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

/**
 * Fastjson2 自定义编解码配置
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Configuration
public class Fastjson2ObjectCodecConfig {

    /**
     * 注册自定义 ObjectWriter 和 ObjectReader
     */
    @PostConstruct
    public void registerCodec() {
        JSONFactory.getDefaultObjectWriterProvider()
                .register(PhoneNumber.class, PhoneNumberObjectWriter.INSTANCE);

        JSONFactory.getDefaultObjectReaderProvider()
                .register(PhoneNumber.class, PhoneNumberObjectReader.INSTANCE);

        log.info("Fastjson2 自定义 ObjectWriter 和 ObjectReader 注册完成");
    }

}
```

定义一个包含手机号字段的 VO。

文件位置：`src/main/java/io/github/atengk/fastjson2/vo/UserContactVO.java`

```java
package io.github.atengk.fastjson2.vo;

import io.github.atengk.fastjson2.model.PhoneNumber;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户联系方式响应对象
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserContactVO {

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 手机号
     */
    private PhoneNumber phoneNumber;

}
```

序列化测试：

```java
UserContactVO vo = new UserContactVO(1L, "ateng", PhoneNumber.of("13812348000"));

String json = JSON.toJSONString(vo);

log.info("自定义 ObjectWriter 序列化结果：{}", json);
```

输出示例：

```json
{
  "id": 1,
  "username": "ateng",
  "phoneNumber": "138****8000"
}
```

自定义 `ObjectWriter` 适合处理具有统一输出规则的类型，例如手机号、身份证号、银行卡号、金额对象、地理坐标对象等。固定敏感字段可以用 `@JSONField(serialize = false)`，但通用值对象更适合使用 `ObjectWriter` 集中处理。

### 自定义 ObjectReader

自定义 `ObjectReader` 用于控制某个类型如何从 JSON 反序列化为 Java 对象。Fastjson2 官方文档说明，自定义反序列化可以实现 `com.alibaba.fastjson2.reader.ObjectReader`，核心方法为 `readObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features)`。([Alibaba Open Source](https://alibaba.github.io/fastjson2/register_custom_reader_writer_cn?utm_source=chatgpt.com))

下面为 `PhoneNumber` 定义自定义反序列化器。

文件位置：`src/main/java/io/github/atengk/fastjson2/json/PhoneNumberObjectReader.java`

```java
package io.github.atengk.fastjson2.json;

import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.reader.ObjectReader;
import io.github.atengk.fastjson2.model.PhoneNumber;

import java.lang.reflect.Type;

/**
 * 手机号自定义反序列化器
 *
 * @author Ateng
 * @since 2026-05-09
 */
public class PhoneNumberObjectReader implements ObjectReader<PhoneNumber> {

    /**
     * 单例实例
     */
    public static final PhoneNumberObjectReader INSTANCE = new PhoneNumberObjectReader();

    /**
     * 读取手机号 JSON 值
     *
     * @param jsonReader JSON 读取器
     * @param fieldType  字段类型
     * @param fieldName  字段名
     * @param features   反序列化特性
     * @return 手机号对象
     */
    @Override
    public PhoneNumber readObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
        if (jsonReader.nextIfNull()) {
            return null;
        }

        String value = jsonReader.readString();
        return PhoneNumber.of(value);
    }

}
```

定义请求 DTO。

文件位置：`src/main/java/io/github/atengk/fastjson2/dto/UserContactDTO.java`

```java
package io.github.atengk.fastjson2.dto;

import io.github.atengk.fastjson2.model.PhoneNumber;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 用户联系方式请求参数
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
public class UserContactDTO {

    /**
     * 用户名
     */
    @NotBlank(message = "用户名不能为空")
    private String username;

    /**
     * 手机号
     */
    @NotNull(message = "手机号不能为空")
    private PhoneNumber phoneNumber;

}
```

反序列化测试：

```java
String json = """
        {
          "username": "ateng",
          "phoneNumber": "13812348000"
        }
        """;

UserContactDTO dto = JSON.parseObject(json, UserContactDTO.class);

log.info("自定义 ObjectReader 反序列化结果：{}", dto);
```

自定义 `ObjectReader` 适合处理值对象、加密字段、特殊格式字段、遗留系统字段等。例如手机号、金额、时间范围、业务编码对象，都可以通过 `ObjectReader` 在反序列化阶段完成格式校验和对象构建。

## 数据校验与异常处理

本节主要说明 Spring Boot 3 中如何结合 Fastjson2、`jakarta.validation` 和全局异常处理完成接口入参校验、JSON 解析异常处理和统一异常响应封装。Spring Boot 3 使用 Jakarta EE 体系，因此参数校验相关包名应使用 `jakarta.validation`，而不是旧版 `javax.validation`。

### 请求参数校验

请求参数校验用于在业务逻辑执行前拦截非法入参。Spring Boot 3 项目中通常引入 `spring-boot-starter-validation`，然后在 DTO 字段上使用 `@NotBlank`、`@NotNull`、`@Min`、`@Max`、`@Size`、`@Pattern` 等注解，并在 Controller 入参处添加 `@Valid`。

Maven 依赖如下，前文已经配置过，此处作为校验能力说明。

文件位置：`pom.xml`

```xml
<!-- 参数校验依赖，用于 DTO 字段校验和 Controller 入参校验 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

定义一个带完整校验规则的请求 DTO。

文件位置：`src/main/java/io/github/atengk/fastjson2/dto/UserRegisterDTO.java`

```java
package io.github.atengk.fastjson2.dto;

import com.alibaba.fastjson2.annotation.JSONField;
import io.github.atengk.fastjson2.enums.GenderEnum;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户注册请求参数
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
public class UserRegisterDTO {

    /**
     * 用户名
     */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 2, max = 30, message = "用户名长度必须在2到30个字符之间")
    @JSONField(name = "user_name", alternateNames = {"username", "userName"})
    private String username;

    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 32, message = "密码长度必须在8到32个字符之间")
    private String password;

    /**
     * 手机号
     */
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /**
     * 年龄
     */
    @NotNull(message = "年龄不能为空")
    @Min(value = 0, message = "年龄不能小于0")
    @Max(value = 150, message = "年龄不能大于150")
    private Integer age;

    /**
     * 性别
     */
    @NotNull(message = "性别不能为空")
    private GenderEnum gender;

    /**
     * 创建时间
     */
    @JSONField(name = "create_time", format = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 角色列表
     */
    @Size(max = 10, message = "角色数量不能超过10个")
    private List<String> roleList;

}
```

Controller 中使用 `@Valid` 触发校验。

文件位置：`src/main/java/io/github/atengk/fastjson2/controller/UserValidationController.java`

```java
package io.github.atengk.fastjson2.controller;

import cn.hutool.core.collection.CollUtil;
import io.github.atengk.fastjson2.common.R;
import io.github.atengk.fastjson2.dto.UserRegisterDTO;
import io.github.atengk.fastjson2.enums.GenderEnum;
import io.github.atengk.fastjson2.vo.UserVO;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户参数校验接口
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@RestController
@RequestMapping("/validation/users")
public class UserValidationController {

    /**
     * 注册用户
     *
     * @param dto 用户注册请求参数
     * @return 用户响应结果
     */
    @PostMapping("/register")
    public R<UserVO> register(@Valid @RequestBody UserRegisterDTO dto) {
        List<String> roleList = CollUtil.isEmpty(dto.getRoleList())
                ? CollUtil.newArrayList("user")
                : dto.getRoleList();

        GenderEnum gender = dto.getGender();
        LocalDateTime createTime = dto.getCreateTime() == null
                ? LocalDateTime.now()
                : dto.getCreateTime();

        UserVO userVO = new UserVO(
                1L,
                dto.getUsername(),
                dto.getAge(),
                gender.getCode(),
                gender.getName(),
                createTime,
                roleList,
                "注册接口内部备注"
        );

        log.info("注册用户成功，username={}，phone={}，gender={}", dto.getUsername(), dto.getPhone(), gender.getCode());
        return R.ok(userVO);
    }

}
```

异常请求示例：

```bash
curl -X POST "http://localhost:8080/validation/users/register" \
  -H "Content-Type: application/json" \
  -d '{
    "user_name": "",
    "password": "123",
    "phone": "10086",
    "age": 200,
    "gender": "MALE"
  }'
```

当 DTO 校验失败时，Spring MVC 会抛出 `MethodArgumentNotValidException`，后续可通过全局异常处理器统一封装响应。

### JSON 解析异常处理

JSON 解析异常通常发生在请求体格式错误、字段类型不匹配、日期格式不正确、枚举值非法、自定义 `ObjectReader` 校验失败等场景。

常见错误请求如下：

```json
{
  "user_name": "ateng",
  "age": "abc",
  "gender": "MALE",
  "create_time": "2026/05/09 10:30:00"
}
```

上面的请求中，`age` 期望是数字，但传入了字符串 `abc`；`create_time` 如果没有兼容该格式，也可能导致日期解析失败。

Spring MVC 在读取 `@RequestBody` 时，如果 JSON 解析失败，通常会抛出 `HttpMessageNotReadableException`。Fastjson2 内部解析异常也可能以 `JSONException` 的形式出现，最终在 Web 层被 Spring 包装为 HTTP 消息不可读异常。因此全局异常处理器中应同时处理 `HttpMessageNotReadableException` 和 `JSONException`。

先定义错误码常量。

文件位置：`src/main/java/io/github/atengk/fastjson2/common/ResultCode.java`

```java
package io.github.atengk.fastjson2.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 响应状态码
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    /**
     * 操作成功
     */
    SUCCESS(200, "操作成功"),

    /**
     * 请求参数错误
     */
    PARAM_ERROR(400, "请求参数错误"),

    /**
     * JSON格式错误
     */
    JSON_PARSE_ERROR(400, "JSON格式错误"),

    /**
     * 系统异常
     */
    SYSTEM_ERROR(500, "系统异常");

    /**
     * 状态码
     */
    private final Integer code;

    /**
     * 消息
     */
    private final String message;

}
```

调整统一响应对象，增加基于 `ResultCode` 的失败方法。

文件位置：`src/main/java/io/github/atengk/fastjson2/common/R.java`

```java
package io.github.atengk.fastjson2.common;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 统一响应结果
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class R<T> {

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
     * 响应时间
     */
    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;

    /**
     * 成功响应
     *
     * @param data 响应数据
     * @param <T>  数据类型
     * @return 统一响应结果
     */
    public static <T> R<T> ok(T data) {
        return new R<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data, LocalDateTime.now());
    }

    /**
     * 成功响应
     *
     * @param message 响应消息
     * @param data    响应数据
     * @param <T>     数据类型
     * @return 统一响应结果
     */
    public static <T> R<T> ok(String message, T data) {
        return new R<>(ResultCode.SUCCESS.getCode(), message, data, LocalDateTime.now());
    }

    /**
     * 失败响应
     *
     * @param resultCode 响应状态码
     * @param <T>        数据类型
     * @return 统一响应结果
     */
    public static <T> R<T> fail(ResultCode resultCode) {
        return new R<>(resultCode.getCode(), resultCode.getMessage(), null, LocalDateTime.now());
    }

    /**
     * 失败响应
     *
     * @param resultCode 响应状态码
     * @param message    响应消息
     * @param <T>        数据类型
     * @return 统一响应结果
     */
    public static <T> R<T> fail(ResultCode resultCode, String message) {
        return new R<>(resultCode.getCode(), message, null, LocalDateTime.now());
    }

    /**
     * 失败响应
     *
     * @param code    状态码
     * @param message 响应消息
     * @param <T>     数据类型
     * @return 统一响应结果
     */
    public static <T> R<T> fail(Integer code, String message) {
        return new R<>(code, message, null, LocalDateTime.now());
    }

}
```

### 全局异常响应封装

全局异常响应封装用于统一处理参数校验异常、JSON 解析异常、业务异常和兜底系统异常，避免 Controller 中大量重复 `try-catch`。

下面的异常处理器覆盖常见 Web 接口异常：

文件位置：`src/main/java/io/github/atengk/fastjson2/exception/GlobalExceptionHandler.java`

```java
package io.github.atengk.fastjson2.exception;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONException;
import io.github.atengk.fastjson2.common.R;
import io.github.atengk.fastjson2.common.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.stream.Collectors;

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
     * 处理请求参数校验异常
     *
     * @param exception 参数校验异常
     * @return 统一响应结果
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<R<Void>> handleMethodArgumentNotValidException(MethodArgumentNotValidException exception) {
        List<FieldError> fieldErrors = exception.getBindingResult().getFieldErrors();

        String message = CollUtil.emptyIfNull(fieldErrors)
                .stream()
                .map(error -> StrUtil.format("{}：{}", error.getField(), error.getDefaultMessage()))
                .distinct()
                .collect(Collectors.joining("；"));

        if (StrUtil.isBlank(message)) {
            message = ResultCode.PARAM_ERROR.getMessage();
        }

        log.warn("请求参数校验失败，message={}", message);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(R.fail(ResultCode.PARAM_ERROR, message));
    }

    /**
     * 处理 HTTP 消息不可读异常
     *
     * @param exception HTTP 消息不可读异常
     * @return 统一响应结果
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<R<Void>> handleHttpMessageNotReadableException(HttpMessageNotReadableException exception) {
        String message = "请求体格式错误，请检查 JSON 格式、字段类型、日期格式或枚举值";

        log.warn("JSON 请求体解析失败，message={}", exception.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(R.fail(ResultCode.JSON_PARSE_ERROR, message));
    }

    /**
     * 处理 Fastjson2 JSON 解析异常
     *
     * @param exception JSON 解析异常
     * @return 统一响应结果
     */
    @ExceptionHandler(JSONException.class)
    public ResponseEntity<R<Void>> handleJSONException(JSONException exception) {
        String message = "JSON 解析失败，请检查 JSON 字符串格式";

        log.warn("Fastjson2 JSON 解析失败，message={}", exception.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(R.fail(ResultCode.JSON_PARSE_ERROR, message));
    }

    /**
     * 处理非法参数异常
     *
     * @param exception 非法参数异常
     * @return 统一响应结果
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<R<Void>> handleIllegalArgumentException(IllegalArgumentException exception) {
        String message = StrUtil.blankToDefault(exception.getMessage(), ResultCode.PARAM_ERROR.getMessage());

        log.warn("非法参数异常，message={}", message);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(R.fail(ResultCode.PARAM_ERROR, message));
    }

    /**
     * 处理系统兜底异常
     *
     * @param exception 系统异常
     * @return 统一响应结果
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<R<Void>> handleException(Exception exception) {
        log.error("系统异常", exception);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(R.fail(ResultCode.SYSTEM_ERROR));
    }

}
```

参数校验失败响应示例：

```json
{
  "code": 400,
  "message": "username：用户名不能为空；password：密码长度必须在8到32个字符之间；phone：手机号格式不正确；age：年龄不能大于150",
  "data": null,
  "timestamp": "2026-05-09 12:00:00"
}
```

JSON 解析失败响应示例：

```json
{
  "code": 400,
  "message": "请求体格式错误，请检查 JSON 格式、字段类型、日期格式或枚举值",
  "data": null,
  "timestamp": "2026-05-09 12:00:00"
}
```

建议异常处理遵循以下规则：

| 异常类型                          | 处理方式                       | 响应状态 |
| --------------------------------- | ------------------------------ | -------- |
| `MethodArgumentNotValidException` | 提取字段错误并返回给前端       | `400`    |
| `HttpMessageNotReadableException` | 返回 JSON 请求体格式错误       | `400`    |
| `JSONException`                   | 返回 JSON 解析失败             | `400`    |
| `IllegalArgumentException`        | 返回业务参数错误               | `400`    |
| `Exception`                       | 记录完整异常堆栈，返回系统异常 | `500`    |

在生产环境中，日志应记录详细异常信息，但接口响应不应直接暴露堆栈、类名、SQL、服务器路径等内部细节。对于 JSON 解析失败、参数校验失败这类客户端错误，返回明确但不过度暴露的提示即可。



## 测试验证

本节主要说明 Spring Boot 3 集成 Fastjson2 后如何进行测试验证，包括普通单元测试、MockMvc 接口测试、序列化结果验证和异常场景验证。测试目标不是只验证接口能否访问，还要确认 JSON 字段名称、日期格式、空值处理、泛型转换、参数校验和异常响应是否符合预期。

### 单元测试

单元测试主要用于验证 Fastjson2 的基础序列化、反序列化、泛型转换和 JSON 数组处理。该类测试不依赖 Web 容器，执行速度快，适合放在基础能力验证中。

下面的测试类用于验证对象序列化、反序列化和泛型对象转换。

文件位置：`src/test/java/io/github/atengk/fastjson2/Fastjson2UnitTest.java`

```java
package io.github.atengk.fastjson2;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import io.github.atengk.fastjson2.common.PageResult;
import io.github.atengk.fastjson2.common.R;
import io.github.atengk.fastjson2.enums.GenderEnum;
import io.github.atengk.fastjson2.vo.UserVO;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Fastjson2 单元测试
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
class Fastjson2UnitTest {

    /**
     * 构建测试用户对象
     *
     * @return 用户响应对象
     */
    private UserVO buildUser() {
        GenderEnum gender = GenderEnum.MALE;
        return new UserVO(
                1L,
                "ateng",
                18,
                gender.getCode(),
                gender.getName(),
                LocalDateTime.of(2026, 5, 9, 10, 30, 0),
                CollUtil.newArrayList("admin", "user"),
                "测试内部备注"
        );
    }

    /**
     * 验证 Java 对象序列化为 JSON 字符串
     */
    @Test
    void serializeObject() {
        UserVO userVO = buildUser();

        String json = JSON.toJSONString(userVO);

        log.info("对象序列化结果：{}", json);

        Assertions.assertTrue(StrUtil.isNotBlank(json));
        Assertions.assertTrue(JSON.isValid(json));
        Assertions.assertTrue(json.contains("\"user_name\""));
        Assertions.assertFalse(json.contains("internalRemark"));
    }

    /**
     * 验证 JSON 字符串反序列化为 Java 对象
     */
    @Test
    void deserializeObject() {
        String json = """
                {
                  "id": 1,
                  "user_name": "ateng",
                  "age": 18,
                  "genderCode": "male",
                  "genderName": "男",
                  "create_time": "2026-05-09 10:30:00",
                  "roleList": ["admin", "user"]
                }
                """;

        UserVO userVO = JSON.parseObject(json, UserVO.class);

        log.info("对象反序列化结果：{}", userVO);

        Assertions.assertEquals(1L, userVO.getId());
        Assertions.assertEquals("ateng", userVO.getUsername());
        Assertions.assertEquals(2, userVO.getRoleList().size());
    }

    /**
     * 验证泛型对象转换
     */
    @Test
    void convertGenericObject() {
        PageResult<UserVO> pageResult = new PageResult<>(
                1L,
                10L,
                1L,
                CollUtil.newArrayList(buildUser())
        );

        R<PageResult<UserVO>> result = R.ok(pageResult);
        String json = JSON.toJSONString(result);

        R<PageResult<UserVO>> parsedResult = JSON.parseObject(
                json,
                new TypeReference<R<PageResult<UserVO>>>() {
                }.getType()
        );

        log.info("泛型对象转换结果：{}", parsedResult);

        Assertions.assertEquals(200, parsedResult.getCode());
        Assertions.assertEquals(1L, parsedResult.getData().getTotal());
        Assertions.assertEquals("ateng", parsedResult.getData().getRecords().get(0).getUsername());
    }

    /**
     * 验证 JSON 数组转换为 Java 集合
     */
    @Test
    void parseArray() {
        String jsonArray = """
                [
                  {
                    "id": 1,
                    "user_name": "ateng",
                    "age": 18,
                    "genderCode": "male",
                    "genderName": "男",
                    "create_time": "2026-05-09 10:30:00",
                    "roleList": ["admin", "user"]
                  }
                ]
                """;

        List<UserVO> userList = JSON.parseArray(jsonArray, UserVO.class);

        log.info("JSON 数组转换结果：{}", userList);

        Assertions.assertEquals(1, userList.size());
        Assertions.assertEquals("ateng", userList.get(0).getUsername());
    }

}
```

执行测试命令：

```bash
mvn test -Dtest=Fastjson2UnitTest
```

该测试主要验证 Fastjson2 在非 Web 环境下是否能正常完成对象转换。如果这里失败，应优先检查 DTO、VO、`@JSONField`、日期格式和泛型类型定义。

### 接口测试

接口测试用于验证 Spring MVC 集成 Fastjson2 后，请求体是否能正确反序列化为 DTO，响应对象是否能正确序列化为 JSON。这里推荐使用 `MockMvc`，不需要真实启动 HTTP 端口即可完成 Controller 层测试。

下面的测试类用于验证前文 `UserController` 的创建用户、查询详情和分页查询接口。

文件位置：`src/test/java/io/github/atengk/fastjson2/UserControllerMockMvcTest.java`

```java
package io.github.atengk.fastjson2;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 用户接口 MockMvc 测试
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@SpringBootTest
@AutoConfigureMockMvc
class UserControllerMockMvcTest {

    /**
     * MockMvc 测试客户端
     */
    @Autowired
    private MockMvc mockMvc;

    /**
     * 验证创建用户接口
     *
     * @throws Exception 请求异常
     */
    @Test
    void createUser() throws Exception {
        Map<String, Object> requestMap = new LinkedHashMap<>();
        requestMap.put("user_name", "ateng");
        requestMap.put("age", 18);
        requestMap.put("gender", "MALE");
        requestMap.put("create_time", "2026-05-09 10:30:00");
        requestMap.put("roleList", CollUtil.newArrayList("admin", "user"));

        String requestBody = JSON.toJSONString(requestMap);

        ResultActions resultActions = mockMvc.perform(
                        post("/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.user_name").value("ateng"))
                .andExpect(jsonPath("$.data.age").value(18))
                .andExpect(jsonPath("$.data.genderCode").value("male"))
                .andExpect(jsonPath("$.data.create_time").value("2026-05-09 10:30:00"));

        String responseBody = resultActions.andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        log.info("创建用户接口响应：{}", responseBody);
    }

    /**
     * 验证查询用户详情接口
     *
     * @throws Exception 请求异常
     */
    @Test
    void getUserDetail() throws Exception {
        ResultActions resultActions = mockMvc.perform(
                        get("/users/{id}", 1L)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.user_name").value("ateng"))
                .andExpect(jsonPath("$.data.roleList[0]").value("admin"));

        String responseBody = resultActions.andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        log.info("查询用户详情接口响应：{}", responseBody);
    }

    /**
     * 验证分页查询用户接口
     *
     * @throws Exception 请求异常
     */
    @Test
    void pageUser() throws Exception {
        ResultActions resultActions = mockMvc.perform(
                        get("/users")
                                .param("current", "1")
                                .param("size", "10")
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.current").value(1))
                .andExpect(jsonPath("$.data.size").value(10))
                .andExpect(jsonPath("$.data.records[0].user_name").value("ateng"));

        String responseBody = resultActions.andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        log.info("分页查询用户接口响应：{}", responseBody);
    }

}
```

执行指定接口测试：

```bash
mvn test -Dtest=UserControllerMockMvcTest
```

接口测试通过后，说明 Fastjson2 已经参与 Spring MVC 请求体和响应体的 JSON 转换流程。如果请求字段映射失败，应检查 DTO 中的 `@JSONField(name = "...")`、全局 `SupportSmartMatch` 配置以及请求 JSON 字段名是否一致。

### 序列化结果验证

序列化结果验证重点关注接口 JSON 输出是否符合约定，包括字段名称、日期格式、空值处理、敏感字段隐藏、枚举输出和泛型结构。

下面的测试类用于验证输出 JSON 的关键字段。

文件位置：`src/test/java/io/github/atengk/fastjson2/Fastjson2SerializationVerifyTest.java`

```java
package io.github.atengk.fastjson2;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter;
import io.github.atengk.fastjson2.common.R;
import io.github.atengk.fastjson2.enums.GenderEnum;
import io.github.atengk.fastjson2.vo.UserNullFeatureVO;
import io.github.atengk.fastjson2.vo.UserVO;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

/**
 * Fastjson2 序列化结果验证测试
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
class Fastjson2SerializationVerifyTest {

    /**
     * 验证字段名称和敏感字段输出
     */
    @Test
    void verifyFieldNameAndIgnoreField() {
        GenderEnum gender = GenderEnum.MALE;

        UserVO userVO = new UserVO(
                1L,
                "ateng",
                18,
                gender.getCode(),
                gender.getName(),
                LocalDateTime.of(2026, 5, 9, 10, 30, 0),
                CollUtil.newArrayList("admin", "user"),
                "不应输出的内部备注"
        );

        String json = JSON.toJSONString(userVO);

        log.info("字段名称与忽略字段验证结果：{}", json);

        Assertions.assertTrue(json.contains("\"user_name\""));
        Assertions.assertTrue(json.contains("\"create_time\""));
        Assertions.assertFalse(json.contains("internalRemark"));
        Assertions.assertFalse(json.contains("不应输出的内部备注"));
    }

    /**
     * 验证日期时间格式
     */
    @Test
    void verifyDateTimeFormat() {
        GenderEnum gender = GenderEnum.MALE;

        UserVO userVO = new UserVO(
                1L,
                "ateng",
                18,
                gender.getCode(),
                gender.getName(),
                LocalDateTime.of(2026, 5, 9, 10, 30, 0),
                CollUtil.newArrayList("admin", "user"),
                null
        );

        String json = JSON.toJSONString(userVO);
        JSONObject jsonObject = JSON.parseObject(json);

        log.info("日期时间格式验证结果：{}", json);

        Assertions.assertEquals("2026-05-09 10:30:00", jsonObject.getString("create_time"));
    }

    /**
     * 验证空值字段输出规则
     */
    @Test
    void verifyNullFieldPolicy() {
        UserNullFeatureVO vo = new UserNullFeatureVO(1L, null, null);

        String json = JSON.toJSONString(
                vo,
                JSONWriter.Feature.WriteNulls,
                JSONWriter.Feature.WriteNullStringAsEmpty,
                JSONWriter.Feature.WriteNullListAsEmpty
        );

        log.info("空值字段输出验证结果：{}", json);

        JSONObject jsonObject = JSON.parseObject(json);

        Assertions.assertEquals("", jsonObject.getString("nickname"));
        Assertions.assertTrue(jsonObject.getJSONArray("roleList").isEmpty());
    }

    /**
     * 验证统一响应结构
     */
    @Test
    void verifyResponseStructure() {
        R<String> result = R.ok("success");
        String json = JSON.toJSONString(result);

        log.info("统一响应结构验证结果：{}", json);

        JSONObject jsonObject = JSON.parseObject(json);

        Assertions.assertEquals(200, jsonObject.getInteger("code"));
        Assertions.assertEquals("操作成功", jsonObject.getString("message"));
        Assertions.assertEquals("success", jsonObject.getString("data"));
        Assertions.assertNotNull(jsonObject.getString("timestamp"));
    }

}
```

执行命令：

```bash
mvn test -Dtest=Fastjson2SerializationVerifyTest
```

序列化结果验证建议重点检查以下内容：

| 验证项   | 目标                                                      |
| -------- | --------------------------------------------------------- |
| 字段名称 | 确认驼峰、下划线、别名映射符合接口约定                    |
| 日期格式 | 确认 `LocalDateTime` 输出为 `yyyy-MM-dd HH:mm:ss`         |
| 空值规则 | 确认 `null`、空字符串、空集合输出符合团队规范             |
| 敏感字段 | 确认密码、密钥、内部备注不出现在响应 JSON 中              |
| 统一响应 | 确认所有接口均返回 `code`、`message`、`data`、`timestamp` |
| 泛型结构 | 确认分页、统一响应、集合包装对象结构稳定                  |

### 异常场景验证

异常场景验证用于确认参数校验异常、JSON 格式异常、字段类型异常、枚举值异常等场景是否能返回统一响应结构。该部分通常与全局异常处理器一起测试。

下面的测试类用于验证参数校验失败和 JSON 解析失败。

文件位置：`src/test/java/io/github/atengk/fastjson2/ExceptionScenarioMockMvcTest.java`

```java
package io.github.atengk.fastjson2;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.nio.charset.StandardCharsets;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 异常场景接口测试
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@SpringBootTest
@AutoConfigureMockMvc
class ExceptionScenarioMockMvcTest {

    /**
     * MockMvc 测试客户端
     */
    @Autowired
    private MockMvc mockMvc;

    /**
     * 验证请求参数校验失败
     *
     * @throws Exception 请求异常
     */
    @Test
    void validateParamError() throws Exception {
        String requestBody = """
                {
                  "user_name": "",
                  "password": "123",
                  "phone": "10086",
                  "age": 200,
                  "gender": "MALE"
                }
                """;

        ResultActions resultActions = mockMvc.perform(
                        post("/validation/users/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.data").doesNotExist());

        String responseBody = resultActions.andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        log.info("参数校验失败响应：{}", responseBody);
    }

    /**
     * 验证 JSON 格式错误
     *
     * @throws Exception 请求异常
     */
    @Test
    void validateJsonFormatError() throws Exception {
        String requestBody = """
                {
                  "user_name": "ateng",
                  "password": "12345678",
                  "phone": "13812348000",
                  "age": 18,
                  "gender": "MALE",
                }
                """;

        ResultActions resultActions = mockMvc.perform(
                        post("/validation/users/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").exists());

        String responseBody = resultActions.andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        log.info("JSON 格式错误响应：{}", responseBody);
    }

    /**
     * 验证字段类型错误
     *
     * @throws Exception 请求异常
     */
    @Test
    void validateFieldTypeError() throws Exception {
        String requestBody = """
                {
                  "user_name": "ateng",
                  "password": "12345678",
                  "phone": "13812348000",
                  "age": "abc",
                  "gender": "MALE"
                }
                """;

        ResultActions resultActions = mockMvc.perform(
                        post("/validation/users/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").exists());

        String responseBody = resultActions.andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        log.info("字段类型错误响应：{}", responseBody);
    }

    /**
     * 验证枚举值错误
     *
     * @throws Exception 请求异常
     */
    @Test
    void validateEnumValueError() throws Exception {
        String requestBody = """
                {
                  "user_name": "ateng",
                  "password": "12345678",
                  "phone": "13812348000",
                  "age": 18,
                  "gender": "INVALID"
                }
                """;

        ResultActions resultActions = mockMvc.perform(
                        post("/validation/users/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").exists());

        String responseBody = resultActions.andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        log.info("枚举值错误响应：{}", responseBody);
    }

}
```

执行命令：

```bash
mvn test -Dtest=ExceptionScenarioMockMvcTest
```

异常测试建议覆盖以下场景：

| 场景          | 示例                         | 期望结果                     |
| ------------- | ---------------------------- | ---------------------------- |
| 必填字段为空  | `user_name` 为空字符串       | 返回 `400` 和字段校验提示    |
| 字段类型错误  | `age` 传入 `"abc"`           | 返回 JSON 解析失败           |
| JSON 格式错误 | 请求体多余逗号、缺少括号     | 返回 JSON 格式错误           |
| 日期格式错误  | `create_time` 格式不符合约定 | 返回 JSON 解析失败或参数错误 |
| 枚举值错误    | `gender` 传入不存在的值      | 返回 JSON 解析失败           |
| 敏感字段输出  | VO 中包含内部字段            | 响应 JSON 不应包含敏感字段   |

## 开发规范

本节整理 Spring Boot 3 集成 Fastjson2 时建议遵循的开发规范，包括 DTO 与 VO 使用规范、日期时间字段规范、JSON 字段命名规范和安全使用建议。规范的目标是保证接口结构稳定、字段语义清晰、序列化行为可控，并减少反序列化安全风险。

### DTO 与 VO 使用规范

DTO 用于接收请求参数，VO 用于返回响应结果。不要直接使用数据库实体类作为接口入参或出参，避免外部请求污染实体字段，也避免敏感字段被误输出。

推荐规范如下：

| 类型   | 用途         | 规范                                                     |
| ------ | ------------ | -------------------------------------------------------- |
| DTO    | 接收请求参数 | 添加参数校验注解，只包含接口需要的入参字段               |
| VO     | 返回响应数据 | 控制响应字段，不包含密码、密钥、内部备注等敏感字段       |
| Entity | 数据库实体   | 只用于持久化层，不直接暴露给接口                         |
| BO     | 业务对象     | 用于业务层内部流转，避免 Controller 直接操作复杂业务模型 |
| `R<T>` | 统一响应     | 封装 `code`、`message`、`data`、`timestamp`              |

DTO 编写建议：

```java
/**
 * 用户查询请求参数
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
public class UserQueryDTO {

    /**
     * 用户名
     */
    @JSONField(name = "user_name", alternateNames = {"username", "userName"})
    private String username;

    /**
     * 当前页码
     */
    @NotNull(message = "当前页码不能为空")
    @Min(value = 1, message = "当前页码不能小于1")
    private Long current;

    /**
     * 每页条数
     */
    @NotNull(message = "每页条数不能为空")
    @Min(value = 1, message = "每页条数不能小于1")
    @Max(value = 100, message = "每页条数不能大于100")
    private Long size;

}
```

VO 编写建议：

```java
/**
 * 用户列表响应对象
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
public class UserListVO {

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 用户名
     */
    @JSONField(name = "user_name")
    private String username;

    /**
     * 用户状态编码
     */
    private String statusCode;

    /**
     * 用户状态名称
     */
    private String statusName;

    /**
     * 创建时间
     */
    @JSONField(name = "create_time", format = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

}
```

开发时应避免以下写法：

```java
// 不推荐：直接使用 Entity 接收请求参数
@PostMapping
public R<UserEntity> create(@RequestBody UserEntity entity) {
    return R.ok(entity);
}

// 不推荐：直接返回数据库实体，容易误输出敏感字段
@GetMapping("/{id}")
public R<UserEntity> detail(@PathVariable Long id) {
    return R.ok(userEntity);
}
```

推荐写法：

```java
// 推荐：请求使用 DTO，响应使用 VO
@PostMapping
public R<UserVO> create(@Valid @RequestBody UserCreateDTO dto) {
    UserVO vo = userService.create(dto);
    return R.ok(vo);
}
```

### 日期时间字段规范

日期时间字段应在全局配置和字段注解两个层面保持一致。全局配置用于兜底，字段注解用于关键接口字段的显式约束。

推荐格式如下：

| Java 类型       | JSON 格式             | 示例                  |
| --------------- | --------------------- | --------------------- |
| `LocalDateTime` | `yyyy-MM-dd HH:mm:ss` | `2026-05-09 10:30:00` |
| `LocalDate`     | `yyyy-MM-dd`          | `2026-05-09`          |
| `LocalTime`     | `HH:mm:ss`            | `10:30:00`            |
| `Date`          | `yyyy-MM-dd HH:mm:ss` | `2026-05-09 10:30:00` |
| 时间戳          | 毫秒时间戳            | `1778293800000`       |

推荐字段写法：

```java
/**
 * 创建时间
 */
@JSONField(format = "yyyy-MM-dd HH:mm:ss")
private LocalDateTime createTime;

/**
 * 生日
 */
@JSONField(format = "yyyy-MM-dd")
private LocalDate birthday;

/**
 * 开始时间
 */
@JSONField(format = "HH:mm:ss")
private LocalTime startTime;
```

全局配置建议：

```java
fastJsonConfig.setDateFormat(DatePattern.NORM_DATETIME_PATTERN);
```

日期时间字段建议遵循以下规则：

| 规则                                                     | 说明                                                         |
| -------------------------------------------------------- | ------------------------------------------------------------ |
| 接口层优先使用 `LocalDateTime`、`LocalDate`、`LocalTime` | 避免继续大量使用旧的 `Date`                                  |
| 对外接口明确声明格式                                     | 使用 `@JSONField(format = "...")`                            |
| 不混用多种日期格式                                       | 同一项目中不要同时出现 `yyyy/MM/dd`、时间戳、ISO 字符串等多种风格 |
| 时间范围字段成对出现                                     | 例如 `startTime`、`endTime`                                  |
| 涉及时区时单独约定                                       | 跨区域系统建议统一 UTC 或明确业务时区                        |

如果是管理后台、内部系统、单时区业务，通常使用 `yyyy-MM-dd HH:mm:ss` 更符合国内后端项目习惯。如果是开放 API 或跨时区系统，需要在接口文档中额外明确时区策略。

### JSON 字段命名规范

JSON 字段命名应在项目前期统一。Java 代码内部建议使用标准驼峰命名，JSON 对外字段可以选择驼峰或下划线，但不建议混用。

常见命名策略如下：

| 命名方式 | 示例        | 说明                                     |
| -------- | ----------- | ---------------------------------------- |
| 小驼峰   | `userName`  | JavaScript 和多数前端项目常用            |
| 下划线   | `user_name` | 开放接口、部分老系统、数据库风格接口常用 |
| 大驼峰   | `UserName`  | 不建议作为常规 JSON 字段风格             |
| 全大写   | `USER_NAME` | 不建议用于普通业务字段                   |

如果项目决定 JSON 使用下划线命名，可以在 DTO 和 VO 中使用 `@JSONField(name = "...")` 明确声明：

```java
/**
 * 用户名
 */
@JSONField(name = "user_name", alternateNames = {"username", "userName"})
private String username;
```

字段命名建议如下：

| 场景           | 建议                                                  |
| -------------- | ----------------------------------------------------- |
| 新项目内部接口 | 优先使用小驼峰，减少注解配置成本                      |
| 对外开放接口   | 可使用下划线，但必须固定规范                          |
| 兼容老接口     | 使用 `alternateNames` 接收多个历史字段名              |
| 返回字段       | 不建议同一个接口同时输出 `userName` 和 `user_name`    |
| 布尔字段       | 建议使用明确语义，例如 `enabled`、`deleted`、`locked` |

不推荐写法：

```json
{
  "userName": "ateng",
  "user_name": "ateng",
  "UserName": "ateng"
}
```

推荐写法：

```json
{
  "user_name": "ateng",
  "create_time": "2026-05-09 10:30:00",
  "roleList": ["admin", "user"]
}
```

如果团队要求严格统一，也可以将 `roleList` 调整为 `role_list`，并在 VO 中增加：

```java
@JSONField(name = "role_list")
private List<String> roleList;
```

关键点是同一项目、同一类接口、同一类字段应保持一致，避免接口消费者额外适配多种命名风格。

### 安全使用建议

Fastjson2 应作为 JSON 编解码工具使用，不应把外部 JSON 反序列化边界放得过宽。对外 HTTP 接口应使用明确 DTO 接收参数，配合参数校验和全局异常处理，避免直接解析成任意对象或开启不必要的类型特性。

安全建议如下：

| 建议                             | 说明                                                |
| -------------------------------- | --------------------------------------------------- |
| 不全局开启 `SupportAutoType`     | 外部请求不应允许通过 JSON 指定任意类型              |
| 不直接反序列化不可信复杂类型     | 外部入参应进入 DTO，而不是任意业务对象              |
| 不返回敏感字段                   | 密码、密钥、Token、身份证号、银行卡号应脱敏或不输出 |
| 限制请求体大小                   | 防止超大 JSON 造成内存压力                          |
| 使用参数校验                     | DTO 字段必须结合 `jakarta.validation` 校验          |
| 记录异常日志但不暴露堆栈         | 响应中不要输出类名、SQL、路径、堆栈信息             |
| 金额字段使用 `BigDecimal`        | 不使用 `Double` 或 `Float` 表示金额                 |
| 泛型反序列化使用 `TypeReference` | 避免泛型数据被解析成 `JSONObject` 后再强转失败      |
| 外部接口字段白名单化             | 不依赖前端传入内部控制字段                          |

敏感字段处理示例：

```java
/**
 * 用户安全响应对象
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
public class UserSecurityVO {

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 手机号脱敏值
     */
    private String phoneMask;

    /**
     * 密码摘要，不允许序列化输出
     */
    @JSONField(serialize = false)
    private String passwordHash;

    /**
     * 内部密钥，不允许序列化输出
     */
    @JSONField(serialize = false)
    private String secretKey;

}
```

手机号脱敏示例：

```java
String phone = "13812348000";
String phoneMask = StrUtil.hide(phone, 3, 7);

log.info("手机号脱敏结果：{}", phoneMask);
```

不推荐的外部入参处理方式：

```java
// 不推荐：直接接收任意 JSONObject，字段边界不清晰
@PostMapping("/unsafe")
public R<JSONObject> unsafe(@RequestBody JSONObject body) {
    return R.ok(body);
}
```

推荐的外部入参处理方式：

```java
// 推荐：使用明确 DTO 接收请求，并进行参数校验
@PostMapping("/safe")
public R<UserVO> safe(@Valid @RequestBody UserCreateDTO dto) {
    UserVO userVO = userService.create(dto);
    return R.ok(userVO);
}
```

最终建议是：Fastjson2 负责 JSON 转换，DTO 负责请求边界，VO 负责响应边界，`jakarta.validation` 负责参数校验，全局异常处理器负责统一错误响应。不要把 JSON 解析逻辑、业务校验逻辑和接口响应结构混在 Controller 中。
