package io.github.atengk.ai.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import io.github.atengk.ai.service.RagIngestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * RAG 文档摄取服务实现（Spring AI 企业级版本）
 *
 * 负责将 Resource 解析为 Document，并完成元数据标准化、清洗、切分、幂等控制与向量库写入。
 *
 * @author Ateng
 * @since 2026-04-21
 */
@Service
public class RagIngestServiceImpl implements RagIngestService {

    private static final Logger log = LoggerFactory.getLogger(RagIngestServiceImpl.class);

    private static final String DEFAULT_SOURCE_TYPE = "RESOURCE";
    private static final String INGEST_MODE_FULL = "full";
    private static final String INGEST_MODE_INCREMENTAL = "incremental";
    private static final String INGEST_MODE_REBUILD = "rebuild";
    private static final String INGEST_MODE_PREVIEW = "preview";
    private static final String SCHEMA_VERSION = "1";
    private static final String DOCUMENT_VERSION_DEFAULT = "1";

    private final VectorStore vectorStore;
    private final int defaultChunkSize;
    private final int defaultChunkOverlap;

    private final ConcurrentMap<String, String> sourceHashCache = new ConcurrentHashMap<>();

    public RagIngestServiceImpl(VectorStore vectorStore,
                                @Value("${app.rag.chunk-size:1000}") int defaultChunkSize,
                                @Value("${app.rag.chunk-overlap:200}") int defaultChunkOverlap) {
        this.vectorStore = Objects.requireNonNull(vectorStore, "vectorStore must not be null");
        this.defaultChunkSize = defaultChunkSize > 0 ? defaultChunkSize : 1000;
        this.defaultChunkOverlap = Math.max(0, defaultChunkOverlap);
    }

    @Override
    public int ingest(Resource resource, Map<String, Object> metadata) {
        Map<String, Object> normalizedMetadata = buildResourceMetadata(resource, metadata, INGEST_MODE_FULL);
        List<Document> parsedDocuments = parse(resource, normalizedMetadata);
        List<Document> processedDocuments = preprocess(parsedDocuments, normalizedMetadata);
        List<Document> chunkDocuments = splitDocuments(processedDocuments, defaultChunkSize, defaultChunkOverlap);
        write(chunkDocuments);
        return chunkDocuments.size();
    }

    @Override
    public int ingest(List<Resource> resources, Map<String, Object> metadata) {
        if (CollUtil.isEmpty(resources)) {
            return 0;
        }

        int total = 0;
        for (Resource resource : resources) {
            total += ingest(resource, metadata);
        }
        return total;
    }

    @Override
    public List<Document> parse(Resource resource, Map<String, Object> metadata) {
        if (resource == null) {
            return Collections.emptyList();
        }

        validateMetadata(metadata);

        TikaDocumentReader reader = new TikaDocumentReader(resource);
        List<Document> rawDocuments = safeRead(reader);
        if (CollUtil.isEmpty(rawDocuments)) {
            log.warn("RAG ingest parse returned empty documents, resource={}", safeResourceName(resource));
            return Collections.emptyList();
        }

        String contentHash = calculateContentHash(rawDocuments);
        Map<String, Object> baseMetadata = new LinkedHashMap<>(normalizeMetadata(metadata));
        baseMetadata.put(METADATA_CONTENT_HASH, contentHash);
        baseMetadata.put(METADATA_DOCUMENT_VERSION, ObjectUtil.defaultIfNull(baseMetadata.get(METADATA_DOCUMENT_VERSION), DOCUMENT_VERSION_DEFAULT));
        baseMetadata.put(METADATA_SCHEMA_VERSION, SCHEMA_VERSION);
        baseMetadata.put(METADATA_UPDATED_AT, DateUtil.now());
        baseMetadata.putIfAbsent(METADATA_CREATED_AT, DateUtil.now());
        baseMetadata.putIfAbsent(METADATA_SOURCE_ID, deriveSourceId(resource));
        baseMetadata.putIfAbsent(METADATA_SOURCE_TYPE, resolveSourceType(resource));
        baseMetadata.putIfAbsent(METADATA_SOURCE_URI, resolveSourceUri(resource));
        baseMetadata.putIfAbsent(METADATA_SOURCE_NAME, resolveSourceName(resource));
        baseMetadata.putIfAbsent(METADATA_INGEST_MODE, INGEST_MODE_FULL);

        List<Document> parsedDocuments = new ArrayList<>(rawDocuments.size());
        for (int i = 0; i < rawDocuments.size(); i++) {
            Document rawDocument = rawDocuments.get(i);
            String text = rawDocument == null ? null : rawDocument.getText();
            String documentId = buildDocumentId(baseMetadata, i, contentHash);
            Map<String, Object> currentMetadata = new LinkedHashMap<>();
            if (rawDocument != null && MapUtil.isNotEmpty(rawDocument.getMetadata())) {
                currentMetadata.putAll(rawDocument.getMetadata());
            }
            currentMetadata.putAll(baseMetadata);
            currentMetadata.put(METADATA_DOC_ID, documentId);
            currentMetadata.put(METADATA_CONTENT_HASH, contentHash);
            currentMetadata.put(METADATA_UPDATED_AT, DateUtil.now());

            Map<String, Object> mergedMetadata = normalizeMetadata(currentMetadata);
            parsedDocuments.add(new Document(documentId, StrUtil.nullToEmpty(text), mergedMetadata));
        }

        return parsedDocuments;
    }

