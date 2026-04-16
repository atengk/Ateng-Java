package local.ateng.java.customutils.utils;

import com.alibaba.fastjson2.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * JSON 工具类。
 * 提供常用的 JSON 序列化、反序列化、对象转换、路径读写、合并与扁平化能力。
 * 基于 FastJson2 实现。
 *
 * @author Ateng
 * @since 2026-04-16
 */
public final class FastJson2Util {

    private static final Logger log = LoggerFactory.getLogger(FastJson2Util.class);

    private FastJson2Util() {
        throw new UnsupportedOperationException("工具类不可实例化");
    }

    // Fastjson2 序列化特性全集，便于统一查阅、单项引用和批量配置。

    /**
     * 基于字段访问（Field）进行序列化，而不是通过 Getter 方法。
     */
    public static final JSONWriter.Feature WRITER_FIELD_BASED = JSONWriter.Feature.FieldBased;

    /**
     * 序列化时忽略非 Serializable 对象。
     */
    public static final JSONWriter.Feature WRITER_IGNORE_NONE_SERIALIZABLE = JSONWriter.Feature.IgnoreNoneSerializable;

    /**
     * 序列化时遇到非 Serializable 对象直接报错。
     */
    public static final JSONWriter.Feature WRITER_ERROR_ON_NONE_SERIALIZABLE = JSONWriter.Feature.ErrorOnNoneSerializable;

    /**
     * 序列化 Bean 为数组格式。
     */
    public static final JSONWriter.Feature WRITER_BEAN_TO_ARRAY = JSONWriter.Feature.BeanToArray;

    /**
     * 序列化时输出 null 字段。
     */
    public static final JSONWriter.Feature WRITER_WRITE_NULLS = JSONWriter.Feature.WriteNulls;

    /**
     * 序列化时输出 Map 中的 null 值。
     */
    public static final JSONWriter.Feature WRITER_WRITE_MAP_NULL_VALUE = JSONWriter.Feature.WriteMapNullValue;

    /**
     * 输出适合浏览器环境的 JSON。
     */
    public static final JSONWriter.Feature WRITER_BROWSER_COMPATIBLE = JSONWriter.Feature.BrowserCompatible;

    /**
     * 序列化时使用默认值替代 null。
     */
    public static final JSONWriter.Feature WRITER_NULL_AS_DEFAULT_VALUE = JSONWriter.Feature.NullAsDefaultValue;

    /**
     * 序列化时将布尔值写成 1/0。
     */
    public static final JSONWriter.Feature WRITER_WRITE_BOOLEAN_AS_NUMBER = JSONWriter.Feature.WriteBooleanAsNumber;

    /**
     * 序列化时将非字符串值写成字符串。
     */
    public static final JSONWriter.Feature WRITER_WRITE_NON_STRING_VALUE_AS_STRING = JSONWriter.Feature.WriteNonStringValueAsString;

    /**
     * 序列化时写入类型信息。
     */
    public static final JSONWriter.Feature WRITER_WRITE_CLASS_NAME = JSONWriter.Feature.WriteClassName;

    /**
     * 序列化时不写根对象类名。
     */
    public static final JSONWriter.Feature WRITER_NOT_WRITE_ROOT_CLASS_NAME = JSONWriter.Feature.NotWriteRootClassName;

    /**
     * 序列化时不写 HashMap 和 ArrayList 的类名。
     */
    public static final JSONWriter.Feature WRITER_NOT_WRITE_HASHMAP_ARRAYLIST_CLASS_NAME = JSONWriter.Feature.NotWriteHashMapArrayListClassName;

    /**
     * 序列化时不写默认值字段。
     */
    public static final JSONWriter.Feature WRITER_NOT_WRITE_DEFAULT_VALUE = JSONWriter.Feature.NotWriteDefaultValue;

    /**
     * 序列化枚举时使用 name。
     */
    public static final JSONWriter.Feature WRITER_WRITE_ENUMS_USING_NAME = JSONWriter.Feature.WriteEnumsUsingName;

    /**
     * 序列化枚举时使用 toString。
     */
    public static final JSONWriter.Feature WRITER_WRITE_ENUM_USING_TO_STRING = JSONWriter.Feature.WriteEnumUsingToString;

    /**
     * 忽略 getter 异常。
     */
    public static final JSONWriter.Feature WRITER_IGNORE_ERROR_GETTER = JSONWriter.Feature.IgnoreErrorGetter;

    /**
     * 美化输出 JSON。
     */
    public static final JSONWriter.Feature WRITER_PRETTY_FORMAT = JSONWriter.Feature.PrettyFormat;

    /**
     * 启用引用检测。
     */
    public static final JSONWriter.Feature WRITER_REFERENCE_DETECTION = JSONWriter.Feature.ReferenceDetection;

    /**
     * 使用字段名符号表优化输出。
     */
    public static final JSONWriter.Feature WRITER_WRITE_NAME_AS_SYMBOL = JSONWriter.Feature.WriteNameAsSymbol;

    /**
     * BigDecimal 使用普通字符串格式输出。
     */
    public static final JSONWriter.Feature WRITER_WRITE_BIG_DECIMAL_AS_PLAIN = JSONWriter.Feature.WriteBigDecimalAsPlain;

    /**
     * 使用单引号输出字符串。
     */
    public static final JSONWriter.Feature WRITER_USE_SINGLE_QUOTES = JSONWriter.Feature.UseSingleQuotes;

