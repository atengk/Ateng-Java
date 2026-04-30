package io.github.atengk.utils.enumutil;

import io.github.atengk.utils.enumutil.annotation.EnumDict;
import io.github.atengk.utils.enumutil.annotation.EnumItem;
import io.github.atengk.utils.enumutil.core.*;
import io.github.atengk.utils.enumutil.model.EnumMetadata;
import io.github.atengk.utils.enumutil.model.EnumOption;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

/**
 * 枚举通用工具类，提供查询、转换、校验、前端字典、包扫描、注册和缓存能力。
 *
 * @author Ateng
 * @since 2026-04-29
 */
public final class EnumUtil {

    private static final Map<Class<?>, EnumCache> ENUM_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Class<? extends Enum<?>>> REGISTERED_ENUMS = new ConcurrentHashMap<>();
    private static final Set<String> GLOBAL_EXTRA_FIELDS = ConcurrentHashMap.newKeySet();
    private static volatile boolean cacheEnabled = true;

    private EnumUtil() {
        throw new UnsupportedOperationException("EnumUtil 不允许实例化");
    }

    /**
     * 获取枚举数组。
     *
     * @param enumClass 枚举类
     * @param <E>       枚举类型
     * @return 枚举数组
     */
    public static <E extends Enum<E>> E[] valuesOf(Class<E> enumClass) {
        return requireEnumClass(enumClass).getEnumConstants();
    }

    /**
     * 获取枚举列表。
     *
     * @param enumClass 枚举类
     * @param <E>       枚举类型
     * @return 枚举列表
     */
    public static <E extends Enum<E>> List<E> listOf(Class<E> enumClass) {
        return List.of(valuesOf(enumClass));
    }

    /**
     * 获取所有枚举名称。
     *
     * @param enumClass 枚举类
     * @param <E>       枚举类型
     * @return 枚举名称列表
     */
    public static <E extends Enum<E>> List<String> namesOf(Class<E> enumClass) {
        return listOf(enumClass).stream().map(Enum::name).toList();
    }

    /**
     * 获取所有枚举序号。
     *
     * @param enumClass 枚举类
     * @param <E>       枚举类型
     * @return 枚举序号列表
     */
    public static <E extends Enum<E>> List<Integer> ordinalsOf(Class<E> enumClass) {
        return listOf(enumClass).stream().map(Enum::ordinal).toList();
    }

    /**
     * 获取枚举数量。
     *
     * @param enumClass 枚举类
     * @param <E>       枚举类型
     * @return 枚举数量
     */
    public static <E extends Enum<E>> int sizeOf(Class<E> enumClass) {
        return valuesOf(enumClass).length;
    }

    /**
     * 判断类是否为枚举类型。
     *
     * @param clazz 类对象
     * @return 是否为枚举类型
     */
    public static boolean isEnum(Class<?> clazz) {
        return clazz != null && clazz.isEnum();
    }

    /**
     * 判断指定枚举名称是否存在。
     *
     * @param enumClass 枚举类
     * @param name      枚举名称
     * @param <E>       枚举类型
     * @return 是否存在
     */
    public static <E extends Enum<E>> boolean isEnumValue(Class<E> enumClass, String name) {
        return containsName(enumClass, name);
    }

    /**
     * 获取枚举类完整类名。
     *
     * @param enumClass 枚举类
     * @param <E>       枚举类型
     * @return 完整类名
     */
    public static <E extends Enum<E>> String getEnumClassName(Class<E> enumClass) {
        return requireEnumClass(enumClass).getName();
    }

    /**
     * 获取枚举类短类名。
     *
     * @param enumClass 枚举类
     * @param <E>       枚举类型
     * @return 短类名
     */
    public static <E extends Enum<E>> String getEnumSimpleName(Class<E> enumClass) {
        return requireEnumClass(enumClass).getSimpleName();
    }

