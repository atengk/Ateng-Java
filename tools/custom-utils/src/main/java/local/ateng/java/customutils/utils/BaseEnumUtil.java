package io.github.atengk;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * 枚举工具类。
 *
 * <p>
 * 基于 {@link BaseEnum} 提供统一的枚举处理能力，包括：
 * <ul>
 *     <li>根据 code、name 获取枚举实例</li>
 *     <li>将枚举转换为 {@code Map<code, name>}</li>
 *     <li>支持自定义 key/value 提取器生成 Map</li>
 *     <li>支持生成包含多个字段的 {@code List<Map<String, Object>>}</li>
 *     <li>支持转换为前端常用的 {@code value/label} 列表</li>
 *     <li>支持一次处理多个枚举类</li>
 *     <li>支持后续扫描所有继承 {@link BaseEnum} 的枚举类后统一处理</li>
 * </ul>
 *
 * <p>
 * 所有转换结果默认使用 {@link LinkedHashMap} 和 {@link ArrayList}，
 * 保证枚举声明顺序与结果顺序一致。
 *
 * @author Ateng
 * @since 2026-09-01
 */
public final class EnumUtil {

    /**
     * 前端默认 value 字段。
     */
    public static final String VALUE = "value";

    /**
     * 前端默认 label 字段。
     */
    public static final String LABEL = "label";

    /**
     * 私有构造方法，禁止实例化。
     */
    private EnumUtil() {
    }

    /**
     * 根据 code 获取枚举实例。
     *
     * @param enumClass 枚举类
     * @param code      枚举 code
     * @param <E>       枚举类型
     * @param <C>       code 类型
     * @param <N>       name 类型
     * @return 匹配到的枚举实例，未找到时返回 {@code null}
     */
    public static <E extends Enum<E> & BaseEnum<C, N>, C, N> E getByCode(
            Class<E> enumClass, C code) {
        if (enumClass == null || code == null) {
            return null;
        }

        for (E item : enumClass.getEnumConstants()) {
            if (Objects.equals(code, item.getCode())) {
                return item;
            }
        }
        return null;
    }

    /**
     * 根据 name 获取枚举实例。
     *
     * @param enumClass 枚举类
     * @param name      枚举 name
     * @param <E>       枚举类型
     * @param <C>       code 类型
     * @param <N>       name 类型
     * @return 匹配到的枚举实例，未找到时返回 {@code null}
     */
    public static <E extends Enum<E> & BaseEnum<C, N>, C, N> E getByName(
            Class<E> enumClass, N name) {
        if (enumClass == null || name == null) {
            return null;
        }

        for (E item : enumClass.getEnumConstants()) {
            if (Objects.equals(name, item.getName())) {
                return item;
            }
        }
        return null;
    }

    /**
     * 根据 code 获取枚举实例，未找到时返回指定默认值。
     *
     * @param enumClass   枚举类
     * @param code        枚举 code
     * @param defaultEnum 默认枚举
     * @param <E>         枚举类型
     * @param <C>         code 类型
     * @param <N>         name 类型
     * @return 匹配到的枚举实例，未找到时返回默认枚举
     */
    public static <E extends Enum<E> & BaseEnum<C, N>, C, N> E getByCode(
            Class<E> enumClass, C code, E defaultEnum) {
        E result = getByCode(enumClass, code);
        return result != null ? result : defaultEnum;
    }

    /**
     * 根据 name 获取枚举实例，未找到时返回指定默认值。
     *
     * @param enumClass   枚举类
     * @param name        枚举 name
     * @param defaultEnum 默认枚举
     * @param <E>         枚举类型
     * @param <C>         code 类型
     * @param <N>         name 类型
     * @return 匹配到的枚举实例，未找到时返回默认枚举
     */
    public static <E extends Enum<E> & BaseEnum<C, N>, C, N> E getByName(
            Class<E> enumClass, N name, E defaultEnum) {
        E result = getByName(enumClass, name);
        return result != null ? result : defaultEnum;
    }

    /**
     * 将枚举转换为标准 Map，key 为 code，value 为 name。
     *
     * @param enumClass 枚举类
     * @param <E>       枚举类型
     * @param <C>       code 类型
     * @param <N>       name 类型
     * @return {@code Map<code, name>}
     */
    public static <E extends Enum<E> & BaseEnum<C, N>, C, N> Map<C, N> toMap(
            Class<E> enumClass) {
        return toMap(enumClass, BaseEnum::getCode, BaseEnum::getName);
    }

    /**
     * 将枚举转换为自定义 key/value Map。
     *
     * <p>
     * 适用于不使用 BaseEnum 默认 code/name 的场景，例如：
     * {@code Map<Long, String>}、{@code Map<String, Integer>} 等。
     *
     * @param enumClass    枚举类
     * @param keyExtractor key 提取器
     * @param valueExtractor value 提取器
     * @param <E>          枚举类型
     * @param <K>          Map key 类型
     * @param <V>          Map value 类型
     * @return {@code Map<K, V>}
     */
    public static <E extends Enum<E>, K, V> Map<K, V> toMap(
            Class<E> enumClass,
            Function<E, K> keyExtractor,
            Function<E, V> valueExtractor) {

        if (enumClass == null || keyExtractor == null || valueExtractor == null) {
            return new LinkedHashMap<>();
        }

        Map<K, V> result = new LinkedHashMap<>();
        for (E item : enumClass.getEnumConstants()) {
            result.put(keyExtractor.apply(item), valueExtractor.apply(item));
        }
        return result;
    }