    /**
     * 已废弃：Map 按 key 排序输出。
     */
    @Deprecated
    public static final JSONWriter.Feature WRITER_MAP_SORT_FIELD = JSONWriter.Feature.MapSortField;

    /**
     * null List 输出为空数组。
     */
    public static final JSONWriter.Feature WRITER_WRITE_NULL_LIST_AS_EMPTY = JSONWriter.Feature.WriteNullListAsEmpty;

    /**
     * null String 输出为空字符串。
     */
    public static final JSONWriter.Feature WRITER_WRITE_NULL_STRING_AS_EMPTY = JSONWriter.Feature.WriteNullStringAsEmpty;

    /**
     * null Number 输出为 0。
     */
    public static final JSONWriter.Feature WRITER_WRITE_NULL_NUMBER_AS_ZERO = JSONWriter.Feature.WriteNullNumberAsZero;

    /**
     * null Boolean 输出为 false。
     */
    public static final JSONWriter.Feature WRITER_WRITE_NULL_BOOLEAN_AS_FALSE = JSONWriter.Feature.WriteNullBooleanAsFalse;

    /**
     * 不写空数组。
     */
    public static final JSONWriter.Feature WRITER_NOT_WRITE_EMPTY_ARRAY = JSONWriter.Feature.NotWriteEmptyArray;

    /**
     * 与 NotWriteEmptyArray 等价，2.0.51 起推荐使用。
     */
    public static final JSONWriter.Feature WRITER_IGNORE_EMPTY = JSONWriter.Feature.IgnoreEmpty;

    /**
     * 非字符串 Map Key 按字符串写出。
     */
    public static final JSONWriter.Feature WRITER_WRITE_NON_STRING_KEY_AS_STRING = JSONWriter.Feature.WriteNonStringKeyAsString;

    /**
     * Key-Value 形式按 JavaBean 输出。
     */
    public static final JSONWriter.Feature WRITER_WRITE_PAIR_AS_JAVA_BEAN = JSONWriter.Feature.WritePairAsJavaBean;

    /**
     * ASCII 优化输出。
     */
    public static final JSONWriter.Feature WRITER_OPTIMIZED_FOR_ASCII = JSONWriter.Feature.OptimizedForAscii;

    /**
     * 7 位 ASCII 之外字符使用转义输出。
     */
    public static final JSONWriter.Feature WRITER_ESCAPE_NONE_ASCII = JSONWriter.Feature.EscapeNoneAscii;

    /**
     * byte[] 使用 Base64 字符串输出。
     */
    public static final JSONWriter.Feature WRITER_WRITE_BYTE_ARRAY_AS_BASE64 = JSONWriter.Feature.WriteByteArrayAsBase64;

    /**
     * 忽略非字段 getter。
     */
    public static final JSONWriter.Feature WRITER_IGNORE_NON_FIELD_GETTER = JSONWriter.Feature.IgnoreNonFieldGetter;

    /**
     * 大对象序列化优化。
     */
    public static final JSONWriter.Feature WRITER_LARGE_OBJECT = JSONWriter.Feature.LargeObject;

    /**
     * long 使用字符串输出。
     */
    public static final JSONWriter.Feature WRITER_WRITE_LONG_AS_STRING = JSONWriter.Feature.WriteLongAsString;

    /**
     * 浏览器安全输出。
     */
    public static final JSONWriter.Feature WRITER_BROWSER_SECURE = JSONWriter.Feature.BrowserSecure;

    /**
     * 枚举使用 ordinal 输出。
     */
    public static final JSONWriter.Feature WRITER_WRITE_ENUM_USING_ORDINAL = JSONWriter.Feature.WriteEnumUsingOrdinal;

    /**
     * Throwable 输出类名。
     */
    public static final JSONWriter.Feature WRITER_WRITE_THROWABLE_CLASS_NAME = JSONWriter.Feature.WriteThrowableClassName;

    /**
     * 字段名不加引号。
     */
    public static final JSONWriter.Feature WRITER_UNQUOTE_FIELD_NAME = JSONWriter.Feature.UnquoteFieldName;

    /**
     * 不写 Set 的类名。
     */
    public static final JSONWriter.Feature WRITER_NOT_WRITE_SET_CLASS_NAME = JSONWriter.Feature.NotWriteSetClassName;

    /**
     * 不写 Number 的类名。
     */
    public static final JSONWriter.Feature WRITER_NOT_WRITE_NUMBER_CLASS_NAME = JSONWriter.Feature.NotWriteNumberClassName;

    /**
     * 按 key 排序输出 Map。
     */
    public static final JSONWriter.Feature WRITER_SORT_MAP_ENTRIES_BY_KEYS = JSONWriter.Feature.SortMapEntriesByKeys;

    /**
     * PrettyFormat 的 2 空格缩进版本。
     */
    public static final JSONWriter.Feature WRITER_PRETTY_FORMAT_WITH_2_SPACE = JSONWriter.Feature.PrettyFormatWith2Space;

    /**
     * PrettyFormat 的 4 空格缩进版本。
     */
    public static final JSONWriter.Feature WRITER_PRETTY_FORMAT_WITH_4_SPACE = JSONWriter.Feature.PrettyFormatWith4Space;

