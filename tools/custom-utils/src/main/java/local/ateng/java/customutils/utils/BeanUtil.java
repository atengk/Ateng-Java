package local.ateng.java.customutils.utils;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
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
     * 将 JavaBean 列表复制为另一种类型的 JavaBean 列表
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
                T target = targetType.newInstance();
                copyProperties(source, target);
                result.add(target);
            } catch (Exception e) {
                throw new RuntimeException("列表元素复制失败: " + e.getMessage(), e);
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

}

