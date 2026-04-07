# XSS

在 Spring Boot 3 中实现 XSS（跨站脚本攻击）防护，核心思路是**对请求参数进行统一过滤与转义**。标准做法是基于 **Filter + HttpServletRequestWrapper** 对请求进行包装，从而在进入 Controller 之前完成清洗。

---



# 配置文件

`application.yml`

```yaml
---
# XSS 配置
xss:
  enabled: true
  exclude-paths:
    - /login
    - /logout
    - /captcha
    - /static/**
    - /api/public/*
```

---

# 配置属性类

```java
package io.github.atengk.xss.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * XSS 配置属性
 *
 * @author 孔余
 * @since 2026-04-05
 */
@Component
@ConfigurationProperties(prefix = "xss")
public class XssProperties {

    /**
     * 是否开启
     */
    private boolean enabled = true;

    /**
     * 忽略路径
     */
    private List<String> excludePaths;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<String> getExcludePaths() {
        return excludePaths;
    }

    public void setExcludePaths(List<String> excludePaths) {
        this.excludePaths = excludePaths;
    }
}

```

---

# XSS 工具类（Hutool实现）

```java
package io.github.atengk.xss.util;

import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HtmlUtil;

import java.util.regex.Pattern;

/**
 * XSS 工具类（企业级）
 *
 * 特性：
 * 1. 严格模式（默认）：全量转义
 * 2. 宽松模式：允许部分 HTML
 * 3. 防 script / 事件 / 协议注入
 *
 * @author 孔余
 * @since 2026-01-29
 */
public class XssUtil {

    /**
     * script 标签
     */
    private static final Pattern SCRIPT_PATTERN =
            Pattern.compile("(?i)<\\s*script[^>]*>(.*?)<\\s*/\\s*script\\s*>");

    /**
     * 事件属性（onclick 等）
     */
    private static final Pattern EVENT_PATTERN =
            Pattern.compile("(?i)on\\w+\\s*=\\s*['\\\"]?[^'\\\"]*['\\\"]?");

    /**
     * javascript: 协议
     */
    private static final Pattern JS_PROTOCOL_PATTERN =
            Pattern.compile("(?i)javascript:");

    /**
     * CSS expression
     */
    private static final Pattern CSS_EXPRESSION_PATTERN =
            Pattern.compile("(?i)expression\\s*\\(");

    /**
     * 默认清理（严格模式）
     */
    public static String clean(String value) {
        return clean(value, true);
    }

    /**
     * 清理 XSS
     *
     * @param value 原始数据
     * @param strict 是否严格模式
     * @return 过滤后数据
     */
    public static String clean(String value, boolean strict) {

        if (StrUtil.isBlank(value)) {
            return value;
        }

        String result = value;

        /*
         * Step1：去除危险标签
         */
        result = ReUtil.replaceAll(result, SCRIPT_PATTERN, "");

        /*
         * Step2：去除事件属性
         */
        result = ReUtil.replaceAll(result, EVENT_PATTERN, "");

        /*
         * Step3：去除 javascript 协议
         */
        result = ReUtil.replaceAll(result, JS_PROTOCOL_PATTERN, "");

        /*
         * Step4：去除 CSS 表达式
         */
        result = ReUtil.replaceAll(result, CSS_EXPRESSION_PATTERN, "");

        /*
         * Step5：HTML 处理策略
         */
        if (strict) {
            /*
             * 严格模式：全部转义
             */
            result = HtmlUtil.escape(result);
        } else {
            /*
             * 宽松模式：去标签（保留文本）
             */
            result = HtmlUtil.cleanHtmlTag(result);
        }

        return result;
    }
}
```

---

# RequestWrapper

