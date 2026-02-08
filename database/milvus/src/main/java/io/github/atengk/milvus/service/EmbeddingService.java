package io.github.atengk.milvus.service;

import java.util.List;

public interface EmbeddingService {

    /**
     * 单条文本 embedding
     */
    List<Float> embed(String text);

    /**
     * 批量 embedding（强烈推荐）
     */
    List<List<Float>> embedBatch(List<String> texts);

    /**
     * 返回 embedding 维度
     */
    int dimension();
}

