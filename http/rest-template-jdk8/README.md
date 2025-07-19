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

