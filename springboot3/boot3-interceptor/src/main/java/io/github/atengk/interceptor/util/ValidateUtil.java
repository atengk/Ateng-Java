package io.github.atengk.interceptor.util;


import io.github.atengk.interceptor.annotation.FieldValid;

import java.lang.reflect.Field;

/**
 * 参数校验工具类
 */
public class ValidateUtil {

    /**
     * 校验对象
     */
    public static void validate(Object obj) {

        if (obj == null) {
            throw new RuntimeException("请求参数不能为空");
        }

        Field[] fields = obj.getClass().getDeclaredFields();

        for (Field field : fields) {

            FieldValid valid = field.getAnnotation(FieldValid.class);
            if (valid == null) {
                continue;
            }

            field.setAccessible(true);

            try {
                Object value = field.get(obj);

                // 必填校验
                if (valid.required() && value == null) {
                    throw new RuntimeException(valid.message());
                }

                if (value != null && value instanceof String) {
                    String str = (String) value;

                    // 最小长度
                    if (valid.minLength() != -1 && str.length() < valid.minLength()) {
                        throw new RuntimeException(valid.message());
                    }

                    // 最大长度
                    if (valid.maxLength() != -1 && str.length() > valid.maxLength()) {
                        throw new RuntimeException(valid.message());
                    }
                }

            } catch (IllegalAccessException e) {
                throw new RuntimeException("参数校验异常");
            }
        }
    }
}
