package io.github.atengk.serialize.config;

import io.github.atengk.serialize.common.jackson.JacksonJsonMapperFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.http.converter.autoconfigure.ServerHttpMessageConvertersCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

/**
 * Spring MVC Jackson JSON 消息转换器配置。
 * <p>
 * 定义 Servlet Web 应用中的服务端 JSON HTTP 消息转换器定制器，
 * 用于 Spring MVC 在 HandlerMethod 参数解析与返回值处理阶段执行 JSON
 * HTTP 消息体编解码。
 * </p>
 * <p>
 * 该配置显式绑定 Web API 契约级 {@link JsonMapper}，使 HTTP 边界层的
 * JSON 序列化、反序列化规则与应用级默认 JsonMapper 解耦。
 * </p>
 *
 * @author Ateng
 * @since 2026-05-01
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class JacksonHttpMessageConverterConfig {

    /**
     * 定制 Spring MVC 服务端 JSON HTTP 消息转换器。
     * <p>
     * Spring Boot 4 中不再建议直接声明旧版 Jackson 2 的
     * MappingJackson2HttpMessageConverter，而是使用 Spring Framework 7
     * 提供的 JacksonJsonHttpMessageConverter，并通过
     * ServerHttpMessageConvertersCustomizer 替换默认 JSON 转换器。
     * </p>
     *
     * @return 服务端 HTTP 消息转换器定制器
     */
    @Bean
    public ServerHttpMessageConvertersCustomizer jacksonServerHttpMessageConvertersCustomizer() {
        return builder -> {
            log.info("初始化 Spring MVC Jackson JSON HTTP 消息转换器");

            JsonMapper jsonMapper = JacksonJsonMapperFactory.buildWebJsonMapper();

            JacksonJsonHttpMessageConverter converter = new JacksonJsonHttpMessageConverter(jsonMapper);
            converter.setSupportedMediaTypes(List.of(
                    MediaType.APPLICATION_JSON,
                    new MediaType("application", "*+json")
            ));

            builder.withJsonConverter(converter);
        };
    }

}