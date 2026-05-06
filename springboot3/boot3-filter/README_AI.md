# Spring Boot 过滤器



## 过滤器概述

过滤器是 Servlet 规范中的核心组件，位于请求进入 Controller 之前、响应返回客户端之前。它可以对 HTTP 请求和响应进行统一处理，是 Web 应用中处理通用逻辑的重要入口。

在 Spring Boot 3 中，过滤器依然基于 Servlet Filter 机制实现，对应接口为 `jakarta.servlet.Filter`。由于 Spring Boot 3 已从 `javax.*` 迁移到 `jakarta.*`，开发过滤器时需要使用 `jakarta.servlet` 包下的相关类。

### 过滤器的作用

过滤器主要用于在请求到达业务接口之前进行预处理，或者在响应返回客户端之前进行后置处理。它不直接负责业务逻辑，而是用于处理横切类功能。

常见作用包括：

| 作用       | 说明                                                       |
| ---------- | ---------------------------------------------------------- |
| 请求预处理 | 在 Controller 执行前检查请求参数、请求头、Token、IP 等信息 |
| 响应后处理 | 在接口返回后追加响应头、记录响应状态、统计耗时             |
| 统一拦截   | 对指定 URL 路径进行统一控制                                |
| 安全校验   | 进行登录状态、Token、签名、来源校验                        |
| 日志记录   | 记录请求路径、请求方式、客户端 IP、执行耗时                |
| 请求包装   | 包装 `HttpServletRequest`，解决请求体只能读取一次的问题    |

过滤器的核心方法是 `doFilter`。在该方法中，可以通过 `FilterChain` 决定请求是否继续向后执行。

```java
filterChain.doFilter(request, response);
```

如果调用该方法，请求会继续进入后续过滤器或最终进入 Controller。

如果不调用该方法，请求会被当前过滤器中断，通常用于认证失败、非法请求、权限不足等场景。

### 典型使用场景

过滤器适合处理与具体业务接口无关、但多个接口都需要统一执行的通用逻辑。

常见场景如下：

| 场景         | 说明                                         |
| ------------ | -------------------------------------------- |
| 请求日志记录 | 记录请求地址、请求方式、请求 IP、接口耗时    |
| 登录认证     | 判断请求是否携带有效 Token                   |
| 接口签名校验 | 校验请求参数签名，防止参数被篡改             |
| 跨域处理     | 在响应中添加 CORS 相关响应头                 |
| 黑白名单控制 | 根据客户端 IP、请求来源、User-Agent 进行限制 |
| 请求体缓存   | 包装请求对象，使 JSON 请求体可以被重复读取   |
| 字符编码处理 | 统一设置请求和响应字符编码                   |
| 链路追踪     | 设置 traceId，便于日志链路排查               |

例如，在 Token 校验场景中，过滤器可以先读取请求头中的 `Authorization`，判断 Token 是否存在、是否有效。如果校验通过，则放行请求；如果校验失败，则直接返回 `401 Unauthorized`。

过滤器更适合处理靠近 Web 容器层面的逻辑。如果逻辑依赖 Spring MVC 的 Handler、Controller 方法、方法参数或注解信息，通常更适合使用拦截器。

### 过滤器与拦截器的区别

过滤器和拦截器都可以在请求进入 Controller 前后执行逻辑，但二者所属层级、执行时机和适用场景不同。

| 对比项                   | 过滤器 Filter                    | 拦截器 Interceptor                     |
| ------------------------ | -------------------------------- | -------------------------------------- |
| 所属规范                 | Servlet 规范                     | Spring MVC 机制                        |
| 核心接口                 | `jakarta.servlet.Filter`         | `HandlerInterceptor`                   |
| 执行位置                 | Servlet 容器层                   | Spring MVC 层                          |
| 执行时机                 | 请求进入 DispatcherServlet 前后  | 请求匹配到 Controller 方法前后         |
| 作用范围                 | 可处理所有 Servlet 请求          | 主要处理 Spring MVC 请求               |
| 是否依赖 Spring MVC      | 不依赖                           | 依赖                                   |
| 获取 Controller 方法信息 | 不方便                           | 可以获取 `HandlerMethod`               |
| 常见用途                 | 编码、CORS、日志、认证、请求包装 | 权限校验、接口限流、注解解析、业务拦截 |

执行顺序可以理解为：

```text
客户端请求
  ↓
Filter 过滤器
  ↓
DispatcherServlet
  ↓
Interceptor 拦截器
  ↓
Controller
  ↓
Interceptor 拦截器
  ↓
Filter 过滤器
  ↓
客户端响应
```

开发时可以按以下原则选择：

| 需求                           | 推荐方式    |
| ------------------------------ | ----------- |
| 处理原始请求和响应             | Filter      |
| 统一设置响应头                 | Filter      |
| 包装请求体                     | Filter      |
| 登录 Token 初步校验            | Filter      |
| 基于 Controller 注解做权限控制 | Interceptor |
| 获取 Controller 方法信息       | Interceptor |
| 处理 Spring MVC 接口调用链     | Interceptor |

简单来说，过滤器更靠近 Servlet 容器，适合做底层通用处理；拦截器更靠近 Spring MVC，适合做与接口方法相关的业务拦截。

## 开发环境准备

本节用于说明开发 Spring Boot 3 过滤器所需的基础环境、Maven 依赖和推荐项目结构。后续编写过滤器、注册过滤器和验证过滤器执行效果，都基于这里的项目配置展开。

### Spring Boot 3 基础环境

Spring Boot 3 过滤器开发需要准备以下基础环境：

| 环境项       | 推荐配置                  |
| ------------ | ------------------------- |
| JDK          | JDK 17 或更高版本         |
| Spring Boot  | Spring Boot 3.x           |
| 构建工具     | Maven 3.8+                |
| Web 容器     | Spring Boot 内置 Tomcat   |
| 开发工具     | IntelliJ IDEA、VS Code 等 |
| 接口测试工具 | curl、Postman、Apifox     |

Spring Boot 3 使用 Jakarta EE 相关 API，因此过滤器相关类需要从 `jakarta.servlet` 包中导入。

```java
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
```

需要注意，Spring Boot 2 中常见的 `javax.servlet.Filter` 在 Spring Boot 3 项目中不再推荐使用。如果项目升级到 Spring Boot 3，需要同步修改相关导包。

### Maven 依赖配置

过滤器属于 Web 应用开发能力，通常只需要引入 `spring-boot-starter-web` 即可。该依赖会间接引入内置 Tomcat 和 Servlet API，不需要单独引入 `jakarta.servlet-api`。

文件位置：`pom.xml`

```xml
<dependencies>
    <!-- Spring Boot Web 依赖：提供 MVC、内置 Tomcat、Servlet Filter 等 Web 开发能力 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Hutool 工具类：用于字符串、JSON、日期、集合等常用工具处理 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.8.35</version>
    </dependency>

    <!-- Lombok：减少 Getter、Setter、日志对象等样板代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- Spring Boot Test：用于单元测试和接口测试 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

如果项目使用 Spring Boot 官方父工程，建议在 `pom.xml` 中统一管理版本。

文件位置：`pom.xml`

```xml
<parent>
    <!-- Spring Boot 父工程：统一管理 Spring Boot 相关依赖版本 -->
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.3.5</version>
    <relativePath/>
</parent>

<properties>
    <!-- Spring Boot 3 要求 JDK 17 或更高版本 -->
    <java.version>17</java.version>
