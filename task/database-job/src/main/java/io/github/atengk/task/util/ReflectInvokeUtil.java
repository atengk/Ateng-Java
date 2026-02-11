package io.github.atengk.task.util;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import cn.hutool.core.util.ReflectUtil;

import java.lang.reflect.Method;
import java.util.List;

public class ReflectInvokeUtil {

    /**
     * 反射调用方法
     */
    public static Object invoke(Object bean,
                                String methodName,
                                String paramTypesJson,
                                String paramsJson) {

        Class<?>[] paramTypes = buildParamTypes(paramTypesJson);
        Object[] args = buildArgs(paramTypes, paramsJson);

        Method method = ReflectUtil.getMethod(bean.getClass(), methodName, paramTypes);

        if (method == null) {
            throw new IllegalArgumentException("Method not found: " + methodName);
        }

        if (paramTypes.length != args.length) {
            throw new IllegalArgumentException("Parameter size mismatch");
        }

        return ReflectUtil.invoke(bean, method, args);
    }

    private static Class<?>[] buildParamTypes(String paramTypesJson) {

        if (StrUtil.isBlank(paramTypesJson) || "[]".equals(paramTypesJson)) {
            return new Class<?>[0];
        }

        List<String> typeList = JSONUtil.toList(paramTypesJson, String.class);

        return typeList.stream()
                .map(ReflectInvokeUtil::loadClass)
                .toArray(Class<?>[]::new);
    }

    private static Object[] buildArgs(Class<?>[] paramTypes, String paramsJson) {

        if (StrUtil.isBlank(paramsJson) || "[]".equals(paramsJson)) {
            return new Object[0];
        }

        List<Object> paramList = JSONUtil.toList(paramsJson, Object.class);

        Object[] args = new Object[paramTypes.length];

        for (int i = 0; i < paramTypes.length; i++) {

            Class<?> targetType = paramTypes[i];
            Object value = paramList.get(i);

            if (JSONUtil.isTypeJSON(String.valueOf(value))) {
                args[i] = JSONUtil.toBean(JSONUtil.parseObj(value), targetType);
            } else {
                args[i] = Convert.convert(targetType, value);
            }
        }

        return args;
    }

    private static Class<?> loadClass(String className) {
        try {
            return Class.forName(className);
        } catch (Exception e) {
            throw new RuntimeException("Load class error: " + className, e);
        }
    }
}
