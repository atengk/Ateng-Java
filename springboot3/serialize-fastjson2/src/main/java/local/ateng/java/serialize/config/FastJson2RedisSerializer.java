package local.ateng.java.serialize.config;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONB;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.filter.Filter;
import com.alibaba.fastjson2.support.config.FastJsonConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * FastJson2 Redis序列化器
 * <p>
 * 功能：
 * 1. 支持JSON与JSONB序列化
 * 2. 支持自动类型（白名单控制）
 * 3. 提供日志输出便于问题排查
 *
 * @param <T> 序列化类型
 * @author Ateng
 * @since 2026-04-11
 */
public class FastJson2RedisSerializer<T> implements RedisSerializer<T> {

    private static final Logger log = LoggerFactory.getLogger(FastJson2RedisSerializer.class);

    /**
     * 空字节数组常量
     */
    private static final byte[] EMPTY_BYTES = new byte[0];

    /**
     * 目标类型
     */
    private final Class<T> type;

    /**
     * FastJson配置
     */
    private final FastJsonConfig config;

    /**
     * 是否使用JSONB
     */
    private final boolean jsonb;

    /**
     * 构造方法（默认JSON模式）
     */
    public FastJson2RedisSerializer(Class<T> type) {
        this(type, buildDefaultConfig(), false);
    }

    /**
     * 构造方法（自定义配置）
     */
    public FastJson2RedisSerializer(Class<T> type, FastJsonConfig config, boolean jsonb) {
        this.type = Objects.requireNonNull(type, "type不能为空");
        this.config = Objects.requireNonNull(config, "config不能为空");
        this.jsonb = jsonb;
    }

    /**
     * 构建默认配置
     */
    private static FastJsonConfig buildDefaultConfig() {
        FastJsonConfig config = new FastJsonConfig();

        // 基础配置
        config.setCharset(StandardCharsets.UTF_8);

        // 序列化特性
        config.setWriterFeatures(
                // 序列化时输出类型信息
                JSONWriter.Feature.WriteClassName,
                // 不输出数字类型的类名
                JSONWriter.Feature.NotWriteNumberClassName,
                // 不输出 Set 类型的类名
                JSONWriter.Feature.NotWriteSetClassName,
                // 不输出 Map 类型的类名
                JSONWriter.Feature.NotWriteHashMapArrayListClassName,
                // 序列化输出空值字段
                JSONWriter.Feature.WriteNulls,
                // 基于字段反序列化
                JSONWriter.Feature.FieldBased
        );

        // 反序列化特性
        config.setReaderFeatures(
                // 默认下是camel case精确匹配，打开这个后，能够智能识别camel/upper/pascal/snake/Kebab五中case
                JSONReader.Feature.SupportSmartMatch,
                // 允许字段名不带引号
                JSONReader.Feature.AllowUnQuotedFieldNames,
                // 忽略无法序列化的字段
                JSONReader.Feature.IgnoreNoneSerializable
        );

        // 自动类型白名单（建议尽量收敛）
        config.setReaderFilters(
                JSONReader.autoTypeFilter(
                        "local.ateng.",
                        "io.github.ateng."
                )
        );

        return config;
    }

    /**
     * 序列化
     */
    @Override
    public byte[] serialize(T value) throws SerializationException {
        if (value == null) {
            return EMPTY_BYTES;
        }

        try {
            byte[] result = jsonb
                    ? JSONB.toBytes(value, config.getSymbolTable(), getWriterFilters(), config.getWriterFeatures())
                    : JSON.toJSONBytes(value, config.getDateFormat(), getWriterFilters(), config.getWriterFeatures());

            if (log.isDebugEnabled()) {
                log.debug("Redis序列化成功，类型：{}，字节大小：{}", type.getName(), result.length);
            }

            return result;
        } catch (Exception e) {
            log.error("Redis序列化失败，类型：{}，对象：{}", type.getName(), value, e);
            throw new SerializationException(buildSerializeError(value), e);
        }
    }

    /**
     * 反序列化
     */
    @Override
    public T deserialize(byte[] bytes) throws SerializationException {
        if (bytes == null || bytes.length == 0) {
            return null;
        }

        try {
            T result = jsonb
                    ? JSONB.parseObject(bytes, type, config.getSymbolTable(), config.getReaderFilters(), config.getReaderFeatures())
                    : JSON.parseObject(bytes, type, config.getDateFormat(), config.getReaderFilters(), config.getReaderFeatures());

            if (log.isDebugEnabled()) {
                log.debug("Redis反序列化成功，类型：{}，字节大小：{}", type.getName(), bytes.length);
            }

            return result;
        } catch (Exception e) {
            log.error("Redis反序列化失败，类型：{}，字节长度：{}", type.getName(), bytes.length, e);
            throw new SerializationException(buildDeserializeError(bytes), e);
        }
    }

    /**
     * 获取WriterFilters，避免空指针
     */
    private Filter[] getWriterFilters() {
        Filter[] filters = config.getWriterFilters();
        return filters == null ? new Filter[0] : filters;
    }

    /**
     * 构建序列化异常信息
     */
    private String buildSerializeError(T value) {
        return "FastJson2序列化失败，type=" + type.getName()
                + ", valueClass=" + (value == null ? "null" : value.getClass().getName());
    }

    /**
     * 构建反序列化异常信息
     */
    private String buildDeserializeError(byte[] bytes) {
        return "FastJson2反序列化失败，type=" + type.getName()
                + ", bytesLength=" + (bytes == null ? 0 : bytes.length);
    }
}