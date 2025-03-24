package local.ateng.java.cloud.config;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 全局缓存获取body请求数据（解决流不能重复读取问题）
 *
 * @author Ateng
 * @email 2385569970@qq.com
 * @since 2025-03-24
 */
@Component
public class GlobalCacheRequestFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 只缓存json类型请求
        String header = exchange.getRequest().getHeaders().getFirst(HttpHeaders.CONTENT_TYPE);
        if (!StringUtils.startsWithIgnoreCase(header, MediaType.APPLICATION_JSON_VALUE)) {
            return chain.filter(exchange);
        }
        return ServerWebExchangeUtils.cacheRequestBody(exchange, (serverHttpRequest) -> {
            if (serverHttpRequest == exchange.getRequest()) {
                return chain.filter(exchange);
            }
            return chain.filter(exchange.mutate().request(serverHttpRequest).build());
        });
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}
