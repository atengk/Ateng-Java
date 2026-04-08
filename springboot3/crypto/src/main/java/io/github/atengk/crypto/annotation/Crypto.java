package io.github.atengk.crypto.annotation;

import java.lang.annotation.*;

/**
 * 接口加解密注解
 *
 * @author 孔余
 * @since 2026-01-29
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Crypto {

    /**
     * 是否解密请求
     */
    boolean decrypt() default true;

    /**
     * 是否加密响应
     */
    boolean encrypt() default true;
}