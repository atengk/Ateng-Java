# RestClient

`RestClient` 是 Spring Boot 3 / Spring Framework 6 中推荐使用的同步 HTTP 客户端，适合普通 Spring MVC 项目调用第三方接口、内部网关接口、开放平台接口、文件服务接口等场景。

本项目统一采用以下结构：

```text
业务 Service
  -> XxxApiClient
      -> 指定 RestClient Bean
          -> RemoteRestClientFactory
              -> Apache HttpClient 5
```

核心约定如下：

```text
1. RestClient Bean 按远程系统或用途创建，不按 GET / POST 创建
2. 所有 RestClient Bean 统一通过 RemoteRestClientFactory 创建
3. 客户端名称统一使用 RemoteHttpConstant 常量，不允许在配置类中写字符串字面量
4. 所有 RestClient 默认具备日志、TraceId、统一错误处理能力
5. 代理、SSL/mTLS、XML、认证、签名、重试、熔断等功能通过扩展点增量实现
6. 业务代码优先封装 XxxApiClient，不建议直接到处注入 RestClient
```

基础必做的 Bean：

```text
1. defaultRestClient      普通 JSON 接口调用
2. fastTimeoutRestClient  快速失败接口调用
3. longTimeoutRestClient  长耗时接口调用
```

增强必做的能力内置在基础配置中：

```text
1. loggingRestClient 能力
2. traceRestClient 能力
3. errorHandledRestClient 能力
```

注意：`loggingRestClient`、`traceRestClient`、`errorHandledRestClient` 不是独立 Bean，而是所有 `RestClient` 默认具备的横切能力。

## 基础配置

这一部分是后续所有功能的统一底座。后续继续做 Bearer Token、API Key、AK/SK 签名、文件上传、文件下载、代理、mTLS、忽略 SSL、XML、重试、熔断时，只新增配置、拦截器、扩展器或 Client 示例，不再重写基础结构。

基础目录结构如下：

```text
src/main/java/io/github/atengk/http/
├── client/
│   └── DemoApiClient.java
├── config/
│   ├── RemoteHttpProperties.java
│   ├── RemoteRestClientFactory.java
│   └── RestClientBaseConfig.java
├── constant/
│   └── RemoteHttpConstant.java
├── exception/
│   └── RemoteCallException.java
├── extension/
│   ├── RemoteRequestFactoryCustomizer.java
│   └── RemoteRestClientBuilderCustomizer.java
├── handler/
│   └── RemoteHttpErrorHandler.java
├── interceptor/
│   ├── RestClientLogInterceptor.java
│   └── RestClientTraceInterceptor.java
└── support/
    ├── DefaultRemoteCallExecutor.java
    └── RemoteCallExecutor.java
```

### Maven 依赖

这里统一使用 `spring-boot-starter-web` 提供 `RestClient`，使用 Apache HttpClient 5 作为底层 HTTP 客户端，后续代理、SSL、mTLS、连接池和超时都基于它扩展。

文件位置：`pom.xml`

```xml
<dependencies>
    <!-- Spring MVC + RestClient 基础能力 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- 配置属性校验 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- Apache HttpClient 5：连接池、超时、代理、SSL、mTLS 的基础 -->
    <dependency>
        <groupId>org.apache.httpcomponents.client5</groupId>
        <artifactId>httpclient5</artifactId>
    </dependency>

    <!-- Hutool 工具类 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.8.35</version>
    </dependency>

    <!-- Lombok：简化 Java 样板代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

### application.yml

这份配置一次性包含基础客户端、日志、Trace、错误处理、代理预留配置、SSL 预留配置。代理和 SSL 默认关闭，后续实现 `proxyRestClient`、`mtlsRestClient`、`unsafeSslRestClient` 时直接使用这些配置字段。

文件位置：`src/main/resources/application.yml`

```yaml
---
# RestClient 配置
remote:
  http:
    logging:
      # 是否启用远程调用日志
      enabled: true

      # 是否打印请求头；敏感 Header 会自动脱敏
      include-headers: true

      # 是否打印请求体；生产环境建议关闭，避免输出敏感数据或大对象
      include-request-body: false

      # 请求体日志最大长度
      max-request-body-length: 2048

      # 敏感 Header，不区分大小写
      sensitive-headers:
        - Authorization
        - Cookie
        - Set-Cookie
        - X-API-Key
        - X-App-Secret

    trace:
      # 是否启用 TraceId 透传
      enabled: true

      # MDC 中保存 TraceId 的 key
      mdc-name: traceId

      # 向远程服务透传的请求头名称
      header-name: X-Trace-Id

      # MDC 不存在 TraceId 时是否自动生成
      generate-if-absent: true

    error-handling:
      # 是否启用统一错误处理
      enabled: true

      # 错误响应体最大保存长度
      max-response-body-length: 4096

    default-config:
      # 默认基础地址，允许为空；为空时调用方需要使用完整 URL
      base-url:

      # 建立 TCP 连接的超时时间
      connect-timeout: 3s

      # 从连接池获取连接的超时时间
      connection-request-timeout: 2s

      # 等待服务端响应的超时时间
      response-timeout: 10s

      # 最大空闲连接存活时间
      max-idle-time: 30s

      # 总连接池大小
      max-conn-total: 200

      # 单个路由最大连接数
      max-conn-per-route: 50

      # 默认请求头
      default-headers:
        Accept: application/json

      proxy:
        # 代理默认关闭，后续 proxyRestClient 使用
        enabled: false
        scheme: http
        host:
        port: 0
        username:
        password:

      ssl:
        # SSL 扩展默认关闭，后续 mtlsRestClient / unsafeSslRestClient 使用
        enabled: false

        # 是否信任所有证书，仅允许测试环境使用
        trust-all: false

        # 是否启用主机名校验，生产环境必须开启
        hostname-verification-enabled: true

        # TLS 协议
        protocol: TLS

        # 客户端证书，后续 mTLS 使用
        key-store-path:
        key-store-password:
        key-store-type: PKCS12

        # 信任证书，后续自定义 CA 使用
        trust-store-path:
        trust-store-password:
        trust-store-type: PKCS12

    clients:
      # 普通 JSON 接口调用
      default:
        base-url: https://api.example.com
        connect-timeout: 3s
        connection-request-timeout: 2s
        response-timeout: 10s
        max-idle-time: 30s
        max-conn-total: 200
        max-conn-per-route: 50
        default-headers:
          Accept: application/json

      # 快速失败客户端，适合实时查询类接口
      fast:
        base-url: https://api.example.com
        connect-timeout: 1s
        connection-request-timeout: 1s
        response-timeout: 3s
        max-idle-time: 20s
        max-conn-total: 200
        max-conn-per-route: 50
        default-headers:
          Accept: application/json

      # 长耗时客户端，适合报表导出、大文件处理、AI 生成等接口
      long:
        base-url: https://api.example.com
        connect-timeout: 5s
        connection-request-timeout: 3s
        response-timeout: 60s
        max-idle-time: 60s
        max-conn-total: 100
        max-conn-per-route: 30
        default-headers:
          Accept: application/json
```

### 基础常量

客户端名称统一放在常量类中，`RestClientBaseConfig` 不能直接写 `"default"`、`"fast"`、`"long"` 字符串，避免后续配置名和 Bean 创建名不一致。

文件位置：`src/main/java/io/github/atengk/http/constant/RemoteHttpConstant.java`

```java
package io.github.atengk.http.constant;

/**
 * 远程 HTTP 调用常量
 *
 * @author Ateng
 * @since 2026-04-29
 */
public final class RemoteHttpConstant {

    public static final String CLIENT_DEFAULT = "default";

    public static final String CLIENT_FAST = "fast";

    public static final String CLIENT_LONG = "long";

    public static final String MDC_TRACE_ID = "traceId";

    public static final String DEFAULT_TRACE_HEADER = "X-Trace-Id";

    public static final String HEADER_AUTHORIZATION = "Authorization";

    public static final String HEADER_CONTENT_TYPE = "Content-Type";

    public static final String HEADER_ACCEPT = "Accept";

    private RemoteHttpConstant() {
    }

}
```

### 配置属性类

这个配置类是整个 RestClient 体系的配置入口。这里已经预留 `ProxyConfig` 和 `SslConfig`，后续做代理、mTLS、忽略 SSL 时直接使用，不需要再改配置模型。

文件位置：`src/main/java/io/github/atengk/http/config/RemoteHttpProperties.java`

```java
package io.github.atengk.http.config;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.http.constant.RemoteHttpConstant;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 远程 HTTP 配置属性
 *
 * @author Ateng
 * @since 2026-04-29
 */
@Data
@Validated
@ConfigurationProperties(prefix = "remote.http")
public class RemoteHttpProperties {

    @Valid
    private LoggingConfig logging = new LoggingConfig();

    @Valid
    private TraceConfig trace = new TraceConfig();

    @Valid
    private ErrorHandlingConfig errorHandling = new ErrorHandlingConfig();

    @Valid
    private ClientConfig defaultConfig = new ClientConfig();

    @Valid
    private Map<String, ClientConfig> clients = new LinkedHashMap<>();

    /**
     * 获取合并后的客户端配置
     *
     * @param clientName 客户端名称
     * @return 客户端配置
     */
    public ClientConfig getMergedClient(String clientName) {
        ClientConfig source = clients.get(clientName);
        if (ObjectUtil.isNull(source)) {
            return defaultConfig;
        }

        ClientConfig merged = new ClientConfig();
        merged.setBaseUrl(ObjectUtil.defaultIfNull(source.getBaseUrl(), defaultConfig.getBaseUrl()));
        merged.setConnectTimeout(ObjectUtil.defaultIfNull(source.getConnectTimeout(), defaultConfig.getConnectTimeout()));
        merged.setConnectionRequestTimeout(ObjectUtil.defaultIfNull(source.getConnectionRequestTimeout(), defaultConfig.getConnectionRequestTimeout()));
        merged.setResponseTimeout(ObjectUtil.defaultIfNull(source.getResponseTimeout(), defaultConfig.getResponseTimeout()));
        merged.setMaxIdleTime(ObjectUtil.defaultIfNull(source.getMaxIdleTime(), defaultConfig.getMaxIdleTime()));
        merged.setMaxConnTotal(ObjectUtil.defaultIfNull(source.getMaxConnTotal(), defaultConfig.getMaxConnTotal()));
        merged.setMaxConnPerRoute(ObjectUtil.defaultIfNull(source.getMaxConnPerRoute(), defaultConfig.getMaxConnPerRoute()));

        Map<String, String> headers = new LinkedHashMap<>();
        if (MapUtil.isNotEmpty(defaultConfig.getDefaultHeaders())) {
            headers.putAll(defaultConfig.getDefaultHeaders());
        }
        if (MapUtil.isNotEmpty(source.getDefaultHeaders())) {
            headers.putAll(source.getDefaultHeaders());
        }
        merged.setDefaultHeaders(headers);

        merged.setProxy(ObjectUtil.defaultIfNull(source.getProxy(), defaultConfig.getProxy()));
        merged.setSsl(ObjectUtil.defaultIfNull(source.getSsl(), defaultConfig.getSsl()));

        return merged;
    }

    /**
     * 远程调用日志配置
     *
     * @author Ateng
     * @since 2026-04-29
     */
    @Data
    public static class LoggingConfig {

        private Boolean enabled = true;

        private Boolean includeHeaders = true;

        private Boolean includeRequestBody = false;

        @Min(value = 0, message = "请求体日志最大长度不能小于 0")
        private Integer maxRequestBodyLength = 2048;

        private List<String> sensitiveHeaders = List.of(
                "Authorization",
                "Cookie",
                "Set-Cookie",
                "X-API-Key",
                "X-App-Secret"
        );

    }

    /**
     * TraceId 配置
     *
     * @author Ateng
     * @since 2026-04-29
     */
    @Data
    public static class TraceConfig {

        private Boolean enabled = true;

        private String mdcName = RemoteHttpConstant.MDC_TRACE_ID;

        private String headerName = RemoteHttpConstant.DEFAULT_TRACE_HEADER;

        private Boolean generateIfAbsent = true;

    }

    /**
     * 错误处理配置
     *
     * @author Ateng
     * @since 2026-04-29
     */
    @Data
    public static class ErrorHandlingConfig {

        private Boolean enabled = true;

        @Min(value = 0, message = "错误响应体最大长度不能小于 0")
        private Integer maxResponseBodyLength = 4096;

    }

    /**
     * HTTP 客户端基础配置
     *
     * @author Ateng
     * @since 2026-04-29
     */
    @Data
    public static class ClientConfig {

        private String baseUrl;

        private Duration connectTimeout = Duration.ofSeconds(3);

        private Duration connectionRequestTimeout = Duration.ofSeconds(2);

        private Duration responseTimeout = Duration.ofSeconds(10);

        private Duration maxIdleTime = Duration.ofSeconds(30);

        @Min(value = 1, message = "总连接数必须大于 0")
        private Integer maxConnTotal = 200;

        @Min(value = 1, message = "单路由连接数必须大于 0")
        private Integer maxConnPerRoute = 50;

        private Map<String, String> defaultHeaders = new LinkedHashMap<>();

        @Valid
        private ProxyConfig proxy = new ProxyConfig();

        @Valid
        private SslConfig ssl = new SslConfig();

    }

    /**
     * HTTP 代理配置
     *
     * @author Ateng
     * @since 2026-04-29
     */
    @Data
    public static class ProxyConfig {

        private Boolean enabled = false;

        private String scheme = "http";

        private String host;

        private Integer port = 0;

        private String username;

        private String password;

    }

    /**
     * SSL 配置
     *
     * @author Ateng
     * @since 2026-04-29
     */
    @Data
    public static class SslConfig {

        private Boolean enabled = false;

        private Boolean trustAll = false;

        private Boolean hostnameVerificationEnabled = true;

        private String protocol = "TLS";

        private String keyStorePath;

        private String keyStorePassword;

        private String keyStoreType = "PKCS12";

        private String trustStorePath;

        private String trustStorePassword;

        private String trustStoreType = "PKCS12";

    }

}
```

### 扩展接口

这两个扩展接口是为了保证后续功能不修改主工厂结构。

`RemoteRestClientBuilderCustomizer` 用于扩展 `RestClient.Builder`，适合 Bearer Token、API Key、签名、XML MessageConverter 等功能。

文件位置：`src/main/java/io/github/atengk/http/extension/RemoteRestClientBuilderCustomizer.java`

```java
package io.github.atengk.http.extension;

import io.github.atengk.http.config.RemoteHttpProperties;
import org.springframework.web.client.RestClient;

/**
 * RestClient Builder 扩展器
 *
 * @author Ateng
 * @since 2026-04-29
 */
public interface RemoteRestClientBuilderCustomizer {

    /**
     * 是否支持当前客户端
     *
     * @param clientName   客户端名称
     * @param clientConfig 客户端配置
     * @return 是否支持
     */
    default boolean supports(String clientName, RemoteHttpProperties.ClientConfig clientConfig) {
        return true;
    }

    /**
     * 自定义 RestClient Builder
     *
     * @param clientName   客户端名称
     * @param clientConfig 客户端配置
     * @param builder      RestClient Builder
     */
    void customize(String clientName, RemoteHttpProperties.ClientConfig clientConfig, RestClient.Builder builder);

}
```

`RemoteRequestFactoryCustomizer` 用于扩展底层请求工厂，适合代理、mTLS、忽略 SSL、自定义 Apache HttpClient 等功能。

文件位置：`src/main/java/io/github/atengk/http/extension/RemoteRequestFactoryCustomizer.java`

```java
package io.github.atengk.http.extension;

import io.github.atengk.http.config.RemoteHttpProperties;
import org.springframework.http.client.ClientHttpRequestFactory;

import java.util.function.Supplier;

/**
 * RestClient 请求工厂扩展器
 *
 * @author Ateng
 * @since 2026-04-29
 */
public interface RemoteRequestFactoryCustomizer {

    /**
     * 是否支持当前客户端
     *
     * @param clientName   客户端名称
     * @param clientConfig 客户端配置
     * @return 是否支持
     */
    default boolean supports(String clientName, RemoteHttpProperties.ClientConfig clientConfig) {
        return true;
    }

    /**
     * 自定义请求工厂
     *
     * @param clientName             客户端名称
     * @param clientConfig           客户端配置
     * @param requestFactorySupplier 默认请求工厂供应器
     * @return 请求工厂
     */
    ClientHttpRequestFactory customize(
            String clientName,
            RemoteHttpProperties.ClientConfig clientConfig,
            Supplier<ClientHttpRequestFactory> requestFactorySupplier
    );

}
```

### 统一异常类

这个异常类用于包装远程 HTTP 调用失败信息。业务层只需要捕获 `RemoteCallException`，不需要关心底层 HTTP 异常类型。

文件位置：`src/main/java/io/github/atengk/http/exception/RemoteCallException.java`

```java
package io.github.atengk.http.exception;

import lombok.Getter;
import org.springframework.http.HttpStatusCode;

import java.net.URI;

/**
 * 远程调用异常
 *
 * @author Ateng
 * @since 2026-04-29
 */
@Getter
public class RemoteCallException extends RuntimeException {

    private final URI uri;

    private final HttpStatusCode statusCode;

    private final String responseBody;

    public RemoteCallException(String message, URI uri, HttpStatusCode statusCode, String responseBody) {
        super(message);
        this.uri = uri;
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

    public RemoteCallException(String message, URI uri, HttpStatusCode statusCode, String responseBody, Throwable cause) {
        super(message, cause);
        this.uri = uri;
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

}
```

### 统一错误处理器

这个处理器统一处理 HTTP 4xx、5xx 响应，并转换成 `RemoteCallException`。错误响应体会按配置截断，避免日志或异常对象过大。

文件位置：`src/main/java/io/github/atengk/http/handler/RemoteHttpErrorHandler.java`

```java
package io.github.atengk.http.handler;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.http.config.RemoteHttpProperties;
import io.github.atengk.http.exception.RemoteCallException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 远程 HTTP 错误处理器
 *
 * @author Ateng
 * @since 2026-04-29
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RemoteHttpErrorHandler implements RestClient.ResponseSpec.ErrorHandler {

    private final RemoteHttpProperties remoteHttpProperties;

    /**
     * 处理异常响应
     *
     * @param request  请求对象
     * @param response 响应对象
     * @throws IOException IO 异常
     */
    @Override
    public void handle(HttpRequest request, ClientHttpResponse response) throws IOException {
        String responseBody = readResponseBody(response);
        String message = StrUtil.format(
                "远程接口调用失败，method={}，uri={}，status={}",
                request.getMethod(),
                request.getURI(),
                response.getStatusCode()
        );

        log.warn("{}，responseBody={}", message, responseBody);

        throw new RemoteCallException(
                message,
                request.getURI(),
                response.getStatusCode(),
                responseBody
        );
    }

    /**
     * 读取错误响应体
     *
     * @param response 响应对象
     * @return 错误响应体
     * @throws IOException IO 异常
     */
    private String readResponseBody(ClientHttpResponse response) throws IOException {
        String responseBody = IoUtil.read(response.getBody(), StandardCharsets.UTF_8);
        int maxLength = remoteHttpProperties.getErrorHandling().getMaxResponseBodyLength();

        if (maxLength <= 0) {
            return "";
        }

        return CharSequenceUtil.subPre(responseBody, maxLength);
    }

}
```

### TraceId 拦截器

这个拦截器从 MDC 中读取 TraceId，并自动写入远程请求头。如果当前线程没有 TraceId，且配置允许自动生成，则会创建一个新的 TraceId。

文件位置：`src/main/java/io/github/atengk/http/interceptor/RestClientTraceInterceptor.java`

```java
package io.github.atengk.http.interceptor;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.http.config.RemoteHttpProperties;
import io.github.atengk.http.constant.RemoteHttpConstant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * RestClient TraceId 拦截器
 *
 * @author Ateng
 * @since 2026-04-29
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RestClientTraceInterceptor implements ClientHttpRequestInterceptor {

    private final RemoteHttpProperties remoteHttpProperties;

    /**
     * 追加 TraceId 请求头
     *
     * @param request   请求对象
     * @param body      请求体
     * @param execution 执行器
     * @return 响应对象
     * @throws IOException IO 异常
     */
    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        RemoteHttpProperties.TraceConfig trace = remoteHttpProperties.getTrace();

        String mdcName = StrUtil.blankToDefault(trace.getMdcName(), RemoteHttpConstant.MDC_TRACE_ID);
        String headerName = StrUtil.blankToDefault(trace.getHeaderName(), RemoteHttpConstant.DEFAULT_TRACE_HEADER);

        String traceId = MDC.get(mdcName);
        if (StrUtil.isBlank(traceId) && Boolean.TRUE.equals(trace.getGenerateIfAbsent())) {
            traceId = IdUtil.fastSimpleUUID();
            MDC.put(mdcName, traceId);
            log.debug("远程调用生成新的 TraceId，traceId={}", traceId);
        }

        if (StrUtil.isNotBlank(traceId)) {
            request.getHeaders().set(headerName, traceId);
        }

        return execution.execute(request, body);
    }

}
```

### 日志拦截器

这个拦截器记录远程接口调用的方法、URI、状态码、耗时、请求头和请求体。请求体默认关闭，敏感 Header 会自动脱敏。

文件位置：`src/main/java/io/github/atengk/http/interceptor/RestClientLogInterceptor.java`

```java
package io.github.atengk.http.interceptor;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.http.config.RemoteHttpProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * RestClient 日志拦截器
 *
 * @author Ateng
 * @since 2026-04-29
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RestClientLogInterceptor implements ClientHttpRequestInterceptor {

    private static final String MASK_VALUE = "******";

    private final RemoteHttpProperties remoteHttpProperties;

    /**
     * 记录远程 HTTP 调用日志
     *
     * @param request   请求对象
     * @param body      请求体
     * @param execution 执行器
     * @return 响应对象
     * @throws IOException IO 异常
     */
    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        long startTime = System.nanoTime();

        try {
            ClientHttpResponse response = execution.execute(request, body);
            long costMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);

            log.info(
                    "远程接口调用完成，method={}，uri={}，status={}，cost={}ms，headers={}，requestBody={}",
                    request.getMethod(),
                    request.getURI(),
                    response.getStatusCode(),
                    costMillis,
                    buildHeadersLog(request.getHeaders()),
                    buildRequestBodyLog(body)
            );

            return response;
        } catch (IOException ex) {
            long costMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);

            log.warn(
                    "远程接口调用IO异常，method={}，uri={}，cost={}ms，message={}",
                    request.getMethod(),
                    request.getURI(),
                    costMillis,
                    ex.getMessage()
            );

            throw ex;
        } catch (RuntimeException ex) {
            long costMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);

            log.warn(
                    "远程接口调用运行时异常，method={}，uri={}，cost={}ms，message={}",
                    request.getMethod(),
                    request.getURI(),
                    costMillis,
                    ex.getMessage()
            );

            throw ex;
        }
    }

    /**
     * 构建请求头日志
     *
     * @param headers 请求头
     * @return 请求头日志
     */
    private Object buildHeadersLog(HttpHeaders headers) {
        RemoteHttpProperties.LoggingConfig logging = remoteHttpProperties.getLogging();
        if (!Boolean.TRUE.equals(logging.getIncludeHeaders())) {
            return "disabled";
        }

        Map<String, List<String>> result = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            String headerName = entry.getKey();
            if (isSensitiveHeader(headerName, logging.getSensitiveHeaders())) {
                result.put(headerName, List.of(MASK_VALUE));
            } else {
                result.put(headerName, entry.getValue());
            }
        }
        return result;
    }

    /**
     * 构建请求体日志
     *
     * @param body 请求体字节数组
     * @return 请求体日志
     */
    private String buildRequestBodyLog(byte[] body) {
        RemoteHttpProperties.LoggingConfig logging = remoteHttpProperties.getLogging();
        if (!Boolean.TRUE.equals(logging.getIncludeRequestBody())) {
            return "disabled";
        }
        if (body == null || body.length == 0) {
            return "";
        }

        int maxLength = logging.getMaxRequestBodyLength();
        if (maxLength <= 0) {
            return "";
        }

        String text = StrUtil.str(body, StandardCharsets.UTF_8);
        return CharSequenceUtil.subPre(text, maxLength);
    }

    /**
     * 判断是否敏感请求头
     *
     * @param headerName       请求头名称
     * @param sensitiveHeaders 敏感请求头列表
     * @return 是否敏感
     */
    private boolean isSensitiveHeader(String headerName, List<String> sensitiveHeaders) {
        if (StrUtil.isBlank(headerName) || CollUtil.isEmpty(sensitiveHeaders)) {
            return false;
        }

        String lowerHeaderName = headerName.toLowerCase(Locale.ROOT);
        return sensitiveHeaders.stream()
                .filter(StrUtil::isNotBlank)
                .map(item -> item.toLowerCase(Locale.ROOT))
                .anyMatch(item -> StrUtil.equals(lowerHeaderName, item));
    }

}
```

### RestClient 工厂

这是所有 `RestClient Bean` 的统一创建入口。这里已经预留两个扩展点：

```text
1. RemoteRequestFactoryCustomizer
   用于扩展代理、mTLS、忽略 SSL、自定义底层请求工厂

2. RemoteRestClientBuilderCustomizer
   用于扩展认证、签名、XML MessageConverter、自定义 Header、拦截器
