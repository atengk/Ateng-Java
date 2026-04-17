package local.ateng.java.customutils.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.alibaba.fastjson.parser.Feature;
import com.alibaba.fastjson.serializer.SerializerFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * JSON 工具类。
 * 提供常用的 JSON 序列化、反序列化、对象转换、路径读写、合并与扁平化能力。
 * 基于 Fastjson1 实现。
 *
 * @author Ateng
 * @since 2026-04-17
 */
public final class FastJsonUtil {

    private static final Logger log = LoggerFactory.getLogger(FastJsonUtil.class);

    private FastJsonUtil() {
        throw new UnsupportedOperationException("工具类不可实例化");
    }

    /**
     * Fastjson1 常用序列化特性，便于统一引用。
     */
    public static final SerializerFeature WRITER_QUOTE_FIELD_NAMES = SerializerFeature.QuoteFieldNames;
    public static final SerializerFeature WRITER_WRITE_MAP_NULL_VALUE = SerializerFeature.WriteMapNullValue;
    public static final SerializerFeature WRITER_WRITE_NULL_LIST_AS_EMPTY = SerializerFeature.WriteNullListAsEmpty;
    public static final SerializerFeature WRITER_WRITE_NULL_STRING_AS_EMPTY = SerializerFeature.WriteNullStringAsEmpty;
    public static final SerializerFeature WRITER_WRITE_NULL_NUMBER_AS_ZERO = SerializerFeature.WriteNullNumberAsZero;
    public static final SerializerFeature WRITER_WRITE_NULL_BOOLEAN_AS_FALSE = SerializerFeature.WriteNullBooleanAsFalse;
    public static final SerializerFeature WRITER_SKIP_TRANSIENT_FIELD = SerializerFeature.SkipTransientField;
    public static final SerializerFeature WRITER_SORT_FIELD = SerializerFeature.SortField;
    public static final SerializerFeature WRITER_WRITE_TAB_AS_SPECIAL = SerializerFeature.WriteTabAsSpecial;
    public static final SerializerFeature WRITER_PRETTY_FORMAT = SerializerFeature.PrettyFormat;
    public static final SerializerFeature WRITER_WRITE_CLASS_NAME = SerializerFeature.WriteClassName;
    public static final SerializerFeature WRITER_DISABLE_CIRCULAR_REFERENCE_DETECT = SerializerFeature.DisableCircularReferenceDetect;
    public static final SerializerFeature WRITER_WRITE_SLASH_AS_SPECIAL = SerializerFeature.WriteSlashAsSpecial;
    public static final SerializerFeature WRITER_BROWSER_COMPATIBLE = SerializerFeature.BrowserCompatible;
    public static final SerializerFeature WRITER_USE_SINGLE_QUOTES = SerializerFeature.UseSingleQuotes;
    public static final SerializerFeature WRITER_MAP_SORT_FIELD = SerializerFeature.MapSortField;
    public static final SerializerFeature WRITER_WRITE_ENUM_USING_TO_STRING = SerializerFeature.WriteEnumUsingToString;
    public static final SerializerFeature WRITER_WRITE_ENUM_USING_NAME = SerializerFeature.WriteEnumUsingName;
    public static final SerializerFeature WRITER_NOT_WRITE_ROOT_CLASS_NAME = SerializerFeature.NotWriteRootClassName;
    public static final SerializerFeature WRITER_WRITE_BIG_DECIMAL_AS_PLAIN = SerializerFeature.WriteBigDecimalAsPlain;
    public static final SerializerFeature WRITER_WRITE_NON_STRING_KEY_AS_STRING = SerializerFeature.WriteNonStringKeyAsString;
    public static final SerializerFeature WRITER_WRITE_NON_STRING_VALUE_AS_STRING = SerializerFeature.WriteNonStringValueAsString;
    public static final SerializerFeature WRITER_BEAN_TO_ARRAY = SerializerFeature.BeanToArray;
    public static final SerializerFeature WRITER_WRITE_DATE_USE_DATE_FORMAT = SerializerFeature.WriteDateUseDateFormat;
    public static final SerializerFeature WRITER_NOT_WRITE_DEFAULT_VALUE = SerializerFeature.NotWriteDefaultValue;
    public static final SerializerFeature WRITER_IGNORE_ERROR_GETTER = SerializerFeature.IgnoreErrorGetter;

    /**
     * Fastjson1 常用反序列化特性，便于统一引用。
     */
    public static final Feature READER_AUTO_CLOSE_SOURCE = Feature.AutoCloseSource;
    public static final Feature READER_ALLOW_COMMENT = Feature.AllowComment;
    public static final Feature READER_ALLOW_UN_QUOTED_FIELD_NAMES = Feature.AllowUnQuotedFieldNames;
    public static final Feature READER_ALLOW_SINGLE_QUOTES = Feature.AllowSingleQuotes;
    public static final Feature READER_ALLOW_ARBITRARY_COMMAS = Feature.AllowArbitraryCommas;
    public static final Feature READER_ALLOW_ISO8601_DATE_FORMAT = Feature.AllowISO8601DateFormat;
    public static final Feature READER_INIT_STRING_FIELD_AS_EMPTY = Feature.InitStringFieldAsEmpty;
    public static final Feature READER_SUPPORT_ARRAY_TO_BEAN = Feature.SupportArrayToBean;
    public static final Feature READER_IGNORE_NOT_MATCH = Feature.IgnoreNotMatch;
    public static final Feature READER_SORT_FEID_FAST_MATCH = Feature.SortFeidFastMatch;
    public static final Feature READER_DISABLE_ASM = Feature.DisableASM;
    public static final Feature READER_DISABLE_CIRCULAR_REFERENCE_DETECT = Feature.DisableCircularReferenceDetect;
    public static final Feature READER_USE_BIG_DECIMAL = Feature.UseBigDecimal;
    public static final Feature READER_ORDERED_FIELD = Feature.OrderedField;
    public static final Feature READER_DISABLE_SPECIAL_KEY_DETECT = Feature.DisableSpecialKeyDetect;

    /**
     * Fastjson1 默认序列化特性。
     * 适用于大多数业务场景，兼顾前端兼容性和字段完整性。
     */
    private static final SerializerFeature[] DEFAULT_JSON_WRITER_FEATURES = new SerializerFeature[]{
            // 输出为 null 的字段，否则默认会被忽略
            SerializerFeature.WriteMapNullValue,
            // 禁用循环引用检测，避免出现 "$ref" 结构
            SerializerFeature.DisableCircularReferenceDetect,
            // BigDecimal 输出为纯字符串（不使用科学计数法）
            SerializerFeature.WriteBigDecimalAsPlain,
    };

