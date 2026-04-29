package io.github.atengk.http.handler;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.http.config.RemoteHttpProperties;
import io.github.atengk.http.exception.RemoteCallException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 远程 HTTP 错误处理器
 *
 * @author Ateng
 * @since 2026-04-29
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RemoteHttpErrorHandler implements RestClient.ResponseSpec.ErrorHandler {

    private final RemoteHttpProperties remoteHttpProperties;

    /**
     * 处理异常响应
     *
     * @param request  请求对象
     * @param response 响应对象
     * @throws IOException IO 异常
     */
    @Override
    public void handle(HttpRequest request, ClientHttpResponse response) throws IOException {
        String responseBody = readResponseBody(response);
        String message = StrUtil.format(
                "远程接口调用失败，method={}，uri={}，status={}",
                request.getMethod(),
                request.getURI(),
                response.getStatusCode()
        );

        log.warn("{}，responseBody={}", message, responseBody);

        throw new RemoteCallException(
                message,
                request.getURI(),
                response.getStatusCode(),
                responseBody
        );
    }

    /**
     * 读取错误响应体
     *
     * @param response 响应对象
     * @return 错误响应体
     * @throws IOException IO 异常
     */
    private String readResponseBody(ClientHttpResponse response) throws IOException {
        String responseBody = IoUtil.read(response.getBody(), StandardCharsets.UTF_8);
        int maxLength = remoteHttpProperties.getErrorHandling().getMaxResponseBodyLength();

        if (maxLength <= 0) {
            return "";
        }

        return CharSequenceUtil.subPre(responseBody, maxLength);
    }

}