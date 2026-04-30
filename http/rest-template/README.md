# RestTemplate 开发使用文档

本文档用于说明在 JDK 21 与 Spring Boot 3 项目中如何使用 `RestTemplate` 调用外部 HTTP 接口，内容覆盖技术背景、环境准备、依赖配置、基础调用、参数处理、响应处理、异常处理和后续封装建议。

## 技术背景

这一部分用于说明 `RestTemplate` 在 Spring 体系中的定位，以及在 Spring Boot 3 项目中继续使用它时需要注意的边界。

### RestTemplate 的定位

`RestTemplate` 是 Spring Framework 提供的同步 HTTP 客户端工具，采用经典的 Template Method API 风格，对底层 HTTP 客户端进行封装，可基于 JDK `HttpURLConnection`、Apache HttpComponents 等不同实现发起远程 HTTP 请求。它适合在传统 Spring MVC、后台任务、同步业务流程、第三方接口集成等场景中使用。

在日常开发中，`RestTemplate` 常用于下面这些场景：

| 场景             | 说明                                            |
| ---------------- | ----------------------------------------------- |
| 调用第三方接口   | 例如短信、支付、地图、物流、认证平台等 HTTP API |
| 服务间同步调用   | 适合简单、低并发、无响应式要求的内部服务调用    |
| 后台任务调用接口 | 例如定时同步外部系统数据                        |
| 遗留项目维护     | 适合已有大量 `RestTemplate` 代码的项目继续维护  |

`RestTemplate` 的核心特点是“同步阻塞调用”。调用线程会等待远程接口返回结果后再继续执行，因此实现简单、调试直接，但不适合大量长连接、流式处理或高并发响应式调用场景。

### Spring Boot 3 中的使用现状

在 Spring Boot 3 中，`RestTemplate` 仍然可以正常使用。Spring Boot 官方文档说明，若偏好阻塞式 API，可以使用 `RestClient` 或 `RestTemplate` 调用远程 REST 服务；如果是非阻塞响应式应用，则推荐使用 `WebClient`。

需要注意的是，Spring Boot 不会默认提供一个全局的 `RestTemplate` Bean，因为不同业务场景通常需要不同的超时时间、拦截器、错误处理器、消息转换器或底层 HTTP 客户端。Spring Boot 会自动配置 `RestTemplateBuilder`，开发者可以通过它创建符合项目要求的 `RestTemplate` 实例。

推荐在 Spring Boot 3 项目中采用下面的使用方式：

1. 不直接 `new RestTemplate()` 作为业务调用入口。
2. 优先通过 `RestTemplateBuilder` 构建实例。
3. 在配置类中统一定义 `RestTemplate` Bean。
4. 为生产环境配置连接超时、读取超时、请求日志、异常处理和连接池。
5. 新项目如无历史包袱，可优先评估 `RestClient`。

### 与 WebClient、RestClient 的关系

Spring Framework 当前提供了多种 REST 客户端选择，包括 `RestClient`、`WebClient`、`RestTemplate` 和基于接口代理的 HTTP Interface。`RestClient` 是同步阻塞客户端，API 风格更现代；`WebClient` 是非阻塞响应式客户端；`RestTemplate` 是经典同步模板式客户端。

三者的主要区别如下：

| 客户端       | 调用模型     | API 风格                      | 适用场景                                 |
| ------------ | ------------ | ----------------------------- | ---------------------------------------- |
| RestTemplate | 同步阻塞     | Template Method               | 存量项目、简单同步 HTTP 调用             |
| RestClient   | 同步阻塞     | Fluent API                    | Spring Framework 6.1+ 新同步调用推荐选择 |
| WebClient    | 非阻塞响应式 | Fluent API + Reactive Streams | 高并发、流式处理、响应式应用             |

`RestClient` 与 `RestTemplate` 共享底层基础设施，例如请求工厂、请求拦截器、初始化器和消息转换器，但 Spring Framework 后续新的高级特性会优先集中在 `RestClient` 上。

因此，本项目如果是存量系统、同步调用为主、团队已经熟悉 `RestTemplate`，可以继续使用 `RestTemplate`；如果是新建 Spring Boot 3.2+ 或更高版本项目，并且没有历史兼容要求，建议优先考虑 `RestClient`；如果项目是 Spring WebFlux 或需要非阻塞调用，则使用 `WebClient`。

## 环境准备

这一部分用于说明开发 `RestTemplate` 示例所需的 JDK、Spring Boot 版本和 Maven 依赖配置。后续章节默认基于这里的环境和依赖展开。

### JDK 21 环境要求

本文示例使用 JDK 21。Spring Boot 3.5.14 要求至少 Java 17，并兼容到 Java 25，因此 JDK 21 满足 Spring Boot 3.5.x 的运行要求。

开发环境建议如下：

| 项目        | 建议版本                                 |
| ----------- | ---------------------------------------- |
| JDK         | 21                                       |
| Maven       | 3.9.x                                    |
| Spring Boot | 3.5.x，或团队统一的 Spring Boot 3.x 版本 |
| 编码        | UTF-8                                    |
| 构建工具    | Maven                                    |

可通过下面命令检查本地 Java 与 Maven 环境。

```bash
# 查看 Java 版本，确认当前使用的是 JDK 21
java -version

# 查看 Maven 版本，同时确认 Maven 使用的 Java 版本
mvn -v
```

正常情况下，`java -version` 应能看到 `21` 相关版本号，`mvn -v` 中的 Java 路径应指向 JDK 21 的安装目录。如果 Maven 使用的不是 JDK 21，需要检查 `JAVA_HOME` 和 `PATH` 配置。

Linux 或 macOS 可参考下面配置：

```bash
# 配置 JDK 21 安装目录，按实际安装路径调整
export JAVA_HOME=/opt/jdk-21

# 将 JDK 命令加入 PATH
export PATH=$JAVA_HOME/bin:$PATH
```

Windows 环境需要在系统环境变量中配置：

```text
JAVA_HOME=C:\Program Files\Java\jdk-21

Path=%JAVA_HOME%\bin
```

### Spring Boot 3 版本说明

本文档以 Spring Boot 3.5.x 为主要示例版本。Spring Boot 3.5.14 基于 Java 17+，需要 Spring Framework 6.2.18 或更高版本，并明确支持 Maven 3.6.3 或更高版本。([Home](https://docs.spring.io/spring-boot/3.5/system-requirements.html))

如果项目已经锁定 Spring Boot 3.3.x 或 3.4.x，也可以继续使用本文中的 `RestTemplate` 写法。需要注意不同 Spring Boot 3 小版本之间的差异：

| Spring Boot 版本 | Spring Framework 主版本 | 说明                                            |
| ---------------- | ----------------------- | ----------------------------------------------- |
| 3.3.x            | 6.1.x                   | 适合稳定维护项目                                |
| 3.4.x            | 6.2.x                   | 适合较新的 Spring Boot 3 项目                   |
| 3.5.x            | 6.2.x                   | 当前 Spring Boot 3 新版本线，适合作为新项目基线 |

在团队项目中，不建议单个模块私自升级 Spring Boot 版本。应以父工程、BOM 或公司基础框架版本为准，避免依赖冲突。

### Maven 依赖配置

这一部分给出 `RestTemplate` 开发所需的基础 Maven 依赖。对于普通 Spring MVC 项目，引入 `spring-boot-starter-web` 后即可使用 `RestTemplate` 相关类；如果后续需要连接池、代理、细粒度超时控制，可额外引入 Apache HttpClient 5。

文件位置：`pom.xml`

下面配置适合 Spring Boot 3 + JDK 21 项目作为基础依赖使用。

```xml
<properties>
    <!-- Java 编译版本，本文统一使用 JDK 21 -->
    <java.version>21</java.version>

    <!-- Hutool 工具类版本，Spring Boot 不托管该版本，需要显式声明 -->
    <hutool.version>5.8.44</hutool.version>
</properties>

<dependencies>
    <!-- Spring MVC 与 Spring Web 基础依赖，包含 RestTemplate 所在的 spring-web 模块 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Apache HttpClient 5，后续用于连接池、代理、超时和更完整的 HTTP 客户端能力 -->
    <dependency>
        <groupId>org.apache.httpcomponents.client5</groupId>
        <artifactId>httpclient5</artifactId>
    </dependency>

    <!-- Hutool 工具类，后续用于字符串、集合、JSON、对象判断等辅助处理 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>${hutool.version}</version>
    </dependency>

    <!-- Lombok，简化日志对象、构造方法、Getter、Setter 等样板代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- Spring Boot 测试依赖，后续用于单元测试和 Mock 接口验证 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

如果项目不是 Web 应用，只是一个后台任务或命令行应用，也可以不引入完整的 `spring-boot-starter-web`，改为只引入 `spring-web`。但在常规 Spring Boot 后端服务中，推荐保留 `spring-boot-starter-web`，后续 Controller、JSON 序列化、接口测试和 Mock 服务都会更方便。

```xml
<dependencies>
    <!-- 非 Web 服务可仅引入 spring-web，提供 RestTemplate、HttpEntity、ResponseEntity 等能力 -->
    <dependency>
        <groupId>org.springframework</groupId>
        <artifactId>spring-web</artifactId>
    </dependency>
</dependencies>
```

依赖配置完成后，可执行下面命令验证项目是否能正常编译。

```bash
# 清理并编译项目，确认依赖可以正常解析
mvn clean compile

# 查看依赖树，排查 spring-web、httpclient5 等依赖是否已正确引入
mvn dependency:tree
```

如果 `mvn clean compile` 正常通过，说明基础环境已经准备完成。后续章节即可开始配置 `RestTemplate` Bean，并编写 GET、POST、PUT、DELETE 等请求调用示例。

以下内容延续你上传的大纲中的“基础使用”部分。

## 基础使用

这一部分用于说明 `RestTemplate` 的基础接入方式，包括 Bean 配置、GET 请求、POST 请求、PUT 请求和 DELETE 请求。后续章节中的参数处理、响应处理、异常封装和工具类封装，都基于这里的基础配置继续展开。

### RestTemplate Bean 配置

`RestTemplate` 建议通过 Spring Bean 统一管理，不建议在业务代码中频繁 `new RestTemplate()`。统一配置 Bean 的好处是可以集中管理根地址、超时时间、默认请求头、拦截器、消息转换器和异常处理器。Spring Boot 3.4+ 中 `RestTemplateBuilder` 推荐使用 `connectTimeout(Duration)` 和 `readTimeout(Duration)`，旧的 `setConnectTimeout(Duration)`、`setReadTimeout(Duration)` 已进入废弃路径。([Javadoc](https://javadoc.io/static/org.springframework.boot/spring-boot/3.4.5/org/springframework/boot/web/client/RestTemplateBuilder.html?utm_source=chatgpt.com))

本节先给出最小可用的基础配置，后续“常用配置”章节再展开连接池、拦截器、消息转换器和统一异常处理。

文件位置：

```text
src/main/resources/application.yml
src/main/java/io/github/atengk/remote/properties/UserRemoteProperties.java
src/main/java/io/github/atengk/config/RestTemplateConfig.java
```

文件位置：`src/main/resources/application.yml`

```yaml
server:
  # 当前 Spring Boot 应用端口
  port: 8080

demo:
  remote:
    user:
      # 示例远程服务地址，实际项目中替换为第三方接口或内部服务地址
      base-url: http://localhost:8081
```

文件位置：`src/main/java/io/github/atengk/remote/properties/UserRemoteProperties.java`

下面的配置类用于读取 `application.yml` 中的远程用户服务地址。

```java
package io.github.atengk.remote.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 用户远程服务配置属性
 *
 * @param baseUrl 远程服务基础地址
 * @author Ateng
 * @since 2026-04-30
 */
@ConfigurationProperties(prefix = "demo.remote.user")
public record UserRemoteProperties(String baseUrl) {
}
```

文件位置：`src/main/java/io/github/atengk/config/RestTemplateConfig.java`

下面的配置类用于创建全局 `RestTemplate` Bean，并设置基础地址、连接超时、读取超时和默认请求头。

```java
package io.github.atengk.config;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.remote.properties.UserRemoteProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * RestTemplate 配置
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Configuration
@EnableConfigurationProperties(UserRemoteProperties.class)
public class RestTemplateConfig {

    /**
     * 创建 RestTemplate Bean。
     *
     * @param builder    RestTemplate 构建器
     * @param properties 用户远程服务配置
     * @return RestTemplate 实例
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder, UserRemoteProperties properties) {
        String rootUri = StrUtil.blankToDefault(properties.baseUrl(), "http://localhost:8081");

        return builder
                // 远程服务基础地址，后续请求可以直接使用 /mock-api/users 这种相对路径
                .rootUri(rootUri)
                // 建立连接的最大等待时间
                .connectTimeout(Duration.ofSeconds(3))
                // 等待响应数据的最大时间
                .readTimeout(Duration.ofSeconds(10))
                // 默认接收 JSON 响应
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

}
```

如果当前项目仍使用 Spring Boot 3.0、3.1、3.2 或 3.3，`connectTimeout` 和 `readTimeout` 可能不可用，可改用下面写法：

```java
return builder
        .rootUri(rootUri)
        .setConnectTimeout(Duration.ofSeconds(3))
        .setReadTimeout(Duration.ofSeconds(10))
        .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        .build();
```

### GET 请求调用

GET 请求通常用于查询数据。`RestTemplate` 中常用的 GET 方法有 `getForObject` 和 `getForEntity`。

`getForObject` 适合只关心响应体的场景，`getForEntity` 适合同时关心响应状态码、响应头和响应体的场景。

本节示例假设远程服务提供下面两个接口：

| 接口                          | 方法 | 说明                   |
| ----------------------------- | ---- | ---------------------- |
| `/mock-api/users/{id}`        | GET  | 根据用户 ID 查询用户   |
| `/mock-api/users?keyword=xxx` | GET  | 根据关键词查询用户列表 |

文件位置：

```text
src/main/java/io/github/atengk/remote/dto/UserResponse.java
src/main/java/io/github/atengk/remote/client/UserApiClient.java
```

文件位置：`src/main/java/io/github/atengk/remote/dto/UserResponse.java`

下面的响应对象用于接收远程用户接口返回的数据。

```java
package io.github.atengk.remote.dto;

import java.time.LocalDateTime;

/**
 * 用户响应数据
 *
 * @param id        用户ID
 * @param username  用户名
 * @param nickname  昵称
 * @param mobile    手机号
 * @param status    状态
 * @param createdAt 创建时间
 * @author Ateng
 * @since 2026-04-30
 */
public record UserResponse(
        Long id,
        String username,
        String nickname,
        String mobile,
        Integer status,
        LocalDateTime createdAt
) {
}
```

文件位置：`src/main/java/io/github/atengk/remote/client/UserApiClient.java`

下面的客户端类封装了 GET 查询方法，业务代码不需要直接拼接远程接口地址。

```java
package io.github.atengk.remote.client;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.remote.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Optional;

/**
 * 用户远程接口客户端
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserApiClient {

    private final RestTemplate restTemplate;

    /**
     * 根据用户ID查询用户。
     *
     * @param userId 用户ID
     * @return 用户响应数据
     */
    public UserResponse getUserById(Long userId) {
        if (ObjUtil.isNull(userId)) {
            log.warn("查询远程用户失败，用户ID为空");
            return null;
        }

        log.info("开始查询远程用户，userId={}", userId);
        return restTemplate.getForObject("/mock-api/users/{id}", UserResponse.class, userId);
    }

    /**
     * 根据关键词查询用户列表。
     *
     * @param keyword 查询关键词
     * @return 用户列表
     */
    public List<UserResponse> listUsers(String keyword) {
        String url = UriComponentsBuilder.fromPath("/mock-api/users")
                .queryParamIfPresent("keyword", Optional.ofNullable(keyword).filter(StrUtil::isNotBlank))
                .build()
                .encode()
                .toUriString();

        log.info("开始查询远程用户列表，keyword={}", keyword);
        ResponseEntity<UserResponse[]> responseEntity = restTemplate.getForEntity(url, UserResponse[].class);

        UserResponse[] body = responseEntity.getBody();
        if (ArrayUtil.isEmpty(body)) {
            log.info("远程用户列表为空，keyword={}", keyword);
            return List.of();
        }

        return CollUtil.newArrayList(body);
    }

}
```

调用示例：

```java
UserResponse user = userApiClient.getUserById(1L);
List<UserResponse> users = userApiClient.listUsers("Ateng");
```

对应的远程请求效果如下：

```bash
# 根据用户ID查询
curl -X GET 'http://localhost:8081/mock-api/users/1'

# 根据关键词查询用户列表
curl -X GET 'http://localhost:8081/mock-api/users?keyword=Ateng'
```

### POST 请求调用

POST 请求通常用于创建数据、提交表单或发送复杂请求体。`RestTemplate` 中常用的 POST 方法有 `postForObject`、`postForEntity` 和 `exchange`。

`postForObject` 适合只获取响应体，`postForEntity` 适合同时获取状态码、响应头和响应体。

本节示例假设远程服务提供下面的接口：

| 接口              | 方法 | 说明     |
| ----------------- | ---- | -------- |
| `/mock-api/users` | POST | 创建用户 |

文件位置：

```text
src/main/java/io/github/atengk/remote/dto/UserCreateRequest.java
src/main/java/io/github/atengk/remote/client/UserApiClient.java
```

文件位置：`src/main/java/io/github/atengk/remote/dto/UserCreateRequest.java`

下面的请求对象用于提交创建用户所需的 JSON 请求体。

```java
package io.github.atengk.remote.dto;

/**
 * 创建用户请求
 *
 * @param username 用户名
 * @param nickname 昵称
 * @param mobile   手机号
 * @author Ateng
 * @since 2026-04-30
 */
public record UserCreateRequest(
        String username,
        String nickname,
        String mobile
) {
}
```

文件位置：`src/main/java/io/github/atengk/remote/client/UserApiClient.java`

在前面的 `UserApiClient` 中继续增加下面两个 POST 方法。

```java
/**
 * 创建用户并直接返回响应体。
 *
 * @param request 创建用户请求
 * @return 用户响应数据
 */
public UserResponse createUser(UserCreateRequest request) {
    if (ObjUtil.isNull(request) || StrUtil.isBlank(request.username())) {
        log.warn("创建远程用户失败，请求参数不完整，request={}", request);
        return null;
    }

    log.info("开始创建远程用户，username={}", request.username());
    return restTemplate.postForObject("/mock-api/users", request, UserResponse.class);
}

/**
 * 创建用户并返回完整响应实体。
 *
 * @param request 创建用户请求
 * @return 用户响应实体
 */
public ResponseEntity<UserResponse> createUserForEntity(UserCreateRequest request) {
    if (ObjUtil.isNull(request) || StrUtil.isBlank(request.username())) {
        log.warn("创建远程用户失败，请求参数不完整，request={}", request);
        return ResponseEntity.badRequest().build();
    }

    log.info("开始创建远程用户并读取响应状态，username={}", request.username());
    return restTemplate.postForEntity("/mock-api/users", request, UserResponse.class);
}
```

如果需要显式设置请求头，可以使用 `HttpEntity` 包装请求体。

```java
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

/**
 * 创建用户并显式设置请求头。
 *
 * @param request 创建用户请求
 * @return 用户响应数据
 */
public UserResponse createUserWithHeader(UserCreateRequest request) {
    if (ObjUtil.isNull(request) || StrUtil.isBlank(request.username())) {
        log.warn("创建远程用户失败，请求参数不完整，request={}", request);
        return null;
    }

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);

    HttpEntity<UserCreateRequest> httpEntity = new HttpEntity<>(request, headers);

    log.info("开始创建远程用户并携带请求头，username={}", request.username());
    return restTemplate.postForObject("/mock-api/users", httpEntity, UserResponse.class);
}
```

调用示例：

```java
UserCreateRequest request = new UserCreateRequest("ateng", "Ateng", "18800000000");

UserResponse user = userApiClient.createUser(request);
ResponseEntity<UserResponse> responseEntity = userApiClient.createUserForEntity(request);
```

对应的远程请求效果如下：

```bash
curl -X POST 'http://localhost:8081/mock-api/users' \
  -H 'Content-Type: application/json' \
  -d '{
    "username": "ateng",
    "nickname": "Ateng",
    "mobile": "18800000000"
  }'
