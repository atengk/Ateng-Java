package io.github.atengk.restclient.config;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
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
                // ✅ 可改成你项目的网关地址
                .baseUrl("https://jsonplaceholder.typicode.com")
                .requestFactory(httpRequestFactory())
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
        // 创建连接池管理器
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        // 最大连接数
        connectionManager.setMaxTotal(100);
        // 每个主机的最大连接数
        connectionManager.setDefaultMaxPerRoute(20);

        // 构建 HttpClient
        return HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(org.apache.hc.client5.http.config.RequestConfig.custom()
                        // 连接超时
                        .setConnectTimeout(Timeout.ofSeconds(10))
                        // 响应超时
                        .setResponseTimeout(Timeout.ofSeconds(30))
                        .build())
                .build();
    }
}
