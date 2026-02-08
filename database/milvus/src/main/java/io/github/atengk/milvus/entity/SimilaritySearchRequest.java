package io.github.atengk.milvus.entity;

import lombok.Data;

import java.util.List;

@Data
public class SimilaritySearchRequest {

    /**
     * collection 名称
     */
    private String collectionName;

    /**
     * 查询向量（embedding）
     */
    private List<Float> embedding;

    /**
     * 返回的相似结果数量
     */
    private int topK = 5;

    /**
     * Milvus expr
     */
    private String expr;

    /**
     * 是否返回向量本身（默认不返回）
     */
    private boolean includeEmbedding = false;

}
