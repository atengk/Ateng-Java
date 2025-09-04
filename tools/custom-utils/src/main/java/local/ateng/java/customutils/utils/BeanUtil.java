package local.ateng.java.customutils.utils;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.*;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bean 工具类
 * 提供常用的 JavaBean 操作方法，如属性复制、对象转 Map 等
 *
 * @author Ateng
 * @since 2025-07-29
 */
public final class BeanUtil {

    /**
     * PropertyDescriptor 缓存，避免每次调用都使用 Introspector
     */
    private static final Map<Class<?>, Map<String, PropertyDescriptor>> CACHE = new ConcurrentHashMap<>();

    /**
     * 禁止实例化工具类
     */
    private BeanUtil() {
        throw new UnsupportedOperationException("工具类不可实例化");
    }

    /**
     * 将源对象的属性值复制到目标对象（默认复制所有属性）
     *
     * @param source 源对象
     * @param target 目标对象
     */
    public static void copy(Object source, Object target) {
        copyInternal(source, target, null, null);
    }

    /**
     * 将源对象的属性值复制到目标对象，忽略指定字段
     *
     * @param source           源对象
     * @param target           目标对象
     * @param ignoreProperties 需要忽略的属性名（可变参数）
     */
    public static void copy(Object source, Object target, String... ignoreProperties) {
        Set<String> ignoreSet = ignoreProperties != null ? new HashSet<>(Arrays.asList(ignoreProperties)) : null;
        copyInternal(source, target, null, ignoreSet);
    }

    /**
     * 将源对象的属性值复制到目标对象，支持字段映射和忽略字段
     *
     * @param source       源对象
     * @param target       目标对象
     * @param fieldMapping 源字段名 -> 目标字段名映射关系
     * @param ignoreFields 需要忽略的目标字段名（可变参数）
     */
    public static void copy(Object source, Object target,
                            Map<String, String> fieldMapping,
                            String... ignoreFields) {
        Set<String> ignoreSet = ignoreFields != null ? new HashSet<>(Arrays.asList(ignoreFields)) : null;
        copyInternal(source, target, fieldMapping, ignoreSet);
    }