</properties>
```

如果项目已经由公司脚手架或父工程统一管理版本，则不需要重复声明父工程，只需要确认已引入 `spring-boot-starter-web`。

### 示例项目结构

过滤器通常放在 `filter`、`config` 或 `web` 包下。为了便于维护，建议将过滤器实现类、注册配置类、测试接口分开存放。

推荐项目结构如下：

```text
springboot-filter-demo
├── pom.xml
└── src
    ├── main
    │   ├── java
    │   │   └── io
    │   │       └── github
    │   │           └── atengk
    │   │               ├── FilterApplication.java
    │   │               ├── config
    │   │               │   └── FilterConfig.java
    │   │               ├── controller
    │   │               │   └── TestController.java
    │   │               └── filter
    │   │                   ├── RequestLogFilter.java
    │   │                   ├── TokenCheckFilter.java
    │   │                   └── CorsHeaderFilter.java
    │   └── resources
    │       └── application.yml
    └── test
        └── java
            └── io
                └── github
                    └── atengk
                        └── FilterApplicationTests.java
```

各目录职责如下：

| 路径                        | 说明                                     |
| --------------------------- | ---------------------------------------- |
| `filter`                    | 存放具体过滤器实现类                     |
| `config`                    | 存放过滤器注册、执行顺序、拦截路径等配置 |
| `controller`                | 存放用于验证过滤器效果的测试接口         |
| `resources/application.yml` | 存放服务端口、日志级别、自定义过滤器配置 |
| `test`                      | 存放单元测试或集成测试代码               |

建议按照过滤器职责拆分类名。例如，请求日志过滤器使用 `RequestLogFilter`，Token 校验过滤器使用 `TokenCheckFilter`，跨域响应头过滤器使用 `CorsHeaderFilter`。这样可以避免所有逻辑堆叠在一个过滤器中，后续排查执行顺序和业务问题也更清晰。



## 过滤器核心开发

本节用于说明过滤器的核心实现方式，包括如何实现 `Filter` 接口、如何编写请求处理逻辑，以及如何控制请求放行和响应返回。示例基于 Spring Boot 3，过滤器相关类统一使用 `jakarta.servlet` 包。

### 实现 Filter 接口

在 Spring Boot 3 中，自定义过滤器可以直接实现 `jakarta.servlet.Filter` 接口。核心方法是 `doFilter`，每次请求进入过滤器链时都会执行该方法。

过滤器常用方法如下：

| 方法       | 说明                                     |
| ---------- | ---------------------------------------- |
| `init`     | 过滤器初始化时执行，通常可省略           |
| `doFilter` | 每次请求进入过滤器链时执行，核心处理方法 |
| `destroy`  | 过滤器销毁时执行，通常可省略             |

最小可用过滤器示例如下。

文件位置：`src/main/java/io/github/atengk/filter/SimpleFilter.java`

```java
package io.github.atengk.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * 简单过滤器示例
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
public class SimpleFilter implements Filter {

    /**
     * 执行过滤器逻辑
     *
     * @param request 请求对象
     * @param response 响应对象
     * @param chain 过滤器链
     * @throws IOException IO 异常
     * @throws ServletException Servlet 异常
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        log.info("进入简单过滤器");

        // 放行请求，继续执行后续过滤器或 Controller
        chain.doFilter(request, response);

        log.info("简单过滤器执行结束");
    }

}
```

该过滤器只是演示基本结构。真正开发时，通常需要将 `ServletRequest` 和 `ServletResponse` 转换为 `HttpServletRequest` 和 `HttpServletResponse`，以便获取请求路径、请求头、请求方法和响应状态码。

### 编写过滤器处理逻辑

过滤器处理逻辑通常包括请求前处理、请求放行、请求后处理三部分。请求前处理用于校验、记录或包装请求；请求放行用于进入后续链路；请求后处理用于记录响应状态、接口耗时或补充响应头。

典型流程如下：

```text
请求进入过滤器
  ↓
请求前处理
  ↓
是否允许放行
  ↓
允许：chain.doFilter(request, response)
  ↓
请求后处理
  ↓
响应返回客户端
```

下面是一个请求日志过滤器示例，用于记录请求方法、请求路径、客户端 IP、响应状态和接口耗时。

文件位置：`src/main/java/io/github/atengk/filter/RequestLogFilter.java`

```java
package io.github.atengk.filter;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.util.StrUtil;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * 请求日志过滤器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
public class RequestLogFilter implements Filter {

    /**
     * 执行请求日志记录
     *
     * @param request 请求对象
     * @param response 响应对象
     * @param chain 过滤器链
     * @throws IOException IO 异常
     * @throws ServletException Servlet 异常
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        TimeInterval timer = DateUtil.timer();
        String method = httpRequest.getMethod();
        String uri = httpRequest.getRequestURI();
        String clientIp = this.getClientIp(httpRequest);

        log.info("请求开始，method={}，uri={}，clientIp={}", method, uri, clientIp);

        try {
            chain.doFilter(request, response);
        } finally {
            long cost = timer.interval();
            int status = httpResponse.getStatus();
            log.info("请求结束，method={}，uri={}，status={}，cost={}ms", method, uri, status, cost);
        }
    }

    /**
     * 获取客户端 IP
     *
     * @param request 请求对象
     * @return 客户端 IP
     */
    private String getClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StrUtil.isNotBlank(forwardedFor)) {
            return StrUtil.splitTrim(forwardedFor, ',').get(0);
        }

        String realIp = request.getHeader("X-Real-IP");
        if (StrUtil.isNotBlank(realIp)) {
            return realIp;
        }

        return request.getRemoteAddr();
    }

}
```

该示例使用 `try...finally` 包裹 `chain.doFilter`，可以保证 Controller 正常返回或抛出异常时，都能记录请求结束日志。对于日志、链路追踪、资源清理等场景，建议使用这种结构。

### 请求放行与响应处理

过滤器是否调用 `chain.doFilter(request, response)`，决定请求是否继续向后执行。

如果调用 `chain.doFilter`，请求会进入后续过滤器，最终进入 `DispatcherServlet` 和 Controller。

如果不调用 `chain.doFilter`，请求会在当前过滤器中被中断，后续过滤器和 Controller 不会继续执行。该方式常用于 Token 校验失败、接口签名错误、IP 黑名单拦截等场景。

下面示例演示 Token 校验过滤器。它会读取请求头中的 `Authorization`，如果 Token 为空，则直接返回 JSON 响应；如果 Token 存在，则放行请求。

文件位置：`src/main/java/io/github/atengk/filter/TokenCheckFilter.java`

```java
package io.github.atengk.filter;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Token 校验过滤器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
public class TokenCheckFilter implements Filter {

    private static final String AUTHORIZATION_HEADER = "Authorization";

    /**
     * 执行 Token 校验
     *
     * @param request 请求对象
     * @param response 响应对象
     * @param chain 过滤器链
     * @throws IOException IO 异常
     * @throws ServletException Servlet 异常
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String uri = httpRequest.getRequestURI();
        String token = httpRequest.getHeader(AUTHORIZATION_HEADER);

        if (StrUtil.isBlank(token)) {
            log.warn("Token 校验失败，请求未携带认证信息，uri={}", uri);
            this.writeUnauthorizedResponse(httpResponse, "未携带认证信息");
            return;
        }

        log.info("Token 校验通过，uri={}", uri);
        chain.doFilter(request, response);
    }

    /**
     * 写入未认证响应
     *
     * @param response 响应对象
     * @param message 响应消息
     * @throws IOException IO 异常
     */
    private void writeUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json;charset=UTF-8");