    /**
     * Date 按毫秒时间戳输出。
     */
    public static final JSONWriter.Feature WRITER_WRITER_UTIL_DATE_AS_MILLIS = JSONWriter.Feature.WriterUtilDateAsMillis;

    /**
     * 浮点特殊值按字符串输出。
     */
    public static final JSONWriter.Feature WRITER_WRITE_FLOAT_SPECIAL_AS_STRING = JSONWriter.Feature.WriteFloatSpecialAsString;

    /**
     * Fastjson2 反序列化特性全集，便于统一查阅、单项引用和批量配置。
     */
    public static final JSONReader.Feature READER_FIELD_BASED = JSONReader.Feature.FieldBased;

    /**
     * 反序列化时忽略非 Serializable 对象。
     */
    public static final JSONReader.Feature READER_IGNORE_NONE_SERIALIZABLE = JSONReader.Feature.IgnoreNoneSerializable;

    /**
     * 反序列化时遇到非 Serializable 对象直接报错。
     */
    public static final JSONReader.Feature READER_ERROR_ON_NONE_SERIALIZABLE = JSONReader.Feature.ErrorOnNoneSerializable;

    /**
     * 支持数组映射到 Bean。
     */
    public static final JSONReader.Feature READER_SUPPORT_ARRAY_TO_BEAN = JSONReader.Feature.SupportArrayToBean;

    /**
     * String 字段初始化为空字符串。
     */
    public static final JSONReader.Feature READER_INIT_STRING_FIELD_AS_EMPTY = JSONReader.Feature.InitStringFieldAsEmpty;

    /**
     * 已废弃：支持自动类型。
     */
    @Deprecated
    public static final JSONReader.Feature READER_SUPPORT_AUTO_TYPE = JSONReader.Feature.SupportAutoType;

    /**
     * 智能匹配字段名。
     */
    public static final JSONReader.Feature READER_SUPPORT_SMART_MATCH = JSONReader.Feature.SupportSmartMatch;

    /**
     * 反序列化时使用原生 Java 对象。
     */
    public static final JSONReader.Feature READER_USE_NATIVE_OBJECT = JSONReader.Feature.UseNativeObject;

    /**
     * 支持 Class.forName。
     */
    public static final JSONReader.Feature READER_SUPPORT_CLASS_FOR_NAME = JSONReader.Feature.SupportClassForName;

    /**
     * 忽略输入中的 null 值。
     */
    public static final JSONReader.Feature READER_IGNORE_SET_NULL_VALUE = JSONReader.Feature.IgnoreSetNullValue;

    /**
     * 尽可能使用默认构造方法。
     */
    public static final JSONReader.Feature READER_USE_DEFAULT_CONSTRUCTOR_AS_POSSIBLE = JSONReader.Feature.UseDefaultConstructorAsPossible;

    /**
     * float 使用 BigDecimal 方式读取。
     */
    public static final JSONReader.Feature READER_USE_BIG_DECIMAL_FOR_FLOATS = JSONReader.Feature.UseBigDecimalForFloats;

    /**
     * double 使用 BigDecimal 方式读取。
     */
    public static final JSONReader.Feature READER_USE_BIG_DECIMAL_FOR_DOUBLES = JSONReader.Feature.UseBigDecimalForDoubles;

    /**
     * 枚举不匹配时抛错。
     */
    public static final JSONReader.Feature READER_ERROR_ON_ENUM_NOT_MATCH = JSONReader.Feature.ErrorOnEnumNotMatch;

    /**
     * 读取字符串时自动 trim。
     */
    public static final JSONReader.Feature READER_TRIM_STRING = JSONReader.Feature.TrimString;

    /**
     * autoType 不支持时抛错。
     */
    public static final JSONReader.Feature READER_ERROR_ON_NOT_SUPPORT_AUTO_TYPE = JSONReader.Feature.ErrorOnNotSupportAutoType;

    /**
     * 重复 Key 的值转为数组。
     */
    public static final JSONReader.Feature READER_DUPLICATE_KEY_VALUE_AS_ARRAY = JSONReader.Feature.DuplicateKeyValueAsArray;

    /**
     * 允许未加引号的字段名。
     */
    public static final JSONReader.Feature READER_ALLOW_UN_QUOTED_FIELD_NAMES = JSONReader.Feature.AllowUnQuotedFieldNames;

    /**
     * 非字符串 Key 按字符串处理。
     */
    public static final JSONReader.Feature READER_NON_STRING_KEY_AS_STRING = JSONReader.Feature.NonStringKeyAsString;

    /**
     * Base64 字符串自动转 byte[]。
     */
    public static final JSONReader.Feature READER_BASE64_STRING_AS_BYTE_ARRAY = JSONReader.Feature.Base64StringAsByteArray;

    /**
     * 忽略资源关闭检查。
     */
    public static final JSONReader.Feature READER_IGNORE_CHECK_CLOSE = JSONReader.Feature.IgnoreCheckClose;

    /**
     * 基本类型遇到 null 时抛错。
     */
    public static final JSONReader.Feature READER_ERROR_ON_NULL_FOR_PRIMITIVES = JSONReader.Feature.ErrorOnNullForPrimitives;

    /**
     * 反序列化出错时返回 null。
     */
    public static final JSONReader.Feature READER_NULL_ON_ERROR = JSONReader.Feature.NullOnError;

