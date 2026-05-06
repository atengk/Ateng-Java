# springdoc-openapi

## 文档概述

本文档用于说明在 `Spring Boot 3` 项目中集成 `springdoc-openapi` 的基础开发规范，重点覆盖 OpenAPI 文档生成、Swagger UI 页面访问、环境版本要求和 Maven 依赖配置。`springdoc-openapi` 可以根据 Spring MVC 或 WebFlux 接口自动生成 OpenAPI 3 规范文档，并提供 Swagger UI 页面用于接口查看、调试和联调。官方文档说明，Swagger UI 默认可通过 `/swagger-ui.html` 访问，OpenAPI JSON 默认可通过 `/v3/api-docs` 访问，YAML 格式可通过 `/v3/api-docs.yaml` 访问。([GitHub](https://github.com/springdoc/springdoc-openapi?utm_source=chatgpt.com))

### 背景与目标

在后端接口开发过程中，接口文档如果依赖人工维护，容易出现接口参数、返回结构、错误码和实际代码不一致的问题。`springdoc-openapi` 通过扫描 Controller、请求参数、响应对象、注解和配置，自动生成符合 OpenAPI 3 规范的接口文档，可以降低接口文档维护成本，提高前后端联调效率。

本文档的目标如下：

1. 统一 `Spring Boot 3` 项目中接口文档的依赖配置方式。
2. 明确 `springdoc-openapi` 与 Spring Boot、JDK、Maven 的版本要求。
3. 提供可直接复制到项目中的 Maven 依赖配置。
4. 为后续编写接口分组、安全认证、接口说明、参数说明、响应示例等章节提供基础环境。

### 适用范围

本文档适用于基于 `Spring Boot 3.x` 的后端项目，主要包括以下场景：

| 场景                | 说明                                              |
| ------------------- | ------------------------------------------------- |
| Spring MVC 项目     | 使用 `spring-boot-starter-web` 构建 REST API      |
| Spring WebFlux 项目 | 使用 `spring-boot-starter-webflux` 构建响应式 API |
| 前后端分离项目      | 为前端、移动端、第三方系统提供接口文档            |
| 微服务项目          | 为单个服务或多个服务提供独立接口文档              |
| 内部管理系统        | 为后台管理接口提供在线调试页面                    |
| 第三方开放接口      | 输出标准 OpenAPI 3 规范文档                       |

本文档不重点覆盖以下内容：

1. Swagger UI 深度定制。
2. OpenAPI 文档网关聚合。
3. 接口文档权限隔离。
4. Knife4j、Apifox、Postman 等第三方接口文档平台集成。
5. Spring Boot 2 项目的旧版 `springdoc-openapi` 配置。

需要注意，`springdoc-openapi v1.x` 主要面向 Spring Boot 1.x 和 2.x，Spring Boot 3 项目应使用 `springdoc-openapi v2.x` 的 starter 依赖。官方 GitHub 说明 `v1.8.0` 是支持 Spring Boot 2.x 和 1.x 的最新开源版本；Spring Boot 3.x 应使用 `springdoc-openapi-starter-*` 系列依赖。([GitHub](https://github.com/springdoc/springdoc-openapi?utm_source=chatgpt.com))

## 环境准备

本节用于明确项目集成 `springdoc-openapi` 前需要满足的基础环境。建议在项目初始化阶段统一确认 Spring Boot、JDK、Maven 和 springdoc-openapi 的版本，避免后续出现依赖冲突、类缺失或 Jakarta 包迁移问题。

### Spring Boot 版本要求

`springdoc-openapi` 在 Spring Boot 3 项目中应使用 `2.x` 版本的 starter 依赖，例如：

```xml
<artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
```

推荐版本组合如下：

| 组件              | 推荐版本                  | 说明                                             |
| ----------------- | ------------------------- | ------------------------------------------------ |
| Spring Boot       | `3.3.x`、`3.4.x`、`3.5.x` | 推荐使用仍处于维护期的 Spring Boot 3 版本        |
| springdoc-openapi | `2.8.17`                  | 当前 springdoc 官方文档展示的 v2 版本为 `2.8.17` |
| Spring Framework  | 由 Spring Boot 管理       | 不建议单独指定版本                               |
| Servlet API       | Jakarta Servlet           | Spring Boot 3 已从 `javax.*` 迁移到 `jakarta.*`  |

截至当前文档编写时，springdoc 官方文档页面展示的版本为 `springdoc-openapi v2.8.17`，并提供 `springdoc-openapi-starter-webmvc-ui`、`springdoc-openapi-starter-webflux-ui` 等 starter 依赖示例。([OpenAPI 3 Library for spring-boot](https://springdoc.org/?utm_source=chatgpt.com)) Maven Central 中 `springdoc-openapi-starter-webmvc-ui` 的 `2.8.17` 版本发布时间为 2026-04-11。([Maven Repository](https://repo.maven.apache.org/maven2/org/springdoc/springdoc-openapi-starter-webmvc-ui/?utm_source=chatgpt.com))

Spring Boot 3 项目不建议使用以下旧依赖：

```xml
<!-- 不推荐：这是旧版 springdoc-openapi 依赖形式，主要用于 Spring Boot 2 -->
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-ui</artifactId>
    <version>1.8.0</version>
</dependency>
```

Spring Boot 3 推荐使用以下新依赖形式：

```xml
<!-- 推荐：Spring Boot 3 使用 springdoc-openapi v2 starter -->
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.8.17</version>
</dependency>
```

### JDK 版本要求

Spring Boot 3 至少要求 `JDK 17`。因此，集成 `springdoc-openapi` 的 Spring Boot 3 项目也应使用 `JDK 17` 或更高版本。

推荐配置如下：

| 项目           | 推荐值                      | 说明                                |
| -------------- | --------------------------- | ----------------------------------- |
| 最低 JDK 版本  | `17`                        | Spring Boot 3 的最低要求            |
| 推荐 JDK 版本  | `17` 或 `21`                | JDK 17 稳定通用，JDK 21 为 LTS 版本 |
| 编译版本       | `17`                        | 与生产运行环境保持一致              |
| Maven 编译参数 | `maven.compiler.release=17` | 推荐使用 `release` 控制编译目标     |

Spring Boot 3.5.x 文档说明至少需要 Java 17，并需要 Spring Framework 6.2.x 或更高版本；Maven 需要 `3.6.3` 或更高版本。([spring-doc.cn](https://www.spring-doc.cn/spring-boot/3.5.0/system-requirements.html?utm_source=chatgpt.com))

Maven 中建议统一配置 Java 版本：

```xml
<properties>
    <!-- Spring Boot 3 最低要求 JDK 17 -->
    <java.version>17</java.version>

    <!-- Maven 编译目标版本，避免本地 JDK 版本过高导致运行环境不兼容 -->
    <maven.compiler.release>17</maven.compiler.release>
</properties>
```

本地环境可以通过以下命令检查版本：

```bash
java -version
mvn -version
```

输出中应确认以下内容：

1. `java version` 或 `openjdk version` 不低于 `17`。
2. `Apache Maven` 不低于 `3.6.3`。
3. Maven 使用的 Java 版本与项目要求一致。

### Maven 依赖配置

本节给出 Spring Boot 3 集成 `springdoc-openapi` 的常用 Maven 配置。普通 Spring MVC 项目优先使用 `springdoc-openapi-starter-webmvc-ui`，如果项目使用 WebFlux，则改用 `springdoc-openapi-starter-webflux-ui`。

Spring MVC 项目的基础 `pom.xml` 配置如下：

```xml
<properties>
    <!-- Spring Boot 3 最低要求 JDK 17 -->
    <java.version>17</java.version>

    <!-- springdoc-openapi v2 用于 Spring Boot 3.x 项目 -->
    <springdoc-openapi.version>2.8.17</springdoc-openapi.version>
</properties>

<dependencies>
    <!-- Spring MVC Web 依赖，用于构建 REST API -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- 参数校验依赖，用于 @Valid、@NotBlank、@NotNull 等注解 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- springdoc-openapi Swagger UI，适用于 Spring MVC 项目 -->
    <dependency>
        <groupId>org.springdoc</groupId>
        <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
        <version>${springdoc-openapi.version}</version>
    </dependency>

    <!-- Lombok 简化实体、DTO、VO 代码，编译期生效 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- Hutool 工具类库，可用于字符串、集合、日期、JSON 等常用工具处理 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.8.38</version>
    </dependency>
</dependencies>
```

如果项目是 WebFlux 响应式项目，则使用以下依赖替换 Web MVC 相关配置：

```xml
<dependencies>
    <!-- Spring WebFlux 依赖，用于构建响应式 REST API -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-webflux</artifactId>
    </dependency>

    <!-- 参数校验依赖，用于请求参数和请求体校验 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- springdoc-openapi Swagger UI，适用于 Spring WebFlux 项目 -->
    <dependency>
        <groupId>org.springdoc</groupId>
        <artifactId>springdoc-openapi-starter-webflux-ui</artifactId>
        <version>${springdoc-openapi.version}</version>
    </dependency>
</dependencies>
```

如果项目只需要生成 OpenAPI JSON 或 YAML 文档，不需要 Swagger UI 页面，可以使用 API-only 依赖：

```xml
<dependencies>
    <!-- 仅生成 OpenAPI 文档接口，不引入 Swagger UI 页面 -->
    <dependency>
        <groupId>org.springdoc</groupId>
        <artifactId>springdoc-openapi-starter-webmvc-api</artifactId>
        <version>${springdoc-openapi.version}</version>
    </dependency>
</dependencies>
```

依赖添加完成后，启动 Spring Boot 项目，默认可访问以下地址：

| 地址                                     | 说明              |
| ---------------------------------------- | ----------------- |
| `http://localhost:8080/swagger-ui.html`  | Swagger UI 页面   |
| `http://localhost:8080/v3/api-docs`      | OpenAPI JSON 文档 |
| `http://localhost:8080/v3/api-docs.yaml` | OpenAPI YAML 文档 |

如果项目配置了 `server.servlet.context-path`，访问地址需要加上上下文路径。例如上下文路径为 `/api` 时，Swagger UI 地址为：

```text
http://localhost:8080/api/swagger-ui.html
```

推荐在 `application.yml` 中补充基础配置，便于统一管理接口文档路径：

```yaml
springdoc:
  api-docs:
    # OpenAPI JSON 文档地址，默认 /v3/api-docs
    path: /v3/api-docs
  swagger-ui:
    # Swagger UI 页面地址，默认 /swagger-ui.html
    path: /swagger-ui.html
    # 按接口路径排序，便于查看
    operations-sorter: alpha
    # 按标签排序，便于模块化展示
    tags-sorter: alpha
```

完成依赖配置后，可以执行以下命令启动项目并验证：

```bash
mvn clean spring-boot:run
```

启动成功后访问：

```text
http://localhost:8080/swagger-ui.html
```

如果页面可以正常打开，并且 `/v3/api-docs` 返回 OpenAPI JSON 数据，说明 `springdoc-openapi` 基础环境配置完成。

## 基础集成

本节用于完成 `springdoc-openapi` 在 Spring Boot 3 项目中的最小可用集成。完成本节配置后，项目启动即可生成 OpenAPI JSON 文档，并通过 Swagger UI 页面查看和调试接口。`springdoc-openapi-starter-webmvc-ui` 会自动为 Spring MVC 项目集成 Swagger UI，默认 OpenAPI JSON 地址为 `/v3/api-docs`，Swagger UI 页面地址为 `/swagger-ui.html`。([GitHub](https://github.com/springdoc/springdoc-openapi?utm_source=chatgpt.com))

### 引入 springdoc-openapi 依赖

Spring Boot 3 项目应使用 `springdoc-openapi v2` 的 starter 依赖，不建议继续使用旧版 `springdoc-openapi-ui`。如果项目基于 `spring-boot-starter-web`，使用 `springdoc-openapi-starter-webmvc-ui`；如果项目基于 `spring-boot-starter-webflux`，则使用 `springdoc-openapi-starter-webflux-ui`。([OpenAPI 3 Library for spring-boot](https://springdoc.org/modules.html?utm_source=chatgpt.com))

普通 Spring MVC 项目在 `pom.xml` 中加入以下依赖：

```xml
<properties>
    <!-- Spring Boot 3 最低要求 JDK 17 -->
    <java.version>17</java.version>

    <!-- springdoc-openapi v2 用于 Spring Boot 3.x 项目 -->
    <springdoc-openapi.version>2.8.17</springdoc-openapi.version>
</properties>

<dependencies>
    <!-- Spring MVC Web 依赖，用于构建 REST API -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- springdoc-openapi Swagger UI，适用于 Spring MVC 项目 -->
    <dependency>
        <groupId>org.springdoc</groupId>
        <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
        <version>${springdoc-openapi.version}</version>
    </dependency>

    <!-- 参数校验依赖，用于 @Valid、@NotBlank、@NotNull 等接口参数校验 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- Lombok 简化 Java Bean、日志对象和构造方法代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- Hutool 工具类库，用于字符串、集合、日期、JSON 等常用处理 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.8.38</version>
    </dependency>
</dependencies>
```

如果项目使用 WebFlux，则替换为以下依赖：

```xml
<dependencies>
    <!-- Spring WebFlux 依赖，用于构建响应式 REST API -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-webflux</artifactId>
    </dependency>

    <!-- springdoc-openapi Swagger UI，适用于 Spring WebFlux 项目 -->
    <dependency>
        <groupId>org.springdoc</groupId>
        <artifactId>springdoc-openapi-starter-webflux-ui</artifactId>
        <version>${springdoc-openapi.version}</version>
    </dependency>

    <!-- 参数校验依赖，用于接口请求参数校验 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
</dependencies>
```

依赖添加完成后，执行以下命令检查项目依赖是否可以正常解析：

```bash
mvn clean dependency:tree
```

如果依赖正常解析，继续启动项目：

```bash
mvn spring-boot:run
```

### 配置 OpenAPI 基础信息

OpenAPI 基础信息用于定义接口文档的标题、描述、版本、联系人、许可证和服务地址。建议在项目中单独创建配置类，集中管理文档元数据，避免将说明信息分散到 Controller 中。

文件位置：`src/main/java/io/github/atengk/config/OpenApiConfig.java`

以下配置类用于声明 OpenAPI 文档基础信息，并初始化接口文档标题、版本、描述和服务地址。

```java
package io.github.atengk.config;

import cn.hutool.core.util.StrUtil;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI 文档配置
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenApi() {
        String title = "Spring Boot 3 接口文档";
        String version = "v1.0.0";
        String description = "基于 springdoc-openapi 生成的 REST API 在线文档";

        log.info("初始化 OpenAPI 文档配置，标题：{}，版本：{}", title, version);

        return new OpenAPI()
                .info(new Info()
                        .title(title)
                        .version(version)
                        .description(description)
                        .contact(new Contact()
                                .name("Ateng")
                                .email("ateng@example.com")
                                .url("https://example.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(List.of(
                        new Server()
                                .url("/")
                                .description("当前服务地址"),
                        new Server()
                                .url(StrUtil.blankToDefault(System.getenv("API_SERVER_URL"), "http://localhost:8080"))
                                .description("本地或环境变量指定地址")
                ))
                .externalDocs(new ExternalDocumentation()
                        .description("项目接口说明文档")
                        .url("https://example.com/docs"));
    }

}
```

该配置类的作用是向 Spring 容器注册一个 `OpenAPI` Bean。`springdoc-openapi` 会读取该 Bean，并将其中的 `Info`、`Server`、`Contact`、`License` 等信息展示到 Swagger UI 页面中。官方示例也使用 `OpenAPI` Bean 来配置接口文档的标题、描述、版本、许可证和外部文档信息。([OpenAPI 3 Library for spring-boot](https://springdoc.org/v4/index.html?utm_source=chatgpt.com))

配置完成后，启动项目并访问：

```text
http://localhost:8080/swagger-ui.html
```

页面顶部应展示配置的标题：

```text
Spring Boot 3 接口文档
```

### 启用 Swagger UI 页面

只要引入 `springdoc-openapi-starter-webmvc-ui` 或 `springdoc-openapi-starter-webflux-ui`，Swagger UI 页面默认会自动启用。Spring MVC 项目默认访问地址为：

```text
http://localhost:8080/swagger-ui.html
```

OpenAPI JSON 文档默认访问地址为：

```text
http://localhost:8080/v3/api-docs
```

OpenAPI YAML 文档默认访问地址为：

```text
http://localhost:8080/v3/api-docs.yaml
```

官方文档说明，Swagger UI 页面默认挂载在 `/swagger-ui.html`，OpenAPI JSON 默认挂载在 `/v3/api-docs`，YAML 文档可通过 `/v3/api-docs.yaml` 访问。([GitHub](https://github.com/springdoc/springdoc-openapi?utm_source=chatgpt.com))

如果需要显式启用 Swagger UI 和 API Docs，可以在 `application.yml` 中加入以下配置：

```yaml
springdoc:
  api-docs:
    # 是否启用 OpenAPI JSON 文档接口，默认 true
    enabled: true
  swagger-ui:
    # 是否启用 Swagger UI 页面
    enabled: true
```

启动项目后，可以通过以下命令验证 OpenAPI JSON 是否正常生成：

```bash
curl http://localhost:8080/v3/api-docs
```

正常情况下会返回包含 `openapi`、`info`、`paths`、`components` 等字段的 JSON 数据：

```json
{
  "openapi": "3.1.0",
  "info": {
    "title": "Spring Boot 3 接口文档",
    "description": "基于 springdoc-openapi 生成的 REST API 在线文档",
    "version": "v1.0.0"
  },
  "paths": {}
}
```

## 常用配置

本节用于说明 `springdoc-openapi` 在实际项目中的常用配置项，包括访问路径、API 分组、扫描包路径和接口排序。建议将这些配置放在 `application.yml` 中统一维护，复杂分组场景再补充 Java 配置类。

### 文档访问路径配置

文档访问路径配置用于修改 OpenAPI JSON、YAML 和 Swagger UI 页面的默认访问地址。`springdoc.api-docs.path` 可用于修改 OpenAPI JSON 文档路径，默认值为 `/v3/api-docs`；`springdoc.swagger-ui.path` 可用于修改 Swagger UI 页面路径。([OpenAPI 3 Library for spring-boot](https://springdoc.org/properties?utm_source=chatgpt.com))

文件位置：`src/main/resources/application.yml`

以下配置用于将 OpenAPI JSON 文档地址调整为 `/openapi/api-docs`，将 Swagger UI 页面地址调整为 `/openapi/swagger-ui.html`。

```yaml
springdoc:
  api-docs:
    # OpenAPI JSON 文档路径，默认 /v3/api-docs
    path: /openapi/api-docs
    # 是否启用 OpenAPI 文档接口
    enabled: true
  swagger-ui:
    # Swagger UI 页面路径，默认 /swagger-ui.html
    path: /openapi/swagger-ui.html
    # 是否启用 Swagger UI 页面
    enabled: true
```

配置完成后，访问地址变为：

```text
http://localhost:8080/openapi/swagger-ui.html
http://localhost:8080/openapi/api-docs
```

如果项目配置了统一上下文路径，例如：

```yaml
server:
  servlet:
    # 应用上下文路径
    context-path: /admin
```

则完整访问地址需要加上上下文路径：

```text
http://localhost:8080/admin/openapi/swagger-ui.html
http://localhost:8080/admin/openapi/api-docs
```

### API 分组配置

API 分组用于将不同模块、不同版本或不同业务域的接口拆分展示。例如将后台管理接口、移动端接口、开放平台接口分别展示，便于开发人员按模块查看接口。

可以直接通过 `application.yml` 配置分组。`springdoc.group-configs` 支持配置 `group`、`display-name`、`packages-to-scan`、`paths-to-match`、`paths-to-exclude` 等属性。([OpenAPI 3 Library for spring-boot](https://springdoc.org/properties?utm_source=chatgpt.com))

文件位置：`src/main/resources/application.yml`

以下配置按接口路径拆分管理端、移动端和开放平台接口。

```yaml
springdoc:
  group-configs:
    - group: admin
      # Swagger UI 中展示的分组名称
      display-name: 后台管理接口
      # 只匹配后台管理接口路径
      paths-to-match:
        - /admin/**
    - group: app
      # Swagger UI 中展示的分组名称
      display-name: 移动端接口
      # 只匹配移动端接口路径
      paths-to-match:
        - /app/**
    - group: open
      # Swagger UI 中展示的分组名称
      display-name: 开放平台接口
      # 只匹配开放接口路径
      paths-to-match:
        - /open/**
```

也可以通过 Java 配置类声明分组。官方文档示例中，多个 OpenAPI 定义可通过 `GroupedOpenApi` Bean 按路径或包名拆分，并生成类似 `/v3/api-docs/{groupName}` 的分组文档地址。([OpenAPI 3 Library for spring-boot](https://springdoc.org/v4/index.html?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/config/OpenApiGroupConfig.java`

以下配置类用于通过 `GroupedOpenApi` Bean 创建多个接口文档分组。

```java
package io.github.atengk.config;

import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 分组配置
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Configuration
public class OpenApiGroupConfig {

    @Bean
    public GroupedOpenApi adminApi() {
        log.info("初始化 OpenAPI 分组：后台管理接口");
        return GroupedOpenApi.builder()
                .group("admin")
                .displayName("后台管理接口")
                .pathsToMatch("/admin/**")
                .build();
    }

    @Bean
    public GroupedOpenApi appApi() {
        log.info("初始化 OpenAPI 分组：移动端接口");
        return GroupedOpenApi.builder()
                .group("app")
                .displayName("移动端接口")
                .pathsToMatch("/app/**")
                .build();
    }

    @Bean
    public GroupedOpenApi openApi() {
        log.info("初始化 OpenAPI 分组：开放平台接口");
        return GroupedOpenApi.builder()
                .group("open")
                .displayName("开放平台接口")
                .pathsToMatch("/open/**")
                .build();
    }

}
```

分组配置完成后，可以分别访问以下文档地址：

```text
http://localhost:8080/v3/api-docs/admin
http://localhost:8080/v3/api-docs/app
http://localhost:8080/v3/api-docs/open
```

Swagger UI 页面中也会出现对应的分组下拉选项，便于按模块查看接口。

### 扫描包路径配置

扫描包路径配置用于限制 `springdoc-openapi` 只扫描指定包下的 Controller。对于大型项目或多模块项目，建议配置扫描包路径，避免将不需要暴露的接口、内部测试接口或公共组件接口展示到文档中。`springdoc.packages-to-scan` 用于配置全局扫描包路径，默认会扫描应用自动配置范围内的接口。([OpenAPI 3 Library for spring-boot](https://springdoc.org/properties?utm_source=chatgpt.com))

文件位置：`src/main/resources/application.yml`

以下配置只扫描 `io.github.atengk` 包下的接口。

```yaml
springdoc:
  # 指定需要扫描的 Controller 包路径
  packages-to-scan:
    - io.github.atengk
  # 指定需要匹配的接口路径
  paths-to-match:
    - /**
```

如果项目按模块拆包，可以按分组配置不同扫描包：

```yaml
springdoc:
  group-configs:
    - group: user
      display-name: 用户模块
      # 只扫描用户模块 Controller
      packages-to-scan:
        - io.github.atengk.user.controller
      paths-to-match:
        - /user/**
    - group: system
      display-name: 系统模块
      # 只扫描系统模块 Controller
      packages-to-scan:
        - io.github.atengk.system.controller
      paths-to-match:
        - /system/**
```

也可以通过 Java 配置类进行精细控制：

```java
package io.github.atengk.config;

import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 模块扫描配置
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Configuration
public class OpenApiScanConfig {

    @Bean
    public GroupedOpenApi userModuleApi() {
        log.info("初始化 OpenAPI 扫描配置：用户模块");
        return GroupedOpenApi.builder()
                .group("user")
                .displayName("用户模块")
                .packagesToScan("io.github.atengk.user.controller")
                .pathsToMatch("/user/**")
                .build();
    }

    @Bean
    public GroupedOpenApi systemModuleApi() {
        log.info("初始化 OpenAPI 扫描配置：系统模块");
        return GroupedOpenApi.builder()
                .group("system")
                .displayName("系统模块")
                .packagesToScan("io.github.atengk.system.controller")
                .pathsToMatch("/system/**")
                .build();
    }

}
```

扫描包路径和接口路径可以同时配置。两者同时存在时，接口需要同时满足包路径和接口路径条件才会进入对应分组。这个方式适合多模块项目、DDD 分层项目和需要控制接口暴露范围的项目。

### 接口排序配置

接口排序配置用于控制 Swagger UI 页面中标签和接口的展示顺序。常见做法是按字母排序，保证每次打开文档时接口顺序稳定，便于查找和对比。`springdoc.swagger-ui.operations-sorter` 可用于配置接口排序，`springdoc.swagger-ui.tags-sorter` 可用于配置标签排序。([OpenAPI 3 Library for spring-boot](https://springdoc.org/properties?utm_source=chatgpt.com))

文件位置：`src/main/resources/application.yml`

以下配置用于按字母顺序展示标签和接口：

```yaml
springdoc:
  swagger-ui:
    # 接口按路径或方法名排序，常用 alpha
    operations-sorter: alpha
    # 标签按名称排序，常用 alpha
    tags-sorter: alpha
```

如果希望接口按 HTTP 方法顺序展示，可以改为：

```yaml
springdoc:
  swagger-ui:
    # 接口按 HTTP Method 排序
    operations-sorter: method
    # 标签按名称排序
    tags-sorter: alpha
```

为了让排序效果更清晰，建议 Controller 使用 `@Tag` 标注模块名称，并使用 `@Operation` 标注接口名称。

文件位置：`src/main/java/io/github/atengk/user/controller/UserController.java`

以下示例 Controller 用于展示 `@Tag` 和 `@Operation` 在接口排序与文档展示中的实际效果。

```java
package io.github.atengk.user.controller;

import cn.hutool.core.util.StrUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Tag(name = "用户管理", description = "用户查询、详情等接口")
@RestController
@RequestMapping("/user")
/**
 * 用户管理接口
 *
 * @author Ateng
 * @since 2026-05-06
 */
public class UserController {

    @Operation(summary = "查询用户详情", description = "根据用户 ID 查询用户基础信息")
    @GetMapping("/{id}")
    public String detail(@PathVariable Long id) {
        log.info("查询用户详情，用户ID：{}", id);
        return StrUtil.format("用户ID：{}", id);
    }

    @Operation(summary = "查询用户列表", description = "查询用户基础列表数据")
    @GetMapping("/list")
    public String list() {
        log.info("查询用户列表");
        return "用户列表";
    }

}
```

上面的类注释位置建议调整为 Java 规范写法，即类注释放在注解之前。实际项目中推荐写成：

```java
package io.github.atengk.user.controller;

import cn.hutool.core.util.StrUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户管理接口
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Tag(name = "用户管理", description = "用户查询、详情等接口")
@RestController
@RequestMapping("/user")
public class UserController {

    @Operation(summary = "查询用户详情", description = "根据用户 ID 查询用户基础信息")
    @GetMapping("/{id}")
    public String detail(@PathVariable Long id) {
        log.info("查询用户详情，用户ID：{}", id);
        return StrUtil.format("用户ID：{}", id);
    }

    @Operation(summary = "查询用户列表", description = "查询用户基础列表数据")
    @GetMapping("/list")
    public String list() {
        log.info("查询用户列表");
        return "用户列表";
    }

}
```

完成配置后，重新启动项目：

```bash
mvn spring-boot:run
```

访问 Swagger UI 页面：

```text
http://localhost:8080/swagger-ui.html
```

检查以下内容是否生效：

| 检查项     | 预期结果                                |
| ---------- | --------------------------------------- |
| 文档标题   | 显示 `Spring Boot 3 接口文档`           |
| 文档路径   | `/v3/api-docs` 或自定义路径可以访问     |
| Swagger UI | `/swagger-ui.html` 或自定义路径可以访问 |
| API 分组   | 页面下拉框中显示配置的分组              |
| 扫描包     | 只展示指定包下的 Controller             |
| 接口排序   | 标签和接口按配置规则排序                |

## 接口注解使用

本节用于规范 Controller、请求参数、请求体、响应结果和模型对象的 OpenAPI 注解写法。`springdoc-openapi` 会自动识别 Spring MVC 注解，同时也支持 Swagger OpenAPI 3 注解增强接口描述，例如 `@Tag`、`@Operation`、`@Parameter`、`@RequestBody`、`@ApiResponse`、`@Schema` 等。`@Operation` 可用于描述接口操作，并支持 `summary`、`description`、`parameters`、`requestBody`、`responses`、`security` 等字段。([docs.swagger.io](https://docs.swagger.io/swagger-core/v2.2.2/apidocs/io/swagger/v3/oas/annotations/Operation.html?utm_source=chatgpt.com))

### 控制器接口说明

控制器接口说明用于描述接口所属模块、接口名称、接口用途和接口行为。通常在 Controller 类上使用 `@Tag` 标注模块，在具体方法上使用 `@Operation` 标注接口摘要和详细说明。

文件位置：`src/main/java/io/github/atengk/user/controller/UserController.java`

以下示例定义了用户管理接口，包含分页查询、新增用户和查询详情三个接口，适合作为接口注解规范示例。

```java
package io.github.atengk.user.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.common.model.Result;
import io.github.atengk.user.dto.UserCreateDTO;
import io.github.atengk.user.vo.UserDetailVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户管理接口
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/user")
@Tag(name = "用户管理", description = "用户查询、新增、详情等接口")
public class UserController {

    /**
     * 查询用户列表
     *
     * @param keyword  用户关键字
     * @param pageNum  页码
     * @param pageSize 每页条数
     * @return 用户列表
     */
    @GetMapping("/list")
    @Operation(summary = "查询用户列表", description = "根据关键字分页查询用户基础信息")
    public Result<List<UserDetailVO>> list(
            @Parameter(description = "用户关键字，支持用户名或手机号模糊查询", example = "ateng")
            @RequestParam(required = false) String keyword,

            @Parameter(description = "页码，从 1 开始", example = "1")
            @Min(value = 1, message = "页码不能小于 1")
            @RequestParam(defaultValue = "1") Integer pageNum,

            @Parameter(description = "每页条数，最大 100", example = "10")
            @Min(value = 1, message = "每页条数不能小于 1")
            @Max(value = 100, message = "每页条数不能大于 100")
            @RequestParam(defaultValue = "10") Integer pageSize) {

        log.info("查询用户列表，关键字：{}，页码：{}，每页条数：{}", keyword, pageNum, pageSize);

        UserDetailVO user = UserDetailVO.builder()
                .id(1L)
                .username(StrUtil.blankToDefault(keyword, "ateng"))
                .nickname("阿腾")
                .mobile("13800000000")
                .enabled(true)
                .createTime(LocalDateTime.now())
                .build();

        return Result.success(CollUtil.newArrayList(user));
    }

    /**
     * 新增用户
     *
     * @param request 用户新增参数
     * @return 用户详情
     */
    @PostMapping
    @Operation(summary = "新增用户", description = "创建一个新的系统用户")
    public Result<UserDetailVO> create(@Valid @RequestBody UserCreateDTO request) {
        log.info("新增用户，用户名：{}，手机号：{}", request.getUsername(), request.getMobile());

        UserDetailVO user = UserDetailVO.builder()
                .id(IdUtil.getSnowflakeNextId())
                .username(request.getUsername())
                .nickname(request.getNickname())
                .mobile(request.getMobile())
                .enabled(true)
                .createTime(LocalDateTime.now())
                .build();

        return Result.success(user);
    }

    /**
     * 查询用户详情
     *
     * @param id 用户ID
     * @return 用户详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询用户详情", description = "根据用户 ID 查询用户详细信息")
    public Result<UserDetailVO> detail(
            @Parameter(description = "用户ID", required = true, example = "1")
            @PathVariable Long id) {

        log.info("查询用户详情，用户ID：{}", id);

        UserDetailVO user = UserDetailVO.builder()
                .id(id)
                .username("ateng")
                .nickname("阿腾")
                .mobile("13800000000")
                .enabled(true)
                .createTime(LocalDateTime.now())
                .build();

        return Result.success(user);
    }

    /**
     * 根据手机号查询用户
     *
     * @param mobile 手机号
     * @return 用户详情
     */
    @GetMapping("/mobile/{mobile}")
    @Operation(summary = "根据手机号查询用户", description = "根据手机号查询用户基础信息")
    public Result<UserDetailVO> getByMobile(
            @Parameter(description = "手机号", required = true, example = "13800000000")
            @NotBlank(message = "手机号不能为空")
            @PathVariable String mobile) {

        log.info("根据手机号查询用户，手机号：{}", mobile);

        UserDetailVO user = UserDetailVO.builder()
                .id(1L)
                .username("ateng")
                .nickname("阿腾")
                .mobile(mobile)
                .enabled(true)
                .createTime(LocalDateTime.now())
                .build();

        return Result.success(user);
    }

}
```

接口注解建议遵循以下规则：

| 注解                   | 使用位置        | 作用                               |
| ---------------------- | --------------- | ---------------------------------- |
| `@Tag`                 | Controller 类   | 描述接口模块                       |
| `@Operation`           | Controller 方法 | 描述接口名称、用途、响应和认证     |
| `@Parameter`           | 方法参数        | 描述路径参数、查询参数、请求头参数 |
| `@Schema`              | DTO、VO、字段   | 描述模型名称、字段含义、示例值     |
| `@ApiResponse`         | Controller 方法 | 描述响应状态码和响应结构           |
| `@SecurityRequirement` | 类或方法        | 描述接口需要的认证方案             |

### 请求参数说明

请求参数说明主要用于描述 `@PathVariable`、`@RequestParam`、`@RequestHeader` 等非请求体参数。对于简单参数，推荐直接在参数上使用 `@Parameter`；对于分页、筛选等复杂查询参数，可以封装为 DTO，并配合 `@ParameterObject` 使用。

文件位置：`src/main/java/io/github/atengk/user/dto/UserQueryDTO.java`

以下 DTO 用于封装用户分页查询参数。

```java
package io.github.atengk.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 用户查询参数
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@Schema(name = "UserQueryDTO", description = "用户查询参数")
public class UserQueryDTO {

    @Schema(description = "用户关键字，支持用户名、昵称、手机号模糊查询", example = "ateng")
    private String keyword;

    @Min(value = 1, message = "页码不能小于 1")
    @Schema(description = "页码，从 1 开始", example = "1", defaultValue = "1")
    private Integer pageNum = 1;

    @Min(value = 1, message = "每页条数不能小于 1")
    @Max(value = 100, message = "每页条数不能大于 100")
    @Schema(description = "每页条数，最大 100", example = "10", defaultValue = "10")
    private Integer pageSize = 10;

    @Schema(description = "用户状态，true 表示启用，false 表示禁用", example = "true")
    private Boolean enabled;

}
```

文件位置：`src/main/java/io/github/atengk/user/controller/UserQueryController.java`

以下 Controller 演示 `@ParameterObject` 的使用方式，适合查询条件较多的接口。

```java
package io.github.atengk.user.controller;

import cn.hutool.core.collection.CollUtil;
import io.github.atengk.common.model.Result;
import io.github.atengk.user.dto.UserQueryDTO;
import io.github.atengk.user.vo.UserDetailVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户查询接口
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@RestController
@RequestMapping("/user/query")
@Tag(name = "用户查询", description = "用户分页查询和条件筛选接口")
public class UserQueryController {

    /**
     * 分页查询用户
     *
     * @param query 查询参数
     * @return 用户列表
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询用户", description = "根据查询条件分页查询用户信息")
    public Result<List<UserDetailVO>> page(@ParameterObject UserQueryDTO query) {
        log.info("分页查询用户，参数：{}", query);

        UserDetailVO user = UserDetailVO.builder()
                .id(1L)
                .username("ateng")
                .nickname("阿腾")
                .mobile("13800000000")
                .enabled(true)
                .createTime(LocalDateTime.now())
                .build();

        return Result.success(CollUtil.newArrayList(user));
    }

}
```

请求参数说明建议与参数校验注解一起使用，例如 `@NotBlank`、`@NotNull`、`@Min`、`@Max`、`@Size`。这样既能在 Swagger UI 中展示字段约束，也能在接口运行时完成参数校验。

### 请求体说明

请求体说明用于描述 `POST`、`PUT`、`PATCH` 等接口中的 JSON、表单或文件上传数据。OpenAPI 3 使用 `requestBody` 描述请求体，Swagger 官方文档也说明 OpenAPI 3 中请求体通过 `requestBody` 关键字表达，常用于创建和更新操作。([Swagger](https://swagger.io/docs/specification/v3_0/describing-request-body/describing-request-body/?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/user/dto/UserCreateDTO.java`

以下 DTO 用于描述新增用户请求体。

```java
package io.github.atengk.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户新增参数
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@Schema(name = "UserCreateDTO", description = "用户新增参数")
public class UserCreateDTO {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 32, message = "用户名长度必须在 3 到 32 个字符之间")
    @Schema(description = "用户名", requiredMode = Schema.RequiredMode.REQUIRED, example = "ateng")
    private String username;

    @NotBlank(message = "昵称不能为空")
    @Size(max = 50, message = "昵称不能超过 50 个字符")
    @Schema(description = "昵称", requiredMode = Schema.RequiredMode.REQUIRED, example = "阿腾")
    private String nickname;

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @Schema(description = "手机号", requiredMode = Schema.RequiredMode.REQUIRED, example = "13800000000")
    private String mobile;

    @Schema(description = "备注", example = "测试用户")
    private String remark;

}
```

如果需要在方法上显式描述请求体，可以使用 OpenAPI 注解中的 `@RequestBody`，注意它与 Spring MVC 的 `@RequestBody` 不是同一个注解。为了避免导入冲突，建议 OpenAPI 的请求体注解使用全限定类名。

文件位置：`src/main/java/io/github/atengk/user/controller/UserBodyController.java`

以下示例演示如何显式声明请求体说明和 JSON 媒体类型。

```java
package io.github.atengk.user.controller;

import cn.hutool.core.util.IdUtil;
import io.github.atengk.common.model.Result;
import io.github.atengk.user.dto.UserCreateDTO;
import io.github.atengk.user.vo.UserDetailVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 用户请求体接口
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@RestController
@RequestMapping("/user/body")
@Tag(name = "用户请求体", description = "用户请求体示例接口")
public class UserBodyController {

    /**
     * 新增用户
     *
     * @param request 用户新增参数
     * @return 用户详情
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "新增用户",
            description = "通过 JSON 请求体新增用户",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "用户新增参数",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = UserCreateDTO.class)
                    )
            )
    )
    public Result<UserDetailVO> create(@Valid @RequestBody UserCreateDTO request) {
        log.info("新增用户，请求体参数：{}", request);

        UserDetailVO user = UserDetailVO.builder()
                .id(IdUtil.getSnowflakeNextId())
                .username(request.getUsername())
                .nickname(request.getNickname())
                .mobile(request.getMobile())
                .enabled(true)
                .createTime(LocalDateTime.now())
                .build();

        return Result.success(user);
    }

}
```

大多数普通 JSON 请求体接口只需要在 DTO 字段上写好 `@Schema`，并在方法参数上使用 Spring MVC 的 `@RequestBody` 即可。只有需要补充媒体类型、请求体说明、示例或者特殊结构时，才需要显式声明 OpenAPI 的 `requestBody`。

### 响应结果说明

响应结果说明用于描述接口返回的状态码、响应结构和响应媒体类型。`@ApiResponse` 可用于描述接口执行后的可能响应结果，`@Content` 和 `@Schema` 可用于指定响应体模型。

文件位置：`src/main/java/io/github/atengk/common/model/Result.java`

以下是统一响应结果模型，后续接口统一返回该结构。

```java
package io.github.atengk.common.model;

import cn.hutool.core.date.DateUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 统一响应结果
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@Schema(name = "Result", description = "统一响应结果")
public class Result<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "业务状态码，200 表示成功", example = "200")
    private Integer code;

    @Schema(description = "响应消息", example = "操作成功")
    private String message;

    @Schema(description = "响应数据")
    private T data;

    @Schema(description = "响应时间", example = "2026-05-06 10:30:00")
    private String timestamp;

    /**
     * 返回成功结果
     *
     * @param data 响应数据
     * @param <T>  数据类型
     * @return 统一响应结果
     */
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage("操作成功");
        result.setData(data);
        result.setTimestamp(DateUtil.now());
        return result;
    }

    /**
     * 返回失败结果
     *
     * @param code    业务状态码
     * @param message 错误消息
     * @param <T>     数据类型
     * @return 统一响应结果
     */
    public static <T> Result<T> fail(Integer code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        result.setTimestamp(DateUtil.now());
        return result;
    }

}
```

文件位置：`src/main/java/io/github/atengk/user/controller/UserResponseController.java`

以下示例演示如何通过 `@ApiResponse` 描述接口成功、参数错误和未授权响应。

```java
package io.github.atengk.user.controller;

import io.github.atengk.common.model.Result;
import io.github.atengk.user.vo.UserDetailVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 用户响应结果接口
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@RestController
@RequestMapping("/user/response")
@Tag(name = "用户响应结果", description = "用户响应结构示例接口")
public class UserResponseController {

    /**
     * 查询用户详情
     *
     * @param id 用户ID
     * @return 用户详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询用户详情", description = "根据用户 ID 查询用户详情，并展示响应结构说明")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "查询成功",
                    content = @Content(schema = @Schema(implementation = Result.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "请求参数错误",
                    content = @Content(schema = @Schema(implementation = Result.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "未认证或 Token 已失效",
                    content = @Content(schema = @Schema(implementation = Result.class))
            )
    })
    public Result<UserDetailVO> detail(@PathVariable Long id) {
        log.info("查询用户详情，用户ID：{}", id);

        UserDetailVO user = UserDetailVO.builder()
                .id(id)
                .username("ateng")
                .nickname("阿腾")
                .mobile("13800000000")
                .enabled(true)
                .createTime(LocalDateTime.now())
                .build();

        return Result.success(user);
    }

}
```

在实际项目中，如果所有接口都使用统一响应结构，不建议每个接口都重复写完整的 `@ApiResponses`。可以只在关键接口、开放接口、第三方接口或错误响应较复杂的接口上显式声明响应说明。

### Schema 模型说明

Schema 模型说明用于描述 DTO、VO、枚举和字段结构。建议所有对外展示的请求对象和响应对象都使用 `@Schema` 标注，保证 Swagger UI 中的模型说明清晰可读。

文件位置：`src/main/java/io/github/atengk/user/vo/UserDetailVO.java`

以下 VO 用于描述用户详情响应模型。

```java
package io.github.atengk.user.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户详情响应
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@Builder
@Schema(name = "UserDetailVO", description = "用户详情响应")
public class UserDetailVO {

    @Schema(description = "用户ID", example = "1")
    private Long id;

    @Schema(description = "用户名", example = "ateng")
    private String username;

    @Schema(description = "昵称", example = "阿腾")
    private String nickname;

    @Schema(description = "手机号", example = "13800000000")
    private String mobile;

    @Schema(description = "是否启用", example = "true")
    private Boolean enabled;

    @Schema(description = "创建时间", example = "2026-05-06 10:30:00")
    private LocalDateTime createTime;

}
```

枚举字段建议使用 `@Schema` 描述枚举含义，并在字段说明中明确可选值。

文件位置：`src/main/java/io/github/atengk/user/enums/UserStatusEnum.java`

以下枚举用于描述用户状态。

```java
package io.github.atengk.user.enums;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 用户状态枚举
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Getter
@AllArgsConstructor
@Schema(name = "UserStatusEnum", description = "用户状态枚举")
public enum UserStatusEnum {

    ENABLED(1, "启用"),

    DISABLED(0, "禁用");

    @Schema(description = "状态值", example = "1")
    private final Integer code;

    @Schema(description = "状态名称", example = "启用")
    private final String name;

}
```

Schema 注解建议遵循以下规则：

| 类型     | 建议                                               |
| -------- | -------------------------------------------------- |
| DTO 类   | 使用 `@Schema(name = "...", description = "...")`  |
| VO 类    | 使用 `@Schema(name = "...", description = "...")`  |
| 字段     | 明确 `description` 和 `example`                    |
| 必填字段 | 使用 `requiredMode = Schema.RequiredMode.REQUIRED` |
| 枚举字段 | 在描述中列出可选值                                 |
| 时间字段 | 给出统一格式示例                                   |
| 金额字段 | 说明单位，例如元、分、美元                         |

## 认证与安全

本节用于说明接口文档页面访问控制、JWT 认证展示和全局请求头配置。需要区分两类安全：一类是接口文档页面本身的访问控制，另一类是业务接口在 Swagger UI 调试时携带 Token 或全局请求头。

### Knife4j 与 Swagger UI 访问控制

Swagger UI 默认在引入 `springdoc-openapi-starter-webmvc-ui` 后自动启用，默认页面路径为 `/swagger-ui.html`，OpenAPI JSON 默认路径为 `/v3/api-docs`。([GitHub](https://github.com/springdoc/springdoc-openapi?utm_source=chatgpt.com)) 在生产环境中，不建议让接口文档页面完全公开访问，常见做法是通过 Spring Security 对文档路径进行访问控制，或者在生产环境关闭文档入口。

如果项目使用 Knife4j，需要注意 Spring Boot 3 只支持 OpenAPI 3 规范，Knife4j 的 Spring Boot 3 starter 已经引用 springdoc-openapi 相关 jar，项目中应避免重复引入造成版本冲突；Knife4j 文档也明确 Spring Boot 3 场景要求 JDK 版本不低于 17。([doc.xiaominfo.com](https://doc.xiaominfo.com/docs/quick-start?utm_source=chatgpt.com))

如需引入 Knife4j，可以使用以下依赖。注意：如果已经引入 Knife4j starter，通常不要再单独引入 `springdoc-openapi-starter-webmvc-ui`，避免 springdoc 版本被重复管理。

```xml
<properties>
    <!-- Knife4j Spring Boot 3 使用 Jakarta 版本 starter -->
    <knife4j.version>4.4.0</knife4j.version>
</properties>

<dependencies>
    <!-- Knife4j OpenAPI3 Jakarta Starter，适用于 Spring Boot 3 -->
    <dependency>
        <groupId>com.github.xiaoymin</groupId>
        <artifactId>knife4j-openapi3-jakarta-spring-boot-starter</artifactId>
        <version>${knife4j.version}</version>
    </dependency>
</dependencies>
```

Knife4j 常用页面地址为：

```text
http://localhost:8080/doc.html
```

Swagger UI 常用页面地址为：

```text
http://localhost:8080/swagger-ui.html
```

建议通过环境配置控制文档是否启用。

文件位置：`src/main/resources/application-dev.yml`

开发环境启用接口文档。

```yaml
springdoc:
  api-docs:
    # 开发环境启用 OpenAPI 文档
    enabled: true
  swagger-ui:
    # 开发环境启用 Swagger UI
    enabled: true

knife4j:
  # 开发环境启用 Knife4j 增强功能
  enable: true
```

文件位置：`src/main/resources/application-prod.yml`

生产环境关闭接口文档入口。

```yaml
springdoc:
  api-docs:
    # 生产环境建议关闭 OpenAPI 文档接口
    enabled: false
  swagger-ui:
    # 生产环境建议关闭 Swagger UI 页面
    enabled: false

knife4j:
  # 生产环境建议关闭 Knife4j 页面
  enable: false
```

如果生产环境必须开放接口文档，应通过 Spring Security 限制访问权限。

文件位置：`src/main/java/io/github/atengk/config/SecurityConfig.java`

以下配置演示如何保护 Swagger UI、OpenAPI JSON 和 Knife4j 页面路径。

```java
package io.github.atengk.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security 安全配置
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Configuration
public class SecurityConfig {

    /**
     * 配置安全过滤链
     *
     * @param http HTTP 安全配置
     * @return 安全过滤链
     * @throws Exception 配置异常
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        log.info("初始化 Spring Security 配置");

        return http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // 登录接口允许匿名访问
                        .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()

                        // 接口文档路径需要登录后访问
                        .requestMatchers(
                                "/doc.html",
                                "/webjars/**",
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs",
                                "/v3/api-docs/**",
                                "/v3/api-docs.yaml"
                        ).authenticated()

                        // 其他接口按业务要求认证
                        .anyRequest().authenticated()
                )
                .httpBasic(Customizer.withDefaults())
                .build();
    }

}
```

如果只是内部开发环境使用，也可以将文档路径放行：

```java
.requestMatchers(
        "/doc.html",
        "/webjars/**",
        "/swagger-ui.html",
        "/swagger-ui/**",
        "/v3/api-docs",
        "/v3/api-docs/**",
        "/v3/api-docs.yaml"
).permitAll()
```

生产环境推荐优先关闭文档入口；如果确实需要保留，应放在 VPN、内网网关、统一认证或白名单之后。

### JWT 认证配置

JWT 认证配置用于让 Swagger UI 页面支持 `Authorization: Bearer xxx` 请求头。配置后，Swagger UI 页面会出现 `Authorize` 按钮，开发人员可以输入 JWT Token，后续调试接口时会自动携带认证头。

文件位置：`src/main/java/io/github/atengk/config/OpenApiSecurityConfig.java`

以下配置通过 `SecurityScheme` 声明 Bearer JWT 认证，并将其作为全局安全要求。

```java
package io.github.atengk.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 认证配置
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Configuration
public class OpenApiSecurityConfig {

    private static final String JWT_SECURITY_SCHEME_NAME = "BearerAuth";

    /**
     * 配置 OpenAPI JWT 认证信息
     *
     * @return OpenAPI 配置
     */
    @Bean
    public OpenAPI openApiSecurity() {
        log.info("初始化 OpenAPI JWT 认证配置，认证方案：{}", JWT_SECURITY_SCHEME_NAME);

        return new OpenAPI()
                .components(new Components()
                        .addSecuritySchemes(JWT_SECURITY_SCHEME_NAME, new SecurityScheme()
                                .name("Authorization")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("请输入 JWT Token，格式：Bearer {token}")))
                .addSecurityItem(new SecurityRequirement().addList(JWT_SECURITY_SCHEME_NAME));
    }

}
```

如果前面已经定义过 `OpenAPI customOpenApi()` 基础信息配置，不建议再额外声明另一个 `OpenAPI` Bean，避免配置覆盖或 Bean 冲突。推荐把基础信息和 JWT 认证合并到同一个配置类中。

文件位置：`src/main/java/io/github/atengk/config/OpenApiConfig.java`

以下配置将文档基础信息和 JWT 认证配置合并在一个 `OpenAPI` Bean 中。

```java
package io.github.atengk.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 文档配置
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Configuration
public class OpenApiConfig {

    private static final String JWT_SECURITY_SCHEME_NAME = "BearerAuth";

    /**
     * 配置 OpenAPI 基础信息和认证信息
     *
     * @return OpenAPI 配置
     */
    @Bean
    public OpenAPI customOpenApi() {
        log.info("初始化 OpenAPI 文档配置和 JWT 认证配置");

        return new OpenAPI()
                .info(new Info()
                        .title("Spring Boot 3 接口文档")
                        .version("v1.0.0")
                        .description("基于 springdoc-openapi 生成的 REST API 在线文档")
                        .contact(new Contact()
                                .name("Ateng")
                                .email("ateng@example.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .externalDocs(new ExternalDocumentation()
                        .description("项目接口说明文档")
                        .url("https://example.com/docs"))
                .components(new Components()
                        .addSecuritySchemes(JWT_SECURITY_SCHEME_NAME, new SecurityScheme()
                                .name("Authorization")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("请输入 JWT Token，格式：Bearer {token}")))
                .addSecurityItem(new SecurityRequirement().addList(JWT_SECURITY_SCHEME_NAME));
    }

}
```

如果只希望部分接口需要 JWT 认证，可以不配置全局 `addSecurityItem`，改为在 Controller 或方法上使用 `@SecurityRequirement`。

文件位置：`src/main/java/io/github/atengk/order/controller/OrderController.java`

以下示例仅对订单接口声明 JWT 认证要求。

```java
package io.github.atengk.order.controller;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.common.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 订单管理接口
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@RestController
@RequestMapping("/order")
@Tag(name = "订单管理", description = "订单查询和处理接口")
@SecurityRequirement(name = "BearerAuth")
public class OrderController {

    /**
     * 查询订单详情
     *
     * @param id 订单ID
     * @return 订单详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询订单详情", description = "根据订单 ID 查询订单详情")
    public Result<String> detail(@PathVariable Long id) {
        log.info("查询订单详情，订单ID：{}", id);
        return Result.success(StrUtil.format("订单ID：{}", id));
    }

}
```

Swagger UI 调试时，点击页面右上角 `Authorize` 按钮，输入以下格式：

```text
Bearer eyJhbGciOiJIUzI1NiJ9.xxxxx.yyyyy
```

如果 `SecurityScheme` 使用 `type = HTTP`、`scheme = bearer`，通常输入原始 Token 或 `Bearer Token` 都能被 Swagger UI 处理；为避免团队使用方式不一致，建议文档中统一约定输入完整格式。

### 全局请求头配置

全局请求头配置用于在所有接口文档中统一追加固定请求头，例如 `Authorization`、`X-Tenant-Id`、`X-Trace-Id`、`X-Client-Version` 等。对于认证头，优先使用前面的 `SecurityScheme`；对于租户、链路追踪、客户端版本等业务头，可以使用 `OperationCustomizer` 统一追加。springdoc 的配置项中也支持设置 API 文档路径、Swagger UI 路径、扫描包和排序等属性，适合基础配置；更复杂的接口级增强通常使用 Java 自定义器完成。([OpenAPI 3 Library for spring-boot](https://springdoc.org/properties?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/config/OpenApiHeaderConfig.java`

以下配置会给所有接口追加租户 ID、链路 ID 和客户端版本请求头。

```java
package io.github.atengk.config;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.parameters.Parameter;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerMethod;

import java.util.ArrayList;
import java.util.List;

/**
 * OpenAPI 全局请求头配置
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Configuration
public class OpenApiHeaderConfig {

    /**
     * 自定义接口全局请求头
     *
     * @return 操作自定义器
     */
    @Bean
    public OperationCustomizer globalHeaderCustomizer() {
        log.info("初始化 OpenAPI 全局请求头配置");

        return (Operation operation, HandlerMethod handlerMethod) -> {
            List<Parameter> parameters = operation.getParameters();
            if (parameters == null) {
                parameters = new ArrayList<>();
                operation.setParameters(parameters);
            }

            parameters.add(new Parameter()
                    .in("header")
                    .name("X-Tenant-Id")
                    .description("租户ID，多租户系统必填")
                    .required(false)
                    .example("10001"));

            parameters.add(new Parameter()
                    .in("header")
                    .name("X-Trace-Id")
                    .description("链路追踪ID，不传则由网关或服务端自动生成")
                    .required(false)
                    .example("trace-20260506103000001"));

            parameters.add(new Parameter()
                    .in("header")
                    .name("X-Client-Version")
                    .description("客户端版本号")
                    .required(false)
                    .example("1.0.0"));

            return operation;
        };
    }

}
```

如果只希望某些分组接口携带特定请求头，可以在 `GroupedOpenApi` 中通过 `addOperationCustomizer` 配置。

文件位置：`src/main/java/io/github/atengk/config/OpenApiGroupHeaderConfig.java`

以下配置只给后台管理接口追加 `X-Admin-Client` 请求头。

```java
package io.github.atengk.config;

import io.swagger.v3.oas.models.parameters.Parameter;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 分组请求头配置
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Configuration
public class OpenApiGroupHeaderConfig {

    /**
     * 后台管理接口分组
     *
     * @return 后台管理接口分组
     */
    @Bean
    public GroupedOpenApi adminGroupedOpenApi() {
        log.info("初始化后台管理接口分组请求头配置");

        return GroupedOpenApi.builder()
                .group("admin")
                .displayName("后台管理接口")
                .pathsToMatch("/admin/**")
                .addOperationCustomizer((operation, handlerMethod) -> {
                    operation.addParametersItem(new Parameter()
                            .in("header")
                            .name("X-Admin-Client")
                            .description("后台管理客户端标识")
                            .required(false)
                            .example("admin-web"));
                    return operation;
                })
                .build();
    }

}
```

全局请求头配置完成后，启动项目并访问：

```bash
mvn spring-boot:run
```

打开 Swagger UI 页面：

```text
http://localhost:8080/swagger-ui.html
```

验证以下内容：

| 检查项             | 预期结果                              |
| ------------------ | ------------------------------------- |
| `Authorize` 按钮   | 页面展示 JWT 认证入口                 |
| `Authorization`    | 接口调试时自动携带 Bearer Token       |
| `X-Tenant-Id`      | 接口参数区域展示租户请求头            |
| `X-Trace-Id`       | 接口参数区域展示链路追踪请求头        |
| `X-Client-Version` | 接口参数区域展示客户端版本请求头      |
| Knife4j 页面       | `/doc.html` 可访问或按环境关闭        |
| Swagger UI 页面    | `/swagger-ui.html` 可访问或按环境关闭 |

认证与安全配置的推荐原则是：认证类请求头使用 `SecurityScheme`，业务类请求头使用 `OperationCustomizer`；开发环境可以开放 Swagger UI 和 Knife4j，测试环境建议限制访问，生产环境默认关闭或放到统一认证和内网访问控制之后。

## 统一响应与异常文档

本节用于统一接口返回结构、错误码说明和异常响应格式。接口文档不仅要展示成功响应，也应清晰展示业务失败、参数校验失败、认证失败、权限不足、资源不存在和系统异常等情况。OpenAPI 规范中的 `responses` 用于描述接口可能返回的 HTTP 状态码、说明和响应体结构，适合与统一异常处理配合使用。([Home](https://docs.spring.io/spring-boot/docs/2.4.7/reference/html/spring-boot-features.html?utm_source=chatgpt.com))

### 统一返回结构定义

统一返回结构用于保证所有接口响应格式一致，便于前端、移动端和第三方调用方统一处理。推荐接口统一返回 `code`、`message`、`data`、`timestamp`、`traceId` 等字段，其中 `code` 表示业务状态码，HTTP 状态码仍由接口响应状态控制。

文件位置：`src/main/java/io/github/atengk/common/model/Result.java`

以下代码定义统一响应结构，支持成功、失败和携带链路追踪 ID 的响应结果。

```java
package io.github.atengk.common.model;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 统一响应结果
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@Schema(name = "Result", description = "统一响应结果")
public class Result<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "业务状态码，200 表示成功", example = "200")
    private Integer code;

    @Schema(description = "响应消息", example = "操作成功")
    private String message;

    @Schema(description = "响应数据")
    private T data;

    @Schema(description = "链路追踪ID", example = "trace-20260506103000001")
    private String traceId;

    @Schema(description = "响应时间", example = "2026-05-06 10:30:00")
    private String timestamp;

    /**
     * 返回成功结果
     *
     * @param data 响应数据
     * @param <T>  数据类型
     * @return 统一响应结果
     */
    public static <T> Result<T> success(T data) {
        return build(200, "操作成功", data, null);
    }

    /**
     * 返回失败结果
     *
     * @param code    业务状态码
     * @param message 错误消息
     * @param <T>     数据类型
     * @return 统一响应结果
     */
    public static <T> Result<T> fail(Integer code, String message) {
        return build(code, message, null, null);
    }

    /**
     * 返回失败结果
     *
     * @param code    业务状态码
     * @param message 错误消息
     * @param traceId 链路追踪ID
     * @param <T>     数据类型
     * @return 统一响应结果
     */
    public static <T> Result<T> fail(Integer code, String message, String traceId) {
        return build(code, message, null, traceId);
    }

    /**
     * 构建统一响应结果
     *
     * @param code    业务状态码
     * @param message 响应消息
     * @param data    响应数据
     * @param traceId 链路追踪ID
     * @param <T>     数据类型
     * @return 统一响应结果
     */
    private static <T> Result<T> build(Integer code, String message, T data, String traceId) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(StrUtil.blankToDefault(message, "操作完成"));
        result.setData(data);
        result.setTraceId(traceId);
        result.setTimestamp(DateUtil.now());
        return result;
    }

}
```

统一响应结构建议遵循以下约定：

| 字段        | 类型      | 说明                                       |
| ----------- | --------- | ------------------------------------------ |
| `code`      | `Integer` | 业务状态码，例如 `200`、`400001`、`500000` |
| `message`   | `String`  | 响应说明，成功或失败原因                   |
| `data`      | `T`       | 业务数据，失败时通常为 `null`              |
| `traceId`   | `String`  | 链路追踪 ID，用于排查线上问题              |
| `timestamp` | `String`  | 响应时间                                   |

接口示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 1,
    "username": "ateng"
  },
  "traceId": "trace-20260506103000001",
  "timestamp": "2026-05-06 10:30:00"
}
```

### 错误码文档说明

错误码文档用于统一说明系统中所有业务失败场景。建议错误码按模块或类型分段，例如通用错误码、认证错误码、权限错误码、用户模块错误码、订单模块错误码等。

文件位置：`src/main/java/io/github/atengk/common/enums/ErrorCode.java`

以下代码定义系统通用错误码枚举。

```java
package io.github.atengk.common.enums;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 系统错误码枚举
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Getter
@AllArgsConstructor
@Schema(name = "ErrorCode", description = "系统错误码枚举")
public enum ErrorCode {

    SUCCESS(200, "操作成功"),

    PARAM_ERROR(400001, "请求参数错误"),

    PARAM_BIND_ERROR(400002, "参数绑定失败"),

    REQUEST_BODY_ERROR(400003, "请求体格式错误"),

    UNAUTHORIZED(401000, "未认证或登录已过期"),

    FORBIDDEN(403000, "无访问权限"),

    NOT_FOUND(404000, "资源不存在"),

    METHOD_NOT_ALLOWED(405000, "请求方法不支持"),

    BUSINESS_ERROR(500001, "业务处理失败"),

    SYSTEM_ERROR(500000, "系统异常，请稍后重试");

    @Schema(description = "错误码", example = "400001")
    private final Integer code;

    @Schema(description = "错误信息", example = "请求参数错误")
    private final String message;

}
```

错误码建议在接口文档中以表格方式维护，便于前后端统一处理。

| 错误码   | HTTP 状态码 | 错误类型     | 说明                                               |
| -------- | ----------- | ------------ | -------------------------------------------------- |
| `200`    | `200`       | 成功         | 请求处理成功                                       |
| `400001` | `400`       | 参数错误     | 查询参数、路径参数或请求头参数不合法               |
| `400002` | `400`       | 参数绑定失败 | 参数类型转换失败或对象绑定失败                     |
| `400003` | `400`       | 请求体错误   | JSON 格式错误、字段类型错误或请求体缺失            |
| `401000` | `401`       | 未认证       | Token 缺失、无效或已过期                           |
| `403000` | `403`       | 无权限       | 当前用户无接口访问权限                             |
| `404000` | `404`       | 资源不存在   | 数据不存在或接口路径不存在                         |
| `405000` | `405`       | 方法不支持   | 请求方法不匹配，例如接口只支持 `POST` 却使用 `GET` |
| `500001` | `500`       | 业务异常     | 业务规则校验失败                                   |
| `500000` | `500`       | 系统异常     | 未预期的系统错误                                   |

文件位置：`src/main/java/io/github/atengk/common/exception/BusinessException.java`

以下代码定义业务异常类，用于主动抛出业务错误。

```java
package io.github.atengk.common.exception;

import io.github.atengk.common.enums.ErrorCode;
import lombok.Getter;

/**
 * 业务异常
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Getter
public class BusinessException extends RuntimeException {

    private final Integer code;

    /**
     * 创建业务异常
     *
     * @param errorCode 错误码枚举
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    /**
     * 创建业务异常
     *
     * @param code    错误码
     * @param message 错误信息
     */
    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }

}
```

业务代码中可以这样使用：

```java
throw new BusinessException(ErrorCode.BUSINESS_ERROR);
```

或者：

```java
throw new BusinessException(500101, "用户状态异常，无法执行该操作");
```

### 全局异常响应说明

全局异常响应用于把系统中的异常转换为统一返回结构。建议通过 `@RestControllerAdvice` 统一处理参数校验异常、请求体解析异常、业务异常和系统异常，避免异常响应格式不一致。

文件位置：`src/main/java/io/github/atengk/common/handler/GlobalExceptionHandler.java`

以下代码定义全局异常处理器，并统一返回 `Result` 结构。

```java
package io.github.atengk.common.handler;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.common.enums.ErrorCode;
import io.github.atengk.common.exception.BusinessException;
import io.github.atengk.common.model.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.List;
import java.util.Set;
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
     * 处理业务异常
     *
     * @param exception 业务异常
     * @param request   HTTP 请求
     * @return 统一响应结果
     */
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException exception, HttpServletRequest request) {
        String traceId = getTraceId(request);
        log.warn("业务异常，请求路径：{}，错误码：{}，错误信息：{}，traceId：{}",
                request.getRequestURI(), exception.getCode(), exception.getMessage(), traceId);
        return Result.fail(exception.getCode(), exception.getMessage(), traceId);
    }

    /**
     * 处理请求体参数校验异常
     *
     * @param exception 参数校验异常
     * @param request   HTTP 请求
     * @return 统一响应结果
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException exception,
                                                              HttpServletRequest request) {
        String traceId = getTraceId(request);
        List<FieldError> fieldErrors = exception.getBindingResult().getFieldErrors();
        String message = CollUtil.emptyIfNull(fieldErrors)
                .stream()
                .map(error -> StrUtil.format("{}：{}", error.getField(), error.getDefaultMessage()))
                .collect(Collectors.joining("；"));

        log.warn("请求体参数校验失败，请求路径：{}，错误信息：{}，traceId：{}",
                request.getRequestURI(), message, traceId);

        return Result.fail(ErrorCode.PARAM_ERROR.getCode(),
                StrUtil.blankToDefault(message, ErrorCode.PARAM_ERROR.getMessage()), traceId);
    }

    /**
     * 处理对象绑定异常
     *
     * @param exception 绑定异常
     * @param request   HTTP 请求
     * @return 统一响应结果
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException exception, HttpServletRequest request) {
        String traceId = getTraceId(request);
        String message = exception.getFieldErrors()
                .stream()
                .map(error -> StrUtil.format("{}：{}", error.getField(), error.getDefaultMessage()))
                .collect(Collectors.joining("；"));

        log.warn("参数绑定失败，请求路径：{}，错误信息：{}，traceId：{}",
                request.getRequestURI(), message, traceId);

        return Result.fail(ErrorCode.PARAM_BIND_ERROR.getCode(),
                StrUtil.blankToDefault(message, ErrorCode.PARAM_BIND_ERROR.getMessage()), traceId);
    }

    /**
     * 处理路径参数或查询参数校验异常
     *
     * @param exception 参数校验异常
     * @param request   HTTP 请求
     * @return 统一响应结果
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraintViolationException(ConstraintViolationException exception,
                                                           HttpServletRequest request) {
        String traceId = getTraceId(request);
        Set<ConstraintViolation<?>> violations = exception.getConstraintViolations();
        String message = violations.stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("；"));

        log.warn("请求参数校验失败，请求路径：{}，错误信息：{}，traceId：{}",
                request.getRequestURI(), message, traceId);

        return Result.fail(ErrorCode.PARAM_ERROR.getCode(),
                StrUtil.blankToDefault(message, ErrorCode.PARAM_ERROR.getMessage()), traceId);
    }

    /**
     * 处理请求体解析异常
     *
     * @param exception 请求体解析异常
     * @param request   HTTP 请求
     * @return 统一响应结果
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleHttpMessageNotReadableException(HttpMessageNotReadableException exception,
                                                              HttpServletRequest request) {
        String traceId = getTraceId(request);
        log.warn("请求体格式错误，请求路径：{}，错误信息：{}，traceId：{}",
                request.getRequestURI(), exception.getMessage(), traceId);

        return Result.fail(ErrorCode.REQUEST_BODY_ERROR.getCode(), ErrorCode.REQUEST_BODY_ERROR.getMessage(), traceId);
    }

    /**
     * 处理请求方法不支持异常
     *
     * @param exception 请求方法不支持异常
     * @param request   HTTP 请求
     * @return 统一响应结果
     */
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Result<Void> handleMethodNotSupportedException(HttpRequestMethodNotSupportedException exception,
                                                          HttpServletRequest request) {
        String traceId = getTraceId(request);
        log.warn("请求方法不支持，请求路径：{}，当前方法：{}，支持方法：{}，traceId：{}",
                request.getRequestURI(), exception.getMethod(), exception.getSupportedHttpMethods(), traceId);

        return Result.fail(ErrorCode.METHOD_NOT_ALLOWED.getCode(), ErrorCode.METHOD_NOT_ALLOWED.getMessage(), traceId);
    }

    /**
     * 处理未知异常
     *
     * @param exception 未知异常
     * @param request   HTTP 请求
     * @return 统一响应结果
     */
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception exception, HttpServletRequest request) {
        String traceId = getTraceId(request);
        log.error("系统异常，请求路径：{}，traceId：{}", request.getRequestURI(), traceId, exception);
        return Result.fail(ErrorCode.SYSTEM_ERROR.getCode(), ErrorCode.SYSTEM_ERROR.getMessage(), traceId);
    }

    /**
     * 获取链路追踪ID
     *
     * @param request HTTP 请求
     * @return 链路追踪ID
     */
    private String getTraceId(HttpServletRequest request) {
        return StrUtil.blankToDefault(request.getHeader("X-Trace-Id"), request.getRequestId());
    }

}
```

全局异常响应示例：

```json
{
  "code": 400001,
  "message": "username：用户名不能为空；mobile：手机号格式不正确",
  "data": null,
  "traceId": "trace-20260506103000001",
  "timestamp": "2026-05-06 10:30:00"
}
```

建议在接口文档中统一说明异常响应结构，而不是在每个接口中重复维护完整错误示例。对外开放接口、支付接口、订单接口等关键接口可以额外补充 `@ApiResponses`，明确常见错误码。

## 多环境使用

本节用于说明 `springdoc-openapi` 在开发、测试和生产环境中的启用策略。Spring Boot 支持通过 Profile 区分不同环境配置，`application-{profile}.yml` 会作为特定 Profile 的配置文件加载，并可覆盖通用配置；也可以通过命令行参数或环境变量激活指定 Profile。([Spring Enterprise 文档](https://docs.enterprise.spring.io/spring-boot/reference/features/external-config.html?utm_source=chatgpt.com))

### 开发环境开放文档

开发环境需要开放 Swagger UI 和 OpenAPI JSON，便于后端自测、前端联调和接口参数确认。`springdoc.swagger-ui.enabled` 可控制 Swagger UI 是否启用，`springdoc.api-docs.enabled` 可控制 OpenAPI 文档接口是否启用；Swagger UI 默认路径为 `/swagger-ui.html`，OpenAPI JSON 默认路径为 `/v3/api-docs`。([OpenAPI 3 Library for spring-boot](https://springdoc.org/properties?utm_source=chatgpt.com))

文件位置：`src/main/resources/application-dev.yml`

```yaml
server:
  port: 8080

springdoc:
  api-docs:
    # 开发环境开放 OpenAPI JSON 文档
    enabled: true
    # OpenAPI JSON 默认路径，可按项目规范调整
    path: /v3/api-docs
  swagger-ui:
    # 开发环境开放 Swagger UI 页面
    enabled: true
    # Swagger UI 默认路径
    path: /swagger-ui.html
    # 默认展开接口调试按钮
    try-it-out-enabled: true
    # 开启接口过滤框，便于按关键字查找接口
    filter: true
    # 标签按名称排序
    tags-sorter: alpha
    # 接口按名称排序
    operations-sorter: alpha
```

启动开发环境：

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

或使用 Jar 启动：

```bash
java -jar target/demo-api.jar --spring.profiles.active=dev
```

验证地址：

```text
http://localhost:8080/swagger-ui.html
http://localhost:8080/v3/api-docs
http://localhost:8080/v3/api-docs.yaml
```

### 测试环境开放文档

测试环境通常也需要开放接口文档，但建议增加访问控制。测试环境面向测试人员、前端人员和联调人员，接口文档可以保留，但不建议完全匿名暴露到公网。

文件位置：`src/main/resources/application-test.yml`

```yaml
server:
  port: 8080

springdoc:
  api-docs:
    # 测试环境开放 OpenAPI JSON 文档，便于自动化测试或接口平台导入
    enabled: true
    path: /v3/api-docs
  swagger-ui:
    # 测试环境开放 Swagger UI，但建议配合认证或内网访问
    enabled: true
    path: /swagger-ui.html
    try-it-out-enabled: true
    filter: true
    tags-sorter: alpha
    operations-sorter: alpha
```

如果项目使用 Spring Security，测试环境建议仅允许登录用户访问文档页面。

文件位置：`src/main/java/io/github/atengk/config/DocSecurityConfig.java`

以下配置仅在 `test` 环境生效，用于保护接口文档路径。

```java
package io.github.atengk.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * 测试环境接口文档安全配置
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Configuration
@Profile("test")
public class DocSecurityConfig {

    /**
     * 记录测试环境文档安全配置加载状态
     */
    public DocSecurityConfig() {
        log.info("测试环境已启用接口文档访问控制，请配合 Spring Security 保护文档路径");
    }

}
```

Spring Security 中需要保护的路径通常包括：

```text
/swagger-ui.html
/swagger-ui/**
/v3/api-docs
/v3/api-docs/**
/v3/api-docs.yaml
/doc.html
/webjars/**
```

其中 `/doc.html` 和 `/webjars/**` 主要用于 Knife4j 页面资源；如果项目没有使用 Knife4j，可以不配置这两个路径。

### 生产环境关闭文档

生产环境建议默认关闭 Swagger UI 和 OpenAPI JSON 文档，避免接口结构、参数、路径、模型字段和调试入口暴露。springdoc 官方属性支持通过 `springdoc.swagger-ui.enabled=false` 关闭 Swagger UI，通过 `springdoc.api-docs.enabled=false` 关闭 OpenAPI 文档接口。([OpenAPI 3 Library for spring-boot](https://springdoc.org/properties?utm_source=chatgpt.com))

文件位置：`src/main/resources/application-prod.yml`

```yaml
server:
  port: 8080

springdoc:
  api-docs:
    # 生产环境关闭 OpenAPI JSON 文档
    enabled: false
  swagger-ui:
    # 生产环境关闭 Swagger UI 页面
    enabled: false
```

生产环境启动命令：

```bash
java -jar target/demo-api.jar --spring.profiles.active=prod
```

也可以通过环境变量指定：

```bash
export SPRING_PROFILES_ACTIVE=prod
java -jar target/demo-api.jar
```

生产环境验证方式：

```bash
curl -i http://localhost:8080/v3/api-docs
curl -i http://localhost:8080/swagger-ui.html
```

预期结果通常为 `404`、`401`、`403` 或无法访问，具体取决于项目是否仍由网关或 Spring Security 处理这些路径。

如果生产环境必须保留文档，建议满足以下条件：

| 控制项   | 建议                                 |
| -------- | ------------------------------------ |
| 网络范围 | 仅内网、VPN 或堡垒机可访问           |
| 身份认证 | 必须接入统一登录或 Spring Security   |
| 访问权限 | 仅开发、测试、运维和接口使用方可访问 |
| 调试能力 | 谨慎开启 `try-it-out-enabled`        |
| 日志审计 | 记录访问人、时间、IP 和路径          |
| 网关控制 | 在网关层配置白名单或认证策略         |

## 接口验证

本节用于说明如何验证 Swagger UI、OpenAPI JSON 和接口注解是否生效。完成依赖、配置、注解、认证和多环境配置后，应通过页面访问、命令行请求和 JSON 结构检查三种方式确认接口文档可用。

### Swagger UI 调试接口

Swagger UI 用于在线查看接口文档和调试接口。引入 `springdoc-openapi-starter-webmvc-ui` 后，Swagger UI 默认地址为 `/swagger-ui.html`，OpenAPI JSON 默认地址为 `/v3/api-docs`；如果配置了 `server.servlet.context-path`，访问路径需要加上上下文路径。([GitHub](https://github.com/springdoc/springdoc-openapi?utm_source=chatgpt.com))

启动项目：

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

访问 Swagger UI：

```text
http://localhost:8080/swagger-ui.html
```

如果项目配置了上下文路径：

```yaml
server:
  servlet:
    # 应用上下文路径
    context-path: /api
```

则访问地址为：

```text
http://localhost:8080/api/swagger-ui.html
```

调试接口时建议按以下步骤操作：

| 步骤 | 操作                                  | 预期结果                                   |
| ---- | ------------------------------------- | ------------------------------------------ |
| 1    | 打开 Swagger UI 页面                  | 页面正常加载接口分组和接口列表             |
| 2    | 点击接口模块                          | 展示接口路径、方法、参数和响应             |
| 3    | 点击 `Try it out`                     | 参数输入区域变为可编辑                     |
| 4    | 填写路径参数、查询参数、请求体        | 参数格式与 DTO 注解一致                    |
| 5    | 如需认证，点击 `Authorize` 输入 Token | 请求自动携带 `Authorization`               |
| 6    | 点击 `Execute`                        | 页面显示请求 URL、curl、响应状态码和响应体 |

JWT 调试示例：

```text
Bearer eyJhbGciOiJIUzI1NiJ9.xxxxx.yyyyy
```

接口请求体示例：

```json
{
  "username": "ateng",
  "nickname": "阿腾",
  "mobile": "13800000000",
  "remark": "测试用户"
}
```

正常响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 1,
    "username": "ateng",
    "nickname": "阿腾",
    "mobile": "13800000000",
    "enabled": true,
    "createTime": "2026-05-06T10:30:00"
  },
  "traceId": "trace-20260506103000001",
  "timestamp": "2026-05-06 10:30:00"
}
```

### OpenAPI JSON 文档验证

OpenAPI JSON 文档验证用于确认接口文档是否真实生成，以及接口路径、分组、模型、认证配置是否输出到规范文档中。默认 JSON 地址为 `/v3/api-docs`，YAML 地址为 `/v3/api-docs.yaml`。([GitHub](https://github.com/springdoc/springdoc-openapi?utm_source=chatgpt.com))

使用 `curl` 验证：

```bash
curl -s http://localhost:8080/v3/api-docs
```

格式化输出需要本地安装 `jq`：

```bash
curl -s http://localhost:8080/v3/api-docs | jq .
```

检查文档基础信息：

```bash
curl -s http://localhost:8080/v3/api-docs | jq '.info'
```

预期结果：

```json
{
  "title": "Spring Boot 3 接口文档",
  "description": "基于 springdoc-openapi 生成的 REST API 在线文档",
  "version": "v1.0.0"
}
```

检查接口路径是否生成：

```bash
curl -s http://localhost:8080/v3/api-docs | jq '.paths | keys'
```

预期结果中应包含项目接口路径：

```json
[
  "/user/list",
  "/user/{id}",
  "/user/body"
]
```

检查 JWT 认证配置是否生成：

```bash
curl -s http://localhost:8080/v3/api-docs | jq '.components.securitySchemes'
```

预期结果：

```json
{
  "BearerAuth": {
    "type": "http",
    "description": "请输入 JWT Token，格式：Bearer {token}",
    "scheme": "bearer",
    "bearerFormat": "JWT"
  }
}
```

检查分组文档：

```bash
curl -s http://localhost:8080/v3/api-docs/admin | jq '.info'
curl -s http://localhost:8080/v3/api-docs/app | jq '.info'
curl -s http://localhost:8080/v3/api-docs/open | jq '.info'
```

如果需要把 OpenAPI JSON 导入接口平台，可以先导出文件：

```bash
curl -s http://localhost:8080/v3/api-docs -o openapi.json
```

导出 YAML：

```bash
curl -s http://localhost:8080/v3/api-docs.yaml -o openapi.yaml
```

### 常见问题排查

常见问题排查用于快速定位 Swagger UI 无法访问、接口未展示、参数说明缺失、认证不生效和生产环境误暴露等问题。排查时建议先确认依赖版本，再确认访问路径，然后检查 Spring Security、Profile 配置和 Controller 扫描范围。

| 问题                   | 常见原因                                                     | 处理方式                                                     |
| ---------------------- | ------------------------------------------------------------ | ------------------------------------------------------------ |
| `/swagger-ui.html` 404 | 未引入 `springdoc-openapi-starter-webmvc-ui`，或生产环境关闭了 Swagger UI | 检查 Maven 依赖和 `springdoc.swagger-ui.enabled`             |
| `/v3/api-docs` 404     | `springdoc.api-docs.enabled=false`，或访问路径被修改         | 检查 `springdoc.api-docs.path`                               |
| Swagger UI 空白        | 静态资源被 Security 拦截                                     | 放行 `/swagger-ui/**`、`/swagger-ui.html`、`/v3/api-docs/**` |
| 接口没有展示           | Controller 不在扫描包内，或路径不匹配分组规则                | 检查 `packages-to-scan`、`paths-to-match`                    |
| DTO 字段说明缺失       | DTO 字段未加 `@Schema`，或泛型响应结构过深                   | 为 DTO、VO 和字段补充 `@Schema`                              |
| 请求体无法显示         | 方法参数未使用 Spring MVC `@RequestBody`                     | 检查导入是否为 `org.springframework.web.bind.annotation.RequestBody` |
| 参数校验不生效         | 未引入 `spring-boot-starter-validation` 或未加 `@Valid`      | 增加依赖并在请求体参数上使用 `@Valid`                        |
| JWT 没有自动携带       | 未配置 `SecurityScheme`，或名称与 `@SecurityRequirement` 不一致 | 统一使用 `BearerAuth` 名称                                   |
| Knife4j 页面 404       | 未引入 Knife4j starter，或路径被 Security 拦截               | 检查 `/doc.html` 和 `/webjars/**`                            |
| 生产环境仍能访问文档   | `prod` Profile 未生效，或配置被外部配置覆盖                  | 检查 `SPRING_PROFILES_ACTIVE` 和启动参数                     |

版本和依赖排查命令：

```bash
mvn dependency:tree | grep springdoc
```

检查当前 Profile：

```bash
curl -s http://localhost:8080/actuator/env/spring.profiles.active
```

如果未启用 Actuator，可以从启动日志中确认：

```text
The following 1 profile is active: "dev"
```

检查接口文档是否被 Spring Security 拦截：

```bash
curl -i http://localhost:8080/v3/api-docs
```

常见状态码说明：

| 状态码 | 说明                 | 处理方式                   |
| ------ | -------------------- | -------------------------- |
| `200`  | 文档正常返回         | 配置正常                   |
| `401`  | 未登录               | 登录后访问，或放行文档路径 |
| `403`  | 无权限               | 检查权限规则               |
| `404`  | 路径不存在或文档关闭 | 检查路径和 `enabled` 配置  |
| `500`  | 服务端异常           | 查看应用日志和依赖冲突     |

排查建议按以下顺序执行：

1. 确认 Spring Boot 3 使用的是 `springdoc-openapi-starter-webmvc-ui` 或 `springdoc-openapi-starter-webflux-ui`。
2. 确认当前环境的 `springdoc.api-docs.enabled` 和 `springdoc.swagger-ui.enabled` 是否为 `true`。
3. 确认访问路径是否包含 `server.servlet.context-path`。
4. 确认 Spring Security 是否放行或正确保护文档路径。
5. 确认 Controller 是否被 Spring Boot 扫描到。
6. 确认 `packages-to-scan` 和 `paths-to-match` 是否限制过严。
7. 查看启动日志中是否存在 springdoc、Swagger UI、Knife4j 或依赖冲突错误。

完成以上验证后，接口文档应同时满足以下结果：开发和测试环境可以访问 Swagger UI，生产环境默认关闭文档入口；OpenAPI JSON 能正确输出接口路径、请求参数、请求体、响应模型和认证配置；接口异常响应保持统一结构，错误码在文档中可被调用方明确识别。