```

### PUT 与 DELETE 请求调用

PUT 请求通常用于更新数据，DELETE 请求通常用于删除数据。`RestTemplate` 提供了 `put` 和 `delete` 方法用于直接发送请求，但这两个方法默认不返回响应体。如果需要读取响应状态码或响应体，可以使用 `exchange` 方法，后续“响应结果处理”章节会继续说明。

本节示例假设远程服务提供下面两个接口：

| 接口                   | 方法   | 说明                 |
| ---------------------- | ------ | -------------------- |
| `/mock-api/users/{id}` | PUT    | 根据用户 ID 更新用户 |
| `/mock-api/users/{id}` | DELETE | 根据用户 ID 删除用户 |

文件位置：

```text
src/main/java/io/github/atengk/remote/dto/UserUpdateRequest.java
src/main/java/io/github/atengk/remote/client/UserApiClient.java
```

文件位置：`src/main/java/io/github/atengk/remote/dto/UserUpdateRequest.java`

下面的请求对象用于提交更新用户所需的 JSON 请求体。

```java
package io.github.atengk.remote.dto;

/**
 * 更新用户请求
 *
 * @param nickname 昵称
 * @param mobile   手机号
 * @param status   状态
 * @author Ateng
 * @since 2026-04-30
 */
public record UserUpdateRequest(
        String nickname,
        String mobile,
        Integer status
) {
}
```

文件位置：`src/main/java/io/github/atengk/remote/client/UserApiClient.java`

在前面的 `UserApiClient` 中继续增加下面的 PUT 和 DELETE 方法。

```java
/**
 * 根据用户ID更新用户。
 *
 * @param userId  用户ID
 * @param request 更新用户请求
 * @return 是否已发起更新请求
 */
public Boolean updateUser(Long userId, UserUpdateRequest request) {
    if (ObjUtil.isNull(userId) || ObjUtil.isNull(request)) {
        log.warn("更新远程用户失败，请求参数不完整，userId={}, request={}", userId, request);
        return false;
    }

    log.info("开始更新远程用户，userId={}", userId);
    restTemplate.put("/mock-api/users/{id}", request, userId);
    return true;
}

/**
 * 根据用户ID删除用户。
 *
 * @param userId 用户ID
 * @return 是否已发起删除请求
 */
public Boolean deleteUser(Long userId) {
    if (ObjUtil.isNull(userId)) {
        log.warn("删除远程用户失败，用户ID为空");
        return false;
    }

    log.info("开始删除远程用户，userId={}", userId);
    restTemplate.delete("/mock-api/users/{id}", userId);
    return true;
}
```

调用示例：

```java
UserUpdateRequest updateRequest = new UserUpdateRequest("Ateng-New", "18800000001", 1);

Boolean updateResult = userApiClient.updateUser(1L, updateRequest);
Boolean deleteResult = userApiClient.deleteUser(1L);
```

对应的远程请求效果如下：

```bash
# 更新用户
curl -X PUT 'http://localhost:8081/mock-api/users/1' \
  -H 'Content-Type: application/json' \
  -d '{
    "nickname": "Ateng-New",
    "mobile": "18800000001",
    "status": 1
  }'

