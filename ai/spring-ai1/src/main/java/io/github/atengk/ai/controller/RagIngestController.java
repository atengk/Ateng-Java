package io.github.atengk.ai.controller;

import io.github.atengk.ai.entity.RagIngestRequest;
import io.github.atengk.ai.service.RagIngestService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/rag")
@RequiredArgsConstructor
public class RagIngestController {

    private final RagIngestService ragIngestService;

    /**
     * 批量写入
     */
    @PostMapping("/ingest")
    public Map<String, Object> ingest(@RequestBody RagIngestRequest request) {
        int count = ragIngestService.ingest(request);
        return Map.of(
                "status", "OK",
                "count", count
        );
    }

    /**
     * 单条写入
     */
    @PostMapping("/ingest/single")
    public String ingestSingle(@RequestParam String text) {
        ragIngestService.ingestSingle(text, null);
        return "OK";
    }

    /**
     * 简单查询，验证 RAG
     */
    @GetMapping("/search")
    public List<Document> search(
            @RequestParam String query,
            @RequestParam(defaultValue = "3") int topK
    ) {
        return ragIngestService.search(query, topK);
    }

    /**
     * 清空知识库
     */
    @DeleteMapping("/clear")
    public String clear() {
        ragIngestService.clearAll();
        return "CLEARED";
    }
}
