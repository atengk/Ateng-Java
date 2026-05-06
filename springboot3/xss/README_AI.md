# Spring Boot XSS 防护

## 功能概述

本章节用于说明 Spring Boot 3 项目中引入 XSS 防护的背景、目标和适用范围。XSS 防护主要作用于请求入口、富文本处理和响应输出环节，用于降低恶意脚本注入、页面篡改、用户信息窃取等安全风险。

### XSS 攻击说明

XSS，全称 Cross Site Scripting，即跨站脚本攻击。攻击者通过请求参数、表单字段、JSON 请求体、富文本内容等入口注入恶意脚本，当这些内容被页面渲染或被浏览器执行时，可能导致用户 Cookie 泄露、页面内容被篡改、接口被冒用调用等问题。

在 Spring Boot 接口系统中，常见的 XSS 输入位置包括搜索关键字、用户昵称、备注、标题、评论、公告内容、富文本正文、地址信息等由用户提交的字段。

常见攻击示例如下：

| 攻击类型      | 说明                             | 示例                                     |
| ------------- | -------------------------------- | ---------------------------------------- |
| 脚本标签注入  | 直接提交 `<script>` 标签         | `<script>alert(1)</script>`              |
| HTML 标签注入 | 通过非法 HTML 影响页面结构       | `<iframe src="xxx"></iframe>`            |
| 事件属性注入  | 借助 HTML 事件触发脚本           | `<img src=x onerror=alert(1)>`           |
| 协议注入      | 通过危险协议执行脚本             | `<a href="javascript:alert(1)">点击</a>` |
| 富文本注入    | 在富文本内容中混入危险标签或属性 | `<p>正文</p><script>alert(1)</script>`   |

XSS 防护不能只依赖前端处理。前端可以减少渲染风险，但后端仍需要在数据入口处进行统一过滤，避免恶意内容进入业务系统、数据库或后续展示流程。

### 防护目标

Spring Boot XSS 防护的目标是对用户输入内容进行统一、安全、可配置的处理，在不影响正常业务数据的前提下，降低脚本注入风险。

核心目标包括：

| 目标            | 说明                                                         |
| --------------- | ------------------------------------------------------------ |
| 请求参数过滤    | 对 Query 参数、表单参数中的危险内容进行转义或清理            |
| JSON 请求体过滤 | 对 `application/json` 请求中的字符串字段进行 XSS 处理        |
| 富文本安全处理  | 对富文本内容使用白名单策略，保留安全标签，移除危险标签和属性 |
| 路径白名单      | 对文件上传、第三方回调、签名校验等接口支持排除过滤           |
| 响应安全控制    | 通过响应头降低浏览器侧脚本执行和内容嗅探风险                 |
| 配置化管理      | 支持通过配置文件控制开关、排除路径、允许标签和过滤策略       |

系统防护设计应遵循以下原则：

1. 普通文本字段优先使用 HTML 转义，避免浏览器解析为可执行内容。
2. 富文本字段不能简单转义，应使用白名单清理策略。
3. 文件上传、加密报文、签名验签接口应支持排除过滤。
4. 防护逻辑应统一封装，避免在 Controller 或 Service 中重复处理。
5. XSS 防护应与参数校验、权限控制、响应头安全策略配合使用。

### 适用场景

该方案适用于基于 Spring Boot 3 的后端接口项目，尤其适合前后端分离系统、管理后台、内容管理系统、用户中心、评论系统、公告系统等包含用户输入和内容展示的业务系统。

适用场景如下：

| 场景           | 是否适用   | 处理方式                       |
| -------------- | ---------- | ------------------------------ |
| GET Query 参数 | 适用       | 通过 RequestWrapper 统一过滤   |
| POST 表单参数  | 适用       | 通过参数读取方法统一过滤       |
| JSON 请求体    | 适用       | 缓存 Body 后进行字段级过滤     |
| 富文本内容     | 适用       | 使用 HTML 白名单清理           |
| 文件上传接口   | 建议排除   | 避免影响二进制流和文件内容     |
| 第三方回调接口 | 视情况排除 | 避免影响签名验签               |
| 内部接口       | 视情况启用 | 根据接口暴露范围和数据来源决定 |
| 页面响应       | 适用       | 配合响应安全头和模板转义       |

对于前后端分离项目，后端负责输入过滤和响应安全控制，前端仍需要避免直接使用 `innerHTML`、`v-html` 等高风险渲染方式。如果业务必须渲染富文本，前后端都应保持一致的白名单策略。

## 技术方案

本章节说明 Spring Boot 3 中 XSS 防护的整体实现思路。推荐通过 Servlet Filter 作为统一入口，对请求参数和请求体进行封装处理，并结合富文本白名单、路径排除和响应安全头形成完整防护链路。

整体处理流程如下：

1. 请求进入 Spring Boot 应用。
2. XSS Filter 判断当前请求是否需要过滤。
3. 如果命中排除路径，直接放行。
4. 如果是普通参数请求，对参数值进行过滤或转义。
5. 如果是 JSON 请求，缓存请求体并对字符串字段进行过滤。
6. 如果字段属于富文本字段，按白名单策略清理。
7. 请求继续进入 Controller 和业务逻辑。
8. 响应返回时统一添加安全响应头。

### 请求参数过滤

请求参数过滤主要用于处理 `GET` 请求参数和 `application/x-www-form-urlencoded` 表单参数。该类参数通常通过 `request.getParameter()`、`request.getParameterValues()` 或 Spring MVC 参数绑定进入 Controller。

推荐使用自定义 `HttpServletRequestWrapper` 包装原始请求，并重写参数读取方法，在参数被业务代码读取前完成统一过滤。

主要处理方法如下：

| 方法                              | 作用                       |
| --------------------------------- | -------------------------- |
| `getParameter(String name)`       | 处理单个参数值             |
| `getParameterValues(String name)` | 处理多个同名参数值         |
| `getParameterMap()`               | 处理完整参数集合           |
| `getHeader(String name)`          | 可选处理请求头中的危险内容 |

普通参数建议使用 HTML 转义策略。例如：

| 原始内容                       | 处理结果                       |
| ------------------------------ | ------------------------------ |
| `<script>alert(1)</script>`    | `<script>alert(1)</script>`    |
| `<img src=x onerror=alert(1)>` | `<img src=x onerror=alert(1)>` |
| `正常关键字`                   | `正常关键字`                   |

参数过滤时需要注意以下规则：

1. 不建议直接删除所有特殊字符，否则可能影响正常业务输入。
2. 对分页、排序、状态等固定格式字段，应配合参数校验和枚举白名单。
3. 对签名字段、加密字段、回调参数等不应修改原文的字段，需要通过排除路径或排除字段处理。
4. 对普通文本内容，优先使用转义而不是删除，便于保留用户原始输入语义。
5. 过滤逻辑应保持幂等，避免同一字段被重复转义。

### JSON 请求体过滤

JSON 请求体过滤用于处理 `Content-Type` 为 `application/json` 的请求，例如新增、编辑、批量保存等接口。该类请求通常通过 `@RequestBody` 绑定为 DTO、Map 或实体对象。

Servlet 请求体默认只能读取一次。如果在 Filter 中直接读取 Body，后续 Controller 中的 `@RequestBody` 将无法再次读取。因此需要通过自定义 RequestWrapper 缓存请求体，实现 Body 可重复读取。

JSON 请求体过滤流程如下：

1. 判断请求是否为 JSON 请求。
2. 读取原始请求体并缓存为字节数组。
3. 将请求体内容解析为 JSON 对象或 JSON 数组。
4. 递归遍历 JSON 内容。
5. 仅对字符串类型字段进行 XSS 过滤。
6. 对富文本字段使用白名单清理策略。
7. 将处理后的 JSON 内容重新写入包装请求。
8. 后续 Controller 正常读取处理后的 Body。

JSON 请求示例：

```json
{
  "title": "<script>alert(1)</script>",
  "remark": "正常备注",
  "content": "<p>公告内容</p><img src=x onerror=alert(1)>"
}
```

普通字段处理后示例：

```json
{
  "title": "&lt;script&gt;alert(1)&lt;/script&gt;",
  "remark": "正常备注",
  "content": "<p>公告内容</p><img src=x onerror=alert(1)>"
}
```

如果 `content` 被配置为富文本字段，则不应使用普通转义，而应进入富文本白名单处理流程。

JSON 过滤需要注意以下事项：

1. 只处理字符串字段，不应修改数字、布尔值、数组结构和对象结构。
2. 需要同时支持 JSON Object 和 JSON Array。
3. 空 Body、非法 JSON、非 JSON 请求应直接放行或交由统一异常处理。
4. 大请求体应设置大小限制，避免 Filter 中缓存超大 Body 造成内存压力。
5. 对加密传输、签名验签和第三方回调接口，应配置为排除路径。
6. JSON 字段过滤应支持递归处理，避免嵌套对象中的危险内容漏处理。

### 富文本内容处理

富文本内容通常包含合法 HTML 标签，例如段落、换行、加粗、列表、图片、链接、表格等。如果直接使用 HTML 转义，会导致富文本无法正常展示。因此富文本内容应采用白名单清理策略，而不是普通文本转义策略。

富文本白名单处理的核心思路是：只允许业务需要的安全标签、属性和协议，其余内容全部移除。

建议允许的基础标签如下：

