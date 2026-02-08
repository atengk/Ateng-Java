package io.github.atengk.milvus.service.impl;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.milvus.entity.VectorDocument;
import io.github.atengk.milvus.service.EmbeddingService;
import io.github.atengk.milvus.service.FileVectorService;
import io.github.atengk.milvus.service.MilvusService;
import io.github.atengk.milvus.util.TextSplitter;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.*;

@Slf4j
@Service
public class PdfFileVectorService implements FileVectorService {

    private static final int CHUNK_SIZE = 800;
    private static final int CHUNK_OVERLAP = 150;

    private final MilvusService milvusService;
    private final EmbeddingService embeddingService;

    public PdfFileVectorService(
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
            Map<String, Object> metadata
    ) {
        try (PDDocument document = PDDocument.load(inputStream)) {

            PDFTextStripper stripper = new PDFTextStripper();
            String fullText = stripper.getText(document);

            if (fullText == null || fullText.isBlank()) {
                log.warn("PDF 内容为空: {}", fileName);
                return;
            }

            // 文本切割
            List<String> chunks = TextSplitter.split(
                    fullText,
                    CHUNK_SIZE,
                    CHUNK_OVERLAP
            );

            List<VectorDocument> docs = new ArrayList<>();

            // 文档级唯一 ID
            String docId = UUID.randomUUID().toString();
            int totalChunks = chunks.size();

            // 用于计算 offset
            int cursor = 0;

            for (int i = 0; i < chunks.size(); i++) {

                String chunk = chunks.get(i);
                if (StrUtil.isBlank(chunk)) {
                    continue;
                }

                int startOffset = cursor;
                int endOffset = cursor + chunk.length();
                cursor = endOffset - CHUNK_OVERLAP;

                VectorDocument doc = new VectorDocument();
                doc.setId(UUID.randomUUID().toString());
                doc.setContent(chunk);
                doc.setEmbedding(embeddingService.embed(chunk));

                // metadata
                com.google.gson.JsonObject meta = new com.google.gson.JsonObject();

                meta.addProperty("fileName", fileName);
                meta.addProperty("chunkIndex", i);

                meta.addProperty("docId", docId);
                meta.addProperty("chunkTotal", totalChunks);
                meta.addProperty("fileType", "pdf");
                meta.addProperty("startOffset", startOffset);
                meta.addProperty("endOffset", endOffset);
                meta.addProperty("chunkSize", CHUNK_SIZE);
                meta.addProperty("chunkOverlap", CHUNK_OVERLAP);

                // 外部 metadata 透传
                if (metadata != null) {
                    metadata.forEach((k, v) ->
                            meta.addProperty(k, String.valueOf(v))
                    );
                }

                doc.setMetadata(meta);
                docs.add(doc);
            }

            milvusService.add(collectionName, docs);

            log.info(
                    "PDF 写入完成: file={}, docId={}, chunks={}",
                    fileName,
                    docId,
                    docs.size()
            );

        } catch (Exception e) {
            throw new RuntimeException("PDF 写入 Milvus 失败", e);
        }
    }

}
