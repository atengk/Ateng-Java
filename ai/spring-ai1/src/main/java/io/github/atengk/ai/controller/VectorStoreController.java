package io.github.atengk.ai.controller;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.InputStreamResource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 向量库操作接口
 *
 * @author Ateng
 * @since 2026-04-21
 */
@Slf4j
@RestController
@RequestMapping("/api/ai/vector")
public class VectorStoreController {

    private final VectorStore vectorStore;

    public VectorStoreController(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    /**
     * 上传文档并向量化入库（带完整分片控制）
     */
    @PostMapping("/upload")
    public Map<String, Object> uploadDocument(@RequestParam("file") MultipartFile file) {

        String fileName = file.getOriginalFilename();

        if (file == null || file.isEmpty()) {
            return fail("文件不能为空");
        }

        log.info("开始处理文件: {}", fileName);

        try {
            // ================== 1. 文档解析 ==================
            TikaDocumentReader reader = new TikaDocumentReader(new InputStreamResource(file.getInputStream()));
            List<Document> documents = reader.get();

            if (documents.isEmpty()) {
                return fail("文档解析失败或内容为空");
            }

            // ================== 2. 分片参数（重点） ==================

            /**
             * chunkSize
             * 每个分片的“目标 token 数”
             * - 太小：语义断裂
             * - 太大：embedding 质量下降 + 成本上升
             * 推荐：
             * - 中文：300 ~ 600
             */
            int chunkSize = 500;

            /**
             * minChunkSizeChars
             * 最小字符数阈值（低于这个值的分片会被丢弃或合并）
             * 作用：
             * - 防止出现无意义碎片（如几个字）
             */
            int minChunkSizeChars = 200;

            /**
             * minChunkLengthToEmbed
             * 最小允许参与 embedding 的长度
             * 小于该值的 chunk 不会被 embedding
             * 作用：
             * - 避免 embedding 噪声数据
             */
            int minChunkLengthToEmbed = 100;

            /**
             * maxNumChunks
             * 单个文档最多分片数量
             * 防止：
             * - 超大文件导致 OOM
             * - 向量库爆炸
             */
            int maxNumChunks = 1000;

            /**
             * keepSeparator
             * 是否保留分隔符（标点）
             * 中文建议 true，否则语义会断
             */
            boolean keepSeparator = true;

            /**
             * punctuationMarks
             * 分割依据的标点
             * ⚠️ 默认是英文标点，不适合中文
             * 这里手动补充中文标点（非常关键）
             */
            List<Character> punctuationMarks = Arrays.asList(
                    '。', '！', '？', '；', '，', '\n',
                    '.', '!', '?', ';', ','
            );

            // ================== 3. 构建分片器 ==================
            TokenTextSplitter splitter = new TokenTextSplitter(
                    chunkSize,
                    minChunkSizeChars,
                    minChunkLengthToEmbed,
                    maxNumChunks,
                    keepSeparator,
                    punctuationMarks
            );

            List<Document> chunks = splitter.apply(documents);

            if (chunks.isEmpty()) {
                return fail("分片结果为空，请检查参数");
            }

            // ================== 4. 元数据增强 ==================
            chunks.forEach((doc) -> {
                doc.getMetadata().put("source", file.getOriginalFilename());
                doc.getMetadata().put("filename", fileName);
                doc.getMetadata().put("uploadTime", System.currentTimeMillis());
                doc.getMetadata().put("length", doc.getText().length());
            });

            // ================== 5. 入库（自动 embedding） ==================
            vectorStore.add(chunks);

            log.info("文件处理完成: {}, 原始文档={}, 分片数={}", fileName, documents.size(), chunks.size());

            return success(Map.of(
                    "fileName", fileName,
                    "docCount", documents.size(),
                    "chunkCount", chunks.size()
            ));

        } catch (IOException e) {
            log.error("文件读取失败: {}", fileName, e);
            return fail("文件读取失败");
        } catch (Exception e) {
            log.error("向量化失败: {}", fileName, e);
            return fail("向量化失败");
        }
    }

    /**
     * 文本直接向量化入库（不走 Tika）
     */
    @PostMapping("/ingest/text")
    public Map<String, Object> ingestText(@RequestParam("text") String text,
                                          @RequestParam(value = "source", required = false) String source) {

        if (StrUtil.isBlank(text)) {
            return fail("text 不能为空");
        }

        // 默认 source（避免为空）
        if (StrUtil.isBlank(source)) {
            source = "text_input_" + System.currentTimeMillis();
        }

        log.info("文本入库开始，source={}", source);

        try {
            // ================== 1. 构建 Document ==================
            Document document = new Document(text);

            // 元数据（统一规范）
            document.getMetadata().put("source", source);
            document.getMetadata().put("type", "text");
            document.getMetadata().put("length", text.length());
            document.getMetadata().put("uploadTime", System.currentTimeMillis());

            List<Document> documents = Collections.singletonList(document);

            // ================== 2. 分片参数（同文件一致） ==================
            int chunkSize = 500;
            int minChunkSizeChars = 200;
            int minChunkLengthToEmbed = 100;
            int maxNumChunks = 1000;
            boolean keepSeparator = true;

            List<Character> punctuationMarks = Arrays.asList(
                    '。', '！', '？', '；', '，', '\n',
                    '.', '!', '?', ';', ','
            );

            TokenTextSplitter splitter = new TokenTextSplitter(
                    chunkSize,
                    minChunkSizeChars,
                    minChunkLengthToEmbed,
                    maxNumChunks,
                    keepSeparator,
                    punctuationMarks
            );

            List<Document> chunks = splitter.apply(documents);

            if (chunks.isEmpty()) {
                return fail("分片结果为空");
            }

            // ================== 3. 补充分片级 metadata ==================
            int total = chunks.size();
            for (int i = 0; i < total; i++) {
                Document doc = chunks.get(i);
                doc.getMetadata().put("chunk_index", i);
                doc.getMetadata().put("total_chunks", total);
            }

            // ================== 4. 入库 ==================
            vectorStore.add(chunks);

            log.info("文本入库完成，source={}, chunk数量={}", source, total);

            return success(Map.of(
                    "source", source,
                    "chunkCount", total
            ));

        } catch (Exception e) {
            log.error("文本入库失败，source={}", source, e);
            return fail("文本入库失败");
        }
    }

    /**
     * 向量检索
     */
    @GetMapping("/search")
    public Map<String, Object> search(@RequestParam("query") String query,
                                      @RequestParam(value = "topK", defaultValue = "5") int topK) {

        if (StrUtil.isBlank(query)) {
            return fail("query 不能为空");
        }

        log.info("向量检索: query={}, topK={}", query, topK);

        try {
            SearchRequest request = SearchRequest.builder()
                    .query(query)
                    .topK(topK)
                    .build();

            List<Document> results = vectorStore.similaritySearch(request);

            List<Map<String, Object>> data = results.stream()
                    .map(doc -> {
                        String text = doc.getText();
                        // 截断，避免返回超大文本
                        if (StrUtil.length(text) > 300) {
                            text = StrUtil.sub(text, 0, 300) + "...";
                        }

                        return Map.of(
                                "content", text,
                                "metadata", doc.getMetadata()
                        );
                    })
                    .collect(Collectors.toList());

            return success(data);

        } catch (Exception e) {
            log.error("检索失败: query={}", query, e);
            return fail("检索失败");
        }
    }

    // ================== 统一返回 ==================

    private Map<String, Object> success(Object data) {
        return Map.of(
                "success", true,
                "data", data
        );
    }

    private Map<String, Object> fail(String msg) {
        return Map.of(
                "success", false,
                "message", msg
        );
    }
}