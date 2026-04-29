package io.github.atengk.utils;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.Temporal;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Java Bean 基础反射工具类。
 *
 * @author Ateng
 * @since 2026-04-29
 */
public final class BeanUtil {

    private static final Map<Class<?>, Class<?>> PRIMITIVE_WRAPPER_MAP = Map.of(
            boolean.class, Boolean.class,
            byte.class, Byte.class,
            char.class, Character.class,
            short.class, Short.class,
            int.class, Integer.class,
            long.class, Long.class,
            float.class, Float.class,
            double.class, Double.class,
            void.class, Void.class
    );

    private static final Map<Class<?>, Class<?>> WRAPPER_PRIMITIVE_MAP = createWrapperPrimitiveMap();

    private static final ClassValue<Map<String, Field>> FIELD_CACHE = new ClassValue<>() {
        @Override
        protected Map<String, Field> computeValue(Class<?> type) {
            Map<String, Field> fieldMap = new LinkedHashMap<>();
            Class<?> currentClass = type;
            while (currentClass != null && currentClass != Object.class) {
                for (Field field : currentClass.getDeclaredFields()) {
                    if (isBeanField(field)) {
                        fieldMap.putIfAbsent(field.getName(), field);
                    }
                }
                currentClass = currentClass.getSuperclass();
            }
            return Collections.unmodifiableMap(fieldMap);
        }
    };

    private static final ClassValue<Map<String, PropertyDescriptor>> PROPERTY_DESCRIPTOR_CACHE = new ClassValue<>() {
        @Override
        protected Map<String, PropertyDescriptor> computeValue(Class<?> type) {
            Map<String, PropertyDescriptor> descriptorMap = new LinkedHashMap<>();
            try {
                BeanInfo beanInfo = Introspector.getBeanInfo(type, Object.class);
                for (PropertyDescriptor descriptor : beanInfo.getPropertyDescriptors()) {
                    descriptorMap.put(descriptor.getName(), descriptor);
                }
                if (type.isRecord()) {
                    for (RecordComponent component : type.getRecordComponents()) {
                        Method accessor = component.getAccessor();
                        descriptorMap.putIfAbsent(component.getName(), new PropertyDescriptor(component.getName(), accessor, null));
                    }
                }
                return Collections.unmodifiableMap(descriptorMap);
            } catch (IntrospectionException e) {
                throw new BeanException("解析Bean属性描述失败：" + type.getName(), e);
            }
        }
    };

    private BeanUtil() {
        throw new UnsupportedOperationException("工具类不允许实例化");
    }

    /**
     * 使用无参构造方法创建实例。
     *
     * @param type 实例类型
     * @return 新实例
     */
    public static <T> T newInstance(Class<T> type) {
        requireClass(type, "type");
        return newInstance(type, new Class<?>[0], new Object[0]);
    }

    /**
     * 使用指定构造方法创建实例。
     *
     * @param type           实例类型
     * @param parameterTypes 构造方法参数类型
     * @param args           构造方法参数值
     * @return 新实例
     */
    public static <T> T newInstance(Class<T> type, Class<?>[] parameterTypes, Object... args) {
        requireClass(type, "type");
        Class<?>[] actualParameterTypes = parameterTypes == null ? new Class<?>[0] : parameterTypes;
        Object[] actualArgs = args == null ? new Object[0] : args;
        if (actualParameterTypes.length != actualArgs.length) {
            throw new IllegalArgumentException("构造方法参数类型数量与参数值数量不一致");
        }
        try {
            Constructor<T> constructor = type.getDeclaredConstructor(actualParameterTypes);
            makeAccessible(constructor);
            return constructor.newInstance(actualArgs);
        } catch (NoSuchMethodException e) {
            throw new BeanException("未找到匹配的构造方法：" + type.getName(), e);
        } catch (InstantiationException e) {
            throw new BeanException("类型无法实例化：" + type.getName(), e);
        } catch (IllegalAccessException e) {
            throw new BeanException("构造方法不可访问：" + type.getName(), e);
        } catch (InvocationTargetException e) {
            throw new BeanException("构造方法执行失败：" + type.getName(), unwrapInvocationTargetException(e));
        }
    }

    /**
     * 判断类型是否适合按普通 Bean 处理。
     *
     * @param type 类型
     * @return 是否为 Bean 类型
     */
    public static boolean isBeanClass(Class<?> type) {
        return type != null
                && !type.isPrimitive()
                && !type.isArray()
                && !type.isEnum()
                && !type.isAnnotation()
                && !type.isInterface()
                && !isSimpleValueType(type);
    }

    /**
     * 判断类型是否为简单值类型。
     *
     * @param type 类型
     * @return 是否为简单值类型
     */
    public static boolean isSimpleValueType(Class<?> type) {
        if (type == null) {
            return false;
        }
        return type.isPrimitive()
                || type.isEnum()
                || CharSequence.class.isAssignableFrom(type)
                || Number.class.isAssignableFrom(type)
                || Boolean.class == type
                || Character.class == type
                || Date.class.isAssignableFrom(type)
                || Calendar.class.isAssignableFrom(type)
                || Temporal.class.isAssignableFrom(type)
                || BigDecimal.class == type
                || BigInteger.class == type
                || URI.class == type
                || URL.class == type
                || UUID.class == type;
    }

    /**
     * 获取 Bean 字段列表，包含父类字段，不包含静态字段和合成字段。
     *
     * @param beanClass Bean 类型
     * @return 字段列表
     */
    public static List<Field> getFields(Class<?> beanClass) {
        requireClass(beanClass, "beanClass");
        return new ArrayList<>(FIELD_CACHE.get(beanClass).values());
    }

    /**
     * 查找字段。
     *
     * @param beanClass Bean 类型
     * @param fieldName 字段名
     * @return 字段对象
     */
    public static Optional<Field> findField(Class<?> beanClass, String fieldName) {
        requireClass(beanClass, "beanClass");
        requireText(fieldName, "fieldName");
        return Optional.ofNullable(FIELD_CACHE.get(beanClass).get(fieldName));
    }

    /**
     * 判断字段是否存在。
     *
     * @param beanClass Bean 类型
     * @param fieldName 字段名
     * @return 是否存在
     */
    public static boolean hasField(Class<?> beanClass, String fieldName) {
        return findField(beanClass, fieldName).isPresent();
    }

    /**
     * 获取字段值。
     *
     * @param bean      Bean 对象
     * @param fieldName 字段名
     * @return 字段值
     */
    public static Object getFieldValue(Object bean, String fieldName) {
        Objects.requireNonNull(bean, "bean不能为null");
        Field field = requireField(bean.getClass(), fieldName);
        try {
            makeAccessible(field);
            return field.get(bean);
        } catch (IllegalAccessException e) {
            throw new BeanException("读取字段失败：" + fieldName, e);
        }
    }

    /**
     * 设置字段值。
     *
     * @param bean      Bean 对象
     * @param fieldName 字段名
     * @param value     字段值
     */
    public static void setFieldValue(Object bean, String fieldName, Object value) {
        Objects.requireNonNull(bean, "bean不能为null");
        Field field = requireField(bean.getClass(), fieldName);
        if (Modifier.isFinal(field.getModifiers())) {
            throw new BeanException("final字段不允许写入：" + fieldName);
        }
        if (!isAssignableValue(field.getType(), value)) {
            throw new BeanException("字段值类型不匹配：" + fieldName + "，目标类型=" + field.getType().getName());
        }
        try {
            makeAccessible(field);
            field.set(bean, value);
        } catch (IllegalAccessException e) {
            throw new BeanException("写入字段失败：" + fieldName, e);
        }
    }

    /**
     * 获取属性名列表。
     *
     * @param beanClass Bean 类型
     * @return 属性名列表
     */
    public static List<String> getPropertyNames(Class<?> beanClass) {
        requireClass(beanClass, "beanClass");
        return new ArrayList<>(PROPERTY_DESCRIPTOR_CACHE.get(beanClass).keySet());
    }

    /**
     * 获取属性类型映射。
     *
     * @param beanClass Bean 类型
     * @return 属性类型映射
     */
    public static Map<String, Class<?>> getPropertyTypes(Class<?> beanClass) {
        requireClass(beanClass, "beanClass");
        Map<String, Class<?>> propertyTypes = new LinkedHashMap<>();
        PROPERTY_DESCRIPTOR_CACHE.get(beanClass).forEach((name, descriptor) -> propertyTypes.put(name, descriptor.getPropertyType()));
        return Collections.unmodifiableMap(propertyTypes);
    }

    /**
     * 判断属性是否存在。
     *
     * @param beanClass    Bean 类型
     * @param propertyName 属性名
     * @return 是否存在
     */
    public static boolean hasProperty(Class<?> beanClass, String propertyName) {
        return findPropertyDescriptor(beanClass, propertyName).isPresent() || hasField(beanClass, propertyName);
    }

    /**
     * 判断属性是否可读。
     *
     * @param beanClass    Bean 类型
     * @param propertyName 属性名
     * @return 是否可读
     */
    public static boolean isReadableProperty(Class<?> beanClass, String propertyName) {
        Optional<PropertyDescriptor> descriptor = findPropertyDescriptor(beanClass, propertyName);
        return descriptor.map(PropertyDescriptor::getReadMethod).isPresent() || hasField(beanClass, propertyName);
    }

    /**
     * 判断属性是否可写。
     *
     * @param beanClass    Bean 类型
     * @param propertyName 属性名
     * @return 是否可写
     */
    public static boolean isWritableProperty(Class<?> beanClass, String propertyName) {
        Optional<PropertyDescriptor> descriptor = findPropertyDescriptor(beanClass, propertyName);
        if (descriptor.map(PropertyDescriptor::getWriteMethod).isPresent()) {
            return true;
        }
        return findField(beanClass, propertyName)
                .map(field -> !Modifier.isFinal(field.getModifiers()))
                .orElse(false);
    }

    /**
     * 获取属性类型。
     *
     * @param beanClass    Bean 类型
     * @param propertyName 属性名
     * @return 属性类型
     */
    public static Class<?> getPropertyType(Class<?> beanClass, String propertyName) {
        Optional<PropertyDescriptor> descriptor = findPropertyDescriptor(beanClass, propertyName);
        if (descriptor.isPresent() && descriptor.get().getPropertyType() != null) {
            return descriptor.get().getPropertyType();
        }
        return requireField(beanClass, propertyName).getType();
    }

    /**
     * 获取属性值，优先调用 Getter，不存在 Getter 时回退到字段读取。
     *
     * @param bean         Bean 对象
     * @param propertyName 属性名
     * @return 属性值
     */
    public static Object getPropertyValue(Object bean, String propertyName) {
        Objects.requireNonNull(bean, "bean不能为null");
        requireText(propertyName, "propertyName");
        Optional<PropertyDescriptor> descriptor = findPropertyDescriptor(bean.getClass(), propertyName);
        if (descriptor.isPresent() && descriptor.get().getReadMethod() != null) {
            return invokeMethod(descriptor.get().getReadMethod(), bean);
        }
        return getFieldValue(bean, propertyName);
    }

    /**
     * 设置属性值，优先调用 Setter，不存在 Setter 时回退到字段写入。
     *
     * @param bean         Bean 对象
     * @param propertyName 属性名
     * @param value        属性值
     */
    public static void setPropertyValue(Object bean, String propertyName, Object value) {
        Objects.requireNonNull(bean, "bean不能为null");
        requireText(propertyName, "propertyName");
        Optional<PropertyDescriptor> descriptor = findPropertyDescriptor(bean.getClass(), propertyName);
        if (descriptor.isPresent() && descriptor.get().getWriteMethod() != null) {
            Method writeMethod = descriptor.get().getWriteMethod();
            Class<?> targetType = writeMethod.getParameterTypes()[0];
            if (!isAssignableValue(targetType, value)) {
                throw new BeanException("属性值类型不匹配：" + propertyName + "，目标类型=" + targetType.getName());
            }
            invokeMethod(writeMethod, bean, value);
            return;
        }
        setFieldValue(bean, propertyName, value);
    }

    /**
     * 判断方法是否为 Getter。
     *
     * @param method 方法
     * @return 是否为 Getter
     */
    public static boolean isGetter(Method method) {
        if (method == null || method.getParameterCount() != 0 || method.getReturnType() == void.class || Modifier.isStatic(method.getModifiers())) {
            return false;
        }
        String name = method.getName();
        if (name.startsWith("get") && name.length() > 3) {
            return true;
        }
        return name.startsWith("is") && name.length() > 2 && (method.getReturnType() == boolean.class || method.getReturnType() == Boolean.class);
    }

    /**
     * 判断方法是否为 Setter。
     *
     * @param method 方法
     * @return 是否为 Setter
     */
    public static boolean isSetter(Method method) {
        return method != null
                && method.getName().startsWith("set")
                && method.getName().length() > 3
                && method.getParameterCount() == 1
                && method.getReturnType() == void.class
                && !Modifier.isStatic(method.getModifiers());
    }

    /**
     * 根据 Getter 或 Setter 方法解析属性名。
     *
     * @param method Getter 或 Setter 方法
     * @return 属性名
     */
    public static String getPropertyName(Method method) {
        Objects.requireNonNull(method, "method不能为null");
        String methodName = method.getName();
        if (isGetter(method)) {
            if (methodName.startsWith("is")) {
                return Introspector.decapitalize(methodName.substring(2));
            }
            return Introspector.decapitalize(methodName.substring(3));
        }
        if (isSetter(method)) {
            return Introspector.decapitalize(methodName.substring(3));
        }
        throw new IllegalArgumentException("方法不是标准Getter或Setter：" + methodName);
    }

    /**
     * 判断值是否可以赋给目标类型。
     *
     * @param targetType 目标类型
     * @param value      值
     * @return 是否可赋值
     */
    public static boolean isAssignableValue(Class<?> targetType, Object value) {
        requireClass(targetType, "targetType");
        if (value == null) {
            return !targetType.isPrimitive();
        }
        Class<?> actualTargetType = targetType.isPrimitive() ? wrapPrimitive(targetType) : targetType;
        return actualTargetType.isAssignableFrom(value.getClass());
    }

    /**
     * 包装基本类型。
     *
     * @param type 类型
     * @return 包装后的类型
     */
    public static Class<?> wrapPrimitive(Class<?> type) {
        requireClass(type, "type");
        return type.isPrimitive() ? PRIMITIVE_WRAPPER_MAP.get(type) : type;
    }

    /**
     * 拆箱包装类型。
     *
     * @param type 类型
     * @return 拆箱后的类型
     */
    public static Class<?> unwrapWrapper(Class<?> type) {
        requireClass(type, "type");
        return WRAPPER_PRIMITIVE_MAP.getOrDefault(type, type);
    }

    /**
     * 首字母小写，遵循 JavaBeans 规范。
     *
     * @param name 名称
     * @return 转换后的名称
     */
    public static String decapitalize(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        return Introspector.decapitalize(name);
    }

    /**
     * 清除指定类型的反射缓存。
     *
     * @param beanClass Bean 类型
     */
    public static void clearCache(Class<?> beanClass) {
        requireClass(beanClass, "beanClass");
        FIELD_CACHE.remove(beanClass);
        PROPERTY_DESCRIPTOR_CACHE.remove(beanClass);
        Introspector.flushFromCaches(beanClass);
    }

    private static Field requireField(Class<?> beanClass, String fieldName) {
        return findField(beanClass, fieldName)
                .orElseThrow(() -> new BeanException("未找到字段：" + beanClass.getName() + "." + fieldName));
    }

    private static Object invokeMethod(Method method, Object target, Object... args) {
        try {
            makeAccessible(method);
            return method.invoke(target, args);
        } catch (IllegalAccessException e) {
            throw new BeanException("方法不可访问：" + method.getName(), e);
        } catch (InvocationTargetException e) {
            throw new BeanException("方法执行失败：" + method.getName(), unwrapInvocationTargetException(e));
        }
    }

    private static void makeAccessible(AccessibleObject accessibleObject) {
        try {
            if (!accessibleObject.trySetAccessible()) {
                throw new BeanException("成员不可访问：" + accessibleObject);
            }
        } catch (SecurityException e) {
            throw new BeanException("成员访问被拒绝：" + accessibleObject, e);
        }
    }

    private static boolean isBeanField(Field field) {
        int modifiers = field.getModifiers();
        return !field.isSynthetic() && !Modifier.isStatic(modifiers);
    }

    private static void requireClass(Class<?> type, String name) {
        if (type == null) {
            throw new IllegalArgumentException(name + "不能为null");
        }
    }

