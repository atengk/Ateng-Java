package io.github.atengk.milvus.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 文本切割工具类
 *
 * <p>
 * 用于将长文本按指定最大长度切割为多个 chunk，
 * 并在相邻 chunk 之间保留一定的字符重叠，
 * 以减少语义在切割边界处的丢失。
 * </p>
 *
 * <p>
 * 常用于：
 * <ul>
 *     <li>RAG 文档切块</li>
 *     <li>Embedding 前文本预处理</li>
 * </ul>
 * </p>
 */
public class TextSplitter {

    private TextSplitter() {
    }

    /**
     * 按最大字符数切割文本（支持重叠）
     *
     * @param text 原始文本
     * @param chunkSize 每个 chunk 的最大字符数，必须 &gt; 0
     * @param overlap 相邻 chunk 之间的重叠字符数，必须 &gt;= 0 且 &lt; chunkSize
     * @return 切割后的文本块列表，按原文顺序排列
     */
    public static List<String> split(
            String text,
            int chunkSize,
            int overlap
    ) {
        /* ---------- 基础校验 ---------- */

        if (text == null || text.isBlank()) {
            return Collections.emptyList();
        }

        if (chunkSize <= 0) {
            throw new IllegalArgumentException("chunkSize must be greater than 0");
        }

        if (overlap < 0) {
            throw new IllegalArgumentException("overlap must be greater than or equal to 0");
        }

        if (overlap >= chunkSize) {
            throw new IllegalArgumentException(
                    "overlap must be smaller than chunkSize"
            );
        }

        /* ---------- 切割逻辑 ---------- */

        List<String> chunks = new ArrayList<>();

        int textLength = text.length();
        int start = 0;

        int step = chunkSize - overlap;
        if (step <= 0) {
            throw new IllegalStateException(
                    "Invalid step size, possible infinite loop: step=" + step
            );
        }

        while (start < textLength) {
            int end = Math.min(start + chunkSize, textLength);
            chunks.add(text.substring(start, end));
            start += step;
        }

        return chunks;
    }
}
