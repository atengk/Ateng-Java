# Spring AI Alibaba 1.1.2.2 使用文档

本文档只整理 Spring AI Alibaba 特有能力，包括 Agent Framework、Graph、Skills、Multi-agent、A2A、Studio、Admin、Sandbox、DashScope 扩展、Nacos 集成等；不展开 Spring AI 原生通用 API 的基础教程。Spring AI Alibaba 1.1.2.2 已在 GitHub Release 中发布，1.1.2.1 被标注为存在若干 bug 并建议使用 1.1.2.2；Maven Central 也已收录 `spring-ai-alibaba-bom` 的 1.1.2.2 版本。官方文档部分页面仍以 1.1.2.0 为示例版本，因此本文按你指定的 `<spring-ai-alibaba.version>1.1.2.2</spring-ai-alibaba.version>` 统一书写。([GitHub][1])

## 1. 版本与依赖管理

本章用于统一项目版本、模块选择和升级边界。Spring AI Alibaba 1.1.x 系列基于 Spring AI 1.1.x，官方版本说明中 1.1.2.0 对应 Spring AI 1.1.2 与 Spring Boot 3.5.x；1.1.2.2 是后续修复版本，可在项目中继续沿用 1.1.2.x 组合。([Spring AI Alibaba][2])

推荐在父工程中统一声明版本属性，然后使用 BOM 管理 Spring AI Alibaba 相关依赖，避免各模块版本不一致。

下面配置统一声明 Spring AI Alibaba 1.1.2.2，并引入常用模块。

```xml
<properties>
    <java.version>17</java.version>
    <spring-ai.version>1.1.2</spring-ai.version>
    <spring-ai-alibaba.version>1.1.2.2</spring-ai-alibaba.version>
    <hutool.version>5.8.36</hutool.version>
</properties>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.alibaba.cloud.ai</groupId>
            <artifactId>spring-ai-alibaba-bom</artifactId>
            <version>${spring-ai-alibaba.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>

        <dependency>
            <groupId>com.alibaba.cloud.ai</groupId>
            <artifactId>spring-ai-alibaba-extensions-bom</artifactId>
            <version>${spring-ai-alibaba.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <!-- Spring AI Alibaba Agent Framework -->
    <dependency>
        <groupId>com.alibaba.cloud.ai</groupId>
        <artifactId>spring-ai-alibaba-agent-framework</artifactId>
    </dependency>

    <!-- DashScope / 通义模型接入 -->
    <dependency>
        <groupId>com.alibaba.cloud.ai</groupId>
        <artifactId>spring-ai-alibaba-starter-dashscope</artifactId>
    </dependency>

    <!-- Graph Core -->
    <dependency>
        <groupId>com.alibaba.cloud.ai</groupId>
        <artifactId>spring-ai-alibaba-graph-core</artifactId>
    </dependency>

    <!-- Agent Chat UI -->
    <dependency>
        <groupId>com.alibaba.cloud.ai</groupId>
        <artifactId>spring-ai-alibaba-studio</artifactId>
    </dependency>

    <!-- Hutool，示例代码中用于字符串、集合、JSON 等处理 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>${hutool.version}</version>
    </dependency>

    <!-- 示例代码中用于日志 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

常用模块选择如下。

| 能力                                  | 推荐模块                                          |
| ----------------------------------- | --------------------------------------------- |
| ReactAgent、Hooks、Skills、Multi-agent | `spring-ai-alibaba-agent-framework`           |
| StateGraph、状态、边、持久化、流式执行            | `spring-ai-alibaba-graph-core`                |
| 通义千问 / DashScope 模型、语音、多模态          | `spring-ai-alibaba-starter-dashscope`         |
| Agent Chat UI                       | `spring-ai-alibaba-studio`                    |
| Sandbox 安全工具执行                      | `spring-ai-alibaba-sandbox`                   |
| A2A + Nacos 注册发现                    | `spring-ai-alibaba-starter-a2a-nacos`         |
| Nacos 动态配置与模型热更新                    | `spring-ai-alibaba-starter-config-nacos`      |
| Graph 可观测性                          | `spring-ai-alibaba-starter-graph-observation` |
| Graph 内置节点                          | `spring-ai-alibaba-starter-builtin-nodes`     |
| AgentScope 集成                       | `spring-ai-alibaba-starter-agentscope`        |

1.1.2.0 Release Notes 中新增了 Agent Skills、多智能体并行、Graph 并行条件边、AllOf / AnyOf 聚合、异步工具执行、工具 `returnDirect`、Sandbox、Studio CORS 可选等能力；1.1.2.2 在此基础上作为后续修复版本使用。([GitHub][1])

## 2. DashScope / 通义模型生态接入

本章用于接入阿里云 DashScope 模型能力。Spring AI Alibaba 的 DashScope 扩展提供 ChatModel、推理内容、视觉多模态、音频能力、TTS 等能力，其中 `spring.ai.dashscope` 是连接 DashScope 的主配置前缀，`spring.ai.dashscope.chat.options.*` 用于配置 DashScope ChatModel 的模型参数。([Spring AI Alibaba][3])

基础配置如下。

```yaml
spring:
  ai:
    model:
      chat: dashscope
    dashscope:
      api-key: ${AI_DASHSCOPE_API_KEY}
      chat:
        options:
          model: qwen-plus
          temperature: 0.7
          topP: 0.8
          maxTokens: 2048
```

在代码中可以手动构建 DashScope API 和 ChatModel，然后交给 ReactAgent 使用。官方 Agent 文档中也采用 `DashScopeApi.builder().apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))` 的方式创建模型。([Spring AI Alibaba][4])

下面代码创建 DashScopeChatModel，并构建一个最小 ReactAgent。

```java
package com.ateng.ai.config;

import cn.hutool.core.util.StrUtil;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring AI Alibaba DashScope Agent 配置
 *
 * @author Ateng
 * @since 2026-04-24
 */
@Slf4j
@Configuration
public class DashScopeAgentConfig {

    @Bean
    public ChatModel dashScopeChatModel() {
        String apiKey = System.getenv("AI_DASHSCOPE_API_KEY");
        if (StrUtil.isBlank(apiKey)) {
            log.warn("未检测到 AI_DASHSCOPE_API_KEY 环境变量，请检查 DashScope API Key 配置");
        }

        DashScopeApi dashScopeApi = DashScopeApi.builder()
                .apiKey(apiKey)
                .build();

        log.info("初始化 DashScope ChatModel");
        return DashScopeChatModel.builder()
                .dashScopeApi(dashScopeApi)
                .build();
    }