        Map<String, Object> result = Map.of(
                "code", HttpServletResponse.SC_UNAUTHORIZED,
                "message", message,
                "success", false
        );

        response.getWriter().write(JSONUtil.toJsonStr(result));
    }

}
```

该过滤器中，认证失败时直接 `return`，不会执行 `chain.doFilter`。这表示请求已经被当前过滤器处理完成，不会进入 Controller。

在实际项目中，Token 校验通常不会只判断是否为空，还会结合 Redis、JWT、Sa-Token 或 Spring Security 校验 Token 是否有效。当前示例只保留过滤器开发重点，便于理解请求放行和响应中断的处理方式。

## 过滤器注册方式

过滤器类创建完成后，需要注册到 Spring Boot 应用中才能生效。常见注册方式有两种：使用 `@Component` 自动注册，或者使用 `FilterRegistrationBean` 手动注册。实际项目中更推荐使用 `FilterRegistrationBean`，因为它可以明确配置过滤器名称、执行顺序和拦截路径。

### 使用 @Component 自动注册

使用 `@Component` 是最简单的注册方式。只要过滤器类被 Spring 扫描到，就会自动注册到 Servlet 容器中。

下面示例将请求日志过滤器直接注册为 Spring Bean。

文件位置：`src/main/java/io/github/atengk/filter/ComponentRequestLogFilter.java`

```java
package io.github.atengk.filter;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Component 自动注册请求日志过滤器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Component
public class ComponentRequestLogFilter implements Filter {

    /**
     * 记录请求日志
     *
     * @param request 请求对象
     * @param response 响应对象
     * @param chain 过滤器链
     * @throws IOException IO 异常
     * @throws ServletException Servlet 异常
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        TimeInterval timer = DateUtil.timer();

        log.info("自动注册过滤器，请求进入，method={}，uri={}",
                httpRequest.getMethod(), httpRequest.getRequestURI());

        chain.doFilter(request, response);

        log.info("自动注册过滤器，请求结束，uri={}，cost={}ms",
                httpRequest.getRequestURI(), timer.interval());
    }

}
```

这种方式配置简单，但不方便精确控制拦截路径。默认情况下，过滤器会作用于所有请求路径。

如果只需要快速添加一个全局过滤器，使用 `@Component` 即可。如果需要控制顺序、URL 匹配规则、过滤器名称或启停状态，建议使用 `FilterRegistrationBean`。

### 使用 FilterRegistrationBean 注册

`FilterRegistrationBean` 是 Spring Boot 推荐的手动注册方式。它可以清晰配置过滤器实例、过滤器名称、拦截路径和执行顺序。

下面示例注册前面定义的 `RequestLogFilter` 和 `TokenCheckFilter`。

文件位置：`src/main/java/io/github/atengk/config/FilterConfig.java`

```java
package io.github.atengk.config;

import io.github.atengk.filter.RequestLogFilter;
import io.github.atengk.filter.TokenCheckFilter;
import jakarta.servlet.Filter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 过滤器配置
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Configuration
public class FilterConfig {

    /**
     * 注册请求日志过滤器
     *
     * @return 过滤器注册对象
     */
    @Bean
    public FilterRegistrationBean<Filter> requestLogFilterRegistrationBean() {
        FilterRegistrationBean<Filter> registrationBean = new FilterRegistrationBean<>();

        registrationBean.setFilter(new RequestLogFilter());
        registrationBean.setName("requestLogFilter");

        // 拦截所有接口请求
        registrationBean.addUrlPatterns("/*");

        // 数值越小，执行优先级越高
        registrationBean.setOrder(1);

        return registrationBean;
    }

    /**
     * 注册 Token 校验过滤器
     *
     * @return 过滤器注册对象
     */
    @Bean
    public FilterRegistrationBean<Filter> tokenCheckFilterRegistrationBean() {
        FilterRegistrationBean<Filter> registrationBean = new FilterRegistrationBean<>();

        registrationBean.setFilter(new TokenCheckFilter());
        registrationBean.setName("tokenCheckFilter");

        // 只拦截业务接口
        registrationBean.addUrlPatterns("/api/*");

        // 日志过滤器先执行，Token 校验过滤器后执行
        registrationBean.setOrder(2);

        return registrationBean;
    }

}
```

使用 `FilterRegistrationBean` 时，过滤器类本身不需要添加 `@Component`。如果同时使用 `@Component` 和 `FilterRegistrationBean` 注册同一个过滤器，可能导致过滤器执行两次。

推荐做法是二选一：

| 注册方式                 | 适用场景                         |
| ------------------------ | -------------------------------- |
| `@Component`             | 简单全局过滤器                   |
| `FilterRegistrationBean` | 需要配置顺序、名称、路径的过滤器 |

### 配置过滤器执行顺序

当系统中存在多个过滤器时，需要明确过滤器执行顺序。Spring Boot 中可以通过 `FilterRegistrationBean#setOrder` 设置顺序。

执行规则是：`order` 数值越小，过滤器越先执行。

例如：

```java
registrationBean.setOrder(1);
```

多个过滤器的执行过程类似嵌套调用。

```text
RequestLogFilter 前置逻辑
  ↓
TokenCheckFilter 前置逻辑
  ↓
Controller
  ↓
TokenCheckFilter 后置逻辑
  ↓
RequestLogFilter 后置逻辑
```

如果两个过滤器顺序如下：

| 过滤器             | order | 执行顺序                 |
| ------------------ | ----- | ------------------------ |
| `RequestLogFilter` | `1`   | 第 1 个进入，第 2 个退出 |
| `TokenCheckFilter` | `2`   | 第 2 个进入，第 1 个退出 |

可以使用常量集中管理顺序，避免数字散落在配置类中。

文件位置：`src/main/java/io/github/atengk/config/FilterOrder.java`

```java
package io.github.atengk.config;

/**
 * 过滤器顺序常量
 *
 * @author Ateng
 * @since 2026-05-06
 */
public final class FilterOrder {

    /**
     * 请求日志过滤器顺序
     */
    public static final int REQUEST_LOG_FILTER = 1;

    /**
     * Token 校验过滤器顺序
     */
    public static final int TOKEN_CHECK_FILTER = 2;

    private FilterOrder() {
    }

}
```

然后在配置类中使用常量。

```java
registrationBean.setOrder(FilterOrder.REQUEST_LOG_FILTER);
```

这样做的好处是后续新增过滤器时，可以统一调整顺序，避免多个配置类之间顺序冲突。

### 配置拦截路径

使用 `FilterRegistrationBean` 可以通过 `addUrlPatterns` 配置过滤器拦截路径。

常见配置如下：

| 配置       | 说明                    |
| ---------- | ----------------------- |
| `/*`       | 拦截所有请求            |
| `/api/*`   | 拦截 `/api/` 下的请求   |
| `/admin/*` | 拦截 `/admin/` 下的请求 |
| `/user/*`  | 拦截 `/user/` 下的请求  |

示例：

```java
registrationBean.addUrlPatterns("/api/*");
```

如果一个过滤器需要拦截多个路径，可以连续添加多个 URL Pattern。

```java
registrationBean.addUrlPatterns("/api/*", "/admin/*");
```

下面示例演示只对 `/api/*` 和 `/admin/*` 路径启用 Token 校验。

文件位置：`src/main/java/io/github/atengk/config/TokenFilterConfig.java`

