package io.github.atengk.interceptor.annotation;

import java.lang.annotation.*;

/**
 * 字段校验注解
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface FieldValid {

    /**
     * 是否必填
     */
    boolean required() default false;

    /**
     * 最小长度
     */
    int minLength() default -1;

    /**
     * 最大长度
     */
    int maxLength() default -1;

    /**
     * 提示信息
     */
    String message() default "参数不合法";
}
