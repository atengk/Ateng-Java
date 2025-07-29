package local.ateng.java.customutils.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

/**
 * JSON 工具类
 * 提供常用的 JSON 序列化与反序列化方法
 * 使用 Jackson 实现
 * <p>
 * 建议全局复用 ObjectMapper 实例
 * </p>
 *
 * @author Ateng
 * @since 2025-07-28
 */
public final class JsonUtil {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    // 日期与时间格式化
    private static String DEFAULT_TIME_ZONE = "Asia/Shanghai";
    private static String DEFAULT_DATE_FORMAT = "yyyy-MM-dd";
    private static String DEFAULT_DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";


    static {
        // 配置日期和时间的序列化与反序列化
        customizeJsonDateTime(OBJECT_MAPPER, DEFAULT_TIME_ZONE, DEFAULT_DATE_FORMAT, DEFAULT_DATE_TIME_FORMAT);
        // 配置 JSON 序列化相关设置
        customizeJsonSerialization(OBJECT_MAPPER);
        // 配置 JSON 反序列化相关设置
        customizeJsonDeserialization(OBJECT_MAPPER);
        // 配置 JSON 解析相关设置
        customizeJsonParsing(OBJECT_MAPPER);
    }

    /**
     * 自定义 Jackson 时间日期的序列化和反序列化规则
     *
     * @param objectMapper Jackson 的 ObjectMapper 实例
     */
    private static void customizeJsonDateTime(ObjectMapper objectMapper, String timeZone, String dateFormat, String dateTimeFormat) {
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
    private static void customizeJsonSerialization(ObjectMapper objectMapper) {
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
    private static void customizeJsonDeserialization(ObjectMapper objectMapper) {
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
    private static void customizeJsonParsing(ObjectMapper objectMapper) {
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
     * 禁止实例化工具类
     */
    private JsonUtil() {
        throw new UnsupportedOperationException("工具类不可实例化");
    }

    /**
     * 对象转 JSON 字符串
     * 使用示例：
     * JsonUtil.toJson(myUser)
     * JsonUtil.toJson(list)
     *
     * @param obj 待序列化的对象
     * @return JSON 字符串，失败时返回 null
     */
    public static String toJson(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    /**
     * 对象转格式化（美化）后的 JSON 字符串
     *
     * @param obj 待序列化的对象
     * @return 格式化后的 JSON 字符串，失败时返回 null
     */
    public static String toPrettyJson(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    /**
     * JSON 字符串转对象
     * 使用示例：JsonUtil.fromJson(json, MyUser.class);
     *
     * @param json  JSON 字符串
     * @param clazz 目标类
     * @param <T>   类型参数
     * @return 转换后的对象，失败时返回 null
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        if (json == null || clazz == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(json, clazz);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * JSON 字符串转复杂泛型对象，如 List、Map 等
     * 使用示例：JsonUtil.fromJson(json, new TypeReference<List<MyUser>>() {});
     *
     * @param json    JSON 字符串
     * @param typeRef 类型引用
     * @param <T>     类型参数
     * @return 转换后的对象，失败时返回 null
     */
    public static <T> T fromJson(String json, TypeReference<T> typeRef) {
        if (json == null || typeRef == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(json, typeRef);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 判断字符串是否为合法 JSON
     *
     * @param json 待验证的字符串
     * @return 是合法 JSON 返回 true，否则返回 false
     */
    public static boolean isJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return false;
        }
        try {
            OBJECT_MAPPER.readTree(json);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 将 JSON 字符串转换为 Map 对象
     *
     * @param json JSON 字符串
     * @return 转换后的 Map，失败时返回空 Map
     */
    public static Map<String, Object> toMap(String json) {
        if (json == null) {
            return Collections.emptyMap();
        }
        try {
            return OBJECT_MAPPER.readValue(json, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    /**
     * 将 JSON 字符串转换为 List 对象
     *
     * @param json        JSON 字符串
     * @param elementType 列表中元素类型
     * @param <T>         类型参数
     * @return 转换后的 List，失败时返回空列表
     */
    public static <T> List<T> toList(String json, Class<T> elementType) {
        if (json == null || elementType == null) {
            return Collections.emptyList();
        }
        try {
            JavaType javaType = OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, elementType);
            return OBJECT_MAPPER.readValue(json, javaType);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    /**
     * 将对象进行深拷贝（基于 JSON 序列化与反序列化）
     *
     * @param obj   原始对象
     * @param clazz 对象类型
     * @param <T>   类型参数
     * @return 拷贝后的新对象，失败时返回 null
     */
    public static <T> T clone(T obj, Class<T> clazz) {
        if (obj == null || clazz == null) {
            return null;
        }
        return fromJson(toJson(obj), clazz);
    }

    /**
     * 读取 JSON 字符串为树形结构节点
     *
     * @param json JSON 字符串
     * @return JsonNode 对象，失败时返回 null
     */
    public static JsonNode readTree(String json) {
        if (json == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readTree(json);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 从 JSON 字符串中提取指定字段的值（字符串形式）
     *
     * @param json      JSON 字符串
     * @param fieldName 字段名称
     * @return 字段值字符串，失败时返回 null
     */
    public static String extract(String json, String fieldName) {
        return extract(json, fieldName, String.class);
    }

    /**
     * 从 JSON 中提取指定字段，并反序列化为指定类型
     *
     * @param json      JSON 字符串
     * @param fieldName 字段名称
     * @param clazz     字段类型
     * @param <T>       类型参数
     * @return 转换后的字段值，失败时返回 null
     */
    public static <T> T extract(String json, String fieldName, Class<T> clazz) {
        JsonNode node = readTree(json);
        if (node != null && node.has(fieldName)) {
            try {
                return OBJECT_MAPPER.treeToValue(node.get(fieldName), clazz);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    /**
     * 从 JSON 中提取指定字段，并反序列化为复杂泛型类型
     *
     * @param json      JSON 字符串
     * @param fieldName 字段名称
     * @param typeRef   字段类型引用
     * @param <T>       类型参数
     * @return 转换后的字段值，失败时返回 null
     */
    public static <T> T extract(String json, String fieldName, TypeReference<T> typeRef) {
        JsonNode node = readTree(json);
        if (node != null && node.has(fieldName)) {
            try {
                return OBJECT_MAPPER.readValue(node.get(fieldName).toString(), typeRef);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    /**
     * 类型转换，基于 Jackson convertValue
     *
     * @param fromValue   源对象
     * @param toValueType 目标类型
     * @param <T>         类型参数
     * @return 转换后的对象，失败返回 null
     */
    public static <T> T convertValue(Object fromValue, Class<T> toValueType) {
        if (fromValue == null || toValueType == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.convertValue(fromValue, toValueType);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 类型转换，基于 Jackson convertValue，支持泛型类型
     *
     * @param fromValue      源对象
     * @param toValueTypeRef 目标泛型类型引用
     * @param <T>            类型参数
     * @return 转换后的对象，失败返回 null
     */
    public static <T> T convertValue(Object fromValue, TypeReference<T> toValueTypeRef) {
        if (fromValue == null || toValueTypeRef == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.convertValue(fromValue, toValueTypeRef);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 更新 JSON 中指定路径的字段值，路径用点分割（如 "user.name"）
     *
     * @param json     原 JSON 字符串
     * @param path     点分割路径（如 "user.name"）
     * @param newValue 新字段值，支持任意类型
     * @return 修改后的 JSON 字符串，失败返回原 JSON
     */
    public static String updateField(String json, String path, Object newValue) {
        if (json == null || path == null || path.isEmpty()) {
            return json;
        }
        try {
            JsonNode rootNode = OBJECT_MAPPER.readTree(json);
            if (!(rootNode instanceof ObjectNode)) {
                return json;
            }
            ObjectNode objNode = (ObjectNode) rootNode;

            String[] keys = path.split("\\.");
            ObjectNode currentNode = objNode;
            for (int i = 0; i < keys.length - 1; i++) {
                JsonNode child = currentNode.get(keys[i]);
                if (child == null || !child.isObject()) {
                    // 新建空对象
                    ObjectNode newNode = OBJECT_MAPPER.createObjectNode();
                    currentNode.set(keys[i], newNode);
                    currentNode = newNode;
                } else {
                    currentNode = (ObjectNode) child;
                }
            }
            // 设置新值
            JsonNode newValueNode = OBJECT_MAPPER.valueToTree(newValue);
            currentNode.set(keys[keys.length - 1], newValueNode);

            return OBJECT_MAPPER.writeValueAsString(objNode);
        } catch (Exception e) {
            return json;
        }
    }

    /**
     * 移除 JSON 中指定路径的字段，路径用点分割（如 "user.name"）
     *
     * @param json 原 JSON 字符串
     * @param path 点分割路径
     * @return 修改后的 JSON 字符串，失败返回原 JSON
     */
    public static String removeField(String json, String path) {
        if (json == null || path == null || path.isEmpty()) {
            return json;
        }
        try {
            JsonNode rootNode = OBJECT_MAPPER.readTree(json);
            if (!(rootNode instanceof ObjectNode)) {
                return json;
            }
            ObjectNode objNode = (ObjectNode) rootNode;

            String[] keys = path.split("\\.");
            ObjectNode currentNode = objNode;
            for (int i = 0; i < keys.length - 1; i++) {
                JsonNode child = currentNode.get(keys[i]);
                if (child == null || !child.isObject()) {
                    return json; // 路径不存在，直接返回原 json
                }
                currentNode = (ObjectNode) child;
            }
            currentNode.remove(keys[keys.length - 1]);

            return OBJECT_MAPPER.writeValueAsString(objNode);
        } catch (Exception e) {
            return json;
        }
    }

    /**
     * 获取全局共享的 ObjectMapper 实例
     *
     * @return ObjectMapper 实例
     */
    public static ObjectMapper getObjectMapper() {
        return OBJECT_MAPPER;
    }

}
