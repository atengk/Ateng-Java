package local.ateng.java.mybatisjdk8.handler;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.baomidou.mybatisplus.extension.handlers.AbstractJsonTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * 通用的 Fastjson 类型处理器，用于 MyBatis Plus 中将 Java 对象与 JSON 字段互相转换。
 * <p>
 * 本处理器使用 Fastjson 1.x 实现，支持自动类型识别、空值处理、类型信息保留等功能。
 * 通常用于如下场景：
 * <pre>{@code
 * @TableField(typeHandler = JacksonTypeHandler.class)
 * private MyEntity data;
 * }</pre>
 * <pre>{@code
 *  * @TableField(typeHandler = JacksonTypeHandler.class)
 *  * private List<MyEntity> dataList;
 *  * }</pre>
 *
 * @param <T> 要序列化或反序列化的目标 Java 类型
 * @author 孔余
 * @since 2025-07-28
 */
@MappedJdbcTypes({JdbcType.VARCHAR, JdbcType.LONGVARCHAR, JdbcType.OTHER}) // 数据库字段类型
@MappedTypes({Map.class, List.class, JSONObject.class, JSONArray.class})     // Java 类型
public class FastjsonTypeHandler<T> extends AbstractJsonTypeHandler<T> {

    private static final Logger log = LoggerFactory.getLogger(FastjsonTypeHandler.class);

    /**
     * 目标类型的 Class 对象，用于反序列化
     */
    private final Class<T> type;

    /**
     * 构造方法，指定当前处理的对象类型
     *
     * @param type 要处理的 Java 类型
     */
    public FastjsonTypeHandler(Class<T> type) {
        this.type = type;
    }

    /**
     * 将 JSON 字符串解析为 Java 对象
     *
     * @param json 数据库中的 JSON 字符串
     * @return Java 对象，解析失败或为空时返回 null
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
                    // 支持 "@type" 字段进行自动类型识别（适用于多态反序列化）
                    Feature.SupportAutoType,
                    // 当 JSON 中存在 Java 类中没有的字段时忽略，不抛出异常
                    Feature.IgnoreNotMatch
            );
        } catch (Exception e) {
            log.error("JSON 解析失败: {}", json, e);
            return null;
        }
    }

    /**
     * 将 Java 对象序列化为 JSON 字符串，用于写入数据库字段
     *
     * @param obj Java 对象
     * @return JSON 字符串，序列化失败或对象为 null 时返回 null
     */
    @Override
    protected String toJson(T obj) {
        try {
            if (obj == null) {
                return null;
            }

            return JSON.toJSONString(obj,
                    // 添加 "@type" 字段，保留类的全限定名，便于反序列化时识别原类型
                    SerializerFeature.WriteClassName,
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
            log.error("对象序列化为 JSON 失败: {}", obj, e);
            return null;
        }
    }
}
