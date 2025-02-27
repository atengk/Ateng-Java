package local.ateng.java.auth.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.text.SimpleDateFormat;
import java.util.TimeZone;

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
    public RedisTemplate redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        // 创建 RedisTemplate 实例
        RedisTemplate template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);  // 设置连接工厂

        // 使用 StringRedisSerializer 来序列化和反序列化 Redis 键
        // Redis 键将被序列化为字符串类型
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringRedisSerializer);  // 设置键的序列化器
        template.setHashKeySerializer(stringRedisSerializer);  // 设置哈希键的序列化器

        // 创建 ObjectMapper 实例，用于配置 Jackson 的序列化和反序列化行为
        ObjectMapper objectMapper = new ObjectMapper();

        // 设置对象的可见性，指定所有属性都可以被序列化（包括 private 字段）
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);

        // 启用默认类型标记，即在 JSON 中添加类信息
        // 通过这种方式，Jackson 会在序列化时将对象的类名作为字段 @class 存储在 JSON 中
        // As.PROPERTY 表示类型信息将作为对象的一个属性存储
        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,  // 使用最宽松的子类验证
                ObjectMapper.DefaultTyping.NON_FINAL,  // 对所有非 final 类启用默认类型标记
                JsonTypeInfo.As.PROPERTY);  // 类型信息作为属性存储

        // 启用自定义的默认类型标识，指定在序列化时在对象中添加 @class 属性
        objectMapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);

        // 设置日期格式为 yyyy-MM-dd HH:mm:ss.SSS，确保在序列化和反序列化过程中使用统一的格式
        objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS"));

        // 设置时区为上海时间
        objectMapper.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));

        // 配置反序列化行为：
        // 如果是原始类型的字段，反序列化时遇到 null 值会转换为字段的默认值
        objectMapper.configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false);

        // 不序列化值为 null 的字段
        objectMapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false);

        // 反序列化时遇到未知属性时不会报错
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // 空字符串反序列化为 null
        objectMapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);

        // 遇到空的 Java Bean 不抛出异常
        objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

        // 反序列化时不抛出遇到未知属性的异常
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        // 创建 Jackson2JsonRedisSerializer，用于序列化和反序列化值
        // 该序列化器使用配置好的 ObjectMapper
        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer =
                new Jackson2JsonRedisSerializer<>(objectMapper, Object.class);

        // 设置 RedisTemplate 的值的序列化器
        template.setValueSerializer(jackson2JsonRedisSerializer);
        template.setHashValueSerializer(jackson2JsonRedisSerializer);  // 设置哈希值的序列化器

        // 返回配置好的 RedisTemplate
        return template;
    }
}

