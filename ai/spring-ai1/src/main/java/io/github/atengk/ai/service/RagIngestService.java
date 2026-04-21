package io.github.atengk.ai.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.core.io.Resource;

import java.util.List;
import java.util.Map;

/**
 * RAG 文档摄取服务（Spring AI 企业级版本）
 *
 * 负责将资源解析为 Document，完成元数据标准化、清洗、切分、写入与治理。
 * 该接口面向常规 RAG / AI 应用开发场景，强调：
 * 1. 统一资源输入
 * 2. 规范化元数据
 * 3. 支持增量与重建
 * 4. 支持基于过滤表达式的数据治理
 *
 * @author Ateng
 * @since 2026-04-21
 */
public interface RagIngestService {

    String METADATA_SOURCE_ID = "source.id";
    String METADATA_SOURCE_TYPE = "source.type";
    String METADATA_SOURCE_URI = "source.uri";
    String METADATA_SOURCE_NAME = "source.name";
    String METADATA_TENANT_ID = "tenant.id";
    String METADATA_DOC_ID = "document.id";
    String METADATA_CONTENT_HASH = "content.hash";
    String METADATA_DOCUMENT_VERSION = "document.version";
    String METADATA_CONTENT_LANGUAGE = "content.language";
    String METADATA_SECURITY_ACL = "security.acl";
    String METADATA_BUSINESS_TAGS = "biz.tags";
    String METADATA_CHUNK_INDEX = "chunk.index";
    String METADATA_CHUNK_COUNT = "chunk.count";
    String METADATA_CHUNK_HASH = "chunk.hash";
    String METADATA_INGEST_BATCH_ID = "ingest.batch.id";
    String METADATA_INGEST_MODE = "ingest.mode";
    String METADATA_SCHEMA_VERSION = "schema.version";
    String METADATA_CREATED_AT = "createdAt";
    String METADATA_UPDATED_AT = "updatedAt";

    /**
     * 摄取单个资源，执行完整流程：读取、解析、清洗、切分、写入。
     *
     * @param resource 数据源
     * @param metadata 资源元数据
     * @return 写入的文档数量
     */
    int ingest(Resource resource, Map<String, Object> metadata);

    /**
     * 批量摄取资源。
     *
     * @param resources 资源列表
     * @param metadata 统一元数据
     * @return 写入的文档总数
     */
    int ingest(List<Resource> resources, Map<String, Object> metadata);

    /**
     * 解析资源为 Document，不执行写入。
     *
     * @param resource 数据源
     * @param metadata 资源元数据
     * @return 解析后的文档列表
     */
    List<Document> parse(Resource resource, Map<String, Object> metadata);

    /**
     * 预处理文档，例如清洗、去噪、规范化和元数据补全。
     *
     * @param documents 原始文档
     * @param metadata 资源元数据
     * @return 处理后的文档列表
     */
    List<Document> preprocess(List<Document> documents, Map<String, Object> metadata);

    /**
     * 对单个文档执行分块。
     *
     * @param document 原始文档
     * @param chunkSize 分块大小
     * @param overlap 重叠大小
     * @return 分块后的文档列表
     */
    List<Document> split(Document document, int chunkSize, int overlap);

    /**
     * 写入向量库。
     *
     * @param documents 已处理好的文档列表
     */
    void write(List<Document> documents);

    /**
     * 增量摄取，通常基于 source.id + content.hash 做幂等控制。
     *
     * @param resource 数据源
     * @param metadata 资源元数据
     * @return 写入的文档数量
     */
    int ingestIncremental(Resource resource, Map<String, Object> metadata);

    /**
     * 全量重建，通常用于版本变更、切分策略变更或内容整体失效。
     *
     * @param resource 数据源
     * @param metadata 资源元数据
     */
    void rebuild(Resource resource, Map<String, Object> metadata);

    /**
     * 删除匹配过滤条件的文档。
     *
     * @param filterExpression 过滤表达式
     */
    void delete(Filter.Expression filterExpression);

    /**
     * 按文档 ID 删除。
     *
     * @param documentIds 文档 ID 列表
     */
    void deleteByDocumentIds(List<String> documentIds);

    /**
     * 按来源 ID 删除。
     *
     * @param sourceId 来源 ID
     */
    void deleteBySourceId(String sourceId);

    /**
     * 按租户 ID 删除。
     *
     * @param tenantId 租户 ID
     */
    void deleteByTenantId(String tenantId);

    /**
     * 标准化元数据，补齐默认字段并统一命名。
     *
     * @param metadata 原始元数据
     * @return 标准化后的元数据
     */
    Map<String, Object> normalizeMetadata(Map<String, Object> metadata);

    /**
     * 校验元数据是否满足入库要求。
     *
     * @param metadata 元数据
     */
    void validateMetadata(Map<String, Object> metadata);

    /**
     * 合并基础元数据与附加元数据。
     *
     * @param baseMetadata 基础元数据
     * @param extraMetadata 附加元数据
     * @return 合并后的元数据
     */
    Map<String, Object> mergeMetadata(Map<String, Object> baseMetadata, Map<String, Object> extraMetadata);

    /**
     * 仅用于预览，不入库。
     *
     * @param resource 数据源
     * @param metadata 资源元数据
     * @return 预览文档列表
     */
    List<Document> preview(Resource resource, Map<String, Object> metadata);
}