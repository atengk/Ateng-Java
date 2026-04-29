package io.github.atengk.http.interceptor;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.http.config.RemoteHttpProperties;
import io.github.atengk.http.constant.RemoteHttpConstant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * RestClient TraceId 拦截器
 *
 * @author Ateng
 * @since 2026-04-29
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RestClientTraceInterceptor implements ClientHttpRequestInterceptor {

    private final RemoteHttpProperties remoteHttpProperties;

    /**
     * 追加 TraceId 请求头
     *
     * @param request   请求对象
     * @param body      请求体
     * @param execution 执行器
     * @return 响应对象
     * @throws IOException IO 异常
     */
    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        RemoteHttpProperties.TraceConfig trace = remoteHttpProperties.getTrace();

        String mdcName = StrUtil.blankToDefault(trace.getMdcName(), RemoteHttpConstant.MDC_TRACE_ID);
        String headerName = StrUtil.blankToDefault(trace.getHeaderName(), RemoteHttpConstant.DEFAULT_TRACE_HEADER);

        String traceId = MDC.get(mdcName);
        if (StrUtil.isBlank(traceId) && Boolean.TRUE.equals(trace.getGenerateIfAbsent())) {
            traceId = IdUtil.fastSimpleUUID();
            MDC.put(mdcName, traceId);
            log.debug("远程调用生成新的 TraceId，traceId={}", traceId);
        }

        if (StrUtil.isNotBlank(traceId)) {
            request.getHeaders().set(headerName, traceId);
        }

        return execution.execute(request, body);
    }

}