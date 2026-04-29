package io.github.atengk.utils;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiPredicate;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * 基于 JDK 原生 API 实现的通用 Map 工具类。
 *
 * @author Ateng
 * @since 2026-04-29
 */
public final class MapUtil {

    private static final DateTimeFormatter DATE_TIME_SPACE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_TIME_MINUTE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final ZoneId SYSTEM_ZONE = ZoneId.systemDefault();

    private MapUtil() {
        throw new UnsupportedOperationException("MapUtil cannot be instantiated");
    }

    /**
     * 判断 Map 是否为 null 或空。
     *
     * @param map 待检查的 Map
     * @return 如果 Map 为 null 或空则返回 true
     */
    public static boolean isEmpty(Map<?, ?> map) {
        return map == null || map.isEmpty();
    }

    /**
     * 判断 Map 是否非 null 且非空。
     *
     * @param map 待检查的 Map
     * @return 如果 Map 至少包含一个条目则返回 true
     */
    public static boolean isNotEmpty(Map<?, ?> map) {
        return !isEmpty(map);
    }

    /**
     * 判断 Map 是否为 null 或空。
     *
     * @param map 待检查的 Map
     * @return 如果 Map 为 null 或空则返回 true
     */
    public static boolean isNullOrEmpty(Map<?, ?> map) {
        return isEmpty(map);
    }

    /**
     * 安全返回 Map 的大小。
     *
     * @param map 待检查的 Map
     * @return Map 大小；如果 Map 为 null 则返回 0
     */
    public static int size(Map<?, ?> map) {
        return map == null ? 0 : map.size();
    }

    /**
     * 安全判断 Map 是否包含指定键。
     *
     * @param map 待检查的 Map
     * @param key 待查找的键
     * @return 如果 Map 包含指定键则返回 true
     */
    public static boolean containsKey(Map<?, ?> map, Object key) {
        return map != null && map.containsKey(key);
    }

    /**
     * 安全判断 Map 是否包含指定值。
     *
     * @param map 待检查的 Map
     * @param value 待查找的值
     * @return 如果 Map 包含指定值则返回 true
     */
    public static boolean containsValue(Map<?, ?> map, Object value) {
        return map != null && map.containsValue(value);
    }

    /**
     * 安全判断 Map 是否拥有指定键。
     *
     * @param map 待检查的 Map
     * @param key 待查找的键
     * @return 如果 Map 包含指定键则返回 true
     */
    public static boolean hasKey(Map<?, ?> map, Object key) {
        return containsKey(map, key);
    }

    /**
     * 安全判断 Map 是否拥有指定值。
     *
     * @param map 待检查的 Map
     * @param value 待查找的值
     * @return 如果 Map 包含指定值则返回 true
     */
    public static boolean hasValue(Map<?, ?> map, Object value) {
        return containsValue(map, value);
    }

    /**
     * 当输入 Map 为 null 时返回空 Map。
     *
     * @param map 源 Map
     * @param <K> 键类型
     * @param <V> 值类型
     * @return 原始 Map；如果为 null 则返回不可变空 Map
     */
    public static <K, V> Map<K, V> emptyIfNull(Map<K, V> map) {
        return map == null ? Collections.emptyMap() : map;
    }

    /**
     * 当源 Map 为 null 时返回默认 Map。
     *
     * @param map 源 Map
     * @param defaultMap 默认 Map
     * @param <K> 键类型
     * @param <V> 值类型
     * @return 源 Map 或默认 Map
     */
    public static <K, V> Map<K, V> defaultIfNull(Map<K, V> map, Map<K, V> defaultMap) {
        return map == null ? defaultMap : map;
    }

    /**
     * 将空 Map 转换为 null。
     *
     * @param map 源 Map
     * @param <K> 键类型
     * @param <V> 值类型
     * @return 如果 Map 为 null 或空则返回 null，否则返回原始 Map
     */
    public static <K, V> Map<K, V> nullIfEmpty(Map<K, V> map) {
        return isEmpty(map) ? null : map;
    }

    /**
     * 将空 Map 转换为 null。
     *
     * @param map 源 Map
     * @param <K> 键类型
     * @param <V> 值类型
     * @return 如果 Map 为 null 或空则返回 null，否则返回原始 Map
     */
    public static <K, V> Map<K, V> emptyToNull(Map<K, V> map) {
        return nullIfEmpty(map);
    }

