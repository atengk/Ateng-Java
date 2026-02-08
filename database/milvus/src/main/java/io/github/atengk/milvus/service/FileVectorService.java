package io.github.atengk.milvus.service;

import java.io.InputStream;
import java.util.Map;

public interface FileVectorService {

    /**
     * 将文件写入 Milvus（自动解析、切割、embedding）
     *
     * @param collectionName Milvus collection
     * @param fileName 文件名（用于 metadata）
     * @param inputStream 文件流
     * @param metadata 额外元数据（如 source、bizId）
     */
    void ingest(
            String collectionName,
            String fileName,
            InputStream inputStream,
            Map<String, Object> metadata
    );
}
