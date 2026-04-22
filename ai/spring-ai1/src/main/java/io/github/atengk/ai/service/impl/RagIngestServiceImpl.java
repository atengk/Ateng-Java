package io.github.atengk.ai.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import io.github.atengk.ai.constant.RagIngestConstants;
import io.github.atengk.ai.service.RagIngestService;
import org.apache.tika.Tika;
import org.apache.tika.io.TikaInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.core.io.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.temporal.TemporalAccessor;
import java.util.*;

/**
 * RAG 文档摄取服务实现
 *
 * <p>负责将 Resource 解析为 Document，并完成资源元数据抽取、元数据标准化、内容清洗、切分、幂等控制与向量库写入。</p>
 *
 * @author Ateng
 * @since 2026-04-21
 */
@Service
public class RagIngestServiceImpl implements RagIngestService {

    private static final Logger log = LoggerFactory.getLogger(RagIngestServiceImpl.class);

    private static final Tika TIKA = new Tika();

    private final VectorStore vectorStore;

    public RagIngestServiceImpl(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Override
    public int ingest(Resource resource, Map<String, Object> metadata) {
        Map<String, Object> normalizedMetadata = buildResourceMetadata(resource, metadata, RagIngestConstants.INGEST_MODE_FULL);
        List<Document> parsedDocuments = parse(resource, normalizedMetadata);
        if (CollUtil.isEmpty(parsedDocuments)) {
            return 0;
        }

        List<Document> processedDocuments = preprocess(parsedDocuments, normalizedMetadata);
        List<Document> chunkDocuments = split(processedDocuments, RagIngestConstants.DEFAULT_CHUNK_SIZE);
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
            log.warn("RAG 文档解析结果为空，resource={}", safeResourceName(resource));
            return Collections.emptyList();
        }

        String contentHash = calculateContentHash(rawDocuments);
        String now = now();

        Map<String, Object> baseMetadata = new LinkedHashMap<>(normalizeMetadata(metadata));
        baseMetadata.put(RagIngestConstants.METADATA_CONTENT_HASH, contentHash);
        baseMetadata.put(RagIngestConstants.METADATA_DOCUMENT_VERSION, ObjectUtil.defaultIfNull(
                baseMetadata.get(RagIngestConstants.METADATA_DOCUMENT_VERSION),
                RagIngestConstants.DEFAULT_DOCUMENT_VERSION
        ));
        baseMetadata.put(RagIngestConstants.METADATA_SCHEMA_VERSION, RagIngestConstants.DEFAULT_SCHEMA_VERSION);
        baseMetadata.put(RagIngestConstants.METADATA_UPDATED_AT, now);
        baseMetadata.putIfAbsent(RagIngestConstants.METADATA_CREATED_AT, now);
        baseMetadata.putIfAbsent(RagIngestConstants.METADATA_SOURCE_ID, deriveSourceId(resource));
        baseMetadata.putIfAbsent(RagIngestConstants.METADATA_SOURCE_TYPE, resolveSourceType(resource));
        baseMetadata.putIfAbsent(RagIngestConstants.METADATA_SOURCE_URI, resolveSourceUri(resource));
        baseMetadata.putIfAbsent(RagIngestConstants.METADATA_SOURCE_NAME, resolveSourceName(resource));
        baseMetadata.putIfAbsent(RagIngestConstants.METADATA_INGEST_MODE, RagIngestConstants.INGEST_MODE_FULL);

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
            currentMetadata.put(RagIngestConstants.METADATA_DOC_ID, documentId);
            currentMetadata.put(RagIngestConstants.METADATA_CONTENT_HASH, contentHash);
            currentMetadata.put(RagIngestConstants.METADATA_UPDATED_AT, now);

            parsedDocuments.add(new Document(documentId, StrUtil.nullToEmpty(text), normalizeMetadata(currentMetadata)));
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
            currentMetadata.put(RagIngestConstants.METADATA_UPDATED_AT, now());

            processed.add(new Document(document.getId(), cleanedText, normalizeMetadata(currentMetadata)));
        }

