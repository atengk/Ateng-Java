package local.ateng.java.aop.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 权限控制注解，标记需要进行权限检查的方法
 */
@Target(ElementType.METHOD)  // 仅限方法上使用
@Retention(RetentionPolicy.RUNTIME)  // 运行时有效
public @interface PermissionCheck {
    String value();  // 权限标识，比如 "ADMIN", "USER", "MANAGER" 等
}

