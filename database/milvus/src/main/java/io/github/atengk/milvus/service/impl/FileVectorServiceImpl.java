package io.github.atengk.milvus.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.google.gson.JsonObject;
import io.github.atengk.milvus.entity.VectorDocument;
import io.github.atengk.milvus.service.EmbeddingService;
import io.github.atengk.milvus.service.FileVectorService;
import io.github.atengk.milvus.service.MilvusService;
import io.github.atengk.milvus.util.TextSplitter;
import io.github.atengk.milvus.util.TikaUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class FileVectorServiceImpl implements FileVectorService {

    private static final int CHUNK_SIZE = 800;
    private static final int CHUNK_OVERLAP = 150;

    private final MilvusService milvusService;
    private final EmbeddingService embeddingService;

    public FileVectorServiceImpl(
            MilvusService milvusService,
            EmbeddingService embeddingService
    ) {
        this.milvusService = milvusService;
        this.embeddingService = embeddingService;
    }

    @Override
    public void ingest(
            String collectionName,
            String fileName,
            InputStream inputStream,
            Map<String, Object> externalMetadata
    ) {
        try {
            // 读取文件字节 + 计算内容指纹
            byte[] fileBytes = inputStream.readAllBytes();
            String documentId = DigestUtil.sha256Hex(fileBytes);

            // 文件防重复
            String expr = String.format(
                    "metadata[\"documentId\"] == \"%s\"",
                    documentId
            );

            if (milvusService.existsByExpr(collectionName, expr)) {
                log.warn("文件重复，跳过写入: fileName={}, SHA-256={}", fileName, documentId);
                return;
            }

            // Tika 解析文本
            InputStream tikaInputStream = new ByteArrayInputStream(fileBytes);
            TikaUtil.TikaResult tikaResult = TikaUtil.parseAll(tikaInputStream, -1);

            String content = tikaResult.getContent();
            if (StrUtil.isBlank(content)) {
                log.warn("文件内容为空，跳过写入: fileName={}", fileName);
                return;
            }

            // 文本切割
            List<String> chunks = TextSplitter.split(
                    content,
                    CHUNK_SIZE,
                    CHUNK_OVERLAP
            );

            if (chunks.isEmpty()) {
                log.warn("文本切割结果为空，跳过写入: fileName={}", fileName);
                return;
            }

            int chunkTotal = chunks.size();
            List<VectorDocument> documents = new ArrayList<>(chunkTotal);

            int offsetCursor = 0;

            // 构建向量文档
            for (int i = 0; i < chunkTotal; i++) {

                String chunk = chunks.get(i);
                if (StrUtil.isBlank(chunk)) {
                    continue;
                }

                String chunkId = UUID.randomUUID().toString();

                int startOffset = offsetCursor;
                int endOffset = startOffset + chunk.length();
                offsetCursor = Math.max(endOffset - CHUNK_OVERLAP, startOffset);

                VectorDocument document = new VectorDocument();
                document.setId(chunkId);
                document.setContent(chunk);
                document.setEmbedding(embeddingService.embed(chunk));

                JsonObject metadata = new JsonObject();

                // 文档级
                metadata.addProperty("documentId", documentId);
                metadata.addProperty("fileHash", documentId);
                metadata.addProperty("fileName", fileName);

                // chunk 级
                metadata.addProperty("chunkId", chunkId);
                metadata.addProperty("chunkIndex", i);
                metadata.addProperty("chunkTotal", chunkTotal);
                metadata.addProperty("startOffset", startOffset);
                metadata.addProperty("endOffset", endOffset);
                metadata.addProperty("chunkSize", CHUNK_SIZE);
                metadata.addProperty("chunkOverlap", CHUNK_OVERLAP);

                // Tika 元数据
                if (tikaResult.getMetadata() != null) {
                    tikaResult.getMetadata().forEach(metadata::addProperty);
                }

                // 外部透传 metadata
                if (externalMetadata != null) {
                    externalMetadata.forEach(
                            (k, v) -> metadata.addProperty(k, String.valueOf(v))
                    );
                }

                document.setMetadata(metadata);
                documents.add(document);
            }

            // 写入 Milvus
            if (!documents.isEmpty()) {
                milvusService.add(collectionName, documents);
            }

            log.info(
                    "文件写入 Milvus 完成: fileName={}, documentId={}, chunks={}",
                    fileName,
                    documentId,
                    documents.size()
            );

        } catch (Exception e) {
            log.error("写入 Milvus 失败: fileName={}", fileName, e);
            throw new RuntimeException("写入 Milvus 失败", e);
        }
    }

}
