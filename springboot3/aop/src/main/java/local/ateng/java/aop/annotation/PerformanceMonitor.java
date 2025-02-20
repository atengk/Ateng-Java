package local.ateng.java.aop.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自定义注解 @PerformanceMonitor 用于标记需要监控执行时间的方法。
 * <p>
 * 1. @Target(ElementType.METHOD) ：指定该注解只能应用于方法。
 * 2. @Retention(RetentionPolicy.RUNTIME) ：表示注解将在运行时通过反射读取。
 */
@Target(ElementType.METHOD) // 仅限方法使用
@Retention(RetentionPolicy.RUNTIME) // 保留至运行时，可通过反射获取
public @interface PerformanceMonitor {
}