```

文件位置：`src/main/java/io/github/atengk/http/config/RemoteRestClientFactory.java`

```java
package io.github.atengk.http.config;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.http.extension.RemoteRequestFactoryCustomizer;
import io.github.atengk.http.extension.RemoteRestClientBuilderCustomizer;
import io.github.atengk.http.handler.RemoteHttpErrorHandler;
import io.github.atengk.http.interceptor.RestClientLogInterceptor;
import io.github.atengk.http.interceptor.RestClientTraceInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 远程 RestClient 工厂
 *
 * @author Ateng
 * @since 2026-04-29
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RemoteRestClientFactory implements DisposableBean {

    private final RestClient.Builder restClientBuilder;

    private final RemoteHttpProperties remoteHttpProperties;

    private final RemoteHttpErrorHandler remoteHttpErrorHandler;

    private final RestClientLogInterceptor restClientLogInterceptor;

    private final RestClientTraceInterceptor restClientTraceInterceptor;

    private final List<RemoteRequestFactoryCustomizer> requestFactoryCustomizers;

    private final List<RemoteRestClientBuilderCustomizer> builderCustomizers;

    private final List<CloseableHttpClient> httpClients = new CopyOnWriteArrayList<>();

    /**
     * 创建 RestClient
     *
     * @param clientName 客户端名称
     * @return RestClient
     */
    public RestClient create(String clientName) {
        return create(clientName, null);
    }

    /**
     * 创建 RestClient
     *
     * @param clientName 客户端名称
     * @param customizer 单个客户端的临时扩展逻辑
     * @return RestClient
     */
    public RestClient create(String clientName, Consumer<RestClient.Builder> customizer) {
        RemoteHttpProperties.ClientConfig clientConfig = remoteHttpProperties.getMergedClient(clientName);

        RestClient.Builder builder = restClientBuilder.clone()
                .requestFactory(createRequestFactory(clientName, clientConfig));

        applyBaseUrl(builder, clientConfig);
        applyDefaultHeaders(builder, clientConfig);
        applyErrorHandler(builder);
        applyTraceInterceptor(builder);
        applyLogInterceptor(builder);
        applyBuilderCustomizers(clientName, clientConfig, builder);

        if (customizer != null) {
            customizer.accept(builder);
        }

        log.info(
                "RestClient 创建完成，clientName={}，baseUrl={}，connectTimeout={}，responseTimeout={}",
                clientName,
                clientConfig.getBaseUrl(),
                clientConfig.getConnectTimeout(),
                clientConfig.getResponseTimeout()
        );

        return builder.build();
    }

    /**
     * 设置基础地址
     *
     * @param builder      RestClient Builder
     * @param clientConfig 客户端配置
     */
    private void applyBaseUrl(RestClient.Builder builder, RemoteHttpProperties.ClientConfig clientConfig) {
        if (StrUtil.isNotBlank(clientConfig.getBaseUrl())) {
            builder.baseUrl(clientConfig.getBaseUrl());
        }
    }

    /**
     * 设置默认请求头
     *
     * @param builder      RestClient Builder
     * @param clientConfig 客户端配置
     */
    private void applyDefaultHeaders(RestClient.Builder builder, RemoteHttpProperties.ClientConfig clientConfig) {
        if (MapUtil.isEmpty(clientConfig.getDefaultHeaders())) {
            return;
        }

        for (Map.Entry<String, String> entry : clientConfig.getDefaultHeaders().entrySet()) {
            builder.defaultHeader(entry.getKey(), entry.getValue());
        }
    }

    /**
     * 设置统一错误处理器
     *
     * @param builder RestClient Builder
     */
    private void applyErrorHandler(RestClient.Builder builder) {
        if (Boolean.TRUE.equals(remoteHttpProperties.getErrorHandling().getEnabled())) {
            builder.defaultStatusHandler(HttpStatusCode::isError, remoteHttpErrorHandler);
        }
    }

    /**
     * 设置 TraceId 拦截器
     *
     * @param builder RestClient Builder
     */
    private void applyTraceInterceptor(RestClient.Builder builder) {
        if (Boolean.TRUE.equals(remoteHttpProperties.getTrace().getEnabled())) {
            builder.requestInterceptor(restClientTraceInterceptor);
        }
    }

    /**
     * 设置日志拦截器
     *
     * @param builder RestClient Builder
     */
    private void applyLogInterceptor(RestClient.Builder builder) {
        if (Boolean.TRUE.equals(remoteHttpProperties.getLogging().getEnabled())) {
            builder.requestInterceptor(restClientLogInterceptor);
        }
    }

    /**
     * 应用 Builder 扩展器
     *
     * @param clientName   客户端名称
     * @param clientConfig 客户端配置
     * @param builder      RestClient Builder
     */
    private void applyBuilderCustomizers(
            String clientName,
            RemoteHttpProperties.ClientConfig clientConfig,
            RestClient.Builder builder
    ) {
        for (RemoteRestClientBuilderCustomizer builderCustomizer : builderCustomizers) {
            if (builderCustomizer.supports(clientName, clientConfig)) {
                builderCustomizer.customize(clientName, clientConfig, builder);
            }
        }
    }

    /**
     * 创建请求工厂
     *
     * @param clientName   客户端名称
     * @param clientConfig 客户端配置
     * @return 请求工厂
     */
    private ClientHttpRequestFactory createRequestFactory(
            String clientName,
            RemoteHttpProperties.ClientConfig clientConfig
    ) {
        Supplier<ClientHttpRequestFactory> supplier = () -> createDefaultRequestFactory(clientConfig);

        for (RemoteRequestFactoryCustomizer requestFactoryCustomizer : requestFactoryCustomizers) {
            if (requestFactoryCustomizer.supports(clientName, clientConfig)) {
                Supplier<ClientHttpRequestFactory> previousSupplier = supplier;
                supplier = () -> requestFactoryCustomizer.customize(clientName, clientConfig, previousSupplier);
            }
        }

        return supplier.get();
    }

    /**
     * 创建默认请求工厂
     *
     * @param clientConfig 客户端配置
     * @return 请求工厂
     */
    private ClientHttpRequestFactory createDefaultRequestFactory(RemoteHttpProperties.ClientConfig clientConfig) {
        ConnectionConfig connectionConfig = ConnectionConfig.custom()
                .setConnectTimeout(toTimeout(clientConfig.getConnectTimeout()))
                .build();

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(toTimeout(clientConfig.getConnectionRequestTimeout()))
                .setResponseTimeout(toTimeout(clientConfig.getResponseTimeout()))
                .build();

        HttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setDefaultConnectionConfig(connectionConfig)
                .setMaxConnTotal(clientConfig.getMaxConnTotal())
                .setMaxConnPerRoute(clientConfig.getMaxConnPerRoute())
                .build();

        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .evictExpiredConnections()
                .evictIdleConnections(TimeValue.ofMilliseconds(clientConfig.getMaxIdleTime().toMillis()))
                .disableAutomaticRetries()
                .build();

        httpClients.add(httpClient);

        return new HttpComponentsClientHttpRequestFactory(httpClient);
    }

    /**
     * 转换超时时间
     *
     * @param duration Duration
     * @return Timeout
     */
    private Timeout toTimeout(Duration duration) {
        return Timeout.ofMilliseconds(duration.toMillis());
    }

    /**
     * 销毁底层 HTTP 客户端
     */
    @Override
    public void destroy() {
        for (CloseableHttpClient httpClient : httpClients) {
            try {
                httpClient.close();
            } catch (IOException ex) {
                log.warn("关闭 HttpClient 失败，message={}", ex.getMessage());
            }
        }
    }

}
```

### 基础 RestClient Bean

这里创建三个基础 Bean。注意：`remoteRestClientFactory.create(...)` 的客户端名称必须使用 `RemoteHttpConstant` 常量。

文件位置：`src/main/java/io/github/atengk/http/config/RestClientBaseConfig.java`

```java
package io.github.atengk.http.config;

import io.github.atengk.http.constant.RemoteHttpConstant;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * RestClient 基础配置
 *
 * @author Ateng
 * @since 2026-04-29
 */
@Configuration
@EnableConfigurationProperties(RemoteHttpProperties.class)
public class RestClientBaseConfig {

    /**
     * 默认 RestClient
     *
     * @param remoteRestClientFactory RestClient 工厂
     * @return RestClient
     */
    @Bean
    public RestClient defaultRestClient(RemoteRestClientFactory remoteRestClientFactory) {
        return remoteRestClientFactory.create(RemoteHttpConstant.CLIENT_DEFAULT);
    }

    /**
     * 快速失败 RestClient
     *
     * @param remoteRestClientFactory RestClient 工厂
     * @return RestClient
     */
    @Bean
    public RestClient fastTimeoutRestClient(RemoteRestClientFactory remoteRestClientFactory) {
        return remoteRestClientFactory.create(RemoteHttpConstant.CLIENT_FAST);
    }

    /**
     * 长超时 RestClient
     *
     * @param remoteRestClientFactory RestClient 工厂
     * @return RestClient
     */
    @Bean
    public RestClient longTimeoutRestClient(RemoteRestClientFactory remoteRestClientFactory) {
        return remoteRestClientFactory.create(RemoteHttpConstant.CLIENT_LONG);
    }

}
```

### 远程调用执行器

这个执行器先做一层轻量封装，当前默认直接执行。后续实现 `retryRestClient`、`circuitBreakerRestClient` 时，可以基于这个执行器扩展重试和熔断能力，而不是把稳定性逻辑散落在每个 `XxxApiClient` 中。

文件位置：`src/main/java/io/github/atengk/http/support/RemoteCallExecutor.java`

```java
package io.github.atengk.http.support;

import java.util.function.Supplier;

/**
 * 远程调用执行器
 *
 * @author Ateng
 * @since 2026-04-29
 */
public interface RemoteCallExecutor {

    /**
     * 执行远程调用
     *
     * @param clientName 客户端名称
     * @param supplier   调用逻辑
     * @param <T>        返回类型
     * @return 调用结果
     */
    <T> T execute(String clientName, Supplier<T> supplier);

    /**
     * 执行无返回值远程调用
     *
     * @param clientName 客户端名称
     * @param runnable   调用逻辑
     */
    default void execute(String clientName, Runnable runnable) {
        execute(clientName, () -> {
            runnable.run();
            return null;
        });
    }

}
```

基础阶段：
DefaultRemoteCallExecutor 使用 @Component 直接注册，不使用 @ConditionalOnMissingBean。

后续接入重试 / 熔断时：
不要再新增多个 RemoteCallExecutor 实现同时注册为 Bean。
应该把默认执行器替换为一个组合执行器，例如 ResilienceRemoteCallExecutor。

文件位置：`src/main/java/io/github/atengk/http/support/DefaultRemoteCallExecutor.java`

```java
package io.github.atengk.http.support;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/**
 * 默认远程调用执行器
 *
 * @author Ateng
 * @since 2026-04-29
 */
@Slf4j
@Component
public class DefaultRemoteCallExecutor implements RemoteCallExecutor {

    /**
     * 执行远程调用
     *
     * @param clientName 客户端名称
     * @param supplier   调用逻辑
     * @param <T>        返回类型
     * @return 调用结果
     */
    @Override
    public <T> T execute(String clientName, Supplier<T> supplier) {
        return supplier.get();
    }

}
```

### 基础调用示例

业务代码建议封装成具体的 `XxxApiClient`。这里演示通过 `defaultRestClient` 调用普通 JSON 接口，并使用 `RemoteCallExecutor` 预留后续重试、熔断扩展能力。

文件位置：`src/main/java/io/github/atengk/http/client/DemoApiClient.java`

```java
package io.github.atengk.http.client;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.http.constant.RemoteHttpConstant;
import io.github.atengk.http.exception.RemoteCallException;
import io.github.atengk.http.support.RemoteCallExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * 示例远程接口客户端
 *
 * @author Ateng
 * @since 2026-04-29
 */
@Slf4j
@Component
public class DemoApiClient {

    private final RestClient restClient;

    private final RemoteCallExecutor remoteCallExecutor;

    public DemoApiClient(
            @Qualifier("defaultRestClient") RestClient restClient,
            RemoteCallExecutor remoteCallExecutor
    ) {
        this.restClient = restClient;
        this.remoteCallExecutor = remoteCallExecutor;
    }

    /**
     * 查询用户详情
     *
     * @param userId 用户ID
     * @return 用户详情
     */
    public UserDetailResp getUserDetail(String userId) {
        if (StrUtil.isBlank(userId)) {
            throw new IllegalArgumentException("用户ID不能为空");
        }

        try {
            return remoteCallExecutor.execute(RemoteHttpConstant.CLIENT_DEFAULT, () -> restClient.get()
                    .uri("/api/users/{userId}", userId)
                    .retrieve()
                    .body(UserDetailResp.class));
        } catch (RemoteCallException ex) {
            log.warn(
                    "查询远程用户详情失败，userId={}，status={}，responseBody={}",
                    userId,
                    ex.getStatusCode(),
                    ex.getResponseBody()
            );
            throw ex;
        }
    }

    /**
     * 创建用户
     *
     * @param request 创建用户请求
     * @return 创建结果
     */
    public UserCreateResp createUser(UserCreateReq request) {
        return remoteCallExecutor.execute(RemoteHttpConstant.CLIENT_DEFAULT, () -> restClient.post()
                .uri("/api/users")
                .body(request)
                .retrieve()
                .body(UserCreateResp.class));
    }

    public record UserDetailResp(Long id, String username, String nickname) {
    }

    public record UserCreateReq(String username, String nickname) {
    }

    public record UserCreateResp(Long id, Boolean success) {
    }

}
```

### 后续功能扩展规则

后续新增普通客户端时，先在常量类中增加客户端名称：

```java
public static final String CLIENT_USER_API = "user-api";
```

然后在 `application.yml` 中增加配置：

```yaml
remote:
  http:
    clients:
      user-api:
        base-url: https://user-api.example.com
        connect-timeout: 3s
        connection-request-timeout: 2s
        response-timeout: 10s
        max-idle-time: 30s
        max-conn-total: 200
        max-conn-per-route: 50
        default-headers:
          Accept: application/json
```

最后在配置类中增加 Bean：

```java
@Bean
public RestClient userApiRestClient(RemoteRestClientFactory remoteRestClientFactory) {
    return remoteRestClientFactory.create(RemoteHttpConstant.CLIENT_USER_API);
}
```

后续不同功能的扩展方式固定如下：

```text
1. Bearer Token
   使用 RemoteRestClientBuilderCustomizer 或 create(clientName, builder -> ...)
   追加 Authorization Header

2. API Key
   使用 RemoteRestClientBuilderCustomizer 或拦截器追加 Header / Query 参数

3. AK/SK 签名
   使用 ClientHttpRequestInterceptor 计算签名

4. 文件上传
   使用普通 RestClient，调用层构造 multipart/form-data 请求体

5. 文件下载
   使用普通 RestClient，调用层接收 byte[] / Resource / exchange

6. 重试
   扩展 RemoteCallExecutor，不改 RestClient 工厂

7. 熔断
   扩展 RemoteCallExecutor，不改 RestClient 工厂

8. 代理
   实现 RemoteRequestFactoryCustomizer，不改 RestClient 工厂

9. mTLS
   实现 RemoteRequestFactoryCustomizer，不改 RestClient 工厂

10. 忽略 SSL
   实现 RemoteRequestFactoryCustomizer，不改 RestClient 工厂

11. XML
   实现 RemoteRestClientBuilderCustomizer 增加 XML MessageConverter
```

这一版基础配置已经把后续 17 个功能需要的主扩展点预留好，并且 `RestClientBaseConfig` 中的客户端名称已经统一改为常量。



## 基础调用示例

这一节补充 `defaultRestClient` 的基础调用示例。它覆盖日常 HTTP 开发中最常见的场景：

```text
1. GET 路径参数
2. GET Query 参数
3. POST JSON
4. PUT JSON
5. DELETE
6. 自定义 Header
7. 读取 ResponseEntity 响应头
8. 使用 exchange 手动处理响应
```

这里统一使用基础配置中已经创建好的 `defaultRestClient`。日志、TraceId、统一错误处理、超时、连接池都来自前面的基础配置。

### 默认客户端调用类

这个客户端类封装远程接口调用，业务 Service 或 Controller 不直接拼接远程 URL。

文件位置：`src/main/java/io/github/atengk/http/client/DefaultRestClientApiClient.java`

```java
package io.github.atengk.http.client;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.atengk.http.constant.RemoteHttpConstant;
import io.github.atengk.http.exception.RemoteCallException;
import io.github.atengk.http.support.RemoteCallContext;
import io.github.atengk.http.support.RemoteCallExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 默认 RestClient 调用示例客户端
 *
 * @author Ateng
 * @since 2026-04-29
 */
@Slf4j
@Component
public class DefaultRestClientApiClient {

    private final RestClient restClient;

    private final RemoteCallExecutor remoteCallExecutor;

    private final ObjectMapper objectMapper;

    public DefaultRestClientApiClient(
            @Qualifier("defaultRestClient") RestClient restClient,
            RemoteCallExecutor remoteCallExecutor,
            ObjectMapper objectMapper
    ) {
        this.restClient = restClient;
        this.remoteCallExecutor = remoteCallExecutor;
        this.objectMapper = objectMapper;
    }

    /**
     * GET 路径参数：查询用户详情
     *
     * @param userId 用户ID
     * @return 用户详情
     */
    public UserDetailResp getUserDetail(String userId) {
        if (StrUtil.isBlank(userId)) {
            throw new IllegalArgumentException("用户ID不能为空");
        }

        RemoteCallContext context = RemoteCallContext.builder()
                .clientName(RemoteHttpConstant.CLIENT_DEFAULT)
                .method("GET")
                .description("查询用户详情")
                .idempotent(true)
                .build();

        log.info("开始查询用户详情，userId={}", userId);

        return remoteCallExecutor.execute(context, () -> restClient.get()
                .uri("/api/users/{userId}", userId)
                .retrieve()
                .body(UserDetailResp.class));
    }

    /**
     * GET Query 参数：分页查询用户
     *
     * @param request 查询条件
     * @return 用户分页数据
     */
    public UserPageResp searchUsers(UserSearchReq request) {
        UserSearchReq safeRequest = ObjectUtil.defaultIfNull(request, UserSearchReq.empty());

        RemoteCallContext context = RemoteCallContext.builder()
                .clientName(RemoteHttpConstant.CLIENT_DEFAULT)
                .method("GET")
                .description("分页查询用户")
                .idempotent(true)
                .build();

        log.info(
                "开始分页查询用户，keyword={}，pageNum={}，pageSize={}",
                safeRequest.keyword(),
                safeRequest.pageNum(),
                safeRequest.pageSize()
        );

        return remoteCallExecutor.execute(context, () -> restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/users")
                        .queryParamIfPresent("keyword", StrUtil.blankToOptional(safeRequest.keyword()))
                        .queryParam("pageNum", ObjectUtil.defaultIfNull(safeRequest.pageNum(), 1))
                        .queryParam("pageSize", ObjectUtil.defaultIfNull(safeRequest.pageSize(), 10))
                        .build())
                .retrieve()
                .body(UserPageResp.class));
    }

    /**
     * POST JSON：创建用户
     *
     * @param request 创建用户请求
     * @return 创建结果
     */
    public UserCreateResp createUser(UserCreateReq request) {
        if (request == null || StrUtil.isBlank(request.username())) {
            throw new IllegalArgumentException("用户名不能为空");
        }

        RemoteCallContext context = RemoteCallContext.builder()
                .clientName(RemoteHttpConstant.CLIENT_DEFAULT)
                .method("POST")
                .description("创建用户")
                .idempotent(false)
                .build();

        log.info("开始创建用户，username={}", request.username());

        return remoteCallExecutor.execute(context, () -> restClient.post()
                .uri("/api/users")
                .body(request)
                .retrieve()
                .body(UserCreateResp.class));
    }

    /**
     * PUT JSON：更新用户
     *
     * @param userId  用户ID
     * @param request 更新用户请求
     * @return 更新结果
     */
    public UserUpdateResp updateUser(String userId, UserUpdateReq request) {
        if (StrUtil.isBlank(userId)) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        if (request == null) {
            throw new IllegalArgumentException("更新请求不能为空");
        }

        RemoteCallContext context = RemoteCallContext.builder()
                .clientName(RemoteHttpConstant.CLIENT_DEFAULT)
                .method("PUT")
                .description("更新用户")
                .idempotent(true)
                .build();

        log.info("开始更新用户，userId={}", userId);

        return remoteCallExecutor.execute(context, () -> restClient.put()
                .uri("/api/users/{userId}", userId)
                .body(request)
                .retrieve()
                .body(UserUpdateResp.class));
    }

    /**
     * DELETE：删除用户
     *
     * @param userId 用户ID
     * @return 是否删除成功
     */
    public Boolean deleteUser(String userId) {
        if (StrUtil.isBlank(userId)) {
            throw new IllegalArgumentException("用户ID不能为空");
        }

        RemoteCallContext context = RemoteCallContext.builder()
                .clientName(RemoteHttpConstant.CLIENT_DEFAULT)
                .method("DELETE")
                .description("删除用户")
                .idempotent(true)
                .build();

        log.info("开始删除用户，userId={}", userId);

        return remoteCallExecutor.execute(context, () -> restClient.delete()
                .uri("/api/users/{userId}", userId)
                .retrieve()
                .body(Boolean.class));
    }

    /**
     * 自定义 Header：查询租户配置
     *
     * @param tenantId 租户ID
     * @return 租户配置
     */
    public TenantConfigResp getTenantConfig(String tenantId) {
        if (StrUtil.isBlank(tenantId)) {
            throw new IllegalArgumentException("租户ID不能为空");
        }

        RemoteCallContext context = RemoteCallContext.builder()
                .clientName(RemoteHttpConstant.CLIENT_DEFAULT)
                .method("GET")
                .description("查询租户配置")
                .idempotent(true)
                .build();

        log.info("开始查询租户配置，tenantId={}", tenantId);

        return remoteCallExecutor.execute(context, () -> restClient.get()
                .uri("/api/tenant/config")
                .header("X-Tenant-Id", tenantId)
                .retrieve()
                .body(TenantConfigResp.class));
    }

    /**
     * ResponseEntity：查询订单状态并读取响应头
     *
     * @param orderNo 订单号
     * @return 订单状态
     */
    public OrderStatusResp getOrderStatus(String orderNo) {
        if (StrUtil.isBlank(orderNo)) {
            throw new IllegalArgumentException("订单号不能为空");
        }

        RemoteCallContext context = RemoteCallContext.builder()
                .clientName(RemoteHttpConstant.CLIENT_DEFAULT)
                .method("GET")
                .description("查询订单状态")
                .idempotent(true)
                .build();

        log.info("开始查询订单状态，orderNo={}", orderNo);

        ResponseEntity<OrderStatusResp> responseEntity = remoteCallExecutor.execute(context, () -> restClient.get()
                .uri("/api/orders/{orderNo}/status", orderNo)
                .retrieve()
                .toEntity(OrderStatusResp.class));

        HttpHeaders headers = responseEntity.getHeaders();
        log.info(
                "订单状态查询完成，orderNo={}，statusCode={}，requestId={}",
                orderNo,
                responseEntity.getStatusCode(),
                headers.getFirst("X-Request-Id")
        );

        return responseEntity.getBody();
    }

    /**
     * exchange：手动处理响应
     *
     * @return 健康检查结果
     */
    public HealthResp checkHealthByExchange() {
        RemoteCallContext context = RemoteCallContext.builder()
                .clientName(RemoteHttpConstant.CLIENT_DEFAULT)
                .method("GET")
                .description("健康检查")
                .idempotent(true)
                .build();

        log.info("开始调用远程健康检查接口");

        return remoteCallExecutor.execute(context, () -> restClient.get()
                .uri("/api/health")
                .exchange((request, response) -> {
                    if (response.getStatusCode().isError()) {
                        throw buildRemoteCallException(request, response);
                    }

                    if (response.getStatusCode().value() == 204) {
                        return new HealthResp("UNKNOWN", "远程服务无响应体");
                    }

                    return objectMapper.readValue(response.getBody(), HealthResp.class);
                }));
    }

    /**
     * 构建远程调用异常
     *
     * @param request  请求对象
     * @param response 响应对象
     * @return 远程调用异常
     * @throws IOException IO 异常
     */
    private RemoteCallException buildRemoteCallException(HttpRequest request, ClientHttpResponse response) throws IOException {
        String responseBody = IoUtil.read(response.getBody(), StandardCharsets.UTF_8);
        String message = StrUtil.format(
                "远程接口调用失败，method={}，uri={}，status={}",
                request.getMethod(),
                request.getURI(),
                response.getStatusCode()
        );

        log.warn("{}，responseBody={}", message, responseBody);

        return new RemoteCallException(
                message,
                request.getURI(),
                response.getStatusCode(),
                responseBody
        );
    }

    /**
     * 用户查询请求
     *
     * @author Ateng
     * @since 2026-04-29
     */
    public record UserSearchReq(String keyword, Integer pageNum, Integer pageSize) {

        /**
         * 创建空查询条件
         *
         * @return 空查询条件
         */
        public static UserSearchReq empty() {
            return new UserSearchReq(null, 1, 10);
        }

    }

    /**
     * 用户分页响应
     *
     * @author Ateng
     * @since 2026-04-29
     */
    public record UserPageResp(Long total, List<UserDetailResp> records) {

        /**
         * 创建空分页结果
         *
         * @return 空分页结果
         */
        public static UserPageResp empty() {
            return new UserPageResp(0L, CollUtil.newArrayList());
        }

    }

    /**
     * 用户详情响应
     *
     * @author Ateng
     * @since 2026-04-29
     */
    public record UserDetailResp(Long id, String username, String nickname, String mobile, String status) {
    }

    /**
     * 创建用户请求
     *
     * @author Ateng
     * @since 2026-04-29
     */
    public record UserCreateReq(String username, String nickname, String mobile) {
    }

    /**
     * 创建用户响应
     *
     * @author Ateng
     * @since 2026-04-29
     */
    public record UserCreateResp(Long id, Boolean success) {
    }

    /**
     * 更新用户请求
     *
     * @author Ateng
     * @since 2026-04-29
     */
    public record UserUpdateReq(String nickname, String mobile, String status) {
    }

    /**
     * 更新用户响应
     *
     * @author Ateng
     * @since 2026-04-29
     */
    public record UserUpdateResp(Long id, Boolean success) {
    }

    /**
     * 租户配置响应
     *
     * @author Ateng
     * @since 2026-04-29
     */
    public record TenantConfigResp(String tenantId, String tenantName, Boolean enabled) {
    }

    /**
     * 订单状态响应
     *
     * @author Ateng
     * @since 2026-04-29
     */
    public record OrderStatusResp(String orderNo, String status, String message) {
    }

    /**
     * 健康检查响应
     *
     * @author Ateng
     * @since 2026-04-29
     */
    public record HealthResp(String status, String message) {
    }

}
```

## 调用示例 Controller

这一节给出本地 Controller 示例，统一调用上面的 `DefaultRestClientApiClient`。它适合用于本地调试、接口联调和验证 `defaultRestClient` 基础能力。

这个 Controller 覆盖以下本地接口：

```text
1. GET /demo/default/users/{userId}
2. GET /demo/default/users
3. POST /demo/default/users
4. PUT /demo/default/users/{userId}
5. DELETE /demo/default/users/{userId}
6. GET /demo/default/tenant/config
7. GET /demo/default/orders/{orderNo}/status
8. GET /demo/default/health
```

### Controller 完整代码

这个 Controller 只负责接收本地 HTTP 请求，并转调 `DefaultRestClientApiClient`。远程调用细节不写在 Controller 中，避免 Controller 变重。

文件位置：`src/main/java/io/github/atengk/http/controller/DefaultRestClientDemoController.java`

```java
package io.github.atengk.http.controller;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.http.client.DefaultRestClientApiClient;
import io.github.atengk.http.client.DefaultRestClientApiClient.HealthResp;
import io.github.atengk.http.client.DefaultRestClientApiClient.OrderStatusResp;
import io.github.atengk.http.client.DefaultRestClientApiClient.TenantConfigResp;
import io.github.atengk.http.client.DefaultRestClientApiClient.UserCreateReq;
import io.github.atengk.http.client.DefaultRestClientApiClient.UserCreateResp;
import io.github.atengk.http.client.DefaultRestClientApiClient.UserDetailResp;
import io.github.atengk.http.client.DefaultRestClientApiClient.UserPageResp;
import io.github.atengk.http.client.DefaultRestClientApiClient.UserSearchReq;
import io.github.atengk.http.client.DefaultRestClientApiClient.UserUpdateReq;
import io.github.atengk.http.client.DefaultRestClientApiClient.UserUpdateResp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 默认 RestClient 调用示例接口
 *
 * @author Ateng
 * @since 2026-04-29
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/demo/default")
public class DefaultRestClientDemoController {

    private final DefaultRestClientApiClient defaultRestClientApiClient;

    /**
     * 查询用户详情
     *
     * @param userId 用户ID
     * @return 用户详情
     */
    @GetMapping("/users/{userId}")
    public UserDetailResp getUserDetail(@PathVariable String userId) {
        log.info("接收查询用户详情请求，userId={}", userId);
        return defaultRestClientApiClient.getUserDetail(userId);
    }

    /**
     * 分页查询用户
     *
     * @param keyword  关键字
     * @param pageNum  页码
     * @param pageSize 每页数量
     * @return 用户分页数据
     */
    @GetMapping("/users")
    public UserPageResp searchUsers(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize
    ) {
        UserSearchReq request = new UserSearchReq(keyword, pageNum, pageSize);

        log.info(
                "接收分页查询用户请求，keyword={}，pageNum={}，pageSize={}",
                keyword,
                pageNum,
                pageSize
        );

        return defaultRestClientApiClient.searchUsers(request);
    }

    /**
     * 创建用户
     *
     * @param request 创建用户请求
     * @return 创建结果
     */
    @PostMapping("/users")
    public UserCreateResp createUser(@RequestBody UserCreateReq request) {
        log.info("接收创建用户请求，username={}", request.username());
        return defaultRestClientApiClient.createUser(request);
    }

    /**
     * 更新用户
     *
     * @param userId  用户ID
     * @param request 更新用户请求
     * @return 更新结果
     */
    @PutMapping("/users/{userId}")
    public UserUpdateResp updateUser(
            @PathVariable String userId,
            @RequestBody UserUpdateReq request
    ) {
        log.info("接收更新用户请求，userId={}", userId);
        return defaultRestClientApiClient.updateUser(userId, request);
    }

    /**
     * 删除用户
     *
     * @param userId 用户ID
     * @return 是否删除成功
     */
    @DeleteMapping("/users/{userId}")
    public Boolean deleteUser(@PathVariable String userId) {
        log.info("接收删除用户请求，userId={}", userId);
        return defaultRestClientApiClient.deleteUser(userId);
    }

    /**
     * 查询租户配置
     *
     * @param tenantId 租户ID
     * @return 租户配置
     */
    @GetMapping("/tenant/config")
    public TenantConfigResp getTenantConfig(@RequestParam("tenantId") String tenantId) {
        if (StrUtil.isBlank(tenantId)) {
            throw new IllegalArgumentException("租户ID不能为空");
        }

        log.info("接收查询租户配置请求，tenantId={}", tenantId);
        return defaultRestClientApiClient.getTenantConfig(tenantId);
    }

