package local.ateng.java.serialize.serializer;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.parser.Feature;
import com.alibaba.fastjson.parser.ParserConfig;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alibaba.fastjson.util.IOUtils;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

/**
 * 基于 Fastjson 1.x 的 Redis 序列化器
 * 适用于 Spring Data Redis 中的 value 序列化方案。
 * <p>
 * 说明：
 * - 支持类型信息输出（WriteClassName）
 * - 支持空值字段输出（WriteMapNullValue）
 * - 支持安全反序列化（白名单机制）
 * - 通用 UTF-8 编码
 *
 * @param <T> 序列化对象类型
 * @author 孔余
 * @since 2025-11-05
 */
public class FastJsonRedisSerializer<T> implements RedisSerializer<T> {

    private static final ParserConfig GLOBAL_PARSER_CONFIG = new ParserConfig();

    static {
        // 启用 AutoType 支持（反序列化时保留类型信息）
        GLOBAL_PARSER_CONFIG.setAutoTypeSupport(true);
        // 禁止 com.sun.、java.、org.apache. 等类被加载（防止安全漏洞）
        GLOBAL_PARSER_CONFIG.addDeny("java.");
        GLOBAL_PARSER_CONFIG.addDeny("javax.");
        GLOBAL_PARSER_CONFIG.addDeny("com.sun.");
        GLOBAL_PARSER_CONFIG.addDeny("sun.");
        GLOBAL_PARSER_CONFIG.addDeny("org.apache.");
        GLOBAL_PARSER_CONFIG.addDeny("org.springframework.");
        GLOBAL_PARSER_CONFIG.addDeny("com.alibaba.");
        GLOBAL_PARSER_CONFIG.addDeny("ognl.");
        GLOBAL_PARSER_CONFIG.addDeny("bsh.");
        GLOBAL_PARSER_CONFIG.addDeny("c3p0.");
        GLOBAL_PARSER_CONFIG.addDeny("net.sf.ehcache.");
        GLOBAL_PARSER_CONFIG.addDeny("org.yaml.");
        GLOBAL_PARSER_CONFIG.addDeny("org.hibernate.");
        GLOBAL_PARSER_CONFIG.addDeny("org.jboss.");
    }

    private final Class<T> clazz;

    public FastJsonRedisSerializer(Class<T> clazz) {
        this.clazz = clazz;
    }

    /**
     * 序列化：将对象转为 JSON 字节数组
     */
    @Override
    public byte[] serialize(T object) throws SerializationException {
        if (object == null) {
            return new byte[0];
        }
        try {
            return JSON.toJSONBytes(
                    object,
                    // 输出类型信息（反序列化时才能还原具体类）
                    SerializerFeature.WriteClassName,
                    // 输出为 null 的字段，否则默认会被忽略
                    SerializerFeature.WriteMapNullValue,
                    // 禁用循环引用检测，避免 $ref 结构
                    SerializerFeature.DisableCircularReferenceDetect,
                    // BigDecimal 输出为纯字符串，避免科学计数法
                    SerializerFeature.WriteBigDecimalAsPlain
            );
        } catch (Exception ex) {
            throw new SerializationException("Redis 序列化失败: " + ex.getMessage(), ex);
        }
    }

    /**
     * 反序列化：将 JSON 字节数组转回对象
     */
    @Override
    public T deserialize(byte[] bytes) throws SerializationException {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        try {
            return JSON.parseObject(
                    new String(bytes, IOUtils.UTF8),
                    clazz,
                    GLOBAL_PARSER_CONFIG,
                    // 忽略 JSON 中不存在的字段
                    Feature.IgnoreNotMatch,
                    // 支持 ISO8601 日期格式
                    Feature.AllowISO8601DateFormat
            );
        } catch (Exception ex) {
            throw new SerializationException("Redis 反序列化失败: " + ex.getMessage(), ex);
        }
    }
}
