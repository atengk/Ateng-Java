package io.github.atengk.ai.service;

import io.github.atengk.ai.entity.RagIngestRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RagIngestService {

    private final VectorStore vectorStore;

    /**
     * 批量写入知识
     */
    public int ingest(RagIngestRequest request) {
        List<Document> documents = request.getTexts()
                .stream()
                .map(text -> new Document(text, buildMetadata(request.getMetadata())))
                .collect(Collectors.toList());

        vectorStore.add(documents);
        return documents.size();
    }

    /**
     * 单条写入，方便测试
     */
    public void ingestSingle(String text, Map<String, Object> metadata) {
        vectorStore.add(List.of(new Document(text, buildMetadata(metadata))));
    }

    /**
     * 简单相似度查询，用于验证 RAG 是否生效
     */
    public List<Document> search(String query, int topK) {
        SearchRequest request = SearchRequest.builder()
                .query(query)
                .topK(topK)
                .build();

        return vectorStore.similaritySearch(request);
    }

    /**
     * 清空知识库（危险操作，慎用）
     */
    public void clearAll() {
        Filter.Expression expression =
                new Filter.Expression(
                        Filter.ExpressionType.EQ,
                        new Filter.Key("category"),
                        new Filter.Value("spring-ai")
                );

        vectorStore.delete(expression);
    }

    private Map<String, Object> buildMetadata(Map<String, Object> metadata) {
        return metadata == null ? Map.of() : metadata;
    }
}
