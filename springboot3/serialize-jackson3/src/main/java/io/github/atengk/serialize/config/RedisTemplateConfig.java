package io.github.atengk.serialize.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.json.JsonMapper;

/**
 * RedisTemplate 配置类，统一定义 Key/Value 的序列化策略。
 * Key 使用字符串序列化，Value 使用 Jackson JSON 序列化。
 *
 * @author Ateng
 * @since 2026-04-14
 */
@Configuration
public class RedisTemplateConfig {

    /**
     * 构建并初始化 RedisTemplate，配置序列化器。
     *
     * @param redisConnectionFactory Redis 连接工厂
     * @return RedisTemplate 实例
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        // 创建 RedisTemplate 实例
        RedisTemplate<String, Object> template = new RedisTemplate<>();

        // 设置 Redis 连接工厂
        template.setConnectionFactory(redisConnectionFactory);

        // 创建字符串序列化器（用于 Key）
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();

        // 设置 Key 序列化器
        template.setKeySerializer(stringRedisSerializer);

        // 设置 Hash Key 序列化器
        template.setHashKeySerializer(stringRedisSerializer);

        // 构建自定义 JsonMapper（统一 JSON 规则）
        JsonMapper jsonMapper = JacksonJsonMapperFactory.buildStorageJsonMapper();

        // 创建 Jackson 序列化器（用于 Value）
        JacksonJsonRedisSerializer<Object> jacksonJsonRedisSerializer =
                new JacksonJsonRedisSerializer<>(jsonMapper, Object.class);

        // 设置 Value 序列化器
        template.setValueSerializer(jacksonJsonRedisSerializer);

        // 设置 Hash Value 序列化器
        template.setHashValueSerializer(jacksonJsonRedisSerializer);

        // 初始化 RedisTemplate
        template.afterPropertiesSet();

        // 返回配置完成的 RedisTemplate
        return template;
    }

}
