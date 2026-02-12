package io.github.atengk.task.util;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import cn.hutool.core.util.ReflectUtil;

import java.lang.reflect.Method;
import java.util.List;

/**
 * 反射调用工具类
 * <p>
 * 功能说明：
 * 1. 支持通过 Spring Bean + 方法名动态调用方法
 * 2. 支持基础类型参数
 * 3. 支持复杂对象参数（JSON转Bean）
 * 4. 支持无参方法
 * <p>
 * 参数格式说明：
 * method_param_types  : ["java.lang.Long","java.lang.String"]
 * method_params       : [10001,"admin"]
 * <p>
 * 注意：
 * 1. 参数类型数组顺序必须与参数值数组顺序一致
 * 2. 参数数量必须匹配
 *
 * @author Ateng
 * @since 2026-02-12
 */
public class ReflectInvokeUtil {

    /**
     * 反射调用指定方法
     *
     * @param bean           Spring 容器中的 Bean 实例
     * @param methodName     方法名
     * @param paramTypesJson 方法参数类型(JSON数组)
     * @param paramsJson     方法参数值(JSON数组)
     * @return 方法返回值
     */
    public static Object invoke(Object bean,
                                String methodName,
                                String paramTypesJson,
                                String paramsJson) {

        if (bean == null) {
            throw new IllegalArgumentException("反射调用失败：Bean 不能为空");
        }

        if (StrUtil.isBlank(methodName)) {
            throw new IllegalArgumentException("反射调用失败：方法名不能为空");
        }

        // 构建参数类型数组
        Class<?>[] paramTypes = buildParamTypes(paramTypesJson);

        // 构建参数值数组
        Object[] args = buildArgs(paramTypes, paramsJson);

        // 查找方法
        Method method = ReflectUtil.getMethod(bean.getClass(), methodName, paramTypes);

        if (method == null) {
            throw new IllegalArgumentException(
                    StrUtil.format("反射调用失败：未找到方法 {}，参数类型={}",
                            methodName,
                            paramTypesJson)
            );
        }

        if (paramTypes.length != args.length) {
            throw new IllegalArgumentException(
                    StrUtil.format("反射调用失败：参数数量不匹配，期望 {} 个，实际 {} 个",
                            paramTypes.length,
                            args.length)
            );
        }

        try {
            return ReflectUtil.invoke(bean, method, args);
        } catch (Exception e) {
            throw new RuntimeException(
                    StrUtil.format("反射调用方法失败：{}#{}",
                            bean.getClass().getName(),
                            methodName),
                    e
            );
        }
    }

    /**
     * 构建参数类型数组
     *
     * @param paramTypesJson 参数类型JSON
     * @return Class数组
     */
    private static Class<?>[] buildParamTypes(String paramTypesJson) {

        if (StrUtil.isBlank(paramTypesJson) || "[]" .equals(paramTypesJson)) {
            return new Class<?>[0];
        }

        List<String> typeList = JSONUtil.toList(paramTypesJson, String.class);

        return typeList.stream()
                .map(ReflectInvokeUtil::loadClass)
                .toArray(Class<?>[]::new);
    }

    /**
     * 构建参数值数组
     *
     * @param paramTypes 参数类型数组
     * @param paramsJson 参数值JSON
     * @return 参数对象数组
     */
    private static Object[] buildArgs(Class<?>[] paramTypes, String paramsJson) {

        if (StrUtil.isBlank(paramsJson) || "[]" .equals(paramsJson)) {
            return new Object[0];
        }

        List<Object> paramList = JSONUtil.toList(paramsJson, Object.class);

        if (paramList.size() != paramTypes.length) {
            throw new IllegalArgumentException(
                    StrUtil.format("参数数量不匹配：期望 {} 个，实际 {} 个",
                            paramTypes.length,
                            paramList.size())
            );
        }

        Object[] args = new Object[paramTypes.length];

        for (int i = 0; i < paramTypes.length; i++) {

            Class<?> targetType = paramTypes[i];
            Object value = paramList.get(i);

            if (value == null) {
                args[i] = null;
                continue;
            }

            // 如果是JSON对象，则转为复杂类型
            if (JSONUtil.isTypeJSON(String.valueOf(value))) {
                args[i] = JSONUtil.toBean(
                        JSONUtil.parseObj(value),
                        targetType
                );
            } else {
                args[i] = Convert.convert(targetType, value);
            }
        }

        return args;
    }

    /**
     * 加载Class
     *
     * @param className 类全限定名
     * @return Class对象
     */
    private static Class<?> loadClass(String className) {

        if (StrUtil.isBlank(className)) {
            throw new IllegalArgumentException("参数类型不能为空");
        }

        try {
            return Class.forName(className);
        } catch (Exception e) {
            throw new RuntimeException(
                    StrUtil.format("加载参数类型失败：{}", className),
                    e
            );
        }
    }
}
