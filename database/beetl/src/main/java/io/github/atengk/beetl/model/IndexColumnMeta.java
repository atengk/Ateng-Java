package io.github.atengk.beetl.model;

import lombok.Data;

/**
 * 索引字段元数据
 *
 * @author 孔余
 * @since 2025-12-18
 */
@Data
public class IndexColumnMeta {

    /**
     * 字段名
     */
    private String columnName;

    /**
     * 排序方式 ASC / DESC
     */
    private String order;
}