    /**
     * 查询订单状态
     *
     * @param orderNo 订单号
     * @return 订单状态
     */
    @GetMapping("/orders/{orderNo}/status")
    public OrderStatusResp getOrderStatus(@PathVariable String orderNo) {
        log.info("接收查询订单状态请求，orderNo={}", orderNo);
        return defaultRestClientApiClient.getOrderStatus(orderNo);
    }

    /**
     * 健康检查
     *
     * @return 健康检查结果
     */
    @GetMapping("/health")
    public HealthResp checkHealth() {
        log.info("接收健康检查请求");
        return defaultRestClientApiClient.checkHealthByExchange();
    }

}
```

### curl 验证

查询用户详情：

```bash
curl -X GET "http://localhost:8080/demo/default/users/1001"
```

分页查询用户：

```bash
curl -X GET "http://localhost:8080/demo/default/users?keyword=ateng&pageNum=1&pageSize=10"
```

创建用户：

```bash
curl -X POST "http://localhost:8080/demo/default/users" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "ateng",
    "nickname": "阿腾",
    "mobile": "13800000000"
  }'
```

更新用户：

```bash
curl -X PUT "http://localhost:8080/demo/default/users/1001" \
  -H "Content-Type: application/json" \
  -d '{
    "nickname": "阿腾-更新",
    "mobile": "13900000000",
    "status": "ENABLE"
  }'
```

删除用户：

```bash
curl -X DELETE "http://localhost:8080/demo/default/users/1001"
```

查询租户配置：

```bash
curl -X GET "http://localhost:8080/demo/default/tenant/config?tenantId=tenant-001"
```

查询订单状态：

```bash
curl -X GET "http://localhost:8080/demo/default/orders/ORDER1001/status"
```

健康检查：

```bash
curl -X GET "http://localhost:8080/demo/default/health"
```

### 使用说明

这一组示例全部使用 `defaultRestClient`，适合验证基础配置是否生效：

```text
1. 是否能正常调用远程 GET / POST / PUT / DELETE
2. Query 参数是否正确拼接
3. Path 参数是否正确替换
4. JSON 请求体是否能正常序列化
5. JSON 响应体是否能正常反序列化
6. 自定义 Header 是否能正常透传
7. ResponseEntity 是否能读取响应头
8. exchange 是否能手动处理特殊响应
9. 日志、TraceId、统一错误处理是否生效
```

如果远程接口返回 4xx 或 5xx，`retrieve()` 调用会走基础配置中的 `RemoteHttpErrorHandler`。如果使用 `exchange()`，需要像 `checkHealthByExchange` 那样手动判断状态码并抛出 `RemoteCallException`。



## 功能：Bearer Token

Bearer Token 适合调用 OAuth2 资源服务、开放平台、网关认证接口等场景。最终请求头格式如下：

```text
Authorization: Bearer xxxxxx
```

这里推荐使用 `RemoteRestClientBuilderCustomizer` 实现。这样后续只需要在 `application.yml` 中给某个客户端开启 `bearer-token.enabled=true`，对应的 `RestClient` 就会自动追加 `Authorization` 请求头，不需要修改 `RemoteRestClientFactory`。

### 配置补充

在已有 `remote.http.clients` 下新增一个 Bearer Token 客户端配置。实际项目中建议把 `bearer` 换成具体系统名称，例如 `user-api`、`auth-api`、`open-platform`。

文件位置：`src/main/resources/application.yml`

```yaml
remote:
  http:
    clients:
      bearer:
        base-url: https://api.example.com
        connect-timeout: 3s
        connection-request-timeout: 2s
        response-timeout: 10s
        max-idle-time: 30s
        max-conn-total: 200
        max-conn-per-route: 50
        default-headers:
          Accept: application/json

        bearer-token:
          # 是否启用 Bearer Token 认证
          enabled: true

          # Token 值，生产环境建议从配置中心、环境变量或密钥系统读取
          token: your-access-token

          # 请求头名称，通常固定为 Authorization
          header-name: Authorization

          # Token 前缀，通常固定为 Bearer
          token-prefix: Bearer
```

### 常量补充

在已有 `RemoteHttpConstant` 中追加 Bearer Token 客户端名称。后续 `RestClientBaseConfig` 中不要直接写 `"bearer"` 字符串。

文件位置：`src/main/java/io/github/atengk/http/constant/RemoteHttpConstant.java`

```java
public static final String CLIENT_BEARER = "bearer";
```

如果前面基础配置里已经有 `HEADER_AUTHORIZATION`，这里不需要重复增加：

```java
public static final String HEADER_AUTHORIZATION = "Authorization";
```

### 配置属性补充

在 `RemoteHttpProperties.ClientConfig` 中增加 Bearer Token 配置字段，并增加对应的内部配置类。

文件位置：`src/main/java/io/github/atengk/http/config/RemoteHttpProperties.java`

```java
@Valid
private BearerTokenConfig bearerToken = new BearerTokenConfig();
```

在 `getMergedClient` 方法中增加 Bearer Token 配置合并逻辑。

```java
merged.setBearerToken(mergeBearerTokenConfig(defaultConfig.getBearerToken(), source.getBearerToken()));
```

在 `RemoteHttpProperties` 中增加合并方法。

```java
    /**
     * 合并 Bearer Token 配置
     *
     * @param defaultValue 默认配置
     * @param source       当前客户端配置
     * @return 合并后的配置
     */
    private BearerTokenConfig mergeBearerTokenConfig(BearerTokenConfig defaultValue, BearerTokenConfig source) {
        BearerTokenConfig merged = new BearerTokenConfig();
        merged.setEnabled(ObjectUtil.defaultIfNull(source.getEnabled(), defaultValue.getEnabled()));
        merged.setToken(ObjectUtil.defaultIfNull(source.getToken(), defaultValue.getToken()));
        merged.setHeaderName(ObjectUtil.defaultIfNull(source.getHeaderName(), defaultValue.getHeaderName()));
        merged.setTokenPrefix(ObjectUtil.defaultIfNull(source.getTokenPrefix(), defaultValue.getTokenPrefix()));
        return merged;
    }
```

在 `RemoteHttpProperties` 中增加 Bearer Token 配置类。

```java
    /**
     * Bearer Token 配置
     *
     * @author Ateng
     * @since 2026-04-29
     */
    @Data
    public static class BearerTokenConfig {

        private Boolean enabled = false;

        private String token;

        private String headerName = RemoteHttpConstant.HEADER_AUTHORIZATION;

        private String tokenPrefix = "Bearer";

    }
```

### Bearer Token 扩展器

这个扩展器会在 `RestClient` 创建时读取当前客户端配置。如果开启了 `bearer-token.enabled=true`，就自动追加 `Authorization: Bearer xxx` 请求头。

文件位置：`src/main/java/io/github/atengk/http/extension/BearerTokenRestClientBuilderCustomizer.java`

```java
package io.github.atengk.http.extension;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.http.config.RemoteHttpProperties;
import io.github.atengk.http.constant.RemoteHttpConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Bearer Token RestClient 扩展器
 *
 * @author Ateng
 * @since 2026-04-29
 */
@Slf4j
@Component
public class BearerTokenRestClientBuilderCustomizer implements RemoteRestClientBuilderCustomizer {

    /**
     * 判断是否支持当前客户端
     *
     * @param clientName   客户端名称
     * @param clientConfig 客户端配置
     * @return 是否支持
     */
    @Override
    public boolean supports(String clientName, RemoteHttpProperties.ClientConfig clientConfig) {
        RemoteHttpProperties.BearerTokenConfig bearerToken = clientConfig.getBearerToken();
        return Boolean.TRUE.equals(bearerToken.getEnabled());
    }

    /**
     * 自定义 RestClient Builder
     *
     * @param clientName   客户端名称
     * @param clientConfig 客户端配置
     * @param builder      RestClient Builder
     */
    @Override
    public void customize(String clientName, RemoteHttpProperties.ClientConfig clientConfig, RestClient.Builder builder) {
        RemoteHttpProperties.BearerTokenConfig bearerToken = clientConfig.getBearerToken();

        if (StrUtil.isBlank(bearerToken.getToken())) {
            throw new IllegalArgumentException(StrUtil.format("Bearer Token 不能为空，clientName={}", clientName));
        }

        String headerName = StrUtil.blankToDefault(
                bearerToken.getHeaderName(),
                RemoteHttpConstant.HEADER_AUTHORIZATION
        );
        String tokenPrefix = StrUtil.blankToDefault(bearerToken.getTokenPrefix(), "Bearer");
        String headerValue = StrUtil.format("{} {}", tokenPrefix, bearerToken.getToken());

        builder.defaultHeader(headerName, headerValue);

        log.info("Bearer Token 认证已启用，clientName={}，headerName={}", clientName, headerName);
    }

}
```

### RestClient Bean 补充

在已有 `RestClientBaseConfig` 中增加 Bearer Token 客户端 Bean。这里使用常量 `RemoteHttpConstant.CLIENT_BEARER`，不要直接写 `"bearer"`。

文件位置：`src/main/java/io/github/atengk/http/config/RestClientBaseConfig.java`

```java
    /**
     * Bearer Token RestClient
     *
     * @param remoteRestClientFactory RestClient 工厂
     * @return RestClient
     */
    @Bean
    public RestClient bearerTokenRestClient(RemoteRestClientFactory remoteRestClientFactory) {
        return remoteRestClientFactory.create(RemoteHttpConstant.CLIENT_BEARER);
    }
```

### 调用示例

这个客户端演示使用 `bearerTokenRestClient` 调用受保护接口。日志、TraceId、统一错误处理能力仍然来自基础配置，不需要重复处理。

文件位置：`src/main/java/io/github/atengk/http/client/BearerDemoApiClient.java`

```java
package io.github.atengk.http.client;

import io.github.atengk.http.constant.RemoteHttpConstant;
import io.github.atengk.http.support.RemoteCallExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Bearer Token 示例客户端
 *
 * @author Ateng
 * @since 2026-04-29
 */
@Slf4j
@Component
public class BearerDemoApiClient {

    private final RestClient restClient;

    private final RemoteCallExecutor remoteCallExecutor;

    public BearerDemoApiClient(
            @Qualifier("bearerTokenRestClient") RestClient restClient,
            RemoteCallExecutor remoteCallExecutor
    ) {
        this.restClient = restClient;
        this.remoteCallExecutor = remoteCallExecutor;
    }

    /**
     * 查询当前用户信息
     *
     * @return 当前用户信息
     */
    public CurrentUserResp getCurrentUser() {
        return remoteCallExecutor.execute(RemoteHttpConstant.CLIENT_BEARER, () -> restClient.get()
                .uri("/api/current-user")
                .retrieve()
                .body(CurrentUserResp.class));
    }

    public record CurrentUserResp(Long id, String username, String nickname) {
    }

}
```

### 临时方式：create 方法直接追加 Header

如果只是某个客户端临时使用 Bearer Token，也可以不写扩展器，直接在创建 Bean 时使用 `create(clientName, builder -> ...)`。

这种方式适合少量、固定 Token 的场景；如果多个客户端都要使用 Bearer Token，还是推荐使用上面的 `BearerTokenRestClientBuilderCustomizer`。

文件位置：`src/main/java/io/github/atengk/http/config/RestClientBaseConfig.java`

```java
    /**
     * 临时 Bearer Token RestClient
     *
     * @param remoteRestClientFactory RestClient 工厂
     * @return RestClient
     */
    @Bean
    public RestClient tempBearerTokenRestClient(RemoteRestClientFactory remoteRestClientFactory) {
        return remoteRestClientFactory.create(RemoteHttpConstant.CLIENT_BEARER, builder -> builder
                .defaultHeader(RemoteHttpConstant.HEADER_AUTHORIZATION, "Bearer your-access-token"));
    }
```

### 注意事项

Bearer Token 如果是固定值，可以使用 `builder.defaultHeader`。如果 Token 会过期、刷新或按租户动态变化，不建议把 Token 固定写入 `RestClient Bean`，应改为使用拦截器在每次请求前动态获取 Token。

当前实现适合：

```text
1. 固定 Token
2. 配置中心下发 Token
3. 环境变量注入 Token
4. 启动后 Token 不频繁变化的系统接口
```

如果后续要做自动刷新 Token，可以新增 `BearerTokenProvider` 和 `ClientHttpRequestInterceptor`，仍然不需要修改 `RemoteRestClientFactory`。

## 功能：API Key

API Key 适合调用供应商接口、SaaS 接口、开放平台接口或内部网关接口。常见放置方式有两种：

```text
1. Header：X-API-Key: xxxxxx
2. Query：?apiKey=xxxxxx
```

推荐优先使用 Header 方式。Query 参数容易出现在网关日志、访问日志、监控系统和错误日志中，不适合传递敏感凭证。

这里使用 `RemoteRestClientBuilderCustomizer` 加拦截器实现。这样后续只需要在 `application.yml` 中给某个客户端开启 `api-key.enabled=true`，对应的 `RestClient` 就会自动追加 API Key。

### 配置补充

在已有 `remote.http.clients` 下新增一个 API Key 客户端配置。实际项目中建议把 `api-key` 换成具体系统名称，例如 `supplier-api`、`map-api`、`sms-api`。

文件位置：`src/main/resources/application.yml`

```yaml
remote:
  http:
    clients:
      api-key:
        base-url: https://api.example.com
        connect-timeout: 3s
        connection-request-timeout: 2s
        response-timeout: 10s
        max-idle-time: 30s
        max-conn-total: 200
        max-conn-per-route: 50
        default-headers:
          Accept: application/json

        api-key:
          # 是否启用 API Key 认证
          enabled: true

          # API Key 名称，Header 模式下是 Header 名，Query 模式下是参数名
          name: X-API-Key

          # API Key 值，生产环境建议从配置中心、环境变量或密钥系统读取
          value: your-api-key

          # API Key 放置位置：header 或 query
          location: header
```

### 常量补充

在已有 `RemoteHttpConstant` 中追加 API Key 客户端名称和默认 Header 名称。后续创建 Bean 时不要直接写 `"api-key"` 字符串。

文件位置：`src/main/java/io/github/atengk/http/constant/RemoteHttpConstant.java`

```java
public static final String CLIENT_API_KEY = "api-key";

public static final String HEADER_API_KEY = "X-API-Key";
```

### 配置属性补充

在 `RemoteHttpProperties.ClientConfig` 中增加 API Key 配置字段。

文件位置：`src/main/java/io/github/atengk/http/config/RemoteHttpProperties.java`

```java
@Valid
private ApiKeyConfig apiKey = new ApiKeyConfig();
```

在 `getMergedClient` 方法中增加 API Key 配置合并逻辑。

```java
merged.setApiKey(mergeApiKeyConfig(defaultConfig.getApiKey(), source.getApiKey()));
```

在 `RemoteHttpProperties` 中增加合并方法。

```java
    /**
     * 合并 API Key 配置
     *
     * @param defaultValue 默认配置
     * @param source       当前客户端配置
     * @return 合并后的配置
     */
    private ApiKeyConfig mergeApiKeyConfig(ApiKeyConfig defaultValue, ApiKeyConfig source) {
        ApiKeyConfig merged = new ApiKeyConfig();
        merged.setEnabled(ObjectUtil.defaultIfNull(source.getEnabled(), defaultValue.getEnabled()));
        merged.setName(ObjectUtil.defaultIfNull(source.getName(), defaultValue.getName()));
        merged.setValue(ObjectUtil.defaultIfNull(source.getValue(), defaultValue.getValue()));
        merged.setLocation(ObjectUtil.defaultIfNull(source.getLocation(), defaultValue.getLocation()));
        return merged;
    }
```

在 `RemoteHttpProperties` 中增加 API Key 配置类。

```java
    /**
     * API Key 配置
     *
     * @author Ateng
     * @since 2026-04-29
     */
    @Data
    public static class ApiKeyConfig {

        private Boolean enabled = false;

        private String name = RemoteHttpConstant.HEADER_API_KEY;

        private String value;

        private String location = "header";

    }
```

### API Key 拦截器

这个拦截器支持两种模式：

```text
1. location=header：把 API Key 放入请求头
2. location=query：把 API Key 放入 URL Query 参数
```

Header 模式最常用，也最推荐。Query 模式只在第三方接口强制要求时使用。

文件位置：`src/main/java/io/github/atengk/http/interceptor/ApiKeyClientHttpRequestInterceptor.java`

```java
package io.github.atengk.http.interceptor;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.http.config.RemoteHttpProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;

/**
 * API Key 请求拦截器
 *
 * @author Ateng
 * @since 2026-04-29
 */
@RequiredArgsConstructor
public class ApiKeyClientHttpRequestInterceptor implements ClientHttpRequestInterceptor {

    private static final String LOCATION_HEADER = "header";

    private static final String LOCATION_QUERY = "query";

    private final RemoteHttpProperties.ApiKeyConfig apiKeyConfig;

    /**
     * 追加 API Key
     *
     * @param request   请求对象
     * @param body      请求体
     * @param execution 执行器
     * @return 响应对象
     * @throws IOException IO 异常
     */
    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        String location = StrUtil.blankToDefault(apiKeyConfig.getLocation(), LOCATION_HEADER);

        if (StrUtil.equalsIgnoreCase(location, LOCATION_QUERY)) {
            URI uri = UriComponentsBuilder.fromUri(request.getURI())
                    .queryParam(apiKeyConfig.getName(), apiKeyConfig.getValue())
                    .build(true)
                    .toUri();

            return execution.execute(new ApiKeyHttpRequestWrapper(request, uri), body);
        }

        request.getHeaders().set(apiKeyConfig.getName(), apiKeyConfig.getValue());
        return execution.execute(request, body);
    }

    /**
     * API Key 请求包装器
     *
     * @author Ateng
     * @since 2026-04-29
     */
    private static class ApiKeyHttpRequestWrapper implements HttpRequest {

        private final HttpRequest delegate;

        private final URI uri;

        private ApiKeyHttpRequestWrapper(HttpRequest delegate, URI uri) {
            this.delegate = delegate;
            this.uri = uri;
        }

        @Override
        public HttpMethod getMethod() {
            return delegate.getMethod();
        }

        @Override
        public URI getURI() {
            return uri;
        }

        @Override
        public HttpHeaders getHeaders() {
            return delegate.getHeaders();
        }

    }

}
```

### API Key 扩展器

这个扩展器会在 `RestClient` 创建时读取当前客户端配置。如果开启了 `api-key.enabled=true`，就自动添加 `ApiKeyClientHttpRequestInterceptor`。

文件位置：`src/main/java/io/github/atengk/http/extension/ApiKeyRestClientBuilderCustomizer.java`

```java
package io.github.atengk.http.extension;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.http.config.RemoteHttpProperties;
import io.github.atengk.http.interceptor.ApiKeyClientHttpRequestInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * API Key RestClient 扩展器
 *
 * @author Ateng
 * @since 2026-04-29
 */
@Slf4j
@Component
public class ApiKeyRestClientBuilderCustomizer implements RemoteRestClientBuilderCustomizer {

    /**
     * 判断是否支持当前客户端
     *
     * @param clientName   客户端名称
     * @param clientConfig 客户端配置
     * @return 是否支持
     */
    @Override
    public boolean supports(String clientName, RemoteHttpProperties.ClientConfig clientConfig) {
        RemoteHttpProperties.ApiKeyConfig apiKey = clientConfig.getApiKey();
        return Boolean.TRUE.equals(apiKey.getEnabled());
    }

    /**
     * 自定义 RestClient Builder
     *
     * @param clientName   客户端名称
     * @param clientConfig 客户端配置
     * @param builder      RestClient Builder
     */
    @Override
    public void customize(String clientName, RemoteHttpProperties.ClientConfig clientConfig, RestClient.Builder builder) {
        RemoteHttpProperties.ApiKeyConfig apiKey = clientConfig.getApiKey();

        if (StrUtil.isBlank(apiKey.getName())) {
            throw new IllegalArgumentException(StrUtil.format("API Key 名称不能为空，clientName={}", clientName));
        }
        if (StrUtil.isBlank(apiKey.getValue())) {
            throw new IllegalArgumentException(StrUtil.format("API Key 值不能为空，clientName={}", clientName));
        }

        String location = StrUtil.blankToDefault(apiKey.getLocation(), "header");
        if (!StrUtil.equalsAnyIgnoreCase(location, "header", "query")) {
            throw new IllegalArgumentException(StrUtil.format("API Key 放置位置不支持，clientName={}，location={}", clientName, location));
        }

        builder.requestInterceptor(new ApiKeyClientHttpRequestInterceptor(apiKey));

        log.info("API Key 认证已启用，clientName={}，location={}，name={}", clientName, location, apiKey.getName());
    }

}
```

### RestClient Bean 补充

在已有 `RestClientBaseConfig` 中增加 API Key 客户端 Bean。这里使用常量 `RemoteHttpConstant.CLIENT_API_KEY`，不要直接写 `"api-key"`。

文件位置：`src/main/java/io/github/atengk/http/config/RestClientBaseConfig.java`

```java
    /**
     * API Key RestClient
     *
     * @param remoteRestClientFactory RestClient 工厂
     * @return RestClient
     */
    @Bean
    public RestClient apiKeyRestClient(RemoteRestClientFactory remoteRestClientFactory) {
        return remoteRestClientFactory.create(RemoteHttpConstant.CLIENT_API_KEY);
    }
```

### 调用示例

这个客户端演示使用 `apiKeyRestClient` 调用供应商订单接口。API Key 会由拦截器自动追加，业务代码不用处理认证细节。

文件位置：`src/main/java/io/github/atengk/http/client/ApiKeyDemoApiClient.java`

```java
package io.github.atengk.http.client;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.http.constant.RemoteHttpConstant;
import io.github.atengk.http.support.RemoteCallExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * API Key 示例客户端
 *
 * @author Ateng
 * @since 2026-04-29
 */
@Slf4j
@Component
public class ApiKeyDemoApiClient {

    private final RestClient restClient;

    private final RemoteCallExecutor remoteCallExecutor;

    public ApiKeyDemoApiClient(
            @Qualifier("apiKeyRestClient") RestClient restClient,
            RemoteCallExecutor remoteCallExecutor
    ) {
        this.restClient = restClient;
        this.remoteCallExecutor = remoteCallExecutor;
    }

    /**
     * 查询供应商订单
     *
     * @param orderNo 订单号
     * @return 订单信息
     */
    public SupplierOrderResp getSupplierOrder(String orderNo) {
        if (StrUtil.isBlank(orderNo)) {
            throw new IllegalArgumentException("订单号不能为空");
        }

        return remoteCallExecutor.execute(RemoteHttpConstant.CLIENT_API_KEY, () -> restClient.get()
                .uri("/api/supplier/orders/{orderNo}", orderNo)
                .retrieve()
                .body(SupplierOrderResp.class));
    }

    public record SupplierOrderResp(String orderNo, String status, String amount) {
    }

}
```

### Header 模式简化写法

如果 API Key 永远放在 Header 中，也可以不用拦截器，直接通过 `create(clientName, builder -> ...)` 增量追加默认 Header。

这种方式最简单，但只适合 Header 固定场景，不支持 Query 模式。

文件位置：`src/main/java/io/github/atengk/http/config/RestClientBaseConfig.java`

```java
    /**
     * Header API Key RestClient
     *
     * @param remoteRestClientFactory RestClient 工厂
     * @return RestClient
     */
    @Bean
    public RestClient headerApiKeyRestClient(RemoteRestClientFactory remoteRestClientFactory) {
        return remoteRestClientFactory.create(RemoteHttpConstant.CLIENT_API_KEY, builder -> builder
                .defaultHeader(RemoteHttpConstant.HEADER_API_KEY, "your-api-key"));
    }
```

### 注意事项

API Key 推荐使用 Header 模式：

```yaml
api-key:
  enabled: true
  name: X-API-Key
  value: your-api-key
  location: header
```

只有第三方接口明确要求 Query 参数时，才使用 Query 模式：

```yaml
api-key:
  enabled: true
  name: apiKey
  value: your-api-key
  location: query
```

如果 API Key 是动态的，例如不同租户、不同用户、不同门店对应不同 Key，不建议把 Key 固定在 `application.yml` 中。此时可以把 `ApiKeyClientHttpRequestInterceptor` 改为依赖一个 `ApiKeyProvider`，在每次请求前根据上下文动态获取 Key。

## 功能：AK/SK 签名

AK/SK 签名适合支付接口、云服务接口、开放平台接口、供应商接口等安全要求较高的 HTTP 调用场景。

AK/SK 通常包含两部分：

```text
AK：Access Key，用于标识调用方身份
SK：Secret Key，用于本地计算签名，不允许在请求中明文传输
```

本方案通过 `ClientHttpRequestInterceptor` 在请求发出前计算签名，并把签名相关信息追加到请求头中。这样业务代码只负责调用接口，不需要手动处理签名逻辑。

签名请求头示例：

```text
X-App-Id: your-access-key
X-Timestamp: 1714377600000
X-Nonce: 2b8d3e5d1c6a4c6e9f6a2a1f4b7e8c9d
X-Signature: 9f6b2c...
```

本示例使用的签名原文格式如下：

```text
HTTP_METHOD
PATH
QUERY
TIMESTAMP
NONCE
BODY_SHA256
```

示例：

```text
POST
/api/orders
type=pay&channel=wechat
1714377600000
2b8d3e5d1c6a4c6e9f6a2a1f4b7e8c9d
d2a84f4b8b650937ec8f73cd8be2c74f...
```

### 配置补充

在已有 `remote.http.clients` 下新增一个签名客户端配置。实际项目中建议把 `sign` 换成具体系统名称，例如 `payment-api`、`cloud-api`、`open-platform`。

文件位置：`src/main/resources/application.yml`

```yaml
remote:
  http:
    logging:
      # 敏感 Header，不区分大小写；签名结果建议脱敏
      sensitive-headers:
        - Authorization
        - Cookie
        - Set-Cookie
        - X-API-Key
        - X-App-Secret
        - X-Signature

    clients:
      sign:
        base-url: https://api.example.com
        connect-timeout: 3s
        connection-request-timeout: 2s
        response-timeout: 10s
        max-idle-time: 30s
        max-conn-total: 200
        max-conn-per-route: 50
        default-headers:
          Accept: application/json

        sign:
          # 是否启用 AK/SK 签名
          enabled: true

          # Access Key，代表调用方身份
          access-key: your-access-key

          # Secret Key，只用于本地计算签名，不能明文传输
          secret-key: your-secret-key

          # Access Key 请求头名称
          access-key-header-name: X-App-Id

          # 时间戳请求头名称
          timestamp-header-name: X-Timestamp

          # 随机字符串请求头名称
          nonce-header-name: X-Nonce

          # 签名请求头名称
          signature-header-name: X-Signature

          # 签名算法
          algorithm: HmacSHA256
```

### 常量补充

在已有 `RemoteHttpConstant` 中追加签名客户端名称和签名请求头常量。后续创建 Bean 时不要直接写 `"sign"` 字符串。

文件位置：`src/main/java/io/github/atengk/http/constant/RemoteHttpConstant.java`

```java
public static final String CLIENT_SIGN = "sign";

public static final String HEADER_APP_ID = "X-App-Id";

public static final String HEADER_TIMESTAMP = "X-Timestamp";

public static final String HEADER_NONCE = "X-Nonce";

