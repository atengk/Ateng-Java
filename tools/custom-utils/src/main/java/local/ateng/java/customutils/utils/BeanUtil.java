package local.ateng.java.customutils.utils;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.util.*;

/**
 * Bean 工具类
 * 提供常用的 JavaBean 操作方法，如属性复制、对象转 Map 等
 *
 * @author Ateng
 * @since 2025-07-29
 */
public final class BeanUtil {

    /**
     * 禁止实例化工具类
     */
    private BeanUtil() {
        throw new UnsupportedOperationException("工具类不可实例化");
    }

    /**
     * 将源 JavaBean 对象的属性值复制到目标 JavaBean 对象中
     *
     * @param source 源对象
     * @param target 目标对象
     */
    public static void copyProperties(Object source, Object target) {
        if (source == null || target == null) {
            return;
        }

        try {
            java.beans.BeanInfo sourceInfo = java.beans.Introspector.getBeanInfo(source.getClass(), Object.class);
            java.beans.BeanInfo targetInfo = java.beans.Introspector.getBeanInfo(target.getClass(), Object.class);

            java.util.Map<String, java.beans.PropertyDescriptor> targetPropertyMap = new java.util.HashMap<>();
            for (java.beans.PropertyDescriptor pd : targetInfo.getPropertyDescriptors()) {
                targetPropertyMap.put(pd.getName(), pd);
            }

            for (java.beans.PropertyDescriptor sourcePd : sourceInfo.getPropertyDescriptors()) {
                java.beans.PropertyDescriptor targetPd = targetPropertyMap.get(sourcePd.getName());
                if (targetPd != null && targetPd.getWriteMethod() != null && sourcePd.getReadMethod() != null) {
                    Object value = sourcePd.getReadMethod().invoke(source);
                    targetPd.getWriteMethod().invoke(target, value);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("复制 Bean 属性失败", e);
        }
    }

    /**
     * 将源 JavaBean 对象的属性值复制到目标 JavaBean 对象中，可忽略指定的属性
     *
     * @param source           源对象
     * @param target           目标对象
     * @param ignoreProperties 需要忽略拷贝的属性名，可变参数
     */
    public static void copyProperties(Object source, Object target, String... ignoreProperties) {
        if (source == null || target == null) {
            return;
        }

        // 将需要忽略的属性名存入 Set 中，方便后续判断
        java.util.Set<String> ignoreSet = new java.util.HashSet<>();
        if (ignoreProperties != null) {
            java.util.Collections.addAll(ignoreSet, ignoreProperties);
        }

        try {
            java.beans.BeanInfo sourceInfo = java.beans.Introspector.getBeanInfo(source.getClass(), Object.class);
            java.beans.BeanInfo targetInfo = java.beans.Introspector.getBeanInfo(target.getClass(), Object.class);

            java.util.Map<String, java.beans.PropertyDescriptor> targetPropertyMap = new java.util.HashMap<>();
            for (java.beans.PropertyDescriptor pd : targetInfo.getPropertyDescriptors()) {
                targetPropertyMap.put(pd.getName(), pd);
            }

            for (java.beans.PropertyDescriptor sourcePd : sourceInfo.getPropertyDescriptors()) {
                // 如果当前属性在忽略列表中，则跳过
                if (ignoreSet.contains(sourcePd.getName())) {
                    continue;
                }
                java.beans.PropertyDescriptor targetPd = targetPropertyMap.get(sourcePd.getName());
                if (targetPd != null && targetPd.getWriteMethod() != null && sourcePd.getReadMethod() != null) {
                    Object value = sourcePd.getReadMethod().invoke(source);
                    targetPd.getWriteMethod().invoke(target, value);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("复制 Bean 属性失败（忽略字段）", e);
        }
    }

    /**
     * 将源 JavaBean 对象的属性值复制到新创建的目标 JavaBean 对象中，并返回目标对象
     *
     * @param source      源对象
     * @param targetClass 目标对象类型，必须有无参构造方法
     * @param <T>         目标对象类型泛型
     * @return 复制属性后的目标对象实例，如果源对象为 null 则返回 null
     */
    public static <T> T copyPropertiesToNew(Object source, Class<T> targetClass) {
        if (source == null || targetClass == null) {
            return null;
        }

        try {
            // 创建目标对象实例
            T target = targetClass.getDeclaredConstructor().newInstance();
            // 直接调用已有的拷贝方法
            copyProperties(source, target);
            return target;
        } catch (Exception e) {
            throw new RuntimeException("复制 Bean 属性到新对象失败", e);
        }
    }

    /**
     * 将源 JavaBean 对象的属性值复制到新创建的目标 JavaBean 对象中（可忽略指定属性），并返回目标对象
     *
     * @param source           源对象
     * @param targetClass      目标对象类型，必须有无参构造方法
     * @param ignoreProperties 需要忽略拷贝的属性名，可变参数
     * @param <T>              目标对象类型泛型
     * @return 复制属性后的目标对象实例，如果源对象为 null 则返回 null
     */
    public static <T> T copyPropertiesToNew(Object source, Class<T> targetClass, String... ignoreProperties) {
        if (source == null || targetClass == null) {
            return null;
        }

        try {
            // 创建目标对象实例
            T target = targetClass.getDeclaredConstructor().newInstance();
            // 调用支持忽略属性的拷贝方法
            copyProperties(source, target, ignoreProperties);
            return target;
        } catch (Exception e) {
            throw new RuntimeException("复制 Bean 属性到新对象失败（忽略字段）", e);
        }
    }

    /**
     * 将源 JavaBean 对象的属性值复制到目标 JavaBean 对象中，支持字段名映射
     *
     * @param source       源对象
     * @param target       目标对象
     * @param fieldMapping 字段映射关系，key 为源对象的属性名，value 为目标对象的属性名
     * @param ignoreFields 需要忽略拷贝的字段名（源对象字段名）
     */
    public static void copyProperties(Object source, Object target,
                                      java.util.Map<String, String> fieldMapping,
                                      String... ignoreFields) {
        if (source == null || target == null) {
            return;
        }

        // 忽略字段集合
        java.util.Set<String> ignoreSet = new java.util.HashSet<>();
        if (ignoreFields != null) {
            java.util.Collections.addAll(ignoreSet, ignoreFields);
        }

        try {
            java.beans.BeanInfo sourceInfo = java.beans.Introspector.getBeanInfo(source.getClass(), Object.class);
            java.beans.BeanInfo targetInfo = java.beans.Introspector.getBeanInfo(target.getClass(), Object.class);

            // 目标对象属性 map
            java.util.Map<String, java.beans.PropertyDescriptor> targetPropertyMap = new java.util.HashMap<>();
            for (java.beans.PropertyDescriptor pd : targetInfo.getPropertyDescriptors()) {
                targetPropertyMap.put(pd.getName(), pd);
            }

            for (java.beans.PropertyDescriptor sourcePd : sourceInfo.getPropertyDescriptors()) {
                String sourceName = sourcePd.getName();

                // 跳过忽略字段
                if (ignoreSet.contains(sourceName)) {
                    continue;
                }

                // 获取目标字段名：优先映射字段，否则同名
                String targetName = fieldMapping != null && fieldMapping.containsKey(sourceName)
                        ? fieldMapping.get(sourceName)
                        : sourceName;

                java.beans.PropertyDescriptor targetPd = targetPropertyMap.get(targetName);
                if (targetPd != null && targetPd.getWriteMethod() != null && sourcePd.getReadMethod() != null) {
                    Object value = sourcePd.getReadMethod().invoke(source);
                    targetPd.getWriteMethod().invoke(target, value);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("复制 Bean 属性失败（字段映射）", e);
        }
    }

    /**
     * 将源对象属性复制到新创建的目标对象中，支持字段映射
     *
     * @param source       源对象
     * @param targetClass  目标对象类型
     * @param fieldMapping 字段映射关系，key 为源对象属性名，value 为目标对象属性名
     * @param ignoreFields 需要忽略拷贝的字段名（源对象字段名）
     * @param <T>          目标对象类型
     * @return 复制属性后的新对象
     */
    public static <T> T copyPropertiesToNew(Object source, Class<T> targetClass,
                                            java.util.Map<String, String> fieldMapping,
                                            String... ignoreFields) {
        if (source == null || targetClass == null) {
            return null;
        }
        try {
            T target = targetClass.getDeclaredConstructor().newInstance();
            copyProperties(source, target, fieldMapping, ignoreFields);
            return target;
        } catch (Exception e) {
            throw new RuntimeException("复制 Bean 属性到新对象失败（字段映射）", e);
        }
    }

    /**
     * 将 JavaBean 列表复制为另一种类型的 JavaBean 列表（普通同名属性复制）
     *
     * @param sourceList 源对象列表
     * @param targetType 目标类型的 Class
     * @param <T>        目标类型
     * @return 转换后的目标类型列表
     */
    public static <T> List<T> copyList(List<?> sourceList, Class<T> targetType) {
        if (sourceList == null || sourceList.isEmpty()) {
            return Collections.emptyList();
        }

        List<T> result = new ArrayList<>();
        for (Object source : sourceList) {
            try {
                T target = targetType.getDeclaredConstructor().newInstance();
                copyProperties(source, target);
                result.add(target);
            } catch (Exception e) {
                throw new RuntimeException("列表元素复制失败", e);
            }
        }
        return result;
    }

    /**
     * 将 JavaBean 列表复制为另一种类型的 JavaBean 列表，支持忽略指定字段
     *
     * @param sourceList       源对象列表
     * @param targetType       目标类型的 Class
     * @param ignoreProperties 需要忽略拷贝的字段名，可变参数
     * @param <T>              目标类型
     * @return 转换后的目标类型列表
     */
    public static <T> List<T> copyList(List<?> sourceList, Class<T> targetType, String... ignoreProperties) {
        if (sourceList == null || sourceList.isEmpty()) {
            return Collections.emptyList();
        }

        List<T> result = new ArrayList<>();
        for (Object source : sourceList) {
            try {
                T target = targetType.getDeclaredConstructor().newInstance();
                copyProperties(source, target, ignoreProperties);
                result.add(target);
            } catch (Exception e) {
                throw new RuntimeException("列表元素复制失败（忽略字段）", e);
            }
        }
        return result;
    }

    /**
     * 将 JavaBean 列表复制为另一种类型的 JavaBean 列表，支持字段映射和忽略字段
     *
     * @param sourceList   源对象列表
     * @param targetType   目标类型的 Class
     * @param fieldMapping 字段映射关系，key 为源对象属性名，value 为目标对象属性名
     * @param ignoreFields 需要忽略拷贝的字段名（源对象字段名）
     * @param <T>          目标类型
     * @return 转换后的目标类型列表
     */
    public static <T> List<T> copyList(List<?> sourceList, Class<T> targetType,
                                       java.util.Map<String, String> fieldMapping,
                                       String... ignoreFields) {
        if (sourceList == null || sourceList.isEmpty()) {
            return Collections.emptyList();
        }

        List<T> result = new ArrayList<>();
        for (Object source : sourceList) {
            try {
                T target = targetType.getDeclaredConstructor().newInstance();
                copyProperties(source, target, fieldMapping, ignoreFields);
                result.add(target);
            } catch (Exception e) {
                throw new RuntimeException("列表元素复制失败（字段映射）", e);
            }
        }
        return result;
    }

    /**
     * 将 JavaBean 对象转换为 Map
     *
     * @param bean JavaBean 对象
     * @return 属性名-属性值 Map
     */
    public static java.util.Map<String, Object> beanToMap(Object bean) {
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        if (bean == null) {
            return map;
        }

        try {
            java.beans.BeanInfo beanInfo = java.beans.Introspector.getBeanInfo(bean.getClass(), Object.class);
            for (java.beans.PropertyDescriptor pd : beanInfo.getPropertyDescriptors()) {
                Object value = pd.getReadMethod().invoke(bean);
                map.put(pd.getName(), value);
            }
        } catch (Exception e) {
            throw new RuntimeException("Bean 转换为 Map 失败", e);
        }

        return map;
    }

    /**
     * 将 JavaBean 列表转换为 Map 列表
     *
     * @param sourceList JavaBean 列表
     * @return Map 列表
     */
    public static List<Map<String, Object>> toMapList(List<?> sourceList) {
        if (sourceList == null || sourceList.isEmpty()) {
            return Collections.emptyList();
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Object source : sourceList) {
            Map<String, Object> map = new HashMap<>();
            copyProperties(source, map);
            result.add(map);
        }
        return result;
    }

    /**
     * 将 Map 转换为 JavaBean 对象
     *
     * @param map  属性名-属性值 Map
     * @param type JavaBean 类型
     * @param <T>  泛型类型
     * @return JavaBean 对象
     */
    public static <T> T mapToBean(java.util.Map<String, Object> map, Class<T> type) {
        if (map == null || type == null) {
            return null;
        }

        try {
            T bean = type.newInstance();
            java.beans.BeanInfo beanInfo = java.beans.Introspector.getBeanInfo(type, Object.class);
            for (java.beans.PropertyDescriptor pd : beanInfo.getPropertyDescriptors()) {
                if (map.containsKey(pd.getName()) && pd.getWriteMethod() != null) {
                    pd.getWriteMethod().invoke(bean, map.get(pd.getName()));
                }
            }
            return bean;
        } catch (Exception e) {
            throw new RuntimeException("Map 转换为 Bean 失败", e);
        }
    }

    /**
     * 将 Map 列表转换为 JavaBean 列表
     *
     * @param mapList    Map 列表
     * @param targetType JavaBean 类型
     * @param <T>        目标泛型
     * @return JavaBean 列表
     */
    public static <T> List<T> mapListToBeanList(List<Map<String, Object>> mapList, Class<T> targetType) {
        if (mapList == null || mapList.isEmpty()) {
            return Collections.emptyList();
        }

        List<T> result = new ArrayList<>();
        for (Map<String, Object> map : mapList) {
            T bean = mapToBean(map, targetType);
            result.add(bean);
        }
        return result;
    }

    /**
     * 判断类是否为标准 JavaBean
     * <p>
     * 判断条件：
     * <ul>
     *     <li>存在 public 无参构造方法</li>
     *     <li>存在至少一个属性（即存在 getter 方法）</li>
     * </ul>
     *
     * @param clazz 类对象
     * @return 是 JavaBean 返回 true，否则 false
     */
    public static boolean isBean(Class<?> clazz) {
        if (clazz == null) {
            return false;
        }

        try {
            // 必须有 public 无参构造函数
            clazz.getConstructor();

            // 至少存在一个属性的读取方法
            PropertyDescriptor[] pds = Introspector.getBeanInfo(clazz, Object.class).getPropertyDescriptors();
            return pds.length > 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 判断对象是否为标准 JavaBean
     * <p>
     * 判断条件：
     * <ul>
     *     <li>对象不为 null</li>
     *     <li>其类满足 {@link #isBean(Class)} 的判断</li>
     * </ul>
     *
     * @param bean 对象实例
     * @return 是 JavaBean 返回 true，否则 false
     */
    public static boolean isBean(Object bean) {
        return bean != null && isBean(bean.getClass());
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
            PropertyDescriptor[] pds =
                    Introspector.getBeanInfo(bean.getClass(), Object.class).getPropertyDescriptors();

            for (PropertyDescriptor pd : pds) {
                if (pd.getReadMethod() != null) {
                    Object value = pd.getReadMethod().invoke(bean);
                    if (value != null) {
                        return false;
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("判断属性是否全为 null 失败", e);
        }

        return true;
    }

    /**
     * 判断对象是否具有指定属性（具备 getter 方法）
     *
     * @param bean         Bean 实例
     * @param propertyName 属性名称
     * @return 存在该属性返回 true，否则返回 false
     */
    public static boolean hasProperty(Object bean, String propertyName) {
        if (bean == null || propertyName == null || propertyName.isEmpty()) {
            return false;
        }

        try {
            PropertyDescriptor[] pds =
                    Introspector.getBeanInfo(bean.getClass(), Object.class).getPropertyDescriptors();
            for (PropertyDescriptor pd : pds) {
                if (propertyName.equals(pd.getName()) && pd.getReadMethod() != null) {
                    return true;
                }
            }
        } catch (Exception ignored) {
        }

        return false;
    }

    /**
     * 根据属性名获取 JavaBean 中对应的值（支持泛型）
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
            // 优先通过 getter 方法获取
            java.beans.PropertyDescriptor pd = new java.beans.PropertyDescriptor(fieldName, bean.getClass());
            if (pd.getReadMethod() != null) {
                return (T) pd.getReadMethod().invoke(bean);
            }
        } catch (Exception ignore) {
            // 如果 getter 获取失败，则尝试直接反射字段
            try {
                java.lang.reflect.Field field = bean.getClass().getDeclaredField(fieldName);
                field.setAccessible(true);
                return (T) field.get(bean);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    /**
     * 根据属性名设置 JavaBean 中对应的值（支持泛型）
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
            // 优先通过 setter 方法设置
            java.beans.PropertyDescriptor pd = new java.beans.PropertyDescriptor(fieldName, bean.getClass());
            if (pd.getWriteMethod() != null) {
                pd.getWriteMethod().invoke(bean, value);
                return true;
            }
        } catch (Exception ignore) {
            // 如果 setter 设置失败，则尝试直接反射字段
            try {
                java.lang.reflect.Field field = bean.getClass().getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(bean, value);
                return true;
            } catch (Exception e) {
                return false;
            }
        }
        return false;
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
        if (bean == null || path == null) {
            return false;
        }
        try {
            String[] fields = path.split("\\.");
            Object current = bean;
            for (int i = 0; i < fields.length - 1; i++) {
                String field = fields[i];
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
                        return false;
                    }
                } else {
                    current = getProperty(current, field);
                }
                if (current == null) {
                    return false;
                }
            }

            String lastField = fields[fields.length - 1];
            int indexStart = lastField.indexOf('[');
            if (indexStart > -1) {
                String propName = lastField.substring(0, indexStart);
                int index = Integer.parseInt(lastField.substring(indexStart + 1, lastField.indexOf(']')));
                Object listOrArray = getProperty(current, propName);
                if (listOrArray instanceof java.util.List) {
                    ((java.util.List<Object>) listOrArray).set(index, value);
                    return true;
                } else if (listOrArray != null && listOrArray.getClass().isArray()) {
                    java.lang.reflect.Array.set(listOrArray, index, value);
                    return true;
                }
                return false;
            } else {
                return setProperty(current, lastField, value);
            }
        } catch (Exception e) {
            throw new RuntimeException("设置嵌套属性失败: " + path, e);
        }
    }

    /**
     * 对象深拷贝（Deep Copy）
     * <p>
     * 使用 Java 序列化和反序列化的方式将对象完全复制一份，
     * 包括其引用类型的字段，生成的新对象与原对象在内存中完全独立。
     * 注意：该方法要求对象及其所有嵌套对象都必须实现 {@link java.io.Serializable} 接口。
     *
     * @param object 原对象
     * @param <T>    对象类型
     * @return 深拷贝后的新对象
     * @throws RuntimeException 如果对象不可序列化或序列化/反序列化过程中发生异常
     */
    @SuppressWarnings("unchecked")
    public static <T> T deepCopy(T object) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(object);

            ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
            ObjectInputStream ois = new ObjectInputStream(bis);
            return (T) ois.readObject();
        } catch (Exception e) {
            throw new RuntimeException("深拷贝失败", e);
        }
    }

    /**
     * 比较两个 JavaBean 对象的属性差异
     * <p>
     * 该方法会逐个比较两个对象的同名属性值（通过 getter 方法获取），
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
        try {
            java.beans.BeanInfo beanInfo = java.beans.Introspector.getBeanInfo(oldBean.getClass(), Object.class);
            for (java.beans.PropertyDescriptor pd : beanInfo.getPropertyDescriptors()) {
                Object oldValue = pd.getReadMethod().invoke(oldBean);
                Object newValue = pd.getReadMethod().invoke(newBean);
                if (!Objects.equals(oldValue, newValue)) {
                    changes.put(pd.getName(), new Object[]{oldValue, newValue});
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Bean 对比失败", e);
        }
        return changes;
    }

    /**
     * 获取指定类（包含父类）的所有字段名。
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
                if (!nameSet.contains(field.getName())) {
                    fieldNames.add(field.getName());
                    nameSet.add(field.getName());
                }
            }
            clazz = clazz.getSuperclass();
        }
        return fieldNames;
    }

}