    /**
     * 忽略 autoType 不匹配。
     */
    public static final JSONReader.Feature READER_IGNORE_AUTO_TYPE_NOT_MATCH = JSONReader.Feature.IgnoreAutoTypeNotMatch;

    /**
     * 非 0 数字转 boolean 时视为 true。
     */
    public static final JSONReader.Feature READER_NON_ZERO_NUMBER_CAST_TO_BOOLEAN_AS_TRUE = JSONReader.Feature.NonZeroNumberCastToBooleanAsTrue;

    /**
     * 忽略 null 属性值。
     */
    public static final JSONReader.Feature READER_IGNORE_NULL_PROPERTY_VALUE = JSONReader.Feature.IgnoreNullPropertyValue;

    /**
     * 遇到未知属性时报错。
     */
    public static final JSONReader.Feature READER_ERROR_ON_UNKNOWN_PROPERTIES = JSONReader.Feature.ErrorOnUnknownProperties;

    /**
     * 空字符串按 null 处理。
     */
    public static final JSONReader.Feature READER_EMPTY_STRING_AS_NULL = JSONReader.Feature.EmptyStringAsNull;

    /**
     * 数值溢出时不报错。
     */
    public static final JSONReader.Feature READER_NON_ERROR_ON_NUMBER_OVERFLOW = JSONReader.Feature.NonErrorOnNumberOverflow;

    /**
     * 整数按 BigInteger 读取。
     */
    public static final JSONReader.Feature READER_USE_BIG_INTEGER_FOR_INTS = JSONReader.Feature.UseBigIntegerForInts;

    /**
     * 整数按 Long 读取。
     */
    public static final JSONReader.Feature READER_USE_LONG_FOR_INTS = JSONReader.Feature.UseLongForInts;

    /**
     * 禁用单引号。
     */
    public static final JSONReader.Feature READER_DISABLE_SINGLE_QUOTE = JSONReader.Feature.DisableSingleQuote;

    /**
     * 小数按 Double 读取。
     */
    public static final JSONReader.Feature READER_USE_DOUBLE_FOR_DECIMALS = JSONReader.Feature.UseDoubleForDecimals;

    /**
     * 禁用引用检测。
     */
    public static final JSONReader.Feature READER_DISABLE_REFERENCE_DETECT = JSONReader.Feature.DisableReferenceDetect;

    /**
     * 禁用单元素字符串数组自动拆包。
     */
    public static final JSONReader.Feature READER_DISABLE_STRING_ARRAY_UNWRAPPING = JSONReader.Feature.DisableStringArrayUnwrapping;

    /**
     * Fastjson2 序列化特性全集。
     */
    public static final JSONWriter.Feature[] JSON_WRITER_FEATURES_ALL = new JSONWriter.Feature[]{
            JSONWriter.Feature.FieldBased,
            JSONWriter.Feature.IgnoreNoneSerializable,
            JSONWriter.Feature.ErrorOnNoneSerializable,
            JSONWriter.Feature.BeanToArray,
            JSONWriter.Feature.WriteNulls,
            JSONWriter.Feature.WriteMapNullValue,
            JSONWriter.Feature.BrowserCompatible,
            JSONWriter.Feature.NullAsDefaultValue,
            JSONWriter.Feature.WriteBooleanAsNumber,
            JSONWriter.Feature.WriteNonStringValueAsString,
            JSONWriter.Feature.WriteClassName,
            JSONWriter.Feature.NotWriteRootClassName,
            JSONWriter.Feature.NotWriteHashMapArrayListClassName,
            JSONWriter.Feature.NotWriteDefaultValue,
            JSONWriter.Feature.WriteEnumsUsingName,
            JSONWriter.Feature.WriteEnumUsingToString,
            JSONWriter.Feature.IgnoreErrorGetter,
            JSONWriter.Feature.PrettyFormat,
            JSONWriter.Feature.ReferenceDetection,
            JSONWriter.Feature.WriteNameAsSymbol,
            JSONWriter.Feature.WriteBigDecimalAsPlain,
            JSONWriter.Feature.UseSingleQuotes,
            JSONWriter.Feature.MapSortField,
            JSONWriter.Feature.WriteNullListAsEmpty,
            JSONWriter.Feature.WriteNullStringAsEmpty,
            JSONWriter.Feature.WriteNullNumberAsZero,
            JSONWriter.Feature.WriteNullBooleanAsFalse,
            JSONWriter.Feature.NotWriteEmptyArray,
            JSONWriter.Feature.IgnoreEmpty,
            JSONWriter.Feature.WriteNonStringKeyAsString,
            JSONWriter.Feature.WritePairAsJavaBean,
            JSONWriter.Feature.OptimizedForAscii,
            JSONWriter.Feature.EscapeNoneAscii,
            JSONWriter.Feature.WriteByteArrayAsBase64,
            JSONWriter.Feature.IgnoreNonFieldGetter,
            JSONWriter.Feature.LargeObject,
            JSONWriter.Feature.WriteLongAsString,
            JSONWriter.Feature.BrowserSecure,
            JSONWriter.Feature.WriteEnumUsingOrdinal,
            JSONWriter.Feature.WriteThrowableClassName,
            JSONWriter.Feature.UnquoteFieldName,
            JSONWriter.Feature.NotWriteSetClassName,
            JSONWriter.Feature.NotWriteNumberClassName,
            JSONWriter.Feature.SortMapEntriesByKeys,
            JSONWriter.Feature.PrettyFormatWith2Space,
            JSONWriter.Feature.PrettyFormatWith4Space,
            JSONWriter.Feature.WriterUtilDateAsMillis,
            JSONWriter.Feature.WriteFloatSpecialAsString
    };

