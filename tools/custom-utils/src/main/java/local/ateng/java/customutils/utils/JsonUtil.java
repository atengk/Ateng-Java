package local.ateng.java.customutils.utils;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import local.ateng.java.customutils.config.JacksonObjectMapperFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigInteger;
import java.util.*;

/**
 * JSON 工具类。
 * 提供常用的 JSON 序列化、反序列化、树节点操作与路径读写能力。
 * 基于 Jackson 实现，建议全局复用 ObjectMapper 实例。
 *
 * @author Ateng
 * @since 2025-07-28
 */
public final class JsonUtil {

    /**
     * 日志对象。
     */
    private static final Logger log = LoggerFactory.getLogger(JsonUtil.class);

    /**
     * Jackson 的全局 ObjectMapper 实例。
     */
    private static volatile ObjectMapper OBJECT_MAPPER = JacksonObjectMapperFactory.buildDefaultObjectMapper();

    /**
     * 禁止实例化工具类。
     */
    private JsonUtil() {
        throw new UnsupportedOperationException("工具类不可实例化");
    }

    /**
     * 对象转 JSON 字符串。
     *
     * @param obj 待序列化的对象
     * @return JSON 字符串，失败时返回 null
     */
    public static String toJsonString(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn("对象转 JSON 失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 对象转 JSON 字符串，并支持指定写入器。
     *
     * @param obj          待序列化的对象
     * @param objectWriter JSON 写入器
     * @return JSON 字符串，失败时返回 null
     */
    public static String toJsonString(Object obj, ObjectWriter objectWriter) {
        if (obj == null) {
            return null;
        }
        try {
            ObjectWriter writer = objectWriter == null ? OBJECT_MAPPER.writer() : objectWriter;
            return writer.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn("对象转 JSON 失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 对象转 JSON 字符串，使用安全数字序列化规则。
     *
     * <p>输出 null 字段，基于字段序列化，Long 和 BigInteger 转字符串，BigDecimal 使用普通数字格式输出。</p>
     *
     * @param obj 待序列化的对象
     * @return JSON 字符串，失败时返回 null
     */
    public static String toJsonStringWithSafeNumber(Object obj) {
        return toJsonString(obj, buildSafeNumberObjectWriter());
    }

    /**
     * 构建安全数字序列化写入器。
     *
     * @return JSON 写入器
     */
    private static ObjectWriter buildSafeNumberObjectWriter() {
        ObjectMapper objectMapper = OBJECT_MAPPER.copy();

        // 输出 null 字段，Jackson 默认会输出 null，这里显式声明，便于统一规则
        objectMapper.setSerializationInclusion(JsonInclude.Include.ALWAYS);

        // 基于字段访问进行序列化，不依赖 Getter 方法
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
        objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

        SimpleModule simpleModule = new SimpleModule();
        // Long 包装类型转字符串，避免前端精度丢失
        simpleModule.addSerializer(Long.class, ToStringSerializer.instance);
        // long 基本类型转字符串，避免前端精度丢失
        simpleModule.addSerializer(Long.TYPE, ToStringSerializer.instance);
        // BigInteger 大整数转字符串，避免前端精度丢失
        simpleModule.addSerializer(BigInteger.class, ToStringSerializer.instance);
        objectMapper.registerModule(simpleModule);

        // BigDecimal 使用普通数字格式输出，避免科学计数法
        return objectMapper.writer()
                .with(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN);
    }

    /**
     * 对象转格式化后的 JSON 字符串。
     *
     * @param obj 待序列化的对象
     * @return 格式化后的 JSON 字符串，失败时返回 null
     */
    public static String toPrettyJsonString(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn("对象转 Pretty JSON 失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 对象转 JSON 字节数组。
     *
     * @param obj 待序列化的对象
     * @return JSON 字节数组，失败时返回 null
     */
    public static byte[] toJsonBytes(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsBytes(obj);
        } catch (JsonProcessingException e) {
            log.warn("对象转 JSON 字节数组失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * JSON 字符串转对象。
     *
     * @param json  JSON 字符串
     * @param clazz 目标类
     * @param <T>   类型参数
     * @return 转换后的对象，失败时返回 null
     */
    public static <T> T parseObject(String json, Class<T> clazz) {
        if (StringUtil.isBlank(json) || clazz == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(json, clazz);
        } catch (Exception e) {
            log.warn("JSON 字符串转对象失败, class={}: {}", clazz.getName(), e.getMessage());
            return null;
        }
    }

    /**
     * JSON 字节数组转对象。
     *
     * @param bytes JSON 字节数组
     * @param clazz 目标类
     * @param <T>   类型参数
     * @return 转换后的对象，失败时返回 null
     */
    public static <T> T parseObject(byte[] bytes, Class<T> clazz) {
        if (bytes == null || clazz == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(bytes, clazz);
        } catch (Exception e) {
            log.warn("JSON 字节数组转对象失败, class={}: {}", clazz.getName(), e.getMessage());
            return null;
        }
    }

    /**
     * JSON 字符串转复杂泛型对象，如 List、Map 等。
     *
     * @param json    JSON 字符串
     * @param typeRef 类型引用
     * @param <T>     类型参数
     * @return 转换后的对象，失败时返回 null
     */
    public static <T> T parseObject(String json, TypeReference<T> typeRef) {
        if (StringUtil.isBlank(json) || typeRef == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(json, typeRef);
        } catch (Exception e) {
            log.warn("JSON 字符串转复杂对象失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * JSON 字节数组转复杂泛型对象，如 List、Map 等。
     *
     * @param bytes   JSON 字节数组
     * @param typeRef 类型引用
     * @param <T>     类型参数
     * @return 转换后的对象，失败时返回 null
     */
    public static <T> T parseObject(byte[] bytes, TypeReference<T> typeRef) {
        if (bytes == null || typeRef == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(bytes, typeRef);
        } catch (Exception e) {
            log.warn("JSON 字节数组转复杂对象失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * JSON 字符串转指定 JavaType 对象。
     *
     * @param json     JSON 字符串
     * @param javaType 目标类型
     * @param <T>      类型参数
     * @return 转换后的对象，失败时返回 null
     */
    public static <T> T parseObject(String json, JavaType javaType) {
        if (StringUtil.isBlank(json) || javaType == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(json, javaType);
        } catch (Exception e) {
            log.warn("JSON 字符串转 JavaType 对象失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * JSON 字节数组转指定 JavaType 对象。
     *
     * @param bytes    JSON 字节数组
     * @param javaType 目标类型
     * @param <T>      类型参数
     * @return 转换后的对象，失败时返回 null
     */
    public static <T> T parseObject(byte[] bytes, JavaType javaType) {
        if (bytes == null || javaType == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(bytes, javaType);
        } catch (Exception e) {
            log.warn("JSON 字节数组转 JavaType 对象失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 将 JSON 字符串转换为 Map<String, Object>。
     *
     * @param json JSON 字符串
     * @return 转换后的 Map，失败时返回空 Map
     */
    public static Map<String, Object> parseMap(String json) {
        if (StringUtil.isBlank(json)) {
            return Collections.emptyMap();
        }
        try {
            return OBJECT_MAPPER.readValue(json, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            log.warn("JSON 字符串转 Map<String, Object> 失败: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    /**
     * 将 JSON 字符串转换为指定键值类型的 Map。
     *
     * @param json      JSON 字符串
     * @param keyType   键类型
     * @param valueType 值类型
     * @param <K>       键类型参数
     * @param <V>       值类型参数
     * @return 转换后的 Map，失败时返回空 Map
     */
    public static <K, V> Map<K, V> parseMap(String json, Class<K> keyType, Class<V> valueType) {
        if (StringUtil.isBlank(json) || keyType == null || valueType == null) {
            return Collections.emptyMap();
        }
        try {
            JavaType javaType = OBJECT_MAPPER.getTypeFactory().constructMapType(LinkedHashMap.class, keyType, valueType);
            return OBJECT_MAPPER.readValue(json, javaType);
        } catch (Exception e) {
            log.warn("JSON 字符串转指定类型 Map 失败: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    /**
     * 将 JSON 字符串转换为 List 对象。
     *
     * @param json        JSON 字符串
     * @param elementType 列表中元素类型
     * @param <T>         类型参数
     * @return 转换后的 List，失败时返回空列表
     */
    public static <T> List<T> parseList(String json, Class<T> elementType) {
        if (StringUtil.isBlank(json) || elementType == null) {
            return Collections.emptyList();
        }
        try {
            JavaType javaType = OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, elementType);
            return OBJECT_MAPPER.readValue(json, javaType);
        } catch (Exception e) {
            log.warn("JSON 字符串转 List 失败, elementType={}: {}", elementType.getName(), e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 将 JSON 字符串转换为 Set 对象。
     *
     * @param json        JSON 字符串
     * @param elementType 集合中元素类型
     * @param <T>         类型参数
     * @return 转换后的 Set，失败时返回空集合
     */
    public static <T> Set<T> parseSet(String json, Class<T> elementType) {
        if (StringUtil.isBlank(json) || elementType == null) {
            return Collections.emptySet();
        }
        try {
            JavaType javaType = OBJECT_MAPPER.getTypeFactory().constructCollectionType(Set.class, elementType);
            return OBJECT_MAPPER.readValue(json, javaType);
        } catch (Exception e) {
            log.warn("JSON 字符串转 Set 失败, elementType={}: {}", elementType.getName(), e.getMessage());
            return Collections.emptySet();
        }
    }

    /**
     * 判断字符串是否为合法 JSON。
     *
     * @param json 待验证的字符串
     * @return 是合法 JSON 返回 true，否则返回 false
     */
    public static boolean isJson(String json) {
        if (StringUtil.isBlank(json)) {
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
     * 判断字符串是否为 JSON 对象。
     *
     * @param json 待验证的字符串
     * @return 是 JSON 对象返回 true，否则返回 false
     */
    public static boolean isObjectJson(String json) {
        JsonNode node = readTree(json);
        return node != null && node.isObject();
    }

    /**
     * 判断字符串是否为 JSON 数组。
     *
     * @param json 待验证的字符串
     * @return 是 JSON 数组返回 true，否则返回 false
     */
    public static boolean isArrayJson(String json) {
        JsonNode node = readTree(json);
        return node != null && node.isArray();
    }

    /**
     * 判断 JSON 是否为空对象或空数组。
     *
     * @param json JSON 字符串
     * @return 是空 JSON 返回 true，否则返回 false
     */
    public static boolean isEmptyJson(String json) {
        if (StringUtil.isBlank(json)) {
            return true;
        }
        JsonNode node = readTree(json);
        if (node == null) {
            return true;
        }
        if (node.isObject()) {
            return node.size() == 0;
        }
        if (node.isArray()) {
            return node.size() == 0;
        }
        return false;
    }

    /**
     * 将对象进行深拷贝。
     *
     * @param obj   原始对象
     * @param clazz 对象类型
     * @param <T>   类型参数
     * @return 拷贝后的新对象，失败时返回 null
     */
    public static <T> T copy(T obj, Class<T> clazz) {
        if (obj == null || clazz == null) {
            return null;
        }
        return convert(obj, clazz);
    }

    /**
     * 将对象进行深拷贝，支持泛型目标类型。
     *
     * @param obj     原始对象
     * @param typeRef 目标类型引用
     * @param <T>     类型参数
     * @return 拷贝后的新对象，失败时返回 null
     */
    public static <T> T copy(T obj, TypeReference<T> typeRef) {
        if (obj == null || typeRef == null) {
            return null;
        }
        return convert(obj, typeRef);
    }

    /**
     * 读取 JSON 字符串为树形结构节点。
     *
     * @param json JSON 字符串
     * @return JsonNode 对象，失败时返回 null
     */
    public static JsonNode readTree(String json) {
        if (StringUtil.isBlank(json)) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readTree(json);
        } catch (Exception e) {
            log.warn("读取 JSON 树失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 读取 JSON 字节数组为树形结构节点。
     *
     * @param bytes JSON 字节数组
     * @return JsonNode 对象，失败时返回 null
     */
    public static JsonNode readTree(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readTree(bytes);
        } catch (IOException e) {
            log.warn("读取 JSON 字节数组树失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 将对象转换为 JsonNode。
     *
     * @param obj 源对象
     * @return JsonNode 对象，失败时返回 null
     */
    public static JsonNode toJsonNode(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.valueToTree(obj);
        } catch (Exception e) {
            log.warn("对象转 JsonNode 失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 从 JSON 字符串中提取指定字段的值（字符串形式）。
     *
     * @param json      JSON 字符串
     * @param fieldName 字段名称，支持点路径，例如 user.name
     * @return 字段值字符串，失败时返回 null
     */
    public static String get(String json, String fieldName) {
        return get(json, fieldName, String.class);
    }

    /**
     * 从 JSON 中提取指定字段，并反序列化为指定类型。
     *
     * @param json      JSON 字符串
     * @param fieldName 字段名称，支持点路径，例如 user.name
     * @param clazz     字段类型
     * @param <T>       类型参数
     * @return 转换后的字段值，失败时返回 null
     */
    public static <T> T get(String json, String fieldName, Class<T> clazz) {
        if (StringUtil.isBlank(json) || StringUtil.isBlank(fieldName) || clazz == null) {
            return null;
        }
        JsonNode node = getNode(json, fieldName);
        if (node == null || node.isNull()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.treeToValue(node, clazz);
        } catch (Exception e) {
            log.warn("从 JSON 提取字段失败, fieldName={}, class={}: {}", fieldName, clazz.getName(), e.getMessage());
            return null;
        }
    }

    /**
     * 从 JSON 中提取指定字段，并反序列化为复杂泛型类型。
     *
     * @param json      JSON 字符串
     * @param fieldName 字段名称，支持点路径，例如 user.name
     * @param typeRef   字段类型引用
     * @param <T>       类型参数
     * @return 转换后的字段值，失败时返回 null
     */
    public static <T> T get(String json, String fieldName, TypeReference<T> typeRef) {
        if (StringUtil.isBlank(json) || StringUtil.isBlank(fieldName) || typeRef == null) {
            return null;
        }
        JsonNode node = getNode(json, fieldName);
        if (node == null || node.isNull()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(node.toString(), typeRef);
        } catch (Exception e) {
            log.warn("从 JSON 提取复杂字段失败, fieldName={}: {}", fieldName, e.getMessage());
            return null;
        }
    }

    /**
     * 从 JSON 中根据路径提取指定节点。
     *
     * @param json JSON 字符串
     * @param path 路径，支持点路径，例如 user.name
     * @return JsonNode，失败时返回 null
     */
    public static JsonNode getNode(String json, String path) {
        JsonNode rootNode = readTree(json);
        return getNode(rootNode, path);
    }

    /**
     * 从 JsonNode 中根据路径提取指定节点。
     *
     * @param rootNode 根节点
     * @param path     路径，支持点路径，例如 user.name
     * @return JsonNode，失败时返回 null
     */
    public static JsonNode getNode(JsonNode rootNode, String path) {
        if (rootNode == null || StringUtil.isBlank(path)) {
            return null;
        }
        String[] keys = path.split("\\.");
        JsonNode current = rootNode;
        for (String key : keys) {
            if (current == null) {
                return null;
            }
            current = current.get(key);
        }
        return current;
    }

    /**
     * 判断 JSON 中是否存在指定路径。
     *
     * @param json JSON 字符串
     * @param path 路径，支持点路径
     * @return 存在返回 true，否则返回 false
     */
    public static boolean has(String json, String path) {
        return getNode(json, path) != null;
    }

    /**
     * 类型转换，基于 Jackson convertValue。
     *
     * @param fromValue   源对象
     * @param toValueType 目标类型
     * @param <T>         类型参数
     * @return 转换后的对象，失败返回 null
     */
    public static <T> T convert(Object fromValue, Class<T> toValueType) {
        if (fromValue == null || toValueType == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.convertValue(fromValue, toValueType);
        } catch (Exception e) {
            log.warn("对象类型转换失败, targetClass={}: {}", toValueType.getName(), e.getMessage());
            return null;
        }
    }

    /**
     * 类型转换，基于 Jackson convertValue，支持泛型类型。
     *
     * @param fromValue      源对象
     * @param toValueTypeRef 目标泛型类型引用
     * @param <T>            类型参数
     * @return 转换后的对象，失败返回 null
     */
    public static <T> T convert(Object fromValue, TypeReference<T> toValueTypeRef) {
        if (fromValue == null || toValueTypeRef == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.convertValue(fromValue, toValueTypeRef);
        } catch (Exception e) {
            log.warn("对象泛型转换失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 类型转换，基于 Jackson convertValue，支持 JavaType。
     *
     * @param fromValue   源对象
     * @param toValueType 目标类型
     * @param <T>         类型参数
     * @return 转换后的对象，失败返回 null
     */
    public static <T> T convert(Object fromValue, JavaType toValueType) {
        if (fromValue == null || toValueType == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.convertValue(fromValue, toValueType);
        } catch (Exception e) {
            log.warn("对象 JavaType 转换失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 对 JSON 中指定路径的字段赋值，路径用点分割，例如 user.name。
     * 中间节点不存在时会自动创建对象节点。
     *
     * @param json     原 JSON 字符串
     * @param path     点分割路径
     * @param newValue 新字段值，支持任意类型
     * @return 修改后的 JSON 字符串，失败返回原 JSON
     */
    public static String put(String json, String path, Object newValue) {
        if (StringUtil.isBlank(json) || StringUtil.isBlank(path)) {
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
                if (child != null && child.isObject()) {
                    currentNode = (ObjectNode) child;
                } else {
                    ObjectNode newNode = OBJECT_MAPPER.createObjectNode();
                    currentNode.set(keys[i], newNode);
                    currentNode = newNode;
                }
            }

            currentNode.set(keys[keys.length - 1], OBJECT_MAPPER.valueToTree(newValue));
            return OBJECT_MAPPER.writeValueAsString(objNode);
        } catch (Exception e) {
            log.warn("JSON 路径赋值失败, path={}: {}", path, e.getMessage());
            return json;
        }
    }

    /**
     * 移除 JSON 中指定路径的字段，路径用点分割，例如 user.name。
     *
     * @param json 原 JSON 字符串
     * @param path 点分割路径
     * @return 修改后的 JSON 字符串，失败返回原 JSON
     */
    public static String remove(String json, String path) {
        if (StringUtil.isBlank(json) || StringUtil.isBlank(path)) {
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
                    return json;
                }
                currentNode = (ObjectNode) child;
            }

            currentNode.remove(keys[keys.length - 1]);
            return OBJECT_MAPPER.writeValueAsString(objNode);
        } catch (Exception e) {
            log.warn("JSON 路径删除失败, path={}: {}", path, e.getMessage());
            return json;
        }
    }

    /**
     * 合并两个 JSON 字符串，后者覆盖前者同名字段。
     * 这是浅合并，不递归合并嵌套对象。
     *
     * @param targetJson 目标 JSON
     * @param sourceJson 源 JSON
     * @return 合并后的 JSON，失败返回目标 JSON
     */
    public static String merge(String targetJson, String sourceJson) {
        if (StringUtil.isBlank(targetJson)) {
            return sourceJson;
        }
        if (StringUtil.isBlank(sourceJson)) {
            return targetJson;
        }
        try {
            JsonNode targetNode = OBJECT_MAPPER.readTree(targetJson);
            JsonNode sourceNode = OBJECT_MAPPER.readTree(sourceJson);
            if (!(targetNode instanceof ObjectNode) || !(sourceNode instanceof ObjectNode)) {
                return targetJson;
            }
            ObjectNode targetObj = (ObjectNode) targetNode;
            targetObj.setAll((ObjectNode) sourceNode);
            return OBJECT_MAPPER.writeValueAsString(targetObj);
        } catch (Exception e) {
            log.warn("JSON 合并失败: {}", e.getMessage());
            return targetJson;
        }
    }

    /**
     * 将 Map 合并进 JSON 字符串，后者覆盖前者同名字段。
     *
     * @param targetJson 目标 JSON
     * @param sourceMap  源 Map
     * @return 合并后的 JSON，失败返回目标 JSON
     */
    public static String merge(String targetJson, Map<String, Object> sourceMap) {
        if (StringUtil.isBlank(targetJson) || sourceMap == null || sourceMap.isEmpty()) {
            return targetJson;
        }
        try {
            JsonNode targetNode = OBJECT_MAPPER.readTree(targetJson);
            if (!(targetNode instanceof ObjectNode)) {
                return targetJson;
            }
            ObjectNode targetObj = (ObjectNode) targetNode;
            JsonNode sourceNode = OBJECT_MAPPER.valueToTree(sourceMap);
            if (sourceNode != null && sourceNode.isObject()) {
                targetObj.setAll((ObjectNode) sourceNode);
            }
            return OBJECT_MAPPER.writeValueAsString(targetObj);
        } catch (Exception e) {
            log.warn("Map 合并 JSON 失败: {}", e.getMessage());
            return targetJson;
        }
    }

    /**
     * 将 JSON 标准化为紧凑格式。
     *
     * @param json JSON 字符串
     * @return 紧凑 JSON，失败返回原字符串
     */
    public static String normalize(String json) {
        JsonNode node = readTree(json);
        if (node == null) {
            return json;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(node);
        } catch (Exception e) {
            log.warn("JSON 标准化失败: {}", e.getMessage());
            return json;
        }
    }

    /**
     * 将 JSON 美化输出。
     *
     * @param json JSON 字符串
     * @return 美化后的 JSON，失败返回原字符串
     */
    public static String pretty(String json) {
        JsonNode node = readTree(json);
        if (node == null) {
            return json;
        }
        try {
            return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(node);
        } catch (Exception e) {
            log.warn("JSON 美化失败: {}", e.getMessage());
            return json;
        }
    }

    /**
     * 获取全局共享的 ObjectMapper 实例。
     *
     * @return ObjectMapper 实例
     */
    public static ObjectMapper getObjectMapper() {
        return OBJECT_MAPPER;
    }

    /**
     * 替换全局 ObjectMapper 实例。
     * 一般只建议在应用启动阶段调用。
     *
     * @param objectMapper 新的 ObjectMapper
     */
    public static synchronized void setObjectMapper(ObjectMapper objectMapper) {
        if (objectMapper != null) {
            OBJECT_MAPPER = objectMapper;
        }
    }

    /**
     * 使用 JSON Pointer 获取节点。
     * 例如：/user/name、/items/0/id。
     *
     * @param json    JSON 字符串
     * @param pointer JSON Pointer 表达式
     * @return 节点，失败返回 null
     */
    public static JsonNode getNodeByPointer(String json, String pointer) {
        if (StringUtil.isBlank(json) || StringUtil.isBlank(pointer)) {
            return null;
        }
        try {
            JsonNode rootNode = OBJECT_MAPPER.readTree(json);
            return rootNode.at(pointer);
        } catch (Exception e) {
            log.warn("根据 JSON Pointer 获取节点失败, pointer={}: {}", pointer, e.getMessage());
            return null;
        }
    }

    /**
     * 根据 JSON Pointer 获取指定类型的值。
     *
     * @param json    JSON 字符串
     * @param pointer JSON Pointer 表达式
     * @param clazz   目标类型
     * @param <T>     类型参数
     * @return 转换后的值，失败返回 null
     */
    public static <T> T getByPointer(String json, String pointer, Class<T> clazz) {
        if (StringUtil.isBlank(json) || StringUtil.isBlank(pointer) || clazz == null) {
            return null;
        }
        JsonNode node = getNodeByPointer(json, pointer);
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.treeToValue(node, clazz);
        } catch (Exception e) {
            log.warn("根据 JSON Pointer 提取值失败, pointer={}, class={}: {}", pointer, clazz.getName(), e.getMessage());
            return null;
        }
    }

    /**
     * 根据 JSON Pointer 获取复杂泛型值。
     *
     * @param json    JSON 字符串
     * @param pointer JSON Pointer 表达式
     * @param typeRef 类型引用
     * @param <T>     类型参数
     * @return 转换后的值，失败返回 null
     */
    public static <T> T getByPointer(String json, String pointer, TypeReference<T> typeRef) {
        if (StringUtil.isBlank(json) || StringUtil.isBlank(pointer) || typeRef == null) {
            return null;
        }
        JsonNode node = getNodeByPointer(json, pointer);
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(node.toString(), typeRef);
        } catch (Exception e) {
            log.warn("根据 JSON Pointer 提取复杂值失败, pointer={}: {}", pointer, e.getMessage());
            return null;
        }
    }

    /**
     * 安全获取字符串字段值，支持默认值。
     *
     * @param json         JSON 字符串
     * @param fieldName    字段名或点路径
     * @param defaultValue 默认值
     * @return 字段值，失败或为空时返回默认值
     */
    public static String getStringOrDefault(String json, String fieldName, String defaultValue) {
        String value = get(json, fieldName, String.class);
        return StringUtil.isBlank(value) ? defaultValue : value;
    }

    /**
     * 安全获取整数字段值，支持默认值。
     *
     * @param json         JSON 字符串
     * @param fieldName    字段名或点路径
     * @param defaultValue 默认值
     * @return 字段值，失败时返回默认值
     */
    public static Integer getIntegerOrDefault(String json, String fieldName, Integer defaultValue) {
        Integer value = get(json, fieldName, Integer.class);
        return value == null ? defaultValue : value;
    }

    /**
     * 安全获取长整型字段值，支持默认值。
     *
     * @param json         JSON 字符串
     * @param fieldName    字段名或点路径
     * @param defaultValue 默认值
     * @return 字段值，失败时返回默认值
     */
    public static Long getLongOrDefault(String json, String fieldName, Long defaultValue) {
        Long value = get(json, fieldName, Long.class);
        return value == null ? defaultValue : value;
    }

    /**
     * 安全获取布尔字段值，支持默认值。
     *
     * @param json         JSON 字符串
     * @param fieldName    字段名或点路径
     * @param defaultValue 默认值
     * @return 字段值，失败时返回默认值
     */
    public static Boolean getBooleanOrDefault(String json, String fieldName, Boolean defaultValue) {
        Boolean value = get(json, fieldName, Boolean.class);
        return value == null ? defaultValue : value;
    }

    /**
     * 安全获取双精度字段值，支持默认值。
     *
     * @param json         JSON 字符串
     * @param fieldName    字段名或点路径
     * @param defaultValue 默认值
     * @return 字段值，失败时返回默认值
     */
    public static Double getDoubleOrDefault(String json, String fieldName, Double defaultValue) {
        Double value = get(json, fieldName, Double.class);
        return value == null ? defaultValue : value;
    }

    /**
     * 获取指定路径下的数组节点。
     *
     * @param json JSON 字符串
     * @param path 点路径，例如 data.items
     * @return 数组节点，失败返回 null
     */
    public static JsonNode getArrayNode(String json, String path) {
        JsonNode node = getNode(json, path);
        if (node != null && node.isArray()) {
            return node;
        }
        return null;
    }

    /**
     * 根据路径获取数组中的某个元素节点。
     * 支持路径格式：items[0]、user.list[2].name。
     *
     * @param json JSON 字符串
     * @param path 带数组下标的路径
     * @return 节点，失败返回 null
     */
    public static JsonNode getNodeWithIndex(String json, String path) {
        if (StringUtil.isBlank(json) || StringUtil.isBlank(path)) {
            return null;
        }
        try {
            JsonNode current = OBJECT_MAPPER.readTree(json);
            String[] parts = path.split("\\.");
            for (String part : parts) {
                if (current == null) {
                    return null;
                }
                int left = part.indexOf('[');
                int right = part.indexOf(']');
                if (left > -1 && right > left) {
                    String fieldName = part.substring(0, left);
                    int index = Integer.parseInt(part.substring(left + 1, right));
                    current = current.get(fieldName);
                    if (current == null || !current.isArray() || index < 0 || index >= current.size()) {
                        return null;
                    }
                    current = current.get(index);
                } else {
                    current = current.get(part);
                }
            }
            return current;
        } catch (Exception e) {
            log.warn("根据数组路径获取节点失败, path={}: {}", path, e.getMessage());
            return null;
        }
    }

    /**
     * 根据路径获取数组中的某个元素并转换为指定类型。
     *
     * @param json  JSON 字符串
     * @param path  带数组下标的路径
     * @param clazz 目标类型
     * @param <T>   类型参数
     * @return 转换后的对象，失败返回 null
     */
    public static <T> T getWithIndex(String json, String path, Class<T> clazz) {
        if (StringUtil.isBlank(json) || StringUtil.isBlank(path) || clazz == null) {
            return null;
        }
        JsonNode node = getNodeWithIndex(json, path);
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.treeToValue(node, clazz);
        } catch (Exception e) {
            log.warn("根据数组路径提取对象失败, path={}, class={}: {}", path, clazz.getName(), e.getMessage());
            return null;
        }
    }

    /**
     * 将对象转换为 Map<String, Object>。
     *
     * @param obj 源对象
     * @return Map，失败返回空 Map
     */
    public static Map<String, Object> toMap(Object obj) {
        if (obj == null) {
            return Collections.emptyMap();
        }
        try {
            return OBJECT_MAPPER.convertValue(obj, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            log.warn("对象转 Map<String, Object> 失败: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    /**
     * 将对象转换为指定键值类型的 Map。
     *
     * @param obj       源对象
     * @param keyType   键类型
     * @param valueType 值类型
     * @param <K>       键类型参数
     * @param <V>       值类型参数
     * @return Map，失败返回空 Map
     */
    public static <K, V> Map<K, V> toMap(Object obj, Class<K> keyType, Class<V> valueType) {
        if (obj == null || keyType == null || valueType == null) {
            return Collections.emptyMap();
        }
        try {
            JavaType javaType = OBJECT_MAPPER.getTypeFactory().constructMapType(LinkedHashMap.class, keyType, valueType);
            return OBJECT_MAPPER.convertValue(obj, javaType);
        } catch (Exception e) {
            log.warn("对象转指定类型 Map 失败: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    /**
     * 将对象转换为 List。
     *
     * @param obj         源对象
     * @param elementType 列表元素类型
     * @param <T>         类型参数
     * @return List，失败返回空列表
     */
    public static <T> List<T> toList(Object obj, Class<T> elementType) {
        if (obj == null || elementType == null) {
            return Collections.emptyList();
        }
        try {
            JavaType javaType = OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, elementType);
            return OBJECT_MAPPER.convertValue(obj, javaType);
        } catch (Exception e) {
            log.warn("对象转 List 失败, elementType={}: {}", elementType.getName(), e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 将 JSON 数组字符串转换为对象列表，支持复杂泛型元素。
     *
     * @param json    JSON 数组字符串
     * @param typeRef 元素类型引用，通常为 List<T> 的 TypeReference
     * @param <T>     类型参数
     * @return 转换后的 List，失败返回空列表
     */
    public static <T> List<T> parseList(String json, TypeReference<List<T>> typeRef) {
        if (StringUtil.isBlank(json) || typeRef == null) {
            return Collections.emptyList();
        }
        try {
            return OBJECT_MAPPER.readValue(json, typeRef);
        } catch (Exception e) {
            log.warn("JSON 字符串转复杂 List 失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 将 JSON 数组字符串转换为对象集合，返回 LinkedHashSet 保留顺序。
     *
     * @param json        JSON 数组字符串
     * @param elementType 元素类型
     * @param <T>         类型参数
     * @return 转换后的 Set，失败返回空集合
     */
    public static <T> Set<T> parseLinkedHashSet(String json, Class<T> elementType) {
        if (StringUtil.isBlank(json) || elementType == null) {
            return Collections.emptySet();
        }
        try {
            JavaType javaType = OBJECT_MAPPER.getTypeFactory().constructCollectionType(java.util.LinkedHashSet.class, elementType);
            return OBJECT_MAPPER.readValue(json, javaType);
        } catch (Exception e) {
            log.warn("JSON 字符串转 LinkedHashSet 失败, elementType={}: {}", elementType.getName(), e.getMessage());
            return Collections.emptySet();
        }
    }

    /**
     * 递归深度合并两个 JSON 对象。
     * source 中的字段会覆盖 target 中同名字段。
     *
     * @param targetJson 目标 JSON
     * @param sourceJson 源 JSON
     * @return 合并后的 JSON，失败返回目标 JSON
     */
    public static String deepMerge(String targetJson, String sourceJson) {
        if (StringUtil.isBlank(targetJson)) {
            return sourceJson;
        }
        if (StringUtil.isBlank(sourceJson)) {
            return targetJson;
        }
        try {
            JsonNode targetNode = OBJECT_MAPPER.readTree(targetJson);
            JsonNode sourceNode = OBJECT_MAPPER.readTree(sourceJson);
            JsonNode mergedNode = deepMergeNode(targetNode, sourceNode);
            return OBJECT_MAPPER.writeValueAsString(mergedNode);
        } catch (Exception e) {
            log.warn("JSON 深度合并失败: {}", e.getMessage());
            return targetJson;
        }
    }

    /**
     * 递归深度合并两个节点。
     *
     * @param target 目标节点
     * @param source 源节点
     * @return 合并后的节点
     */
    private static JsonNode deepMergeNode(JsonNode target, JsonNode source) {
        if (target == null) {
            return source;
        }
        if (source == null) {
            return target;
        }
        if (target.isObject() && source.isObject()) {
            ObjectNode targetObj = (ObjectNode) target;
            source.fields().forEachRemaining(entry -> {
                String key = entry.getKey();
                JsonNode sourceValue = entry.getValue();
                JsonNode targetValue = targetObj.get(key);
                if (targetValue != null && targetValue.isObject() && sourceValue.isObject()) {
                    targetObj.set(key, deepMergeNode(targetValue, sourceValue));
                } else {
                    targetObj.set(key, sourceValue);
                }
            });
            return targetObj;
        }
        return source;
    }

    /**
     * 将 JSON 对象扁平化为 Map。
     * 例如：{"user":{"name":"a"}} -> {"user.name":"a"}
     *
     * @param json JSON 字符串
     * @return 扁平化结果，失败返回空 Map
     */
    public static Map<String, Object> flatten(String json) {
        JsonNode node = readTree(json);
        if (node == null || !node.isObject()) {
            return Collections.emptyMap();
        }
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        flattenNode("", node, result);
        return result;
    }

    /**
     * 递归扁平化节点。
     *
     * @param prefix 前缀路径
     * @param node   当前节点
     * @param result 结果容器
     */
    private static void flattenNode(String prefix, JsonNode node, Map<String, Object> result) {
        if (node == null) {
            return;
        }
        if (node.isObject()) {
            node.fields().forEachRemaining(entry -> {
                String key = StringUtil.isBlank(prefix) ? entry.getKey() : prefix + "." + entry.getKey();
                flattenNode(key, entry.getValue(), result);
            });
            return;
        }
        if (node.isArray()) {
            for (int i = 0; i < node.size(); i++) {
                String key = prefix + "[" + i + "]";
                flattenNode(key, node.get(i), result);
            }
            return;
        }
        result.put(prefix, node.isNull() ? null : node.asText());
    }

    /**
     * 将对象转换成指定字段的值列表。
     * 适合从对象数组中提取某个字段，例如 userList -> id 列表。
     *
     * @param obj       源对象，通常为集合或数组
     * @param fieldName 字段名
     * @param fieldType 字段类型
     * @param <T>       字段类型参数
     * @return 字段值列表，失败返回空列表
     */
    public static <T> List<T> extractFieldList(Object obj, String fieldName, Class<T> fieldType) {
        if (obj == null || StringUtil.isBlank(fieldName) || fieldType == null) {
            return Collections.emptyList();
        }
        try {
            JsonNode node = toJsonNode(obj);
            if (node == null || !node.isArray()) {
                return Collections.emptyList();
            }
            java.util.ArrayList<T> result = new java.util.ArrayList<T>();
            for (JsonNode item : node) {
                JsonNode fieldNode = item.get(fieldName);
                if (fieldNode == null || fieldNode.isNull()) {
                    continue;
                }
                T value = OBJECT_MAPPER.treeToValue(fieldNode, fieldType);
                if (value != null) {
                    result.add(value);
                }
            }
            return result;
        } catch (Exception e) {
            log.warn("提取字段列表失败, fieldName={}, fieldType={}: {}", fieldName, fieldType.getName(), e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 判断 JSON 中指定路径的字段是否为空。
     * 字符串空、null、空数组、空对象都会视为“空”。
     *
     * @param json JSON 字符串
     * @param path 路径
     * @return 是空返回 true，否则返回 false
     */
    public static boolean isEmptyAt(String json, String path) {
        JsonNode node = getNode(json, path);
        if (node == null || node.isMissingNode() || node.isNull()) {
            return true;
        }
        if (node.isTextual()) {
            return StringUtil.isBlank(node.asText());
        }
        if (node.isArray() || node.isObject()) {
            return node.size() == 0;
        }
        return false;
    }

    /**
     * 判断 JSON 中指定路径的字段是否存在且非空。
     *
     * @param json JSON 字符串
     * @param path 路径
     * @return 存在且非空返回 true，否则返回 false
     */
    public static boolean hasNonEmpty(String json, String path) {
        return !isEmptyAt(json, path);
    }

    /**
     * 将 JSON 字符串转换为字节数组再恢复，强制刷新序列化结果。
     * 适合用于规整对象字段顺序或清理临时代理对象。
     *
     * @param json JSON 字符串
     * @return 标准化后的 JSON 字符串，失败返回原字符串
     */
    public static String normalizeByBytes(String json) {
        if (StringUtil.isBlank(json)) {
            return json;
        }
        try {
            JsonNode node = OBJECT_MAPPER.readTree(json);
            byte[] bytes = OBJECT_MAPPER.writeValueAsBytes(node);
            return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("通过字节数组标准化 JSON 失败: {}", e.getMessage());
            return json;
        }
    }

    /**
     * 根据带数组下标的路径写入值。
     * 支持路径示例：items[0].name、user.list[2].id、matrix[0][1]。
     * 中间节点不存在时会自动创建对象节点或数组节点。
     *
     * @param json 原 JSON 字符串
     * @param path 路径表达式
     * @param newValue 新值
     * @return 修改后的 JSON 字符串，失败返回原 JSON
     */
    public static String putWithIndexPath(String json, String path, Object newValue) {
        if (StringUtil.isBlank(path)) {
            return json;
        }
        try {
            List<PathStep> steps = parsePathSteps(path);
            if (steps.isEmpty()) {
                return json;
            }

            JsonNode rootNode;
            if (StringUtil.isBlank(json)) {
                rootNode = createRootContainer(steps.get(0));
            } else {
                rootNode = OBJECT_MAPPER.readTree(json);
                if (rootNode == null || rootNode.isMissingNode() || rootNode.isNull()) {
                    rootNode = createRootContainer(steps.get(0));
                }
            }

            JsonNode valueNode = OBJECT_MAPPER.valueToTree(newValue);
            JsonNode updatedNode = putBySteps(rootNode, steps, 0, valueNode);
            return OBJECT_MAPPER.writeValueAsString(updatedNode);
        } catch (Exception e) {
            log.warn("根据数组路径写入 JSON 失败, path={}: {}", path, e.getMessage());
            return json;
        }
    }

    /**
     * 根据带数组下标的路径删除字段或数组元素。
     * 支持路径示例：items[0].name、user.list[2]、matrix[0][1]。
     *
     * @param json 原 JSON 字符串
     * @param path 路径表达式
     * @return 修改后的 JSON 字符串，失败返回原 JSON
     */
    public static String removeWithIndexPath(String json, String path) {
        if (StringUtil.isBlank(json) || StringUtil.isBlank(path)) {
            return json;
        }
        try {
            JsonNode rootNode = OBJECT_MAPPER.readTree(json);
            if (rootNode == null || rootNode.isMissingNode() || rootNode.isNull()) {
                return json;
            }

            List<PathStep> steps = parsePathSteps(path);
            if (steps.isEmpty()) {
                return json;
            }

            JsonNode updatedNode = removeBySteps(rootNode, steps, 0);
            return OBJECT_MAPPER.writeValueAsString(updatedNode);
        } catch (Exception e) {
            log.warn("根据数组路径删除 JSON 失败, path={}: {}", path, e.getMessage());
            return json;
        }
    }

    /**
     * 根据带数组下标的路径合并 JSON。
     * 如果目标节点和源节点都是对象，则执行深度合并；否则直接覆盖。
     *
     * @param json 原 JSON 字符串
     * @param path 路径表达式
     * @param sourceJson 源 JSON 字符串
     * @return 修改后的 JSON 字符串，失败返回原 JSON
     */
    public static String mergeWithIndexPath(String json, String path, String sourceJson) {
        if (StringUtil.isBlank(path) || StringUtil.isBlank(sourceJson)) {
            return json;
        }
        try {
            List<PathStep> steps = parsePathSteps(path);
            if (steps.isEmpty()) {
                return json;
            }

            JsonNode sourceNode = OBJECT_MAPPER.readTree(sourceJson);
            if (sourceNode == null || sourceNode.isMissingNode() || sourceNode.isNull()) {
                return json;
            }

            JsonNode rootNode;
            if (StringUtil.isBlank(json)) {
                rootNode = createRootContainer(steps.get(0));
            } else {
                rootNode = OBJECT_MAPPER.readTree(json);
                if (rootNode == null || rootNode.isMissingNode() || rootNode.isNull()) {
                    rootNode = createRootContainer(steps.get(0));
                }
            }

            JsonNode updatedNode = mergeBySteps(rootNode, steps, 0, sourceNode);
            return OBJECT_MAPPER.writeValueAsString(updatedNode);
        } catch (Exception e) {
            log.warn("根据数组路径合并 JSON 失败, path={}: {}", path, e.getMessage());
            return json;
        }
    }

    /**
     * 按路径步骤写入节点。
     *
     * @param current 当前节点
     * @param steps 路径步骤
     * @param index 当前步骤索引
     * @param valueNode 待写入节点
     * @return 修改后的节点
     */
    private static JsonNode putBySteps(JsonNode current, List<PathStep> steps, int index, JsonNode valueNode) {
        PathStep step = steps.get(index);
        boolean last = index == steps.size() - 1;

        if (step.isField()) {
            ObjectNode objectNode = current != null && current.isObject() ? (ObjectNode) current : OBJECT_MAPPER.createObjectNode();
            if (last) {
                objectNode.set(step.getFieldName(), valueNode);
                return objectNode;
            }

            JsonNode childNode = objectNode.get(step.getFieldName());
            if (childNode == null || childNode.isMissingNode() || childNode.isNull() || !isContainerCompatible(childNode, steps.get(index + 1))) {
                childNode = createContainerForStep(steps.get(index + 1));
            }

            objectNode.set(step.getFieldName(), putBySteps(childNode, steps, index + 1, valueNode));
            return objectNode;
        }

        ArrayNode arrayNode = current != null && current.isArray() ? (ArrayNode) current : OBJECT_MAPPER.createArrayNode();
        ensureArraySize(arrayNode, step.getIndex());

        if (last) {
            arrayNode.set(step.getIndex(), valueNode);
            return arrayNode;
        }

        JsonNode childNode = arrayNode.get(step.getIndex());
        if (childNode == null || childNode.isMissingNode() || childNode.isNull() || !isContainerCompatible(childNode, steps.get(index + 1))) {
            childNode = createContainerForStep(steps.get(index + 1));
        }

        arrayNode.set(step.getIndex(), putBySteps(childNode, steps, index + 1, valueNode));
        return arrayNode;
    }

    /**
     * 按路径步骤删除节点。
     *
     * @param current 当前节点
     * @param steps 路径步骤
     * @param index 当前步骤索引
     * @return 修改后的节点
     */
    private static JsonNode removeBySteps(JsonNode current, List<PathStep> steps, int index) {
        PathStep step = steps.get(index);
        boolean last = index == steps.size() - 1;

        if (step.isField()) {
            if (current == null || !current.isObject()) {
                return current;
            }
            ObjectNode objectNode = (ObjectNode) current;
            if (last) {
                objectNode.remove(step.getFieldName());
                return objectNode;
            }

            JsonNode childNode = objectNode.get(step.getFieldName());
            if (childNode == null || childNode.isMissingNode() || childNode.isNull()) {
                return objectNode;
            }

            objectNode.set(step.getFieldName(), removeBySteps(childNode, steps, index + 1));
            return objectNode;
        }

        if (current == null || !current.isArray()) {
            return current;
        }
        ArrayNode arrayNode = (ArrayNode) current;
        int removeIndex = step.getIndex();
        if (removeIndex < 0 || removeIndex >= arrayNode.size()) {
            return arrayNode;
        }

        if (last) {
            arrayNode.remove(removeIndex);
            return arrayNode;
        }

        JsonNode childNode = arrayNode.get(removeIndex);
        if (childNode == null || childNode.isMissingNode() || childNode.isNull()) {
            return arrayNode;
        }

        arrayNode.set(removeIndex, removeBySteps(childNode, steps, index + 1));
        return arrayNode;
    }

    /**
     * 按路径步骤合并节点。
     *
     * @param current 当前节点
     * @param steps 路径步骤
     * @param index 当前步骤索引
     * @param sourceNode 源节点
     * @return 修改后的节点
     */
    private static JsonNode mergeBySteps(JsonNode current, List<PathStep> steps, int index, JsonNode sourceNode) {
        PathStep step = steps.get(index);
        boolean last = index == steps.size() - 1;

        if (step.isField()) {
            ObjectNode objectNode = current != null && current.isObject() ? (ObjectNode) current : OBJECT_MAPPER.createObjectNode();
            if (last) {
                JsonNode targetNode = objectNode.get(step.getFieldName());
                if (targetNode != null && targetNode.isObject() && sourceNode.isObject()) {
                    objectNode.set(step.getFieldName(), mergeNodes(targetNode, sourceNode));
                } else {
                    objectNode.set(step.getFieldName(), sourceNode);
                }
                return objectNode;
            }

            JsonNode childNode = objectNode.get(step.getFieldName());
            if (childNode == null || childNode.isMissingNode() || childNode.isNull() || !isContainerCompatible(childNode, steps.get(index + 1))) {
                childNode = createContainerForStep(steps.get(index + 1));
            }

            objectNode.set(step.getFieldName(), mergeBySteps(childNode, steps, index + 1, sourceNode));
            return objectNode;
        }

        ArrayNode arrayNode = current != null && current.isArray() ? (ArrayNode) current : OBJECT_MAPPER.createArrayNode();
        ensureArraySize(arrayNode, step.getIndex());

        if (last) {
            JsonNode targetNode = arrayNode.get(step.getIndex());
            if (targetNode != null && targetNode.isObject() && sourceNode.isObject()) {
                arrayNode.set(step.getIndex(), mergeNodes(targetNode, sourceNode));
            } else {
                arrayNode.set(step.getIndex(), sourceNode);
            }
            return arrayNode;
        }

        JsonNode childNode = arrayNode.get(step.getIndex());
        if (childNode == null || childNode.isMissingNode() || childNode.isNull() || !isContainerCompatible(childNode, steps.get(index + 1))) {
            childNode = createContainerForStep(steps.get(index + 1));
        }

        arrayNode.set(step.getIndex(), mergeBySteps(childNode, steps, index + 1, sourceNode));
        return arrayNode;
    }

    /**
     * 深度合并两个 JSON 节点。
     * 仅对对象节点做递归合并，其他类型直接使用源节点覆盖。
     *
     * @param target 目标节点
     * @param source 源节点
     * @return 合并后的节点
     */
    private static JsonNode mergeNodes(JsonNode target, JsonNode source) {
        if (target == null) {
            return source;
        }
        if (source == null) {
            return target;
        }
        if (target.isObject() && source.isObject()) {
            ObjectNode targetObject = (ObjectNode) target;
            source.fields().forEachRemaining(entry -> {
                String key = entry.getKey();
                JsonNode sourceValue = entry.getValue();
                JsonNode targetValue = targetObject.get(key);
                if (targetValue != null && targetValue.isObject() && sourceValue.isObject()) {
                    targetObject.set(key, mergeNodes(targetValue, sourceValue));
                } else {
                    targetObject.set(key, sourceValue);
                }
            });
            return targetObject;
        }
        return source;
    }

    /**
     * 解析带数组下标的路径。
     *
     * @param path 路径表达式
     * @return 路径步骤列表
     */
    private static List<PathStep> parsePathSteps(String path) {
        if (StringUtil.isBlank(path)) {
            return Collections.emptyList();
        }

        List<PathStep> steps = new java.util.ArrayList<PathStep>();
        String[] parts = path.split("\\.");
        for (String part : parts) {
            if (StringUtil.isBlank(part)) {
                continue;
            }

            int cursor = 0;
            while (cursor < part.length()) {
                char ch = part.charAt(cursor);
                if (ch == '[') {
                    int endIndex = part.indexOf(']', cursor);
                    if (endIndex < 0) {
                        return Collections.emptyList();
                    }
                    String indexText = part.substring(cursor + 1, endIndex).trim();
                    if (!StringUtil.isNumeric(indexText)) {
                        return Collections.emptyList();
                    }
                    steps.add(PathStep.ofIndex(Integer.parseInt(indexText)));
                    cursor = endIndex + 1;
                    continue;
                }

                int nextBracket = part.indexOf('[', cursor);
                String fieldName = nextBracket < 0 ? part.substring(cursor) : part.substring(cursor, nextBracket);
                fieldName = fieldName.trim();
                if (StringUtil.isNotBlank(fieldName)) {
                    steps.add(PathStep.ofField(fieldName));
                }
                cursor = nextBracket < 0 ? part.length() : nextBracket;
            }
        }
        return steps;
    }

    /**
     * 创建根容器。
     *
     * @param firstStep 首个路径步骤
     * @return 根节点容器
     */
    private static JsonNode createRootContainer(PathStep firstStep) {
        if (firstStep != null && firstStep.isIndex()) {
            return OBJECT_MAPPER.createArrayNode();
        }
        return OBJECT_MAPPER.createObjectNode();
    }

    /**
     * 根据下一步路径创建容器节点。
     *
     * @param nextStep 下一步路径
     * @return 容器节点
     */
    private static JsonNode createContainerForStep(PathStep nextStep) {
        if (nextStep != null && nextStep.isIndex()) {
            return OBJECT_MAPPER.createArrayNode();
        }
        return OBJECT_MAPPER.createObjectNode();
    }

    /**
     * 判断当前节点是否与下一步路径兼容。
     *
     * @param current 当前节点
     * @param nextStep 下一步路径
     * @return 兼容返回 true，否则返回 false
     */
    private static boolean isContainerCompatible(JsonNode current, PathStep nextStep) {
        if (current == null || current.isMissingNode() || current.isNull()) {
            return false;
        }
        if (nextStep == null) {
            return current.isObject() || current.isArray();
        }
        return nextStep.isIndex() ? current.isArray() : current.isObject();
    }

    /**
     * 确保数组长度足够。
     *
     * @param arrayNode 数组节点
     * @param index 目标下标
     */
    private static void ensureArraySize(ArrayNode arrayNode, int index) {
        while (arrayNode.size() <= index) {
            arrayNode.addNull();
        }
    }

    /**
     * 路径步骤。
     */
    private static final class PathStep {

        /**
         * 字段名。
         */
        private final String fieldName;

        /**
         * 数组下标。
         */
        private final Integer index;

        private PathStep(String fieldName, Integer index) {
            this.fieldName = fieldName;
            this.index = index;
        }

        /**
         * 创建字段步骤。
         *
         * @param fieldName 字段名
         * @return 路径步骤
         */
        private static PathStep ofField(String fieldName) {
            return new PathStep(fieldName, null);
        }

        /**
         * 创建数组下标步骤。
         *
         * @param index 数组下标
         * @return 路径步骤
         */
        private static PathStep ofIndex(Integer index) {
            return new PathStep(null, index);
        }

        /**
         * 是否为字段步骤。
         *
         * @return 是字段步骤返回 true
         */
        private boolean isField() {
            return fieldName != null;
        }

        /**
         * 是否为数组下标步骤。
         *
         * @return 是数组下标步骤返回 true
         */
        private boolean isIndex() {
            return index != null;
        }

        /**
         * 获取字段名。
         *
         * @return 字段名
         */
        private String getFieldName() {
            return fieldName;
        }

        /**
         * 获取数组下标。
         *
         * @return 数组下标
         */
        private Integer getIndex() {
            return index;
        }
    }

    /**
     * 获取 BigDecimal 值。
     *
     * @param json JSON 字符串
     * @param fieldName 字段名或路径
     * @return BigDecimal，失败返回 null
     */
    public static java.math.BigDecimal getBigDecimal(String json, String fieldName) {
        return getBigDecimalOrDefault(json, fieldName, null);
    }

    /**
     * 获取 BigDecimal 值，支持默认值。
     *
     * @param json JSON 字符串
     * @param fieldName 字段名或路径
     * @param defaultValue 默认值
     * @return BigDecimal，失败返回默认值
     */
    public static java.math.BigDecimal getBigDecimalOrDefault(String json, String fieldName, java.math.BigDecimal defaultValue) {
        JsonNode node = getNode(json, fieldName);
        if (node == null || node.isMissingNode() || node.isNull()) {
            return defaultValue;
        }
        try {
            if (node.isBigDecimal() || node.isNumber()) {
                return node.decimalValue();
            }
            String text = node.asText(null);
            if (StringUtil.isBlank(text)) {
                return defaultValue;
            }
            return new java.math.BigDecimal(text.trim());
        } catch (Exception e) {
            log.warn("获取 BigDecimal 失败, fieldName={}: {}", fieldName, e.getMessage());
            return defaultValue;
        }
    }

    /**
     * 获取 BigInteger 值。
     *
     * @param json JSON 字符串
     * @param fieldName 字段名或路径
     * @return BigInteger，失败返回 null
     */
    public static java.math.BigInteger getBigInteger(String json, String fieldName) {
        return getBigIntegerOrDefault(json, fieldName, null);
    }

    /**
     * 获取 BigInteger 值，支持默认值。
     *
     * @param json JSON 字符串
     * @param fieldName 字段名或路径
     * @param defaultValue 默认值
     * @return BigInteger，失败返回默认值
     */
    public static java.math.BigInteger getBigIntegerOrDefault(String json, String fieldName, java.math.BigInteger defaultValue) {
        JsonNode node = getNode(json, fieldName);
        if (node == null || node.isMissingNode() || node.isNull()) {
            return defaultValue;
        }
        try {
            if (node.isBigInteger() || node.isNumber()) {
                return node.bigIntegerValue();
            }
            String text = node.asText(null);
            if (StringUtil.isBlank(text)) {
                return defaultValue;
            }
            return new java.math.BigInteger(text.trim());
        } catch (Exception e) {
            log.warn("获取 BigInteger 失败, fieldName={}: {}", fieldName, e.getMessage());
            return defaultValue;
        }
    }

    /**
     * 获取 LocalDateTime 值。
     *
     * @param json JSON 字符串
     * @param fieldName 字段名或路径
     * @return LocalDateTime，失败返回 null
     */
    public static java.time.LocalDateTime getLocalDateTime(String json, String fieldName) {
        return getLocalDateTimeOrDefault(json, fieldName, null);
    }

    /**
     * 获取 LocalDateTime 值，支持默认值。
     *
     * @param json JSON 字符串
     * @param fieldName 字段名或路径
     * @param defaultValue 默认值
     * @return LocalDateTime，失败返回默认值
     */
    public static java.time.LocalDateTime getLocalDateTimeOrDefault(String json, String fieldName, java.time.LocalDateTime defaultValue) {
        String text = getTextValue(json, fieldName);
        if (StringUtil.isBlank(text)) {
            return defaultValue;
        }
        try {
            return parseLocalDateTime(text.trim());
        } catch (Exception e) {
            log.warn("获取 LocalDateTime 失败, fieldName={}: {}", fieldName, e.getMessage());
            return defaultValue;
        }
    }

    /**
     * 获取 LocalDate 值。
     *
     * @param json JSON 字符串
     * @param fieldName 字段名或路径
     * @return LocalDate，失败返回 null
     */
    public static java.time.LocalDate getLocalDate(String json, String fieldName) {
        return getLocalDateOrDefault(json, fieldName, null);
    }

    /**
     * 获取 LocalDate 值，支持默认值。
     *
     * @param json JSON 字符串
     * @param fieldName 字段名或路径
     * @param defaultValue 默认值
     * @return LocalDate，失败返回默认值
     */
    public static java.time.LocalDate getLocalDateOrDefault(String json, String fieldName, java.time.LocalDate defaultValue) {
        String text = getTextValue(json, fieldName);
        if (StringUtil.isBlank(text)) {
            return defaultValue;
        }
        try {
            return parseLocalDate(text.trim());
        } catch (Exception e) {
            log.warn("获取 LocalDate 失败, fieldName={}: {}", fieldName, e.getMessage());
            return defaultValue;
        }
    }

    /**
     * 获取 LocalTime 值。
     *
     * @param json JSON 字符串
     * @param fieldName 字段名或路径
     * @return LocalTime，失败返回 null
     */
    public static java.time.LocalTime getLocalTime(String json, String fieldName) {
        return getLocalTimeOrDefault(json, fieldName, null);
    }

    /**
     * 获取 LocalTime 值，支持默认值。
     *
     * @param json JSON 字符串
     * @param fieldName 字段名或路径
     * @param defaultValue 默认值
     * @return LocalTime，失败返回默认值
     */
    public static java.time.LocalTime getLocalTimeOrDefault(String json, String fieldName, java.time.LocalTime defaultValue) {
        String text = getTextValue(json, fieldName);
        if (StringUtil.isBlank(text)) {
            return defaultValue;
        }
        try {
            return parseLocalTime(text.trim());
        } catch (Exception e) {
            log.warn("获取 LocalTime 失败, fieldName={}: {}", fieldName, e.getMessage());
            return defaultValue;
        }
    }

    /**
     * 获取 Instant 值。
     *
     * @param json JSON 字符串
     * @param fieldName 字段名或路径
     * @return Instant，失败返回 null
     */
    public static java.time.Instant getInstant(String json, String fieldName) {
        return getInstantOrDefault(json, fieldName, null);
    }

    /**
     * 获取 Instant 值，支持默认值。
     *
     * @param json JSON 字符串
     * @param fieldName 字段名或路径
     * @param defaultValue 默认值
     * @return Instant，失败返回默认值
     */
    public static java.time.Instant getInstantOrDefault(String json, String fieldName, java.time.Instant defaultValue) {
        String text = getTextValue(json, fieldName);
        if (StringUtil.isBlank(text)) {
            return defaultValue;
        }
        try {
            return parseInstant(text.trim());
        } catch (Exception e) {
            log.warn("获取 Instant 失败, fieldName={}: {}", fieldName, e.getMessage());
            return defaultValue;
        }
    }

    /**
     * 获取 OffsetDateTime 值。
     *
     * @param json JSON 字符串
     * @param fieldName 字段名或路径
     * @return OffsetDateTime，失败返回 null
     */
    public static java.time.OffsetDateTime getOffsetDateTime(String json, String fieldName) {
        return getOffsetDateTimeOrDefault(json, fieldName, null);
    }

    /**
     * 获取 OffsetDateTime 值，支持默认值。
     *
     * @param json JSON 字符串
     * @param fieldName 字段名或路径
     * @param defaultValue 默认值
     * @return OffsetDateTime，失败返回默认值
     */
    public static java.time.OffsetDateTime getOffsetDateTimeOrDefault(String json, String fieldName, java.time.OffsetDateTime defaultValue) {
        String text = getTextValue(json, fieldName);
        if (StringUtil.isBlank(text)) {
            return defaultValue;
        }
        try {
            return parseOffsetDateTime(text.trim());
        } catch (Exception e) {
            log.warn("获取 OffsetDateTime 失败, fieldName={}: {}", fieldName, e.getMessage());
            return defaultValue;
        }
    }

    /**
     * 获取 ZonedDateTime 值。
     *
     * @param json JSON 字符串
     * @param fieldName 字段名或路径
     * @return ZonedDateTime，失败返回 null
     */
    public static java.time.ZonedDateTime getZonedDateTime(String json, String fieldName) {
        return getZonedDateTimeOrDefault(json, fieldName, null);
    }

    /**
     * 获取 ZonedDateTime 值，支持默认值。
     *
     * @param json JSON 字符串
     * @param fieldName 字段名或路径
     * @param defaultValue 默认值
     * @return ZonedDateTime，失败返回默认值
     */
    public static java.time.ZonedDateTime getZonedDateTimeOrDefault(String json, String fieldName, java.time.ZonedDateTime defaultValue) {
        String text = getTextValue(json, fieldName);
        if (StringUtil.isBlank(text)) {
            return defaultValue;
        }
        try {
            return parseZonedDateTime(text.trim());
        } catch (Exception e) {
            log.warn("获取 ZonedDateTime 失败, fieldName={}: {}", fieldName, e.getMessage());
            return defaultValue;
        }
    }

    /**
     * 获取 Date 值。
     *
     * @param json JSON 字符串
     * @param fieldName 字段名或路径
     * @return Date，失败返回 null
     */
    public static java.util.Date getDate(String json, String fieldName) {
        return getDateOrDefault(json, fieldName, null);
    }

    /**
     * 获取 Date 值，支持默认值。
     *
     * @param json JSON 字符串
     * @param fieldName 字段名或路径
     * @param defaultValue 默认值
     * @return Date，失败返回默认值
     */
    public static java.util.Date getDateOrDefault(String json, String fieldName, java.util.Date defaultValue) {
        String text = getTextValue(json, fieldName);
        if (StringUtil.isBlank(text)) {
            return defaultValue;
        }
        try {
            return parseDate(text.trim());
        } catch (Exception e) {
            log.warn("获取 Date 失败, fieldName={}: {}", fieldName, e.getMessage());
            return defaultValue;
        }
    }

    /**
     * 获取枚举值。
     *
     * @param json JSON 字符串
     * @param fieldName 字段名或路径
     * @param enumType 枚举类型
     * @param <E> 枚举类型参数
     * @return 枚举值，失败返回 null
     */
    public static <E extends Enum<E>> E getEnum(String json, String fieldName, Class<E> enumType) {
        return getEnumOrDefault(json, fieldName, enumType, null);
    }

    /**
     * 获取枚举值，支持默认值。
     *
     * @param json JSON 字符串
     * @param fieldName 字段名或路径
     * @param enumType 枚举类型
     * @param defaultValue 默认值
     * @param <E> 枚举类型参数
     * @return 枚举值，失败返回默认值
     */
    public static <E extends Enum<E>> E getEnumOrDefault(String json, String fieldName, Class<E> enumType, E defaultValue) {
        if (enumType == null) {
            return defaultValue;
        }
        String text = getTextValue(json, fieldName);
        if (StringUtil.isBlank(text)) {
            return defaultValue;
        }
        try {
            E enumValue = parseEnum(text.trim(), enumType);
            return enumValue == null ? defaultValue : enumValue;
        } catch (Exception e) {
            log.warn("获取枚举值失败, fieldName={}, enumType={}: {}", fieldName, enumType.getName(), e.getMessage());
            return defaultValue;
        }
    }

    /**
     * 从节点中提取文本值。
     *
     * @param json JSON 字符串
     * @param fieldName 字段名或路径
     * @return 文本值，失败返回 null
     */
    private static String getTextValue(String json, String fieldName) {
        JsonNode node = getNode(json, fieldName);
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        String text = node.asText(null);
        return StringUtil.isBlank(text) ? null : text;
    }

    /**
     * 解析 LocalDateTime。
     *
     * @param text 文本值
     * @return LocalDateTime
     */
    private static java.time.LocalDateTime parseLocalDateTime(String text) {
        if (text.matches("^-?\\d+$")) {
            long millis = Long.parseLong(text);
            return java.time.LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(millis), java.time.ZoneId.systemDefault());
        }
        try {
            return java.time.LocalDateTime.parse(text);
        } catch (Exception ignore) {
            java.util.Date date = parseDate(text);
            if (date != null) {
                return java.time.LocalDateTime.ofInstant(date.toInstant(), java.time.ZoneId.systemDefault());
            }
            throw ignore;
        }
    }

    /**
     * 解析 LocalDate。
     *
     * @param text 文本值
     * @return LocalDate
     */
    private static java.time.LocalDate parseLocalDate(String text) {
        if (text.matches("^-?\\d+$")) {
            long millis = Long.parseLong(text);
            return java.time.LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(millis), java.time.ZoneId.systemDefault()).toLocalDate();
        }
        try {
            return java.time.LocalDate.parse(text);
        } catch (Exception ignore) {
            java.util.Date date = parseDate(text);
            if (date != null) {
                return java.time.LocalDateTime.ofInstant(date.toInstant(), java.time.ZoneId.systemDefault()).toLocalDate();
            }
            throw ignore;
        }
    }

    /**
     * 解析 LocalTime。
     *
     * @param text 文本值
     * @return LocalTime
     */
    private static java.time.LocalTime parseLocalTime(String text) {
        if (text.matches("^-?\\d+$")) {
            long millis = Long.parseLong(text);
            return java.time.LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(millis), java.time.ZoneId.systemDefault()).toLocalTime();
        }
        try {
            return java.time.LocalTime.parse(text);
        } catch (Exception ignore) {
            java.util.Date date = parseDate(text);
            if (date != null) {
                return java.time.LocalDateTime.ofInstant(date.toInstant(), java.time.ZoneId.systemDefault()).toLocalTime();
            }
            throw ignore;
        }
    }

    /**
     * 解析 Instant。
     *
     * @param text 文本值
     * @return Instant
     */
    private static java.time.Instant parseInstant(String text) {
        if (text.matches("^-?\\d+$")) {
            return java.time.Instant.ofEpochMilli(Long.parseLong(text));
        }
        try {
            return java.time.Instant.parse(text);
        } catch (Exception ignore) {
            java.util.Date date = parseDate(text);
            if (date != null) {
                return date.toInstant();
            }
            throw ignore;
        }
    }

    /**
     * 解析 OffsetDateTime。
     *
     * @param text 文本值
     * @return OffsetDateTime
     */
    private static java.time.OffsetDateTime parseOffsetDateTime(String text) {
        if (text.matches("^-?\\d+$")) {
            return java.time.OffsetDateTime.ofInstant(java.time.Instant.ofEpochMilli(Long.parseLong(text)), java.time.ZoneId.systemDefault());
        }
        try {
            return java.time.OffsetDateTime.parse(text);
        } catch (Exception ignore) {
            java.util.Date date = parseDate(text);
            if (date != null) {
                return java.time.OffsetDateTime.ofInstant(date.toInstant(), java.time.ZoneId.systemDefault());
            }
            throw ignore;
        }
    }

    /**
     * 解析 ZonedDateTime。
     *
     * @param text 文本值
     * @return ZonedDateTime
     */
    private static java.time.ZonedDateTime parseZonedDateTime(String text) {
        if (text.matches("^-?\\d+$")) {
            return java.time.ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(Long.parseLong(text)), java.time.ZoneId.systemDefault());
        }
        try {
            return java.time.ZonedDateTime.parse(text);
        } catch (Exception ignore) {
            java.util.Date date = parseDate(text);
            if (date != null) {
                return java.time.ZonedDateTime.ofInstant(date.toInstant(), java.time.ZoneId.systemDefault());
            }
            throw ignore;
        }
    }

    /**
     * 解析 Date。
     *
     * @param text 文本值
     * @return Date
     */
    private static java.util.Date parseDate(String text) {
        if (StringUtil.isBlank(text)) {
            return null;
        }
        try {
            if (text.matches("^-?\\d+$")) {
                return new java.util.Date(Long.parseLong(text));
            }
            return DateTimeUtil.toDate(DateTimeUtil.parse(text));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 解析枚举值，支持大小写不敏感匹配和 ordinal 数值匹配。
     *
     * @param text 文本值
     * @param enumType 枚举类型
     * @param <E> 枚举类型参数
     * @return 枚举值，失败返回 null
     */
    private static <E extends Enum<E>> E parseEnum(String text, Class<E> enumType) {
        if (StringUtil.isBlank(text) || enumType == null) {
            return null;
        }

        String candidate = text.trim();
        try {
            return Enum.valueOf(enumType, candidate);
        } catch (Exception ignore) {
            E[] constants = enumType.getEnumConstants();
            if (constants == null || constants.length == 0) {
                return null;
            }

            for (E constant : constants) {
                if (StringUtil.equalsIgnoreCase(constant.name(), candidate)) {
                    return constant;
                }
            }

            if (candidate.matches("^\\d+$")) {
                int ordinal = Integer.parseInt(candidate);
                if (ordinal >= 0 && ordinal < constants.length) {
                    return constants[ordinal];
                }
            }
            return null;
        }
    }

}