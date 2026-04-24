package io.github.atengk.ai.config;

import cn.hutool.core.util.StrUtil;
import com.alibaba.cloud.ai.graph.RunnableConfig;

/**
 * Agent 执行配置工厂
 *
 * @author Ateng
 * @since 2026-04-24
 */
public class AgentRunnableConfigFactory {

    /**
     * 创建 Agent 执行配置
     *
     * @param threadId 会话 ID
     * @param userId   用户 ID
     * @param tenantId 租户 ID
     * @return Agent 执行配置
     */
    public static RunnableConfig create(String threadId, String userId, String tenantId) {
        String conversationId = StrUtil.blankToDefault(threadId, "default-thread");

        return RunnableConfig.builder()
                .threadId(conversationId)
                .addMetadata("user_id", userId)
                .addMetadata("tenant_id", tenantId)
                .addMetadata("source", "spring-ai-alibaba-demo")
                .build();
    }

}