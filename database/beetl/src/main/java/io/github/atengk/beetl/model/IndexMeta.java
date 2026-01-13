package io.github.atengk.beetl.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 索引元数据
 *
 * @author 孔余
 * @since 2025-12-18
 */
@Data
public class IndexMeta {

    /**
     * 索引名称
     */
    private String name;

    /**
     * PRIMARY / UNIQUE / NORMAL / FULLTEXT
     */
    private String type;

    /**
     * BTREE / HASH
     */
    private String using;

    /**
     * 索引字段
     */
    private List<IndexColumnMeta> columns = new ArrayList<>();
}
