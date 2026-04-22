package io.github.atengk.ai.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.ai.constant.RagIngestConstants;
import io.github.atengk.ai.service.RagIngestService;
import io.github.atengk.ai.util.ResourceUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * RAG 文档摄取控制器
 *
 * @author Ateng
 * @since 2026-04-21
 */
@RestController
@RequestMapping("/api/rag")
@RequiredArgsConstructor
public class RagIngestController {

    private static final Logger log = LoggerFactory.getLogger(RagIngestController.class);

    private final RagIngestService ragIngestService;

    // =========================
    // ingest
    // =========================

    /**
     * 单文件摄取（全流程 RAG ingest）
     * <p>
     * HTTP Method: POST
     * URL: /rag/ingest
     * Content-Type: multipart/form-data
     * <p>
     * Request:
     * - file: MultipartFile（待解析文件，如 pdf/docx/txt/html）
     * - metadata: form-data key-value（RAG 元数据）
     * <p>
     * metadata 示例：
     * {
     * "source.id": "file-001",
     * "tenant.id": "t1",
     * "biz.tags": "ai,rag,test",
     * "security.acl": "public"
     * }
     * <p>
     * Processing:
     * file -> Resource -> Tika解析 -> Document -> chunk -> VectorStore(Milvus)
     * <p>
     * Response:
     * - int：写入向量库的 chunk 数量
     */
    @PostMapping(value = "/ingest", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public int ingest(@RequestPart("file") MultipartFile file,
                      @RequestParam Map<String, Object> metadata) throws Exception {

        Resource resource = file.getResource();

        log.info("RAG ingest start, file={}", file.getOriginalFilename());

        return ragIngestService.ingest(resource, metadata);
    }

    /**
     * 字符串内容摄取
     */
    @PostMapping("/ingest/text")
    public int ingestText(@RequestParam String content,
                          @RequestParam Map<String, Object> metadata) {

        if (StrUtil.isBlank(content)) {
            return 0;
        }

        // 构造 Resource
        Resource resource = ResourceUtil.fromString(content, "default.txt", StandardCharsets.UTF_8);

        log.info("RAG ingest start, textLength={}", content.length());

        return ragIngestService.ingest(resource, metadata);
    }

    /**
     * URL 摄取
     */
    @PostMapping("/ingest/url")
    public int ingestUrl(@RequestParam String url,
                         @RequestParam Map<String, Object> metadata) throws Exception {

        if (StrUtil.isBlank(url)) {
            return 0;
        }

        Resource resource = ResourceUtil.getResource(url);

        log.info("RAG ingest start, url={}", url);

        return ragIngestService.ingest(resource, metadata);
    }

    /**
     * 批量文件摄取（多文件 RAG ingest）
     * <p>
     * HTTP Method: POST
     * URL: /rag/ingest/batch
     * Content-Type: multipart/form-data
     * <p>
     * Request:
     * - files: MultipartFile[]（多个文件）
     * - metadata: 全局元数据（会应用到所有文件）
     * <p>
     * Processing:
     * files -> Resource List -> parse -> preprocess -> split -> vector store
     * <p>
     * Response:
     * - int：总写入 chunk 数
     */
    @PostMapping(value = "/ingest/batch", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public int ingestBatch(@RequestPart("files") List<MultipartFile> files,
                           @RequestParam Map<String, Object> metadata) {

        List<Resource> resources = files.stream()
                .map(MultipartFile::getResource)
                .toList();

        return ragIngestService.ingest(resources, metadata);
    }

    // =========================
    // incremental / rebuild
    // =========================

    /**
     * 增量摄取（幂等写入）
     * <p>
     * HTTP Method: POST
     * URL: /rag/ingest/incremental
     * Content-Type: multipart/form-data
     * <p>
     * 特性：
     * - 基于 source.id + content.hash 去重
     * - 内容未变化则跳过写入
     * <p>
     * Request:
     * - file: 文件资源
     * - metadata: RAG 元数据（必须包含 source.id）
     * <p>
     * Response:
     * - int：新增 chunk 数
     */
    @PostMapping(value = "/ingest/incremental", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public int ingestIncremental(@RequestPart("file") MultipartFile file,
                                 @RequestParam Map<String, Object> metadata) throws Exception {

        return ragIngestService.ingestIncremental(file.getResource(), metadata);
    }

    /**
     * 重建索引（全量删除 + 重建）
     * <p>
     * HTTP Method: POST
     * URL: /rag/rebuild
     * Content-Type: multipart/form-data
     * <p>
     * 使用场景：
     * - chunk策略变更
     * - embedding模型变更
     * - 数据结构变更
     * <p>
     * Request:
     * - file: 数据源
     * - metadata: RAG上下文（tenant/source等）
     * <p>
     * Response:
     * - String: 执行结果
     */
    @PostMapping(value = "/rebuild", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String rebuild(@RequestPart("file") MultipartFile file,
                          @RequestParam Map<String, Object> metadata) throws Exception {

        ragIngestService.rebuild(file.getResource(), metadata);

        return "rebuild success";
    }

    // =========================
    // parse / preview
    // =========================

    /**
     * 文档解析（不写入向量库）
     * <p>
     * HTTP Method: POST
     * URL: /rag/parse
     * <p>
     * Request:
     * - file: 文件资源
     * - metadata: 元数据（用于解析上下文）
     * <p>
     * Response:
     * - List<Document>: 原始解析结果（未chunk）
     * <p>
     * 用途：
     * - 调试 Tika 解析结果
     * - 验证文本抽取效果
     */
    @PostMapping(value = "/parse", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public List<Document> parse(@RequestPart("file") MultipartFile file,
                                @RequestParam Map<String, Object> metadata) throws Exception {

        return ragIngestService.parse(file.getResource(), metadata);
    }

    /**
     * RAG chunk预览（调试用）
     * <p>
     * HTTP Method: POST
     * URL: /rag/preview
     * <p>
     * 返回：
     * - chunk后的 Document 列表（但不入库）
     * <p>
     * 用途：
     * - chunk效果评估
     * - embedding前内容验证
     */
    @PostMapping(value = "/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public List<Document> preview(@RequestPart("file") MultipartFile file,
                                  @RequestParam Map<String, Object> metadata) throws Exception {

        return ragIngestService.preview(file.getResource(), metadata);
    }

    // =========================
    // query
    // =========================

    /**
     * 判断指定 sourceId + contentHash 是否存在（幂等校验）
     */
    @GetMapping("/exists/hash")
    public boolean existsByHash(@RequestParam String sourceId,
                                @RequestParam String contentHash) {

        boolean exists = ragIngestService.exists(sourceId, contentHash);

        log.info("RAG 存在性校验（hash），sourceId={}, contentHash={}, exists={}",
                sourceId, contentHash, exists);

        return exists;
    }

    /**
     * 根据 sourceId 判断是否存在数据（通用 exists）
     */
    @GetMapping("/exists/source")
    public boolean existsBySource(@RequestParam String sourceId) {

        Filter.Expression expression = new FilterExpressionBuilder()
                .eq(RagIngestConstants.METADATA_SOURCE_ID, sourceId)
                .build();

        boolean exists = ragIngestService.exists(expression);

        log.info("RAG 存在性校验（sourceId），sourceId={}, exists={}", sourceId, exists);

        return exists;
    }

    /**
     * 根据 sourceId 查询文档列表
     */
    @GetMapping("/list/source")
    public List<Document> listBySource(@RequestParam String sourceId,
                                       @RequestParam(defaultValue = "10") int topK) {

        Filter.Expression expression = new FilterExpressionBuilder()
                .eq(RagIngestConstants.METADATA_SOURCE_ID, sourceId)
                .build();

        List<Document> documents = ragIngestService.list(expression, topK);

        log.info("RAG 文档查询，sourceId={}, 返回数量={}",
                sourceId, CollUtil.size(documents));

        return documents;
    }

    /**
     * 复杂条件查询（sourceId + tenantId + 时间范围）
     */
    @GetMapping("/list/complex")
    public List<Document> listByComplex(@RequestParam String sourceId,
                                        @RequestParam String tenantId,
                                        @RequestParam(required = false) String startTime,
                                        @RequestParam(required = false) String endTime,
                                        @RequestParam(defaultValue = "10") int topK) {
        FilterExpressionBuilder builder = new FilterExpressionBuilder();

        // 基础条件
        FilterExpressionBuilder.Op op = builder.and(
                builder.eq(RagIngestConstants.METADATA_SOURCE_ID, sourceId),
                builder.eq(RagIngestConstants.METADATA_TENANT_ID, tenantId)
        );

        // 时间范围
        if (StrUtil.isNotBlank(startTime)) {
            op = builder.and(
                    op,
                    builder.gte(RagIngestConstants.METADATA_UPDATED_AT, startTime)
            );
        }

        if (StrUtil.isNotBlank(endTime)) {
            op = builder.and(
                    op,
                    builder.lte(RagIngestConstants.METADATA_UPDATED_AT, endTime)
            );
        }

        // 最后 build
        Filter.Expression expression = op.build();

        List<Document> documents = ragIngestService.list(expression, topK);

        log.info("RAG 复杂查询，sourceId={}, tenantId={}, startTime={}, endTime={}, 返回数量={}",
                sourceId, tenantId, startTime, endTime, CollUtil.size(documents));

        return documents;
    }

    /**
     * 相似度查询（基础）
     */
    @GetMapping("/search")
    public List<Document> search(@RequestParam String query,
                                 @RequestParam(defaultValue = "5") int topK) {

        return ragIngestService.similaritySearch(query, topK, null);
    }

    /**
     * 相似度查询（带 sourceId 过滤）
     */
    @GetMapping("/search/bySource")
    public List<Document> searchBySource(@RequestParam String query,
                                         @RequestParam String sourceId,
                                         @RequestParam(defaultValue = "5") int topK) {

        Filter.Expression expression = new FilterExpressionBuilder()
                .eq(RagIngestConstants.METADATA_SOURCE_ID, sourceId)
                .build();

        return ragIngestService.similaritySearch(query, topK, expression);
    }

    // =========================
    // delete
    // =========================

    /**
     * 按 sourceId 删除向量数据
     * <p>
     * HTTP Method: DELETE
     * URL: /rag/source/{sourceId}
     * <p>
     * Path Param:
     * - sourceId: 数据源唯一标识
     * <p>
     * Effect:
     * - 删除 Milvus 中对应 metadata.source.id 的所有 chunk
     */
    @DeleteMapping("/source/{sourceId}")
    public String deleteBySource(@PathVariable String sourceId) {

        if (StrUtil.isBlank(sourceId)) {
            return "sourceId is blank";
        }

        ragIngestService.deleteBySourceId(sourceId);

        return "delete success";
    }

    /**
     * 按 tenantId 删除向量数据（多租户隔离）
     * <p>
     * HTTP Method: DELETE
     * URL: /rag/tenant/{tenantId}
     * <p>
     * Path Param:
     * - tenantId: 租户标识
     * <p>
     * Effect:
     * - 删除该租户下所有 RAG 数据
     */
    @DeleteMapping("/tenant/{tenantId}")
    public String deleteByTenant(@PathVariable String tenantId) {

        if (StrUtil.isBlank(tenantId)) {
            return "tenantId is blank";
        }

        ragIngestService.deleteByTenantId(tenantId);

        return "delete success";
    }

    /**
     * 全量删除（谨慎使用）
     * <p>
     * HTTP Method: DELETE
     * URL: /rag/all
     * <p>
     * Effect:
     * - 删除所有 RAG 向量数据（通常用于测试环境）
     */
    @DeleteMapping("/all")
    public String deleteAll() {

        ragIngestService.delete(null);

        return "delete all success";
    }
}