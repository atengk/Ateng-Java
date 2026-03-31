package io.github.atengk.interceptor.annotation;

import java.lang.annotation.*;

/**
 * 接口签名校验注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SignCheck {

    /**
     * 过期时间（秒）
     */
    int expire() default 60;
}
