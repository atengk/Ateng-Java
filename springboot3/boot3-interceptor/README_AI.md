# Spring Boot 拦截器

Spring Boot 拦截器是 Spring MVC 提供的一种请求拦截机制，主要用于在请求进入 Controller 前后执行统一处理逻辑。它通常用于登录校验、权限判断、请求日志记录、接口耗时统计等场景，可以减少 Controller 中重复的公共代码，让业务接口更加聚焦于自身逻辑。

## 拦截器概述

本节主要介绍 Spring Boot 拦截器的基本作用、适用场景，以及拦截器和过滤器之间的区别。理解这些内容后，再进行拦截器编码和注册配置会更加清晰。

### 拦截器的作用

拦截器的核心作用是在 Spring MVC 处理请求的过程中，对请求进行前置处理、后置处理和完成后处理。它工作在 `DispatcherServlet` 之后、Controller 方法调用前后的阶段，可以拿到当前请求对象、响应对象以及即将执行的处理器对象。

Spring Boot 3 中常用的拦截器接口是 `HandlerInterceptor`，该接口位于以下包中：

```java
org.springframework.web.servlet.HandlerInterceptor
```

`HandlerInterceptor` 提供了三个常用方法：

| 方法              | 执行时机                              | 常见用途                             |
| ----------------- | ------------------------------------- | ------------------------------------ |
| `preHandle`       | Controller 方法执行之前               | 登录校验、权限校验、请求参数预检查   |
| `postHandle`      | Controller 方法执行之后，视图渲染之前 | 追加模型数据、记录业务执行结果       |
| `afterCompletion` | 整个请求完成之后                      | 资源清理、异常日志记录、接口耗时统计 |

在实际开发中，使用频率最高的是 `preHandle` 和 `afterCompletion`。其中 `preHandle` 可以决定请求是否继续向下执行，如果返回 `true`，请求继续进入 Controller；如果返回 `false`，请求会被拦截，后续 Controller 方法不会执行。

例如登录校验、权限校验这类逻辑，不建议写在每一个 Controller 方法中。更合理的做法是将它们抽取到拦截器中统一处理。这样可以避免重复代码，也方便后续统一维护。

### 适用场景

拦截器适合处理与 Web 请求流程强相关的通用逻辑，尤其适合在请求进入 Controller 前后进行统一增强。

常见适用场景如下：

| 场景             | 说明                                                      |
| ---------------- | --------------------------------------------------------- |
| 登录状态校验     | 判断请求是否携带有效登录凭证，例如 Token、Session、Cookie |
| 权限前置校验     | 在接口执行前判断当前用户是否拥有访问权限                  |
| 请求日志记录     | 记录请求地址、请求方式、客户端 IP、请求参数、响应耗时     |
| 接口耗时统计     | 在请求开始时记录时间，在请求结束后计算接口执行耗时        |
| 多租户上下文处理 | 从请求头中读取租户标识，并放入当前线程上下文              |
| 国际化语言处理   | 根据请求头或请求参数设置当前请求的语言环境                |
| 防重复提交       | 在请求进入业务方法前判断是否存在短时间内重复提交          |
| 接口访问控制     | 对指定路径进行统一访问限制，例如后台管理接口、内部接口    |

拦截器通常用于处理 Controller 层相关的请求控制逻辑。如果需要处理更底层的 Servlet 请求，例如修改请求体、统一设置编码、处理跨域预检请求等，过滤器通常更合适。

### 拦截器与过滤器的区别

拦截器和过滤器都可以对 Web 请求进行统一处理，但它们所属的技术体系、执行位置和使用场景不同。

| 对比项                       | 拦截器                                     | 过滤器                                               |
| ---------------------------- | ------------------------------------------ | ---------------------------------------------------- |
| 技术来源                     | Spring MVC                                 | Servlet 规范                                         |
| 核心接口                     | `HandlerInterceptor`                       | `Filter`                                             |
| 执行位置                     | 请求进入 Spring MVC 并匹配到 Handler 之后  | 请求进入 Servlet 容器后、到达 DispatcherServlet 之前 |
| 作用范围                     | 主要拦截 Controller 请求                   | 可以拦截几乎所有 Web 请求                            |
| 是否依赖 Spring MVC          | 依赖                                       | 不依赖                                               |
| 是否容易获取 Controller 信息 | 可以获取 Handler 信息                      | 通常无法直接获取 Controller 方法信息                 |
| 常见用途                     | 登录校验、权限判断、接口日志、接口耗时统计 | 编码处理、CORS、安全过滤、请求包装                   |

二者的执行顺序可以简单理解为：

```text
客户端请求
  -> Filter
  -> DispatcherServlet
  -> HandlerInterceptor.preHandle
  -> Controller
  -> HandlerInterceptor.postHandle
  -> HandlerInterceptor.afterCompletion
  -> Filter
  -> 客户端响应
```

实际开发中可以按照以下原则选择：

1. 需要处理 Spring MVC 层面的接口访问控制，优先使用拦截器。
2. 需要获取 Controller 方法、Handler 对象或方法注解，优先使用拦截器。
3. 需要在请求进入 Spring MVC 之前处理，优先使用过滤器。
4. 需要包装 `HttpServletRequest`、处理编码、处理跨域、安全链路等底层逻辑，优先使用过滤器。

## 开发环境准备

本节主要说明开发 Spring Boot 3 拦截器所需的基础环境和 Maven 依赖配置。拦截器属于 Spring MVC 功能，因此项目中需要引入 Web 场景依赖。

### Spring Boot 3 基础环境

Spring Boot 3 基于 Spring Framework 6 构建，最低要求 Java 17。因此在开发拦截器之前，需要先确认本地 JDK、Maven 和项目版本满足要求。

| 环境        | 推荐版本       | 说明                                |
| ----------- | -------------- | ----------------------------------- |
| JDK         | 17 或更高版本  | Spring Boot 3 最低要求 Java 17      |
| Maven       | 3.8 或更高版本 | 用于项目构建和依赖管理              |
| Spring Boot | 3.x            | 当前文档基于 Spring Boot 3          |
| IDE         | IntelliJ IDEA  | 推荐用于 Spring Boot 项目开发和调试 |

可以通过以下命令查看本地 Java 和 Maven 版本：

```bash
java -version
mvn -version
```

如果输出中可以看到 Java 17 或更高版本，说明 JDK 环境满足 Spring Boot 3 的基本要求。

示例输出如下：

```text
java version "17.0.x"
Apache Maven 3.8.x
```

推荐的项目基础目录结构如下：

