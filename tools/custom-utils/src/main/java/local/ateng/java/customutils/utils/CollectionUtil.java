package local.ateng.java.customutils.utils;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 集合工具类
 * 提供常用集合处理方法
 *
 * @author Ateng
 * @since 2025-07-30
 */
public final class CollectionUtil {

    /**
     * 默认分隔符，逗号
     */
    public static final String DEFAULT_DELIMITER = ",";

    /**
     * 禁止实例化工具类
     */
    private CollectionUtil() {
        throw new UnsupportedOperationException("工具类不可实例化");
    }

    /**
     * 判断集合是否为 null 或空集合
     *
     * @param collection 输入集合
     * @return 为 null 或空集合返回 true，否则返回 false
     */
    public static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    /**
     * 判断集合是否不为空
     *
     * @param collection 输入集合
     * @return 不为 null 且不为空集合返回 true，否则返回 false
     */
    public static boolean isNotEmpty(Collection<?> collection) {
        return !isEmpty(collection);
    }

    /**
     * 判断 Map 是否为 null 或空
     *
     * @param map 输入 Map
     * @return 为 null 或空返回 true，否则返回 false
     */
    public static boolean isEmpty(Map<?, ?> map) {
        return map == null || map.isEmpty();
    }

    /**
     * 判断 Map 是否不为空
     *
     * @param map 输入 Map
     * @return 不为 null 且不为空返回 true，否则返回 false
     */
    public static boolean isNotEmpty(Map<?, ?> map) {
        return !isEmpty(map);
    }

    /**
     * 使用指定分隔符拼接字符串数组
     *
     * @param delimiter 分隔符
     * @param elements  元素数组
     * @return 拼接结果
     */
    public static String join(String delimiter, String... elements) {
        if (elements == null || elements.length == 0) {
            return "";
        }
        return String.join(delimiter, elements);
    }

    /**
     * 使用指定分隔符拼接集合中的字符串元素
     *
     * @param delimiter 分隔符
     * @param elements  字符串集合
     * @return 拼接结果，集合为空返回空字符串
     */
    public static String join(String delimiter, java.util.Collection<String> elements) {
        if (elements == null || elements.isEmpty()) {
            return "";
        }
        return String.join(delimiter, elements);
    }

    /**
     * 使用指定分隔符拼接对象数组，每个对象调用 toString() 方法
     *
     * @param delimiter 分隔符
     * @param elements  对象数组
     * @return 拼接结果，对象为 null 会被转换为 "null"
     */
    public static String joinObjects(String delimiter, Object... elements) {
        if (elements == null || elements.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < elements.length; i++) {
            if (i > 0) {
                sb.append(delimiter);
            }
            sb.append(elements[i] == null ? "null" : elements[i].toString());
        }
        return sb.toString();
    }

    /**
     * 使用指定分隔符拼接集合中的对象元素，每个对象调用 toString() 方法
     *
     * @param delimiter 分隔符
     * @param elements  对象集合
     * @return 拼接结果，集合为空返回空字符串
     */
    public static String joinObjects(String delimiter, java.util.Collection<?> elements) {
        if (elements == null || elements.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Object obj : elements) {
            if (!first) {
                sb.append(delimiter);
            } else {
                first = false;
            }
            sb.append(obj == null ? "null" : obj.toString());
        }
        return sb.toString();
    }

    /**
     * 使用默认分隔符拼接字符串数组
     *
     * @param elements 字符串数组
     * @return 拼接结果，数组为空返回空字符串
     */
    public static String join(String... elements) {
        return join(DEFAULT_DELIMITER, elements);
    }

    /**
     * 使用默认分隔符拼接字符串集合
     *
     * @param elements 字符串集合
     * @return 拼接结果，集合为空返回空字符串
     */
    public static String join(java.util.Collection<String> elements) {
        return join(DEFAULT_DELIMITER, elements);
    }

    /**
     * 使用默认分隔符拼接对象数组
     *
     * @param elements 对象数组
     * @return 拼接结果，数组为空返回空字符串
     */
    public static String joinObjects(Object... elements) {
        return joinObjects(DEFAULT_DELIMITER, elements);
    }

    /**
     * 使用默认分隔符拼接对象集合
     *
     * @param elements 对象集合
     * @return 拼接结果，集合为空返回空字符串
     */
    public static String joinObjects(java.util.Collection<?> elements) {
        return joinObjects(DEFAULT_DELIMITER, elements);
    }

    /**
     * 将字符串按分隔符分割成数组（空字符串或 null 返回空数组）
     *
     * @param str       原始字符串
     * @param delimiter 分隔符
     * @return 分割后的字符串数组
     */
    public static String[] split(String str, String delimiter) {
        if (str == null || str.trim().isEmpty() || delimiter == null) {
            return new String[0];
        }
        return str.split(Pattern.quote(delimiter));
    }

