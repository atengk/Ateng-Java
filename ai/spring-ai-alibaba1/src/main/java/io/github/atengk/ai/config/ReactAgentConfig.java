package io.github.atengk.ai.config;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class ReactAgentConfig {

    @Bean
    public ReactAgent reactAgent(DashScopeChatModel dashScopeChatModel) {
        return ReactAgent.builder()
                .name("default_agent")
                .model(dashScopeChatModel)
                .saver(new MemorySaver())
                .build();
    }

    @Bean
    public ReactAgent toolReactAgent(
            DashScopeChatModel dashScopeChatModel,
            List<ToolCallback> toolCallbacks
    ) {
        return ReactAgent.builder()
                .name("tool_agent")
                .tools(toolCallbacks)
                .model(dashScopeChatModel)
                .saver(new MemorySaver())
                .build();
    }

}
