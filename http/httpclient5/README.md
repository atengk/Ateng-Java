# Apache HttpClient 5

Apache HttpClient 5 是一款功能强大的 Java HTTP 客户端库，用于发送 HTTP/HTTPS 请求，支持同步和异步操作。它提供了连接池管理、请求重试、超时控制、拦截器和高级 TLS 配置，便于构建高性能、可靠的客户端应用。

相比 4.x 版本，HttpClient 5 重构了 API，支持 HTTP/2、多路复用和现代 TLS 策略，同时提供更灵活的请求配置和响应处理机制，使开发者能够更精细地控制连接和安全策略。



## 添加依赖

```xml
<properties>
    <spring.boot.version>3.5.7</spring.boot.version>
</properties>
```

```xml
<!-- Apache HttpClient 5 HTTP 客户端库 -->
<dependency>
    <groupId>org.apache.httpcomponents.client5</groupId>
    <artifactId>httpclient5</artifactId>
</dependency>
```

或者加上版本（Springboot管理了版本，一般情况下不用配置）

```xml
<!-- Apache HttpClient 5 HTTP 客户端库 -->
<dependency>
    <groupId>org.apache.httpcomponents.client5</groupId>
    <artifactId>httpclient5</artifactId>
    <version>5.5.1</version>
</dependency>
```



## 使用

### GET 请求示例

```java
    /**
     * GET 请求示例
     */
    @Test
    void testGet() throws IOException {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpGet request = new HttpGet("https://jsonplaceholder.typicode.com/posts/1");

            String body = client.execute(request, response ->
                    EntityUtils.toString(response.getEntity())
            );

            System.out.println("GET Response:\n" + body);

        }
    }
```

### POST 请求示例（JSON body）

```java
    /**
     * POST 请求示例（JSON body）
     */
    @Test
    void testPost() throws IOException {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost request = new HttpPost("https://jsonplaceholder.typicode.com/posts");

            // JSON 请求体
            String json = "{ \"title\": \"foo\", \"body\": \"bar\", \"userId\": 1 }";
            request.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));

            String body = client.execute(request, response ->
                    EntityUtils.toString(response.getEntity())
            );

            System.out.println("POST Response:\n" + body);
        }
    }
```

### PUT 请求示例（更新资源）

```java
    /**
     * PUT 请求示例（更新资源）
     */
    @Test
    void testPut() throws IOException {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPut request = new HttpPut("https://jsonplaceholder.typicode.com/posts/1");

            String json = "{ \"id\": 1, \"title\": \"updated\", \"body\": \"bar\", \"userId\": 1 }";
            request.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));

            String body = client.execute(request, response ->
                    EntityUtils.toString(response.getEntity())
            );

            System.out.println("PUT Response:\n" + body);
        }
    }
```

### DELETE 请求示例

```java
    /**
     * DELETE 请求示例
     */
    @Test
    void testDelete() throws IOException {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpDelete request = new HttpDelete("https://jsonplaceholder.typicode.com/posts/1");

            String body = client.execute(request, response ->
                    "Status code: " + response.getCode()
            );

            System.out.println("DELETE Response:\n" + body);
        }
    }
```

### GET 请求带 Header 和查询参数示例

```java
    /**
     * GET 请求带 Header 和查询参数示例
     */
    @Test
    void testGetWithHeaders() throws IOException {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            // 查询参数可以直接拼接到 URL
            HttpGet request = new HttpGet("https://jsonplaceholder.typicode.com/posts?userId=1");

            // 自定义 Header
            request.addHeader("Accept", "application/json");
            request.addHeader("Custom-Header", "MyValue");

            String body = client.execute(request, response ->
                    EntityUtils.toString(response.getEntity())
            );

            System.out.println("GET with headers Response:\n" + body);
        }
    }
```



## 创建Client

### 连接池