```text
springboot-interceptor-demo
├── pom.xml
└── src
    └── main
        ├── java
        │   └── io
        │       └── github
        │           └── atengk
        │               ├── SpringBootInterceptorApplication.java
        │               ├── config
        │               │   └── WebMvcConfig.java
        │               ├── interceptor
        │               │   └── LoginInterceptor.java
        │               └── controller
        │                   └── TestController.java
        └── resources
            └── application.yml
```

其中，`interceptor` 目录用于存放自定义拦截器类，`config` 目录用于存放 Spring MVC 配置类，`controller` 目录用于编写测试接口，`application.yml` 用于维护项目基础配置。

### Maven 依赖配置

Spring Boot 拦截器依赖 Spring MVC，因此项目中必须引入 `spring-boot-starter-web`。该依赖会自动引入 Spring MVC、内置 Tomcat、Jackson 等 Web 开发相关组件。

如果项目使用 Spring Boot 官方父工程，推荐使用以下 Maven 配置。

文件位置：`pom.xml`

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">

    <modelVersion>4.0.0</modelVersion>

    <!-- 使用 Spring Boot 父工程统一管理插件和依赖版本 -->
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.3.5</version>
        <relativePath/>
    </parent>

    <groupId>io.github.atengk</groupId>
    <artifactId>springboot-interceptor-demo</artifactId>
    <version>1.0.0</version>
    <name>springboot-interceptor-demo</name>
    <description>Spring Boot 3 拦截器示例项目</description>

    <properties>
        <!-- Spring Boot 3 最低要求 Java 17 -->
        <java.version>17</java.version>

        <!-- Hutool 工具类版本 -->
        <hutool.version>5.8.32</hutool.version>
    </properties>

    <dependencies>
        <!-- Web 开发依赖：提供 Spring MVC、内置 Tomcat 和拦截器能力 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Hutool 工具类：简化字符串、集合、日期、JSON 等常用处理 -->
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
            <version>${hutool.version}</version>
        </dependency>

        <!-- Lombok：简化日志对象、Getter、Setter、构造方法等代码 -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- 测试依赖：用于编写单元测试和接口测试 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

</project>
```

如果不使用 Spring Boot 父工程，也可以通过 `dependencyManagement` 统一管理 Spring Boot 依赖版本。

文件位置：`pom.xml`

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">

    <modelVersion>4.0.0</modelVersion>

    <groupId>io.github.atengk</groupId>
    <artifactId>springboot-interceptor-demo</artifactId>
    <version>1.0.0</version>
    <name>springboot-interceptor-demo</name>
    <description>Spring Boot 3 拦截器示例项目</description>

    <properties>
        <!-- Spring Boot 3 要求 JDK 17 或更高版本 -->
        <java.version>17</java.version>

        <!-- Spring Boot 依赖版本 -->
        <spring-boot.version>3.3.5</spring-boot.version>

        <!-- Hutool 工具类版本 -->
        <hutool.version>5.8.32</hutool.version>

        <!-- Lombok 版本 -->
        <lombok.version>1.18.34</lombok.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <!-- 统一管理 Spring Boot 相关依赖版本 -->
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-dependencies</artifactId>
                <version>${spring-boot.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <dependencies>
        <!-- Web 开发依赖：包含 Spring MVC、内置 Tomcat、JSON 序列化等能力 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Hutool 工具类：用于字符串、对象、集合、日期、JSON 等常用处理 -->
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
            <version>${hutool.version}</version>
        </dependency>

        <!-- Lombok：简化实体类、日志对象和构造方法代码 -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <version>${lombok.version}</version>
            <optional>true</optional>
        </dependency>

        <!-- 测试依赖：用于编写 Spring Boot 单元测试和接口测试 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <!-- Spring Boot Maven 插件：用于打包和运行 Spring Boot 应用 -->
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>

            <!-- Maven 编译插件：指定 Java 编译版本 -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <configuration>
                    <source>${java.version}</source>
                    <target>${java.version}</target>
                    <encoding>UTF-8</encoding>
                </configuration>
            </plugin>
        </plugins>
    </build>

</project>
```

对于基础拦截器开发，最小依赖只需要 `spring-boot-starter-web`。如果后续文档需要继续编写登录校验、Token 解析、请求日志记录等示例，建议同时保留 Hutool 和 Lombok，用于简化工具处理和日志代码。



## 拦截器核心实现

本节主要介绍如何创建自定义拦截器，并分别实现 `preHandle`、`postHandle` 和 `afterCompletion` 三个核心方法。示例中通过登录令牌校验和请求耗时统计演示拦截器的常见用法。

### 创建自定义拦截器

自定义拦截器需要实现 Spring MVC 提供的 `HandlerInterceptor` 接口。该接口位于 `org.springframework.web.servlet` 包下，适用于 Spring Boot 3 Web 项目。

推荐将拦截器类放在 `interceptor` 包下，目录结构如下：

```text
src/main/java/io/github/atengk/interceptor/LoginInterceptor.java
```

下面的代码创建了一个登录校验拦截器，用于拦截指定接口请求，并根据请求头中的 Token 判断是否允许继续访问。

文件位置：`src/main/java/io/github/atengk/interceptor/LoginInterceptor.java`

