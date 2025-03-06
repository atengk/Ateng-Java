package local.ateng.java.serialize.config;


import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * RedisTemplate 配置类
 * <p>
 * 该类负责配置 RedisTemplate，允许对象进行序列化和反序列化。
 * 在这里，我们使用了 StringRedisSerializer 来序列化和反序列化 Redis 键，
 * 使用 Jackson2JsonRedisSerializer 来序列化和反序列化 Redis 值，确保 Redis 能够存储 Java 对象。
 * 另外，ObjectMapper 的配置确保 JSON 的格式和解析行为符合预期。
 * </p>
 */
@Configuration
public class RedisTemplateConfig {
    // 日期与时间格式化
    public static String DEFAULT_TIME_ZONE = "Asia/Shanghai";
    public static String DEFAULT_DATE_FORMAT = "yyyy-MM-dd";
    public static String DEFAULT_DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss.SSSSSS";

    /**
     * 自定义 ObjectMapper 配置以启用默认类型标记。
     * 该方法的作用是在 JSON 序列化和反序列化时包含类类型信息，
     * 以便在反序列化时能够正确地识别对象的具体类型。
     *
     * @param objectMapper 要配置的 ObjectMapper 实例
     */
    public static void customizeJsonClassType(ObjectMapper objectMapper) {
        // 启用默认类型标记，使 JSON 中包含对象的类信息
        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance, // 允许所有子类型的验证器（最宽松）
                ObjectMapper.DefaultTyping.NON_FINAL,  // 仅对非 final 类启用类型信息
                JsonTypeInfo.As.PROPERTY                // 以 JSON 属性的形式存储类型信息
        );
    }

    /**
     * 配置 RedisTemplate
     * <p>
     * 创建 RedisTemplate，并指定如何序列化和反序列化 Redis 中的键值。
     * 该配置支持使用 Jackson2JsonRedisSerializer 序列化值，并使用 StringRedisSerializer 序列化键。
     * </p>
     *
     * @param redisConnectionFactory Redis 连接工厂
     * @return 配置好的 RedisTemplate
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        // 创建 RedisTemplate 实例
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);  // 设置连接工厂

        // 使用 StringRedisSerializer 来序列化和反序列化 Redis 键
        // Redis 键将被序列化为字符串类型
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringRedisSerializer);  // 设置键的序列化器
        template.setHashKeySerializer(stringRedisSerializer);  // 设置哈希键的序列化器

        // 创建 ObjectMapper 实例，用于配置 Jackson 的序列化和反序列化行为
        ObjectMapper objectMapper = new ObjectMapper();
        JacksonConfig.customizeJsonDateTime(objectMapper, DEFAULT_TIME_ZONE, DEFAULT_DATE_FORMAT, DEFAULT_DATE_TIME_FORMAT);
        JacksonConfig.customizeJsonSerialization(objectMapper);
        JacksonConfig.customizeJsonDeserialization(objectMapper);
        JacksonConfig.customizeJsonParsing(objectMapper);
        customizeJsonClassType(objectMapper);

        // 创建 Jackson2JsonRedisSerializer，用于序列化和反序列化值
        // 该序列化器使用配置好的 ObjectMapper
        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer =
                new Jackson2JsonRedisSerializer<>(objectMapper, Object.class);

        // 设置 RedisTemplate 的值的序列化器
        template.setValueSerializer(jackson2JsonRedisSerializer);
        template.setHashValueSerializer(jackson2JsonRedisSerializer);  // 设置哈希值的序列化器

        // 返回配置好的 RedisTemplate
        template.afterPropertiesSet();
        return template;
    }

}