    /**
     * 返回一个移除了 null 值条目的副本。
     *
     * @param map 源 Map
     * @param <K> 键类型
     * @param <V> 值类型
     * @return 不包含 null 值的新 Map
     */
    public static <K, V> Map<K, V> removeNullValue(Map<K, V> map) {
        if (map == null) {
            return new LinkedHashMap<>();
        }
        Map<K, V> result = new LinkedHashMap<>();
        for (Map.Entry<K, V> entry : map.entrySet()) {
            if (entry.getValue() != null) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    /**
     * 返回一个移除了 null 键条目的副本。
     *
     * @param map 源 Map
     * @param <K> 键类型
     * @param <V> 值类型
     * @return 不包含 null 键的新 Map
     */
    public static <K, V> Map<K, V> removeNullKey(Map<K, V> map) {
        if (map == null) {
            return new LinkedHashMap<>();
        }
        Map<K, V> result = new LinkedHashMap<>();
        for (Map.Entry<K, V> entry : map.entrySet()) {
            if (entry.getKey() != null) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    /**
     * 返回一个移除了空白字符序列值条目的副本。
     *
     * @param map 源 Map
     * @param <K> 键类型
     * @return 不包含空白字符串值的新 Map
     */
    public static <K> Map<K, Object> removeBlankValue(Map<K, ?> map) {
        if (map == null) {
            return new LinkedHashMap<>();
        }
        Map<K, Object> result = new LinkedHashMap<>();
        for (Map.Entry<K, ?> entry : map.entrySet()) {
            Object value = entry.getValue();
            if (!(value instanceof CharSequence text && text.toString().trim().isEmpty())) {
                result.put(entry.getKey(), value);
            }
        }
        return result;
    }

    /**
     * 返回一个副本，并去除所有字符序列值两端的空白。
     *
     * @param map 源 Map
     * @param <K> 键类型
     * @return 字符串值已去除首尾空白的新 Map
     */
    public static <K> Map<K, Object> trimStringValue(Map<K, ?> map) {
        if (map == null) {
            return new LinkedHashMap<>();
        }
        Map<K, Object> result = new LinkedHashMap<>();
        for (Map.Entry<K, ?> entry : map.entrySet()) {
            Object value = entry.getValue();
            result.put(entry.getKey(), value instanceof CharSequence text ? text.toString().trim() : value);
        }
        return result;
    }

    /**
     * 创建一个新的 HashMap。
     *
     * @param <K> 键类型
     * @param <V> 值类型
     * @return 新的 HashMap
     */
    public static <K, V> HashMap<K, V> newHashMap() {
        return new HashMap<>();
    }

    /**
     * 创建一个指定初始容量的 HashMap。
     *
     * @param initialCapacity 初始容量
     * @param <K> 键类型
     * @param <V> 值类型
     * @return 新的 HashMap
     */
    public static <K, V> HashMap<K, V> newHashMap(int initialCapacity) {
        if (initialCapacity < 0) {
            throw new IllegalArgumentException("initialCapacity must be >= 0");
        }
        return new HashMap<>(initialCapacity);
    }

    /**
     * 创建一个新的 LinkedHashMap。
     *
     * @param <K> 键类型
     * @param <V> 值类型
     * @return 新的 LinkedHashMap
     */
    public static <K, V> LinkedHashMap<K, V> newLinkedHashMap() {
        return new LinkedHashMap<>();
    }

    /**
     * 创建一个指定初始容量的 LinkedHashMap。
     *
     * @param initialCapacity 初始容量
     * @param <K> 键类型
     * @param <V> 值类型
     * @return 新的 LinkedHashMap
     */
    public static <K, V> LinkedHashMap<K, V> newLinkedHashMap(int initialCapacity) {
        if (initialCapacity < 0) {
            throw new IllegalArgumentException("initialCapacity must be >= 0");
        }
        return new LinkedHashMap<>(initialCapacity);
    }

    /**
     * 创建一个新的 ConcurrentHashMap。
     *
     * @param <K> 键类型
     * @param <V> 值类型
     * @return 新的 ConcurrentHashMap
     */
    public static <K, V> ConcurrentHashMap<K, V> newConcurrentHashMap() {
        return new ConcurrentHashMap<>();
    }

    /**
     * 创建一个包含一个条目的不可变 Map。
     *
     * @param key 键
     * @param value 值
     * @param <K> 键类型
     * @param <V> 值类型
     * @return 不可变 Map
     */
    public static <K, V> Map<K, V> of(K key, V value) {
        Map<K, V> result = new LinkedHashMap<>();
        result.put(key, value);
        return Collections.unmodifiableMap(result);
    }

    /**
     * 创建一个包含两个条目的不可变 Map。
     *
     * @param k1 第一个键
     * @param v1 第一个值
     * @param k2 第二个键
     * @param v2 第二个值
     * @param <K> 键类型
     * @param <V> 值类型
     * @return 不可变 Map
     */
    public static <K, V> Map<K, V> of(K k1, V v1, K k2, V v2) {
        Map<K, V> result = new LinkedHashMap<>();
        result.put(k1, v1);
        result.put(k2, v2);
        return Collections.unmodifiableMap(result);
    }

    /**
     * 根据键值对创建可变 HashMap。
     *
     * @param keyValues 键值对，长度必须为偶数
     * @param <K> 键类型
     * @param <V> 值类型
     * @return 可变 HashMap
     */
    @SuppressWarnings("unchecked")
    public static <K, V> Map<K, V> mutableOf(Object... keyValues) {
        validateKeyValues(keyValues);
        Map<K, V> result = new HashMap<>(keyValues.length / 2);
        for (int i = 0; i < keyValues.length; i += 2) {
            result.put((K) keyValues[i], (V) keyValues[i + 1]);
        }
        return result;
    }

    /**
     * 根据键值对创建可变 LinkedHashMap。
     *
     * @param keyValues 键值对，长度必须为偶数
     * @param <K> 键类型
     * @param <V> 值类型
     * @return 可变 LinkedHashMap
     */
    @SuppressWarnings("unchecked")
    public static <K, V> Map<K, V> linkedOf(Object... keyValues) {
        validateKeyValues(keyValues);
        Map<K, V> result = new LinkedHashMap<>(keyValues.length / 2);
        for (int i = 0; i < keyValues.length; i += 2) {
            result.put((K) keyValues[i], (V) keyValues[i + 1]);
        }
        return result;
    }

    /**
     * 创建一个有序 Map 构建器。
     *
     * @param <K> 键类型
     * @param <V> 值类型
     * @return Map 构建器
     */
    public static <K, V> Builder<K, V> builder() {
        return new Builder<>();
    }

    /**
     * 从 Map 中安全读取值。
     *
     * @param map 源 Map
     * @param key 待读取的键
     * @param <K> 键类型
     * @param <V> 值类型
     * @return 值；如果不存在则返回 null
     */
    public static <K, V> V get(Map<K, V> map, K key) {
        return map == null ? null : map.get(key);
    }

    /**
     * 安全读取值，不存在时返回默认值。
     *
     * @param map 源 Map
     * @param key 待读取的键
     * @param defaultValue 默认值
     * @param <K> 键类型
     * @param <V> 值类型
     * @return 读取到的值或默认值
     */
    public static <K, V> V getOrDefault(Map<K, V> map, K key, V defaultValue) {
        if (map == null) {
            return defaultValue;
        }
        V value = map.get(key);
        return value == null ? defaultValue : value;
    }

    /**
     * 安全读取值，不存在时返回 null。
     *
     * @param map 源 Map
     * @param key 待读取的键
     * @param <K> 键类型
     * @param <V> 值类型
     * @return 值；如果不存在则返回 null
     */
    public static <K, V> V getOrNull(Map<K, V> map, K key) {
        return get(map, key);
    }

    /**
     * 读取必填值。
     *
     * @param map 源 Map
     * @param key 待读取的键
     * @param <K> 键类型
     * @param <V> 值类型
     * @return 非 null 值
     */
    public static <K, V> V getRequired(Map<K, V> map, K key) {
        if (map == null || !map.containsKey(key) || map.get(key) == null) {
            throw new NoSuchElementException("Required map value is missing: " + key);
        }
        return map.get(key);
    }

    /**
     * 读取候选键中第一个存在的键对应的值。
     *
     * @param map 源 Map
     * @param keys 候选键集合
     * @param <K> 键类型
     * @param <V> 值类型
     * @return 第一个匹配的值；如果不存在则返回 null
     */
    public static <K, V> V getFirst(Map<K, V> map, Collection<K> keys) {
        if (map == null || keys == null) {
            return null;
        }
        for (K key : keys) {
            if (map.containsKey(key)) {
                return map.get(key);
            }
        }
        return null;
    }

    /**
     * 从候选键中读取第一个非 null 值。
     *
     * @param map 源 Map
     * @param keys 候选键集合
     * @param <K> 键类型
     * @param <V> 值类型
     * @return 第一个非 null 值；如果不存在则返回 null
     */
    public static <K, V> V getFirstNonNull(Map<K, V> map, Collection<K> keys) {
        if (map == null || keys == null) {
            return null;
        }
        for (K key : keys) {
            V value = map.get(key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    /**
     * 根据点号分隔路径读取嵌套值。
     *
     * @param map 源 Map
     * @param path 点号分隔路径
     * @return 值；如果不存在则返回 null
     */
    public static Object getByPath(Map<String, Object> map, String path) {
        return getByPath(map, path, null);
    }

    /**
     * 根据点号分隔路径读取嵌套值，不存在时返回默认值。
     *
     * @param map 源 Map
     * @param path 点号分隔路径
     * @param defaultValue 默认值
     * @return 读取到的值或默认值
     */
    @SuppressWarnings("unchecked")
    public static Object getByPath(Map<String, Object> map, String path, Object defaultValue) {
        if (map == null || isBlank(path)) {
            return defaultValue;
        }
        Object current = map;
        for (String part : splitPath(path)) {
            if (!(current instanceof Map<?, ?> currentMap)) {
                return defaultValue;
            }
            if (!currentMap.containsKey(part)) {
                return defaultValue;
            }
            current = ((Map<String, Object>) currentMap).get(part);
        }
        return current == null ? defaultValue : current;
    }

    /**
     * 读取 String 值。
     *
     * @param map 源 Map
     * @param key 待读取的键
     * @return String 值；如果不存在则返回 null
     */
    public static String getStr(Map<?, ?> map, Object key) {
        return getStr(map, key, null);
    }

    /**
     * 读取 String 值，不存在时返回默认值。
     *
     * @param map 源 Map
     * @param key 待读取的键
     * @param defaultValue 默认值
     * @return String 值或默认值
     */
    public static String getStr(Map<?, ?> map, Object key, String defaultValue) {
        Object value = rawGet(map, key);
        return value == null ? defaultValue : String.valueOf(value);
    }

    /**
     * 读取 Integer 值。
     *
     * @param map 源 Map
     * @param key 待读取的键
     * @return Integer 值；如果不存在则返回 null
     */
    public static Integer getInt(Map<?, ?> map, Object key) {
        return convertToInteger(rawGet(map, key));
    }

    /**
     * 读取 Long 值。
     *
     * @param map 源 Map
     * @param key 待读取的键
     * @return Long 值；如果不存在则返回 null
     */
    public static Long getLong(Map<?, ?> map, Object key) {
        return convertToLong(rawGet(map, key));
    }

    /**
     * 读取 Double 值。
     *
     * @param map 源 Map
     * @param key 待读取的键
     * @return Double 值；如果不存在则返回 null
     */
    public static Double getDouble(Map<?, ?> map, Object key) {
        return convertToDouble(rawGet(map, key));
    }

    /**
     * 读取 BigDecimal 值。
     *
     * @param map 源 Map
     * @param key 待读取的键
     * @return BigDecimal 值；如果不存在则返回 null
     */
    public static BigDecimal getBigDecimal(Map<?, ?> map, Object key) {
        return convertToBigDecimal(rawGet(map, key));
    }

    /**
     * 读取 Boolean 值。
     *
     * @param map 源 Map
     * @param key 待读取的键
     * @return Boolean 值；如果不存在则返回 null
     */
    public static Boolean getBool(Map<?, ?> map, Object key) {
        return convertToBoolean(rawGet(map, key));
    }

    /**
     * 读取 Date 值。
     *
     * @param map 源 Map
     * @param key 待读取的键
     * @return Date 值；如果不存在则返回 null
     */
    public static Date getDate(Map<?, ?> map, Object key) {
        return convertToDate(rawGet(map, key));
    }

    /**
     * 读取 LocalDate 值。
     *
     * @param map 源 Map
     * @param key 待读取的键
     * @return LocalDate 值；如果不存在则返回 null
     */
    public static LocalDate getLocalDate(Map<?, ?> map, Object key) {
        return convertToLocalDate(rawGet(map, key));
    }

    /**
     * 读取 LocalDateTime 值。
     *
     * @param map 源 Map
     * @param key 待读取的键
     * @return LocalDateTime 值；如果不存在则返回 null
     */
    public static LocalDateTime getLocalDateTime(Map<?, ?> map, Object key) {
        return convertToLocalDateTime(rawGet(map, key));
    }

    /**
     * 读取指定元素类型的列表值。
     *
     * @param map 源 Map
     * @param key 待读取的键
     * @param elementType 元素类型
     * @param <T> 元素类型
     * @return 指定类型列表；如果值为 null 则返回空列表
     */
    public static <T> List<T> getList(Map<?, ?> map, Object key, Class<T> elementType) {
        Objects.requireNonNull(elementType, "elementType must not be null");
        Object value = rawGet(map, key);
        if (value == null) {
            return new ArrayList<>();
        }
        List<T> result = new ArrayList<>();
        if (value instanceof Collection<?> collection) {
            for (Object item : collection) {
                result.add(convertValue(item, elementType));
            }
            return result;
        }
        if (value.getClass().isArray()) {
            int length = Array.getLength(value);
            for (int i = 0; i < length; i++) {
                result.add(convertValue(Array.get(value, i), elementType));
            }
            return result;
        }
        result.add(convertValue(value, elementType));
        return result;
    }

    /**
     * 读取嵌套 Map 值，并将键转换为 String。
     *
     * @param map 源 Map
     * @param key 待读取的键
     * @return 嵌套 Map；如果值为 null 则返回空 Map
     */
    public static Map<String, Object> getMap(Map<?, ?> map, Object key) {
        Object value = rawGet(map, key);
        if (value == null) {
            return new LinkedHashMap<>();
        }
        if (!(value instanceof Map<?, ?> valueMap)) {
            throw new IllegalArgumentException("Value is not a map: " + key);
        }
        return toObjectMap(valueMap);
    }

    /**
     * 读取枚举值。
     *
     * @param map 源 Map
     * @param key 待读取的键
     * @param enumType 枚举类型
     * @param <E> 枚举类型
     * @return 枚举值；如果不存在则返回 null
     */
    public static <E extends Enum<E>> E getEnum(Map<?, ?> map, Object key, Class<E> enumType) {
        Objects.requireNonNull(enumType, "enumType must not be null");
        return convertToEnum(rawGet(map, key), enumType);
    }

    /**
     * 当值非 null 时写入 Map。
     *
     * @param map 目标 Map
     * @param key 待写入的键
     * @param value 待写入的值
     * @param <K> 键类型
     * @param <V> 值类型
     * @return 目标 Map
     */
    public static <K, V> Map<K, V> putIfNotNull(Map<K, V> map, K key, V value) {
        requireMap(map);
        if (value != null) {
            map.put(key, value);
        }
        return map;
    }

    /**
     * 当字符串值非空白时写入 Map。
     *
     * @param map 目标 Map
     * @param key 待写入的键
     * @param value 待写入的值
     * @param <K> 键类型
     * @return 目标 Map
     */
    public static <K> Map<K, String> putIfNotBlank(Map<K, String> map, K key, String value) {
        requireMap(map);
        if (!isBlank(value)) {
            map.put(key, value);
        }
        return map;
    }

    /**
     * 当键不存在时写入值。
     *
     * @param map 目标 Map
     * @param key 待写入的键
     * @param value 待写入的值
     * @param <K> 键类型
     * @param <V> 值类型
     * @return 目标 Map
     */
    public static <K, V> Map<K, V> putIfAbsent(Map<K, V> map, K key, V value) {
        requireMap(map);
        map.putIfAbsent(key, value);
        return map;
    }

    /**
     * 当键已存在时写入值。
     *
     * @param map 目标 Map
     * @param key 待写入的键
     * @param value 待写入的值
     * @param <K> 键类型
     * @param <V> 值类型
     * @return 目标 Map
     */
    public static <K, V> Map<K, V> putIfPresent(Map<K, V> map, K key, V value) {
        requireMap(map);
        if (map.containsKey(key)) {
            map.put(key, value);
        }
        return map;
    }

    /**
     * 当源 Map 非 null 时批量写入所有条目。
     *
     * @param target 目标 Map
     * @param source 源 Map
     * @param <K> 键类型
     * @param <V> 值类型
     * @return 目标 Map
     */
    public static <K, V> Map<K, V> putAllIfNotNull(Map<K, V> target, Map<K, V> source) {
        requireMap(target);
        if (source != null) {
            target.putAll(source);
        }
        return target;
    }

    /**
     * 批量写入所有条目，并跳过值为 null 的条目。
     *
     * @param target 目标 Map
     * @param source 源 Map
     * @param <K> 键类型
     * @param <V> 值类型
     * @return 目标 Map
     */
    public static <K, V> Map<K, V> putAllIgnoreNullValue(Map<K, V> target, Map<K, V> source) {
        requireMap(target);
        if (source != null) {
            for (Map.Entry<K, V> entry : source.entrySet()) {
                if (entry.getValue() != null) {
                    target.put(entry.getKey(), entry.getValue());
                }
            }
        }
        return target;
    }

    /**
     * 当当前值为 null 时设置默认值。
     *
     * @param map 目标 Map
     * @param key 待写入的键
     * @param defaultValue 默认值
     * @param <K> 键类型
     * @param <V> 值类型
     * @return 目标 Map
     */
    public static <K, V> Map<K, V> setDefault(Map<K, V> map, K key, V defaultValue) {
        requireMap(map);
        if (map.get(key) == null) {
            map.put(key, defaultValue);
        }
        return map;
    }

    /**
     * 替换指定键对应的值。
     *
     * @param map 目标 Map
     * @param key 待替换的键
     * @param value 新值
     * @param <K> 键类型
     * @param <V> 值类型
     * @return 目标 Map
     */
    public static <K, V> Map<K, V> replaceValue(Map<K, V> map, K key, V value) {
        requireMap(map);
        if (map.containsKey(key)) {
            map.put(key, value);
        }
        return map;
    }

    /**
     * 批量移除指定键集合。
     *
     * @param map 目标 Map
     * @param keys 待移除的键集合
     * @param <K> 键类型
     * @param <V> 值类型
     * @return 目标 Map
     */
    public static <K, V> Map<K, V> removeKeys(Map<K, V> map, Collection<K> keys) {
        requireMap(map);
        if (keys != null) {
            for (K key : keys) {
                map.remove(key);
            }
        }
        return map;
    }

    /**
     * 合并源 Map 和目标 Map，目标 Map 条目覆盖源 Map 条目。
     *
     * @param source 源 Map
     * @param target 目标 Map
     * @param <K> 键类型
     * @param <V> 值类型
     * @return 合并后的 Map
     */
    public static <K, V> Map<K, V> merge(Map<K, V> source, Map<K, V> target) {
        return mergeOverwrite(source, target);
    }

    /**
     * 合并 Map，覆盖 Map 条目覆盖基础 Map 条目。
     *
     * @param base 基础 Map
     * @param override 覆盖 Map
     * @param <K> 键类型
     * @param <V> 值类型
     * @return 合并后的 Map
     */
    public static <K, V> Map<K, V> mergeOverwrite(Map<K, V> base, Map<K, V> override) {
        Map<K, V> result = new LinkedHashMap<>();
        if (base != null) {
            result.putAll(base);
        }
        if (override != null) {
            result.putAll(override);
        }
        return result;
    }

    /**
     * 合并 Map，并忽略覆盖 Map 中值为 null 的条目。
     *
     * @param base 基础 Map
     * @param override 覆盖 Map
     * @param <K> 键类型
     * @param <V> 值类型
     * @return 合并后的 Map
     */
    public static <K, V> Map<K, V> mergeIgnoreNull(Map<K, V> base, Map<K, V> override) {
        Map<K, V> result = new LinkedHashMap<>();
        if (base != null) {
            result.putAll(base);
        }
        if (override != null) {
            for (Map.Entry<K, V> entry : override.entrySet()) {
                if (entry.getValue() != null) {
                    result.put(entry.getKey(), entry.getValue());
                }
            }
        }
        return result;
    }

    /**
     * 合并 Map，键冲突时保留原始值。
     *
     * @param base 基础 Map
     * @param append 追加 Map
     * @param <K> 键类型
     * @param <V> 值类型
     * @return 合并后的 Map
     */
    public static <K, V> Map<K, V> mergeKeepOriginal(Map<K, V> base, Map<K, V> append) {
        Map<K, V> result = new LinkedHashMap<>();
        if (base != null) {
            result.putAll(base);
        }
        if (append != null) {
            for (Map.Entry<K, V> entry : append.entrySet()) {
                result.putIfAbsent(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    /**
     * 使用自定义值合并函数合并 Map。
     *
     * @param left 左侧 Map
     * @param right 右侧 Map
     * @param valueMerger 重复键的值合并函数
     * @param <K> 键类型
     * @param <V> 值类型
     * @return 合并后的 Map
     */
    public static <K, V> Map<K, V> mergeWith(Map<K, V> left, Map<K, V> right, BinaryOperator<V> valueMerger) {
        Objects.requireNonNull(valueMerger, "valueMerger must not be null");
        Map<K, V> result = new LinkedHashMap<>();
        if (left != null) {
            result.putAll(left);
        }
        if (right != null) {
            for (Map.Entry<K, V> entry : right.entrySet()) {
                result.merge(entry.getKey(), entry.getValue(), valueMerger);
            }
        }
        return result;
    }

    /**
     * 深度合并嵌套 Map，覆盖 Map 条目覆盖基础 Map 条目。
     *
     * @param base 基础 Map
     * @param override 覆盖 Map
     * @return 深度合并后的 Map
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> deepMerge(Map<String, Object> base, Map<String, Object> override) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (base != null) {
            for (Map.Entry<String, Object> entry : base.entrySet()) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        if (override != null) {
            for (Map.Entry<String, Object> entry : override.entrySet()) {
                Object oldValue = result.get(entry.getKey());
                Object newValue = entry.getValue();
                if (oldValue instanceof Map<?, ?> oldMap && newValue instanceof Map<?, ?> newMap) {
                    result.put(entry.getKey(), deepMerge(toObjectMap(oldMap), toObjectMap(newMap)));
                } else {
                    result.put(entry.getKey(), newValue);
                }
            }
        }
        return result;
    }

    /**
     * 合并多个 Map，后面的 Map 覆盖前面的 Map。
     *
     * @param maps 源 Map 集合
     * @param <K> 键类型
     * @param <V> 值类型
     * @return 合并后的 Map
     */
    public static <K, V> Map<K, V> mergeList(Collection<Map<K, V>> maps) {
        Map<K, V> result = new LinkedHashMap<>();
        if (maps != null) {
            for (Map<K, V> map : maps) {
                if (map != null) {
                    result.putAll(map);
                }
            }
        }
        return result;
    }

    /**
     * 使用指定断言过滤条目。
     *
     * @param map 源 Map
     * @param predicate 条目断言
     * @param <K> 键类型
     * @param <V> 值类型
     * @return 过滤后的 Map
     */
    public static <K, V> Map<K, V> filter(Map<K, V> map, BiPredicate<K, V> predicate) {
        Objects.requireNonNull(predicate, "predicate must not be null");
        Map<K, V> result = new LinkedHashMap<>();
        if (map != null) {
            for (Map.Entry<K, V> entry : map.entrySet()) {
                if (predicate.test(entry.getKey(), entry.getValue())) {
                    result.put(entry.getKey(), entry.getValue());
                }
            }
        }
        return result;
    }

    /**
     * 按键过滤条目。
     *
     * @param map 源 Map
     * @param predicate 键断言
     * @param <K> 键类型
     * @param <V> 值类型
     * @return 过滤后的 Map
     */
    public static <K, V> Map<K, V> filterKeys(Map<K, V> map, Predicate<K> predicate) {
        Objects.requireNonNull(predicate, "predicate must not be null");
        return filter(map, (key, value) -> predicate.test(key));
    }

    /**
     * 按值过滤条目。
     *
     * @param map 源 Map
     * @param predicate 值断言
     * @param <K> 键类型
     * @param <V> 值类型
     * @return 过滤后的 Map
     */
    public static <K, V> Map<K, V> filterValues(Map<K, V> map, Predicate<V> predicate) {
        Objects.requireNonNull(predicate, "predicate must not be null");
        return filter(map, (key, value) -> predicate.test(value));
    }

    /**
     * 仅保留指定键。
     *
     * @param map 源 Map
     * @param keys 待保留的键集合
     * @param <K> 键类型
     * @param <V> 值类型
     * @return 过滤后的 Map
     */
    public static <K, V> Map<K, V> includeKeys(Map<K, V> map, Collection<K> keys) {
        if (map == null || keys == null) {
            return new LinkedHashMap<>();
        }
        Set<K> keySet = new LinkedHashSet<>(keys);
        return filterKeys(map, keySet::contains);
    }

    /**
     * 排除指定键。
     *
     * @param map 源 Map
     * @param keys 待排除的键集合
     * @param <K> 键类型
     * @param <V> 值类型
     * @return 过滤后的 Map
     */
    public static <K, V> Map<K, V> excludeKeys(Map<K, V> map, Collection<K> keys) {
        if (map == null) {
            return new LinkedHashMap<>();
        }
        if (keys == null) {
            return new LinkedHashMap<>(map);
        }
        Set<K> keySet = new HashSet<>(keys);
        return filterKeys(map, key -> !keySet.contains(key));
    }

    /**
     * 挑选指定键。
     *
     * @param map 源 Map
     * @param keys 待挑选的键集合
     * @param <K> 键类型
     * @param <V> 值类型
     * @return 过滤后的 Map
     */
    public static <K, V> Map<K, V> pick(Map<K, V> map, Collection<K> keys) {
        return includeKeys(map, keys);
    }

    /**
     * 忽略指定键。
     *
     * @param map 源 Map
     * @param keys 待忽略的键集合
     * @param <K> 键类型
     * @param <V> 值类型
     * @return 过滤后的 Map
     */
    public static <K, V> Map<K, V> omit(Map<K, V> map, Collection<K> keys) {
        return excludeKeys(map, keys);
    }

    /**
     * 保留值非 null 的条目。
     *
     * @param map 源 Map
     * @param <K> 键类型
     * @param <V> 值类型
     * @return 过滤后的 Map
     */
    public static <K, V> Map<K, V> filterNotNullValue(Map<K, V> map) {
        return filterValues(map, Objects::nonNull);
    }

    /**
     * 保留字符串值非空白的条目。
     *
     * @param map 源 Map
     * @param <K> 键类型
     * @return 过滤后的 Map
     */
    public static <K> Map<K, String> filterNotBlankValue(Map<K, String> map) {
        return filterValues(map, value -> !isBlank(value));
    }

    /**
     * 转换键并保留原始值。
     *
     * @param map 源 Map
     * @param keyMapper 键映射函数
     * @param <K> 源键类型
     * @param <V> 值类型
     * @param <NK> 新键类型
     * @return 映射后的 Map
     */
    public static <K, V, NK> Map<NK, V> mapKeys(Map<K, V> map, Function<K, NK> keyMapper) {
        Objects.requireNonNull(keyMapper, "keyMapper must not be null");
        Map<NK, V> result = new LinkedHashMap<>();
        if (map != null) {
            for (Map.Entry<K, V> entry : map.entrySet()) {
                result.put(keyMapper.apply(entry.getKey()), entry.getValue());
            }
        }
        return result;
    }

    /**
     * 转换值并保留原始键。
     *
     * @param map 源 Map
     * @param valueMapper 值映射函数
     * @param <K> 键类型
     * @param <V> 源值类型
     * @param <NV> 新值类型
     * @return 映射后的 Map
     */
    public static <K, V, NV> Map<K, NV> mapValues(Map<K, V> map, Function<V, NV> valueMapper) {
        Objects.requireNonNull(valueMapper, "valueMapper must not be null");
        Map<K, NV> result = new LinkedHashMap<>();
        if (map != null) {
            for (Map.Entry<K, V> entry : map.entrySet()) {
                result.put(entry.getKey(), valueMapper.apply(entry.getValue()));
            }
        }
        return result;
    }

    /**
     * 将条目映射为新的条目。
     *
     * @param map 源 Map
     * @param mapper 条目映射函数
     * @param <K> 源键类型
     * @param <V> 源值类型
     * @param <NK> 新键类型
     * @param <NV> 新值类型
     * @return 映射后的 Map
     */
    public static <K, V, NK, NV> Map<NK, NV> mapEntries(Map<K, V> map, Function<Map.Entry<K, V>, Map.Entry<NK, NV>> mapper) {
        Objects.requireNonNull(mapper, "mapper must not be null");
        Map<NK, NV> result = new LinkedHashMap<>();
        if (map != null) {
            for (Map.Entry<K, V> entry : map.entrySet()) {
                Map.Entry<NK, NV> mapped = mapper.apply(entry);
                if (mapped != null) {
                    result.put(mapped.getKey(), mapped.getValue());
                }
            }
        }
        return result;
    }

    /**
     * 将 Map 条目转换为列表。
     *
     * @param map 源 Map
     * @param <K> 键类型
     * @param <V> 值类型
     * @return 条目列表
     */
    public static <K, V> List<Map.Entry<K, V>> toList(Map<K, V> map) {
        return map == null ? new ArrayList<>() : new ArrayList<>(map.entrySet());
    }

    /**
     * 将 Map 键转换为列表。
     *
     * @param map 源 Map
     * @param <K> 键类型
     * @param <V> 值类型
     * @return 键列表
     */
    public static <K, V> List<K> toKeyList(Map<K, V> map) {
        return map == null ? new ArrayList<>() : new ArrayList<>(map.keySet());
    }

    /**
     * 将 Map 值转换为列表。
     *
     * @param map 源 Map
     * @param <K> 键类型
     * @param <V> 值类型
     * @return 值列表
     */
    public static <K, V> List<V> toValueList(Map<K, V> map) {
        return map == null ? new ArrayList<>() : new ArrayList<>(map.values());
    }

    /**
     * 将 Map 条目转换为集合。
     *
     * @param map 源 Map
     * @param <K> 键类型
     * @param <V> 值类型
     * @return 条目集合
     */
    public static <K, V> Set<Map.Entry<K, V>> toSet(Map<K, V> map) {
        return map == null ? new LinkedHashSet<>() : new LinkedHashSet<>(map.entrySet());
    }

    /**
     * 将键转换为 String，并将值保留为 Object。
     *
     * @param map 源 Map
     * @return Object 值 Map
     */
    public static Map<String, Object> toObjectMap(Map<?, ?> map) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (map != null) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                result.put(String.valueOf(entry.getKey()), entry.getValue());
            }
        }
        return result;
    }

    /**
     * 将键和值都转换为 String。
     *
     * @param map 源 Map
     * @return String 值 Map
     */
    public static Map<String, String> toStringMap(Map<?, ?> map) {
        Map<String, String> result = new LinkedHashMap<>();
        if (map != null) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                result.put(String.valueOf(entry.getKey()), entry.getValue() == null ? null : String.valueOf(entry.getValue()));
            }
        }
        return result;
    }

    /**
     * 将 Map 转换为 LinkedHashMap。
     *
     * @param map 源 Map
     * @param <K> 键类型
     * @param <V> 值类型
     * @return 有序 Map
     */
    public static <K, V> LinkedHashMap<K, V> toLinkedMap(Map<K, V> map) {
        return map == null ? new LinkedHashMap<>() : new LinkedHashMap<>(map);
    }

    /**
     * 将 Bean 转换为 Map。
     *
     * @param bean 源 Bean
     * @return Bean 字段 Map
     */
    public static Map<String, Object> beanToMap(Object bean) {
        return beanToMap(bean, false);
    }

    /**
     * 将 Bean 转换为 Map。
     *
     * @param bean 源 Bean
     * @param ignoreNull 是否忽略 null 字段值
     * @return Bean 字段 Map
     */
    public static Map<String, Object> beanToMap(Object bean, boolean ignoreNull) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (bean == null) {
            return result;
        }
        for (Field field : allFields(bean.getClass())) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            try {
                field.setAccessible(true);
                Object value = field.get(bean);
                if (!ignoreNull || value != null) {
                    result.put(field.getName(), value);
                }
            } catch (IllegalAccessException e) {
                throw new IllegalArgumentException("Unable to read field: " + field.getName(), e);
            }
        }
        return result;
    }

    /**
     * 将 Map 转换为 Bean 实例。
     *
     * @param map 源 Map
     * @param beanType Bean 类型
     * @param <T> Bean 类型
     * @return Bean 实例
     */
    public static <T> T mapToBean(Map<String, Object> map, Class<T> beanType) {
        Objects.requireNonNull(beanType, "beanType must not be null");
        T bean = instantiate(beanType);
        copyToBean(map, bean);
        return bean;
    }

    /**
     * 将 Map 转换为 Bean 实例，并忽略字段赋值失败。
     *
     * @param map 源 Map
     * @param beanType Bean 类型
     * @param <T> Bean 类型
     * @return Bean 实例
     */
    public static <T> T mapToBeanIgnoreError(Map<String, Object> map, Class<T> beanType) {
        Objects.requireNonNull(beanType, "beanType must not be null");
        T bean = instantiate(beanType);
        if (map == null) {
            return bean;
        }
        for (Field field : allFields(beanType)) {
            if (Modifier.isStatic(field.getModifiers()) || Modifier.isFinal(field.getModifiers()) || !map.containsKey(field.getName())) {
                continue;
            }
            try {
                field.setAccessible(true);
                Object value = map.get(field.getName());
                if (value != null || !field.getType().isPrimitive()) {
                    field.set(bean, convertValue(value, field.getType()));
                }
            } catch (RuntimeException | IllegalAccessException ignored) {
                // 当前 API 变体需要主动忽略非法字段值。
            }
        }
        return bean;
    }

    /**
     * 将 Map 值复制到已有 Bean。
     *
     * @param map 源 Map
     * @param bean 目标 Bean
     * @param <T> Bean 类型
     * @return 目标 Bean
     */
    public static <T> T copyToBean(Map<String, Object> map, T bean) {
        Objects.requireNonNull(bean, "bean must not be null");
        if (map == null) {
            return bean;
        }
        for (Field field : allFields(bean.getClass())) {
            if (Modifier.isStatic(field.getModifiers()) || Modifier.isFinal(field.getModifiers()) || !map.containsKey(field.getName())) {
                continue;
            }
            try {
                field.setAccessible(true);
                Object value = map.get(field.getName());
                if (value != null || !field.getType().isPrimitive()) {
                    field.set(bean, convertValue(value, field.getType()));
                }
            } catch (IllegalAccessException e) {
                throw new IllegalArgumentException("Unable to write field: " + field.getName(), e);
            }
        }
        return bean;
    }

    /**
     * 将 Bean 字段复制到目标 Map。
     *
     * @param bean 源 Bean
     * @param target 目标 Map
     * @return 目标 Map
     */
    public static Map<String, Object> copyFromBean(Object bean, Map<String, Object> target) {
        requireMap(target);
        target.putAll(beanToMap(bean));
        return target;
    }

    /**
     * 将 Bean 转换为字符串 Map。
     *
     * @param bean 源 Bean
     * @return String 值 Map
     */
    public static Map<String, String> beanToStringMap(Object bean) {
        return toStringMap(beanToMap(bean));
    }

    /**
     * 按键的自然顺序排序 Map。
     *
     * @param map 源 Map
     * @param <K> 可比较键类型
     * @param <V> 值类型
     * @return 排序后的有序 Map
     */
    public static <K extends Comparable<? super K>, V> Map<K, V> sortByKey(Map<K, V> map) {
        return sortByKey(map, Comparator.nullsFirst(Comparator.naturalOrder()));
    }

    /**
     * 使用比较器按键排序 Map。
     *
     * @param map 源 Map
     * @param comparator 键比较器
     * @param <K> 键类型
     * @param <V> 值类型
     * @return 排序后的有序 Map
     */
    public static <K, V> Map<K, V> sortByKey(Map<K, V> map, Comparator<K> comparator) {
        Objects.requireNonNull(comparator, "comparator must not be null");
        Map<K, V> result = new LinkedHashMap<>();
        if (map != null) {
            map.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey(comparator))
                    .forEach(entry -> result.put(entry.getKey(), entry.getValue()));
        }
        return result;
    }

    /**
     * 按值的自然顺序排序 Map。
     *
     * @param map 源 Map
     * @param <K> 键类型
     * @param <V> 可比较值类型
     * @return 排序后的有序 Map
     */
    public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
        return sortByValue(map, Comparator.nullsFirst(Comparator.naturalOrder()));
    }

    /**
     * 使用比较器按值排序 Map。
     *
     * @param map 源 Map
     * @param comparator 值比较器
     * @param <K> 键类型
     * @param <V> 值类型
     * @return 排序后的有序 Map
     */
    public static <K, V> Map<K, V> sortByValue(Map<K, V> map, Comparator<V> comparator) {
        Objects.requireNonNull(comparator, "comparator must not be null");
        Map<K, V> result = new LinkedHashMap<>();
        if (map != null) {
            map.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue(comparator))
                    .forEach(entry -> result.put(entry.getKey(), entry.getValue()));
        }
        return result;
    }

    /**
     * 将 Map 转换为 TreeMap。
     *
     * @param map 源 Map
     * @param <K> 可比较键类型
     * @param <V> 值类型
     * @return TreeMap
     */
    public static <K extends Comparable<? super K>, V> TreeMap<K, V> toTreeMap(Map<K, V> map) {
        TreeMap<K, V> result = new TreeMap<>();
        if (map != null) {
            result.putAll(map);
        }
        return result;
    }

    /**
     * 将 Map 转换为 LinkedHashMap。
     *
     * @param map 源 Map
     * @param <K> 键类型
     * @param <V> 值类型
     * @return LinkedHashMap
     */
    public static <K, V> LinkedHashMap<K, V> toLinkedHashMap(Map<K, V> map) {
        return toLinkedMap(map);
    }

    /**
     * 反转当前迭代顺序。
     *
     * @param map 源 Map
     * @param <K> 键类型
     * @param <V> 值类型
     * @return 反转后的有序 Map
     */
    public static <K, V> Map<K, V> reverse(Map<K, V> map) {
        Map<K, V> result = new LinkedHashMap<>();
        if (map == null) {
            return result;
        }
        ArrayDeque<Map.Entry<K, V>> stack = new ArrayDeque<>(map.entrySet());
        while (!stack.isEmpty()) {
            Map.Entry<K, V> entry = stack.removeLast();
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }

    /**
     * 根据集合构建索引 Map，后面的元素覆盖前面的元素。
     *
     * @param collection 源集合
     * @param keyMapper 键映射函数
     * @param <T> 元素类型
     * @param <K> 键类型
     * @return 索引 Map
     */
    public static <T, K> Map<K, T> indexBy(Collection<T> collection, Function<T, K> keyMapper) {
        return indexBy(collection, keyMapper, (oldValue, newValue) -> newValue);
    }

    /**
     * 根据集合和合并函数构建索引 Map。
     *
     * @param collection 源集合
     * @param keyMapper 键映射函数
     * @param mergeFunction 重复键的合并函数
     * @param <T> 元素类型
     * @param <K> 键类型
     * @return 索引 Map
     */
    public static <T, K> Map<K, T> indexBy(Collection<T> collection, Function<T, K> keyMapper, BinaryOperator<T> mergeFunction) {
        Objects.requireNonNull(keyMapper, "keyMapper must not be null");
        Objects.requireNonNull(mergeFunction, "mergeFunction must not be null");
        Map<K, T> result = new LinkedHashMap<>();
        if (collection != null) {
            for (T item : collection) {
                K key = keyMapper.apply(item);
                if (result.containsKey(key)) {
                    result.put(key, mergeFunction.apply(result.get(key), item));
                } else {
                    result.put(key, item);
                }
            }
        }
        return result;
    }

    /**
     * 按键对集合元素分组。
     *
     * @param collection 源集合
     * @param keyMapper 键映射函数
     * @param <T> 元素类型
     * @param <K> 键类型
     * @return 分组 Map
     */
    public static <T, K> Map<K, List<T>> groupBy(Collection<T> collection, Function<T, K> keyMapper) {
        Objects.requireNonNull(keyMapper, "keyMapper must not be null");
        Map<K, List<T>> result = new LinkedHashMap<>();
        if (collection != null) {
            for (T item : collection) {
                result.computeIfAbsent(keyMapper.apply(item), key -> new ArrayList<>()).add(item);
            }
        }
        return result;
    }

    /**
     * 按键统计集合元素数量。
     *
     * @param collection 源集合
     * @param keyMapper 键映射函数
     * @param <T> 元素类型
     * @param <K> 键类型
     * @return 计数 Map
     */
    public static <T, K> Map<K, Long> groupCount(Collection<T> collection, Function<T, K> keyMapper) {
        Objects.requireNonNull(keyMapper, "keyMapper must not be null");
        Map<K, Long> result = new LinkedHashMap<>();
        if (collection != null) {
            for (T item : collection) {
                result.merge(keyMapper.apply(item), 1L, Long::sum);
            }
        }
        return result;
    }

    /**
     * 按键汇总 BigDecimal 值。
     *
     * @param collection 源集合
     * @param keyMapper 键映射函数
     * @param valueMapper 值映射函数
     * @param <T> 元素类型
     * @param <K> 键类型
     * @return 汇总 Map
     */
    public static <T, K> Map<K, BigDecimal> groupSum(Collection<T> collection, Function<T, K> keyMapper, Function<T, BigDecimal> valueMapper) {
        Objects.requireNonNull(keyMapper, "keyMapper must not be null");
        Objects.requireNonNull(valueMapper, "valueMapper must not be null");
        Map<K, BigDecimal> result = new LinkedHashMap<>();
        if (collection != null) {
            for (T item : collection) {
                BigDecimal value = valueMapper.apply(item);
                result.merge(keyMapper.apply(item), value == null ? BigDecimal.ZERO : value, BigDecimal::add);
            }
        }
        return result;
    }

    /**
     * 将集合转换为 Map，后面的元素覆盖前面的元素。
     *
     * @param collection 源集合
     * @param keyMapper 键映射函数
     * @param valueMapper 值映射函数
     * @param <T> 元素类型
     * @param <K> 键类型
     * @param <V> 值类型
     * @return 映射后的 Map
     */
    public static <T, K, V> Map<K, V> toMap(Collection<T> collection, Function<T, K> keyMapper, Function<T, V> valueMapper) {
        Objects.requireNonNull(keyMapper, "keyMapper must not be null");
        Objects.requireNonNull(valueMapper, "valueMapper must not be null");
        Map<K, V> result = new HashMap<>();
        if (collection != null) {
            for (T item : collection) {
                result.put(keyMapper.apply(item), valueMapper.apply(item));
            }
        }
        return result;
    }

    /**
     * 将集合转换为 LinkedHashMap，后面的元素覆盖前面的元素。
     *
     * @param collection 源集合
     * @param keyMapper 键映射函数
     * @param valueMapper 值映射函数
     * @param <T> 元素类型
     * @param <K> 键类型
     * @param <V> 值类型
     * @return 映射后的有序 Map
     */
    public static <T, K, V> Map<K, V> toLinkedMap(Collection<T> collection, Function<T, K> keyMapper, Function<T, V> valueMapper) {
        Objects.requireNonNull(keyMapper, "keyMapper must not be null");
        Objects.requireNonNull(valueMapper, "valueMapper must not be null");
        Map<K, V> result = new LinkedHashMap<>();
        if (collection != null) {
            for (T item : collection) {
                result.put(keyMapper.apply(item), valueMapper.apply(item));
            }
        }
        return result;
    }

    /**
     * 根据点号分隔路径写入值，并在需要时创建中间 Map。
     *
     * @param map 目标 Map
     * @param path 点号分隔路径
     * @param value 待写入的值
     * @return 目标 Map
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> putByPath(Map<String, Object> map, String path, Object value) {
        requireMap(map);
        List<String> parts = splitPath(path);
        if (parts.isEmpty()) {
            throw new IllegalArgumentException("path must not be blank");
        }
        Map<String, Object> current = map;
        for (int i = 0; i < parts.size() - 1; i++) {
            String part = parts.get(i);
            Object child = current.get(part);
            if (!(child instanceof Map<?, ?>)) {
                child = new LinkedHashMap<String, Object>();
                current.put(part, child);
            }
            current = (Map<String, Object>) child;
        }
        current.put(parts.get(parts.size() - 1), value);
        return map;
    }

    /**
     * 判断点号分隔路径是否存在。
     *
     * @param map 源 Map
     * @param path 点号分隔路径
     * @return 如果路径存在则返回 true
     */
    public static boolean containsPath(Map<String, Object> map, String path) {
        return containsPathInternal(map, path);
    }

    /**
     * 根据点号分隔路径移除值。
     *
     * @param map 目标 Map
     * @param path 点号分隔路径
     * @return 被移除的值；如果不存在则返回 null
     */
    @SuppressWarnings("unchecked")
    public static Object removeByPath(Map<String, Object> map, String path) {
        if (map == null || isBlank(path)) {
            return null;
        }
        List<String> parts = splitPath(path);
        Map<String, Object> current = map;
        for (int i = 0; i < parts.size() - 1; i++) {
            Object child = current.get(parts.get(i));
            if (!(child instanceof Map<?, ?> childMap)) {
                return null;
            }
            current = (Map<String, Object>) childMap;
        }
        return current.remove(parts.get(parts.size() - 1));
    }

    /**
     * 将嵌套 Map 扁平化为点号分隔键。
     *
     * @param map 源 Map
     * @return 扁平化后的 Map
     */
    public static Map<String, Object> flatten(Map<String, Object> map) {
        Map<String, Object> result = new LinkedHashMap<>();
        flattenInto(null, map, result);
        return result;
    }

    /**
     * 将点号分隔键的扁平 Map 还原为嵌套 Map。
     *
     * @param map 扁平化 Map
     * @return 嵌套 Map
     */
    public static Map<String, Object> unflatten(Map<String, Object> map) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (map != null) {
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                putByPath(result, entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    /**
     * 比较两个 Map 并返回差异模型。
     *
     * @param oldMap 旧 Map
     * @param newMap 新 Map
     * @param <K> 键类型
     * @param <V> 值类型
     * @return 差异模型
     */
    public static <K, V> MapDiff<K, V> diff(Map<K, V> oldMap, Map<K, V> newMap) {
        return new MapDiff<>(added(oldMap, newMap), removed(oldMap, newMap), changed(oldMap, newMap));
    }

    /**
     * 返回仅存在于新 Map 中的条目。
     *
     * @param oldMap 旧 Map
     * @param newMap 新 Map
     * @param <K> 键类型
     * @param <V> 值类型
     * @return 新增条目
     */
    public static <K, V> Map<K, V> added(Map<K, V> oldMap, Map<K, V> newMap) {
        Map<K, V> result = new LinkedHashMap<>();
        if (newMap != null) {
            for (Map.Entry<K, V> entry : newMap.entrySet()) {
                if (oldMap == null || !oldMap.containsKey(entry.getKey())) {
                    result.put(entry.getKey(), entry.getValue());
                }
            }
        }
        return result;
    }

    /**
     * 返回仅存在于旧 Map 中的条目。
     *
     * @param oldMap 旧 Map
     * @param newMap 新 Map
     * @param <K> 键类型
     * @param <V> 值类型
     * @return 删除条目
     */
    public static <K, V> Map<K, V> removed(Map<K, V> oldMap, Map<K, V> newMap) {
        Map<K, V> result = new LinkedHashMap<>();
        if (oldMap != null) {
            for (Map.Entry<K, V> entry : oldMap.entrySet()) {
                if (newMap == null || !newMap.containsKey(entry.getKey())) {
                    result.put(entry.getKey(), entry.getValue());
                }
            }
        }
        return result;
    }

    /**
     * 返回共同键中发生变化的值。
     *
     * @param oldMap 旧 Map
     * @param newMap 新 Map
     * @param <K> 键类型
     * @param <V> 值类型
     * @return 变更条目
     */
    public static <K, V> Map<K, ValueChange<V>> changed(Map<K, V> oldMap, Map<K, V> newMap) {
        Map<K, ValueChange<V>> result = new LinkedHashMap<>();
        if (oldMap == null || newMap == null) {
            return result;
        }
        for (Map.Entry<K, V> entry : oldMap.entrySet()) {
            K key = entry.getKey();
            if (newMap.containsKey(key) && !Objects.equals(entry.getValue(), newMap.get(key))) {
                result.put(key, new ValueChange<>(entry.getValue(), newMap.get(key)));
            }
        }
        return result;
    }

    /**
     * 判断两个 Map 是否相等。
     *
     * @param left 左侧 Map
     * @param right 右侧 Map
     * @return 如果两个 Map 相等则返回 true
     */
    public static boolean same(Map<?, ?> left, Map<?, ?> right) {
        return Objects.equals(left, right);
    }

    /**
     * 判断两个 Map 是否相等，不考虑迭代顺序。
     *
     * @param left 左侧 Map
     * @param right 右侧 Map
     * @return 如果两个 Map 包含相同条目则返回 true
     */
    public static boolean equalsIgnoreOrder(Map<?, ?> left, Map<?, ?> right) {
        return Objects.equals(left, right);
    }

    /**
     * 比较两个 Map 中指定键的值。
     *
     * @param oldMap 旧 Map
     * @param newMap 新 Map
     * @param key 待比较的键
     * @param <K> 键类型
     * @param <V> 值类型
     * @return 当键状态或值不同时返回值变化对象
     */
    public static <K, V> Optional<ValueChange<V>> compareValue(Map<K, V> oldMap, Map<K, V> newMap, K key) {
        boolean oldContains = oldMap != null && oldMap.containsKey(key);
        boolean newContains = newMap != null && newMap.containsKey(key);
        V oldValue = oldContains ? oldMap.get(key) : null;
        V newValue = newContains ? newMap.get(key) : null;
        if (oldContains != newContains || !Objects.equals(oldValue, newValue)) {
            return Optional.of(new ValueChange<>(oldValue, newValue));
        }
        return Optional.empty();
    }

    /**
     * 将 Map 转换为未进行 URL 编码的查询字符串。
     *
     * @param map 源 Map
     * @return 查询字符串
     */
    public static String toQueryString(Map<String, ?> map) {
        return toQueryString(map, false);
    }

    /**
     * 将 Map 转换为 URL 查询字符串。
     *
     * @param map 源 Map
     * @param encode 是否对键和值进行 URL 编码
     * @return 查询字符串
     */
    public static String toQueryString(Map<String, ?> map, boolean encode) {
        if (map == null || map.isEmpty()) {
            return "";
        }
        List<String> parts = new ArrayList<>();
        for (Map.Entry<String, ?> entry : map.entrySet()) {
            String key = encode ? urlEncode(entry.getKey()) : entry.getKey();
            String value = entry.getValue() == null ? "" : String.valueOf(entry.getValue());
            parts.add(key + "=" + (encode ? urlEncode(value) : value));
        }
        return String.join("&", parts);
    }

    /**
     * 将 Map 按键排序后转换为 URL 查询字符串。
     *
     * @param map 源 Map
     * @return 排序后的查询字符串
     */
    public static String toSortedQueryString(Map<String, ?> map) {
        if (map == null) {
            return "";
        }
        return toQueryString(new TreeMap<>(map), false);
    }

    /**
     * 使用自定义分隔符拼接条目。
     *
     * @param map 源 Map
     * @param entrySeparator 条目之间的分隔符
     * @param keyValueSeparator 键和值之间的分隔符
     * @return 拼接后的字符串
     */
    public static String join(Map<?, ?> map, String entrySeparator, String keyValueSeparator) {
        if (map == null || map.isEmpty()) {
            return "";
        }
        String entrySep = entrySeparator == null ? "" : entrySeparator;
        String keyValueSep = keyValueSeparator == null ? "" : keyValueSeparator;
        List<String> parts = new ArrayList<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            parts.add(String.valueOf(entry.getKey()) + keyValueSep + (entry.getValue() == null ? "" : entry.getValue()));
        }
        return String.join(entrySep, parts);
    }

    /**
     * 使用自定义分隔符拼接条目，并跳过 null 值。
     *
     * @param map 源 Map
     * @param entrySeparator 条目之间的分隔符
     * @param keyValueSeparator 键和值之间的分隔符
     * @return 拼接后的字符串
     */
    public static String joinIgnoreNull(Map<?, ?> map, String entrySeparator, String keyValueSeparator) {
        return join(filterValues(map, Objects::nonNull), entrySeparator, keyValueSeparator);
    }

    /**
     * 将 Map 按键排序并忽略 null 值后转换为签名字符串。
     *
     * @param map 源 Map
     * @return 签名字符串
     */
    public static String toSignString(Map<String, ?> map) {
        if (map == null) {
            return "";
        }
        return joinIgnoreNull(new TreeMap<>(map), "&", "=");
    }

    /**
     * 将 Map 转换为适合日志输出的字符串。
     *
     * @param map 源 Map
     * @return 日志字符串
     */
    public static String toLogString(Map<?, ?> map) {
        return map == null ? "{}" : map.toString();
    }

    /**
     * 将 URL 查询字符串解析为 Map。
     *
     * @param queryString 查询字符串
     * @return 解析后的 Map
     */
    public static Map<String, String> parseQueryString(String queryString) {
        Map<String, String> result = new LinkedHashMap<>();
        if (isBlank(queryString)) {
            return result;
        }
        String query = queryString.startsWith("?") ? queryString.substring(1) : queryString;
        if (query.isEmpty()) {
            return result;
        }
        for (String part : query.split("&")) {
            if (part.isEmpty()) {
                continue;
            }
            int index = part.indexOf('=');
            String key = index >= 0 ? part.substring(0, index) : part;
            String value = index >= 0 ? part.substring(index + 1) : "";
            result.put(urlDecode(key), urlDecode(value));
        }
        return result;
    }

    /**
     * 使用默认脱敏规则对指定键进行脱敏。
     *
     * @param map 源 Map
     * @param keys 待脱敏的键集合
     * @return 包含脱敏值的 Map 副本
     */
    public static Map<String, Object> mask(Map<String, Object> map, Collection<String> keys) {
        Map<String, Object> result = map == null ? new LinkedHashMap<>() : new LinkedHashMap<>(map);
        if (keys != null) {
            for (String key : keys) {
                if (result.containsKey(key)) {
                    result.put(key, defaultMask(result.get(key)));
                }
            }
        }
        return result;
    }

    /**
     * 使用自定义脱敏规则处理值。
     *
     * @param map 源 Map
     * @param maskRules 键与脱敏函数规则
     * @return 包含脱敏值的 Map 副本
     */
    public static Map<String, Object> mask(Map<String, Object> map, Map<String, Function<Object, Object>> maskRules) {
        Map<String, Object> result = map == null ? new LinkedHashMap<>() : new LinkedHashMap<>(map);
        if (maskRules != null) {
            for (Map.Entry<String, Function<Object, Object>> rule : maskRules.entrySet()) {
                if (result.containsKey(rule.getKey()) && rule.getValue() != null) {
                    result.put(rule.getKey(), rule.getValue().apply(result.get(rule.getKey())));
                }
            }
        }
        return result;
    }

    /**
     * 返回一个不包含敏感键的副本。
     *
     * @param map 源 Map
     * @param keys 待移除的键集合
     * @return 不包含敏感键的 Map 副本
     */
    public static Map<String, Object> removeSensitiveKeys(Map<String, Object> map, Collection<String> keys) {
        return copyWithoutSensitiveKeys(map, keys);
    }

    /**
     * 返回一个不包含敏感键的副本。
     *
     * @param map 源 Map
     * @param keys 待移除的键集合
     * @return 不包含敏感键的 Map 副本
     */
    public static Map<String, Object> copyWithoutSensitiveKeys(Map<String, Object> map, Collection<String> keys) {
        Map<String, Object> result = map == null ? new LinkedHashMap<>() : new LinkedHashMap<>(map);
        if (keys != null) {
            for (String key : keys) {
                result.remove(key);
            }
        }
        return result;
    }

    /**
     * 对指定键对应的手机号值进行脱敏。
     *
     * @param map 源 Map
     * @param key 待脱敏的键
     * @return 包含脱敏手机号值的 Map 副本
     */
    public static Map<String, Object> maskPhone(Map<String, Object> map, String key) {
        return mask(map, Map.of(key, MapUtil::maskPhoneValue));
    }

    /**
     * 对指定键对应的邮箱值进行脱敏。
     *
     * @param map 源 Map
     * @param key 待脱敏的键
     * @return 包含脱敏邮箱值的 Map 副本
     */
    public static Map<String, Object> maskEmail(Map<String, Object> map, String key) {
        return mask(map, Map.of(key, MapUtil::maskEmailValue));
    }

    /**
     * 对指定键对应的身份证号值进行脱敏。
     *
     * @param map 源 Map
     * @param key 待脱敏的键
     * @return 包含脱敏身份证号值的 Map 副本
     */
    public static Map<String, Object> maskIdCard(Map<String, Object> map, String key) {
        return mask(map, Map.of(key, MapUtil::maskIdCardValue));
    }

    /**
     * 要求 Map 不能为空。
     *
     * @param map 待校验的 Map
     * @param message 异常消息
     * @param <K> 键类型
     * @param <V> 值类型
     * @return 原始 Map
     */
    public static <K, V> Map<K, V> requireNotEmpty(Map<K, V> map, String message) {
        if (isEmpty(map)) {
            throw new IllegalArgumentException(message == null ? "map must not be empty" : message);
        }
        return map;
    }

    /**
     * 要求指定键必须存在。
     *
     * @param map 待校验的 Map
     * @param key 必须存在的键
     * @param message 异常消息
     */
    public static void requireKey(Map<?, ?> map, Object key, String message) {
        if (map == null || !map.containsKey(key)) {
            throw new IllegalArgumentException(message == null ? "required key is missing: " + key : message);
        }
    }

    /**
     * 要求所有指定键都必须存在。
     *
     * @param map 待校验的 Map
     * @param keys 必须存在的键集合
     * @param message 异常消息
     */
    public static void requireKeys(Map<?, ?> map, Collection<?> keys, String message) {
        List<?> missing = missingKeys(map, keys);
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException(message == null ? "required keys are missing: " + missing : message);
        }
    }

    /**
     * 要求指定键对应的值不能为 null。
     *
     * @param map 待校验的 Map
     * @param key 必须存在的键
     * @param message 异常消息
     */
    public static void requireValue(Map<?, ?> map, Object key, String message) {
        if (map == null || !map.containsKey(key) || map.get(key) == null) {
            throw new IllegalArgumentException(message == null ? "required value is missing: " + key : message);
        }
    }

    /**
     * 返回缺失的键。
     *
     * @param map 待校验的 Map
     * @param keys 必须存在的键集合
     * @return 缺失键列表
     */
    public static List<Object> missingKeys(Map<?, ?> map, Collection<?> keys) {
        List<Object> result = new ArrayList<>();
        if (keys == null) {
            return result;
        }
        for (Object key : keys) {
            if (map == null || !map.containsKey(key)) {
                result.add(key);
            }
        }
        return result;
    }

    /**
     * 判断所有指定键是否都存在。
     *
     * @param map 待校验的 Map
     * @param keys 必须存在的键集合
     * @return 如果所有键都存在则返回 true
     */
    public static boolean hasAllKeys(Map<?, ?> map, Collection<?> keys) {
        return missingKeys(map, keys).isEmpty();
    }

    /**
     * 判断任意指定键是否存在。
     *
     * @param map 待校验的 Map
     * @param keys 候选键集合
     * @return 如果任意键存在则返回 true
     */
    public static boolean hasAnyKey(Map<?, ?> map, Collection<?> keys) {
        if (map == null || keys == null) {
            return false;
        }
        for (Object key : keys) {
            if (map.containsKey(key)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 有序 Map 构建器。
     *
     * @param <K> 键类型
     * @param <V> 值类型
     */
    public static final class Builder<K, V> {
        private final LinkedHashMap<K, V> map = new LinkedHashMap<>();

        private Builder() {
        }

        /**
         * 添加一个条目。
         *
         * @param key 键
         * @param value 值
         * @return 当前构建器
         */
        public Builder<K, V> put(K key, V value) {
            map.put(key, value);
            return this;
        }

        /**
         * 当值非 null 时添加条目。
         *
         * @param key 键
         * @param value 值
         * @return 当前构建器
         */
        public Builder<K, V> putIfNotNull(K key, V value) {
            if (value != null) {
                map.put(key, value);
            }
            return this;
        }

        /**
         * 添加所有条目。
         *
         * @param source 源 Map
         * @return 当前构建器
         */
        public Builder<K, V> putAll(Map<K, V> source) {
            if (source != null) {
                map.putAll(source);
            }
            return this;
        }

        /**
         * 构建不可变 Map。
         *
         * @return 不可变 Map
         */
        public Map<K, V> build() {
            return Collections.unmodifiableMap(new LinkedHashMap<>(map));
        }

        /**
         * 构建可变有序 Map。
         *
         * @return 可变有序 Map
         */
        public LinkedHashMap<K, V> buildMutable() {
            return new LinkedHashMap<>(map);
        }

        /**
         * 构建不可变 Map。
         *
         * @return 不可变 Map
         */
        public Map<K, V> buildImmutable() {
            return build();
        }
    }

    /**
     * 值变化模型。
     *
     * @param <V> 值类型
     */
    public static final class ValueChange<V> {
        private final V oldValue;
        private final V newValue;

        private ValueChange(V oldValue, V newValue) {
            this.oldValue = oldValue;
            this.newValue = newValue;
        }

        /**
         * 返回旧值。
         *
         * @return 旧值
         */
        public V getOldValue() {
            return oldValue;
        }

        /**
         * 返回新值。
         *
         * @return 新值
         */
        public V getNewValue() {
            return newValue;
        }
    }

    /**
     * Map 差异模型。
     *
     * @param <K> 键类型
     * @param <V> 值类型
     */
    public static final class MapDiff<K, V> {
        private final Map<K, V> added;
        private final Map<K, V> removed;
        private final Map<K, ValueChange<V>> changed;

        private MapDiff(Map<K, V> added, Map<K, V> removed, Map<K, ValueChange<V>> changed) {
            this.added = Collections.unmodifiableMap(new LinkedHashMap<>(added));
            this.removed = Collections.unmodifiableMap(new LinkedHashMap<>(removed));
            this.changed = Collections.unmodifiableMap(new LinkedHashMap<>(changed));
        }

        /**
         * 返回新增条目。
         *
         * @return 新增条目
         */
        public Map<K, V> getAdded() {
            return added;
        }

        /**
         * 返回删除条目。
         *
         * @return 删除条目
         */
        public Map<K, V> getRemoved() {
            return removed;
        }

        /**
         * 返回变更条目。
         *
         * @return 变更条目
         */
        public Map<K, ValueChange<V>> getChanged() {
            return changed;
        }
    }

    private static void validateKeyValues(Object... keyValues) {
        if (keyValues == null) {
            throw new IllegalArgumentException("keyValues must not be null");
        }
        if (keyValues.length % 2 != 0) {
            throw new IllegalArgumentException("keyValues length must be even");
        }
    }

    private static Object rawGet(Map<?, ?> map, Object key) {
        return map == null ? null : map.get(key);
    }

    private static void requireMap(Map<?, ?> map) {
        Objects.requireNonNull(map, "map must not be null");
    }

    private static boolean isBlank(CharSequence text) {
        return text == null || text.toString().trim().isEmpty();
    }

    private static List<String> splitPath(String path) {
        if (isBlank(path)) {
            return new ArrayList<>();
        }
        String[] parts = path.split("\\.");
        List<String> result = new ArrayList<>();
        for (String part : parts) {
            if (isBlank(part)) {
                throw new IllegalArgumentException("path contains blank segment: " + path);
            }
            result.add(part);
        }
        return result;
    }

    private static Integer convertToInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.valueOf(value.toString().trim());
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Cannot convert value to Integer: " + value, e);
        }
    }

    private static Long convertToLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.valueOf(value.toString().trim());
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Cannot convert value to Long: " + value, e);
        }
    }

    private static Double convertToDouble(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return Double.valueOf(value.toString().trim());
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Cannot convert value to Double: " + value, e);
        }
    }