```java
package io.github.atengk.interceptor;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 登录校验拦截器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Component
public class LoginInterceptor implements HandlerInterceptor {

    /**
     * 请求开始时间属性名
     */
    private static final String REQUEST_START_TIME = "requestStartTime";

    /**
     * 登录令牌请求头
     */
    private static final String TOKEN_HEADER = "Authorization";

    /**
     * 请求进入 Controller 之前执行
     *
     * @param request  当前请求对象
     * @param response 当前响应对象
     * @param handler  当前处理器对象
     * @return true 表示放行，false 表示拦截
     * @throws Exception 请求处理异常
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        request.setAttribute(REQUEST_START_TIME, System.currentTimeMillis());

        String requestUri = request.getRequestURI();
        String method = request.getMethod();
        String token = request.getHeader(TOKEN_HEADER);

        log.info("请求进入拦截器，请求方式：{}，请求地址：{}", method, requestUri);

        if (StrUtil.isBlank(token)) {
            log.warn("请求缺少登录令牌，请求地址：{}", requestUri);
            writeUnauthorizedResponse(response, "请先登录后再访问");
            return false;
        }

        if (!checkToken(token)) {
            log.warn("登录令牌校验失败，请求地址：{}，令牌：{}", requestUri, token);
            writeUnauthorizedResponse(response, "登录状态无效或已过期");
            return false;
        }

        log.info("登录令牌校验通过，请求地址：{}", requestUri);
        return true;
    }

    /**
     * Controller 方法执行之后，视图渲染之前执行
     *
     * @param request      当前请求对象
     * @param response     当前响应对象
     * @param handler      当前处理器对象
     * @param modelAndView 模型和视图对象
     */
    @Override
    public void postHandle(HttpServletRequest request,
                           HttpServletResponse response,
                           Object handler,
                           org.springframework.web.servlet.ModelAndView modelAndView) {
        log.debug("Controller 方法执行完成，请求地址：{}", request.getRequestURI());
    }

    /**
     * 整个请求完成之后执行
     *
     * @param request  当前请求对象
     * @param response 当前响应对象
     * @param handler  当前处理器对象
     * @param ex       请求处理过程中的异常对象
     */
    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) {
        Object startTimeObj = request.getAttribute(REQUEST_START_TIME);
        long startTime = startTimeObj instanceof Long ? (Long) startTimeObj : System.currentTimeMillis();
        long costTime = System.currentTimeMillis() - startTime;

        if (ex != null) {
            log.error("请求执行异常，请求地址：{}，耗时：{}ms", request.getRequestURI(), costTime, ex);
            return;
        }

        log.info("请求执行完成，请求地址：{}，响应状态：{}，耗时：{}ms",
                request.getRequestURI(),
                response.getStatus(),
                costTime);
    }

    /**
     * 校验登录令牌
     *
     * @param token 登录令牌
     * @return true 表示令牌有效，false 表示令牌无效
     */
    private boolean checkToken(String token) {
        // 示例写法：实际项目中可以替换为 JWT、Sa-Token、Redis Token 或数据库会话校验
        return StrUtil.equals("Bearer ateng-token", token);
    }

    /**
     * 写入未授权响应
     *
     * @param response 当前响应对象
     * @param message  响应提示信息
     * @throws IOException 响应写入异常
     */
    private void writeUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json;charset=UTF-8");

        Map<String, Object> result = new HashMap<>();
        result.put("code", HttpStatus.UNAUTHORIZED.value());
        result.put("message", message);
        result.put("data", null);

        response.getWriter().write(JSONUtil.toJsonStr(result));
    }

}
```

该拦截器中使用 `Authorization` 请求头作为登录令牌来源。示例中的合法 Token 为：

```text
Bearer ateng-token
```

实际项目中不建议使用固定字符串校验 Token，可以替换为 JWT 解析、Sa-Token 校验、Redis 会话校验或统一认证中心校验。

### 实现 preHandle 方法

`preHandle` 方法在 Controller 方法执行之前调用，是拦截器中最常用的方法。它的返回值决定请求是否继续执行。

方法签名如下：

```java
boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception;
```

返回值含义如下：

| 返回值  | 说明                                   |
| ------- | -------------------------------------- |
| `true`  | 请求继续向下执行，进入 Controller 方法 |
| `false` | 请求被拦截，Controller 方法不会执行    |

`preHandle` 通常用于登录校验、权限校验、接口限流、请求参数预检查等前置逻辑。

在上面的示例中，`preHandle` 主要完成三件事：

1. 记录请求开始时间，用于后续计算接口耗时。
2. 从请求头中读取 `Authorization` 登录令牌。
3. 校验令牌是否存在、是否有效。

核心逻辑如下：

```java
@Override
public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
    request.setAttribute(REQUEST_START_TIME, System.currentTimeMillis());

    String requestUri = request.getRequestURI();
    String method = request.getMethod();
    String token = request.getHeader(TOKEN_HEADER);

    log.info("请求进入拦截器，请求方式：{}，请求地址：{}", method, requestUri);

    if (StrUtil.isBlank(token)) {
        log.warn("请求缺少登录令牌，请求地址：{}", requestUri);
        writeUnauthorizedResponse(response, "请先登录后再访问");
        return false;
    }

    if (!checkToken(token)) {
        log.warn("登录令牌校验失败，请求地址：{}，令牌：{}", requestUri, token);
        writeUnauthorizedResponse(response, "登录状态无效或已过期");
        return false;
    }

    log.info("登录令牌校验通过，请求地址：{}", requestUri);
    return true;
}
```

这里使用 Hutool 的 `StrUtil.isBlank` 判断 Token 是否为空，可以同时处理 `null`、空字符串和空白字符串。相比手动判断，代码更简洁。

需要注意的是，当 `preHandle` 返回 `false` 时，应该主动向客户端写入响应内容，否则客户端可能拿不到明确的错误提示。

### 实现 postHandle 方法

`postHandle` 方法在 Controller 方法执行完成之后调用，但它的执行时机早于视图渲染。对于传统 MVC 页面应用，可以在该方法中对 `ModelAndView` 进行统一处理。

方法签名如下：

```java
void postHandle(HttpServletRequest request,
                HttpServletResponse response,
                Object handler,
                ModelAndView modelAndView) throws Exception;
```

在前后端分离的 REST 接口项目中，`postHandle` 使用频率相对较低，因为接口通常直接返回 JSON 数据，不涉及传统视图渲染。

常见用途包括：

| 用途                     | 说明                                   |
| ------------------------ | -------------------------------------- |
| 追加公共模型数据         | 给页面统一追加系统名称、当前用户信息等 |
| 记录 Controller 执行结果 | 在 Controller 正常执行后记录日志       |
| 修改视图信息             | 对传统 MVC 页面视图进行统一处理        |

示例代码如下：

```java
@Override
public void postHandle(HttpServletRequest request,
                       HttpServletResponse response,
                       Object handler,
                       org.springframework.web.servlet.ModelAndView modelAndView) {
    log.debug("Controller 方法执行完成，请求地址：{}", request.getRequestURI());
}
```

如果项目是纯 REST API 项目，`postHandle` 可以不实现，或者仅用于调试日志。请求耗时统计、异常日志、资源清理等逻辑更适合放在 `afterCompletion` 中处理。

### 实现 afterCompletion 方法

`afterCompletion` 方法在整个请求完成之后执行。无论 Controller 正常返回，还是请求过程中出现异常，只要请求进入了拦截器链，该方法通常都会被调用。

方法签名如下：

```java
void afterCompletion(HttpServletRequest request,
                     HttpServletResponse response,
                     Object handler,
                     Exception ex) throws Exception;
```

`afterCompletion` 适合处理请求结束后的收尾逻辑，例如接口耗时统计、异常日志记录、ThreadLocal 清理等。

在示例中，`afterCompletion` 主要根据 `preHandle` 中记录的开始时间计算请求耗时：

```java
@Override
public void afterCompletion(HttpServletRequest request,
                            HttpServletResponse response,
                            Object handler,
                            Exception ex) {
    Object startTimeObj = request.getAttribute(REQUEST_START_TIME);
    long startTime = startTimeObj instanceof Long ? (Long) startTimeObj : System.currentTimeMillis();
    long costTime = System.currentTimeMillis() - startTime;

    if (ex != null) {
        log.error("请求执行异常，请求地址：{}，耗时：{}ms", request.getRequestURI(), costTime, ex);
        return;
    }

    log.info("请求执行完成，请求地址：{}，响应状态：{}，耗时：{}ms",
            request.getRequestURI(),
            response.getStatus(),
            costTime);
}
```

