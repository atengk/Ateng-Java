package local.ateng.java.serialize.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

/**
 * Jackson 使用 ObjectMapper 序列化和反序列化配置
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-03-05
 */
@Configuration
public class JacksonConfig {

    // 日期与时间格式化
    public static String DEFAULT_TIME_ZONE = "Asia/Shanghai";
    public static String DEFAULT_DATE_FORMAT = "yyyy-MM-dd";
    public static String DEFAULT_DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    /**
     * 自定义 Jackson 时间日期的序列化和反序列化规则
     *
     * @param objectMapper Jackson 的 ObjectMapper 实例
     */
    public static void customizeJsonDateTime(ObjectMapper objectMapper, String timeZone,String dateFormat, String dateTimeFormat) {
        // 设置全局时区，确保 Date 类型数据使用此时区
        objectMapper.setTimeZone(TimeZone.getTimeZone(timeZone));

        // 关闭默认时间戳序列化，改为标准格式
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 避免与 JavaTimeModule 冲突
        objectMapper.setDateFormat(new SimpleDateFormat(dateTimeFormat));

        // Java 8 时间模块
        JavaTimeModule javaTimeModule = new JavaTimeModule();

        // LocalDateTime 序列化 & 反序列化
        javaTimeModule.addSerializer(LocalDateTime.class,
                new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(dateTimeFormat)));
        javaTimeModule.addDeserializer(LocalDateTime.class,
                new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern(dateTimeFormat)));

        // LocalDate 序列化 & 反序列化
        javaTimeModule.addSerializer(LocalDate.class,
                new LocalDateSerializer(DateTimeFormatter.ofPattern(dateFormat)));
        javaTimeModule.addDeserializer(LocalDate.class,
                new LocalDateDeserializer(DateTimeFormatter.ofPattern(dateFormat)));

        // 注册 JavaTimeModule
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
     * 配置并返回一个定制化的 ObjectMapper 实例。
     * 该方法配置了日期时间格式、JSON 序列化、反序列化和解析等相关设置。
     * 通过调用不同的定制方法，确保 ObjectMapper 在各种场景下都能正确处理数据。
     *
     * @return 配置完成的 ObjectMapper 实例
     */
    @Bean
    public ObjectMapper objectMapper() {
        // 创建 ObjectMapper 实例
        ObjectMapper objectMapper = new ObjectMapper();
        // 配置日期和时间的序列化与反序列化
        customizeJsonDateTime(objectMapper, DEFAULT_TIME_ZONE,DEFAULT_DATE_FORMAT,DEFAULT_DATE_TIME_FORMAT);
        // 配置 JSON 序列化相关设置
        customizeJsonSerialization(objectMapper);
        // 配置 JSON 反序列化相关设置
        customizeJsonDeserialization(objectMapper);
        // 配置 JSON 解析相关设置
        customizeJsonParsing(objectMapper);
        // 返回配置完成的 ObjectMapper 实例
        return objectMapper;
    }

}