# 删除用户
curl -X DELETE 'http://localhost:8081/mock-api/users/1'
```

到这里，`RestTemplate` 的基础 GET、POST、PUT、DELETE 调用已经完成。实际项目中不建议把远程调用逻辑散落在 Controller 或 Service 中，推荐像本节一样封装成独立的 `XxxApiClient`，由业务 Service 调用该客户端类。

以下内容延续你上传的大纲中的“请求参数处理”部分。

## 请求参数处理

这一部分用于说明 `RestTemplate` 调用远程接口时常见的参数传递方式，包括路径参数、查询参数、请求头参数和请求体参数。实际开发中，一个接口通常会同时包含多种参数，例如路径中传递资源 ID，请求头中传递认证信息，请求体中传递 JSON 数据。

常见参数类型如下：

| 参数类型     | 位置           | 常见场景                      | RestTemplate 常用写法                     |
| ------------ | -------------- | ----------------------------- | ----------------------------------------- |
| PathVariable | URL 路径       | `/users/{id}`                 | `getForObject`、`exchange` + URI 变量     |
| Query        | URL 查询字符串 | `/users?keyword=Ateng`        | `UriComponentsBuilder`                    |
| Header       | HTTP 请求头    | `Authorization`、`X-Trace-Id` | `HttpHeaders` + `HttpEntity`              |
| Body         | HTTP 请求体    | JSON、表单数据                | `HttpEntity`、`postForEntity`、`exchange` |

### PathVariable 参数

PathVariable 参数用于把变量放在 URL 路径中，常见于根据 ID 查询、更新或删除资源的接口。例如 `/mock-api/users/{id}` 中的 `{id}` 就是路径参数。

`RestTemplate` 支持通过可变参数或 `Map` 传递路径参数。单个路径参数可以直接使用可变参数，多个路径参数建议使用 `Map`，可读性更好，也能避免参数顺序错误。

文件位置：

```text
src/main/java/io/github/atengk/remote/client/UserParamApiClient.java
```

文件位置：`src/main/java/io/github/atengk/remote/client/UserParamApiClient.java`

下面的客户端类集中演示路径参数、查询参数、请求头参数和请求体参数的处理方式。

```java
package io.github.atengk.remote.client;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.remote.dto.UserResponse;
import io.github.atengk.remote.dto.UserSearchRequest;
import io.github.atengk.remote.dto.UserUpdateRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 用户远程接口参数示例客户端
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserParamApiClient {

    private final RestTemplate restTemplate;

    /**
     * 使用单个 PathVariable 查询用户。
     *
     * @param userId 用户ID
     * @return 用户响应数据
     */
    public UserResponse getUserByPathVariable(Long userId) {
        if (ObjUtil.isNull(userId)) {
            log.warn("查询远程用户失败，用户ID为空");
            return null;
        }

        log.info("开始通过路径参数查询远程用户，userId={}", userId);
        return restTemplate.getForObject("/mock-api/users/{id}", UserResponse.class, userId);
    }

    /**
     * 使用多个 PathVariable 查询组织下的用户。
     *
     * @param orgId  组织ID
     * @param userId 用户ID
     * @return 用户响应数据
     */
    public UserResponse getOrgUserByPathVariable(Long orgId, Long userId) {
        if (ObjUtil.hasNull(orgId, userId)) {
            log.warn("查询组织用户失败，参数不完整，orgId={}, userId={}", orgId, userId);
            return null;
        }

        Map<String, Object> pathVariables = new HashMap<>();
        pathVariables.put("orgId", orgId);
        pathVariables.put("userId", userId);

        log.info("开始通过多个路径参数查询组织用户，orgId={}, userId={}", orgId, userId);
        return restTemplate.getForObject(
                "/mock-api/orgs/{orgId}/users/{userId}",
                UserResponse.class,
                pathVariables
        );
    }

    /**
     * 使用 Query 参数查询用户列表。
     *
     * @param keyword  关键词
     * @param status   状态
     * @param pageNum  页码
     * @param pageSize 每页数量
     * @return 用户列表
     */
    public List<UserResponse> listUsersByQuery(String keyword, Integer status, Integer pageNum, Integer pageSize) {
        String url = UriComponentsBuilder.fromPath("/mock-api/users")
                .queryParamIfPresent("keyword", Optional.ofNullable(keyword).filter(StrUtil::isNotBlank))
                .queryParamIfPresent("status", Optional.ofNullable(status))
                .queryParam("pageNum", ObjUtil.defaultIfNull(pageNum, 1))
                .queryParam("pageSize", ObjUtil.defaultIfNull(pageSize, 10))
                .build()
                .encode()
                .toUriString();

        log.info("开始通过查询参数查询远程用户列表，keyword={}, status={}, pageNum={}, pageSize={}",
                keyword, status, pageNum, pageSize);

        ResponseEntity<List<UserResponse>> responseEntity = restTemplate.exchange(
                url,
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<>() {
                }
        );

        List<UserResponse> body = responseEntity.getBody();
        if (CollUtil.isEmpty(body)) {
            log.info("远程用户列表为空，keyword={}, status={}", keyword, status);
            return List.of();
        }
        return body;
    }

    /**
     * 使用 Header 参数查询用户。
     *
     * @param userId 用户ID
     * @param token  访问令牌
     * @return 用户响应数据
     */
    public UserResponse getUserWithHeader(Long userId, String token) {
        if (ObjUtil.isNull(userId)) {
            log.warn("携带请求头查询远程用户失败，用户ID为空");
            return null;
        }
        if (StrUtil.isBlank(token)) {
            log.warn("携带请求头查询远程用户失败，访问令牌为空，userId={}", userId);
            return null;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        headers.set("X-Trace-Id", IdUtil.fastSimpleUUID());

        HttpEntity<Void> httpEntity = new HttpEntity<>(headers);

        log.info("开始携带请求头查询远程用户，userId={}", userId);
        ResponseEntity<UserResponse> responseEntity = restTemplate.exchange(
                "/mock-api/users/{id}",
                HttpMethod.GET,
                httpEntity,
                UserResponse.class,
                userId
        );

        return responseEntity.getBody();
    }

    /**
     * 使用 Body 参数搜索用户。
     *
     * @param request 用户搜索请求
     * @return 用户列表
     */
    public List<UserResponse> searchUsersByBody(UserSearchRequest request) {
        if (ObjUtil.isNull(request)) {
            log.warn("搜索远程用户失败，请求体为空");
            return List.of();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        headers.set("X-Trace-Id", IdUtil.fastSimpleUUID());

        HttpEntity<UserSearchRequest> httpEntity = new HttpEntity<>(request, headers);

        log.info("开始通过请求体搜索远程用户，request={}", request);
        ResponseEntity<List<UserResponse>> responseEntity = restTemplate.exchange(
                "/mock-api/users/search",
                HttpMethod.POST,
                httpEntity,
                new ParameterizedTypeReference<>() {
                }
        );

        List<UserResponse> body = responseEntity.getBody();
        return CollUtil.emptyIfNull(body);
    }

    /**
     * 同时使用 PathVariable、Header 和 Body 更新用户。
     *
     * @param userId  用户ID
     * @param token   访问令牌
     * @param request 更新用户请求
     * @return 是否更新成功
     */
    public Boolean updateUserWithPathHeaderAndBody(Long userId, String token, UserUpdateRequest request) {
        if (ObjUtil.hasNull(userId, request)) {
            log.warn("更新远程用户失败，参数不完整，userId={}, request={}", userId, request);
            return false;
        }
        if (StrUtil.isBlank(token)) {
            log.warn("更新远程用户失败，访问令牌为空，userId={}", userId);
            return false;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        headers.set("X-Trace-Id", IdUtil.fastSimpleUUID());

        HttpEntity<UserUpdateRequest> httpEntity = new HttpEntity<>(request, headers);

        log.info("开始更新远程用户，userId={}", userId);
        ResponseEntity<Void> responseEntity = restTemplate.exchange(
                "/mock-api/users/{id}",
                HttpMethod.PUT,
                httpEntity,
                Void.class,
                userId
        );

        return responseEntity.getStatusCode().is2xxSuccessful();
    }

}
```

单个路径参数调用时，路径变量按照方法参数顺序替换：

```java
UserResponse user = userParamApiClient.getUserByPathVariable(1L);
```

对应 HTTP 请求如下：

```bash
curl -X GET 'http://localhost:8081/mock-api/users/1'
```

多个路径参数调用时，推荐使用 `Map` 指定变量名称：

```java
UserResponse user = userParamApiClient.getOrgUserByPathVariable(100L, 1L);
```

对应 HTTP 请求如下：

```bash
curl -X GET 'http://localhost:8081/mock-api/orgs/100/users/1'
```

使用 PathVariable 时需要注意，路径变量名称要和 URL 模板中的 `{变量名}` 保持一致。如果使用可变参数传值，则要保证参数顺序与 URL 模板中的占位符顺序一致。

### Query 参数

Query 参数位于 URL 的 `?` 后面，常用于分页查询、条件筛选、关键词搜索等接口。例如 `/mock-api/users?keyword=Ateng&status=1&pageNum=1&pageSize=10`。

不建议手动拼接 Query 字符串，尤其是参数中可能包含空格、中文、特殊符号时，容易出现编码问题。推荐使用 `UriComponentsBuilder` 构建 URL。

示例代码在前面的 `UserParamApiClient#listUsersByQuery` 方法中：

```java
String url = UriComponentsBuilder.fromPath("/mock-api/users")
        .queryParamIfPresent("keyword", Optional.ofNullable(keyword).filter(StrUtil::isNotBlank))
        .queryParamIfPresent("status", Optional.ofNullable(status))
        .queryParam("pageNum", ObjUtil.defaultIfNull(pageNum, 1))
        .queryParam("pageSize", ObjUtil.defaultIfNull(pageSize, 10))
        .build()
        .encode()
        .toUriString();
```

调用示例：

```java
List<UserResponse> users = userParamApiClient.listUsersByQuery("Ateng", 1, 1, 10);
```

对应 HTTP 请求如下：

```bash
curl -X GET 'http://localhost:8081/mock-api/users?keyword=Ateng&status=1&pageNum=1&pageSize=10'
```

如果 Query 参数是集合，可以通过多次追加同名参数或使用逗号拼接，具体取决于远程接口约定。

```java
List<Integer> statusList = List.of(1, 2, 3);

String url = UriComponentsBuilder.fromPath("/mock-api/users")
        // 生成 status=1&status=2&status=3
        .queryParam("status", statusList.toArray())
        .build()
        .encode()
        .toUriString();
```

如果远程接口约定使用逗号分隔，例如 `status=1,2,3`，可以使用 Hutool 的集合工具处理：

```java
List<Integer> statusList = List.of(1, 2, 3);

String statusText = CollUtil.join(statusList, ",");

String url = UriComponentsBuilder.fromPath("/mock-api/users")
        // 生成 status=1,2,3
        .queryParam("status", statusText)
        .build()
        .encode()
        .toUriString();
```

Query 参数处理建议如下：

| 建议                        | 说明                                    |
| --------------------------- | --------------------------------------- |
| 使用 `UriComponentsBuilder` | 避免手动拼接 URL 导致编码错误           |
| 空值不传递                  | 使用 `queryParamIfPresent` 控制可选参数 |
| 分页参数给默认值            | 例如 `pageNum=1`、`pageSize=10`         |
| 集合参数遵循接口约定        | 明确使用重复参数还是逗号分隔            |
| 关键词需要编码              | 中文、空格、特殊符号必须经过编码        |

### Header 参数

Header 参数用于传递请求元信息，例如认证令牌、内容类型、链路追踪 ID、租户 ID、客户端来源等。常见请求头包括 `Authorization`、`Content-Type`、`Accept`、`X-Trace-Id`、`X-Tenant-Id`。

`RestTemplate` 中请求头通常通过 `HttpHeaders` 设置，再通过 `HttpEntity` 包装。对于 GET 请求，请求体可以为空，但仍然可以使用 `HttpEntity<Void>` 携带请求头。

示例代码在前面的 `UserParamApiClient#getUserWithHeader` 方法中：

```java
HttpHeaders headers = new HttpHeaders();
headers.setBearerAuth(token);
headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
headers.set("X-Trace-Id", IdUtil.fastSimpleUUID());

HttpEntity<Void> httpEntity = new HttpEntity<>(headers);

ResponseEntity<UserResponse> responseEntity = restTemplate.exchange(
        "/mock-api/users/{id}",
        HttpMethod.GET,
        httpEntity,
        UserResponse.class,
        userId
);
```

调用示例：

```java
UserResponse user = userParamApiClient.getUserWithHeader(1L, "access-token-value");
```

对应 HTTP 请求如下：

```bash
curl -X GET 'http://localhost:8081/mock-api/users/1' \
  -H 'Authorization: Bearer access-token-value' \
  -H 'Accept: application/json' \
  -H 'X-Trace-Id: 6f51de75b89d4f4e9e8fb1e6b7d2d5ad'
```

常见 Header 设置方式如下：

```java
HttpHeaders headers = new HttpHeaders();

// 设置 Bearer Token，最终生成 Authorization: Bearer xxx
headers.setBearerAuth("access-token-value");

// 设置请求体类型，POST、PUT、PATCH 请求常用
headers.setContentType(MediaType.APPLICATION_JSON);

// 设置期望响应类型
headers.setAccept(List.of(MediaType.APPLICATION_JSON));

// 设置自定义链路追踪ID
headers.set("X-Trace-Id", IdUtil.fastSimpleUUID());

// 设置租户ID，多租户系统常用
headers.set("X-Tenant-Id", "tenant-001");
```

Header 参数处理建议如下：

| 请求头          | 作用         | 示例                        |
| --------------- | ------------ | --------------------------- |
| `Authorization` | 认证信息     | `Bearer access-token-value` |
| `Content-Type`  | 请求体格式   | `application/json`          |
| `Accept`        | 期望响应格式 | `application/json`          |
| `X-Trace-Id`    | 链路追踪 ID  | UUID、雪花 ID、网关 TraceId |
| `X-Tenant-Id`   | 租户标识     | `tenant-001`                |

在实际项目中，通用 Header 不建议在每个方法里重复设置。后续“请求拦截器配置”章节可以通过 `ClientHttpRequestInterceptor` 统一追加 `X-Trace-Id`、租户 ID、认证信息等公共请求头。

### Body 参数

Body 参数用于传递请求体，常见格式是 JSON。POST、PUT、PATCH 请求通常会使用 Body 参数，例如创建用户、更新用户、复杂条件搜索等接口。

本节先定义一个用于搜索用户的请求对象。

文件位置：`src/main/java/io/github/atengk/remote/dto/UserSearchRequest.java`

下面的请求对象用于封装复杂查询条件，适合通过 POST 请求体传递。

```java
package io.github.atengk.remote.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户搜索请求
 *
 * @param keyword       关键词
 * @param statusList    状态列表
 * @param startTime     开始时间
 * @param endTime       结束时间
 * @param includeLocked 是否包含锁定用户
 * @author Ateng
 * @since 2026-04-30
 */
public record UserSearchRequest(
        String keyword,
        List<Integer> statusList,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Boolean includeLocked
) {
}
```

Body 参数可以直接通过 `postForEntity` 发送，也可以通过 `HttpEntity` 同时包装请求头和请求体。推荐使用 `HttpEntity`，因为实际项目中大多数远程接口都需要设置 `Content-Type`、认证信息或链路追踪 ID。

示例代码在前面的 `UserParamApiClient#searchUsersByBody` 方法中：

```java
HttpHeaders headers = new HttpHeaders();
headers.setContentType(MediaType.APPLICATION_JSON);
headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
headers.set("X-Trace-Id", IdUtil.fastSimpleUUID());

HttpEntity<UserSearchRequest> httpEntity = new HttpEntity<>(request, headers);

ResponseEntity<List<UserResponse>> responseEntity = restTemplate.exchange(
        "/mock-api/users/search",
        HttpMethod.POST,
        httpEntity,
        new ParameterizedTypeReference<>() {
        }
);
```

调用示例：

```java
UserSearchRequest request = new UserSearchRequest(
        "Ateng",
        List.of(1, 2),
        null,
        null,
        false
);

List<UserResponse> users = userParamApiClient.searchUsersByBody(request);
```

对应 HTTP 请求如下：

```bash
curl -X POST 'http://localhost:8081/mock-api/users/search' \
  -H 'Content-Type: application/json' \
  -H 'Accept: application/json' \
  -H 'X-Trace-Id: 6f51de75b89d4f4e9e8fb1e6b7d2d5ad' \
  -d '{
    "keyword": "Ateng",
    "statusList": [1, 2],
    "startTime": null,
    "endTime": null,
    "includeLocked": false
  }'
```

如果接口同时包含路径参数、请求头和请求体，可以统一使用 `exchange` 方法处理。前面的 `updateUserWithPathHeaderAndBody` 方法就是典型写法：

```java
HttpEntity<UserUpdateRequest> httpEntity = new HttpEntity<>(request, headers);

ResponseEntity<Void> responseEntity = restTemplate.exchange(
        "/mock-api/users/{id}",
        HttpMethod.PUT,
        httpEntity,
        Void.class,
        userId
);
```

对应 HTTP 请求如下：

```bash
curl -X PUT 'http://localhost:8081/mock-api/users/1' \
  -H 'Authorization: Bearer access-token-value' \
  -H 'Content-Type: application/json' \
  -H 'Accept: application/json' \
  -H 'X-Trace-Id: 6f51de75b89d4f4e9e8fb1e6b7d2d5ad' \
  -d '{
    "nickname": "Ateng-New",
    "mobile": "18800000001",
    "status": 1
  }'
```

Body 参数处理建议如下：

| 建议                    | 说明                                        |
| ----------------------- | ------------------------------------------- |
| 使用 DTO 承载请求体     | 不建议直接传散乱的 `Map`，除非字段完全动态  |
| 显式设置 `Content-Type` | JSON 请求设置为 `application/json`          |
| 复杂调用使用 `exchange` | 同时处理 Method、Header、Body、ResponseType |
| 请求对象先做空值校验    | 避免把无效请求发送到远程服务                |
| 日志不要输出敏感字段    | 例如密码、Token、身份证号、银行卡号等       |

到这里，`RestTemplate` 中常见的 PathVariable、Query、Header 和 Body 参数已经覆盖。实际项目中，建议把公共 Header、TraceId、Token、租户信息等通用参数统一放到拦截器中处理，把接口特有参数保留在具体客户端方法中，避免远程调用代码重复膨胀。

以下内容延续你上传的大纲中的“响应结果处理”部分。

## 响应结果处理

这一部分用于说明 `RestTemplate` 如何接收和处理远程接口响应，包括字符串响应、对象响应、泛型响应以及 `ResponseEntity` 的使用。实际开发中，响应处理不应只关注响应体，还需要结合 HTTP 状态码、响应头、空响应、泛型反序列化和业务状态码进行判断。

常见响应处理方式如下：

| 响应类型   | 适用场景                              | 推荐写法                     |
| ---------- | ------------------------------------- | ---------------------------- |
| 字符串响应 | 文本、HTML、原始 JSON、简单状态值     | `String.class`               |
| 对象响应   | 返回固定 JSON 对象                    | `UserResponse.class`         |
| 泛型响应   | 返回 `List<T>`、`Result<T>`、分页对象 | `ParameterizedTypeReference` |
| 完整响应   | 需要状态码、响应头、响应体            | `ResponseEntity<T>`          |

### 字符串响应

字符串响应适合处理纯文本、HTML、XML、原始 JSON 字符串或第三方接口返回格式不固定的场景。使用 `String.class` 接收响应时，`RestTemplate` 不会把响应体转换成业务对象，而是直接返回原始文本内容。

常见场景包括：

| 场景            | 示例                             |
| --------------- | -------------------------------- |
| 健康检查接口    | `OK`、`success`                  |
| 第三方原始 JSON | 需要先记录日志，再按业务字段解析 |
| HTML 页面       | 获取页面源码                     |
| XML 文本        | 对接老系统或传统接口             |

文件位置：

```text
src/main/java/io/github/atengk/remote/client/UserResponseApiClient.java
```

文件位置：`src/main/java/io/github/atengk/remote/client/UserResponseApiClient.java`

下面的客户端类集中演示字符串、对象、泛型和 `ResponseEntity` 响应处理方式。

```java
package io.github.atengk.remote.client;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import io.github.atengk.remote.dto.ApiResult;
import io.github.atengk.remote.dto.PageResponse;
import io.github.atengk.remote.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * 用户远程接口响应示例客户端
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserResponseApiClient {

    private final RestTemplate restTemplate;

    /**
     * 获取远程服务健康检查文本。
     *
     * @return 健康检查响应文本
     */
    public String getHealthText() {
        log.info("开始获取远程服务健康检查文本");
        String responseText = restTemplate.getForObject("/mock-api/health", String.class);

        if (StrUtil.isBlank(responseText)) {
            log.warn("远程服务健康检查响应为空");
            return "";
        }

        log.info("远程服务健康检查响应，responseText={}", responseText);
        return responseText;
    }

    /**
     * 获取远程用户原始 JSON 字符串。
     *
     * @param userId 用户ID
     * @return 原始 JSON 字符串
     */
    public String getUserRawJson(Long userId) {
        if (ObjUtil.isNull(userId)) {
            log.warn("获取远程用户原始JSON失败，用户ID为空");
            return "";
        }

        log.info("开始获取远程用户原始JSON，userId={}", userId);
        String rawJson = restTemplate.getForObject("/mock-api/users/{id}", String.class, userId);

        if (StrUtil.isBlank(rawJson)) {
            log.warn("远程用户原始JSON为空，userId={}", userId);
            return "";
        }

        return rawJson;
    }

    /**
     * 获取远程用户并手动解析 JSON。
     *
     * @param userId 用户ID
     * @return 用户响应数据
     */
    public UserResponse getUserByManualJsonParse(Long userId) {
        String rawJson = this.getUserRawJson(userId);
        if (StrUtil.isBlank(rawJson)) {
            return null;
        }

        log.info("开始解析远程用户JSON，userId={}", userId);
        return JSONUtil.toBean(rawJson, UserResponse.class);
    }

    /**
     * 获取远程用户对象。
     *
     * @param userId 用户ID
     * @return 用户响应数据
     */
    public UserResponse getUserObject(Long userId) {
        if (ObjUtil.isNull(userId)) {
            log.warn("获取远程用户对象失败，用户ID为空");
            return null;
        }

        log.info("开始获取远程用户对象，userId={}", userId);
        return restTemplate.getForObject("/mock-api/users/{id}", UserResponse.class, userId);
    }

    /**
     * 获取远程用户列表。
     *
     * @return 用户列表
     */
    public List<UserResponse> listUserObjects() {
        log.info("开始获取远程用户列表");

        ResponseEntity<List<UserResponse>> responseEntity = restTemplate.exchange(
                "/mock-api/users",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {
                }
        );

        List<UserResponse> body = responseEntity.getBody();
        if (CollUtil.isEmpty(body)) {
            log.info("远程用户列表为空");
            return List.of();
        }

        return body;
    }

    /**
     * 获取统一响应包装的用户对象。
     *
     * @param userId 用户ID
     * @return 统一响应中的用户数据
     */
    public UserResponse getUserFromApiResult(Long userId) {
        if (ObjUtil.isNull(userId)) {
            log.warn("获取统一响应用户失败，用户ID为空");
            return null;
        }

        log.info("开始获取统一响应用户，userId={}", userId);
        ResponseEntity<ApiResult<UserResponse>> responseEntity = restTemplate.exchange(
                "/mock-api/result/users/{id}",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {
                },
                userId
        );

        ApiResult<UserResponse> result = responseEntity.getBody();
        if (ObjUtil.isNull(result)) {
            log.warn("统一响应为空，userId={}", userId);
            return null;
        }
        if (!result.success()) {
            log.warn("远程接口业务处理失败，userId={}, code={}, message={}",
                    userId, result.code(), result.message());
            return null;
        }

        return result.data();
    }

    /**
     * 获取统一响应包装的分页用户数据。
     *
     * @param pageNum  页码
     * @param pageSize 每页数量
     * @return 分页用户数据
     */
    public PageResponse<UserResponse> pageUsersFromApiResult(Integer pageNum, Integer pageSize) {
        Integer currentPageNum = ObjUtil.defaultIfNull(pageNum, 1);
        Integer currentPageSize = ObjUtil.defaultIfNull(pageSize, 10);

        log.info("开始获取统一响应分页用户数据，pageNum={}, pageSize={}", currentPageNum, currentPageSize);
        ResponseEntity<ApiResult<PageResponse<UserResponse>>> responseEntity = restTemplate.exchange(
                "/mock-api/result/users/page?pageNum={pageNum}&pageSize={pageSize}",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {
                },
                currentPageNum,
                currentPageSize
        );

        ApiResult<PageResponse<UserResponse>> result = responseEntity.getBody();
        if (ObjUtil.isNull(result)) {
            log.warn("分页统一响应为空，pageNum={}, pageSize={}", currentPageNum, currentPageSize);
            return PageResponse.empty();
        }
        if (!result.success()) {
            log.warn("远程分页接口业务处理失败，code={}, message={}", result.code(), result.message());
            return PageResponse.empty();
        }

        return ObjUtil.defaultIfNull(result.data(), PageResponse.empty());
    }

    /**
     * 获取完整响应实体。
     *
     * @param userId 用户ID
     * @return 用户响应实体
     */
    public ResponseEntity<UserResponse> getUserResponseEntity(Long userId) {
        if (ObjUtil.isNull(userId)) {
            log.warn("获取完整响应实体失败，用户ID为空");
            return ResponseEntity.badRequest().build();
        }

        log.info("开始获取完整响应实体，userId={}", userId);
        return restTemplate.getForEntity("/mock-api/users/{id}", UserResponse.class, userId);
    }

    /**
     * 获取响应状态、响应头和响应体。
     *
     * @param userId 用户ID
     * @return 用户响应数据
     */
    public UserResponse getUserWithResponseInfo(Long userId) {
        ResponseEntity<UserResponse> responseEntity = this.getUserResponseEntity(userId);

        if (!responseEntity.getStatusCode().is2xxSuccessful()) {
            log.warn("远程接口HTTP状态异常，userId={}, statusCode={}", userId, responseEntity.getStatusCode());
            return null;
        }

        HttpHeaders headers = responseEntity.getHeaders();
        String traceId = headers.getFirst("X-Trace-Id");
        String requestId = headers.getFirst("X-Request-Id");

        log.info("远程接口响应成功，userId={}, statusCode={}, traceId={}, requestId={}",
                userId, responseEntity.getStatusCode(), traceId, requestId);

        return responseEntity.getBody();
    }

}
```

调用字符串响应示例：

```java
String healthText = userResponseApiClient.getHealthText();
String rawJson = userResponseApiClient.getUserRawJson(1L);
UserResponse user = userResponseApiClient.getUserByManualJsonParse(1L);
```

对应 HTTP 请求如下：

```bash
# 获取健康检查文本
curl -X GET 'http://localhost:8081/mock-api/health'

# 获取用户原始 JSON
curl -X GET 'http://localhost:8081/mock-api/users/1'
```

如果远程接口返回的是 JSON，通常优先使用对象响应或泛型响应。只有在接口返回结构不稳定、需要落库原始报文、需要签名验签、需要兼容历史接口时，才建议使用字符串响应。

### 对象响应

对象响应适合远程接口返回结构固定的 JSON，例如根据用户 ID 查询用户信息。`RestTemplate` 会通过 Spring MVC 默认配置的 Jackson 消息转换器，把 JSON 响应体反序列化为 Java 对象。

前面章节已经定义过 `UserResponse`，这里继续复用该对象：

```java
package io.github.atengk.remote.dto;

import java.time.LocalDateTime;

/**
 * 用户响应数据
 *
 * @param id        用户ID
 * @param username  用户名
 * @param nickname  昵称
 * @param mobile    手机号
 * @param status    状态
 * @param createdAt 创建时间
 * @author Ateng
 * @since 2026-04-30
 */
public record UserResponse(
        Long id,
        String username,
        String nickname,
        String mobile,
        Integer status,
        LocalDateTime createdAt
) {
}
```

对象响应可以使用 `getForObject`，也可以使用 `getForEntity`。

```java
/**
 * 获取远程用户对象。
 *
 * @param userId 用户ID
 * @return 用户响应数据
 */
public UserResponse getUserObject(Long userId) {
    if (ObjUtil.isNull(userId)) {
        log.warn("获取远程用户对象失败，用户ID为空");
        return null;
    }

    log.info("开始获取远程用户对象，userId={}", userId);
    return restTemplate.getForObject("/mock-api/users/{id}", UserResponse.class, userId);
}
```

调用示例：

```java
UserResponse user = userResponseApiClient.getUserObject(1L);
```

远程接口响应示例：

```json
{
  "id": 1,
  "username": "ateng",
  "nickname": "Ateng",
  "mobile": "18800000000",
  "status": 1,
  "createdAt": "2026-04-30T10:30:00"
}
```

对象响应处理建议如下：

| 建议                        | 说明                                   |
| --------------------------- | -------------------------------------- |
| 字段名称保持一致            | JSON 字段名应与 Java 字段名匹配        |
| 时间格式统一                | `LocalDateTime` 建议使用 ISO-8601 格式 |
| 不确定字段可忽略            | 可通过 Jackson 配置忽略未知字段        |
| 不建议用 `Map` 承接稳定对象 | 固定结构优先使用 DTO                   |
| 空响应需要判断              | 远程接口可能返回空 body                |

如果第三方接口字段命名不符合 Java 习惯，例如返回 `user_id`、`created_at`，可以使用 Jackson 注解处理。

文件位置：`src/main/java/io/github/atengk/remote/dto/ThirdUserResponse.java`

下面的 DTO 用于适配下划线命名的第三方接口响应。

```java
package io.github.atengk.remote.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

/**
 * 第三方用户响应数据
 *
 * @param userId    用户ID
 * @param userName  用户名
 * @param createdAt 创建时间
 * @author Ateng
 * @since 2026-04-30
 */
public record ThirdUserResponse(
        @JsonProperty("user_id")
        Long userId,

        @JsonProperty("user_name")
        String userName,

        @JsonProperty("created_at")
        LocalDateTime createdAt
) {
}
```

### 泛型响应

泛型响应是 `RestTemplate` 中最容易出错的部分。由于 Java 泛型存在类型擦除，不能直接使用 `List<UserResponse>.class` 或 `ApiResult<UserResponse>.class`。处理 `List<T>`、`Result<T>`、`Page<T>` 等响应时，推荐使用 `exchange` 搭配 `ParameterizedTypeReference`。

常见泛型响应包括：

| 响应结构     | 示例                                    |
| ------------ | --------------------------------------- |
| 列表响应     | `List<UserResponse>`                    |
| 统一响应对象 | `ApiResult<UserResponse>`               |
| 分页响应对象 | `ApiResult<PageResponse<UserResponse>>` |
| Map 响应     | `Map<String, Object>`                   |

文件位置：`src/main/java/io/github/atengk/remote/dto/ApiResult.java`

下面的统一响应对象用于承接远程服务常见的业务包装结构。

```java
package io.github.atengk.remote.dto;

import cn.hutool.core.util.StrUtil;

/**
 * 统一接口响应
 *
 * @param code    业务状态码
 * @param message 响应消息
 * @param data    响应数据
 * @param <T>     数据类型
 * @author Ateng
 * @since 2026-04-30
 */
public record ApiResult<T>(
        Integer code,
        String message,
        T data
) {

    /**
     * 判断业务响应是否成功。
     *
     * @return 是否成功
     */
    public boolean success() {
        return Integer.valueOf(200).equals(code) || StrUtil.equalsIgnoreCase("success", message);
    }

}
```

文件位置：`src/main/java/io/github/atengk/remote/dto/PageResponse.java`

下面的分页响应对象用于承接远程服务返回的分页数据。

```java
package io.github.atengk.remote.dto;

import java.util.List;

/**
 * 分页响应数据
 *
 * @param records  当前页记录
 * @param total    总记录数
 * @param pageNum  当前页码
 * @param pageSize 每页数量
 * @param <T>      记录类型
 * @author Ateng
 * @since 2026-04-30
 */
public record PageResponse<T>(
        List<T> records,
        Long total,
        Integer pageNum,
        Integer pageSize
) {

    /**
     * 创建空分页对象。
     *
     * @param <T> 记录类型
     * @return 空分页对象
     */
    public static <T> PageResponse<T> empty() {
        return new PageResponse<>(List.of(), 0L, 1, 10);
    }

}
```

获取 `List<UserResponse>` 示例：

```java
/**
 * 获取远程用户列表。
 *
 * @return 用户列表
 */
public List<UserResponse> listUserObjects() {
    log.info("开始获取远程用户列表");

    ResponseEntity<List<UserResponse>> responseEntity = restTemplate.exchange(
            "/mock-api/users",
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<>() {
            }
    );

    List<UserResponse> body = responseEntity.getBody();
    if (CollUtil.isEmpty(body)) {
        log.info("远程用户列表为空");
        return List.of();
    }

    return body;
}
```

获取 `ApiResult<UserResponse>` 示例：

```java
/**
 * 获取统一响应包装的用户对象。
 *
 * @param userId 用户ID
 * @return 统一响应中的用户数据
 */
public UserResponse getUserFromApiResult(Long userId) {
    if (ObjUtil.isNull(userId)) {
        log.warn("获取统一响应用户失败，用户ID为空");
        return null;
    }

    log.info("开始获取统一响应用户，userId={}", userId);
    ResponseEntity<ApiResult<UserResponse>> responseEntity = restTemplate.exchange(
            "/mock-api/result/users/{id}",
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<>() {
            },
            userId
    );

    ApiResult<UserResponse> result = responseEntity.getBody();
    if (ObjUtil.isNull(result)) {
        log.warn("统一响应为空，userId={}", userId);
        return null;
    }
    if (!result.success()) {
        log.warn("远程接口业务处理失败，userId={}, code={}, message={}",
                userId, result.code(), result.message());
        return null;
    }

    return result.data();
}
```

获取 `ApiResult<PageResponse<UserResponse>>` 示例：

```java
/**
 * 获取统一响应包装的分页用户数据。
 *
 * @param pageNum  页码
 * @param pageSize 每页数量
 * @return 分页用户数据
 */
public PageResponse<UserResponse> pageUsersFromApiResult(Integer pageNum, Integer pageSize) {
    Integer currentPageNum = ObjUtil.defaultIfNull(pageNum, 1);
    Integer currentPageSize = ObjUtil.defaultIfNull(pageSize, 10);

    log.info("开始获取统一响应分页用户数据，pageNum={}, pageSize={}", currentPageNum, currentPageSize);
    ResponseEntity<ApiResult<PageResponse<UserResponse>>> responseEntity = restTemplate.exchange(
            "/mock-api/result/users/page?pageNum={pageNum}&pageSize={pageSize}",
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<>() {
            },
            currentPageNum,
            currentPageSize
    );

    ApiResult<PageResponse<UserResponse>> result = responseEntity.getBody();
    if (ObjUtil.isNull(result)) {
        log.warn("分页统一响应为空，pageNum={}, pageSize={}", currentPageNum, currentPageSize);
        return PageResponse.empty();
    }
    if (!result.success()) {
        log.warn("远程分页接口业务处理失败，code={}, message={}", result.code(), result.message());
        return PageResponse.empty();
    }

    return ObjUtil.defaultIfNull(result.data(), PageResponse.empty());
}
```

调用示例：

```java
List<UserResponse> users = userResponseApiClient.listUserObjects();

UserResponse user = userResponseApiClient.getUserFromApiResult(1L);

PageResponse<UserResponse> pageResponse = userResponseApiClient.pageUsersFromApiResult(1, 10);
```

分页响应示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [
      {
        "id": 1,
        "username": "ateng",
        "nickname": "Ateng",
        "mobile": "18800000000",
        "status": 1,
        "createdAt": "2026-04-30T10:30:00"
      }
    ],
    "total": 1,
    "pageNum": 1,
    "pageSize": 10
  }
}
```

泛型响应处理建议如下：

| 建议                               | 说明                             |
| ---------------------------------- | -------------------------------- |
| 使用 `ParameterizedTypeReference`  | 避免泛型类型擦除导致反序列化异常 |
| 不要使用 `List.class` 承接业务列表 | 可能得到 `List<LinkedHashMap>`   |
| 统一响应先判断业务状态             | HTTP 200 不代表业务成功          |
| 分页对象提供空对象                 | 避免调用方重复判断 `null`        |
| 日志记录业务失败信息               | 便于定位第三方接口异常           |

### ResponseEntity 使用

`ResponseEntity<T>` 用于接收完整 HTTP 响应信息，包括状态码、响应头和响应体。相比 `getForObject` 只返回响应体，`ResponseEntity` 更适合生产项目中的远程接口调用，因为它可以判断 HTTP 状态、读取响应头中的链路信息、分页信息、限流信息或文件信息。

常见使用场景如下：

| 场景             | 说明                                                 |
| ---------------- | ---------------------------------------------------- |
| 判断 HTTP 状态码 | 判断是否为 2xx、4xx、5xx                             |
| 读取响应头       | 获取 `X-Trace-Id`、`Location`、`Content-Disposition` |
| 下载文件         | 读取文件名、文件类型、文件字节                       |
| 处理空响应       | 例如 DELETE 返回 `204 No Content`                    |
| 调试接口         | 记录状态码和响应头                                   |

使用 `getForEntity` 获取完整响应：

```java
/**
 * 获取完整响应实体。
 *
 * @param userId 用户ID
 * @return 用户响应实体
 */
public ResponseEntity<UserResponse> getUserResponseEntity(Long userId) {
    if (ObjUtil.isNull(userId)) {
        log.warn("获取完整响应实体失败，用户ID为空");
        return ResponseEntity.badRequest().build();
    }

    log.info("开始获取完整响应实体，userId={}", userId);
    return restTemplate.getForEntity("/mock-api/users/{id}", UserResponse.class, userId);
}
```

读取状态码、响应头和响应体：

```java
/**
 * 获取响应状态、响应头和响应体。
 *
 * @param userId 用户ID
 * @return 用户响应数据
 */
public UserResponse getUserWithResponseInfo(Long userId) {
    ResponseEntity<UserResponse> responseEntity = this.getUserResponseEntity(userId);

    if (!responseEntity.getStatusCode().is2xxSuccessful()) {
        log.warn("远程接口HTTP状态异常，userId={}, statusCode={}", userId, responseEntity.getStatusCode());
        return null;
    }

    HttpHeaders headers = responseEntity.getHeaders();
    String traceId = headers.getFirst("X-Trace-Id");
    String requestId = headers.getFirst("X-Request-Id");

    log.info("远程接口响应成功，userId={}, statusCode={}, traceId={}, requestId={}",
            userId, responseEntity.getStatusCode(), traceId, requestId);

    return responseEntity.getBody();
}
```

调用示例：

```java
ResponseEntity<UserResponse> responseEntity = userResponseApiClient.getUserResponseEntity(1L);

if (responseEntity.getStatusCode().is2xxSuccessful()) {
    UserResponse user = responseEntity.getBody();
}
```

对应 HTTP 请求如下：

```bash
curl -i -X GET 'http://localhost:8081/mock-api/users/1'
```

`curl -i` 会同时输出响应头和响应体，适合验证 `ResponseEntity` 能读取到的内容。

示例响应：

```text
HTTP/1.1 200
Content-Type: application/json
X-Trace-Id: 6f51de75b89d4f4e9e8fb1e6b7d2d5ad
X-Request-Id: req-20260430103000001

{
  "id": 1,
  "username": "ateng",
  "nickname": "Ateng",
  "mobile": "18800000000",
  "status": 1,
  "createdAt": "2026-04-30T10:30:00"
}
```

如果接口没有响应体，例如删除接口返回 `204 No Content`，可以使用 `ResponseEntity<Void>`。

```java
/**
 * 删除用户并读取 HTTP 状态。
 *
 * @param userId 用户ID
 * @return 是否删除成功
 */
public Boolean deleteUserWithResponseEntity(Long userId) {
    if (ObjUtil.isNull(userId)) {
        log.warn("删除远程用户失败，用户ID为空");
        return false;
    }

    log.info("开始删除远程用户并读取响应状态，userId={}", userId);
    ResponseEntity<Void> responseEntity = restTemplate.exchange(
            "/mock-api/users/{id}",
            HttpMethod.DELETE,
            null,
            Void.class,
            userId
    );

    boolean success = responseEntity.getStatusCode().is2xxSuccessful();
    log.info("删除远程用户完成，userId={}, success={}, statusCode={}",
            userId, success, responseEntity.getStatusCode());
    return success;
}
```

`ResponseEntity` 使用建议如下：

| 建议                              | 说明                        |
| --------------------------------- | --------------------------- |
| 生产代码优先使用 `ResponseEntity` | 方便判断状态码和响应头      |
| 只关心响应体可用 `getForObject`   | 简单查询可以简化代码        |
| DELETE、PUT 可用 `exchange`       | 便于获取状态码              |
| 业务响应和 HTTP 响应分开判断      | HTTP 2xx 不代表业务一定成功 |
| 日志中记录状态码和 TraceId        | 便于排查接口链路问题        |

到这里，`RestTemplate` 的字符串响应、对象响应、泛型响应和完整响应实体处理已经覆盖。实际项目中建议优先封装统一响应解析逻辑，避免每个远程调用方法都重复判断空响应、HTTP 状态码和业务状态码。

## 常用配置

这一部分用于说明 `RestTemplate` 在生产项目中常用的基础配置，包括连接超时、读取超时、请求拦截器和消息转换器。`RestTemplate` 通常作为共享组件使用，但配置应在启动阶段完成，不建议在运行过程中动态修改；如果不同业务接口需要不同配置，建议创建多个不同命名的 `RestTemplate` Bean。([Home](https://docs.spring.io/spring-framework/docs/6.1.14/javadoc-api/org/springframework/web/client/RestTemplate.html?utm_source=chatgpt.com))

本节配置基于前面章节的 `RestTemplate` Bean 继续增强，当前大纲来源于你上传的文档结构。

### 连接超时配置

连接超时用于控制客户端与远程服务建立 TCP 连接时的最大等待时间。如果远程服务地址不可达、网络不通、端口未监听或防火墙阻断，连接超时可以避免业务线程长时间阻塞。

在 Spring Boot 3.4+ 中，`RestTemplateBuilder` 推荐使用 `connectTimeout(Duration)` 和 `readTimeout(Duration)`；旧的 `setConnectTimeout(Duration)`、`setReadTimeout(Duration)` 已标记为废弃并计划在后续版本移除。([Home](https://docs.spring.io/spring-boot/3.4.5/api/java/org/springframework/boot/web/client/RestTemplateBuilder.html?utm_source=chatgpt.com))

文件位置：

```text
src/main/resources/application.yml
src/main/java/io/github/atengk/config/properties/RestTemplateProperties.java
src/main/java/io/github/atengk/config/RestTemplateConfig.java
```

文件位置：`src/main/resources/application.yml`

```yaml
demo:
  rest-template:
    # 远程服务基础地址，使用 rootUri 后请求可直接写 /mock-api/users
    root-uri: http://localhost:8081
    # 建立连接的最大等待时间，网络不可达或端口不可用时生效
    connect-timeout: 3s
    # 从连接池获取连接的最大等待时间，使用 Apache HttpClient 连接池时生效
    connection-request-timeout: 2s
    # 连接建立成功后，等待远程服务返回数据的最大时间
    read-timeout: 10s
```

文件位置：`src/main/java/io/github/atengk/config/properties/RestTemplateProperties.java`

下面的配置属性类用于读取 `application.yml` 中的超时参数。

```java
package io.github.atengk.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * RestTemplate 配置属性
 *
 * @param rootUri                  远程服务基础地址
 * @param connectTimeout           连接超时时间
 * @param connectionRequestTimeout 从连接池获取连接的超时时间
 * @param readTimeout              读取超时时间
 * @author Ateng
 * @since 2026-04-30
 */
@ConfigurationProperties(prefix = "demo.rest-template")
public record RestTemplateProperties(
        String rootUri,
        Duration connectTimeout,
        Duration connectionRequestTimeout,
        Duration readTimeout
) {
}
```

文件位置：`src/main/java/io/github/atengk/config/RestTemplateConfig.java`

下面的配置类通过 `RestTemplateBuilder` 设置连接超时、读取超时和基础地址。

```java
package io.github.atengk.config;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.config.properties.RestTemplateProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * RestTemplate 基础配置
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Configuration
@EnableConfigurationProperties(RestTemplateProperties.class)
public class RestTemplateConfig {

    /**
     * 创建 RestTemplate Bean。
     *
     * @param builder    RestTemplate 构建器
     * @param properties RestTemplate 配置属性
     * @return RestTemplate 实例
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder, RestTemplateProperties properties) {
        String rootUri = StrUtil.blankToDefault(properties.rootUri(), "http://localhost:8081");
        Duration connectTimeout = defaultDuration(properties.connectTimeout(), Duration.ofSeconds(3));
        Duration readTimeout = defaultDuration(properties.readTimeout(), Duration.ofSeconds(10));

        return builder
                // 统一远程服务基础地址
                .rootUri(rootUri)
                // 建立连接的最大等待时间
                .connectTimeout(connectTimeout)
                // 读取响应数据的最大等待时间
                .readTimeout(readTimeout)
                // 默认接收 JSON 响应
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * 获取默认时间配置。
     *
     * @param value        配置值
     * @param defaultValue 默认值
     * @return 最终时间配置
     */
    private Duration defaultDuration(Duration value, Duration defaultValue) {
        return value == null ? defaultValue : value;
    }

}
```

如果项目使用的是 Spring Boot 3.0 至 3.3，仍可使用下面写法：

```java
return builder
        .rootUri(rootUri)
        .setConnectTimeout(connectTimeout)
        .setReadTimeout(readTimeout)
        .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        .build();
```

连接超时建议值如下：

| 场景             | 建议连接超时               |
| ---------------- | -------------------------- |
| 内网服务调用     | `1s` 到 `3s`               |
| 第三方公网接口   | `3s` 到 `5s`               |
| 弱网络或跨境接口 | `5s` 到 `10s`              |
| 定时任务批量同步 | 可适当放宽，但必须设置上限 |

连接超时不宜设置过长。对于在线接口，连接超时过长会直接占用业务线程，放大远程服务不可用带来的影响。

### 读取超时配置

读取超时用于控制连接建立成功后，客户端等待远程服务返回响应数据的最大时间。如果远程服务已经连接成功，但接口处理慢、数据库慢、下游依赖慢或响应数据迟迟不返回，就会触发读取超时。

`HttpComponentsClientHttpRequestFactory` 可以基于 Apache HttpComponents HttpClient 创建请求，并支持连接超时、连接池取连接超时和读取超时等配置；Spring Framework 6.2 中的读取超时对应底层 `RequestConfig` 的 response timeout。([Home](https://docs.spring.io/spring-framework/docs/6.2.5/javadoc-api/org/springframework/http/client/HttpComponentsClientHttpRequestFactory.html?utm_source=chatgpt.com))

如果项目需要更完整的连接池能力，建议使用 Apache HttpClient 5 作为底层请求工厂。前面“环境准备”章节已经引入过 `httpclient5` 依赖，这里直接使用。

文件位置：`src/main/java/io/github/atengk/config/RestTemplatePoolConfig.java`

下面的配置类使用 Apache HttpClient 5 创建带连接池的 `RestTemplate`。

```java
package io.github.atengk.config;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.config.properties.RestTemplateProperties;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.Timeout;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * RestTemplate 连接池配置
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Configuration
@EnableConfigurationProperties(RestTemplateProperties.class)
public class RestTemplatePoolConfig {

    /**
     * 创建带连接池的 RestTemplate Bean。
     *
     * @param builder    RestTemplate 构建器
     * @param properties RestTemplate 配置属性
     * @return RestTemplate 实例
     */
    @Bean("poolRestTemplate")
    public RestTemplate poolRestTemplate(RestTemplateBuilder builder, RestTemplateProperties properties) {
        String rootUri = StrUtil.blankToDefault(properties.rootUri(), "http://localhost:8081");
        Duration connectTimeout = defaultDuration(properties.connectTimeout(), Duration.ofSeconds(3));
        Duration connectionRequestTimeout = defaultDuration(properties.connectionRequestTimeout(), Duration.ofSeconds(2));
        Duration readTimeout = defaultDuration(properties.readTimeout(), Duration.ofSeconds(10));

        return builder
                .rootUri(rootUri)
                .requestFactory(() -> createRequestFactory(connectTimeout, connectionRequestTimeout, readTimeout))
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * 创建 HttpComponents 请求工厂。
     *
     * @param connectTimeout           连接超时时间
     * @param connectionRequestTimeout 从连接池获取连接的超时时间
     * @param readTimeout              读取超时时间
     * @return 请求工厂
     */
    private HttpComponentsClientHttpRequestFactory createRequestFactory(
            Duration connectTimeout,
            Duration connectionRequestTimeout,
            Duration readTimeout
    ) {
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        // 最大连接数，根据接口并发量调整
        connectionManager.setMaxTotal(200);
        // 单个路由最大连接数，通常按目标服务维度限制
        connectionManager.setDefaultMaxPerRoute(50);

        RequestConfig requestConfig = RequestConfig.custom()
                // 建立连接的超时时间
                .setConnectTimeout(Timeout.ofMilliseconds(connectTimeout.toMillis()))
                // 从连接池获取连接的超时时间
                .setConnectionRequestTimeout(Timeout.ofMilliseconds(connectionRequestTimeout.toMillis()))
                // 等待响应数据的超时时间
                .setResponseTimeout(Timeout.ofMilliseconds(readTimeout.toMillis()))
                .build();

        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .evictExpiredConnections()
                .build();

        return new HttpComponentsClientHttpRequestFactory(httpClient);
    }

    /**
     * 获取默认时间配置。
     *
     * @param value        配置值
     * @param defaultValue 默认值
     * @return 最终时间配置
     */
    private Duration defaultDuration(Duration value, Duration defaultValue) {
        return value == null ? defaultValue : value;
    }

}
```

如果同时存在多个 `RestTemplate` Bean，业务类注入时需要指定名称。

```java
package io.github.atengk.remote.client;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * 带连接池的远程接口客户端
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Component
@RequiredArgsConstructor
public class PoolUserApiClient {

    @Qualifier("poolRestTemplate")
    private final RestTemplate restTemplate;

}
```

读取超时建议值如下：

| 场景                 | 建议读取超时                             |
| -------------------- | ---------------------------------------- |
| 普通查询接口         | `3s` 到 `10s`                            |
| 文件下载接口         | 按文件大小单独配置                       |
| 第三方支付、短信接口 | `5s` 到 `15s`                            |
| 批量同步接口         | 可设置更长，但必须配合重试和任务状态记录 |

读取超时不应代替业务性能优化。如果接口频繁读取超时，应优先排查远程服务耗时、数据库慢 SQL、下游依赖阻塞和网络质量。

### 请求拦截器配置

请求拦截器用于在请求发出前或响应返回后执行统一逻辑，例如追加公共请求头、生成 TraceId、记录请求耗时、打印调用日志、处理租户信息等。`RestTemplate` 与 `RestClient` 共享请求工厂、请求拦截器和消息转换器等基础设施。([Home](https://docs.spring.io/spring-framework/docs/6.1.14/javadoc-api/org/springframework/web/client/RestTemplate.html?utm_source=chatgpt.com))

请求拦截器适合处理通用逻辑，不适合写具体业务参数。业务参数仍应放在具体的远程客户端方法中。

文件位置：

```text
src/main/java/io/github/atengk/config/interceptor/RestTemplateTraceInterceptor.java
src/main/java/io/github/atengk/config/RestTemplateInterceptorConfig.java
```

文件位置：`src/main/java/io/github/atengk/config/interceptor/RestTemplateTraceInterceptor.java`

下面的拦截器用于统一追加 TraceId，并记录远程调用耗时。

```java
package io.github.atengk.config.interceptor;

import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * RestTemplate 链路追踪拦截器
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
public class RestTemplateTraceInterceptor implements ClientHttpRequestInterceptor {

    private static final String TRACE_ID_HEADER = "X-Trace-Id";

    /**
     * 拦截 RestTemplate 请求。
     *
     * @param request   HTTP 请求
     * @param body      请求体字节
     * @param execution 请求执行器
     * @return HTTP 响应
     * @throws IOException IO异常
     */
    @Override
    public ClientHttpResponse intercept(
            HttpRequest request,
            byte[] body,
            ClientHttpRequestExecution execution
    ) throws IOException {
        HttpHeaders headers = request.getHeaders();
        String traceId = headers.getFirst(TRACE_ID_HEADER);
        if (StrUtil.isBlank(traceId)) {
            traceId = IdUtil.fastSimpleUUID();
            headers.set(TRACE_ID_HEADER, traceId);
        }

        TimeInterval timer = new TimeInterval();
        try {
            log.info("开始调用远程接口，method={}, uri={}, traceId={}",
                    request.getMethod(), request.getURI(), traceId);

            ClientHttpResponse response = execution.execute(request, body);

            log.info("远程接口调用完成，method={}, uri={}, statusCode={}, costMs={}, traceId={}",
                    request.getMethod(), request.getURI(), response.getStatusCode(), timer.interval(), traceId);

            return response;
        } catch (IOException ex) {
            log.warn("远程接口调用异常，method={}, uri={}, costMs={}, traceId={}, error={}",
                    request.getMethod(), request.getURI(), timer.interval(), traceId, ex.getMessage());
            throw ex;
        }
    }

}
```

文件位置：`src/main/java/io/github/atengk/config/RestTemplateInterceptorConfig.java`

下面的配置类将自定义拦截器追加到 `RestTemplate` 中。

```java
package io.github.atengk.config;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.config.interceptor.RestTemplateTraceInterceptor;
import io.github.atengk.config.properties.RestTemplateProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * RestTemplate 拦截器配置
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(RestTemplateProperties.class)
public class RestTemplateInterceptorConfig {

    private final RestTemplateTraceInterceptor restTemplateTraceInterceptor;

    /**
     * 创建带请求拦截器的 RestTemplate Bean。
     *
     * @param builder    RestTemplate 构建器
     * @param properties RestTemplate 配置属性
     * @return RestTemplate 实例
     */
    @Bean("traceRestTemplate")
    public RestTemplate traceRestTemplate(RestTemplateBuilder builder, RestTemplateProperties properties) {
        String rootUri = StrUtil.blankToDefault(properties.rootUri(), "http://localhost:8081");
        Duration connectTimeout = properties.connectTimeout() == null ? Duration.ofSeconds(3) : properties.connectTimeout();
        Duration readTimeout = properties.readTimeout() == null ? Duration.ofSeconds(10) : properties.readTimeout();

        return builder
                .rootUri(rootUri)
                .connectTimeout(connectTimeout)
                .readTimeout(readTimeout)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                // 追加自定义拦截器，保留已有自动配置
                .additionalInterceptors(restTemplateTraceInterceptor)
                .build();
    }

}
```

调用任意远程接口后，可以在日志中看到类似输出：

```text
开始调用远程接口，method=GET, uri=http://localhost:8081/mock-api/users/1, traceId=7f6c4f9bfa4b4f2c9f25f522cf4767a4
远程接口调用完成，method=GET, uri=http://localhost:8081/mock-api/users/1, statusCode=200 OK, costMs=36, traceId=7f6c4f9bfa4b4f2c9f25f522cf4767a4
```

请求拦截器建议只处理横切逻辑：

| 适合放入拦截器   | 不建议放入拦截器    |
| ---------------- | ------------------- |
| TraceId          | 具体业务参数        |
| 统一租户 ID      | 某个接口独有 Header |
| 统一调用日志     | 复杂业务判断        |
| 统一认证信息透传 | 请求体字段修改      |
| 统一耗时统计     | 响应体业务解析      |

如果需要打印请求体和响应体，要谨慎处理。请求体和响应体可能包含密码、Token、手机号、身份证号等敏感信息，不建议在生产环境完整打印。

### 消息转换器配置

消息转换器用于处理 HTTP 请求体和响应体与 Java 对象之间的转换。例如 JSON 转 Java 对象、Java 对象转 JSON、字符串响应转 `String`、字节响应转 `byte[]` 等。`RestTemplate` 默认会初始化一组 `HttpMessageConverter`，也允许开发者通过 `setMessageConverters` 或获取转换器列表后追加、调整转换器。([Home](https://docs.spring.io/spring-framework/docs/6.1.16/javadoc-api/org/springframework/web/client/RestTemplate.html?utm_source=chatgpt.com))

在 Spring Boot Web 项目中，大多数 JSON 场景不需要手动配置消息转换器。只有在出现中文乱码、时间格式不符合要求、第三方接口返回特殊 `Content-Type`、需要支持额外媒体类型时，才建议定制。

文件位置：

```text
src/main/java/io/github/atengk/config/RestTemplateMessageConverterConfig.java
```

文件位置：`src/main/java/io/github/atengk/config/RestTemplateMessageConverterConfig.java`

下面的配置类定制了字符串编码和 JSON 消息转换器。

```java
package io.github.atengk.config;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.atengk.config.interceptor.RestTemplateTraceInterceptor;
import io.github.atengk.config.properties.RestTemplateProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * RestTemplate 消息转换器配置
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(RestTemplateProperties.class)
public class RestTemplateMessageConverterConfig {

    private final RestTemplateTraceInterceptor restTemplateTraceInterceptor;

    /**
     * 创建带消息转换器的 RestTemplate Bean。
     *
     * @param builder      RestTemplate 构建器
     * @param properties   RestTemplate 配置属性
     * @param objectMapper JSON 对象映射器
     * @return RestTemplate 实例
     */
    @Bean("converterRestTemplate")
    public RestTemplate converterRestTemplate(
            RestTemplateBuilder builder,
            RestTemplateProperties properties,
            ObjectMapper objectMapper
    ) {
        String rootUri = StrUtil.blankToDefault(properties.rootUri(), "http://localhost:8081");
        Duration connectTimeout = properties.connectTimeout() == null ? Duration.ofSeconds(3) : properties.connectTimeout();
        Duration readTimeout = properties.readTimeout() == null ? Duration.ofSeconds(10) : properties.readTimeout();

        RestTemplate restTemplate = builder
                .rootUri(rootUri)
                .connectTimeout(connectTimeout)
                .readTimeout(readTimeout)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .additionalInterceptors(restTemplateTraceInterceptor)
                .build();

        this.configureMessageConverters(restTemplate, objectMapper);
        return restTemplate;
    }

    /**
     * 配置消息转换器。
     *
     * @param restTemplate RestTemplate 实例
     * @param objectMapper JSON 对象映射器
     */
    private void configureMessageConverters(RestTemplate restTemplate, ObjectMapper objectMapper) {
        List<MediaType> jsonMediaTypes = new ArrayList<>();
        jsonMediaTypes.add(MediaType.APPLICATION_JSON);
        jsonMediaTypes.add(new MediaType("application", "*+json"));
        // 兼容部分第三方接口错误地使用 text/plain 返回 JSON 的情况
        jsonMediaTypes.add(MediaType.TEXT_PLAIN);

        MappingJackson2HttpMessageConverter jacksonConverter = new MappingJackson2HttpMessageConverter(objectMapper);
        jacksonConverter.setSupportedMediaTypes(jsonMediaTypes);

        StringHttpMessageConverter stringConverter = new StringHttpMessageConverter(StandardCharsets.UTF_8);
        stringConverter.setWriteAcceptCharset(false);

        List<?> converters = restTemplate.getMessageConverters();
        if (CollUtil.isEmpty(converters)) {
            restTemplate.getMessageConverters().add(stringConverter);
            restTemplate.getMessageConverters().add(jacksonConverter);
            return;
        }

        restTemplate.getMessageConverters().removeIf(converter ->
                converter instanceof StringHttpMessageConverter
                        || converter instanceof MappingJackson2HttpMessageConverter
        );

        // String 转换器放前面，优先处理纯文本响应
        restTemplate.getMessageConverters().add(0, stringConverter);
        // JSON 转换器放后面，处理对象序列化和反序列化
        restTemplate.getMessageConverters().add(jacksonConverter);
    }

}
```

如果第三方接口返回的是 JSON，但响应头错误地设置成了 `text/plain`，上面的 `MappingJackson2HttpMessageConverter` 支持 `MediaType.TEXT_PLAIN` 后，可以继续反序列化为 Java 对象。

例如远程响应头如下：

```text
Content-Type: text/plain;charset=UTF-8
```

响应体仍然是 JSON：

```json
{
  "id": 1,
  "username": "ateng",
  "nickname": "Ateng",
  "mobile": "18800000000",
  "status": 1,
  "createdAt": "2026-04-30T10:30:00"
}
```

调用时仍可使用对象接收：

```java
UserResponse user = restTemplate.getForObject("/mock-api/users/{id}", UserResponse.class, 1L);
```

消息转换器配置建议如下：

| 场景                              | 处理方式                                                  |
| --------------------------------- | --------------------------------------------------------- |
| 普通 JSON 请求响应                | 使用默认 Jackson 转换器即可                               |
| 中文字符串乱码                    | 设置 `StringHttpMessageConverter(StandardCharsets.UTF_8)` |
| 第三方接口 `text/plain` 返回 JSON | 给 Jackson 转换器追加 `MediaType.TEXT_PLAIN`              |
| 文件下载                          | 使用 `byte[].class` 或流式处理，不要用 JSON 转换器        |
| XML 接口                          | 按需引入 XML 转换器或手动解析                             |

完成本章配置后，`RestTemplate` 已具备生产项目中常见的超时控制、连接池、请求拦截和消息转换能力。后续“异常处理”章节可以继续在此基础上补充 HTTP 状态码异常、业务异常和统一异常封装。

## 异常处理

这一部分用于说明 `RestTemplate` 调用远程接口时常见的异常类型、HTTP 状态码异常处理方式，以及如何封装统一远程调用异常。默认情况下，`RestTemplate` 会把 4xx 和 5xx HTTP 状态码作为错误处理，并通过错误处理器抛出 `RestClientException` 体系下的异常；如果需要自定义判断逻辑，可以注册 `ResponseErrorHandler`。([Home](https://docs.spring.io/spring-framework/reference/6.2-SNAPSHOT/integration/rest-clients.html?utm_source=chatgpt.com))

本节内容继续基于当前文档大纲展开。

### 常见异常类型

`RestTemplate` 的异常主要分为三类：HTTP 状态码异常、网络访问异常和客户端调用异常。HTTP 状态码异常通常表示远程服务已经返回响应，但状态码是 4xx 或 5xx；网络访问异常通常表示请求没有成功到达远程服务，或者读取响应时发生 I/O 问题。

常见异常如下：

| 异常类型                         | 典型场景                     | 处理建议                                           |
| -------------------------------- | ---------------------------- | -------------------------------------------------- |
| `RestClientException`            | `RestTemplate` 异常基类      | 兜底捕获                                           |
| `RestClientResponseException`    | 远程服务已返回 HTTP 响应     | 可读取状态码、响应头、响应体                       |
| `HttpClientErrorException`       | 4xx 客户端错误               | 按业务区分参数错误、认证失败、权限不足、资源不存在 |
| `HttpServerErrorException`       | 5xx 服务端错误               | 通常记录日志、告警、重试或熔断                     |
| `ResourceAccessException`        | I/O 异常、连接失败、读取超时 | 重点排查网络、域名、端口、超时配置                 |
| `UnknownHttpStatusCodeException` | 未知 HTTP 状态码             | 记录原始响应并兜底处理                             |

`RestClientResponseException` 是包含实际 HTTP 响应数据的通用异常基类，可以获取状态码、响应头和响应体；`ResourceAccessException` 通常用于表示 I/O 访问异常。([Home](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/client/RestClientResponseException.html?utm_source=chatgpt.com))

文件位置：

```text
src/main/java/io/github/atengk/remote/client/UserExceptionApiClient.java
```

文件位置：`src/main/java/io/github/atengk/remote/client/UserExceptionApiClient.java`

下面的客户端类演示直接在调用处捕获 `RestTemplate` 常见异常。

```java
package io.github.atengk.remote.client;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.remote.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

/**
 * 用户远程接口异常示例客户端
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserExceptionApiClient {

    private final RestTemplate restTemplate;

    /**
     * 根据用户ID查询用户，并处理常见远程调用异常。
     *
     * @param userId 用户ID
     * @return 用户响应数据
     */
    public UserResponse getUserWithExceptionHandle(Long userId) {
        if (ObjUtil.isNull(userId)) {
            log.warn("查询远程用户失败，用户ID为空");
            return null;
        }

        try {
            log.info("开始查询远程用户，userId={}", userId);
            return restTemplate.getForObject("/mock-api/users/{id}", UserResponse.class, userId);
        } catch (HttpClientErrorException ex) {
            log.warn("远程接口客户端错误，userId={}, statusCode={}, responseBody={}",
                    userId, ex.getStatusCode(), limitResponseBody(ex.getResponseBodyAsString()));
            return null;
        } catch (HttpServerErrorException ex) {
            log.error("远程接口服务端错误，userId={}, statusCode={}, responseBody={}",
                    userId, ex.getStatusCode(), limitResponseBody(ex.getResponseBodyAsString()));
            return null;
        } catch (ResourceAccessException ex) {
            log.error("远程接口访问失败，userId={}, error={}", userId, ex.getMessage());
            return null;
        } catch (RestClientResponseException ex) {
            log.error("远程接口响应异常，userId={}, statusCode={}, responseBody={}",
                    userId, ex.getStatusCode(), limitResponseBody(ex.getResponseBodyAsString()));
            return null;
        }
    }

    /**
     * 判断 HTTP 状态码是否为成功状态。
     *
     * @param statusCode HTTP 状态码
     * @return 是否成功
     */
    private boolean isSuccessStatus(HttpStatusCode statusCode) {
        return ObjUtil.isNotNull(statusCode) && statusCode.is2xxSuccessful();
    }

    /**
     * 限制响应体日志长度，避免异常报文过大。
     *
     * @param responseBody 响应体
     * @return 截断后的响应体
     */
    private String limitResponseBody(String responseBody) {
        if (StrUtil.isBlank(responseBody)) {
            return "";
        }
        return StrUtil.maxLength(responseBody, 1000);
    }

}
```

调用示例：

```java
UserResponse user = userExceptionApiClient.getUserWithExceptionHandle(1L);
```

对应 HTTP 请求如下：

```bash
curl -X GET 'http://localhost:8081/mock-api/users/1'
```

直接在调用处 `try-catch` 的优点是处理逻辑清晰，适合少量远程接口；缺点是多个接口会重复编写异常判断、日志记录和响应体截断逻辑。项目中远程调用较多时，建议使用统一异常封装。

### HTTP 状态码异常处理

HTTP 状态码异常处理可以分为两种方式：在每个调用方法中捕获 `RestClientResponseException`，或者为 `RestTemplate` 注册统一的 `ResponseErrorHandler`。`ResponseErrorHandler` 会先判断响应是否错误，只有 `hasError` 返回 `true` 时，才会调用 `handleError` 处理错误响应。([Home](https://docs.spring.io/spring-framework/docs/6.2.12/javadoc-api/org/springframework/web/client/ResponseErrorHandler.html?utm_source=chatgpt.com))

常见 HTTP 状态码处理策略如下：

| 状态码                | 含义             | 建议处理                               |
| --------------------- | ---------------- | -------------------------------------- |
| `400`                 | 请求参数错误     | 记录请求参数，提示调用方修正           |
| `401`                 | 未认证           | 检查 Token、签名、认证配置             |
| `403`                 | 无权限           | 检查接口权限、租户、角色               |
| `404`                 | 资源不存在       | 可按业务返回空结果或抛出资源不存在异常 |
| `409`                 | 数据冲突         | 按业务提示重复提交、状态冲突           |
| `429`                 | 请求过多         | 限流场景，建议退避重试                 |
| `500`                 | 服务端异常       | 记录日志，必要时告警                   |
| `502` / `503` / `504` | 网关或服务不可用 | 可结合重试、熔断、降级处理             |

文件位置：

```text
src/main/java/io/github/atengk/remote/exception/RemoteCallException.java
src/main/java/io/github/atengk/remote/handler/RestTemplateResponseErrorHandler.java
src/main/java/io/github/atengk/config/RestTemplateErrorHandlerConfig.java
```

文件位置：`src/main/java/io/github/atengk/remote/exception/RemoteCallException.java`

下面的异常类用于承接远程 HTTP 状态码、响应体和错误信息。

```java
package io.github.atengk.remote.exception;

import lombok.Getter;
import org.springframework.http.HttpStatusCode;

/**
 * 远程调用异常
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Getter
public class RemoteCallException extends RuntimeException {

    private final HttpStatusCode statusCode;

    private final String responseBody;

    /**
     * 创建远程调用异常。
     *
     * @param message      异常消息
     * @param statusCode   HTTP 状态码
     * @param responseBody 响应体
     */
    public RemoteCallException(String message, HttpStatusCode statusCode, String responseBody) {
        super(message);
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

    /**
     * 创建远程调用异常。
     *
     * @param message      异常消息
     * @param statusCode   HTTP 状态码
     * @param responseBody 响应体
     * @param cause        原始异常
     */
    public RemoteCallException(String message, HttpStatusCode statusCode, String responseBody, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

}
```

文件位置：`src/main/java/io/github/atengk/remote/handler/RestTemplateResponseErrorHandler.java`

下面的错误处理器用于统一处理 4xx 和 5xx 响应，并转换为自定义 `RemoteCallException`。

```java
package io.github.atengk.remote.handler;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.remote.exception.RemoteCallException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.ResponseErrorHandler;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

/**
 * RestTemplate 统一错误处理器
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
public class RestTemplateResponseErrorHandler implements ResponseErrorHandler {

    /**
     * 判断响应是否为错误响应。
     *
     * @param response HTTP 响应
     * @return 是否错误响应
     * @throws IOException IO异常
     */
    @Override
    public boolean hasError(ClientHttpResponse response) throws IOException {
        return response.getStatusCode().isError();
    }

    /**
     * 处理错误响应。
     *
     * @param url      请求地址
     * @param method   请求方法
     * @param response HTTP 响应
     * @throws IOException IO异常
     */
    @Override
    public void handleError(URI url, HttpMethod method, ClientHttpResponse response) throws IOException {
        HttpStatusCode statusCode = response.getStatusCode();
        String responseBody = StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8);
        String limitedBody = StrUtil.maxLength(StrUtil.blankToDefault(responseBody, ""), 1000);

        log.warn("远程接口返回异常状态，method={}, url={}, statusCode={}, responseBody={}",
                method, url, statusCode, limitedBody);

        String message = buildErrorMessage(statusCode, limitedBody);
        throw new RemoteCallException(message, statusCode, limitedBody);
    }

    /**
     * 构建异常消息。
     *
     * @param statusCode   HTTP 状态码
     * @param responseBody 响应体
     * @return 异常消息
     */
    private String buildErrorMessage(HttpStatusCode statusCode, String responseBody) {
        if (statusCode.is4xxClientError()) {
            return StrUtil.format("远程接口客户端错误，statusCode={}，responseBody={}", statusCode, responseBody);
        }
        if (statusCode.is5xxServerError()) {
            return StrUtil.format("远程接口服务端错误，statusCode={}，responseBody={}", statusCode, responseBody);
        }
        return StrUtil.format("远程接口状态异常，statusCode={}，responseBody={}", statusCode, responseBody);
    }

}
```

文件位置：`src/main/java/io/github/atengk/config/RestTemplateErrorHandlerConfig.java`

下面的配置类把自定义错误处理器注册到 `RestTemplate` 中。

```java
package io.github.atengk.config;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.config.properties.RestTemplateProperties;
import io.github.atengk.remote.handler.RestTemplateResponseErrorHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * RestTemplate 异常处理配置
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(RestTemplateProperties.class)
public class RestTemplateErrorHandlerConfig {

