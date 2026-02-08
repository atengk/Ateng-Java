package io.github.atengk.milvus.util;

import java.util.ArrayList;
import java.util.List;

public class TextSplitter {

    /**
     * 按最大字符数切割，带重叠
     */
    public static List<String> split(
            String text,
            int chunkSize,
            int overlap
    ) {
        List<String> chunks = new ArrayList<>();

        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());
            chunks.add(text.substring(start, end));
            start = end - overlap;
            if (start < 0) {
                start = 0;
            }
        }
        return chunks;
    }
}
