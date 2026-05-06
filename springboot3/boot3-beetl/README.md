# Beetl

Beetl 是一款 Java 模板引擎，适合在 Spring Boot Web 项目中作为服务端页面渲染方案使用。本文以 Spring Boot 3 为基础，说明 Beetl 的功能定位、典型集成场景、项目环境要求、目录规划和 Maven 依赖配置。Spring Boot 3 基于 Java 17、Spring Framework 6 和 Jakarta EE 体系，因此在选择 Beetl 依赖时需要重点关注依赖版本与 `javax.*` / `jakarta.*` 兼容问题。([Home](https://docs.spring.io/spring-boot/docs/3.0.0/reference/html/getting-started.html?utm_source=chatgpt.com))

## 技术概述

本节用于说明 Beetl 在 Spring Boot 3 项目中的角色。Beetl 不负责请求路由、权限控制、数据库访问或业务编排，它主要承担“将后端 Model 数据渲染为 HTML 页面”的职责。

### Beetl 功能定位

Beetl 的核心定位是 Java 服务端模板引擎。它通常位于 Spring MVC 的 Controller 与前端 HTML 页面之间：Controller 接收请求、准备数据并返回视图名称，Beetl 根据模板文件和 Model 数据生成最终 HTML 响应。

在传统后台管理系统、内部运营系统、简单门户页面、表单页面和服务端渲染页面中，Beetl 可以减少前后端分离带来的接口、路由、构建和部署复杂度。对于页面交互不复杂、SEO 或首屏渲染要求较高、团队以 Java 后端为主的项目，Beetl 是一种轻量且直接的选择。

Beetl 常见能力包括变量输出、条件判断、循环渲染、模板引入、公共布局、函数扩展和全局变量配置。实际开发中，它通常用于渲染列表页、详情页、新增编辑页、异常页以及后台管理页面。

在 Spring Boot 3 项目中，建议将 Beetl 作为 MVC 视图层组件使用，而不是替代 REST API、Vue、React 或其他前端框架。它更适合“后端直接输出页面”的场景，不适合复杂 SPA、强交互前端或大型前端工程化场景。

### Spring Boot 3 集成场景

Spring Boot 3 集成 Beetl 的典型方式是：使用 `spring-boot-starter-web` 提供 Spring MVC 能力，再接入 Beetl 模板引擎，通过 ViewResolver 将 Controller 返回的视图名称解析为 `.html` 模板文件。Spring Boot 的 MVC 文档说明，ViewResolver 用于将 Controller 返回的视图名称解析成具体 View，DispatcherServlet 会按顺序查找可用的视图解析器。([Home](https://docs.spring.io/spring-boot/3.5-SNAPSHOT/how-to/spring-mvc.html?utm_source=chatgpt.com))

适合使用 Beetl 的 Spring Boot 3 场景包括：

| 场景           | 说明                                                     |
| -------------- | -------------------------------------------------------- |
| 后台管理系统   | 页面结构固定，表格、表单、详情页较多，后端直接渲染效率高 |
| 内部运营平台   | 权限、菜单、字典、用户信息等公共变量可由后端统一注入     |
| 简单门户页面   | 页面交互不重，服务端渲染部署简单                         |
| 表单型业务页面 | 新增、编辑、提交、校验失败回显等流程清晰                 |
| 异常页与提示页 | 404、500、权限不足、操作成功提示等页面可统一渲染         |

不建议优先使用 Beetl 的场景包括复杂前端交互、移动端 H5 工程化项目、大型 SPA 项目、多端复用 API 项目。这类项目通常更适合 Vue、React 或其他前端框架配合 REST API。

需要注意的是，`com.ibeetl:beetl-framework-starter` 在 Maven Central 上显示的最新版本为 `1.2.40.Beetl.RELEASE`，发布时间早于 Spring Boot 3 时代；而 Beetl 核心包 `com.ibeetl:beetl` 当前可见版本为 `3.21.0.RELEASE`。因此，Spring Boot 3 项目更建议优先使用 Beetl 核心包并手动配置视图解析器，避免直接依赖较旧 Starter 带来的兼容性不确定性。([Maven Central](https://central.sonatype.com/artifact/com.ibeetl/beetl-framework-starter?utm_source=chatgpt.com))

## 环境准备

本节用于确定项目运行版本、目录结构和 Maven 依赖。Spring Boot 3 项目对 JDK、Servlet 容器和依赖包命名空间都有明确要求，配置前应先统一基础环境。

### JDK 与 Spring Boot 版本

Spring Boot 3.0.x 官方要求 Java 17 起步，并依赖 Spring Framework 6.x；Spring Boot 3.5 系列同样要求至少 Java 17，Maven 构建工具建议使用 3.6.3 或更高版本。([Home](https://docs.spring.io/spring-boot/docs/3.0.0/reference/html/getting-started.html?utm_source=chatgpt.com))

推荐开发环境如下：

| 项目         | 推荐版本       | 说明                                                      |
| ------------ | -------------- | --------------------------------------------------------- |
| JDK          | 17 或 21       | Spring Boot 3 最低要求 Java 17，生产环境建议使用 LTS 版本 |
| Spring Boot  | 3.3.x 或 3.5.x | 根据企业依赖基线选择，避免混用 Boot 2 时代组件            |
| Maven        | 3.8.x 或更高   | 满足 Spring Boot 3 构建要求                               |
| Servlet 容器 | Tomcat 10.1+   | Spring Boot 3 使用 Jakarta Servlet 体系                   |
| 编码         | UTF-8          | 模板、配置文件、Java 文件统一使用 UTF-8                   |

Spring Boot 3 使用 Jakarta EE 命名空间，旧项目中依赖 `javax.servlet.*` 的组件需要特别检查。对于模板引擎、过滤器、拦截器、Servlet、文件上传、验证码、权限框架等 Web 相关依赖，应优先选择兼容 Spring Boot 3 / Jakarta 的版本。Spring Boot 官方迁移指南也强调，迁移到 Spring Boot 3 前需要审查第三方依赖兼容性。([GitHub](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-3.0-Migration-Guide?utm_source=chatgpt.com))

可以使用以下命令检查本地环境：

```bash
# 查看 JDK 版本，建议为 17 或 21
java -version

# 查看 Maven 版本，建议为 3.8.x 或更高
mvn -version
```

### 项目结构规划

Beetl 模板文件建议统一放在 `src/main/resources/templates` 目录下，静态资源放在 `src/main/resources/static` 目录下。Java 代码按 Spring Boot 常规分层组织，Controller 专门负责页面跳转和 Model 数据装配。

推荐项目结构如下：

```text
springboot3-beetl-demo
├── pom.xml
└── src
    └── main
        ├── java
        │   └── io
        │       └── github
        │           └── atengk
        │               ├── BeetlApplication.java
        │               ├── config
        │               │   └── BeetlConfig.java
        │               ├── controller
        │               │   └── IndexController.java
        │               ├── service
        │               │   └── UserService.java
        │               └── vo
        │                   └── UserVO.java
        └── resources
            ├── application.yml
            ├── beetl.properties
            ├── static
            │   ├── css
            │   │   └── app.css
            │   ├── js
            │   │   └── app.js
            │   └── images
            │       └── logo.png
            └── templates
                ├── index.html
                ├── layout
                │   └── main.html
                ├── user
                │   ├── list.html
                │   ├── detail.html
                │   └── form.html
                └── error
                    ├── 404.html
                    └── 500.html
```

目录职责建议如下：

| 路径                                        | 作用                                     |
| ------------------------------------------- | ---------------------------------------- |
| `src/main/java/io/github/atengk/config`     | 放置 Beetl、MVC、拦截器、全局变量等配置  |
| `src/main/java/io/github/atengk/controller` | 页面跳转 Controller，返回 Beetl 模板路径 |
| `src/main/java/io/github/atengk/service`    | 业务逻辑处理                             |
| `src/main/java/io/github/atengk/vo`         | 页面展示对象，避免直接暴露 Entity        |
| `src/main/resources/templates`              | Beetl 页面模板                           |
| `src/main/resources/templates/layout`       | 公共布局、头部、底部、菜单               |
| `src/main/resources/templates/error`        | 全局异常页面                             |
| `src/main/resources/static`                 | CSS、JavaScript、图片等静态资源          |
| `src/main/resources/beetl.properties`       | Beetl 模板引擎配置                       |
| `src/main/resources/application.yml`        | Spring Boot 应用配置                     |

模板命名建议保持路径清晰。例如用户列表页使用 `templates/user/list.html`，Controller 返回视图时使用 `user/list.html` 或与自定义 ViewResolver 约定一致的路径。不要在不同模块中重复使用含义模糊的模板名，例如 `page.html`、`main.html`、`test.html`。

### Maven 依赖配置

Spring Boot 3 项目建议先引入 `spring-boot-starter-web`，再引入 Beetl 核心依赖。由于旧版 Beetl Starter 最后可见版本较老，本文依赖示例采用 `com.ibeetl:beetl` 作为基础，后续章节通过手动配置 `BeetlGroupUtilConfiguration` 和 `BeetlSpringViewResolver` 完成集成。Maven Central 当前显示 `com.ibeetl:beetl` 的可见版本为 `3.21.0.RELEASE`。([Maven Central](https://central.sonatype.com/artifact/com.ibeetl/beetl?utm_source=chatgpt.com))

文件位置：`pom.xml`

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <!-- 使用 Spring Boot 3 作为项目父工程，统一管理 Spring 生态依赖版本 -->
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.5.5</version>
        <relativePath/>
    </parent>

    <groupId>io.github.atengk</groupId>
    <artifactId>springboot3-beetl-demo</artifactId>
    <version>1.0.0</version>
    <name>springboot3-beetl-demo</name>
    <description>Spring Boot 3 集成 Beetl 模板引擎示例项目</description>

    <properties>
        <!-- Spring Boot 3 最低要求 Java 17 -->
        <java.version>17</java.version>

        <!-- Beetl 核心依赖版本，按项目兼容性测试后统一升级 -->
        <beetl.version>3.21.0.RELEASE</beetl.version>

        <!-- Hutool 工具包版本，用于后续日期、集合、字符串等工具处理 -->
        <hutool.version>5.8.36</hutool.version>
    </properties>

    <dependencies>
        <!-- Spring MVC、内嵌 Tomcat、JSON 转换等 Web 基础能力 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Beetl 模板引擎核心依赖，Spring Boot 3 推荐配合手动配置视图解析器 -->
        <dependency>
            <groupId>com.ibeetl</groupId>
            <artifactId>beetl</artifactId>
            <version>${beetl.version}</version>
        </dependency>

        <!-- Hutool 工具类库，后续可用于自定义函数、日期格式化、字符串处理等场景 -->
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
            <version>${hutool.version}</version>
        </dependency>

        <!-- Lombok 简化 DTO、VO、配置类等样板代码，编译期生效 -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Spring Boot 测试依赖，用于后续 Controller、配置类和模板渲染测试 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <!-- Spring Boot Maven 插件，用于打包可执行 Jar -->
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <!-- 打包时排除 Lombok，避免无意义依赖进入运行包 -->
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

如果项目明确使用旧版 Spring Boot 2，可以考虑 `com.ibeetl:beetl-framework-starter`；但在 Spring Boot 3 项目中，不建议将它作为默认方案，因为 Maven Central 显示该 Starter 最新可见版本为 `1.2.40.Beetl.RELEASE`，发布时间早于 Spring Boot 3 正式发布。([Maven Central](https://central.sonatype.com/artifact/com.ibeetl/beetl-framework-starter?utm_source=chatgpt.com))

依赖添加完成后，可以执行以下命令确认依赖解析是否正常：

```bash
# 清理并编译项目，检查依赖是否能够正常下载和编译
mvn clean compile

# 查看 Beetl 相关依赖是否进入依赖树
mvn dependency:tree -Dincludes=com.ibeetl

# 查看 Spring Boot Web 相关依赖，确认项目使用的是 Spring Boot 3 体系
mvn dependency:tree -Dincludes=org.springframework.boot
```

命令执行成功后，说明基础依赖已准备完成。后续即可继续编写 `application.yml`、`beetl.properties` 和 Beetl 视图解析器配置。



## 基础配置

本节用于完成 Spring Boot 3 项目中 Beetl 的基础接入，包括 Starter 依赖、模板目录、视图后缀、模板编码和缓存检查策略。Spring Boot 3 项目建议优先选择面向 JDK 17 / Spring Boot 3 的 Beetl Starter，而不是较早的 `beetl-framework-starter`；Maven Central 中可以看到 `beetl-springboot-starter-jdk17` 用于 JDK 17 场景，而旧的 `beetl-framework-starter` 版本线主要停留在 Spring Boot 2 时代。([Maven Central](https://central.sonatype.com/artifact/com.ibeetl/beetl-springboot-starter-jdk17/3.20.0.RELEASE?utm_source=chatgpt.com))

### Beetl Starter 配置

Beetl Starter 负责把 Beetl 模板引擎接入 Spring MVC 的视图解析流程。配置完成后，Controller 可以直接返回视图名称，Spring MVC 会通过 Beetl 渲染 `templates` 目录下的模板文件。

文件位置：`pom.xml`

```xml
<properties>
    <!-- Spring Boot 3 最低要求 Java 17 -->
    <java.version>17</java.version>

    <!-- Beetl Spring Boot 3 / JDK17 Starter 版本，生产项目建议统一在父 POM 中管理 -->
    <beetl.version>3.20.0.RELEASE</beetl.version>
</properties>

<dependencies>
    <!-- Spring MVC、内嵌 Tomcat、JSON 序列化等 Web 基础能力 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Beetl 对 Spring Boot 3 / JDK17 场景的 Starter 支持 -->
    <dependency>
        <groupId>com.ibeetl</groupId>
        <artifactId>beetl-springboot-starter-jdk17</artifactId>
        <version>${beetl.version}</version>
    </dependency>

    <!-- Hutool 工具类，后续可用于日期、字符串、集合和自定义函数处理 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.8.36</version>
    </dependency>

    <!-- Lombok 简化 VO、DTO、配置类等样板代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

如果项目中已经引入过旧版 `beetl-framework-starter`，建议先移除，避免同一个项目中同时存在两套 Beetl 自动配置逻辑。旧版 Starter 在 Maven Central 中显示的最新版本为 `1.2.40.Beetl.RELEASE`，其 POM 中使用的是 Spring Boot 2.5.2 父工程，不适合作为 Spring Boot 3 项目的默认选择。([Maven Central](https://central.sonatype.com/artifact/com.ibeetl/beetl-framework-starter?utm_source=chatgpt.com))

依赖添加完成后执行以下命令检查：

```bash
# 检查依赖能否正常解析和编译
mvn clean compile

# 查看 Beetl 相关依赖
mvn dependency:tree -Dincludes=com.ibeetl

# 查看 Spring Boot Web 相关依赖
mvn dependency:tree -Dincludes=org.springframework.boot
```

如果 `mvn clean compile` 成功，并且依赖树中出现 `beetl-springboot-starter-jdk17`，说明基础依赖已经接入完成。

### 模板路径配置

模板路径用于指定 Beetl 从哪里加载页面文件。常规 Spring Boot 项目建议将模板文件统一放在 `src/main/resources/templates` 下，并通过视图名称定位具体模板。

推荐目录如下：

```text
src/main/resources
├── application.yml
├── beetl.properties
├── static
│   ├── css
│   │   └── app.css
│   ├── js
│   │   └── app.js
│   └── images
│       └── logo.png
└── templates
    ├── index.html
    ├── layout
    │   ├── header.html
    │   ├── footer.html
    │   └── main.html
    └── user
        ├── list.html
        ├── detail.html
        └── form.html
```

文件位置：`src/main/resources/application.yml`

```yaml
server:
  port: 8080

spring:
  application:
    name: springboot3-beetl-demo

# Beetl 视图配置
beetl:
  # 模板根目录，默认从 classpath 下加载
  templatesPath: templates

  # 模板文件后缀，Controller 返回 user/list 时会定位到 user/list.html
  suffix: html
```

在该配置下，Controller 返回的视图名称与模板文件的对应关系如下：

| Controller 返回值 | 实际模板位置                            |
| ----------------- | --------------------------------------- |
| `index`           | `classpath:/templates/index.html`       |
| `user/list`       | `classpath:/templates/user/list.html`   |
| `user/detail`     | `classpath:/templates/user/detail.html` |
| `user/form`       | `classpath:/templates/user/form.html`   |

为了保持模板路径清晰，建议 Controller 返回值不要以 `/` 开头，也不要带 `.html` 后缀。推荐写法是 `return "user/list";`，而不是 `return "/user/list.html";`。

### 编码与缓存配置

编码配置用于避免中文乱码，缓存配置用于控制模板文件修改后是否自动刷新。开发环境建议开启模板变更检查，生产环境建议关闭自动检查以减少运行时开销。Beetl 的基础配置文件支持 `TEMPLATE_CHARSET`、`RESOURCE.autoCheck`、占位符定界符和语句定界符等配置项。([javamonkey.github.io](https://javamonkey.github.io/guide/beetl.html?utm_source=chatgpt.com))

文件位置：`src/main/resources/beetl.properties`

```properties
# 模板字符集，建议与项目源码、配置文件、HTML 页面统一使用 UTF-8
TEMPLATE_CHARSET=UTF-8

# 变量输出占位符开始标记，例如 ${userName}
DELIMITER_PLACEHOLDER_START=${

# 变量输出占位符结束标记
DELIMITER_PLACEHOLDER_END=}

# Beetl 逻辑语句开始标记，例如 <% if (...) { %>
DELIMITER_STATEMENT_START=<%

# Beetl 逻辑语句结束标记
DELIMITER_STATEMENT_END=%>

# 是否检测模板文件变化，开发环境建议 true，生产环境建议 false
RESOURCE.autoCheck=true

# 是否允许模板中直接调用 Java 对象方法
NATIVE_CALL=TRUE

# 是否启用严格 MVC，开启后会限制模板中编写复杂业务逻辑
MVC_STRICT=FALSE
```

开发环境可以保持如下配置：

```properties
# 开发环境：方便调试，模板修改后自动生效
RESOURCE.autoCheck=true
```

生产环境建议改为：

```properties
# 生产环境：减少模板文件检查开销，提高渲染稳定性
RESOURCE.autoCheck=false
```

如果项目使用多环境配置，也可以把缓存策略拆分到不同 Profile 中管理。

文件位置：`src/main/resources/application-dev.yml`

```yaml
beetl:
  # 开发环境保留模板热检查，便于页面调试
  cache: false
```

文件位置：`src/main/resources/application-prod.yml`

```yaml
beetl:
  # 生产环境开启缓存或关闭模板自动检查，避免频繁检测模板文件
  cache: true
```

实际项目中，`application.yml` 管理 Spring Boot 级别配置，`beetl.properties` 管理 Beetl 引擎级别配置。不要把所有配置混在一个文件中，否则后续排查视图解析、模板语法和缓存问题时会不够清晰。

## 模板开发

本节说明 Beetl 模板文件的基本写法。模板开发的核心是：后端 Controller 将数据放入 Model，Beetl 在 HTML 模板中通过变量输出、条件判断、循环和模板引入完成页面渲染。

### 页面模板语法

Beetl 模板通常是 HTML 与 Beetl 表达式的组合。HTML 负责页面结构，`${...}` 负责变量输出，`<% ... %>` 负责条件、循环、变量定义等逻辑控制。Beetl 文档中也说明，占位符用于输出变量，逻辑语句可以通过配置文件中的定界符控制。([javamonkey.github.io](https://javamonkey.github.io/guide/beetl.html?utm_source=chatgpt.com))

文件位置：`src/main/resources/templates/index.html`

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>${title}</title>
    <link rel="stylesheet" href="/css/app.css">
</head>
<body>
<h1>${title}</h1>

<% var description = "Spring Boot 3 集成 Beetl 模板引擎"; %>
<p>${description}</p>

<script src="/js/app.js"></script>
</body>
</html>
```

上面模板中，`${title}` 表示从 Model 中读取 `title` 变量并输出到页面；`<% var description = "..."; %>` 表示在模板内部声明局部变量。

如果 Controller 中传入的数据如下：

```java
model.addAttribute("title", "Beetl 首页");
```

最终页面中的 `<title>` 和 `<h1>` 都会渲染为 `Beetl 首页`。

### 变量输出

变量输出用于把后端传入的 Model 数据显示到页面中。常见输出包括普通字符串、对象属性、Map 值、集合元素和带默认值的安全输出。

文件位置：`src/main/resources/templates/user/detail.html`

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>用户详情</title>
</head>
<body>
<h1>用户详情</h1>

<div class="user-card">
    <p>用户编号：${user.id}</p>
    <p>用户名称：${user.name}</p>
    <p>用户邮箱：${user.email}</p>

    <!-- 使用安全输出，避免字段为空时模板渲染异常 -->
    <p>用户备注：${user.remark!"暂无备注"}</p>

    <!-- Map 数据输出 -->
    <p>用户等级：${userExt["level"]!"普通用户"}</p>
</div>
</body>
</html>
```

变量输出常用写法如下：

| 写法                | 说明                       |
| ------------------- | -------------------------- |
| `${name}`           | 输出普通变量               |
| `${user.name}`      | 输出对象属性               |
| `${user.email!"-"}` | 字段为空时输出默认值       |
| `${map["key"]}`     | 输出 Map 中指定 key 的值   |
| `${list[0].name}`   | 输出集合中第一个元素的属性 |

后端传入对象时，建议使用 VO 而不是 Entity。VO 字段应面向页面展示，避免把数据库字段、敏感字段或内部状态直接暴露到模板中。

### 条件判断与循环

条件判断用于控制页面内容是否显示，循环用于渲染列表、菜单、表格、选项等重复结构。实际后台页面中，列表渲染是最常见的 Beetl 使用场景。

文件位置：`src/main/resources/templates/user/list.html`

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>用户列表</title>
</head>
<body>
<h1>用户列表</h1>

<% if (userList == null || userList.~size == 0) { %>
    <div class="empty">暂无用户数据</div>
<% } else { %>
    <table border="1" cellspacing="0" cellpadding="8">
        <thead>
        <tr>
            <th>序号</th>
            <th>用户编号</th>
            <th>用户名称</th>
            <th>邮箱</th>
            <th>状态</th>
        </tr>
        </thead>
        <tbody>
        <% for (user in userList) { %>
            <tr>
                <td>${userLP.index}</td>
                <td>${user.id}</td>
                <td>${user.name}</td>
                <td>${user.email!"-"}</td>
                <td>
                    <% if (user.enabled) { %>
                        <span class="status-normal">启用</span>
                    <% } else { %>
                        <span class="status-disabled">禁用</span>
                    <% } %>
                </td>
            </tr>
        <% } %>
        </tbody>
    </table>
<% } %>
</body>
</html>
```

循环中建议关注两个点。第一，集合可能为空，页面应提供空数据展示；第二，字段可能为空，输出时应使用默认值，避免页面出现异常或显示不完整。

常见判断写法：

```html
<% if (user.enabled) { %>
    <span>启用</span>
<% } else { %>
    <span>禁用</span>
<% } %>
```

常见循环写法：

```html
<% for (user in userList) { %>
    <p>${user.name}</p>
<% } %>
```

对于后台管理页面，建议把复杂业务判断前置到 Service 或 Controller 中，模板中只保留轻量展示逻辑。例如“是否允许删除”“是否显示编辑按钮”“状态名称转换”等逻辑，可以在后端提前处理成字段，模板只负责读取并展示。

### 模板引入与布局

模板引入用于复用公共页面片段，例如头部、底部、菜单、分页条和公共脚本。布局模板用于统一页面结构，避免每个页面重复编写完整 HTML 骨架。

推荐将公共模板放在 `templates/layout` 目录下：

```text
templates
├── layout
│   ├── header.html
│   ├── footer.html
│   └── main.html
└── user
    └── list.html
```

文件位置：`src/main/resources/templates/layout/header.html`

```html
<header class="app-header">
    <div class="logo">Beetl Demo</div>
    <nav>
        <a href="/">首页</a>
        <a href="/user/list">用户列表</a>
    </nav>
</header>
```

文件位置：`src/main/resources/templates/layout/footer.html`

```html
<footer class="app-footer">
    <p>Copyright © ${currentYear!"2026"} Spring Boot 3 Beetl Demo</p>
</footer>
```

用户列表页面中引入公共头部和底部：

文件位置：`src/main/resources/templates/user/list.html`

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>用户列表</title>
    <link rel="stylesheet" href="/css/app.css">
</head>
<body>
<% include("/layout/header.html"){} %>

<main class="app-main">
    <h1>用户列表</h1>

    <% if (userList == null || userList.~size == 0) { %>
        <div class="empty">暂无用户数据</div>
    <% } else { %>
        <table border="1" cellspacing="0" cellpadding="8">
            <thead>
            <tr>
                <th>用户编号</th>
                <th>用户名称</th>
                <th>邮箱</th>
            </tr>
            </thead>
            <tbody>
            <% for (user in userList) { %>
                <tr>
                    <td>${user.id}</td>
                    <td>${user.name}</td>
                    <td>${user.email!"-"}</td>
                </tr>
            <% } %>
            </tbody>
        </table>
    <% } %>
</main>

<% include("/layout/footer.html"){} %>
</body>
</html>
```

公共模板中如果依赖全局变量，例如 `currentYear`、`loginUser`、`menuList`，建议后续在“全局处理”章节中统一注入，不要在每个 Controller 中重复添加。这样可以避免页面公共区域与具体业务 Controller 强耦合。

模板开发阶段可以按以下步骤验证：

```bash
# 启动项目
mvn spring-boot:run

# 访问首页
curl http://localhost:8080/

# 访问用户列表页
curl http://localhost:8080/user/list
```

如果页面显示乱码，优先检查 `beetl.properties` 中的 `TEMPLATE_CHARSET=UTF-8`、HTML 中的 `<meta charset="UTF-8">`，以及 IDE 文件编码是否统一为 UTF-8。



## 后端集成

本节用于说明 Spring Boot 3 Controller 如何与 Beetl 页面模板配合使用。典型流程是 Controller 接收请求、调用 Service 获取数据、通过 `Model` 写入模板变量，最后返回逻辑视图名称，由 Spring MVC 的 ViewResolver 解析并渲染页面。Spring MVC 中 `String` 返回值可以作为视图名称解析，`Model` 用于向隐式模型中添加页面属性。([Home](https://docs.spring.vmware.com/spring-framework/reference/web/webmvc/mvc-controller/ann-methods/return-types.html?utm_source=chatgpt.com))

### Controller 页面跳转

Controller 页面跳转用于处理浏览器访问路径与 Beetl 模板之间的映射关系。普通页面跳转方法通常使用 `@GetMapping`，返回值是模板路径，不需要添加 `.html` 后缀。

先准备本章节需要的基础文件结构：

```text
src/main/java/io/github/atengk
├── controller
│   └── UserPageController.java
├── service
│   ├── UserService.java
│   └── impl
│       └── UserServiceImpl.java
├── form
│   └── UserForm.java
└── vo
    └── UserVO.java

src/main/resources/templates
└── user
    ├── list.html
    ├── detail.html
    └── form.html
```

表单校验需要额外引入 `spring-boot-starter-validation`。Spring Boot 官方文档说明，只要 Bean Validation 实现位于 classpath 中，方法参数、返回值和模型对象上的 Jakarta Validation 约束即可被启用；Spring MVC 也支持对 `@ModelAttribute`、`@RequestBody` 等参数进行 `@Valid` 校验。([Home](https://docs.spring.io/spring-boot/reference/io/validation.html?utm_source=chatgpt.com))

文件位置：`pom.xml`

```xml
<dependencies>
    <!-- Spring MVC、内嵌 Tomcat、JSON 转换等 Web 基础能力 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Beetl Spring Boot 3 / JDK17 Starter -->
    <dependency>
        <groupId>com.ibeetl</groupId>
        <artifactId>beetl-springboot-starter-jdk17</artifactId>
        <version>${beetl.version}</version>
    </dependency>

    <!-- Jakarta Bean Validation，表单参数校验需要使用 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- Hutool 工具类，简化字符串、集合、ID 等处理 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.8.36</version>
    </dependency>

    <!-- Lombok 简化实体、表单和构造器代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

下面的 Controller 负责用户列表、详情、新增、编辑和保存页面流转。

文件位置：`src/main/java/io/github/atengk/controller/UserPageController.java`

```java
package io.github.atengk.controller;

import cn.hutool.core.collection.CollUtil;
import io.github.atengk.form.UserForm;
import io.github.atengk.service.UserService;
import io.github.atengk.vo.UserVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户页面控制器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/user")
public class UserPageController {

    private final UserService userService;

    /**
     * 用户列表页
     *
     * @param model 页面模型
     * @return 视图路径
     */
    @GetMapping("/list")
    public String list(Model model) {
        List<UserVO> userList = userService.listUsers();

        model.addAttribute("userList", userList);
        model.addAttribute("empty", CollUtil.isEmpty(userList));

        log.info("访问用户列表页，用户数量：{}", userList.size());
        return "user/list";
    }

    /**
     * 用户详情页
     *
     * @param id    用户编号
     * @param model 页面模型
     * @return 视图路径
     */
    @GetMapping("/detail/{id}")
    public String detail(@PathVariable String id, Model model) {
        UserVO user = userService.getUser(id);

        if (user == null) {
            log.warn("用户详情不存在，用户编号：{}", id);
            model.addAttribute("message", "用户不存在");
            return "user/detail";
        }

        model.addAttribute("user", user);
        return "user/detail";
    }

    /**
     * 新增用户页
     *
     * @param model 页面模型
     * @return 视图路径
     */
    @GetMapping("/add")
    public String add(Model model) {
        UserForm form = new UserForm();
        form.setEnabled(true);

        fillFormModel(model, "新增用户", form, new HashMap<>());
        return "user/form";
    }

    /**
     * 编辑用户页
     *
     * @param id    用户编号
     * @param model 页面模型
     * @return 视图路径
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable String id, Model model) {
        UserVO user = userService.getUser(id);

        if (user == null) {
            log.warn("编辑用户不存在，用户编号：{}", id);
            model.addAttribute("message", "用户不存在");
            return "user/detail";
        }

        UserForm form = UserForm.from(user);
        fillFormModel(model, "编辑用户", form, new HashMap<>());
        return "user/form";
    }

    /**
     * 保存用户表单
     *
     * @param form               表单参数
     * @param bindingResult      校验结果
     * @param model              页面模型
     * @param redirectAttributes 重定向参数
     * @return 视图路径或重定向路径
     */
    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("form") UserForm form,
                       BindingResult bindingResult,
                       Model model,
                       RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            Map<String, String> formErrors = buildErrorMap(bindingResult);
            log.warn("用户表单校验失败，错误数量：{}", formErrors.size());

            String pageTitle = form.isCreateMode() ? "新增用户" : "编辑用户";
            fillFormModel(model, pageTitle, form, formErrors);
            return "user/form";
        }

        UserVO savedUser = userService.saveUser(form);
        redirectAttributes.addFlashAttribute("message", "用户保存成功");

        log.info("用户保存成功，用户编号：{}，用户名称：{}", savedUser.getId(), savedUser.getName());
        return "redirect:/user/list";
    }

    /**
     * 填充表单页面公共模型
     *
     * @param model      页面模型
     * @param pageTitle  页面标题
     * @param form       表单对象
     * @param formErrors 表单错误
     */
    private void fillFormModel(Model model, String pageTitle, UserForm form, Map<String, String> formErrors) {
        model.addAttribute("pageTitle", pageTitle);
        model.addAttribute("form", form);
        model.addAttribute("formErrors", formErrors);
    }

    /**
     * 构建字段错误映射
     *
     * @param bindingResult 校验结果
     * @return 字段错误映射
     */
    private Map<String, String> buildErrorMap(BindingResult bindingResult) {
        Map<String, String> errorMap = new HashMap<>();

        for (FieldError fieldError : bindingResult.getFieldErrors()) {
            errorMap.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        return errorMap;
    }

}
```

页面跳转对应关系如下：

| 请求路径            | Controller 方法 | 返回视图                                            |
| ------------------- | --------------- | --------------------------------------------------- |
| `/user/list`        | `list`          | `user/list`                                         |
| `/user/detail/{id}` | `detail`        | `user/detail`                                       |
| `/user/add`         | `add`           | `user/form`                                         |
| `/user/edit/{id}`   | `edit`          | `user/form`                                         |
| `/user/save`        | `save`          | 校验失败返回 `user/form`，成功重定向到 `/user/list` |

### Model 数据传递

Model 数据传递用于把后端对象、集合、状态值、提示消息等写入模板上下文。Beetl 模板中可以通过 `${变量名}`、`${对象.属性}`、`${集合变量}` 读取这些数据。

下面的 VO 用于页面展示，不直接暴露数据库 Entity。

文件位置：`src/main/java/io/github/atengk/vo/UserVO.java`

```java
package io.github.atengk.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户页面展示对象
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserVO {

    /**
     * 用户编号
     */
    private String id;

    /**
     * 用户名称
     */
    private String name;

    /**
     * 用户邮箱
     */
    private String email;

    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 用户备注
     */
    private String remark;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

}
```

下面的 Service 使用内存 Map 模拟数据存储，便于文档示例直接运行。生产项目中可以替换为 MyBatis-Plus、JPA 或其他持久层实现。

文件位置：`src/main/java/io/github/atengk/service/UserService.java`

```java
package io.github.atengk.service;

import io.github.atengk.form.UserForm;
import io.github.atengk.vo.UserVO;

import java.util.List;

/**
 * 用户服务接口
 *
 * @author Ateng
 * @since 2026-05-06
 */
public interface UserService {

    /**
     * 查询用户列表
     *
     * @return 用户列表
     */
    List<UserVO> listUsers();

    /**
     * 查询用户详情
     *
     * @param id 用户编号
     * @return 用户详情
     */
    UserVO getUser(String id);

    /**
     * 保存用户
     *
     * @param form 表单参数
     * @return 保存后的用户
     */
    UserVO saveUser(UserForm form);

}
```

文件位置：`src/main/java/io/github/atengk/service/impl/UserServiceImpl.java`

```java
package io.github.atengk.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.form.UserForm;
import io.github.atengk.service.UserService;
import io.github.atengk.vo.UserVO;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用户服务内存实现
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Service
public class UserServiceImpl implements UserService {

    private final Map<String, UserVO> userStore = new ConcurrentHashMap<>();

    /**
     * 初始化演示数据
     */
    @PostConstruct
    public void initData() {
        if (CollUtil.isNotEmpty(userStore)) {
            return;
        }

        UserVO admin = UserVO.builder()
                .id(IdUtil.fastSimpleUUID())
                .name("管理员")
                .email("admin@example.com")
                .enabled(true)
                .remark("系统初始化用户")
                .createTime(LocalDateTime.now())
                .build();

        UserVO testUser = UserVO.builder()
                .id(IdUtil.fastSimpleUUID())
                .name("测试用户")
                .email("test@example.com")
                .enabled(false)
                .remark("用于页面渲染测试")
                .createTime(LocalDateTime.now())
                .build();

        userStore.put(admin.getId(), admin);
        userStore.put(testUser.getId(), testUser);

        log.info("初始化用户演示数据完成，数量：{}", userStore.size());
    }

    /**
     * 查询用户列表
     *
     * @return 用户列表
     */
    @Override
    public List<UserVO> listUsers() {
        return userStore.values()
                .stream()
                .sorted(Comparator.comparing(UserVO::getCreateTime).reversed())
                .toList();
    }

    /**
     * 查询用户详情
     *
     * @param id 用户编号
     * @return 用户详情
     */
    @Override
    public UserVO getUser(String id) {
        if (StrUtil.isBlank(id)) {
            log.warn("查询用户详情失败，用户编号为空");
            return null;
        }

        return userStore.get(id);
    }

    /**
     * 保存用户
     *
     * @param form 表单参数
     * @return 保存后的用户
     */
    @Override
    public UserVO saveUser(UserForm form) {
        String id = StrUtil.blankToDefault(form.getId(), IdUtil.fastSimpleUUID());
        UserVO oldUser = userStore.get(id);

        UserVO user = UserVO.builder()
                .id(id)
                .name(StrUtil.trim(form.getName()))
                .email(StrUtil.trim(form.getEmail()))
                .enabled(Boolean.TRUE.equals(form.getEnabled()))
                .remark(StrUtil.trimToEmpty(form.getRemark()))
                .createTime(oldUser == null ? LocalDateTime.now() : oldUser.getCreateTime())
                .build();

        userStore.put(id, user);

        log.info("保存用户数据完成，用户编号：{}，是否新增：{}", id, oldUser == null);
        return user;
    }

}
```

在 Controller 中调用 `model.addAttribute("userList", userList)` 后，模板中即可使用 `${userList}` 进行循环渲染；调用 `model.addAttribute("user", user)` 后，模板中即可使用 `${user.name}`、`${user.email}` 等属性。

### 表单参数接收

表单参数接收用于处理新增、编辑页面提交的数据。Spring MVC 可以使用 `@ModelAttribute` 将表单字段绑定到 Java 对象，并通过 `@Valid` 触发 Jakarta Bean Validation 校验；如果方法参数后紧跟 `BindingResult`，则可以在当前 Controller 中接收校验结果并返回原页面展示错误信息。([Home](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-validation.html?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/form/UserForm.java`

```java
package io.github.atengk.form;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.vo.UserVO;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户表单对象
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
public class UserForm {

    /**
     * 用户编号，新增时为空，编辑时有值
     */
    private String id;

    /**
     * 用户名称
     */
    @NotBlank(message = "用户名称不能为空")
    @Size(max = 30, message = "用户名称不能超过30个字符")
    private String name;

    /**
     * 用户邮箱
     */
    @NotBlank(message = "用户邮箱不能为空")
    @Email(message = "用户邮箱格式不正确")
    @Size(max = 100, message = "用户邮箱不能超过100个字符")
    private String email;

    /**
     * 是否启用
     */
    private Boolean enabled = true;

    /**
     * 用户备注
     */
    @Size(max = 200, message = "用户备注不能超过200个字符")
    private String remark;

    /**
     * 是否新增模式
     *
     * @return true 表示新增，false 表示编辑
     */
    public boolean isCreateMode() {
        return StrUtil.isBlank(id);
    }

    /**
     * 从用户展示对象转换为表单对象
     *
     * @param user 用户展示对象
     * @return 表单对象
     */
    public static UserForm from(UserVO user) {
        UserForm form = new UserForm();
        form.setId(user.getId());
        form.setName(user.getName());
        form.setEmail(user.getEmail());
        form.setEnabled(user.getEnabled());
        form.setRemark(user.getRemark());
        return form;
    }

}
```

页面表单提交时，字段名称需要与 `UserForm` 属性名称一致。例如 `<input name="name">` 会绑定到 `UserForm.name`，`<input name="email">` 会绑定到 `UserForm.email`。

## 静态资源处理

本节用于说明 CSS、JavaScript、图片和下载文件在 Spring Boot 3 项目中的放置方式。Spring Boot 默认会从 classpath 下的 `/static`、`/public`、`/resources`、`/META-INF/resources` 等目录提供静态资源，常规项目中推荐使用 `src/main/resources/static`。([Home](https://docs.spring.io/spring-boot/docs/3.0.x/reference/html/web.html?utm_source=chatgpt.com))

### CSS 与 JavaScript 目录

CSS 与 JavaScript 建议统一放在 `src/main/resources/static/css` 和 `src/main/resources/static/js` 目录下。模板页面可以直接通过 `/css/app.css`、`/js/app.js` 访问这些资源。

推荐目录：

```text
src/main/resources/static
├── css
│   └── app.css
├── js
│   └── app.js
└── images
    └── logo.png
```

文件位置：`src/main/resources/static/css/app.css`

```css
body {
    margin: 0;
    padding: 0;
    font-family: Arial, "Microsoft YaHei", sans-serif;
    background: #f5f7fa;
    color: #303133;
}

.app-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    height: 56px;
    padding: 0 24px;
    background: #ffffff;
    border-bottom: 1px solid #e4e7ed;
}

.app-header a {
    margin-left: 16px;
    color: #409eff;
    text-decoration: none;
}

.app-main {
    width: 960px;
    margin: 24px auto;
    padding: 24px;
    background: #ffffff;
    border-radius: 6px;
}

.table {
    width: 100%;
    border-collapse: collapse;
}

.table th,
.table td {
    padding: 10px;
    border: 1px solid #ebeef5;
    text-align: left;
}

.form-item {
    margin-bottom: 16px;
}

.form-item label {
    display: inline-block;
    width: 90px;
}

.form-item input,
.form-item textarea {
    width: 320px;
    padding: 8px;
    border: 1px solid #dcdfe6;
    border-radius: 4px;
}

.error {
    margin-left: 90px;
    color: #f56c6c;
    font-size: 13px;
}

.message {
    padding: 10px 12px;
    margin-bottom: 16px;
    background: #f0f9eb;
    color: #67c23a;
    border: 1px solid #e1f3d8;
}

.empty {
    padding: 24px;
    color: #909399;
    text-align: center;
    border: 1px dashed #dcdfe6;
}

.button {
    display: inline-block;
    padding: 8px 14px;
    color: #ffffff;
    background: #409eff;
    border: none;
    border-radius: 4px;
    text-decoration: none;
    cursor: pointer;
}

.button.secondary {
    background: #909399;
}

.status-normal {
    color: #67c23a;
}

.status-disabled {
    color: #f56c6c;
}
```

文件位置：`src/main/resources/static/js/app.js`

```javascript
(function () {
    console.log("Spring Boot 3 Beetl 页面脚本已加载");

    window.confirmSubmit = function () {
        return confirm("确认提交当前表单吗？");
    };

    window.goBack = function () {
        window.history.back();
    };
})();
```

模板中引用方式如下：

```html
<link rel="stylesheet" href="/css/app.css">
<script src="/js/app.js"></script>
```

如果项目配置了上下文路径，例如 `server.servlet.context-path=/beetl-demo`，则浏览器访问路径会变成 `/beetl-demo/css/app.css`。模板中如需兼容上下文路径，建议后续通过全局变量统一注入 `ctxPath`，避免硬编码。

### 图片与文件资源访问

图片、图标、公开下载文件可以放在 `static/images`、`static/files` 等目录下。只要位于 Spring Boot 默认静态资源目录中，浏览器即可直接访问。([Home](https://docs.spring.io/spring-boot/docs/3.0.x/reference/html/web.html?utm_source=chatgpt.com))

推荐目录：

```text
src/main/resources/static
├── images
│   ├── logo.png
│   └── avatar-default.png
└── files
    └── demo.pdf
```

模板中引用图片：

```html
<img src="/images/logo.png" alt="系统 Logo">
<img src="/images/avatar-default.png" alt="默认头像">
```

模板中引用下载文件：

```html
<a href="/files/demo.pdf" target="_blank">查看示例 PDF</a>
```

静态资源只适合放置公开可访问内容，不建议放置用户隐私文件、合同、导入模板中的敏感数据或需要权限控制的业务文件。需要鉴权的文件下载应通过 Controller 接口读取文件流并进行权限校验。

## 常用功能开发

本节基于前面的 Controller、Service、VO、Form 和静态资源配置，补充列表、详情、新增、编辑、提交和校验页面的完整模板示例。Beetl 模板中常见语法包括 `${...}` 变量输出、`<% if (...) { %>` 条件判断、`<% for (...) { %>` 循环，以及 `includeFileTemplate` 引入公共模板；这些写法属于 Beetl 模板的基础语法能力。([beetl.sourceforge.net](https://beetl.sourceforge.net/guide/guide.html?utm_source=chatgpt.com))

### 列表页面渲染

列表页面用于展示多条数据，通常包含查询结果、空数据提示、操作按钮和跳转链接。后端通过 `model.addAttribute("userList", userList)` 传入集合，模板通过 `for` 循环渲染表格行。

文件位置：`src/main/resources/templates/user/list.html`

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>用户列表</title>
    <link rel="stylesheet" href="/css/app.css">
</head>
<body>
<header class="app-header">
    <strong>Beetl 用户管理</strong>
    <nav>
        <a href="/user/list">用户列表</a>
        <a href="/user/add">新增用户</a>
    </nav>
</header>

<main class="app-main">
    <h1>用户列表</h1>

    <% if (message!"" != "") { %>
        <div class="message">${message}</div>
    <% } %>

    <p>
        <a class="button" href="/user/add">新增用户</a>
    </p>

    <% if (empty) { %>
        <div class="empty">暂无用户数据</div>
    <% } else { %>
        <table class="table">
            <thead>
            <tr>
                <th>用户编号</th>
                <th>用户名称</th>
                <th>邮箱</th>
                <th>状态</th>
                <th>备注</th>
                <th>操作</th>
            </tr>
            </thead>
            <tbody>
            <% for (user in userList) { %>
                <tr>
                    <td>${user.id}</td>
                    <td>${user.name}</td>
                    <td>${user.email}</td>
                    <td>
                        <% if (user.enabled) { %>
                            <span class="status-normal">启用</span>
                        <% } else { %>
                            <span class="status-disabled">禁用</span>
                        <% } %>
                    </td>
                    <td>${user.remark!"-"}</td>
                    <td>
                        <a href="/user/detail/${user.id}">详情</a>
                        <a href="/user/edit/${user.id}">编辑</a>
                    </td>
                </tr>
            <% } %>
            </tbody>
        </table>
    <% } %>
</main>

<script src="/js/app.js"></script>
</body>
</html>
```

访问地址：

```bash
curl http://localhost:8080/user/list
```

浏览器访问 `http://localhost:8080/user/list` 后，应能看到 Service 初始化的用户数据。如果列表为空，页面会展示“暂无用户数据”。

### 详情页面渲染

详情页面用于展示单条数据，通常通过路径参数传入业务编号。后端根据 `/user/detail/{id}` 查询用户详情，并向模板传入 `user` 对象。

文件位置：`src/main/resources/templates/user/detail.html`

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>用户详情</title>
    <link rel="stylesheet" href="/css/app.css">
</head>
<body>
<header class="app-header">
    <strong>Beetl 用户管理</strong>
    <nav>
        <a href="/user/list">用户列表</a>
        <a href="/user/add">新增用户</a>
    </nav>
</header>

<main class="app-main">
    <h1>用户详情</h1>

    <% if (message!"" != "") { %>
        <div class="empty">${message}</div>
        <p>
            <a class="button secondary" href="/user/list">返回列表</a>
        </p>
    <% } else { %>
        <table class="table">
            <tr>
                <th>用户编号</th>
                <td>${user.id}</td>
            </tr>
            <tr>
                <th>用户名称</th>
                <td>${user.name}</td>
            </tr>
            <tr>
                <th>用户邮箱</th>
                <td>${user.email}</td>
            </tr>
            <tr>
                <th>用户状态</th>
                <td>
                    <% if (user.enabled) { %>
                        <span class="status-normal">启用</span>
                    <% } else { %>
                        <span class="status-disabled">禁用</span>
                    <% } %>
                </td>
            </tr>
            <tr>
                <th>用户备注</th>
                <td>${user.remark!"-"}</td>
            </tr>
            <tr>
                <th>创建时间</th>
                <td>${user.createTime}</td>
            </tr>
        </table>

        <p>
            <a class="button" href="/user/edit/${user.id}">编辑</a>
            <a class="button secondary" href="/user/list">返回列表</a>
        </p>
    <% } %>
</main>

<script src="/js/app.js"></script>
</body>
</html>
```

验证方式：

```bash
# 先访问列表页，复制页面中的用户编号
curl http://localhost:8080/user/list

# 将 {id} 替换为实际用户编号
curl http://localhost:8080/user/detail/{id}
```

如果用户编号不存在，Controller 会返回同一个详情模板，但只显示“用户不存在”的提示信息。

### 新增与编辑页面

新增与编辑页面可以共用同一个模板。新增时 `form.id` 为空，编辑时 `form.id` 有值；页面标题由 `pageTitle` 控制，表单提交地址统一为 `/user/save`。

文件位置：`src/main/resources/templates/user/form.html`

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>${pageTitle}</title>
    <link rel="stylesheet" href="/css/app.css">
</head>
<body>
<header class="app-header">
    <strong>Beetl 用户管理</strong>
    <nav>
        <a href="/user/list">用户列表</a>
        <a href="/user/add">新增用户</a>
    </nav>
</header>

<main class="app-main">
    <h1>${pageTitle}</h1>

    <form method="post" action="/user/save" onsubmit="return confirmSubmit();">
        <input type="hidden" name="id" value="${form.id!""}">

        <div class="form-item">
            <label for="name">用户名称</label>
            <input id="name" name="name" type="text" value="${form.name!""}" maxlength="30">
            <% if (formErrors["name"]!"" != "") { %>
                <div class="error">${formErrors["name"]}</div>
            <% } %>
        </div>

        <div class="form-item">
            <label for="email">用户邮箱</label>
            <input id="email" name="email" type="text" value="${form.email!""}" maxlength="100">
            <% if (formErrors["email"]!"" != "") { %>
                <div class="error">${formErrors["email"]}</div>
            <% } %>
        </div>

        <div class="form-item">
            <label for="enabled">启用状态</label>
            <input id="enabled" name="enabled" type="checkbox" value="true" <% if (form.enabled!true) { %>checked<% } %>>
            <span>启用</span>
        </div>

        <div class="form-item">
            <label for="remark">用户备注</label>
            <textarea id="remark" name="remark" rows="4" maxlength="200">${form.remark!""}</textarea>
            <% if (formErrors["remark"]!"" != "") { %>
                <div class="error">${formErrors["remark"]}</div>
            <% } %>
        </div>

        <div class="form-item">
            <label></label>
            <button class="button" type="submit">保存</button>
            <a class="button secondary" href="/user/list">取消</a>
        </div>
    </form>
</main>

<script src="/js/app.js"></script>
</body>
</html>
```

访问新增页面：

```bash
curl http://localhost:8080/user/add
```

访问编辑页面：

```bash
curl http://localhost:8080/user/edit/{id}
```

新增和编辑复用同一个模板可以减少重复代码，但要保证 Controller 每次进入页面时都传入 `pageTitle`、`form`、`formErrors` 三个变量。否则模板读取变量时容易出现空值或未定义问题。

### 表单提交与校验

表单提交与校验用于处理用户输入的合法性。后端通过 `@Valid` 校验 `UserForm`，通过 `BindingResult` 接收校验错误；如果存在错误，则重新返回 `user/form` 页面并回显原始输入和错误信息。Spring MVC 对 `@ModelAttribute` 参数支持数据绑定，`@Valid` 或 `@Validated` 可以触发校验。([Home](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-validation.html?utm_source=chatgpt.com))

表单正常提交示例：

```bash
curl -X POST http://localhost:8080/user/save \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "name=张三" \
  -d "email=zhangsan@example.com" \
  -d "enabled=true" \
  -d "remark=通过 curl 新增的用户"
```

表单校验失败示例：

```bash
curl -X POST http://localhost:8080/user/save \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "name=" \
  -d "email=invalid-email" \
  -d "enabled=true" \
  -d "remark=测试非法邮箱"
```

校验失败时，页面会重新渲染 `user/form.html`，并在对应字段下方显示错误信息，例如“用户名称不能为空”“用户邮箱格式不正确”。

如果表单中存在复选框字段，需要注意未勾选时浏览器不会提交该字段。当前示例中 `UserForm.enabled` 默认值为 `true`，适合“新增用户默认启用”的场景；如果业务要求未勾选表示禁用，可以在 Controller 保存前增加兜底处理：

```java
if (form.getEnabled() == null) {
    form.setEnabled(false);
}
```

建议将该处理放入 `save` 方法的校验通过分支中，避免用户未勾选时保存出错。生产项目还应增加 CSRF 防护、重复提交控制、权限校验和服务端唯一性校验，例如邮箱不能重复、用户名不能重复等。



## 全局处理

本节用于处理多个页面都会使用的公共能力，例如系统名称、上下文路径、当前年份、登录用户、公共函数和异常页面。公共能力应尽量集中配置，避免在每个 Controller 中重复写入相同的 Model 数据。

### 公共变量配置

公共变量适合放置所有页面都需要读取的数据，例如系统名称、上下文路径、当前年份、登录用户、菜单列表和静态资源版本号。Spring MVC 中可以通过 `@ControllerAdvice` 和 `@ModelAttribute` 统一向所有页面模型注入数据，这种方式不依赖具体模板引擎，适合 Beetl、Thymeleaf、FreeMarker 等服务端模板通用场景。

文件位置：`src/main/java/io/github/atengk/config/GlobalModelAdvice.java`

```java
package io.github.atengk.config;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Map;

/**
 * 全局页面变量配置
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@ControllerAdvice
public class GlobalModelAdvice {

    /**
     * 注入所有页面可用的公共变量
     *
     * @param model   页面模型
     * @param request HTTP 请求
     */
    @ModelAttribute
    public void addGlobalAttributes(Model model, HttpServletRequest request) {
        String contextPath = StrUtil.blankToDefault(request.getContextPath(), "");
        String requestUri = request.getRequestURI();

        model.addAttribute("appName", "Spring Boot 3 Beetl Demo");
        model.addAttribute("ctxPath", contextPath);
        model.addAttribute("currentYear", DateUtil.thisYear());
        model.addAttribute("requestUri", requestUri);

        // 示例登录用户，实际项目应从 Sa-Token、Spring Security 或 Session 中读取
        model.addAttribute("loginUser", Map.of(
                "id", "10001",
                "name", "管理员",
                "role", "admin"
        ));

        log.debug("注入页面公共变量，请求路径：{}", requestUri);
    }

}
```

模板中可以直接读取这些公共变量。

文件位置：`src/main/resources/templates/layout/header.html`

```html
<header class="app-header">
    <strong>${appName}</strong>
    <nav>
        <a href="${ctxPath}/user/list">用户列表</a>
        <a href="${ctxPath}/user/add">新增用户</a>
    </nav>
    <span>当前用户：${loginUser.name!"未登录"}</span>
</header>
```

文件位置：`src/main/resources/templates/layout/footer.html`

```html
<footer class="app-footer">
    <p>Copyright © ${currentYear} ${appName}</p>
</footer>
```

使用公共变量后，页面中不再硬编码系统名称和上下文路径。后续如果项目配置了 `server.servlet.context-path=/beetl-demo`，页面链接也可以通过 `${ctxPath}` 自动适配。

### 自定义函数

自定义函数用于封装模板中的通用处理逻辑，例如日期格式化、字符串脱敏、金额格式化、字典转换等。模板中不建议写复杂 Java 逻辑，适合将可复用的展示逻辑封装为 Beetl 函数。

下面示例提供一个日期格式化函数，支持 `Date`、`LocalDate`、`LocalDateTime` 三类常见时间对象。

文件位置：`src/main/java/io/github/atengk/beetl/function/BeetlDateFormatFunction.java`

```java
package io.github.atengk.beetl.function;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import org.beetl.core.Context;
import org.beetl.core.Function;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * Beetl 日期格式化函数
 *
 * @author Ateng
 * @since 2026-05-06
 */
public class BeetlDateFormatFunction implements Function {

    private static final String DEFAULT_PATTERN = "yyyy-MM-dd HH:mm:ss";

    /**
     * 执行日期格式化
     *
     * @param paras 函数参数，第一个参数为日期对象，第二个参数为格式
     * @param ctx   Beetl 上下文
     * @return 格式化后的日期字符串
     */
    @Override
    public Object call(Object[] paras, Context ctx) {
        if (ArrayUtil.isEmpty(paras) || paras[0] == null) {
            return "";
        }

        Object value = paras[0];
        String pattern = paras.length > 1 && paras[1] != null
                ? StrUtil.toString(paras[1])
                : DEFAULT_PATTERN;

        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime.format(DateTimeFormatter.ofPattern(pattern));
        }

        if (value instanceof LocalDate localDate) {
            return localDate.format(DateTimeFormatter.ofPattern(pattern));
        }

        if (value instanceof Date date) {
            return DateUtil.format(date, pattern);
        }

        return StrUtil.toString(value);
    }

}
```

通过 `beetl.properties` 注册函数。

文件位置：`src/main/resources/beetl.properties`

```properties
# 注册自定义日期格式化函数，模板中可通过 dateFormat(...) 调用
FN.dateFormat=io.github.atengk.beetl.function.BeetlDateFormatFunction
```

模板中使用函数。

文件位置：`src/main/resources/templates/user/detail.html`

```html
<tr>
    <th>创建时间</th>
    <td>${dateFormat(user.createTime, "yyyy-MM-dd HH:mm")}</td>
</tr>
```

对于字典转换、金额格式化、手机号脱敏等展示逻辑，也可以按同样方式封装函数。建议只把“展示格式化”放入函数，不要把数据库查询、远程接口调用、权限判断等重业务逻辑放入模板函数。

### 全局异常页面

全局异常页面用于统一处理 404、500、业务异常和系统异常。Spring Boot 默认会将错误请求转发到 `/error`，项目可以通过 Controller 或模板文件提供自定义错误页面。

先配置错误页面模板路径。

文件位置：`src/main/resources/templates/error/404.html`

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>页面不存在</title>
    <link rel="stylesheet" href="${ctxPath}/css/app.css">
</head>
<body>
<% include("/layout/header.html"){} %>

<main class="app-main">
    <h1>页面不存在</h1>
    <p>请求地址不存在或已被移除。</p>
    <p>请求路径：${path!"-"}</p>
    <a class="button" href="${ctxPath}/user/list">返回首页</a>
</main>

<% include("/layout/footer.html"){} %>
</body>
</html>
```

文件位置：`src/main/resources/templates/error/500.html`

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>系统异常</title>
    <link rel="stylesheet" href="${ctxPath}/css/app.css">
</head>
<body>
<% include("/layout/header.html"){} %>

<main class="app-main">
    <h1>系统异常</h1>
    <p>${message!"系统处理请求时发生异常，请稍后重试。"}</p>
    <p>请求路径：${path!"-"}</p>
    <a class="button" href="${ctxPath}/user/list">返回首页</a>
</main>

<% include("/layout/footer.html"){} %>
</body>
</html>
```

如果需要在 Controller 层统一处理异常，可以增加 `@ControllerAdvice`。

文件位置：`src/main/java/io/github/atengk/config/GlobalExceptionHandler.java`

```java
package io.github.atengk.config;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * 全局页面异常处理器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理页面请求异常
     *
     * @param exception 异常对象
     * @param request   HTTP 请求
     * @param model     页面模型
     * @return 错误页面
     */
    @ExceptionHandler(Exception.class)
    public String handleException(Exception exception, HttpServletRequest request, Model model) {
        String path = request.getRequestURI();

        log.error("页面请求处理异常，请求路径：{}", path, exception);

        model.addAttribute("path", path);
        model.addAttribute("message", "系统处理请求时发生异常");

        return "error/500";
    }

}
```

对于 REST API 和页面请求混合的项目，建议将异常处理拆分：页面 Controller 返回 Beetl 错误页，接口 Controller 返回 JSON 错误体。否则接口调用发生异常时可能收到 HTML 页面，不利于前端处理。

## 开发调试

本节用于说明 Beetl 页面开发过程中常见的调试方式。模板调试主要关注三类问题：模板修改后不生效、变量不存在或为空、页面语法错误导致渲染失败。

### 模板热加载

模板热加载用于在开发环境中修改 `.html` 模板后立即生效。开发环境建议开启模板文件自动检查，生产环境建议关闭自动检查以减少运行时开销。

文件位置：`src/main/resources/beetl.properties`

```properties
# 开发环境开启模板自动检查，修改模板后无需重新打包
RESOURCE.autoCheck=true

# 模板统一使用 UTF-8，避免中文乱码
TEMPLATE_CHARSET=UTF-8
```

如果按 Profile 区分环境，可以在开发环境保留自动检查。

文件位置：`src/main/resources/application-dev.yml`

```yaml
spring:
  config:
    activate:
      on-profile: dev

# 开发环境建议保留详细日志，方便排查模板渲染问题
logging:
  level:
    io.github.atengk: debug
    org.beetl: debug
```

启动开发环境：

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

如果模板修改后仍然不生效，优先检查以下内容：

| 检查项       | 说明                                    |
| ------------ | --------------------------------------- |
| 模板文件位置 | 是否位于 `src/main/resources/templates` |
| 视图返回值   | Controller 返回值是否与模板路径一致     |
| 文件后缀     | 模板后缀是否与 Beetl 配置一致           |
| 自动检查     | `RESOURCE.autoCheck` 是否为 `true`      |
| 浏览器缓存   | CSS、JS 修改后是否被浏览器缓存          |
| 打包运行     | Jar 包运行时修改源码目录模板不会生效    |

### 日志排查

日志排查用于定位 Controller 是否进入、Model 数据是否正确、模板路径是否正确以及异常栈信息。页面渲染问题不应只看浏览器白屏，应结合后端日志和模板文件一起排查。

建议在开发环境配置日志级别。

文件位置：`src/main/resources/application-dev.yml`

```yaml
logging:
  level:
    # 项目业务日志
    io.github.atengk: debug

    # Spring MVC 请求映射和异常处理日志
    org.springframework.web: info

    # Beetl 模板引擎日志
    org.beetl: debug
```

Controller 中保留关键日志。

```java
log.info("访问用户列表页，用户数量：{}", userList.size());
log.warn("用户详情不存在，用户编号：{}", id);
log.error("页面请求处理异常，请求路径：{}", path, exception);
```

排查页面问题时建议按以下顺序处理：

| 问题现象   | 排查方向                                          |
| ---------- | ------------------------------------------------- |
| 404        | Controller 路径是否正确、请求方式是否匹配         |
| 500        | 查看异常栈，通常是模板语法或空值访问问题          |
| 页面空白   | 检查模板是否返回、Model 变量是否存在              |
| 样式丢失   | 检查 `/css/app.css` 是否可直接访问                |
| 中文乱码   | 检查 `TEMPLATE_CHARSET`、HTML `charset`、文件编码 |
| 修改不生效 | 检查模板缓存、浏览器缓存、运行方式                |

可以使用 `curl` 快速确认页面是否正常返回。

```bash
# 查看响应状态和响应头
curl -I http://localhost:8080/user/list

# 查看页面 HTML 内容
curl http://localhost:8080/user/list
```

### 常见问题处理

常见问题处理用于沉淀 Beetl 开发中的高频错误，便于后续排查和团队协作。

| 问题                   | 原因                                    | 处理方式                                                     |
| ---------------------- | --------------------------------------- | ------------------------------------------------------------ |
| 模板找不到             | Controller 返回路径与模板文件路径不一致 | 检查 `return "user/list"` 是否对应 `templates/user/list.html` |
| 变量不存在             | Controller 未写入 Model 或变量名不一致  | 检查 `model.addAttribute` 名称                               |
| 空值异常               | 模板直接访问空对象属性                  | 使用默认值输出，例如 `${user.remark!"-"}`                    |
| 表单错误不显示         | 未把 `BindingResult` 转成页面可读变量   | 将字段错误写入 `formErrors`                                  |
| 静态资源 404           | 文件未放入 `static` 目录                | 放入 `src/main/resources/static/css`、`static/js` 等目录     |
| 生产环境修改模板无效   | Jar 包中的模板已打包，不会读取源码目录  | 重新构建并发布 Jar                                           |
| 页面链接上下文路径错误 | 页面写死 `/user/list`                   | 使用 `${ctxPath}/user/list`                                  |

模板中建议使用安全输出，降低空值导致页面异常的概率。

```html
<p>备注：${user.remark!"-"}</p>
<p>当前用户：${loginUser.name!"未登录"}</p>
```

对于列表数据，建议后端提前传入 `empty` 变量，模板只做简单判断。

```java
model.addAttribute("userList", userList);
model.addAttribute("empty", CollUtil.isEmpty(userList));
```

模板中使用：

```html
<% if (empty) { %>
    <div class="empty">暂无用户数据</div>
<% } %>
```

## 打包与部署

本节用于说明 Spring Boot 3 Beetl 项目的生产配置、Jar 包运行方式和模板缓存策略。部署时应重点关注 Profile、日志级别、模板缓存、静态资源访问和可执行 Jar 的启动参数。

### 生产环境配置

生产环境配置应关闭调试日志，启用合理的模板缓存策略，统一设置端口、上下文路径和日志输出。不要在生产环境暴露详细异常堆栈到页面中。

文件位置：`src/main/resources/application-prod.yml`

```yaml
spring:
  config:
    activate:
      on-profile: prod

server:
  port: 8080
  servlet:
    # 如果不需要上下文路径，可删除该配置
    context-path: /

logging:
  level:
    # 生产环境业务日志建议使用 info
    io.github.atengk: info

    # 生产环境不建议打开模板引擎 debug 日志
    org.beetl: warn
    org.springframework.web: warn

  file:
    # 生产日志输出位置
    name: logs/springboot3-beetl-demo.log
```

文件位置：`src/main/resources/beetl.properties`

```properties
# 生产环境建议使用 UTF-8
TEMPLATE_CHARSET=UTF-8

# 生产环境建议关闭模板自动检查，减少每次渲染时的文件检测开销
RESOURCE.autoCheck=false

# 保持模板语法边界清晰
DELIMITER_PLACEHOLDER_START=${
DELIMITER_PLACEHOLDER_END=}
DELIMITER_STATEMENT_START=<%
DELIMITER_STATEMENT_END=%>
```

生产环境建议遵循以下规则：

| 配置项   | 建议                                   |
| -------- | -------------------------------------- |
| Profile  | 使用 `prod`                            |
| 日志级别 | 业务日志 `info`，框架日志 `warn`       |
| 模板检查 | `RESOURCE.autoCheck=false`             |
| 模板编码 | `UTF-8`                                |
| 静态资源 | 使用 Spring Boot 静态目录或 Nginx 代理 |
| 异常页面 | 返回友好提示，不暴露堆栈               |

### Jar 包运行

Spring Boot 项目通常通过 Maven 打包成可执行 Jar，然后在服务器上使用 `java -jar` 运行。打包前应先执行测试或至少执行编译，确认依赖和模板资源都能正常进入 Jar 包。

打包命令如下：

```bash
# 清理并打包项目，生成可执行 Jar
mvn clean package -DskipTests
```

打包完成后，Jar 文件通常位于 `target` 目录。

```bash
# 查看打包产物
ls -lh target/*.jar
```

启动生产环境：

```bash
# 使用 prod Profile 启动 Jar
java -jar target/springboot3-beetl-demo-1.0.0.jar --spring.profiles.active=prod
```

后台运行示例：

```bash
# 后台启动，并将控制台日志输出到 app.log
nohup java -jar target/springboot3-beetl-demo-1.0.0.jar \
  --spring.profiles.active=prod \
  > app.log 2>&1 &
```

查看运行状态：

```bash
# 查看 Java 进程
ps -ef | grep springboot3-beetl-demo | grep -v grep

# 查看启动日志
tail -f app.log

# 检查页面是否可访问
curl -I http://localhost:8080/user/list
```

如果需要指定 JVM 参数，可以使用以下方式：

```bash
java -Xms512m -Xmx512m \
  -Dfile.encoding=UTF-8 \
  -jar target/springboot3-beetl-demo-1.0.0.jar \
  --spring.profiles.active=prod
```

其中 `-Xms` 表示 JVM 初始堆内存，`-Xmx` 表示最大堆内存，`-Dfile.encoding=UTF-8` 用于保证 JVM 运行时编码一致。

### 模板缓存策略

模板缓存策略直接影响页面渲染性能和模板变更生效方式。开发环境关注修改后快速生效，生产环境关注稳定性和性能，因此两类环境的配置应分开处理。

推荐策略如下：

| 环境     | `RESOURCE.autoCheck` | 说明                             |
| -------- | -------------------- | -------------------------------- |
| 开发环境 | `true`               | 模板修改后自动检查并生效         |
| 测试环境 | `true` 或 `false`    | 联调阶段可开启，压测阶段建议关闭 |
| 生产环境 | `false`              | 避免运行时频繁检查模板文件       |

开发环境配置：

```properties
# 开发环境：模板修改后自动检查
RESOURCE.autoCheck=true
```

生产环境配置：

```properties
# 生产环境：关闭模板自动检查，提升渲染稳定性
RESOURCE.autoCheck=false
```

需要注意的是，打成可执行 Jar 后，模板文件已经被打入 Jar 包内部。生产环境即使开启自动检查，也不应依赖直接修改服务器源码目录模板来更新页面。正确发布流程应是修改源码、重新构建、重新部署 Jar。

推荐发布流程如下：

```bash
# 1. 本地或构建机打包
mvn clean package -DskipTests

# 2. 上传 Jar 到服务器
scp target/springboot3-beetl-demo-1.0.0.jar user@server:/opt/apps/beetl-demo/

# 3. 登录服务器
ssh user@server

# 4. 启动或重启服务
cd /opt/apps/beetl-demo
nohup java -jar springboot3-beetl-demo-1.0.0.jar \
  --spring.profiles.active=prod \
  > app.log 2>&1 &
```

如果页面更新后仍然显示旧内容，按以下顺序排查：

| 排查项           | 说明                                                   |
| ---------------- | ------------------------------------------------------ |
| Jar 是否更新     | 检查服务器上的 Jar 文件时间                            |
| Profile 是否正确 | 确认启动参数中是否包含 `--spring.profiles.active=prod` |
| 浏览器缓存       | 强制刷新或清理浏览器缓存                               |
| 反向代理缓存     | 如果使用 Nginx/CDN，检查静态资源缓存                   |
| 模板路径         | 确认修改的是实际被打包的 `templates` 文件              |
| 服务是否重启     | 确认新进程已启动，旧进程已停止                         |

生产环境更推荐通过完整发布替换模板，而不是让服务器上的模板热更新。这样可以保证模板、Java 代码、静态资源和配置文件处于同一个版本，避免出现页面与后端逻辑不一致的问题。