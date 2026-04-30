package io.github.atengk.utils.annotation;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.RecordComponent;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Predicate;

/**
 * JDK 原生注解工具类，提供注解判断、查找、属性读取、元注解解析、合并、缓存等通用能力。
 *
 * @author Ateng
 * @since 2026-04-30
 */
public final class AnnotationUtil {

    private static final String VALUE_ATTRIBUTE = "value";
    private static final Set<String> OBJECT_METHOD_NAMES = Set.of("equals", "hashCode", "toString", "annotationType");
    private static final Map<AnnotatedElement, Annotation[]> ANNOTATION_CACHE = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<Annotation, Map<String, Object>> ATTRIBUTE_CACHE = Collections.synchronizedMap(new WeakHashMap<>());
    private static volatile boolean cacheEnabled = true;

    private AnnotationUtil() {
        throw new UnsupportedOperationException("AnnotationUtil 是静态工具类，不能被实例化");
    }

    /**
     * 判断元素上是否存在指定注解。
     *
     * @param element 元素
     * @param annotationType 注解类型
     * @return 存在返回 true，否则返回 false
     */
    public static boolean hasAnnotation(AnnotatedElement element, Class<? extends Annotation> annotationType) {
        return getAnnotation(element, annotationType) != null;
    }

    /**
     * 判断元素上是否存在任意一个指定注解。
     *
     * @param element 元素
     * @param annotationTypes 注解类型数组
     * @return 存在任意一个返回 true，否则返回 false
     */
    @SafeVarargs
    public static boolean hasAnyAnnotation(AnnotatedElement element, Class<? extends Annotation>... annotationTypes) {
        if (element == null || annotationTypes == null || annotationTypes.length == 0) {
            return false;
        }
        for (Class<? extends Annotation> annotationType : annotationTypes) {
            if (hasAnnotation(element, annotationType)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断元素上是否同时存在全部指定注解。
     *
     * @param element 元素
     * @param annotationTypes 注解类型数组
     * @return 全部存在返回 true，否则返回 false
     */
    @SafeVarargs
    public static boolean hasAllAnnotations(AnnotatedElement element, Class<? extends Annotation>... annotationTypes) {
        if (element == null || annotationTypes == null || annotationTypes.length == 0) {
            return false;
        }
        for (Class<? extends Annotation> annotationType : annotationTypes) {
            if (!hasAnnotation(element, annotationType)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 判断元素上是否直接声明了指定注解。
     *
     * @param element 元素
     * @param annotationType 注解类型
     * @return 直接声明返回 true，否则返回 false
     */
    public static boolean hasDeclaredAnnotation(AnnotatedElement element, Class<? extends Annotation> annotationType) {
        return getDeclaredAnnotation(element, annotationType) != null;
    }

    /**
     * 获取元素上的指定注解。
     *
     * @param element 元素
     * @param annotationType 注解类型
     * @param <A> 注解泛型
     * @return 注解实例；不存在或参数非法时返回 null
     */
    public static <A extends Annotation> A getAnnotation(AnnotatedElement element, Class<A> annotationType) {
        if (element == null || !isAnnotationType(annotationType)) {
            return null;
        }
        try {
            return element.getAnnotation(annotationType);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    /**
     * 查找元素上的指定注解。
     *
     * @param element 元素
     * @param annotationType 注解类型
     * @param <A> 注解泛型
     * @return 注解 Optional
     */
    public static <A extends Annotation> Optional<A> findAnnotation(AnnotatedElement element, Class<A> annotationType) {
        return Optional.ofNullable(getAnnotation(element, annotationType));
    }

    /**
     * 获取元素上直接声明的指定注解。
     *
     * @param element 元素
     * @param annotationType 注解类型
     * @param <A> 注解泛型
     * @return 注解实例；不存在或参数非法时返回 null
     */
    public static <A extends Annotation> A getDeclaredAnnotation(AnnotatedElement element, Class<A> annotationType) {
        if (element == null || !isAnnotationType(annotationType)) {
            return null;
        }
        try {
            return element.getDeclaredAnnotation(annotationType);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    /**
     * 获取元素上的全部注解。
     *
     * @param element 元素
     * @return 注解数组；参数非法时返回空数组
     */
    public static Annotation[] getAnnotations(AnnotatedElement element) {
        if (element == null) {
            return new Annotation[0];
        }
        try {
            return element.getAnnotations();
        } catch (RuntimeException ex) {
            return new Annotation[0];
        }
    }

    /**
     * 获取元素上直接声明的全部注解。
     *
     * @param element 元素
     * @return 注解数组；参数非法时返回空数组
     */
    public static Annotation[] getDeclaredAnnotations(AnnotatedElement element) {
        if (element == null) {
            return new Annotation[0];
        }
        try {
            return element.getDeclaredAnnotations();
        } catch (RuntimeException ex) {
            return new Annotation[0];
        }
    }

    /**
     * 获取元素上的指定类型注解，支持可重复注解展开。
     *
     * @param element 元素
     * @param annotationType 注解类型
     * @param <A> 注解泛型
     * @return 注解列表
     */
    public static <A extends Annotation> List<A> getAnnotationsByType(AnnotatedElement element, Class<A> annotationType) {
        if (element == null || !isAnnotationType(annotationType)) {
            return List.of();
        }
        try {
            return List.of(element.getAnnotationsByType(annotationType));
        } catch (RuntimeException ex) {
            return List.of();
        }
    }

    /**
     * 获取元素上直接声明的指定类型注解，支持可重复注解展开。
     *
     * @param element 元素
     * @param annotationType 注解类型
     * @param <A> 注解泛型
     * @return 注解列表
     */
    public static <A extends Annotation> List<A> getDeclaredAnnotationsByType(AnnotatedElement element, Class<A> annotationType) {
        if (element == null || !isAnnotationType(annotationType)) {
            return List.of();
        }
        try {
            return List.of(element.getDeclaredAnnotationsByType(annotationType));
        } catch (RuntimeException ex) {
            return List.of();
        }
    }

    /**
     * 查找类上的指定注解。
     *
     * @param targetClass 目标类
     * @param annotationType 注解类型
     * @param <A> 注解泛型
     * @return 注解 Optional
     */
    public static <A extends Annotation> Optional<A> findClassAnnotation(Class<?> targetClass, Class<A> annotationType) {
        return findAnnotation(targetClass, annotationType);
    }

    /**
     * 沿父类继承链查找指定注解。
     *
     * @param targetClass 目标类
     * @param annotationType 注解类型
     * @param <A> 注解泛型
     * @return 注解 Optional
     */
    public static <A extends Annotation> Optional<A> findInheritedAnnotation(Class<?> targetClass, Class<A> annotationType) {
        if (targetClass == null || !isAnnotationType(annotationType)) {
            return Optional.empty();
        }
        Class<?> current = targetClass;
        while (current != null && current != Object.class) {
            A annotation = getDeclaredAnnotation(current, annotationType);
            if (annotation != null) {
                return Optional.of(annotation);
            }
            current = current.getSuperclass();
        }
        return Optional.empty();
    }

    /**
     * 从当前类、父类和接口层级中查找指定注解。
     *
     * @param targetClass 目标类
     * @param annotationType 注解类型
     * @param <A> 注解泛型
     * @return 注解 Optional
     */
    public static <A extends Annotation> Optional<A> findAnnotationInHierarchy(Class<?> targetClass, Class<A> annotationType) {
        if (targetClass == null || !isAnnotationType(annotationType)) {
            return Optional.empty();
        }
        for (Class<?> type : collectTypeHierarchy(targetClass)) {
            A annotation = getDeclaredAnnotation(type, annotationType);
            if (annotation != null) {
                return Optional.of(annotation);
            }
        }
        return Optional.empty();
    }

    /**
     * 获取类上的全部注解。
     *
     * @param targetClass 目标类
     * @return 注解数组
     */
    public static Annotation[] getClassAnnotations(Class<?> targetClass) {
        return getAnnotations(targetClass);
    }

    /**
     * 获取类上直接声明的全部注解。
     *
     * @param targetClass 目标类
     * @return 注解数组
     */
    public static Annotation[] getDeclaredClassAnnotations(Class<?> targetClass) {
        return getDeclaredAnnotations(targetClass);
    }

    /**
     * 判断类上是否存在指定注解。
     *
     * @param targetClass 目标类
     * @param annotationType 注解类型
     * @return 存在返回 true，否则返回 false
     */
    public static boolean hasClassAnnotation(Class<?> targetClass, Class<? extends Annotation> annotationType) {
        return hasAnnotation(targetClass, annotationType);
    }

    /**
     * 判断类、父类或接口层级中是否存在指定注解。
     *
     * @param targetClass 目标类
     * @param annotationType 注解类型
     * @return 存在返回 true，否则返回 false
     */
    public static boolean hasAnnotationInHierarchy(Class<?> targetClass, Class<? extends Annotation> annotationType) {
        return findAnnotationInHierarchy(targetClass, annotationType).isPresent();
    }

    /**
     * 查找方法上的指定注解。
     *
     * @param method 方法
     * @param annotationType 注解类型
     * @param <A> 注解泛型
     * @return 注解 Optional
     */
    public static <A extends Annotation> Optional<A> findMethodAnnotation(Method method, Class<A> annotationType) {
        return findAnnotation(method, annotationType);
    }

    /**
     * 根据方法名查找类中方法上的指定注解。
     *
     * @param targetClass 目标类
     * @param methodName 方法名
     * @param annotationType 注解类型
     * @param <A> 注解泛型
     * @return 注解 Optional
     */
    public static <A extends Annotation> Optional<A> findMethodAnnotation(Class<?> targetClass, String methodName, Class<A> annotationType) {
        if (targetClass == null || isBlank(methodName) || !isAnnotationType(annotationType)) {
            return Optional.empty();
        }
        for (Method method : getAllMethods(targetClass, true)) {
            if (method.getName().equals(methodName)) {
                A annotation = getAnnotation(method, annotationType);
                if (annotation != null) {
                    return Optional.of(annotation);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * 从当前方法、父类方法和接口方法中查找指定注解。
     *
     * @param targetClass 目标类
     * @param method 方法
     * @param annotationType 注解类型
     * @param <A> 注解泛型
     * @return 注解 Optional
     */
    public static <A extends Annotation> Optional<A> findMethodAnnotationInHierarchy(Class<?> targetClass, Method method, Class<A> annotationType) {
        if (method == null || !isAnnotationType(annotationType)) {
            return Optional.empty();
        }
        Class<?> startClass = targetClass == null ? method.getDeclaringClass() : targetClass;
        for (Class<?> type : collectTypeHierarchy(startClass)) {
            Method matchedMethod = findDeclaredMethod(type, method.getName(), method.getParameterTypes());
            if (matchedMethod != null) {
                A annotation = getDeclaredAnnotation(matchedMethod, annotationType);
                if (annotation != null) {
                    return Optional.of(annotation);
                }
            }
        }
        return findMethodAnnotation(method, annotationType);
    }

    /**
     * 获取方法上的全部注解。
     *
     * @param method 方法
     * @return 注解数组
     */
    public static Annotation[] getMethodAnnotations(Method method) {
        return getAnnotations(method);
    }

    /**
     * 获取方法上直接声明的全部注解。
     *
     * @param method 方法
     * @return 注解数组
     */
    public static Annotation[] getDeclaredMethodAnnotations(Method method) {
        return getDeclaredAnnotations(method);
    }

    /**
     * 判断方法上是否存在指定注解。
     *
     * @param method 方法
     * @param annotationType 注解类型
     * @return 存在返回 true，否则返回 false
     */
    public static boolean hasMethodAnnotation(Method method, Class<? extends Annotation> annotationType) {
        return hasAnnotation(method, annotationType);
    }

    /**
     * 判断方法、父类方法或接口方法中是否存在指定注解。
     *
     * @param targetClass 目标类
     * @param method 方法
     * @param annotationType 注解类型
     * @return 存在返回 true，否则返回 false
     */
    public static boolean hasMethodAnnotationInHierarchy(Class<?> targetClass, Method method, Class<? extends Annotation> annotationType) {
        return findMethodAnnotationInHierarchy(targetClass, method, annotationType).isPresent();
    }

    /**
     * 查找字段上的指定注解。
     *
     * @param field 字段
     * @param annotationType 注解类型
     * @param <A> 注解泛型
     * @return 注解 Optional
     */
    public static <A extends Annotation> Optional<A> findFieldAnnotation(Field field, Class<A> annotationType) {
        return findAnnotation(field, annotationType);
    }

    /**
     * 根据字段名查找类中字段上的指定注解。
     *
     * @param targetClass 目标类
     * @param fieldName 字段名
     * @param annotationType 注解类型
     * @param <A> 注解泛型
     * @return 注解 Optional
     */
    public static <A extends Annotation> Optional<A> findFieldAnnotation(Class<?> targetClass, String fieldName, Class<A> annotationType) {
        if (targetClass == null || isBlank(fieldName) || !isAnnotationType(annotationType)) {
            return Optional.empty();
        }
        Field field = findDeclaredField(targetClass, fieldName);
        return findFieldAnnotation(field, annotationType);
    }

    /**
     * 从当前类和父类字段中查找指定注解。
     *
     * @param targetClass 目标类
     * @param fieldName 字段名
     * @param annotationType 注解类型
     * @param <A> 注解泛型
     * @return 注解 Optional
     */
    public static <A extends Annotation> Optional<A> findFieldAnnotationInHierarchy(Class<?> targetClass, String fieldName, Class<A> annotationType) {
        if (targetClass == null || isBlank(fieldName) || !isAnnotationType(annotationType)) {
            return Optional.empty();
        }
        Class<?> current = targetClass;
        while (current != null && current != Object.class) {
            Field field = findDeclaredField(current, fieldName);
            A annotation = getDeclaredAnnotation(field, annotationType);
            if (annotation != null) {
                return Optional.of(annotation);
            }
            current = current.getSuperclass();
        }
        return Optional.empty();
    }

    /**
     * 获取字段上的全部注解。
     *
     * @param field 字段
     * @return 注解数组
     */
    public static Annotation[] getFieldAnnotations(Field field) {
        return getAnnotations(field);
    }

    /**
     * 获取字段上直接声明的全部注解。
     *
     * @param field 字段
     * @return 注解数组
     */
    public static Annotation[] getDeclaredFieldAnnotations(Field field) {
        return getDeclaredAnnotations(field);
    }

    /**
     * 判断字段上是否存在指定注解。
     *
     * @param field 字段
     * @param annotationType 注解类型
     * @return 存在返回 true，否则返回 false
     */
    public static boolean hasFieldAnnotation(Field field, Class<? extends Annotation> annotationType) {
        return hasAnnotation(field, annotationType);
    }

    /**
     * 获取当前类中标记了指定注解的字段。
     *
     * @param targetClass 目标类
     * @param annotationType 注解类型
     * @return 字段列表
     */
    public static List<Field> getAnnotatedFields(Class<?> targetClass, Class<? extends Annotation> annotationType) {
        if (targetClass == null || !isAnnotationType(annotationType)) {
            return List.of();
        }
        List<Field> result = new ArrayList<>();
        for (Field field : targetClass.getDeclaredFields()) {
            if (hasAnnotation(field, annotationType)) {
                result.add(field);
            }
        }
        return List.copyOf(result);
    }

    /**
     * 获取当前类和父类中标记了指定注解的字段。
     *
     * @param targetClass 目标类
     * @param annotationType 注解类型
     * @return 字段列表
     */
    public static List<Field> getAllAnnotatedFields(Class<?> targetClass, Class<? extends Annotation> annotationType) {
        if (targetClass == null || !isAnnotationType(annotationType)) {
            return List.of();
        }
        List<Field> result = new ArrayList<>();
        Class<?> current = targetClass;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (hasAnnotation(field, annotationType)) {
                    result.add(field);
                }
            }
            current = current.getSuperclass();
        }
        return List.copyOf(result);
    }

    /**
     * 查找构造器上的指定注解。
     *
     * @param constructor 构造器
     * @param annotationType 注解类型
     * @param <A> 注解泛型
     * @return 注解 Optional
     */
    public static <A extends Annotation> Optional<A> findConstructorAnnotation(Constructor<?> constructor, Class<A> annotationType) {
        return findAnnotation(constructor, annotationType);
    }

    /**
     * 获取构造器上的全部注解。
     *
     * @param constructor 构造器
     * @return 注解数组
     */
    public static Annotation[] getConstructorAnnotations(Constructor<?> constructor) {
        return getAnnotations(constructor);
    }

    /**
     * 查找参数上的指定注解。
     *
     * @param parameter 参数
     * @param annotationType 注解类型
     * @param <A> 注解泛型
     * @return 注解 Optional
     */
    public static <A extends Annotation> Optional<A> findParameterAnnotation(Parameter parameter, Class<A> annotationType) {
        return findAnnotation(parameter, annotationType);
    }

    /**
     * 获取参数上的全部注解。
     *
     * @param parameter 参数
     * @return 注解数组
     */
    public static Annotation[] getParameterAnnotations(Parameter parameter) {
        return getAnnotations(parameter);
    }

    /**
     * 获取方法或构造器中带指定注解的参数。
     *
     * @param executable 方法或构造器
     * @param annotationType 注解类型
     * @return 参数列表
     */
    public static List<Parameter> getAnnotatedParameters(Executable executable, Class<? extends Annotation> annotationType) {
        if (executable == null || !isAnnotationType(annotationType)) {
            return List.of();
        }
        List<Parameter> result = new ArrayList<>();
        for (Parameter parameter : executable.getParameters()) {
            if (hasAnnotation(parameter, annotationType)) {
                result.add(parameter);
            }
        }
        return List.copyOf(result);
    }

    /**
     * 查找 Record 组件上的指定注解。
     *
     * @param component Record 组件
     * @param annotationType 注解类型
     * @param <A> 注解泛型
     * @return 注解 Optional
     */
    public static <A extends Annotation> Optional<A> findRecordComponentAnnotation(RecordComponent component, Class<A> annotationType) {
        return findAnnotation(component, annotationType);
    }

    /**
     * 获取 Record 组件上的全部注解。
     *
     * @param component Record 组件
     * @return 注解数组
     */
    public static Annotation[] getRecordComponentAnnotations(RecordComponent component) {
        return getAnnotations(component);
    }

    /**
     * 获取 Record 中带指定注解的组件。
     *
     * @param recordClass Record 类型
     * @param annotationType 注解类型
     * @return Record 组件列表
     */
    public static List<RecordComponent> getAnnotatedRecordComponents(Class<?> recordClass, Class<? extends Annotation> annotationType) {
        if (recordClass == null || !recordClass.isRecord() || !isAnnotationType(annotationType)) {
            return List.of();
        }
        List<RecordComponent> result = new ArrayList<>();
        for (RecordComponent component : recordClass.getRecordComponents()) {
            if (hasAnnotation(component, annotationType)) {
                result.add(component);
            }
        }
        return List.copyOf(result);
    }

    /**
     * 判断注解类型上是否存在指定元注解，支持递归查找。
     *
     * @param annotationType 注解类型
     * @param metaAnnotationType 元注解类型
     * @return 存在返回 true，否则返回 false
     */
    public static boolean hasMetaAnnotation(Class<? extends Annotation> annotationType, Class<? extends Annotation> metaAnnotationType) {
        return findMetaAnnotation(annotationType, metaAnnotationType).isPresent();
    }

    /**
     * 查找注解类型上的指定元注解，支持递归查找。
     *
     * @param annotationType 注解类型
     * @param metaAnnotationType 元注解类型
     * @param <A> 元注解泛型
     * @return 元注解 Optional
     */
    public static <A extends Annotation> Optional<A> findMetaAnnotation(Class<? extends Annotation> annotationType, Class<A> metaAnnotationType) {
        if (!isAnnotationType(annotationType) || !isAnnotationType(metaAnnotationType)) {
            return Optional.empty();
        }
        Set<Class<? extends Annotation>> visited = new HashSet<>();
        Queue<Class<? extends Annotation>> queue = new ArrayDeque<>();
        queue.add(annotationType);
        visited.add(annotationType);
        while (!queue.isEmpty()) {
            Class<? extends Annotation> current = queue.poll();
            for (Annotation annotation : current.getDeclaredAnnotations()) {
                Class<? extends Annotation> currentMetaType = annotation.annotationType();
                if (currentMetaType.equals(metaAnnotationType)) {
                    return Optional.of(metaAnnotationType.cast(annotation));
                }
                if (!isJdkAnnotation(currentMetaType) && visited.add(currentMetaType)) {
                    queue.add(currentMetaType);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * 查找元素上的直接注解或组合注解中的指定注解。
     *
     * @param element 元素
     * @param annotationType 注解类型
     * @param <A> 注解泛型
     * @return 注解 Optional
     */
    public static <A extends Annotation> Optional<A> findMergedAnnotation(AnnotatedElement element, Class<A> annotationType) {
        if (element == null || !isAnnotationType(annotationType)) {
            return Optional.empty();
        }
        A direct = getAnnotation(element, annotationType);
        if (direct != null) {
            return Optional.of(direct);
        }
        for (Annotation annotation : getAnnotations(element)) {
            Optional<A> metaAnnotation = findMetaAnnotation(annotation.annotationType(), annotationType);
            if (metaAnnotation.isPresent()) {
                return metaAnnotation;
            }
        }
        return Optional.empty();
    }

    /**
     * 查找被指定元注解标记的直接注解。
     *
     * @param element 元素
     * @param metaAnnotationType 元注解类型
     * @return 注解 Optional
     */
    public static Optional<Annotation> findAnnotationByMeta(AnnotatedElement element, Class<? extends Annotation> metaAnnotationType) {
        if (element == null || !isAnnotationType(metaAnnotationType)) {
            return Optional.empty();
        }
        for (Annotation annotation : getAnnotations(element)) {
            if (hasMetaAnnotation(annotation.annotationType(), metaAnnotationType)) {
                return Optional.of(annotation);
            }
        }
        return Optional.empty();
    }

    /**
     * 获取注解类型上的直接元注解。
     *
     * @param annotationType 注解类型
     * @return 元注解列表
     */
    public static List<Annotation> getMetaAnnotations(Class<? extends Annotation> annotationType) {
        if (!isAnnotationType(annotationType)) {
            return List.of();
        }
        return List.of(annotationType.getDeclaredAnnotations());
    }

    /**
     * 判断注解实例是否包含指定元注解。
     *
     * @param annotation 注解实例
     * @param metaAnnotationType 元注解类型
     * @return 包含返回 true，否则返回 false
     */
    public static boolean isMetaAnnotation(Annotation annotation, Class<? extends Annotation> metaAnnotationType) {
        return annotation != null && hasMetaAnnotation(annotation.annotationType(), metaAnnotationType);
    }

    /**
     * 递归解析注解类型上的非 JDK 元注解集合。
     *
     * @param annotationType 注解类型
     * @return 元注解集合
     */
    public static Set<Annotation> resolveMetaAnnotations(Class<? extends Annotation> annotationType) {
        if (!isAnnotationType(annotationType)) {
            return Set.of();
        }
        Map<Class<? extends Annotation>, Annotation> result = new LinkedHashMap<>();
        Set<Class<? extends Annotation>> visited = new HashSet<>();
        Queue<Class<? extends Annotation>> queue = new ArrayDeque<>();
        queue.add(annotationType);
        visited.add(annotationType);
        while (!queue.isEmpty()) {
            Class<? extends Annotation> current = queue.poll();
            for (Annotation annotation : current.getDeclaredAnnotations()) {
                Class<? extends Annotation> metaType = annotation.annotationType();
                if (isJdkAnnotation(metaType) || !visited.add(metaType)) {
                    continue;
                }
                result.put(metaType, annotation);
                queue.add(metaType);
            }
        }
        return Set.copyOf(result.values());
    }

    /**
     * 获取元素上被指定元注解标记的注解。
     *
     * @param element 元素
     * @param metaAnnotationType 元注解类型
     * @return 注解列表
     */
    public static List<Annotation> getAnnotationsByMeta(AnnotatedElement element, Class<? extends Annotation> metaAnnotationType) {
        if (element == null || !isAnnotationType(metaAnnotationType)) {
            return List.of();
        }
        List<Annotation> result = new ArrayList<>();
        for (Annotation annotation : getAnnotations(element)) {
            if (hasMetaAnnotation(annotation.annotationType(), metaAnnotationType)) {
                result.add(annotation);
            }
        }
        return List.copyOf(result);
    }

    /**
     * 获取指定类型的可重复注解。
     *
     * @param element 元素
     * @param annotationType 注解类型
     * @param <A> 注解泛型
     * @return 注解列表
     */
    public static <A extends Annotation> List<A> getRepeatableAnnotations(AnnotatedElement element, Class<A> annotationType) {
        return getAnnotationsByType(element, annotationType);
    }

    /**
     * 获取直接声明的指定类型可重复注解。
     *
     * @param element 元素
     * @param annotationType 注解类型
     * @param <A> 注解泛型
     * @return 注解列表
     */
    public static <A extends Annotation> List<A> getDeclaredRepeatableAnnotations(AnnotatedElement element, Class<A> annotationType) {
        return getDeclaredAnnotationsByType(element, annotationType);
    }

    /**
     * 判断元素上是否存在指定可重复注解。
     *
     * @param element 元素
     * @param annotationType 注解类型
     * @return 存在返回 true，否则返回 false
     */
    public static boolean hasRepeatableAnnotation(AnnotatedElement element, Class<? extends Annotation> annotationType) {
        return !getRepeatableAnnotations(element, annotationType).isEmpty();
    }

    /**
     * 在类层级中查找指定可重复注解。
     *
     * @param targetClass 目标类
     * @param annotationType 注解类型
     * @param <A> 注解泛型
     * @return 注解列表
     */
    public static <A extends Annotation> List<A> findRepeatableAnnotationsInHierarchy(Class<?> targetClass, Class<A> annotationType) {
        if (targetClass == null || !isAnnotationType(annotationType)) {
            return List.of();
        }
        List<A> result = new ArrayList<>();
        for (Class<?> type : collectTypeHierarchy(targetClass)) {
            result.addAll(getDeclaredAnnotationsByType(type, annotationType));
        }
        return List.copyOf(result);
    }

    /**
     * 在方法层级中查找指定可重复注解。
     *
     * @param targetClass 目标类
     * @param method 方法
     * @param annotationType 注解类型
     * @param <A> 注解泛型
     * @return 注解列表
     */
    public static <A extends Annotation> List<A> findMethodRepeatableAnnotationsInHierarchy(Class<?> targetClass, Method method, Class<A> annotationType) {
        if (method == null || !isAnnotationType(annotationType)) {
            return List.of();
        }
        Class<?> startClass = targetClass == null ? method.getDeclaringClass() : targetClass;
        List<A> result = new ArrayList<>();
        for (Class<?> type : collectTypeHierarchy(startClass)) {
            Method matchedMethod = findDeclaredMethod(type, method.getName(), method.getParameterTypes());
            if (matchedMethod != null) {
                result.addAll(getDeclaredAnnotationsByType(matchedMethod, annotationType));
            }
        }
        return List.copyOf(result);
    }

    /**
     * 读取注解的指定属性值。
     *
     * @param annotation 注解实例
     * @param attributeName 属性名
     * @return 属性值 Optional
     */
    public static Optional<Object> readAttribute(Annotation annotation, String attributeName) {
        if (annotation == null || isBlank(attributeName)) {
            return Optional.empty();
        }
        try {
            Method method = annotation.annotationType().getDeclaredMethod(attributeName);
            if (!isAttributeMethod(method)) {
                return Optional.empty();
            }
            return Optional.ofNullable(method.invoke(annotation));
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | RuntimeException ex) {
            return Optional.empty();
        }
    }

    /**
     * 读取注解的字符串属性。
     *
     * @param annotation 注解实例
     * @param attributeName 属性名
     * @return 字符串 Optional
     */
    public static Optional<String> readStringAttribute(Annotation annotation, String attributeName) {
        return readAttribute(annotation, attributeName).filter(String.class::isInstance).map(String.class::cast);
    }

    /**
     * 读取注解的布尔属性。
     *
     * @param annotation 注解实例
     * @param attributeName 属性名
     * @return 布尔值 Optional
     */
    public static Optional<Boolean> readBooleanAttribute(Annotation annotation, String attributeName) {
        return readAttribute(annotation, attributeName).filter(Boolean.class::isInstance).map(Boolean.class::cast);
    }

    /**
     * 读取注解的 Class 属性。
     *
     * @param annotation 注解实例
     * @param attributeName 属性名
     * @return Class Optional
     */
    public static Optional<Class<?>> readClassAttribute(Annotation annotation, String attributeName) {
        return readAttribute(annotation, attributeName).filter(Class.class::isInstance).map(Class.class::cast);
    }

    /**
     * 读取注解的枚举属性。
     *
     * @param annotation 注解实例
     * @param attributeName 属性名
     * @param enumType 枚举类型
     * @param <E> 枚举泛型
     * @return 枚举 Optional
     */
    public static <E extends Enum<E>> Optional<E> readEnumAttribute(Annotation annotation, String attributeName, Class<E> enumType) {
        if (enumType == null || !enumType.isEnum()) {
            return Optional.empty();
        }
        return readAttribute(annotation, attributeName).filter(enumType::isInstance).map(enumType::cast);
    }

    /**
     * 读取注解的数组属性。
     *
     * @param annotation 注解实例
     * @param attributeName 属性名
     * @param componentType 数组元素类型
     * @param <T> 元素泛型
     * @return 数组元素列表
     */
    public static <T> List<T> readArrayAttribute(Annotation annotation, String attributeName, Class<T> componentType) {
        if (componentType == null) {
            return List.of();
        }
        Optional<Object> valueOptional = readAttribute(annotation, attributeName);
        if (valueOptional.isEmpty()) {
            return List.of();
        }
        Object value = valueOptional.get();
        if (!value.getClass().isArray()) {
            return List.of();
        }
        int length = Array.getLength(value);
        List<T> result = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            Object item = Array.get(value, i);
            if (!isAssignableValue(componentType, item)) {
                return List.of();
            }
            result.add(castValue(item));
        }
        return List.copyOf(result);
    }

    /**
     * 读取注解的 value 属性。
     *
     * @param annotation 注解实例
     * @return value 属性 Optional
     */
    public static Optional<Object> readValue(Annotation annotation) {
        return readAttribute(annotation, VALUE_ATTRIBUTE);
    }

    /**
     * 读取注解的全部属性。
     *
     * @param annotation 注解实例
     * @return 属性 Map
     */
    public static Map<String, Object> readAttributes(Annotation annotation) {
        return readAttributes(annotation, AnnotationUtil::isAttributeMethod);
    }

    /**
     * 按属性方法过滤器读取注解属性。
     *
     * @param annotation 注解实例
     * @param attributeFilter 属性方法过滤器
     * @return 属性 Map
     */
    public static Map<String, Object> readAttributes(Annotation annotation, Predicate<Method> attributeFilter) {
        if (annotation == null) {
            return Map.of();
        }
        Predicate<Method> filter = attributeFilter == null ? AnnotationUtil::isAttributeMethod : attributeFilter;
        Map<String, Object> result = new LinkedHashMap<>();
        for (Method method : getAttributeMethods(annotation.annotationType())) {
            if (!filter.test(method)) {
                continue;
            }
            readAttribute(annotation, method.getName()).ifPresent(value -> result.put(method.getName(), value));
        }
        return Map.copyOf(result);
    }

    /**
     * 读取注解属性的默认值。
     *
     * @param annotationType 注解类型
     * @param attributeName 属性名
     * @return 默认值 Optional
     */
    public static Optional<Object> readDefaultValue(Class<? extends Annotation> annotationType, String attributeName) {
        if (!isAnnotationType(annotationType) || isBlank(attributeName)) {
            return Optional.empty();
        }
        try {
            Method method = annotationType.getDeclaredMethod(attributeName);
            if (!isAttributeMethod(method)) {
                return Optional.empty();
            }
            return Optional.ofNullable(method.getDefaultValue());
        } catch (NoSuchMethodException | RuntimeException ex) {
            return Optional.empty();
        }
    }

    /**
     * 读取注解类型的全部默认属性。
     *
     * @param annotationType 注解类型
     * @return 默认属性 Map
     */
    public static Map<String, Object> readDefaultAttributes(Class<? extends Annotation> annotationType) {
        if (!isAnnotationType(annotationType)) {
            return Map.of();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        for (Method method : getAttributeMethods(annotationType)) {
            Object defaultValue = method.getDefaultValue();
            if (defaultValue != null) {
                result.put(method.getName(), defaultValue);
            }
        }
        return Map.copyOf(result);
    }

    /**
     * 判断注解属性是否存在默认值。
     *
     * @param annotationType 注解类型
     * @param attributeName 属性名
     * @return 存在默认值返回 true，否则返回 false
     */
    public static boolean hasDefaultValue(Class<? extends Annotation> annotationType, String attributeName) {
        return readDefaultValue(annotationType, attributeName).isPresent();
    }

    /**
     * 判断注解实例的属性值是否等于默认值。
     *
     * @param annotation 注解实例
     * @param attributeName 属性名
     * @return 等于默认值返回 true，否则返回 false
     */
    public static boolean isDefaultValue(Annotation annotation, String attributeName) {
        if (annotation == null || isBlank(attributeName)) {
            return false;
        }
        Optional<Object> value = readAttribute(annotation, attributeName);
        Optional<Object> defaultValue = readDefaultValue(annotation.annotationType(), attributeName);
        return value.isPresent() && defaultValue.isPresent() && deepEquals(value.get(), defaultValue.get());
    }

    /**
     * 获取注解中与默认值不同的属性。
     *
     * @param annotation 注解实例
     * @return 非默认属性 Map
     */
    public static Map<String, Object> getNonDefaultAttributes(Annotation annotation) {
        if (annotation == null) {
            return Map.of();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : readAttributes(annotation).entrySet()) {
            Optional<Object> defaultValue = readDefaultValue(annotation.annotationType(), entry.getKey());
            if (defaultValue.isEmpty() || !deepEquals(entry.getValue(), defaultValue.get())) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return Map.copyOf(result);
    }

    /**
     * 从注解数组中过滤注解。
     *
     * @param annotations 注解数组
     * @param predicate 过滤条件
     * @return 注解列表
     */
    public static List<Annotation> filterAnnotations(Annotation[] annotations, Predicate<Annotation> predicate) {
        if (annotations == null || annotations.length == 0) {
            return List.of();
        }
        Predicate<Annotation> filter = predicate == null ? annotation -> true : predicate;
        List<Annotation> result = new ArrayList<>();
        for (Annotation annotation : annotations) {
            if (annotation != null && filter.test(annotation)) {
                result.add(annotation);
            }
        }
        return List.copyOf(result);
    }

    /**
     * 获取元素上满足条件的注解。
     *
     * @param element 元素
     * @param predicate 过滤条件
     * @return 注解列表
     */
    public static List<Annotation> getAnnotationsByPredicate(AnnotatedElement element, Predicate<Annotation> predicate) {
        return filterAnnotations(getAnnotations(element), predicate);
    }

    /**
     * 获取元素上指定包路径下的注解。
     *
     * @param element 元素
     * @param packageName 包名
     * @return 注解列表
     */
    public static List<Annotation> getAnnotationsByPackage(AnnotatedElement element, String packageName) {
        if (element == null || isBlank(packageName)) {
            return List.of();
        }
        String prefix = packageName.endsWith(".") ? packageName : packageName + ".";
        return getAnnotationsByPredicate(element, annotation -> annotation.annotationType().getName().startsWith(prefix));
    }

    /**
     * 排除 JDK 标准注解。
     *
     * @param annotations 注解数组
     * @return 非 JDK 注解列表
     */
    public static List<Annotation> excludeJdkAnnotations(Annotation[] annotations) {
        return filterAnnotations(annotations, annotation -> !isJdkAnnotation(annotation.annotationType()));
    }

    /**
     * 判断注解类型是否属于 JDK 标准注解。
     *
     * @param annotationType 注解类型
     * @return 属于 JDK 标准注解返回 true，否则返回 false
     */
    public static boolean isJdkAnnotation(Class<? extends Annotation> annotationType) {
        if (!isAnnotationType(annotationType)) {
            return false;
        }
        String name = annotationType.getName();
        return name.startsWith("java.lang.annotation.")
                || name.startsWith("java.lang.")
                || name.startsWith("jdk.")
                || name.startsWith("javax.annotation.");
    }

    /**
     * 判断注解类型是否为业务自定义注解。
     *
     * @param annotationType 注解类型
     * @return 自定义注解返回 true，否则返回 false
     */
    public static boolean isCustomAnnotation(Class<? extends Annotation> annotationType) {
        return isAnnotationType(annotationType) && !isJdkAnnotation(annotationType);
    }

    /**
     * 获取当前类中标记了指定注解的方法。
     *
     * @param targetClass 目标类
     * @param annotationType 注解类型
     * @return 方法列表
     */
    public static List<Method> getAnnotatedMethods(Class<?> targetClass, Class<? extends Annotation> annotationType) {
        if (targetClass == null || !isAnnotationType(annotationType)) {
            return List.of();
        }
        List<Method> result = new ArrayList<>();
        for (Method method : targetClass.getDeclaredMethods()) {
            if (hasAnnotation(method, annotationType)) {
                result.add(method);
            }
        }
        return List.copyOf(result);
    }

    /**
     * 获取当前类、父类和接口中标记了指定注解的方法。
     *
     * @param targetClass 目标类
     * @param annotationType 注解类型
     * @return 方法列表
     */
    public static List<Method> getAllAnnotatedMethods(Class<?> targetClass, Class<? extends Annotation> annotationType) {
        if (targetClass == null || !isAnnotationType(annotationType)) {
            return List.of();
        }
        Map<String, Method> result = new LinkedHashMap<>();
        for (Method method : getAllMethods(targetClass, true)) {
            if (hasAnnotation(method, annotationType)) {
                result.putIfAbsent(methodSignature(method), method);
            }
        }
        return List.copyOf(result.values());
    }

    /**
     * 获取带指定注解的构造器。
     *
     * @param targetClass 目标类
     * @param annotationType 注解类型
     * @return 构造器列表
     */
    public static List<Constructor<?>> getAnnotatedConstructors(Class<?> targetClass, Class<? extends Annotation> annotationType) {
        if (targetClass == null || !isAnnotationType(annotationType)) {
            return List.of();
        }
        List<Constructor<?>> result = new ArrayList<>();
        for (Constructor<?> constructor : targetClass.getDeclaredConstructors()) {
            if (hasAnnotation(constructor, annotationType)) {
                result.add(constructor);
            }
        }
        return List.copyOf(result);
    }

    /**
     * 汇总获取类、字段、方法、构造器和 Record 组件中被指定注解标记的元素。
     *
     * @param targetClass 目标类
     * @param annotationType 注解类型
     * @return 元素列表
     */
    public static List<AnnotatedElement> getAnnotatedElements(Class<?> targetClass, Class<? extends Annotation> annotationType) {
        if (targetClass == null || !isAnnotationType(annotationType)) {
            return List.of();
        }
        List<AnnotatedElement> result = new ArrayList<>();
        if (hasAnnotation(targetClass, annotationType)) {
            result.add(targetClass);
        }
        result.addAll(getAllAnnotatedFields(targetClass, annotationType));
        result.addAll(getAllAnnotatedMethods(targetClass, annotationType));
        result.addAll(getAnnotatedConstructors(targetClass, annotationType));
        result.addAll(getAnnotatedRecordComponents(targetClass, annotationType));
        return List.copyOf(result);
    }

    /**
     * 获取注解保留策略；未声明 Retention 时返回 CLASS。
     *
     * @param annotationType 注解类型
     * @return 保留策略 Optional
     */
    public static Optional<RetentionPolicy> getRetentionPolicy(Class<? extends Annotation> annotationType) {
        if (!isAnnotationType(annotationType)) {
            return Optional.empty();
        }
        Retention retention = annotationType.getAnnotation(Retention.class);
        return Optional.of(retention == null ? RetentionPolicy.CLASS : retention.value());
    }

    /**
     * 获取注解支持的目标类型；未声明 Target 时返回全部 ElementType。
     *
     * @param annotationType 注解类型
     * @return 目标类型集合
     */
    public static Set<ElementType> getElementTypes(Class<? extends Annotation> annotationType) {
        if (!isAnnotationType(annotationType)) {
            return Set.of();
        }
        Target target = annotationType.getAnnotation(Target.class);
        if (target == null) {
            return Set.copyOf(EnumSet.allOf(ElementType.class));
        }
        return Set.copyOf(EnumSet.copyOf(Arrays.asList(target.value())));
    }

    /**
     * 判断注解是否支持指定目标位置。
     *
     * @param annotationType 注解类型
     * @param elementType 目标位置
     * @return 支持返回 true，否则返回 false
     */
    public static boolean hasTarget(Class<? extends Annotation> annotationType, ElementType elementType) {
        return elementType != null && getElementTypes(annotationType).contains(elementType);
    }

    /**
     * 判断注解是否运行时可见。
     *
     * @param annotationType 注解类型
     * @return 运行时可见返回 true，否则返回 false
     */
    public static boolean isRuntimeRetention(Class<? extends Annotation> annotationType) {
        return getRetentionPolicy(annotationType).filter(policy -> policy == RetentionPolicy.RUNTIME).isPresent();
    }

    /**
     * 判断注解是否标记了 Inherited。
     *
     * @param annotationType 注解类型
     * @return 已标记返回 true，否则返回 false
     */
    public static boolean isInheritedAnnotation(Class<? extends Annotation> annotationType) {
        return hasAnnotation(annotationType, Inherited.class);
    }

    /**
     * 判断注解是否标记了 Repeatable。
     *
     * @param annotationType 注解类型
     * @return 已标记返回 true，否则返回 false
     */
    public static boolean isRepeatableAnnotation(Class<? extends Annotation> annotationType) {
        return hasAnnotation(annotationType, Repeatable.class);
    }

    /**
     * 获取可重复注解的容器注解类型。
     *
     * @param annotationType 注解类型
     * @return 容器注解类型 Optional
     */
    public static Optional<Class<? extends Annotation>> getRepeatableContainerType(Class<? extends Annotation> annotationType) {
        if (!isAnnotationType(annotationType)) {
            return Optional.empty();
        }
        Repeatable repeatable = annotationType.getAnnotation(Repeatable.class);
        return repeatable == null ? Optional.empty() : Optional.of(repeatable.value());
    }

    /**
     * 合并两个注解的属性，primary 优先覆盖 fallback。
     *
     * @param primary 优先注解
     * @param fallback 兜底注解
     * @return 合并后的属性 Map
     */
    public static Map<String, Object> mergeAnnotations(Annotation primary, Annotation fallback) {
        return mergeAttributes(primary, fallback);
    }

    /**
     * 合并两个注解的属性，primary 优先覆盖 fallback。
     *
     * @param primary 优先注解
     * @param fallback 兜底注解
     * @return 合并后的属性 Map
     */
    public static Map<String, Object> mergeAttributes(Annotation primary, Annotation fallback) {
        return mergeAttributes(readAttributes(primary), readAttributes(fallback));
    }

    /**
     * 合并两个属性 Map，primary 优先覆盖 fallback。
     *
     * @param primary 优先属性
     * @param fallback 兜底属性
     * @return 合并后的属性 Map
     */
    public static Map<String, Object> mergeAttributes(Map<String, Object> primary, Map<String, Object> fallback) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (fallback != null) {
            result.putAll(fallback);
        }
        if (primary != null) {
            result.putAll(primary);
        }
        return Map.copyOf(result);
    }

    /**
     * 查找注解并返回注解属性。
     *
     * @param element 元素
     * @param annotationType 注解类型
     * @return 属性 Map
     */
    public static Map<String, Object> findMergedAttributes(AnnotatedElement element, Class<? extends Annotation> annotationType) {
        return findMergedAnnotation(element, annotationType).map(AnnotationUtil::readAttributes).orElseGet(Map::of);
    }

    /**
     * 合并类级别和方法级别注解属性，方法上的非默认属性优先。
     *
     * @param targetClass 目标类
     * @param method 方法
     * @param annotationType 注解类型
     * @return 合并后的属性 Map
     */
    public static Map<String, Object> findMethodMergedAttributes(Class<?> targetClass, Method method, Class<? extends Annotation> annotationType) {
        if (method == null || !isAnnotationType(annotationType)) {
            return Map.of();
        }
        Map<String, Object> result = new LinkedHashMap<>(readDefaultAttributes(annotationType));
        findAnnotationInHierarchy(targetClass == null ? method.getDeclaringClass() : targetClass, annotationType)
                .ifPresent(annotation -> result.putAll(readAttributes(annotation)));
        findMethodAnnotationInHierarchy(targetClass, method, annotationType)
                .ifPresent(annotation -> result.putAll(getNonDefaultAttributes(annotation)));
        return Map.copyOf(result);
    }

    /**
     * 使用覆盖属性合并源属性。
     *
     * @param source 源属性
     * @param override 覆盖属性
     * @return 合并后的属性 Map
     */
    public static Map<String, Object> overrideAttributes(Map<String, Object> source, Map<String, Object> override) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (source != null) {
            result.putAll(source);
        }
        if (override != null) {
            result.putAll(override);
        }
        return Map.copyOf(result);
    }

    /**
     * 校验类型是否为注解类型。
     *
     * @param annotationType 待校验类型
     * @return 是注解类型返回 true，否则返回 false
     */
    public static boolean checkAnnotationType(Class<?> annotationType) {
        return isAnnotationType(annotationType);
    }

    /**
     * 判断类型是否为注解类型。
     *
     * @param type 类型
     * @return 是注解类型返回 true，否则返回 false
     */
    public static boolean isAnnotationType(Class<?> type) {
        return type != null && type.isAnnotation();
    }

    /**
     * 要求类型必须为注解类型。
     *
     * @param type 类型
     * @return 注解类型
     */
    public static Class<? extends Annotation> requireAnnotationType(Class<?> type) {
        if (!isAnnotationType(type)) {
            throw new IllegalArgumentException("类型必须是注解类型");
        }
        @SuppressWarnings("unchecked")
        Class<? extends Annotation> annotationType = (Class<? extends Annotation>) type;
        return annotationType;
    }

    /**
     * 要求元素不能为空。
     *
     * @param element 元素
     * @return 非空元素
     */
    public static AnnotatedElement requireElement(AnnotatedElement element) {
        if (element == null) {
            throw new IllegalArgumentException("元素不能为空");
        }
        return element;
    }

    /**
     * 安全获取元素上的指定注解。
     *
     * @param element 元素
     * @param annotationType 注解类型
     * @param <A> 注解泛型
     * @return 注解 Optional
     */
    public static <A extends Annotation> Optional<A> safeGetAnnotation(AnnotatedElement element, Class<A> annotationType) {
        try {
            return findAnnotation(element, annotationType);
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
    }

    /**
     * 安全读取注解属性。
     *
     * @param annotation 注解实例
     * @param attributeName 属性名
     * @return 属性值 Optional
     */
    public static Optional<Object> safeReadAttribute(Annotation annotation, String attributeName) {
        try {
            return readAttribute(annotation, attributeName);
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
    }

    /**
     * 判断方法是否为注解属性方法。
     *
     * @param method 方法
     * @return 是属性方法返回 true，否则返回 false
     */
    public static boolean isAttributeMethod(Method method) {
        return method != null
                && method.getDeclaringClass().isAnnotation()
                && method.getParameterCount() == 0
                && method.getReturnType() != Void.TYPE
                && !method.isSynthetic()
                && !method.isBridge()
                && !OBJECT_METHOD_NAMES.contains(method.getName())
                && Modifier.isPublic(method.getModifiers());
    }

    /**
     * 获取注解类型中的属性方法。
     *
     * @param annotationType 注解类型
     * @return 属性方法列表
     */
    public static List<Method> getAttributeMethods(Class<? extends Annotation> annotationType) {
        if (!isAnnotationType(annotationType)) {
            return List.of();
        }
        List<Method> result = new ArrayList<>();
        for (Method method : annotationType.getDeclaredMethods()) {
            if (isAttributeMethod(method)) {
                result.add(method);
            }
        }
        result.sort(Comparator.comparing(Method::getName));
        return List.copyOf(result);
    }

    /**
     * 从缓存中获取元素上的指定注解。
     *
     * @param element 元素
     * @param annotationType 注解类型
     * @param <A> 注解泛型
     * @return 注解 Optional
     */
    public static <A extends Annotation> Optional<A> getCachedAnnotation(AnnotatedElement element, Class<A> annotationType) {
        if (element == null || !isAnnotationType(annotationType)) {
            return Optional.empty();
        }
        for (Annotation annotation : getCachedAnnotations(element)) {
            if (annotationType.equals(annotation.annotationType())) {
                return Optional.of(annotationType.cast(annotation));
            }
        }
        return Optional.empty();
    }

    /**
     * 从缓存中获取元素上的全部注解。
     *
     * @param element 元素
     * @return 注解数组
     */
    public static Annotation[] getCachedAnnotations(AnnotatedElement element) {
        if (element == null) {
            return new Annotation[0];
        }
        if (!cacheEnabled) {
            return getAnnotations(element);
        }
        synchronized (ANNOTATION_CACHE) {
            Annotation[] annotations = ANNOTATION_CACHE.computeIfAbsent(element, AnnotatedElement::getAnnotations);
            return annotations.clone();
        }
    }

    /**
     * 从缓存中获取注解属性。
     *
     * @param annotation 注解实例
     * @return 属性 Map
     */
    public static Map<String, Object> getCachedAttributes(Annotation annotation) {
        if (annotation == null) {
            return Map.of();
        }
        if (!cacheEnabled) {
            return readAttributes(annotation);
        }
        synchronized (ATTRIBUTE_CACHE) {
            return ATTRIBUTE_CACHE.computeIfAbsent(annotation, AnnotationUtil::readAttributes);
        }
    }

    /**
     * 清空全部缓存。
     */
    public static void clearCache() {
        synchronized (ANNOTATION_CACHE) {
            ANNOTATION_CACHE.clear();
        }
        synchronized (ATTRIBUTE_CACHE) {
            ATTRIBUTE_CACHE.clear();
        }
    }

    /**
     * 清空指定元素的注解缓存。
     *
     * @param element 元素
     */
    public static void clearCache(AnnotatedElement element) {
        if (element == null) {
            return;
        }
        synchronized (ANNOTATION_CACHE) {
            ANNOTATION_CACHE.remove(element);
        }
    }

    /**
     * 判断是否启用缓存。
     *
     * @return 启用返回 true，否则返回 false
     */
    public static boolean isCacheEnabled() {
        return cacheEnabled;
    }

    /**
     * 设置是否启用缓存。
     *
     * @param enabled 是否启用
     */
    public static void setCacheEnabled(boolean enabled) {
        cacheEnabled = enabled;
    }

    /**
     * 优先查找方法注解，再查找类注解。
     *
     * @param targetClass 目标类
     * @param method 方法
     * @param annotationType 注解类型
     * @param <A> 注解泛型
     * @return 注解 Optional
     */
    public static <A extends Annotation> Optional<A> findClassOrMethodAnnotation(Class<?> targetClass, Method method, Class<A> annotationType) {
        return findMethodOrClassAnnotation(targetClass, method, annotationType);
    }

    /**
     * 优先查找方法注解，再查找类注解。
     *
     * @param targetClass 目标类
     * @param method 方法
     * @param annotationType 注解类型
     * @param <A> 注解泛型
     * @return 注解 Optional
     */
    public static <A extends Annotation> Optional<A> findMethodOrClassAnnotation(Class<?> targetClass, Method method, Class<A> annotationType) {
        Optional<A> methodAnnotation = findMethodAnnotationInHierarchy(targetClass, method, annotationType);
        return methodAnnotation.isPresent()
                ? methodAnnotation
                : findAnnotationInHierarchy(targetClass == null && method != null ? method.getDeclaringClass() : targetClass, annotationType);
    }

    /**
     * 判断类或方法上是否存在指定注解。
     *
     * @param targetClass 目标类
     * @param method 方法
     * @param annotationType 注解类型
     * @return 存在返回 true，否则返回 false
     */
    public static boolean hasClassOrMethodAnnotation(Class<?> targetClass, Method method, Class<? extends Annotation> annotationType) {
        return findMethodOrClassAnnotation(targetClass, method, annotationType).isPresent();
    }

    /**
     * 从多个候选注解中查找第一个存在的注解。
     *
     * @param element 元素
     * @param annotationTypes 注解类型数组
     * @return 注解 Optional
     */
    @SafeVarargs
    public static Optional<Annotation> findFirstAnnotation(AnnotatedElement element, Class<? extends Annotation>... annotationTypes) {
        if (element == null || annotationTypes == null || annotationTypes.length == 0) {
            return Optional.empty();
        }
        for (Class<? extends Annotation> annotationType : annotationTypes) {
            Annotation annotation = getAnnotation(element, annotationType);
            if (annotation != null) {
                return Optional.of(annotation);
            }
        }
        return Optional.empty();
    }

    /**
     * 获取元素上指定注解的 value 属性。
     *
     * @param element 元素
     * @param annotationType 注解类型
     * @return value 属性 Optional
     */
    public static Optional<Object> getAnnotationValue(AnnotatedElement element, Class<? extends Annotation> annotationType) {
        return findAnnotation(element, annotationType).flatMap(AnnotationUtil::readValue);
    }

    /**
     * 获取元素上指定注解的指定属性。
     *
     * @param element 元素
     * @param annotationType 注解类型
     * @param attributeName 属性名
     * @return 属性值 Optional
     */
    public static Optional<Object> getAnnotationAttribute(AnnotatedElement element, Class<? extends Annotation> annotationType, String attributeName) {
        return findAnnotation(element, annotationType).flatMap(annotation -> readAttribute(annotation, attributeName));
    }

    /**
     * 获取元素上指定注解的全部属性。
     *
     * @param element 元素
     * @param annotationType 注解类型
     * @return 属性 Map
     */
    public static Map<String, Object> getAnnotationAttributes(AnnotatedElement element, Class<? extends Annotation> annotationType) {
        return findAnnotation(element, annotationType).map(AnnotationUtil::readAttributes).orElseGet(Map::of);
    }

    private static List<Class<?>> collectTypeHierarchy(Class<?> targetClass) {
        if (targetClass == null) {
            return List.of();
        }
        List<Class<?>> result = new ArrayList<>();
        Set<Class<?>> visited = new HashSet<>();
        Queue<Class<?>> queue = new ArrayDeque<>();
        queue.add(targetClass);
        while (!queue.isEmpty()) {
            Class<?> current = queue.poll();
            if (current == null || current == Object.class || !visited.add(current)) {
                continue;
            }
            result.add(current);
            for (Class<?> interfaceType : current.getInterfaces()) {
                queue.add(interfaceType);
            }
            queue.add(current.getSuperclass());
        }
        return List.copyOf(result);
    }

    private static List<Method> getAllMethods(Class<?> targetClass, boolean includeInterfaces) {
        if (targetClass == null) {
            return List.of();
        }
        Map<String, Method> result = new LinkedHashMap<>();
        Collection<Class<?>> types = includeInterfaces ? collectTypeHierarchy(targetClass) : collectClassHierarchy(targetClass);
        for (Class<?> type : types) {
            for (Method method : type.getDeclaredMethods()) {
                result.putIfAbsent(methodSignature(method), method);
            }
        }
        return List.copyOf(result.values());
    }

    private static List<Class<?>> collectClassHierarchy(Class<?> targetClass) {
        List<Class<?>> result = new ArrayList<>();
        Class<?> current = targetClass;
        while (current != null && current != Object.class) {
            result.add(current);
            current = current.getSuperclass();
        }
        return List.copyOf(result);
    }

    private static Method findDeclaredMethod(Class<?> targetClass, String methodName, Class<?>[] parameterTypes) {
        if (targetClass == null || isBlank(methodName)) {
            return null;
        }
        try {
            return targetClass.getDeclaredMethod(methodName, parameterTypes == null ? new Class<?>[0] : parameterTypes);
        } catch (NoSuchMethodException | SecurityException ex) {
            return null;
        }
    }

    private static Field findDeclaredField(Class<?> targetClass, String fieldName) {
        if (targetClass == null || isBlank(fieldName)) {
            return null;
        }
        try {
            return targetClass.getDeclaredField(fieldName);
        } catch (NoSuchFieldException | SecurityException ex) {
            return null;
        }
    }

    private static String methodSignature(Method method) {
        return method.getName() + Arrays.toString(method.getParameterTypes());
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static boolean deepEquals(Object first, Object second) {
        return Arrays.deepEquals(new Object[]{first}, new Object[]{second});
    }
    @SuppressWarnings("unchecked")
    private static <T> T castValue(Object value) {
        return (T) value;
    }

    private static boolean isAssignableValue(Class<?> targetType, Object value) {
        if (value == null) {
            return !targetType.isPrimitive();
        }
        return wrapPrimitive(targetType).isInstance(value);
    }

    private static Class<?> wrapPrimitive(Class<?> type) {
        if (type == null || !type.isPrimitive()) {
            return type;
        }
        if (type == boolean.class) {
            return Boolean.class;
        }
        if (type == byte.class) {
            return Byte.class;
        }
        if (type == short.class) {
            return Short.class;
        }
        if (type == int.class) {
            return Integer.class;
        }
        if (type == long.class) {
            return Long.class;
        }
        if (type == float.class) {
            return Float.class;
        }
        if (type == double.class) {
            return Double.class;
        }
        if (type == char.class) {
            return Character.class;
        }
        if (type == void.class) {
            return Void.class;
        }
        return type;
    }
}
