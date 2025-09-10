package local.ateng.java.rest.config;

import local.ateng.java.rest.interceptor.AuthInterceptor;
import local.ateng.java.rest.interceptor.LoggingInterceptor;
import local.ateng.java.rest.interceptor.MyRequestInterceptor;
import local.ateng.java.rest.interceptor.RetryInterceptor;
import lombok.RequiredArgsConstructor;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;
import java.util.ArrayList;
import java.util.List;

/**
 * RestTemplate配置文件
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-02-02
 */
//@Configuration
@RequiredArgsConstructor
public class RestTemplateTempConfig {
    private final MyRequestInterceptor myRequestInterceptor;
    private final LoggingInterceptor loggingInterceptor;
    private final AuthInterceptor authInterceptor;
    private final RetryInterceptor retryInterceptor;

    /*@Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }*/

    @Bean("myRestTemplate")
    public RestTemplate myRestTemplate() {
        // 创建 RestTemplate
        RestTemplate restTemplate = new RestTemplate();

        // 创建并注册拦截器
        List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>();
        interceptors.add(authInterceptor);
        interceptors.add(loggingInterceptor);
        interceptors.add(retryInterceptor);

        restTemplate.setInterceptors(interceptors);
        return restTemplate;
    }

    /**
     * 构建支持连接池的 RestTemplate Bean。
     * 设置连接超时、读取超时等参数，适合生产环境使用。
     *
     * @return 配置后的 RestTemplate 实例
     */
    @Bean
    public RestTemplate restTemplate() {
        // 创建底层 HTTP 请求工厂
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();

        // 设置连接超时时间（单位：毫秒），连接建立时限
        factory.setConnectTimeout(5000);

        // 设置读取超时时间（单位：毫秒），服务器响应时限
        factory.setReadTimeout(10000);

        // 设置从连接池获取连接的超时时间（单位：毫秒）
        factory.setConnectionRequestTimeout(3000);

        // 创建连接池管理器
        PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager();

        // 设置最大连接数（整个连接池）
        connManager.setMaxTotal(100);

        // 设置每个路由（目标主机）上的最大连接数
        connManager.setDefaultMaxPerRoute(20);

        // 构建 HttpClient 并注入连接池管理器
        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(connManager)
                .build();

        // 将 HttpClient 设置到请求工厂中
        factory.setHttpClient(httpClient);

        // 返回使用自定义请求工厂的 RestTemplate 实例
        return new RestTemplate(factory);
    }


    /**
     * 创建一个忽略 HTTPS 证书校验的 HttpClient，仅用于测试环境。
     *
     * @return CloseableHttpClient
     */
    public static CloseableHttpClient createIgnoreSSLHttpClient() {
        try {
            SSLContext sslContext = SSLContextBuilder.create()
                    .loadTrustMaterial((chain, authType) -> true) // 信任所有
                    .build();

            SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(
                    sslContext, NoopHostnameVerifier.INSTANCE);

            return HttpClients.custom()
                    .setSSLSocketFactory(socketFactory)
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("创建忽略证书校验 HttpClient 失败", e);
        }
    }

    @Bean
    public RestTemplate unsafeRestTemplate() {
        // 使用忽略证书校验的 HttpClient
        CloseableHttpClient httpClient = createIgnoreSSLHttpClient();

        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setHttpClient(httpClient);

        return new RestTemplate(factory);
    }



}
