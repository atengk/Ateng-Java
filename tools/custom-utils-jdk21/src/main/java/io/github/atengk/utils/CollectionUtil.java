package io.github.atengk.utils;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * 集合工具类。
 *
 * @author Ateng
 * @since 2026-04-29
 */
public final class CollectionUtil {

    /**
     * 工具类禁止实例化。
     */
    private CollectionUtil() {
        throw new UnsupportedOperationException("CollectionUtil 不允许实例化");
    }

    /**
     * 判断集合是否为空。
     *
     * @param collection 集合
     * @return true 表示集合为 null 或无元素
     */
    public static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    /**
     * 判断集合是否非空。
     *
     * @param collection 集合
     * @return true 表示集合不为 null 且至少包含一个元素
     */
    public static boolean isNotEmpty(Collection<?> collection) {
        return !isEmpty(collection);
    }

    /**
     * 获取集合大小。
     *
     * @param collection 集合
     * @return 集合大小，null 返回 0
     */
    public static int size(Collection<?> collection) {
        return collection == null ? 0 : collection.size();
    }

    /**
     * 判断集合大小是否等于指定值。
     *
     * @param collection   集合
     * @param expectedSize 期望大小
     * @return true 表示集合大小等于指定值
     */
    public static boolean hasSize(Collection<?> collection, int expectedSize) {
        checkExpectedSize(expectedSize);
        return size(collection) == expectedSize;
    }

    /**
     * 判断集合大小是否大于指定值。
     *
     * @param collection 集合
     * @param minSize    最小大小，不包含该值
     * @return true 表示集合大小大于指定值
     */
    public static boolean sizeGreaterThan(Collection<?> collection, int minSize) {
        checkExpectedSize(minSize);
        return size(collection) > minSize;
    }

    /**
     * 判断集合大小是否大于等于指定值。
     *
     * @param collection 集合
     * @param minSize    最小大小，包含该值
     * @return true 表示集合大小大于等于指定值
     */
    public static boolean sizeGreaterThanOrEqual(Collection<?> collection, int minSize) {
        checkExpectedSize(minSize);
        return size(collection) >= minSize;
    }

    /**
     * 判断集合大小是否小于指定值。
     *
     * @param collection 集合
     * @param maxSize    最大大小，不包含该值
     * @return true 表示集合大小小于指定值
     */
    public static boolean sizeLessThan(Collection<?> collection, int maxSize) {
        checkExpectedSize(maxSize);
        return size(collection) < maxSize;
    }

    /**
     * 判断集合大小是否小于等于指定值。
     *
     * @param collection 集合
     * @param maxSize    最大大小，包含该值
     * @return true 表示集合大小小于等于指定值
     */
    public static boolean sizeLessThanOrEqual(Collection<?> collection, int maxSize) {
        checkExpectedSize(maxSize);
        return size(collection) <= maxSize;
    }

    /**
     * 判断集合大小是否在指定范围内。
     *
     * @param collection 集合
     * @param minSize    最小大小，包含该值
     * @param maxSize    最大大小，包含该值
     * @return true 表示集合大小在指定范围内
     */
    public static boolean sizeBetween(Collection<?> collection, int minSize, int maxSize) {
        checkSizeRange(minSize, maxSize);
        int size = size(collection);
        return size >= minSize && size <= maxSize;
    }

    /**
     * 判断集合是否只有一个元素。
     *
     * @param collection 集合
     * @return true 表示集合不为 null 且只有一个元素
     */
    public static boolean isSingle(Collection<?> collection) {
        return size(collection) == 1;
    }

    /**
     * 判断集合是否包含多个元素。
     *
     * @param collection 集合
     * @return true 表示集合不为 null 且元素数量大于 1
     */
    public static boolean isMulti(Collection<?> collection) {
        return size(collection) > 1;
    }

    /**
     * 判断 Map 是否为空。
     *
     * @param map Map 对象
     * @return true 表示 Map 为 null 或无元素
     */
    public static boolean isEmpty(Map<?, ?> map) {
        return map == null || map.isEmpty();
    }

    /**
     * 判断 Map 是否非空。
     *
     * @param map Map 对象
     * @return true 表示 Map 不为 null 且至少包含一个元素
     */
    public static boolean isNotEmpty(Map<?, ?> map) {
        return !isEmpty(map);
    }

    /**
     * 获取 Map 大小。
     *
     * @param map Map 对象
     * @return Map 大小，null 返回 0
     */
    public static int size(Map<?, ?> map) {
        return map == null ? 0 : map.size();
    }

    /**
     * 判断 Map 大小是否等于指定值。
     *
     * @param map          Map 对象
     * @param expectedSize 期望大小
     * @return true 表示 Map 大小等于指定值
     */
    public static boolean hasSize(Map<?, ?> map, int expectedSize) {
        checkExpectedSize(expectedSize);
        return size(map) == expectedSize;
    }

    /**
     * 判断 Map 大小是否大于指定值。
     *
     * @param map     Map 对象
     * @param minSize 最小大小，不包含该值
     * @return true 表示 Map 大小大于指定值
     */
    public static boolean sizeGreaterThan(Map<?, ?> map, int minSize) {
        checkExpectedSize(minSize);
        return size(map) > minSize;
    }

    /**
     * 判断 Map 大小是否大于等于指定值。
     *
     * @param map     Map 对象
     * @param minSize 最小大小，包含该值
     * @return true 表示 Map 大小大于等于指定值
     */
    public static boolean sizeGreaterThanOrEqual(Map<?, ?> map, int minSize) {
        checkExpectedSize(minSize);
        return size(map) >= minSize;
    }

    /**
     * 判断 Map 大小是否小于指定值。
     *
     * @param map     Map 对象
     * @param maxSize 最大大小，不包含该值
     * @return true 表示 Map 大小小于指定值
     */
    public static boolean sizeLessThan(Map<?, ?> map, int maxSize) {
        checkExpectedSize(maxSize);
        return size(map) < maxSize;
    }

    /**
     * 判断 Map 大小是否小于等于指定值。
     *
     * @param map     Map 对象
     * @param maxSize 最大大小，包含该值
     * @return true 表示 Map 大小小于等于指定值
     */
    public static boolean sizeLessThanOrEqual(Map<?, ?> map, int maxSize) {
        checkExpectedSize(maxSize);
        return size(map) <= maxSize;
    }

    /**
     * 判断 Map 大小是否在指定范围内。
     *
     * @param map     Map 对象
     * @param minSize 最小大小，包含该值
     * @param maxSize 最大大小，包含该值
     * @return true 表示 Map 大小在指定范围内
     */
    public static boolean sizeBetween(Map<?, ?> map, int minSize, int maxSize) {
        checkSizeRange(minSize, maxSize);
        int size = size(map);
        return size >= minSize && size <= maxSize;
    }

    /**
     * 判断 Map 是否只有一个元素。
     *
     * @param map Map 对象
     * @return true 表示 Map 不为 null 且只有一个元素
     */
    public static boolean isSingle(Map<?, ?> map) {
        return size(map) == 1;
    }

    /**
     * 判断 Map 是否包含多个元素。
     *
     * @param map Map 对象
     * @return true 表示 Map 不为 null 且元素数量大于 1
     */
    public static boolean isMulti(Map<?, ?> map) {
        return size(map) > 1;
    }

    /**
     * 判断 Iterable 是否为空。
     *
     * <p>
     * 如果参数本身是 Collection，会直接使用 Collection 的 size 能力；
     * 否则通过 iterator().hasNext() 判断。
     * </p>
     *
     * @param iterable Iterable 对象
     * @return true 表示 Iterable 为 null 或无元素
     */
    public static boolean isEmpty(Iterable<?> iterable) {
        if (iterable == null) {
            return true;
        }
        if (iterable instanceof Collection<?> collection) {
            return collection.isEmpty();
        }
        return !iterable.iterator().hasNext();
    }

    /**
     * 判断 Iterable 是否非空。
     *
     * @param iterable Iterable 对象
     * @return true 表示 Iterable 不为 null 且至少包含一个元素
     */
    public static boolean isNotEmpty(Iterable<?> iterable) {
        return !isEmpty(iterable);
    }

    /**
     * 获取 Iterable 元素数量。
     *
     * <p>
     * 如果参数本身是 Collection，会直接使用 Collection 的 size 能力；
     * 否则会遍历 Iterable 统计数量。
     * </p>
     *
     * @param iterable Iterable 对象
     * @return 元素数量，null 返回 0
     */
    public static int size(Iterable<?> iterable) {
        if (iterable == null) {
            return 0;
        }
        if (iterable instanceof Collection<?> collection) {
            return collection.size();
        }

        int count = 0;
        for (Object ignored : iterable) {
            count++;
        }
        return count;
    }

    /**
     * 判断 Iterable 是否只有一个元素。
     *
     * @param iterable Iterable 对象
     * @return true 表示 Iterable 不为 null 且只有一个元素
     */
    public static boolean isSingle(Iterable<?> iterable) {
        return size(iterable) == 1;
    }

    /**
     * 判断 Iterable 是否包含多个元素。
     *
     * @param iterable Iterable 对象
     * @return true 表示 Iterable 不为 null 且元素数量大于 1
     */
    public static boolean isMulti(Iterable<?> iterable) {
        return size(iterable) > 1;
    }

    /**
     * 判断 Iterator 是否为空。
     *
     * <p>
     * 该方法只调用 hasNext()，不会消费迭代器中的元素。
     * </p>
     *
     * @param iterator Iterator 对象
     * @return true 表示 Iterator 为 null 或无下一个元素
     */
    public static boolean isEmpty(Iterator<?> iterator) {
        return iterator == null || !iterator.hasNext();
    }

    /**
     * 判断 Iterator 是否非空。
     *
     * @param iterator Iterator 对象
     * @return true 表示 Iterator 不为 null 且存在下一个元素
     */
    public static boolean isNotEmpty(Iterator<?> iterator) {
        return !isEmpty(iterator);
    }

    /**
     * 判断对象数组是否为空。
     *
     * @param array 对象数组
     * @return true 表示数组为 null 或长度为 0
     */
    public static boolean isEmpty(Object[] array) {
        return array == null || array.length == 0;
    }

    /**
     * 判断对象数组是否非空。
     *
     * @param array 对象数组
     * @return true 表示数组不为 null 且长度大于 0
     */
    public static boolean isNotEmpty(Object[] array) {
        return !isEmpty(array);
    }

    /**
     * 获取对象数组长度。
     *
     * @param array 对象数组
     * @return 数组长度，null 返回 0
     */
    public static int size(Object[] array) {
        return array == null ? 0 : array.length;
    }

    /**
     * 判断对象数组长度是否等于指定值。
     *
     * @param array        对象数组
     * @param expectedSize 期望长度
     * @return true 表示数组长度等于指定值
     */
    public static boolean hasSize(Object[] array, int expectedSize) {
        checkExpectedSize(expectedSize);
        return size(array) == expectedSize;
    }

    /**
     * 判断对象数组是否只有一个元素。
     *
     * @param array 对象数组
     * @return true 表示数组不为 null 且只有一个元素
     */
    public static boolean isSingle(Object[] array) {
        return size(array) == 1;
    }

    /**
     * 判断对象数组是否包含多个元素。
     *
     * @param array 对象数组
     * @return true 表示数组不为 null 且元素数量大于 1
     */
    public static boolean isMulti(Object[] array) {
        return size(array) > 1;
    }

    /**
     * 判断任意数组是否为空，支持对象数组和基本类型数组。
     *
     * <p>
     * 示例：String[]、int[]、long[]、boolean[] 均可判断。
     * 非数组对象会抛出 IllegalArgumentException。
     * </p>
     *
     * @param array 数组对象
     * @return true 表示数组为 null 或长度为 0
     */
    public static boolean isArrayEmpty(Object array) {
        return array == null || arrayLength(array) == 0;
    }

    /**
     * 判断任意数组是否非空，支持对象数组和基本类型数组。
     *
     * @param array 数组对象
     * @return true 表示数组不为 null 且长度大于 0
     */
    public static boolean isArrayNotEmpty(Object array) {
        return !isArrayEmpty(array);
    }

    /**
     * 获取任意数组长度，支持对象数组和基本类型数组。
     *
     * <p>
     * 示例：String[]、int[]、long[]、boolean[] 均可获取长度。
     * 非数组对象会抛出 IllegalArgumentException。
     * </p>
     *
     * @param array 数组对象
     * @return 数组长度，null 返回 0
     */
    public static int arrayLength(Object array) {
        if (array == null) {
            return 0;
        }
        if (!array.getClass().isArray()) {
            throw new IllegalArgumentException("参数必须是数组类型");
        }
        return Array.getLength(array);
    }

    /**
     * 判断任意数组长度是否等于指定值。
     *
     * @param array        数组对象
     * @param expectedSize 期望长度
     * @return true 表示数组长度等于指定值
     */
    public static boolean hasArraySize(Object array, int expectedSize) {
        checkExpectedSize(expectedSize);
        return arrayLength(array) == expectedSize;
    }

    /**
     * 判断任意数组是否只有一个元素。
     *
     * @param array 数组对象
     * @return true 表示数组不为 null 且只有一个元素
     */
    public static boolean isArraySingle(Object array) {
        return arrayLength(array) == 1;
    }

    /**
     * 判断任意数组是否包含多个元素。
     *
     * @param array 数组对象
     * @return true 表示数组不为 null 且元素数量大于 1
     */
    public static boolean isArrayMulti(Object array) {
        return arrayLength(array) > 1;
    }

    /**
     * 判断集合是否不为 null，且不存在 null 元素。
     *
     * @param collection 集合
     * @return true 表示集合不为 null 且所有元素都不为 null
     */
    public static boolean isAllNotNull(Collection<?> collection) {
        if (collection == null) {
            return false;
        }
        return collection.stream().allMatch(Objects::nonNull);
    }

    /**
     * 判断集合是否为 null，或至少存在一个 null 元素。
     *
     * @param collection 集合
     * @return true 表示集合为 null 或存在 null 元素
     */
    public static boolean hasNull(Collection<?> collection) {
        if (collection == null) {
            return true;
        }
        return collection.stream().anyMatch(Objects::isNull);
    }

    /**
     * 检查期望大小是否合法。
     *
     * @param expectedSize 期望大小
     */
    private static void checkExpectedSize(int expectedSize) {
        if (expectedSize < 0) {
            throw new IllegalArgumentException("集合大小不能小于 0");
        }
    }

    /**
     * 检查大小范围是否合法。
     *
     * @param minSize 最小大小
     * @param maxSize 最大大小
     */
    private static void checkSizeRange(int minSize, int maxSize) {
        checkExpectedSize(minSize);
        checkExpectedSize(maxSize);
        if (minSize > maxSize) {
            throw new IllegalArgumentException("最小大小不能大于最大大小");
        }
    }

    /**
     * null 安全返回 List。
     *
     * @param list List 对象
     * @param <T>  元素类型
     * @return 原 List，null 返回不可变空 List
     */
    public static <T> List<T> emptyListIfNull(List<T> list) {
        return list == null ? List.of() : list;
    }

    /**
     * null 安全返回 Set。
     *
     * @param set Set 对象
     * @param <T> 元素类型
     * @return 原 Set，null 返回不可变空 Set
     */
    public static <T> Set<T> emptySetIfNull(Set<T> set) {
        return set == null ? Set.of() : set;
    }

    /**
     * null 安全返回 Map。
     *
     * @param map Map 对象
     * @param <K> Key 类型
     * @param <V> Value 类型
     * @return 原 Map，null 返回不可变空 Map
     */
    public static <K, V> Map<K, V> emptyMapIfNull(Map<K, V> map) {
        return map == null ? Map.of() : map;
    }

    /**
     * null 安全返回 Collection。
     *
     * @param collection Collection 对象
     * @param <T>        元素类型
     * @return 原 Collection，null 返回不可变空 List
     */
    public static <T> Collection<T> emptyCollectionIfNull(Collection<T> collection) {
        return collection == null ? List.of() : collection;
    }

    /**
     * 创建空 ArrayList。
     *
     * @param <T> 元素类型
     * @return 空 ArrayList
     */
    public static <T> List<T> newArrayList() {
        return new ArrayList<>();
    }

    /**
     * 创建指定初始容量的 ArrayList。
     *
     * @param initialCapacity 初始容量
     * @param <T>             元素类型
     * @return 空 ArrayList
     */
    public static <T> List<T> newArrayList(int initialCapacity) {
        checkExpectedSize(initialCapacity);
        return new ArrayList<>(initialCapacity);
    }

    /**
     * 根据 Collection 创建 ArrayList。
     *
     * @param collection Collection 对象
     * @param <T>        元素类型
     * @return 新 ArrayList，参数为 null 时返回空 ArrayList
     */
    public static <T> List<T> newArrayList(Collection<? extends T> collection) {
        return collection == null ? new ArrayList<>() : new ArrayList<>(collection);
    }

    /**
     * 根据可变参数创建 ArrayList。
     *
     * @param elements 元素数组
     * @param <T>      元素类型
     * @return 新 ArrayList，参数为 null 时返回空 ArrayList
     */
    @SafeVarargs
    public static <T> List<T> newArrayList(T... elements) {
        if (elements == null || elements.length == 0) {
            return new ArrayList<>();
        }

        List<T> list = new ArrayList<>(elements.length);
        Collections.addAll(list, elements);
        return list;
    }

    /**
     * 创建空 LinkedList。
     *
     * @param <T> 元素类型
     * @return 空 LinkedList
     */
    public static <T> List<T> newLinkedList() {
        return new LinkedList<>();
    }

    /**
     * 根据 Collection 创建 LinkedList。
     *
     * @param collection Collection 对象
     * @param <T>        元素类型
     * @return 新 LinkedList，参数为 null 时返回空 LinkedList
     */
    public static <T> List<T> newLinkedList(Collection<? extends T> collection) {
        return collection == null ? new LinkedList<>() : new LinkedList<>(collection);
    }

    /**
     * 根据可变参数创建 LinkedList。
     *
     * @param elements 元素数组
     * @param <T>      元素类型
     * @return 新 LinkedList，参数为 null 时返回空 LinkedList
     */
    @SafeVarargs
    public static <T> List<T> newLinkedList(T... elements) {
        List<T> list = new LinkedList<>();
        if (elements != null) {
            Collections.addAll(list, elements);
        }
        return list;
    }

    /**
     * 创建空 HashSet。
     *
     * @param <T> 元素类型
     * @return 空 HashSet
     */
    public static <T> Set<T> newHashSet() {
        return new HashSet<>();
    }

    /**
     * 创建指定初始容量的 HashSet。
     *
     * @param initialCapacity 初始容量
     * @param <T>             元素类型
     * @return 空 HashSet
     */
    public static <T> Set<T> newHashSet(int initialCapacity) {
        checkExpectedSize(initialCapacity);
        return new HashSet<>(initialCapacity);
    }

    /**
     * 根据 Collection 创建 HashSet。
     *
     * @param collection Collection 对象
     * @param <T>        元素类型
     * @return 新 HashSet，参数为 null 时返回空 HashSet
     */
    public static <T> Set<T> newHashSet(Collection<? extends T> collection) {
        return collection == null ? new HashSet<>() : new HashSet<>(collection);
    }

    /**
     * 根据可变参数创建 HashSet。
     *
     * @param elements 元素数组
     * @param <T>      元素类型
     * @return 新 HashSet，参数为 null 时返回空 HashSet
     */
    @SafeVarargs
    public static <T> Set<T> newHashSet(T... elements) {
        if (elements == null || elements.length == 0) {
            return new HashSet<>();
        }

        Set<T> set = new HashSet<>(calculateHashMapCapacity(elements.length));
        Collections.addAll(set, elements);
        return set;
    }

    /**
     * 创建空 LinkedHashSet。
     *
     * @param <T> 元素类型
     * @return 空 LinkedHashSet
     */
    public static <T> Set<T> newLinkedHashSet() {
        return new LinkedHashSet<>();
    }

    /**
     * 创建指定初始容量的 LinkedHashSet。
     *
     * @param initialCapacity 初始容量
     * @param <T>             元素类型
     * @return 空 LinkedHashSet
     */
    public static <T> Set<T> newLinkedHashSet(int initialCapacity) {
        checkExpectedSize(initialCapacity);
        return new LinkedHashSet<>(initialCapacity);
    }

    /**
     * 根据 Collection 创建 LinkedHashSet。
     *
     * @param collection Collection 对象
     * @param <T>        元素类型
     * @return 新 LinkedHashSet，参数为 null 时返回空 LinkedHashSet
     */
    public static <T> Set<T> newLinkedHashSet(Collection<? extends T> collection) {
        return collection == null ? new LinkedHashSet<>() : new LinkedHashSet<>(collection);
    }

    /**
     * 根据可变参数创建 LinkedHashSet。
     *
     * <p>
     * LinkedHashSet 会保留元素插入顺序。
     * </p>
     *
     * @param elements 元素数组
     * @param <T>      元素类型
     * @return 新 LinkedHashSet，参数为 null 时返回空 LinkedHashSet
     */
    @SafeVarargs
    public static <T> Set<T> newLinkedHashSet(T... elements) {
        if (elements == null || elements.length == 0) {
            return new LinkedHashSet<>();
        }

        Set<T> set = new LinkedHashSet<>(calculateHashMapCapacity(elements.length));
        Collections.addAll(set, elements);
        return set;
    }

    /**
     * 创建空 TreeSet。
     *
     * @param <T> 元素类型
     * @return 空 TreeSet
     */
    public static <T> SortedSet<T> newTreeSet() {
        return new TreeSet<>();
    }

    /**
     * 根据 Comparator 创建空 TreeSet。
     *
     * @param comparator 排序比较器
     * @param <T>        元素类型
     * @return 空 TreeSet
     */
    public static <T> SortedSet<T> newTreeSet(Comparator<? super T> comparator) {
        return new TreeSet<>(comparator);
    }

    /**
     * 根据 Collection 创建 TreeSet。
     *
     * @param collection Collection 对象
     * @param <T>        元素类型
     * @return 新 TreeSet，参数为 null 时返回空 TreeSet
     */
    public static <T> SortedSet<T> newTreeSet(Collection<? extends T> collection) {
        SortedSet<T> set = new TreeSet<>();
        if (collection != null) {
            set.addAll(collection);
        }
        return set;
    }

    /**
     * 根据 Comparator 和 Collection 创建 TreeSet。
     *
     * @param comparator 排序比较器
     * @param collection Collection 对象
     * @param <T>        元素类型
     * @return 新 TreeSet，参数为 null 时返回空 TreeSet
     */
    public static <T> SortedSet<T> newTreeSet(Comparator<? super T> comparator, Collection<? extends T> collection) {
        SortedSet<T> set = new TreeSet<>(comparator);
        if (collection != null) {
            set.addAll(collection);
        }
        return set;
    }

    /**
     * 创建空 HashMap。
     *
     * @param <K> Key 类型
     * @param <V> Value 类型
     * @return 空 HashMap
     */
    public static <K, V> Map<K, V> newHashMap() {
        return new HashMap<>();
    }

    /**
     * 创建指定初始容量的 HashMap。
     *
     * @param initialCapacity 初始容量
     * @param <K>             Key 类型
     * @param <V>             Value 类型
     * @return 空 HashMap
     */
    public static <K, V> Map<K, V> newHashMap(int initialCapacity) {
        checkExpectedSize(initialCapacity);
        return new HashMap<>(initialCapacity);
    }

    /**
     * 根据 Map 创建 HashMap。
     *
     * @param map Map 对象
     * @param <K> Key 类型
     * @param <V> Value 类型
     * @return 新 HashMap，参数为 null 时返回空 HashMap
     */
    public static <K, V> Map<K, V> newHashMap(Map<? extends K, ? extends V> map) {
        return map == null ? new HashMap<>() : new HashMap<>(map);
    }

    /**
     * 创建空 LinkedHashMap。
     *
     * @param <K> Key 类型
     * @param <V> Value 类型
     * @return 空 LinkedHashMap
     */
    public static <K, V> Map<K, V> newLinkedHashMap() {
        return new LinkedHashMap<>();
    }

    /**
     * 创建指定初始容量的 LinkedHashMap。
     *
     * @param initialCapacity 初始容量
     * @param <K>             Key 类型
     * @param <V>             Value 类型
     * @return 空 LinkedHashMap
     */
    public static <K, V> Map<K, V> newLinkedHashMap(int initialCapacity) {
        checkExpectedSize(initialCapacity);
        return new LinkedHashMap<>(initialCapacity);
    }

    /**
     * 根据 Map 创建 LinkedHashMap。
     *
     * <p>
     * LinkedHashMap 会保留原 Map 遍历顺序。
     * </p>
     *
     * @param map Map 对象
     * @param <K> Key 类型
     * @param <V> Value 类型
     * @return 新 LinkedHashMap，参数为 null 时返回空 LinkedHashMap
     */
    public static <K, V> Map<K, V> newLinkedHashMap(Map<? extends K, ? extends V> map) {
        return map == null ? new LinkedHashMap<>() : new LinkedHashMap<>(map);
    }

    /**
     * 创建空 ConcurrentHashMap。
     *
     * @param <K> Key 类型
     * @param <V> Value 类型
     * @return 空 ConcurrentHashMap
     */
    public static <K, V> ConcurrentHashMap<K, V> newConcurrentHashMap() {
        return new ConcurrentHashMap<>();
    }

    /**
     * 创建指定初始容量的 ConcurrentHashMap。
     *
     * @param initialCapacity 初始容量
     * @param <K>             Key 类型
     * @param <V>             Value 类型
     * @return 空 ConcurrentHashMap
     */
    public static <K, V> ConcurrentHashMap<K, V> newConcurrentHashMap(int initialCapacity) {
        checkExpectedSize(initialCapacity);
        return new ConcurrentHashMap<>(initialCapacity);
    }

    /**
     * 根据 Map 创建 ConcurrentHashMap。
     *
     * @param map Map 对象
     * @param <K> Key 类型
     * @param <V> Value 类型
     * @return 新 ConcurrentHashMap，参数为 null 时返回空 ConcurrentHashMap
     */
    public static <K, V> ConcurrentHashMap<K, V> newConcurrentHashMap(Map<? extends K, ? extends V> map) {
        return map == null ? new ConcurrentHashMap<>() : new ConcurrentHashMap<>(map);
    }

    /**
     * 创建不可变 List。
     *
     * <p>
     * JDK 原生不可变集合不允许 null 元素。
     * </p>
     *
     * @param elements 元素数组
     * @param <T>      元素类型
     * @return 不可变 List，参数为 null 时返回空 List
     */
    @SafeVarargs
    public static <T> List<T> immutableList(T... elements) {
        return elements == null || elements.length == 0 ? List.of() : List.of(elements);
    }

    /**
     * 根据 Collection 创建不可变 List。
     *
     * <p>
     * JDK 原生不可变集合不允许 null 元素。
     * </p>
     *
     * @param collection Collection 对象
     * @param <T>        元素类型
     * @return 不可变 List，参数为 null 时返回空 List
     */
    public static <T> List<T> immutableList(Collection<? extends T> collection) {
        return collection == null || collection.isEmpty() ? List.of() : List.copyOf(collection);
    }

    /**
     * 创建不可变 Set。
     *
     * <p>
     * JDK 原生不可变集合不允许 null 元素。
     * </p>
     *
     * @param elements 元素数组
     * @param <T>      元素类型
     * @return 不可变 Set，参数为 null 时返回空 Set
     */
    @SafeVarargs
    public static <T> Set<T> immutableSet(T... elements) {
        return elements == null || elements.length == 0 ? Set.of() : Set.of(elements);
    }

    /**
     * 根据 Collection 创建不可变 Set。
     *
     * <p>
     * JDK 原生不可变集合不允许 null 元素。
     * </p>
     *
     * @param collection Collection 对象
     * @param <T>        元素类型
     * @return 不可变 Set，参数为 null 时返回空 Set
     */
    public static <T> Set<T> immutableSet(Collection<? extends T> collection) {
        return collection == null || collection.isEmpty() ? Set.of() : Set.copyOf(collection);
    }

    /**
     * 根据 Map 创建不可变 Map。
     *
     * <p>
     * JDK 原生不可变集合不允许 null key 和 null value。
     * </p>
     *
     * @param map Map 对象
     * @param <K> Key 类型
     * @param <V> Value 类型
     * @return 不可变 Map，参数为 null 时返回空 Map
     */
    public static <K, V> Map<K, V> immutableMap(Map<? extends K, ? extends V> map) {
        return map == null || map.isEmpty() ? Map.of() : Map.copyOf(map);
    }

    /**
     * 创建允许 null 元素的可变 List。
     *
     * @param elements 元素数组
     * @param <T>      元素类型
     * @return 可变 List，参数为 null 时返回空 ArrayList
     */
    @SafeVarargs
    public static <T> List<T> mutableList(T... elements) {
        return newArrayList(elements);
    }

    /**
     * 根据 Collection 创建允许 null 元素的可变 List。
     *
     * @param collection Collection 对象
     * @param <T>        元素类型
     * @return 可变 List，参数为 null 时返回空 ArrayList
     */
    public static <T> List<T> mutableList(Collection<? extends T> collection) {
        return newArrayList(collection);
    }

    /**
     * 创建允许 null 元素的可变 Set。
     *
     * @param elements 元素数组
     * @param <T>      元素类型
     * @return 可变 Set，参数为 null 时返回空 HashSet
     */
    @SafeVarargs
    public static <T> Set<T> mutableSet(T... elements) {
        return newHashSet(elements);
    }

    /**
     * 根据 Collection 创建允许 null 元素的可变 Set。
     *
     * @param collection Collection 对象
     * @param <T>        元素类型
     * @return 可变 Set，参数为 null 时返回空 HashSet
     */
    public static <T> Set<T> mutableSet(Collection<? extends T> collection) {
        return newHashSet(collection);
    }

    /**
     * 根据 Map 创建可变 Map。
     *
     * @param map Map 对象
     * @param <K> Key 类型
     * @param <V> Value 类型
     * @return 可变 Map，参数为 null 时返回空 HashMap
     */
    public static <K, V> Map<K, V> mutableMap(Map<? extends K, ? extends V> map) {
        return newHashMap(map);
    }

    /**
     * 创建保序可变 List。
     *
     * @param elements 元素数组
     * @param <T>      元素类型
     * @return 可变 ArrayList，参数为 null 时返回空 ArrayList
     */
    @SafeVarargs
    public static <T> List<T> orderedList(T... elements) {
        return newArrayList(elements);
    }

    /**
     * 创建保序去重可变 Set。
     *
     * @param elements 元素数组
     * @param <T>      元素类型
     * @return 可变 LinkedHashSet，参数为 null 时返回空 LinkedHashSet
     */
    @SafeVarargs
    public static <T> Set<T> orderedSet(T... elements) {
        return newLinkedHashSet(elements);
    }

    /**
     * 根据 Collection 创建保序去重可变 Set。
     *
     * @param collection Collection 对象
     * @param <T>        元素类型
     * @return 可变 LinkedHashSet，参数为 null 时返回空 LinkedHashSet
     */
    public static <T> Set<T> orderedSet(Collection<? extends T> collection) {
        return newLinkedHashSet(collection);
    }

    /**
     * 根据 Map 创建保序可变 Map。
     *
     * @param map Map 对象
     * @param <K> Key 类型
     * @param <V> Value 类型
     * @return 可变 LinkedHashMap，参数为 null 时返回空 LinkedHashMap
     */
    public static <K, V> Map<K, V> orderedMap(Map<? extends K, ? extends V> map) {
        return newLinkedHashMap(map);
    }

    /**
     * 计算适合 HashMap、HashSet、LinkedHashMap、LinkedHashSet 使用的初始容量。
     *
     * @param expectedSize 期望元素数量
     * @return 初始容量
     */
    private static int calculateHashMapCapacity(int expectedSize) {
        checkExpectedSize(expectedSize);
        if (expectedSize < 3) {
            return expectedSize + 1;
        }
        if (expectedSize < 1 << 30) {
            return (int) (expectedSize / 0.75F + 1.0F);
        }
        return Integer.MAX_VALUE;
    }

    /**
     * Collection 转 ArrayList。
     *
     * @param collection Collection 对象
     * @param <T>        元素类型
     * @return 可变 ArrayList，参数为 null 时返回空 ArrayList
     */
    public static <T> List<T> toList(Collection<? extends T> collection) {
        return collection == null ? new ArrayList<>() : new ArrayList<>(collection);
    }

    /**
     * Iterable 转 ArrayList。
     *
     * @param iterable Iterable 对象
     * @param <T>      元素类型
     * @return 可变 ArrayList，参数为 null 时返回空 ArrayList
     */
    public static <T> List<T> toList(Iterable<? extends T> iterable) {
        if (iterable == null) {
            return new ArrayList<>();
        }
        if (iterable instanceof Collection<? extends T> collection) {
            return new ArrayList<>(collection);
        }

        List<T> list = new ArrayList<>();
        for (T item : iterable) {
            list.add(item);
        }
        return list;
    }

    /**
     * Iterator 转 ArrayList。
     *
     * <p>
     * 该方法会消费 Iterator。
     * </p>
     *
     * @param iterator Iterator 对象
     * @param <T>      元素类型
     * @return 可变 ArrayList，参数为 null 时返回空 ArrayList
     */
    public static <T> List<T> toList(Iterator<? extends T> iterator) {
        List<T> list = new ArrayList<>();
        if (iterator == null) {
            return list;
        }

        while (iterator.hasNext()) {
            list.add(iterator.next());
        }
        return list;
    }

    /**
     * Stream 转 ArrayList。
     *
     * <p>
     * 该方法会消费 Stream，不会主动关闭 Stream。
     * </p>
     *
     * @param stream Stream 对象
     * @param <T>    元素类型
     * @return 可变 ArrayList，参数为 null 时返回空 ArrayList
     */
    public static <T> List<T> toList(Stream<? extends T> stream) {
        return stream == null ? new ArrayList<>() : stream.collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * 数组转 ArrayList。
     *
     * @param array 数组
     * @param <T>   元素类型
     * @return 可变 ArrayList，参数为 null 时返回空 ArrayList
     */
    public static <T> List<T> toList(T[] array) {
        if (array == null || array.length == 0) {
            return new ArrayList<>();
        }

        List<T> list = new ArrayList<>(array.length);
        Collections.addAll(list, array);
        return list;
    }

    /**
     * 任意数组转 ArrayList，支持对象数组和基本类型数组。
     *
     * @param array 数组对象
     * @return 可变 ArrayList，参数为 null 时返回空 ArrayList
     */
    public static List<Object> toListFromArray(Object array) {
        if (array == null) {
            return new ArrayList<>();
        }
        if (!array.getClass().isArray()) {
            throw new IllegalArgumentException("参数必须是数组类型");
        }

        int length = Array.getLength(array);
        List<Object> list = new ArrayList<>(length);
        for (int index = 0; index < length; index++) {
            list.add(Array.get(array, index));
        }
        return list;
    }

    /**
     * Collection 转 HashSet。
     *
     * @param collection Collection 对象
     * @param <T>        元素类型
     * @return 可变 HashSet，参数为 null 时返回空 HashSet
     */
    public static <T> Set<T> toSet(Collection<? extends T> collection) {
        return collection == null ? new HashSet<>() : new HashSet<>(collection);
    }

    /**
     * Iterable 转 HashSet。
     *
     * @param iterable Iterable 对象
     * @param <T>      元素类型
     * @return 可变 HashSet，参数为 null 时返回空 HashSet
     */
    public static <T> Set<T> toSet(Iterable<? extends T> iterable) {
        if (iterable == null) {
            return new HashSet<>();
        }
        if (iterable instanceof Collection<? extends T> collection) {
            return new HashSet<>(collection);
        }

        Set<T> set = new HashSet<>();
        for (T item : iterable) {
            set.add(item);
        }
        return set;
    }

    /**
     * Iterator 转 HashSet。
     *
     * <p>
     * 该方法会消费 Iterator。
     * </p>
     *
     * @param iterator Iterator 对象
     * @param <T>      元素类型
     * @return 可变 HashSet，参数为 null 时返回空 HashSet
     */
    public static <T> Set<T> toSet(Iterator<? extends T> iterator) {
        Set<T> set = new HashSet<>();
        if (iterator == null) {
            return set;
        }

        while (iterator.hasNext()) {
            set.add(iterator.next());
        }
        return set;
    }

    /**
     * Stream 转 HashSet。
     *
     * <p>
     * 该方法会消费 Stream，不会主动关闭 Stream。
     * </p>
     *
     * @param stream Stream 对象
     * @param <T>    元素类型
     * @return 可变 HashSet，参数为 null 时返回空 HashSet
     */
    public static <T> Set<T> toSet(Stream<? extends T> stream) {
        return stream == null ? new HashSet<>() : stream.collect(Collectors.toCollection(HashSet::new));
    }

    /**
     * 数组转 HashSet。
     *
     * @param array 数组
     * @param <T>   元素类型
     * @return 可变 HashSet，参数为 null 时返回空 HashSet
     */
    public static <T> Set<T> toSet(T[] array) {
        if (array == null || array.length == 0) {
            return new HashSet<>();
        }

        Set<T> set = new HashSet<>(calculateHashMapCapacity(array.length));
        Collections.addAll(set, array);
        return set;
    }

    /**
     * Collection 转 LinkedHashSet。
     *
     * <p>
     * LinkedHashSet 会保留原集合遍历顺序。
     * </p>
     *
     * @param collection Collection 对象
     * @param <T>        元素类型
     * @return 可变 LinkedHashSet，参数为 null 时返回空 LinkedHashSet
     */
    public static <T> Set<T> toLinkedHashSet(Collection<? extends T> collection) {
        return collection == null ? new LinkedHashSet<>() : new LinkedHashSet<>(collection);
    }

    /**
     * Iterable 转 LinkedHashSet。
     *
     * <p>
     * LinkedHashSet 会保留原 Iterable 遍历顺序。
     * </p>
     *
     * @param iterable Iterable 对象
     * @param <T>      元素类型
     * @return 可变 LinkedHashSet，参数为 null 时返回空 LinkedHashSet
     */
    public static <T> Set<T> toLinkedHashSet(Iterable<? extends T> iterable) {
        if (iterable == null) {
            return new LinkedHashSet<>();
        }
        if (iterable instanceof Collection<? extends T> collection) {
            return new LinkedHashSet<>(collection);
        }

        Set<T> set = new LinkedHashSet<>();
        for (T item : iterable) {
            set.add(item);
        }
        return set;
    }

    /**
     * Iterator 转 LinkedHashSet。
     *
     * <p>
     * 该方法会消费 Iterator，并保留 Iterator 遍历顺序。
     * </p>
     *
     * @param iterator Iterator 对象
     * @param <T>      元素类型
     * @return 可变 LinkedHashSet，参数为 null 时返回空 LinkedHashSet
     */
    public static <T> Set<T> toLinkedHashSet(Iterator<? extends T> iterator) {
        Set<T> set = new LinkedHashSet<>();
        if (iterator == null) {
            return set;
        }

        while (iterator.hasNext()) {
            set.add(iterator.next());
        }
        return set;
    }

    /**
     * Stream 转 LinkedHashSet。
     *
     * <p>
     * 该方法会消费 Stream，不会主动关闭 Stream。
     * </p>
     *
     * @param stream Stream 对象
     * @param <T>    元素类型
     * @return 可变 LinkedHashSet，参数为 null 时返回空 LinkedHashSet
     */
    public static <T> Set<T> toLinkedHashSet(Stream<? extends T> stream) {
        return stream == null ? new LinkedHashSet<>() : stream.collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * 数组转 LinkedHashSet。
     *
     * @param array 数组
     * @param <T>   元素类型
     * @return 可变 LinkedHashSet，参数为 null 时返回空 LinkedHashSet
     */
    public static <T> Set<T> toLinkedHashSet(T[] array) {
        if (array == null || array.length == 0) {
            return new LinkedHashSet<>();
        }

        Set<T> set = new LinkedHashSet<>(calculateHashMapCapacity(array.length));
        Collections.addAll(set, array);
        return set;
    }

    /**
     * Collection 转 TreeSet。
     *
     * @param collection Collection 对象
     * @param <T>        元素类型
     * @return 可变 TreeSet，参数为 null 时返回空 TreeSet
     */
    public static <T> SortedSet<T> toTreeSet(Collection<? extends T> collection) {
        SortedSet<T> set = new TreeSet<>();
        if (collection != null) {
            set.addAll(collection);
        }
        return set;
    }

    /**
     * Collection 转 TreeSet。
     *
     * @param collection Collection 对象
     * @param comparator 排序比较器
     * @param <T>        元素类型
     * @return 可变 TreeSet，参数为 null 时返回空 TreeSet
     */
    public static <T> SortedSet<T> toTreeSet(Collection<? extends T> collection, Comparator<? super T> comparator) {
        SortedSet<T> set = new TreeSet<>(comparator);
        if (collection != null) {
            set.addAll(collection);
        }
        return set;
    }

    /**
     * Collection 转不可变 List。
     *
     * <p>
     * JDK 原生不可变集合不允许 null 元素。
     * </p>
     *
     * @param collection Collection 对象
     * @param <T>        元素类型
     * @return 不可变 List，参数为 null 时返回空 List
     */
    public static <T> List<T> toImmutableList(Collection<? extends T> collection) {
        return collection == null || collection.isEmpty() ? List.of() : List.copyOf(collection);
    }

    /**
     * Iterable 转不可变 List。
     *
     * <p>
     * JDK 原生不可变集合不允许 null 元素。
     * </p>
     *
     * @param iterable Iterable 对象
     * @param <T>      元素类型
     * @return 不可变 List，参数为 null 时返回空 List
     */
    public static <T> List<T> toImmutableList(Iterable<? extends T> iterable) {
        List<T> list = toList(iterable);
        return list.isEmpty() ? List.of() : List.copyOf(list);
    }

    /**
     * Stream 转不可变 List。
     *
     * <p>
     * 该方法会消费 Stream，且 JDK 原生不可变集合不允许 null 元素。
     * </p>
     *
     * @param stream Stream 对象
     * @param <T>    元素类型
     * @return 不可变 List，参数为 null 时返回空 List
     */
    public static <T> List<T> toImmutableList(Stream<? extends T> stream) {
        return stream == null ? List.of() : stream.<T>map(Function.identity()).toList();
    }

    /**
     * Collection 转不可变 Set。
     *
     * <p>
     * JDK 原生不可变集合不允许 null 元素。
     * </p>
     *
     * @param collection Collection 对象
     * @param <T>        元素类型
     * @return 不可变 Set，参数为 null 时返回空 Set
     */
    public static <T> Set<T> toImmutableSet(Collection<? extends T> collection) {
        return collection == null || collection.isEmpty() ? Set.of() : Set.copyOf(collection);
    }

    /**
     * Iterable 转不可变 Set。
     *
     * <p>
     * JDK 原生不可变集合不允许 null 元素。
     * </p>
     *
     * @param iterable Iterable 对象
     * @param <T>      元素类型
     * @return 不可变 Set，参数为 null 时返回空 Set
     */
    public static <T> Set<T> toImmutableSet(Iterable<? extends T> iterable) {
        Set<T> set = toSet(iterable);
        return set.isEmpty() ? Set.of() : Set.copyOf(set);
    }

    /**
     * Stream 转不可变 Set。
     *
     * <p>
     * 该方法会消费 Stream，且 JDK 原生不可变集合不允许 null 元素。
     * </p>
     *
     * @param stream Stream 对象
     * @param <T>    元素类型
     * @return 不可变 Set，参数为 null 时返回空 Set
     */
    public static <T> Set<T> toImmutableSet(Stream<? extends T> stream) {
        return stream == null ? Set.of() : stream.collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Collection 转 Map，元素本身作为 Value。
     *
     * <p>
     * 如果 key 重复，保留后一个元素。
     * </p>
     *
     * @param collection Collection 对象
     * @param keyMapper  Key 映射函数
     * @param <T>        元素类型
     * @param <K>        Key 类型
     * @return 可变 LinkedHashMap，参数为空时返回空 LinkedHashMap
     */
    public static <T, K> Map<K, T> toMap(Collection<? extends T> collection, Function<? super T, ? extends K> keyMapper) {
        return toMap(collection, keyMapper, Function.identity(), (oldValue, newValue) -> newValue, LinkedHashMap::new);
    }

    /**
     * Collection 转 Map。
     *
     * <p>
     * 如果 key 重复，保留后一个 Value。
     * </p>
     *
     * @param collection  Collection 对象
     * @param keyMapper   Key 映射函数
     * @param valueMapper Value 映射函数
     * @param <T>         元素类型
     * @param <K>         Key 类型
     * @param <V>         Value 类型
     * @return 可变 LinkedHashMap，参数为空时返回空 LinkedHashMap
     */
    public static <T, K, V> Map<K, V> toMap(Collection<? extends T> collection,
                                            Function<? super T, ? extends K> keyMapper,
                                            Function<? super T, ? extends V> valueMapper) {
        return toMap(collection, keyMapper, valueMapper, (oldValue, newValue) -> newValue, LinkedHashMap::new);
    }

    /**
     * Collection 转 Map。
     *
     * @param collection    Collection 对象
     * @param keyMapper     Key 映射函数
     * @param valueMapper   Value 映射函数
     * @param mergeFunction Key 冲突时的 Value 合并函数
     * @param <T>           元素类型
     * @param <K>           Key 类型
     * @param <V>           Value 类型
     * @return 可变 LinkedHashMap，参数为空时返回空 LinkedHashMap
     */
    public static <T, K, V> Map<K, V> toMap(Collection<? extends T> collection,
                                            Function<? super T, ? extends K> keyMapper,
                                            Function<? super T, ? extends V> valueMapper,
                                            BinaryOperator<V> mergeFunction) {
        return toMap(collection, keyMapper, valueMapper, mergeFunction, LinkedHashMap::new);
    }

    /**
     * Collection 转指定类型 Map。
     *
     * @param collection    Collection 对象
     * @param keyMapper     Key 映射函数
     * @param valueMapper   Value 映射函数
     * @param mergeFunction Key 冲突时的 Value 合并函数
     * @param mapSupplier   Map 创建函数
     * @param <T>           元素类型
     * @param <K>           Key 类型
     * @param <V>           Value 类型
     * @param <M>           Map 类型
     * @return 指定类型 Map，参数为空时返回空 Map
     */
    public static <T, K, V, M extends Map<K, V>> M toMap(Collection<? extends T> collection,
                                                         Function<? super T, ? extends K> keyMapper,
                                                         Function<? super T, ? extends V> valueMapper,
                                                         BinaryOperator<V> mergeFunction,
                                                         Supplier<M> mapSupplier) {
        Objects.requireNonNull(keyMapper, "keyMapper 不能为 null");
        Objects.requireNonNull(valueMapper, "valueMapper 不能为 null");
        Objects.requireNonNull(mergeFunction, "mergeFunction 不能为 null");
        Objects.requireNonNull(mapSupplier, "mapSupplier 不能为 null");

        M map = mapSupplier.get();
        if (collection == null || collection.isEmpty()) {
            return map;
        }

        for (T item : collection) {
            K key = keyMapper.apply(item);
            V value = valueMapper.apply(item);
            map.merge(key, value, mergeFunction);
        }
        return map;
    }

    /**
     * Map 的 Key 转 ArrayList。
     *
     * @param map Map 对象
     * @param <K> Key 类型
     * @return 可变 ArrayList，参数为 null 时返回空 ArrayList
     */
    public static <K> List<K> keysToList(Map<? extends K, ?> map) {
        return map == null ? new ArrayList<>() : new ArrayList<>(map.keySet());
    }

    /**
     * Map 的 Value 转 ArrayList。
     *
     * @param map Map 对象
     * @param <V> Value 类型
     * @return 可变 ArrayList，参数为 null 时返回空 ArrayList
     */
    public static <V> List<V> valuesToList(Map<?, ? extends V> map) {
        return map == null ? new ArrayList<>() : new ArrayList<>(map.values());
    }

    /**
     * Map 的 Entry 转 ArrayList。
     *
     * @param map Map 对象
     * @param <K> Key 类型
     * @param <V> Value 类型
     * @return 可变 ArrayList，参数为 null 时返回空 ArrayList
     */
    public static <K, V> List<Map.Entry<K, V>> entriesToList(Map<? extends K, ? extends V> map) {
        List<Map.Entry<K, V>> list = new ArrayList<>();
        if (map == null || map.isEmpty()) {
            return list;
        }

        for (Map.Entry<? extends K, ? extends V> entry : map.entrySet()) {
            list.add(Map.entry(entry.getKey(), entry.getValue()));
        }
        return list;
    }

    /**
     * Map 的 Key 转 LinkedHashSet。
     *
     * @param map Map 对象
     * @param <K> Key 类型
     * @return 可变 LinkedHashSet，参数为 null 时返回空 LinkedHashSet
     */
    public static <K> Set<K> keysToSet(Map<? extends K, ?> map) {
        return map == null ? new LinkedHashSet<>() : new LinkedHashSet<>(map.keySet());
    }

    /**
     * Map 的 Value 转 LinkedHashSet。
     *
     * @param map Map 对象
     * @param <V> Value 类型
     * @return 可变 LinkedHashSet，参数为 null 时返回空 LinkedHashSet
     */
    public static <V> Set<V> valuesToSet(Map<?, ? extends V> map) {
        return map == null ? new LinkedHashSet<>() : new LinkedHashSet<>(map.values());
    }

    /**
     * Collection 转数组。
     *
     * @param collection Collection 对象
     * @param generator  数组创建函数
     * @param <T>        元素类型
     * @return 数组
     */
    public static <T> T[] toArray(Collection<? extends T> collection, IntFunction<T[]> generator) {
        Objects.requireNonNull(generator, "generator 不能为 null");
        if (collection == null || collection.isEmpty()) {
            return generator.apply(0);
        }
        return collection.toArray(generator);
    }

    /**
     * Iterable 转数组。
     *
     * @param iterable  Iterable 对象
     * @param generator 数组创建函数
     * @param <T>       元素类型
     * @return 数组
     */
    public static <T> T[] toArray(Iterable<? extends T> iterable, IntFunction<T[]> generator) {
        Objects.requireNonNull(generator, "generator 不能为 null");
        return toList(iterable).toArray(generator);
    }

    /**
     * Iterator 转数组。
     *
     * <p>
     * 该方法会消费 Iterator。
     * </p>
     *
     * @param iterator  Iterator 对象
     * @param generator 数组创建函数
     * @param <T>       元素类型
     * @return 数组
     */
    public static <T> T[] toArray(Iterator<? extends T> iterator, IntFunction<T[]> generator) {
        Objects.requireNonNull(generator, "generator 不能为 null");
        return toList(iterator).toArray(generator);
    }

    /**
     * 数组转 Stream。
     *
     * @param array 数组
     * @param <T>   元素类型
     * @return Stream，参数为 null 时返回空 Stream
     */
    public static <T> Stream<T> toStream(T[] array) {
        return array == null ? Stream.empty() : Arrays.stream(array);
    }

    /**
     * Collection 转 Stream。
     *
     * @param collection Collection 对象
     * @param <T>        元素类型
     * @return Stream，参数为 null 时返回空 Stream
     */
    public static <T> Stream<T> toStream(Collection<? extends T> collection) {
        return collection == null ? Stream.empty() : collection.stream().map(Function.identity());
    }

    /**
     * Iterable 转 Stream。
     *
     * @param iterable Iterable 对象
     * @param <T>      元素类型
     * @return Stream，参数为 null 时返回空 Stream
     */
    public static <T> Stream<T> toStream(Iterable<? extends T> iterable) {
        if (iterable == null) {
            return Stream.empty();
        }
        if (iterable instanceof Collection<? extends T> collection) {
            return collection.stream().map(Function.identity());
        }
        return StreamSupport.stream(iterable.spliterator(), false).map(Function.identity());
    }

    /**
     * Iterator 转 Stream。
     *
     * <p>
     * Stream 消费时会同步消费 Iterator。
     * </p>
     *
     * @param iterator Iterator 对象
     * @param <T>      元素类型
     * @return Stream，参数为 null 时返回空 Stream
     */
    public static <T> Stream<T> toStream(Iterator<? extends T> iterator) {
        if (iterator == null) {
            return Stream.empty();
        }
        Spliterator<? extends T> spliterator = Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED);
        return StreamSupport.stream(spliterator, false).map(Function.identity());
    }

    /**
     * Map 的 Entry 转 Stream。
     *
     * @param map Map 对象
     * @param <K> Key 类型
     * @param <V> Value 类型
     * @return Entry Stream，参数为 null 时返回空 Stream
     */
    public static <K, V> Stream<Map.Entry<K, V>> entryStream(Map<? extends K, ? extends V> map) {
        if (map == null || map.isEmpty()) {
            return Stream.empty();
        }

        return map.entrySet()
                .stream()
                .map(entry -> new AbstractMap.SimpleImmutableEntry<K, V>(entry.getKey(), entry.getValue()));
    }

    /**
     * Map 的 Key 转 Stream。
     *
     * @param map Map 对象
     * @param <K> Key 类型
     * @return Key Stream，参数为 null 时返回空 Stream
     */
    public static <K> Stream<K> keyStream(Map<? extends K, ?> map) {
        return map == null ? Stream.empty() : map.keySet().stream().map(Function.identity());
    }

    /**
     * Map 的 Value 转 Stream。
     *
     * @param map Map 对象
     * @param <V> Value 类型
     * @return Value Stream，参数为 null 时返回空 Stream
     */
    public static <V> Stream<V> valueStream(Map<?, ? extends V> map) {
        return map == null ? Stream.empty() : map.values().stream().map(Function.identity());
    }

    /**
     * Iterator 转 Iterable。
     *
     * <p>
     * 返回的 Iterable 只能按原 Iterator 的剩余元素遍历。
     * </p>
     *
     * @param iterator Iterator 对象
     * @param <T>      元素类型
     * @return Iterable，参数为 null 时返回空 Iterable
     */
    public static <T> Iterable<T> toIterable(Iterator<? extends T> iterator) {
        if (iterator == null) {
            return List.of();
        }
        return () -> new Iterator<>() {

            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public T next() {
                return iterator.next();
            }
        };
    }

    /**
     * 数组转 Iterable。
     *
     * @param array 数组
     * @param <T>   元素类型
     * @return Iterable，参数为 null 时返回空 Iterable
     */
    public static <T> Iterable<T> toIterable(T[] array) {
        return array == null ? List.of() : Arrays.asList(array);
    }

    /**
     * 获取 List 第一个元素。
     *
     * @param list List 对象
     * @param <T>  元素类型
     * @return 第一个元素，List 为空时返回 null
     */
    public static <T> T first(List<? extends T> list) {
        return isEmpty(list) ? null : list.getFirst();
    }

    /**
     * 获取 Collection 第一个元素。
     *
     * @param collection Collection 对象
     * @param <T>        元素类型
     * @return 第一个元素，Collection 为空时返回 null
     */
    public static <T> T first(Collection<? extends T> collection) {
        if (isEmpty(collection)) {
            return null;
        }
        return collection.iterator().next();
    }

    /**
     * 获取 Iterable 第一个元素。
     *
     * @param iterable Iterable 对象
     * @param <T>      元素类型
     * @return 第一个元素，Iterable 为空时返回 null
     */
    public static <T> T first(Iterable<? extends T> iterable) {
        if (iterable == null) {
            return null;
        }
        if (iterable instanceof Collection<? extends T> collection) {
            return first(collection);
        }

        Iterator<? extends T> iterator = iterable.iterator();
        return iterator.hasNext() ? iterator.next() : null;
    }

    /**
     * 获取 Iterator 第一个元素。
     *
     * <p>
     * 该方法会消费 Iterator 的一个元素。
     * </p>
     *
     * @param iterator Iterator 对象
     * @param <T>      元素类型
     * @return 第一个元素，Iterator 为空时返回 null
     */
    public static <T> T first(Iterator<? extends T> iterator) {
        return iterator == null || !iterator.hasNext() ? null : iterator.next();
    }

    /**
     * 获取数组第一个元素。
     *
     * @param array 数组
     * @param <T>   元素类型
     * @return 第一个元素，数组为空时返回 null
     */
    public static <T> T first(T[] array) {
        return isEmpty(array) ? null : array[0];
    }

    /**
     * 获取任意数组第一个元素，支持对象数组和基本类型数组。
     *
     * @param array 数组对象
     * @return 第一个元素，数组为空时返回 null
     */
    public static Object firstFromArray(Object array) {
        return isArrayEmpty(array) ? null : Array.get(array, 0);
    }

    /**
     * 获取 List 最后一个元素。
     *
     * @param list List 对象
     * @param <T>  元素类型
     * @return 最后一个元素，List 为空时返回 null
     */
    public static <T> T last(List<? extends T> list) {
        return isEmpty(list) ? null : list.getLast();
    }

    /**
     * 获取 Collection 最后一个元素。
     *
     * @param collection Collection 对象
     * @param <T>        元素类型
     * @return 最后一个元素，Collection 为空时返回 null
     */
    public static <T> T last(Collection<? extends T> collection) {
        if (isEmpty(collection)) {
            return null;
        }
        if (collection instanceof List<? extends T> list) {
            return last(list);
        }

        T result = null;
        for (T item : collection) {
            result = item;
        }
        return result;
    }

    /**
     * 获取 Iterable 最后一个元素。
     *
     * @param iterable Iterable 对象
     * @param <T>      元素类型
     * @return 最后一个元素，Iterable 为空时返回 null
     */
    public static <T> T last(Iterable<? extends T> iterable) {
        if (iterable == null) {
            return null;
        }
        if (iterable instanceof Collection<? extends T> collection) {
            return last(collection);
        }

        T result = null;
        for (T item : iterable) {
            result = item;
        }
        return result;
    }

    /**
     * 获取 Iterator 最后一个元素。
     *
     * <p>
     * 该方法会消费 Iterator。
     * </p>
     *
     * @param iterator Iterator 对象
     * @param <T>      元素类型
     * @return 最后一个元素，Iterator 为空时返回 null
     */
    public static <T> T last(Iterator<? extends T> iterator) {
        if (iterator == null) {
            return null;
        }

        T result = null;
        while (iterator.hasNext()) {
            result = iterator.next();
        }
        return result;
    }

    /**
     * 获取数组最后一个元素。
     *
     * @param array 数组
     * @param <T>   元素类型
     * @return 最后一个元素，数组为空时返回 null
     */
    public static <T> T last(T[] array) {
        return isEmpty(array) ? null : array[array.length - 1];
    }

    /**
     * 获取任意数组最后一个元素，支持对象数组和基本类型数组。
     *
     * @param array 数组对象
     * @return 最后一个元素，数组为空时返回 null
     */
    public static Object lastFromArray(Object array) {
        int length = arrayLength(array);
        return length == 0 ? null : Array.get(array, length - 1);
    }

    /**
     * 安全获取 List 指定下标元素。
     *
     * @param list  List 对象
     * @param index 下标，从 0 开始
     * @param <T>   元素类型
     * @return 指定下标元素，下标越界或 List 为空时返回 null
     */
    public static <T> T get(List<? extends T> list, int index) {
        if (isEmpty(list) || index < 0 || index >= list.size()) {
            return null;
        }
        return list.get(index);
    }

    /**
     * 安全获取 Iterable 指定下标元素。
     *
     * @param iterable Iterable 对象
     * @param index    下标，从 0 开始
     * @param <T>      元素类型
     * @return 指定下标元素，下标越界或 Iterable 为空时返回 null
     */
    public static <T> T get(Iterable<? extends T> iterable, int index) {
        if (iterable == null || index < 0) {
            return null;
        }
        if (iterable instanceof List<? extends T> list) {
            return get(list, index);
        }

        int currentIndex = 0;
        for (T item : iterable) {
            if (currentIndex == index) {
                return item;
            }
            currentIndex++;
        }
        return null;
    }

    /**
     * 安全获取 Iterator 指定下标元素。
     *
     * <p>
     * 该方法会消费 Iterator，直到读取到指定下标或遍历结束。
     * </p>
     *
     * @param iterator Iterator 对象
     * @param index    下标，从 0 开始
     * @param <T>      元素类型
     * @return 指定下标元素，下标越界或 Iterator 为空时返回 null
     */
    public static <T> T get(Iterator<? extends T> iterator, int index) {
        if (iterator == null || index < 0) {
            return null;
        }

        int currentIndex = 0;
        while (iterator.hasNext()) {
            T item = iterator.next();
            if (currentIndex == index) {
                return item;
            }
            currentIndex++;
        }
        return null;
    }

    /**
     * 安全获取数组指定下标元素。
     *
     * @param array 数组
     * @param index 下标，从 0 开始
     * @param <T>   元素类型
     * @return 指定下标元素，下标越界或数组为空时返回 null
     */
    public static <T> T get(T[] array, int index) {
        if (array == null || index < 0 || index >= array.length) {
            return null;
        }
        return array[index];
    }

    /**
     * 安全获取任意数组指定下标元素，支持对象数组和基本类型数组。
     *
     * @param array 数组对象
     * @param index 下标，从 0 开始
     * @return 指定下标元素，下标越界或数组为空时返回 null
     */
    public static Object getFromArray(Object array, int index) {
        int length = arrayLength(array);
        if (index < 0 || index >= length) {
            return null;
        }
        return Array.get(array, index);
    }

    /**
     * 获取 List 指定下标元素，越界时返回默认值。
     *
     * @param list         List 对象
     * @param index        下标，从 0 开始
     * @param defaultValue 默认值
     * @param <T>          元素类型
     * @return 指定下标元素，下标越界或 List 为空时返回默认值
     */
    public static <T> T getOrDefault(List<? extends T> list, int index, T defaultValue) {
        T value = get(list, index);
        return value == null ? defaultValue : value;
    }

    /**
     * 获取 Iterable 指定下标元素，越界时返回默认值。
     *
     * @param iterable     Iterable 对象
     * @param index        下标，从 0 开始
     * @param defaultValue 默认值
     * @param <T>          元素类型
     * @return 指定下标元素，下标越界或 Iterable 为空时返回默认值
     */
    public static <T> T getOrDefault(Iterable<? extends T> iterable, int index, T defaultValue) {
        T value = get(iterable, index);
        return value == null ? defaultValue : value;
    }

    /**
     * 获取数组指定下标元素，越界时返回默认值。
     *
     * @param array        数组
     * @param index        下标，从 0 开始
     * @param defaultValue 默认值
     * @param <T>          元素类型
     * @return 指定下标元素，下标越界或数组为空时返回默认值
     */
    public static <T> T getOrDefault(T[] array, int index, T defaultValue) {
        T value = get(array, index);
        return value == null ? defaultValue : value;
    }

    /**
     * 从 List 中获取随机元素。
     *
     * @param list List 对象
     * @param <T>  元素类型
     * @return 随机元素，List 为空时返回 null
     */
    public static <T> T random(List<? extends T> list) {
        if (isEmpty(list)) {
            return null;
        }
        int index = java.util.concurrent.ThreadLocalRandom.current().nextInt(list.size());
        return list.get(index);
    }

    /**
     * 从 Collection 中获取随机元素。
     *
     * @param collection Collection 对象
     * @param <T>        元素类型
     * @return 随机元素，Collection 为空时返回 null
     */
    public static <T> T random(Collection<? extends T> collection) {
        if (isEmpty(collection)) {
            return null;
        }
        if (collection instanceof List<? extends T> list) {
            return random(list);
        }

        int targetIndex = java.util.concurrent.ThreadLocalRandom.current().nextInt(collection.size());
        int currentIndex = 0;
        for (T item : collection) {
            if (currentIndex == targetIndex) {
                return item;
            }
            currentIndex++;
        }
        return null;
    }

    /**
     * 从数组中获取随机元素。
     *
     * @param array 数组
     * @param <T>   元素类型
     * @return 随机元素，数组为空时返回 null
     */
    public static <T> T random(T[] array) {
        if (isEmpty(array)) {
            return null;
        }
        int index = java.util.concurrent.ThreadLocalRandom.current().nextInt(array.length);
        return array[index];
    }

    /**
     * 从任意数组中获取随机元素，支持对象数组和基本类型数组。
     *
     * @param array 数组对象
     * @return 随机元素，数组为空时返回 null
     */
    public static Object randomFromArray(Object array) {
        int length = arrayLength(array);
        if (length == 0) {
            return null;
        }
        int index = java.util.concurrent.ThreadLocalRandom.current().nextInt(length);
        return Array.get(array, index);
    }

    /**
     * 获取 Map 第一个 Key。
     *
     * <p>
     * 取值顺序取决于 Map 本身的遍历顺序。
     * </p>
     *
     * @param map Map 对象
     * @param <K> Key 类型
     * @return 第一个 Key，Map 为空时返回 null
     */
    public static <K> K firstKey(Map<? extends K, ?> map) {
        if (isEmpty(map)) {
            return null;
        }
        return map.keySet().iterator().next();
    }

    /**
     * 获取 Map 第一个 Value。
     *
     * <p>
     * 取值顺序取决于 Map 本身的遍历顺序。
     * </p>
     *
     * @param map Map 对象
     * @param <V> Value 类型
     * @return 第一个 Value，Map 为空时返回 null
     */
    public static <V> V firstValue(Map<?, ? extends V> map) {
        if (isEmpty(map)) {
            return null;
        }
        return map.values().iterator().next();
    }

    /**
     * 获取 Map 第一个 Entry。
     *
     * <p>
     * 返回不可变 Entry 快照，取值顺序取决于 Map 本身的遍历顺序。
     * </p>
     *
     * @param map Map 对象
     * @param <K> Key 类型
     * @param <V> Value 类型
     * @return 第一个 Entry，Map 为空时返回 null
     */
    public static <K, V> Map.Entry<K, V> firstEntry(Map<? extends K, ? extends V> map) {
        if (isEmpty(map)) {
            return null;
        }

        Map.Entry<? extends K, ? extends V> entry = map.entrySet().iterator().next();
        return Map.entry(entry.getKey(), entry.getValue());
    }

    /**
     * 获取 Map 最后一个 Key。
     *
     * <p>
     * 取值顺序取决于 Map 本身的遍历顺序。
     * </p>
     *
     * @param map Map 对象
     * @param <K> Key 类型
     * @return 最后一个 Key，Map 为空时返回 null
     */
    public static <K> K lastKey(Map<? extends K, ?> map) {
        if (isEmpty(map)) {
            return null;
        }

        K result = null;
        for (K key : map.keySet()) {
            result = key;
        }
        return result;
    }

    /**
     * 获取 Map 最后一个 Value。
     *
     * <p>
     * 取值顺序取决于 Map 本身的遍历顺序。
     * </p>
     *
     * @param map Map 对象
     * @param <V> Value 类型
     * @return 最后一个 Value，Map 为空时返回 null
     */
    public static <V> V lastValue(Map<?, ? extends V> map) {
        if (isEmpty(map)) {
            return null;
        }

        V result = null;
        for (V value : map.values()) {
            result = value;
        }
        return result;
    }

    /**
     * 获取 Map 最后一个 Entry。
     *
     * <p>
     * 返回不可变 Entry 快照，取值顺序取决于 Map 本身的遍历顺序。
     * </p>
     *
     * @param map Map 对象
     * @param <K> Key 类型
     * @param <V> Value 类型
     * @return 最后一个 Entry，Map 为空时返回 null
     */
    public static <K, V> Map.Entry<K, V> lastEntry(Map<? extends K, ? extends V> map) {
        if (isEmpty(map)) {
            return null;
        }

        Map.Entry<? extends K, ? extends V> result = null;
        for (Map.Entry<? extends K, ? extends V> entry : map.entrySet()) {
            result = entry;
        }
        return result == null ? null : Map.entry(result.getKey(), result.getValue());
    }

    /**
     * 从 Map 中随机获取一个 Key。
     *
     * @param map Map 对象
     * @param <K> Key 类型
     * @return 随机 Key，Map 为空时返回 null
     */
    public static <K> K randomKey(Map<? extends K, ?> map) {
        if (isEmpty(map)) {
            return null;
        }

        int targetIndex = java.util.concurrent.ThreadLocalRandom.current().nextInt(map.size());
        int currentIndex = 0;
        for (K key : map.keySet()) {
            if (currentIndex == targetIndex) {
                return key;
            }
            currentIndex++;
        }
        return null;
    }

    /**
     * 从 Map 中随机获取一个 Value。
     *
     * @param map Map 对象
     * @param <V> Value 类型
     * @return 随机 Value，Map 为空时返回 null
     */
    public static <V> V randomValue(Map<?, ? extends V> map) {
        if (isEmpty(map)) {
            return null;
        }

        int targetIndex = java.util.concurrent.ThreadLocalRandom.current().nextInt(map.size());
        int currentIndex = 0;
        for (V value : map.values()) {
            if (currentIndex == targetIndex) {
                return value;
            }
            currentIndex++;
        }
        return null;
    }

    /**
     * 从 Map 中随机获取一个 Entry。
     *
     * <p>
     * 返回不可变 Entry 快照。
     * </p>
     *
     * @param map Map 对象
     * @param <K> Key 类型
     * @param <V> Value 类型
     * @return 随机 Entry，Map 为空时返回 null
     */
    public static <K, V> Map.Entry<K, V> randomEntry(Map<? extends K, ? extends V> map) {
        if (isEmpty(map)) {
            return null;
        }

        int targetIndex = java.util.concurrent.ThreadLocalRandom.current().nextInt(map.size());
        int currentIndex = 0;
        for (Map.Entry<? extends K, ? extends V> entry : map.entrySet()) {
            if (currentIndex == targetIndex) {
                return Map.entry(entry.getKey(), entry.getValue());
            }
            currentIndex++;
        }
        return null;
    }

    /**
     * 获取 List 第一个元素 Optional。
     *
     * @param list List 对象
     * @param <T>  元素类型
     * @return Optional 包装结果
     */
    public static <T> Optional<T> firstOptional(List<? extends T> list) {
        return Optional.ofNullable(first(list));
    }

    /**
     * 获取 List 最后一个元素 Optional。
     *
     * @param list List 对象
     * @param <T>  元素类型
     * @return Optional 包装结果
     */
    public static <T> Optional<T> lastOptional(List<? extends T> list) {
        return Optional.ofNullable(last(list));
    }

    /**
     * 获取 List 指定下标元素 Optional。
     *
     * @param list  List 对象
     * @param index 下标，从 0 开始
     * @param <T>   元素类型
     * @return Optional 包装结果
     */
    public static <T> Optional<T> getOptional(List<? extends T> list, int index) {
        return Optional.ofNullable(get(list, index));
    }

    /**
     * 从 List 中获取随机元素 Optional。
     *
     * @param list List 对象
     * @param <T>  元素类型
     * @return Optional 包装结果
     */
    public static <T> Optional<T> randomOptional(List<? extends T> list) {
        return Optional.ofNullable(random(list));
    }

    /**
     * 向集合中添加元素。
     *
     * @param collection 目标集合
     * @param element    元素
     * @param <T>        元素类型
     * @return true 表示添加成功，目标集合为 null 时返回 false
     */
    public static <T> boolean add(Collection<? super T> collection, T element) {
        if (collection == null) {
            return false;
        }
        return collection.add(element);
    }

    /**
     * 向集合中添加非 null 元素。
     *
     * @param collection 目标集合
     * @param element    元素
     * @param <T>        元素类型
     * @return true 表示添加成功，目标集合或元素为 null 时返回 false
     */
    public static <T> boolean addIfNotNull(Collection<? super T> collection, T element) {
        if (collection == null || element == null) {
            return false;
        }
        return collection.add(element);
    }

    /**
     * 向集合中添加不存在的元素。
     *
     * @param collection 目标集合
     * @param element    元素
     * @param <T>        元素类型
     * @return true 表示元素原本不存在且添加成功
     */
    public static <T> boolean addIfAbsent(Collection<? super T> collection, T element) {
        if (collection == null || collection.contains(element)) {
            return false;
        }
        return collection.add(element);
    }

    /**
     * 向集合中添加非 null 且不存在的元素。
     *
     * @param collection 目标集合
     * @param element    元素
     * @param <T>        元素类型
     * @return true 表示元素原本不存在且添加成功
     */
    public static <T> boolean addIfNotNullAndAbsent(Collection<? super T> collection, T element) {
        if (collection == null || element == null || collection.contains(element)) {
            return false;
        }
        return collection.add(element);
    }

    /**
     * 向 List 头部添加元素。
     *
     * @param list    目标 List
     * @param element 元素
     * @param <T>     元素类型
     * @return true 表示添加成功，目标 List 为 null 时返回 false
     */
    public static <T> boolean addFirst(List<? super T> list, T element) {
        if (list == null) {
            return false;
        }
        list.add(0, element);
        return true;
    }

    /**
     * 向 List 尾部添加元素。
     *
     * @param list    目标 List
     * @param element 元素
     * @param <T>     元素类型
     * @return true 表示添加成功，目标 List 为 null 时返回 false
     */
    public static <T> boolean addLast(List<? super T> list, T element) {
        if (list == null) {
            return false;
        }
        list.add(element);
        return true;
    }

    /**
     * 向 List 指定下标位置添加元素。
     *
     * <p>
     * index 小于 0 时添加到头部，index 大于当前长度时添加到尾部。
     * </p>
     *
     * @param list    目标 List
     * @param index   下标
     * @param element 元素
     * @param <T>     元素类型
     * @return true 表示添加成功，目标 List 为 null 时返回 false
     */
    public static <T> boolean addAt(List<? super T> list, int index, T element) {
        if (list == null) {
            return false;
        }

        int targetIndex = Math.max(0, Math.min(index, list.size()));
        list.add(targetIndex, element);
        return true;
    }

    /**
     * 向集合中批量添加数组元素。
     *
     * @param collection 目标集合
     * @param elements   元素数组
     * @param <T>        元素类型
     * @return true 表示目标集合发生变化
     */
    @SafeVarargs
    public static <T> boolean addAll(Collection<? super T> collection, T... elements) {
        if (collection == null || elements == null || elements.length == 0) {
            return false;
        }

        boolean changed = false;
        for (T element : elements) {
            changed |= collection.add(element);
        }
        return changed;
    }

    /**
     * 向集合中批量添加 Collection 元素。
     *
     * @param collection 目标集合
     * @param elements   待添加元素集合
     * @param <T>        元素类型
     * @return true 表示目标集合发生变化
     */
    public static <T> boolean addAll(Collection<? super T> collection, Collection<? extends T> elements) {
        if (collection == null || elements == null || elements.isEmpty()) {
            return false;
        }
        return collection.addAll(elements);
    }

    /**
     * 向集合中批量添加 Iterable 元素。
     *
     * @param collection 目标集合
     * @param elements   待添加元素
     * @param <T>        元素类型
     * @return true 表示目标集合发生变化
     */
    public static <T> boolean addAll(Collection<? super T> collection, Iterable<? extends T> elements) {
        if (collection == null || elements == null) {
            return false;
        }
        if (elements instanceof Collection<? extends T> elementCollection) {
            return addAll(collection, elementCollection);
        }

        boolean changed = false;
        for (T element : elements) {
            changed |= collection.add(element);
        }
        return changed;
    }

    /**
     * 向集合中批量添加 Iterator 元素。
     *
     * <p>
     * 该方法会消费 Iterator。
     * </p>
     *
     * @param collection 目标集合
     * @param iterator   待添加元素迭代器
     * @param <T>        元素类型
     * @return true 表示目标集合发生变化
     */
    public static <T> boolean addAll(Collection<? super T> collection, Iterator<? extends T> iterator) {
        if (collection == null || iterator == null) {
            return false;
        }

        boolean changed = false;
        while (iterator.hasNext()) {
            changed |= collection.add(iterator.next());
        }
        return changed;
    }

    /**
     * 向集合中批量添加数组中的非 null 元素。
     *
     * @param collection 目标集合
     * @param elements   元素数组
     * @param <T>        元素类型
     * @return true 表示目标集合发生变化
     */
    @SafeVarargs
    public static <T> boolean addAllIfNotNull(Collection<? super T> collection, T... elements) {
        if (collection == null || elements == null || elements.length == 0) {
            return false;
        }

        boolean changed = false;
        for (T element : elements) {
            if (element != null) {
                changed |= collection.add(element);
            }
        }
        return changed;
    }

    /**
     * 向集合中批量添加 Collection 中的非 null 元素。
     *
     * @param collection 目标集合
     * @param elements   待添加元素集合
     * @param <T>        元素类型
     * @return true 表示目标集合发生变化
     */
    public static <T> boolean addAllIfNotNull(Collection<? super T> collection, Collection<? extends T> elements) {
        if (collection == null || elements == null || elements.isEmpty()) {
            return false;
        }

        boolean changed = false;
        for (T element : elements) {
            if (element != null) {
                changed |= collection.add(element);
            }
        }
        return changed;
    }

    /**
     * 向集合中批量添加不存在的元素。
     *
     * @param collection 目标集合
     * @param elements   元素数组
     * @param <T>        元素类型
     * @return true 表示目标集合发生变化
     */
    @SafeVarargs
    public static <T> boolean addAllIfAbsent(Collection<? super T> collection, T... elements) {
        if (collection == null || elements == null || elements.length == 0) {
            return false;
        }

        boolean changed = false;
        for (T element : elements) {
            if (!collection.contains(element)) {
                changed |= collection.add(element);
            }
        }
        return changed;
    }

    /**
     * 向集合中批量添加不存在的元素。
     *
     * @param collection 目标集合
     * @param elements   待添加元素集合
     * @param <T>        元素类型
     * @return true 表示目标集合发生变化
     */
    public static <T> boolean addAllIfAbsent(Collection<? super T> collection, Collection<? extends T> elements) {
        if (collection == null || elements == null || elements.isEmpty()) {
            return false;
        }

        boolean changed = false;
        for (T element : elements) {
            if (!collection.contains(element)) {
                changed |= collection.add(element);
            }
        }
        return changed;
    }

    /**
     * 向集合中批量添加非 null 且不存在的元素。
     *
     * @param collection 目标集合
     * @param elements   元素数组
     * @param <T>        元素类型
     * @return true 表示目标集合发生变化
     */
    @SafeVarargs
    public static <T> boolean addAllIfNotNullAndAbsent(Collection<? super T> collection, T... elements) {
        if (collection == null || elements == null || elements.length == 0) {
            return false;
        }

        boolean changed = false;
        for (T element : elements) {
            if (element != null && !collection.contains(element)) {
                changed |= collection.add(element);
            }
        }
        return changed;
    }

    /**
     * 向集合中批量添加非 null 且不存在的元素。
     *
     * @param collection 目标集合
     * @param elements   待添加元素集合
     * @param <T>        元素类型
     * @return true 表示目标集合发生变化
     */
    public static <T> boolean addAllIfNotNullAndAbsent(Collection<? super T> collection, Collection<? extends T> elements) {
        if (collection == null || elements == null || elements.isEmpty()) {
            return false;
        }

        boolean changed = false;
        for (T element : elements) {
            if (element != null && !collection.contains(element)) {
                changed |= collection.add(element);
            }
        }
        return changed;
    }

    /**
     * 向 Map 中添加键值对。
     *
     * @param map   目标 Map
     * @param key   Key
     * @param value Value
     * @param <K>   Key 类型
     * @param <V>   Value 类型
     * @return true 表示添加成功，目标 Map 为 null 时返回 false
     */
    public static <K, V> boolean put(Map<? super K, ? super V> map, K key, V value) {
        if (map == null) {
            return false;
        }
        map.put(key, value);
        return true;
    }

    /**
     * 向 Map 中添加非 null Key 的键值对。
     *
     * @param map   目标 Map
     * @param key   Key
     * @param value Value
     * @param <K>   Key 类型
     * @param <V>   Value 类型
     * @return true 表示添加成功，目标 Map 或 Key 为 null 时返回 false
     */
    public static <K, V> boolean putIfKeyNotNull(Map<? super K, ? super V> map, K key, V value) {
        if (map == null || key == null) {
            return false;
        }
        map.put(key, value);
        return true;
    }

    /**
     * 向 Map 中添加非 null Value 的键值对。
     *
     * @param map   目标 Map
     * @param key   Key
     * @param value Value
     * @param <K>   Key 类型
     * @param <V>   Value 类型
     * @return true 表示添加成功，目标 Map 或 Value 为 null 时返回 false
     */
    public static <K, V> boolean putIfValueNotNull(Map<? super K, ? super V> map, K key, V value) {
        if (map == null || value == null) {
            return false;
        }
        map.put(key, value);
        return true;
    }

    /**
     * 向 Map 中添加 Key 和 Value 均非 null 的键值对。
     *
     * @param map   目标 Map
     * @param key   Key
     * @param value Value
     * @param <K>   Key 类型
     * @param <V>   Value 类型
     * @return true 表示添加成功，目标 Map、Key 或 Value 为 null 时返回 false
     */
    public static <K, V> boolean putIfNotNull(Map<? super K, ? super V> map, K key, V value) {
        if (map == null || key == null || value == null) {
            return false;
        }
        map.put(key, value);
        return true;
    }

    /**
     * 当 Key 不存在时向 Map 中添加键值对。
     *
     * @param map   目标 Map
     * @param key   Key
     * @param value Value
     * @param <K>   Key 类型
     * @param <V>   Value 类型
     * @return true 表示 Key 原本不存在且添加成功
     */
    public static <K, V> boolean putIfAbsent(Map<? super K, ? super V> map, K key, V value) {
        if (map == null || map.containsKey(key)) {
            return false;
        }
        map.put(key, value);
        return true;
    }

    /**
     * 向 Map 中批量添加另一个 Map。
     *
     * @param targetMap 目标 Map
     * @param sourceMap 来源 Map
     * @param <K>       Key 类型
     * @param <V>       Value 类型
     * @return true 表示目标 Map 发生变化
     */
    public static <K, V> boolean putAll(Map<? super K, ? super V> targetMap, Map<? extends K, ? extends V> sourceMap) {
        if (targetMap == null || sourceMap == null || sourceMap.isEmpty()) {
            return false;
        }

        targetMap.putAll(sourceMap);
        return true;
    }

    /**
     * 向 Map 中批量添加 Key 非 null 的键值对。
     *
     * @param targetMap 目标 Map
     * @param sourceMap 来源 Map
     * @param <K>       Key 类型
     * @param <V>       Value 类型
     * @return true 表示目标 Map 发生变化
     */
    public static <K, V> boolean putAllIfKeyNotNull(Map<? super K, ? super V> targetMap, Map<? extends K, ? extends V> sourceMap) {
        if (targetMap == null || sourceMap == null || sourceMap.isEmpty()) {
            return false;
        }

        boolean changed = false;
        for (Map.Entry<? extends K, ? extends V> entry : sourceMap.entrySet()) {
            if (entry.getKey() != null) {
                targetMap.put(entry.getKey(), entry.getValue());
                changed = true;
            }
        }
        return changed;
    }

    /**
     * 向 Map 中批量添加 Value 非 null 的键值对。
     *
     * @param targetMap 目标 Map
     * @param sourceMap 来源 Map
     * @param <K>       Key 类型
     * @param <V>       Value 类型
     * @return true 表示目标 Map 发生变化
     */
    public static <K, V> boolean putAllIfValueNotNull(Map<? super K, ? super V> targetMap, Map<? extends K, ? extends V> sourceMap) {
        if (targetMap == null || sourceMap == null || sourceMap.isEmpty()) {
            return false;
        }

        boolean changed = false;
        for (Map.Entry<? extends K, ? extends V> entry : sourceMap.entrySet()) {
            if (entry.getValue() != null) {
                targetMap.put(entry.getKey(), entry.getValue());
                changed = true;
            }
        }
        return changed;
    }

    /**
     * 向 Map 中批量添加 Key 和 Value 均非 null 的键值对。
     *
     * @param targetMap 目标 Map
     * @param sourceMap 来源 Map
     * @param <K>       Key 类型
     * @param <V>       Value 类型
     * @return true 表示目标 Map 发生变化
     */
    public static <K, V> boolean putAllIfNotNull(Map<? super K, ? super V> targetMap, Map<? extends K, ? extends V> sourceMap) {
        if (targetMap == null || sourceMap == null || sourceMap.isEmpty()) {
            return false;
        }

        boolean changed = false;
        for (Map.Entry<? extends K, ? extends V> entry : sourceMap.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                targetMap.put(entry.getKey(), entry.getValue());
                changed = true;
            }
        }
        return changed;
    }

    /**
     * 向 Map 中批量添加不存在的 Key。
     *
     * @param targetMap 目标 Map
     * @param sourceMap 来源 Map
     * @param <K>       Key 类型
     * @param <V>       Value 类型
     * @return true 表示目标 Map 发生变化
     */
    public static <K, V> boolean putAllIfAbsent(Map<? super K, ? super V> targetMap, Map<? extends K, ? extends V> sourceMap) {
        if (targetMap == null || sourceMap == null || sourceMap.isEmpty()) {
            return false;
        }

        boolean changed = false;
        for (Map.Entry<? extends K, ? extends V> entry : sourceMap.entrySet()) {
            if (!targetMap.containsKey(entry.getKey())) {
                targetMap.put(entry.getKey(), entry.getValue());
                changed = true;
            }
        }
        return changed;
    }

    /**
     * 合并多个 Collection 为 ArrayList。
     *
     * @param collections Collection 数组
     * @param <T>         元素类型
     * @return 合并后的可变 ArrayList
     */
    @SafeVarargs
    public static <T> List<T> mergeToList(Collection<? extends T>... collections) {
        List<T> result = new ArrayList<>();
        if (collections == null || collections.length == 0) {
            return result;
        }

        for (Collection<? extends T> collection : collections) {
            addAll(result, collection);
        }
        return result;
    }

    /**
     * 合并多个 Collection 为 ArrayList，并过滤 null 元素。
     *
     * @param collections Collection 数组
     * @param <T>         元素类型
     * @return 合并后的可变 ArrayList
     */
    @SafeVarargs
    public static <T> List<T> mergeToListIfNotNull(Collection<? extends T>... collections) {
        List<T> result = new ArrayList<>();
        if (collections == null || collections.length == 0) {
            return result;
        }

        for (Collection<? extends T> collection : collections) {
            addAllIfNotNull(result, collection);
        }
        return result;
    }

    /**
     * 合并多个 Collection 为 HashSet。
     *
     * @param collections Collection 数组
     * @param <T>         元素类型
     * @return 合并后的可变 HashSet
     */
    @SafeVarargs
    public static <T> Set<T> mergeToSet(Collection<? extends T>... collections) {
        Set<T> result = new HashSet<>();
        if (collections == null || collections.length == 0) {
            return result;
        }

        for (Collection<? extends T> collection : collections) {
            addAll(result, collection);
        }
        return result;
    }

    /**
     * 合并多个 Collection 为 LinkedHashSet。
     *
     * <p>
     * LinkedHashSet 会按集合传入顺序和元素遍历顺序保留首次出现位置。
     * </p>
     *
     * @param collections Collection 数组
     * @param <T>         元素类型
     * @return 合并后的可变 LinkedHashSet
     */
    @SafeVarargs
    public static <T> Set<T> mergeToLinkedHashSet(Collection<? extends T>... collections) {
        Set<T> result = new LinkedHashSet<>();
        if (collections == null || collections.length == 0) {
            return result;
        }

        for (Collection<? extends T> collection : collections) {
            addAll(result, collection);
        }
        return result;
    }

    /**
     * 合并多个 Collection 为 LinkedHashSet，并过滤 null 元素。
     *
     * @param collections Collection 数组
     * @param <T>         元素类型
     * @return 合并后的可变 LinkedHashSet
     */
    @SafeVarargs
    public static <T> Set<T> mergeToLinkedHashSetIfNotNull(Collection<? extends T>... collections) {
        Set<T> result = new LinkedHashSet<>();
        if (collections == null || collections.length == 0) {
            return result;
        }

        for (Collection<? extends T> collection : collections) {
            addAllIfNotNull(result, collection);
        }
        return result;
    }

    /**
     * 合并多个 Map 为 HashMap。
     *
     * <p>
     * Key 冲突时，后传入的 Map 会覆盖先传入的 Map。
     * </p>
     *
     * @param maps Map 数组
     * @param <K>  Key 类型
     * @param <V>  Value 类型
     * @return 合并后的可变 HashMap
     */
    @SafeVarargs
    public static <K, V> Map<K, V> mergeToMap(Map<? extends K, ? extends V>... maps) {
        Map<K, V> result = new HashMap<>();
        if (maps == null || maps.length == 0) {
            return result;
        }

        for (Map<? extends K, ? extends V> map : maps) {
            putAll(result, map);
        }
        return result;
    }

    /**
     * 合并多个 Map 为 LinkedHashMap。
     *
     * <p>
     * Key 冲突时，后传入的 Map 会覆盖先传入的 Map；Key 顺序保留首次插入顺序。
     * </p>
     *
     * @param maps Map 数组
     * @param <K>  Key 类型
     * @param <V>  Value 类型
     * @return 合并后的可变 LinkedHashMap
     */
    @SafeVarargs
    public static <K, V> Map<K, V> mergeToLinkedHashMap(Map<? extends K, ? extends V>... maps) {
        Map<K, V> result = new LinkedHashMap<>();
        if (maps == null || maps.length == 0) {
            return result;
        }

        for (Map<? extends K, ? extends V> map : maps) {
            putAll(result, map);
        }
        return result;
    }

    /**
     * 合并多个 Map 为 LinkedHashMap，并过滤 Key 和 Value 均非 null 的键值对。
     *
     * @param maps Map 数组
     * @param <K>  Key 类型
     * @param <V>  Value 类型
     * @return 合并后的可变 LinkedHashMap
     */
    @SafeVarargs
    public static <K, V> Map<K, V> mergeToLinkedHashMapIfNotNull(Map<? extends K, ? extends V>... maps) {
        Map<K, V> result = new LinkedHashMap<>();
        if (maps == null || maps.length == 0) {
            return result;
        }

        for (Map<? extends K, ? extends V> map : maps) {
            putAllIfNotNull(result, map);
        }
        return result;
    }

    /**
     * 合并两个 Collection 为指定目标集合。
     *
     * @param first    第一个集合
     * @param second   第二个集合
     * @param supplier 目标集合创建函数
     * @param <T>      元素类型
     * @param <C>      目标集合类型
     * @return 合并后的目标集合
     */
    public static <T, C extends Collection<T>> C merge(Collection<? extends T> first,
                                                       Collection<? extends T> second,
                                                       Supplier<C> supplier) {
        Objects.requireNonNull(supplier, "supplier 不能为 null");

        C result = supplier.get();
        addAll(result, first);
        addAll(result, second);
        return result;
    }

    /**
     * 合并多个 Collection 为指定目标集合。
     *
     * @param supplier    目标集合创建函数
     * @param collections Collection 数组
     * @param <T>         元素类型
     * @param <C>         目标集合类型
     * @return 合并后的目标集合
     */
    @SafeVarargs
    public static <T, C extends Collection<T>> C merge(Supplier<C> supplier, Collection<? extends T>... collections) {
        Objects.requireNonNull(supplier, "supplier 不能为 null");

        C result = supplier.get();
        if (collections == null || collections.length == 0) {
            return result;
        }

        for (Collection<? extends T> collection : collections) {
            addAll(result, collection);
        }
        return result;
    }

    /**
     * 合并两个 Map 为指定目标 Map。
     *
     * <p>
     * Key 冲突时，第二个 Map 会覆盖第一个 Map。
     * </p>
     *
     * @param first    第一个 Map
     * @param second   第二个 Map
     * @param supplier 目标 Map 创建函数
     * @param <K>      Key 类型
     * @param <V>      Value 类型
     * @param <M>      目标 Map 类型
     * @return 合并后的目标 Map
     */
    public static <K, V, M extends Map<K, V>> M merge(Map<? extends K, ? extends V> first,
                                                      Map<? extends K, ? extends V> second,
                                                      Supplier<M> supplier) {
        Objects.requireNonNull(supplier, "supplier 不能为 null");

        M result = supplier.get();
        putAll(result, first);
        putAll(result, second);
        return result;
    }

    /**
     * 合并多个 Map 为指定目标 Map。
     *
     * <p>
     * Key 冲突时，后传入的 Map 会覆盖先传入的 Map。
     * </p>
     *
     * @param supplier 目标 Map 创建函数
     * @param maps     Map 数组
     * @param <K>      Key 类型
     * @param <V>      Value 类型
     * @param <M>      目标 Map 类型
     * @return 合并后的目标 Map
     */
    @SafeVarargs
    public static <K, V, M extends Map<K, V>> M merge(Supplier<M> supplier, Map<? extends K, ? extends V>... maps) {
        Objects.requireNonNull(supplier, "supplier 不能为 null");

        M result = supplier.get();
        if (maps == null || maps.length == 0) {
            return result;
        }

        for (Map<? extends K, ? extends V> map : maps) {
            putAll(result, map);
        }
        return result;
    }

    /**
     * 按合并函数合并两个 Map。
     *
     * @param first         第一个 Map
     * @param second        第二个 Map
     * @param mergeFunction Key 冲突时的 Value 合并函数
     * @param <K>           Key 类型
     * @param <V>           Value 类型
     * @return 合并后的可变 LinkedHashMap
     */
    public static <K, V> Map<K, V> mergeMap(Map<? extends K, ? extends V> first,
                                            Map<? extends K, ? extends V> second,
                                            BinaryOperator<V> mergeFunction) {
        return mergeMap(LinkedHashMap::new, mergeFunction, first, second);
    }

    /**
     * 按合并函数合并多个 Map。
     *
     * @param supplier      目标 Map 创建函数
     * @param mergeFunction Key 冲突时的 Value 合并函数
     * @param maps          Map 数组
     * @param <K>           Key 类型
     * @param <V>           Value 类型
     * @param <M>           目标 Map 类型
     * @return 合并后的目标 Map
     */
    @SafeVarargs
    public static <K, V, M extends Map<K, V>> M mergeMap(Supplier<M> supplier,
                                                         BinaryOperator<V> mergeFunction,
                                                         Map<? extends K, ? extends V>... maps) {
        Objects.requireNonNull(supplier, "supplier 不能为 null");
        Objects.requireNonNull(mergeFunction, "mergeFunction 不能为 null");

        M result = supplier.get();
        if (maps == null || maps.length == 0) {
            return result;
        }

        for (Map<? extends K, ? extends V> map : maps) {
            if (map == null || map.isEmpty()) {
                continue;
            }
            for (Map.Entry<? extends K, ? extends V> entry : map.entrySet()) {
                result.merge(entry.getKey(), entry.getValue(), mergeFunction);
            }
        }
        return result;
    }

    /**
     * 过滤 Collection，并返回 ArrayList。
     *
     * @param collection Collection 对象
     * @param predicate  过滤条件
     * @param <T>        元素类型
     * @return 过滤后的可变 ArrayList
     */
    public static <T> List<T> filter(Collection<? extends T> collection, Predicate<? super T> predicate) {
        return filterToList(collection, predicate);
    }

    /**
     * 过滤 Iterable，并返回 ArrayList。
     *
     * @param iterable  Iterable 对象
     * @param predicate 过滤条件
     * @param <T>       元素类型
     * @return 过滤后的可变 ArrayList
     */
    public static <T> List<T> filter(Iterable<? extends T> iterable, Predicate<? super T> predicate) {
        return filterToList(iterable, predicate);
    }

    /**
     * 过滤 Iterator，并返回 ArrayList。
     *
     * <p>
     * 该方法会消费 Iterator。
     * </p>
     *
     * @param iterator  Iterator 对象
     * @param predicate 过滤条件
     * @param <T>       元素类型
     * @return 过滤后的可变 ArrayList
     */
    public static <T> List<T> filter(Iterator<? extends T> iterator, Predicate<? super T> predicate) {
        return filterToList(iterator, predicate);
    }

    /**
     * 过滤数组，并返回 ArrayList。
     *
     * @param array     数组
     * @param predicate 过滤条件
     * @param <T>       元素类型
     * @return 过滤后的可变 ArrayList
     */
    public static <T> List<T> filter(T[] array, Predicate<? super T> predicate) {
        return filterToList(array, predicate);
    }

    /**
     * 过滤 Collection，并返回 ArrayList。
     *
     * @param collection Collection 对象
     * @param predicate  过滤条件
     * @param <T>        元素类型
     * @return 过滤后的可变 ArrayList
     */
    public static <T> List<T> filterToList(Collection<? extends T> collection, Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate 不能为 null");

        List<T> result = new ArrayList<>();
        if (collection == null || collection.isEmpty()) {
            return result;
        }

        for (T item : collection) {
            if (predicate.test(item)) {
                result.add(item);
            }
        }
        return result;
    }

    /**
     * 过滤 Iterable，并返回 ArrayList。
     *
     * @param iterable  Iterable 对象
     * @param predicate 过滤条件
     * @param <T>       元素类型
     * @return 过滤后的可变 ArrayList
     */
    public static <T> List<T> filterToList(Iterable<? extends T> iterable, Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate 不能为 null");

        List<T> result = new ArrayList<>();
        if (iterable == null) {
            return result;
        }

        for (T item : iterable) {
            if (predicate.test(item)) {
                result.add(item);
            }
        }
        return result;
    }

    /**
     * 过滤 Iterator，并返回 ArrayList。
     *
     * <p>
     * 该方法会消费 Iterator。
     * </p>
     *
     * @param iterator  Iterator 对象
     * @param predicate 过滤条件
     * @param <T>       元素类型
     * @return 过滤后的可变 ArrayList
     */
    public static <T> List<T> filterToList(Iterator<? extends T> iterator, Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate 不能为 null");

        List<T> result = new ArrayList<>();
        if (iterator == null) {
            return result;
        }

        while (iterator.hasNext()) {
            T item = iterator.next();
            if (predicate.test(item)) {
                result.add(item);
            }
        }
        return result;
    }

    /**
     * 过滤数组，并返回 ArrayList。
     *
     * @param array     数组
     * @param predicate 过滤条件
     * @param <T>       元素类型
     * @return 过滤后的可变 ArrayList
     */
    public static <T> List<T> filterToList(T[] array, Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate 不能为 null");

        List<T> result = new ArrayList<>();
        if (array == null || array.length == 0) {
            return result;
        }

        for (T item : array) {
            if (predicate.test(item)) {
                result.add(item);
            }
        }
        return result;
    }

    /**
     * 过滤 Collection，并返回 LinkedHashSet。
     *
     * <p>
     * LinkedHashSet 会保留元素首次出现顺序。
     * </p>
     *
     * @param collection Collection 对象
     * @param predicate  过滤条件
     * @param <T>        元素类型
     * @return 过滤后的可变 LinkedHashSet
     */
    public static <T> Set<T> filterToSet(Collection<? extends T> collection, Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate 不能为 null");

        Set<T> result = new LinkedHashSet<>();
        if (collection == null || collection.isEmpty()) {
            return result;
        }

        for (T item : collection) {
            if (predicate.test(item)) {
                result.add(item);
            }
        }
        return result;
    }

    /**
     * 过滤 Iterable，并返回 LinkedHashSet。
     *
     * @param iterable  Iterable 对象
     * @param predicate 过滤条件
     * @param <T>       元素类型
     * @return 过滤后的可变 LinkedHashSet
     */
    public static <T> Set<T> filterToSet(Iterable<? extends T> iterable, Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate 不能为 null");

        Set<T> result = new LinkedHashSet<>();
        if (iterable == null) {
            return result;
        }

        for (T item : iterable) {
            if (predicate.test(item)) {
                result.add(item);
            }
        }
        return result;
    }

    /**
     * 过滤 Iterator，并返回 LinkedHashSet。
     *
     * <p>
     * 该方法会消费 Iterator。
     * </p>
     *
     * @param iterator  Iterator 对象
     * @param predicate 过滤条件
     * @param <T>       元素类型
     * @return 过滤后的可变 LinkedHashSet
     */
    public static <T> Set<T> filterToSet(Iterator<? extends T> iterator, Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate 不能为 null");

        Set<T> result = new LinkedHashSet<>();
        if (iterator == null) {
            return result;
        }

        while (iterator.hasNext()) {
            T item = iterator.next();
            if (predicate.test(item)) {
                result.add(item);
            }
        }
        return result;
    }

    /**
     * 过滤数组，并返回 LinkedHashSet。
     *
     * @param array     数组
     * @param predicate 过滤条件
     * @param <T>       元素类型
     * @return 过滤后的可变 LinkedHashSet
     */
    public static <T> Set<T> filterToSet(T[] array, Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate 不能为 null");

        Set<T> result = new LinkedHashSet<>();
        if (array == null || array.length == 0) {
            return result;
        }

        for (T item : array) {
            if (predicate.test(item)) {
                result.add(item);
            }
        }
        return result;
    }

    /**
     * 过滤 Collection，并返回指定类型集合。
     *
     * @param collection Collection 对象
     * @param predicate  过滤条件
     * @param supplier   目标集合创建函数
     * @param <T>        元素类型
     * @param <C>        目标集合类型
     * @return 过滤后的目标集合
     */
    public static <T, C extends Collection<T>> C filterTo(Collection<? extends T> collection,
                                                          Predicate<? super T> predicate,
                                                          Supplier<C> supplier) {
        Objects.requireNonNull(predicate, "predicate 不能为 null");
        Objects.requireNonNull(supplier, "supplier 不能为 null");

        C result = supplier.get();
        if (collection == null || collection.isEmpty()) {
            return result;
        }

        for (T item : collection) {
            if (predicate.test(item)) {
                result.add(item);
            }
        }
        return result;
    }

    /**
     * 过滤 Iterable，并返回指定类型集合。
     *
     * @param iterable  Iterable 对象
     * @param predicate 过滤条件
     * @param supplier  目标集合创建函数
     * @param <T>       元素类型
     * @param <C>       目标集合类型
     * @return 过滤后的目标集合
     */
    public static <T, C extends Collection<T>> C filterTo(Iterable<? extends T> iterable,
                                                          Predicate<? super T> predicate,
                                                          Supplier<C> supplier) {
        Objects.requireNonNull(predicate, "predicate 不能为 null");
        Objects.requireNonNull(supplier, "supplier 不能为 null");

        C result = supplier.get();
        if (iterable == null) {
            return result;
        }

        for (T item : iterable) {
            if (predicate.test(item)) {
                result.add(item);
            }
        }
        return result;
    }

    /**
     * 反向过滤 Collection，并返回 ArrayList。
     *
     * @param collection Collection 对象
     * @param predicate  排除条件
     * @param <T>        元素类型
     * @return 反向过滤后的可变 ArrayList
     */
    public static <T> List<T> reject(Collection<? extends T> collection, Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate 不能为 null");
        return filterToList(collection, predicate.negate());
    }

    /**
     * 反向过滤 Iterable，并返回 ArrayList。
     *
     * @param iterable  Iterable 对象
     * @param predicate 排除条件
     * @param <T>       元素类型
     * @return 反向过滤后的可变 ArrayList
     */
    public static <T> List<T> reject(Iterable<? extends T> iterable, Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate 不能为 null");
        return filterToList(iterable, predicate.negate());
    }

    /**
     * 反向过滤数组，并返回 ArrayList。
     *
     * @param array     数组
     * @param predicate 排除条件
     * @param <T>       元素类型
     * @return 反向过滤后的可变 ArrayList
     */
    public static <T> List<T> reject(T[] array, Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate 不能为 null");
        return filterToList(array, predicate.negate());
    }

    /**
     * 过滤 Collection 中的 null 元素。
     *
     * @param collection Collection 对象
     * @param <T>        元素类型
     * @return 不包含 null 元素的可变 ArrayList
     */
    public static <T> List<T> filterNotNull(Collection<? extends T> collection) {
        return filterToList(collection, Objects::nonNull);
    }

    /**
     * 过滤 Iterable 中的 null 元素。
     *
     * @param iterable Iterable 对象
     * @param <T>      元素类型
     * @return 不包含 null 元素的可变 ArrayList
     */
    public static <T> List<T> filterNotNull(Iterable<? extends T> iterable) {
        return filterToList(iterable, Objects::nonNull);
    }

    /**
     * 过滤 Iterator 中的 null 元素。
     *
     * <p>
     * 该方法会消费 Iterator。
     * </p>
     *
     * @param iterator Iterator 对象
     * @param <T>      元素类型
     * @return 不包含 null 元素的可变 ArrayList
     */
    public static <T> List<T> filterNotNull(Iterator<? extends T> iterator) {
        return filterToList(iterator, Objects::nonNull);
    }

    /**
     * 过滤数组中的 null 元素。
     *
     * @param array 数组
     * @param <T>   元素类型
     * @return 不包含 null 元素的可变 ArrayList
     */
    public static <T> List<T> filterNotNull(T[] array) {
        return filterToList(array, Objects::nonNull);
    }

    /**
     * 只保留 Collection 中的 null 元素。
     *
     * @param collection Collection 对象
     * @param <T>        元素类型
     * @return 只包含 null 元素的可变 ArrayList
     */
    public static <T> List<T> filterNull(Collection<? extends T> collection) {
        return filterToList(collection, Objects::isNull);
    }

    /**
     * 只保留数组中的 null 元素。
     *
     * @param array 数组
     * @param <T>   元素类型
     * @return 只包含 null 元素的可变 ArrayList
     */
    public static <T> List<T> filterNull(T[] array) {
        return filterToList(array, Objects::isNull);
    }

    /**
     * 查找 Collection 中第一个符合条件的元素。
     *
     * @param collection Collection 对象
     * @param predicate  查找条件
     * @param <T>        元素类型
     * @return 第一个符合条件的元素，未找到返回 null
     */
    public static <T> T findFirst(Collection<? extends T> collection, Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate 不能为 null");

        if (collection == null || collection.isEmpty()) {
            return null;
        }

        for (T item : collection) {
            if (predicate.test(item)) {
                return item;
            }
        }
        return null;
    }

    /**
     * 查找 Iterable 中第一个符合条件的元素。
     *
     * @param iterable  Iterable 对象
     * @param predicate 查找条件
     * @param <T>       元素类型
     * @return 第一个符合条件的元素，未找到返回 null
     */
    public static <T> T findFirst(Iterable<? extends T> iterable, Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate 不能为 null");

        if (iterable == null) {
            return null;
        }

        for (T item : iterable) {
            if (predicate.test(item)) {
                return item;
            }
        }
        return null;
    }

    /**
     * 查找 Iterator 中第一个符合条件的元素。
     *
     * <p>
     * 该方法会消费 Iterator，直到找到目标元素或遍历结束。
     * </p>
     *
     * @param iterator  Iterator 对象
     * @param predicate 查找条件
     * @param <T>       元素类型
     * @return 第一个符合条件的元素，未找到返回 null
     */
    public static <T> T findFirst(Iterator<? extends T> iterator, Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate 不能为 null");

        if (iterator == null) {
            return null;
        }

        while (iterator.hasNext()) {
            T item = iterator.next();
            if (predicate.test(item)) {
                return item;
            }
        }
        return null;
    }

    /**
     * 查找数组中第一个符合条件的元素。
     *
     * @param array     数组
     * @param predicate 查找条件
     * @param <T>       元素类型
     * @return 第一个符合条件的元素，未找到返回 null
     */
    public static <T> T findFirst(T[] array, Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate 不能为 null");

        if (array == null || array.length == 0) {
            return null;
        }

        for (T item : array) {
            if (predicate.test(item)) {
                return item;
            }
        }
        return null;
    }

    /**
     * 查找 Collection 中最后一个符合条件的元素。
     *
     * @param collection Collection 对象
     * @param predicate  查找条件
     * @param <T>        元素类型
     * @return 最后一个符合条件的元素，未找到返回 null
     */
    public static <T> T findLast(Collection<? extends T> collection, Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate 不能为 null");

        if (collection == null || collection.isEmpty()) {
            return null;
        }

        T result = null;
        for (T item : collection) {
            if (predicate.test(item)) {
                result = item;
            }
        }
        return result;
    }

    /**
     * 查找 Iterable 中最后一个符合条件的元素。
     *
     * @param iterable  Iterable 对象
     * @param predicate 查找条件
     * @param <T>       元素类型
     * @return 最后一个符合条件的元素，未找到返回 null
     */
    public static <T> T findLast(Iterable<? extends T> iterable, Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate 不能为 null");

        if (iterable == null) {
            return null;
        }

        T result = null;
        for (T item : iterable) {
            if (predicate.test(item)) {
                result = item;
            }
        }
        return result;
    }

    /**
     * 查找数组中最后一个符合条件的元素。
     *
     * @param array     数组
     * @param predicate 查找条件
     * @param <T>       元素类型
     * @return 最后一个符合条件的元素，未找到返回 null
     */
    public static <T> T findLast(T[] array, Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate 不能为 null");

        if (array == null || array.length == 0) {
            return null;
        }

        T result = null;
        for (T item : array) {
            if (predicate.test(item)) {
                result = item;
            }
        }
        return result;
    }

    /**
     * 查找 Collection 中第一个符合条件的元素，并返回 Optional。
     *
     * @param collection Collection 对象
     * @param predicate  查找条件
     * @param <T>        元素类型
     * @return Optional 包装结果
     */
    public static <T> Optional<T> findFirstOptional(Collection<? extends T> collection, Predicate<? super T> predicate) {
        return Optional.ofNullable(findFirst(collection, predicate));
    }

    /**
     * 查找 Iterable 中第一个符合条件的元素，并返回 Optional。
     *
     * @param iterable  Iterable 对象
     * @param predicate 查找条件
     * @param <T>       元素类型
     * @return Optional 包装结果
     */
    public static <T> Optional<T> findFirstOptional(Iterable<? extends T> iterable, Predicate<? super T> predicate) {
        return Optional.ofNullable(findFirst(iterable, predicate));
    }

    /**
     * 查找数组中第一个符合条件的元素，并返回 Optional。
     *
     * @param array     数组
     * @param predicate 查找条件
     * @param <T>       元素类型
     * @return Optional 包装结果
     */
    public static <T> Optional<T> findFirstOptional(T[] array, Predicate<? super T> predicate) {
        return Optional.ofNullable(findFirst(array, predicate));
    }

    /**
     * 查找 Collection 中最后一个符合条件的元素，并返回 Optional。
     *
     * @param collection Collection 对象
     * @param predicate  查找条件
     * @param <T>        元素类型
     * @return Optional 包装结果
     */
    public static <T> Optional<T> findLastOptional(Collection<? extends T> collection, Predicate<? super T> predicate) {
        return Optional.ofNullable(findLast(collection, predicate));
    }

    /**
     * 查找 Iterable 中最后一个符合条件的元素，并返回 Optional。
     *
     * @param iterable  Iterable 对象
     * @param predicate 查找条件
     * @param <T>       元素类型
     * @return Optional 包装结果
     */
    public static <T> Optional<T> findLastOptional(Iterable<? extends T> iterable, Predicate<? super T> predicate) {
        return Optional.ofNullable(findLast(iterable, predicate));
    }

    /**
     * 查找数组中最后一个符合条件的元素，并返回 Optional。
     *
     * @param array     数组
     * @param predicate 查找条件
     * @param <T>       元素类型
     * @return Optional 包装结果
     */
    public static <T> Optional<T> findLastOptional(T[] array, Predicate<? super T> predicate) {
        return Optional.ofNullable(findLast(array, predicate));
    }

    /**
     * 判断 Collection 中是否存在符合条件的元素。
     *
     * @param collection Collection 对象
     * @param predicate  判断条件
     * @param <T>        元素类型
     * @return true 表示存在符合条件的元素
     */
    public static <T> boolean anyMatch(Collection<? extends T> collection, Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate 不能为 null");

        if (collection == null || collection.isEmpty()) {
            return false;
        }

        for (T item : collection) {
            if (predicate.test(item)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断 Iterable 中是否存在符合条件的元素。
     *
     * @param iterable  Iterable 对象
     * @param predicate 判断条件
     * @param <T>       元素类型
     * @return true 表示存在符合条件的元素
     */
    public static <T> boolean anyMatch(Iterable<? extends T> iterable, Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate 不能为 null");

        if (iterable == null) {
            return false;
        }

        for (T item : iterable) {
            if (predicate.test(item)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断 Iterator 中是否存在符合条件的元素。
     *
     * <p>
     * 该方法会消费 Iterator，直到匹配成功或遍历结束。
     * </p>
     *
     * @param iterator  Iterator 对象
     * @param predicate 判断条件
     * @param <T>       元素类型
     * @return true 表示存在符合条件的元素
     */
    public static <T> boolean anyMatch(Iterator<? extends T> iterator, Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate 不能为 null");

        if (iterator == null) {
            return false;
        }

        while (iterator.hasNext()) {
            if (predicate.test(iterator.next())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断数组中是否存在符合条件的元素。
     *
     * @param array     数组
     * @param predicate 判断条件
     * @param <T>       元素类型
     * @return true 表示存在符合条件的元素
     */
    public static <T> boolean anyMatch(T[] array, Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate 不能为 null");

        if (array == null || array.length == 0) {
            return false;
        }

        for (T item : array) {
            if (predicate.test(item)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断 Collection 中所有元素是否都符合条件。
     *
     * <p>
     * 空集合返回 false，避免业务校验中将空数据误判为全部满足。
     * </p>
     *
     * @param collection Collection 对象
     * @param predicate  判断条件
     * @param <T>        元素类型
     * @return true 表示集合非空且所有元素都符合条件
     */
    public static <T> boolean allMatch(Collection<? extends T> collection, Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate 不能为 null");

        if (collection == null || collection.isEmpty()) {
            return false;
        }

        for (T item : collection) {
            if (!predicate.test(item)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 判断 Iterable 中所有元素是否都符合条件。
     *
     * <p>
     * 空 Iterable 返回 false，避免业务校验中将空数据误判为全部满足。
     * </p>
     *
     * @param iterable  Iterable 对象
     * @param predicate 判断条件
     * @param <T>       元素类型
     * @return true 表示 Iterable 非空且所有元素都符合条件
     */
    public static <T> boolean allMatch(Iterable<? extends T> iterable, Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate 不能为 null");

        if (iterable == null) {
            return false;
        }

        boolean hasElement = false;
        for (T item : iterable) {
            hasElement = true;
            if (!predicate.test(item)) {
                return false;
            }
        }
        return hasElement;
    }

    /**
     * 判断数组中所有元素是否都符合条件。
     *
     * <p>
     * 空数组返回 false，避免业务校验中将空数据误判为全部满足。
     * </p>
     *
     * @param array     数组
     * @param predicate 判断条件
     * @param <T>       元素类型
     * @return true 表示数组非空且所有元素都符合条件
     */
    public static <T> boolean allMatch(T[] array, Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate 不能为 null");

        if (array == null || array.length == 0) {
            return false;
        }

        for (T item : array) {
            if (!predicate.test(item)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 判断 Collection 中所有元素是否都不符合条件。
     *
     * @param collection Collection 对象
     * @param predicate  判断条件
     * @param <T>        元素类型
     * @return true 表示不存在符合条件的元素
     */
    public static <T> boolean noneMatch(Collection<? extends T> collection, Predicate<? super T> predicate) {
        return !anyMatch(collection, predicate);
    }

    /**
     * 判断 Iterable 中所有元素是否都不符合条件。
     *
     * @param iterable  Iterable 对象
     * @param predicate 判断条件
     * @param <T>       元素类型
     * @return true 表示不存在符合条件的元素
     */
    public static <T> boolean noneMatch(Iterable<? extends T> iterable, Predicate<? super T> predicate) {
        return !anyMatch(iterable, predicate);
    }

    /**
     * 判断数组中所有元素是否都不符合条件。
     *
     * @param array     数组
     * @param predicate 判断条件
     * @param <T>       元素类型
     * @return true 表示不存在符合条件的元素
     */
    public static <T> boolean noneMatch(T[] array, Predicate<? super T> predicate) {
        return !anyMatch(array, predicate);
    }

    /**
     * 统计 Collection 中符合条件的元素数量。
     *
     * @param collection Collection 对象
     * @param predicate  判断条件
     * @param <T>        元素类型
     * @return 符合条件的元素数量
     */
    public static <T> long countMatches(Collection<? extends T> collection, Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate 不能为 null");

        if (collection == null || collection.isEmpty()) {
            return 0L;
        }

        long count = 0L;
        for (T item : collection) {
            if (predicate.test(item)) {
                count++;
            }
        }
        return count;
    }

    /**
     * 统计 Iterable 中符合条件的元素数量。
     *
     * @param iterable  Iterable 对象
     * @param predicate 判断条件
     * @param <T>       元素类型
     * @return 符合条件的元素数量
     */
    public static <T> long countMatches(Iterable<? extends T> iterable, Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate 不能为 null");

        if (iterable == null) {
            return 0L;
        }

        long count = 0L;
        for (T item : iterable) {
            if (predicate.test(item)) {
                count++;
            }
        }
        return count;
    }

    /**
     * 统计数组中符合条件的元素数量。
     *
     * @param array     数组
     * @param predicate 判断条件
     * @param <T>       元素类型
     * @return 符合条件的元素数量
     */
    public static <T> long countMatches(T[] array, Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate 不能为 null");

        if (array == null || array.length == 0) {
            return 0L;
        }

        long count = 0L;
        for (T item : array) {
            if (predicate.test(item)) {
                count++;
            }
        }
        return count;
    }

    /**
     * 查找 List 中第一个符合条件的元素下标。
     *
     * @param list      List 对象
     * @param predicate 查找条件
     * @param <T>       元素类型
     * @return 第一个符合条件的元素下标，未找到返回 -1
     */
    public static <T> int indexOf(List<? extends T> list, Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate 不能为 null");

        if (list == null || list.isEmpty()) {
            return -1;
        }

        for (int index = 0; index < list.size(); index++) {
            if (predicate.test(list.get(index))) {
                return index;
            }
        }
        return -1;
    }

    /**
     * 查找数组中第一个符合条件的元素下标。
     *
     * @param array     数组
     * @param predicate 查找条件
     * @param <T>       元素类型
     * @return 第一个符合条件的元素下标，未找到返回 -1
     */
    public static <T> int indexOf(T[] array, Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate 不能为 null");

        if (array == null || array.length == 0) {
            return -1;
        }

        for (int index = 0; index < array.length; index++) {
            if (predicate.test(array[index])) {
                return index;
            }
        }
        return -1;
    }

    /**
     * 查找 List 中最后一个符合条件的元素下标。
     *
     * @param list      List 对象
     * @param predicate 查找条件
     * @param <T>       元素类型
     * @return 最后一个符合条件的元素下标，未找到返回 -1
     */
    public static <T> int lastIndexOf(List<? extends T> list, Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate 不能为 null");

        if (list == null || list.isEmpty()) {
            return -1;
        }

        for (int index = list.size() - 1; index >= 0; index--) {
            if (predicate.test(list.get(index))) {
                return index;
            }
        }
        return -1;
    }

    /**
     * 查找数组中最后一个符合条件的元素下标。
     *
     * @param array     数组
     * @param predicate 查找条件
     * @param <T>       元素类型
     * @return 最后一个符合条件的元素下标，未找到返回 -1
     */
    public static <T> int lastIndexOf(T[] array, Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate 不能为 null");

        if (array == null || array.length == 0) {
            return -1;
        }

        for (int index = array.length - 1; index >= 0; index--) {
            if (predicate.test(array[index])) {
                return index;
            }
        }
        return -1;
    }

    /**
     * 判断 Collection 是否包含指定元素。
     *
     * @param collection Collection 对象
     * @param element    目标元素
     * @return true 表示包含目标元素
     */
    public static boolean contains(Collection<?> collection, Object element) {
        return collection != null && collection.contains(element);
    }

    /**
     * 判断数组是否包含指定元素。
     *
     * @param array   数组
     * @param element 目标元素
     * @param <T>     元素类型
     * @return true 表示包含目标元素
     */
    public static <T> boolean contains(T[] array, T element) {
        if (array == null || array.length == 0) {
            return false;
        }

        for (T item : array) {
            if (Objects.equals(item, element)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断 Collection 是否包含任意一个目标元素。
     *
     * @param collection Collection 对象
     * @param targets    目标元素集合
     * @return true 表示至少包含一个目标元素
     */
    public static boolean containsAny(Collection<?> collection, Collection<?> targets) {
        if (collection == null || collection.isEmpty() || targets == null || targets.isEmpty()) {
            return false;
        }

        for (Object target : targets) {
            if (collection.contains(target)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断 Collection 是否包含全部目标元素。
     *
     * @param collection Collection 对象
     * @param targets    目标元素集合
     * @return true 表示包含全部目标元素
     */
    public static boolean containsAll(Collection<?> collection, Collection<?> targets) {
        if (collection == null || collection.isEmpty() || targets == null || targets.isEmpty()) {
            return false;
        }
        return collection.containsAll(targets);
    }

    /**
     * 判断 Collection 是否不包含指定元素。
     *
     * @param collection Collection 对象
     * @param element    目标元素
     * @return true 表示不包含目标元素
     */
    public static boolean notContains(Collection<?> collection, Object element) {
        return !contains(collection, element);
    }

    /**
     * 映射 Collection，并返回 ArrayList。
     *
     * @param collection Collection 对象
     * @param mapper     映射函数
     * @param <T>        原元素类型
     * @param <R>        目标元素类型
     * @return 映射后的可变 ArrayList
     */
    public static <T, R> List<R> map(Collection<? extends T> collection, Function<? super T, ? extends R> mapper) {
        return mapToList(collection, mapper);
    }

    /**
     * 映射 Iterable，并返回 ArrayList。
     *
     * @param iterable Iterable 对象
     * @param mapper   映射函数
     * @param <T>      原元素类型
     * @param <R>      目标元素类型
     * @return 映射后的可变 ArrayList
     */
    public static <T, R> List<R> map(Iterable<? extends T> iterable, Function<? super T, ? extends R> mapper) {
        return mapToList(iterable, mapper);
    }

    /**
     * 映射 Iterator，并返回 ArrayList。
     *
     * <p>
     * 该方法会消费 Iterator。
     * </p>
     *
     * @param iterator Iterator 对象
     * @param mapper   映射函数
     * @param <T>      原元素类型
     * @param <R>      目标元素类型
     * @return 映射后的可变 ArrayList
     */
    public static <T, R> List<R> map(Iterator<? extends T> iterator, Function<? super T, ? extends R> mapper) {
        return mapToList(iterator, mapper);
    }

    /**
     * 映射数组，并返回 ArrayList。
     *
     * @param array  数组
     * @param mapper 映射函数
     * @param <T>    原元素类型
     * @param <R>    目标元素类型
     * @return 映射后的可变 ArrayList
     */
    public static <T, R> List<R> map(T[] array, Function<? super T, ? extends R> mapper) {
        return mapToList(array, mapper);
    }

    /**
     * 映射 Collection，并返回 ArrayList。
     *
     * @param collection Collection 对象
     * @param mapper     映射函数
     * @param <T>        原元素类型
     * @param <R>        目标元素类型
     * @return 映射后的可变 ArrayList
     */
    public static <T, R> List<R> mapToList(Collection<? extends T> collection, Function<? super T, ? extends R> mapper) {
        Objects.requireNonNull(mapper, "mapper 不能为 null");

        List<R> result = new ArrayList<>();
        if (collection == null || collection.isEmpty()) {
            return result;
        }

        for (T item : collection) {
            result.add(mapper.apply(item));
        }
        return result;
    }

    /**
     * 映射 Iterable，并返回 ArrayList。
     *
     * @param iterable Iterable 对象
     * @param mapper   映射函数
     * @param <T>      原元素类型
     * @param <R>      目标元素类型
     * @return 映射后的可变 ArrayList
     */
    public static <T, R> List<R> mapToList(Iterable<? extends T> iterable, Function<? super T, ? extends R> mapper) {
        Objects.requireNonNull(mapper, "mapper 不能为 null");

        List<R> result = new ArrayList<>();
        if (iterable == null) {
            return result;
        }

        for (T item : iterable) {
            result.add(mapper.apply(item));
        }
        return result;
    }

    /**
     * 映射 Iterator，并返回 ArrayList。
     *
     * <p>
     * 该方法会消费 Iterator。
     * </p>
     *
     * @param iterator Iterator 对象
     * @param mapper   映射函数
     * @param <T>      原元素类型
     * @param <R>      目标元素类型
     * @return 映射后的可变 ArrayList
     */
    public static <T, R> List<R> mapToList(Iterator<? extends T> iterator, Function<? super T, ? extends R> mapper) {
        Objects.requireNonNull(mapper, "mapper 不能为 null");

        List<R> result = new ArrayList<>();
        if (iterator == null) {
            return result;
        }

        while (iterator.hasNext()) {
            result.add(mapper.apply(iterator.next()));
        }
        return result;
    }

    /**
     * 映射数组，并返回 ArrayList。
     *
     * @param array  数组
     * @param mapper 映射函数
     * @param <T>    原元素类型
     * @param <R>    目标元素类型
     * @return 映射后的可变 ArrayList
     */
    public static <T, R> List<R> mapToList(T[] array, Function<? super T, ? extends R> mapper) {
        Objects.requireNonNull(mapper, "mapper 不能为 null");

        List<R> result = new ArrayList<>();
        if (array == null || array.length == 0) {
            return result;
        }

        for (T item : array) {
            result.add(mapper.apply(item));
        }
        return result;
    }

    /**
     * 映射 Collection，并过滤 null 映射结果。
     *
     * @param collection Collection 对象
     * @param mapper     映射函数
     * @param <T>        原元素类型
     * @param <R>        目标元素类型
     * @return 映射后的可变 ArrayList，不包含 null
     */
    public static <T, R> List<R> mapNotNull(Collection<? extends T> collection, Function<? super T, ? extends R> mapper) {
        Objects.requireNonNull(mapper, "mapper 不能为 null");

        List<R> result = new ArrayList<>();
        if (collection == null || collection.isEmpty()) {
            return result;
        }

        for (T item : collection) {
            R value = mapper.apply(item);
            if (value != null) {
                result.add(value);
            }
        }
        return result;
    }

    /**
     * 映射 Iterable，并过滤 null 映射结果。
     *
     * @param iterable Iterable 对象
     * @param mapper   映射函数
     * @param <T>      原元素类型
     * @param <R>      目标元素类型
     * @return 映射后的可变 ArrayList，不包含 null
     */
    public static <T, R> List<R> mapNotNull(Iterable<? extends T> iterable, Function<? super T, ? extends R> mapper) {
        Objects.requireNonNull(mapper, "mapper 不能为 null");

        List<R> result = new ArrayList<>();
        if (iterable == null) {
            return result;
        }

        for (T item : iterable) {
            R value = mapper.apply(item);
            if (value != null) {
                result.add(value);
            }
        }
        return result;
    }

    /**
     * 映射数组，并过滤 null 映射结果。
     *
     * @param array  数组
     * @param mapper 映射函数
     * @param <T>    原元素类型
     * @param <R>    目标元素类型
     * @return 映射后的可变 ArrayList，不包含 null
     */
    public static <T, R> List<R> mapNotNull(T[] array, Function<? super T, ? extends R> mapper) {
        Objects.requireNonNull(mapper, "mapper 不能为 null");

        List<R> result = new ArrayList<>();
        if (array == null || array.length == 0) {
            return result;
        }

        for (T item : array) {
            R value = mapper.apply(item);
            if (value != null) {
                result.add(value);
            }
        }
        return result;
    }

    /**
     * 带下标映射 List，并返回 ArrayList。
     *
     * @param list   List 对象
     * @param mapper 映射函数，第一个参数为元素，第二个参数为下标
     * @param <T>    原元素类型
     * @param <R>    目标元素类型
     * @return 映射后的可变 ArrayList
     */
    public static <T, R> List<R> mapIndexed(List<? extends T> list, BiFunction<? super T, Integer, ? extends R> mapper) {
        Objects.requireNonNull(mapper, "mapper 不能为 null");

        List<R> result = new ArrayList<>();
        if (list == null || list.isEmpty()) {
            return result;
        }

        for (int index = 0; index < list.size(); index++) {
            result.add(mapper.apply(list.get(index), index));
        }
        return result;
    }

    /**
     * 带下标映射 Iterable，并返回 ArrayList。
     *
     * @param iterable Iterable 对象
     * @param mapper   映射函数，第一个参数为元素，第二个参数为下标
     * @param <T>      原元素类型
     * @param <R>      目标元素类型
     * @return 映射后的可变 ArrayList
     */
    public static <T, R> List<R> mapIndexed(Iterable<? extends T> iterable, BiFunction<? super T, Integer, ? extends R> mapper) {
        Objects.requireNonNull(mapper, "mapper 不能为 null");

        List<R> result = new ArrayList<>();
        if (iterable == null) {
            return result;
        }

        int index = 0;
        for (T item : iterable) {
            result.add(mapper.apply(item, index));
            index++;
        }
        return result;
    }

    /**
     * 带下标映射数组，并返回 ArrayList。
     *
     * @param array  数组
     * @param mapper 映射函数，第一个参数为元素，第二个参数为下标
     * @param <T>    原元素类型
     * @param <R>    目标元素类型
     * @return 映射后的可变 ArrayList
     */
    public static <T, R> List<R> mapIndexed(T[] array, BiFunction<? super T, Integer, ? extends R> mapper) {
        Objects.requireNonNull(mapper, "mapper 不能为 null");

        List<R> result = new ArrayList<>();
        if (array == null || array.length == 0) {
            return result;
        }

        for (int index = 0; index < array.length; index++) {
            result.add(mapper.apply(array[index], index));
        }
        return result;
    }

    /**
     * 映射 Collection，并返回 LinkedHashSet。
     *
     * <p>
     * LinkedHashSet 会按映射结果首次出现顺序去重。
     * </p>
     *
     * @param collection Collection 对象
     * @param mapper     映射函数
     * @param <T>        原元素类型
     * @param <R>        目标元素类型
     * @return 映射后的可变 LinkedHashSet
     */
    public static <T, R> Set<R> mapToSet(Collection<? extends T> collection, Function<? super T, ? extends R> mapper) {
        Objects.requireNonNull(mapper, "mapper 不能为 null");

        Set<R> result = new LinkedHashSet<>();
        if (collection == null || collection.isEmpty()) {
            return result;
        }

        for (T item : collection) {
            result.add(mapper.apply(item));
        }
        return result;
    }

    /**
     * 映射 Iterable，并返回 LinkedHashSet。
     *
     * @param iterable Iterable 对象
     * @param mapper   映射函数
     * @param <T>      原元素类型
     * @param <R>      目标元素类型
     * @return 映射后的可变 LinkedHashSet
     */
    public static <T, R> Set<R> mapToSet(Iterable<? extends T> iterable, Function<? super T, ? extends R> mapper) {
        Objects.requireNonNull(mapper, "mapper 不能为 null");

        Set<R> result = new LinkedHashSet<>();
        if (iterable == null) {
            return result;
        }

        for (T item : iterable) {
            result.add(mapper.apply(item));
        }
        return result;
    }

    /**
     * 映射数组，并返回 LinkedHashSet。
     *
     * @param array  数组
     * @param mapper 映射函数
     * @param <T>    原元素类型
     * @param <R>    目标元素类型
     * @return 映射后的可变 LinkedHashSet
     */
    public static <T, R> Set<R> mapToSet(T[] array, Function<? super T, ? extends R> mapper) {
        Objects.requireNonNull(mapper, "mapper 不能为 null");

        Set<R> result = new LinkedHashSet<>();
        if (array == null || array.length == 0) {
            return result;
        }

        for (T item : array) {
            result.add(mapper.apply(item));
        }
        return result;
    }

    /**
     * 映射 Collection，并返回指定类型集合。
     *
     * @param collection Collection 对象
     * @param mapper     映射函数
     * @param supplier   目标集合创建函数
     * @param <T>        原元素类型
     * @param <R>        目标元素类型
     * @param <C>        目标集合类型
     * @return 映射后的目标集合
     */
    public static <T, R, C extends Collection<R>> C mapTo(Collection<? extends T> collection,
                                                          Function<? super T, ? extends R> mapper,
                                                          Supplier<C> supplier) {
        Objects.requireNonNull(mapper, "mapper 不能为 null");
        Objects.requireNonNull(supplier, "supplier 不能为 null");

        C result = supplier.get();
        if (collection == null || collection.isEmpty()) {
            return result;
        }

        for (T item : collection) {
            result.add(mapper.apply(item));
        }
        return result;
    }

    /**
     * 映射 Iterable，并返回指定类型集合。
     *
     * @param iterable Iterable 对象
     * @param mapper   映射函数
     * @param supplier 目标集合创建函数
     * @param <T>      原元素类型
     * @param <R>      目标元素类型
     * @param <C>      目标集合类型
     * @return 映射后的目标集合
     */
    public static <T, R, C extends Collection<R>> C mapTo(Iterable<? extends T> iterable,
                                                          Function<? super T, ? extends R> mapper,
                                                          Supplier<C> supplier) {
        Objects.requireNonNull(mapper, "mapper 不能为 null");
        Objects.requireNonNull(supplier, "supplier 不能为 null");

        C result = supplier.get();
        if (iterable == null) {
            return result;
        }

        for (T item : iterable) {
            result.add(mapper.apply(item));
        }
        return result;
    }

    /**
     * 映射数组，并返回指定类型集合。
     *
     * @param array    数组
     * @param mapper   映射函数
     * @param supplier 目标集合创建函数
     * @param <T>      原元素类型
     * @param <R>      目标元素类型
     * @param <C>      目标集合类型
     * @return 映射后的目标集合
     */
    public static <T, R, C extends Collection<R>> C mapTo(T[] array,
                                                          Function<? super T, ? extends R> mapper,
                                                          Supplier<C> supplier) {
        Objects.requireNonNull(mapper, "mapper 不能为 null");
        Objects.requireNonNull(supplier, "supplier 不能为 null");

        C result = supplier.get();
        if (array == null || array.length == 0) {
            return result;
        }

        for (T item : array) {
            result.add(mapper.apply(item));
        }
        return result;
    }

    /**
     * 扁平映射 Collection，并返回 ArrayList。
     *
     * @param collection Collection 对象
     * @param mapper     映射函数，返回 Iterable
     * @param <T>        原元素类型
     * @param <R>        目标元素类型
     * @return 扁平映射后的可变 ArrayList
     */
    public static <T, R> List<R> flatMap(Collection<? extends T> collection,
                                         Function<? super T, ? extends Iterable<? extends R>> mapper) {
        return flatMapToList(collection, mapper);
    }

    /**
     * 扁平映射 Iterable，并返回 ArrayList。
     *
     * @param iterable Iterable 对象
     * @param mapper   映射函数，返回 Iterable
     * @param <T>      原元素类型
     * @param <R>      目标元素类型
     * @return 扁平映射后的可变 ArrayList
     */
    public static <T, R> List<R> flatMap(Iterable<? extends T> iterable,
                                         Function<? super T, ? extends Iterable<? extends R>> mapper) {
        return flatMapToList(iterable, mapper);
    }

    /**
     * 扁平映射数组，并返回 ArrayList。
     *
     * @param array  数组
     * @param mapper 映射函数，返回 Iterable
     * @param <T>    原元素类型
     * @param <R>    目标元素类型
     * @return 扁平映射后的可变 ArrayList
     */
    public static <T, R> List<R> flatMap(T[] array,
                                         Function<? super T, ? extends Iterable<? extends R>> mapper) {
        return flatMapToList(array, mapper);
    }

    /**
     * 扁平映射 Collection，并返回 ArrayList。
     *
     * @param collection Collection 对象
     * @param mapper     映射函数，返回 Iterable
     * @param <T>        原元素类型
     * @param <R>        目标元素类型
     * @return 扁平映射后的可变 ArrayList
     */
    public static <T, R> List<R> flatMapToList(Collection<? extends T> collection,
                                               Function<? super T, ? extends Iterable<? extends R>> mapper) {
        Objects.requireNonNull(mapper, "mapper 不能为 null");

        List<R> result = new ArrayList<>();
        if (collection == null || collection.isEmpty()) {
            return result;
        }

        for (T item : collection) {
            Iterable<? extends R> values = mapper.apply(item);
            if (values == null) {
                continue;
            }
            for (R value : values) {
                result.add(value);
            }
        }
        return result;
    }

    /**
     * 扁平映射 Iterable，并返回 ArrayList。
     *
     * @param iterable Iterable 对象
     * @param mapper   映射函数，返回 Iterable
     * @param <T>      原元素类型
     * @param <R>      目标元素类型
     * @return 扁平映射后的可变 ArrayList
     */
    public static <T, R> List<R> flatMapToList(Iterable<? extends T> iterable,
                                               Function<? super T, ? extends Iterable<? extends R>> mapper) {
        Objects.requireNonNull(mapper, "mapper 不能为 null");

        List<R> result = new ArrayList<>();
        if (iterable == null) {
            return result;
        }

        for (T item : iterable) {
            Iterable<? extends R> values = mapper.apply(item);
            if (values == null) {
                continue;
            }
            for (R value : values) {
                result.add(value);
            }
        }
        return result;
    }

    /**
     * 扁平映射 Iterator，并返回 ArrayList。
     *
     * <p>
     * 该方法会消费 Iterator。
     * </p>
     *
     * @param iterator Iterator 对象
     * @param mapper   映射函数，返回 Iterable
     * @param <T>      原元素类型
     * @param <R>      目标元素类型
     * @return 扁平映射后的可变 ArrayList
     */
    public static <T, R> List<R> flatMapToList(Iterator<? extends T> iterator,
                                               Function<? super T, ? extends Iterable<? extends R>> mapper) {
        Objects.requireNonNull(mapper, "mapper 不能为 null");

        List<R> result = new ArrayList<>();
        if (iterator == null) {
            return result;
        }

        while (iterator.hasNext()) {
            Iterable<? extends R> values = mapper.apply(iterator.next());
            if (values == null) {
                continue;
            }
            for (R value : values) {
                result.add(value);
            }
        }
        return result;
    }

    /**
     * 扁平映射数组，并返回 ArrayList。
     *
     * @param array  数组
     * @param mapper 映射函数，返回 Iterable
     * @param <T>    原元素类型
     * @param <R>    目标元素类型
     * @return 扁平映射后的可变 ArrayList
     */
    public static <T, R> List<R> flatMapToList(T[] array,
                                               Function<? super T, ? extends Iterable<? extends R>> mapper) {
        Objects.requireNonNull(mapper, "mapper 不能为 null");

        List<R> result = new ArrayList<>();
        if (array == null || array.length == 0) {
            return result;
        }

        for (T item : array) {
            Iterable<? extends R> values = mapper.apply(item);
            if (values == null) {
                continue;
            }
            for (R value : values) {
                result.add(value);
            }
        }
        return result;
    }

    /**
     * 扁平映射 Collection，并返回 LinkedHashSet。
     *
     * @param collection Collection 对象
     * @param mapper     映射函数，返回 Iterable
     * @param <T>        原元素类型
     * @param <R>        目标元素类型
     * @return 扁平映射后的可变 LinkedHashSet
     */
    public static <T, R> Set<R> flatMapToSet(Collection<? extends T> collection,
                                             Function<? super T, ? extends Iterable<? extends R>> mapper) {
        Objects.requireNonNull(mapper, "mapper 不能为 null");

        Set<R> result = new LinkedHashSet<>();
        if (collection == null || collection.isEmpty()) {
            return result;
        }

        for (T item : collection) {
            Iterable<? extends R> values = mapper.apply(item);
            if (values == null) {
                continue;
            }
            for (R value : values) {
                result.add(value);
            }
        }
        return result;
    }

    /**
     * 扁平映射 Iterable，并返回 LinkedHashSet。
     *
     * @param iterable Iterable 对象
     * @param mapper   映射函数，返回 Iterable
     * @param <T>      原元素类型
     * @param <R>      目标元素类型
     * @return 扁平映射后的可变 LinkedHashSet
     */
    public static <T, R> Set<R> flatMapToSet(Iterable<? extends T> iterable,
                                             Function<? super T, ? extends Iterable<? extends R>> mapper) {
        Objects.requireNonNull(mapper, "mapper 不能为 null");

        Set<R> result = new LinkedHashSet<>();
        if (iterable == null) {
            return result;
        }

        for (T item : iterable) {
            Iterable<? extends R> values = mapper.apply(item);
            if (values == null) {
                continue;
            }
            for (R value : values) {
                result.add(value);
            }
        }
        return result;
    }

    /**
     * 扁平映射 Collection，并返回指定类型集合。
     *
     * @param collection Collection 对象
     * @param mapper     映射函数，返回 Iterable
     * @param supplier   目标集合创建函数
     * @param <T>        原元素类型
     * @param <R>        目标元素类型
     * @param <C>        目标集合类型
     * @return 扁平映射后的目标集合
     */
    public static <T, R, C extends Collection<R>> C flatMapTo(Collection<? extends T> collection,
                                                              Function<? super T, ? extends Iterable<? extends R>> mapper,
                                                              Supplier<C> supplier) {
        Objects.requireNonNull(mapper, "mapper 不能为 null");
        Objects.requireNonNull(supplier, "supplier 不能为 null");

        C result = supplier.get();
        if (collection == null || collection.isEmpty()) {
            return result;
        }

        for (T item : collection) {
            Iterable<? extends R> values = mapper.apply(item);
            if (values == null) {
                continue;
            }
            for (R value : values) {
                result.add(value);
            }
        }
        return result;
    }

    /**
     * 提取 Collection 中对象字段，并返回 ArrayList。
     *
     * <p>
     * 该方法是 mapToList 的语义化别名，适合 User::getId、Order::getNo 等字段提取场景。
     * </p>
     *
     * @param collection Collection 对象
     * @param extractor  字段提取函数
     * @param <T>        对象类型
     * @param <R>        字段类型
     * @return 字段值列表
     */
    public static <T, R> List<R> extractToList(Collection<? extends T> collection, Function<? super T, ? extends R> extractor) {
        return mapToList(collection, extractor);
    }

    /**
     * 提取 Iterable 中对象字段，并返回 ArrayList。
     *
     * @param iterable  Iterable 对象
     * @param extractor 字段提取函数
     * @param <T>       对象类型
     * @param <R>       字段类型
     * @return 字段值列表
     */
    public static <T, R> List<R> extractToList(Iterable<? extends T> iterable, Function<? super T, ? extends R> extractor) {
        return mapToList(iterable, extractor);
    }

    /**
     * 提取数组中对象字段，并返回 ArrayList。
     *
     * @param array     数组
     * @param extractor 字段提取函数
     * @param <T>       对象类型
     * @param <R>       字段类型
     * @return 字段值列表
     */
    public static <T, R> List<R> extractToList(T[] array, Function<? super T, ? extends R> extractor) {
        return mapToList(array, extractor);
    }

    /**
     * 提取 Collection 中对象字段，并过滤 null 字段值。
     *
     * @param collection Collection 对象
     * @param extractor  字段提取函数
     * @param <T>        对象类型
     * @param <R>        字段类型
     * @return 字段值列表，不包含 null
     */
    public static <T, R> List<R> extractNotNullToList(Collection<? extends T> collection, Function<? super T, ? extends R> extractor) {
        return mapNotNull(collection, extractor);
    }

    /**
     * 提取 Collection 中对象字段，并返回 LinkedHashSet。
     *
     * <p>
     * LinkedHashSet 会按字段值首次出现顺序去重。
     * </p>
     *
     * @param collection Collection 对象
     * @param extractor  字段提取函数
     * @param <T>        对象类型
     * @param <R>        字段类型
     * @return 字段值集合
     */
    public static <T, R> Set<R> extractToSet(Collection<? extends T> collection, Function<? super T, ? extends R> extractor) {
        return mapToSet(collection, extractor);
    }

    /**
     * 提取数组中对象字段，并返回 LinkedHashSet。
     *
     * @param array     数组
     * @param extractor 字段提取函数
     * @param <T>       对象类型
     * @param <R>       字段类型
     * @return 字段值集合
     */
    public static <T, R> Set<R> extractToSet(T[] array, Function<? super T, ? extends R> extractor) {
        return mapToSet(array, extractor);
    }

    /**
     * 将 Collection 转换为另一个 Collection 类型。
     *
     * <p>
     * 该方法是 mapTo 的语义化别名，适合 DTO、VO、Entity 之间转换。
     * </p>
     *
     * @param collection Collection 对象
     * @param converter  转换函数
     * @param supplier   目标集合创建函数
     * @param <T>        原元素类型
     * @param <R>        目标元素类型
     * @param <C>        目标集合类型
     * @return 转换后的目标集合
     */
    public static <T, R, C extends Collection<R>> C convertTo(Collection<? extends T> collection,
                                                              Function<? super T, ? extends R> converter,
                                                              Supplier<C> supplier) {
        return mapTo(collection, converter, supplier);
    }

    /**
     * 将 Collection 转换为 ArrayList。
     *
     * <p>
     * 该方法是 mapToList 的语义化别名，适合 DTO、VO、Entity 之间转换。
     * </p>
     *
     * @param collection Collection 对象
     * @param converter  转换函数
     * @param <T>        原元素类型
     * @param <R>        目标元素类型
     * @return 转换后的可变 ArrayList
     */
    public static <T, R> List<R> convertToList(Collection<? extends T> collection, Function<? super T, ? extends R> converter) {
        return mapToList(collection, converter);
    }

    /**
     * 将 Iterable 转换为 ArrayList。
     *
     * @param iterable  Iterable 对象
     * @param converter 转换函数
     * @param <T>       原元素类型
     * @param <R>       目标元素类型
     * @return 转换后的可变 ArrayList
     */
    public static <T, R> List<R> convertToList(Iterable<? extends T> iterable, Function<? super T, ? extends R> converter) {
        return mapToList(iterable, converter);
    }

    /**
     * 将数组转换为 ArrayList。
     *
     * @param array     数组
     * @param converter 转换函数
     * @param <T>       原元素类型
     * @param <R>       目标元素类型
     * @return 转换后的可变 ArrayList
     */
    public static <T, R> List<R> convertToList(T[] array, Function<? super T, ? extends R> converter) {
        return mapToList(array, converter);
    }

    /**
     * 将 Collection 转换为 LinkedHashSet。
     *
     * @param collection Collection 对象
     * @param converter  转换函数
     * @param <T>        原元素类型
     * @param <R>        目标元素类型
     * @return 转换后的可变 LinkedHashSet
     */
    public static <T, R> Set<R> convertToSet(Collection<? extends T> collection, Function<? super T, ? extends R> converter) {
        return mapToSet(collection, converter);
    }

    /**
     * 映射 Map 的 Value，并保留原 Key。
     *
     * @param map         Map 对象
     * @param valueMapper Value 映射函数
     * @param <K>         Key 类型
     * @param <V>         原 Value 类型
     * @param <R>         目标 Value 类型
     * @return 映射后的可变 LinkedHashMap
     */
    public static <K, V, R> Map<K, R> mapValues(Map<? extends K, ? extends V> map,
                                                Function<? super V, ? extends R> valueMapper) {
        Objects.requireNonNull(valueMapper, "valueMapper 不能为 null");

        Map<K, R> result = new LinkedHashMap<>();
        if (map == null || map.isEmpty()) {
            return result;
        }

        for (Map.Entry<? extends K, ? extends V> entry : map.entrySet()) {
            result.put(entry.getKey(), valueMapper.apply(entry.getValue()));
        }
        return result;
    }

    /**
     * 映射 Map 的 Key，并保留原 Value。
     *
     * <p>
     * 如果映射后的 Key 重复，后面的 Value 会覆盖前面的 Value。
     * </p>
     *
     * @param map       Map 对象
     * @param keyMapper Key 映射函数
     * @param <K>       原 Key 类型
     * @param <R>       目标 Key 类型
     * @param <V>       Value 类型
     * @return 映射后的可变 LinkedHashMap
     */
    public static <K, R, V> Map<R, V> mapKeys(Map<? extends K, ? extends V> map,
                                              Function<? super K, ? extends R> keyMapper) {
        return mapKeys(map, keyMapper, (oldValue, newValue) -> newValue);
    }

    /**
     * 映射 Map 的 Key，并保留原 Value。
     *
     * @param map           Map 对象
     * @param keyMapper     Key 映射函数
     * @param mergeFunction Key 冲突时的 Value 合并函数
     * @param <K>           原 Key 类型
     * @param <R>           目标 Key 类型
     * @param <V>           Value 类型
     * @return 映射后的可变 LinkedHashMap
     */
    public static <K, R, V> Map<R, V> mapKeys(Map<? extends K, ? extends V> map,
                                              Function<? super K, ? extends R> keyMapper,
                                              BinaryOperator<V> mergeFunction) {
        Objects.requireNonNull(keyMapper, "keyMapper 不能为 null");
        Objects.requireNonNull(mergeFunction, "mergeFunction 不能为 null");

        Map<R, V> result = new LinkedHashMap<>();
        if (map == null || map.isEmpty()) {
            return result;
        }

        for (Map.Entry<? extends K, ? extends V> entry : map.entrySet()) {
            result.merge(keyMapper.apply(entry.getKey()), entry.getValue(), mergeFunction);
        }
        return result;
    }

    /**
     * 同时映射 Map 的 Key 和 Value。
     *
     * <p>
     * 如果映射后的 Key 重复，后面的 Value 会覆盖前面的 Value。
     * </p>
     *
     * @param map         Map 对象
     * @param keyMapper   Key 映射函数
     * @param valueMapper Value 映射函数
     * @param <K>         原 Key 类型
     * @param <V>         原 Value 类型
     * @param <RK>        目标 Key 类型
     * @param <RV>        目标 Value 类型
     * @return 映射后的可变 LinkedHashMap
     */
    public static <K, V, RK, RV> Map<RK, RV> mapEntries(Map<? extends K, ? extends V> map,
                                                        Function<? super K, ? extends RK> keyMapper,
                                                        Function<? super V, ? extends RV> valueMapper) {
        return mapEntries(map, keyMapper, valueMapper, (oldValue, newValue) -> newValue);
    }

    /**
     * 同时映射 Map 的 Key 和 Value。
     *
     * @param map           Map 对象
     * @param keyMapper     Key 映射函数
     * @param valueMapper   Value 映射函数
     * @param mergeFunction Key 冲突时的 Value 合并函数
     * @param <K>           原 Key 类型
     * @param <V>           原 Value 类型
     * @param <RK>          目标 Key 类型
     * @param <RV>          目标 Value 类型
     * @return 映射后的可变 LinkedHashMap
     */
    public static <K, V, RK, RV> Map<RK, RV> mapEntries(Map<? extends K, ? extends V> map,
                                                        Function<? super K, ? extends RK> keyMapper,
                                                        Function<? super V, ? extends RV> valueMapper,
                                                        BinaryOperator<RV> mergeFunction) {
        Objects.requireNonNull(keyMapper, "keyMapper 不能为 null");
        Objects.requireNonNull(valueMapper, "valueMapper 不能为 null");
        Objects.requireNonNull(mergeFunction, "mergeFunction 不能为 null");

        Map<RK, RV> result = new LinkedHashMap<>();
        if (map == null || map.isEmpty()) {
            return result;
        }

        for (Map.Entry<? extends K, ? extends V> entry : map.entrySet()) {
            RK key = keyMapper.apply(entry.getKey());
            RV value = valueMapper.apply(entry.getValue());
            result.merge(key, value, mergeFunction);
        }
        return result;
    }

    /**
     * 将 Map 的 Entry 映射为 ArrayList。
     *
     * @param map    Map 对象
     * @param mapper Entry 映射函数
     * @param <K>    Key 类型
     * @param <V>    Value 类型
     * @param <R>    目标元素类型
     * @return 映射后的可变 ArrayList
     */
    public static <K, V, R> List<R> mapEntryToList(Map<? extends K, ? extends V> map,
                                                   BiFunction<? super K, ? super V, ? extends R> mapper) {
        Objects.requireNonNull(mapper, "mapper 不能为 null");

        List<R> result = new ArrayList<>();
        if (map == null || map.isEmpty()) {
            return result;
        }

        for (Map.Entry<? extends K, ? extends V> entry : map.entrySet()) {
            result.add(mapper.apply(entry.getKey(), entry.getValue()));
        }
        return result;
    }

    /**
     * 将 Map 的 Entry 映射为 LinkedHashSet。
     *
     * @param map    Map 对象
     * @param mapper Entry 映射函数
     * @param <K>    Key 类型
     * @param <V>    Value 类型
     * @param <R>    目标元素类型
     * @return 映射后的可变 LinkedHashSet
     */
    public static <K, V, R> Set<R> mapEntryToSet(Map<? extends K, ? extends V> map,
                                                 BiFunction<? super K, ? super V, ? extends R> mapper) {
        Objects.requireNonNull(mapper, "mapper 不能为 null");

        Set<R> result = new LinkedHashSet<>();
        if (map == null || map.isEmpty()) {
            return result;
        }

        for (Map.Entry<? extends K, ? extends V> entry : map.entrySet()) {
            result.add(mapper.apply(entry.getKey(), entry.getValue()));
        }
        return result;
    }

    /**
     * 对 Collection 去重，并返回 ArrayList。
     *
     * <p>
     * 保留元素首次出现顺序。
     * </p>
     *
     * @param collection Collection 对象
     * @param <T>        元素类型
     * @return 去重后的可变 ArrayList
     */
    public static <T> List<T> distinct(Collection<? extends T> collection) {
        return distinctToList(collection);
    }

    /**
     * 对 Iterable 去重，并返回 ArrayList。
     *
     * <p>
     * 保留元素首次出现顺序。
     * </p>
     *
     * @param iterable Iterable 对象
     * @param <T>      元素类型
     * @return 去重后的可变 ArrayList
     */
    public static <T> List<T> distinct(Iterable<? extends T> iterable) {
        return distinctToList(iterable);
    }

    /**
     * 对 Iterator 去重，并返回 ArrayList。
     *
     * <p>
     * 该方法会消费 Iterator，并保留元素首次出现顺序。
     * </p>
     *
     * @param iterator Iterator 对象
     * @param <T>      元素类型
     * @return 去重后的可变 ArrayList
     */
    public static <T> List<T> distinct(Iterator<? extends T> iterator) {
        return distinctToList(iterator);
    }

    /**
     * 对数组去重，并返回 ArrayList。
     *
     * <p>
     * 保留元素首次出现顺序。
     * </p>
     *
     * @param array 数组
     * @param <T>   元素类型
     * @return 去重后的可变 ArrayList
     */
    public static <T> List<T> distinct(T[] array) {
        return distinctToList(array);
    }

    /**
     * 对 Collection 去重，并返回 ArrayList。
     *
     * <p>
     * 保留元素首次出现顺序。
     * </p>
     *
     * @param collection Collection 对象
     * @param <T>        元素类型
     * @return 去重后的可变 ArrayList
     */
    public static <T> List<T> distinctToList(Collection<? extends T> collection) {
        if (collection == null || collection.isEmpty()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(new LinkedHashSet<>(collection));
    }

    /**
     * 对 Iterable 去重，并返回 ArrayList。
     *
     * <p>
     * 保留元素首次出现顺序。
     * </p>
     *
     * @param iterable Iterable 对象
     * @param <T>      元素类型
     * @return 去重后的可变 ArrayList
     */
    public static <T> List<T> distinctToList(Iterable<? extends T> iterable) {
        if (iterable == null) {
            return new ArrayList<>();
        }
        if (iterable instanceof Collection<? extends T> collection) {
            return distinctToList(collection);
        }

        Set<T> exists = new LinkedHashSet<>();
        for (T item : iterable) {
            exists.add(item);
        }
        return new ArrayList<>(exists);
    }

    /**
     * 对 Iterator 去重，并返回 ArrayList。
     *
     * <p>
     * 该方法会消费 Iterator，并保留元素首次出现顺序。
     * </p>
     *
     * @param iterator Iterator 对象
     * @param <T>      元素类型
     * @return 去重后的可变 ArrayList
     */
    public static <T> List<T> distinctToList(Iterator<? extends T> iterator) {
        if (iterator == null) {
            return new ArrayList<>();
        }

        Set<T> exists = new LinkedHashSet<>();
        while (iterator.hasNext()) {
            exists.add(iterator.next());
        }
        return new ArrayList<>(exists);
    }

    /**
     * 对数组去重，并返回 ArrayList。
     *
     * <p>
     * 保留元素首次出现顺序。
     * </p>
     *
     * @param array 数组
     * @param <T>   元素类型
     * @return 去重后的可变 ArrayList
     */
    public static <T> List<T> distinctToList(T[] array) {
        if (array == null || array.length == 0) {
            return new ArrayList<>();
        }

        Set<T> exists = new LinkedHashSet<>(calculateHashMapCapacity(array.length));
        Collections.addAll(exists, array);
        return new ArrayList<>(exists);
    }

    /**
     * 对 Collection 去重，并返回 LinkedHashSet。
     *
     * <p>
     * 保留元素首次出现顺序。
     * </p>
     *
     * @param collection Collection 对象
     * @param <T>        元素类型
     * @return 去重后的可变 LinkedHashSet
     */
    public static <T> Set<T> distinctToSet(Collection<? extends T> collection) {
        return collection == null || collection.isEmpty() ? new LinkedHashSet<>() : new LinkedHashSet<>(collection);
    }

    /**
     * 对 Iterable 去重，并返回 LinkedHashSet。
     *
     * <p>
     * 保留元素首次出现顺序。
     * </p>
     *
     * @param iterable Iterable 对象
     * @param <T>      元素类型
     * @return 去重后的可变 LinkedHashSet
     */
    public static <T> Set<T> distinctToSet(Iterable<? extends T> iterable) {
        if (iterable == null) {
            return new LinkedHashSet<>();
        }
        if (iterable instanceof Collection<? extends T> collection) {
            return distinctToSet(collection);
        }

        Set<T> result = new LinkedHashSet<>();
        for (T item : iterable) {
            result.add(item);
        }
        return result;
    }

    /**
     * 对 Iterator 去重，并返回 LinkedHashSet。
     *
     * <p>
     * 该方法会消费 Iterator，并保留元素首次出现顺序。
     * </p>
     *
     * @param iterator Iterator 对象
     * @param <T>      元素类型
     * @return 去重后的可变 LinkedHashSet
     */
    public static <T> Set<T> distinctToSet(Iterator<? extends T> iterator) {
        Set<T> result = new LinkedHashSet<>();
        if (iterator == null) {
            return result;
        }

        while (iterator.hasNext()) {
            result.add(iterator.next());
        }
        return result;
    }

    /**
     * 对数组去重，并返回 LinkedHashSet。
     *
     * <p>
     * 保留元素首次出现顺序。
     * </p>
     *
     * @param array 数组
     * @param <T>   元素类型
     * @return 去重后的可变 LinkedHashSet
     */
    public static <T> Set<T> distinctToSet(T[] array) {
        if (array == null || array.length == 0) {
            return new LinkedHashSet<>();
        }

        Set<T> result = new LinkedHashSet<>(calculateHashMapCapacity(array.length));
        Collections.addAll(result, array);
        return result;
    }

    /**
     * 根据 Key 对 Collection 去重，并返回 ArrayList。
     *
     * <p>
     * 保留每个 Key 首次出现的元素。
     * </p>
     *
     * @param collection Collection 对象
     * @param keyMapper  Key 映射函数
     * @param <T>        元素类型
     * @param <K>        Key 类型
     * @return 去重后的可变 ArrayList
     */
    public static <T, K> List<T> distinctBy(Collection<? extends T> collection, Function<? super T, ? extends K> keyMapper) {
        return distinctByToList(collection, keyMapper);
    }

    /**
     * 根据 Key 对 Iterable 去重，并返回 ArrayList。
     *
     * <p>
     * 保留每个 Key 首次出现的元素。
     * </p>
     *
     * @param iterable  Iterable 对象
     * @param keyMapper Key 映射函数
     * @param <T>       元素类型
     * @param <K>       Key 类型
     * @return 去重后的可变 ArrayList
     */
    public static <T, K> List<T> distinctBy(Iterable<? extends T> iterable, Function<? super T, ? extends K> keyMapper) {
        return distinctByToList(iterable, keyMapper);
    }

    /**
     * 根据 Key 对 Iterator 去重，并返回 ArrayList。
     *
     * <p>
     * 该方法会消费 Iterator，并保留每个 Key 首次出现的元素。
     * </p>
     *
     * @param iterator  Iterator 对象
     * @param keyMapper Key 映射函数
     * @param <T>       元素类型
     * @param <K>       Key 类型
     * @return 去重后的可变 ArrayList
     */
    public static <T, K> List<T> distinctBy(Iterator<? extends T> iterator, Function<? super T, ? extends K> keyMapper) {
        return distinctByToList(iterator, keyMapper);
    }

    /**
     * 根据 Key 对数组去重，并返回 ArrayList。
     *
     * <p>
     * 保留每个 Key 首次出现的元素。
     * </p>
     *
     * @param array     数组
     * @param keyMapper Key 映射函数
     * @param <T>       元素类型
     * @param <K>       Key 类型
     * @return 去重后的可变 ArrayList
     */
    public static <T, K> List<T> distinctBy(T[] array, Function<? super T, ? extends K> keyMapper) {
        return distinctByToList(array, keyMapper);
    }

    /**
     * 根据 Key 对 Collection 去重，并返回 ArrayList。
     *
     * <p>
     * 保留每个 Key 首次出现的元素。
     * </p>
     *
     * @param collection Collection 对象
     * @param keyMapper  Key 映射函数
     * @param <T>        元素类型
     * @param <K>        Key 类型
     * @return 去重后的可变 ArrayList
     */
    public static <T, K> List<T> distinctByToList(Collection<? extends T> collection, Function<? super T, ? extends K> keyMapper) {
        Objects.requireNonNull(keyMapper, "keyMapper 不能为 null");

        List<T> result = new ArrayList<>();
        if (collection == null || collection.isEmpty()) {
            return result;
        }

        Set<K> exists = new LinkedHashSet<>();
        for (T item : collection) {
            K key = keyMapper.apply(item);
            if (exists.add(key)) {
                result.add(item);
            }
        }
        return result;
    }

    /**
     * 根据 Key 对 Iterable 去重，并返回 ArrayList。
     *
     * <p>
     * 保留每个 Key 首次出现的元素。
     * </p>
     *
     * @param iterable  Iterable 对象
     * @param keyMapper Key 映射函数
     * @param <T>       元素类型
     * @param <K>       Key 类型
     * @return 去重后的可变 ArrayList
     */
    public static <T, K> List<T> distinctByToList(Iterable<? extends T> iterable, Function<? super T, ? extends K> keyMapper) {
        Objects.requireNonNull(keyMapper, "keyMapper 不能为 null");

        List<T> result = new ArrayList<>();
        if (iterable == null) {
            return result;
        }

        Set<K> exists = new LinkedHashSet<>();
        for (T item : iterable) {
            K key = keyMapper.apply(item);
            if (exists.add(key)) {
                result.add(item);
            }
        }
        return result;
    }

    /**
     * 根据 Key 对 Iterator 去重，并返回 ArrayList。
     *
     * <p>
     * 该方法会消费 Iterator，并保留每个 Key 首次出现的元素。
     * </p>
     *
     * @param iterator  Iterator 对象
     * @param keyMapper Key 映射函数
     * @param <T>       元素类型
     * @param <K>       Key 类型
     * @return 去重后的可变 ArrayList
     */
    public static <T, K> List<T> distinctByToList(Iterator<? extends T> iterator, Function<? super T, ? extends K> keyMapper) {
        Objects.requireNonNull(keyMapper, "keyMapper 不能为 null");

        List<T> result = new ArrayList<>();
        if (iterator == null) {
            return result;
        }

        Set<K> exists = new LinkedHashSet<>();
        while (iterator.hasNext()) {
            T item = iterator.next();
            K key = keyMapper.apply(item);
            if (exists.add(key)) {
                result.add(item);
            }
        }
        return result;
    }

    /**
     * 根据 Key 对数组去重，并返回 ArrayList。
     *
     * <p>
     * 保留每个 Key 首次出现的元素。
     * </p>
     *
     * @param array     数组
     * @param keyMapper Key 映射函数
     * @param <T>       元素类型
     * @param <K>       Key 类型
     * @return 去重后的可变 ArrayList
     */
    public static <T, K> List<T> distinctByToList(T[] array, Function<? super T, ? extends K> keyMapper) {
        Objects.requireNonNull(keyMapper, "keyMapper 不能为 null");

        List<T> result = new ArrayList<>();
        if (array == null || array.length == 0) {
            return result;
        }

        Set<K> exists = new LinkedHashSet<>(calculateHashMapCapacity(array.length));
        for (T item : array) {
            K key = keyMapper.apply(item);
            if (exists.add(key)) {
                result.add(item);
            }
        }
        return result;
    }

    /**
     * 根据 Key 对 Collection 去重，并返回 LinkedHashSet。
     *
     * <p>
     * 保留每个 Key 首次出现的元素。
     * </p>
     *
     * @param collection Collection 对象
     * @param keyMapper  Key 映射函数
     * @param <T>        元素类型
     * @param <K>        Key 类型
     * @return 去重后的可变 LinkedHashSet
     */
    public static <T, K> Set<T> distinctByToSet(Collection<? extends T> collection, Function<? super T, ? extends K> keyMapper) {
        Objects.requireNonNull(keyMapper, "keyMapper 不能为 null");

        Set<T> result = new LinkedHashSet<>();
        if (collection == null || collection.isEmpty()) {
            return result;
        }

        Set<K> exists = new LinkedHashSet<>();
        for (T item : collection) {
            K key = keyMapper.apply(item);
            if (exists.add(key)) {
                result.add(item);
            }
        }
        return result;
    }

    /**
     * 根据 Key 对 Iterable 去重，并返回 LinkedHashSet。
     *
     * <p>
     * 保留每个 Key 首次出现的元素。
     * </p>
     *
     * @param iterable  Iterable 对象
     * @param keyMapper Key 映射函数
     * @param <T>       元素类型
     * @param <K>       Key 类型
     * @return 去重后的可变 LinkedHashSet
     */
    public static <T, K> Set<T> distinctByToSet(Iterable<? extends T> iterable, Function<? super T, ? extends K> keyMapper) {
        Objects.requireNonNull(keyMapper, "keyMapper 不能为 null");

        Set<T> result = new LinkedHashSet<>();
        if (iterable == null) {
            return result;
        }

        Set<K> exists = new LinkedHashSet<>();
        for (T item : iterable) {
            K key = keyMapper.apply(item);
            if (exists.add(key)) {
                result.add(item);
            }
        }
        return result;
    }

    /**
     * 根据 Key 对数组去重，并返回 LinkedHashSet。
     *
     * <p>
     * 保留每个 Key 首次出现的元素。
     * </p>
     *
     * @param array     数组
     * @param keyMapper Key 映射函数
     * @param <T>       元素类型
     * @param <K>       Key 类型
     * @return 去重后的可变 LinkedHashSet
     */
    public static <T, K> Set<T> distinctByToSet(T[] array, Function<? super T, ? extends K> keyMapper) {
        Objects.requireNonNull(keyMapper, "keyMapper 不能为 null");

        Set<T> result = new LinkedHashSet<>();
        if (array == null || array.length == 0) {
            return result;
        }

        Set<K> exists = new LinkedHashSet<>(calculateHashMapCapacity(array.length));
        for (T item : array) {
            K key = keyMapper.apply(item);
            if (exists.add(key)) {
                result.add(item);
            }
        }
        return result;
    }

    /**
     * 根据 Key 对 Collection 去重，并保留每个 Key 最后一次出现的元素。
     *
     * <p>
     * 返回顺序按 Key 首次出现顺序排列，元素值为该 Key 最后一次对应的元素。
     * </p>
     *
     * @param collection Collection 对象
     * @param keyMapper  Key 映射函数
     * @param <T>        元素类型
     * @param <K>        Key 类型
     * @return 去重后的可变 ArrayList
     */
    public static <T, K> List<T> distinctByKeepLast(Collection<? extends T> collection, Function<? super T, ? extends K> keyMapper) {
        Objects.requireNonNull(keyMapper, "keyMapper 不能为 null");

        if (collection == null || collection.isEmpty()) {
            return new ArrayList<>();
        }

        Map<K, T> map = new LinkedHashMap<>();
        for (T item : collection) {
            map.put(keyMapper.apply(item), item);
        }
        return new ArrayList<>(map.values());
    }

    /**
     * 根据 Key 对 Iterable 去重，并保留每个 Key 最后一次出现的元素。
     *
     * <p>
     * 返回顺序按 Key 首次出现顺序排列，元素值为该 Key 最后一次对应的元素。
     * </p>
     *
     * @param iterable  Iterable 对象
     * @param keyMapper Key 映射函数
     * @param <T>       元素类型
     * @param <K>       Key 类型
     * @return 去重后的可变 ArrayList
     */
    public static <T, K> List<T> distinctByKeepLast(Iterable<? extends T> iterable, Function<? super T, ? extends K> keyMapper) {
        Objects.requireNonNull(keyMapper, "keyMapper 不能为 null");

        if (iterable == null) {
            return new ArrayList<>();
        }

        Map<K, T> map = new LinkedHashMap<>();
        for (T item : iterable) {
            map.put(keyMapper.apply(item), item);
        }
        return new ArrayList<>(map.values());
    }

    /**
     * 根据 Key 对数组去重，并保留每个 Key 最后一次出现的元素。
     *
     * <p>
     * 返回顺序按 Key 首次出现顺序排列，元素值为该 Key 最后一次对应的元素。
     * </p>
     *
     * @param array     数组
     * @param keyMapper Key 映射函数
     * @param <T>       元素类型
     * @param <K>       Key 类型
     * @return 去重后的可变 ArrayList
     */
    public static <T, K> List<T> distinctByKeepLast(T[] array, Function<? super T, ? extends K> keyMapper) {
        Objects.requireNonNull(keyMapper, "keyMapper 不能为 null");

        if (array == null || array.length == 0) {
            return new ArrayList<>();
        }

        Map<K, T> map = new LinkedHashMap<>(calculateHashMapCapacity(array.length));
        for (T item : array) {
            map.put(keyMapper.apply(item), item);
        }
        return new ArrayList<>(map.values());
    }

    /**
     * 对 Collection 去重，并过滤 null 元素。
     *
     * <p>
     * 保留非 null 元素首次出现顺序。
     * </p>
     *
     * @param collection Collection 对象
     * @param <T>        元素类型
     * @return 去重后的可变 ArrayList，不包含 null
     */
    public static <T> List<T> distinctNotNull(Collection<? extends T> collection) {
        List<T> result = new ArrayList<>();
        if (collection == null || collection.isEmpty()) {
            return result;
        }

        Set<T> exists = new LinkedHashSet<>();
        for (T item : collection) {
            if (item != null && exists.add(item)) {
                result.add(item);
            }
        }
        return result;
    }

    /**
     * 对 Iterable 去重，并过滤 null 元素。
     *
     * <p>
     * 保留非 null 元素首次出现顺序。
     * </p>
     *
     * @param iterable Iterable 对象
     * @param <T>      元素类型
     * @return 去重后的可变 ArrayList，不包含 null
     */
    public static <T> List<T> distinctNotNull(Iterable<? extends T> iterable) {
        List<T> result = new ArrayList<>();
        if (iterable == null) {
            return result;
        }

        Set<T> exists = new LinkedHashSet<>();
        for (T item : iterable) {
            if (item != null && exists.add(item)) {
                result.add(item);
            }
        }
        return result;
    }

    /**
     * 对数组去重，并过滤 null 元素。
     *
     * <p>
     * 保留非 null 元素首次出现顺序。
     * </p>
     *
     * @param array 数组
     * @param <T>   元素类型
     * @return 去重后的可变 ArrayList，不包含 null
     */
    public static <T> List<T> distinctNotNull(T[] array) {
        List<T> result = new ArrayList<>();
        if (array == null || array.length == 0) {
            return result;
        }

        Set<T> exists = new LinkedHashSet<>(calculateHashMapCapacity(array.length));
        for (T item : array) {
            if (item != null && exists.add(item)) {
                result.add(item);
            }
        }
        return result;
    }

    /**
     * 根据非 null Key 对 Collection 去重。
     *
     * <p>
     * 如果映射出的 Key 为 null，则该元素会被忽略；保留每个非 null Key 首次出现的元素。
     * </p>
     *
     * @param collection Collection 对象
     * @param keyMapper  Key 映射函数
     * @param <T>        元素类型
     * @param <K>        Key 类型
     * @return 去重后的可变 ArrayList
     */
    public static <T, K> List<T> distinctByNotNullKey(Collection<? extends T> collection, Function<? super T, ? extends K> keyMapper) {
        Objects.requireNonNull(keyMapper, "keyMapper 不能为 null");

        List<T> result = new ArrayList<>();
        if (collection == null || collection.isEmpty()) {
            return result;
        }

        Set<K> exists = new LinkedHashSet<>();
        for (T item : collection) {
            K key = keyMapper.apply(item);
            if (key != null && exists.add(key)) {
                result.add(item);
            }
        }
        return result;
    }

    /**
     * 根据非 null Key 对 Iterable 去重。
     *
     * <p>
     * 如果映射出的 Key 为 null，则该元素会被忽略；保留每个非 null Key 首次出现的元素。
     * </p>
     *
     * @param iterable  Iterable 对象
     * @param keyMapper Key 映射函数
     * @param <T>       元素类型
     * @param <K>       Key 类型
     * @return 去重后的可变 ArrayList
     */
    public static <T, K> List<T> distinctByNotNullKey(Iterable<? extends T> iterable, Function<? super T, ? extends K> keyMapper) {
        Objects.requireNonNull(keyMapper, "keyMapper 不能为 null");

        List<T> result = new ArrayList<>();
        if (iterable == null) {
            return result;
        }

        Set<K> exists = new LinkedHashSet<>();
        for (T item : iterable) {
            K key = keyMapper.apply(item);
            if (key != null && exists.add(key)) {
                result.add(item);
            }
        }
        return result;
    }

    /**
     * 根据非 null Key 对数组去重。
     *
     * <p>
     * 如果映射出的 Key 为 null，则该元素会被忽略；保留每个非 null Key 首次出现的元素。
     * </p>
     *
     * @param array     数组
     * @param keyMapper Key 映射函数
     * @param <T>       元素类型
     * @param <K>       Key 类型
     * @return 去重后的可变 ArrayList
     */
    public static <T, K> List<T> distinctByNotNullKey(T[] array, Function<? super T, ? extends K> keyMapper) {
        Objects.requireNonNull(keyMapper, "keyMapper 不能为 null");

        List<T> result = new ArrayList<>();
        if (array == null || array.length == 0) {
            return result;
        }

        Set<K> exists = new LinkedHashSet<>(calculateHashMapCapacity(array.length));
        for (T item : array) {
            K key = keyMapper.apply(item);
            if (key != null && exists.add(key)) {
                result.add(item);
            }
        }
        return result;
    }

    /**
     * 根据自定义相等判断对 Collection 去重。
     *
     * <p>
     * 该方法使用双重遍历，适合小集合或无法提取稳定 Key 的场景。
     * </p>
     *
     * @param collection      Collection 对象
     * @param equalsPredicate 相等判断函数，第一个参数为已保留元素，第二个参数为当前元素
     * @param <T>             元素类型
     * @return 去重后的可变 ArrayList
     */
    public static <T> List<T> distinctByPredicate(Collection<? extends T> collection,
                                                  BiPredicate<? super T, ? super T> equalsPredicate) {
        Objects.requireNonNull(equalsPredicate, "equalsPredicate 不能为 null");

        List<T> result = new ArrayList<>();
        if (collection == null || collection.isEmpty()) {
            return result;
        }

        for (T item : collection) {
            boolean exists = false;
            for (T saved : result) {
                if (equalsPredicate.test(saved, item)) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                result.add(item);
            }
        }
        return result;
    }

    /**
     * 根据自定义相等判断对 Iterable 去重。
     *
     * <p>
     * 该方法使用双重遍历，适合小集合或无法提取稳定 Key 的场景。
     * </p>
     *
     * @param iterable        Iterable 对象
     * @param equalsPredicate 相等判断函数，第一个参数为已保留元素，第二个参数为当前元素
     * @param <T>             元素类型
     * @return 去重后的可变 ArrayList
     */
    public static <T> List<T> distinctByPredicate(Iterable<? extends T> iterable,
                                                  BiPredicate<? super T, ? super T> equalsPredicate) {
        Objects.requireNonNull(equalsPredicate, "equalsPredicate 不能为 null");

        List<T> result = new ArrayList<>();
        if (iterable == null) {
            return result;
        }

        for (T item : iterable) {
            boolean exists = false;
            for (T saved : result) {
                if (equalsPredicate.test(saved, item)) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                result.add(item);
            }
        }
        return result;
    }

    /**
     * 根据自定义相等判断对数组去重。
     *
     * <p>
     * 该方法使用双重遍历，适合小数组或无法提取稳定 Key 的场景。
     * </p>
     *
     * @param array           数组
     * @param equalsPredicate 相等判断函数，第一个参数为已保留元素，第二个参数为当前元素
     * @param <T>             元素类型
     * @return 去重后的可变 ArrayList
     */
    public static <T> List<T> distinctByPredicate(T[] array,
                                                  BiPredicate<? super T, ? super T> equalsPredicate) {
        Objects.requireNonNull(equalsPredicate, "equalsPredicate 不能为 null");

        List<T> result = new ArrayList<>();
        if (array == null || array.length == 0) {
            return result;
        }

        for (T item : array) {
            boolean exists = false;
            for (T saved : result) {
                if (equalsPredicate.test(saved, item)) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                result.add(item);
            }
        }
        return result;
    }

    /**
     * 判断 Collection 是否存在重复元素。
     *
     * @param collection Collection 对象
     * @return true 表示存在重复元素
     */
    public static boolean hasDuplicate(Collection<?> collection) {
        if (collection == null || collection.size() <= 1) {
            return false;
        }
        return new HashSet<>(collection).size() != collection.size();
    }

    /**
     * 判断 Iterable 是否存在重复元素。
     *
     * @param iterable Iterable 对象
     * @return true 表示存在重复元素
     */
    public static boolean hasDuplicate(Iterable<?> iterable) {
        if (iterable == null) {
            return false;
        }
        if (iterable instanceof Collection<?> collection) {
            return hasDuplicate(collection);
        }

        Set<Object> exists = new HashSet<>();
        for (Object item : iterable) {
            if (!exists.add(item)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断数组是否存在重复元素。
     *
     * @param array 数组
     * @return true 表示存在重复元素
     */
    public static boolean hasDuplicate(Object[] array) {
        if (array == null || array.length <= 1) {
            return false;
        }

        Set<Object> exists = new HashSet<>(calculateHashMapCapacity(array.length));
        for (Object item : array) {
            if (!exists.add(item)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断 Collection 是否存在重复 Key。
     *
     * @param collection Collection 对象
     * @param keyMapper  Key 映射函数
     * @param <T>        元素类型
     * @param <K>        Key 类型
     * @return true 表示存在重复 Key
     */
    public static <T, K> boolean hasDuplicateBy(Collection<? extends T> collection, Function<? super T, ? extends K> keyMapper) {
        Objects.requireNonNull(keyMapper, "keyMapper 不能为 null");

        if (collection == null || collection.size() <= 1) {
            return false;
        }

        Set<K> exists = new HashSet<>(calculateHashMapCapacity(collection.size()));
        for (T item : collection) {
            if (!exists.add(keyMapper.apply(item))) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断 Iterable 是否存在重复 Key。
     *
     * @param iterable  Iterable 对象
     * @param keyMapper Key 映射函数
     * @param <T>       元素类型
     * @param <K>       Key 类型
     * @return true 表示存在重复 Key
     */
    public static <T, K> boolean hasDuplicateBy(Iterable<? extends T> iterable, Function<? super T, ? extends K> keyMapper) {
        Objects.requireNonNull(keyMapper, "keyMapper 不能为 null");

        if (iterable == null) {
            return false;
        }
        if (iterable instanceof Collection<? extends T> collection) {
            return hasDuplicateBy(collection, keyMapper);
        }

        Set<K> exists = new HashSet<>();
        for (T item : iterable) {
            if (!exists.add(keyMapper.apply(item))) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断数组是否存在重复 Key。
     *
     * @param array     数组
     * @param keyMapper Key 映射函数
     * @param <T>       元素类型
     * @param <K>       Key 类型
     * @return true 表示存在重复 Key
     */
    public static <T, K> boolean hasDuplicateBy(T[] array, Function<? super T, ? extends K> keyMapper) {
        Objects.requireNonNull(keyMapper, "keyMapper 不能为 null");

        if (array == null || array.length <= 1) {
            return false;
        }

        Set<K> exists = new HashSet<>(calculateHashMapCapacity(array.length));
        for (T item : array) {
            if (!exists.add(keyMapper.apply(item))) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取 Collection 中重复出现的元素。
     *
     * <p>
     * 返回每个重复元素本身，且每个重复元素只返回一次；返回顺序为重复元素第二次出现的顺序。
     * </p>
     *
     * @param collection Collection 对象
     * @param <T>        元素类型
     * @return 重复元素列表
     */
    public static <T> List<T> duplicateElements(Collection<? extends T> collection) {
        List<T> result = new ArrayList<>();
        if (collection == null || collection.size() <= 1) {
            return result;
        }

        Set<T> exists = new HashSet<>(calculateHashMapCapacity(collection.size()));
        Set<T> duplicates = new LinkedHashSet<>();
        for (T item : collection) {
            if (!exists.add(item)) {
                duplicates.add(item);
            }
        }
        result.addAll(duplicates);
        return result;
    }

    /**
     * 获取 Iterable 中重复出现的元素。
     *
     * <p>
     * 返回每个重复元素本身，且每个重复元素只返回一次；返回顺序为重复元素第二次出现的顺序。
     * </p>
     *
     * @param iterable Iterable 对象
     * @param <T>      元素类型
     * @return 重复元素列表
     */
    public static <T> List<T> duplicateElements(Iterable<? extends T> iterable) {
        List<T> result = new ArrayList<>();
        if (iterable == null) {
            return result;
        }
        if (iterable instanceof Collection<? extends T> collection) {
            return duplicateElements(collection);
        }

        Set<T> exists = new HashSet<>();
        Set<T> duplicates = new LinkedHashSet<>();
        for (T item : iterable) {
            if (!exists.add(item)) {
                duplicates.add(item);
            }
        }
        result.addAll(duplicates);
        return result;
    }

    /**
     * 获取数组中重复出现的元素。
     *
     * <p>
     * 返回每个重复元素本身，且每个重复元素只返回一次；返回顺序为重复元素第二次出现的顺序。
     * </p>
     *
     * @param array 数组
     * @param <T>   元素类型
     * @return 重复元素列表
     */
    public static <T> List<T> duplicateElements(T[] array) {
        List<T> result = new ArrayList<>();
        if (array == null || array.length <= 1) {
            return result;
        }

        Set<T> exists = new HashSet<>(calculateHashMapCapacity(array.length));
        Set<T> duplicates = new LinkedHashSet<>();
        for (T item : array) {
            if (!exists.add(item)) {
                duplicates.add(item);
            }
        }
        result.addAll(duplicates);
        return result;
    }

    /**
     * 获取 Collection 中重复出现的 Key。
     *
     * <p>
     * 返回每个重复 Key，且每个重复 Key 只返回一次；返回顺序为重复 Key 第二次出现的顺序。
     * </p>
     *
     * @param collection Collection 对象
     * @param keyMapper  Key 映射函数
     * @param <T>        元素类型
     * @param <K>        Key 类型
     * @return 重复 Key 列表
     */
    public static <T, K> List<K> duplicateKeys(Collection<? extends T> collection, Function<? super T, ? extends K> keyMapper) {
        Objects.requireNonNull(keyMapper, "keyMapper 不能为 null");

        List<K> result = new ArrayList<>();
        if (collection == null || collection.size() <= 1) {
            return result;
        }

        Set<K> exists = new HashSet<>(calculateHashMapCapacity(collection.size()));
        Set<K> duplicates = new LinkedHashSet<>();
        for (T item : collection) {
            K key = keyMapper.apply(item);
            if (!exists.add(key)) {
                duplicates.add(key);
            }
        }
        result.addAll(duplicates);
        return result;
    }

    /**
     * 获取 Iterable 中重复出现的 Key。
     *
     * <p>
     * 返回每个重复 Key，且每个重复 Key 只返回一次；返回顺序为重复 Key 第二次出现的顺序。
     * </p>
     *
     * @param iterable  Iterable 对象
     * @param keyMapper Key 映射函数
     * @param <T>       元素类型
     * @param <K>       Key 类型
     * @return 重复 Key 列表
     */
    public static <T, K> List<K> duplicateKeys(Iterable<? extends T> iterable, Function<? super T, ? extends K> keyMapper) {
        Objects.requireNonNull(keyMapper, "keyMapper 不能为 null");

        List<K> result = new ArrayList<>();
        if (iterable == null) {
            return result;
        }
        if (iterable instanceof Collection<? extends T> collection) {
            return duplicateKeys(collection, keyMapper);
        }

        Set<K> exists = new HashSet<>();
        Set<K> duplicates = new LinkedHashSet<>();
        for (T item : iterable) {
            K key = keyMapper.apply(item);
            if (!exists.add(key)) {
                duplicates.add(key);
            }
        }
        result.addAll(duplicates);
        return result;
    }

    /**
     * 获取数组中重复出现的 Key。
     *
     * <p>
     * 返回每个重复 Key，且每个重复 Key 只返回一次；返回顺序为重复 Key 第二次出现的顺序。
     * </p>
     *
     * @param array     数组
     * @param keyMapper Key 映射函数
     * @param <T>       元素类型
     * @param <K>       Key 类型
     * @return 重复 Key 列表
     */
    public static <T, K> List<K> duplicateKeys(T[] array, Function<? super T, ? extends K> keyMapper) {
        Objects.requireNonNull(keyMapper, "keyMapper 不能为 null");

        List<K> result = new ArrayList<>();
        if (array == null || array.length <= 1) {
            return result;
        }

        Set<K> exists = new HashSet<>(calculateHashMapCapacity(array.length));
        Set<K> duplicates = new LinkedHashSet<>();
        for (T item : array) {
            K key = keyMapper.apply(item);
            if (!exists.add(key)) {
                duplicates.add(key);
            }
        }
        result.addAll(duplicates);
        return result;
    }

    /**
     * 原地去重 List。
     *
     * <p>
     * 保留元素首次出现顺序，并直接修改传入的 List。
     * </p>
     *
     * @param list 目标 List
     * @param <T>  元素类型
     * @return true 表示 List 发生变化
     */
    public static <T> boolean distinctInPlace(List<T> list) {
        if (list == null || list.size() <= 1) {
            return false;
        }

        List<T> distinctList = distinctToList(list);
        if (distinctList.size() == list.size()) {
            return false;
        }

        list.clear();
        list.addAll(distinctList);
        return true;
    }

    /**
     * 根据 Key 原地去重 List。
     *
     * <p>
     * 保留每个 Key 首次出现的元素，并直接修改传入的 List。
     * </p>
     *
     * @param list      目标 List
     * @param keyMapper Key 映射函数
     * @param <T>       元素类型
     * @param <K>       Key 类型
     * @return true 表示 List 发生变化
     */
    public static <T, K> boolean distinctByInPlace(List<T> list, Function<? super T, ? extends K> keyMapper) {
        Objects.requireNonNull(keyMapper, "keyMapper 不能为 null");

        if (list == null || list.size() <= 1) {
            return false;
        }

        List<T> distinctList = distinctByToList(list, keyMapper);
        if (distinctList.size() == list.size()) {
            return false;
        }

        list.clear();
        list.addAll(distinctList);
        return true;
    }

    /**
     * 按 Key 对 Collection 分组。
     *
     * <p>
     * 返回 LinkedHashMap，保留分组 Key 首次出现顺序；每组元素使用 ArrayList 保存。
     * </p>
     *
     * @param collection Collection 对象
     * @param keyMapper  Key 映射函数
     * @param <T>        元素类型
     * @param <K>        Key 类型
     * @return 分组后的可变 LinkedHashMap
     */
    public static <T, K> Map<K, List<T>> groupBy(Collection<? extends T> collection, Function<? super T, ? extends K> keyMapper) {
        return groupByToList(collection, keyMapper);
    }

    /**
     * 按 Key 对 Iterable 分组。
     *
     * <p>
     * 返回 LinkedHashMap，保留分组 Key 首次出现顺序；每组元素使用 ArrayList 保存。
     * </p>
     *
     * @param iterable  Iterable 对象
     * @param keyMapper Key 映射函数
     * @param <T>       元素类型
     * @param <K>       Key 类型
     * @return 分组后的可变 LinkedHashMap
     */
    public static <T, K> Map<K, List<T>> groupBy(Iterable<? extends T> iterable, Function<? super T, ? extends K> keyMapper) {
        return groupByToList(iterable, keyMapper);
    }

    /**
     * 按 Key 对 Iterator 分组。
     *
     * <p>
     * 该方法会消费 Iterator；返回 LinkedHashMap，保留分组 Key 首次出现顺序。
     * </p>
     *
     * @param iterator  Iterator 对象
     * @param keyMapper Key 映射函数
     * @param <T>       元素类型
     * @param <K>       Key 类型
     * @return 分组后的可变 LinkedHashMap
     */
    public static <T, K> Map<K, List<T>> groupBy(Iterator<? extends T> iterator, Function<? super T, ? extends K> keyMapper) {
        return groupByToList(iterator, keyMapper);
    }

    /**
     * 按 Key 对数组分组。
     *
     * <p>
     * 返回 LinkedHashMap，保留分组 Key 首次出现顺序；每组元素使用 ArrayList 保存。
     * </p>
     *
     * @param array     数组
     * @param keyMapper Key 映射函数
     * @param <T>       元素类型
     * @param <K>       Key 类型
     * @return 分组后的可变 LinkedHashMap
     */
    public static <T, K> Map<K, List<T>> groupBy(T[] array, Function<? super T, ? extends K> keyMapper) {
        return groupByToList(array, keyMapper);
    }

    /**
     * 按 Key 对 Collection 分组，并将每组元素保存为 ArrayList。
     *
     * @param collection Collection 对象
     * @param keyMapper  Key 映射函数
     * @param <T>        元素类型
     * @param <K>        Key 类型
     * @return 分组后的可变 LinkedHashMap
     */
    public static <T, K> Map<K, List<T>> groupByToList(Collection<? extends T> collection, Function<? super T, ? extends K> keyMapper) {
        Objects.requireNonNull(keyMapper, "keyMapper 不能为 null");

        Map<K, List<T>> result = new LinkedHashMap<>();
        if (collection == null || collection.isEmpty()) {
            return result;
        }

        for (T item : collection) {
            K key = keyMapper.apply(item);
            result.computeIfAbsent(key, ignored -> new ArrayList<>()).add(item);
        }
        return result;
    }

    /**
     * 按 Key 对 Iterable 分组，并将每组元素保存为 ArrayList。
     *
     * @param iterable  Iterable 对象
     * @param keyMapper Key 映射函数
     * @param <T>       元素类型
     * @param <K>       Key 类型
     * @return 分组后的可变 LinkedHashMap
     */
    public static <T, K> Map<K, List<T>> groupByToList(Iterable<? extends T> iterable, Function<? super T, ? extends K> keyMapper) {
        Objects.requireNonNull(keyMapper, "keyMapper 不能为 null");

        Map<K, List<T>> result = new LinkedHashMap<>();
        if (iterable == null) {
            return result;
        }

        for (T item : iterable) {
            K key = keyMapper.apply(item);
            result.computeIfAbsent(key, ignored -> new ArrayList<>()).add(item);
        }
        return result;
    }

    /**
     * 按 Key 对 Iterator 分组，并将每组元素保存为 ArrayList。
     *
     * <p>
     * 该方法会消费 Iterator。
     * </p>
     *
     * @param iterator  Iterator 对象
     * @param keyMapper Key 映射函数
     * @param <T>       元素类型
     * @param <K>       Key 类型
     * @return 分组后的可变 LinkedHashMap
     */
    public static <T, K> Map<K, List<T>> groupByToList(Iterator<? extends T> iterator, Function<? super T, ? extends K> keyMapper) {
        Objects.requireNonNull(keyMapper, "keyMapper 不能为 null");

        Map<K, List<T>> result = new LinkedHashMap<>();
        if (iterator == null) {
            return result;
        }

        while (iterator.hasNext()) {
            T item = iterator.next();
            K key = keyMapper.apply(item);
            result.computeIfAbsent(key, ignored -> new ArrayList<>()).add(item);
        }
        return result;
    }

    /**
     * 按 Key 对数组分组，并将每组元素保存为 ArrayList。
     *
     * @param array     数组
     * @param keyMapper Key 映射函数
     * @param <T>       元素类型
     * @param <K>       Key 类型
     * @return 分组后的可变 LinkedHashMap
     */
    public static <T, K> Map<K, List<T>> groupByToList(T[] array, Function<? super T, ? extends K> keyMapper) {
        Objects.requireNonNull(keyMapper, "keyMapper 不能为 null");

        Map<K, List<T>> result = new LinkedHashMap<>();
        if (array == null || array.length == 0) {
            return result;
        }

        for (T item : array) {
            K key = keyMapper.apply(item);
            result.computeIfAbsent(key, ignored -> new ArrayList<>()).add(item);
        }
        return result;
    }

    /**
     * 按 Key 对 Collection 分组，并将每组元素保存为 LinkedHashSet。
     *
     * <p>
     * 每组元素会按首次出现顺序去重。
     * </p>
     *
     * @param collection Collection 对象
     * @param keyMapper  Key 映射函数
     * @param <T>        元素类型
     * @param <K>        Key 类型
     * @return 分组后的可变 LinkedHashMap
     */
    public static <T, K> Map<K, Set<T>> groupByToSet(Collection<? extends T> collection, Function<? super T, ? extends K> keyMapper) {
        Objects.requireNonNull(keyMapper, "keyMapper 不能为 null");

        Map<K, Set<T>> result = new LinkedHashMap<>();
        if (collection == null || collection.isEmpty()) {
            return result;
        }

        for (T item : collection) {
            K key = keyMapper.apply(item);
            result.computeIfAbsent(key, ignored -> new LinkedHashSet<>()).add(item);
        }
        return result;
    }

    /**
     * 按 Key 对 Iterable 分组，并将每组元素保存为 LinkedHashSet。
     *
     * @param iterable  Iterable 对象
     * @param keyMapper Key 映射函数
     * @param <T>       元素类型
     * @param <K>       Key 类型
     * @return 分组后的可变 LinkedHashMap
     */
    public static <T, K> Map<K, Set<T>> groupByToSet(Iterable<? extends T> iterable, Function<? super T, ? extends K> keyMapper) {
        Objects.requireNonNull(keyMapper, "keyMapper 不能为 null");

        Map<K, Set<T>> result = new LinkedHashMap<>();
        if (iterable == null) {
            return result;
        }

        for (T item : iterable) {
            K key = keyMapper.apply(item);
            result.computeIfAbsent(key, ignored -> new LinkedHashSet<>()).add(item);
        }
        return result;
    }

    /**
     * 按 Key 对数组分组，并将每组元素保存为 LinkedHashSet。
     *
     * @param array     数组
     * @param keyMapper Key 映射函数
     * @param <T>       元素类型
     * @param <K>       Key 类型
     * @return 分组后的可变 LinkedHashMap
     */
    public static <T, K> Map<K, Set<T>> groupByToSet(T[] array, Function<? super T, ? extends K> keyMapper) {
        Objects.requireNonNull(keyMapper, "keyMapper 不能为 null");

        Map<K, Set<T>> result = new LinkedHashMap<>();
        if (array == null || array.length == 0) {
            return result;
        }

        for (T item : array) {
            K key = keyMapper.apply(item);
            result.computeIfAbsent(key, ignored -> new LinkedHashSet<>()).add(item);
        }
        return result;
    }

    /**
     * 按 Key 对 Collection 分组，并映射每组 Value。
     *
     * <p>
     * 每组 Value 使用 ArrayList 保存。
     * </p>
     *
     * @param collection  Collection 对象
     * @param keyMapper   Key 映射函数
     * @param valueMapper Value 映射函数
     * @param <T>         元素类型
     * @param <K>         Key 类型
     * @param <V>         Value 类型
     * @return 分组后的可变 LinkedHashMap
     */
    public static <T, K, V> Map<K, List<V>> groupMapping(Collection<? extends T> collection,
                                                         Function<? super T, ? extends K> keyMapper,
                                                         Function<? super T, ? extends V> valueMapper) {
        return groupMappingToList(collection, keyMapper, valueMapper);
    }

    /**
     * 按 Key 对 Iterable 分组，并映射每组 Value。
     *
     * <p>
     * 每组 Value 使用 ArrayList 保存。
     * </p>
     *
     * @param iterable    Iterable 对象
     * @param keyMapper   Key 映射函数
     * @param valueMapper Value 映射函数
     * @param <T>         元素类型
     * @param <K>         Key 类型
     * @param <V>         Value 类型
     * @return 分组后的可变 LinkedHashMap
     */
    public static <T, K, V> Map<K, List<V>> groupMapping(Iterable<? extends T> iterable,
                                                         Function<? super T, ? extends K> keyMapper,
                                                         Function<? super T, ? extends V> valueMapper) {
        return groupMappingToList(iterable, keyMapper, valueMapper);
    }

    /**
     * 按 Key 对数组分组，并映射每组 Value。
     *
     * <p>
     * 每组 Value 使用 ArrayList 保存。
     * </p>
     *
     * @param array       数组
     * @param keyMapper   Key 映射函数
     * @param valueMapper Value 映射函数
     * @param <T>         元素类型
     * @param <K>         Key 类型
     * @param <V>         Value 类型
     * @return 分组后的可变 LinkedHashMap
     */
    public static <T, K, V> Map<K, List<V>> groupMapping(T[] array,
                                                         Function<? super T, ? extends K> keyMapper,
                                                         Function<? super T, ? extends V> valueMapper) {
        return groupMappingToList(array, keyMapper, valueMapper);
    }

    /**
     * 按 Key 对 Collection 分组，并将映射后的 Value 保存为 ArrayList。
     *
     * @param collection  Collection 对象
     * @param keyMapper   Key 映射函数
     * @param valueMapper Value 映射函数
     * @param <T>         元素类型
     * @param <K>         Key 类型
     * @param <V>         Value 类型
     * @return 分组后的可变 LinkedHashMap
     */
    public static <T, K, V> Map<K, List<V>> groupMappingToList(Collection<? extends T> collection,
                                                               Function<? super T, ? extends K> keyMapper,
                                                               Function<? super T, ? extends V> valueMapper) {
        Objects.requireNonNull(keyMapper, "keyMapper 不能为 null");
        Objects.requireNonNull(valueMapper, "valueMapper 不能为 null");

        Map<K, List<V>> result = new LinkedHashMap<>();
        if (collection == null || collection.isEmpty()) {
            return result;
        }

        for (T item : collection) {
            K key = keyMapper.apply(item);
            V value = valueMapper.apply(item);
            result.computeIfAbsent(key, ignored -> new ArrayList<>()).add(value);
        }
        return result;
    }

    /**
     * 按 Key 对 Iterable 分组，并将映射后的 Value 保存为 ArrayList。
     *
     * @param iterable    Iterable 对象
     * @param keyMapper   Key 映射函数
     * @param valueMapper Value 映射函数
     * @param <T>         元素类型
     * @param <K>         Key 类型
     * @param <V>         Value 类型
     * @return 分组后的可变 LinkedHashMap
     */
    public static <T, K, V> Map<K, List<V>> groupMappingToList(Iterable<? extends T> iterable,
                                                               Function<? super T, ? extends K> keyMapper,
                                                               Function<? super T, ? extends V> valueMapper) {
        Objects.requireNonNull(keyMapper, "keyMapper 不能为 null");
        Objects.requireNonNull(valueMapper, "valueMapper 不能为 null");

        Map<K, List<V>> result = new LinkedHashMap<>();
        if (iterable == null) {
            return result;
        }

        for (T item : iterable) {
            K key = keyMapper.apply(item);
            V value = valueMapper.apply(item);
            result.computeIfAbsent(key, ignored -> new ArrayList<>()).add(value);
        }
        return result;
    }

    /**
     * 按 Key 对 Iterator 分组，并将映射后的 Value 保存为 ArrayList。
     *
     * <p>
     * 该方法会消费 Iterator。
     * </p>
     *
     * @param iterator    Iterator 对象
     * @param keyMapper   Key 映射函数
     * @param valueMapper Value 映射函数
     * @param <T>         元素类型
     * @param <K>         Key 类型
     * @param <V>         Value 类型
     * @return 分组后的可变 LinkedHashMap
     */
    public static <T, K, V> Map<K, List<V>> groupMappingToList(Iterator<? extends T> iterator,
                                                               Function<? super T, ? extends K> keyMapper,
                                                               Function<? super T, ? extends V> valueMapper) {
        Objects.requireNonNull(keyMapper, "keyMapper 不能为 null");
        Objects.requireNonNull(valueMapper, "valueMapper 不能为 null");

        Map<K, List<V>> result = new LinkedHashMap<>();
        if (iterator == null) {
            return result;
        }

        while (iterator.hasNext()) {
            T item = iterator.next();
            K key = keyMapper.apply(item);
            V value = valueMapper.apply(item);
            result.computeIfAbsent(key, ignored -> new ArrayList<>()).add(value);
        }
        return result;
    }

    /**
     * 按 Key 对数组分组，并将映射后的 Value 保存为 ArrayList。
     *
     * @param array       数组
     * @param keyMapper   Key 映射函数
     * @param valueMapper Value 映射函数
     * @param <T>         元素类型
     * @param <K>         Key 类型
     * @param <V>         Value 类型
     * @return 分组后的可变 LinkedHashMap
     */
    public static <T, K, V> Map<K, List<V>> groupMappingToList(T[] array,
                                                               Function<? super T, ? extends K> keyMapper,
                                                               Function<? super T, ? extends V> valueMapper) {
        Objects.requireNonNull(keyMapper, "keyMapper 不能为 null");
        Objects.requireNonNull(valueMapper, "valueMapper 不能为 null");

        Map<K, List<V>> result = new LinkedHashMap<>();
        if (array == null || array.length == 0) {
            return result;
        }

        for (T item : array) {
            K key = keyMapper.apply(item);
            V value = valueMapper.apply(item);
            result.computeIfAbsent(key, ignored -> new ArrayList<>()).add(value);
        }
        return result;
    }

    /**
     * 按 Key 对 Collection 分组，并将映射后的 Value 保存为 LinkedHashSet。
     *
     * <p>
     * 每组 Value 会按首次出现顺序去重。
     * </p>
     *
     * @param collection  Collection 对象
     * @param keyMapper   Key 映射函数
     * @param valueMapper Value 映射函数
     * @param <T>         元素类型
     * @param <K>         Key 类型
     * @param <V>         Value 类型
     * @return 分组后的可变 LinkedHashMap
     */
    public static <T, K, V> Map<K, Set<V>> groupMappingToSet(Collection<? extends T> collection,
                                                             Function<? super T, ? extends K> keyMapper,
                                                             Function<? super T, ? extends V> valueMapper) {
        Objects.requireNonNull(keyMapper, "keyMapper 不能为 null");
        Objects.requireNonNull(valueMapper, "valueMapper 不能为 null");

        Map<K, Set<V>> result = new LinkedHashMap<>();
        if (collection == null || collection.isEmpty()) {
            return result;
        }

        for (T item : collection) {
            K key = keyMapper.apply(item);
            V value = valueMapper.apply(item);
            result.computeIfAbsent(key, ignored -> new LinkedHashSet<>()).add(value);
        }
        return result;
    }

    /**
     * 按 Key 对 Iterable 分组，并将映射后的 Value 保存为 LinkedHashSet。
     *
     * @param iterable    Iterable 对象
     * @param keyMapper   Key 映射函数
     * @param valueMapper Value 映射函数
     * @param <T>         元素类型
     * @param <K>         Key 类型
     * @param <V>         Value 类型
     * @return 分组后的可变 LinkedHashMap
     */
    public static <T, K, V> Map<K, Set<V>> groupMappingToSet(Iterable<? extends T> iterable,
                                                             Function<? super T, ? extends K> keyMapper,
                                                             Function<? super T, ? extends V> valueMapper) {
        Objects.requireNonNull(keyMapper, "keyMapper 不能为 null");
        Objects.requireNonNull(valueMapper, "valueMapper 不能为 null");

        Map<K, Set<V>> result = new LinkedHashMap<>();
        if (iterable == null) {
            return result;
        }

        for (T item : iterable) {
            K key = keyMapper.apply(item);
            V value = valueMapper.apply(item);
            result.computeIfAbsent(key, ignored -> new LinkedHashSet<>()).add(value);
        }
        return result;
    }

    /**
     * 按 Key 对数组分组，并将映射后的 Value 保存为 LinkedHashSet。
     *
     * @param array       数组
     * @param keyMapper   Key 映射函数
     * @param valueMapper Value 映射函数
     * @param <T>         元素类型
     * @param <K>         Key 类型
     * @param <V>         Value 类型
     * @return 分组后的可变 LinkedHashMap
     */
    public static <T, K, V> Map<K, Set<V>> groupMappingToSet(T[] array,
                                                             Function<? super T, ? extends K> keyMapper,
                                                             Function<? super T, ? extends V> valueMapper) {
        Objects.requireNonNull(keyMapper, "keyMapper 不能为 null");
        Objects.requireNonNull(valueMapper, "valueMapper 不能为 null");

        Map<K, Set<V>> result = new LinkedHashMap<>();
        if (array == null || array.length == 0) {
            return result;
        }

        for (T item : array) {
            K key = keyMapper.apply(item);
            V value = valueMapper.apply(item);
            result.computeIfAbsent(key, ignored -> new LinkedHashSet<>()).add(value);
        }
        return result;
    }

    /**
     * 按 Key 对 Collection 分组，并使用指定集合保存每组元素。
     *
     * @param collection         Collection 对象
     * @param keyMapper          Key 映射函数
     * @param collectionSupplier 每组集合创建函数
     * @param <T>                元素类型
     * @param <K>                Key 类型
     * @param <C>                每组集合类型
     * @return 分组后的可变 LinkedHashMap
     */
    public static <T, K, C extends Collection<T>> Map<K, C> groupByTo(Collection<? extends T> collection,
                                                                      Function<? super T, ? extends K> keyMapper,
                                                                      Supplier<C> collectionSupplier) {
        Objects.requireNonNull(keyMapper, "keyMapper 不能为 null");
        Objects.requireNonNull(collectionSupplier, "collectionSupplier 不能为 null");

        Map<K, C> result = new LinkedHashMap<>();
        if (collection == null || collection.isEmpty()) {
            return result;
        }

        for (T item : collection) {
            K key = keyMapper.apply(item);
            result.computeIfAbsent(key, ignored -> collectionSupplier.get()).add(item);
        }
        return result;
    }

    /**
     * 按 Key 对 Iterable 分组，并使用指定集合保存每组元素。
     *
     * @param iterable           Iterable 对象
     * @param keyMapper          Key 映射函数
     * @param collectionSupplier 每组集合创建函数
     * @param <T>                元素类型
     * @param <K>                Key 类型
     * @param <C>                每组集合类型
     * @return 分组后的可变 LinkedHashMap
     */
    public static <T, K, C extends Collection<T>> Map<K, C> groupByTo(Iterable<? extends T> iterable,
                                                                      Function<? super T, ? extends K> keyMapper,
                                                                      Supplier<C> collectionSupplier) {
        Objects.requireNonNull(keyMapper, "keyMapper 不能为 null");
        Objects.requireNonNull(collectionSupplier, "collectionSupplier 不能为 null");

        Map<K, C> result = new LinkedHashMap<>();
        if (iterable == null) {
            return result;
        }

        for (T item : iterable) {
            K key = keyMapper.apply(item);
            result.computeIfAbsent(key, ignored -> collectionSupplier.get()).add(item);
        }
        return result;
    }

    /**
     * 按 Key 对 Collection 分组，并使用指定 Map 与指定集合保存结果。
     *
     * @param collection         Collection 对象
     * @param keyMapper          Key 映射函数
     * @param collectionSupplier 每组集合创建函数
     * @param mapSupplier        分组 Map 创建函数
     * @param <T>                元素类型
     * @param <K>                Key 类型
     * @param <C>                每组集合类型
     * @param <M>                分组 Map 类型
     * @return 分组后的目标 Map
     */
    public static <T, K, C extends Collection<T>, M extends Map<K, C>> M groupByTo(Collection<? extends T> collection,
                                                                                   Function<? super T, ? extends K> keyMapper,
                                                                                   Supplier<C> collectionSupplier,
                                                                                   Supplier<M> mapSupplier) {
        Objects.requireNonNull(keyMapper, "keyMapper 不能为 null");
        Objects.requireNonNull(collectionSupplier, "collectionSupplier 不能为 null");
        Objects.requireNonNull(mapSupplier, "mapSupplier 不能为 null");

        M result = mapSupplier.get();
        if (collection == null || collection.isEmpty()) {
            return result;
        }

        for (T item : collection) {
            K key = keyMapper.apply(item);
            result.computeIfAbsent(key, ignored -> collectionSupplier.get()).add(item);
        }
        return result;
    }

    /**
     * 按 Key 对 Collection 分组，并使用指定集合保存映射后的 Value。
     *
     * @param collection         Collection 对象
     * @param keyMapper          Key 映射函数
     * @param valueMapper        Value 映射函数
     * @param collectionSupplier 每组集合创建函数
     * @param <T>                元素类型
     * @param <K>                Key 类型
     * @param <V>                Value 类型
     * @param <C>                每组集合类型
     * @return 分组后的可变 LinkedHashMap
     */
    public static <T, K, V, C extends Collection<V>> Map<K, C> groupMappingTo(Collection<? extends T> collection,
                                                                              Function<? super T, ? extends K> keyMapper,
                                                                              Function<? super T, ? extends V> valueMapper,
                                                                              Supplier<C> collectionSupplier) {
        Objects.requireNonNull(keyMapper, "keyMapper 不能为 null");
        Objects.requireNonNull(valueMapper, "valueMapper 不能为 null");
        Objects.requireNonNull(collectionSupplier, "collectionSupplier 不能为 null");

        Map<K, C> result = new LinkedHashMap<>();
        if (collection == null || collection.isEmpty()) {
            return result;
        }

        for (T item : collection) {
            K key = keyMapper.apply(item);
            V value = valueMapper.apply(item);
            result.computeIfAbsent(key, ignored -> collectionSupplier.get()).add(value);
        }
        return result;
    }

    /**
     * 按 Key 对 Collection 分组，并使用指定 Map 与指定集合保存映射后的 Value。
     *
     * @param collection         Collection 对象
     * @param keyMapper          Key 映射函数
     * @param valueMapper        Value 映射函数
     * @param collectionSupplier 每组集合创建函数
     * @param mapSupplier        分组 Map 创建函数
     * @param <T>                元素类型
     * @param <K>                Key 类型
     * @param <V>                Value 类型
     * @param <C>                每组集合类型
     * @param <M>                分组 Map 类型
     * @return 分组后的目标 Map
     */
    public static <T, K, V, C extends Collection<V>, M extends Map<K, C>> M groupMappingTo(Collection<? extends T> collection,
                                                                                           Function<? super T, ? extends K> keyMapper,
                                                                                           Function<? super T, ? extends V> valueMapper,
                                                                                           Supplier<C> collectionSupplier,
                                                                                           Supplier<M> mapSupplier) {
        Objects.requireNonNull(keyMapper, "keyMapper 不能为 null");
        Objects.requireNonNull(valueMapper, "valueMapper 不能为 null");
        Objects.requireNonNull(collectionSupplier, "collectionSupplier 不能为 null");
        Objects.requireNonNull(mapSupplier, "mapSupplier 不能为 null");

        M result = mapSupplier.get();
        if (collection == null || collection.isEmpty()) {
            return result;
        }

        for (T item : collection) {
            K key = keyMapper.apply(item);
            V value = valueMapper.apply(item);
            result.computeIfAbsent(key, ignored -> collectionSupplier.get()).add(value);
        }
        return result;
    }

    /**
     * 按 Key 对 Collection 分组计数。
     *
     * @param collection Collection 对象
     * @param keyMapper  Key 映射函数
     * @param <T>        元素类型
     * @param <K>        Key 类型
     * @return 每个 Key 对应的元素数量
     */
    public static <T, K> Map<K, Long> groupCount(Collection<? extends T> collection, Function<? super T, ? extends K> keyMapper) {
        Objects.requireNonNull(keyMapper, "keyMapper 不能为 null");

        Map<K, Long> result = new LinkedHashMap<>();
        if (collection == null || collection.isEmpty()) {
            return result;
        }

        for (T item : collection) {
            K key = keyMapper.apply(item);
            result.merge(key, 1L, Long::sum);
        }
        return result;
    }

    /**
     * 按 Key 对 Iterable 分组计数。
     *
     * @param iterable  Iterable 对象
     * @param keyMapper Key 映射函数
     * @param <T>       元素类型
     * @param <K>       Key 类型
     * @return 每个 Key 对应的元素数量
     */
    public static <T, K> Map<K, Long> groupCount(Iterable<? extends T> iterable, Function<? super T, ? extends K> keyMapper) {
        Objects.requireNonNull(keyMapper, "keyMapper 不能为 null");

        Map<K, Long> result = new LinkedHashMap<>();
        if (iterable == null) {
            return result;
        }

        for (T item : iterable) {
            K key = keyMapper.apply(item);
            result.merge(key, 1L, Long::sum);
        }
        return result;
    }

    /**
     * 按 Key 对 Iterator 分组计数。
     *
     * <p>
     * 该方法会消费 Iterator。
     * </p>
     *
     * @param iterator  Iterator 对象
     * @param keyMapper Key 映射函数
     * @param <T>       元素类型
     * @param <K>       Key 类型
     * @return 每个 Key 对应的元素数量
     */
    public static <T, K> Map<K, Long> groupCount(Iterator<? extends T> iterator, Function<? super T, ? extends K> keyMapper) {
        Objects.requireNonNull(keyMapper, "keyMapper 不能为 null");

        Map<K, Long> result = new LinkedHashMap<>();
        if (iterator == null) {
            return result;
        }

        while (iterator.hasNext()) {
            T item = iterator.next();
            K key = keyMapper.apply(item);
            result.merge(key, 1L, Long::sum);
        }
        return result;
    }

    /**
     * 按 Key 对数组分组计数。
     *
     * @param array     数组
     * @param keyMapper Key 映射函数
     * @param <T>       元素类型
     * @param <K>       Key 类型
     * @return 每个 Key 对应的元素数量
     */
    public static <T, K> Map<K, Long> groupCount(T[] array, Function<? super T, ? extends K> keyMapper) {
        Objects.requireNonNull(keyMapper, "keyMapper 不能为 null");

        Map<K, Long> result = new LinkedHashMap<>();
        if (array == null || array.length == 0) {
            return result;
        }

        for (T item : array) {
            K key = keyMapper.apply(item);
            result.merge(key, 1L, Long::sum);
        }
        return result;
    }

    /**
     * 按 Key 对 Collection 分组，并对 int 值求和。
     *
     * @param collection  Collection 对象
     * @param keyMapper   Key 映射函数
     * @param valueMapper int 值映射函数
     * @param <T>         元素类型
     * @param <K>         Key 类型
     * @return 每个 Key 对应的 int 求和结果
     */
    public static <T, K> Map<K, Integer> groupSumInt(Collection<? extends T> collection,
                                                     Function<? super T, ? extends K> keyMapper,
                                                     ToIntFunction<? super T> valueMapper) {
        Objects.requireNonNull(keyMapper, "keyMapper 不能为 null");
        Objects.requireNonNull(valueMapper, "valueMapper 不能为 null");

        Map<K, Integer> result = new LinkedHashMap<>();
        if (collection == null || collection.isEmpty()) {
            return result;
        }

        for (T item : collection) {
            K key = keyMapper.apply(item);
            result.merge(key, valueMapper.applyAsInt(item), Integer::sum);
        }
        return result;
    }

    /**
     * 按 Key 对 Iterable 分组，并对 int 值求和。
     *
     * @param iterable    Iterable 对象
     * @param keyMapper   Key 映射函数
     * @param valueMapper int 值映射函数
     * @param <T>         元素类型
     * @param <K>         Key 类型
     * @return 每个 Key 对应的 int 求和结果
     */
    public static <T, K> Map<K, Integer> groupSumInt(Iterable<? extends T> iterable,
                                                     Function<? super T, ? extends K> keyMapper,
                                                     ToIntFunction<? super T> valueMapper) {
        Objects.requireNonNull(keyMapper, "keyMapper 不能为 null");
        Objects.requireNonNull(valueMapper, "valueMapper 不能为 null");

        Map<K, Integer> result = new LinkedHashMap<>();
        if (iterable == null) {
            return result;
        }

        for (T item : iterable) {
            K key = keyMapper.apply(item);
            result.merge(key, valueMapper.applyAsInt(item), Integer::sum);
        }
        return result;
    }

    /**
     * 按 Key 对数组分组，并对 int 值求和。
     *
     * @param array       数组
     * @param keyMapper   Key 映射函数
     * @param valueMapper int 值映射函数
     * @param <T>         元素类型
     * @param <K>         Key 类型
     * @return 每个 Key 对应的 int 求和结果
     */
    public static <T, K> Map<K, Integer> groupSumInt(T[] array,
                                                     Function<? super T, ? extends K> keyMapper,
                                                     ToIntFunction<? super T> valueMapper) {
        Objects.requireNonNull(keyMapper, "keyMapper 不能为 null");
        Objects.requireNonNull(valueMapper, "valueMapper 不能为 null");

        Map<K, Integer> result = new LinkedHashMap<>();
        if (array == null || array.length == 0) {
            return result;
        }

        for (T item : array) {
            K key = keyMapper.apply(item);
            result.merge(key, valueMapper.applyAsInt(item), Integer::sum);
        }
        return result;
    }

    /**
     * 按 Key 对 Collection 分组，并对 long 值求和。
     *
     * @param collection  Collection 对象
     * @param keyMapper   Key 映射函数
     * @param valueMapper long 值映射函数
     * @param <T>         元素类型
     * @param <K>         Key 类型
     * @return 每个 Key 对应的 long 求和结果
     */
    public static <T, K> Map<K, Long> groupSumLong(Collection<? extends T> collection,
                                                   Function<? super T, ? extends K> keyMapper,
                                                   ToLongFunction<? super T> valueMapper) {
        Objects.requireNonNull(keyMapper, "keyMapper 不能为 null");
        Objects.requireNonNull(valueMapper, "valueMapper 不能为 null");

        Map<K, Long> result = new LinkedHashMap<>();
        if (collection == null || collection.isEmpty()) {
            return result;
        }

        for (T item : collection) {
            K key = keyMapper.apply(item);
            result.merge(key, valueMapper.applyAsLong(item), Long::sum);
        }
        return result;
    }

    /**
     * 按 Key 对 Iterable 分组，并对 long 值求和。
     *
     * @param iterable    Iterable 对象
     * @param keyMapper   Key 映射函数
     * @param valueMapper long 值映射函数
     * @param <T>         元素类型
     * @param <K>         Key 类型
     * @return 每个 Key 对应的 long 求和结果
     */
    public static <T, K> Map<K, Long> groupSumLong(Iterable<? extends T> iterable,
                                                   Function<? super T, ? extends K> keyMapper,
                                                   ToLongFunction<? super T> valueMapper) {
        Objects.requireNonNull(keyMapper, "keyMapper 不能为 null");
        Objects.requireNonNull(valueMapper, "valueMapper 不能为 null");

        Map<K, Long> result = new LinkedHashMap<>();
        if (iterable == null) {
            return result;
        }

        for (T item : iterable) {
            K key = keyMapper.apply(item);
            result.merge(key, valueMapper.applyAsLong(item), Long::sum);
        }
        return result;
    }

    /**
     * 按 Key 对数组分组，并对 long 值求和。
     *
     * @param array       数组
     * @param keyMapper   Key 映射函数
     * @param valueMapper long 值映射函数
     * @param <T>         元素类型
     * @param <K>         Key 类型
     * @return 每个 Key 对应的 long 求和结果
     */
    public static <T, K> Map<K, Long> groupSumLong(T[] array,
                                                   Function<? super T, ? extends K> keyMapper,
                                                   ToLongFunction<? super T> valueMapper) {
        Objects.requireNonNull(keyMapper, "keyMapper 不能为 null");
        Objects.requireNonNull(valueMapper, "valueMapper 不能为 null");

        Map<K, Long> result = new LinkedHashMap<>();
        if (array == null || array.length == 0) {
            return result;
        }

        for (T item : array) {
            K key = keyMapper.apply(item);
            result.merge(key, valueMapper.applyAsLong(item), Long::sum);
        }
        return result;
    }

    /**
     * 按 Key 对 Collection 分组，并对 double 值求和。
     *
     * @param collection  Collection 对象
     * @param keyMapper   Key 映射函数
     * @param valueMapper double 值映射函数
     * @param <T>         元素类型
     * @param <K>         Key 类型
     * @return 每个 Key 对应的 double 求和结果
     */
    public static <T, K> Map<K, Double> groupSumDouble(Collection<? extends T> collection,
                                                       Function<? super T, ? extends K> keyMapper,
                                                       ToDoubleFunction<? super T> valueMapper) {
        Objects.requireNonNull(keyMapper, "keyMapper 不能为 null");
        Objects.requireNonNull(valueMapper, "valueMapper 不能为 null");

        Map<K, Double> result = new LinkedHashMap<>();
        if (collection == null || collection.isEmpty()) {
            return result;
        }

        for (T item : collection) {
            K key = keyMapper.apply(item);
            result.merge(key, valueMapper.applyAsDouble(item), Double::sum);
        }
        return result;
    }

    /**
     * 按 Key 对 Iterable 分组，并对 double 值求和。
     *
     * @param iterable    Iterable 对象
     * @param keyMapper   Key 映射函数
     * @param valueMapper double 值映射函数
     * @param <T>         元素类型
     * @param <K>         Key 类型
     * @return 每个 Key 对应的 double 求和结果
     */
    public static <T, K> Map<K, Double> groupSumDouble(Iterable<? extends T> iterable,
                                                       Function<? super T, ? extends K> keyMapper,
                                                       ToDoubleFunction<? super T> valueMapper) {
        Objects.requireNonNull(keyMapper, "keyMapper 不能为 null");
        Objects.requireNonNull(valueMapper, "valueMapper 不能为 null");

        Map<K, Double> result = new LinkedHashMap<>();
        if (iterable == null) {
            return result;
        }

        for (T item : iterable) {
            K key = keyMapper.apply(item);
            result.merge(key, valueMapper.applyAsDouble(item), Double::sum);
        }
        return result;
    }

    /**
     * 按 Key 对数组分组，并对 double 值求和。
     *
     * @param array       数组
     * @param keyMapper   Key 映射函数
     * @param valueMapper double 值映射函数
     * @param <T>         元素类型
     * @param <K>         Key 类型
     * @return 每个 Key 对应的 double 求和结果
     */
    public static <T, K> Map<K, Double> groupSumDouble(T[] array,
                                                       Function<? super T, ? extends K> keyMapper,
                                                       ToDoubleFunction<? super T> valueMapper) {
        Objects.requireNonNull(keyMapper, "keyMapper 不能为 null");
        Objects.requireNonNull(valueMapper, "valueMapper 不能为 null");

        Map<K, Double> result = new LinkedHashMap<>();
        if (array == null || array.length == 0) {
            return result;
        }

        for (T item : array) {
            K key = keyMapper.apply(item);
            result.merge(key, valueMapper.applyAsDouble(item), Double::sum);
        }
        return result;
    }

    /**
     * 按 Key 对 Collection 分组，并对 BigDecimal 值求和。
     *
     * <p>
     * valueMapper 返回 null 时按 BigDecimal.ZERO 处理。
     * </p>
     *
     * @param collection  Collection 对象
     * @param keyMapper   Key 映射函数
     * @param valueMapper BigDecimal 值映射函数
     * @param <T>         元素类型
     * @param <K>         Key 类型
     * @return 每个 Key 对应的 BigDecimal 求和结果
     */
    public static <T, K> Map<K, BigDecimal> groupSumBigDecimal(Collection<? extends T> collection,
                                                               Function<? super T, ? extends K> keyMapper,
                                                               Function<? super T, BigDecimal> valueMapper) {
        Objects.requireNonNull(keyMapper, "keyMapper 不能为 null");
        Objects.requireNonNull(valueMapper, "valueMapper 不能为 null");

        Map<K, BigDecimal> result = new LinkedHashMap<>();
        if (collection == null || collection.isEmpty()) {
            return result;
        }

        for (T item : collection) {
            K key = keyMapper.apply(item);
            BigDecimal value = Objects.requireNonNullElse(valueMapper.apply(item), BigDecimal.ZERO);
            result.merge(key, value, BigDecimal::add);
        }
        return result;
    }

    /**
     * 按 Key 对 Iterable 分组，并对 BigDecimal 值求和。
     *
     * <p>
     * valueMapper 返回 null 时按 BigDecimal.ZERO 处理。
     * </p>
     *
     * @param iterable    Iterable 对象
     * @param keyMapper   Key 映射函数
     * @param valueMapper BigDecimal 值映射函数
     * @param <T>         元素类型
     * @param <K>         Key 类型
     * @return 每个 Key 对应的 BigDecimal 求和结果
     */
    public static <T, K> Map<K, BigDecimal> groupSumBigDecimal(Iterable<? extends T> iterable,
                                                               Function<? super T, ? extends K> keyMapper,
                                                               Function<? super T, BigDecimal> valueMapper) {
        Objects.requireNonNull(keyMapper, "keyMapper 不能为 null");
        Objects.requireNonNull(valueMapper, "valueMapper 不能为 null");

        Map<K, BigDecimal> result = new LinkedHashMap<>();
        if (iterable == null) {
            return result;
        }

        for (T item : iterable) {
            K key = keyMapper.apply(item);
            BigDecimal value = Objects.requireNonNullElse(valueMapper.apply(item), BigDecimal.ZERO);
            result.merge(key, value, BigDecimal::add);
        }
        return result;
    }

    /**
     * 按 Key 对数组分组，并对 BigDecimal 值求和。
     *
     * <p>
     * valueMapper 返回 null 时按 BigDecimal.ZERO 处理。
     * </p>
     *
     * @param array       数组
     * @param keyMapper   Key 映射函数
     * @param valueMapper BigDecimal 值映射函数
     * @param <T>         元素类型
     * @param <K>         Key 类型
     * @return 每个 Key 对应的 BigDecimal 求和结果
     */
    public static <T, K> Map<K, BigDecimal> groupSumBigDecimal(T[] array,
                                                               Function<? super T, ? extends K> keyMapper,
                                                               Function<? super T, BigDecimal> valueMapper) {
        Objects.requireNonNull(keyMapper, "keyMapper 不能为 null");
        Objects.requireNonNull(valueMapper, "valueMapper 不能为 null");

        Map<K, BigDecimal> result = new LinkedHashMap<>();
        if (array == null || array.length == 0) {
            return result;
        }

        for (T item : array) {
            K key = keyMapper.apply(item);
            BigDecimal value = Objects.requireNonNullElse(valueMapper.apply(item), BigDecimal.ZERO);
            result.merge(key, value, BigDecimal::add);
        }
        return result;
    }

    /**
     * 按条件将 Collection 分为 true 和 false 两组。
     *
     * @param collection Collection 对象
     * @param predicate  分区条件
     * @param <T>        元素类型
     * @return 分区后的可变 LinkedHashMap
     */
    public static <T> Map<Boolean, List<T>> partition(Collection<? extends T> collection, Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate 不能为 null");

        Map<Boolean, List<T>> result = new LinkedHashMap<>();
        result.put(Boolean.TRUE, new ArrayList<>());
        result.put(Boolean.FALSE, new ArrayList<>());

        if (collection == null || collection.isEmpty()) {
            return result;
        }

        for (T item : collection) {
            result.get(predicate.test(item)).add(item);
        }
        return result;
    }

    /**
     * 按条件将 Iterable 分为 true 和 false 两组。
     *
     * @param iterable  Iterable 对象
     * @param predicate 分区条件
     * @param <T>       元素类型
     * @return 分区后的可变 LinkedHashMap
     */
    public static <T> Map<Boolean, List<T>> partition(Iterable<? extends T> iterable, Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate 不能为 null");

        Map<Boolean, List<T>> result = new LinkedHashMap<>();
        result.put(Boolean.TRUE, new ArrayList<>());
        result.put(Boolean.FALSE, new ArrayList<>());

        if (iterable == null) {
            return result;
        }

        for (T item : iterable) {
            result.get(predicate.test(item)).add(item);
        }
        return result;
    }

    /**
     * 按条件将数组分为 true 和 false 两组。
     *
     * @param array     数组
     * @param predicate 分区条件
     * @param <T>       元素类型
     * @return 分区后的可变 LinkedHashMap
     */
    public static <T> Map<Boolean, List<T>> partition(T[] array, Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate 不能为 null");

        Map<Boolean, List<T>> result = new LinkedHashMap<>();
        result.put(Boolean.TRUE, new ArrayList<>());
        result.put(Boolean.FALSE, new ArrayList<>());

        if (array == null || array.length == 0) {
            return result;
        }

        for (T item : array) {
            result.get(predicate.test(item)).add(item);
        }
        return result;
    }

    /**
     * 使用 Comparator 对 Collection 排序，并返回 ArrayList。
     *
     * <p>
     * 不修改原集合。
     * </p>
     *
     * @param collection Collection 对象
     * @param comparator 排序比较器
     * @param <T>        元素类型
     * @return 排序后的可变 ArrayList
     */
    public static <T> List<T> sort(Collection<? extends T> collection, Comparator<? super T> comparator) {
        Objects.requireNonNull(comparator, "comparator 不能为 null");

        List<T> result = toList(collection);
        result.sort(comparator);
        return result;
    }

    /**
     * 使用 Comparator 对 Iterable 排序，并返回 ArrayList。
     *
     * <p>
     * 不修改原 Iterable。
     * </p>
     *
     * @param iterable   Iterable 对象
     * @param comparator 排序比较器
     * @param <T>        元素类型
     * @return 排序后的可变 ArrayList
     */
    public static <T> List<T> sort(Iterable<? extends T> iterable, Comparator<? super T> comparator) {
        Objects.requireNonNull(comparator, "comparator 不能为 null");

        List<T> result = toList(iterable);
        result.sort(comparator);
        return result;
    }

    /**
     * 使用 Comparator 对 Iterator 排序，并返回 ArrayList。
     *
     * <p>
     * 该方法会消费 Iterator。
     * </p>
     *
     * @param iterator   Iterator 对象
     * @param comparator 排序比较器
     * @param <T>        元素类型
     * @return 排序后的可变 ArrayList
     */
    public static <T> List<T> sort(Iterator<? extends T> iterator, Comparator<? super T> comparator) {
        Objects.requireNonNull(comparator, "comparator 不能为 null");

        List<T> result = toList(iterator);
        result.sort(comparator);
        return result;
    }

    /**
     * 使用 Comparator 对数组排序，并返回 ArrayList。
     *
     * <p>
     * 不修改原数组。
     * </p>
     *
     * @param array      数组
     * @param comparator 排序比较器
     * @param <T>        元素类型
     * @return 排序后的可变 ArrayList
     */
    public static <T> List<T> sort(T[] array, Comparator<? super T> comparator) {
        Objects.requireNonNull(comparator, "comparator 不能为 null");

        List<T> result = toList(array);
        result.sort(comparator);
        return result;
    }

    /**
     * 对 List 原地排序。
     *
     * @param list       目标 List
     * @param comparator 排序比较器
     * @param <T>        元素类型
     * @return true 表示执行了排序，List 为 null 或元素数量小于 2 时返回 false
     */
    public static <T> boolean sortInPlace(List<T> list, Comparator<? super T> comparator) {
        Objects.requireNonNull(comparator, "comparator 不能为 null");

        if (list == null || list.size() < 2) {
            return false;
        }

        list.sort(comparator);
        return true;
    }

    /**
     * 对数组原地排序。
     *
     * @param array      数组
     * @param comparator 排序比较器
     * @param <T>        元素类型
     * @return true 表示执行了排序，数组为 null 或元素数量小于 2 时返回 false
     */
    public static <T> boolean sortInPlace(T[] array, Comparator<? super T> comparator) {
        Objects.requireNonNull(comparator, "comparator 不能为 null");

        if (array == null || array.length < 2) {
            return false;
        }

        Arrays.sort(array, comparator);
        return true;
    }

    /**
     * 按自然顺序升序排序 Collection。
     *
     * <p>
     * null 元素排在最后。
     * </p>
     *
     * @param collection Collection 对象
     * @param <T>        元素类型
     * @return 排序后的可变 ArrayList
     */
    public static <T extends Comparable<? super T>> List<T> sortAsc(Collection<? extends T> collection) {
        return sort(collection, Comparator.nullsLast(Comparator.naturalOrder()));
    }

    /**
     * 按自然顺序升序排序 Iterable。
     *
     * <p>
     * null 元素排在最后。
     * </p>
     *
     * @param iterable Iterable 对象
     * @param <T>      元素类型
     * @return 排序后的可变 ArrayList
     */
    public static <T extends Comparable<? super T>> List<T> sortAsc(Iterable<? extends T> iterable) {
        return sort(iterable, Comparator.nullsLast(Comparator.naturalOrder()));
    }

    /**
     * 按自然顺序升序排序数组。
     *
     * <p>
     * null 元素排在最后。
     * </p>
     *
     * @param array 数组
     * @param <T>   元素类型
     * @return 排序后的可变 ArrayList
     */
    public static <T extends Comparable<? super T>> List<T> sortAsc(T[] array) {
        return sort(array, Comparator.nullsLast(Comparator.naturalOrder()));
    }

    /**
     * 按自然顺序降序排序 Collection。
     *
     * <p>
     * null 元素排在最后。
     * </p>
     *
     * @param collection Collection 对象
     * @param <T>        元素类型
     * @return 排序后的可变 ArrayList
     */
    public static <T extends Comparable<? super T>> List<T> sortDesc(Collection<? extends T> collection) {
        return sort(collection, Comparator.nullsLast(Comparator.reverseOrder()));
    }

    /**
     * 按自然顺序降序排序 Iterable。
     *
     * <p>
     * null 元素排在最后。
     * </p>
     *
     * @param iterable Iterable 对象
     * @param <T>      元素类型
     * @return 排序后的可变 ArrayList
     */
    public static <T extends Comparable<? super T>> List<T> sortDesc(Iterable<? extends T> iterable) {
        return sort(iterable, Comparator.nullsLast(Comparator.reverseOrder()));
    }

    /**
     * 按自然顺序降序排序数组。
     *
     * <p>
     * null 元素排在最后。
     * </p>
     *
     * @param array 数组
     * @param <T>   元素类型
     * @return 排序后的可变 ArrayList
     */
    public static <T extends Comparable<? super T>> List<T> sortDesc(T[] array) {
        return sort(array, Comparator.nullsLast(Comparator.reverseOrder()));
    }

    /**
     * 按 Key 升序排序 Collection。
     *
     * <p>
     * Key 为 null 时排在最后。
     * </p>
     *
     * @param collection Collection 对象
     * @param keyMapper  Key 映射函数
     * @param <T>        元素类型
     * @param <K>        Key 类型
     * @return 排序后的可变 ArrayList
     */
    public static <T, K extends Comparable<? super K>> List<T> sortByAsc(Collection<? extends T> collection,
                                                                         Function<? super T, ? extends K> keyMapper) {
        Objects.requireNonNull(keyMapper, "keyMapper 不能为 null");

        return sort(collection, Comparator.comparing(keyMapper, Comparator.nullsLast(Comparator.naturalOrder())));
    }

    /**
     * 按 Key 升序排序 Iterable。
     *
     * <p>
     * Key 为 null 时排在最后。
     * </p>
     *
     * @param iterable  Iterable 对象
     * @param keyMapper Key 映射函数
     * @param <T>       元素类型
     * @param <K>       Key 类型
     * @return 排序后的可变 ArrayList
     */
    public static <T, K extends Comparable<? super K>> List<T> sortByAsc(Iterable<? extends T> iterable,
                                                                         Function<? super T, ? extends K> keyMapper) {
        Objects.requireNonNull(keyMapper, "keyMapper 不能为 null");

        return sort(iterable, Comparator.comparing(keyMapper, Comparator.nullsLast(Comparator.naturalOrder())));
    }

    /**
     * 按 Key 升序排序数组。
     *
     * <p>
     * Key 为 null 时排在最后。
     * </p>
     *
     * @param array     数组
     * @param keyMapper Key 映射函数
     * @param <T>       元素类型
     * @param <K>       Key 类型
     * @return 排序后的可变 ArrayList
     */
    public static <T, K extends Comparable<? super K>> List<T> sortByAsc(T[] array,
                                                                         Function<? super T, ? extends K> keyMapper) {
        Objects.requireNonNull(keyMapper, "keyMapper 不能为 null");

        return sort(array, Comparator.comparing(keyMapper, Comparator.nullsLast(Comparator.naturalOrder())));
    }

    /**
     * 按 Key 降序排序 Collection。
     *
     * <p>
     * Key 为 null 时排在最后。
     * </p>
     *
     * @param collection Collection 对象
     * @param keyMapper  Key 映射函数
     * @param <T>        元素类型
     * @param <K>        Key 类型
     * @return 排序后的可变 ArrayList
     */
    public static <T, K extends Comparable<? super K>> List<T> sortByDesc(Collection<? extends T> collection,
                                                                          Function<? super T, ? extends K> keyMapper) {
        Objects.requireNonNull(keyMapper, "keyMapper 不能为 null");

        return sort(collection, Comparator.comparing(keyMapper, Comparator.nullsLast(Comparator.reverseOrder())));
    }

    /**
     * 按 Key 降序排序 Iterable。
     *
     * <p>
     * Key 为 null 时排在最后。
     * </p>
     *
     * @param iterable  Iterable 对象
     * @param keyMapper Key 映射函数
     * @param <T>       元素类型
     * @param <K>       Key 类型
     * @return 排序后的可变 ArrayList
     */
    public static <T, K extends Comparable<? super K>> List<T> sortByDesc(Iterable<? extends T> iterable,
                                                                          Function<? super T, ? extends K> keyMapper) {
        Objects.requireNonNull(keyMapper, "keyMapper 不能为 null");

        return sort(iterable, Comparator.comparing(keyMapper, Comparator.nullsLast(Comparator.reverseOrder())));
    }

    /**
     * 按 Key 降序排序数组。
     *
     * <p>
     * Key 为 null 时排在最后。
     * </p>
     *
     * @param array     数组
     * @param keyMapper Key 映射函数
     * @param <T>       元素类型
     * @param <K>       Key 类型
     * @return 排序后的可变 ArrayList
     */
    public static <T, K extends Comparable<? super K>> List<T> sortByDesc(T[] array,
                                                                          Function<? super T, ? extends K> keyMapper) {
        Objects.requireNonNull(keyMapper, "keyMapper 不能为 null");

        return sort(array, Comparator.comparing(keyMapper, Comparator.nullsLast(Comparator.reverseOrder())));
    }

    /**
     * 按 Key 排序 Collection。
     *
     * <p>
     * Key 为 null 时排在最后。
     * </p>
     *
     * @param collection    Collection 对象
     * @param keyMapper     Key 映射函数
     * @param keyComparator Key 比较器
     * @param <T>           元素类型
     * @param <K>           Key 类型
     * @return 排序后的可变 ArrayList
     */
    public static <T, K> List<T> sortBy(Collection<? extends T> collection,
                                        Function<? super T, ? extends K> keyMapper,
                                        Comparator<? super K> keyComparator) {
        Objects.requireNonNull(keyMapper, "keyMapper 不能为 null");
        Objects.requireNonNull(keyComparator, "keyComparator 不能为 null");

        return sort(collection, Comparator.comparing(keyMapper, Comparator.nullsLast(keyComparator)));
    }

    /**
     * 按 Key 排序 Iterable。
     *
     * <p>
     * Key 为 null 时排在最后。
     * </p>
     *
     * @param iterable      Iterable 对象
     * @param keyMapper     Key 映射函数
     * @param keyComparator Key 比较器
     * @param <T>           元素类型
     * @param <K>           Key 类型
     * @return 排序后的可变 ArrayList
     */
    public static <T, K> List<T> sortBy(Iterable<? extends T> iterable,
                                        Function<? super T, ? extends K> keyMapper,
                                        Comparator<? super K> keyComparator) {
        Objects.requireNonNull(keyMapper, "keyMapper 不能为 null");
        Objects.requireNonNull(keyComparator, "keyComparator 不能为 null");

        return sort(iterable, Comparator.comparing(keyMapper, Comparator.nullsLast(keyComparator)));
    }

    /**
     * 按 Key 排序数组。
     *
     * <p>
     * Key 为 null 时排在最后。
     * </p>
     *
     * @param array         数组
     * @param keyMapper     Key 映射函数
     * @param keyComparator Key 比较器
     * @param <T>           元素类型
     * @param <K>           Key 类型
     * @return 排序后的可变 ArrayList
     */
    public static <T, K> List<T> sortBy(T[] array,
                                        Function<? super T, ? extends K> keyMapper,
                                        Comparator<? super K> keyComparator) {
        Objects.requireNonNull(keyMapper, "keyMapper 不能为 null");
        Objects.requireNonNull(keyComparator, "keyComparator 不能为 null");

        return sort(array, Comparator.comparing(keyMapper, Comparator.nullsLast(keyComparator)));
    }

    /**
     * 按 int Key 升序排序 Collection。
     *
     * @param collection Collection 对象
     * @param keyMapper  int Key 映射函数
     * @param <T>        元素类型
     * @return 排序后的可变 ArrayList
     */
    public static <T> List<T> sortByIntAsc(Collection<? extends T> collection, ToIntFunction<? super T> keyMapper) {
        Objects.requireNonNull(keyMapper, "keyMapper 不能为 null");

        return sort(collection, Comparator.comparingInt(keyMapper));
    }

    /**
     * 按 int Key 降序排序 Collection。
     *
     * @param collection Collection 对象
     * @param keyMapper  int Key 映射函数
     * @param <T>        元素类型
     * @return 排序后的可变 ArrayList
     */
    public static <T> List<T> sortByIntDesc(Collection<? extends T> collection, ToIntFunction<? super T> keyMapper) {
        Objects.requireNonNull(keyMapper, "keyMapper 不能为 null");

        return sort(collection, Comparator.comparingInt(keyMapper).reversed());
    }

    /**
     * 按 long Key 升序排序 Collection。
     *
     * @param collection Collection 对象
     * @param keyMapper  long Key 映射函数
     * @param <T>        元素类型
     * @return 排序后的可变 ArrayList
     */
    public static <T> List<T> sortByLongAsc(Collection<? extends T> collection, ToLongFunction<? super T> keyMapper) {
        Objects.requireNonNull(keyMapper, "keyMapper 不能为 null");

        return sort(collection, Comparator.comparingLong(keyMapper));
    }

    /**
     * 按 long Key 降序排序 Collection。
     *
     * @param collection Collection 对象
     * @param keyMapper  long Key 映射函数
     * @param <T>        元素类型
     * @return 排序后的可变 ArrayList
     */
    public static <T> List<T> sortByLongDesc(Collection<? extends T> collection, ToLongFunction<? super T> keyMapper) {
        Objects.requireNonNull(keyMapper, "keyMapper 不能为 null");

        return sort(collection, Comparator.comparingLong(keyMapper).reversed());
    }

    /**
     * 按 double Key 升序排序 Collection。
     *
     * @param collection Collection 对象
     * @param keyMapper  double Key 映射函数
     * @param <T>        元素类型
     * @return 排序后的可变 ArrayList
     */
    public static <T> List<T> sortByDoubleAsc(Collection<? extends T> collection, ToDoubleFunction<? super T> keyMapper) {
        Objects.requireNonNull(keyMapper, "keyMapper 不能为 null");

        return sort(collection, Comparator.comparingDouble(keyMapper));
    }

    /**
     * 按 double Key 降序排序 Collection。
     *
     * @param collection Collection 对象
     * @param keyMapper  double Key 映射函数
     * @param <T>        元素类型
     * @return 排序后的可变 ArrayList
     */
    public static <T> List<T> sortByDoubleDesc(Collection<? extends T> collection, ToDoubleFunction<? super T> keyMapper) {
        Objects.requireNonNull(keyMapper, "keyMapper 不能为 null");

        return sort(collection, Comparator.comparingDouble(keyMapper).reversed());
    }

    /**
     * 多字段排序 Collection。
     *
     * <p>
     * 按传入的 Comparator 顺序依次比较。
     * </p>
     *
     * @param collection  Collection 对象
     * @param comparators 排序比较器数组
     * @param <T>         元素类型
     * @return 排序后的可变 ArrayList
     */
    @SafeVarargs
    public static <T> List<T> sortByMany(Collection<? extends T> collection, Comparator<? super T>... comparators) {
        return sort(collection, combineComparators(comparators));
    }

    /**
     * 多字段排序 Iterable。
     *
     * <p>
     * 按传入的 Comparator 顺序依次比较。
     * </p>
     *
     * @param iterable    Iterable 对象
     * @param comparators 排序比较器数组
     * @param <T>         元素类型
     * @return 排序后的可变 ArrayList
     */
    @SafeVarargs
    public static <T> List<T> sortByMany(Iterable<? extends T> iterable, Comparator<? super T>... comparators) {
        return sort(iterable, combineComparators(comparators));
    }

    /**
     * 多字段排序数组。
     *
     * <p>
     * 按传入的 Comparator 顺序依次比较。
     * </p>
     *
     * @param array       数组
     * @param comparators 排序比较器数组
     * @param <T>         元素类型
     * @return 排序后的可变 ArrayList
     */
    @SafeVarargs
    public static <T> List<T> sortByMany(T[] array, Comparator<? super T>... comparators) {
        return sort(array, combineComparators(comparators));
    }

    /**
     * 使用 Comparator 排序 Collection，并将 null 元素排在前面。
     *
     * @param collection Collection 对象
     * @param comparator 非 null 元素比较器
     * @param <T>        元素类型
     * @return 排序后的可变 ArrayList
     */
    public static <T> List<T> sortNullsFirst(Collection<? extends T> collection, Comparator<? super T> comparator) {
        Objects.requireNonNull(comparator, "comparator 不能为 null");

        return sort(collection, Comparator.nullsFirst(comparator));
    }

    /**
     * 使用 Comparator 排序 Collection，并将 null 元素排在最后。
     *
     * @param collection Collection 对象
     * @param comparator 非 null 元素比较器
     * @param <T>        元素类型
     * @return 排序后的可变 ArrayList
     */
    public static <T> List<T> sortNullsLast(Collection<? extends T> collection, Comparator<? super T> comparator) {
        Objects.requireNonNull(comparator, "comparator 不能为 null");

        return sort(collection, Comparator.nullsLast(comparator));
    }

    /**
     * 按自然顺序升序排序 Collection，并将 null 元素排在前面。
     *
     * @param collection Collection 对象
     * @param <T>        元素类型
     * @return 排序后的可变 ArrayList
     */
    public static <T extends Comparable<? super T>> List<T> sortAscNullsFirst(Collection<? extends T> collection) {
        return sort(collection, Comparator.nullsFirst(Comparator.naturalOrder()));
    }

    /**
     * 按自然顺序升序排序 Collection，并将 null 元素排在最后。
     *
     * @param collection Collection 对象
     * @param <T>        元素类型
     * @return 排序后的可变 ArrayList
     */
    public static <T extends Comparable<? super T>> List<T> sortAscNullsLast(Collection<? extends T> collection) {
        return sort(collection, Comparator.nullsLast(Comparator.naturalOrder()));
    }

    /**
     * 按自然顺序降序排序 Collection，并将 null 元素排在前面。
     *
     * @param collection Collection 对象
     * @param <T>        元素类型
     * @return 排序后的可变 ArrayList
     */
    public static <T extends Comparable<? super T>> List<T> sortDescNullsFirst(Collection<? extends T> collection) {
        return sort(collection, Comparator.nullsFirst(Comparator.reverseOrder()));
    }

    /**
     * 按自然顺序降序排序 Collection，并将 null 元素排在最后。
     *
     * @param collection Collection 对象
     * @param <T>        元素类型
     * @return 排序后的可变 ArrayList
     */
    public static <T extends Comparable<? super T>> List<T> sortDescNullsLast(Collection<? extends T> collection) {
        return sort(collection, Comparator.nullsLast(Comparator.reverseOrder()));
    }

    /**
     * 反转 Collection 的遍历顺序。
     *
     * <p>
     * 不修改原集合。
     * </p>
     *
     * @param collection Collection 对象
     * @param <T>        元素类型
     * @return 反转后的可变 ArrayList
     */
    public static <T> List<T> reverse(Collection<? extends T> collection) {
        List<T> result = toList(collection);
        Collections.reverse(result);
        return result;
    }

    /**
     * 反转 Iterable 的遍历顺序。
     *
     * @param iterable Iterable 对象
     * @param <T>      元素类型
     * @return 反转后的可变 ArrayList
     */
    public static <T> List<T> reverse(Iterable<? extends T> iterable) {
        List<T> result = toList(iterable);
        Collections.reverse(result);
        return result;
    }

    /**
     * 反转数组元素顺序。
     *
     * <p>
     * 不修改原数组。
     * </p>
     *
     * @param array 数组
     * @param <T>   元素类型
     * @return 反转后的可变 ArrayList
     */
    public static <T> List<T> reverse(T[] array) {
        List<T> result = toList(array);
        Collections.reverse(result);
        return result;
    }

    /**
     * 原地反转 List。
     *
     * @param list 目标 List
     * @param <T>  元素类型
     * @return true 表示执行了反转，List 为 null 或元素数量小于 2 时返回 false
     */
    public static <T> boolean reverseInPlace(List<T> list) {
        if (list == null || list.size() < 2) {
            return false;
        }

        Collections.reverse(list);
        return true;
    }

    /**
     * 原地反转数组。
     *
     * @param array 目标数组
     * @param <T>   元素类型
     * @return true 表示执行了反转，数组为 null 或元素数量小于 2 时返回 false
     */
    public static <T> boolean reverseInPlace(T[] array) {
        if (array == null || array.length < 2) {
            return false;
        }

        int left = 0;
        int right = array.length - 1;
        while (left < right) {
            T temp = array[left];
            array[left] = array[right];
            array[right] = temp;
            left++;
            right--;
        }
        return true;
    }

    /**
     * 获取 Collection 排序后的前 N 个元素。
     *
     * @param collection Collection 对象
     * @param limit      返回数量
     * @param comparator 排序比较器
     * @param <T>        元素类型
     * @return 排序后的前 N 个元素
     */
    public static <T> List<T> top(Collection<? extends T> collection, int limit, Comparator<? super T> comparator) {
        checkExpectedSize(limit);
        if (limit == 0) {
            return new ArrayList<>();
        }

        List<T> result = sort(collection, comparator);
        return result.size() <= limit ? result : new ArrayList<>(result.subList(0, limit));
    }

    /**
     * 获取 Collection 按自然顺序升序排序后的前 N 个元素。
     *
     * <p>
     * null 元素排在最后。
     * </p>
     *
     * @param collection Collection 对象
     * @param limit      返回数量
     * @param <T>        元素类型
     * @return 排序后的前 N 个元素
     */
    public static <T extends Comparable<? super T>> List<T> topAsc(Collection<? extends T> collection, int limit) {
        return top(collection, limit, Comparator.nullsLast(Comparator.naturalOrder()));
    }

    /**
     * 获取 Collection 按自然顺序降序排序后的前 N 个元素。
     *
     * <p>
     * null 元素排在最后。
     * </p>
     *
     * @param collection Collection 对象
     * @param limit      返回数量
     * @param <T>        元素类型
     * @return 排序后的前 N 个元素
     */
    public static <T extends Comparable<? super T>> List<T> topDesc(Collection<? extends T> collection, int limit) {
        return top(collection, limit, Comparator.nullsLast(Comparator.reverseOrder()));
    }

    /**
     * 获取 Collection 按 Key 升序排序后的前 N 个元素。
     *
     * <p>
     * Key 为 null 时排在最后。
     * </p>
     *
     * @param collection Collection 对象
     * @param limit      返回数量
     * @param keyMapper  Key 映射函数
     * @param <T>        元素类型
     * @param <K>        Key 类型
     * @return 排序后的前 N 个元素
     */
    public static <T, K extends Comparable<? super K>> List<T> topByAsc(Collection<? extends T> collection,
                                                                        int limit,
                                                                        Function<? super T, ? extends K> keyMapper) {
        checkExpectedSize(limit);
        if (limit == 0) {
            return new ArrayList<>();
        }

        List<T> result = sortByAsc(collection, keyMapper);
        return result.size() <= limit ? result : new ArrayList<>(result.subList(0, limit));
    }

    /**
     * 获取 Collection 按 Key 降序排序后的前 N 个元素。
     *
     * <p>
     * Key 为 null 时排在最后。
     * </p>
     *
     * @param collection Collection 对象
     * @param limit      返回数量
     * @param keyMapper  Key 映射函数
     * @param <T>        元素类型
     * @param <K>        Key 类型
     * @return 排序后的前 N 个元素
     */
    public static <T, K extends Comparable<? super K>> List<T> topByDesc(Collection<? extends T> collection,
                                                                         int limit,
                                                                         Function<? super T, ? extends K> keyMapper) {
        checkExpectedSize(limit);
        if (limit == 0) {
            return new ArrayList<>();
        }

        List<T> result = sortByDesc(collection, keyMapper);
        return result.size() <= limit ? result : new ArrayList<>(result.subList(0, limit));
    }

    /**
     * 对 Collection 排序并去重。
     *
     * <p>
     * 先按 Comparator 排序，再按元素 equals 语义去重。
     * </p>
     *
     * @param collection Collection 对象
     * @param comparator 排序比较器
     * @param <T>        元素类型
     * @return 排序去重后的可变 ArrayList
     */
    public static <T> List<T> sortDistinct(Collection<? extends T> collection, Comparator<? super T> comparator) {
        List<T> sortedList = sort(collection, comparator);
        return distinctToList(sortedList);
    }

    /**
     * 按自然顺序升序排序并去重。
     *
     * <p>
     * null 元素排在最后。
     * </p>
     *
     * @param collection Collection 对象
     * @param <T>        元素类型
     * @return 排序去重后的可变 ArrayList
     */
    public static <T extends Comparable<? super T>> List<T> sortDistinctAsc(Collection<? extends T> collection) {
        return sortDistinct(collection, Comparator.nullsLast(Comparator.naturalOrder()));
    }

    /**
     * 按自然顺序降序排序并去重。
     *
     * <p>
     * null 元素排在最后。
     * </p>
     *
     * @param collection Collection 对象
     * @param <T>        元素类型
     * @return 排序去重后的可变 ArrayList
     */
    public static <T extends Comparable<? super T>> List<T> sortDistinctDesc(Collection<? extends T> collection) {
        return sortDistinct(collection, Comparator.nullsLast(Comparator.reverseOrder()));
    }

    /**
     * 合并多个 Comparator。
     *
     * @param comparators Comparator 数组
     * @param <T>         元素类型
     * @return 合并后的 Comparator
     */
    @SafeVarargs
    private static <T> Comparator<T> combineComparators(Comparator<? super T>... comparators) {
        if (comparators == null || comparators.length == 0) {
            throw new IllegalArgumentException("comparators 不能为空");
        }

        Comparator<T> result = null;
        for (Comparator<? super T> comparator : comparators) {
            if (comparator == null) {
                continue;
            }

            Comparator<T> typedComparator = (left, right) -> comparator.compare(left, right);
            result = result == null ? typedComparator : result.thenComparing(typedComparator);
        }

        if (result == null) {
            throw new IllegalArgumentException("comparators 不能全部为 null");
        }
        return result;
    }

    /**
     * 对 Collection 进行内存分页。
     *
     * <p>
     * pageNum 从 1 开始；pageSize 必须大于等于 0。
     * pageSize 为 0 时返回空列表。
     * </p>
     *
     * @param collection Collection 对象
     * @param pageNum    页码，从 1 开始
     * @param pageSize   每页数量
     * @param <T>        元素类型
     * @return 分页后的可变 ArrayList
     */
    public static <T> List<T> page(Collection<? extends T> collection, int pageNum, int pageSize) {
        if (collection == null || collection.isEmpty()) {
            return new ArrayList<>();
        }
        return page(toList(collection), pageNum, pageSize);
    }

    /**
     * 对 List 进行内存分页。
     *
     * <p>
     * pageNum 从 1 开始；pageSize 必须大于等于 0。
     * pageSize 为 0 时返回空列表。
     * </p>
     *
     * @param list     List 对象
     * @param pageNum  页码，从 1 开始
     * @param pageSize 每页数量
     * @param <T>      元素类型
     * @return 分页后的可变 ArrayList
     */
    public static <T> List<T> page(List<? extends T> list, int pageNum, int pageSize) {
        checkPageParam(pageNum, pageSize);

        if (list == null || list.isEmpty() || pageSize == 0) {
            return new ArrayList<>();
        }

        int fromIndex = pageOffset(pageNum, pageSize);
        return slice(list, fromIndex, fromIndex + pageSize);
    }

    /**
     * 对 Iterable 进行内存分页。
     *
     * <p>
     * pageNum 从 1 开始；该方法会遍历 Iterable。
     * </p>
     *
     * @param iterable Iterable 对象
     * @param pageNum  页码，从 1 开始
     * @param pageSize 每页数量
     * @param <T>      元素类型
     * @return 分页后的可变 ArrayList
     */
    public static <T> List<T> page(Iterable<? extends T> iterable, int pageNum, int pageSize) {
        if (iterable == null) {
            return new ArrayList<>();
        }
        return page(toList(iterable), pageNum, pageSize);
    }

    /**
     * 对 Iterator 进行内存分页。
     *
     * <p>
     * pageNum 从 1 开始；该方法会消费 Iterator。
     * </p>
     *
     * @param iterator Iterator 对象
     * @param pageNum  页码，从 1 开始
     * @param pageSize 每页数量
     * @param <T>      元素类型
     * @return 分页后的可变 ArrayList
     */
    public static <T> List<T> page(Iterator<? extends T> iterator, int pageNum, int pageSize) {
        checkPageParam(pageNum, pageSize);

        List<T> result = new ArrayList<>();
        if (iterator == null || pageSize == 0) {
            return result;
        }

        int fromIndex = pageOffset(pageNum, pageSize);
        int toIndex = fromIndex + pageSize;
        int index = 0;

        while (iterator.hasNext()) {
            T item = iterator.next();
            if (index >= fromIndex && index < toIndex) {
                result.add(item);
            }
            if (index >= toIndex) {
                break;
            }
            index++;
        }
        return result;
    }

    /**
     * 对数组进行内存分页。
     *
     * <p>
     * pageNum 从 1 开始；pageSize 必须大于等于 0。
     * </p>
     *
     * @param array    数组
     * @param pageNum  页码，从 1 开始
     * @param pageSize 每页数量
     * @param <T>      元素类型
     * @return 分页后的可变 ArrayList
     */
    public static <T> List<T> page(T[] array, int pageNum, int pageSize) {
        checkPageParam(pageNum, pageSize);

        if (array == null || array.length == 0 || pageSize == 0) {
            return new ArrayList<>();
        }

        int fromIndex = pageOffset(pageNum, pageSize);
        return slice(array, fromIndex, fromIndex + pageSize);
    }

    /**
     * 根据 offset 和 limit 截取 Collection。
     *
     * <p>
     * offset 从 0 开始；limit 为最多返回数量。
     * </p>
     *
     * @param collection Collection 对象
     * @param offset     偏移量，从 0 开始
     * @param limit      返回数量
     * @param <T>        元素类型
     * @return 截取后的可变 ArrayList
     */
    public static <T> List<T> pageByOffset(Collection<? extends T> collection, int offset, int limit) {
        return limit(collection, offset, limit);
    }

    /**
     * 根据 offset 和 limit 截取 List。
     *
     * <p>
     * offset 从 0 开始；limit 为最多返回数量。
     * </p>
     *
     * @param list   List 对象
     * @param offset 偏移量，从 0 开始
     * @param limit  返回数量
     * @param <T>    元素类型
     * @return 截取后的可变 ArrayList
     */
    public static <T> List<T> pageByOffset(List<? extends T> list, int offset, int limit) {
        return limit(list, offset, limit);
    }

    /**
     * 根据 offset 和 limit 截取 Iterable。
     *
     * <p>
     * offset 从 0 开始；limit 为最多返回数量。
     * </p>
     *
     * @param iterable Iterable 对象
     * @param offset   偏移量，从 0 开始
     * @param limit    返回数量
     * @param <T>      元素类型
     * @return 截取后的可变 ArrayList
     */
    public static <T> List<T> pageByOffset(Iterable<? extends T> iterable, int offset, int limit) {
        return limit(iterable, offset, limit);
    }

    /**
     * 根据 offset 和 limit 截取数组。
     *
     * <p>
     * offset 从 0 开始；limit 为最多返回数量。
     * </p>
     *
     * @param array  数组
     * @param offset 偏移量，从 0 开始
     * @param limit  返回数量
     * @param <T>    元素类型
     * @return 截取后的可变 ArrayList
     */
    public static <T> List<T> pageByOffset(T[] array, int offset, int limit) {
        return limit(array, offset, limit);
    }

    /**
     * 截取 List 指定区间。
     *
     * <p>
     * fromIndex 包含，toIndex 不包含；下标会自动收敛到合法范围。
     * </p>
     *
     * @param list      List 对象
     * @param fromIndex 开始下标，包含
     * @param toIndex   结束下标，不包含
     * @param <T>       元素类型
     * @return 截取后的可变 ArrayList
     */
    public static <T> List<T> slice(List<? extends T> list, int fromIndex, int toIndex) {
        if (list == null || list.isEmpty()) {
            return new ArrayList<>();
        }

        int size = list.size();
        int start = Math.max(0, Math.min(fromIndex, size));
        int end = Math.max(start, Math.min(toIndex, size));

        if (start >= end) {
            return new ArrayList<>();
        }
        return new ArrayList<>(list.subList(start, end));
    }

    /**
     * 截取 Collection 指定区间。
     *
     * <p>
     * fromIndex 包含，toIndex 不包含；下标会自动收敛到合法范围。
     * </p>
     *
     * @param collection Collection 对象
     * @param fromIndex  开始下标，包含
     * @param toIndex    结束下标，不包含
     * @param <T>        元素类型
     * @return 截取后的可变 ArrayList
     */
    public static <T> List<T> slice(Collection<? extends T> collection, int fromIndex, int toIndex) {
        if (collection == null || collection.isEmpty()) {
            return new ArrayList<>();
        }
        if (collection instanceof List<? extends T> list) {
            return slice(list, fromIndex, toIndex);
        }
        return slice(toList(collection), fromIndex, toIndex);
    }

    /**
     * 截取 Iterable 指定区间。
     *
     * <p>
     * fromIndex 包含，toIndex 不包含；下标会自动收敛到合法范围。
     * </p>
     *
     * @param iterable  Iterable 对象
     * @param fromIndex 开始下标，包含
     * @param toIndex   结束下标，不包含
     * @param <T>       元素类型
     * @return 截取后的可变 ArrayList
     */
    public static <T> List<T> slice(Iterable<? extends T> iterable, int fromIndex, int toIndex) {
        if (iterable == null) {
            return new ArrayList<>();
        }
        if (iterable instanceof List<? extends T> list) {
            return slice(list, fromIndex, toIndex);
        }
        return slice(toList(iterable), fromIndex, toIndex);
    }

    /**
     * 截取 Iterator 指定区间。
     *
     * <p>
     * fromIndex 包含，toIndex 不包含；该方法会消费 Iterator。
     * </p>
     *
     * @param iterator  Iterator 对象
     * @param fromIndex 开始下标，包含
     * @param toIndex   结束下标，不包含
     * @param <T>       元素类型
     * @return 截取后的可变 ArrayList
     */
    public static <T> List<T> slice(Iterator<? extends T> iterator, int fromIndex, int toIndex) {
        List<T> result = new ArrayList<>();
        if (iterator == null) {
            return result;
        }

        int start = Math.max(0, fromIndex);
        int end = Math.max(start, toIndex);
        int index = 0;

        while (iterator.hasNext()) {
            T item = iterator.next();
            if (index >= start && index < end) {
                result.add(item);
            }
            if (index >= end) {
                break;
            }
            index++;
        }
        return result;
    }

    /**
     * 截取数组指定区间。
     *
     * <p>
     * fromIndex 包含，toIndex 不包含；下标会自动收敛到合法范围。
     * </p>
     *
     * @param array     数组
     * @param fromIndex 开始下标，包含
     * @param toIndex   结束下标，不包含
     * @param <T>       元素类型
     * @return 截取后的可变 ArrayList
     */
    public static <T> List<T> slice(T[] array, int fromIndex, int toIndex) {
        if (array == null || array.length == 0) {
            return new ArrayList<>();
        }

        int size = array.length;
        int start = Math.max(0, Math.min(fromIndex, size));
        int end = Math.max(start, Math.min(toIndex, size));

        List<T> result = new ArrayList<>(end - start);
        for (int index = start; index < end; index++) {
            result.add(array[index]);
        }
        return result;
    }

    /**
     * 截取 Collection 前 limit 个元素。
     *
     * @param collection Collection 对象
     * @param limit      返回数量
     * @param <T>        元素类型
     * @return 截取后的可变 ArrayList
     */
    public static <T> List<T> limit(Collection<? extends T> collection, int limit) {
        checkExpectedSize(limit);

        if (collection == null || collection.isEmpty() || limit == 0) {
            return new ArrayList<>();
        }
        return slice(collection, 0, limit);
    }

    /**
     * 截取 List 前 limit 个元素。
     *
     * @param list  List 对象
     * @param limit 返回数量
     * @param <T>   元素类型
     * @return 截取后的可变 ArrayList
     */
    public static <T> List<T> limit(List<? extends T> list, int limit) {
        checkExpectedSize(limit);

        if (list == null || list.isEmpty() || limit == 0) {
            return new ArrayList<>();
        }
        return slice(list, 0, limit);
    }

    /**
     * 截取 Iterable 前 limit 个元素。
     *
     * @param iterable Iterable 对象
     * @param limit    返回数量
     * @param <T>      元素类型
     * @return 截取后的可变 ArrayList
     */
    public static <T> List<T> limit(Iterable<? extends T> iterable, int limit) {
        checkExpectedSize(limit);

        if (iterable == null || limit == 0) {
            return new ArrayList<>();
        }
        return slice(iterable, 0, limit);
    }

    /**
     * 截取 Iterator 前 limit 个元素。
     *
     * <p>
     * 该方法会消费 Iterator，最多消费 limit 个元素。
     * </p>
     *
     * @param iterator Iterator 对象
     * @param limit    返回数量
     * @param <T>      元素类型
     * @return 截取后的可变 ArrayList
     */
    public static <T> List<T> limit(Iterator<? extends T> iterator, int limit) {
        checkExpectedSize(limit);

        List<T> result = new ArrayList<>();
        if (iterator == null || limit == 0) {
            return result;
        }

        int count = 0;
        while (iterator.hasNext() && count < limit) {
            result.add(iterator.next());
            count++;
        }
        return result;
    }

    /**
     * 截取数组前 limit 个元素。
     *
     * @param array 数组
     * @param limit 返回数量
     * @param <T>   元素类型
     * @return 截取后的可变 ArrayList
     */
    public static <T> List<T> limit(T[] array, int limit) {
        checkExpectedSize(limit);

        if (array == null || array.length == 0 || limit == 0) {
            return new ArrayList<>();
        }
        return slice(array, 0, limit);
    }

    /**
     * 从 offset 开始截取最多 limit 个 Collection 元素。
     *
     * @param collection Collection 对象
     * @param offset     偏移量，从 0 开始
     * @param limit      返回数量
     * @param <T>        元素类型
     * @return 截取后的可变 ArrayList
     */
    public static <T> List<T> limit(Collection<? extends T> collection, int offset, int limit) {
        checkOffsetLimit(offset, limit);

        if (collection == null || collection.isEmpty() || limit == 0) {
            return new ArrayList<>();
        }
        return slice(collection, offset, offset + limit);
    }

    /**
     * 从 offset 开始截取最多 limit 个 List 元素。
     *
     * @param list   List 对象
     * @param offset 偏移量，从 0 开始
     * @param limit  返回数量
     * @param <T>    元素类型
     * @return 截取后的可变 ArrayList
     */
    public static <T> List<T> limit(List<? extends T> list, int offset, int limit) {
        checkOffsetLimit(offset, limit);

        if (list == null || list.isEmpty() || limit == 0) {
            return new ArrayList<>();
        }
        return slice(list, offset, offset + limit);
    }

    /**
     * 从 offset 开始截取最多 limit 个 Iterable 元素。
     *
     * @param iterable Iterable 对象
     * @param offset   偏移量，从 0 开始
     * @param limit    返回数量
     * @param <T>      元素类型
     * @return 截取后的可变 ArrayList
     */
    public static <T> List<T> limit(Iterable<? extends T> iterable, int offset, int limit) {
        checkOffsetLimit(offset, limit);

        if (iterable == null || limit == 0) {
            return new ArrayList<>();
        }
        return slice(iterable, offset, offset + limit);
    }

    /**
     * 从 offset 开始截取最多 limit 个 Iterator 元素。
     *
     * <p>
     * 该方法会消费 Iterator，直到截取完成或遍历结束。
     * </p>
     *
     * @param iterator Iterator 对象
     * @param offset   偏移量，从 0 开始
     * @param limit    返回数量
     * @param <T>      元素类型
     * @return 截取后的可变 ArrayList
     */
    public static <T> List<T> limit(Iterator<? extends T> iterator, int offset, int limit) {
        checkOffsetLimit(offset, limit);

        if (iterator == null || limit == 0) {
            return new ArrayList<>();
        }
        return slice(iterator, offset, offset + limit);
    }

    /**
     * 从 offset 开始截取最多 limit 个数组元素。
     *
     * @param array  数组
     * @param offset 偏移量，从 0 开始
     * @param limit  返回数量
     * @param <T>    元素类型
     * @return 截取后的可变 ArrayList
     */
    public static <T> List<T> limit(T[] array, int offset, int limit) {
        checkOffsetLimit(offset, limit);

        if (array == null || array.length == 0 || limit == 0) {
            return new ArrayList<>();
        }
        return slice(array, offset, offset + limit);
    }

    /**
     * 跳过 Collection 前 skip 个元素。
     *
     * @param collection Collection 对象
     * @param skip       跳过数量
     * @param <T>        元素类型
     * @return 跳过后的可变 ArrayList
     */
    public static <T> List<T> skip(Collection<? extends T> collection, int skip) {
        checkExpectedSize(skip);

        if (collection == null || collection.isEmpty()) {
            return new ArrayList<>();
        }
        return slice(collection, skip, collection.size());
    }

    /**
     * 跳过 List 前 skip 个元素。
     *
     * @param list List 对象
     * @param skip 跳过数量
     * @param <T>  元素类型
     * @return 跳过后的可变 ArrayList
     */
    public static <T> List<T> skip(List<? extends T> list, int skip) {
        checkExpectedSize(skip);

        if (list == null || list.isEmpty()) {
            return new ArrayList<>();
        }
        return slice(list, skip, list.size());
    }

    /**
     * 跳过 Iterable 前 skip 个元素。
     *
     * @param iterable Iterable 对象
     * @param skip     跳过数量
     * @param <T>      元素类型
     * @return 跳过后的可变 ArrayList
     */
    public static <T> List<T> skip(Iterable<? extends T> iterable, int skip) {
        checkExpectedSize(skip);

        if (iterable == null) {
            return new ArrayList<>();
        }

        List<T> list = toList(iterable);
        return slice(list, skip, list.size());
    }

    /**
     * 跳过 Iterator 前 skip 个元素。
     *
     * <p>
     * 该方法会消费 Iterator。
     * </p>
     *
     * @param iterator Iterator 对象
     * @param skip     跳过数量
     * @param <T>      元素类型
     * @return 跳过后的可变 ArrayList
     */
    public static <T> List<T> skip(Iterator<? extends T> iterator, int skip) {
        checkExpectedSize(skip);

        List<T> result = new ArrayList<>();
        if (iterator == null) {
            return result;
        }

        int index = 0;
        while (iterator.hasNext()) {
            T item = iterator.next();
            if (index >= skip) {
                result.add(item);
            }
            index++;
        }
        return result;
    }

    /**
     * 跳过数组前 skip 个元素。
     *
     * @param array 数组
     * @param skip  跳过数量
     * @param <T>   元素类型
     * @return 跳过后的可变 ArrayList
     */
    public static <T> List<T> skip(T[] array, int skip) {
        checkExpectedSize(skip);

        if (array == null || array.length == 0) {
            return new ArrayList<>();
        }
        return slice(array, skip, array.length);
    }

    /**
     * 获取 Collection 前 count 个元素。
     *
     * @param collection Collection 对象
     * @param count      数量
     * @param <T>        元素类型
     * @return 前 count 个元素
     */
    public static <T> List<T> head(Collection<? extends T> collection, int count) {
        return limit(collection, count);
    }

    /**
     * 获取 List 前 count 个元素。
     *
     * @param list  List 对象
     * @param count 数量
     * @param <T>   元素类型
     * @return 前 count 个元素
     */
    public static <T> List<T> head(List<? extends T> list, int count) {
        return limit(list, count);
    }

    /**
     * 获取 Iterable 前 count 个元素。
     *
     * @param iterable Iterable 对象
     * @param count    数量
     * @param <T>      元素类型
     * @return 前 count 个元素
     */
    public static <T> List<T> head(Iterable<? extends T> iterable, int count) {
        return limit(iterable, count);
    }

    /**
     * 获取数组前 count 个元素。
     *
     * @param array 数组
     * @param count 数量
     * @param <T>   元素类型
     * @return 前 count 个元素
     */
    public static <T> List<T> head(T[] array, int count) {
        return limit(array, count);
    }

    /**
     * 获取 Collection 后 count 个元素。
     *
     * @param collection Collection 对象
     * @param count      数量
     * @param <T>        元素类型
     * @return 后 count 个元素
     */
    public static <T> List<T> tail(Collection<? extends T> collection, int count) {
        checkExpectedSize(count);

        if (collection == null || collection.isEmpty() || count == 0) {
            return new ArrayList<>();
        }

        int size = collection.size();
        return slice(collection, Math.max(0, size - count), size);
    }

    /**
     * 获取 List 后 count 个元素。
     *
     * @param list  List 对象
     * @param count 数量
     * @param <T>   元素类型
     * @return 后 count 个元素
     */
    public static <T> List<T> tail(List<? extends T> list, int count) {
        checkExpectedSize(count);

        if (list == null || list.isEmpty() || count == 0) {
            return new ArrayList<>();
        }

        int size = list.size();
        return slice(list, Math.max(0, size - count), size);
    }

    /**
     * 获取 Iterable 后 count 个元素。
     *
     * @param iterable Iterable 对象
     * @param count    数量
     * @param <T>      元素类型
     * @return 后 count 个元素
     */
    public static <T> List<T> tail(Iterable<? extends T> iterable, int count) {
        checkExpectedSize(count);

        if (iterable == null || count == 0) {
            return new ArrayList<>();
        }

        List<T> list = toList(iterable);
        return tail(list, count);
    }

    /**
     * 获取数组后 count 个元素。
     *
     * @param array 数组
     * @param count 数量
     * @param <T>   元素类型
     * @return 后 count 个元素
     */
    public static <T> List<T> tail(T[] array, int count) {
        checkExpectedSize(count);

        if (array == null || array.length == 0 || count == 0) {
            return new ArrayList<>();
        }

        int size = array.length;
        return slice(array, Math.max(0, size - count), size);
    }

    /**
     * 从 List 中间截取一段窗口。
     *
     * <p>
     * centerIndex 表示窗口中心点；windowSize 表示窗口大小。
     * </p>
     *
     * @param list        List 对象
     * @param centerIndex 中心下标
     * @param windowSize  窗口大小
     * @param <T>         元素类型
     * @return 窗口元素列表
     */
    public static <T> List<T> window(List<? extends T> list, int centerIndex, int windowSize) {
        checkExpectedSize(windowSize);

        if (list == null || list.isEmpty() || windowSize == 0) {
            return new ArrayList<>();
        }

        int half = windowSize / 2;
        int fromIndex = centerIndex - half;
        int toIndex = fromIndex + windowSize;
        return slice(list, fromIndex, toIndex);
    }

    /**
     * 从 Collection 中间截取一段窗口。
     *
     * <p>
     * centerIndex 表示窗口中心点；windowSize 表示窗口大小。
     * </p>
     *
     * @param collection  Collection 对象
     * @param centerIndex 中心下标
     * @param windowSize  窗口大小
     * @param <T>         元素类型
     * @return 窗口元素列表
     */
    public static <T> List<T> window(Collection<? extends T> collection, int centerIndex, int windowSize) {
        if (collection == null || collection.isEmpty()) {
            return new ArrayList<>();
        }
        return window(toList(collection), centerIndex, windowSize);
    }

    /**
     * 从数组中间截取一段窗口。
     *
     * <p>
     * centerIndex 表示窗口中心点；windowSize 表示窗口大小。
     * </p>
     *
     * @param array       数组
     * @param centerIndex 中心下标
     * @param windowSize  窗口大小
     * @param <T>         元素类型
     * @return 窗口元素列表
     */
    public static <T> List<T> window(T[] array, int centerIndex, int windowSize) {
        checkExpectedSize(windowSize);

        if (array == null || array.length == 0 || windowSize == 0) {
            return new ArrayList<>();
        }

        int half = windowSize / 2;
        int fromIndex = centerIndex - half;
        int toIndex = fromIndex + windowSize;
        return slice(array, fromIndex, toIndex);
    }

    /**
     * 计算分页偏移量。
     *
     * <p>
     * pageNum 从 1 开始。
     * </p>
     *
     * @param pageNum  页码
     * @param pageSize 每页数量
     * @return 偏移量
     */
    public static int pageOffset(int pageNum, int pageSize) {
        checkPageParam(pageNum, pageSize);
        return Math.multiplyExact(pageNum - 1, pageSize);
    }

    /**
     * 计算总页数。
     *
     * @param total    总数量
     * @param pageSize 每页数量
     * @return 总页数
     */
    public static int pageCount(int total, int pageSize) {
        checkExpectedSize(total);
        if (pageSize <= 0) {
            throw new IllegalArgumentException("pageSize 必须大于 0");
        }
        if (total == 0) {
            return 0;
        }
        return (total + pageSize - 1) / pageSize;
    }

    /**
     * 判断是否存在上一页。
     *
     * @param pageNum 当前页码，从 1 开始
     * @return true 表示存在上一页
     */
    public static boolean hasPreviousPage(int pageNum) {
        if (pageNum < 1) {
            throw new IllegalArgumentException("pageNum 必须大于等于 1");
        }
        return pageNum > 1;
    }

    /**
     * 判断是否存在下一页。
     *
     * @param total    总数量
     * @param pageNum  当前页码，从 1 开始
     * @param pageSize 每页数量
     * @return true 表示存在下一页
     */
    public static boolean hasNextPage(int total, int pageNum, int pageSize) {
        int pageCount = pageCount(total, pageSize);
        if (pageNum < 1) {
            throw new IllegalArgumentException("pageNum 必须大于等于 1");
        }
        return pageNum < pageCount;
    }

    /**
     * 判断页码是否在有效范围内。
     *
     * @param total    总数量
     * @param pageNum  当前页码，从 1 开始
     * @param pageSize 每页数量
     * @return true 表示页码有效
     */
    public static boolean isValidPage(int total, int pageNum, int pageSize) {
        checkExpectedSize(total);
        if (pageNum < 1 || pageSize <= 0) {
            return false;
        }

        int pageCount = pageCount(total, pageSize);
        return pageCount > 0 && pageNum <= pageCount;
    }

    /**
     * 检查分页参数。
     *
     * @param pageNum  页码
     * @param pageSize 每页数量
     */
    private static void checkPageParam(int pageNum, int pageSize) {
        if (pageNum < 1) {
            throw new IllegalArgumentException("pageNum 必须大于等于 1");
        }
        checkExpectedSize(pageSize);
    }

    /**
     * 检查 offset 和 limit 参数。
     *
     * @param offset 偏移量
     * @param limit  返回数量
     */
    private static void checkOffsetLimit(int offset, int limit) {
        if (offset < 0) {
            throw new IllegalArgumentException("offset 不能小于 0");
        }
        checkExpectedSize(limit);
    }

    /**
     * 将 Collection 按固定大小分批。
     *
     * <p>
     * batchSize 必须大于 0；每个批次都是新的 ArrayList。
     * </p>
     *
     * @param collection Collection 对象
     * @param batchSize  每批大小
     * @param <T>        元素类型
     * @return 分批后的列表
     */
    public static <T> List<List<T>> batch(Collection<? extends T> collection, int batchSize) {
        return split(collection, batchSize);
    }

    /**
     * 将 List 按固定大小分批。
     *
     * <p>
     * batchSize 必须大于 0；每个批次都是新的 ArrayList。
     * </p>
     *
     * @param list      List 对象
     * @param batchSize 每批大小
     * @param <T>       元素类型
     * @return 分批后的列表
     */
    public static <T> List<List<T>> batch(List<? extends T> list, int batchSize) {
        return split(list, batchSize);
    }

    /**
     * 将 Iterable 按固定大小分批。
     *
     * <p>
     * batchSize 必须大于 0；该方法会遍历 Iterable。
     * </p>
     *
     * @param iterable  Iterable 对象
     * @param batchSize 每批大小
     * @param <T>       元素类型
     * @return 分批后的列表
     */
    public static <T> List<List<T>> batch(Iterable<? extends T> iterable, int batchSize) {
        return split(iterable, batchSize);
    }

    /**
     * 将 Iterator 按固定大小分批。
     *
     * <p>
     * batchSize 必须大于 0；该方法会消费 Iterator。
     * </p>
     *
     * @param iterator  Iterator 对象
     * @param batchSize 每批大小
     * @param <T>       元素类型
     * @return 分批后的列表
     */
    public static <T> List<List<T>> batch(Iterator<? extends T> iterator, int batchSize) {
        return split(iterator, batchSize);
    }

    /**
     * 将数组按固定大小分批。
     *
     * <p>
     * batchSize 必须大于 0；每个批次都是新的 ArrayList。
     * </p>
     *
     * @param array     数组
     * @param batchSize 每批大小
     * @param <T>       元素类型
     * @return 分批后的列表
     */
    public static <T> List<List<T>> batch(T[] array, int batchSize) {
        return split(array, batchSize);
    }

    /**
     * 将 Collection 按固定大小切分。
     *
     * <p>
     * batchSize 必须大于 0；每个批次都是新的 ArrayList。
     * </p>
     *
     * @param collection Collection 对象
     * @param batchSize  每批大小
     * @param <T>        元素类型
     * @return 切分后的列表
     */
    public static <T> List<List<T>> split(Collection<? extends T> collection, int batchSize) {
        checkBatchSize(batchSize);

        if (collection == null || collection.isEmpty()) {
            return new ArrayList<>();
        }
        if (collection instanceof List<? extends T> list) {
            return split(list, batchSize);
        }
        return split(toList(collection), batchSize);
    }

    /**
     * 将 List 按固定大小切分。
     *
     * <p>
     * batchSize 必须大于 0；每个批次都是新的 ArrayList。
     * </p>
     *
     * @param list      List 对象
     * @param batchSize 每批大小
     * @param <T>       元素类型
     * @return 切分后的列表
     */
    public static <T> List<List<T>> split(List<? extends T> list, int batchSize) {
        checkBatchSize(batchSize);

        List<List<T>> result = new ArrayList<>();
        if (list == null || list.isEmpty()) {
            return result;
        }

        int size = list.size();
        int batchCount = batchCount(size, batchSize);
        result = new ArrayList<>(batchCount);

        for (int fromIndex = 0; fromIndex < size; fromIndex += batchSize) {
            int toIndex = Math.min(fromIndex + batchSize, size);
            result.add(new ArrayList<>(list.subList(fromIndex, toIndex)));
        }
        return result;
    }

    /**
     * 将 Iterable 按固定大小切分。
     *
     * <p>
     * batchSize 必须大于 0；该方法会遍历 Iterable。
     * </p>
     *
     * @param iterable  Iterable 对象
     * @param batchSize 每批大小
     * @param <T>       元素类型
     * @return 切分后的列表
     */
    public static <T> List<List<T>> split(Iterable<? extends T> iterable, int batchSize) {
        checkBatchSize(batchSize);

        if (iterable == null) {
            return new ArrayList<>();
        }
        if (iterable instanceof Collection<? extends T> collection) {
            return split(collection, batchSize);
        }

        List<List<T>> result = new ArrayList<>();
        List<T> currentBatch = new ArrayList<>(batchSize);

        for (T item : iterable) {
            currentBatch.add(item);
            if (currentBatch.size() == batchSize) {
                result.add(currentBatch);
                currentBatch = new ArrayList<>(batchSize);
            }
        }

        if (!currentBatch.isEmpty()) {
            result.add(currentBatch);
        }
        return result;
    }

    /**
     * 将 Iterator 按固定大小切分。
     *
     * <p>
     * batchSize 必须大于 0；该方法会消费 Iterator。
     * </p>
     *
     * @param iterator  Iterator 对象
     * @param batchSize 每批大小
     * @param <T>       元素类型
     * @return 切分后的列表
     */
    public static <T> List<List<T>> split(Iterator<? extends T> iterator, int batchSize) {
        checkBatchSize(batchSize);

        List<List<T>> result = new ArrayList<>();
        if (iterator == null) {
            return result;
        }

        List<T> currentBatch = new ArrayList<>(batchSize);
        while (iterator.hasNext()) {
            currentBatch.add(iterator.next());
            if (currentBatch.size() == batchSize) {
                result.add(currentBatch);
                currentBatch = new ArrayList<>(batchSize);
            }
        }

        if (!currentBatch.isEmpty()) {
            result.add(currentBatch);
        }
        return result;
    }

    /**
     * 将数组按固定大小切分。
     *
     * <p>
     * batchSize 必须大于 0；每个批次都是新的 ArrayList。
     * </p>
     *
     * @param array     数组
     * @param batchSize 每批大小
     * @param <T>       元素类型
     * @return 切分后的列表
     */
    public static <T> List<List<T>> split(T[] array, int batchSize) {
        checkBatchSize(batchSize);

        List<List<T>> result = new ArrayList<>();
        if (array == null || array.length == 0) {
            return result;
        }

        int batchCount = batchCount(array.length, batchSize);
        result = new ArrayList<>(batchCount);

        for (int fromIndex = 0; fromIndex < array.length; fromIndex += batchSize) {
            int toIndex = Math.min(fromIndex + batchSize, array.length);
            List<T> batch = new ArrayList<>(toIndex - fromIndex);
            for (int index = fromIndex; index < toIndex; index++) {
                batch.add(array[index]);
            }
            result.add(batch);
        }
        return result;
    }

    /**
     * 将 Collection 按固定大小切分，并将每个批次保存为 LinkedHashSet。
     *
     * <p>
     * 每个批次内部会按首次出现顺序去重。
     * </p>
     *
     * @param collection Collection 对象
     * @param batchSize  每批大小
     * @param <T>        元素类型
     * @return 切分后的 Set 批次列表
     */
    public static <T> List<Set<T>> splitToSet(Collection<? extends T> collection, int batchSize) {
        checkBatchSize(batchSize);

        List<Set<T>> result = new ArrayList<>();
        if (collection == null || collection.isEmpty()) {
            return result;
        }

        Set<T> currentBatch = new LinkedHashSet<>(calculateHashMapCapacity(batchSize));
        for (T item : collection) {
            currentBatch.add(item);
            if (currentBatch.size() == batchSize) {
                result.add(currentBatch);
                currentBatch = new LinkedHashSet<>(calculateHashMapCapacity(batchSize));
            }
        }

        if (!currentBatch.isEmpty()) {
            result.add(currentBatch);
        }
        return result;
    }

    /**
     * 将 Iterable 按固定大小切分，并将每个批次保存为 LinkedHashSet。
     *
     * <p>
     * 每个批次内部会按首次出现顺序去重。
     * </p>
     *
     * @param iterable  Iterable 对象
     * @param batchSize 每批大小
     * @param <T>       元素类型
     * @return 切分后的 Set 批次列表
     */
    public static <T> List<Set<T>> splitToSet(Iterable<? extends T> iterable, int batchSize) {
        checkBatchSize(batchSize);

        List<Set<T>> result = new ArrayList<>();
        if (iterable == null) {
            return result;
        }

        Set<T> currentBatch = new LinkedHashSet<>(calculateHashMapCapacity(batchSize));
        for (T item : iterable) {
            currentBatch.add(item);
            if (currentBatch.size() == batchSize) {
                result.add(currentBatch);
                currentBatch = new LinkedHashSet<>(calculateHashMapCapacity(batchSize));
            }
        }

        if (!currentBatch.isEmpty()) {
            result.add(currentBatch);
        }
        return result;
    }

    /**
     * 将数组按固定大小切分，并将每个批次保存为 LinkedHashSet。
     *
     * <p>
     * 每个批次内部会按首次出现顺序去重。
     * </p>
     *
     * @param array     数组
     * @param batchSize 每批大小
     * @param <T>       元素类型
     * @return 切分后的 Set 批次列表
     */
    public static <T> List<Set<T>> splitToSet(T[] array, int batchSize) {
        checkBatchSize(batchSize);

        List<Set<T>> result = new ArrayList<>();
        if (array == null || array.length == 0) {
            return result;
        }

        Set<T> currentBatch = new LinkedHashSet<>(calculateHashMapCapacity(batchSize));
        for (T item : array) {
            currentBatch.add(item);
            if (currentBatch.size() == batchSize) {
                result.add(currentBatch);
                currentBatch = new LinkedHashSet<>(calculateHashMapCapacity(batchSize));
            }
        }

        if (!currentBatch.isEmpty()) {
            result.add(currentBatch);
        }
        return result;
    }

    /**
     * 将 Collection 按固定大小切分，并使用指定集合类型保存每个批次。
     *
     * @param collection Collection 对象
     * @param batchSize  每批大小
     * @param supplier   每批集合创建函数
     * @param <T>        元素类型
     * @param <C>        每批集合类型
     * @return 切分后的批次列表
     */
    public static <T, C extends Collection<T>> List<C> splitTo(Collection<? extends T> collection,
                                                               int batchSize,
                                                               Supplier<C> supplier) {
        Objects.requireNonNull(supplier, "supplier 不能为 null");
        checkBatchSize(batchSize);

        List<C> result = new ArrayList<>();
        if (collection == null || collection.isEmpty()) {
            return result;
        }

        C currentBatch = supplier.get();
        for (T item : collection) {
            currentBatch.add(item);
            if (currentBatch.size() == batchSize) {
                result.add(currentBatch);
                currentBatch = supplier.get();
            }
        }

        if (!currentBatch.isEmpty()) {
            result.add(currentBatch);
        }
        return result;
    }

    /**
     * 将 Iterable 按固定大小切分，并使用指定集合类型保存每个批次。
     *
     * @param iterable  Iterable 对象
     * @param batchSize 每批大小
     * @param supplier  每批集合创建函数
     * @param <T>       元素类型
     * @param <C>       每批集合类型
     * @return 切分后的批次列表
     */
    public static <T, C extends Collection<T>> List<C> splitTo(Iterable<? extends T> iterable,
                                                               int batchSize,
                                                               Supplier<C> supplier) {
        Objects.requireNonNull(supplier, "supplier 不能为 null");
        checkBatchSize(batchSize);

        List<C> result = new ArrayList<>();
        if (iterable == null) {
            return result;
        }

        C currentBatch = supplier.get();
        for (T item : iterable) {
            currentBatch.add(item);
            if (currentBatch.size() == batchSize) {
                result.add(currentBatch);
                currentBatch = supplier.get();
            }
        }

        if (!currentBatch.isEmpty()) {
            result.add(currentBatch);
        }
        return result;
    }

    /**
     * 将 Iterator 按固定大小切分，并使用指定集合类型保存每个批次。
     *
     * <p>
     * 该方法会消费 Iterator。
     * </p>
     *
     * @param iterator  Iterator 对象
     * @param batchSize 每批大小
     * @param supplier  每批集合创建函数
     * @param <T>       元素类型
     * @param <C>       每批集合类型
     * @return 切分后的批次列表
     */
    public static <T, C extends Collection<T>> List<C> splitTo(Iterator<? extends T> iterator,
                                                               int batchSize,
                                                               Supplier<C> supplier) {
        Objects.requireNonNull(supplier, "supplier 不能为 null");
        checkBatchSize(batchSize);

        List<C> result = new ArrayList<>();
        if (iterator == null) {
            return result;
        }

        C currentBatch = supplier.get();
        while (iterator.hasNext()) {
            currentBatch.add(iterator.next());
            if (currentBatch.size() == batchSize) {
                result.add(currentBatch);
                currentBatch = supplier.get();
            }
        }

        if (!currentBatch.isEmpty()) {
            result.add(currentBatch);
        }
        return result;
    }

    /**
     * 将数组按固定大小切分，并使用指定集合类型保存每个批次。
     *
     * @param array     数组
     * @param batchSize 每批大小
     * @param supplier  每批集合创建函数
     * @param <T>       元素类型
     * @param <C>       每批集合类型
     * @return 切分后的批次列表
     */
    public static <T, C extends Collection<T>> List<C> splitTo(T[] array,
                                                               int batchSize,
                                                               Supplier<C> supplier) {
        Objects.requireNonNull(supplier, "supplier 不能为 null");
        checkBatchSize(batchSize);

        List<C> result = new ArrayList<>();
        if (array == null || array.length == 0) {
            return result;
        }

        C currentBatch = supplier.get();
        for (T item : array) {
            currentBatch.add(item);
            if (currentBatch.size() == batchSize) {
                result.add(currentBatch);
                currentBatch = supplier.get();
            }
        }

        if (!currentBatch.isEmpty()) {
            result.add(currentBatch);
        }
        return result;
    }

    /**
     * 按固定大小遍历 Collection 的每个批次。
     *
     * <p>
     * batchConsumer 接收到的是新的 ArrayList。
     * </p>
     *
     * @param collection    Collection 对象
     * @param batchSize     每批大小
     * @param batchConsumer 批次消费函数
     * @param <T>           元素类型
     */
    public static <T> void forEachBatch(Collection<? extends T> collection,
                                        int batchSize,
                                        java.util.function.Consumer<List<T>> batchConsumer) {
        Objects.requireNonNull(batchConsumer, "batchConsumer 不能为 null");
        for (List<T> batch : CollectionUtil.<T>split(collection, batchSize)) {
            batchConsumer.accept(batch);
        }
    }

    /**
     * 按固定大小遍历 Iterable 的每个批次。
     *
     * <p>
     * batchConsumer 接收到的是新的 ArrayList。
     * </p>
     *
     * @param iterable      Iterable 对象
     * @param batchSize     每批大小
     * @param batchConsumer 批次消费函数
     * @param <T>           元素类型
     */
    public static <T> void forEachBatch(Iterable<? extends T> iterable,
                                        int batchSize,
                                        java.util.function.Consumer<List<T>> batchConsumer) {
        Objects.requireNonNull(batchConsumer, "batchConsumer 不能为 null");
        for (List<T> batch : CollectionUtil.<T>split(iterable, batchSize)) {
            batchConsumer.accept(batch);
        }
    }

    /**
     * 按固定大小遍历 Iterator 的每个批次。
     *
     * <p>
     * 该方法会消费 Iterator；batchConsumer 接收到的是新的 ArrayList。
     * </p>
     *
     * @param iterator      Iterator 对象
     * @param batchSize     每批大小
     * @param batchConsumer 批次消费函数
     * @param <T>           元素类型
     */
    public static <T> void forEachBatch(Iterator<? extends T> iterator,
                                        int batchSize,
                                        java.util.function.Consumer<List<T>> batchConsumer) {
        Objects.requireNonNull(batchConsumer, "batchConsumer 不能为 null");
        for (List<T> batch : CollectionUtil.<T>split(iterator, batchSize)) {
            batchConsumer.accept(batch);
        }
    }

    /**
     * 按固定大小遍历数组的每个批次。
     *
     * <p>
     * batchConsumer 接收到的是新的 ArrayList。
     * </p>
     *
     * @param array         数组
     * @param batchSize     每批大小
     * @param batchConsumer 批次消费函数
     * @param <T>           元素类型
     */
    public static <T> void forEachBatch(T[] array,
                                        int batchSize,
                                        java.util.function.Consumer<List<T>> batchConsumer) {
        Objects.requireNonNull(batchConsumer, "batchConsumer 不能为 null");
        for (List<T> batch : split(array, batchSize)) {
            batchConsumer.accept(batch);
        }
    }

    /**
     * 按固定大小遍历 Collection 的每个批次，并传入批次下标。
     *
     * <p>
     * batchIndex 从 0 开始。
     * </p>
     *
     * @param collection    Collection 对象
     * @param batchSize     每批大小
     * @param batchConsumer 批次消费函数，第一个参数为批次下标，第二个参数为批次数据
     * @param <T>           元素类型
     */
    public static <T> void forEachBatchIndexed(Collection<? extends T> collection,
                                               int batchSize,
                                               java.util.function.BiConsumer<Integer, List<T>> batchConsumer) {
        Objects.requireNonNull(batchConsumer, "batchConsumer 不能为 null");

        List<List<T>> batches = split(collection, batchSize);
        for (int index = 0; index < batches.size(); index++) {
            batchConsumer.accept(index, batches.get(index));
        }
    }

    /**
     * 按固定大小遍历 Iterable 的每个批次，并传入批次下标。
     *
     * <p>
     * batchIndex 从 0 开始。
     * </p>
     *
     * @param iterable      Iterable 对象
     * @param batchSize     每批大小
     * @param batchConsumer 批次消费函数，第一个参数为批次下标，第二个参数为批次数据
     * @param <T>           元素类型
     */
    public static <T> void forEachBatchIndexed(Iterable<? extends T> iterable,
                                               int batchSize,
                                               java.util.function.BiConsumer<Integer, List<T>> batchConsumer) {
        Objects.requireNonNull(batchConsumer, "batchConsumer 不能为 null");

        List<List<T>> batches = split(iterable, batchSize);
        for (int index = 0; index < batches.size(); index++) {
            batchConsumer.accept(index, batches.get(index));
        }
    }

    /**
     * 按固定大小遍历数组的每个批次，并传入批次下标。
     *
     * <p>
     * batchIndex 从 0 开始。
     * </p>
     *
     * @param array         数组
     * @param batchSize     每批大小
     * @param batchConsumer 批次消费函数，第一个参数为批次下标，第二个参数为批次数据
     * @param <T>           元素类型
     */
    public static <T> void forEachBatchIndexed(T[] array,
                                               int batchSize,
                                               java.util.function.BiConsumer<Integer, List<T>> batchConsumer) {
        Objects.requireNonNull(batchConsumer, "batchConsumer 不能为 null");

        List<List<T>> batches = split(array, batchSize);
        for (int index = 0; index < batches.size(); index++) {
            batchConsumer.accept(index, batches.get(index));
        }
    }

    /**
     * 分批映射 Collection，并扁平合并每批处理结果。
     *
     * @param collection  Collection 对象
     * @param batchSize   每批大小
     * @param batchMapper 批次映射函数
     * @param <T>         原元素类型
     * @param <R>         目标元素类型
     * @return 合并后的可变 ArrayList
     */
    public static <T, R> List<R> mapBatch(Collection<? extends T> collection,
                                          int batchSize,
                                          Function<List<T>, ? extends Collection<? extends R>> batchMapper) {
        Objects.requireNonNull(batchMapper, "batchMapper 不能为 null");

        List<R> result = new ArrayList<>();
        for (List<T> batch : CollectionUtil.<T>split(collection, batchSize)) {
            Collection<? extends R> mappedBatch = batchMapper.apply(batch);
            if (mappedBatch != null && !mappedBatch.isEmpty()) {
                result.addAll(mappedBatch);
            }
        }
        return result;
    }

    /**
     * 分批映射 Iterable，并扁平合并每批处理结果。
     *
     * @param iterable    Iterable 对象
     * @param batchSize   每批大小
     * @param batchMapper 批次映射函数
     * @param <T>         原元素类型
     * @param <R>         目标元素类型
     * @return 合并后的可变 ArrayList
     */
    public static <T, R> List<R> mapBatch(Iterable<? extends T> iterable,
                                          int batchSize,
                                          Function<List<T>, ? extends Collection<? extends R>> batchMapper) {
        Objects.requireNonNull(batchMapper, "batchMapper 不能为 null");

        List<R> result = new ArrayList<>();
        for (List<T> batch : CollectionUtil.<T>split(iterable, batchSize)) {
            Collection<? extends R> mappedBatch = batchMapper.apply(batch);
            if (mappedBatch != null && !mappedBatch.isEmpty()) {
                result.addAll(mappedBatch);
            }
        }
        return result;
    }

    /**
     * 分批映射 Iterator，并扁平合并每批处理结果。
     *
     * <p>
     * 该方法会消费 Iterator。
     * </p>
     *
     * @param iterator    Iterator 对象
     * @param batchSize   每批大小
     * @param batchMapper 批次映射函数
     * @param <T>         原元素类型
     * @param <R>         目标元素类型
     * @return 合并后的可变 ArrayList
     */
    public static <T, R> List<R> mapBatch(Iterator<? extends T> iterator,
                                          int batchSize,
                                          Function<List<T>, ? extends Collection<? extends R>> batchMapper) {
        Objects.requireNonNull(batchMapper, "batchMapper 不能为 null");

        List<R> result = new ArrayList<>();
        for (List<T> batch : CollectionUtil.<T>split(iterator, batchSize)) {
            Collection<? extends R> mappedBatch = batchMapper.apply(batch);
            if (mappedBatch != null && !mappedBatch.isEmpty()) {
                result.addAll(mappedBatch);
            }
        }
        return result;
    }

    /**
     * 分批映射数组，并扁平合并每批处理结果。
     *
     * @param array       数组
     * @param batchSize   每批大小
     * @param batchMapper 批次映射函数
     * @param <T>         原元素类型
     * @param <R>         目标元素类型
     * @return 合并后的可变 ArrayList
     */
    public static <T, R> List<R> mapBatch(T[] array,
                                          int batchSize,
                                          Function<List<T>, ? extends Collection<? extends R>> batchMapper) {
        Objects.requireNonNull(batchMapper, "batchMapper 不能为 null");

        List<R> result = new ArrayList<>();
        for (List<T> batch : split(array, batchSize)) {
            Collection<? extends R> mappedBatch = batchMapper.apply(batch);
            if (mappedBatch != null && !mappedBatch.isEmpty()) {
                result.addAll(mappedBatch);
            }
        }
        return result;
    }

    /**
     * 分批映射 Collection，每个批次返回一个结果。
     *
     * @param collection  Collection 对象
     * @param batchSize   每批大小
     * @param batchMapper 批次映射函数
     * @param <T>         原元素类型
     * @param <R>         目标元素类型
     * @return 每个批次的映射结果列表
     */
    public static <T, R> List<R> mapEachBatch(Collection<? extends T> collection,
                                              int batchSize,
                                              Function<List<T>, ? extends R> batchMapper) {
        Objects.requireNonNull(batchMapper, "batchMapper 不能为 null");

        List<R> result = new ArrayList<>();
        for (List<T> batch : CollectionUtil.<T>split(collection, batchSize)) {
            result.add(batchMapper.apply(batch));
        }
        return result;
    }

    /**
     * 分批映射 Iterable，每个批次返回一个结果。
     *
     * @param iterable    Iterable 对象
     * @param batchSize   每批大小
     * @param batchMapper 批次映射函数
     * @param <T>         原元素类型
     * @param <R>         目标元素类型
     * @return 每个批次的映射结果列表
     */
    public static <T, R> List<R> mapEachBatch(Iterable<? extends T> iterable,
                                              int batchSize,
                                              Function<List<T>, ? extends R> batchMapper) {
        Objects.requireNonNull(batchMapper, "batchMapper 不能为 null");

        List<R> result = new ArrayList<>();
        for (List<T> batch : CollectionUtil.<T>split(iterable, batchSize)) {
            result.add(batchMapper.apply(batch));
        }
        return result;
    }

    /**
     * 判断 Collection 是否需要分批处理。
     *
     * @param collection Collection 对象
     * @param batchSize  每批大小
     * @return true 表示集合大小大于每批大小
     */
    public static boolean needBatch(Collection<?> collection, int batchSize) {
        checkBatchSize(batchSize);
        return collection != null && collection.size() > batchSize;
    }

    /**
     * 判断数组是否需要分批处理。
     *
     * @param array     数组
     * @param batchSize 每批大小
     * @return true 表示数组长度大于每批大小
     */
    public static boolean needBatch(Object[] array, int batchSize) {
        checkBatchSize(batchSize);
        return array != null && array.length > batchSize;
    }

    /**
     * 计算分批数量。
     *
     * @param total     总数量
     * @param batchSize 每批大小
     * @return 批次数量
     */
    public static int batchCount(int total, int batchSize) {
        checkExpectedSize(total);
        checkBatchSize(batchSize);

        if (total == 0) {
            return 0;
        }
        return (total + batchSize - 1) / batchSize;
    }

    /**
     * 计算 Collection 分批数量。
     *
     * @param collection Collection 对象
     * @param batchSize  每批大小
     * @return 批次数量
     */
    public static int batchCount(Collection<?> collection, int batchSize) {
        checkBatchSize(batchSize);
        return collection == null || collection.isEmpty() ? 0 : batchCount(collection.size(), batchSize);
    }

    /**
     * 计算数组分批数量。
     *
     * @param array     数组
     * @param batchSize 每批大小
     * @return 批次数量
     */
    public static int batchCount(Object[] array, int batchSize) {
        checkBatchSize(batchSize);
        return array == null || array.length == 0 ? 0 : batchCount(array.length, batchSize);
    }

    /**
     * 计算指定元素下标所属的批次下标。
     *
     * <p>
     * 返回值从 0 开始。
     * </p>
     *
     * @param elementIndex 元素下标，从 0 开始
     * @param batchSize    每批大小
     * @return 批次下标
     */
    public static int batchIndex(int elementIndex, int batchSize) {
        checkExpectedSize(elementIndex);
        checkBatchSize(batchSize);
        return elementIndex / batchSize;
    }

    /**
     * 计算指定批次的开始下标。
     *
     * <p>
     * batchIndex 从 0 开始。
     * </p>
     *
     * @param batchIndex 批次下标
     * @param batchSize  每批大小
     * @return 开始下标
     */
    public static int batchStartIndex(int batchIndex, int batchSize) {
        checkExpectedSize(batchIndex);
        checkBatchSize(batchSize);
        return Math.multiplyExact(batchIndex, batchSize);
    }

    /**
     * 计算指定批次的结束下标。
     *
     * <p>
     * 返回值不包含；batchIndex 从 0 开始。
     * </p>
     *
     * @param total      总数量
     * @param batchIndex 批次下标
     * @param batchSize  每批大小
     * @return 结束下标，不包含
     */
    public static int batchEndIndex(int total, int batchIndex, int batchSize) {
        checkExpectedSize(total);
        checkExpectedSize(batchIndex);
        checkBatchSize(batchSize);

        int startIndex = batchStartIndex(batchIndex, batchSize);
        return Math.min(startIndex + batchSize, total);
    }

    /**
     * 检查分批大小。
     *
     * @param batchSize 每批大小
     */
    private static void checkBatchSize(int batchSize) {
        if (batchSize <= 0) {
            throw new IllegalArgumentException("batchSize 必须大于 0");
        }
    }

    /**
     * 求两个 Collection 的并集。
     *
     * <p>
     * 返回 LinkedHashSet，保留元素首次出现顺序。
     * </p>
     *
     * @param first  第一个 Collection
     * @param second 第二个 Collection
     * @param <T>    元素类型
     * @return 并集结果
     */
    public static <T> Set<T> union(Collection<? extends T> first, Collection<? extends T> second) {
        Set<T> result = new LinkedHashSet<>();
        addAll(result, first);
        addAll(result, second);
        return result;
    }

    /**
     * 求多个 Collection 的并集。
     *
     * <p>
     * 返回 LinkedHashSet，按集合传入顺序和元素遍历顺序保留首次出现位置。
     * </p>
     *
     * @param collections Collection 数组
     * @param <T>         元素类型
     * @return 并集结果
     */
    @SafeVarargs
    public static <T> Set<T> union(Collection<? extends T>... collections) {
        Set<T> result = new LinkedHashSet<>();
        if (collections == null || collections.length == 0) {
            return result;
        }

        for (Collection<? extends T> collection : collections) {
            addAll(result, collection);
        }
        return result;
    }

    /**
     * 求 Collection 与数组的并集。
     *
     * <p>
     * 返回 LinkedHashSet，保留元素首次出现顺序。
     * </p>
     *
     * @param collection Collection 对象
     * @param array      数组
     * @param <T>        元素类型
     * @return 并集结果
     */
    public static <T> Set<T> union(Collection<? extends T> collection, T[] array) {
        Set<T> result = new LinkedHashSet<>();
        addAll(result, collection);
        addAll(result, array);
        return result;
    }

    /**
     * 求两个数组的并集。
     *
     * <p>
     * 返回 LinkedHashSet，保留元素首次出现顺序。
     * </p>
     *
     * @param first  第一个数组
     * @param second 第二个数组
     * @param <T>    元素类型
     * @return 并集结果
     */
    public static <T> Set<T> union(T[] first, T[] second) {
        Set<T> result = new LinkedHashSet<>();
        addAll(result, first);
        addAll(result, second);
        return result;
    }

    /**
     * 求两个 Collection 的交集。
     *
     * <p>
     * 返回 LinkedHashSet，结果顺序以第一个 Collection 的遍历顺序为准。
     * </p>
     *
     * @param first  第一个 Collection
     * @param second 第二个 Collection
     * @param <T>    元素类型
     * @return 交集结果
     */
    public static <T> Set<T> intersection(Collection<? extends T> first, Collection<?> second) {
        Set<T> result = new LinkedHashSet<>();
        if (first == null || first.isEmpty() || second == null || second.isEmpty()) {
            return result;
        }

        for (T item : first) {
            if (second.contains(item)) {
                result.add(item);
            }
        }
        return result;
    }

    /**
     * 求多个 Collection 的交集。
     *
     * <p>
     * 返回 LinkedHashSet，结果顺序以第一个 Collection 的遍历顺序为准。
     * 只要存在一个 null 或空集合，结果就是空集合。
     * </p>
     *
     * @param first  第一个 Collection
     * @param others 其他 Collection
     * @param <T>    元素类型
     * @return 交集结果
     */
    @SafeVarargs
    public static <T> Set<T> intersection(Collection<? extends T> first, Collection<?>... others) {
        Set<T> result = new LinkedHashSet<>();
        if (first == null || first.isEmpty() || others == null || others.length == 0) {
            return result;
        }

        for (Collection<?> other : others) {
            if (other == null || other.isEmpty()) {
                return result;
            }
        }

        for (T item : first) {
            boolean containsAll = true;
            for (Collection<?> other : others) {
                if (!other.contains(item)) {
                    containsAll = false;
                    break;
                }
            }
            if (containsAll) {
                result.add(item);
            }
        }
        return result;
    }

    /**
     * 求 Collection 与数组的交集。
     *
     * <p>
     * 返回 LinkedHashSet，结果顺序以 Collection 的遍历顺序为准。
     * </p>
     *
     * @param collection Collection 对象
     * @param array      数组
     * @param <T>        元素类型
     * @return 交集结果
     */
    public static <T> Set<T> intersection(Collection<? extends T> collection, T[] array) {
        if (array == null || array.length == 0) {
            return new LinkedHashSet<>();
        }
        return intersection(collection, toSet(array));
    }

    /**
     * 求两个数组的交集。
     *
     * <p>
     * 返回 LinkedHashSet，结果顺序以第一个数组的元素顺序为准。
     * </p>
     *
     * @param first  第一个数组
     * @param second 第二个数组
     * @param <T>    元素类型
     * @return 交集结果
     */
    public static <T> Set<T> intersection(T[] first, T[] second) {
        if (first == null || first.length == 0 || second == null || second.length == 0) {
            return new LinkedHashSet<>();
        }
        return intersection(toList(first), toSet(second));
    }

    /**
     * 求第一个 Collection 相对于第二个 Collection 的差集。
     *
     * <p>
     * 返回 LinkedHashSet，结果顺序以第一个 Collection 的遍历顺序为准。
     * </p>
     *
     * @param first  第一个 Collection
     * @param second 第二个 Collection
     * @param <T>    元素类型
     * @return 差集结果
     */
    public static <T> Set<T> difference(Collection<? extends T> first, Collection<?> second) {
        Set<T> result = new LinkedHashSet<>();
        if (first == null || first.isEmpty()) {
            return result;
        }
        if (second == null || second.isEmpty()) {
            result.addAll(first);
            return result;
        }

        for (T item : first) {
            if (!second.contains(item)) {
                result.add(item);
            }
        }
        return result;
    }

    /**
     * 求第一个 Collection 相对于多个 Collection 的差集。
     *
     * <p>
     * 返回 LinkedHashSet，结果顺序以第一个 Collection 的遍历顺序为准。
     * </p>
     *
     * @param first  第一个 Collection
     * @param others 需要排除的其他 Collection
     * @param <T>    元素类型
     * @return 差集结果
     */
    @SafeVarargs
    public static <T> Set<T> difference(Collection<? extends T> first, Collection<?>... others) {
        Set<T> result = new LinkedHashSet<>();
        if (first == null || first.isEmpty()) {
            return result;
        }
        if (others == null || others.length == 0) {
            result.addAll(first);
            return result;
        }

        Set<Object> excludeSet = new HashSet<>();
        for (Collection<?> other : others) {
            if (other != null && !other.isEmpty()) {
                excludeSet.addAll(other);
            }
        }

        if (excludeSet.isEmpty()) {
            result.addAll(first);
            return result;
        }

        for (T item : first) {
            if (!excludeSet.contains(item)) {
                result.add(item);
            }
        }
        return result;
    }

    /**
     * 求 Collection 相对于数组的差集。
     *
     * <p>
     * 返回 LinkedHashSet，结果顺序以 Collection 的遍历顺序为准。
     * </p>
     *
     * @param collection Collection 对象
     * @param array      数组
     * @param <T>        元素类型
     * @return 差集结果
     */
    public static <T> Set<T> difference(Collection<? extends T> collection, T[] array) {
        return difference(collection, toSet(array));
    }

    /**
     * 求第一个数组相对于第二个数组的差集。
     *
     * <p>
     * 返回 LinkedHashSet，结果顺序以第一个数组的元素顺序为准。
     * </p>
     *
     * @param first  第一个数组
     * @param second 第二个数组
     * @param <T>    元素类型
     * @return 差集结果
     */
    public static <T> Set<T> difference(T[] first, T[] second) {
        return difference(toList(first), toSet(second));
    }

    /**
     * 求两个 Collection 的对称差集。
     *
     * <p>
     * 对称差集表示只存在于其中一个集合中的元素；返回 LinkedHashSet。
     * </p>
     *
     * @param first  第一个 Collection
     * @param second 第二个 Collection
     * @param <T>    元素类型
     * @return 对称差集结果
     */
    public static <T> Set<T> symmetricDifference(Collection<? extends T> first, Collection<? extends T> second) {
        Set<T> result = new LinkedHashSet<>();
        result.addAll(difference(first, second));
        result.addAll(difference(second, first));
        return result;
    }

    /**
     * 求 Collection 与数组的对称差集。
     *
     * <p>
     * 对称差集表示只存在于其中一个集合中的元素；返回 LinkedHashSet。
     * </p>
     *
     * @param collection Collection 对象
     * @param array      数组
     * @param <T>        元素类型
     * @return 对称差集结果
     */
    public static <T> Set<T> symmetricDifference(Collection<? extends T> collection, T[] array) {
        return symmetricDifference(collection, toList(array));
    }

    /**
     * 求两个数组的对称差集。
     *
     * <p>
     * 对称差集表示只存在于其中一个数组中的元素；返回 LinkedHashSet。
     * </p>
     *
     * @param first  第一个数组
     * @param second 第二个数组
     * @param <T>    元素类型
     * @return 对称差集结果
     */
    public static <T> Set<T> symmetricDifference(T[] first, T[] second) {
        return symmetricDifference(toList(first), toList(second));
    }

    /**
     * 判断两个 Collection 是否存在交集。
     *
     * @param first  第一个 Collection
     * @param second 第二个 Collection
     * @return true 表示存在至少一个相同元素
     */
    public static boolean hasIntersection(Collection<?> first, Collection<?> second) {
        if (first == null || first.isEmpty() || second == null || second.isEmpty()) {
            return false;
        }

        Collection<?> smaller = first.size() <= second.size() ? first : second;
        Collection<?> larger = first.size() <= second.size() ? second : first;

        for (Object item : smaller) {
            if (larger.contains(item)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断 Collection 与数组是否存在交集。
     *
     * @param collection Collection 对象
     * @param array      数组
     * @return true 表示存在至少一个相同元素
     */
    public static boolean hasIntersection(Collection<?> collection, Object[] array) {
        if (array == null || array.length == 0) {
            return false;
        }
        return hasIntersection(collection, toSet(array));
    }

    /**
     * 判断两个数组是否存在交集。
     *
     * @param first  第一个数组
     * @param second 第二个数组
     * @return true 表示存在至少一个相同元素
     */
    public static boolean hasIntersection(Object[] first, Object[] second) {
        if (first == null || first.length == 0 || second == null || second.length == 0) {
            return false;
        }
        return hasIntersection(toSet(first), toSet(second));
    }

    /**
     * 判断两个 Collection 是否无交集。
     *
     * @param first  第一个 Collection
     * @param second 第二个 Collection
     * @return true 表示两个 Collection 没有任何相同元素
     */
    public static boolean disjoint(Collection<?> first, Collection<?> second) {
        return !hasIntersection(first, second);
    }

    /**
     * 判断 Collection 是否包含另一个 Collection 的任意元素。
     *
     * @param source  源 Collection
     * @param targets 目标 Collection
     * @return true 表示至少包含一个目标元素
     */
    public static boolean containsAnyElement(Collection<?> source, Collection<?> targets) {
        return hasIntersection(source, targets);
    }

    /**
     * 判断 Collection 是否包含另一个 Collection 的全部元素。
     *
     * <p>
     * targets 为 null 或空集合时返回 false，避免空目标被误判为业务满足。
     * </p>
     *
     * @param source  源 Collection
     * @param targets 目标 Collection
     * @return true 表示包含全部目标元素
     */
    public static boolean containsAllElements(Collection<?> source, Collection<?> targets) {
        if (source == null || source.isEmpty() || targets == null || targets.isEmpty()) {
            return false;
        }
        return source.containsAll(targets);
    }

    /**
     * 判断 Collection 是否包含数组中的任意元素。
     *
     * @param source  源 Collection
     * @param targets 目标数组
     * @return true 表示至少包含一个目标元素
     */
    public static boolean containsAnyElement(Collection<?> source, Object[] targets) {
        if (targets == null || targets.length == 0) {
            return false;
        }
        return containsAnyElement(source, toSet(targets));
    }

    /**
     * 判断 Collection 是否包含数组中的全部元素。
     *
     * @param source  源 Collection
     * @param targets 目标数组
     * @return true 表示包含全部目标元素
     */
    public static boolean containsAllElements(Collection<?> source, Object[] targets) {
        if (targets == null || targets.length == 0) {
            return false;
        }
        return containsAllElements(source, toSet(targets));
    }

    /**
     * 判断两个 Collection 是否元素完全相同，忽略顺序和重复次数。
     *
     * @param first  第一个 Collection
     * @param second 第二个 Collection
     * @return true 表示两个 Collection 的元素集合完全相同
     */
    public static boolean equalsIgnoreOrder(Collection<?> first, Collection<?> second) {
        if (first == second) {
            return true;
        }
        if (first == null || second == null) {
            return false;
        }

        return new HashSet<>(first).equals(new HashSet<>(second));
    }

    /**
     * 判断两个数组是否元素完全相同，忽略顺序和重复次数。
     *
     * @param first  第一个数组
     * @param second 第二个数组
     * @return true 表示两个数组的元素集合完全相同
     */
    public static boolean equalsIgnoreOrder(Object[] first, Object[] second) {
        if (first == second) {
            return true;
        }
        if (first == null || second == null) {
            return false;
        }

        return equalsIgnoreOrder(toSet(first), toSet(second));
    }

    /**
     * 判断两个 Collection 是否元素完全相同，忽略顺序但不忽略重复次数。
     *
     * <p>
     * 该方法会统计每个元素出现次数。
     * </p>
     *
     * @param first  第一个 Collection
     * @param second 第二个 Collection
     * @return true 表示两个 Collection 元素及出现次数完全相同
     */
    public static boolean equalsIgnoreOrderWithCount(Collection<?> first, Collection<?> second) {
        if (first == second) {
            return true;
        }
        if (first == null || second == null || first.size() != second.size()) {
            return false;
        }

        Map<Object, Long> firstCountMap = elementCount(first);
        Map<Object, Long> secondCountMap = elementCount(second);
        return firstCountMap.equals(secondCountMap);
    }

    /**
     * 判断两个数组是否元素完全相同，忽略顺序但不忽略重复次数。
     *
     * @param first  第一个数组
     * @param second 第二个数组
     * @return true 表示两个数组元素及出现次数完全相同
     */
    public static boolean equalsIgnoreOrderWithCount(Object[] first, Object[] second) {
        if (first == second) {
            return true;
        }
        if (first == null || second == null || first.length != second.length) {
            return false;
        }

        return equalsIgnoreOrderWithCount(toList(first), toList(second));
    }

    /**
     * 统计 Collection 中每个元素出现次数。
     *
     * <p>
     * 返回 LinkedHashMap，保留元素首次出现顺序。
     * </p>
     *
     * @param collection Collection 对象
     * @param <T>        元素类型
     * @return 元素出现次数 Map
     */
    public static <T> Map<T, Long> elementCount(Collection<? extends T> collection) {
        Map<T, Long> result = new LinkedHashMap<>();
        if (collection == null || collection.isEmpty()) {
            return result;
        }

        for (T item : collection) {
            result.merge(item, 1L, Long::sum);
        }
        return result;
    }

    /**
     * 统计 Iterable 中每个元素出现次数。
     *
     * <p>
     * 返回 LinkedHashMap，保留元素首次出现顺序。
     * </p>
     *
     * @param iterable Iterable 对象
     * @param <T>      元素类型
     * @return 元素出现次数 Map
     */
    public static <T> Map<T, Long> elementCount(Iterable<? extends T> iterable) {
        Map<T, Long> result = new LinkedHashMap<>();
        if (iterable == null) {
            return result;
        }

        for (T item : iterable) {
            result.merge(item, 1L, Long::sum);
        }
        return result;
    }

    /**
     * 统计数组中每个元素出现次数。
     *
     * <p>
     * 返回 LinkedHashMap，保留元素首次出现顺序。
     * </p>
     *
     * @param array 数组
     * @param <T>   元素类型
     * @return 元素出现次数 Map
     */
    public static <T> Map<T, Long> elementCount(T[] array) {
        Map<T, Long> result = new LinkedHashMap<>();
        if (array == null || array.length == 0) {
            return result;
        }

        for (T item : array) {
            result.merge(item, 1L, Long::sum);
        }
        return result;
    }

    /**
     * 根据 Key 统计 Collection 中每个 Key 出现次数。
     *
     * <p>
     * 返回 LinkedHashMap，保留 Key 首次出现顺序。
     * </p>
     *
     * @param collection Collection 对象
     * @param keyMapper  Key 映射函数
     * @param <T>        元素类型
     * @param <K>        Key 类型
     * @return Key 出现次数 Map
     */
    public static <T, K> Map<K, Long> keyCount(Collection<? extends T> collection, Function<? super T, ? extends K> keyMapper) {
        Objects.requireNonNull(keyMapper, "keyMapper 不能为 null");

        Map<K, Long> result = new LinkedHashMap<>();
        if (collection == null || collection.isEmpty()) {
            return result;
        }

        for (T item : collection) {
            result.merge(keyMapper.apply(item), 1L, Long::sum);
        }
        return result;
    }

    /**
     * 根据 Key 统计数组中每个 Key 出现次数。
     *
     * <p>
     * 返回 LinkedHashMap，保留 Key 首次出现顺序。
     * </p>
     *
     * @param array     数组
     * @param keyMapper Key 映射函数
     * @param <T>       元素类型
     * @param <K>       Key 类型
     * @return Key 出现次数 Map
     */
    public static <T, K> Map<K, Long> keyCount(T[] array, Function<? super T, ? extends K> keyMapper) {
        Objects.requireNonNull(keyMapper, "keyMapper 不能为 null");

        Map<K, Long> result = new LinkedHashMap<>();
        if (array == null || array.length == 0) {
            return result;
        }

        for (T item : array) {
            result.merge(keyMapper.apply(item), 1L, Long::sum);
        }
        return result;
    }

    /**
     * 判断 first 是否是 second 的子集。
     *
     * <p>
     * first 为 null 或空集合时返回 false，避免空集合被误判为业务满足。
     * </p>
     *
     * @param first  待判断 Collection
     * @param second 目标 Collection
     * @return true 表示 first 是 second 的子集
     */
    public static boolean isSubset(Collection<?> first, Collection<?> second) {
        if (first == null || first.isEmpty() || second == null || second.isEmpty()) {
            return false;
        }
        return second.containsAll(first);
    }

    /**
     * 判断 first 是否是 second 的真子集。
     *
     * @param first  待判断 Collection
     * @param second 目标 Collection
     * @return true 表示 first 是 second 的真子集
     */
    public static boolean isProperSubset(Collection<?> first, Collection<?> second) {
        if (!isSubset(first, second)) {
            return false;
        }
        return new HashSet<>(first).size() < new HashSet<>(second).size();
    }

    /**
     * 判断 first 是否是 second 的超集。
     *
     * <p>
     * second 为 null 或空集合时返回 false，避免空集合被误判为业务满足。
     * </p>
     *
     * @param first  待判断 Collection
     * @param second 目标 Collection
     * @return true 表示 first 是 second 的超集
     */
    public static boolean isSuperset(Collection<?> first, Collection<?> second) {
        return isSubset(second, first);
    }

    /**
     * 判断 first 是否是 second 的真超集。
     *
     * @param first  待判断 Collection
     * @param second 目标 Collection
     * @return true 表示 first 是 second 的真超集
     */
    public static boolean isProperSuperset(Collection<?> first, Collection<?> second) {
        return isProperSubset(second, first);
    }

    /**
     * 从 Collection 中移除另一个 Collection 包含的元素，并返回新 ArrayList。
     *
     * <p>
     * 不修改原 Collection。
     * </p>
     *
     * @param source      源 Collection
     * @param removeItems 需要移除的元素集合
     * @param <T>         元素类型
     * @return 移除后的可变 ArrayList
     */
    public static <T> List<T> removeAllToList(Collection<? extends T> source, Collection<?> removeItems) {
        List<T> result = new ArrayList<>();
        if (source == null || source.isEmpty()) {
            return result;
        }
        if (removeItems == null || removeItems.isEmpty()) {
            result.addAll(source);
            return result;
        }

        for (T item : source) {
            if (!removeItems.contains(item)) {
                result.add(item);
            }
        }
        return result;
    }

    /**
     * 从数组中移除另一个数组包含的元素，并返回新 ArrayList。
     *
     * <p>
     * 不修改原数组。
     * </p>
     *
     * @param source      源数组
     * @param removeItems 需要移除的元素数组
     * @param <T>         元素类型
     * @return 移除后的可变 ArrayList
     */
    public static <T> List<T> removeAllToList(T[] source, T[] removeItems) {
        return removeAllToList(toList(source), toSet(removeItems));
    }

    /**
     * 只保留 Collection 中另一个 Collection 包含的元素，并返回新 ArrayList。
     *
     * <p>
     * 不修改原 Collection。
     * </p>
     *
     * @param source      源 Collection
     * @param retainItems 需要保留的元素集合
     * @param <T>         元素类型
     * @return 保留后的可变 ArrayList
     */
    public static <T> List<T> retainAllToList(Collection<? extends T> source, Collection<?> retainItems) {
        List<T> result = new ArrayList<>();
        if (source == null || source.isEmpty() || retainItems == null || retainItems.isEmpty()) {
            return result;
        }

        for (T item : source) {
            if (retainItems.contains(item)) {
                result.add(item);
            }
        }
        return result;
    }

    /**
     * 只保留数组中另一个数组包含的元素，并返回新 ArrayList。
     *
     * <p>
     * 不修改原数组。
     * </p>
     *
     * @param source      源数组
     * @param retainItems 需要保留的元素数组
     * @param <T>         元素类型
     * @return 保留后的可变 ArrayList
     */
    public static <T> List<T> retainAllToList(T[] source, T[] retainItems) {
        return retainAllToList(toList(source), toSet(retainItems));
    }

    /**
     * null 安全获取 Map 中指定 Key 的 Value。
     *
     * @param map Map 对象
     * @param key Key
     * @param <K> Key 类型
     * @param <V> Value 类型
     * @return Value，Map 为 null 或 Key 不存在时返回 null
     */
    public static <K, V> V get(Map<? extends K, ? extends V> map, K key) {
        return map == null ? null : map.get(key);
    }

    /**
     * null 安全获取 Map 中指定 Key 的 Value，未获取到时返回默认值。
     *
     * <p>
     * 如果 Key 存在但 Value 为 null，也返回默认值。
     * </p>
     *
     * @param map          Map 对象
     * @param key          Key
     * @param defaultValue 默认值
     * @param <K>          Key 类型
     * @param <V>          Value 类型
     * @return Value 或默认值
     */
    public static <K, V> V getOrDefault(Map<? extends K, ? extends V> map, K key, V defaultValue) {
        if (map == null) {
            return defaultValue;
        }

        V value = map.get(key);
        return value == null ? defaultValue : value;
    }

    /**
     * null 安全获取 Map 中指定 Key 的 Value，未获取到时通过 Supplier 创建默认值。
     *
     * <p>
     * 如果 Key 存在但 Value 为 null，也返回 Supplier 创建的默认值。
     * </p>
     *
     * @param map             Map 对象
     * @param key             Key
     * @param defaultSupplier 默认值创建函数
     * @param <K>             Key 类型
     * @param <V>             Value 类型
     * @return Value 或默认值
     */
    public static <K, V> V getOrSupply(Map<? extends K, ? extends V> map, K key, Supplier<? extends V> defaultSupplier) {
        Objects.requireNonNull(defaultSupplier, "defaultSupplier 不能为 null");

        if (map == null) {
            return defaultSupplier.get();
        }

        V value = map.get(key);
        return value == null ? defaultSupplier.get() : value;
    }

    /**
     * 获取 Map 中指定 Key 的 Value，Value 为 null 时抛出异常。
     *
     * @param map Map 对象
     * @param key Key
     * @param <K> Key 类型
     * @param <V> Value 类型
     * @return Value
     */
    public static <K, V> V getRequired(Map<? extends K, ? extends V> map, K key) {
        V value = get(map, key);
        if (value == null) {
            throw new NoSuchElementException("未找到指定 Key 对应的 Value");
        }
        return value;
    }

    /**
     * 获取 Map 中指定 Key 的 Value，并返回 Optional。
     *
     * @param map Map 对象
     * @param key Key
     * @param <K> Key 类型
     * @param <V> Value 类型
     * @return Optional 包装结果
     */
    public static <K, V> Optional<V> getOptional(Map<? extends K, ? extends V> map, K key) {
        return Optional.ofNullable(get(map, key));
    }

    /**
     * 从 Map 中按多个 Key 依次查找第一个非 null Value。
     *
     * @param map  Map 对象
     * @param keys Key 数组
     * @param <K>  Key 类型
     * @param <V>  Value 类型
     * @return 第一个非 null Value，未找到返回 null
     */
    @SafeVarargs
    public static <K, V> V getFirstNotNull(Map<? extends K, ? extends V> map, K... keys) {
        if (map == null || map.isEmpty() || keys == null || keys.length == 0) {
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
     * 从 Map 中按多个 Key 依次查找第一个存在的 Value。
     *
     * <p>
     * 只要 Key 存在就返回对应 Value，即使 Value 为 null。
     * </p>
     *
     * @param map  Map 对象
     * @param keys Key 数组
     * @param <K>  Key 类型
     * @param <V>  Value 类型
     * @return 第一个存在 Key 对应的 Value，未找到返回 null
     */
    @SafeVarargs
    public static <K, V> V getFirstPresent(Map<? extends K, ? extends V> map, K... keys) {
        if (map == null || map.isEmpty() || keys == null || keys.length == 0) {
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
     * 从 Map 中按多个 Key 提取 Value，并返回 ArrayList。
     *
     * <p>
     * 不存在的 Key 会被忽略；存在但 Value 为 null 的 Key 会保留 null。
     * </p>
     *
     * @param map  Map 对象
     * @param keys Key 集合
     * @param <K>  Key 类型
     * @param <V>  Value 类型
     * @return Value 列表
     */
    public static <K, V> List<V> getValues(Map<? extends K, ? extends V> map, Collection<? extends K> keys) {
        List<V> result = new ArrayList<>();
        if (map == null || map.isEmpty() || keys == null || keys.isEmpty()) {
            return result;
        }

        for (K key : keys) {
            if (map.containsKey(key)) {
                result.add(map.get(key));
            }
        }
        return result;
    }

    /**
     * 从 Map 中按多个 Key 提取非 null Value，并返回 ArrayList。
     *
     * @param map  Map 对象
     * @param keys Key 集合
     * @param <K>  Key 类型
     * @param <V>  Value 类型
     * @return 非 null Value 列表
     */
    public static <K, V> List<V> getNotNullValues(Map<? extends K, ? extends V> map, Collection<? extends K> keys) {
        List<V> result = new ArrayList<>();
        if (map == null || map.isEmpty() || keys == null || keys.isEmpty()) {
            return result;
        }

        for (K key : keys) {
            V value = map.get(key);
            if (value != null) {
                result.add(value);
            }
        }
        return result;
    }

    /**
     * 判断 Map 是否包含指定 Key。
     *
     * @param map Map 对象
     * @param key Key
     * @return true 表示包含指定 Key
     */
    public static boolean containsKey(Map<?, ?> map, Object key) {
        return map != null && map.containsKey(key);
    }

    /**
     * 判断 Map 是否不包含指定 Key。
     *
     * @param map Map 对象
     * @param key Key
     * @return true 表示不包含指定 Key
     */
    public static boolean notContainsKey(Map<?, ?> map, Object key) {
        return !containsKey(map, key);
    }

    /**
     * 判断 Map 是否包含指定 Value。
     *
     * @param map   Map 对象
     * @param value Value
     * @return true 表示包含指定 Value
     */
    public static boolean containsValue(Map<?, ?> map, Object value) {
        return map != null && map.containsValue(value);
    }

    /**
     * 判断 Map 是否不包含指定 Value。
     *
     * @param map   Map 对象
     * @param value Value
     * @return true 表示不包含指定 Value
     */
    public static boolean notContainsValue(Map<?, ?> map, Object value) {
        return !containsValue(map, value);
    }

    /**
     * 判断 Map 是否包含任意一个 Key。
     *
     * @param map  Map 对象
     * @param keys Key 集合
     * @return true 表示至少包含一个 Key
     */
    public static boolean containsAnyKey(Map<?, ?> map, Collection<?> keys) {
        if (map == null || map.isEmpty() || keys == null || keys.isEmpty()) {
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
     * 判断 Map 是否包含任意一个 Key。
     *
     * @param map  Map 对象
     * @param keys Key 数组
     * @return true 表示至少包含一个 Key
     */
    public static boolean containsAnyKey(Map<?, ?> map, Object[] keys) {
        if (keys == null || keys.length == 0) {
            return false;
        }
        return containsAnyKey(map, toList(keys));
    }

    /**
     * 判断 Map 是否包含全部 Key。
     *
     * <p>
     * keys 为 null 或空集合时返回 false，避免空目标被误判为业务满足。
     * </p>
     *
     * @param map  Map 对象
     * @param keys Key 集合
     * @return true 表示包含全部 Key
     */
    public static boolean containsAllKeys(Map<?, ?> map, Collection<?> keys) {
        if (map == null || map.isEmpty() || keys == null || keys.isEmpty()) {
            return false;
        }
        return map.keySet().containsAll(keys);
    }

    /**
     * 判断 Map 是否包含全部 Key。
     *
     * @param map  Map 对象
     * @param keys Key 数组
     * @return true 表示包含全部 Key
     */
    public static boolean containsAllKeys(Map<?, ?> map, Object[] keys) {
        if (keys == null || keys.length == 0) {
            return false;
        }
        return containsAllKeys(map, toList(keys));
    }

    /**
     * 判断 Map 是否包含任意一个 Value。
     *
     * @param map    Map 对象
     * @param values Value 集合
     * @return true 表示至少包含一个 Value
     */
    public static boolean containsAnyValue(Map<?, ?> map, Collection<?> values) {
        if (map == null || map.isEmpty() || values == null || values.isEmpty()) {
            return false;
        }

        for (Object value : values) {
            if (map.containsValue(value)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断 Map 是否包含全部 Value。
     *
     * <p>
     * values 为 null 或空集合时返回 false，避免空目标被误判为业务满足。
     * </p>
     *
     * @param map    Map 对象
     * @param values Value 集合
     * @return true 表示包含全部 Value
     */
    public static boolean containsAllValues(Map<?, ?> map, Collection<?> values) {
        if (map == null || map.isEmpty() || values == null || values.isEmpty()) {
            return false;
        }
        return map.values().containsAll(values);
    }

    /**
     * 根据 Key 集合从 Map 中提取子 Map。
     *
     * <p>
     * 返回 LinkedHashMap，顺序以 keys 的遍历顺序为准；不存在的 Key 会被忽略。
     * </p>
     *
     * @param map  Map 对象
     * @param keys Key 集合
     * @param <K>  Key 类型
     * @param <V>  Value 类型
     * @return 子 Map
     */
    public static <K, V> Map<K, V> subMap(Map<? extends K, ? extends V> map, Collection<? extends K> keys) {
        Map<K, V> result = new LinkedHashMap<>();
        if (map == null || map.isEmpty() || keys == null || keys.isEmpty()) {
            return result;
        }

        for (K key : keys) {
            if (map.containsKey(key)) {
                result.put(key, map.get(key));
            }
        }
        return result;
    }

    /**
     * 根据 Key 数组从 Map 中提取子 Map。
     *
     * <p>
     * 返回 LinkedHashMap，顺序以 keys 的元素顺序为准；不存在的 Key 会被忽略。
     * </p>
     *
     * @param map  Map 对象
     * @param keys Key 数组
     * @param <K>  Key 类型
     * @param <V>  Value 类型
     * @return 子 Map
     */
    @SafeVarargs
    public static <K, V> Map<K, V> subMap(Map<? extends K, ? extends V> map, K... keys) {
        return subMap(map, toList(keys));
    }

    /**
     * 根据 Key 集合从 Map 中排除指定 Key。
     *
     * <p>
     * 返回 LinkedHashMap，不修改原 Map。
     * </p>
     *
     * @param map         Map 对象
     * @param excludeKeys 需要排除的 Key 集合
     * @param <K>         Key 类型
     * @param <V>         Value 类型
     * @return 排除后的 Map
     */
    public static <K, V> Map<K, V> excludeKeys(Map<? extends K, ? extends V> map, Collection<?> excludeKeys) {
        Map<K, V> result = new LinkedHashMap<>();
        if (map == null || map.isEmpty()) {
            return result;
        }
        if (excludeKeys == null || excludeKeys.isEmpty()) {
            result.putAll(map);
            return result;
        }

        for (Map.Entry<? extends K, ? extends V> entry : map.entrySet()) {
            if (!excludeKeys.contains(entry.getKey())) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    /**
     * 根据 Key 数组从 Map 中排除指定 Key。
     *
     * <p>
     * 返回 LinkedHashMap，不修改原 Map。
     * </p>
     *
     * @param map         Map 对象
     * @param excludeKeys 需要排除的 Key 数组
     * @param <K>         Key 类型
     * @param <V>         Value 类型
     * @return 排除后的 Map
     */
    public static <K, V> Map<K, V> excludeKeys(Map<? extends K, ? extends V> map, Object[] excludeKeys) {
        return excludeKeys(map, toSet(excludeKeys));
    }

    /**
     * 过滤 Map Entry。
     *
     * <p>
     * 返回 LinkedHashMap，保留原 Map 遍历顺序。
     * </p>
     *
     * @param map       Map 对象
     * @param predicate Entry 判断条件
     * @param <K>       Key 类型
     * @param <V>       Value 类型
     * @return 过滤后的 Map
     */
    public static <K, V> Map<K, V> filterMap(Map<? extends K, ? extends V> map,
                                             Predicate<? super Map.Entry<? extends K, ? extends V>> predicate) {
        Objects.requireNonNull(predicate, "predicate 不能为 null");

        Map<K, V> result = new LinkedHashMap<>();
        if (map == null || map.isEmpty()) {
            return result;
        }

        for (Map.Entry<? extends K, ? extends V> entry : map.entrySet()) {
            if (predicate.test(entry)) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    /**
     * 按 Key 过滤 Map。
     *
     * <p>
     * 返回 LinkedHashMap，保留原 Map 遍历顺序。
     * </p>
     *
     * @param map          Map 对象
     * @param keyPredicate Key 判断条件
     * @param <K>          Key 类型
     * @param <V>          Value 类型
     * @return 过滤后的 Map
     */
    public static <K, V> Map<K, V> filterMapByKey(Map<? extends K, ? extends V> map,
                                                  Predicate<? super K> keyPredicate) {
        Objects.requireNonNull(keyPredicate, "keyPredicate 不能为 null");

        Map<K, V> result = new LinkedHashMap<>();
        if (map == null || map.isEmpty()) {
            return result;
        }

        for (Map.Entry<? extends K, ? extends V> entry : map.entrySet()) {
            if (keyPredicate.test(entry.getKey())) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    /**
     * 按 Value 过滤 Map。
     *
     * <p>
     * 返回 LinkedHashMap，保留原 Map 遍历顺序。
     * </p>
     *
     * @param map            Map 对象
     * @param valuePredicate Value 判断条件
     * @param <K>            Key 类型
     * @param <V>            Value 类型
     * @return 过滤后的 Map
     */
    public static <K, V> Map<K, V> filterMapByValue(Map<? extends K, ? extends V> map,
                                                    Predicate<? super V> valuePredicate) {
        Objects.requireNonNull(valuePredicate, "valuePredicate 不能为 null");

        Map<K, V> result = new LinkedHashMap<>();
        if (map == null || map.isEmpty()) {
            return result;
        }

        for (Map.Entry<? extends K, ? extends V> entry : map.entrySet()) {
            if (valuePredicate.test(entry.getValue())) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    /**
     * 过滤 Map 中 Key 为 null 的 Entry。
     *
     * @param map Map 对象
     * @param <K> Key 类型
     * @param <V> Value 类型
     * @return Key 非 null 的 Map
     */
    public static <K, V> Map<K, V> filterMapKeyNotNull(Map<? extends K, ? extends V> map) {
        return filterMapByKey(map, Objects::nonNull);
    }

    /**
     * 过滤 Map 中 Value 为 null 的 Entry。
     *
     * @param map Map 对象
     * @param <K> Key 类型
     * @param <V> Value 类型
     * @return Value 非 null 的 Map
     */
    public static <K, V> Map<K, V> filterMapValueNotNull(Map<? extends K, ? extends V> map) {
        return filterMapByValue(map, Objects::nonNull);
    }

    /**
     * 过滤 Map 中 Key 和 Value 均非 null 的 Entry。
     *
     * @param map Map 对象
     * @param <K> Key 类型
     * @param <V> Value 类型
     * @return Key 和 Value 均非 null 的 Map
     */
    public static <K, V> Map<K, V> filterMapNotNull(Map<? extends K, ? extends V> map) {
        Map<K, V> result = new LinkedHashMap<>();
        if (map == null || map.isEmpty()) {
            return result;
        }

        for (Map.Entry<? extends K, ? extends V> entry : map.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    /**
     * 反向过滤 Map Entry。
     *
     * <p>
     * 返回 LinkedHashMap，保留原 Map 遍历顺序。
     * </p>
     *
     * @param map       Map 对象
     * @param predicate Entry 排除条件
     * @param <K>       Key 类型
     * @param <V>       Value 类型
     * @return 反向过滤后的 Map
     */
    public static <K, V> Map<K, V> rejectMap(Map<? extends K, ? extends V> map,
                                             Predicate<? super Map.Entry<? extends K, ? extends V>> predicate) {
        Objects.requireNonNull(predicate, "predicate 不能为 null");
        return filterMap(map, predicate.negate());
    }

    /**
     * 反向过滤 Map Key。
     *
     * @param map          Map 对象
     * @param keyPredicate Key 排除条件
     * @param <K>          Key 类型
     * @param <V>          Value 类型
     * @return 反向过滤后的 Map
     */
    public static <K, V> Map<K, V> rejectMapByKey(Map<? extends K, ? extends V> map,
                                                  Predicate<? super K> keyPredicate) {
        Objects.requireNonNull(keyPredicate, "keyPredicate 不能为 null");
        return filterMapByKey(map, keyPredicate.negate());
    }

    /**
     * 反向过滤 Map Value。
     *
     * @param map            Map 对象
     * @param valuePredicate Value 排除条件
     * @param <K>            Key 类型
     * @param <V>            Value 类型
     * @return 反向过滤后的 Map
     */
    public static <K, V> Map<K, V> rejectMapByValue(Map<? extends K, ? extends V> map,
                                                    Predicate<? super V> valuePredicate) {
        Objects.requireNonNull(valuePredicate, "valuePredicate 不能为 null");
        return filterMapByValue(map, valuePredicate.negate());
    }

    /**
     * 查找第一个符合条件的 Entry。
     *
     * <p>
     * 返回不可变 Entry 快照。
     * </p>
     *
     * @param map       Map 对象
     * @param predicate Entry 判断条件
     * @param <K>       Key 类型
     * @param <V>       Value 类型
     * @return 第一个符合条件的 Entry，未找到返回 null
     */
    public static <K, V> Map.Entry<K, V> findFirstEntry(Map<? extends K, ? extends V> map,
                                                        Predicate<? super Map.Entry<? extends K, ? extends V>> predicate) {
        Objects.requireNonNull(predicate, "predicate 不能为 null");

        if (map == null || map.isEmpty()) {
            return null;
        }

        for (Map.Entry<? extends K, ? extends V> entry : map.entrySet()) {
            if (predicate.test(entry)) {
                return Map.entry(entry.getKey(), entry.getValue());
            }
        }
        return null;
    }

    /**
     * 查找第一个符合条件的 Key。
     *
     * @param map          Map 对象
     * @param keyPredicate Key 判断条件
     * @param <K>          Key 类型
     * @return 第一个符合条件的 Key，未找到返回 null
     */
    public static <K> K findFirstKey(Map<? extends K, ?> map, Predicate<? super K> keyPredicate) {
        Objects.requireNonNull(keyPredicate, "keyPredicate 不能为 null");

        if (map == null || map.isEmpty()) {
            return null;
        }

        for (K key : map.keySet()) {
            if (keyPredicate.test(key)) {
                return key;
            }
        }
        return null;
    }

    /**
     * 查找第一个符合条件的 Value。
     *
     * @param map            Map 对象
     * @param valuePredicate Value 判断条件
     * @param <V>            Value 类型
     * @return 第一个符合条件的 Value，未找到返回 null
     */
    public static <V> V findFirstValue(Map<?, ? extends V> map, Predicate<? super V> valuePredicate) {
        Objects.requireNonNull(valuePredicate, "valuePredicate 不能为 null");

        if (map == null || map.isEmpty()) {
            return null;
        }

        for (V value : map.values()) {
            if (valuePredicate.test(value)) {
                return value;
            }
        }
        return null;
    }

    /**
     * 判断 Map 是否存在符合条件的 Entry。
     *
     * @param map       Map 对象
     * @param predicate Entry 判断条件
     * @param <K>       Key 类型
     * @param <V>       Value 类型
     * @return true 表示存在符合条件的 Entry
     */
    public static <K, V> boolean anyEntryMatch(Map<? extends K, ? extends V> map,
                                               Predicate<? super Map.Entry<? extends K, ? extends V>> predicate) {
        return findFirstEntry(map, predicate) != null;
    }

    /**
     * 判断 Map 中所有 Entry 是否都符合条件。
     *
     * <p>
     * 空 Map 返回 false，避免空数据被误判为全部满足。
     * </p>
     *
     * @param map       Map 对象
     * @param predicate Entry 判断条件
     * @param <K>       Key 类型
     * @param <V>       Value 类型
     * @return true 表示 Map 非空且所有 Entry 都符合条件
     */
    public static <K, V> boolean allEntryMatch(Map<? extends K, ? extends V> map,
                                               Predicate<? super Map.Entry<? extends K, ? extends V>> predicate) {
        Objects.requireNonNull(predicate, "predicate 不能为 null");

        if (map == null || map.isEmpty()) {
            return false;
        }

        for (Map.Entry<? extends K, ? extends V> entry : map.entrySet()) {
            if (!predicate.test(entry)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 判断 Map 中所有 Entry 是否都不符合条件。
     *
     * @param map       Map 对象
     * @param predicate Entry 判断条件
     * @param <K>       Key 类型
     * @param <V>       Value 类型
     * @return true 表示不存在符合条件的 Entry
     */
    public static <K, V> boolean noneEntryMatch(Map<? extends K, ? extends V> map,
                                                Predicate<? super Map.Entry<? extends K, ? extends V>> predicate) {
        return !anyEntryMatch(map, predicate);
    }

    /**
     * 将 Map 的 Key 转为 LinkedHashSet。
     *
     * @param map Map 对象
     * @param <K> Key 类型
     * @return Key 集合
     */
    public static <K> Set<K> keySet(Map<? extends K, ?> map) {
        return map == null || map.isEmpty() ? new LinkedHashSet<>() : new LinkedHashSet<>(map.keySet());
    }

    /**
     * 将 Map 的 Value 转为 ArrayList。
     *
     * @param map Map 对象
     * @param <V> Value 类型
     * @return Value 列表
     */
    public static <V> List<V> valueList(Map<?, ? extends V> map) {
        return map == null || map.isEmpty() ? new ArrayList<>() : new ArrayList<>(map.values());
    }

    /**
     * 将 Map 的 Value 转为 LinkedHashSet。
     *
     * @param map Map 对象
     * @param <V> Value 类型
     * @return Value 集合
     */
    public static <V> Set<V> valueSet(Map<?, ? extends V> map) {
        return map == null || map.isEmpty() ? new LinkedHashSet<>() : new LinkedHashSet<>(map.values());
    }

    /**
     * 将 Map 的 Entry 转为 ArrayList。
     *
     * <p>
     * 返回不可变 Entry 快照。
     * </p>
     *
     * @param map Map 对象
     * @param <K> Key 类型
     * @param <V> Value 类型
     * @return Entry 列表
     */
    public static <K, V> List<Map.Entry<K, V>> entryList(Map<? extends K, ? extends V> map) {
        List<Map.Entry<K, V>> result = new ArrayList<>();
        if (map == null || map.isEmpty()) {
            return result;
        }

        for (Map.Entry<? extends K, ? extends V> entry : map.entrySet()) {
            result.add(Map.entry(entry.getKey(), entry.getValue()));
        }
        return result;
    }

    /**
     * 将 Map 的 Entry 转为 LinkedHashSet。
     *
     * <p>
     * 返回不可变 Entry 快照。
     * </p>
     *
     * @param map Map 对象
     * @param <K> Key 类型
     * @param <V> Value 类型
     * @return Entry 集合
     */
    public static <K, V> Set<Map.Entry<K, V>> entrySet(Map<? extends K, ? extends V> map) {
        Set<Map.Entry<K, V>> result = new LinkedHashSet<>();
        if (map == null || map.isEmpty()) {
            return result;
        }

        for (Map.Entry<? extends K, ? extends V> entry : map.entrySet()) {
            result.add(Map.entry(entry.getKey(), entry.getValue()));
        }
        return result;
    }

    /**
     * 反转 Map 的 Key 和 Value。
     *
     * <p>
     * 如果多个 Key 对应同一个 Value，后遍历到的 Key 会覆盖前面的 Key。
     * </p>
     *
     * @param map Map 对象
     * @param <K> 原 Key 类型
     * @param <V> 原 Value 类型
     * @return 反转后的 LinkedHashMap
     */
    public static <K, V> Map<V, K> invertMap(Map<? extends K, ? extends V> map) {
        return invertMap(map, (oldValue, newValue) -> newValue);
    }

    /**
     * 反转 Map 的 Key 和 Value。
     *
     * @param map           Map 对象
     * @param mergeFunction Value 冲突时的原 Key 合并函数
     * @param <K>           原 Key 类型
     * @param <V>           原 Value 类型
     * @return 反转后的 LinkedHashMap
     */
    public static <K, V> Map<V, K> invertMap(Map<? extends K, ? extends V> map, BinaryOperator<K> mergeFunction) {
        Objects.requireNonNull(mergeFunction, "mergeFunction 不能为 null");

        Map<V, K> result = new LinkedHashMap<>();
        if (map == null || map.isEmpty()) {
            return result;
        }

        for (Map.Entry<? extends K, ? extends V> entry : map.entrySet()) {
            result.merge(entry.getValue(), entry.getKey(), mergeFunction);
        }
        return result;
    }

    /**
     * 反转 Map 的 Key 和 Value，并将重复 Value 对应的 Key 收集为 List。
     *
     * @param map Map 对象
     * @param <K> 原 Key 类型
     * @param <V> 原 Value 类型
     * @return 反转后的 LinkedHashMap
     */
    public static <K, V> Map<V, List<K>> invertMapToList(Map<? extends K, ? extends V> map) {
        Map<V, List<K>> result = new LinkedHashMap<>();
        if (map == null || map.isEmpty()) {
            return result;
        }

        for (Map.Entry<? extends K, ? extends V> entry : map.entrySet()) {
            result.computeIfAbsent(entry.getValue(), ignored -> new ArrayList<>()).add(entry.getKey());
        }
        return result;
    }

    /**
     * 将 Map 的 Value 合并到另一个 Map。
     *
     * <p>
     * Key 相同时使用 mergeFunction 合并 Value。
     * </p>
     *
     * @param target        目标 Map
     * @param source        来源 Map
     * @param mergeFunction Value 合并函数
     * @param <K>           Key 类型
     * @param <V>           Value 类型
     * @return true 表示目标 Map 发生变化
     */
    public static <K, V> boolean mergeInto(Map<K, V> target,
                                           Map<? extends K, ? extends V> source,
                                           BinaryOperator<V> mergeFunction) {
        Objects.requireNonNull(mergeFunction, "mergeFunction 不能为 null");

        if (target == null || source == null || source.isEmpty()) {
            return false;
        }

        for (Map.Entry<? extends K, ? extends V> entry : source.entrySet()) {
            target.merge(entry.getKey(), entry.getValue(), mergeFunction);
        }
        return true;
    }

    /**
     * 根据 Key 对 Map 排序。
     *
     * <p>
     * 返回 LinkedHashMap，排序后的 Entry 顺序会被保留。
     * </p>
     *
     * @param map        Map 对象
     * @param comparator Key 比较器
     * @param <K>        Key 类型
     * @param <V>        Value 类型
     * @return 排序后的 LinkedHashMap
     */
    public static <K, V> Map<K, V> sortMapByKey(Map<? extends K, ? extends V> map, Comparator<? super K> comparator) {
        Objects.requireNonNull(comparator, "comparator 不能为 null");

        Map<K, V> result = new LinkedHashMap<>();
        if (map == null || map.isEmpty()) {
            return result;
        }

        List<Map.Entry<? extends K, ? extends V>> entries = new ArrayList<>(map.entrySet());
        entries.sort((left, right) -> comparator.compare(left.getKey(), right.getKey()));

        for (Map.Entry<? extends K, ? extends V> entry : entries) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }

    /**
     * 根据 Value 对 Map 排序。
     *
     * <p>
     * 返回 LinkedHashMap，排序后的 Entry 顺序会被保留。
     * </p>
     *
     * @param map        Map 对象
     * @param comparator Value 比较器
     * @param <K>        Key 类型
     * @param <V>        Value 类型
     * @return 排序后的 LinkedHashMap
     */
    public static <K, V> Map<K, V> sortMapByValue(Map<? extends K, ? extends V> map, Comparator<? super V> comparator) {
        Objects.requireNonNull(comparator, "comparator 不能为 null");

        Map<K, V> result = new LinkedHashMap<>();
        if (map == null || map.isEmpty()) {
            return result;
        }

        List<Map.Entry<? extends K, ? extends V>> entries = new ArrayList<>(map.entrySet());
        entries.sort((left, right) -> comparator.compare(left.getValue(), right.getValue()));

        for (Map.Entry<? extends K, ? extends V> entry : entries) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }

    /**
     * 根据 Key 自然升序排序 Map。
     *
     * <p>
     * null Key 排在最后。
     * </p>
     *
     * @param map Map 对象
     * @param <K> Key 类型
     * @param <V> Value 类型
     * @return 排序后的 LinkedHashMap
     */
    public static <K extends Comparable<? super K>, V> Map<K, V> sortMapByKeyAsc(Map<? extends K, ? extends V> map) {
        return sortMapByKey(map, Comparator.nullsLast(Comparator.naturalOrder()));
    }

    /**
     * 根据 Key 自然降序排序 Map。
     *
     * <p>
     * null Key 排在最后。
     * </p>
     *
     * @param map Map 对象
     * @param <K> Key 类型
     * @param <V> Value 类型
     * @return 排序后的 LinkedHashMap
     */
    public static <K extends Comparable<? super K>, V> Map<K, V> sortMapByKeyDesc(Map<? extends K, ? extends V> map) {
        return sortMapByKey(map, Comparator.nullsLast(Comparator.reverseOrder()));
    }

    /**
     * 根据 Value 自然升序排序 Map。
     *
     * <p>
     * null Value 排在最后。
     * </p>
     *
     * @param map Map 对象
     * @param <K> Key 类型
     * @param <V> Value 类型
     * @return 排序后的 LinkedHashMap
     */
    public static <K, V extends Comparable<? super V>> Map<K, V> sortMapByValueAsc(Map<? extends K, ? extends V> map) {
        return sortMapByValue(map, Comparator.nullsLast(Comparator.naturalOrder()));
    }

    /**
     * 根据 Value 自然降序排序 Map。
     *
     * <p>
     * null Value 排在最后。
     * </p>
     *
     * @param map Map 对象
     * @param <K> Key 类型
     * @param <V> Value 类型
     * @return 排序后的 LinkedHashMap
     */
    public static <K, V extends Comparable<? super V>> Map<K, V> sortMapByValueDesc(Map<? extends K, ? extends V> map) {
        return sortMapByValue(map, Comparator.nullsLast(Comparator.reverseOrder()));
    }

    /**
     * 限制 Map 最大返回 Entry 数量。
     *
     * <p>
     * 返回 LinkedHashMap，保留原 Map 遍历顺序。
     * </p>
     *
     * @param map   Map 对象
     * @param limit 返回数量
     * @param <K>   Key 类型
     * @param <V>   Value 类型
     * @return 截取后的 Map
     */
    public static <K, V> Map<K, V> limitMap(Map<? extends K, ? extends V> map, int limit) {
        checkExpectedSize(limit);

        Map<K, V> result = new LinkedHashMap<>();
        if (map == null || map.isEmpty() || limit == 0) {
            return result;
        }

        int count = 0;
        for (Map.Entry<? extends K, ? extends V> entry : map.entrySet()) {
            if (count >= limit) {
                break;
            }
            result.put(entry.getKey(), entry.getValue());
            count++;
        }
        return result;
    }

    /**
     * 跳过 Map 前 skip 个 Entry。
     *
     * <p>
     * 返回 LinkedHashMap，保留原 Map 遍历顺序。
     * </p>
     *
     * @param map  Map 对象
     * @param skip 跳过数量
     * @param <K>  Key 类型
     * @param <V>  Value 类型
     * @return 跳过后的 Map
     */
    public static <K, V> Map<K, V> skipMap(Map<? extends K, ? extends V> map, int skip) {
        checkExpectedSize(skip);

        Map<K, V> result = new LinkedHashMap<>();
        if (map == null || map.isEmpty()) {
            return result;
        }

        int index = 0;
        for (Map.Entry<? extends K, ? extends V> entry : map.entrySet()) {
            if (index >= skip) {
                result.put(entry.getKey(), entry.getValue());
            }
            index++;
        }
        return result;
    }

    /**
     * 截取 Map 指定区间的 Entry。
     *
     * <p>
     * fromIndex 包含，toIndex 不包含；返回 LinkedHashMap，保留原 Map 遍历顺序。
     * </p>
     *
     * @param map       Map 对象
     * @param fromIndex 开始下标，包含
     * @param toIndex   结束下标，不包含
     * @param <K>       Key 类型
     * @param <V>       Value 类型
     * @return 截取后的 Map
     */
    public static <K, V> Map<K, V> sliceMap(Map<? extends K, ? extends V> map, int fromIndex, int toIndex) {
        Map<K, V> result = new LinkedHashMap<>();
        if (map == null || map.isEmpty()) {
            return result;
        }

        int size = map.size();
        int start = Math.max(0, Math.min(fromIndex, size));
        int end = Math.max(start, Math.min(toIndex, size));

        int index = 0;
        for (Map.Entry<? extends K, ? extends V> entry : map.entrySet()) {
            if (index >= start && index < end) {
                result.put(entry.getKey(), entry.getValue());
            }
            if (index >= end) {
                break;
            }
            index++;
        }
        return result;
    }

    /**
     * 对 Map 进行内存分页。
     *
     * <p>
     * pageNum 从 1 开始；pageSize 为 0 时返回空 Map。
     * </p>
     *
     * @param map      Map 对象
     * @param pageNum  页码，从 1 开始
     * @param pageSize 每页数量
     * @param <K>      Key 类型
     * @param <V>      Value 类型
     * @return 分页后的 Map
     */
    public static <K, V> Map<K, V> pageMap(Map<? extends K, ? extends V> map, int pageNum, int pageSize) {
        checkPageParam(pageNum, pageSize);

        if (map == null || map.isEmpty() || pageSize == 0) {
            return new LinkedHashMap<>();
        }

        int fromIndex = pageOffset(pageNum, pageSize);
        return sliceMap(map, fromIndex, fromIndex + pageSize);
    }

    /**
     * 将 Map 原地移除指定 Key。
     *
     * @param map Map 对象
     * @param key Key
     * @return true 表示成功移除
     */
    public static boolean removeKey(Map<?, ?> map, Object key) {
        if (map == null || !map.containsKey(key)) {
            return false;
        }

        map.remove(key);
        return true;
    }

    /**
     * 将 Map 原地移除多个 Key。
     *
     * @param map  Map 对象
     * @param keys Key 集合
     * @return true 表示 Map 发生变化
     */
    public static boolean removeKeys(Map<?, ?> map, Collection<?> keys) {
        if (map == null || map.isEmpty() || keys == null || keys.isEmpty()) {
            return false;
        }

        boolean changed = false;
        for (Object key : keys) {
            if (map.containsKey(key)) {
                map.remove(key);
                changed = true;
            }
        }
        return changed;
    }

    /**
     * 将 Map 原地移除多个 Key。
     *
     * @param map  Map 对象
     * @param keys Key 数组
     * @return true 表示 Map 发生变化
     */
    public static boolean removeKeys(Map<?, ?> map, Object[] keys) {
        if (keys == null || keys.length == 0) {
            return false;
        }
        return removeKeys(map, toList(keys));
    }

    /**
     * 将 Map 原地移除 Value 为 null 的 Entry。
     *
     * @param map Map 对象
     * @return true 表示 Map 发生变化
     */
    public static boolean removeNullValues(Map<?, ?> map) {
        if (map == null || map.isEmpty()) {
            return false;
        }

        int oldSize = map.size();
        map.values().removeIf(Objects::isNull);
        return map.size() != oldSize;
    }

    /**
     * 将 Map 原地移除符合条件的 Entry。
     *
     * @param map       Map 对象
     * @param predicate Entry 判断条件
     * @param <K>       Key 类型
     * @param <V>       Value 类型
     * @return true 表示 Map 发生变化
     */
    public static <K, V> boolean removeIf(Map<K, V> map, Predicate<? super Map.Entry<K, V>> predicate) {
        Objects.requireNonNull(predicate, "predicate 不能为 null");

        if (map == null || map.isEmpty()) {
            return false;
        }

        int oldSize = map.size();
        map.entrySet().removeIf(predicate);
        return map.size() != oldSize;
    }

    /**
     * 将 Map 原地移除符合条件的 Key。
     *
     * @param map          Map 对象
     * @param keyPredicate Key 判断条件
     * @param <K>          Key 类型
     * @param <V>          Value 类型
     * @return true 表示 Map 发生变化
     */
    public static <K, V> boolean removeIfKey(Map<K, V> map, Predicate<? super K> keyPredicate) {
        Objects.requireNonNull(keyPredicate, "keyPredicate 不能为 null");
        return removeIf(map, entry -> keyPredicate.test(entry.getKey()));
    }

    /**
     * 将 Map 原地移除符合条件的 Value。
     *
     * @param map            Map 对象
     * @param valuePredicate Value 判断条件
     * @param <K>            Key 类型
     * @param <V>            Value 类型
     * @return true 表示 Map 发生变化
     */
    public static <K, V> boolean removeIfValue(Map<K, V> map, Predicate<? super V> valuePredicate) {
        Objects.requireNonNull(valuePredicate, "valuePredicate 不能为 null");
        return removeIf(map, entry -> valuePredicate.test(entry.getValue()));
    }

    /**
     * 将 Map 转为不可变 Map。
     *
     * <p>
     * JDK 原生不可变 Map 不允许 null Key 和 null Value。
     * </p>
     *
     * @param map Map 对象
     * @param <K> Key 类型
     * @param <V> Value 类型
     * @return 不可变 Map
     */
    public static <K, V> Map<K, V> toUnmodifiableMap(Map<? extends K, ? extends V> map) {
        return map == null || map.isEmpty() ? Map.of() : Map.copyOf(map);
    }

    /**
     * 将 Map 转为保序不可变 Map。
     *
     * <p>
     * 返回值不可修改，但会保留原 Map 遍历顺序；允许 null Key 和 null Value。
     * </p>
     *
     * @param map Map 对象
     * @param <K> Key 类型
     * @param <V> Value 类型
     * @return 保序不可变 Map
     */
    public static <K, V> Map<K, V> toOrderedUnmodifiableMap(Map<? extends K, ? extends V> map) {
        Map<K, V> result = new LinkedHashMap<>();
        if (map != null && !map.isEmpty()) {
            result.putAll(map);
        }
        return Collections.unmodifiableMap(result);
    }

    /**
     * 将列表构建为树形结构。
     *
     * <p>
     * rootParentId 表示根节点的 parentId；返回根节点列表，根节点顺序按原列表遍历顺序保留。
     * 子节点使用 ArrayList 保存。
     * </p>
     *
     * @param nodes          节点集合
     * @param rootParentId   根节点 parentId
     * @param idMapper       节点 ID 映射函数
     * @param parentIdMapper 父节点 ID 映射函数
     * @param childrenSetter 子节点写入函数
     * @param <T>            节点类型
     * @param <K>            ID 类型
     * @return 树形根节点列表
     */
    public static <T, K> List<T> buildTree(Collection<? extends T> nodes,
                                           K rootParentId,
                                           Function<? super T, ? extends K> idMapper,
                                           Function<? super T, ? extends K> parentIdMapper,
                                           BiConsumer<? super T, List<T>> childrenSetter) {
        Objects.requireNonNull(idMapper, "idMapper 不能为 null");
        Objects.requireNonNull(parentIdMapper, "parentIdMapper 不能为 null");
        Objects.requireNonNull(childrenSetter, "childrenSetter 不能为 null");

        return buildTree(nodes, node -> Objects.equals(parentIdMapper.apply(node), rootParentId), idMapper, parentIdMapper, childrenSetter);
    }

    /**
     * 将列表构建为树形结构。
     *
     * <p>
     * rootPredicate 用于判断根节点；返回根节点列表，根节点顺序按原列表遍历顺序保留。
     * 子节点使用 ArrayList 保存。
     * </p>
     *
     * @param nodes          节点集合
     * @param rootPredicate  根节点判断条件
     * @param idMapper       节点 ID 映射函数
     * @param parentIdMapper 父节点 ID 映射函数
     * @param childrenSetter 子节点写入函数
     * @param <T>            节点类型
     * @param <K>            ID 类型
     * @return 树形根节点列表
     */
    public static <T, K> List<T> buildTree(Collection<? extends T> nodes,
                                           Predicate<? super T> rootPredicate,
                                           Function<? super T, ? extends K> idMapper,
                                           Function<? super T, ? extends K> parentIdMapper,
                                           BiConsumer<? super T, List<T>> childrenSetter) {
        Objects.requireNonNull(rootPredicate, "rootPredicate 不能为 null");
        Objects.requireNonNull(idMapper, "idMapper 不能为 null");
        Objects.requireNonNull(parentIdMapper, "parentIdMapper 不能为 null");
        Objects.requireNonNull(childrenSetter, "childrenSetter 不能为 null");

        List<T> roots = new ArrayList<>();
        if (nodes == null || nodes.isEmpty()) {
            return roots;
        }

        Map<K, T> nodeMap = new LinkedHashMap<>();
        Map<K, List<T>> childrenMap = new LinkedHashMap<>();

        for (T node : nodes) {
            if (node == null) {
                continue;
            }
            nodeMap.put(idMapper.apply(node), node);
        }

        for (T node : nodes) {
            if (node == null) {
                continue;
            }

            if (rootPredicate.test(node)) {
                roots.add(node);
                continue;
            }

            K parentId = parentIdMapper.apply(node);
            if (nodeMap.containsKey(parentId)) {
                childrenMap.computeIfAbsent(parentId, ignored -> new ArrayList<>()).add(node);
            } else {
                roots.add(node);
            }
        }

        for (Map.Entry<K, T> entry : nodeMap.entrySet()) {
            List<T> children = childrenMap.getOrDefault(entry.getKey(), new ArrayList<>());
            childrenSetter.accept(entry.getValue(), children);
        }

        return roots;
    }

    /**
     * 将列表构建为树形结构，并忽略找不到父节点的孤儿节点。
     *
     * <p>
     * rootParentId 表示根节点的 parentId；孤儿节点不会放入根节点列表。
     * </p>
     *
     * @param nodes          节点集合
     * @param rootParentId   根节点 parentId
     * @param idMapper       节点 ID 映射函数
     * @param parentIdMapper 父节点 ID 映射函数
     * @param childrenSetter 子节点写入函数
     * @param <T>            节点类型
     * @param <K>            ID 类型
     * @return 树形根节点列表
     */
    public static <T, K> List<T> buildTreeIgnoreOrphans(Collection<? extends T> nodes,
                                                        K rootParentId,
                                                        Function<? super T, ? extends K> idMapper,
                                                        Function<? super T, ? extends K> parentIdMapper,
                                                        BiConsumer<? super T, List<T>> childrenSetter) {
        Objects.requireNonNull(idMapper, "idMapper 不能为 null");
        Objects.requireNonNull(parentIdMapper, "parentIdMapper 不能为 null");
        Objects.requireNonNull(childrenSetter, "childrenSetter 不能为 null");

        List<T> roots = new ArrayList<>();
        if (nodes == null || nodes.isEmpty()) {
            return roots;
        }

        Map<K, T> nodeMap = new LinkedHashMap<>();
        Map<K, List<T>> childrenMap = new LinkedHashMap<>();

        for (T node : nodes) {
            if (node != null) {
                nodeMap.put(idMapper.apply(node), node);
            }
        }

        for (T node : nodes) {
            if (node == null) {
                continue;
            }

            K parentId = parentIdMapper.apply(node);
            if (Objects.equals(parentId, rootParentId)) {
                roots.add(node);
            } else if (nodeMap.containsKey(parentId)) {
                childrenMap.computeIfAbsent(parentId, ignored -> new ArrayList<>()).add(node);
            }
        }

        for (Map.Entry<K, T> entry : nodeMap.entrySet()) {
            List<T> children = childrenMap.getOrDefault(entry.getKey(), new ArrayList<>());
            childrenSetter.accept(entry.getValue(), children);
        }

        return roots;
    }

    /**
     * 将列表构建为树形结构，并使用指定集合类型保存子节点。
     *
     * @param nodes            节点集合
     * @param rootParentId     根节点 parentId
     * @param idMapper         节点 ID 映射函数
     * @param parentIdMapper   父节点 ID 映射函数
     * @param childrenSetter   子节点写入函数
     * @param childrenSupplier 子节点集合创建函数
     * @param <T>              节点类型
     * @param <K>              ID 类型
     * @param <C>              子节点集合类型
     * @return 树形根节点列表
     */
    public static <T, K, C extends Collection<T>> List<T> buildTreeTo(Collection<? extends T> nodes,
                                                                      K rootParentId,
                                                                      Function<? super T, ? extends K> idMapper,
                                                                      Function<? super T, ? extends K> parentIdMapper,
                                                                      BiConsumer<? super T, C> childrenSetter,
                                                                      Supplier<C> childrenSupplier) {
        Objects.requireNonNull(idMapper, "idMapper 不能为 null");
        Objects.requireNonNull(parentIdMapper, "parentIdMapper 不能为 null");
        Objects.requireNonNull(childrenSetter, "childrenSetter 不能为 null");
        Objects.requireNonNull(childrenSupplier, "childrenSupplier 不能为 null");

        List<T> roots = new ArrayList<>();
        if (nodes == null || nodes.isEmpty()) {
            return roots;
        }

        Map<K, T> nodeMap = new LinkedHashMap<>();
        Map<K, C> childrenMap = new LinkedHashMap<>();

        for (T node : nodes) {
            if (node != null) {
                nodeMap.put(idMapper.apply(node), node);
            }
        }

        for (T node : nodes) {
            if (node == null) {
                continue;
            }

            K parentId = parentIdMapper.apply(node);
            if (Objects.equals(parentId, rootParentId)) {
                roots.add(node);
            } else if (nodeMap.containsKey(parentId)) {
                childrenMap.computeIfAbsent(parentId, ignored -> childrenSupplier.get()).add(node);
            } else {
                roots.add(node);
            }
        }

        for (Map.Entry<K, T> entry : nodeMap.entrySet()) {
            C children = childrenMap.get(entry.getKey());
            if (children == null) {
                children = childrenSupplier.get();
            }
            childrenSetter.accept(entry.getValue(), children);
        }

        return roots;
    }

    /**
     * 将树形结构前序展开为列表。
     *
     * <p>
     * 先添加当前节点，再添加子节点。
     * </p>
     *
     * @param roots          根节点集合
     * @param childrenGetter 子节点获取函数
     * @param <T>            节点类型
     * @return 展开后的可变 ArrayList
     */
    public static <T> List<T> flattenTree(Collection<? extends T> roots,
                                          Function<? super T, ? extends Collection<? extends T>> childrenGetter) {
        return flattenTreePreOrder(roots, childrenGetter);
    }

    /**
     * 将树形结构前序展开为列表。
     *
     * <p>
     * 先添加当前节点，再添加子节点。
     * </p>
     *
     * @param roots          根节点集合
     * @param childrenGetter 子节点获取函数
     * @param <T>            节点类型
     * @return 展开后的可变 ArrayList
     */
    public static <T> List<T> flattenTreePreOrder(Collection<? extends T> roots,
                                                  Function<? super T, ? extends Collection<? extends T>> childrenGetter) {
        Objects.requireNonNull(childrenGetter, "childrenGetter 不能为 null");

        List<T> result = new ArrayList<>();
        if (roots == null || roots.isEmpty()) {
            return result;
        }

        for (T root : roots) {
            flattenTreePreOrder(root, childrenGetter, result);
        }
        return result;
    }

    /**
     * 将单个树节点前序展开到目标列表。
     *
     * @param node           当前节点
     * @param childrenGetter 子节点获取函数
     * @param result         结果列表
     * @param <T>            节点类型
     */
    private static <T> void flattenTreePreOrder(T node,
                                                Function<? super T, ? extends Collection<? extends T>> childrenGetter,
                                                List<T> result) {
        if (node == null) {
            return;
        }

        result.add(node);
        Collection<? extends T> children = childrenGetter.apply(node);
        if (children == null || children.isEmpty()) {
            return;
        }

        for (T child : children) {
            flattenTreePreOrder(child, childrenGetter, result);
        }
    }

    /**
     * 将树形结构后序展开为列表。
     *
     * <p>
     * 先添加子节点，再添加当前节点。
     * </p>
     *
     * @param roots          根节点集合
     * @param childrenGetter 子节点获取函数
     * @param <T>            节点类型
     * @return 展开后的可变 ArrayList
     */
    public static <T> List<T> flattenTreePostOrder(Collection<? extends T> roots,
                                                   Function<? super T, ? extends Collection<? extends T>> childrenGetter) {
        Objects.requireNonNull(childrenGetter, "childrenGetter 不能为 null");

        List<T> result = new ArrayList<>();
        if (roots == null || roots.isEmpty()) {
            return result;
        }

        for (T root : roots) {
            flattenTreePostOrder(root, childrenGetter, result);
        }
        return result;
    }

    /**
     * 将单个树节点后序展开到目标列表。
     *
     * @param node           当前节点
     * @param childrenGetter 子节点获取函数
     * @param result         结果列表
     * @param <T>            节点类型
     */
    private static <T> void flattenTreePostOrder(T node,
                                                 Function<? super T, ? extends Collection<? extends T>> childrenGetter,
                                                 List<T> result) {
        if (node == null) {
            return;
        }

        Collection<? extends T> children = childrenGetter.apply(node);
        if (children != null && !children.isEmpty()) {
            for (T child : children) {
                flattenTreePostOrder(child, childrenGetter, result);
            }
        }

        result.add(node);
    }

    /**
     * 广度优先展开树形结构。
     *
     * @param roots          根节点集合
     * @param childrenGetter 子节点获取函数
     * @param <T>            节点类型
     * @return 展开后的可变 ArrayList
     */
    public static <T> List<T> flattenTreeBreadthFirst(Collection<? extends T> roots,
                                                      Function<? super T, ? extends Collection<? extends T>> childrenGetter) {
        Objects.requireNonNull(childrenGetter, "childrenGetter 不能为 null");

        List<T> result = new ArrayList<>();
        if (roots == null || roots.isEmpty()) {
            return result;
        }

        ArrayDeque<T> queue = new ArrayDeque<>();
        for (T root : roots) {
            if (root != null) {
                queue.add(root);
            }
        }

        while (!queue.isEmpty()) {
            T node = queue.poll();
            result.add(node);

            Collection<? extends T> children = childrenGetter.apply(node);
            if (children == null || children.isEmpty()) {
                continue;
            }

            for (T child : children) {
                if (child != null) {
                    queue.add(child);
                }
            }
        }
        return result;
    }

    /**
     * 前序遍历树形结构。
     *
     * @param roots          根节点集合
     * @param childrenGetter 子节点获取函数
     * @param consumer       节点消费函数
     * @param <T>            节点类型
     */
    public static <T> void forEachTree(Collection<? extends T> roots,
                                       Function<? super T, ? extends Collection<? extends T>> childrenGetter,
                                       Consumer<? super T> consumer) {
        Objects.requireNonNull(childrenGetter, "childrenGetter 不能为 null");
        Objects.requireNonNull(consumer, "consumer 不能为 null");

        if (roots == null || roots.isEmpty()) {
            return;
        }

        for (T root : roots) {
            forEachTree(root, childrenGetter, consumer);
        }
    }

    /**
     * 前序遍历单个树节点。
     *
     * @param node           当前节点
     * @param childrenGetter 子节点获取函数
     * @param consumer       节点消费函数
     * @param <T>            节点类型
     */
    private static <T> void forEachTree(T node,
                                        Function<? super T, ? extends Collection<? extends T>> childrenGetter,
                                        Consumer<? super T> consumer) {
        if (node == null) {
            return;
        }

        consumer.accept(node);

        Collection<? extends T> children = childrenGetter.apply(node);
        if (children == null || children.isEmpty()) {
            return;
        }

        for (T child : children) {
            forEachTree(child, childrenGetter, consumer);
        }
    }

    /**
     * 前序遍历树形结构，并传入节点深度。
     *
     * <p>
     * 根节点深度为 0。
     * </p>
     *
     * @param roots          根节点集合
     * @param childrenGetter 子节点获取函数
     * @param consumer       节点消费函数，第一个参数为节点，第二个参数为深度
     * @param <T>            节点类型
     */
    public static <T> void forEachTreeDepth(Collection<? extends T> roots,
                                            Function<? super T, ? extends Collection<? extends T>> childrenGetter,
                                            BiConsumer<? super T, Integer> consumer) {
        Objects.requireNonNull(childrenGetter, "childrenGetter 不能为 null");
        Objects.requireNonNull(consumer, "consumer 不能为 null");

        if (roots == null || roots.isEmpty()) {
            return;
        }

        for (T root : roots) {
            forEachTreeDepth(root, 0, childrenGetter, consumer);
        }
    }

    /**
     * 前序遍历单个树节点，并传入节点深度。
     *
     * @param node           当前节点
     * @param depth          当前深度
     * @param childrenGetter 子节点获取函数
     * @param consumer       节点消费函数
     * @param <T>            节点类型
     */
    private static <T> void forEachTreeDepth(T node,
                                             int depth,
                                             Function<? super T, ? extends Collection<? extends T>> childrenGetter,
                                             BiConsumer<? super T, Integer> consumer) {
        if (node == null) {
            return;
        }

        consumer.accept(node, depth);

        Collection<? extends T> children = childrenGetter.apply(node);
        if (children == null || children.isEmpty()) {
            return;
        }

        for (T child : children) {
            forEachTreeDepth(child, depth + 1, childrenGetter, consumer);
        }
    }

    /**
     * 在树形结构中查找第一个符合条件的节点。
     *
     * <p>
     * 使用前序遍历查找。
     * </p>
     *
     * @param roots          根节点集合
     * @param childrenGetter 子节点获取函数
     * @param predicate      节点判断条件
     * @param <T>            节点类型
     * @return 第一个符合条件的节点，未找到返回 null
     */
    public static <T> T findTreeNode(Collection<? extends T> roots,
                                     Function<? super T, ? extends Collection<? extends T>> childrenGetter,
                                     Predicate<? super T> predicate) {
        Objects.requireNonNull(childrenGetter, "childrenGetter 不能为 null");
        Objects.requireNonNull(predicate, "predicate 不能为 null");

        if (roots == null || roots.isEmpty()) {
            return null;
        }

        for (T root : roots) {
            T found = findTreeNode(root, childrenGetter, predicate);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    /**
     * 在单个树节点中查找第一个符合条件的节点。
     *
     * @param node           当前节点
     * @param childrenGetter 子节点获取函数
     * @param predicate      节点判断条件
     * @param <T>            节点类型
     * @return 第一个符合条件的节点，未找到返回 null
     */
    private static <T> T findTreeNode(T node,
                                      Function<? super T, ? extends Collection<? extends T>> childrenGetter,
                                      Predicate<? super T> predicate) {
        if (node == null) {
            return null;
        }

        if (predicate.test(node)) {
            return node;
        }

        Collection<? extends T> children = childrenGetter.apply(node);
        if (children == null || children.isEmpty()) {
            return null;
        }

        for (T child : children) {
            T found = findTreeNode(child, childrenGetter, predicate);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    /**
     * 在树形结构中查找全部符合条件的节点。
     *
     * @param roots          根节点集合
     * @param childrenGetter 子节点获取函数
     * @param predicate      节点判断条件
     * @param <T>            节点类型
     * @return 符合条件的节点列表
     */
    public static <T> List<T> findTreeNodes(Collection<? extends T> roots,
                                            Function<? super T, ? extends Collection<? extends T>> childrenGetter,
                                            Predicate<? super T> predicate) {
        Objects.requireNonNull(childrenGetter, "childrenGetter 不能为 null");
        Objects.requireNonNull(predicate, "predicate 不能为 null");

        List<T> result = new ArrayList<>();
        if (roots == null || roots.isEmpty()) {
            return result;
        }

        forEachTree(roots, childrenGetter, node -> {
            if (predicate.test(node)) {
                result.add(node);
            }
        });
        return result;
    }

    /**
     * 判断树形结构中是否存在符合条件的节点。
     *
     * @param roots          根节点集合
     * @param childrenGetter 子节点获取函数
     * @param predicate      节点判断条件
     * @param <T>            节点类型
     * @return true 表示存在符合条件的节点
     */
    public static <T> boolean anyTreeNodeMatch(Collection<? extends T> roots,
                                               Function<? super T, ? extends Collection<? extends T>> childrenGetter,
                                               Predicate<? super T> predicate) {
        return findTreeNode(roots, childrenGetter, predicate) != null;
    }

    /**
     * 统计树形结构中的节点数量。
     *
     * @param roots          根节点集合
     * @param childrenGetter 子节点获取函数
     * @param <T>            节点类型
     * @return 节点数量
     */
    public static <T> int countTreeNodes(Collection<? extends T> roots,
                                         Function<? super T, ? extends Collection<? extends T>> childrenGetter) {
        Objects.requireNonNull(childrenGetter, "childrenGetter 不能为 null");

        if (roots == null || roots.isEmpty()) {
            return 0;
        }

        int count = 0;
        for (T root : roots) {
            count += countTreeNodes(root, childrenGetter);
        }
        return count;
    }

    /**
     * 统计单个树节点及其子节点数量。
     *
     * @param node           当前节点
     * @param childrenGetter 子节点获取函数
     * @param <T>            节点类型
     * @return 节点数量
     */
    private static <T> int countTreeNodes(T node,
                                          Function<? super T, ? extends Collection<? extends T>> childrenGetter) {
        if (node == null) {
            return 0;
        }

        int count = 1;
        Collection<? extends T> children = childrenGetter.apply(node);
        if (children == null || children.isEmpty()) {
            return count;
        }

        for (T child : children) {
            count += countTreeNodes(child, childrenGetter);
        }
        return count;
    }

    /**
     * 计算树形结构最大深度。
     *
     * <p>
     * 空树深度为 0；只有根节点时深度为 1。
     * </p>
     *
     * @param roots          根节点集合
     * @param childrenGetter 子节点获取函数
     * @param <T>            节点类型
     * @return 最大深度
     */
    public static <T> int maxTreeDepth(Collection<? extends T> roots,
                                       Function<? super T, ? extends Collection<? extends T>> childrenGetter) {
        Objects.requireNonNull(childrenGetter, "childrenGetter 不能为 null");

        if (roots == null || roots.isEmpty()) {
            return 0;
        }

        int maxDepth = 0;
        for (T root : roots) {
            maxDepth = Math.max(maxDepth, maxTreeDepth(root, childrenGetter));
        }
        return maxDepth;
    }

    /**
     * 计算单个树节点最大深度。
     *
     * @param node           当前节点
     * @param childrenGetter 子节点获取函数
     * @param <T>            节点类型
     * @return 最大深度
     */
    private static <T> int maxTreeDepth(T node,
                                        Function<? super T, ? extends Collection<? extends T>> childrenGetter) {
        if (node == null) {
            return 0;
        }

        Collection<? extends T> children = childrenGetter.apply(node);
        if (children == null || children.isEmpty()) {
            return 1;
        }

        int maxChildDepth = 0;
        for (T child : children) {
            maxChildDepth = Math.max(maxChildDepth, maxTreeDepth(child, childrenGetter));
        }
        return maxChildDepth + 1;
    }

    /**
     * 判断节点是否为叶子节点。
     *
     * @param node           节点
     * @param childrenGetter 子节点获取函数
     * @param <T>            节点类型
     * @return true 表示节点不为 null 且没有子节点
     */
    public static <T> boolean isLeafNode(T node,
                                         Function<? super T, ? extends Collection<? extends T>> childrenGetter) {
        Objects.requireNonNull(childrenGetter, "childrenGetter 不能为 null");

        if (node == null) {
            return false;
        }

        Collection<? extends T> children = childrenGetter.apply(node);
        return children == null || children.isEmpty();
    }

    /**
     * 获取树形结构中的所有叶子节点。
     *
     * @param roots          根节点集合
     * @param childrenGetter 子节点获取函数
     * @param <T>            节点类型
     * @return 叶子节点列表
     */
    public static <T> List<T> leafNodes(Collection<? extends T> roots,
                                        Function<? super T, ? extends Collection<? extends T>> childrenGetter) {
        Objects.requireNonNull(childrenGetter, "childrenGetter 不能为 null");

        List<T> result = new ArrayList<>();
        if (roots == null || roots.isEmpty()) {
            return result;
        }

        forEachTree(roots, childrenGetter, node -> {
            if (isLeafNode(node, childrenGetter)) {
                result.add(node);
            }
        });
        return result;
    }

    /**
     * 获取树形结构中的所有非叶子节点。
     *
     * @param roots          根节点集合
     * @param childrenGetter 子节点获取函数
     * @param <T>            节点类型
     * @return 非叶子节点列表
     */
    public static <T> List<T> nonLeafNodes(Collection<? extends T> roots,
                                           Function<? super T, ? extends Collection<? extends T>> childrenGetter) {
        Objects.requireNonNull(childrenGetter, "childrenGetter 不能为 null");

        List<T> result = new ArrayList<>();
        if (roots == null || roots.isEmpty()) {
            return result;
        }

        forEachTree(roots, childrenGetter, node -> {
            if (!isLeafNode(node, childrenGetter)) {
                result.add(node);
            }
        });
        return result;
    }

    /**
     * 查找从根节点到目标节点的路径。
     *
     * <p>
     * 使用前序深度优先查找；找到后返回路径列表。
     * </p>
     *
     * @param roots          根节点集合
     * @param childrenGetter 子节点获取函数
     * @param predicate      目标节点判断条件
     * @param <T>            节点类型
     * @return 路径列表，未找到返回空列表
     */
    public static <T> List<T> findTreePath(Collection<? extends T> roots,
                                           Function<? super T, ? extends Collection<? extends T>> childrenGetter,
                                           Predicate<? super T> predicate) {
        Objects.requireNonNull(childrenGetter, "childrenGetter 不能为 null");
        Objects.requireNonNull(predicate, "predicate 不能为 null");

        List<T> path = new ArrayList<>();
        if (roots == null || roots.isEmpty()) {
            return path;
        }

        for (T root : roots) {
            if (findTreePath(root, childrenGetter, predicate, path)) {
                return path;
            }
        }

        path.clear();
        return path;
    }

    /**
     * 查找单个树节点到目标节点的路径。
     *
     * @param node           当前节点
     * @param childrenGetter 子节点获取函数
     * @param predicate      目标节点判断条件
     * @param path           当前路径
     * @param <T>            节点类型
     * @return true 表示找到目标节点
     */
    private static <T> boolean findTreePath(T node,
                                            Function<? super T, ? extends Collection<? extends T>> childrenGetter,
                                            Predicate<? super T> predicate,
                                            List<T> path) {
        if (node == null) {
            return false;
        }

        path.add(node);
        if (predicate.test(node)) {
            return true;
        }

        Collection<? extends T> children = childrenGetter.apply(node);
        if (children != null && !children.isEmpty()) {
            for (T child : children) {
                if (findTreePath(child, childrenGetter, predicate, path)) {
                    return true;
                }
            }
        }

        path.removeLast();
        return false;
    }

    /**
     * 获取目标节点的所有后代节点。
     *
     * <p>
     * 不包含当前节点，只包含子孙节点。
     * </p>
     *
     * @param node           当前节点
     * @param childrenGetter 子节点获取函数
     * @param <T>            节点类型
     * @return 后代节点列表
     */
    public static <T> List<T> descendants(T node,
                                          Function<? super T, ? extends Collection<? extends T>> childrenGetter) {
        Objects.requireNonNull(childrenGetter, "childrenGetter 不能为 null");

        List<T> result = new ArrayList<>();
        if (node == null) {
            return result;
        }

        Collection<? extends T> children = childrenGetter.apply(node);
        if (children == null || children.isEmpty()) {
            return result;
        }

        for (T child : children) {
            flattenTreePreOrder(child, childrenGetter, result);
        }
        return result;
    }

    /**
     * 获取目标节点的所有后代节点 ID。
     *
     * <p>
     * 不包含当前节点 ID，只包含子孙节点 ID。
     * </p>
     *
     * @param node           当前节点
     * @param childrenGetter 子节点获取函数
     * @param idMapper       节点 ID 映射函数
     * @param <T>            节点类型
     * @param <K>            ID 类型
     * @return 后代节点 ID 列表
     */
    public static <T, K> List<K> descendantIds(T node,
                                               Function<? super T, ? extends Collection<? extends T>> childrenGetter,
                                               Function<? super T, ? extends K> idMapper) {
        Objects.requireNonNull(idMapper, "idMapper 不能为 null");

        return mapToList(descendants(node, childrenGetter), idMapper);
    }

    /**
     * 根据列表数据获取指定节点的所有祖先节点。
     *
     * <p>
     * 返回顺序为从父节点到根节点。
     * </p>
     *
     * @param nodes          全量节点集合
     * @param node           当前节点
     * @param idMapper       节点 ID 映射函数
     * @param parentIdMapper 父节点 ID 映射函数
     * @param <T>            节点类型
     * @param <K>            ID 类型
     * @return 祖先节点列表
     */
    public static <T, K> List<T> ancestors(Collection<? extends T> nodes,
                                           T node,
                                           Function<? super T, ? extends K> idMapper,
                                           Function<? super T, ? extends K> parentIdMapper) {
        Objects.requireNonNull(idMapper, "idMapper 不能为 null");
        Objects.requireNonNull(parentIdMapper, "parentIdMapper 不能为 null");

        List<T> result = new ArrayList<>();
        if (nodes == null || nodes.isEmpty() || node == null) {
            return result;
        }

        Map<K, T> nodeMap = new LinkedHashMap<>();
        for (T item : nodes) {
            if (item != null) {
                nodeMap.put(idMapper.apply(item), item);
            }
        }

        K parentId = parentIdMapper.apply(node);
        Set<K> visited = new LinkedHashSet<>();

        while (parentId != null && visited.add(parentId)) {
            T parent = nodeMap.get(parentId);
            if (parent == null) {
                break;
            }

            result.add(parent);
            parentId = parentIdMapper.apply(parent);
        }

        return result;
    }

    /**
     * 根据列表数据获取指定节点的所有祖先节点 ID。
     *
     * <p>
     * 返回顺序为从父节点 ID 到根节点 ID。
     * </p>
     *
     * @param nodes          全量节点集合
     * @param node           当前节点
     * @param idMapper       节点 ID 映射函数
     * @param parentIdMapper 父节点 ID 映射函数
     * @param <T>            节点类型
     * @param <K>            ID 类型
     * @return 祖先节点 ID 列表
     */
    public static <T, K> List<K> ancestorIds(Collection<? extends T> nodes,
                                             T node,
                                             Function<? super T, ? extends K> idMapper,
                                             Function<? super T, ? extends K> parentIdMapper) {
        return mapToList(ancestors(nodes, node, idMapper, parentIdMapper), idMapper);
    }

    /**
     * 判断列表结构中指定节点是否为根节点。
     *
     * @param node           当前节点
     * @param rootParentId   根节点 parentId
     * @param parentIdMapper 父节点 ID 映射函数
     * @param <T>            节点类型
     * @param <K>            ID 类型
     * @return true 表示当前节点是根节点
     */
    public static <T, K> boolean isRootNode(T node,
                                            K rootParentId,
                                            Function<? super T, ? extends K> parentIdMapper) {
        Objects.requireNonNull(parentIdMapper, "parentIdMapper 不能为 null");

        return node != null && Objects.equals(parentIdMapper.apply(node), rootParentId);
    }

    /**
     * 判断列表结构中指定节点是否有子节点。
     *
     * @param nodes          全量节点集合
     * @param node           当前节点
     * @param idMapper       节点 ID 映射函数
     * @param parentIdMapper 父节点 ID 映射函数
     * @param <T>            节点类型
     * @param <K>            ID 类型
     * @return true 表示存在子节点
     */
    public static <T, K> boolean hasChildNode(Collection<? extends T> nodes,
                                              T node,
                                              Function<? super T, ? extends K> idMapper,
                                              Function<? super T, ? extends K> parentIdMapper) {
        Objects.requireNonNull(idMapper, "idMapper 不能为 null");
        Objects.requireNonNull(parentIdMapper, "parentIdMapper 不能为 null");

        if (nodes == null || nodes.isEmpty() || node == null) {
            return false;
        }

        K id = idMapper.apply(node);
        for (T item : nodes) {
            if (item != null && Objects.equals(parentIdMapper.apply(item), id)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 根据列表结构获取指定节点的直接子节点。
     *
     * @param nodes          全量节点集合
     * @param node           当前节点
     * @param idMapper       节点 ID 映射函数
     * @param parentIdMapper 父节点 ID 映射函数
     * @param <T>            节点类型
     * @param <K>            ID 类型
     * @return 直接子节点列表
     */
    public static <T, K> List<T> childNodes(Collection<? extends T> nodes,
                                            T node,
                                            Function<? super T, ? extends K> idMapper,
                                            Function<? super T, ? extends K> parentIdMapper) {
        Objects.requireNonNull(idMapper, "idMapper 不能为 null");
        Objects.requireNonNull(parentIdMapper, "parentIdMapper 不能为 null");

        List<T> result = new ArrayList<>();
        if (nodes == null || nodes.isEmpty() || node == null) {
            return result;
        }

        K id = idMapper.apply(node);
        for (T item : nodes) {
            if (item != null && Objects.equals(parentIdMapper.apply(item), id)) {
                result.add(item);
            }
        }
        return result;
    }

    /**
     * 根据列表结构获取指定节点的所有后代节点。
     *
     * <p>
     * 不包含当前节点。
     * </p>
     *
     * @param nodes          全量节点集合
     * @param node           当前节点
     * @param idMapper       节点 ID 映射函数
     * @param parentIdMapper 父节点 ID 映射函数
     * @param <T>            节点类型
     * @param <K>            ID 类型
     * @return 后代节点列表
     */
    public static <T, K> List<T> descendantsFromList(Collection<? extends T> nodes,
                                                     T node,
                                                     Function<? super T, ? extends K> idMapper,
                                                     Function<? super T, ? extends K> parentIdMapper) {
        Objects.requireNonNull(idMapper, "idMapper 不能为 null");
        Objects.requireNonNull(parentIdMapper, "parentIdMapper 不能为 null");

        List<T> result = new ArrayList<>();
        if (nodes == null || nodes.isEmpty() || node == null) {
            return result;
        }

        Map<K, List<T>> childrenMap = groupByToList(nodes, parentIdMapper);
        collectDescendantsFromList(node, idMapper, childrenMap, result);
        return result;
    }

    /**
     * 从列表结构中递归收集后代节点。
     *
     * @param node        当前节点
     * @param idMapper    节点 ID 映射函数
     * @param childrenMap 父节点 ID 与子节点列表映射
     * @param result      结果列表
     * @param <T>         节点类型
     * @param <K>         ID 类型
     */
    private static <T, K> void collectDescendantsFromList(T node,
                                                          Function<? super T, ? extends K> idMapper,
                                                          Map<K, List<T>> childrenMap,
                                                          List<T> result) {
        if (node == null) {
            return;
        }

        K id = idMapper.apply(node);
        List<T> children = childrenMap.get(id);
        if (children == null || children.isEmpty()) {
            return;
        }

        for (T child : children) {
            result.add(child);
            collectDescendantsFromList(child, idMapper, childrenMap, result);
        }
    }

    /**
     * 根据列表结构获取指定节点的所有后代节点 ID。
     *
     * <p>
     * 不包含当前节点 ID。
     * </p>
     *
     * @param nodes          全量节点集合
     * @param node           当前节点
     * @param idMapper       节点 ID 映射函数
     * @param parentIdMapper 父节点 ID 映射函数
     * @param <T>            节点类型
     * @param <K>            ID 类型
     * @return 后代节点 ID 列表
     */
    public static <T, K> List<K> descendantIdsFromList(Collection<? extends T> nodes,
                                                       T node,
                                                       Function<? super T, ? extends K> idMapper,
                                                       Function<? super T, ? extends K> parentIdMapper) {
        return mapToList(descendantsFromList(nodes, node, idMapper, parentIdMapper), idMapper);
    }

    /**
     * 按条件过滤树形结构。
     *
     * <p>
     * 节点自身匹配，或者存在匹配的子节点时，会保留该节点。
     * 该方法会重写节点的子节点集合。
     * </p>
     *
     * @param roots          根节点集合
     * @param childrenGetter 子节点获取函数
     * @param childrenSetter 子节点写入函数
     * @param predicate      节点判断条件
     * @param <T>            节点类型
     * @return 过滤后的根节点列表
     */
    public static <T> List<T> filterTree(Collection<? extends T> roots,
                                         Function<? super T, ? extends Collection<? extends T>> childrenGetter,
                                         BiConsumer<? super T, List<T>> childrenSetter,
                                         Predicate<? super T> predicate) {
        Objects.requireNonNull(childrenGetter, "childrenGetter 不能为 null");
        Objects.requireNonNull(childrenSetter, "childrenSetter 不能为 null");
        Objects.requireNonNull(predicate, "predicate 不能为 null");

        List<T> result = new ArrayList<>();
        if (roots == null || roots.isEmpty()) {
            return result;
        }

        for (T root : roots) {
            T filtered = filterTreeNode(root, childrenGetter, childrenSetter, predicate);
            if (filtered != null) {
                result.add(filtered);
            }
        }
        return result;
    }

    /**
     * 过滤单个树节点。
     *
     * @param node           当前节点
     * @param childrenGetter 子节点获取函数
     * @param childrenSetter 子节点写入函数
     * @param predicate      节点判断条件
     * @param <T>            节点类型
     * @return 保留的节点，不保留时返回 null
     */
    private static <T> T filterTreeNode(T node,
                                        Function<? super T, ? extends Collection<? extends T>> childrenGetter,
                                        BiConsumer<? super T, List<T>> childrenSetter,
                                        Predicate<? super T> predicate) {
        if (node == null) {
            return null;
        }

        List<T> filteredChildren = new ArrayList<>();
        Collection<? extends T> children = childrenGetter.apply(node);
        if (children != null && !children.isEmpty()) {
            for (T child : children) {
                T filteredChild = filterTreeNode(child, childrenGetter, childrenSetter, predicate);
                if (filteredChild != null) {
                    filteredChildren.add(filteredChild);
                }
            }
        }

        boolean matched = predicate.test(node);
        if (matched || !filteredChildren.isEmpty()) {
            childrenSetter.accept(node, filteredChildren);
            return node;
        }

        return null;
    }

    /**
     * 清空树形结构中的空子节点集合。
     *
     * <p>
     * 对没有子节点的节点写入空 ArrayList。
     * </p>
     *
     * @param roots          根节点集合
     * @param childrenGetter 子节点获取函数
     * @param childrenSetter 子节点写入函数
     * @param <T>            节点类型
     */
    public static <T> void normalizeEmptyChildren(Collection<? extends T> roots,
                                                  Function<? super T, ? extends Collection<? extends T>> childrenGetter,
                                                  BiConsumer<? super T, List<T>> childrenSetter) {
        Objects.requireNonNull(childrenGetter, "childrenGetter 不能为 null");
        Objects.requireNonNull(childrenSetter, "childrenSetter 不能为 null");

        if (roots == null || roots.isEmpty()) {
            return;
        }

        for (T root : roots) {
            normalizeEmptyChildren(root, childrenGetter, childrenSetter);
        }
    }

    /**
     * 清空单个树节点中的空子节点集合。
     *
     * @param node           当前节点
     * @param childrenGetter 子节点获取函数
     * @param childrenSetter 子节点写入函数
     * @param <T>            节点类型
     */
    private static <T> void normalizeEmptyChildren(T node,
                                                   Function<? super T, ? extends Collection<? extends T>> childrenGetter,
                                                   BiConsumer<? super T, List<T>> childrenSetter) {
        if (node == null) {
            return;
        }

        Collection<? extends T> children = childrenGetter.apply(node);
        if (children == null || children.isEmpty()) {
            childrenSetter.accept(node, new ArrayList<>());
            return;
        }

        for (T child : children) {
            normalizeEmptyChildren(child, childrenGetter, childrenSetter);
        }
    }

    /**
     * 统计 Collection 元素数量。
     *
     * @param collection Collection 对象
     * @return 元素数量，null 返回 0
     */
    public static long count(Collection<?> collection) {
        return collection == null ? 0L : collection.size();
    }

    /**
     * 统计 Iterable 元素数量。
     *
     * @param iterable Iterable 对象
     * @return 元素数量，null 返回 0
     */
    public static long count(Iterable<?> iterable) {
        if (iterable == null) {
            return 0L;
        }
        if (iterable instanceof Collection<?> collection) {
            return collection.size();
        }

        long count = 0L;
        for (Object ignored : iterable) {
            count++;
        }
        return count;
    }

    /**
     * 统计 Iterator 元素数量。
     *
     * <p>
     * 该方法会消费 Iterator。
     * </p>
     *
     * @param iterator Iterator 对象
     * @return 元素数量，null 返回 0
     */
    public static long count(Iterator<?> iterator) {
        if (iterator == null) {
            return 0L;
        }

        long count = 0L;
        while (iterator.hasNext()) {
            iterator.next();
            count++;
        }
        return count;
    }

    /**
     * 统计数组元素数量。
     *
     * @param array 数组
     * @return 元素数量，null 返回 0
     */
    public static long count(Object[] array) {
        return array == null ? 0L : array.length;
    }

    /**
     * 对 Collection 中的 int 值求和。
     *
     * @param collection Collection 对象
     * @param mapper     int 值映射函数
     * @param <T>        元素类型
     * @return 求和结果
     */
    public static <T> int sumInt(Collection<? extends T> collection, ToIntFunction<? super T> mapper) {
        Objects.requireNonNull(mapper, "mapper 不能为 null");

        if (collection == null || collection.isEmpty()) {
            return 0;
        }

        int sum = 0;
        for (T item : collection) {
            sum = Math.addExact(sum, mapper.applyAsInt(item));
        }
        return sum;
    }

    /**
     * 对 Iterable 中的 int 值求和。
     *
     * @param iterable Iterable 对象
     * @param mapper   int 值映射函数
     * @param <T>      元素类型
     * @return 求和结果
     */
    public static <T> int sumInt(Iterable<? extends T> iterable, ToIntFunction<? super T> mapper) {
        Objects.requireNonNull(mapper, "mapper 不能为 null");

        if (iterable == null) {
            return 0;
        }

        int sum = 0;
        for (T item : iterable) {
            sum = Math.addExact(sum, mapper.applyAsInt(item));
        }
        return sum;
    }

    /**
     * 对 Iterator 中的 int 值求和。
     *
     * <p>
     * 该方法会消费 Iterator。
     * </p>
     *
     * @param iterator Iterator 对象
     * @param mapper   int 值映射函数
     * @param <T>      元素类型
     * @return 求和结果
     */
    public static <T> int sumInt(Iterator<? extends T> iterator, ToIntFunction<? super T> mapper) {
        Objects.requireNonNull(mapper, "mapper 不能为 null");

        if (iterator == null) {
            return 0;
        }

        int sum = 0;
        while (iterator.hasNext()) {
            sum = Math.addExact(sum, mapper.applyAsInt(iterator.next()));
        }
        return sum;
    }

    /**
     * 对数组中的 int 值求和。
     *
     * @param array  数组
     * @param mapper int 值映射函数
     * @param <T>    元素类型
     * @return 求和结果
     */
    public static <T> int sumInt(T[] array, ToIntFunction<? super T> mapper) {
        Objects.requireNonNull(mapper, "mapper 不能为 null");

        if (array == null || array.length == 0) {
            return 0;
        }

        int sum = 0;
        for (T item : array) {
            sum = Math.addExact(sum, mapper.applyAsInt(item));
        }
        return sum;
    }

    /**
     * 对 Collection 中的 int 值求和，并返回 long。
     *
     * @param collection Collection 对象
     * @param mapper     int 值映射函数
     * @param <T>        元素类型
     * @return long 求和结果
     */
    public static <T> long sumIntToLong(Collection<? extends T> collection, ToIntFunction<? super T> mapper) {
        Objects.requireNonNull(mapper, "mapper 不能为 null");

        if (collection == null || collection.isEmpty()) {
            return 0L;
        }

        long sum = 0L;
        for (T item : collection) {
            sum = Math.addExact(sum, mapper.applyAsInt(item));
        }
        return sum;
    }

    /**
     * 对 Iterable 中的 int 值求和，并返回 long。
     *
     * @param iterable Iterable 对象
     * @param mapper   int 值映射函数
     * @param <T>      元素类型
     * @return long 求和结果
     */
    public static <T> long sumIntToLong(Iterable<? extends T> iterable, ToIntFunction<? super T> mapper) {
        Objects.requireNonNull(mapper, "mapper 不能为 null");

        if (iterable == null) {
            return 0L;
        }

        long sum = 0L;
        for (T item : iterable) {
            sum = Math.addExact(sum, mapper.applyAsInt(item));
        }
        return sum;
    }

    /**
     * 对数组中的 int 值求和，并返回 long。
     *
     * @param array  数组
     * @param mapper int 值映射函数
     * @param <T>    元素类型
     * @return long 求和结果
     */
    public static <T> long sumIntToLong(T[] array, ToIntFunction<? super T> mapper) {
        Objects.requireNonNull(mapper, "mapper 不能为 null");

        if (array == null || array.length == 0) {
            return 0L;
        }

        long sum = 0L;
        for (T item : array) {
            sum = Math.addExact(sum, mapper.applyAsInt(item));
        }
        return sum;
    }

    /**
     * 对 Collection 中的 long 值求和。
     *
     * @param collection Collection 对象
     * @param mapper     long 值映射函数
     * @param <T>        元素类型
     * @return 求和结果
     */
    public static <T> long sumLong(Collection<? extends T> collection, ToLongFunction<? super T> mapper) {
        Objects.requireNonNull(mapper, "mapper 不能为 null");

        if (collection == null || collection.isEmpty()) {
            return 0L;
        }

        long sum = 0L;
        for (T item : collection) {
            sum = Math.addExact(sum, mapper.applyAsLong(item));
        }
        return sum;
    }

    /**
     * 对 Iterable 中的 long 值求和。
     *
     * @param iterable Iterable 对象
     * @param mapper   long 值映射函数
     * @param <T>      元素类型
     * @return 求和结果
     */
    public static <T> long sumLong(Iterable<? extends T> iterable, ToLongFunction<? super T> mapper) {
        Objects.requireNonNull(mapper, "mapper 不能为 null");

        if (iterable == null) {
            return 0L;
        }

        long sum = 0L;
        for (T item : iterable) {
            sum = Math.addExact(sum, mapper.applyAsLong(item));
        }
        return sum;
    }

    /**
     * 对 Iterator 中的 long 值求和。
     *
     * <p>
     * 该方法会消费 Iterator。
     * </p>
     *
     * @param iterator Iterator 对象
     * @param mapper   long 值映射函数
     * @param <T>      元素类型
     * @return 求和结果
     */
    public static <T> long sumLong(Iterator<? extends T> iterator, ToLongFunction<? super T> mapper) {
        Objects.requireNonNull(mapper, "mapper 不能为 null");

        if (iterator == null) {
            return 0L;
        }

        long sum = 0L;
        while (iterator.hasNext()) {
            sum = Math.addExact(sum, mapper.applyAsLong(iterator.next()));
        }
        return sum;
    }

    /**
     * 对数组中的 long 值求和。
     *
     * @param array  数组
     * @param mapper long 值映射函数
     * @param <T>    元素类型
     * @return 求和结果
     */
    public static <T> long sumLong(T[] array, ToLongFunction<? super T> mapper) {
        Objects.requireNonNull(mapper, "mapper 不能为 null");

        if (array == null || array.length == 0) {
            return 0L;
        }

        long sum = 0L;
        for (T item : array) {
            sum = Math.addExact(sum, mapper.applyAsLong(item));
        }
        return sum;
    }

    /**
     * 对 Collection 中的 double 值求和。
     *
     * @param collection Collection 对象
     * @param mapper     double 值映射函数
     * @param <T>        元素类型
     * @return 求和结果
     */
    public static <T> double sumDouble(Collection<? extends T> collection, ToDoubleFunction<? super T> mapper) {
        Objects.requireNonNull(mapper, "mapper 不能为 null");

        if (collection == null || collection.isEmpty()) {
            return 0D;
        }

        double sum = 0D;
        for (T item : collection) {
            sum += mapper.applyAsDouble(item);
        }
        return sum;
    }

    /**
     * 对 Iterable 中的 double 值求和。
     *
     * @param iterable Iterable 对象
     * @param mapper   double 值映射函数
     * @param <T>      元素类型
     * @return 求和结果
     */
    public static <T> double sumDouble(Iterable<? extends T> iterable, ToDoubleFunction<? super T> mapper) {
        Objects.requireNonNull(mapper, "mapper 不能为 null");

        if (iterable == null) {
            return 0D;
        }

        double sum = 0D;
        for (T item : iterable) {
            sum += mapper.applyAsDouble(item);
        }
        return sum;
    }

    /**
     * 对 Iterator 中的 double 值求和。
     *
     * <p>
     * 该方法会消费 Iterator。
     * </p>
     *
     * @param iterator Iterator 对象
     * @param mapper   double 值映射函数
     * @param <T>      元素类型
     * @return 求和结果
     */
    public static <T> double sumDouble(Iterator<? extends T> iterator, ToDoubleFunction<? super T> mapper) {
        Objects.requireNonNull(mapper, "mapper 不能为 null");

        if (iterator == null) {
            return 0D;
        }

        double sum = 0D;
        while (iterator.hasNext()) {
            sum += mapper.applyAsDouble(iterator.next());
        }
        return sum;
    }

    /**
     * 对数组中的 double 值求和。
     *
     * @param array  数组
     * @param mapper double 值映射函数
     * @param <T>    元素类型
     * @return 求和结果
     */
    public static <T> double sumDouble(T[] array, ToDoubleFunction<? super T> mapper) {
        Objects.requireNonNull(mapper, "mapper 不能为 null");

        if (array == null || array.length == 0) {
            return 0D;
        }

        double sum = 0D;
        for (T item : array) {
            sum += mapper.applyAsDouble(item);
        }
        return sum;
    }

    /**
     * 对 Collection 中的 BigDecimal 值求和。
     *
     * <p>
     * mapper 返回 null 时按 BigDecimal.ZERO 处理。
     * </p>
     *
     * @param collection Collection 对象
     * @param mapper     BigDecimal 值映射函数
     * @param <T>        元素类型
     * @return 求和结果
     */
    public static <T> BigDecimal sumBigDecimal(Collection<? extends T> collection, Function<? super T, BigDecimal> mapper) {
        Objects.requireNonNull(mapper, "mapper 不能为 null");

        if (collection == null || collection.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal sum = BigDecimal.ZERO;
        for (T item : collection) {
            sum = sum.add(Objects.requireNonNullElse(mapper.apply(item), BigDecimal.ZERO));
        }
        return sum;
    }

    /**
     * 对 Iterable 中的 BigDecimal 值求和。
     *
     * <p>
     * mapper 返回 null 时按 BigDecimal.ZERO 处理。
     * </p>
     *
     * @param iterable Iterable 对象
     * @param mapper   BigDecimal 值映射函数
     * @param <T>      元素类型
     * @return 求和结果
     */
    public static <T> BigDecimal sumBigDecimal(Iterable<? extends T> iterable, Function<? super T, BigDecimal> mapper) {
        Objects.requireNonNull(mapper, "mapper 不能为 null");

        if (iterable == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal sum = BigDecimal.ZERO;
        for (T item : iterable) {
            sum = sum.add(Objects.requireNonNullElse(mapper.apply(item), BigDecimal.ZERO));
        }
        return sum;
    }

    /**
     * 对 Iterator 中的 BigDecimal 值求和。
     *
     * <p>
     * 该方法会消费 Iterator；mapper 返回 null 时按 BigDecimal.ZERO 处理。
     * </p>
     *
     * @param iterator Iterator 对象
     * @param mapper   BigDecimal 值映射函数
     * @param <T>      元素类型
     * @return 求和结果
     */
    public static <T> BigDecimal sumBigDecimal(Iterator<? extends T> iterator, Function<? super T, BigDecimal> mapper) {
        Objects.requireNonNull(mapper, "mapper 不能为 null");

        if (iterator == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal sum = BigDecimal.ZERO;
        while (iterator.hasNext()) {
            sum = sum.add(Objects.requireNonNullElse(mapper.apply(iterator.next()), BigDecimal.ZERO));
        }
        return sum;
    }

    /**
     * 对数组中的 BigDecimal 值求和。
     *
     * <p>
     * mapper 返回 null 时按 BigDecimal.ZERO 处理。
     * </p>
     *
     * @param array  数组
     * @param mapper BigDecimal 值映射函数
     * @param <T>    元素类型
     * @return 求和结果
     */
    public static <T> BigDecimal sumBigDecimal(T[] array, Function<? super T, BigDecimal> mapper) {
        Objects.requireNonNull(mapper, "mapper 不能为 null");

        if (array == null || array.length == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal sum = BigDecimal.ZERO;
        for (T item : array) {
            sum = sum.add(Objects.requireNonNullElse(mapper.apply(item), BigDecimal.ZERO));
        }
        return sum;
    }

    /**
     * 计算 Collection 中 int 值平均数。
     *
     * @param collection Collection 对象
     * @param mapper     int 值映射函数
     * @param <T>        元素类型
     * @return 平均数，空集合返回 OptionalDouble.empty()
     */
    public static <T> OptionalDouble averageInt(Collection<? extends T> collection, ToIntFunction<? super T> mapper) {
        Objects.requireNonNull(mapper, "mapper 不能为 null");

        if (collection == null || collection.isEmpty()) {
            return OptionalDouble.empty();
        }

        long sum = 0L;
        long count = 0L;
        for (T item : collection) {
            sum = Math.addExact(sum, mapper.applyAsInt(item));
            count++;
        }
        return OptionalDouble.of((double) sum / count);
    }

    /**
     * 计算 Iterable 中 int 值平均数。
     *
     * @param iterable Iterable 对象
     * @param mapper   int 值映射函数
     * @param <T>      元素类型
     * @return 平均数，空 Iterable 返回 OptionalDouble.empty()
     */
    public static <T> OptionalDouble averageInt(Iterable<? extends T> iterable, ToIntFunction<? super T> mapper) {
        Objects.requireNonNull(mapper, "mapper 不能为 null");

        if (iterable == null) {
            return OptionalDouble.empty();
        }

        long sum = 0L;
        long count = 0L;
        for (T item : iterable) {
            sum = Math.addExact(sum, mapper.applyAsInt(item));
            count++;
        }
        return count == 0L ? OptionalDouble.empty() : OptionalDouble.of((double) sum / count);
    }

    /**
     * 计算数组中 int 值平均数。
     *
     * @param array  数组
     * @param mapper int 值映射函数
     * @param <T>    元素类型
     * @return 平均数，空数组返回 OptionalDouble.empty()
     */
    public static <T> OptionalDouble averageInt(T[] array, ToIntFunction<? super T> mapper) {
        Objects.requireNonNull(mapper, "mapper 不能为 null");

        if (array == null || array.length == 0) {
            return OptionalDouble.empty();
        }

        long sum = 0L;
        for (T item : array) {
            sum = Math.addExact(sum, mapper.applyAsInt(item));
        }
        return OptionalDouble.of((double) sum / array.length);
    }

    /**
     * 计算 Collection 中 long 值平均数。
     *
     * @param collection Collection 对象
     * @param mapper     long 值映射函数
     * @param <T>        元素类型
     * @return 平均数，空集合返回 OptionalDouble.empty()
     */
    public static <T> OptionalDouble averageLong(Collection<? extends T> collection, ToLongFunction<? super T> mapper) {
        Objects.requireNonNull(mapper, "mapper 不能为 null");

        if (collection == null || collection.isEmpty()) {
            return OptionalDouble.empty();
        }

        long sum = 0L;
        long count = 0L;
        for (T item : collection) {
            sum = Math.addExact(sum, mapper.applyAsLong(item));
            count++;
        }
        return OptionalDouble.of((double) sum / count);
    }

    /**
     * 计算 Iterable 中 long 值平均数。
     *
     * @param iterable Iterable 对象
     * @param mapper   long 值映射函数
     * @param <T>      元素类型
     * @return 平均数，空 Iterable 返回 OptionalDouble.empty()
     */
    public static <T> OptionalDouble averageLong(Iterable<? extends T> iterable, ToLongFunction<? super T> mapper) {
        Objects.requireNonNull(mapper, "mapper 不能为 null");

        if (iterable == null) {
            return OptionalDouble.empty();
        }

        long sum = 0L;
        long count = 0L;
        for (T item : iterable) {
            sum = Math.addExact(sum, mapper.applyAsLong(item));
            count++;
        }
        return count == 0L ? OptionalDouble.empty() : OptionalDouble.of((double) sum / count);
    }

    /**
     * 计算数组中 long 值平均数。
     *
     * @param array  数组
     * @param mapper long 值映射函数
     * @param <T>    元素类型
     * @return 平均数，空数组返回 OptionalDouble.empty()
     */
    public static <T> OptionalDouble averageLong(T[] array, ToLongFunction<? super T> mapper) {
        Objects.requireNonNull(mapper, "mapper 不能为 null");

        if (array == null || array.length == 0) {
            return OptionalDouble.empty();
        }

        long sum = 0L;
        for (T item : array) {
            sum = Math.addExact(sum, mapper.applyAsLong(item));
        }
        return OptionalDouble.of((double) sum / array.length);
    }

    /**
     * 计算 Collection 中 double 值平均数。
     *
     * @param collection Collection 对象
     * @param mapper     double 值映射函数
     * @param <T>        元素类型
     * @return 平均数，空集合返回 OptionalDouble.empty()
     */
    public static <T> OptionalDouble averageDouble(Collection<? extends T> collection, ToDoubleFunction<? super T> mapper) {
        Objects.requireNonNull(mapper, "mapper 不能为 null");

        if (collection == null || collection.isEmpty()) {
            return OptionalDouble.empty();
        }

        double sum = 0D;
        long count = 0L;
        for (T item : collection) {
            sum += mapper.applyAsDouble(item);
            count++;
        }
        return OptionalDouble.of(sum / count);
    }

    /**
     * 计算 Iterable 中 double 值平均数。
     *
     * @param iterable Iterable 对象
     * @param mapper   double 值映射函数
     * @param <T>      元素类型
     * @return 平均数，空 Iterable 返回 OptionalDouble.empty()
     */
    public static <T> OptionalDouble averageDouble(Iterable<? extends T> iterable, ToDoubleFunction<? super T> mapper) {
        Objects.requireNonNull(mapper, "mapper 不能为 null");

        if (iterable == null) {
            return OptionalDouble.empty();
        }

        double sum = 0D;
        long count = 0L;
        for (T item : iterable) {
            sum += mapper.applyAsDouble(item);
            count++;
        }
        return count == 0L ? OptionalDouble.empty() : OptionalDouble.of(sum / count);
    }

    /**
     * 计算数组中 double 值平均数。
     *
     * @param array  数组
     * @param mapper double 值映射函数
     * @param <T>    元素类型
     * @return 平均数，空数组返回 OptionalDouble.empty()
     */
    public static <T> OptionalDouble averageDouble(T[] array, ToDoubleFunction<? super T> mapper) {
        Objects.requireNonNull(mapper, "mapper 不能为 null");

        if (array == null || array.length == 0) {
            return OptionalDouble.empty();
        }

        double sum = 0D;
        for (T item : array) {
            sum += mapper.applyAsDouble(item);
        }
        return OptionalDouble.of(sum / array.length);
    }

    /**
     * 计算 Collection 中 BigDecimal 值平均数。
     *
     * <p>
     * mapper 返回 null 时按 BigDecimal.ZERO 处理。
     * </p>
     *
     * @param collection   Collection 对象
     * @param mapper       BigDecimal 值映射函数
     * @param scale        小数位数
     * @param roundingMode 舍入模式
     * @param <T>          元素类型
     * @return 平均数，空集合返回 Optional.empty()
     */
    public static <T> Optional<BigDecimal> averageBigDecimal(Collection<? extends T> collection,
                                                             Function<? super T, BigDecimal> mapper,
                                                             int scale,
                                                             java.math.RoundingMode roundingMode) {
        Objects.requireNonNull(mapper, "mapper 不能为 null");
        Objects.requireNonNull(roundingMode, "roundingMode 不能为 null");
        checkExpectedSize(scale);

        if (collection == null || collection.isEmpty()) {
            return Optional.empty();
        }

        BigDecimal sum = BigDecimal.ZERO;
        long count = 0L;
        for (T item : collection) {
            sum = sum.add(Objects.requireNonNullElse(mapper.apply(item), BigDecimal.ZERO));
            count++;
        }
        return Optional.of(sum.divide(BigDecimal.valueOf(count), scale, roundingMode));
    }

    /**
     * 计算 Iterable 中 BigDecimal 值平均数。
     *
     * <p>
     * mapper 返回 null 时按 BigDecimal.ZERO 处理。
     * </p>
     *
     * @param iterable     Iterable 对象
     * @param mapper       BigDecimal 值映射函数
     * @param scale        小数位数
     * @param roundingMode 舍入模式
     * @param <T>          元素类型
     * @return 平均数，空 Iterable 返回 Optional.empty()
     */
    public static <T> Optional<BigDecimal> averageBigDecimal(Iterable<? extends T> iterable,
                                                             Function<? super T, BigDecimal> mapper,
                                                             int scale,
                                                             java.math.RoundingMode roundingMode) {
        Objects.requireNonNull(mapper, "mapper 不能为 null");
        Objects.requireNonNull(roundingMode, "roundingMode 不能为 null");
        checkExpectedSize(scale);

        if (iterable == null) {
            return Optional.empty();
        }

        BigDecimal sum = BigDecimal.ZERO;
        long count = 0L;
        for (T item : iterable) {
            sum = sum.add(Objects.requireNonNullElse(mapper.apply(item), BigDecimal.ZERO));
            count++;
        }
        return count == 0L ? Optional.empty() : Optional.of(sum.divide(BigDecimal.valueOf(count), scale, roundingMode));
    }

    /**
     * 计算数组中 BigDecimal 值平均数。
     *
     * <p>
     * mapper 返回 null 时按 BigDecimal.ZERO 处理。
     * </p>
     *
     * @param array        数组
     * @param mapper       BigDecimal 值映射函数
     * @param scale        小数位数
     * @param roundingMode 舍入模式
     * @param <T>          元素类型
     * @return 平均数，空数组返回 Optional.empty()
     */
    public static <T> Optional<BigDecimal> averageBigDecimal(T[] array,
                                                             Function<? super T, BigDecimal> mapper,
                                                             int scale,
                                                             java.math.RoundingMode roundingMode) {
        Objects.requireNonNull(mapper, "mapper 不能为 null");
        Objects.requireNonNull(roundingMode, "roundingMode 不能为 null");
        checkExpectedSize(scale);

        if (array == null || array.length == 0) {
            return Optional.empty();
        }

        BigDecimal sum = BigDecimal.ZERO;
        for (T item : array) {
            sum = sum.add(Objects.requireNonNullElse(mapper.apply(item), BigDecimal.ZERO));
        }
        return Optional.of(sum.divide(BigDecimal.valueOf(array.length), scale, roundingMode));
    }

    /**
     * 获取 Collection 中的最小元素。
     *
     * @param collection Collection 对象
     * @param comparator 比较器
     * @param <T>        元素类型
     * @return 最小元素，空集合返回 Optional.empty()
     */
    public static <T> Optional<T> min(Collection<? extends T> collection, Comparator<? super T> comparator) {
        Objects.requireNonNull(comparator, "comparator 不能为 null");

        if (collection == null || collection.isEmpty()) {
            return Optional.empty();
        }

        T min = null;
        boolean found = false;
        for (T item : collection) {
            if (!found || comparator.compare(item, min) < 0) {
                min = item;
                found = true;
            }
        }
        return Optional.ofNullable(min);
    }

    /**
     * 获取 Iterable 中的最小元素。
     *
     * @param iterable   Iterable 对象
     * @param comparator 比较器
     * @param <T>        元素类型
     * @return 最小元素，空 Iterable 返回 Optional.empty()
     */
    public static <T> Optional<T> min(Iterable<? extends T> iterable, Comparator<? super T> comparator) {
        Objects.requireNonNull(comparator, "comparator 不能为 null");

        if (iterable == null) {
            return Optional.empty();
        }

        T min = null;
        boolean found = false;
        for (T item : iterable) {
            if (!found || comparator.compare(item, min) < 0) {
                min = item;
                found = true;
            }
        }
        return found ? Optional.ofNullable(min) : Optional.empty();
    }

    /**
     * 获取数组中的最小元素。
     *
     * @param array      数组
     * @param comparator 比较器
     * @param <T>        元素类型
     * @return 最小元素，空数组返回 Optional.empty()
     */
    public static <T> Optional<T> min(T[] array, Comparator<? super T> comparator) {
        Objects.requireNonNull(comparator, "comparator 不能为 null");

        if (array == null || array.length == 0) {
            return Optional.empty();
        }

        T min = array[0];
        for (int index = 1; index < array.length; index++) {
            if (comparator.compare(array[index], min) < 0) {
                min = array[index];
            }
        }
        return Optional.ofNullable(min);
    }

    /**
     * 获取 Collection 中的最大元素。
     *
     * @param collection Collection 对象
     * @param comparator 比较器
     * @param <T>        元素类型
     * @return 最大元素，空集合返回 Optional.empty()
     */
    public static <T> Optional<T> max(Collection<? extends T> collection, Comparator<? super T> comparator) {
        Objects.requireNonNull(comparator, "comparator 不能为 null");

        if (collection == null || collection.isEmpty()) {
            return Optional.empty();
        }

        T max = null;
        boolean found = false;
        for (T item : collection) {
            if (!found || comparator.compare(item, max) > 0) {
                max = item;
                found = true;
            }
        }
        return Optional.ofNullable(max);
    }

    /**
     * 获取 Iterable 中的最大元素。
     *
     * @param iterable   Iterable 对象
     * @param comparator 比较器
     * @param <T>        元素类型
     * @return 最大元素，空 Iterable 返回 Optional.empty()
     */
    public static <T> Optional<T> max(Iterable<? extends T> iterable, Comparator<? super T> comparator) {
        Objects.requireNonNull(comparator, "comparator 不能为 null");

        if (iterable == null) {
            return Optional.empty();
        }

        T max = null;
        boolean found = false;
        for (T item : iterable) {
            if (!found || comparator.compare(item, max) > 0) {
                max = item;
                found = true;
            }
        }
        return found ? Optional.ofNullable(max) : Optional.empty();
    }

    /**
     * 获取数组中的最大元素。
     *
     * @param array      数组
     * @param comparator 比较器
     * @param <T>        元素类型
     * @return 最大元素，空数组返回 Optional.empty()
     */
    public static <T> Optional<T> max(T[] array, Comparator<? super T> comparator) {
        Objects.requireNonNull(comparator, "comparator 不能为 null");

        if (array == null || array.length == 0) {
            return Optional.empty();
        }

        T max = array[0];
        for (int index = 1; index < array.length; index++) {
            if (comparator.compare(array[index], max) > 0) {
                max = array[index];
            }
        }
        return Optional.ofNullable(max);
    }

    /**
     * 按自然顺序获取 Collection 中的最小元素。
     *
     * <p>
     * null 元素排在最后。
     * </p>
     *
     * @param collection Collection 对象
     * @param <T>        元素类型
     * @return 最小元素，空集合返回 Optional.empty()
     */
    public static <T extends Comparable<? super T>> Optional<T> minNatural(Collection<? extends T> collection) {
        return min(collection, Comparator.nullsLast(Comparator.naturalOrder()));
    }

    /**
     * 按自然顺序获取 Collection 中的最大元素。
     *
     * <p>
     * null 元素排在最后。
     * </p>
     *
     * @param collection Collection 对象
     * @param <T>        元素类型
     * @return 最大元素，空集合返回 Optional.empty()
     */
    public static <T extends Comparable<? super T>> Optional<T> maxNatural(Collection<? extends T> collection) {
        return max(collection, Comparator.nullsLast(Comparator.naturalOrder()));
    }

    /**
     * 根据 Key 获取 Collection 中 Key 最小的元素。
     *
     * <p>
     * Key 为 null 时排在最后。
     * </p>
     *
     * @param collection Collection 对象
     * @param keyMapper  Key 映射函数
     * @param <T>        元素类型
     * @param <K>        Key 类型
     * @return Key 最小的元素，空集合返回 Optional.empty()
     */
    public static <T, K extends Comparable<? super K>> Optional<T> minBy(Collection<? extends T> collection,
                                                                         Function<? super T, ? extends K> keyMapper) {
        Objects.requireNonNull(keyMapper, "keyMapper 不能为 null");

        return min(collection, Comparator.comparing(keyMapper, Comparator.nullsLast(Comparator.naturalOrder())));
    }

    /**
     * 根据 Key 获取 Collection 中 Key 最大的元素。
     *
     * <p>
     * Key 为 null 时排在最后。
     * </p>
     *
     * @param collection Collection 对象
     * @param keyMapper  Key 映射函数
     * @param <T>        元素类型
     * @param <K>        Key 类型
     * @return Key 最大的元素，空集合返回 Optional.empty()
     */
    public static <T, K extends Comparable<? super K>> Optional<T> maxBy(Collection<? extends T> collection,
                                                                         Function<? super T, ? extends K> keyMapper) {
        Objects.requireNonNull(keyMapper, "keyMapper 不能为 null");

        return max(collection, Comparator.comparing(keyMapper, Comparator.nullsLast(Comparator.naturalOrder())));
    }

    /**
     * 获取 Collection 中 int 值最小值。
     *
     * @param collection Collection 对象
     * @param mapper     int 值映射函数
     * @param <T>        元素类型
     * @return 最小值，空集合返回 OptionalInt.empty()
     */
    public static <T> OptionalInt minInt(Collection<? extends T> collection, ToIntFunction<? super T> mapper) {
        Objects.requireNonNull(mapper, "mapper 不能为 null");

        if (collection == null || collection.isEmpty()) {
            return OptionalInt.empty();
        }

        boolean found = false;
        int min = 0;
        for (T item : collection) {
            int value = mapper.applyAsInt(item);
            if (!found || value < min) {
                min = value;
                found = true;
            }
        }
        return OptionalInt.of(min);
    }

    /**
     * 获取 Collection 中 int 值最大值。
     *
     * @param collection Collection 对象
     * @param mapper     int 值映射函数
     * @param <T>        元素类型
     * @return 最大值，空集合返回 OptionalInt.empty()
     */
    public static <T> OptionalInt maxInt(Collection<? extends T> collection, ToIntFunction<? super T> mapper) {
        Objects.requireNonNull(mapper, "mapper 不能为 null");

        if (collection == null || collection.isEmpty()) {
            return OptionalInt.empty();
        }

        boolean found = false;
        int max = 0;
        for (T item : collection) {
            int value = mapper.applyAsInt(item);
            if (!found || value > max) {
                max = value;
                found = true;
            }
        }
        return OptionalInt.of(max);
    }

    /**
     * 获取 Collection 中 long 值最小值。
     *
     * @param collection Collection 对象
     * @param mapper     long 值映射函数
     * @param <T>        元素类型
     * @return 最小值，空集合返回 OptionalLong.empty()
     */
    public static <T> OptionalLong minLong(Collection<? extends T> collection, ToLongFunction<? super T> mapper) {
        Objects.requireNonNull(mapper, "mapper 不能为 null");

        if (collection == null || collection.isEmpty()) {
            return OptionalLong.empty();
        }

        boolean found = false;
        long min = 0L;
        for (T item : collection) {
            long value = mapper.applyAsLong(item);
            if (!found || value < min) {
                min = value;
                found = true;
            }
        }
        return OptionalLong.of(min);
    }

    /**
     * 获取 Collection 中 long 值最大值。
     *
     * @param collection Collection 对象
     * @param mapper     long 值映射函数
     * @param <T>        元素类型
     * @return 最大值，空集合返回 OptionalLong.empty()
     */
    public static <T> OptionalLong maxLong(Collection<? extends T> collection, ToLongFunction<? super T> mapper) {
        Objects.requireNonNull(mapper, "mapper 不能为 null");

        if (collection == null || collection.isEmpty()) {
            return OptionalLong.empty();
        }

        boolean found = false;
        long max = 0L;
        for (T item : collection) {
            long value = mapper.applyAsLong(item);
            if (!found || value > max) {
                max = value;
                found = true;
            }
        }
        return OptionalLong.of(max);
    }

    /**
     * 获取 Collection 中 double 值最小值。
     *
     * @param collection Collection 对象
     * @param mapper     double 值映射函数
     * @param <T>        元素类型
     * @return 最小值，空集合返回 OptionalDouble.empty()
     */
    public static <T> OptionalDouble minDouble(Collection<? extends T> collection, ToDoubleFunction<? super T> mapper) {
        Objects.requireNonNull(mapper, "mapper 不能为 null");

        if (collection == null || collection.isEmpty()) {
            return OptionalDouble.empty();
        }

        boolean found = false;
        double min = 0D;
        for (T item : collection) {
            double value = mapper.applyAsDouble(item);
            if (!found || value < min) {
                min = value;
                found = true;
            }
        }
        return OptionalDouble.of(min);
    }

    /**
     * 获取 Collection 中 double 值最大值。
     *
     * @param collection Collection 对象
     * @param mapper     double 值映射函数
     * @param <T>        元素类型
     * @return 最大值，空集合返回 OptionalDouble.empty()
     */
    public static <T> OptionalDouble maxDouble(Collection<? extends T> collection, ToDoubleFunction<? super T> mapper) {
        Objects.requireNonNull(mapper, "mapper 不能为 null");

        if (collection == null || collection.isEmpty()) {
            return OptionalDouble.empty();
        }

        boolean found = false;
        double max = 0D;
        for (T item : collection) {
            double value = mapper.applyAsDouble(item);
            if (!found || value > max) {
                max = value;
                found = true;
            }
        }
        return OptionalDouble.of(max);
    }

    /**
     * 获取 Collection 中 BigDecimal 最小值。
     *
     * <p>
     * mapper 返回 null 的值会被忽略。
     * </p>
     *
     * @param collection Collection 对象
     * @param mapper     BigDecimal 值映射函数
     * @param <T>        元素类型
     * @return 最小值，不存在有效值时返回 Optional.empty()
     */
    public static <T> Optional<BigDecimal> minBigDecimal(Collection<? extends T> collection, Function<? super T, BigDecimal> mapper) {
        Objects.requireNonNull(mapper, "mapper 不能为 null");

        if (collection == null || collection.isEmpty()) {
            return Optional.empty();
        }

        BigDecimal min = null;
        for (T item : collection) {
            BigDecimal value = mapper.apply(item);
            if (value != null && (min == null || value.compareTo(min) < 0)) {
                min = value;
            }
        }
        return Optional.ofNullable(min);
    }

    /**
     * 获取 Collection 中 BigDecimal 最大值。
     *
     * <p>
     * mapper 返回 null 的值会被忽略。
     * </p>
     *
     * @param collection Collection 对象
     * @param mapper     BigDecimal 值映射函数
     * @param <T>        元素类型
     * @return 最大值，不存在有效值时返回 Optional.empty()
     */
    public static <T> Optional<BigDecimal> maxBigDecimal(Collection<? extends T> collection, Function<? super T, BigDecimal> mapper) {
        Objects.requireNonNull(mapper, "mapper 不能为 null");

        if (collection == null || collection.isEmpty()) {
            return Optional.empty();
        }

        BigDecimal max = null;
        for (T item : collection) {
            BigDecimal value = mapper.apply(item);
            if (value != null && (max == null || value.compareTo(max) > 0)) {
                max = value;
            }
        }
        return Optional.ofNullable(max);
    }

    /**
     * 获取 Collection 的 int 统计信息。
     *
     * @param collection Collection 对象
     * @param mapper     int 值映射函数
     * @param <T>        元素类型
     * @return IntSummaryStatistics 统计信息
     */
    public static <T> IntSummaryStatistics summarizeInt(Collection<? extends T> collection, ToIntFunction<? super T> mapper) {
        Objects.requireNonNull(mapper, "mapper 不能为 null");

        IntSummaryStatistics statistics = new IntSummaryStatistics();
        if (collection == null || collection.isEmpty()) {
            return statistics;
        }

        for (T item : collection) {
            statistics.accept(mapper.applyAsInt(item));
        }
        return statistics;
    }

    /**
     * 获取 Iterable 的 int 统计信息。
     *
     * @param iterable Iterable 对象
     * @param mapper   int 值映射函数
     * @param <T>      元素类型
     * @return IntSummaryStatistics 统计信息
     */
    public static <T> IntSummaryStatistics summarizeInt(Iterable<? extends T> iterable, ToIntFunction<? super T> mapper) {
        Objects.requireNonNull(mapper, "mapper 不能为 null");

        IntSummaryStatistics statistics = new IntSummaryStatistics();
        if (iterable == null) {
            return statistics;
        }

        for (T item : iterable) {
            statistics.accept(mapper.applyAsInt(item));
        }
        return statistics;
    }

    /**
     * 获取数组的 int 统计信息。
     *
     * @param array  数组
     * @param mapper int 值映射函数
     * @param <T>    元素类型
     * @return IntSummaryStatistics 统计信息
     */
    public static <T> IntSummaryStatistics summarizeInt(T[] array, ToIntFunction<? super T> mapper) {
        Objects.requireNonNull(mapper, "mapper 不能为 null");

        IntSummaryStatistics statistics = new IntSummaryStatistics();
        if (array == null || array.length == 0) {
            return statistics;
        }

        for (T item : array) {
            statistics.accept(mapper.applyAsInt(item));
        }
        return statistics;
    }

    /**
     * 获取 Collection 的 long 统计信息。
     *
     * @param collection Collection 对象
     * @param mapper     long 值映射函数
     * @param <T>        元素类型
     * @return LongSummaryStatistics 统计信息
     */
    public static <T> LongSummaryStatistics summarizeLong(Collection<? extends T> collection, ToLongFunction<? super T> mapper) {
        Objects.requireNonNull(mapper, "mapper 不能为 null");

        LongSummaryStatistics statistics = new LongSummaryStatistics();
        if (collection == null || collection.isEmpty()) {
            return statistics;
        }

        for (T item : collection) {
            statistics.accept(mapper.applyAsLong(item));
        }
        return statistics;
    }

    /**
     * 获取 Iterable 的 long 统计信息。
     *
     * @param iterable Iterable 对象
     * @param mapper   long 值映射函数
     * @param <T>      元素类型
     * @return LongSummaryStatistics 统计信息
     */
    public static <T> LongSummaryStatistics summarizeLong(Iterable<? extends T> iterable, ToLongFunction<? super T> mapper) {
        Objects.requireNonNull(mapper, "mapper 不能为 null");

        LongSummaryStatistics statistics = new LongSummaryStatistics();
        if (iterable == null) {
            return statistics;
        }

        for (T item : iterable) {
            statistics.accept(mapper.applyAsLong(item));
        }
        return statistics;
    }

    /**
     * 获取数组的 long 统计信息。
     *
     * @param array  数组
     * @param mapper long 值映射函数
     * @param <T>    元素类型
     * @return LongSummaryStatistics 统计信息
     */
    public static <T> LongSummaryStatistics summarizeLong(T[] array, ToLongFunction<? super T> mapper) {
        Objects.requireNonNull(mapper, "mapper 不能为 null");

        LongSummaryStatistics statistics = new LongSummaryStatistics();
        if (array == null || array.length == 0) {
            return statistics;
        }

        for (T item : array) {
            statistics.accept(mapper.applyAsLong(item));
        }
        return statistics;
    }

    /**
     * 获取 Collection 的 double 统计信息。
     *
     * @param collection Collection 对象
     * @param mapper     double 值映射函数
     * @param <T>        元素类型
     * @return DoubleSummaryStatistics 统计信息
     */
    public static <T> DoubleSummaryStatistics summarizeDouble(Collection<? extends T> collection, ToDoubleFunction<? super T> mapper) {
        Objects.requireNonNull(mapper, "mapper 不能为 null");

        DoubleSummaryStatistics statistics = new DoubleSummaryStatistics();
        if (collection == null || collection.isEmpty()) {
            return statistics;
        }

        for (T item : collection) {
            statistics.accept(mapper.applyAsDouble(item));
        }
        return statistics;
    }

    /**
     * 获取 Iterable 的 double 统计信息。
     *
     * @param iterable Iterable 对象
     * @param mapper   double 值映射函数
     * @param <T>      元素类型
     * @return DoubleSummaryStatistics 统计信息
     */
    public static <T> DoubleSummaryStatistics summarizeDouble(Iterable<? extends T> iterable, ToDoubleFunction<? super T> mapper) {
        Objects.requireNonNull(mapper, "mapper 不能为 null");

        DoubleSummaryStatistics statistics = new DoubleSummaryStatistics();
        if (iterable == null) {
            return statistics;
        }

        for (T item : iterable) {
            statistics.accept(mapper.applyAsDouble(item));
        }
        return statistics;
    }

    /**
     * 获取数组的 double 统计信息。
     *
     * @param array  数组
     * @param mapper double 值映射函数
     * @param <T>    元素类型
     * @return DoubleSummaryStatistics 统计信息
     */
    public static <T> DoubleSummaryStatistics summarizeDouble(T[] array, ToDoubleFunction<? super T> mapper) {
        Objects.requireNonNull(mapper, "mapper 不能为 null");

        DoubleSummaryStatistics statistics = new DoubleSummaryStatistics();
        if (array == null || array.length == 0) {
            return statistics;
        }

        for (T item : array) {
            statistics.accept(mapper.applyAsDouble(item));
        }
        return statistics;
    }

    /**
     * 拼接 Collection 元素。
     *
     * <p>
     * 元素使用 String.valueOf 转为字符串，null 元素会被拼接为字符串 "null"。
     * </p>
     *
     * @param collection Collection 对象
     * @param delimiter  分隔符，null 按空字符串处理
     * @return 拼接结果
     */
    public static String join(Collection<?> collection, CharSequence delimiter) {
        if (collection == null || collection.isEmpty()) {
            return "";
        }

        StringJoiner joiner = new StringJoiner(nullToEmpty(delimiter));
        for (Object item : collection) {
            joiner.add(String.valueOf(item));
        }
        return joiner.toString();
    }

    /**
     * 拼接 Iterable 元素。
     *
     * <p>
     * 元素使用 String.valueOf 转为字符串，null 元素会被拼接为字符串 "null"。
     * </p>
     *
     * @param iterable  Iterable 对象
     * @param delimiter 分隔符，null 按空字符串处理
     * @return 拼接结果
     */
    public static String join(Iterable<?> iterable, CharSequence delimiter) {
        if (iterable == null) {
            return "";
        }

        StringJoiner joiner = new StringJoiner(nullToEmpty(delimiter));
        for (Object item : iterable) {
            joiner.add(String.valueOf(item));
        }
        return joiner.toString();
    }

    /**
     * 拼接 Iterator 元素。
     *
     * <p>
     * 该方法会消费 Iterator；元素使用 String.valueOf 转为字符串，null 元素会被拼接为字符串 "null"。
     * </p>
     *
     * @param iterator  Iterator 对象
     * @param delimiter 分隔符，null 按空字符串处理
     * @return 拼接结果
     */
    public static String join(Iterator<?> iterator, CharSequence delimiter) {
        if (iterator == null) {
            return "";
        }

        StringJoiner joiner = new StringJoiner(nullToEmpty(delimiter));
        while (iterator.hasNext()) {
            joiner.add(String.valueOf(iterator.next()));
        }
        return joiner.toString();
    }

    /**
     * 拼接数组元素。
     *
     * <p>
     * 元素使用 String.valueOf 转为字符串，null 元素会被拼接为字符串 "null"。
     * </p>
     *
     * @param array     数组
     * @param delimiter 分隔符，null 按空字符串处理
     * @return 拼接结果
     */
    public static String join(Object[] array, CharSequence delimiter) {
        if (array == null || array.length == 0) {
            return "";
        }

        StringJoiner joiner = new StringJoiner(nullToEmpty(delimiter));
        for (Object item : array) {
            joiner.add(String.valueOf(item));
        }
        return joiner.toString();
    }

    /**
     * 拼接任意数组元素，支持对象数组和基本类型数组。
     *
     * <p>
     * 元素使用 String.valueOf 转为字符串，null 元素会被拼接为字符串 "null"。
     * </p>
     *
     * @param array     数组对象
     * @param delimiter 分隔符，null 按空字符串处理
     * @return 拼接结果
     */
    public static String joinArray(Object array, CharSequence delimiter) {
        if (array == null) {
            return "";
        }
        if (!array.getClass().isArray()) {
            throw new IllegalArgumentException("参数必须是数组类型");
        }

        int length = Array.getLength(array);
        if (length == 0) {
            return "";
        }

        StringJoiner joiner = new StringJoiner(nullToEmpty(delimiter));
        for (int index = 0; index < length; index++) {
            joiner.add(String.valueOf(Array.get(array, index)));
        }
        return joiner.toString();
    }

    /**
     * 拼接 Collection 元素，并添加前缀和后缀。
     *
     * @param collection Collection 对象
     * @param delimiter  分隔符，null 按空字符串处理
     * @param prefix     前缀，null 按空字符串处理
     * @param suffix     后缀，null 按空字符串处理
     * @return 拼接结果
     */
    public static String join(Collection<?> collection, CharSequence delimiter, CharSequence prefix, CharSequence suffix) {
        if (collection == null || collection.isEmpty()) {
            return nullToEmpty(prefix) + nullToEmpty(suffix);
        }

        StringJoiner joiner = new StringJoiner(nullToEmpty(delimiter), nullToEmpty(prefix), nullToEmpty(suffix));
        for (Object item : collection) {
            joiner.add(String.valueOf(item));
        }
        return joiner.toString();
    }

    /**
     * 拼接 Iterable 元素，并添加前缀和后缀。
     *
     * @param iterable  Iterable 对象
     * @param delimiter 分隔符，null 按空字符串处理
     * @param prefix    前缀，null 按空字符串处理
     * @param suffix    后缀，null 按空字符串处理
     * @return 拼接结果
     */
    public static String join(Iterable<?> iterable, CharSequence delimiter, CharSequence prefix, CharSequence suffix) {
        if (iterable == null) {
            return nullToEmpty(prefix) + nullToEmpty(suffix);
        }

        StringJoiner joiner = new StringJoiner(nullToEmpty(delimiter), nullToEmpty(prefix), nullToEmpty(suffix));
        for (Object item : iterable) {
            joiner.add(String.valueOf(item));
        }
        return joiner.toString();
    }

    /**
     * 拼接数组元素，并添加前缀和后缀。
     *
     * @param array     数组
     * @param delimiter 分隔符，null 按空字符串处理
     * @param prefix    前缀，null 按空字符串处理
     * @param suffix    后缀，null 按空字符串处理
     * @return 拼接结果
     */
    public static String join(Object[] array, CharSequence delimiter, CharSequence prefix, CharSequence suffix) {
        if (array == null || array.length == 0) {
            return nullToEmpty(prefix) + nullToEmpty(suffix);
        }

        StringJoiner joiner = new StringJoiner(nullToEmpty(delimiter), nullToEmpty(prefix), nullToEmpty(suffix));
        for (Object item : array) {
            joiner.add(String.valueOf(item));
        }
        return joiner.toString();
    }

    /**
     * 拼接 Collection 中的非 null 元素。
     *
     * @param collection Collection 对象
     * @param delimiter  分隔符，null 按空字符串处理
     * @return 拼接结果
     */
    public static String joinNotNull(Collection<?> collection, CharSequence delimiter) {
        if (collection == null || collection.isEmpty()) {
            return "";
        }

        StringJoiner joiner = new StringJoiner(nullToEmpty(delimiter));
        for (Object item : collection) {
            if (item != null) {
                joiner.add(String.valueOf(item));
            }
        }
        return joiner.toString();
    }

    /**
     * 拼接 Iterable 中的非 null 元素。
     *
     * @param iterable  Iterable 对象
     * @param delimiter 分隔符，null 按空字符串处理
     * @return 拼接结果
     */
    public static String joinNotNull(Iterable<?> iterable, CharSequence delimiter) {
        if (iterable == null) {
            return "";
        }

        StringJoiner joiner = new StringJoiner(nullToEmpty(delimiter));
        for (Object item : iterable) {
            if (item != null) {
                joiner.add(String.valueOf(item));
            }
        }
        return joiner.toString();
    }

    /**
     * 拼接 Iterator 中的非 null 元素。
     *
     * <p>
     * 该方法会消费 Iterator。
     * </p>
     *
     * @param iterator  Iterator 对象
     * @param delimiter 分隔符，null 按空字符串处理
     * @return 拼接结果
     */
    public static String joinNotNull(Iterator<?> iterator, CharSequence delimiter) {
        if (iterator == null) {
            return "";
        }

        StringJoiner joiner = new StringJoiner(nullToEmpty(delimiter));
        while (iterator.hasNext()) {
            Object item = iterator.next();
            if (item != null) {
                joiner.add(String.valueOf(item));
            }
        }
        return joiner.toString();
    }

    /**
     * 拼接数组中的非 null 元素。
     *
     * @param array     数组
     * @param delimiter 分隔符，null 按空字符串处理
     * @return 拼接结果
     */
    public static String joinNotNull(Object[] array, CharSequence delimiter) {
        if (array == null || array.length == 0) {
            return "";
        }

        StringJoiner joiner = new StringJoiner(nullToEmpty(delimiter));
        for (Object item : array) {
            if (item != null) {
                joiner.add(String.valueOf(item));
            }
        }
        return joiner.toString();
    }

    /**
     * 拼接 Collection 中的非空白字符串。
     *
     * <p>
     * null、空字符串、空白字符串会被忽略。
     * </p>
     *
     * @param collection Collection 对象
     * @param delimiter  分隔符，null 按空字符串处理
     * @return 拼接结果
     */
    public static String joinNotBlank(Collection<? extends CharSequence> collection, CharSequence delimiter) {
        if (collection == null || collection.isEmpty()) {
            return "";
        }

        StringJoiner joiner = new StringJoiner(nullToEmpty(delimiter));
        for (CharSequence item : collection) {
            if (isNotBlank(item)) {
                joiner.add(item.toString());
            }
        }
        return joiner.toString();
    }

    /**
     * 拼接 Iterable 中的非空白字符串。
     *
     * <p>
     * null、空字符串、空白字符串会被忽略。
     * </p>
     *
     * @param iterable  Iterable 对象
     * @param delimiter 分隔符，null 按空字符串处理
     * @return 拼接结果
     */
    public static String joinNotBlank(Iterable<? extends CharSequence> iterable, CharSequence delimiter) {
        if (iterable == null) {
            return "";
        }

        StringJoiner joiner = new StringJoiner(nullToEmpty(delimiter));
        for (CharSequence item : iterable) {
            if (isNotBlank(item)) {
                joiner.add(item.toString());
            }
        }
        return joiner.toString();
    }

    /**
     * 拼接数组中的非空白字符串。
     *
     * <p>
     * null、空字符串、空白字符串会被忽略。
     * </p>
     *
     * @param array     字符串数组
     * @param delimiter 分隔符，null 按空字符串处理
     * @return 拼接结果
     */
    public static String joinNotBlank(CharSequence[] array, CharSequence delimiter) {
        if (array == null || array.length == 0) {
            return "";
        }

        StringJoiner joiner = new StringJoiner(nullToEmpty(delimiter));
        for (CharSequence item : array) {
            if (isNotBlank(item)) {
                joiner.add(item.toString());
            }
        }
        return joiner.toString();
    }

    /**
     * 映射 Collection 元素后拼接。
     *
     * @param collection Collection 对象
     * @param delimiter  分隔符，null 按空字符串处理
     * @param mapper     字符串映射函数
     * @param <T>        元素类型
     * @return 拼接结果
     */
    public static <T> String joinBy(Collection<? extends T> collection,
                                    CharSequence delimiter,
                                    Function<? super T, ?> mapper) {
        Objects.requireNonNull(mapper, "mapper 不能为 null");

        if (collection == null || collection.isEmpty()) {
            return "";
        }

        StringJoiner joiner = new StringJoiner(nullToEmpty(delimiter));
        for (T item : collection) {
            joiner.add(String.valueOf(mapper.apply(item)));
        }
        return joiner.toString();
    }

    /**
     * 映射 Iterable 元素后拼接。
     *
     * @param iterable  Iterable 对象
     * @param delimiter 分隔符，null 按空字符串处理
     * @param mapper    字符串映射函数
     * @param <T>       元素类型
     * @return 拼接结果
     */
    public static <T> String joinBy(Iterable<? extends T> iterable,
                                    CharSequence delimiter,
                                    Function<? super T, ?> mapper) {
        Objects.requireNonNull(mapper, "mapper 不能为 null");

        if (iterable == null) {
            return "";
        }

        StringJoiner joiner = new StringJoiner(nullToEmpty(delimiter));
        for (T item : iterable) {
            joiner.add(String.valueOf(mapper.apply(item)));
        }
        return joiner.toString();
    }

    /**
     * 映射 Iterator 元素后拼接。
     *
     * <p>
     * 该方法会消费 Iterator。
     * </p>
     *
     * @param iterator  Iterator 对象
     * @param delimiter 分隔符，null 按空字符串处理
     * @param mapper    字符串映射函数
     * @param <T>       元素类型
     * @return 拼接结果
     */
    public static <T> String joinBy(Iterator<? extends T> iterator,
                                    CharSequence delimiter,
                                    Function<? super T, ?> mapper) {
        Objects.requireNonNull(mapper, "mapper 不能为 null");

        if (iterator == null) {
            return "";
        }

        StringJoiner joiner = new StringJoiner(nullToEmpty(delimiter));
        while (iterator.hasNext()) {
            joiner.add(String.valueOf(mapper.apply(iterator.next())));
        }
        return joiner.toString();
    }

    /**
     * 映射数组元素后拼接。
     *
     * @param array     数组
     * @param delimiter 分隔符，null 按空字符串处理
     * @param mapper    字符串映射函数
     * @param <T>       元素类型
     * @return 拼接结果
     */
    public static <T> String joinBy(T[] array,
                                    CharSequence delimiter,
                                    Function<? super T, ?> mapper) {
        Objects.requireNonNull(mapper, "mapper 不能为 null");

        if (array == null || array.length == 0) {
            return "";
        }

        StringJoiner joiner = new StringJoiner(nullToEmpty(delimiter));
        for (T item : array) {
            joiner.add(String.valueOf(mapper.apply(item)));
        }
        return joiner.toString();
    }

    /**
     * 映射 Collection 元素后拼接，并过滤 null 映射结果。
     *
     * @param collection Collection 对象
     * @param delimiter  分隔符，null 按空字符串处理
     * @param mapper     字符串映射函数
     * @param <T>        元素类型
     * @return 拼接结果
     */
    public static <T> String joinByNotNull(Collection<? extends T> collection,
                                           CharSequence delimiter,
                                           Function<? super T, ?> mapper) {
        Objects.requireNonNull(mapper, "mapper 不能为 null");

        if (collection == null || collection.isEmpty()) {
            return "";
        }

        StringJoiner joiner = new StringJoiner(nullToEmpty(delimiter));
        for (T item : collection) {
            Object value = mapper.apply(item);
            if (value != null) {
                joiner.add(String.valueOf(value));
            }
        }
        return joiner.toString();
    }

    /**
     * 映射 Iterable 元素后拼接，并过滤 null 映射结果。
     *
     * @param iterable  Iterable 对象
     * @param delimiter 分隔符，null 按空字符串处理
     * @param mapper    字符串映射函数
     * @param <T>       元素类型
     * @return 拼接结果
     */
    public static <T> String joinByNotNull(Iterable<? extends T> iterable,
                                           CharSequence delimiter,
                                           Function<? super T, ?> mapper) {
        Objects.requireNonNull(mapper, "mapper 不能为 null");

        if (iterable == null) {
            return "";
        }

        StringJoiner joiner = new StringJoiner(nullToEmpty(delimiter));
        for (T item : iterable) {
            Object value = mapper.apply(item);
            if (value != null) {
                joiner.add(String.valueOf(value));
            }
        }
        return joiner.toString();
    }

    /**
     * 映射数组元素后拼接，并过滤 null 映射结果。
     *
     * @param array     数组
     * @param delimiter 分隔符，null 按空字符串处理
     * @param mapper    字符串映射函数
     * @param <T>       元素类型
     * @return 拼接结果
     */
    public static <T> String joinByNotNull(T[] array,
                                           CharSequence delimiter,
                                           Function<? super T, ?> mapper) {
        Objects.requireNonNull(mapper, "mapper 不能为 null");

        if (array == null || array.length == 0) {
            return "";
        }

        StringJoiner joiner = new StringJoiner(nullToEmpty(delimiter));
        for (T item : array) {
            Object value = mapper.apply(item);
            if (value != null) {
                joiner.add(String.valueOf(value));
            }
        }
        return joiner.toString();
    }

    /**
     * 映射 Collection 元素后拼接，并过滤空白映射结果。
     *
     * <p>
     * mapper 返回 null、空字符串、空白字符串时会被忽略。
     * </p>
     *
     * @param collection Collection 对象
     * @param delimiter  分隔符，null 按空字符串处理
     * @param mapper     字符串映射函数
     * @param <T>        元素类型
     * @return 拼接结果
     */
    public static <T> String joinByNotBlank(Collection<? extends T> collection,
                                            CharSequence delimiter,
                                            Function<? super T, ? extends CharSequence> mapper) {
        Objects.requireNonNull(mapper, "mapper 不能为 null");

        if (collection == null || collection.isEmpty()) {
            return "";
        }

        StringJoiner joiner = new StringJoiner(nullToEmpty(delimiter));
        for (T item : collection) {
            CharSequence value = mapper.apply(item);
            if (isNotBlank(value)) {
                joiner.add(value.toString());
            }
        }
        return joiner.toString();
    }

    /**
     * 映射 Iterable 元素后拼接，并过滤空白映射结果。
     *
     * <p>
     * mapper 返回 null、空字符串、空白字符串时会被忽略。
     * </p>
     *
     * @param iterable  Iterable 对象
     * @param delimiter 分隔符，null 按空字符串处理
     * @param mapper    字符串映射函数
     * @param <T>       元素类型
     * @return 拼接结果
     */
    public static <T> String joinByNotBlank(Iterable<? extends T> iterable,
                                            CharSequence delimiter,
                                            Function<? super T, ? extends CharSequence> mapper) {
        Objects.requireNonNull(mapper, "mapper 不能为 null");

        if (iterable == null) {
            return "";
        }

        StringJoiner joiner = new StringJoiner(nullToEmpty(delimiter));
        for (T item : iterable) {
            CharSequence value = mapper.apply(item);
            if (isNotBlank(value)) {
                joiner.add(value.toString());
            }
        }
        return joiner.toString();
    }

    /**
     * 映射数组元素后拼接，并过滤空白映射结果。
     *
     * <p>
     * mapper 返回 null、空字符串、空白字符串时会被忽略。
     * </p>
     *
     * @param array     数组
     * @param delimiter 分隔符，null 按空字符串处理
     * @param mapper    字符串映射函数
     * @param <T>       元素类型
     * @return 拼接结果
     */
    public static <T> String joinByNotBlank(T[] array,
                                            CharSequence delimiter,
                                            Function<? super T, ? extends CharSequence> mapper) {
        Objects.requireNonNull(mapper, "mapper 不能为 null");

        if (array == null || array.length == 0) {
            return "";
        }

        StringJoiner joiner = new StringJoiner(nullToEmpty(delimiter));
        for (T item : array) {
            CharSequence value = mapper.apply(item);
            if (isNotBlank(value)) {
                joiner.add(value.toString());
            }
        }
        return joiner.toString();
    }

    /**
     * 映射 Collection 元素后拼接，并添加前缀和后缀。
     *
     * @param collection Collection 对象
     * @param delimiter  分隔符，null 按空字符串处理
     * @param prefix     前缀，null 按空字符串处理
     * @param suffix     后缀，null 按空字符串处理
     * @param mapper     字符串映射函数
     * @param <T>        元素类型
     * @return 拼接结果
     */
    public static <T> String joinBy(Collection<? extends T> collection,
                                    CharSequence delimiter,
                                    CharSequence prefix,
                                    CharSequence suffix,
                                    Function<? super T, ?> mapper) {
        Objects.requireNonNull(mapper, "mapper 不能为 null");

        if (collection == null || collection.isEmpty()) {
            return nullToEmpty(prefix) + nullToEmpty(suffix);
        }

        StringJoiner joiner = new StringJoiner(nullToEmpty(delimiter), nullToEmpty(prefix), nullToEmpty(suffix));
        for (T item : collection) {
            joiner.add(String.valueOf(mapper.apply(item)));
        }
        return joiner.toString();
    }

    /**
     * 将 Collection 每个元素包装后再拼接。
     *
     * <p>
     * 适合生成 'a','b','c' 或 (1),(2),(3) 这类格式。
     * </p>
     *
     * @param collection Collection 对象
     * @param delimiter  分隔符，null 按空字符串处理
     * @param itemPrefix 元素前缀，null 按空字符串处理
     * @param itemSuffix 元素后缀，null 按空字符串处理
     * @return 拼接结果
     */
    public static String joinWrapped(Collection<?> collection,
                                     CharSequence delimiter,
                                     CharSequence itemPrefix,
                                     CharSequence itemSuffix) {
        if (collection == null || collection.isEmpty()) {
            return "";
        }

        StringJoiner joiner = new StringJoiner(nullToEmpty(delimiter));
        String prefix = nullToEmpty(itemPrefix);
        String suffix = nullToEmpty(itemSuffix);

        for (Object item : collection) {
            joiner.add(prefix + String.valueOf(item) + suffix);
        }
        return joiner.toString();
    }

    /**
     * 将数组每个元素包装后再拼接。
     *
     * <p>
     * 适合生成 'a','b','c' 或 (1),(2),(3) 这类格式。
     * </p>
     *
     * @param array      数组
     * @param delimiter  分隔符，null 按空字符串处理
     * @param itemPrefix 元素前缀，null 按空字符串处理
     * @param itemSuffix 元素后缀，null 按空字符串处理
     * @return 拼接结果
     */
    public static String joinWrapped(Object[] array,
                                     CharSequence delimiter,
                                     CharSequence itemPrefix,
                                     CharSequence itemSuffix) {
        if (array == null || array.length == 0) {
            return "";
        }

        StringJoiner joiner = new StringJoiner(nullToEmpty(delimiter));
        String prefix = nullToEmpty(itemPrefix);
        String suffix = nullToEmpty(itemSuffix);

        for (Object item : array) {
            joiner.add(prefix + String.valueOf(item) + suffix);
        }
        return joiner.toString();
    }

    /**
     * 映射 Collection 元素并包装后再拼接。
     *
     * @param collection Collection 对象
     * @param delimiter  分隔符，null 按空字符串处理
     * @param itemPrefix 元素前缀，null 按空字符串处理
     * @param itemSuffix 元素后缀，null 按空字符串处理
     * @param mapper     字符串映射函数
     * @param <T>        元素类型
     * @return 拼接结果
     */
    public static <T> String joinByWrapped(Collection<? extends T> collection,
                                           CharSequence delimiter,
                                           CharSequence itemPrefix,
                                           CharSequence itemSuffix,
                                           Function<? super T, ?> mapper) {
        Objects.requireNonNull(mapper, "mapper 不能为 null");

        if (collection == null || collection.isEmpty()) {
            return "";
        }

        StringJoiner joiner = new StringJoiner(nullToEmpty(delimiter));
        String prefix = nullToEmpty(itemPrefix);
        String suffix = nullToEmpty(itemSuffix);

        for (T item : collection) {
            joiner.add(prefix + String.valueOf(mapper.apply(item)) + suffix);
        }
        return joiner.toString();
    }

    /**
     * 将 Collection 拼接为 SQL IN 片段内容。
     *
     * <p>
     * 会过滤 null 元素，并使用单引号包裹每个元素；单引号会转义为两个单引号。
     * 仅返回 IN 内部内容，不包含括号。
     * </p>
     *
     * @param collection Collection 对象
     * @return SQL IN 片段内容
     */
    public static String joinSqlIn(Collection<?> collection) {
        if (collection == null || collection.isEmpty()) {
            return "";
        }

        StringJoiner joiner = new StringJoiner(",");
        for (Object item : collection) {
            if (item != null) {
                joiner.add("'" + escapeSqlLiteral(String.valueOf(item)) + "'");
            }
        }
        return joiner.toString();
    }

    /**
     * 将数组拼接为 SQL IN 片段内容。
     *
     * <p>
     * 会过滤 null 元素，并使用单引号包裹每个元素；单引号会转义为两个单引号。
     * 仅返回 IN 内部内容，不包含括号。
     * </p>
     *
     * @param array 数组
     * @return SQL IN 片段内容
     */
    public static String joinSqlIn(Object[] array) {
        if (array == null || array.length == 0) {
            return "";
        }

        StringJoiner joiner = new StringJoiner(",");
        for (Object item : array) {
            if (item != null) {
                joiner.add("'" + escapeSqlLiteral(String.valueOf(item)) + "'");
            }
        }
        return joiner.toString();
    }

    /**
     * 映射 Collection 后拼接为 SQL IN 片段内容。
     *
     * <p>
     * 会过滤 null 映射结果，并使用单引号包裹每个元素；单引号会转义为两个单引号。
     * 仅返回 IN 内部内容，不包含括号。
     * </p>
     *
     * @param collection Collection 对象
     * @param mapper     字符串映射函数
     * @param <T>        元素类型
     * @return SQL IN 片段内容
     */
    public static <T> String joinSqlInBy(Collection<? extends T> collection, Function<? super T, ?> mapper) {
        Objects.requireNonNull(mapper, "mapper 不能为 null");

        if (collection == null || collection.isEmpty()) {
            return "";
        }

        StringJoiner joiner = new StringJoiner(",");
        for (T item : collection) {
            Object value = mapper.apply(item);
            if (value != null) {
                joiner.add("'" + escapeSqlLiteral(String.valueOf(value)) + "'");
            }
        }
        return joiner.toString();
    }

    /**
     * 拼接 Map Entry。
     *
     * <p>
     * 每个 Entry 使用 keyValueDelimiter 连接，Entry 之间使用 entryDelimiter 连接。
     * </p>
     *
     * @param map               Map 对象
     * @param entryDelimiter    Entry 分隔符，null 按空字符串处理
     * @param keyValueDelimiter Key 与 Value 分隔符，null 按空字符串处理
     * @return 拼接结果
     */
    public static String joinMap(Map<?, ?> map, CharSequence entryDelimiter, CharSequence keyValueDelimiter) {
        if (map == null || map.isEmpty()) {
            return "";
        }

        StringJoiner joiner = new StringJoiner(nullToEmpty(entryDelimiter));
        String kvDelimiter = nullToEmpty(keyValueDelimiter);

        for (Map.Entry<?, ?> entry : map.entrySet()) {
            joiner.add(String.valueOf(entry.getKey()) + kvDelimiter + String.valueOf(entry.getValue()));
        }
        return joiner.toString();
    }

    /**
     * 拼接 Map Entry，并过滤 Key 或 Value 为 null 的 Entry。
     *
     * <p>
     * 每个 Entry 使用 keyValueDelimiter 连接，Entry 之间使用 entryDelimiter 连接。
     * </p>
     *
     * @param map               Map 对象
     * @param entryDelimiter    Entry 分隔符，null 按空字符串处理
     * @param keyValueDelimiter Key 与 Value 分隔符，null 按空字符串处理
     * @return 拼接结果
     */
    public static String joinMapNotNull(Map<?, ?> map, CharSequence entryDelimiter, CharSequence keyValueDelimiter) {
        if (map == null || map.isEmpty()) {
            return "";
        }

        StringJoiner joiner = new StringJoiner(nullToEmpty(entryDelimiter));
        String kvDelimiter = nullToEmpty(keyValueDelimiter);

        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                joiner.add(String.valueOf(entry.getKey()) + kvDelimiter + String.valueOf(entry.getValue()));
            }
        }
        return joiner.toString();
    }

    /**
     * 自定义格式拼接 Map Entry。
     *
     * @param map       Map 对象
     * @param delimiter Entry 分隔符，null 按空字符串处理
     * @param mapper    Entry 映射函数
     * @param <K>       Key 类型
     * @param <V>       Value 类型
     * @return 拼接结果
     */
    public static <K, V> String joinMapBy(Map<? extends K, ? extends V> map,
                                          CharSequence delimiter,
                                          BiFunction<? super K, ? super V, ? extends CharSequence> mapper) {
        Objects.requireNonNull(mapper, "mapper 不能为 null");

        if (map == null || map.isEmpty()) {
            return "";
        }

        StringJoiner joiner = new StringJoiner(nullToEmpty(delimiter));
        for (Map.Entry<? extends K, ? extends V> entry : map.entrySet()) {
            joiner.add(String.valueOf(mapper.apply(entry.getKey(), entry.getValue())));
        }
        return joiner.toString();
    }

    /**
     * 自定义格式拼接 Map Entry，并过滤 null 映射结果。
     *
     * @param map       Map 对象
     * @param delimiter Entry 分隔符，null 按空字符串处理
     * @param mapper    Entry 映射函数
     * @param <K>       Key 类型
     * @param <V>       Value 类型
     * @return 拼接结果
     */
    public static <K, V> String joinMapByNotNull(Map<? extends K, ? extends V> map,
                                                 CharSequence delimiter,
                                                 BiFunction<? super K, ? super V, ? extends CharSequence> mapper) {
        Objects.requireNonNull(mapper, "mapper 不能为 null");

        if (map == null || map.isEmpty()) {
            return "";
        }

        StringJoiner joiner = new StringJoiner(nullToEmpty(delimiter));
        for (Map.Entry<? extends K, ? extends V> entry : map.entrySet()) {
            CharSequence value = mapper.apply(entry.getKey(), entry.getValue());
            if (value != null) {
                joiner.add(value.toString());
            }
        }
        return joiner.toString();
    }

    /**
     * 使用默认逗号拼接 Collection 元素。
     *
     * @param collection Collection 对象
     * @return 拼接结果
     */
    public static String commaJoin(Collection<?> collection) {
        return join(collection, ",");
    }

    /**
     * 使用默认逗号拼接数组元素。
     *
     * @param array 数组
     * @return 拼接结果
     */
    public static String commaJoin(Object[] array) {
        return join(array, ",");
    }

    /**
     * 使用默认逗号拼接 Collection 中的非 null 元素。
     *
     * @param collection Collection 对象
     * @return 拼接结果
     */
    public static String commaJoinNotNull(Collection<?> collection) {
        return joinNotNull(collection, ",");
    }

    /**
     * 使用默认逗号拼接数组中的非 null 元素。
     *
     * @param array 数组
     * @return 拼接结果
     */
    public static String commaJoinNotNull(Object[] array) {
        return joinNotNull(array, ",");
    }

    /**
     * 使用默认逗号拼接 Collection 中的非空白字符串。
     *
     * @param collection Collection 对象
     * @return 拼接结果
     */
    public static String commaJoinNotBlank(Collection<? extends CharSequence> collection) {
        return joinNotBlank(collection, ",");
    }

    /**
     * 将 CharSequence 转为字符串，null 转为空字符串。
     *
     * @param value 字符序列
     * @return 字符串
     */
    private static String nullToEmpty(CharSequence value) {
        return value == null ? "" : value.toString();
    }

    /**
     * 判断字符串是否非空白。
     *
     * @param value 字符序列
     * @return true 表示非 null、非空字符串、非空白字符串
     */
    private static boolean isNotBlank(CharSequence value) {
        return value != null && !value.toString().isBlank();
    }

    /**
     * 转义 SQL 字符串字面量中的单引号。
     *
     * @param value 字符串
     * @return 转义后的字符串
     */
    private static String escapeSqlLiteral(String value) {
        return value == null ? "" : value.replace("'", "''");
    }

    /**
     * 过滤 Collection 中的 null 元素，并返回 ArrayList。
     *
     * @param collection Collection 对象
     * @param <T>        元素类型
     * @return 不包含 null 的可变 ArrayList
     */
    public static <T> List<T> removeNullToList(Collection<? extends T> collection) {
        List<T> result = new ArrayList<>();
        if (collection == null || collection.isEmpty()) {
            return result;
        }

        for (T item : collection) {
            if (item != null) {
                result.add(item);
            }
        }
        return result;
    }

    /**
     * 过滤 Iterable 中的 null 元素，并返回 ArrayList。
     *
     * @param iterable Iterable 对象
     * @param <T>      元素类型
     * @return 不包含 null 的可变 ArrayList
     */
    public static <T> List<T> removeNullToList(Iterable<? extends T> iterable) {
        List<T> result = new ArrayList<>();
        if (iterable == null) {
            return result;
        }

        for (T item : iterable) {
            if (item != null) {
                result.add(item);
            }
        }
        return result;
    }

    /**
     * 过滤 Iterator 中的 null 元素，并返回 ArrayList。
     *
     * <p>
     * 该方法会消费 Iterator。
     * </p>
     *
     * @param iterator Iterator 对象
     * @param <T>      元素类型
     * @return 不包含 null 的可变 ArrayList
     */
    public static <T> List<T> removeNullToList(Iterator<? extends T> iterator) {
        List<T> result = new ArrayList<>();
        if (iterator == null) {
            return result;
        }

        while (iterator.hasNext()) {
            T item = iterator.next();
            if (item != null) {
                result.add(item);
            }
        }
        return result;
    }

    /**
     * 过滤数组中的 null 元素，并返回 ArrayList。
     *
     * @param array 数组
     * @param <T>   元素类型
     * @return 不包含 null 的可变 ArrayList
     */
    public static <T> List<T> removeNullToList(T[] array) {
        List<T> result = new ArrayList<>();
        if (array == null || array.length == 0) {
            return result;
        }

        for (T item : array) {
            if (item != null) {
                result.add(item);
            }
        }
        return result;
    }

    /**
     * 过滤 Collection 中的 null 元素，并返回 LinkedHashSet。
     *
     * <p>
     * 返回结果会按首次出现顺序去重。
     * </p>
     *
     * @param collection Collection 对象
     * @param <T>        元素类型
     * @return 不包含 null 的可变 LinkedHashSet
     */
    public static <T> Set<T> removeNullToSet(Collection<? extends T> collection) {
        Set<T> result = new LinkedHashSet<>();
        if (collection == null || collection.isEmpty()) {
            return result;
        }

        for (T item : collection) {
            if (item != null) {
                result.add(item);
            }
        }
        return result;
    }

    /**
     * 过滤 Iterable 中的 null 元素，并返回 LinkedHashSet。
     *
     * @param iterable Iterable 对象
     * @param <T>      元素类型
     * @return 不包含 null 的可变 LinkedHashSet
     */
    public static <T> Set<T> removeNullToSet(Iterable<? extends T> iterable) {
        Set<T> result = new LinkedHashSet<>();
        if (iterable == null) {
            return result;
        }

        for (T item : iterable) {
            if (item != null) {
                result.add(item);
            }
        }
        return result;
    }

    /**
     * 过滤数组中的 null 元素，并返回 LinkedHashSet。
     *
     * @param array 数组
     * @param <T>   元素类型
     * @return 不包含 null 的可变 LinkedHashSet
     */
    public static <T> Set<T> removeNullToSet(T[] array) {
        Set<T> result = new LinkedHashSet<>();
        if (array == null || array.length == 0) {
            return result;
        }

        for (T item : array) {
            if (item != null) {
                result.add(item);
            }
        }
        return result;
    }

    /**
     * 原地移除 Collection 中的 null 元素。
     *
     * @param collection Collection 对象
     * @return true 表示 Collection 发生变化
     */
    public static boolean removeNullInPlace(Collection<?> collection) {
        if (collection == null || collection.isEmpty()) {
            return false;
        }

        return collection.removeIf(Objects::isNull);
    }

    /**
     * 原地移除 List 中的 null 元素。
     *
     * @param list List 对象
     * @return true 表示 List 发生变化
     */
    public static boolean removeNullInPlace(List<?> list) {
        if (list == null || list.isEmpty()) {
            return false;
        }

        return list.removeIf(Objects::isNull);
    }

    /**
     * 原地移除数组中的 null 元素无法缩容，因此返回清理后的新数组。
     *
     * @param array     数组
     * @param generator 数组创建函数
     * @param <T>       元素类型
     * @return 不包含 null 的新数组
     */
    public static <T> T[] removeNullToArray(T[] array, IntFunction<T[]> generator) {
        Objects.requireNonNull(generator, "generator 不能为 null");
        return removeNullToList(array).toArray(generator);
    }

    /**
     * 过滤 Collection 中的空字符串。
     *
     * <p>
     * null 会保留；仅移除长度为 0 的字符串。
     * </p>
     *
     * @param collection 字符串集合
     * @return 不包含空字符串的可变 ArrayList
     */
    public static List<String> removeEmptyStringToList(Collection<String> collection) {
        List<String> result = new ArrayList<>();
        if (collection == null || collection.isEmpty()) {
            return result;
        }

        for (String item : collection) {
            if (item == null || !item.isEmpty()) {
                result.add(item);
            }
        }
        return result;
    }

    /**
     * 过滤数组中的空字符串。
     *
     * <p>
     * null 会保留；仅移除长度为 0 的字符串。
     * </p>
     *
     * @param array 字符串数组
     * @return 不包含空字符串的可变 ArrayList
     */
    public static List<String> removeEmptyStringToList(String[] array) {
        List<String> result = new ArrayList<>();
        if (array == null || array.length == 0) {
            return result;
        }

        for (String item : array) {
            if (item == null || !item.isEmpty()) {
                result.add(item);
            }
        }
        return result;
    }

    /**
     * 原地移除 Collection 中的空字符串。
     *
     * <p>
     * null 会保留；仅移除长度为 0 的字符串。
     * </p>
     *
     * @param collection 字符串集合
     * @return true 表示 Collection 发生变化
     */
    public static boolean removeEmptyStringInPlace(Collection<String> collection) {
        if (collection == null || collection.isEmpty()) {
            return false;
        }

        return collection.removeIf(item -> item != null && item.isEmpty());
    }

    /**
     * 过滤 Collection 中的空白字符串。
     *
     * <p>
     * null 会保留；空字符串和空白字符串会被移除。
     * </p>
     *
     * @param collection 字符串集合
     * @return 不包含空白字符串的可变 ArrayList
     */
    public static List<String> removeBlankStringToList(Collection<String> collection) {
        List<String> result = new ArrayList<>();
        if (collection == null || collection.isEmpty()) {
            return result;
        }

        for (String item : collection) {
            if (item == null || !item.isBlank()) {
                result.add(item);
            }
        }
        return result;
    }

    /**
     * 过滤数组中的空白字符串。
     *
     * <p>
     * null 会保留；空字符串和空白字符串会被移除。
     * </p>
     *
     * @param array 字符串数组
     * @return 不包含空白字符串的可变 ArrayList
     */
    public static List<String> removeBlankStringToList(String[] array) {
        List<String> result = new ArrayList<>();
        if (array == null || array.length == 0) {
            return result;
        }

        for (String item : array) {
            if (item == null || !item.isBlank()) {
                result.add(item);
            }
        }
        return result;
    }

    /**
     * 原地移除 Collection 中的空白字符串。
     *
     * <p>
     * null 会保留；空字符串和空白字符串会被移除。
     * </p>
     *
     * @param collection 字符串集合
     * @return true 表示 Collection 发生变化
     */
    public static boolean removeBlankStringInPlace(Collection<String> collection) {
        if (collection == null || collection.isEmpty()) {
            return false;
        }

        return collection.removeIf(item -> item != null && item.isBlank());
    }

    /**
     * 过滤 Collection 中的 null 和空字符串。
     *
     * @param collection 字符串集合
     * @return 不包含 null 和空字符串的可变 ArrayList
     */
    public static List<String> removeNullAndEmptyStringToList(Collection<String> collection) {
        List<String> result = new ArrayList<>();
        if (collection == null || collection.isEmpty()) {
            return result;
        }

        for (String item : collection) {
            if (item != null && !item.isEmpty()) {
                result.add(item);
            }
        }
        return result;
    }

    /**
     * 过滤数组中的 null 和空字符串。
     *
     * @param array 字符串数组
     * @return 不包含 null 和空字符串的可变 ArrayList
     */
    public static List<String> removeNullAndEmptyStringToList(String[] array) {
        List<String> result = new ArrayList<>();
        if (array == null || array.length == 0) {
            return result;
        }

        for (String item : array) {
            if (item != null && !item.isEmpty()) {
                result.add(item);
            }
        }
        return result;
    }

    /**
     * 原地移除 Collection 中的 null 和空字符串。
     *
     * @param collection 字符串集合
     * @return true 表示 Collection 发生变化
     */
    public static boolean removeNullAndEmptyStringInPlace(Collection<String> collection) {
        if (collection == null || collection.isEmpty()) {
            return false;
        }

        return collection.removeIf(item -> item == null || item.isEmpty());
    }

    /**
     * 过滤 Collection 中的 null 和空白字符串。
     *
     * @param collection 字符串集合
     * @return 不包含 null 和空白字符串的可变 ArrayList
     */
    public static List<String> removeNullAndBlankStringToList(Collection<String> collection) {
        List<String> result = new ArrayList<>();
        if (collection == null || collection.isEmpty()) {
            return result;
        }

        for (String item : collection) {
            if (item != null && !item.isBlank()) {
                result.add(item);
            }
        }
        return result;
    }

    /**
     * 过滤数组中的 null 和空白字符串。
     *
     * @param array 字符串数组
     * @return 不包含 null 和空白字符串的可变 ArrayList
     */
    public static List<String> removeNullAndBlankStringToList(String[] array) {
        List<String> result = new ArrayList<>();
        if (array == null || array.length == 0) {
            return result;
        }

        for (String item : array) {
            if (item != null && !item.isBlank()) {
                result.add(item);
            }
        }
        return result;
    }

    /**
     * 原地移除 Collection 中的 null 和空白字符串。
     *
     * @param collection 字符串集合
     * @return true 表示 Collection 发生变化
     */
    public static boolean removeNullAndBlankStringInPlace(Collection<String> collection) {
        if (collection == null || collection.isEmpty()) {
            return false;
        }

        return collection.removeIf(item -> item == null || item.isBlank());
    }

    /**
     * 清理 Collection 中的空值元素。
     *
     * <p>
     * 空值定义：null、空字符串、空白字符串、空 Collection、空 Map、空数组、Optional.empty()。
     * </p>
     *
     * @param collection Collection 对象
     * @param <T>        元素类型
     * @return 清理后的可变 ArrayList
     */
    public static <T> List<T> removeEmptyValueToList(Collection<? extends T> collection) {
        List<T> result = new ArrayList<>();
        if (collection == null || collection.isEmpty()) {
            return result;
        }

        for (T item : collection) {
            if (!isEmptyValue(item)) {
                result.add(item);
            }
        }
        return result;
    }

    /**
     * 清理 Iterable 中的空值元素。
     *
     * <p>
     * 空值定义：null、空字符串、空白字符串、空 Collection、空 Map、空数组、Optional.empty()。
     * </p>
     *
     * @param iterable Iterable 对象
     * @param <T>      元素类型
     * @return 清理后的可变 ArrayList
     */
    public static <T> List<T> removeEmptyValueToList(Iterable<? extends T> iterable) {
        List<T> result = new ArrayList<>();
        if (iterable == null) {
            return result;
        }

        for (T item : iterable) {
            if (!isEmptyValue(item)) {
                result.add(item);
            }
        }
        return result;
    }

    /**
     * 清理 Iterator 中的空值元素。
     *
     * <p>
     * 该方法会消费 Iterator。
     * 空值定义：null、空字符串、空白字符串、空 Collection、空 Map、空数组、Optional.empty()。
     * </p>
     *
     * @param iterator Iterator 对象
     * @param <T>      元素类型
     * @return 清理后的可变 ArrayList
     */
    public static <T> List<T> removeEmptyValueToList(Iterator<? extends T> iterator) {
        List<T> result = new ArrayList<>();
        if (iterator == null) {
            return result;
        }

        while (iterator.hasNext()) {
            T item = iterator.next();
            if (!isEmptyValue(item)) {
                result.add(item);
            }
        }
        return result;
    }

    /**
     * 清理数组中的空值元素。
     *
     * <p>
     * 空值定义：null、空字符串、空白字符串、空 Collection、空 Map、空数组、Optional.empty()。
     * </p>
     *
     * @param array 数组
     * @param <T>   元素类型
     * @return 清理后的可变 ArrayList
     */
    public static <T> List<T> removeEmptyValueToList(T[] array) {
        List<T> result = new ArrayList<>();
        if (array == null || array.length == 0) {
            return result;
        }

        for (T item : array) {
            if (!isEmptyValue(item)) {
                result.add(item);
            }
        }
        return result;
    }

    /**
     * 清理 Collection 中的空值元素，并返回 LinkedHashSet。
     *
     * <p>
     * 空值定义：null、空字符串、空白字符串、空 Collection、空 Map、空数组、Optional.empty()。
     * 返回结果会按首次出现顺序去重。
     * </p>
     *
     * @param collection Collection 对象
     * @param <T>        元素类型
     * @return 清理后的可变 LinkedHashSet
     */
    public static <T> Set<T> removeEmptyValueToSet(Collection<? extends T> collection) {
        Set<T> result = new LinkedHashSet<>();
        if (collection == null || collection.isEmpty()) {
            return result;
        }

        for (T item : collection) {
            if (!isEmptyValue(item)) {
                result.add(item);
            }
        }
        return result;
    }

    /**
     * 原地清理 Collection 中的空值元素。
     *
     * <p>
     * 空值定义：null、空字符串、空白字符串、空 Collection、空 Map、空数组、Optional.empty()。
     * </p>
     *
     * @param collection Collection 对象
     * @return true 表示 Collection 发生变化
     */
    public static boolean removeEmptyValueInPlace(Collection<?> collection) {
        if (collection == null || collection.isEmpty()) {
            return false;
        }

        return collection.removeIf(CollectionUtil::isEmptyValue);
    }

    /**
     * 过滤 Collection 中的空 Collection 元素。
     *
     * <p>
     * null 会保留；仅移除非 null 且 isEmpty() 为 true 的 Collection。
     * </p>
     *
     * @param collection Collection 对象
     * @param <C>        元素集合类型
     * @return 不包含空 Collection 的可变 ArrayList
     */
    public static <C extends Collection<?>> List<C> removeEmptyCollectionToList(Collection<? extends C> collection) {
        List<C> result = new ArrayList<>();
        if (collection == null || collection.isEmpty()) {
            return result;
        }

        for (C item : collection) {
            if (item == null || !item.isEmpty()) {
                result.add(item);
            }
        }
        return result;
    }

    /**
     * 过滤 Collection 中的 null 和空 Collection 元素。
     *
     * @param collection Collection 对象
     * @param <C>        元素集合类型
     * @return 不包含 null 和空 Collection 的可变 ArrayList
     */
    public static <C extends Collection<?>> List<C> removeNullAndEmptyCollectionToList(Collection<? extends C> collection) {
        List<C> result = new ArrayList<>();
        if (collection == null || collection.isEmpty()) {
            return result;
        }

        for (C item : collection) {
            if (item != null && !item.isEmpty()) {
                result.add(item);
            }
        }
        return result;
    }

    /**
     * 过滤 Collection 中的空 Map 元素。
     *
     * <p>
     * null 会保留；仅移除非 null 且 isEmpty() 为 true 的 Map。
     * </p>
     *
     * @param collection Collection 对象
     * @param <M>        元素 Map 类型
     * @return 不包含空 Map 的可变 ArrayList
     */
    public static <M extends Map<?, ?>> List<M> removeEmptyMapToList(Collection<? extends M> collection) {
        List<M> result = new ArrayList<>();
        if (collection == null || collection.isEmpty()) {
            return result;
        }

        for (M item : collection) {
            if (item == null || !item.isEmpty()) {
                result.add(item);
            }
        }
        return result;
    }

    /**
     * 过滤 Collection 中的 null 和空 Map 元素。
     *
     * @param collection Collection 对象
     * @param <M>        元素 Map 类型
     * @return 不包含 null 和空 Map 的可变 ArrayList
     */
    public static <M extends Map<?, ?>> List<M> removeNullAndEmptyMapToList(Collection<? extends M> collection) {
        List<M> result = new ArrayList<>();
        if (collection == null || collection.isEmpty()) {
            return result;
        }

        for (M item : collection) {
            if (item != null && !item.isEmpty()) {
                result.add(item);
            }
        }
        return result;
    }

    /**
     * 移除 Map 中 Key 为 null 的 Entry。
     *
     * <p>
     * 返回 LinkedHashMap，不修改原 Map。
     * </p>
     *
     * @param map Map 对象
     * @param <K> Key 类型
     * @param <V> Value 类型
     * @return Key 非 null 的 Map
     */
    public static <K, V> Map<K, V> removeNullKeyToMap(Map<? extends K, ? extends V> map) {
        Map<K, V> result = new LinkedHashMap<>();
        if (map == null || map.isEmpty()) {
            return result;
        }

        for (Map.Entry<? extends K, ? extends V> entry : map.entrySet()) {
            if (entry.getKey() != null) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    /**
     * 移除 Map 中 Value 为 null 的 Entry。
     *
     * <p>
     * 返回 LinkedHashMap，不修改原 Map。
     * </p>
     *
     * @param map Map 对象
     * @param <K> Key 类型
     * @param <V> Value 类型
     * @return Value 非 null 的 Map
     */
    public static <K, V> Map<K, V> removeNullValueToMap(Map<? extends K, ? extends V> map) {
        Map<K, V> result = new LinkedHashMap<>();
        if (map == null || map.isEmpty()) {
            return result;
        }

        for (Map.Entry<? extends K, ? extends V> entry : map.entrySet()) {
            if (entry.getValue() != null) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    /**
     * 移除 Map 中 Key 或 Value 为 null 的 Entry。
     *
     * <p>
     * 返回 LinkedHashMap，不修改原 Map。
     * </p>
     *
     * @param map Map 对象
     * @param <K> Key 类型
     * @param <V> Value 类型
     * @return Key 和 Value 均非 null 的 Map
     */
    public static <K, V> Map<K, V> removeNullEntryToMap(Map<? extends K, ? extends V> map) {
        Map<K, V> result = new LinkedHashMap<>();
        if (map == null || map.isEmpty()) {
            return result;
        }

        for (Map.Entry<? extends K, ? extends V> entry : map.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    /**
     * 移除 Map 中空字符串 Key 的 Entry。
     *
     * <p>
     * null Key 会保留；仅移除类型为 String 且长度为 0 的 Key。
     * 返回 LinkedHashMap，不修改原 Map。
     * </p>
     *
     * @param map Map 对象
     * @param <V> Value 类型
     * @return Key 非空字符串的 Map
     */
    public static <V> Map<String, V> removeEmptyStringKeyToMap(Map<String, ? extends V> map) {
        Map<String, V> result = new LinkedHashMap<>();
        if (map == null || map.isEmpty()) {
            return result;
        }

        for (Map.Entry<String, ? extends V> entry : map.entrySet()) {
            String key = entry.getKey();
            if (key == null || !key.isEmpty()) {
                result.put(key, entry.getValue());
            }
        }
        return result;
    }

    /**
     * 移除 Map 中空白字符串 Key 的 Entry。
     *
     * <p>
     * null Key 会保留；仅移除空字符串和空白字符串 Key。
     * 返回 LinkedHashMap，不修改原 Map。
     * </p>
     *
     * @param map Map 对象
     * @param <V> Value 类型
     * @return Key 非空白字符串的 Map
     */
    public static <V> Map<String, V> removeBlankStringKeyToMap(Map<String, ? extends V> map) {
        Map<String, V> result = new LinkedHashMap<>();
        if (map == null || map.isEmpty()) {
            return result;
        }

        for (Map.Entry<String, ? extends V> entry : map.entrySet()) {
            String key = entry.getKey();
            if (key == null || !key.isBlank()) {
                result.put(key, entry.getValue());
            }
        }
        return result;
    }

    /**
     * 移除 Map 中 null 或空白字符串 Key 的 Entry。
     *
     * <p>
     * 返回 LinkedHashMap，不修改原 Map。
     * </p>
     *
     * @param map Map 对象
     * @param <V> Value 类型
     * @return Key 非 null 且非空白字符串的 Map
     */
    public static <V> Map<String, V> removeNullAndBlankStringKeyToMap(Map<String, ? extends V> map) {
        Map<String, V> result = new LinkedHashMap<>();
        if (map == null || map.isEmpty()) {
            return result;
        }

        for (Map.Entry<String, ? extends V> entry : map.entrySet()) {
            String key = entry.getKey();
            if (key != null && !key.isBlank()) {
                result.put(key, entry.getValue());
            }
        }
        return result;
    }

    /**
     * 移除 Map 中空字符串 Value 的 Entry。
     *
     * <p>
     * null Value 会保留；仅移除长度为 0 的 Value。
     * 返回 LinkedHashMap，不修改原 Map。
     * </p>
     *
     * @param map Map 对象
     * @param <K> Key 类型
     * @return Value 非空字符串的 Map
     */
    public static <K> Map<K, String> removeEmptyStringValueToMap(Map<? extends K, String> map) {
        Map<K, String> result = new LinkedHashMap<>();
        if (map == null || map.isEmpty()) {
            return result;
        }

        for (Map.Entry<? extends K, String> entry : map.entrySet()) {
            String value = entry.getValue();
            if (value == null || !value.isEmpty()) {
                result.put(entry.getKey(), value);
            }
        }
        return result;
    }

    /**
     * 移除 Map 中空白字符串 Value 的 Entry。
     *
     * <p>
     * null Value 会保留；仅移除空字符串和空白字符串 Value。
     * 返回 LinkedHashMap，不修改原 Map。
     * </p>
     *
     * @param map Map 对象
     * @param <K> Key 类型
     * @return Value 非空白字符串的 Map
     */
    public static <K> Map<K, String> removeBlankStringValueToMap(Map<? extends K, String> map) {
        Map<K, String> result = new LinkedHashMap<>();
        if (map == null || map.isEmpty()) {
            return result;
        }

        for (Map.Entry<? extends K, String> entry : map.entrySet()) {
            String value = entry.getValue();
            if (value == null || !value.isBlank()) {
                result.put(entry.getKey(), value);
            }
        }
        return result;
    }

    /**
     * 移除 Map 中 null 或空白字符串 Value 的 Entry。
     *
     * <p>
     * 返回 LinkedHashMap，不修改原 Map。
     * </p>
     *
     * @param map Map 对象
     * @param <K> Key 类型
     * @return Value 非 null 且非空白字符串的 Map
     */
    public static <K> Map<K, String> removeNullAndBlankStringValueToMap(Map<? extends K, String> map) {
        Map<K, String> result = new LinkedHashMap<>();
        if (map == null || map.isEmpty()) {
            return result;
        }

        for (Map.Entry<? extends K, String> entry : map.entrySet()) {
            String value = entry.getValue();
            if (value != null && !value.isBlank()) {
                result.put(entry.getKey(), value);
            }
        }
        return result;
    }

    /**
     * 移除 Map 中空值 Value 的 Entry。
     *
     * <p>
     * 空值定义：null、空字符串、空白字符串、空 Collection、空 Map、空数组、Optional.empty()。
     * 返回 LinkedHashMap，不修改原 Map。
     * </p>
     *
     * @param map Map 对象
     * @param <K> Key 类型
     * @param <V> Value 类型
     * @return Value 非空值的 Map
     */
    public static <K, V> Map<K, V> removeEmptyValueToMap(Map<? extends K, ? extends V> map) {
        Map<K, V> result = new LinkedHashMap<>();
        if (map == null || map.isEmpty()) {
            return result;
        }

        for (Map.Entry<? extends K, ? extends V> entry : map.entrySet()) {
            if (!isEmptyValue(entry.getValue())) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    /**
     * 原地移除 Map 中 Key 为 null 的 Entry。
     *
     * @param map Map 对象
     * @return true 表示 Map 发生变化
     */
    public static boolean removeNullKeyInPlace(Map<?, ?> map) {
        if (map == null || map.isEmpty()) {
            return false;
        }

        int oldSize = map.size();
        map.keySet().removeIf(Objects::isNull);
        return map.size() != oldSize;
    }

    /**
     * 原地移除 Map 中 Key 或 Value 为 null 的 Entry。
     *
     * @param map Map 对象
     * @return true 表示 Map 发生变化
     */
    public static boolean removeNullEntryInPlace(Map<?, ?> map) {
        if (map == null || map.isEmpty()) {
            return false;
        }

        int oldSize = map.size();
        map.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);
        return map.size() != oldSize;
    }

    /**
     * 原地移除 Map 中空值 Value 的 Entry。
     *
     * <p>
     * 空值定义：null、空字符串、空白字符串、空 Collection、空 Map、空数组、Optional.empty()。
     * </p>
     *
     * @param map Map 对象
     * @return true 表示 Map 发生变化
     */
    public static boolean removeEmptyValueInPlace(Map<?, ?> map) {
        if (map == null || map.isEmpty()) {
            return false;
        }

        int oldSize = map.size();
        map.entrySet().removeIf(entry -> isEmptyValue(entry.getValue()));
        return map.size() != oldSize;
    }

    /**
     * 将 Collection 中的字符串元素 trim 后返回新列表。
     *
     * <p>
     * null 元素会保留。
     * </p>
     *
     * @param collection 字符串集合
     * @return trim 后的可变 ArrayList
     */
    public static List<String> trimStringToList(Collection<String> collection) {
        List<String> result = new ArrayList<>();
        if (collection == null || collection.isEmpty()) {
            return result;
        }

        for (String item : collection) {
            result.add(item == null ? null : item.trim());
        }
        return result;
    }

    /**
     * 将数组中的字符串元素 trim 后返回新列表。
     *
     * <p>
     * null 元素会保留。
     * </p>
     *
     * @param array 字符串数组
     * @return trim 后的可变 ArrayList
     */
    public static List<String> trimStringToList(String[] array) {
        List<String> result = new ArrayList<>();
        if (array == null || array.length == 0) {
            return result;
        }

        for (String item : array) {
            result.add(item == null ? null : item.trim());
        }
        return result;
    }

    /**
     * 原地 trim List 中的字符串元素。
     *
     * <p>
     * null 元素会保留。
     * </p>
     *
     * @param list 字符串 List
     * @return true 表示执行了处理
     */
    public static boolean trimStringInPlace(List<String> list) {
        if (list == null || list.isEmpty()) {
            return false;
        }

        for (int index = 0; index < list.size(); index++) {
            String item = list.get(index);
            list.set(index, item == null ? null : item.trim());
        }
        return true;
    }

    /**
     * 将字符串 trim 后再移除 null 和空白字符串。
     *
     * @param collection 字符串集合
     * @return 清理后的可变 ArrayList
     */
    public static List<String> trimAndRemoveBlankToList(Collection<String> collection) {
        List<String> result = new ArrayList<>();
        if (collection == null || collection.isEmpty()) {
            return result;
        }

        for (String item : collection) {
            if (item == null) {
                continue;
            }

            String value = item.trim();
            if (!value.isBlank()) {
                result.add(value);
            }
        }
        return result;
    }

    /**
     * 将字符串 trim 后再移除 null 和空白字符串。
     *
     * @param array 字符串数组
     * @return 清理后的可变 ArrayList
     */
    public static List<String> trimAndRemoveBlankToList(String[] array) {
        List<String> result = new ArrayList<>();
        if (array == null || array.length == 0) {
            return result;
        }

        for (String item : array) {
            if (item == null) {
                continue;
            }

            String value = item.trim();
            if (!value.isBlank()) {
                result.add(value);
            }
        }
        return result;
    }

    /**
     * 原地 trim 字符串并移除 null 和空白字符串。
     *
     * @param list 字符串 List
     * @return true 表示 List 发生变化或执行了 trim
     */
    public static boolean trimAndRemoveBlankInPlace(List<String> list) {
        if (list == null || list.isEmpty()) {
            return false;
        }

        int oldSize = list.size();
        for (int index = 0; index < list.size(); index++) {
            String item = list.get(index);
            list.set(index, item == null ? null : item.trim());
        }

        list.removeIf(item -> item == null || item.isBlank());
        return list.size() != oldSize || oldSize > 0;
    }

    /**
     * 判断对象是否为空值。
     *
     * <p>
     * 空值定义：null、空字符串、空白字符串、空 Collection、空 Map、空数组、Optional.empty()。
     * </p>
     *
     * @param value 对象
     * @return true 表示为空值
     */
    public static boolean isEmptyValue(Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof CharSequence charSequence) {
            return charSequence.toString().isBlank();
        }
        if (value instanceof Collection<?> collection) {
            return collection.isEmpty();
        }
        if (value instanceof Map<?, ?> map) {
            return map.isEmpty();
        }
        if (value instanceof Optional<?> optional) {
            return optional.isEmpty();
        }
        if (value.getClass().isArray()) {
            return Array.getLength(value) == 0;
        }
        return false;
    }

    /**
     * 判断对象是否非空值。
     *
     * <p>
     * 空值定义：null、空字符串、空白字符串、空 Collection、空 Map、空数组、Optional.empty()。
     * </p>
     *
     * @param value 对象
     * @return true 表示非空值
     */
    public static boolean isNotEmptyValue(Object value) {
        return !isEmptyValue(value);
    }

    /**
     * null 安全遍历 Collection。
     *
     * @param collection Collection 对象
     * @param consumer   元素消费函数
     * @param <T>        元素类型
     */
    public static <T> void forEach(Collection<? extends T> collection, Consumer<? super T> consumer) {
        Objects.requireNonNull(consumer, "consumer 不能为 null");

        if (collection == null || collection.isEmpty()) {
            return;
        }

        for (T item : collection) {
            consumer.accept(item);
        }
    }

    /**
     * null 安全遍历 Iterable。
     *
     * @param iterable Iterable 对象
     * @param consumer 元素消费函数
     * @param <T>      元素类型
     */
    public static <T> void forEach(Iterable<? extends T> iterable, Consumer<? super T> consumer) {
        Objects.requireNonNull(consumer, "consumer 不能为 null");

        if (iterable == null) {
            return;
        }

        for (T item : iterable) {
            consumer.accept(item);
        }
    }

    /**
     * null 安全遍历 Iterator。
     *
     * <p>
     * 该方法会消费 Iterator。
     * </p>
     *
     * @param iterator Iterator 对象
     * @param consumer 元素消费函数
     * @param <T>      元素类型
     */
    public static <T> void forEach(Iterator<? extends T> iterator, Consumer<? super T> consumer) {
        Objects.requireNonNull(consumer, "consumer 不能为 null");

        if (iterator == null) {
            return;
        }

        while (iterator.hasNext()) {
            consumer.accept(iterator.next());
        }
    }

    /**
     * null 安全遍历数组。
     *
     * @param array    数组
     * @param consumer 元素消费函数
     * @param <T>      元素类型
     */
    public static <T> void forEach(T[] array, Consumer<? super T> consumer) {
        Objects.requireNonNull(consumer, "consumer 不能为 null");

        if (array == null || array.length == 0) {
            return;
        }

        for (T item : array) {
            consumer.accept(item);
        }
    }

    /**
     * null 安全遍历任意数组，支持对象数组和基本类型数组。
     *
     * @param array    数组对象
     * @param consumer 元素消费函数
     */
    public static void forEachArray(Object array, Consumer<Object> consumer) {
        Objects.requireNonNull(consumer, "consumer 不能为 null");

        if (array == null) {
            return;
        }
        if (!array.getClass().isArray()) {
            throw new IllegalArgumentException("参数必须是数组类型");
        }

        int length = Array.getLength(array);
        for (int index = 0; index < length; index++) {
            consumer.accept(Array.get(array, index));
        }
    }

    /**
     * null 安全遍历 Collection 中的非 null 元素。
     *
     * @param collection Collection 对象
     * @param consumer   元素消费函数
     * @param <T>        元素类型
     */
    public static <T> void forEachNotNull(Collection<? extends T> collection, Consumer<? super T> consumer) {
        Objects.requireNonNull(consumer, "consumer 不能为 null");

        if (collection == null || collection.isEmpty()) {
            return;
        }

        for (T item : collection) {
            if (item != null) {
                consumer.accept(item);
            }
        }
    }

    /**
     * null 安全遍历 Iterable 中的非 null 元素。
     *
     * @param iterable Iterable 对象
     * @param consumer 元素消费函数
     * @param <T>      元素类型
     */
    public static <T> void forEachNotNull(Iterable<? extends T> iterable, Consumer<? super T> consumer) {
        Objects.requireNonNull(consumer, "consumer 不能为 null");

        if (iterable == null) {
            return;
        }

        for (T item : iterable) {
            if (item != null) {
                consumer.accept(item);
            }
        }
    }

    /**
     * null 安全遍历 Iterator 中的非 null 元素。
     *
     * <p>
     * 该方法会消费 Iterator。
     * </p>
     *
     * @param iterator Iterator 对象
     * @param consumer 元素消费函数
     * @param <T>      元素类型
     */
    public static <T> void forEachNotNull(Iterator<? extends T> iterator, Consumer<? super T> consumer) {
        Objects.requireNonNull(consumer, "consumer 不能为 null");

        if (iterator == null) {
            return;
        }

        while (iterator.hasNext()) {
            T item = iterator.next();
            if (item != null) {
                consumer.accept(item);
            }
        }
    }

    /**
     * null 安全遍历数组中的非 null 元素。
     *
     * @param array    数组
     * @param consumer 元素消费函数
     * @param <T>      元素类型
     */
    public static <T> void forEachNotNull(T[] array, Consumer<? super T> consumer) {
        Objects.requireNonNull(consumer, "consumer 不能为 null");

        if (array == null || array.length == 0) {
            return;
        }

        for (T item : array) {
            if (item != null) {
                consumer.accept(item);
            }
        }
    }

    /**
     * null 安全遍历 Collection 中的非空值元素。
     *
     * <p>
     * 空值定义使用 isEmptyValue(Object)。
     * </p>
     *
     * @param collection Collection 对象
     * @param consumer   元素消费函数
     * @param <T>        元素类型
     */
    public static <T> void forEachNotEmptyValue(Collection<? extends T> collection, Consumer<? super T> consumer) {
        Objects.requireNonNull(consumer, "consumer 不能为 null");

        if (collection == null || collection.isEmpty()) {
            return;
        }

        for (T item : collection) {
            if (!isEmptyValue(item)) {
                consumer.accept(item);
            }
        }
    }

    /**
     * null 安全遍历数组中的非空值元素。
     *
     * <p>
     * 空值定义使用 isEmptyValue(Object)。
     * </p>
     *
     * @param array    数组
     * @param consumer 元素消费函数
     * @param <T>      元素类型
     */
    public static <T> void forEachNotEmptyValue(T[] array, Consumer<? super T> consumer) {
        Objects.requireNonNull(consumer, "consumer 不能为 null");

        if (array == null || array.length == 0) {
            return;
        }

        for (T item : array) {
            if (!isEmptyValue(item)) {
                consumer.accept(item);
            }
        }
    }

    /**
     * 带下标遍历 List。
     *
     * <p>
     * 下标从 0 开始。
     * </p>
     *
     * @param list     List 对象
     * @param consumer 元素和下标消费函数，第一个参数为元素，第二个参数为下标
     * @param <T>      元素类型
     */
    public static <T> void forEachIndexed(List<? extends T> list, BiConsumer<? super T, Integer> consumer) {
        Objects.requireNonNull(consumer, "consumer 不能为 null");

        if (list == null || list.isEmpty()) {
            return;
        }

        for (int index = 0; index < list.size(); index++) {
            consumer.accept(list.get(index), index);
        }
    }

    /**
     * 带下标遍历 Iterable。
     *
     * <p>
     * 下标从 0 开始。
     * </p>
     *
     * @param iterable Iterable 对象
     * @param consumer 元素和下标消费函数，第一个参数为元素，第二个参数为下标
     * @param <T>      元素类型
     */
    public static <T> void forEachIndexed(Iterable<? extends T> iterable, BiConsumer<? super T, Integer> consumer) {
        Objects.requireNonNull(consumer, "consumer 不能为 null");

        if (iterable == null) {
            return;
        }

        int index = 0;
        for (T item : iterable) {
            consumer.accept(item, index);
            index++;
        }
    }

    /**
     * 带下标遍历 Iterator。
     *
     * <p>
     * 该方法会消费 Iterator；下标从 0 开始。
     * </p>
     *
     * @param iterator Iterator 对象
     * @param consumer 元素和下标消费函数，第一个参数为元素，第二个参数为下标
     * @param <T>      元素类型
     */
    public static <T> void forEachIndexed(Iterator<? extends T> iterator, BiConsumer<? super T, Integer> consumer) {
        Objects.requireNonNull(consumer, "consumer 不能为 null");

        if (iterator == null) {
            return;
        }

        int index = 0;
        while (iterator.hasNext()) {
            consumer.accept(iterator.next(), index);
            index++;
        }
    }

    /**
     * 带下标遍历数组。
     *
     * <p>
     * 下标从 0 开始。
     * </p>
     *
     * @param array    数组
     * @param consumer 元素和下标消费函数，第一个参数为元素，第二个参数为下标
     * @param <T>      元素类型
     */
    public static <T> void forEachIndexed(T[] array, BiConsumer<? super T, Integer> consumer) {
        Objects.requireNonNull(consumer, "consumer 不能为 null");

        if (array == null || array.length == 0) {
            return;
        }

        for (int index = 0; index < array.length; index++) {
            consumer.accept(array[index], index);
        }
    }

    /**
     * 带下标遍历任意数组，支持对象数组和基本类型数组。
     *
     * @param array    数组对象
     * @param consumer 元素和下标消费函数，第一个参数为元素，第二个参数为下标
     */
    public static void forEachArrayIndexed(Object array, BiConsumer<Object, Integer> consumer) {
        Objects.requireNonNull(consumer, "consumer 不能为 null");

        if (array == null) {
            return;
        }
        if (!array.getClass().isArray()) {
            throw new IllegalArgumentException("参数必须是数组类型");
        }

        int length = Array.getLength(array);
        for (int index = 0; index < length; index++) {
            consumer.accept(Array.get(array, index), index);
        }
    }

    /**
     * 反向遍历 List。
     *
     * <p>
     * 从最后一个元素遍历到第一个元素。
     * </p>
     *
     * @param list     List 对象
     * @param consumer 元素消费函数
     * @param <T>      元素类型
     */
    public static <T> void forEachReverse(List<? extends T> list, Consumer<? super T> consumer) {
        Objects.requireNonNull(consumer, "consumer 不能为 null");

        if (list == null || list.isEmpty()) {
            return;
        }

        for (int index = list.size() - 1; index >= 0; index--) {
            consumer.accept(list.get(index));
        }
    }

    /**
     * 反向遍历数组。
     *
     * <p>
     * 从最后一个元素遍历到第一个元素。
     * </p>
     *
     * @param array    数组
     * @param consumer 元素消费函数
     * @param <T>      元素类型
     */
    public static <T> void forEachReverse(T[] array, Consumer<? super T> consumer) {
        Objects.requireNonNull(consumer, "consumer 不能为 null");

        if (array == null || array.length == 0) {
            return;
        }

        for (int index = array.length - 1; index >= 0; index--) {
            consumer.accept(array[index]);
        }
    }

    /**
     * 反向带下标遍历 List。
     *
     * <p>
     * 下标为元素原始下标。
     * </p>
     *
     * @param list     List 对象
     * @param consumer 元素和下标消费函数，第一个参数为元素，第二个参数为下标
     * @param <T>      元素类型
     */
    public static <T> void forEachReverseIndexed(List<? extends T> list, BiConsumer<? super T, Integer> consumer) {
        Objects.requireNonNull(consumer, "consumer 不能为 null");

        if (list == null || list.isEmpty()) {
            return;
        }

        for (int index = list.size() - 1; index >= 0; index--) {
            consumer.accept(list.get(index), index);
        }
    }

    /**
     * 反向带下标遍历数组。
     *
     * <p>
     * 下标为元素原始下标。
     * </p>
     *
     * @param array    数组
     * @param consumer 元素和下标消费函数，第一个参数为元素，第二个参数为下标
     * @param <T>      元素类型
     */
    public static <T> void forEachReverseIndexed(T[] array, BiConsumer<? super T, Integer> consumer) {
        Objects.requireNonNull(consumer, "consumer 不能为 null");

        if (array == null || array.length == 0) {
            return;
        }

        for (int index = array.length - 1; index >= 0; index--) {
            consumer.accept(array[index], index);
        }
    }

    /**
     * 遍历 Collection，直到条件返回 false。
     *
     * <p>
     * predicate 返回 true 时继续遍历，返回 false 时停止遍历。
     * </p>
     *
     * @param collection Collection 对象
     * @param predicate  遍历控制函数
     * @param <T>        元素类型
     * @return 已处理元素数量
     */
    public static <T> int forEachWhile(Collection<? extends T> collection, Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate 不能为 null");

        if (collection == null || collection.isEmpty()) {
            return 0;
        }

        int count = 0;
        for (T item : collection) {
            if (!predicate.test(item)) {
                break;
            }
            count++;
        }
        return count;
    }

    /**
     * 遍历 Iterable，直到条件返回 false。
     *
     * <p>
     * predicate 返回 true 时继续遍历，返回 false 时停止遍历。
     * </p>
     *
     * @param iterable  Iterable 对象
     * @param predicate 遍历控制函数
     * @param <T>       元素类型
     * @return 已处理元素数量
     */
    public static <T> int forEachWhile(Iterable<? extends T> iterable, Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate 不能为 null");

        if (iterable == null) {
            return 0;
        }

        int count = 0;
        for (T item : iterable) {
            if (!predicate.test(item)) {
                break;
            }
            count++;
        }
        return count;
    }

    /**
     * 遍历 Iterator，直到条件返回 false。
     *
     * <p>
     * 该方法会消费 Iterator；predicate 返回 true 时继续遍历，返回 false 时停止遍历。
     * </p>
     *
     * @param iterator  Iterator 对象
     * @param predicate 遍历控制函数
     * @param <T>       元素类型
     * @return 已处理元素数量
     */
    public static <T> int forEachWhile(Iterator<? extends T> iterator, Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate 不能为 null");

        if (iterator == null) {
            return 0;
        }

        int count = 0;
        while (iterator.hasNext()) {
            if (!predicate.test(iterator.next())) {
                break;
            }
            count++;
        }
        return count;
    }

    /**
     * 遍历数组，直到条件返回 false。
     *
     * <p>
     * predicate 返回 true 时继续遍历，返回 false 时停止遍历。
     * </p>
     *
     * @param array     数组
     * @param predicate 遍历控制函数
     * @param <T>       元素类型
     * @return 已处理元素数量
     */
    public static <T> int forEachWhile(T[] array, Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate 不能为 null");

        if (array == null || array.length == 0) {
            return 0;
        }

        int count = 0;
        for (T item : array) {
            if (!predicate.test(item)) {
                break;
            }
            count++;
        }
        return count;
    }

    /**
     * 带下标遍历 List，直到条件返回 false。
     *
     * <p>
     * predicate 返回 true 时继续遍历，返回 false 时停止遍历。
     * </p>
     *
     * @param list      List 对象
     * @param predicate 遍历控制函数，第一个参数为元素，第二个参数为下标
     * @param <T>       元素类型
     * @return 已处理元素数量
     */
    public static <T> int forEachIndexedWhile(List<? extends T> list, BiPredicate<? super T, Integer> predicate) {
        Objects.requireNonNull(predicate, "predicate 不能为 null");

        if (list == null || list.isEmpty()) {
            return 0;
        }

        int count = 0;
        for (int index = 0; index < list.size(); index++) {
            if (!predicate.test(list.get(index), index)) {
                break;
            }
            count++;
        }
        return count;
    }

    /**
     * 带下标遍历 Iterable，直到条件返回 false。
     *
     * <p>
     * predicate 返回 true 时继续遍历，返回 false 时停止遍历。
     * </p>
     *
     * @param iterable  Iterable 对象
     * @param predicate 遍历控制函数，第一个参数为元素，第二个参数为下标
     * @param <T>       元素类型
     * @return 已处理元素数量
     */
    public static <T> int forEachIndexedWhile(Iterable<? extends T> iterable, BiPredicate<? super T, Integer> predicate) {
        Objects.requireNonNull(predicate, "predicate 不能为 null");

        if (iterable == null) {
            return 0;
        }

        int index = 0;
        int count = 0;
        for (T item : iterable) {
            if (!predicate.test(item, index)) {
                break;
            }
            index++;
            count++;
        }
        return count;
    }

    /**
     * 带下标遍历数组，直到条件返回 false。
     *
     * <p>
     * predicate 返回 true 时继续遍历，返回 false 时停止遍历。
     * </p>
     *
     * @param array     数组
     * @param predicate 遍历控制函数，第一个参数为元素，第二个参数为下标
     * @param <T>       元素类型
     * @return 已处理元素数量
     */
    public static <T> int forEachIndexedWhile(T[] array, BiPredicate<? super T, Integer> predicate) {
        Objects.requireNonNull(predicate, "predicate 不能为 null");

        if (array == null || array.length == 0) {
            return 0;
        }

        int count = 0;
        for (int index = 0; index < array.length; index++) {
            if (!predicate.test(array[index], index)) {
                break;
            }
            count++;
        }
        return count;
    }

    /**
     * 安全遍历 Collection，并隔离单个元素处理异常。
     *
     * <p>
     * consumer 抛出 RuntimeException 时，不中断后续元素处理。
     * </p>
     *
     * @param collection        Collection 对象
     * @param consumer          元素消费函数
     * @param exceptionConsumer 异常消费函数，第一个参数为元素，第二个参数为异常
     * @param <T>               元素类型
     * @return 成功处理的元素数量
     */
    public static <T> int forEachSafely(Collection<? extends T> collection,
                                        Consumer<? super T> consumer,
                                        BiConsumer<? super T, ? super RuntimeException> exceptionConsumer) {
        Objects.requireNonNull(consumer, "consumer 不能为 null");

        if (collection == null || collection.isEmpty()) {
            return 0;
        }

        int successCount = 0;
        for (T item : collection) {
            try {
                consumer.accept(item);
                successCount++;
            } catch (RuntimeException exception) {
                if (exceptionConsumer != null) {
                    exceptionConsumer.accept(item, exception);
                }
            }
        }
        return successCount;
    }

    /**
     * 安全遍历 Iterable，并隔离单个元素处理异常。
     *
     * <p>
     * consumer 抛出 RuntimeException 时，不中断后续元素处理。
     * </p>
     *
     * @param iterable          Iterable 对象
     * @param consumer          元素消费函数
     * @param exceptionConsumer 异常消费函数，第一个参数为元素，第二个参数为异常
     * @param <T>               元素类型
     * @return 成功处理的元素数量
     */
    public static <T> int forEachSafely(Iterable<? extends T> iterable,
                                        Consumer<? super T> consumer,
                                        BiConsumer<? super T, ? super RuntimeException> exceptionConsumer) {
        Objects.requireNonNull(consumer, "consumer 不能为 null");

        if (iterable == null) {
            return 0;
        }

        int successCount = 0;
        for (T item : iterable) {
            try {
                consumer.accept(item);
                successCount++;
            } catch (RuntimeException exception) {
                if (exceptionConsumer != null) {
                    exceptionConsumer.accept(item, exception);
                }
            }
        }
        return successCount;
    }

    /**
     * 安全遍历 Iterator，并隔离单个元素处理异常。
     *
     * <p>
     * 该方法会消费 Iterator；consumer 抛出 RuntimeException 时，不中断后续元素处理。
     * </p>
     *
     * @param iterator          Iterator 对象
     * @param consumer          元素消费函数
     * @param exceptionConsumer 异常消费函数，第一个参数为元素，第二个参数为异常
     * @param <T>               元素类型
     * @return 成功处理的元素数量
     */
    public static <T> int forEachSafely(Iterator<? extends T> iterator,
                                        Consumer<? super T> consumer,
                                        BiConsumer<? super T, ? super RuntimeException> exceptionConsumer) {
        Objects.requireNonNull(consumer, "consumer 不能为 null");

        if (iterator == null) {
            return 0;
        }

        int successCount = 0;
        while (iterator.hasNext()) {
            T item = iterator.next();
            try {
                consumer.accept(item);
                successCount++;
            } catch (RuntimeException exception) {
                if (exceptionConsumer != null) {
                    exceptionConsumer.accept(item, exception);
                }
            }
        }
        return successCount;
    }

    /**
     * 安全遍历数组，并隔离单个元素处理异常。
     *
     * <p>
     * consumer 抛出 RuntimeException 时，不中断后续元素处理。
     * </p>
     *
     * @param array             数组
     * @param consumer          元素消费函数
     * @param exceptionConsumer 异常消费函数，第一个参数为元素，第二个参数为异常
     * @param <T>               元素类型
     * @return 成功处理的元素数量
     */
    public static <T> int forEachSafely(T[] array,
                                        Consumer<? super T> consumer,
                                        BiConsumer<? super T, ? super RuntimeException> exceptionConsumer) {
        Objects.requireNonNull(consumer, "consumer 不能为 null");

        if (array == null || array.length == 0) {
            return 0;
        }

        int successCount = 0;
        for (T item : array) {
            try {
                consumer.accept(item);
                successCount++;
            } catch (RuntimeException exception) {
                if (exceptionConsumer != null) {
                    exceptionConsumer.accept(item, exception);
                }
            }
        }
        return successCount;
    }

    /**
     * 带下标安全遍历 List，并隔离单个元素处理异常。
     *
     * @param list              List 对象
     * @param consumer          元素和下标消费函数，第一个参数为元素，第二个参数为下标
     * @param exceptionConsumer 异常消费函数，第一个参数为元素，第二个参数为异常
     * @param <T>               元素类型
     * @return 成功处理的元素数量
     */
    public static <T> int forEachIndexedSafely(List<? extends T> list,
                                               BiConsumer<? super T, Integer> consumer,
                                               BiConsumer<? super T, ? super RuntimeException> exceptionConsumer) {
        Objects.requireNonNull(consumer, "consumer 不能为 null");

        if (list == null || list.isEmpty()) {
            return 0;
        }

        int successCount = 0;
        for (int index = 0; index < list.size(); index++) {
            T item = list.get(index);
            try {
                consumer.accept(item, index);
                successCount++;
            } catch (RuntimeException exception) {
                if (exceptionConsumer != null) {
                    exceptionConsumer.accept(item, exception);
                }
            }
        }
        return successCount;
    }

    /**
     * 带下标安全遍历数组，并隔离单个元素处理异常。
     *
     * @param array             数组
     * @param consumer          元素和下标消费函数，第一个参数为元素，第二个参数为下标
     * @param exceptionConsumer 异常消费函数，第一个参数为元素，第二个参数为异常
     * @param <T>               元素类型
     * @return 成功处理的元素数量
     */
    public static <T> int forEachIndexedSafely(T[] array,
                                               BiConsumer<? super T, Integer> consumer,
                                               BiConsumer<? super T, ? super RuntimeException> exceptionConsumer) {
        Objects.requireNonNull(consumer, "consumer 不能为 null");

        if (array == null || array.length == 0) {
            return 0;
        }

        int successCount = 0;
        for (int index = 0; index < array.length; index++) {
            T item = array[index];
            try {
                consumer.accept(item, index);
                successCount++;
            } catch (RuntimeException exception) {
                if (exceptionConsumer != null) {
                    exceptionConsumer.accept(item, exception);
                }
            }
        }
        return successCount;
    }

    /**
     * 遍历 Collection，并返回处理失败的元素列表。
     *
     * <p>
     * consumer 抛出 RuntimeException 时，该元素会被加入失败列表。
     * </p>
     *
     * @param collection Collection 对象
     * @param consumer   元素消费函数
     * @param <T>        元素类型
     * @return 处理失败的元素列表
     */
    public static <T> List<T> forEachCollectFailures(Collection<? extends T> collection, Consumer<? super T> consumer) {
        Objects.requireNonNull(consumer, "consumer 不能为 null");

        List<T> failures = new ArrayList<>();
        if (collection == null || collection.isEmpty()) {
            return failures;
        }

        for (T item : collection) {
            try {
                consumer.accept(item);
            } catch (RuntimeException ignored) {
                failures.add(item);
            }
        }
        return failures;
    }

    /**
     * 遍历 Iterable，并返回处理失败的元素列表。
     *
     * <p>
     * consumer 抛出 RuntimeException 时，该元素会被加入失败列表。
     * </p>
     *
     * @param iterable Iterable 对象
     * @param consumer 元素消费函数
     * @param <T>      元素类型
     * @return 处理失败的元素列表
     */
    public static <T> List<T> forEachCollectFailures(Iterable<? extends T> iterable, Consumer<? super T> consumer) {
        Objects.requireNonNull(consumer, "consumer 不能为 null");

        List<T> failures = new ArrayList<>();
        if (iterable == null) {
            return failures;
        }

        for (T item : iterable) {
            try {
                consumer.accept(item);
            } catch (RuntimeException ignored) {
                failures.add(item);
            }
        }
        return failures;
    }

    /**
     * 遍历数组，并返回处理失败的元素列表。
     *
     * <p>
     * consumer 抛出 RuntimeException 时，该元素会被加入失败列表。
     * </p>
     *
     * @param array    数组
     * @param consumer 元素消费函数
     * @param <T>      元素类型
     * @return 处理失败的元素列表
     */
    public static <T> List<T> forEachCollectFailures(T[] array, Consumer<? super T> consumer) {
        Objects.requireNonNull(consumer, "consumer 不能为 null");

        List<T> failures = new ArrayList<>();
        if (array == null || array.length == 0) {
            return failures;
        }

        for (T item : array) {
            try {
                consumer.accept(item);
            } catch (RuntimeException ignored) {
                failures.add(item);
            }
        }
        return failures;
    }

    /**
     * 遍历 Map Entry。
     *
     * @param map      Map 对象
     * @param consumer Entry 消费函数
     * @param <K>      Key 类型
     * @param <V>      Value 类型
     */
    public static <K, V> void forEachEntry(Map<? extends K, ? extends V> map,
                                           Consumer<? super Map.Entry<? extends K, ? extends V>> consumer) {
        Objects.requireNonNull(consumer, "consumer 不能为 null");

        if (map == null || map.isEmpty()) {
            return;
        }

        for (Map.Entry<? extends K, ? extends V> entry : map.entrySet()) {
            consumer.accept(entry);
        }
    }

    /**
     * 遍历 Map Entry。
     *
     * @param map      Map 对象
     * @param consumer Key 和 Value 消费函数
     * @param <K>      Key 类型
     * @param <V>      Value 类型
     */
    public static <K, V> void forEachEntry(Map<? extends K, ? extends V> map,
                                           BiConsumer<? super K, ? super V> consumer) {
        Objects.requireNonNull(consumer, "consumer 不能为 null");

        if (map == null || map.isEmpty()) {
            return;
        }

        for (Map.Entry<? extends K, ? extends V> entry : map.entrySet()) {
            consumer.accept(entry.getKey(), entry.getValue());
        }
    }

    /**
     * 遍历 Map Key。
     *
     * @param map      Map 对象
     * @param consumer Key 消费函数
     * @param <K>      Key 类型
     */
    public static <K> void forEachKey(Map<? extends K, ?> map, Consumer<? super K> consumer) {
        Objects.requireNonNull(consumer, "consumer 不能为 null");

        if (map == null || map.isEmpty()) {
            return;
        }

        for (K key : map.keySet()) {
            consumer.accept(key);
        }
    }

    /**
     * 遍历 Map Value。
     *
     * @param map      Map 对象
     * @param consumer Value 消费函数
     * @param <V>      Value 类型
     */
    public static <V> void forEachValue(Map<?, ? extends V> map, Consumer<? super V> consumer) {
        Objects.requireNonNull(consumer, "consumer 不能为 null");

        if (map == null || map.isEmpty()) {
            return;
        }

        for (V value : map.values()) {
            consumer.accept(value);
        }
    }

    /**
     * 带下标遍历 Map Entry。
     *
     * <p>
     * 下标从 0 开始，顺序取决于 Map 本身的遍历顺序。
     * </p>
     *
     * @param map      Map 对象
     * @param consumer Entry 和下标消费函数，第一个参数为 Entry，第二个参数为下标
     * @param <K>      Key 类型
     * @param <V>      Value 类型
     */
    public static <K, V> void forEachEntryIndexed(Map<? extends K, ? extends V> map,
                                                  BiConsumer<? super Map.Entry<? extends K, ? extends V>, Integer> consumer) {
        Objects.requireNonNull(consumer, "consumer 不能为 null");

        if (map == null || map.isEmpty()) {
            return;
        }

        int index = 0;
        for (Map.Entry<? extends K, ? extends V> entry : map.entrySet()) {
            consumer.accept(entry, index);
            index++;
        }
    }

    /**
     * 带下标遍历 Map Key。
     *
     * <p>
     * 下标从 0 开始，顺序取决于 Map 本身的遍历顺序。
     * </p>
     *
     * @param map      Map 对象
     * @param consumer Key 和下标消费函数，第一个参数为 Key，第二个参数为下标
     * @param <K>      Key 类型
     */
    public static <K> void forEachKeyIndexed(Map<? extends K, ?> map, BiConsumer<? super K, Integer> consumer) {
        Objects.requireNonNull(consumer, "consumer 不能为 null");

        if (map == null || map.isEmpty()) {
            return;
        }

        int index = 0;
        for (K key : map.keySet()) {
            consumer.accept(key, index);
            index++;
        }
    }

    /**
     * 带下标遍历 Map Value。
     *
     * <p>
     * 下标从 0 开始，顺序取决于 Map 本身的遍历顺序。
     * </p>
     *
     * @param map      Map 对象
     * @param consumer Value 和下标消费函数，第一个参数为 Value，第二个参数为下标
     * @param <V>      Value 类型
     */
    public static <V> void forEachValueIndexed(Map<?, ? extends V> map, BiConsumer<? super V, Integer> consumer) {
        Objects.requireNonNull(consumer, "consumer 不能为 null");

        if (map == null || map.isEmpty()) {
            return;
        }

        int index = 0;
        for (V value : map.values()) {
            consumer.accept(value, index);
            index++;
        }
    }

    /**
     * 安全遍历 Map Entry，并隔离单个 Entry 处理异常。
     *
     * <p>
     * consumer 抛出 RuntimeException 时，不中断后续 Entry 处理。
     * </p>
     *
     * @param map               Map 对象
     * @param consumer          Key 和 Value 消费函数
     * @param exceptionConsumer 异常消费函数，第一个参数为 Entry，第二个参数为异常
     * @param <K>               Key 类型
     * @param <V>               Value 类型
     * @return 成功处理的 Entry 数量
     */
    public static <K, V> int forEachEntrySafely(Map<? extends K, ? extends V> map,
                                                BiConsumer<? super K, ? super V> consumer,
                                                BiConsumer<? super Map.Entry<? extends K, ? extends V>, ? super RuntimeException> exceptionConsumer) {
        Objects.requireNonNull(consumer, "consumer 不能为 null");

        if (map == null || map.isEmpty()) {
            return 0;
        }

        int successCount = 0;
        for (Map.Entry<? extends K, ? extends V> entry : map.entrySet()) {
            try {
                consumer.accept(entry.getKey(), entry.getValue());
                successCount++;
            } catch (RuntimeException exception) {
                if (exceptionConsumer != null) {
                    exceptionConsumer.accept(entry, exception);
                }
            }
        }
        return successCount;
    }

    /**
     * 安全遍历 Map Key，并隔离单个 Key 处理异常。
     *
     * @param map               Map 对象
     * @param consumer          Key 消费函数
     * @param exceptionConsumer 异常消费函数，第一个参数为 Key，第二个参数为异常
     * @param <K>               Key 类型
     * @return 成功处理的 Key 数量
     */
    public static <K> int forEachKeySafely(Map<? extends K, ?> map,
                                           Consumer<? super K> consumer,
                                           BiConsumer<? super K, ? super RuntimeException> exceptionConsumer) {
        Objects.requireNonNull(consumer, "consumer 不能为 null");

        if (map == null || map.isEmpty()) {
            return 0;
        }

        int successCount = 0;
        for (K key : map.keySet()) {
            try {
                consumer.accept(key);
                successCount++;
            } catch (RuntimeException exception) {
                if (exceptionConsumer != null) {
                    exceptionConsumer.accept(key, exception);
                }
            }
        }
        return successCount;
    }

    /**
     * 安全遍历 Map Value，并隔离单个 Value 处理异常。
     *
     * @param map               Map 对象
     * @param consumer          Value 消费函数
     * @param exceptionConsumer 异常消费函数，第一个参数为 Value，第二个参数为异常
     * @param <V>               Value 类型
     * @return 成功处理的 Value 数量
     */
    public static <V> int forEachValueSafely(Map<?, ? extends V> map,
                                             Consumer<? super V> consumer,
                                             BiConsumer<? super V, ? super RuntimeException> exceptionConsumer) {
        Objects.requireNonNull(consumer, "consumer 不能为 null");

        if (map == null || map.isEmpty()) {
            return 0;
        }

        int successCount = 0;
        for (V value : map.values()) {
            try {
                consumer.accept(value);
                successCount++;
            } catch (RuntimeException exception) {
                if (exceptionConsumer != null) {
                    exceptionConsumer.accept(value, exception);
                }
            }
        }
        return successCount;
    }

    /**
     * 对 Collection 中每个元素执行替换处理，并返回新 ArrayList。
     *
     * <p>
     * 该方法适合安全地对元素做逐个转换，不修改原集合。
     * </p>
     *
     * @param collection Collection 对象
     * @param operator   元素处理函数
     * @param <T>        元素类型
     * @return 处理后的可变 ArrayList
     */
    public static <T> List<T> replaceEach(Collection<? extends T> collection, UnaryOperator<T> operator) {
        Objects.requireNonNull(operator, "operator 不能为 null");

        List<T> result = new ArrayList<>();
        if (collection == null || collection.isEmpty()) {
            return result;
        }

        for (T item : collection) {
            result.add(operator.apply(item));
        }
        return result;
    }

    /**
     * 对 Iterable 中每个元素执行替换处理，并返回新 ArrayList。
     *
     * @param iterable Iterable 对象
     * @param operator 元素处理函数
     * @param <T>      元素类型
     * @return 处理后的可变 ArrayList
     */
    public static <T> List<T> replaceEach(Iterable<? extends T> iterable, UnaryOperator<T> operator) {
        Objects.requireNonNull(operator, "operator 不能为 null");

        List<T> result = new ArrayList<>();
        if (iterable == null) {
            return result;
        }

        for (T item : iterable) {
            result.add(operator.apply(item));
        }
        return result;
    }

    /**
     * 对数组中每个元素执行替换处理，并返回新 ArrayList。
     *
     * @param array    数组
     * @param operator 元素处理函数
     * @param <T>      元素类型
     * @return 处理后的可变 ArrayList
     */
    public static <T> List<T> replaceEach(T[] array, UnaryOperator<T> operator) {
        Objects.requireNonNull(operator, "operator 不能为 null");

        List<T> result = new ArrayList<>();
        if (array == null || array.length == 0) {
            return result;
        }

        for (T item : array) {
            result.add(operator.apply(item));
        }
        return result;
    }

    /**
     * 原地替换 List 中每个元素。
     *
     * @param list     List 对象
     * @param operator 元素处理函数
     * @param <T>      元素类型
     * @return true 表示执行了替换
     */
    public static <T> boolean replaceEachInPlace(List<T> list, UnaryOperator<T> operator) {
        Objects.requireNonNull(operator, "operator 不能为 null");

        if (list == null || list.isEmpty()) {
            return false;
        }

        list.replaceAll(operator);
        return true;
    }

    /**
     * 原地替换数组中每个元素。
     *
     * @param array    数组
     * @param operator 元素处理函数
     * @param <T>      元素类型
     * @return true 表示执行了替换
     */
    public static <T> boolean replaceEachInPlace(T[] array, UnaryOperator<T> operator) {
        Objects.requireNonNull(operator, "operator 不能为 null");

        if (array == null || array.length == 0) {
            return false;
        }

        for (int index = 0; index < array.length; index++) {
            array[index] = operator.apply(array[index]);
        }
        return true;
    }

    /**
     * 安全原地替换 List 中每个元素，并隔离单个元素处理异常。
     *
     * <p>
     * operator 抛出 RuntimeException 时，当前元素保持原值，并继续处理后续元素。
     * </p>
     *
     * @param list              List 对象
     * @param operator          元素处理函数
     * @param exceptionConsumer 异常消费函数，第一个参数为元素，第二个参数为异常
     * @param <T>               元素类型
     * @return 成功替换的元素数量
     */
    public static <T> int replaceEachInPlaceSafely(List<T> list,
                                                   UnaryOperator<T> operator,
                                                   BiConsumer<? super T, ? super RuntimeException> exceptionConsumer) {
        Objects.requireNonNull(operator, "operator 不能为 null");

        if (list == null || list.isEmpty()) {
            return 0;
        }

        int successCount = 0;
        for (int index = 0; index < list.size(); index++) {
            T item = list.get(index);
            try {
                list.set(index, operator.apply(item));
                successCount++;
            } catch (RuntimeException exception) {
                if (exceptionConsumer != null) {
                    exceptionConsumer.accept(item, exception);
                }
            }
        }
        return successCount;
    }

    /**
     * 安全原地替换数组中每个元素，并隔离单个元素处理异常。
     *
     * <p>
     * operator 抛出 RuntimeException 时，当前元素保持原值，并继续处理后续元素。
     * </p>
     *
     * @param array             数组
     * @param operator          元素处理函数
     * @param exceptionConsumer 异常消费函数，第一个参数为元素，第二个参数为异常
     * @param <T>               元素类型
     * @return 成功替换的元素数量
     */
    public static <T> int replaceEachInPlaceSafely(T[] array,
                                                   UnaryOperator<T> operator,
                                                   BiConsumer<? super T, ? super RuntimeException> exceptionConsumer) {
        Objects.requireNonNull(operator, "operator 不能为 null");

        if (array == null || array.length == 0) {
            return 0;
        }

        int successCount = 0;
        for (int index = 0; index < array.length; index++) {
            T item = array[index];
            try {
                array[index] = operator.apply(item);
                successCount++;
            } catch (RuntimeException exception) {
                if (exceptionConsumer != null) {
                    exceptionConsumer.accept(item, exception);
                }
            }
        }
        return successCount;
    }

    /**
     * 遍历 Collection 并统计成功处理数量。
     *
     * @param collection Collection 对象
     * @param consumer   元素消费函数
     * @param <T>        元素类型
     * @return 处理数量
     */
    public static <T> int forEachCount(Collection<? extends T> collection, Consumer<? super T> consumer) {
        Objects.requireNonNull(consumer, "consumer 不能为 null");

        if (collection == null || collection.isEmpty()) {
            return 0;
        }

        int count = 0;
        for (T item : collection) {
            consumer.accept(item);
            count++;
        }
        return count;
    }

    /**
     * 遍历 Iterable 并统计成功处理数量。
     *
     * @param iterable Iterable 对象
     * @param consumer 元素消费函数
     * @param <T>      元素类型
     * @return 处理数量
     */
    public static <T> int forEachCount(Iterable<? extends T> iterable, Consumer<? super T> consumer) {
        Objects.requireNonNull(consumer, "consumer 不能为 null");

        if (iterable == null) {
            return 0;
        }

        int count = 0;
        for (T item : iterable) {
            consumer.accept(item);
            count++;
        }
        return count;
    }

    /**
     * 遍历 Iterator 并统计成功处理数量。
     *
     * <p>
     * 该方法会消费 Iterator。
     * </p>
     *
     * @param iterator Iterator 对象
     * @param consumer 元素消费函数
     * @param <T>      元素类型
     * @return 处理数量
     */
    public static <T> int forEachCount(Iterator<? extends T> iterator, Consumer<? super T> consumer) {
        Objects.requireNonNull(consumer, "consumer 不能为 null");

        if (iterator == null) {
            return 0;
        }

        int count = 0;
        while (iterator.hasNext()) {
            consumer.accept(iterator.next());
            count++;
        }
        return count;
    }

    /**
     * 遍历数组并统计成功处理数量。
     *
     * @param array    数组
     * @param consumer 元素消费函数
     * @param <T>      元素类型
     * @return 处理数量
     */
    public static <T> int forEachCount(T[] array, Consumer<? super T> consumer) {
        Objects.requireNonNull(consumer, "consumer 不能为 null");

        if (array == null || array.length == 0) {
            return 0;
        }

        int count = 0;
        for (T item : array) {
            consumer.accept(item);
            count++;
        }
        return count;
    }

    /**
     * 断言 Collection 不为 null。
     *
     * @param collection Collection 对象
     * @param message    异常消息
     * @param <C>        Collection 类型
     * @return 原 Collection
     */
    public static <C extends Collection<?>> C requireNotNull(C collection, String message) {
        return Objects.requireNonNull(collection, message);
    }

    /**
     * 断言 Collection 不为 null。
     *
     * @param collection Collection 对象
     * @param <C>        Collection 类型
     * @return 原 Collection
     */
    public static <C extends Collection<?>> C requireNotNull(C collection) {
        return requireNotNull(collection, "collection 不能为 null");
    }

    /**
     * 断言 Map 不为 null。
     *
     * @param map     Map 对象
     * @param message 异常消息
     * @param <M>     Map 类型
     * @return 原 Map
     */
    public static <M extends Map<?, ?>> M requireNotNull(M map, String message) {
        return Objects.requireNonNull(map, message);
    }

    /**
     * 断言 Map 不为 null。
     *
     * @param map Map 对象
     * @param <M> Map 类型
     * @return 原 Map
     */
    public static <M extends Map<?, ?>> M requireNotNull(M map) {
        return requireNotNull(map, "map 不能为 null");
    }

    /**
     * 断言数组不为 null。
     *
     * @param array   数组
     * @param message 异常消息
     * @param <T>     元素类型
     * @return 原数组
     */
    public static <T> T[] requireNotNull(T[] array, String message) {
        return Objects.requireNonNull(array, message);
    }

    /**
     * 断言数组不为 null。
     *
     * @param array 数组
     * @param <T>   元素类型
     * @return 原数组
     */
    public static <T> T[] requireNotNull(T[] array) {
        return requireNotNull(array, "array 不能为 null");
    }

    /**
     * 断言 Collection 不为空。
     *
     * @param collection Collection 对象
     * @param message    异常消息
     * @param <C>        Collection 类型
     * @return 原 Collection
     */
    public static <C extends Collection<?>> C requireNotEmpty(C collection, String message) {
        if (collection == null || collection.isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return collection;
    }

    /**
     * 断言 Collection 不为空。
     *
     * @param collection Collection 对象
     * @param <C>        Collection 类型
     * @return 原 Collection
     */
    public static <C extends Collection<?>> C requireNotEmpty(C collection) {
        return requireNotEmpty(collection, "collection 不能为空");
    }

    /**
     * 断言 Map 不为空。
     *
     * @param map     Map 对象
     * @param message 异常消息
     * @param <M>     Map 类型
     * @return 原 Map
     */
    public static <M extends Map<?, ?>> M requireNotEmpty(M map, String message) {
        if (map == null || map.isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return map;
    }

    /**
     * 断言 Map 不为空。
     *
     * @param map Map 对象
     * @param <M> Map 类型
     * @return 原 Map
     */
    public static <M extends Map<?, ?>> M requireNotEmpty(M map) {
        return requireNotEmpty(map, "map 不能为空");
    }

    /**
     * 断言数组不为空。
     *
     * @param array   数组
     * @param message 异常消息
     * @param <T>     元素类型
     * @return 原数组
     */
    public static <T> T[] requireNotEmpty(T[] array, String message) {
        if (array == null || array.length == 0) {
            throw new IllegalArgumentException(message);
        }
        return array;
    }

    /**
     * 断言数组不为空。
     *
     * @param array 数组
     * @param <T>   元素类型
     * @return 原数组
     */
    public static <T> T[] requireNotEmpty(T[] array) {
        return requireNotEmpty(array, "array 不能为空");
    }

    /**
     * 断言任意数组对象不为空，支持对象数组和基本类型数组。
     *
     * @param array   数组对象
     * @param message 异常消息
     * @return 原数组对象
     */
    public static Object requireArrayNotEmpty(Object array, String message) {
        if (array == null) {
            throw new IllegalArgumentException(message);
        }
        if (!array.getClass().isArray()) {
            throw new IllegalArgumentException("参数必须是数组类型");
        }
        if (Array.getLength(array) == 0) {
            throw new IllegalArgumentException(message);
        }
        return array;
    }

    /**
     * 断言任意数组对象不为空，支持对象数组和基本类型数组。
     *
     * @param array 数组对象
     * @return 原数组对象
     */
    public static Object requireArrayNotEmpty(Object array) {
        return requireArrayNotEmpty(array, "array 不能为空");
    }

    /**
     * 断言 Collection 为空。
     *
     * @param collection Collection 对象
     * @param message    异常消息
     */
    public static void requireEmpty(Collection<?> collection, String message) {
        if (collection != null && !collection.isEmpty()) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * 断言 Collection 为空。
     *
     * @param collection Collection 对象
     */
    public static void requireEmpty(Collection<?> collection) {
        requireEmpty(collection, "collection 必须为空");
    }

    /**
     * 断言 Map 为空。
     *
     * @param map     Map 对象
     * @param message 异常消息
     */
    public static void requireEmpty(Map<?, ?> map, String message) {
        if (map != null && !map.isEmpty()) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * 断言 Map 为空。
     *
     * @param map Map 对象
     */
    public static void requireEmpty(Map<?, ?> map) {
        requireEmpty(map, "map 必须为空");
    }

    /**
     * 断言数组为空。
     *
     * @param array   数组
     * @param message 异常消息
     */
    public static void requireEmpty(Object[] array, String message) {
        if (array != null && array.length > 0) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * 断言数组为空。
     *
     * @param array 数组
     */
    public static void requireEmpty(Object[] array) {
        requireEmpty(array, "array 必须为空");
    }

    /**
     * 断言 Collection 大小等于指定值。
     *
     * @param collection   Collection 对象
     * @param expectedSize 期望大小
     * @param message      异常消息
     * @param <C>          Collection 类型
     * @return 原 Collection
     */
    public static <C extends Collection<?>> C requireSize(C collection, int expectedSize, String message) {
        checkExpectedSize(expectedSize);

        if (collection == null || collection.size() != expectedSize) {
            throw new IllegalArgumentException(message);
        }
        return collection;
    }

    /**
     * 断言 Collection 大小等于指定值。
     *
     * @param collection   Collection 对象
     * @param expectedSize 期望大小
     * @param <C>          Collection 类型
     * @return 原 Collection
     */
    public static <C extends Collection<?>> C requireSize(C collection, int expectedSize) {
        return requireSize(collection, expectedSize, "collection 大小必须等于 " + expectedSize);
    }

    /**
     * 断言 Map 大小等于指定值。
     *
     * @param map          Map 对象
     * @param expectedSize 期望大小
     * @param message      异常消息
     * @param <M>          Map 类型
     * @return 原 Map
     */
    public static <M extends Map<?, ?>> M requireSize(M map, int expectedSize, String message) {
        checkExpectedSize(expectedSize);

        if (map == null || map.size() != expectedSize) {
            throw new IllegalArgumentException(message);
        }
        return map;
    }

    /**
     * 断言 Map 大小等于指定值。
     *
     * @param map          Map 对象
     * @param expectedSize 期望大小
     * @param <M>          Map 类型
     * @return 原 Map
     */
    public static <M extends Map<?, ?>> M requireSize(M map, int expectedSize) {
        return requireSize(map, expectedSize, "map 大小必须等于 " + expectedSize);
    }

    /**
     * 断言数组长度等于指定值。
     *
     * @param array        数组
     * @param expectedSize 期望长度
     * @param message      异常消息
     * @param <T>          元素类型
     * @return 原数组
     */
    public static <T> T[] requireSize(T[] array, int expectedSize, String message) {
        checkExpectedSize(expectedSize);

        if (array == null || array.length != expectedSize) {
            throw new IllegalArgumentException(message);
        }
        return array;
    }

    /**
     * 断言数组长度等于指定值。
     *
     * @param array        数组
     * @param expectedSize 期望长度
     * @param <T>          元素类型
     * @return 原数组
     */
    public static <T> T[] requireSize(T[] array, int expectedSize) {
        return requireSize(array, expectedSize, "array 长度必须等于 " + expectedSize);
    }

    /**
     * 断言 Collection 大小大于等于指定值。
     *
     * @param collection Collection 对象
     * @param minSize    最小大小
     * @param message    异常消息
     * @param <C>        Collection 类型
     * @return 原 Collection
     */
    public static <C extends Collection<?>> C requireMinSize(C collection, int minSize, String message) {
        checkExpectedSize(minSize);

        if (collection == null || collection.size() < minSize) {
            throw new IllegalArgumentException(message);
        }
        return collection;
    }

    /**
     * 断言 Collection 大小大于等于指定值。
     *
     * @param collection Collection 对象
     * @param minSize    最小大小
     * @param <C>        Collection 类型
     * @return 原 Collection
     */
    public static <C extends Collection<?>> C requireMinSize(C collection, int minSize) {
        return requireMinSize(collection, minSize, "collection 大小不能小于 " + minSize);
    }

    /**
     * 断言 Map 大小大于等于指定值。
     *
     * @param map     Map 对象
     * @param minSize 最小大小
     * @param message 异常消息
     * @param <M>     Map 类型
     * @return 原 Map
     */
    public static <M extends Map<?, ?>> M requireMinSize(M map, int minSize, String message) {
        checkExpectedSize(minSize);

        if (map == null || map.size() < minSize) {
            throw new IllegalArgumentException(message);
        }
        return map;
    }

    /**
     * 断言 Map 大小大于等于指定值。
     *
     * @param map     Map 对象
     * @param minSize 最小大小
     * @param <M>     Map 类型
     * @return 原 Map
     */
    public static <M extends Map<?, ?>> M requireMinSize(M map, int minSize) {
        return requireMinSize(map, minSize, "map 大小不能小于 " + minSize);
    }

    /**
     * 断言数组长度大于等于指定值。
     *
     * @param array   数组
     * @param minSize 最小长度
     * @param message 异常消息
     * @param <T>     元素类型
     * @return 原数组
     */
    public static <T> T[] requireMinSize(T[] array, int minSize, String message) {
        checkExpectedSize(minSize);

        if (array == null || array.length < minSize) {
            throw new IllegalArgumentException(message);
        }
        return array;
    }

    /**
     * 断言数组长度大于等于指定值。
     *
     * @param array   数组
     * @param minSize 最小长度
     * @param <T>     元素类型
     * @return 原数组
     */
    public static <T> T[] requireMinSize(T[] array, int minSize) {
        return requireMinSize(array, minSize, "array 长度不能小于 " + minSize);
    }

    /**
     * 断言 Collection 大小小于等于指定值。
     *
     * @param collection Collection 对象
     * @param maxSize    最大大小
     * @param message    异常消息
     * @param <C>        Collection 类型
     * @return 原 Collection
     */
    public static <C extends Collection<?>> C requireMaxSize(C collection, int maxSize, String message) {
        checkExpectedSize(maxSize);

        if (collection == null || collection.size() > maxSize) {
            throw new IllegalArgumentException(message);
        }
        return collection;
    }

    /**
     * 断言 Collection 大小小于等于指定值。
     *
     * @param collection Collection 对象
     * @param maxSize    最大大小
     * @param <C>        Collection 类型
     * @return 原 Collection
     */
    public static <C extends Collection<?>> C requireMaxSize(C collection, int maxSize) {
        return requireMaxSize(collection, maxSize, "collection 大小不能大于 " + maxSize);
    }

    /**
     * 断言 Map 大小小于等于指定值。
     *
     * @param map     Map 对象
     * @param maxSize 最大大小
     * @param message 异常消息
     * @param <M>     Map 类型
     * @return 原 Map
     */
    public static <M extends Map<?, ?>> M requireMaxSize(M map, int maxSize, String message) {
        checkExpectedSize(maxSize);

        if (map == null || map.size() > maxSize) {
            throw new IllegalArgumentException(message);
        }
        return map;
    }

    /**
     * 断言 Map 大小小于等于指定值。
     *
     * @param map     Map 对象
     * @param maxSize 最大大小
     * @param <M>     Map 类型
     * @return 原 Map
     */
    public static <M extends Map<?, ?>> M requireMaxSize(M map, int maxSize) {
        return requireMaxSize(map, maxSize, "map 大小不能大于 " + maxSize);
    }

    /**
     * 断言数组长度小于等于指定值。
     *
     * @param array   数组
     * @param maxSize 最大长度
     * @param message 异常消息
     * @param <T>     元素类型
     * @return 原数组
     */
    public static <T> T[] requireMaxSize(T[] array, int maxSize, String message) {
        checkExpectedSize(maxSize);

        if (array == null || array.length > maxSize) {
            throw new IllegalArgumentException(message);
        }
        return array;
    }

    /**
     * 断言数组长度小于等于指定值。
     *
     * @param array   数组
     * @param maxSize 最大长度
     * @param <T>     元素类型
     * @return 原数组
     */
    public static <T> T[] requireMaxSize(T[] array, int maxSize) {
        return requireMaxSize(array, maxSize, "array 长度不能大于 " + maxSize);
    }

    /**
     * 断言 Collection 大小在指定范围内。
     *
     * @param collection Collection 对象
     * @param minSize    最小大小，包含
     * @param maxSize    最大大小，包含
     * @param message    异常消息
     * @param <C>        Collection 类型
     * @return 原 Collection
     */
    public static <C extends Collection<?>> C requireSizeBetween(C collection, int minSize, int maxSize, String message) {
        checkSizeRange(minSize, maxSize);

        if (collection == null || collection.size() < minSize || collection.size() > maxSize) {
            throw new IllegalArgumentException(message);
        }
        return collection;
    }

    /**
     * 断言 Collection 大小在指定范围内。
     *
     * @param collection Collection 对象
     * @param minSize    最小大小，包含
     * @param maxSize    最大大小，包含
     * @param <C>        Collection 类型
     * @return 原 Collection
     */
    public static <C extends Collection<?>> C requireSizeBetween(C collection, int minSize, int maxSize) {
        return requireSizeBetween(collection, minSize, maxSize, "collection 大小必须在 [" + minSize + ", " + maxSize + "] 范围内");
    }

    /**
     * 断言 Map 大小在指定范围内。
     *
     * @param map     Map 对象
     * @param minSize 最小大小，包含
     * @param maxSize 最大大小，包含
     * @param message 异常消息
     * @param <M>     Map 类型
     * @return 原 Map
     */
    public static <M extends Map<?, ?>> M requireSizeBetween(M map, int minSize, int maxSize, String message) {
        checkSizeRange(minSize, maxSize);

        if (map == null || map.size() < minSize || map.size() > maxSize) {
            throw new IllegalArgumentException(message);
        }
        return map;
    }

    /**
     * 断言 Map 大小在指定范围内。
     *
     * @param map     Map 对象
     * @param minSize 最小大小，包含
     * @param maxSize 最大大小，包含
     * @param <M>     Map 类型
     * @return 原 Map
     */
    public static <M extends Map<?, ?>> M requireSizeBetween(M map, int minSize, int maxSize) {
        return requireSizeBetween(map, minSize, maxSize, "map 大小必须在 [" + minSize + ", " + maxSize + "] 范围内");
    }

    /**
     * 断言数组长度在指定范围内。
     *
     * @param array   数组
     * @param minSize 最小长度，包含
     * @param maxSize 最大长度，包含
     * @param message 异常消息
     * @param <T>     元素类型
     * @return 原数组
     */
    public static <T> T[] requireSizeBetween(T[] array, int minSize, int maxSize, String message) {
        checkSizeRange(minSize, maxSize);

        if (array == null || array.length < minSize || array.length > maxSize) {
            throw new IllegalArgumentException(message);
        }
        return array;
    }

    /**
     * 断言数组长度在指定范围内。
     *
     * @param array   数组
     * @param minSize 最小长度，包含
     * @param maxSize 最大长度，包含
     * @param <T>     元素类型
     * @return 原数组
     */
    public static <T> T[] requireSizeBetween(T[] array, int minSize, int maxSize) {
        return requireSizeBetween(array, minSize, maxSize, "array 长度必须在 [" + minSize + ", " + maxSize + "] 范围内");
    }

    /**
     * 断言 Collection 不包含 null 元素。
     *
     * @param collection Collection 对象
     * @param message    异常消息
     * @param <C>        Collection 类型
     * @return 原 Collection
     */
    public static <C extends Collection<?>> C requireNoNullElements(C collection, String message) {
        requireNotNull(collection);

        for (Object item : collection) {
            if (item == null) {
                throw new IllegalArgumentException(message);
            }
        }
        return collection;
    }

    /**
     * 断言 Collection 不包含 null 元素。
     *
     * @param collection Collection 对象
     * @param <C>        Collection 类型
     * @return 原 Collection
     */
    public static <C extends Collection<?>> C requireNoNullElements(C collection) {
        return requireNoNullElements(collection, "collection 不能包含 null 元素");
    }

    /**
     * 断言数组不包含 null 元素。
     *
     * @param array   数组
     * @param message 异常消息
     * @param <T>     元素类型
     * @return 原数组
     */
    public static <T> T[] requireNoNullElements(T[] array, String message) {
        requireNotNull(array);

        for (T item : array) {
            if (item == null) {
                throw new IllegalArgumentException(message);
            }
        }
        return array;
    }

    /**
     * 断言数组不包含 null 元素。
     *
     * @param array 数组
     * @param <T>   元素类型
     * @return 原数组
     */
    public static <T> T[] requireNoNullElements(T[] array) {
        return requireNoNullElements(array, "array 不能包含 null 元素");
    }

    /**
     * 断言 Map 不包含 null Key。
     *
     * @param map     Map 对象
     * @param message 异常消息
     * @param <M>     Map 类型
     * @return 原 Map
     */
    public static <M extends Map<?, ?>> M requireNoNullKeys(M map, String message) {
        requireNotNull(map);

        for (Object key : map.keySet()) {
            if (key == null) {
                throw new IllegalArgumentException(message);
            }
        }
        return map;
    }

    /**
     * 断言 Map 不包含 null Key。
     *
     * @param map Map 对象
     * @param <M> Map 类型
     * @return 原 Map
     */
    public static <M extends Map<?, ?>> M requireNoNullKeys(M map) {
        return requireNoNullKeys(map, "map 不能包含 null Key");
    }

    /**
     * 断言 Map 不包含 null Value。
     *
     * @param map     Map 对象
     * @param message 异常消息
     * @param <M>     Map 类型
     * @return 原 Map
     */
    public static <M extends Map<?, ?>> M requireNoNullValues(M map, String message) {
        requireNotNull(map);

        for (Object value : map.values()) {
            if (value == null) {
                throw new IllegalArgumentException(message);
            }
        }
        return map;
    }

    /**
     * 断言 Map 不包含 null Value。
     *
     * @param map Map 对象
     * @param <M> Map 类型
     * @return 原 Map
     */
    public static <M extends Map<?, ?>> M requireNoNullValues(M map) {
        return requireNoNullValues(map, "map 不能包含 null Value");
    }

    /**
     * 断言 Map 不包含 null Key 和 null Value。
     *
     * @param map     Map 对象
     * @param message 异常消息
     * @param <M>     Map 类型
     * @return 原 Map
     */
    public static <M extends Map<?, ?>> M requireNoNullEntries(M map, String message) {
        requireNotNull(map);

        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                throw new IllegalArgumentException(message);
            }
        }
        return map;
    }

    /**
     * 断言 Map 不包含 null Key 和 null Value。
     *
     * @param map Map 对象
     * @param <M> Map 类型
     * @return 原 Map
     */
    public static <M extends Map<?, ?>> M requireNoNullEntries(M map) {
        return requireNoNullEntries(map, "map 不能包含 null Key 或 null Value");
    }

    /**
     * 断言 Collection 不包含空值元素。
     *
     * <p>
     * 空值定义使用 isEmptyValue(Object)。
     * </p>
     *
     * @param collection Collection 对象
     * @param message    异常消息
     * @param <C>        Collection 类型
     * @return 原 Collection
     */
    public static <C extends Collection<?>> C requireNoEmptyValues(C collection, String message) {
        requireNotNull(collection);

        for (Object item : collection) {
            if (isEmptyValue(item)) {
                throw new IllegalArgumentException(message);
            }
        }
        return collection;
    }

    /**
     * 断言 Collection 不包含空值元素。
     *
     * @param collection Collection 对象
     * @param <C>        Collection 类型
     * @return 原 Collection
     */
    public static <C extends Collection<?>> C requireNoEmptyValues(C collection) {
        return requireNoEmptyValues(collection, "collection 不能包含空值元素");
    }

    /**
     * 断言数组不包含空值元素。
     *
     * <p>
     * 空值定义使用 isEmptyValue(Object)。
     * </p>
     *
     * @param array   数组
     * @param message 异常消息
     * @param <T>     元素类型
     * @return 原数组
     */
    public static <T> T[] requireNoEmptyValues(T[] array, String message) {
        requireNotNull(array);

        for (T item : array) {
            if (isEmptyValue(item)) {
                throw new IllegalArgumentException(message);
            }
        }
        return array;
    }

    /**
     * 断言数组不包含空值元素。
     *
     * @param array 数组
     * @param <T>   元素类型
     * @return 原数组
     */
    public static <T> T[] requireNoEmptyValues(T[] array) {
        return requireNoEmptyValues(array, "array 不能包含空值元素");
    }

    /**
     * 断言 Map 不包含空值 Value。
     *
     * <p>
     * 空值定义使用 isEmptyValue(Object)。
     * </p>
     *
     * @param map     Map 对象
     * @param message 异常消息
     * @param <M>     Map 类型
     * @return 原 Map
     */
    public static <M extends Map<?, ?>> M requireNoEmptyMapValues(M map, String message) {
        requireNotNull(map);

        for (Object value : map.values()) {
            if (isEmptyValue(value)) {
                throw new IllegalArgumentException(message);
            }
        }
        return map;
    }

    /**
     * 断言 Map 不包含空值 Value。
     *
     * @param map Map 对象
     * @param <M> Map 类型
     * @return 原 Map
     */
    public static <M extends Map<?, ?>> M requireNoEmptyMapValues(M map) {
        return requireNoEmptyMapValues(map, "map 不能包含空值 Value");
    }

    /**
     * 断言 Collection 包含指定元素。
     *
     * @param collection Collection 对象
     * @param element    目标元素
     * @param message    异常消息
     * @param <C>        Collection 类型
     * @return 原 Collection
     */
    public static <C extends Collection<?>> C requireContains(C collection, Object element, String message) {
        if (collection == null || !collection.contains(element)) {
            throw new IllegalArgumentException(message);
        }
        return collection;
    }

    /**
     * 断言 Collection 包含指定元素。
     *
     * @param collection Collection 对象
     * @param element    目标元素
     * @param <C>        Collection 类型
     * @return 原 Collection
     */
    public static <C extends Collection<?>> C requireContains(C collection, Object element) {
        return requireContains(collection, element, "collection 必须包含指定元素");
    }

    /**
     * 断言 Collection 不包含指定元素。
     *
     * @param collection Collection 对象
     * @param element    目标元素
     * @param message    异常消息
     * @param <C>        Collection 类型
     * @return 原 Collection
     */
    public static <C extends Collection<?>> C requireNotContains(C collection, Object element, String message) {
        if (collection != null && collection.contains(element)) {
            throw new IllegalArgumentException(message);
        }
        return collection;
    }

    /**
     * 断言 Collection 不包含指定元素。
     *
     * @param collection Collection 对象
     * @param element    目标元素
     * @param <C>        Collection 类型
     * @return 原 Collection
     */
    public static <C extends Collection<?>> C requireNotContains(C collection, Object element) {
        return requireNotContains(collection, element, "collection 不能包含指定元素");
    }

    /**
     * 断言 Collection 包含全部目标元素。
     *
     * @param collection Collection 对象
     * @param targets    目标元素集合
     * @param message    异常消息
     * @param <C>        Collection 类型
     * @return 原 Collection
     */
    public static <C extends Collection<?>> C requireContainsAll(C collection, Collection<?> targets, String message) {
        if (collection == null || collection.isEmpty() || targets == null || targets.isEmpty() || !collection.containsAll(targets)) {
            throw new IllegalArgumentException(message);
        }
        return collection;
    }

    /**
     * 断言 Collection 包含全部目标元素。
     *
     * @param collection Collection 对象
     * @param targets    目标元素集合
     * @param <C>        Collection 类型
     * @return 原 Collection
     */
    public static <C extends Collection<?>> C requireContainsAll(C collection, Collection<?> targets) {
        return requireContainsAll(collection, targets, "collection 必须包含全部目标元素");
    }

    /**
     * 断言 Collection 至少包含一个目标元素。
     *
     * @param collection Collection 对象
     * @param targets    目标元素集合
     * @param message    异常消息
     * @param <C>        Collection 类型
     * @return 原 Collection
     */
    public static <C extends Collection<?>> C requireContainsAny(C collection, Collection<?> targets, String message) {
        if (!containsAny(collection, targets)) {
            throw new IllegalArgumentException(message);
        }
        return collection;
    }

    /**
     * 断言 Collection 至少包含一个目标元素。
     *
     * @param collection Collection 对象
     * @param targets    目标元素集合
     * @param <C>        Collection 类型
     * @return 原 Collection
     */
    public static <C extends Collection<?>> C requireContainsAny(C collection, Collection<?> targets) {
        return requireContainsAny(collection, targets, "collection 必须至少包含一个目标元素");
    }

    /**
     * 断言 Map 包含指定 Key。
     *
     * @param map     Map 对象
     * @param key     Key
     * @param message 异常消息
     * @param <M>     Map 类型
     * @return 原 Map
     */
    public static <M extends Map<?, ?>> M requireContainsKey(M map, Object key, String message) {
        if (map == null || !map.containsKey(key)) {
            throw new IllegalArgumentException(message);
        }
        return map;
    }

    /**
     * 断言 Map 包含指定 Key。
     *
     * @param map Map 对象
     * @param key Key
     * @param <M> Map 类型
     * @return 原 Map
     */
    public static <M extends Map<?, ?>> M requireContainsKey(M map, Object key) {
        return requireContainsKey(map, key, "map 必须包含指定 Key");
    }

    /**
     * 断言 Map 不包含指定 Key。
     *
     * @param map     Map 对象
     * @param key     Key
     * @param message 异常消息
     * @param <M>     Map 类型
     * @return 原 Map
     */
    public static <M extends Map<?, ?>> M requireNotContainsKey(M map, Object key, String message) {
        if (map != null && map.containsKey(key)) {
            throw new IllegalArgumentException(message);
        }
        return map;
    }

    /**
     * 断言 Map 不包含指定 Key。
     *
     * @param map Map 对象
     * @param key Key
     * @param <M> Map 类型
     * @return 原 Map
     */
    public static <M extends Map<?, ?>> M requireNotContainsKey(M map, Object key) {
        return requireNotContainsKey(map, key, "map 不能包含指定 Key");
    }

    /**
     * 断言 Map 包含指定 Value。
     *
     * @param map     Map 对象
     * @param value   Value
     * @param message 异常消息
     * @param <M>     Map 类型
     * @return 原 Map
     */
    public static <M extends Map<?, ?>> M requireContainsValue(M map, Object value, String message) {
        if (map == null || !map.containsValue(value)) {
            throw new IllegalArgumentException(message);
        }
        return map;
    }

    /**
     * 断言 Map 包含指定 Value。
     *
     * @param map   Map 对象
     * @param value Value
     * @param <M>   Map 类型
     * @return 原 Map
     */
    public static <M extends Map<?, ?>> M requireContainsValue(M map, Object value) {
        return requireContainsValue(map, value, "map 必须包含指定 Value");
    }

    /**
     * 断言 Map 包含全部目标 Key。
     *
     * @param map     Map 对象
     * @param keys    Key 集合
     * @param message 异常消息
     * @param <M>     Map 类型
     * @return 原 Map
     */
    public static <M extends Map<?, ?>> M requireContainsAllKeys(M map, Collection<?> keys, String message) {
        if (!containsAllKeys(map, keys)) {
            throw new IllegalArgumentException(message);
        }
        return map;
    }

    /**
     * 断言 Map 包含全部目标 Key。
     *
     * @param map  Map 对象
     * @param keys Key 集合
     * @param <M>  Map 类型
     * @return 原 Map
     */
    public static <M extends Map<?, ?>> M requireContainsAllKeys(M map, Collection<?> keys) {
        return requireContainsAllKeys(map, keys, "map 必须包含全部目标 Key");
    }

    /**
     * 断言 Map 至少包含一个目标 Key。
     *
     * @param map     Map 对象
     * @param keys    Key 集合
     * @param message 异常消息
     * @param <M>     Map 类型
     * @return 原 Map
     */
    public static <M extends Map<?, ?>> M requireContainsAnyKey(M map, Collection<?> keys, String message) {
        if (!containsAnyKey(map, keys)) {
            throw new IllegalArgumentException(message);
        }
        return map;
    }

    /**
     * 断言 Map 至少包含一个目标 Key。
     *
     * @param map  Map 对象
     * @param keys Key 集合
     * @param <M>  Map 类型
     * @return 原 Map
     */
    public static <M extends Map<?, ?>> M requireContainsAnyKey(M map, Collection<?> keys) {
        return requireContainsAnyKey(map, keys, "map 必须至少包含一个目标 Key");
    }

    /**
     * 断言 Collection 所有元素都符合条件。
     *
     * <p>
     * 空集合会校验失败。
     * </p>
     *
     * @param collection Collection 对象
     * @param predicate  判断条件
     * @param message    异常消息
     * @param <C>        Collection 类型
     * @param <T>        元素类型
     * @return 原 Collection
     */
    public static <C extends Collection<? extends T>, T> C requireAllMatch(C collection, Predicate<? super T> predicate, String message) {
        Objects.requireNonNull(predicate, "predicate 不能为 null");

        if (collection == null || collection.isEmpty()) {
            throw new IllegalArgumentException(message);
        }

        for (T item : collection) {
            if (!predicate.test(item)) {
                throw new IllegalArgumentException(message);
            }
        }
        return collection;
    }

    /**
     * 断言 Collection 所有元素都符合条件。
     *
     * @param collection Collection 对象
     * @param predicate  判断条件
     * @param <C>        Collection 类型
     * @param <T>        元素类型
     * @return 原 Collection
     */
    public static <C extends Collection<? extends T>, T> C requireAllMatch(C collection, Predicate<? super T> predicate) {
        return requireAllMatch(collection, predicate, "collection 中存在不符合条件的元素");
    }

    /**
     * 断言 Collection 至少存在一个符合条件的元素。
     *
     * @param collection Collection 对象
     * @param predicate  判断条件
     * @param message    异常消息
     * @param <C>        Collection 类型
     * @param <T>        元素类型
     * @return 原 Collection
     */
    public static <C extends Collection<? extends T>, T> C requireAnyMatch(C collection, Predicate<? super T> predicate, String message) {
        Objects.requireNonNull(predicate, "predicate 不能为 null");

        if (collection == null || collection.isEmpty()) {
            throw new IllegalArgumentException(message);
        }

        for (T item : collection) {
            if (predicate.test(item)) {
                return collection;
            }
        }

        throw new IllegalArgumentException(message);
    }

    /**
     * 断言 Collection 至少存在一个符合条件的元素。
     *
     * @param collection Collection 对象
     * @param predicate  判断条件
     * @param <C>        Collection 类型
     * @param <T>        元素类型
     * @return 原 Collection
     */
    public static <C extends Collection<? extends T>, T> C requireAnyMatch(C collection, Predicate<? super T> predicate) {
        return requireAnyMatch(collection, predicate, "collection 中至少需要一个符合条件的元素");
    }

    /**
     * 断言 Collection 不存在符合条件的元素。
     *
     * @param collection Collection 对象
     * @param predicate  判断条件
     * @param message    异常消息
     * @param <C>        Collection 类型
     * @param <T>        元素类型
     * @return 原 Collection
     */
    public static <C extends Collection<? extends T>, T> C requireNoneMatch(C collection, Predicate<? super T> predicate, String message) {
        Objects.requireNonNull(predicate, "predicate 不能为 null");

        if (collection == null || collection.isEmpty()) {
            return collection;
        }

        for (T item : collection) {
            if (predicate.test(item)) {
                throw new IllegalArgumentException(message);
            }
        }
        return collection;
    }

    /**
     * 断言 Collection 不存在符合条件的元素。
     *
     * @param collection Collection 对象
     * @param predicate  判断条件
     * @param <C>        Collection 类型
     * @param <T>        元素类型
     * @return 原 Collection
     */
    public static <C extends Collection<? extends T>, T> C requireNoneMatch(C collection, Predicate<? super T> predicate) {
        return requireNoneMatch(collection, predicate, "collection 中不能存在符合条件的元素");
    }

    /**
     * 断言数组所有元素都符合条件。
     *
     * <p>
     * 空数组会校验失败。
     * </p>
     *
     * @param array     数组
     * @param predicate 判断条件
     * @param message   异常消息
     * @param <T>       元素类型
     * @return 原数组
     */
    public static <T> T[] requireAllMatch(T[] array, Predicate<? super T> predicate, String message) {
        Objects.requireNonNull(predicate, "predicate 不能为 null");

        if (array == null || array.length == 0) {
            throw new IllegalArgumentException(message);
        }

        for (T item : array) {
            if (!predicate.test(item)) {
                throw new IllegalArgumentException(message);
            }
        }
        return array;
    }

    /**
     * 断言数组所有元素都符合条件。
     *
     * @param array     数组
     * @param predicate 判断条件
     * @param <T>       元素类型
     * @return 原数组
     */
    public static <T> T[] requireAllMatch(T[] array, Predicate<? super T> predicate) {
        return requireAllMatch(array, predicate, "array 中存在不符合条件的元素");
    }

    /**
     * 断言数组至少存在一个符合条件的元素。
     *
     * @param array     数组
     * @param predicate 判断条件
     * @param message   异常消息
     * @param <T>       元素类型
     * @return 原数组
     */
    public static <T> T[] requireAnyMatch(T[] array, Predicate<? super T> predicate, String message) {
        Objects.requireNonNull(predicate, "predicate 不能为 null");

        if (array == null || array.length == 0) {
            throw new IllegalArgumentException(message);
        }

        for (T item : array) {
            if (predicate.test(item)) {
                return array;
            }
        }

        throw new IllegalArgumentException(message);
    }

    /**
     * 断言数组至少存在一个符合条件的元素。
     *
     * @param array     数组
     * @param predicate 判断条件
     * @param <T>       元素类型
     * @return 原数组
     */
    public static <T> T[] requireAnyMatch(T[] array, Predicate<? super T> predicate) {
        return requireAnyMatch(array, predicate, "array 中至少需要一个符合条件的元素");
    }

    /**
     * 断言数组不存在符合条件的元素。
     *
     * @param array     数组
     * @param predicate 判断条件
     * @param message   异常消息
     * @param <T>       元素类型
     * @return 原数组
     */
    public static <T> T[] requireNoneMatch(T[] array, Predicate<? super T> predicate, String message) {
        Objects.requireNonNull(predicate, "predicate 不能为 null");

        if (array == null || array.length == 0) {
            return array;
        }

        for (T item : array) {
            if (predicate.test(item)) {
                throw new IllegalArgumentException(message);
            }
        }
        return array;
    }

    /**
     * 断言数组不存在符合条件的元素。
     *
     * @param array     数组
     * @param predicate 判断条件
     * @param <T>       元素类型
     * @return 原数组
     */
    public static <T> T[] requireNoneMatch(T[] array, Predicate<? super T> predicate) {
        return requireNoneMatch(array, predicate, "array 中不能存在符合条件的元素");
    }

    /**
     * 断言 Map 所有 Entry 都符合条件。
     *
     * <p>
     * 空 Map 会校验失败。
     * </p>
     *
     * @param map       Map 对象
     * @param predicate Entry 判断条件
     * @param message   异常消息
     * @param <M>       Map 类型
     * @param <K>       Key 类型
     * @param <V>       Value 类型
     * @return 原 Map
     */
    public static <M extends Map<? extends K, ? extends V>, K, V> M requireAllEntryMatch(
            M map,
            Predicate<? super Map.Entry<? extends K, ? extends V>> predicate,
            String message) {
        Objects.requireNonNull(predicate, "predicate 不能为 null");

        if (map == null || map.isEmpty()) {
            throw new IllegalArgumentException(message);
        }

        for (Map.Entry<? extends K, ? extends V> entry : map.entrySet()) {
            if (!predicate.test(entry)) {
                throw new IllegalArgumentException(message);
            }
        }
        return map;
    }

    /**
     * 断言 Map 所有 Entry 都符合条件。
     *
     * @param map       Map 对象
     * @param predicate Entry 判断条件
     * @param <M>       Map 类型
     * @param <K>       Key 类型
     * @param <V>       Value 类型
     * @return 原 Map
     */
    public static <M extends Map<? extends K, ? extends V>, K, V> M requireAllEntryMatch(
            M map,
            Predicate<? super Map.Entry<? extends K, ? extends V>> predicate) {
        return requireAllEntryMatch(map, predicate, "map 中存在不符合条件的 Entry");
    }

    /**
     * 断言 Map 至少存在一个符合条件的 Entry。
     *
     * @param map       Map 对象
     * @param predicate Entry 判断条件
     * @param message   异常消息
     * @param <M>       Map 类型
     * @param <K>       Key 类型
     * @param <V>       Value 类型
     * @return 原 Map
     */
    public static <M extends Map<? extends K, ? extends V>, K, V> M requireAnyEntryMatch(
            M map,
            Predicate<? super Map.Entry<? extends K, ? extends V>> predicate,
            String message) {
        Objects.requireNonNull(predicate, "predicate 不能为 null");

        if (map == null || map.isEmpty()) {
            throw new IllegalArgumentException(message);
        }

        for (Map.Entry<? extends K, ? extends V> entry : map.entrySet()) {
            if (predicate.test(entry)) {
                return map;
            }
        }

        throw new IllegalArgumentException(message);
    }

    /**
     * 断言 Map 至少存在一个符合条件的 Entry。
     *
     * @param map       Map 对象
     * @param predicate Entry 判断条件
     * @param <M>       Map 类型
     * @param <K>       Key 类型
     * @param <V>       Value 类型
     * @return 原 Map
     */
    public static <M extends Map<? extends K, ? extends V>, K, V> M requireAnyEntryMatch(
            M map,
            Predicate<? super Map.Entry<? extends K, ? extends V>> predicate) {
        return requireAnyEntryMatch(map, predicate, "map 中至少需要一个符合条件的 Entry");
    }

    /**
     * 断言 Collection 中元素不重复。
     *
     * @param collection Collection 对象
     * @param message    异常消息
     * @param <C>        Collection 类型
     * @return 原 Collection
     */
    public static <C extends Collection<?>> C requireNoDuplicate(C collection, String message) {
        if (hasDuplicate(collection)) {
            throw new IllegalArgumentException(message);
        }
        return collection;
    }

    /**
     * 断言 Collection 中元素不重复。
     *
     * @param collection Collection 对象
     * @param <C>        Collection 类型
     * @return 原 Collection
     */
    public static <C extends Collection<?>> C requireNoDuplicate(C collection) {
        return requireNoDuplicate(collection, "collection 不能包含重复元素");
    }

    /**
     * 断言数组中元素不重复。
     *
     * @param array   数组
     * @param message 异常消息
     * @param <T>     元素类型
     * @return 原数组
     */
    public static <T> T[] requireNoDuplicate(T[] array, String message) {
        if (hasDuplicate(array)) {
            throw new IllegalArgumentException(message);
        }
        return array;
    }

    /**
     * 断言数组中元素不重复。
     *
     * @param array 数组
     * @param <T>   元素类型
     * @return 原数组
     */
    public static <T> T[] requireNoDuplicate(T[] array) {
        return requireNoDuplicate(array, "array 不能包含重复元素");
    }

    /**
     * 断言 Collection 中映射出的 Key 不重复。
     *
     * @param collection Collection 对象
     * @param keyMapper  Key 映射函数
     * @param message    异常消息
     * @param <C>        Collection 类型
     * @param <T>        元素类型
     * @param <K>        Key 类型
     * @return 原 Collection
     */
    public static <C extends Collection<? extends T>, T, K> C requireNoDuplicateBy(
            C collection,
            Function<? super T, ? extends K> keyMapper,
            String message) {
        if (hasDuplicateBy(collection, keyMapper)) {
            throw new IllegalArgumentException(message);
        }
        return collection;
    }

    /**
     * 断言 Collection 中映射出的 Key 不重复。
     *
     * @param collection Collection 对象
     * @param keyMapper  Key 映射函数
     * @param <C>        Collection 类型
     * @param <T>        元素类型
     * @param <K>        Key 类型
     * @return 原 Collection
     */
    public static <C extends Collection<? extends T>, T, K> C requireNoDuplicateBy(
            C collection,
            Function<? super T, ? extends K> keyMapper) {
        return requireNoDuplicateBy(collection, keyMapper, "collection 不能包含重复 Key");
    }

    /**
     * 断言数组中映射出的 Key 不重复。
     *
     * @param array     数组
     * @param keyMapper Key 映射函数
     * @param message   异常消息
     * @param <T>       元素类型
     * @param <K>       Key 类型
     * @return 原数组
     */
    public static <T, K> T[] requireNoDuplicateBy(T[] array, Function<? super T, ? extends K> keyMapper, String message) {
        if (hasDuplicateBy(array, keyMapper)) {
            throw new IllegalArgumentException(message);
        }
        return array;
    }

    /**
     * 断言数组中映射出的 Key 不重复。
     *
     * @param array     数组
     * @param keyMapper Key 映射函数
     * @param <T>       元素类型
     * @param <K>       Key 类型
     * @return 原数组
     */
    public static <T, K> T[] requireNoDuplicateBy(T[] array, Function<? super T, ? extends K> keyMapper) {
        return requireNoDuplicateBy(array, keyMapper, "array 不能包含重复 Key");
    }

    /**
     * 断言两个 Collection 存在交集。
     *
     * @param first   第一个 Collection
     * @param second  第二个 Collection
     * @param message 异常消息
     */
    public static void requireHasIntersection(Collection<?> first, Collection<?> second, String message) {
        if (!hasIntersection(first, second)) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * 断言两个 Collection 存在交集。
     *
     * @param first  第一个 Collection
     * @param second 第二个 Collection
     */
    public static void requireHasIntersection(Collection<?> first, Collection<?> second) {
        requireHasIntersection(first, second, "两个 collection 必须存在交集");
    }

    /**
     * 断言两个 Collection 不存在交集。
     *
     * @param first   第一个 Collection
     * @param second  第二个 Collection
     * @param message 异常消息
     */
    public static void requireDisjoint(Collection<?> first, Collection<?> second, String message) {
        if (hasIntersection(first, second)) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * 断言两个 Collection 不存在交集。
     *
     * @param first  第一个 Collection
     * @param second 第二个 Collection
     */
    public static void requireDisjoint(Collection<?> first, Collection<?> second) {
        requireDisjoint(first, second, "两个 collection 不能存在交集");
    }

    /**
     * 断言两个 Collection 忽略顺序后元素相同。
     *
     * <p>
     * 忽略重复次数。
     * </p>
     *
     * @param first   第一个 Collection
     * @param second  第二个 Collection
     * @param message 异常消息
     */
    public static void requireEqualsIgnoreOrder(Collection<?> first, Collection<?> second, String message) {
        if (!equalsIgnoreOrder(first, second)) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * 断言两个 Collection 忽略顺序后元素相同。
     *
     * @param first  第一个 Collection
     * @param second 第二个 Collection
     */
    public static void requireEqualsIgnoreOrder(Collection<?> first, Collection<?> second) {
        requireEqualsIgnoreOrder(first, second, "两个 collection 的元素必须相同");
    }

    /**
     * 断言两个 Collection 忽略顺序后元素和出现次数相同。
     *
     * @param first   第一个 Collection
     * @param second  第二个 Collection
     * @param message 异常消息
     */
    public static void requireEqualsIgnoreOrderWithCount(Collection<?> first, Collection<?> second, String message) {
        if (!equalsIgnoreOrderWithCount(first, second)) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * 断言两个 Collection 忽略顺序后元素和出现次数相同。
     *
     * @param first  第一个 Collection
     * @param second 第二个 Collection
     */
    public static void requireEqualsIgnoreOrderWithCount(Collection<?> first, Collection<?> second) {
        requireEqualsIgnoreOrderWithCount(first, second, "两个 collection 的元素和出现次数必须相同");
    }

    /**
     * 断言 first 是 second 的子集。
     *
     * @param first   待判断 Collection
     * @param second  目标 Collection
     * @param message 异常消息
     */
    public static void requireSubset(Collection<?> first, Collection<?> second, String message) {
        if (!isSubset(first, second)) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * 断言 first 是 second 的子集。
     *
     * @param first  待判断 Collection
     * @param second 目标 Collection
     */
    public static void requireSubset(Collection<?> first, Collection<?> second) {
        requireSubset(first, second, "first 必须是 second 的子集");
    }

    /**
     * 断言 first 是 second 的超集。
     *
     * @param first   待判断 Collection
     * @param second  目标 Collection
     * @param message 异常消息
     */
    public static void requireSuperset(Collection<?> first, Collection<?> second, String message) {
        if (!isSuperset(first, second)) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * 断言 first 是 second 的超集。
     *
     * @param first  待判断 Collection
     * @param second 目标 Collection
     */
    public static void requireSuperset(Collection<?> first, Collection<?> second) {
        requireSuperset(first, second, "first 必须是 second 的超集");
    }

    /**
     * 断言 List 下标有效。
     *
     * @param list    List 对象
     * @param index   下标
     * @param message 异常消息
     */
    public static void requireValidIndex(List<?> list, int index, String message) {
        if (list == null || index < 0 || index >= list.size()) {
            throw new IndexOutOfBoundsException(message);
        }
    }

    /**
     * 断言 List 下标有效。
     *
     * @param list  List 对象
     * @param index 下标
     */
    public static void requireValidIndex(List<?> list, int index) {
        requireValidIndex(list, index, "index 越界");
    }

    /**
     * 断言数组下标有效。
     *
     * @param array   数组
     * @param index   下标
     * @param message 异常消息
     */
    public static void requireValidIndex(Object[] array, int index, String message) {
        if (array == null || index < 0 || index >= array.length) {
            throw new IndexOutOfBoundsException(message);
        }
    }

    /**
     * 断言数组下标有效。
     *
     * @param array 数组
     * @param index 下标
     */
    public static void requireValidIndex(Object[] array, int index) {
        requireValidIndex(array, index, "index 越界");
    }

    /**
     * 断言任意数组下标有效，支持对象数组和基本类型数组。
     *
     * @param array   数组对象
     * @param index   下标
     * @param message 异常消息
     */
    public static void requireValidArrayIndex(Object array, int index, String message) {
        if (array == null || !array.getClass().isArray() || index < 0 || index >= Array.getLength(array)) {
            throw new IndexOutOfBoundsException(message);
        }
    }

    /**
     * 断言任意数组下标有效，支持对象数组和基本类型数组。
     *
     * @param array 数组对象
     * @param index 下标
     */
    public static void requireValidArrayIndex(Object array, int index) {
        requireValidArrayIndex(array, index, "index 越界");
    }

    /**
     * 断言区间有效。
     *
     * <p>
     * fromIndex 包含，toIndex 不包含。
     * </p>
     *
     * @param size      总大小
     * @param fromIndex 开始下标，包含
     * @param toIndex   结束下标，不包含
     */
    public static void requireValidRange(int size, int fromIndex, int toIndex) {
        checkExpectedSize(size);

        if (fromIndex < 0 || toIndex < fromIndex || toIndex > size) {
            throw new IndexOutOfBoundsException("区间必须满足 0 <= fromIndex <= toIndex <= size");
        }
    }

    /**
     * 断言 List 区间有效。
     *
     * <p>
     * fromIndex 包含，toIndex 不包含。
     * </p>
     *
     * @param list      List 对象
     * @param fromIndex 开始下标，包含
     * @param toIndex   结束下标，不包含
     */
    public static void requireValidRange(List<?> list, int fromIndex, int toIndex) {
        if (list == null) {
            throw new IndexOutOfBoundsException("list 不能为 null");
        }
        requireValidRange(list.size(), fromIndex, toIndex);
    }

    /**
     * 断言数组区间有效。
     *
     * <p>
     * fromIndex 包含，toIndex 不包含。
     * </p>
     *
     * @param array     数组
     * @param fromIndex 开始下标，包含
     * @param toIndex   结束下标，不包含
     */
    public static void requireValidRange(Object[] array, int fromIndex, int toIndex) {
        if (array == null) {
            throw new IndexOutOfBoundsException("array 不能为 null");
        }
        requireValidRange(array.length, fromIndex, toIndex);
    }

    /**
     * 校验 Collection 是否不为空。
     *
     * @param collection Collection 对象
     * @return true 表示非 null 且非空
     */
    public static boolean validNotEmpty(Collection<?> collection) {
        return collection != null && !collection.isEmpty();
    }

    /**
     * 校验 Map 是否不为空。
     *
     * @param map Map 对象
     * @return true 表示非 null 且非空
     */
    public static boolean validNotEmpty(Map<?, ?> map) {
        return map != null && !map.isEmpty();
    }

    /**
     * 校验数组是否不为空。
     *
     * @param array 数组
     * @return true 表示非 null 且长度大于 0
     */
    public static boolean validNotEmpty(Object[] array) {
        return array != null && array.length > 0;
    }

    /**
     * 校验 Collection 大小是否在指定范围内。
     *
     * @param collection Collection 对象
     * @param minSize    最小大小，包含
     * @param maxSize    最大大小，包含
     * @return true 表示大小在范围内
     */
    public static boolean validSizeBetween(Collection<?> collection, int minSize, int maxSize) {
        checkSizeRange(minSize, maxSize);
        return collection != null && collection.size() >= minSize && collection.size() <= maxSize;
    }

    /**
     * 校验 Map 大小是否在指定范围内。
     *
     * @param map     Map 对象
     * @param minSize 最小大小，包含
     * @param maxSize 最大大小，包含
     * @return true 表示大小在范围内
     */
    public static boolean validSizeBetween(Map<?, ?> map, int minSize, int maxSize) {
        checkSizeRange(minSize, maxSize);
        return map != null && map.size() >= minSize && map.size() <= maxSize;
    }

    /**
     * 校验数组长度是否在指定范围内。
     *
     * @param array   数组
     * @param minSize 最小长度，包含
     * @param maxSize 最大长度，包含
     * @return true 表示长度在范围内
     */
    public static boolean validSizeBetween(Object[] array, int minSize, int maxSize) {
        checkSizeRange(minSize, maxSize);
        return array != null && array.length >= minSize && array.length <= maxSize;
    }

    /**
     * 校验 List 下标是否有效。
     *
     * @param list  List 对象
     * @param index 下标
     * @return true 表示下标有效
     */
    public static boolean validIndex(List<?> list, int index) {
        return list != null && index >= 0 && index < list.size();
    }

    /**
     * 校验数组下标是否有效。
     *
     * @param array 数组
     * @param index 下标
     * @return true 表示下标有效
     */
    public static boolean validIndex(Object[] array, int index) {
        return array != null && index >= 0 && index < array.length;
    }

    /**
     * 校验区间是否有效。
     *
     * <p>
     * fromIndex 包含，toIndex 不包含。
     * </p>
     *
     * @param size      总大小
     * @param fromIndex 开始下标，包含
     * @param toIndex   结束下标，不包含
     * @return true 表示区间有效
     */
    public static boolean validRange(int size, int fromIndex, int toIndex) {
        return size >= 0 && fromIndex >= 0 && toIndex >= fromIndex && toIndex <= size;
    }

    /**
     * 校验 List 区间是否有效。
     *
     * @param list      List 对象
     * @param fromIndex 开始下标，包含
     * @param toIndex   结束下标，不包含
     * @return true 表示区间有效
     */
    public static boolean validRange(List<?> list, int fromIndex, int toIndex) {
        return list != null && validRange(list.size(), fromIndex, toIndex);
    }

    /**
     * 校验数组区间是否有效。
     *
     * @param array     数组
     * @param fromIndex 开始下标，包含
     * @param toIndex   结束下标，不包含
     * @return true 表示区间有效
     */
    public static boolean validRange(Object[] array, int fromIndex, int toIndex) {
        return array != null && validRange(array.length, fromIndex, toIndex);
    }

    /**
     * 校验 Collection 是否不包含 null 元素。
     *
     * @param collection Collection 对象
     * @return true 表示非 null 且不包含 null 元素
     */
    public static boolean validNoNullElements(Collection<?> collection) {
        if (collection == null) {
            return false;
        }

        for (Object item : collection) {
            if (item == null) {
                return false;
            }
        }
        return true;
    }

    /**
     * 校验数组是否不包含 null 元素。
     *
     * @param array 数组
     * @return true 表示非 null 且不包含 null 元素
     */
    public static boolean validNoNullElements(Object[] array) {
        if (array == null) {
            return false;
        }

        for (Object item : array) {
            if (item == null) {
                return false;
            }
        }
        return true;
    }

    /**
     * 校验 Map 是否不包含 null Key。
     *
     * @param map Map 对象
     * @return true 表示非 null 且不包含 null Key
     */
    public static boolean validNoNullKeys(Map<?, ?> map) {
        return map != null && !map.containsKey(null);
    }

    /**
     * 校验 Map 是否不包含 null Value。
     *
     * @param map Map 对象
     * @return true 表示非 null 且不包含 null Value
     */
    public static boolean validNoNullValues(Map<?, ?> map) {
        if (map == null) {
            return false;
        }

        for (Object value : map.values()) {
            if (value == null) {
                return false;
            }
        }
        return true;
    }

    /**
     * 校验 Map 是否不包含 null Key 和 null Value。
     *
     * @param map Map 对象
     * @return true 表示非 null 且不包含 null Key 和 null Value
     */
    public static boolean validNoNullEntries(Map<?, ?> map) {
        return validNoNullKeys(map) && validNoNullValues(map);
    }

}