    @Override
    public List<Document> preprocess(List<Document> documents, Map<String, Object> metadata) {
        if (CollUtil.isEmpty(documents)) {
            return Collections.emptyList();
        }

        Map<String, Object> normalizedMetadata = normalizeMetadata(metadata);
        List<Document> processed = new ArrayList<>(documents.size());

        for (Document document : documents) {
            if (document == null) {
                continue;
            }

            String cleanedText = normalizeContent(document.getText());
            if (StrUtil.isBlank(cleanedText)) {
                continue;
            }

            Map<String, Object> currentMetadata = new LinkedHashMap<>();
            if (MapUtil.isNotEmpty(document.getMetadata())) {
                currentMetadata.putAll(document.getMetadata());
            }
            currentMetadata.putAll(normalizedMetadata);
            currentMetadata.put(METADATA_UPDATED_AT, DateUtil.now());

            Map<String, Object> mergedMetadata = normalizeMetadata(currentMetadata);
            processed.add(new Document(document.getId(), cleanedText, mergedMetadata));
        }

        return processed;
    }

    @Override
    public List<Document> split(Document document, int chunkSize, int overlap) {
        if (document == null || StrUtil.isBlank(document.getText())) {
            return Collections.emptyList();
        }

        if (chunkSize <= 0) {
            chunkSize = defaultChunkSize;
        }

        if (overlap < 0) {
            overlap = 0;
        }

        if (overlap >= chunkSize) {
            overlap = Math.max(0, chunkSize / 5);
        }

        List<String> chunks = splitContent(document.getText(), chunkSize, overlap);
        if (CollUtil.isEmpty(chunks)) {
            return Collections.emptyList();
        }

        List<Document> chunkDocuments = new ArrayList<>(chunks.size());
        String sourceId = String.valueOf(document.getMetadata().getOrDefault(METADATA_SOURCE_ID, IdUtil.fastSimpleUUID()));
        String parentDocumentId = document.getId();

        for (int i = 0; i < chunks.size(); i++) {
            String chunkText = chunks.get(i);
            String chunkId = buildChunkId(parentDocumentId, i);
            Map<String, Object> chunkMetadata = new LinkedHashMap<>();
            if (MapUtil.isNotEmpty(document.getMetadata())) {
                chunkMetadata.putAll(document.getMetadata());
            }
            chunkMetadata.put(METADATA_SOURCE_ID, sourceId);
            chunkMetadata.put(METADATA_DOC_ID, chunkId);
            chunkMetadata.put(METADATA_CHUNK_INDEX, i);
            chunkMetadata.put(METADATA_CHUNK_COUNT, chunks.size());
            chunkMetadata.put(METADATA_CHUNK_HASH, DigestUtil.sha256Hex(chunkText));
            chunkMetadata.put(METADATA_UPDATED_AT, DateUtil.now());

            Map<String, Object> mergedMetadata = normalizeMetadata(chunkMetadata);
            chunkDocuments.add(new Document(chunkId, chunkText, mergedMetadata));
        }

        return chunkDocuments;
    }