    /**
     * 内部通用拷贝方法
     *
     * @param source       源对象
     * @param target       目标对象
     * @param fieldMapping 源字段 -> 目标字段映射
     * @param ignoreSet    需要忽略的目标字段集合
     */
    private static void copyInternal(Object source, Object target,
                                     Map<String, String> fieldMapping,
                                     Set<String> ignoreSet) {
        if (source == null || target == null) {
            return;
        }

        try {
            // 获取源对象和目标对象的 PropertyDescriptor
            Map<String, PropertyDescriptor> sourceMap = getPropertyDescriptors(source.getClass());
            Map<String, PropertyDescriptor> targetMap = getPropertyDescriptors(target.getClass());

            for (Map.Entry<String, PropertyDescriptor> entry : sourceMap.entrySet()) {
                String sourceName = entry.getKey();
                PropertyDescriptor sourcePd = entry.getValue();

                // 确定目标字段名
                String targetName = fieldMapping != null && fieldMapping.containsKey(sourceName)
                        ? fieldMapping.get(sourceName)
                        : sourceName;

                if (ignoreSet != null && ignoreSet.contains(targetName)) {
                    // 忽略指定字段
                    continue;
                }

                PropertyDescriptor targetPd = targetMap.get(targetName);
                if (targetPd != null && targetPd.getWriteMethod() != null && sourcePd.getReadMethod() != null) {
                    Method readMethod = sourcePd.getReadMethod();
                    Method writeMethod = targetPd.getWriteMethod();
                    Object value = readMethod.invoke(source);
                    writeMethod.invoke(target, value);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Bean 属性复制失败", e);
        }
    }

    /**
     * 内部通用 copyTo 方法
     *
     * @param source       源对象
     * @param targetClass  目标对象类型
     * @param fieldMapping 源字段 -> 目标字段映射
     * @param ignoreSet    需要忽略的目标字段集合
     * @param <T>          泛型
     * @return 目标对象实例
     */
    private static <T> T copyToInternal(Object source, Class<T> targetClass,
                                        Map<String, String> fieldMapping,
                                        Set<String> ignoreSet) {
        if (source == null || targetClass == null) {
            return null;
        }

        try {
            // 创建目标对象实例
            T target = targetClass.newInstance();

            // 调用之前封装的 copyInternal 完成属性拷贝
            copyInternal(source, target, fieldMapping, ignoreSet);

            return target;
        } catch (Exception e) {
            throw new RuntimeException("Bean 属性复制失败", e);
        }
    }

    /**
     * 内部通用 copyListTo 方法
     *
     * @param sourceList   源对象列表
     * @param targetType   目标对象类型
     * @param fieldMapping 源字段 -> 目标字段映射
     * @param ignoreSet    需要忽略的目标字段集合
     * @param <T>          泛型
     * @return 目标对象列表
     */
    private static <T> List<T> copyListToInternal(List<?> sourceList,
                                                  Class<T> targetType,
                                                  Map<String, String> fieldMapping,
                                                  Set<String> ignoreSet) {
        if (sourceList == null || sourceList.isEmpty() || targetType == null) {
            return Collections.emptyList();
        }

        List<T> targetList = new ArrayList<>(sourceList.size());
        for (Object source : sourceList) {
            T target = copyToInternal(source, targetType, fieldMapping, ignoreSet);
            targetList.add(target);
        }

        return targetList;
    }

    /**
     * 获取类的 PropertyDescriptor 并缓存
     *
     * @param clazz 类对象
     * @return 属性名 -> PropertyDescriptor 映射
     * @throws Exception 抛出异常
     */
    private static Map<String, PropertyDescriptor> getPropertyDescriptors(Class<?> clazz) throws Exception {
        if (CACHE.containsKey(clazz)) {
            return CACHE.get(clazz);
        }

        // 先拿所有属性描述符
        BeanInfo beanInfo = Introspector.getBeanInfo(clazz, Object.class);
        PropertyDescriptor[] pds = beanInfo.getPropertyDescriptors();
        Map<String, PropertyDescriptor> pdMap = new HashMap<>();
        for (PropertyDescriptor pd : pds) {
            pdMap.put(pd.getName(), pd);
        }

        // 按字段声明顺序重新排列
        Map<String, PropertyDescriptor> ordered = new LinkedHashMap<>();
        for (Field field : clazz.getDeclaredFields()) {
            int mod = field.getModifiers();
            if (Modifier.isStatic(mod) || Modifier.isTransient(mod) || field.isSynthetic()) {
                continue;
            }
            PropertyDescriptor pd = pdMap.get(field.getName());
            if (pd != null) {
                ordered.put(field.getName(), pd);
            }
        }

        // 补充没有对应字段的 getter/setter 属性（例如 isXxx 或 getClass）
        for (PropertyDescriptor pd : pds) {
            if (!ordered.containsKey(pd.getName())) {
                ordered.put(pd.getName(), pd);
            }
        }

        CACHE.put(clazz, ordered);
        return ordered;
    }

    /**
     * 将源对象属性复制到目标类型的新对象
     *
     * @param source      源对象
     * @param targetClass 目标对象类型
     * @param <T>         泛型
     * @return 目标对象实例
     */
    public static <T> T copyTo(Object source, Class<T> targetClass) {
        return copyToInternal(source, targetClass, null, null);
    }

    /**
     * 将源对象属性复制到目标类型的新对象，忽略指定属性
     *
     * @param source           源对象
     * @param targetClass      目标对象类型
     * @param ignoreProperties 需要忽略的属性名（可变参数）
     * @param <T>              泛型
     * @return 目标对象实例
     */
    public static <T> T copyTo(Object source, Class<T> targetClass, String... ignoreProperties) {
        Set<String> ignoreSet = ignoreProperties != null ? new HashSet<>(Arrays.asList(ignoreProperties)) : null;
        return copyToInternal(source, targetClass, null, ignoreSet);
    }

    /**
     * 将源对象属性复制到目标类型的新对象，支持字段映射和忽略字段
     *
     * @param source       源对象
     * @param targetClass  目标对象类型
     * @param fieldMapping 源字段名 -> 目标字段名映射
     * @param ignoreFields 需要忽略的目标字段名（可变参数）
     * @param <T>          泛型
     * @return 目标对象实例
     */
    public static <T> T copyTo(Object source, Class<T> targetClass,
                               Map<String, String> fieldMapping,
                               String... ignoreFields) {
        Set<String> ignoreSet = ignoreFields != null ? new HashSet<>(Arrays.asList(ignoreFields)) : null;
        return copyToInternal(source, targetClass, fieldMapping, ignoreSet);
    }

    /**
     * 将源 List 拷贝为目标类型 List
     *
     * @param sourceList 源对象列表
     * @param targetType 目标对象类型
     * @param <T>        泛型
     * @return 目标对象列表
     */
    public static <T> List<T> copyListTo(List<?> sourceList, Class<T> targetType) {
        return copyListToInternal(sourceList, targetType, null, null);
    }

    /**
     * 将源 List 拷贝为目标类型 List，忽略指定属性
     *
     * @param sourceList       源对象列表
     * @param targetType       目标对象类型
     * @param ignoreProperties 需要忽略的属性名（可变参数）
     * @param <T>              泛型
     * @return 目标对象列表
     */
    public static <T> List<T> copyListTo(List<?> sourceList, Class<T> targetType, String... ignoreProperties) {
        Set<String> ignoreSet = ignoreProperties != null ? new HashSet<>(Arrays.asList(ignoreProperties)) : null;
        return copyListToInternal(sourceList, targetType, null, ignoreSet);
    }

    /**
     * 将源 List 拷贝为目标类型 List，支持字段映射和忽略字段
     *
     * @param sourceList   源对象列表
     * @param targetType   目标对象类型
     * @param fieldMapping 源字段名 -> 目标字段名映射
     * @param ignoreFields 需要忽略的目标字段名（可变参数）
     * @param <T>          泛型
     * @return 目标对象列表
     */
    public static <T> List<T> copyListTo(List<?> sourceList, Class<T> targetType,
                                         Map<String, String> fieldMapping,
                                         String... ignoreFields) {
        Set<String> ignoreSet = ignoreFields != null ? new HashSet<>(Arrays.asList(ignoreFields)) : null;
        return copyListToInternal(sourceList, targetType, fieldMapping, ignoreSet);
    }

    /**
     * 通用深拷贝对象（序列化方式）
     * <p>
     * 注意：对象及内部对象必须实现 Serializable。
     *
     * @param obj 待拷贝对象
     * @param <T> 对象类型
     * @return 深拷贝的新对象，如果 obj 为 null 返回 null
     * @throws RuntimeException 当拷贝失败时抛出
     */
    @SuppressWarnings("unchecked")
    public static <T extends Serializable> T deepCopy(T obj) {
        if (obj == null) {
            return null;
        }
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(baos)) {

            oos.writeObject(obj);
            oos.flush();

            try (ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
                 ObjectInputStream ois = new ObjectInputStream(bais)) {
                return (T) ois.readObject();
            }

        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException("对象深拷贝失败", e);
        }
    }

    /**
     * 深拷贝对象并返回指定目标类型（序列化方式）
     *
     * @param obj         待拷贝对象
     * @param targetClass 目标类型
     * @param <T>         目标类型
     * @return 深拷贝后的对象，如果 obj 为 null 返回 null
     * @throws RuntimeException 当拷贝失败或类型转换异常时抛出
     */
    public static <T extends Serializable> T deepCopy(T obj, Class<T> targetClass) {
        if (obj == null) {
            return null;
        }
        try {
            T copyObj = deepCopy(obj);

            T target = targetClass.newInstance();

            copy(copyObj, target);

            return target;
        } catch (Exception e) {
            throw new RuntimeException("深拷贝并转换类型失败", e);
        }
    }

    /**
     * 将 Bean 转为 Map（属性名 -> 属性值）
     *
     * @param bean JavaBean 对象
     * @return 属性名 -> 属性值映射，bean 为 null 返回空 Map
     */
    public static Map<String, Object> toMap(Object bean) {
        return toMap(bean, null, (String[]) null);
    }

    /**
     * 将 Bean 转为 Map，忽略指定属性
     *
     * @param bean             JavaBean 对象
     * @param ignoreProperties 要忽略的属性名，可变参数
     * @return 属性名 -> 属性值映射
     */
    public static Map<String, Object> toMap(Object bean, String... ignoreProperties) {
        return toMap(bean, null, ignoreProperties);
    }

    /**
     * 将 Bean 转为 Map，支持嵌套对象递归转换
     *
     * @param bean         JavaBean 对象
     * @param fieldMapping 属性名映射（原属性名 -> Map key），可为 null
     * @param ignoreFields 要忽略的属性名，可变参数
     * @return Map<String, Object>，不会返回 null
     */
    public static Map<String, Object> toMap(Object bean, Map<String, String> fieldMapping, String... ignoreFields) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (bean == null) {
            return map;
        }

        Set<String> ignoreSet = ignoreFields != null ? new HashSet<>(Arrays.asList(ignoreFields)) : null;

        Map<String, PropertyDescriptor> pdMap;
        try {
            pdMap = getPropertyDescriptors(bean.getClass());
        } catch (Exception e) {
            return map;
        }
        for (Map.Entry<String, PropertyDescriptor> entry : pdMap.entrySet()) {
            String name = entry.getKey();
            PropertyDescriptor pd = entry.getValue();

            if ((ignoreSet != null && ignoreSet.contains(name)) || pd.getReadMethod() == null) {
                continue;
            }

            try {
                Object value = pd.getReadMethod().invoke(bean);
                String mapKey = (fieldMapping != null && fieldMapping.containsKey(name)) ? fieldMapping.get(name) : name;
                map.put(mapKey, convertValue(value, fieldMapping, ignoreFields));
            } catch (Exception e) {
                map.put(name, null);
            }
        }

        return map;
    }

