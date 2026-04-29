package io.github.atengk.http.extension;

import io.github.atengk.http.config.RemoteHttpProperties;
import org.springframework.web.client.RestClient;

/**
 * RestClient Builder 扩展器
 *
 * @author Ateng
 * @since 2026-04-29
 */
public interface RemoteRestClientBuilderCustomizer {

    /**
     * 是否支持当前客户端
     *
     * @param clientName   客户端名称
     * @param clientConfig 客户端配置
     * @return 是否支持
     */
    default boolean supports(String clientName, RemoteHttpProperties.ClientConfig clientConfig) {
        return true;
    }

    /**
     * 自定义 RestClient Builder
     *
     * @param clientName   客户端名称
     * @param clientConfig 客户端配置
     * @param builder      RestClient Builder
     */
    void customize(String clientName, RemoteHttpProperties.ClientConfig clientConfig, RestClient.Builder builder);

}