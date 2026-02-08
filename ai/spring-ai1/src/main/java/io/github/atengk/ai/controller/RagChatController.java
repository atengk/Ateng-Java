package io.github.atengk.ai.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * RAG 对话接口
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ai/rag")
@Slf4j
public class RagChatController {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    @GetMapping("/chat")
    public String chat(@RequestParam String message) {

        // 构建 RAG 增强器：在模型回答前，先根据用户问题去向量库检索相关文档
        RetrievalAugmentationAdvisor advisor = RetrievalAugmentationAdvisor
                .builder()
                // 使用向量检索器，从 VectorStore（如 Milvus）中查找相似文档
                .documentRetriever(
                        VectorStoreDocumentRetriever
                                .builder()
                                // 指定实际使用的向量存储实现
                                .vectorStore(vectorStore)
                                .build()
                )
                .build();

        // 发送用户问题，并在推理前自动注入检索到的文档上下文
        return chatClient
                .prompt()
                .user(message)
                .advisors(advisor)
                .call()
                .content();
    }

}