| 类型     | 标签                                        |
| -------- | ------------------------------------------- |
| 文本结构 | `p`、`br`、`span`、`div`                    |
| 文本样式 | `strong`、`b`、`em`、`i`、`u`               |
| 列表     | `ul`、`ol`、`li`                            |
| 引用     | `blockquote`                                |
| 链接     | `a`                                         |
| 图片     | `img`                                       |
| 表格     | `table`、`thead`、`tbody`、`tr`、`th`、`td` |

建议允许的安全属性如下：

| 标签     | 允许属性                                    |
| -------- | ------------------------------------------- |
| `a`      | `href`、`title`、`target`、`rel`            |
| `img`    | `src`、`alt`、`title`、`width`、`height`    |
| `table`  | `border`、`cellpadding`、`cellspacing`      |
| 通用标签 | `class`、`style` 是否允许应根据业务严格评估 |

必须移除的危险内容包括：

| 类型       | 示例                                           |
| ---------- | ---------------------------------------------- |
| 危险标签   | `script`、`iframe`、`object`、`embed`、`style` |
| 事件属性   | `onclick`、`onerror`、`onload`、`onmouseover`  |
| 危险协议   | `javascript:`、`vbscript:`、`data:`            |
| 非可信资源 | 非白名单域名下的图片、脚本、iframe 地址        |

富文本清理示例：

处理前：

```html
<p>系统公告</p>
<img src="x" onerror="alert(1)">
<a href="javascript:alert(1)">点击查看</a>
<script>alert(1)</script>
```

处理后：

```html
<p>系统公告</p>
<img src="x">
<a>点击查看</a>
```

富文本处理建议如下：

1. 富文本内容建议在入库前完成清理，避免危险内容长期存储。
2. 展示富文本时，前端仍应谨慎使用 `v-html` 或 `innerHTML`。
3. 图片地址建议限制为可信对象存储域名或系统静态资源域名。
4. 链接建议自动补充 `rel="noopener noreferrer"`。
5. 如果业务需要 iframe、视频嵌入或第三方内容，应额外增加可信域名白名单。
6. 富文本字段应通过配置维护，例如 `content`、`description`、`noticeContent` 等字段。

### 响应内容安全控制

响应内容安全控制用于降低浏览器侧的内容解析和脚本执行风险。请求输入过滤可以减少恶意内容进入系统，响应安全头则可以约束浏览器如何加载、解析和展示页面资源。

推荐在统一 Filter、Interceptor、网关或 Nginx 中添加安全响应头。

常用响应头如下：

| 响应头                    | 推荐值                            | 作用                                     |
| ------------------------- | --------------------------------- | ---------------------------------------- |
| `X-Content-Type-Options`  | `nosniff`                         | 禁止浏览器进行 MIME 类型嗅探             |
| `X-Frame-Options`         | `DENY` 或 `SAMEORIGIN`            | 限制页面被 iframe 嵌入                   |
| `Referrer-Policy`         | `strict-origin-when-cross-origin` | 控制 Referer 信息传递                    |
| `Content-Security-Policy` | 按业务配置                        | 限制脚本、样式、图片等资源来源           |
| `X-XSS-Protection`        | `0`                               | 关闭旧版浏览器 XSS Auditor，避免兼容问题 |

基础 CSP 示例：

```http
Content-Security-Policy: default-src 'self'; script-src 'self'; object-src 'none'; base-uri 'self'; frame-ancestors 'none'
```

如果系统需要加载 CDN、对象存储图片或第三方静态资源，可以按实际域名扩展：

```http
Content-Security-Policy: default-src 'self'; script-src 'self' https://cdn.example.com; img-src 'self' data: https://oss.example.com; object-src 'none'; base-uri 'self'
```

响应安全控制需要注意以下事项：

1. CSP 配置过严可能导致前端脚本、样式、字体、图片无法加载，需要结合实际资源来源调整。
2. 不建议长期使用 `'unsafe-inline'` 和 `'unsafe-eval'`，否则会明显削弱 CSP 防护效果。
3. 响应安全头不能替代请求参数过滤和富文本清理。
4. 下载接口、文件预览接口、第三方嵌入页面应单独评估响应头策略。
5. 前后端分离项目中，CSP 可以由前端服务、Nginx 或网关统一配置，后端接口可重点处理输入过滤和基础安全头。

通过请求参数过滤、JSON 请求体过滤、富文本白名单处理和响应安全控制，可以形成较完整的 Spring Boot XSS 防护方案。该方案既能覆盖常规接口输入，也能兼容富文本业务和特殊接口放行需求。



## 环境与依赖

本章节说明 Spring Boot 3 XSS 防护功能所需的基础运行环境、核心依赖和组件选型。该方案基于 Servlet Filter 实现请求入口统一拦截，使用 Hutool 进行字符串、集合、JSON 和 HTML 转义处理，使用 Jsoup 对富文本 HTML 进行白名单清理。

### Spring Boot 3 基础环境

Spring Boot 3 默认基于 Jakarta EE 规范，Servlet 相关类包名已经从 `javax.servlet` 迁移到 `jakarta.servlet`。因此在实现 Filter、RequestWrapper、FilterRegistrationBean 等组件时，需要使用 Jakarta 包路径。

建议基础环境如下：

| 项目        | 推荐版本      | 说明                                  |
| ----------- | ------------- | ------------------------------------- |
| JDK         | 17 或以上     | Spring Boot 3 最低要求 JDK 17         |
| Spring Boot | 3.x           | 使用 Jakarta Servlet API              |
| Maven       | 3.8+          | 用于依赖管理和项目构建                |
| Hutool      | 5.8.x         | 用于字符串、集合、JSON、HTML 工具处理 |
| Jsoup       | 1.17.x 或以上 | 用于富文本 HTML 白名单清理            |

Maven 依赖配置如下。

文件位置：`pom.xml`