```java
    /**
     * 创建带连接池的 CloseableHttpClient 实例。
     *
     * <p>该客户端支持最大连接数、每路由最大连接数、空闲连接验证及过期/空闲连接自动清理。
     *
     * @return 配置好的 CloseableHttpClient 对象
     */
    public static CloseableHttpClient createPooledClient() {
        PoolingHttpClientConnectionManager connectionManager =
                PoolingHttpClientConnectionManagerBuilder.create()
                        // 设置最大连接总数
                        .setMaxConnTotal(200)
                        // 设置每路由最大连接数
                        .setMaxConnPerRoute(50)
                        // 设置空闲连接验证间隔
                        .setValidateAfterInactivity(TimeValue.ofSeconds(30))
                        .build();

        return HttpClients.custom()
                .setConnectionManager(connectionManager)
                // 清理过期连接
                .evictExpiredConnections()
                // 清理空闲连接
                .evictIdleConnections(TimeValue.ofSeconds(30))
                .build();
    }
```

### 配置请求超时

```java
    /**
     * 创建带连接池的 CloseableHttpClient 实例，并配置请求超时。
     *
     * <p>该客户端支持：
     * - 最大连接数和每路由最大连接数
     * - 空闲连接验证及过期/空闲连接自动清理
     * - 连接超时、响应超时、获取连接超时
     *
     * @return 配置好的 CloseableHttpClient 对象
     */
    public static CloseableHttpClient createPooledClientWithTimeout() {
        PoolingHttpClientConnectionManager connectionManager =
                PoolingHttpClientConnectionManagerBuilder.create()
                        // 设置最大连接总数
                        .setMaxConnTotal(200)
                        // 设置每路由最大连接数
                        .setMaxConnPerRoute(50)
                        // 设置空闲连接验证间隔
                        .setValidateAfterInactivity(TimeValue.ofSeconds(30))
                        .build();

        RequestConfig requestConfig = RequestConfig.custom()
                // 连接建立超时
                .setConnectTimeout(Timeout.ofSeconds(5))
                // 响应超时
                .setResponseTimeout(Timeout.ofSeconds(10))
                // 从连接池获取连接超时
                .setConnectionRequestTimeout(Timeout.ofSeconds(2))
                .build();

        return HttpClients.custom()
                .setConnectionManager(connectionManager)
                // 清理过期连接
                .evictExpiredConnections()
                // 清理空闲连接
                .evictIdleConnections(TimeValue.ofSeconds(30))
                .setDefaultRequestConfig(requestConfig)
                .build();
    }
```

### 请求/响应拦截器

```java
    /**
     * 创建带连接池的 CloseableHttpClient 实例，并添加请求/响应拦截器。
     *
     * <p>该客户端支持：
     * - 最大连接数和每路由最大连接数
     * - 请求和响应拦截器（可用于日志或自定义处理）
     * - 过期连接自动清理
     *
     * @return 配置好的 CloseableHttpClient 对象
     */
    public static CloseableHttpClient createPooledClientWithInterceptor() {
        PoolingHttpClientConnectionManager connectionManager =
                PoolingHttpClientConnectionManagerBuilder.create()
                        // 设置最大连接总数
                        .setMaxConnTotal(200)
                        // 设置每路由最大连接数
                        .setMaxConnPerRoute(50)
                        // 设置空闲连接验证间隔
                        .setValidateAfterInactivity(TimeValue.ofSeconds(30))
                        .build();

        return HttpClients.custom()
                .setConnectionManager(connectionManager)
                // 请求拦截器：打印请求 URI
                .addRequestInterceptorLast((request, entity, context) -> {
                    System.out.println("Request URI: " + request.getRequestUri());
                })
                // 响应拦截器：打印响应状态码
                .addResponseInterceptorLast((response, entity, context) -> {
                    System.out.println("Response Code: " + response.getCode());
                })
                // 清理过期连接
                .evictExpiredConnections()
                // 清理空闲连接
                .evictIdleConnections(TimeValue.ofSeconds(30))
                .build();
    }
```

### 信任所有 SSL 证书

