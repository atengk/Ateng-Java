package io.github.atengk.milvus.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import io.github.atengk.milvus.service.EmbeddingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 基于 Ollama 的 Embedding 服务实现（动态维度版）
 *
 * @author Ateng
 * @since 2026-04-15
 */
@Slf4j
@Service
public class OllamaEmbeddingServiceImpl implements EmbeddingService {

    /**
     * Ollama Embedding 接口地址
     */
    private static final String OLLAMA_EMBEDDING_URL = "http://localhost:11434/api/embeddings";

    /**
     * 模型名称
     */
    private static final String MODEL = "qwen3-embedding:4b";

    @Override
    public List<Float> embed(String text) {

        if (ObjectUtils.isEmpty(text)) {
            throw new IllegalArgumentException("text 不能为空");
        }

        log.info("调用 Ollama Embedding，文本长度：{}", text.length());

        JSONObject request = new JSONObject();
        request.set("model", MODEL);
        request.set("prompt", text);

        String response = HttpRequest.post(OLLAMA_EMBEDDING_URL)
                .timeout(5000)
                .body(request.toString())
                .execute()
                .body();

        if (ObjectUtils.isEmpty(response)) {
            throw new RuntimeException("Ollama 返回为空");
        }

        JSONObject json = new JSONObject(response);
        JSONArray embeddingArray = json.getJSONArray("embedding");

        if (ObjectUtils.isEmpty(embeddingArray)) {
            throw new RuntimeException("embedding 解析失败");
        }

        List<Float> result = new ArrayList<>(embeddingArray.size());

        for (Object obj : embeddingArray) {
            result.add(Convert.toFloat(obj));
        }

        return result;
    }

    @Override
    public List<List<Float>> embedBatch(List<String> texts) {

        if (CollUtil.isEmpty(texts)) {
            return new ArrayList<>();
        }

        log.info("批量调用 Ollama Embedding，数量：{}", texts.size());

        List<List<Float>> result = new ArrayList<>(texts.size());

        for (String text : texts) {
            result.add(embed(text));
        }

        return result;
    }

    @Override
    public int dimension() {
        // 使用一个极小文本做探测
        String probe = "dim";
        return embed(probe).size();
    }
}