```xml
<dependencies>
    <!-- Spring Boot Web 基础依赖，包含 MVC、内置 Tomcat、Jakarta Servlet API -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Spring Boot 配置属性提示，用于 application.yml 配置提示 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-configuration-processor</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- Hutool 工具包，用于字符串、集合、JSON、HTML 转义等处理 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.8.35</version>
    </dependency>

    <!-- Jsoup HTML 清理组件，用于富文本白名单过滤 -->
    <dependency>
        <groupId>org.jsoup</groupId>
        <artifactId>jsoup</artifactId>
        <version>1.17.2</version>
    </dependency>

    <!-- Lombok，减少配置类和日志代码样板 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

如果项目已经引入 `spring-boot-starter-web`，无需重复引入 Servlet API。Spring Boot 3 中编写 Filter 时直接使用 `jakarta.servlet.Filter`、`jakarta.servlet.http.HttpServletRequest`、`jakarta.servlet.http.HttpServletResponse` 即可。

### Servlet Filter 配置

XSS 防护推荐使用 Servlet Filter 实现，因为 Filter 位于 Spring MVC 参数绑定之前，可以在请求进入 Controller 前统一处理参数和请求体。

Filter 处理顺序建议靠前，但不应影响跨域、认证、日志链路等已有组件。通常可以将 XSS Filter 放在字符编码过滤器之后、业务 Controller 之前。

推荐配置项如下：

| 配置项                       | 说明                             |
| ---------------------------- | -------------------------------- |
| `xss.enabled`                | 是否启用 XSS 防护                |
| `xss.url-patterns`           | Filter 拦截路径，默认 `/*`       |
| `xss.excludes`               | 排除路径，例如文件上传、回调接口 |
| `xss.rich-text-fields`       | 富文本字段名                     |
| `xss.max-body-size`          | 请求体最大处理大小               |
| `xss.enable-header-filter`   | 是否过滤请求头                   |
| `xss.enable-response-header` | 是否添加响应安全头               |

配置示例如下。

文件位置：`src/main/resources/application.yml`

```yaml
xss:
  # 是否启用 XSS 防护
  enabled: true

  # Filter 拦截路径，通常保持默认即可
  url-patterns:
    - /*

  # 排除路径，文件上传、三方回调、签名验签接口建议排除
  excludes:
    - /actuator/**
    - /upload/**
    - /file/**
    - /callback/**
    - /webhook/**

  # 富文本字段，命中这些字段时使用 HTML 白名单清理，而不是普通转义
  rich-text-fields:
    - content
    - description
    - noticeContent
    - articleContent

  # 请求体最大处理大小，超过后建议直接放行或拒绝，避免内存压力
  max-body-size: 1048576

  # 是否过滤 Header，默认关闭，避免影响 Token、签名等字段
  enable-header-filter: false

  # 是否添加响应安全头
  enable-response-header: true
```

该配置用于控制 XSS Filter 的启用、排除路径、富文本字段和请求体处理限制。核心实现中会通过 `@ConfigurationProperties` 将配置映射为 Java 配置对象。

### Hutool 工具类使用

本方案中 Hutool 主要用于字符串判断、集合处理、JSON 解析和 HTML 转义，避免重复编写基础工具逻辑。

常用 Hutool 工具类如下：

| 工具类       | 用途                             |
| ------------ | -------------------------------- |
| `StrUtil`    | 字符串判空、格式判断、前后缀处理 |
| `CollUtil`   | 集合判空、集合遍历               |
| `ArrayUtil`  | 数组判空、数组转换               |
| `HtmlUtil`   | HTML 字符转义                    |
| `JSONUtil`   | JSON 字符串解析和判断            |
| `JSONObject` | JSON 对象处理                    |
| `JSONArray`  | JSON 数组处理                    |

普通文本字段建议使用 `HtmlUtil.escape()` 进行 HTML 转义。该方式不会删除用户输入内容，而是将危险字符转换为 HTML 实体，避免浏览器将其解析为可执行脚本。

示例：

```java
String value = "<script>alert(1)</script>";
String safeValue = HtmlUtil.escape(value);
```

处理结果：

```text
&lt;script&gt;alert(1)&lt;/script&gt;
```

对于 JSON 请求体，推荐使用 Hutool JSON 工具解析后递归处理字符串字段；对于富文本字段，则交给 Jsoup 按白名单清理。

### HTML 清理组件选择

HTML 清理组件主要用于富文本场景。普通文本字段使用 HTML 转义即可，但富文本字段需要保留部分合法标签，因此需要使用支持白名单策略的 HTML 清理组件。

推荐使用 Jsoup，原因如下：

| 维度       | 说明                                 |
| ---------- | ------------------------------------ |
| 白名单能力 | 支持定义允许标签、属性和协议         |
| 使用成本   | API 简单，适合后端接口清理富文本     |
| 维护情况   | 使用广泛，适合常规 HTML 清理场景     |
| 兼容性     | 可直接用于 Spring Boot 3 项目        |
| 安全策略   | 可以移除危险标签、危险属性和危险协议 |

富文本清理推荐策略如下：

1. 使用 `Safelist.relaxed()` 作为基础白名单。
2. 根据业务需要追加表格、图片、链接等标签属性。
3. 移除 `script`、`iframe`、`object`、`embed` 等危险标签。
4. 限制 `a.href` 和 `img.src` 的协议。
5. 对链接自动补充 `rel="noopener noreferrer"`。

普通文本和富文本的处理方式应明确区分：

| 内容类型      | 推荐处理方式                          |
| ------------- | ------------------------------------- |
| 普通文本      | 使用 Hutool `HtmlUtil.escape()` 转义  |
| 富文本内容    | 使用 Jsoup `Jsoup.clean()` 白名单清理 |
| JSON 数值字段 | 不处理                                |
| JSON 布尔字段 | 不处理                                |
| 文件流        | 不处理，建议排除路径                  |

## 核心实现

本章节给出 XSS 防护的核心代码实现，包括配置属性、过滤器、RequestWrapper、参数转义、JSON 请求体重复读取和白名单路径判断。示例代码基于 Spring Boot 3、Jakarta Servlet、Hutool 和 Jsoup 实现。

建议文件结构如下：

```text
src/main/java/io/github/atengk/xss
├── config
│   ├── XssFilterConfig.java
│   └── XssProperties.java
├── filter
│   └── XssFilter.java
├── support
│   ├── XssHttpServletRequestWrapper.java
│   └── XssUtils.java
```

### XSS 过滤器定义

XSS 过滤器用于拦截请求，判断是否启用防护、是否命中排除路径、是否需要包装请求对象，并在响应返回前添加基础安全响应头。

下面代码定义 XSS 配置属性，用于承接 `application.yml` 中的配置。

文件位置：`src/main/java/io/github/atengk/xss/config/XssProperties.java`

```java
package io.github.atengk.xss.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * XSS 防护配置属性
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@ConfigurationProperties(prefix = "xss")
public class XssProperties {

    /**
     * 是否启用 XSS 防护
     */
    private Boolean enabled = true;

    /**
     * Filter 拦截路径
     */
    private List<String> urlPatterns = new ArrayList<>(List.of("/*"));

    /**
     * 排除路径
     */
    private List<String> excludes = new ArrayList<>();

    /**
     * 富文本字段名称
     */
    private List<String> richTextFields = new ArrayList<>();

    /**
     * 请求体最大处理大小，默认 1MB
     */
    private Integer maxBodySize = 1024 * 1024;

    /**
     * 是否过滤请求头
     */
    private Boolean enableHeaderFilter = false;

    /**
     * 是否添加响应安全头
     */
    private Boolean enableResponseHeader = true;
}
```

下面代码注册 XSS Filter，并将配置对象注入到过滤器中。

文件位置：`src/main/java/io/github/atengk/xss/config/XssFilterConfig.java`

```java
package io.github.atengk.xss.config;

import io.github.atengk.xss.filter.XssFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * XSS 过滤器配置
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(XssProperties.class)
public class XssFilterConfig {

    private final XssProperties xssProperties;

    /**
     * 注册 XSS 过滤器
     *
     * @return Filter 注册对象
     */
    @Bean
    @ConditionalOnProperty(prefix = "xss", name = "enabled", havingValue = "true", matchIfMissing = true)
    public FilterRegistrationBean<XssFilter> xssFilterRegistrationBean() {
        FilterRegistrationBean<XssFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new XssFilter(xssProperties));
        registrationBean.setUrlPatterns(getUrlPatterns());
        registrationBean.setName("xssFilter");
        registrationBean.setOrder(1);
        return registrationBean;
    }

    /**
     * 获取拦截路径
     *
     * @return 拦截路径集合
     */
    private List<String> getUrlPatterns() {
        if (xssProperties.getUrlPatterns() == null || xssProperties.getUrlPatterns().isEmpty()) {
            return List.of("/*");
        }
        return xssProperties.getUrlPatterns();
    }
}
```

下面代码定义核心 XSS Filter，实现路径排除、请求包装和响应头处理。

文件位置：`src/main/java/io/github/atengk/xss/filter/XssFilter.java`

```java
package io.github.atengk.xss.filter;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.xss.config.XssProperties;
import io.github.atengk.xss.support.XssHttpServletRequestWrapper;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.AntPathMatcher;

import java.io.IOException;

/**
 * XSS 防护过滤器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@RequiredArgsConstructor
public class XssFilter implements Filter {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final XssProperties xssProperties;

    /**
     * 执行 XSS 过滤
     *
     * @param request 原始请求
     * @param response 原始响应
     * @param chain 过滤器链
     * @throws IOException IO 异常
     * @throws ServletException Servlet 异常
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (!(request instanceof HttpServletRequest httpRequest)
                || !(response instanceof HttpServletResponse httpResponse)) {
            chain.doFilter(request, response);
            return;
        }

        if (Boolean.TRUE.equals(xssProperties.getEnableResponseHeader())) {
            addSecurityHeaders(httpResponse);
        }

        String requestUri = httpRequest.getRequestURI();
        if (isExcludeUri(requestUri)) {
            log.debug("当前请求命中 XSS 排除路径，直接放行：{}", requestUri);
            chain.doFilter(request, response);
            return;
        }

        XssHttpServletRequestWrapper requestWrapper = new XssHttpServletRequestWrapper(httpRequest, xssProperties);
        chain.doFilter(requestWrapper, response);
    }

    /**
     * 判断请求路径是否排除
     *
     * @param requestUri 请求路径
     * @return 是否排除
     */
    private boolean isExcludeUri(String requestUri) {
        if (StrUtil.isBlank(requestUri) || CollUtil.isEmpty(xssProperties.getExcludes())) {
            return false;
        }
        return xssProperties.getExcludes().stream()
                .filter(StrUtil::isNotBlank)
                .anyMatch(pattern -> PATH_MATCHER.match(pattern, requestUri));
    }

    /**
     * 添加响应安全头
     *
     * @param response 响应对象
     */
    private void addSecurityHeaders(HttpServletResponse response) {
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "SAMEORIGIN");
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        response.setHeader("X-XSS-Protection", "0");
    }
}
```

### RequestWrapper 封装

RequestWrapper 用于统一处理请求参数、请求头和 JSON 请求体。由于 Servlet 请求体只能读取一次，因此包装类需要在构造阶段缓存请求体，并重写 `getInputStream()` 和 `getReader()` 方法，保证后续 `@RequestBody` 可以正常读取。

下面代码实现请求包装、参数转义和请求体缓存读取。

文件位置：`src/main/java/io/github/atengk/xss/support/XssHttpServletRequestWrapper.java`

```java
package io.github.atengk.xss.support;

import cn.hutool.core.array.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.xss.config.XssProperties;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * XSS 请求包装器
 *
 * @author Ateng
 * @since 2026-05-06
 */
public class XssHttpServletRequestWrapper extends HttpServletRequestWrapper {

    private final XssProperties xssProperties;

    private final byte[] body;

    /**
     * 构造 XSS 请求包装器
     *
     * @param request 原始请求
     * @param xssProperties XSS 配置
     * @throws IOException IO 异常
     */
    public XssHttpServletRequestWrapper(HttpServletRequest request, XssProperties xssProperties) throws IOException {
        super(request);
        this.xssProperties = xssProperties;
        this.body = initBody(request);
    }

    /**
     * 获取单个请求参数
     *
     * @param name 参数名
     * @return 处理后的参数值
     */
    @Override
    public String getParameter(String name) {
        String value = super.getParameter(name);
        return XssUtils.cleanText(value);
    }

