package io.github.atengk.restclient.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;


/**
 * RestClient 拦截器：记录请求和响应的详细日志
 *
 * @author 孔余
 * @since 2025-07-30
 */
@Component
@Slf4j
public class LoggingInterceptor implements ClientHttpRequestInterceptor {

    /**
     * 拦截请求并打印日志
     *
     * @param request   当前请求
     * @param body      请求体
     * @param execution 拦截器执行器（用于传递调用链）
     * @return 响应结果
     * @throws IOException IO 异常
     */
    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {

        long startTime = System.currentTimeMillis();

        log.info("====== 请求开始 ======");
        log.info("请求地址: {}", request.getURI());
        log.info("请求方式: {}", request.getMethod());
        log.info("请求头: {}", request.getHeaders());
        // 记录请求体，仅限于有请求体的情况（如 POST、PUT）
        if (body != null && body.length > 0) {
            log.info("请求体: {}", new String(body, StandardCharsets.UTF_8));
        } else {
            log.info("请求体: 无");
        }
        ClientHttpResponse response;
        try {
            // 执行请求
            response = execution.execute(request, body);
        } catch (IOException e) {
            log.error("请求执行异常: {}", e.getMessage(), e);
            throw e;
        }

        // 包装响应体，避免响应流只能读取一次的问题
        ClientHttpResponse wrappedResponse = new BufferingClientHttpResponseWrapper(response);
        String responseBody = StreamUtils.copyToString(wrappedResponse.getBody(), StandardCharsets.UTF_8);

        log.info("响应状态: {}", wrappedResponse.getStatusCode());
        log.info("响应头: {}", wrappedResponse.getHeaders());
        log.info("响应体: {}", responseBody);
        log.info("耗时: {} ms", System.currentTimeMillis() - startTime);
        log.info("====== 请求结束 ======");

        return wrappedResponse;
    }

    /**
     * 响应包装类，用于缓存响应体以便多次读取
     */
    private static class BufferingClientHttpResponseWrapper implements ClientHttpResponse {

        private final ClientHttpResponse response;
        private byte[] body;

        public BufferingClientHttpResponseWrapper(ClientHttpResponse response) throws IOException {
            this.response = response;
            this.body = StreamUtils.copyToByteArray(response.getBody());
        }

        @Override
        public HttpStatusCode getStatusCode() throws IOException {
            return response.getStatusCode();
        }

        @Override
        public String getStatusText() throws IOException {
            return response.getStatusText();
        }

        @Override
        public void close() {
            response.close();
        }

        @Override
        public org.springframework.http.HttpHeaders getHeaders() {
            return response.getHeaders();
        }

        @Override
        public java.io.InputStream getBody() {
            return new ByteArrayInputStream(body);
        }
    }
}
