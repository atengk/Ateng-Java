# FreeMarker

## 技术概述

本节用于说明 FreeMarker 的基本定位、在 Spring Boot 3 中的集成方式，以及它在实际后端项目中的典型使用场景。FreeMarker 属于服务端模板引擎，适合将后端数据渲染为 HTML、邮件内容、配置文件、代码文件或其他文本内容。

### FreeMarker 简介

FreeMarker 是一款基于 Java 的模板引擎，主要用于根据模板文件和动态数据生成文本内容。它本身不依赖 Servlet 容器，因此既可以用于 Web 页面渲染，也可以用于邮件模板、代码生成、静态页面生成等非 Web 场景。

在 Spring Boot Web 项目中，FreeMarker 通常用于服务端页面渲染。后端 Controller 将业务数据放入 `Model`，然后返回模板名称，Spring MVC 会调用 FreeMarker 将 `.ftl` 模板渲染为最终的 HTML 页面。

FreeMarker 模板文件通常以 `.ftl` 作为后缀。模板中可以编写变量输出、条件判断、列表遍历、模板引入、宏定义等逻辑。它的核心思想是将页面结构与后端业务代码分离，让 Java 代码专注于数据处理，让模板文件专注于页面展示。

文件位置：`src/main/resources/templates/user/list.ftl`

下面是一个简单的 FreeMarker 用户列表模板，用于展示后端传入的用户集合数据。

```ftl
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>用户列表</title>
</head>
<body>
<h1>用户列表</h1>

<#-- 判断用户列表是否存在且有数据 -->
<#if userList?? && userList?size gt 0>
    <ul>
        <#list userList as user>
            <li>${user.username} - ${user.email}</li>
        </#list>
    </ul>
<#else>
    <p>暂无用户数据</p>
</#if>
</body>
</html>
```

在该示例中，`${user.username}` 用于输出变量，`<#if>` 用于条件判断，`<#list>` 用于集合遍历。后端只需要向模板传入 `userList` 数据，页面即可完成渲染。

### Spring Boot 3 集成方式

Spring Boot 3 对 FreeMarker 提供了自动配置支持。项目引入 `spring-boot-starter-freemarker` 依赖后，Spring Boot 会自动创建 FreeMarker 相关配置对象，并默认从 `classpath:/templates/` 目录加载 `.ftl` 模板文件。

在 Spring Boot 3 中，FreeMarker 通常与 Spring MVC 配合使用。Controller 方法返回字符串时，该字符串会被视为模板名称，而不是接口响应内容。例如返回 `user/list`，Spring Boot 会查找 `src/main/resources/templates/user/list.ftl` 文件并执行渲染。

基础调用流程如下：

1. 浏览器请求后端页面地址，例如 `/users`
2. Controller 查询或构造页面所需数据
3. Controller 将数据写入 `Model`
4. Controller 返回模板路径，例如 `user/list`
5. Spring Boot 查找 `templates/user/list.ftl`
6. FreeMarker 将模板和数据合并，生成 HTML 响应

文件位置：`src/main/java/io/github/atengk/freemarker/controller/UserPageController.java`

下面的 Controller 用于向 FreeMarker 模板传递用户列表数据，并返回用户列表页面。

```java
package io.github.atengk.freemarker.controller;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.lang.Dict;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

/**
 * 用户页面控制器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Controller
public class UserPageController {

    /**
     * 渲染用户列表页面
     *
     * @param model 页面模型对象
     * @return 用户列表模板路径
     */
    @GetMapping("/users")
    public String list(Model model) {
        List<Dict> userList = ListUtil.of(
                Dict.create().set("username", "admin").set("email", "admin@example.com"),
                Dict.create().set("username", "ateng").set("email", "ateng@example.com")
        );

        model.addAttribute("userList", userList);
        log.info("渲染用户列表页面，用户数量：{}", userList.size());
        return "user/list";
    }
}
```

需要注意的是，这里使用的是 `@Controller`，不是 `@RestController`。如果使用 `@RestController`，返回的字符串会直接作为响应体输出，不会进入模板渲染流程。

### 典型使用场景

FreeMarker 适用于需要在服务端生成文本内容的场景，尤其适合页面结构相对稳定、数据由后端统一渲染的系统。

在 Spring Boot 项目中，FreeMarker 常见使用场景如下：

| 场景             | 说明                                             |
| ---------------- | ------------------------------------------------ |
| 后台管理页面     | 适合中小型管理系统、内部运营平台、低频页面功能   |
| 服务端 HTML 页面 | 后端直接渲染 HTML，浏览器无需再发起额外接口请求  |
| 邮件模板         | 根据用户数据动态生成注册通知、告警邮件、审批邮件 |
| 导出模板         | 生成 HTML、XML、Markdown、配置文件等文本内容     |
| 静态页面生成     | 将动态数据渲染为静态 HTML 文件，提高访问性能     |
| 代码生成         | 根据元数据生成 Java 类、SQL、Mapper、配置文件等  |

对于前后端分离项目，FreeMarker 通常不是主页面技术，但仍然可以用于邮件模板、通知模板、打印页面、协议页面、静态资源页等后端渲染场景。

## 环境准备

本节用于说明开发 Spring Boot 3 + FreeMarker 项目前需要准备的基础环境、Maven 依赖以及推荐的模板目录结构。环境准备完成后，后续页面开发、数据渲染和静态资源处理都基于该项目结构展开。

### 项目基础环境

Spring Boot 3 项目建议使用 JDK 17 或更高版本。因为 Spring Boot 3 基于 Jakarta EE 规范，最低运行环境要求已经提升到 Java 17。

推荐开发环境如下：

| 环境项           | 推荐版本或说明                  |
| ---------------- | ------------------------------- |
| JDK              | JDK 17+                         |
| Spring Boot      | 3.x                             |
| 构建工具         | Maven 3.8+                      |
| 模板引擎         | FreeMarker                      |
| Web 组件         | Spring MVC                      |
| 编码格式         | UTF-8                           |
| 模板后缀         | `.ftl`                          |
| 默认模板目录     | `src/main/resources/templates/` |
| 默认静态资源目录 | `src/main/resources/static/`    |

可以使用以下命令检查本地 Java 和 Maven 环境。

```bash
java -version
mvn -version
```

命令说明：

`java -version` 用于检查当前终端使用的 JDK 版本，建议确认输出版本不低于 17。

`mvn -version` 用于检查 Maven 版本以及 Maven 当前绑定的 Java 版本。如果 Maven 显示的 Java 版本低于 17，需要调整 `JAVA_HOME` 环境变量。

推荐项目基础包路径如下：

```text
io.github.atengk.freemarker
```

该包路径后续可以继续拆分为 `controller`、`service`、`model`、`config` 等子包，便于保持 Spring Boot 项目结构清晰。

### Maven 依赖配置

本节用于配置 Spring Boot 3 集成 FreeMarker 所需的 Maven 依赖。核心依赖是 `spring-boot-starter-web` 和 `spring-boot-starter-freemarker`，前者提供 Web MVC 能力，后者提供 FreeMarker 自动配置和模板渲染能力。

文件位置：`pom.xml`

下面是项目所需的核心依赖配置。

```xml
<dependencies>
    <!-- Spring Boot Web：提供 Spring MVC、内置 Tomcat、JSON 处理等 Web 基础能力 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Spring Boot FreeMarker：提供 FreeMarker 模板引擎集成与自动配置 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-freemarker</artifactId>
    </dependency>

    <!-- Lombok：简化日志对象、Getter、Setter、构造方法等样板代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- Hutool：提供集合、字符串、日期、文件等常用工具类 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.8.36</version>
    </dependency>

    <!-- Spring Boot Test：提供单元测试与集成测试支持 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

如果项目使用 Spring Boot Maven 插件进行打包，可以添加以下配置。

文件位置：`pom.xml`

```xml
<build>
    <plugins>
        <!-- Spring Boot Maven 插件：用于打包可执行 Jar -->
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
        </plugin>
    </plugins>
</build>
```

配置完成后，可以执行以下命令下载依赖并编译项目。

```bash
mvn clean compile
```

命令说明：

`mvn clean compile` 会先清理 `target` 目录，然后重新下载依赖并编译项目源码。若依赖配置正确，该命令应能正常结束，不应出现依赖无法解析、JDK 版本不兼容或编译失败等错误。

### 模板目录结构

本节用于约定 FreeMarker 模板文件、静态资源文件和后端代码的存放位置。统一目录结构可以减少模板路径错误，也便于后续进行公共模板抽取、页面复用和静态资源管理。

推荐项目结构如下：

```text
springboot3-freemarker-demo
├── pom.xml
└── src
    └── main
        ├── java
        │   └── io
        │       └── github
        │           └── atengk
        │               └── freemarker
        │                   ├── FreeMarkerApplication.java
        │                   └── controller
        │                       └── UserPageController.java
        └── resources
            ├── application.yml
            ├── static
            │   ├── css
            │   │   └── app.css
            │   ├── js
            │   │   └── app.js
            │   └── images
            │       └── logo.png
            └── templates
                ├── index.ftl
                ├── common
                │   ├── header.ftl
                │   └── footer.ftl
                └── user
                    └── list.ftl
```

目录说明如下：

| 路径                                  | 说明                               |
| ------------------------------------- | ---------------------------------- |
| `src/main/java`                       | Java 源码目录                      |
| `src/main/resources/application.yml`  | Spring Boot 配置文件               |
| `src/main/resources/templates`        | FreeMarker 默认模板目录            |
| `src/main/resources/templates/common` | 公共模板目录，例如头部、底部、菜单 |
| `src/main/resources/templates/user`   | 用户模块页面模板目录               |
| `src/main/resources/static`           | 静态资源目录                       |
| `src/main/resources/static/css`       | CSS 文件目录                       |
| `src/main/resources/static/js`        | JavaScript 文件目录                |
| `src/main/resources/static/images`    | 图片资源目录                       |

模板路径和 Controller 返回值需要保持对应关系。例如 Controller 返回：

```java
return "user/list";
```

Spring Boot 会默认查找以下模板文件：

```text
src/main/resources/templates/user/list.ftl
```

静态资源可以直接通过根路径访问。例如文件：

```text
src/main/resources/static/css/app.css
```

在 FreeMarker 页面中可以这样引入：

```ftl
<link rel="stylesheet" href="/css/app.css">
```

建议按照业务模块拆分模板目录，例如 `user`、`order`、`product`、`system` 等。公共页面片段统一放在 `common` 目录中，后续可以通过 `<#include>` 或宏定义进行复用。



## 基础配置

本节用于配置 Spring Boot 3 中 FreeMarker 的模板加载路径、模板后缀、字符编码、缓存策略以及开发环境和生产环境的差异化配置。基础配置完成后，后续 Controller 返回模板名称时，Spring Boot 才能正确定位并渲染 `.ftl` 文件。

### application.yml 配置

Spring Boot 3 集成 FreeMarker 后，可以通过 `application.yml` 统一配置模板路径、文件后缀、编码、缓存和请求上下文对象。开发环境通常关闭模板缓存，便于修改模板后立即刷新页面查看效果；生产环境建议开启缓存，提高模板解析性能。

文件位置：`src/main/resources/application.yml`

下面是开发环境下常用的 FreeMarker 配置。

```yaml
server:
  port: 8080

spring:
  application:
    name: springboot3-freemarker-demo

  freemarker:
    # 是否启用 FreeMarker 模板引擎
    enabled: true

    # 模板文件加载路径，默认从 resources/templates 目录加载
    template-loader-path: classpath:/templates/

    # 模板文件后缀，Controller 返回 user/list 时会匹配 user/list.ftl
    suffix: .ftl

    # 模板编码，建议统一使用 UTF-8
    charset: UTF-8

    # 响应内容类型
    content-type: text/html

    # 是否检查模板路径是否存在
    check-template-location: true

    # 开发环境建议关闭缓存，修改模板后刷新页面即可生效
    cache: false

    # 是否允许模板访问 request 属性
    expose-request-attributes: true

    # 是否允许模板访问 session 属性
    expose-session-attributes: true

    # 是否暴露 Spring 宏辅助对象
    expose-spring-macro-helpers: true

    # 在模板中使用 ${request.contextPath} 获取上下文路径
    request-context-attribute: request

    settings:
      # 数字默认格式，避免数字被格式化为 1,000 这类形式
      number_format: 0.##
      # 日期时间默认格式
      datetime_format: yyyy-MM-dd HH:mm:ss
      # 日期默认格式
      date_format: yyyy-MM-dd
      # 时间默认格式
      time_format: HH:mm:ss
```

如果需要区分开发环境和生产环境，可以使用多环境配置。

文件位置：`src/main/resources/application-dev.yml`

