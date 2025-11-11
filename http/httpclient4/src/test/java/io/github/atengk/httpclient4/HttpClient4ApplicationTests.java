package io.github.atengk.httpclient4;

import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.ServiceUnavailableRetryStrategy;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.*;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

class HttpClient4ApplicationTests {
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

    @Test
    void test2() throws IOException {
        HttpGet get = new HttpGet("https://self-signed.badssl.com/");
        try (CloseableHttpResponse response = createPooledClientWithTrustAllSSL().execute(get)) {
            System.out.println("Response Code: " + response.getStatusLine().getStatusCode());
            System.out.println(EntityUtils.toString(response.getEntity()));
        }
    }

    @Test
    void test3() throws IOException {
        HttpGet get = new HttpGet("https://ateng.local/get/");
        try (CloseableHttpResponse response = createPooledClientWithRetry().execute(get)) {
            System.out.println("Response Code: " + response.getStatusLine().getStatusCode());
            System.out.println(EntityUtils.toString(response.getEntity()));
        }
    }

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

}
