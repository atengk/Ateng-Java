package io.github.atengk.http.interceptor;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.http.config.RemoteHttpProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * RestClient 日志拦截器
 *
 * @author Ateng
 * @since 2026-04-29
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RestClientLogInterceptor implements ClientHttpRequestInterceptor {

    private static final String MASK_VALUE = "******";

    private final RemoteHttpProperties remoteHttpProperties;

    /**
     * 记录远程 HTTP 调用日志
     *
     * @param request   请求对象
     * @param body      请求体
     * @param execution 执行器
     * @return 响应对象
     * @throws IOException IO 异常
     */
    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        long startTime = System.nanoTime();

        try {
            ClientHttpResponse response = execution.execute(request, body);
            long costMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);

            log.info(
                    "远程接口调用完成，method={}，uri={}，status={}，cost={}ms，headers={}，requestBody={}",
                    request.getMethod(),
                    request.getURI(),
                    response.getStatusCode(),
                    costMillis,
                    buildHeadersLog(request.getHeaders()),
                    buildRequestBodyLog(body)
            );

            return response;
        } catch (IOException ex) {
            long costMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);

            log.warn(
                    "远程接口调用IO异常，method={}，uri={}，cost={}ms，message={}",
                    request.getMethod(),
                    request.getURI(),
                    costMillis,
                    ex.getMessage()
            );

            throw ex;
        } catch (RuntimeException ex) {
            long costMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);

            log.warn(
                    "远程接口调用运行时异常，method={}，uri={}，cost={}ms，message={}",
                    request.getMethod(),
                    request.getURI(),
                    costMillis,
                    ex.getMessage()
            );

            throw ex;
        }
    }

    /**
     * 构建请求头日志
     *
     * @param headers 请求头
     * @return 请求头日志
     */
    private Object buildHeadersLog(HttpHeaders headers) {
        RemoteHttpProperties.LoggingConfig logging = remoteHttpProperties.getLogging();
        if (!Boolean.TRUE.equals(logging.getIncludeHeaders())) {
            return "disabled";
        }

        Map<String, List<String>> result = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            String headerName = entry.getKey();
            if (isSensitiveHeader(headerName, logging.getSensitiveHeaders())) {
                result.put(headerName, List.of(MASK_VALUE));
            } else {
                result.put(headerName, entry.getValue());
            }
        }
        return result;
    }

    /**
     * 构建请求体日志
     *
     * @param body 请求体字节数组
     * @return 请求体日志
     */
    private String buildRequestBodyLog(byte[] body) {
        RemoteHttpProperties.LoggingConfig logging = remoteHttpProperties.getLogging();
        if (!Boolean.TRUE.equals(logging.getIncludeRequestBody())) {
            return "disabled";
        }
        if (body == null || body.length == 0) {
            return "";
        }

        int maxLength = logging.getMaxRequestBodyLength();
        if (maxLength <= 0) {
            return "";
        }

        String text = StrUtil.str(body, StandardCharsets.UTF_8);
        return CharSequenceUtil.subPre(text, maxLength);
    }

    /**
     * 判断是否敏感请求头
     *
     * @param headerName       请求头名称
     * @param sensitiveHeaders 敏感请求头列表
     * @return 是否敏感
     */
    private boolean isSensitiveHeader(String headerName, List<String> sensitiveHeaders) {
        if (StrUtil.isBlank(headerName) || CollUtil.isEmpty(sensitiveHeaders)) {
            return false;
        }

        String lowerHeaderName = headerName.toLowerCase(Locale.ROOT);
        return sensitiveHeaders.stream()
                .filter(StrUtil::isNotBlank)
                .map(item -> item.toLowerCase(Locale.ROOT))
                .anyMatch(item -> StrUtil.equals(lowerHeaderName, item));
    }

}