        return processed;
    }

    @Override
    public List<Document> split(List<Document> documentList, int chunkSize) {
        if (CollUtil.isEmpty(documentList)) {
            return Collections.emptyList();
        }

        if (chunkSize <= 0) {
            chunkSize = RagIngestConstants.DEFAULT_CHUNK_SIZE;
        }

        TokenTextSplitter splitter = TokenTextSplitter.builder()
                .withChunkSize(chunkSize)
                .withMinChunkSizeChars(RagIngestConstants.MIN_CHUNK_SIZE_CHARS)
                .withMinChunkLengthToEmbed(RagIngestConstants.MIN_CHUNK_LENGTH_TO_EMBED)
                .withMaxNumChunks(RagIngestConstants.MAX_NUM_CHUNKS)
                .withKeepSeparator(RagIngestConstants.KEEP_SEPARATOR)
                .withPunctuationMarks(RagIngestConstants.DEFAULT_PUNCTUATION_MARKS)
                .build();

        List<Document> splitDocs = splitter.apply(documentList);
        if (CollUtil.isEmpty(splitDocs)) {
            return Collections.emptyList();
        }

        List<Document> validChunks = new ArrayList<>(splitDocs.size());
        for (Document chunk : splitDocs) {
            if (chunk == null || StrUtil.isBlank(chunk.getText())) {
                continue;
            }
            validChunks.add(chunk);
        }

        if (CollUtil.isEmpty(validChunks)) {
            return Collections.emptyList();
        }

        List<Document> result = new ArrayList<>(validChunks.size());
        int totalCount = validChunks.size();

        for (int i = 0; i < totalCount; i++) {
            Document chunk = validChunks.get(i);

            Map<String, Object> metadata = new LinkedHashMap<>();
            if (MapUtil.isNotEmpty(chunk.getMetadata())) {
                metadata.putAll(chunk.getMetadata());
            }

            metadata.put(RagIngestConstants.METADATA_CHUNK_INDEX, i);
            metadata.put(RagIngestConstants.METADATA_CHUNK_COUNT, totalCount);
            metadata.put(RagIngestConstants.METADATA_CHUNK_HASH, DigestUtil.sha256Hex(chunk.getText()));
            metadata.put(RagIngestConstants.METADATA_UPDATED_AT, now());

            String chunkId = StrUtil.blankToDefault(chunk.getId(), IdUtil.fastSimpleUUID());
            result.add(new Document(chunkId, chunk.getText(), normalizeMetadata(metadata)));
        }

        return result;
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

        Map<String, Object> metadata = safeDocuments.get(0).getMetadata();
        String sourceId = metadata == null ? null : String.valueOf(metadata.get(RagIngestConstants.METADATA_SOURCE_ID));
        String sourceName = metadata == null ? null : String.valueOf(metadata.get(RagIngestConstants.METADATA_SOURCE_NAME));

        try {
            vectorStore.add(safeDocuments);
            log.info("RAG 文档写入成功，数量={}，sourceId={}, sourceName={}", safeDocuments.size(), sourceId, sourceName);
        } catch (Exception ex) {
            log.error("RAG 文档写入失败，数量={}，sourceId={}, sourceName={}", safeDocuments.size(), sourceId, sourceName, ex);
            throw ex;
        }
    }

    @Override
    public int ingestIncremental(Resource resource, Map<String, Object> metadata) {
        Map<String, Object> normalizedMetadata = buildResourceMetadata(resource, metadata, RagIngestConstants.INGEST_MODE_INCREMENTAL);
        List<Document> parsedDocuments = parse(resource, normalizedMetadata);
        if (CollUtil.isEmpty(parsedDocuments)) {
            return 0;
        }

        Map<String, Object> firstMetadata = parsedDocuments.get(0).getMetadata();
        String sourceId = getMetadataString(firstMetadata, RagIngestConstants.METADATA_SOURCE_ID);
        String contentHash = getMetadataString(firstMetadata, RagIngestConstants.METADATA_CONTENT_HASH);

        if (exists(sourceId, contentHash)) {
            log.info("RAG 增量摄取跳过（内容未变化），sourceId={}, contentHash={}", sourceId, contentHash);
            return 0;
        }

        deleteBySourceId(sourceId);

        List<Document> processedDocuments = preprocess(parsedDocuments, normalizedMetadata);
        List<Document> chunkDocuments = split(processedDocuments, RagIngestConstants.DEFAULT_CHUNK_SIZE);
        write(chunkDocuments);

        return chunkDocuments.size();
    }

    @Override
    public void rebuild(Resource resource, Map<String, Object> metadata) {
        Map<String, Object> normalizedMetadata = buildResourceMetadata(resource, metadata, RagIngestConstants.INGEST_MODE_REBUILD);
        List<Document> parsedDocuments = parse(resource, normalizedMetadata);
        if (CollUtil.isEmpty(parsedDocuments)) {
            return;
        }

        Map<String, Object> firstMetadata = parsedDocuments.get(0).getMetadata();
        String sourceId = getMetadataString(firstMetadata, RagIngestConstants.METADATA_SOURCE_ID);

        deleteBySourceId(sourceId);

        List<Document> processedDocuments = preprocess(parsedDocuments, normalizedMetadata);
        List<Document> chunkDocuments = split(processedDocuments, RagIngestConstants.DEFAULT_CHUNK_SIZE);
        write(chunkDocuments);
    }

    @Override
    public boolean exists(Filter.Expression expression) {
        if (expression == null) {
            return false;
        }
        SearchRequest request = SearchRequest.builder()
                .query("exist-check")
                .topK(1)
                .filterExpression(expression)
                .build();
        return CollUtil.isNotEmpty(vectorStore.similaritySearch(request));
    }

    @Override
    public boolean exists(String sourceId, String contentHash) {
        if (StrUtil.isBlank(sourceId) || StrUtil.isBlank(contentHash)) {
            return false;
        }

        Filter.Expression expression = new FilterExpressionBuilder()
                .and(
                        new FilterExpressionBuilder().eq(RagIngestConstants.METADATA_SOURCE_ID, sourceId),
                        new FilterExpressionBuilder().eq(RagIngestConstants.METADATA_CONTENT_HASH, contentHash)
                )
                .build();

        SearchRequest request = SearchRequest.builder()
                .query("exist-check")
                .topK(1)
                .filterExpression(expression)
                .build();

        return CollUtil.isNotEmpty(vectorStore.similaritySearch(request));
    }

    @Override
    public List<Document> list(Filter.Expression expression, int topK) {
        if (expression == null) {
            return Collections.emptyList();
        }
        SearchRequest request = SearchRequest.builder()
                .query("list-check")
                .topK(topK)
                .filterExpression(expression)
                .build();
        return vectorStore.similaritySearch(request);
    }

    @Override
    public List<Document> search(String query, int topK) {
        return similaritySearch(query, topK, null);
    }

    @Override
    public List<Document> similaritySearch(String query, int topK, Filter.Expression expression) {

        if (StrUtil.isBlank(query)) {
            return Collections.emptyList();
        }

        if (topK <= 0) {
            topK = 5;
        }

        try {

            SearchRequest.Builder builder = SearchRequest.builder()
                    .query(query)
                    .topK(topK);

            if (expression != null) {
                builder.filterExpression(expression);
            }

            List<Document> results = vectorStore.similaritySearch(builder.build());

            log.info("RAG 相似度查询完成，query={}, topK={}, 返回数量={}",
                    query, topK, CollUtil.size(results));

            return CollUtil.isEmpty(results) ? Collections.emptyList() : results;

        } catch (Exception ex) {
            log.error("RAG 相似度查询失败，query={}", query, ex);
            throw ex;
        }
    }

    @Override
    public void delete(Filter.Expression filterExpression) {
        if (filterExpression == null) {
            return;
        }

        vectorStore.delete(filterExpression);
        log.info("RAG 文档已按过滤条件删除");
    }

    @Override
    public void deleteByDocumentIds(List<String> documentIds) {
        if (CollUtil.isEmpty(documentIds)) {
            return;
        }

        vectorStore.delete(documentIds);
        log.info("RAG 文档已按 documentIds 删除，数量={}", documentIds.size());
    }

    @Override
    public void deleteBySourceId(String sourceId) {
        if (StrUtil.isBlank(sourceId)) {
            return;
        }

        Filter.Expression expression = new FilterExpressionBuilder()
                .eq(RagIngestConstants.METADATA_SOURCE_ID, sourceId)
                .build();

        vectorStore.delete(expression);
        log.info("RAG 文档已按 sourceId 删除，sourceId={}", sourceId);
    }

    @Override
    public void deleteByTenantId(String tenantId) {
        if (StrUtil.isBlank(tenantId)) {
            return;
        }

        Filter.Expression expression = new FilterExpressionBuilder()
                .eq(RagIngestConstants.METADATA_TENANT_ID, tenantId)
                .build();

        vectorStore.delete(expression);
        log.info("RAG 文档已按 tenantId 删除，tenantId={}", tenantId);
    }

    @Override
    public Map<String, Object> normalizeMetadata(Map<String, Object> metadata) {
        String now = now();

        if (MapUtil.isEmpty(metadata)) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put(RagIngestConstants.METADATA_SCHEMA_VERSION, RagIngestConstants.DEFAULT_SCHEMA_VERSION);
            result.put(RagIngestConstants.METADATA_CREATED_AT, now);
            result.put(RagIngestConstants.METADATA_UPDATED_AT, now);
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

        normalized.putIfAbsent(RagIngestConstants.METADATA_SCHEMA_VERSION, RagIngestConstants.DEFAULT_SCHEMA_VERSION);
        normalized.putIfAbsent(RagIngestConstants.METADATA_CREATED_AT, now);
        normalized.put(RagIngestConstants.METADATA_UPDATED_AT, now);
        return normalized;
    }

    @Override
    public void validateMetadata(Map<String, Object> metadata) {
        if (MapUtil.isEmpty(metadata)) {
            return;
        }

        for (Map.Entry<String, Object> entry : metadata.entrySet()) {
            if (StrUtil.isBlank(entry.getKey())) {
                throw new IllegalArgumentException("元数据 key 不能为空");
            }
            if (entry.getValue() == null) {
                continue;
            }
            if (!isSupportedMetadataValue(entry.getValue())) {
                throw new IllegalArgumentException("不支持的元数据值类型，key=" + entry.getKey());
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
        Map<String, Object> normalizedMetadata = buildResourceMetadata(resource, metadata, RagIngestConstants.INGEST_MODE_PREVIEW);
        List<Document> parsedDocuments = parse(resource, normalizedMetadata);
        List<Document> processedDocuments = preprocess(parsedDocuments, normalizedMetadata);
        return split(processedDocuments, RagIngestConstants.DEFAULT_CHUNK_SIZE);
    }

    private Map<String, Object> buildResourceMetadata(Resource resource, Map<String, Object> metadata, String ingestMode) {
        Map<String, Object> merged = new LinkedHashMap<>();
        if (MapUtil.isNotEmpty(metadata)) {
            merged.putAll(metadata);
        }

        String now = now();

        merged.putIfAbsent(RagIngestConstants.METADATA_SOURCE_ID, deriveSourceId(resource));
        merged.putIfAbsent(RagIngestConstants.METADATA_SOURCE_TYPE, resolveSourceType(resource));
        merged.putIfAbsent(RagIngestConstants.METADATA_SOURCE_NAME, resolveSourceName(resource));
        merged.putIfAbsent(RagIngestConstants.METADATA_SOURCE_URI, resolveSourceUri(resource));
        merged.putIfAbsent(RagIngestConstants.METADATA_DOCUMENT_VERSION, RagIngestConstants.DEFAULT_DOCUMENT_VERSION);
        merged.put(RagIngestConstants.METADATA_INGEST_MODE, ingestMode);
        merged.put(RagIngestConstants.METADATA_SCHEMA_VERSION, RagIngestConstants.DEFAULT_SCHEMA_VERSION);
        merged.put(RagIngestConstants.METADATA_UPDATED_AT, now);
        merged.putIfAbsent(RagIngestConstants.METADATA_CREATED_AT, now);

        addFileMetadata(resource, merged);

        validateMetadata(merged);
        return normalizeMetadata(merged);
    }

    private void addFileMetadata(Resource resource, Map<String, Object> merged) {
        if (resource == null || !resource.exists()) {
            return;
        }

        String fileName = safeResourceName(resource);
        if (StrUtil.isNotBlank(fileName)) {
            merged.putIfAbsent(RagIngestConstants.METADATA_FILE_NAME, fileName);
        }

        Long fileSize = safeContentLength(resource);
        if (fileSize != null && fileSize > 0) {
            merged.putIfAbsent(RagIngestConstants.METADATA_FILE_SIZE, fileSize);
        }

        String fileExtension = safeFileExtension(resource);
        if (StrUtil.isNotBlank(fileExtension)) {
            merged.putIfAbsent(RagIngestConstants.METADATA_FILE_EXTENSION, fileExtension);
        }

        String fileType = safeMimeType(resource);
        if (StrUtil.isNotBlank(fileType)) {
            merged.putIfAbsent(RagIngestConstants.METADATA_FILE_TYPE, fileType);
        }
    }

    private Long safeContentLength(Resource resource) {
        if (resource == null || resource instanceof InputStreamResource) {
            return null;
        }

        try {
            long contentLength = resource.contentLength();
            return contentLength > 0 ? contentLength : null;
        } catch (Exception ex) {
            log.debug("获取文件大小失败，resource={}", safeResourceName(resource), ex);
            return null;
        }
    }

    private String safeFileExtension(Resource resource) {
        if (resource == null) {
            return null;
        }

        String filename = resource.getFilename();
        if (StrUtil.isBlank(filename)) {
            return null;
        }

        String extName = FileUtil.extName(filename);
        return StrUtil.isBlank(extName) ? null : extName.toLowerCase(Locale.ROOT);
    }

    private String safeMimeType(Resource resource) {
        if (resource == null || resource instanceof InputStreamResource) {
            return null;
        }

        try (InputStream inputStream = resource.getInputStream();
             TikaInputStream tikaInputStream = TikaInputStream.get(inputStream)) {

            return TIKA.detect(tikaInputStream);

        } catch (Exception ex) {
            log.debug("检测文件类型失败，resource={}", safeResourceName(resource), ex);
            return null;
        }
    }

    private List<Document> safeRead(TikaDocumentReader reader) {
        try {
            return reader.get();
        } catch (Exception ex) {
            throw new IllegalStateException("使用 TikaDocumentReader 读取资源失败", ex);
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
            builder.append(document.getText()).append('\n');
        }
        return DigestUtil.sha256Hex(builder.toString());
    }

    private String buildDocumentId(Map<String, Object> metadata, int index, String contentHash) {
        String sourceId = getMetadataString(metadata, RagIngestConstants.METADATA_SOURCE_ID);
        if (StrUtil.isBlank(sourceId)) {
            sourceId = IdUtil.fastSimpleUUID();
        }
        return DigestUtil.sha256Hex(sourceId + ':' + contentHash + ':' + index);
    }

    private String deriveSourceId(Resource resource) {
        String sourceKey = resolveSourceUri(resource);
        if (StrUtil.isBlank(sourceKey)) {
            sourceKey = safeResourceName(resource);
        }
        if (StrUtil.isBlank(sourceKey)) {
            sourceKey = IdUtil.fastSimpleUUID();
        }
        return RagIngestConstants.DEFAULT_SOURCE_ID_PREFIX + DigestUtil.sha256Hex(sourceKey);
    }

    private String resolveSourceType(Resource resource) {
        if (resource == null) {
            return RagIngestConstants.DEFAULT_SOURCE_TYPE;
        }
        if (resource instanceof ClassPathResource) {
            return RagIngestConstants.DEFAULT_SOURCE_TYPE_CLASSPATH;
        }
        if (resource instanceof FileSystemResource) {
            return RagIngestConstants.DEFAULT_SOURCE_TYPE_FILE;
        }
        if (resource instanceof UrlResource) {
            return RagIngestConstants.DEFAULT_SOURCE_TYPE_URL;
        }
        if (resource instanceof InputStreamResource) {
            return RagIngestConstants.DEFAULT_SOURCE_TYPE_STREAM;
        }
        return RagIngestConstants.DEFAULT_SOURCE_TYPE;
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

        return RagIngestConstants.DEFAULT_UNKNOWN_RESOURCE_NAME;
    }

    private String safeResourceName(Resource resource) {
        if (resource == null) {
            return RagIngestConstants.DEFAULT_UNKNOWN_RESOURCE_NAME;
        }

        String name = resource.getFilename();
        if (StrUtil.isBlank(name)) {
            name = resource.getDescription();
        }

        return StrUtil.blankToDefault(name, RagIngestConstants.DEFAULT_UNKNOWN_RESOURCE_NAME);
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

        if (value instanceof Date) {
            return DateUtil.format((Date) value, "yyyy-MM-dd HH:mm:ss");
        }

        if (value instanceof TemporalAccessor) {
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
                || value instanceof Date
                || value instanceof TemporalAccessor;
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

            Map<String, Object> mergedMetadata = normalizeMetadata(document.getMetadata());
            String id = StrUtil.blankToDefault(document.getId(), IdUtil.fastSimpleUUID());
            ordered.put(id, new Document(id, document.getText(), mergedMetadata));
        }

        return new ArrayList<>(ordered.values());
    }

    private String getMetadataString(Map<String, Object> metadata, String key) {
        if (MapUtil.isEmpty(metadata) || StrUtil.isBlank(key)) {
            return null;
        }

        Object value = metadata.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private String now() {
        return DateUtil.now();
    }
}