    @Override
    public void write(List<Document> documents) {
        if (CollUtil.isEmpty(documents)) {
            return;
        }

        List<Document> safeDocuments = deduplicateAndNormalize(documents);
        if (CollUtil.isEmpty(safeDocuments)) {
            return;
        }

        vectorStore.add(safeDocuments);
        log.info("RAG ingest write success, count={}", safeDocuments.size());
    }

    @Override
    public int ingestIncremental(Resource resource, Map<String, Object> metadata) {
        Map<String, Object> normalizedMetadata = buildResourceMetadata(resource, metadata, INGEST_MODE_INCREMENTAL);
        List<Document> parsedDocuments = parse(resource, normalizedMetadata);
        if (CollUtil.isEmpty(parsedDocuments)) {
            return 0;
        }

        String sourceId = String.valueOf(parsedDocuments.get(0).getMetadata().get(METADATA_SOURCE_ID));
        String contentHash = String.valueOf(parsedDocuments.get(0).getMetadata().get(METADATA_CONTENT_HASH));

        String cachedHash = sourceHashCache.get(sourceId);
        if (StrUtil.isNotBlank(cachedHash) && StrUtil.equals(cachedHash, contentHash)) {
            log.info("RAG incremental ingest skipped, sourceId={}, contentHash={}", sourceId, contentHash);
            return 0;
        }

        deleteBySourceId(sourceId);

        List<Document> processedDocuments = preprocess(parsedDocuments, normalizedMetadata);
        List<Document> chunkDocuments = splitDocuments(processedDocuments, defaultChunkSize, defaultChunkOverlap);
        write(chunkDocuments);

        sourceHashCache.put(sourceId, contentHash);
        return chunkDocuments.size();
    }

    @Override
    public void rebuild(Resource resource, Map<String, Object> metadata) {
        Map<String, Object> normalizedMetadata = buildResourceMetadata(resource, metadata, INGEST_MODE_REBUILD);
        List<Document> parsedDocuments = parse(resource, normalizedMetadata);
        if (CollUtil.isEmpty(parsedDocuments)) {
            return;
        }

        String sourceId = String.valueOf(parsedDocuments.get(0).getMetadata().get(METADATA_SOURCE_ID));
        deleteBySourceId(sourceId);

        List<Document> processedDocuments = preprocess(parsedDocuments, normalizedMetadata);
        List<Document> chunkDocuments = splitDocuments(processedDocuments, defaultChunkSize, defaultChunkOverlap);
        write(chunkDocuments);

        String contentHash = String.valueOf(parsedDocuments.get(0).getMetadata().get(METADATA_CONTENT_HASH));
        sourceHashCache.put(sourceId, contentHash);
    }

    @Override
    public void delete(Filter.Expression filterExpression) {
        if (filterExpression == null) {
            return;
        }

        vectorStore.delete(filterExpression);
        sourceHashCache.clear();
        log.info("RAG documents deleted by filter expression");
    }

    @Override
    public void deleteByDocumentIds(List<String> documentIds) {
        if (CollUtil.isEmpty(documentIds)) {
            return;
        }

        vectorStore.delete(documentIds);
        sourceHashCache.clear();
        log.info("RAG documents deleted by documentIds, count={}", documentIds.size());
    }

    @Override
    public void deleteBySourceId(String sourceId) {
        if (StrUtil.isBlank(sourceId)) {
            return;
        }

        Filter.Expression expression = new FilterExpressionBuilder().eq(METADATA_SOURCE_ID, sourceId).build();
        vectorStore.delete(expression);
        sourceHashCache.remove(sourceId);
        log.info("RAG documents deleted by sourceId={}", sourceId);
    }