public static final String HEADER_SIGNATURE = "X-Signature";
```

### 配置属性补充

在 `RemoteHttpProperties.ClientConfig` 中增加签名配置字段。

文件位置：`src/main/java/io/github/atengk/http/config/RemoteHttpProperties.java`

```java
@Valid
private SignConfig sign = new SignConfig();
```

在 `getMergedClient` 方法中增加签名配置合并逻辑。

```java
merged.setSign(mergeSignConfig(defaultConfig.getSign(), source.getSign()));
```

在 `RemoteHttpProperties` 中增加合并方法。

```java
    /**
     * 合并签名配置
     *
     * @param defaultValue 默认配置
     * @param source       当前客户端配置
     * @return 合并后的配置
     */
    private SignConfig mergeSignConfig(SignConfig defaultValue, SignConfig source) {
        SignConfig merged = new SignConfig();
        merged.setEnabled(ObjectUtil.defaultIfNull(source.getEnabled(), defaultValue.getEnabled()));
        merged.setAccessKey(ObjectUtil.defaultIfNull(source.getAccessKey(), defaultValue.getAccessKey()));
        merged.setSecretKey(ObjectUtil.defaultIfNull(source.getSecretKey(), defaultValue.getSecretKey()));
        merged.setAccessKeyHeaderName(ObjectUtil.defaultIfNull(source.getAccessKeyHeaderName(), defaultValue.getAccessKeyHeaderName()));
        merged.setTimestampHeaderName(ObjectUtil.defaultIfNull(source.getTimestampHeaderName(), defaultValue.getTimestampHeaderName()));
        merged.setNonceHeaderName(ObjectUtil.defaultIfNull(source.getNonceHeaderName(), defaultValue.getNonceHeaderName()));
        merged.setSignatureHeaderName(ObjectUtil.defaultIfNull(source.getSignatureHeaderName(), defaultValue.getSignatureHeaderName()));
        merged.setAlgorithm(ObjectUtil.defaultIfNull(source.getAlgorithm(), defaultValue.getAlgorithm()));
        return merged;
    }
```

在 `RemoteHttpProperties` 中增加签名配置类。

```java
    /**
     * AK/SK 签名配置
     *
     * @author Ateng
     * @since 2026-04-29
     */
    @Data
    public static class SignConfig {

        private Boolean enabled = false;

        private String accessKey;

        private String secretKey;

        private String accessKeyHeaderName = RemoteHttpConstant.HEADER_APP_ID;

        private String timestampHeaderName = RemoteHttpConstant.HEADER_TIMESTAMP;

        private String nonceHeaderName = RemoteHttpConstant.HEADER_NONCE;

        private String signatureHeaderName = RemoteHttpConstant.HEADER_SIGNATURE;

        private String algorithm = "HmacSHA256";

    }
```

### 签名工具类

这个工具类负责构造签名原文、计算请求体 SHA-256、计算 HMAC-SHA256 签名。

这里使用 Hutool 的 `DigestUtil` 和 `HMac`，避免自己手写摘要和 HMAC 细节。

文件位置：`src/main/java/io/github/atengk/http/support/SignUtils.java`

```java
package io.github.atengk.http.support;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.crypto.digest.HMac;
import cn.hutool.crypto.digest.HmacAlgorithm;

import java.net.URI;
import java.nio.charset.StandardCharsets;

/**
 * 签名工具类
 *
 * @author Ateng
 * @since 2026-04-29
 */
public final class SignUtils {

    private SignUtils() {
    }

    /**
     * 构建签名原文
     *
     * @param method    HTTP 方法
     * @param uri       请求 URI
     * @param timestamp 时间戳
     * @param nonce     随机字符串
     * @param body      请求体
     * @return 签名原文
     */
    public static String buildSignText(String method, URI uri, String timestamp, String nonce, byte[] body) {
        String path = StrUtil.blankToDefault(uri.getRawPath(), "/");
        String query = StrUtil.blankToDefault(uri.getRawQuery(), "");
        String bodySha256 = sha256Hex(body);

        return StrUtil.join(
                "\n",
                method,
                path,
                query,
                timestamp,
                nonce,
                bodySha256
        );
    }

    /**
     * 计算 HMAC-SHA256 签名
     *
     * @param signText  签名原文
     * @param secretKey Secret Key
     * @return 签名结果
     */
    public static String hmacSha256Hex(String signText, String secretKey) {
        HMac hmac = new HMac(HmacAlgorithm.HmacSHA256, secretKey.getBytes(StandardCharsets.UTF_8));
        return hmac.digestHex(signText, StandardCharsets.UTF_8);
    }

    /**
     * 计算请求体 SHA-256
     *
     * @param body 请求体
     * @return SHA-256 十六进制字符串
     */
    public static String sha256Hex(byte[] body) {
        if (body == null || body.length == 0) {
            return DigestUtil.sha256Hex("");
        }
        return DigestUtil.sha256Hex(body);
    }

}
```

### 签名拦截器

这个拦截器会在请求发出前生成时间戳、随机字符串、签名原文和签名结果，然后把签名相关信息写入请求头。

文件位置：`src/main/java/io/github/atengk/http/interceptor/SignClientHttpRequestInterceptor.java`

```java
package io.github.atengk.http.interceptor;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.http.config.RemoteHttpProperties;
import io.github.atengk.http.support.SignUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

/**
 * AK/SK 签名请求拦截器
 *
 * @author Ateng
 * @since 2026-04-29
 */
@Slf4j
@RequiredArgsConstructor
public class SignClientHttpRequestInterceptor implements ClientHttpRequestInterceptor {

    private final RemoteHttpProperties.SignConfig signConfig;

    /**
     * 追加签名请求头
     *
     * @param request   请求对象
     * @param body      请求体
     * @param execution 执行器
     * @return 响应对象
     * @throws IOException IO 异常
     */
    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String nonce = IdUtil.fastSimpleUUID();

        String signText = SignUtils.buildSignText(
                request.getMethod().name(),
                request.getURI(),
                timestamp,
                nonce,
                body
        );
        String signature = SignUtils.hmacSha256Hex(signText, signConfig.getSecretKey());

        request.getHeaders().set(signConfig.getAccessKeyHeaderName(), signConfig.getAccessKey());
        request.getHeaders().set(signConfig.getTimestampHeaderName(), timestamp);
        request.getHeaders().set(signConfig.getNonceHeaderName(), nonce);
        request.getHeaders().set(signConfig.getSignatureHeaderName(), signature);

        log.debug("远程接口签名完成，method={}，uri={}，nonce={}", request.getMethod(), request.getURI(), nonce);

        return execution.execute(request, body);
    }

}
```

### 签名扩展器

这个扩展器会在 `RestClient` 创建时读取当前客户端配置。如果开启了 `sign.enabled=true`，就自动添加 `SignClientHttpRequestInterceptor`。

文件位置：`src/main/java/io/github/atengk/http/extension/SignRestClientBuilderCustomizer.java`

```java
package io.github.atengk.http.extension;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.http.config.RemoteHttpProperties;
import io.github.atengk.http.interceptor.SignClientHttpRequestInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * AK/SK 签名 RestClient 扩展器
 *
 * @author Ateng
 * @since 2026-04-29
 */
@Slf4j
@Component
public class SignRestClientBuilderCustomizer implements RemoteRestClientBuilderCustomizer {

    /**
     * 判断是否支持当前客户端
     *
     * @param clientName   客户端名称
     * @param clientConfig 客户端配置
     * @return 是否支持
     */
    @Override
    public boolean supports(String clientName, RemoteHttpProperties.ClientConfig clientConfig) {
        RemoteHttpProperties.SignConfig sign = clientConfig.getSign();
        return Boolean.TRUE.equals(sign.getEnabled());
    }

    /**
     * 自定义 RestClient Builder
     *
     * @param clientName   客户端名称
     * @param clientConfig 客户端配置
     * @param builder      RestClient Builder
     */
    @Override
    public void customize(String clientName, RemoteHttpProperties.ClientConfig clientConfig, RestClient.Builder builder) {
        RemoteHttpProperties.SignConfig sign = clientConfig.getSign();

        if (StrUtil.isBlank(sign.getAccessKey())) {
            throw new IllegalArgumentException(StrUtil.format("签名 AccessKey 不能为空，clientName={}", clientName));
        }
        if (StrUtil.isBlank(sign.getSecretKey())) {
            throw new IllegalArgumentException(StrUtil.format("签名 SecretKey 不能为空，clientName={}", clientName));
        }
        if (!StrUtil.equalsIgnoreCase(sign.getAlgorithm(), "HmacSHA256")) {
            throw new IllegalArgumentException(StrUtil.format("暂不支持该签名算法，clientName={}，algorithm={}", clientName, sign.getAlgorithm()));
        }

        builder.requestInterceptor(new SignClientHttpRequestInterceptor(sign));

        log.info("AK/SK 签名已启用，clientName={}，algorithm={}", clientName, sign.getAlgorithm());
    }

}
```

### RestClient Bean 补充

在已有 `RestClientBaseConfig` 中增加签名客户端 Bean。这里使用常量 `RemoteHttpConstant.CLIENT_SIGN`，不要直接写 `"sign"`。

文件位置：`src/main/java/io/github/atengk/http/config/RestClientBaseConfig.java`

```java
    /**
     * AK/SK 签名 RestClient
     *
     * @param remoteRestClientFactory RestClient 工厂
     * @return RestClient
     */
    @Bean
    public RestClient signRestClient(RemoteRestClientFactory remoteRestClientFactory) {
        return remoteRestClientFactory.create(RemoteHttpConstant.CLIENT_SIGN);
    }
```

### 调用示例

这个客户端演示使用 `signRestClient` 调用远程下单接口。签名由拦截器自动完成，业务代码不需要手动生成签名。

文件位置：`src/main/java/io/github/atengk/http/client/SignDemoApiClient.java`

```java
package io.github.atengk.http.client;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.http.constant.RemoteHttpConstant;
import io.github.atengk.http.support.RemoteCallExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * AK/SK 签名示例客户端
 *
 * @author Ateng
 * @since 2026-04-29
 */
@Slf4j
@Component
public class SignDemoApiClient {

    private final RestClient restClient;

    private final RemoteCallExecutor remoteCallExecutor;

    public SignDemoApiClient(
            @Qualifier("signRestClient") RestClient restClient,
            RemoteCallExecutor remoteCallExecutor
    ) {
        this.restClient = restClient;
        this.remoteCallExecutor = remoteCallExecutor;
    }

    /**
     * 创建远程订单
     *
     * @param request 创建订单请求
     * @return 创建结果
     */
    public CreateOrderResp createOrder(CreateOrderReq request) {
        if (StrUtil.isBlank(request.orderNo())) {
            throw new IllegalArgumentException("订单号不能为空");
        }

        return remoteCallExecutor.execute(RemoteHttpConstant.CLIENT_SIGN, () -> restClient.post()
                .uri("/api/orders?type=pay")
                .body(request)
                .retrieve()
                .body(CreateOrderResp.class));
    }

    public record CreateOrderReq(String orderNo, String productCode, Integer quantity) {
    }

    public record CreateOrderResp(String orderNo, Boolean success) {
    }

}
```

### 服务端验签规则参考

如果服务端也由你们维护，需要使用完全一致的签名规则。服务端验签时通常需要检查：

```text
1. AccessKey 是否存在
2. Timestamp 是否在允许时间窗口内，例如 5 分钟
3. Nonce 是否已使用，防止重放攻击
4. 使用 SecretKey 重新计算签名
5. 对比客户端传入的 X-Signature 是否一致
```

签名原文必须与客户端保持一致：

```text
HTTP_METHOD
PATH
QUERY
TIMESTAMP
NONCE
BODY_SHA256
```

注意：`QUERY` 当前使用 `uri.getRawQuery()` 原始顺序。如果服务端要求 Query 参数按字典序排序，则客户端和服务端都必须统一为排序后的 Query 字符串，不能一边使用原始顺序、一边使用排序顺序。

### 注意事项

`SecretKey` 不能写死在代码里，建议从配置中心、环境变量或密钥系统读取。

请求日志中建议把 `X-Signature` 加入敏感 Header 列表，避免签名结果明文输出。

当前实现适合大多数 HMAC-SHA256 签名场景。如果第三方平台有自己的签名规范，例如需要排序 Query、追加 Content-Type、追加 Date、使用 Base64 输出、RSA 签名等，只需要替换 `SignUtils.buildSignText` 和 `SignUtils.hmacSha256Hex` 的实现，不需要修改 `RemoteRestClientFactory`。

## 功能：文件上传

文件上传适合调用对象存储服务、附件服务、供应商文件接口、图片识别接口、报表导入接口等场景。HTTP 层通常使用 `multipart/form-data`，一个请求中可以同时包含文件、普通表单字段和 JSON 元数据。

Spring 的 `MultipartBodyBuilder` 可以构造 multipart 请求体，它支持添加字符串字段、`Resource` 文件资源、普通 Java 对象以及自定义 part header；`RestClient` 是同步 fluent API，支持通过 `body(Object)` 发送请求体。([Home](https://docs.spring.io/spring-framework/docs/6.1.10/javadoc-api/org/springframework/http/client/MultipartBodyBuilder.html?utm_source=chatgpt.com))

本方案不需要修改 `RemoteRestClientFactory`。文件上传属于调用方式差异，直接复用基础配置中的连接池、超时、日志、Trace、错误处理能力即可。

### 配置补充

在已有 `remote.http.clients` 下新增一个文件上传客户端配置。上传接口通常比普通查询接口耗时更长，因此这里的 `response-timeout` 可以适当放大。

文件位置：`src/main/resources/application.yml`

```yaml
remote:
  http:
    clients:
      multipart:
        base-url: https://file-api.example.com
        connect-timeout: 5s
        connection-request-timeout: 3s
        response-timeout: 60s
        max-idle-time: 60s
        max-conn-total: 100
        max-conn-per-route: 30
        default-headers:
          Accept: application/json
```

注意：这里不建议在 `default-headers` 中手动固定写死 `Content-Type: multipart/form-data`。上传时让 `RestClient` 和消息转换器自动生成带 boundary 的 `Content-Type` 更稳妥。multipart 请求必须带 boundary，如果手动写死 Content-Type，容易出现服务端无法解析文件的问题。

### 常量补充

在已有 `RemoteHttpConstant` 中追加文件上传客户端名称。后续创建 Bean 时不要直接写 `"multipart"` 字符串。

文件位置：`src/main/java/io/github/atengk/http/constant/RemoteHttpConstant.java`

```java
public static final String CLIENT_MULTIPART = "multipart";
```

### RestClient Bean 补充

在已有 `RestClientBaseConfig` 中增加文件上传客户端 Bean。这里使用常量 `RemoteHttpConstant.CLIENT_MULTIPART`。

文件位置：`src/main/java/io/github/atengk/http/config/RestClientBaseConfig.java`

```java
    /**
     * 文件上传 RestClient
     *
     * @param remoteRestClientFactory RestClient 工厂
     * @return RestClient
     */
    @Bean
    public RestClient multipartRestClient(RemoteRestClientFactory remoteRestClientFactory) {
        return remoteRestClientFactory.create(RemoteHttpConstant.CLIENT_MULTIPART);
    }
```

### 上传请求 DTO

这个 DTO 用于封装上传文件时的业务参数。实际项目中可以根据接口要求调整字段，例如业务类型、租户 ID、目录、文件标签等。

文件位置：`src/main/java/io/github/atengk/http/client/dto/FileUploadReq.java`

```java
package io.github.atengk.http.client.dto;

import lombok.Data;

/**
 * 文件上传请求
 *
 * @author Ateng
 * @since 2026-04-29
 */
@Data
public class FileUploadReq {

    /**
     * 业务类型
     */
    private String bizType;

    /**
     * 文件目录
     */
    private String directory;

    /**
     * 是否覆盖同名文件
     */
    private Boolean overwrite;

}
```

### 上传响应 DTO

这个 DTO 用于接收远程文件服务的上传结果。字段按常见文件服务返回设计，实际项目中按供应商接口调整。

文件位置：`src/main/java/io/github/atengk/http/client/dto/FileUploadResp.java`

```java
package io.github.atengk.http.client.dto;

import lombok.Data;

/**
 * 文件上传响应
 *
 * @author Ateng
 * @since 2026-04-29
 */
@Data
public class FileUploadResp {

    /**
     * 文件ID
     */
    private String fileId;

    /**
     * 文件名称
     */
    private String fileName;

    /**
     * 文件访问地址
     */
    private String fileUrl;

    /**
     * 文件大小，单位字节
     */
    private Long size;

}
```

### 文件上传客户端

这个客户端演示三种常见上传方式：

```text
1. 上传单个 MultipartFile
2. 上传本地 File
3. 上传文件并附带 JSON 元数据
```

`MultipartFile` 通常来自 Controller 接收的用户上传文件；`FileSystemResource` 适合上传本地文件；JSON 元数据可以作为一个独立 part 传给服务端。`MultipartBodyBuilder#part` 支持添加字符串、`Resource` 文件资源和普通对象。([Home](https://docs.spring.io/spring-framework/docs/6.1.10/javadoc-api/org/springframework/http/client/MultipartBodyBuilder.html?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/http/client/FileUploadApiClient.java`

```java
package io.github.atengk.http.client;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.http.client.dto.FileUploadReq;
import io.github.atengk.http.client.dto.FileUploadResp;
import io.github.atengk.http.constant.RemoteHttpConstant;
import io.github.atengk.http.support.RemoteCallExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

/**
 * 文件上传远程客户端
 *
 * @author Ateng
 * @since 2026-04-29
 */
@Slf4j
@Component
public class FileUploadApiClient {

    private final RestClient restClient;

    private final RemoteCallExecutor remoteCallExecutor;

    public FileUploadApiClient(
            @Qualifier("multipartRestClient") RestClient restClient,
            RemoteCallExecutor remoteCallExecutor
    ) {
        this.restClient = restClient;
        this.remoteCallExecutor = remoteCallExecutor;
    }

    /**
     * 上传 MultipartFile 文件
     *
     * @param file    上传文件
     * @param request 上传参数
     * @return 上传结果
     */
    public FileUploadResp uploadMultipartFile(MultipartFile file, FileUploadReq request) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }
        if (request == null || StrUtil.isBlank(request.getBizType())) {
            throw new IllegalArgumentException("业务类型不能为空");
        }

        log.info("开始上传 MultipartFile 文件，fileName={}，bizType={}", file.getOriginalFilename(), request.getBizType());

        MultiValueMap<String, ?> body = buildMultipartFileBody(file, request);

        return remoteCallExecutor.execute(RemoteHttpConstant.CLIENT_MULTIPART, () -> restClient.post()
                .uri("/api/files/upload")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body)
                .retrieve()
                .body(FileUploadResp.class));
    }

    /**
     * 上传本地文件
     *
     * @param file    本地文件
     * @param request 上传参数
     * @return 上传结果
     */
    public FileUploadResp uploadLocalFile(File file, FileUploadReq request) {
        if (file == null || !FileUtil.exist(file) || !FileUtil.isFile(file)) {
            throw new IllegalArgumentException("本地文件不存在");
        }
        if (request == null || StrUtil.isBlank(request.getBizType())) {
            throw new IllegalArgumentException("业务类型不能为空");
        }

        log.info("开始上传本地文件，filePath={}，bizType={}", file.getAbsolutePath(), request.getBizType());

        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("file", new FileSystemResource(file))
                .filename(file.getName())
                .contentType(MediaType.APPLICATION_OCTET_STREAM);
        builder.part("bizType", request.getBizType());
        builder.part("directory", StrUtil.blankToDefault(request.getDirectory(), "/"));
        builder.part("overwrite", Boolean.TRUE.equals(request.getOverwrite()).toString());

        return remoteCallExecutor.execute(RemoteHttpConstant.CLIENT_MULTIPART, () -> restClient.post()
                .uri("/api/files/upload")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(builder.build())
                .retrieve()
                .body(FileUploadResp.class));
    }

    /**
     * 上传文件并附带 JSON 元数据
     *
     * @param file    上传文件
     * @param request 上传参数
     * @return 上传结果
     */
    public FileUploadResp uploadWithMetadata(MultipartFile file, FileUploadReq request) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }
        if (request == null || StrUtil.isBlank(request.getBizType())) {
            throw new IllegalArgumentException("业务类型不能为空");
        }

        log.info("开始上传文件和元数据，fileName={}，bizType={}", file.getOriginalFilename(), request.getBizType());

        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("file", buildMultipartFileResource(file))
                .filename(StrUtil.blankToDefault(file.getOriginalFilename(), "upload-file"))
                .contentType(resolveMediaType(file.getContentType()));
        builder.part("metadata", request, MediaType.APPLICATION_JSON);

        return remoteCallExecutor.execute(RemoteHttpConstant.CLIENT_MULTIPART, () -> restClient.post()
                .uri("/api/files/upload-with-metadata")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(builder.build())
                .retrieve()
                .body(FileUploadResp.class));
    }

    /**
     * 构建 MultipartFile 请求体
     *
     * @param file    上传文件
     * @param request 上传参数
     * @return multipart 请求体
     */
    private MultiValueMap<String, ?> buildMultipartFileBody(MultipartFile file, FileUploadReq request) {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("file", buildMultipartFileResource(file))
                .filename(StrUtil.blankToDefault(file.getOriginalFilename(), "upload-file"))
                .contentType(resolveMediaType(file.getContentType()));
        builder.part("bizType", request.getBizType());
        builder.part("directory", StrUtil.blankToDefault(request.getDirectory(), "/"));
        builder.part("overwrite", Boolean.TRUE.equals(request.getOverwrite()).toString());
        return builder.build();
    }

    /**
     * 构建 MultipartFile 资源对象
     *
     * @param file 上传文件
     * @return 字节数组资源
     */
    private ByteArrayResource buildMultipartFileResource(MultipartFile file) {
        try {
            byte[] bytes = file.getBytes();
            String fileName = StrUtil.blankToDefault(file.getOriginalFilename(), "upload-file");

            return new ByteArrayResource(bytes) {
                @Override
                public String getFilename() {
                    return fileName;
                }
            };
        } catch (IOException ex) {
            log.warn("读取上传文件失败，fileName={}，message={}", file.getOriginalFilename(), ex.getMessage());
            throw new IllegalArgumentException("读取上传文件失败", ex);
        }
    }

    /**
     * 解析文件类型
     *
     * @param contentType 文件 Content-Type
     * @return MediaType
     */
    private MediaType resolveMediaType(String contentType) {
        if (StrUtil.isBlank(contentType)) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        return MediaType.parseMediaType(contentType);
    }

}
```

### Controller 调用示例

这个 Controller 演示如何接收前端上传文件，并调用 `FileUploadApiClient` 转发到远程文件服务。

文件位置：`src/main/java/io/github/atengk/http/controller/FileUploadController.java`

```java
package io.github.atengk.http.controller;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.http.client.FileUploadApiClient;
import io.github.atengk.http.client.dto.FileUploadReq;
import io.github.atengk.http.client.dto.FileUploadResp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传接口
 *
 * @author Ateng
 * @since 2026-04-29
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class FileUploadController {

    private final FileUploadApiClient fileUploadApiClient;

    /**
     * 上传文件
     *
     * @param file      文件
     * @param bizType   业务类型
     * @param directory 文件目录
     * @return 上传结果
     */
    @PostMapping("/demo/files/upload")
    public FileUploadResp upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("bizType") String bizType,
            @RequestParam(value = "directory", required = false) String directory
    ) {
        FileUploadReq request = new FileUploadReq();
        request.setBizType(bizType);
        request.setDirectory(StrUtil.blankToDefault(directory, "/"));
        request.setOverwrite(false);

        log.info("接收文件上传请求，fileName={}，bizType={}", file.getOriginalFilename(), bizType);

        return fileUploadApiClient.uploadMultipartFile(file, request);
    }

}
```

### curl 验证

启动服务后，可以用下面命令验证本地 Controller 上传接口。该接口会把文件转发给远程文件服务。

```bash
curl -X POST "http://localhost:8080/demo/files/upload" \
  -F "file=@/tmp/demo.xlsx" \
  -F "bizType=report" \
  -F "directory=/reports"
```

这里的 `-F` 表示按 `multipart/form-data` 方式提交表单字段和文件。`file=@/tmp/demo.xlsx` 表示上传本地 `/tmp/demo.xlsx` 文件，`bizType` 和 `directory` 是普通表单字段。

### 大文件注意事项

上面的 `MultipartFile -> ByteArrayResource` 写法会把文件读入内存，适合中小文件。大文件场景不建议这样做。

如果文件已经落到本地磁盘，优先使用：

```java
new FileSystemResource(file)
```

如果上传入口接收的是 `MultipartFile`，但文件很大，建议先保存到临时文件，再使用 `FileSystemResource` 上传，避免占用过多 JVM 堆内存。

### 注意事项

文件上传客户端通常不需要专门的认证逻辑。如果远程文件服务需要认证，可以复用前面的 Bearer Token、API Key 或 AK/SK 签名能力，把认证配置加到 `multipart` 客户端上即可。

例如上传接口需要 Bearer Token 时：

```yaml
remote:
  http:
    clients:
      multipart:
        base-url: https://file-api.example.com
        response-timeout: 60s
        default-headers:
          Accept: application/json
        bearer-token:
          enabled: true
          token: your-access-token
          header-name: Authorization
          token-prefix: Bearer
```

这样 `multipartRestClient` 会同时具备：

```text
1. 文件上传能力
2. Bearer Token 认证能力
3. 日志能力
4. TraceId 透传能力
5. 统一错误处理能力
6. 长超时配置
```

文件上传时不要手动设置固定 boundary。只需要在请求中设置：

```java
.contentType(MediaType.MULTIPART_FORM_DATA)
```

具体 boundary 由底层消息转换器自动生成。

## 功能：文件下载

文件下载适合调用文件服务、对象存储代理接口、报表导出接口、供应商附件接口等场景。常见处理方式有三种：

```text
1. 小文件：直接接收 byte[]
2. 需要响应头：使用 ResponseEntity<byte[]>
3. 大文件：使用 exchange 流式写入本地文件
```

