package local.ateng.java.mybatisjdk8.handler;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.extension.handlers.AbstractJsonTypeHandler;

/**
 * MyBatis Plus 使用 Fastjson2 自定义 TypeHandler
 * <p>
 * 该类用于 JSON 字符串与 Java 对象之间的转换，推荐用于字段存储为 JSON 的场景。
 * 在处理过程中，对 JSON 的解析、序列化做了一些容错处理，比如：
 * 1. JSON 字符串字段有多余属性时不抛出异常。
 * 2. 对 JSON 解析和序列化时的配置进行自定义，以确保兼容性和性能。
 *
 * @author 孔余
 * @since 2025-07-25
 */
public class Fastjson2TypeHandler<T> extends AbstractJsonTypeHandler<T> {

    private final Class<T> type;

    /**
     * 构造方法，初始化目标类型
     *
     * @param type Java 对象的类型
     */
    public Fastjson2TypeHandler(Class<T> type) {
        this.type = type;
    }

    /**
     * 解析 JSON 字符串为 Java 对象
     * <p>
     * 通过 Fastjson2 库的 JSON.parseObject 方法，将 JSON 字符串转换为指定的 Java 对象。
     * 同时，配置了自动类型支持、忽略不匹配字段以及支持数组映射为对象等特性。
     *
     * @param json JSON 字符串
     * @return 转换后的 Java 对象，如果解析失败返回 null
     */
    @Override
    protected T parse(String json) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            return JSON.parseObject(json, type);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 将 Java 对象转换为 JSON 字符串
     * <p>
     * 使用 Fastjson2 序列化对象时，启用了多个序列化特性，包括：
     * 1. 输出类名（支持多态反序列化）。
     * 2. 包含 null 值字段。
     * 3. 关闭循环引用检测以提高性能。
     *
     * @param obj Java 对象
     * @return 对象的 JSON 字符串表示，序列化失败时返回空 JSON 对象
     */
    @Override
    protected String toJson(T obj) {
        try {
            if (obj == null) {
                return null;
            }
            // 序列化对象为 JSON 字符串，并启用相关特性
            return JSON.toJSONString(obj);
        } catch (Exception e) {
            return null;
        }
    }
}