    private static void requireText(String text, String name) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException(name + "不能为空");
        }
    }

    private static Throwable unwrapInvocationTargetException(InvocationTargetException e) {
        return e.getTargetException() == null ? e : e.getTargetException();
    }

    private static Map<Class<?>, Class<?>> createWrapperPrimitiveMap() {
        Map<Class<?>, Class<?>> map = new LinkedHashMap<>();
        PRIMITIVE_WRAPPER_MAP.forEach((primitiveType, wrapperType) -> map.put(wrapperType, primitiveType));
        return Collections.unmodifiableMap(map);
    }

    /**
     * Bean 工具异常。
     *
     * @author Ateng
     * @since 2026-04-29
     */
    public static class BeanException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        /**
         * 创建异常。
         *
         * @param message 异常消息
         */
        public BeanException(String message) {
            super(message);
        }

        /**
         * 创建异常。
         *
         * @param message 异常消息
         * @param cause   原始异常
         */
        public BeanException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private static <T> Optional<Constructor<T>> findCompatibleConstructor(Class<T> type, Object[] args) {
        Constructor<?>[] constructors = type.getDeclaredConstructors();

        for (Constructor<?> constructor : constructors) {
            if (isCompatibleConstructor(constructor, args)) {
                @SuppressWarnings("unchecked")
                Constructor<T> matchedConstructor = (Constructor<T>) constructor;
                return Optional.of(matchedConstructor);
            }
        }

        return Optional.empty();
    }

    private static boolean isCompatibleConstructor(Constructor<?> constructor, Object[] args) {
        Class<?>[] parameterTypes = constructor.getParameterTypes();
        if (parameterTypes.length != args.length) {
            return false;
        }

        for (int i = 0; i < parameterTypes.length; i++) {
            if (!isAssignableValue(parameterTypes[i], args[i])) {
                return false;
            }
        }

        return true;
    }

    private static void assertInstantiable(Class<?> type) {
        if (!isInstantiable(type)) {
            throw new BeanException("类型不支持实例化：" + type.getName());
        }
    }

    private static String getValueTypeName(Object value) {
        return value == null ? "null" : value.getClass().getName();
    }

    /**
     * 根据参数值自动匹配构造方法创建实例。
     *
     * @param type 实例类型
     * @param args 构造方法参数值
     * @return 新实例
     */
    public static <T> T newInstance(Class<T> type, Object... args) {
        requireClass(type, "type");
        assertInstantiable(type);

        Object[] actualArgs = args == null ? new Object[0] : args;
        Constructor<T> constructor = findCompatibleConstructor(type, actualArgs)
                .orElseThrow(() -> new BeanException("未找到兼容的构造方法：" + type.getName()));

        try {
            makeAccessible(constructor);
            return constructor.newInstance(actualArgs);
        } catch (InstantiationException e) {
            throw new BeanException("类型无法实例化：" + type.getName(), e);
        } catch (IllegalAccessException e) {
            throw new BeanException("构造方法不可访问：" + type.getName(), e);
        } catch (InvocationTargetException e) {
            throw new BeanException("构造方法执行失败：" + type.getName(), unwrapInvocationTargetException(e));
        }
    }

    /**
     * 尝试使用无参构造方法创建实例。
     *
     * @param type 实例类型
     * @return 创建成功返回实例，失败返回空
     */
    public static <T> Optional<T> tryNewInstance(Class<T> type) {
        if (type == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(newInstance(type));
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    /**
     * 判断类型是否可以被实例化。
     *
     * @param type 类型
     * @return 是否可以实例化
     */
    public static boolean isInstantiable(Class<?> type) {
        if (type == null) {
            return false;
        }
        int modifiers = type.getModifiers();
        return !type.isPrimitive()
                && !type.isArray()
                && !type.isInterface()
                && !type.isAnnotation()
                && !type.isEnum()
                && !Modifier.isAbstract(modifiers);
    }

    /**
     * 获取当前类声明的构造方法。
     *
     * @param type 类型
     * @return 构造方法列表
     */
    public static List<Constructor<?>> getConstructors(Class<?> type) {
        requireClass(type, "type");
        return List.of(type.getDeclaredConstructors());
    }

    /**
     * 获取属性值并校验返回类型。
     *
     * @param bean         Bean 对象
     * @param propertyName 属性名
     * @param targetType   目标类型
     * @return 属性值
     */
    public static <T> T getPropertyValue(Object bean, String propertyName, Class<T> targetType) {
        requireClass(targetType, "targetType");

        Object value = getPropertyValue(bean, propertyName);
        if (value == null) {
            return null;
        }
        if (!targetType.isInstance(value)) {
            throw new BeanException("属性值类型不匹配：" + propertyName
                    + "，期望类型=" + targetType.getName()
                    + "，实际类型=" + value.getClass().getName());
        }
        return targetType.cast(value);
    }

    /**
     * 获取属性值，结果为空时返回默认值。
     *
     * @param bean         Bean 对象
     * @param propertyName 属性名
     * @param defaultValue 默认值
     * @return 属性值或默认值
     */
    public static <T> T getPropertyValueOrDefault(Object bean, String propertyName, T defaultValue) {
        Object value = getPropertyValue(bean, propertyName);
        if (value == null) {
            return defaultValue;
        }

        try {
            @SuppressWarnings("unchecked")
            T result = (T) value;
            return result;
        } catch (ClassCastException e) {
            throw new BeanException("属性值类型与默认值类型不匹配：" + propertyName, e);
        }
    }

    /**
     * 批量读取所有可读属性。
     *
     * @param bean Bean 对象
     * @return 属性值映射
     */
    public static Map<String, Object> getReadablePropertyValues(Object bean) {
        Objects.requireNonNull(bean, "bean不能为null");

        Map<String, Object> result = new LinkedHashMap<>();
        Map<String, PropertyDescriptor> descriptorMap = PROPERTY_DESCRIPTOR_CACHE.get(bean.getClass());

        for (Map.Entry<String, PropertyDescriptor> entry : descriptorMap.entrySet()) {
            String propertyName = entry.getKey();
            Method readMethod = entry.getValue().getReadMethod();
            if (readMethod != null) {
                result.put(propertyName, invokeMethod(readMethod, bean));
            }
        }

        return Collections.unmodifiableMap(result);
    }

    /**
     * 属性存在时写入属性值，不存在时忽略。
     *
     * @param bean         Bean 对象
     * @param propertyName 属性名
     * @param value        属性值
     * @return 是否完成写入
     */
    public static boolean setPropertyValueIfPresent(Object bean, String propertyName, Object value) {
        Objects.requireNonNull(bean, "bean不能为null");
        requireText(propertyName, "propertyName");

        if (!hasProperty(bean.getClass(), propertyName)) {
            return false;
        }

        setPropertyValue(bean, propertyName, value);
        return true;
    }

    /**
     * 批量设置属性值。
     *
     * @param bean           Bean 对象
     * @param propertyValues 属性值映射
     */
    public static void setPropertyValues(Object bean, Map<String, ?> propertyValues) {
        setPropertyValues(bean, propertyValues, false, false);
    }

    /**
     * 批量设置属性值。
     *
     * @param bean           Bean 对象
     * @param propertyValues 属性值映射
     * @param ignoreUnknown  是否忽略不存在的属性
     * @param ignoreNull     是否忽略空值
     */
    public static void setPropertyValues(Object bean, Map<String, ?> propertyValues, boolean ignoreUnknown, boolean ignoreNull) {
        Objects.requireNonNull(bean, "bean不能为null");

        if (propertyValues == null || propertyValues.isEmpty()) {
            return;
        }

        for (Map.Entry<String, ?> entry : propertyValues.entrySet()) {
            String propertyName = entry.getKey();
            Object value = entry.getValue();

            if (propertyName == null || propertyName.isBlank()) {
                continue;
            }
            if (ignoreNull && value == null) {
                continue;
            }
            if (!hasProperty(bean.getClass(), propertyName)) {
                if (ignoreUnknown) {
                    continue;
                }
                throw new BeanException("属性不存在：" + bean.getClass().getName() + "." + propertyName);
            }

            setPropertyValue(bean, propertyName, value);
        }
    }

    /**
     * 获取 Bean 字段映射，Key 为字段名。
     *
     * @param beanClass Bean 类型
     * @return 字段映射
     */
    public static Map<String, Field> getFieldMap(Class<?> beanClass) {
        requireClass(beanClass, "beanClass");
        return FIELD_CACHE.get(beanClass);
    }

    /**
     * 获取字段名列表。
     *
     * @param beanClass Bean 类型
     * @return 字段名列表
     */
    public static List<String> getFieldNames(Class<?> beanClass) {
        requireClass(beanClass, "beanClass");
        return new ArrayList<>(FIELD_CACHE.get(beanClass).keySet());
    }

    /**
     * 获取字段类型。
     *
     * @param beanClass Bean 类型
     * @param fieldName 字段名
     * @return 字段类型
     */
    public static Class<?> getFieldType(Class<?> beanClass, String fieldName) {
        return requireField(beanClass, fieldName).getType();
    }

    /**
     * 获取属性描述列表。
     *
     * @param beanClass Bean 类型
     * @return 属性描述列表
     */
    public static List<PropertyDescriptor> getPropertyDescriptors(Class<?> beanClass) {
        requireClass(beanClass, "beanClass");
        return new ArrayList<>(PROPERTY_DESCRIPTOR_CACHE.get(beanClass).values());
    }

    /**
     * 获取属性描述映射，Key 为属性名。
     *
     * @param beanClass Bean 类型
     * @return 属性描述映射
     */
    public static Map<String, PropertyDescriptor> getPropertyDescriptorMap(Class<?> beanClass) {
        requireClass(beanClass, "beanClass");
        return PROPERTY_DESCRIPTOR_CACHE.get(beanClass);
    }

    /**
     * 查找属性描述。
     *
     * @param beanClass    Bean 类型
     * @param propertyName 属性名
     * @return 属性描述
     */
    public static Optional<PropertyDescriptor> findPropertyDescriptor(Class<?> beanClass, String propertyName) {
        requireClass(beanClass, "beanClass");
        requireText(propertyName, "propertyName");
        return Optional.ofNullable(PROPERTY_DESCRIPTOR_CACHE.get(beanClass).get(propertyName));
    }

    /**
     * 获取可读属性名列表。
     *
     * @param beanClass Bean 类型
     * @return 可读属性名列表
     */
    public static List<String> getReadablePropertyNames(Class<?> beanClass) {
        requireClass(beanClass, "beanClass");

        List<String> propertyNames = new ArrayList<>();
        getBeanPropertyMap(beanClass).forEach((name, property) -> {
            if (property.readable()) {
                propertyNames.add(name);
            }
        });
        return propertyNames;
    }

    /**
     * 获取可写属性名列表。
     *
     * @param beanClass Bean 类型
     * @return 可写属性名列表
     */
    public static List<String> getWritablePropertyNames(Class<?> beanClass) {
        requireClass(beanClass, "beanClass");

        List<String> propertyNames = new ArrayList<>();
        getBeanPropertyMap(beanClass).forEach((name, property) -> {
            if (property.writable()) {
                propertyNames.add(name);
            }
        });
        return propertyNames;
    }

    /**
     * 获取属性读取方法。
     *
     * @param beanClass    Bean 类型
     * @param propertyName 属性名
     * @return 读取方法
     */
    public static Optional<Method> getReadMethod(Class<?> beanClass, String propertyName) {
        return findPropertyDescriptor(beanClass, propertyName)
                .map(PropertyDescriptor::getReadMethod);
    }

    /**
     * 获取属性写入方法。
     *
     * @param beanClass    Bean 类型
     * @param propertyName 属性名
     * @return 写入方法
     */
    public static Optional<Method> getWriteMethod(Class<?> beanClass, String propertyName) {
        return findPropertyDescriptor(beanClass, propertyName)
                .map(PropertyDescriptor::getWriteMethod);
    }

    /**
     * 获取 Getter 方法映射，Key 为属性名。
     *
     * @param beanClass Bean 类型
     * @return Getter 方法映射
     */
    public static Map<String, Method> getGetterMethodMap(Class<?> beanClass) {
        requireClass(beanClass, "beanClass");

        Map<String, Method> methodMap = new LinkedHashMap<>();
        PROPERTY_DESCRIPTOR_CACHE.get(beanClass).forEach((name, descriptor) -> {
            Method readMethod = descriptor.getReadMethod();
            if (readMethod != null) {
                methodMap.put(name, readMethod);
            }
        });
        return Collections.unmodifiableMap(methodMap);
    }

    /**
     * 获取 Setter 方法映射，Key 为属性名。
     *
     * @param beanClass Bean 类型
     * @return Setter 方法映射
     */
    public static Map<String, Method> getSetterMethodMap(Class<?> beanClass) {
        requireClass(beanClass, "beanClass");

        Map<String, Method> methodMap = new LinkedHashMap<>();
        PROPERTY_DESCRIPTOR_CACHE.get(beanClass).forEach((name, descriptor) -> {
            Method writeMethod = descriptor.getWriteMethod();
            if (writeMethod != null) {
                methodMap.put(name, writeMethod);
            }
        });
        return Collections.unmodifiableMap(methodMap);
    }

    /**
     * 获取 Bean 属性元信息。
     *
     * @param beanClass    Bean 类型
     * @param propertyName 属性名
     * @return 属性元信息
     */
    public static BeanProperty getBeanProperty(Class<?> beanClass, String propertyName) {
        requireClass(beanClass, "beanClass");
        requireText(propertyName, "propertyName");

        BeanProperty property = getBeanPropertyMap(beanClass).get(propertyName);
        if (property == null) {
            throw new BeanException("属性不存在：" + beanClass.getName() + "." + propertyName);
        }
        return property;
    }

    /**
     * 获取 Bean 属性元信息映射，Key 为属性名。
     *
     * @param beanClass Bean 类型
     * @return 属性元信息映射
     */
    public static Map<String, BeanProperty> getBeanPropertyMap(Class<?> beanClass) {
        requireClass(beanClass, "beanClass");

        Map<String, Field> fieldMap = FIELD_CACHE.get(beanClass);
        Map<String, PropertyDescriptor> descriptorMap = PROPERTY_DESCRIPTOR_CACHE.get(beanClass);
        Map<String, BeanProperty> propertyMap = new LinkedHashMap<>();

        descriptorMap.forEach((name, descriptor) -> {
            Field field = fieldMap.get(name);
            Method readMethod = descriptor.getReadMethod();
            Method writeMethod = descriptor.getWriteMethod();
            Class<?> type = resolvePropertyType(field, descriptor, readMethod, writeMethod);
            boolean writable = writeMethod != null || (field != null && !Modifier.isFinal(field.getModifiers()));
            boolean readable = readMethod != null || field != null;
            propertyMap.put(name, new BeanProperty(name, type, field, readMethod, writeMethod, readable, writable));
        });

        fieldMap.forEach((name, field) -> propertyMap.putIfAbsent(name, new BeanProperty(
                name,
                field.getType(),
                field,
                null,
                null,
                true,
                !Modifier.isFinal(field.getModifiers())
        )));

        return Collections.unmodifiableMap(propertyMap);
    }

    /**
     * 判断字段是否为可读字段。
     *
     * @param field 字段
     * @return 是否可读
     */
    public static boolean isReadableField(Field field) {
        return field != null && isBeanField(field);
    }

    /**
     * 判断字段是否为可写字段。
     *
     * @param field 字段
     * @return 是否可写
     */
    public static boolean isWritableField(Field field) {
        return field != null && isBeanField(field) && !Modifier.isFinal(field.getModifiers());
    }

    /**
     * 判断类型是否包含无参构造方法。
     *
     * @param type 类型
     * @return 是否包含无参构造方法
     */
    public static boolean hasNoArgsConstructor(Class<?> type) {
        if (type == null) {
            return false;
        }
        try {
            type.getDeclaredConstructor();
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    /**
     * 判断类型是否为 Java Record。
     *
     * @param type 类型
     * @return 是否为 Record
     */
    public static boolean isRecordClass(Class<?> type) {
        return type != null && type.isRecord();
    }

    private static Class<?> resolvePropertyType(Field field, PropertyDescriptor descriptor, Method readMethod, Method writeMethod) {
        if (descriptor != null && descriptor.getPropertyType() != null) {
            return descriptor.getPropertyType();
        }
        if (field != null) {
            return field.getType();
        }
        if (readMethod != null) {
            return readMethod.getReturnType();
        }
        if (writeMethod != null && writeMethod.getParameterCount() == 1) {
            return writeMethod.getParameterTypes()[0];
        }
        return null;
    }

    /**
     * Bean 属性元信息。
     *
     * @param name        属性名
     * @param type        属性类型
     * @param field       字段
     * @param readMethod  读取方法
     * @param writeMethod 写入方法
     * @param readable    是否可读
     * @param writable    是否可写
     * @author Ateng
     * @since 2026-04-29
     */
    public record BeanProperty(
            String name,
            Class<?> type,
            Field field,
            Method readMethod,
            Method writeMethod,
            boolean readable,
            boolean writable
    ) {
    }

    /**
     * Bean 转 Map，默认保留空值。
     *
     * @param bean Bean 对象
     * @return Map 数据
     */
    public static Map<String, Object> toMap(Object bean) {
        return toMap(bean, BeanToMapOptions.defaults());
    }

    /**
     * Bean 转 Map。
     *
     * @param bean       Bean 对象
     * @param ignoreNull 是否忽略空值
     * @return Map 数据
     */
    public static Map<String, Object> toMap(Object bean, boolean ignoreNull) {
        return toMap(bean, new BeanToMapOptions(ignoreNull, Collections.emptyList(), Collections.emptyList()));
    }

    /**
     * Bean 转 Map。
     *
     * @param bean              Bean 对象
     * @param excludeProperties 排除的属性名集合
     * @return Map 数据
     */
    public static Map<String, Object> toMap(Object bean, Collection<String> excludeProperties) {
        return toMap(bean, new BeanToMapOptions(false, Collections.emptyList(), excludeProperties));
    }

    /**
     * Bean 转 Map。
     *
     * @param bean              Bean 对象
     * @param ignoreNull        是否忽略空值
     * @param includeProperties 仅包含的属性名集合，为空表示不过滤
     * @param excludeProperties 排除的属性名集合
     * @return Map 数据
     */
    public static Map<String, Object> toMap(Object bean, boolean ignoreNull, Collection<String> includeProperties, Collection<String> excludeProperties) {
        return toMap(bean, new BeanToMapOptions(ignoreNull, includeProperties, excludeProperties));
    }

    /**
     * Bean 转 Map。
     *
     * @param bean    Bean 对象
     * @param options 转换配置
     * @return Map 数据
     */
    public static Map<String, Object> toMap(Object bean, BeanToMapOptions options) {
        Objects.requireNonNull(bean, "bean不能为null");

        BeanToMapOptions actualOptions = options == null ? BeanToMapOptions.defaults() : options;
        Map<String, Object> result = new LinkedHashMap<>();
        Map<String, BeanProperty> propertyMap = getBeanPropertyMap(bean.getClass());

        for (Map.Entry<String, BeanProperty> entry : propertyMap.entrySet()) {
            String propertyName = entry.getKey();
            BeanProperty property = entry.getValue();

            if (!property.readable()) {
                continue;
            }
            if (!shouldIncludeProperty(propertyName, actualOptions.includeProperties(), actualOptions.excludeProperties())) {
                continue;
            }

            Object value = getPropertyValue(bean, propertyName);
            if (actualOptions.ignoreNull() && value == null) {
                continue;
            }

            result.put(propertyName, value);
        }

        return result;
    }

    /**
     * Map 转 Bean，默认不忽略未知属性。
     *
     * @param sourceMap  源 Map
     * @param targetType 目标类型
     * @return Bean 对象
     */
    public static <T> T toBean(Map<String, ?> sourceMap, Class<T> targetType) {
        return toBean(sourceMap, targetType, MapToBeanOptions.defaults());
    }

    /**
     * Map 转 Bean。
     *
     * @param sourceMap     源 Map
     * @param targetType    目标类型
     * @param ignoreUnknown 是否忽略未知属性
     * @return Bean 对象
     */
    public static <T> T toBean(Map<String, ?> sourceMap, Class<T> targetType, boolean ignoreUnknown) {
        return toBean(sourceMap, targetType, new MapToBeanOptions(ignoreUnknown, false, Collections.emptyList(), Collections.emptyList()));
    }

    /**
     * Map 转 Bean。
     *
     * @param sourceMap  源 Map
     * @param targetType 目标类型
     * @param options    转换配置
     * @return Bean 对象
     */
    public static <T> T toBean(Map<String, ?> sourceMap, Class<T> targetType, MapToBeanOptions options) {
        requireClass(targetType, "targetType");

        T targetBean = newInstance(targetType);
        fillBean(sourceMap, targetBean, options);
        return targetBean;
    }

    /**
     * 使用 Map 填充 Bean，默认不忽略未知属性。
     *
     * @param sourceMap  源 Map
     * @param targetBean 目标 Bean
     */
    public static void fillBean(Map<String, ?> sourceMap, Object targetBean) {
        fillBean(sourceMap, targetBean, MapToBeanOptions.defaults());
    }

    /**
     * 使用 Map 填充 Bean。
     *
     * @param sourceMap     源 Map
     * @param targetBean    目标 Bean
     * @param ignoreUnknown 是否忽略未知属性
     * @param ignoreNull    是否忽略空值
     */
    public static void fillBean(Map<String, ?> sourceMap, Object targetBean, boolean ignoreUnknown, boolean ignoreNull) {
        fillBean(sourceMap, targetBean, new MapToBeanOptions(ignoreUnknown, ignoreNull, Collections.emptyList(), Collections.emptyList()));
    }

    /**
     * 使用 Map 填充 Bean。
     *
     * @param sourceMap  源 Map
     * @param targetBean 目标 Bean
     * @param options    转换配置
     */
    public static void fillBean(Map<String, ?> sourceMap, Object targetBean, MapToBeanOptions options) {
        Objects.requireNonNull(targetBean, "targetBean不能为null");

        if (sourceMap == null || sourceMap.isEmpty()) {
            return;
        }

        MapToBeanOptions actualOptions = options == null ? MapToBeanOptions.defaults() : options;
        Class<?> targetClass = targetBean.getClass();

        for (Map.Entry<String, ?> entry : sourceMap.entrySet()) {
            String propertyName = entry.getKey();
            Object value = entry.getValue();

            if (propertyName == null || propertyName.isBlank()) {
                continue;
            }
            if (actualOptions.ignoreNull() && value == null) {
                continue;
            }
            if (!shouldIncludeProperty(propertyName, actualOptions.includeProperties(), actualOptions.excludeProperties())) {
                continue;
            }
            if (!hasProperty(targetClass, propertyName)) {
                if (actualOptions.ignoreUnknown()) {
                    continue;
                }
                throw new BeanException("属性不存在：" + targetClass.getName() + "." + propertyName);
            }
            if (!isWritableProperty(targetClass, propertyName)) {
                if (actualOptions.ignoreUnknown()) {
                    continue;
                }
                throw new BeanException("属性不可写：" + targetClass.getName() + "." + propertyName);
            }

            setPropertyValue(targetBean, propertyName, value);
        }
    }

    /**
     * Map 转 Bean，方法别名。
     *
     * @param sourceMap  源 Map
     * @param targetType 目标类型
     * @return Bean 对象
     */
    public static <T> T mapToBean(Map<String, ?> sourceMap, Class<T> targetType) {
        return toBean(sourceMap, targetType);
    }

    /**
     * Bean 转 Map，方法别名。
     *
     * @param bean Bean 对象
     * @return Map 数据
     */
    public static Map<String, Object> beanToMap(Object bean) {
        return toMap(bean);
    }

    private static boolean shouldIncludeProperty(String propertyName, Collection<String> includeProperties, Collection<String> excludeProperties) {
        Collection<String> actualIncludeProperties = includeProperties == null ? Collections.emptyList() : includeProperties;
        Collection<String> actualExcludeProperties = excludeProperties == null ? Collections.emptyList() : excludeProperties;

        if (!actualIncludeProperties.isEmpty() && !actualIncludeProperties.contains(propertyName)) {
            return false;
        }

        return !actualExcludeProperties.contains(propertyName);
    }

    /**
     * Bean 转 Map 配置。
     *
     * @param ignoreNull        是否忽略空值
     * @param includeProperties 仅包含的属性名集合，为空表示不过滤
     * @param excludeProperties 排除的属性名集合
     * @author Ateng
     * @since 2026-04-29
     */
    public record BeanToMapOptions(
            boolean ignoreNull,
            Collection<String> includeProperties,
            Collection<String> excludeProperties
    ) {

        /**
         * 默认配置。
         *
         * @return Bean 转 Map 配置
         */
        public static BeanToMapOptions defaults() {
            return new BeanToMapOptions(false, Collections.emptyList(), Collections.emptyList());
        }

        /**
         * 忽略空值配置。
         *
         * @return Bean 转 Map 配置
         */
        public static BeanToMapOptions ofIgnoreNull() {
            return new BeanToMapOptions(true, Collections.emptyList(), Collections.emptyList());
        }
    }

    /**
     * Map 转 Bean 配置。
     *
     * @param ignoreUnknown     是否忽略未知属性
     * @param ignoreNull        是否忽略空值
     * @param includeProperties 仅包含的属性名集合，为空表示不过滤
     * @param excludeProperties 排除的属性名集合
     * @author Ateng
     * @since 2026-04-29
     */
    public record MapToBeanOptions(
            boolean ignoreUnknown,
            boolean ignoreNull,
            Collection<String> includeProperties,
            Collection<String> excludeProperties
    ) {

        /**
         * 默认配置。
         *
         * @return Map 转 Bean 配置
         */
        public static MapToBeanOptions defaults() {
            return new MapToBeanOptions(false, false, Collections.emptyList(), Collections.emptyList());
        }

        /**
         * 忽略未知属性配置。
         *
         * @return Map 转 Bean 配置
         */
        public static MapToBeanOptions ofIgnoreUnknown() {
            return new MapToBeanOptions(true, false, Collections.emptyList(), Collections.emptyList());
        }

        /**
         * 忽略未知属性和空值配置。
         *
         * @return Map 转 Bean 配置
         */
        public static MapToBeanOptions ignoreUnknownAndNull() {
            return new MapToBeanOptions(true, true, Collections.emptyList(), Collections.emptyList());
        }
    }

    private static final List<String> DEFAULT_DATE_TIME_PATTERNS = List.of(
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd HH:mm",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm",
            "yyyy-MM-dd"
    );

    /**
     * 拷贝 Bean 属性，默认不忽略空值。
     *
     * @param sourceBean 源 Bean
     * @param targetBean 目标 Bean
     */
    public static void copyProperties(Object sourceBean, Object targetBean) {
        copyProperties(sourceBean, targetBean, CopyOptions.defaults());
    }

    /**
     * 拷贝 Bean 属性。
     *
     * @param sourceBean 源 Bean
     * @param targetBean 目标 Bean
     * @param ignoreNull 是否忽略空值
     */
    public static void copyProperties(Object sourceBean, Object targetBean, boolean ignoreNull) {
        copyProperties(sourceBean, targetBean, new CopyOptions(ignoreNull, true, true, Collections.emptyList(), Collections.emptyList()));
    }

    /**
     * 拷贝 Bean 属性。
     *
     * @param sourceBean 源 Bean
     * @param targetBean 目标 Bean
     * @param options    拷贝配置
     */
    public static void copyProperties(Object sourceBean, Object targetBean, CopyOptions options) {
        Objects.requireNonNull(sourceBean, "sourceBean不能为null");
        Objects.requireNonNull(targetBean, "targetBean不能为null");

        CopyOptions actualOptions = options == null ? CopyOptions.defaults() : options;
        Class<?> sourceClass = sourceBean.getClass();
        Class<?> targetClass = targetBean.getClass();
        Map<String, BeanProperty> sourcePropertyMap = getBeanPropertyMap(sourceClass);
        Map<String, BeanProperty> targetPropertyMap = getBeanPropertyMap(targetClass);

        for (Map.Entry<String, BeanProperty> entry : sourcePropertyMap.entrySet()) {
            String propertyName = entry.getKey();
            BeanProperty sourceProperty = entry.getValue();

            if (!sourceProperty.readable()) {
                continue;
            }
            if (!shouldIncludeProperty(propertyName, actualOptions.includeProperties(), actualOptions.excludeProperties())) {
                continue;
            }

            BeanProperty targetProperty = targetPropertyMap.get(propertyName);
            if (targetProperty == null) {
                if (actualOptions.ignoreUnknown()) {
                    continue;
                }
                throw new BeanException("目标属性不存在：" + targetClass.getName() + "." + propertyName);
            }
            if (!targetProperty.writable()) {
                if (actualOptions.ignoreNotWritable()) {
                    continue;
                }
                throw new BeanException("目标属性不可写：" + targetClass.getName() + "." + propertyName);
            }

            Object sourceValue = getPropertyValue(sourceBean, propertyName);
            if (actualOptions.ignoreNull() && sourceValue == null) {
                continue;
            }

            Object targetValue = convertValue(sourceValue, targetProperty.type());
            setPropertyValue(targetBean, propertyName, targetValue);
        }
    }

    /**
     * 拷贝 Bean 并创建目标对象。
     *
     * @param sourceBean 源 Bean
     * @param targetType 目标类型
     * @return 目标 Bean
     */
    public static <T> T copyToBean(Object sourceBean, Class<T> targetType) {
        return copyToBean(sourceBean, targetType, CopyOptions.defaults());
    }

    /**
     * 拷贝 Bean 并创建目标对象。
     *
     * @param sourceBean 源 Bean
     * @param targetType 目标类型
     * @param options    拷贝配置
     * @return 目标 Bean
     */
    public static <T> T copyToBean(Object sourceBean, Class<T> targetType, CopyOptions options) {
        Objects.requireNonNull(sourceBean, "sourceBean不能为null");
        requireClass(targetType, "targetType");

        T targetBean = newInstance(targetType);
        copyProperties(sourceBean, targetBean, options);
        return targetBean;
    }

    /**
     * 拷贝 Bean，方法别名。
     *
     * @param sourceBean 源 Bean
     * @param targetBean 目标 Bean
     */
    public static void copyBean(Object sourceBean, Object targetBean) {
        copyProperties(sourceBean, targetBean);
    }

    /**
     * 拷贝 Bean，方法别名。
     *
     * @param sourceBean 源 Bean
     * @param targetType 目标类型
     * @return 目标 Bean
     */
    public static <T> T copyBean(Object sourceBean, Class<T> targetType) {
        return copyToBean(sourceBean, targetType);
    }

    /**
     * 批量拷贝 Bean 集合。
     *
     * @param sourceList 源 Bean 集合
     * @param targetType 目标类型
     * @return 目标 Bean 集合
     */
    public static <T> List<T> copyList(Collection<?> sourceList, Class<T> targetType) {
        return copyList(sourceList, targetType, CopyOptions.defaults());
    }

    /**
     * 批量拷贝 Bean 集合。
     *
     * @param sourceList 源 Bean 集合
     * @param targetType 目标类型
     * @param options    拷贝配置
     * @return 目标 Bean 集合
     */
    public static <T> List<T> copyList(Collection<?> sourceList, Class<T> targetType, CopyOptions options) {
        requireClass(targetType, "targetType");

        if (sourceList == null || sourceList.isEmpty()) {
            return new ArrayList<>();
        }

        List<T> targetList = new ArrayList<>(sourceList.size());
        for (Object sourceBean : sourceList) {
            if (sourceBean == null) {
                continue;
            }
            targetList.add(copyToBean(sourceBean, targetType, options));
        }
        return targetList;
    }

    /**
     * 批量拷贝 Bean 数组。
     *
     * @param sourceArray 源 Bean 数组
     * @param targetType  目标类型
     * @return 目标 Bean 集合
     */
    public static <T> List<T> copyArray(Object[] sourceArray, Class<T> targetType) {
        if (sourceArray == null || sourceArray.length == 0) {
            return new ArrayList<>();
        }
        return copyList(List.of(sourceArray), targetType);
    }

    /**
     * 批量拷贝 Bean 数组。
     *
     * @param sourceArray 源 Bean 数组
     * @param targetType  目标类型
     * @param options     拷贝配置
     * @return 目标 Bean 集合
     */
    public static <T> List<T> copyArray(Object[] sourceArray, Class<T> targetType, CopyOptions options) {
        if (sourceArray == null || sourceArray.length == 0) {
            return new ArrayList<>();
        }
        return copyList(List.of(sourceArray), targetType, options);
    }

    /**
     * 批量填充目标 Bean 集合。
     *
     * @param sourceList 源 Bean 集合
     * @param targetList 目标 Bean 集合
     * @param targetType 目标类型
     */
    public static <T> void copyListTo(Collection<?> sourceList, Collection<T> targetList, Class<T> targetType) {
        copyListTo(sourceList, targetList, targetType, CopyOptions.defaults());
    }

    /**
     * 批量填充目标 Bean 集合。
     *
     * @param sourceList 源 Bean 集合
     * @param targetList 目标 Bean 集合
     * @param targetType 目标类型
     * @param options    拷贝配置
     */
    public static <T> void copyListTo(Collection<?> sourceList, Collection<T> targetList, Class<T> targetType, CopyOptions options) {
        Objects.requireNonNull(targetList, "targetList不能为null");
        requireClass(targetType, "targetType");

        if (sourceList == null || sourceList.isEmpty()) {
            return;
        }

        for (Object sourceBean : sourceList) {
            if (sourceBean == null) {
                continue;
            }
            targetList.add(copyToBean(sourceBean, targetType, options));
        }
    }

    /**
     * 转换属性值类型。
     *
     * @param value      原始值
     * @param targetType 目标类型
     * @return 转换后的值
     */
    public static <T> T convertValue(Object value, Class<T> targetType) {
        requireClass(targetType, "targetType");

        if (value == null) {
            if (targetType.isPrimitive()) {
                throw new BeanException("基本类型不允许转换null：" + targetType.getName());
            }
            return null;
        }

        Class<?> actualTargetType = targetType.isPrimitive() ? wrapPrimitive(targetType) : targetType;
        if (actualTargetType.isInstance(value)) {
            @SuppressWarnings("unchecked")
            T result = (T) value;
            return result;
        }

        Object convertedValue;
        if (actualTargetType == String.class) {
            convertedValue = String.valueOf(value);
        } else if (actualTargetType == Boolean.class) {
            convertedValue = convertToBoolean(value);
        } else if (actualTargetType == Character.class) {
            convertedValue = convertToCharacter(value);
        } else if (Number.class.isAssignableFrom(actualTargetType)) {
            convertedValue = convertToNumber(value, actualTargetType);
        } else if (actualTargetType.isEnum()) {
            convertedValue = convertToEnum(value, actualTargetType);
        } else if (actualTargetType == LocalDate.class) {
            convertedValue = convertToLocalDate(value);
        } else if (actualTargetType == LocalDateTime.class) {
            convertedValue = convertToLocalDateTime(value);
        } else if (actualTargetType == LocalTime.class) {
            convertedValue = convertToLocalTime(value);
        } else if (actualTargetType == Instant.class) {
            convertedValue = convertToInstant(value);
        } else if (actualTargetType == Date.class) {
            convertedValue = convertToDate(value);
        } else if (actualTargetType == UUID.class) {
            convertedValue = UUID.fromString(String.valueOf(value));
        } else if (actualTargetType == URI.class) {
            convertedValue = URI.create(String.valueOf(value));
        } else if (actualTargetType == URL.class) {
            convertedValue = convertToUrl(value);
        } else {
            throw new BeanException("不支持的类型转换：" + value.getClass().getName() + " -> " + targetType.getName());
        }

        @SuppressWarnings("unchecked")
        T result = (T) convertedValue;
        return result;
    }

    /**
     * 转换属性值类型，失败时返回默认值。
     *
     * @param value        原始值
     * @param targetType   目标类型
     * @param defaultValue 默认值
     * @return 转换后的值
     */
    public static <T> T convertValue(Object value, Class<T> targetType, T defaultValue) {
        try {
            T convertedValue = convertValue(value, targetType);
            return convertedValue == null ? defaultValue : convertedValue;
        } catch (RuntimeException e) {
            return defaultValue;
        }
    }

    /**
     * 判断属性值是否支持转换为目标类型。
     *
     * @param value      原始值
     * @param targetType 目标类型
     * @return 是否支持转换
     */
    public static boolean canConvertValue(Object value, Class<?> targetType) {
        if (targetType == null) {
            return false;
        }
        try {
            convertValue(value, targetType);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    /**
     * 判断类型是否支持当前内置转换。
     *
     * @param type 类型
     * @return 是否支持转换
     */
    public static boolean isSupportedConvertType(Class<?> type) {
        if (type == null) {
            return false;
        }

        Class<?> actualType = type.isPrimitive() ? wrapPrimitive(type) : type;
        return actualType == String.class
                || actualType == Boolean.class
                || actualType == Character.class
                || Number.class.isAssignableFrom(actualType)
                || actualType.isEnum()
                || actualType == LocalDate.class
                || actualType == LocalDateTime.class
                || actualType == LocalTime.class
                || actualType == Instant.class
                || actualType == Date.class
                || actualType == UUID.class
                || actualType == URI.class
                || actualType == URL.class;
    }

    private static Boolean convertToBoolean(Object value) {
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        if (value instanceof Number numberValue) {
            return numberValue.intValue() != 0;
        }

        String text = String.valueOf(value).trim();
        if (text.equalsIgnoreCase("true")
                || text.equalsIgnoreCase("yes")
                || text.equalsIgnoreCase("y")
                || text.equals("1")
                || text.equals("是")) {
            return true;
        }
        if (text.equalsIgnoreCase("false")
                || text.equalsIgnoreCase("no")
                || text.equalsIgnoreCase("n")
                || text.equals("0")
                || text.equals("否")) {
            return false;
        }

        throw new BeanException("无法转换为Boolean：" + value);
    }

    private static Character convertToCharacter(Object value) {
        if (value instanceof Character characterValue) {
            return characterValue;
        }

        String text = String.valueOf(value);
        if (text.length() != 1) {
            throw new BeanException("无法转换为Character：" + value);
        }

        return text.charAt(0);
    }

    private static Object convertToNumber(Object value, Class<?> targetType) {
        if (value instanceof Number numberValue) {
            return convertNumberToTargetType(numberValue, targetType);
        }

        String text = String.valueOf(value).trim();
        if (text.isEmpty()) {
            throw new BeanException("空字符串无法转换为数字类型：" + targetType.getName());
        }

        try {
            if (targetType == Byte.class) {
                return Byte.valueOf(text);
            }
            if (targetType == Short.class) {
                return Short.valueOf(text);
            }
            if (targetType == Integer.class) {
                return Integer.valueOf(text);
            }
            if (targetType == Long.class) {
                return Long.valueOf(text);
            }
            if (targetType == Float.class) {
                return Float.valueOf(text);
            }
            if (targetType == Double.class) {
                return Double.valueOf(text);
            }
            if (targetType == BigInteger.class) {
                return new BigInteger(text);
            }
            if (targetType == BigDecimal.class) {
                return new BigDecimal(text);
            }
        } catch (NumberFormatException e) {
            throw new BeanException("数字转换失败：" + value + " -> " + targetType.getName(), e);
        }

        throw new BeanException("不支持的数字类型：" + targetType.getName());
    }

    private static Object convertNumberToTargetType(Number value, Class<?> targetType) {
        if (targetType == Byte.class) {
            return value.byteValue();
        }
        if (targetType == Short.class) {
            return value.shortValue();
        }
        if (targetType == Integer.class) {
            return value.intValue();
        }
        if (targetType == Long.class) {
            return value.longValue();
        }
        if (targetType == Float.class) {
            return value.floatValue();
        }
        if (targetType == Double.class) {
            return value.doubleValue();
        }
        if (targetType == BigInteger.class) {
            if (value instanceof BigDecimal bigDecimalValue) {
                return bigDecimalValue.toBigInteger();
            }
            return BigInteger.valueOf(value.longValue());
        }
        if (targetType == BigDecimal.class) {
            if (value instanceof BigInteger bigIntegerValue) {
                return new BigDecimal(bigIntegerValue);
            }
            return BigDecimal.valueOf(value.doubleValue());
        }

        throw new BeanException("不支持的数字类型：" + targetType.getName());
    }

    private static Object convertToEnum(Object value, Class<?> targetType) {
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) {
            throw new BeanException("空字符串无法转换为枚举：" + targetType.getName());
        }

        Object[] enumConstants = targetType.getEnumConstants();
        for (Object enumConstant : enumConstants) {
            Enum<?> enumValue = (Enum<?>) enumConstant;
            if (enumValue.name().equals(text)) {
                return enumValue;
            }
        }

        for (Object enumConstant : enumConstants) {
            Enum<?> enumValue = (Enum<?>) enumConstant;
            if (enumValue.name().equalsIgnoreCase(text)) {
                return enumValue;
            }
        }

        if (value instanceof Number numberValue) {
            int ordinal = numberValue.intValue();
            if (ordinal >= 0 && ordinal < enumConstants.length) {
                return enumConstants[ordinal];
            }
        }

        throw new BeanException("无法转换为枚举：" + value + " -> " + targetType.getName());
    }

    private static LocalDate convertToLocalDate(Object value) {
        if (value instanceof LocalDate localDateValue) {
            return localDateValue;
        }
        if (value instanceof LocalDateTime localDateTimeValue) {
            return localDateTimeValue.toLocalDate();
        }
        if (value instanceof Date dateValue) {
            return dateValue.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }
        if (value instanceof Instant instantValue) {
            return instantValue.atZone(ZoneId.systemDefault()).toLocalDate();
        }

        String text = String.valueOf(value).trim();
        try {
            return LocalDate.parse(text);
        } catch (DateTimeParseException ignored) {
            LocalDateTime localDateTime = parseLocalDateTime(text);
            return localDateTime.toLocalDate();
        }
    }

    private static LocalDateTime convertToLocalDateTime(Object value) {
        if (value instanceof LocalDateTime localDateTimeValue) {
            return localDateTimeValue;
        }
        if (value instanceof LocalDate localDateValue) {
            return localDateValue.atStartOfDay();
        }
        if (value instanceof Date dateValue) {
            return LocalDateTime.ofInstant(dateValue.toInstant(), ZoneId.systemDefault());
        }
        if (value instanceof Instant instantValue) {
            return LocalDateTime.ofInstant(instantValue, ZoneId.systemDefault());
        }

        return parseLocalDateTime(String.valueOf(value).trim());
    }

    private static LocalTime convertToLocalTime(Object value) {
        if (value instanceof LocalTime localTimeValue) {
            return localTimeValue;
        }
        if (value instanceof LocalDateTime localDateTimeValue) {
            return localDateTimeValue.toLocalTime();
        }

        String text = String.valueOf(value).trim();
        try {
            return LocalTime.parse(text);
        } catch (DateTimeParseException e) {
            throw new BeanException("无法转换为LocalTime：" + value, e);
        }
    }

    private static Instant convertToInstant(Object value) {
        if (value instanceof Instant instantValue) {
            return instantValue;
        }
        if (value instanceof Date dateValue) {
            return dateValue.toInstant();
        }
        if (value instanceof LocalDateTime localDateTimeValue) {
            return localDateTimeValue.atZone(ZoneId.systemDefault()).toInstant();
        }
        if (value instanceof LocalDate localDateValue) {
            return localDateValue.atStartOfDay(ZoneId.systemDefault()).toInstant();
        }
        if (value instanceof Number numberValue) {
            return Instant.ofEpochMilli(numberValue.longValue());
        }

        String text = String.valueOf(value).trim();
        try {
            return Instant.parse(text);
        } catch (DateTimeParseException ignored) {
            return parseLocalDateTime(text).atZone(ZoneId.systemDefault()).toInstant();
        }
    }

    private static Date convertToDate(Object value) {
        if (value instanceof Date dateValue) {
            return dateValue;
        }
        if (value instanceof Instant instantValue) {
            return Date.from(instantValue);
        }
        if (value instanceof LocalDateTime localDateTimeValue) {
            return Date.from(localDateTimeValue.atZone(ZoneId.systemDefault()).toInstant());
        }
        if (value instanceof LocalDate localDateValue) {
            return Date.from(localDateValue.atStartOfDay(ZoneId.systemDefault()).toInstant());
        }
        if (value instanceof Number numberValue) {
            return new Date(numberValue.longValue());
        }

        return Date.from(convertToInstant(value));
    }

    private static URL convertToUrl(Object value) {
        try {
            return URI.create(String.valueOf(value)).toURL();
        } catch (MalformedURLException e) {
            throw new BeanException("无法转换为URL：" + value, e);
        }
    }

    private static LocalDateTime parseLocalDateTime(String text) {
        if (text == null || text.isBlank()) {
            throw new BeanException("空字符串无法转换为LocalDateTime");
        }

        for (String pattern : DEFAULT_DATE_TIME_PATTERNS) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
            try {
                if ("yyyy-MM-dd".equals(pattern)) {
                    return LocalDate.parse(text, formatter).atStartOfDay();
                }
                return LocalDateTime.parse(text, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }

        try {
            return LocalDateTime.parse(text);
        } catch (DateTimeParseException e) {
            throw new BeanException("无法转换为LocalDateTime：" + text, e);
        }
    }

    /**
     * Bean 拷贝配置。
     *
     * @param ignoreNull        是否忽略空值
     * @param ignoreUnknown     是否忽略目标不存在的属性
     * @param ignoreNotWritable 是否忽略目标不可写属性
     * @param includeProperties 仅包含的属性名集合，为空表示不过滤
     * @param excludeProperties 排除的属性名集合
     * @author Ateng
     * @since 2026-04-29
     */
    public record CopyOptions(
            boolean ignoreNull,
            boolean ignoreUnknown,
            boolean ignoreNotWritable,
            Collection<String> includeProperties,
            Collection<String> excludeProperties
    ) {

        /**
         * 默认配置。
         *
         * @return Bean 拷贝配置
         */
        public static CopyOptions defaults() {
            return new CopyOptions(false, true, true, Collections.emptyList(), Collections.emptyList());
        }

        /**
         * 忽略空值配置。
         *
         * @return Bean 拷贝配置
         */
        public static CopyOptions ofIgnoreNull() {
            return new CopyOptions(true, true, true, Collections.emptyList(), Collections.emptyList());
        }

        /**
         * 严格配置。
         *
         * @return Bean 拷贝配置
         */
        public static CopyOptions strict() {
            return new CopyOptions(false, false, false, Collections.emptyList(), Collections.emptyList());
        }
    }

    /**
     * 获取嵌套属性值。
     *
     * @param bean         Bean 对象
     * @param propertyPath 属性路径，例如：user.address.city
     * @return 属性值
     */
    public static Object getNestedPropertyValue(Object bean, String propertyPath) {
        Objects.requireNonNull(bean, "bean不能为null");
        List<String> propertyNames = parsePropertyPath(propertyPath);

        Object currentValue = bean;
        for (String propertyName : propertyNames) {
            if (currentValue == null) {
                return null;
            }
            currentValue = getPropertyValue(currentValue, propertyName);
        }

        return currentValue;
    }

    /**
     * 获取嵌套属性值并校验返回类型。
     *
     * @param bean         Bean 对象
     * @param propertyPath 属性路径，例如：user.address.city
     * @param targetType   目标类型
     * @return 属性值
     */
    public static <T> T getNestedPropertyValue(Object bean, String propertyPath, Class<T> targetType) {
        requireClass(targetType, "targetType");

        Object value = getNestedPropertyValue(bean, propertyPath);
        if (value == null) {
            return null;
        }
        if (!targetType.isInstance(value)) {
            throw new BeanException("嵌套属性值类型不匹配：" + propertyPath
                    + "，期望类型=" + targetType.getName()
                    + "，实际类型=" + value.getClass().getName());
        }

        return targetType.cast(value);
    }

    /**
     * 获取嵌套属性值，结果为空时返回默认值。
     *
     * @param bean         Bean 对象
     * @param propertyPath 属性路径，例如：user.address.city
     * @param defaultValue 默认值
     * @return 属性值或默认值
     */
    public static <T> T getNestedPropertyValueOrDefault(Object bean, String propertyPath, T defaultValue) {
        Object value = getNestedPropertyValue(bean, propertyPath);
        if (value == null) {
            return defaultValue;
        }

        try {
            @SuppressWarnings("unchecked")
            T result = (T) value;
            return result;
        } catch (ClassCastException e) {
            throw new BeanException("嵌套属性值类型与默认值类型不匹配：" + propertyPath, e);
        }
    }

    /**
     * 设置嵌套属性值，中间对象为空时默认自动创建。
     *
     * @param bean         Bean 对象
     * @param propertyPath 属性路径，例如：user.address.city
     * @param value        属性值
     */
    public static void setNestedPropertyValue(Object bean, String propertyPath, Object value) {
        setNestedPropertyValue(bean, propertyPath, value, true);
    }

    /**
     * 设置嵌套属性值。
     *
     * @param bean                 Bean 对象
     * @param propertyPath         属性路径，例如：user.address.city
     * @param value                属性值
     * @param autoCreateNestedBean 中间对象为空时是否自动创建
     */
    public static void setNestedPropertyValue(Object bean, String propertyPath, Object value, boolean autoCreateNestedBean) {
        Objects.requireNonNull(bean, "bean不能为null");
        List<String> propertyNames = parsePropertyPath(propertyPath);

        if (propertyNames.size() == 1) {
            setPropertyValue(bean, propertyNames.getFirst(), value);
            return;
        }

        Object currentBean = bean;
        for (int i = 0; i < propertyNames.size() - 1; i++) {
            String propertyName = propertyNames.get(i);
            Object nestedValue = getPropertyValue(currentBean, propertyName);

            if (nestedValue == null) {
                if (!autoCreateNestedBean) {
                    throw new BeanException("嵌套属性中间对象为空：" + joinPropertyPath(propertyNames, 0, i + 1));
                }

                Class<?> nestedType = getPropertyType(currentBean.getClass(), propertyName);
                if (!isInstantiable(nestedType)) {
                    throw new BeanException("嵌套属性类型不支持自动创建：" + nestedType.getName());
                }

                nestedValue = newInstance(nestedType);
                setPropertyValue(currentBean, propertyName, nestedValue);
            }

            currentBean = nestedValue;
        }

        setPropertyValue(currentBean, propertyNames.getLast(), value);
    }

    /**
     * 判断嵌套属性是否存在。
     *
     * @param beanClass    Bean 类型
     * @param propertyPath 属性路径，例如：user.address.city
     * @return 是否存在
     */
    public static boolean hasNestedProperty(Class<?> beanClass, String propertyPath) {
        if (beanClass == null || propertyPath == null || propertyPath.isBlank()) {
            return false;
        }

        try {
            Class<?> currentType = beanClass;
            for (String propertyName : parsePropertyPath(propertyPath)) {
                if (!hasProperty(currentType, propertyName)) {
                    return false;
                }
                currentType = getPropertyType(currentType, propertyName);
            }
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    /**
     * 判断嵌套属性是否可读。
     *
     * @param beanClass    Bean 类型
     * @param propertyPath 属性路径，例如：user.address.city
     * @return 是否可读
     */
    public static boolean isReadableNestedProperty(Class<?> beanClass, String propertyPath) {
        if (beanClass == null || propertyPath == null || propertyPath.isBlank()) {
            return false;
        }

        try {
            Class<?> currentType = beanClass;
            List<String> propertyNames = parsePropertyPath(propertyPath);
            for (String propertyName : propertyNames) {
                if (!isReadableProperty(currentType, propertyName)) {
                    return false;
                }
                currentType = getPropertyType(currentType, propertyName);
            }
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    /**
     * 判断嵌套属性是否可写。
     *
     * @param beanClass    Bean 类型
     * @param propertyPath 属性路径，例如：user.address.city
     * @return 是否可写
     */
    public static boolean isWritableNestedProperty(Class<?> beanClass, String propertyPath) {
        if (beanClass == null || propertyPath == null || propertyPath.isBlank()) {
            return false;
        }

        try {
            Class<?> currentType = beanClass;
            List<String> propertyNames = parsePropertyPath(propertyPath);
            for (int i = 0; i < propertyNames.size(); i++) {
                String propertyName = propertyNames.get(i);
                if (i == propertyNames.size() - 1) {
                    return isWritableProperty(currentType, propertyName);
                }
                if (!isReadableProperty(currentType, propertyName)) {
                    return false;
                }
                currentType = getPropertyType(currentType, propertyName);
            }
            return false;
        } catch (RuntimeException e) {
            return false;
        }
    }

    /**
     * 解析嵌套属性路径。
     *
     * @param propertyPath 属性路径
     * @return 属性名列表
     */
    public static List<String> parsePropertyPath(String propertyPath) {
        requireText(propertyPath, "propertyPath");

        String[] segments = propertyPath.split("\\.", -1);
        List<String> propertyNames = Arrays.stream(segments)
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .toList();

        if (propertyNames.isEmpty()) {
            throw new IllegalArgumentException("属性路径不能为空");
        }
        if (propertyNames.size() != segments.length) {
            throw new IllegalArgumentException("属性路径格式不正确：" + propertyPath);
        }

        return propertyNames;
    }

    /**
     * 根据字段过滤配置获取字段列表。
     *
     * @param beanClass Bean 类型
     * @param options   字段过滤配置
     * @return 字段列表
     */
    public static List<Field> getFields(Class<?> beanClass, FieldFilterOptions options) {
        requireClass(beanClass, "beanClass");

        FieldFilterOptions actualOptions = options == null ? FieldFilterOptions.defaults() : options;
        return getFields(beanClass).stream()
                .filter(field -> matchesFieldFilter(field, actualOptions))
                .toList();
    }

    /**
     * 根据字段名包含和排除规则获取字段列表。
     *
     * @param beanClass    Bean 类型
     * @param includeNames 仅包含的字段名集合，为空表示不过滤
     * @param excludeNames 排除的字段名集合
     * @return 字段列表
     */
    public static List<Field> getFields(Class<?> beanClass, Collection<String> includeNames, Collection<String> excludeNames) {
        return getFields(beanClass, new FieldFilterOptions(includeNames, excludeNames, false, false, null));
    }

    /**
     * 根据字段过滤器获取字段列表。
     *
     * @param beanClass Bean 类型
     * @param predicate 字段过滤器
     * @return 字段列表
     */
    public static List<Field> getFields(Class<?> beanClass, Predicate<Field> predicate) {
        requireClass(beanClass, "beanClass");

        Predicate<Field> actualPredicate = predicate == null ? field -> true : predicate;
        return getFields(beanClass).stream()
                .filter(actualPredicate)
                .toList();
    }

    /**
     * 根据字段过滤配置获取字段名列表。
     *
     * @param beanClass Bean 类型
     * @param options   字段过滤配置
     * @return 字段名列表
     */
    public static List<String> getFieldNames(Class<?> beanClass, FieldFilterOptions options) {
        return getFields(beanClass, options).stream()
                .map(Field::getName)
                .toList();
    }

    /**
     * 根据字段过滤配置获取字段映射。
     *
     * @param beanClass Bean 类型
     * @param options   字段过滤配置
     * @return 字段映射
     */
    public static Map<String, Field> getFieldMap(Class<?> beanClass, FieldFilterOptions options) {
        Map<String, Field> result = new LinkedHashMap<>();
        for (Field field : getFields(beanClass, options)) {
            result.put(field.getName(), field);
        }
        return Collections.unmodifiableMap(result);
    }

    /**
     * 根据属性过滤配置获取属性元信息列表。
     *
     * @param beanClass Bean 类型
     * @param options   属性过滤配置
     * @return 属性元信息列表
     */
    public static List<BeanProperty> getBeanProperties(Class<?> beanClass, PropertyFilterOptions options) {
        requireClass(beanClass, "beanClass");

        PropertyFilterOptions actualOptions = options == null ? PropertyFilterOptions.defaults() : options;
        return getBeanPropertyMap(beanClass).values().stream()
                .filter(property -> matchesPropertyFilter(property, actualOptions))
                .toList();
    }

    /**
     * 根据属性名包含和排除规则获取属性名列表。
     *
     * @param beanClass    Bean 类型
     * @param includeNames 仅包含的属性名集合，为空表示不过滤
     * @param excludeNames 排除的属性名集合
     * @return 属性名列表
     */
    public static List<String> getPropertyNames(Class<?> beanClass, Collection<String> includeNames, Collection<String> excludeNames) {
        return getBeanProperties(beanClass, new PropertyFilterOptions(includeNames, excludeNames, false, false, null))
                .stream()
                .map(BeanProperty::name)
                .toList();
    }

    /**
     * 根据属性过滤器获取属性元信息列表。
     *
     * @param beanClass Bean 类型
     * @param predicate 属性过滤器
     * @return 属性元信息列表
     */
    public static List<BeanProperty> getBeanProperties(Class<?> beanClass, Predicate<BeanProperty> predicate) {
        requireClass(beanClass, "beanClass");

        Predicate<BeanProperty> actualPredicate = predicate == null ? property -> true : predicate;
        return getBeanPropertyMap(beanClass).values().stream()
                .filter(actualPredicate)
                .toList();
    }

    /**
     * 过滤 Map 中的属性。
     *
     * @param sourceMap         源 Map
     * @param includeProperties 仅包含的属性名集合，为空表示不过滤
     * @param excludeProperties 排除的属性名集合
     * @return 过滤后的 Map
     */
    public static Map<String, Object> filterPropertyMap(Map<String, ?> sourceMap, Collection<String> includeProperties, Collection<String> excludeProperties) {
        if (sourceMap == null || sourceMap.isEmpty()) {
            return new LinkedHashMap<>();
        }

        Map<String, Object> result = new LinkedHashMap<>();
        sourceMap.forEach((propertyName, value) -> {
            if (propertyName != null && shouldIncludeProperty(propertyName, includeProperties, excludeProperties)) {
                result.put(propertyName, value);
            }
        });

        return result;
    }

    /**
     * 判断类上是否存在指定注解。
     *
     * @param type           类型
     * @param annotationType 注解类型
     * @return 是否存在
     */
    public static boolean hasAnnotation(Class<?> type, Class<? extends Annotation> annotationType) {
        return findAnnotation(type, annotationType).isPresent();
    }

    /**
     * 查找类上的指定注解。
     *
     * @param type           类型
     * @param annotationType 注解类型
     * @return 注解对象
     */
    public static <A extends Annotation> Optional<A> findAnnotation(Class<?> type, Class<A> annotationType) {
        if (type == null || annotationType == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(type.getAnnotation(annotationType));
    }

    /**
     * 判断字段上是否存在指定注解。
     *
     * @param field          字段
     * @param annotationType 注解类型
     * @return 是否存在
     */
    public static boolean hasAnnotation(Field field, Class<? extends Annotation> annotationType) {
        return findAnnotation(field, annotationType).isPresent();
    }

    /**
     * 查找字段上的指定注解。
     *
     * @param field          字段
     * @param annotationType 注解类型
     * @return 注解对象
     */
    public static <A extends Annotation> Optional<A> findAnnotation(Field field, Class<A> annotationType) {
        if (field == null || annotationType == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(field.getAnnotation(annotationType));
    }

    /**
     * 判断方法上是否存在指定注解。
     *
     * @param method         方法
     * @param annotationType 注解类型
     * @return 是否存在
     */
    public static boolean hasAnnotation(Method method, Class<? extends Annotation> annotationType) {
        return findAnnotation(method, annotationType).isPresent();
    }

    /**
     * 查找方法上的指定注解。
     *
     * @param method         方法
     * @param annotationType 注解类型
     * @return 注解对象
     */
    public static <A extends Annotation> Optional<A> findAnnotation(Method method, Class<A> annotationType) {
        if (method == null || annotationType == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(method.getAnnotation(annotationType));
    }

    /**
     * 判断属性上是否存在指定注解。
     *
     * @param beanClass      Bean 类型
     * @param propertyName   属性名
     * @param annotationType 注解类型
     * @return 是否存在
     */
    public static boolean hasPropertyAnnotation(Class<?> beanClass, String propertyName, Class<? extends Annotation> annotationType) {
        return findPropertyAnnotation(beanClass, propertyName, annotationType).isPresent();
    }

    /**
     * 查找属性上的指定注解，按字段、Getter、Setter 的顺序查找。
     *
     * @param beanClass      Bean 类型
     * @param propertyName   属性名
     * @param annotationType 注解类型
     * @return 注解对象
     */
    public static <A extends Annotation> Optional<A> findPropertyAnnotation(Class<?> beanClass, String propertyName, Class<A> annotationType) {
        if (beanClass == null || propertyName == null || propertyName.isBlank() || annotationType == null) {
            return Optional.empty();
        }

        BeanProperty property = getBeanPropertyMap(beanClass).get(propertyName);
        if (property == null) {
            return Optional.empty();
        }

        if (property.field() != null) {
            A annotation = property.field().getAnnotation(annotationType);
            if (annotation != null) {
                return Optional.of(annotation);
            }
        }
        if (property.readMethod() != null) {
            A annotation = property.readMethod().getAnnotation(annotationType);
            if (annotation != null) {
                return Optional.of(annotation);
            }
        }
        if (property.writeMethod() != null) {
            A annotation = property.writeMethod().getAnnotation(annotationType);
            if (annotation != null) {
                return Optional.of(annotation);
            }
        }

        return Optional.empty();
    }

    /**
     * 获取带有指定注解的字段列表。
     *
     * @param beanClass      Bean 类型
     * @param annotationType 注解类型
     * @return 字段列表
     */
    public static List<Field> getAnnotatedFields(Class<?> beanClass, Class<? extends Annotation> annotationType) {
        requireClass(beanClass, "beanClass");
        Objects.requireNonNull(annotationType, "annotationType不能为null");

        return getFields(beanClass).stream()
                .filter(field -> field.isAnnotationPresent(annotationType))
                .toList();
    }

    /**
     * 获取带有指定注解的方法列表。
     *
     * @param beanClass      Bean 类型
     * @param annotationType 注解类型
     * @return 方法列表
     */
    public static List<Method> getAnnotatedMethods(Class<?> beanClass, Class<? extends Annotation> annotationType) {
        requireClass(beanClass, "beanClass");
        Objects.requireNonNull(annotationType, "annotationType不能为null");

        List<Method> methods = new ArrayList<>();
        Class<?> currentClass = beanClass;
        while (currentClass != null && currentClass != Object.class) {
            for (Method method : currentClass.getDeclaredMethods()) {
                if (!method.isSynthetic() && method.isAnnotationPresent(annotationType)) {
                    methods.add(method);
                }
            }
            currentClass = currentClass.getSuperclass();
        }

        return methods;
    }

    /**
     * 获取带有指定注解的属性元信息列表。
     *
     * @param beanClass      Bean 类型
     * @param annotationType 注解类型
     * @return 属性元信息列表
     */
    public static List<BeanProperty> getAnnotatedProperties(Class<?> beanClass, Class<? extends Annotation> annotationType) {
        requireClass(beanClass, "beanClass");
        Objects.requireNonNull(annotationType, "annotationType不能为null");

        return getBeanPropertyMap(beanClass).values().stream()
                .filter(property -> hasPropertyAnnotation(beanClass, property.name(), annotationType))
                .toList();
    }

    /**
     * 获取字段上的所有注解。
     *
     * @param field 字段
     * @return 注解列表
     */
    public static List<Annotation> getAnnotations(Field field) {
        if (field == null) {
            return Collections.emptyList();
        }

        return List.of(field.getAnnotations());
    }

    /**
     * 获取方法上的所有注解。
     *
     * @param method 方法
     * @return 注解列表
     */
    public static List<Annotation> getAnnotations(Method method) {
        if (method == null) {
            return Collections.emptyList();
        }

        return List.of(method.getAnnotations());
    }

    /**
     * 获取类上的所有注解。
     *
     * @param type 类型
     * @return 注解列表
     */
    public static List<Annotation> getAnnotations(Class<?> type) {
        if (type == null) {
            return Collections.emptyList();
        }

        return List.of(type.getAnnotations());
    }

    /**
     * 获取属性上的所有注解，合并字段、Getter、Setter 上的注解。
     *
     * @param beanClass    Bean 类型
     * @param propertyName 属性名
     * @return 注解列表
     */
    public static List<Annotation> getPropertyAnnotations(Class<?> beanClass, String propertyName) {
        if (beanClass == null || propertyName == null || propertyName.isBlank()) {
            return Collections.emptyList();
        }

        BeanProperty property = getBeanPropertyMap(beanClass).get(propertyName);
        if (property == null) {
            return Collections.emptyList();
        }

        Map<Class<? extends Annotation>, Annotation> annotationMap = new LinkedHashMap<>();
        if (property.field() != null) {
            for (Annotation annotation : property.field().getAnnotations()) {
                annotationMap.put(annotation.annotationType(), annotation);
            }
        }
        if (property.readMethod() != null) {
            for (Annotation annotation : property.readMethod().getAnnotations()) {
                annotationMap.putIfAbsent(annotation.annotationType(), annotation);
            }
        }
        if (property.writeMethod() != null) {
            for (Annotation annotation : property.writeMethod().getAnnotations()) {
                annotationMap.putIfAbsent(annotation.annotationType(), annotation);
            }
        }

        return new ArrayList<>(annotationMap.values());
    }

    private static boolean matchesFieldFilter(Field field, FieldFilterOptions options) {
        if (field == null) {
            return false;
        }
        if (!shouldIncludeProperty(field.getName(), options.includeNames(), options.excludeNames())) {
            return false;
        }
        if (options.onlyReadable() && !isReadableField(field)) {
            return false;
        }
        if (options.onlyWritable() && !isWritableField(field)) {
            return false;
        }
        return options.predicate() == null || options.predicate().test(field);
    }

    private static boolean matchesPropertyFilter(BeanProperty property, PropertyFilterOptions options) {
        if (property == null) {
            return false;
        }
        if (!shouldIncludeProperty(property.name(), options.includeNames(), options.excludeNames())) {
            return false;
        }
        if (options.onlyReadable() && !property.readable()) {
            return false;
        }
        if (options.onlyWritable() && !property.writable()) {
            return false;
        }
        return options.predicate() == null || options.predicate().test(property);
    }

    private static String joinPropertyPath(List<String> propertyNames, int startInclusive, int endExclusive) {
        if (propertyNames == null || propertyNames.isEmpty()) {
            return "";
        }

        return String.join(".", propertyNames.subList(startInclusive, endExclusive));
    }

    /**
     * 字段过滤配置。
     *
     * @param includeNames 仅包含的字段名集合，为空表示不过滤
     * @param excludeNames 排除的字段名集合
     * @param onlyReadable 是否只保留可读字段
     * @param onlyWritable 是否只保留可写字段
     * @param predicate    自定义字段过滤器
     * @author Ateng
     * @since 2026-04-29
     */
    public record FieldFilterOptions(
            Collection<String> includeNames,
            Collection<String> excludeNames,
            boolean onlyReadable,
            boolean onlyWritable,
            Predicate<Field> predicate
    ) {

        /**
         * 默认配置。
         *
         * @return 字段过滤配置
         */
        public static FieldFilterOptions defaults() {
            return new FieldFilterOptions(Collections.emptyList(), Collections.emptyList(), false, false, null);
        }

        /**
         * 仅保留可写字段。
         *
         * @return 字段过滤配置
         */
        public static FieldFilterOptions ofOnlyWritable() {
            return new FieldFilterOptions(Collections.emptyList(), Collections.emptyList(), false, true, null);
        }

        /**
         * 仅保留可读字段。
         *
         * @return 字段过滤配置
         */
        public static FieldFilterOptions ofOnlyReadable() {
            return new FieldFilterOptions(Collections.emptyList(), Collections.emptyList(), true, false, null);
        }
    }

    /**
     * 属性过滤配置。
     *
     * @param includeNames 仅包含的属性名集合，为空表示不过滤
     * @param excludeNames 排除的属性名集合
     * @param onlyReadable 是否只保留可读属性
     * @param onlyWritable 是否只保留可写属性
     * @param predicate    自定义属性过滤器
     * @author Ateng
     * @since 2026-04-29
     */
    public record PropertyFilterOptions(
            Collection<String> includeNames,
            Collection<String> excludeNames,
            boolean onlyReadable,
            boolean onlyWritable,
            Predicate<BeanProperty> predicate
    ) {

        /**
         * 默认配置。
         *
         * @return 属性过滤配置
         */
        public static PropertyFilterOptions defaults() {
            return new PropertyFilterOptions(Collections.emptyList(), Collections.emptyList(), false, false, null);
        }

        /**
         * 仅保留可读属性。
         *
         * @return 属性过滤配置
         */
        public static PropertyFilterOptions ofOnlyReadable() {
            return new PropertyFilterOptions(Collections.emptyList(), Collections.emptyList(), true, false, null);
        }

        /**
         * 仅保留可写属性。
         *
         * @return 属性过滤配置
         */
        public static PropertyFilterOptions ofOnlyWritable() {
            return new PropertyFilterOptions(Collections.emptyList(), Collections.emptyList(), false, true, null);
        }
    }

    /**
     * 方法缓存，包含当前类和父类声明的方法，不包含合成方法。
     */
    private static final ClassValue<List<Method>> METHOD_CACHE = new ClassValue<>() {
        @Override
        protected List<Method> computeValue(Class<?> type) {
            List<Method> methods = new ArrayList<>();
            Class<?> currentClass = type;
            while (currentClass != null && currentClass != Object.class) {
                for (Method method : currentClass.getDeclaredMethods()) {
                    if (!method.isSynthetic()) {
                        methods.add(method);
                    }
                }
                currentClass = currentClass.getSuperclass();
            }
            return Collections.unmodifiableList(methods);
        }
    };

    /**
     * 构造方法缓存，包含当前类声明的构造方法。
     */
    private static final ClassValue<List<Constructor<?>>> CONSTRUCTOR_CACHE = new ClassValue<>() {
        @Override
        protected List<Constructor<?>> computeValue(Class<?> type) {
            return List.of(type.getDeclaredConstructors());
        }
    };

    /**
     * 判断对象是否为 null。
     *
     * @param value 对象
     * @return 是否为 null
     */
    public static boolean isNull(Object value) {
        return value == null;
    }

    /**
     * 判断对象是否不为 null。
     *
     * @param value 对象
     * @return 是否不为 null
     */
    public static boolean isNotNull(Object value) {
        return value != null;
    }

    /**
     * 判断字符串是否为空白。
     *
     * @param value 字符串
     * @return 是否为空白
     */
    public static boolean isBlank(CharSequence value) {
        return value == null || value.toString().isBlank();
    }

    /**
     * 判断字符串是否非空白。
     *
     * @param value 字符串
     * @return 是否非空白
     */
    public static boolean isNotBlank(CharSequence value) {
        return !isBlank(value);
    }

    /**
     * 判断对象是否为空。
     *
     * @param value 对象
     * @return 是否为空
     */
    public static boolean isEmpty(Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof CharSequence charSequence) {
            return charSequence.isEmpty();
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
     * 判断对象是否非空。
     *
     * @param value 对象
     * @return 是否非空
     */
    public static boolean isNotEmpty(Object value) {
        return !isEmpty(value);
    }

    /**
     * 判断对象是否为空白。
     *
     * @param value 对象
     * @return 是否为空白
     */
    public static boolean isBlankValue(Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof CharSequence charSequence) {
            return charSequence.toString().isBlank();
        }
        return isEmpty(value);
    }

    /**
     * null 时返回默认值。
     *
     * @param value        原始值
     * @param defaultValue 默认值
     * @return 原始值或默认值
     */
    public static <T> T defaultIfNull(T value, T defaultValue) {
        return value == null ? defaultValue : value;
    }

    /**
     * null 时通过 Supplier 获取默认值。
     *
     * @param value           原始值
     * @param defaultSupplier 默认值供应器
     * @return 原始值或默认值
     */
    public static <T> T defaultIfNull(T value, Supplier<? extends T> defaultSupplier) {
        if (value != null) {
            return value;
        }
        return defaultSupplier == null ? null : defaultSupplier.get();
    }

    /**
     * 空对象时返回默认值。
     *
     * @param value        原始值
     * @param defaultValue 默认值
     * @return 原始值或默认值
     */
    public static <T> T defaultIfEmpty(T value, T defaultValue) {
        return isEmpty(value) ? defaultValue : value;
    }

    /**
     * 空对象时通过 Supplier 获取默认值。
     *
     * @param value           原始值
     * @param defaultSupplier 默认值供应器
     * @return 原始值或默认值
     */
    public static <T> T defaultIfEmpty(T value, Supplier<? extends T> defaultSupplier) {
        if (!isEmpty(value)) {
            return value;
        }
        return defaultSupplier == null ? null : defaultSupplier.get();
    }

    /**
     * 空白字符串时返回默认值。
     *
     * @param value        原始值
     * @param defaultValue 默认值
     * @return 原始值或默认值
     */
    public static String defaultIfBlank(String value, String defaultValue) {
        return isBlank(value) ? defaultValue : value;
    }

    /**
     * 返回第一个非 null 值。
     *
     * @param values 候选值
     * @return 第一个非 null 值
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
     * 返回第一个非空值。
     *
     * @param values 候选值
     * @return 第一个非空值
     */
    @SafeVarargs
    public static <T> T firstNonEmpty(T... values) {
        if (values == null || values.length == 0) {
            return null;
        }

        for (T value : values) {
            if (!isEmpty(value)) {
                return value;
            }
        }

        return null;
    }

    /**
     * null 转空字符串。
     *
     * @param value 原始字符串
     * @return 字符串
     */
    public static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    /**
     * 空字符串转 null。
     *
     * @param value 原始字符串
     * @return 字符串或 null
     */
    public static String emptyToNull(String value) {
        return value == null || value.isEmpty() ? null : value;
    }

    /**
     * 空白字符串转 null。
     *
     * @param value 原始字符串
     * @return 字符串或 null
     */
    public static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    /**
     * 获取类型默认值。
     *
     * @param type 类型
     * @return 默认值
     */
    public static Object getDefaultValue(Class<?> type) {
        requireClass(type, "type");

        if (!type.isPrimitive()) {
            return null;
        }
        if (type == boolean.class) {
            return false;
        }
        if (type == byte.class) {
            return (byte) 0;
        }
        if (type == char.class) {
            return '\0';
        }
        if (type == short.class) {
            return (short) 0;
        }
        if (type == int.class) {
            return 0;
        }
        if (type == long.class) {
            return 0L;
        }
        if (type == float.class) {
            return 0F;
        }
        if (type == double.class) {
            return 0D;
        }

        return null;
    }

    /**
     * 判断属性值是否为 null。
     *
     * @param bean         Bean 对象
     * @param propertyName 属性名
     * @return 是否为 null
     */
    public static boolean isNullProperty(Object bean, String propertyName) {
        return getPropertyValue(bean, propertyName) == null;
    }

    /**
     * 判断属性值是否为空。
     *
     * @param bean         Bean 对象
     * @param propertyName 属性名
     * @return 是否为空
     */
    public static boolean isEmptyProperty(Object bean, String propertyName) {
        return isEmpty(getPropertyValue(bean, propertyName));
    }

    /**
     * 判断属性值是否为空白。
     *
     * @param bean         Bean 对象
     * @param propertyName 属性名
     * @return 是否为空白
     */
    public static boolean isBlankProperty(Object bean, String propertyName) {
        return isBlankValue(getPropertyValue(bean, propertyName));
    }

    /**
     * 获取 null 属性名列表。
     *
     * @param bean Bean 对象
     * @return 属性名列表
     */
    public static List<String> getNullPropertyNames(Object bean) {
        Objects.requireNonNull(bean, "bean不能为null");

        return getBeanPropertyMap(bean.getClass()).values().stream()
                .filter(BeanProperty::readable)
                .filter(property -> getPropertyValue(bean, property.name()) == null)
                .map(BeanProperty::name)
                .toList();
    }

    /**
     * 获取非 null 属性名列表。
     *
     * @param bean Bean 对象
     * @return 属性名列表
     */
    public static List<String> getNotNullPropertyNames(Object bean) {
        Objects.requireNonNull(bean, "bean不能为null");

        return getBeanPropertyMap(bean.getClass()).values().stream()
                .filter(BeanProperty::readable)
                .filter(property -> getPropertyValue(bean, property.name()) != null)
                .map(BeanProperty::name)
                .toList();
    }

    /**
     * 获取空属性名列表。
     *
     * @param bean Bean 对象
     * @return 属性名列表
     */
    public static List<String> getEmptyPropertyNames(Object bean) {
        Objects.requireNonNull(bean, "bean不能为null");

        return getBeanPropertyMap(bean.getClass()).values().stream()
                .filter(BeanProperty::readable)
                .filter(property -> isEmpty(getPropertyValue(bean, property.name())))
                .map(BeanProperty::name)
                .toList();
    }

    /**
     * null 时设置属性默认值。
     *
     * @param bean         Bean 对象
     * @param propertyName 属性名
     * @param defaultValue 默认值
     * @return 是否设置成功
     */
    public static boolean setDefaultIfNull(Object bean, String propertyName, Object defaultValue) {
        Objects.requireNonNull(bean, "bean不能为null");
        requireText(propertyName, "propertyName");

        Object value = getPropertyValue(bean, propertyName);
        if (value != null) {
            return false;
        }

        setPropertyValue(bean, propertyName, defaultValue);
        return true;
    }

    /**
     * 空对象时设置属性默认值。
     *
     * @param bean         Bean 对象
     * @param propertyName 属性名
     * @param defaultValue 默认值
     * @return 是否设置成功
     */
    public static boolean setDefaultIfEmpty(Object bean, String propertyName, Object defaultValue) {
        Objects.requireNonNull(bean, "bean不能为null");
        requireText(propertyName, "propertyName");

        Object value = getPropertyValue(bean, propertyName);
        if (!isEmpty(value)) {
            return false;
        }

        setPropertyValue(bean, propertyName, defaultValue);
        return true;
    }

    /**
     * 根据默认值 Map 填充 null 属性。
     *
     * @param bean          Bean 对象
     * @param defaultValues 默认值映射
     */
    public static void fillDefaultsIfNull(Object bean, Map<String, ?> defaultValues) {
        Objects.requireNonNull(bean, "bean不能为null");

        if (defaultValues == null || defaultValues.isEmpty()) {
            return;
        }

        for (Map.Entry<String, ?> entry : defaultValues.entrySet()) {
            String propertyName = entry.getKey();
            if (propertyName == null || propertyName.isBlank() || !isWritableProperty(bean.getClass(), propertyName)) {
                continue;
            }
            setDefaultIfNull(bean, propertyName, entry.getValue());
        }
    }

    /**
     * 根据默认值 Map 填充空属性。
     *
     * @param bean          Bean 对象
     * @param defaultValues 默认值映射
     */
    public static void fillDefaultsIfEmpty(Object bean, Map<String, ?> defaultValues) {
        Objects.requireNonNull(bean, "bean不能为null");

        if (defaultValues == null || defaultValues.isEmpty()) {
            return;
        }

        for (Map.Entry<String, ?> entry : defaultValues.entrySet()) {
            String propertyName = entry.getKey();
            if (propertyName == null || propertyName.isBlank() || !isWritableProperty(bean.getClass(), propertyName)) {
                continue;
            }
            setDefaultIfEmpty(bean, propertyName, entry.getValue());
        }
    }

    /**
     * 获取已缓存字段数量。
     *
     * @param beanClass Bean 类型
     * @return 字段数量
     */
    public static int getCachedFieldCount(Class<?> beanClass) {
        requireClass(beanClass, "beanClass");
        return FIELD_CACHE.get(beanClass).size();
    }

    /**
     * 获取已缓存属性数量。
     *
     * @param beanClass Bean 类型
     * @return 属性数量
     */
    public static int getCachedPropertyCount(Class<?> beanClass) {
        requireClass(beanClass, "beanClass");
        return PROPERTY_DESCRIPTOR_CACHE.get(beanClass).size();
    }

    /**
     * 获取已缓存方法数量。
     *
     * @param beanClass Bean 类型
     * @return 方法数量
     */
    public static int getCachedMethodCount(Class<?> beanClass) {
        requireClass(beanClass, "beanClass");
        return METHOD_CACHE.get(beanClass).size();
    }

    /**
     * 获取已缓存构造方法数量。
     *
     * @param beanClass Bean 类型
     * @return 构造方法数量
     */
    public static int getCachedConstructorCount(Class<?> beanClass) {
        requireClass(beanClass, "beanClass");
        return CONSTRUCTOR_CACHE.get(beanClass).size();
    }

    /**
     * 预热指定类型的反射缓存。
     *
     * @param beanClass Bean 类型
     */
    public static void warmUpCache(Class<?> beanClass) {
        requireClass(beanClass, "beanClass");

        FIELD_CACHE.get(beanClass);
        PROPERTY_DESCRIPTOR_CACHE.get(beanClass);
        METHOD_CACHE.get(beanClass);
        CONSTRUCTOR_CACHE.get(beanClass);
    }

    /**
     * 批量预热反射缓存。
     *
     * @param beanClasses Bean 类型集合
     */
    public static void warmUpCaches(Collection<Class<?>> beanClasses) {
        if (beanClasses == null || beanClasses.isEmpty()) {
            return;
        }

        for (Class<?> beanClass : beanClasses) {
            if (beanClass != null) {
                warmUpCache(beanClass);
            }
        }
    }

    /**
     * 清除指定类型的扩展反射缓存。
     *
     * @param beanClass Bean 类型
     */
    public static void clearExtendedCache(Class<?> beanClass) {
        requireClass(beanClass, "beanClass");

        METHOD_CACHE.remove(beanClass);
        CONSTRUCTOR_CACHE.remove(beanClass);
    }

    /**
     * 清除指定类型的全部反射缓存。
     *
     * @param beanClass Bean 类型
     */
    public static void clearAllCache(Class<?> beanClass) {
        requireClass(beanClass, "beanClass");

        clearCache(beanClass);
        clearExtendedCache(beanClass);
    }

    /**
     * 批量清除指定类型的全部反射缓存。
     *
     * @param beanClasses Bean 类型集合
     */
    public static void clearAllCaches(Collection<Class<?>> beanClasses) {
        if (beanClasses == null || beanClasses.isEmpty()) {
            return;
        }

        for (Class<?> beanClass : beanClasses) {
            if (beanClass != null) {
                clearAllCache(beanClass);
            }
        }
    }

    /**
     * 获取缓存快照。
     *
     * @param beanClass Bean 类型
     * @return 缓存快照
     */
    public static CacheSnapshot getCacheSnapshot(Class<?> beanClass) {
        requireClass(beanClass, "beanClass");

        return new CacheSnapshot(
                beanClass,
                FIELD_CACHE.get(beanClass).size(),
                PROPERTY_DESCRIPTOR_CACHE.get(beanClass).size(),
                METHOD_CACHE.get(beanClass).size(),
                CONSTRUCTOR_CACHE.get(beanClass).size()
        );
    }

    /**
     * 安全获取字段值。
     *
     * @param bean      Bean 对象
     * @param fieldName 字段名
     * @return 字段值
     */
    public static Optional<Object> safeGetFieldValue(Object bean, String fieldName) {
        try {
            if (bean == null || fieldName == null || fieldName.isBlank()) {
                return Optional.empty();
            }
            return Optional.ofNullable(getFieldValue(bean, fieldName));
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    /**
     * 安全获取属性值。
     *
     * @param bean         Bean 对象
     * @param propertyName 属性名
     * @return 属性值
     */
    public static Optional<Object> safeGetPropertyValue(Object bean, String propertyName) {
        try {
            if (bean == null || propertyName == null || propertyName.isBlank()) {
                return Optional.empty();
            }
            return Optional.ofNullable(getPropertyValue(bean, propertyName));
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    /**
     * 安全获取属性值并校验返回类型。
     *
     * @param bean         Bean 对象
     * @param propertyName 属性名
     * @param targetType   目标类型
     * @return 属性值
     */
    public static <T> Optional<T> safeGetPropertyValue(Object bean, String propertyName, Class<T> targetType) {
        try {
            if (bean == null || propertyName == null || propertyName.isBlank() || targetType == null) {
                return Optional.empty();
            }
            return Optional.ofNullable(getPropertyValue(bean, propertyName, targetType));
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    /**
     * 安全获取嵌套属性值。
     *
     * @param bean         Bean 对象
     * @param propertyPath 属性路径
     * @return 属性值
     */
    public static Optional<Object> safeGetNestedPropertyValue(Object bean, String propertyPath) {
        try {
            if (bean == null || propertyPath == null || propertyPath.isBlank()) {
                return Optional.empty();
            }
            return Optional.ofNullable(getNestedPropertyValue(bean, propertyPath));
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    /**
     * 安全设置字段值。
     *
     * @param bean      Bean 对象
     * @param fieldName 字段名
     * @param value     字段值
     * @return 是否设置成功
     */
    public static boolean safeSetFieldValue(Object bean, String fieldName, Object value) {
        try {
            if (bean == null || fieldName == null || fieldName.isBlank()) {
                return false;
            }
            setFieldValue(bean, fieldName, value);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    /**
     * 安全设置属性值。
     *
     * @param bean         Bean 对象
     * @param propertyName 属性名
     * @param value        属性值
     * @return 是否设置成功
     */
    public static boolean safeSetPropertyValue(Object bean, String propertyName, Object value) {
        try {
            if (bean == null || propertyName == null || propertyName.isBlank()) {
                return false;
            }
            setPropertyValue(bean, propertyName, value);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    /**
     * 安全设置嵌套属性值。
     *
     * @param bean         Bean 对象
     * @param propertyPath 属性路径
     * @param value        属性值
     * @return 是否设置成功
     */
    public static boolean safeSetNestedPropertyValue(Object bean, String propertyPath, Object value) {
        try {
            if (bean == null || propertyPath == null || propertyPath.isBlank()) {
                return false;
            }
            setNestedPropertyValue(bean, propertyPath, value);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    /**
     * 安全创建实例。
     *
     * @param type 实例类型
     * @return 实例
     */
    public static <T> Optional<T> safeNewInstance(Class<T> type) {
        return tryNewInstance(type);
    }

    /**
     * 安全执行反射读取。
     *
     * @param supplier 读取逻辑
     * @return 读取结果
     */
    public static <T> Optional<T> safeAccess(Supplier<T> supplier) {
        try {
            if (supplier == null) {
                return Optional.empty();
            }
            return Optional.ofNullable(supplier.get());
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    /**
     * 安全执行反射操作。
     *
     * @param runnable 操作逻辑
     * @return 是否执行成功
     */
    public static boolean safeAccess(ReflectiveRunnable runnable) {
        try {
            if (runnable == null) {
                return false;
            }
            runnable.run();
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    /**
     * 获取方法列表，包含父类方法。
     *
     * @param beanClass Bean 类型
     * @return 方法列表
     */
    public static List<Method> getMethods(Class<?> beanClass) {
        requireClass(beanClass, "beanClass");
        return new ArrayList<>(METHOD_CACHE.get(beanClass));
    }

    /**
     * 查找无参方法。
     *
     * @param beanClass  Bean 类型
     * @param methodName 方法名
     * @return 方法对象
     */
    public static Optional<Method> findMethod(Class<?> beanClass, String methodName) {
        requireClass(beanClass, "beanClass");
        requireText(methodName, "methodName");

        return METHOD_CACHE.get(beanClass).stream()
                .filter(method -> method.getName().equals(methodName))
                .filter(method -> method.getParameterCount() == 0)
                .findFirst();
    }

    /**
     * 查找指定参数类型的方法。
     *
     * @param beanClass      Bean 类型
     * @param methodName     方法名
     * @param parameterTypes 参数类型
     * @return 方法对象
     */
    public static Optional<Method> findMethod(Class<?> beanClass, String methodName, Class<?>... parameterTypes) {
        requireClass(beanClass, "beanClass");
        requireText(methodName, "methodName");

        Class<?>[] actualParameterTypes = parameterTypes == null ? new Class<?>[0] : parameterTypes;
        return METHOD_CACHE.get(beanClass).stream()
                .filter(method -> method.getName().equals(methodName))
                .filter(method -> isSameParameterTypes(method.getParameterTypes(), actualParameterTypes))
                .findFirst();
    }

    /**
     * 安全调用方法。
     *
     * @param target     目标对象
     * @param methodName 方法名
     * @param args       参数
     * @return 调用结果
     */
    public static Optional<Object> safeInvokeMethod(Object target, String methodName, Object... args) {
        try {
            if (target == null || methodName == null || methodName.isBlank()) {
                return Optional.empty();
            }

            Object[] actualArgs = args == null ? new Object[0] : args;
            Method method = findCompatibleMethod(target.getClass(), methodName, actualArgs).orElse(null);
            if (method == null) {
                return Optional.empty();
            }

            return Optional.ofNullable(invokeMethod(method, target, actualArgs));
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    /**
     * 查找兼容参数的方法。
     *
     * @param beanClass  Bean 类型
     * @param methodName 方法名
     * @param args       参数
     * @return 方法对象
     */
    public static Optional<Method> findCompatibleMethod(Class<?> beanClass, String methodName, Object... args) {
        requireClass(beanClass, "beanClass");
        requireText(methodName, "methodName");

        Object[] actualArgs = args == null ? new Object[0] : args;
        return METHOD_CACHE.get(beanClass).stream()
                .filter(method -> method.getName().equals(methodName))
                .filter(method -> isCompatibleMethod(method, actualArgs))
                .findFirst();
    }

    private static boolean isCompatibleMethod(Method method, Object[] args) {
        Class<?>[] parameterTypes = method.getParameterTypes();
        if (parameterTypes.length != args.length) {
            return false;
        }

        for (int i = 0; i < parameterTypes.length; i++) {
            if (!isAssignableValue(parameterTypes[i], args[i])) {
                return false;
            }
        }

        return true;
    }

    private static boolean isSameParameterTypes(Class<?>[] sourceTypes, Class<?>[] targetTypes) {
        if (sourceTypes.length != targetTypes.length) {
            return false;
        }

        for (int i = 0; i < sourceTypes.length; i++) {
            if (!sourceTypes[i].equals(targetTypes[i])) {
                return false;
            }
        }

        return true;
    }

    /**
     * 缓存快照。
     *
     * @param beanClass        Bean 类型
     * @param fieldCount       字段数量
     * @param propertyCount    属性数量
     * @param methodCount      方法数量
     * @param constructorCount 构造方法数量
     * @author Ateng
     * @since 2026-04-29
     */
    public record CacheSnapshot(
            Class<?> beanClass,
            int fieldCount,
            int propertyCount,
            int methodCount,
            int constructorCount
    ) {
    }

    /**
     * 反射操作函数。
     *
     * @author Ateng
     * @since 2026-04-29
     */
    @FunctionalInterface
    public interface ReflectiveRunnable {

        /**
         * 执行反射操作。
         */
        void run();
    }

    /**
     * 比较两个 Bean 的同名可读属性。
     *
     * @param sourceBean 源 Bean
     * @param targetBean 目标 Bean
     * @return 差异列表
     */
    public static List<BeanDifference> compareProperties(Object sourceBean, Object targetBean) {
        return compareProperties(sourceBean, targetBean, BeanCompareOptions.defaults());
    }

    /**
     * 比较两个 Bean 的同名可读属性。
     *
     * @param sourceBean 源 Bean
     * @param targetBean 目标 Bean
     * @param options    比较配置
     * @return 差异列表
     */
    public static List<BeanDifference> compareProperties(Object sourceBean, Object targetBean, BeanCompareOptions options) {
        Objects.requireNonNull(sourceBean, "sourceBean不能为null");
        Objects.requireNonNull(targetBean, "targetBean不能为null");

        BeanCompareOptions actualOptions = options == null ? BeanCompareOptions.defaults() : options;
        Map<String, BeanProperty> sourcePropertyMap = getBeanPropertyMap(sourceBean.getClass());
        Map<String, BeanProperty> targetPropertyMap = getBeanPropertyMap(targetBean.getClass());
        List<BeanDifference> differences = new ArrayList<>();

        for (Map.Entry<String, BeanProperty> entry : sourcePropertyMap.entrySet()) {
            String propertyName = entry.getKey();
            BeanProperty sourceProperty = entry.getValue();

            if (!sourceProperty.readable()) {
                continue;
            }
            if (!shouldIncludeProperty(propertyName, actualOptions.includeProperties(), actualOptions.excludeProperties())) {
                continue;
            }

            BeanProperty targetProperty = targetPropertyMap.get(propertyName);
            if (targetProperty == null) {
                if (actualOptions.ignoreMissingProperty()) {
                    continue;
                }
                differences.add(new BeanDifference(propertyName, getPropertyValue(sourceBean, propertyName), null, DifferenceType.MISSING_TARGET_PROPERTY));
                continue;
            }
            if (!targetProperty.readable()) {
                if (actualOptions.ignoreUnreadableProperty()) {
                    continue;
                }
                differences.add(new BeanDifference(propertyName, getPropertyValue(sourceBean, propertyName), null, DifferenceType.UNREADABLE_TARGET_PROPERTY));
                continue;
            }

            Object sourceValue = getPropertyValue(sourceBean, propertyName);
            Object targetValue = getPropertyValue(targetBean, propertyName);

            if (actualOptions.ignoreBothNull() && sourceValue == null && targetValue == null) {
                continue;
            }
            if (actualOptions.ignoreBothEmpty() && isEmpty(sourceValue) && isEmpty(targetValue)) {
                continue;
            }

            boolean equals = actualOptions.comparator() == null
                    ? isSameValue(sourceValue, targetValue)
                    : actualOptions.comparator().test(sourceValue, targetValue);

            if (!equals) {
                differences.add(new BeanDifference(propertyName, sourceValue, targetValue, DifferenceType.VALUE_DIFFERENT));
            }
        }

        return differences;
    }

    /**
     * 比较指定属性。
     *
     * @param sourceBean   源 Bean
     * @param targetBean   目标 Bean
     * @param propertyName 属性名
     * @return 差异信息，属性值相同则返回空
     */
    public static Optional<BeanDifference> compareProperty(Object sourceBean, Object targetBean, String propertyName) {
        Objects.requireNonNull(sourceBean, "sourceBean不能为null");
        Objects.requireNonNull(targetBean, "targetBean不能为null");
        requireText(propertyName, "propertyName");

        if (!isReadableProperty(sourceBean.getClass(), propertyName)) {
            throw new BeanException("源属性不可读：" + sourceBean.getClass().getName() + "." + propertyName);
        }
        if (!hasProperty(targetBean.getClass(), propertyName)) {
            return Optional.of(new BeanDifference(propertyName, getPropertyValue(sourceBean, propertyName), null, DifferenceType.MISSING_TARGET_PROPERTY));
        }
        if (!isReadableProperty(targetBean.getClass(), propertyName)) {
            return Optional.of(new BeanDifference(propertyName, getPropertyValue(sourceBean, propertyName), null, DifferenceType.UNREADABLE_TARGET_PROPERTY));
        }

        Object sourceValue = getPropertyValue(sourceBean, propertyName);
        Object targetValue = getPropertyValue(targetBean, propertyName);

        if (isSameValue(sourceValue, targetValue)) {
            return Optional.empty();
        }

        return Optional.of(new BeanDifference(propertyName, sourceValue, targetValue, DifferenceType.VALUE_DIFFERENT));
    }

    /**
     * 判断两个 Bean 的同名可读属性是否一致。
     *
     * @param sourceBean 源 Bean
     * @param targetBean 目标 Bean
     * @return 是否一致
     */
    public static boolean equalsProperties(Object sourceBean, Object targetBean) {
        return compareProperties(sourceBean, targetBean).isEmpty();
    }

    /**
     * 判断两个 Bean 是否存在属性差异。
     *
     * @param sourceBean 源 Bean
     * @param targetBean 目标 Bean
     * @return 是否存在差异
     */
    public static boolean hasChanges(Object sourceBean, Object targetBean) {
        return !compareProperties(sourceBean, targetBean).isEmpty();
    }

    /**
     * 获取发生变化的属性名列表。
     *
     * @param sourceBean 源 Bean
     * @param targetBean 目标 Bean
     * @return 属性名列表
     */
    public static List<String> getChangedPropertyNames(Object sourceBean, Object targetBean) {
        return compareProperties(sourceBean, targetBean).stream()
                .map(BeanDifference::propertyName)
                .toList();
    }

    /**
     * 获取发生变化的属性值映射。
     *
     * @param sourceBean 源 Bean
     * @param targetBean 目标 Bean
     * @return 属性差异映射
     */
    public static Map<String, BeanDifference> getChangedPropertyMap(Object sourceBean, Object targetBean) {
        Map<String, BeanDifference> result = new LinkedHashMap<>();
        for (BeanDifference difference : compareProperties(sourceBean, targetBean)) {
            result.put(difference.propertyName(), difference);
        }
        return Collections.unmodifiableMap(result);
    }

    /**
     * 比较两个值是否一致，支持数组内容比较。
     *
     * @param sourceValue 源值
     * @param targetValue 目标值
     * @return 是否一致
     */
    public static boolean isSameValue(Object sourceValue, Object targetValue) {
        if (sourceValue == targetValue) {
            return true;
        }
        if (sourceValue == null || targetValue == null) {
            return false;
        }
        if (sourceValue.getClass().isArray() && targetValue.getClass().isArray()) {
            return isSameArrayValue(sourceValue, targetValue);
        }
        return Objects.equals(sourceValue, targetValue);
    }

    /**
     * 校验 Bean 不为 null。
     *
     * @param bean Bean 对象
     */
    public static void requireBean(Object bean) {
        if (bean == null) {
            throw new BeanException("Bean对象不能为null");
        }
    }

    /**
     * 校验属性存在。
     *
     * @param beanClass    Bean 类型
     * @param propertyName 属性名
     */
    public static void requireProperty(Class<?> beanClass, String propertyName) {
        requireClass(beanClass, "beanClass");
        requireText(propertyName, "propertyName");

        if (!hasProperty(beanClass, propertyName)) {
            throw new BeanException("属性不存在：" + beanClass.getName() + "." + propertyName);
        }
    }

    /**
     * 校验属性可读。
     *
     * @param beanClass    Bean 类型
     * @param propertyName 属性名
     */
    public static void requireReadableProperty(Class<?> beanClass, String propertyName) {
        requireProperty(beanClass, propertyName);

        if (!isReadableProperty(beanClass, propertyName)) {
            throw new BeanException("属性不可读：" + beanClass.getName() + "." + propertyName);
        }
    }

    /**
     * 校验属性可写。
     *
     * @param beanClass    Bean 类型
     * @param propertyName 属性名
     */
    public static void requireWritableProperty(Class<?> beanClass, String propertyName) {
        requireProperty(beanClass, propertyName);

        if (!isWritableProperty(beanClass, propertyName)) {
            throw new BeanException("属性不可写：" + beanClass.getName() + "." + propertyName);
        }
    }

    /**
     * 校验字段存在。
     *
     * @param beanClass Bean 类型
     * @param fieldName 字段名
     */
    public static void requireFieldExists(Class<?> beanClass, String fieldName) {
        requireClass(beanClass, "beanClass");
        requireText(fieldName, "fieldName");

        if (!hasField(beanClass, fieldName)) {
            throw new BeanException("字段不存在：" + beanClass.getName() + "." + fieldName);
        }
    }

    /**
     * 校验属性值不为 null。
     *
     * @param bean         Bean 对象
     * @param propertyName 属性名
     */
    public static void requireNotNullProperty(Object bean, String propertyName) {
        requireBean(bean);
        requireReadableProperty(bean.getClass(), propertyName);

        if (getPropertyValue(bean, propertyName) == null) {
            throw new BeanException("属性值不能为null：" + bean.getClass().getName() + "." + propertyName);
        }
    }

    /**
     * 校验属性值非空。
     *
     * @param bean         Bean 对象
     * @param propertyName 属性名
     */
    public static void requireNotEmptyProperty(Object bean, String propertyName) {
        requireBean(bean);
        requireReadableProperty(bean.getClass(), propertyName);

        if (isEmpty(getPropertyValue(bean, propertyName))) {
            throw new BeanException("属性值不能为空：" + bean.getClass().getName() + "." + propertyName);
        }
    }

    /**
     * 校验属性值非空白。
     *
     * @param bean         Bean 对象
     * @param propertyName 属性名
     */
    public static void requireNotBlankProperty(Object bean, String propertyName) {
        requireBean(bean);
        requireReadableProperty(bean.getClass(), propertyName);

        if (isBlankValue(getPropertyValue(bean, propertyName))) {
            throw new BeanException("属性值不能为空白：" + bean.getClass().getName() + "." + propertyName);
        }
    }

    /**
     * 获取 null 属性名列表。
     *
     * @param bean          Bean 对象
     * @param propertyNames 指定属性名集合
     * @return 属性名列表
     */
    public static List<String> getNullPropertyNames(Object bean, Collection<String> propertyNames) {
        requireBean(bean);

        if (propertyNames == null || propertyNames.isEmpty()) {
            return getNullPropertyNames(bean);
        }

        List<String> result = new ArrayList<>();
        for (String propertyName : propertyNames) {
            if (propertyName == null || propertyName.isBlank()) {
                continue;
            }
            requireReadableProperty(bean.getClass(), propertyName);
            if (getPropertyValue(bean, propertyName) == null) {
                result.add(propertyName);
            }
        }

        return result;
    }

    /**
     * 获取空属性名列表。
     *
     * @param bean          Bean 对象
     * @param propertyNames 指定属性名集合
     * @return 属性名列表
     */
    public static List<String> getEmptyPropertyNames(Object bean, Collection<String> propertyNames) {
        requireBean(bean);

        if (propertyNames == null || propertyNames.isEmpty()) {
            return getEmptyPropertyNames(bean);
        }

        List<String> result = new ArrayList<>();
        for (String propertyName : propertyNames) {
            if (propertyName == null || propertyName.isBlank()) {
                continue;
            }
            requireReadableProperty(bean.getClass(), propertyName);
            if (isEmpty(getPropertyValue(bean, propertyName))) {
                result.add(propertyName);
            }
        }

        return result;
    }

    /**
     * 获取空白属性名列表。
     *
     * @param bean          Bean 对象
     * @param propertyNames 指定属性名集合
     * @return 属性名列表
     */
    public static List<String> getBlankPropertyNames(Object bean, Collection<String> propertyNames) {
        requireBean(bean);

        Collection<String> actualPropertyNames = propertyNames == null || propertyNames.isEmpty()
                ? getReadablePropertyNames(bean.getClass())
                : propertyNames;

        List<String> result = new ArrayList<>();
        for (String propertyName : actualPropertyNames) {
            if (propertyName == null || propertyName.isBlank()) {
                continue;
            }
            requireReadableProperty(bean.getClass(), propertyName);
            if (isBlankValue(getPropertyValue(bean, propertyName))) {
                result.add(propertyName);
            }
        }

        return result;
    }

    /**
     * 校验必填属性不为 null。
     *
     * @param bean               Bean 对象
     * @param requiredProperties 必填属性名集合
     * @return 校验结果
     */
    public static BeanValidationResult validateRequiredProperties(Object bean, Collection<String> requiredProperties) {
        return validateBean(bean, new BeanValidationOptions(requiredProperties, Collections.emptyList(), Collections.emptyList(), true));
    }

    /**
     * 校验属性非空。
     *
     * @param bean               Bean 对象
     * @param requiredProperties 非空属性名集合
     * @return 校验结果
     */
    public static BeanValidationResult validateNotEmptyProperties(Object bean, Collection<String> requiredProperties) {
        return validateBean(bean, new BeanValidationOptions(Collections.emptyList(), requiredProperties, Collections.emptyList(), true));
    }

    /**
     * 校验属性非空白。
     *
     * @param bean               Bean 对象
     * @param requiredProperties 非空白属性名集合
     * @return 校验结果
     */
    public static BeanValidationResult validateNotBlankProperties(Object bean, Collection<String> requiredProperties) {
        return validateBean(bean, new BeanValidationOptions(Collections.emptyList(), Collections.emptyList(), requiredProperties, true));
    }

    /**
     * 校验 Bean。
     *
     * @param bean    Bean 对象
     * @param options 校验配置
     * @return 校验结果
     */
    public static BeanValidationResult validateBean(Object bean, BeanValidationOptions options) {
        if (bean == null) {
            return BeanValidationResult.invalid(List.of(new BeanViolation("", "Bean对象不能为null", null, ViolationType.BEAN_NULL)));
        }

        BeanValidationOptions actualOptions = options == null ? BeanValidationOptions.defaults() : options;
        List<BeanViolation> violations = new ArrayList<>();

        validateNotNullProperties(bean, actualOptions.notNullProperties(), actualOptions.ignoreUnknownProperty(), violations);
        validateNotEmptyProperties(bean, actualOptions.notEmptyProperties(), actualOptions.ignoreUnknownProperty(), violations);
        validateNotBlankProperties(bean, actualOptions.notBlankProperties(), actualOptions.ignoreUnknownProperty(), violations);

        return violations.isEmpty() ? BeanValidationResult.ok() : BeanValidationResult.invalid(violations);
    }

    /**
     * 断言 Bean 校验通过。
     *
     * @param bean    Bean 对象
     * @param options 校验配置
     */
    public static void assertValidBean(Object bean, BeanValidationOptions options) {
        BeanValidationResult result = validateBean(bean, options);
        if (!result.valid()) {
            throw new BeanException("Bean校验失败：" + result.firstMessage());
        }
    }

    /**
     * 判断 Bean 校验是否通过。
     *
     * @param bean    Bean 对象
     * @param options 校验配置
     * @return 是否通过
     */
    public static boolean isValidBean(Object bean, BeanValidationOptions options) {
        return validateBean(bean, options).valid();
    }

    private static void validateNotNullProperties(Object bean, Collection<String> propertyNames, boolean ignoreUnknownProperty, List<BeanViolation> violations) {
        if (propertyNames == null || propertyNames.isEmpty()) {
            return;
        }

        for (String propertyName : propertyNames) {
            if (!isValidValidationProperty(bean, propertyName, ignoreUnknownProperty, violations)) {
                continue;
            }

            Object value = getPropertyValue(bean, propertyName);
            if (value == null) {
                violations.add(new BeanViolation(propertyName, "属性值不能为null：" + propertyName, value, ViolationType.NULL_VALUE));
            }
        }
    }

    private static void validateNotEmptyProperties(Object bean, Collection<String> propertyNames, boolean ignoreUnknownProperty, List<BeanViolation> violations) {
        if (propertyNames == null || propertyNames.isEmpty()) {
            return;
        }

        for (String propertyName : propertyNames) {
            if (!isValidValidationProperty(bean, propertyName, ignoreUnknownProperty, violations)) {
                continue;
            }

            Object value = getPropertyValue(bean, propertyName);
            if (isEmpty(value)) {
                violations.add(new BeanViolation(propertyName, "属性值不能为空：" + propertyName, value, ViolationType.EMPTY_VALUE));
            }
        }
    }

    private static void validateNotBlankProperties(Object bean, Collection<String> propertyNames, boolean ignoreUnknownProperty, List<BeanViolation> violations) {
        if (propertyNames == null || propertyNames.isEmpty()) {
            return;
        }

        for (String propertyName : propertyNames) {
            if (!isValidValidationProperty(bean, propertyName, ignoreUnknownProperty, violations)) {
                continue;
            }

            Object value = getPropertyValue(bean, propertyName);
            if (isBlankValue(value)) {
                violations.add(new BeanViolation(propertyName, "属性值不能为空白：" + propertyName, value, ViolationType.BLANK_VALUE));
            }
        }
    }

    private static boolean isValidValidationProperty(Object bean, String propertyName, boolean ignoreUnknownProperty, List<BeanViolation> violations) {
        if (propertyName == null || propertyName.isBlank()) {
            violations.add(new BeanViolation("", "属性名不能为空", null, ViolationType.INVALID_PROPERTY_NAME));
            return false;
        }

        Class<?> beanClass = bean.getClass();
        if (!hasProperty(beanClass, propertyName)) {
            if (!ignoreUnknownProperty) {
                violations.add(new BeanViolation(propertyName, "属性不存在：" + propertyName, null, ViolationType.UNKNOWN_PROPERTY));
            }
            return false;
        }

        if (!isReadableProperty(beanClass, propertyName)) {
            if (!ignoreUnknownProperty) {
                violations.add(new BeanViolation(propertyName, "属性不可读：" + propertyName, null, ViolationType.UNREADABLE_PROPERTY));
            }
            return false;
        }

        return true;
    }

    private static boolean isSameArrayValue(Object sourceArray, Object targetArray) {
        int sourceLength = Array.getLength(sourceArray);
        int targetLength = Array.getLength(targetArray);

        if (sourceLength != targetLength) {
            return false;
        }

        for (int i = 0; i < sourceLength; i++) {
            Object sourceItem = Array.get(sourceArray, i);
            Object targetItem = Array.get(targetArray, i);
            if (!isSameValue(sourceItem, targetItem)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Bean 差异信息。
     *
     * @param propertyName 属性名
     * @param sourceValue  源值
     * @param targetValue  目标值
     * @param type         差异类型
     * @author Ateng
     * @since 2026-04-29
     */
    public record BeanDifference(
            String propertyName,
            Object sourceValue,
            Object targetValue,
            DifferenceType type
    ) {
    }

    /**
     * Bean 比较配置。
     *
     * @param ignoreMissingProperty    是否忽略目标不存在的属性
     * @param ignoreUnreadableProperty 是否忽略目标不可读属性
     * @param ignoreBothNull           是否忽略两边都为 null 的属性
     * @param ignoreBothEmpty          是否忽略两边都为空的属性
     * @param includeProperties        仅包含的属性名集合，为空表示不过滤
     * @param excludeProperties        排除的属性名集合
     * @param comparator               自定义值比较器
     * @author Ateng
     * @since 2026-04-29
     */
    public record BeanCompareOptions(
            boolean ignoreMissingProperty,
            boolean ignoreUnreadableProperty,
            boolean ignoreBothNull,
            boolean ignoreBothEmpty,
            Collection<String> includeProperties,
            Collection<String> excludeProperties,
            BiPredicate<Object, Object> comparator
    ) {

        /**
         * 默认配置。
         *
         * @return Bean 比较配置
         */
        public static BeanCompareOptions defaults() {
            return new BeanCompareOptions(true, true, true, false, Collections.emptyList(), Collections.emptyList(), null);
        }

        /**
         * 严格配置。
         *
         * @return Bean 比较配置
         */
        public static BeanCompareOptions strict() {
            return new BeanCompareOptions(false, false, false, false, Collections.emptyList(), Collections.emptyList(), null);
        }
    }

    /**
     * Bean 校验配置。
     *
     * @param notNullProperties     不能为 null 的属性名集合
     * @param notEmptyProperties    不能为空的属性名集合
     * @param notBlankProperties    不能为空白的属性名集合
     * @param ignoreUnknownProperty 是否忽略不存在或不可读属性
     * @author Ateng
     * @since 2026-04-29
     */
    public record BeanValidationOptions(
            Collection<String> notNullProperties,
            Collection<String> notEmptyProperties,
            Collection<String> notBlankProperties,
            boolean ignoreUnknownProperty
    ) {

        /**
         * 默认配置。
         *
         * @return Bean 校验配置
         */
        public static BeanValidationOptions defaults() {
            return new BeanValidationOptions(Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), true);
        }

        /**
         * 严格配置。
         *
         * @return Bean 校验配置
         */
        public static BeanValidationOptions strict() {
            return new BeanValidationOptions(Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), false);
        }
    }

    /**
     * Bean 校验结果。
     *
     * @param valid      是否校验通过
     * @param violations 违规信息列表
     * @author Ateng
     * @since 2026-04-29
     */
    public record BeanValidationResult(
            boolean valid,
            List<BeanViolation> violations
    ) {

        /**
         * 创建校验通过结果。
         *
         * @return 校验结果
         */
        public static BeanValidationResult ok() {
            return new BeanValidationResult(true, Collections.emptyList());
        }

        /**
         * 创建校验失败结果。
         *
         * @param violations 违规信息列表
         * @return 校验结果
         */
        public static BeanValidationResult invalid(List<BeanViolation> violations) {
            return new BeanValidationResult(false, violations == null ? Collections.emptyList() : List.copyOf(violations));
        }

        /**
         * 获取第一条错误消息。
         *
         * @return 错误消息
         */
        public String firstMessage() {
            if (violations == null || violations.isEmpty()) {
                return "";
            }
            return violations.getFirst().message();
        }

        /**
         * 获取错误消息列表。
         *
         * @return 错误消息列表
         */
        public List<String> messages() {
            if (violations == null || violations.isEmpty()) {
                return Collections.emptyList();
            }
            return violations.stream()
                    .map(BeanViolation::message)
                    .toList();
        }
    }

    /**
     * Bean 校验违规信息。
     *
     * @param propertyName 属性名
     * @param message      违规消息
     * @param value        当前值
     * @param type         违规类型
     * @author Ateng
     * @since 2026-04-29
     */
    public record BeanViolation(
            String propertyName,
            String message,
            Object value,
            ViolationType type
    ) {
    }

    /**
     * 差异类型。
     *
     * @author Ateng
     * @since 2026-04-29
     */
    public enum DifferenceType {

        /**
         * 值不同。
         */
        VALUE_DIFFERENT,

        /**
         * 目标属性不存在。
         */
        MISSING_TARGET_PROPERTY,

        /**
         * 目标属性不可读。
         */
        UNREADABLE_TARGET_PROPERTY
    }

    /**
     * 校验违规类型。
     *
     * @author Ateng
     * @since 2026-04-29
     */
    public enum ViolationType {

        /**
         * Bean 对象为 null。
         */
        BEAN_NULL,

        /**
         * 属性名无效。
         */
        INVALID_PROPERTY_NAME,

        /**
         * 属性不存在。
         */
        UNKNOWN_PROPERTY,

        /**
         * 属性不可读。
         */
        UNREADABLE_PROPERTY,

        /**
         * 属性值为 null。
         */
        NULL_VALUE,

        /**
         * 属性值为空。
         */
        EMPTY_VALUE,

        /**
         * 属性值为空白。
         */
        BLANK_VALUE
    }
}