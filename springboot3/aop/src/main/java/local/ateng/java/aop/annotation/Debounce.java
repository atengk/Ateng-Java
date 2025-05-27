package local.ateng.java.aop.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * 自定义防抖注解，防止接口重复提交
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-02-21
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Debounce {

    // 设置防抖的时间，单位：毫秒，默认值为500ms
    long interval() default 500;

    // 时间单位
    TimeUnit timeUnit() default TimeUnit.MILLISECONDS;

    // 提示消息
    String message() default "操作频繁，请稍候再试";

    // 自定义参数keys做判断，支持 Spring EL 表达式
    String[] keys() default {};
}