    /**
     * 将枚举转换为多字段 Map 列表。
     *
     * <p>
     * 例如：
     * <pre>
     * LinkedHashMap&lt;String, Function&lt;StatusEnum, ?&gt;&gt; fields = new LinkedHashMap&lt;&gt;();
     * fields.put("code", StatusEnum::getCode);
     * fields.put("name", StatusEnum::getName);
     * fields.put("enabled", StatusEnum::getEnabled);
     *
     * List&lt;Map&lt;String, Object&gt;&gt; result = EnumUtil.toList(StatusEnum.class, fields);
     * </pre>
     *
     * @param enumClass 枚举类
     * @param fields    字段提取器，key 为结果字段名，value 为字段值提取器
     * @param <E>       枚举类型
     * @return 多字段 Map 列表
     */
    public static <E extends Enum<E>> List<Map<String, Object>> toList(
            Class<E> enumClass,
            LinkedHashMap<String, Function<E, ?>> fields) {

        if (enumClass == null || fields == null || fields.isEmpty()) {
            return new ArrayList<>();
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (E item : enumClass.getEnumConstants()) {
            Map<String, Object> row = new LinkedHashMap<>(fields.size());
            fields.forEach((fieldName, extractor) ->
                    row.put(fieldName, extractor == null ? null : extractor.apply(item))
            );
            result.add(row);
        }
        return result;
    }

    /**
     * 将枚举转换为标准的前端 value/label 列表。
     *
     * <p>
     * 返回结构：
     * <pre>
     * [
     *     {"value": 1, "label": "启用"},
     *     {"value": 0, "label": "禁用"}
     * ]
     * </pre>
     *
     * @param enumClass 枚举类
     * @param <E>       枚举类型
     * @param <C>       code 类型
     * @param <N>       name 类型
     * @return 前端 value/label 列表
     */
    public static <E extends Enum<E> & BaseEnum<C, N>, C, N> List<Map<String, Object>> toOptions(
            Class<E> enumClass) {

        if (enumClass == null) {
            return new ArrayList<>();
        }

        LinkedHashMap<String, Function<E, ?>> fields = new LinkedHashMap<>();
        fields.put(VALUE, BaseEnum::getCode);
        fields.put(LABEL, BaseEnum::getName);

        return toList(enumClass, fields);
    }

    /**
     * 将枚举转换为自定义字段名的前端列表。
     *
     * @param enumClass 枚举类
     * @param valueKey  value 字段名
     * @param labelKey  label 字段名
     * @param <E>       枚举类型
     * @param <C>       code 类型
     * @param <N>       name 类型
     * @return 前端 value/label 列表
     */
    public static <E extends Enum<E> & BaseEnum<C, N>, C, N> List<Map<String, Object>> toOptions(
            Class<E> enumClass,
            String valueKey,
            String labelKey) {

        if (enumClass == null) {
            return new ArrayList<>();
        }

        LinkedHashMap<String, Function<E, ?>> fields = new LinkedHashMap<>();
        fields.put(valueKey, BaseEnum::getCode);
        fields.put(labelKey, BaseEnum::getName);

        return toList(enumClass, fields);
    }

    /**
     * 将枚举转换为包含额外字段的前端列表。
     *
     * <p>
     * 在标准 value/label 的基础上可以继续增加其它字段。
     *
     * @param enumClass      枚举类
     * @param extraFields    额外字段
     * @param <E>            枚举类型
     * @param <C>            code 类型
     * @param <N>            name 类型
     * @return 前端枚举列表
     */
    public static <E extends Enum<E> & BaseEnum<C, N>, C, N> List<Map<String, Object>> toOptions(
            Class<E> enumClass,
            LinkedHashMap<String, Function<E, ?>> extraFields) {

        if (enumClass == null) {
            return new ArrayList<>();
        }

        LinkedHashMap<String, Function<E, ?>> fields = new LinkedHashMap<>();
        fields.put(VALUE, BaseEnum::getCode);
        fields.put(LABEL, BaseEnum::getName);

        if (extraFields != null) {
            fields.putAll(extraFields);
        }

        return toList(enumClass, fields);
    }

    /**
     * 将多个枚举转换为前端 value/label 列表。
     *
     * <p>
     * 多个枚举会按照传入顺序依次拼接。
     *
     * @param enumClasses 枚举类集合
     * @return 前端 value/label 列表
     */
    public static List<Map<String, Object>> toOptions(
            Collection<? extends Class<? extends BaseEnum<?, ?>>> enumClasses) {

        if (enumClasses == null || enumClasses.isEmpty()) {
            return new ArrayList<>();
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Class<? extends BaseEnum<?, ?>> enumClass : enumClasses) {
            result.addAll(toOptionsUnchecked(enumClass));
        }
        return result;
    }

    /**
     * 将多个枚举转换为前端 value/label 列表。
     *
     * @param enumClasses 枚举类
     * @return 前端 value/label 列表
     */
    @SafeVarargs
    public static List<Map<String, Object>> toOptions(
            Class<? extends BaseEnum<?, ?>>... enumClasses) {

        if (enumClasses == null || enumClasses.length == 0) {
            return new ArrayList<>();
        }

        return toOptions(Arrays.asList(enumClasses));
    }

    /**
     * 将多个枚举合并为 Map。
     *
     * <p>
     * 后传入的枚举出现相同 code 时，会覆盖前一个枚举中的相同 code。
     *
     * @param enumClasses 枚举类集合
     * @return 合并后的 Map
     */
    public static Map<Object, Object> mergeToMap(
            Collection<? extends Class<? extends BaseEnum<?, ?>>> enumClasses) {

        if (enumClasses == null || enumClasses.isEmpty()) {
            return new LinkedHashMap<>();
        }

        Map<Object, Object> result = new LinkedHashMap<>();
        for (Class<? extends BaseEnum<?, ?>> enumClass : enumClasses) {
            result.putAll(toMapUnchecked(enumClass));
        }
        return result;
    }

    /**
     * 将多个枚举合并为 Map。
     *
     * @param enumClasses 枚举类
     * @return 合并后的 Map
     */
    @SafeVarargs
    public static Map<Object, Object> mergeToMap(
            Class<? extends BaseEnum<?, ?>>... enumClasses) {

        if (enumClasses == null || enumClasses.length == 0) {
            return new LinkedHashMap<>();
        }

        return mergeToMap(Arrays.asList(enumClasses));
    }

    /**
     * 判断指定 class 是否为枚举，并且实现 BaseEnum。
     *
     * <p>
     * 该方法主要用于后续扫描包路径、ClassPath、Spring Bean 等场景，
     * 筛选出所有可参与枚举处理的类型。
     *
     * @param clazz 待判断类型
     * @return 如果是实现 BaseEnum 的枚举类返回 {@code true}
     */
    public static boolean isBaseEnum(Class<?> clazz) {
        if (clazz == null || !clazz.isEnum()) {
            return false;
        }

        return BaseEnum.class.isAssignableFrom(clazz);
    }

    /**
     * 从待扫描的 Class 集合中筛选 BaseEnum 枚举。
     *
     * @param classes 待筛选 Class 集合
     * @return 实现 BaseEnum 的枚举类
     */
    public static List<Class<? extends BaseEnum<?, ?>>> filterBaseEnums(
            Collection<Class<?>> classes) {

        if (classes == null || classes.isEmpty()) {
            return new ArrayList<>();
        }

        List<Class<? extends BaseEnum<?, ?>>> result = new ArrayList<>();
        for (Class<?> clazz : classes) {
            if (isBaseEnum(clazz)) {
                @SuppressWarnings("unchecked")
                Class<? extends BaseEnum<?, ?>> enumClass =
                        (Class<? extends BaseEnum<?, ?>>) clazz;
                result.add(enumClass);
            }
        }
        return result;
    }

    /**
     * 将 BaseEnum 类型转换为前端 options。
     *
     * <p>
     * 用于处理无法在编译期确定具体泛型参数的场景，
     * 例如通过包扫描动态获取枚举类。
     *
     * @param enumClass BaseEnum 枚举类
     * @return 前端 value/label 列表
     */
    private static List<Map<String, Object>> toOptionsUnchecked(
            Class<? extends BaseEnum<?, ?>> enumClass) {

        if (enumClass == null || !enumClass.isEnum()) {
            return new ArrayList<>();
        }

        List<Map<String, Object>> result = new ArrayList<>();
        Object[] constants = enumClass.getEnumConstants();

        if (constants == null || constants.length == 0) {
            return result;
        }

        for (Object constant : constants) {
            BaseEnum<?, ?> item = (BaseEnum<?, ?>) constant;

            Map<String, Object> row = new LinkedHashMap<>(2);
            row.put(VALUE, item.getCode());
            row.put(LABEL, item.getName());
            result.add(row);
        }

        return result;
    }

    /**
     * 将 BaseEnum 类型转换为标准 Map。
     *
     * @param enumClass BaseEnum 枚举类
     * @return {@code Map<code, name>}
     */
    private static Map<Object, Object> toMapUnchecked(
            Class<? extends BaseEnum<?, ?>> enumClass) {

        if (enumClass == null || !enumClass.isEnum()) {
            return new LinkedHashMap<>();
        }

        Map<Object, Object> result = new LinkedHashMap<>();
        Object[] constants = enumClass.getEnumConstants();

        if (constants == null || constants.length == 0) {
            return result;
        }

        for (Object constant : constants) {
            BaseEnum<?, ?> item = (BaseEnum<?, ?>) constant;
            result.put(item.getCode(), item.getName());
        }

        return result;
    }
}
