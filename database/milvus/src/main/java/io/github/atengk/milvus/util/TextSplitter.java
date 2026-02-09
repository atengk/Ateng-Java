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
        if (chunkSize <= 0) {
            throw new IllegalArgumentException("chunkSize must be > 0");
        }
        if (overlap < 0) {
            throw new IllegalArgumentException("overlap must be >= 0");
        }
        if (overlap >= chunkSize) {
            throw new IllegalArgumentException(
                    "overlap must be smaller than chunkSize"
            );
        }

        List<String> chunks = new ArrayList<>();

        int start = 0;
        int textLength = text.length();

        while (start < textLength) {
            int end = Math.min(start + chunkSize, textLength);
            chunks.add(text.substring(start, end));

            start += (chunkSize - overlap);
        }

        return chunks;
    }

}