```java
package io.github.atengk.config;

import io.github.atengk.filter.TokenCheckFilter;
import jakarta.servlet.Filter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Token 过滤器配置
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Configuration
public class TokenFilterConfig {

    /**
     * 注册 Token 校验过滤器
     *
     * @return 过滤器注册对象
     */
    @Bean
    public FilterRegistrationBean<Filter> tokenCheckFilterRegistrationBean() {
        FilterRegistrationBean<Filter> registrationBean = new FilterRegistrationBean<>();

        registrationBean.setFilter(new TokenCheckFilter());
        registrationBean.setName("tokenCheckFilter");

        // 只拦截需要认证的接口路径
        registrationBean.addUrlPatterns("/api/*", "/admin/*");

        // Token 校验在日志过滤器之后执行
        registrationBean.setOrder(FilterOrder.TOKEN_CHECK_FILTER);

        return registrationBean;
    }

}
```

需要注意，`addUrlPatterns` 使用的是 Servlet URL Pattern 规则，不是 Spring MVC 的 `AntPathMatcher` 规则。因此这里通常使用 `/api/*`，而不是 `/api/**`。

如果需要更灵活的路径排除逻辑，例如排除 `/api/login`、`/api/register`，可以在过滤器内部自行判断。

下面示例演示在过滤器内部配置白名单路径。

文件位置：`src/main/java/io/github/atengk/filter/WhitelistTokenCheckFilter.java`

```java
package io.github.atengk.filter;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * 带白名单的 Token 校验过滤器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
public class WhitelistTokenCheckFilter implements Filter {

    private static final String AUTHORIZATION_HEADER = "Authorization";

    private static final List<String> WHITE_LIST = List.of(
            "/api/login",
            "/api/register",
            "/api/health"
    );

    /**
     * 执行 Token 校验
     *
     * @param request 请求对象
     * @param response 响应对象
     * @param chain 过滤器链
     * @throws IOException IO 异常
     * @throws ServletException Servlet 异常
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String uri = httpRequest.getRequestURI();

        if (this.isWhiteUri(uri)) {
            log.info("命中白名单路径，直接放行，uri={}", uri);
            chain.doFilter(request, response);
            return;
        }

        String token = httpRequest.getHeader(AUTHORIZATION_HEADER);
        if (StrUtil.isBlank(token)) {
            log.warn("Token 校验失败，请求未携带认证信息，uri={}", uri);
            this.writeUnauthorizedResponse(httpResponse, "未携带认证信息");
            return;
        }

        log.info("Token 校验通过，uri={}", uri);
        chain.doFilter(request, response);
    }

    /**
     * 判断是否为白名单路径
     *
     * @param uri 请求路径
     * @return 是否白名单路径
     */
    private boolean isWhiteUri(String uri) {
        return CollUtil.contains(WHITE_LIST, uri);
    }

    /**
     * 写入未认证响应
     *
     * @param response 响应对象
     * @param message 响应消息
     * @throws IOException IO 异常
     */
    private void writeUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json;charset=UTF-8");

        Map<String, Object> result = Map.of(
                "code", HttpServletResponse.SC_UNAUTHORIZED,
                "message", message,
                "success", false
        );

        response.getWriter().write(JSONUtil.toJsonStr(result));
    }

}
```

如果白名单路径较多，建议将路径配置到 `application.yml`，再通过配置属性类读取，避免硬编码在过滤器中。当前示例主要用于说明过滤器内部如何进行路径放行控制。



## 常见业务示例

本节给出几个过滤器在实际项目中的常见用法，包括请求日志、跨域响应头、Token 校验和请求参数包装。示例均基于 Spring Boot 3，使用 `jakarta.servlet` 相关 API。

### 请求日志过滤器

请求日志过滤器通常用于记录接口访问情况，便于排查线上问题、统计接口耗时和定位异常请求。常见记录内容包括请求方法、请求路径、客户端 IP、响应状态码和接口耗时。

文件位置：`src/main/java/io/github/atengk/filter/RequestLogFilter.java`

```java
package io.github.atengk.filter;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.util.StrUtil;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * 请求日志过滤器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
public class RequestLogFilter implements Filter {

    /**
     * 记录请求日志
     *
     * @param request 请求对象
     * @param response 响应对象
     * @param chain 过滤器链
     * @throws IOException IO 异常
     * @throws ServletException Servlet 异常
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        TimeInterval timer = DateUtil.timer();
        String method = httpRequest.getMethod();
        String uri = httpRequest.getRequestURI();
        String clientIp = this.getClientIp(httpRequest);

        log.info("请求开始，method={}，uri={}，clientIp={}", method, uri, clientIp);

        try {
            chain.doFilter(request, response);
        } finally {
            long cost = timer.interval();
            int status = httpResponse.getStatus();
            log.info("请求结束，method={}，uri={}，status={}，cost={}ms", method, uri, status, cost);
        }
    }

    /**
     * 获取客户端 IP
     *
     * @param request 请求对象
     * @return 客户端 IP
     */
    private String getClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StrUtil.isNotBlank(forwardedFor)) {
            return StrUtil.splitTrim(forwardedFor, ',').get(0);
        }

        String realIp = request.getHeader("X-Real-IP");
        if (StrUtil.isNotBlank(realIp)) {
            return realIp;
        }

        return request.getRemoteAddr();
    }

}
```

该过滤器建议放在过滤器链的靠前位置，这样可以覆盖后续 Token 校验失败、业务异常、Controller 正常返回等多数请求场景。`finally` 中记录响应状态和耗时，可以保证请求发生异常时仍然输出结束日志。

### 跨域处理过滤器

跨域处理过滤器用于在响应中统一追加 CORS 相关响应头。对于前后端分离项目，如果前端域名和后端接口域名不同，浏览器会触发跨域限制，此时可以通过过滤器统一处理跨域响应。

文件位置：`src/main/java/io/github/atengk/filter/CorsHeaderFilter.java`

```java
package io.github.atengk.filter;

import cn.hutool.core.util.StrUtil;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * 跨域响应头过滤器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
public class CorsHeaderFilter implements Filter {

    /**
     * 处理跨域响应头
     *
     * @param request 请求对象
     * @param response 响应对象
     * @param chain 过滤器链
     * @throws IOException IO 异常
     * @throws ServletException Servlet 异常
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String origin = httpRequest.getHeader("Origin");
        if (StrUtil.isNotBlank(origin)) {
            httpResponse.setHeader("Access-Control-Allow-Origin", origin);
            httpResponse.setHeader("Access-Control-Allow-Credentials", "true");
            httpResponse.setHeader("Vary", "Origin");
        }

        httpResponse.setHeader("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS");
        httpResponse.setHeader("Access-Control-Allow-Headers", "Authorization,Content-Type,X-Requested-With");
        httpResponse.setHeader("Access-Control-Max-Age", "3600");

        if ("OPTIONS".equalsIgnoreCase(httpRequest.getMethod())) {
            log.info("跨域预检请求直接返回，uri={}", httpRequest.getRequestURI());
            httpResponse.setStatus(HttpServletResponse.SC_NO_CONTENT);
            return;
        }

        chain.doFilter(request, response);
    }

}
```

该示例对浏览器预检请求 `OPTIONS` 直接返回 `204`，避免预检请求继续进入后续认证或业务逻辑。实际生产环境中，建议将允许的域名做成配置项，不建议无条件放行所有来源。

### Token 校验过滤器

Token 校验过滤器用于在请求进入业务接口之前完成认证校验。常见处理方式是读取请求头中的 `Authorization`，判断 Token 是否存在、格式是否正确、是否过期或是否能在 Redis 中查询到登录态。

下面示例只演示基础 Token 判断逻辑，用于说明过滤器中断请求和返回 JSON 响应的方式。

