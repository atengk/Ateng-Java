# Thymeleaf



## 技术概述

本节说明 Thymeleaf 在 Spring Boot 3 Web 项目中的定位、集成方式和适用场景。Thymeleaf 属于服务端模板引擎，主要负责在服务端将模板页面与业务数据合并，最终输出 HTML 页面给浏览器。

### Thymeleaf 基本定位

Thymeleaf 是一个现代服务端 Java 模板引擎，既可用于 Web 环境，也可用于独立 Java 环境；它可以处理 HTML、XML、JavaScript、CSS 和纯文本等模板类型。Thymeleaf 的核心特点是“自然模板”，即模板文件本身仍然是合法 HTML，可以直接在浏览器中预览，便于前后端或设计人员协作。([thymeleaf.org](https://www.thymeleaf.org/doc/tutorials/3.1/usingthymeleaf.html?utm_source=chatgpt.com))

在 Spring MVC 项目中，Thymeleaf 通常位于视图层，负责把 Controller 放入 `Model` 的数据渲染到 HTML 页面中。它不是前端框架，也不是接口返回格式工具，而是服务端页面渲染方案。典型流程如下：

```text
浏览器请求
   ↓
Spring MVC Controller
   ↓
业务处理并写入 Model
   ↓
返回 Thymeleaf 模板视图名
   ↓
Thymeleaf 渲染 HTML
   ↓
浏览器展示页面
```

Thymeleaf 模板通常存放在 `src/main/resources/templates/` 目录下，静态资源通常存放在 `src/main/resources/static/` 目录下。页面模板中通过 `th:text`、`th:if`、`th:each`、`th:href`、`th:action` 等属性完成文本输出、条件判断、循环渲染、链接生成和表单提交。

### Spring Boot 3 集成方式

Spring Boot 3 通过 `spring-boot-starter-thymeleaf` 提供 Thymeleaf 集成能力，该 Starter 用于构建基于 Thymeleaf 视图的 Spring MVC Web 应用。Spring Boot 的 Starter 机制会集中管理一组经过验证的依赖版本，通常不需要在每个 Spring 相关依赖上手动指定版本。([Home](https://docs.spring.io/spring-boot/3.5/reference/using/build-systems.html))

在 Spring Boot 3 项目中，只需要引入 Web 与 Thymeleaf Starter，Controller 返回模板名称即可完成页面跳转。例如返回 `"index"` 时，默认会解析到 `classpath:/templates/index.html`。Thymeleaf 的 Spring 集成支持 Spring MVC Controller 视图跳转、Spring EL 表达式、表单绑定、校验错误回显、国际化消息和 Spring 资源解析机制。([thymeleaf.org](https://www.thymeleaf.org/doc/tutorials/3.1/thymeleafspring.html?utm_source=chatgpt.com))

基础访问示例：

```text
请求地址：GET /hello
Controller 返回：hello
实际模板：src/main/resources/templates/hello.html
浏览器输出：渲染后的 HTML 页面
```

在工程中通常按下面方式组织页面访问：

```java
package io.github.atengk.thymeleaf.controller;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Thymeleaf 示例页面控制器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Controller
public class HelloController {

    /**
     * 进入 hello 页面
     *
     * @param name 页面显示名称
     * @param model 页面数据模型
     * @return 模板视图名称
     */
    @GetMapping("/hello")
    public String hello(@RequestParam(required = false) String name, Model model) {
        String username = StrUtil.blankToDefault(name, "Thymeleaf");
        log.info("访问 Thymeleaf 示例页面，用户名：{}", username);
        model.addAttribute("username", username);
        return "hello";
    }
}
```

对应模板文件：

文件位置：`src/main/resources/templates/hello.html`

```html
<!DOCTYPE html>
<html lang="zh-CN" xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>Thymeleaf 示例页面</title>
</head>
<body>
<h1 th:text="'你好，' + ${username}">你好，Thymeleaf</h1>
</body>
</html>
```

### 适用场景

Thymeleaf 适合以服务端渲染为主的 Spring Boot Web 系统，尤其适合后台管理、内部运营平台、表单页面、数据列表页面、详情页、低复杂度门户页面，以及需要较好 SEO 基础的普通 HTML 页面。

适合使用 Thymeleaf 的场景包括：

| 场景         | 说明                                                         |
| ------------ | ------------------------------------------------------------ |
| 后台管理系统 | 页面结构稳定，表单、表格、查询条件较多，服务端渲染开发成本低 |
| 内部业务系统 | 用户量可控，交互复杂度中等，不需要完整前后端分离             |
| 表单流程页面 | 适合参数绑定、校验错误回显、CSRF 表单处理                    |
| 数据展示页面 | 适合列表、详情、分页、条件查询等传统 MVC 页面                |
| 原型协作页面 | Thymeleaf 模板可作为自然 HTML 预览，便于页面原型维护         |

不建议优先使用 Thymeleaf 的场景包括高交互 SPA、复杂前端状态管理、大量组件化交互、移动端 H5 应用、需要独立前端团队长期维护的系统。这类场景通常更适合 Vue、React 或 Angular 与 REST API 的前后端分离架构。

## 开发环境准备

本节给出 Spring Boot 3 与 Thymeleaf 项目的基础环境要求、Maven 依赖和推荐目录结构。完成本节配置后，即可继续开发模板页面、Controller 跳转和表单功能。

### JDK 与 Spring Boot 版本

Spring Boot 3.5.14 要求至少 Java 17，兼容到 Java 25，并要求 Spring Framework 6.2.18 或以上版本；Maven 需要 3.6.3 或以上版本。当前 Spring 官方文档同时提示最新稳定主线已进入 Spring Boot 4.x，但本文主题是 Spring Boot 3，因此示例版本使用 `3.5.14`。([Home](https://docs.spring.io/spring-boot/3.5/system-requirements.html))

推荐环境如下：

| 工具        | 推荐版本      | 说明                                                      |
| ----------- | ------------- | --------------------------------------------------------- |
| JDK         | 17 或 21      | Spring Boot 3 最低要求 Java 17，生产项目建议使用 LTS 版本 |
| Spring Boot | 3.5.14        | Spring Boot 3 当前稳定分支示例版本                        |
| Maven       | 3.6.3+        | 用于依赖管理、编译、打包和运行                            |
| IDE         | IntelliJ IDEA | 推荐启用 Lombok、Maven、Spring Boot、Thymeleaf 相关支持   |
| 编码        | UTF-8         | 避免页面中文、配置文件和日志乱码                          |

可以使用以下命令检查本地环境：

```bash
java -version
mvn -version
```

如果 `java -version` 输出的主版本低于 17，需要先升级 JDK；如果 `mvn -version` 无法执行，需要先安装 Maven 并配置 `PATH`。

### Maven 依赖配置

本节给出一个可直接用于 Spring Boot 3 + Thymeleaf 的 `pom.xml` 示例。该配置包含 Web、Thymeleaf、Validation、Lombok、Hutool 和测试依赖，其中 Hutool 用于常见字符串、日期、集合、对象处理等工具方法。Hutool 5.8.x 当前可用稳定版本示例使用 `5.8.44`。([Maven Repository](https://mvnrepository.com/artifact/cn.hutool/hutool-all/versions?utm_source=chatgpt.com))

文件位置：`pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="
             http://maven.apache.org/POM/4.0.0
             https://maven.apache.org/xsd/maven-4.0.0.xsd">

    <modelVersion>4.0.0</modelVersion>

    <!-- Spring Boot 父工程：统一管理 Spring 生态依赖版本和 Maven 插件版本 -->
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.5.14</version>
        <relativePath/>
    </parent>

    <groupId>io.github.atengk</groupId>
    <artifactId>springboot3-thymeleaf-demo</artifactId>
    <version>1.0.0</version>
    <name>springboot3-thymeleaf-demo</name>
    <description>Spring Boot 3 Thymeleaf 示例项目</description>

    <properties>
        <!-- Spring Boot 3 最低要求 Java 17，生产环境建议使用 17 或 21 LTS -->
        <java.version>17</java.version>

        <!-- Hutool 工具类版本，Spring Boot 不托管该依赖版本，需要显式声明 -->
        <hutool.version>5.8.44</hutool.version>
    </properties>

    <dependencies>
        <!-- Spring MVC Web 支持，内置 Tomcat，提供 Controller、静态资源、JSON 等能力 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Thymeleaf 模板引擎支持，用于服务端渲染 HTML 页面 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-thymeleaf</artifactId>
        </dependency>

        <!-- 参数校验支持，常用于表单提交、DTO 校验和错误信息回显 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <!-- Hutool 工具类库，简化字符串、日期、集合、对象等常见处理 -->
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
            <version>${hutool.version}</version>
        </dependency>

        <!-- Lombok 简化实体类、日志对象和构造器代码；IDE 需启用 Lombok 插件 -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Spring Boot 测试依赖，包含 JUnit Jupiter、MockMvc 等测试能力 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <!-- Spring Boot Maven 插件，用于打包可执行 Jar 和本地运行 -->
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

依赖配置完成后，可以执行以下命令检查依赖是否正常解析：

```bash
mvn dependency:tree
mvn clean package
```

`mvn dependency:tree` 用于查看依赖树，确认 `spring-boot-starter-thymeleaf`、`spring-boot-starter-web`、`hutool-all` 是否已被正确引入；`mvn clean package` 用于执行完整编译和打包，确认项目基础配置没有语法或依赖问题。

### 项目目录结构

Spring Boot 3 + Thymeleaf 项目建议按 MVC 分层组织 Java 代码，并将模板页面、静态资源和配置文件放在 Spring Boot 默认约定目录下。默认约定能减少额外配置，便于后续章节直接围绕页面、Controller、表单和静态资源展开。

推荐目录结构如下：

```text
springboot3-thymeleaf-demo
├── pom.xml
├── src
│   ├── main
│   │   ├── java
│   │   │   └── io
│   │   │       └── github
│   │   │           └── atengk
│   │   │               └── thymeleaf
│   │   │                   ├── ThymeleafApplication.java
│   │   │                   ├── controller
│   │   │                   │   └── HelloController.java
│   │   │                   ├── service
│   │   │                   ├── model
│   │   │                   │   ├── dto
│   │   │                   │   └── vo
│   │   │                   └── config
│   │   └── resources
│   │       ├── application.yml
│   │       ├── templates
│   │       │   ├── hello.html
│   │       │   ├── index.html
│   │       │   ├── layout
│   │       │   │   ├── header.html
│   │       │   │   ├── footer.html
│   │       │   │   └── menu.html
│   │       │   └── user
│   │       │       ├── list.html
│   │       │       ├── add.html
│   │       │       └── edit.html
│   │       ├── static
│   │       │   ├── css
│   │       │   │   └── app.css
│   │       │   ├── js
│   │       │   │   └── app.js
│   │       │   └── images
│   │       └── messages.properties
│   └── test
│       └── java
│           └── io
│               └── github
│                   └── atengk
│                       └── thymeleaf
│                           └── ThymeleafApplicationTests.java
```

核心目录说明：

| 路径                           | 作用                                                     |
| ------------------------------ | -------------------------------------------------------- |
| `src/main/java`                | Java 源码目录，存放启动类、Controller、Service、配置类等 |
| `controller`                   | 页面请求入口，负责返回模板视图名和写入 `Model` 数据      |
| `service`                      | 业务逻辑层，处理页面展示所需的业务数据                   |
| `model/dto`                    | 表单提交、查询条件等入参对象                             |
| `model/vo`                     | 页面展示对象，面向 Thymeleaf 模板输出                    |
| `config`                       | Web、国际化、安全、模板等配置类                          |
| `src/main/resources/templates` | Thymeleaf 模板目录                                       |
| `src/main/resources/static`    | 静态资源目录，存放 CSS、JavaScript、图片                 |
| `application.yml`              | Spring Boot 配置文件                                     |
| `messages.properties`          | 国际化资源文件                                           |

启动类示例：

文件位置：`src/main/java/io/github/atengk/thymeleaf/ThymeleafApplication.java`

```java
package io.github.atengk.thymeleaf;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Thymeleaf 示例项目启动类
 *
 * @author Ateng
 * @since 2026-05-06
 */
@SpringBootApplication
public class ThymeleafApplication {

    /**
     * 应用启动入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(ThymeleafApplication.class, args);
    }
}
```

基础配置文件示例：

文件位置：`src/main/resources/application.yml`

```yaml
server:
  # Web 服务端口
  port: 8080

spring:
  application:
    # 应用名称
    name: springboot3-thymeleaf-demo

  thymeleaf:
    # 模板文件默认目录
    prefix: classpath:/templates/

    # 模板文件默认后缀
    suffix: .html

    # 模板编码
    encoding: UTF-8

    # 模板模式，HTML 是常见页面模式
    mode: HTML

    # 开发阶段建议关闭缓存，生产环境建议开启
    cache: false
```

完成上述结构后，可以启动应用并访问：

```bash
mvn spring-boot:run
```

访问地址：

```text
http://localhost:8080/hello?name=Ateng
```

如果页面正常显示 `你好，Ateng`，说明 Spring Boot 3、Thymeleaf 模板目录、Controller 视图解析和基础依赖均已配置成功。



## 基础配置

本节说明 Thymeleaf 在 Spring Boot 3 项目中的基础配置方式。Spring Boot 对 Thymeleaf、Spring MVC、静态资源访问和模板缓存都提供了默认约定，常规项目只需要引入依赖并按默认目录放置文件即可运行，只有在路径、缓存、资源访问规则需要调整时才需要显式配置。Spring Boot 对 Thymeleaf、FreeMarker、Mustache 等模板引擎提供自动配置，并且默认会从 `src/main/resources/templates` 加载模板文件。([Home](https://docs.spring.io/spring-boot/reference/web/servlet.html?utm_source=chatgpt.com))

### Thymeleaf 自动配置

Thymeleaf 自动配置用于减少手动声明 `TemplateResolver`、`TemplateEngine` 和视图解析器的工作。只要项目引入 `spring-boot-starter-thymeleaf`，并且处于 Spring MVC Web 环境中，Spring Boot 会自动装配 Thymeleaf 相关组件，Controller 返回的视图名称会被解析为对应的 HTML 模板。

默认情况下，Controller 返回 `"index"` 时，会按照 Thymeleaf 的模板前缀和后缀解析为：

```text
classpath:/templates/index.html
```

Spring Boot 的 Thymeleaf 配置属性统一使用 `spring.thymeleaf` 前缀，常用属性包括 `prefix`、`suffix`、`mode`、`encoding`、`cache`、`enabled` 等。官方 `ThymeleafProperties` 类也明确使用 `spring.thymeleaf` 作为配置前缀。([Home](https://docs.spring.io/spring-boot/docs/current/api/org/springframework/boot/autoconfigure/thymeleaf/ThymeleafProperties.html?utm_source=chatgpt.com))

基础配置放在 `application.yml` 中。

文件位置：`src/main/resources/application.yml`

```yaml
server:
  # Web 服务端口
  port: 8080

spring:
  application:
    # 应用名称
    name: springboot3-thymeleaf-demo

  thymeleaf:
    # 是否启用 Thymeleaf 模板引擎
    enabled: true

    # 模板文件所在目录
    prefix: classpath:/templates/

    # 模板文件后缀
    suffix: .html

    # 模板编码
    encoding: UTF-8

    # 模板模式，常规 HTML 页面使用 HTML
    mode: HTML

    # 开发环境建议关闭缓存，方便修改模板后立即生效
    cache: false
```

可以通过一个简单 Controller 验证自动配置是否生效。

文件位置：`src/main/java/io/github/atengk/thymeleaf/controller/IndexController.java`

```java
package io.github.atengk.thymeleaf.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 首页控制器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Controller
public class IndexController {

    /**
     * 进入首页
     *
     * @return 首页模板视图
     */
    @GetMapping("/")
    public String index() {
        log.info("访问 Thymeleaf 首页");
        return "index";
    }
}
```

对应模板文件如下。

文件位置：`src/main/resources/templates/index.html`

```html
<!DOCTYPE html>
<html lang="zh-CN" xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>首页</title>
</head>
<body>
<h1>Spring Boot 3 Thymeleaf 首页</h1>
</body>
</html>
```

启动项目后访问：

```text
http://localhost:8080/
```

如果页面能够正常显示，说明 Thymeleaf Starter、自动配置、模板目录和视图解析流程已经生效。

### 模板路径配置

模板路径配置用于指定 Thymeleaf 从哪里加载页面模板。Spring Boot 默认模板路径为 `classpath:/templates/`，默认模板后缀为 `.html`，因此大多数项目不需要修改。官方配置属性中，`spring.thymeleaf.prefix` 的默认值是 `classpath:/templates/`，`spring.thymeleaf.suffix` 的默认值是 `.html`。([Home](https://docs.spring.io/spring-boot/appendix/application-properties/index.html?utm_source=chatgpt.com))

默认目录结构如下：

```text
src/main/resources/templates
├── index.html
├── hello.html
├── user
│   ├── list.html
│   ├── add.html
│   └── edit.html
└── layout
    ├── header.html
    ├── footer.html
    └── menu.html
```

Controller 返回视图名时，不需要写模板前缀和后缀：

```java
return "user/list";
```

上面的返回值会被解析为：

```text
classpath:/templates/user/list.html
```

如果项目希望将模板目录调整为 `views`，可以修改如下配置。

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  thymeleaf:
    # 将模板目录从 templates 调整为 views
    prefix: classpath:/views/

    # 保持 HTML 模板后缀
    suffix: .html
```

调整后目录结构应同步修改：

```text
src/main/resources/views
├── index.html
└── user
    └── list.html
```

除非项目已有统一目录规范，否则建议保持 Spring Boot 默认的 `templates` 目录。默认约定更利于团队协作，也能减少后续模板路径配置错误。

### 静态资源路径配置

静态资源路径配置用于管理 CSS、JavaScript、图片、字体等无需经过 Thymeleaf 渲染的资源。Spring Boot 默认会将静态资源映射到 `/**`，并从 `classpath:/META-INF/resources/`、`classpath:/resources/`、`classpath:/static/`、`classpath:/public/` 等位置加载资源。静态资源访问路径也可以通过 `spring.mvc.static-path-pattern` 调整，资源物理位置可以通过 `spring.web.resources.static-locations` 调整。([Home](https://docs.spring.io/spring-boot/reference/web/servlet.html?utm_source=chatgpt.com))

推荐静态资源目录如下：

```text
src/main/resources/static
├── css
│   └── app.css
├── js
│   └── app.js
└── images
    └── logo.png
```

默认情况下，资源访问路径如下：

```text
src/main/resources/static/css/app.css       -> http://localhost:8080/css/app.css
src/main/resources/static/js/app.js         -> http://localhost:8080/js/app.js
src/main/resources/static/images/logo.png   -> http://localhost:8080/images/logo.png
```

Thymeleaf 页面中建议使用 `@{...}` 链接表达式引用静态资源。Thymeleaf 的链接表达式使用 `@{...}`，可以生成上下文相关 URL，也支持查询参数、路径变量和表单提交地址。([thymeleaf.org](https://www.thymeleaf.org/doc/articles/standardurlsyntax.html?utm_source=chatgpt.com))

文件位置：`src/main/resources/templates/index.html`

```html
<!DOCTYPE html>
<html lang="zh-CN" xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>静态资源示例</title>

    <!-- 引入 CSS 静态资源 -->
    <link rel="stylesheet" th:href="@{/css/app.css}">
</head>
<body>
<h1>静态资源访问示例</h1>

<!-- 引入图片静态资源 -->
<img th:src="@{/images/logo.png}" alt="Logo">

<!-- 引入 JavaScript 静态资源 -->
<script th:src="@{/js/app.js}"></script>
</body>
</html>
```

如果希望所有静态资源统一通过 `/assets/**` 访问，可以配置如下。

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  mvc:
    # 将静态资源访问路径从 /** 调整为 /assets/**
    static-path-pattern: /assets/**
```

调整后访问路径变为：

```text
http://localhost:8080/assets/css/app.css
http://localhost:8080/assets/js/app.js
http://localhost:8080/assets/images/logo.png
```

此时 Thymeleaf 页面中的引用也要同步调整：

```html
<link rel="stylesheet" th:href="@{/assets/css/app.css}">
<script th:src="@{/assets/js/app.js}"></script>
```

如果要改变静态资源实际存放目录，可以配置 `spring.web.resources.static-locations`。

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  web:
    resources:
      # 自定义静态资源目录；配置后会替换默认静态资源位置
      static-locations:
        - classpath:/static/
        - classpath:/assets/
```

一般项目只需要使用默认的 `src/main/resources/static` 即可。只有在历史项目迁移、统一前端资源目录、接入构建产物目录时，才建议自定义静态资源位置。

### 缓存配置

缓存配置用于控制 Thymeleaf 是否缓存解析后的模板。开发环境通常关闭缓存，方便修改 HTML 后立即看到效果；生产环境应开启缓存，避免每次请求都重新解析模板，从而降低页面渲染开销。Spring Boot 的 `spring.thymeleaf.cache` 属性用于控制模板缓存，官方配置项中该属性默认值为 `true`。([Home](https://docs.spring.io/spring-boot/appendix/application-properties/index.html?utm_source=chatgpt.com))

建议使用多环境配置区分开发和生产。

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  profiles:
    # 默认启用开发环境
    active: dev
```

开发环境关闭模板缓存。

文件位置：`src/main/resources/application-dev.yml`

```yaml
spring:
  thymeleaf:
    # 开发环境关闭缓存，修改页面后便于立即验证
    cache: false
```

生产环境开启模板缓存。

文件位置：`src/main/resources/application-prod.yml`

```yaml
spring:
  thymeleaf:
    # 生产环境开启缓存，提高模板渲染性能
    cache: true
```

启动时可以通过参数指定环境：

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

打包后运行生产环境：

```bash
java -jar target/springboot3-thymeleaf-demo-1.0.0.jar --spring.profiles.active=prod
```

`mvn spring-boot:run -Dspring-boot.run.profiles=dev` 用于本地开发运行，并启用 `application-dev.yml`；`java -jar ... --spring.profiles.active=prod` 用于运行已打包应用，并启用生产环境配置。开发阶段如果发现模板修改后页面不变化，优先检查 `spring.thymeleaf.cache` 是否仍为 `true`。

## 页面模板开发

本节说明 Thymeleaf 页面模板的基础写法，包括 HTML 模板结构、变量表达式、链接表达式、条件渲染、循环渲染和片段复用。Thymeleaf 模板是标准 HTML 文件，通过 `th:*` 属性扩展动态渲染能力，因此模板即使不经过服务端渲染，也能保留基本的静态预览能力。Thymeleaf 官方文档将这种方式称为可自然显示的模板能力，片段、表达式和属性处理都围绕标准 HTML 标记展开。([thymeleaf.org](https://www.thymeleaf.org/doc/tutorials/3.1/usingthymeleaf.html?utm_source=chatgpt.com))

### HTML 模板结构

HTML 模板结构是 Thymeleaf 页面开发的基础。每个 Thymeleaf HTML 文件建议声明 HTML5 文档类型、中文语言标识、UTF-8 编码，并在 `<html>` 标签上声明 Thymeleaf 命名空间。

基础模板如下。

文件位置：`src/main/resources/templates/example/basic.html`

```html
<!DOCTYPE html>
<html lang="zh-CN" xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>基础模板结构</title>
</head>
<body>
<h1>基础模板结构</h1>
<p>这是一个 Thymeleaf HTML 页面。</p>
</body>
</html>
```

推荐模板结构如下：

```text
templates
├── index.html
├── example
│   ├── basic.html
│   ├── variable.html
│   ├── link.html
│   ├── condition.html
│   └── loop.html
└── layout
    ├── header.html
    └── footer.html
```

对于业务页面，建议按模块建目录。例如用户相关页面放在 `templates/user/`，订单相关页面放在 `templates/order/`。Controller 返回视图时也按模块路径返回，例如 `user/list`、`order/detail`，这样页面结构和业务模块更容易对应。

### 变量表达式

变量表达式用于读取 Controller 通过 `Model`、`ModelMap`、`ModelAndView` 传入页面的数据。Thymeleaf 使用 `${...}` 读取变量，常用于文本输出、属性赋值、条件判断和循环渲染。Thymeleaf 标准表达式体系包括变量表达式、消息表达式、链接表达式、片段表达式等，页面中最常用的是 `${...}` 和 `@{...}`。([thymeleaf.org](https://www.thymeleaf.org/doc/tutorials/3.1/usingthymeleaf.html?utm_source=chatgpt.com))

先准备一个页面 Controller，用于向模板传递用户信息。

文件位置：`src/main/java/io/github/atengk/thymeleaf/controller/TemplateExampleController.java`

```java
package io.github.atengk.thymeleaf.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Map;

/**
 * Thymeleaf 模板示例控制器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Controller
public class TemplateExampleController {

    /**
     * 变量表达式示例页面
     *
     * @param model 页面数据模型
     * @return 模板视图名称
     */
    @GetMapping("/example/variable")
    public String variable(Model model) {
        Map<String, Object> user = Map.of(
                "id", 1001L,
                "username", "Ateng",
                "nickname", "阿腾",
                "email", "ateng@example.com",
                "enabled", true
        );

        model.addAttribute("user", user);
        model.addAttribute("now", DateUtil.now());
        log.info("进入变量表达式示例页面，用户ID：{}", user.get("id"));
        return "example/variable";
    }

    /**
     * 列表渲染示例页面
     *
     * @param model 页面数据模型
     * @return 模板视图名称
     */
    @GetMapping("/example/loop")
    public String loop(Model model) {
        List<Map<String, Object>> userList = CollUtil.newArrayList(
                Map.of("id", 1001L, "username", "Ateng", "roleName", "管理员", "enabled", true),
                Map.of("id", 1002L, "username", "Tom", "roleName", "普通用户", "enabled", false),
                Map.of("id", 1003L, "username", "Jerry", "roleName", "审计员", "enabled", true)
        );

        model.addAttribute("userList", userList);
        log.info("进入循环渲染示例页面，用户数量：{}", userList.size());
        return "example/loop";
    }
}
```

变量表达式页面如下。

文件位置：`src/main/resources/templates/example/variable.html`

```html
<!DOCTYPE html>
<html lang="zh-CN" xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>变量表达式</title>
</head>
<body>
<h1>变量表达式示例</h1>

<p>用户ID：<span th:text="${user.id}">1000</span></p>
<p>用户名：<span th:text="${user.username}">username</span></p>
<p>昵称：<span th:text="${user.nickname}">nickname</span></p>
<p>邮箱：<span th:text="${user.email}">email</span></p>
<p>当前时间：<span th:text="${now}">2026-05-06 12:00:00</span></p>

<!-- th:text 会转义 HTML，适合输出普通文本 -->
<p th:text="'欢迎你，' + ${user.nickname}">欢迎语</p>
</body>
</html>
```

访问地址：

```text
http://localhost:8080/example/variable
```

常用变量输出方式如下：

| 写法                         | 说明                                       |
| ---------------------------- | ------------------------------------------ |
| `${user}`                    | 读取整个对象                               |
| `${user.id}`                 | 读取对象属性或 Map 中的 `id`               |
| `${user.username}`           | 读取对象属性或 Map 中的 `username`         |
| `th:text="${user.username}"` | 输出文本，并自动进行 HTML 转义             |
| `th:utext="${htmlContent}"`  | 输出未转义 HTML，存在 XSS 风险，需谨慎使用 |

实际项目中，普通文本优先使用 `th:text`。只有内容来源可信并且确实需要渲染 HTML 标签时，才考虑使用 `th:utext`。

### 链接表达式

链接表达式用于生成页面跳转地址、静态资源地址和表单提交地址。Thymeleaf 使用 `@{...}` 表示链接表达式，支持上下文路径、查询参数、路径变量和动态参数。官方文档说明，`@{...}` 不仅能用于 `th:href`，也能用于 `th:src`、`th:action` 和其他表达式位置。([thymeleaf.org](https://www.thymeleaf.org/doc/articles/standardurlsyntax.html?utm_source=chatgpt.com))

链接表达式常见写法如下。

文件位置：`src/main/resources/templates/example/link.html`

```html
<!DOCTYPE html>
<html lang="zh-CN" xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>链接表达式</title>

    <!-- 静态资源链接 -->
    <link rel="stylesheet" th:href="@{/css/app.css}">
</head>
<body>
<h1>链接表达式示例</h1>

<!-- 普通页面跳转 -->
<p>
    <a th:href="@{/}">返回首页</a>
</p>

<!-- 查询参数：生成 /user/detail?id=1001 -->
<p>
    <a th:href="@{/user/detail(id=${userId})}">查看用户详情</a>
</p>

<!-- 路径变量：生成 /user/1001/detail -->
<p>
    <a th:href="@{/user/{id}/detail(id=${userId})}">查看路径变量详情</a>
</p>

<!-- 多参数：生成 /user/list?page=1&size=10 -->
<p>
    <a th:href="@{/user/list(page=1,size=10)}">用户分页列表</a>
</p>

<!-- 表单提交地址 -->
<form th:action="@{/user/save}" method="post">
    <input type="text" name="username" placeholder="请输入用户名">
    <button type="submit">提交</button>
</form>

<!-- JavaScript 静态资源 -->
<script th:src="@{/js/app.js}"></script>
</body>
</html>
```

对应 Controller 增加页面入口。

文件位置：`src/main/java/io/github/atengk/thymeleaf/controller/LinkExampleController.java`

```java
package io.github.atengk.thymeleaf.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 链接表达式示例控制器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Controller
public class LinkExampleController {

    /**
     * 链接表达式示例页面
     *
     * @param model 页面数据模型
     * @return 模板视图名称
     */
    @GetMapping("/example/link")
    public String link(Model model) {
        model.addAttribute("userId", 1001L);
        log.info("进入链接表达式示例页面");
        return "example/link";
    }
}
```

访问地址：

```text
http://localhost:8080/example/link
```

实际开发中，不建议在模板中直接拼接 URL 字符串。优先使用 `@{...}`，这样在应用配置了 `server.servlet.context-path` 或部署到非根路径时，链接仍能按上下文路径正确生成。

### 条件渲染

条件渲染用于根据服务端数据决定页面元素是否显示。Thymeleaf 常用 `th:if` 和 `th:unless` 处理条件展示，其中 `th:if` 表示条件成立时渲染，`th:unless` 表示条件不成立时渲染。条件表达式通常结合布尔值、对象是否为空、字符串状态码、用户权限标识等使用。Thymeleaf 的条件、表达式和属性处理都在模板执行阶段完成，最终输出给浏览器的是普通 HTML。([thymeleaf.org](https://www.thymeleaf.org/doc/tutorials/3.1/usingthymeleaf.html?utm_source=chatgpt.com))

条件渲染页面如下。

文件位置：`src/main/resources/templates/example/condition.html`

```html
<!DOCTYPE html>
<html lang="zh-CN" xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>条件渲染</title>
</head>
<body>
<h1>条件渲染示例</h1>

<!-- 根据布尔值判断 -->
<p th:if="${user.enabled}">用户状态：启用</p>
<p th:unless="${user.enabled}">用户状态：禁用</p>

<!-- 根据角色判断 -->
<div th:if="${user.roleCode == 'ADMIN'}">
    <button type="button">系统配置</button>
    <button type="button">用户管理</button>
</div>

<!-- 根据数据是否为空判断 -->
<div th:if="${message != null}">
    <p th:text="${message}">提示信息</p>
</div>

<!-- 三元表达式 -->
<p th:text="${user.enabled} ? '当前账号可用' : '当前账号不可用'">账号状态</p>
</body>
</html>
```

对应 Controller 如下。

文件位置：`src/main/java/io/github/atengk/thymeleaf/controller/ConditionExampleController.java`

```java
package io.github.atengk.thymeleaf.controller;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

/**
 * 条件渲染示例控制器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Controller
public class ConditionExampleController {

    /**
     * 条件渲染示例页面
     *
     * @param model 页面数据模型
     * @return 模板视图名称
     */
    @GetMapping("/example/condition")
    public String condition(Model model) {
        Map<String, Object> user = Map.of(
                "id", 1001L,
                "username", "Ateng",
                "roleCode", "ADMIN",
                "enabled", true
        );

        String message = StrUtil.format("欢迎管理员：{}", user.get("username"));
        model.addAttribute("user", user);
        model.addAttribute("message", message);
        log.info("进入条件渲染示例页面，角色：{}", user.get("roleCode"));
        return "example/condition";
    }
}
```

访问地址：

```text
http://localhost:8080/example/condition
```

权限按钮、菜单项、操作列按钮可以使用条件渲染控制显示。但需要注意，前端隐藏不等于后端鉴权。涉及新增、修改、删除、导出等敏感操作时，Controller 或安全框架中仍然必须进行权限校验。

### 循环渲染

循环渲染用于展示列表数据，例如用户列表、订单列表、菜单列表和字典选项。Thymeleaf 使用 `th:each` 遍历集合，也可以通过状态变量获取序号、索引、奇偶行、是否第一条、是否最后一条等信息。Thymeleaf 3.1 也支持对 Java Stream 进行迭代。([thymeleaf.org](https://www.thymeleaf.org/doc/articles/thymeleaf31whatsnew.html?utm_source=chatgpt.com))

列表页面如下。

文件位置：`src/main/resources/templates/example/loop.html`

```html
<!DOCTYPE html>
<html lang="zh-CN" xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>循环渲染</title>
</head>
<body>
<h1>循环渲染示例</h1>

<table border="1" cellpadding="8" cellspacing="0">
    <thead>
    <tr>
        <th>序号</th>
        <th>用户ID</th>
        <th>用户名</th>
        <th>角色</th>
        <th>状态</th>
        <th>操作</th>
    </tr>
    </thead>
    <tbody>
    <tr th:each="user, stat : ${userList}">
        <td th:text="${stat.count}">1</td>
        <td th:text="${user.id}">1001</td>
        <td th:text="${user.username}">Ateng</td>
        <td th:text="${user.roleName}">管理员</td>
        <td th:text="${user.enabled} ? '启用' : '禁用'">启用</td>
        <td>
            <a th:href="@{/user/{id}/detail(id=${user.id})}">详情</a>
            <a th:href="@{/user/{id}/edit(id=${user.id})}">编辑</a>
        </td>
    </tr>

    <!-- 空列表兜底展示 -->
    <tr th:if="${#lists.isEmpty(userList)}">
        <td colspan="6">暂无用户数据</td>
    </tr>
    </tbody>
</table>
</body>
</html>
```

`th:each` 状态变量常用属性如下：

| 属性         | 说明            |
| ------------ | --------------- |
| `stat.index` | 从 0 开始的索引 |
| `stat.count` | 从 1 开始的序号 |
| `stat.size`  | 集合总数        |
| `stat.first` | 是否第一条      |
| `stat.last`  | 是否最后一条    |
| `stat.odd`   | 是否奇数行      |
| `stat.even`  | 是否偶数行      |

访问地址：

```text
http://localhost:8080/example/loop
```

循环渲染时建议始终处理空列表场景。后台返回空集合比返回 `null` 更适合模板渲染，能减少页面空指针判断和异常分支。

### 片段复用

片段复用用于抽取公共页面结构，例如头部、底部、菜单、面包屑、搜索区域和分页组件。Thymeleaf 使用 `th:fragment` 定义片段，使用 `th:insert` 或 `th:replace` 引入片段。官方文档说明，`th:insert` 会把片段插入到宿主标签内部，`th:replace` 会用片段替换宿主标签本身；Thymeleaf 3.1 中也建议使用 `th:insert` 和 `th:replace`，不要继续使用旧的 `th:include`。([thymeleaf.org](https://www.thymeleaf.org/doc/tutorials/3.1/usingthymeleaf.html?utm_source=chatgpt.com))

公共头部片段如下。

文件位置：`src/main/resources/templates/layout/header.html`

```html
<!DOCTYPE html>
<html lang="zh-CN" xmlns:th="http://www.thymeleaf.org">
<body>
<header th:fragment="header(activeMenu)">
    <h1>Spring Boot 3 Thymeleaf 示例系统</h1>

    <nav>
        <a th:href="@{/}" th:classappend="${activeMenu == 'home'} ? ' active' : ''">首页</a>
        <a th:href="@{/example/variable}" th:classappend="${activeMenu == 'variable'} ? ' active' : ''">变量表达式</a>
        <a th:href="@{/example/loop}" th:classappend="${activeMenu == 'loop'} ? ' active' : ''">循环渲染</a>
    </nav>

    <hr>
</header>
</body>
</html>
```

公共底部片段如下。

文件位置：`src/main/resources/templates/layout/footer.html`

```html
<!DOCTYPE html>
<html lang="zh-CN" xmlns:th="http://www.thymeleaf.org">
<body>
<footer th:fragment="footer">
    <hr>
    <p>© 2026 Spring Boot 3 Thymeleaf 示例系统</p>
</footer>
</body>
</html>
```

业务页面中引入公共片段。

文件位置：`src/main/resources/templates/example/fragment.html`

```html
<!DOCTYPE html>
<html lang="zh-CN" xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>片段复用</title>
    <link rel="stylesheet" th:href="@{/css/app.css}">
</head>
<body>

<!-- 使用 th:replace 替换当前 header 标签 -->
<header th:replace="~{layout/header :: header('home')}"></header>

<main>
    <h2>片段复用示例</h2>
    <p>当前页面复用了公共头部和公共底部。</p>
</main>

<!-- 使用 th:insert 将 footer 片段插入当前 div 内部 -->
<div th:insert="~{layout/footer :: footer}"></div>

</body>
</html>
```

对应 Controller 如下。

文件位置：`src/main/java/io/github/atengk/thymeleaf/controller/FragmentExampleController.java`

```java
package io.github.atengk.thymeleaf.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 片段复用示例控制器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Controller
public class FragmentExampleController {

    /**
     * 片段复用示例页面
     *
     * @return 模板视图名称
     */
    @GetMapping("/example/fragment")
    public String fragment() {
        log.info("进入片段复用示例页面");
        return "example/fragment";
    }
}
```

访问地址：

```text
http://localhost:8080/example/fragment
```

片段复用建议遵循以下原则：

| 建议                  | 说明                                                         |
| --------------------- | ------------------------------------------------------------ |
| 公共区域独立文件      | 头部、底部、菜单放在 `templates/layout/`                     |
| 片段名称语义化        | 例如 `header`、`footer`、`menu`、`pagination`                |
| 优先使用 `th:replace` | 页面布局中通常希望直接替换宿主标签                           |
| 参数保持简单          | 片段参数适合传递当前菜单、标题、状态，不适合承载复杂业务逻辑 |
| 业务逻辑放在后端      | 模板负责展示，不建议在模板中写复杂判断链                     |

完成本节后，项目已经具备 Thymeleaf 页面开发的基础能力：可以通过配置控制模板路径、静态资源和缓存策略，也可以在页面中完成变量输出、链接生成、条件判断、循环展示和公共布局复用。



## Controller 页面跳转

本节说明 Spring Boot 3 中如何通过 `@Controller` 返回 Thymeleaf 模板页面。与 `@RestController` 返回 JSON 不同，`@Controller` 更适合传统 MVC 页面开发，方法返回值通常表示模板视图名称，由 Thymeleaf 视图解析器解析为 `templates` 目录下的 HTML 文件。

### 返回模板视图

返回模板视图是 Thymeleaf 页面跳转的基础。Controller 方法返回字符串时，如果方法没有 `@ResponseBody`，该字符串会被当作视图名称处理。例如返回 `controller/view`，默认会解析到 `src/main/resources/templates/controller/view.html`。

完整示例 Controller 如下。

文件位置：`src/main/java/io/github/atengk/thymeleaf/controller/PageJumpController.java`

```java
package io.github.atengk.thymeleaf.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

/**
 * 页面跳转示例控制器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Controller
public class PageJumpController {

    /**
     * 返回普通模板视图
     *
     * @return 模板视图名称
     */
    @GetMapping("/controller/view")
    public String view() {
        log.info("访问普通模板视图页面");
        return "controller/view";
    }

    /**
     * 重定向到普通模板页面
     *
     * @return 重定向地址
     */
    @GetMapping("/controller/redirect")
    public String redirect() {
        log.info("执行页面重定向");
        return "redirect:/controller/view";
    }

    /**
     * Model 数据传递示例
     *
     * @param model 页面数据模型
     * @return 模板视图名称
     */
    @GetMapping("/controller/model")
    public String model(Model model) {
        model.addAttribute("title", "Model 数据传递示例");
        model.addAttribute("username", "Ateng");
        model.addAttribute("message", "这是从 Controller 传递到 Thymeleaf 页面的数据");
        log.info("访问 Model 数据传递页面");
        return "controller/model";
    }

    /**
     * 路径参数处理示例
     *
     * @param id 用户ID
     * @param model 页面数据模型
     * @return 模板视图名称
     */
    @GetMapping("/controller/user/{id}")
    public String pathVariable(@PathVariable Long id, Model model) {
        Map<String, Object> user = Map.of(
                "id", id,
                "username", StrUtil.format("user{}", id),
                "nickname", "示例用户",
                "enabled", true
        );

        model.addAttribute("user", user);
        log.info("访问用户详情页面，用户ID：{}", id);
        return "controller/user-detail";
    }

    /**
     * 表单查询参数处理示例
     *
     * @param keyword 查询关键字
     * @param enabled 启用状态
     * @param page 当前页码
     * @param model 页面数据模型
     * @return 模板视图名称
     */
    @GetMapping("/controller/search")
    public String search(@RequestParam(required = false) String keyword,
                         @RequestParam(required = false) Boolean enabled,
                         @RequestParam(defaultValue = "1") Integer page,
                         Model model) {
        String trimKeyword = StrUtil.trimToEmpty(keyword);
        List<Map<String, Object>> userList = buildUserList().stream()
                .filter(item -> StrUtil.isBlank(trimKeyword)
                        || StrUtil.containsIgnoreCase(String.valueOf(item.get("username")), trimKeyword)
                        || StrUtil.containsIgnoreCase(String.valueOf(item.get("nickname")), trimKeyword))
                .filter(item -> enabled == null || enabled.equals(item.get("enabled")))
                .toList();

        model.addAttribute("keyword", trimKeyword);
        model.addAttribute("enabled", enabled);
        model.addAttribute("page", page);
        model.addAttribute("userList", userList);

        log.info("执行用户查询，关键字：{}，启用状态：{}，页码：{}，结果数量：{}",
                trimKeyword, enabled, page, userList.size());
        return "controller/search";
    }

    /**
     * 构建示例用户列表
     *
     * @return 用户列表
     */
    private List<Map<String, Object>> buildUserList() {
        return CollUtil.newArrayList(
                Map.of("id", 1001L, "username", "Ateng", "nickname", "阿腾", "enabled", true),
                Map.of("id", 1002L, "username", "Tom", "nickname", "汤姆", "enabled", false),
                Map.of("id", 1003L, "username", "Jerry", "nickname", "杰瑞", "enabled", true)
        );
    }
}
```

普通视图页面如下。

文件位置：`src/main/resources/templates/controller/view.html`

```html
<!DOCTYPE html>
<html lang="zh-CN" xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>普通模板视图</title>
</head>
<body>
<h1>普通模板视图</h1>
<p>当前页面由 Controller 返回模板视图名称后渲染。</p>

<p>
    <a th:href="@{/controller/model}">查看 Model 数据传递示例</a>
</p>
</body>
</html>
```

需要注意，`@Controller` 和 `@RestController` 的语义不同：

| 类型              | 返回值含义         | 适用场景                                |
| ----------------- | ------------------ | --------------------------------------- |
| `@Controller`     | 默认作为视图名称   | Thymeleaf、JSP 等服务端页面             |
| `@RestController` | 默认作为响应体     | REST API、JSON 接口                     |
| `@ResponseBody`   | 当前方法返回响应体 | 在 `@Controller` 中单独返回 JSON 或文本 |

在 Thymeleaf 页面开发中，页面跳转 Controller 应使用 `@Controller`，不要使用 `@RestController`。

### Model 数据传递

Model 数据传递用于把后端数据放入页面上下文，供 Thymeleaf 模板通过 `${...}` 表达式读取。常用方式包括 `Model`、`ModelMap` 和 `ModelAndView`，其中 `Model` 最常见，写法简洁，适合大多数页面渲染场景。

页面模板如下。

文件位置：`src/main/resources/templates/controller/model.html`

```html
<!DOCTYPE html>
<html lang="zh-CN" xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title th:text="${title}">Model 数据传递</title>
</head>
<body>
<h1 th:text="${title}">标题</h1>

<p>用户名：<span th:text="${username}">username</span></p>
<p>提示信息：<span th:text="${message}">message</span></p>

<p>
    <a th:href="@{/controller/view}">返回普通视图页面</a>
</p>
</body>
</html>
```

访问地址：

```text
http://localhost:8080/controller/model
```

Model 中的数据只在本次请求内有效。如果需要重定向后仍然传递提示消息，应使用 `RedirectAttributes` 的 `addFlashAttribute`，而不是普通的 `model.addAttribute`。

### 路径参数处理

路径参数处理用于接收 URL 路径中的动态变量，例如详情页、编辑页、删除确认页等。Spring MVC 使用 `@PathVariable` 绑定路径参数，Thymeleaf 页面通过 `Model` 获取后端查询到的数据。

页面模板如下。

文件位置：`src/main/resources/templates/controller/user-detail.html`

```html
<!DOCTYPE html>
<html lang="zh-CN" xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>用户详情</title>
</head>
<body>
<h1>用户详情</h1>

<table border="1" cellpadding="8" cellspacing="0">
    <tr>
        <th>用户ID</th>
        <td th:text="${user.id}">1001</td>
    </tr>
    <tr>
        <th>用户名</th>
        <td th:text="${user.username}">Ateng</td>
    </tr>
    <tr>
        <th>昵称</th>
        <td th:text="${user.nickname}">阿腾</td>
    </tr>
    <tr>
        <th>状态</th>
        <td th:text="${user.enabled} ? '启用' : '禁用'">启用</td>
    </tr>
</table>

<p>
    <a th:href="@{/controller/search}">返回用户查询</a>
</p>
</body>
</html>
```

访问地址：

```text
http://localhost:8080/controller/user/1001
```

在模板中生成路径参数链接时，推荐使用 `@{/user/{id}/detail(id=${user.id})}` 这种写法，而不是手动字符串拼接。这样在上下文路径变化时，链接仍然能正确生成。

### 表单参数处理

表单参数处理通常用于列表查询、条件筛选和简单搜索。查询类表单一般使用 `GET` 方法，这样参数会体现在 URL 中，便于刷新、复制链接和分页跳转。

查询页面如下。

文件位置：`src/main/resources/templates/controller/search.html`

```html
<!DOCTYPE html>
<html lang="zh-CN" xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>表单参数处理</title>
</head>
<body>
<h1>表单参数处理示例</h1>

<form th:action="@{/controller/search}" method="get">
    <label>
        关键字：
        <input type="text" name="keyword" th:value="${keyword}" placeholder="用户名或昵称">
    </label>

    <label>
        状态：
        <select name="enabled">
            <option value="" th:selected="${enabled == null}">全部</option>
            <option value="true" th:selected="${enabled == true}">启用</option>
            <option value="false" th:selected="${enabled == false}">禁用</option>
        </select>
    </label>

    <input type="hidden" name="page" value="1">

    <button type="submit">查询</button>
    <a th:href="@{/controller/search}">重置</a>
</form>

<table border="1" cellpadding="8" cellspacing="0">
    <thead>
    <tr>
        <th>序号</th>
        <th>用户ID</th>
        <th>用户名</th>
        <th>昵称</th>
        <th>状态</th>
        <th>操作</th>
    </tr>
    </thead>
    <tbody>
    <tr th:each="user, stat : ${userList}">
        <td th:text="${stat.count}">1</td>
        <td th:text="${user.id}">1001</td>
        <td th:text="${user.username}">Ateng</td>
        <td th:text="${user.nickname}">阿腾</td>
        <td th:text="${user.enabled} ? '启用' : '禁用'">启用</td>
        <td>
            <a th:href="@{/controller/user/{id}(id=${user.id})}">详情</a>
        </td>
    </tr>

    <tr th:if="${#lists.isEmpty(userList)}">
        <td colspan="6">暂无匹配数据</td>
    </tr>
    </tbody>
</table>
</body>
</html>
```

访问地址示例：

```text
http://localhost:8080/controller/search
http://localhost:8080/controller/search?keyword=Ateng
http://localhost:8080/controller/search?keyword=Ateng&enabled=true&page=1
```

对于查询表单，推荐使用 `@RequestParam` 接收参数；对于新增、编辑等复杂表单，推荐使用 DTO 对象配合 `@ModelAttribute` 绑定参数。

## 表单开发

本节说明 Thymeleaf 表单的完整开发流程，包括表单对象绑定、数据提交、参数校验和错误信息回显。Thymeleaf 与 Spring MVC 集成后，可以通过 `th:object` 和 `th:field` 直接绑定表单对象，并结合 `jakarta.validation` 完成后端参数校验。

### 表单绑定

表单绑定用于将页面表单字段与后端 DTO 对象建立对应关系。Controller 在进入表单页面时，需要向 `Model` 中放入一个表单对象；模板通过 `th:object` 指定绑定对象，通过 `th:field="*{字段名}"` 绑定具体字段。

先创建用户表单 DTO。

文件位置：`src/main/java/io/github/atengk/thymeleaf/model/dto/UserFormDTO.java`

```java
package io.github.atengk.thymeleaf.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户表单参数
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
public class UserFormDTO {

    /**
     * 用户名
     */
    @NotBlank(message = "用户名不能为空")
    @Size(max = 20, message = "用户名长度不能超过20个字符")
    private String username;

    /**
     * 昵称
     */
    @NotBlank(message = "昵称不能为空")
    @Size(max = 30, message = "昵称长度不能超过30个字符")
    private String nickname;

    /**
     * 邮箱
     */
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    /**
     * 年龄
     */
    @NotNull(message = "年龄不能为空")
    @Min(value = 1, message = "年龄不能小于1")
    @Max(value = 120, message = "年龄不能大于120")
    private Integer age;

    /**
     * 角色编码
     */
    @NotBlank(message = "请选择角色")
    private String roleCode;

    /**
     * 是否启用
     */
    @NotNull(message = "请选择启用状态")
    private Boolean enabled;
}
```

创建表单 Controller。

文件位置：`src/main/java/io/github/atengk/thymeleaf/controller/UserFormController.java`

```java
package io.github.atengk.thymeleaf.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.id.NanoId;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.thymeleaf.model.dto.UserFormDTO;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

/**
 * 用户表单示例控制器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Controller
public class UserFormController {

    /**
     * 提供角色下拉选项
     *
     * @return 角色列表
     */
    @ModelAttribute("roleList")
    public List<Map<String, String>> roleList() {
        return CollUtil.newArrayList(
                Map.of("code", "ADMIN", "name", "管理员"),
                Map.of("code", "USER", "name", "普通用户"),
                Map.of("code", "AUDITOR", "name", "审计员")
        );
    }

    /**
     * 进入用户新增表单页面
     *
     * @param model 页面数据模型
     * @return 模板视图名称
     */
    @GetMapping("/form/user/add")
    public String add(Model model) {
        if (!model.containsAttribute("userForm")) {
            UserFormDTO userForm = new UserFormDTO();
            userForm.setEnabled(true);
            model.addAttribute("userForm", userForm);
        }

        log.info("进入用户新增表单页面");
        return "form/user-add";
    }

    /**
     * 保存用户表单
     *
     * @param userForm 表单参数
     * @param bindingResult 参数绑定和校验结果
     * @param redirectAttributes 重定向参数
     * @return 模板视图或重定向地址
     */
    @PostMapping("/form/user/save")
    public String save(@Valid @ModelAttribute("userForm") UserFormDTO userForm,
                       BindingResult bindingResult,
                       RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            log.warn("用户表单校验失败，错误数量：{}", bindingResult.getErrorCount());
            return "form/user-add";
        }

        String userId = NanoId.randomNanoId(10);
        log.info("用户表单提交成功，用户ID：{}，用户名：{}", userId, userForm.getUsername());

        redirectAttributes.addFlashAttribute("message",
                StrUtil.format("用户 {} 保存成功，用户ID：{}", userForm.getUsername(), userId));
        return "redirect:/form/user/success";
    }

    /**
     * 用户保存成功页面
     *
     * @return 模板视图名称
     */
    @GetMapping("/form/user/success")
    public String success() {
        log.info("进入用户保存成功页面");
        return "form/user-success";
    }
}
```

表单页面如下。

文件位置：`src/main/resources/templates/form/user-add.html`

```html
<!DOCTYPE html>
<html lang="zh-CN" xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>用户新增</title>
    <style>
        .form-item {
            margin-bottom: 12px;
        }

        .error {
            color: #c00;
            font-size: 13px;
        }

        .field-error {
            border: 1px solid #c00;
        }
    </style>
</head>
<body>
<h1>用户新增</h1>

<form th:action="@{/form/user/save}" th:object="${userForm}" method="post">
    <div class="form-item">
        <label>
            用户名：
            <input type="text" th:field="*{username}" th:errorclass="field-error" placeholder="请输入用户名">
        </label>
        <div class="error" th:if="${#fields.hasErrors('username')}" th:errors="*{username}"></div>
    </div>

    <div class="form-item">
        <label>
            昵称：
            <input type="text" th:field="*{nickname}" th:errorclass="field-error" placeholder="请输入昵称">
        </label>
        <div class="error" th:if="${#fields.hasErrors('nickname')}" th:errors="*{nickname}"></div>
    </div>

    <div class="form-item">
        <label>
            邮箱：
            <input type="text" th:field="*{email}" th:errorclass="field-error" placeholder="请输入邮箱">
        </label>
        <div class="error" th:if="${#fields.hasErrors('email')}" th:errors="*{email}"></div>
    </div>

    <div class="form-item">
        <label>
            年龄：
            <input type="number" th:field="*{age}" th:errorclass="field-error" placeholder="请输入年龄">
        </label>
        <div class="error" th:if="${#fields.hasErrors('age')}" th:errors="*{age}"></div>
    </div>

    <div class="form-item">
        <label>
            角色：
            <select th:field="*{roleCode}" th:errorclass="field-error">
                <option value="">请选择角色</option>
                <option th:each="role : ${roleList}"
                        th:value="${role.code}"
                        th:text="${role.name}">
                    普通用户
                </option>
            </select>
        </label>
        <div class="error" th:if="${#fields.hasErrors('roleCode')}" th:errors="*{roleCode}"></div>
    </div>

    <div class="form-item">
        启用状态：
        <label>
            <input type="radio" th:field="*{enabled}" value="true"> 启用
        </label>
        <label>
            <input type="radio" th:field="*{enabled}" value="false"> 禁用
        </label>
        <div class="error" th:if="${#fields.hasErrors('enabled')}" th:errors="*{enabled}"></div>
    </div>

    <div class="form-item">
        <button type="submit">保存</button>
        <a th:href="@{/controller/search}">返回列表</a>
    </div>
</form>
</body>
</html>
```

访问地址：

```text
http://localhost:8080/form/user/add
```

表单绑定时需要保证三点：`Model` 中存在 `userForm`，页面 `th:object="${userForm}"` 与后端属性名一致，字段名与 DTO 属性名一致。

### 数据提交

数据提交用于把页面表单内容发送到后端。新增、编辑、删除等会改变服务端状态的操作建议使用 `POST`，查询和筛选建议使用 `GET`。在 Thymeleaf 表单中，`th:action` 用于生成提交地址，`method="post"` 用于指定提交方式。

当前示例的提交流程如下：

```text
GET /form/user/add
   ↓
返回 form/user-add.html
   ↓
填写表单并提交
   ↓
POST /form/user/save
   ↓
后端校验通过
   ↓
redirect:/form/user/success
   ↓
显示保存成功页面
```

保存成功页面如下。

文件位置：`src/main/resources/templates/form/user-success.html`

```html
<!DOCTYPE html>
<html lang="zh-CN" xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>保存成功</title>
</head>
<body>
<h1>保存成功</h1>

<p th:if="${message != null}" th:text="${message}">用户保存成功</p>
<p th:if="${message == null}">操作已完成。</p>

<p>
    <a th:href="@{/form/user/add}">继续新增</a>
    <a th:href="@{/controller/search}">返回列表</a>
</p>
</body>
</html>
```

提交成功后使用 `redirect:/form/user/success`，属于常见的 PRG 模式，即 Post/Redirect/Get。这样可以避免用户刷新页面时重复提交表单。

如果提交成功后需要携带一次性提示信息，推荐使用：

```java
redirectAttributes.addFlashAttribute("message", "保存成功");
```

`addFlashAttribute` 适合重定向后的短期提示信息，例如“保存成功”“删除成功”“操作失败”。如果使用 `model.addAttribute` 后再重定向，普通 Model 数据不会自动进入重定向后的页面上下文。

### 参数校验

参数校验用于保证表单数据在进入业务逻辑前满足基本规则。Spring Boot 3 使用 `jakarta.validation` 包下的校验注解，例如 `@NotBlank`、`@NotNull`、`@Email`、`@Min`、`@Max`、`@Size` 等。

Controller 中启用校验的关键写法如下：

```java
@PostMapping("/form/user/save")
public String save(@Valid @ModelAttribute("userForm") UserFormDTO userForm,
                   BindingResult bindingResult,
                   RedirectAttributes redirectAttributes) {
    if (bindingResult.hasErrors()) {
        return "form/user-add";
    }

    redirectAttributes.addFlashAttribute("message", "保存成功");
    return "redirect:/form/user/success";
}
```

这里有一个关键点：`BindingResult` 必须紧跟在被 `@Valid` 标记的参数后面。如果参数顺序错误，校验失败时可能不会回到表单页面，而是直接抛出异常。

常用校验注解如下：

| 注解        | 适用字段       | 说明                                |
| ----------- | -------------- | ----------------------------------- |
| `@NotBlank` | `String`       | 不能为 `null`，且去除空白后不能为空 |
| `@NotNull`  | 任意对象       | 不能为 `null`                       |
| `@Email`    | `String`       | 必须符合邮箱格式                    |
| `@Size`     | `String`、集合 | 限制长度或元素数量                  |
| `@Min`      | 数字           | 最小值                              |
| `@Max`      | 数字           | 最大值                              |

实际项目中，前端校验只能提升用户体验，不能替代后端校验。所有新增、编辑、导入、提交类请求都应在后端进行参数校验。

### 错误信息回显

错误信息回显用于在表单校验失败后，将用户已经填写的数据和对应错误提示重新展示到页面。Thymeleaf 与 Spring MVC 集成后，可以通过 `#fields.hasErrors(...)` 判断字段是否存在错误，通过 `th:errors="*{字段名}"` 输出错误信息。

当前示例中，校验失败后 Controller 直接返回原表单页面：

```java
if (bindingResult.hasErrors()) {
    log.warn("用户表单校验失败，错误数量：{}", bindingResult.getErrorCount());
    return "form/user-add";
}
```

由于返回的是同一个请求，`BindingResult` 和 `userForm` 会保留在请求上下文中，页面可以自动回显字段值和错误信息。

字段错误回显示例：

```html
<div class="form-item">
    <label>
        用户名：
        <input type="text" th:field="*{username}" th:errorclass="field-error" placeholder="请输入用户名">
    </label>
    <div class="error" th:if="${#fields.hasErrors('username')}" th:errors="*{username}"></div>
</div>
```

全局错误也可以统一展示：

```html
<div class="error" th:if="${#fields.hasGlobalErrors()}">
    <p th:each="err : ${#fields.globalErrors()}" th:text="${err}">全局错误信息</p>
</div>
```

表单错误回显的常见问题如下：

| 问题                     | 原因                                 | 处理方式                                                |
| ------------------------ | ------------------------------------ | ------------------------------------------------------- |
| 页面没有错误提示         | 没有使用 `th:object` 或字段名不一致  | 确认 `th:object="${userForm}"` 与 Controller 参数名一致 |
| 校验失败后直接报错       | `BindingResult` 位置不正确           | `BindingResult` 必须紧跟 `@Valid` 参数                  |
| 下拉框校验失败后没有选项 | 返回页面时没有重新提供选项数据       | 使用 `@ModelAttribute("roleList")` 统一提供下拉数据     |
| 输入值没有回显           | 使用了普通 `value` 而不是 `th:field` | 表单字段优先使用 `th:field`                             |
| 提交成功后刷新重复提交   | POST 后直接返回成功页                | 使用 `redirect:/...` 实现 PRG 模式                      |

完成本节后，项目已经具备 Thymeleaf 页面开发中最核心的后端交互能力：可以从 Controller 返回页面、传递 Model 数据、处理路径参数和查询参数，也可以完成表单绑定、POST 提交、后端校验和错误信息回显。



## 页面布局复用

页面布局复用用于抽取多个页面中重复出现的结构，例如公共头部、底部、菜单、侧边栏、面包屑、分页区域等。Thymeleaf 通过 `th:fragment` 定义页面片段，通过 `th:insert` 或 `th:replace` 引入片段。官方文档中也明确将头部、底部、菜单等公共区域作为模板片段复用的典型场景。([Thymeleaf](https://www.thymeleaf.org/doc/tutorials/3.1/usingthymeleaf.html?utm_source=chatgpt.com))

推荐将公共布局文件集中放在 `templates/layout/` 目录下，业务页面按模块放在对应目录中：

```text
src/main/resources/templates
├── layout
│   ├── header.html
│   ├── footer.html
│   └── menu.html
├── page
│   └── dashboard.html
└── user
    └── list.html
```

### 公共头部

公共头部通常用于放置系统标题、当前登录用户、顶部导航、公共 CSS 引入和页面顶部区域。头部片段可以接收参数，例如当前菜单编码、页面标题、登录用户昵称等。

文件位置：`src/main/resources/templates/layout/header.html`

```html
<!DOCTYPE html>
<html lang="zh-CN" xmlns:th="http://www.thymeleaf.org">
<body>
<header th:fragment="header(pageTitle, activeMenu)">
    <div class="app-header">
        <div class="app-title">
            <a th:href="@{/}" th:text="${pageTitle}">Spring Boot 3 Thymeleaf 示例系统</a>
        </div>

        <nav class="app-nav">
            <a th:href="@{/page/dashboard}"
               th:classappend="${activeMenu == 'dashboard'} ? ' active' : ''">
                控制台
            </a>
            <a th:href="@{/user/list}"
               th:classappend="${activeMenu == 'user'} ? ' active' : ''">
                用户管理
            </a>
            <a th:href="@{/form/user/add}"
               th:classappend="${activeMenu == 'form'} ? ' active' : ''">
                表单示例
            </a>
        </nav>
    </div>
</header>
</body>
</html>
```

这里的 `th:fragment="header(pageTitle, activeMenu)"` 表示定义一个名为 `header` 的片段，并声明两个参数。业务页面引入时可以传入不同标题和菜单编码，从而实现当前菜单高亮。

### 公共底部

公共底部通常用于放置版权信息、系统版本、备案信息、技术支持信息或全局脚本区域。对于中后台系统，底部一般比较稳定，适合作为独立片段维护。

文件位置：`src/main/resources/templates/layout/footer.html`

```html
<!DOCTYPE html>
<html lang="zh-CN" xmlns:th="http://www.thymeleaf.org">
<body>
<footer th:fragment="footer">
    <div class="app-footer">
        <span>© 2026 Spring Boot 3 Thymeleaf 示例系统</span>
        <span>版本：v1.0.0</span>
    </div>
</footer>
</body>
</html>
```

如果多个页面都需要底部，只需要在页面中统一引入该片段。后续修改版权、版本或底部说明时，只需要修改 `footer.html` 一个文件。

### 公共菜单

公共菜单适合承载系统级导航，例如左侧菜单、顶部菜单或模块导航。菜单片段可以接收当前激活菜单编码，根据编码控制样式高亮。

文件位置：`src/main/resources/templates/layout/menu.html`

```html
<!DOCTYPE html>
<html lang="zh-CN" xmlns:th="http://www.thymeleaf.org">
<body>
<aside th:fragment="menu(activeMenu)">
    <ul class="app-menu">
        <li>
            <a th:href="@{/page/dashboard}"
               th:classappend="${activeMenu == 'dashboard'} ? ' active' : ''">
                控制台
            </a>
        </li>
        <li>
            <a th:href="@{/user/list}"
               th:classappend="${activeMenu == 'user'} ? ' active' : ''">
                用户管理
            </a>
        </li>
        <li>
            <a th:href="@{/controller/search}"
               th:classappend="${activeMenu == 'search'} ? ' active' : ''">
                查询示例
            </a>
        </li>
        <li>
            <a th:href="@{/form/user/add}"
               th:classappend="${activeMenu == 'form'} ? ' active' : ''">
                表单示例
            </a>
        </li>
    </ul>
</aside>
</body>
</html>
```

菜单权限控制可以先通过后端传入菜单列表，再使用 `th:each` 动态渲染；如果只是简单示例或固定后台菜单，可以直接写在公共菜单片段中。涉及真实权限控制时，页面隐藏菜单只属于展示控制，后端接口仍然必须进行权限校验。

### 页面片段引入

页面片段引入用于把公共头部、菜单、底部组合到业务页面中。Thymeleaf 推荐使用片段表达式 `~{...}`，并通过 `th:insert` 或 `th:replace` 引入。`th:insert` 会把片段插入宿主标签内部，`th:replace` 会使用片段替换宿主标签本身。([Thymeleaf](https://www.thymeleaf.org/doc/tutorials/3.1/usingthymeleaf.html?utm_source=chatgpt.com))

下面给出一个完整业务页面，展示如何组合头部、菜单和底部。

文件位置：`src/main/resources/templates/page/dashboard.html`

```html
<!DOCTYPE html>
<html lang="zh-CN" xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>控制台</title>
    <link rel="stylesheet" th:href="@{/css/app.css}">
</head>
<body>

<!-- 使用公共头部替换当前 header 标签 -->
<header th:replace="~{layout/header :: header('Spring Boot 3 Thymeleaf 示例系统', 'dashboard')}"></header>

<div class="app-container">
    <!-- 使用公共菜单替换当前 aside 标签 -->
    <aside th:replace="~{layout/menu :: menu('dashboard')}"></aside>

    <main class="app-main">
        <h1>控制台</h1>
        <p>当前页面通过 Thymeleaf 片段复用了公共头部、公共菜单和公共底部。</p>

        <section class="card-list">
            <div class="card">
                <h2>用户数量</h2>
                <p>128</p>
            </div>
            <div class="card">
                <h2>今日访问</h2>
                <p>1024</p>
            </div>
        </section>
    </main>
</div>

<!-- 使用公共底部替换当前 footer 标签 -->
<footer th:replace="~{layout/footer :: footer}"></footer>

<script th:src="@{/js/app.js}"></script>
</body>
</html>
```

增加页面入口 Controller。

文件位置：`src/main/java/io/github/atengk/thymeleaf/controller/DashboardController.java`

```java
package io.github.atengk.thymeleaf.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 控制台页面控制器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Controller
public class DashboardController {

    /**
     * 进入控制台页面
     *
     * @return 控制台模板视图
     */
    @GetMapping("/page/dashboard")
    public String dashboard() {
        log.info("访问控制台页面");
        return "page/dashboard";
    }
}
```

访问地址：

```text
http://localhost:8080/page/dashboard
```

片段复用建议优先使用 `th:replace`，因为它会直接替换宿主标签，最终 HTML 结构更干净。只有需要保留宿主标签并把片段插入内部时，才使用 `th:insert`。

## 静态资源管理

静态资源管理用于统一维护 CSS、JavaScript、图片、字体、第三方前端库等文件。Spring Boot 默认会从类路径下的 `/static`、`/public`、`/resources`、`/META-INF/resources` 等目录提供静态资源，并默认映射到 `/**`；WebJars 资源默认通过 `/webjars/**` 访问。([Home](https://docs.spring.io/spring-boot/reference/web/servlet.html?utm_source=chatgpt.com))

推荐目录结构如下：

```text
src/main/resources/static
├── css
│   └── app.css
├── js
│   └── app.js
├── images
│   └── logo.svg
└── upload
    └── avatar-default.png
```

在 Thymeleaf 页面中，应优先使用链接表达式 `@{...}` 引入静态资源。这样可以自动适配应用上下文路径，避免项目部署到非根路径时资源地址失效。

### CSS 引入

CSS 引入用于控制页面布局、颜色、间距、表格、按钮和表单样式。默认情况下，放在 `src/main/resources/static/css/app.css` 的文件可以通过 `/css/app.css` 访问。

文件位置：`src/main/resources/static/css/app.css`

```css
body {
    margin: 0;
    font-family: Arial, "Microsoft YaHei", sans-serif;
    background: #f5f7fa;
    color: #333;
}

a {
    color: #1677ff;
    text-decoration: none;
}

a:hover {
    text-decoration: underline;
}

.app-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 12px 24px;
    background: #1f2937;
    color: #fff;
}

.app-title a {
    color: #fff;
    font-size: 18px;
    font-weight: 700;
}

.app-nav a {
    margin-left: 16px;
    color: #d1d5db;
}

.app-nav a.active,
.app-menu a.active {
    color: #1677ff;
    font-weight: 700;
}

.app-container {
    display: flex;
    min-height: calc(100vh - 96px);
}

.app-menu {
    width: 200px;
    margin: 0;
    padding: 16px;
    list-style: none;
    background: #fff;
    border-right: 1px solid #e5e7eb;
}

.app-menu li {
    margin-bottom: 10px;
}

.app-main {
    flex: 1;
    padding: 24px;
}

.app-footer {
    padding: 16px 24px;
    text-align: center;
    background: #fff;
    border-top: 1px solid #e5e7eb;
    color: #666;
}

.card-list {
    display: flex;
    gap: 16px;
}

.card {
    min-width: 180px;
    padding: 16px;
    background: #fff;
    border: 1px solid #e5e7eb;
    border-radius: 8px;
}
```

在模板中引入 CSS：

```html
<link rel="stylesheet" th:href="@{/css/app.css}">
```

浏览器实际访问地址：

```text
http://localhost:8080/css/app.css
```

如果配置了上下文路径，例如 `server.servlet.context-path=/demo`，使用 `th:href="@{/css/app.css}"` 会自动生成 `/demo/css/app.css`，不需要手动拼接上下文路径。

### JavaScript 引入

JavaScript 引入用于处理页面交互，例如按钮确认、表单提交前检查、列表操作、菜单折叠、异步请求等。默认情况下，放在 `src/main/resources/static/js/app.js` 的文件可以通过 `/js/app.js` 访问。

文件位置：`src/main/resources/static/js/app.js`

```javascript
/**
 * 全局页面脚本
 */
document.addEventListener('DOMContentLoaded', function () {
    console.log('Thymeleaf 页面脚本已加载');

    const deleteButtons = document.querySelectorAll('[data-confirm]');
    deleteButtons.forEach(function (button) {
        button.addEventListener('click', function (event) {
            const message = button.getAttribute('data-confirm') || '确认执行该操作吗？';
            if (!window.confirm(message)) {
                event.preventDefault();
            }
        });
    });
});
```

在模板中引入 JavaScript：

```html
<script th:src="@{/js/app.js}"></script>
```

业务页面使用确认提示：

```html
<a th:href="@{/user/{id}/delete(id=${user.id})}"
   data-confirm="确认删除该用户吗？">
    删除
</a>
```

浏览器实际访问地址：

```text
http://localhost:8080/js/app.js
```

脚本建议放在 `body` 结束标签前，减少阻塞页面渲染。通用脚本放在 `app.js`，页面专属脚本可以按模块拆分，例如 `user-list.js`、`user-form.js`。

### 图片资源访问

图片资源访问用于展示 Logo、头像、图标、业务图片和默认占位图。默认情况下，放在 `src/main/resources/static/images/logo.svg` 的文件可以通过 `/images/logo.svg` 访问。

推荐图片目录如下：

```text
src/main/resources/static/images
├── logo.svg
├── avatar-default.png
└── empty.svg
```

页面中引入图片：

```html
<img th:src="@{/images/logo.svg}" alt="系统 Logo" width="120">

<img th:src="@{/images/avatar-default.png}" alt="默认头像" width="48" height="48">
```

如果图片路径来自后端 Model，也可以动态渲染：

```html
<img th:src="@{${avatarUrl}}" alt="用户头像" width="48" height="48">
```

对应 Controller 示例：

文件位置：`src/main/java/io/github/atengk/thymeleaf/controller/ImageExampleController.java`

```java
package io.github.atengk.thymeleaf.controller;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 图片资源示例控制器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Controller
public class ImageExampleController {

    /**
     * 图片资源示例页面
     *
     * @param model 页面数据模型
     * @return 模板视图名称
     */
    @GetMapping("/resource/image")
    public String image(Model model) {
        String avatarUrl = StrUtil.blankToDefault(null, "/images/avatar-default.png");
        model.addAttribute("avatarUrl", avatarUrl);
        log.info("访问图片资源示例页面，头像地址：{}", avatarUrl);
        return "resource/image";
    }
}
```

图片示例页面：

文件位置：`src/main/resources/templates/resource/image.html`

```html
<!DOCTYPE html>
<html lang="zh-CN" xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>图片资源访问</title>
    <link rel="stylesheet" th:href="@{/css/app.css}">
</head>
<body>
<h1>图片资源访问</h1>

<p>固定图片：</p>
<img th:src="@{/images/logo.svg}" alt="系统 Logo" width="120">

<p>动态图片：</p>
<img th:src="@{${avatarUrl}}" alt="用户头像" width="48" height="48">

<script th:src="@{/js/app.js}"></script>
</body>
</html>
```

访问地址：

```text
http://localhost:8080/resource/image
```

图片如果来自用户上传目录，通常不建议直接放入 `resources/static`。上传文件更适合放在独立磁盘目录、对象存储或文件服务中，再通过配置静态资源映射或接口访问。

### WebJars 使用

WebJars 用于将前端库以 Jar 包形式引入 JVM Web 项目，例如 Bootstrap、jQuery、Font Awesome 等。WebJars 官方说明中将其定义为打包在 Jar 中的客户端 Web 库，并部署在 Maven Central，适合传统 Spring MVC 或 Thymeleaf 项目统一管理前端依赖。([webjars.org](https://www.webjars.org/?utm_source=chatgpt.com))

Spring Boot 对 WebJars 有默认静态资源处理，符合 WebJars 格式的资源默认可以通过 `/webjars/**` 访问；如果需要使用不带版本号的 WebJars URL，可以添加 `org.webjars:webjars-locator-lite` 依赖。([Home](https://docs.spring.io/spring-boot/reference/web/servlet.html?utm_source=chatgpt.com))

在 `pom.xml` 中增加 WebJars 依赖。Bootstrap Classic WebJar 当前 Maven Central 页面显示版本为 `5.3.8`，示例使用该版本。([Home](https://docs.spring.io/spring-boot/reference/web/servlet.html?utm_source=chatgpt.com))

文件位置：`pom.xml`

```xml
<dependencies>
    <!-- WebJars 版本定位器：支持 /webjars/bootstrap/css/bootstrap.min.css 这类不带版本号的访问路径 -->
    <dependency>
        <groupId>org.webjars</groupId>
        <artifactId>webjars-locator-lite</artifactId>
    </dependency>

    <!-- Bootstrap WebJar：用于在 Thymeleaf 页面中引入 Bootstrap 样式和脚本 -->
    <dependency>
        <groupId>org.webjars</groupId>
        <artifactId>bootstrap</artifactId>
        <version>5.3.8</version>
    </dependency>
</dependencies>
```

引入 Bootstrap WebJars 的页面如下。

文件位置：`src/main/resources/templates/resource/webjars.html`

```html
<!DOCTYPE html>
<html lang="zh-CN" xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>WebJars 使用</title>

    <!-- 使用 WebJars 引入 Bootstrap CSS -->
    <link rel="stylesheet" th:href="@{/webjars/bootstrap/css/bootstrap.min.css}">
</head>
<body>
<div class="container py-4">
    <h1 class="mb-3">WebJars 使用示例</h1>

    <div class="alert alert-primary" role="alert">
        当前页面通过 WebJars 引入 Bootstrap。
    </div>

    <button type="button" class="btn btn-primary">
        Bootstrap 按钮
    </button>
</div>

<!-- 使用 WebJars 引入 Bootstrap JS -->
<script th:src="@{/webjars/bootstrap/js/bootstrap.bundle.min.js}"></script>
</body>
</html>
```

增加页面入口 Controller。

文件位置：`src/main/java/io/github/atengk/thymeleaf/controller/WebJarsExampleController.java`

```java
package io.github.atengk.thymeleaf.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * WebJars 示例控制器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Controller
public class WebJarsExampleController {

    /**
     * WebJars 示例页面
     *
     * @return 模板视图名称
     */
    @GetMapping("/resource/webjars")
    public String webjars() {
        log.info("访问 WebJars 示例页面");
        return "resource/webjars";
    }
}
```

访问地址：

```text
http://localhost:8080/resource/webjars
```

也可以使用带版本号的 WebJars 访问路径：

```html
<link rel="stylesheet" th:href="@{/webjars/bootstrap/5.3.8/css/bootstrap.min.css}">
<script th:src="@{/webjars/bootstrap/5.3.8/js/bootstrap.bundle.min.js}"></script>
```

如果使用了 `webjars-locator-lite`，推荐使用不带版本号的路径：

```html
<link rel="stylesheet" th:href="@{/webjars/bootstrap/css/bootstrap.min.css}">
<script th:src="@{/webjars/bootstrap/js/bootstrap.bundle.min.js}"></script>
```

WebJars 适合传统服务端渲染项目统一管理少量前端库。如果项目已经使用 Vite、Webpack、pnpm、npm 或完整前端工程体系，通常不需要再使用 WebJars，应由前端构建工具管理依赖并输出静态资源。



## 国际化支持

国际化支持用于根据用户语言环境显示不同语言的页面文本，例如中文、英文、日文等。Spring Boot 默认会在 classpath 根目录查找 `messages` 资源包，并在存在默认资源文件 `messages.properties` 时自动配置 `MessageSource`；如果只提供 `messages_zh_CN.properties`、`messages_en_US.properties` 这类语言文件而没有默认文件，则不会触发默认消息源自动配置。([Home](https://docs.spring.io/spring-boot/reference/features/internationalization.html?utm_source=chatgpt.com))

### 资源文件配置

资源文件配置用于定义不同语言环境下的文本内容。Spring Boot 使用 `spring.messages` 命名空间配置国际化资源，例如 `basename`、`encoding`、`fallback-to-system-locale` 等；其中 `spring.messages.basename` 支持多个资源位置，可以是 classpath 根目录下的资源名，也可以是包路径形式。([Home](https://docs.spring.io/spring-boot/reference/features/internationalization.html?utm_source=chatgpt.com))

推荐目录结构如下：

```text
src/main/resources
├── application.yml
├── messages.properties
├── messages_zh_CN.properties
└── messages_en_US.properties
```

基础配置如下。

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  messages:
    # 国际化资源文件基础名称，默认就是 messages
    basename: messages

    # 国际化资源文件编码
    encoding: UTF-8

    # 找不到当前语言文本时，不回退到操作系统默认语言
    fallback-to-system-locale: false
```

默认资源文件用于兜底显示。

文件位置：`src/main/resources/messages.properties`

```properties
# 页面标题
app.title=Spring Boot 3 Thymeleaf Demo

# 菜单文本
menu.home=Home
menu.user=User Management
menu.form=Form Example

# 通用按钮
button.save=Save
button.cancel=Cancel
button.search=Search
button.reset=Reset

# 用户页面
user.title=User List
user.username=Username
user.nickname=Nickname
user.email=Email
user.status=Status
user.status.enabled=Enabled
user.status.disabled=Disabled
```

中文资源文件如下。

文件位置：`src/main/resources/messages_zh_CN.properties`

```properties
# 页面标题
app.title=Spring Boot 3 Thymeleaf 示例系统

# 菜单文本
menu.home=首页
menu.user=用户管理
menu.form=表单示例

# 通用按钮
button.save=保存
button.cancel=取消
button.search=查询
button.reset=重置

# 用户页面
user.title=用户列表
user.username=用户名
user.nickname=昵称
user.email=邮箱
user.status=状态
user.status.enabled=启用
user.status.disabled=禁用
```

英文资源文件如下。

文件位置：`src/main/resources/messages_en_US.properties`

```properties
# Page title
app.title=Spring Boot 3 Thymeleaf Demo

# Menu text
menu.home=Home
menu.user=User Management
menu.form=Form Example

# Common buttons
button.save=Save
button.cancel=Cancel
button.search=Search
button.reset=Reset

# User page
user.title=User List
user.username=Username
user.nickname=Nickname
user.email=Email
user.status=Status
user.status.enabled=Enabled
user.status.disabled=Disabled
```

国际化资源文件建议使用 UTF-8 编码，Key 使用模块化命名，例如 `user.title`、`button.save`、`menu.home`。同一个 Key 在不同语言文件中保持一致，页面只引用 Key，不直接写死中文或英文。

### 页面文本国际化

页面文本国际化用于在 Thymeleaf 模板中读取资源文件中的文本。Thymeleaf 使用 `#{...}` 读取国际化消息，例如 `#{app.title}`、`#{button.save}`。这类表达式会根据当前请求的 `Locale` 从对应资源文件中查找文本。

创建国际化示例页面。

文件位置：`src/main/resources/templates/i18n/index.html`

```html
<!DOCTYPE html>
<html lang="zh-CN" xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title th:text="#{app.title}">Spring Boot 3 Thymeleaf 示例系统</title>
    <link rel="stylesheet" th:href="@{/css/app.css}">
</head>
<body>
<header class="app-header">
    <div class="app-title" th:text="#{app.title}">Spring Boot 3 Thymeleaf 示例系统</div>
    <nav class="app-nav">
        <a th:href="@{/i18n}" th:text="#{menu.home}">首页</a>
        <a th:href="@{/controller/search}" th:text="#{menu.user}">用户管理</a>
        <a th:href="@{/form/user/add}" th:text="#{menu.form}">表单示例</a>
    </nav>
</header>

<main class="app-main">
    <h1 th:text="#{user.title}">用户列表</h1>

    <table border="1" cellpadding="8" cellspacing="0">
        <thead>
        <tr>
            <th th:text="#{user.username}">用户名</th>
            <th th:text="#{user.nickname}">昵称</th>
            <th th:text="#{user.email}">邮箱</th>
            <th th:text="#{user.status}">状态</th>
        </tr>
        </thead>
        <tbody>
        <tr th:each="user : ${userList}">
            <td th:text="${user.username}">Ateng</td>
            <td th:text="${user.nickname}">阿腾</td>
            <td th:text="${user.email}">ateng@example.com</td>
            <td th:text="${user.enabled} ? #{user.status.enabled} : #{user.status.disabled}">启用</td>
        </tr>
        </tbody>
    </table>

    <p>
        <button type="button" th:text="#{button.search}">查询</button>
        <button type="button" th:text="#{button.reset}">重置</button>
    </p>
</main>

<script th:src="@{/js/app.js}"></script>
</body>
</html>
```

对应 Controller 如下。

文件位置：`src/main/java/io/github/atengk/thymeleaf/controller/I18nController.java`

```java
package io.github.atengk.thymeleaf.controller;

import cn.hutool.core.collection.CollUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Map;

/**
 * 国际化页面控制器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Controller
public class I18nController {

    /**
     * 进入国际化示例页面
     *
     * @param model 页面数据模型
     * @return 国际化页面模板
     */
    @GetMapping("/i18n")
    public String index(Model model) {
        List<Map<String, Object>> userList = CollUtil.newArrayList(
                Map.of("username", "Ateng", "nickname", "阿腾", "email", "ateng@example.com", "enabled", true),
                Map.of("username", "Tom", "nickname", "汤姆", "email", "tom@example.com", "enabled", false)
        );

        model.addAttribute("userList", userList);
        log.info("访问国际化示例页面，用户数量：{}", userList.size());
        return "i18n/index";
    }
}
```

访问地址：

```text
http://localhost:8080/i18n
```

在模板中，普通变量使用 `${...}`，国际化文本使用 `#{...}`，链接使用 `@{...}`。三者语义不同，不建议混用。

### 语言切换

语言切换用于让用户在页面上主动选择语言，例如中文和英文。浏览器默认会通过 `Accept-Language` 请求头传递语言偏好，但如果需要通过页面按钮切换语言，通常需要配置 `LocaleResolver` 和 `LocaleChangeInterceptor`。

下面示例使用 Cookie 保存语言选择，并通过 `?lang=zh_CN`、`?lang=en_US` 切换当前语言。

文件位置：`src/main/java/io/github/atengk/thymeleaf/config/I18nConfig.java`

```java
package io.github.atengk.thymeleaf.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

import java.time.Duration;
import java.util.Locale;

/**
 * 国际化配置
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Configuration
public class I18nConfig implements WebMvcConfigurer {

    /**
     * 配置 Locale 解析器
     *
     * @return LocaleResolver
     */
    @Bean
    public LocaleResolver localeResolver() {
        CookieLocaleResolver resolver = new CookieLocaleResolver("ATENG_LOCALE");
        resolver.setDefaultLocale(Locale.SIMPLIFIED_CHINESE);
        resolver.setCookieMaxAge(Duration.ofDays(30));
        log.info("初始化国际化 Locale 解析器");
        return resolver;
    }

    /**
     * 配置语言切换拦截器
     *
     * @return LocaleChangeInterceptor
     */
    @Bean
    public LocaleChangeInterceptor localeChangeInterceptor() {
        LocaleChangeInterceptor interceptor = new LocaleChangeInterceptor();
        interceptor.setParamName("lang");
        log.info("初始化国际化语言切换拦截器，参数名：lang");
        return interceptor;
    }

    /**
     * 注册 MVC 拦截器
     *
     * @param registry 拦截器注册器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(localeChangeInterceptor());
    }
}
```

在页面中增加语言切换入口。

文件位置：`src/main/resources/templates/i18n/index.html`

```html
<div class="language-switch">
    <a th:href="@{/i18n(lang='zh_CN')}">中文</a>
    <span>|</span>
    <a th:href="@{/i18n(lang='en_US')}">English</a>
</div>
```

访问地址：

```text
http://localhost:8080/i18n?lang=zh_CN
http://localhost:8080/i18n?lang=en_US
```

如果访问 `?lang=zh_CN` 后页面变为中文，访问 `?lang=en_US` 后页面变为英文，说明国际化资源、语言解析器和语言切换拦截器配置正常。

## 安全与权限控制

安全与权限控制用于保护 Thymeleaf 页面、表单提交和后台操作入口。Spring Boot 3 对应 Spring Security 6，推荐使用 `SecurityFilterChain` Bean 配置安全规则，不再使用旧版 `WebSecurityConfigurerAdapter`。Spring Security 支持表单登录，未认证用户访问受保护资源时会被重定向到登录页；配置自定义登录页后，需要自己提供对应的 Controller 和模板。([Home](https://docs.spring.io/spring-security/reference/servlet/authentication/passwords/form.html?utm_source=chatgpt.com))

### Spring Security 集成

Spring Security 集成用于给页面项目增加登录认证、URL 授权、退出登录和 CSRF 防护。Thymeleaf 页面如果需要使用 `sec:authorize`、`sec:authentication` 等权限标签，还需要引入 `thymeleaf-extras-springsecurity6`。该 Thymeleaf Extras 模块提供 `sec` 方言，支持 `sec:authorize`、`sec:authentication`、`#authentication` 和 `#authorization` 等能力；在 Spring Boot 应用中，引入对应 Thymeleaf、Spring Security 和 extras 依赖后，该方言会自动配置。([GitHub](https://github.com/thymeleaf/thymeleaf-extras-springsecurity?utm_source=chatgpt.com))

先增加依赖配置。

文件位置：`pom.xml`

```xml
<dependencies>
    <!-- Spring Security 安全框架，提供认证、授权、登录、退出和 CSRF 防护 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>

    <!-- Thymeleaf 与 Spring Security 6 集成，支持 sec:authorize、sec:authentication 等权限标签 -->
    <dependency>
        <groupId>org.thymeleaf.extras</groupId>
        <artifactId>thymeleaf-extras-springsecurity6</artifactId>
    </dependency>
</dependencies>
```

推荐安全配置如下。该示例使用内存用户，适合文档演示和本地开发验证；生产项目应替换为数据库用户、LDAP、OAuth2、OIDC 或统一认证平台。

文件位置：`src/main/java/io/github/atengk/thymeleaf/config/SecurityConfig.java`

```java
package io.github.atengk.thymeleaf.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security 安全配置
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Configuration
public class SecurityConfig {

    /**
     * 配置安全过滤器链
     *
     * @param http HttpSecurity
     * @return SecurityFilterChain
     * @throws Exception 安全配置异常
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        log.info("初始化 Spring Security 安全过滤器链");

        http
                // 配置请求授权规则
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/login",
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/webjars/**",
                                "/favicon.ico"
                        ).permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/user/**", "/form/**", "/controller/**", "/page/**", "/i18n/**").authenticated()
                        .anyRequest().authenticated()
                )

                // 配置自定义登录页
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/page/dashboard", true)
                        .failureUrl("/login?error")
                        .permitAll()
                )

                // 配置退出登录；CSRF 开启时，默认应使用 POST 请求退出
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                )

                // 保持 CSRF 默认防护
                .csrf(Customizer.withDefaults());

        return http.build();
    }

    /**
     * 配置密码编码器
     *
     * @return PasswordEncoder
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 配置内存用户
     *
     * @param passwordEncoder 密码编码器
     * @return InMemoryUserDetailsManager
     */
    @Bean
    public InMemoryUserDetailsManager userDetailsService(PasswordEncoder passwordEncoder) {
        UserDetails admin = User.withUsername("admin")
                .password(passwordEncoder.encode("123456"))
                .roles("ADMIN", "USER")
                .build();

        UserDetails user = User.withUsername("user")
                .password(passwordEncoder.encode("123456"))
                .roles("USER")
                .build();

        log.info("初始化内存用户：admin、user");
        return new InMemoryUserDetailsManager(admin, user);
    }
}
```

当前配置下的访问规则如下：

| 路径                                                         | 权限要求          |
| ------------------------------------------------------------ | ----------------- |
| `/login`                                                     | 匿名可访问        |
| `/css/**`、`/js/**`、`/images/**`、`/webjars/**`             | 匿名可访问        |
| `/admin/**`                                                  | 需要 `ADMIN` 角色 |
| `/user/**`、`/form/**`、`/controller/**`、`/page/**`、`/i18n/**` | 登录后可访问      |
| 其他路径                                                     | 登录后可访问      |

启动后可以使用以下账号验证：

```text
管理员账号：admin / 123456
普通用户账号：user / 123456
```

### 登录页面开发

登录页面开发用于替换 Spring Security 默认登录页。Spring Security 文档说明，当配置了自定义 `loginPage("/login")` 后，应用需要自己渲染登录页；默认登录表单提交地址通常为 `POST /login`，用户名参数名为 `username`，密码参数名为 `password`，登录失败时会出现 `error` 参数，退出成功后会出现 `logout` 参数。([Home](https://docs.spring.io/spring-security/reference/servlet/authentication/passwords/form.html?utm_source=chatgpt.com))

创建登录页 Controller。

文件位置：`src/main/java/io/github/atengk/thymeleaf/controller/LoginController.java`

```java
package io.github.atengk.thymeleaf.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 登录页面控制器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Controller
public class LoginController {

    /**
     * 进入登录页面
     *
     * @return 登录模板视图
     */
    @GetMapping("/login")
    public String login() {
        log.info("访问登录页面");
        return "login";
    }
}
```

创建登录模板。

文件位置：`src/main/resources/templates/login.html`

```html
<!DOCTYPE html>
<html lang="zh-CN" xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>系统登录</title>
    <link rel="stylesheet" th:href="@{/css/app.css}">
    <style>
        .login-container {
            width: 360px;
            margin: 120px auto;
            padding: 24px;
            background: #fff;
            border: 1px solid #e5e7eb;
            border-radius: 8px;
        }

        .login-title {
            margin-bottom: 20px;
            text-align: center;
        }

        .login-item {
            margin-bottom: 14px;
        }

        .login-item input {
            width: 100%;
            box-sizing: border-box;
            padding: 8px;
        }

        .login-button {
            width: 100%;
            padding: 8px;
        }

        .login-error {
            margin-bottom: 14px;
            color: #c00;
        }

        .login-success {
            margin-bottom: 14px;
            color: #168000;
        }
    </style>
</head>
<body>
<div class="login-container">
    <h1 class="login-title">系统登录</h1>

    <div class="login-error" th:if="${param.error}">
        用户名或密码错误。
    </div>

    <div class="login-success" th:if="${param.logout}">
        已成功退出登录。
    </div>

    <form th:action="@{/login}" method="post">
        <div class="login-item">
            <input type="text" name="username" placeholder="请输入用户名" autocomplete="username">
        </div>

        <div class="login-item">
            <input type="password" name="password" placeholder="请输入密码" autocomplete="current-password">
        </div>

        <button class="login-button" type="submit">登录</button>
    </form>
</div>
</body>
</html>
```

访问受保护页面时，如果尚未登录，会被重定向到：

```text
http://localhost:8080/login
```

登录表单提交地址为：

```text
POST http://localhost:8080/login
```

Spring Security 会接管 `POST /login` 请求，验证用户名和密码。Controller 只需要处理 `GET /login` 页面展示，不需要自己编写登录认证逻辑。

### 权限按钮控制

权限按钮控制用于根据当前登录用户角色动态显示或隐藏页面元素，例如新增按钮、编辑按钮、删除按钮、系统配置入口等。Thymeleaf Extras Spring Security 提供 `sec:authorize` 属性，可根据 Spring Security 表达式判断当前用户是否有权限查看某个元素；该模块还提供 `sec:authentication` 用于输出当前认证对象属性。([GitHub](https://github.com/thymeleaf/thymeleaf-extras-springsecurity?utm_source=chatgpt.com))

在使用 `sec:*` 标签前，需要在 HTML 中声明命名空间：

```html
<html lang="zh-CN"
      xmlns:th="http://www.thymeleaf.org"
      xmlns:sec="http://www.thymeleaf.org/extras/spring-security">
```

权限按钮页面如下。

文件位置：`src/main/resources/templates/security/permission.html`

```html
<!DOCTYPE html>
<html lang="zh-CN"
      xmlns:th="http://www.thymeleaf.org"
      xmlns:sec="http://www.thymeleaf.org/extras/spring-security">
<head>
    <meta charset="UTF-8">
    <title>权限按钮控制</title>
    <link rel="stylesheet" th:href="@{/css/app.css}">
</head>
<body>
<header class="app-header">
    <div class="app-title">权限按钮控制</div>
    <div>
        当前用户：
        <span sec:authentication="name">anonymous</span>
    </div>
</header>

<main class="app-main">
    <h1>权限按钮控制示例</h1>

    <p>
        <button type="button" sec:authorize="hasRole('ADMIN')">新增用户</button>
        <button type="button" sec:authorize="hasAnyRole('ADMIN','USER')">查看用户</button>
        <button type="button" sec:authorize="hasRole('ADMIN')">删除用户</button>
    </p>

    <table border="1" cellpadding="8" cellspacing="0">
        <thead>
        <tr>
            <th>用户ID</th>
            <th>用户名</th>
            <th>操作</th>
        </tr>
        </thead>
        <tbody>
        <tr>
            <td>1001</td>
            <td>Ateng</td>
            <td>
                <a th:href="@{/user/1001/detail}" sec:authorize="hasAnyRole('ADMIN','USER')">详情</a>
                <a th:href="@{/user/1001/edit}" sec:authorize="hasRole('ADMIN')">编辑</a>
                <a th:href="@{/user/1001/delete}" sec:authorize="hasRole('ADMIN')" data-confirm="确认删除该用户吗？">删除</a>
            </td>
        </tr>
        </tbody>
    </table>

    <p sec:authorize="isAuthenticated()">
        当前内容仅登录用户可见。
    </p>

    <p sec:authorize="isAnonymous()">
        当前内容仅匿名用户可见。
    </p>
</main>

<script th:src="@{/js/app.js}"></script>
</body>
</html>
```

对应 Controller 如下。

文件位置：`src/main/java/io/github/atengk/thymeleaf/controller/SecurityPageController.java`

```java
package io.github.atengk.thymeleaf.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 安全页面控制器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Controller
public class SecurityPageController {

    /**
     * 进入权限按钮控制页面
     *
     * @return 权限按钮页面模板
     */
    @GetMapping("/security/permission")
    public String permission() {
        log.info("访问权限按钮控制页面");
        return "security/permission";
    }
}
```

如果希望该页面登录后可访问，需要在安全配置中加入路径规则：

文件位置：`src/main/java/io/github/atengk/thymeleaf/config/SecurityConfig.java`

```java
.requestMatchers("/security/**").authenticated()
```

权限控制常用表达式如下：

| 表达式                                    | 说明                             |
| ----------------------------------------- | -------------------------------- |
| `isAuthenticated()`                       | 当前用户已登录                   |
| `isAnonymous()`                           | 当前用户未登录                   |
| `hasRole('ADMIN')`                        | 当前用户具备 `ROLE_ADMIN`        |
| `hasAnyRole('ADMIN','USER')`              | 当前用户具备任意一个指定角色     |
| `hasAuthority('user:add')`                | 当前用户具备指定权限标识         |
| `hasAnyAuthority('user:add','user:edit')` | 当前用户具备任意一个指定权限标识 |

页面权限按钮控制只负责“显示或隐藏”。真正的权限校验必须在后端安全配置、Controller、Service 或方法安全注解中完成，不能只依赖前端隐藏按钮。

### CSRF 表单处理

CSRF 表单处理用于防止跨站请求伪造攻击。Spring Security 对非安全 HTTP 方法默认启用 CSRF 防护，例如 `POST` 请求；对于 Thymeleaf 表单，Spring Security 文档说明，使用 Thymeleaf 等与 `RequestDataValueProcessor` 集成的视图技术时，非安全方法表单会自动包含实际 CSRF Token。([Home](https://docs.spring.io/spring-security/reference/7.1/servlet/exploits/csrf.html?utm_source=chatgpt.com))

普通 Thymeleaf POST 表单可以这样写：

文件位置：`src/main/resources/templates/security/csrf-form.html`

```html
<!DOCTYPE html>
<html lang="zh-CN" xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>CSRF 表单处理</title>
    <link rel="stylesheet" th:href="@{/css/app.css}">
</head>
<body>
<main class="app-main">
    <h1>CSRF 表单处理</h1>

    <form th:action="@{/security/profile/update}" method="post">
        <div>
            <label>
                昵称：
                <input type="text" name="nickname" value="阿腾">
            </label>
        </div>

        <div>
            <label>
                邮箱：
                <input type="text" name="email" value="ateng@example.com">
            </label>
        </div>

        <button type="submit">保存</button>
    </form>

    <p>使用 Thymeleaf 渲染 POST 表单时，CSRF Token 会自动写入隐藏字段。</p>
</main>
</body>
</html>
```

渲染后的 HTML 中会包含类似隐藏字段：

```html
<input type="hidden" name="_csrf" value="实际生成的Token值">
```

对应 Controller 如下。

文件位置：`src/main/java/io/github/atengk/thymeleaf/controller/CsrfExampleController.java`

```java
package io.github.atengk.thymeleaf.controller;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * CSRF 表单示例控制器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Controller
public class CsrfExampleController {

    /**
     * 进入 CSRF 表单页面
     *
     * @return CSRF 表单模板
     */
    @GetMapping("/security/csrf-form")
    public String form() {
        log.info("访问 CSRF 表单页面");
        return "security/csrf-form";
    }

    /**
     * 更新用户资料
     *
     * @param nickname 昵称
     * @param email 邮箱
     * @return 重定向地址
     */
    @PostMapping("/security/profile/update")
    public String updateProfile(String nickname, String email) {
        String safeNickname = StrUtil.trimToEmpty(nickname);
        String safeEmail = StrUtil.trimToEmpty(email);

        log.info("更新用户资料，昵称：{}，邮箱：{}", safeNickname, safeEmail);
        return "redirect:/security/csrf-form";
    }
}
```

退出登录也应使用 POST 表单。Spring Security 文档说明，在 CSRF 启用时，`LogoutFilter` 默认只处理 HTTP POST 退出请求，这样可以避免恶意页面通过 GET 链接强制用户退出。([Home](https://docs.spring.io/spring-security/reference/7.1/servlet/exploits/csrf.html?utm_source=chatgpt.com))

推荐退出登录写法如下：

```html
<form th:action="@{/logout}" method="post" style="display: inline;">
    <button type="submit">退出登录</button>
</form>
```

如果需要在 AJAX 请求中携带 CSRF Token，可以在页面中输出 Meta 信息：

```html
<meta name="_csrf" th:content="${_csrf.token}">
<meta name="_csrf_header" th:content="${_csrf.headerName}">
```

JavaScript 请求时读取并设置请求头：

```javascript
const token = document.querySelector('meta[name="_csrf"]').getAttribute('content');
const header = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');

fetch('/security/profile/update', {
    method: 'POST',
    headers: {
        [header]: token,
        'Content-Type': 'application/x-www-form-urlencoded'
    },
    body: new URLSearchParams({
        nickname: '阿腾',
        email: 'ateng@example.com'
    })
});
```

CSRF 处理建议如下：

| 场景                 | 建议                                                         |
| -------------------- | ------------------------------------------------------------ |
| 普通 Thymeleaf 表单  | 使用 `th:action` + `method="post"`，让 Thymeleaf 自动处理 Token |
| 退出登录             | 使用 POST 表单提交 `/logout`                                 |
| AJAX POST/PUT/DELETE | 从 `_csrf` 中读取 Token，并放入请求头                        |
| REST API 无状态认证  | 根据认证方案单独评估是否禁用 CSRF                            |
| 后台管理页面         | 不建议关闭 CSRF，除非明确知道风险和替代防护方案              |

完成本节后，项目已经具备基础国际化能力和页面安全控制能力：可以通过资源文件管理多语言文本，通过链接切换语言；也可以使用 Spring Security 完成登录认证、URL 授权、页面权限按钮控制和 CSRF 表单防护。



## 数据展示

数据展示用于完成后台系统中常见的列表页、详情页、分页页和条件查询页。Thymeleaf 负责页面渲染，Controller 负责接收请求参数、查询数据并写入 `Model`，模板页面通过 `${...}`、`th:each`、`th:if` 和 `@{...}` 展示数据与生成链接。

本节示例使用内存集合模拟数据库数据，实际项目中可以替换为 MyBatis-Plus、JPA 或远程接口调用。

### 列表页面

列表页面用于展示多条业务数据，常见组成包括查询区域、数据表格、操作按钮和空数据提示。Thymeleaf 中通常使用 `th:each` 遍历集合，使用 `th:text` 输出字段内容。

下面的 Controller 提供用户列表、详情、分页和条件查询能力。

文件位置：`src/main/java/io/github/atengk/thymeleaf/controller/UserDataController.java`

```java
package io.github.atengk.thymeleaf.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

/**
 * 用户数据展示控制器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Controller
public class UserDataController {

    /**
     * 用户列表页面
     *
     * @param keyword 查询关键字
     * @param enabled 启用状态
     * @param page 当前页码
     * @param size 每页条数
     * @param model 页面数据模型
     * @return 用户列表模板
     */
    @GetMapping("/data/user/list")
    public String list(@RequestParam(required = false) String keyword,
                       @RequestParam(required = false) Boolean enabled,
                       @RequestParam(defaultValue = "1") Integer page,
                       @RequestParam(defaultValue = "5") Integer size,
                       Model model) {
        String trimKeyword = StrUtil.trimToEmpty(keyword);
        int currentPage = Math.max(ObjectUtil.defaultIfNull(page, 1), 1);
        int pageSize = Math.max(ObjectUtil.defaultIfNull(size, 5), 1);

        List<Map<String, Object>> filteredList = buildUserList().stream()
                .filter(user -> StrUtil.isBlank(trimKeyword)
                        || StrUtil.containsIgnoreCase(String.valueOf(user.get("username")), trimKeyword)
                        || StrUtil.containsIgnoreCase(String.valueOf(user.get("nickname")), trimKeyword)
                        || StrUtil.containsIgnoreCase(String.valueOf(user.get("email")), trimKeyword))
                .filter(user -> enabled == null || enabled.equals(user.get("enabled")))
                .toList();

        int total = filteredList.size();
        int totalPage = total == 0 ? 0 : (int) Math.ceil((double) total / pageSize);
        int fromIndex = Math.min((currentPage - 1) * pageSize, total);
        int toIndex = Math.min(fromIndex + pageSize, total);
        List<Map<String, Object>> pageList = filteredList.subList(fromIndex, toIndex);

        model.addAttribute("keyword", trimKeyword);
        model.addAttribute("enabled", enabled);
        model.addAttribute("page", currentPage);
        model.addAttribute("size", pageSize);
        model.addAttribute("total", total);
        model.addAttribute("totalPage", totalPage);
        model.addAttribute("hasPrevious", currentPage > 1);
        model.addAttribute("hasNext", totalPage > 0 && currentPage < totalPage);
        model.addAttribute("userList", pageList);

        log.info("查询用户列表，关键字：{}，状态：{}，页码：{}，每页条数：{}，总数：{}",
                trimKeyword, enabled, currentPage, pageSize, total);
        return "data/user-list";
    }

    /**
     * 用户详情页面
     *
     * @param id 用户ID
     * @param model 页面数据模型
     * @return 用户详情模板
     */
    @GetMapping("/data/user/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Map<String, Object> user = buildUserList().stream()
                .filter(item -> id.equals(item.get("id")))
                .findFirst()
                .orElse(null);

        if (user == null) {
            log.warn("用户不存在，用户ID：{}", id);
        } else {
            log.info("查询用户详情，用户ID：{}，用户名：{}", id, user.get("username"));
        }

        model.addAttribute("user", user);
        return "data/user-detail";
    }

    /**
     * 构建模拟用户数据
     *
     * @return 用户列表
     */
    private List<Map<String, Object>> buildUserList() {
        return CollUtil.newArrayList(
                Map.of("id", 1001L, "username", "Ateng", "nickname", "阿腾", "email", "ateng@example.com", "roleName", "管理员", "enabled", true),
                Map.of("id", 1002L, "username", "Tom", "nickname", "汤姆", "email", "tom@example.com", "roleName", "普通用户", "enabled", false),
                Map.of("id", 1003L, "username", "Jerry", "nickname", "杰瑞", "email", "jerry@example.com", "roleName", "审计员", "enabled", true),
                Map.of("id", 1004L, "username", "Alice", "nickname", "爱丽丝", "email", "alice@example.com", "roleName", "普通用户", "enabled", true),
                Map.of("id", 1005L, "username", "Bob", "nickname", "鲍勃", "email", "bob@example.com", "roleName", "普通用户", "enabled", false),
                Map.of("id", 1006L, "username", "Lucy", "nickname", "露西", "email", "lucy@example.com", "roleName", "管理员", "enabled", true)
        );
    }
}
```

用户列表模板如下。

文件位置：`src/main/resources/templates/data/user-list.html`

```html
<!DOCTYPE html>
<html lang="zh-CN" xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>用户列表</title>
    <link rel="stylesheet" th:href="@{/css/app.css}">
</head>
<body>
<main class="app-main">
    <h1>用户列表</h1>

    <form th:action="@{/data/user/list}" method="get">
        <label>
            关键字：
            <input type="text" name="keyword" th:value="${keyword}" placeholder="用户名、昵称或邮箱">
        </label>

        <label>
            状态：
            <select name="enabled">
                <option value="" th:selected="${enabled == null}">全部</option>
                <option value="true" th:selected="${enabled == true}">启用</option>
                <option value="false" th:selected="${enabled == false}">禁用</option>
            </select>
        </label>

        <input type="hidden" name="page" value="1">
        <input type="hidden" name="size" th:value="${size}">

        <button type="submit">查询</button>
        <a th:href="@{/data/user/list}">重置</a>
    </form>

    <table border="1" cellpadding="8" cellspacing="0">
        <thead>
        <tr>
            <th>序号</th>
            <th>用户ID</th>
            <th>用户名</th>
            <th>昵称</th>
            <th>邮箱</th>
            <th>角色</th>
            <th>状态</th>
            <th>操作</th>
        </tr>
        </thead>
        <tbody>
        <tr th:each="user, stat : ${userList}">
            <td th:text="${(page - 1) * size + stat.count}">1</td>
            <td th:text="${user.id}">1001</td>
            <td th:text="${user.username}">Ateng</td>
            <td th:text="${user.nickname}">阿腾</td>
            <td th:text="${user.email}">ateng@example.com</td>
            <td th:text="${user.roleName}">管理员</td>
            <td th:text="${user.enabled} ? '启用' : '禁用'">启用</td>
            <td>
                <a th:href="@{/data/user/{id}(id=${user.id})}">详情</a>
            </td>
        </tr>

        <tr th:if="${#lists.isEmpty(userList)}">
            <td colspan="8">暂无用户数据</td>
        </tr>
        </tbody>
    </table>

    <div class="pagination">
        <span th:text="'共 ' + ${total} + ' 条'">共 0 条</span>
        <span th:text="'第 ' + ${page} + ' / ' + ${totalPage} + ' 页'">第 1 / 1 页</span>

        <a th:if="${hasPrevious}"
           th:href="@{/data/user/list(keyword=${keyword},enabled=${enabled},page=${page - 1},size=${size})}">
            上一页
        </a>

        <a th:if="${hasNext}"
           th:href="@{/data/user/list(keyword=${keyword},enabled=${enabled},page=${page + 1},size=${size})}">
            下一页
        </a>
    </div>
</main>
</body>
</html>
```

访问地址：

```text
http://localhost:8080/data/user/list
```

带查询条件访问：

```text
http://localhost:8080/data/user/list?keyword=Ateng
http://localhost:8080/data/user/list?enabled=true
http://localhost:8080/data/user/list?keyword=a&enabled=true&page=1&size=5
```

列表页面需要注意空数据处理。即使后端返回空集合，页面也应显示“暂无数据”，避免用户误认为页面渲染失败。

### 详情页面

详情页面用于展示单条业务数据，通常从列表页点击“详情”进入。详情页通过路径参数定位数据，例如 `/data/user/1001`，Controller 使用 `@PathVariable` 接收用户 ID。

用户详情模板如下。

文件位置：`src/main/resources/templates/data/user-detail.html`

```html
<!DOCTYPE html>
<html lang="zh-CN" xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>用户详情</title>
    <link rel="stylesheet" th:href="@{/css/app.css}">
</head>
<body>
<main class="app-main">
    <h1>用户详情</h1>

    <div th:if="${user == null}">
        <p>用户不存在或已被删除。</p>
        <p>
            <a th:href="@{/data/user/list}">返回用户列表</a>
        </p>
    </div>

    <div th:if="${user != null}">
        <table border="1" cellpadding="8" cellspacing="0">
            <tr>
                <th>用户ID</th>
                <td th:text="${user.id}">1001</td>
            </tr>
            <tr>
                <th>用户名</th>
                <td th:text="${user.username}">Ateng</td>
            </tr>
            <tr>
                <th>昵称</th>
                <td th:text="${user.nickname}">阿腾</td>
            </tr>
            <tr>
                <th>邮箱</th>
                <td th:text="${user.email}">ateng@example.com</td>
            </tr>
            <tr>
                <th>角色</th>
                <td th:text="${user.roleName}">管理员</td>
            </tr>
            <tr>
                <th>状态</th>
                <td th:text="${user.enabled} ? '启用' : '禁用'">启用</td>
            </tr>
        </table>

        <p>
            <a th:href="@{/data/user/list}">返回用户列表</a>
        </p>
    </div>
</main>
</body>
</html>
```

访问地址：

```text
http://localhost:8080/data/user/1001
```

不存在的数据也应能正常展示提示信息：

```text
http://localhost:8080/data/user/9999
```

详情页面不要直接假设对象一定存在。建议在模板中使用 `th:if="${user != null}"` 和 `th:if="${user == null}"` 分别处理正常数据和空数据。

### 分页展示

分页展示用于控制列表每次只显示部分数据，避免一次性渲染过多记录。分页通常需要维护当前页码、每页条数、总记录数、总页数、上一页和下一页状态。

当前示例中，分页参数如下：

| 参数          | 示例    | 说明                |
| ------------- | ------- | ------------------- |
| `page`        | `1`     | 当前页码，从 1 开始 |
| `size`        | `5`     | 每页显示条数        |
| `total`       | `6`     | 符合条件的总记录数  |
| `totalPage`   | `2`     | 总页数              |
| `hasPrevious` | `false` | 是否存在上一页      |
| `hasNext`     | `true`  | 是否存在下一页      |

分页链接需要保留查询条件，否则点击上一页或下一页时会丢失筛选结果。

```html
<a th:if="${hasPrevious}"
   th:href="@{/data/user/list(keyword=${keyword},enabled=${enabled},page=${page - 1},size=${size})}">
    上一页
</a>

<a th:if="${hasNext}"
   th:href="@{/data/user/list(keyword=${keyword},enabled=${enabled},page=${page + 1},size=${size})}">
    下一页
</a>
```

分页访问示例：

```text
http://localhost:8080/data/user/list?page=1&size=5
http://localhost:8080/data/user/list?page=2&size=5
http://localhost:8080/data/user/list?keyword=a&page=1&size=5
```

分页查询时建议对 `page` 和 `size` 做边界保护，例如页码小于 1 时强制为 1，每页条数小于 1 时使用默认值。示例代码中通过 `Math.max` 和 Hutool 的 `ObjectUtil.defaultIfNull` 完成默认值处理。

### 条件查询

条件查询用于根据用户输入筛选数据，常见查询条件包括关键字、状态、类型、时间范围、所属部门等。查询类表单建议使用 `GET` 请求，这样查询条件会体现在 URL 中，便于刷新页面、复制链接和分页跳转。

查询表单示例：

```html
<form th:action="@{/data/user/list}" method="get">
    <label>
        关键字：
        <input type="text" name="keyword" th:value="${keyword}" placeholder="用户名、昵称或邮箱">
    </label>

    <label>
        状态：
        <select name="enabled">
            <option value="" th:selected="${enabled == null}">全部</option>
            <option value="true" th:selected="${enabled == true}">启用</option>
            <option value="false" th:selected="${enabled == false}">禁用</option>
        </select>
    </label>

    <input type="hidden" name="page" value="1">
    <input type="hidden" name="size" th:value="${size}">

    <button type="submit">查询</button>
    <a th:href="@{/data/user/list}">重置</a>
</form>
```

后端处理条件查询时，应对空字符串和空对象做保护：

```java
String trimKeyword = StrUtil.trimToEmpty(keyword);

List<Map<String, Object>> filteredList = buildUserList().stream()
        .filter(user -> StrUtil.isBlank(trimKeyword)
                || StrUtil.containsIgnoreCase(String.valueOf(user.get("username")), trimKeyword)
                || StrUtil.containsIgnoreCase(String.valueOf(user.get("nickname")), trimKeyword)
                || StrUtil.containsIgnoreCase(String.valueOf(user.get("email")), trimKeyword))
        .filter(user -> enabled == null || enabled.equals(user.get("enabled")))
        .toList();
```

条件查询页面需要注意两点：提交查询时应把 `page` 重置为 `1`，避免停留在旧页码导致无数据；分页链接应继续携带当前查询条件，避免翻页后条件丢失。

## 开发调试

开发调试用于提高 Thymeleaf 页面开发效率，主要包括模板热加载、异常排查和日志配置。开发阶段建议关闭 Thymeleaf 模板缓存，启用 DevTools，并适当提高项目包和 Web 请求相关日志级别。

### 模板热加载

模板热加载用于在修改 HTML、CSS、JavaScript 后快速看到效果。开发环境建议引入 `spring-boot-devtools`，同时在 `application-dev.yml` 中关闭 Thymeleaf 缓存。

在 Maven 中加入 DevTools 依赖。

文件位置：`pom.xml`

```xml
<dependencies>
    <!-- Spring Boot 开发工具：支持自动重启、开发期默认配置和 LiveReload -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-devtools</artifactId>
        <scope>runtime</scope>
        <optional>true</optional>
    </dependency>
</dependencies>
```

开发环境配置如下。

文件位置：`src/main/resources/application-dev.yml`

```yaml
spring:
  thymeleaf:
    # 开发环境关闭模板缓存，修改 HTML 后刷新浏览器即可查看效果
    cache: false

  web:
    resources:
      cache:
        # 开发环境关闭静态资源缓存
        period: 0
      chain:
        # 开发环境关闭资源链缓存
        cache: false

  devtools:
    restart:
      # 启用自动重启
      enabled: true
    livereload:
      # 启用 LiveReload
      enabled: true
```

开发环境启动命令：

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

常见修改效果如下：

| 修改内容           | 是否通常需要重启 | 说明                                  |
| ------------------ | ---------------- | ------------------------------------- |
| `templates/*.html` | 不需要           | 关闭 Thymeleaf 缓存后，刷新浏览器即可 |
| `static/css/*.css` | 不需要           | 浏览器可能缓存，需要强制刷新          |
| `static/js/*.js`   | 不需要           | 浏览器可能缓存，需要强制刷新          |
| Java 类            | 通常自动重启     | DevTools 会触发应用重启               |
| Maven 依赖         | 需要手动重启     | 依赖变化需要重新构建应用              |

如果模板修改后不生效，优先检查当前是否启用了 `dev` 环境，以及 `spring.thymeleaf.cache` 是否为 `false`。

### 常见异常排查

常见异常排查用于快速定位 Thymeleaf 页面开发中的典型问题。问题通常集中在 Controller 注解、模板路径、表达式字段、静态资源路径、表单绑定和 Spring Security 权限控制上。

| 异常或现象         | 常见原因                           | 处理方式                                        |
| ------------------ | ---------------------------------- | ----------------------------------------------- |
| 页面返回 404       | Controller 路径不匹配              | 检查 `@GetMapping` 地址和浏览器访问地址         |
| 模板找不到         | 返回视图名与模板文件路径不一致     | 检查 `templates` 目录、视图名和文件后缀         |
| 页面直接显示字符串 | 使用了 `@RestController`           | 改为 `@Controller`，或移除 `@ResponseBody`      |
| 表达式解析失败     | 模板字段名写错                     | 检查 `${user.xxx}` 是否与对象属性一致           |
| 空对象访问异常     | 后端未传入对象或数据不存在         | 使用 `th:if` 判断对象是否为空                   |
| 静态资源 404       | 文件未放在 `static` 目录或路径写错 | 检查 `static/css`、`static/js`、`static/images` |
| 表单错误不回显     | `BindingResult` 参数位置错误       | `BindingResult` 必须紧跟 `@Valid` 参数          |
| POST 表单 403      | CSRF Token 缺失                    | 使用 Thymeleaf 表单或手动携带 `_csrf`           |

模板路径错误示例：

```java
@GetMapping("/wrong")
public String wrong() {
    return "user/detail";
}
```

上面的返回值要求模板文件必须存在于：

```text
src/main/resources/templates/user/detail.html
```

如果文件实际路径是：

```text
src/main/resources/templates/data/user-detail.html
```

则 Controller 应返回：

```java
return "data/user-detail";
```

空对象访问建议写法如下：

```html
<div th:if="${user != null}">
    <span th:text="${user.username}">用户名</span>
</div>

<div th:if="${user == null}">
    用户不存在。
</div>
```

排查 Thymeleaf 问题时，建议先确认请求是否进入 Controller，再确认 Controller 返回的视图名是否正确，最后检查模板表达式和 Model 属性名称是否一致。

### 日志配置

日志配置用于观察页面请求、查询参数、数据结果、异常分支和权限拦截情况。开发环境可以适当提高日志级别，生产环境应控制日志量，避免输出过多框架细节。

开发环境日志配置如下。

文件位置：`src/main/resources/application-dev.yml`

```yaml
logging:
  level:
    # 项目业务代码输出 DEBUG 日志，便于观察 Controller 和 Service 执行流程
    io.github.atengk: debug

    # Spring Web 日志输出 DEBUG，便于观察请求映射和参数处理
    web: debug

    # Spring Security 日志按需提高级别
    org.springframework.security: info

  file:
    # 开发环境日志文件
    name: logs/thymeleaf-demo-dev.log
```

生产环境日志配置如下。

文件位置：`src/main/resources/application-prod.yml`

```yaml
logging:
  level:
    # 生产环境根日志保持 INFO
    root: info

    # 项目业务日志保持 INFO
    io.github.atengk: info

    # Spring 框架日志减少噪声
    org.springframework: warn

  file:
    # 生产环境日志文件
    name: logs/thymeleaf-demo-prod.log

  logback:
    rollingpolicy:
      # 单个日志文件最大大小
      max-file-size: 20MB

      # 日志保留天数
      max-history: 30

      # 历史日志总大小上限
      total-size-cap: 2GB
```

Controller 和 Service 中建议记录关键路径和核心参数：

```java
log.info("查询用户列表，关键字：{}，状态：{}，页码：{}，每页条数：{}，总数：{}",
        trimKeyword, enabled, currentPage, pageSize, total);

log.warn("用户不存在，用户ID：{}", id);
```

日志使用建议如下：

| 场景                   | 建议级别 | 说明                       |
| ---------------------- | -------- | -------------------------- |
| 页面访问、查询成功     | `INFO`   | 记录关键业务路径和核心参数 |
| 参数异常、数据不存在   | `WARN`   | 记录可恢复但需要关注的问题 |
| 系统异常、外部依赖失败 | `ERROR`  | 记录异常堆栈和关键上下文   |
| 开发期请求映射调试     | `DEBUG`  | 仅开发环境开启             |
| 生产环境框架细节       | `WARN`   | 避免框架日志过多           |

日志中不要输出密码、完整 Token、身份证号、银行卡号等敏感信息。查询条件、用户 ID、业务编号等定位问题所需字段可以适度输出。



## 项目实践

项目实践用于把前文中的页面跳转、Model 数据传递、表单绑定、参数校验、错误回显和列表渲染串成一个完整的用户管理功能。本节实现用户列表、新增、编辑和删除功能，数据仍使用内存集合模拟，便于直接运行和理解；实际项目中可以将 Service 中的集合操作替换为 MyBatis-Plus 或 JPA 数据库操作。

推荐目录结构如下：

```text
src/main/java/io/github/atengk/thymeleaf
├── controller
│   └── UserPracticeController.java
├── model
│   ├── dto
│   │   └── UserPracticeFormDTO.java
│   └── vo
│       └── UserPracticeVO.java
└── service
    ├── UserPracticeService.java
    └── impl
        └── UserPracticeServiceImpl.java

src/main/resources/templates/practice/user
├── list.html
├── add.html
└── edit.html
```

### 用户列表页面

用户列表页面用于展示当前用户数据，并提供新增、编辑和删除入口。列表页面通常是用户管理功能的入口，后续新增、编辑和删除完成后都可以重定向回列表页。

先定义用户展示对象。

文件位置：`src/main/java/io/github/atengk/thymeleaf/model/vo/UserPracticeVO.java`

```java
package io.github.atengk.thymeleaf.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户实践展示对象
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserPracticeVO {

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 是否启用
     */
    private Boolean enabled;
}
```

定义用户表单对象。

文件位置：`src/main/java/io/github/atengk/thymeleaf/model/dto/UserPracticeFormDTO.java`

```java
package io.github.atengk.thymeleaf.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户实践表单参数
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
public class UserPracticeFormDTO {

    /**
     * 用户ID，新增时为空，编辑时必填
     */
    private Long id;

    /**
     * 用户名
     */
    @NotBlank(message = "用户名不能为空")
    @Size(max = 20, message = "用户名长度不能超过20个字符")
    private String username;

    /**
     * 昵称
     */
    @NotBlank(message = "昵称不能为空")
    @Size(max = 30, message = "昵称长度不能超过30个字符")
    private String nickname;

    /**
     * 邮箱
     */
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    /**
     * 是否启用
     */
    @NotNull(message = "请选择启用状态")
    private Boolean enabled;
}
```

定义 Service 接口。

文件位置：`src/main/java/io/github/atengk/thymeleaf/service/UserPracticeService.java`

```java
package io.github.atengk.thymeleaf.service;

import io.github.atengk.thymeleaf.model.dto.UserPracticeFormDTO;
import io.github.atengk.thymeleaf.model.vo.UserPracticeVO;

import java.util.List;

/**
 * 用户实践服务
 *
 * @author Ateng
 * @since 2026-05-06
 */
public interface UserPracticeService {

    /**
     * 查询用户列表
     *
     * @return 用户列表
     */
    List<UserPracticeVO> list();

    /**
     * 查询用户详情
     *
     * @param id 用户ID
     * @return 用户详情
     */
    UserPracticeVO detail(Long id);

    /**
     * 新增用户
     *
     * @param form 表单参数
     * @return 新增后的用户ID
     */
    Long create(UserPracticeFormDTO form);

    /**
     * 更新用户
     *
     * @param form 表单参数
     * @return 是否更新成功
     */
    boolean update(UserPracticeFormDTO form);

    /**
     * 删除用户
     *
     * @param id 用户ID
     * @return 是否删除成功
     */
    boolean delete(Long id);
}
```

实现 Service。该实现使用内存 `Map` 模拟数据库表，适合文档演示；生产项目应替换为数据库持久化。

文件位置：`src/main/java/io/github/atengk/thymeleaf/service/impl/UserPracticeServiceImpl.java`

```java
package io.github.atengk.thymeleaf.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.thymeleaf.model.dto.UserPracticeFormDTO;
import io.github.atengk.thymeleaf.model.vo.UserPracticeVO;
import io.github.atengk.thymeleaf.service.UserPracticeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 用户实践服务实现
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Service
public class UserPracticeServiceImpl implements UserPracticeService {

    private static final AtomicLong ID_GENERATOR = new AtomicLong(1003L);

    private static final Map<Long, UserPracticeVO> USER_STORE = new ConcurrentHashMap<>();

    static {
        USER_STORE.put(1001L, new UserPracticeVO(1001L, "Ateng", "阿腾", "ateng@example.com", true));
        USER_STORE.put(1002L, new UserPracticeVO(1002L, "Tom", "汤姆", "tom@example.com", false));
        USER_STORE.put(1003L, new UserPracticeVO(1003L, "Jerry", "杰瑞", "jerry@example.com", true));
    }

    /**
     * 查询用户列表
     *
     * @return 用户列表
     */
    @Override
    public List<UserPracticeVO> list() {
        List<UserPracticeVO> userList = USER_STORE.values().stream()
                .sorted(Comparator.comparing(UserPracticeVO::getId))
                .toList();

        log.info("查询用户实践列表，数量：{}", userList.size());
        return userList;
    }

    /**
     * 查询用户详情
     *
     * @param id 用户ID
     * @return 用户详情
     */
    @Override
    public UserPracticeVO detail(Long id) {
        UserPracticeVO user = USER_STORE.get(id);
        if (user == null) {
            log.warn("用户实践详情不存在，用户ID：{}", id);
            return null;
        }

        log.info("查询用户实践详情，用户ID：{}，用户名：{}", id, user.getUsername());
        return user;
    }

    /**
     * 新增用户
     *
     * @param form 表单参数
     * @return 新增后的用户ID
     */
    @Override
    public Long create(UserPracticeFormDTO form) {
        Long id = ID_GENERATOR.incrementAndGet();
        UserPracticeVO user = new UserPracticeVO(
                id,
                form.getUsername(),
                form.getNickname(),
                form.getEmail(),
                ObjectUtil.defaultIfNull(form.getEnabled(), true)
        );

        USER_STORE.put(id, user);
        log.info("新增用户实践数据，用户ID：{}，用户名：{}", id, form.getUsername());
        return id;
    }

    /**
     * 更新用户
     *
     * @param form 表单参数
     * @return 是否更新成功
     */
    @Override
    public boolean update(UserPracticeFormDTO form) {
        if (form.getId() == null || !USER_STORE.containsKey(form.getId())) {
            log.warn("更新用户实践数据失败，用户不存在，用户ID：{}", form.getId());
            return false;
        }

        UserPracticeVO user = new UserPracticeVO(
                form.getId(),
                form.getUsername(),
                form.getNickname(),
                form.getEmail(),
                ObjectUtil.defaultIfNull(form.getEnabled(), true)
        );

        USER_STORE.put(form.getId(), user);
        log.info("更新用户实践数据，用户ID：{}，用户名：{}", form.getId(), form.getUsername());
        return true;
    }

    /**
     * 删除用户
     *
     * @param id 用户ID
     * @return 是否删除成功
     */
    @Override
    public boolean delete(Long id) {
        UserPracticeVO removed = USER_STORE.remove(id);
        if (removed == null) {
            log.warn("删除用户实践数据失败，用户不存在，用户ID：{}", id);
            return false;
        }

        log.info("删除用户实践数据，用户ID：{}，用户名：{}", id, removed.getUsername());
        return true;
    }
}
```

创建 Controller，统一处理列表、新增、编辑和删除页面请求。

文件位置：`src/main/java/io/github/atengk/thymeleaf/controller/UserPracticeController.java`

```java
package io.github.atengk.thymeleaf.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.BooleanUtil;
import io.github.atengk.thymeleaf.model.dto.UserPracticeFormDTO;
import io.github.atengk.thymeleaf.model.vo.UserPracticeVO;
import io.github.atengk.thymeleaf.service.UserPracticeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 用户实践页面控制器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class UserPracticeController {

    private final UserPracticeService userPracticeService;

    /**
     * 用户列表页面
     *
     * @param model 页面数据模型
     * @return 用户列表模板
     */
    @GetMapping("/practice/user/list")
    public String list(Model model) {
        model.addAttribute("userList", userPracticeService.list());
        log.info("访问用户实践列表页面");
        return "practice/user/list";
    }

    /**
     * 用户新增页面
     *
     * @param model 页面数据模型
     * @return 用户新增模板
     */
    @GetMapping("/practice/user/add")
    public String add(Model model) {
        if (!model.containsAttribute("userForm")) {
            UserPracticeFormDTO form = new UserPracticeFormDTO();
            form.setEnabled(true);
            model.addAttribute("userForm", form);
        }

        log.info("访问用户实践新增页面");
        return "practice/user/add";
    }

    /**
     * 保存新增用户
     *
     * @param userForm 表单参数
     * @param bindingResult 参数校验结果
     * @param redirectAttributes 重定向参数
     * @return 视图或重定向地址
     */
    @PostMapping("/practice/user/save")
    public String save(@Valid UserPracticeFormDTO userForm,
                       BindingResult bindingResult,
                       RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            log.warn("新增用户实践数据校验失败，错误数量：{}", bindingResult.getErrorCount());
            return "practice/user/add";
        }

        Long id = userPracticeService.create(userForm);
        redirectAttributes.addFlashAttribute("message", "用户新增成功，用户ID：" + id);
        return "redirect:/practice/user/list";
    }

    /**
     * 用户编辑页面
     *
     * @param id 用户ID
     * @param model 页面数据模型
     * @param redirectAttributes 重定向参数
     * @return 用户编辑模板或重定向地址
     */
    @GetMapping("/practice/user/{id}/edit")
    public String edit(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        UserPracticeVO user = userPracticeService.detail(id);
        if (user == null) {
            redirectAttributes.addFlashAttribute("message", "用户不存在或已被删除");
            return "redirect:/practice/user/list";
        }

        if (!model.containsAttribute("userForm")) {
            UserPracticeFormDTO form = BeanUtil.copyProperties(user, UserPracticeFormDTO.class);
            model.addAttribute("userForm", form);
        }

        log.info("访问用户实践编辑页面，用户ID：{}", id);
        return "practice/user/edit";
    }

    /**
     * 更新用户
     *
     * @param userForm 表单参数
     * @param bindingResult 参数校验结果
     * @param redirectAttributes 重定向参数
     * @return 视图或重定向地址
     */
    @PostMapping("/practice/user/update")
    public String update(@Valid UserPracticeFormDTO userForm,
                         BindingResult bindingResult,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            log.warn("更新用户实践数据校验失败，错误数量：{}", bindingResult.getErrorCount());
            return "practice/user/edit";
        }

        boolean updated = userPracticeService.update(userForm);
        if (BooleanUtil.isFalse(updated)) {
            redirectAttributes.addFlashAttribute("message", "用户不存在或已被删除");
            return "redirect:/practice/user/list";
        }

        redirectAttributes.addFlashAttribute("message", "用户更新成功");
        return "redirect:/practice/user/list";
    }

    /**
     * 删除用户
     *
     * @param id 用户ID
     * @param redirectAttributes 重定向参数
     * @return 重定向地址
     */
    @PostMapping("/practice/user/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        boolean deleted = userPracticeService.delete(id);
        redirectAttributes.addFlashAttribute("message", deleted ? "用户删除成功" : "用户不存在或已被删除");
        return "redirect:/practice/user/list";
    }
}
```

用户列表页面如下。

文件位置：`src/main/resources/templates/practice/user/list.html`

```html
<!DOCTYPE html>
<html lang="zh-CN" xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>用户管理</title>
    <link rel="stylesheet" th:href="@{/css/app.css}">
</head>
<body>
<main class="app-main">
    <h1>用户管理</h1>

    <p th:if="${message != null}" th:text="${message}">操作提示</p>

    <p>
        <a th:href="@{/practice/user/add}">新增用户</a>
    </p>

    <table border="1" cellpadding="8" cellspacing="0">
        <thead>
        <tr>
            <th>用户ID</th>
            <th>用户名</th>
            <th>昵称</th>
            <th>邮箱</th>
            <th>状态</th>
            <th>操作</th>
        </tr>
        </thead>
        <tbody>
        <tr th:each="user : ${userList}">
            <td th:text="${user.id}">1001</td>
            <td th:text="${user.username}">Ateng</td>
            <td th:text="${user.nickname}">阿腾</td>
            <td th:text="${user.email}">ateng@example.com</td>
            <td th:text="${user.enabled} ? '启用' : '禁用'">启用</td>
            <td>
                <a th:href="@{/practice/user/{id}/edit(id=${user.id})}">编辑</a>

                <form th:action="@{/practice/user/{id}/delete(id=${user.id})}" method="post" style="display: inline;">
                    <button type="submit" onclick="return confirm('确认删除该用户吗？')">删除</button>
                </form>
            </td>
        </tr>

        <tr th:if="${#lists.isEmpty(userList)}">
            <td colspan="6">暂无用户数据</td>
        </tr>
        </tbody>
    </table>
</main>
</body>
</html>
```

访问地址：

```text
http://localhost:8080/practice/user/list
```

列表页中的删除按钮使用 `POST` 表单提交，避免使用 `GET` 执行删除操作。如果项目已集成 Spring Security，Thymeleaf 表单会自动处理 CSRF 隐藏字段。

### 用户新增页面

用户新增页面用于录入用户信息，并通过 POST 请求提交到后端。后端使用 `@Valid` 校验参数，校验失败时返回新增页面并回显错误信息，校验通过后执行新增并重定向回列表页。

新增页面模板如下。

文件位置：`src/main/resources/templates/practice/user/add.html`

```html
<!DOCTYPE html>
<html lang="zh-CN" xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>新增用户</title>
    <link rel="stylesheet" th:href="@{/css/app.css}">
    <style>
        .form-item {
            margin-bottom: 12px;
        }

        .error {
            color: #c00;
            font-size: 13px;
        }
    </style>
</head>
<body>
<main class="app-main">
    <h1>新增用户</h1>

    <form th:action="@{/practice/user/save}" th:object="${userForm}" method="post">
        <div class="form-item">
            <label>
                用户名：
                <input type="text" th:field="*{username}" placeholder="请输入用户名">
            </label>
            <div class="error" th:if="${#fields.hasErrors('username')}" th:errors="*{username}"></div>
        </div>

        <div class="form-item">
            <label>
                昵称：
                <input type="text" th:field="*{nickname}" placeholder="请输入昵称">
            </label>
            <div class="error" th:if="${#fields.hasErrors('nickname')}" th:errors="*{nickname}"></div>
        </div>

        <div class="form-item">
            <label>
                邮箱：
                <input type="text" th:field="*{email}" placeholder="请输入邮箱">
            </label>
            <div class="error" th:if="${#fields.hasErrors('email')}" th:errors="*{email}"></div>
        </div>

        <div class="form-item">
            启用状态：
            <label>
                <input type="radio" th:field="*{enabled}" value="true"> 启用
            </label>
            <label>
                <input type="radio" th:field="*{enabled}" value="false"> 禁用
            </label>
            <div class="error" th:if="${#fields.hasErrors('enabled')}" th:errors="*{enabled}"></div>
        </div>

        <div class="form-item">
            <button type="submit">保存</button>
            <a th:href="@{/practice/user/list}">返回列表</a>
        </div>
    </form>
</main>
</body>
</html>
```

访问地址：

```text
http://localhost:8080/practice/user/add
```

新增页面需要保证 `Model` 中存在 `userForm` 对象，否则 `th:object="${userForm}"` 无法完成表单绑定。

### 用户编辑页面

用户编辑页面用于加载已有用户数据，并提交更新后的内容。编辑页与新增页结构类似，但需要隐藏字段保存用户 ID，并且提交地址应指向更新接口。

编辑页面模板如下。

文件位置：`src/main/resources/templates/practice/user/edit.html`

```html
<!DOCTYPE html>
<html lang="zh-CN" xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>编辑用户</title>
    <link rel="stylesheet" th:href="@{/css/app.css}">
    <style>
        .form-item {
            margin-bottom: 12px;
        }

        .error {
            color: #c00;
            font-size: 13px;
        }
    </style>
</head>
<body>
<main class="app-main">
    <h1>编辑用户</h1>

    <form th:action="@{/practice/user/update}" th:object="${userForm}" method="post">
        <input type="hidden" th:field="*{id}">

        <div class="form-item">
            <label>
                用户名：
                <input type="text" th:field="*{username}" placeholder="请输入用户名">
            </label>
            <div class="error" th:if="${#fields.hasErrors('username')}" th:errors="*{username}"></div>
        </div>

        <div class="form-item">
            <label>
                昵称：
                <input type="text" th:field="*{nickname}" placeholder="请输入昵称">
            </label>
            <div class="error" th:if="${#fields.hasErrors('nickname')}" th:errors="*{nickname}"></div>
        </div>

        <div class="form-item">
            <label>
                邮箱：
                <input type="text" th:field="*{email}" placeholder="请输入邮箱">
            </label>
            <div class="error" th:if="${#fields.hasErrors('email')}" th:errors="*{email}"></div>
        </div>

        <div class="form-item">
            启用状态：
            <label>
                <input type="radio" th:field="*{enabled}" value="true"> 启用
            </label>
            <label>
                <input type="radio" th:field="*{enabled}" value="false"> 禁用
            </label>
            <div class="error" th:if="${#fields.hasErrors('enabled')}" th:errors="*{enabled}"></div>
        </div>

        <div class="form-item">
            <button type="submit">保存</button>
            <a th:href="@{/practice/user/list}">返回列表</a>
        </div>
    </form>
</main>
</body>
</html>
```

访问地址：

```text
http://localhost:8080/practice/user/1001/edit
```

编辑页面加载流程如下：

```text
GET /practice/user/1001/edit
   ↓
根据 ID 查询用户
   ↓
复制用户数据到 userForm
   ↓
返回 practice/user/edit.html
   ↓
提交 POST /practice/user/update
```

如果编辑时用户不存在，应重定向回列表页并给出提示，不建议直接进入空白编辑页。

### 用户删除功能

用户删除功能用于从列表页发起删除操作。删除属于会改变服务端状态的操作，应使用 `POST` 请求，不建议使用 `GET` 链接直接删除。

列表页中的删除表单如下：

```html
<form th:action="@{/practice/user/{id}/delete(id=${user.id})}" method="post" style="display: inline;">
    <button type="submit" onclick="return confirm('确认删除该用户吗？')">删除</button>
</form>
```

后端删除方法如下：

```java
@PostMapping("/practice/user/{id}/delete")
public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
    boolean deleted = userPracticeService.delete(id);
    redirectAttributes.addFlashAttribute("message", deleted ? "用户删除成功" : "用户不存在或已被删除");
    return "redirect:/practice/user/list";
}
```

删除功能的验证方式：

```text
1. 访问 http://localhost:8080/practice/user/list
2. 点击任意用户行的“删除”按钮
3. 确认浏览器弹窗
4. 页面重定向回用户列表
5. 被删除用户不再显示，并展示“用户删除成功”
```

如果项目启用了 Spring Security 的 CSRF 防护，使用 Thymeleaf 渲染的 POST 表单会自动包含 CSRF Token；不要把删除操作改为普通 GET 链接，否则既不符合 HTTP 语义，也会增加误删风险。

## 打包与部署

打包与部署用于将开发完成的 Thymeleaf 项目构建为可运行产物，并在测试、预发或生产环境中启动。Spring Boot Maven Plugin 可以创建包含应用依赖的可执行 Jar 或 War，生成后的 Jar 可以通过 `java -jar` 直接运行。([Home](https://docs.spring.io/spring-boot/maven-plugin/packaging.html?utm_source=chatgpt.com))

### 打包配置

打包配置主要依赖 `spring-boot-maven-plugin`。如果项目使用 `spring-boot-starter-parent`，通常只需要在 `pom.xml` 中声明该插件即可，`repackage` 执行已经由父工程预配置。([Home](https://docs.spring.io/spring-boot/maven-plugin/packaging.html?utm_source=chatgpt.com))

文件位置：`pom.xml`

```xml
<build>
    <plugins>
        <!-- Spring Boot Maven 插件：用于打包可执行 Jar -->
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
            <configuration>
                <!-- 打包时排除 DevTools，避免开发工具进入生产运行包 -->
                <excludeDevtools>true</excludeDevtools>
            </configuration>
        </plugin>
    </plugins>
</build>
```

执行打包命令：

```bash
mvn clean package -DskipTests
```

命令执行完成后，产物通常位于：

```text
target/springboot3-thymeleaf-demo-1.0.0.jar
```

如果需要先执行测试再打包，可以去掉 `-DskipTests`：

```bash
mvn clean package
```

常用 Maven 命令说明：

| 命令                            | 说明                          |
| ------------------------------- | ----------------------------- |
| `mvn clean`                     | 清理 `target` 构建目录        |
| `mvn package`                   | 编译、测试并打包项目          |
| `mvn clean package -DskipTests` | 清理并打包，但跳过测试执行    |
| `java -jar target/*.jar`        | 运行打包后的 Spring Boot 应用 |

生产打包前建议确认三点：项目能正常编译，生产配置文件存在，`spring.thymeleaf.cache` 在生产环境为 `true`。

### 运行方式

运行方式包括本地开发运行、Jar 包运行、指定环境运行和后台运行。Spring Boot 支持通过命令行参数、系统属性和环境变量设置配置项；常见做法是在启动时指定 `spring.profiles.active`，从而加载不同环境的配置。Spring Boot 官方文档说明，可以通过系统属性、环境变量或命令行方式设置激活的 Profile。([Home](https://docs.spring.io/spring-boot/3.5/how-to/properties-and-configuration.html?utm_source=chatgpt.com))

开发环境运行：

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Jar 包运行：

```bash
java -jar target/springboot3-thymeleaf-demo-1.0.0.jar
```

指定生产环境运行：

```bash
java -jar target/springboot3-thymeleaf-demo-1.0.0.jar --spring.profiles.active=prod
```

指定端口运行：

```bash
java -jar target/springboot3-thymeleaf-demo-1.0.0.jar --server.port=8080 --spring.profiles.active=prod
```

Linux 后台运行示例：

```bash
mkdir -p logs

nohup java -jar target/springboot3-thymeleaf-demo-1.0.0.jar \
  --spring.profiles.active=prod \
  --server.port=8080 \
  > logs/start.log 2>&1 &
```

查看启动日志：

```bash
tail -f logs/start.log
```

查看进程：

```bash
ps -ef | grep springboot3-thymeleaf-demo | grep -v grep
```

停止进程时，可以先查看进程号，再执行 `kill`：

```bash
ps -ef | grep springboot3-thymeleaf-demo | grep -v grep

kill <PID>
```

其中 `<PID>` 是上一条命令查询到的 Java 进程号。生产环境建议使用 systemd、Docker、Kubernetes 或专业发布平台管理进程，不建议长期依赖手工 `nohup`。

### 生产环境缓存配置

生产环境缓存配置用于提高模板渲染和静态资源访问性能。开发环境通常关闭 Thymeleaf 缓存，方便修改模板后立即生效；生产环境应开启 Thymeleaf 缓存，避免每次请求都重新解析模板。Spring Boot 的 Thymeleaf 缓存配置项是 `spring.thymeleaf.cache`，该属性控制是否启用模板缓存。([Home](https://docs.spring.io/spring-boot/3.5/how-to/properties-and-configuration.html?utm_source=chatgpt.com))

生产环境配置如下。

文件位置：`src/main/resources/application-prod.yml`

```yaml
spring:
  thymeleaf:
    # 生产环境开启模板缓存，提高页面渲染性能
    cache: true

  web:
    resources:
      cache:
        # 静态资源缓存时间，生产环境可根据发布频率调整
        period: 7d
      chain:
        # 开启资源链缓存
        cache: true

server:
  # 生产环境服务端口
  port: 8080

logging:
  level:
    # 生产环境业务日志建议保持 INFO
    io.github.atengk: info

    # Spring 框架日志降低噪声
    org.springframework: warn

  file:
    # 生产环境日志文件
    name: logs/thymeleaf-demo-prod.log
```

开发环境配置应与生产环境区分。

文件位置：`src/main/resources/application-dev.yml`

```yaml
spring:
  thymeleaf:
    # 开发环境关闭模板缓存，方便调试页面
    cache: false

  web:
    resources:
      cache:
        # 开发环境关闭静态资源缓存
        period: 0
      chain:
        # 开发环境关闭资源链缓存
        cache: false

logging:
  level:
    # 开发环境提高项目日志级别
    io.github.atengk: debug
    web: debug
```

运行生产环境：

```bash
java -jar target/springboot3-thymeleaf-demo-1.0.0.jar --spring.profiles.active=prod
```

验证生产环境缓存配置是否生效：

```text
1. 启动时指定 --spring.profiles.active=prod
2. 访问 http://localhost:8080/practice/user/list
3. 确认页面可以正常渲染
4. 修改 templates 下的 HTML 后不重新打包、不重启应用
5. 页面通常不会立即体现修改，说明模板缓存已开启
```

生产环境注意事项：

| 项目               | 建议                                               |
| ------------------ | -------------------------------------------------- |
| Thymeleaf 模板缓存 | 开启，配置 `spring.thymeleaf.cache=true`           |
| 静态资源缓存       | 开启，并结合版本号、文件指纹或发布流程处理缓存刷新 |
| DevTools           | 不进入生产包，或在生产依赖中排除                   |
| 日志级别           | 业务包 `INFO`，框架包 `WARN`                       |
| 配置文件           | 使用 `application-prod.yml` 或外部配置             |
| 启动参数           | 明确指定 `--spring.profiles.active=prod`           |
| 进程管理           | 优先使用 systemd、Docker、Kubernetes 或发布平台    |

完成本节后，一个基于 Spring Boot 3 和 Thymeleaf 的用户管理示例已经形成完整闭环：可以展示列表、新增用户、编辑用户、删除用户，也可以通过 Maven 打包为可执行 Jar，并在开发环境和生产环境中使用不同缓存策略运行。