    /**
     * 转换属性值，支持嵌套 Bean、集合、数组、Map
     */
    @SuppressWarnings("unchecked")
    private static Object convertValue(Object value, Map<String, String> fieldMapping, String... ignoreFields) {
        if (value == null) {
            return null;
        }

        Class<?> clazz = value.getClass();

        // 基本类型、包装类、字符串、枚举、时间类，直接返回
        if (isSimpleValueType(value)) {
            return value;
        }

        // 数组
        if (clazz.isArray()) {
            int length = Array.getLength(value);
            List<Object> list = new ArrayList<>(length);
            for (int i = 0; i < length; i++) {
                Object element = Array.get(value, i);
                list.add(convertValue(element, fieldMapping, ignoreFields));
            }
            return list;
        }

        // 集合
        if (value instanceof Collection) {
            Collection<?> collection = (Collection<?>) value;
            List<Object> list = new ArrayList<>(collection.size());
            for (Object element : collection) {
                list.add(convertValue(element, fieldMapping, ignoreFields));
            }
            return list;
        }

        // Map
        if (value instanceof Map) {
            Map<Object, Object> original = (Map<Object, Object>) value;
            Map<Object, Object> converted = new LinkedHashMap<>();
            for (Map.Entry<Object, Object> entry : original.entrySet()) {
                converted.put(entry.getKey(), convertValue(entry.getValue(), fieldMapping, ignoreFields));
            }
            return converted;
        }

        // 其他情况：认为是 JavaBean
        return toMap(value, fieldMapping, ignoreFields);
    }