文件位置：`src/main/java/io/github/atengk/filter/SimpleTokenFilter.java`

```java
package io.github.atengk.filter;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 简单 Token 校验过滤器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
public class SimpleTokenFilter implements Filter {

    private static final String AUTHORIZATION_HEADER = "Authorization";

    /**
     * 校验请求 Token
     *
     * @param request 请求对象
     * @param response 响应对象
     * @param chain 过滤器链
     * @throws IOException IO 异常
     * @throws ServletException Servlet 异常
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String uri = httpRequest.getRequestURI();

        if (this.isPermitUri(uri)) {
            log.info("公开接口直接放行，uri={}", uri);
            chain.doFilter(request, response);
            return;
        }

        String token = httpRequest.getHeader(AUTHORIZATION_HEADER);
        if (StrUtil.isBlank(token)) {
            log.warn("Token 校验失败，认证信息为空，uri={}", uri);
            this.writeJson(httpResponse, HttpServletResponse.SC_UNAUTHORIZED, "未登录或登录已过期");
            return;
        }

        if (!StrUtil.startWith(token, "Bearer ")) {
            log.warn("Token 校验失败，认证格式错误，uri={}", uri);
            this.writeJson(httpResponse, HttpServletResponse.SC_UNAUTHORIZED, "认证格式错误");
            return;
        }

        log.info("Token 校验通过，uri={}", uri);
        chain.doFilter(request, response);
    }

    /**
     * 判断是否为公开接口
     *
     * @param uri 请求路径
     * @return 是否公开接口
     */
    private boolean isPermitUri(String uri) {
        return StrUtil.equalsAny(uri, "/api/login", "/api/register", "/api/health");
    }

    /**
     * 写入 JSON 响应
     *
     * @param response 响应对象
     * @param status HTTP 状态码
     * @param message 响应消息
     * @throws IOException IO 异常
     */
    private void writeJson(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json;charset=UTF-8");

        Map<String, Object> result = Map.of(
                "code", status,
                "message", message,
                "success", false
        );

        response.getWriter().write(JSONUtil.toJsonStr(result));
    }

}
```

如果认证失败，过滤器直接写入响应并 `return`，不再执行 `chain.doFilter`。这表示请求在当前过滤器中结束，不会进入 Controller。

实际项目中可以将 `Bearer` Token 解析逻辑替换为 JWT、Sa-Token、Spring Security 或自定义登录态校验逻辑。

### 请求参数包装过滤器

请求参数包装过滤器主要用于解决请求体只能读取一次的问题。默认情况下，`HttpServletRequest` 的输入流只能读取一次。如果过滤器中读取了 JSON 请求体，Controller 中使用 `@RequestBody` 时可能无法再次读取。

解决方式是自定义 `HttpServletRequestWrapper`，在包装类中提前缓存请求体字节数组，并重写 `getInputStream` 和 `getReader` 方法。

文件位置：`src/main/java/io/github/atengk/filter/CachedBodyRequestWrapper.java`

```java
package io.github.atengk.filter;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * 可重复读取请求体包装类
 *
 * @author Ateng
 * @since 2026-05-06
 */
public class CachedBodyRequestWrapper extends HttpServletRequestWrapper {

    private final byte[] cachedBody;

    /**
     * 构建请求体包装对象
     *
     * @param request 原始请求对象
     * @throws IOException IO 异常
     */
    public CachedBodyRequestWrapper(HttpServletRequest request) throws IOException {
        super(request);
        this.cachedBody = IoUtil.readBytes(request.getInputStream());
    }

    /**
     * 获取请求输入流
     *
     * @return Servlet 输入流
     */
    @Override
    public ServletInputStream getInputStream() {
        ByteArrayInputStream bodyInputStream = new ByteArrayInputStream(this.cachedBody);

        return new ServletInputStream() {

            /**
             * 读取请求体字节
             *
             * @return 字节内容
             */
            @Override
            public int read() {
                return bodyInputStream.read();
            }

            /**
             * 判断是否读取完成
             *
             * @return 是否读取完成
             */
            @Override
            public boolean isFinished() {
                return bodyInputStream.available() == 0;
            }

            /**
             * 判断是否可读
             *
             * @return 是否可读
             */
            @Override
            public boolean isReady() {
                return true;
            }

            /**
             * 设置读取监听器
             *
             * @param readListener 读取监听器
             */
            @Override
            public void setReadListener(ReadListener readListener) {
                if (readListener == null) {
                    return;
                }

                try {
                    if (isFinished()) {
                        readListener.onAllDataRead();
                    } else {
                        readListener.onDataAvailable();
                    }
                } catch (IOException e) {
                    readListener.onError(e);
                }
            }
        };
    }

    /**
     * 获取请求字符流
     *
     * @return BufferedReader
     */
    @Override
    public BufferedReader getReader() {
        String charsetName = StrUtil.blankToDefault(this.getCharacterEncoding(), "UTF-8");
        return new BufferedReader(new InputStreamReader(this.getInputStream(), java.nio.charset.Charset.forName(charsetName)));
    }

    /**
     * 获取缓存的请求体
     *
     * @return 请求体字节数组
     */
    public byte[] getCachedBody() {
        return this.cachedBody.clone();
    }

}
```

包装类创建完成后，需要通过过滤器将原始请求替换为包装后的请求对象。

文件位置：`src/main/java/io/github/atengk/filter/CachedBodyFilter.java`

```java
package io.github.atengk.filter;

import cn.hutool.core.util.StrUtil;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * 请求体缓存过滤器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
public class CachedBodyFilter implements Filter {

    /**
     * 包装请求体，使请求体可以重复读取
     *
     * @param request 请求对象
     * @param response 响应对象
     * @param chain 过滤器链
     * @throws IOException IO 异常
     * @throws ServletException Servlet 异常
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;

        if (!this.needWrap(httpRequest)) {
            chain.doFilter(request, response);
            return;
        }

        CachedBodyRequestWrapper requestWrapper = new CachedBodyRequestWrapper(httpRequest);
        log.info("请求体已包装，uri={}", httpRequest.getRequestURI());

        chain.doFilter(requestWrapper, response);
    }

    /**
     * 判断是否需要包装请求体
     *
     * @param request 请求对象
     * @return 是否需要包装
     */
    private boolean needWrap(HttpServletRequest request) {
        String method = request.getMethod();
        String contentType = request.getContentType();

        if (!StrUtil.equalsAnyIgnoreCase(method, "POST", "PUT", "PATCH")) {
            return false;
        }

        return StrUtil.containsAny(contentType, "application/json", "text/plain", "application/xml");
    }

}
```

该过滤器建议只对 `POST`、`PUT`、`PATCH` 等可能携带请求体的方法生效，不建议对所有请求无差别包装。请求体较大时，包装类会将请求体读入内存，需要结合网关、Nginx 或应用配置限制请求体大小。

## 过滤器执行流程

过滤器的执行流程可以理解为一条请求处理链。请求从客户端进入应用后，会依次经过多个过滤器，然后进入 `DispatcherServlet`、拦截器和 Controller。响应返回时，会按照相反方向依次回到过滤器。

### 请求进入过滤器链

当客户端发起 HTTP 请求后，Servlet 容器会先根据 URL Pattern 找到匹配的过滤器，并按照顺序组成过滤器链。每个过滤器都可以在 `chain.doFilter` 前后执行自己的逻辑。

基本流程如下：

```text
客户端请求
  ↓
Servlet 容器匹配 URL Pattern
  ↓
进入 FilterChain
  ↓
执行第一个 Filter
  ↓
执行后续 Filter
  ↓
进入 DispatcherServlet
  ↓
进入 Controller
  ↓
返回响应
```