```yaml
spring:
  freemarker:
    # 开发环境关闭缓存，便于调试页面
    cache: false
```

文件位置：`src/main/resources/application-prod.yml`

```yaml
spring:
  freemarker:
    # 生产环境开启缓存，减少模板重复解析开销
    cache: true
```

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  profiles:
    # 默认启用开发环境
    active: dev
```

配置完成后，启动项目并访问页面接口。如果 Controller 返回的模板名称与 `templates` 目录下的 `.ftl` 文件路径一致，页面即可正常渲染。

### 模板加载路径配置

模板加载路径用于指定 FreeMarker 从哪里查找 `.ftl` 文件。Spring Boot 默认模板路径是 `classpath:/templates/`，对应项目中的 `src/main/resources/templates/` 目录。

推荐目录结构如下：

```text
src
└── main
    └── resources
        ├── application.yml
        ├── static
        │   ├── css
        │   ├── js
        │   └── images
        └── templates
            ├── index.ftl
            ├── common
            │   ├── header.ftl
            │   └── footer.ftl
            └── user
                ├── list.ftl
                ├── add.ftl
                ├── edit.ftl
                └── detail.ftl
```

当配置如下时：

```yaml
spring:
  freemarker:
    template-loader-path: classpath:/templates/
    suffix: .ftl
```

Controller 返回值与模板文件的对应关系如下：

| Controller 返回值 | 实际模板文件                                     |
| ----------------- | ------------------------------------------------ |
| `index`           | `src/main/resources/templates/index.ftl`         |
| `user/list`       | `src/main/resources/templates/user/list.ftl`     |
| `user/add`        | `src/main/resources/templates/user/add.ftl`      |
| `common/header`   | `src/main/resources/templates/common/header.ftl` |

例如下面的 Controller 方法返回 `user/list`，Spring Boot 会加载 `templates/user/list.ftl`。

文件位置：`src/main/java/io/github/atengk/freemarker/controller/UserPageController.java`

下面的代码用于演示 Controller 返回模板路径与实际 `.ftl` 文件之间的对应关系。

```java
package io.github.atengk.freemarker.controller;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.lang.Dict;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

/**
 * 用户页面控制器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Controller
public class UserPageController {

    /**
     * 用户列表页面
     *
     * @param model 页面模型对象
     * @return 用户列表模板路径
     */
    @GetMapping("/users")
    public String list(Model model) {
        List<Dict> userList = ListUtil.of(
                Dict.create().set("id", 1).set("username", "admin").set("email", "admin@example.com"),
                Dict.create().set("id", 2).set("username", "ateng").set("email", "ateng@example.com")
        );

        model.addAttribute("userList", userList);
        log.info("访问用户列表页面，用户数量：{}", userList.size());
        return "user/list";
    }

    /**
     * 用户新增页面
     *
     * @return 用户新增模板路径
     */
    @GetMapping("/users/add")
    public String add() {
        log.info("访问用户新增页面");
        return "user/add";
    }
}
```

如果需要自定义模板目录，例如将模板放到 `resources/views` 目录下，可以修改配置。

```yaml
spring:
  freemarker:
    # 自定义模板目录
    template-loader-path: classpath:/views/
    suffix: .ftl
```

对应目录应调整为：

```text
src/main/resources/views/user/list.ftl
```

一般建议使用默认的 `templates` 目录，除非项目已有统一的资源目录规范。

### 字符编码与缓存配置

字符编码和缓存配置会直接影响模板渲染效果。编码配置不正确时，页面可能出现中文乱码；缓存配置不合理时，开发阶段可能出现修改模板后页面不生效的问题。

推荐统一使用 UTF-8 编码。

```yaml
spring:
  freemarker:
    # 模板文件编码
    charset: UTF-8

    # HTTP 响应内容类型
    content-type: text/html
```

同时，模板文件自身也建议显式声明 UTF-8。

文件位置：`src/main/resources/templates/index.ftl`

```ftl
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>FreeMarker 首页</title>
</head>
<body>
<h1>${title!'FreeMarker 示例项目'}</h1>
</body>
</html>
```

缓存配置建议按环境区分。

开发环境配置如下：

```yaml
spring:
  freemarker:
    # 开发环境关闭缓存，便于调试模板
    cache: false
```

生产环境配置如下：

```yaml
spring:
  freemarker:
    # 生产环境开启缓存，提高模板解析性能
    cache: true
```

开发阶段如果发现模板修改后页面没有变化，优先检查以下几项：

| 检查项                    | 说明                                          |
| ------------------------- | --------------------------------------------- |
| `spring.freemarker.cache` | 开发环境应设置为 `false`                      |
| 浏览器缓存                | 可以强制刷新页面或清理浏览器缓存              |
| 模板路径                  | 确认 Controller 返回值与模板文件路径一致      |
| 文件后缀                  | 确认模板文件后缀为 `.ftl`                     |
| 编译目录                  | 确认模板文件已进入 `target/classes/templates` |

可以通过以下命令重新编译项目，确认资源文件已被复制到编译目录。

```bash
mvn clean compile
```

命令执行后，可以检查模板文件是否存在于以下目录：

```text
target/classes/templates/
```

## 页面模板开发

本节用于说明 FreeMarker 页面模板的基本开发方式，包括 FTL 语法、变量输出、条件判断、列表遍历和模板引入。掌握这些语法后，就可以完成大部分服务端页面渲染需求。

### FTL 模板语法

FTL 是 FreeMarker Template Language 的缩写，即 FreeMarker 模板语言。它通过普通 HTML 结构配合 FreeMarker 指令完成动态页面渲染。

FreeMarker 模板通常由以下几类内容组成：

| 类型       | 示例                             | 说明                             |
| ---------- | -------------------------------- | -------------------------------- |
| 普通 HTML  | `<h1>用户列表</h1>`              | 静态页面结构                     |
| 变量输出   | `${username}`                    | 输出后端传入的数据               |
| 默认值处理 | `${username!'匿名用户'}`         | 变量为空时使用默认值             |
| 空值判断   | `<#if user??>`                   | 判断变量是否存在                 |
| 条件判断   | `<#if enabled>...</#if>`         | 根据条件控制页面内容             |
| 列表遍历   | `<#list userList as user>`       | 遍历集合数据                     |
| 模板注释   | `<#-- 注释内容 -->`              | FreeMarker 注释，不会输出到 HTML |
| 模板引入   | `<#include "common/header.ftl">` | 引入公共模板片段                 |

一个完整页面模板示例如下。

文件位置：`src/main/resources/templates/index.ftl`

下面的模板用于展示首页标题、登录用户和当前时间。

```ftl
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>${title!'首页'}</title>
</head>
<body>
<#-- 页面标题 -->
<h1>${title!'FreeMarker 示例项目'}</h1>

<#-- 当前登录用户 -->
<p>当前用户：${loginUser.username!'未登录'}</p>

<#-- 当前时间 -->
<p>当前时间：${now?string('yyyy-MM-dd HH:mm:ss')}</p>
</body>
</html>
```

对应的 Controller 可以向模板传入 `title`、`loginUser` 和 `now` 数据。

文件位置：`src/main/java/io/github/atengk/freemarker/controller/IndexController.java`

下面的代码用于向首页模板传递基础展示数据。

```java
package io.github.atengk.freemarker.controller;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Dict;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
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
     * 首页
     *
     * @param model 页面模型对象
     * @return 首页模板路径
     */
    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("title", "FreeMarker 示例项目");
        model.addAttribute("loginUser", Dict.create().set("username", "Ateng"));
        model.addAttribute("now", DateUtil.date());

        log.info("访问 FreeMarker 首页");
        return "index";
    }
}
```

### 变量输出

变量输出用于将后端传入 `Model` 的数据展示到页面中。FreeMarker 使用 `${变量名}` 输出变量内容，支持普通字符串、数字、日期、对象属性、Map 属性以及集合元素。

常见变量输出方式如下：

```ftl
<#-- 输出普通字符串 -->
<p>标题：${title}</p>

<#-- 输出对象属性 -->
<p>用户名：${user.username}</p>

<#-- 输出 Map 属性 -->
<p>邮箱：${user.email}</p>

<#-- 输出默认值，变量为空或不存在时显示匿名用户 -->
<p>昵称：${user.nickname!'匿名用户'}</p>

<#-- 判断变量是否存在 -->
<#if user??>
    <p>用户信息已加载</p>
</#if>

<#-- 日期格式化 -->
<p>创建时间：${user.createTime?string('yyyy-MM-dd HH:mm:ss')}</p>

<#-- HTML 转义，避免直接输出用户输入内容造成 XSS 风险 -->
<p>个人简介：${user.description!?html}</p>
```

推荐在页面输出用户输入内容时使用 `?html` 进行 HTML 转义。例如用户昵称、备注、简介、评论等字段，都不建议直接输出原始内容。

文件位置：`src/main/resources/templates/user/detail.ftl`

下面的模板用于展示用户详情，并对可能为空的字段设置默认值。

```ftl
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>用户详情</title>
</head>
<body>
<h1>用户详情</h1>

<p>用户ID：${user.id}</p>
<p>用户名：${user.username!'未设置'}</p>
<p>邮箱：${user.email!'未设置'}</p>
<p>昵称：${user.nickname!'匿名用户'}</p>
<p>状态：${user.status!'未知'}</p>
<p>创建时间：${user.createTime?string('yyyy-MM-dd HH:mm:ss')}</p>
<p>个人简介：${user.description!?html}</p>

<a href="/users">返回用户列表</a>
</body>
</html>
```

在实际开发中，推荐对可能为空的字段使用 `!` 设置默认值，避免模板渲染时因为空值处理不当导致异常。

### 条件判断

条件判断用于根据后端数据动态控制页面展示内容。FreeMarker 使用 `<#if>`、`<#elseif>`、`<#else>` 完成条件分支。

常见条件判断示例如下：

```ftl
<#-- 判断变量是否存在 -->
<#if user??>
    <p>当前用户：${user.username}</p>
<#else>
    <p>当前未登录</p>
</#if>

<#-- 判断字符串是否有内容 -->
<#if keyword?? && keyword?length gt 0>
    <p>搜索关键字：${keyword}</p>
<#else>
    <p>请输入搜索关键字</p>
</#if>

<#-- 判断数字状态 -->
<#if user.status == 1>
    <span>正常</span>
<#elseif user.status == 0>
    <span>禁用</span>
<#else>
    <span>未知</span>
</#if>

<#-- 判断布尔值 -->
<#if admin>
    <button>删除用户</button>
</#if>
```

文件位置：`src/main/resources/templates/user/detail.ftl`

下面的模板片段用于根据用户状态显示不同的页面内容。

```ftl
<h2>账号状态</h2>

<#if user.status == 1>
    <p style="color: green;">账号状态：正常</p>
<#elseif user.status == 0>
    <p style="color: red;">账号状态：禁用</p>
<#else>
    <p style="color: gray;">账号状态：未知</p>
</#if>

<#if admin?? && admin>
    <div>
        <a href="/users/${user.id}/edit">编辑用户</a>
        <button type="button">删除用户</button>
    </div>
<#else>
    <p>当前用户无管理权限</p>
</#if>
```

条件判断中常用操作符如下：

| 操作符 | 说明             |
| ------ | ---------------- |
| `??`   | 判断变量是否存在 |
| `!`    | 设置默认值       |
| `==`   | 等于             |
| `!=`   | 不等于           |
| `gt`   | 大于             |
| `gte`  | 大于等于         |
| `lt`   | 小于             |
| `lte`  | 小于等于         |
| `&&`   | 并且             |
| `      |                  |

在模板中处理数字比较时，建议使用 `gt`、`gte`、`lt`、`lte`，可读性更清晰，也能避免部分符号在 HTML 中产生歧义。

### 列表遍历

列表遍历用于渲染集合数据，例如用户列表、订单列表、菜单列表、字典列表等。FreeMarker 使用 `<#list 集合 as 元素>` 遍历集合。

基础语法如下：

```ftl
<#list userList as user>
    <p>${user.username}</p>
</#list>
```

实际页面中通常需要处理空集合情况，可以使用 `<#if>` 先判断集合是否存在且有数据。

文件位置：`src/main/resources/templates/user/list.ftl`

下面的模板用于渲染用户列表表格，并处理列表为空的情况。

