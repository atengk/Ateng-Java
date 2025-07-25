package local.ateng.java.mybatisjdk8.handler;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;
import com.baomidou.mybatisplus.core.toolkit.Assert;
import com.baomidou.mybatisplus.extension.handlers.AbstractJsonTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mybatis Plus 使用 Fastjson2 自定义 TypeHandler
 * <p>
 * 用于 JSON 字符串与对象之间的转换（推荐用在对象字段为 JSON 时）
 *
 * @author 孔余
 * @since 2025-07-25
 */
@MappedTypes({Object.class})
@MappedJdbcTypes({JdbcType.VARCHAR})
public class Fastjson2TypeHandler extends AbstractJsonTypeHandler<Object> {

    private static final Logger log = LoggerFactory.getLogger(Fastjson2TypeHandler.class);

    private final Class<?> type;

    // 自动类型识别支持的包名前缀（白名单）
    private static final JSONReader.AutoTypeBeforeHandler AUTO_TYPE_HANDLER =
            JSONReader.autoTypeFilter("local.kongyu.java.", "local.ateng.java");

    private static final JSONReader.Feature[] READER_FEATURES = {
            // 默认下是camel case精确匹配，打开这个后，能够智能识别camel/upper/pascal/snake/Kebab五中case
            JSONReader.Feature.SupportSmartMatch
    };

    private static final JSONWriter.Feature[] WRITER_FEATURES = {
            // 序列化时输出类型信息
            JSONWriter.Feature.WriteClassName,
            // 不输出数字类型的类名
            JSONWriter.Feature.NotWriteNumberClassName,
            // 不输出 Set 类型的类名
            JSONWriter.Feature.NotWriteSetClassName,
            // 序列化输出空值字段
            JSONWriter.Feature.WriteNulls,
            // 在大范围超过JavaScript支持的整数，输出为字符串格式
            JSONWriter.Feature.BrowserCompatible,
            // 序列化BigDecimal使用toPlainString，避免科学计数法
            JSONWriter.Feature.WriteBigDecimalAsPlain
    };

    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";

    private static final JSONWriter.Context WRITER_CONTEXT = new JSONWriter.Context(WRITER_FEATURES);

    static {
        WRITER_CONTEXT.setDateFormat(DATE_FORMAT);
    }

    public Fastjson2TypeHandler(Class<?> type) {
        Assert.notNull(type, "Type argument cannot be null");
        this.type = type;
    }

    @Override
    protected Object parse(String json) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        return JSON.parseObject(json, type, AUTO_TYPE_HANDLER, READER_FEATURES);
    }

    @Override
    protected String toJson(Object obj) {
        return JSON.toJSONString(obj, WRITER_FEATURES);
    }
}