    /**
     * 获取多个同名请求参数
     *
     * @param name 参数名
     * @return 处理后的参数值数组
     */
    @Override
    public String[] getParameterValues(String name) {
        String[] values = super.getParameterValues(name);
        if (ArrayUtil.isEmpty(values)) {
            return values;
        }

        String[] cleanValues = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            cleanValues[i] = XssUtils.cleanText(values[i]);
        }
        return cleanValues;
    }

    /**
     * 获取请求参数 Map
     *
     * @return 处理后的请求参数 Map
     */
    @Override
    public Map<String, String[]> getParameterMap() {
        Map<String, String[]> parameterMap = super.getParameterMap();
        Map<String, String[]> cleanMap = new LinkedHashMap<>(parameterMap.size());

        parameterMap.forEach((key, values) -> {
            if (ArrayUtil.isEmpty(values)) {
                cleanMap.put(key, values);
                return;
            }

            String[] cleanValues = new String[values.length];
            for (int i = 0; i < values.length; i++) {
                cleanValues[i] = XssUtils.cleanText(values[i]);
            }
            cleanMap.put(key, cleanValues);
        });

        return cleanMap;
    }

    /**
     * 获取请求头
     *
     * @param name 请求头名称
     * @return 请求头内容
     */
    @Override
    public String getHeader(String name) {
        String value = super.getHeader(name);
        if (Boolean.TRUE.equals(xssProperties.getEnableHeaderFilter())) {
            return XssUtils.cleanText(value);
        }
        return value;
    }

    /**
     * 获取可重复读取的输入流
     *
     * @return Servlet 输入流
     */
    @Override
    public ServletInputStream getInputStream() {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(body);

        return new ServletInputStream() {
            @Override
            public int read() {
                return byteArrayInputStream.read();
            }

            @Override
            public boolean isFinished() {
                return byteArrayInputStream.available() <= 0;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener listener) {
                // 当前包装器基于内存字节数组读取，不需要异步监听
            }
        };
    }

    /**
     * 获取可重复读取的字符流
     *
     * @return BufferedReader
     */
    @Override
    public BufferedReader getReader() {
        return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
    }

    /**
     * 初始化请求体
     *
     * @param request 原始请求
     * @return 处理后的请求体字节数组
     * @throws IOException IO 异常
     */
    private byte[] initBody(HttpServletRequest request) throws IOException {
        if (!XssUtils.isJsonRequest(request)) {
            return new byte[0];
        }

        int contentLength = request.getContentLength();
        Integer maxBodySize = xssProperties.getMaxBodySize();
        if (contentLength > 0 && maxBodySize != null && contentLength > maxBodySize) {
            return request.getInputStream().readAllBytes();
        }

        String bodyText = new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        if (StrUtil.isBlank(bodyText)) {
            return new byte[0];
        }

        String cleanBody = XssUtils.cleanJsonBody(bodyText, xssProperties.getRichTextFields());
        return cleanBody.getBytes(StandardCharsets.UTF_8);
    }
}
```

### 参数转义处理

参数转义处理由工具类统一完成。普通文本字段使用 Hutool `HtmlUtil.escape()` 进行 HTML 转义；富文本字段使用 Jsoup 白名单清理；JSON 请求体通过递归方式处理对象和数组中的字符串字段。

下面代码提供 XSS 文本处理、JSON 处理和富文本清理能力。

文件位置：`src/main/java/io/github/atengk/xss/support/XssUtils.java`

```java
package io.github.atengk.xss.support;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HtmlUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONConfig;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

import java.util.List;
import java.util.Map;

/**
 * XSS 防护工具类
 *
 * @author Ateng
 * @since 2026-05-06
 */
public final class XssUtils {

    private static final String CONTENT_TYPE_JSON = "application/json";

    private XssUtils() {
    }

    /**
     * 判断是否为 JSON 请求
     *
     * @param request 请求对象
     * @return 是否 JSON 请求
     */
    public static boolean isJsonRequest(HttpServletRequest request) {
        String contentType = request.getContentType();
        return StrUtil.isNotBlank(contentType)
                && StrUtil.containsIgnoreCase(contentType, CONTENT_TYPE_JSON);
    }

    /**
     * 清理普通文本
     *
     * @param value 原始文本
     * @return 转义后的安全文本
     */
    public static String cleanText(String value) {
        if (StrUtil.isBlank(value)) {
            return value;
        }
        return HtmlUtil.escape(value);
    }

    /**
     * 清理 JSON 请求体
     *
     * @param bodyText 原始 JSON 字符串
     * @param richTextFields 富文本字段名称
     * @return 处理后的 JSON 字符串
     */
    public static String cleanJsonBody(String bodyText, List<String> richTextFields) {
        if (StrUtil.isBlank(bodyText)) {
            return bodyText;
        }

        if (!JSONUtil.isTypeJSON(bodyText)) {
            return bodyText;
        }

        Object json = JSONUtil.parse(bodyText, JSONConfig.create().setIgnoreNullValue(false));
        Object cleanJson = cleanJsonValue(null, json, richTextFields);
        return JSONUtil.toJsonStr(cleanJson);
    }

    /**
     * 递归清理 JSON 值
     *
     * @param fieldName 字段名
     * @param value 字段值
     * @param richTextFields 富文本字段名称
     * @return 处理后的字段值
     */
    private static Object cleanJsonValue(String fieldName, Object value, List<String> richTextFields) {
        if (value instanceof JSONObject jsonObject) {
            for (Map.Entry<String, Object> entry : jsonObject.entrySet()) {
                entry.setValue(cleanJsonValue(entry.getKey(), entry.getValue(), richTextFields));
            }
            return jsonObject;
        }

        if (value instanceof JSONArray jsonArray) {
            for (int i = 0; i < jsonArray.size(); i++) {
                jsonArray.set(i, cleanJsonValue(fieldName, jsonArray.get(i), richTextFields));
            }
            return jsonArray;
        }

        if (value instanceof CharSequence charSequence) {
            String text = charSequence.toString();
            if (isRichTextField(fieldName, richTextFields)) {
                return cleanRichText(text);
            }
            return cleanText(text);
        }

        return value;
    }

    /**
     * 判断是否为富文本字段
     *
     * @param fieldName 字段名
     * @param richTextFields 富文本字段名称
     * @return 是否富文本字段
     */
    private static boolean isRichTextField(String fieldName, List<String> richTextFields) {
        if (StrUtil.isBlank(fieldName) || CollUtil.isEmpty(richTextFields)) {
            return false;
        }
        return richTextFields.stream()
                .filter(StrUtil::isNotBlank)
                .anyMatch(item -> CharSequenceUtil.equalsIgnoreCase(item, fieldName));
    }