```ftl
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>用户列表</title>
</head>
<body>
<h1>用户列表</h1>

<a href="/users/add">新增用户</a>

<#if userList?? && userList?size gt 0>
    <table border="1" cellpadding="8" cellspacing="0">
        <thead>
        <tr>
            <th>序号</th>
            <th>用户ID</th>
            <th>用户名</th>
            <th>邮箱</th>
            <th>状态</th>
            <th>操作</th>
        </tr>
        </thead>
        <tbody>
        <#list userList as user>
            <tr>
                <td>${user_index + 1}</td>
                <td>${user.id}</td>
                <td>${user.username!'未设置'}</td>
                <td>${user.email!'未设置'}</td>
                <td>
                    <#if user.status == 1>
                        正常
                    <#elseif user.status == 0>
                        禁用
                    <#else>
                        未知
                    </#if>
                </td>
                <td>
                    <a href="/users/${user.id}">详情</a>
                    <a href="/users/${user.id}/edit">编辑</a>
                </td>
            </tr>
        </#list>
        </tbody>
    </table>
<#else>
    <p>暂无用户数据</p>
</#if>
</body>
</html>
```

在 `<#list>` 中，FreeMarker 会自动提供一些循环相关变量。以上示例中的 `user_index` 表示当前元素下标，从 `0` 开始，因此页面显示序号时通常使用 `${user_index + 1}`。

常用循环变量如下：

| 变量            | 说明                                           |
| --------------- | ---------------------------------------------- |
| `item_index`    | 当前元素下标，从 `0` 开始，`item` 为循环变量名 |
| `item_has_next` | 当前元素后面是否还有元素                       |
| `item_parity`   | 奇偶标记，常用于表格行样式                     |
| `item_counter`  | 当前循环计数，从 `1` 开始                      |

例如循环变量名为 `user`，则对应循环变量为 `user_index`、`user_has_next`、`user_parity`、`user_counter`。

### 模板引入

模板引入用于复用公共页面片段，例如页面头部、底部、导航栏、侧边栏、分页区域等。FreeMarker 常用 `<#include>` 引入公共模板。

推荐公共模板目录如下：

```text
src/main/resources/templates
├── common
│   ├── header.ftl
│   ├── footer.ftl
│   └── nav.ftl
└── user
    └── list.ftl
```

文件位置：`src/main/resources/templates/common/header.ftl`

下面的模板用于定义公共头部区域。

```ftl
<header>
    <h1>${systemName!'后台管理系统'}</h1>
    <nav>
        <a href="/">首页</a>
        <a href="/users">用户管理</a>
    </nav>
    <hr>
</header>
```

文件位置：`src/main/resources/templates/common/footer.ftl`

下面的模板用于定义公共底部区域。

```ftl
<hr>
<footer>
    <p>Copyright © ${year!'2026'} ${systemName!'后台管理系统'}</p>
</footer>
```

文件位置：`src/main/resources/templates/user/list.ftl`

下面的页面通过 `<#include>` 引入公共头部和底部模板。

```ftl
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>用户列表</title>
</head>
<body>
<#include "../common/header.ftl">

<h2>用户列表</h2>

<#if userList?? && userList?size gt 0>
    <ul>
        <#list userList as user>
            <li>
                ${user.username!'未设置'} -
                ${user.email!'未设置'}
            </li>
        </#list>
    </ul>
<#else>
    <p>暂无用户数据</p>
</#if>

<#include "../common/footer.ftl">
</body>
</html>
```

如果当前模板位于 `templates/user/list.ftl`，而公共模板位于 `templates/common/header.ftl`，可以使用相对路径 `../common/header.ftl` 引入。

也可以使用绝对模板路径写法。

```ftl
<#include "/common/header.ftl">
<#include "/common/footer.ftl">
```

在项目中更推荐使用绝对路径写法，因为它不依赖当前模板所在目录，后续移动页面模板位置时不容易出现路径错误。

公共模板中使用的变量，例如 `systemName`、`year`，需要由 Controller 或全局配置提前传入。简单示例如下：

```java
model.addAttribute("systemName", "FreeMarker 示例系统");
model.addAttribute("year", "2026");
```

模板引入适合复用简单页面片段。如果项目中需要更复杂的布局复用，例如统一页面骨架、页面内容插槽、组件化布局，可以进一步使用 FreeMarker 宏定义和布局模板。



## 后端数据渲染

本节用于说明 Spring Boot 3 如何将后端数据传递给 FreeMarker 模板，并在页面中完成对象、集合、日期、状态值等内容的渲染。FreeMarker 的核心流程是 Controller 准备数据，Model 承载数据，模板文件负责展示数据。

### Controller 返回页面

在 Spring Boot 3 中，返回 FreeMarker 页面时需要使用 `@Controller` 注解。Controller 方法返回的字符串表示模板路径，而不是响应文本。例如返回 `user/list` 时，Spring Boot 会查找 `src/main/resources/templates/user/list.ftl` 模板文件。

需要注意，返回页面时不能使用 `@RestController`。`@RestController` 等价于 `@Controller` + `@ResponseBody`，返回值会直接写入 HTTP 响应体，不会进入模板解析流程。

文件位置：`src/main/java/io/github/atengk/freemarker/controller/UserPageController.java`

下面的 Controller 用于演示列表页面、详情页面和新增页面的返回方式。

```java
package io.github.atengk.freemarker.controller;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.NumberUtil;
import io.github.atengk.freemarker.vo.UserVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户页面控制器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Controller
public class UserPageController {

    /**
     * 用户列表页面
     *
     * @param model 页面模型对象
     * @return 用户列表模板路径
     */
    @GetMapping("/users")
    public String list(Model model) {
        List<UserVO> userList = ListUtil.of(
                new UserVO(1L, "admin", "admin@example.com", "系统管理员", 1, LocalDateTime.now()),
                new UserVO(2L, "ateng", "ateng@example.com", "普通用户", 1, LocalDateTime.now()),
                new UserVO(3L, "test", "test@example.com", null, 0, LocalDateTime.now())
        );

        model.addAttribute("pageTitle", "用户列表");
        model.addAttribute("userList", userList);
        model.addAttribute("total", userList.size());

        log.info("访问用户列表页面，用户数量：{}", userList.size());
        return "user/list";
    }

    /**
     * 用户详情页面
     *
     * @param id 用户ID
     * @param model 页面模型对象
     * @return 用户详情模板路径
     */
    @GetMapping("/users/{id}")
    public String detail(@PathVariable Long id, Model model) {
        if (!NumberUtil.isLong(String.valueOf(id))) {
            log.warn("用户ID格式异常，id：{}", id);
            return "error/400";
        }

        UserVO user = new UserVO(
                id,
                "ateng",
                "ateng@example.com",
                "普通用户",
                1,
                LocalDateTime.now()
        );

        model.addAttribute("pageTitle", "用户详情");
        model.addAttribute("user", user);

        log.info("访问用户详情页面，用户ID：{}", id);
        return "user/detail";
    }

    /**
     * 用户新增页面
     *
     * @param model 页面模型对象
     * @return 用户新增模板路径
     */
    @GetMapping("/users/add")
    public String add(Model model) {
        model.addAttribute("pageTitle", "新增用户");
        model.addAttribute("user", Dict.create());

        log.info("访问用户新增页面");
        return "user/add";
    }
}
```

文件位置：`src/main/java/io/github/atengk/freemarker/vo/UserVO.java`

下面的 VO 类用于承载页面展示所需的用户数据。

```java
package io.github.atengk.freemarker.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户展示对象
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserVO {

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 状态：1正常，0禁用
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
```

Controller 返回值与模板文件的对应关系如下：

| 请求路径      | Controller 返回值 | 模板文件                    |
| ------------- | ----------------- | --------------------------- |
| `/users`      | `user/list`       | `templates/user/list.ftl`   |
| `/users/{id}` | `user/detail`     | `templates/user/detail.ftl` |
| `/users/add`  | `user/add`        | `templates/user/add.ftl`    |

启动项目后，可以访问以下地址验证页面是否正常渲染：

```text
http://localhost:8080/users
http://localhost:8080/users/1
http://localhost:8080/users/add
```

### Model 数据传递

`Model` 是 Spring MVC 用于向页面模板传递数据的对象。Controller 将数据通过 `model.addAttribute()` 放入模型后，FreeMarker 模板可以通过变量名直接访问这些数据。

常见数据传递方式如下：

```java
model.addAttribute("pageTitle", "用户列表");
model.addAttribute("user", user);
model.addAttribute("userList", userList);
model.addAttribute("total", userList.size());
```

在 FreeMarker 模板中，可以直接使用 `${pageTitle}`、`${user.username}`、`${userList}`、`${total}` 获取这些数据。

文件位置：`src/main/resources/templates/user/detail.ftl`

下面的模板用于展示 Controller 传入的单个用户对象。

```ftl
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>${pageTitle!'用户详情'}</title>
</head>
<body>
<h1>${pageTitle!'用户详情'}</h1>

<#if user??>
    <p>用户ID：${user.id}</p>
    <p>用户名：${user.username!'未设置'}</p>
    <p>邮箱：${user.email!'未设置'}</p>
    <p>昵称：${user.nickname!'匿名用户'}</p>

    <p>
        状态：
        <#if user.status == 1>
            正常
        <#elseif user.status == 0>
            禁用
        <#else>
            未知
        </#if>
    </p>

    <p>创建时间：${user.createTime?string('yyyy-MM-dd HH:mm:ss')}</p>
<#else>
    <p>用户不存在</p>
</#if>

<a href="/users">返回用户列表</a>
</body>
</html>
```

`Model` 支持传递普通字符串、数字、布尔值、日期、Java Bean、Map、List 等类型。实际开发中，建议传递专门用于页面展示的 VO 对象，而不是直接将数据库实体对象暴露给模板。

常见传值类型如下：

| 后端数据类型       | 模板访问方式                            | 说明       |
| ------------------ | --------------------------------------- | ---------- |
| `String`           | `${pageTitle}`                          | 普通文本   |
| `Integer` / `Long` | `${total}`                              | 数字       |
| `Boolean`          | `<#if admin>`                           | 布尔判断   |
| `LocalDateTime`    | `${time?string('yyyy-MM-dd HH:mm:ss')}` | 日期格式化 |
| Java Bean          | `${user.username}`                      | 对象属性   |
| `Map` / `Dict`     | `${user.username}`                      | Map 属性   |
| `List`             | `<#list userList as user>`              | 集合遍历   |

如果页面中可能出现空值，建议使用默认值语法处理。

```ftl
<p>用户名：${user.username!'未设置'}</p>
<p>昵称：${user.nickname!'匿名用户'}</p>
<p>备注：${user.remark!'暂无备注'}</p>
```

这样可以避免页面字段为空时出现模板渲染异常，也可以提升页面展示的稳定性。

### 对象与集合渲染

对象渲染适合详情页、编辑页、表单回显等场景；集合渲染适合列表页、表格页、菜单页、下拉选项等场景。FreeMarker 通过点语法访问对象属性，通过 `<#list>` 遍历集合数据。

文件位置：`src/main/resources/templates/user/list.ftl`

下面的模板用于渲染用户集合，并展示序号、用户状态和操作按钮。

```ftl
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>${pageTitle!'用户列表'}</title>
</head>
<body>
<h1>${pageTitle!'用户列表'}</h1>

<p>用户总数：${total!0}</p>

<a href="/users/add">新增用户</a>

<#if userList?? && userList?size gt 0>
    <table border="1" cellpadding="8" cellspacing="0">
        <thead>
        <tr>
            <th>序号</th>
            <th>用户ID</th>
            <th>用户名</th>
            <th>邮箱</th>
            <th>昵称</th>
            <th>状态</th>
            <th>创建时间</th>
            <th>操作</th>
        </tr>
        </thead>
        <tbody>
        <#list userList as user>
            <tr>
                <td>${user_counter}</td>
                <td>${user.id}</td>
                <td>${user.username!'未设置'}</td>
                <td>${user.email!'未设置'}</td>
                <td>${user.nickname!'匿名用户'}</td>
                <td>
                    <#if user.status == 1>
                        正常
                    <#elseif user.status == 0>
                        禁用
                    <#else>
                        未知
                    </#if>
                </td>
                <td>${user.createTime?string('yyyy-MM-dd HH:mm:ss')}</td>
                <td>
                    <a href="/users/${user.id}">详情</a>
                    <a href="/users/${user.id}/edit">编辑</a>
                </td>
            </tr>
        </#list>
        </tbody>
    </table>
<#else>
    <p>暂无用户数据</p>
</#if>
</body>
</html>
```

在 `<#list userList as user>` 中，`user` 是当前循环元素，FreeMarker 会根据循环变量名自动生成循环辅助变量。

| 变量            | 说明                     |
| --------------- | ------------------------ |
| `user_index`    | 当前下标，从 `0` 开始    |
| `user_counter`  | 当前计数，从 `1` 开始    |
| `user_has_next` | 当前元素后面是否还有数据 |
| `user_parity`   | 当前行奇偶标识           |

