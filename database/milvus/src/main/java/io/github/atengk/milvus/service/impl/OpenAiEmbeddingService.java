package io.github.atengk.milvus.service.impl;

import io.github.atengk.milvus.service.EmbeddingService;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 基于 OpenAI 的 Embedding 服务实现
 *
 * <p>
 * 说明：
 * <ul>
 *     <li>内部通过 Spring AI 的 EmbeddingModel 调用 OpenAI Embeddings API</li>
 *     <li>embed(text) 会自动委托到批量接口</li>
 *     <li>dimension 在首次调用后缓存</li>
 * </ul>
 */
@Service
public class OpenAiEmbeddingService implements EmbeddingService {

    /**
     * Spring AI 抽象的 EmbeddingModel
     */
    private final EmbeddingModel embeddingModel;

    /**
     * embedding 维度缓存
     */
    private volatile Integer dimensionCache;

    public OpenAiEmbeddingService(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    /**
     * 单条文本 embedding
     */
    @Override
    public List<Float> embed(String text) {
        if (ObjectUtils.isEmpty(text)) {
            return Collections.emptyList();
        }

        List<List<Float>> result = embedBatch(Collections.singletonList(text));
        return result.isEmpty() ? Collections.emptyList() : result.get(0);
    }

    /**
     * 批量文本 embedding
     */
    @Override
    public List<List<Float>> embedBatch(List<String> texts) {
        if (ObjectUtils.isEmpty(texts)) {
            return Collections.emptyList();
        }

        EmbeddingRequest request = new EmbeddingRequest(texts, null);

        EmbeddingResponse response = embeddingModel.call(request);

        List<Embedding> embeddings = response.getResults();
        if (ObjectUtils.isEmpty(embeddings)) {
            return Collections.emptyList();
        }

        List<List<Float>> vectors = embeddings.stream()
                .map(Embedding::getOutput)
                .filter(Objects::nonNull)
                .map(this::toFloatList)
                .collect(Collectors.toList());

        cacheDimensionIfNecessary(vectors);

        return vectors;
    }

    /**
     * 将 float[] 转换为 List<Float>
     */
    private List<Float> toFloatList(float[] vector) {
        List<Float> result = new ArrayList<>(vector.length);
        for (float v : vector) {
            result.add(v);
        }
        return result;
    }

    /**
     * 返回 embedding 向量维度
     */
    @Override
    public int dimension() {
        if (dimensionCache != null) {
            return dimensionCache;
        }

        List<Float> vector = embed("dimension_probe");
        if (ObjectUtils.isEmpty(vector)) {
            throw new IllegalStateException("Failed to determine embedding dimension");
        }

        dimensionCache = vector.size();
        return dimensionCache;
    }

    /**
     * 缓存 embedding 维度
     */
    private void cacheDimensionIfNecessary(List<List<Float>> vectors) {
        if (dimensionCache != null) {
            return;
        }

        if (ObjectUtils.isEmpty(vectors)) {
            return;
        }

        List<Float> first = vectors.get(0);
        if (!ObjectUtils.isEmpty(first)) {
            dimensionCache = first.size();
        }
    }
}
