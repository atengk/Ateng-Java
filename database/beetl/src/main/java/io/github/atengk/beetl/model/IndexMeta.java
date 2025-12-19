package io.github.atengk.beetl.model;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

/**
 * 索引元数据
 */
@Data
public class IndexMeta {

    /**
     * 索引名称
     */
    private String name;

    /**
     * 是否主键索引
     */
    private Boolean primary;

    /**
     * 是否唯一索引
     */
    private Boolean unique;

    /**
     * 索引字段
     */
    private List<String> columns = new ArrayList<>();
}
