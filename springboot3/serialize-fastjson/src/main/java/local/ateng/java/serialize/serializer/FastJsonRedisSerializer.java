package local.ateng.java.serialize.serializer;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.parser.Feature;
import com.alibaba.fastjson.parser.ParserConfig;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alibaba.fastjson.util.IOUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

import java.util.Objects;

/**
 * Fastjson1 Redis序列化器
 * <p>
 * 功能：
 * 1. 支持类型信息序列化
 * 2. 支持安全反序列化（白名单控制）
 * 3. 提供日志输出，便于排查问题
 *
 * @param <T> 序列化类型
 * @author Ateng
 * @since 2026-04-11
 */
@Slf4j
public class FastJsonRedisSerializer<T> implements RedisSerializer<T> {

    /**
     * 空字节数组
     */
    private static final byte[] EMPTY_BYTES = new byte[0];

    /**
     * 全局解析配置（线程安全）
     */
    private static final ParserConfig GLOBAL_CONFIG = new ParserConfig();

    static {
        // 开启AutoType（必须配合白名单使用）
        GLOBAL_CONFIG.setAutoTypeSupport(true);

        // 黑名单（高危包）
        GLOBAL_CONFIG.addDeny("java.");
        GLOBAL_CONFIG.addDeny("javax.");
        GLOBAL_CONFIG.addDeny("sun.");
        GLOBAL_CONFIG.addDeny("com.sun.");
        GLOBAL_CONFIG.addDeny("org.apache.");
        GLOBAL_CONFIG.addDeny("org.springframework.");
        GLOBAL_CONFIG.addDeny("com.alibaba.");
        GLOBAL_CONFIG.addDeny("ognl.");
        GLOBAL_CONFIG.addDeny("bsh.");
        GLOBAL_CONFIG.addDeny("c3p0.");
        GLOBAL_CONFIG.addDeny("org.yaml.");
        GLOBAL_CONFIG.addDeny("org.hibernate.");
        GLOBAL_CONFIG.addDeny("org.jboss.");

        // 建议：生产环境应增加白名单（更安全）
         GLOBAL_CONFIG.addAccept("local.ateng.");
         GLOBAL_CONFIG.addAccept("io.github.atengk.");
    }

    /**
     * 目标类型
     */
    private final Class<T> clazz;

    /**
     * 构造方法
     */
    public FastJsonRedisSerializer(Class<T> clazz) {
        this.clazz = Objects.requireNonNull(clazz, "clazz不能为空");
    }

    /**
     * 序列化：对象转字节数组
     */
    @Override
    public byte[] serialize(T object) throws SerializationException {
        if (object == null) {
            return EMPTY_BYTES;
        }

        try {
            byte[] result = JSON.toJSONBytes(
                    object,
                    // 输出类型信息（反序列化时才能还原具体类）
                    SerializerFeature.WriteClassName,
                    // 输出为 null 的字段，否则默认会被忽略
                    SerializerFeature.WriteMapNullValue,
                    // 禁用循环引用检测，避免出现 "$ref" 结构
                    SerializerFeature.DisableCircularReferenceDetect,
                    // BigDecimal 输出为纯字符串（不使用科学计数法）
                    SerializerFeature.WriteBigDecimalAsPlain
            );

            if (log.isDebugEnabled()) {
                log.debug("Redis序列化成功，类型：{}，字节大小：{}", clazz.getName(), result.length);
            }

            return result;
        } catch (Exception e) {
            log.error("Redis序列化失败，类型：{}，对象：{}", clazz.getName(), object, e);
            throw new SerializationException(buildSerializeError(object), e);
        }
    }

    /**
     * 反序列化：字节数组转对象
     */
    @Override
    public T deserialize(byte[] bytes) throws SerializationException {
        if (bytes == null || bytes.length == 0) {
            return null;
        }

        try {
            T result = JSON.parseObject(
                    new String(bytes, IOUtils.UTF8),
                    clazz,
                    GLOBAL_CONFIG,
                    // 允许 JSON 中包含注释（// 或 /* */）
                    Feature.AllowComment,
                    // 允许字段名不加双引号
                    Feature.AllowUnQuotedFieldNames,
                    // 允许单引号作为字符串定界符
                    Feature.AllowSingleQuotes,
                    // 字段名使用常量池优化内存
                    Feature.InternFieldNames,
                    // 允许多余的逗号
                    Feature.AllowArbitraryCommas,
                    // 忽略 JSON 中不存在的字段
                    Feature.IgnoreNotMatch,
                    // 将小数解析为 BigDecimal（而不是 Double）
                    Feature.UseBigDecimal,
                    // 允许 ISO 8601 日期格式（例如：2023-10-11T14:30:00Z）
                    Feature.AllowISO8601DateFormat
            );

            if (log.isDebugEnabled()) {
                log.debug("Redis反序列化成功，类型：{}，字节大小：{}", clazz.getName(), bytes.length);
            }

            return result;
        } catch (Exception e) {
            log.error("Redis反序列化失败，类型：{}，字节长度：{}", clazz.getName(), bytes.length, e);
            throw new SerializationException(buildDeserializeError(bytes), e);
        }
    }

    /**
     * 构建序列化异常信息
     */
    private String buildSerializeError(T object) {
        return "Fastjson序列化失败，type=" + clazz.getName()
                + ", valueClass=" + (object == null ? "null" : object.getClass().getName());
    }

    /**
     * 构建反序列化异常信息
     */
    private String buildDeserializeError(byte[] bytes) {
        return "Fastjson反序列化失败，type=" + clazz.getName()
                + ", bytesLength=" + (bytes == null ? 0 : bytes.length);
    }
}