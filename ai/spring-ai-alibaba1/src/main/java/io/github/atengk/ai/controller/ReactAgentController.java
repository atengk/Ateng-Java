package io.github.atengk.ai.controller;

import cn.hutool.core.util.StrUtil;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * ReactAgent 对话接口
 *
 * @author Ateng
 * @since 2026-04-24
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class ReactAgentController {

    private final ReactAgent reactAgent;

    /**
     * 调用 ReactAgent 生成回答
     *
     * @param message  用户问题
     * @param threadId 会话 ID
     * @return Agent 响应内容
     */
    @GetMapping("/agent/react/chat")
    public Map<String, Object> chat(@RequestParam(required = false) String message,
                                    @RequestParam(required = false) String threadId) throws GraphRunnerException {
        String userMessage = StrUtil.blankToDefault(message, "请介绍一下 Spring AI Alibaba ReactAgent");
        String conversationId = StrUtil.blankToDefault(threadId, "default-thread");

        log.info("收到 ReactAgent 请求，threadId={}，message={}", conversationId, userMessage);

        RunnableConfig config = RunnableConfig.builder()
                .threadId(conversationId)
                .build();

        AssistantMessage response = reactAgent.call(userMessage, config);

        return Map.of(
                "agent", "dashscope_react_agent",
                "threadId", conversationId,
                "content", response.getText()
        );
    }

}