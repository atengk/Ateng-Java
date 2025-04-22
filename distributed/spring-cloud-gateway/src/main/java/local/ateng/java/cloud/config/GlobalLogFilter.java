package local.ateng.java.cloud.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ORIGINAL_REQUEST_URL_ATTR;

/**
 * 全局日志过滤器
 * 用于打印请求执行参数与响应时间等等
 *
 * @author Ateng
 * @email 2385569970@qq.com
 * @since 2025-03-24
 */
@Slf4j
@Component
public class GlobalLogFilter implements GlobalFilter, Ordered {

    private static final String START_TIME = "startTime";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        LinkedHashSet<URI> uris = exchange.getAttributeOrDefault(GATEWAY_ORIGINAL_REQUEST_URL_ATTR, new LinkedHashSet<>());
        URI requestUri = uris.stream().findFirst().orElse(request.getURI());
        String path = UriComponentsBuilder.fromPath(requestUri.getRawPath()).build().toUriString();
        String url = request.getMethod().name() + " " + path;

        // 打印请求参数
        String header = exchange.getRequest().getHeaders().getFirst(HttpHeaders.CONTENT_TYPE);
        if (StringUtils.startsWithIgnoreCase(header, MediaType.APPLICATION_JSON_VALUE)) {
            // 从缓存中读取request内的body
            String jsonParam;
            Object obj = exchange.getAttributes().get(ServerWebExchangeUtils.CACHED_REQUEST_BODY_ATTR);
            DataBuffer buffer = (DataBuffer) obj;
            try (DataBuffer.ByteBufferIterator iterator = buffer.readableByteBuffers()) {
                CharBuffer charBuffer = StandardCharsets.UTF_8.decode(iterator.next());
                jsonParam = charBuffer.toString();
            }
            log.info("[Ateng]开始请求 => URL[{}],参数类型[json],参数:[{}]", url, jsonParam);
        } else {
            MultiValueMap<String, String> parameterMap = request.getQueryParams();
            log.info("[Ateng]开始请求 => URL[{}],param={}", url, parameterMap);
        }
        exchange.getAttributes().put(START_TIME, System.currentTimeMillis());
        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            Long startTime = exchange.getAttribute(START_TIME);
            if (startTime != null) {
                long executeTime = (System.currentTimeMillis() - startTime);
                log.info("[Ateng]结束请求 => URL[{}],耗时:[{}]毫秒", url, executeTime);
            }
        }));
    }

    @Override
    public int getOrder() {
        // 日志处理器在负载均衡器之后执行 负载均衡器会导致线程切换 无法获取上下文内容
        // 如需在日志内操作线程上下文 例如获取登录用户数据等 可以打开下方注释代码
        // return ReactiveLoadBalancerClientFilter.LOAD_BALANCER_CLIENT_FILTER_ORDER - 1;
        return Ordered.LOWEST_PRECEDENCE;
    }

}