如果拦截器中使用了 `ThreadLocal` 保存用户信息、租户信息、链路追踪 ID 等上下文数据，建议在 `afterCompletion` 中统一清理，避免线程复用导致数据污染。

示例：

```java
@Override
public void afterCompletion(HttpServletRequest request,
                            HttpServletResponse response,
                            Object handler,
                            Exception ex) {
    try {
        log.info("请求处理完成，请求地址：{}", request.getRequestURI());
    } finally {
        // 示例：清理当前线程上下文，实际项目中替换为自己的上下文工具类
        // UserContextHolder.clear();
        // TenantContextHolder.clear();
    }
}
```

## 拦截器注册配置

本节主要介绍如何将自定义拦截器注册到 Spring MVC 中，并配置需要拦截的路径和需要放行的路径。只有完成注册配置后，自定义拦截器才会真正生效。

### 实现 WebMvcConfigurer

Spring Boot 3 中推荐通过实现 `WebMvcConfigurer` 接口来扩展 Spring MVC 配置。注册拦截器时，需要重写 `addInterceptors` 方法。

配置类建议放在 `config` 包下，目录结构如下：

```text
src/main/java/io/github/atengk/config/WebMvcConfig.java
```

下面的配置类用于注册前面创建的 `LoginInterceptor`。

文件位置：`src/main/java/io/github/atengk/config/WebMvcConfig.java`

```java
package io.github.atengk.config;

import io.github.atengk.interceptor.LoginInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC 配置类
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final LoginInterceptor loginInterceptor;

    /**
     * 注册 Spring MVC 拦截器
     *
     * @param registry 拦截器注册器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/login",
                        "/api/auth/logout",
                        "/api/public/**",
                        "/error"
                );
    }

}
```

该配置表示：拦截 `/api/**` 下的所有请求，但放行登录、退出、公开接口和错误处理路径。

### 注册拦截器实例

注册拦截器时，可以通过 `registry.addInterceptor` 添加一个 `HandlerInterceptor` 实例。

如果拦截器类已经使用 `@Component` 交给 Spring 容器管理，推荐通过构造方法注入拦截器实例：

```java
private final LoginInterceptor loginInterceptor;
```

然后在 `addInterceptors` 方法中注册：

```java
@Override
public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(loginInterceptor);
}
```

这种方式适合拦截器中需要注入 Service、RedisTemplate、配置属性或其他 Spring Bean 的场景。

不推荐直接使用 `new LoginInterceptor()` 注册拦截器，原因是手动创建的对象不受 Spring 容器管理，后续如果拦截器中需要注入其他 Bean，可能会出现依赖无法注入的问题。

不推荐写法：

```java
@Override
public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(new LoginInterceptor());
}
```

推荐写法：

```java
@Override
public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(loginInterceptor);
}
```

如果有多个拦截器，也可以继续追加注册：

```java
@Override
public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(loginInterceptor)
            .addPathPatterns("/api/**")
            .excludePathPatterns("/api/auth/login");

    registry.addInterceptor(operationLogInterceptor)
            .addPathPatterns("/api/**");
}
```

多个拦截器的执行顺序与注册顺序有关。先注册的拦截器，其 `preHandle` 会先执行；请求完成时，`afterCompletion` 通常按相反顺序执行。

### 配置拦截路径

拦截路径通过 `addPathPatterns` 方法配置。该方法用于指定当前拦截器需要拦截哪些请求路径。

常见路径配置如下：

| 配置        | 说明                         |
| ----------- | ---------------------------- |
| `/**`       | 拦截所有请求                 |
| `/api/**`   | 拦截 `/api/` 下的所有请求    |
| `/admin/**` | 拦截后台管理接口             |
| `/user/*`   | 拦截 `/user/` 下一级路径     |
| `/user/**`  | 拦截 `/user/` 下所有层级路径 |

示例：拦截所有接口请求。

```java
@Override
public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(loginInterceptor)
            .addPathPatterns("/**");
}
```

示例：只拦截业务接口，不拦截静态资源和系统默认路径。

```java
@Override
public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(loginInterceptor)
            .addPathPatterns("/api/**");
}
```

示例：同时拦截用户接口和后台接口。

```java
@Override
public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(loginInterceptor)
            .addPathPatterns("/api/user/**", "/api/admin/**");
}
```

实际项目中更推荐将业务接口统一放在 `/api` 前缀下，然后拦截 `/api/**`。这样可以避免误拦截静态资源、健康检查接口、Swagger 文档接口等非业务请求。

### 配置排除路径

排除路径通过 `excludePathPatterns` 方法配置。该方法用于指定当前拦截器不需要拦截的路径。

常见需要排除的路径包括登录接口、注册接口、验证码接口、公开接口、错误处理路径、接口文档路径和静态资源路径。

示例配置如下：

```java
@Override
public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(loginInterceptor)
            .addPathPatterns("/api/**")
            .excludePathPatterns(
                    "/api/auth/login",
                    "/api/auth/register",
                    "/api/captcha",
                    "/api/public/**",
                    "/error"
            );
}
```

如果项目集成了 Swagger 或 Knife4j，也需要根据实际访问路径放行接口文档资源。

示例：

```java
@Override
public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(loginInterceptor)
            .addPathPatterns("/api/**")
            .excludePathPatterns(
                    "/api/auth/login",
                    "/api/auth/register",
                    "/api/public/**",
                    "/error",

                    // Swagger / Knife4j 常见放行路径
                    "/doc.html",
                    "/webjars/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/v3/api-docs/**"
            );
}
```

需要注意的是，`excludePathPatterns` 只对当前注册的拦截器生效。如果项目中配置了多个拦截器，每个拦截器都需要根据自身业务独立配置排除路径。

完整配置示例：

文件位置：`src/main/java/io/github/atengk/config/WebMvcConfig.java`

```java
package io.github.atengk.config;

import io.github.atengk.interceptor.LoginInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC 配置类
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final LoginInterceptor loginInterceptor;

    /**
     * 注册 Spring MVC 拦截器
     *
     * @param registry 拦截器注册器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor)
                // 拦截业务接口
                .addPathPatterns("/api/**")
                // 放行登录、注册、验证码、公开接口和接口文档
                .excludePathPatterns(
                        "/api/auth/login",
                        "/api/auth/register",
                        "/api/captcha",
                        "/api/public/**",
                        "/error",
                        "/doc.html",
                        "/webjars/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/**"
                );
    }

}
```

