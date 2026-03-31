package io.github.atengk.filter.filter;


import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;

/**
 * 请求日志过滤器
 *
 * 功能：
 * 1. 记录请求 URL、Method、Header、Body
 * 2. 记录响应状态码
 * 3. 统计接口耗时
 *
 * 注意：
 * 1. 支持 Body 可重复读取
 * 2. 避免流只能读取一次的问题
 *
 * @author atengk
 */
@Component
public class RequestLoggingFilter implements Filter {

    /**
     * 请求体缓存包装类
     */
    private static class CachedBodyHttpServletRequest extends jakarta.servlet.http.HttpServletRequestWrapper {

        /**
         * 缓存请求体
         */
        private final byte[] cachedBody;

        /**
         * 构造方法
         *
         * @param request 原始请求
         * @throws IOException IO异常
         */
        public CachedBodyHttpServletRequest(HttpServletRequest request) throws IOException {
            super(request);
            this.cachedBody = request.getInputStream().readAllBytes();
        }

        /**
         * 重写输入流
         *
         * @return ServletInputStream
         */
        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(this.cachedBody);
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

        /**
         * 重写 reader
         *
         * @return BufferedReader
         */
        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }

        /**
         * 获取请求体字符串
         *
         * @return 请求体
         */
        public String getBody() {
            return new String(this.cachedBody, StandardCharsets.UTF_8);
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        long startTime = System.currentTimeMillis();

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        CachedBodyHttpServletRequest wrappedRequest = new CachedBodyHttpServletRequest(httpRequest);

        String requestUri = httpRequest.getRequestURI();
        String method = httpRequest.getMethod();

        StringBuilder headerBuilder = new StringBuilder();
        Enumeration<String> headerNames = httpRequest.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            String headerValue = httpRequest.getHeader(headerName);
            headerBuilder.append(headerName)
                    .append(": ")
                    .append(headerValue)
                    .append("; ");
        }

        String requestBody = wrappedRequest.getBody();

        System.out.println("========== Request Start ==========");
        System.out.println("URI: " + requestUri);
        System.out.println("Method: " + method);
        System.out.println("Headers: " + headerBuilder);
        System.out.println("Body: " + requestBody);

        chain.doFilter(wrappedRequest, response);

        int status = httpResponse.getStatus();
        long duration = System.currentTimeMillis() - startTime;

        System.out.println("Status: " + status);
        System.out.println("Duration(ms): " + duration);
        System.out.println("========== Request End ==========");
    }
}