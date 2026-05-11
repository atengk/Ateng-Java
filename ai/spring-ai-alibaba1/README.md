# Spring AI Alibaba



## 版本信息

| 组件             | 版本                                                         |
| ---------------- | ------------------------------------------------------------ |
| JDK              | 21                                                           |
| Maven            | 3.9.12                                                       |
| SpringBoot       | 3.5.13                                                       |
| SpringAI         | 1.1.4                                                        |
| SpringAI Alibaba | 1.1.2.2                                                      |
| Model            | [阿里云百炼](https://bailian.console.aliyun.com/cn-beijing/?tab=model#/model-usage/free-quota) |

------

## 基础配置

本节用于配置 Maven 依赖和 DashScope 连接信息。示例使用 `spring-ai-alibaba-starter-dashscope`，默认接入阿里云百炼 DashScope。

### 添加依赖

通过 BOM 管理 Spring AI 和 Spring AI Alibaba 版本，业务依赖中不需要重复声明版本号。

```xml
<properties>
    <spring-ai.version>1.1.4</spring-ai.version>
    <spring-ai-alibaba.version>1.1.2.2</spring-ai-alibaba.version>
</properties>
<dependencies>
    <!-- Spring AI Alibaba DashScope -->
    <dependency>
        <groupId>com.alibaba.cloud.ai</groupId>
        <artifactId>spring-ai-alibaba-starter-dashscope</artifactId>
    </dependency>
</dependencies>
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-bom</artifactId>
            <version>${spring-ai.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
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
```

### 编辑配置

`spring.ai.model.chat` 和 `spring.ai.model.embedding` 用于指定默认模型提供方。这里统一使用 `dashscope`。

```yaml
---
# Spring AI Alibaba 配置
spring:
  ai:
    model:
      # 聊天模型使用 DashScope
      chat: dashscope
      # 嵌入模型使用 DashScope
      embedding: dashscope

    dashscope:
      # DashScope 服务地址
      base-url: https://dashscope.aliyuncs.com

      # 建议通过环境变量注入
      api-key: ${DASHSCOPE_API_KEY}

      chat:
        options:
          # DashScope Chat 模型名称
          # 通用对话 / Agent：qwen3.6-plus、qwen-plus、qwen-max
          # 视觉理解：qwen3-vl-plus、qwen-vl-plus、qwen-vl-max
          model: qwen-plus

          # 是否使用多模态调用链
          # 传入图片、视频等 Media 时设为 true；普通文本对话设为 false
          multi-model: false

          # 输出随机性，越低越稳定
          temperature: 0.7

          # 最大输出 token 数，按需开启
          # max-tokens: 2048

          # 核采样参数，按需开启
          # top-p: 0.8

      embedding:
        options:
          # 嵌入模型，RAG / 向量检索常用
          model: text-embedding-v4

```

如果需要调用视觉理解模型或传入图片、视频等多模态内容，可以按需调整为：

```yaml
spring:
  ai:
    dashscope:
      chat:
        options:
          model: qwen3-vl-plus
          multi-model: true
```

---

## 快速开始

本节提供两个最小 Controller：一个用于文本对话，一个用于文本向量化。代码统一注入 Spring AI 抽象接口，避免直接绑定具体实现类。

### 文本对话

下面的接口通过 `ChatModel` 调用 DashScope 聊天模型，提供普通响应和流式响应两个接口。

```java
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
```

测试普通接口：

```bash
curl "http://localhost:19003/ai/generate?message=你好，请介绍一下Spring AI Alibaba"
```

测试流式接口：

```bash
curl -N "http://localhost:19003/ai/generateStream?message=写一段Spring AI Alibaba简介"
```

### 文本向量

下面的接口通过 `EmbeddingModel` 调用 DashScope 嵌入模型。接口只返回向量维度和前 8 个向量值，避免响应体过大。

```java
package io.github.atengk.ai.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 嵌入模型接口
 *
 * @author Ateng
 * @since 2026-04-24
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class EmbeddingController {

    private final EmbeddingModel embeddingModel;

    /**
     * 调用 DashScope Embedding 模型生成文本向量
     *
     * @param text 待向量化文本
     * @return 向量维度和部分向量值
     */
    @GetMapping("/ai/embedding")
    public Map<String, Object> embedding(@RequestParam(required = false) String text) {
        String input = StrUtil.blankToDefault(text, "Spring AI Alibaba 是一个面向 Java 生态的 AI 应用开发框架");
        log.info("收到嵌入模型请求，text={}", input);

        float[] vector = embeddingModel.embed(input);
        List<Double> preview = Arrays.stream(toDoubleArray(vector))
                .limit(8)
                .map(value -> NumberUtil.round(value, 6).doubleValue())
                .boxed()
                .toList();

        return Map.of(
                "modelProvider", "dashscope",
                "text", input,
                "dimension", vector.length,
                "preview", CollUtil.defaultIfEmpty(preview, List.of())
        );
    }

    /**
     * float 数组转换为 double 数组，便于使用 Stream 处理
     *
     * @param vector float 向量
     * @return double 向量
     */
    private double[] toDoubleArray(float[] vector) {
        double[] values = new double[vector.length];
        for (int index = 0; index < vector.length; index++) {
            values[index] = vector[index];
        }
        return values;
    }

}
```

测试接口：

```bash
curl "http://localhost:19003/ai/embedding?text=什么是Spring AI Alibaba"
```

返回结构示例：

```json
{
    "dimension": 1024,
    "preview": [
        -0.061817,
        0.011084,
        0.003405,
        0.004771,
        -0.057352,
        -0.013993,
        0.019803,
        0.095664
    ],
    "modelProvider": "dashscope",
    "text": "Spring AI Alibaba 是一个面向 Java 生态的 AI 应用开发框架"
}
```



## ReactAgent

这一节用于说明 Spring AI Alibaba Agent Framework 中 `ReactAgent` 的基础使用方式。`ReactAgent` 是 Spring AI Alibaba 提供的 Agent 实现，基于 ReAct 思想和 Graph Runtime 构建，能够在“大模型推理”和“工具调用”之间循环执行，直到得到最终答案或达到停止条件。官方文档说明，`ReactAgent` 的核心节点包括 Model Node、Tool Node 和 Hook Nodes，分别负责模型推理、工具执行和流程控制。

### 依赖配置

使用 `ReactAgent` 需要引入 Agent Framework 依赖。

```xml
<dependencies>
    <!-- Spring AI Alibaba Agent Framework -->
    <dependency>
        <groupId>com.alibaba.cloud.ai</groupId>
        <artifactId>spring-ai-alibaba-agent-framework</artifactId>
    </dependency>
</dependencies>
```

### 创建 ReactAgent

下面定义一个基础 `ReactAgent`。它注入 Spring 容器中的 `ChatModel`，并通过 `instruction` 指定 Agent 的行为约束。Spring AI Alibaba 官方示例中，`ReactAgent.builder().model(chatModel)` 是最基础的模型接入方式。

`ReactAgent` 底层依赖的是 Spring AI 的 `ChatModel` 抽象，因此只要当前项目中注入的是 DashScope 的 `ChatModel`，Agent 推理就会走通义千问模型。

下面的配置类创建一个基础 ReactAgent Bean，用于普通推理任务。

```java
package io.github.atengk.ai.config;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.hook.modelcalllimit.ModelCallLimitHook;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
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
        log.info("初始化 Spring AI Alibaba ReactAgent");

        return ReactAgent.builder()
                .name("dashscope_react_agent")
                .model(chatModel)
                .description("基于 DashScope 通义千问的 ReAct 智能体")
                .instruction("""
                        你是一个专业的 Java 技术助手。
                        回答问题时需要先理解用户意图，再给出清晰、准确、可落地的方案。
                        如果需要使用工具，应根据工具返回结果继续分析，不要编造工具结果。
                        """)
                .hooks(ModelCallLimitHook.builder().runLimit(5).build())
                .saver(new MemorySaver())
                .build();
    }

}

```

这里有两个配置点建议保留：

`ModelCallLimitHook` 用于限制模型调用次数，避免 Agent 在复杂问题中无限循环。官方文档也建议通过 Hooks 控制 Agent 执行迭代，防止过度调用和成本失控。

`MemorySaver` 是内存级会话状态存储，适合本地开发和快速验证。生产环境如果需要持久化会话，可以替换为 Redis、MongoDB 等持久化 Saver。官方文档中也说明，`MemorySaver` 可通过 `threadId` 维护对话上下文。

### 调用 ReactAgent

下面提供一个简单 Controller，通过 HTTP 接口调用 `ReactAgent`。为了支持多轮上下文，接口增加了 `threadId` 参数；同一个 `threadId` 下的调用可以复用 Agent 记忆。

下面的 Controller 提供 ReactAgent 对话接口，并通过 threadId 维护会话上下文。

```java
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
 * ReactAgent 调用接口
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
     * 调用 ReactAgent
     *
     * @param message  用户问题
     * @param threadId 会话 ID
     * @return Agent 响应结果
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

```

测试接口：

```bash
curl "http://localhost:19003/agent/react/chat?threadId=user-001&message=我叫Ateng，请记住"
```

继续测试上下文：

```bash
curl "http://localhost:19003/agent/react/chat?threadId=user-001&message=我叫什么名字"
```

### 接入工具

`ReactAgent` 的关键能力是工具调用。Spring AI Alibaba 支持多种工具接入方式，包括 `tools()`、`methodTools()`、`toolCallbackProviders()`、`toolNames() + resolver()` 等。官方文档中说明，`methodTools()` 可以直接传入带有 `@Tool` 注解方法的对象，适合将相关工具组织在类中。

下面定义一个简单的时间工具，供 Agent 在需要时主动调用。

```java
package io.github.atengk.ai.tool;

import cn.hutool.core.date.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

/**
 * 时间工具
 *
 * @author Ateng
 * @since 2026-04-24
 */
@Slf4j
@Component
public class DateTimeTool {

    @Tool(description = "获取当前系统时间")
    public String now() {
        String now = DateUtil.now();
        log.info("调用时间工具，当前时间={}", now);
        return now;
    }

}

```

然后在 `ReactAgentConfig` 中注入工具类，并通过 `methodTools()` 注册。

```java
package com.example.ai.config;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.hook.modelcalllimit.ModelCallLimitHook;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.example.ai.tool.DateTimeTool;
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
    public ReactAgent reactAgent(ChatModel chatModel, DateTimeTool dateTimeTool) {
        log.info("初始化带工具的 Spring AI Alibaba ReactAgent");

        return ReactAgent.builder()
                .name("dashscope_react_agent")
                .model(chatModel)
                .description("基于 DashScope 通义千问的 ReAct 智能体")
                .instruction("""
                        你是一个专业的 Java 技术助手。
                        当问题需要实时信息或工具能力时，应优先调用可用工具。
                        工具返回结果不足时，需要明确说明不确定性。
                        """)
                .methodTools(dateTimeTool)
                .hooks(ModelCallLimitHook.builder().runLimit(5).build())
                .saver(new MemorySaver())
                .build();
    }

}
```

测试工具调用：

```bash
curl "http://localhost:8080/agent/react/chat?message=现在系统时间是多少"
```

### 使用 ToolCallback 注册工具

如果你不想使用 `@Tool` 注解，也可以通过 `FunctionToolCallback` 注册工具。官方文档中给出的 `tools()` 方式适合工具数量较少、定义明确的场景。

下面示例将一个 Java 函数注册为 Agent 工具。

```java
package com.example.ai.config;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Function;

/**
 * ReactAgent 工具回调配置
 *
 * @author Ateng
 * @since 2026-04-24
 */
@Slf4j
@Configuration
public class ReactAgentToolCallbackConfig {

    @Bean
    public ReactAgent toolCallbackReactAgent(ChatModel chatModel) {
        ToolCallback echoTool = FunctionToolCallback
                .builder("echo", (Function<String, String>) text -> {
                    log.info("调用 echo 工具，text={}", text);
                    return "工具返回：" + text;
                })
                .description("回显用户输入内容")
                .inputType(String.class)
                .build();

        return ReactAgent.builder()
                .name("tool_callback_react_agent")
                .model(chatModel)
                .instruction("你是一个可以使用工具完成任务的助手。")
                .tools(echoTool)
                .saver(new MemorySaver())
                .build();
    }

}
```



## Agent Framework

本章节用于说明 Spring AI Alibaba Agent Framework 的核心使用方式，重点围绕 `ReactAgent` 展开。`ReactAgent` 适合处理需要“模型推理 + 工具调用 + 状态管理 + 多轮执行”的任务；如果只是普通问答，直接使用前面章节的 `ChatModel` 更轻量。Spring AI Alibaba 官方说明中，Agent 会在模型推理和工具调用之间循环执行，直到模型给出最终答案或达到停止条件。

使用 Agent Framework 需要引入 Agent 相关依赖。

```xml
<!-- Spring AI Alibaba Agent Framework -->
<dependency>
    <groupId>com.alibaba.cloud.ai</groupId>
    <artifactId>spring-ai-alibaba-agent-framework</artifactId>
</dependency>
```

### ReactAgent

`ReactAgent` 是 Spring AI Alibaba 提供的 ReAct Agent 实现。ReAct 的核心是 Reasoning + Acting，也就是模型先推理当前任务，再决定是否调用工具，工具执行后将观察结果返回给模型，模型继续推理，直到输出最终答案。Spring AI Alibaba 的 `ReactAgent` 基于 Graph Runtime 构建，内部包含模型节点和工具节点，适合构建企业级智能体应用。

下面的配置类创建一个基础 `ReactAgent`，底层模型使用当前 Spring 容器中的 `ChatModel`，也就是前面配置的 DashScope 模型。

```java
package io.github.atengk.ai.config;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.hook.modelcalllimit.ModelCallLimitHook;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
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

}
```

`ModelCallLimitHook` 用于限制模型循环调用次数，避免复杂任务中出现无限推理或过度消耗。`MemorySaver` 是内存级 Checkpointer，适合开发和测试；生产环境如果需要恢复会话状态，建议替换为 Redis、数据库等持久化 Checkpointer。Spring AI Alibaba 文档说明，短期记忆依赖 Checkpointer 按 `threadId` 隔离和恢复会话状态。

### Agent 调用方式

`ReactAgent` 常用调用方式可以分为两类：`call` 和 `invoke`。`call` 更适合普通对话场景，通常直接返回 `AssistantMessage`；`invoke` 更适合需要读取完整 Graph 状态的场景，通常返回 `Optional<OverAllState>`。官方 Agents 文档中给出了 `call` 配合 `RunnableConfig` 以及 `invoke` 读取 `messages` 等状态的用法。

下面的 Controller 提供一个最小 Agent 对话接口。

```java
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
```

如果业务需要查看完整状态，可以使用 `invoke`。

```java
Optional<OverAllState> result = reactAgent.invoke(userMessage, config);
```

`OverAllState` 中通常可以读取 `messages`、结构化输出、自定义状态等内容，适合调试 Agent 执行过程、落库审计或做二次处理。

### Agent 状态管理

Agent 状态管理主要由 `RunnableConfig`、`OverAllState`、`Checkpointer` 和 `Store` 组成。`threadId` 用于隔离不同会话，`metadata` 用于传递用户 ID、租户 ID、业务场景等运行时参数，`Store` 可用于跨会话长期记忆。Spring AI Alibaba 文档说明，短期记忆存储在 Graph 状态中，并通过 Checkpointer 按线程恢复；长期记忆可以通过 `MemoryStore` 等 Store 实现跨会话数据管理。

下面示例展示如何在调用 Agent 时传递会话 ID 和业务元数据。

```java
RunnableConfig config = RunnableConfig.builder()
        .threadId("user-session-001")
        .addMetadata("user_id", "10001")
        .addMetadata("tenant_id", "default")
        .build();

AssistantMessage response = reactAgent.call("请记住我正在学习 Spring AI Alibaba", config);
```

如果是生产环境，应优先使用持久化 Checkpointer，避免服务重启后丢失 Agent 会话状态。

```java
@Bean
public ReactAgent reactAgent(ChatModel chatModel, RedisSaver redisSaver) {
    return ReactAgent.builder()
            .name("dashscope_react_agent")
            .model(chatModel)
            .saver(redisSaver)
            .build();
}
```

状态管理的推荐边界是：短期对话上下文放在 Checkpointer 中，用户画像、偏好、历史摘要等跨会话信息放在 Store 中，业务数据库仍然作为权威数据源，不建议把关键业务状态只保存在 Agent 内存状态里。

### Agent 结构化输出

结构化输出用于让 Agent 返回可预测的数据格式，例如 JSON 或 Java POJO。Spring AI Alibaba 的 `ReactAgent.Builder` 支持通过 `outputSchema` 和 `outputType` 指定输出结构；文档说明，`outputType(Class<?>)` 更类型安全，框架会使用 `BeanOutputConverter` 自动生成 JSON Schema。

下面定义一个结构化输出对象。

```java
package io.github.atengk.ai.model;

/**
 * 问题分析结果
 *
 * @author Ateng
 * @since 2026-04-24
 */
public class AnalysisResult {

    private String summary;

    private String riskLevel;

    private String suggestion;

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public String getSuggestion() {
        return suggestion;
    }

    public void setSuggestion(String suggestion) {
        this.suggestion = suggestion;
    }

}
```

下面创建一个带结构化输出约束的 Agent。

```java
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
```

调用后可以从 Agent 响应文本中获取 JSON，也可以在使用 `invoke` 时从 `OverAllState` 中读取结构化结果。Spring AI Alibaba 文档说明，结构化响应会进入 Agent 状态对象，可通过 `structured_output` 读取。

### Agent 流式消息

Agent 流式消息适合用于 Web 页面、SSE 接口、命令行交互等场景，可以实时返回模型 token、工具执行进度或节点输出。Spring AI Alibaba 1.1.2.0 Release Notes 中说明新增了 `streamMessages` API，用于简化消息流式输出，同时 Graph 流式节点会输出包含 `_FINISHED` 在内的完整输出事件。

下面是一个 SSE 接口示例。不同小版本的 `streamMessages` 返回类型可能略有差异，如果 IDE 中方法签名不同，按实际返回对象提取文本即可。

```java
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
```

如果当前依赖版本没有 `streamMessages`，可以先检查是否为 `1.1.2.x`，或者使用 Agent / Graph 提供的 `stream` 类 API 做适配。官方 Release Notes 中明确 `1.1.2.0` 引入了新的 `streamMessages` API，建议使用 `1.1.2.2` 规避 `1.1.2.1` 已知问题。

### Agent 执行配置

Agent 执行配置主要通过 `RunnableConfig` 控制。常用配置包括 `threadId`、`metadata`、`store` 等。`threadId` 决定会话隔离，`metadata` 传递运行时业务上下文，`store` 用于长期记忆或跨会话数据访问。官方文档示例中，`RunnableConfig.builder().threadId(...).addMetadata(...).store(...)` 用于将用户信息和记忆存储传递给 Agent。

下面是一个推荐的执行配置封装方法。

```java
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
```

执行配置不建议滥用。稳定的业务数据应从业务数据库查询；`metadata` 适合传递轻量上下文，例如用户 ID、租户 ID、语言、权限标识、链路 ID。复杂对象建议放入 Store 或业务服务中，再通过工具按需读取。

---

### RedisSaver 状态持久化

`RedisSaver` 用于将 Agent 的短期记忆和 Graph 执行状态保存到 Redis 中。前面示例中的 `MemorySaver` 只适合本地开发和快速验证，应用重启后会话状态会丢失；如果需要在生产环境中保留 `threadId` 对应的多轮上下文，建议使用 `RedisSaver`。Spring AI Alibaba 官方文档也说明，生产环境可以使用 `RedisSaver`、`MongoSaver` 等持久化 Checkpointer 替代 `MemorySaver`。

`RedisSaver` 依赖 `RedissonClient`，因此项目中需要先引入 Redisson 依赖。

```xml
<dependency>
    <groupId>org.redisson</groupId>
    <artifactId>redisson-spring-boot-starter</artifactId>
</dependency>
```

配置 Redis 连接信息。

```yaml
spring:
  data:
    redis:
      host: 127.0.0.1
      port: 6379
      password:
      database: 0
```

如果使用 `redisson-spring-boot-starter`，通常会自动创建 `RedissonClient`。如果你的项目没有自动装配成功，可以手动定义一个 `RedissonClient`。

下面的配置类用于创建 `RedissonClient`，连接本地 Redis 服务。

```java
package com.example.ai.config;

import cn.hutool.core.util.StrUtil;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redisson 配置
 *
 * @author Ateng
 * @since 2026-04-24
 */
@Configuration
public class RedissonConfig {

    @Value("${spring.data.redis.host:127.0.0.1}")
    private String host;

    @Value("${spring.data.redis.port:6379}")
    private Integer port;

    @Value("${spring.data.redis.password:}")
    private String password;

    @Value("${spring.data.redis.database:0}")
    private Integer database;

    /**
     * 创建 Redisson 客户端
     *
     * @return Redisson 客户端
     */
    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        Config config = new Config();
        String address = StrUtil.format("redis://{}:{}", host, port);

        config.useSingleServer()
                .setAddress(address)
                .setDatabase(database);

        if (StrUtil.isNotBlank(password)) {
            config.useSingleServer().setPassword(password);
        }

        return Redisson.create(config);
    }

}
```

然后在 `ReactAgent` 中将 `MemorySaver` 替换为 `RedisSaver`。官方示例中也是通过 `new RedisSaver(redissonClient)` 创建 Redis Checkpointer，再传入 `ReactAgent.builder().saver(...)`。([Spring AI Alibaba](https://java2ai.com/docs/frameworks/agent-framework/tutorials/memory/?utm_source=chatgpt.com))

下面的配置类使用 RedisSaver 保存 ReactAgent 的会话状态。

```java
package com.example.ai.config;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.hook.modelcalllimit.ModelCallLimitHook;
import com.alibaba.cloud.ai.graph.checkpoint.savers.RedisSaver;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ReactAgent Redis 状态持久化配置
 *
 * @author Ateng
 * @since 2026-04-24
 */
@Slf4j
@Configuration
public class ReactAgentRedisConfig {

    @Bean
    public ReactAgent redisReactAgent(ChatModel chatModel, RedissonClient redissonClient) {
        log.info("初始化 RedisSaver ReactAgent");

        RedisSaver redisSaver = new RedisSaver(redissonClient);

        return ReactAgent.builder()
                .name("dashscope_redis_react_agent")
                .model(chatModel)
                .description("使用 RedisSaver 持久化状态的 ReAct 智能体")
                .instruction("""
                        你是一个专业的 Java 技术助手。
                        需要结合当前会话上下文回答用户问题。
                        如果上下文不足，需要主动说明无法确认。
                        """)
                .hooks(ModelCallLimitHook.builder().runLimit(5).build())
                .saver(redisSaver)
                .build();
    }

}
```

调用时仍然通过 `RunnableConfig.threadId(...)` 维护会话。只要 `threadId` 一致，Agent 就可以从 Redis 中恢复同一会话的短期记忆。

```java
RunnableConfig config = RunnableConfig.builder()
        .threadId("user-001")
        .build();

reactAgent.call("我叫 Ateng，请记住", config);
reactAgent.call("我叫什么名字？", config);
```

如果在 Controller 中使用，建议保留 `threadId` 参数，由调用方传入用户会话 ID。

```java
@GetMapping("/agent/react/chat")
public Map<String, Object> chat(@RequestParam(required = false) String message,
                                @RequestParam(required = false) String threadId) throws GraphRunnerException {
    String userMessage = StrUtil.blankToDefault(message, "请介绍一下 Spring AI Alibaba ReactAgent");
    String conversationId = StrUtil.blankToDefault(threadId, "default-thread");

    log.info("收到 RedisSaver ReactAgent 请求，threadId={}，message={}", conversationId, userMessage);

    RunnableConfig config = RunnableConfig.builder()
            .threadId(conversationId)
            .build();

    AssistantMessage response = reactAgent.call(userMessage, config);

    return Map.of(
            "agent", "dashscope_redis_react_agent",
            "threadId", conversationId,
            "content", StrUtil.blankToDefault(response.getText(), "")
    );
}
```

使用 `RedisSaver` 时需要注意：`threadId` 是会话隔离的关键字段，不同用户或不同业务会话不要复用同一个 `threadId`；Redis 中保存的是 Agent 执行状态和短期上下文，不建议把它当作业务数据库使用；生产环境还需要配置 Redis 过期策略、容量监控和敏感信息治理。对于用户画像、偏好、长期资料等跨会话信息，更适合使用 Store 或业务数据库，而不是仅依赖 Checkpointer。

## Tools 增强能力

Tools 是 Agent 访问外部世界的主要方式。模型本身只负责推理和决策，真正的业务操作应封装成工具，例如查询数据库、调用 HTTP 接口、检索知识库、执行计算任务等。Spring AI Alibaba 文档说明，工具可以通过 `tools()`、`methodTools()`、`toolCallbackProviders()`、`toolNames() + resolver()` 等方式提供给 `ReactAgent`。([Spring AI Alibaba](https://java2ai.com/en/docs/frameworks/agent-framework/tutorials/tools?utm_source=chatgpt.com))

### ToolContext

`ToolContext` 用于在工具执行时访问运行时上下文。Spring AI 官方文档说明，`ToolContext` 可以把用户提供的上下文信息传给工具方法，并且这些上下文不会发送给模型。Spring AI Alibaba 文档进一步说明，在 Agent 场景中，工具可以通过 `ToolContext` 访问状态、上下文、Store、`RunnableConfig` 和当前工具调用 ID。([Home](https://docs.spring.io/spring-ai/reference/api/tools.html?utm_source=chatgpt.com))

下面示例展示工具如何从 `ToolContext` 中读取运行时配置。

```java
package com.example.ai.tool;

import cn.hutool.core.util.StrUtil;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

/**
 * 用户上下文工具
 *
 * @author Ateng
 * @since 2026-04-24
 */
@Slf4j
@Component
public class UserContextTool {

    @Tool(description = "获取当前用户上下文信息")
    public String getUserContext(ToolContext toolContext) {
        RunnableConfig config = (RunnableConfig) toolContext.getContext().get("config");
        String userId = config == null ? "" : String.valueOf(config.metadata("user_id").orElse(""));
        String tenantId = config == null ? "" : String.valueOf(config.metadata("tenant_id").orElse(""));

        log.info("读取工具上下文，userId={}，tenantId={}", userId, tenantId);

        if (StrUtil.isBlank(userId)) {
            return "未获取到用户上下文";
        }

        return "当前用户ID：" + userId + "，租户ID：" + tenantId;
    }

}
```

工具参数中加入 `ToolContext` 后，模型不会看到该参数，它只由框架在工具执行时注入。适合在工具中读取登录用户、租户、权限、会话、长期记忆等运行时信息。

### ToolContextHelper

`ToolContextHelper` 是 Spring AI Alibaba 1.1.2.0 引入的辅助能力，用于简化从 `ToolContext` 中读取 metadata 的过程。Release Notes 明确说明，`ToolContextHelper` 是用于访问 tool context metadata 的 helper class。([GitHub](https://github.com/alibaba/spring-ai-alibaba/releases?utm_source=chatgpt.com))

如果项目当前版本已经包含 `ToolContextHelper`，建议用它替代手动从 `toolContext.getContext()` 中强转 `RunnableConfig` 的方式。具体方法名以当前依赖版本的 IDE 提示为准，使用思路如下：

```java
@Tool(description = "根据当前上下文获取用户ID")
public String getCurrentUserId(ToolContext toolContext) {
    // 示例：优先使用 ToolContextHelper 读取 metadata
    // String userId = ToolContextHelper.getMetadata(toolContext, "user_id", String.class);

    // 如果当前版本 API 不一致，可以退回到手动读取 RunnableConfig
    RunnableConfig config = (RunnableConfig) toolContext.getContext().get("config");
    String userId = config == null ? "" : String.valueOf(config.metadata("user_id").orElse(""));

    return StrUtil.blankToDefault(userId, "unknown");
}
```

文档中建议说明：`ToolContextHelper` 主要解决重复样板代码问题，业务工具不应直接依赖过深的上下文 Map 结构；如果框架版本升级导致上下文结构变化，Helper 的维护成本低于大量工具方法中的强转逻辑。

### 工具拦截器

工具拦截器用于在工具执行前后进行统一处理，例如日志审计、权限校验、参数清洗、错误重试、动态工具选择等。Spring AI Alibaba 文档说明，Hooks 和 Interceptors 可以在 Agent 执行过程中进行监控、修改、控制和护栏处理；Release Notes 也列出了 `ToolRetryInterceptor`、`ToolSelectionInterceptor`、`ContextEditingInterceptor` 等内置能力。([Spring AI Alibaba](https://java2ai.com/docs/frameworks/agent-framework/tutorials/hooks?utm_source=chatgpt.com))

典型使用场景包括：

| 场景     | 处理方式                                         |
| -------- | ------------------------------------------------ |
| 工具审计 | 记录工具名、输入摘要、执行耗时、调用结果         |
| 权限控制 | 根据 `metadata` 中的用户角色过滤工具             |
| 参数治理 | 对工具参数做脱敏、截断、格式校验                 |
| 异常治理 | 统一包装工具异常，避免异常栈直接暴露给模型       |
| 动态工具 | 根据任务类型、用户权限、业务状态决定可用工具集合 |

如果业务只是需要简单日志，优先在工具方法内部记录即可；如果多个工具都有相同处理逻辑，再抽象为 Tool Interceptor。

### 工具错误处理

工具错误处理的原则是：不要把 Java 异常栈直接返回给模型，也不要让工具异常中断整个服务请求。工具内部应捕获可预期异常，返回结构化、可理解、可继续推理的错误信息。对于临时网络异常、限流异常，可以配合重试拦截器或业务重试机制处理。Spring AI Alibaba 1.1.x Release Notes 中列出了 `ToolRetryInterceptor`，用于工具重试类场景。([GitHub](https://github.com/alibaba/spring-ai-alibaba/releases?utm_source=chatgpt.com))

下面示例展示一个带错误处理的查询工具。

```java
package com.example.ai.tool;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

/**
 * 订单查询工具
 *
 * @author Ateng
 * @since 2026-04-24
 */
@Slf4j
@Component
public class OrderQueryTool {

    @Tool(description = "根据订单号查询订单状态")
    public String queryOrderStatus(String orderNo) {
        if (StrUtil.isBlank(orderNo)) {
            return "订单号不能为空";
        }

        try {
            log.info("查询订单状态，orderNo={}", orderNo);

            // 示例逻辑：实际项目中替换为数据库或远程服务调用
            if (StrUtil.startWith(orderNo, "TEST")) {
                return "订单状态：已完成";
            }

            return "未查询到订单，请确认订单号是否正确";
        }
        catch (Exception ex) {
            log.warn("查询订单状态失败，orderNo={}，原因={}", orderNo, ex.getMessage());
            return "订单查询失败，请稍后重试";
        }
    }

}
```

工具返回内容应尽量短、准、可被模型继续推理。对于不可恢复异常，建议返回“失败原因 + 下一步建议”；对于权限异常，返回“无权限执行该操作”，不要返回敏感系统信息。

### 异步工具执行

异步工具执行适合耗时 IO 场景，例如远程 HTTP 调用、检索服务、复杂计算或第三方系统查询。Spring AI Alibaba 1.1.2.0 Release Notes 明确说明，AgentToolNode 支持异步工具执行，并支持工具执行的并行化能力。([GitHub](https://github.com/alibaba/spring-ai-alibaba/releases?utm_source=chatgpt.com))

业务代码层面，建议把耗时逻辑放入独立 Service 中，再由工具方法调用，避免工具类膨胀。

下面示例展示异步服务封装方式。

```java
package com.example.ai.service;

import cn.hutool.core.thread.ThreadUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * 异步检索服务
 *
 * @author Ateng
 * @since 2026-04-24
 */
@Slf4j
@Service
public class AsyncSearchService {

    /**
     * 异步检索知识内容
     *
     * @param keyword 检索关键词
     * @return 检索结果
     */
    @Async
    public CompletableFuture<String> search(String keyword) {
        log.info("开始异步检索，keyword={}", keyword);

        // 示例逻辑：模拟远程检索耗时
        ThreadUtil.sleep(300);

        return CompletableFuture.completedFuture("检索结果：" + keyword + " 与 Spring AI Alibaba Agent Framework 相关");
    }

}
```

工具方法中可以等待异步结果，也可以根据当前框架版本能力直接返回异步结果。实际项目中建议结合超时控制，避免工具调用长时间挂起。

```java
@Tool(description = "异步检索知识内容")
public String searchKnowledge(String keyword) {
    try {
        return asyncSearchService.search(keyword).get(3, TimeUnit.SECONDS);
    }
    catch (Exception ex) {
        log.warn("异步检索失败，keyword={}，原因={}", keyword, ex.getMessage());
        return "检索超时或失败，请稍后重试";
    }
}
```

### 并行工具执行

并行工具执行适合模型一次需要调用多个互不依赖的工具，例如同时查询用户信息、订单信息和库存信息，然后再由模型综合结果。Spring AI Alibaba 1.1.2.0 Release Notes 说明，框架支持 async and parallel tool execution，同时 Graph 增强了并行条件边和 AllOf / AnyOf 聚合策略。([GitHub](https://github.com/alibaba/spring-ai-alibaba/releases?utm_source=chatgpt.com))

对于 Agent 工具来说，是否并行通常由模型的多工具调用结果和框架执行策略共同决定。业务侧应保证工具之间无隐式共享可变状态，或者对共享资源做好线程安全处理。

适合并行的工具：

| 工具类型     | 是否适合并行         |
| ------------ | -------------------- |
| 查询用户资料 | 适合                 |
| 查询订单状态 | 适合                 |
| 查询库存     | 适合                 |
| 写数据库     | 谨慎                 |
| 扣减库存     | 不建议随意并行       |
| 支付、退款   | 不建议由模型自由并行 |

如果工具之间存在强顺序关系，例如“先创建订单，再扣减库存，再发起支付”，不建议交给模型自由并行，应使用明确的工作流或业务服务编排。

### Tool returnDirect

`returnDirect` 用于控制工具结果是否直接返回给调用方，而不是再交给模型进行二次总结。Spring AI 文档说明，默认情况下工具结果会回传给模型，让模型继续对话；如果工具的 `returnDirect` 为 `true`，工具结果会直接返回给调用者。文档还特别说明，如果一次请求中存在多个工具调用，只有所有工具都设置 `returnDirect=true` 时，结果才会直接返回，否则仍会回到模型。([Home](https://docs.spring.io/spring-ai/reference/api/tools.html?utm_source=chatgpt.com))

声明式工具可以直接在 `@Tool` 上设置 `returnDirect`。

```java
package com.example.ai.tool;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

/**
 * 直接返回工具
 *
 * @author Ateng
 * @since 2026-04-24
 */
@Slf4j
@Component
public class DirectReturnTool {

    @Tool(description = "查询系统健康状态，并将结果直接返回给调用方", returnDirect = true)
    public String health() {
        log.info("查询系统健康状态");
        return "系统状态正常";
    }

    @Tool(description = "根据关键字检索原始文档片段，并直接返回给调用方", returnDirect = true)
    public String searchRawDocument(String keyword) {
        String query = StrUtil.blankToDefault(keyword, "Spring AI Alibaba");
        log.info("检索原始文档片段，keyword={}", query);

        return "原始文档片段：" + query + " 是 Spring AI Alibaba 应用开发中的关键概念。";
    }

}
```

`returnDirect` 适合以下场景：

| 场景             | 说明                   |
| ---------------- | ---------------------- |
| RAG 原始片段返回 | 不希望模型改写检索结果 |
| 文件下载链接     | 工具生成结果后直接返回 |
| 业务查询结果     | 结果已是最终展示格式   |
| 健康检查         | 无需模型总结           |
| 表格 / JSON      | 避免模型破坏结构       |

不建议对需要模型二次分析的工具设置 `returnDirect`。例如“查询订单 + 分析异常原因 + 给出解决建议”这种场景，工具结果应回传给模型继续推理，而不是直接结束 Agent 循环。
