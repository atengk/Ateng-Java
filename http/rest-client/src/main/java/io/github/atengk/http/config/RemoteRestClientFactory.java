package io.github.atengk.http.config;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.http.extension.RemoteRequestFactoryCustomizer;
import io.github.atengk.http.extension.RemoteRestClientBuilderCustomizer;
import io.github.atengk.http.handler.RemoteHttpErrorHandler;
import io.github.atengk.http.interceptor.RestClientLogInterceptor;
import io.github.atengk.http.interceptor.RestClientTraceInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 远程 RestClient 工厂
 *
 * @author Ateng
 * @since 2026-04-29
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RemoteRestClientFactory implements DisposableBean {

    private final RestClient.Builder restClientBuilder;

    private final RemoteHttpProperties remoteHttpProperties;

    private final RemoteHttpErrorHandler remoteHttpErrorHandler;

    private final RestClientLogInterceptor restClientLogInterceptor;

    private final RestClientTraceInterceptor restClientTraceInterceptor;

    private final List<RemoteRequestFactoryCustomizer> requestFactoryCustomizers;

    private final List<RemoteRestClientBuilderCustomizer> builderCustomizers;

    private final List<CloseableHttpClient> httpClients = new CopyOnWriteArrayList<>();

    /**
     * 创建 RestClient
     *
     * @param clientName 客户端名称
     * @return RestClient
     */
    public RestClient create(String clientName) {
        return create(clientName, null);
    }

    /**
     * 创建 RestClient
     *
     * @param clientName 客户端名称
     * @param customizer 单个客户端的临时扩展逻辑
     * @return RestClient
     */
    public RestClient create(String clientName, Consumer<RestClient.Builder> customizer) {
        RemoteHttpProperties.ClientConfig clientConfig = remoteHttpProperties.getMergedClient(clientName);

        RestClient.Builder builder = restClientBuilder.clone()
                .requestFactory(createRequestFactory(clientName, clientConfig));

        applyBaseUrl(builder, clientConfig);
        applyDefaultHeaders(builder, clientConfig);
        applyErrorHandler(builder);
        applyTraceInterceptor(builder);
        applyLogInterceptor(builder);
        applyBuilderCustomizers(clientName, clientConfig, builder);

        if (customizer != null) {
            customizer.accept(builder);
        }

        log.info(
                "RestClient 创建完成，clientName={}，baseUrl={}，connectTimeout={}，responseTimeout={}",
                clientName,
                clientConfig.getBaseUrl(),
                clientConfig.getConnectTimeout(),
                clientConfig.getResponseTimeout()
        );

        return builder.build();
    }

    /**
     * 设置基础地址
     *
     * @param builder      RestClient Builder
     * @param clientConfig 客户端配置
     */
    private void applyBaseUrl(RestClient.Builder builder, RemoteHttpProperties.ClientConfig clientConfig) {
        if (StrUtil.isNotBlank(clientConfig.getBaseUrl())) {
            builder.baseUrl(clientConfig.getBaseUrl());
        }
    }

    /**
     * 设置默认请求头
     *
     * @param builder      RestClient Builder
     * @param clientConfig 客户端配置
     */
    private void applyDefaultHeaders(RestClient.Builder builder, RemoteHttpProperties.ClientConfig clientConfig) {
        if (MapUtil.isEmpty(clientConfig.getDefaultHeaders())) {
            return;
        }

        for (Map.Entry<String, String> entry : clientConfig.getDefaultHeaders().entrySet()) {
            builder.defaultHeader(entry.getKey(), entry.getValue());
        }
    }

    /**
     * 设置统一错误处理器
     *
     * @param builder RestClient Builder
     */
    private void applyErrorHandler(RestClient.Builder builder) {
        if (Boolean.TRUE.equals(remoteHttpProperties.getErrorHandling().getEnabled())) {
            builder.defaultStatusHandler(HttpStatusCode::isError, remoteHttpErrorHandler);
        }
    }

    /**
     * 设置 TraceId 拦截器
     *
     * @param builder RestClient Builder
     */
    private void applyTraceInterceptor(RestClient.Builder builder) {
        if (Boolean.TRUE.equals(remoteHttpProperties.getTrace().getEnabled())) {
            builder.requestInterceptor(restClientTraceInterceptor);
        }
    }

    /**
     * 设置日志拦截器
     *
     * @param builder RestClient Builder
     */
    private void applyLogInterceptor(RestClient.Builder builder) {
        if (Boolean.TRUE.equals(remoteHttpProperties.getLogging().getEnabled())) {
            builder.requestInterceptor(restClientLogInterceptor);
        }
    }

    /**
     * 应用 Builder 扩展器
     *
     * @param clientName   客户端名称
     * @param clientConfig 客户端配置
     * @param builder      RestClient Builder
     */
    private void applyBuilderCustomizers(
            String clientName,
            RemoteHttpProperties.ClientConfig clientConfig,
            RestClient.Builder builder
    ) {
        for (RemoteRestClientBuilderCustomizer builderCustomizer : builderCustomizers) {
            if (builderCustomizer.supports(clientName, clientConfig)) {
                builderCustomizer.customize(clientName, clientConfig, builder);
            }
        }
    }

    /**
     * 创建请求工厂
     *
     * @param clientName   客户端名称
     * @param clientConfig 客户端配置
     * @return 请求工厂
     */
    private ClientHttpRequestFactory createRequestFactory(
            String clientName,
            RemoteHttpProperties.ClientConfig clientConfig
    ) {
        Supplier<ClientHttpRequestFactory> supplier = () -> createDefaultRequestFactory(clientConfig);

        for (RemoteRequestFactoryCustomizer requestFactoryCustomizer : requestFactoryCustomizers) {
            if (requestFactoryCustomizer.supports(clientName, clientConfig)) {
                Supplier<ClientHttpRequestFactory> previousSupplier = supplier;
                supplier = () -> requestFactoryCustomizer.customize(clientName, clientConfig, previousSupplier);
            }
        }

        return supplier.get();
    }

    /**
     * 创建默认请求工厂
     *
     * @param clientConfig 客户端配置
     * @return 请求工厂
     */
    private ClientHttpRequestFactory createDefaultRequestFactory(RemoteHttpProperties.ClientConfig clientConfig) {
        ConnectionConfig connectionConfig = ConnectionConfig.custom()
                .setConnectTimeout(toTimeout(clientConfig.getConnectTimeout()))
                .build();

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(toTimeout(clientConfig.getConnectionRequestTimeout()))
                .setResponseTimeout(toTimeout(clientConfig.getResponseTimeout()))
                .build();

        HttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setDefaultConnectionConfig(connectionConfig)
                .setMaxConnTotal(clientConfig.getMaxConnTotal())
                .setMaxConnPerRoute(clientConfig.getMaxConnPerRoute())
                .build();

        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .evictExpiredConnections()
                .evictIdleConnections(TimeValue.ofMilliseconds(clientConfig.getMaxIdleTime().toMillis()))
                .disableAutomaticRetries()
                .build();

        httpClients.add(httpClient);

        return new HttpComponentsClientHttpRequestFactory(httpClient);
    }

    /**
     * 转换超时时间
     *
     * @param duration Duration
     * @return Timeout
     */
    private Timeout toTimeout(Duration duration) {
        return Timeout.ofMilliseconds(duration.toMillis());
    }

    /**
     * 销毁底层 HTTP 客户端
     */
    @Override
    public void destroy() {
        for (CloseableHttpClient httpClient : httpClients) {
            try {
                httpClient.close();
            } catch (IOException ex) {
                log.warn("关闭 HttpClient 失败，message={}", ex.getMessage());
            }
        }
    }

}