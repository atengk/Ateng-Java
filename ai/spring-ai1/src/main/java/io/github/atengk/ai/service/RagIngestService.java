package io.github.atengk.ai.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.core.io.Resource;

import java.util.List;
import java.util.Map;

/**
 * RAG 文档摄取服务
 *
 * <p>负责将 Resource 解析为 Document，并完成资源元数据抽取、元数据标准化、内容清洗、切分、幂等控制与向量库写入。</p>
 *
 * @author Ateng
 * @since 2026-04-21
 */
public interface RagIngestService {

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
     * @param metadata  统一元数据
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
     * @param metadata  资源元数据
     * @return 处理后的文档列表
     */
    List<Document> preprocess(List<Document> documents, Map<String, Object> metadata);

    /**
     * 对多个文档执行分块。
     *
     * @param documentList 原始文档列表
     * @param chunkSize    分块大小
     * @return 分块后的文档列表
     */
    List<Document> split(List<Document> documentList, int chunkSize);

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
     * 根据过滤表达式判断向量库中是否存在匹配的文档
     *
     * <p>通常用于幂等控制、数据存在性校验等场景。内部一般通过向量检索结合过滤条件实现，
     * 仅判断是否存在至少一条满足条件的数据，不保证返回完整结果。</p>
     *
     * @param expression 过滤表达式
     * @return true 表示存在，false 表示不存在
     */
    boolean exists(Filter.Expression expression);

    /**
     * 根据 sourceId 和 contentHash 判断文档是否已存在
     *
     * <p>用于 RAG 增量摄取场景的幂等控制。只有当 sourceId 与 contentHash 同时匹配时，
     * 才认为当前数据已存在（即内容未发生变化）。</p>
     *
     * @param sourceId    数据源唯一标识
     * @param contentHash 内容哈希值（通常为文档内容的摘要）
     * @return true 表示已存在，false 表示不存在或内容已变化
     */
    boolean exists(String sourceId, String contentHash);

    /**
     * 根据过滤表达式查询文档列表
     *
     * <p>用于按条件获取向量库中的文档数据，支持结合元数据进行过滤。
     * 返回结果数量由 topK 控制，不保证返回全部匹配数据。</p>
     *
     * @param expression 过滤表达式
     * @param topK       最大返回数量
     * @return 文档列表
     */
    List<Document> list(Filter.Expression expression, int topK);

    /**
     * 基于相似度查询文档
     *
     * @param query 查询文本
     * @param topK  返回结果数量
     * @return 相似文档列表
     */
    List<Document> search(String query, int topK);

    /**
     * 基于相似度查询文档
     *
     * <p>通过向量检索返回与 query 语义最相似的文档列表，
     * 支持结合过滤条件进行元数据约束。</p>
     *
     * @param query      查询文本
     * @param topK       返回结果数量
     * @param expression 过滤表达式（可为空）
     * @return 相似文档列表
     */
    List<Document> similaritySearch(String query, int topK, Filter.Expression expression);

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
     * @param baseMetadata  基础元数据
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