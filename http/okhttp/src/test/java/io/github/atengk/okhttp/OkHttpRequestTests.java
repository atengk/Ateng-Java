package io.github.atengk.okhttp;

import okhttp3.*;
import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.concurrent.TimeUnit;

public class OkHttpRequestTests {

    /**
     * 创建 OkHttpClient 实例，可复用
     */
    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build();

    // GET 请求示例
    @Test
    void testGet() throws IOException {
        Request request = new Request.Builder()
                .url("https://httpbin.org/get")
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            System.out.println("GET 返回:\n" + response.body().string());
        }
    }

    // POST 请求示例（JSON body）
    @Test
    void testPostJson() throws IOException {
        MediaType JSON = MediaType.get("application/json; charset=utf-8");
        String jsonString = "{\"username\":\"tony\",\"password\":\"123456\"}";

        RequestBody body = RequestBody.create(jsonString, JSON);

        Request request = new Request.Builder()
                .url("https://httpbin.org/post")
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            System.out.println("POST JSON 返回:\n" + response.body().string());
        }
    }

    // PUT 请求示例（更新资源）
    @Test
    void testPut() throws IOException {
        MediaType JSON = MediaType.get("application/json; charset=utf-8");
        String jsonString = "{\"id\":1,\"username\":\"tony_updated\"}";

        RequestBody body = RequestBody.create(jsonString, JSON);

        Request request = new Request.Builder()
                .url("https://httpbin.org/put")
                .put(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            System.out.println("PUT 返回:\n" + response.body().string());
        }
    }

    // DELETE 请求示例
    @Test
    void testDelete() throws IOException {
        Request request = new Request.Builder()
                .url("https://httpbin.org/delete")
                .delete()
                .build();

        try (Response response = client.newCall(request).execute()) {
            System.out.println("DELETE 返回:\n" + response.body().string());
        }
    }

    // GET 请求带 Header 和查询参数示例
    @Test
    void testGetWithHeadersAndParams() throws IOException {
        HttpUrl url = HttpUrl.parse("https://httpbin.org/get")
                .newBuilder()
                .addQueryParameter("param1", "value1")
                .addQueryParameter("param2", "value2")
                .build();

        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer test_token")
                .header("User-Agent", "OkHttp-Test")
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            System.out.println("GET 带 Header & 参数 返回:\n" + response.body().string());
        }
    }

    /**
     * 创建可复用的 OkHttpClient 实例
     * 使用连接池、超时设置和自动重试
     *
     * @return OkHttpClient 实例
     */
    public static OkHttpClient createHttpClient() {
        // 创建连接池：最大 10 个空闲连接，保持 5 分钟
        ConnectionPool connectionPool = new ConnectionPool(
                // 最大空闲连接数
                20,
                // 空闲连接存活时间
                5,
                // 时间单位
                TimeUnit.MINUTES
        );

        // 创建 OkHttpClient 并配置连接池
        return new OkHttpClient.Builder()
                // 使用自定义连接池
                .connectionPool(connectionPool)
                // 连接超时
                .connectTimeout(10, TimeUnit.SECONDS)
                // 读取超时
                .readTimeout(30, TimeUnit.SECONDS)
                // 写入超时
                .writeTimeout(30, TimeUnit.SECONDS)
                // 失败自动重试
                .retryOnConnectionFailure(true)
                .build();
    }

    /**
     * 创建可复用的 OkHttpClient 实例
     * 信任所有 SSL 证书（适用于测试环境或自签名证书）
     *
     * @return OkHttpClient 实例
     */
    public static OkHttpClient createHttpClientWithTrustAllSSL() throws NoSuchAlgorithmException, KeyManagementException {
        // 信任所有证书 TrustManager
        TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                    }

                    @Override
                    public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                    }

                    @Override
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return new java.security.cert.X509Certificate[]{};
                    }
                }
        };
        // SSLContext 初始化
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
        SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
        // 创建连接池：最大 10 个空闲连接，保持 5 分钟
        ConnectionPool connectionPool = new ConnectionPool(
                // 最大空闲连接数
                20,
                // 空闲连接存活时间
                5,
                // 时间单位
                TimeUnit.MINUTES
        );

        // 创建 OkHttpClient 并配置连接池
        return new OkHttpClient.Builder()
                .sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0])
                // 信任所有主机名
                .hostnameVerifier((hostname, session) -> true)
                // 使用自定义连接池
                .connectionPool(connectionPool)
                // 连接超时
                .connectTimeout(10, TimeUnit.SECONDS)
                // 读取超时
                .readTimeout(30, TimeUnit.SECONDS)
                // 写入超时
                .writeTimeout(30, TimeUnit.SECONDS)
                // 失败自动重试
                .retryOnConnectionFailure(true)
                .build();
    }

    @Test
    void test2() {
        Request request = new Request.Builder()
                .url("https://self-signed.badssl.com/")
                .get()
                .build();

        try (Response response = createHttpClientWithTrustAllSSL().newCall(request).execute()) {
            System.out.println("GET 返回:\n" + response.body().string());
        } catch (KeyManagementException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }


}