    private final RestTemplateResponseErrorHandler responseErrorHandler;

    /**
     * 创建带统一错误处理器的 RestTemplate Bean。
     *
     * @param builder    RestTemplate 构建器
     * @param properties RestTemplate 配置属性
     * @return RestTemplate 实例
     */
    @Bean("errorHandlerRestTemplate")
    public RestTemplate errorHandlerRestTemplate(RestTemplateBuilder builder, RestTemplateProperties properties) {
        String rootUri = StrUtil.blankToDefault(properties.rootUri(), "http://localhost:8081");
        Duration connectTimeout = properties.connectTimeout() == null ? Duration.ofSeconds(3) : properties.connectTimeout();
        Duration readTimeout = properties.readTimeout() == null ? Duration.ofSeconds(10) : properties.readTimeout();

        return builder
                .rootUri(rootUri)
                .connectTimeout(connectTimeout)
                .readTimeout(readTimeout)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                // 注册统一 HTTP 状态码错误处理器
                .errorHandler(responseErrorHandler)
                .build();
    }

}
```

使用统一错误处理器后，业务调用方法可以只捕获自定义异常。

```java
package io.github.atengk.remote.client;

import cn.hutool.core.util.ObjUtil;
import io.github.atengk.remote.dto.UserResponse;
import io.github.atengk.remote.exception.RemoteCallException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