如果只是下载小文件，使用 `retrieve().body(byte[].class)` 或 `retrieve().toEntity(byte[].class)` 即可。如果要处理大文件，推荐使用 `exchange()` 获取底层响应流并写入磁盘。需要注意：Spring 官方文档说明，`exchange()` 会直接暴露完整响应，因此不会应用默认 status handler，需要在 `exchange()` 里面自己判断错误状态码。([Home](https://docs.spring.io/spring-framework/reference/integration/rest-clients.html?utm_source=chatgpt.com))

本方案不需要修改 `RemoteRestClientFactory`。文件下载属于调用方式差异，直接复用基础配置中的连接池、超时、日志、Trace、错误处理能力即可；只有使用 `exchange()` 做流式下载时，需要在方法内部手动处理非 2xx 响应。

### 配置补充

在已有 `remote.http.clients` 下新增一个文件下载客户端配置。下载接口可能返回大文件或生成型报表，因此这里的 `response-timeout` 可以适当放大。

文件位置：`src/main/resources/application.yml`

```yaml
remote:
  http:
    clients:
      download:
        base-url: https://file-api.example.com
        connect-timeout: 5s
        connection-request-timeout: 3s
        response-timeout: 120s
        max-idle-time: 60s
        max-conn-total: 100
        max-conn-per-route: 30
        default-headers:
          Accept: application/octet-stream
```

如果下载接口返回的是 JSON 包装结构，例如先获取文件临时地址，再下载文件，可以把 `Accept` 保持为 `application/json`。如果接口直接返回文件流，使用 `application/octet-stream` 更合适。

### 常量补充

在已有 `RemoteHttpConstant` 中追加文件下载客户端名称。后续创建 Bean 时不要直接写 `"download"` 字符串。

文件位置：`src/main/java/io/github/atengk/http/constant/RemoteHttpConstant.java`

```java
public static final String CLIENT_DOWNLOAD = "download";
```

### RestClient Bean 补充

在已有 `RestClientBaseConfig` 中增加文件下载客户端 Bean。这里使用常量 `RemoteHttpConstant.CLIENT_DOWNLOAD`。

文件位置：`src/main/java/io/github/atengk/http/config/RestClientBaseConfig.java`

```java
    /**
     * 文件下载 RestClient
     *
     * @param remoteRestClientFactory RestClient 工厂
     * @return RestClient
     */
    @Bean
    public RestClient downloadRestClient(RemoteRestClientFactory remoteRestClientFactory) {
        return remoteRestClientFactory.create(RemoteHttpConstant.CLIENT_DOWNLOAD);
    }
```

### 下载响应 DTO

这个 DTO 用于封装小文件下载结果。它保留文件名、内容类型、文件大小和字节内容，适合文件较小且需要继续在内存中处理的场景。

文件位置：`src/main/java/io/github/atengk/http/client/dto/FileDownloadResp.java`

```java
package io.github.atengk.http.client.dto;

import lombok.Data;

/**
 * 文件下载响应
 *
 * @author Ateng
 * @since 2026-04-29
 */
@Data
public class FileDownloadResp {

    /**
     * 文件名称
     */
    private String fileName;

    /**
     * 文件类型
     */
    private String contentType;

    /**
     * 文件大小，单位字节
     */
    private Long size;

    /**
     * 文件内容
     */
    private byte[] content;

}
```

### 文件下载客户端

这个客户端演示三种下载方式：

```text
1. downloadBytes：下载为 byte[]
2. downloadWithHeaders：下载文件内容并读取响应头
3. downloadToFile：大文件流式写入本地文件
```

`retrieve().toEntity(byte[].class)` 可以同时获取响应状态、响应头和响应体；Spring 官方文档也说明可以通过 `ResponseEntity` 访问响应头和响应体。([Home](https://docs.spring.io/spring-framework/reference/integration/rest-clients.html?utm_source=chatgpt.com)) 大文件场景建议使用 `exchange()` 直接读取响应流，避免把完整文件加载到 JVM 堆内存。

文件位置：`src/main/java/io/github/atengk/http/client/FileDownloadApiClient.java`

```java
package io.github.atengk.http.client;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.http.client.dto.FileDownloadResp;
import io.github.atengk.http.constant.RemoteHttpConstant;
import io.github.atengk.http.exception.RemoteCallException;
import io.github.atengk.http.support.RemoteCallExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

/**
 * 文件下载远程客户端
 *
 * @author Ateng
 * @since 2026-04-29
 */
@Slf4j
@Component
public class FileDownloadApiClient {

    private final RestClient restClient;

    private final RemoteCallExecutor remoteCallExecutor;

    public FileDownloadApiClient(
            @Qualifier("downloadRestClient") RestClient restClient,
            RemoteCallExecutor remoteCallExecutor
    ) {
        this.restClient = restClient;
        this.remoteCallExecutor = remoteCallExecutor;
    }

    /**
     * 下载小文件为字节数组
     *
     * @param fileId 文件ID
     * @return 文件字节数组
     */
    public byte[] downloadBytes(String fileId) {
        if (StrUtil.isBlank(fileId)) {
            throw new IllegalArgumentException("文件ID不能为空");
        }

        log.info("开始下载文件字节数组，fileId={}", fileId);

        return remoteCallExecutor.execute(RemoteHttpConstant.CLIENT_DOWNLOAD, () -> restClient.get()
                .uri("/api/files/{fileId}/download", fileId)
                .accept(MediaType.APPLICATION_OCTET_STREAM)
                .retrieve()
                .body(byte[].class));
    }

    /**
     * 下载文件并读取响应头
     *
     * @param fileId 文件ID
     * @return 文件下载响应
     */
    public FileDownloadResp downloadWithHeaders(String fileId) {
        if (StrUtil.isBlank(fileId)) {
            throw new IllegalArgumentException("文件ID不能为空");
        }

        log.info("开始下载文件并读取响应头，fileId={}", fileId);

        ResponseEntity<byte[]> responseEntity = remoteCallExecutor.execute(RemoteHttpConstant.CLIENT_DOWNLOAD, () -> restClient.get()
                .uri("/api/files/{fileId}/download", fileId)
                .accept(MediaType.APPLICATION_OCTET_STREAM)
                .retrieve()
                .toEntity(byte[].class));

        byte[] body = responseEntity.getBody();
        HttpHeaders headers = responseEntity.getHeaders();

        FileDownloadResp response = new FileDownloadResp();
        response.setFileName(resolveFileName(headers, fileId));
        response.setContentType(resolveContentType(headers));
        response.setSize(resolveContentLength(headers, body));
        response.setContent(body);

        log.info("文件下载完成，fileId={}，fileName={}，size={}", fileId, response.getFileName(), response.getSize());

        return response;
    }

    /**
     * 大文件流式下载到本地
     *
     * @param fileId     文件ID
     * @param targetFile 目标文件
     * @return 目标文件
     */
    public File downloadToFile(String fileId, File targetFile) {
        if (StrUtil.isBlank(fileId)) {
            throw new IllegalArgumentException("文件ID不能为空");
        }
        if (targetFile == null) {
            throw new IllegalArgumentException("目标文件不能为空");
        }

        log.info("开始流式下载文件，fileId={}，targetFile={}", fileId, targetFile.getAbsolutePath());

        return remoteCallExecutor.execute(RemoteHttpConstant.CLIENT_DOWNLOAD, () -> restClient.get()
                .uri("/api/files/{fileId}/download", fileId)
                .accept(MediaType.APPLICATION_OCTET_STREAM)
                .exchange((request, response) -> {
                    if (response.getStatusCode().isError()) {
                        throw buildRemoteCallException(request, response);
                    }

                    FileUtil.mkParentDirs(targetFile);

                    try (OutputStream outputStream = Files.newOutputStream(
                            targetFile.toPath(),
                            StandardOpenOption.CREATE,
                            StandardOpenOption.TRUNCATE_EXISTING,
                            StandardOpenOption.WRITE
                    )) {
                        IoUtil.copy(response.getBody(), outputStream);
                    }

                    log.info("文件流式下载完成，fileId={}，targetFile={}", fileId, targetFile.getAbsolutePath());
                    return targetFile;
                }));
    }

    /**
     * 解析文件名
     *
     * @param headers      响应头
     * @param defaultValue 默认文件名
     * @return 文件名
     */
    private String resolveFileName(HttpHeaders headers, String defaultValue) {
        ContentDisposition contentDisposition = headers.getContentDisposition();
        String fileName = contentDisposition.getFilename();
        return StrUtil.blankToDefault(fileName, defaultValue);
    }

    /**
     * 解析文件类型
     *
     * @param headers 响应头
     * @return 文件类型
     */
    private String resolveContentType(HttpHeaders headers) {
        MediaType contentType = headers.getContentType();
        if (contentType == null) {
            return MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
        return contentType.toString();
    }

    /**
     * 解析文件大小
     *
     * @param headers 响应头
     * @param body    响应体
     * @return 文件大小
     */
    private Long resolveContentLength(HttpHeaders headers, byte[] body) {
        long contentLength = headers.getContentLength();
        if (contentLength >= 0) {
            return contentLength;
        }
        return body == null ? 0L : body.length;
    }

    /**
     * 构建远程调用异常
     *
     * @param request  请求对象
     * @param response 响应对象
     * @return 远程调用异常
     * @throws IOException IO 异常
     */
    private RemoteCallException buildRemoteCallException(HttpRequest request, ClientHttpResponse response) throws IOException {
        String responseBody = IoUtil.read(response.getBody(), StandardCharsets.UTF_8);
        String message = StrUtil.format(
                "远程文件下载失败，method={}，uri={}，status={}",
                request.getMethod(),
                request.getURI(),
                response.getStatusCode()
        );

        log.warn("{}，responseBody={}", message, responseBody);

        return new RemoteCallException(
                message,
                request.getURI(),
                response.getStatusCode(),
                responseBody
        );
    }

}
```

### Controller 下载示例

这个 Controller 演示如何把远程文件下载结果返回给前端浏览器。

文件位置：`src/main/java/io/github/atengk/http/controller/FileDownloadController.java`

```java
package io.github.atengk.http.controller;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.http.client.FileDownloadApiClient;
import io.github.atengk.http.client.dto.FileDownloadResp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.nio.charset.StandardCharsets;

/**
 * 文件下载接口
 *
 * @author Ateng
 * @since 2026-04-29
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class FileDownloadController {

    private final FileDownloadApiClient fileDownloadApiClient;

    /**
     * 下载小文件
     *
     * @param fileId 文件ID
     * @return 文件响应
     */
    @GetMapping("/demo/files/{fileId}/download")
    public ResponseEntity<byte[]> download(@PathVariable String fileId) {
        FileDownloadResp response = fileDownloadApiClient.downloadWithHeaders(fileId);

        String fileName = StrUtil.blankToDefault(response.getFileName(), fileId);
        MediaType mediaType = MediaType.parseMediaType(
                StrUtil.blankToDefault(response.getContentType(), MediaType.APPLICATION_OCTET_STREAM_VALUE)
        );

        log.info("返回文件下载响应，fileId={}，fileName={}，size={}", fileId, fileName, response.getSize());

        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(response.getSize())
                .header(HttpHeaders.CONTENT_DISPOSITION, buildAttachmentContentDisposition(fileName))
                .body(response.getContent());
    }

    /**
     * 流式下载大文件到本地后再返回
     *
     * @param fileId 文件ID
     * @return 文件资源
     */
    @GetMapping("/demo/files/{fileId}/download-local")
    public ResponseEntity<FileSystemResource> downloadLocal(@PathVariable String fileId) {
        File targetFile = new File("/tmp/download/" + fileId);
        File file = fileDownloadApiClient.downloadToFile(fileId, targetFile);

        log.info("返回本地文件资源，fileId={}，filePath={}", fileId, file.getAbsolutePath());

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(file.length())
                .header(HttpHeaders.CONTENT_DISPOSITION, buildAttachmentContentDisposition(file.getName()))
                .body(new FileSystemResource(file));
    }

    /**
     * 构建附件下载响应头
     *
     * @param fileName 文件名
     * @return Content-Disposition
     */
    private String buildAttachmentContentDisposition(String fileName) {
        return ContentDisposition.attachment()
                .filename(fileName, StandardCharsets.UTF_8)
                .build()
                .toString();
    }

}
```

### curl 验证

启动服务后，可以用下面命令验证小文件下载接口。

```bash
curl -L "http://localhost:8080/demo/files/10001/download" \
  -o demo-download.bin
```

验证大文件流式下载接口。

```bash
curl -L "http://localhost:8080/demo/files/10001/download-local" \
  -o demo-download-local.bin
```

`-L` 表示跟随重定向，`-o` 表示把响应内容保存到本地文件。实际文件名和扩展名可以根据远程服务返回的 `Content-Disposition` 或业务文件名调整。

### 小文件与大文件选择

小文件可以直接使用：

```java
.retrieve()
.body(byte[].class)
```

需要响应头时使用：

```java
.retrieve()
.toEntity(byte[].class)
```

大文件建议使用：

```java
.exchange((request, response) -> {
    // 手动判断状态码
    // 读取 response.getBody()
    // 写入本地文件或输出流
})
```

这里要特别注意：使用 `exchange()` 时默认 status handler 不会自动执行，需要自己判断 `response.getStatusCode().isError()` 并抛出业务异常。([Home](https://docs.spring.io/spring-framework/reference/integration/rest-clients.html?utm_source=chatgpt.com))

### 注意事项

文件下载客户端通常不需要单独的特殊工厂。认证、日志、TraceId、超时、连接池都复用基础配置。

如果远程文件服务需要认证，可以直接把 Bearer Token、API Key 或 AK/SK 签名配置加到 `download` 客户端上。例如：

```yaml
remote:
  http:
    clients:
      download:
        base-url: https://file-api.example.com
        response-timeout: 120s
        default-headers:
          Accept: application/octet-stream
        bearer-token:
          enabled: true
          token: your-access-token
          header-name: Authorization
          token-prefix: Bearer
```

这样 `downloadRestClient` 会同时具备：

```text
1. 文件下载能力
2. Bearer Token 认证能力
3. 日志能力
4. TraceId 透传能力
5. 统一错误处理能力
6. 长超时配置
```

如果文件很大，不建议使用 `byte[]` 方式下载，否则可能导致 JVM 堆内存压力过大。优先使用 `exchange()` 流式写入本地文件，或者在 Controller 中直接流式写入响应输出流。

## 功能：重试

重试适合处理短暂网络抖动、连接超时、读超时、远程服务瞬时不可用等临时失败场景。

本方案不把重试逻辑放进 `RemoteRestClientFactory`，而是扩展前面基础配置中预留的 `RemoteCallExecutor`。这样可以保持职责清晰：

```text
RestClient 负责 HTTP 调用
RemoteRestClientFactory 负责创建客户端
RemoteCallExecutor 负责调用执行策略
RetryRemoteCallExecutor 负责重试
```

重试必须谨慎使用。不是所有接口都适合重试，尤其是下单、支付、扣款、发券、发短信、创建订单等非幂等接口。默认建议只对查询、下载、状态检查、幂等提交等接口启用重试。

### 配置补充

这里在 `remote.http.clients` 下给指定客户端增加 `retry` 配置。重试配置跟随客户端，不做全局强制重试。

文件位置：`src/main/resources/application.yml`

```yaml
remote:
  http:
    clients:
      retry:
        base-url: https://api.example.com
        connect-timeout: 3s
        connection-request-timeout: 2s
        response-timeout: 10s
        max-idle-time: 30s
        max-conn-total: 200
        max-conn-per-route: 50
        default-headers:
          Accept: application/json

        retry:
          # 是否启用重试
          enabled: true

          # 最大尝试次数，包含第一次正常调用；3 表示最多调用 3 次
          max-attempts: 3

          # 首次重试等待时间
          initial-interval: 300ms

          # 最大重试等待时间
          max-interval: 3s

          # 退避倍数；例如 300ms、600ms、1200ms
          multiplier: 2.0

          # 是否只重试幂等请求；建议保持 true
          idempotent-only: true

          # 允许重试的 HTTP 方法
          retry-methods:
            - GET
            - HEAD
            - OPTIONS
            - DELETE

          # 允许重试的 HTTP 状态码
          retry-status-codes:
            - 408
            - 429
            - 500
            - 502
            - 503
            - 504
```

说明：`max-attempts` 包含第一次调用。例如配置为 `3`，实际最多执行 1 次原始调用 + 2 次重试。

### 常量补充

在已有 `RemoteHttpConstant` 中追加重试客户端名称。后续创建 Bean 或执行远程调用时不要直接写 `"retry"` 字符串。

文件位置：`src/main/java/io/github/atengk/http/constant/RemoteHttpConstant.java`

```java
public static final String CLIENT_RETRY = "retry";
```

### 配置属性补充

在 `RemoteHttpProperties.ClientConfig` 中增加重试配置字段。

文件位置：`src/main/java/io/github/atengk/http/config/RemoteHttpProperties.java`

```java
@Valid
private RetryConfig retry = new RetryConfig();
```

在 `getMergedClient` 方法中增加重试配置合并逻辑。

```java
merged.setRetry(mergeRetryConfig(defaultConfig.getRetry(), source.getRetry()));
```

在 `RemoteHttpProperties` 中增加合并方法。

```java
    /**
     * 合并重试配置
     *
     * @param defaultValue 默认配置
     * @param source       当前客户端配置
     * @return 合并后的配置
     */
    private RetryConfig mergeRetryConfig(RetryConfig defaultValue, RetryConfig source) {
        RetryConfig merged = new RetryConfig();
        merged.setEnabled(ObjectUtil.defaultIfNull(source.getEnabled(), defaultValue.getEnabled()));
        merged.setMaxAttempts(ObjectUtil.defaultIfNull(source.getMaxAttempts(), defaultValue.getMaxAttempts()));
        merged.setInitialInterval(ObjectUtil.defaultIfNull(source.getInitialInterval(), defaultValue.getInitialInterval()));
        merged.setMaxInterval(ObjectUtil.defaultIfNull(source.getMaxInterval(), defaultValue.getMaxInterval()));
        merged.setMultiplier(ObjectUtil.defaultIfNull(source.getMultiplier(), defaultValue.getMultiplier()));
        merged.setIdempotentOnly(ObjectUtil.defaultIfNull(source.getIdempotentOnly(), defaultValue.getIdempotentOnly()));
        merged.setRetryMethods(ObjectUtil.defaultIfNull(source.getRetryMethods(), defaultValue.getRetryMethods()));
        merged.setRetryStatusCodes(ObjectUtil.defaultIfNull(source.getRetryStatusCodes(), defaultValue.getRetryStatusCodes()));
        return merged;
    }
```

在 `RemoteHttpProperties` 中增加重试配置类。

```java
    /**
     * 重试配置
     *
     * @author Ateng
     * @since 2026-04-29
     */
    @Data
    public static class RetryConfig {

        private Boolean enabled = false;

        @Min(value = 1, message = "最大尝试次数必须大于 0")
        private Integer maxAttempts = 3;

        private Duration initialInterval = Duration.ofMillis(300);

        private Duration maxInterval = Duration.ofSeconds(3);

        private Double multiplier = 2.0;

        private Boolean idempotentOnly = true;

        private List<String> retryMethods = List.of("GET", "HEAD", "OPTIONS", "DELETE");

        private List<Integer> retryStatusCodes = List.of(408, 429, 500, 502, 503, 504);

    }
```

### 重试上下文

`RemoteCallExecutor` 原始接口只有 `clientName` 和 `Supplier`，无法知道当前调用是不是幂等操作。因此这里增加一个轻量上下文，用于传递方法名、业务说明和是否允许重试。

文件位置：`src/main/java/io/github/atengk/http/support/RemoteCallContext.java`

```java
package io.github.atengk.http.support;

import cn.hutool.core.util.StrUtil;
import lombok.Builder;
import lombok.Getter;

/**
 * 远程调用上下文
 *
 * @author Ateng
 * @since 2026-04-29
 */
@Getter
@Builder
public class RemoteCallContext {

    /**
     * 客户端名称
     */
    private final String clientName;

    /**
     * HTTP 方法
     */
    private final String method;

    /**
     * 业务描述
     */
    private final String description;

    /**
     * 是否幂等
     */
    private final Boolean idempotent;

    /**
     * 创建默认上下文
     *
     * @param clientName 客户端名称
     * @return 调用上下文
     */
    public static RemoteCallContext of(String clientName) {
        return RemoteCallContext.builder()
                .clientName(clientName)
                .method("GET")
                .description(clientName)
                .idempotent(true)
                .build();
    }

    /**
     * 判断是否幂等
     *
     * @return 是否幂等
     */
    public boolean isIdempotent() {
        return Boolean.TRUE.equals(idempotent);
    }

    /**
     * 获取安全描述
     *
     * @return 描述
     */
    public String getSafeDescription() {
        return StrUtil.blankToDefault(description, clientName);
    }

}
```

### RemoteCallExecutor 接口调整

为了兼容原来的写法，保留 `execute(String clientName, Supplier<T> supplier)`，同时新增支持上下文的重载方法。原有调用代码不需要强制修改。

文件位置：`src/main/java/io/github/atengk/http/support/RemoteCallExecutor.java`

```java
package io.github.atengk.http.support;

import java.util.function.Supplier;

/**
 * 远程调用执行器
 *
 * @author Ateng
 * @since 2026-04-29
 */
public interface RemoteCallExecutor {

    /**
     * 执行远程调用
     *
     * @param clientName 客户端名称
     * @param supplier   调用逻辑
     * @param <T>        返回类型
     * @return 调用结果
     */
    default <T> T execute(String clientName, Supplier<T> supplier) {
        return execute(RemoteCallContext.of(clientName), supplier);
    }

    /**
     * 执行远程调用
     *
     * @param context  调用上下文
     * @param supplier 调用逻辑
     * @param <T>      返回类型
     * @return 调用结果
     */
    <T> T execute(RemoteCallContext context, Supplier<T> supplier);

    /**
     * 执行无返回值远程调用
     *
     * @param clientName 客户端名称
     * @param runnable   调用逻辑
     */
    default void execute(String clientName, Runnable runnable) {
        execute(clientName, () -> {
            runnable.run();
            return null;
        });
    }

    /**
     * 执行无返回值远程调用
     *
     * @param context  调用上下文
     * @param runnable 调用逻辑
     */
    default void execute(RemoteCallContext context, Runnable runnable) {
        execute(context, () -> {
            runnable.run();
            return null;
        });
    }

}
```

### 默认执行器调整

如果没有启用重试或熔断，默认执行器直接执行调用逻辑。

文件位置：`src/main/java/io/github/atengk/http/support/DefaultRemoteCallExecutor.java`

```java
package io.github.atengk.http.support;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/**
 * 默认远程调用执行器
 *
 * @author Ateng
 * @since 2026-04-29
 */
@Slf4j
@Component
@ConditionalOnMissingBean(RemoteCallExecutor.class)
public class DefaultRemoteCallExecutor implements RemoteCallExecutor {

    /**
     * 执行远程调用
     *
     * @param context  调用上下文
     * @param supplier 调用逻辑
     * @param <T>      返回类型
     * @return 调用结果
     */
    @Override
    public <T> T execute(RemoteCallContext context, Supplier<T> supplier) {
        return supplier.get();
    }

}
```

### 重试异常判断工具

这个工具类负责判断异常是否允许重试。当前规则：

```text
1. RemoteCallException：只对配置中的 HTTP 状态码重试
2. ResourceAccessException：通常是连接超时、读超时、网络异常，允许重试
3. 其他异常：默认不重试
```

文件位置：`src/main/java/io/github/atengk/http/support/RemoteRetryUtils.java`

```java
package io.github.atengk.http.support;

import cn.hutool.core.collection.CollUtil;
import io.github.atengk.http.config.RemoteHttpProperties;
import io.github.atengk.http.exception.RemoteCallException;
import org.springframework.web.client.ResourceAccessException;

/**
 * 远程调用重试工具类
 *
 * @author Ateng
 * @since 2026-04-29
 */
public final class RemoteRetryUtils {

    private RemoteRetryUtils() {
    }

    /**
     * 判断是否允许重试
     *
     * @param retryConfig 重试配置
     * @param throwable   异常
     * @return 是否允许重试
     */
    public static boolean canRetry(RemoteHttpProperties.RetryConfig retryConfig, Throwable throwable) {
        if (throwable instanceof RemoteCallException remoteCallException) {
            int statusCode = remoteCallException.getStatusCode().value();
            return CollUtil.isNotEmpty(retryConfig.getRetryStatusCodes())
                    && retryConfig.getRetryStatusCodes().contains(statusCode);
        }

        return throwable instanceof ResourceAccessException;
    }

}
```

### 重试执行器

这个执行器是本节核心。它实现 `RemoteCallExecutor`，根据客户端配置决定是否重试。

重试策略使用指数退避：

```text
第 1 次失败后等待 300ms
第 2 次失败后等待 600ms
第 3 次失败后等待 1200ms
```

等待时间不会超过 `max-interval`。

文件位置：`src/main/java/io/github/atengk/http/support/RetryRemoteCallExecutor.java`

```java
package io.github.atengk.http.support;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.http.config.RemoteHttpProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * 重试远程调用执行器
 *
 * @author Ateng
 * @since 2026-04-29
 */
@Slf4j
@Primary
@Component
@RequiredArgsConstructor
public class RetryRemoteCallExecutor implements RemoteCallExecutor {

    private final RemoteHttpProperties remoteHttpProperties;

    /**
     * 执行远程调用
     *
     * @param context  调用上下文
     * @param supplier 调用逻辑
     * @param <T>      返回类型
     * @return 调用结果
     */
    @Override
    public <T> T execute(RemoteCallContext context, Supplier<T> supplier) {
        RemoteHttpProperties.ClientConfig clientConfig = remoteHttpProperties.getMergedClient(context.getClientName());
        RemoteHttpProperties.RetryConfig retryConfig = clientConfig.getRetry();

        if (!isRetryEnabled(context, retryConfig)) {
            return supplier.get();
        }

        int maxAttempts = Math.max(1, retryConfig.getMaxAttempts());
        Throwable lastThrowable = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                T result = supplier.get();

                if (attempt > 1) {
                    log.info(
                            "远程调用重试成功，clientName={}，description={}，attempt={}",
                            context.getClientName(),
                            context.getSafeDescription(),
                            attempt
                    );
                }

                return result;
            } catch (Throwable ex) {
                lastThrowable = ex;

                if (attempt >= maxAttempts || !RemoteRetryUtils.canRetry(retryConfig, ex)) {
                    log.warn(
                            "远程调用重试结束，clientName={}，description={}，attempt={}，maxAttempts={}，message={}",
                            context.getClientName(),
                            context.getSafeDescription(),
                            attempt,
                            maxAttempts,
                            ex.getMessage()
                    );
                    throw ex;
                }

                Duration sleepDuration = calculateSleepDuration(retryConfig, attempt);

                log.warn(
                        "远程调用失败，准备重试，clientName={}，description={}，attempt={}，maxAttempts={}，sleep={}ms，message={}",
                        context.getClientName(),
                        context.getSafeDescription(),
                        attempt,
                        maxAttempts,
                        sleepDuration.toMillis(),
                        ex.getMessage()
                );

                ThreadUtil.sleep(sleepDuration.toMillis());
            }
        }

        throw new IllegalStateException("远程调用重试异常结束", lastThrowable);
    }

    /**
     * 判断是否启用重试
     *
     * @param context     调用上下文
     * @param retryConfig 重试配置
     * @return 是否启用
     */
    private boolean isRetryEnabled(RemoteCallContext context, RemoteHttpProperties.RetryConfig retryConfig) {
        if (!Boolean.TRUE.equals(retryConfig.getEnabled())) {
            return false;
        }

        if (Boolean.TRUE.equals(retryConfig.getIdempotentOnly()) && !context.isIdempotent()) {
            log.debug(
                    "远程调用非幂等，不启用重试，clientName={}，description={}",
                    context.getClientName(),
                    context.getSafeDescription()
            );
            return false;
        }

        if (ObjectUtil.isNull(retryConfig.getRetryMethods())) {
            return true;
        }

        return retryConfig.getRetryMethods().stream()
                .anyMatch(method -> method.equalsIgnoreCase(context.getMethod()));
    }

    /**
     * 计算等待时间
     *
     * @param retryConfig 重试配置
     * @param attempt     当前尝试次数
     * @return 等待时间
     */
    private Duration calculateSleepDuration(RemoteHttpProperties.RetryConfig retryConfig, int attempt) {
        Duration initialInterval = ObjectUtil.defaultIfNull(retryConfig.getInitialInterval(), Duration.ofMillis(300));
        Duration maxInterval = ObjectUtil.defaultIfNull(retryConfig.getMaxInterval(), Duration.ofSeconds(3));
        double multiplier = ObjectUtil.defaultIfNull(retryConfig.getMultiplier(), 2.0D);

        double sleepMillis = initialInterval.toMillis() * Math.pow(multiplier, Math.max(0, attempt - 1));
        long finalSleepMillis = Math.min((long) sleepMillis, maxInterval.toMillis());

        return Duration.ofMillis(finalSleepMillis);
    }

}
```

### RestClient Bean 补充

在已有 `RestClientBaseConfig` 中增加重试客户端 Bean。这里使用常量 `RemoteHttpConstant.CLIENT_RETRY`。

文件位置：`src/main/java/io/github/atengk/http/config/RestClientBaseConfig.java`

```java
    /**
     * 重试 RestClient
     *
     * @param remoteRestClientFactory RestClient 工厂
     * @return RestClient
     */
    @Bean
    public RestClient retryRestClient(RemoteRestClientFactory remoteRestClientFactory) {
        return remoteRestClientFactory.create(RemoteHttpConstant.CLIENT_RETRY);
    }
```

### 调用示例

这个客户端演示一个适合重试的查询接口。通过 `RemoteCallContext` 明确说明这是 `GET` 幂等调用，可以按配置进行重试。

文件位置：`src/main/java/io/github/atengk/http/client/RetryDemoApiClient.java`

```java
package io.github.atengk.http.client;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.http.constant.RemoteHttpConstant;
import io.github.atengk.http.support.RemoteCallContext;
import io.github.atengk.http.support.RemoteCallExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * 重试示例客户端
 *
 * @author Ateng
 * @since 2026-04-29
 */
@Slf4j
@Component
public class RetryDemoApiClient {

    private final RestClient restClient;

    private final RemoteCallExecutor remoteCallExecutor;

    public RetryDemoApiClient(
            @Qualifier("retryRestClient") RestClient restClient,
            RemoteCallExecutor remoteCallExecutor
    ) {
        this.restClient = restClient;
        this.remoteCallExecutor = remoteCallExecutor;
    }

    /**
     * 查询远程订单状态
     *
     * @param orderNo 订单号
     * @return 订单状态
     */
    public OrderStatusResp getOrderStatus(String orderNo) {
        if (StrUtil.isBlank(orderNo)) {
            throw new IllegalArgumentException("订单号不能为空");
        }

        RemoteCallContext context = RemoteCallContext.builder()
                .clientName(RemoteHttpConstant.CLIENT_RETRY)
                .method("GET")
                .description("查询远程订单状态")
                .idempotent(true)
                .build();

        return remoteCallExecutor.execute(context, () -> restClient.get()
                .uri("/api/orders/{orderNo}/status", orderNo)
                .retrieve()
                .body(OrderStatusResp.class));
    }

    /**
     * 创建远程订单
     *
     * @param request 创建订单请求
     * @return 创建结果
     */
    public CreateOrderResp createOrder(CreateOrderReq request) {
        RemoteCallContext context = RemoteCallContext.builder()
                .clientName(RemoteHttpConstant.CLIENT_RETRY)
                .method("POST")
                .description("创建远程订单")
                .idempotent(false)
                .build();

        return remoteCallExecutor.execute(context, () -> restClient.post()
                .uri("/api/orders")
                .body(request)
                .retrieve()
                .body(CreateOrderResp.class));
    }

    public record OrderStatusResp(String orderNo, String status) {
    }

    public record CreateOrderReq(String orderNo, String productCode, Integer quantity) {
    }

    public record CreateOrderResp(String orderNo, Boolean success) {
    }

}
```

这里的 `getOrderStatus` 会按配置重试，因为它是 `GET` 且 `idempotent=true`。

`createOrder` 不会重试，因为它是 `POST` 且 `idempotent=false`。这可以避免因为网络抖动导致重复创建订单。

### 使用建议

推荐重试的接口：

```text
1. 查询接口
2. 状态检查接口
3. 文件下载接口
4. 幂等更新接口
5. 已有幂等键保护的提交接口
```

不推荐默认重试的接口：

```text
1. 创建订单
2. 支付扣款
3. 发放优惠券
4. 发送短信
5. 创建发货单
6. 没有幂等键保护的 POST 请求
```

如果必须重试 `POST`，建议请求中必须包含业务幂等键，例如：

```text
orderNo
requestId
idempotencyKey
externalNo
```

### 注意事项

当前实现是轻量级本地重试，不依赖额外框架，适合普通项目使用。

如果项目已经使用 Resilience4j，可以把 `RetryRemoteCallExecutor` 替换为基于 Resilience4j Retry 的实现；调用层仍然使用 `RemoteCallExecutor`，不需要修改 `RestClient`、`RemoteRestClientFactory` 或具体 `XxxApiClient` 的结构。

重试会放大远程服务压力。对于 `429 Too Many Requests`、`503 Service Unavailable` 这类响应，建议配合退避等待；对于持续失败的远程服务，后续应继续接入熔断能力，避免本系统线程长期阻塞。

## 功能：熔断

熔断适合保护本系统，避免远程服务持续失败时，本系统线程被大量阻塞，最终形成级联故障。

重试和熔断的职责不同：

```text
重试：处理短暂失败，尝试再次调用
熔断：发现远程服务持续失败后，短时间内直接拒绝调用
```

本方案继续扩展 `RemoteCallExecutor`，不改 `RemoteRestClientFactory`。也就是说：

```text
RestClient 负责 HTTP 请求
RemoteRestClientFactory 负责创建客户端
RemoteCallExecutor 负责执行策略
CircuitBreakerRemoteCallExecutor 负责熔断保护
```

注意：如果你已经接入第 6 节的 `RetryRemoteCallExecutor`，不要同时再注册一个 `@Primary RemoteCallExecutor`，否则 Spring 会出现多个主 Bean 冲突。实际项目建议最终只保留一个组合执行器，例如 `ResilienceRemoteCallExecutor`，统一处理重试和熔断。这里先单独给出熔断实现，便于文档分节维护。

### 配置补充

这里在 `remote.http.clients` 下给指定客户端增加 `circuit-breaker` 配置。熔断配置跟随客户端，不做全局强制熔断。

文件位置：`src/main/resources/application.yml`

```yaml
remote:
  http:
    clients:
      circuit-breaker:
        base-url: https://api.example.com
        connect-timeout: 3s
        connection-request-timeout: 2s
        response-timeout: 10s
        max-idle-time: 30s
        max-conn-total: 200
        max-conn-per-route: 50
        default-headers:
          Accept: application/json

        circuit-breaker:
          # 是否启用熔断
          enabled: true

          # 统计窗口大小，最近 N 次调用参与熔断判断
          sliding-window-size: 10

          # 最小调用次数，未达到该次数前不触发熔断
          minimum-number-of-calls: 5

          # 失败率阈值，达到或超过该百分比时打开熔断器
          failure-rate-threshold: 50.0

          # 熔断打开后的等待时间，等待结束后进入半开状态
          wait-duration-in-open-state: 30s

          # 半开状态允许通过的调用次数
          permitted-calls-in-half-open-state: 3
```

熔断状态说明：

```text
CLOSED：关闭状态，正常放行请求
OPEN：打开状态，直接拒绝请求
HALF_OPEN：半开状态，放行少量请求用于探测远程服务是否恢复
```

### 常量补充

在已有 `RemoteHttpConstant` 中追加熔断客户端名称。后续创建 Bean 或执行远程调用时不要直接写 `"circuit-breaker"` 字符串。

文件位置：`src/main/java/io/github/atengk/http/constant/RemoteHttpConstant.java`

```java
public static final String CLIENT_CIRCUIT_BREAKER = "circuit-breaker";
```

### 配置属性补充

在 `RemoteHttpProperties.ClientConfig` 中增加熔断配置字段。

文件位置：`src/main/java/io/github/atengk/http/config/RemoteHttpProperties.java`

```java
@Valid
private CircuitBreakerConfig circuitBreaker = new CircuitBreakerConfig();
```

在 `getMergedClient` 方法中增加熔断配置合并逻辑。

```java
merged.setCircuitBreaker(mergeCircuitBreakerConfig(defaultConfig.getCircuitBreaker(), source.getCircuitBreaker()));
```

在 `RemoteHttpProperties` 中增加合并方法。

```java
    /**
     * 合并熔断配置
     *
     * @param defaultValue 默认配置
     * @param source       当前客户端配置
     * @return 合并后的配置
     */
    private CircuitBreakerConfig mergeCircuitBreakerConfig(CircuitBreakerConfig defaultValue, CircuitBreakerConfig source) {
        CircuitBreakerConfig merged = new CircuitBreakerConfig();
        merged.setEnabled(ObjectUtil.defaultIfNull(source.getEnabled(), defaultValue.getEnabled()));
        merged.setSlidingWindowSize(ObjectUtil.defaultIfNull(source.getSlidingWindowSize(), defaultValue.getSlidingWindowSize()));
        merged.setMinimumNumberOfCalls(ObjectUtil.defaultIfNull(source.getMinimumNumberOfCalls(), defaultValue.getMinimumNumberOfCalls()));
        merged.setFailureRateThreshold(ObjectUtil.defaultIfNull(source.getFailureRateThreshold(), defaultValue.getFailureRateThreshold()));
        merged.setWaitDurationInOpenState(ObjectUtil.defaultIfNull(source.getWaitDurationInOpenState(), defaultValue.getWaitDurationInOpenState()));
        merged.setPermittedCallsInHalfOpenState(ObjectUtil.defaultIfNull(source.getPermittedCallsInHalfOpenState(), defaultValue.getPermittedCallsInHalfOpenState()));
        return merged;
    }
```

在 `RemoteHttpProperties` 中增加熔断配置类。

```java
    /**
     * 熔断配置
     *
     * @author Ateng
     * @since 2026-04-29
     */
    @Data
    public static class CircuitBreakerConfig {

        private Boolean enabled = false;

        @Min(value = 1, message = "统计窗口大小必须大于 0")
        private Integer slidingWindowSize = 10;

        @Min(value = 1, message = "最小调用次数必须大于 0")
        private Integer minimumNumberOfCalls = 5;

        private Double failureRateThreshold = 50.0D;

        private Duration waitDurationInOpenState = Duration.ofSeconds(30);

        @Min(value = 1, message = "半开状态允许调用次数必须大于 0")
        private Integer permittedCallsInHalfOpenState = 3;

    }
```

### 熔断异常

当熔断器处于打开状态时，调用会被直接拒绝，并抛出 `RemoteCircuitBreakerOpenException`。

文件位置：`src/main/java/io/github/atengk/http/exception/RemoteCircuitBreakerOpenException.java`

```java
package io.github.atengk.http.exception;

/**
 * 远程调用熔断打开异常
 *
 * @author Ateng
 * @since 2026-04-29
 */
public class RemoteCircuitBreakerOpenException extends RuntimeException {

    public RemoteCircuitBreakerOpenException(String message) {
        super(message);
    }

}
```

### 熔断状态枚举

这个枚举表示熔断器当前状态。

文件位置：`src/main/java/io/github/atengk/http/support/CircuitBreakerState.java`

```java
package io.github.atengk.http.support;

/**
 * 熔断器状态
 *
 * @author Ateng
 * @since 2026-04-29
 */
public enum CircuitBreakerState {

    /**
     * 关闭状态，正常放行请求
     */
    CLOSED,

    /**
     * 打开状态，直接拒绝请求
     */
    OPEN,

    /**
     * 半开状态，放行少量请求探测服务是否恢复
     */
    HALF_OPEN

}
```

### 熔断器统计对象

这个类维护单个客户端的熔断状态、统计窗口、失败次数、半开状态放行次数等信息。

当前实现是轻量级本地内存熔断器，适合单体服务或普通微服务实例使用。如果是大规模集群，建议后续接入 Resilience4j、Sentinel 或网关侧熔断能力。

文件位置：`src/main/java/io/github/atengk/http/support/RemoteCircuitBreaker.java`

```java
package io.github.atengk.http.support;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.NumberUtil;
import io.github.atengk.http.config.RemoteHttpProperties;
import io.github.atengk.http.exception.RemoteCircuitBreakerOpenException;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 远程调用熔断器
 *
 * @author Ateng
 * @since 2026-04-29
 */
@Slf4j
public class RemoteCircuitBreaker {

    private final String clientName;

    private final RemoteHttpProperties.CircuitBreakerConfig config;

    private final Deque<Boolean> callResults = new ArrayDeque<>();

    private CircuitBreakerState state = CircuitBreakerState.CLOSED;

    private long openTimeMillis = 0L;

    private int halfOpenCalls = 0;

    public RemoteCircuitBreaker(String clientName, RemoteHttpProperties.CircuitBreakerConfig config) {
        this.clientName = clientName;
        this.config = config;
    }

    /**
     * 尝试获取调用许可
     */
    public synchronized void acquirePermission() {
        if (state == CircuitBreakerState.CLOSED) {
            return;
        }

        if (state == CircuitBreakerState.OPEN) {
            if (isOpenWaitExpired()) {
                state = CircuitBreakerState.HALF_OPEN;
                halfOpenCalls = 0;
                log.warn("熔断器进入半开状态，clientName={}", clientName);
            } else {
                throw new RemoteCircuitBreakerOpenException("远程调用熔断器已打开，clientName=" + clientName);
            }
        }

        if (state == CircuitBreakerState.HALF_OPEN) {
            int permittedCalls = Math.max(1, config.getPermittedCallsInHalfOpenState());
            if (halfOpenCalls >= permittedCalls) {
                throw new RemoteCircuitBreakerOpenException("远程调用熔断器半开状态限流，clientName=" + clientName);
            }
            halfOpenCalls++;
        }
    }

    /**
     * 记录成功调用
     */
    public synchronized void onSuccess() {
        if (state == CircuitBreakerState.HALF_OPEN) {
            close();
            return;
        }

        recordResult(true);
    }

    /**
     * 记录失败调用
     */
    public synchronized void onFailure() {
        if (state == CircuitBreakerState.HALF_OPEN) {
            open();
            return;
        }

        recordResult(false);

        if (shouldOpen()) {
            open();
        }
    }

    /**
     * 获取当前状态
     *
     * @return 熔断器状态
     */
    public synchronized CircuitBreakerState getState() {
        return state;
    }

    /**
     * 记录调用结果
     *
     * @param success 是否成功
     */
    private void recordResult(boolean success) {
        callResults.addLast(success);

        int slidingWindowSize = Math.max(1, config.getSlidingWindowSize());
        while (callResults.size() > slidingWindowSize) {
            callResults.removeFirst();
        }
    }

    /**
     * 判断是否应该打开熔断器
     *
     * @return 是否打开
     */
    private boolean shouldOpen() {
        int minimumNumberOfCalls = Math.max(1, config.getMinimumNumberOfCalls());
        if (callResults.size() < minimumNumberOfCalls) {
            return false;
        }

        long failureCount = callResults.stream()
                .filter(success -> !success)
                .count();

        double failureRate = NumberUtil.div(failureCount * 100.0D, callResults.size(), 2).doubleValue();
        double threshold = config.getFailureRateThreshold();

        log.debug(
                "熔断器统计结果，clientName={}，failureCount={}，totalCount={}，failureRate={}，threshold={}",
                clientName,
                failureCount,
                callResults.size(),
                failureRate,
                threshold
        );

        return failureRate >= threshold;
    }

    /**
     * 判断打开状态等待时间是否结束
     *
     * @return 是否结束
     */
    private boolean isOpenWaitExpired() {
        Duration waitDuration = config.getWaitDurationInOpenState();
        long waitMillis = waitDuration == null ? 30000L : waitDuration.toMillis();
        return System.currentTimeMillis() - openTimeMillis >= waitMillis;
    }

    /**
     * 打开熔断器
     */
    private void open() {
        state = CircuitBreakerState.OPEN;
        openTimeMillis = System.currentTimeMillis();
        halfOpenCalls = 0;
        log.warn("熔断器已打开，clientName={}", clientName);
    }

    /**
     * 关闭熔断器
     */
    private void close() {
        state = CircuitBreakerState.CLOSED;
        openTimeMillis = 0L;
        halfOpenCalls = 0;
        CollUtil.clear(callResults);
        log.info("熔断器已关闭，clientName={}", clientName);
    }

}
```

### 熔断管理器

这个管理器按照 `clientName` 维护不同客户端的熔断器实例。不同远程系统互不影响。

文件位置：`src/main/java/io/github/atengk/http/support/RemoteCircuitBreakerRegistry.java`

```java
package io.github.atengk.http.support;

import io.github.atengk.http.config.RemoteHttpProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 远程调用熔断器注册表
 *
 * @author Ateng
 * @since 2026-04-29
 */
@Component
@RequiredArgsConstructor
public class RemoteCircuitBreakerRegistry {

    private final RemoteHttpProperties remoteHttpProperties;

    private final Map<String, RemoteCircuitBreaker> circuitBreakerMap = new ConcurrentHashMap<>();

    /**
     * 获取熔断器
     *
     * @param clientName 客户端名称
     * @return 熔断器
     */
    public RemoteCircuitBreaker getCircuitBreaker(String clientName) {
        return circuitBreakerMap.computeIfAbsent(clientName, this::createCircuitBreaker);
    }

    /**
     * 创建熔断器
     *
     * @param clientName 客户端名称
     * @return 熔断器
     */
    private RemoteCircuitBreaker createCircuitBreaker(String clientName) {
        RemoteHttpProperties.ClientConfig clientConfig = remoteHttpProperties.getMergedClient(clientName);
        return new RemoteCircuitBreaker(clientName, clientConfig.getCircuitBreaker());
    }

}
```

### 熔断执行器

这个执行器实现 `RemoteCallExecutor`，在远程调用前检查熔断状态，调用成功后记录成功，调用失败后记录失败。

如果熔断未启用，则直接执行调用逻辑。

文件位置：`src/main/java/io/github/atengk/http/support/CircuitBreakerRemoteCallExecutor.java`

```java
package io.github.atengk.http.support;

import io.github.atengk.http.config.RemoteHttpProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/**
 * 熔断远程调用执行器
 *
 * @author Ateng
 * @since 2026-04-29
 */
@Slf4j
@Primary
@Component
@RequiredArgsConstructor
public class CircuitBreakerRemoteCallExecutor implements RemoteCallExecutor {

    private final RemoteHttpProperties remoteHttpProperties;

    private final RemoteCircuitBreakerRegistry circuitBreakerRegistry;

    /**
     * 执行远程调用
     *
     * @param context  调用上下文
     * @param supplier 调用逻辑
     * @param <T>      返回类型
     * @return 调用结果
     */
    @Override
    public <T> T execute(RemoteCallContext context, Supplier<T> supplier) {
        RemoteHttpProperties.ClientConfig clientConfig = remoteHttpProperties.getMergedClient(context.getClientName());
        RemoteHttpProperties.CircuitBreakerConfig circuitBreakerConfig = clientConfig.getCircuitBreaker();

        if (!Boolean.TRUE.equals(circuitBreakerConfig.getEnabled())) {
            return supplier.get();
        }

        RemoteCircuitBreaker circuitBreaker = circuitBreakerRegistry.getCircuitBreaker(context.getClientName());
        circuitBreaker.acquirePermission();

        try {
            T result = supplier.get();
            circuitBreaker.onSuccess();
            return result;
        } catch (Throwable ex) {
            circuitBreaker.onFailure();

            log.warn(
                    "远程调用失败，熔断器已记录失败，clientName={}，description={}，state={}，message={}",
                    context.getClientName(),
                    context.getSafeDescription(),
                    circuitBreaker.getState(),
                    ex.getMessage()
            );

            throw ex;
        }
    }

}
```

### RestClient Bean 补充

在已有 `RestClientBaseConfig` 中增加熔断客户端 Bean。这里使用常量 `RemoteHttpConstant.CLIENT_CIRCUIT_BREAKER`。

文件位置：`src/main/java/io/github/atengk/http/config/RestClientBaseConfig.java`

```java
    /**
     * 熔断 RestClient
     *
     * @param remoteRestClientFactory RestClient 工厂
     * @return RestClient
     */
    @Bean
    public RestClient circuitBreakerRestClient(RemoteRestClientFactory remoteRestClientFactory) {
        return remoteRestClientFactory.create(RemoteHttpConstant.CLIENT_CIRCUIT_BREAKER);
    }
```

### 调用示例

这个客户端演示调用一个受熔断保护的远程接口。多次失败达到阈值后，后续调用会直接抛出 `RemoteCircuitBreakerOpenException`，不会再访问远程服务。

文件位置：`src/main/java/io/github/atengk/http/client/CircuitBreakerDemoApiClient.java`

```java
package io.github.atengk.http.client;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.http.constant.RemoteHttpConstant;
import io.github.atengk.http.support.RemoteCallContext;
import io.github.atengk.http.support.RemoteCallExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * 熔断示例客户端
 *
 * @author Ateng
 * @since 2026-04-29
 */
@Slf4j
@Component
public class CircuitBreakerDemoApiClient {

    private final RestClient restClient;

    private final RemoteCallExecutor remoteCallExecutor;

    public CircuitBreakerDemoApiClient(
            @Qualifier("circuitBreakerRestClient") RestClient restClient,
            RemoteCallExecutor remoteCallExecutor
    ) {
        this.restClient = restClient;
        this.remoteCallExecutor = remoteCallExecutor;
    }

    /**
     * 查询远程商品库存
     *
     * @param productCode 商品编码
     * @return 库存信息
     */
    public StockResp getStock(String productCode) {
        if (StrUtil.isBlank(productCode)) {
            throw new IllegalArgumentException("商品编码不能为空");
        }

        RemoteCallContext context = RemoteCallContext.builder()
                .clientName(RemoteHttpConstant.CLIENT_CIRCUIT_BREAKER)
                .method("GET")
                .description("查询远程商品库存")
                .idempotent(true)
                .build();

        return remoteCallExecutor.execute(context, () -> restClient.get()
                .uri("/api/products/{productCode}/stock", productCode)
                .retrieve()
                .body(StockResp.class));
    }

    public record StockResp(String productCode, Integer stock) {
    }

}
```

### 与重试组合的推荐顺序

如果一个客户端同时启用重试和熔断，推荐执行顺序是：

```text
1. 先检查熔断器是否允许调用
2. 如果允许，再执行重试逻辑
3. 整个重试过程最终成功，熔断器记录成功
4. 整个重试过程最终失败，熔断器记录一次失败
```

不建议每次重试失败都累计一次熔断失败，否则一次业务调用可能被统计成多次失败，导致熔断器过早打开。

推荐逻辑：

```text
业务调用一次
  -> 熔断器判断是否放行
      -> 重试执行器内部最多尝试 N 次
          -> 最终成功：熔断器记录 1 次成功
          -> 最终失败：熔断器记录 1 次失败
```

如果你已经使用第 6 节的 `RetryRemoteCallExecutor`，并且现在要加入熔断，建议不要同时保留两个 `@Primary RemoteCallExecutor`。更稳的做法是后续合并成一个 `ResilienceRemoteCallExecutor`：

```text
ResilienceRemoteCallExecutor
  -> circuit breaker acquirePermission
  -> retry execute
  -> circuit breaker onSuccess / onFailure
```

### 注意事项

熔断适合保护本系统，但不能替代超时配置。每个 `RestClient` 仍然必须设置合理的 `connect-timeout`、`connection-request-timeout` 和 `response-timeout`。

熔断打开后，调用会直接失败。如果业务需要兜底数据，可以在 `XxxApiClient` 或业务 Service 中捕获 `RemoteCircuitBreakerOpenException`，返回缓存数据、默认值或友好提示。

当前实现是本地内存熔断器，每个应用实例独立统计。如果服务有多个副本，每个副本的熔断状态互不共享。大规模生产环境可以考虑使用 Resilience4j、Sentinel 或网关层熔断。

## 功能：代理

代理适合服务部署在内网、需要通过公司代理访问外网、供应商接口只能通过固定出口访问、测试环境需要抓包调试等场景。

代理属于底层 HTTP 客户端能力，不能只靠 `RestClient.Builder` 完成。本方案使用前面基础配置预留的 `RemoteRequestFactoryCustomizer` 扩展，不修改 `RemoteRestClientFactory` 主流程。

最终结构如下：

```text
proxyRestClient
  -> RemoteRestClientFactory
      -> ProxyRequestFactoryCustomizer
          -> Apache HttpClient 5 Proxy
```

代理支持两种模式：

```text
1. 普通 HTTP 代理
2. 带用户名密码认证的 HTTP 代理
```

### 配置补充

在已有 `remote.http.clients` 下新增一个代理客户端配置。代理配置放在当前客户端下，只影响这个客户端，不影响其他 `RestClient Bean`。

文件位置：`src/main/resources/application.yml`

```yaml
remote:
  http:
    logging:
      # 如果后续手动使用 Proxy-Authorization Header，建议加入脱敏列表
      sensitive-headers:
        - Authorization
        - Proxy-Authorization
        - Cookie
        - Set-Cookie
        - X-API-Key
        - X-App-Secret
        - X-Signature

    clients:
      proxy:
        base-url: https://api.example.com
        connect-timeout: 5s
        connection-request-timeout: 3s
        response-timeout: 30s
        max-idle-time: 60s
        max-conn-total: 100
        max-conn-per-route: 30
        default-headers:
          Accept: application/json

        proxy:
          # 是否启用代理
          enabled: true

          # 代理协议，常用 http
          scheme: http

          # 代理服务器地址
          host: proxy.example.com

          # 代理服务器端口
          port: 8080

          # 代理用户名；无认证代理可留空
          username:

          # 代理密码；无认证代理可留空
          password:
```

如果代理需要认证，则配置：

```yaml
proxy:
  enabled: true
  scheme: http
  host: proxy.example.com
  port: 8080
  username: proxy-user
  password: proxy-password
```

### 常量补充

在已有 `RemoteHttpConstant` 中追加代理客户端名称。后续创建 Bean 时不要直接写 `"proxy"` 字符串。

文件位置：`src/main/java/io/github/atengk/http/constant/RemoteHttpConstant.java`

```java
public static final String CLIENT_PROXY = "proxy";

public static final String HEADER_PROXY_AUTHORIZATION = "Proxy-Authorization";
```

### 配置属性核对

如果你前面的基础配置已经包含 `ProxyConfig`，这里不需要重复添加。只需要确认 `RemoteHttpProperties.ClientConfig` 中有下面字段。

文件位置：`src/main/java/io/github/atengk/http/config/RemoteHttpProperties.java`

```java
@Valid
private ProxyConfig proxy = new ProxyConfig();
```

同时确认 `getMergedClient` 方法中已经合并代理配置。

```java
merged.setProxy(ObjectUtil.defaultIfNull(source.getProxy(), defaultConfig.getProxy()));
```

如果基础配置中还没有 `ProxyConfig`，补充下面这个内部类。

```java
    /**
     * HTTP 代理配置
     *
     * @author Ateng
     * @since 2026-04-29
     */
    @Data
    public static class ProxyConfig {

        private Boolean enabled = false;

        private String scheme = "http";

        private String host;

        private Integer port = 0;

        private String username;

        private String password;

    }
```

### 代理请求工厂扩展器

这个扩展器是本节核心。它实现 `RemoteRequestFactoryCustomizer`，当某个客户端开启 `proxy.enabled=true` 时，会创建带代理配置的 Apache HttpClient 请求工厂。

这里没有修改 `RemoteRestClientFactory`，只是替换当前客户端的底层 `ClientHttpRequestFactory`。

文件位置：`src/main/java/io/github/atengk/http/extension/ProxyRequestFactoryCustomizer.java`

```java
package io.github.atengk.http.extension;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.http.config.RemoteHttpProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.core.annotation.Order;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

/**
 * 代理请求工厂扩展器
 *
 * @author Ateng
 * @since 2026-04-29
 */
@Slf4j
@Order(100)
@Component
public class ProxyRequestFactoryCustomizer implements RemoteRequestFactoryCustomizer, DisposableBean {

    private final List<CloseableHttpClient> httpClients = new CopyOnWriteArrayList<>();

    /**
     * 判断是否支持当前客户端
     *
     * @param clientName   客户端名称
     * @param clientConfig 客户端配置
     * @return 是否支持
     */
    @Override
    public boolean supports(String clientName, RemoteHttpProperties.ClientConfig clientConfig) {
        RemoteHttpProperties.ProxyConfig proxy = clientConfig.getProxy();
        return proxy != null && Boolean.TRUE.equals(proxy.getEnabled());
    }

    /**
     * 自定义请求工厂
     *
     * @param clientName             客户端名称
     * @param clientConfig           客户端配置
     * @param requestFactorySupplier 默认请求工厂供应器
     * @return 请求工厂
     */
    @Override
    public ClientHttpRequestFactory customize(
            String clientName,
            RemoteHttpProperties.ClientConfig clientConfig,
            Supplier<ClientHttpRequestFactory> requestFactorySupplier
    ) {
        RemoteHttpProperties.ProxyConfig proxy = clientConfig.getProxy();
        validateProxyConfig(clientName, proxy);

        HttpHost proxyHost = new HttpHost(
                StrUtil.blankToDefault(proxy.getScheme(), "http"),
                proxy.getHost(),
                proxy.getPort()
        );

        CloseableHttpClient httpClient = createProxyHttpClient(clientConfig, proxy, proxyHost);
        httpClients.add(httpClient);

        log.info(
                "代理 RestClient 请求工厂创建完成，clientName={}，proxy={}://{}:{}",
                clientName,
                proxy.getScheme(),
                proxy.getHost(),
                proxy.getPort()
        );

        return new HttpComponentsClientHttpRequestFactory(httpClient);
    }

    /**
     * 创建代理 HttpClient
     *
     * @param clientConfig 客户端配置
     * @param proxy        代理配置
     * @param proxyHost    代理主机
     * @return HttpClient
     */
    private CloseableHttpClient createProxyHttpClient(
            RemoteHttpProperties.ClientConfig clientConfig,
            RemoteHttpProperties.ProxyConfig proxy,
            HttpHost proxyHost
    ) {
        ConnectionConfig connectionConfig = ConnectionConfig.custom()
                .setConnectTimeout(toTimeout(clientConfig.getConnectTimeout()))
                .build();

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(toTimeout(clientConfig.getConnectionRequestTimeout()))
                .setResponseTimeout(toTimeout(clientConfig.getResponseTimeout()))
                .build();

        HttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setDefaultConnectionConfig(connectionConfig)
                .setMaxConnTotal(clientConfig.getMaxConnTotal())
                .setMaxConnPerRoute(clientConfig.getMaxConnPerRoute())
                .build();

        var httpClientBuilder = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .setProxy(proxyHost)
                .evictExpiredConnections()
                .evictIdleConnections(TimeValue.ofMilliseconds(clientConfig.getMaxIdleTime().toMillis()))
                .disableAutomaticRetries();

        if (StrUtil.isNotBlank(proxy.getUsername())) {
            BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(
                    new AuthScope(proxy.getHost(), proxy.getPort()),
                    new UsernamePasswordCredentials(
                            proxy.getUsername(),
                            StrUtil.nullToEmpty(proxy.getPassword()).toCharArray()
                    )
            );
            httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
        }

        return httpClientBuilder.build();
    }

    /**
     * 校验代理配置
     *
     * @param clientName 客户端名称
     * @param proxy      代理配置
     */
    private void validateProxyConfig(String clientName, RemoteHttpProperties.ProxyConfig proxy) {
        if (proxy == null) {
            throw new IllegalArgumentException(StrUtil.format("代理配置不能为空，clientName={}", clientName));
        }
        if (StrUtil.isBlank(proxy.getHost())) {
            throw new IllegalArgumentException(StrUtil.format("代理 Host 不能为空，clientName={}", clientName));
        }
        if (proxy.getPort() == null || proxy.getPort() <= 0) {
            throw new IllegalArgumentException(StrUtil.format("代理端口不合法，clientName={}，port={}", clientName, proxy.getPort()));
        }
    }

    /**
     * 转换超时时间
     *
     * @param duration Duration
     * @return Timeout
     */
    private Timeout toTimeout(Duration duration) {
        return Timeout.ofMilliseconds(duration.toMillis());
    }

    /**
     * 关闭代理 HttpClient
     */
    @Override
    public void destroy() {
        for (CloseableHttpClient httpClient : httpClients) {
            try {
                httpClient.close();
            } catch (IOException ex) {
                log.warn("关闭代理 HttpClient 失败，message={}", ex.getMessage());
            }
        }
    }

}
```

### RestClient Bean 补充

在已有 `RestClientBaseConfig` 中增加代理客户端 Bean。这里使用常量 `RemoteHttpConstant.CLIENT_PROXY`。

文件位置：`src/main/java/io/github/atengk/http/config/RestClientBaseConfig.java`

```java
    /**
     * 代理 RestClient
     *
     * @param remoteRestClientFactory RestClient 工厂
     * @return RestClient
     */
    @Bean
    public RestClient proxyRestClient(RemoteRestClientFactory remoteRestClientFactory) {
        return remoteRestClientFactory.create(RemoteHttpConstant.CLIENT_PROXY);
    }
```

### 调用示例

这个客户端演示通过代理访问远程接口。业务代码不需要感知代理细节，代理能力由 `ProxyRequestFactoryCustomizer` 自动接入。

文件位置：`src/main/java/io/github/atengk/http/client/ProxyDemoApiClient.java`

```java
package io.github.atengk.http.client;

import io.github.atengk.http.constant.RemoteHttpConstant;
import io.github.atengk.http.support.RemoteCallExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * 代理示例客户端
 *
 * @author Ateng
 * @since 2026-04-29
 */
@Slf4j
@Component
public class ProxyDemoApiClient {

    private final RestClient restClient;

    private final RemoteCallExecutor remoteCallExecutor;

    public ProxyDemoApiClient(
            @Qualifier("proxyRestClient") RestClient restClient,
            RemoteCallExecutor remoteCallExecutor
    ) {
        this.restClient = restClient;
        this.remoteCallExecutor = remoteCallExecutor;
    }

    /**
     * 查询远程服务信息
     *
     * @return 服务信息
     */
    public RemoteInfoResp getRemoteInfo() {
        return remoteCallExecutor.execute(RemoteHttpConstant.CLIENT_PROXY, () -> restClient.get()
                .uri("/api/info")
                .retrieve()
                .body(RemoteInfoResp.class));
    }

    public record RemoteInfoResp(String name, String version, String environment) {
    }

}
```

### 验证方式

如果你有可用代理服务，可以先把 `base-url` 配置为一个能返回请求来源 IP 的接口，例如公司内部测试服务或供应商测试接口。

调用业务方法后，观察代理服务日志或远程服务日志，确认请求确实从代理出口发出。

也可以在代理服务不可用时临时配置错误端口：

```yaml
proxy:
  enabled: true
  scheme: http
  host: 127.0.0.1
  port: 9999
```

如果调用时出现连接代理失败，说明代理配置已生效。

### 与认证能力组合

代理能力和 Bearer Token、API Key、AK/SK 签名可以叠加使用。代理是底层网络出口，认证是业务请求头或签名逻辑，两者不冲突。

例如一个接口既要走代理，又要 Bearer Token：

```yaml
remote:
  http:
    clients:
      proxy:
        base-url: https://api.example.com
        response-timeout: 30s
        default-headers:
          Accept: application/json

        proxy:
          enabled: true
          scheme: http
          host: proxy.example.com
          port: 8080

        bearer-token:
          enabled: true
          token: your-access-token
          header-name: Authorization
          token-prefix: Bearer
```

这样 `proxyRestClient` 会同时具备：

```text
1. 代理访问能力
2. Bearer Token 认证能力
3. 日志能力
4. TraceId 透传能力
5. 统一错误处理能力
6. 连接池和超时能力
```

### 注意事项

代理属于底层网络能力，建议按客户端单独配置，不要默认对所有客户端开启。否则内部服务调用也可能被错误地转发到代理。

生产环境中代理用户名和密码不要写死在代码里，建议从配置中心、环境变量或密钥系统读取。

如果后续同时启用代理和 mTLS，需要注意两者都属于底层请求工厂扩展。最稳的方式是让后续 mTLS 扩展器在创建请求工厂时同时读取 `proxy` 和 `ssl` 配置，构建一个同时支持代理和证书的 Apache HttpClient。当前代理扩展器适合普通代理场景。

## 功能：mTLS

mTLS，也就是双向 TLS 认证，适合银行、支付、政务、供应商专线接口、企业网关等安全要求较高的场景。

普通 HTTPS 只校验服务端证书：

```text
客户端 -> 校验服务端证书 -> 建立 HTTPS 连接
```

mTLS 会同时校验客户端证书：

```text
客户端携带客户端证书 -> 服务端校验客户端证书 -> 客户端校验服务端证书 -> 建立 HTTPS 连接
```

本方案通过 `RemoteRequestFactoryCustomizer` 扩展底层 `ClientHttpRequestFactory`，不修改 `RemoteRestClientFactory` 主流程。

### 配置补充

在已有 `remote.http.clients` 下新增一个 mTLS 客户端配置。证书路径可以使用 `classpath:` 或文件系统路径。

文件位置：`src/main/resources/application.yml`

```yaml
remote:
  http:
    clients:
      mtls:
        base-url: https://secure-api.example.com
        connect-timeout: 5s
        connection-request-timeout: 3s
        response-timeout: 30s
        max-idle-time: 60s
        max-conn-total: 100
        max-conn-per-route: 30
        default-headers:
          Accept: application/json

        ssl:
          # 是否启用 SSL 扩展
          enabled: true

          # mTLS 场景不建议信任所有证书，生产环境必须为 false
          trust-all: false

          # 是否启用主机名校验，生产环境必须为 true
          hostname-verification-enabled: true

          # TLS 协议
          protocol: TLS

          # 客户端证书，用于服务端校验当前调用方身份
          key-store-path: classpath:cert/client.p12
          key-store-password: your-client-cert-password
          key-store-type: PKCS12

          # 信任证书，用于客户端校验服务端证书；如果使用系统默认 CA，可以留空
          trust-store-path: classpath:cert/truststore.p12
          trust-store-password: your-truststore-password
          trust-store-type: PKCS12
```

如果服务端证书是公网可信 CA 签发，`trust-store-path` 可以不配置，使用 JDK 默认信任链即可。

如果服务端证书是自签名证书、企业内部 CA 或供应商私有 CA，必须配置 `trust-store-path`。

### 常量补充

在已有 `RemoteHttpConstant` 中追加 mTLS 客户端名称。

文件位置：`src/main/java/io/github/atengk/http/constant/RemoteHttpConstant.java`

```java
public static final String CLIENT_MTLS = "mtls";
```

### 配置属性核对

如果你前面的基础配置已经包含 `SslConfig`，这里不需要重复添加。只需要确认 `RemoteHttpProperties.ClientConfig` 中有下面字段。

文件位置：`src/main/java/io/github/atengk/http/config/RemoteHttpProperties.java`

```java
@Valid
private SslConfig ssl = new SslConfig();
```

并确认 `getMergedClient` 方法中已经合并 SSL 配置。

```java
merged.setSsl(ObjectUtil.defaultIfNull(source.getSsl(), defaultConfig.getSsl()));
```

如果基础配置中还没有 `SslConfig`，补充下面这个内部类。

```java
    /**
     * SSL 配置
     *
     * @author Ateng
     * @since 2026-04-29
     */
    @Data
    public static class SslConfig {

        private Boolean enabled = false;

        private Boolean trustAll = false;

        private Boolean hostnameVerificationEnabled = true;

        private String protocol = "TLS";

        private String keyStorePath;

        private String keyStorePassword;

        private String keyStoreType = "PKCS12";

        private String trustStorePath;

        private String trustStorePassword;

        private String trustStoreType = "PKCS12";

    }
```

### mTLS 请求工厂扩展器

这个扩展器实现 `RemoteRequestFactoryCustomizer`。当某个客户端开启 `ssl.enabled=true` 且配置了 `key-store-path` 时，会创建支持客户端证书的 Apache HttpClient 请求工厂。

这里同时兼容代理配置。如果同一个客户端同时启用了 `proxy.enabled=true` 和 `ssl.enabled=true`，mTLS 扩展器会在构建底层 HttpClient 时一起设置代理，避免代理扩展器和 mTLS 扩展器互相覆盖。

文件位置：`src/main/java/io/github/atengk/http/extension/MtlsRequestFactoryCustomizer.java`

```java
package io.github.atengk.http.extension;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.http.config.RemoteHttpProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.ssl.DefaultHostnameVerifier;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

/**
 * mTLS 请求工厂扩展器
 *
 * @author Ateng
 * @since 2026-04-29
 */
@Slf4j
@Order(200)
@Component
@RequiredArgsConstructor
public class MtlsRequestFactoryCustomizer implements RemoteRequestFactoryCustomizer, DisposableBean {

    private final ResourceLoader resourceLoader;

    private final List<CloseableHttpClient> httpClients = new CopyOnWriteArrayList<>();

    /**
     * 判断是否支持当前客户端
     *
     * @param clientName   客户端名称
     * @param clientConfig 客户端配置
     * @return 是否支持
     */
    @Override
    public boolean supports(String clientName, RemoteHttpProperties.ClientConfig clientConfig) {
        RemoteHttpProperties.SslConfig ssl = clientConfig.getSsl();
        return ssl != null
                && Boolean.TRUE.equals(ssl.getEnabled())
                && StrUtil.isNotBlank(ssl.getKeyStorePath());
    }

    /**
     * 自定义请求工厂
     *
     * @param clientName             客户端名称
     * @param clientConfig           客户端配置
     * @param requestFactorySupplier 默认请求工厂供应器
     * @return 请求工厂
     */
    @Override
    public ClientHttpRequestFactory customize(
            String clientName,
            RemoteHttpProperties.ClientConfig clientConfig,
            Supplier<ClientHttpRequestFactory> requestFactorySupplier
    ) {
        RemoteHttpProperties.SslConfig ssl = clientConfig.getSsl();
        validateSslConfig(clientName, ssl);

        try {
            SSLContext sslContext = buildSslContext(ssl);
            CloseableHttpClient httpClient = createMtlsHttpClient(clientConfig, sslContext);
            httpClients.add(httpClient);

            log.info("mTLS RestClient 请求工厂创建完成，clientName={}，keyStorePath={}", clientName, ssl.getKeyStorePath());

            return new HttpComponentsClientHttpRequestFactory(httpClient);
        } catch (Exception ex) {
            throw new IllegalStateException(StrUtil.format("创建 mTLS 请求工厂失败，clientName={}", clientName), ex);
        }
    }

    /**
     * 构建 SSLContext
     *
     * @param ssl SSL 配置
     * @return SSLContext
     * @throws Exception 构建异常
     */
    private SSLContext buildSslContext(RemoteHttpProperties.SslConfig ssl) throws Exception {
        KeyStore keyStore = loadKeyStore(
                ssl.getKeyStorePath(),
                ssl.getKeyStorePassword(),
                ssl.getKeyStoreType()
        );

        SSLContextBuilder sslContextBuilder = SSLContextBuilder.create()
                .setProtocol(StrUtil.blankToDefault(ssl.getProtocol(), "TLS"))
                .loadKeyMaterial(keyStore, StrUtil.nullToEmpty(ssl.getKeyStorePassword()).toCharArray());

        if (StrUtil.isNotBlank(ssl.getTrustStorePath())) {
            KeyStore trustStore = loadKeyStore(
                    ssl.getTrustStorePath(),
                    ssl.getTrustStorePassword(),
                    ssl.getTrustStoreType()
            );
            sslContextBuilder.loadTrustMaterial(trustStore, null);
        }

        return sslContextBuilder.build();
    }

    /**
     * 创建 mTLS HttpClient
     *
     * @param clientConfig 客户端配置
     * @param sslContext   SSLContext
     * @return HttpClient
     */
    private CloseableHttpClient createMtlsHttpClient(
            RemoteHttpProperties.ClientConfig clientConfig,
            SSLContext sslContext
    ) {
        SSLConnectionSocketFactory sslSocketFactory = SSLConnectionSocketFactoryBuilder.create()
                .setSslContext(sslContext)
                .setHostnameVerifier(resolveHostnameVerifier(clientConfig.getSsl()))
                .build();

        ConnectionConfig connectionConfig = ConnectionConfig.custom()
                .setConnectTimeout(toTimeout(clientConfig.getConnectTimeout()))
                .build();

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(toTimeout(clientConfig.getConnectionRequestTimeout()))
                .setResponseTimeout(toTimeout(clientConfig.getResponseTimeout()))
                .build();

        HttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setSSLSocketFactory(sslSocketFactory)
                .setDefaultConnectionConfig(connectionConfig)
                .setMaxConnTotal(clientConfig.getMaxConnTotal())
                .setMaxConnPerRoute(clientConfig.getMaxConnPerRoute())
                .build();

        HttpClientBuilder httpClientBuilder = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .evictExpiredConnections()
                .evictIdleConnections(TimeValue.ofMilliseconds(clientConfig.getMaxIdleTime().toMillis()))
                .disableAutomaticRetries();

        applyProxy(clientConfig, httpClientBuilder);

        return httpClientBuilder.build();
    }

    /**
     * 设置代理
     *
     * @param clientConfig     客户端配置
     * @param httpClientBuilder HttpClient Builder
     */
    private void applyProxy(
            RemoteHttpProperties.ClientConfig clientConfig,
            HttpClientBuilder httpClientBuilder
    ) {
        RemoteHttpProperties.ProxyConfig proxy = clientConfig.getProxy();
        if (proxy == null || !Boolean.TRUE.equals(proxy.getEnabled())) {
            return;
        }

        validateProxyConfig(proxy);

        HttpHost proxyHost = new HttpHost(
                StrUtil.blankToDefault(proxy.getScheme(), "http"),
                proxy.getHost(),
                proxy.getPort()
        );
        httpClientBuilder.setProxy(proxyHost);

        if (StrUtil.isNotBlank(proxy.getUsername())) {
            BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(
                    new AuthScope(proxy.getHost(), proxy.getPort()),
                    new UsernamePasswordCredentials(
                            proxy.getUsername(),
                            StrUtil.nullToEmpty(proxy.getPassword()).toCharArray()
                    )
            );
            httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
        }
    }

    /**
     * 加载 KeyStore
     *
     * @param path     证书路径
     * @param password 证书密码
     * @param type     证书类型
     * @return KeyStore
     * @throws Exception 加载异常
     */
    private KeyStore loadKeyStore(String path, String password, String type) throws Exception {
        Resource resource = resourceLoader.getResource(path);
        if (!resource.exists()) {
            throw new IllegalArgumentException(StrUtil.format("证书文件不存在，path={}", path));
        }

        KeyStore keyStore = KeyStore.getInstance(StrUtil.blankToDefault(type, "PKCS12"));
        try (InputStream inputStream = resource.getInputStream()) {
            keyStore.load(inputStream, StrUtil.nullToEmpty(password).toCharArray());
        }
        return keyStore;
    }

    /**
     * 解析主机名校验器
     *
     * @param ssl SSL 配置
     * @return HostnameVerifier
     */
    private HostnameVerifier resolveHostnameVerifier(RemoteHttpProperties.SslConfig ssl) {
        if (Boolean.TRUE.equals(ssl.getHostnameVerificationEnabled())) {
            return new DefaultHostnameVerifier();
        }

        log.warn("mTLS 已关闭主机名校验，仅允许测试环境使用");
        return NoopHostnameVerifier.INSTANCE;
    }

    /**
     * 校验 SSL 配置
     *
     * @param clientName 客户端名称
     * @param ssl        SSL 配置
     */
    private void validateSslConfig(String clientName, RemoteHttpProperties.SslConfig ssl) {
        if (ssl == null) {
            throw new IllegalArgumentException(StrUtil.format("SSL 配置不能为空，clientName={}", clientName));
        }
        if (StrUtil.isBlank(ssl.getKeyStorePath())) {
            throw new IllegalArgumentException(StrUtil.format("mTLS 客户端证书路径不能为空，clientName={}", clientName));
        }
        if (Boolean.TRUE.equals(ssl.getTrustAll())) {
            throw new IllegalArgumentException(StrUtil.format("mTLS 不允许启用 trust-all，clientName={}", clientName));
        }
    }

    /**
     * 校验代理配置
     *
     * @param proxy 代理配置
     */
    private void validateProxyConfig(RemoteHttpProperties.ProxyConfig proxy) {
        if (StrUtil.isBlank(proxy.getHost())) {
            throw new IllegalArgumentException("代理 Host 不能为空");
        }
        if (proxy.getPort() == null || proxy.getPort() <= 0) {
            throw new IllegalArgumentException(StrUtil.format("代理端口不合法，port={}", proxy.getPort()));
        }
    }

    /**
     * 转换超时时间
     *
     * @param duration Duration
     * @return Timeout
     */
    private Timeout toTimeout(Duration duration) {
        return Timeout.ofMilliseconds(duration.toMillis());
    }

    /**
     * 关闭 mTLS HttpClient
     */
    @Override
    public void destroy() {
        for (CloseableHttpClient httpClient : httpClients) {
            try {
                httpClient.close();
            } catch (IOException ex) {
                log.warn("关闭 mTLS HttpClient 失败，message={}", ex.getMessage());
            }
        }
    }

}
```

### RestClient Bean 补充

在已有 `RestClientBaseConfig` 中增加 mTLS 客户端 Bean。这里使用常量 `RemoteHttpConstant.CLIENT_MTLS`。

文件位置：`src/main/java/io/github/atengk/http/config/RestClientBaseConfig.java`

```java
    /**
     * mTLS RestClient
     *
     * @param remoteRestClientFactory RestClient 工厂
     * @return RestClient
     */
    @Bean
    public RestClient mtlsRestClient(RemoteRestClientFactory remoteRestClientFactory) {
        return remoteRestClientFactory.create(RemoteHttpConstant.CLIENT_MTLS);
    }
```

### 调用示例

这个客户端演示通过 mTLS 调用安全接口。业务代码不需要处理证书加载、SSLContext、主机名校验等底层细节。

文件位置：`src/main/java/io/github/atengk/http/client/MtlsDemoApiClient.java`

```java
package io.github.atengk.http.client;

import io.github.atengk.http.constant.RemoteHttpConstant;
import io.github.atengk.http.support.RemoteCallExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * mTLS 示例客户端
 *
 * @author Ateng
 * @since 2026-04-29
 */
@Slf4j
@Component
public class MtlsDemoApiClient {

    private final RestClient restClient;

    private final RemoteCallExecutor remoteCallExecutor;

    public MtlsDemoApiClient(
            @Qualifier("mtlsRestClient") RestClient restClient,
            RemoteCallExecutor remoteCallExecutor
    ) {
        this.restClient = restClient;
        this.remoteCallExecutor = remoteCallExecutor;
    }

    /**
     * 查询安全服务信息
     *
     * @return 安全服务信息
     */
    public SecureInfoResp getSecureInfo() {
        return remoteCallExecutor.execute(RemoteHttpConstant.CLIENT_MTLS, () -> restClient.get()
                .uri("/api/secure/info")
                .retrieve()
                .body(SecureInfoResp.class));
    }

    public record SecureInfoResp(String serviceName, String status, String certificateSubject) {
    }

}
```

### 证书放置方式

如果证书放在项目 classpath 下，推荐路径如下：

```text
src/main/resources/cert/client.p12
src/main/resources/cert/truststore.p12
```

对应配置：

```yaml
ssl:
  enabled: true
  key-store-path: classpath:cert/client.p12
  key-store-password: your-client-cert-password
  key-store-type: PKCS12
  trust-store-path: classpath:cert/truststore.p12
  trust-store-password: your-truststore-password
  trust-store-type: PKCS12
```

如果证书由运维挂载到服务器目录，使用文件路径：

```yaml
ssl:
  enabled: true
  key-store-path: file:/opt/app/cert/client.p12
  key-store-password: your-client-cert-password
  key-store-type: PKCS12
  trust-store-path: file:/opt/app/cert/truststore.p12
  trust-store-password: your-truststore-password
  trust-store-type: PKCS12
```

生产环境更推荐文件挂载或密钥系统注入，不建议把生产证书打进应用 jar 包。

### 与代理组合

如果接口既要走代理，又要使用 mTLS，可以在同一个客户端中同时配置 `proxy` 和 `ssl`。

```yaml
remote:
  http:
    clients:
      mtls:
        base-url: https://secure-api.example.com
        response-timeout: 30s
        default-headers:
          Accept: application/json

        proxy:
          enabled: true
          scheme: http
          host: proxy.example.com
          port: 8080
          username: proxy-user
          password: proxy-password

        ssl:
          enabled: true
          trust-all: false
          hostname-verification-enabled: true
          key-store-path: classpath:cert/client.p12
          key-store-password: your-client-cert-password
          key-store-type: PKCS12
          trust-store-path: classpath:cert/truststore.p12
          trust-store-password: your-truststore-password
          trust-store-type: PKCS12
```

由于 `MtlsRequestFactoryCustomizer` 内部已经读取并应用了 `proxy` 配置，所以该客户端会同时具备：

```text
1. mTLS 客户端证书认证
2. 服务端证书校验
3. 代理访问能力
4. 日志能力
5. TraceId 透传能力
6. 统一错误处理能力
7. 连接池和超时能力
```

### 验证方式

可以先用 `openssl` 查看服务端证书链：

```bash
openssl s_client -connect secure-api.example.com:443 -showcerts
```

这个命令会连接目标服务并输出服务端证书链。你可以用它确认服务端证书是否由公网 CA、企业 CA 或供应商私有 CA 签发。

如果客户端证书或密码错误，通常会出现类似问题：

```text
1. bad_certificate
2. handshake_failure
3. certificate_unknown
4. keystore password was incorrect
5. unable to find valid certification path to requested target
```

其中 `unable to find valid certification path to requested target` 通常表示客户端不信任服务端证书，需要配置 `trust-store-path`。

### 注意事项

mTLS 生产环境必须保持：

```yaml
ssl:
  trust-all: false
  hostname-verification-enabled: true
```

不要在生产环境关闭主机名校验，也不要信任所有证书。`trust-all` 应放到后续“忽略 SSL”章节中，仅用于本地测试或临时联调。

客户端证书密码、信任库密码不要写死在代码里，建议从配置中心、环境变量、Kubernetes Secret、Vault 或云厂商密钥管理服务读取。

如果客户端证书中的 key password 和 store password 不一致，需要在 `SslConfig` 中额外增加 `key-password` 字段，并在 `loadKeyMaterial(keyStore, keyPassword)` 时使用 key password。当前示例默认两者一致。

## 功能：忽略 SSL

忽略 SSL 适合本地开发、测试环境、临时联调、自签名证书接口等场景。它会跳过服务端证书校验，必要时还会关闭主机名校验。

这种能力有明显安全风险，只允许在非生产环境使用：

```text
1. 本地开发环境
2. 测试环境
3. 临时联调环境
4. 自签名证书接口调试
```

不要在生产环境启用：

```yaml
ssl:
  trust-all: true
  hostname-verification-enabled: false
```

本方案通过 `RemoteRequestFactoryCustomizer` 扩展底层请求工厂，不修改 `RemoteRestClientFactory` 主流程。

### 配置补充

在已有 `remote.http.clients` 下新增一个忽略 SSL 的客户端配置。

文件位置：`src/main/resources/application.yml`

```yaml
remote:
  http:
    clients:
      unsafe-ssl:
        base-url: https://self-signed-api.example.com
        connect-timeout: 5s
        connection-request-timeout: 3s
        response-timeout: 30s
        max-idle-time: 60s
        max-conn-total: 100
        max-conn-per-route: 30
        default-headers:
          Accept: application/json

        ssl:
          # 是否启用 SSL 扩展
          enabled: true

          # 是否信任所有证书；仅允许测试环境使用
          trust-all: true

          # 是否启用主机名校验；忽略 SSL 时通常关闭
          hostname-verification-enabled: false

          # TLS 协议
          protocol: TLS

          # 忽略 SSL 场景不需要配置客户端证书
          key-store-path:
          key-store-password:
          key-store-type: PKCS12

          # 忽略 SSL 场景不需要配置服务端信任库
          trust-store-path:
          trust-store-password:
          trust-store-type: PKCS12
```

如果只是信任自签名证书，但仍想保持相对安全，更推荐配置 `trust-store-path`，而不是 `trust-all: true`。`trust-all` 是更宽松、更危险的方式。

### 常量补充

在已有 `RemoteHttpConstant` 中追加忽略 SSL 客户端名称。

文件位置：`src/main/java/io/github/atengk/http/constant/RemoteHttpConstant.java`

```java
public static final String CLIENT_UNSAFE_SSL = "unsafe-ssl";
```

### 配置属性核对

如果前面的基础配置已经包含 `SslConfig`，这里不需要重复添加。只需要确认 `RemoteHttpProperties.ClientConfig` 中有下面字段。

文件位置：`src/main/java/io/github/atengk/http/config/RemoteHttpProperties.java`

```java
@Valid
private SslConfig ssl = new SslConfig();
```

并确认 `getMergedClient` 方法中已经合并 SSL 配置。

```java
merged.setSsl(ObjectUtil.defaultIfNull(source.getSsl(), defaultConfig.getSsl()));
```

如果基础配置中还没有 `SslConfig`，补充下面这个内部类。

```java
    /**
     * SSL 配置
     *
     * @author Ateng
     * @since 2026-04-29
     */
    @Data
    public static class SslConfig {

        private Boolean enabled = false;

        private Boolean trustAll = false;

        private Boolean hostnameVerificationEnabled = true;

        private String protocol = "TLS";

        private String keyStorePath;

        private String keyStorePassword;

        private String keyStoreType = "PKCS12";

        private String trustStorePath;

        private String trustStorePassword;

        private String trustStoreType = "PKCS12";

    }
```

### 忽略 SSL 请求工厂扩展器

这个扩展器实现 `RemoteRequestFactoryCustomizer`。当某个客户端开启 `ssl.enabled=true` 且 `ssl.trust-all=true` 时，会创建信任所有证书的 Apache HttpClient 请求工厂。

这里也兼容代理配置。如果同一个客户端同时配置了 `proxy.enabled=true`，该扩展器会一起设置代理。

文件位置：`src/main/java/io/github/atengk/http/extension/UnsafeSslRequestFactoryCustomizer.java`

```java
package io.github.atengk.http.extension;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.http.config.RemoteHttpProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.ssl.DefaultHostnameVerifier;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.apache.hc.core5.ssl.TrustAllStrategy;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.core.annotation.Order;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

/**
 * 忽略 SSL 请求工厂扩展器
 *
 * @author Ateng
 * @since 2026-04-29
 */
@Slf4j
@Order(300)
@Component
public class UnsafeSslRequestFactoryCustomizer implements RemoteRequestFactoryCustomizer, DisposableBean {

    private final List<CloseableHttpClient> httpClients = new CopyOnWriteArrayList<>();

    /**
     * 判断是否支持当前客户端
     *
     * @param clientName   客户端名称
     * @param clientConfig 客户端配置
     * @return 是否支持
     */
    @Override
    public boolean supports(String clientName, RemoteHttpProperties.ClientConfig clientConfig) {
        RemoteHttpProperties.SslConfig ssl = clientConfig.getSsl();
        return ssl != null
                && Boolean.TRUE.equals(ssl.getEnabled())
                && Boolean.TRUE.equals(ssl.getTrustAll());
    }

    /**
     * 自定义请求工厂
     *
     * @param clientName             客户端名称
     * @param clientConfig           客户端配置
     * @param requestFactorySupplier 默认请求工厂供应器
     * @return 请求工厂
     */
    @Override
    public ClientHttpRequestFactory customize(
            String clientName,
            RemoteHttpProperties.ClientConfig clientConfig,
            Supplier<ClientHttpRequestFactory> requestFactorySupplier
    ) {
        RemoteHttpProperties.SslConfig ssl = clientConfig.getSsl();
        validateSslConfig(clientName, ssl);

        try {
            SSLContext sslContext = SSLContextBuilder.create()
                    .setProtocol(StrUtil.blankToDefault(ssl.getProtocol(), "TLS"))
                    .loadTrustMaterial(TrustAllStrategy.INSTANCE)
                    .build();

            CloseableHttpClient httpClient = createUnsafeSslHttpClient(clientConfig, sslContext);
            httpClients.add(httpClient);

            log.warn(
                    "忽略 SSL 校验 RestClient 请求工厂创建完成，仅允许测试环境使用，clientName={}，hostnameVerificationEnabled={}",
                    clientName,
                    ssl.getHostnameVerificationEnabled()
            );

            return new HttpComponentsClientHttpRequestFactory(httpClient);
        } catch (Exception ex) {
            throw new IllegalStateException(StrUtil.format("创建忽略 SSL 请求工厂失败，clientName={}", clientName), ex);
        }
    }

    /**
     * 创建忽略 SSL 校验的 HttpClient
     *
     * @param clientConfig 客户端配置
     * @param sslContext   SSLContext
     * @return HttpClient
     */
    private CloseableHttpClient createUnsafeSslHttpClient(
            RemoteHttpProperties.ClientConfig clientConfig,
            SSLContext sslContext
    ) {
        SSLConnectionSocketFactory sslSocketFactory = SSLConnectionSocketFactoryBuilder.create()
                .setSslContext(sslContext)
                .setHostnameVerifier(resolveHostnameVerifier(clientConfig.getSsl()))
                .build();

        ConnectionConfig connectionConfig = ConnectionConfig.custom()
                .setConnectTimeout(toTimeout(clientConfig.getConnectTimeout()))
                .build();

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(toTimeout(clientConfig.getConnectionRequestTimeout()))
                .setResponseTimeout(toTimeout(clientConfig.getResponseTimeout()))
                .build();

        HttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setSSLSocketFactory(sslSocketFactory)
                .setDefaultConnectionConfig(connectionConfig)
                .setMaxConnTotal(clientConfig.getMaxConnTotal())
                .setMaxConnPerRoute(clientConfig.getMaxConnPerRoute())
                .build();

        HttpClientBuilder httpClientBuilder = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .evictExpiredConnections()
                .evictIdleConnections(TimeValue.ofMilliseconds(clientConfig.getMaxIdleTime().toMillis()))
                .disableAutomaticRetries();

        applyProxy(clientConfig, httpClientBuilder);

        return httpClientBuilder.build();
    }

    /**
     * 设置代理
     *
     * @param clientConfig      客户端配置
     * @param httpClientBuilder HttpClient Builder
     */
    private void applyProxy(
            RemoteHttpProperties.ClientConfig clientConfig,
            HttpClientBuilder httpClientBuilder
    ) {
        RemoteHttpProperties.ProxyConfig proxy = clientConfig.getProxy();
        if (proxy == null || !Boolean.TRUE.equals(proxy.getEnabled())) {
            return;
        }

        validateProxyConfig(proxy);

        HttpHost proxyHost = new HttpHost(
                StrUtil.blankToDefault(proxy.getScheme(), "http"),
                proxy.getHost(),
                proxy.getPort()
        );
        httpClientBuilder.setProxy(proxyHost);

        if (StrUtil.isNotBlank(proxy.getUsername())) {
            BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(
                    new AuthScope(proxy.getHost(), proxy.getPort()),
                    new UsernamePasswordCredentials(
                            proxy.getUsername(),
                            StrUtil.nullToEmpty(proxy.getPassword()).toCharArray()
                    )
            );
            httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
        }
    }

    /**
     * 解析主机名校验器
     *
     * @param ssl SSL 配置
     * @return HostnameVerifier
     */
    private HostnameVerifier resolveHostnameVerifier(RemoteHttpProperties.SslConfig ssl) {
        if (Boolean.TRUE.equals(ssl.getHostnameVerificationEnabled())) {
            return new DefaultHostnameVerifier();
        }

        log.warn("已关闭 HTTPS 主机名校验，仅允许测试环境使用");
        return NoopHostnameVerifier.INSTANCE;
    }

    /**
     * 校验 SSL 配置
     *
     * @param clientName 客户端名称
     * @param ssl        SSL 配置
     */
    private void validateSslConfig(String clientName, RemoteHttpProperties.SslConfig ssl) {
        if (ssl == null) {
            throw new IllegalArgumentException(StrUtil.format("SSL 配置不能为空，clientName={}", clientName));
        }
        if (!Boolean.TRUE.equals(ssl.getTrustAll())) {
            throw new IllegalArgumentException(StrUtil.format("忽略 SSL 必须启用 trust-all，clientName={}", clientName));
        }
        if (StrUtil.isNotBlank(ssl.getKeyStorePath())) {
            throw new IllegalArgumentException(StrUtil.format("忽略 SSL 客户端不应配置 key-store-path，clientName={}", clientName));
        }
    }

    /**
     * 校验代理配置
     *
     * @param proxy 代理配置
     */
    private void validateProxyConfig(RemoteHttpProperties.ProxyConfig proxy) {
        if (StrUtil.isBlank(proxy.getHost())) {
            throw new IllegalArgumentException("代理 Host 不能为空");
        }
        if (proxy.getPort() == null || proxy.getPort() <= 0) {
            throw new IllegalArgumentException(StrUtil.format("代理端口不合法，port={}", proxy.getPort()));
        }
    }

    /**
     * 转换超时时间
     *
     * @param duration Duration
     * @return Timeout
     */
    private Timeout toTimeout(Duration duration) {
        return Timeout.ofMilliseconds(duration.toMillis());
    }

    /**
     * 关闭忽略 SSL 的 HttpClient
     */
    @Override
    public void destroy() {
        for (CloseableHttpClient httpClient : httpClients) {
            try {
                httpClient.close();
            } catch (IOException ex) {
                log.warn("关闭忽略 SSL HttpClient 失败，message={}", ex.getMessage());
            }
        }
    }

}
```

### RestClient Bean 补充

在已有 `RestClientBaseConfig` 中增加忽略 SSL 客户端 Bean。这里使用常量 `RemoteHttpConstant.CLIENT_UNSAFE_SSL`。

文件位置：`src/main/java/io/github/atengk/http/config/RestClientBaseConfig.java`

```java
    /**
     * 忽略 SSL RestClient
     *
     * @param remoteRestClientFactory RestClient 工厂
     * @return RestClient
     */
    @Bean
    public RestClient unsafeSslRestClient(RemoteRestClientFactory remoteRestClientFactory) {
        return remoteRestClientFactory.create(RemoteHttpConstant.CLIENT_UNSAFE_SSL);
    }
```

### 调用示例

这个客户端演示调用自签名 HTTPS 接口。证书信任逻辑由 `UnsafeSslRequestFactoryCustomizer` 完成，业务代码不需要处理 SSL 细节。

文件位置：`src/main/java/io/github/atengk/http/client/UnsafeSslDemoApiClient.java`

```java
package io.github.atengk.http.client;

import io.github.atengk.http.constant.RemoteHttpConstant;
import io.github.atengk.http.support.RemoteCallExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * 忽略 SSL 示例客户端
 *
 * @author Ateng
 * @since 2026-04-29
 */
@Slf4j
@Component
public class UnsafeSslDemoApiClient {

    private final RestClient restClient;

    private final RemoteCallExecutor remoteCallExecutor;

    public UnsafeSslDemoApiClient(
            @Qualifier("unsafeSslRestClient") RestClient restClient,
            RemoteCallExecutor remoteCallExecutor
    ) {
        this.restClient = restClient;
        this.remoteCallExecutor = remoteCallExecutor;
    }

    /**
     * 查询自签名 HTTPS 服务信息
     *
     * @return 服务信息
     */
    public UnsafeSslInfoResp getInfo() {
        return remoteCallExecutor.execute(RemoteHttpConstant.CLIENT_UNSAFE_SSL, () -> restClient.get()
                .uri("/api/info")
                .retrieve()
                .body(UnsafeSslInfoResp.class));
    }

    public record UnsafeSslInfoResp(String name, String version, String status) {
    }

}
```

### 与代理组合

忽略 SSL 可以和代理组合使用。比如测试环境需要通过代理访问一个自签名 HTTPS 接口：

```yaml
remote:
  http:
    clients:
      unsafe-ssl:
        base-url: https://self-signed-api.example.com
        response-timeout: 30s
        default-headers:
          Accept: application/json

        proxy:
          enabled: true
          scheme: http
          host: proxy.example.com
          port: 8080
          username: proxy-user
          password: proxy-password

        ssl:
          enabled: true
          trust-all: true
          hostname-verification-enabled: false
          protocol: TLS
```

这样 `unsafeSslRestClient` 会同时具备：

```text
1. 忽略服务端证书校验
2. 可选关闭主机名校验
3. 代理访问能力
4. 日志能力
5. TraceId 透传能力
6. 统一错误处理能力
7. 连接池和超时能力
```

### 与 mTLS 的关系

`unsafeSslRestClient` 和 `mtlsRestClient` 不建议混用。

mTLS 的目标是强校验：

```text
1. 服务端校验客户端证书
2. 客户端校验服务端证书
3. 保持主机名校验
```

忽略 SSL 的目标是临时绕过校验：

```text
1. 信任所有服务端证书
2. 可选关闭主机名校验
3. 仅用于测试和临时联调
```

因此同一个客户端不要同时配置：

```yaml
ssl:
  trust-all: true
  key-store-path: classpath:cert/client.p12
```

如果确实遇到“mTLS + 自签名服务端证书”的情况，正确做法不是 `trust-all: true`，而是配置 `trust-store-path`，把服务端 CA 或服务端证书加入信任库。

### 验证方式

可以先使用一个自签名 HTTPS 测试服务验证。未启用忽略 SSL 时，通常会出现类似异常：

```text
PKIX path building failed
unable to find valid certification path to requested target
```

启用下面配置后，如果接口能正常调用，说明忽略 SSL 配置已生效。

```yaml
ssl:
  enabled: true
  trust-all: true
  hostname-verification-enabled: false
```

### 注意事项

生产环境禁止使用忽略 SSL。更安全的方式是：

```text
1. 使用公网可信 CA 证书
2. 使用企业内部 CA，并配置 trust-store-path
3. 使用供应商提供的 CA 证书，并配置 trust-store-path
4. 保持 hostname-verification-enabled: true
```

`trust-all: true` 会让客户端信任任意服务端证书。如果网络链路被中间人劫持，客户端也可能无法识别风险。

`hostname-verification-enabled: false` 会跳过域名和证书 CN/SAN 的匹配校验。即使服务端证书不是当前域名签发的，也可能被接受。

因此该能力只作为测试和临时联调工具保留，不应作为正式环境方案。

## 功能：XML

XML 适合对接老系统、银行接口、政务接口、SOAP-like 接口、供应商存量接口等场景。虽然新系统更常用 JSON，但实际企业集成中 XML 仍然很常见。

本方案通过 `RemoteRestClientBuilderCustomizer` 给指定 `RestClient` 增加 XML 消息转换器，不修改 `RemoteRestClientFactory` 主流程。

最终结构如下：

```text
xmlRestClient
  -> RemoteRestClientFactory
      -> XmlRestClientBuilderCustomizer
          -> MappingJackson2XmlHttpMessageConverter
```

XML 调用通常需要明确设置：

```text
Content-Type: application/xml
Accept: application/xml
```

### Maven 依赖补充

XML 支持需要额外引入 Jackson XML。版本由 Spring Boot 依赖管理控制，不建议手动指定版本。

文件位置：`pom.xml`

```xml
<!-- Jackson XML：支持 application/xml、text/xml 的序列化和反序列化 -->
<dependency>
    <groupId>com.fasterxml.jackson.dataformat</groupId>
    <artifactId>jackson-dataformat-xml</artifactId>
</dependency>
```

### 配置补充

在已有 `remote.http.clients` 下新增一个 XML 客户端配置。

文件位置：`src/main/resources/application.yml`

```yaml
remote:
  http:
    clients:
      xml:
        base-url: https://xml-api.example.com
        connect-timeout: 5s
        connection-request-timeout: 3s
        response-timeout: 30s
        max-idle-time: 60s
        max-conn-total: 100
        max-conn-per-route: 30
        default-headers:
          Accept: application/xml

        xml:
          # 是否启用 XML 消息转换器
          enabled: true

          # 是否格式化输出 XML；生产环境通常关闭
          pretty-print: false
```

如果对方接口返回 `text/xml`，也可以把 `Accept` 改成：

```yaml
default-headers:
  Accept: text/xml
```

实际调用方法中仍然可以通过 `.accept(...)` 覆盖默认值。

### 常量补充

在已有 `RemoteHttpConstant` 中追加 XML 客户端名称。

文件位置：`src/main/java/io/github/atengk/http/constant/RemoteHttpConstant.java`

```java
public static final String CLIENT_XML = "xml";
```

### 配置属性补充

在 `RemoteHttpProperties.ClientConfig` 中增加 XML 配置字段。

文件位置：`src/main/java/io/github/atengk/http/config/RemoteHttpProperties.java`

```java
@Valid
private XmlConfig xml = new XmlConfig();
```

在 `getMergedClient` 方法中增加 XML 配置合并逻辑。

```java
merged.setXml(mergeXmlConfig(defaultConfig.getXml(), source.getXml()));
```

在 `RemoteHttpProperties` 中增加合并方法。

```java
    /**
     * 合并 XML 配置
     *
     * @param defaultValue 默认配置
     * @param source       当前客户端配置
     * @return 合并后的配置
     */
    private XmlConfig mergeXmlConfig(XmlConfig defaultValue, XmlConfig source) {
        XmlConfig merged = new XmlConfig();
        merged.setEnabled(ObjectUtil.defaultIfNull(source.getEnabled(), defaultValue.getEnabled()));
        merged.setPrettyPrint(ObjectUtil.defaultIfNull(source.getPrettyPrint(), defaultValue.getPrettyPrint()));
        return merged;
    }
```

在 `RemoteHttpProperties` 中增加 XML 配置类。

```java
    /**
     * XML 配置
     *
     * @author Ateng
     * @since 2026-04-29
     */
    @Data
    public static class XmlConfig {

        private Boolean enabled = false;

        private Boolean prettyPrint = false;

    }
```

### XML Builder 扩展器

这个扩展器会在 `RestClient` 创建时读取当前客户端配置。如果开启了 `xml.enabled=true`，就自动添加 `MappingJackson2XmlHttpMessageConverter`。

文件位置：`src/main/java/io/github/atengk/http/extension/XmlRestClientBuilderCustomizer.java`

```java
package io.github.atengk.http.extension;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;
import io.github.atengk.http.config.RemoteHttpProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * XML RestClient 扩展器
 *
 * @author Ateng
 * @since 2026-04-29
 */
@Slf4j
@Component
public class XmlRestClientBuilderCustomizer implements RemoteRestClientBuilderCustomizer {

    /**
     * 判断是否支持当前客户端
     *
     * @param clientName   客户端名称
     * @param clientConfig 客户端配置
     * @return 是否支持
     */
    @Override
    public boolean supports(String clientName, RemoteHttpProperties.ClientConfig clientConfig) {
        RemoteHttpProperties.XmlConfig xml = clientConfig.getXml();
        return xml != null && Boolean.TRUE.equals(xml.getEnabled());
    }

    /**
     * 自定义 RestClient Builder
     *
     * @param clientName   客户端名称
     * @param clientConfig 客户端配置
     * @param builder      RestClient Builder
     */
    @Override
    public void customize(String clientName, RemoteHttpProperties.ClientConfig clientConfig, RestClient.Builder builder) {
        MappingJackson2XmlHttpMessageConverter xmlConverter = buildXmlConverter(clientConfig.getXml());

        builder.messageConverters(converters -> {
            removeOldXmlConverter(converters);
            converters.add(0, xmlConverter);
        });

        log.info("XML 消息转换器已启用，clientName={}", clientName);
    }

    /**
     * 构建 XML 消息转换器
     *
     * @param xml XML 配置
     * @return XML 消息转换器
     */
    private MappingJackson2XmlHttpMessageConverter buildXmlConverter(RemoteHttpProperties.XmlConfig xml) {
        XmlMapper xmlMapper = XmlMapper.builder()
                .configure(ToXmlGenerator.Feature.WRITE_XML_DECLARATION, true)
                .configure(SerializationFeature.INDENT_OUTPUT, Boolean.TRUE.equals(xml.getPrettyPrint()))
                .build();

        MappingJackson2XmlHttpMessageConverter converter = new MappingJackson2XmlHttpMessageConverter(xmlMapper);
        converter.setSupportedMediaTypes(List.of(
                MediaType.APPLICATION_XML,
                MediaType.TEXT_XML,
                MediaType.valueOf("application/*+xml")
        ));
        return converter;
    }

    /**
     * 移除旧 XML 转换器
     *
     * @param converters 消息转换器列表
     */
    private void removeOldXmlConverter(List<HttpMessageConverter<?>> converters) {
        converters.removeIf(MappingJackson2XmlHttpMessageConverter.class::isInstance);
    }

}
```

### RestClient Bean 补充

在已有 `RestClientBaseConfig` 中增加 XML 客户端 Bean。这里使用常量 `RemoteHttpConstant.CLIENT_XML`。

文件位置：`src/main/java/io/github/atengk/http/config/RestClientBaseConfig.java`

```java
    /**
     * XML RestClient
     *
     * @param remoteRestClientFactory RestClient 工厂
     * @return RestClient
     */
    @Bean
    public RestClient xmlRestClient(RemoteRestClientFactory remoteRestClientFactory) {
        return remoteRestClientFactory.create(RemoteHttpConstant.CLIENT_XML);
    }
```

### XML 请求 DTO

XML DTO 建议显式声明根节点和字段节点名称。这样即使 Java 字段名调整，也不容易影响对接方 XML 协议。

文件位置：`src/main/java/io/github/atengk/http/client/dto/XmlOrderQueryReq.java`

```java
package io.github.atengk.http.client.dto;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Data;

/**
 * XML 订单查询请求
 *
 * @author Ateng
 * @since 2026-04-29
 */
@Data
@JacksonXmlRootElement(localName = "OrderQueryRequest")
public class XmlOrderQueryReq {

    /**
     * 订单号
     */
    @JacksonXmlProperty(localName = "OrderNo")
    private String orderNo;

    /**
     * 商户号
     */
    @JacksonXmlProperty(localName = "MerchantNo")
    private String merchantNo;

}
```

### XML 响应 DTO

响应 DTO 同样建议显式声明 XML 节点名称，避免大小写、下划线、命名风格不一致导致反序列化失败。

文件位置：`src/main/java/io/github/atengk/http/client/dto/XmlOrderQueryResp.java`

```java
package io.github.atengk.http.client.dto;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Data;

/**
 * XML 订单查询响应
 *
 * @author Ateng
 * @since 2026-04-29
 */
@Data
@JacksonXmlRootElement(localName = "OrderQueryResponse")
public class XmlOrderQueryResp {

    /**
     * 响应编码
     */
    @JacksonXmlProperty(localName = "Code")
    private String code;

    /**
     * 响应消息
     */
    @JacksonXmlProperty(localName = "Message")
    private String message;

    /**
     * 订单号
     */
    @JacksonXmlProperty(localName = "OrderNo")
    private String orderNo;

    /**
     * 订单状态
     */
    @JacksonXmlProperty(localName = "Status")
    private String status;

}
```

### XML 客户端

这个客户端演示两种 XML 调用方式：

```text
1. POST XML 对象，请求体和响应体都使用 XML
2. GET XML 响应，只接收 XML 响应体
```

文件位置：`src/main/java/io/github/atengk/http/client/XmlDemoApiClient.java`

```java
package io.github.atengk.http.client;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.http.client.dto.XmlOrderQueryReq;
import io.github.atengk.http.client.dto.XmlOrderQueryResp;
import io.github.atengk.http.constant.RemoteHttpConstant;
import io.github.atengk.http.support.RemoteCallContext;
import io.github.atengk.http.support.RemoteCallExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * XML 示例客户端
 *
 * @author Ateng
 * @since 2026-04-29
 */
@Slf4j
@Component
public class XmlDemoApiClient {

    private final RestClient restClient;

    private final RemoteCallExecutor remoteCallExecutor;

    public XmlDemoApiClient(
            @Qualifier("xmlRestClient") RestClient restClient,
            RemoteCallExecutor remoteCallExecutor
    ) {
        this.restClient = restClient;
        this.remoteCallExecutor = remoteCallExecutor;
    }

    /**
     * 使用 POST XML 查询订单
     *
     * @param request 查询请求
     * @return 查询响应
     */
    public XmlOrderQueryResp queryOrderByPost(XmlOrderQueryReq request) {
        if (request == null || StrUtil.isBlank(request.getOrderNo())) {
            throw new IllegalArgumentException("订单号不能为空");
        }

        RemoteCallContext context = RemoteCallContext.builder()
                .clientName(RemoteHttpConstant.CLIENT_XML)
                .method("POST")
                .description("POST XML 查询订单")
                .idempotent(true)
                .build();

        log.info("开始调用 XML 订单查询接口，orderNo={}", request.getOrderNo());

        return remoteCallExecutor.execute(context, () -> restClient.post()
                .uri("/api/xml/orders/query")
                .contentType(MediaType.APPLICATION_XML)
                .accept(MediaType.APPLICATION_XML)
                .body(request)
                .retrieve()
                .body(XmlOrderQueryResp.class));
    }

    /**
     * 使用 GET 接收 XML 响应
     *
     * @param orderNo 订单号
     * @return 查询响应
     */
    public XmlOrderQueryResp queryOrderByGet(String orderNo) {
        if (StrUtil.isBlank(orderNo)) {
            throw new IllegalArgumentException("订单号不能为空");
        }

        RemoteCallContext context = RemoteCallContext.builder()
                .clientName(RemoteHttpConstant.CLIENT_XML)
                .method("GET")
                .description("GET XML 查询订单")
                .idempotent(true)
                .build();

        log.info("开始调用 GET XML 订单查询接口，orderNo={}", orderNo);

        return remoteCallExecutor.execute(context, () -> restClient.get()
                .uri("/api/xml/orders/{orderNo}", orderNo)
                .accept(MediaType.APPLICATION_XML)
                .retrieve()
                .body(XmlOrderQueryResp.class));
    }

}
```

### XML 字符串调用方式

如果对接方 XML 协议非常不规范，或者需要手动控制 XML 声明、CDATA、命名空间、字段顺序，可以直接发送 XML 字符串。

这种方式不依赖 DTO 注解，适合复杂老接口。

文件位置：`src/main/java/io/github/atengk/http/client/XmlRawApiClient.java`

```java
package io.github.atengk.http.client;

import cn.hutool.core.util.XmlUtil;
import io.github.atengk.http.constant.RemoteHttpConstant;
import io.github.atengk.http.support.RemoteCallContext;
import io.github.atengk.http.support.RemoteCallExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.w3c.dom.Document;

/**
 * XML 原始字符串示例客户端
 *
 * @author Ateng
 * @since 2026-04-29
 */
@Slf4j
@Component
public class XmlRawApiClient {

    private final RestClient restClient;

    private final RemoteCallExecutor remoteCallExecutor;

    public XmlRawApiClient(
            @Qualifier("xmlRestClient") RestClient restClient,
            RemoteCallExecutor remoteCallExecutor
    ) {
        this.restClient = restClient;
        this.remoteCallExecutor = remoteCallExecutor;
    }

    /**
     * 使用 XML 字符串查询订单
     *
     * @param orderNo 订单号
     * @return XML 响应文档
     */
    public Document queryOrderByRawXml(String orderNo) {
        String xmlBody = """
                <?xml version="1.0" encoding="UTF-8"?>
                <OrderQueryRequest>
                    <OrderNo>%s</OrderNo>
                </OrderQueryRequest>
                """.formatted(orderNo);

        RemoteCallContext context = RemoteCallContext.builder()
                .clientName(RemoteHttpConstant.CLIENT_XML)
                .method("POST")
                .description("原始 XML 字符串查询订单")
                .idempotent(true)
                .build();

        log.info("开始调用原始 XML 订单查询接口，orderNo={}", orderNo);

        String responseXml = remoteCallExecutor.execute(context, () -> restClient.post()
                .uri("/api/xml/orders/query")
                .contentType(MediaType.APPLICATION_XML)
                .accept(MediaType.APPLICATION_XML)
                .body(xmlBody)
                .retrieve()
                .body(String.class));

        return XmlUtil.parseXml(responseXml);
    }

}
```

### XML 命名空间处理

如果接口 XML 带命名空间，例如：

```xml
<OrderQueryRequest xmlns="http://example.com/order">
    <OrderNo>10001</OrderNo>
</OrderQueryRequest>
```

DTO 可以这样写：

```java
@JacksonXmlRootElement(localName = "OrderQueryRequest", namespace = "http://example.com/order")
public class XmlOrderQueryReq {
}
```

字段也可以声明命名空间：

```java
@JacksonXmlProperty(localName = "OrderNo", namespace = "http://example.com/order")
private String orderNo;
```

如果对方命名空间规则复杂，优先使用 XML 字符串方式手动构造请求。

### 与认证能力组合

XML 可以和 Bearer Token、API Key、AK/SK 签名、代理、mTLS 组合使用。比如银行接口经常是：

```text
XML + mTLS + AK/SK 签名
```

配置示例：

```yaml
remote:
  http:
    clients:
      xml:
        base-url: https://secure-xml-api.example.com
        response-timeout: 30s
        default-headers:
          Accept: application/xml

        xml:
          enabled: true
          pretty-print: false

        sign:
          enabled: true
          access-key: your-access-key
          secret-key: your-secret-key

        ssl:
          enabled: true
          trust-all: false
          hostname-verification-enabled: true
          key-store-path: classpath:cert/client.p12
          key-store-password: your-client-cert-password
          key-store-type: PKCS12
          trust-store-path: classpath:cert/truststore.p12
          trust-store-password: your-truststore-password
          trust-store-type: PKCS12
```

这样 `xmlRestClient` 会同时具备：

```text
1. XML 序列化和反序列化
2. AK/SK 签名
3. mTLS 双向认证
4. 日志能力
5. TraceId 透传能力
6. 统一错误处理能力
7. 连接池和超时能力
```

### 注意事项

XML 对字段名称、根节点名称、命名空间和字段顺序更敏感。对接前必须确认对方接口文档中的 XML 样例。

如果 DTO 方式反序列化失败，优先检查：

```text
1. 根节点名称是否一致
2. 字段节点大小写是否一致
3. 是否存在命名空间
4. 是否存在 CDATA
5. 响应 Content-Type 是否是 application/xml 或 text/xml
6. 响应是否实际返回了 HTML 错误页
```

如果接口返回 `text/xml;charset=UTF-8`，当前 `MappingJackson2XmlHttpMessageConverter` 已支持 `MediaType.TEXT_XML`。

如果对方返回 XML 但 `Content-Type` 写成 `text/plain`，建议让对方修正响应头；实在无法修正时，可以使用 `body(String.class)` 先接收字符串，再用 Hutool `XmlUtil.parseXml(...)` 手动解析。