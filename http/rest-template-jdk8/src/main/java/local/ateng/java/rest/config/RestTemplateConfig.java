package local.ateng.java.rest.config;

import lombok.RequiredArgsConstructor;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplate 配置类
 *
 * <p>该配置类主要用于统一管理 RestTemplate 的实例化与底层 HTTP 客户端配置，
 * 包括连接池管理、超时时间设置等。通过将参数抽取为全局常量，避免魔法值，
 * 提升可维护性和可扩展性。</p>
 *
 * <p>本配置基于 Spring Boot 2.7 环境，适用于需要高并发访问外部接口的场景。</p>
 *
 * @author 孔余
 * @since 2025-09-10
 */
@Configuration
@RequiredArgsConstructor
public class RestTemplateConfig {

    /**
     * 连接建立超时时间（毫秒）
     */
    private static final int CONNECT_TIMEOUT = 5000;

    /**
     * 读取数据超时时间（毫秒）
     */
    private static final int READ_TIMEOUT = 10000;

    /**
     * 从连接池获取连接的超时时间（毫秒）
     */
    private static final int CONNECTION_REQUEST_TIMEOUT = 3000;

    /**
     * 连接池最大连接数
     */
    private static final int MAX_TOTAL_CONNECTIONS = 100;

    /**
     * 每个路由（目标主机）的最大连接数
     */
    private static final int MAX_CONNECTIONS_PER_ROUTE = 20;

    /**
     * 构建并注册 RestTemplate Bean
     *
     * <p>该方法会配置底层的 {@link HttpComponentsClientHttpRequestFactory}，
     * 并使用 {@link PoolingHttpClientConnectionManager} 来管理连接池。
     * 最终生成的 RestTemplate 实例可直接在业务中通过注入方式使用。</p>
     *
     * @return 配置完成的 RestTemplate 实例
     */
    @Bean("atengRestTemplate")
    public RestTemplate atengRestTemplate() {
        // 创建 HTTP 请求工厂
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT);
        factory.setReadTimeout(READ_TIMEOUT);
        factory.setConnectionRequestTimeout(CONNECTION_REQUEST_TIMEOUT);

        // 创建连接池管理器
        PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager();
        connManager.setMaxTotal(MAX_TOTAL_CONNECTIONS);
        connManager.setDefaultMaxPerRoute(MAX_CONNECTIONS_PER_ROUTE);

        // 构建 HttpClient 并注入连接池管理器
        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(connManager)
                .build();

        // 将 HttpClient 设置到请求工厂中
        factory.setHttpClient(httpClient);

        // 创建 RestTemplate
        RestTemplate restTemplate = new RestTemplate(factory);

        // 创建并注册拦截器
        /*List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>();
        interceptors.add(authInterceptor);
        interceptors.add(loggingInterceptor);
        interceptors.add(retryInterceptor);
        restTemplate.setInterceptors(interceptors);*/

        // 返回配置完成的 RestTemplate
        return restTemplate;
    }
}
