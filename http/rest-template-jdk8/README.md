# RestTemplate

RestTemplate是一个用于发送HTTP请求并获取响应的客户端工具



## 基础配置

### 配置 pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns="http://maven.apache.org/POM/4.0.0"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <!-- 项目模型版本 -->
    <modelVersion>4.0.0</modelVersion>

    <!-- 项目坐标 -->
    <groupId>local.ateng.java</groupId>
    <artifactId>rest-template-jdk8</artifactId>
    <version>v1.0</version>
    <name>rest-template-jdk8</name>
    <description>RestTemplate是一个用于发送HTTP请求并获取响应的客户端工具</description>

    <!-- 项目属性 -->
    <properties>
        <java.version>8</java.version>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>
        <spring-boot.version>2.7.18</spring-boot.version>
        <maven-compiler.version>3.12.1</maven-compiler.version>
        <lombok.version>1.18.36</lombok.version>
        <fastjson2.version>2.0.53</fastjson2.version>
    </properties>

    <!-- 项目依赖 -->
    <dependencies>
        <!-- Spring Boot Web Starter: 包含用于构建Web应用程序的Spring Boot依赖项 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Spring Boot Starter Test: 包含用于测试Spring Boot应用程序的依赖项 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>

        <!-- Lombok: 简化Java代码编写的依赖项 -->
        <!-- https://mvnrepository.com/artifact/org.projectlombok/lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <version>${lombok.version}</version>
            <scope>provided</scope>
        </dependency>

        <!-- 高性能的JSON库 -->
        <!-- https://github.com/alibaba/fastjson2/wiki/fastjson2_intro_cn#0-fastjson-20%E4%BB%8B%E7%BB%8D -->
        <dependency>
            <groupId>com.alibaba.fastjson2</groupId>
            <artifactId>fastjson2</artifactId>
            <version>${fastjson2.version}</version>
        </dependency>

        <!-- Apache HttpComponents客户端 -->
        <dependency>
            <groupId>org.apache.httpcomponents</groupId>
            <artifactId>httpclient</artifactId>
        </dependency>

    </dependencies>

    <!-- Spring Boot 依赖管理 -->
    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-dependencies</artifactId>
                <version>${spring-boot.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <!-- 插件仓库配置 -->
    <repositories>
        <!-- Central Repository -->
        <repository>
            <id>central</id>
            <name>阿里云中央仓库</name>
            <url>https://maven.aliyun.com/repository/central</url>
            <!--<name>Maven官方中央仓库</name>
            <url>https://repo.maven.apache.org/maven2/</url>-->
        </repository>
    </repositories>

    <!-- 构建配置 -->
    <build>
        <finalName>${project.name}-${project.version}</finalName>
        <plugins>
            <!-- Maven 编译插件 -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>${maven-compiler.version}</version>
                <configuration>
                    <source>${java.version}</source>
                    <target>${java.version}</target>
                    <encoding>${project.build.sourceEncoding}</encoding>
                    <!-- 编译参数 -->
                    <compilerArgs>
                        <!-- 启用Java 8参数名称保留功能 -->
                        <arg>-parameters</arg>
                    </compilerArgs>
                </configuration>
            </plugin>

            <!-- Spring Boot Maven 插件 -->
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <version>${spring-boot.version}</version>
                <executions>
                    <execution>
                        <id>repackage</id>
                        <goals>
                            <goal>repackage</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
        <resources>
            <!-- 第一个资源配置块 -->
            <resource>
                <directory>src/main/resources</directory>
                <filtering>false</filtering>
            </resource>
            <!-- 第二个资源配置块 -->
            <resource>
                <directory>src/main/resources</directory>
                <includes>
                    <include>application*</include>
                    <include>bootstrap*.yml</include>
                    <include>common*</include>
                    <include>banner*</include>
                </includes>
                <filtering>true</filtering>
            </resource>
        </resources>
    </build>

</project>
```

### 编辑配置文件

```java
package local.ateng.java.rest.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplate配置文件
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-02-02
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

}
```



## 使用RestTemplate

### 创建测试类

```java
@SpringBootTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class RestTemplateApplicationTests {
    private final RestTemplate restTemplate;
}
```



### GET请求

getForObject方法，获取响应体，将其转换为第二个参数指定的类型

```java
    @Test
    void getForObject() {
        //getForObject方法，获取响应体，将其转换为第二个参数指定的类型
        JSONObject jsonObject = restTemplate.getForObject("https://www.wanandroid.com/article/list/0/json", JSONObject.class);
        String string = JSON.toJSONString(jsonObject, JSONWriter.Feature.PrettyFormat);
        System.out.println(string);
    }
```

getForEntity方法，返回值为ResponseEntity类型

```java
    @Test
    void getForEntity() {
        //getForEntity方法，返回值为ResponseEntity类型
        // ResponseEntity中包含了响应结果中的所有信息，比如头、状态、body
        ResponseEntity<JSONObject> response = restTemplate.getForEntity("https://www.wanandroid.com/article/list/0/json", JSONObject.class);
        // 获取状态对象
        HttpStatusCode httpStatusCode = response.getStatusCode();
        System.out.println("StatusCode=" + httpStatusCode);
        // 获取状态码
        int statusCodeValue = response.getStatusCodeValue();
        System.out.println("StatusCodeValue=" + statusCodeValue);
        // 获取headers
        HttpHeaders httpHeaders = response.getHeaders();
        System.out.println("Headers=" + httpHeaders);
        // 获取body
        JSONObject body = response.getBody();
        System.out.println(body);
    }
```

### GET请求参数

url中有动态参数

```java
    @Test
    void getForObject2() {
        //url中有动态参数
        String url = "https://www.wanandroid.com/article/list/{id}/json";
        HashMap<String, String> map = new HashMap<>();
        map.put("id", "3");
        JSONObject jsonObject = restTemplate.getForObject(url, JSONObject.class, map);
        System.out.println(jsonObject);
    }
