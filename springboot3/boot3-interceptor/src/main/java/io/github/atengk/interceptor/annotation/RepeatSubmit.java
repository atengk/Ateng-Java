package io.github.atengk.interceptor.annotation;

import java.lang.annotation.*;

/**
 * 防重复提交注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RepeatSubmit {

    /**
     * 防重复时间窗口（秒）
     */
    int expire() default 5;
}