    /**
     * 清理富文本
     *
     * @param html 原始 HTML
     * @return 清理后的 HTML
     */
    public static String cleanRichText(String html) {
        if (StrUtil.isBlank(html)) {
            return html;
        }

        Safelist safelist = Safelist.relaxed()
                .addTags("table", "thead", "tbody", "tr", "th", "td")
                .addAttributes("table", "border", "cellpadding", "cellspacing")
                .addAttributes("a", "target", "rel")
                .addProtocols("a", "href", "http", "https", "mailto")
                .addProtocols("img", "src", "http", "https");

        return Jsoup.clean(html, safelist);
    }
}
```

参数转义处理需要注意以下事项：

1. 普通文本使用转义，保留原始语义，避免浏览器解析为标签。
2. 富文本使用清理，保留允许的 HTML 标签。
3. JSON 中只处理字符串字段，不处理数字、布尔值和空值。
4. 工具方法应保持无状态，避免并发请求之间相互影响。
5. 不建议在业务 Service 中再次进行全量转义，避免重复转义。

### 请求体重复读取处理

请求体重复读取是 JSON 请求过滤的关键点。Servlet 原生 `InputStream` 只能读取一次，Filter 中读取后，Controller 的 `@RequestBody` 将无法再次读取。解决方式是在 RequestWrapper 中将请求体缓存为字节数组，并基于该字节数组重新创建输入流。

核心实现点如下：

| 重写方法           | 说明                                     |
| ------------------ | ---------------------------------------- |
| `getInputStream()` | 返回基于缓存字节数组的新输入流           |
| `getReader()`      | 返回基于缓存输入流的新字符流             |
| 构造方法           | 读取原始 Body，完成过滤后写入缓存        |
| `initBody()`       | 判断 JSON 请求、读取 Body、执行 XSS 处理 |

在本方案中，`XssHttpServletRequestWrapper` 的处理策略如下：

1. 非 JSON 请求不读取 Body，避免影响表单和文件上传。
2. JSON 请求读取原始 Body。
3. 如果 Body 为空，则缓存空字节数组。
4. 如果 Body 超过最大处理大小，则不执行 JSON 转义，避免内存压力。
5. 如果 Body 是合法 JSON，则递归处理字符串字段。
6. Controller 后续通过 `@RequestBody` 读取的是处理后的 Body。

请求体重复读取验证示例。

请求接口：

```java
@PostMapping("/notice")
public String saveNotice(@RequestBody NoticeSaveRequest request) {
    return request.getTitle();
}
```

请求数据：

```json
{
  "title": "<script>alert(1)</script>",
  "content": "<p>正文</p><script>alert(1)</script>"
}
```

如果 `content` 配置为富文本字段，则 Controller 实际接收到的内容类似如下：

```json
{
  "title": "&lt;script&gt;alert(1)&lt;/script&gt;",
  "content": "<p>正文</p>"
}
```

该方式可以保证 Filter 已经读取过请求体的情况下，Controller 仍然可以正常完成 `@RequestBody` 参数绑定。

### 白名单路径配置

白名单路径用于排除不适合 XSS 过滤的接口。部分接口如果被统一转义，可能导致签名失败、文件损坏或第三方回调验签异常，因此必须提供路径级排除能力。

建议排除以下类型接口：

| 接口类型   | 示例路径            | 排除原因                 |
| ---------- | ------------------- | ------------------------ |
| 文件上传   | `/upload/**`        | 避免读取或修改文件流     |
| 文件下载   | `/file/download/**` | 不需要处理请求体         |
| 第三方回调 | `/callback/**`      | 避免影响验签             |
| Webhook    | `/webhook/**`       | 外部平台通常要求原始报文 |
| Actuator   | `/actuator/**`      | 监控端点不需要业务过滤   |
| 加密接口   | `/api/secure/**`    | 加密报文被修改后无法解密 |
| 签名接口   | `/api/sign/**`      | 参数转义会导致签名不一致 |

配置示例：

```yaml
xss:
  enabled: true
  excludes:
    - /actuator/**
    - /upload/**
    - /file/**
    - /callback/**
    - /webhook/**
    - /api/secure/**
    - /api/sign/**
```

白名单匹配推荐使用 Spring 提供的 `AntPathMatcher`，支持常见通配符写法：

| 表达式          | 说明                        |
| --------------- | --------------------------- |
| `/upload/**`    | 匹配 `/upload` 下所有路径   |
| `/callback/*`   | 匹配 `/callback` 下一级路径 |
| `/api/*/import` | 匹配中间一级动态路径        |
| `/**/*.html`    | 匹配任意层级下的 HTML 路径  |

白名单配置建议如下：

1. 白名单范围应尽量小，不要直接排除 `/api/**`。
2. 文件上传、回调、签名验签接口应优先排除。
3. 管理后台普通增删改查接口不建议排除。
4. 白名单路径应定期审查，避免历史配置扩大攻击面。
5. 对被排除的第三方回调接口，应在业务层进行签名校验、来源校验和参数合法性校验。

通过以上实现，XSS 防护能力已经具备基础可用性：请求路径可排除、普通参数可转义、JSON Body 可重复读取并过滤、富文本字段可按白名单清理。下一步可以继续补充配置设计、接口适配和安全验证部分。



## 配置设计

本章节用于说明 XSS 防护功能的配置项设计。配置设计的目标是让防护能力具备可开关、可排除、可扩展和可调整的能力，避免将过滤规则硬编码到业务代码中。

### XSS 开关配置

XSS 开关用于控制整个防护功能是否启用。该配置通常用于不同环境下的灵活控制，例如开发环境临时关闭、测试环境验证规则、生产环境强制开启。

配置示例：

```yaml
xss:
  # 是否启用 XSS 防护
  enabled: true
```

开关配置建议如下：

| 配置值  | 说明                                                    |
| ------- | ------------------------------------------------------- |
| `true`  | 启用 XSS Filter，所有命中拦截路径的请求都会进入防护流程 |
| `false` | 关闭 XSS Filter，系统不对请求参数和请求体进行统一过滤   |

在生产环境中建议保持开启，除非系统已经在网关、WAF 或统一安全中间件中完成了等效防护。即使存在前端过滤，也不建议关闭后端 XSS 防护，因为前端输入校验可以被绕过。

在 Spring Boot 中可以通过 `@ConditionalOnProperty` 控制 Filter 是否注册。当 `xss.enabled=false` 时，XSS Filter 不会加入 Servlet 过滤器链。

配置生效逻辑如下：

| 场景                 | 处理结果          |
| -------------------- | ----------------- |
| 未配置 `xss.enabled` | 默认启用          |
| `xss.enabled=true`   | 注册 XSS Filter   |
| `xss.enabled=false`  | 不注册 XSS Filter |

该设计可以保证安全能力默认开启，同时保留必要的环境级关闭能力。

### 排除 URL 配置

排除 URL 用于配置不需要经过 XSS 过滤的接口路径。部分接口对原始请求内容有强依赖，如果被转义或清理，可能导致业务异常，例如签名验签失败、文件上传异常、第三方回调报文不一致等。

配置示例：

```yaml
xss:
  # 排除路径，命中后不进行参数过滤和请求体过滤
  excludes:
    - /actuator/**
    - /upload/**
    - /file/**
    - /callback/**
    - /webhook/**
    - /api/secure/**
    - /api/sign/**
```

建议排除的接口类型如下：

| 接口类型       | 示例路径         | 排除原因                       |
| -------------- | ---------------- | ------------------------------ |
| 文件上传接口   | `/upload/**`     | 避免读取或修改文件流           |
| 文件下载接口   | `/file/**`       | 下载接口通常不需要处理请求体   |
| 第三方回调接口 | `/callback/**`   | 回调报文需要保持原文用于验签   |
| Webhook 接口   | `/webhook/**`    | 外部平台通常要求原始请求体一致 |
| 加密报文接口   | `/api/secure/**` | 请求内容被修改后可能无法解密   |
| 签名验签接口   | `/api/sign/**`   | 参数转义会导致签名不一致       |
| 监控端点       | `/actuator/**`   | 监控端点不属于业务输入场景     |

路径匹配建议使用 Spring 的 `AntPathMatcher`，支持常见通配符规则：

| 匹配规则        | 说明                          |
| --------------- | ----------------------------- |
| `/upload/**`    | 匹配 `/upload` 下所有层级路径 |
| `/callback/*`   | 匹配 `/callback` 下一级路径   |
| `/api/*/import` | 匹配中间一级动态路径          |
| `/**/*.html`    | 匹配任意层级下的 HTML 路径    |

排除 URL 配置需要遵循最小化原则。不要为了方便直接排除 `/api/**` 或 `/**`，否则会使 XSS 防护失效。对于已排除的接口，应在业务层补充签名校验、来源校验、文件类型校验、文件大小限制等安全措施。

### 允许标签配置

允许标签配置主要用于富文本字段处理。普通文本字段使用 HTML 转义即可，但富文本字段需要保留部分 HTML 标签，所以需要配置允许的标签、属性和协议。

配置示例：

```yaml
xss:
  # 富文本字段，命中字段后使用白名单清理
  rich-text-fields:
    - content
    - description
    - noticeContent
    - articleContent

  # 富文本允许标签
  allowed-tags:
    - p
    - br
    - span
    - div
    - strong
    - b
    - em
    - i
    - u
    - ul
    - ol
    - li
    - blockquote
    - a
    - img
    - table
    - thead
    - tbody
    - tr
    - th
    - td
```

建议允许的基础标签如下：

| 标签类型 | 标签                                        |
| -------- | ------------------------------------------- |
| 段落结构 | `p`、`br`、`span`、`div`                    |
| 文本样式 | `strong`、`b`、`em`、`i`、`u`               |
| 列表     | `ul`、`ol`、`li`                            |
| 引用     | `blockquote`                                |
| 链接     | `a`                                         |
| 图片     | `img`                                       |
| 表格     | `table`、`thead`、`tbody`、`tr`、`th`、`td` |

允许标签配置需要注意以下事项：

1. 不允许配置 `script`、`iframe`、`object`、`embed` 等高风险标签。
2. 如果业务必须支持 `iframe`，应额外限制可信域名，不能直接开放任意地址。
3. `style` 属性需要谨慎开放，因为部分 CSS 表达式或 URL 资源也可能带来安全风险。
4. 图片 `src` 建议限制为 `http`、`https`，并结合可信域名校验。
5. 链接 `href` 不允许 `javascript:`、`vbscript:` 等危险协议。
6. 允许标签越多，富文本展示能力越强，但安全边界也越复杂。

如果系统对富文本要求较简单，建议优先使用 Jsoup 的 `Safelist.basic()` 或 `Safelist.relaxed()` 作为基础策略，再按业务需要追加标签和属性。

### 过滤策略配置

过滤策略配置用于控制不同类型内容的处理方式。由于普通文本、JSON 字段、富文本字段和请求头的处理方式不同，因此需要支持策略化配置。

配置示例：

```yaml
xss:
  # 普通文本处理策略：escape 表示 HTML 转义，clean 表示清理危险内容
  text-strategy: escape

  # JSON 字符串字段处理策略
  json-strategy: escape

  # 富文本字段处理策略：rich-clean 表示使用白名单清理
  rich-text-strategy: rich-clean

  # 是否过滤请求头，默认建议关闭
  enable-header-filter: false

  # 是否添加响应安全头
  enable-response-header: true

  # 请求体最大处理大小，默认 1MB
  max-body-size: 1048576
```

策略说明如下：

| 策略项                   | 推荐值       | 说明                                    |
| ------------------------ | ------------ | --------------------------------------- |
| `text-strategy`          | `escape`     | 普通文本字段使用 HTML 转义              |
| `json-strategy`          | `escape`     | JSON 字符串字段使用 HTML 转义           |
| `rich-text-strategy`     | `rich-clean` | 富文本字段使用白名单清理                |
| `enable-header-filter`   | `false`      | 默认不处理请求头，避免影响 Token 和签名 |
| `enable-response-header` | `true`       | 添加基础安全响应头                      |
| `max-body-size`          | `1048576`    | 限制请求体最大处理大小，避免内存压力    |

策略设计建议如下：

1. 普通文本字段建议使用 `escape`，不要直接删除字符。
2. 富文本字段建议使用 `rich-clean`，不要使用普通转义。
3. 请求头默认不建议过滤，尤其是 `Authorization`、`X-Signature`、`X-Token` 等字段。
4. 请求体大小应设置上限，避免大 Body 在 Filter 中被完整缓存导致内存压力。
5. 对非法 JSON 可以直接放行给后续统一异常处理，也可以返回参数错误，具体按项目规范决定。
6. 不同项目可以在工具类中通过策略枚举扩展不同处理方式。

如果后续需要支持更多策略，可以将策略定义为枚举，例如：

```java
package io.github.atengk.xss.enums;

/**
 * XSS 过滤策略
 *
 * @author Ateng
 * @since 2026-05-06
 */