    /**
     * 根据枚举名称获取枚举。
     *
     * @param enumClass 枚举类
     * @param name      枚举名称
     * @param <E>       枚举类型
     * @return 匹配的枚举，不存在时返回 null
     */
    public static <E extends Enum<E>> E getByName(Class<E> enumClass, String name) {
        requireEnumClass(enumClass);
        if (isBlank(name)) {
            return null;
        }
        try {
            return Enum.valueOf(enumClass, name);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    /**
     * 忽略大小写根据枚举名称获取枚举。
     *
     * @param enumClass 枚举类
     * @param name      枚举名称
     * @param <E>       枚举类型
     * @return 匹配的枚举，不存在时返回 null
     */
    public static <E extends Enum<E>> E getByNameIgnoreCase(Class<E> enumClass, String name) {
        requireEnumClass(enumClass);
        if (isBlank(name)) {
            return null;
        }
        for (E value : valuesOf(enumClass)) {
            if (value.name().equalsIgnoreCase(name.trim())) {
                return value;
            }
        }
        return null;
    }

    /**
     * 根据枚举序号获取枚举。
     *
     * @param enumClass 枚举类
     * @param ordinal   枚举序号
     * @param <E>       枚举类型
     * @return 匹配的枚举，不存在时返回 null
     */
    public static <E extends Enum<E>> E getByOrdinal(Class<E> enumClass, int ordinal) {
        E[] values = valuesOf(enumClass);
        if (ordinal < 0 || ordinal >= values.length) {
            return null;
        }
        return values[ordinal];
    }

    /**
     * 根据业务编码获取枚举。
     *
     * @param enumClass 枚举类
     * @param code      业务编码
     * @param <E>       枚举类型
     * @return 匹配的枚举，不存在时返回 null
     */
    public static <E extends Enum<E>> E getByCode(Class<E> enumClass, Object code) {
        requireEnumClass(enumClass);
        if (code == null) {
            return null;
        }
        return getFirstByPredicate(enumClass, item -> valueEquals(getCode(item), code));
    }

    /**
     * 根据展示名称获取枚举。
     *
     * @param enumClass 枚举类
     * @param label     展示名称
     * @param <E>       枚举类型
     * @return 匹配的枚举，不存在时返回 null
     */
    public static <E extends Enum<E>> E getByLabel(Class<E> enumClass, String label) {
        requireEnumClass(enumClass);
        if (isBlank(label)) {
            return null;
        }
        return getFirstByPredicate(enumClass, item -> valueEquals(getLabel(item), label.trim()));
    }

    /**
     * 根据描述获取枚举。
     *
     * @param enumClass 枚举类
     * @param desc      描述
     * @param <E>       枚举类型
     * @return 匹配的枚举，不存在时返回 null
     */
    public static <E extends Enum<E>> E getByDesc(Class<E> enumClass, String desc) {
        requireEnumClass(enumClass);
        if (isBlank(desc)) {
            return null;
        }
        return getFirstByPredicate(enumClass, item -> valueEquals(getDesc(item), desc.trim()));
    }

    /**
     * 根据指定字段获取第一个匹配枚举。
     *
     * @param enumClass 枚举类
     * @param fieldName 字段名或属性名
     * @param value     匹配值
     * @param <E>       枚举类型
     * @return 匹配的枚举，不存在时返回 null
     */
    public static <E extends Enum<E>> E getFirstByField(Class<E> enumClass, String fieldName, Object value) {
        requireNonBlank(fieldName, "fieldName 不能为空");
        return getFirstByPredicate(enumClass, item -> valueEquals(getFieldValue(item, fieldName), value));
    }

    /**
     * 根据指定字段获取匹配枚举列表。
     *
     * @param enumClass 枚举类
     * @param fieldName 字段名或属性名
     * @param value     匹配值
     * @param <E>       枚举类型
     * @return 匹配的枚举列表
     */
    public static <E extends Enum<E>> List<E> getListByField(Class<E> enumClass, String fieldName, Object value) {
        requireNonBlank(fieldName, "fieldName 不能为空");
        return getListByPredicate(enumClass, item -> valueEquals(getFieldValue(item, fieldName), value));
    }

    /**
     * 根据自定义条件获取第一个匹配枚举。
     *
     * @param enumClass 枚举类
     * @param predicate 匹配条件
     * @param <E>       枚举类型
     * @return 匹配的枚举，不存在时返回 null
     */
    public static <E extends Enum<E>> E getByPredicate(Class<E> enumClass, Predicate<E> predicate) {
        return getFirstByPredicate(enumClass, predicate);
    }

    /**
     * 根据自定义条件获取匹配枚举列表。
     *
     * @param enumClass 枚举类
     * @param predicate 匹配条件
     * @param <E>       枚举类型
     * @return 匹配的枚举列表
     */
    public static <E extends Enum<E>> List<E> getListByPredicate(Class<E> enumClass, Predicate<E> predicate) {
        requireEnumClass(enumClass);
        Objects.requireNonNull(predicate, "predicate 不能为空");
        List<E> result = new ArrayList<>();
        for (E item : valuesOf(enumClass)) {
            if (predicate.test(item)) {
                result.add(item);
            }
        }
        return List.copyOf(result);
    }

    /**
     * 获取枚举名称。
     *
     * @param enumValue 枚举对象
     * @return 枚举名称
     */
    public static String getName(Enum<?> enumValue) {
        return enumValue == null ? null : enumValue.name();
    }

    /**
     * 获取枚举序号。
     *
     * @param enumValue 枚举对象
     * @return 枚举序号
     */
    public static Integer getOrdinal(Enum<?> enumValue) {
        return enumValue == null ? null : enumValue.ordinal();
    }

    /**
     * 获取枚举业务编码。
     *
     * @param enumValue 枚举对象
     * @return 业务编码
     */
    public static Object getCode(Enum<?> enumValue) {
        if (enumValue == null) {
            return null;
        }
        if (enumValue instanceof CodeEnum<?> codeEnum) {
            return codeEnum.getCode();
        }
        Object value = getPropertyValue(enumValue, "code");
        return value == null ? enumValue.name() : value;
    }

    /**
     * 获取枚举展示名称。
     *
     * @param enumValue 枚举对象
     * @return 展示名称
     */
    public static String getLabel(Enum<?> enumValue) {
        if (enumValue == null) {
            return null;
        }
        if (enumValue instanceof LabelEnum labelEnum) {
            return labelEnum.getLabel();
        }
        EnumItem annotation = getItemAnnotation(enumValue);
        if (annotation != null && !isBlank(annotation.label())) {
            return annotation.label();
        }
        Object value = firstNonNull(
                getPropertyValue(enumValue, "label"),
                getPropertyValue(enumValue, "title"),
                getPropertyValue(enumValue, "text"),
                getPropertyValue(enumValue, "name")
        );
        return value == null ? enumValue.name() : String.valueOf(value);
    }

    /**
     * 获取枚举描述。
     *
     * @param enumValue 枚举对象
     * @return 描述
     */
    public static String getDesc(Enum<?> enumValue) {
        if (enumValue == null) {
            return null;
        }
        if (enumValue instanceof DescEnum descEnum && descEnum.getDesc() != null) {
            return descEnum.getDesc();
        }
        EnumItem annotation = getItemAnnotation(enumValue);
        if (annotation != null && !isBlank(annotation.desc())) {
            return annotation.desc();
        }
        Object value = firstNonNull(
                getPropertyValue(enumValue, "desc"),
                getPropertyValue(enumValue, "description"),
                getPropertyValue(enumValue, "remark")
        );
        return value == null ? null : String.valueOf(value);
    }

    /**
     * 获取枚举排序值。
     *
     * @param enumValue 枚举对象
     * @return 排序值
     */
    public static Integer getSort(Enum<?> enumValue) {
        if (enumValue == null) {
            return null;
        }
        if (enumValue instanceof SortEnum sortEnum && sortEnum.getSort() != null) {
            return sortEnum.getSort();
        }
        EnumItem annotation = getItemAnnotation(enumValue);
        if (annotation != null && annotation.sort() != 0) {
            return annotation.sort();
        }
        Object value = getPropertyValue(enumValue, "sort");
        Integer sort = toInteger(value);
        return sort == null ? enumValue.ordinal() : sort;
    }

    /**
     * 获取枚举是否启用。
     *
     * @param enumValue 枚举对象
     * @return 是否启用
     */
    public static Boolean getEnabled(Enum<?> enumValue) {
        if (enumValue == null) {
            return null;
        }
        if (enumValue instanceof EnabledEnum enabledEnum && enabledEnum.getEnabled() != null) {
            return enabledEnum.getEnabled();
        }
        EnumItem annotation = getItemAnnotation(enumValue);
        if (annotation != null) {
            return annotation.enabled();
        }
        Object value = firstNonNull(getPropertyValue(enumValue, "enabled"), getPropertyValue(enumValue, "enable"));
        return value == null ? Boolean.TRUE : toBoolean(value);
    }

    /**
     * 根据字段名或属性名获取枚举字段值。
     *
     * @param enumValue 枚举对象
     * @param fieldName 字段名或属性名
     * @return 字段值
     */
    public static Object getFieldValue(Enum<?> enumValue, String fieldName) {
        requireNonBlank(fieldName, "fieldName 不能为空");
        if (enumValue == null) {
            return null;
        }
        return switch (fieldName.trim()) {
            case "name" -> getName(enumValue);
            case "ordinal" -> getOrdinal(enumValue);
            case "code" -> getCode(enumValue);
            case "label" -> getLabel(enumValue);
            case "desc", "description" -> getDesc(enumValue);
            case "sort" -> getSort(enumValue);
            case "enabled", "enable" -> getEnabled(enumValue);
            case "group" -> getGroupValue(enumValue);
            case "parentCode" -> getParentCode(enumValue);
            case "extra" -> getExtra(enumValue);
            default -> getPropertyValue(enumValue, fieldName.trim());
        };
    }

    /**
     * 通过方法引用获取枚举字段值。
     *
     * @param enumValue 枚举对象
     * @param getter    字段获取函数
     * @param <E>       枚举类型
     * @param <R>       字段类型
     * @return 字段值
     */
    public static <E extends Enum<E>, R> R getFieldValue(E enumValue, Function<E, R> getter) {
        Objects.requireNonNull(getter, "getter 不能为空");
        return enumValue == null ? null : getter.apply(enumValue);
    }

    /**
     * 判断是否包含指定枚举名称。
     *
     * @param enumClass 枚举类
     * @param name      枚举名称
     * @param <E>       枚举类型
     * @return 是否包含
     */
    public static <E extends Enum<E>> boolean containsName(Class<E> enumClass, String name) {
        return getByName(enumClass, name) != null;
    }

    /**
     * 判断是否包含指定业务编码。
     *
     * @param enumClass 枚举类
     * @param code      业务编码
     * @param <E>       枚举类型
     * @return 是否包含
     */
    public static <E extends Enum<E>> boolean containsCode(Class<E> enumClass, Object code) {
        return getByCode(enumClass, code) != null;
    }

    /**
     * 判断是否包含指定展示名称。
     *
     * @param enumClass 枚举类
     * @param label     展示名称
     * @param <E>       枚举类型
     * @return 是否包含
     */
    public static <E extends Enum<E>> boolean containsLabel(Class<E> enumClass, String label) {
        return getByLabel(enumClass, label) != null;
    }

    /**
     * 判断是否包含指定序号。
     *
     * @param enumClass 枚举类
     * @param ordinal   枚举序号
     * @param <E>       枚举类型
     * @return 是否包含
     */
    public static <E extends Enum<E>> boolean containsOrdinal(Class<E> enumClass, int ordinal) {
        return getByOrdinal(enumClass, ordinal) != null;
    }

    /**
     * 判断是否包含指定字段值。
     *
     * @param enumClass 枚举类
     * @param fieldName 字段名或属性名
     * @param value     字段值
     * @param <E>       枚举类型
     * @return 是否包含
     */
    public static <E extends Enum<E>> boolean containsFieldValue(Class<E> enumClass, String fieldName, Object value) {
        return getFirstByField(enumClass, fieldName, value) != null;
    }

    /**
     * 判断业务编码是否合法。
     *
     * @param enumClass 枚举类
     * @param code      业务编码
     * @param <E>       枚举类型
     * @return 是否合法
     */
    public static <E extends Enum<E>> boolean isValidCode(Class<E> enumClass, Object code) {
        return containsCode(enumClass, code);
    }

    /**
     * 判断枚举名称是否合法。
     *
     * @param enumClass 枚举类
     * @param name      枚举名称
     * @param <E>       枚举类型
     * @return 是否合法
     */
    public static <E extends Enum<E>> boolean isValidName(Class<E> enumClass, String name) {
        return containsName(enumClass, name);
    }

    /**
     * 根据业务编码获取枚举，不存在时抛出异常。
     *
     * @param enumClass 枚举类
     * @param code      业务编码
     * @param <E>       枚举类型
     * @return 匹配的枚举
     */
    public static <E extends Enum<E>> E requireByCode(Class<E> enumClass, Object code) {
        return requireByCode(enumClass, code, "枚举编码不存在: " + code);
    }

    /**
     * 根据枚举名称获取枚举，不存在时抛出异常。
     *
     * @param enumClass 枚举类
     * @param name      枚举名称
     * @param <E>       枚举类型
     * @return 匹配的枚举
     */
    public static <E extends Enum<E>> E requireByName(Class<E> enumClass, String name) {
        return requireByName(enumClass, name, "枚举名称不存在: " + name);
    }

    /**
     * 校验业务编码必须合法。
     *
     * @param enumClass 枚举类
     * @param code      业务编码
     * @param <E>       枚举类型
     */
    public static <E extends Enum<E>> void requireValidCode(Class<E> enumClass, Object code) {
        requireByCode(enumClass, code);
    }

    /**
     * 校验枚举名称必须合法。
     *
     * @param enumClass 枚举类
     * @param name      枚举名称
     * @param <E>       枚举类型
     */
    public static <E extends Enum<E>> void requireValidName(Class<E> enumClass, String name) {
        requireByName(enumClass, name);
    }

    /**
     * 将业务编码转换为枚举。
     *
     * @param enumClass 枚举类
     * @param code      业务编码
     * @param <E>       枚举类型
     * @return 匹配的枚举，不存在时返回 null
     */
    public static <E extends Enum<E>> E codeToEnum(Class<E> enumClass, Object code) {
        return getByCode(enumClass, code);
    }

    /**
     * 将枚举名称转换为枚举。
     *
     * @param enumClass 枚举类
     * @param name      枚举名称
     * @param <E>       枚举类型
     * @return 匹配的枚举，不存在时返回 null
     */
    public static <E extends Enum<E>> E nameToEnum(Class<E> enumClass, String name) {
        return getByName(enumClass, name);
    }

    /**
     * 将枚举序号转换为枚举。
     *
     * @param enumClass 枚举类
     * @param ordinal   枚举序号
     * @param <E>       枚举类型
     * @return 匹配的枚举，不存在时返回 null
     */
    public static <E extends Enum<E>> E ordinalToEnum(Class<E> enumClass, int ordinal) {
        return getByOrdinal(enumClass, ordinal);
    }

    /**
     * 将枚举转换为业务编码。
     *
     * @param enumValue 枚举对象
     * @return 业务编码
     */
    public static Object enumToCode(Enum<?> enumValue) {
        return getCode(enumValue);
    }

    /**
     * 将枚举转换为展示名称。
     *
     * @param enumValue 枚举对象
     * @return 展示名称
     */
    public static String enumToLabel(Enum<?> enumValue) {
        return getLabel(enumValue);
    }

    /**
     * 将枚举转换为描述。
     *
     * @param enumValue 枚举对象
     * @return 描述
     */
    public static String enumToDesc(Enum<?> enumValue) {
        return getDesc(enumValue);
    }

    /**
     * 将业务编码转换为展示名称。
     *
     * @param enumClass 枚举类
     * @param code      业务编码
     * @param <E>       枚举类型
     * @return 展示名称，不存在时返回 null
     */
    public static <E extends Enum<E>> String codeToLabel(Class<E> enumClass, Object code) {
        return enumToLabel(getByCode(enumClass, code));
    }

    /**
     * 将业务编码转换为描述。
     *
     * @param enumClass 枚举类
     * @param code      业务编码
     * @param <E>       枚举类型
     * @return 描述，不存在时返回 null
     */
    public static <E extends Enum<E>> String codeToDesc(Class<E> enumClass, Object code) {
        return enumToDesc(getByCode(enumClass, code));
    }

    /**
     * 将枚举名称转换为业务编码。
     *
     * @param enumClass 枚举类
     * @param name      枚举名称
     * @param <E>       枚举类型
     * @return 业务编码，不存在时返回 null
     */
    public static <E extends Enum<E>> Object nameToCode(Class<E> enumClass, String name) {
        return enumToCode(getByName(enumClass, name));
    }

    /**
     * 将展示名称转换为业务编码。
     *
     * @param enumClass 枚举类
     * @param label     展示名称
     * @param <E>       枚举类型
     * @return 业务编码，不存在时返回 null
     */
    public static <E extends Enum<E>> Object labelToCode(Class<E> enumClass, String label) {
        return enumToCode(getByLabel(enumClass, label));
    }

    /**
     * 根据源字段和目标字段进行枚举值转换。
     *
     * @param enumClass   枚举类
     * @param value       源字段值
     * @param sourceField 源字段名
     * @param targetField 目标字段名
     * @param <E>         枚举类型
     * @return 目标字段值，不存在时返回 null
     */
    public static <E extends Enum<E>> Object convert(Class<E> enumClass, Object value, String sourceField, String targetField) {
        requireNonBlank(sourceField, "sourceField 不能为空");
        requireNonBlank(targetField, "targetField 不能为空");
        E enumValue = getFirstByField(enumClass, sourceField, value);
        return enumValue == null ? null : getFieldValue(enumValue, targetField);
    }

    /**
     * 转换为枚举名称列表。
     *
     * @param enumClass 枚举类
     * @param <E>       枚举类型
     * @return 名称列表
     */
    public static <E extends Enum<E>> List<String> toNameList(Class<E> enumClass) {
        return namesOf(enumClass);
    }

    /**
     * 转换为业务编码列表。
     *
     * @param enumClass 枚举类
     * @param <E>       枚举类型
     * @return 编码列表
     */
    public static <E extends Enum<E>> List<Object> toCodeList(Class<E> enumClass) {
        return listOf(enumClass).stream().map(EnumUtil::getCode).toList();
    }

    /**
     * 转换为展示名称列表。
     *
     * @param enumClass 枚举类
     * @param <E>       枚举类型
     * @return 展示名称列表
     */
    public static <E extends Enum<E>> List<String> toLabelList(Class<E> enumClass) {
        return listOf(enumClass).stream().map(EnumUtil::getLabel).toList();
    }

    /**
     * 按枚举名称构建映射。
     *
     * @param enumClass 枚举类
     * @param <E>       枚举类型
     * @return 名称到枚举的映射
     */
    public static <E extends Enum<E>> Map<String, E> toMapByName(Class<E> enumClass) {
        return toEnumMap(enumClass, Enum::name);
    }

    /**
     * 按业务编码构建映射。
     *
     * @param enumClass 枚举类
     * @param <E>       枚举类型
     * @return 编码到枚举的映射
     */
    public static <E extends Enum<E>> Map<Object, E> toMapByCode(Class<E> enumClass) {
        return toEnumMap(enumClass, EnumUtil::getCode);
    }

    /**
     * 按展示名称构建映射。
     *
     * @param enumClass 枚举类
     * @param <E>       枚举类型
     * @return 展示名称到枚举的映射
     */
    public static <E extends Enum<E>> Map<String, E> toMapByLabel(Class<E> enumClass) {
        return toEnumMap(enumClass, EnumUtil::getLabel);
    }

    /**
     * 根据指定字段构建字段映射。
     *
     * @param enumClass  枚举类
     * @param keyField   键字段
     * @param valueField 值字段
     * @param <E>        枚举类型
     * @return 字段映射
     */
    public static <E extends Enum<E>> Map<Object, Object> toFieldMap(Class<E> enumClass, String keyField, String valueField) {
        requireNonBlank(keyField, "keyField 不能为空");
        requireNonBlank(valueField, "valueField 不能为空");
        Map<Object, Object> result = new LinkedHashMap<>();
        for (E item : valuesOf(enumClass)) {
            result.put(getFieldValue(item, keyField), getFieldValue(item, valueField));
        }
        return Collections.unmodifiableMap(result);
    }

    /**
     * 根据自定义键构建枚举映射。
     *
     * @param enumClass 枚举类
     * @param keyGetter 键获取函数
     * @param <E>       枚举类型
     * @param <K>       键类型
     * @return 键到枚举的映射
     */
    public static <E extends Enum<E>, K> Map<K, E> toEnumMap(Class<E> enumClass, Function<E, K> keyGetter) {
        Objects.requireNonNull(keyGetter, "keyGetter 不能为空");
        Map<K, E> result = new LinkedHashMap<>();
        for (E item : valuesOf(enumClass)) {
            result.put(keyGetter.apply(item), item);
        }
        return Collections.unmodifiableMap(result);
    }

    /**
     * 根据自定义键值函数构建值映射。
     *
     * @param enumClass   枚举类
     * @param keyGetter   键获取函数
     * @param valueGetter 值获取函数
     * @param <E>         枚举类型
     * @param <K>         键类型
     * @param <V>         值类型
     * @return 键值映射
     */
    public static <E extends Enum<E>, K, V> Map<K, V> toValueMap(Class<E> enumClass, Function<E, K> keyGetter, Function<E, V> valueGetter) {
        Objects.requireNonNull(keyGetter, "keyGetter 不能为空");
        Objects.requireNonNull(valueGetter, "valueGetter 不能为空");
        Map<K, V> result = new LinkedHashMap<>();
        for (E item : valuesOf(enumClass)) {
            result.put(keyGetter.apply(item), valueGetter.apply(item));
        }
        return Collections.unmodifiableMap(result);
    }

    /**
     * 根据指定字段分组。
     *
     * @param enumClass 枚举类
     * @param fieldName 字段名或属性名
     * @param <E>       枚举类型
     * @return 分组结果
     */
    public static <E extends Enum<E>> Map<Object, List<E>> groupByField(Class<E> enumClass, String fieldName) {
        requireNonBlank(fieldName, "fieldName 不能为空");
        return groupBy(enumClass, item -> getFieldValue(item, fieldName));
    }

    /**
     * 根据自定义分组函数分组。
     *
     * @param enumClass   枚举类
     * @param groupGetter 分组获取函数
     * @param <E>         枚举类型
     * @param <K>         分组键类型
     * @return 分组结果
     */
    public static <E extends Enum<E>, K> Map<K, List<E>> groupBy(Class<E> enumClass, Function<E, K> groupGetter) {
        Objects.requireNonNull(groupGetter, "groupGetter 不能为空");
        Map<K, List<E>> result = new LinkedHashMap<>();
        for (E item : valuesOf(enumClass)) {
            result.computeIfAbsent(groupGetter.apply(item), key -> new ArrayList<>()).add(item);
        }
        return immutableListMap(result);
    }

    /**
     * 转换为前端选项列表。
     *
     * @param enumClass 枚举类
     * @param <E>       枚举类型
     * @return 前端选项列表
     */
    public static <E extends Enum<E>> List<EnumOption> toOptions(Class<E> enumClass) {
        return toOptions(enumClass, false);
    }

    /**
     * 转换为前端选项列表。
     *
     * @param enumClass   枚举类
     * @param onlyEnabled 是否只返回启用项
     * @param <E>         枚举类型
     * @return 前端选项列表
     */
    public static <E extends Enum<E>> List<EnumOption> toOptions(Class<E> enumClass, boolean onlyEnabled) {
        return listOf(enumClass).stream()
                .filter(item -> !onlyEnabled || Boolean.TRUE.equals(getEnabled(item)))
                .sorted(enumComparator())
                .map(item -> toOption(item, false, false, null, null))
                .toList();
    }

    /**
     * 按指定编码字段和展示字段转换为前端选项列表。
     *
     * @param enumClass  枚举类
     * @param codeField  编码字段
     * @param labelField 展示字段
     * @param <E>        枚举类型
     * @return 前端选项列表
     */
    public static <E extends Enum<E>> List<EnumOption> toOptions(Class<E> enumClass, String codeField, String labelField) {
        requireNonBlank(codeField, "codeField 不能为空");
        requireNonBlank(labelField, "labelField 不能为空");
        return listOf(enumClass).stream()
                .sorted(enumComparator())
                .map(item -> toOption(item, false, false, codeField, labelField))
                .toList();
    }

    /**
     * 转换为编码到前端选项的映射。
     *
     * @param enumClass 枚举类
     * @param <E>       枚举类型
     * @return 编码到前端选项的映射
     */
    public static <E extends Enum<E>> Map<Object, EnumOption> toOptionMap(Class<E> enumClass) {
        Map<Object, EnumOption> result = new LinkedHashMap<>();
        for (EnumOption option : toOptions(enumClass)) {
            result.put(option.code(), option);
        }
        return Collections.unmodifiableMap(result);
    }

    /**
     * 转换为标准字典对象。
     *
     * @param enumClass 枚举类
     * @param <E>       枚举类型
     * @return 字典对象
     */
    public static <E extends Enum<E>> Map<String, Object> toDict(Class<E> enumClass) {
        EnumMetadata metadata = getEnumMetadata(enumClass);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("key", metadata.key());
        result.put("title", metadata.title());
        result.put("module", metadata.module());
        result.put("description", metadata.description());
        result.put("expose", metadata.expose());
        result.put("sort", metadata.sort());
        result.put("group", metadata.group());
        result.put("deprecatedFlag", metadata.deprecatedFlag());
        result.put("className", metadata.className());
        result.put("simpleName", metadata.simpleName());
        result.put("options", toOptions(enumClass));
        return Collections.unmodifiableMap(result);
    }

    /**
     * 转换为字典列表。
     *
     * @param enumClass 枚举类
     * @param <E>       枚举类型
     * @return 字典列表
     */
    public static <E extends Enum<E>> List<EnumOption> toDictList(Class<E> enumClass) {
        return toOptions(enumClass);
    }

    /**
     * 转换为单个枚举字典映射。
     *
     * @param enumClass 枚举类
     * @param <E>       枚举类型
     * @return 字典 key 到选项列表的映射
     */
    public static <E extends Enum<E>> Map<String, List<EnumOption>> toDictMap(Class<E> enumClass) {
        Map<String, List<EnumOption>> result = new LinkedHashMap<>();
        result.put(getEnumKey(enumClass), toOptions(enumClass));
        return Collections.unmodifiableMap(result);
    }

    /**
     * 转换为多个枚举字典映射。
     *
     * @param enumClasses 枚举类集合
     * @return 字典 key 到选项列表的映射
     */
    public static Map<String, List<EnumOption>> toDictMap(Collection<Class<?>> enumClasses) {
        if (enumClasses == null || enumClasses.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, List<EnumOption>> result = new LinkedHashMap<>();
        for (Class<?> enumClass : enumClasses) {
            if (isEnum(enumClass)) {
                @SuppressWarnings("unchecked")
                Class<? extends Enum<?>> castClass = (Class<? extends Enum<?>>) (Class<?>) enumClass.asSubclass(Enum.class);
                result.put(getEnumKey(castClass), toOptionsUnchecked(castClass));
            }
        }
        return Collections.unmodifiableMap(result);
    }

    /**
     * 转换为简单前端选项列表。
     *
     * @param enumClass 枚举类
     * @param <E>       枚举类型
     * @return 简单选项列表
     */
    public static <E extends Enum<E>> List<EnumOption> toSimpleOptions(Class<E> enumClass) {
        return listOf(enumClass).stream()
                .sorted(enumComparator())
                .map(item -> toOption(item, true, false, null, null))
                .toList();
    }

    /**
     * 转换为完整前端选项列表。
     *
     * @param enumClass 枚举类
     * @param <E>       枚举类型
     * @return 完整选项列表
     */
    public static <E extends Enum<E>> List<EnumOption> toFullOptions(Class<E> enumClass) {
        return listOf(enumClass).stream()
                .sorted(enumComparator())
                .map(item -> toOption(item, false, true, null, null))
                .toList();
    }

    /**
     * 转换为按分组聚合的前端选项。
     *
     * @param enumClass 枚举类
     * @param <E>       枚举类型
     * @return 分组到选项列表的映射
     */
    public static <E extends Enum<E>> Map<Object, List<EnumOption>> toGroupedOptions(Class<E> enumClass) {
        Map<Object, List<EnumOption>> result = new LinkedHashMap<>();
        for (EnumOption option : toOptions(enumClass)) {
            result.computeIfAbsent(option.group(), key -> new ArrayList<>()).add(option);
        }
        return immutableListMap(result);
    }

    /**
     * 转换为树形前端选项。
     *
     * @param enumClass 枚举类
     * @param <E>       枚举类型
     * @return 树形选项列表
     */
    public static <E extends Enum<E>> List<EnumOption> toTreeOptions(Class<E> enumClass) {
        List<EnumOption> options = toFullOptions(enumClass);
        Map<Object, List<EnumOption>> childrenMap = options.stream()
                .filter(option -> option.parentCode() != null && !isBlank(String.valueOf(option.parentCode())))
                .collect(Collectors.groupingBy(EnumOption::parentCode, LinkedHashMap::new, Collectors.toList()));
        List<EnumOption> roots = new ArrayList<>();
        for (EnumOption option : options) {
            if (option.parentCode() == null || isBlank(String.valueOf(option.parentCode())) || !containsOptionCode(options, option.parentCode())) {
                roots.add(option.withChildren(childrenMap.getOrDefault(option.code(), List.of())));
            }
        }
        return List.copyOf(roots);
    }

    /**
     * 根据业务编码获取单个前端选项。
     *
     * @param enumClass 枚举类
     * @param code      业务编码
     * @param <E>       枚举类型
     * @return 前端选项，不存在时返回 null
     */
    public static <E extends Enum<E>> EnumOption getOptionByCode(Class<E> enumClass, Object code) {
        E enumValue = getByCode(enumClass, code);
        return enumValue == null ? null : toOption(enumValue, false, true, null, null);
    }

    /**
     * 扫描指定包下所有枚举类。
     *
     * @param basePackage 基础包路径
     * @return 枚举类列表
     */
    public static List<Class<? extends Enum<?>>> scanEnums(String basePackage) {
        return scanEnumClasses(basePackage);
    }

    /**
     * 批量扫描多个包下的枚举类。
     *
     * @param basePackages 基础包路径集合
     * @return 枚举类列表
     */
    public static List<Class<? extends Enum<?>>> scanEnums(Collection<String> basePackages) {
        if (basePackages == null || basePackages.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Class<? extends Enum<?>>> result = new LinkedHashSet<>();
        for (String basePackage : basePackages) {
            if (!isBlank(basePackage)) {
                result.addAll(scanEnumClasses(basePackage));
            }
        }
        return List.copyOf(result);
    }

    /**
     * 扫描实现指定接口的枚举类。
     *
     * @param basePackage    基础包路径
     * @param interfaceClass 接口类
     * @return 枚举类列表
     */
    public static List<Class<? extends Enum<?>>> scanEnumsByInterface(String basePackage, Class<?> interfaceClass) {
        Objects.requireNonNull(interfaceClass, "interfaceClass 不能为空");
        return scanEnumClasses(basePackage).stream()
                .filter(enumClass -> interfaceClass.isAssignableFrom(enumClass))
                .toList();
    }

    /**
     * 扫描标注指定注解的枚举类。
     *
     * @param basePackage     基础包路径
     * @param annotationClass 注解类
     * @return 枚举类列表
     */
    public static List<Class<? extends Enum<?>>> scanEnumsByAnnotation(String basePackage, Class<? extends Annotation> annotationClass) {
        Objects.requireNonNull(annotationClass, "annotationClass 不能为空");
        return scanEnumClasses(basePackage).stream()
                .filter(enumClass -> enumClass.isAnnotationPresent(annotationClass))
                .toList();
    }

    /**
     * 扫描指定包及子包下所有枚举类。
     *
     * @param basePackage 基础包路径
     * @return 枚举类列表
     */
    public static List<Class<? extends Enum<?>>> scanEnumClasses(String basePackage) {
        return scanEnumClasses(basePackage, true);
    }

    /**
     * 扫描指定包下所有枚举类。
     *
     * @param basePackage 基础包路径
     * @param recursive   是否递归扫描子包
     * @return 枚举类列表
     */
    public static List<Class<? extends Enum<?>>> scanEnumClasses(String basePackage, boolean recursive) {
        requireNonBlank(basePackage, "basePackage 不能为空");
        String packagePath = basePackage.trim().replace('.', '/');
        ClassLoader classLoader = getClassLoader();
        Set<Class<? extends Enum<?>>> result = new LinkedHashSet<>();
        try {
            Enumeration<URL> resources = classLoader.getResources(packagePath);
            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                String protocol = url.getProtocol();
                if ("file".equals(protocol)) {
                    scanFilePackage(basePackage.trim(), url, recursive, classLoader, result);
                } else if ("jar".equals(protocol)) {
                    scanJarPackage(packagePath, url, recursive, classLoader, result);
                }
            }
        } catch (IOException ex) {
            throw new IllegalStateException("扫描枚举包失败: " + basePackage, ex);
        }
        return result.stream()
                .sorted(Comparator.comparing(Class::getName))
                .toList();
    }

    /**
     * 扫描并转换为字典列表。
     *
     * @param basePackage 基础包路径
     * @return 字典对象列表
     */
    public static List<Map<String, Object>> scanEnumDicts(String basePackage) {
        return scanEnumClasses(basePackage).stream()
                .filter(EnumUtil::isExposed)
                .map(EnumUtil::toDictUnchecked)
                .toList();
    }

    /**
     * 扫描并转换为前端字典映射。
     *
     * @param basePackage 基础包路径
     * @return 字典 key 到选项列表的映射
     */
    public static Map<String, List<EnumOption>> scanEnumDictMap(String basePackage) {
        Map<String, List<EnumOption>> result = new LinkedHashMap<>();
        for (Class<? extends Enum<?>> enumClass : scanEnumClasses(basePackage)) {
            if (isExposed(enumClass)) {
                result.put(getEnumKey(enumClass), toOptionsUnchecked(enumClass));
            }
        }
        return Collections.unmodifiableMap(result);
    }

    /**
     * 扫描指定包并注册枚举。
     *
     * @param basePackage 基础包路径
     */
    public static void registerEnums(String basePackage) {
        registerEnums(new ArrayList<>(scanEnumClasses(basePackage)));
    }

    /**
     * 注册单个枚举。
     *
     * @param enumClass 枚举类
     * @param <E>       枚举类型
     */
    public static <E extends Enum<E>> void registerEnum(Class<E> enumClass) {
        requireEnumClass(enumClass);
        @SuppressWarnings("unchecked")
        Class<? extends Enum<?>> castClass = (Class<? extends Enum<?>>) (Class<?>) enumClass.asSubclass(Enum.class);
        REGISTERED_ENUMS.put(getEnumKey(enumClass), castClass);
    }

    /**
     * 批量注册枚举。
     *
     * @param enumClasses 枚举类集合
     */
    public static void registerEnums(Collection<Class<?>> enumClasses) {
        if (enumClasses == null || enumClasses.isEmpty()) {
            return;
        }
        for (Class<?> enumClass : enumClasses) {
            if (isEnum(enumClass)) {
                @SuppressWarnings("unchecked")
                Class<? extends Enum<?>> castClass = (Class<? extends Enum<?>>) (Class<?>) enumClass.asSubclass(Enum.class);
                REGISTERED_ENUMS.put(getEnumKey(castClass), castClass);
            }
        }
    }

    /**
     * 获取已注册枚举。
     *
     * @return 已注册枚举映射
     */
    public static Map<String, Class<? extends Enum<?>>> getRegisteredEnums() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(REGISTERED_ENUMS));
    }

    /**
     * 获取已注册枚举字典映射。
     *
     * @return 字典 key 到选项列表的映射
     */
    public static Map<String, List<EnumOption>> getRegisteredEnumDictMap() {
        Map<String, List<EnumOption>> result = new LinkedHashMap<>();
        REGISTERED_ENUMS.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    if (isExposed(entry.getValue())) {
                        result.put(entry.getKey(), toOptionsUnchecked(entry.getValue()));
                    }
                });
        return Collections.unmodifiableMap(result);
    }

    /**
     * 刷新枚举注册缓存。
     */
    public static void refreshEnumRegistry() {
        Map<String, Class<? extends Enum<?>>> copy = new LinkedHashMap<>(REGISTERED_ENUMS);
        REGISTERED_ENUMS.clear();
        copy.values().forEach(enumClass -> REGISTERED_ENUMS.put(getEnumKey(enumClass), enumClass));
    }

    /**
     * 清空枚举注册缓存。
     */
    public static void clearEnumRegistry() {
        REGISTERED_ENUMS.clear();
    }

    /**
     * 获取枚举字典 key。
     *
     * @param enumClass 枚举类
     * @return 字典 key
     */
    public static String getEnumKey(Class<?> enumClass) {
        Class<?> checked = requireAnyEnumClass(enumClass);
        EnumDict annotation = checked.getAnnotation(EnumDict.class);
        if (annotation != null && !isBlank(annotation.key())) {
            return annotation.key().trim();
        }
        Object value = invokeStaticNoArg(checked, "getEnumKey");
        if (value != null && !isBlank(String.valueOf(value))) {
            return String.valueOf(value).trim();
        }
        value = invokeStaticNoArg(checked, "getDictKey");
        if (value != null && !isBlank(String.valueOf(value))) {
            return String.valueOf(value).trim();
        }
        return toKebabKey(checked.getSimpleName());
    }

    /**
     * 获取枚举字典标题。
     *
     * @param enumClass 枚举类
     * @return 字典标题
     */
    public static String getEnumTitle(Class<?> enumClass) {
        Class<?> checked = requireAnyEnumClass(enumClass);
        EnumDict annotation = checked.getAnnotation(EnumDict.class);
        if (annotation != null && !isBlank(annotation.title())) {
            return annotation.title().trim();
        }
        return checked.getSimpleName();
    }

    /**
     * 获取枚举所属模块。
     *
     * @param enumClass 枚举类
     * @return 所属模块
     */
    public static String getEnumModule(Class<?> enumClass) {
        Class<?> checked = requireAnyEnumClass(enumClass);
        EnumDict annotation = checked.getAnnotation(EnumDict.class);
        return annotation == null ? null : emptyToNull(annotation.module());
    }

    /**
     * 获取枚举描述。
     *
     * @param enumClass 枚举类
     * @return 枚举描述
     */
    public static String getEnumDescription(Class<?> enumClass) {
        Class<?> checked = requireAnyEnumClass(enumClass);
        EnumDict annotation = checked.getAnnotation(EnumDict.class);
        return annotation == null ? null : emptyToNull(annotation.description());
    }

    /**
     * 判断枚举是否允许暴露给前端。
     *
     * @param enumClass 枚举类
     * @return 是否允许暴露
     */
    public static boolean isExposed(Class<?> enumClass) {
        Class<?> checked = requireAnyEnumClass(enumClass);
        EnumDict annotation = checked.getAnnotation(EnumDict.class);
        return annotation == null || annotation.expose();
    }

    /**
     * 判断枚举是否废弃。
     *
     * @param enumClass 枚举类
     * @return 是否废弃
     */
    public static boolean isDeprecated(Class<?> enumClass) {
        Class<?> checked = requireAnyEnumClass(enumClass);
        EnumDict annotation = checked.getAnnotation(EnumDict.class);
        return checked.isAnnotationPresent(Deprecated.class) || (annotation != null && annotation.deprecatedFlag());
    }

    /**
     * 获取枚举元数据。
     *
     * @param enumClass 枚举类
     * @return 枚举元数据
     */
    public static EnumMetadata getEnumMetadata(Class<?> enumClass) {
        Class<?> checked = requireAnyEnumClass(enumClass);
        EnumDict annotation = checked.getAnnotation(EnumDict.class);
        List<String> extraFields = annotation == null ? List.of() : Arrays.stream(annotation.extraFields()).filter(item -> !isBlank(item)).toList();
        return new EnumMetadata(
                getEnumKey(checked),
                getEnumTitle(checked),
                getEnumModule(checked),
                getEnumDescription(checked),
                isExposed(checked),
                annotation == null ? 0 : annotation.sort(),
                annotation == null ? null : emptyToNull(annotation.group()),
                isDeprecated(checked),
                extraFields,
                checked.getName(),
                checked.getSimpleName()
        );
    }

    /**
     * 获取枚举项元数据。
     *
     * @param enumValue 枚举对象
     * @return 枚举项元数据映射
     */
    public static Map<String, Object> getItemMetadata(Enum<?> enumValue) {
        if (enumValue == null) {
            return Collections.emptyMap();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("name", getName(enumValue));
        result.put("ordinal", getOrdinal(enumValue));
        result.put("code", getCode(enumValue));
        result.put("label", getLabel(enumValue));
        result.put("desc", getDesc(enumValue));
        result.put("sort", getSort(enumValue));
        result.put("enabled", getEnabled(enumValue));
        result.put("group", getGroupValue(enumValue));
        result.put("parentCode", getParentCode(enumValue));
        result.put("extra", getExtra(enumValue));
        return Collections.unmodifiableMap(result);
    }

    /**
     * 过滤启用枚举。
     *
     * @param enumClass 枚举类
     * @param <E>       枚举类型
     * @return 启用枚举列表
     */
    public static <E extends Enum<E>> List<E> filterEnabled(Class<E> enumClass) {
        return getListByPredicate(enumClass, item -> Boolean.TRUE.equals(getEnabled(item)));
    }

    /**
     * 过滤禁用枚举。
     *
     * @param enumClass 枚举类
     * @param <E>       枚举类型
     * @return 禁用枚举列表
     */
    public static <E extends Enum<E>> List<E> filterDisabled(Class<E> enumClass) {
        return getListByPredicate(enumClass, item -> Boolean.FALSE.equals(getEnabled(item)));
    }

    /**
     * 按分组过滤枚举。
     *
     * @param enumClass 枚举类
     * @param group     分组值
     * @param <E>       枚举类型
     * @return 匹配的枚举列表
     */
    public static <E extends Enum<E>> List<E> filterByGroup(Class<E> enumClass, Object group) {
        return getListByPredicate(enumClass, item -> valueEquals(getGroupValue(item), group));
    }

    /**
     * 按字段过滤枚举。
     *
     * @param enumClass 枚举类
     * @param fieldName 字段名或属性名
     * @param value     字段值
     * @param <E>       枚举类型
     * @return 匹配的枚举列表
     */
    public static <E extends Enum<E>> List<E> filterByField(Class<E> enumClass, String fieldName, Object value) {
        return getListByField(enumClass, fieldName, value);
    }

    /**
     * 按枚举序号排序。
     *
     * @param enumClass 枚举类
     * @param <E>       枚举类型
     * @return 排序后的枚举列表
     */
    public static <E extends Enum<E>> List<E> sortByOrdinal(Class<E> enumClass) {
        return listOf(enumClass).stream().sorted(Comparator.comparingInt(Enum::ordinal)).toList();
    }

    /**
     * 按业务编码排序。
     *
     * @param enumClass 枚举类
     * @param <E>       枚举类型
     * @return 排序后的枚举列表
     */
    public static <E extends Enum<E>> List<E> sortByCode(Class<E> enumClass) {
        return sortByField(enumClass, "code");
    }

    /**
     * 按业务排序字段排序。
     *
     * @param enumClass 枚举类
     * @param <E>       枚举类型
     * @return 排序后的枚举列表
     */
    public static <E extends Enum<E>> List<E> sortBySort(Class<E> enumClass) {
        return listOf(enumClass).stream().sorted(enumComparator()).toList();
    }

    /**
     * 按指定字段排序。
     *
     * @param enumClass 枚举类
     * @param fieldName 字段名或属性名
     * @param <E>       枚举类型
     * @return 排序后的枚举列表
     */
    public static <E extends Enum<E>> List<E> sortByField(Class<E> enumClass, String fieldName) {
        requireNonBlank(fieldName, "fieldName 不能为空");
        return listOf(enumClass).stream()
                .sorted((left, right) -> compareNullable(getFieldValue(left, fieldName), getFieldValue(right, fieldName)))
                .toList();
    }

    /**
     * 按业务分组字段分组。
     *
     * @param enumClass 枚举类
     * @param <E>       枚举类型
     * @return 分组结果
     */
    public static <E extends Enum<E>> Map<Object, List<E>> groupByGroup(Class<E> enumClass) {
        return groupBy(enumClass, EnumUtil::getGroupValue);
    }

    /**
     * 按模块分组枚举类。
     *
     * @param enumClasses 枚举类集合
     * @return 模块到枚举类的分组结果
     */
    public static Map<String, List<Class<?>>> groupByModule(Collection<Class<?>> enumClasses) {
        if (enumClasses == null || enumClasses.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, List<Class<?>>> result = new LinkedHashMap<>();
        for (Class<?> enumClass : enumClasses) {
            if (isEnum(enumClass)) {
                result.computeIfAbsent(getEnumModule(enumClass), key -> new ArrayList<>()).add(enumClass);
            }
        }
        return immutableListMap(result);
    }

    /**
     * 批量将编码转换为枚举列表。
     *
     * @param enumClass 枚举类
     * @param codes     编码集合
     * @param <E>       枚举类型
     * @return 枚举列表
     */
    public static <E extends Enum<E>> List<E> codesToEnums(Class<E> enumClass, Collection<?> codes) {
        requireEnumClass(enumClass);
        if (codes == null || codes.isEmpty()) {
            return Collections.emptyList();
        }
        return codes.stream().map(code -> getByCode(enumClass, code)).filter(Objects::nonNull).toList();
    }

    /**
     * 批量将枚举转换为编码列表。
     *
     * @param enumValues 枚举集合
     * @return 编码列表
     */
    public static List<Object> enumsToCodes(Collection<? extends Enum<?>> enumValues) {
        if (enumValues == null || enumValues.isEmpty()) {
            return Collections.emptyList();
        }
        return enumValues.stream().filter(Objects::nonNull).map(EnumUtil::getCode).toList();
    }

    /**
     * 批量将编码转换为展示名称列表。
     *
     * @param enumClass 枚举类
     * @param codes     编码集合
     * @param <E>       枚举类型
     * @return 展示名称列表
     */
    public static <E extends Enum<E>> List<String> codesToLabels(Class<E> enumClass, Collection<?> codes) {
        return codesToEnums(enumClass, codes).stream().map(EnumUtil::getLabel).toList();
    }

    /**
     * 批量将名称转换为枚举列表。
     *
     * @param enumClass 枚举类
     * @param names     名称集合
     * @param <E>       枚举类型
     * @return 枚举列表
     */
    public static <E extends Enum<E>> List<E> namesToEnums(Class<E> enumClass, Collection<String> names) {
        requireEnumClass(enumClass);
        if (names == null || names.isEmpty()) {
            return Collections.emptyList();
        }
        return names.stream().map(name -> getByName(enumClass, name)).filter(Objects::nonNull).toList();
    }

    /**
     * 根据编码集合生成编码到展示名称映射。
     *
     * @param enumClass 枚举类
     * @param codes     编码集合
     * @param <E>       枚举类型
     * @return 编码到展示名称映射
     */
    public static <E extends Enum<E>> Map<Object, String> toLabelMap(Class<E> enumClass, Collection<?> codes) {
        if (codes == null || codes.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Object, String> result = new LinkedHashMap<>();
        for (Object code : codes) {
            result.put(code, codeToLabel(enumClass, code));
        }
        return Collections.unmodifiableMap(result);
    }

    /**
     * 给业务列表填充枚举展示名称。
     *
     * @param records     业务记录集合
     * @param codeGetter  编码获取函数
     * @param labelSetter 展示名称写入函数
     * @param enumClass   枚举类
     * @param <T>         业务记录类型
     * @param <E>         枚举类型
     */
    public static <T, E extends Enum<E>> void fillLabel(Collection<T> records, Function<T, ?> codeGetter, BiConsumer<T, String> labelSetter, Class<E> enumClass) {
        if (records == null || records.isEmpty()) {
            return;
        }
        Objects.requireNonNull(codeGetter, "codeGetter 不能为空");
        Objects.requireNonNull(labelSetter, "labelSetter 不能为空");
        requireEnumClass(enumClass);
        for (T record : records) {
            if (record != null) {
                labelSetter.accept(record, codeToLabel(enumClass, codeGetter.apply(record)));
            }
        }
    }

    /**
     * 将编码替换为展示名称。
     *
     * @param enumClass 枚举类
     * @param code      编码
     * @param <E>       枚举类型
     * @return 展示名称，不存在时返回原编码字符串
     */
    public static <E extends Enum<E>> String replaceCodeWithLabel(Class<E> enumClass, Object code) {
        String label = codeToLabel(enumClass, code);
        return label == null ? (code == null ? null : String.valueOf(code)) : label;
    }

    /**
     * 解析分隔字符串为枚举列表。
     *
     * @param enumClass 枚举类
     * @param codes     编码字符串
     * @param separator 分隔符
     * @param <E>       枚举类型
     * @return 枚举列表
     */
    public static <E extends Enum<E>> List<E> parseCodes(Class<E> enumClass, String codes, String separator) {
        requireEnumClass(enumClass);
        if (isBlank(codes)) {
            return Collections.emptyList();
        }
        String actualSeparator = separator == null || separator.isEmpty() ? "," : separator;
        return Arrays.stream(codes.split(java.util.regex.Pattern.quote(actualSeparator)))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .map(code -> getByCode(enumClass, code))
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * 转换为带扩展字段的前端选项列表。
     *
     * @param enumClass 枚举类
     * @param <E>       枚举类型
     * @return 带扩展字段的选项列表
     */
    public static <E extends Enum<E>> List<EnumOption> toOptionsWithExtra(Class<E> enumClass) {
        return listOf(enumClass).stream()
                .sorted(enumComparator())
                .map(item -> toOption(item, false, true, null, null))
                .toList();
    }

    /**
     * 转换为完整字典对象。
     *
     * @param enumClass 枚举类
     * @param <E>       枚举类型
     * @return 完整字典对象
     */
    public static <E extends Enum<E>> Map<String, Object> toFullDict(Class<E> enumClass) {
        Map<String, Object> result = new LinkedHashMap<>(toDict(enumClass));
        result.put("metadata", getEnumMetadata(enumClass));
        result.put("options", toFullOptions(enumClass));
        return Collections.unmodifiableMap(result);
    }

    /**
     * 获取枚举扩展字段。
     *
     * @param enumValue 枚举对象
     * @return 扩展字段映射
     */
    public static Map<String, Object> getExtra(Enum<?> enumValue) {
        if (enumValue == null) {
            return Collections.emptyMap();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        if (enumValue instanceof FrontendEnum<?> frontendEnum) {
            Map<String, Object> extra = frontendEnum.getExtra();
            if (extra != null) {
                result.putAll(extra);
            }
        }
        EnumItem annotation = getItemAnnotation(enumValue);
        if (annotation != null) {
            putIfNotBlank(result, "color", annotation.color());
            putIfNotBlank(result, "tagType", annotation.tagType());
            putIfNotBlank(result, "icon", annotation.icon());
            putIfNotBlank(result, "cssClass", annotation.cssClass());
            putIfNotBlank(result, "permission", annotation.permission());
            putIfNotBlank(result, "remark", annotation.remark());
            putIfNotBlank(result, "parentCode", annotation.parentCode());
        }
        addKnownExtraField(result, enumValue, "color");
        addKnownExtraField(result, enumValue, "tagType");
        addKnownExtraField(result, enumValue, "icon");
        addKnownExtraField(result, enumValue, "cssClass");
        addKnownExtraField(result, enumValue, "permission");
        addKnownExtraField(result, enumValue, "remark");
        return Collections.unmodifiableMap(result);
    }

    /**
     * 获取指定扩展字段。
     *
     * @param enumValue 枚举对象
     * @param fieldName 扩展字段名
     * @return 扩展字段值
     */
    public static Object getExtraField(Enum<?> enumValue, String fieldName) {
        requireNonBlank(fieldName, "fieldName 不能为空");
        return getExtra(enumValue).get(fieldName.trim());
    }

    /**
     * 转换枚举项为扩展字段映射。
     *
     * @param enumValue 枚举对象
     * @return 扩展字段映射
     */
    public static Map<String, Object> toExtraMap(Enum<?> enumValue) {
        return getExtra(enumValue);
    }

    /**
     * 设置全局允许输出的扩展字段。
     *
     * @param fields 字段集合，空集合表示不限制
     */
    public static void setExtraFields(Collection<String> fields) {
        GLOBAL_EXTRA_FIELDS.clear();
        if (fields != null) {
            fields.stream().filter(item -> !isBlank(item)).map(String::trim).forEach(GLOBAL_EXTRA_FIELDS::add);
        }
    }

    /**
     * 过滤扩展字段。
     *
     * @param extra    扩展字段映射
     * @param includes 允许保留的字段集合
     * @return 过滤后的扩展字段映射
     */
    public static Map<String, Object> filterExtraFields(Map<String, Object> extra, Collection<String> includes) {
        if (extra == null || extra.isEmpty()) {
            return Collections.emptyMap();
        }
        if (includes == null || includes.isEmpty()) {
            return Collections.unmodifiableMap(new LinkedHashMap<>(extra));
        }
        Set<String> includeSet = includes.stream().filter(item -> !isBlank(item)).map(String::trim).collect(Collectors.toCollection(LinkedHashSet::new));
        Map<String, Object> result = new LinkedHashMap<>();
        extra.forEach((key, value) -> {
            if (includeSet.contains(key)) {
                result.put(key, value);
            }
        });
        return Collections.unmodifiableMap(result);
    }

    /**
     * 根据业务编码获取枚举，不存在时返回默认值。
     *
     * @param enumClass    枚举类
     * @param code         业务编码
     * @param defaultValue 默认枚举
     * @param <E>          枚举类型
     * @return 匹配枚举或默认枚举
     */
    public static <E extends Enum<E>> E getByCodeOrDefault(Class<E> enumClass, Object code, E defaultValue) {
        E value = getByCode(enumClass, code);
        return value == null ? defaultValue : value;
    }

    /**
     * 根据业务编码获取展示名称，不存在时返回默认展示名称。
     *
     * @param enumClass    枚举类
     * @param code         业务编码
     * @param defaultLabel 默认展示名称
     * @param <E>          枚举类型
     * @return 展示名称
     */
    public static <E extends Enum<E>> String getLabelOrDefault(Class<E> enumClass, Object code, String defaultLabel) {
        String label = codeToLabel(enumClass, code);
        return label == null ? defaultLabel : label;
    }

    /**
     * 根据业务编码获取描述，不存在时返回默认描述。
     *
     * @param enumClass   枚举类
     * @param code        业务编码
     * @param defaultDesc 默认描述
     * @param <E>         枚举类型
     * @return 描述
     */
    public static <E extends Enum<E>> String getDescOrDefault(Class<E> enumClass, Object code, String defaultDesc) {
        String desc = codeToDesc(enumClass, code);
        return desc == null ? defaultDesc : desc;
    }

    /**
     * 根据业务编码获取枚举，不存在时抛出指定异常信息。
     *
     * @param enumClass 枚举类
     * @param code      业务编码
     * @param message   异常信息
     * @param <E>       枚举类型
     * @return 匹配枚举
     */
    public static <E extends Enum<E>> E requireByCode(Class<E> enumClass, Object code, String message) {
        E value = getByCode(enumClass, code);
        if (value == null) {
            throw new IllegalArgumentException(isBlank(message) ? "枚举编码不存在" : message);
        }
        return value;
    }

    /**
     * 根据枚举名称获取枚举，不存在时抛出指定异常信息。
     *
     * @param enumClass 枚举类
     * @param name      枚举名称
     * @param message   异常信息
     * @param <E>       枚举类型
     * @return 匹配枚举
     */
    public static <E extends Enum<E>> E requireByName(Class<E> enumClass, String name, String message) {
        E value = getByName(enumClass, name);
        if (value == null) {
            throw new IllegalArgumentException(isBlank(message) ? "枚举名称不存在" : message);
        }
        return value;
    }

    /**
     * 根据业务编码获取 Optional 枚举。
     *
     * @param enumClass 枚举类
     * @param code      业务编码
     * @param <E>       枚举类型
     * @return Optional 枚举
     */
    public static <E extends Enum<E>> Optional<E> optionalByCode(Class<E> enumClass, Object code) {
        return Optional.ofNullable(getByCode(enumClass, code));
    }

    /**
     * 根据枚举名称获取 Optional 枚举。
     *
     * @param enumClass 枚举类
     * @param name      枚举名称
     * @param <E>       枚举类型
     * @return Optional 枚举
     */
    public static <E extends Enum<E>> Optional<E> optionalByName(Class<E> enumClass, String name) {
        return Optional.ofNullable(getByName(enumClass, name));
    }

    /**
     * 启用缓存。
     */
    public static void enableCache() {
        cacheEnabled = true;
    }

    /**
     * 禁用缓存并清理已缓存内容。
     */
    public static void disableCache() {
        cacheEnabled = false;
        clearCache();
    }

    /**
     * 判断缓存是否启用。
     *
     * @return 是否启用缓存
     */
    public static boolean isCacheEnabled() {
        return cacheEnabled;
    }

    /**
     * 清理全部缓存。
     */
    public static void clearCache() {
        ENUM_CACHE.clear();
    }

    /**
     * 清理指定枚举缓存。
     *
     * @param enumClass 枚举类
     * @param <E>       枚举类型
     */
    public static <E extends Enum<E>> void clearCache(Class<E> enumClass) {
        requireEnumClass(enumClass);
        ENUM_CACHE.remove(enumClass);
    }

    /**
     * 刷新指定枚举缓存。
     *
     * @param enumClass 枚举类
     * @param <E>       枚举类型
     */
    public static <E extends Enum<E>> void refreshCache(Class<E> enumClass) {
        requireEnumClass(enumClass);
        ENUM_CACHE.remove(enumClass);
        if (cacheEnabled) {
            getEnumCache(enumClass);
        }
    }

    /**
     * 刷新全部枚举缓存。
     */
    public static void refreshAllCache() {
        List<Class<?>> classes = new ArrayList<>(ENUM_CACHE.keySet());
        ENUM_CACHE.clear();
        if (cacheEnabled) {
            classes.forEach(EnumUtil::getEnumCacheUnchecked);
        }
    }

    /**
     * 获取缓存数量。
     *
     * @return 缓存数量
     */
    public static int getCacheSize() {
        return ENUM_CACHE.size();
    }

    /**
     * 获取已缓存枚举类。
     *
     * @return 已缓存枚举类集合
     */
    public static Set<Class<?>> getCachedEnumClasses() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(ENUM_CACHE.keySet()));
    }

    private static <E extends Enum<E>> E getFirstByPredicate(Class<E> enumClass, Predicate<E> predicate) {
        requireEnumClass(enumClass);
        Objects.requireNonNull(predicate, "predicate 不能为空");
        for (E item : valuesOf(enumClass)) {
            if (predicate.test(item)) {
                return item;
            }
        }
        return null;
    }

    private static <E extends Enum<E>> Class<E> requireEnumClass(Class<E> enumClass) {
        Objects.requireNonNull(enumClass, "enumClass 不能为空");
        if (!enumClass.isEnum()) {
            throw new IllegalArgumentException("enumClass 必须是枚举类型: " + enumClass.getName());
        }
        return enumClass;
    }

    private static Class<?> requireAnyEnumClass(Class<?> enumClass) {
        Objects.requireNonNull(enumClass, "enumClass 不能为空");
        if (!enumClass.isEnum()) {
            throw new IllegalArgumentException("enumClass 必须是枚举类型: " + enumClass.getName());
        }
        return enumClass;
    }

    private static void requireNonBlank(String value, String message) {
        if (isBlank(value)) {
            throw new IllegalArgumentException(message);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String emptyToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private static boolean valueEquals(Object source, Object target) {
        if (Objects.equals(source, target)) {
            return true;
        }
        if (source == null || target == null) {
            return false;
        }
        return String.valueOf(source).equals(String.valueOf(target));
    }

    private static Object firstNonNull(Object... values) {
        if (values == null) {
            return null;
        }
        for (Object value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private static Object getPropertyValue(Enum<?> enumValue, String propertyName) {
        if (enumValue == null || isBlank(propertyName)) {
            return null;
        }
        EnumCache cache = getEnumCache(enumValue.getDeclaringClass());
        Method method = cache.methods().get(propertyName);
        if (method != null) {
            return invokeMethod(enumValue, method);
        }
        Field field = cache.fields().get(propertyName);
        if (field != null) {
            return getField(enumValue, field);
        }
        return null;
    }

    private static EnumCache getEnumCache(Class<?> enumClass) {
        requireAnyEnumClass(enumClass);
        if (!cacheEnabled) {
            return buildEnumCache(enumClass);
        }
        return ENUM_CACHE.computeIfAbsent(enumClass, EnumUtil::buildEnumCache);
    }

    private static EnumCache getEnumCacheUnchecked(Class<?> enumClass) {
        return getEnumCache(enumClass);
    }

    private static EnumCache buildEnumCache(Class<?> enumClass) {
        Map<String, Method> methods = new LinkedHashMap<>();
        for (Method method : enumClass.getMethods()) {
            if (method.getParameterCount() == 0 && !Modifier.isStatic(method.getModifiers()) && method.getDeclaringClass() != Object.class) {
                String name = method.getName();
                try {
                    method.setAccessible(true);
                } catch (RuntimeException ignored) {
                    // public 方法无需强制开放访问权限。
                }
                methods.putIfAbsent(name, method);
                if (name.startsWith("get") && name.length() > 3) {
                    methods.putIfAbsent(decapitalize(name.substring(3)), method);
                } else if (name.startsWith("is") && name.length() > 2) {
                    methods.putIfAbsent(decapitalize(name.substring(2)), method);
                }
            }
        }
        Map<String, Field> fields = new LinkedHashMap<>();
        Class<?> current = enumClass;
        while (current != null && current != Object.class && current != Enum.class) {
            for (Field field : current.getDeclaredFields()) {
                if (!field.isSynthetic() && !field.isEnumConstant() && !Modifier.isStatic(field.getModifiers())) {
                    field.setAccessible(true);
                    fields.putIfAbsent(field.getName(), field);
                }
            }
            current = current.getSuperclass();
        }
        return new EnumCache(Collections.unmodifiableMap(methods), Collections.unmodifiableMap(fields));
    }

    private static String decapitalize(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        return Character.toLowerCase(value.charAt(0)) + value.substring(1);
    }

    private static Object invokeMethod(Object target, Method method) {
        try {
            return method.invoke(target);
        } catch (IllegalAccessException | InvocationTargetException ex) {
            throw new IllegalStateException("调用枚举方法失败: " + method.getName(), ex);
        }
    }

    private static Object getField(Object target, Field field) {
        try {
            return field.get(target);
        } catch (IllegalAccessException ex) {
            throw new IllegalStateException("读取枚举字段失败: " + field.getName(), ex);
        }
    }

    private static EnumItem getItemAnnotation(Enum<?> enumValue) {
        if (enumValue == null) {
            return null;
        }
        try {
            Field field = enumValue.getDeclaringClass().getField(enumValue.name());
            return field.getAnnotation(EnumItem.class);
        } catch (NoSuchFieldException ex) {
            return null;
        }
    }

    private static Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.valueOf(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static Boolean toBoolean(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        return Boolean.valueOf(String.valueOf(value));
    }

    private static Object getGroupValue(Enum<?> enumValue) {
        if (enumValue == null) {
            return null;
        }
        if (enumValue instanceof GroupEnum<?> groupEnum) {
            return groupEnum.getGroup();
        }
        EnumItem annotation = getItemAnnotation(enumValue);
        if (annotation != null && !isBlank(annotation.group())) {
            return annotation.group();
        }
        return getPropertyValue(enumValue, "group");
    }

    private static Object getParentCode(Enum<?> enumValue) {
        if (enumValue == null) {
            return null;
        }
        Object value = getPropertyValue(enumValue, "parentCode");
        if (value != null) {
            return value;
        }
        EnumItem annotation = getItemAnnotation(enumValue);
        return annotation == null ? null : emptyToNull(annotation.parentCode());
    }

    private static <E extends Enum<E>> Comparator<E> enumComparator() {
        return Comparator.<E, Integer>comparing(EnumUtil::getSort, Comparator.nullsLast(Integer::compareTo))
                .thenComparingInt(Enum::ordinal)
                .thenComparing(Enum::name);
    }

    private static int compareNullable(Object left, Object right) {
        if (left == right) {
            return 0;
        }
        if (left == null) {
            return 1;
        }
        if (right == null) {
            return -1;
        }
        if (left instanceof Comparable<?> comparable && left.getClass().isInstance(right)) {
            @SuppressWarnings("unchecked")
            Comparable<Object> castComparable = (Comparable<Object>) comparable;
            return castComparable.compareTo(right);
        }
        return String.valueOf(left).compareTo(String.valueOf(right));
    }

    private static <K, V> Map<K, List<V>> immutableListMap(Map<K, List<V>> source) {
        Map<K, List<V>> result = new LinkedHashMap<>();
        source.forEach((key, value) -> result.put(key, List.copyOf(value)));
        return Collections.unmodifiableMap(result);
    }

    private static EnumOption toOption(Enum<?> enumValue, boolean simple, boolean includeExtra, String codeField, String labelField) {
        Object code = codeField == null ? getCode(enumValue) : getFieldValue(enumValue, codeField);
        String label = labelField == null ? getLabel(enumValue) : objectToString(getFieldValue(enumValue, labelField));
        Map<String, Object> extra = includeExtra ? getFilteredExtra(enumValue) : Map.of();
        if (simple) {
            return new EnumOption(code, label, null, null, null, null, null, null, List.of(), Map.of());
        }
        return new EnumOption(
                code,
                label,
                getName(enumValue),
                getDesc(enumValue),
                getSort(enumValue),
                getEnabled(enumValue),
                getGroupValue(enumValue),
                getParentCode(enumValue),
                List.of(),
                extra
        );
    }

    private static Map<String, Object> getFilteredExtra(Enum<?> enumValue) {
        Map<String, Object> extra = getExtra(enumValue);
        EnumMetadata metadata = getEnumMetadata(enumValue.getDeclaringClass());
        if (!metadata.extraFields().isEmpty()) {
            extra = filterExtraFields(extra, metadata.extraFields());
        }
        if (!GLOBAL_EXTRA_FIELDS.isEmpty()) {
            extra = filterExtraFields(extra, GLOBAL_EXTRA_FIELDS);
        }
        return extra;
    }

    private static String objectToString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static boolean containsOptionCode(List<EnumOption> options, Object code) {
        return options.stream().anyMatch(option -> valueEquals(option.code(), code));
    }

    private static void putIfNotBlank(Map<String, Object> result, String key, String value) {
        if (!isBlank(value)) {
            result.putIfAbsent(key, value.trim());
        }
    }

    private static void addKnownExtraField(Map<String, Object> result, Enum<?> enumValue, String fieldName) {
        if (!result.containsKey(fieldName)) {
            Object value = getPropertyValue(enumValue, fieldName);
            if (value != null) {
                result.put(fieldName, value);
            }
        }
    }

    private static List<EnumOption> toOptionsUnchecked(Class<? extends Enum<?>> enumClass) {
        Enum<?>[] values = enumClass.getEnumConstants();
        if (values == null) {
            return Collections.emptyList();
        }
        return Arrays.stream(values)
                .sorted((left, right) -> enumComparatorRaw(left, right))
                .map(item -> toOption(item, false, false, null, null))
                .toList();
    }

    private static int enumComparatorRaw(Enum<?> left, Enum<?> right) {
        int sortCompare = compareNullable(getSort(left), getSort(right));
        if (sortCompare != 0) {
            return sortCompare;
        }
        int ordinalCompare = Integer.compare(left.ordinal(), right.ordinal());
        return ordinalCompare != 0 ? ordinalCompare : left.name().compareTo(right.name());
    }

    private static Map<String, Object> toDictUnchecked(Class<? extends Enum<?>> enumClass) {
        Map<String, Object> result = new LinkedHashMap<>();
        EnumMetadata metadata = getEnumMetadata(enumClass);
        result.put("key", metadata.key());
        result.put("title", metadata.title());
        result.put("module", metadata.module());
        result.put("description", metadata.description());
        result.put("expose", metadata.expose());
        result.put("sort", metadata.sort());
        result.put("group", metadata.group());
        result.put("deprecatedFlag", metadata.deprecatedFlag());
        result.put("className", metadata.className());
        result.put("simpleName", metadata.simpleName());
        result.put("options", toOptionsUnchecked(enumClass));
        return Collections.unmodifiableMap(result);
    }

    private static String toKebabKey(String simpleName) {
        String name = simpleName;
        if (name.endsWith("Enum")) {
            name = name.substring(0, name.length() - 4);
        }
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < name.length(); index++) {
            char ch = name.charAt(index);
            if (Character.isUpperCase(ch)) {
                if (index > 0) {
                    builder.append('-');
                }
                builder.append(Character.toLowerCase(ch));
            } else if (ch == '_' || ch == ' ') {
                builder.append('-');
            } else {
                builder.append(Character.toLowerCase(ch));
            }
        }
        return builder.toString().replaceAll("-+", "-").toLowerCase(Locale.ROOT);
    }

    private static Object invokeStaticNoArg(Class<?> clazz, String methodName) {
        try {
            Method method = clazz.getMethod(methodName);
            if (Modifier.isStatic(method.getModifiers()) && method.getParameterCount() == 0) {
                method.setAccessible(true);
                return method.invoke(null);
            }
        } catch (NoSuchMethodException ex) {
            return null;
        } catch (IllegalAccessException | InvocationTargetException ex) {
            throw new IllegalStateException("调用枚举静态方法失败: " + methodName, ex);
        }
        return null;
    }

    private static ClassLoader getClassLoader() {
        ClassLoader context = Thread.currentThread().getContextClassLoader();
        return context == null ? EnumUtil.class.getClassLoader() : context;
    }

    private static void scanFilePackage(String basePackage, URL url, boolean recursive, ClassLoader classLoader, Set<Class<? extends Enum<?>>> result) {
        try {
            URI uri = url.toURI();
            Path basePath = Path.of(uri);
            if (!Files.exists(basePath)) {
                return;
            }
            try (var stream = Files.walk(basePath, recursive ? Integer.MAX_VALUE : 1)) {
                stream.filter(path -> Files.isRegularFile(path) && path.toString().endsWith(".class"))
                        .forEach(path -> loadFileClass(basePackage, basePath, path, classLoader, result));
            }
        } catch (IOException | URISyntaxException ex) {
            throw new IllegalStateException("扫描文件枚举包失败: " + url, ex);
        }
    }

    private static void loadFileClass(String basePackage, Path basePath, Path classFile, ClassLoader classLoader, Set<Class<? extends Enum<?>>> result) {
        String relative = basePath.relativize(classFile).toString();
        if (File.separatorChar != '/') {
            relative = relative.replace(File.separatorChar, '.');
        } else {
            relative = relative.replace('/', '.');
        }
        String className = basePackage + "." + relative.substring(0, relative.length() - ".class".length());
        loadEnumClass(className, classLoader, result);
    }

    private static void scanJarPackage(String packagePath, URL url, boolean recursive, ClassLoader classLoader, Set<Class<? extends Enum<?>>> result) {
        try {
            JarURLConnection connection = (JarURLConnection) url.openConnection();
            try (JarFile jarFile = connection.getJarFile()) {
                scanJarEntries(packagePath, jarFile, recursive, classLoader, result);
            }
        } catch (ClassCastException ex) {
            String urlText = URLDecoder.decode(url.getFile(), StandardCharsets.UTF_8);
            int separatorIndex = urlText.indexOf("!/");
            if (separatorIndex > 0) {
                String jarPath = urlText.substring(0, separatorIndex);
                if (jarPath.startsWith("file:")) {
                    jarPath = jarPath.substring("file:".length());
                }
                try (JarFile jarFile = new JarFile(jarPath)) {
                    scanJarEntries(packagePath, jarFile, recursive, classLoader, result);
                } catch (IOException ioException) {
                    throw new IllegalStateException("扫描 JAR 枚举包失败: " + url, ioException);
                }
            }
        } catch (IOException ex) {
            throw new IllegalStateException("扫描 JAR 枚举包失败: " + url, ex);
        }
    }

    private static void scanJarEntries(String packagePath, JarFile jarFile, boolean recursive, ClassLoader classLoader, Set<Class<? extends Enum<?>>> result) {
        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String name = entry.getName();
            if (entry.isDirectory() || !name.startsWith(packagePath) || !name.endsWith(".class")) {
                continue;
            }
            String remainder = name.substring(packagePath.length());
            if (!recursive && remainder.indexOf('/', 1) > 0) {
                continue;
            }
            String className = name.substring(0, name.length() - ".class".length()).replace('/', '.');
            loadEnumClass(className, classLoader, result);
        }
    }

    @SuppressWarnings("unchecked")
    private static void loadEnumClass(String className, ClassLoader classLoader, Set<Class<? extends Enum<?>>> result) {
        if (className.contains("$") || isBlank(className)) {
            return;
        }
        try {
            Class<?> clazz = Class.forName(className, false, classLoader);
            if (clazz.isEnum()) {
                result.add((Class<? extends Enum<?>>) clazz.asSubclass(Enum.class));
            }
        } catch (ClassNotFoundException | LinkageError ex) {
            // 忽略无法加载的类，避免单个类影响整体包扫描。
        }
    }

    private record EnumCache(Map<String, Method> methods, Map<String, Field> fields) {
    }
}
