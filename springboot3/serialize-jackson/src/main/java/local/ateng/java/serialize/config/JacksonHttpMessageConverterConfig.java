package local.ateng.java.serialize.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import local.ateng.java.serialize.common.jackson.JacksonObjectMapperFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import java.util.List;

/**
 * Spring MVC Jackson 消息转换器配置。
 * <p>
 * 定义 Servlet Web 应用中的 {@link MappingJackson2HttpMessageConverter}，
 * 用于 Spring MVC 在 HandlerMethod 参数解析与返回值处理阶段执行 JSON
 * HTTP 消息体编解码。
 * </p>
 * <p>
 * 该配置显式绑定 Web API 契约级 {@link ObjectMapper}，使 HTTP 边界层的
 * JSON 序列化、反序列化规则与应用级默认 {@link ObjectMapper} 解耦。
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
     * 声明 Spring MVC Jackson HTTP 消息转换器。
     * <p>
     * 该转换器覆盖 MVC JSON 消息体处理链路中的 Jackson 编解码策略，
     * 适用于 {@code @RequestBody}、{@code @ResponseBody} 以及
     * {@code @RestController} 返回值的 JSON 读写。
     * </p>
     *
     * @return Spring MVC Jackson HTTP 消息转换器
     */
    @Bean
    public MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter() {
        log.info("初始化 Spring MVC Jackson HTTP 消息转换器");

        ObjectMapper objectMapper = JacksonObjectMapperFactory.buildWebObjectMapper();

        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter(objectMapper);
        converter.setSupportedMediaTypes(List.of(
                MediaType.APPLICATION_JSON,
                new MediaType("application", "*+json")
        ));

        return converter;
    }

}