    @Bean
    public ReactAgent assistantAgent(ChatModel dashScopeChatModel) {
        log.info("创建 Spring AI Alibaba ReactAgent，agentName=assistant_agent");
        return ReactAgent.builder()
                .name("assistant_agent")
                .description("通用问答智能体")
                .model(dashScopeChatModel)
                .instruction("你是一个专业、简洁的 Java AI 应用助手。")
                .outputKey("assistant_output")
                .build();
    }
}
```

DashScope 推理模型可通过 `enableThinking` 和 `thinkingBudget` 打开推理内容，Spring AI Alibaba 会把响应中的推理内容映射到 `AssistantMessage` metadata 的 `reasoningContent` 字段；流式响应中也可以按 chunk 累积该字段。该能力依赖模型本身是否支持 thinking。([Spring AI Alibaba][5])

下面代码读取 Qwen3 推理模型的 reasoning content。

```java
package com.ateng.ai.service;

import cn.hutool.core.util.StrUtil;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

/**
 * DashScope 推理内容服务
 *
 * @author Ateng
 * @since 2026-04-24
 */
@Slf4j
@Service
public class DashScopeReasoningService {

    private final ChatModel chatModel;

    public DashScopeReasoningService(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    public String askWithReasoning(String question) {
        DashScopeChatOptions options = DashScopeChatOptions.builder()
                .model("qwen3")
                .enableThinking(true)
                .thinkingBudget(1000)
                .build();

        ChatResponse response = chatModel.call(new Prompt(question, options));
        AssistantMessage message = response.getResult().getOutput();

        Object reasoningContent = message.getMetadata().get("reasoningContent");
        if (reasoningContent != null && StrUtil.isNotBlank(reasoningContent.toString())) {
            log.info("模型推理内容：{}", reasoningContent);
        }

        return message.getText();
    }
}
```

DashScope 多模态能力主要包括 Vision、Video、音频输入输出等。DashScope 文档中列出 `qwen-vl-plus`、`qwen-vl-max` 等视觉模型，并支持在用户消息中传入图片 URL 或 Resource；DashScope TTS 支持 `cosyvoice-v1`、`cosyvoice-v2`、`cosyvoice-v3`、`cosyvoice-v3-plus` 等模型，并支持 WebSocket 实时音频流。([Spring AI Alibaba][5])

## 3. Agent Framework

本章用于构建 Spring AI Alibaba 的核心智能体。ReactAgent 是 Spring AI Alibaba 的生产级 Agent 实现，运行在 Graph Runtime 之上，由模型节点、工具节点、Hook 节点组合形成 ReAct 循环。([Spring AI Alibaba][4])

ReactAgent 常用配置项包括：

| 配置项                           | 说明                                 |
| ----------------------------- | ---------------------------------- |
| `name`                        | Agent 唯一名称                         |
| `description`                 | Agent 描述，用于多 Agent 场景中的选择和说明       |
| `model`                       | Agent 使用的 ChatModel                |
| `instruction`                 | 任务指令，可结合状态占位符                      |
| `systemPrompt`                | 系统提示                               |
| `tools` / `methodTools`       | 工具注册                               |
| `hooks`                       | Agent / Model / Message Hook       |
| `interceptors`                | ModelInterceptor / ToolInterceptor |
| `saver`                       | 状态持久化                              |
| `outputKey`                   | Agent 输出写入状态的 key                  |
| `outputType` / `outputSchema` | 结构化输出配置                            |

调用方式通常分为三类：`call` 获取最终 `AssistantMessage`，`invoke` 获取完整 `OverAllState`，`stream` 或 `streamMessages` 获取流式执行输出。1.1.2.0 Release Notes 中提到新增 `streamMessages` API，并为 `invoke` / `call` 增加额外输入重载。([GitHub][1])

下面代码封装一个业务侧 Agent 调用服务。

```java
package com.ateng.ai.service;

import cn.hutool.core.map.MapUtil;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

/**
 * ReactAgent 调用服务
 *
 * @author Ateng
 * @since 2026-04-24
 */
@Slf4j
@Service
public class ReactAgentService {

    private final ReactAgent assistantAgent;

    public ReactAgentService(ReactAgent assistantAgent) {
        this.assistantAgent = assistantAgent;
    }

    public String call(String userId, String question) {
        RunnableConfig config = RunnableConfig.builder()
                .threadId(userId)
                .addMetadata("userId", userId)
                .build();

        log.info("开始调用 ReactAgent，userId={}", userId);
        AssistantMessage message = assistantAgent.call(question, config);
        return message.getText();
    }

    public Map<String, Object> invoke(String userId, String question) {
        RunnableConfig config = RunnableConfig.builder()
                .threadId(userId)
                .addMetadata("userId", userId)
                .build();

        Optional<OverAllState> result = assistantAgent.invoke(question, config);
        if (result.isEmpty()) {
            log.warn("ReactAgent 未返回状态，userId={}", userId);
            return MapUtil.empty();
        }

        log.info("ReactAgent 执行完成，userId={}", userId);
        return result.get().data();
    }
}
```

结构化输出适合提取、分类、评分、审批结果等场景。Spring AI Alibaba 支持通过 `outputType` 或 `outputSchema` 控制 Agent 最终输出格式，官方文档中建议在 JSON 不稳定时配合 try-catch、验证和重试策略。([Spring AI Alibaba][6])

## 4. Tools 增强能力

本章用于说明 Spring AI Alibaba 在工具调用方面的增强。工具是 Agent 与外部系统交互的入口，Spring AI Alibaba 支持直接工具、方法工具、ToolCallbackProvider、工具名称解析等注册方式，并进一步提供 ToolContext、ToolInterceptor、并行工具执行、异步工具执行和 `returnDirect` 等能力。([Spring AI Alibaba][7])

Spring AI Alibaba Tools 使用时优先关注以下能力：

| 能力                  | 使用场景                    |
| ------------------- | ----------------------- |
| `ToolContext`       | 工具中访问状态、运行配置、上下文、长期存储   |
| `ToolContextHelper` | 便捷读取 ToolContext 中的元数据  |
| `ToolInterceptor`   | 统一做权限、审计、异常转换、耗时统计      |
| 异步工具执行              | 工具调用耗时长，避免阻塞 Agent 主流程  |
| 并行工具执行              | 多工具可独立执行时降低总耗时          |
| `returnDirect`      | 工具结果直接返回给客户端，不再进入模型二次总结 |

`ToolContext` 可以让工具访问状态、上下文、Store、RunnableConfig 和 Tool Call ID；官方 Tools 文档中说明工具只需在签名中加入 `ToolContext` 参数即可注入运行时信息。([Spring AI Alibaba][7])

下面代码定义一个带上下文读取和日志审计的业务工具。

```java
package com.ateng.ai.tool;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 订单查询工具
 *
 * @author Ateng
 * @since 2026-04-24
 */
@Slf4j
@Component
public class OrderQueryTools {

