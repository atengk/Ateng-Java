package local.ateng.java.customutils.utils;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Map 工具类
 * 提供常用 Map 处理方法
 *
 * @author Ateng
 * @since 2025-07-30
 */
public final class MapUtil {

    /**
     * 禁止实例化工具类
     */
    private MapUtil() {
        throw new UnsupportedOperationException("工具类不可实例化");
    }

    /**
     * 判断 Map 是否为 null 或空
     *
     * @param map 待判断的 Map
     * @return 若为 null 或空，返回 true；否则返回 false
     */
    public static boolean isEmpty(Map<?, ?> map) {
        return map == null || map.isEmpty();
    }

    /**
     * 判断 Map 是否不为空
     *
     * @param map 待判断的 Map
     * @return 若不为 null 且不为空，返回 true；否则返回 false
     */
    public static boolean isNotEmpty(Map<?, ?> map) {
        return !isEmpty(map);
    }

    /**
     * 安全获取 Map 中的值，若 Map 为 null 或 key 不存在，则返回默认值
     *
     * @param map          Map 实例
     * @param key          键
     * @param defaultValue 默认值
     * @param <K>          键类型
     * @param <V>          值类型
     * @return 对应 key 的值，若不存在则返回 defaultValue
     */
    public static <K, V> V getOrDefault(Map<K, V> map, K key, V defaultValue) {
        return map != null ? map.getOrDefault(key, defaultValue) : defaultValue;
    }

    /**
     * 将 Map 的键值对转换为新的 Map
     *
     * @param map       原始 Map
     * @param keyFunc   键转换函数
     * @param valueFunc 值转换函数
     * @param <K>       原始键类型
     * @param <V>       原始值类型
     * @param <K2>      新键类型
     * @param <V2>      新值类型
     * @return 转换后的新 Map，若输入为 null 则返回空 Map
     */
    public static <K, V, K2, V2> Map<K2, V2> mapTransform(Map<K, V> map,
                                                          Function<K, K2> keyFunc,
                                                          Function<V, V2> valueFunc) {
        if (map == null || keyFunc == null || valueFunc == null) {
            return Collections.emptyMap();
        }
        Map<K2, V2> result = new HashMap<>();
        for (Map.Entry<K, V> entry : map.entrySet()) {
            result.put(keyFunc.apply(entry.getKey()), valueFunc.apply(entry.getValue()));
        }
        return result;
    }

