package local.ateng.java.serialize.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import local.ateng.java.serialize.common.jackson.JacksonObjectMapperFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * RedisTemplate 序列化配置。
 * <p>
 * 定义 Redis 数据访问组件的序列化策略，统一约束 Key、Value、HashKey、HashValue
 * 在 Redis 协议边界上的编码与解码行为。
 * </p>
 * <p>
 * Key 与 HashKey 采用字符串序列化策略，保证键空间具备可读性与命令行可操作性；
 * Value 与 HashValue 采用 Jackson JSON 序列化策略，保证对象结构以 JSON 形式
 * 持久化，并与 JDK 原生序列化机制解耦。
 * </p>
 *
 * @author Ateng
 * @since 2026-04-14
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
public class RedisTemplateConfig {

    /**
     * 声明 RedisTemplate 实例。
     * <p>
     * 该模板面向 Redis 对象化访问场景，使用字符串序列化器处理 Key 与 HashKey，
     * 使用存储场景专用 Jackson 序列化器处理 Value 与 HashValue。
     * </p>
     *
     * @param redisConnectionFactory Redis 连接工厂
     * @return RedisTemplate 实例
     */
    @Bean(name = "redisTemplate")
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        log.info("初始化 RedisTemplate 序列化配置");

        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);

        RedisSerializer<String> keySerializer = new StringRedisSerializer();
        RedisSerializer<Object> valueSerializer = redisValueSerializer();

        redisTemplate.setKeySerializer(keySerializer);
        redisTemplate.setHashKeySerializer(keySerializer);
        redisTemplate.setStringSerializer(keySerializer);

        redisTemplate.setValueSerializer(valueSerializer);
        redisTemplate.setHashValueSerializer(valueSerializer);
        redisTemplate.setDefaultSerializer(valueSerializer);

        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }

    /**
     * 构建 Redis Value 序列化器。
     * <p>
     * 使用存储场景专用 ObjectMapper 构建 JSON 序列化器，使 Redis 数据结构
     * 与 HTTP API 响应模型保持序列化策略隔离。
     * </p>
     *
     * @return Redis Value 序列化器
     */
    private RedisSerializer<Object> redisValueSerializer() {
        ObjectMapper objectMapper = JacksonObjectMapperFactory.buildStorageObjectMapper();
        return new GenericJackson2JsonRedisSerializer(objectMapper);
    }

}