    @Tool(description = "根据订单号查询订单摘要信息")
    public String queryOrder(
            @ToolParam(description = "订单号") String orderNo,
            ToolContext toolContext) {

        if (StrUtil.isBlank(orderNo)) {
            log.warn("订单查询失败，订单号为空");
            return "订单号不能为空";
        }

        Map<String, Object> context = toolContext.getContext();
        Object userId = context.get("userId");
        log.info("执行订单查询工具，userId={}，orderNo={}", userId, orderNo);

        return "订单号：" + orderNo + "，状态：已支付，物流：运输中";
    }
}
```

下面代码将方法工具注册到 ReactAgent。

```java
package com.ateng.ai.config;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.ateng.ai.tool.OrderQueryTools;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 工具型 Agent 配置
 *
 * @author Ateng
 * @since 2026-04-24
 */
@Slf4j
@Configuration
public class ToolAgentConfig {

    @Bean
    public ReactAgent orderAgent(ChatModel chatModel, OrderQueryTools orderQueryTools) {
        log.info("创建订单查询 Agent，agentName=order_agent");
        return ReactAgent.builder()
                .name("order_agent")
                .description("可查询订单状态的智能体")
                .model(chatModel)
                .instruction("你负责根据用户输入调用订单工具，并返回简洁的订单状态。")
                .methodTools(orderQueryTools)
                .outputKey("order_agent_output")
                .build();
    }
}
```

工具拦截器适合统一异常处理、权限校验、审计记录、结果脱敏。1.1.2.0 Release Notes 明确新增异步/并行工具执行、工具 `returnDirect` 和 `ToolContextHelper`。([GitHub][1])

## 5. Hooks 与 Interceptors

本章用于控制 Agent 生命周期。Hooks 与 Interceptors 允许在 Agent 调用模型、选择工具、执行工具、返回结果前后插入逻辑，可用于监控、修改、控制和强制执行策略。官方文档列出的典型用途包括日志分析、转换提示、工具选择、输出格式、重试、回退、提前终止、速率限制、护栏和 PII 检测。([Spring AI Alibaba][8])

Spring AI Alibaba 中常见扩展点如下。

| 扩展点                 | 触发位置            | 典型用途                          |
| ------------------- | --------------- | ----------------------------- |
| `AgentHook`         | Agent 开始 / 结束   | 审计、初始化上下文、统计总耗时               |
| `ModelHook`         | 模型调用前 / 后       | 追加上下文、裁剪消息、记录响应               |
| `MessagesModelHook` | 消息级处理           | 消息压缩、摘要、删除、替换                 |
| `ModelInterceptor`  | 包裹模型调用          | 动态 System Prompt、模型路由、护栏      |
| `ToolInterceptor`   | 包裹工具调用          | 工具权限、重试、异常转换                  |
| Flow Agent Hooks    | Flow Agent 执行过程 | Routing、Supervisor、Loop 等统一拦截 |

官方内置了消息压缩 SummarizationHook，可在接近 token 限制时压缩对话历史，并提供 `model`、`maxTokensBeforeSummary`、`messagesToKeep` 等参数。([Spring AI Alibaba][8])

下面代码通过 ModelInterceptor 注入动态 System Prompt。

```java
package com.ateng.ai.interceptor;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelCallHandler;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.SystemMessage;

/**
 * 动态系统提示拦截器
 *
 * @author Ateng
 * @since 2026-04-24
 */
@Slf4j
public class DynamicSystemPromptInterceptor extends ModelInterceptor {

    @Override
    public ModelResponse interceptModel(ModelRequest request, ModelCallHandler handler) {
        String role = Convert.toStr(request.getContext().getOrDefault("userRole", "default"));

        String dynamicPrompt = switch (role) {
            case "developer" -> "你正在服务 Java 开发工程师，回答应包含工程化实践和关键代码。";
            case "ops" -> "你正在服务运维人员，回答应强调部署、监控和故障排查。";
            default -> "你正在服务普通业务用户，回答应简洁直接。";
        };

        SystemMessage systemMessage = request.getSystemMessage();
        String mergedPrompt = systemMessage == null
                ? dynamicPrompt
                : StrUtil.format("{}\n{}", systemMessage.getText(), dynamicPrompt);

        log.info("动态注入系统提示，role={}", role);
        ModelRequest enhancedRequest = ModelRequest.builder(request)
                .systemMessage(new SystemMessage(mergedPrompt))
                .build();

        return handler.call(enhancedRequest);
    }

    @Override
    public String getName() {
        return "dynamic_system_prompt_interceptor";
    }
}
```

## 6. Memory 与上下文工程

本章用于管理短期记忆、长期记忆和模型上下文。Spring AI Alibaba 将短期记忆作为 Agent 状态的一部分管理，默认通过 `messages` 键保存对话历史，并可结合 Checkpointer 持久化不同 thread 的状态。([Spring AI Alibaba][9])

短期记忆的核心实践是使用 `threadId` 隔离不同会话。`MemorySaver` 适合本地开发和测试，生产环境应使用 Redis、MongoDB、PostgreSQL 等持久化 Checkpointer。

```java
RunnableConfig config = RunnableConfig.builder()
        .threadId("user-10001")
        .addMetadata("userId", "10001")
        .build();

agent.call("我叫 Ateng", config);
agent.call("我叫什么名字？", config);
```

上下文工程关注每次模型调用看到什么内容，包括 System Prompt、消息、工具、模型和响应格式。官方上下文工程文档强调，Agent 失败通常不是单纯因为模型能力不足，而是没有向模型提供正确上下文；Spring AI Alibaba 通过 Agent 抽象、状态、工具上下文和生命周期 Hook 支持上下文工程。([Spring AI Alibaba][10])

常见上下文工程策略如下。

| 策略    | 说明                                      |
| ----- | --------------------------------------- |
| 消息裁剪  | 保留首条系统消息和最近 N 条消息                       |
| 消息摘要  | 将历史长对话压缩为摘要                             |
| 动态提示  | 根据用户角色、任务阶段、状态动态注入 System Prompt        |
| 工具过滤  | 根据权限或阶段暴露不同工具                           |
| 输出约束  | 使用 `outputType` / `outputSchema` 固定输出格式 |
| 上下文隔离 | 使用 `threadId` 和 metadata 隔离用户、租户、流程实例   |

## 7. Agent Skills

本章用于构建可复用技能。Skills 是 Spring AI Alibaba 1.1.2.x 中非常重要的能力，它将可复用指令、上下文和资源打包为技能目录；Agent 先只看到技能列表，在需要时通过 `read_skill(skill_name)` 读取完整 `SKILL.md`，这就是渐进式披露。官方 Skills 文档说明，`SkillsAgentHook` 会注册 `read_skill` 工具并将技能列表注入系统提示。([Spring AI Alibaba][11])

推荐技能目录结构如下。

```text
skills/
└── sql-review/
    ├── SKILL.md
    ├── references/
    │   └── sql-style.md
    ├── examples/
    │   └── order-query.sql
    └── scripts/
        └── analyze_sql.py
```

`SKILL.md` 应声明技能名和描述。

```markdown
---
name: sql-review
description: Review SQL statements for performance, safety, and maintainability.
---

# SQL Review

该技能用于审查 SQL 性能、安全性和可维护性。

## 使用方式

当用户请求 SQL 审查、慢 SQL 优化、索引建议或查询安全检查时，先读取本技能内容，再结合 references/sql-style.md 进行判断。
```

下面代码通过 FileSystemSkillRegistry 加载本地技能目录，并将 SkillsAgentHook 挂载到 ReactAgent。官方文档说明，用户级目录默认是 `~/saa/skills`，项目级默认是 `./skills`，同名技能项目级覆盖用户级。([Spring AI Alibaba][11])

```java
package com.ateng.ai.config;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.hook.skills.SkillsAgentHook;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.skills.registry.SkillRegistry;
import com.alibaba.cloud.ai.graph.skills.registry.filesystem.FileSystemSkillRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Agent Skills 配置
 *
 * @author Ateng
 * @since 2026-04-24
 */
@Slf4j
@Configuration
public class SkillsAgentConfig {