/**
 * 用户远程接口统一错误处理客户端
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserErrorHandlerApiClient {

    @Qualifier("errorHandlerRestTemplate")
    private final RestTemplate restTemplate;

    /**
     * 根据用户ID查询用户。
     *
     * @param userId 用户ID
     * @return 用户响应数据
     */
    public UserResponse getUser(Long userId) {
        if (ObjUtil.isNull(userId)) {
            log.warn("查询远程用户失败，用户ID为空");
            return null;
        }

        try {
            log.info("开始查询远程用户，userId={}", userId);
            return restTemplate.getForObject("/mock-api/users/{id}", UserResponse.class, userId);
        } catch (RemoteCallException ex) {
            log.warn("远程接口返回异常状态，userId={}, statusCode={}, responseBody={}",
                    userId, ex.getStatusCode(), ex.getResponseBody());
            return null;
        } catch (ResourceAccessException ex) {
            log.error("远程接口访问失败，userId={}, error={}", userId, ex.getMessage());
            return null;
        }
    }

}
```

对应验证命令如下：

```bash
# 正常查询
curl -X GET 'http://localhost:8081/mock-api/users/1'

# 模拟资源不存在
curl -X GET 'http://localhost:8081/mock-api/users/999999'

# 模拟服务端异常，具体路径按 Mock 服务实现调整
curl -X GET 'http://localhost:8081/mock-api/users/error'
```

如果远程接口返回 `404`，日志示例：

```text
远程接口返回异常状态，method=GET, url=http://localhost:8081/mock-api/users/999999, statusCode=404 NOT_FOUND, responseBody={"code":404,"message":"用户不存在"}
远程接口返回异常状态，userId=999999, statusCode=404 NOT_FOUND, responseBody={"code":404,"message":"用户不存在"}
```

### 统一异常封装

统一异常封装用于把 `RestTemplate` 的原始异常转换成项目内部可识别的异常结构，避免业务层直接依赖 `HttpClientErrorException`、`HttpServerErrorException`、`ResourceAccessException` 等底层异常。

统一封装建议遵循下面原则：

| 原则                     | 说明                                    |
| ------------------------ | --------------------------------------- |
| 保留状态码               | HTTP 状态码有助于判断错误类型           |
| 保留响应体               | 第三方接口错误原因通常在响应体中        |
| 响应体限制长度           | 避免日志过大或输出敏感信息              |
| 区分 HTTP 异常和网络异常 | 4xx/5xx 与超时、连接失败不是一类问题    |
| 对外返回统一业务异常     | 不把底层客户端异常直接暴露给 Controller |

文件位置：

```text
src/main/java/io/github/atengk/remote/exception/RemoteAccessException.java
src/main/java/io/github/atengk/remote/support/RestTemplateExceptionTranslator.java
src/main/java/io/github/atengk/remote/client/UserUnifiedExceptionApiClient.java
```

文件位置：`src/main/java/io/github/atengk/remote/exception/RemoteAccessException.java`

下面的异常类用于表示连接失败、读取超时、DNS 异常等网络访问问题。

```java
package io.github.atengk.remote.exception;

import lombok.Getter;

/**
 * 远程访问异常
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Getter
public class RemoteAccessException extends RuntimeException {

    private final String remoteService;

    /**
     * 创建远程访问异常。
     *
     * @param message       异常消息
     * @param remoteService 远程服务标识
     * @param cause         原始异常
     */
    public RemoteAccessException(String message, String remoteService, Throwable cause) {
        super(message, cause);
        this.remoteService = remoteService;
    }

}
```

文件位置：`src/main/java/io/github/atengk/remote/support/RestTemplateExceptionTranslator.java`

下面的转换器用于把 `RestTemplate` 原始异常统一转换成项目自定义异常。

```java
package io.github.atengk.remote.support;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.remote.exception.RemoteAccessException;
import io.github.atengk.remote.exception.RemoteCallException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

