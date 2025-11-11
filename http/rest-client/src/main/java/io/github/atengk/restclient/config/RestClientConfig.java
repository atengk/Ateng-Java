package io.github.atengk.restclient.config;

import io.github.atengk.restclient.interceptor.AuthInterceptor;
import io.github.atengk.restclient.interceptor.LoggingInterceptor;
import io.github.atengk.restclient.interceptor.MyRequestInterceptor;
import io.github.atengk.restclient.interceptor.RetryInterceptor;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    /**
     * 定义 RestClient Bean（可直接注入使用）
     *
     * @return RestClient 对象
     */
    @Bean
    public RestClient restClient() {
        return RestClient.builder()
                // 可改成你项目的网关地址
                .baseUrl("https://jsonplaceholder.typicode.com")
                .requestFactory(httpRequestFactory())
                .requestInterceptors(list -> {
                    list.add(new MyRequestInterceptor());
                    list.add(new RetryInterceptor());
                    list.add(new AuthInterceptor());
                    list.add(new LoggingInterceptor());
                })
                .defaultHeader("User-Agent", "SpringBoot3-RestClient")
                .build();
    }

    /**
     * 创建 HttpClient 请求工厂
     *
     * @return ClientHttpRequestFactory 对象
     */
    private ClientHttpRequestFactory httpRequestFactory() {
        return new HttpComponentsClientHttpRequestFactory(httpClient());
    }

    /**
     * 创建 HttpClient 实例
     *
     * @return HttpClient 对象
     */
    private HttpClient httpClient() {
        PoolingHttpClientConnectionManager connectionManager =
                PoolingHttpClientConnectionManagerBuilder.create()
                        // 设置最大连接总数
                        .setMaxConnTotal(200)
                        // 设置每路由最大连接数
                        .setMaxConnPerRoute(50)
                        // 设置空闲连接验证间隔
                        .setValidateAfterInactivity(TimeValue.ofSeconds(30))
                        .build();

        RequestConfig requestConfig = RequestConfig.custom()
                // 连接建立超时
                .setConnectTimeout(Timeout.ofSeconds(5))
                // 响应超时
                .setResponseTimeout(Timeout.ofSeconds(10))
                // 从连接池获取连接超时
                .setConnectionRequestTimeout(Timeout.ofSeconds(2))
                .build();

        // 构建 HttpClient
        return HttpClients.custom()
                .setConnectionManager(connectionManager)
                // 清理过期连接
                .evictExpiredConnections()
                // 清理空闲连接
                .evictIdleConnections(TimeValue.ofSeconds(30))
                .setDefaultRequestConfig(requestConfig)
                .build();
    }
}
