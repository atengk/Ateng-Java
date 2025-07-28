package local.ateng.java.mybatisjdk8.handler;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;
import com.baomidou.mybatisplus.extension.handlers.AbstractJsonTypeHandler;

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
public class Fastjson2TypeHandler<T> extends AbstractJsonTypeHandler<T> {

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

        return JSON.parseObject(
                json,
                type,
                // 开启自动类型识别，仅允许指定包名
                JSONReader.autoTypeFilter("local.kongyu.java.", "local.ateng.java"),
                // 开启智能字段匹配（允许字段名不完全匹配）
                JSONReader.Feature.SupportSmartMatch
        );
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
                    // 序列化时输出类型信息（用于反序列化）
                    JSONWriter.Feature.WriteClassName,
                    // 不输出数字类型的类名（如 Integer、Long 等）
                    JSONWriter.Feature.NotWriteNumberClassName,
                    // 不输出 Set 类型的类名（如 HashSet）
                    JSONWriter.Feature.NotWriteSetClassName,
                    // 序列化时包含值为 null 的字段
                    JSONWriter.Feature.WriteNulls,
                    // 为兼容 JavaScript，大整数转为字符串输出
                    JSONWriter.Feature.BrowserCompatible,
                    // 序列化 BigDecimal 时使用非科学计数法（toPlainString）
                    JSONWriter.Feature.WriteBigDecimalAsPlain
            );
        } catch (Exception e) {
            // 序列化失败返回 null（可视情况记录日志）
            return null;
        }
    }
}
