package io.github.atengk.interceptor.annotation;


import java.lang.annotation.*;

/**
 * 滑动窗口限流注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SlidingWindowLimit {

    /**
     * 时间窗口（秒）
     */
    int time() default 60;

    /**
     * 最大请求数
     */
    int count() default 10;
}