    @Bean
    public ReactAgent skillsAgent(ChatModel chatModel) {
        SkillRegistry registry = FileSystemSkillRegistry.builder()
                .projectSkillsDirectory(System.getProperty("user.dir") + "/skills")
                .build();

        SkillsAgentHook skillsHook = SkillsAgentHook.builder()
                .skillRegistry(registry)
                .autoReload(true)
                .build();

        log.info("创建 Skills Agent，skillsDir={}", System.getProperty("user.dir") + "/skills");
        return ReactAgent.builder()
                .name("skills_agent")
                .description("支持按需读取技能的智能体")
                .model(chatModel)
                .saver(new MemorySaver())
                .hooks(List.of(skillsHook))
                .enableLogging(true)
                .build();
    }
}
```

Skills 还支持 `ClasspathSkillRegistry`，适合将技能放入 `src/main/resources/skills` 并随 JAR 打包。高级用法包括 `groupedTools`，即工具跟随技能渐进式披露：只有当模型读取某个技能后，该技能绑定的工具才会暴露给模型。([Spring AI Alibaba][11])

在 Graph 或 ChatClient 链路中，可使用 `SkillPromptAugmentAdvisor` 注入技能列表；但它只负责注入技能元数据，不自动注册 `read_skill` 工具，若要读取完整技能仍需额外注册工具或使用带 `SkillsAgentHook` 的 ReactAgent 节点。([Spring AI Alibaba][11])

## 8. Multi-agent Patterns

本章用于将复杂任务拆解给多个专业 Agent。官方 Multi-agent 文档指出，多智能体适合单个 Agent 工具过多、上下文过大或任务需要专业分工的场景；Spring AI Alibaba 支持 Tool Calling 和 Handoffs 两类核心模式，并提供 SequentialAgent、ParallelAgent、LlmRoutingAgent、SupervisorAgent 等实现。([Spring AI Alibaba][12])

常见模式如下。

| 模式                | 说明                         | 适用场景         |
| ----------------- | -------------------------- | ------------ |
| `SequentialAgent` | 多个 Agent 按顺序执行             | 写作 → 审查 → 翻译 |
| `ParallelAgent`   | 多个 Agent 并行处理同一输入          | 多角度分析、并行研究   |
| `LlmRoutingAgent` | LLM 判断路由到哪个专家 Agent        | 意图分类、客服分流    |
| `SupervisorAgent` | 监督者循环选择子 Agent，直到 FINISH   | 多步骤复杂任务      |
| Subagent          | 主 Agent 将任务委托给子 Agent      | 代码检索、网页研究    |
| Agent Tool        | 将 Agent 包装成工具给另一个 Agent 调用 | 中心化编排        |
| Handoffs          | 当前 Agent 将控制权交给另一个 Agent   | 跨领域对话接管      |
| Loop Agent        | 循环执行直到满足条件                 | 反复修订、验证      |
| Custom FlowAgent  | 自定义流程型 Agent               | 特殊业务编排       |

1.1.2.2 Release Notes 中新增或强化了 AgentScope Integration、Subagent、Supervisor、Skills、Routing、Handoffs、Workflow、Voice Agent、Multimodal 等示例和模式说明。([GitHub][1])

SequentialAgent 适合固定流水线，ParallelAgent 适合互不依赖的子任务，LlmRoutingAgent 适合单次路由，SupervisorAgent 适合多步骤循环路由。官方文档明确指出 SupervisorAgent 与 LlmRoutingAgent 的区别在于：Supervisor 支持子 Agent 执行后返回监督者继续决策，适合复杂多步骤任务。([Spring AI Alibaba][12])

## 9. Workflow

本章用于说明 Spring AI Alibaba 的工作流能力。Graph 是 Agent Framework 的底层运行时，也是低级工作流与多智能体编排框架；Spring AI Alibaba 会将 Agent 编排为由节点串联而成的 DAG 图。Graph 的三个核心概念是 State、Node、Edge。([Spring AI Alibaba][13])

Workflow 适合以下场景：

| 场景         | 说明                   |
| ---------- | -------------------- |
| 固定流程       | 每一步清晰、节点顺序稳定         |
| 条件路由       | 根据状态决定下一步            |
| 人工审批       | 在关键节点中断并等待审批         |
| 多 Agent 编排 | 将多个 ReactAgent 作为节点  |
| 混合流程       | 普通业务节点 + Agent 节点混合  |
| 可恢复流程      | 配合 Checkpointer 恢复执行 |

官方 Workflow 文档说明，Graph 可直接用于复杂应用编排，支持流式执行 `compiledGraph.stream()` 和同步执行 `compiledGraph.invoke()`。([Spring AI Alibaba][14])

下面是一个简化的 Graph 工作流结构。

```java
package com.ateng.ai.workflow;

import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;

import java.util.HashMap;
import java.util.Map;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

/**
 * 简单 Graph 工作流示例
 *
 * @author Ateng
 * @since 2026-04-24
 */
public class SimpleWorkflow {

