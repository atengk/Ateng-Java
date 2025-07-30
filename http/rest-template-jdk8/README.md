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

