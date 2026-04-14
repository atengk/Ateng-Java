package local.ateng.java.mybatisjdk8.handler;


import com.baomidou.mybatisplus.extension.handlers.AbstractJsonTypeHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import local.ateng.java.mybatisjdk8.config.JacksonObjectMapperFactory;
import local.ateng.java.mybatisjdk8.entity.MyData;
import local.ateng.java.mybatisjdk8.entity.MyDataList;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * 基于 Jackson 的通用 JSON TypeHandler 实现
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
@MappedTypes({MyData.class, MyDataList.class, List.class, Map.class})
// 指定当前 TypeHandler 绑定的 Java 类型（用于全局匹配，泛型类型在此无法生效）
public class JacksonTypeHandler<T> extends AbstractJsonTypeHandler<T> {

    private static final Logger log = LoggerFactory.getLogger(JacksonTypeHandler.class);

    /**
     * Jackson 的全局 ObjectMapper 实例
     */
    private static volatile ObjectMapper OBJECT_MAPPER = JacksonObjectMapperFactory.buildStorageObjectMapper();

    /**
     * 目标类型的 Class 对象，用于反序列化
     */
    private final Class<T> type;

    /**
     * 构造函数，指定当前处理的对象类型
     *
     * @param type 要处理的 Java 类型
     */
    public JacksonTypeHandler(Class<T> type) {
        this.type = type;
    }

    /**
     * 反序列化 JSON 字符串为 Java 对象
     *
     * @param json JSON 字符串
     * @return Java 对象，失败或为空时返回 null
     */
    @Override
    protected T parse(String json) {
        try {
            return OBJECT_MAPPER.readValue(json, this.type);
        } catch (Exception e) {
            log.error("Jackson 反序列化失败，json={}", json, e);
            return null;
        }
    }

    /**
     * 将 Java 对象序列化为 JSON 字符串
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
            return OBJECT_MAPPER.writeValueAsString(obj);
        } catch (Exception e) {
            log.error("Jackson 序列化失败，obj={}", obj, e);
            return null;
        }
    }

}