    /**
     * Fastjson1 默认反序列化特性。
     * 适用于大多数业务场景，兼顾字段匹配和数值解析稳定性。
     */
    private static final Feature[] DEFAULT_JSON_READER_FEATURES = new Feature[]{
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
    };

    /**
     * 对象转 JSON 字符串（默认配置）。
     *
     * @param obj 待序列化对象
     * @return JSON 字符串，失败返回 null
     */
    public static String toJsonStringWithDefault(Object obj) {
        return toJsonString(obj, DEFAULT_JSON_WRITER_FEATURES);
    }

    /**
     * JSON 字符串转对象（默认配置）。
     *
     * @param json  JSON 字符串
     * @param clazz 目标类型
     * @param <T>   类型参数
     * @return 目标对象，失败返回 null
     */
    public static <T> T parseObjectWithDefault(String json, Class<T> clazz) {
        return parseObject(json, clazz, DEFAULT_JSON_READER_FEATURES);
    }

    /**
     * 对象转 JSON 字符串。
     *
     * @param obj 源对象
     * @return JSON 字符串，失败返回 null
     */
    public static String toJsonString(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return JSON.toJSONString(obj);
        } catch (Exception e) {
            log.warn("对象转 JSON 失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 对象转 JSON 字符串，并支持额外写入特性。
     *
     * @param obj      源对象
     * @param features 写入特性
     * @return JSON 字符串，失败返回 null
     */
    public static String toJsonString(Object obj, SerializerFeature... features) {
        if (obj == null) {
            return null;
        }
        try {
            return JSON.toJSONString(obj, features);
        } catch (Exception e) {
            log.warn("对象转 JSON 失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 对象转格式化 JSON 字符串。
     *
     * @param obj 源对象
     * @return 格式化 JSON 字符串，失败返回 null
     */
    public static String toPrettyJsonString(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return JSON.toJSONString(obj, SerializerFeature.PrettyFormat);
        } catch (Exception e) {
            log.warn("对象转 Pretty JSON 失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 对象转 JSON 字节数组。
     *
     * @param obj 源对象
     * @return JSON 字节数组，失败返回 null
     */
    public static byte[] toJsonBytes(Object obj) {
        String json = toJsonString(obj);
        return json == null ? null : json.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * 对象转 JSON 字节数组。
     *
     * @param obj      源对象
     * @param features 写入特性
     * @return JSON 字节数组，失败返回 null
     */
    public static byte[] toJsonBytes(Object obj, SerializerFeature... features) {
        String json = toJsonString(obj, features);
        return json == null ? null : json.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * JSON 字符串转对象。
     *
     * @param json  JSON 字符串
     * @param clazz 目标类型
     * @param <T>   类型参数
     * @return 目标对象，失败返回 null
     */
    public static <T> T parseObject(String json, Class<T> clazz) {
        if (isBlank(json) || clazz == null) {
            return null;
        }
        try {
            return JSON.parseObject(json, clazz);
        } catch (Exception e) {
            log.warn("JSON 转对象失败, class={}: {}", clazz.getName(), e.getMessage());
            return null;
        }
    }

    /**
     * JSON 字符串转对象。
     *
     * @param json     JSON 字符串
     * @param clazz    目标类型
     * @param features 读取特性
     * @param <T>      类型参数
     * @return 目标对象，失败返回 null
     */
    public static <T> T parseObject(String json, Class<T> clazz, Feature... features) {
        return parseObject(json, clazz);
    }

    /**
     * JSON 字节数组转对象。
     *
     * @param bytes JSON 字节数组
     * @param clazz 目标类型
     * @param <T>   类型参数
     * @return 目标对象，失败返回 null
     */
    public static <T> T parseObject(byte[] bytes, Class<T> clazz) {
        if (bytes == null || bytes.length == 0 || clazz == null) {
            return null;
        }
        return parseObject(new String(bytes, StandardCharsets.UTF_8), clazz);
    }

    /**
     * JSON 字节数组转对象。
     *
     * @param bytes    JSON 字节数组
     * @param clazz    目标类型
     * @param features 读取特性
     * @param <T>      类型参数
     * @return 目标对象，失败返回 null
     */
    public static <T> T parseObject(byte[] bytes, Class<T> clazz, Feature... features) {
        return parseObject(bytes, clazz);
    }

    /**
     * JSON 字符串转复杂类型。
     *
     * @param json    JSON 字符串
     * @param typeRef 类型引用
     * @param <T>     类型参数
     * @return 目标对象，失败返回 null
     */
    public static <T> T parseObject(String json, TypeReference<T> typeRef) {
        if (isBlank(json) || typeRef == null) {
            return null;
        }
        try {
            return JSON.parseObject(json, typeRef);
        } catch (Exception e) {
            log.warn("JSON 转复杂对象失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * JSON 字符串转复杂类型。
     *
     * @param json     JSON 字符串
     * @param typeRef  类型引用
     * @param features 读取特性
     * @param <T>      类型参数
     * @return 目标对象，失败返回 null
     */
    public static <T> T parseObject(String json, TypeReference<T> typeRef, Feature... features) {
        return parseObject(json, typeRef);
    }

    /**
     * JSON 字节数组转复杂类型。
     *
     * @param bytes   JSON 字节数组
     * @param typeRef 类型引用
     * @param <T>     类型参数
     * @return 目标对象，失败返回 null
     */
    public static <T> T parseObject(byte[] bytes, TypeReference<T> typeRef) {
        if (bytes == null || bytes.length == 0 || typeRef == null) {
            return null;
        }
        return parseObject(new String(bytes, StandardCharsets.UTF_8), typeRef);
    }

    /**
     * JSON 字节数组转复杂类型。
     *
     * @param bytes    JSON 字节数组
     * @param typeRef  类型引用
     * @param features 读取特性
     * @param <T>      类型参数
     * @return 目标对象，失败返回 null
     */
    public static <T> T parseObject(byte[] bytes, TypeReference<T> typeRef, Feature... features) {
        return parseObject(bytes, typeRef);
    }

    /**
     * JSON 字符串转指定 Type。
     *
     * @param json JSON 字符串
     * @param type 目标类型
     * @param <T>  类型参数
     * @return 目标对象，失败返回 null
     */
    public static <T> T parseObject(String json, Type type) {
        if (isBlank(json) || type == null) {
            return null;
        }
        try {
            return JSON.parseObject(json, type);
        } catch (Exception e) {
            log.warn("JSON 转 Type 对象失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * JSON 字符串转指定 Type。
     *
     * @param json     JSON 字符串
     * @param type     目标类型
     * @param features 读取特性
     * @param <T>      类型参数
     * @return 目标对象，失败返回 null
     */
    public static <T> T parseObject(String json, Type type, Feature... features) {
        return parseObject(json, type);
    }

    /**
     * JSON 字符串转 List。
     *
     * @param json        JSON 字符串
     * @param elementType 元素类型
     * @param <T>         类型参数
     * @return List，失败返回空列表
     */
    public static <T> List<T> parseList(String json, Class<T> elementType) {
        if (isBlank(json) || elementType == null) {
            return Collections.emptyList();
        }
        try {
            Object root = parse(json);
            List<Object> list = toObjectList(root);
            if (list.isEmpty()) {
                return Collections.emptyList();
            }
            List<T> result = new ArrayList<T>(list.size());
            for (Object item : list) {
                T value = convert(item, elementType);
                if (value != null) {
                    result.add(value);
                }
            }
            return result;
        } catch (Exception e) {
            log.warn("JSON 转 List 失败, elementType={}: {}", elementType.getName(), e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * JSON 字符串转 List。
     *
     * @param json        JSON 字符串
     * @param elementType 元素类型
     * @param features    读取特性
     * @param <T>         类型参数
     * @return List，失败返回空列表
     */
    public static <T> List<T> parseList(String json, Class<T> elementType, Feature... features) {
        return parseList(json, elementType);
    }

    /**
     * JSON 字符串转 Set。
     *
     * @param json        JSON 字符串
     * @param elementType 元素类型
     * @param <T>         类型参数
     * @return Set，失败返回空集合
     */
    public static <T> Set<T> parseSet(String json, Class<T> elementType) {
        if (isBlank(json) || elementType == null) {
            return Collections.emptySet();
        }
        try {
            List<T> list = parseList(json, elementType);
            if (list.isEmpty()) {
                return Collections.emptySet();
            }
            return new LinkedHashSet<T>(list);
        } catch (Exception e) {
            log.warn("JSON 转 Set 失败, elementType={}: {}", elementType.getName(), e.getMessage());
            return Collections.emptySet();
        }
    }

    /**
     * JSON 字符串转 Set。
     *
     * @param json        JSON 字符串
     * @param elementType 元素类型
     * @param features    读取特性
     * @param <T>         类型参数
     * @return Set，失败返回空集合
     */
    public static <T> Set<T> parseSet(String json, Class<T> elementType, Feature... features) {
        return parseSet(json, elementType);
    }

    /**
     * 将 JSON 字符串转为 Map<String, Object>。
     *
     * @param json JSON 字符串
     * @return Map，失败返回空 Map
     */
    public static Map<String, Object> parseMap(String json) {
        if (isBlank(json)) {
            return Collections.emptyMap();
        }
        try {
            return JSON.parseObject(json, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            log.warn("JSON 转 Map<String, Object> 失败: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    /**
     * 将 JSON 字符串转为 Map<String, Object>。
     *
     * @param json     JSON 字符串
     * @param features 读取特性
     * @return Map，失败返回空 Map
     */
    public static Map<String, Object> parseMap(String json, Feature... features) {
        return parseMap(json);
    }

    /**
     * 将 JSON 字符串转为指定键值类型的 Map。
     *
     * @param json      JSON 字符串
     * @param keyType   键类型
     * @param valueType 值类型
     * @param <K>       键类型参数
     * @param <V>       值类型参数
     * @return Map，失败返回空 Map
     */
    public static <K, V> Map<K, V> parseMap(String json, Class<K> keyType, Class<V> valueType) {
        if (isBlank(json) || keyType == null || valueType == null) {
            return Collections.emptyMap();
        }
        try {
            return JSON.parseObject(json, new TypeReference<Map<K, V>>() {
            });
        } catch (Exception e) {
            log.warn("JSON 转指定类型 Map 失败: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    /**
     * 将 JSON 字符串转为指定键值类型的 Map。
     *
     * @param json      JSON 字符串
     * @param keyType   键类型
     * @param valueType 值类型
     * @param features  读取特性
     * @param <K>       键类型参数
     * @param <V>       值类型参数
     * @return Map，失败返回空 Map
     */
    public static <K, V> Map<K, V> parseMap(String json, Class<K> keyType, Class<V> valueType, Feature... features) {
        return parseMap(json, keyType, valueType);
    }

    /**
     * 判断字符串是否为合法 JSON。
     *
     * @param json 待判断字符串
     * @return true 表示合法 JSON
     */
    public static boolean isJson(String json) {
        if (isBlank(json)) {
            return false;
        }
        try {
            JSON.parse(json);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 判断字符串是否为 JSON 对象。
     *
     * @param json 待判断字符串
     * @return true 表示 JSON 对象
     */
    public static boolean isObjectJson(String json) {
        Object root = parse(json);
        return root instanceof JSONObject;
    }

    /**
     * 判断字符串是否为 JSON 数组。
     *
     * @param json 待判断字符串
     * @return true 表示 JSON 数组
     */
    public static boolean isArrayJson(String json) {
        Object root = parse(json);
        return root instanceof JSONArray;
    }

    /**
     * 判断 JSON 是否为空对象或空数组。
     *
     * @param json JSON 字符串
     * @return true 表示空 JSON
     */
    public static boolean isEmptyJson(String json) {
        if (isBlank(json)) {
            return true;
        }
        Object root = parse(json);
        if (root == null) {
            return true;
        }
        if (root instanceof JSONObject) {
            return ((JSONObject) root).isEmpty();
        }
        if (root instanceof JSONArray) {
            return ((JSONArray) root).isEmpty();
        }
        if (root instanceof Collection) {
            return ((Collection<?>) root).isEmpty();
        }
        if (root.getClass().isArray()) {
            return Array.getLength(root) == 0;
        }
        return false;
    }

    /**
     * 读取 JSON 为通用对象。
     *
     * @param json JSON 字符串
     * @return 对象节点，失败返回 null
     */
    public static Object parse(String json) {
        if (isBlank(json)) {
            return null;
        }
        try {
            return JSON.parse(json);
        } catch (Exception e) {
            log.warn("解析 JSON 失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 读取 JSON 为通用对象，并支持额外读取特性。
     *
     * @param json     JSON 字符串
     * @param features 读取特性
     * @return 对象节点，失败返回 null
     */
    public static Object parse(String json, Feature... features) {
        return parse(json);
    }

    /**
     * 转换为 JSONObject。
     *
     * @param json JSON 字符串
     * @return JSONObject，失败返回 null
     */
    public static JSONObject parseObjectNode(String json) {
        Object root = parse(json);
        return root instanceof JSONObject ? (JSONObject) root : null;
    }

    /**
     * 转换为 JSONObject。
     *
     * @param json     JSON 字符串
     * @param features 读取特性
     * @return JSONObject，失败返回 null
     */
    public static JSONObject parseObjectNode(String json, Feature... features) {
        return parseObjectNode(json);
    }

    /**
     * 转换为 JSONArray。
     *
     * @param json JSON 字符串
     * @return JSONArray，失败返回 null
     */
    public static JSONArray parseArrayNode(String json) {
        Object root = parse(json);
        return root instanceof JSONArray ? (JSONArray) root : null;
    }

    /**
     * 转换为 JSONArray。
     *
     * @param json     JSON 字符串
     * @param features 读取特性
     * @return JSONArray，失败返回 null
     */
    public static JSONArray parseArrayNode(String json, Feature... features) {
        return parseArrayNode(json);
    }

    /**
     * 深拷贝对象。
     *
     * @param obj   原对象
     * @param clazz 目标类型
     * @param <T>   类型参数
     * @return 拷贝结果，失败返回 null
     */
    public static <T> T copy(Object obj, Class<T> clazz) {
        if (obj == null || clazz == null) {
            return null;
        }
        return convert(obj, clazz);
    }

    /**
     * 深拷贝对象，支持复杂类型。
     *
     * @param obj     原对象
     * @param typeRef 目标类型引用
     * @param <T>     类型参数
     * @return 拷贝结果，失败返回 null
     */
    public static <T> T copy(Object obj, TypeReference<T> typeRef) {
        if (obj == null || typeRef == null) {
            return null;
        }
        return convert(obj, typeRef);
    }

    /**
     * 类型转换。
     *
     * @param fromValue 源对象
     * @param clazz     目标类型
     * @param <T>       类型参数
     * @return 转换结果，失败返回 null
     */
    public static <T> T convert(Object fromValue, Class<T> clazz) {
        if (fromValue == null || clazz == null) {
            return null;
        }
        try {
            return JSON.parseObject(JSON.toJSONString(fromValue), clazz);
        } catch (Exception e) {
            log.warn("对象类型转换失败, targetClass={}: {}", clazz.getName(), e.getMessage());
            return null;
        }
    }

    /**
     * 类型转换，支持复杂类型。
     *
     * @param fromValue 源对象
     * @param typeRef   目标类型引用
     * @param <T>       类型参数
     * @return 转换结果，失败返回 null
     */
    public static <T> T convert(Object fromValue, TypeReference<T> typeRef) {
        if (fromValue == null || typeRef == null) {
            return null;
        }
        try {
            return JSON.parseObject(JSON.toJSONString(fromValue), typeRef);
        } catch (Exception e) {
            log.warn("对象复杂类型转换失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 类型转换，支持 Type。
     *
     * @param fromValue 源对象
     * @param type      目标类型
     * @param <T>       类型参数
     * @return 转换结果，失败返回 null
     */
    public static <T> T convert(Object fromValue, Type type) {
        if (fromValue == null || type == null) {
            return null;
        }
        try {
            return JSON.parseObject(JSON.toJSONString(fromValue), type);
        } catch (Exception e) {
            log.warn("对象 Type 转换失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 获取指定路径的节点值。
     *
     * @param json JSON 字符串
     * @param path 点路径，支持 user.name、items[0].id、$.user.name
     * @return 节点值，失败返回 null
     */
    public static Object getNode(String json, String path) {
        Object root = parse(json);
        return getNode(root, path);
    }

    /**
     * 获取指定路径的节点值。
     *
     * @param root 根对象
     * @param path 点路径，支持 user.name、items[0].id、$.user.name
     * @return 节点值，失败返回 null
     */
    public static Object getNode(Object root, String path) {
        if (root == null || isBlank(path)) {
            return null;
        }
        List<PathStep> steps = parsePathSteps(path);
        if (steps.isEmpty()) {
            return null;
        }
        Object current = root;
        for (PathStep step : steps) {
            if (current == null) {
                return null;
            }
            if (step.isField()) {
                current = getFieldValue(current, step.getFieldName());
            } else {
                current = getIndexValue(current, step.getIndex());
            }
        }
        return current;
    }

    /**
     * 获取指定路径的字符串值。
     *
     * @param json JSON 字符串
     * @param path 路径
     * @return 字符串值，失败返回 null
     */
    public static String getString(String json, String path) {
        Object value = getNode(json, path);
        return value == null ? null : String.valueOf(value);
    }

    /**
     * 获取指定路径的字符串值，支持默认值。
     *
     * @param json         JSON 字符串
     * @param path         路径
     * @param defaultValue 默认值
     * @return 字符串值
     */
    public static String getStringOrDefault(String json, String path, String defaultValue) {
        String value = getString(json, path);
        return isBlank(value) ? defaultValue : value;
    }

    /**
     * 获取指定路径的整型值。
     *
     * @param json JSON 字符串
     * @param path 路径
     * @return Integer，失败返回 null
     */
    public static Integer getInteger(String json, String path) {
        Object value = getNode(json, path);
        if (value == null) {
            return null;
        }
        try {
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
            String text = String.valueOf(value).trim();
            if (isBlank(text)) {
                return null;
            }
            return Integer.valueOf(text);
        } catch (Exception e) {
            log.warn("获取 Integer 失败, path={}: {}", path, e.getMessage());
            return null;
        }
    }

    /**
     * 获取指定路径的整型值，支持默认值。
     *
     * @param json         JSON 字符串
     * @param path         路径
     * @param defaultValue 默认值
     * @return Integer
     */
    public static Integer getIntegerOrDefault(String json, String path, Integer defaultValue) {
        Integer value = getInteger(json, path);
        return value == null ? defaultValue : value;
    }

    /**
     * 获取指定路径的长整型值。
     *
     * @param json JSON 字符串
     * @param path 路径
     * @return Long，失败返回 null
     */
    public static Long getLong(String json, String path) {
        Object value = getNode(json, path);
        if (value == null) {
            return null;
        }
        try {
            if (value instanceof Number) {
                return ((Number) value).longValue();
            }
            String text = String.valueOf(value).trim();
            if (isBlank(text)) {
                return null;
            }
            return Long.valueOf(text);
        } catch (Exception e) {
            log.warn("获取 Long 失败, path={}: {}", path, e.getMessage());
            return null;
        }
    }

    /**
     * 获取指定路径的长整型值，支持默认值。
     *
     * @param json         JSON 字符串
     * @param path         路径
     * @param defaultValue 默认值
     * @return Long
     */
    public static Long getLongOrDefault(String json, String path, Long defaultValue) {
        Long value = getLong(json, path);
        return value == null ? defaultValue : value;
    }

    /**
     * 获取指定路径的布尔值。
     *
     * @param json JSON 字符串
     * @param path 路径
     * @return Boolean，失败返回 null
     */
    public static Boolean getBoolean(String json, String path) {
        Object value = getNode(json, path);
        if (value == null) {
            return null;
        }
        try {
            if (value instanceof Boolean) {
                return (Boolean) value;
            }
            String text = String.valueOf(value).trim();
            if (isBlank(text)) {
                return null;
            }
            return Boolean.valueOf(text);
        } catch (Exception e) {
            log.warn("获取 Boolean 失败, path={}: {}", path, e.getMessage());
            return null;
        }
    }

    /**
     * 获取指定路径的布尔值，支持默认值。
     *
     * @param json         JSON 字符串
     * @param path         路径
     * @param defaultValue 默认值
     * @return Boolean
     */
    public static Boolean getBooleanOrDefault(String json, String path, Boolean defaultValue) {
        Boolean value = getBoolean(json, path);
        return value == null ? defaultValue : value;
    }

    /**
     * 获取指定路径的双精度值。
     *
     * @param json JSON 字符串
     * @param path 路径
     * @return Double，失败返回 null
     */
    public static Double getDouble(String json, String path) {
        Object value = getNode(json, path);
        if (value == null) {
            return null;
        }
        try {
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }
            String text = String.valueOf(value).trim();
            if (isBlank(text)) {
                return null;
            }
            return Double.valueOf(text);
        } catch (Exception e) {
            log.warn("获取 Double 失败, path={}: {}", path, e.getMessage());
            return null;
        }
    }

    /**
     * 获取指定路径的双精度值，支持默认值。
     *
     * @param json         JSON 字符串
     * @param path         路径
     * @param defaultValue 默认值
     * @return Double
     */
    public static Double getDoubleOrDefault(String json, String path, Double defaultValue) {
        Double value = getDouble(json, path);
        return value == null ? defaultValue : value;
    }

    /**
     * 获取指定路径的 BigDecimal 值。
     *
     * @param json JSON 字符串
     * @param path 路径
     * @return BigDecimal，失败返回 null
     */
    public static BigDecimal getBigDecimal(String json, String path) {
        Object value = getNode(json, path);
        if (value == null) {
            return null;
        }
        try {
            if (value instanceof BigDecimal) {
                return (BigDecimal) value;
            }
            if (value instanceof Number) {
                return new BigDecimal(String.valueOf(value));
            }
            String text = String.valueOf(value).trim();
            if (isBlank(text)) {
                return null;
            }
            return new BigDecimal(text);
        } catch (Exception e) {
            log.warn("获取 BigDecimal 失败, path={}: {}", path, e.getMessage());
            return null;
        }
    }

    /**
     * 获取指定路径的 BigDecimal 值，支持默认值。
     *
     * @param json         JSON 字符串
     * @param path         路径
     * @param defaultValue 默认值
     * @return BigDecimal
     */
    public static BigDecimal getBigDecimalOrDefault(String json, String path, BigDecimal defaultValue) {
        BigDecimal value = getBigDecimal(json, path);
        return value == null ? defaultValue : value;
    }

    /**
     * 获取指定路径的 Date 值。
     *
     * @param json JSON 字符串
     * @param path 路径
     * @return Date，失败返回 null
     */
    public static Date getDate(String json, String path) {
        Object value = getNode(json, path);
        if (value == null) {
            return null;
        }
        try {
            if (value instanceof Date) {
                return (Date) value;
            }
            if (value instanceof Number) {
                return new Date(((Number) value).longValue());
            }
            return JSON.parseObject(JSON.toJSONString(value), Date.class);
        } catch (Exception e) {
            log.warn("获取 Date 失败, path={}: {}", path, e.getMessage());
            return null;
        }
    }

    /**
     * 获取指定路径的 Date 值，支持默认值。
     *
     * @param json         JSON 字符串
     * @param path         路径
     * @param defaultValue 默认值
     * @return Date
     */
    public static Date getDateOrDefault(String json, String path, Date defaultValue) {
        Date value = getDate(json, path);
        return value == null ? defaultValue : value;
    }

    /**
     * 判断指定路径是否存在。
     *
     * @param json JSON 字符串
     * @param path 路径
     * @return true 表示存在
     */
    public static boolean has(String json, String path) {
        return getNode(json, path) != null;
    }

    /**
     * 判断指定路径是否为空。
     *
     * @param json JSON 字符串
     * @param path 路径
     * @return true 表示为空
     */
    public static boolean isEmptyAt(String json, String path) {
        Object value = getNode(json, path);
        if (value == null) {
            return true;
        }
        if (value instanceof String) {
            return isBlank((String) value);
        }
        if (value instanceof JSONObject) {
            return ((JSONObject) value).isEmpty();
        }
        if (value instanceof JSONArray) {
            return ((JSONArray) value).isEmpty();
        }
        if (value instanceof Collection) {
            return ((Collection<?>) value).isEmpty();
        }
        if (value.getClass().isArray()) {
            return Array.getLength(value) == 0;
        }
        return false;
    }

    /**
     * 对 JSON 中指定路径赋值，支持 user.name、items[0].id。
     * 中间节点不存在时自动创建对象或数组。
     *
     * @param json     原 JSON
     * @param path     路径
     * @param newValue 新值
     * @return 修改后的 JSON，失败返回原 JSON
     */
    public static String put(String json, String path, Object newValue) {
        if (isBlank(path)) {
            return json;
        }
        try {
            List<PathStep> steps = parsePathSteps(path);
            if (steps.isEmpty()) {
                return json;
            }

            Object root = parse(json);
            if (root == null) {
                root = createRootContainer(steps.get(0));
            } else if (!(root instanceof JSONObject) && !(root instanceof JSONArray)) {
                return json;
            }

            Object updated = putBySteps(root, steps, 0, newValue);
            return JSON.toJSONString(updated);
        } catch (Exception e) {
            log.warn("JSON 路径赋值失败, path={}: {}", path, e.getMessage());
            return json;
        }
    }

    /**
     * 删除 JSON 中指定路径的值，支持 user.name、items[0].id。
     *
     * @param json 原 JSON
     * @param path 路径
     * @return 修改后的 JSON，失败返回原 JSON
     */
    public static String remove(String json, String path) {
        if (isBlank(json) || isBlank(path)) {
            return json;
        }
        try {
            Object root = parse(json);
            if (!(root instanceof JSONObject) && !(root instanceof JSONArray)) {
                return json;
            }

            List<PathStep> steps = parsePathSteps(path);
            if (steps.isEmpty()) {
                return json;
            }

            Object updated = removeBySteps(root, steps, 0);
            return JSON.toJSONString(updated);
        } catch (Exception e) {
            log.warn("JSON 路径删除失败, path={}: {}", path, e.getMessage());
            return json;
        }
    }

    /**
     * 合并两个 JSON 字符串，后者覆盖前者同名字段。
     *
     * @param targetJson 目标 JSON
     * @param sourceJson 源 JSON
     * @return 合并后的 JSON，失败返回目标 JSON
     */
    public static String merge(String targetJson, String sourceJson) {
        if (isBlank(targetJson)) {
            return sourceJson;
        }
        if (isBlank(sourceJson)) {
            return targetJson;
        }
        try {
            Object target = parse(targetJson);
            Object source = parse(sourceJson);
            if (!(target instanceof JSONObject) || !(source instanceof JSONObject)) {
                return targetJson;
            }
            JSONObject merged = (JSONObject) target;
            merged.putAll((JSONObject) source);
            return JSON.toJSONString(merged);
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
        if (isBlank(targetJson) || sourceMap == null || sourceMap.isEmpty()) {
            return targetJson;
        }
        try {
            Object target = parse(targetJson);
            if (!(target instanceof JSONObject)) {
                return targetJson;
            }
            JSONObject merged = (JSONObject) target;
            merged.putAll(sourceMap);
            return JSON.toJSONString(merged);
        } catch (Exception e) {
            log.warn("Map 合并 JSON 失败: {}", e.getMessage());
            return targetJson;
        }
    }

    /**
     * 递归深度合并两个 JSON 字符串。
     *
     * @param targetJson 目标 JSON
     * @param sourceJson 源 JSON
     * @return 合并后的 JSON，失败返回目标 JSON
     */
    public static String deepMerge(String targetJson, String sourceJson) {
        if (isBlank(targetJson)) {
            return sourceJson;
        }
        if (isBlank(sourceJson)) {
            return targetJson;
        }
        try {
            Object target = parse(targetJson);
            Object source = parse(sourceJson);
            Object merged = deepMergeNode(target, source);
            return JSON.toJSONString(merged);
        } catch (Exception e) {
            log.warn("JSON 深度合并失败: {}", e.getMessage());
            return targetJson;
        }
    }

    /**
     * 标准化 JSON 为紧凑格式。
     *
     * @param json JSON 字符串
     * @return 紧凑 JSON，失败返回原字符串
     */
    public static String normalize(String json) {
        Object root = parse(json);
        if (root == null) {
            return json;
        }
        try {
            return JSON.toJSONString(root);
        } catch (Exception e) {
            log.warn("JSON 标准化失败: {}", e.getMessage());
            return json;
        }
    }

    /**
     * 美化 JSON。
     *
     * @param json JSON 字符串
     * @return 美化后的 JSON，失败返回原字符串
     */
    public static String pretty(String json) {
        Object root = parse(json);
        if (root == null) {
            return json;
        }
        try {
            return JSON.toJSONString(root, SerializerFeature.PrettyFormat);
        } catch (Exception e) {
            log.warn("JSON 美化失败: {}", e.getMessage());
            return json;
        }
    }

    /**
     * 将 JSON 对象扁平化为 Map。
     * 例如：{"user":{"name":"a"}} -> {"user.name":"a"}
     *
     * @param json JSON 字符串
     * @return 扁平化结果，失败返回空 Map
     */
    public static Map<String, Object> flatten(String json) {
        Object root = parse(json);
        if (!(root instanceof JSONObject)) {
            return Collections.emptyMap();
        }
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        flattenNode("", root, result);
        return result;
    }

    /**
     * 提取对象数组中某个字段的值列表。
     *
     * @param obj       源对象
     * @param fieldName 字段名
     * @param fieldType 字段类型
     * @param <T>       字段类型参数
     * @return 字段值列表，失败返回空列表
     */
    public static <T> List<T> extractFieldList(Object obj, String fieldName, Class<T> fieldType) {
        if (obj == null || isBlank(fieldName) || fieldType == null) {
            return Collections.emptyList();
        }
        try {
            Object root = JSON.parse(JSON.toJSONString(obj));
            List<Object> items = toObjectList(root);
            if (items.isEmpty()) {
                return Collections.emptyList();
            }

            List<T> result = new ArrayList<T>();
            for (Object item : items) {
                Object fieldValue = getNode(item, fieldName);
                if (fieldValue == null) {
                    continue;
                }
                T value = convert(fieldValue, fieldType);
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
     * 获取通用 Map。
     *
     * @param obj 源对象
     * @return Map，失败返回空 Map
     */
    public static Map<String, Object> toMap(Object obj) {
        if (obj == null) {
            return Collections.emptyMap();
        }
        try {
            return JSON.parseObject(JSON.toJSONString(obj), new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            log.warn("对象转 Map 失败: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    /**
     * 获取指定键值类型的 Map。
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
            String json = JSON.toJSONString(obj);
            Map<K, V> map = JSON.parseObject(json, new TypeReference<Map<K, V>>() {
            });
            return map == null ? Collections.<K, V>emptyMap() : map;
        } catch (Exception e) {
            log.warn("对象转指定类型 Map 失败: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    /**
     * 将对象转换为 List。
     *
     * @param obj         源对象
     * @param elementType 元素类型
     * @param <T>         类型参数
     * @return List，失败返回空列表
     */
    public static <T> List<T> toList(Object obj, Class<T> elementType) {
        if (obj == null || elementType == null) {
            return Collections.emptyList();
        }
        try {
            Object root = JSON.parse(JSON.toJSONString(obj));
            List<Object> items = toObjectList(root);
            if (items.isEmpty()) {
                return Collections.emptyList();
            }
            List<T> result = new ArrayList<T>(items.size());
            for (Object item : items) {
                T value = convert(item, elementType);
                if (value != null) {
                    result.add(value);
                }
            }
            return result;
        } catch (Exception e) {
            log.warn("对象转 List 失败, elementType={}: {}", elementType.getName(), e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 获取 JSON 字符串中的指定路径，并转换为指定类型。
     *
     * @param json  JSON 字符串
     * @param path  路径
     * @param clazz 目标类型
     * @param <T>   类型参数
     * @return 转换后的值，失败返回 null
     */
    public static <T> T get(String json, String path, Class<T> clazz) {
        if (isBlank(json) || isBlank(path) || clazz == null) {
            return null;
        }
        Object value = getNode(json, path);
        return convert(value, clazz);
    }

    /**
     * 获取 JSON 字符串中的指定路径，并转换为复杂类型。
     *
     * @param json    JSON 字符串
     * @param path    路径
     * @param typeRef 目标类型引用
     * @param <T>     类型参数
     * @return 转换后的值，失败返回 null
     */
    public static <T> T get(String json, String path, TypeReference<T> typeRef) {
        if (isBlank(json) || isBlank(path) || typeRef == null) {
            return null;
        }
        Object value = getNode(json, path);
        return convert(value, typeRef);
    }

    /**
     * 根据 JSON Pointer 获取节点。
     *
     * @param json    JSON 字符串
     * @param pointer JSON Pointer，示例：/user/name
     * @return 节点，失败返回 null
     */
    public static Object getNodeByPointer(String json, String pointer) {
        if (isBlank(json) || isBlank(pointer)) {
            return null;
        }
        try {
            Object root = parse(json);
            if (root == null) {
                return null;
            }
            if ("/".equals(pointer)) {
                return root;
            }
            String[] parts = pointer.split("/");
            Object current = root;
            for (int i = 1; i < parts.length; i++) {
                String part = unescapePointerToken(parts[i]);
                if (current == null) {
                    return null;
                }
                if (current instanceof JSONObject) {
                    current = ((JSONObject) current).get(part);
                } else if (current instanceof Map) {
                    current = ((Map<?, ?>) current).get(part);
                } else if (current instanceof JSONArray) {
                    current = getArrayElement((JSONArray) current, part);
                } else if (current instanceof List) {
                    current = getArrayElement((List<?>) current, part);
                } else {
                    return null;
                }
            }
            return current;
        } catch (Exception e) {
            log.warn("根据 JSON Pointer 获取节点失败, pointer={}: {}", pointer, e.getMessage());
            return null;
        }
    }

    /**
     * 根据 JSON Pointer 获取指定类型的值。
     *
     * @param json    JSON 字符串
     * @param pointer JSON Pointer
     * @param clazz   目标类型
     * @param <T>     类型参数
     * @return 转换后的值，失败返回 null
     */
    public static <T> T getByPointer(String json, String pointer, Class<T> clazz) {
        if (isBlank(json) || isBlank(pointer) || clazz == null) {
            return null;
        }
        Object value = getNodeByPointer(json, pointer);
        return convert(value, clazz);
    }

    /**
     * 根据 JSON Pointer 获取复杂类型值。
     *
     * @param json    JSON 字符串
     * @param pointer JSON Pointer
     * @param typeRef 目标类型引用
     * @param <T>     类型参数
     * @return 转换后的值，失败返回 null
     */
    public static <T> T getByPointer(String json, String pointer, TypeReference<T> typeRef) {
        if (isBlank(json) || isBlank(pointer) || typeRef == null) {
            return null;
        }
        Object value = getNodeByPointer(json, pointer);
        return convert(value, typeRef);
    }

    /**
     * 获取全局共享的类型转换结果为 JSONObject。
     *
     * @param obj 源对象
     * @return JSONObject，失败返回 null
     */
    public static JSONObject toJSONObject(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            Object parsed = JSON.parse(JSON.toJSONString(obj));
            return parsed instanceof JSONObject ? (JSONObject) parsed : null;
        } catch (Exception e) {
            log.warn("对象转 JSONObject 失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 获取全局共享的类型转换结果为 JSONArray。
     *
     * @param obj 源对象
     * @return JSONArray，失败返回 null
     */
    public static JSONArray toJSONArray(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            Object parsed = JSON.parse(JSON.toJSONString(obj));
            return parsed instanceof JSONArray ? (JSONArray) parsed : null;
        } catch (Exception e) {
            log.warn("对象转 JSONArray 失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 提取 JSON 中指定路径的值，并转换为指定类型。
     *
     * @param json  JSON 字符串
     * @param path  路径
     * @param clazz 目标类型
     * @param <T>   类型参数
     * @return 转换后的值
     */
    public static <T> T extractByJsonPath(String json, String path, Class<T> clazz) {
        if (isBlank(json) || isBlank(path) || clazz == null) {
            return null;
        }
        return convert(getNode(json, path), clazz);
    }

    /**
     * 提取 JSON 中指定路径的值。
     *
     * @param json JSON 字符串
     * @param path 路径
     * @return 提取结果，失败返回 null
     */
    public static String extractByJsonPath(String json, String path) {
        Object value = getNode(json, path);
        return value == null ? null : String.valueOf(value);
    }

    private static Object getFieldValue(Object current, String fieldName) {
        if (current == null || isBlank(fieldName)) {
            return null;
        }
        if (current instanceof JSONObject) {
            return ((JSONObject) current).get(fieldName);
        }
        if (current instanceof Map) {
            return ((Map<?, ?>) current).get(fieldName);
        }
        Object parsed = parse(JSON.toJSONString(current));
        if (parsed instanceof JSONObject) {
            return ((JSONObject) parsed).get(fieldName);
        }
        return null;
    }

    private static Object getIndexValue(Object current, int index) {
        if (current == null || index < 0) {
            return null;
        }
        if (current instanceof JSONArray) {
            JSONArray array = (JSONArray) current;
            return index >= array.size() ? null : array.get(index);
        }
        if (current instanceof List) {
            List<?> list = (List<?>) current;
            return index >= list.size() ? null : list.get(index);
        }
        if (current instanceof Collection) {
            int i = 0;
            for (Object item : (Collection<?>) current) {
                if (i == index) {
                    return item;
                }
                i++;
            }
            return null;
        }
        if (current.getClass().isArray()) {
            return index >= Array.getLength(current) ? null : Array.get(current, index);
        }
        return null;
    }

    private static Object getArrayElement(JSONArray array, String part) {
        try {
            int index = Integer.parseInt(part);
            return getIndexValue(array, index);
        } catch (Exception e) {
            return null;
        }
    }

    private static Object getArrayElement(List<?> list, String part) {
        try {
            int index = Integer.parseInt(part);
            return getIndexValue(list, index);
        } catch (Exception e) {
            return null;
        }
    }

    private static Object putBySteps(Object current, List<PathStep> steps, int index, Object value) {
        PathStep step = steps.get(index);
        boolean last = index == steps.size() - 1;

        if (step.isField()) {
            JSONObject object = current instanceof JSONObject ? (JSONObject) current : new JSONObject();
            if (last) {
                object.put(step.getFieldName(), value);
                return object;
            }

            Object child = object.get(step.getFieldName());
            if (!isCompatibleContainer(child, steps.get(index + 1))) {
                child = createContainerForStep(steps.get(index + 1));
            }

            object.put(step.getFieldName(), putBySteps(child, steps, index + 1, value));
            return object;
        }

        JSONArray array = current instanceof JSONArray ? (JSONArray) current : new JSONArray();
        ensureArraySize(array, step.getIndex());

        if (last) {
            array.set(step.getIndex(), value);
            return array;
        }

        Object child = array.get(step.getIndex());
        if (!isCompatibleContainer(child, steps.get(index + 1))) {
            child = createContainerForStep(steps.get(index + 1));
        }

        array.set(step.getIndex(), putBySteps(child, steps, index + 1, value));
        return array;
    }

    private static Object removeBySteps(Object current, List<PathStep> steps, int index) {
        PathStep step = steps.get(index);
        boolean last = index == steps.size() - 1;

        if (step.isField()) {
            if (!(current instanceof JSONObject)) {
                return current;
            }
            JSONObject object = (JSONObject) current;
            if (last) {
                object.remove(step.getFieldName());
                return object;
            }
            Object child = object.get(step.getFieldName());
            if (child == null) {
                return object;
            }
            object.put(step.getFieldName(), removeBySteps(child, steps, index + 1));
            return object;
        }

        if (!(current instanceof JSONArray)) {
            return current;
        }
        JSONArray array = (JSONArray) current;
        int removeIndex = step.getIndex();
        if (removeIndex < 0 || removeIndex >= array.size()) {
            return array;
        }
        if (last) {
            array.remove(removeIndex);
            return array;
        }
        Object child = array.get(removeIndex);
        if (child == null) {
            return array;
        }
        array.set(removeIndex, removeBySteps(child, steps, index + 1));
        return array;
    }

    private static Object deepMergeNode(Object target, Object source) {
        if (target == null) {
            return source;
        }
        if (source == null) {
            return target;
        }

        if (target instanceof JSONObject && source instanceof JSONObject) {
            JSONObject targetObj = (JSONObject) target;
            JSONObject sourceObj = (JSONObject) source;
            for (Map.Entry<String, Object> entry : sourceObj.entrySet()) {
                String key = entry.getKey();
                Object sourceValue = entry.getValue();
                Object targetValue = targetObj.get(key);
                if (targetValue instanceof JSONObject && sourceValue instanceof JSONObject) {
                    targetObj.put(key, deepMergeNode(targetValue, sourceValue));
                } else {
                    targetObj.put(key, sourceValue);
                }
            }
            return targetObj;
        }

        if (target instanceof Map && source instanceof Map) {
            JSONObject targetObj = toJSONObject(target);
            JSONObject sourceObj = toJSONObject(source);
            if (targetObj == null || sourceObj == null) {
                return source;
            }
            return deepMergeNode(targetObj, sourceObj);
        }

        return source;
    }

    private static void flattenNode(String prefix, Object node, Map<String, Object> result) {
        if (node == null) {
            result.put(prefix, null);
            return;
        }
        if (node instanceof JSONObject) {
            JSONObject object = (JSONObject) node;
            for (Map.Entry<String, Object> entry : object.entrySet()) {
                String key = isBlank(prefix) ? entry.getKey() : prefix + "." + entry.getKey();
                flattenNode(key, entry.getValue(), result);
            }
            return;
        }
        if (node instanceof JSONArray) {
            JSONArray array = (JSONArray) node;
            for (int i = 0; i < array.size(); i++) {
                flattenNode(prefix + "[" + i + "]", array.get(i), result);
            }
            return;
        }
        result.put(prefix, node);
    }

    private static boolean isCompatibleContainer(Object current, PathStep nextStep) {
        if (current == null) {
            return false;
        }
        if (nextStep == null) {
            return current instanceof JSONObject || current instanceof JSONArray;
        }
        return nextStep.isIndex() ? current instanceof JSONArray : current instanceof JSONObject;
    }

    private static Object createRootContainer(PathStep firstStep) {
        return firstStep != null && firstStep.isIndex() ? new JSONArray() : new JSONObject();
    }

    private static Object createContainerForStep(PathStep step) {
        return step != null && step.isIndex() ? new JSONArray() : new JSONObject();
    }

    private static void ensureArraySize(JSONArray array, int index) {
        while (array.size() <= index) {
            array.add(null);
        }
    }

    private static List<Object> toObjectList(Object root) {
        if (root == null) {
            return Collections.emptyList();
        }
        if (root instanceof JSONArray) {
            return (JSONArray) root;
        }
        if (root instanceof List) {
            return (List<Object>) root;
        }
        if (root instanceof Collection) {
            return new ArrayList<Object>((Collection<?>) root);
        }
        if (root.getClass().isArray()) {
            int length = Array.getLength(root);
            List<Object> result = new ArrayList<Object>(length);
            for (int i = 0; i < length; i++) {
                result.add(Array.get(root, i));
            }
            return result;
        }
        return Collections.emptyList();
    }

    private static List<PathStep> parsePathSteps(String path) {
        if (isBlank(path)) {
            return Collections.emptyList();
        }

        String normalized = path.trim();
        if (normalized.startsWith("$")) {
            normalized = normalized.substring(1);
        }
        if (normalized.startsWith(".")) {
            normalized = normalized.substring(1);
        }
        if (isBlank(normalized)) {
            return Collections.emptyList();
        }

        List<PathStep> steps = new ArrayList<PathStep>();
        String[] parts = normalized.split("\\.");
        for (String part : parts) {
            if (isBlank(part)) {
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
                    if (isBlank(indexText)) {
                        return Collections.emptyList();
                    }
                    try {
                        steps.add(PathStep.ofIndex(Integer.parseInt(indexText)));
                    } catch (Exception e) {
                        return Collections.emptyList();
                    }
                    cursor = endIndex + 1;
                    continue;
                }

                int nextBracket = part.indexOf('[', cursor);
                String fieldName = nextBracket < 0 ? part.substring(cursor) : part.substring(cursor, nextBracket);
                fieldName = fieldName.trim();
                if (!isBlank(fieldName)) {
                    steps.add(PathStep.ofField(fieldName));
                }
                cursor = nextBracket < 0 ? part.length() : nextBracket;
            }
        }
        return steps;
    }

    private static String unescapePointerToken(String token) {
        if (token == null) {
            return null;
        }
        return token.replace("~1", "/").replace("~0", "~");
    }

    private static boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * 路径步骤。
     */
    private static final class PathStep {

        private final String fieldName;
        private final Integer index;

        private PathStep(String fieldName, Integer index) {
            this.fieldName = fieldName;
            this.index = index;
        }

        private static PathStep ofField(String fieldName) {
            return new PathStep(fieldName, null);
        }

        private static PathStep ofIndex(Integer index) {
            return new PathStep(null, index);
        }

        private boolean isField() {
            return fieldName != null;
        }

        private boolean isIndex() {
            return index != null;
        }

        private String getFieldName() {
            return fieldName;
        }

        private Integer getIndex() {
            return index;
        }
    }
}