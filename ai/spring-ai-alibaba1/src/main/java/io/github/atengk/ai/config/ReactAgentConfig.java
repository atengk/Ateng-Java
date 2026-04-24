package io.github.atengk.ai.config;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.hook.modelcalllimit.ModelCallLimitHook;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import io.github.atengk.ai.model.AnalysisResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ReactAgent 配置
 *
 * @author Ateng
 * @since 2026-04-24
 */
@Slf4j
@Configuration
public class ReactAgentConfig {

    @Bean
    public ReactAgent reactAgent(ChatModel chatModel) {
        log.info("初始化 ReactAgent");

        return ReactAgent.builder()
                .name("dashscope_react_agent")
                .model(chatModel)
                .description("基于 DashScope 的 ReAct 智能体")
                .instruction("""
                        你是一个专业的 Java 技术助手。
                        需要先理解用户意图，再根据任务复杂度决定是否调用工具。
                        如果工具结果不足以支撑结论，需要明确说明不确定性。
                        """)
                .hooks(ModelCallLimitHook.builder().runLimit(5).build())
                .saver(new MemorySaver())
                .build();
    }

    @Bean
    public ReactAgent structuredReactAgent(ChatModel chatModel) {
        return ReactAgent.builder()
                .name("structured_react_agent")
                .model(chatModel)
                .description("返回结构化分析结果的 Agent")
                .instruction("你需要分析用户问题，并按照指定结构返回结果。")
                .outputType(AnalysisResult.class)
                .saver(new MemorySaver())
                .build();
    }

}