完成上述配置后，启动 Spring Boot 项目，访问 `/api/**` 下的接口时，`LoginInterceptor` 就会自动执行。没有携带合法 `Authorization` 请求头的请求会被拦截，携带 `Bearer ateng-token` 的请求会被放行。

## 常见业务应用

本节主要介绍拦截器在实际业务中的常见用法，包括登录状态校验、请求日志记录和权限前置校验。下面的示例基于前面已经引入的 `spring-boot-starter-web`、Hutool 和 Lombok 依赖，包路径统一使用 `io.github.atengk`。

### 登录状态校验

登录状态校验是拦截器最常见的应用场景。它通常在请求进入 Controller 之前执行，根据请求头、Cookie、Session 或 Token 判断当前请求是否已经登录。

本示例使用请求头 `Authorization` 模拟登录令牌校验。实际项目中可以替换为 JWT、Sa-Token、Redis Token 或统一认证中心校验。

文件位置：`src/main/java/io/github/atengk/context/LoginUserContext.java`

```java
package io.github.atengk.context;

import cn.hutool.core.util.StrUtil;

/**
 * 登录用户上下文
 *
 * @author Ateng
 * @since 2026-05-06
 */
public class LoginUserContext {

    private static final ThreadLocal<String> USER_ID_HOLDER = new ThreadLocal<>();

    /**
     * 设置当前登录用户ID
     *
     * @param userId 用户ID
     */
    public static void setUserId(String userId) {
        USER_ID_HOLDER.set(userId);
    }

    /**
     * 获取当前登录用户ID
     *
     * @return 当前登录用户ID
     */
    public static String getUserId() {
        return USER_ID_HOLDER.get();
    }

    /**
     * 判断当前用户是否已登录
     *
     * @return true 表示已登录，false 表示未登录
     */
    public static boolean isLogin() {
        return StrUtil.isNotBlank(USER_ID_HOLDER.get());
    }

    /**
     * 清理当前线程用户信息
     */
    public static void clear() {
        USER_ID_HOLDER.remove();
    }

}
```

下面的拦截器负责读取请求头中的 Token，并将用户信息写入 `LoginUserContext`。为了便于演示，示例中约定合法 Token 为 `Bearer ateng-token`。

文件位置：`src/main/java/io/github/atengk/interceptor/LoginCheckInterceptor.java`

```java
package io.github.atengk.interceptor;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import io.github.atengk.context.LoginUserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 登录状态校验拦截器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Component
public class LoginCheckInterceptor implements HandlerInterceptor {

    private static final String TOKEN_HEADER = "Authorization";

    /**
     * Controller 方法执行前校验登录状态
     *
     * @param request  当前请求对象
     * @param response 当前响应对象
     * @param handler  当前处理器对象
     * @return true 表示放行，false 表示拦截
     * @throws Exception 请求处理异常
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = request.getHeader(TOKEN_HEADER);
        String requestUri = request.getRequestURI();

        if (StrUtil.isBlank(token)) {
            log.warn("用户未登录，请求地址：{}", requestUri);
            writeErrorResponse(response, HttpStatus.UNAUTHORIZED.value(), "请先登录后再访问");
            return false;
        }

        if (!StrUtil.equals("Bearer ateng-token", token)) {
            log.warn("登录令牌无效，请求地址：{}，令牌：{}", requestUri, token);
            writeErrorResponse(response, HttpStatus.UNAUTHORIZED.value(), "登录状态无效或已过期");
            return false;
        }

        LoginUserContext.setUserId("10001");
        log.info("登录状态校验通过，用户ID：{}，请求地址：{}", LoginUserContext.getUserId(), requestUri);
        return true;
    }

    /**
     * 请求完成后清理用户上下文
     *
     * @param request  当前请求对象
     * @param response 当前响应对象
     * @param handler  当前处理器对象
     * @param ex       请求处理异常
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        LoginUserContext.clear();
        log.debug("登录用户上下文已清理，请求地址：{}", request.getRequestURI());
    }

    /**
     * 写入错误响应
     *
     * @param response 响应对象
     * @param code     状态码
     * @param message  提示信息
     * @throws IOException 响应写入异常
     */
    private void writeErrorResponse(HttpServletResponse response, Integer code, String message) throws IOException {
        response.setStatus(code);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json;charset=UTF-8");

        Map<String, Object> result = new HashMap<>();
        result.put("code", code);
        result.put("message", message);
        result.put("data", null);

        response.getWriter().write(JSONUtil.toJsonStr(result));
    }

}
```

该拦截器的重点是：在 `preHandle` 中完成登录校验，在校验通过后写入当前用户上下文；在 `afterCompletion` 中清理 `ThreadLocal`，避免线程复用导致用户信息污染。

### 请求日志记录

请求日志记录通常用于排查接口问题、统计接口耗时、定位异常请求。它适合记录请求路径、请求方式、客户端 IP、响应状态码和接口耗时。

下面的请求日志拦截器会在请求开始时记录开始时间，在请求完成后输出接口耗时。

文件位置：`src/main/java/io/github/atengk/interceptor/RequestLogInterceptor.java`

```java
package io.github.atengk.interceptor;

import cn.hutool.core.date.SystemClock;
import cn.hutool.core.util.StrUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 请求日志拦截器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Component
public class RequestLogInterceptor implements HandlerInterceptor {

    private static final String START_TIME_ATTRIBUTE = "requestStartTime";

    /**
     * 请求开始时记录基础信息
     *
     * @param request  当前请求对象
     * @param response 当前响应对象
     * @param handler  当前处理器对象
     * @return true 表示继续执行
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(START_TIME_ATTRIBUTE, SystemClock.now());

        log.info("接口请求开始，请求方式：{}，请求地址：{}，客户端IP：{}",
                request.getMethod(),
                request.getRequestURI(),
                getClientIp(request));

        return true;
    }

    /**
     * 请求完成后记录耗时和响应状态
     *
     * @param request  当前请求对象
     * @param response 当前响应对象
     * @param handler  当前处理器对象
     * @param ex       请求处理异常
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        Object startTimeObj = request.getAttribute(START_TIME_ATTRIBUTE);
        long startTime = startTimeObj instanceof Long ? (Long) startTimeObj : SystemClock.now();
        long costTime = SystemClock.now() - startTime;

        if (ex != null) {
            log.error("接口请求异常，请求方式：{}，请求地址：{}，响应状态：{}，耗时：{}ms",
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    costTime,
                    ex);
            return;
        }

        log.info("接口请求完成，请求方式：{}，请求地址：{}，响应状态：{}，耗时：{}ms",
                request.getMethod(),
                request.getRequestURI(),
                response.getStatus(),
                costTime);
    }

    /**
     * 获取客户端真实IP
     *
     * @param request 当前请求对象
     * @return 客户端IP
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (StrUtil.isNotBlank(xForwardedFor)) {
            return StrUtil.subBefore(xForwardedFor, ",", false);
        }

        String realIp = request.getHeader("X-Real-IP");
        if (StrUtil.isNotBlank(realIp)) {
            return realIp;
        }

        return request.getRemoteAddr();
    }

}
```