过滤器中的关键代码是：

```java
chain.doFilter(request, response);
```

该方法表示继续执行后续链路。如果当前过滤器没有调用该方法，请求会在当前过滤器中结束。

例如 Token 校验失败时：

```text
请求进入 TokenFilter
  ↓
读取 Authorization 请求头
  ↓
Token 为空
  ↓
写入 401 JSON 响应
  ↓
return
  ↓
Controller 不执行
```

因此过滤器非常适合做认证、黑名单、签名校验等前置阻断逻辑。

### 多个过滤器的执行顺序

多个过滤器并不是简单地从上到下执行完就结束，而是类似嵌套调用。前置逻辑按照顺序执行，后置逻辑按照相反顺序执行。

假设有三个过滤器：

| 过滤器              | order | 职责           |
| ------------------- | ----- | -------------- |
| `RequestLogFilter`  | 1     | 记录请求日志   |
| `CorsHeaderFilter`  | 2     | 处理跨域响应头 |
| `SimpleTokenFilter` | 3     | 校验 Token     |

执行过程如下：

```text
RequestLogFilter 前置逻辑
  ↓
CorsHeaderFilter 前置逻辑
  ↓
SimpleTokenFilter 前置逻辑
  ↓
Controller
  ↓
SimpleTokenFilter 后置逻辑
  ↓
CorsHeaderFilter 后置逻辑
  ↓
RequestLogFilter 后置逻辑
```

如果 `SimpleTokenFilter` 校验失败，不调用 `chain.doFilter`，则流程会变成：

```text
RequestLogFilter 前置逻辑
  ↓
CorsHeaderFilter 前置逻辑
  ↓
SimpleTokenFilter 前置逻辑
  ↓
SimpleTokenFilter 写入 401 响应并 return
  ↓
CorsHeaderFilter 后置逻辑
  ↓
RequestLogFilter 后置逻辑
```

这里需要注意，当前过滤器不放行时，当前过滤器后面的链路不会执行，但当前过滤器之前已经进入的过滤器，其 `chain.doFilter` 后面的代码仍然会继续执行。

因此日志过滤器通常放在较前位置，可以记录到大多数请求的最终响应状态。

### 异常处理流程

过滤器链中任意位置发生异常时，异常会沿调用栈向外抛出。过滤器可以选择捕获异常并自行写入响应，也可以继续抛出给 Spring Boot 的异常处理机制。

常见异常来源包括：

| 异常位置       | 示例                             |
| -------------- | -------------------------------- |
| 过滤器前置逻辑 | Token 解析异常、请求参数读取异常 |
| 后续过滤器     | 请求包装失败、跨域配置异常       |
| Controller     | 业务异常、参数校验异常           |
| Service        | 数据库异常、远程调用异常         |

如果过滤器只做日志记录，通常不要吞掉异常，而是记录后继续抛出，避免影响全局异常处理。

文件位置：`src/main/java/io/github/atengk/filter/ExceptionLogFilter.java`

```java
package io.github.atengk.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * 异常日志过滤器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
public class ExceptionLogFilter implements Filter {

    /**
     * 记录过滤器链中的异常
     *
     * @param request 请求对象
     * @param response 响应对象
     * @param chain 过滤器链
     * @throws IOException IO 异常
     * @throws ServletException Servlet 异常
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;

        try {
            chain.doFilter(request, response);
        } catch (IOException | ServletException e) {
            log.error("过滤器链执行异常，uri={}", httpRequest.getRequestURI(), e);
            throw e;
        } catch (RuntimeException e) {
            log.error("业务运行异常，uri={}", httpRequest.getRequestURI(), e);
            throw e;
        }
    }

}
```

如果过滤器自身需要直接返回错误响应，例如 Token 校验失败、签名校验失败，可以在过滤器内部写入 JSON 响应并结束请求。

```java
response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
response.setContentType("application/json;charset=UTF-8");
response.getWriter().write("{\"code\":401,\"message\":\"未登录或登录已过期\"}");
return;
```

需要区分两类异常处理方式：

| 处理方式           | 适用场景                                                   |
| ------------------ | ---------------------------------------------------------- |
| 记录日志后继续抛出 | Controller 或 Service 中的业务异常，需要交给全局异常处理   |
| 过滤器直接写响应   | 认证失败、签名失败、非法来源等不需要进入 Controller 的场景 |

在实际项目中，建议认证失败、跨域预检、黑名单拦截这类请求由过滤器直接处理；业务异常、参数校验异常和数据库异常交给 Spring Boot 全局异常处理器统一处理。



## 配置与验证

本节用于说明过滤器相关配置如何落地到 `application.yml`，以及如何通过测试接口、curl 命令和日志输出验证过滤器是否生效。实际开发时，建议先完成最小可运行链路，再逐步增加日志、跨域、Token 校验和请求体包装等能力。

### application.yml 配置示例

过滤器本身可以直接通过 Java 配置类注册，也可以将是否启用、拦截路径、白名单路径等内容放到配置文件中，便于不同环境调整。

文件位置：`src/main/resources/application.yml`

```yaml
server:
  # 应用服务端口
  port: 8080

spring:
  application:
    # 应用名称
    name: springboot-filter-demo

logging:
  level:
    # 项目包日志级别
    io.github.atengk: info
    # Spring Web 日志级别
    org.springframework.web: info

demo:
  filter:
    # 是否启用请求日志过滤器
    request-log-enabled: true

    # 是否启用 Token 校验过滤器
    token-enabled: true

    # Token 请求头名称
    token-header-name: Authorization

    # Token 前缀
    token-prefix: "Bearer "

    # Token 校验白名单路径
    token-white-list:
      - /api/login
      - /api/register
      - /api/health

    # 是否启用请求体缓存过滤器
    cached-body-enabled: true
```

如果希望在过滤器中读取这些配置，可以定义一个配置属性类。

文件位置：`src/main/java/io/github/atengk/config/FilterProperties.java`

```java
package io.github.atengk.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 过滤器配置属性
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@ConfigurationProperties(prefix = "demo.filter")
public class FilterProperties {

    /**
     * 是否启用请求日志过滤器
     */
    private Boolean requestLogEnabled = true;

    /**
     * 是否启用 Token 校验过滤器
     */
    private Boolean tokenEnabled = true;

    /**
     * Token 请求头名称
     */
    private String tokenHeaderName = "Authorization";

    /**
     * Token 前缀
     */
    private String tokenPrefix = "Bearer ";

    /**
     * Token 白名单路径
     */
    private List<String> tokenWhiteList = new ArrayList<>();

    /**
     * 是否启用请求体缓存过滤器
     */
    private Boolean cachedBodyEnabled = true;

}
```

注册配置属性类时，可以在过滤器配置类上使用 `@EnableConfigurationProperties`。

文件位置：`src/main/java/io/github/atengk/config/FilterConfig.java`

