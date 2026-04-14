package local.ateng.java.serialize.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jackson Web 层 ObjectMapper 配置。
 *
 * <p>
 * 向 Spring 容器注册自定义 ObjectMapper，用于统一控制 Web（Controller）层
 * 的 JSON 序列化与反序列化行为。
 * </p>
 *
 * <p>
 * 注意：一旦定义该 Bean，会覆盖 Spring Boot 默认的 ObjectMapper，
 * 从而影响整个 Spring Web 的序列化与反序列化策略。
 * </p>
 *
 * @author Ateng
 * @since 2026-04-13
 */
@Configuration
public class WebJacksonConfig {

    /**
     * Web 场景 ObjectMapper
     */
    @Bean
    public ObjectMapper objectMapper() {
        return JacksonObjectMapperFactory.buildWebObjectMapper();
    }

}
