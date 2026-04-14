package local.ateng.java.mybatis.handler;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.extension.handlers.AbstractJsonTypeHandler;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import local.ateng.java.mybatis.config.JacksonObjectMapperFactory;
import local.ateng.java.mybatis.entity.MyData;
import local.ateng.java.mybatis.entity.MyDataList;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.List;

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
@MappedTypes({MyData.class, MyDataList.class, List.class})
// 指定当前 TypeHandler 绑定的 Java 类型（用于全局匹配，泛型类型在此无法生效）
public class JacksonTypeHandler<T> extends AbstractJsonTypeHandler<T> {

    private static final Logger log = LoggerFactory.getLogger(JacksonTypeHandler.class);

    /**
     * Jackson 的全局 ObjectMapper 实例
     */
    private static volatile ObjectMapper OBJECT_MAPPER = buildDefaultObjectMapper();

    /**
     * 标准构造方法（MyBatis 使用）
     * <p>
     * 用于非泛型或无法获取字段上下文的场景
     *
     * @param type 目标 Java 类型
     */
    public JacksonTypeHandler(Class<?> type) {
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
    public JacksonTypeHandler(Class<?> type, Field field) {
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

        if (ObjectUtil.isEmpty(json)) {
            return null;
        }

        try {
            Type fieldType = getFieldType();
            JavaType javaType = OBJECT_MAPPER.getTypeFactory().constructType(fieldType);
            return OBJECT_MAPPER.readValue(json, javaType);

        } catch (Exception e) {
            log.error("Jackson 反序列化失败，json={}", json, e);
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

        if (ObjectUtil.isEmpty(obj)) {
            return null;
        }

        try {
            return OBJECT_MAPPER.writeValueAsString(obj);

        } catch (Exception e) {
            log.error("Jackson 序列化失败，obj={}", obj, e);
            return null;
        }
    }

    /**
     * 构建默认 ObjectMapper。
     *
     * @return 默认 ObjectMapper
     */
    private static ObjectMapper buildDefaultObjectMapper() {
        return JacksonObjectMapperFactory.buildStorageObjectMapper();
    }

}