package io.github.atengk.ai.controller;

import cn.hutool.core.util.StrUtil;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Flux;

/**
 * ReactAgent 流式消息接口
 *
 * @author Ateng
 * @since 2026-04-24
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class ReactAgentStreamController {

    private final ReactAgent reactAgent;

    /**
     * 通过 SSE 流式返回 Agent 消息
     *
     * @param message 用户问题
     * @param threadId 会话 ID
     * @return 流式文本
     */
    @GetMapping(value = "/agent/react/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> stream(@RequestParam(required = false) String message,
                               @RequestParam(required = false) String threadId) throws GraphRunnerException {
        String userMessage = StrUtil.blankToDefault(message, "请介绍一下 ReactAgent 的执行流程");
        String conversationId = StrUtil.blankToDefault(threadId, "default-thread");

        log.info("收到 ReactAgent 流式请求，threadId={}，message={}", conversationId, userMessage);

        RunnableConfig config = RunnableConfig.builder()
                .threadId(conversationId)
                .build();

        return reactAgent.streamMessages(userMessage, config)
                .map(item -> String.valueOf(item))
                .filter(StrUtil::isNotBlank);
    }

}
