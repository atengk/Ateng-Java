package io.github.atengk.http.extension;

import io.github.atengk.http.config.RemoteHttpProperties;
import org.springframework.http.client.ClientHttpRequestFactory;

import java.util.function.Supplier;

/**
 * RestClient 请求工厂扩展器
 *
 * @author Ateng
 * @since 2026-04-29
 */
public interface RemoteRequestFactoryCustomizer {

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
     * 自定义请求工厂
     *
     * @param clientName             客户端名称
     * @param clientConfig           客户端配置
     * @param requestFactorySupplier 默认请求工厂供应器
     * @return 请求工厂
     */
    ClientHttpRequestFactory customize(
            String clientName,
            RemoteHttpProperties.ClientConfig clientConfig,
            Supplier<ClientHttpRequestFactory> requestFactorySupplier
    );

}