    private static BigDecimal convertToBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }
        if (value instanceof Number || value instanceof CharSequence) {
            try {
                return new BigDecimal(value.toString().trim());
            } catch (RuntimeException e) {
                throw new IllegalArgumentException("Cannot convert value to BigDecimal: " + value, e);
            }
        }
        throw new IllegalArgumentException("Cannot convert value to BigDecimal: " + value);
    }

    private static Boolean convertToBoolean(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        String text = value.toString().trim().toLowerCase();
        return switch (text) {
            case "true", "1", "yes", "y", "on" -> Boolean.TRUE;
            case "false", "0", "no", "n", "off" -> Boolean.FALSE;
            default -> throw new IllegalArgumentException("Cannot convert value to Boolean: " + value);
        };
    }

    private static Date convertToDate(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Date date) {
            return date;
        }
        if (value instanceof Number number) {
            return new Date(number.longValue());
        }
        if (value instanceof Instant instant) {
            return Date.from(instant);
        }
        if (value instanceof LocalDateTime localDateTime) {
            return Date.from(localDateTime.atZone(SYSTEM_ZONE).toInstant());
        }
        if (value instanceof LocalDate localDate) {
            return Date.from(localDate.atStartOfDay(SYSTEM_ZONE).toInstant());
        }
        String text = value.toString().trim();
        try {
            return Date.from(Instant.parse(text));
        } catch (DateTimeParseException ignored) {
            // 尝试下面的常用本地日期时间格式。
        }
        for (String pattern : List.of("yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm", "yyyy-MM-dd")) {
            try {
                return new SimpleDateFormat(pattern).parse(text);
            } catch (ParseException ignored) {
                // 继续尝试下一个格式。
            }
        }
        throw new IllegalArgumentException("Cannot convert value to Date: " + value);
    }

    private static LocalDate convertToLocalDate(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime.toLocalDate();
        }
        if (value instanceof Date date) {
            return date.toInstant().atZone(SYSTEM_ZONE).toLocalDate();
        }
        String text = value.toString().trim();
        try {
            return LocalDate.parse(text);
        } catch (DateTimeParseException ignored) {
            // 尝试下面的本地日期时间格式。
        }
        return convertToLocalDateTime(value).toLocalDate();
    }

    private static LocalDateTime convertToLocalDateTime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }
        if (value instanceof LocalDate localDate) {
            return localDate.atStartOfDay();
        }
        if (value instanceof Date date) {
            return date.toInstant().atZone(SYSTEM_ZONE).toLocalDateTime();
        }
        String text = value.toString().trim();
        try {
            return LocalDateTime.parse(text);
        } catch (DateTimeParseException ignored) {
            // 尝试下面的常用本地日期时间格式。
        }
        try {
            return LocalDateTime.parse(text, DATE_TIME_SPACE_FORMATTER);
        } catch (DateTimeParseException ignored) {
            // 继续尝试分钟精度格式。
        }
        try {
            return LocalDateTime.parse(text, DATE_TIME_MINUTE_FORMATTER);
        } catch (DateTimeParseException ignored) {
            // 继续尝试仅日期格式。
        }
        try {
            return LocalDate.parse(text).atStartOfDay();
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Cannot convert value to LocalDateTime: " + value, e);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <E extends Enum<E>> E convertToEnum(Object value, Class<E> enumType) {
        if (value == null) {
            return null;
        }
        if (enumType.isInstance(value)) {
            return enumType.cast(value);
        }
        if (value instanceof Number number) {
            E[] constants = enumType.getEnumConstants();
            int ordinal = number.intValue();
            if (ordinal >= 0 && ordinal < constants.length) {
                return constants[ordinal];
            }
            throw new IllegalArgumentException("Enum ordinal is out of range: " + value);
        }
        return (E) Enum.valueOf((Class) enumType, value.toString().trim());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> T convertValue(Object value, Class<T> targetType) {
        if (value == null) {
            return null;
        }
        Class<?> wrappedType = wrapPrimitive(targetType);
        if (wrappedType.isInstance(value)) {
            return (T) value;
        }
        if (wrappedType == String.class) {
            return (T) String.valueOf(value);
        }
        if (wrappedType == Integer.class) {
            return (T) convertToInteger(value);
        }
        if (wrappedType == Long.class) {
            return (T) convertToLong(value);
        }
        if (wrappedType == Double.class) {
            return (T) convertToDouble(value);
        }
        if (wrappedType == BigDecimal.class) {
            return (T) convertToBigDecimal(value);
        }
        if (wrappedType == Boolean.class) {
            return (T) convertToBoolean(value);
        }
        if (wrappedType == LocalDate.class) {
            return (T) convertToLocalDate(value);
        }
        if (wrappedType == LocalDateTime.class) {
            return (T) convertToLocalDateTime(value);
        }
        if (wrappedType == Date.class) {
            return (T) convertToDate(value);
        }
        if (wrappedType.isEnum()) {
            return (T) convertToEnum(value, (Class<? extends Enum>) wrappedType);
        }
        if (value instanceof Map<?, ?> valueMap) {
            return mapToBean(toObjectMap(valueMap), targetType);
        }
        throw new IllegalArgumentException("Unsupported conversion from " + value.getClass().getName() + " to " + targetType.getName());
    }

    private static Class<?> wrapPrimitive(Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        }
        if (type == int.class) {
            return Integer.class;
        }
        if (type == long.class) {
            return Long.class;
        }
        if (type == double.class) {
            return Double.class;
        }
        if (type == boolean.class) {
            return Boolean.class;
        }
        if (type == float.class) {
            return Float.class;
        }
        if (type == short.class) {
            return Short.class;
        }
        if (type == byte.class) {
            return Byte.class;
        }
        if (type == char.class) {
            return Character.class;
        }
        return type;
    }

    private static List<Field> allFields(Class<?> type) {
        List<Field> fields = new ArrayList<>();
        Class<?> current = type;
        while (current != null && current != Object.class) {
            Collections.addAll(fields, current.getDeclaredFields());
            current = current.getSuperclass();
        }
        return fields;
    }

    private static <T> T instantiate(Class<T> beanType) {
        try {
            Constructor<T> constructor = beanType.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalArgumentException("Bean type must have a no-args constructor: " + beanType.getName(), e);
        }
    }

    private static boolean containsPathInternal(Map<String, Object> map, String path) {
        if (map == null || isBlank(path)) {
            return false;
        }
        Object current = map;
        for (String part : splitPath(path)) {
            if (!(current instanceof Map<?, ?> currentMap) || !currentMap.containsKey(part)) {
                return false;
            }
            current = currentMap.get(part);
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    private static void flattenInto(String prefix, Map<String, Object> source, Map<String, Object> target) {
        if (source == null) {
            return;
        }
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            String key = prefix == null ? entry.getKey() : prefix + "." + entry.getKey();
            Object value = entry.getValue();
            if (value instanceof Map<?, ?> valueMap) {
                flattenInto(key, (Map<String, Object>) valueMap, target);
            } else {
                target.put(key, value);
            }
        }
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private static String urlDecode(String value) {
        return URLDecoder.decode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private static Object defaultMask(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value);
        if (text.isEmpty()) {
            return text;
        }
        if (text.length() <= 2) {
            return "*".repeat(text.length());
        }
        return text.charAt(0) + "*".repeat(Math.max(1, text.length() - 2)) + text.charAt(text.length() - 1);
    }

    private static Object maskPhoneValue(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value);
        if (text.length() < 7) {
            return defaultMask(text);
        }
        return text.substring(0, 3) + "****" + text.substring(text.length() - 4);
    }

    private static Object maskEmailValue(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value);
        int at = text.indexOf('@');
        if (at <= 0) {
            return defaultMask(text);
        }
        return defaultMask(text.substring(0, at)) + text.substring(at);
    }

    private static Object maskIdCardValue(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value);
        if (text.length() <= 10) {
            return defaultMask(text);
        }
        return text.substring(0, 6) + "********" + text.substring(text.length() - 4);
    }
}