如果需要渲染下拉选项，可以使用同样的集合遍历方式。

文件位置：`src/main/resources/templates/user/add.ftl`

下面的模板用于渲染新增用户表单，并展示状态下拉选项。

```ftl
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>${pageTitle!'新增用户'}</title>
</head>
<body>
<h1>${pageTitle!'新增用户'}</h1>

<form method="post" action="/users">
    <p>
        <label>用户名：</label>
        <input type="text" name="username" value="${user.username!''}">
    </p>

    <p>
        <label>邮箱：</label>
        <input type="email" name="email" value="${user.email!''}">
    </p>

    <p>
        <label>昵称：</label>
        <input type="text" name="nickname" value="${user.nickname!''}">
    </p>

    <p>
        <label>状态：</label>
        <select name="status">
            <option value="1">正常</option>
            <option value="0">禁用</option>
        </select>
    </p>

    <button type="submit">保存</button>
    <a href="/users">返回</a>
</form>
</body>
</html>
```

对象和集合渲染时应注意以下几点：

| 注意项       | 说明                                  |
| ------------ | ------------------------------------- |
| 空对象处理   | 使用 `user??` 判断对象是否存在        |
| 空字段处理   | 使用 `${field!'默认值'}` 设置默认值   |
| 空集合处理   | 使用 `list?? && list?size gt 0` 判断  |
| 日期格式化   | 使用 `?string('yyyy-MM-dd HH:mm:ss')` |
| 用户输入输出 | 使用 `?html` 进行 HTML 转义           |

对于用户提交的昵称、备注、评论等字段，建议在模板中使用 HTML 转义。

```ftl
<p>昵称：${user.nickname!?html}</p>
<p>备注：${user.remark!?html}</p>
```

这样可以降低页面直接渲染用户输入内容带来的 XSS 风险。

## 静态资源处理

本节用于说明 FreeMarker 页面中如何引入 CSS、JavaScript、图片资源以及 WebJars 资源。Spring Boot 默认会将 `src/main/resources/static/` 目录下的文件映射为静态资源，页面可以直接通过根路径访问。

### CSS 与 JavaScript 引入

Spring Boot 默认静态资源目录包括 `classpath:/static/`、`classpath:/public/`、`classpath:/resources/` 和 `classpath:/META-INF/resources/`。实际项目中最常用的是 `src/main/resources/static/`。

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

文件位置：`src/main/resources/static/css/app.css`

下面的样式文件用于定义页面的基础布局和表格样式。

```css
body {
    margin: 0;
    padding: 24px;
    font-family: Arial, "Microsoft YaHei", sans-serif;
    background-color: #f7f8fa;
    color: #333;
}

h1 {
    margin-bottom: 16px;
}

a {
    margin-right: 8px;
    color: #1677ff;
    text-decoration: none;
}

table {
    width: 100%;
    margin-top: 16px;
    border-collapse: collapse;
    background-color: #fff;
}

th {
    background-color: #f0f2f5;
}

th,
td {
    padding: 8px;
    border: 1px solid #dcdfe6;
    text-align: left;
}

button {
    padding: 6px 12px;
    cursor: pointer;
}
```

文件位置：`src/main/resources/static/js/app.js`

下面的脚本文件用于页面加载后输出日志，并封装一个简单的删除确认方法。

```javascript
window.addEventListener('DOMContentLoaded', function () {
    console.log('FreeMarker 页面加载完成');
});

function confirmDelete(username) {
    return window.confirm('确认删除用户：' + username + ' 吗？');
}
```

文件位置：`src/main/resources/templates/user/list.ftl`

下面的模板演示如何引入 CSS 和 JavaScript 静态资源。

```ftl
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>${pageTitle!'用户列表'}</title>

    <link rel="stylesheet" href="/css/app.css">
</head>
<body>
<h1>${pageTitle!'用户列表'}</h1>

<#if userList?? && userList?size gt 0>
    <table>
        <thead>
        <tr>
            <th>序号</th>
            <th>用户名</th>
            <th>邮箱</th>
            <th>操作</th>
        </tr>
        </thead>
        <tbody>
        <#list userList as user>
            <tr>
                <td>${user_counter}</td>
                <td>${user.username!'未设置'}</td>
                <td>${user.email!'未设置'}</td>
                <td>
                    <a href="/users/${user.id}">详情</a>
                    <button type="button" onclick="return confirmDelete('${user.username!?js_string}')">
                        删除
                    </button>
                </td>
            </tr>
        </#list>
        </tbody>
    </table>
<#else>
    <p>暂无用户数据</p>
</#if>

<script src="/js/app.js"></script>
</body>
</html>
```

引入静态资源时，路径 `/css/app.css` 对应文件 `src/main/resources/static/css/app.css`，路径 `/js/app.js` 对应文件 `src/main/resources/static/js/app.js`。

如果项目配置了上下文路径，例如：

```yaml
server:
  servlet:
    context-path: /freemarker-demo
```

页面中建议通过 `request.contextPath` 拼接资源路径。

```ftl
<link rel="stylesheet" href="${request.contextPath}/css/app.css">
<script src="${request.contextPath}/js/app.js"></script>
```

这样即使项目部署在 `/freemarker-demo` 子路径下，静态资源也可以正常加载。

### 图片资源访问

图片资源通常放在 `src/main/resources/static/images/` 目录下。Spring Boot 会自动将该目录映射到 Web 根路径，因此页面可以直接通过 `/images/xxx.png` 访问图片。

推荐图片资源目录如下：

```text
src/main/resources/static/images
├── logo.png
├── avatar-default.png
└── empty.png
```

文件位置：`src/main/resources/templates/index.ftl`

下面的模板用于展示系统 Logo 和默认头像图片。

```ftl
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>${pageTitle!'首页'}</title>
    <link rel="stylesheet" href="/css/app.css">
</head>
<body>
<header>
    <img src="/images/logo.png" alt="系统Logo" style="height: 48px;">
    <h1>${systemName!'FreeMarker 示例系统'}</h1>
</header>

<section>
    <h2>当前用户</h2>
    <img src="/images/avatar-default.png" alt="默认头像" style="height: 80px;">
    <p>${loginUser.username!'未登录'}</p>
</section>
</body>
</html>
```

如果图片路径由后端动态传入，也可以通过变量渲染。

Controller 示例：

```java
model.addAttribute("logoPath", "/images/logo.png");
model.addAttribute("avatarPath", "/images/avatar-default.png");
```

模板示例：

```ftl
<img src="${logoPath!'/images/logo.png'}" alt="系统Logo">
<img src="${avatarPath!'/images/avatar-default.png'}" alt="用户头像">
```

对于用户上传的图片，不建议直接放在 `static` 目录下。`static` 目录适合存放项目内置资源，例如 Logo、默认头像、背景图、图标等。用户上传资源更适合存放在文件服务器、对象存储或专门的上传目录中，并通过接口或资源映射方式访问。

图片资源访问常见问题如下：

| 问题                 | 可能原因                      | 处理方式                      |
| -------------------- | ----------------------------- | ----------------------------- |
| 图片 404             | 文件不在 `static/images` 目录 | 检查文件路径和文件名          |
| 本地正常，部署后异常 | 大小写不一致                  | Linux 文件路径区分大小写      |
| 页面路径正确但不显示 | 浏览器缓存                    | 强制刷新或清理缓存            |
| 子路径部署后图片失效 | 未拼接 context-path           | 使用 `${request.contextPath}` |

如果项目存在上下文路径，推荐这样写：

```ftl
<img src="${request.contextPath}/images/logo.png" alt="系统Logo">
```

### WebJars 使用方式

WebJars 可以将前端依赖以 Jar 包形式引入 Maven 项目，例如 Bootstrap、jQuery、Axios 等。对于传统服务端模板项目，WebJars 可以减少手动下载前端静态资源的工作量，并让前端资源版本跟随 Maven 依赖统一管理。

文件位置：`pom.xml`

下面示例引入 Bootstrap 和 jQuery 的 WebJars 依赖。

```xml
<dependencies>
    <!-- Bootstrap WebJar：提供 Bootstrap CSS 和 JavaScript 静态资源 -->
    <dependency>
        <groupId>org.webjars</groupId>
        <artifactId>bootstrap</artifactId>
        <version>5.3.3</version>
    </dependency>

    <!-- jQuery WebJar：部分旧插件或传统页面脚本可能依赖 jQuery -->
    <dependency>
        <groupId>org.webjars</groupId>
        <artifactId>jquery</artifactId>
        <version>3.7.1</version>
    </dependency>

    <!-- WebJars Locator：支持省略 WebJar 资源路径中的版本号 -->
    <dependency>
        <groupId>org.webjars</groupId>
        <artifactId>webjars-locator-core</artifactId>
    </dependency>
</dependencies>
```

引入依赖后，WebJars 资源默认可以通过 `/webjars/**` 路径访问。

不使用 `webjars-locator-core` 时，需要写完整版本号路径：

```ftl
<link rel="stylesheet" href="/webjars/bootstrap/5.3.3/css/bootstrap.min.css">
<script src="/webjars/jquery/3.7.1/jquery.min.js"></script>
<script src="/webjars/bootstrap/5.3.3/js/bootstrap.bundle.min.js"></script>
```

引入 `webjars-locator-core` 后，通常可以省略版本号：

```ftl
<link rel="stylesheet" href="/webjars/bootstrap/css/bootstrap.min.css">
<script src="/webjars/jquery/jquery.min.js"></script>
<script src="/webjars/bootstrap/js/bootstrap.bundle.min.js"></script>
```

文件位置：`src/main/resources/templates/user/list.ftl`

下面的模板使用 Bootstrap 渲染用户列表页面。

```ftl
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>${pageTitle!'用户列表'}</title>

    <link rel="stylesheet" href="/webjars/bootstrap/css/bootstrap.min.css">
</head>
<body>
<div class="container mt-4">
    <div class="d-flex align-items-center justify-content-between mb-3">
        <h1 class="h3">${pageTitle!'用户列表'}</h1>
        <a class="btn btn-primary" href="/users/add">新增用户</a>
    </div>

    <#if userList?? && userList?size gt 0>
        <table class="table table-bordered table-hover">
            <thead class="table-light">
            <tr>
                <th>序号</th>
                <th>用户名</th>
                <th>邮箱</th>
                <th>昵称</th>
                <th>状态</th>
                <th>操作</th>
            </tr>
            </thead>
            <tbody>
            <#list userList as user>
                <tr>
                    <td>${user_counter}</td>
                    <td>${user.username!'未设置'}</td>
                    <td>${user.email!'未设置'}</td>
                    <td>${user.nickname!'匿名用户'}</td>
                    <td>
                        <#if user.status == 1>
                            <span class="badge text-bg-success">正常</span>
                        <#elseif user.status == 0>
                            <span class="badge text-bg-secondary">禁用</span>
                        <#else>
                            <span class="badge text-bg-warning">未知</span>
                        </#if>
                    </td>
                    <td>
                        <a class="btn btn-sm btn-outline-primary" href="/users/${user.id}">详情</a>
                        <a class="btn btn-sm btn-outline-secondary" href="/users/${user.id}/edit">编辑</a>
                    </td>
                </tr>
            </#list>
            </tbody>
        </table>
    <#else>
        <div class="alert alert-info">暂无用户数据</div>
    </#if>
</div>

<script src="/webjars/bootstrap/js/bootstrap.bundle.min.js"></script>
</body>
</html>
```

添加 WebJars 依赖后，可以通过浏览器直接访问资源路径验证是否可用。

```text
http://localhost:8080/webjars/bootstrap/css/bootstrap.min.css
http://localhost:8080/webjars/jquery/jquery.min.js
```

如果访问返回 404，优先检查以下内容：

| 检查项     | 说明                                          |
| ---------- | --------------------------------------------- |
| Maven 依赖 | 确认 WebJars 依赖已经引入                     |
| 版本路径   | 未使用 locator 时必须带版本号                 |
| 资源路径   | 确认 CSS、JS 文件路径正确                     |
| 依赖下载   | 执行 `mvn clean compile` 重新下载依赖         |
| 上下文路径 | 子路径部署时需要拼接 `${request.contextPath}` |

WebJars 适合传统模板项目、后台管理页面和小型服务端渲染项目。如果项目已经使用 Vue、React、Vite、Webpack 等现代前端工程化方案，通常不需要再使用 WebJars 管理前端依赖。



## 表单开发

本节用于说明 FreeMarker 页面中表单的编写方式、表单数据提交方式，以及 Spring Boot 3 后端如何接收参数并进行校验。表单开发通常涉及页面模板、DTO 参数对象、Controller 接收逻辑和校验错误回显。

### 表单页面编写

