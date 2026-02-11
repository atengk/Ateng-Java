package io.github.atengk.ai.controller;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
public class ReactAgentController {

    private final ReactAgent reactAgent;
    private final ReactAgent toolReactAgent;

    @GetMapping("/simple")
    public String simple(String message) throws GraphRunnerException {
        AssistantMessage call = reactAgent.call(message);
        return call.getText();
    }

    @GetMapping("/tool")
    public String tool(String message) throws GraphRunnerException {
        AssistantMessage call = toolReactAgent.call(message);
        return call.getText();
    }

}