public enum XssStrategy {

    /**
     * HTML 转义
     */
    ESCAPE,

    /**
     * HTML 白名单清理
     */
    CLEAN,

    /**
     * 富文本白名单清理
     */
    RICH_CLEAN,

    /**
     * 不处理
     */
    NONE
}
```

该枚举用于描述不同字段的处理策略，后续可以在配置属性中按字段或按接口扩展更细粒度的过滤规则。

## 接口适配

本章节用于说明 XSS 防护在不同接口类型下的适配方式。不同请求类型进入 Spring MVC 的方式不同，Filter 中需要分别处理表单参数、Query 参数、JSON 请求体和文件上传接口。

### 表单参数处理

表单参数主要指 `Content-Type` 为 `application/x-www-form-urlencoded` 的请求。该类请求通常用于普通表单提交，参数会通过 Servlet 参数 API 读取，并由 Spring MVC 自动绑定到方法参数或对象字段。

请求示例：

```http
POST /api/user/save HTTP/1.1
Content-Type: application/x-www-form-urlencoded

username=<script>alert(1)</script>&remark=正常备注
```

Controller 示例：

```java
package io.github.atengk.user.controller;

import io.github.atengk.user.dto.UserSaveRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户接口
 *
 * @author Ateng
 * @since 2026-05-06
 */
@RestController
public class UserController {

    /**
     * 保存用户
     *
     * @param request 用户保存参数
     * @return 处理结果
     */
    @PostMapping("/api/user/save")
    public String save(UserSaveRequest request) {
        return request.getUsername();
    }
}
```

对于表单参数，XSS 防护通过重写 `getParameter()`、`getParameterValues()` 和 `getParameterMap()` 生效。Spring MVC 在进行参数绑定时会读取包装后的参数，因此 Controller 接收到的是已经转义后的值。

处理结果示例：

| 字段       | 原始值                      | Controller 接收值           |
| ---------- | --------------------------- | --------------------------- |
| `username` | `<script>alert(1)</script>` | `<script>alert(1)</script>` |
| `remark`   | `正常备注`                  | `正常备注`                  |

表单参数适配注意事项：

1. 表单字段适合使用普通 HTML 转义策略。
2. 不建议对表单参数直接删除特殊字符。
3. 如果表单中包含富文本字段，需要根据字段名进入富文本白名单清理。
4. 如果表单用于签名验签，应将该接口加入排除 URL。
5. 表单文件上传接口通常是 `multipart/form-data`，不应按普通表单处理。

### Query 参数处理

Query 参数主要指出现在 URL 后面的请求参数，常见于列表查询、搜索、筛选、详情查询等接口。

请求示例：

```http
GET /api/user/page?keyword=<img src=x onerror=alert(1)>&status=1 HTTP/1.1
```

Controller 示例：

```java
package io.github.atengk.user.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户查询接口
 *
 * @author Ateng
 * @since 2026-05-06
 */
@RestController
public class UserQueryController {

    /**
     * 查询用户列表
     *
     * @param keyword 搜索关键字
     * @param status 状态
     * @return 查询条件摘要
     */
    @GetMapping("/api/user/page")
    public String page(@RequestParam String keyword, @RequestParam Integer status) {
        return "keyword=" + keyword + ", status=" + status;
    }
}
```

Query 参数处理结果示例：

| 字段      | 原始值                         | Controller 接收值              |
| --------- | ------------------------------ | ------------------------------ |
| `keyword` | `<img src=x onerror=alert(1)>` | `<img src=x onerror=alert(1)>` |
| `status`  | `1`                            | `1`                            |

Query 参数适配建议如下：

1. 搜索关键字、名称、标题等字符串参数应进行 HTML 转义。
2. 状态、类型、排序字段等参数应配合白名单校验。
3. 分页参数应使用数值类型接收，避免字符串拼接导致其他安全风险。
4. 排序字段不能直接拼接到 SQL，应使用字段白名单映射。
5. Query 参数过滤只能降低 XSS 风险，不能替代 SQL 注入防护和参数合法性校验。

对于排序类参数，建议业务层继续做白名单处理。例如只允许 `createTime`、`updateTime`、`username` 等固定字段，不允许直接使用前端传入字段拼接 SQL。

### JSON 参数处理

JSON 参数主要指 `Content-Type` 为 `application/json` 的请求，常见于新增、编辑、批量保存、复杂条件查询等接口。该类请求通常通过 `@RequestBody` 接收。

请求示例：

```http
POST /api/notice/save HTTP/1.1
Content-Type: application/json

{
  "title": "<script>alert(1)</script>",
  "content": "<p>公告正文</p><script>alert(1)</script>",
  "sort": 1
}
```

DTO 示例：

```java
package io.github.atengk.notice.dto;

import lombok.Data;

/**
 * 公告保存请求参数
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
public class NoticeSaveRequest {

    /**
     * 标题
     */
    private String title;

    /**
     * 公告内容，富文本字段
     */
    private String content;

    /**
     * 排序
     */
    private Integer sort;
}
```

Controller 示例：

```java
package io.github.atengk.notice.controller;

import io.github.atengk.notice.dto.NoticeSaveRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 公告接口
 *
 * @author Ateng
 * @since 2026-05-06
 */
@RestController
public class NoticeController {

    /**
     * 保存公告
     *
     * @param request 公告保存参数
     * @return 处理结果
     */
    @PostMapping("/api/notice/save")
    public NoticeSaveRequest save(@RequestBody NoticeSaveRequest request) {
        return request;
    }
}
```

如果 `content` 配置为富文本字段：

```yaml
xss:
  rich-text-fields:
    - content
```

Controller 实际接收到的数据如下：

```json
{
  "title": "&lt;script&gt;alert(1)&lt;/script&gt;",
  "content": "<p>公告正文</p>",
  "sort": 1
}
```

JSON 参数适配建议如下：

1. 只处理字符串字段，不处理数字、布尔值、对象结构和数组结构本身。
2. 嵌套对象和数组中的字符串字段需要递归处理。
3. 富文本字段根据字段名使用白名单清理策略。
4. 请求体需要支持重复读取，否则 `@RequestBody` 会绑定失败。
5. 非法 JSON 不建议在 XSS Filter 中吞掉异常，应交由统一异常处理。
6. 大 Body 请求应限制处理大小，避免内存占用过高。

对于批量 JSON 请求，也应支持数组结构：

```json
[
  {
    "title": "<script>alert(1)</script>",
    "content": "<p>第一条</p><script>alert(1)</script>"
  },
  {
    "title": "<img src=x onerror=alert(1)>",
    "content": "<p>第二条</p>"
  }
]
```

处理后：

```json
[
  {
    "title": "&lt;script&gt;alert(1)&lt;/script&gt;",
    "content": "<p>第一条</p>"
  },
  {
    "title": "&lt;img src=x onerror=alert(1)&gt;",
    "content": "<p>第二条</p>"
  }
]
```

### 文件上传接口排除

文件上传接口通常使用 `multipart/form-data`，请求体中包含文件二进制内容和普通表单字段。如果 XSS Filter 直接读取或修改上传请求体，可能导致文件流损坏、上传失败或内存压力增大。

因此文件上传接口建议通过排除 URL 配置直接放行。

配置示例：

```yaml
xss:
  excludes:
    - /upload/**
    - /file/upload/**
    - /api/*/import
```

文件上传 Controller 示例：

```java
package io.github.atengk.file.controller;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传接口
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@RestController
public class FileUploadController {

    /**
     * 上传文件
     *
     * @param file 文件对象
     * @param bizType 业务类型
     * @return 上传结果
     */
    @GetMapping("/upload/{bizType}")
    public String upload(@RequestPart("file") MultipartFile file, @PathVariable String bizType) {
        log.info("开始上传文件，业务类型：{}，文件名：{}", bizType, file.getOriginalFilename());

        if (file.isEmpty()) {
            return "文件不能为空";
        }

        if (StrUtil.isBlank(bizType)) {
            return "业务类型不能为空";
        }

        return "上传成功";
    }