表单页面主要用于新增、编辑、查询等业务操作。FreeMarker 中的表单本质上仍然是普通 HTML 表单，只是可以通过模板变量实现数据回显、默认值处理和校验错误展示。

推荐目录结构如下：

```text
src/main/resources/templates
└── user
    ├── add.ftl
    └── edit.ftl
```

文件位置：`src/main/resources/templates/user/add.ftl`

下面的模板用于编写用户新增表单，并支持字段默认值和错误信息回显。

```ftl
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>${pageTitle!'新增用户'}</title>
    <link rel="stylesheet" href="${request.contextPath}/css/app.css">
</head>
<body>
<h1>${pageTitle!'新增用户'}</h1>

<form method="post" action="${request.contextPath}/users">
    <p>
        <label>用户名：</label>
        <input type="text" name="username" value="${form.username!''}">
        <#if errors?? && errors.username??>
            <span style="color: red;">${errors.username}</span>
        </#if>
    </p>

    <p>
        <label>邮箱：</label>
        <input type="email" name="email" value="${form.email!''}">
        <#if errors?? && errors.email??>
            <span style="color: red;">${errors.email}</span>
        </#if>
    </p>

    <p>
        <label>昵称：</label>
        <input type="text" name="nickname" value="${form.nickname!''}">
        <#if errors?? && errors.nickname??>
            <span style="color: red;">${errors.nickname}</span>
        </#if>
    </p>

    <p>
        <label>状态：</label>
        <select name="status">
            <option value="1" <#if (form.status!1) == 1>selected</#if>>正常</option>
            <option value="0" <#if (form.status!1) == 0>selected</#if>>禁用</option>
        </select>
        <#if errors?? && errors.status??>
            <span style="color: red;">${errors.status}</span>
        </#if>
    </p>

    <button type="submit">保存</button>
    <a href="${request.contextPath}/users">返回</a>
</form>
</body>
</html>
```

表单中的 `name` 属性需要和后端 DTO 字段名称保持一致。例如 `name="username"` 对应后端 `UserForm.username` 字段。这样 Spring MVC 才能自动完成请求参数绑定。

对于编辑页面，通常需要从后端传入已有用户数据，并将数据回显到表单中。

文件位置：`src/main/resources/templates/user/edit.ftl`

下面的模板用于编辑用户信息，并通过隐藏字段提交用户 ID。

```ftl
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>${pageTitle!'编辑用户'}</title>
    <link rel="stylesheet" href="${request.contextPath}/css/app.css">
</head>
<body>
<h1>${pageTitle!'编辑用户'}</h1>

<form method="post" action="${request.contextPath}/users/${form.id}">
    <input type="hidden" name="id" value="${form.id}">

    <p>
        <label>用户名：</label>
        <input type="text" name="username" value="${form.username!''}">
        <#if errors?? && errors.username??>
            <span style="color: red;">${errors.username}</span>
        </#if>
    </p>

    <p>
        <label>邮箱：</label>
        <input type="email" name="email" value="${form.email!''}">
        <#if errors?? && errors.email??>
            <span style="color: red;">${errors.email}</span>
        </#if>
    </p>

    <p>
        <label>昵称：</label>
        <input type="text" name="nickname" value="${form.nickname!''}">
        <#if errors?? && errors.nickname??>
            <span style="color: red;">${errors.nickname}</span>
        </#if>
    </p>

    <p>
        <label>状态：</label>
        <select name="status">
            <option value="1" <#if form.status == 1>selected</#if>>正常</option>
            <option value="0" <#if form.status == 0>selected</#if>>禁用</option>
        </select>
        <#if errors?? && errors.status??>
            <span style="color: red;">${errors.status}</span>
        </#if>
    </p>

    <button type="submit">保存修改</button>
    <a href="${request.contextPath}/users">返回</a>
</form>
</body>
</html>
```

表单页面开发时，建议统一使用 `form` 作为表单对象变量名，统一使用 `errors` 作为校验错误变量名。这样新增和编辑页面可以保持一致的回显逻辑。

### 表单数据提交

表单数据提交通常使用 `POST` 请求。浏览器提交表单后，Spring MVC 会根据请求参数名称自动绑定到 Controller 方法参数或 DTO 对象中。

文件位置：`src/main/java/io/github/atengk/freemarker/form/UserForm.java`

下面的表单对象用于接收用户新增和编辑请求参数。

```java
package io.github.atengk.freemarker.form;

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
public class UserForm {

    /**
     * 用户ID，新增时为空，编辑时必填
     */
    private Long id;

    /**
     * 用户名
     */
    @NotBlank(message = "用户名不能为空")
    @Size(max = 30, message = "用户名长度不能超过30个字符")
    private String username;

    /**
     * 邮箱
     */
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    /**
     * 昵称
     */
    @Size(max = 30, message = "昵称长度不能超过30个字符")
    private String nickname;

    /**
     * 状态：1正常，0禁用
     */
    @NotNull(message = "状态不能为空")
    @Min(value = 0, message = "状态值不正确")
    @Max(value = 1, message = "状态值不正确")
    private Integer status;
}
```

如果项目中还没有引入参数校验依赖，需要在 `pom.xml` 中加入以下依赖。

文件位置：`pom.xml`

```xml
<!-- Spring Boot Validation：提供 Jakarta Validation 参数校验能力 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

文件位置：`src/main/java/io/github/atengk/freemarker/controller/UserFormController.java`

下面的 Controller 用于处理用户新增和编辑表单提交。

```java
package io.github.atengk.freemarker.controller;

import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.freemarker.form.UserForm;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户表单控制器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Controller
public class UserFormController {

    /**
     * 用户新增页面
     *
     * @param model 页面模型对象
     * @return 用户新增模板路径
     */
    @GetMapping("/users/add")
    public String addPage(Model model) {
        UserForm form = new UserForm();
        form.setStatus(1);

        model.addAttribute("pageTitle", "新增用户");
        model.addAttribute("form", form);

        log.info("访问用户新增页面");
        return "user/add";
    }

    /**
     * 提交新增用户表单
     *
     * @param form 表单参数
     * @param bindingResult 参数绑定和校验结果
     * @param model 页面模型对象
     * @return 成功时跳转用户列表，失败时返回新增页面
     */
    @PostMapping("/users")
    public String create(@Valid UserForm form, BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            log.warn("新增用户参数校验失败，用户名：{}", form.getUsername());
            model.addAttribute("pageTitle", "新增用户");
            model.addAttribute("form", form);
            model.addAttribute("errors", toErrorMap(bindingResult));
            return "user/add";
        }

        String nickname = StrUtil.blankToDefault(form.getNickname(), "匿名用户");
        log.info("新增用户成功，用户名：{}，昵称：{}", form.getUsername(), nickname);

        return "redirect:/users";
    }

    /**
     * 用户编辑页面
     *
     * @param id 用户ID
     * @param model 页面模型对象
     * @return 用户编辑模板路径
     */
    @GetMapping("/users/{id}/edit")
    public String editPage(@PathVariable Long id, Model model) {
        UserForm form = new UserForm();
        form.setId(id);
        form.setUsername("ateng");
        form.setEmail("ateng@example.com");
        form.setNickname("普通用户");
        form.setStatus(1);

        model.addAttribute("pageTitle", "编辑用户");
        model.addAttribute("form", form);

        log.info("访问用户编辑页面，用户ID：{}", id);
        return "user/edit";
    }

    /**
     * 提交编辑用户表单
     *
     * @param id 用户ID
     * @param form 表单参数
     * @param bindingResult 参数绑定和校验结果
     * @param model 页面模型对象
     * @return 成功时跳转用户列表，失败时返回编辑页面
     */
    @PostMapping("/users/{id}")
    public String update(@PathVariable Long id, @Valid UserForm form, BindingResult bindingResult, Model model) {
        form.setId(id);

        if (bindingResult.hasErrors()) {
            log.warn("编辑用户参数校验失败，用户ID：{}", id);
            model.addAttribute("pageTitle", "编辑用户");
            model.addAttribute("form", form);
            model.addAttribute("errors", toErrorMap(bindingResult));
            return "user/edit";
        }

        log.info("编辑用户成功，用户ID：{}，用户名：{}", id, form.getUsername());
        return "redirect:/users";
    }

    /**
     * 转换字段错误信息
     *
     * @param bindingResult 参数绑定和校验结果
     * @return 字段错误映射
     */
    private Map<String, String> toErrorMap(BindingResult bindingResult) {
        return bindingResult.getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        FieldError::getDefaultMessage,
                        (first, second) -> first
                ));
    }
}
```

需要注意，`BindingResult` 必须紧跟在被 `@Valid` 标注的参数后面。否则参数校验失败时，Spring MVC 可能不会进入当前 Controller 方法，而是直接抛出异常。

### 参数接收与校验

Spring Boot 3 使用 Jakarta Validation 进行参数校验，常用注解来自 `jakarta.validation.constraints` 包。表单提交后，如果参数不符合约束规则，错误信息会进入 `BindingResult`，再由 Controller 回填到页面。

常用校验注解如下：

| 注解        | 说明                                 |
| ----------- | ------------------------------------ |
| `@NotNull`  | 不能为 `null`                        |
| `@NotBlank` | 字符串不能为空，且不能只包含空白字符 |
| `@Size`     | 限制字符串、集合长度                 |
| `@Email`    | 校验邮箱格式                         |
| `@Min`      | 数值最小值                           |
| `@Max`      | 数值最大值                           |
| `@Pattern`  | 正则表达式校验                       |

表单参数接收流程如下：

1. 用户填写 FreeMarker 表单
2. 浏览器通过 `POST` 提交表单
3. Spring MVC 将请求参数绑定到 `UserForm`
4. Jakarta Validation 执行字段校验
5. `BindingResult` 保存校验结果
6. Controller 判断是否有错误
7. 有错误则返回原页面并回显错误
8. 无错误则执行业务逻辑并跳转

可以通过以下命令测试表单提交。

```bash
curl -X POST "http://localhost:8080/users" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=ateng&email=ateng@example.com&nickname=普通用户&status=1"
```

如果需要测试校验失败场景，可以提交空用户名或错误邮箱。

```bash
curl -X POST "http://localhost:8080/users" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=&email=wrong-email&nickname=测试&status=1"
```

表单开发中需要重点关注空值、参数类型转换和错误回显。对于新增和编辑表单，建议使用同一个 `UserForm` 参数对象，减少字段重复定义。

## 公共模板封装

本节用于说明如何封装 FreeMarker 公共模板，包括公共头部、公共底部和页面布局复用。公共模板可以减少重复 HTML 代码，让页面结构更加统一，也便于后续维护菜单、标题、静态资源和版权信息。

### 公共头部模板

公共头部模板通常用于封装页面顶部区域，例如系统名称、导航菜单、登录用户信息、公共 CSS 引入等。头部模板建议放在 `templates/common` 目录下。

文件位置：`src/main/resources/templates/common/header.ftl`

下面的模板用于封装系统公共头部和导航菜单。

```ftl
<header class="app-header">
    <div class="app-title">
        <a href="${request.contextPath}/">${systemName!'FreeMarker 示例系统'}</a>
    </div>

    <nav class="app-nav">
        <a href="${request.contextPath}/">首页</a>
        <a href="${request.contextPath}/users">用户管理</a>
        <a href="${request.contextPath}/users/add">新增用户</a>
    </nav>

    <div class="app-user">
        当前用户：${loginUser.username!'未登录'}
    </div>
</header>
```

在业务页面中可以通过 `<#include>` 引入公共头部。

```ftl
<#include "/common/header.ftl">
```

如果公共头部中使用了 `systemName`、`loginUser` 等变量，需要由 Controller 或全局模型提前提供。

文件位置：`src/main/java/io/github/atengk/freemarker/config/GlobalModelConfig.java`

下面的配置类用于为所有 FreeMarker 页面统一提供公共模型数据。

```java
package io.github.atengk.freemarker.config;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Dict;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * 全局页面模型配置
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@ControllerAdvice
public class GlobalModelConfig {

    /**
     * 系统名称
     *
     * @return 系统名称
     */
    @ModelAttribute("systemName")
    public String systemName() {
        return "FreeMarker 示例系统";
    }

    /**
     * 当前年份
     *
     * @return 当前年份
     */
    @ModelAttribute("year")
    public String year() {
        return DateUtil.format(DateUtil.date(), "yyyy");
    }

    /**
     * 当前登录用户
     *
     * @return 登录用户信息
     */
    @ModelAttribute("loginUser")
    public Dict loginUser() {
        return Dict.create()
                .set("username", "Ateng")
                .set("roleName", "管理员");
    }
}
```

