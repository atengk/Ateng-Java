package io.github.atengk.utils.enumutil.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 枚举字典元数据注解。
 *
 * @author Ateng
 * @since 2026-04-29
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface EnumDict {

    /**
     * 字典唯一标识。
     *
     * @return 字典唯一标识
     */
    String key() default "";

    /**
     * 字典标题。
     *
     * @return 字典标题
     */
    String title() default "";

    /**
     * 所属模块。
     *
     * @return 所属模块
     */
    String module() default "";

    /**
     * 字典描述。
     *
     * @return 字典描述
     */
    String description() default "";

    /**
     * 是否允许接口暴露。
     *
     * @return 是否允许接口暴露
     */
    boolean expose() default true;

    /**
     * 字典排序值。
     *
     * @return 排序值
     */
    int sort() default 0;

    /**
     * 字典分组。
     *
     * @return 字典分组
     */
    String group() default "";

    /**
     * 是否废弃。
     *
     * @return 是否废弃
     */
    boolean deprecatedFlag() default false;

    /**
     * 允许输出的扩展字段。
     *
     * @return 扩展字段名数组
     */
    String[] extraFields() default {};
}
