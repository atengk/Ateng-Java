package local.ateng.java.redis.config;

import lombok.RequiredArgsConstructor;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JacksonCodec;
import org.redisson.codec.JsonCodec;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

/**
 * Redisson 自动配置
 * 用于根据配置文件创建 RedissonClient，并提供 RedisJSON 使用的 JsonCodec。
 *
 * @author Ateng
 * @since 2026-04-27
 */
@Configuration
@RequiredArgsConstructor
public class RedissonConfig {

    /**
     * Redisson 配置属性。
     */
    private final RedissonProperties redissonProperties;

    /**
     * 创建 Redisson 客户端。
     * <p>
     * 这里从配置文件读取 Redisson YAML 配置内容，
     * 并通过 {@link Config#fromYAML(String)} 转换为 Redisson 原生配置对象。
     * </p>
     *
     * @return RedissonClient
     * @throws IOException Redisson YAML 配置解析异常
     */
    @Bean
    public RedissonClient redissonClient() throws IOException {
        // 解析 application.yml 中配置的 Redisson YAML 内容
        Config config = Config.fromYAML(redissonProperties.getConfig());

        // 如果需要统一使用自定义 Jackson 序列化配置，可以启用下面这一行
        // config.setCodec(new CustomJacksonCodec());

        // 根据配置创建 Redisson 客户端实例
        return Redisson.create(config);
    }

    /**
     * 创建 Redisson JSON 编解码器。
     * <p>
     * 该 Bean 主要用于 RJsonBucket / RedisJSON 相关能力。
     * 注意：这里需要使用实现 JsonCodec 的 JacksonCodec，
     * 不能使用普通对象序列化用的 JsonJacksonCodec。
     * </p>
     *
     * @return JsonCodec
     */
    @Bean
    public JsonCodec jsonCodec() {
        return new JacksonCodec(Object.class);
    }

}