通过 `@ControllerAdvice` + `@ModelAttribute` 定义的数据，会自动加入所有 Controller 返回页面的 `Model` 中，适合放置系统名称、当前用户、版权年份、菜单数据等公共变量。

### 公共底部模板

公共底部模板通常用于封装版权信息、备案信息、公共 JavaScript 引入等内容。底部模板也建议放在 `templates/common` 目录下。

文件位置：`src/main/resources/templates/common/footer.ftl`

下面的模板用于封装页面底部版权信息和公共脚本。

```ftl
<footer class="app-footer">
    <p>Copyright © ${year!'2026'} ${systemName!'FreeMarker 示例系统'}</p>
</footer>

<script src="${request.contextPath}/js/app.js"></script>
```

业务页面中引入底部模板：

```ftl
<#include "/common/footer.ftl">
```

完整页面使用方式如下。

文件位置：`src/main/resources/templates/user/list.ftl`

下面的页面通过公共头部和公共底部减少重复代码。

```ftl
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>${pageTitle!'用户列表'} - ${systemName!'FreeMarker 示例系统'}</title>
    <link rel="stylesheet" href="${request.contextPath}/css/app.css">
</head>
<body>
<#include "/common/header.ftl">

<main class="app-main">
    <h1>${pageTitle!'用户列表'}</h1>

    <#if userList?? && userList?size gt 0>
        <ul>
            <#list userList as user>
                <li>${user.username!'未设置'} - ${user.email!'未设置'}</li>
            </#list>
        </ul>
    <#else>
        <p>暂无用户数据</p>
    </#if>
</main>

<#include "/common/footer.ftl">
</body>
</html>
```

这种方式简单直接，适合页面数量不多的项目。如果系统页面较多，建议进一步使用布局模板统一 HTML 骨架。

### 页面布局复用

页面布局复用适合封装完整 HTML 骨架，例如 `DOCTYPE`、`head`、CSS、头部、底部和公共脚本。业务页面只需要提供页面主体内容即可。

推荐使用 FreeMarker 宏定义实现布局复用。

文件位置：`src/main/resources/templates/common/layout.ftl`

下面的布局模板封装了完整页面结构，并通过 `<#nested>` 插入业务页面内容。

```ftl
<#macro page title="页面">
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>${title} - ${systemName!'FreeMarker 示例系统'}</title>
    <link rel="stylesheet" href="${request.contextPath}/css/app.css">
</head>
<body>
<#include "/common/header.ftl">

<main class="app-main">
    <#nested>
</main>

<#include "/common/footer.ftl">
</body>
</html>
</#macro>
```

文件位置：`src/main/resources/templates/user/list.ftl`

下面的页面通过导入布局模板，只保留用户列表自身的业务内容。

```ftl
<#import "/common/layout.ftl" as layout>

<@layout.page title=pageTitle!'用户列表'>
    <h1>${pageTitle!'用户列表'}</h1>

    <div class="toolbar">
        <a href="${request.contextPath}/users/add">新增用户</a>
    </div>

    <#if userList?? && userList?size gt 0>
        <table>
            <thead>
            <tr>
                <th>序号</th>
                <th>用户名</th>
                <th>邮箱</th>
                <th>状态</th>
            </tr>
            </thead>
            <tbody>
            <#list userList as user>
                <tr>
                    <td>${user_counter}</td>
                    <td>${user.username!'未设置'}</td>
                    <td>${user.email!'未设置'}</td>
                    <td>
                        <#if user.status == 1>
                            正常
                        <#else>
                            禁用
                        </#if>
                    </td>
                </tr>
            </#list>
            </tbody>
        </table>
    <#else>
        <p>暂无用户数据</p>
    </#if>
</@layout.page>
```

布局复用后，每个业务页面不再重复编写完整 HTML 结构，有利于统一样式和维护公共资源。如果需要增加全局 CSS、菜单或统计脚本，只需要修改 `layout.ftl` 或公共模板即可。

## 异常与错误页面

本节用于说明 Spring Boot 3 + FreeMarker 项目中如何处理异常、配置自定义错误页面，以及排查常见模板渲染异常。服务端模板项目中，异常处理不仅要返回正确 HTTP 状态码，还需要渲染用户可读的错误页面。

### 统一异常处理

统一异常处理可以集中处理业务异常、参数异常、系统异常等错误，避免 Controller 中重复编写错误页面跳转逻辑。对于页面项目，异常处理方法通常返回 `ModelAndView`。

文件位置：`src/main/java/io/github/atengk/freemarker/exception/BusinessException.java`

下面的异常类用于表示业务处理异常。

```java
package io.github.atengk.freemarker.exception;

import lombok.Getter;

/**
 * 业务异常
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Getter
public class BusinessException extends RuntimeException {

    /**
     * 错误码
     */
    private final String code;

    /**
     * 创建业务异常
     *
     * @param code 错误码
     * @param message 错误消息
     */
    public BusinessException(String code, String message) {
        super(message);
        this.code = code;
    }
}
```

文件位置：`src/main/java/io/github/atengk/freemarker/handler/GlobalExceptionHandler.java`

下面的异常处理器用于将不同异常统一转换为 FreeMarker 错误页面。

```java
package io.github.atengk.freemarker.handler;

import io.github.atengk.freemarker.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

/**
 * 全局异常处理器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常
     *
     * @param request 请求对象
     * @param exception 业务异常
     * @return 错误页面
     */
    @ExceptionHandler(BusinessException.class)
    public ModelAndView handleBusinessException(HttpServletRequest request, BusinessException exception) {
        log.warn("业务异常，请求地址：{}，错误码：{}，错误信息：{}",
                request.getRequestURI(), exception.getCode(), exception.getMessage());

        ModelMap model = new ModelMap();
        model.addAttribute("status", HttpStatus.BAD_REQUEST.value());
        model.addAttribute("error", "业务处理失败");
        model.addAttribute("message", exception.getMessage());
        model.addAttribute("path", request.getRequestURI());

        return new ModelAndView("error/error", model, HttpStatus.BAD_REQUEST);
    }

    /**
     * 处理系统异常
     *
     * @param request 请求对象
     * @param exception 系统异常
     * @return 错误页面
     */
    @ExceptionHandler(Exception.class)
    public ModelAndView handleException(HttpServletRequest request, Exception exception) {
        log.error("系统异常，请求地址：{}", request.getRequestURI(), exception);

        ModelMap model = new ModelMap();
        model.addAttribute("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        model.addAttribute("error", "系统内部错误");
        model.addAttribute("message", "服务暂时不可用，请稍后再试");
        model.addAttribute("path", request.getRequestURI());

        return new ModelAndView("error/error", model, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
```

文件位置：`src/main/resources/templates/error/error.ftl`

下面的模板用于统一展示异常信息。

```ftl
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>错误页面</title>
    <link rel="stylesheet" href="${request.contextPath}/css/app.css">
</head>
<body>
<h1>请求处理失败</h1>

<table>
    <tr>
        <th>状态码</th>
        <td>${status!'500'}</td>
    </tr>
    <tr>
        <th>错误类型</th>
        <td>${error!'系统异常'}</td>
    </tr>
    <tr>
        <th>错误信息</th>
        <td>${message!'服务暂时不可用'}</td>
    </tr>
    <tr>
        <th>请求路径</th>
        <td>${path!''}</td>
    </tr>
</table>

<p>
    <a href="${request.contextPath}/">返回首页</a>
</p>
</body>
</html>
```

统一异常处理适合处理业务代码中主动抛出的异常。如果是 404、500 等容器级错误页面，还需要配置 Spring Boot 的默认错误页模板。

### 自定义错误页面

Spring Boot 支持在 `templates/error/` 目录下放置状态码对应的错误页面。对于 FreeMarker 项目，可以创建 `404.ftl`、`500.ftl` 等模板文件。

推荐目录结构如下：

```text
src/main/resources/templates
└── error
    ├── 404.ftl
    ├── 500.ftl
    └── error.ftl
```

文件位置：`src/main/resources/templates/error/404.ftl`

下面的模板用于展示 404 页面。

```ftl
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>页面不存在</title>
    <link rel="stylesheet" href="${request.contextPath}/css/app.css">
</head>
<body>
<h1>404</h1>
<p>页面不存在或已经被删除。</p>
<p>请求路径：${path!''}</p>
<a href="${request.contextPath}/">返回首页</a>
</body>
</html>
```

文件位置：`src/main/resources/templates/error/500.ftl`

下面的模板用于展示 500 页面。

```ftl
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>系统内部错误</title>
    <link rel="stylesheet" href="${request.contextPath}/css/app.css">
</head>
<body>
<h1>500</h1>
<p>系统内部错误，请稍后再试。</p>
<p>请求路径：${path!''}</p>
<a href="${request.contextPath}/">返回首页</a>
</body>
</html>
```

如果希望错误页面能显示更详细的错误信息，可以在配置文件中开启错误属性暴露。但生产环境不建议暴露异常堆栈。

文件位置：`src/main/resources/application-dev.yml`

```yaml
server:
  error:
    # 开发环境显示错误消息，便于排查问题
    include-message: always
    # 开发环境可按需显示绑定错误
    include-binding-errors: always
    # 开发环境可按需显示异常信息
    include-exception: true
    # 不建议默认暴露堆栈，可通过请求参数 trace=true 控制
    include-stacktrace: on_param
```

文件位置：`src/main/resources/application-prod.yml`

```yaml
server:
  error:
    # 生产环境不暴露具体异常消息
    include-message: never
    # 生产环境不暴露参数绑定错误细节
    include-binding-errors: never
    # 生产环境不暴露异常类名
    include-exception: false
    # 生产环境禁止暴露堆栈
    include-stacktrace: never
```

自定义错误页面主要用于提升用户体验。具体异常细节应写入日志系统，而不是直接展示给用户。

### 模板渲染异常排查

模板渲染异常通常发生在模板路径错误、变量为空、语法错误、静态资源路径错误或编码配置不正确时。排查时建议先看控制台异常堆栈，再对照 Controller 返回值、模板目录和模板变量逐项检查。

常见问题如下：

| 问题               | 常见原因                          | 处理方式                                     |
| ------------------ | --------------------------------- | -------------------------------------------- |
| 页面返回字符串     | 使用了 `@RestController`          | 改为 `@Controller`                           |
| 找不到模板         | Controller 返回值和模板路径不一致 | 检查 `templates` 目录和 `.ftl` 文件名        |
| 中文乱码           | 编码配置不一致                    | 统一使用 UTF-8                               |
| 修改模板不生效     | 开启了模板缓存                    | 开发环境设置 `spring.freemarker.cache=false` |
| 变量不存在异常     | 模板直接访问空变量                | 使用 `??` 判断或 `!` 设置默认值              |
| 静态资源 404       | 资源路径写错                      | 检查 `static` 目录和访问路径                 |
| 子路径部署资源失效 | 未拼接上下文路径                  | 使用 `${request.contextPath}`                |

模板路径排查示例：

```text
Controller 返回值：user/list
模板实际路径：src/main/resources/templates/user/list.ftl
配置模板后缀：spring.freemarker.suffix=.ftl
配置加载路径：spring.freemarker.template-loader-path=classpath:/templates/
```

变量空值排查示例：

```ftl
<#-- 不推荐：变量不存在时可能渲染失败 -->
<p>${user.nickname}</p>

<#-- 推荐：提供默认值 -->
<p>${user.nickname!'匿名用户'}</p>

<#-- 推荐：先判断对象是否存在 -->
<#if user??>
    <p>${user.nickname!'匿名用户'}</p>
<#else>
    <p>用户不存在</p>
</#if>
```

开发环境建议使用以下配置降低排查成本。

```yaml
spring:
  freemarker:
    # 开发环境关闭缓存，模板修改后立即生效
    cache: false
    # 确认模板目录存在
    check-template-location: true
    # 统一模板编码
    charset: UTF-8
```

可以通过以下命令重新编译项目，确认模板和静态资源已经复制到 `target/classes` 目录。

```bash
mvn clean compile
```

命令执行后，重点检查以下目录是否存在对应文件：

```text
target/classes/templates/
target/classes/static/
```

如果模板文件存在于 `src/main/resources/templates/`，但运行时仍然找不到模板，需要确认项目是否使用了正确的运行模块、运行环境是否加载了最新编译结果，以及 IDE 是否开启了资源文件自动复制。



## 项目实践

本节基于前文的基础配置、页面模板、表单处理和公共模板封装，完成一个简单的用户管理页面实践。示例使用内存数据模拟用户存储，便于直接运行和验证；实际项目中可以替换为 MyBatis-Plus、JPA 或其他持久层实现。

本实践包含以下页面：

