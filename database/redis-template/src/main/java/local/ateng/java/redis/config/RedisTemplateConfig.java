package local.ateng.java.redis.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisTemplateConfig {

    @Bean
    public RedisTemplate redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        return getRedisTemplate(redisConnectionFactory);
    }

    /**
     * 在 Spring Data Redis 中集成 Fastjson2
     * 使用 GenericFastJsonRedisSerializer 作为 RedisTemplate 的 RedisSerializer 来提升JSON序列化和反序列化速度。
     * https://github.com/alibaba/fastjson2/blob/main/docs/spring_support_cn.md#4-%E5%9C%A8-spring-data-redis-%E4%B8%AD%E9%9B%86%E6%88%90-fastjson2
     *
     * @param redisConnectionFactory
     * @return
     */
    private RedisTemplate getRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate redisTemplate = new RedisTemplate();
        redisTemplate.setConnectionFactory(redisConnectionFactory);

        /**
         * 使用GenericFastJsonRedisSerializer来序列化和反序列化redis的key和value值
         */
        //GenericFastJsonRedisSerializer genericFastJsonRedisSerializer = new GenericFastJsonRedisSerializer();
        //redisTemplate.setDefaultSerializer(fastJsonRedisSerializer);//设置默认的Serialize，包含 keySerializer & valueSerializer

        /**
         * 使用StringRedisSerializer来序列化和反序列化redis的key值
         */
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        redisTemplate.setKeySerializer(stringRedisSerializer);
        redisTemplate.setHashKeySerializer(stringRedisSerializer);
        /**
         * 自定义Serializer来序列化和反序列化redis的value值
         */
        MyFastJsonRedisSerializer myFastJsonRedisSerializer = new MyFastJsonRedisSerializer(Object.class);
        redisTemplate.setValueSerializer(myFastJsonRedisSerializer);
        redisTemplate.setHashValueSerializer(myFastJsonRedisSerializer);
        redisTemplate.setStringSerializer(myFastJsonRedisSerializer);
        // 返回redisTemplate
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }

}