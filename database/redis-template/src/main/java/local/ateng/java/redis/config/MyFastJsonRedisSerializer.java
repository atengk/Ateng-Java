package local.ateng.java.redis.config;


import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONB;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.support.config.FastJsonConfig;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

import java.nio.charset.Charset;

/**
 * RedisTemplate Fastjson2 Serializer
 * 自定义Redis序列化
 *
 * @author 孔余
 * @since 2024-01-30 17:29
 */
public class MyFastJsonRedisSerializer<T> implements RedisSerializer<T> {
    private final Class<T> type;
    private FastJsonConfig config = new FastJsonConfig();

    public MyFastJsonRedisSerializer(Class<T> type) {
        config.setCharset(Charset.forName("UTF-8"));
        config.setDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        // 配置 JSONWriter 的特性
        config.setWriterFeatures(
                JSONWriter.Feature.WriteClassName,             // 在 JSON 输出中写入类名
                JSONWriter.Feature.NotWriteNumberClassName,   // 不输出数字类型的类名
                JSONWriter.Feature.NotWriteSetClassName,      // 不输出 Set 类型的类名
                JSONWriter.Feature.WriteNulls,                // 输出 null 值的字段
                JSONWriter.Feature.FieldBased,                // 基于字段访问数据，而不是使用 getter/setter
                JSONWriter.Feature.BrowserCompatible,         // 生成与浏览器兼容的 JSON
                JSONWriter.Feature.WriteMapNullValue         // 输出 Map 中 null 值的键
        );

        // 配置 JSONReader 的特性
        config.setReaderFeatures(
                JSONReader.Feature.FieldBased,                // 基于字段访问数据，而不是使用 getter/setter
                JSONReader.Feature.SupportArrayToBean        // 支持将 JSON 数组解析为 Java Bean
        );


        // 支持自动类型，要读取带"@type"类型信息的JSON数据，需要显式打开SupportAutoType
        config.setReaderFilters(
                JSONReader.autoTypeFilter(
                        // 按需加上需要支持自动类型的类名前缀，范围越小越安全
                        "local.ateng.java."
                )
        );
        this.type = type;
    }

    public FastJsonConfig getFastJsonConfig() {
        return config;
    }

    public void setFastJsonConfig(FastJsonConfig fastJsonConfig) {
        this.config = fastJsonConfig;
    }

    @Override
    public byte[] serialize(T t) throws SerializationException {
        if (t == null) {
            return new byte[0];
        }
        try {
            if (config.isJSONB()) {
                return JSONB.toBytes(t, config.getSymbolTable(), config.getWriterFilters(), config.getWriterFeatures());
            } else {
                return JSON.toJSONBytes(t, config.getDateFormat(), config.getWriterFilters(), config.getWriterFeatures());
            }
        } catch (Exception ex) {
            throw new SerializationException("Could not serialize: " + ex.getMessage(), ex);
        }
    }

    @Override
    public T deserialize(byte[] bytes) throws SerializationException {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        try {
            if (config.isJSONB()) {
                return JSONB.parseObject(bytes, type, config.getSymbolTable(), config.getReaderFilters(), config.getReaderFeatures());
            } else {
                return JSON.parseObject(bytes, type, config.getDateFormat(), config.getReaderFilters(), config.getReaderFeatures());
            }
        } catch (Exception ex) {
            throw new SerializationException("Could not deserialize: " + ex.getMessage(), ex);
        }
    }
}

