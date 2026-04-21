package io.github.atengk.ai.controller;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.ai.service.RagIngestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * RAG 文档摄取控制器（企业级 API 设计）
 * <p>
 * 提供基于 Spring AI 的 RAG 文档摄取、解析、预览与治理能力。
 * <p>
 * API 设计说明：
 * - 所有文件上传统一使用 multipart/form-data
 * - metadata 使用 application/x-www-form-urlencoded 或 query param 传递
 * - Resource 由 MultipartFile 自动转换
 * - 所有 delete 操作基于 path variable 或 filter expression
 * <p>
 * metadata 约定字段（推荐）：
 * - source.id        : 数据源唯一标识（必填）
 * - tenant.id        : 租户ID（多租户隔离）
 * - biz.tags         : 业务标签（逗号分隔或数组）
 * - security.acl     : 权限控制标识
 * - document.version : 文档版本号
 *
 * @author Ateng
 * @since 2026-04-21
 */
@RestController
@RequestMapping("/rag")
public class RagIngestController {

    private static final Logger log = LoggerFactory.getLogger(RagIngestController.class);

    @Autowired
    private RagIngestService ragIngestService;

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