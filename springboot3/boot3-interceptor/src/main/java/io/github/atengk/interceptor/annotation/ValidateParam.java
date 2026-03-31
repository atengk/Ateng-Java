package io.github.atengk.interceptor.annotation;

import java.lang.annotation.*;

/**
 * 开启参数校验
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ValidateParam {
}
