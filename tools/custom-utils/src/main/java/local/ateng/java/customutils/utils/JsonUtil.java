package local.ateng.java.customutils.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import local.ateng.java.customutils.config.JacksonObjectMapperFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;

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

    /**
     * Jackson 的全局 ObjectMapper 实例
     */
    private static volatile ObjectMapper OBJECT_MAPPER = JacksonObjectMapperFactory.buildDefaultObjectMapper();

    /**
     * 禁止实例化工具类
     */
    private JsonUtil() {
        throw new UnsupportedOperationException("工具类不可实例化");
    }

    /**
     * 对象转 JSON 字符串
     * 使用示例：
     * JsonUtil.toJsonString(myUser)
     * JsonUtil.toJsonString(list)
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
            return null;
        }
    }

    /**
     * 对象转格式化（美化）后的 JSON 字符串
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
            return null;
        }
    }

    /**
     * JSON 字符串转对象
     * 使用示例：JsonUtil.parseObject(json, MyUser0.class);
     *
     * @param json  JSON 字符串
     * @param clazz 目标类
     * @param <T>   类型参数
     * @return 转换后的对象，失败时返回 null
     */
    public static <T> T parseObject(String json, Class<T> clazz) {
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
     * 使用示例：JsonUtil.parseObject(json, new TypeReference<List<MyUser0>>() {});
     *
     * @param json    JSON 字符串
     * @param typeRef 类型引用
     * @param <T>     类型参数
     * @return 转换后的对象，失败时返回 null
     */
    public static <T> T parseObject(String json, TypeReference<T> typeRef) {
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
     * 将 JSON 字符串转换为 Map 对象
     *
     * @param json JSON 字符串
     * @return 转换后的 Map，失败时返回空 Map
     */
    public static Map<String, Object> parseMap(String json) {
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
    public static <T> List<T> parseList(String json, Class<T> elementType) {
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
     * 将对象进行深拷贝（基于 JSON 序列化与反序列化）
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
        return parseObject(toJsonString(obj), clazz);
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
    public static String get(String json, String fieldName) {
        return get(json, fieldName, String.class);
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
    public static <T> T get(String json, String fieldName, Class<T> clazz) {
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
    public static <T> T get(String json, String fieldName, TypeReference<T> typeRef) {
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
    public static <T> T convert(Object fromValue, Class<T> toValueType) {
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
    public static <T> T convert(Object fromValue, TypeReference<T> toValueTypeRef) {
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
    public static String put(String json, String path, Object newValue) {
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
    public static String remove(String json, String path) {
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
                    // 路径不存在，直接返回原 json
                    return json;
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