    /**
     * 遍历 Map 并执行指定操作
     *
     * @param map      要遍历的 Map
     * @param consumer 操作逻辑，接收 key 和 value
     * @param <K>      键类型
     * @param <V>      值类型
     */
    public static <K, V> void forEach(Map<K, V> map, BiConsumer<K, V> consumer) {
        if (map != null && consumer != null) {
            for (Map.Entry<K, V> entry : map.entrySet()) {
                consumer.accept(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * 返回一个不可变的空 Map
     *
     * @param <K> 键类型
     * @param <V> 值类型
     * @return 空 Map（不可变）
     */
    public static <K, V> Map<K, V> emptyMap() {
        return Collections.emptyMap();
    }

    /**
     * 将一个数组构建为 Map（键值交错方式）
     *
     * @param entries 键值交错的数组，如 {"key1", "value1", "key2", "value2"}
     * @return 构建的 Map
     * @throws IllegalArgumentException 如果参数个数不是偶数
     */
    public static Map<String, String> of(String... entries) {
        int stepTwo = 2;
        String entriesMustBeEvenMsg = "参数个数必须为偶数";

        if (entries == null || entries.length % stepTwo != 0) {
            throw new IllegalArgumentException(entriesMustBeEvenMsg);
        }

        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < entries.length; i += stepTwo) {
            map.put(entries[i], entries[i + 1]);
        }
        return map;
    }

    /**
     * 合并两个 Map，后者覆盖前者的键值对
     *
     * @param first  第一个 Map
     * @param second 第二个 Map
     * @param <K>    键类型
     * @param <V>    值类型
     * @return 合并后的 Map，若两个都是 null 返回空 Map
     */
    public static <K, V> Map<K, V> merge(Map<K, V> first, Map<K, V> second) {
        Map<K, V> result = new HashMap<>();
        if (first != null) {
            result.putAll(first);
        }
        if (second != null) {
            // 第二个覆盖第一个
            result.putAll(second);
        }
        return result;
    }

    /**
     * 根据条件过滤 Map 的键值对
     *
     * @param map       原始 Map
     * @param predicate 过滤条件（返回 true 表示保留）
     * @param <K>       键类型
     * @param <V>       值类型
     * @return 过滤后的 Map，若 map 为 null 则返回空 Map
     */
    public static <K, V> Map<K, V> filter(Map<K, V> map, BiConsumer<K, V> predicate) {
        if (map == null || predicate == null) {
            return Collections.emptyMap();
        }
        Map<K, V> result = new HashMap<>();
        for (Map.Entry<K, V> entry : map.entrySet()) {
            try {
                predicate.accept(entry.getKey(), entry.getValue());
                result.put(entry.getKey(), entry.getValue());
            } catch (Exception ignored) {
                // 若异常则视为不保留
            }
        }
        return result;
    }

    /**
     * 排序 Map（按键升序），返回新的 LinkedHashMap 保持顺序
     *
     * @param map 原始 Map
     * @param <K> 键类型（必须实现 Comparable）
     * @param <V> 值类型
     * @return 排序后的 Map，若 map 为 null 或空则返回空 Map
     */
    public static <K extends Comparable<K>, V> Map<K, V> sortByKey(Map<K, V> map) {
        if (isEmpty(map)) {
            return Collections.emptyMap();
        }
        Map<K, V> result = new LinkedHashMap<>();
        map.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEachOrdered(e -> result.put(e.getKey(), e.getValue()));
        return result;
    }

    /**
     * 排序 Map（按值升序），返回新的 LinkedHashMap 保持顺序
     *
     * @param map 原始 Map
     * @param <K> 键类型
     * @param <V> 值类型（必须实现 Comparable）
     * @return 排序后的 Map，若 map 为 null 或空则返回空 Map
     */
    public static <K, V extends Comparable<V>> Map<K, V> sortByValue(Map<K, V> map) {
        if (isEmpty(map)) {
            return Collections.emptyMap();
        }
        Map<K, V> result = new LinkedHashMap<>();
        map.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .forEachOrdered(e -> result.put(e.getKey(), e.getValue()));
        return result;
    }

    /**
     * 创建只包含指定键的子 Map（若键不存在则忽略）
     *
     * @param map 原始 Map
     * @param keys 需要保留的键
     * @param <K> 键类型
     * @param <V> 值类型
     * @return 包含指定键的子 Map
     */
    @SafeVarargs
    public static <K, V> Map<K, V> pick(Map<K, V> map, K... keys) {
        if (isEmpty(map) || keys == null || keys.length == 0) {
            return Collections.emptyMap();
        }
        Map<K, V> result = new HashMap<>();
        for (K key : keys) {
            if (map.containsKey(key)) {
                result.put(key, map.get(key));
            }
        }
        return result;
    }

    /**
     * 清空 Map 并返回之前的内容（线程不安全）
     *
     * @param map 原始 Map
     * @param <K> 键类型
     * @param <V> 值类型
     * @return 原有内容的副本
     */
    public static <K, V> Map<K, V> clearAndReturn(Map<K, V> map) {
        if (isEmpty(map)) {
            return Collections.emptyMap();
        }
        Map<K, V> copy = new HashMap<>(map);
        map.clear();
        return copy;
    }

    /**
     * 移除值为 null 的键值对
     *
     * @param map 原始 Map
     * @param <K> 键类型
     * @param <V> 值类型
     * @return 移除后的 Map（副本），若 map 为 null 返回空 Map
     */
    public static <K, V> Map<K, V> removeNullValues(Map<K, V> map) {
        if (isEmpty(map)) {
            return Collections.emptyMap();
        }
        Map<K, V> result = new HashMap<>();
        for (Map.Entry<K, V> entry : map.entrySet()) {
            if (entry.getValue() != null) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    /**
     * 检查 Map 是否包含指定全部 key
     *
     * @param map  原始 Map
     * @param keys 要检查的键集合
     * @param <K>  键类型
     * @return 所有 key 都存在则返回 true，否则 false
     */
    @SafeVarargs
    public static <K> boolean containsAllKeys(Map<K, ?> map, K... keys) {
        if (isEmpty(map) || keys == null || keys.length == 0) {
            return false;
        }
        for (K key : keys) {
            if (!map.containsKey(key)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 检查 Map 是否包含任意一个指定 key
     *
     * @param map  原始 Map
     * @param keys 要检查的键集合
     * @param <K>  键类型
     * @return 任意 key 存在则返回 true，否则 false
     */
    @SafeVarargs
    public static <K> boolean containsAnyKey(Map<K, ?> map, K... keys) {
        if (isEmpty(map) || keys == null || keys.length == 0) {
            return false;
        }
        for (K key : keys) {
            if (map.containsKey(key)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 将 Map 转换为字符串表示（key=value 形式，使用指定分隔符）
     *
     * @param map       原始 Map
     * @param entrySep  键值对分隔符（如 ","）
     * @param kvSep     键与值之间的分隔符（如 "="）
     * @return 转换后的字符串表示，若 map 为空返回空字符串
     */
    public static String toString(Map<?, ?> map, String entrySep, String kvSep) {
        if (isEmpty(map)) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            sb.append(entry.getKey()).append(kvSep).append(entry.getValue()).append(entrySep);
        }
        // 移除最后一个分隔符
        if (sb.length() > 0) {
            sb.setLength(sb.length() - entrySep.length());
        }
        return sb.toString();
    }

    /**
     * 将数组转换为 Map，使用索引作为键
     *
     * @param values 数组值
     * @param <V>    值类型
     * @return 转换后的 Map，key 为数组索引
     */
    public static <V> Map<Integer, V> arrayToMap(V[] values) {
        if (values == null || values.length == 0) {
            return Collections.emptyMap();
        }
        Map<Integer, V> result = new HashMap<>();
        for (int i = 0; i < values.length; i++) {
            result.put(i, values[i]);
        }
        return result;
    }

    /**
     * 获取 Map 中指定 key 对应的值，如果 key 不存在则抛出异常
     *
     * @param map Map 实例
     * @param key 要获取的键
     * @param <K> 键类型
     * @param <V> 值类型
     * @return 对应值
     * @throws IllegalArgumentException 若 key 不存在
     */
    public static <K, V> V getRequired(Map<K, V> map, K key) {
        if (map == null || !map.containsKey(key)) {
            throw new IllegalArgumentException("key 不存在: " + key);
        }
        return map.get(key);
    }

    /**
     * 计算两个 Map 的键交集（只保留共同的 key）
     *
     * @param first  第一个 Map
     * @param second 第二个 Map
     * @param <K>    键类型
     * @param <V>    值类型
     * @return 包含两个 Map 共同键的子 Map（取 first 的值）
     */
    public static <K, V> Map<K, V> intersection(Map<K, V> first, Map<K, ?> second) {
        if (isEmpty(first) || isEmpty(second)) {
            return Collections.emptyMap();
        }
        Map<K, V> result = new HashMap<>();
        for (Map.Entry<K, V> entry : first.entrySet()) {
            if (second.containsKey(entry.getKey())) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    /**
     * 计算两个 Map 的键差集（仅保留 first 中不存在于 second 的 key）
     *
     * @param first  第一个 Map
     * @param second 第二个 Map
     * @param <K>    键类型
     * @param <V>    值类型
     * @return 差集 Map（取 first 中不在 second 中的键）
     */
    public static <K, V> Map<K, V> difference(Map<K, V> first, Map<K, ?> second) {
        if (isEmpty(first)) {
            return Collections.emptyMap();
        }
        if (isEmpty(second)) {
            return new HashMap<>(first);
        }
        Map<K, V> result = new HashMap<>();
        for (Map.Entry<K, V> entry : first.entrySet()) {
            if (!second.containsKey(entry.getKey())) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    /**
     * 计算多个 Map 的并集，后者覆盖前者的键值
     *
     * @param maps 多个 Map 实例
     * @param <K>  键类型
     * @param <V>  值类型
     * @return 合并后的 Map
     */
    @SafeVarargs
    public static <K, V> Map<K, V> union(Map<K, V>... maps) {
        Map<K, V> result = new HashMap<>();
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
     * 合并多个 Map，value 为 List 类型时自动追加（适用于 Map<String, List<V>>）
     *
     * @param maps 多个 Map 实例
     * @param <K>  键类型
     * @param <V>  值类型（List 元素类型）
     * @return 合并后的 Map，value 为列表拼接结果
     */
    @SafeVarargs
    public static <K, V> Map<K, java.util.List<V>> mergeListValues(Map<K, java.util.List<V>>... maps) {
        Map<K, java.util.List<V>> result = new HashMap<>();
        if (maps != null) {
            for (Map<K, java.util.List<V>> map : maps) {
                if (map == null) {
                    continue;
                }
                for (Map.Entry<K, java.util.List<V>> entry : map.entrySet()) {
                    K key = entry.getKey();
                    java.util.List<V> value = entry.getValue();
                    if (value == null) {
                        continue;
                    }
                    result.computeIfAbsent(key, k -> new java.util.ArrayList<>()).addAll(value);
                }
            }
        }
        return result;
    }

    /**
     * 将键和值调换（注意：若存在重复 value，会覆盖之前的 key）
     *
     * @param map 原始 Map
     * @param <K> 键类型
     * @param <V> 值类型（用作新键）
     * @return 颠倒键值的新 Map
     */
    public static <K, V> Map<V, K> invert(Map<K, V> map) {
        if (isEmpty(map)) {
            return Collections.emptyMap();
        }
        Map<V, K> result = new HashMap<>();
        for (Map.Entry<K, V> entry : map.entrySet()) {
            result.put(entry.getValue(), entry.getKey());
        }
        return result;
    }

    /**
     * 将 Map 克隆为一个浅拷贝副本
     *
     * @param map 原始 Map
     * @param <K> 键类型
     * @param <V> 值类型
     * @return 拷贝后的 Map
     */
    public static <K, V> Map<K, V> clone(Map<K, V> map) {
        return isEmpty(map) ? Collections.emptyMap() : new HashMap<>(map);
    }

    /**
     * 安全获取嵌套 Map 中的值
     *
     * @param map       外层 Map
     * @param outerKey  外层键
     * @param innerKey  内层键
     * @param <K1>      外层键类型
     * @param <K2>      内层键类型
     * @param <V>       值类型
     * @return 值，若任意层级为 null 或键不存在则返回 null
     */
    public static <K1, K2, V> V getNestedValue(Map<K1, Map<K2, V>> map, K1 outerKey, K2 innerKey) {
        if (map == null || outerKey == null || innerKey == null) {
            return null;
        }
        Map<K2, V> inner = map.get(outerKey);
        return inner != null ? inner.get(innerKey) : null;
    }

    /**
     * 向嵌套 Map 中安全地添加值（如果不存在则自动创建）
     *
     * @param map       外层 Map
     * @param outerKey  外层键
     * @param innerKey  内层键
     * @param value     要添加的值
     * @param <K1>      外层键类型
     * @param <K2>      内层键类型
     * @param <V>       值类型
     */
    public static <K1, K2, V> void putNestedValue(Map<K1, Map<K2, V>> map, K1 outerKey, K2 innerKey, V value) {
        if (map == null || outerKey == null || innerKey == null) {
            return;
        }
        map.computeIfAbsent(outerKey, k -> new HashMap<>()).put(innerKey, value);
    }

    /**
     * 按指定值对 Map 分组（值作为分组 key）
     *
     * @param map 原始 Map
     * @param <K> 原始 Map 的键类型
     * @param <V> 原始 Map 的值类型
     * @return 分组结果，key 为值，value 为原始 key 的集合
     */
    public static <K, V> Map<V, List<K>> groupByValue(Map<K, V> map) {
        if (isEmpty(map)) {
            return Collections.emptyMap();
        }
        Map<V, List<K>> result = new HashMap<>();
        for (Map.Entry<K, V> entry : map.entrySet()) {
            result.computeIfAbsent(entry.getValue(), k -> new ArrayList<>()).add(entry.getKey());
        }
        return result;
    }

    /**
     * 按指定函数对 Map 分组（通过键或值映射规则）
     *
     * @param map         原始 Map
     * @param classifier  分组函数（可用 key 或 value 构造规则）
     * @param <K>         键类型
     * @param <V>         值类型
     * @param <G>         分组键类型
     * @return 分组结果 Map
     */
    public static <K, V, G> Map<G, List<Map.Entry<K, V>>> groupBy(Map<K, V> map, Function<Map.Entry<K, V>, G> classifier) {
        if (isEmpty(map) || classifier == null) {
            return Collections.emptyMap();
        }
        Map<G, List<Map.Entry<K, V>>> result = new HashMap<>();
        for (Map.Entry<K, V> entry : map.entrySet()) {
            G groupKey = classifier.apply(entry);
            result.computeIfAbsent(groupKey, k -> new ArrayList<>()).add(entry);
        }
        return result;
    }

    /**
     * 多级 Map 安全读取（支持任意层级嵌套 Map）
     *
     * @param root 根 Map
     * @param path 访问路径（多个 key 依次嵌套）
     * @return 目标对象或 null
     */
    public static Object getDeepValue(Map<?, ?> root, Object... path) {
        if (isEmpty(root) || path == null || path.length == 0) {
            return null;
        }
        Object current = root;
        for (Object key : path) {
            if (!(current instanceof Map)) {
                return null;
            }
            current = ((Map<?, ?>) current).get(key);
            if (current == null) {
                return null;
            }
        }
        return current;
    }

    /**
     * 将 Map<String, String> 转换为 Properties 对象
     *
     * @param map 原始 Map
     * @return 转换后的 Properties，若 map 为 null 返回空 Properties
     */
    public static Properties toProperties(Map<String, String> map) {
        Properties props = new Properties();
        if (map != null) {
            for (Map.Entry<String, String> entry : map.entrySet()) {
                props.setProperty(entry.getKey(), entry.getValue());
            }
        }
        return props;
    }

    /**
     * 将 Properties 转换为 Map<String, String>
     *
     * @param props Properties 对象
     * @return 转换后的 Map
     */
    public static Map<String, String> fromProperties(Properties props) {
        if (props == null || props.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, String> map = new HashMap<>();
        for (String name : props.stringPropertyNames()) {
            map.put(name, props.getProperty(name));
        }
        return map;
    }

    /**
     * 将 Map<String, String> 转为 URL 查询字符串（key1=value1&key2=value2）
     *
     * @param map 原始 Map
     * @return 查询字符串，若 map 为空返回空串
     */
    public static String toQueryString(Map<String, String> map) {
        if (isEmpty(map)) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            sb.append(entry.getKey())
                    .append("=")
                    .append(entry.getValue())
                    .append("&");
        }
        if (sb.length() > 0) {
            // 移除尾部 &
            sb.setLength(sb.length() - 1);
        }
        return sb.toString();
    }

    /**
     * 将 URL 查询字符串解析为 Map<String, String>
     *
     * @param queryString 形如 "key1=value1&key2=value2" 的字符串
     * @return 解析后的 Map
     */
    public static Map<String, String> parseQueryString(String queryString) {
        if (queryString == null || queryString.trim().isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, String> map = new HashMap<>();
        String[] pairs = queryString.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf('=');
            if (idx > 0 && idx < pair.length() - 1) {
                String key = pair.substring(0, idx);
                String value = pair.substring(idx + 1);
                map.put(key, value);
            }
        }
        return map;
    }

    /**
     * 将 JavaBean 转为 Map<String, Object>
     *
     * @param bean JavaBean 对象
     * @return 属性名 -> 属性值 的 Map
     */
    public static Map<String, Object> beanToMap(Object bean) {
        if (bean == null) {
            return Collections.emptyMap();
        }
        Map<String, Object> map = new HashMap<>();
        try {
            Class<?> clazz = bean.getClass();
            for (Field field : clazz.getDeclaredFields()) {
                field.setAccessible(true);
                map.put(field.getName(), field.get(bean));
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException("bean 转 map 失败", e);
        }
        return map;
    }

    /**
     * 将 Map<String, Object> 映射为 JavaBean（通过属性名映射）
     *
     * @param map   属性 Map
     * @param clazz 目标 Bean 类型
     * @param <T>   泛型类型
     * @return 实例对象
     */
    public static <T> T mapToBean(Map<String, Object> map, Class<T> clazz) {
        if (map == null || clazz == null) {
            return null;
        }
        try {
            T obj = clazz.newInstance();
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                Field field;
                try {
                    field = clazz.getDeclaredField(entry.getKey());
                } catch (NoSuchFieldException e) {
                    continue; // 跳过不存在的字段
                }
                field.setAccessible(true);
                field.set(obj, entry.getValue());
            }
            return obj;
        } catch (Exception e) {
            throw new RuntimeException("map 转 bean 失败", e);
        }
    }

    /**
     * 原子添加键值对（如果 key 不存在）
     *
     * @param map   目标 ConcurrentMap
     * @param key   键
     * @param value 值
     * @param <K>   键类型
     * @param <V>   值类型
     * @return 旧值或 null（如果是第一次添加）
     */
    public static <K, V> V putIfAbsent(ConcurrentMap<K, V> map, K key, V value) {
        return map != null ? map.putIfAbsent(key, value) : null;
    }

    /**
     * 原子更新值（如果 key 存在）
     *
     * @param map    目标 ConcurrentMap
     * @param key    键
     * @param update 更新函数
     * @param <K>    键类型
     * @param <V>    值类型
     */
    public static <K, V> void updateIfPresent(ConcurrentMap<K, V> map, K key, Function<V, V> update) {
        if (map != null && key != null && update != null && map.containsKey(key)) {
            map.computeIfPresent(key, (k, oldVal) -> update.apply(oldVal));
        }
    }

    /**
     * 从 Map 中安全获取 Integer 类型值，转换失败或不存在返回默认值
     *
     * @param map          Map实例
     * @param key          键
     * @param defaultValue 默认值
     * @return Integer 类型值或默认值
     */
    public static Integer getInteger(Map<?, ?> map, Object key, Integer defaultValue) {
        if (map == null) {
            return defaultValue;
        }
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException ignored) {
            }
        }
        return defaultValue;
    }

    /**
     * 从 Map 中安全获取 Long 类型值，转换失败或不存在返回默认值
     *
     * @param map          Map实例
     * @param key          键
     * @param defaultValue 默认值
     * @return Long 类型值或默认值
     */
    public static Long getLong(Map<?, ?> map, Object key, Long defaultValue) {
        if (map == null) {
            return defaultValue;
        }
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException ignored) {
            }
        }
        return defaultValue;
    }

    /**
     * 从 Map 中安全获取 Boolean 类型值，转换失败或不存在返回默认值
     *
     * @param map          Map实例
     * @param key          键
     * @param defaultValue 默认值
     * @return Boolean 类型值或默认值
     */
    public static Boolean getBoolean(Map<?, ?> map, Object key, Boolean defaultValue) {
        if (map == null) {
            return defaultValue;
        }
        Object value = map.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return defaultValue;
    }

    /**
     * 从 Map 中安全获取 String 类型值，转换失败或不存在返回默认值
     *
     * @param map          Map实例
     * @param key          键
     * @param defaultValue 默认值
     * @return String 类型值或默认值
     */
    public static String getString(Map<?, ?> map, Object key, String defaultValue) {
        if (map == null) {
            return defaultValue;
        }
        Object value = map.get(key);
        if (value != null) {
            return value.toString();
        }
        return defaultValue;
    }

    /**
     * 从 Map 安全获取指定类型的值，类型不匹配时返回默认值
     *
     * @param map          Map 实例
     * @param key          键
     * @param clazz        目标类型 Class
     * @param defaultValue 默认值
     * @param <T>          类型泛型
     * @return 转换后的值或默认值
     */
    public static <T> T getTypedValue(Map<?, ?> map, Object key, Class<T> clazz, T defaultValue) {
        if (map == null || clazz == null) {
            return defaultValue;
        }
        Object value = map.get(key);
        if (clazz.isInstance(value)) {
            return clazz.cast(value);
        }
        return defaultValue;
    }

    /**
     * 深度复制 Map（仅支持 Map<K, V> 和 V 为 Map 类型的情况）
     *
     * @param original 原始 Map
     * @param <K>      键类型
     * @param <V>      值类型
     * @return 深拷贝的 Map
     */
    @SuppressWarnings("unchecked")
    public static <K, V> Map<K, V> deepCopy(Map<K, V> original) {
        if (original == null) {
            return null;
        }
        Map<K, V> copy = new HashMap<>();
        for (Map.Entry<K, V> entry : original.entrySet()) {
            K key = entry.getKey();
            V value = entry.getValue();
            if (value instanceof Map) {
                // 递归深拷贝
                copy.put(key, (V) deepCopy((Map<?, ?>) value));
            } else {
                copy.put(key, value);
            }
        }
        return copy;
    }

}