    /**
     * Fastjson2 反序列化特性全集。
     */
    public static final JSONReader.Feature[] JSON_READER_FEATURES_ALL = new JSONReader.Feature[]{
            JSONReader.Feature.FieldBased,
            JSONReader.Feature.IgnoreNoneSerializable,
            JSONReader.Feature.ErrorOnNoneSerializable,
            JSONReader.Feature.SupportArrayToBean,
            JSONReader.Feature.InitStringFieldAsEmpty,
            JSONReader.Feature.SupportAutoType,
            JSONReader.Feature.SupportSmartMatch,
            JSONReader.Feature.UseNativeObject,
            JSONReader.Feature.SupportClassForName,
            JSONReader.Feature.IgnoreSetNullValue,
            JSONReader.Feature.UseDefaultConstructorAsPossible,
            JSONReader.Feature.UseBigDecimalForFloats,
            JSONReader.Feature.UseBigDecimalForDoubles,
            JSONReader.Feature.ErrorOnEnumNotMatch,
            JSONReader.Feature.TrimString,
            JSONReader.Feature.ErrorOnNotSupportAutoType,
            JSONReader.Feature.DuplicateKeyValueAsArray,
            JSONReader.Feature.AllowUnQuotedFieldNames,
            JSONReader.Feature.NonStringKeyAsString,
            JSONReader.Feature.Base64StringAsByteArray,
            JSONReader.Feature.IgnoreCheckClose,
            JSONReader.Feature.ErrorOnNullForPrimitives,
            JSONReader.Feature.NullOnError,
            JSONReader.Feature.IgnoreAutoTypeNotMatch,
            JSONReader.Feature.NonZeroNumberCastToBooleanAsTrue,
            JSONReader.Feature.IgnoreNullPropertyValue,
            JSONReader.Feature.ErrorOnUnknownProperties,
            JSONReader.Feature.EmptyStringAsNull,
            JSONReader.Feature.NonErrorOnNumberOverflow,
            JSONReader.Feature.UseBigIntegerForInts,
            JSONReader.Feature.UseLongForInts,
            JSONReader.Feature.DisableSingleQuote,
            JSONReader.Feature.UseDoubleForDecimals,
            JSONReader.Feature.DisableReferenceDetect,
            JSONReader.Feature.DisableStringArrayUnwrapping
    };

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
    public static String toJsonString(Object obj, JSONWriter.Feature... features) {
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
            return JSON.toJSONString(obj, JSONWriter.Feature.PrettyFormat);
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
        return json == null ? null : json.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    /**
     * 对象转 JSON 字节数组。
     *
     * @param obj      源对象
     * @param features 写入特性
     * @return JSON 字节数组，失败返回 null
     */
    public static byte[] toJsonBytes(Object obj, JSONWriter.Feature... features) {
        String json = toJsonString(obj, features);
        return json == null ? null : json.getBytes(java.nio.charset.StandardCharsets.UTF_8);
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
     * @param <T>      类型参数
     * @param features 读取特性
     * @return 目标对象，失败返回 null
     */
    public static <T> T parseObject(String json, Class<T> clazz, JSONReader.Feature... features) {
        if (isBlank(json) || clazz == null) {
            return null;
        }
        try {
            return JSON.parseObject(json, clazz, features);
        } catch (Exception e) {
            log.warn("JSON 转对象失败, class={}: {}", clazz.getName(), e.getMessage());
            return null;
        }
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
        try {
            return JSON.parseObject(new String(bytes, java.nio.charset.StandardCharsets.UTF_8), clazz);
        } catch (Exception e) {
            log.warn("JSON 字节数组转对象失败, class={}: {}", clazz.getName(), e.getMessage());
            return null;
        }
    }

    /**
     * JSON 字节数组转对象。
     *
     * @param bytes    JSON 字节数组
     * @param clazz    目标类型
     * @param <T>      类型参数
     * @param features 读取特性
     * @return 目标对象，失败返回 null
     */
    public static <T> T parseObject(byte[] bytes, Class<T> clazz, JSONReader.Feature... features) {
        if (bytes == null || bytes.length == 0 || clazz == null) {
            return null;
        }
        try {
            return JSON.parseObject(new String(bytes, java.nio.charset.StandardCharsets.UTF_8), clazz, features);
        } catch (Exception e) {
            log.warn("JSON 字节数组转对象失败, class={}: {}", clazz.getName(), e.getMessage());
            return null;
        }
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
            return JSON.parseObject(json, typeRef.getType());
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
     * @param <T>      类型参数
     * @param features 读取特性
     * @return 目标对象，失败返回 null
     */
    public static <T> T parseObject(String json, TypeReference<T> typeRef, JSONReader.Feature... features) {
        if (isBlank(json) || typeRef == null) {
            return null;
        }
        try {
            return JSON.parseObject(json, typeRef.getType(), features);
        } catch (Exception e) {
            log.warn("JSON 转复杂对象失败: {}", e.getMessage());
            return null;
        }
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
        try {
            return JSON.parseObject(new String(bytes, java.nio.charset.StandardCharsets.UTF_8), typeRef.getType());
        } catch (Exception e) {
            log.warn("JSON 字节数组转复杂对象失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * JSON 字节数组转复杂类型。
     *
     * @param bytes    JSON 字节数组
     * @param typeRef  类型引用
     * @param <T>      类型参数
     * @param features 读取特性
     * @return 目标对象，失败返回 null
     */
    public static <T> T parseObject(byte[] bytes, TypeReference<T> typeRef, JSONReader.Feature... features) {
        if (bytes == null || bytes.length == 0 || typeRef == null) {
            return null;
        }
        try {
            return JSON.parseObject(new String(bytes, java.nio.charset.StandardCharsets.UTF_8), typeRef.getType(), features);
        } catch (Exception e) {
            log.warn("JSON 字节数组转复杂对象失败: {}", e.getMessage());
            return null;
        }
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
     * @param <T>      类型参数
     * @param features 读取特性
     * @return 目标对象，失败返回 null
     */
    public static <T> T parseObject(String json, Type type, JSONReader.Feature... features) {
        if (isBlank(json) || type == null) {
            return null;
        }
        try {
            return JSON.parseObject(json, type, features);
        } catch (Exception e) {
            log.warn("JSON 转 Type 对象失败: {}", e.getMessage());
            return null;
        }
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
            return JSON.parseArray(json, elementType);
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
     * @param <T>         类型参数
     * @param features    读取特性
     * @return List，失败返回空列表
     */
    public static <T> List<T> parseList(String json, Class<T> elementType, JSONReader.Feature... features) {
        if (isBlank(json) || elementType == null) {
            return Collections.emptyList();
        }
        try {
            return JSON.parseArray(json, elementType, features);
        } catch (Exception e) {
            log.warn("JSON 转 List 失败, elementType={}: {}", elementType.getName(), e.getMessage());
            return Collections.emptyList();
        }
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
            List<T> list = JSON.parseArray(json, elementType);
            if (list == null || list.isEmpty()) {
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
     * @param <T>         类型参数
     * @param features    读取特性
     * @return Set，失败返回空集合
     */
    public static <T> Set<T> parseSet(String json, Class<T> elementType, JSONReader.Feature... features) {
        if (isBlank(json) || elementType == null) {
            return Collections.emptySet();
        }
        try {
            List<T> list = JSON.parseArray(json, elementType, features);
            if (list == null || list.isEmpty()) {
                return Collections.emptySet();
            }
            return new LinkedHashSet<T>(list);
        } catch (Exception e) {
            log.warn("JSON 转 Set 失败, elementType={}: {}", elementType.getName(), e.getMessage());
            return Collections.emptySet();
        }
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
    public static Map<String, Object> parseMap(String json, JSONReader.Feature... features) {
        if (isBlank(json)) {
            return Collections.emptyMap();
        }
        try {
            return JSON.parseObject(json, new TypeReference<Map<String, Object>>() {
            }, features);
        } catch (Exception e) {
            log.warn("JSON 转 Map<String, Object> 失败: {}", e.getMessage());
            return Collections.emptyMap();
        }
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
     * @param <K>       键类型参数
     * @param <V>       值类型参数
     * @param features  读取特性
     * @return Map，失败返回空 Map
     */
    public static <K, V> Map<K, V> parseMap(String json, Class<K> keyType, Class<V> valueType, JSONReader.Feature... features) {
        if (isBlank(json) || keyType == null || valueType == null) {
            return Collections.emptyMap();
        }
        try {
            return JSON.parseObject(json, new TypeReference<Map<K, V>>() {
            }, features);
        } catch (Exception e) {
            log.warn("JSON 转指定类型 Map 失败: {}", e.getMessage());
            return Collections.emptyMap();
        }
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
    public static Object parse(String json, JSONReader.Feature... features) {
        if (isBlank(json)) {
            return null;
        }
        try {
            return JSON.parse(json, features);
        } catch (Exception e) {
            log.warn("解析 JSON 失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 转换为 JSONObject。
     *
     * @param json JSON 字符串
     * @return JSONObject，失败返回 null
     */
    public static JSONObject parseObjectNode(String json) {
        Object root = parse(json);
        if (root instanceof JSONObject) {
            return (JSONObject) root;
        }
        return null;
    }

    /**
     * 转换为 JSONObject。
     *
     * @param json     JSON 字符串
     * @param features 读取特性
     * @return JSONObject，失败返回 null
     */
    public static JSONObject parseObjectNode(String json, JSONReader.Feature... features) {
        Object root = parse(json, features);
        if (root instanceof JSONObject) {
            return (JSONObject) root;
        }
        return null;
    }

    /**
     * 转换为 JSONArray。
     *
     * @param json JSON 字符串
     * @return JSONArray，失败返回 null
     */
    public static JSONArray parseArrayNode(String json) {
        Object root = parse(json);
        if (root instanceof JSONArray) {
            return (JSONArray) root;
        }
        return null;
    }

    /**
     * 转换为 JSONArray。
     *
     * @param json     JSON 字符串
     * @param features 读取特性
     * @return JSONArray，失败返回 null
     */
    public static JSONArray parseArrayNode(String json, JSONReader.Feature... features) {
        Object root = parse(json);
        if (root instanceof JSONArray) {
            return (JSONArray) root;
        }
        return null;
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
            return JSON.parseObject(JSON.toJSONString(fromValue), typeRef.getType());
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
     * @param path 点路径，支持 user.name、items[0].id
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
     * @param path 点路径，支持 user.name、items[0].id
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
            return JSON.toJSONString(root, JSONWriter.Feature.PrettyFormat);
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

        List<PathStep> steps = new ArrayList<PathStep>();
        String[] parts = path.split("\\.");
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

    /*
        ========================
        一、基础路径
        ========================

        $                     // 根节点
        $.name                // 根节点下 name
        $.user.name           // 嵌套对象字段
        $.user.age

        ========================
        二、数组操作
        ========================

        $.items[0]            // 第一个元素
        $.items[1].id         // 第二个元素的 id
        $.items[*]            // 所有元素
        $.items[*].id         // 所有元素的 id

        $.items[-1]           // 最后一个元素（部分实现支持）
        $.items[0,1]          // 多个索引
        $.items[0:2]          // 区间（0 到 1）

        ========================
        三、通配符
        ========================

        $.*                   // 根下所有字段
        $.user.*              // user 下所有字段

        ========================
        四、条件过滤（非常重要）
        ========================

        $.items[?(@.id == 1)]                // id 等于 1
        $.items[?(@.age > 18)]               // age 大于 18
        $.items[?(@.name == 'ateng')]        // 字符串匹配

        $.items[?(@.price >= 100 && @.price <= 200)]  // 区间过滤

        ========================
        五、多条件组合
        ========================

        $.items[?(@.age > 18 && @.city == '重庆')]
        $.items[?(@.age > 18 || @.vip == true)]

        ========================
        六、字段存在判断
        ========================

        $.items[?(@.name)]
        $.items[?(@.age != null)]

        ========================
        七、深度扫描（递归查找）
        ========================

        $..name              // 查找所有 name 字段（全局）
        $..id                // 所有 id

        ========================
        八、长度 / size
        ========================

        $.items.size()       // 数组长度
        $.items.length()     // 同上（部分实现支持）

        ========================
        九、函数（部分支持）
        ========================

        $.items.min()
        $.items.max()
        $.items.avg()

        ========================
        十、复杂组合（实战）
        ========================

        $.items[?(@.status == 1)].id
        // 取 status=1 的所有 id

        $.orders[?(@.amount > 100)].user.name
        // 过滤订单后再取用户名字

        $..items[?(@.price > 100)].name
        // 全局查找价格大于100的商品名称

        ========================
        十一、更新 / 删除常用路径
        ========================

        $.user.name
        $.items[0].id
        $.items[?(@.id == 1)].name   // 注意：复杂路径 set/remove 需谨慎

        ========================
        十二、推荐实践
        ========================

        1. 高频路径建议缓存 JSONPath 对象
        2. 大 JSON 使用 extract（避免全量解析）
        3. set/remove 尽量使用“确定路径”，避免复杂过滤路径
        4. 复杂过滤建议先 eval 再处理（更安全）

     */

    /**
     * JSONPath 缓存。
     * <p>
     * Fastjson2 官方建议缓存 JSONPath 实例，用于高频路径提取场景。
     *
     * @author Ateng
     * @since 2026-04-16
     */
    private static final ConcurrentMap<String, JSONPath> JSON_PATH_CACHE = new ConcurrentHashMap<String, JSONPath>();

    /**
     * 获取或创建 JSONPath 实例。
     * <p>
     * 适合在高频使用同一路径时复用，避免重复解析路径表达式。
     *
     * @param path JSONPath 表达式
     * @return JSONPath 实例，失败返回 null
     */
    public static JSONPath getJsonPath(String path) {
        if (isBlank(path)) {
            return null;
        }
        try {
            JSONPath jsonPath = JSON_PATH_CACHE.get(path);
            if (jsonPath != null) {
                return jsonPath;
            }
            JSONPath newJsonPath = JSONPath.of(path);
            JSONPath oldJsonPath = JSON_PATH_CACHE.putIfAbsent(path, newJsonPath);
            return oldJsonPath == null ? newJsonPath : oldJsonPath;
        } catch (Exception e) {
            log.warn("创建 JSONPath 失败, path={}: {}", path, e.getMessage());
            return null;
        }
    }

    /**
     * 使用 JSONPath 从对象中提取值。
     *
     * @param root 根对象
     * @param path JSONPath 表达式
     * @return 提取结果，失败返回 null
     */
    public static Object evalByJsonPath(Object root, String path) {
        if (root == null || isBlank(path)) {
            return null;
        }
        try {
            return JSONPath.eval(root, path);
        } catch (Exception e) {
            log.warn("JSONPath 提取失败, path={}: {}", path, e.getMessage());
            return null;
        }
    }

    /**
     * 使用 JSONPath 从 JSON 字符串中提取值。
     *
     * @param json JSON 字符串
     * @param path JSONPath 表达式
     * @return 提取结果，失败返回 null
     */
    public static Object evalByJsonPath(String json, String path) {
        if (isBlank(json) || isBlank(path)) {
            return null;
        }
        Object root = parse(json);
        return evalByJsonPath(root, path);
    }

    /**
     * 使用 JSONPath 从对象中提取值，并转换为指定类型。
     *
     * @param root  根对象
     * @param path  JSONPath 表达式
     * @param clazz 目标类型
     * @param <T>   类型参数
     * @return 转换后的结果，失败返回 null
     */
    public static <T> T evalByJsonPath(Object root, String path, Class<T> clazz) {
        if (root == null || isBlank(path) || clazz == null) {
            return null;
        }
        return convert(evalByJsonPath(root, path), clazz);
    }

    /**
     * 使用 JSONPath 从 JSON 字符串中提取值，并转换为指定类型。
     *
     * @param json  JSON 字符串
     * @param path  JSONPath 表达式
     * @param clazz 目标类型
     * @param <T>   类型参数
     * @return 转换后的结果，失败返回 null
     */
    public static <T> T evalByJsonPath(String json, String path, Class<T> clazz) {
        if (isBlank(json) || isBlank(path) || clazz == null) {
            return null;
        }
        return convert(evalByJsonPath(json, path), clazz);
    }

    /**
     * 判断 JSONPath 是否命中目标节点。
     *
     * @param root 根对象
     * @param path JSONPath 表达式
     * @return 命中返回 true，否则返回 false
     */
    public static boolean containsByJsonPath(Object root, String path) {
        if (root == null || isBlank(path)) {
            return false;
        }
        try {
            return JSONPath.contains(root, path);
        } catch (Exception e) {
            log.warn("JSONPath 命中判断失败, path={}: {}", path, e.getMessage());
            return false;
        }
    }

    /**
     * 判断 JSON 字符串中是否存在指定 JSONPath。
     *
     * @param json JSON 字符串
     * @param path JSONPath 表达式
     * @return 命中返回 true，否则返回 false
     */
    public static boolean containsByJsonPath(String json, String path) {
        if (isBlank(json) || isBlank(path)) {
            return false;
        }
        Object root = parse(json);
        return containsByJsonPath(root, path);
    }

    /**
     * 使用 JSONPath 对对象进行赋值。
     * <p>
     * 对象会被原地修改。
     *
     * @param root  根对象
     * @param path  JSONPath 表达式
     * @param value 新值
     */
    public static void setByJsonPath(Object root, String path, Object value) {
        if (root == null || isBlank(path)) {
            return;
        }
        try {
            JSONPath.set(root, path, value);
        } catch (Exception e) {
            log.warn("JSONPath 赋值失败, path={}: {}", path, e.getMessage());
        }
    }

    /**
     * 使用 JSONPath 对 JSON 字符串进行赋值。
     *
     * @param json  JSON 字符串
     * @param path  JSONPath 表达式
     * @param value 新值
     * @return 修改后的 JSON 字符串，失败返回原字符串
     */
    public static String setByJsonPath(String json, String path, Object value) {
        if (isBlank(json) || isBlank(path)) {
            return json;
        }
        try {
            Object root = parse(json);
            if (root == null) {
                return json;
            }
            setByJsonPath(root, path, value);
            return toJsonString(root);
        } catch (Exception e) {
            log.warn("JSONPath 字符串赋值失败, path={}: {}", path, e.getMessage());
            return json;
        }
    }

    /**
     * 使用 JSONPath 从对象中删除节点。
     * <p>
     * 对象会被原地修改。
     *
     * @param root 根对象
     * @param path JSONPath 表达式
     */
    public static void removeByJsonPath(Object root, String path) {
        if (root == null || isBlank(path)) {
            return;
        }
        try {
            JSONPath.remove(root, path);
        } catch (Exception e) {
            log.warn("JSONPath 删除失败, path={}: {}", path, e.getMessage());
        }
    }

    /**
     * 使用 JSONPath 从 JSON 字符串中删除节点。
     *
     * @param json JSON 字符串
     * @param path JSONPath 表达式
     * @return 修改后的 JSON 字符串，失败返回原字符串
     */
    public static String removeByJsonPath(String json, String path) {
        if (isBlank(json) || isBlank(path)) {
            return json;
        }
        try {
            Object root = parse(json);
            if (root == null) {
                return json;
            }
            removeByJsonPath(root, path);
            return toJsonString(root);
        } catch (Exception e) {
            log.warn("JSONPath 字符串删除失败, path={}: {}", path, e.getMessage());
            return json;
        }
    }

    /**
     * 使用 JSONPath 对 JSON 字符串做部分提取。
     * <p>
     * 适合大 JSON 的按需读取，避免整文档完整解析。
     *
     * @param json JSON 字符串
     * @param path JSONPath 表达式
     * @return 提取结果，失败返回 null
     */
    public static Object extractByJsonPath(String json, String path) {
        if (isBlank(json) || isBlank(path)) {
            return null;
        }
        try {
            JSONPath jsonPath = getJsonPath(path);
            if (jsonPath == null) {
                return null;
            }
            return jsonPath.extract(JSONReader.of(json));
        } catch (Exception e) {
            log.warn("JSONPath 部分提取失败, path={}: {}", path, e.getMessage());
            return null;
        }
    }

    /**
     * 使用 JSONPath 对 JSON 字符串做部分提取，并转换为指定类型。
     *
     * @param json  JSON 字符串
     * @param path  JSONPath 表达式
     * @param clazz 目标类型
     * @param <T>   类型参数
     * @return 转换后的结果，失败返回 null
     */
    public static <T> T extractByJsonPath(String json, String path, Class<T> clazz) {
        if (isBlank(json) || isBlank(path) || clazz == null) {
            return null;
        }
        return convert(extractByJsonPath(json, path), clazz);
    }

}