package local.ateng.java.redisjdk8.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import com.alibaba.fastjson2.support.spring.data.redis.GenericFastJsonRedisSerializer;

/**
 * RedisTemplate 配置类
 * <p>
 * 该类负责配置 RedisTemplate，允许对象进行序列化和反序列化。
 * 在这里，我们使用了 StringRedisSerializer 来序列化和反序列化 Redis 键，
 * 使用 Jackson2JsonRedisSerializer 来序列化和反序列化 Redis 值，确保 Redis 能够存储 Java 对象。
 * 另外，ObjectMapper 的配置确保 JSON 的格式和解析行为符合预期。
 * </p>
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-03-06
 */
@Configuration
public class RedisTemplateConfig {

    @Bean(name = "redisTemplate")
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // 设置 Key 序列化器
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // 设置 Value 序列化器（可根据你项目需求使用其他序列化器）
        template.setValueSerializer(new GenericFastJsonRedisSerializer());
        template.setHashValueSerializer(new GenericFastJsonRedisSerializer());

        template.afterPropertiesSet();
        return template;
    }


}