```java
package io.github.atengk.config;

import io.github.atengk.filter.CachedBodyFilter;
import io.github.atengk.filter.RequestLogFilter;
import io.github.atengk.filter.SimpleTokenFilter;
import jakarta.servlet.Filter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 过滤器注册配置
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Configuration
@EnableConfigurationProperties(FilterProperties.class)
public class FilterConfig {

    /**
     * 注册请求体缓存过滤器
     *
     * @param properties 过滤器配置属性
     * @return 过滤器注册对象
     */
    @Bean
    public FilterRegistrationBean<Filter> cachedBodyFilterRegistrationBean(FilterProperties properties) {
        FilterRegistrationBean<Filter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new CachedBodyFilter());
        registrationBean.setName("cachedBodyFilter");
        registrationBean.addUrlPatterns("/api/*");
        registrationBean.setOrder(1);
        registrationBean.setEnabled(Boolean.TRUE.equals(properties.getCachedBodyEnabled()));
        return registrationBean;
    }

    /**
     * 注册请求日志过滤器
     *
     * @param properties 过滤器配置属性
     * @return 过滤器注册对象
     */
    @Bean
    public FilterRegistrationBean<Filter> requestLogFilterRegistrationBean(FilterProperties properties) {
        FilterRegistrationBean<Filter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new RequestLogFilter());
        registrationBean.setName("requestLogFilter");
        registrationBean.addUrlPatterns("/*");
        registrationBean.setOrder(2);
        registrationBean.setEnabled(Boolean.TRUE.equals(properties.getRequestLogEnabled()));
        return registrationBean;
    }

    /**
     * 注册 Token 校验过滤器
     *
     * @param properties 过滤器配置属性
     * @return 过滤器注册对象
     */
    @Bean
    public FilterRegistrationBean<Filter> tokenFilterRegistrationBean(FilterProperties properties) {
        FilterRegistrationBean<Filter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new SimpleTokenFilter());
        registrationBean.setName("simpleTokenFilter");
        registrationBean.addUrlPatterns("/api/*");
        registrationBean.setOrder(3);
        registrationBean.setEnabled(Boolean.TRUE.equals(properties.getTokenEnabled()));
        return registrationBean;
    }

}
```

这里将请求体缓存过滤器放在请求日志和 Token 校验之前，避免后续过滤器读取请求体后导致 Controller 无法再次读取。请求日志过滤器放在 Token 过滤器之前，可以记录 Token 校验失败的请求。

### 接口测试方式

为了验证过滤器是否生效，可以创建一个简单的测试 Controller。测试接口应覆盖公开接口、认证接口、POST JSON 请求和异常接口。

文件位置：`src/main/java/io/github/atengk/controller/TestController.java`

```java
package io.github.atengk.controller;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.map.MapUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 过滤器测试接口
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@RestController
@RequestMapping("/api")
public class TestController {

    /**
     * 健康检查接口
     *
     * @return 响应内容
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        log.info("健康检查接口执行");
        return MapUtil.builder("success", true)
                .put("message", "服务正常")
                .put("time", DateUtil.now())
                .build();
    }

    /**
     * 登录模拟接口
     *
     * @return 响应内容
     */
    @PostMapping("/login")
    public Map<String, Object> login() {
        log.info("登录模拟接口执行");
        return MapUtil.builder("success", true)
                .put("token", "Bearer test-token")
                .put("message", "登录成功")
                .build();
    }

    /**
     * 普通业务接口
     *
     * @return 响应内容
     */
    @GetMapping("/hello")
    public Map<String, Object> hello() {
        log.info("普通业务接口执行");
        return MapUtil.builder("success", true)
                .put("message", "hello filter")
                .build();
    }

    /**
     * JSON 请求体测试接口
     *
     * @param body 请求体
     * @return 响应内容
     */
    @PostMapping("/echo")
    public Map<String, Object> echo(@RequestBody Map<String, Object> body) {
        log.info("JSON 请求体测试接口执行，body={}", body);
        return MapUtil.builder("success", true)
                .put("data", body)
                .build();
    }

    /**
     * 异常测试接口
     *
     * @return 响应内容
     */
    @GetMapping("/error-test")
    public Map<String, Object> errorTest() {
        log.info("异常测试接口执行");
        throw new IllegalStateException("模拟业务异常");
    }

}
```

启动项目后，可以使用以下命令验证不同场景。

公开接口不携带 Token，预期可以正常访问。

```bash
curl -i http://localhost:8080/api/health
```

未携带 Token 访问业务接口，预期返回 `401`。

```bash
curl -i http://localhost:8080/api/hello
```

携带 Token 访问业务接口，预期返回成功响应。

```bash
curl -i \
  -H "Authorization: Bearer test-token" \
  http://localhost:8080/api/hello
```

POST JSON 请求体测试，预期 Controller 能正常接收到请求体。

```bash
curl -i \
  -X POST \
  -H "Authorization: Bearer test-token" \
  -H "Content-Type: application/json" \
  -d '{"username":"ateng","age":18}' \
  http://localhost:8080/api/echo
```

跨域预检请求测试，预期直接返回 `204` 或包含跨域响应头。

```bash
curl -i \
  -X OPTIONS \
  -H "Origin: http://localhost:5173" \
  -H "Access-Control-Request-Method: POST" \
  http://localhost:8080/api/echo
```

异常接口测试，预期请求日志过滤器仍然能输出请求结束日志。

```bash
curl -i \
  -H "Authorization: Bearer test-token" \
  http://localhost:8080/api/error-test
```

上述命令中，`-i` 用于输出响应头，便于查看 HTTP 状态码、跨域响应头和响应内容。`-H` 用于设置请求头，`-d` 用于发送 JSON 请求体。

### 日志输出验证

日志验证主要用于确认过滤器是否执行、执行顺序是否正确、请求是否被放行，以及异常场景下日志是否完整。

正常请求的日志示例如下：

```text
请求体已包装，uri=/api/echo
请求开始，method=POST，uri=/api/echo，clientIp=127.0.0.1
Token 校验通过，uri=/api/echo
JSON 请求体测试接口执行，body={username=ateng, age=18}
请求结束，method=POST，uri=/api/echo，status=200，cost=15ms
```

未携带 Token 的日志示例如下：

```text
请求开始，method=GET，uri=/api/hello，clientIp=127.0.0.1
Token 校验失败，认证信息为空，uri=/api/hello
请求结束，method=GET，uri=/api/hello，status=401，cost=3ms
```

业务异常的日志示例如下：

```text
请求开始，method=GET，uri=/api/error-test，clientIp=127.0.0.1
Token 校验通过，uri=/api/error-test
异常测试接口执行
请求结束，method=GET，uri=/api/error-test，status=500，cost=8ms
```

通过日志可以重点确认以下内容：

| 验证项               | 判断方式                                            |
| -------------------- | --------------------------------------------------- |
| 过滤器是否生效       | 是否输出过滤器日志                                  |
| 执行顺序是否正确     | 日志顺序是否符合 `order` 配置                       |
| Token 是否拦截成功   | 未携带 Token 时是否返回 `401`                       |
| 请求体是否可重复读取 | 过滤器包装后 Controller 是否还能接收 `@RequestBody` |
| 异常是否被记录       | Controller 抛异常后是否仍输出请求结束日志           |
| 跨域是否生效         | `OPTIONS` 请求是否直接返回，响应头是否存在          |

如果日志没有输出，优先检查过滤器是否已注册、URL Pattern 是否匹配、`setEnabled` 是否为 `true`，以及当前日志级别是否屏蔽了项目包日志。

## 开发注意事项

本节总结过滤器开发中容易出现的问题，包括请求体重复读取、执行顺序冲突、异常响应处理和 Spring Boot 3 兼容性。实际项目中，这些问题比过滤器本身的写法更容易导致线上故障。

### 请求体重复读取问题

`HttpServletRequest` 的请求体输入流默认只能读取一次。如果过滤器中调用了 `request.getInputStream()` 或 `request.getReader()`，后续 Controller 中的 `@RequestBody` 可能读取不到内容。

容易出问题的场景包括：