```

查询参数

```java
    @Test
    void getForObjectWithQueryParams() {
        // url中有动态路径参数
        String url = "https://www.wanandroid.com/article/list/{id}/json";

        // 设置路径参数
        HashMap<String, String> pathParams = new HashMap<>();
        pathParams.put("id", "3");

        // 使用UriComponentsBuilder添加查询参数
        String finalUrl = UriComponentsBuilder.fromHttpUrl(url)
                .queryParam("page", 1)
                .queryParam("limit", 10)
                .encode() // 强制使用UTF-8编码
                .toUriString();
        System.out.println(finalUrl);

        // 发起请求并获取响应体
        JSONObject jsonObject = restTemplate.getForObject(finalUrl, JSONObject.class, pathParams);
        System.out.println(jsonObject);
    }
```

### 请求头

```java
    @Test
    void getForObject3() {
        // get请求，带请求头的方式，使用exchange
        String url = "https://www.wanandroid.com/article/list/{id}/json";
        // 请求头
        HttpHeaders headers = new HttpHeaders();
        headers.add("header-1", "V1");
        headers.add("header-2", "Spring");
        headers.add("header-3", "SpringBoot");
        // url的参数
        HashMap<String, String> uriVariables = new HashMap<>();
        uriVariables.put("id", "3");
        // HttpEntity：HTTP实体，内部包含了请求头和请求体
        HttpEntity requestEntity = new HttpEntity(
                null,//body部分，get请求没有body，所以为null
                headers //头
        );
        // 使用exchange发送请求
        ResponseEntity<JSONObject> responseEntity = restTemplate.exchange(
                url, //url
                HttpMethod.GET, //请求方式
                requestEntity, //请求实体（头、body）
                JSONObject.class,//返回的结果类型
                uriVariables //url中的占位符对应的值
        );
        System.out.println(responseEntity.getStatusCodeValue());
        System.out.println(responseEntity.getHeaders());
        System.out.println(responseEntity.getBody());
    }
```

### 表单请求

发送form-data表单post请求

```java
    @Test
    void postForObject() {
        // 发送form-data表单post请求
        String url = "https://api.apiopen.top/api/login";
        //①：表单信息，需要放在MultiValueMap中，MultiValueMap相当于Map<String,List<String>>
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        //调用add方法填充表单数据(表单名称:值)
        body.add("account", "309324904@qq.com");
        body.add("password", "123456");
        //②：发送请求(url,请求体，返回值需要转换的类型)
        JSONObject jsonObject = restTemplate.postForObject(url, body, JSONObject.class);
        System.out.println(jsonObject);
    }
```

### POST请求

使用postForObject

```java
    @Test
    public void postRequestUsingPostForObject() {
        String url = "https://jsonplaceholder.typicode.com/posts";

        // 创建请求体
        String requestBody = "{\"title\":\"foo\",\"body\":\"bar\",\"userId\":1}";

        // 使用 postForObject 发送 POST 请求并返回响应体
        String response = restTemplate.postForObject(url, requestBody, String.class);

        // 打印响应
        System.out.println(response);
    }
```

使用exchange

```java
    @Test
    public void postRequestUsingExchange() {
        String url = "https://jsonplaceholder.typicode.com/posts";

        // 构建请求体
        String requestBody = "{\"title\":\"foo\",\"body\":\"bar\",\"userId\":1}";

        // 创建HttpHeaders并设置Content-Type
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 创建HttpEntity，将请求体和请求头封装在一起
        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

        // 使用 exchange 发送 POST 请求
        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.POST,  // 请求方法为 POST
                entity,           // 请求体和头
                String.class      // 响应类型
        );

        // 打印响应
        System.out.println(response.getBody());
    }
```



## 使用拦截器

### 创建拦截器

在请求之行前回之行的操作

```java
package local.ateng.java.rest.interceptor;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class MyRequestInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                        ClientHttpRequestExecution execution) throws IOException {
        // 在请求之前执行的操作，比如添加请求头、日志记录等
        System.out.println("请求 URL: " + request.getURI());
        System.out.println("请求方法: " + request.getMethod());

        // 继续执行请求
        return execution.execute(request, body);
    }
}
```

### 创建RestTemplate并注册拦截器

```java
package local.ateng.java.rest.config;

import local.ateng.java.rest.interceptor.MyRequestInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * RestTemplate配置文件
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-02-02
 */