    /**
     * 将字符串按分隔符拆分为列表
     *
     * @param str       原始字符串
     * @param delimiter 分隔符（如 ","）
     * @return 拆分后的 List，空字符串返回空列表
     */
    public static List<String> splitToList(String str, String delimiter) {
        if (str == null || str.isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.asList(str.split(Pattern.quote(delimiter)));
    }

    /**
     * 将字符串按分隔符拆分为列表
     *
     * @param str 原始字符串
     * @return 拆分后的 List，空字符串返回空列表
     */
    public static List<String> splitToList(String str) {
        if (str == null || str.isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.asList(str.split(Pattern.quote(DEFAULT_DELIMITER)));
    }

    /**
     * 将字符串按分隔符拆分为 Set，自动去重
     *
     * @param str       原始字符串
     * @param delimiter 分隔符
     * @return 拆分后的 Set
     */
    public static Set<String> splitToSet(String str, String delimiter) {
        if (str == null || str.isEmpty()) {
            return Collections.emptySet();
        }
        return Arrays.stream(str.split(Pattern.quote(delimiter)))
                .collect(Collectors.toSet());
    }

    /**
     * 按指定分隔符分割字符串，并转换为指定类型的列表
     *
     * @param <T>       目标类型
     * @param str       原始字符串
     * @param delimiter 分隔符
     * @param converter 转换函数，将字符串转为 T 类型
     * @return 转换成功的 T 类型列表，字符串为空或无有效元素时返回空列表
     */
    public static <T> List<T> splitToList(String str, String delimiter, Function<String, T> converter) {
        List<T> result = new ArrayList<>();
        if (str == null || delimiter == null || converter == null) {
            return result;
        }
        String[] parts = str.split(Pattern.quote(delimiter));
        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            try {
                T value = converter.apply(trimmed);
                if (value != null) {
                    result.add(value);
                }
            } catch (Exception e) {
                // 转换失败时跳过该元素，防止抛出异常
            }
        }
        return result;
    }

    /**
     * 按默认分隔符分割字符串，并转换为指定类型的列表
     *
     * @param <T>       目标类型
     * @param str       原始字符串
     * @param converter 转换函数，将字符串转为 T 类型
     * @return 转换成功的 T 类型列表，字符串为空或无有效元素时返回空列表
     */
    public static <T> List<T> splitToList(String str, Function<String, T> converter) {
        return splitToList(str, DEFAULT_DELIMITER, converter);
    }

    /**
     * 将字符串按分隔符拆分为指定类型的 List
     *
     * @param str         待拆分字符串
     * @param delimiter   分隔符
     * @param converter   转换函数，将每个分隔后的字符串转换为目标类型
     * @param ignoreError 是否忽略转换异常，true 忽略并跳过错误数据，false 抛出异常
     * @param <T>         目标类型
     * @return 转换后的 List，输入为空返回空列表
     */
    public static <T> List<T> splitToList(String str, String delimiter, Function<String, T> converter, boolean ignoreError) {
        if (str == null || str.isEmpty()) {
            return Collections.emptyList();
        }
        if (delimiter == null || delimiter.isEmpty()) {
            throw new IllegalArgumentException("分隔符不能为空");
        }
        if (converter == null) {
            throw new IllegalArgumentException("转换函数不能为空");
        }

        String[] parts = str.split(delimiter);
        List<T> result = new ArrayList<>(parts.length);

        for (String part : parts) {
            try {
                result.add(converter.apply(part));
            } catch (Exception e) {
                if (!ignoreError) {
                    throw e instanceof RuntimeException ? (RuntimeException) e : new RuntimeException(e);
                }
                // 忽略错误时跳过该元素
            }
        }
        return result;
    }

    /**
     * 按指定分隔符拆分字符串并转换为 Integer 类型列表
     *
     * @param str       需要拆分的字符串
     * @param delimiter 分隔符字符串
     * @return Integer 类型列表，转换失败的元素会被跳过
     */
    public static List<Integer> splitToIntegerList(String str, String delimiter) {
        return splitToList(str, delimiter, Integer::parseInt);
    }

    /**
     * 按指定分隔符拆分字符串并转换为 Long 类型列表
     *
     * @param str       需要拆分的字符串
     * @param delimiter 分隔符字符串
     * @return Long 类型列表，转换失败的元素会被跳过
     */
    public static List<Long> splitToLongList(String str, String delimiter) {
        return splitToList(str, delimiter, Long::parseLong);
    }

    /**
     * 按指定分隔符拆分字符串并转换为 Double 类型列表
     *
     * @param str       需要拆分的字符串
     * @param delimiter 分隔符字符串
     * @return Double 类型列表，转换失败的元素会被跳过
     */
    public static List<Double> splitToDoubleList(String str, String delimiter) {
        return splitToList(str, delimiter, Double::parseDouble);
    }

    /**
     * 按指定分隔符拆分字符串并转换为 Boolean 类型列表
     *
     * <p>转换规则为忽略大小写的 true/false，非 true 字符串均为 false。</p>
     *
     * @param str       需要拆分的字符串
     * @param delimiter 分隔符字符串
     * @return Boolean 类型列表，转换失败的元素会被跳过
     */
    public static List<Boolean> splitToBooleanList(String str, String delimiter) {
        return splitToList(str, delimiter, s -> Boolean.parseBoolean(s.toLowerCase()));
    }

    /**
     * 按指定分隔符拆分字符串并转换为去除空白字符串的列表
     *
     * @param str       需要拆分的字符串
     * @param delimiter 分隔符字符串
     * @return 非空字符串列表
     */
    public static List<String> splitToStringList(String str, String delimiter) {
        return splitToList(str, delimiter, s -> s);
    }

    /**
     * 获取集合的第一个元素
     *
     * @param collection 输入集合
     * @param <T>        元素类型
     * @return 第一个元素，若集合为空返回 null
     */
    public static <T> T getFirst(Collection<T> collection) {
        if (isEmpty(collection)) {
            return null;
        }
        return collection.iterator().next();
    }

    /**
     * 安全获取集合长度
     *
     * @param collection 输入集合
     * @return 集合长度，若为 null 则返回 0
     */
    public static int size(Collection<?> collection) {
        return collection == null ? 0 : collection.size();
    }

    /**
     * 安全获取 Map 的大小
     *
     * @param map 输入 Map
     * @return Map 大小，若为 null 则返回 0
     */
    public static int size(Map<?, ?> map) {
        return map == null ? 0 : map.size();
    }

    /**
     * 对集合进行去重（使用元素的 equals/hashCode）
     *
     * @param collection 输入集合
     * @param <T>        元素类型
     * @return 去重后的列表，若输入为 null 返回空列表
     */
    public static <T> List<T> distinct(Collection<T> collection) {
        if (isEmpty(collection)) {
            return Collections.emptyList();
        }
        return collection.stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * 过滤集合中符合条件的元素
     *
     * @param collection 输入集合
     * @param predicate  过滤条件
     * @param <T>        元素类型
     * @return 过滤后的列表，若输入为 null 返回空列表
     */
    public static <T> List<T> filter(Collection<T> collection, Predicate<T> predicate) {
        if (isEmpty(collection) || predicate == null) {
            return Collections.emptyList();
        }
        return collection.stream()
                .filter(predicate)
                .collect(Collectors.toList());
    }

    /**
     * 将集合元素映射为另一种类型
     *
     * @param collection 输入集合
     * @param mapper     映射函数
     * @param <T>        原始类型
     * @param <R>        映射后类型
     * @return 映射后的列表，若输入为 null 返回空列表
     */
    public static <T, R> List<R> map(Collection<T> collection, Function<T, R> mapper) {
        if (isEmpty(collection) || mapper == null) {
            return Collections.emptyList();
        }
        return collection.stream()
                .map(mapper)
                .collect(Collectors.toList());
    }

    /**
     * 将集合按照指定键进行分组
     *
     * @param collection 输入集合
     * @param classifier 分组依据函数
     * @param <T>        元素类型
     * @param <K>        分组 key 类型
     * @return 分组后的 Map，若输入为 null 返回空 Map
     */
    public static <T, K> Map<K, List<T>> groupBy(Collection<T> collection, Function<T, K> classifier) {
        if (isEmpty(collection) || classifier == null) {
            return Collections.emptyMap();
        }
        return collection.stream()
                .collect(Collectors.groupingBy(classifier));
    }

    /**
     * 将集合转为 Set（自动去重）
     *
     * @param collection 输入集合
     * @param <T>        元素类型
     * @return 去重后的 Set，若输入为 null 返回空 Set
     */
    public static <T> Set<T> toSet(Collection<T> collection) {
        if (isEmpty(collection)) {
            return Collections.emptySet();
        }
        return collection.stream()
                .collect(Collectors.toSet());
    }

    /**
     * 将任意类型的 List 转换为 List<String>，null 元素转换为空字符串
     *
     * @param list 待转换列表
     * @param <T>  原列表元素类型
     * @return 转换后的字符串列表，输入为 null 返回空列表
     */
    public static <T> List<String> toStringList(List<T> list) {
        if (list == null) {
            return Collections.emptyList();
        }
        return list.stream()
                .map(item -> item == null ? "" : item.toString())
                .collect(Collectors.toList());
    }

    /**
     * 判断集合中是否存在满足条件的元素
     *
     * @param collection 输入集合
     * @param predicate  条件判断
     * @param <T>        元素类型
     * @return 存在返回 true，否则 false
     */
    public static <T> boolean exists(Collection<T> collection, Predicate<T> predicate) {
        if (isEmpty(collection) || predicate == null) {
            return false;
        }
        return collection.stream().anyMatch(predicate);
    }

    /**
     * 对集合进行排序
     *
     * @param collection 输入集合
     * @param comparator 比较器
     * @param <T>        元素类型
     * @return 排序后的列表，若输入为 null 返回空列表
     */
    public static <T> List<T> sort(Collection<T> collection, Comparator<T> comparator) {
        if (isEmpty(collection)) {
            return Collections.emptyList();
        }
        return collection.stream()
                .sorted(comparator)
                .collect(Collectors.toList());
    }

    /**
     * 反转列表中的元素顺序
     *
     * @param list 输入列表
     * @param <T>  元素类型
     * @return 反转后的新列表，若输入为 null 返回空列表
     */
    public static <T> List<T> reverse(List<T> list) {
        if (isEmpty(list)) {
            return Collections.emptyList();
        }
        List<T> reversed = new ArrayList<>(list);
        Collections.reverse(reversed);
        return reversed;
    }

    /**
     * 查找集合中第一个满足条件的元素
     *
     * @param collection 输入集合
     * @param predicate  条件判断
     * @param <T>        元素类型
     * @return 第一个符合条件的元素，若无匹配或集合为 null 返回 null
     */
    public static <T> T findFirst(Collection<T> collection, Predicate<T> predicate) {
        if (isEmpty(collection) || predicate == null) {
            return null;
        }
        return collection.stream()
                .filter(predicate)
                .findFirst()
                .orElse(null);
    }

    /**
     * 查找集合中最后一个满足条件的元素
     *
     * @param collection 输入集合
     * @param predicate  条件判断
     * @param <T>        元素类型
     * @return 最后一个符合条件的元素，若无匹配或集合为 null 返回 null
     */
    public static <T> T findLast(Collection<T> collection, Predicate<T> predicate) {
        if (isEmpty(collection) || predicate == null) {
            return null;
        }
        List<T> list = collection.stream()
                .filter(predicate)
                .collect(Collectors.toList());
        return list.isEmpty() ? null : list.get(list.size() - 1);
    }

    /**
     * 将集合元素按照 key 映射为 Map（key 冲突会覆盖）
     *
     * @param collection 输入集合
     * @param keyMapper  key 映射函数
     * @param <T>        元素类型
     * @param <K>        key 类型
     * @return 转换后的 Map，若输入为 null 返回空 Map
     */
    public static <T, K> Map<K, T> toMap(Collection<T> collection, Function<T, K> keyMapper) {
        if (isEmpty(collection) || keyMapper == null) {
            return Collections.emptyMap();
        }
        return collection.stream()
                .collect(Collectors.toMap(
                        keyMapper,
                        Function.identity(),
                        (v1, v2) -> v2
                ));
    }

    /**
     * 将集合元素按照 key 和 value 映射为 Map（key 冲突会覆盖）
     *
     * @param collection  输入集合
     * @param keyMapper   key 映射函数
     * @param valueMapper value 映射函数
     * @param <T>         原始元素类型
     * @param <K>         key 类型
     * @param <V>         value 类型
     * @return 转换后的 Map，若输入为 null 返回空 Map
     */
    public static <T, K, V> Map<K, V> toMap(Collection<T> collection, Function<T, K> keyMapper, Function<T, V> valueMapper) {
        if (isEmpty(collection) || keyMapper == null || valueMapper == null) {
            return Collections.emptyMap();
        }
        return collection.stream()
                .collect(Collectors.toMap(
                        keyMapper,
                        valueMapper,
                        (v1, v2) -> v2
                ));
    }

    /**
     * 将集合中元素进行扁平化处理（flatMap）
     *
     * @param collection 输入集合
     * @param mapper     映射为子集合的函数
     * @param <T>        原始元素类型
     * @param <R>        映射后元素类型
     * @return 扁平化后的列表，若输入为 null 返回空列表
     */
    public static <T, R> List<R> flatMap(Collection<T> collection, Function<T, Collection<R>> mapper) {
        if (isEmpty(collection) || mapper == null) {
            return Collections.emptyList();
        }
        return collection.stream()
                .filter(Objects::nonNull)
                .map(mapper)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    /**
     * 移除集合中的 null 元素
     *
     * @param collection 输入集合
     * @param <T>        元素类型
     * @return 移除 null 后的新列表，若输入为 null 返回空列表
     */
    public static <T> List<T> removeNulls(Collection<T> collection) {
        if (isEmpty(collection)) {
            return Collections.emptyList();
        }
        return collection.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 合并多个列表为一个列表
     *
     * @param lists 多个列表
     * @param <T>   元素类型
     * @return 合并后的新列表，若所有列表为 null 返回空列表
     */
    @SafeVarargs
    public static <T> List<T> mergeList(List<T>... lists) {
        if (lists == null || lists.length == 0) {
            return Collections.emptyList();
        }
        return Arrays.stream(lists)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    /**
     * 将集合按条件拆分为两个列表
     *
     * @param collection 输入集合
     * @param predicate  条件判断函数
     * @param <T>        元素类型
     * @return 拆分结果，Map 中 key 为 true 表示满足条件的元素，false 表示不满足条件的元素
     */
    public static <T> Map<Boolean, List<T>> partitionBy(Collection<T> collection, Predicate<T> predicate) {
        if (isEmpty(collection) || predicate == null) {
            Map<Boolean, List<T>> result = new HashMap<>();
            result.put(true, Collections.emptyList());
            result.put(false, Collections.emptyList());
            return result;
        }
        return collection.stream()
                .collect(Collectors.partitioningBy(predicate));
    }

    /**
     * 判断集合中所有元素是否都满足指定条件
     *
     * @param collection 输入集合
     * @param predicate  条件判断函数
     * @param <T>        元素类型
     * @return 所有元素都满足条件返回 true，否则返回 false，若集合为空也返回 true
     */
    public static <T> boolean allMatch(Collection<T> collection, Predicate<T> predicate) {
        if (predicate == null) {
            return false;
        }
        return collection == null || collection.stream().allMatch(predicate);
    }

    /**
     * 判断集合中是否存在至少一个元素满足给定条件
     *
     * @param collection 输入集合
     * @param predicate  条件函数
     * @param <T>        元素类型
     * @return 存在满足条件元素返回 true，否则 false
     */
    public static <T> boolean anyMatch(Collection<T> collection, Predicate<? super T> predicate) {
        if (predicate == null) {
            return false;
        }
        return collection == null || collection.stream().anyMatch(predicate);
    }

    /**
     * 判断集合中是否所有元素都不满足指定条件
     *
     * @param collection 输入集合
     * @param predicate  条件判断函数
     * @param <T>        元素类型
     * @return 所有元素都不满足条件返回 true，否则返回 false
     */
    public static <T> boolean noneMatch(Collection<T> collection, Predicate<T> predicate) {
        if (isEmpty(collection) || predicate == null) {
            return true;
        }
        return collection.stream().noneMatch(predicate);
    }

    /**
     * 对集合中数值进行求和
     *
     * @param collection 输入集合
     * @param mapper     数值映射函数
     * @param <T>        元素类型
     * @return 求和结果，若为空返回 0
     */
    public static <T> double sum(Collection<T> collection, ToDoubleFunction<T> mapper) {
        if (isEmpty(collection) || mapper == null) {
            return 0;
        }
        return collection.stream()
                .mapToDouble(mapper)
                .sum();
    }

    /**
     * 计算集合中数值的平均值
     *
     * @param collection 输入集合
     * @param mapper     数值映射函数
     * @param <T>        元素类型
     * @return 平均值，若集合为空返回 0
     */
    public static <T> double average(Collection<T> collection, ToDoubleFunction<T> mapper) {
        if (isEmpty(collection) || mapper == null) {
            return 0;
        }
        return collection.stream()
                .mapToDouble(mapper)
                .average()
                .orElse(0);
    }

    /**
     * 获取集合中最大元素（自定义比较器）
     *
     * @param collection 输入集合
     * @param comparator 比较器
     * @param <T>        元素类型
     * @return 最大元素，若集合为空返回 null
     */
    public static <T> T maxBy(Collection<T> collection, Comparator<T> comparator) {
        if (isEmpty(collection) || comparator == null) {
            return null;
        }
        return collection.stream()
                .max(comparator)
                .orElse(null);
    }

    /**
     * 获取集合中最小元素（自定义比较器）
     *
     * @param collection 输入集合
     * @param comparator 比较器
     * @param <T>        元素类型
     * @return 最小元素，若集合为空返回 null
     */
    public static <T> T minBy(Collection<T> collection, Comparator<T> comparator) {
        if (isEmpty(collection) || comparator == null) {
            return null;
        }
        return collection.stream()
                .min(comparator)
                .orElse(null);
    }

    /**
     * 查找元素在列表中的索引位置（基于 equals 比较）
     *
     * @param list  输入列表
     * @param value 目标值
     * @param <T>   元素类型
     * @return 索引位置，若未找到返回 -1
     */
    public static <T> int indexOf(List<T> list, T value) {
        if (isEmpty(list)) {
            return -1;
        }
        for (int i = 0; i < list.size(); i++) {
            if (Objects.equals(list.get(i), value)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 移除集合中满足条件的元素（原地操作）
     *
     * @param collection 输入集合
     * @param predicate  条件函数
     * @param <T>        元素类型
     * @return 是否移除过元素，若集合为空返回 false
     */
    public static <T> boolean removeIf(Collection<T> collection, Predicate<T> predicate) {
        if (isEmpty(collection) || predicate == null) {
            return false;
        }
        return collection.removeIf(predicate);
    }

    /**
     * 将集合按指定大小进行切分
     *
     * @param collection 输入集合
     * @param size       每组大小，必须大于 0
     * @param <T>        元素类型
     * @return 分组后的列表，若输入为空返回空列表
     * @throws IllegalArgumentException 如果 size 小于等于 0
     */
    public static <T> List<List<T>> chunk(Collection<T> collection, int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("分组大小必须大于 0");
        }
        if (isEmpty(collection)) {
            return Collections.emptyList();
        }

        List<T> list = new ArrayList<>(collection);
        List<List<T>> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            int end = Math.min(i + size, list.size());
            result.add(list.subList(i, end));
        }
        return result;
    }

    /**
     * 将两个集合合并为键值对列表（长度取短的一方）
     *
     * @param keys   key 集合
     * @param values value 集合
     * @param <K>    key 类型
     * @param <V>    value 类型
     * @return 合并后的列表，元素为 Map.Entry<K, V>，若任意集合为空返回空列表
     */
    public static <K, V> List<Map.Entry<K, V>> zip(Collection<K> keys, Collection<V> values) {
        if (isEmpty(keys) || isEmpty(values)) {
            return Collections.emptyList();
        }

        Iterator<K> keyIt = keys.iterator();
        Iterator<V> valIt = values.iterator();
        List<Map.Entry<K, V>> result = new ArrayList<>();

        while (keyIt.hasNext() && valIt.hasNext()) {
            K k = keyIt.next();
            V v = valIt.next();
            result.add(new AbstractMap.SimpleEntry<>(k, v));
        }

        return result;
    }

    /**
     * 将两个集合合并为一个 Map（长度取短的一方）
     *
     * @param keys   key 集合
     * @param values value 集合
     * @param <K>    key 类型
     * @param <V>    value 类型
     * @return 合并后的 Map，若任意集合为空返回空 Map
     */
    public static <K, V> Map<K, V> zipToMap(Collection<K> keys, Collection<V> values) {
        if (isEmpty(keys) || isEmpty(values)) {
            return Collections.emptyMap();
        }

        Iterator<K> keyIt = keys.iterator();
        Iterator<V> valIt = values.iterator();
        Map<K, V> result = new LinkedHashMap<>();

        while (keyIt.hasNext() && valIt.hasNext()) {
            result.put(keyIt.next(), valIt.next());
        }

        return result;
    }

    /**
     * 判断两个集合内容是否相同（忽略顺序）
     *
     * @param a   集合 A
     * @param b   集合 B
     * @param <T> 元素类型
     * @return 若内容相同（不比较顺序）返回 true，否则 false
     */
    public static <T> boolean isEqualIgnoreOrder(Collection<T> a, Collection<T> b) {
        if (a == null && b == null) {
            return true;
        }
        if (a == null || b == null || a.size() != b.size()) {
            return false;
        }
        return new HashSet<>(a).equals(new HashSet<>(b));
    }

    /**
     * 将 Map<K, V> 转换为 List<Map.Entry<K, V>>
     *
     * @param map 输入 Map
     * @param <K> key 类型
     * @param <V> value 类型
     * @return Entry 列表，若为 null 返回空列表
     */
    public static <K, V> List<Map.Entry<K, V>> toEntryList(Map<K, V> map) {
        if (map == null || map.isEmpty()) {
            return Collections.emptyList();
        }
        return new ArrayList<>(map.entrySet());
    }

    /**
     * 根据指定 keyList 顺序，提取 Map 中匹配的键值对（保持顺序）
     *
     * @param map     原始 Map
     * @param keyList 需要的 key 顺序
     * @param <K>     key 类型
     * @param <V>     value 类型
     * @return 有序 Map，按 keyList 顺序排列，若 key 不存在则跳过
     */
    public static <K, V> Map<K, V> extractOrdered(Map<K, V> map, List<K> keyList) {
        if (map == null || isEmpty(keyList)) {
            return Collections.emptyMap();
        }

        Map<K, V> result = new LinkedHashMap<>();
        for (K key : keyList) {
            if (map.containsKey(key)) {
                result.put(key, map.get(key));
            }
        }
        return result;
    }

    /**
     * 判断两个集合是否存在交集
     *
     * @param a   集合 A
     * @param b   集合 B
     * @param <T> 元素类型
     * @return 存在交集返回 true，否则返回 false
     */
    public static <T> boolean hasIntersection(Collection<T> a, Collection<T> b) {
        if (isEmpty(a) || isEmpty(b)) {
            return false;
        }
        Set<T> set = new HashSet<>(a);
        for (T item : b) {
            if (set.contains(item)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取两个集合的交集
     *
     * @param a   集合 A
     * @param b   集合 B
     * @param <T> 元素类型
     * @return 交集集合，若无交集返回空集合
     */
    public static <T> Set<T> intersection(Collection<T> a, Collection<T> b) {
        if (isEmpty(a) || isEmpty(b)) {
            return Collections.emptySet();
        }
        Set<T> result = new HashSet<>(a);
        result.retainAll(b);
        return result;
    }

    /**
     * 获取两个集合的差集（只保留存在于 a 但不在 b 中的元素）
     *
     * @param a   集合 A
     * @param b   集合 B
     * @param <T> 元素类型
     * @return 差集集合，若无差异返回空集合
     */
    public static <T> Set<T> difference(Collection<T> a, Collection<T> b) {
        if (isEmpty(a)) {
            return Collections.emptySet();
        }
        if (isEmpty(b)) {
            return new HashSet<>(a);
        }
        Set<T> result = new HashSet<>(a);
        result.removeAll(b);
        return result;
    }

    /**
     * 获取两个集合的并集（自动去重）
     *
     * @param a   集合 A
     * @param b   集合 B
     * @param <T> 元素类型
     * @return 并集集合，若均为空返回空集合
     */
    public static <T> Set<T> union(Collection<T> a, Collection<T> b) {
        Set<T> result = new HashSet<>();
        if (!isEmpty(a)) {
            result.addAll(a);
        }
        if (!isEmpty(b)) {
            result.addAll(b);
        }
        return result;
    }

    /**
     * 统计集合中满足指定条件的元素数量
     *
     * @param collection 输入集合
     * @param predicate  判断条件
     * @param <T>        元素类型
     * @return 满足条件的数量
     */
    public static <T> long countBy(Collection<T> collection, Predicate<T> predicate) {
        if (isEmpty(collection) || predicate == null) {
            return 0;
        }
        return collection.stream()
                .filter(predicate)
                .count();
    }

    /**
     * 对集合元素按照指定键分组并统计每组数量
     *
     * @param collection 输入集合
     * @param classifier 分组函数
     * @param <T>        元素类型
     * @param <K>        分组 key 类型
     * @return 分组计数 Map，若输入为空返回空 Map
     */
    public static <T, K> Map<K, Long> groupCount(Collection<T> collection, Function<T, K> classifier) {
        if (isEmpty(collection) || classifier == null) {
            return Collections.emptyMap();
        }
        return collection.stream()
                .collect(Collectors.groupingBy(classifier, Collectors.counting()));
    }

    /**
     * 将集合元素按照指定键进行一对多分组（Multimap）
     *
     * @param collection 输入集合
     * @param classifier 分组函数
     * @param <T>        元素类型
     * @param <K>        分组 key 类型
     * @return 分组后的 Map，key 对应多个值，若输入为空返回空 Map
     */
    public static <T, K> Map<K, List<T>> toMultimap(Collection<T> collection, Function<T, K> classifier) {
        if (isEmpty(collection) || classifier == null) {
            return Collections.emptyMap();
        }
        return collection.stream()
                .collect(Collectors.groupingBy(classifier));
    }

    /**
     * 将集合元素映射为多个元素后，按 key 分组（扁平化分组）
     *
     * @param collection 输入集合
     * @param flatMapper 元素映射为子集合函数
     * @param keyMapper  每个子元素的 key 提取函数
     * @param <T>        原始类型
     * @param <K>        分组 key 类型
     * @param <R>        子元素类型
     * @return 分组后的 Map，key 对应多个值
     */
    public static <T, K, R> Map<K, List<R>> flatGroup(Collection<T> collection,
                                                      Function<T, Collection<R>> flatMapper,
                                                      Function<R, K> keyMapper) {
        if (isEmpty(collection) || flatMapper == null || keyMapper == null) {
            return Collections.emptyMap();
        }
        return collection.stream()
                .filter(Objects::nonNull)
                .map(flatMapper)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .collect(Collectors.groupingBy(keyMapper));
    }

    /**
     * 合并多个 Map 为一个 List<Map.Entry<K, V>>，按插入顺序保留
     *
     * @param maps 多个 Map
     * @param <K>  key 类型
     * @param <V>  value 类型
     * @return 合并后的条目列表，若无有效 Map 返回空列表
     */
    @SafeVarargs
    public static <K, V> List<Map.Entry<K, V>> mergeMapList(Map<K, V>... maps) {
        if (maps == null || maps.length == 0) {
            return Collections.emptyList();
        }
        List<Map.Entry<K, V>> result = new ArrayList<>();
        for (Map<K, V> map : maps) {
            if (map != null && !map.isEmpty()) {
                result.addAll(map.entrySet());
            }
        }
        return result;
    }

    /**
     * 基于字段名对对象集合排序（需实现 getter，反射方式）
     *
     * @param list      对象列表
     * @param fieldName 字段名称
     * @param ascending 是否升序
     * @param <T>       对象类型
     * @return 排序后的新列表，若输入为 null 返回空列表
     */
    public static <T> List<T> sortByField(List<T> list, String fieldName, boolean ascending) {
        if (isEmpty(list) || StringUtil.isEmpty(fieldName)) {
            return Collections.emptyList();
        }

        List<T> copy = new ArrayList<>(list);
        copy.sort((o1, o2) -> {
            try {
                Object v1 = getFieldValue(o1, fieldName);
                Object v2 = getFieldValue(o2, fieldName);

                if (v1 instanceof Comparable && v2 instanceof Comparable) {
                    @SuppressWarnings("unchecked")
                    int cmp = ((Comparable<Object>) v1).compareTo(v2);
                    return ascending ? cmp : -cmp;
                }
            } catch (Exception ignored) {
            }
            return 0;
        });

        return copy;
    }

    /**
     * 获取对象的字段值（反射方式）
     *
     * @param obj       对象
     * @param fieldName 字段名
     * @return 字段值
     * @throws ReflectiveOperationException 反射异常
     */
    private static Object getFieldValue(Object obj, String fieldName) throws ReflectiveOperationException {
        Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(obj);
    }

    /**
     * 根据 key 提取函数去重
     *
     * @param collection 输入集合
     * @param keyMapper  唯一标识函数
     * @param <T>        元素类型
     * @param <K>        唯一 key 类型
     * @return 去重后的列表，若输入为 null 返回空列表
     */
    public static <T, K> List<T> deduplicateByKey(Collection<T> collection, Function<T, K> keyMapper) {
        if (isEmpty(collection) || keyMapper == null) {
            return Collections.emptyList();
        }

        Set<K> seen = new HashSet<>();
        List<T> result = new ArrayList<>();

        for (T item : collection) {
            if (item == null) {
                continue;
            }
            K key = keyMapper.apply(item);
            if (seen.add(key)) {
                result.add(item);
            }
        }

        return result;
    }

    /**
     * 将集合分页
     *
     * @param list     原始列表
     * @param page     页码（从 1 开始）
     * @param pageSize 每页大小
     * @param <T>      元素类型
     * @return 当前页的子列表，超出范围返回空列表
     */
    public static <T> List<T> paginate(List<T> list, int page, int pageSize) {
        if (isEmpty(list) || page <= 0 || pageSize <= 0) {
            return Collections.emptyList();
        }

        int fromIndex = (page - 1) * pageSize;
        if (fromIndex >= list.size()) {
            return Collections.emptyList();
        }

        int toIndex = Math.min(fromIndex + pageSize, list.size());
        return list.subList(fromIndex, toIndex);
    }

    /**
     * 将平铺结构的列表转换为树形结构（支持单个根父 ID，递归构建，支持循环引用检测）
     *
     * <p>此方法为便捷版，内部直接调用 {@link #buildMultiRootTree(List, Function, Function, BiConsumer, Set)}，
     * 适用于只有一个根父 ID 的情况。</p>
     *
     * @param items          原始列表（平铺结构）
     * @param idGetter       获取当前节点 ID 的函数，例如：Menu::getId
     * @param parentGetter   获取父节点 ID 的函数，例如：Menu::getParentId
     * @param childrenSetter 设置子节点列表的函数，例如：Menu::setChildren
     * @param rootParentId   根节点父 ID（例如 0 或 null）
     * @param <T>            元素类型
     * @param <K>            ID 类型
     * @return 树形结构的根节点列表（可能为空列表）
     *
     * <p><b>示例实体类：</b></p>
     * <pre>{@code
     * public class Menu {
     *     private Integer id;             // 当前节点 ID
     *     private Integer parentId;       // 父节点 ID
     *     private String name;            // 节点名称（可选）
     *     private List<Menu> children;    // 子节点列表
     *
     *     // 构造方法、getter、setter 略
     * }
     *
     * List<Menu> menus = Arrays.asList(
     *     new Menu(1, 0, "系统管理"),
     *     new Menu(2, 1, "用户管理"),
     *     new Menu(3, 1, "角色管理")
     * );
     *
     * List<Menu> tree = CollectionUtil.buildTree(
     *     menus,
     *     Menu::getId,
     *     Menu::getParentId,
     *     Menu::setChildren,
     *     0
     * );
     * }</pre>
     */
    public static <T, K> List<T> buildTree(List<T> items,
                                           Function<T, K> idGetter,
                                           Function<T, K> parentGetter,
                                           BiConsumer<T, List<T>> childrenSetter,
                                           K rootParentId) {
        return buildMultiRootTree(items, idGetter, parentGetter, childrenSetter, Collections.singleton(rootParentId));
    }

    /**
     * 将平铺结构的列表转换为树形结构（支持多个根父 ID，递归构建，支持循环引用检测）
     *
     * @param items          原始列表（平铺结构）
     * @param idGetter       获取当前节点 ID 的函数，例如：Menu::getId
     * @param parentGetter   获取父节点 ID 的函数，例如：Menu::getParentId
     * @param childrenSetter 设置子节点列表的函数，例如：Menu::setChildren
     * @param rootParentIds  根节点父 ID 集合（任意一个匹配即为根节点，例如 0、-1、null）
     * @param <T>            元素类型
     * @param <K>            ID 类型
     * @return 树形结构的根节点列表（可能为空列表）
     *
     * <p><b>示例实体类：</b></p>
     * <pre>{@code
     * public class Menu {
     *     private Integer id;             // 当前节点 ID
     *     private Integer parentId;       // 父节点 ID
     *     private String name;            // 节点名称（可选）
     *     private List<Menu> children;    // 子节点列表
     *
     *     // 构造方法、getter、setter 略
     * }
     *
     * List<Menu> menus = Arrays.asList(
     *     new Menu(1, 0, "系统管理"),
     *     new Menu(2, 1, "用户管理"),
     *     new Menu(3, 1, "角色管理"),
     *     new Menu(4, -1, "内容管理")
     * );
     *
     * // 支持多个根父 ID（0 和 -1 都是根节点）
     * List<Menu> tree = CollectionUtil.buildMultiRootTree(
     *     menus,
     *     Menu::getId,
     *     Menu::getParentId,
     *     Menu::setChildren,
     *     new HashSet<>(Arrays.asList(0, -1))
     * );
     * }</pre>
     */
    public static <T, K> List<T> buildMultiRootTree(List<T> items,
                                                    Function<T, K> idGetter,
                                                    Function<T, K> parentGetter,
                                                    BiConsumer<T, List<T>> childrenSetter,
                                                    Set<K> rootParentIds) {

        if (items == null || items.isEmpty()
                || idGetter == null || parentGetter == null || childrenSetter == null
                || rootParentIds == null || rootParentIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 1) 建立 id -> 节点 映射（使用 LinkedHashMap 保序；遇到重复 id 保留第一个）
        Map<K, T> idMap = new LinkedHashMap<>();
        for (T item : items) {
            if (item == null) {
                continue;
            }
            K id = idGetter.apply(item);
            idMap.putIfAbsent(id, item);
        }

        // 2) 建立 parentId -> 子节点 桶
        Map<K, List<T>> bucket = new HashMap<>();
        for (T item : items) {
            if (item == null) {
                continue;
            }
            K p = parentGetter.apply(item);
            bucket.computeIfAbsent(p, k -> new ArrayList<>()).add(item);
        }

        // 3) 为每个节点设置 children（无则设空列表；顺带规避“自指向”造成的显式自循环）
        for (T item : items) {
            if (item == null) {
                continue;
            }
            K id = idGetter.apply(item);
            List<T> raw = bucket.get(id);
            if (raw == null || raw.isEmpty()) {
                childrenSetter.accept(item, new ArrayList<>());
            } else {
                List<T> filtered = new ArrayList<>(raw.size());
                for (T child : raw) {
                    if (child == null) {
                        continue;
                    }
                    // 过滤掉 parentId == id 且 child.id == id 的“自作为子”的场景
                    K childId = idGetter.apply(child);
                    if (!Objects.equals(childId, id)) {
                        filtered.add(child);
                    }
                }
                childrenSetter.accept(item, filtered.isEmpty() ? new ArrayList<>() : filtered);
            }
        }

        // 4) 选出根：parentId ∈ rootParentIds 或 parentId 不在 idMap 中（孤儿挂根）
        List<T> roots = new ArrayList<>();
        for (T item : items) {
            if (item == null) {
                continue;
            }
            K p = parentGetter.apply(item);
            if (rootParentIds.contains(p) || !idMap.containsKey(p)) {
                roots.add(item);
            }
        }
        return roots;
    }

    /**
     * 将平铺结构的列表转换为树形结构（单根父 ID，递归构建，支持循环引用检测，并可对同级节点排序）
     *
     * <p>此方法是 {@link #buildMultiRootTree(List, Function, Function, BiConsumer, Function, Set, Comparator)}
     * 的便捷版本，适用于只有一个根父 ID 的情况。</p>
     *
     * <p>特点：</p>
     * <ul>
     *     <li>支持单个根 parentId</li>
     *     <li>孤儿节点挂在根层</li>
     *     <li>children 永不为 null</li>
     *     <li>避免自循环造成死循环</li>
     *     <li>可传入 Comparator 对同级节点排序，支持多字段排序</li>
     * </ul>
     *
     * @param items          原始列表（平铺结构）
     * @param idGetter       获取节点 ID 的函数，例如：Menu::getId
     * @param parentGetter   获取父节点 ID 的函数，例如：Menu::getParentId
     * @param childrenSetter 设置子节点列表的函数，例如：Menu::setChildren
     * @param childrenGetter 获取子节点列表的函数，例如：Menu::getChildren
     * @param rootParentId   根节点父 ID（例如 0 或 null）
     * @param comparator     同级节点排序规则，支持多字段排序，可为 null 表示不排序
     * @param <T>            元素类型
     * @param <K>            ID 类型
     * @return 树形结构根节点列表，如果没有数据返回空列表
     *
     * <p><b>示例实体类：</b></p>
     * <pre>{@code
     * public class Menu {
     *     private Integer id;
     *     private Integer parentId;
     *     private String name;
     *     private List<Menu> children;
     *
     *     // 构造方法、getter、setter 略
     * }
     *
     * List<Menu> menus = Arrays.asList(
     *     new Menu(1, 0, "系统管理"),
     *     new Menu(2, 1, "用户管理"),
     *     new Menu(3, 1, "角色管理"),
     *     new Menu(4, 2, "用户列表"),
     *     new Menu(5, 0, "首页")
     * );
     *
     * Comparator<Menu> comparator = Comparator.comparing(Menu::getId).reversed();
     *
     * List<Menu> tree = CollectionUtil.buildTree(
     *     menus,
     *     Menu::getId,
     *     Menu::getParentId,
     *     Menu::setChildren,
     *     Menu::getChildren,
     *     0,
     *     comparator
     * );
     * }</pre>
     */
    public static <T, K> List<T> buildTree(
            List<T> items,
            Function<T, K> idGetter,
            Function<T, K> parentGetter,
            BiConsumer<T, List<T>> childrenSetter,
            Function<T, List<T>> childrenGetter,
            K rootParentId,
            Comparator<T> comparator) {

        if (items == null || items.isEmpty() || idGetter == null || parentGetter == null
                || childrenSetter == null || childrenGetter == null) {
            return Collections.emptyList();
        }

        // 调用多根版本，包装单根为集合
        return buildMultiRootTree(items, idGetter, parentGetter, childrenSetter, childrenGetter,
                Collections.singleton(rootParentId), comparator);
    }

    /**
     * 将平铺结构的列表转换为树形结构（支持多个根父 ID，递归构建，支持循环引用检测，并可对同级节点排序）
     *
     * <p>此方法可将任意平铺列表根据父子关系构建成树形结构，同时支持：</p>
     * <ul>
     *     <li>多个根父 ID</li>
     *     <li>孤儿节点挂在根层</li>
     *     <li>children 永不为 null</li>
     *     <li>避免自循环造成死循环</li>
     *     <li>可传入 Comparator 对同级节点排序，支持多字段排序</li>
     * </ul>
     *
     * @param items          原始列表（平铺结构）
     * @param idGetter       获取节点 ID 的函数，例如：Menu::getId
     * @param parentGetter   获取父节点 ID 的函数，例如：Menu::getParentId
     * @param childrenSetter 设置子节点列表的函数，例如：Menu::setChildren
     * @param childrenGetter 获取子节点列表的函数，例如：Menu::getChildren
     * @param rootParentIds  根节点父 ID 集合（任意一个匹配即为根节点，例如 0、-1、null）
     * @param comparator     同级节点排序规则，支持多字段排序，可为 null 表示不排序
     * @param <T>            元素类型
     * @param <K>            ID 类型
     * @return 树形结构根节点列表，如果没有数据返回空列表
     *
     * <p><b>示例实体类：</b></p>
     * <pre>{@code
     * public class Menu {
     *     private Integer id;             // 当前节点 ID
     *     private Integer parentId;       // 父节点 ID
     *     private String name;            // 节点名称（可选）
     *     private List<Menu> children;    // 子节点列表
     *
     *     // 构造方法、getter、setter 略
     * }
     *
     * List<Menu> menus = Arrays.asList(
     *     new Menu(1, 0, "系统管理"),
     *     new Menu(2, 1, "用户管理"),
     *     new Menu(3, 1, "角色管理"),
     *     new Menu(4, 2, "用户列表"),
     *     new Menu(5, 0, "首页"),
     *     new Menu(6, 3, "权限设置")
     * );
     *
     * // 支持多个根父 ID，并对同级节点按 ID 倒序排序
     * Comparator<Menu> comparator = Comparator.comparing(Menu::getId, Comparator.reverseOrder())
     *                                        .thenComparing(Menu::getName, Comparator.reverseOrder());
     *
     * List<Menu> tree = CollectionUtil.buildMultiRootTree(
     *     menus,
     *     Menu::getId,
     *     Menu::getParentId,
     *     Menu::setChildren,
     *     Menu::getChildren,
     *     new HashSet<>(Arrays.asList(0, null)),
     *     comparator
     * );
     * }</pre>
     */
    public static <T, K> List<T> buildMultiRootTree(
            List<T> items,
            Function<T, K> idGetter,
            Function<T, K> parentGetter,
            BiConsumer<T, List<T>> childrenSetter,
            Function<T, List<T>> childrenGetter,
            Set<K> rootParentIds,
            Comparator<T> comparator) {

        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }

        if (idGetter == null || parentGetter == null || childrenSetter == null || childrenGetter == null) {
            return Collections.emptyList();
        }

        if (rootParentIds == null || rootParentIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 1. 建立 ID -> 节点 映射，使用 LinkedHashMap 保序，遇到重复 ID 保留第一个
        Map<K, T> idMap = new LinkedHashMap<>();
        for (T item : items) {
            if (item != null) {
                K id = idGetter.apply(item);
                idMap.putIfAbsent(id, item);
            }
        }

        // 2. 建立 parentId -> 子节点 列表映射
        Map<K, List<T>> bucket = new HashMap<>();
        for (T item : items) {
            if (item != null) {
                K parentId = parentGetter.apply(item);
                bucket.computeIfAbsent(parentId, k -> new ArrayList<>()).add(item);
            }
        }

        // 3. 为每个节点设置 children，过滤自循环，children 永不为 null
        for (T item : items) {
            if (item != null) {
                K id = idGetter.apply(item);
                List<T> rawChildren = bucket.get(id);
                if (rawChildren == null || rawChildren.isEmpty()) {
                    childrenSetter.accept(item, new ArrayList<>());
                } else {
                    List<T> filteredChildren = new ArrayList<>();
                    for (T child : rawChildren) {
                        if (child != null) {
                            K childId = idGetter.apply(child);
                            if (!Objects.equals(childId, id)) {
                                filteredChildren.add(child);
                            }
                        }
                    }
                    childrenSetter.accept(item, filteredChildren.isEmpty() ? new ArrayList<>() : filteredChildren);
                }
            }
        }

        // 4. 选出根节点，根节点条件：parentId ∈ rootParentIds 或 parentId 不在 idMap 中
        List<T> roots = new ArrayList<>();
        for (T item : items) {
            if (item != null) {
                K parentId = parentGetter.apply(item);
                if (rootParentIds.contains(parentId) || !idMap.containsKey(parentId)) {
                    roots.add(item);
                }
            }
        }

        // 5. 对根节点及所有子节点递归排序
        if (comparator != null && !roots.isEmpty()) {
            sortRecursively(roots, childrenSetter, childrenGetter, comparator);
        }

        return roots;
    }

    /**
     * 递归对子节点排序
     *
     * @param nodes          当前节点列表
     * @param childrenSetter 设置子节点列表的方法
     * @param childrenGetter 获取子节点列表的方法
     * @param comparator     排序规则
     * @param <T>            元素类型
     */
    private static <T> void sortRecursively(
            List<T> nodes,
            BiConsumer<T, List<T>> childrenSetter,
            Function<T, List<T>> childrenGetter,
            Comparator<T> comparator) {

        if (nodes == null || nodes.isEmpty()) {
            return;
        }

        nodes.sort(comparator);

        for (T node : nodes) {
            List<T> children = childrenGetter.apply(node);
            if (children != null && !children.isEmpty()) {
                sortRecursively(children, childrenSetter, childrenGetter, comparator);
            }
        }
    }

    /**
     * 将树结构展开为扁平列表，并还原每个节点的子节点为空。
     *
     * <p>本方法以递归方式遍历树的所有节点，收集成一个列表，
     * 并且清空每个节点的子节点集合，完全还原成构建树之前的节点状态。
     * 返回结果会根据指定的比较器进行排序，通常用于按照节点id排序。</p>
     *
     * <p>注意：调用时需传入获取子节点的方法引用和设置子节点的方法引用，
     * 以便遍历和还原操作。排序规则由调用者通过 Comparator 指定。</p>
     *
     * @param rootList       树的根节点集合（非空）
     * @param childrenGetter 获取子节点列表的方法引用，形如 T -> Collection<T>
     * @param childrenSetter 设置子节点列表的方法引用，形如 (T, List<T>) -> void
     * @param idComparator   用于节点排序的比较器，通常根据节点id排序，为null就不排序
     * @param <T>            节点类型
     * @return 扁平化后且子节点被清空的节点列表，按照传入比较器排序
     *
     * <pre>{@code
     * // 示例实体类
     * public class Menu {
     *     private Integer id;             // 当前节点 ID
     *     private Integer parentId;       // 父节点 ID
     *     private String name;            // 节点名称（可选）
     *     private List<Menu> children;    // 子节点列表
     *
     *     // 构造方法、getter、setter 略
     * }
     *
     * // 使用示例：
     * List<Menu> flatList = treeToList(
     *     rootMenus,
     *     Menu::getChildren,
     *     Menu::setChildren,
     *     Comparator.comparing(Menu::getId)
     * );
     * }</pre>
     */
    public static <T> List<T> treeToList(Collection<T> rootList,
                                         Function<? super T, Collection<T>> childrenGetter,
                                         BiConsumer<? super T, List<T>> childrenSetter,
                                         Comparator<? super T> idComparator) {
        if (rootList == null || rootList.isEmpty() || childrenGetter == null || childrenSetter == null) {
            return Collections.emptyList();
        }

        List<T> result = new ArrayList<>();

        // 递归遍历树，扁平化所有节点
        traverse(rootList, childrenGetter, childrenSetter, result);

        // 按指定比较器排序（一般按id排序）
        if (idComparator != null) {
            result.sort(idComparator);
        }

        return result;
    }

    /**
     * 递归遍历树结构，将所有节点添加到结果列表中，
     * 并将每个节点的子节点集合清空以还原原始状态。
     *
     * @param nodes          当前层节点集合
     * @param childrenGetter 获取子节点集合的方法引用
     * @param childrenSetter 设置子节点集合的方法引用（用于清空子节点）
     * @param result         用于收集所有节点的列表
     * @param <T>            节点类型
     */
    private static <T> void traverse(Collection<T> nodes,
                                     Function<? super T, Collection<T>> childrenGetter,
                                     BiConsumer<? super T, List<T>> childrenSetter,
                                     List<T> result) {
        for (T node : nodes) {
            // 添加当前节点
            result.add(node);

            // 递归遍历子节点
            Collection<T> children = childrenGetter.apply(node);
            if (children != null && !children.isEmpty()) {
                traverse(children, childrenGetter, childrenSetter, result);
            }

            // 清空当前节点的子节点列表，恢复构建树前的状态
            childrenSetter.accept(node, Collections.emptyList());
        }
    }

    /**
     * 获取列表中的 Top N 元素（需元素可比较）
     *
     * @param list 原始列表
     * @param n    Top 个数
     * @param <T>  元素类型（必须实现 Comparable）
     * @return Top N 元素列表，若不足 N 个则返回全部，已排序
     */
    public static <T extends Comparable<T>> List<T> topN(List<T> list, int n) {
        if (isEmpty(list) || n <= 0) {
            return Collections.emptyList();
        }

        return list.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.reverseOrder())
                .limit(n)
                .collect(Collectors.toList());
    }

    /**
     * 按指定字段进行排名（字段必须可比较）
     *
     * @param list      原始对象列表
     * @param keyMapper 获取排名字段值的方法
     * @param ascending 是否升序（true 为小值排前）
     * @param <T>       元素类型
     * @param <U>       可比较字段类型
     * @return 排好序的列表
     */
    public static <T, U extends Comparable<U>> List<T> rank(List<T> list,
                                                            Function<T, U> keyMapper,
                                                            boolean ascending) {
        if (isEmpty(list) || keyMapper == null) {
            return Collections.emptyList();
        }

        Comparator<T> comparator = Comparator.comparing(keyMapper);
        if (!ascending) {
            comparator = comparator.reversed();
        }

        return list.stream()
                .filter(Objects::nonNull)
                .sorted(comparator)
                .collect(Collectors.toList());
    }

    /**
     * 按多个字段顺序排序
     *
     * @param list        要排序的集合
     * @param comparators 多个比较器，按顺序优先级使用
     * @param <T>         元素类型
     * @return 排序后的列表，若输入为空返回空列表
     */
    @SafeVarargs
    public static <T> List<T> multiSort(List<T> list, Comparator<T>... comparators) {
        if (isEmpty(list) || comparators == null || comparators.length == 0) {
            return list;
        }
        Comparator<T> combined = Arrays.stream(comparators)
                .reduce(Comparator::thenComparing)
                .orElseThrow(IllegalArgumentException::new);

        return list.stream()
                .sorted(combined)
                .collect(Collectors.toList());
    }

    /**
     * 将集合按指定大小分批
     *
     * @param list      原始集合
     * @param batchSize 每批大小，必须 > 0
     * @param <T>       元素类型
     * @return 分批后的列表集合，若输入为空或 batchSize 无效则返回空列表
     */
    public static <T> List<List<T>> partition(List<T> list, int batchSize) {
        if (isEmpty(list) || batchSize <= 0) {
            return Collections.emptyList();
        }
        List<List<T>> result = new ArrayList<>();
        int total = list.size();
        for (int i = 0; i < total; i += batchSize) {
            result.add(new ArrayList<>(list.subList(i, Math.min(total, i + batchSize))));
        }
        return result;
    }

    /**
     * 对集合进行过滤并转换为新的类型
     *
     * @param list   原始集合
     * @param filter 过滤条件
     * @param mapper 映射函数
     * @param <T>    原始类型
     * @param <R>    映射后的类型
     * @return 映射结果集合，若输入为空返回空列表
     */
    public static <T, R> List<R> filterMap(Collection<T> list,
                                           Predicate<? super T> filter,
                                           Function<? super T, ? extends R> mapper) {
        if (isEmpty(list) || filter == null || mapper == null) {
            return Collections.emptyList();
        }
        return list.stream()
                .filter(filter)
                .map(mapper)
                .collect(Collectors.toList());
    }

    /**
     * 移除 Map 中 key 为 null 的条目
     *
     * @param map 输入 map
     * @param <K> key 类型
     * @param <V> value 类型
     * @return 新的 map，已移除 null key
     */
    public static <K, V> Map<K, V> removeNullKeys(Map<K, V> map) {
        if (map == null || map.isEmpty()) {
            return Collections.emptyMap();
        }
        return map.entrySet().stream()
                .filter(e -> e.getKey() != null)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * 判断子集是否完全包含于父集
     *
     * @param parentSet 父集合（全集）
     * @param subset    子集合
     * @param <T>       元素类型
     * @return 如果 subset 的所有元素都在 parentSet 中，则返回 true，否则返回 false
     */
    public static <T> boolean isSubset(Collection<T> parentSet, Collection<T> subset) {
        if (isEmpty(subset)) {
            // 空集是任何集合的子集
            return true;
        }
        if (isEmpty(parentSet)) {
            return false;
        }
        return parentSet.containsAll(subset);
    }

    /**
     * 判断两个集合是否无序相等（即元素相同，顺序可不同）
     *
     * @param a   第一个集合
     * @param b   第二个集合
     * @param <T> 元素类型（需正确实现 equals 和 hashCode）
     * @return 若两个集合元素一致，返回 true；否则返回 false
     */
    public static <T> boolean compareListEqualIgnoreOrder(Collection<T> a, Collection<T> b) {
        if (a == null && b == null) {
            return true;
        }
        if (a == null || b == null || a.size() != b.size()) {
            return false;
        }

        Map<T, Long> freqA = a.stream().collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        Map<T, Long> freqB = b.stream().collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        return freqA.equals(freqB);
    }

    /**
     * 查找集合中重复出现的元素（只返回重复的元素，去重后）
     *
     * @param collection 输入集合
     * @param <T>        元素类型
     * @return 重复元素的集合，若无重复或输入为空返回空集合
     */
    public static <T> Set<T> findDuplicates(Collection<T> collection) {
        if (collection == null || collection.isEmpty()) {
            return Collections.emptySet();
        }
        Set<T> seen = new HashSet<>();
        return collection.stream()
                .filter(e -> !seen.add(e))
                .collect(Collectors.toSet());
    }

    /**
     * 循环移动列表元素，正数向右移，负数向左移
     *
     * @param list   要旋转的列表（非空）
     * @param offset 旋转偏移量（可正可负）
     * @param <T>    元素类型
     * @return 旋转后的新列表，输入为空返回空列表
     */
    public static <T> List<T> rotate(List<T> list, int offset) {
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        int size = list.size();
        // 保证 offset 在 [0, size) 范围
        int normalizedOffset = ((offset % size) + size) % size;
        if (normalizedOffset == 0) {
            return new ArrayList<>(list);
        }

        List<T> rotated = new ArrayList<>(size);
        rotated.addAll(list.subList(size - normalizedOffset, size));
        rotated.addAll(list.subList(0, size - normalizedOffset));
        return rotated;
    }

    /**
     * 安全获取列表指定索引元素，索引越界或列表为空返回默认值
     *
     * @param list         目标列表
     * @param index        索引位置
     * @param defaultValue 索引越界时返回的默认值
     * @param <T>          元素类型
     * @return 索引对应元素或默认值
     */
    public static <T> T safeGet(List<T> list, int index, T defaultValue) {
        if (list == null || index < 0 || index >= list.size()) {
            return defaultValue;
        }
        return list.get(index);
    }


    /**
     * 将 List 转为 Map，key 唯一，value 为元素本身
     *
     * @param list      输入列表
     * @param keyMapper 取键函数
     * @param <K>       键类型
     * @param <V>       元素类型
     * @return 转换后的 Map，key 唯一，key 冲突时保留后一个元素
     */
    public static <K, V> Map<K, V> mapListToMapByKey(List<V> list, Function<? super V, ? extends K> keyMapper) {
        if (list == null || keyMapper == null) {
            return Collections.emptyMap();
        }
        return list.stream()
                .collect(Collectors.toMap(
                        keyMapper,
                        Function.identity(),
                        (existing, replacement) -> replacement
                ));
    }

    /**
     * 将 List 转为 Map<K, List<V>>，根据键分组
     *
     * @param list      输入列表
     * @param keyMapper 取键函数
     * @param <K>       键类型
     * @param <V>       元素类型
     * @return 分组后的 Map
     */
    public static <K, V> Map<K, List<V>> mapListToMultiMap(List<V> list, Function<? super V, ? extends K> keyMapper) {
        if (list == null || keyMapper == null) {
            return Collections.emptyMap();
        }
        return list.stream()
                .collect(Collectors.groupingBy(keyMapper));
    }

    /**
     * List 分组并计数
     *
     * @param list      输入列表
     * @param keyMapper 取键函数
     * @param <K>       键类型
     * @param <V>       元素类型
     * @return 分组计数 Map
     */
    public static <K, V> Map<K, Long> groupAndCount(List<V> list, Function<? super V, ? extends K> keyMapper) {
        if (list == null || keyMapper == null) {
            return Collections.emptyMap();
        }
        return list.stream()
                .collect(Collectors.groupingBy(keyMapper, Collectors.counting()));
    }

    /**
     * 将 Map 的 value（集合）展开成一个扁平的 List
     *
     * @param map Map<K, Collection<V>>
     * @param <K> 键类型
     * @param <V> 值元素类型
     * @return 扁平 List
     */
    public static <K, V> List<V> mapValuesToList(Map<K, ? extends Collection<V>> map) {
        if (map == null) {
            return Collections.emptyList();
        }
        return map.values().stream()
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    /**
     * 根据 Map 中的 value 条件过滤出符合的键值对
     *
     * @param map       输入 Map
     * @param predicate 值判断条件
     * @param <K>       键类型
     * @param <V>       值类型
     * @return 过滤后的 Map
     */
    public static <K, V> Map<K, V> filterMapByValue(Map<K, V> map, Predicate<? super V> predicate) {
        if (map == null || predicate == null) {
            return Collections.emptyMap();
        }
        return map.entrySet().stream()
                .filter(e -> predicate.test(e.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * 将 Map<K, List<V>> 合并为 List<V>
     *
     * @param map Map<K, List<V>>
     * @param <K> 键类型
     * @param <V> 值类型
     * @return 合并后的 List，元素顺序按 Map.values() 顺序
     */
    public static <K, V> List<V> mapKeyListToList(Map<K, List<V>> map) {
        if (map == null) {
            return Collections.emptyList();
        }
        return map.values().stream()
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    /**
     * 将 List<T> 转换为 List<R>
     *
     * @param list   输入列表
     * @param mapper 转换函数
     * @param <T>    输入元素类型
     * @param <R>    输出元素类型
     * @return 转换后的列表
     */
    public static <T, R> List<R> mapListToList(List<T> list, Function<? super T, ? extends R> mapper) {
        if (list == null || mapper == null) {
            return Collections.emptyList();
        }
        return list.stream()
                .map(mapper)
                .collect(Collectors.toList());
    }

    /**
     * 合并多个 Map，遇到相同 key 可自定义合并规则
     *
     * @param maps          多个 Map
     * @param mergeFunction 冲突 key 的合并函数
     * @param <K>           键类型
     * @param <V>           值类型
     * @return 合并后的 Map
     */
    @SafeVarargs
    public static <K, V> Map<K, V> mergeMaps(BinaryOperator<V> mergeFunction, Map<K, V>... maps) {
        if (mergeFunction == null || maps == null || maps.length == 0) {
            return Collections.emptyMap();
        }
        return Arrays.stream(maps)
                .filter(Objects::nonNull)
                .flatMap(m -> m.entrySet().stream())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        mergeFunction
                ));
    }

    /**
     * 反转 Map，key 和 value 互换，value 应唯一，否则后面的会覆盖前面的
     *
     * @param map 输入 Map
     * @param <K> 键类型
     * @param <V> 值类型
     * @return 反转后的 Map
     */
    public static <K, V> Map<V, K> invertMap(Map<K, V> map) {
        if (map == null) {
            return Collections.emptyMap();
        }
        return map.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getValue,
                        Map.Entry::getKey,
                        (existing, replacement) -> replacement
                ));
    }

    /**
     * 安全从 Map<K, List<V>> 获取指定 key 的列表，避免空指针，找不到返回空列表
     *
     * @param map Map<K, List<V>>
     * @param key 查询 key
     * @param <K> 键类型
     * @param <V> 值元素类型
     * @return 不为 null 的列表
     */
    public static <K, V> List<V> safeGetFromMapList(Map<K, List<V>> map, K key) {
        if (map == null || key == null) {
            return Collections.emptyList();
        }
        List<V> list = map.get(key);
        return list == null ? Collections.emptyList() : list;
    }

    /**
     * 对 Map 的所有 key 进行映射转换，生成新的 Map，value 保持不变
     *
     * @param map       原始 Map
     * @param keyMapper key 映射函数
     * @param <K>       原 key 类型
     * @param <V>       value 类型
     * @param <R>       新 key 类型
     * @return 转换后的新 Map，如果输入 map 或 keyMapper 为 null 返回空 Map
     */
    public static <K, V, R> Map<R, V> mapKeys(Map<K, V> map, Function<? super K, ? extends R> keyMapper) {
        if (map == null || keyMapper == null) {
            return Collections.emptyMap();
        }
        return map.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> keyMapper.apply(e.getKey()),
                        Map.Entry::getValue,
                        // 如果 key 冲突，保留后者
                        (v1, v2) -> v2
                ));
    }

    /**
     * 对 Map 的所有 value 进行映射转换，生成新的 Map，key 保持不变
     *
     * @param map         原始 Map
     * @param valueMapper value 映射函数
     * @param <K>         key 类型
     * @param <V>         原 value 类型
     * @param <R>         新 value 类型
     * @return 转换后的新 Map，如果输入 map 或 valueMapper 为 null 返回空 Map
     */
    public static <K, V, R> Map<K, R> mapValues(Map<K, V> map, Function<? super V, ? extends R> valueMapper) {
        if (map == null || valueMapper == null) {
            return Collections.emptyMap();
        }
        return map.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> valueMapper.apply(e.getValue()),
                        // 如果 key 冲突，保留后者（一般不会冲突）
                        (r1, r2) -> r2
                ));
    }

    /**
     * 按批次处理大集合
     *
     * @param list      需要处理的集合
     * @param batchSize 每批大小，必须大于0
     * @param processor 批处理函数，接收每批子集合进行处理
     * @param <T>       集合元素类型
     * @throws IllegalArgumentException 如果 batchSize <= 0 或 processor 为 null
     */
    public static <T> void batchProcess(List<T> list, int batchSize, Consumer<List<T>> processor) {
        if (batchSize <= 0) {
            throw new IllegalArgumentException("batchSize 必须大于0");
        }
        if (processor == null) {
            throw new IllegalArgumentException("processor 不可为空");
        }
        if (list == null || list.isEmpty()) {
            return;
        }

        int total = list.size();
        for (int start = 0; start < total; start += batchSize) {
            int end = Math.min(start + batchSize, total);
            List<T> batch = list.subList(start, end);
            processor.accept(batch);
        }
    }

    /**
     * 异步批处理集合
     *
     * @param list      待处理列表
     * @param batchSize 每批大小，必须大于0
     * @param processor 批处理函数，接受一批元素，返回结果列表
     * @param <T>       输入元素类型
     * @param <R>       处理结果类型
     * @return 所有批次结果合并后的列表
     * @throws IllegalArgumentException 如果参数非法
     */
    public static <T, R> List<R> batchProcessAsync(
            List<T> list,
            int batchSize,
            Function<List<T>, List<R>> processor
    ) {
        if (batchSize <= 0) {
            throw new IllegalArgumentException("batchSize 必须大于0");
        }
        if (processor == null) {
            throw new IllegalArgumentException("processor 不可为空");
        }
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }

        ExecutorService executor = Executors.newFixedThreadPool(
                Math.min((list.size() + batchSize - 1) / batchSize, Runtime.getRuntime().availableProcessors())
        );

        try {
            List<CompletableFuture<List<R>>> futures = new ArrayList<>();
            int total = list.size();
            for (int start = 0; start < total; start += batchSize) {
                int end = Math.min(start + batchSize, total);
                List<T> batch = list.subList(start, end);

                CompletableFuture<List<R>> future = CompletableFuture.supplyAsync(() -> processor.apply(batch), executor);
                futures.add(future);
            }

            CompletableFuture<Void> allDone = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
            CompletableFuture<List<R>> allResults = allDone.thenApply(v ->
                    futures.stream()
                            .flatMap(f -> f.join().stream())
                            .collect(Collectors.toList())
            );

            return allResults.join();

        } finally {
            executor.shutdown();
        }
    }

    /**
     * 异步批处理集合，支持异常处理和超时
     *
     * @param list      待处理列表
     * @param batchSize 每批大小，必须大于0
     * @param processor 批处理函数，接收批次列表，返回结果列表
     * @param executor  执行异步任务的线程池
     * @param timeout   超时时间，单位毫秒，超时会取消任务
     * @param <T>       输入元素类型
     * @param <R>       处理结果类型
     * @return 所有批次合并的结果列表
     * @throws IllegalArgumentException 参数非法抛出
     * @throws TimeoutException         超时抛出
     * @throws ExecutionException       执行异常抛出
     * @throws InterruptedException     线程中断异常
     */
    public static <T, R> List<R> batchProcessAsync(
            List<T> list,
            int batchSize,
            Function<List<T>, List<R>> processor,
            ExecutorService executor,
            long timeout
    ) throws InterruptedException, ExecutionException, TimeoutException {
        if (batchSize <= 0) {
            throw new IllegalArgumentException("batchSize 必须大于0");
        }
        if (processor == null) {
            throw new IllegalArgumentException("processor 不可为空");
        }
        if (executor == null) {
            throw new IllegalArgumentException("executor 不可为空");
        }
        if (timeout <= 0) {
            throw new IllegalArgumentException("timeout 必须大于0");
        }
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }

        int total = list.size();
        List<CompletableFuture<List<R>>> futures = new ArrayList<>();

        for (int start = 0; start < total; start += batchSize) {
            int end = Math.min(start + batchSize, total);
            List<T> batch = list.subList(start, end);
            CompletableFuture<List<R>> future = CompletableFuture.supplyAsync(() -> {
                try {
                    return processor.apply(batch);
                } catch (Exception e) {
                    throw new CompletionException(e);
                }
            }, executor);
            futures.add(future);
        }

        CompletableFuture<Void> allDoneFuture = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));

        // 等待所有任务完成，带超时控制
        allDoneFuture.get(timeout, TimeUnit.MILLISECONDS);

        List<R> result = new ArrayList<>();
        for (CompletableFuture<List<R>> future : futures) {
            try {
                result.addAll(future.getNow(Collections.emptyList()));
            } catch (CompletionException ce) {
                // 这里你可以选择如何处理单个批次异常，比如忽略或重新抛出
                throw new ExecutionException("批处理异常", ce.getCause());
            }
        }

        return result;
    }

    /**
     * 将联表查询后的扁平数据聚合为父对象并填充子对象列表。
     *
     * @param flatList          联表后的扁平列表
     * @param parentIdGetter    获取父主键
     * @param parentConstructor 父对象构建器（从 flat 对象构建新父对象）
     * @param childIdGetter     获取子主键（用于去重）
     * @param childConstructor  子对象构建器（从 flat 对象构建新子对象）
     * @param childrenGetter    获取父对象下子列表
     * @param childrenSetter    设置父对象的子列表
     * @param <F>               扁平记录类型（如联表后的 DTO）
     * @param <P>               父类型（如 ConfTypeVO）
     * @param <C>               子类型（如 ConfScoreBaseDTO）
     * @param <K>               主键类型
     * @return 聚合后的父对象列表
     */
    public static <F, P, C, K> List<P> groupAsParentChildren(
            List<F> flatList,
            Function<F, K> parentIdGetter,
            Function<F, P> parentConstructor,
            Function<F, K> childIdGetter,
            Function<F, C> childConstructor,
            Function<P, List<C>> childrenGetter,
            BiConsumer<P, List<C>> childrenSetter
    ) {
        if (flatList == null || flatList.isEmpty()) {
            return Collections.emptyList();
        }

        Map<K, P> parentMap = new LinkedHashMap<>();
        Map<K, Set<K>> childExistMap = new HashMap<>();

        for (F f : flatList) {

            K parentId = parentIdGetter.apply(f);
            P parent = parentMap.get(parentId);

            if (parent == null) {
                parent = parentConstructor.apply(f);
                parentMap.put(parentId, parent);
                childrenSetter.accept(parent, new ArrayList<>());
                childExistMap.put(parentId, new HashSet<>());
            }

            K childId = childIdGetter.apply(f);
            if (childId != null && childExistMap.get(parentId).add(childId)) {
                C child = childConstructor.apply(f);
                childrenGetter.apply(parent).add(child);
            }
        }

        return new ArrayList<>(parentMap.values());
    }

}
