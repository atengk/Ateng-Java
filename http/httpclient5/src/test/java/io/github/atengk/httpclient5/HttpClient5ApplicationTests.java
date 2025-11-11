package io.github.atengk.httpclient5;

import org.apache.hc.client5.http.HttpRequestRetryStrategy;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.apache.hc.client5.http.ssl.TrustAllStrategy;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLContext;
import java.io.IOException;

class HttpClient5ApplicationTests {
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
                    "Status code: " + response.getCode()
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
    void test1() {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet("https://httpbin.org/get");
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                System.out.println(response.getCode()); // HTTP 状态码
                System.out.println(EntityUtils.toString(response.getEntity()));
            }
        } catch (IOException | ParseException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void test2() {
        HttpGet get = new HttpGet("https://self-signed.badssl.com/");
        try (CloseableHttpResponse response = createPooledClientWithTrustAllSSL().execute(get)) {
            System.out.println("Response Code: " + response.getCode());
            System.out.println(EntityUtils.toString(response.getEntity()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void test3() throws IOException, ParseException {
        HttpGet request = new HttpGet("https://ateng.local/get");
        CloseableHttpResponse response = createPooledClientWithRetry().execute(request);
        System.out.println(response.getCode()); // HTTP 状态码
        System.out.println(EntityUtils.toString(response.getEntity()));
    }

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


}
