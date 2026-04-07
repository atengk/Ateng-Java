package io.github.atengk.crypto.config;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 解密后请求包装
 *
 * @author 孔余
 * @since 2026-01-29
 */
public class DecryptedHttpServletRequest extends CachedBodyHttpServletRequest {

    private final byte[] newBody;

    public DecryptedHttpServletRequest(HttpServletRequest request, String body) throws IOException {
        super(request);
        this.newBody = body.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public ServletInputStream getInputStream() {

        ByteArrayInputStream inputStream = new ByteArrayInputStream(newBody);

        return new ServletInputStream() {

            @Override
            public int read() {
                return inputStream.read();
            }

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
        };
    }
}
