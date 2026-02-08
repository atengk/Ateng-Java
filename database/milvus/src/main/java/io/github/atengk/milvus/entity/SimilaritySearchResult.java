package io.github.atengk.milvus.entity;

import lombok.Data;

@Data
public class SimilaritySearchResult {
    private VectorDocument document;
    /**
     * 相似度分数
     * - COSINE: 越大越相似（最大为 1）
     * - L2: 越小越相似
     */
    private float score;

}