    /**
     * 导入文件
     *
     * @param file 文件对象
     * @param source 来源
     * @return 导入结果
     */
    @GetMapping("/api/user/import")
    public String importUser(@RequestPart("file") MultipartFile file, @RequestParam String source) {
        log.info("开始导入用户文件，来源：{}，文件名：{}", source, file.getOriginalFilename());

        if (file.isEmpty()) {
            return "导入文件不能为空";
        }

        return "导入成功";
    }
}
```

上面的上传接口应命中 `/upload/**` 或 `/api/*/import` 排除规则，不进入 XSS 请求体过滤流程。

文件上传接口虽然排除了 XSS Filter，但仍需要进行其他安全校验：

| 校验项   | 说明                                      |
| -------- | ----------------------------------------- |
| 文件大小 | 限制单个文件大小和总上传大小              |
| 文件类型 | 校验扩展名和 MIME 类型                    |
| 文件内容 | 对图片、Excel、PDF 等文件进行必要格式校验 |
| 文件名   | 避免使用原始文件名直接落盘                |
| 存储路径 | 防止路径穿越，例如 `../../`               |
| 权限控制 | 上传接口必须校验登录态和业务权限          |
| 病毒扫描 | 高安全场景建议接入文件扫描能力            |

文件上传接口排除的是 XSS 请求体过滤，不代表该接口不需要安全处理。上传文件中的文本内容如果后续会被解析、入库或展示，例如 Excel 导入用户昵称、文章标题、备注等字段，应在解析后进入业务参数校验和 XSS 清理流程。



## 安全验证

本章节用于验证 XSS 防护功能是否生效。验证重点包括普通脚本注入、HTML 标签注入、事件属性注入和 JSON 请求体注入，确保 Query 参数、表单参数、JSON 字段和富文本字段都能按预期处理。

建议先准备一个测试 Controller，用于接收不同类型的请求参数并返回处理后的结果。

文件位置：`src/main/java/io/github/atengk/xss/controller/XssTestController.java`

下面代码用于提供 XSS 防护测试接口，覆盖 Query 参数、表单参数和 JSON 请求体三类常见输入场景。

```java
package io.github.atengk.xss.controller;

import cn.hutool.core.map.MapUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * XSS 防护测试接口
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@RestController
@RequestMapping("/test/xss")
public class XssTestController {

    /**
     * 测试 Query 参数过滤
     *
     * @param keyword 搜索关键字
     * @return 过滤后的参数
     */
    @GetMapping("/query")
    public Map<String, Object> query(@RequestParam String keyword) {
        log.info("接收到 Query 参数：{}", keyword);
        return MapUtil.<String, Object>builder()
                .put("keyword", keyword)
                .build();
    }

    /**
     * 测试表单参数过滤
     *
     * @param username 用户名
     * @param remark 备注
     * @return 过滤后的参数
     */
    @PostMapping("/form")
    public Map<String, Object> form(@RequestParam String username, @RequestParam String remark) {
        log.info("接收到表单参数，username：{}，remark：{}", username, remark);
        return MapUtil.<String, Object>builder()
                .put("username", username)
                .put("remark", remark)
                .build();
    }

    /**
     * 测试 JSON 请求体过滤
     *
     * @param request 请求参数
     * @return 过滤后的参数
     */
    @PostMapping("/json")
    public XssTestRequest json(@RequestBody XssTestRequest request) {
        log.info("接收到 JSON 参数：{}", request);
        return request;
    }

    /**
     * XSS 测试请求参数
     *
     * @author Ateng
     * @since 2026-05-06
     */
    @Data
    public static class XssTestRequest {

        /**
         * 标题
         */
        private String title;

        /**
         * 普通备注
         */
        private String remark;

        /**
         * 富文本内容
         */
        private String content;
    }
}
```

### 普通脚本注入测试

普通脚本注入测试用于验证 `<script>` 标签是否会被转义，避免脚本被浏览器解析执行。该测试适用于 Query 参数、表单参数和普通 JSON 字符串字段。

测试 Query 参数：

```bash
curl -G "http://localhost:8080/test/xss/query" \
  --data-urlencode "keyword=<script>alert(1)</script>"
```

预期响应：

```json
{
  "keyword": "&lt;script&gt;alert(1)&lt;/script&gt;"
}
```

测试表单参数：

```bash
curl -X POST "http://localhost:8080/test/xss/form" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  --data-urlencode "username=<script>alert(1)</script>" \
  --data-urlencode "remark=正常备注"
```

预期响应：

```json
{
  "username": "&lt;script&gt;alert(1)&lt;/script&gt;",
  "remark": "正常备注"
}
```

验证结论：

| 验证项              | 预期结果             |
| ------------------- | -------------------- |
| `<script>` 标签     | 被转义为 HTML 实体   |
| 正常中文内容        | 保持原样             |
| Controller 参数绑定 | 正常接收             |
| 后端日志输出        | 输出转义后的安全内容 |

如果响应中仍然原样返回 `<script>alert(1)</script>`，说明参数过滤未生效，需要检查 Filter 是否注册、请求路径是否被排除、`xss.enabled` 是否为 `true`。

### HTML 标签注入测试

HTML 标签注入测试用于验证普通 HTML 标签是否会被转义，避免用户输入被浏览器当作页面结构解析。该测试适用于昵称、标题、搜索关键字、备注等普通文本字段。

测试请求：

```bash
curl -G "http://localhost:8080/test/xss/query" \
  --data-urlencode "keyword=<h1>标题</h1><iframe src='https://example.com'></iframe>"
```

预期响应：

```json
{
  "keyword": "&lt;h1&gt;标题&lt;/h1&gt;&lt;iframe src=&#39;https://example.com&#39;&gt;&lt;/iframe&gt;"
}
```

验证重点如下：

| 输入内容        | 预期处理                   |
| --------------- | -------------------------- |
| `<h1>标题</h1>` | 普通文本字段中应被转义     |
| `<iframe>`      | 普通文本字段中应被转义     |
| 中文内容        | 保留原始语义               |
| 单引号、双引号  | 根据转义工具转换为安全实体 |

普通文本字段不应保留 HTML 标签。如果业务确实需要保留 HTML 标签，应将对应字段配置为富文本字段，并使用白名单清理策略。

### 事件属性注入测试

事件属性注入测试用于验证 `onerror`、`onclick`、`onload` 等 HTML 事件属性是否会被处理。该类攻击常见于图片、链接、按钮等标签中。

测试 Query 参数：

```bash
curl -G "http://localhost:8080/test/xss/query" \
  --data-urlencode "keyword=<img src=x onerror=alert(1)>"
```

预期响应：

```json
{
  "keyword": "&lt;img src=x onerror=alert(1)&gt;"
}
```

测试表单参数：

```bash
curl -X POST "http://localhost:8080/test/xss/form" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  --data-urlencode "username=张三" \
  --data-urlencode "remark=<button onclick=alert(1)>提交</button>"
```

预期响应：

```json
{
  "username": "张三",
  "remark": "&lt;button onclick=alert(1)&gt;提交&lt;/button&gt;"
}
```

对于普通文本字段，事件属性所在的完整标签应被转义，不应作为 HTML 结构返回。

如果是富文本字段，处理结果应由白名单清理组件决定。事件属性应被移除，安全标签可以保留。

富文本输入示例：

```html
<p>正文内容</p><img src="https://example.com/a.png" onerror="alert(1)">
```

富文本预期结果：

```html
<p>正文内容</p><img src="https://example.com/a.png">
```

验证重点如下：

| 验证项    | 普通文本字段   | 富文本字段   |
| --------- | -------------- | ------------ |
| HTML 标签 | 整体转义       | 按白名单保留 |
| 事件属性  | 随标签整体转义 | 移除         |
| 危险协议  | 整体转义       | 移除或清空   |
| 安全文本  | 保留           | 保留         |

### JSON 请求体注入测试

JSON 请求体注入测试用于验证 `@RequestBody` 场景下请求体是否可以被正确读取、过滤并继续完成参数绑定。该测试可以验证 RequestWrapper 的请求体重复读取能力。

测试普通 JSON 字段：

```bash
curl -X POST "http://localhost:8080/test/xss/json" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "<script>alert(1)</script>",
    "remark": "<img src=x onerror=alert(1)>",
    "content": "<p>公告正文</p><script>alert(1)</script>"
  }'
```

如果 `content` 已配置为富文本字段：

```yaml
xss:
  rich-text-fields:
    - content
