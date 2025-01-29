package local.ateng.java.http.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.*;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class WebClientConfig {
    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient
                .builder()
                .filter(logRequest())  // 添加请求日志
                .filter(logResponse()) // 添加响应日志
                .filter(authHeaderFilter()) // 添加认证头
                .filter(retryOnError())  // 添加重试逻辑
                ;
    }

    // 请求日志记录
    private ExchangeFilterFunction logRequest() {
        return (request, next) -> {
            log.info("Request: " + request.method() + " " + request.url());
            request.headers().forEach((name, values) ->
                    values.forEach(value -> log.info(name + ": " + value)));
            return next.exchange(request);
        };
    }

    // 响应日志记录
    private ExchangeFilterFunction logResponse() {
        return (request, next) -> next.exchange(request)
                .doOnTerminate(() -> {
                    log.info("Response: " + request.method() + " " + request.url());
                });
    }

    // 认证头过滤器
    private ExchangeFilterFunction authHeaderFilter() {
        return (request, next) -> {
            ClientRequest modifiedRequest = ClientRequest.from(request)
                    .header("Authorization", "Bearer 1234567890")  // 添加 Authorization 头
                    .build();
            return next.exchange(modifiedRequest);
        };
    }

    // 重试过滤器
    public ExchangeFilterFunction retryOnError() {
        return (request, next) -> attemptRequest(request, next, new AtomicInteger(0));
    }

    private Mono<ClientResponse> attemptRequest(ClientRequest request, ExchangeFunction next, AtomicInteger retryCount) {
        return next.exchange(request)
                .flatMap(response -> shouldRetry(response)
                        .flatMap(shouldRetry -> {
                            if (shouldRetry && retryCount.incrementAndGet() <= 3) {
                                log.info("Retrying: " + request.method() + " " + request.url());
                                return Mono.delay(Duration.ofSeconds(2))
                                        .flatMap(aLong -> attemptRequest(request, next, retryCount));
                            }
                            return Mono.just(response);
                        }));
    }

    private Mono<Boolean> shouldRetry(ClientResponse response) {
        if (response.statusCode().is5xxServerError()) {
            return Mono.just(true); // 服务器错误，直接重试
        }
        if (response.statusCode().is4xxClientError()) {
            return response.bodyToMono(String.class)
                    .map(body -> {
                        // 解析 body，判断是否需要重试
                        return body.contains("ok");
                    }).defaultIfEmpty(false); // 防止 body 为空时报错
        }
        return Mono.just(false); // 其他情况不重试
    }


}

