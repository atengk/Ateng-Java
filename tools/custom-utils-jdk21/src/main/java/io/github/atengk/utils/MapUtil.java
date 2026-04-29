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
 * General purpose Map utilities implemented with JDK native APIs only.
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
     * Returns whether the map is null or empty.
     *
     * @param map map to check
     * @return true if the map is null or empty
     */
    public static boolean isEmpty(Map<?, ?> map) {
        return map == null || map.isEmpty();
    }

    /**
     * Returns whether the map is not null and not empty.
     *
     * @param map map to check
     * @return true if the map has at least one entry
     */
    public static boolean isNotEmpty(Map<?, ?> map) {
        return !isEmpty(map);
    }

    /**
     * Returns whether the map is null or empty.
     *
     * @param map map to check
     * @return true if the map is null or empty
     */
    public static boolean isNullOrEmpty(Map<?, ?> map) {
        return isEmpty(map);
    }

    /**
     * Safely returns the map size.
     *
     * @param map map to inspect
     * @return map size, or 0 if map is null
     */
    public static int size(Map<?, ?> map) {
        return map == null ? 0 : map.size();
    }

    /**
     * Safely checks whether the map contains the key.
     *
     * @param map map to inspect
     * @param key key to find
     * @return true if the map contains the key
     */
    public static boolean containsKey(Map<?, ?> map, Object key) {
        return map != null && map.containsKey(key);
    }

    /**
     * Safely checks whether the map contains the value.
     *
     * @param map map to inspect
     * @param value value to find
     * @return true if the map contains the value
     */
    public static boolean containsValue(Map<?, ?> map, Object value) {
        return map != null && map.containsValue(value);
    }

    /**
     * Safely checks whether the map has the key.
     *
     * @param map map to inspect
     * @param key key to find
     * @return true if the map contains the key
     */
    public static boolean hasKey(Map<?, ?> map, Object key) {
        return containsKey(map, key);
    }

    /**
     * Safely checks whether the map has the value.
     *
     * @param map map to inspect
     * @param value value to find
     * @return true if the map contains the value
     */
    public static boolean hasValue(Map<?, ?> map, Object value) {
        return containsValue(map, value);
    }

    /**
     * Returns an empty map when the input map is null.
     *
     * @param map source map
     * @param <K> key type
     * @param <V> value type
     * @return original map or an immutable empty map
     */
    public static <K, V> Map<K, V> emptyIfNull(Map<K, V> map) {
        return map == null ? Collections.emptyMap() : map;
    }

    /**
     * Returns the default map when the source map is null.
     *
     * @param map source map
     * @param defaultMap default map
     * @param <K> key type
     * @param <V> value type
     * @return source map or default map
     */
    public static <K, V> Map<K, V> defaultIfNull(Map<K, V> map, Map<K, V> defaultMap) {
        return map == null ? defaultMap : map;
    }

    /**
     * Converts an empty map to null.
     *
     * @param map source map
     * @param <K> key type
     * @param <V> value type
     * @return null if the map is null or empty, otherwise the original map
     */
    public static <K, V> Map<K, V> nullIfEmpty(Map<K, V> map) {
        return isEmpty(map) ? null : map;
    }

    /**
     * Converts an empty map to null.
     *
     * @param map source map
     * @param <K> key type
     * @param <V> value type
     * @return null if the map is null or empty, otherwise the original map
     */
    public static <K, V> Map<K, V> emptyToNull(Map<K, V> map) {
        return nullIfEmpty(map);
    }

    /**
     * Returns a copy without entries whose value is null.
     *
     * @param map source map
     * @param <K> key type
     * @param <V> value type
     * @return new map without null values
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
     * Returns a copy without entries whose key is null.
     *
     * @param map source map
     * @param <K> key type
     * @param <V> value type
     * @return new map without null keys
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
     * Returns a copy without entries whose value is a blank CharSequence.
     *
     * @param map source map
     * @param <K> key type
     * @return new map without blank string values
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
     * Returns a copy and trims all CharSequence values.
     *
     * @param map source map
     * @param <K> key type
     * @return new map with trimmed string values
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
     * Creates a new HashMap.
     *
     * @param <K> key type
     * @param <V> value type
     * @return new HashMap
     */
    public static <K, V> HashMap<K, V> newHashMap() {
        return new HashMap<>();
    }

    /**
     * Creates a new HashMap with the initial capacity.
     *
     * @param initialCapacity initial capacity
     * @param <K> key type
     * @param <V> value type
     * @return new HashMap
     */
    public static <K, V> HashMap<K, V> newHashMap(int initialCapacity) {
        if (initialCapacity < 0) {
            throw new IllegalArgumentException("initialCapacity must be >= 0");
        }
        return new HashMap<>(initialCapacity);
    }

    /**
     * Creates a new LinkedHashMap.
     *
     * @param <K> key type
     * @param <V> value type
     * @return new LinkedHashMap
     */
    public static <K, V> LinkedHashMap<K, V> newLinkedHashMap() {
        return new LinkedHashMap<>();
    }

    /**
     * Creates a new LinkedHashMap with the initial capacity.
     *
     * @param initialCapacity initial capacity
     * @param <K> key type
     * @param <V> value type
     * @return new LinkedHashMap
     */
    public static <K, V> LinkedHashMap<K, V> newLinkedHashMap(int initialCapacity) {
        if (initialCapacity < 0) {
            throw new IllegalArgumentException("initialCapacity must be >= 0");
        }
        return new LinkedHashMap<>(initialCapacity);
    }

    /**
     * Creates a new ConcurrentHashMap.
     *
     * @param <K> key type
     * @param <V> value type
     * @return new ConcurrentHashMap
     */
    public static <K, V> ConcurrentHashMap<K, V> newConcurrentHashMap() {
        return new ConcurrentHashMap<>();
    }

    /**
     * Creates an immutable map with one entry.
     *
     * @param key key
     * @param value value
     * @param <K> key type
     * @param <V> value type
     * @return immutable map
     */
    public static <K, V> Map<K, V> of(K key, V value) {
        Map<K, V> result = new LinkedHashMap<>();
        result.put(key, value);
        return Collections.unmodifiableMap(result);
    }

    /**
     * Creates an immutable map with two entries.
     *
     * @param k1 first key
     * @param v1 first value
     * @param k2 second key
     * @param v2 second value
     * @param <K> key type
     * @param <V> value type
     * @return immutable map
     */
    public static <K, V> Map<K, V> of(K k1, V v1, K k2, V v2) {
        Map<K, V> result = new LinkedHashMap<>();
        result.put(k1, v1);
        result.put(k2, v2);
        return Collections.unmodifiableMap(result);
    }

    /**
     * Creates a mutable HashMap from key-value pairs.
     *
     * @param keyValues key-value pairs, length must be even
     * @param <K> key type
     * @param <V> value type
     * @return mutable HashMap
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
     * Creates a mutable LinkedHashMap from key-value pairs.
     *
     * @param keyValues key-value pairs, length must be even
     * @param <K> key type
     * @param <V> value type
     * @return mutable LinkedHashMap
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
     * Creates a linked map builder.
     *
     * @param <K> key type
     * @param <V> value type
     * @return map builder
     */
    public static <K, V> Builder<K, V> builder() {
        return new Builder<>();
    }

    /**
     * Safely reads a value from the map.
     *
     * @param map source map
     * @param key key to read
     * @param <K> key type
     * @param <V> value type
     * @return value or null
     */
    public static <K, V> V get(Map<K, V> map, K key) {
        return map == null ? null : map.get(key);
    }

    /**
     * Safely reads a value or returns the default value.
     *
     * @param map source map
     * @param key key to read
     * @param defaultValue default value
     * @param <K> key type
     * @param <V> value type
     * @return value or default value
     */
    public static <K, V> V getOrDefault(Map<K, V> map, K key, V defaultValue) {
        if (map == null) {
            return defaultValue;
        }
        V value = map.get(key);
        return value == null ? defaultValue : value;
    }

    /**
     * Safely reads a value or returns null.
     *
     * @param map source map
     * @param key key to read
     * @param <K> key type
     * @param <V> value type
     * @return value or null
     */
    public static <K, V> V getOrNull(Map<K, V> map, K key) {
        return get(map, key);
    }

    /**
     * Reads a required value.
     *
     * @param map source map
     * @param key key to read
     * @param <K> key type
     * @param <V> value type
     * @return non-null value
     */
    public static <K, V> V getRequired(Map<K, V> map, K key) {
        if (map == null || !map.containsKey(key) || map.get(key) == null) {
            throw new NoSuchElementException("Required map value is missing: " + key);
        }
        return map.get(key);
    }

    /**
     * Reads the first value whose key exists in the map.
     *
     * @param map source map
     * @param keys candidate keys
     * @param <K> key type
     * @param <V> value type
     * @return first matched value or null
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
     * Reads the first non-null value from candidate keys.
     *
     * @param map source map
     * @param keys candidate keys
     * @param <K> key type
     * @param <V> value type
     * @return first non-null value or null
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
     * Reads a nested value by dot-separated path.
     *
     * @param map source map
     * @param path dot-separated path
     * @return value or null
     */
    public static Object getByPath(Map<String, Object> map, String path) {
        return getByPath(map, path, null);
    }

    /**
     * Reads a nested value by dot-separated path or returns a default value.
     *
     * @param map source map
     * @param path dot-separated path
     * @param defaultValue default value
     * @return value or default value
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
     * Reads a String value.
     *
     * @param map source map
     * @param key key to read
     * @return String value or null
     */
    public static String getStr(Map<?, ?> map, Object key) {
        return getStr(map, key, null);
    }

    /**
     * Reads a String value or returns the default value.
     *
     * @param map source map
     * @param key key to read
     * @param defaultValue default value
     * @return String value or default value
     */
    public static String getStr(Map<?, ?> map, Object key, String defaultValue) {
        Object value = rawGet(map, key);
        return value == null ? defaultValue : String.valueOf(value);
    }

    /**
     * Reads an Integer value.
     *
     * @param map source map
     * @param key key to read
     * @return Integer value or null
     */
    public static Integer getInt(Map<?, ?> map, Object key) {
        return convertToInteger(rawGet(map, key));
    }

    /**
     * Reads a Long value.
     *
     * @param map source map
     * @param key key to read
     * @return Long value or null
     */
    public static Long getLong(Map<?, ?> map, Object key) {
        return convertToLong(rawGet(map, key));
    }

    /**
     * Reads a Double value.
     *
     * @param map source map
     * @param key key to read
     * @return Double value or null
     */
    public static Double getDouble(Map<?, ?> map, Object key) {
        return convertToDouble(rawGet(map, key));
    }

    /**
     * Reads a BigDecimal value.
     *
     * @param map source map
     * @param key key to read
     * @return BigDecimal value or null
     */
    public static BigDecimal getBigDecimal(Map<?, ?> map, Object key) {
        return convertToBigDecimal(rawGet(map, key));
    }

    /**
     * Reads a Boolean value.
     *
     * @param map source map
     * @param key key to read
     * @return Boolean value or null
     */
    public static Boolean getBool(Map<?, ?> map, Object key) {
        return convertToBoolean(rawGet(map, key));
    }

    /**
     * Reads a Date value.
     *
     * @param map source map
     * @param key key to read
     * @return Date value or null
     */
    public static Date getDate(Map<?, ?> map, Object key) {
        return convertToDate(rawGet(map, key));
    }

    /**
     * Reads a LocalDate value.
     *
     * @param map source map
     * @param key key to read
     * @return LocalDate value or null
     */
    public static LocalDate getLocalDate(Map<?, ?> map, Object key) {
        return convertToLocalDate(rawGet(map, key));
    }

    /**
     * Reads a LocalDateTime value.
     *
     * @param map source map
     * @param key key to read
     * @return LocalDateTime value or null
     */
    public static LocalDateTime getLocalDateTime(Map<?, ?> map, Object key) {
        return convertToLocalDateTime(rawGet(map, key));
    }

    /**
     * Reads a typed list value.
     *
     * @param map source map
     * @param key key to read
     * @param elementType element type
     * @param <T> element type
     * @return typed list, empty if value is null
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
     * Reads a nested map value and converts keys to String.
     *
     * @param map source map
     * @param key key to read
     * @return nested map, empty if value is null
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
     * Reads an enum value.
     *
     * @param map source map
     * @param key key to read
     * @param enumType enum class
     * @param <E> enum type
     * @return enum value or null
     */
    public static <E extends Enum<E>> E getEnum(Map<?, ?> map, Object key, Class<E> enumType) {
        Objects.requireNonNull(enumType, "enumType must not be null");
        return convertToEnum(rawGet(map, key), enumType);
    }

    /**
     * Puts a value when it is not null.
     *
     * @param map target map
     * @param key key to write
     * @param value value to write
     * @param <K> key type
     * @param <V> value type
     * @return target map
     */
    public static <K, V> Map<K, V> putIfNotNull(Map<K, V> map, K key, V value) {
        requireMap(map);
        if (value != null) {
            map.put(key, value);
        }
        return map;
    }

    /**
     * Puts a string value when it is not blank.
     *
     * @param map target map
     * @param key key to write
     * @param value value to write
     * @param <K> key type
     * @return target map
     */
    public static <K> Map<K, String> putIfNotBlank(Map<K, String> map, K key, String value) {
        requireMap(map);
        if (!isBlank(value)) {
            map.put(key, value);
        }
        return map;
    }

    /**
     * Puts a value when the key is absent.
     *
     * @param map target map
     * @param key key to write
     * @param value value to write
     * @param <K> key type
     * @param <V> value type
     * @return target map
     */
    public static <K, V> Map<K, V> putIfAbsent(Map<K, V> map, K key, V value) {
        requireMap(map);
        map.putIfAbsent(key, value);
        return map;
    }

    /**
     * Puts a value when the key already exists.
     *
     * @param map target map
     * @param key key to write
     * @param value value to write
     * @param <K> key type
     * @param <V> value type
     * @return target map
     */
    public static <K, V> Map<K, V> putIfPresent(Map<K, V> map, K key, V value) {
        requireMap(map);
        if (map.containsKey(key)) {
            map.put(key, value);
        }
        return map;
    }

    /**
     * Puts all entries when source is not null.
     *
     * @param target target map
     * @param source source map
     * @param <K> key type
     * @param <V> value type
     * @return target map
     */
    public static <K, V> Map<K, V> putAllIfNotNull(Map<K, V> target, Map<K, V> source) {
        requireMap(target);
        if (source != null) {
            target.putAll(source);
        }
        return target;
    }

    /**
     * Puts all entries and skips entries whose value is null.
     *
     * @param target target map
     * @param source source map
     * @param <K> key type
     * @param <V> value type
     * @return target map
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
     * Sets a default value when the current value is null.
     *
     * @param map target map
     * @param key key to write
     * @param defaultValue default value
     * @param <K> key type
     * @param <V> value type
     * @return target map
     */
    public static <K, V> Map<K, V> setDefault(Map<K, V> map, K key, V defaultValue) {
        requireMap(map);
        if (map.get(key) == null) {
            map.put(key, defaultValue);
        }
        return map;
    }

    /**
     * Replaces a value for the given key.
     *
     * @param map target map
     * @param key key to replace
     * @param value new value
     * @param <K> key type
     * @param <V> value type
     * @return target map
     */
    public static <K, V> Map<K, V> replaceValue(Map<K, V> map, K key, V value) {
        requireMap(map);
        if (map.containsKey(key)) {
            map.put(key, value);
        }
        return map;
    }

    /**
     * Removes a collection of keys.
     *
     * @param map target map
     * @param keys keys to remove
     * @param <K> key type
     * @param <V> value type
     * @return target map
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
     * Merges source and target maps, where target entries override source entries.
     *
     * @param source source map
     * @param target target map
     * @param <K> key type
     * @param <V> value type
     * @return merged map
     */
    public static <K, V> Map<K, V> merge(Map<K, V> source, Map<K, V> target) {
        return mergeOverwrite(source, target);
    }

    /**
     * Merges maps, where override entries override base entries.
     *
     * @param base base map
     * @param override override map
     * @param <K> key type
     * @param <V> value type
     * @return merged map
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
     * Merges maps and ignores null values in the override map.
     *
     * @param base base map
     * @param override override map
     * @param <K> key type
     * @param <V> value type
     * @return merged map
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
     * Merges maps and keeps original values when keys conflict.
     *
     * @param base base map
     * @param append append map
     * @param <K> key type
     * @param <V> value type
     * @return merged map
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
     * Merges maps with a custom value merge function.
     *
     * @param left left map
     * @param right right map
     * @param valueMerger value merger for duplicate keys
     * @param <K> key type
     * @param <V> value type
     * @return merged map
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
     * Deeply merges nested maps, where override entries override base entries.
     *
     * @param base base map
     * @param override override map
     * @return deeply merged map
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
     * Merges a collection of maps, where later maps override earlier maps.
     *
     * @param maps source maps
     * @param <K> key type
     * @param <V> value type
     * @return merged map
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
     * Filters entries with the given predicate.
     *
     * @param map source map
     * @param predicate entry predicate
     * @param <K> key type
     * @param <V> value type
     * @return filtered map
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
     * Filters entries by key.
     *
     * @param map source map
     * @param predicate key predicate
     * @param <K> key type
     * @param <V> value type
     * @return filtered map
     */
    public static <K, V> Map<K, V> filterKeys(Map<K, V> map, Predicate<K> predicate) {
        Objects.requireNonNull(predicate, "predicate must not be null");
        return filter(map, (key, value) -> predicate.test(key));
    }

    /**
     * Filters entries by value.
     *
     * @param map source map
     * @param predicate value predicate
     * @param <K> key type
     * @param <V> value type
     * @return filtered map
     */
    public static <K, V> Map<K, V> filterValues(Map<K, V> map, Predicate<V> predicate) {
        Objects.requireNonNull(predicate, "predicate must not be null");
        return filter(map, (key, value) -> predicate.test(value));
    }

    /**
     * Includes only the given keys.
     *
     * @param map source map
     * @param keys keys to include
     * @param <K> key type
     * @param <V> value type
     * @return filtered map
     */
    public static <K, V> Map<K, V> includeKeys(Map<K, V> map, Collection<K> keys) {
        if (map == null || keys == null) {
            return new LinkedHashMap<>();
        }
        Set<K> keySet = new LinkedHashSet<>(keys);
        return filterKeys(map, keySet::contains);
    }

    /**
     * Excludes the given keys.
     *
     * @param map source map
     * @param keys keys to exclude
     * @param <K> key type
     * @param <V> value type
     * @return filtered map
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
     * Picks only the given keys.
     *
     * @param map source map
     * @param keys keys to pick
     * @param <K> key type
     * @param <V> value type
     * @return filtered map
     */
    public static <K, V> Map<K, V> pick(Map<K, V> map, Collection<K> keys) {
        return includeKeys(map, keys);
    }

    /**
     * Omits the given keys.
     *
     * @param map source map
     * @param keys keys to omit
     * @param <K> key type
     * @param <V> value type
     * @return filtered map
     */
    public static <K, V> Map<K, V> omit(Map<K, V> map, Collection<K> keys) {
        return excludeKeys(map, keys);
    }

    /**
     * Keeps entries whose value is not null.
     *
     * @param map source map
     * @param <K> key type
     * @param <V> value type
     * @return filtered map
     */
    public static <K, V> Map<K, V> filterNotNullValue(Map<K, V> map) {
        return filterValues(map, Objects::nonNull);
    }

    /**
     * Keeps entries whose string value is not blank.
     *
     * @param map source map
     * @param <K> key type
     * @return filtered map
     */
    public static <K> Map<K, String> filterNotBlankValue(Map<K, String> map) {
        return filterValues(map, value -> !isBlank(value));
    }

    /**
     * Maps keys and keeps original values.
     *
     * @param map source map
     * @param keyMapper key mapper
     * @param <K> source key type
     * @param <V> value type
     * @param <NK> new key type
     * @return mapped map
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
     * Maps values and keeps original keys.
     *
     * @param map source map
     * @param valueMapper value mapper
     * @param <K> key type
     * @param <V> source value type
     * @param <NV> new value type
     * @return mapped map
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
     * Maps entries to new entries.
     *
     * @param map source map
     * @param mapper entry mapper
     * @param <K> source key type
     * @param <V> source value type
     * @param <NK> new key type
     * @param <NV> new value type
     * @return mapped map
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
     * Converts map entries to a list.
     *
     * @param map source map
     * @param <K> key type
     * @param <V> value type
     * @return entry list
     */
    public static <K, V> List<Map.Entry<K, V>> toList(Map<K, V> map) {
        return map == null ? new ArrayList<>() : new ArrayList<>(map.entrySet());
    }

    /**
     * Converts map keys to a list.
     *
     * @param map source map
     * @param <K> key type
     * @param <V> value type
     * @return key list
     */
    public static <K, V> List<K> toKeyList(Map<K, V> map) {
        return map == null ? new ArrayList<>() : new ArrayList<>(map.keySet());
    }

    /**
     * Converts map values to a list.
     *
     * @param map source map
     * @param <K> key type
     * @param <V> value type
     * @return value list
     */
    public static <K, V> List<V> toValueList(Map<K, V> map) {
        return map == null ? new ArrayList<>() : new ArrayList<>(map.values());
    }

    /**
     * Converts map entries to a set.
     *
     * @param map source map
     * @param <K> key type
     * @param <V> value type
     * @return entry set
     */
    public static <K, V> Set<Map.Entry<K, V>> toSet(Map<K, V> map) {
        return map == null ? new LinkedHashSet<>() : new LinkedHashSet<>(map.entrySet());
    }

    /**
     * Converts keys to String and keeps values as Object.
     *
     * @param map source map
     * @return object map
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
     * Converts keys and values to String.
     *
     * @param map source map
     * @return string map
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
     * Converts the map to a LinkedHashMap.
     *
     * @param map source map
     * @param <K> key type
     * @param <V> value type
     * @return linked map
     */
    public static <K, V> LinkedHashMap<K, V> toLinkedMap(Map<K, V> map) {
        return map == null ? new LinkedHashMap<>() : new LinkedHashMap<>(map);
    }

    /**
     * Converts a bean to a map.
     *
     * @param bean source bean
     * @return bean field map
     */
    public static Map<String, Object> beanToMap(Object bean) {
        return beanToMap(bean, false);
    }

    /**
     * Converts a bean to a map.
     *
     * @param bean source bean
     * @param ignoreNull whether null field values should be ignored
     * @return bean field map
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
     * Converts a map to a bean instance.
     *
     * @param map source map
     * @param beanType bean class
     * @param <T> bean type
     * @return bean instance
     */
    public static <T> T mapToBean(Map<String, Object> map, Class<T> beanType) {
        Objects.requireNonNull(beanType, "beanType must not be null");
        T bean = instantiate(beanType);
        copyToBean(map, bean);
        return bean;
    }

    /**
     * Converts a map to a bean instance and ignores failed field assignments.
     *
     * @param map source map
     * @param beanType bean class
     * @param <T> bean type
     * @return bean instance
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
                // Intentionally ignore invalid field values for this API variant.
            }
        }
        return bean;
    }

    /**
     * Copies map values to an existing bean.
     *
     * @param map source map
     * @param bean target bean
     * @param <T> bean type
     * @return target bean
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
     * Copies bean fields to a target map.
     *
     * @param bean source bean
     * @param target target map
     * @return target map
     */
    public static Map<String, Object> copyFromBean(Object bean, Map<String, Object> target) {
        requireMap(target);
        target.putAll(beanToMap(bean));
        return target;
    }

    /**
     * Converts a bean to a string map.
     *
     * @param bean source bean
     * @return string map
     */
    public static Map<String, String> beanToStringMap(Object bean) {
        return toStringMap(beanToMap(bean));
    }

    /**
     * Sorts a map by key using natural order.
     *
     * @param map source map
     * @param <K> comparable key type
     * @param <V> value type
     * @return sorted linked map
     */
    public static <K extends Comparable<? super K>, V> Map<K, V> sortByKey(Map<K, V> map) {
        return sortByKey(map, Comparator.nullsFirst(Comparator.naturalOrder()));
    }

    /**
     * Sorts a map by key using the comparator.
     *
     * @param map source map
     * @param comparator key comparator
     * @param <K> key type
     * @param <V> value type
     * @return sorted linked map
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
     * Sorts a map by value using natural order.
     *
     * @param map source map
     * @param <K> key type
     * @param <V> comparable value type
     * @return sorted linked map
     */
    public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
        return sortByValue(map, Comparator.nullsFirst(Comparator.naturalOrder()));
    }

    /**
     * Sorts a map by value using the comparator.
     *
     * @param map source map
     * @param comparator value comparator
     * @param <K> key type
     * @param <V> value type
     * @return sorted linked map
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
     * Converts a map to a TreeMap.
     *
     * @param map source map
     * @param <K> comparable key type
     * @param <V> value type
     * @return tree map
     */
    public static <K extends Comparable<? super K>, V> TreeMap<K, V> toTreeMap(Map<K, V> map) {
        TreeMap<K, V> result = new TreeMap<>();
        if (map != null) {
            result.putAll(map);
        }
        return result;
    }

    /**
     * Converts a map to a LinkedHashMap.
     *
     * @param map source map
     * @param <K> key type
     * @param <V> value type
     * @return linked hash map
     */
    public static <K, V> LinkedHashMap<K, V> toLinkedHashMap(Map<K, V> map) {
        return toLinkedMap(map);
    }

    /**
     * Reverses the current iteration order.
     *
     * @param map source map
     * @param <K> key type
     * @param <V> value type
     * @return reversed linked map
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
     * Builds an index map from a collection, where later items override earlier items.
     *
     * @param collection source collection
     * @param keyMapper key mapper
     * @param <T> item type
     * @param <K> key type
     * @return indexed map
     */
    public static <T, K> Map<K, T> indexBy(Collection<T> collection, Function<T, K> keyMapper) {
        return indexBy(collection, keyMapper, (oldValue, newValue) -> newValue);
    }

    /**
     * Builds an index map from a collection with a merge function.
     *
     * @param collection source collection
     * @param keyMapper key mapper
     * @param mergeFunction merge function for duplicate keys
     * @param <T> item type
     * @param <K> key type
     * @return indexed map
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
     * Groups collection items by key.
     *
     * @param collection source collection
     * @param keyMapper key mapper
     * @param <T> item type
     * @param <K> key type
     * @return grouped map
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
     * Counts collection items by key.
     *
     * @param collection source collection
     * @param keyMapper key mapper
     * @param <T> item type
     * @param <K> key type
     * @return count map
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
     * Sums BigDecimal values by key.
     *
     * @param collection source collection
     * @param keyMapper key mapper
     * @param valueMapper value mapper
     * @param <T> item type
     * @param <K> key type
     * @return sum map
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
     * Converts a collection to a map, where later items override earlier items.
     *
     * @param collection source collection
     * @param keyMapper key mapper
     * @param valueMapper value mapper
     * @param <T> item type
     * @param <K> key type
     * @param <V> value type
     * @return mapped map
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
     * Converts a collection to a LinkedHashMap, where later items override earlier items.
     *
     * @param collection source collection
     * @param keyMapper key mapper
     * @param valueMapper value mapper
     * @param <T> item type
     * @param <K> key type
     * @param <V> value type
     * @return mapped linked map
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
     * Writes a value by dot-separated path and creates intermediate maps when needed.
     *
     * @param map target map
     * @param path dot-separated path
     * @param value value to write
     * @return target map
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
     * Checks whether a dot-separated path exists.
     *
     * @param map source map
     * @param path dot-separated path
     * @return true if the path exists
     */
    public static boolean containsPath(Map<String, Object> map, String path) {
        return containsPathInternal(map, path);
    }

    /**
     * Removes a value by dot-separated path.
     *
     * @param map target map
     * @param path dot-separated path
     * @return removed value or null
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
     * Flattens a nested map into dot-separated keys.
     *
     * @param map source map
     * @return flattened map
     */
    public static Map<String, Object> flatten(Map<String, Object> map) {
        Map<String, Object> result = new LinkedHashMap<>();
        flattenInto(null, map, result);
        return result;
    }

    /**
     * Restores a flattened dot-separated map to nested maps.
     *
     * @param map flattened map
     * @return nested map
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
     * Compares two maps and returns a diff model.
     *
     * @param oldMap old map
     * @param newMap new map
     * @param <K> key type
     * @param <V> value type
     * @return diff model
     */
    public static <K, V> MapDiff<K, V> diff(Map<K, V> oldMap, Map<K, V> newMap) {
        return new MapDiff<>(added(oldMap, newMap), removed(oldMap, newMap), changed(oldMap, newMap));
    }

    /**
     * Returns entries that exist only in the new map.
     *
     * @param oldMap old map
     * @param newMap new map
     * @param <K> key type
     * @param <V> value type
     * @return added entries
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
     * Returns entries that exist only in the old map.
     *
     * @param oldMap old map
     * @param newMap new map
     * @param <K> key type
     * @param <V> value type
     * @return removed entries
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
     * Returns changed values for common keys.
     *
     * @param oldMap old map
     * @param newMap new map
     * @param <K> key type
     * @param <V> value type
     * @return changed entries
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
     * Checks whether two maps are equal.
     *
     * @param left left map
     * @param right right map
     * @return true if maps are equal
     */
    public static boolean same(Map<?, ?> left, Map<?, ?> right) {
        return Objects.equals(left, right);
    }

    /**
     * Checks whether two maps are equal regardless of iteration order.
     *
     * @param left left map
     * @param right right map
     * @return true if maps contain the same entries
     */
    public static boolean equalsIgnoreOrder(Map<?, ?> left, Map<?, ?> right) {
        return Objects.equals(left, right);
    }

    /**
     * Compares one key value between two maps.
     *
     * @param oldMap old map
     * @param newMap new map
     * @param key key to compare
     * @param <K> key type
     * @param <V> value type
     * @return value change when the key state or value differs
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
     * Converts a map to a URL query string without URL encoding.
     *
     * @param map source map
     * @return query string
     */
    public static String toQueryString(Map<String, ?> map) {
        return toQueryString(map, false);
    }

    /**
     * Converts a map to a URL query string.
     *
     * @param map source map
     * @param encode whether key and value should be URL encoded
     * @return query string
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
     * Converts a map to a URL query string sorted by key.
     *
     * @param map source map
     * @return sorted query string
     */
    public static String toSortedQueryString(Map<String, ?> map) {
        if (map == null) {
            return "";
        }
        return toQueryString(new TreeMap<>(map), false);
    }

    /**
     * Joins entries with custom separators.
     *
     * @param map source map
     * @param entrySeparator separator between entries
     * @param keyValueSeparator separator between key and value
     * @return joined string
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
     * Joins entries with custom separators and skips null values.
     *
     * @param map source map
     * @param entrySeparator separator between entries
     * @param keyValueSeparator separator between key and value
     * @return joined string
     */
    public static String joinIgnoreNull(Map<?, ?> map, String entrySeparator, String keyValueSeparator) {
        return join(filterValues(map, Objects::nonNull), entrySeparator, keyValueSeparator);
    }

    /**
     * Converts a map to a signature string sorted by key and ignoring null values.
     *
     * @param map source map
     * @return signature string
     */
    public static String toSignString(Map<String, ?> map) {
        if (map == null) {
            return "";
        }
        return joinIgnoreNull(new TreeMap<>(map), "&", "=");
    }

    /**
     * Converts a map to a log-friendly string.
     *
     * @param map source map
     * @return log string
     */
    public static String toLogString(Map<?, ?> map) {
        return map == null ? "{}" : map.toString();
    }

    /**
     * Parses a URL query string to a map.
     *
     * @param queryString query string
     * @return parsed map
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
     * Masks selected keys with the default mask rule.
     *
     * @param map source map
     * @param keys keys to mask
     * @return copied map with masked values
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
     * Masks values with custom mask rules.
     *
     * @param map source map
     * @param maskRules key to mask function rules
     * @return copied map with masked values
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
     * Returns a copy without sensitive keys.
     *
     * @param map source map
     * @param keys keys to remove
     * @return copied map without sensitive keys
     */
    public static Map<String, Object> removeSensitiveKeys(Map<String, Object> map, Collection<String> keys) {
        return copyWithoutSensitiveKeys(map, keys);
    }

    /**
     * Returns a copy without sensitive keys.
     *
     * @param map source map
     * @param keys keys to remove
     * @return copied map without sensitive keys
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
     * Masks a phone number value for the given key.
     *
     * @param map source map
     * @param key key to mask
     * @return copied map with masked phone value
     */
    public static Map<String, Object> maskPhone(Map<String, Object> map, String key) {
        return mask(map, Map.of(key, MapUtil::maskPhoneValue));
    }

    /**
     * Masks an email value for the given key.
     *
     * @param map source map
     * @param key key to mask
     * @return copied map with masked email value
     */
    public static Map<String, Object> maskEmail(Map<String, Object> map, String key) {
        return mask(map, Map.of(key, MapUtil::maskEmailValue));
    }

    /**
     * Masks an ID card value for the given key.
     *
     * @param map source map
     * @param key key to mask
     * @return copied map with masked ID card value
     */
    public static Map<String, Object> maskIdCard(Map<String, Object> map, String key) {
        return mask(map, Map.of(key, MapUtil::maskIdCardValue));
    }

    /**
     * Requires a map to be not empty.
     *
     * @param map map to validate
     * @param message exception message
     * @param <K> key type
     * @param <V> value type
     * @return original map
     */
    public static <K, V> Map<K, V> requireNotEmpty(Map<K, V> map, String message) {
        if (isEmpty(map)) {
            throw new IllegalArgumentException(message == null ? "map must not be empty" : message);
        }
        return map;
    }

    /**
     * Requires a key to exist.
     *
     * @param map map to validate
     * @param key required key
     * @param message exception message
     */
    public static void requireKey(Map<?, ?> map, Object key, String message) {
        if (map == null || !map.containsKey(key)) {
            throw new IllegalArgumentException(message == null ? "required key is missing: " + key : message);
        }
    }

    /**
     * Requires all keys to exist.
     *
     * @param map map to validate
     * @param keys required keys
     * @param message exception message
     */
    public static void requireKeys(Map<?, ?> map, Collection<?> keys, String message) {
        List<?> missing = missingKeys(map, keys);
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException(message == null ? "required keys are missing: " + missing : message);
        }
    }

    /**
     * Requires a key value to be not null.
     *
     * @param map map to validate
     * @param key required key
     * @param message exception message
     */
    public static void requireValue(Map<?, ?> map, Object key, String message) {
        if (map == null || !map.containsKey(key) || map.get(key) == null) {
            throw new IllegalArgumentException(message == null ? "required value is missing: " + key : message);
        }
    }

    /**
     * Returns missing keys.
     *
     * @param map map to validate
     * @param keys required keys
     * @return missing key list
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
     * Checks whether all keys exist.
     *
     * @param map map to validate
     * @param keys required keys
     * @return true if all keys exist
     */
    public static boolean hasAllKeys(Map<?, ?> map, Collection<?> keys) {
        return missingKeys(map, keys).isEmpty();
    }

    /**
     * Checks whether any key exists.
     *
     * @param map map to validate
     * @param keys candidate keys
     * @return true if any key exists
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
     * Linked map builder.
     *
     * @param <K> key type
     * @param <V> value type
     */
    public static final class Builder<K, V> {
        private final LinkedHashMap<K, V> map = new LinkedHashMap<>();

        private Builder() {
        }

        /**
         * Adds an entry.
         *
         * @param key key
         * @param value value
         * @return this builder
         */
        public Builder<K, V> put(K key, V value) {
            map.put(key, value);
            return this;
        }

        /**
         * Adds an entry when value is not null.
         *
         * @param key key
         * @param value value
         * @return this builder
         */
        public Builder<K, V> putIfNotNull(K key, V value) {
            if (value != null) {
                map.put(key, value);
            }
            return this;
        }

        /**
         * Adds all entries.
         *
         * @param source source map
         * @return this builder
         */
        public Builder<K, V> putAll(Map<K, V> source) {
            if (source != null) {
                map.putAll(source);
            }
            return this;
        }

        /**
         * Builds an immutable map.
         *
         * @return immutable map
         */
        public Map<K, V> build() {
            return Collections.unmodifiableMap(new LinkedHashMap<>(map));
        }

        /**
         * Builds a mutable linked map.
         *
         * @return mutable linked map
         */
        public LinkedHashMap<K, V> buildMutable() {
            return new LinkedHashMap<>(map);
        }

        /**
         * Builds an immutable map.
         *
         * @return immutable map
         */
        public Map<K, V> buildImmutable() {
            return build();
        }
    }

    /**
     * Value change model.
     *
     * @param <V> value type
     */
    public static final class ValueChange<V> {
        private final V oldValue;
        private final V newValue;

        private ValueChange(V oldValue, V newValue) {
            this.oldValue = oldValue;
            this.newValue = newValue;
        }

        /**
         * Returns the old value.
         *
         * @return old value
         */
        public V getOldValue() {
            return oldValue;
        }

        /**
         * Returns the new value.
         *
         * @return new value
         */
        public V getNewValue() {
            return newValue;
        }
    }

    /**
     * Map diff model.
     *
     * @param <K> key type
     * @param <V> value type
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
         * Returns added entries.
         *
         * @return added entries
         */
        public Map<K, V> getAdded() {
            return added;
        }

        /**
         * Returns removed entries.
         *
         * @return removed entries
         */
        public Map<K, V> getRemoved() {
            return removed;
        }

        /**
         * Returns changed entries.
         *
         * @return changed entries
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
            // Try common local date-time formats below.
        }
        for (String pattern : List.of("yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm", "yyyy-MM-dd")) {
            try {
                return new SimpleDateFormat(pattern).parse(text);
            } catch (ParseException ignored) {
                // Continue to next pattern.
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
            // Try local date-time formats below.
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
            // Try common local date-time formats below.
        }
        try {
            return LocalDateTime.parse(text, DATE_TIME_SPACE_FORMATTER);
        } catch (DateTimeParseException ignored) {
            // Try minute precision below.
        }
        try {
            return LocalDateTime.parse(text, DATE_TIME_MINUTE_FORMATTER);
        } catch (DateTimeParseException ignored) {
            // Try date only below.
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
