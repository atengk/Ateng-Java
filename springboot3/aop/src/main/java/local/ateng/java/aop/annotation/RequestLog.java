package local.ateng.java.aop.annotation;

import local.ateng.java.aop.constants.Module;
import local.ateng.java.aop.constants.Operation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用于标记需要记录 HTTP 请求日志的方法
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-02-20
 */
@Target(ElementType.METHOD)  // 仅限用于方法
@Retention(RetentionPolicy.RUNTIME)  // 运行时有效
public @interface RequestLog {
    Module module() default Module.NULL;  // 模块名

    Operation operation() default Operation.NULL;  // 操作类型（如：查询、添加、删除等）

    String description() default "";  // 操作说明

    boolean logParams() default true;  // 是否记录请求参数

    boolean logHeaders() default true;  // 是否记录请求头

    boolean logBody() default true;  // 是否记录请求体

    boolean logResponse() default true;  // 是否记录响应数据

    boolean logExecutionTime() default true;  // 是否记录执行时间
}
