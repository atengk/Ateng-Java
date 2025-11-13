package local.ateng.java.customutils.utils;

import local.ateng.java.customutils.enums.BaseEnum;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.SimpleMetadataReaderFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;


/**
 * 通用枚举工具类（字段名不固定）
 * 通过反射按指定字段提取枚举值，适配任意结构枚举类
 * <p>
 * 使用场景：字段名不统一，例如 code/value/id，name/label/text 等
 *
 * @author Ateng
 * @since 2025-07-29
 */
public final class EnumUtil {

    private EnumUtil() {
        throw new UnsupportedOperationException("工具类不可实例化");
    }

    /**
     * 根据枚举类中指定字段的值获取对应的枚举实例。
     * <p>
     * 使用反射获取枚举类中的字段，并逐一对比枚举常量的字段值是否等于目标值，
     * 如果找到匹配项则返回该枚举实例，否则返回 {@code null}。
     * <p>
     * ⚠ 注意事项：
     * <ul>
     *   <li>此方法基于反射，性能上比直接调用枚举方法略低，不适合高频调用场景。</li>
     *   <li>如果字段不存在或类型不匹配，不会抛出异常，而是返回 {@code null}。</li>
     *   <li>常见字段示例：{@code code}、{@code value}、{@code key}、{@code name} 等。</li>
     * </ul>
     *
     * @param enumClass   枚举类的 {@link Class} 对象，例如 {@code StatusEnum.class}
     * @param fieldName   要匹配的字段名，例如 {@code "code"} 或 {@code "name"}
     * @param targetValue 目标值，例如 {@code 0} 或 {@code "在线"}
     * @param <E>         枚举类型，需继承 {@link Enum}
     * @return 匹配的枚举实例；如果未找到或发生异常，返回 {@code null}
     */
    public static <E extends Enum<E>> E getByFieldValue(Class<E> enumClass, String fieldName, Object targetValue) {
        if (enumClass == null || fieldName == null || targetValue == null) {
            return null;
        }
        try {
            Field field = enumClass.getDeclaredField(fieldName);
            field.setAccessible(true);
            for (E e : enumClass.getEnumConstants()) {
                Object value = field.get(e);
                if (Objects.equals(value, targetValue)) {
                    return e;
                }
            }
        } catch (Exception e) {
            // 可根据需要打印异常信息
        }
        return null;
    }

    /**
     * 枚举是否包含指定字段值
     */
    public static <E extends Enum<E>> boolean containsFieldValue(Class<E> enumClass, String fieldName, Object value) {
        return getByFieldValue(enumClass, fieldName, value) != null;
    }

