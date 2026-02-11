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
import org.springframework.beans.factory.annotation.Qualifier;
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
            @Qualifier("openAiEmbeddingService") EmbeddingService embeddingService
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
            /* ========================= file fingerprint ========================= */

            byte[] fileBytes = inputStream.readAllBytes();
            String documentId = DigestUtil.sha256Hex(fileBytes);

            String expr = String.format(
                    "metadata[\"documentId\"] == \"%s\"",
                    documentId
            );

            if (milvusService.existsByExpr(collectionName, expr)) {
                log.warn(
                        "文件重复，跳过写入: fileName={}, documentId={}",
                        fileName,
                        documentId
                );
                return;
            }

            /* ========================= tika parse ========================= */

            InputStream tikaInputStream = new ByteArrayInputStream(fileBytes);
            TikaUtil.TikaResult tikaResult = TikaUtil.parseAll(tikaInputStream, -1);

            String content = tikaResult.getContent();
            if (StrUtil.isBlank(content)) {
                log.warn("文件内容为空，跳过写入: fileName={}", fileName);
                return;
            }

            /* ========================= text split ========================= */

            List<String> chunks = TextSplitter.split(
                    content,
                    CHUNK_SIZE,
                    CHUNK_OVERLAP
            );

            if (chunks.isEmpty()) {
                log.warn("文本切割结果为空，跳过写入: fileName={}", fileName);
                return;
            }

            /* ========================= batch embedding ========================= */

            List<String> chunkTexts = new ArrayList<>(chunks.size());
            for (String chunk : chunks) {
                if (StrUtil.isNotBlank(chunk)) {
                    chunkTexts.add(chunk);
                }
            }

            List<List<Float>> embeddings =
                    embeddingService.embedBatch(chunkTexts);

            if (embeddings.size() != chunkTexts.size()) {
                throw new IllegalStateException(
                        "Embedding 结果数量不匹配: texts="
                                + chunkTexts.size()
                                + ", embeddings="
                                + embeddings.size()
                );
            }

            /* ========================= build vector documents ========================= */

            int chunkTotal = chunkTexts.size();
            List<VectorDocument> documents = new ArrayList<>(chunkTotal);

            int offsetCursor = 0;

            for (int i = 0; i < chunkTotal; i++) {

                String chunk = chunkTexts.get(i);
                List<Float> vector = embeddings.get(i);

                String chunkId = UUID.randomUUID().toString();

                int startOffset = offsetCursor;
                int endOffset = startOffset + chunk.length();
                offsetCursor = Math.max(endOffset - CHUNK_OVERLAP, startOffset);

                VectorDocument document = new VectorDocument();
                document.setId(chunkId);
                document.setContent(chunk);
                document.setEmbedding(vector);

                JsonObject metadata = new JsonObject();

                /* ---------- document level ---------- */
                metadata.addProperty("documentId", documentId);
                metadata.addProperty("fileHash", documentId);
                metadata.addProperty("fileName", fileName);

                /* ---------- chunk level ---------- */
                metadata.addProperty("chunkId", chunkId);
                metadata.addProperty("chunkIndex", i);
                metadata.addProperty("chunkTotal", chunkTotal);
                metadata.addProperty("startOffset", startOffset);
                metadata.addProperty("endOffset", endOffset);
                metadata.addProperty("chunkSize", CHUNK_SIZE);
                metadata.addProperty("chunkOverlap", CHUNK_OVERLAP);

                /* ---------- tika metadata ---------- */
                if (tikaResult.getMetadata() != null) {
                    tikaResult.getMetadata().forEach(metadata::addProperty);
                }

                /* ---------- external metadata ---------- */
                if (externalMetadata != null) {
                    externalMetadata.forEach(
                            (k, v) -> metadata.addProperty(k, String.valueOf(v))
                    );
                }

                document.setMetadata(metadata);
                documents.add(document);
            }

            /* ========================= write to milvus ========================= */

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