```java
package io.github.atengk.xss.config;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;
import io.github.atengk.xss.util.XssUtil;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * XSS 请求包装类（企业级）
 *
 * 支持：
 * 1. GET / 表单参数过滤
 * 2. JSON 精准过滤（只处理 value）
 * 3. 流重复读取
 *
 * @author 孔余
 * @since 2026-04-05
 */
public class XssHttpServletRequestWrapper extends HttpServletRequestWrapper {

    /**
     * 请求体缓存
     */
    private byte[] body;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public XssHttpServletRequestWrapper(HttpServletRequest request) {
        super(request);

        String contentType = request.getContentType();

        /*
         * multipart 不处理（避免文件流损坏）
         */
        if (StrUtil.containsIgnoreCase(contentType, "multipart/form-data")) {
            return;
        }

        /*
         * 只处理有 body 的请求
         */
        if (!hasBody(request)) {
            return;
        }

        try {
            String bodyStr = IoUtil.read(request.getInputStream(), StandardCharsets.UTF_8);

            if (StrUtil.isBlank(bodyStr)) {
                return;
            }

            /*
             * JSON 请求
             */
            if (isJson(contentType)) {
                bodyStr = cleanJson(bodyStr);
            } else {
                /*
                 * 普通文本
                 */
                bodyStr = XssUtil.clean(bodyStr);
            }

            this.body = bodyStr.getBytes(StandardCharsets.UTF_8);

        } catch (Exception e) {
            throw new RuntimeException("XSS 处理异常", e);
        }
    }

    /**
     * 判断是否有 body
     */
    private boolean hasBody(HttpServletRequest request) {
        String method = request.getMethod();
        return "POST".equalsIgnoreCase(method)
                || "PUT".equalsIgnoreCase(method)
                || "PATCH".equalsIgnoreCase(method);
    }

    /**
     * 是否 JSON
     */
    private boolean isJson(String contentType) {
        return StrUtil.containsIgnoreCase(contentType, "application/json");
    }

    /**
     * JSON 精准清洗
     */
    private String cleanJson(String json) {
        try {
            JsonNode root = MAPPER.readTree(json);
            cleanNode(root);
            return MAPPER.writeValueAsString(root);
        } catch (Exception e) {
            /*
             * JSON 解析失败直接返回原始值，避免请求报错
             */
            return json;
        }
    }

    /**
     * 递归清洗 JSON
     */
    private void cleanNode(JsonNode node) {

        if (node instanceof ObjectNode objNode) {

            objNode.fieldNames().forEachRemaining(field -> {
                JsonNode value = objNode.get(field);

                if (value.isTextual()) {
                    objNode.put(field, XssUtil.clean(value.asText()));
                } else {
                    cleanNode(value);
                }
            });

        } else if (node instanceof ArrayNode arrayNode) {

            for (int i = 0; i < arrayNode.size(); i++) {
                JsonNode item = arrayNode.get(i);

                if (item.isTextual()) {
                    arrayNode.set(i, TextNode.valueOf(XssUtil.clean(item.asText())));
                } else {
                    cleanNode(item);
                }
            }
        }
    }

    /**
     * GET / 表单参数过滤
     */
    @Override
    public String getParameter(String name) {
        return XssUtil.clean(super.getParameter(name));
    }

    @Override
    public String[] getParameterValues(String name) {
        String[] values = super.getParameterValues(name);
        if (values == null) {
            return null;
        }

        for (int i = 0; i < values.length; i++) {
            values[i] = XssUtil.clean(values[i]);
        }
        return values;
    }

    /**
     * Header 过滤
     */
    @Override
    public String getHeader(String name) {
        return XssUtil.clean(super.getHeader(name));
    }

    /**
     * 重写输入流（支持多次读取）
     */
    @Override
    public ServletInputStream getInputStream() throws IOException {

        if (body == null) {
            return super.getInputStream();
        }

        ByteArrayInputStream inputStream = new ByteArrayInputStream(body);

        return new ServletInputStream() {

            @Override
            public boolean isFinished() {
                return inputStream.available() == 0;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener readListener) {
            }

            @Override
            public int read() {
                return inputStream.read();
            }
        };
    }

    @Override
    public BufferedReader getReader() throws IOException {
        if (body == null) {
            return super.getReader();
        }
        return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
    }
}
```

---

# XSS 过滤器（支持路径匹配）

```java
package io.github.atengk.xss.config;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.io.IOException;

/**
 * XSS 过滤器（企业级）
 * <p>
 * 特性：
 * 1. 支持 * 和 **
 * 2. 支持 context-path
 * 3. URI 标准化（防绕过）
 * 4. Pattern 预编译（提升性能）
 *
 * @author 孔余
 * @since 2026-04-05
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class XssFilter implements Filter {

    private final XssProperties properties;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public XssFilter(XssProperties properties) {
        this.properties = properties;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (!properties.isEnabled()) {
            chain.doFilter(request, response);
            return;
        }

        HttpServletRequest req = (HttpServletRequest) request;

        /*
         * 获取标准化 URI（去除 context-path + 解码）
         */
        String uri = normalizeUri(req);

        /*
         * 忽略路径
         */
        if (isExclude(uri)) {
            chain.doFilter(request, response);
            return;
        }

        /*
         * 包装请求
         */
        XssHttpServletRequestWrapper wrapper =
                new XssHttpServletRequestWrapper(req);

        chain.doFilter(wrapper, response);
    }

    /**
     * URI 标准化（企业级关键点）
     */
    private String normalizeUri(HttpServletRequest request) {

        String uri = request.getRequestURI();

        /*
         * 去掉 context-path
         */
        String contextPath = request.getContextPath();
        if (StrUtil.isNotBlank(contextPath) && uri.startsWith(contextPath)) {
            uri = uri.substring(contextPath.length());
        }

        /*
         * URL 解码（防止 %2F 绕过）
         */
        uri = URLUtil.decode(uri, "UTF-8");

        /*
         * 统一格式
         */
        uri = StrUtil.removeSuffix(uri, "/");

        return uri;
    }

    /**
     * 是否忽略
     */
    private boolean isExclude(String uri) {

        if (CollUtil.isEmpty(properties.getExcludePaths())) {
            return false;
        }

        for (String pattern : properties.getExcludePaths()) {
            if (pathMatcher.match(pattern, uri)) {
                return true;
            }
        }

        return false;
    }
}
```

---

# 接口测试

```java
package io.github.atengk.xss.controller;

import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * XSS 测试接口
 */
@RestController
@RequestMapping("/xss")
public class XssTestController {

    /**
     * GET 请求测试
     * 示例：
     * /xss/get?name=<script>alert(1)</script>
     */
    @GetMapping("/get")
    public String testGet(@RequestParam String name) {
        return "GET 接收参数：" + name;
    }

    /**
     * POST JSON 测试
     */
    @PostMapping("/post")
    public Map<String, Object> testPost(@RequestBody Map<String, Object> body) {
        return body;
    }
}

```

**GET 请求测试**

请求示例

```
curl "http://localhost:8080/xss/get?name=<script>alert(1)</script>"
```

预期结果

```
GET 接收参数：
```

**POST 请求测试（JSON）**

请求示例

```
curl -X POST "http://localhost:8080/xss/post" \
-H "Content-Type: application/json" \
-d '{
  "name": "<script>alert(1)</script>",
  "desc": "<img src=x onerror=alert(2)>"
}'
```

预期返回

```
{
  "name": "",
  "desc": "&lt;img src=x "
}
```

