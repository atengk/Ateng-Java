package io.github.atengk.serialize.config;

import io.github.atengk.serialize.common.jackson.JacksonJsonMapperFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import tools.jackson.databind.json.JsonMapper;

/**
 * Jackson 基础序列化组件配置。
 * <p>
 * 定义应用上下文中的默认 {@link JsonMapper} 实例，作为非 Spring MVC
 * HTTP Message Conversion 链路的通用 JSON 编解码基础设施。
 * </p>
 * <p>
 * 该配置不参与 Servlet Web 栈中的请求体读取与响应体写出，避免应用级
 * JSON 编解码策略与 Web API 契约级 JSON 编解码策略产生隐式耦合。
 * </p>
 *
 * @author Ateng
 * @since 2026-05-01
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
public class JsonMapperConfig {

    /**
     * 声明应用级默认 JsonMapper。
     * <p>
     * 通过 {@link Primary} 指定类型注入优先级，使容器中存在多个
     * {@link JsonMapper} 实例时，未显式限定名称的依赖解析优先绑定该实例。
     * </p>
     *
     * @return 应用级默认 JsonMapper
     */
    @Bean
    @Primary
    public JsonMapper JsonMapper() {
        log.info("初始化应用级默认 Jackson JsonMapper");
        return JacksonJsonMapperFactory.buildDefaultJsonMapper();
    }

}