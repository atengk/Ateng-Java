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