```

预期响应：

```json
{
  "title": "&lt;script&gt;alert(1)&lt;/script&gt;",
  "remark": "&lt;img src=x onerror=alert(1)&gt;",
  "content": "<p>公告正文</p>"
}
```

测试嵌套 JSON 或数组时，也应验证递归处理是否生效。

嵌套 JSON 请求示例：

```json
{
  "title": "<script>alert(1)</script>",
  "items": [
    {
      "name": "<img src=x onerror=alert(1)>",
      "content": "<p>第一条</p><script>alert(1)</script>"
    }
  ]
}
```

预期处理规则：

| 字段         | 字段类型     | 预期处理                   |
| ------------ | ------------ | -------------------------- |
| `title`      | 普通字符串   | HTML 转义                  |
| `items`      | 数组         | 保留结构，递归处理内部字段 |
| `name`       | 普通字符串   | HTML 转义                  |
| `content`    | 富文本字符串 | 白名单清理                 |
| 数字、布尔值 | 非字符串     | 不处理                     |

如果 Controller 能正常接收到 `@RequestBody` 参数，并且返回值符合上述预期，说明 JSON 请求体过滤和重复读取逻辑均已生效。

## 使用说明

本章节说明 XSS 防护功能的实际接入方式，包括配置示例、接入步骤、测试示例和注意事项。接入时应先完成依赖引入和核心类创建，再通过配置文件启用功能并执行接口验证。

### 配置示例

以下是一个较完整的 XSS 防护配置示例，适合 Spring Boot 3 前后端分离项目使用。

文件位置：`src/main/resources/application.yml`

```yaml
xss:
  # 是否启用 XSS 防护，生产环境建议开启
  enabled: true

  # Filter 拦截路径，默认拦截全部请求
  url-patterns:
    - /*

  # 排除路径，文件上传、回调、签名验签接口建议排除
  excludes:
    - /actuator/**
    - /upload/**
    - /file/**
    - /callback/**
    - /webhook/**
    - /api/secure/**
    - /api/sign/**

  # 富文本字段，命中字段后使用 Jsoup 白名单清理
  rich-text-fields:
    - content
    - description
    - noticeContent
    - articleContent

  # 普通文本处理策略：escape 表示 HTML 转义
  text-strategy: escape

  # JSON 字符串字段处理策略
  json-strategy: escape

  # 富文本字段处理策略
  rich-text-strategy: rich-clean

  # 请求体最大处理大小，默认 1MB
  max-body-size: 1048576

  # 是否过滤请求头，默认关闭，避免影响 Authorization、Token、签名字段
  enable-header-filter: false

  # 是否添加基础响应安全头
  enable-response-header: true
```

配置项说明如下：

| 配置项                       | 默认值       | 说明                    |
| ---------------------------- | ------------ | ----------------------- |
| `xss.enabled`                | `true`       | 是否启用 XSS 防护       |
| `xss.url-patterns`           | `/*`         | Filter 拦截路径         |
| `xss.excludes`               | 空集合       | 不进行 XSS 过滤的路径   |
| `xss.rich-text-fields`       | 空集合       | 需要按富文本处理的字段  |
| `xss.text-strategy`          | `escape`     | 普通文本处理策略        |
| `xss.json-strategy`          | `escape`     | JSON 字符串字段处理策略 |
| `xss.rich-text-strategy`     | `rich-clean` | 富文本处理策略          |
| `xss.max-body-size`          | `1048576`    | 请求体最大处理大小      |
| `xss.enable-header-filter`   | `false`      | 是否过滤请求头          |
| `xss.enable-response-header` | `true`       | 是否添加响应安全头      |

配置完成后，重启应用即可生效。如果 `xss.enabled=false`，则 Filter 不会注册，所有请求都不会进入 XSS 防护逻辑。

### 接入步骤

XSS 防护功能建议按以下步骤接入，避免遗漏依赖、配置或过滤器注册。

第一步，引入依赖。

文件位置：`pom.xml`

```xml
<dependencies>
    <!-- Spring Boot Web 基础依赖，提供 MVC 和 Servlet 环境 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Hutool 工具包，用于字符串、集合、JSON、HTML 转义处理 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.8.35</version>
    </dependency>

    <!-- Jsoup HTML 清理组件，用于富文本白名单过滤 -->
    <dependency>
        <groupId>org.jsoup</groupId>
        <artifactId>jsoup</artifactId>
        <version>1.17.2</version>
    </dependency>

    <!-- Lombok，减少配置类、DTO 和日志代码样板 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

第二步，创建配置属性类 `XssProperties`，用于读取 `application.yml` 中的 XSS 配置。

文件位置：

```text
src/main/java/io/github/atengk/xss/config/XssProperties.java
```

第三步，创建工具类 `XssUtils`，封装普通文本转义、JSON 递归处理和富文本白名单清理逻辑。

文件位置：

```text
src/main/java/io/github/atengk/xss/support/XssUtils.java
```

第四步，创建请求包装器 `XssHttpServletRequestWrapper`，重写参数读取方法和请求体读取方法，保证参数可过滤、Body 可重复读取。

文件位置：

```text
src/main/java/io/github/atengk/xss/support/XssHttpServletRequestWrapper.java
```

第五步，创建过滤器 `XssFilter`，在请求进入 Controller 前判断排除路径并包装请求对象。

文件位置：

```text
src/main/java/io/github/atengk/xss/filter/XssFilter.java
```

第六步，创建过滤器注册配置 `XssFilterConfig`，通过 `FilterRegistrationBean` 注册 XSS Filter。

文件位置：

```text
src/main/java/io/github/atengk/xss/config/XssFilterConfig.java
```

第七步，在 `application.yml` 中启用 XSS 防护并配置排除路径、富文本字段和请求体大小限制。

文件位置：

```text
src/main/resources/application.yml
```

第八步，启动应用并通过 curl、Postman 或接口自动化测试工具验证防护结果。

启动命令如下：

```bash
mvn spring-boot:run
```

该命令会启动当前 Spring Boot 项目。启动后可以通过接口测试验证 Query 参数、表单参数和 JSON 请求体是否按预期完成过滤。

### 测试示例

接入完成后，可以使用以下命令进行快速验证。测试前请确认应用端口为 `8080`，如果实际端口不同，需要替换命令中的端口号。

测试 Query 参数：

```bash
curl -G "http://localhost:8080/test/xss/query" \
  --data-urlencode "keyword=<script>alert(1)</script>"
```

预期结果：

```json
{
  "keyword": "&lt;script&gt;alert(1)&lt;/script&gt;"
}
```

测试表单参数：

```bash
curl -X POST "http://localhost:8080/test/xss/form" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  --data-urlencode "username=<img src=x onerror=alert(1)>" \
  --data-urlencode "remark=正常备注"
```

预期结果：

```json
{
  "username": "&lt;img src=x onerror=alert(1)&gt;",
  "remark": "正常备注"
}
```

测试 JSON 请求体：

```bash
curl -X POST "http://localhost:8080/test/xss/json" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "<script>alert(1)</script>",
    "remark": "<img src=x onerror=alert(1)>",
    "content": "<p>正文</p><script>alert(1)</script>"
  }'
```

预期结果：

```json
{
  "title": "&lt;script&gt;alert(1)&lt;/script&gt;",
  "remark": "&lt;img src=x onerror=alert(1)&gt;",
  "content": "<p>正文</p>"
}
```

测试排除路径：

```bash
curl -X POST "http://localhost:8080/upload/avatar" \
  -F "file=@./test.png"
```

预期结果是文件上传接口正常进入业务逻辑，不被 XSS Filter 读取或修改请求体。如果文件上传接口出现请求体为空、文件损坏或参数解析失败，需要检查 `/upload/**` 是否已加入 `xss.excludes`。

测试响应安全头：

```bash
curl -I "http://localhost:8080/test/xss/query?keyword=test"
```

预期响应头中包含以下内容：

```http
X-Content-Type-Options: nosniff
X-Frame-Options: SAMEORIGIN
Referrer-Policy: strict-origin-when-cross-origin
X-XSS-Protection: 0
```

测试通过标准如下：

| 测试项          | 通过标准                             |
| --------------- | ------------------------------------ |
| Query 参数      | 危险 HTML 被转义                     |
| 表单参数        | 危险 HTML 被转义                     |
| JSON 普通字段   | 字符串字段被转义                     |
| JSON 富文本字段 | 危险标签和属性被清理                 |
| 文件上传接口    | 正常上传，不被请求体过滤影响         |
| 响应安全头      | 返回预期安全响应头                   |
| `@RequestBody`  | 参数绑定正常，不出现 Body 已读取异常 |

### 注意事项

XSS 防护属于输入安全和输出安全的一部分，不能替代权限控制、参数校验、SQL 注入防护、文件安全校验和前端安全渲染。接入时需要结合项目整体安全策略统一考虑。

主要注意事项如下：

1. 不要将所有接口都加入排除路径。排除范围过大会导致 XSS Filter 形同虚设。
2. 文件上传接口建议排除，但上传文件解析出的文本内容如果会入库或展示，仍需要进行业务层清理。
3. 第三方回调、Webhook、签名验签接口建议排除，避免修改原始报文导致验签失败。
4. 普通文本字段建议使用 HTML 转义，不建议直接删除特殊字符。
5. 富文本字段建议使用白名单清理，不建议直接保存前端提交的原始 HTML。
6. 请求头默认不建议过滤，避免影响 `Authorization`、`X-Token`、`X-Signature` 等认证或签名字段。
7. JSON 请求体需要控制最大处理大小，避免大请求体被完整缓存造成内存压力。
8. 不要在 Filter、Controller、Service 多处重复转义同一字段，避免出现 `&lt;` 这类重复转义问题。
9. 前端展示普通文本时应使用安全插值，不要直接写入 `innerHTML`。
10. 前端展示富文本时应确认内容已经经过后端白名单清理，必要时前端也应进行二次清理。
11. CSP 响应头需要结合前端资源来源逐步调整，避免一次性配置过严导致页面资源无法加载。
12. 生产环境建议配合 WAF、网关限流、接口鉴权、参数校验、审计日志一起使用。

常见问题排查如下：

| 问题                   | 可能原因                                     | 处理方式                      |
| ---------------------- | -------------------------------------------- | ----------------------------- |
| XSS 过滤未生效         | `xss.enabled=false` 或 Filter 未注册         | 检查配置和启动日志            |
| `@RequestBody` 为空    | Filter 读取 Body 后未重写 `getInputStream()` | 检查 RequestWrapper 实现      |
| 文件上传失败           | 上传路径未加入排除配置                       | 将上传路径加入 `xss.excludes` |
| 第三方回调验签失败     | 请求体被转义或清理                           | 将回调路径加入排除配置        |
| 富文本标签全部变成文本 | 富文本字段未配置                             | 将字段加入 `rich-text-fields` |
| 出现重复转义           | 多层重复调用转义逻辑                         | 保证只在统一入口处理一次      |
| 前端页面资源加载失败   | CSP 配置过严                                 | 按资源域名调整 CSP 配置       |

最终接入完成后，应将 XSS 防护测试纳入接口回归测试。对于新增富文本字段、新增文件上传接口、新增第三方回调接口，应同步检查是否需要调整 `rich-text-fields` 或 `excludes` 配置。