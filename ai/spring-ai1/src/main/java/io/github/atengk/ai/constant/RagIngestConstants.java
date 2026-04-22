package io.github.atengk.ai.constant;

import java.util.List;

/**
 * RAG 摄取相关常量
 *
 * <p>统一管理：
 * 1. 文本切分配置
 * 2. 元数据字段
 * 3. 默认值与模式
 * <p>
 * 避免业务代码中出现魔法值
 *
 * @author Ateng
 * @since 2026-04-21
 */
public final class RagIngestConstants {

    // =========================
    // 文本切分配置
    // =========================

    /**
     * 默认分块大小（token 粒度）
     */
    public static final int DEFAULT_CHUNK_SIZE = 800;

    /**
     * 最小分块字符数（避免切太碎）
     */
    public static final int MIN_CHUNK_SIZE_CHARS = 350;

    /**
     * 最小可嵌入长度（太短的不参与向量化）
     */
    public static final int MIN_CHUNK_LENGTH_TO_EMBED = 5;

    /**
     * 最大分块数量（防止异常数据）
     */
    public static final int MAX_NUM_CHUNKS = 10000;

    /**
     * 是否保留分隔符
     */
    public static final boolean KEEP_SEPARATOR = true;

    /**
     * 默认切分标点（中英文 + 换行）
     */
    public static final List<Character> DEFAULT_PUNCTUATION_MARKS = List.of(
            '.', '?', '!',
            '。', '？', '！',
            ';', '；',
            '\n'
    );


    // =========================
    // Metadata 字段定义
    // =========================

    /**
     * 数据源信息
     */
    public static final String METADATA_SOURCE_ID = "source.id";
    public static final String METADATA_SOURCE_TYPE = "source.type";
    public static final String METADATA_SOURCE_URI = "source.uri";
    public static final String METADATA_SOURCE_NAME = "source.name";

    /**
     * 租户信息（多租户场景）
     */
    public static final String METADATA_TENANT_ID = "tenant.id";

    /**
     * 文档信息（逻辑文档）
     */
    public static final String METADATA_DOC_ID = "document.id";
    public static final String METADATA_DOCUMENT_VERSION = "document.version";

    /**
     * 内容信息
     */
    public static final String METADATA_CONTENT_HASH = "content.hash";
    public static final String METADATA_CONTENT_LANGUAGE = "content.language";

    /**
     * 文件信息
     */
    public static final String METADATA_FILE_NAME = "file.name";
    public static final String METADATA_FILE_SIZE = "file.size";
    public static final String METADATA_FILE_TYPE = "file.type";
    public static final String METADATA_FILE_EXTENSION = "file.extension";

    /**
     * 权限与业务标签
     */
    public static final String METADATA_SECURITY_ACL = "security.acl";
    public static final String METADATA_BUSINESS_TAGS = "biz.tags";

    /**
     * 分块信息（chunk 级别）
     */
    public static final String METADATA_CHUNK_INDEX = "chunk.index";
    public static final String METADATA_CHUNK_COUNT = "chunk.count";
    public static final String METADATA_CHUNK_HASH = "chunk.hash";

    /**
     * 摄取信息
     */
    public static final String METADATA_INGEST_BATCH_ID = "ingest.batch.id";
    public static final String METADATA_INGEST_MODE = "ingest.mode";

    /**
     * 元数据版本
     */
    public static final String METADATA_SCHEMA_VERSION = "schema.version";

    /**
     * 时间字段
     */
    public static final String METADATA_CREATED_AT = "createdAt";
    public static final String METADATA_UPDATED_AT = "updatedAt";


    // =========================
    // 摄取模式
    // =========================

    /**
     * 全量摄取（直接覆盖）
     */
    public static final String INGEST_MODE_FULL = "full";

    /**
     * 增量摄取（基于 hash 判断）
     */
    public static final String INGEST_MODE_INCREMENTAL = "incremental";

    /**
     * 重建（先删后写）
     */
    public static final String INGEST_MODE_REBUILD = "rebuild";

    /**
     * 预览（不入库）
     */
    public static final String INGEST_MODE_PREVIEW = "preview";


    // =========================
    // 默认值
    // =========================

    /**
     * 默认数据源类型
     */
    public static final String DEFAULT_SOURCE_TYPE = "RESOURCE";

    /**
     * 默认 schema 版本
     */
    public static final String DEFAULT_SCHEMA_VERSION = "1";

    /**
     * 默认文档版本
     */
    public static final String DEFAULT_DOCUMENT_VERSION = "1";

    /**
     * sourceId 前缀
     */
    public static final String DEFAULT_SOURCE_ID_PREFIX = "src_";

    /**
     * 常见资源类型
     */
    public static final String DEFAULT_SOURCE_TYPE_CLASSPATH = "CLASSPATH";
    public static final String DEFAULT_SOURCE_TYPE_FILE = "FILE";
    public static final String DEFAULT_SOURCE_TYPE_URL = "URL";
    public static final String DEFAULT_SOURCE_TYPE_STREAM = "STREAM";

    /**
     * 未知资源名称
     */
    public static final String DEFAULT_UNKNOWN_RESOURCE_NAME = "unknown-resource";


    private RagIngestConstants() {
    }
}