    /**
     * 根据某个字段值获取同一枚举中的另一个字段值。
     *
     * @param enumClass   枚举类，例如 {@code StatusEnum.class}
     * @param matchField  用于匹配的字段名（如 "code"）
     * @param matchValue  匹配字段对应的值
     * @param targetField 需要返回的字段名（如 "name"）
     * @param <E>         枚举类型
     * @param <T>         返回值类型
     * @return 匹配到的目标字段值，未找到时返回 null
     */
    @SuppressWarnings("unchecked")
    public static <E extends Enum<E>, T> T getFieldValueByOtherField(
            Class<E> enumClass,
            String matchField,
            Object matchValue,
            String targetField) {

        E e = getByFieldValue(enumClass, matchField, matchValue);
        if (e == null) {
            return null;
        }
        try {
            Field field = enumClass.getDeclaredField(targetField);
            field.setAccessible(true);
            return (T) field.get(e);
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * 根据枚举中的 code 字段值获取对应的 name 字段值。
     *
     * <p>要求枚举类中必须包含 {@code code} 和 {@code name} 字段，例如：</p>
     * <pre>
     * {@code
     * @AllArgsConstructor
     * @Getter
     * public enum StatusEnum {
     *     OFFLINE(0, "离线"),
     *     ONLINE(1, "在线");
     *
     *     private final int code;
     *     private final String name;
     * }
     * }
     * </pre>
     *
     * @param enumClass 枚举类类型，例如 {@code StatusEnum.class}
     * @param code      枚举中的 code 值
     * @param <E>       枚举类型
     * @return 匹配到的 name 值，未找到时返回 {@code null}
     */
    public static <E extends Enum<E>> String getNameByCode(Class<E> enumClass, Object code) {
        return getFieldValueByOtherField(enumClass, "code", code, "name");
    }

    /**
     * 根据枚举中的 name 字段值获取对应的 code 字段值。
     *
     * <p>要求枚举类中必须包含 {@code code} 和 {@code name} 字段，例如：</p>
     * <pre>
     * {@code
     * @AllArgsConstructor
     * @Getter
     * public enum StatusEnum {
     *     OFFLINE(0, "离线"),
     *     ONLINE(1, "在线");
     *
     *     private final int code;
     *     private final String name;
     * }
     * }
     * </pre>
     *
     * @param enumClass 枚举类类型，例如 {@code StatusEnum.class}
     * @param name      枚举中的 name 值
     * @param <E>       枚举类型
     * @param <T>       返回值类型（取决于枚举中 code 字段的类型，如 Integer、String 等）
     * @return 匹配到的 code 值，未找到时返回 {@code null}
     */
    public static <E extends Enum<E>, T> T getCodeByName(Class<E> enumClass, String name) {
        return getFieldValueByOtherField(enumClass, "name", name, "code");
    }

    /**
     * 枚举字段映射（如：code -> name，或者 id -> label）
     *
     * @param enumClass  枚举类
     * @param keyField   key 字段名
     * @param valueField value 字段名
     * @param <E>        枚举类型
     * @return 映射表（keyField 对应值 -> valueField 对应值）
     */
    public static <E extends Enum<E>> Map<Object, Object> mapFieldToField(Class<E> enumClass, String keyField, String valueField) {
        Map<Object, Object> map = new HashMap<>();
        if (enumClass == null || keyField == null || valueField == null) {
            return map;
        }
        try {
            Field kField = enumClass.getDeclaredField(keyField);
            Field vField = enumClass.getDeclaredField(valueField);
            kField.setAccessible(true);
            vField.setAccessible(true);
            for (E e : enumClass.getEnumConstants()) {
                Object key = kField.get(e);
                Object value = vField.get(e);
                map.put(key, value);
            }
        } catch (Exception e) {
            // 可根据需要打印异常信息
        }
        return map;
    }

    /**
     * 获取所有枚举实例指定字段的值
     *
     * @param enumClass 枚举类
     * @param fieldName 字段名（如 value、code、label）
     * @param <E>       枚举类型
     * @return 字段值列表
     */
    public static <E extends Enum<E>> java.util.List<Object> getFieldValueList(Class<E> enumClass, String fieldName) {
        java.util.List<Object> list = new java.util.ArrayList<>();
        if (enumClass == null || fieldName == null) {
            return list;
        }
        try {
            Field field = enumClass.getDeclaredField(fieldName);
            field.setAccessible(true);
            for (E e : enumClass.getEnumConstants()) {
                list.add(field.get(e));
            }
        } catch (Exception ignored) {
        }
        return list;
    }

    /**
     * 枚举名（name） -> 任意字段值映射
     *
     * @param enumClass 枚举类
     * @param fieldName 字段名（如 label、text、desc）
     * @param <E>       枚举类型
     * @return 映射表（枚举名 -> 字段值）
     */
    public static <E extends Enum<E>> Map<String, Object> nameToFieldValueMap(Class<E> enumClass, String fieldName) {
        Map<String, Object> map = new HashMap<>();
        if (enumClass == null || fieldName == null) {
            return map;
        }
        try {
            Field field = enumClass.getDeclaredField(fieldName);
            field.setAccessible(true);
            for (E e : enumClass.getEnumConstants()) {
                map.put(e.name(), field.get(e));
            }
        } catch (Exception ignored) {
        }
        return map;
    }

    /**
     * 枚举转换为列表，每个元素包含指定字段
     *
     * @param enumClass 枚举类
     * @param fields    字段名数组（如 {"value", "label"}）
     * @param <E>       枚举类型
     * @return List<Map> 格式数据，每个 Map 表示一个枚举项的字段值
     */
    public static <E extends Enum<E>> java.util.List<Map<String, Object>> toListMap(Class<E> enumClass, String... fields) {
        java.util.List<Map<String, Object>> list = new java.util.ArrayList<>();
        if (enumClass == null || fields == null || fields.length == 0) {
            return list;
        }
        try {
            Field[] reflectFields = new Field[fields.length];
            for (int i = 0; i < fields.length; i++) {
                reflectFields[i] = enumClass.getDeclaredField(fields[i]);
                reflectFields[i].setAccessible(true);
            }

            for (E e : enumClass.getEnumConstants()) {
                Map<String, Object> map = new HashMap<>();
                for (int i = 0; i < fields.length; i++) {
                    map.put(fields[i], reflectFields[i].get(e));
                }
                list.add(map);
            }
        } catch (Exception ignored) {
        }
        return list;
    }

    /**
     * 枚举字段值 ➝ 枚举名映射（如 code -> "ENABLED"）
     *
     * @param enumClass 枚举类
     * @param fieldName 字段名
     * @param <E>       枚举类型
     * @return 映射表（字段值 -> 枚举名）
     */
    public static <E extends Enum<E>> Map<Object, String> fieldValueToEnumNameMap(Class<E> enumClass, String fieldName) {
        Map<Object, String> map = new HashMap<>();
        if (enumClass == null || fieldName == null) {
            return map;
        }
        try {
            Field field = enumClass.getDeclaredField(fieldName);
            field.setAccessible(true);
            for (E e : enumClass.getEnumConstants()) {
                Object key = field.get(e);
                map.put(key, e.name());
            }
        } catch (Exception ignored) {
        }
        return map;
    }

    /**
     * 枚举字段值 ➝ 枚举实例映射（如 value -> Status.ENABLED）
     *
     * @param enumClass 枚举类
     * @param fieldName 字段名（如 code、value）
     * @param <E>       枚举类型
     * @return 映射表（字段值 -> 枚举实例）
     */
    public static <E extends Enum<E>> Map<Object, E> fieldValueToEnumMap(Class<E> enumClass, String fieldName) {
        Map<Object, E> map = new HashMap<>();
        if (enumClass == null || fieldName == null) {
            return map;
        }
        try {
            Field field = enumClass.getDeclaredField(fieldName);
            field.setAccessible(true);
            for (E e : enumClass.getEnumConstants()) {
                Object key = field.get(e);
                map.put(key, e);
            }
        } catch (Exception ignored) {
        }
        return map;
    }

    /**
     * 枚举名 ➝ 任意字段值映射（如 name -> label）
     *
     * @param enumClass 枚举类
     * @param fieldName 字段名
     * @param <E>       枚举类型
     * @return 映射表（枚举名 -> 字段值）
     */
    public static <E extends Enum<E>> Map<String, Object> enumNameToFieldValueMap(Class<E> enumClass, String fieldName) {
        Map<String, Object> map = new HashMap<>();
        if (enumClass == null || fieldName == null) {
            return map;
        }
        try {
            Field field = enumClass.getDeclaredField(fieldName);
            field.setAccessible(true);
            for (E e : enumClass.getEnumConstants()) {
                Object value = field.get(e);
                map.put(e.name(), value);
            }
        } catch (Exception ignored) {
        }
        return map;
    }

    /**
     * 判断指定字段值是否在枚举中唯一（无重复值）
     *
     * @param enumClass 枚举类
     * @param fieldName 字段名
     * @param <E>       枚举类型
     * @return true 表示字段值唯一；false 表示存在重复值或异常
     */
    public static <E extends Enum<E>> boolean isFieldValueUnique(Class<E> enumClass, String fieldName) {
        java.util.Set<Object> seen = new java.util.HashSet<>();
        if (enumClass == null || fieldName == null) {
            return false;
        }
        try {
            Field field = enumClass.getDeclaredField(fieldName);
            field.setAccessible(true);
            for (E e : enumClass.getEnumConstants()) {
                Object val = field.get(e);
                if (!seen.add(val)) {
                    // 出现重复
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 将枚举类转换为 Map，指定枚举中的两个字段作为 key 和 value
     *
     * @param keyField   作为 key 的字段名（对应 getter 方法）
     * @param valueField 作为 value 的字段名（对应 getter 方法）
     * @param enumClass  枚举类
     * @param <E>        枚举类型
     * @param <K>        Map 的 key 类型
     * @param <V>        Map 的 value 类型
     * @return 枚举转 Map
     */
    public static <E extends Enum<E>, K, V> Map<K, V> toMap(String keyField, String valueField, Class<E> enumClass) {
        try {
            // 通过反射获取 getter 方法
            Method keyMethod = enumClass.getMethod("get" + capitalize(keyField));
            Method valueMethod = enumClass.getMethod("get" + capitalize(valueField));

            return Arrays.stream(enumClass.getEnumConstants())
                    .collect(Collectors.toMap(
                            e -> invokeMethod(e, keyMethod),
                            e -> invokeMethod(e, valueMethod)
                    ));
        } catch (Exception e) {
            throw new IllegalArgumentException("枚举转 Map 失败，请检查字段是否存在对应 getter 方法", e);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T invokeMethod(Object target, Method method) {
        try {
            return (T) method.invoke(target);
        } catch (Exception e) {
            throw new RuntimeException("反射调用方法失败: " + method.getName(), e);
        }
    }

    private static String capitalize(String fieldName) {
        if (fieldName == null || fieldName.isEmpty()) {
            return fieldName;
        }
        return Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
    }

    /**
     * 将指定枚举类转换为前端常用的 List<Map<String, Object>> 格式。
     * <p>
     * 每个 Map 包含两个固定键：
     * <ul>
     *     <li>value: 对应枚举的指定字段值（如 code、id、value）</li>
     *     <li>label: 对应枚举的指定字段值（如 name、label、desc）</li>
     * </ul>
     * <p>
     * 典型应用场景：
     * <ul>
     *     <li>前端下拉框、单选/多选组件数据源</li>
     *     <li>数据字典展示</li>
     * </ul>
     * <p>
     * 注意：
     * <ul>
     *     <li>枚举字段必须有对应的 public getter 方法，如 getCode()、getLabel() 等</li>
     *     <li>字段名参数需传入属性名（非方法名），方法内部会自动拼接 "get" 前缀反射调用</li>
     *     <li>若枚举类或字段不存在，会抛出 IllegalArgumentException</li>
     * </ul>
     *
     * @param valueField 枚举中作为 value 的字段名（对应 getter 方法）
     * @param labelField 枚举中作为 label 的字段名（对应 getter 方法）
     * @param enumClass  需要转换的枚举类
     * @param <E>        枚举类型
     * @return 返回 List<Map<String, Object>>，每个 Map 包含键 "value" 和 "label"
     * @throws IllegalArgumentException 如果枚举类或字段不存在，或 getter 方法不可访问
     */
    public static <E extends Enum<E>> List<Map<String, Object>> toLabelValueList(String valueField, String labelField, Class<E> enumClass) {
        Map<Object, Object> map = toMap(valueField, labelField, enumClass);
        List<Map<String, Object>> list = new ArrayList<>();
        String valueKey = "value";
        String labelKey = "label";
        for (Map.Entry<Object, Object> entry : map.entrySet()) {
            Map<String, Object> item = new HashMap<>();

            item.put(valueKey, entry.getKey());
            item.put(labelKey, entry.getValue());

            list.add(item);
        }
        return list;
    }

    /**
     * 将多个枚举类批量转换为前端常用的 Map<String, List<Map<String, Object>>> 格式。
     * <p>
     * 每个枚举类会生成一组数据，存放在 Map 中，key 为枚举类的简单类名，
     * value 为对应的枚举值列表，每个元素是一个 Map，包含：
     * <ul>
     *     <li>value: 对应枚举的指定字段值（如 code、id、value）</li>
     *     <li>label: 对应枚举的指定字段值（如 name、label、desc）</li>
     * </ul>
     * <p>
     * 典型应用场景：
     * <ul>
     *     <li>前端批量加载字典数据</li>
     *     <li>枚举映射表展示</li>
     * </ul>
     * <p>
     * 注意：
     * <ul>
     *     <li>枚举字段必须有对应的 public getter 方法，如 getCode()、getLabel() 等</li>
     *     <li>字段名参数需传入属性名（非方法名），方法内部会自动拼接 "get" 前缀反射调用</li>
     *     <li>若枚举类或字段不存在，会抛出 IllegalArgumentException</li>
     * </ul>
     *
     * @param valueField  枚举中作为 value 的字段名（对应 getter 方法）
     * @param labelField  枚举中作为 label 的字段名（对应 getter 方法）
     * @param enumClasses 需要转换的枚举类数组
     * @return Map，key 为枚举类名value 为对应的 List<Map<String, Object>>
     * @throws IllegalArgumentException 如果枚举类或字段不存在，或 getter 方法不可访问
     */
    @SafeVarargs
    public static Map<String, List<Map<String, Object>>> toMultiLabelValueMap(
            String valueField, String labelField, Class<? extends Enum<?>>... enumClasses) {
        Map<String, List<Map<String, Object>>> result = new LinkedHashMap<>();

        for (Class<? extends Enum<?>> enumClassRaw : enumClasses) {
            // 调用辅助方法，避免泛型捕获问题
            List<Map<String, Object>> list = toLabelValueListInternal(valueField, labelField, enumClassRaw);

            String key = enumClassRaw.getSimpleName();
            result.put(key, list);
        }

        return result;
    }

    /**
     * 将枚举类转换为 List<Map<String, Object>>（内部方法，用于解决泛型捕获问题）。
     * <p>
     * 该方法主要作为 {@link #toMultiLabelValueMap(String, String, Class[])} 的内部实现，
     * 对传入的原始枚举类进行安全类型转换，再调用通用的 {@code toLabelValueList} 方法。
     *
     * @param valueField   枚举中作为 value 的字段名（对应 getter 方法）
     * @param labelField   枚举中作为 label 的字段名（对应 getter 方法）
     * @param enumClassRaw 原始枚举类（Class<?> 类型）
     * @param <E>          枚举泛型类型
     * @return 转换后的枚举列表，每个元素为包含 value 和 label 的 Map
     */
    @SuppressWarnings("unchecked")
    private static <E extends Enum<E>> List<Map<String, Object>> toLabelValueListInternal(
            String valueField,
            String labelField,
            Class<?> enumClassRaw) {

        return toLabelValueList(valueField, labelField, (Class<E>) enumClassRaw);
    }



    /**
     * 扫描指定包下所有实现 {@link BaseEnum} 接口的枚举类。
     *
     * @param basePackage 包名，例如 "com.example.enums"
     * @return 扫描到的枚举类集合
     */
    @SuppressWarnings("unchecked")
    public static Set<Class<? extends BaseEnum<?, ?>>> scanAllBaseEnums(String basePackage) {
        if (org.springframework.util.ObjectUtils.isEmpty(basePackage)) {
            throw new IllegalArgumentException("包名不能为空");
        }

        Set<Class<? extends BaseEnum<?, ?>>> result = new HashSet<>();
        char dotChar = '.';
        char slashChar = '/';
        String classPattern = "/**/*.class";
        String classpathAllPrefix = "classpath*:";

        try {
            String pattern = basePackage.replace(dotChar, slashChar) + classPattern;
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            SimpleMetadataReaderFactory factory = new SimpleMetadataReaderFactory();

            for (Resource resource : resolver.getResources(classpathAllPrefix + pattern)) {
                if (resource.isReadable()) {
                    MetadataReader reader = factory.getMetadataReader(resource);
                    String className = reader.getClassMetadata().getClassName();
                    Class<?> clazz = org.springframework.util.ClassUtils.forName(
                            className,
                            Thread.currentThread().getContextClassLoader()
                    );
                    if (BaseEnum.class.isAssignableFrom(clazz) && clazz.isEnum()) {
                        result.add((Class<? extends BaseEnum<?, ?>>) clazz);
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("扫描枚举失败，包路径：" + basePackage, e);
        }
        return result;
    }

    /**
     * 扫描多个包下所有实现 {@link BaseEnum} 接口的枚举类。
     * <p>内部复用 {@link #scanAllBaseEnums(String)} 方法。</p>
     *
     * @param basePackages 多个包名，例如 "com.example.enums", "io.github.atengk.enums"
     * @return 扫描到的枚举类集合
     */
    public static Set<Class<? extends BaseEnum<?, ?>>> scanAllBaseEnums(String... basePackages) {
        Set<Class<? extends BaseEnum<?, ?>>> result = new HashSet<>();
        if (basePackages == null || basePackages.length == 0) {
            return result;
        }
        for (String basePackage : basePackages) {
            result.addAll(scanAllBaseEnums(basePackage));
        }
        return result;
    }

    /**
     * 扫描指定包下所有实现 {@link BaseEnum} 接口的枚举类。
     * <p>内部复用 {@link #scanAllBaseEnums(String)} 方法</p>
     *
     * @return 扫描到的枚举类集合
     */
    public static Set<Class<? extends BaseEnum<?, ?>>> scanAllBaseEnums() {
        // 这里复用单包扫描方法，传入空字符串表示扫描所有包
        return scanAllBaseEnums("local.ateng.java", "io.github.atengk", "com.brx");
    }

    /**
     * 扫描指定包下所有实现 BaseEnum 接口的枚举类，并转换成前端 label/value Map
     *
     * @param basePackage 扫描的包名，例如 "com.example.enums"
     * @return Map，key 为枚举类名，value 为对应枚举列表
     */
    public static Map<String, List<Map<String, Object>>> getAllBaseEnumMap(String basePackage) {
        // 1. 扫描所有 BaseEnum 枚举类
        Set<Class<? extends BaseEnum<?, ?>>> enumClasses = scanAllBaseEnums(basePackage);

        // 2. 转换成前端需要的 Map<String, List<Map<String,Object>>>
        String codeKey = "code";
        String nameKey = "name";
        return toMultiLabelValueMap(codeKey, nameKey, enumClasses.toArray(new Class[0]));
    }

    /**
     * 扫描默认包（Spring Boot 启动类所在包）下的 BaseEnum 枚举类，并转换为前端 Map
     *
     * @return Map，key 为枚举类名，value 为对应 label/value 列表
     */
    public static Map<String, List<Map<String, Object>>> getAllBaseEnumMap() {
        // 获取 Spring Boot 启动类所在包
        String basePackage = SpringUtil.getMainApplicationPackage();
        return getAllBaseEnumMap(basePackage);
    }

    /**
     * 扫描默认包（Spring Boot 启动类所在包）下的 BaseEnum 枚举类，并转换为前端 Map
     *
     * @param mainClazz 启动类 Class 对象
     * @return Map，key 为枚举类名，value 为对应 label/value 列表
     */
    public static Map<String, List<Map<String, Object>>> getAllBaseEnumMap(Class<?> mainClazz) {
        // 获取 Spring Boot 启动类所在包
        String basePackage = SpringUtil.getMainApplicationPackage(mainClazz);
        return getAllBaseEnumMap(basePackage);
    }

    /**
     * 根据枚举类名获取对应的前端 label/value 列表
     *
     * <p>内部复用 {@link #getAllBaseEnumMap()}，不需要额外扫描包路径。
     *
     * <p>示例：
     * <pre>
     *     List<Map<String, Object>> list = EnumUtil.getLabelValueListByEnumName("StatusEnum");
     * </pre>
     *
     * <p>返回值格式：
     * <pre>
     *     [
     *       {"value": 0, "label": "离线"},
     *       {"value": 1, "label": "在线"}
     *     ]
     * </pre>
     *
     * @param enumSimpleName 枚举类简单名，例如 "StatusEnum"
     * @return 前端 label/value 列表，如果未找到对应枚举类，返回空列表
     */
    public static List<Map<String, Object>> getLabelValueListByEnumName(String enumSimpleName) {
        if (enumSimpleName == null || enumSimpleName.trim().isEmpty()) {
            return Collections.emptyList();
        }

        // 获取所有 BaseEnum 枚举映射
        Map<String, List<Map<String, Object>>> allEnums = getAllBaseEnumMap();

        return allEnums.getOrDefault(enumSimpleName, Collections.emptyList());
    }

    /**
     * 根据枚举类名获取对应的前端 label/value 列表
     *
     * <p>内部复用 {@link #getAllBaseEnumMap()}，不需要额外扫描包路径。
     *
     * <p>示例：
     * <pre>
     *     List<Map<String, Object>> list = EnumUtil.getLabelValueListByEnumName("StatusEnum");
     * </pre>
     *
     * <p>返回值格式：
     * <pre>
     *     [
     *       {"value": 0, "label": "离线"},
     *       {"value": 1, "label": "在线"}
     *     ]
     * </pre>
     *
     * @param enumSimpleName 枚举类简单名，例如 "StatusEnum"
     * @param mainClazz      启动类 Class 对象
     * @return 前端 label/value 列表，如果未找到对应枚举类，返回空列表
     */
    public static List<Map<String, Object>> getLabelValueListByEnumName(String enumSimpleName, Class<?> mainClazz) {
        if (enumSimpleName == null || enumSimpleName.trim().isEmpty() || mainClazz == null) {
            return Collections.emptyList();
        }

        // 获取所有 BaseEnum 枚举映射
        Map<String, List<Map<String, Object>>> allEnums = getAllBaseEnumMap(mainClazz);

        return allEnums.getOrDefault(enumSimpleName, Collections.emptyList());
    }

    /**
     * 根据枚举类名获取对应的前端 label/value 列表
     *
     * <p>内部复用 {@link #getAllBaseEnumMap()}，不需要额外扫描包路径。
     *
     * <p>示例：
     * <pre>
     *     List<Map<String, Object>> list = EnumUtil.getLabelValueListByEnumName("StatusEnum");
     * </pre>
     *
     * <p>返回值格式：
     * <pre>
     *     [
     *       {"value": 0, "label": "离线"},
     *       {"value": 1, "label": "在线"}
     *     ]
     * </pre>
     *
     * @param enumSimpleName 枚举类简单名，例如 "StatusEnum"
     * @param basePackage    扫描的包名，例如 "com.example.enums"
     * @return 前端 label/value 列表，如果未找到对应枚举类，返回空列表
     */
    public static List<Map<String, Object>> getLabelValueListByEnumName(String enumSimpleName, String basePackage) {
        if (enumSimpleName == null || enumSimpleName.trim().isEmpty() || basePackage == null || basePackage.trim().isEmpty()) {
            return Collections.emptyList();
        }

        // 获取所有 BaseEnum 枚举映射
        Map<String, List<Map<String, Object>>> allEnums = getAllBaseEnumMap(basePackage);

        return allEnums.getOrDefault(enumSimpleName, Collections.emptyList());
    }


}