```java
    /**
     * 创建带连接池的 CloseableHttpClient 实例，并信任所有 SSL 证书。
     *
     * <p>该客户端支持：
     * - 最大连接数和每路由最大连接数
     * - 空闲连接验证及过期/空闲连接自动清理
     * - 信任所有 SSL 证书（适用于测试环境或自签名证书）
     *
     * @return 配置好的 CloseableHttpClient 对象
     */
    public static CloseableHttpClient createPooledClientWithTrustAllSSL() {
        try {
            // 创建 SSLContext，信任所有证书
            SSLContext sslContext = SSLContexts.custom()
                    .loadTrustMaterial(null, new TrustAllStrategy())
                    .build();

            // 构建 TLS 策略
            DefaultClientTlsStrategy tlsStrategy = new DefaultClientTlsStrategy(sslContext);

            // 构建连接池管理器
            PoolingHttpClientConnectionManager connectionManager =
                    PoolingHttpClientConnectionManagerBuilder.create()
                            // 设置最大连接总数
                            .setMaxConnTotal(200)
                            // 设置每路由最大连接数
                            .setMaxConnPerRoute(50)
                            // 设置空闲连接验证间隔
                            .setValidateAfterInactivity(TimeValue.ofSeconds(30))
                            // 设置 TLS 策略
                            .setTlsSocketStrategy(tlsStrategy)
                            .build();

            // 构建 HttpClient 实例
            return HttpClients.custom()
                    // 设置连接池管理器
                    .setConnectionManager(connectionManager)
                    // 清理过期连接
                    .evictExpiredConnections()
                    // 清理空闲连接
                    .evictIdleConnections(TimeValue.ofSeconds(30))
                    .build();

        } catch (Exception e) {
            throw new RuntimeException("Failed to create pooled HttpClient", e);
        }
    }
```

### 请求重试策略

```java
    /**
     * 创建带连接池的 CloseableHttpClient 实例，并配置请求重试策略。
     *
     * <p>该客户端支持：
     * - 最大连接数和每路由最大连接数
     * - 空闲连接验证及过期/空闲连接自动清理
     * - 请求异常和 5xx 响应状态码的重试，最多重试 3 次
     * - 重试间隔 2 秒
     *
     * @return 配置好的 CloseableHttpClient 对象
     */
    public static CloseableHttpClient createPooledClientWithRetry() {
        // 构建连接池管理器
        PoolingHttpClientConnectionManager connectionManager =
                PoolingHttpClientConnectionManagerBuilder.create()
                        // 设置最大连接总数
                        .setMaxConnTotal(200)
                        // 设置每路由最大连接数
                        .setMaxConnPerRoute(50)
                        // 设置空闲连接验证间隔
                        .setValidateAfterInactivity(TimeValue.ofSeconds(30))
                        .build();

        // 自定义重试策略
        HttpRequestRetryStrategy retryStrategy = new HttpRequestRetryStrategy() {

            @Override
            public boolean retryRequest(HttpRequest request, IOException exception, int execCount, HttpContext context) {
                // 超过最大重试次数则不再重试
                if (execCount > 3) {
                    return false;
                }
                // 针对连接超时、连接中断等可恢复异常进行重试
                String name = exception.getClass().getSimpleName();
                return name.contains("Timeout") || name.contains("Unknown") || name.contains("Connection") || name.contains("Socket");
            }

            @Override
            public boolean retryRequest(HttpResponse response, int execCount, HttpContext context) {
                if (execCount > 3) {
                    return false;
                }
                int status = response.getCode();
                // 仅对 5xx（服务器内部错误）进行重试
                return status >= 500 && status < 600;
            }

            @Override
            public TimeValue getRetryInterval(HttpRequest request, IOException exception, int execCount, HttpContext context) {
                return TimeValue.ofSeconds(2);
            }

            @Override
            public TimeValue getRetryInterval(HttpResponse response, int execCount, HttpContext context) {
                return TimeValue.ofSeconds(2);
            }
        };

        // 构建 HttpClient 实例
        return HttpClients.custom()
                .setConnectionManager(connectionManager)
                // 清理过期连接
                .evictExpiredConnections()
                // 清理空闲连接
                .evictIdleConnections(TimeValue.ofSeconds(30))
                // 设置重试策略
                .setRetryStrategy(retryStrategy)
                .build();
    }
```

