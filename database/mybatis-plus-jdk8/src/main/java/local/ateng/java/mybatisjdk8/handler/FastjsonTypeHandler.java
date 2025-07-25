package local.ateng.java.mybatisjdk8.handler;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.parser.Feature;
import com.alibaba.fastjson.parser.ParserConfig;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.baomidou.mybatisplus.core.toolkit.Assert;
import com.baomidou.mybatisplus.extension.handlers.AbstractJsonTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mybatis Plus 使用 Fastjson 自定义 TypeHandler
 * <p>
 * 用于 JSON 字符串与对象之间的转换（推荐用在对象字段为 JSON 时）
 *
 * @author 孔余
 * @since 2025-07-25
 */
@MappedTypes({Object.class})
@MappedJdbcTypes({JdbcType.VARCHAR})
public class FastjsonTypeHandler extends AbstractJsonTypeHandler<Object> {
    private static final Logger log = LoggerFactory.getLogger(FastjsonTypeHandler.class);
    private final Class<?> type;

    public FastjsonTypeHandler(Class<?> type) {
        if (log.isTraceEnabled()) {
            log.trace("FastjsonTypeHandler(" + type + ")");
        }

        Assert.notNull(type, "Type argument cannot be null", new Object[0]);
        this.type = type;
    }

    @Override
    protected Object parse(String json) {
        ParserConfig config = new ParserConfig();
        // 添加包名前缀到 autoType 白名单中
        config.addAccept("com.brx.hr.tms.ca.");
        return JSON.parseObject(
                json, this.type,
                config,
                // 支持 @type 字段进行反序列化（用于多态、自动识别类型）
                Feature.SupportAutoType,
                // 	JSON 中有多余字段时忽略，不抛异常
                Feature.IgnoreNotMatch,
                // 支持将 JSON 数组映射为 Java Bean（按顺序赋值）
                Feature.SupportArrayToBean
        );
    }

    @Override
    protected String toJson(Object obj) {
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
                SerializerFeature.DisableCircularReferenceDetect);
    }
}