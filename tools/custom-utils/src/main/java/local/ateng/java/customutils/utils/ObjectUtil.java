package local.ateng.java.customutils.utils;

import java.io.*;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * 对象工具类
 *
 * @author Ateng
 * @since 2025-07-26
 */
public final class ObjectUtil {

    /**
     * 禁止实例化工具类
     */
    private ObjectUtil() {
        throw new UnsupportedOperationException("工具类不可实例化");
    }

    /**
     * 检查对象是否为空
     *
     * @param object 要检查的对象
     * @return true：对象为空；false：对象不为空
     */
    public static boolean isNull(Object object) {
        return object == null;
    }

    /**
     * 检查对象是否非空
     *
     * @param object 要检查的对象
     * @return true：对象非空；false：对象为空
     */
    public static boolean isNotNull(Object object) {
        return object != null;
    }

    /**
     * 判断两个对象是否相等（考虑到 null 的情况）
     *
     * @param obj1 第一个对象
     * @param obj2 第二个对象
     * @return true：两个对象相等；false：两个对象不相等
     */
    public static boolean equals(Object obj1, Object obj2) {
        if (obj1 == obj2) {
            return true;
        }
        if (obj1 == null || obj2 == null) {
            return false;
        }
        return obj1.equals(obj2);
    }

    /**
     * 判断对象是否为指定类型的实例
     *
     * @param object 目标对象
     * @param clazz  目标类型
     * @return true：对象是指定类型的实例；false：不是
     */
    public static boolean isInstanceOf(Object object, Class<?> clazz) {
        return clazz.isInstance(object);
    }

    /**
     * 获取对象的非空默认值，如果对象为 null 则返回默认值
     *
     * @param object       要检查的对象
     * @param defaultValue 默认值
     * @param <T>          对象类型
     * @return 对象非空时返回对象值，否则返回默认值
     */
    public static <T> T defaultIfNull(T object, T defaultValue) {
        return object == null ? defaultValue : object;
    }

    /**
     * 判断对象是否为基本数据类型的包装类型
     *
     * @param object 要检查的对象
     * @return true：是基本数据类型的包装类；false：不是
     */
    public static boolean isWrapperType(Object object) {
        return object instanceof Integer ||
                object instanceof Long ||
                object instanceof Double ||
                object instanceof Float ||
                object instanceof Character ||
                object instanceof Boolean ||
                object instanceof Byte ||
                object instanceof Short ||
                object instanceof BigDecimal ||
                object instanceof BigInteger;
    }

    /**
     * 判断对象是否为空
     * <p>支持以下类型：</p>
     * <ul>
     *     <li>null</li>
     *     <li>CharSequence (String、StringBuilder、StringBuffer)</li>
     *     <li>Collection</li>
     *     <li>Map</li>
     *     <li>Optional</li>
     *     <li>数组（包括原始类型数组）</li>
     *     <li>Iterator</li>
     *     <li>Enumeration</li>
     * </ul>
     *
     * @param object 要判断的对象
     * @return true：对象为空；false：对象不为空
     */
    public static boolean isEmpty(Object object) {
        if (object == null) {
            return true;
        }
        // 字符串及其子类
        if (object instanceof CharSequence) {
            return ((CharSequence) object).toString().trim().isEmpty();
        }
        // 集合
        if (object instanceof Collection) {
            return ((Collection<?>) object).isEmpty();
        }
        // Map
        if (object instanceof Map) {
            return ((Map<?, ?>) object).isEmpty();
        }
        // Optional
        if (object instanceof Optional) {
            return !((Optional<?>) object).isPresent();
        }
        // 数组（包括原始类型数组）
        if (object.getClass().isArray()) {
            return Array.getLength(object) == 0;
        }
        // Iterator
        if (object instanceof Iterator) {
            return !((Iterator<?>) object).hasNext();
        }
        // Enumeration
        if (object instanceof Enumeration) {
            return !((Enumeration<?>) object).hasMoreElements();
        }
        // 其他对象不认为是空
        return false;
    }

    /**
     * 判断对象是否非空（不为 null、空字符串、空集合等）
     *
     * @param object 要判断的对象
     * @return true：对象非空；false：对象为空
     */
    public static boolean isNotEmpty(Object object) {
        return !isEmpty(object);
    }