    /**
     * 判断是否为简单值类型
     */
    private static boolean isSimpleValueType(Object value) {
        if (value == null) {
            return true;
        }
        Class<?> clazz = value.getClass();
        return clazz.isPrimitive()
                || value instanceof CharSequence
                || value instanceof Number
                || value instanceof Boolean
                || value instanceof Character
                || value instanceof Enum
                || value instanceof java.util.Date
                || value instanceof java.time.temporal.TemporalAccessor
                || value instanceof Class
                || value instanceof java.util.UUID;
    }

    /**
     * 将 Java Bean 转换为 Map，并对指定字段进行脱敏（支持嵌套 Bean/集合）
     *
     * @param bean              JavaBean 对象
     * @param desensitizeFields 需要脱敏字段集合
     * @param maskChar          脱敏字符
     * @return Map<String,Object>，嵌套 Bean/集合会被递归转换
     */
    public static Map<String, Object> toDesensitizedMap(Object bean, Collection<String> desensitizeFields, String maskChar) {
        if (bean == null) {
            return Collections.emptyMap();
        }
        return (Map<String, Object>) convert(bean, new HashSet<>(desensitizeFields), maskChar, false);
    }

    /**
     * 递归转换对象
     *
     * @param value             待转换对象
     * @param desensitizeFields 根对象层级需要脱敏的字段集合
     * @param maskChar          脱敏字符
     * @param forceDesensitize  是否强制整条对象脱敏
     * @return 转换后的对象（Map/List/值）
     */
    @SuppressWarnings("unchecked")
    private static Object convert(Object value, Set<String> desensitizeFields, String maskChar, boolean forceDesensitize) {
        if (value == null) {
            return null;
        }

        if (isSimpleValueType(value)) {
            return forceDesensitize ? maskChar : value;
        }

        // 数组
        if (value.getClass().isArray()) {
            int len = Array.getLength(value);
            List<Object> list = new ArrayList<>(len);
            for (int i = 0; i < len; i++) {
                // 嵌套层 forceDesensitize 继承父级
                list.add(convert(Array.get(value, i), Collections.emptySet(), maskChar, forceDesensitize));
            }
            return list;
        }

        // 集合
        if (value instanceof Collection) {
            Collection<?> col = (Collection<?>) value;
            List<Object> list = new ArrayList<>(col.size());
            for (Object e : col) {
                list.add(convert(e, Collections.emptySet(), maskChar, forceDesensitize));
            }
            return list;
        }

        // Map
        if (value instanceof Map) {
            Map<Object, Object> map = new LinkedHashMap<>();
            ((Map<Object, Object>) value).forEach((k, v) -> map.put(k, convert(v, Collections.emptySet(), maskChar, forceDesensitize)));
            return map;
        }

        // Bean
        Map<String, Object> map = new LinkedHashMap<>();
        try {
            Map<String, PropertyDescriptor> pdMap = BeanUtil.getPropertyDescriptors(value.getClass());
            for (Map.Entry<String, PropertyDescriptor> entry : pdMap.entrySet()) {
                String name = entry.getKey();
                PropertyDescriptor pd = entry.getValue();
                if (pd.getReadMethod() == null) continue;
                Object fieldValue = pd.getReadMethod().invoke(value);

                if (forceDesensitize) {
                    // 父字段命中脱敏 → 子字段全部脱敏
                    map.put(name, convert(fieldValue, Collections.emptySet(), maskChar, true));
                } else if (desensitizeFields.contains(name)) {
                    // 当前字段命中脱敏
                    if (isSimpleValueType(fieldValue)) {
                        map.put(name, maskChar);
                    } else {
                        // 复杂对象或集合 → 全部脱敏
                        map.put(name, convert(fieldValue, Collections.emptySet(), maskChar, true));
                    }
                } else {
                    // 当前字段未命中脱敏 → 递归转换嵌套对象，但不脱敏
                    map.put(name, convert(fieldValue, Collections.emptySet(), maskChar, false));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Bean 转 Map 脱敏失败", e);
        }
        return map;
    }

    /**
     * 将 JavaBean 对象转换为 Map，并对指定字段进行值映射
     * <p>
     * 示例：
     * <pre>
     *   Map<String, Map<Object, Object>> valueMapping = new HashMap<>();
     *   Map<Object, Object> statusMap = new HashMap<>();
     *   statusMap.put(1, "未开始");
     *   statusMap.put(2, "进行中");
     *   statusMap.put(3, "已完成");
     *   valueMapping.put("status", statusMap);
     *
     *   Map<String, Object> result = BeanUtil.toMapWithValueMapping(bean, valueMapping, "ignoreField1", "ignoreField2");
     * </pre>
     *
     * @param bean         JavaBean 对象
     * @param valueMapping 字段值映射表（key 为字段名，value 为该字段的值映射关系）
     * @param ignoreFields 要忽略的属性名称
     * @return 转换后的 Map
     */
    public static Map<String, Object> toMapWithValueMapping(Object bean,
                                                            Map<String, Map<Object, Object>> valueMapping,
                                                            String... ignoreFields) {
        Map<String, Object> result = new HashMap<>();
        if (bean == null) {
            return result;
        }

        // 转换 ignoreFields 为 Set，提高查询效率
        Set<String> ignoreSet = new HashSet<>();
        if (ignoreFields != null && ignoreFields.length > 0) {
            ignoreSet.addAll(Arrays.asList(ignoreFields));
        }

        try {
            // 使用缓存的 PropertyDescriptor
            Map<String, PropertyDescriptor> descriptorMap = getPropertyDescriptors(bean.getClass());

            for (Map.Entry<String, PropertyDescriptor> entry : descriptorMap.entrySet()) {
                String fieldName = entry.getKey();
                PropertyDescriptor pd = entry.getValue();

                // 跳过忽略字段
                if (ignoreSet.contains(fieldName)) {
                    continue;
                }

                // 调用 getter 获取值
                Object value = pd.getReadMethod().invoke(bean);

                // 如果有映射表，则转换值
                if (valueMapping != null && valueMapping.containsKey(fieldName)) {
                    Map<Object, Object> mapping = valueMapping.get(fieldName);
                    if (mapping != null && mapping.containsKey(value)) {
                        value = mapping.get(value);
                    }
                }

                result.put(fieldName, value);
            }
        } catch (Exception e) {
            throw new RuntimeException("Bean 转换为 Map 失败: " + bean.getClass().getName(), e);
        }

        return result;
    }

    /**
     * 将 JavaBean 对象转换为 Map，并对指定字段进行枚举映射（code → name）
     * <p>
     * ignoreProperties 用于忽略不转换的字段
     * enumMapping 用于将字段值对应枚举实例映射为描述字符串
     * 枚举要求有两个字段：int/Integer code 和 String name
     *
     * @param bean         JavaBean 对象
     * @param ignoreFields 要忽略的属性名称
     * @param enumMapping  字段枚举映射表（字段名 → 枚举 Class）
     * @return 转换后的 Map
     */
    public static Map<String, Object> toMapWithEnum(Object bean,
                                                    Map<String, Class<? extends Enum<?>>> enumMapping,
                                                    String... ignoreFields) {
        Map<String, Object> map = new HashMap<>();
        if (bean == null) {
            return map;
        }

        Set<String> ignoreSet = new HashSet<>();
        if (ignoreFields != null) {
            ignoreSet.addAll(Arrays.asList(ignoreFields));
        }

        try {
            java.beans.BeanInfo beanInfo = java.beans.Introspector.getBeanInfo(bean.getClass(), Object.class);
            for (java.beans.PropertyDescriptor pd : beanInfo.getPropertyDescriptors()) {
                String name = pd.getName();
                if (ignoreSet.contains(name)) {
                    continue;
                }

                Object value = pd.getReadMethod().invoke(bean);

                // 枚举映射处理 (code → name)
                if (enumMapping != null && enumMapping.containsKey(name) && value != null) {
                    Class<? extends Enum<?>> enumClass = enumMapping.get(name);
                    for (Enum<?> e : enumClass.getEnumConstants()) {
                        Integer code = (Integer) e.getClass().getMethod("getCode").invoke(e);
                        String label = (String) e.getClass().getMethod("getName").invoke(e);
                        if (code.equals(value)) {
                            value = label;
                            break;
                        }
                    }
                }

                map.put(name, value);
            }
        } catch (Exception e) {
            throw new RuntimeException("Bean 转换为 Map（枚举 code→name）失败", e);
        }

        return map;
    }

    /**
     * 将 JavaBean 对象列表转换为 Map 列表
     *
     * @param sourceList JavaBean 对象列表
     * @return List<Map < String, Object>>，每个 Map 对应一个对象
     */
    public static List<Map<String, Object>> toMapList(List<?> sourceList) {
        if (sourceList == null || sourceList.isEmpty()) {
            return Collections.emptyList();
        }

        List<Map<String, Object>> mapList = new ArrayList<>(sourceList.size());
        for (Object bean : sourceList) {
            mapList.add(toMap(bean));
        }

        return mapList;
    }

    /**
     * 将 JavaBean 列表转换为 Map 列表，并对指定字段进行值映射
     *
     * @param beanList     JavaBean 列表
     * @param valueMapping 字段值映射表（字段名 → 值映射表）
     * @param ignoreFields 要忽略的属性名称
     * @return Map 列表
     */
    public static List<Map<String, Object>> toMapListWithValueMapping(List<?> beanList,
                                                                      Map<String, Map<Object, Object>> valueMapping,
                                                                      String... ignoreFields) {
        if (beanList == null || beanList.isEmpty()) {
            return Collections.emptyList();
        }

        List<Map<String, Object>> result = new ArrayList<>(beanList.size());
        for (Object bean : beanList) {
            Map<String, Object> map = toMapWithValueMapping(bean, valueMapping, ignoreFields);
            result.add(map);
        }
        return result;
    }

    /**
     * 将 JavaBean 列表转换为 Map 列表，并对指定字段进行枚举映射（code → name）
     * <p>
     * 枚举类要求必须有两个方法：
     * <ul>
     *   <li>{@code Integer getCode()}</li>
     *   <li>{@code String getName()}</li>
     * </ul>
     *
     * @param beanList     JavaBean 列表
     * @param enumMapping  字段枚举映射表（字段名 → 枚举 Class）
     * @param ignoreFields 要忽略的属性名称
     * @return Map 列表
     */
    public static List<Map<String, Object>> toMapListWithEnum(List<?> beanList,
                                                              Map<String, Class<? extends Enum<?>>> enumMapping,
                                                              String... ignoreFields) {
        if (beanList == null || beanList.isEmpty()) {
            return Collections.emptyList();
        }

        List<Map<String, Object>> result = new ArrayList<>(beanList.size());
        for (Object bean : beanList) {
            Map<String, Object> map = toMapWithEnum(bean, enumMapping, ignoreFields);
            result.add(map);
        }
        return result;
    }

    /**
     * 将 Map 转换为指定类型的 JavaBean
     *
     * @param map  Map 对象，key 为属性名，value 为属性值
     * @param type 目标对象类型
     * @param <T>  泛型
     * @return 转换后的 JavaBean 对象
     */
    public static <T> T toBean(Map<String, Object> map, Class<T> type) {
        if (map == null || map.isEmpty() || type == null) {
            return null;
        }

        try {
            T bean = type.newInstance();
            Map<String, PropertyDescriptor> targetMap = getPropertyDescriptors(type);

            for (Map.Entry<String, Object> entry : map.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                PropertyDescriptor pd = targetMap.get(key);
                if (pd != null && pd.getWriteMethod() != null) {
                    // 简单类型赋值，可扩展类型转换逻辑
                    pd.getWriteMethod().invoke(bean, value);
                }
            }

            return bean;
        } catch (Exception e) {
            throw new RuntimeException("Map 转 Bean 失败", e);
        }
    }

    /**
     * 将 Map 列表转换为指定类型的 JavaBean 列表
     *
     * @param mapList    Map 列表
     * @param targetType 目标对象类型
     * @param <T>        泛型
     * @return JavaBean 列表
     */
    public static <T> List<T> toBeanList(List<Map<String, Object>> mapList, Class<T> targetType) {
        if (mapList == null || mapList.isEmpty() || targetType == null) {
            return Collections.emptyList();
        }

        List<T> resultList = new ArrayList<>(mapList.size());
        for (Map<String, Object> map : mapList) {
            resultList.add(toBean(map, targetType));
        }

        return resultList;
    }

    /**
     * 判断指定类是否为 JavaBean
     *
     * @param clazz 待判断类
     * @return true 表示是 Bean，false 表示不是 Bean
     */
    public static boolean isBean(Class<?> clazz) {
        if (clazz == null) {
            return false;
        }

        // 排除基本类型、包装类型、String、数组、集合、Map、枚举
        if (clazz.isPrimitive()
                || Number.class.isAssignableFrom(clazz)
                || Boolean.class.isAssignableFrom(clazz)
                || Character.class.isAssignableFrom(clazz)
                || CharSequence.class.isAssignableFrom(clazz)
                || clazz.isArray()
                || Collection.class.isAssignableFrom(clazz)
                || Map.class.isAssignableFrom(clazz)
                || clazz.isEnum()) {
            return false;
        }

        try {
            // 至少有一个可读写属性则认为是 Bean
            PropertyDescriptor[] pds = getPropertyDescriptors(clazz).values().toArray(new PropertyDescriptor[0]);
            for (PropertyDescriptor pd : pds) {
                if (pd.getReadMethod() != null && pd.getWriteMethod() != null) {
                    return true;
                }
            }
        } catch (Exception e) {
            return false;
        }

        return false;
    }

    /**
     * 判断指定对象是否为 JavaBean
     *
     * @param bean 待判断对象
     * @return true 表示是 Bean，false 表示不是 Bean
     */
    public static boolean isBean(Object bean) {
        if (bean == null) {
            return false;
        }
        return isBean(bean.getClass());
    }

    /**
     * 判断 JavaBean 的所有属性是否全部为 null
     * <p>
     * 仅检查具有 getter 方法的属性，不包括静态字段或父类 Object 的方法。
     *
     * @param bean JavaBean 实例
     * @return 所有属性为 null 返回 true，否则返回 false
     */
    public static boolean isAllPropertyNull(Object bean) {
        if (bean == null) {
            return true;
        }

        try {
            Map<String, PropertyDescriptor> propertyMap = getPropertyDescriptors(bean.getClass());
            for (PropertyDescriptor pd : propertyMap.values()) {
                if (pd.getReadMethod() != null) {
                    Object value = pd.getReadMethod().invoke(bean);
                    if (value != null) {
                        // 有一个属性不为 null，则返回 false
                        return false;
                    }
                }
            }
            // 全部属性为 null
            return true;
        } catch (Exception e) {
            throw new RuntimeException("判断 Bean 属性是否全部为 null 失败", e);
        }
    }

    /**
     * 判断对象是否具有指定属性（具备 getter 方法）
     *
     * @param bean         Bean 实例
     * @param propertyName 属性名称
     * @return 存在该属性且具备 getter 方法返回 true，否则返回 false
     */
    public static boolean hasProperty(Object bean, String propertyName) {
        if (bean == null || propertyName == null || propertyName.isEmpty()) {
            return false;
        }

        try {
            Map<String, PropertyDescriptor> pdMap = getPropertyDescriptors(bean.getClass());
            PropertyDescriptor pd = pdMap.get(propertyName);
            return pd != null && pd.getReadMethod() != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 根据属性名获取 JavaBean 中对应的值（支持泛型）
     * <p>
     * 优先通过 getter 方法获取，如果不存在则尝试直接反射字段。
     *
     * @param bean      JavaBean 对象
     * @param fieldName 属性名
     * @param <T>       返回值类型
     * @return 属性值，如果对象或属性不存在返回 null
     */
    @SuppressWarnings("unchecked")
    public static <T> T getProperty(Object bean, String fieldName) {
        if (bean == null || fieldName == null || fieldName.trim().isEmpty()) {
            return null;
        }

        try {
            Map<String, PropertyDescriptor> pdMap = getPropertyDescriptors(bean.getClass());
            PropertyDescriptor pd = pdMap.get(fieldName);
            if (pd != null && pd.getReadMethod() != null) {
                Method readMethod = pd.getReadMethod();
                readMethod.setAccessible(true);
                return (T) readMethod.invoke(bean);
            }
        } catch (Exception ignored) {
        }

        // 如果 getter 获取失败，则尝试直接反射字段
        try {
            Field field = bean.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return (T) field.get(bean);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 根据属性名设置 JavaBean 中对应的值（支持泛型）
     * <p>
     * 优先通过 setter 方法设置，如果不存在则尝试直接反射字段。
     *
     * @param bean      JavaBean 对象
     * @param fieldName 属性名
     * @param value     要设置的值
     * @param <T>       属性值类型
     * @return true 表示设置成功，false 表示失败
     */
    public static <T> boolean setProperty(Object bean, String fieldName, T value) {
        if (bean == null || fieldName == null || fieldName.trim().isEmpty()) {
            return false;
        }

        try {
            Map<String, PropertyDescriptor> pdMap = getPropertyDescriptors(bean.getClass());
            PropertyDescriptor pd = pdMap.get(fieldName);
            if (pd != null && pd.getWriteMethod() != null) {
                Method writeMethod = pd.getWriteMethod();
                writeMethod.setAccessible(true);
                writeMethod.invoke(bean, value);
                return true;
            }
        } catch (Exception ignored) {
        }

        // 如果 setter 设置失败，则尝试直接反射字段
        try {
            Field field = bean.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(bean, value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取 JavaBean、Map 或集合/数组的嵌套属性值（支持多级路径和下标访问）。
     * <p>
     * 支持的路径示例：
     * <ul>
     *     <li>{@code "customer.name"}  —— 获取嵌套 Bean 属性</li>
     *     <li>{@code "order.items[0].name"} —— 获取集合中指定下标元素的属性</li>
     *     <li>{@code "products[2].price"} —— 获取数组中指定下标元素的属性</li>
     *     <li>{@code "attributes['key']"} —— 如果是 Map，也可以直接用属性名（不支持 key 表达式）</li>
     * </ul>
     *
     * @param bean 起始对象（可以是 JavaBean、Map、List、数组）
     * @param path 属性路径（使用"."分隔，支持集合/数组下标，如 "items[0].name"）
     * @param <T>  返回值类型（会自动转换）
     * @return 属性值，如果路径中任意一步为 null，则返回 null
     * @throws RuntimeException 当路径解析或反射调用失败时抛出
     */
    @SuppressWarnings("unchecked")
    public static <T> T getNestedProperty(Object bean, String path) {
        if (bean == null || path == null) {
            return null;
        }
        try {
            String[] fields = path.split("\\.");
            Object current = bean;
            for (String field : fields) {
                if (current == null) {
                    return null;
                }
                // 处理数组或集合下标
                int indexStart = field.indexOf('[');
                if (indexStart > -1) {
                    String propName = field.substring(0, indexStart);
                    int index = Integer.parseInt(field.substring(indexStart + 1, field.indexOf(']')));
                    current = getProperty(current, propName);
                    if (current instanceof java.util.List) {
                        current = ((java.util.List<?>) current).get(index);
                    } else if (current != null && current.getClass().isArray()) {
                        current = java.lang.reflect.Array.get(current, index);
                    } else {
                        return null;
                    }
                } else {
                    current = getProperty(current, field);
                }
            }
            return (T) current;
        } catch (Exception e) {
            throw new RuntimeException("获取嵌套属性失败: " + path, e);
        }
    }

    /**
     * 设置 JavaBean、Map 或集合/数组的嵌套属性值（支持多级路径和下标访问）。
     * <p>
     * 支持的路径示例：
     * <ul>
     *     <li>{@code "customer.name"}  —— 设置嵌套 Bean 属性</li>
     *     <li>{@code "order.items[0].name"} —— 设置集合中指定下标元素的属性</li>
     *     <li>{@code "products[2].price"} —— 设置数组中指定下标元素的属性</li>
     * </ul>
     * 注意：中间路径的对象必须已存在，否则无法设置（不会自动创建）。
     *
     * @param bean  起始对象（可以是 JavaBean、Map、List、数组）
     * @param path  属性路径（使用"."分隔，支持集合/数组下标，如 "items[0].name"）
     * @param value 要设置的值
     * @param <T>   值类型
     * @return 设置成功返回 true；如果路径无效或中途遇到 null，则返回 false
     * @throws RuntimeException 当路径解析或反射调用失败时抛出
     */
    public static <T> boolean setNestedProperty(Object bean, String path, T value) {
        if (bean == null || path == null || path.trim().isEmpty()) {
            return false;
        }

        try {
            String[] fields = path.split("\\.");
            Object current = bean;

            // 遍历路径中的中间节点
            for (int i = 0; i < fields.length - 1; i++) {
                current = getIndexedProperty(current, fields[i]);
                if (current == null) {
                    // 中间对象为 null，无法继续
                    return false;
                }
            }

            // 处理最后一个属性
            String lastField = fields[fields.length - 1];
            return setIndexedProperty(current, lastField, value);

        } catch (Exception e) {
            throw new RuntimeException("设置嵌套属性失败: " + path, e);
        }
    }

    /**
     * 获取支持下标访问的属性（Bean、Map、List、数组）
     */
    private static Object getIndexedProperty(Object obj, String field) {
        int indexStart = field.indexOf('[');
        if (indexStart > -1) {
            String propName = field.substring(0, indexStart);
            int index = Integer.parseInt(field.substring(indexStart + 1, field.indexOf(']')));
            Object value = getProperty(obj, propName);
            return getFromCollectionOrArray(value, index);
        } else {
            return getProperty(obj, field);
        }
    }

    /**
     * 设置支持下标访问的属性（Bean、Map、List、数组）
     */
    @SuppressWarnings("unchecked")
    private static <T> boolean setIndexedProperty(Object obj, String field, T value) {
        int indexStart = field.indexOf('[');
        if (indexStart > -1) {
            String propName = field.substring(0, indexStart);
            int index = Integer.parseInt(field.substring(indexStart + 1, field.indexOf(']')));
            Object collectionOrArray = getProperty(obj, propName);
            if (collectionOrArray instanceof List) {
                ((List<Object>) collectionOrArray).set(index, value);
                return true;
            } else if (collectionOrArray != null && collectionOrArray.getClass().isArray()) {
                Array.set(collectionOrArray, index, value);
                return true;
            }
            return false;
        } else {
            return setProperty(obj, field, value);
        }
    }

    /**
     * 从集合或数组中获取指定下标的元素
     */
    private static Object getFromCollectionOrArray(Object obj, int index) {
        if (obj instanceof List) {
            return ((List<?>) obj).get(index);
        } else if (obj != null && obj.getClass().isArray()) {
            return Array.get(obj, index);
        }
        return null;
    }

    /**
     * 获取指定类（包含父类）的所有字段名
     * <p>
     * 默认会递归向上查找父类字段，直到 Object 为止。
     * 子类中如果有与父类同名的字段，将覆盖父类的同名字段。
     *
     * @param clazz 要获取字段的类
     * @return 字段名列表（List<String>），不会返回 null
     */
    public static List<String> getAllFieldNames(Class<?> clazz) {
        List<String> fieldNames = new ArrayList<>();
        // 去重，子类优先
        Set<String> nameSet = new HashSet<>();

        while (clazz != null && clazz != Object.class) {
            Field[] fields = clazz.getDeclaredFields();
            for (Field field : fields) {
                // 如果未添加过，则加入
                if (nameSet.add(field.getName())) {
                    fieldNames.add(field.getName());
                }
            }
            clazz = clazz.getSuperclass();
        }
        return fieldNames;
    }

    /**
     * 比较两个 JavaBean 对象的属性差异
     * <p>
     * 逐个比较两个对象的同名属性值（通过 getter 方法获取），
     * 并返回所有不相等的属性及其旧值和新值。
     * <p>
     * 返回的 Map 中：
     * <ul>
     *     <li>key 为属性名</li>
     *     <li>value 为长度为 2 的数组，其中 [0] 是旧值，[1] 是新值</li>
     * </ul>
     *
     * @param oldBean 原对象
     * @param newBean 新对象
     * @return 包含差异属性的 Map，如果两个对象属性完全相同则返回空 Map
     * @throws RuntimeException 如果反射操作失败或属性访问异常
     */
    public static Map<String, Object[]> diff(Object oldBean, Object newBean) {
        Map<String, Object[]> changes = new HashMap<>();
        if (oldBean == null || newBean == null || !oldBean.getClass().equals(newBean.getClass())) {
            throw new IllegalArgumentException("两个对象必须非空且类型相同");
        }

        try {
            Map<String, PropertyDescriptor> pdMap = getPropertyDescriptors(oldBean.getClass());
            for (PropertyDescriptor pd : pdMap.values()) {
                if (pd.getReadMethod() != null) {
                    Object oldValue = pd.getReadMethod().invoke(oldBean);
                    Object newValue = pd.getReadMethod().invoke(newBean);
                    if (!Objects.equals(oldValue, newValue)) {
                        changes.put(pd.getName(), new Object[]{oldValue, newValue});
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Bean 对比失败", e);
        }
        return changes;
    }


}
