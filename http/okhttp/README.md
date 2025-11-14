# OkHttp 

**OkHttp** 是一款由 Square 开发的高效 Java HTTP 客户端库，支持同步与异步请求、连接池复用、HTTP/2、多路复用及透明 GZIP 压缩。它轻量、性能高，适合在 Java 和 Android 项目中进行 RESTful 接口调用。

OkHttp 提供丰富的拦截器机制，可统一处理请求头、日志、鉴权或重试策略，并可灵活配置连接池、超时和 SSL 安全策略。最新版本 5.x 基于 Java 8+，对现代 HTTP 特性支持更完善。

官网地址：[链接](https://github.com/square/okhttp)



## 添加依赖

```xml
<properties>
    <spring-boot.version>2.7.18</spring-boot.version>
</properties>
```

```xml
<!-- OkHttp HTTP 客户端库-->
<dependency>
    <groupId>com.squareup.okhttp3</groupId>
    <artifactId>okhttp</artifactId>
</dependency>
```

或者加上版本（Springboot管理了版本，一般情况下不用配置）

```xml
<!-- OkHttp HTTP 客户端库-->
<dependency>
    <groupId>com.squareup.okhttp3</groupId>
    <artifactId>okhttp</artifactId>
    <version>4.12.0</version>
</dependency>
```



## 使用

### 创建实例

```java
    /**
     * 创建 OkHttpClient 实例，可复用
     */
    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build();
```

### GET 请求示例

```java
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
```

### POST 请求示例（JSON body）

```java
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
```

### PUT 请求示例（更新资源）

```java
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
```

### DELETE 请求示例

```java
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
```

### GET 请求带 Header 和查询参数示例

```java
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
```



## 创建Client

### 连接池

```java
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
```

### 信任所有 SSL 证书

```java
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
```