| 场景                           | 风险                          |
| ------------------------------ | ----------------------------- |
| 日志过滤器打印 JSON 请求体     | Controller 无法再次读取请求体 |
| 签名过滤器读取请求体计算签名   | 后续参数绑定失败              |
| Token 过滤器读取请求体中的字段 | 请求体被提前消费              |
| 全链路审计记录请求参数         | 大请求体占用内存              |

推荐处理方式是使用 `HttpServletRequestWrapper` 缓存请求体，并将包装后的请求对象继续传入过滤器链。

```java
CachedBodyRequestWrapper requestWrapper = new CachedBodyRequestWrapper(httpRequest);
chain.doFilter(requestWrapper, response);
```

开发时需要注意两点。

第一，不建议对所有请求都缓存请求体。一般只处理 `POST`、`PUT`、`PATCH`，并且只处理 `application/json`、`text/plain`、`application/xml` 等确实需要读取请求体的类型。

第二，请求体缓存会占用 JVM 内存。对于文件上传、大 JSON、批量导入等接口，应避免在过滤器中读取完整请求体，或者通过网关和服务端配置限制请求体大小。

### 过滤器顺序冲突问题

多个过滤器同时存在时，顺序配置不当会导致认证失效、跨域失败、日志不完整或请求体读取异常。

常见顺序问题如下：

| 问题                      | 原因                                              |
| ------------------------- | ------------------------------------------------- |
| 跨域预检请求返回 `401`    | Token 过滤器先于跨域过滤器执行                    |
| Controller 读取不到请求体 | 请求体缓存过滤器执行太晚                          |
| 认证失败请求没有日志      | 日志过滤器执行顺序太靠后                          |
| 同一个过滤器执行两次      | 同时使用 `@Component` 和 `FilterRegistrationBean` |
| 路径配置不生效            | 使用了 `/api/**` 这类 Spring MVC 风格路径         |

建议顺序如下：

```text
CorsHeaderFilter
  ↓
CachedBodyFilter
  ↓
RequestLogFilter
  ↓
TokenFilter
  ↓
其他业务过滤器
  ↓
DispatcherServlet
```

如果跨域过滤器负责处理 `OPTIONS` 预检请求，应尽量放在 Token 校验过滤器之前，避免浏览器预检请求被认证逻辑拦截。

建议使用常量统一管理过滤器顺序。

文件位置：`src/main/java/io/github/atengk/config/FilterOrder.java`

```java
package io.github.atengk.config;

/**
 * 过滤器顺序常量
 *
 * @author Ateng
 * @since 2026-05-06
 */
public final class FilterOrder {

    /**
     * 跨域过滤器顺序
     */
    public static final int CORS = 0;

    /**
     * 请求体缓存过滤器顺序
     */
    public static final int CACHED_BODY = 1;

    /**
     * 请求日志过滤器顺序
     */
    public static final int REQUEST_LOG = 2;

    /**
     * Token 校验过滤器顺序
     */
    public static final int TOKEN = 3;

    private FilterOrder() {
    }

}
```

同时，注册过滤器时建议只使用一种方式。使用 `FilterRegistrationBean` 时，过滤器类不要再添加 `@Component`，否则容易重复注册。

### 异常响应统一处理

过滤器位于 Spring MVC 之前，部分过滤器异常不会进入 Controller，也不一定会被 `@RestControllerAdvice` 统一处理。因此，过滤器中需要区分“直接响应”和“继续抛出”两种处理方式。

适合在过滤器中直接响应的场景包括：

| 场景             | 推荐响应                              |
| ---------------- | ------------------------------------- |
| Token 为空       | `401 Unauthorized`                    |
| Token 无效或过期 | `401 Unauthorized`                    |
| 签名错误         | `401 Unauthorized` 或 `403 Forbidden` |
| IP 黑名单        | `403 Forbidden`                       |
| 请求来源非法     | `403 Forbidden`                       |
| 请求方法不允许   | `405 Method Not Allowed`              |

适合继续抛出的场景包括：

| 场景                | 推荐处理                     |
| ------------------- | ---------------------------- |
| Controller 业务异常 | 交给全局异常处理             |
| 参数校验异常        | 交给 Spring MVC 参数校验机制 |
| 数据库异常          | 交给全局异常处理             |
| 远程调用异常        | 交给业务层或全局异常处理     |

建议在过滤器中封装统一 JSON 响应方法，避免不同过滤器返回格式不一致。

文件位置：`src/main/java/io/github/atengk/filter/FilterResponseWriter.java`

```java
package io.github.atengk.filter;

import cn.hutool.json.JSONUtil;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 过滤器响应写入工具
 *
 * @author Ateng
 * @since 2026-05-06
 */
public final class FilterResponseWriter {

    private FilterResponseWriter() {
    }

    /**
     * 写入 JSON 响应
     *
     * @param response 响应对象
     * @param status HTTP 状态码
     * @param message 响应消息
     * @throws IOException IO 异常
     */
    public static void writeJson(HttpServletResponse response, int status, String message) throws IOException {
        if (response.isCommitted()) {
            return;
        }

        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json;charset=UTF-8");

        Map<String, Object> result = Map.of(
                "code", status,
                "message", message,
                "success", false
        );

        response.getWriter().write(JSONUtil.toJsonStr(result));
    }

}
```

过滤器中使用示例：

```java
FilterResponseWriter.writeJson(httpResponse, HttpServletResponse.SC_UNAUTHORIZED, "未登录或登录已过期");
return;
```

需要注意，如果响应已经提交，即 `response.isCommitted()` 为 `true`，再写入响应可能会抛出异常或导致响应内容不完整。因此统一响应方法中建议先判断响应是否已提交。

### Spring Boot 3 兼容性注意事项

Spring Boot 3 基于 Jakarta EE 规范，过滤器开发时需要重点关注包名迁移和依赖兼容问题。

主要变化如下：

| Spring Boot 2                            | Spring Boot 3                              |
| ---------------------------------------- | ------------------------------------------ |
| `javax.servlet.Filter`                   | `jakarta.servlet.Filter`                   |
| `javax.servlet.FilterChain`              | `jakarta.servlet.FilterChain`              |
| `javax.servlet.ServletRequest`           | `jakarta.servlet.ServletRequest`           |
| `javax.servlet.http.HttpServletRequest`  | `jakarta.servlet.http.HttpServletRequest`  |
| `javax.servlet.http.HttpServletResponse` | `jakarta.servlet.http.HttpServletResponse` |

Spring Boot 3 项目中应使用以下导包：

```java
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
```

不建议在 Spring Boot 3 项目中继续使用以下导包：

```java
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
```

兼容性检查建议如下：

| 检查项       | 说明                                                  |
| ------------ | ----------------------------------------------------- |
| JDK 版本     | Spring Boot 3 要求 JDK 17 或更高版本                  |
| Servlet 包名 | 使用 `jakarta.servlet.*`，不要使用 `javax.servlet.*`  |
| 第三方依赖   | 老版本依赖可能仍依赖 `javax.*`，需要升级              |
| 内置容器     | Spring Boot 3 默认使用兼容 Jakarta 的 Tomcat 10+      |
| 过滤器注册   | `FilterRegistrationBean` 仍可使用，但泛型和导包应匹配 |
| 全局异常处理 | 过滤器直接中断请求时，不会进入 Controller 层异常处理  |

如果项目是从 Spring Boot 2 升级到 Spring Boot 3，建议先全局搜索 `javax.servlet`，统一替换为 `jakarta.servlet`，然后检查所有 Web、权限、安全、网关、Swagger、文件上传等相关依赖版本是否支持 Spring Boot 3。