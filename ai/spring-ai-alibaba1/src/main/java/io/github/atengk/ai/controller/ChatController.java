package io.github.atengk.ai.controller;

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * 文本对话接口
 *
 * @author Ateng
 * @since 2026-04-24
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class ChatController {

    private final ChatModel chatModel;

    /**
     * 普通文本生成接口
     *
     * @param message 用户输入内容
     * @return 模型生成结果
     */
    @GetMapping("/ai/generate")
    public Map<String, String> generate(@RequestParam(value = "message", required = false) String message) {
        String userMessage = StrUtil.blankToDefault(message, "请用一句话介绍 Spring AI Alibaba");
        log.info("收到普通聊天请求，message={}", userMessage);

        String content = chatModel.call(userMessage);
        return Map.of("generation", content);
    }

    /**
     * 流式文本生成接口
     *
     * @param message 用户输入内容
     * @return 流式模型响应
     */
    @GetMapping(value = "/ai/generateStream", produces = "text/event-stream;charset=UTF-8")
    public Flux<String> generateStream(@RequestParam(value = "message", required = false) String message) {
        String userMessage = StrUtil.blankToDefault(message, "请列出 Spring AI Alibaba 的三个核心能力");
        log.info("收到流式聊天请求，message={}", userMessage);

        Prompt prompt = new Prompt(new UserMessage(userMessage));
        return chatModel.stream(prompt)
                .map(ChatResponse::getResult)
                .map(result -> result.getOutput().getText())
                .filter(StrUtil::isNotBlank);
    }

}