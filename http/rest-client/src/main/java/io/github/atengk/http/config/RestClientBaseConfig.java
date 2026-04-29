package io.github.atengk.http.config;

import io.github.atengk.http.constant.RemoteHttpConstant;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * RestClient 基础配置
 *
 * @author Ateng
 * @since 2026-04-29
 */
@Configuration
@EnableConfigurationProperties(RemoteHttpProperties.class)
public class RestClientBaseConfig {

    /**
     * 默认 RestClient
     *
     * @param remoteRestClientFactory RestClient 工厂
     * @return RestClient
     */
    @Bean
    public RestClient defaultRestClient(RemoteRestClientFactory remoteRestClientFactory) {
        return remoteRestClientFactory.create(RemoteHttpConstant.CLIENT_DEFAULT);
    }

    /**
     * 快速失败 RestClient
     *
     * @param remoteRestClientFactory RestClient 工厂
     * @return RestClient
     */
    @Bean
    public RestClient fastTimeoutRestClient(RemoteRestClientFactory remoteRestClientFactory) {
        return remoteRestClientFactory.create(RemoteHttpConstant.CLIENT_FAST);
    }

    /**
     * 长超时 RestClient
     *
     * @param remoteRestClientFactory RestClient 工厂
     * @return RestClient
     */
    @Bean
    public RestClient longTimeoutRestClient(RemoteRestClientFactory remoteRestClientFactory) {
        return remoteRestClientFactory.create(RemoteHttpConstant.CLIENT_LONG);
    }

}