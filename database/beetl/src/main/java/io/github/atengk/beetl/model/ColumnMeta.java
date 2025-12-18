package io.github.atengk.beetl.model;

import lombok.Data;

/**
 * 表字段定义
 *
 * @author 孔余
 * @since 2025-12-18
 */
@Data
public class ColumnMeta {

    /**
     * 字段名
     */
    private String name;

    /**
     * 字段类型（varchar / bigint / decimal 等）
     */
    private String type;

    /**
     * 长度（varchar、char）
     */
    private Integer length;

    /**
     * 精度（decimal）
     */
    private Integer precision;

    /**
     * 小数位（decimal）
     */
    private Integer scale;

    /**
     * 是否无符号（int / bigint）
     */
    private Boolean unsigned;

    /**
     * 是否主键
     */
    private Boolean primaryKey;

    /**
     * 是否自增
     */
    private Boolean autoIncrement;

    /**
     * 是否唯一
     */
    private Boolean unique;

    /**
     * 是否允许为空
     */
    private Boolean nullable;

    /**
     * 默认值（不自动加引号）
     */
    private String defaultValue;

    /**
     * 是否 ON UPDATE CURRENT_TIMESTAMP
     */
    private Boolean onUpdateCurrentTimestamp;

    /**
     * 字段备注
     */
    private String comment;
}
