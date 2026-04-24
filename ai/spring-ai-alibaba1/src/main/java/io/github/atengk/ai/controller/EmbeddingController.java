package io.github.atengk.ai.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 嵌入模型接口
 *
 * @author Ateng
 * @since 2026-04-24
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class EmbeddingController {

    private final EmbeddingModel embeddingModel;

    /**
     * 调用 DashScope Embedding 模型生成文本向量
     *
     * @param text 待向量化文本
     * @return 向量维度和部分向量值
     */
    @GetMapping("/ai/embedding")
    public Map<String, Object> embedding(@RequestParam(required = false) String text) {
        String input = StrUtil.blankToDefault(text, "Spring AI Alibaba 是一个面向 Java 生态的 AI 应用开发框架");
        log.info("收到嵌入模型请求，text={}", input);

        float[] vector = embeddingModel.embed(input);
        List<Double> preview = Arrays.stream(toDoubleArray(vector))
                .limit(8)
                .map(value -> NumberUtil.round(value, 6).doubleValue())
                .boxed()
                .toList();

        return Map.of(
                "modelProvider", "dashscope",
                "text", input,
                "dimension", vector.length,
                "preview", CollUtil.defaultIfEmpty(preview, List.of())
        );
    }

    /**
     * float 数组转换为 double 数组，便于使用 Stream 处理
     *
     * @param vector float 向量
     * @return double 向量
     */
    private double[] toDoubleArray(float[] vector) {
        double[] values = new double[vector.length];
        for (int index = 0; index < vector.length; index++) {
            values[index] = vector[index];
        }
        return values;
    }

}