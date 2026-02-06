package io.github.atengk.ai.controller;

import io.github.atengk.ai.service.RagIngestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * RAG 对话接口
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ai/rag")
@Slf4j
public class RagChatController {

    private final ChatClient chatClient;
    private final RagIngestService ragIngestService;

    @GetMapping("/chat")
    public String chat(@RequestParam String question) {

        // 从 Milvus 检索
        List<Document> documents = ragIngestService.search(question, 5);

        // 拼上下文
        String context = buildContext(documents);


        // 构建 Prompt
        String prompt = """
                你是一个专业助手，请基于以下已知内容回答问题。
                如果无法从内容中得到答案，请明确说明不知道。

                【已知内容】
                %s

                【用户问题】
                %s
                """.formatted(context, question);

        // 调用模型
        log.info(prompt);
        return chatClient.prompt(prompt).call().content();
    }

    private String buildContext(List<Document> documents) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < documents.size(); i++) {
            builder.append("[").append(i + 1).append("] ")
                    .append(documents.get(i).getText())
                    .append("\n");
        }
        return builder.toString();
    }

}
