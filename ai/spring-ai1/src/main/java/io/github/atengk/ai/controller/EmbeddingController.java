package io.github.atengk.ai.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.embedding.Embedding;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 向量嵌入接口
 *
 * @author Ateng
 * @since 2026-04-21
 */
@Slf4j
@RestController
@RequestMapping("/api/ai")
public class EmbeddingController {

    private final EmbeddingModel embeddingModel;

    @Autowired
    public EmbeddingController(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    /**
     * 单条文本向量化
     */
    @GetMapping("/embedding")
    public Map<String, Object> embed(@RequestParam("text") String text) {
        try {
            EmbeddingResponse response = embeddingModel.embedForResponse(Collections.singletonList(text));

            List<float[]> vectors = response.getResults()
                    .stream()
                    .map(Embedding::getOutput)
                    .collect(Collectors.toList());

            return buildResult(vectors);
        } catch (Exception e) {
            log.error("embedding 失败，text={}", text, e);
            return error("embedding 失败");
        }
    }

    /**
     * 批量文本向量化
     */
    @PostMapping("/embedding/batch")
    public Map<String, Object> embedBatch(@RequestBody List<String> texts) {
        try {
            EmbeddingResponse response = embeddingModel.embedForResponse(texts);

            List<float[]> vectors = response.getResults()
                    .stream()
                    .map(Embedding::getOutput)
                    .collect(Collectors.toList());

            return buildResult(vectors);
        } catch (Exception e) {
            log.error("embedding 批量失败，texts={}", texts, e);
            return error("embedding 批量失败");
        }
    }

    /**
     * 构建统一返回结构
     */
    private Map<String, Object> buildResult(List<float[]> vectors) {
        Map<String, Object> result = new HashMap<>();
        result.put("vectors", vectors);
        result.put("dimension", vectors.isEmpty() ? 0 : vectors.get(0).length);
        result.put("count", vectors.size());
        return result;
    }

    private Map<String, Object> error(String msg) {
        return Map.of("success", false, "message", msg);
    }
}