请求日志拦截器一般建议注册到较大的路径范围，例如 `/api/**`。如果项目中接口较多，可以进一步配合日志链路 ID、用户 ID、租户 ID 等上下文信息一起输出，方便后续排查问题。

### 权限前置校验

权限前置校验通常用于判断当前登录用户是否拥有访问某个接口的权限。常见实现方式是在 Controller 方法上添加权限注解，然后在拦截器中读取注解并进行校验。

下面先定义一个权限注解，用于标记接口所需权限。

文件位置：`src/main/java/io/github/atengk/annotation/RequirePermission.java`

```java
package io.github.atengk.annotation;

import java.lang.annotation.*;

/**
 * 权限校验注解
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequirePermission {

    /**
     * 权限标识
     *
     * @return 权限标识
     */
    String value();

}
```

下面的拦截器会读取 Controller 方法上的 `@RequirePermission` 注解，并判断当前用户是否拥有指定权限。

文件位置：`src/main/java/io/github/atengk/interceptor/PermissionInterceptor.java`

```java
package io.github.atengk.interceptor;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import io.github.atengk.annotation.RequirePermission;
import io.github.atengk.context.LoginUserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 权限前置校验拦截器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Component
public class PermissionInterceptor implements HandlerInterceptor {

    /**
     * Controller 方法执行前校验接口权限
     *
     * @param request  当前请求对象
     * @param response 当前响应对象
     * @param handler  当前处理器对象
     * @return true 表示放行，false 表示拦截
     * @throws Exception 请求处理异常
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        RequirePermission requirePermission = handlerMethod.getMethodAnnotation(RequirePermission.class);
        if (requirePermission == null) {
            return true;
        }

        String permissionCode = requirePermission.value();
        String userId = LoginUserContext.getUserId();

        List<String> userPermissions = getUserPermissions(userId);
        if (CollUtil.contains(userPermissions, permissionCode)) {
            log.info("权限校验通过，用户ID：{}，权限标识：{}，请求地址：{}", userId, permissionCode, request.getRequestURI());
            return true;
        }

        log.warn("权限校验失败，用户ID：{}，权限标识：{}，请求地址：{}", userId, permissionCode, request.getRequestURI());
        writeErrorResponse(response, HttpStatus.FORBIDDEN.value(), "暂无接口访问权限");
        return false;
    }

    /**
     * 获取用户权限列表
     *
     * @param userId 用户ID
     * @return 权限标识列表
     */
    private List<String> getUserPermissions(String userId) {
        // 示例写法：实际项目中可以从数据库、Redis、权限中心或 Sa-Token 中读取
        return List.of("user:query", "user:add", "order:query");
    }

    /**
     * 写入错误响应
     *
     * @param response 响应对象
     * @param code     状态码
     * @param message  提示信息
     * @throws IOException 响应写入异常
     */
    private void writeErrorResponse(HttpServletResponse response, Integer code, String message) throws IOException {
        response.setStatus(code);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json;charset=UTF-8");

        Map<String, Object> result = new HashMap<>();
        result.put("code", code);
        result.put("message", message);
        result.put("data", null);

        response.getWriter().write(JSONUtil.toJsonStr(result));
    }

}
```

权限拦截器依赖登录用户上下文，因此它应该在登录校验拦截器之后执行。也就是说，请求需要先通过登录状态校验，再执行权限校验。

拦截器注册顺序示例：

文件位置：`src/main/java/io/github/atengk/config/WebMvcConfig.java`

```java
package io.github.atengk.config;

import io.github.atengk.interceptor.LoginCheckInterceptor;
import io.github.atengk.interceptor.PermissionInterceptor;
import io.github.atengk.interceptor.RequestLogInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC 配置类
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final RequestLogInterceptor requestLogInterceptor;
    private final LoginCheckInterceptor loginCheckInterceptor;
    private final PermissionInterceptor permissionInterceptor;

    /**
     * 注册 Spring MVC 拦截器
     *
     * @param registry 拦截器注册器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(requestLogInterceptor)
                .addPathPatterns("/api/**");

        registry.addInterceptor(loginCheckInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/login",
                        "/api/public/**",
                        "/error"
                );

        registry.addInterceptor(permissionInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/login",
                        "/api/public/**",
                        "/error"
                );
    }

}
```

这里的执行顺序是：请求日志拦截器先记录请求开始信息，然后登录校验拦截器判断用户是否登录，最后权限拦截器判断用户是否具备接口权限。

## 接口测试与验证

本节主要提供用于验证拦截器效果的测试接口，并通过 `curl` 命令验证拦截路径和排除路径是否符合预期。测试重点包括：未登录请求是否被拦截、合法登录请求是否放行、无权限请求是否返回 `403`、排除路径是否不进入登录校验。

### 编写测试接口

测试接口建议统一放在 `/api` 路径下，方便被拦截器统一拦截。下面提供三个接口：登录接口、公开接口和用户查询接口。

文件位置：`src/main/java/io/github/atengk/controller/TestController.java`

```java
package io.github.atengk.controller;

import cn.hutool.core.map.MapUtil;
import io.github.atengk.annotation.RequirePermission;
import io.github.atengk.context.LoginUserContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 拦截器测试接口
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@RestController
@RequestMapping("/api")
public class TestController {

    /**
     * 登录接口
     *
     * @return 登录令牌
     */
    @PostMapping("/auth/login")
    public Map<String, Object> login() {
        log.info("用户登录成功，返回测试令牌");
        return MapUtil.<String, Object>builder()
                .put("code", 200)
                .put("message", "登录成功")
                .put("token", "Bearer ateng-token")
                .build();
    }

    /**
     * 公开接口
     *
     * @return 公开接口响应
     */
    @GetMapping("/public/hello")
    public Map<String, Object> publicHello() {
        return MapUtil.<String, Object>builder()
                .put("code", 200)
                .put("message", "公开接口访问成功")
                .build();
    }

    /**
     * 查询当前用户信息
     *
     * @return 当前用户信息
     */
    @RequirePermission("user:query")
    @GetMapping("/user/info")
    public Map<String, Object> userInfo() {
        String userId = LoginUserContext.getUserId();

        return MapUtil.<String, Object>builder()
                .put("code", 200)
                .put("message", "查询成功")
                .put("userId", userId)
                .build();
    }

    /**
     * 删除用户接口
     *
     * @return 删除结果
     */
    @RequirePermission("user:delete")
    @DeleteMapping("/user/delete")
    public Map<String, Object> deleteUser() {
        return MapUtil.<String, Object>builder()
                .put("code", 200)
                .put("message", "删除成功")
                .build();
    }

}
```

