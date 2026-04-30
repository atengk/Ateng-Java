package io.github.atengk.utils.enumutil.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 枚举项元数据注解。
 *
 * @author Ateng
 * @since 2026-04-29
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface EnumItem {

    /**
     * 展示名称。
     *
     * @return 展示名称
     */
    String label() default "";

    /**
     * 描述。
     *
     * @return 描述
     */
    String desc() default "";

    /**
     * 排序值。
     *
     * @return 排序值
     */
    int sort() default 0;

    /**
     * 是否启用。
     *
     * @return 是否启用
     */
    boolean enabled() default true;

    /**
     * 分组。
     *
     * @return 分组
     */
    String group() default "";

    /**
     * 状态颜色。
     *
     * @return 状态颜色
     */
    String color() default "";

    /**
     * 前端标签类型。
     *
     * @return 标签类型
     */
    String tagType() default "";

    /**
     * 图标。
     *
     * @return 图标
     */
    String icon() default "";

    /**
     * 样式类名。
     *
     * @return 样式类名
     */
    String cssClass() default "";

    /**
     * 权限标识。
     *
     * @return 权限标识
     */
    String permission() default "";

    /**
     * 备注。
     *
     * @return 备注
     */
    String remark() default "";

    /**
     * 父级编码。
     *
     * @return 父级编码
     */
    String parentCode() default "";
}