/**
 * RestTemplate 异常转换器
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
public class RestTemplateExceptionTranslator {

    /**
     * 转换 RestTemplate 异常。
     *
     * @param remoteService 远程服务标识
     * @param exception     原始异常
     * @return 统一异常
     */
    public RuntimeException translate(String remoteService, RuntimeException exception) {
        if (exception instanceof RemoteCallException remoteCallException) {
            return remoteCallException;
        }

        if (exception instanceof RestClientResponseException responseException) {
            HttpStatusCode statusCode = responseException.getStatusCode();
            String responseBody = limitResponseBody(responseException.getResponseBodyAsString());

            log.warn("远程接口响应异常，remoteService={}, statusCode={}, responseBody={}",
                    remoteService, statusCode, responseBody);

            return new RemoteCallException(
                    StrUtil.format("远程接口响应异常，remoteService={}，statusCode={}", remoteService, statusCode),
                    statusCode,
                    responseBody,
                    responseException
            );
        }

        if (exception instanceof ResourceAccessException resourceAccessException) {
            log.error("远程接口访问异常，remoteService={}, error={}",
                    remoteService, resourceAccessException.getMessage());

            return new RemoteAccessException(
                    StrUtil.format("远程接口访问异常，remoteService={}", remoteService),
                    remoteService,
                    resourceAccessException
            );
        }

        log.error("远程接口未知异常，remoteService={}, error={}", remoteService, exception.getMessage());

        return new RemoteAccessException(
                StrUtil.format("远程接口未知异常，remoteService={}", remoteService),
                remoteService,
                exception
        );
    }

    /**
     * 限制响应体长度。
     *
     * @param responseBody 响应体
     * @return 截断后的响应体
     */
    private String limitResponseBody(String responseBody) {
        if (StrUtil.isBlank(responseBody)) {
            return "";
        }
        return StrUtil.maxLength(responseBody, 1000);
    }

}
```

文件位置：`src/main/java/io/github/atengk/remote/client/UserUnifiedExceptionApiClient.java`

下面的客户端类演示在远程接口调用层统一转换异常，再由上层业务决定返回默认值、继续抛出或触发降级逻辑。

```java
package io.github.atengk.remote.client;

import cn.hutool.core.util.ObjUtil;
import io.github.atengk.remote.dto.UserResponse;
import io.github.atengk.remote.support.RestTemplateExceptionTranslator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * 用户远程接口统一异常封装客户端
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserUnifiedExceptionApiClient {

    private static final String REMOTE_SERVICE = "user-service";

    private final RestTemplate restTemplate;

    private final RestTemplateExceptionTranslator exceptionTranslator;

    /**
     * 查询用户，异常统一转换后继续抛出。
     *
     * @param userId 用户ID
     * @return 用户响应数据
     */
    public UserResponse getUserOrThrow(Long userId) {
        if (ObjUtil.isNull(userId)) {
            log.warn("查询远程用户失败，用户ID为空");
            return null;
        }

        try {
            log.info("开始查询远程用户，userId={}", userId);
            return restTemplate.getForObject("/mock-api/users/{id}", UserResponse.class, userId);
        } catch (RestClientException ex) {
            throw exceptionTranslator.translate(REMOTE_SERVICE, ex);
        }
    }

    /**
     * 查询用户，异常时返回空结果。
     *
     * @param userId 用户ID
     * @return 用户响应数据
     */
    public UserResponse getUserOrNull(Long userId) {
        try {
            return this.getUserOrThrow(userId);
        } catch (RuntimeException ex) {
            log.warn("查询远程用户失败，返回空结果，userId={}, error={}", userId, ex.getMessage());
            return null;
        }
    }

}
```

业务层调用示例：

```java
UserResponse user = userUnifiedExceptionApiClient.getUserOrNull(1L);
```

如果业务希望把远程异常继续向上抛给统一异常处理器，可以调用：

```java
UserResponse user = userUnifiedExceptionApiClient.getUserOrThrow(1L);
```

统一异常封装后的处理流程如下：

```text
业务 Service
  -> UserUnifiedExceptionApiClient
    -> RestTemplate 调用远程接口
      -> 4xx / 5xx 响应
        -> RestClientResponseException 或 RemoteCallException
      -> 连接失败 / 读取超时
        -> ResourceAccessException
    -> RestTemplateExceptionTranslator 转换异常
  -> 业务层决定返回空值、降级、重试或继续抛出
```

如果项目已有全局异常处理器，可以把 `RemoteCallException` 和 `RemoteAccessException` 转换成统一响应。

文件位置：`src/main/java/io/github/atengk/common/handler/GlobalExceptionHandler.java`

下面的异常处理器用于把远程调用异常转换成统一接口响应。

```java
package io.github.atengk.common.handler;

import cn.hutool.core.util.ObjUtil;
import io.github.atengk.remote.exception.RemoteAccessException;
import io.github.atengk.remote.exception.RemoteCallException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * 全局异常处理器
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理远程 HTTP 状态异常。
     *
     * @param exception 远程调用异常
     * @return 统一响应
     */
    @ExceptionHandler(RemoteCallException.class)
    public Map<String, Object> handleRemoteCallException(RemoteCallException exception) {
        HttpStatusCode statusCode = exception.getStatusCode();
        Integer code = ObjUtil.isNull(statusCode) ? 500 : statusCode.value();

        log.warn("远程调用HTTP状态异常，statusCode={}, responseBody={}",
                statusCode, exception.getResponseBody());

        return Map.of(
                "code", code,
                "message", "远程服务返回异常状态",
                "data", Map.of(
                        "statusCode", code,
                        "responseBody", exception.getResponseBody()
                )
        );
    }

    /**
     * 处理远程访问异常。
     *
     * @param exception 远程访问异常
     * @return 统一响应
     */
    @ExceptionHandler(RemoteAccessException.class)
    public Map<String, Object> handleRemoteAccessException(RemoteAccessException exception) {
        log.error("远程服务访问异常，remoteService={}, error={}",
                exception.getRemoteService(), exception.getMessage());

        return Map.of(
                "code", 503,
                "message", "远程服务暂时不可用",
                "data", Map.of(
                        "remoteService", exception.getRemoteService()
                )
        );
    }

}
```

统一异常封装建议如下：

| 建议                       | 说明                                    |
| -------------------------- | --------------------------------------- |
| 客户端层统一转换异常       | 避免业务层感知过多底层异常              |
| 区分远程状态异常和访问异常 | 4xx/5xx 与超时、连接失败处理方式不同    |
| 保留远程响应体             | 方便定位第三方接口返回的业务错误        |
| 生产环境限制日志长度       | 防止异常报文过大                        |
| 敏感字段不要完整打印       | Token、密码、手机号、身份证号等需要脱敏 |
| 是否返回空值由业务决定     | 基础客户端不应擅自吞掉所有异常          |

到这里，`RestTemplate` 的常见异常类型、HTTP 状态码异常处理和统一异常封装已经完成。实际项目中建议优先采用“统一错误处理器 + 异常转换器 + 全局异常处理器”的组合：`ResponseErrorHandler` 负责识别 HTTP 错误响应，异常转换器负责归一化底层异常，全局异常处理器负责对外输出统一响应。

## 实战封装

这一部分用于把前面章节中的 `RestTemplate` 基础调用、参数处理、响应处理和异常处理整合成一个可复用的 HTTP 调用封装。实际项目中不建议在业务代码里直接散落大量 `restTemplate.exchange(...)`，而是建议统一封装请求构建、Header 处理、日志记录、响应解析和异常转换逻辑。当前章节继续基于你上传的大纲展开。

### 通用 HTTP 工具类设计

通用 HTTP 工具类的目标是把 GET、POST、PUT、DELETE 等常见调用方式统一起来，让业务代码只关心接口路径、请求参数、请求体和响应类型。封装后可以减少重复代码，也方便后续统一接入日志、TraceId、Token、租户信息、异常转换和响应解析。

推荐封装边界如下：

| 封装内容 | 说明                                      |
| -------- | ----------------------------------------- |
| 请求路径 | 支持相对路径，例如 `/mock-api/users/{id}` |
| 路径参数 | 支持 `PathVariable` 参数                  |
| 查询参数 | 支持 `Query` 参数                         |
| 请求头   | 支持自定义 Header                         |
| 请求体   | 支持 JSON Body                            |
| 响应类型 | 支持普通对象和泛型对象                    |
| 异常处理 | 交给前面章节中的统一异常转换器处理        |

文件位置：

```text
src/main/java/io/github/atengk/common/http/HttpRequestOptions.java
src/main/java/io/github/atengk/common/http/RestTemplateHttpClient.java
```

文件位置：`src/main/java/io/github/atengk/common/http/HttpRequestOptions.java`

下面的请求选项类用于统一承载路径参数、查询参数和请求头。

```java
package io.github.atengk.common.http;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpHeaders;

import java.util.Map;

/**
 * HTTP 请求选项
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HttpRequestOptions {

    /**
     * PathVariable 参数，例如 /users/{id} 中的 id。
     */
    private Map<String, Object> pathVariables;

    /**
     * Query 参数，例如 ?keyword=Ateng&pageNum=1。
     */
    private Map<String, Object> queryParams;

    /**
     * 自定义请求头。
     */
    private HttpHeaders headers;

    /**
     * 创建空请求选项。
     *
     * @return 空请求选项
     */
    public static HttpRequestOptions empty() {
        return new HttpRequestOptions();
    }

}
```

文件位置：`src/main/java/io/github/atengk/common/http/RestTemplateHttpClient.java`

下面的通用客户端封装了 GET、POST、PUT、DELETE 和泛型 `exchange` 调用。

```java
package io.github.atengk.common.http;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import io.github.atengk.remote.support.RestTemplateExceptionTranslator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * RestTemplate 通用 HTTP 客户端
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RestTemplateHttpClient {

    private static final String REMOTE_SERVICE = "default-remote-service";

    private final RestTemplate restTemplate;

    private final RestTemplateExceptionTranslator exceptionTranslator;

    /**
     * 发送 GET 请求。
     *
     * @param path         请求路径
     * @param options      请求选项
     * @param responseType 响应类型
     * @param <T>          响应数据类型
     * @return 响应数据
     */
    public <T> T get(String path, HttpRequestOptions options, Class<T> responseType) {
        return this.exchange(path, HttpMethod.GET, null, options, responseType);
    }

    /**
     * 发送 POST 请求。
     *
     * @param path         请求路径
     * @param requestBody  请求体
     * @param options      请求选项
     * @param responseType 响应类型
     * @param <T>          响应数据类型
     * @return 响应数据
     */
    public <T> T post(String path, Object requestBody, HttpRequestOptions options, Class<T> responseType) {
        return this.exchange(path, HttpMethod.POST, requestBody, options, responseType);
    }

    /**
     * 发送 PUT 请求。
     *
     * @param path         请求路径
     * @param requestBody  请求体
     * @param options      请求选项
     * @param responseType 响应类型
     * @param <T>          响应数据类型
     * @return 响应数据
     */
    public <T> T put(String path, Object requestBody, HttpRequestOptions options, Class<T> responseType) {
        return this.exchange(path, HttpMethod.PUT, requestBody, options, responseType);
    }

    /**
     * 发送 DELETE 请求。
     *
     * @param path         请求路径
     * @param options      请求选项
     * @param responseType 响应类型
     * @param <T>          响应数据类型
     * @return 响应数据
     */
    public <T> T delete(String path, HttpRequestOptions options, Class<T> responseType) {
        return this.exchange(path, HttpMethod.DELETE, null, options, responseType);
    }

    /**
     * 发送普通响应类型请求。
     *
     * @param path         请求路径
     * @param method       请求方法
     * @param requestBody  请求体
     * @param options      请求选项
     * @param responseType 响应类型
     * @param <T>          响应数据类型
     * @return 响应数据
     */
    public <T> T exchange(
            String path,
            HttpMethod method,
            Object requestBody,
            HttpRequestOptions options,
            Class<T> responseType
    ) {
        try {
            HttpRequestOptions currentOptions = ObjUtil.defaultIfNull(options, HttpRequestOptions.empty());
            String url = this.buildUrl(path, currentOptions.getQueryParams());
            HttpEntity<Object> httpEntity = this.buildHttpEntity(requestBody, currentOptions.getHeaders());

            log.info("开始发送远程请求，method={}, url={}", method, url);
            return restTemplate.exchange(
                    url,
                    method,
                    httpEntity,
                    responseType,
                    this.defaultPathVariables(currentOptions.getPathVariables())
            ).getBody();
        } catch (RestClientException ex) {
            throw exceptionTranslator.translate(REMOTE_SERVICE, ex);
        }
    }

    /**
     * 发送泛型响应类型请求。
     *
     * @param path         请求路径
     * @param method       请求方法
     * @param requestBody  请求体
     * @param options      请求选项
     * @param responseType 泛型响应类型
     * @param <T>          响应数据类型
     * @return 响应数据
     */
    public <T> T exchange(
            String path,
            HttpMethod method,
            Object requestBody,
            HttpRequestOptions options,
            ParameterizedTypeReference<T> responseType
    ) {
        try {
            HttpRequestOptions currentOptions = ObjUtil.defaultIfNull(options, HttpRequestOptions.empty());
            String url = this.buildUrl(path, currentOptions.getQueryParams());
            HttpEntity<Object> httpEntity = this.buildHttpEntity(requestBody, currentOptions.getHeaders());

            log.info("开始发送远程泛型请求，method={}, url={}", method, url);
            return restTemplate.exchange(
                    url,
                    method,
                    httpEntity,
                    responseType,
                    this.defaultPathVariables(currentOptions.getPathVariables())
            ).getBody();
        } catch (RestClientException ex) {
            throw exceptionTranslator.translate(REMOTE_SERVICE, ex);
        }
    }

    /**
     * 构建请求地址。
     *
     * @param path        请求路径
     * @param queryParams 查询参数
     * @return 请求地址
     */
    private String buildUrl(String path, Map<String, Object> queryParams) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath(path);

        if (CollUtil.isNotEmpty(queryParams)) {
            queryParams.forEach((key, value) -> {
                if (ObjUtil.isNull(value)) {
                    return;
                }
                if (value instanceof Collection<?> collection) {
                    builder.queryParam(key, collection.toArray());
                    return;
                }
                builder.queryParam(key, value);
            });
        }

        return builder.build().encode().toUriString();
    }

    /**
     * 构建 HTTP 请求实体。
     *
     * @param requestBody 请求体
     * @param headers     请求头
     * @return HTTP 请求实体
     */
    private HttpEntity<Object> buildHttpEntity(Object requestBody, HttpHeaders headers) {
        HttpHeaders currentHeaders = new HttpHeaders();

        if (ObjUtil.isNotNull(headers)) {
            currentHeaders.addAll(headers);
        }

        if (!currentHeaders.containsKey(HttpHeaders.ACCEPT)) {
            currentHeaders.setAccept(List.of(MediaType.APPLICATION_JSON));
        }

        if (ObjUtil.isNotNull(requestBody) && !currentHeaders.containsKey(HttpHeaders.CONTENT_TYPE)) {
            currentHeaders.setContentType(MediaType.APPLICATION_JSON);
        }

        return new HttpEntity<>(requestBody, currentHeaders);
    }

    /**
     * 获取默认路径参数。
     *
     * @param pathVariables 路径参数
     * @return 路径参数
     */
    private Map<String, Object> defaultPathVariables(Map<String, Object> pathVariables) {
        if (CollUtil.isEmpty(pathVariables)) {
            return Map.of();
        }
        return pathVariables;
    }

}
```

使用示例：

```java
HttpHeaders headers = new HttpHeaders();
headers.setBearerAuth("access-token-value");

HttpRequestOptions options = HttpRequestOptions.builder()
        .pathVariables(Map.of("id", 1L))
        .queryParams(Map.of("includeDetail", true))
        .headers(headers)
        .build();

UserResponse user = restTemplateHttpClient.get(
        "/mock-api/users/{id}",
        options,
        UserResponse.class
);
```

对应请求效果如下：

```bash
curl -X GET 'http://localhost:8081/mock-api/users/1?includeDetail=true' \
  -H 'Authorization: Bearer access-token-value' \
  -H 'Accept: application/json'
```

泛型响应调用示例：

```java
ApiResult<PageResponse<UserResponse>> result = restTemplateHttpClient.exchange(
        "/mock-api/result/users/page",
        HttpMethod.GET,
        null,
        HttpRequestOptions.builder()
                .queryParams(Map.of("pageNum", 1, "pageSize", 10))
                .build(),
        new ParameterizedTypeReference<>() {
        }
);
```

通用 HTTP 工具类封装建议如下：

| 建议                                          | 说明                                   |
| --------------------------------------------- | -------------------------------------- |
| 业务层不要直接使用 `RestTemplate`             | 统一通过 `RestTemplateHttpClient` 调用 |
| 请求参数统一放入 `HttpRequestOptions`         | 避免方法参数过多                       |
| 泛型响应统一使用 `ParameterizedTypeReference` | 避免 `List<LinkedHashMap>` 问题        |
| 异常统一交给转换器                            | 避免每个方法重复捕获底层异常           |
| 不在工具类中写具体业务判断                    | 工具类只负责 HTTP 层能力               |

### 请求日志记录

请求日志记录用于观察远程调用的请求方法、请求地址、请求耗时、响应状态和链路追踪 ID。生产环境中建议至少记录 method、uri、statusCode、costMs、traceId；请求体和响应体要谨慎记录，避免泄露密码、Token、手机号、身份证号等敏感信息。

如果需要在拦截器中读取响应体，建议配合 `BufferingClientHttpRequestFactory` 使用，否则响应流可能被拦截器提前读取，导致后续消息转换器无法再次读取。

文件位置：

```text
src/main/java/io/github/atengk/common/http/interceptor/RestTemplateAccessLogInterceptor.java
src/main/java/io/github/atengk/config/RestTemplateLogConfig.java
```

文件位置：`src/main/java/io/github/atengk/common/http/interceptor/RestTemplateAccessLogInterceptor.java`

下面的拦截器用于记录远程 HTTP 请求访问日志。

```java
package io.github.atengk.common.http.interceptor;

import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * RestTemplate 访问日志拦截器
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
public class RestTemplateAccessLogInterceptor implements ClientHttpRequestInterceptor {

    private static final String TRACE_ID_HEADER = "X-Trace-Id";

    /**
     * 拦截并记录远程请求日志。
     *
     * @param request   HTTP 请求
     * @param body      请求体
     * @param execution 请求执行器
     * @return HTTP 响应
     * @throws IOException IO异常
     */
    @Override
    public ClientHttpResponse intercept(
            HttpRequest request,
            byte[] body,
            ClientHttpRequestExecution execution
    ) throws IOException {
        String traceId = this.getOrCreateTraceId(request.getHeaders());
        TimeInterval timer = new TimeInterval();

        try {
            log.info("远程请求开始，method={}, uri={}, traceId={}",
                    request.getMethod(), request.getURI(), traceId);

            ClientHttpResponse response = execution.execute(request, body);

            log.info("远程请求完成，method={}, uri={}, statusCode={}, costMs={}, traceId={}",
                    request.getMethod(), request.getURI(), response.getStatusCode(), timer.interval(), traceId);

            return response;
        } catch (IOException ex) {
            log.warn("远程请求异常，method={}, uri={}, costMs={}, traceId={}, error={}",
                    request.getMethod(), request.getURI(), timer.interval(), traceId, ex.getMessage());
            throw ex;
        }
    }

    /**
     * 获取或创建链路追踪ID。
     *
     * @param headers 请求头
     * @return 链路追踪ID
     */
    private String getOrCreateTraceId(HttpHeaders headers) {
        String traceId = headers.getFirst(TRACE_ID_HEADER);
        if (StrUtil.isBlank(traceId)) {
            traceId = IdUtil.fastSimpleUUID();
            headers.set(TRACE_ID_HEADER, traceId);
        }
        return traceId;
    }

}
```

文件位置：`src/main/java/io/github/atengk/config/RestTemplateLogConfig.java`

下面的配置类用于注册访问日志拦截器，并使用缓冲请求工厂支持多次读取响应体。

```java
package io.github.atengk.config;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.common.http.interceptor.RestTemplateAccessLogInterceptor;
import io.github.atengk.config.properties.RestTemplateProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * RestTemplate 请求日志配置
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(RestTemplateProperties.class)
public class RestTemplateLogConfig {

