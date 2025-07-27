package local.ateng.java.mybatisjdk8.handler;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.parser.Feature;
import com.alibaba.fastjson.parser.ParserConfig;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.baomidou.mybatisplus.extension.handlers.AbstractJsonTypeHandler;

/**
 * MyBatis Plus 使用 Fastjson 实现的自定义 TypeHandler
 * <p>
 * 该类用于 JSON 字符串与 Java 对象之间的转换，推荐用于字段存储为 JSON 的场景。
 * 在处理过程中，对 JSON 的解析、序列化做了一些容错处理，比如：
 * 1. JSON 字符串字段有多余属性时不抛出异常。
 * 2. 对 JSON 解析和序列化时的配置进行自定义，以确保兼容性和性能。
 *
 * @param <T> JSON 对应的 Java 类型
 * @since 2025-07-25
 */
public class FastjsonTypeHandler<T> extends AbstractJsonTypeHandler<T> {

    // 存储 Java 对象的目标类型
    private final Class<T> type;

    /**
     * 构造方法，初始化目标类型
     *
     * @param type Java 对象的类型
     */
    public FastjsonTypeHandler(Class<T> type) {
        this.type = type;
    }

    /**
     * 解析 JSON 字符串为 Java 对象
     * <p>
     * 通过 Fastjson 库的 JSON.parseObject 方法，将 JSON 字符串转换为指定的 Java 对象。
     * 同时，配置了自动类型支持、忽略不匹配字段以及支持数组映射为对象等特性。
     *
     * @param json JSON 字符串
     * @return 转换后的 Java 对象，如果解析失败返回 null
     */
    @Override
    protected T parse(String json) {
        try {
            // 配置 ParserConfig，设置自动类型支持，并允许包名前缀
            ParserConfig config = new ParserConfig();
            config.addAccept("local.ateng.java.");

            return JSON.parseObject(
                    json, this.type,
                    config,
                    // 支持 @type 字段进行反序列化（用于多态、自动识别类型）
                    Feature.SupportAutoType,
                    // JSON 中有多余字段时忽略，不抛异常
                    Feature.IgnoreNotMatch,
                    // 支持将 JSON 数组映射为 Java Bean（按顺序赋值）
                    Feature.SupportArrayToBean
            );
        } catch (Exception e) {
            // 解析失败时，不抛出异常，直接返回 null
            return null;
        }
    }

    /**
     * 将 Java 对象转换为 JSON 字符串
     * <p>
     * 使用 Fastjson 序列化对象时，启用了多个序列化特性，包括：
     * 1. 输出类名（支持多态反序列化）。
     * 2. 包含 null 值字段。
     * 3. 关闭循环引用检测以提高性能。
     *
     * @param obj Java 对象
     * @return 对象的 JSON 字符串表示
     */
    @Override
    protected String toJson(T obj) {
        try {
            if (obj == null) {
                return null;
            }

            // 序列化对象为 JSON 字符串，并启用相关特性
            return JSON.toJSONString(obj,
                    // 添加 @type 字段（全类名），支持反序列化为原类型
                    SerializerFeature.WriteClassName,
                    // Map 中字段即使为 null 也输出
                    SerializerFeature.WriteMapNullValue,
                    // 将 null 的 List 序列化为 []
                    SerializerFeature.WriteNullListAsEmpty,
                    // 将 null 的字符串序列化为 ""
                    SerializerFeature.WriteNullStringAsEmpty,
                    // null 的数字字段序列化为 0
                    SerializerFeature.WriteNullNumberAsZero,
                    // null 的布尔字段序列化为 false
                    SerializerFeature.WriteNullBooleanAsFalse,
                    // 关闭循环引用检测（性能更高）
                    SerializerFeature.DisableCircularReferenceDetect
            );
        } catch (Exception e) {
            return null;
        }
    }

}