    public StateGraph createWorkflow() {
        KeyStrategyFactory keyStrategyFactory = () -> {
            Map<String, KeyStrategy> strategies = new HashMap<>();
            strategies.put("input", new ReplaceStrategy());
            strategies.put("output", new ReplaceStrategy());
            return strategies;
        };

        return new StateGraph(keyStrategyFactory)
                .addNode("process", node_async(state -> {
                    Object input = state.value("input").orElse("");
                    return Map.of("output", "已处理：" + input);
                }))
                .addEdge(START, "process")
                .addEdge("process", END);
    }
}
```

## 10. RAG 增强

本章只说明 Spring AI Alibaba 在 Agent Framework 中组织 RAG 的方式，不展开 Spring AI 原生 RAG Advisor。官方 RAG 文档将 RAG 分为 Two-step RAG、Agentic RAG、Hybrid RAG。Two-step RAG 是先检索再生成；Agentic RAG 让 Agent 在推理过程中自行决定何时检索；Hybrid RAG 则加入查询改写、检索校验、答案验证等中间步骤。([Spring AI Alibaba][15])

推荐选型如下。

| 模式                 | 特点                 | 适用场景             |
| ------------------ | ------------------ | ---------------- |
| Two-step RAG       | 流程固定、可预测           | 企业知识库问答          |
| Agentic RAG        | Agent 自主决定是否检索     | 复杂研究、动态检索        |
| Hybrid RAG         | 查询改写、检索校验、答案验证     | 高准确率问答           |
| Graph RAG Workflow | 每个步骤显式建模为 Graph 节点 | 可观测、可恢复、可审批的 RAG |

Spring AI Alibaba 中可用 `MessagesModelHook` 在模型调用前检索并追加上下文，也可以把检索工具注册给 ReactAgent 实现 Agentic RAG。对生产系统，更推荐将查询改写、检索、验证、生成、答案校验拆成 Graph 节点，便于日志、监控、重试和人审。

## 11. Human-in-the-Loop

本章用于处理高风险操作审批。Spring AI Alibaba 的 Human-in-the-Loop Hook 可以在模型提出工具调用时暂停执行，等待人工决策；人工决策可为 `approve`、`edit` 或 `reject`。执行暂停后，图状态通过 Checkpointer 保存，因此可以安全恢复。([Spring AI Alibaba][16])

典型审批场景包括：

| 场景              | 审批原因       |
| --------------- | ---------- |
| 执行 SQL 写操作      | 防止误删、误改    |
| 发送邮件 / 消息       | 防止错误收件人或内容 |
| 资金、订单、退款        | 高风险业务操作    |
| 文件写入 / Shell 命令 | 防止破坏系统     |
| 外部 API 调用       | 防止不可逆操作    |

Graph Core 也支持两种人类反馈模式：节点主动返回 `InterruptionMetadata` 动态中断，或在编译配置中使用 `interruptBefore` 在指定节点前中断。([Spring AI Alibaba][17])

## 12. A2A 分布式智能体

本章用于跨服务调用 Agent。Spring AI Alibaba 的 A2A 实现包含 A2A Server、A2A Registry、A2A Discovery 三个核心组件，支持将本地 ReactAgent 暴露为 A2A 服务，并通过 Nacos 做注册与发现。([Spring AI Alibaba][18])

A2A 主要角色如下。

| 组件             | 说明                        |
| -------------- | ------------------------- |
| A2A Server     | 将本地 ReactAgent 暴露为远程服务    |
| A2A Registry   | 注册 AgentCard，支持 Nacos     |
| A2A Discovery  | 查询远程 AgentCard，支持 Nacos   |
| AgentCard      | 描述 Agent 名称、能力、提供者、端点等元数据 |
| A2aRemoteAgent | 将远程 Agent 包装为可调用对象        |

服务提供者需要配置 A2A Server 与 AgentCard。官方文档说明启动后会暴露 `/.well-known/agent.json` 和 `/a2a/message` 等端点，并可通过 Nacos Registry 自动注册。([Spring AI Alibaba][18])

```yaml
spring:
  ai:
    alibaba:
      a2a:
        nacos:
          server-addr: 127.0.0.1:8848
          username: nacos
          password: nacos
          registry:
            enabled: true
          discovery:
            enabled: true
        server:
          version: 1.0.0
          card:
            name: data_analysis_agent
            description: 专门用于数据分析和统计计算的智能体
            provider:
              name: demo-provider
              organization: demo-org
```

使用 A2A 时需要保证 `server.card.name` 与 ReactAgent Bean 的 `name` 一致；消费者侧通过 AgentCardProvider 发现远程 Agent，再用 `A2aRemoteAgent` 调用。

## 13. AgentScope 集成

本章用于说明 1.1.2.2 中的 AgentScope Integration。1.1.2.2 Release Notes 中新增 `spring-ai-alibaba-starter-agentscope`，并说明 `AgentScopeAgent` 可包装 AgentScope ReActAgent，使其作为 BaseAgent 用于 SAA Graph 工作流。([GitHub][1])

依赖如下。

```xml
<dependency>
    <groupId>com.alibaba.cloud.ai</groupId>
    <artifactId>spring-ai-alibaba-starter-agentscope</artifactId>
</dependency>
```

适用场景包括：

| 场景                  | 说明                                     |
| ------------------- | -------------------------------------- |
| 复用 AgentScope Agent | 将已有 AgentScope ReActAgent 纳入 SAA Graph |
| Graph 编排            | 把 AgentScopeAgent 作为节点                 |
| 多 Agent 混合          | SAA ReactAgent 与 AgentScopeAgent 混合编排  |
| 渐进迁移                | 从单独 AgentScope 应用迁移到 Graph 工作流         |

## 14. Graph Core

本章用于说明 Spring AI Alibaba Graph 的底层能力。Graph Core 的核心对象包括 `StateGraph`、`OverAllState`、`NodeAction`、`EdgeAction`、`KeyStrategy`、`CompiledGraph`、`RunnableConfig`。Graph 用 State 传递上下文，用 Node 执行业务逻辑或模型逻辑，用 Edge 决定控制流。([Spring AI Alibaba][13])

常用能力如下。

| 能力                 | 说明                          |
| ------------------ | --------------------------- |
| StateGraph         | 定义节点和边                      |
| KeyStrategy        | 定义状态 key 的合并策略              |
| ReplaceStrategy    | 后值替换前值                      |
| AppendStrategy     | 将新值追加到历史                    |
| 条件边                | 根据状态动态选择下一节点                |
| 并行分支               | 多个节点并行执行                    |
| 子图                 | 将一个 Graph 作为节点复用            |
| 执行取消               | 对长流程执行优雅取消或立即取消             |
| 流式输出               | 节点返回 Flux 或 StreamingOutput |
| Mermaid / PlantUML | 用于流程可视化，具体能力以版本 API 为准      |

1.1.2.0 Release Notes 对 Graph 增强包括：并行条件边、AllOf / AnyOf 聚合策略、批量 `addEdge`、`interruptAfter`、AgentToolNode 异步工具执行和流式节点完整输出。([GitHub][1])

并行节点需要为并行节点配置 Executor；官方文档说明，若未指定 Executor，并行节点会顺序调度，若要并发运行需要依赖 CompletableFuture 异步能力或配置并行节点 Executor。([Spring AI Alibaba][19])

## 15. Graph 持久化

本章用于说明 Graph 状态保存、恢复、历史查询和时间旅行。Spring AI Alibaba Graph 内置持久化层，通过 Checkpointer 在每个 super-step 保存图状态检查点；这些检查点属于一个 thread，因此必须在 `RunnableConfig` 中指定 `threadId`。([Spring AI Alibaba][20])

持久化相关概念如下。

| 概念                | 说明                                         |
| ----------------- | ------------------------------------------ |
| Thread            | 一次会话或流程实例                                  |
| Checkpoint        | 某个时间点的状态快照                                 |
| StateSnapshot     | 检查点对象，包含 config、metadata、values、next、tasks |
| `getState`        | 获取最新或指定 checkpoint 的状态                     |
| `getStateHistory` | 获取 thread 的历史状态                            |
| Time Travel       | 回看或恢复历史状态                                  |
| Fault Tolerance   | 失败后基于 checkpoint 恢复                        |

Checkpointer 实现包括：

| 实现                | 适用场景          |
| ----------------- | ------------- |
| `MemorySaver`     | 本地开发、单机测试     |
| `RedisSaver`      | 高性能短期状态持久化    |
| `PostgreSqlSaver` | 强一致、可审计、关系型存储 |
| `MongoDbSaver`    | 文档型状态存储       |

PostgreSQL Checkpointer 会自动创建 `checkpoints` 和 `checkpoint_metadata` 表，用于存储工作流状态和元数据。([Spring AI Alibaba][21])

## 16. Graph Streaming

本章用于处理流式输出。Graph 支持节点流式输出、LLM 流式输出和并行节点流式输出。官方文档说明，并行流式输出允许并行分支中的每个节点独立产生 `Flux<T>` 流式数据，并保留各自的节点 ID，便于前端区分来源。([Spring AI Alibaba][22])

常见流式事件处理策略如下。

| 输出类型             | 处理方式       |
| ---------------- | ---------- |
| 模型增量文本           | 直接推送给前端    |
| reasoningContent | 单独展示为“思考中” |
| 工具调用结果           | 展示工具执行状态   |
| 节点完成事件           | 更新流程图节点状态  |
| `_FINISHED` 输出   | 标记节点完整输出   |

1.1.2.0 Release Notes 中提到流式节点现在会发出完整输出，包括 `_FINISHED` OutputType。([GitHub][1])

## 17. Graph MCP 能力

本章用于把 MCP 工具分配给指定 Graph 节点。Spring AI Alibaba Graph 示例中提供了 MCP 节点相关用法，可将指定 MCP 分配给指定 node 节点。([Spring AI Alibaba][23])

常见使用方式如下。

| 能力            | 说明                 |
| ------------- | ------------------ |
| MCP 工具分配      | 指定某个节点可用的 MCP 工具   |
| AgentToolNode | 让 Agent 节点具备工具调用能力 |
| 异步工具调用        | 工具执行不阻塞主线程         |
| MCP + Graph   | 适合构建可观测、可恢复的外部工具链  |

生产建议：不要给所有节点挂载所有 MCP 工具，应根据节点职责最小化授权。例如“文档检索节点”只挂文档搜索 MCP，“代码分析节点”只挂仓库读取 MCP，“发布节点”必须增加人工审批。

## 18. Built-in Nodes

本章用于说明内置节点的使用边界。官方版本说明中列出 `spring-ai-alibaba-starter-builtin-nodes` 作为预置图节点模块，包含 LlmNode、AgentNode 等能力。([Spring AI Alibaba][2])

常见节点职责如下。

| 节点              | 说明                  |
| --------------- | ------------------- |
| `LlmNode`       | 单次模型调用节点            |
| `AgentNode`     | 将 Agent 作为 Graph 节点 |
| `ToolNode`      | 工具执行节点              |
| `AgentToolNode` | Agent 工具调用节点        |
| 自定义 Node        | 实现业务逻辑、校验、转换、存储     |

选择建议：确定性业务逻辑用自定义 Node；需要模型推理时用 LlmNode；需要完整 ReAct 循环时用 AgentNode；需要执行工具时用 ToolNode 或 AgentToolNode。

## 19. Graph 可观测性

本章用于对 Graph 执行链路做监控。官方版本说明中列出 `spring-ai-alibaba-starter-graph-observation`，用于 Graph 可观测性并集成 Micrometer / OpenTelemetry。([Spring AI Alibaba][2])

可观测性应覆盖以下内容。

| 维度       | 指标                           |
| -------- | ---------------------------- |
| Graph 级别 | graphName、threadId、总耗时、成功/失败 |
| 节点级别     | nodeName、执行耗时、输入输出摘要         |
| 模型级别     | modelName、token、耗时、错误码       |
| 工具级别     | toolName、耗时、是否成功             |
| 人工审批     | 中断时间、审批结果、恢复时间               |
| 持久化      | checkpoint 数量、恢复次数、失败次数      |

Admin 平台也支持 Observability，官方 Admin 文档中说明其集成 OpenTelemetry，可提供 Trace Tracking、服务监控和 Span 分析。([Spring AI Alibaba][24])

## 20. Sandbox

本章用于安全执行工具、脚本或 Shell 命令。1.1.2.0 Release Notes 中新增 SAA Sandbox 模块，说明其提供安全隔离的工具执行能力。([GitHub][1])

Sandbox 适合以下场景：

| 场景          | 说明                  |
| ----------- | ------------------- |
| Python 工具执行 | 数据处理、文件解析、图表生成      |
| Shell 工具执行  | 受控命令、项目内脚本          |
| 技能脚本运行      | Skills 目录下的 scripts |
| 文件处理        | PDF、CSV、日志处理        |
| 高风险隔离       | 限制目录、命令、资源、网络       |

生产建议：Shell / Python 工具必须配合白名单、工作目录限制、超时限制、输出长度限制和人工审批；不能让模型自由执行任意命令。

## 21. Studio / Agent Chat UI

本章用于本地调试和可视化聊天。Agent Chat UI 可以用可视化方式与任意 Spring AI Alibaba Agent 对话，支持嵌入式模式和独立运行模式。嵌入式模式只需在 Agent 项目中加入 `spring-ai-alibaba-studio` 依赖，然后访问 `/chatui/index.html`；独立模式则进入 `spring-ai-alibaba-studio/agent-chat-ui` 运行前端项目。([Spring AI Alibaba][25])

嵌入式模式依赖如下。

```xml
<dependency>
    <groupId>com.alibaba.cloud.ai</groupId>
    <artifactId>spring-ai-alibaba-studio</artifactId>
</dependency>
```

独立模式的前端配置通常包括：

```properties
NEXT_PUBLIC_API_URL=http://localhost:8080
NEXT_PUBLIC_APP_NAME=research_agent
NEXT_PUBLIC_USER_ID=user-001
```

Studio 适合开发期调试 Agent、观察流式响应、验证工具调用和演示 DeepResearch 等示例工程。

## 22. Admin 平台

本章用于说明 Spring AI Alibaba Admin 的定位。Admin 是基于 Spring AI Alibaba 的 AI Agent 开发与评估平台，覆盖 Prompt 工程、数据集管理、评估器配置、实验执行、结果分析等生命周期能力。官方 Admin 文档列出核心功能包括 Prompt Management、Dataset Management、Evaluator Management、Experiment Management、Observability 和 Model Configuration。([Spring AI Alibaba][24])

Admin 典型能力如下。

| 能力        | 说明                                |
| --------- | --------------------------------- |
| Prompt 管理 | 模板、版本、在线调试、多轮会话                   |
| 数据集管理     | 数据集导入、版本、数据项 CRUD、Trace 创建数据集     |
| 评估器管理     | 评估器配置、模板、自定义评估逻辑                  |
| 实验管理      | 批量实验、停止、重启、结果比较                   |
| 可观测性      | OpenTelemetry Trace、Span 分析       |
| 模型配置      | OpenAI、DashScope、DeepSeek 等模型统一配置 |
| 动态切换      | 运行时更新模型配置                         |

启动方式通常是克隆 Admin 仓库、配置模型 API Key、可选配置 Nacos，然后执行 `start.sh` 启动依赖服务，再运行 Admin Server。([Spring AI Alibaba][24])

## 23. Nacos 集成

本章用于动态配置、Prompt 管理、模型参数热更新和 A2A 注册发现。Spring AI Alibaba 版本说明中列出 `spring-ai-alibaba-starter-a2a-nacos` 用于基于 Nacos 的 A2A 通信，`spring-ai-alibaba-starter-config-nacos` 用于动态配置与模型热更新。([Spring AI Alibaba][2])

Nacos 典型用途如下。

| 用途                     | 说明                            |
| ---------------------- | ----------------------------- |
| A2A Registry           | 注册本地 AgentCard                |
| A2A Discovery          | 发现远程 Agent                    |
| Prompt 动态配置            | 在线更新 Prompt 模板                |
| 模型参数热更新                | 动态调整 temperature、topP、model 等 |
| Admin 连接               | Admin 平台与 Agent 应用联动          |
| NacosReactAgentBuilder | 从 Nacos 配置构建或合并 Agent 配置      |

动态 Prompt 的旧版实践文档中说明，可用 Nacos 配置中心管理 Prompt 模板并实现动态更新；虽然该实践页基于早期版本，但设计方向仍适用于理解 Nacos 动态配置场景。([Spring AI Alibaba][26])

## 24. 多模态与语音 Agent

本章用于处理语音、图片、视频和多模态工具结果。1.1.2.2 Release Notes 中说明 Voice Agent 采用 STT → ReactAgent → TTS 的“三明治架构”，基于 WebSocket 实现实时语音交互，并通过 `stt_chunk`、`agent_chunk`、`tts_chunk` 等事件流式输出；Multimodal 示例覆盖视觉、图像输出、TTS、DashScope 视觉模型、Wanx 图像生成和 `ToolMultimodalResult`。([GitHub][1])

常见架构如下。

```text
用户语音
  ↓
STT：语音转文本
  ↓
ReactAgent：理解、工具调用、生成回复
  ↓
TTS：文本转语音
  ↓
前端播放音频
```

DashScope TTS 支持普通调用和流式实时音频，配置前缀为 `spring.ai.dashscope.audio.speech`，支持模型、音色、输出格式、语速、采样率、音量、音调、SSML、时间戳和情绪控制等参数。([Spring AI Alibaba][27])

语音 Agent 生产建议：

| 项        | 建议                           |
| -------- | ---------------------------- |
| STT 分片   | 前端分片上传，后端聚合识别                |
| Agent 输出 | 使用流式响应降低等待时间                 |
| TTS 输出   | 边生成边播放                       |
| 工具调用     | 对慢工具增加状态提示                   |
| 多模态结果    | 用结构化对象表达 URL、base64、mimeType |
| 审计       | 保存文本转写和最终回复，不默认保存原始音频        |

## 25. 示例工程

本章用于选择学习路径。官方版本说明列出 examples 下的 Chatbot、DeepResearch、Documentation 示例工程；DeepResearch 文档说明其是一个基于 Spring AI Alibaba 的深度研究 Agent，结合任务规划、子 Agent、文件系统访问和详细 Prompt 处理复杂多步骤研究任务。([Spring AI Alibaba][2])

推荐学习顺序如下。

| 示例              | 学习重点                       |
| --------------- | -------------------------- |
| Chatbot         | 基础对话、DashScope、Studio、工具   |
| Documentation   | Agent 与 Graph 文档配套示例       |
| DeepResearch    | 多步骤研究、任务规划、子 Agent、MCP     |
| Graph Examples  | StateGraph、并行、持久化、HITL、MCP |
| A2A Examples    | Agent 注册、发现、远程调用           |
| Skills Examples | SKILL.md、read_skill、渐进式披露  |

DeepResearch 适合学习复杂 Agent 工程化。它不只是简单工具调用循环，而是结合 planning、context management 和 subagent collaboration 来处理深度研究任务。([Spring AI Alibaba][28])

## 26. 生产实践

本章用于落地 Spring AI Alibaba 应用。生产系统中不要把所有能力堆到一个 Agent，应按任务边界拆分 Agent、工具、Graph 节点和审批节点。

推荐实践如下。

| 领域      | 建议                                             |
| ------- | ---------------------------------------------- |
| 版本      | 所有 SAA 模块使用同一个 `${spring-ai-alibaba.version}`  |
| 模型      | 默认模型和高阶模型分层，复杂任务再升级模型                          |
| Agent   | 单 Agent 工具数量控制在合理范围，工具过多时拆分多 Agent             |
| 工具      | 工具描述明确，输入 schema 精确，返回值简洁                      |
| Memory  | 使用 threadId 隔离会话，生产使用 Redis/PostgreSQL/MongoDB |
| Skills  | SKILL.md 控制大小，长资料放 references                  |
| Graph   | 关键业务流程显式建模为 Graph                              |
| HITL    | 高风险工具必须审批                                      |
| Sandbox | Shell/Python 必须白名单和超时                          |
| 可观测性    | 对模型、工具、节点、审批、checkpoint 做 Trace                |
| 成本      | 控制消息长度、摘要历史、并行任务限流                             |
| 安全      | 不向模型暴露密钥、数据库直连密码、无限权限工具                        |

推荐分层结构如下。

```text
controller
  └── application service
        ├── agent service
        ├── workflow service
        ├── tool service
        ├── memory service
        └── audit service
```

工程规范上，Agent 不应直接操作数据库写入；应通过受控 Tool 调用业务 Service。Tool 层负责参数校验、权限判断、日志审计和异常转换。Graph 层负责流程编排、状态传递、审批和恢复。Admin / Studio 负责调试、评估和观测。

## 27. 常见问题

本章整理开发过程中最容易混淆的问题。

**Spring AI Alibaba 与 Spring AI 的边界是什么？**

Spring AI Alibaba 基于 Spring AI，但本文只关注 SAA 特有能力：ReactAgent、Graph Runtime、Agent Skills、Multi-agent Patterns、A2A、Studio、Admin、Sandbox、Nacos 集成、DashScope 扩展等。ChatClient、通用 ToolCallback、通用 Advisor 等属于 Spring AI 基础能力，不在本文展开。

**为什么官方文档很多示例还是 1.1.2.0？**

官方版本页和部分模块页仍以 1.1.2.0 为示例；GitHub Release 和 Maven Central 已有 1.1.2.2，且 1.1.2.1 被建议改用 1.1.2.2。因此项目中可按你要求统一使用 1.1.2.2。([Spring AI Alibaba][2])

**什么时候用 ReactAgent，什么时候用 Graph？**

单个任务闭环、工具数量有限、流程不复杂时用 ReactAgent。流程固定、需要条件分支、并行、审批、恢复、可观测、可回放时用 Graph。Workflow 文档也说明 Graph 是低级工作流和多智能体编排框架，可直接使用 Graph API 实现复杂编排。([Spring AI Alibaba][13])

**什么时候用 SupervisorAgent，什么时候用 LlmRoutingAgent？**

简单单次路由用 LlmRoutingAgent；多步骤任务、需要子 Agent 执行后返回监督者继续决策时用 SupervisorAgent。官方 Multi-agent 文档明确区分了两者：LlmRoutingAgent 是单次路由，SupervisorAgent 支持多步骤循环路由。([Spring AI Alibaba][12])

**Skills 和 Tool 有什么区别？**

Skill 是指令、上下文和资源包；Tool 是可执行能力。Skills 解决“模型什么时候需要什么知识”的问题，Tool 解决“模型能调用什么操作”的问题。通过 `groupedTools` 可将工具绑定到 Skill，只有读取技能后才暴露对应工具。([Spring AI Alibaba][11])

**生产环境 MemorySaver 可以用吗？**

不建议。`MemorySaver` 适合本地开发和测试。生产环境应使用 Redis、PostgreSQL 或 MongoDB 等持久化 Checkpointer，以支持恢复、历史查询、HITL 和容错。([Spring AI Alibaba][20])

**A2A 一定要用 Nacos 吗？**

不一定，但 Spring AI Alibaba 当前文档重点展示了 Nacos Registry / Discovery 集成。若业务系统已经使用 Nacos，A2A + Nacos 是较自然的注册发现方案。([Spring AI Alibaba][18])

**DashScope reasoningContent 可以直接展示给用户吗？**

技术上可以读取，但是否展示取决于产品策略和安全策略。建议内部调试时记录或展示，面向用户时可转化为“分析摘要”，避免暴露过长或不稳定的中间过程。该字段是否存在取决于模型是否支持 thinking 以及是否启用 `enableThinking`。([Spring AI Alibaba][5])

[1]: https://github.com/alibaba/spring-ai-alibaba/releases "Releases · alibaba/spring-ai-alibaba · GitHub"
[2]: https://java2ai.com/docs/versions "版本说明 | Spring AI Alibaba"
[3]: https://java2ai.com/integration/chatmodels/dashScope/ "DashScope | Spring AI Alibaba"
[4]: https://java2ai.com/docs/frameworks/agent-framework/tutorials/agents/ "Agents | Spring AI Alibaba"
[5]: https://java2ai.com/integration/chatmodels/dashScope/?utm_source=chatgpt.com "DashScope | Spring AI Alibaba"
[6]: https://java2ai.com/docs/frameworks/agent-framework/tutorials/structured-output/?utm_source=chatgpt.com "Structured Output 结构化输出 | Spring AI Alibaba"
[7]: https://java2ai.com/en/docs/frameworks/agent-framework/tutorials/tools?utm_source=chatgpt.com "Tools 工具 | Spring AI Alibaba"
[8]: https://java2ai.com/docs/frameworks/agent-framework/tutorials/hooks "Hooks 和 Interceptors | Spring AI Alibaba"
[9]: https://java2ai.com/docs/frameworks/agent-framework/tutorials/memory?utm_source=chatgpt.com "Memory 短期记忆 | Spring AI Alibaba"
[10]: https://java2ai.com/docs/frameworks/agent-framework/advanced/context-engineering?utm_source=chatgpt.com "上下文工程（Context Engineering） | Spring AI Alibaba"
[11]: https://java2ai.com/docs/frameworks/agent-framework/tutorials/skills "Skills 技能 | Spring AI Alibaba"
[12]: https://java2ai.com/en/docs/frameworks/agent-framework/advanced/multi-agent?utm_source=chatgpt.com "多智能体（Multi-agent） | Spring AI Alibaba"
[13]: https://java2ai.com/en/docs/frameworks/agent-framework/advanced/workflow "工作流（Workflow） | Spring AI Alibaba"
[14]: https://java2ai.com/en/docs/frameworks/agent-framework/advanced/workflow?utm_source=chatgpt.com "工作流（Workflow） | Spring AI Alibaba"
[15]: https://java2ai.com/en/docs/frameworks/agent-framework/advanced/rag "检索增强生成（RAG） | Spring AI Alibaba"
[16]: https://java2ai.com/en/docs/frameworks/agent-framework/advanced/human-in-the-loop?utm_source=chatgpt.com "人工介入（Human-in-the-Loop） | Spring AI Alibaba"
[17]: https://java2ai.com/docs/frameworks/graph-core/examples/human-in-the-loop?utm_source=chatgpt.com "人类反馈 | Spring AI Alibaba"
[18]: https://java2ai.com/docs/frameworks/agent-framework/advanced/a2a?utm_source=chatgpt.com "分布式智能体（A2A Agent） | Spring AI Alibaba"
[19]: https://java2ai.com/docs/frameworks/graph-core/examples/parallel-branch?utm_source=chatgpt.com "并行节点定义 | Spring AI Alibaba"
[20]: https://java2ai.com/en/docs/frameworks/graph-core/core/persistence?utm_source=chatgpt.com "持久化 | Spring AI Alibaba"
[21]: https://java2ai.com/en/docs/frameworks/graph-core/core/checkpoint-postgres?utm_source=chatgpt.com "PostgreSQL 检查点持久化 | Spring AI Alibaba"
[22]: https://java2ai.com/docs/frameworks/graph-core/examples/parallel-streaming?utm_source=chatgpt.com "并行流式输出 | Spring AI Alibaba"
[23]: https://java2ai.com/docs/frameworks/graph-core/examples/mcp-node?utm_source=chatgpt.com "分配MCP工具给指定节点 | Spring AI Alibaba"
[24]: https://java2ai.com/en/ecosystem/admin/quick-start "快速开始 | Spring AI Alibaba"
[25]: https://java2ai.com/docs/frameworks/studio/quick-start "快速开始 | Spring AI Alibaba"
[26]: https://java2ai.com/en/docs/1.0.0.2/practices/dynamic-prompt/dynamic-prompt/?utm_source=chatgpt.com "Dynamic Prompt Best Practices-Alibaba CloudSpring AI Alibaba官网Official Website"
[27]: https://java2ai.com/integration/multimodals/audio/speech/dashscope-speech/?utm_source=chatgpt.com "DashScope Text-to-Speech (TTS) | Spring AI Alibaba"
[28]: https://java2ai.com/agents/deepresearch/quick-start "DeepResearch 快速开始 | Spring AI Alibaba"