@Configuration
@RequiredArgsConstructor
public class RestTemplateConfig {
    private final MyRequestInterceptor myRequestInterceptor;

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public RestTemplate myRestTemplate() {
        // 创建 RestTemplate
        RestTemplate restTemplate = new RestTemplate();

        // 创建并注册拦截器
        List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>();
        interceptors.add(myRequestInterceptor);

        restTemplate.setInterceptors(interceptors);
        return restTemplate;
    }

}
```

### 使用拦截器

```java
@SpringBootTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class HTTPRestTemplateJdk8Tests {
    private final RestTemplate restTemplate;
    private final RestTemplate myRestTemplate;

    @Test
    void getForObjectOther() {
        //getForObject方法，获取响应体，将其转换为第二个参数指定的类型
        JSONObject jsonObject = myRestTemplate.getForObject("https://www.wanandroid.com/article/list/0/json", JSONObject.class);
        String string = JSON.toJSONString(jsonObject, JSONWriter.Feature.PrettyFormat);
        System.out.println(string);
    }
}
```

输出：

```
请求 URL: https://www.wanandroid.com/article/list/0/json
请求方法: GET
{
	"data":{
...
```



### AuthInterceptor

```java
package local.ateng.java.rest.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * RestTemplate 拦截器：用于添加统一的认证 Token（如 JWT）到请求头中。
 * <p>
 * 注意：此实现为模拟 Token 获取逻辑，实际生产中应接入真实的缓存（如 Redis）或配置中心。
 *
 * @author 孔余
 * @since 2025-07-30
 */
@Component
@Slf4j
public class AuthInterceptor implements ClientHttpRequestInterceptor {

    /**
     * 拦截请求，添加 Authorization 头。
     *
     * @param request   原始请求
     * @param body      请求体字节数组
     * @param execution 请求执行器（用于继续调用链）
     * @return 响应结果
     * @throws IOException IO 异常
     */
    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {

        // 获取 Token（可替换为从 Redis、配置中心或上下文中获取）
        String token = getToken();

        // 日志记录请求 URI 和添加 token 的行为（仅开发调试阶段启用）
        log.debug("Adding Authorization token to request: {}", request.getURI());

        // 若 token 为空，可选择记录日志或抛出异常（视业务场景而定）
        if (token == null || token.isEmpty()) {
            log.warn("Authorization token is missing.");
            // 你也可以选择抛出自定义异常
            // throw new IllegalStateException("Missing authorization token");
        }

        // 设置请求头
        HttpHeaders headers = request.getHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + token);

        // 执行请求
        return execution.execute(request, body);
    }

    /**
     * 获取 Token 的方法（可扩展为从缓存或配置服务获取）
     *
     * @return JWT Token 字符串
     */
    private String getToken() {
        // 模拟：返回一个硬编码 Token（请替换为实际获取逻辑）
        return "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...";
    }
}

```

### LoggingInterceptor

```java
package local.ateng.java.rest.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;


/**
 * RestTemplate 拦截器：记录请求和响应的详细日志
 *
 * @author 孔余
 * @since 2025-07-30
 */
@Component
@Slf4j
public class LoggingInterceptor implements ClientHttpRequestInterceptor {

    /**
     * 拦截请求并打印日志
     *
     * @param request   当前请求
     * @param body      请求体
     * @param execution 拦截器执行器（用于传递调用链）
     * @return 响应结果
     * @throws IOException IO 异常
     */
    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {

        long startTime = System.currentTimeMillis();

        log.info("====== 请求开始 ======");
        log.info("请求地址: {}", request.getURI());
        log.info("请求方式: {}", request.getMethod());
        log.info("请求头: {}", request.getHeaders());
        // 记录请求体，仅限于有请求体的情况（如 POST、PUT）
        if (body != null && body.length > 0) {
            log.info("请求体: {}", new String(body, StandardCharsets.UTF_8));
        } else {
            log.info("请求体: 无");
        }
        ClientHttpResponse response;
        try {
            // 执行请求
            response = execution.execute(request, body);
        } catch (IOException e) {
            log.error("请求执行异常: {}", e.getMessage(), e);
            throw e;
        }

        // 包装响应体，避免响应流只能读取一次的问题
        ClientHttpResponse wrappedResponse = new BufferingClientHttpResponseWrapper(response);
        String responseBody = StreamUtils.copyToString(wrappedResponse.getBody(), StandardCharsets.UTF_8);

        log.info("响应状态: {}", wrappedResponse.getStatusCode());
        log.info("响应头: {}", wrappedResponse.getHeaders());
        log.info("响应体: {}", responseBody);
        log.info("耗时: {} ms", System.currentTimeMillis() - startTime);
        log.info("====== 请求结束 ======");

        return wrappedResponse;
    }

    /**
     * 响应包装类，用于缓存响应体以便多次读取
     */
    private static class BufferingClientHttpResponseWrapper implements ClientHttpResponse {

        private final ClientHttpResponse response;
        private byte[] body;

        public BufferingClientHttpResponseWrapper(ClientHttpResponse response) throws IOException {
            this.response = response;
            this.body = StreamUtils.copyToByteArray(response.getBody());
        }

        @Override
        public org.springframework.http.HttpStatus getStatusCode() throws IOException {
            return response.getStatusCode();
        }

        @Override
        public int getRawStatusCode() throws IOException {
            return response.getRawStatusCode();
        }

        @Override
        public String getStatusText() throws IOException {
            return response.getStatusText();
        }

        @Override
        public void close() {
            response.close();
        }

        @Override
        public org.springframework.http.HttpHeaders getHeaders() {
            return response.getHeaders();
        }

        @Override
        public java.io.InputStream getBody() {
            return new ByteArrayInputStream(body);
        }
    }
}

```

### RetryInterceptor

```java
package local.ateng.java.rest.interceptor;


import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * RestTemplate 拦截器：用于在请求失败时自动进行重试，并支持指数退避策略。
 *
 * <p>支持以下两类失败情况自动重试：
 * <ul>
 *     <li>网络异常（如连接超时、读取超时等）</li>
 *     <li>服务端错误（状态码 5xx）</li>
 * </ul>
 *
 * <p>支持通过静态方法 {@link #setMaxRetries(int)} 动态设置最大重试次数，具备线程安全性。
 * 默认采用指数退避策略，避免高并发下请求雪崩。
 *
 * @author 孔余
 * @since 2025-07-30
 */
@Component
@Slf4j
public class RetryInterceptor implements ClientHttpRequestInterceptor {

    /**
     * 最大重试次数（支持线程安全修改）
     * 默认为 3 次
     */
    private static final AtomicInteger MAX_RETRIES = new AtomicInteger(3);

    /**
     * 初始重试等待时间（单位：毫秒），每次重试会指数增长
     */
    private static final long INITIAL_INTERVAL_MS = 300;

    /**
     * 最大重试等待时间（单位：毫秒），用于限制指数退避的上限
     */
    private static final long MAX_INTERVAL_MS = 5000;

    /**
     * 设置最大重试次数（必须大于 0）
     *
     * @param retries 新的最大重试次数
     */
    public static void setMaxRetries(int retries) {
        if (retries > 0) {
            MAX_RETRIES.set(retries);
        } else {
            log.warn("设置的最大重试次数无效：{}", retries);
        }
    }

    /**
     * 拦截请求并执行重试逻辑
     *
     * @param request   当前请求对象
     * @param body      请求体内容字节数组
     * @param execution 请求执行器，用于继续调用链
     * @return 请求响应
     * @throws IOException 当超过最大重试次数后仍然失败，则抛出异常
     */
    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {

        int attempt = 0;

        while (true) {
            try {
                // 正常执行请求
                return execution.execute(request, body);

            } catch (HttpStatusCodeException e) {
                // 处理服务端错误（5xx）重试
                if (shouldRetry(e.getStatusCode().value()) && attempt < MAX_RETRIES.get()) {
                    attempt++;
                    long waitTime = calculateBackoffTime(attempt);
                    log.warn("请求失败（状态码：{}），第 {} 次重试：{}，等待 {} 毫秒",
                            e.getStatusCode().value(), attempt, request.getURI(), waitTime);
                    sleep(waitTime);
                } else {
                    log.error("请求失败，状态码：{}，不再重试：{}", e.getStatusCode(), request.getURI());
                    throw e;
                }

            } catch (IOException e) {
                // 网络异常重试
                if (attempt < MAX_RETRIES.get()) {
                    attempt++;
                    long waitTime = calculateBackoffTime(attempt);
                    log.warn("网络异常，第 {} 次重试：{}，等待 {} 毫秒，异常信息：{}",
                            attempt, request.getURI(), waitTime, e.getMessage());
                    sleep(waitTime);
                } else {
                    log.error("网络异常，重试结束：{}，异常信息：{}", request.getURI(), e.getMessage());
                    throw e;
                }
            }
        }
    }

    /**
     * 判断当前状态码是否应当进行重试
     *
     * @param statusCode HTTP 状态码
     * @return 是否应当重试
     */
    private boolean shouldRetry(int statusCode) {
        // 默认只重试服务端错误（5xx）
        return statusCode >= 500 && statusCode < 600;
    }

    /**
     * 根据当前重试次数计算指数退避时间，并限制最大等待时间
     *
     * @param attempt 当前重试次数（从 1 开始）
     * @return 等待时间（单位：毫秒）
     */
    private long calculateBackoffTime(int attempt) {
        long waitTime = (long) (INITIAL_INTERVAL_MS * Math.pow(2, attempt - 1));
        return Math.min(waitTime, MAX_INTERVAL_MS);
    }

    /**
     * 安全执行线程等待，如果被中断则恢复线程中断状态
     *
     * @param millis 等待时间（单位：毫秒）
     */
    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}


```





## 配置连接池

### 添加依赖

```xml
<!-- Apache HttpComponents客户端 -->
<dependency>
    <groupId>org.apache.httpcomponents</groupId>
    <artifactId>httpclient</artifactId>
</dependency>
```

### 创建RestTemplate

```java
/**
 * 构建支持连接池的 RestTemplate Bean。
 * 设置连接超时、读取超时等参数，适合生产环境使用。
 *
 * @return 配置后的 RestTemplate 实例
 */
@Bean
public RestTemplate restTemplate() {
    // 创建底层 HTTP 请求工厂
    HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();

    // 设置连接超时时间（单位：毫秒），连接建立时限
    factory.setConnectTimeout(5000);

    // 设置读取超时时间（单位：毫秒），服务器响应时限
    factory.setReadTimeout(10000);

    // 设置从连接池获取连接的超时时间（单位：毫秒）
    factory.setConnectionRequestTimeout(3000);

    // 创建连接池管理器
    PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager();

    // 设置最大连接数（整个连接池）
    connManager.setMaxTotal(100);

    // 设置每个路由（目标主机）上的最大连接数
    connManager.setDefaultMaxPerRoute(20);

    // 构建 HttpClient 并注入连接池管理器
    CloseableHttpClient httpClient = HttpClients.custom()
            .setConnectionManager(connManager)
            .build();

    // 将 HttpClient 设置到请求工厂中
    factory.setHttpClient(httpClient);

    // 返回使用自定义请求工厂的 RestTemplate 实例
    return new RestTemplate(factory);
}
```



## 忽略 HTTPS 证书校验

### 创建RestTemplate

```java
 /**
     * 创建一个忽略 HTTPS 证书校验的 HttpClient，仅用于测试环境。
     *
     * @return CloseableHttpClient
     */
    public static CloseableHttpClient createIgnoreSSLHttpClient() {
        try {
            SSLContext sslContext = SSLContextBuilder.create()
                    .loadTrustMaterial((chain, authType) -> true) // 信任所有
                    .build();

            SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(
                    sslContext, NoopHostnameVerifier.INSTANCE);

            return HttpClients.custom()
                    .setSSLSocketFactory(socketFactory)
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("创建忽略证书校验 HttpClient 失败", e);
        }
    }

    @Bean
    public RestTemplate unsafeRestTemplate() {
        // 使用忽略证书校验的 HttpClient
        CloseableHttpClient httpClient = createIgnoreSSLHttpClient();

        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setHttpClient(httpClient);

        return new RestTemplate(factory);
    }
```



## 生产配置

```java
package local.ateng.java.rest.config;

import lombok.RequiredArgsConstructor;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplate 配置类
 *
 * <p>该配置类主要用于统一管理 RestTemplate 的实例化与底层 HTTP 客户端配置，
 * 包括连接池管理、超时时间设置等。通过将参数抽取为全局常量，避免魔法值，
 * 提升可维护性和可扩展性。</p>
 *
 * <p>本配置基于 Spring Boot 2.7 环境，适用于需要高并发访问外部接口的场景。</p>
 *
 * @author 孔余
 * @since 2025-09-10
 */
@Configuration
@RequiredArgsConstructor
public class RestTemplateConfig {

    /**
     * 连接建立超时时间（毫秒）
     */
    private static final int CONNECT_TIMEOUT = 5000;

    /**
     * 读取数据超时时间（毫秒）
     */
    private static final int READ_TIMEOUT = 10000;

    /**
     * 从连接池获取连接的超时时间（毫秒）
     */
    private static final int CONNECTION_REQUEST_TIMEOUT = 3000;

    /**
     * 连接池最大连接数
     */
    private static final int MAX_TOTAL_CONNECTIONS = 100;

    /**
     * 每个路由（目标主机）的最大连接数
     */
    private static final int MAX_CONNECTIONS_PER_ROUTE = 20;

    /**
     * 构建并注册 RestTemplate Bean
     *
     * <p>该方法会配置底层的 {@link HttpComponentsClientHttpRequestFactory}，
     * 并使用 {@link PoolingHttpClientConnectionManager} 来管理连接池。
     * 最终生成的 RestTemplate 实例可直接在业务中通过注入方式使用。</p>
     *
     * @return 配置完成的 RestTemplate 实例
     */
    @Bean("atengRestTemplate")
    public RestTemplate atengRestTemplate() {
        // 创建 HTTP 请求工厂
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT);
        factory.setReadTimeout(READ_TIMEOUT);
        factory.setConnectionRequestTimeout(CONNECTION_REQUEST_TIMEOUT);

        // 创建连接池管理器
        PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager();
        connManager.setMaxTotal(MAX_TOTAL_CONNECTIONS);
        connManager.setDefaultMaxPerRoute(MAX_CONNECTIONS_PER_ROUTE);

        // 构建 HttpClient 并注入连接池管理器
        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(connManager)
                .build();

        // 将 HttpClient 设置到请求工厂中
        factory.setHttpClient(httpClient);

        // 创建 RestTemplate
        RestTemplate restTemplate = new RestTemplate(factory);

        // 创建并注册拦截器
        /*List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>();
        interceptors.add(authInterceptor);
        interceptors.add(loggingInterceptor);
        interceptors.add(retryInterceptor);
        restTemplate.setInterceptors(interceptors);*/

        // 返回配置完成的 RestTemplate
        return restTemplate;
    }
}
```



## 工具类RestUtil

```java
package local.ateng.java.rest.utils;

import cn.hutool.extra.spring.SpringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.util.Map;

/**
 * RestUtil 工具类
 *
 * <p>基于自定义 {@code myRestTemplate} 封装常用的 HTTP 接口调用方法，
 * 旨在简化服务间 RESTful 接口的调用流程，提高开发效率和代码可读性。</p>
 *
 * <h2>核心功能：</h2>
 * <ul>
 *     <li>快速调用常用 HTTP 方法：GET、POST、PUT、DELETE</li>
 *     <li>通用 {@code exchange} 方法，支持自定义 HTTP 方法、请求体和泛型返回值</li>
 *     <li>支持多种请求体格式：JSON、XML、表单（x-www-form-urlencoded）、文件上传（multipart/form-data）</li>
 *     <li>统一异常捕获与日志记录，便于排查接口调用问题</li>
 *     <li>支持动态 URL 参数和占位符替换</li>
 * </ul>
 *
 * <h2>使用示例：</h2>
 * <pre>
 *     // JSON POST 请求
 *     Map&lt;String, Object&gt; payload = new HashMap&lt;&gt;();
 *     payload.put("id", 123);
 *     payload.put("name", "张三");
 *     HttpEntity&lt;Map&lt;String, Object&gt;&gt; entity = RestUtil.jsonBodyWithHeaders(payload, null);
 *
 *     ResponseEntity&lt;String&gt; response = RestUtil.exchange(
 *         "https://api.example.com/user",
 *         HttpMethod.POST,
 *         entity,
 *         String.class
 *     );
 *
 *     // GET 请求示例
 *     ResponseEntity&lt;User&gt; resp = RestUtil.get("https://api.example.com/user/{id}", User.class, 123);
 *
 *     // 文件上传示例
 *     File file = new File("D:/test.png");
 *     HttpEntity&lt;MultiValueMap&lt;String, Object&gt;&gt; fileEntity =
 *         RestUtil.multipartBodyWithHeaders(file, "file", null);
 *     ResponseEntity&lt;String&gt; uploadResp = RestUtil.exchange(
 *         "https://api.example.com/upload",
 *         HttpMethod.POST,
 *         fileEntity,
 *         String.class
 *     );
 * </pre>
 *
 * <h2>适用场景：</h2>
 * <ul>
 *     <li>微服务间 RESTful 接口调用</li>
 *     <li>第三方 API 调用（JSON / XML / Form / File）</li>
 *     <li>需要统一接口调用日志和异常处理</li>
 * </ul>
 *
 * <h2>注意事项：</h2>
 * <ul>
 *     <li>本工具类依赖于注入的 {@code myRestTemplate} Bean</li>
 *     <li>POJO 请求体需保证 RestTemplate 配置了对应的 HttpMessageConverter（如 Jackson）</li>
 *     <li>文件上传需服务端支持 multipart/form-data</li>
 * </ul>
 *
 * <p>通过 RestUtil，开发者可快速发起接口请求，无需重复构建 HttpEntity 或处理异常，提高了接口调用的一致性和可维护性。</p>
 *
 * @author 孔余
 * @since 2025-09-09
 */
public class RestUtil {

    private static final Logger log = LoggerFactory.getLogger(RestUtil.class);

    /**
     * Spring 容器中注入的自定义 RestTemplate
     */
    private static final RestTemplate restTemplate = SpringUtil.getBean("atengRestTemplate", RestTemplate.class);

    // ================= GET =================

    /**
     * 发送 GET 请求，返回响应体对象。
     *
     * <p>适用于直接获取目标类型的响应场景。
     *
     * <p>使用示例：
     * <pre>
     *     String result = RestUtil.getForObject("http://example.com/api/{id}", String.class, 123);
     * </pre>
     *
     * @param url          请求 URL，可包含占位符
     * @param responseType 响应体类型
     * @param uriVariables URL 中的占位符参数
     * @param <T>          返回值泛型
     * @return 响应体对象
     * @throws RestClientException 请求失败时抛出
     */
    public static <T> T getForObject(String url, Class<T> responseType, Object... uriVariables) {
        try {
            return restTemplate.getForObject(url, responseType, uriVariables);
        } catch (RestClientException e) {
            log.error("GET 请求失败: url={}, 参数={}", url, uriVariables, e);
            throw e;
        }
    }

    /**
     * 发送 GET 请求，返回完整的 {@link ResponseEntity}。
     *
     * <p>适用于需要获取响应头、状态码等额外信息的场景。
     *
     * <p>使用示例：
     * <pre>
     *     ResponseEntity&lt;String&gt; resp = RestUtil.getForEntity("http://example.com/api/{id}", String.class, 123);
     *     if (resp.getStatusCode().is2xxSuccessful()) {
     *         System.out.println(resp.getBody());
     *     }
     * </pre>
     *
     * @param url          请求 URL，可包含占位符
     * @param responseType 响应体类型
     * @param uriVariables URL 中的占位符参数
     * @param <T>          返回值泛型
     * @return 包含响应状态码、头信息及响应体的对象
     * @throws RestClientException 请求失败时抛出
     */
    public static <T> ResponseEntity<T> getForEntity(String url, Class<T> responseType, Object... uriVariables) {
        try {
            return restTemplate.getForEntity(url, responseType, uriVariables);
        } catch (RestClientException e) {
            log.error("GET 请求失败: url={}, 参数={}", url, uriVariables, e);
            throw e;
        }
    }

    // ================= POST =================

    /**
     * 发送 POST 请求，返回响应体对象。
     *
     * <p>适用于需要提交请求体并直接获取结果的场景。
     *
     * <p>使用示例：
     * <pre>
     *     User user = new User("Tom", 18);
     *     String result = RestUtil.postForObject("http://example.com/save", user, String.class);
     * </pre>
     *
     * @param url          请求 URL，可包含占位符
     * @param request      请求体对象
     * @param responseType 响应体类型
     * @param uriVariables URL 中的占位符参数
     * @param <T>          返回值泛型
     * @return 响应体对象
     * @throws RestClientException 请求失败时抛出
     */
    public static <T> T postForObject(String url, Object request, Class<T> responseType, Object... uriVariables) {
        try {
            return restTemplate.postForObject(url, request, responseType, uriVariables);
        } catch (RestClientException e) {
            log.error("POST 请求失败: url={}, 请求体={}", url, request, e);
            throw e;
        }
    }

    /**
     * 发送 POST 请求，返回完整的 {@link ResponseEntity}。
     *
     * <p>适用于需要获取响应头、状态码等额外信息的场景。
     *
     * <p>使用示例：
     * <pre>
     *     User user = new User("Tom", 18);
     *     ResponseEntity&lt;String&gt; resp = RestUtil.postForEntity("http://example.com/save", user, String.class);
     *     if (resp.getStatusCode().is2xxSuccessful()) {
     *         System.out.println(resp.getBody());
     *     }
     * </pre>
     *
     * @param url          请求 URL，可包含占位符
     * @param request      请求体对象
     * @param responseType 响应体类型
     * @param uriVariables URL 中的占位符参数
     * @param <T>          返回值泛型
     * @return 包含响应状态码、头信息及响应体的对象
     * @throws RestClientException 请求失败时抛出
     */
    public static <T> ResponseEntity<T> postForEntity(String url, Object request, Class<T> responseType, Object... uriVariables) {
        try {
            return restTemplate.postForEntity(url, request, responseType, uriVariables);
        } catch (RestClientException e) {
            log.error("POST 请求失败: url={}, 请求体={}", url, request, e);
            throw e;
        }
    }

    // ================= PUT =================

    /**
     * 发送 PUT 请求，不返回响应体。
     *
     * <p>适用于更新资源的场景，通常 REST 接口返回 204 No Content。
     *
     * <p>使用示例：
     * <pre>
     *     User user = new User("Tom", 20);
     *     RestUtil.put("http://example.com/update/{id}", user, 123);
     * </pre>
     *
     * @param url          请求 URL，可包含占位符
     * @param request      请求体对象
     * @param uriVariables URL 中的占位符参数
     * @throws RestClientException 请求失败时抛出
     */
    public static void put(String url, Object request, Object... uriVariables) {
        try {
            restTemplate.put(url, request, uriVariables);
        } catch (RestClientException e) {
            log.error("PUT 请求失败: url={}, 请求体={}", url, request, e);
            throw e;
        }
    }

    // ================= DELETE =================

    /**
     * 发送 DELETE 请求。
     *
     * <p>适用于删除资源的场景。
     *
     * <p>使用示例：
     * <pre>
     *     RestUtil.delete("http://example.com/delete/{id}", 123);
     * </pre>
     *
     * @param url          请求 URL，可包含占位符
     * @param uriVariables URL 中的占位符参数
     * @throws RestClientException 请求失败时抛出
     */
    public static void delete(String url, Object... uriVariables) {
        try {
            restTemplate.delete(url, uriVariables);
        } catch (RestClientException e) {
            log.error("DELETE 请求失败: url={}, 参数={}", url, uriVariables, e);
            throw e;
        }
    }

    // ================= Exchange =================

    /**
     * 通用请求方法，支持指定 HTTP 方法、请求体、响应类型。
     *
     * <p>适用于需要高度自定义请求头或复杂请求场景。
     *
     * <p>使用示例：
     * <pre>
     *     HttpHeaders headers = new HttpHeaders();
     *     headers.setContentType(MediaType.APPLICATION_JSON);
     *     HttpEntity&lt;User&gt; entity = new HttpEntity&lt;&gt;(new User("Tom", 18), headers);
     *
     *     ResponseEntity&lt;String&gt; resp = RestUtil.exchange(
     *         "http://example.com/save",
     *         HttpMethod.POST,
     *         entity,
     *         String.class
     *     );
     * </pre>
     *
     * @param url           请求 URL
     * @param method        HTTP 方法（GET, POST, PUT, DELETE 等）
     * @param requestEntity 封装了请求头和请求体的实体对象
     * @param responseType  响应体类型
     * @param uriVariables  URL 中的占位符参数
     * @param <T>           返回值泛型
     * @return 包含响应状态码、头信息及响应体的对象
     * @throws RestClientException 请求失败时抛出
     */
    public static <T> ResponseEntity<T> exchange(
            String url,
            HttpMethod method,
            HttpEntity<?> requestEntity,
            Class<T> responseType,
            Object... uriVariables) {
        try {
            return restTemplate.exchange(url, method, requestEntity, responseType, uriVariables);
        } catch (RestClientException e) {
            log.error("HTTP 请求失败: url={}, method={}, 请求体={}", url, method, requestEntity, e);
            throw e;
        }
    }

    /**
     * 通用请求方法，支持复杂的泛型返回值（如 List&lt;Map&lt;String,Object&gt;&gt;）。
     *
     * <p>适用于需要反序列化为复杂集合或泛型对象的场景。
     *
     * <p>使用示例：
     * <pre>
     *     HttpHeaders headers = new HttpHeaders();
     *     headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
     *     HttpEntity&lt;Void&gt; entity = new HttpEntity&lt;&gt;(headers);
     *
     *     ResponseEntity&lt;List&lt;User&gt;&gt; resp = RestUtil.exchange(
     *         "http://example.com/users",
     *         HttpMethod.GET,
     *         entity,
     *         new ParameterizedTypeReference&lt;List&lt;User&gt;&gt;() {}
     *     );
     * </pre>
     *
     * @param url           请求 URL
     * @param method        HTTP 方法（GET, POST, PUT, DELETE 等）
     * @param requestEntity 封装了请求头和请求体的实体对象
     * @param responseType  响应体类型的泛型引用
     * @param <T>           返回值泛型
     * @return 包含响应状态码、头信息及响应体的对象
     * @throws RestClientException 请求失败时抛出
     */
    public static <T> ResponseEntity<T> exchange(
            String url,
            HttpMethod method,
            HttpEntity<?> requestEntity,
            ParameterizedTypeReference<T> responseType) {
        try {
            return restTemplate.exchange(url, method, requestEntity, responseType);
        } catch (RestClientException e) {
            log.error("HTTP 请求失败: url={}, method={}, 请求体={}", url, method, requestEntity, e);
            throw e;
        }
    }

    /**
     * 构建仅包含请求头的 {@link HttpEntity}。
     *
     * <p>使用示例：
     * <pre>
     *     HttpEntity&lt;Void&gt; entity = RestEntityBuilder.headersOnly(Collections.singletonMap("Authorization", "Bearer xxx"));
     *     ResponseEntity&lt;String&gt; resp = RestUtil.exchange(url, HttpMethod.GET, entity, String.class);
     * </pre>
     *
     * @param headersMap 请求头键值对
     * @return HttpEntity 实例
     */
    public static HttpEntity<Void> headersOnly(Map<String, String> headersMap) {
        HttpHeaders headers = new HttpHeaders();
        if (headersMap != null) {
            headersMap.forEach(headers::set);
        }
        return new HttpEntity<>(headers);
    }

    /**
     * 构建包含 JSON 请求体的 {@link HttpEntity}。
     *
     * <p>Content-Type 自动设置为 {@code application/json}。
     *
     * <p>使用示例：
     * <pre>
     *     User user = new User("Tom", 18);
     *     HttpEntity&lt;User&gt; entity = RestEntityBuilder.jsonBody(user);
     *     ResponseEntity&lt;String&gt; resp = RestUtil.exchange(url, HttpMethod.POST, entity, String.class);
     * </pre>
     *
     * @param body 请求体对象
     * @param <T>  请求体类型
     * @return HttpEntity 实例
     */
    public static <T> HttpEntity<T> jsonBody(T body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    /**
     * 构建包含 XML 请求体的 {@link HttpEntity}。
     *
     * <p>Content-Type 自动设置为 {@code application/xml}。
     * <p>适用于调用需要 XML 格式请求体的接口，例如部分 SOAP/老系统接口。
     *
     * <p>使用示例：
     * <pre>
     *     String xml = "&lt;user&gt;&lt;name&gt;Tom&lt;/name&gt;&lt;age&gt;18&lt;/age&gt;&lt;/user&gt;";
     *     HttpEntity&lt;String&gt; entity = RestEntityBuilder.xmlBody(xml);
     *     ResponseEntity&lt;String&gt; resp = RestUtil.exchange(url, HttpMethod.POST, entity, String.class);
     * </pre>
     *
     * @param xmlBody XML 格式的字符串
     * @return HttpEntity 实例
     */
    public static HttpEntity<String> xmlBody(String xmlBody) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_XML);
        return new HttpEntity<>(xmlBody, headers);
    }

    /**
     * 通用构建方法：根据传入的请求体、请求头 map 和 Content-Type 构建 {@link HttpEntity}。
     *
     * <p>使用示例（POJO -> JSON）：
     * <pre>
     *     User user = new User("Tom", 18);
     *     HttpEntity&lt;User&gt; entity = RestEntityBuilder.bodyWithHeaders(user,
     *             Collections.singletonMap("Authorization", "Bearer x"),
     *             MediaType.APPLICATION_JSON);
     *     ResponseEntity&lt;String&gt; resp = RestUtil.exchange(url, HttpMethod.POST, entity, String.class);
     * </pre>
     *
     * <p>使用示例（已经是 JSON 字符串）：
     * <pre>
     *     String json = "{\"id\":123,\"name\":\"张三\"}";
     *     HttpEntity&lt;String&gt; entity = RestEntityBuilder.bodyWithHeaders(json,
     *             Collections.singletonMap("X-Custom","v"),
     *             MediaType.APPLICATION_JSON);
     * </pre>
     *
     * @param body       请求体，可以是 {@code String}（已序列化）或 POJO（由 RestTemplate 的 converter 序列化）
     * @param headersMap 请求头键值对（可为 null）
     * @param contentType Content-Type（可为 null，null 时不会设置 Content-Type）
     * @param <T>        请求体类型
     * @return 构建好的 {@link HttpEntity}
     */
    public static <T> HttpEntity<T> bodyWithHeaders(T body, Map<String, String> headersMap, MediaType contentType) {
        HttpHeaders headers = new HttpHeaders();
        if (headersMap != null && !headersMap.isEmpty()) {
            headersMap.forEach(headers::set);
        }
        if (contentType != null) {
            headers.setContentType(contentType);
        }
        return new HttpEntity<>(body, headers);
    }

    /**
     * 快捷：构建 JSON 请求体的 {@link HttpEntity}（委托给 {@link #bodyWithHeaders}）。
     *
     * <p>说明：
     * <ul>
     *   <li>当 body 为 POJO 时，RestTemplate 会使用 Jackson 等 converter 自动序列化为 JSON（前提是相关 converter 已配置）。</li>
     *   <li>当 body 为 String 时，方法会把该字符串原样作为请求体发送（不会再做序列化）。</li>
     * </ul>
     * </p>
     *
     * <p>使用示例（POJO）：</p>
     * <pre>
     *     User user = new User("Tom", 18);
     *     HttpEntity&lt;User&gt; entity = RestEntityBuilder.jsonBodyWithHeaders(user,
     *             Collections.singletonMap("Authorization", "Bearer xxx"));
     *     ResponseEntity&lt;String&gt; resp = RestUtil.exchange(url, HttpMethod.POST, entity, String.class);
     * </pre>
     *
     * @param body       请求体（POJO 或已序列化的 JSON 字符串）
     * @param headersMap 请求头（可为 null）
     * @param <T>        请求体类型
     * @return HttpEntity
     */
    public static <T> HttpEntity<T> jsonBodyWithHeaders(T body, Map<String, String> headersMap) {
        return bodyWithHeaders(body, headersMap, MediaType.APPLICATION_JSON);
    }

    /**
     * 快捷：构建 XML 请求体的 {@link HttpEntity}（委托给 {@link #bodyWithHeaders}）。
     *
     * <p>通常场景：向需要 application/xml 的老系统或 SOAP-like 接口发送 XML 字符串。</p>
     *
     * <p>使用示例（XML 字符串）：</p>
     * <pre>
     *     String xml = "&lt;user&gt;&lt;name&gt;Tom&lt;/name&gt;&lt;age&gt;18&lt;/age&gt;&lt;/user&gt;";
     *     HttpEntity&lt;String&gt; entity = RestEntityBuilder.xmlBodyWithHeaders(xml,
     *             Collections.singletonMap("Authorization", "Bearer xxx"));
     *     ResponseEntity&lt;String&gt; resp = RestUtil.exchange(url, HttpMethod.POST, entity, String.class);
     * </pre>
     *
     * @param xmlBody    XML 字符串（建议传 String）
     * @param headersMap 请求头（可为 null）
     * @return HttpEntity&lt;String&gt;
     */
    public static HttpEntity<String> xmlBodyWithHeaders(String xmlBody, Map<String, String> headersMap) {
        return bodyWithHeaders(xmlBody, headersMap, MediaType.APPLICATION_XML);
    }

    // ================= 表单（application/x-www-form-urlencoded） ================= //

    /**
     * 构建 application/x-www-form-urlencoded 的表单请求体。
     *
     * <p>使用示例：</p>
     * <pre>
     *     Map&lt;String, String&gt; formData = new HashMap&lt;&gt;();
     *     formData.put("username", "tom");
     *     formData.put("password", "123456");
     *
     *     HttpEntity&lt;MultiValueMap&lt;String, String&gt;&gt; entity =
     *         RestEntityBuilder.formBodyWithHeaders(formData, null);
     * </pre>
     *
     * @param formData   表单数据（键值对）
     * @param headersMap 请求头（可为 null）
     * @return HttpEntity
     */
    public static HttpEntity<MultiValueMap<String, String>> formBodyWithHeaders(Map<String, String> formData,
                                                                                Map<String, String> headersMap) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        if (formData != null) {
            formData.forEach(body::add);
        }
        return bodyWithHeaders(body, headersMap, MediaType.APPLICATION_FORM_URLENCODED);
    }

    // ================= 文件上传（multipart/form-data） ================= //

    /**
     * 构建 multipart/form-data 的文件上传请求体。
     *
     * <p>使用示例：</p>
     * <pre>
     *     File file = new File("D:/test.png");
     *     HttpEntity&lt;MultiValueMap&lt;String, Object&gt;&gt; entity =
     *         RestEntityBuilder.multipartBodyWithHeaders(file, "file", null);
     * </pre>
     *
     * @param file       文件对象
     * @param fieldName  表单字段名，例如 "file"
     * @param headersMap 请求头（可为 null）
     * @return HttpEntity
     */
    public static HttpEntity<MultiValueMap<String, Object>> multipartBodyWithHeaders(File file,
                                                                                     String fieldName,
                                                                                     Map<String, String> headersMap) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add(fieldName, file);

        return bodyWithHeaders(body, headersMap, MediaType.MULTIPART_FORM_DATA);
    }

    /**
     * 构建 multipart/form-data 的多文件上传请求体。
     *
     * @param files      多个文件（字段名相同）
     * @param fieldName  表单字段名
     * @param headersMap 请求头（可为 null）
     * @return HttpEntity
     */
    public static HttpEntity<MultiValueMap<String, Object>> multipartBodyWithHeaders(File[] files,
                                                                                     String fieldName,
                                                                                     Map<String, String> headersMap) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        if (files != null) {
            for (File file : files) {
                body.add(fieldName, file);
            }
        }
        return bodyWithHeaders(body, headersMap, MediaType.MULTIPART_FORM_DATA);
    }

}

```

