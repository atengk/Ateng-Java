package io.github.atengk.interceptor.annotation;

import java.lang.annotation.*;

/**
 * 灰度控制注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GrayRelease {

    /**
     * 灰度比例（0-100）
     */
    int percent() default 10;

    /**
     * Header控制Key（可选）
     */
    String header() default "Gray-Version";

    /**
     * 灰度版本标识
     */
    String version() default "v2";
}
