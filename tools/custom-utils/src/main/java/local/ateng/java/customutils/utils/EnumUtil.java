package local.ateng.java.customutils.utils;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 通用枚举工具类（字段名不固定）
 * 通过反射按指定字段提取枚举值，适配任意结构枚举类
 *
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
     * 根据字段值获取枚举实例
     *
     * @param enumClass 枚举类
     * @param fieldName 要匹配的字段名（如 code、value、key）
     * @param targetValue 目标值
     * @param <E> 枚举类型
     * @return 匹配的枚举实例，未找到返回 null
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
     * 枚举字段映射（如：code -> name，或者 id -> label）
     *
     * @param enumClass 枚举类
     * @param keyField  key 字段名
     * @param valueField value 字段名
     * @param <E> 枚举类型
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
     * @param <E> 枚举类型
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
     * @param <E> 枚举类型
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
     * @param <E> 枚举类型
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
                    return false; // 出现重复
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

}