    @Override
    public void deleteByTenantId(String tenantId) {
        if (StrUtil.isBlank(tenantId)) {
            return;
        }

        Filter.Expression expression = new FilterExpressionBuilder().eq(METADATA_TENANT_ID, tenantId).build();
        vectorStore.delete(expression);
        sourceHashCache.clear();
        log.info("RAG documents deleted by tenantId={}", tenantId);
    }

    @Override
    public Map<String, Object> normalizeMetadata(Map<String, Object> metadata) {
        if (MapUtil.isEmpty(metadata)) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put(METADATA_SCHEMA_VERSION, SCHEMA_VERSION);
            result.put(METADATA_CREATED_AT, DateUtil.now());
            result.put(METADATA_UPDATED_AT, DateUtil.now());
            return result;
        }

        Map<String, Object> normalized = new LinkedHashMap<>();
        metadata.forEach((key, value) -> {
            if (StrUtil.isBlank(key) || value == null) {
                return;
            }
            String normalizedKey = StrUtil.trim(key);
            Object normalizedValue = normalizeMetadataValue(value);
            if (normalizedValue != null) {
                normalized.put(normalizedKey, normalizedValue);
            }
        });

        normalized.putIfAbsent(METADATA_SCHEMA_VERSION, SCHEMA_VERSION);
        normalized.putIfAbsent(METADATA_CREATED_AT, DateUtil.now());
        normalized.put(METADATA_UPDATED_AT, DateUtil.now());
        return normalized;
    }

    @Override
    public void validateMetadata(Map<String, Object> metadata) {
        if (MapUtil.isEmpty(metadata)) {
            return;
        }

        for (Map.Entry<String, Object> entry : metadata.entrySet()) {
            if (StrUtil.isBlank(entry.getKey())) {
                throw new IllegalArgumentException("metadata key must not be blank");
            }
            if (entry.getValue() == null) {
                continue;
            }
            if (!isSupportedMetadataValue(entry.getValue())) {
                throw new IllegalArgumentException("unsupported metadata value type for key: " + entry.getKey());
            }
        }
    }

    @Override
    public Map<String, Object> mergeMetadata(Map<String, Object> baseMetadata, Map<String, Object> extraMetadata) {
        Map<String, Object> merged = new LinkedHashMap<>();
        if (MapUtil.isNotEmpty(baseMetadata)) {
            merged.putAll(baseMetadata);
        }
        if (MapUtil.isNotEmpty(extraMetadata)) {
            merged.putAll(extraMetadata);
        }
        return normalizeMetadata(merged);
    }

    @Override
    public List<Document> preview(Resource resource, Map<String, Object> metadata) {
        Map<String, Object> normalizedMetadata = buildResourceMetadata(resource, metadata, INGEST_MODE_PREVIEW);
        List<Document> parsedDocuments = parse(resource, normalizedMetadata);
        List<Document> processedDocuments = preprocess(parsedDocuments, normalizedMetadata);
        return splitDocuments(processedDocuments, defaultChunkSize, defaultChunkOverlap);
    }

    private List<Document> splitDocuments(List<Document> documents, int chunkSize, int overlap) {
        if (CollUtil.isEmpty(documents)) {
            return Collections.emptyList();
        }

        List<Document> result = new ArrayList<>();
        for (Document document : documents) {
            result.addAll(split(document, chunkSize, overlap));
        }
        return result;
    }

    private Map<String, Object> buildResourceMetadata(Resource resource, Map<String, Object> metadata, String ingestMode) {
        Map<String, Object> merged = new LinkedHashMap<>();
        if (MapUtil.isNotEmpty(metadata)) {
            merged.putAll(metadata);
        }

        merged.putIfAbsent(METADATA_SOURCE_ID, deriveSourceId(resource));
        merged.putIfAbsent(METADATA_SOURCE_TYPE, resolveSourceType(resource));
        merged.putIfAbsent(METADATA_SOURCE_NAME, resolveSourceName(resource));
        merged.putIfAbsent(METADATA_SOURCE_URI, resolveSourceUri(resource));
        merged.putIfAbsent(METADATA_DOCUMENT_VERSION, DOCUMENT_VERSION_DEFAULT);
        merged.put(METADATA_INGEST_MODE, ingestMode);
        merged.put(METADATA_SCHEMA_VERSION, SCHEMA_VERSION);
        merged.put(METADATA_UPDATED_AT, DateUtil.now());
        merged.putIfAbsent(METADATA_CREATED_AT, DateUtil.now());

        validateMetadata(merged);
        return normalizeMetadata(merged);
    }

    private List<Document> safeRead(TikaDocumentReader reader) {
        try {
            return reader.get();
        } catch (Exception ex) {
            throw new IllegalStateException("failed to read resource by TikaDocumentReader", ex);
        }
    }

    private String calculateContentHash(List<Document> documents) {
        if (CollUtil.isEmpty(documents)) {
            return DigestUtil.sha256Hex("");
        }

        StringBuilder builder = new StringBuilder();
        for (Document document : documents) {
            if (document == null || StrUtil.isBlank(document.getText())) {
                continue;
            }
            builder.append(document.getText());
            builder.append('\n');
        }
        return DigestUtil.sha256Hex(builder.toString());
    }

    private String buildDocumentId(Map<String, Object> metadata, int index, String contentHash) {
        String sourceId = String.valueOf(metadata.getOrDefault(METADATA_SOURCE_ID, IdUtil.fastSimpleUUID()));
        return DigestUtil.sha256Hex(sourceId + ":" + contentHash + ":" + index);
    }

    private String buildChunkId(String parentDocumentId, int index) {
        return DigestUtil.sha256Hex(parentDocumentId + ":" + index);
    }

    private String deriveSourceId(Resource resource) {
        String sourceKey = resolveSourceUri(resource);
        if (StrUtil.isBlank(sourceKey)) {
            sourceKey = safeResourceName(resource);
        }
        if (StrUtil.isBlank(sourceKey)) {
            sourceKey = IdUtil.fastSimpleUUID();
        }
        return "src_" + DigestUtil.sha256Hex(sourceKey);
    }

    private String resolveSourceType(Resource resource) {
        if (resource == null) {
            return DEFAULT_SOURCE_TYPE;
        }

        String typeName = resource.getClass().getSimpleName().toLowerCase();
        if (typeName.contains("classpath")) {
            return "CLASSPATH";
        }
        if (typeName.contains("file")) {
            return "FILE";
        }
        if (typeName.contains("url") || typeName.contains("http")) {
            return "URL";
        }
        return DEFAULT_SOURCE_TYPE;
    }

    private String resolveSourceUri(Resource resource) {
        if (resource == null) {
            return null;
        }
        try {
            URI uri = resource.getURI();
            return uri == null ? null : uri.toString();
        } catch (IOException ex) {
            return resource.getDescription();
        }
    }

    private String resolveSourceName(Resource resource) {
        if (resource == null) {
            return null;
        }
        String fileName = resource.getFilename();
        if (StrUtil.isNotBlank(fileName)) {
            return fileName;
        }
        String description = resource.getDescription();
        if (StrUtil.isNotBlank(description)) {
            return description;
        }
        return "unknown-resource";
    }

    private String safeResourceName(Resource resource) {
        if (resource == null) {
            return "unknown-resource";
        }
        String name = resource.getFilename();
        if (StrUtil.isBlank(name)) {
            name = resource.getDescription();
        }
        return StrUtil.blankToDefault(name, "unknown-resource");
    }

    private String normalizeContent(String content) {
        if (StrUtil.isBlank(content)) {
            return null;
        }

        String normalized = content
                .replace("\u0000", "")
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .replace('\t', ' ');

        normalized = normalized.replaceAll("[ ]{2,}", " ");
        normalized = normalized.replaceAll("\\n{3,}", "\n\n");
        normalized = normalized.trim();

        return StrUtil.isBlank(normalized) ? null : normalized;
    }

    private List<String> splitContent(String content, int chunkSize, int overlap) {
        if (StrUtil.isBlank(content)) {
            return Collections.emptyList();
        }

        int length = content.length();
        if (length <= chunkSize) {
            return Collections.singletonList(content.trim());
        }

        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < length) {
            int end = Math.min(start + chunkSize, length);
            if (end < length) {
                end = findBestBoundary(content, start, end, chunkSize);
            }

            if (end <= start) {
                end = Math.min(start + chunkSize, length);
            }

            String chunk = content.substring(start, end).trim();
            if (StrUtil.isNotBlank(chunk)) {
                chunks.add(chunk);
            }

            if (end >= length) {
                break;
            }

            int nextStart = end - overlap;
            if (nextStart <= start) {
                nextStart = end;
            }
            start = nextStart;
        }

        return chunks;
    }

    private int findBestBoundary(String content, int start, int end, int chunkSize) {
        int minBoundary = start + Math.max(1, chunkSize / 2);
        int boundary = end;

        for (int i = end - 1; i >= minBoundary; i--) {
            char c = content.charAt(i);
            if (Character.isWhitespace(c) || isSentenceBoundary(c)) {
                boundary = i + 1;
                break;
            }
        }

        return boundary;
    }

    private boolean isSentenceBoundary(char c) {
        return c == '.' || c == '!' || c == '?' || c == ';' || c == '。' || c == '！' || c == '？' || c == '；';
    }

    private Map<String, Object> normalizeDocumentMetadata(Map<String, Object> metadata) {
        return normalizeMetadata(metadata);
    }

    private Object normalizeMetadataValue(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof String
                || value instanceof Integer
                || value instanceof Long
                || value instanceof Double
                || value instanceof Float
                || value instanceof Boolean) {
            return value;
        }

        if (value instanceof Enum<?>) {
            return ((Enum<?>) value).name();
        }

        if (value instanceof Number) {
            return String.valueOf(value);
        }

        if (value instanceof Collection<?>) {
            return value.toString();
        }

        if (value instanceof Map<?, ?>) {
            return value.toString();
        }

        if (value.getClass().isArray()) {
            if (value instanceof Object[]) {
                return java.util.Arrays.deepToString((Object[]) value);
            }
            if (value instanceof int[]) {
                return java.util.Arrays.toString((int[]) value);
            }
            if (value instanceof long[]) {
                return java.util.Arrays.toString((long[]) value);
            }
            if (value instanceof double[]) {
                return java.util.Arrays.toString((double[]) value);
            }
            if (value instanceof float[]) {
                return java.util.Arrays.toString((float[]) value);
            }
            if (value instanceof boolean[]) {
                return java.util.Arrays.toString((boolean[]) value);
            }
            if (value instanceof byte[]) {
                return java.util.Arrays.toString((byte[]) value);
            }
            if (value instanceof short[]) {
                return java.util.Arrays.toString((short[]) value);
            }
            if (value instanceof char[]) {
                return java.util.Arrays.toString((char[]) value);
            }
        }

        if (value instanceof java.util.Date) {
            return DateUtil.format((java.util.Date) value, "yyyy-MM-dd HH:mm:ss");
        }

        if (value instanceof java.time.temporal.TemporalAccessor) {
            return value.toString();
        }

        return value.toString();
    }

    private boolean isSupportedMetadataValue(Object value) {
        return value instanceof String
                || value instanceof Integer
                || value instanceof Long
                || value instanceof Double
                || value instanceof Float
                || value instanceof Boolean
                || value instanceof Enum<?>
                || value instanceof Number
                || value instanceof Collection<?>
                || value instanceof Map<?, ?>
                || value.getClass().isArray()
                || value instanceof java.util.Date
                || value instanceof java.time.temporal.TemporalAccessor;
    }

    private List<Document> deduplicateAndNormalize(List<Document> documents) {
        if (CollUtil.isEmpty(documents)) {
            return Collections.emptyList();
        }

        Map<String, Document> ordered = new LinkedHashMap<>();
        for (Document document : documents) {
            if (document == null || StrUtil.isBlank(document.getText())) {
                continue;
            }
            Map<String, Object> mergedMetadata = normalizeDocumentMetadata(document.getMetadata());
            String id = StrUtil.blankToDefault(document.getId(), IdUtil.fastSimpleUUID());
            Document normalizedDocument = new Document(id, document.getText(), mergedMetadata);
            ordered.put(id, normalizedDocument);
        }

        return new ArrayList<>(ordered.values());
    }
}