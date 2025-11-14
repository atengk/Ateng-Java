# Apache HttpClient 4

Apache HttpClient 4 是广泛使用的 Java HTTP 客户端库，提供了稳定的同步请求能力和丰富的配置选项。它支持连接池、请求重试、代理设置、认证机制和 Cookie 管理，适合大多数传统 Web 服务调用场景。

HttpClient 4 的设计偏向面向对象与易用性，API 清晰、扩展性强，是许多框架（如 Spring RestTemplate）的底层实现基础，但相比 HttpClient 5，它在性能优化与 HTTP/2 支持上略显不足。



## 添加依赖

```xml
<properties>
    <spring-boot.version>2.7.18</spring-boot.version>
</properties>
```

```xml
<!-- Apache HttpClient 4 HTTP 客户端库 -->
<dependency>
    <groupId>org.apache.httpcomponents</groupId>
    <artifactId>httpclient</artifactId>
</dependency>
```

或者加上版本（Springboot管理了版本，一般情况下不用配置）

```xml
<!-- Apache HttpClient 4 HTTP 客户端库 -->
<dependency>
    <groupId>org.apache.httpcomponents</groupId>
    <artifactId>httpclient</artifactId>
    <version>4.5.14</version>
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
                    "Status code: " + response.getStatusLine().getStatusCode()
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
        // 创建连接池管理器
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();

        // 设置最大连接数
        connectionManager.setMaxTotal(200);
        // 设置每个路由的最大连接数
        connectionManager.setDefaultMaxPerRoute(50);
        // 设置空闲连接验证间隔
        connectionManager.setValidateAfterInactivity(30_000);

        return HttpClients.custom()
                .setConnectionManager(connectionManager)
                // 清理过期连接
                .evictExpiredConnections()
                // 清理空闲连接
                .evictIdleConnections(30, TimeUnit.SECONDS)
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
        // 创建连接池管理器
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();

        // 设置最大连接总数
        connectionManager.setMaxTotal(200);
        // 设置每路由最大连接数
        connectionManager.setDefaultMaxPerRoute(50);
        // 设置空闲连接验证间隔
        connectionManager.setValidateAfterInactivity(30_000);

        // 配置请求超时参数
        RequestConfig requestConfig = RequestConfig.custom()
                // 连接建立超时（单位：毫秒）
                .setConnectTimeout(5_000)
                // 从连接池获取连接超时（单位：毫秒）
                .setConnectionRequestTimeout(2_000)
                // 响应超时（单位：毫秒）
                .setSocketTimeout(10_000)
                .build();

        // 构建 HttpClient 实例
        return HttpClients.custom()
                .setConnectionManager(connectionManager)
                // 清理过期连接
                .evictExpiredConnections()
                // 清理空闲连接
                .evictIdleConnections(30, TimeUnit.SECONDS)
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
        // 创建连接池管理器
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();

        // 设置最大连接总数
        connectionManager.setMaxTotal(200);
        // 设置每路由最大连接数
        connectionManager.setDefaultMaxPerRoute(50);
        // 设置空闲连接验证间隔
        connectionManager.setValidateAfterInactivity(30_000);

        // 构建 HttpClient 实例
        return HttpClients.custom()
                .setConnectionManager(connectionManager)
                // 请求拦截器：打印请求 URI
                .addInterceptorLast((HttpRequestInterceptor) (request, context) -> {
                    System.out.println("Request URI: " + request.getRequestLine().getUri());
                })
                // 响应拦截器：打印响应状态码
                .addInterceptorLast((HttpResponseInterceptor) (response, context) -> {
                    System.out.println("Response Code: " + response.getStatusLine().getStatusCode());
                })
                // 清理过期连接
                .evictExpiredConnections()
                // 清理空闲连接
                .evictIdleConnections(30, TimeUnit.SECONDS)
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
            SSLContext sslContext = new SSLContextBuilder()
                    .loadTrustMaterial(null, (certificate, authType) -> true)
                    .build();

            // 创建允许所有主机名的验证器
            SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(
                    sslContext,
                    NoopHostnameVerifier.INSTANCE
            );

            // 注册支持 HTTP 和 HTTPS 的 socket 工厂
            Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                    .register("http", PlainConnectionSocketFactory.getSocketFactory())
                    .register("https", sslSocketFactory)
                    .build();

            // 创建连接池管理器
            PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);

            // 设置最大连接总数
            connectionManager.setMaxTotal(200);
            // 设置每路由最大连接数
            connectionManager.setDefaultMaxPerRoute(50);
            // 设置空闲连接验证间隔
            connectionManager.setValidateAfterInactivity(30_000);

            // 构建 HttpClient 实例
            return HttpClients.custom()
                    .setConnectionManager(connectionManager)
                    // 清理过期连接
                    .evictExpiredConnections()
                    // 清理空闲连接
                    .evictIdleConnections(30, TimeUnit.SECONDS)
                    // 设置 SSL 工厂
                    .setSSLSocketFactory(sslSocketFactory)
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
        // 创建连接池管理器
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();

        // 设置最大连接总数
        connectionManager.setMaxTotal(200);
        // 设置每路由最大连接数
        connectionManager.setDefaultMaxPerRoute(50);
        // 设置空闲连接验证间隔
        connectionManager.setValidateAfterInactivity(30_000);

        // 自定义重试处理器
        HttpRequestRetryHandler retryHandler = (exception, executionCount, context) -> {
            // 超过最大重试次数则不再重试
            if (executionCount > 3) {
                return false;
            }
            // 针对连接超时、连接中断等可恢复异常进行重试
            String name = exception.getClass().getSimpleName();
            return name.contains("Timeout") || name.contains("Unknown") || name.contains("Connection") || name.contains("Socket");
        };

        // 自定义服务端 5xx 响应重试策略
        ServiceUnavailableRetryStrategy serviceRetryStrategy = new ServiceUnavailableRetryStrategy() {
            @Override
            public boolean retryRequest(HttpResponse response, int executionCount, HttpContext context) {
                if (executionCount > 3) {
                    return false;
                }
                int status = response.getStatusLine().getStatusCode();
                return status >= 500 && status < 600;
            }

            @Override
            public long getRetryInterval() {
                return 2_000; // 重试间隔 2 秒
            }
        };

        // 构建 HttpClient 实例
        return HttpClients.custom()
                .setConnectionManager(connectionManager)
                // 清理过期连接
                .evictExpiredConnections()
                // 清理空闲连接
                .evictIdleConnections(30, TimeUnit.SECONDS)
                // 设置请求异常重试
                .setRetryHandler(retryHandler)
                // 设置服务端 5xx 响应重试
                .setServiceUnavailableRetryStrategy(serviceRetryStrategy)
                .build();
    }
```