    private final RestTemplateAccessLogInterceptor accessLogInterceptor;

    /**
     * 创建带访问日志的 RestTemplate Bean。
     *
     * @param builder    RestTemplate 构建器
     * @param properties RestTemplate 配置属性
     * @return RestTemplate 实例
     */
    @Bean("logRestTemplate")
    public RestTemplate logRestTemplate(RestTemplateBuilder builder, RestTemplateProperties properties) {
        String rootUri = StrUtil.blankToDefault(properties.rootUri(), "http://localhost:8081");
        Duration connectTimeout = properties.connectTimeout() == null ? Duration.ofSeconds(3) : properties.connectTimeout();
        Duration readTimeout = properties.readTimeout() == null ? Duration.ofSeconds(10) : properties.readTimeout();

        SimpleClientHttpRequestFactory simpleFactory = new SimpleClientHttpRequestFactory();
        simpleFactory.setConnectTimeout(connectTimeout);
        simpleFactory.setReadTimeout(readTimeout);

        return builder
                .rootUri(rootUri)
                // 使用缓冲工厂，便于拦截器扩展读取响应体
                .requestFactory(() -> new BufferingClientHttpRequestFactory(simpleFactory))
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .additionalInterceptors(accessLogInterceptor)
                .build();
    }

}
```

日志输出示例：

```text
远程请求开始，method=GET, uri=http://localhost:8081/mock-api/users/1, traceId=9cb9b77e0cef48359735a89fb6e52c89
远程请求完成，method=GET, uri=http://localhost:8081/mock-api/users/1, statusCode=200 OK, costMs=42, traceId=9cb9b77e0cef48359735a89fb6e52c89
```

请求日志记录建议如下：

| 建议                    | 说明                   |
| ----------------------- | ---------------------- |
| 默认记录请求方法和地址  | 便于定位调用了哪个接口 |
| 默认记录耗时            | 便于发现慢接口         |
| 默认记录 TraceId        | 便于跨服务排查         |
| 谨慎记录请求体和响应体  | 避免敏感信息泄露       |
| 响应体日志限制长度      | 防止大报文刷爆日志     |
| 文件上传下载不记录 Body | 避免内存占用和日志膨胀 |

如果确实需要记录请求体，建议只在开发环境开启，并对敏感字段脱敏。

### 统一响应解析

统一响应解析用于处理远程服务常见的包装结构，例如 `{"code":200,"message":"success","data":{...}}`。如果每个业务方法都手动判断 `code`、`message`、`data`，代码会快速膨胀。推荐把响应解析封装成独立组件，由它负责判断空响应、业务状态码和默认值。

文件位置：

```text
src/main/java/io/github/atengk/common/http/RemoteResponseParser.java
src/main/java/io/github/atengk/remote/client/UserWrappedApiClient.java
```

文件位置：`src/main/java/io/github/atengk/common/http/RemoteResponseParser.java`

下面的解析器用于解析统一响应结构，并在业务失败时抛出自定义远程调用异常。

```java
package io.github.atengk.common.http;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.remote.dto.ApiResult;
import io.github.atengk.remote.exception.RemoteCallException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;

/**
 * 远程统一响应解析器
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
public class RemoteResponseParser {

    /**
     * 解析统一响应数据。
     *
     * @param result 统一响应
     * @param <T>    数据类型
     * @return 响应数据
     */
    public <T> T parse(ApiResult<T> result) {
        if (ObjUtil.isNull(result)) {
            log.warn("远程接口响应为空");
            throw new RemoteCallException("远程接口响应为空", HttpStatusCode.valueOf(500), "");
        }

        if (!this.isSuccess(result)) {
            String message = StrUtil.blankToDefault(result.message(), "远程接口业务处理失败");
            log.warn("远程接口业务失败，code={}, message={}", result.code(), message);
            throw new RemoteCallException(message, HttpStatusCode.valueOf(200), "");
        }

        return result.data();
    }

    /**
     * 解析统一响应数据，失败时返回默认值。
     *
     * @param result       统一响应
     * @param defaultValue 默认值
     * @param <T>          数据类型
     * @return 响应数据
     */
    public <T> T parseOrDefault(ApiResult<T> result, T defaultValue) {
        try {
            T data = this.parse(result);
            return ObjUtil.defaultIfNull(data, defaultValue);
        } catch (RemoteCallException ex) {
            log.warn("远程响应解析失败，返回默认值，error={}", ex.getMessage());
            return defaultValue;
        }
    }

    /**
     * 判断统一响应是否成功。
     *
     * @param result 统一响应
     * @return 是否成功
     */
    private boolean isSuccess(ApiResult<?> result) {
        return Integer.valueOf(200).equals(result.code())
                || StrUtil.equalsAnyIgnoreCase(result.message(), "success", "ok", "操作成功");
    }

}
```

文件位置：`src/main/java/io/github/atengk/remote/client/UserWrappedApiClient.java`

下面的客户端类演示如何通过通用 HTTP 客户端和统一响应解析器调用包装结构接口。

```java
package io.github.atengk.remote.client;

import io.github.atengk.common.http.HttpRequestOptions;
import io.github.atengk.common.http.RemoteResponseParser;
import io.github.atengk.common.http.RestTemplateHttpClient;
import io.github.atengk.remote.dto.ApiResult;
import io.github.atengk.remote.dto.PageResponse;
import io.github.atengk.remote.dto.UserCreateRequest;
import io.github.atengk.remote.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 用户包装响应远程接口客户端
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserWrappedApiClient {

    private final RestTemplateHttpClient httpClient;

    private final RemoteResponseParser responseParser;

    /**
     * 查询用户详情。
     *
     * @param userId 用户ID
     * @return 用户响应数据
     */
    public UserResponse getUser(Long userId) {
        ApiResult<UserResponse> result = httpClient.exchange(
                "/mock-api/result/users/{id}",
                HttpMethod.GET,
                null,
                HttpRequestOptions.builder()
                        .pathVariables(Map.of("id", userId))
                        .build(),
                new ParameterizedTypeReference<>() {
                }
        );

        return responseParser.parse(result);
    }

    /**
     * 查询用户分页。
     *
     * @param pageNum  页码
     * @param pageSize 每页数量
     * @return 用户分页数据
     */
    public PageResponse<UserResponse> pageUsers(Integer pageNum, Integer pageSize) {
        ApiResult<PageResponse<UserResponse>> result = httpClient.exchange(
                "/mock-api/result/users/page",
                HttpMethod.GET,
                null,
                HttpRequestOptions.builder()
                        .queryParams(Map.of("pageNum", pageNum, "pageSize", pageSize))
                        .build(),
                new ParameterizedTypeReference<>() {
                }
        );

        return responseParser.parseOrDefault(result, PageResponse.empty());
    }

    /**
     * 创建用户。
     *
     * @param request 创建用户请求
     * @return 用户响应数据
     */
    public UserResponse createUser(UserCreateRequest request) {
        ApiResult<UserResponse> result = httpClient.exchange(
                "/mock-api/result/users",
                HttpMethod.POST,
                request,
                HttpRequestOptions.empty(),
                new ParameterizedTypeReference<>() {
                }
        );

        return responseParser.parse(result);
    }

    /**
     * 查询用户列表，失败时返回空列表。
     *
     * @return 用户列表
     */
    public List<UserResponse> listUsersOrEmpty() {
        ApiResult<List<UserResponse>> result = httpClient.exchange(
                "/mock-api/result/users",
                HttpMethod.GET,
                null,
                HttpRequestOptions.empty(),
                new ParameterizedTypeReference<>() {
                }
        );

        return responseParser.parseOrDefault(result, List.of());
    }

}
```

调用示例：

```java
UserResponse user = userWrappedApiClient.getUser(1L);

PageResponse<UserResponse> pageResponse = userWrappedApiClient.pageUsers(1, 10);

UserResponse createdUser = userWrappedApiClient.createUser(
        new UserCreateRequest("ateng", "Ateng", "18800000000")
);

List<UserResponse> users = userWrappedApiClient.listUsersOrEmpty();
```

远程统一响应示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "username": "ateng",
    "nickname": "Ateng",
    "mobile": "18800000000",
    "status": 1,
    "createdAt": "2026-04-30T10:30:00"
  }
}
```

统一响应解析建议如下：

| 建议                             | 说明                                   |
| -------------------------------- | -------------------------------------- |
| 远程客户端只负责调用接口         | 不写复杂业务逻辑                       |
| 响应解析器负责判断业务成功       | 统一处理 `code`、`message`、`data`     |
| 默认值由调用方法决定             | 查询列表可返回空列表，详情接口可抛异常 |
| 业务失败不要静默吞掉             | 至少记录 code 和 message               |
| 响应结构不同的第三方接口单独适配 | 不强行套用项目内部统一响应             |

### Hutool 辅助工具使用

Hutool 在 `RestTemplate` 封装中适合用于空值判断、字符串处理、集合处理、JSON 辅助解析、TraceId 生成、耗时统计和日志字段截断。Hutool 不应替代 Spring 的核心 HTTP 能力，而是作为辅助工具减少样板代码。

本章已经使用到的 Hutool 工具如下：

| Hutool 工具类  | 使用场景                 |
| -------------- | ------------------------ |
| `ObjUtil`      | 对象空值判断、默认值处理 |
| `StrUtil`      | 字符串判空、截断、格式化 |
| `CollUtil`     | 集合判空、集合拼接       |
| `IdUtil`       | 生成 TraceId             |
| `TimeInterval` | 统计请求耗时             |
| `JSONUtil`     | 原始 JSON 字符串解析     |

文件位置：

```text
src/main/java/io/github/atengk/common/http/HttpHutoolHelper.java
```

文件位置：`src/main/java/io/github/atengk/common/http/HttpHutoolHelper.java`

下面的辅助类集中封装常用的 Hutool HTTP 辅助能力，例如 TraceId、响应体截断、集合参数拼接和默认值处理。

```java
package io.github.atengk.common.http;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;

import java.util.Collection;
import java.util.List;

/**
 * HTTP Hutool 辅助工具
 *
 * @author Ateng
 * @since 2026-04-30
 */
public class HttpHutoolHelper {

    private static final int DEFAULT_LOG_BODY_LENGTH = 1000;

    private HttpHutoolHelper() {
    }

    /**
     * 生成链路追踪ID。
     *
     * @return 链路追踪ID
     */
    public static String traceId() {
        return IdUtil.fastSimpleUUID();
    }

    /**
     * 限制日志响应体长度。
     *
     * @param body 响应体
     * @return 截断后的响应体
     */
    public static String limitBody(String body) {
        if (StrUtil.isBlank(body)) {
            return "";
        }
        return StrUtil.maxLength(body, DEFAULT_LOG_BODY_LENGTH);
    }

    /**
     * 将集合参数拼接成逗号分隔字符串。
     *
     * @param values 参数集合
     * @return 逗号分隔字符串
     */
    public static String joinQueryValues(Collection<?> values) {
        if (CollUtil.isEmpty(values)) {
            return "";
        }
        return CollUtil.join(values, ",");
    }

    /**
     * 获取非空列表。
     *
     * @param values 原始列表
     * @param <T>    元素类型
     * @return 非空列表
     */
    public static <T> List<T> emptyIfNull(List<T> values) {
        if (CollUtil.isEmpty(values)) {
            return List.of();
        }
        return values;
    }

    /**
     * 获取默认值。
     *
     * @param value        原始值
     * @param defaultValue 默认值
     * @param <T>          值类型
     * @return 最终值
     */
    public static <T> T defaultIfNull(T value, T defaultValue) {
        return ObjUtil.defaultIfNull(value, defaultValue);
    }

}
```

使用示例：

```java
String traceId = HttpHutoolHelper.traceId();

String responseBody = HttpHutoolHelper.limitBody(rawResponseBody);

String statusText = HttpHutoolHelper.joinQueryValues(List.of(1, 2, 3));

List<UserResponse> users = HttpHutoolHelper.emptyIfNull(userList);

Integer pageSize = HttpHutoolHelper.defaultIfNull(requestPageSize, 10);
```

在 `RestTemplate` 封装中使用 Hutool 时，需要注意下面几点：

| 建议                                   | 说明                                   |
| -------------------------------------- | -------------------------------------- |
| 空值判断可以使用 `ObjUtil`             | 代码更简洁                             |
| 字符串判断可以使用 `StrUtil`           | 比手写 `null` 和 `isBlank` 更统一      |
| 日志响应体必须截断                     | 使用 `StrUtil.maxLength` 控制长度      |
| TraceId 可用 `IdUtil.fastSimpleUUID()` | 简单、无横杠，适合日志追踪             |
| 集合参数可用 `CollUtil.join`           | 适合逗号分隔的 Query 参数              |
| 不要过度封装 Hutool                    | 简单直接使用即可，不必所有工具都包一层 |

最终推荐的实战调用结构如下：

```text
业务 Service
  -> XxxApiClient
    -> RestTemplateHttpClient
      -> RestTemplate
        -> RequestInterceptor 记录请求日志和 TraceId
        -> ResponseErrorHandler 处理 HTTP 错误状态
    -> RemoteResponseParser 解析统一响应
  -> 业务 Service 根据结果继续处理业务逻辑
```

到这里，`RestTemplate` 的实战封装已经形成一套完整链路：`RestTemplateHttpClient` 负责通用 HTTP 调用，`RestTemplateAccessLogInterceptor` 负责请求日志记录，`RemoteResponseParser` 负责统一响应解析，`HttpHutoolHelper` 提供常用辅助能力。实际项目中，业务代码只需要面向具体的 `XxxApiClient`，不应直接操作底层 `RestTemplate`。

## 测试与验证

这一部分用于说明如何对 `RestTemplate` 远程调用代码进行测试，包括使用 Mock 接口模拟远程服务、编写单元测试验证请求行为，以及通过 curl、日志和断言确认调用结果。当前章节继续基于你上传的大纲展开。

### 使用 Mock 接口测试

Mock 接口测试用于在不依赖真实第三方服务的情况下验证远程调用逻辑。Spring Framework 提供了 `MockRestServiceServer`，它可以绑定到 `RestTemplate`，预先设置期望请求和模拟响应，从而测试使用 `RestTemplate` 的客户端代码；Spring 官方文档也说明，对于更完整的传输层和网络条件测试，可以使用 WireMock、OkHttp MockWebServer 等独立 Mock Web Server。([Home](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/test/web/client/MockRestServiceServer.html?utm_source=chatgpt.com))

在当前文档中，建议采用两类测试：

| 测试方式                | 适用场景                                             |
| ----------------------- | ---------------------------------------------------- |
| `MockRestServiceServer` | 单元测试远程客户端是否按预期发送请求、解析响应       |
| 独立 Mock 服务          | 联调阶段模拟真实 HTTP 服务、超时、错误状态码、响应头 |

如果只是验证 `RestTemplate` 客户端封装是否正确，优先使用 `MockRestServiceServer`，它不需要启动真实 HTTP 端口，测试速度更快。

文件位置：

```text
src/test/java/io/github/atengk/remote/client/UserApiClientMockTest.java
```

文件位置：`src/test/java/io/github/atengk/remote/client/UserApiClientMockTest.java`

下面的测试类使用 `MockRestServiceServer` 模拟远程用户接口。

```java
package io.github.atengk.remote.client;

import io.github.atengk.remote.dto.UserCreateRequest;
import io.github.atengk.remote.dto.UserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * 用户远程接口 Mock 测试
 *
 * @author Ateng
 * @since 2026-04-30
 */
class UserApiClientMockTest {

    private MockRestServiceServer mockServer;

    private UserApiClient userApiClient;

    /**
     * 初始化测试环境。
     */
    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplateBuilder()
                // 测试环境使用固定基础地址，便于断言完整请求地址
                .rootUri("http://mock-server")
                .build();

        this.mockServer = MockRestServiceServer.bindTo(restTemplate).build();
        this.userApiClient = new UserApiClient(restTemplate);
    }

    /**
     * 测试根据用户ID查询用户。
     */
    @Test
    void shouldGetUserById() {
        String responseBody = """
                {
                  "id": 1,
                  "username": "ateng",
                  "nickname": "Ateng",
                  "mobile": "18800000000",
                  "status": 1,
                  "createdAt": "2026-04-30T10:30:00"
                }
                """;

        this.mockServer.expect(once(), requestTo("http://mock-server/mock-api/users/1"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        UserResponse user = this.userApiClient.getUserById(1L);

        assertThat(user).isNotNull();
        assertThat(user.id()).isEqualTo(1L);
        assertThat(user.username()).isEqualTo("ateng");
        assertThat(user.nickname()).isEqualTo("Ateng");
        assertThat(user.createdAt()).isEqualTo(LocalDateTime.of(2026, 4, 30, 10, 30));

        this.mockServer.verify();
    }

    /**
     * 测试根据关键词查询用户列表。
     */
    @Test
    void shouldListUsersByKeyword() {
        String responseBody = """
                [
                  {
                    "id": 1,
                    "username": "ateng",
                    "nickname": "Ateng",
                    "mobile": "18800000000",
                    "status": 1,
                    "createdAt": "2026-04-30T10:30:00"
                  }
                ]
                """;

        this.mockServer.expect(once(), requestTo("http://mock-server/mock-api/users?keyword=Ateng"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        List<UserResponse> users = this.userApiClient.listUsers("Ateng");

        assertThat(users).hasSize(1);
        assertThat(users.getFirst().username()).isEqualTo("ateng");

        this.mockServer.verify();
    }

    /**
     * 测试创建用户请求。
     */
    @Test
    void shouldCreateUser() {
        String responseBody = """
                {
                  "id": 1,
                  "username": "ateng",
                  "nickname": "Ateng",
                  "mobile": "18800000000",
                  "status": 1,
                  "createdAt": "2026-04-30T10:30:00"
                }
                """;

        this.mockServer.expect(once(), requestTo("http://mock-server/mock-api/users"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Content-Type", MediaType.APPLICATION_JSON_VALUE))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.username").value("ateng"))
                .andExpect(jsonPath("$.nickname").value("Ateng"))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        UserCreateRequest request = new UserCreateRequest("ateng", "Ateng", "18800000000");
        UserResponse user = this.userApiClient.createUserWithHeader(request);

        assertThat(user).isNotNull();
        assertThat(user.id()).isEqualTo(1L);
        assertThat(user.username()).isEqualTo("ateng");

        this.mockServer.verify();
    }

}
```

如果项目中 `UserApiClient` 是 Spring Bean，也可以使用 `@SpringBootTest` 加载 Spring 上下文，但单元测试中更推荐直接构造依赖对象，避免启动完整容器导致测试变慢。

运行测试命令如下：

```bash
# 运行全部测试
mvn test

# 只运行指定测试类
mvn -Dtest=UserApiClientMockTest test

# 只运行指定测试方法
mvn -Dtest=UserApiClientMockTest#shouldGetUserById test
```

`mvn test` 用于执行全部单元测试；`-Dtest=UserApiClientMockTest` 用于指定测试类；`#shouldGetUserById` 用于只运行某个测试方法，适合定位单个远程调用测试失败问题。

### 单元测试编写

单元测试应重点验证远程客户端是否正确构造请求、是否正确解析响应、是否正确处理异常。对于 `RestTemplate` 封装类，不建议只测试“返回不为空”，还应验证请求方法、请求地址、请求头、请求体和异常分支。

建议覆盖下面几类测试场景：

| 测试场景      | 验证点                                             |
| ------------- | -------------------------------------------------- |
| GET 查询成功  | 请求地址、PathVariable、Query、响应对象            |
| POST 创建成功 | 请求方法、Content-Type、请求体字段、响应对象       |
| 4xx 响应      | 是否转换为统一远程调用异常                         |
| 5xx 响应      | 是否记录异常并返回预期结果                         |
| 空响应        | 是否返回默认值或抛出明确异常                       |
| 泛型响应      | `ApiResult<T>`、`PageResponse<T>` 是否正确反序列化 |

