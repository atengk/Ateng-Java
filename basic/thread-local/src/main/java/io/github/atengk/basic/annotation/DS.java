package io.github.atengk.basic.annotation;

import java.lang.annotation.*;

/**
 * 数据源切换注解
 *
 * @author Ateng
 * @since 2026-04-10
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DS {

    /**
     * 数据源名称
     */
    String value() default "master";
}