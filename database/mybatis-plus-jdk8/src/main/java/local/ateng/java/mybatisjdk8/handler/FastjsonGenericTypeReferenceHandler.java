package local.ateng.java.mybatisjdk8.handler;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.alibaba.fastjson.parser.Feature;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.baomidou.mybatisplus.extension.handlers.AbstractJsonTypeHandler;

import java.lang.reflect.Type;

/**
 * 通用的 Fastjson 泛型类型处理器，用于 MyBatis Plus 将复杂泛型结构与 JSON 之间进行序列化/反序列化。
 * <p>
 * 相比普通的类型处理器，此类支持 {@link java.util.List}、{@link java.util.Map}、嵌套泛型等复杂类型。
 * 通过 {@link TypeReference} 保留泛型类型信息。
 *
 * @param <T> 要处理的 Java 泛型类型
 * @author 孔余
 * @since 2025-07-28
 */
public class FastjsonGenericTypeReferenceHandler<T> extends AbstractJsonTypeHandler<T> {

    /**
     * 目标泛型类型，使用 Type 而不是 Class 以支持嵌套泛型结构。
     */
    private final Type type;

    /**
     * 构造函数，接收带泛型的类型引用用于保留完整类型信息。
     *
     * @param typeReference TypeReference<T> 用于描述泛型类型
     */
    public FastjsonGenericTypeReferenceHandler(TypeReference<T> typeReference) {
        this.type = typeReference.getType();
    }

    /**
     * 解析 JSON 字符串为 Java 泛型对象
     *
     * @param json 数据库中的 JSON 字符串
     * @return 反序列化后的 Java 对象，失败或为空时返回 null
     */
    @Override
    protected T parse(String json) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            return JSON.parseObject(
                    json,
                    this.type,
                    // 当 JSON 中存在 Java 类中没有的字段时忽略，不抛出异常
                    Feature.IgnoreNotMatch
            );
        } catch (Exception e) {
            // 解析异常时返回 null（可视情况添加日志）
            return null;
        }
    }

    /**
     * 将 Java 泛型对象序列化为 JSON 字符串，用于写入数据库字段
     *
     * @param obj Java 对象
     * @return JSON 字符串，失败或对象为 null 时返回 null
     */
    @Override
    protected String toJson(T obj) {
        try {
            if (obj == null) {
                return null;
            }
            return JSON.toJSONString(obj,
                    // Map 类型字段即使为 null 也输出
                    SerializerFeature.WriteMapNullValue,
                    // 将 null 的 List 类型字段序列化为空数组 []
                    SerializerFeature.WriteNullListAsEmpty,
                    // 将 null 的字符串字段序列化为空字符串 ""
                    SerializerFeature.WriteNullStringAsEmpty,
                    // 将 null 的数字字段序列化为 0
                    SerializerFeature.WriteNullNumberAsZero,
                    // 将 null 的布尔字段序列化为 false
                    SerializerFeature.WriteNullBooleanAsFalse,
                    // 禁用循环引用检测，提高性能（如果存在对象引用自身需谨慎）
                    SerializerFeature.DisableCircularReferenceDetect
            );
        } catch (Exception e) {
            // 序列化失败时返回 null（可添加日志）
            return null;
        }
    }
}
