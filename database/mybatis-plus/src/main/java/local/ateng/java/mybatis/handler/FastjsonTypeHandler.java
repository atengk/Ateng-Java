package local.ateng.java.mybatis.handler;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.parser.Feature;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.baomidou.mybatisplus.extension.handlers.AbstractJsonTypeHandler;
import local.ateng.java.mybatis.entity.MyData;
import local.ateng.java.mybatis.entity.MyDataList;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Type;

/**
 * 基于 Fastjson 的通用 JSON TypeHandler 实现
 * <p>
 * 用于实现数据库 JSON 字符串与 Java 对象之间的自动转换，兼容 MyBatis-Plus 的泛型处理机制
 * <p>
 * 特性说明：
 * 1. 支持普通 Java Bean 的序列化与反序列化
 * 2. 支持泛型类型（如 List、Map、自定义泛型包装类）
 * 3. 在字段级别可通过反射自动解析泛型真实类型（依赖 Field 元信息）
 * 4. 泛型推断仅在字段级 TypeHandler 生效，全局 @MappedTypes 无法感知泛型参数
 * <p>
 * 适用场景：
 * - JSON 字段（VARCHAR / TEXT / JSON）映射为复杂对象结构
 * - 需要强类型而非原始字符串处理 JSON 数据
 *
 * @author Ateng
 * @since 2026-04-13
 */
@MappedJdbcTypes({JdbcType.VARCHAR, JdbcType.LONGVARCHAR, JdbcType.OTHER})
// 指定当前 TypeHandler 适用的 JDBC 类型（对应数据库 JSON 字段常见存储类型）
@MappedTypes({MyData.class, MyDataList.class})
// 指定当前 TypeHandler 绑定的 Java 类型（用于全局匹配，泛型类型在此无法生效）
public class FastjsonTypeHandler<T> extends AbstractJsonTypeHandler<T> {

    private static final Logger log = LoggerFactory.getLogger(FastjsonTypeHandler.class);

    /**
     * 标准构造方法（MyBatis 使用）
     * <p>
     * 用于非泛型或无法获取字段上下文的场景
     *
     * @param type 目标 Java 类型
     */
    public FastjsonTypeHandler(Class<?> type) {
        super(type);
    }

    /**
     * 扩展构造方法（MyBatis-Plus 使用）
     * <p>
     * 通过 Field 获取泛型信息，用于精确反序列化（如 List<T>、Wrapper<T>）
     *
     * @param type  目标 Java 类型
     * @param field 当前字段反射对象
     */
    public FastjsonTypeHandler(Class<?> type, Field field) {
        super(type, field);
    }

    /**
     * 将 JSON 字符串解析为对象
     *
     * @param json 数据库中存储的 JSON 字符串
     * @return 解析后的 Java 对象，解析失败或为空则返回 null
     */
    @Override
    public T parse(String json) {

        if (json == null || json.trim().isEmpty()) {
            return null;
        }

        try {
            Type fieldType = getFieldType();

            return JSON.parseObject(
                    json,
                    fieldType,
                    // 允许 JSON 中包含注释（// 或 /* */）
                    Feature.AllowComment,
                    // 允许字段名不加双引号
                    Feature.AllowUnQuotedFieldNames,
                    // 允许单引号作为字符串定界符
                    Feature.AllowSingleQuotes,
                    // 字段名使用常量池优化内存
                    Feature.InternFieldNames,
                    // 允许多余的逗号
                    Feature.AllowArbitraryCommas,
                    // 忽略 JSON 中不存在的字段
                    Feature.IgnoreNotMatch,
                    // 将小数解析为 BigDecimal（而不是 Double）
                    Feature.UseBigDecimal,
                    // 允许 ISO 8601 日期格式（例如：2023-10-11T14:30:00Z）
                    Feature.AllowISO8601DateFormat
            );

        } catch (Exception e) {
            log.error("Fastjson 反序列化失败，json={}", json, e);
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
    public String toJson(T obj) {

        if (obj == null) {
            return null;
        }

        try {
            return JSON.toJSONString(
                    obj,
                    // 输出为 null 的字段，否则默认会被忽略
                    SerializerFeature.WriteMapNullValue,
                    // 禁用循环引用检测，避免出现 "$ref" 结构
                    SerializerFeature.DisableCircularReferenceDetect
            );

        } catch (Exception e) {
            log.error("Fastjson 序列化失败，obj={}", obj, e);
            return null;
        }
    }
}