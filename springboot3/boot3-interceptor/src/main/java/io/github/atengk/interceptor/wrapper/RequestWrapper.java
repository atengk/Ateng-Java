package io.github.atengk.interceptor.wrapper;


import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * HttpServletRequest 包装类
 * 解决 request.getInputStream() 只能读取一次的问题
 */
public class RequestWrapper extends HttpServletRequestWrapper {

    private final byte[] body;

    public RequestWrapper(HttpServletRequest request) throws IOException {
        super(request);

        // 读取请求体并缓存
        InputStream is = request.getInputStream();
        this.body = is.readAllBytes();
    }

    /**
     * 重写输入流
     */
    @Override
    public ServletInputStream getInputStream() {
        ByteArrayInputStream bis = new ByteArrayInputStream(body);

        return new ServletInputStream() {
            @Override
            public boolean isFinished() {
                return bis.available() == 0;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener listener) {}

            @Override
            public int read() {
                return bis.read();
            }
        };
    }

    /**
     * 获取请求体字符串
     */
    public String getBody() {
        return new String(body, StandardCharsets.UTF_8);
    }
}
