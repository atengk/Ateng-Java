package local.ateng.java.cloud.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;

/**
 * 网关统一异常处理
 *
 * @author Ateng
 * @email 2385569970@qq.com
 * @since 2025-03-24
 */
@Slf4j
@Order(-1)
@Configuration
public class GatewayExceptionHandler implements ErrorWebExceptionHandler {

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();

        if (exchange.getResponse().isCommitted()) {
            return Mono.error(ex);
        }

        String msg;

        if (ex instanceof NotFoundException) {
            msg = "服务未找到";
        } else if (ex instanceof ResponseStatusException responseStatusException) {
            msg = responseStatusException.getMessage();
        } else {
            msg = "内部服务器错误";
        }

        log.error("[网关异常处理]请求路径:{},异常信息:{}", exchange.getRequest().getPath(), ex.getMessage());

        // 返回
        response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
        LinkedHashMap<String, String> result = new LinkedHashMap<>() {{
            put("code", "-1");
            put("msg", msg);
            put("data", null);
        }};
        DataBuffer dataBuffer = response.bufferFactory().wrap(result.toString().getBytes());
        return response.writeWith(Mono.just(dataBuffer));
    }
}