文件位置：

```text
src/test/java/io/github/atengk/common/http/RestTemplateHttpClientTest.java
```

文件位置：`src/test/java/io/github/atengk/common/http/RestTemplateHttpClientTest.java`

下面的测试类用于验证前面章节封装的 `RestTemplateHttpClient`。

```java
package io.github.atengk.common.http;

import io.github.atengk.remote.dto.ApiResult;
import io.github.atengk.remote.dto.PageResponse;
import io.github.atengk.remote.dto.UserCreateRequest;
import io.github.atengk.remote.dto.UserResponse;
import io.github.atengk.remote.exception.RemoteCallException;
import io.github.atengk.remote.support.RestTemplateExceptionTranslator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * RestTemplate 通用 HTTP 客户端测试
 *
 * @author Ateng
 * @since 2026-04-30
 */
class RestTemplateHttpClientTest {

    private MockRestServiceServer mockServer;

    private RestTemplateHttpClient httpClient;

    /**
     * 初始化测试环境。
     */
    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplateBuilder()
                .rootUri("http://mock-server")
                .build();

        this.mockServer = MockRestServiceServer.bindTo(restTemplate).build();
        this.httpClient = new RestTemplateHttpClient(restTemplate, new RestTemplateExceptionTranslator());
    }

    /**
     * 测试 GET 请求携带路径参数和查询参数。
     */
    @Test
    void shouldSendGetWithPathAndQueryParams() {
        String responseBody = """
                {
                  "id": 1,
                  "username": "ateng",
                  "nickname": "Ateng",
                  "mobile": "18800000000",
                  "status": 1,
                  "createdAt": "2026-04-30T10:30:00"
                }
                """;

        this.mockServer.expect(once(), requestTo("http://mock-server/mock-api/users/1?includeDetail=true"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        UserResponse user = this.httpClient.get(
                "/mock-api/users/{id}",
                HttpRequestOptions.builder()
                        .pathVariables(Map.of("id", 1L))
                        .queryParams(Map.of("includeDetail", true))
                        .build(),
                UserResponse.class
        );

        assertThat(user).isNotNull();
        assertThat(user.id()).isEqualTo(1L);
        assertThat(user.username()).isEqualTo("ateng");

        this.mockServer.verify();
    }

    /**
     * 测试 POST 请求携带 JSON 请求体。
     */
    @Test
    void shouldSendPostWithJsonBody() {
        String responseBody = """
                {
                  "id": 1,
                  "username": "ateng",
                  "nickname": "Ateng",
                  "mobile": "18800000000",
                  "status": 1,
                  "createdAt": "2026-04-30T10:30:00"
                }
                """;

        this.mockServer.expect(once(), requestTo("http://mock-server/mock-api/users"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.username").value("ateng"))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        UserCreateRequest request = new UserCreateRequest("ateng", "Ateng", "18800000000");

        UserResponse user = this.httpClient.post(
                "/mock-api/users",
                request,
                HttpRequestOptions.empty(),
                UserResponse.class
        );

        assertThat(user).isNotNull();
        assertThat(user.username()).isEqualTo("ateng");

        this.mockServer.verify();
    }

    /**
     * 测试泛型响应解析。
     */
    @Test
    void shouldParseGenericApiResultPageResponse() {
        String responseBody = """
                {
                  "code": 200,
                  "message": "success",
                  "data": {
                    "records": [
                      {
                        "id": 1,
                        "username": "ateng",
                        "nickname": "Ateng",
                        "mobile": "18800000000",
                        "status": 1,
                        "createdAt": "2026-04-30T10:30:00"
                      }
                    ],
                    "total": 1,
                    "pageNum": 1,
                    "pageSize": 10
                  }
                }
                """;

        this.mockServer.expect(once(), requestTo("http://mock-server/mock-api/result/users/page?pageNum=1&pageSize=10"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        ApiResult<PageResponse<UserResponse>> result = this.httpClient.exchange(
                "/mock-api/result/users/page",
                HttpMethod.GET,
                null,
                HttpRequestOptions.builder()
                        .queryParams(Map.of("pageNum", 1, "pageSize", 10))
                        .build(),
                new ParameterizedTypeReference<>() {
                }
        );

        assertThat(result).isNotNull();
        assertThat(result.code()).isEqualTo(200);
        assertThat(result.data().records()).hasSize(1);
        assertThat(result.data().records().getFirst().username()).isEqualTo("ateng");

        this.mockServer.verify();
    }

    /**
     * 测试服务端异常响应。
     */
    @Test
    void shouldThrowRemoteCallExceptionWhenServerError() {
        this.mockServer.expect(once(), requestTo("http://mock-server/mock-api/users/1"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withServerError());

        assertThatThrownBy(() -> this.httpClient.get(
                "/mock-api/users/{id}",
                HttpRequestOptions.builder()
                        .pathVariables(Map.of("id", 1L))
                        .build(),
                UserResponse.class
        )).isInstanceOf(RemoteCallException.class);

        this.mockServer.verify();
    }

}
```

测试代码中使用了 AssertJ 断言，它由 `spring-boot-starter-test` 默认引入。测试应重点验证“请求是否正确发出”和“响应是否正确解析”，而不是只验证方法是否执行完成。

如果需要测试超时、连接失败、DNS 异常等传输层行为，建议使用独立 Mock Web Server 或 WireMock，而不是只依赖 `MockRestServiceServer`。Spring 官方测试文档也说明，mock web server 更适合完整测试传输层和网络条件。([Home](https://docs.spring.io/spring-framework/reference/testing/spring-mvc-test-client.html?utm_source=chatgpt.com))

### 调用结果验证

调用结果验证用于确认远程接口调用在真实运行环境中是否符合预期。验证内容不应只看接口返回值，还应同时检查请求参数、响应状态、日志输出、异常处理、超时配置和业务默认值。

建议从下面几个维度验证：

| 验证项      | 说明                                              |
| ----------- | ------------------------------------------------- |
| HTTP 状态码 | 是否为预期的 `2xx`、`4xx`、`5xx`                  |
| 响应体      | JSON 字段是否完整，是否能反序列化                 |
| 响应头      | 是否包含 `Content-Type`、`X-Trace-Id` 等          |
| 日志        | 是否输出 method、uri、statusCode、costMs、traceId |
| 异常        | 4xx、5xx、超时是否进入预期异常分支                |
| 默认值      | 空响应、业务失败时是否返回预期默认值              |

可以先通过 curl 验证 Mock 接口，再通过应用日志验证 `RestTemplate` 调用链路。

```bash
# 验证用户详情接口
curl -i -X GET 'http://localhost:8081/mock-api/users/1'

# 验证用户列表接口
curl -i -X GET 'http://localhost:8081/mock-api/users?keyword=Ateng&pageNum=1&pageSize=10'

# 验证创建用户接口
curl -i -X POST 'http://localhost:8081/mock-api/users' \
  -H 'Content-Type: application/json' \
  -d '{
    "username": "ateng",
    "nickname": "Ateng",
    "mobile": "18800000000"
  }'

# 验证异常状态码接口，具体路径按 Mock 服务实现调整
curl -i -X GET 'http://localhost:8081/mock-api/users/error'
```

`curl -i` 会输出响应头和响应体，适合检查状态码、`Content-Type`、`X-Trace-Id` 和 JSON 响应内容。

期望日志示例：

```text
远程请求开始，method=GET, uri=http://localhost:8081/mock-api/users/1, traceId=9cb9b77e0cef48359735a89fb6e52c89
远程请求完成，method=GET, uri=http://localhost:8081/mock-api/users/1, statusCode=200 OK, costMs=42, traceId=9cb9b77e0cef48359735a89fb6e52c89
```

如果测试失败，可以按下面顺序排查：

| 问题                   | 排查方向                                     |
| ---------------------- | -------------------------------------------- |
| 请求未命中 Mock        | 检查 rootUri、请求路径、Query 参数顺序       |
| JSON 解析失败          | 检查字段名、时间格式、Content-Type           |
| Header 断言失败        | 检查拦截器是否追加 Header                    |
| 泛型解析异常           | 检查是否使用 `ParameterizedTypeReference`    |
| 4xx/5xx 未进入异常分支 | 检查是否注册 `ResponseErrorHandler`          |
| 测试互相影响           | 每个测试方法重新创建 `MockRestServiceServer` |

完成测试后，建议至少保留下面几类用例：

```text
GET 查询成功
POST 创建成功
PUT 更新成功
DELETE 删除成功
Query 参数编码正确
Header 参数传递正确
统一响应解析成功
HTTP 4xx 异常处理
HTTP 5xx 异常处理
空响应默认值处理
```

## 使用建议

这一部分用于总结 `RestTemplate` 的适用场景、开发注意事项，以及在 Spring Boot 3 项目中如何逐步迁移到 `RestClient`。`RestTemplate` 仍适合维护存量同步调用代码，但新项目应结合 Spring Framework 版本和团队技术栈评估是否优先使用 `RestClient`。

### 适用场景

`RestTemplate` 适合同步、阻塞、调用链路清晰的远程 HTTP 调用场景。它 API 直接，学习成本低，尤其适合传统 Spring MVC 项目、后台任务、第三方接口集成和存量系统维护。

推荐使用 `RestTemplate` 的场景如下：

| 场景                 | 说明                                                 |
| -------------------- | ---------------------------------------------------- |
| 存量项目维护         | 项目中已有大量 `RestTemplate` 调用，不适合一次性迁移 |
| 简单同步 HTTP 调用   | 调用第三方接口、内部服务接口、后台同步任务           |
| 传统 Spring MVC 项目 | 业务线程模型本身就是同步阻塞                         |
| 团队已封装成熟工具类 | 已有统一异常、日志、Header、响应解析封装             |
| 短期兼容历史代码     | 保持稳定优先，不引入大规模改造                       |

不建议优先使用 `RestTemplate` 的场景如下：

| 场景                                  | 推荐替代                                     |
| ------------------------------------- | -------------------------------------------- |
| 新建 Spring Framework 6.1+ 同步客户端 | `RestClient`                                 |
| 响应式应用                            | `WebClient`                                  |
| 高并发非阻塞调用                      | `WebClient`                                  |
| 流式响应处理                          | `WebClient`                                  |
| 希望使用声明式 HTTP 接口              | HTTP Interface + `RestClient` 或 `WebClient` |

Spring Framework 文档将 `RestClient` 定位为同步、流式 API 风格的 HTTP 客户端；`WebClient` 是非阻塞响应式客户端；`RestTemplate` 是同步模板式客户端。([Home](https://docs.spring.io/spring-framework/reference/6.2-SNAPSHOT/integration/rest-clients.html?utm_source=chatgpt.com))

### 注意事项

使用 `RestTemplate` 时，应把重点放在“统一配置”和“统一封装”上，而不是在业务代码中直接堆叠 HTTP 调用细节。生产项目中尤其需要关注超时、连接池、异常处理、日志脱敏和响应解析。

建议遵守下面规则：

| 注意事项                                  | 说明                                       |
| ----------------------------------------- | ------------------------------------------ |
| 必须配置超时时间                          | 避免远程服务异常导致业务线程长时间阻塞     |
| 生产环境建议使用连接池                    | 高并发或频繁调用时使用 Apache HttpClient 5 |
| 不要在业务代码中反复 `new RestTemplate()` | 统一交给 Spring Bean 管理                  |
| 不要手动拼接复杂 URL                      | 使用 `UriComponentsBuilder`                |
| 泛型响应使用 `ParameterizedTypeReference` | 避免反序列化成 `LinkedHashMap`             |
| 统一处理 4xx 和 5xx                       | 使用 `ResponseErrorHandler` 或异常转换器   |
| 记录 TraceId 和耗时                       | 便于排查远程调用问题                       |
| 不完整打印敏感信息                        | Token、密码、手机号、身份证号等需要脱敏    |
| 文件上传下载单独处理                      | 不要复用普通 JSON 请求日志策略             |
| 多个远程服务建议拆分客户端                | 每个第三方服务一个 `XxxApiClient`          |

推荐的项目结构如下：

```text
src/main/java/io/github/atengk
├── config
│   ├── RestTemplateConfig.java
│   ├── RestTemplatePoolConfig.java
│   └── properties
│       └── RestTemplateProperties.java
├── common
│   └── http
│       ├── HttpRequestOptions.java
│       ├── RestTemplateHttpClient.java
│       ├── RemoteResponseParser.java
│       └── interceptor
│           └── RestTemplateAccessLogInterceptor.java
└── remote
    ├── client
    │   ├── UserApiClient.java
    │   └── UserWrappedApiClient.java
    ├── dto
    │   ├── ApiResult.java
    │   ├── PageResponse.java
    │   ├── UserCreateRequest.java
    │   └── UserResponse.java
    ├── exception
    │   ├── RemoteAccessException.java
    │   └── RemoteCallException.java
    └── support
        └── RestTemplateExceptionTranslator.java
```

推荐调用流程如下：

```text
Controller
  -> Service
    -> XxxApiClient
      -> RestTemplateHttpClient
        -> RestTemplate
          -> Interceptor 追加 TraceId 和访问日志
          -> ResponseErrorHandler 处理 HTTP 错误状态
      -> RemoteResponseParser 解析业务响应
    -> Service 处理业务逻辑
```

这套结构可以保证业务层不直接依赖 `RestTemplate` 的细节，也便于后续把底层客户端替换为 `RestClient`。

### 迁移到 RestClient 的建议

`RestClient` 是 Spring Framework 6.1 引入的同步 HTTP 客户端，提供现代化的 Fluent API，并且可以通过 `RestClient.create(RestTemplate)` 或 `RestClient.builder(RestTemplate)` 复用已有 `RestTemplate` 配置。([Home](https://docs.spring.io/spring-framework/docs/6.2.x/javadoc-api/org/springframework/web/client/RestClient.html?utm_source=chatgpt.com))

对于 Spring Boot 3 项目，建议采用渐进式迁移，不建议一次性全量替换。尤其是已经封装了连接池、拦截器、消息转换器、异常处理器的项目，可以先从单个低风险远程客户端开始迁移。

推荐迁移步骤如下：

| 步骤   | 说明                                                         |
| ------ | ------------------------------------------------------------ |
| 第一步 | 保留现有 `RestTemplate` Bean 和封装                          |
| 第二步 | 使用 `RestClient.create(restTemplate)` 创建兼容配置的 `RestClient` |
| 第三步 | 选择低风险 `XxxApiClient` 迁移请求写法                       |
| 第四步 | 统一封装 `RestClientHttpClient`                              |
| 第五步 | 新接口默认使用 `RestClient`，旧接口逐步迁移                  |
| 第六步 | 清理不再使用的 `RestTemplate` 客户端代码                     |

Spring Framework 当前文档也给出渐进迁移思路：可以先从现有 `RestTemplate` 实例创建 `RestClient`，逐步替换请求调用代码；由于两者共享请求工厂、拦截器和消息转换器等基础设施，后续再用 `RestClient.Builder` 复刻原有配置。([Home](https://docs.spring.io/spring-framework/reference/integration/rest-clients.html?utm_source=chatgpt.com))

文件位置：

```text
src/main/java/io/github/atengk/config/RestClientConfig.java
src/main/java/io/github/atengk/remote/client/UserRestClientApiClient.java
```

文件位置：`src/main/java/io/github/atengk/config/RestClientConfig.java`

下面的配置类基于已有 `RestTemplate` 创建 `RestClient`，适合作为迁移第一步。

```java
package io.github.atengk.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

/**
 * RestClient 配置
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Configuration
public class RestClientConfig {

    /**
     * 基于已有 RestTemplate 创建 RestClient。
     *
     * @param restTemplate 现有 RestTemplate 实例
     * @return RestClient 实例
     */
    @Bean
    public RestClient restClient(RestTemplate restTemplate) {
        return RestClient.create(restTemplate);
    }

}
```

文件位置：`src/main/java/io/github/atengk/remote/client/UserRestClientApiClient.java`

下面的客户端类演示使用 `RestClient` 改写常见 GET 和 POST 请求。

```java
package io.github.atengk.remote.client;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.remote.dto.UserCreateRequest;
import io.github.atengk.remote.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * 用户 RestClient 远程接口客户端
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserRestClientApiClient {

    private final RestClient restClient;

    /**
     * 根据用户ID查询用户。
     *
     * @param userId 用户ID
     * @return 用户响应数据
     */
    public UserResponse getUser(Long userId) {
        if (ObjUtil.isNull(userId)) {
            log.warn("RestClient 查询用户失败，用户ID为空");
            return null;
        }

        log.info("RestClient 开始查询用户，userId={}", userId);
        return restClient.get()
                .uri("/mock-api/users/{id}", userId)
                .retrieve()
                .body(UserResponse.class);
    }

    /**
     * 查询用户列表。
     *
     * @param keyword 关键词
     * @return 用户列表
     */
    public List<UserResponse> listUsers(String keyword) {
        log.info("RestClient 开始查询用户列表，keyword={}", keyword);
        List<UserResponse> users = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/mock-api/users")
                        .queryParamIfPresent("keyword", StrUtil.isBlank(keyword) ? java.util.Optional.empty() : java.util.Optional.of(keyword))
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

        return users == null ? List.of() : users;
    }

    /**
     * 创建用户。
     *
     * @param request 创建用户请求
     * @return 用户响应数据
     */
    public UserResponse createUser(UserCreateRequest request) {
        if (ObjUtil.isNull(request) || StrUtil.isBlank(request.username())) {
            log.warn("RestClient 创建用户失败，请求参数不完整，request={}", request);
            return null;
        }

        log.info("RestClient 开始创建用户，username={}", request.username());
        return restClient.post()
                .uri("/mock-api/users")
                .body(request)
                .retrieve()
                .body(UserResponse.class);
    }

}
```

`RestTemplate` 与 `RestClient` 的常见写法对比如下：

| 场景         | RestTemplate                                           | RestClient                                                   |
| ------------ | ------------------------------------------------------ | ------------------------------------------------------------ |
| GET 对象     | `getForObject(url, UserResponse.class, id)`            | `get().uri(url, id).retrieve().body(UserResponse.class)`     |
| POST 对象    | `postForObject(url, request, UserResponse.class)`      | `post().uri(url).body(request).retrieve().body(UserResponse.class)` |
| 泛型响应     | `exchange(..., new ParameterizedTypeReference<>() {})` | `retrieve().body(new ParameterizedTypeReference<>() {})`     |
| 完整响应     | `getForEntity(...)`、`exchange(...)`                   | `retrieve().toEntity(...)`                                   |
| 统一状态处理 | `ResponseErrorHandler`                                 | `defaultStatusHandler(...)`                                  |

对于新项目或新模块，建议优先考虑 `RestClient`。对于已有 `RestTemplate` 项目，建议先保持现有封装稳定，再逐个客户端迁移。Spring Framework 7.0 文档已经说明 `RestTemplate` 在 Spring Framework 7.0 起被标记为弃用并建议使用 `RestClient`；如果项目未来计划升级到 Spring Framework 7 或更高版本，应尽早制定迁移计划。([Home](https://docs.spring.io/spring-framework/reference/integration/rest-clients.html?utm_source=chatgpt.com))

最终建议如下：

| 项目类型                           | 建议                                                   |
| ---------------------------------- | ------------------------------------------------------ |
| Spring Boot 3 存量项目             | 继续维护 `RestTemplate`，新增接口逐步使用 `RestClient` |
| Spring Boot 3 新项目               | 优先使用 `RestClient`                                  |
| Spring WebFlux 项目                | 使用 `WebClient`                                       |
| 第三方接口大量接入项目             | 优先封装统一 HTTP 客户端，再决定底层实现               |
| 准备升级 Spring Framework 7 的项目 | 制定 `RestTemplate` 到 `RestClient` 的迁移计划         |

到这里，`RestTemplate` 开发使用文档的大纲内容已经覆盖完整：从技术背景、环境准备、基础使用、参数处理、响应处理、常用配置、异常处理、实战封装，到测试验证和迁移建议。实际项目中建议把本文中的示例代码整理成独立 `remote` 模块或 `common-http` 模块，作为团队统一远程 HTTP 调用规范。