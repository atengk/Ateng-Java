package local.ateng.java.mybatisjdk8.handler;


import com.baomidou.mybatisplus.extension.handlers.AbstractJsonTypeHandler;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

/**
 * 通用的 Jackson 类型处理器，用于将 JSON 字段与 Java 对象之间互相转换。
 * <p>
 * 该处理器基于 Jackson 实现，适用于 MyBatis Plus 的 JSON 类型字段映射。
 * <p>
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
 * @param <T> 要序列化或反序列化的目标类型
 * @author 孔余
 * @since 2025-07-28
 */
@MappedJdbcTypes({JdbcType.VARCHAR, JdbcType.LONGVARCHAR, JdbcType.OTHER}) // 数据库字段类型
@MappedTypes({Map.class, List.class, JsonNode.class, ObjectNode.class, ArrayNode.class})     // Java 类型
public class JacksonTypeHandler<T> extends AbstractJsonTypeHandler<T> {

    /**
     * Jackson 的全局 ObjectMapper 实例（懒加载、单例）
     */
    private static volatile ObjectMapper OBJECT_MAPPER;

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
            return getObjectMapper().readValue(json, this.type);
        } catch (Exception e) {
            // 可按需添加日志记录
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
            return getObjectMapper().writeValueAsString(obj);
        } catch (Exception e) {
            // 可按需添加日志记录
            return null;
        }
    }

    // 日期与时间格式化
    public static String DEFAULT_TIME_ZONE = "Asia/Shanghai";
    public static String DEFAULT_DATE_FORMAT = "yyyy-MM-dd";
    public static String DEFAULT_DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";
    public static String DEFAULT_TIME_FORMAT = "HH:mm:ss.SSS";

    public static ObjectMapper getObjectMapper() {
        if (OBJECT_MAPPER == null) {
            synchronized (JacksonTypeHandler.class) {
                if (OBJECT_MAPPER == null) {
                    OBJECT_MAPPER = new ObjectMapper();
                    // 配置日期和时间的序列化与反序列化
                    customizeJsonDateTime(OBJECT_MAPPER, DEFAULT_TIME_ZONE, DEFAULT_DATE_FORMAT, DEFAULT_TIME_FORMAT, DEFAULT_DATE_TIME_FORMAT);
                    // 配置 JSON 序列化相关设置
                    customizeJsonSerialization(OBJECT_MAPPER);
                    // 配置 JSON 反序列化相关设置
                    customizeJsonDeserialization(OBJECT_MAPPER);
                    // 配置 JSON 解析相关设置
                    customizeJsonParsing(OBJECT_MAPPER);
                    // 配置反序列化时自动转换的设置
                    customizeJsonClassType(OBJECT_MAPPER);
                }
            }
        }
        return OBJECT_MAPPER;
    }

    /**
     * 自定义 Jackson 时间日期的序列化和反序列化规则
     *
     * @param objectMapper Jackson 的 ObjectMapper 实例
     */
    public static void customizeJsonDateTime(ObjectMapper objectMapper, String timeZone, String dateFormat, String timeFormat, String dateTimeFormat) {
        // 设置全局时区，确保 Date 类型数据使用此时区
        objectMapper.setTimeZone(TimeZone.getTimeZone(timeZone));

        // 关闭默认时间戳序列化，改为标准格式
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 避免与 JavaTimeModule 冲突
        objectMapper.setDateFormat(new SimpleDateFormat(dateTimeFormat));

        // Java 8 时间模块
        JavaTimeModule javaTimeModule = new JavaTimeModule();

        // LocalDateTime
        javaTimeModule.addSerializer(LocalDateTime.class,
                new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(dateTimeFormat)));
        javaTimeModule.addDeserializer(LocalDateTime.class,
                new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern(dateTimeFormat)));

        // LocalDate
        javaTimeModule.addSerializer(LocalDate.class,
                new LocalDateSerializer(DateTimeFormatter.ofPattern(dateFormat)));
        javaTimeModule.addDeserializer(LocalDate.class,
                new LocalDateDeserializer(DateTimeFormatter.ofPattern(dateFormat)));

        // LocalTime
        javaTimeModule.addSerializer(LocalTime.class,
                new LocalTimeSerializer(DateTimeFormatter.ofPattern(timeFormat)));
        javaTimeModule.addDeserializer(LocalTime.class,
                new LocalTimeDeserializer(DateTimeFormatter.ofPattern(timeFormat)));

        // 注册模块
        objectMapper.registerModule(javaTimeModule);
    }

    /**
     * 自定义 Jackson 序列化规则
     *
     * @param objectMapper Jackson 的 ObjectMapper 实例
     */
    public static void customizeJsonSerialization(ObjectMapper objectMapper) {
        // 关闭 JSON 美化输出（生产环境建议关闭，提高性能）
        objectMapper.disable(SerializationFeature.INDENT_OUTPUT);

        // 避免 "No serializer found for class" 异常
        objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

        // 过滤 null 值，减少 JSON 体积
        //objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        // 过滤空集合、空字符串等无效数据，进一步精简 JSON
        //objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

        // 不过滤 null、空集合、空字符串等无效数据值，保持数据的原始状态
        objectMapper.setSerializationInclusion(JsonInclude.Include.ALWAYS);

        // 枚举类型：使用 `toString()` 方式序列化，而不是默认的 `name()`
        objectMapper.enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);

        // BigDecimal 序列化时不使用科学计数法，确保数据精确
        objectMapper.enable(SerializationFeature.WRITE_BIGDECIMAL_AS_PLAIN);

        // 排序字段名，保证 JSON 输出的键顺序固定（有助于缓存和数据比对）
        objectMapper.enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);

        // 将 Long 和 BigInteger 序列化为字符串，防止 JavaScript 丢失精度
        SimpleModule simpleModule = new SimpleModule();
        ToStringSerializer stringSerializer = ToStringSerializer.instance;
        simpleModule.addSerializer(BigInteger.class, stringSerializer);
        simpleModule.addSerializer(BigDecimal.class, stringSerializer);
        simpleModule.addSerializer(BigInteger.class, stringSerializer);
        simpleModule.addSerializer(Long.class, stringSerializer);
        simpleModule.addSerializer(Long.TYPE, stringSerializer);
        objectMapper.registerModule(simpleModule);
    }

    /**
     * 自定义 Jackson 反序列化规则
     *
     * @param objectMapper Jackson 的 ObjectMapper 实例
     */
    public static void customizeJsonDeserialization(ObjectMapper objectMapper) {
        // 允许单个值转数组（例如 1 -> [1]）
        objectMapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);

        // 忽略未知字段（避免因缺少字段报错，提升兼容性）
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        // 处理 原始类型（如 int, long, boolean 等）在反序列化时如果遇到 null 值将其替换为默认值，而不是抛出异常
        objectMapper.configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false);

        // 使用 BigDecimal 反序列化浮点数，避免精度丢失
        objectMapper.enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);

        // 使用枚举的 `toString()` 方法进行反序列化，而不是默认的 `name()`
        objectMapper.enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING);

        // 允许特殊字符转义
        objectMapper.enable(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS);
        objectMapper.enable(JsonParser.Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER);

        // 错误时提供类型检查，增强反序列化稳定性
        objectMapper.enable(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE);
    }

    /**
     * 自定义 Jackson JSON 解析设置
     *
     * @param objectMapper Jackson 的 ObjectMapper 实例
     */
    public static void customizeJsonParsing(ObjectMapper objectMapper) {
        // 允许 JSON 中带注释，方便开发阶段使用
        objectMapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);

        // 允许字段名不带引号（可处理某些特殊格式的 JSON）
        objectMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);

        // 允许单引号作为 JSON 字符串的定界符（适用于某些特殊格式）
        objectMapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);

        // 允许控制字符的转义（例如，`\n` 或 `\t`）
        objectMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);

        // 允许反斜杠转义任何字符（如：`\\`）
        objectMapper.configure(JsonParser.Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER, true);

        // 允许无效的 UTF-8 字符（如果 JSON 编码不完全符合标准）
        objectMapper.configure(JsonParser.Feature.IGNORE_UNDEFINED, true);

        // 允许 JSON 中无序字段（通常是为了性能优化）
        objectMapper.configure(JsonParser.Feature.ALLOW_NON_NUMERIC_NUMBERS, true);
    }

    /**
     * 自定义 ObjectMapper 配置以启用默认类型标记。
     * 该方法的作用是在 JSON 序列化和反序列化时包含类类型信息，
     * 以便在反序列化时能够正确地识别对象的具体类型。
     *
     * @param objectMapper 要配置的 ObjectMapper 实例
     */
    public static void customizeJsonClassType(ObjectMapper objectMapper) {
        // 启用默认类型标记，使 JSON 中包含对象的类信息
        objectMapper.activateDefaultTyping(
                // 允许所有子类型的验证器（最宽松）
                LaissezFaireSubTypeValidator.instance,
                // 仅对非 final 类启用类型信息
                ObjectMapper.DefaultTyping.NON_FINAL,
                // 以 JSON 属性的形式存储类型信息
                JsonTypeInfo.As.PROPERTY
        );
    }

}