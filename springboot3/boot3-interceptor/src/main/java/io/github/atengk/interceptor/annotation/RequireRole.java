package io.github.atengk.interceptor.annotation;

import java.lang.annotation.*;

/**
 * 权限注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireRole {

    /**
     * 允许访问的角色
     */
    String[] value();
}
