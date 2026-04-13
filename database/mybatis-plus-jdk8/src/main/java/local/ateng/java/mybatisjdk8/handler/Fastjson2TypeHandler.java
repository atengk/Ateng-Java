package local.ateng.java.mybatisjdk8.handler;

import com.alibaba.fastjson2.*;
import com.baomidou.mybatisplus.extension.handlers.AbstractJsonTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * 通用的 Fastjson2 类型处理器，用于 MyBatis Plus 中将对象以 JSON 格式读写数据库字段。
 * <p>
 * 适用于 JSON 字段与自定义 Java 对象之间的转换，
 * 实现了序列化与反序列化的逻辑，支持自动类型识别和特定的序列化配置。
 * 通常用于如下场景：
 * <pre>{@code
 * @TableField(typeHandler = JacksonTypeHandler.class)
 * private MyEntity data;
 * }</pre>
 *
 * @param <T> 要序列化或反序列化的目标类型
 * @author 孔余
 * @since 2025-07-28
 */
@MappedJdbcTypes({JdbcType.VARCHAR, JdbcType.LONGVARCHAR, JdbcType.OTHER}) // 数据库字段类型
@MappedTypes({Map.class, List.class, JSONObject.class, JSONArray.class})     // Java 类型
public class Fastjson2TypeHandler<T> extends AbstractJsonTypeHandler<T> {

    private static final Logger log = LoggerFactory.getLogger(Fastjson2TypeHandler.class);

    /**
     * 要处理的目标类型
     */
    private final Class<T> type;

    /**
     * 构造方法，指定处理的 Java 类型
     *
     * @param type 目标类类型
     */
    public Fastjson2TypeHandler(Class<T> type) {
        this.type = type;
    }

    /**
     * 将 JSON 字符串解析为对象
     *
     * @param json 数据库中存储的 JSON 字符串
     * @return 解析后的 Java 对象，解析失败或为空则返回 null
     */
    @Override
    protected T parse(String json) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }

        try {
            return JSON.parseObject(
                    json,
                    type,
                    // 默认下是camel case精确匹配，打开这个后，能够智能识别camel/upper/pascal/snake/Kebab五中case
                    JSONReader.Feature.SupportSmartMatch,
                    // 允许字段名不带引号
                    JSONReader.Feature.AllowUnQuotedFieldNames,
                    // 忽略无法序列化的字段
                    JSONReader.Feature.IgnoreNoneSerializable
            );
        } catch (Exception e) {
            log.error("JSON 解析失败: {}", json, e);
            return null;
        }
    }

    /**
     * 将对象序列化为 JSON 字符串，用于写入数据库
     *
     * @param obj Java 对象
     * @return 序列化后的 JSON 字符串，失败或为空返回 null
     */
    @Override
    protected String toJson(T obj) {
        try {
            if (obj == null) {
                return null;
            }

            return JSON.toJSONString(
                    obj,
                    // 序列化输出空值字段
                    JSONWriter.Feature.WriteNulls,
                    // 基于字段反序列化
                    JSONWriter.Feature.FieldBased
            );
        } catch (Exception e) {
            log.error("对象序列化为 JSON 失败: {}", obj, e);
            return null;
        }
    }
}
