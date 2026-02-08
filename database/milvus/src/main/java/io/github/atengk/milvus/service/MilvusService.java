package io.github.atengk.milvus.service;

import io.github.atengk.milvus.entity.*;
import jakarta.servlet.Filter;

import java.util.List;
import java.util.Optional;

public interface MilvusService {

    /* ========================= health ========================= */

    /**
     * Milvus 是否可用（健康检查）
     */
    boolean isHealthy();


    /* ========================= database ========================= */

    /**
     * 创建 database（幂等）
     */
    void createDatabaseIfNotExists(String databaseName);

    /**
     * 删除 database
     */
    void dropDatabase(String databaseName);


    /* ========================= collection ========================= */

    /**
     * 创建 collection（包含 schema + 默认向量索引）
     */
    void createCollection(CollectionSpec spec);

    /**
     * collection 是否存在
     */
    boolean collectionExists(String collectionName);

    /**
     * 加载 collection（运行态）
     */
    void loadCollection(String collectionName);

    /**
     * 释放 collection（资源管理）
     */
    void releaseCollection(String collectionName);

    /**
     * 获取 collection 真实 schema 信息
     */
    Optional<CollectionSpec> getCollection(String collectionName);

    /**
     * 删除 collection
     */
    void dropCollection(String collectionName);


    /* ========================= document ========================= */

    /**
     * 新增文档（不允许覆盖）
     */
    void add(String collectionName, List<VectorDocument> documents);

    /**
     * 根据 ID 查询
     */
    List<VectorDocument> getByIds(String collectionName, List<String> ids);

    /**
     * 根据 ID 删除
     */
    void deleteByIds(String collectionName, List<String> ids);

    /**
     * 根据条件表达式删除数据
     *
     * @param collectionName collection 名称
     * @param expr Milvus 表达式，例如：id in ["1","2"]
     */
    void deleteByExpr(String collectionName, String expr);

    /* ========================= vector search ========================= */

    /**
     * 相似度检索（默认返回 score）
     */
    List<SimilaritySearchResult> similaritySearch(SimilaritySearchRequest request);


    /* ========================= admin / maintenance ========================= */

    /**
     * 强制持久化（一般不需要手动调用）
     */
    void flush(String collectionName);
}