上面的接口说明如下：

| 接口                | 请求方式 | 是否需要登录 | 是否需要权限 | 说明                     |
| ------------------- | -------- | ------------ | ------------ | ------------------------ |
| `/api/auth/login`   | POST     | 否           | 否           | 登录接口，已配置排除路径 |
| `/api/public/hello` | GET      | 否           | 否           | 公开接口，已配置排除路径 |
| `/api/user/info`    | GET      | 是           | 是           | 需要 `user:query` 权限   |
| `/api/user/delete`  | DELETE   | 是           | 是           | 需要 `user:delete` 权限  |

### 验证拦截路径

拦截路径用于验证 `/api/**` 下的受保护接口是否会被拦截。根据前面的配置，`/api/user/info` 和 `/api/user/delete` 都会进入登录校验和权限校验。

先启动 Spring Boot 项目：

```bash
mvn spring-boot:run
```

该命令会使用 Maven 启动当前 Spring Boot 项目。默认情况下，项目会运行在 `8080` 端口；如果你在 `application.yml` 中修改了端口，需要将下面命令中的 `8080` 替换为实际端口。

验证未携带 Token 访问受保护接口：

```bash
curl -i -X GET http://localhost:8080/api/user/info
```

预期响应结果如下：

```json
{
  "code": 401,
  "message": "请先登录后再访问",
  "data": null
}
```

验证携带错误 Token 访问受保护接口：

```bash
curl -i -X GET http://localhost:8080/api/user/info \
  -H "Authorization: Bearer error-token"
```

预期响应结果如下：

```json
{
  "code": 401,
  "message": "登录状态无效或已过期",
  "data": null
}
```

验证携带正确 Token 访问有权限接口：

```bash
curl -i -X GET http://localhost:8080/api/user/info \
  -H "Authorization: Bearer ateng-token"
```

预期响应结果如下：

```json
{
  "code": 200,
  "message": "查询成功",
  "userId": "10001"
}
```

验证携带正确 Token 访问无权限接口：

```bash
curl -i -X DELETE http://localhost:8080/api/user/delete \
  -H "Authorization: Bearer ateng-token"
```

预期响应结果如下：

```json
{
  "code": 403,
  "message": "暂无接口访问权限",
  "data": null
}
```

因为示例中的用户权限列表只包含 `user:query`、`user:add`、`order:query`，不包含 `user:delete`，所以访问删除接口时会被权限拦截器拦截。

### 验证排除路径

排除路径用于验证登录接口、公开接口等是否可以在不携带 Token 的情况下正常访问。根据前面的配置，`/api/auth/login` 和 `/api/public/**` 不会进入登录校验。

验证登录接口：

```bash
curl -i -X POST http://localhost:8080/api/auth/login
```

预期响应结果如下：

```json
{
  "code": 200,
  "message": "登录成功",
  "token": "Bearer ateng-token"
}
```

验证公开接口：

```bash
curl -i -X GET http://localhost:8080/api/public/hello
```

预期响应结果如下：

```json
{
  "code": 200,
  "message": "公开接口访问成功"
}
```

如果以上两个接口在不携带 `Authorization` 请求头的情况下可以正常访问，说明排除路径配置已经生效。

可以通过控制台日志进一步确认拦截器执行情况。访问 `/api/user/info` 时，正常情况下会看到请求日志、登录校验日志和权限校验日志；访问 `/api/auth/login` 或 `/api/public/hello` 时，不会触发登录校验和权限校验，但仍可能触发请求日志拦截器，具体取决于请求日志拦截器的注册路径。

## 开发注意事项

本节主要说明 Spring Boot 拦截器开发中容易踩坑的部分，包括多个拦截器的执行顺序、异常处理方式，以及静态资源和接口文档路径的放行配置。合理处理这些细节，可以避免接口误拦截、异常响应不统一、静态资源无法访问等问题。

### 执行顺序

当项目中注册多个拦截器时，拦截器的执行顺序与注册顺序有关。先注册的拦截器，其 `preHandle` 方法会先执行；请求完成后，`postHandle` 和 `afterCompletion` 通常按照相反顺序执行。

假设项目中有三个拦截器：

1. `RequestLogInterceptor`：请求日志拦截器
2. `LoginCheckInterceptor`：登录校验拦截器
3. `PermissionInterceptor`：权限校验拦截器

推荐注册顺序如下：

```java
@Override
public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(requestLogInterceptor)
            .addPathPatterns("/api/**");

    registry.addInterceptor(loginCheckInterceptor)
            .addPathPatterns("/api/**")
            .excludePathPatterns(
                    "/api/auth/login",
                    "/api/public/**",
                    "/error"
            );

    registry.addInterceptor(permissionInterceptor)
            .addPathPatterns("/api/**")
            .excludePathPatterns(
                    "/api/auth/login",
                    "/api/public/**",
                    "/error"
            );
}
```

按照上面的注册顺序，请求进入时的执行顺序如下：

```text
RequestLogInterceptor.preHandle
  -> LoginCheckInterceptor.preHandle
    -> PermissionInterceptor.preHandle
      -> Controller
```

请求完成后的执行顺序如下：

```text
PermissionInterceptor.afterCompletion
  -> LoginCheckInterceptor.afterCompletion
    -> RequestLogInterceptor.afterCompletion
```

如果某个拦截器的 `preHandle` 返回 `false`，后续拦截器和 Controller 都不会继续执行。例如登录校验失败时，权限校验拦截器不会执行，Controller 方法也不会执行。

```text
RequestLogInterceptor.preHandle
  -> LoginCheckInterceptor.preHandle 返回 false
    -> 请求被拦截
```

开发时需要特别注意依赖关系。权限校验通常依赖登录用户信息，因此登录校验拦截器必须在权限校验拦截器之前注册。请求日志拦截器通常希望记录完整请求链路，因此可以放在最前面注册。

如果希望明确控制执行顺序，也可以通过拆分配置或保持统一注册入口来避免顺序混乱。实际项目中建议将拦截器统一放在一个 `WebMvcConfig` 中注册，减少多人协作时顺序被误改的风险。

### 异常处理

拦截器中可以直接向 `HttpServletResponse` 写入错误响应，也可以抛出业务异常交给全局异常处理器统一处理。两种方式都可以使用，但在同一个项目中建议保持风格一致。

