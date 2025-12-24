package io.github.atengk.beetl.model;

import lombok.Data;

import java.util.List;

/**
 * 表结构定义
 *
 * @author 孔余
 * @since 2025-12-18
 */
@Data
public class TableMeta {

    /**
     * 表名
     */
    private String tableName;

    /**
     * 是否忽略表已存在（CREATE TABLE IF NOT EXISTS）
     */
    private Boolean ifNotExists;

    /**
     * 表备注
     */
    private String comment;

    /**
     * 存储引擎
     */
    private String engine;

    /**
     * 字符集
     */
    private String charset;

    /**
     * 排序规则
     */
    private String collation;

    /**
     * 字段列表
     */
    private List<ColumnMeta> columns;

    /**
     * 索引列表
     */
    private List<IndexMeta> indexes;
}
