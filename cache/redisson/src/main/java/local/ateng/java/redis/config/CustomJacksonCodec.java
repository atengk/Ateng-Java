package local.ateng.java.redis.config;

import org.redisson.codec.JsonJacksonCodec;

/**
 * 自定义 Redisson Jackson 编解码器
 * 用于让 Redisson 普通对象序列化使用项目自定义的 ObjectMapper 配置。
 *
 * @author Ateng
 * @since 2026-04-27
 */
public class CustomJacksonCodec extends JsonJacksonCodec {

    /**
     * 创建自定义 Jackson 编解码器。
     * <p>
     * 这里使用 {@link JacksonObjectMapperFactory#buildStorageObjectMapper()}
     * 构建用于 Redis 存储的 ObjectMapper，便于统一处理时间格式、类型信息、
     * 空值策略、反序列化兼容性等配置。
     * </p>
     */
    public CustomJacksonCodec() {
        super(JacksonObjectMapperFactory.buildStorageObjectMapper());
    }

}