    /**
     * 将对象转换为字节数组
     *
     * @param object 要转换的对象
     * @return 对象的字节数组，如果对象为 null 返回空字节数组
     */
    public static byte[] toByteArray(Object object) {
        if (object == null) {
            return new byte[0];
        }
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream)) {
            objectOutputStream.writeObject(object);
            return byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("对象转换为字节数组失败", e);
        }
    }

    /**
     * 将字节数组转换为对象
     *
     * @param bytes 字节数组
     * @return 转换后的对象
     */
    public static Object fromByteArray(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
             ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream)) {
            return objectInputStream.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException("字节数组转换为对象失败", e);
        }
    }

    /**
     * 深拷贝对象（实现 Cloneable 接口的对象）
     *
     * @param object 要拷贝的对象
     * @param <T>    对象类型
     * @return 拷贝后的对象
     */
    public static <T extends Cloneable> T deepClone(T object) {
        try {
            Method cloneMethod = object.getClass().getMethod("clone");
            return (T) cloneMethod.invoke(object);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("深拷贝失败", e);
        }
    }

    /**
     * 安全获取对象属性值，支持链式调用，避免 NullPointerException
     *
     * @param supplier 属性访问 lambda 表达式
     * @param <T>      返回值类型
     * @return 属性值，若中间任何一步为 null，则返回 null
     */
    public static <T> T getSafe(Supplier<T> supplier) {
        try {
            return supplier.get();
        } catch (NullPointerException e) {
            return null;
        }
    }

    /**
     * 获取对象的类名（不带包名）
     *
     * @param object 对象
     * @return 类名字符串；对象为 null 时返回 null
     */
    public static String getClassName(Object object) {
        return object == null ? null : object.getClass().getSimpleName();
    }

    /**
     * 获取对象的完整类名（带包名）
     *
     * @param object 对象
     * @return 完整类名；对象为 null 时返回 null
     */
    public static String getFullClassName(Object object) {
        return object == null ? null : object.getClass().getName();
    }

    /**
     * 判断两个对象是否类型一致（即使内容不同）
     *
     * @param obj1 第一个对象
     * @param obj2 第二个对象
     * @return true：同一类型；false：不同类型或有 null
     */
    public static boolean isSameType(Object obj1, Object obj2) {
        if (obj1 == null || obj2 == null) {
            return false;
        }
        return obj1.getClass().equals(obj2.getClass());
    }

    /**
     * 判断对象是否实现了指定接口
     *
     * @param object         对象
     * @param interfaceClass 接口类
     * @return true：实现了接口；false：没有或为 null
     */
    public static boolean implementsInterface(Object object, Class<?> interfaceClass) {
        if (object == null || interfaceClass == null || !interfaceClass.isInterface()) {
            return false;
        }
        return interfaceClass.isAssignableFrom(object.getClass());
    }

    /**
     * 判断对象是否为数组类型
     *
     * @param object 要判断的对象
     * @return true：是数组；false：不是或为 null
     */
    public static boolean isArray(Object object) {
        return object != null && object.getClass().isArray();
    }

    /**
     * 获取对象指定字段的值（支持 private 字段）
     *
     * @param object    目标对象
     * @param fieldName 字段名
     * @return 字段值，如果不存在则返回 null
     */
    public static Object getFieldValue(Object object, String fieldName) {
        if (object == null || fieldName == null || fieldName.isEmpty()) {
            return null;
        }
        try {
            Field field = getDeclaredField(object.getClass(), fieldName);
            if (field != null) {
                field.setAccessible(true);
                return field.get(object);
            }
        } catch (IllegalAccessException e) {
            // ignore
        }
        return null;
    }

    /**
     * 设置对象指定字段的值（支持 private 字段）
     *
     * @param object    目标对象
     * @param fieldName 字段名
     * @param value     要设置的值
     * @return true：设置成功；false：设置失败
     */
    public static boolean setFieldValue(Object object, String fieldName, Object value) {
        if (object == null || fieldName == null || fieldName.isEmpty()) {
            return false;
        }
        try {
            Field field = getDeclaredField(object.getClass(), fieldName);
            if (field != null) {
                field.setAccessible(true);
                field.set(object, value);
                return true;
            }
        } catch (IllegalAccessException e) {
            // ignore
        }
        return false;
    }

    /**
     * 获取对象所有字段（包括父类字段）
     *
     * @param clazz 对象类
     * @return 字段数组
     */
    public static Field[] getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        while (clazz != null && clazz != Object.class) {
            Collections.addAll(fields, clazz.getDeclaredFields());
            clazz = clazz.getSuperclass();
        }
        return fields.toArray(new Field[0]);
    }

    /**
     * 判断对象是否包含指定字段（包括父类字段）
     *
     * @param object    对象
     * @param fieldName 字段名
     * @return true：包含；false：不包含
     */
    public static boolean hasField(Object object, String fieldName) {
        if (object == null || fieldName == null || fieldName.isEmpty()) {
            return false;
        }
        return getDeclaredField(object.getClass(), fieldName) != null;
    }

    /**
     * 获取指定类的声明字段（支持继承层级）
     *
     * @param clazz     类
     * @param fieldName 字段名
     * @return 字段对象，如果找不到返回 null
     */
    private static Field getDeclaredField(Class<?> clazz, String fieldName) {
        while (clazz != null && clazz != Object.class) {
            try {
                return clazz.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        return null;
    }

    /**
     * 深度比较两个对象是否相等（递归比较对象内部属性）
     *
     * @param obj1 第一个对象
     * @param obj2 第二个对象
     * @return true：两个对象深度相等；false：不相等
     */
    public static boolean deepEquals(Object obj1, Object obj2) {
        if (obj1 == obj2) {
            return true;
        }
        if (obj1 == null || obj2 == null) {
            return false;
        }
        if (obj1.getClass() != obj2.getClass()) {
            return false;
        }

        // 如果是基本数据类型或字符串，直接比较
        if (obj1.getClass().isPrimitive() || obj1 instanceof String) {
            return obj1.equals(obj2);
        }

        // 如果是数组，递归比较每个元素
        if (obj1.getClass().isArray()) {
            if (Array.getLength(obj1) != Array.getLength(obj2)) {
                return false;
            }
            for (int i = 0; i < Array.getLength(obj1); i++) {
                if (!deepEquals(Array.get(obj1, i), Array.get(obj2, i))) {
                    return false;
                }
            }
            return true;
        }

        // 对象字段递归比较
        Field[] fields = getAllFields(obj1.getClass());
        for (Field field : fields) {
            try {
                field.setAccessible(true);
                if (!deepEquals(field.get(obj1), field.get(obj2))) {
                    return false;
                }
            } catch (IllegalAccessException e) {
                // ignore
            }
        }

        return true;
    }

    /**
     * 深度复制对象（通过序列化）
     *
     * @param object 要复制的对象
     * @param <T>    对象类型
     * @return 深度复制后的新对象
     */
    public static <T> T deepCopy(T object) {
        if (object == null) {
            return null;
        }
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream)) {
            // 序列化对象
            objectOutputStream.writeObject(object);
            // 反序列化对象
            try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
                 ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream)) {
                return (T) objectInputStream.readObject();
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException("深度复制对象失败", e);
        }
    }

    /**
     * 获取对象属性（包括父类和接口）并返回一个 Map
     *
     * @param object 目标对象
     * @return Map，key 为字段名，value 为字段值
     */
    public static Map<String, Object> getObjectProperties(Object object) {
        Map<String, Object> properties = new HashMap<>();
        if (object == null) {
            return properties;
        }

        // 获取所有字段，包括父类字段
        Field[] fields = getAllFields(object.getClass());
        for (Field field : fields) {
            try {
                field.setAccessible(true);
                properties.put(field.getName(), field.get(object));
            } catch (IllegalAccessException e) {
                // ignore
            }
        }
        return properties;
    }

    /**
     * 比较对象的两个字段值是否相等（支持嵌套对象）
     *
     * @param object 对象
     * @param field1 第一个字段名
     * @param field2 第二个字段名
     * @return true：两个字段值相等；false：不相等
     */
    public static boolean compareFields(Object object, String field1, String field2) {
        if (object == null || field1 == null || field2 == null) {
            return false;
        }
        try {
            Object value1 = getFieldValue(object, field1);
            Object value2 = getFieldValue(object, field2);
            return deepEquals(value1, value2);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 将对象的属性值从一个对象拷贝到另一个对象（通过反射）
     *
     * @param source 源对象
     * @param target 目标对象
     * @return true：拷贝成功；false：拷贝失败
     */
    public static boolean copyProperties(Object source, Object target) {
        if (source == null || target == null) {
            return false;
        }
        try {
            Field[] sourceFields = getAllFields(source.getClass());
            for (Field sourceField : sourceFields) {
                sourceField.setAccessible(true);
                Object value = sourceField.get(source);
                Field targetField = getDeclaredField(target.getClass(), sourceField.getName());
                if (targetField != null) {
                    targetField.setAccessible(true);
                    targetField.set(target, value);
                }
            }
            return true;
        } catch (IllegalAccessException e) {
            return false;
        }
    }

    /**
     * 将对象的属性值从一个对象拷贝到另一个对象（包括嵌套对象），支持集合类型
     *
     * @param source 源对象
     * @param target 目标对象
     * @param <T>    对象类型
     * @return true：拷贝成功；false：拷贝失败
     */
    public static <T> boolean deepCopyProperties(Object source, T target) {
        if (source == null || target == null) {
            return false;
        }
        try {
            Field[] sourceFields = getAllFields(source.getClass());
            for (Field sourceField : sourceFields) {
                sourceField.setAccessible(true);
                Object value = sourceField.get(source);

                Field targetField = getDeclaredField(target.getClass(), sourceField.getName());
                if (targetField != null) {
                    targetField.setAccessible(true);

                    // 如果字段类型是集合类型，需要转换每个元素
                    if (Collection.class.isAssignableFrom(targetField.getType()) && value instanceof Collection) {
                        Collection<?> sourceCollection = (Collection<?>) value;
                        Collection<Object> targetCollection = (Collection<Object>) targetField.get(target);
                        if (targetCollection == null) {
                            targetCollection = new ArrayList<>();
                        }

                        for (Object item : sourceCollection) {
                            targetCollection.add(item);
                        }

                        targetField.set(target, targetCollection);
                    } else {
                        targetField.set(target, value);
                    }
                }
            }
            return true;
        } catch (IllegalAccessException e) {
            return false;
        }
    }

    /**
     * 将一个对象列表转换为另一种类型的对象列表
     *
     * @param sourceList  源对象列表
     * @param targetClass 目标对象类型
     * @param <T>         目标对象类型
     * @param <R>         源对象类型
     * @return 转换后的对象列表
     */
    public static <T, R> List<T> convertList(List<R> sourceList, Class<T> targetClass) {
        if (sourceList == null || targetClass == null) {
            return Collections.emptyList();
        }
        List<T> targetList = new ArrayList<>();
        try {
            for (R source : sourceList) {
                T target = targetClass.getDeclaredConstructor().newInstance();
                deepCopyProperties(source, target);
                targetList.add(target);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return targetList;
    }

    /**
     * 将对象列表中的某个字段提取成列表
     *
     * @param sourceList 源对象列表
     * @param fieldName  目标字段名称
     * @param <T>        对象类型
     * @param <V>        字段类型
     * @return 字段值的列表
     */
    public static <T, V> List<V> extractFieldToList(List<T> sourceList, String fieldName) {
        List<V> result = new ArrayList<>();
        for (T item : sourceList) {
            V fieldValue = (V) getFieldValue(item, fieldName);
            if (fieldValue != null) {
                result.add(fieldValue);
            }
        }
        return result;
    }

    /**
     * 将对象列表转换为另一个对象列表，并对字段进行修改（例如增加、修改、过滤）
     *
     * @param sourceList  源对象列表
     * @param transformer 转换函数（可以修改字段）
     * @param <T>         目标对象类型
     * @param <R>         源对象类型
     * @return 转换后的对象列表
     */
    public static <T, R> List<T> transformList(List<R> sourceList, Function<R, T> transformer) {
        if (sourceList == null || transformer == null) {
            return Collections.emptyList();
        }
        List<T> targetList = new ArrayList<>();
        for (R source : sourceList) {
            targetList.add(transformer.apply(source));
        }
        return targetList;
    }

    /**
     * 从对象列表中按条件过滤出符合条件的对象
     *
     * @param sourceList 源对象列表
     * @param predicate  过滤条件
     * @param <T>        对象类型
     * @return 符合条件的对象列表
     */
    public static <T> List<T> filterList(List<T> sourceList, Predicate<T> predicate) {
        if (sourceList == null || predicate == null) {
            return Collections.emptyList();
        }
        List<T> result = new ArrayList<>();
        for (T item : sourceList) {
            if (predicate.test(item)) {
                result.add(item);
            }
        }
        return result;
    }

    /**
     * 将一个对象转换为指定类型（通过反射或自定义映射）
     *
     * @param source 要转换的源对象
     * @param targetClass 目标对象类型
     * @param <T> 目标对象类型
     * @return 转换后的目标对象
     */
    public static <T> T convertTo(Object source, Class<T> targetClass) {
        if (source == null || targetClass == null) {
            return null;
        }
        try {
            T target = targetClass.getDeclaredConstructor().newInstance();
            deepCopyProperties(source, target);
            return target;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取对象的属性值，如果为空则返回指定默认值
     *
     * @param object 对象
     * @param fieldName 字段名
     * @param defaultValue 默认值
     * @return 字段值，如果字段为空则返回默认值
     */
    public static <T> T getFieldOrDefault(Object object, String fieldName, T defaultValue) {
        T fieldValue = (T) getFieldValue(object, fieldName);
        return fieldValue == null ? defaultValue : fieldValue;
    }

    /**
     * 判断对象是否为空（支持 Optional 类型）
     *
     * @param object 对象
     * @return true：对象为空；false：对象非空
     */
    public static boolean isNullOrEmpty(Object object) {
        if (object == null) {
            return true;
        }
        if (object instanceof Optional) {
            return !((Optional<?>) object).isPresent();
        }
        if (object instanceof Collection) {
            return ((Collection<?>) object).isEmpty();
        }
        if (object instanceof Map) {
            return ((Map<?, ?>) object).isEmpty();
        }
        if (object instanceof String) {
            return ((String) object).trim().isEmpty();
        }
        return false;
    }

    /**
     * 自定义比较器（如排序），比较两个对象的某个字段
     *
     * @param <T> 对象类型
     * @param fieldName 字段名
     * @param comparator 字段比较器
     * @return 比较器
     */
    public static <T> Comparator<T> fieldComparator(String fieldName, Comparator<Object> comparator) {
        return (T obj1, T obj2) -> {
            Object field1 = getFieldValue(obj1, fieldName);
            Object field2 = getFieldValue(obj2, fieldName);
            return comparator.compare(field1, field2);
        };
    }

    /**
     * 对象列表去重（根据指定字段）
     *
     * @param sourceList 源对象列表
     * @param fieldName 用于去重的字段名
     * @param <T> 对象类型
     * @return 去重后的列表
     */
    public static <T> List<T> distinctByField(List<T> sourceList, String fieldName) {
        if (sourceList == null || fieldName == null) {
            return Collections.emptyList();
        }
        Set<Object> seen = new HashSet<>();
        List<T> resultList = new ArrayList<>();
        for (T item : sourceList) {
            Object fieldValue = getFieldValue(item, fieldName);
            if (seen.add(fieldValue)) {
                resultList.add(item);
            }
        }
        return resultList;
    }

    /**
     * 将对象转换为字节数组并进行 Base64 编码
     *
     * @param object 对象
     * @return 编码后的 Base64 字符串
     */
    public static String objectToBase64(Object object) {
        if (object == null) {
            return "";
        }
        try {
            byte[] bytes = toByteArray(object);
            return Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            throw new RuntimeException("对象转换为 Base64 字符串失败", e);
        }
    }

    /**
     * 将 Base64 编码的字符串转换回对象
     *
     * @param base64String Base64 编码的字符串
     * @return 转换后的对象
     */
    public static Object base64ToObject(String base64String) {
        if (base64String == null || base64String.isEmpty()) {
            return null;
        }
        try {
            byte[] bytes = Base64.getDecoder().decode(base64String);
            return fromByteArray(bytes);
        } catch (Exception e) {
            throw new RuntimeException("Base64 字符串转换回对象失败", e);
        }
    }

    /**
     * 判断两个对象是否不相等（考虑 null）
     *
     * @param obj1 第一个对象
     * @param obj2 第二个对象
     * @return true：不相等；false：相等
     */
    public static boolean notEquals(Object obj1, Object obj2) {
        return !equals(obj1, obj2);
    }

    /**
     * 判断对象是否等于任意一个目标值
     *
     * @param object  目标对象
     * @param targets 待比较目标值
     * @return true：存在相等值；false：不存在
     */
    public static boolean equalsAny(Object object, Object... targets) {
        if (targets == null || targets.length == 0) {
            return false;
        }
        for (Object target : targets) {
            if (equals(object, target)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断对象是否不等于所有目标值
     *
     * @param object  目标对象
     * @param targets 待比较目标值
     * @return true：全部不相等；false：存在相等值
     */
    public static boolean notEqualsAny(Object object, Object... targets) {
        return !equalsAny(object, targets);
    }

    /**
     * 获取非空默认值，如果对象为 null 则通过 Supplier 获取默认值
     *
     * @param object          要检查的对象
     * @param defaultSupplier 默认值提供器
     * @param <T>             对象类型
     * @return 对象非空时返回对象值，否则返回默认值
     */
    public static <T> T defaultIfNullGet(T object, Supplier<T> defaultSupplier) {
        if (object != null) {
            return object;
        }
        return defaultSupplier == null ? null : defaultSupplier.get();
    }

    /**
     * 获取非空默认值，如果对象为空字符串、空集合、空数组等则返回默认值
     *
     * @param object       要检查的对象
     * @param defaultValue 默认值
     * @param <T>          对象类型
     * @return 对象非空时返回对象值，否则返回默认值
     */
    public static <T> T defaultIfEmpty(T object, T defaultValue) {
        return isEmpty(object) ? defaultValue : object;
    }

    /**
     * 获取非空默认值，如果对象为空字符串、空集合、空数组等则通过 Supplier 获取默认值
     *
     * @param object          要检查的对象
     * @param defaultSupplier 默认值提供器
     * @param <T>             对象类型
     * @return 对象非空时返回对象值，否则返回默认值
     */
    public static <T> T defaultIfEmptyGet(T object, Supplier<T> defaultSupplier) {
        if (isNotEmpty(object)) {
            return object;
        }
        return defaultSupplier == null ? null : defaultSupplier.get();
    }

    /**
     * 返回第一个非 null 对象
     *
     * @param values 对象数组
     * @param <T>    对象类型
     * @return 第一个非 null 对象，如果不存在则返回 null
     */
    @SafeVarargs
    public static <T> T firstNonNull(T... values) {
        if (values == null || values.length == 0) {
            return null;
        }
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    /**
     * 返回第一个非空对象
     *
     * @param values 对象数组
     * @param <T>    对象类型
     * @return 第一个非空对象，如果不存在则返回 null
     */
    @SafeVarargs
    public static <T> T firstNonEmpty(T... values) {
        if (values == null || values.length == 0) {
            return null;
        }
        for (T value : values) {
            if (isNotEmpty(value)) {
                return value;
            }
        }
        return null;
    }

    /**
     * 校验对象不能为 null
     *
     * @param object  对象
     * @param message 异常信息
     * @param <T>     对象类型
     * @return 原对象
     */
    public static <T> T requireNonNull(T object, String message) {
        if (object == null) {
            throw new IllegalArgumentException(message == null ? "对象不能为 null" : message);
        }
        return object;
    }

    /**
     * 校验对象不能为空
     *
     * @param object  对象
     * @param message 异常信息
     * @param <T>     对象类型
     * @return 原对象
     */
    public static <T> T requireNonEmpty(T object, String message) {
        if (isEmpty(object)) {
            throw new IllegalArgumentException(message == null ? "对象不能为空" : message);
        }
        return object;
    }

    /**
     * 判断是否全部为 null
     *
     * @param objects 对象数组
     * @return true：全部为 null；false：存在非 null
     */
    public static boolean isAllNull(Object... objects) {
        if (objects == null || objects.length == 0) {
            return true;
        }
        for (Object object : objects) {
            if (object != null) {
                return false;
            }
        }
        return true;
    }

    /**
     * 判断是否存在 null
     *
     * @param objects 对象数组
     * @return true：存在 null；false：全部非 null
     */
    public static boolean isAnyNull(Object... objects) {
        if (objects == null || objects.length == 0) {
            return true;
        }
        for (Object object : objects) {
            if (object == null) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断是否全部非 null
     *
     * @param objects 对象数组
     * @return true：全部非 null；false：存在 null
     */
    public static boolean isNoneNull(Object... objects) {
        return !isAnyNull(objects);
    }

    /**
     * 判断是否全部为空
     *
     * @param objects 对象数组
     * @return true：全部为空；false：存在非空
     */
    public static boolean isAllEmpty(Object... objects) {
        if (objects == null || objects.length == 0) {
            return true;
        }
        for (Object object : objects) {
            if (isNotEmpty(object)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 判断是否存在空对象
     *
     * @param objects 对象数组
     * @return true：存在空对象；false：全部非空
     */
    public static boolean isAnyEmpty(Object... objects) {
        if (objects == null || objects.length == 0) {
            return true;
        }
        for (Object object : objects) {
            if (isEmpty(object)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断是否全部非空
     *
     * @param objects 对象数组
     * @return true：全部非空；false：存在空对象
     */
    public static boolean isNoneEmpty(Object... objects) {
        return !isAnyEmpty(objects);
    }

    /**
     * 判断 Class 是否为基本类型包装类
     *
     * @param clazz 类型
     * @return true：是包装类；false：不是
     */
    public static boolean isWrapperType(Class<?> clazz) {
        if (clazz == null) {
            return false;
        }
        return clazz == Integer.class ||
                clazz == Long.class ||
                clazz == Double.class ||
                clazz == Float.class ||
                clazz == Character.class ||
                clazz == Boolean.class ||
                clazz == Byte.class ||
                clazz == Short.class ||
                clazz == Void.class ||
                clazz == BigDecimal.class ||
                clazz == BigInteger.class;
    }

    /**
     * 判断对象是否为简单值类型
     *
     * @param object 对象
     * @return true：简单值类型；false：非简单值类型
     */
    public static boolean isSimpleValueType(Object object) {
        return object != null && isSimpleValueType(object.getClass());
    }

    /**
     * 判断 Class 是否为简单值类型
     *
     * @param clazz 类型
     * @return true：简单值类型；false：非简单值类型
     */
    public static boolean isSimpleValueType(Class<?> clazz) {
        if (clazz == null) {
            return false;
        }
        return clazz.isPrimitive() ||
                isWrapperType(clazz) ||
                clazz == String.class ||
                clazz == CharSequence.class ||
                Number.class.isAssignableFrom(clazz) ||
                Date.class.isAssignableFrom(clazz) ||
                clazz.isEnum() ||
                clazz == UUID.class ||
                clazz == Class.class ||
                java.time.temporal.Temporal.class.isAssignableFrom(clazz);
    }

    /**
     * 判断对象是否可序列化
     *
     * @param object 对象
     * @return true：可序列化；false：不可序列化
     */
    public static boolean isSerializable(Object object) {
        return object instanceof Serializable;
    }

    /**
     * 判断对象是否为集合类型
     *
     * @param object 对象
     * @return true：集合类型；false：非集合类型
     */
    public static boolean isCollection(Object object) {
        return object instanceof Collection;
    }

    /**
     * 判断对象是否为 Map 类型
     *
     * @param object 对象
     * @return true：Map 类型；false：非 Map 类型
     */
    public static boolean isMap(Object object) {
        return object instanceof Map;
    }

    /**
     * 判断对象是否为 CharSequence 类型
     *
     * @param object 对象
     * @return true：CharSequence 类型；false：非 CharSequence 类型
     */
    public static boolean isCharSequence(Object object) {
        return object instanceof CharSequence;
    }

    /**
     * 获取对象长度
     * <p>支持 CharSequence、Collection、Map、Optional、数组、Iterator、Enumeration。</p>
     *
     * @param object 对象
     * @return 对象长度；不支持的类型返回 -1
     */
    public static int length(Object object) {
        if (object == null) {
            return 0;
        }
        if (object instanceof CharSequence) {
            return ((CharSequence) object).length();
        }
        if (object instanceof Collection) {
            return ((Collection<?>) object).size();
        }
        if (object instanceof Map) {
            return ((Map<?, ?>) object).size();
        }
        if (object instanceof Optional) {
            return ((Optional<?>) object).isPresent() ? 1 : 0;
        }
        if (object.getClass().isArray()) {
            return Array.getLength(object);
        }
        if (object instanceof Iterator) {
            int count = 0;
            Iterator<?> iterator = (Iterator<?>) object;
            while (iterator.hasNext()) {
                iterator.next();
                count++;
            }
            return count;
        }
        if (object instanceof Enumeration) {
            int count = 0;
            Enumeration<?> enumeration = (Enumeration<?>) object;
            while (enumeration.hasMoreElements()) {
                enumeration.nextElement();
                count++;
            }
            return count;
        }
        return -1;
    }

    /**
     * 判断容器对象是否包含指定元素
     * <p>支持 CharSequence、Collection、Map、数组。</p>
     *
     * @param container 容器对象
     * @param element   元素
     * @return true：包含；false：不包含
     */
    public static boolean contains(Object container, Object element) {
        if (container == null) {
            return false;
        }
        if (container instanceof CharSequence) {
            return element != null && container.toString().contains(element.toString());
        }
        if (container instanceof Collection) {
            return ((Collection<?>) container).contains(element);
        }
        if (container instanceof Map) {
            return ((Map<?, ?>) container).containsKey(element);
        }
        if (container.getClass().isArray()) {
            int length = Array.getLength(container);
            for (int i = 0; i < length; i++) {
                if (equals(Array.get(container, i), element)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 判断对象是否在给定数组中
     *
     * @param object  目标对象
     * @param targets 候选对象
     * @return true：存在；false：不存在
     */
    public static boolean isIn(Object object, Object... targets) {
        return contains(targets, object);
    }

    /**
     * 安全获取数组元素
     *
     * @param array 数组对象
     * @param index 索引
     * @return 元素值，数组无效或索引越界时返回 null
     */
    public static Object getArrayElement(Object array, int index) {
        if (array == null || !array.getClass().isArray()) {
            return null;
        }
        int length = Array.getLength(array);
        if (index < 0 || index >= length) {
            return null;
        }
        return Array.get(array, index);
    }

    /**
     * 安全转换对象类型
     *
     * @param object      对象
     * @param targetClass 目标类型
     * @param <T>         目标类型
     * @return 转换后的对象，无法转换返回 null
     */
    public static <T> T cast(Object object, Class<T> targetClass) {
        if (object == null || targetClass == null || !targetClass.isInstance(object)) {
            return null;
        }
        return targetClass.cast(object);
    }

    /**
     * 安全转换对象类型，无法转换时返回默认值
     *
     * @param object       对象
     * @param targetClass  目标类型
     * @param defaultValue 默认值
     * @param <T>          目标类型
     * @return 转换后的对象，无法转换返回默认值
     */
    public static <T> T castOrDefault(Object object, Class<T> targetClass, T defaultValue) {
        T value = cast(object, targetClass);
        return value == null ? defaultValue : value;
    }

    /**
     * 创建对象实例
     *
     * @param clazz 类型
     * @param <T>   对象类型
     * @return 对象实例，创建失败返回 null
     */
    public static <T> T newInstance(Class<T> clazz) {
        if (clazz == null) {
            return null;
        }
        try {
            java.lang.reflect.Constructor<T> constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取对象所有方法（包括父类方法）
     *
     * @param clazz 类型
     * @return 方法数组
     */
    public static Method[] getAllMethods(Class<?> clazz) {
        List<Method> methods = new ArrayList<>();
        Set<String> methodSignatures = new HashSet<>();
        while (clazz != null && clazz != Object.class) {
            Method[] declaredMethods = clazz.getDeclaredMethods();
            for (Method method : declaredMethods) {
                String signature = method.getName() + Arrays.toString(method.getParameterTypes());
                if (methodSignatures.add(signature)) {
                    methods.add(method);
                }
            }
            clazz = clazz.getSuperclass();
        }
        return methods.toArray(new Method[0]);
    }

    /**
     * 判断对象是否包含指定方法
     *
     * @param object         对象
     * @param methodName     方法名
     * @param parameterTypes 参数类型
     * @return true：包含；false：不包含
     */
    public static boolean hasMethod(Object object, String methodName, Class<?>... parameterTypes) {
        if (object == null || methodName == null || methodName.isEmpty()) {
            return false;
        }
        return getDeclaredMethod(object.getClass(), methodName, parameterTypes) != null;
    }

    /**
     * 调用对象无参方法
     *
     * @param object     对象
     * @param methodName 方法名
     * @return 方法返回值，调用失败返回 null
     */
    public static Object invokeMethod(Object object, String methodName) {
        return invokeMethod(object, methodName, new Class<?>[0]);
    }

    /**
     * 调用对象方法
     *
     * @param object         对象
     * @param methodName     方法名
     * @param parameterTypes 参数类型
     * @param args           参数值
     * @return 方法返回值，调用失败返回 null
     */
    public static Object invokeMethod(Object object, String methodName, Class<?>[] parameterTypes, Object... args) {
        if (object == null || methodName == null || methodName.isEmpty()) {
            return null;
        }
        try {
            Method method = getDeclaredMethod(object.getClass(), methodName, parameterTypes);
            if (method == null) {
                return null;
            }
            method.setAccessible(true);
            return method.invoke(object, args);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取嵌套字段值，字段路径使用 "." 分隔
     *
     * @param object    对象
     * @param fieldPath 字段路径，例如 user.address.city
     * @return 字段值，获取失败返回 null
     */
    public static Object getNestedFieldValue(Object object, String fieldPath) {
        if (object == null || fieldPath == null || fieldPath.trim().isEmpty()) {
            return null;
        }
        Object current = object;
        String[] fieldNames = fieldPath.split("\\.");
        for (String fieldName : fieldNames) {
            if (current == null || fieldName == null || fieldName.trim().isEmpty()) {
                return null;
            }
            current = getFieldValue(current, fieldName);
        }
        return current;
    }

    /**
     * 设置嵌套字段值，字段路径使用 "." 分隔
     *
     * @param object    对象
     * @param fieldPath 字段路径，例如 user.address.city
     * @param value     字段值
     * @return true：设置成功；false：设置失败
     */
    public static boolean setNestedFieldValue(Object object, String fieldPath, Object value) {
        if (object == null || fieldPath == null || fieldPath.trim().isEmpty()) {
            return false;
        }
        String[] fieldNames = fieldPath.split("\\.");
        if (fieldNames.length == 0) {
            return false;
        }
        Object current = object;
        for (int i = 0; i < fieldNames.length - 1; i++) {
            current = getFieldValue(current, fieldNames[i]);
            if (current == null) {
                return false;
            }
        }
        return setFieldValue(current, fieldNames[fieldNames.length - 1], value);
    }

    /**
     * 判断对象是否包含嵌套字段路径
     *
     * @param object    对象
     * @param fieldPath 字段路径
     * @return true：包含；false：不包含
     */
    public static boolean hasNestedField(Object object, String fieldPath) {
        if (object == null || fieldPath == null || fieldPath.trim().isEmpty()) {
            return false;
        }
        Object current = object;
        String[] fieldNames = fieldPath.split("\\.");
        for (int i = 0; i < fieldNames.length; i++) {
            if (current == null || fieldNames[i] == null || fieldNames[i].trim().isEmpty()) {
                return false;
            }
            if (!hasField(current, fieldNames[i])) {
                return false;
            }
            if (i < fieldNames.length - 1) {
                current = getFieldValue(current, fieldNames[i]);
            }
        }
        return true;
    }

    /**
     * 获取多个字段值
     *
     * @param object     对象
     * @param fieldNames 字段名数组
     * @return 字段值 Map
     */
    public static Map<String, Object> getFieldValues(Object object, String... fieldNames) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (object == null || fieldNames == null || fieldNames.length == 0) {
            return result;
        }
        for (String fieldName : fieldNames) {
            if (fieldName != null && !fieldName.trim().isEmpty()) {
                result.put(fieldName, getFieldValue(object, fieldName));
            }
        }
        return result;
    }

    /**
     * 判断对象是否包含任意字段
     *
     * @param object     对象
     * @param fieldNames 字段名数组
     * @return true：包含任意字段；false：不包含
     */
    public static boolean hasAnyField(Object object, String... fieldNames) {
        if (object == null || fieldNames == null || fieldNames.length == 0) {
            return false;
        }
        for (String fieldName : fieldNames) {
            if (hasField(object, fieldName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断对象是否包含全部字段
     *
     * @param object     对象
     * @param fieldNames 字段名数组
     * @return true：包含全部字段；false：不包含
     */
    public static boolean hasAllFields(Object object, String... fieldNames) {
        if (object == null || fieldNames == null || fieldNames.length == 0) {
            return false;
        }
        for (String fieldName : fieldNames) {
            if (!hasField(object, fieldName)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 拷贝对象属性，忽略 null 值
     *
     * @param source 源对象
     * @param target 目标对象
     * @return true：拷贝成功；false：拷贝失败
     */
    public static boolean copyPropertiesIgnoreNull(Object source, Object target) {
        return copyProperties(source, target, true);
    }

    /**
     * 拷贝对象属性，支持忽略 null 值和指定字段
     *
     * @param source       源对象
     * @param target       目标对象
     * @param ignoreNull   是否忽略 null 值
     * @param ignoreFields 忽略字段名
     * @return true：拷贝成功；false：拷贝失败
     */
    public static boolean copyProperties(Object source, Object target, boolean ignoreNull, String... ignoreFields) {
        if (source == null || target == null) {
            return false;
        }
        Set<String> ignoreFieldSet = new HashSet<>();
        if (ignoreFields != null) {
            ignoreFieldSet.addAll(Arrays.asList(ignoreFields));
        }
        try {
            Field[] sourceFields = getAllFields(source.getClass());
            for (Field sourceField : sourceFields) {
                if (ignoreFieldSet.contains(sourceField.getName())) {
                    continue;
                }
                sourceField.setAccessible(true);
                Object value = sourceField.get(source);
                if (ignoreNull && value == null) {
                    continue;
                }

                Field targetField = getDeclaredField(target.getClass(), sourceField.getName());
                if (targetField == null || java.lang.reflect.Modifier.isFinal(targetField.getModifiers())) {
                    continue;
                }
                targetField.setAccessible(true);
                if (isAssignableValue(targetField.getType(), value)) {
                    targetField.set(target, value);
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 对象转 Map，默认不忽略 null 值
     *
     * @param object 对象
     * @return Map 对象
     */
    public static Map<String, Object> toMap(Object object) {
        return toMap(object, false);
    }

    /**
     * 对象转 Map
     *
     * @param object     对象
     * @param ignoreNull 是否忽略 null 值
     * @return Map 对象
     */
    public static Map<String, Object> toMap(Object object, boolean ignoreNull) {
        return toMap(object, ignoreNull, true, true);
    }

    /**
     * 对象转 Map
     *
     * @param object          对象
     * @param ignoreNull      是否忽略 null 值
     * @param ignoreStatic    是否忽略 static 字段
     * @param ignoreTransient 是否忽略 transient 字段
     * @return Map 对象
     */
    public static Map<String, Object> toMap(Object object, boolean ignoreNull, boolean ignoreStatic, boolean ignoreTransient) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (object == null) {
            return result;
        }
        Field[] fields = getAllFields(object.getClass());
        for (Field field : fields) {
            int modifiers = field.getModifiers();
            if (ignoreStatic && java.lang.reflect.Modifier.isStatic(modifiers)) {
                continue;
            }
            if (ignoreTransient && java.lang.reflect.Modifier.isTransient(modifiers)) {
                continue;
            }
            try {
                field.setAccessible(true);
                Object value = field.get(object);
                if (ignoreNull && value == null) {
                    continue;
                }
                result.put(field.getName(), value);
            } catch (IllegalAccessException e) {
                // ignore
            }
        }
        return result;
    }

    /**
     * Map 填充对象属性
     *
     * @param map    Map 数据
     * @param target 目标对象
     * @param <T>    目标对象类型
     * @return true：填充成功；false：填充失败
     */
    public static <T> boolean fillObjectFromMap(Map<String, ?> map, T target) {
        if (map == null || map.isEmpty() || target == null) {
            return false;
        }
        try {
            for (Map.Entry<String, ?> entry : map.entrySet()) {
                Field field = getDeclaredField(target.getClass(), entry.getKey());
                if (field == null || java.lang.reflect.Modifier.isFinal(field.getModifiers())) {
                    continue;
                }
                Object value = entry.getValue();
                if (!isAssignableValue(field.getType(), value)) {
                    continue;
                }
                field.setAccessible(true);
                field.set(target, value);
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Map 转对象
     *
     * @param map         Map 数据
     * @param targetClass 目标对象类型
     * @param <T>         目标对象类型
     * @return 目标对象，转换失败返回 null
     */
    public static <T> T mapToObject(Map<String, ?> map, Class<T> targetClass) {
        if (map == null || targetClass == null) {
            return null;
        }
        T target = newInstance(targetClass);
        if (target == null) {
            return null;
        }
        fillObjectFromMap(map, target);
        return target;
    }

    /**
     * 对象为 null 时返回空字符串，否则返回对象字符串
     *
     * @param object 对象
     * @return 字符串
     */
    public static String nullToEmpty(Object object) {
        return object == null ? "" : object.toString();
    }

    /**
     * 对象为 null 时返回默认字符串，否则返回对象字符串
     *
     * @param object       对象
     * @param defaultValue 默认字符串
     * @return 字符串
     */
    public static String toStringOrDefault(Object object, String defaultValue) {
        return object == null ? defaultValue : object.toString();
    }

    /**
     * 空对象转 null
     *
     * @param object 对象
     * @param <T>    对象类型
     * @return 空对象返回 null，否则返回原对象
     */
    public static <T> T emptyToNull(T object) {
        return isEmpty(object) ? null : object;
    }

    /**
     * 获取对象标识字符串
     *
     * @param object 对象
     * @return 对象标识字符串，对象为 null 返回 null
     */
    public static String identityToString(Object object) {
        if (object == null) {
            return null;
        }
        return object.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(object));
    }

    /**
     * 计算多个对象的 hash 值
     *
     * @param objects 对象数组
     * @return hash 值
     */
    public static int hash(Object... objects) {
        return Arrays.hashCode(objects);
    }

    /**
     * 获取指定类的声明方法（支持继承层级）
     *
     * @param clazz          类
     * @param methodName     方法名
     * @param parameterTypes 参数类型
     * @return 方法对象，如果找不到返回 null
     */
    private static Method getDeclaredMethod(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        while (clazz != null && clazz != Object.class) {
            try {
                return clazz.getDeclaredMethod(methodName, parameterTypes);
            } catch (NoSuchMethodException e) {
                clazz = clazz.getSuperclass();
            }
        }
        return null;
    }

    /**
     * 判断字段值是否可以赋值给目标类型
     *
     * @param targetType 目标类型
     * @param value      字段值
     * @return true：可以赋值；false：不可赋值
     */
    private static boolean isAssignableValue(Class<?> targetType, Object value) {
        if (targetType == null) {
            return false;
        }
        if (value == null) {
            return !targetType.isPrimitive();
        }
        Class<?> valueType = value.getClass();
        if (targetType.isPrimitive()) {
            return primitiveToWrapper(targetType).isAssignableFrom(valueType);
        }
        return targetType.isAssignableFrom(valueType);
    }

    /**
     * 基本类型转包装类型
     *
     * @param clazz 基本类型
     * @return 包装类型
     */
    private static Class<?> primitiveToWrapper(Class<?> clazz) {
        if (clazz == int.class) {
            return Integer.class;
        }
        if (clazz == long.class) {
            return Long.class;
        }
        if (clazz == double.class) {
            return Double.class;
        }
        if (clazz == float.class) {
            return Float.class;
        }
        if (clazz == boolean.class) {
            return Boolean.class;
        }
        if (clazz == char.class) {
            return Character.class;
        }
        if (clazz == byte.class) {
            return Byte.class;
        }
        if (clazz == short.class) {
            return Short.class;
        }
        if (clazz == void.class) {
            return Void.class;
        }
        return clazz;
    }
}