| 页面         | 请求路径           | 说明         |
| ------------ | ------------------ | ------------ |
| 用户列表页面 | `/users`           | 展示用户列表 |
| 用户新增页面 | `/users/add`       | 新增用户表单 |
| 用户编辑页面 | `/users/{id}/edit` | 编辑用户表单 |
| 用户详情页面 | `/users/{id}`      | 查看用户详情 |

推荐目录结构如下：

```text
src/main/java/io/github/atengk/freemarker
├── controller
│   └── UserPracticeController.java
├── form
│   └── UserForm.java
├── service
│   └── UserPracticeService.java
└── vo
    └── UserPageVO.java

src/main/resources/templates
└── user
    ├── list.ftl
    ├── add.ftl
    ├── edit.ftl
    └── detail.ftl
```

### 用户列表页面

用户列表页面用于展示所有用户数据，并提供详情、编辑、新增等入口。Controller 负责查询用户集合，模板负责渲染表格。

文件位置：`src/main/java/io/github/atengk/freemarker/vo/UserPageVO.java`

下面的 VO 用于承载页面展示所需的用户信息。

```java
package io.github.atengk.freemarker.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户页面展示对象
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserPageVO {

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 状态：1正常，0禁用
     */
    private Integer status;

    /**
     * 创建时间
     */
    private String createTime;
}
```

文件位置：`src/main/java/io/github/atengk/freemarker/service/UserPracticeService.java`

下面的 Service 使用内存 Map 模拟用户数据存储，包含查询、新增、编辑和详情查询方法。

```java
package io.github.atengk.freemarker.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.freemarker.exception.BusinessException;
import io.github.atengk.freemarker.form.UserForm;
import io.github.atengk.freemarker.vo.UserPageVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 用户实践服务
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Service
public class UserPracticeService {

    /**
     * 用户ID生成器
     */
    private static final AtomicLong ID_GENERATOR = new AtomicLong(1000);

    /**
     * 内存用户数据
     */
    private static final Map<Long, UserPageVO> USER_MAP = new ConcurrentHashMap<>();

    static {
        USER_MAP.put(1L, new UserPageVO(1L, "admin", "admin@example.com", "系统管理员", 1, DateUtil.now()));
        USER_MAP.put(2L, new UserPageVO(2L, "ateng", "ateng@example.com", "普通用户", 1, DateUtil.now()));
        USER_MAP.put(3L, new UserPageVO(3L, "test", "test@example.com", "测试用户", 0, DateUtil.now()));
    }

    /**
     * 查询用户列表
     *
     * @return 用户列表
     */
    public List<UserPageVO> list() {
        List<UserPageVO> userList = ListUtil.toList(USER_MAP.values());
        userList.sort(Comparator.comparing(UserPageVO::getId));
        return userList;
    }

    /**
     * 查询用户详情
     *
     * @param id 用户ID
     * @return 用户信息
     */
    public UserPageVO detail(Long id) {
        UserPageVO user = USER_MAP.get(id);
        if (ObjectUtil.isNull(user)) {
            log.warn("用户不存在，用户ID：{}", id);
            throw new BusinessException("USER_NOT_FOUND", "用户不存在");
        }
        return user;
    }

    /**
     * 新增用户
     *
     * @param form 用户表单
     */
    public void create(UserForm form) {
        Long id = ID_GENERATOR.incrementAndGet();

        UserPageVO user = new UserPageVO();
        BeanUtil.copyProperties(form, user);
        user.setId(id);
        user.setNickname(StrUtil.blankToDefault(form.getNickname(), "匿名用户"));
        user.setCreateTime(DateUtil.now());

        USER_MAP.put(id, user);
        log.info("新增用户成功，用户ID：{}，用户名：{}", id, user.getUsername());
    }

    /**
     * 更新用户
     *
     * @param id 用户ID
     * @param form 用户表单
     */
    public void update(Long id, UserForm form) {
        UserPageVO user = detail(id);

        user.setUsername(form.getUsername());
        user.setEmail(form.getEmail());
        user.setNickname(StrUtil.blankToDefault(form.getNickname(), "匿名用户"));
        user.setStatus(form.getStatus());

        USER_MAP.put(id, user);
        log.info("更新用户成功，用户ID：{}，用户名：{}", id, user.getUsername());
    }
}
```

文件位置：`src/main/java/io/github/atengk/freemarker/controller/UserPracticeController.java`

下面的 Controller 负责用户列表、详情、新增、编辑页面的跳转和表单提交处理。

```java
package io.github.atengk.freemarker.controller;

import io.github.atengk.freemarker.form.UserForm;
import io.github.atengk.freemarker.service.UserPracticeService;
import io.github.atengk.freemarker.vo.UserPageVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户实践控制器
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
     * @param model 页面模型对象
     * @return 用户列表模板
     */
    @GetMapping("/users")
    public String list(Model model) {
        model.addAttribute("pageTitle", "用户列表");
        model.addAttribute("userList", userPracticeService.list());

        log.info("访问用户列表页面");
        return "user/list";
    }

    /**
     * 用户详情页面
     *
     * @param id 用户ID
     * @param model 页面模型对象
     * @return 用户详情模板
     */
    @GetMapping("/users/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("pageTitle", "用户详情");
        model.addAttribute("user", userPracticeService.detail(id));

        log.info("访问用户详情页面，用户ID：{}", id);
        return "user/detail";
    }

    /**
     * 用户新增页面
     *
     * @param model 页面模型对象
     * @return 用户新增模板
     */
    @GetMapping("/users/add")
    public String addPage(Model model) {
        UserForm form = new UserForm();
        form.setStatus(1);

        model.addAttribute("pageTitle", "新增用户");
        model.addAttribute("form", form);

        log.info("访问用户新增页面");
        return "user/add";
    }

    /**
     * 提交新增用户
     *
     * @param form 用户表单
     * @param bindingResult 参数校验结果
     * @param model 页面模型对象
     * @return 成功跳转列表页，失败返回新增页
     */
    @PostMapping("/users")
    public String create(@Valid UserForm form, BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("pageTitle", "新增用户");
            model.addAttribute("form", form);
            model.addAttribute("errors", toErrorMap(bindingResult));
            log.warn("新增用户参数校验失败");
            return "user/add";
        }

        userPracticeService.create(form);
        return "redirect:/users";
    }

    /**
     * 用户编辑页面
     *
     * @param id 用户ID
     * @param model 页面模型对象
     * @return 用户编辑模板
     */
    @GetMapping("/users/{id}/edit")
    public String editPage(@PathVariable Long id, Model model) {
        UserPageVO user = userPracticeService.detail(id);

        UserForm form = new UserForm();
        form.setId(user.getId());
        form.setUsername(user.getUsername());
        form.setEmail(user.getEmail());
        form.setNickname(user.getNickname());
        form.setStatus(user.getStatus());

        model.addAttribute("pageTitle", "编辑用户");
        model.addAttribute("form", form);

        log.info("访问用户编辑页面，用户ID：{}", id);
        return "user/edit";
    }

    /**
     * 提交编辑用户
     *
     * @param id 用户ID
     * @param form 用户表单
     * @param bindingResult 参数校验结果
     * @param model 页面模型对象
     * @return 成功跳转列表页，失败返回编辑页
     */
    @PostMapping("/users/{id}")
    public String update(@PathVariable Long id, @Valid UserForm form, BindingResult bindingResult, Model model) {
        form.setId(id);

        if (bindingResult.hasErrors()) {
            model.addAttribute("pageTitle", "编辑用户");
            model.addAttribute("form", form);
            model.addAttribute("errors", toErrorMap(bindingResult));
            log.warn("编辑用户参数校验失败，用户ID：{}", id);
            return "user/edit";
        }

        userPracticeService.update(id, form);
        return "redirect:/users";
    }

    /**
     * 转换字段校验错误
     *
     * @param bindingResult 参数校验结果
     * @return 字段错误映射
     */
    private Map<String, String> toErrorMap(BindingResult bindingResult) {
        return bindingResult.getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        FieldError::getDefaultMessage,
                        (first, second) -> first
                ));
    }
}
```

文件位置：`src/main/resources/templates/user/list.ftl`

下面的模板用于渲染用户列表页面。

```ftl
<#import "/common/layout.ftl" as layout>

<@layout.page title=pageTitle!'用户列表'>
    <h1>${pageTitle!'用户列表'}</h1>

    <div class="toolbar">
        <a href="${request.contextPath}/users/add">新增用户</a>
    </div>

    <#if userList?? && userList?size gt 0>
        <table>
            <thead>
            <tr>
                <th>序号</th>
                <th>用户ID</th>
                <th>用户名</th>
                <th>邮箱</th>
                <th>昵称</th>
                <th>状态</th>
                <th>创建时间</th>
                <th>操作</th>
            </tr>
            </thead>
            <tbody>
            <#list userList as user>
                <tr>
                    <td>${user_counter}</td>
                    <td>${user.id}</td>
                    <td>${user.username!?html}</td>
                    <td>${user.email!?html}</td>
                    <td>${user.nickname!'匿名用户'?html}</td>
                    <td>
                        <#if user.status == 1>
                            正常
                        <#elseif user.status == 0>
                            禁用
                        <#else>
                            未知
                        </#if>
                    </td>
                    <td>${user.createTime!''}</td>
                    <td>
                        <a href="${request.contextPath}/users/${user.id}">详情</a>
                        <a href="${request.contextPath}/users/${user.id}/edit">编辑</a>
                    </td>
                </tr>
            </#list>
            </tbody>
        </table>
    <#else>
        <p>暂无用户数据</p>
    </#if>
</@layout.page>
```

### 用户新增页面

用户新增页面用于提交用户基础信息。新增成功后跳转到用户列表页面；校验失败时返回当前页面并回显错误信息。

文件位置：`src/main/resources/templates/user/add.ftl`

下面的模板用于渲染新增用户表单。

```ftl
<#import "/common/layout.ftl" as layout>

<@layout.page title=pageTitle!'新增用户'>
    <h1>${pageTitle!'新增用户'}</h1>

    <form method="post" action="${request.contextPath}/users">
        <p>
            <label>用户名：</label>
            <input type="text" name="username" value="${form.username!''}">
            <#if errors?? && errors.username??>
                <span class="error">${errors.username}</span>
            </#if>
        </p>

        <p>
            <label>邮箱：</label>
            <input type="email" name="email" value="${form.email!''}">
            <#if errors?? && errors.email??>
                <span class="error">${errors.email}</span>
            </#if>
        </p>

        <p>
            <label>昵称：</label>
            <input type="text" name="nickname" value="${form.nickname!''}">
            <#if errors?? && errors.nickname??>
                <span class="error">${errors.nickname}</span>
            </#if>
        </p>

        <p>
            <label>状态：</label>
            <select name="status">
                <option value="1" <#if (form.status!1) == 1>selected</#if>>正常</option>
                <option value="0" <#if (form.status!1) == 0>selected</#if>>禁用</option>
            </select>
            <#if errors?? && errors.status??>
                <span class="error">${errors.status}</span>
            </#if>
        </p>

        <button type="submit">保存</button>
        <a href="${request.contextPath}/users">返回</a>
    </form>
</@layout.page>
```

### 用户编辑页面

用户编辑页面用于回显已有用户数据，并提交修改后的字段。编辑页面与新增页面结构基本一致，但需要携带用户 ID，并将表单提交到 `/users/{id}`。

文件位置：`src/main/resources/templates/user/edit.ftl`

下面的模板用于渲染编辑用户表单。

```ftl
<#import "/common/layout.ftl" as layout>

<@layout.page title=pageTitle!'编辑用户'>
    <h1>${pageTitle!'编辑用户'}</h1>

    <form method="post" action="${request.contextPath}/users/${form.id}">
        <input type="hidden" name="id" value="${form.id}">

        <p>
            <label>用户名：</label>
            <input type="text" name="username" value="${form.username!''}">
            <#if errors?? && errors.username??>
                <span class="error">${errors.username}</span>
            </#if>
        </p>

        <p>
            <label>邮箱：</label>
            <input type="email" name="email" value="${form.email!''}">
            <#if errors?? && errors.email??>
                <span class="error">${errors.email}</span>
            </#if>
        </p>

        <p>
            <label>昵称：</label>
            <input type="text" name="nickname" value="${form.nickname!''}">
            <#if errors?? && errors.nickname??>
                <span class="error">${errors.nickname}</span>
            </#if>
        </p>

        <p>
            <label>状态：</label>
            <select name="status">
                <option value="1" <#if (form.status!1) == 1>selected</#if>>正常</option>
                <option value="0" <#if (form.status!1) == 0>selected</#if>>禁用</option>
            </select>
            <#if errors?? && errors.status??>
                <span class="error">${errors.status}</span>
            </#if>
        </p>

        <button type="submit">保存修改</button>
        <a href="${request.contextPath}/users/${form.id}">查看详情</a>
        <a href="${request.contextPath}/users">返回</a>
    </form>
</@layout.page>
```

