package io.github.atengk.interceptor.annotation;

import java.lang.annotation.*;

/**
 * 限流注解（固定窗口）
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /**
     * 时间窗口（秒）
     */
    int time() default 60;

    /**
     * 最大请求次数
     */
    int count() default 10;
}
