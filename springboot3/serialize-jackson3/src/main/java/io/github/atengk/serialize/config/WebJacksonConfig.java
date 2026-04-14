package io.github.atengk.serialize.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

/**
 * Jackson Web 层 JsonMapper 配置。
 *
 * <p>
 * 向 Spring 容器注册自定义 JsonMapper，用于统一控制 Web（Controller）层
 * 的 JSON 序列化与反序列化行为。
 * </p>
 *
 * <p>
 * 该配置会覆盖 Spring Boot 默认的 Jackson 3 自动配置结果。
 * </p>
 *
 * @author Ateng
 * @since 2026-04-13
 */
@Configuration
public class WebJacksonConfig {

    /**
     * Web 场景 JsonMapper。
     *
     * @return Web 场景 JsonMapper
     */
    @Bean
    public JsonMapper jsonMapper() {
        return JacksonJsonMapperFactory.buildWebJsonMapper();
    }

}