### 用户详情页面

用户详情页面用于展示单个用户的完整信息。详情页通常只读展示，不处理表单提交。

文件位置：`src/main/resources/templates/user/detail.ftl`

下面的模板用于渲染用户详情。

```ftl
<#import "/common/layout.ftl" as layout>

<@layout.page title=pageTitle!'用户详情'>
    <h1>${pageTitle!'用户详情'}</h1>

    <#if user??>
        <table>
            <tr>
                <th>用户ID</th>
                <td>${user.id}</td>
            </tr>
            <tr>
                <th>用户名</th>
                <td>${user.username!?html}</td>
            </tr>
            <tr>
                <th>邮箱</th>
                <td>${user.email!?html}</td>
            </tr>
            <tr>
                <th>昵称</th>
                <td>${user.nickname!'匿名用户'?html}</td>
            </tr>
            <tr>
                <th>状态</th>
                <td>
                    <#if user.status == 1>
                        正常
                    <#elseif user.status == 0>
                        禁用
                    <#else>
                        未知
                    </#if>
                </td>
            </tr>
            <tr>
                <th>创建时间</th>
                <td>${user.createTime!''}</td>
            </tr>
        </table>

        <p>
            <a href="${request.contextPath}/users/${user.id}/edit">编辑用户</a>
            <a href="${request.contextPath}/users">返回列表</a>
        </p>
    <#else>
        <p>用户不存在</p>
        <a href="${request.contextPath}/users">返回列表</a>
    </#if>
</@layout.page>
```

完成以上页面后，启动项目并依次访问：

```text
http://localhost:8080/users
http://localhost:8080/users/add
http://localhost:8080/users/1
http://localhost:8080/users/1/edit
```

## 打包与部署

本节用于说明 Spring Boot 3 FreeMarker 项目的本地运行、Jar 包部署和生产环境配置。FreeMarker 模板文件位于 `src/main/resources/templates`，静态资源位于 `src/main/resources/static`，打包后会进入 Jar 包的 classpath 中。

### 本地运行

本地开发阶段可以直接使用 Maven 启动项目，也可以通过 IDE 启动主启动类。开发阶段建议关闭 FreeMarker 缓存，便于修改模板后立即查看效果。

文件位置：`src/main/resources/application-dev.yml`

```yaml
server:
  port: 8080

spring:
  freemarker:
    # 开发环境关闭模板缓存，便于调试
    cache: false
    # 开发环境检查模板目录是否存在
    check-template-location: true
    # 统一模板编码
    charset: UTF-8
```

使用 Maven 本地运行：

```bash
mvn spring-boot:run
```

也可以先编译项目：

```bash
mvn clean compile
```

本地运行后访问：

```text
http://localhost:8080/users
```

如果页面无法访问，优先检查以下内容：

| 检查项          | 说明                                        |
| --------------- | ------------------------------------------- |
| 启动端口        | 是否为 `8080`                               |
| Controller 注解 | 页面 Controller 应使用 `@Controller`        |
| 模板路径        | Controller 返回值是否和 `.ftl` 文件路径一致 |
| 模板缓存        | 开发环境是否设置 `cache: false`             |
| 静态资源        | CSS、JS 是否位于 `static` 目录下            |

### Jar 包部署

Spring Boot 项目可以通过 Maven 打包为可执行 Jar。FreeMarker 模板和静态资源会一起打入 Jar 包中，运行时通过 classpath 加载。

执行打包命令：

```bash
mvn clean package -DskipTests
```

命令说明：

`clean` 用于清理旧的构建产物。
`package` 用于编译并打包项目。
`-DskipTests` 用于跳过测试阶段，适合部署前快速打包；正式流水线中建议保留测试。

打包完成后，Jar 文件通常位于：

```text
target/springboot3-freemarker-demo-0.0.1-SNAPSHOT.jar
```

运行 Jar 包：

```bash
java -jar target/springboot3-freemarker-demo-0.0.1-SNAPSHOT.jar
```

指定生产环境配置运行：

```bash
java -jar target/springboot3-freemarker-demo-0.0.1-SNAPSHOT.jar \
  --spring.profiles.active=prod
```

后台运行示例：

```bash
nohup java -jar springboot3-freemarker-demo-0.0.1-SNAPSHOT.jar \
  --spring.profiles.active=prod \
  > app.log 2>&1 &
```

命令说明：

`nohup` 用于让进程在终端退出后继续运行。
`--spring.profiles.active=prod` 用于启用生产环境配置。
`> app.log 2>&1` 用于将标准输出和错误输出写入日志文件。
`&` 表示后台运行。

查看进程：

```bash
ps -ef | grep springboot3-freemarker-demo
```

查看日志：

```bash
tail -f app.log
```

停止服务时，可以先查找进程 ID，再执行 `kill`：

```bash
ps -ef | grep springboot3-freemarker-demo
kill <pid>
```

### 生产环境配置

生产环境应开启模板缓存，关闭详细错误信息暴露，统一日志输出，并根据部署方式配置端口、上下文路径、静态资源访问和反向代理。

文件位置：`src/main/resources/application-prod.yml`

```yaml
server:
  port: 8080
  servlet:
    # 如果部署在域名根路径下，可以不配置 context-path
    context-path: /

spring:
  freemarker:
    # 生产环境开启模板缓存，提高渲染性能
    cache: true
    # 模板加载路径
    template-loader-path: classpath:/templates/
    # 模板后缀
    suffix: .ftl
    # 模板编码
    charset: UTF-8
    # 响应内容类型
    content-type: text/html
    # 检查模板目录
    check-template-location: true
    settings:
      # 数字格式，避免展示为 1,000 这类形式
      number_format: 0.##

server:
  error:
    # 生产环境不暴露具体异常消息
    include-message: never
    # 生产环境不暴露参数绑定错误
    include-binding-errors: never
    # 生产环境不暴露异常类名
    include-exception: false
    # 生产环境不暴露堆栈信息
    include-stacktrace: never

logging:
  file:
    # 日志文件路径
    name: logs/freemarker-demo.log
  level:
    # 业务包日志级别
    io.github.atengk: info
    # Spring 框架日志级别
    org.springframework: warn
```

上面的 YAML 示例中存在两个 `server` 节点，实际项目中应合并为一个，推荐写法如下。

文件位置：`src/main/resources/application-prod.yml`

```yaml
server:
  port: 8080
  servlet:
    context-path: /
  error:
    # 生产环境不暴露具体异常消息
    include-message: never
    # 生产环境不暴露参数绑定错误
    include-binding-errors: never
    # 生产环境不暴露异常类名
    include-exception: false
    # 生产环境不暴露堆栈信息
    include-stacktrace: never

spring:
  freemarker:
    # 生产环境开启模板缓存，提高渲染性能
    cache: true
    template-loader-path: classpath:/templates/
    suffix: .ftl
    charset: UTF-8
    content-type: text/html
    check-template-location: true
    settings:
      number_format: 0.##

logging:
  file:
    name: logs/freemarker-demo.log
  level:
    io.github.atengk: info
    org.springframework: warn
```

生产环境建议通过 Nginx 反向代理访问 Spring Boot 服务。

文件位置：`/etc/nginx/conf.d/freemarker-demo.conf`

```nginx
server {
    listen 80;
    server_name example.com;

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;

        # 反向代理超时时间
        proxy_connect_timeout 60s;
        proxy_read_timeout 60s;
        proxy_send_timeout 60s;
    }
}
```

重新加载 Nginx：

```bash
nginx -t
nginx -s reload
```

`nginx -t` 用于检查配置文件语法是否正确。
`nginx -s reload` 用于平滑重新加载 Nginx 配置。

## 开发注意事项

本节用于总结 Spring Boot 3 FreeMarker 开发中最容易出现的问题，包括模板缓存、空值处理和 XSS 安全处理。实际开发中，这些问题会直接影响页面调试效率、运行稳定性和前端安全。

### 模板缓存处理

FreeMarker 模板缓存用于减少模板重复解析，提高页面渲染性能。开发环境和生产环境对缓存的要求不同：开发环境需要快速看到模板修改结果，生产环境需要稳定和性能。

开发环境建议关闭缓存：

```yaml
spring:
  freemarker:
    # 开发环境关闭缓存
    cache: false
```

生产环境建议开启缓存：

```yaml
spring:
  freemarker:
    # 生产环境开启缓存
    cache: true
```

如果开发阶段修改 `.ftl` 文件后页面没有变化，可以按以下顺序排查：

| 排查项          | 处理方式                                 |
| --------------- | ---------------------------------------- |
| FreeMarker 缓存 | 确认 `spring.freemarker.cache=false`     |
| 浏览器缓存      | 强制刷新页面                             |
| 编译目录        | 检查 `target/classes/templates` 是否更新 |
| IDE 配置        | 确认资源文件是否自动复制                 |
| 运行模块        | 确认启动的是当前模块                     |
| 模板路径        | 检查 Controller 返回值与模板路径是否一致 |

可以重新编译项目确认资源复制情况：

```bash
mvn clean compile
```

然后检查模板是否进入编译目录：

```text
target/classes/templates/user/list.ftl
```

### 空值处理

FreeMarker 对空值较敏感。模板中直接访问不存在的变量或空对象属性时，可能导致模板渲染异常。开发时应对可能为空的数据进行默认值处理或存在性判断。

不推荐写法：

```ftl
<p>昵称：${user.nickname}</p>
```

推荐写法：

```ftl
<p>昵称：${user.nickname!'匿名用户'}</p>
```

对象可能为空时，推荐先判断对象是否存在：

```ftl
<#if user??>
    <p>用户名：${user.username!'未设置'}</p>
    <p>邮箱：${user.email!'未设置'}</p>
<#else>
    <p>用户不存在</p>
</#if>
```

集合可能为空时，推荐同时判断变量存在和集合长度：

```ftl
<#if userList?? && userList?size gt 0>
    <#list userList as user>
        <p>${user.username!'未设置'}</p>
    </#list>
<#else>
    <p>暂无用户数据</p>
</#if>
```

常用空值处理方式如下：

| 写法                             | 说明                 |
| -------------------------------- | -------------------- |
| `${name!'默认值'}`               | 变量为空时使用默认值 |
| `<#if user??>`                   | 判断对象是否存在     |
| `<#if list?? && list?size gt 0>` | 判断集合是否有数据   |
| `${user.nickname!''}`            | 空值时输出空字符串   |

对于页面展示字段，建议后端尽量提供完整 VO 数据，模板侧再做兜底处理。这样可以减少页面异常，也能让展示逻辑更稳定。

### XSS 安全处理

XSS 是指攻击者将恶意脚本注入页面，当其他用户访问页面时脚本被浏览器执行。FreeMarker 页面如果直接输出用户输入内容，可能存在 XSS 风险，例如昵称、备注、评论、简介等字段。

不推荐直接输出用户输入内容：

```ftl
<p>用户昵称：${user.nickname}</p>
<p>个人简介：${user.description}</p>
```

推荐使用 HTML 转义：

```ftl
<p>用户昵称：${user.nickname!?html}</p>
<p>个人简介：${user.description!?html}</p>
```

在 JavaScript 字符串中输出变量时，推荐使用 `?js_string`：

```ftl
<button onclick="alert('${user.username!?js_string}')">查看用户名</button>
```

在 URL 参数中输出变量时，推荐使用 `?url`：

```ftl
<a href="${request.contextPath}/search?keyword=${keyword!?url}">搜索</a>
```

常见输出场景和处理方式如下：

| 输出场景          | 推荐处理                       |
| ----------------- | ------------------------------ |
| HTML 文本内容     | `?html`                        |
| HTML 属性值       | `?html`                        |
| JavaScript 字符串 | `?js_string`                   |
| URL 参数          | `?url`                         |
| 普通后端状态值    | 可直接输出，但仍建议兜底默认值 |

安全示例：

```ftl
<p>昵称：${user.nickname!'匿名用户'?html}</p>

<input type="text" name="nickname" value="${user.nickname!''?html}">

<script>
    const username = '${user.username!''?js_string}';
</script>

<a href="${request.contextPath}/search?keyword=${keyword!''?url}">搜索</a>
```

后端也应对用户输入进行校验和清洗。页面转义是最后一道展示层防护，不应替代后端参数校验、权限控制和内容安全策略。

生产环境还可以通过响应头增强安全性，例如配置 CSP、X-Content-Type-Options、X-Frame-Options 等。如果项目接入 Spring Security，可以统一配置安全响应头。