如果是登录失败、权限不足这类明确的前置拦截场景，可以直接在拦截器中写入响应并返回 `false`：

```java
if (StrUtil.isBlank(token)) {
    writeErrorResponse(response, HttpStatus.UNAUTHORIZED.value(), "请先登录后再访问");
    return false;
}
```

这种方式的优点是逻辑直接，缺点是多个拦截器中可能重复编写响应结构。

更推荐的方式是定义统一业务异常，在拦截器中抛出异常，然后由全局异常处理器统一返回 JSON 响应。这样可以保证 Controller、Service、Interceptor 的异常响应格式一致。

文件位置：`src/main/java/io/github/atengk/exception/BizException.java`

```java
package io.github.atengk.exception;

import lombok.Getter;

/**
 * 业务异常
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Getter
public class BizException extends RuntimeException {

    private final Integer code;

    /**
     * 创建业务异常
     *
     * @param code    状态码
     * @param message 异常信息
     */
    public BizException(Integer code, String message) {
        super(message);
        this.code = code;
    }

}
```

文件位置：`src/main/java/io/github/atengk/handler/GlobalExceptionHandler.java`

```java
package io.github.atengk.handler;

import cn.hutool.core.map.MapUtil;
import io.github.atengk.exception.BizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * 全局异常处理器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常
     *
     * @param ex 业务异常
     * @return 统一响应结果
     */
    @ExceptionHandler(BizException.class)
    public Map<String, Object> handleBizException(BizException ex) {
        log.warn("业务异常：{}", ex.getMessage());

        return MapUtil.<String, Object>builder()
                .put("code", ex.getCode())
                .put("message", ex.getMessage())
                .put("data", null)
                .build();
    }

    /**
     * 处理系统异常
     *
     * @param ex 系统异常
     * @return 统一响应结果
     */
    @ExceptionHandler(Exception.class)
    public Map<String, Object> handleException(Exception ex) {
        log.error("系统异常", ex);

        return MapUtil.<String, Object>builder()
                .put("code", HttpStatus.INTERNAL_SERVER_ERROR.value())
                .put("message", "系统异常，请稍后重试")
                .put("data", null)
                .build();
    }

}
```

拦截器中可以直接抛出业务异常：

```java
@Override
public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
    String token = request.getHeader("Authorization");

    if (StrUtil.isBlank(token)) {
        log.warn("请求缺少登录令牌，请求地址：{}", request.getRequestURI());
        throw new BizException(HttpStatus.UNAUTHORIZED.value(), "请先登录后再访问");
    }

    return true;
}
```

需要注意的是，如果拦截器已经手动写入了响应内容，就不要再继续抛异常，否则可能出现响应重复写入的问题。也就是说，下面两种方式二选一即可：

```java
// 方式一：手动写入响应，然后 return false
writeErrorResponse(response, 401, "请先登录后再访问");
return false;
// 方式二：抛出异常，交给全局异常处理器
throw new BizException(401, "请先登录后再访问");
```

在实际项目中，推荐使用“抛出业务异常 + 全局异常处理器”的方式，因为它可以保证接口响应格式统一，也方便后续扩展错误码、国际化提示和异常日志策略。

### 静态资源放行

如果拦截器配置了 `/**` 或较大的路径范围，可能会误拦截静态资源、接口文档、错误处理路径等请求，导致页面资源加载失败、Swagger 无法打开、Knife4j 文档异常、错误页面再次进入拦截器等问题。

常见需要放行的路径包括：

| 路径               | 说明                         |
| ------------------ | ---------------------------- |
| `/error`           | Spring Boot 默认错误处理路径 |
| `/favicon.ico`     | 浏览器默认站点图标           |
| `/static/**`       | 静态资源目录                 |
| `/public/**`       | 公开资源目录                 |
| `/webjars/**`      | WebJar 静态资源              |
| `/swagger-ui/**`   | Swagger UI 资源              |
| `/swagger-ui.html` | Swagger 旧版入口             |
| `/v3/api-docs/**`  | OpenAPI 3 接口文档           |
| `/doc.html`        | Knife4j 文档入口             |

如果业务接口统一以 `/api` 开头，最推荐的方式是只拦截 `/api/**`，这样可以天然避开大部分静态资源路径：

```java
@Override
public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(loginCheckInterceptor)
            .addPathPatterns("/api/**")
            .excludePathPatterns(
                    "/api/auth/login",
                    "/api/public/**",
                    "/error"
            );
}
```

如果项目必须拦截 `/**`，则需要显式排除静态资源和接口文档路径。

文件位置：`src/main/java/io/github/atengk/config/WebMvcConfig.java`

```java
package io.github.atengk.config;

import io.github.atengk.interceptor.LoginCheckInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC 配置类
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final LoginCheckInterceptor loginCheckInterceptor;

    /**
     * 注册 Spring MVC 拦截器
     *
     * @param registry 拦截器注册器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginCheckInterceptor)
                // 拦截全部请求时，必须注意静态资源和文档路径放行
                .addPathPatterns("/**")
                .excludePathPatterns(
                        // 登录和公开业务接口
                        "/api/auth/login",
                        "/api/auth/register",
                        "/api/public/**",

                        // Spring Boot 默认错误路径
                        "/error",

                        // 浏览器默认图标
                        "/favicon.ico",

                        // 静态资源路径
                        "/static/**",
                        "/public/**",
                        "/resources/**",
                        "/META-INF/resources/**",

                        // Swagger / Knife4j 文档路径
                        "/doc.html",
                        "/webjars/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/**"
                );
    }

}
```

如果项目中配置了静态资源映射，也需要保证拦截器放行路径与资源映射路径一致。

例如，配置本地文件访问路径：

文件位置：`src/main/java/io/github/atengk/config/StaticResourceConfig.java`

```java
package io.github.atengk.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 静态资源配置类
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    /**
     * 配置静态资源映射
     *
     * @param registry 静态资源注册器
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/files/**")
                // 示例路径：实际项目中建议改为配置文件参数
                .addResourceLocations("file:/data/upload/");
    }

}
```

此时拦截器中也应该放行 `/files/**`：

```java
@Override
public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(loginCheckInterceptor)
            .addPathPatterns("/**")
            .excludePathPatterns(
                    "/api/auth/login",
                    "/api/public/**",
                    "/files/**",
                    "/error",
                    "/favicon.ico"
            );
}
```

静态资源放行的核心原则是：业务接口和资源接口要有清晰的路径边界。推荐将业务接口统一放在 `/api/**` 下，将静态资源统一放在 `/static/**`、`/files/**` 或对象存储访问域名下，避免拦截器规则过宽导致误拦截。