# Spring AI

## 版本信息

| 组件       | 版本                                     |
| ---------- | ---------------------------------------- |
| JDK        | 21                                       |
| Maven      | 3.9.12                                   |
| SpringBoot | 3.5.10                                   |
| SpringAI   | 1.1.2                                    |
| Model      | OpenAI（DeepSeek、Qwen 兼容 OpenAI API） |



------

## 基础配置

**添加依赖**

```xml
    <properties>
        <spring-ai.version>1.1.2</spring-ai.version>
    </properties>
    <dependencies>
        <!-- Spring AI - OpenAI 依赖 -->
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-starter-model-openai</artifactId>
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
        </dependencies>
    </dependencyManagement>
```

**编辑配置**

免费使用 API Key：[GPT_API_free](https://github.com/chatanywhere/GPT_API_free)

```yaml
---
# Spring AI 配置
spring:
  ai:
    openai:
      base-url: https://api.chatanywhere.tech
      api-key: ${OPENAI_API_KEY}
      chat:
        options:
          model: gpt-4o-mini
```

## 基础使用

**controller创建**

```java
package io.github.atengk.ai.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/ai")
public class BaseChatController {

    private final ChatClient chatClient;

    public BaseChatController(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

}
```

### 最基础的同步对话

```java
/**
 * 最基础的同步对话
 */
@GetMapping("/chat")
public String chat(@RequestParam String message) {
    return chatClient
            .prompt()
            .user(message)
            .call()
            .content();
}
```

GET /api/ai/chat?message=SpringAI是什么？

![image-20260205100433151](./assets/image-20260205100433151.png)

### 流式对话（SSE / WebFlux 场景）

```java
/**
 * 流式对话（SSE / WebFlux 场景）
 */
@GetMapping("/chat/stream")
public Flux<String> stream(@RequestParam String message) {
    return chatClient
            .prompt()
            .user(message)
            .stream()
            .content();
}
```

GET /api/ai/chat/stream?message=SpringAI是什么？

![image-20260205100607964](./assets/image-20260205100607964.png)

### 带 System Prompt 的基础用法

```java
/**
 * 带 System Prompt 的基础用法
 */
@GetMapping("/chat/system")
public String chatWithSystem(
        @RequestParam String system,
        @RequestParam String message) {

    return chatClient
            .prompt()
            .system(system)
            .user(message)
            .call()
            .content();
}
```

GET /api/ai/chat/system?system=你是一个Java专家&message=什么是SpringAI

![image-20260205100749241](./assets/image-20260205100749241.png)

### 使用 Prompt Template 的基础示例

```java
/**
 * 使用 Prompt Template 的基础示例
 */
@GetMapping("/chat/template")
public String chatWithTemplate(
        @RequestParam String topic,
        @RequestParam(defaultValue = "Java") String language) {

    return chatClient
            .prompt()
            .user(u -> u.text("""
                    请用 {language} 的视角，
                    解释一下 {topic}，
                    并给出一个简单示例
                    """)
                    .param("topic", topic)
                    .param("language", language)
            )
            .call()
            .content();
}
```

GET /api/ai/chat/template?topic=SpringAI是什么？

![image-20260205100840340](./assets/image-20260205100840340.png)



## Prompt 与模型参数管理

## 多轮对话与上下文管理

**添加依赖**

```xml
<!-- Spring AI JDBC Chat Memory -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-model-chat-memory-repository-jdbc</artifactId>
</dependency>

<!-- HikariCP 数据源 依赖 -->
<dependency>
    <groupId>com.zaxxer</groupId>
    <artifactId>HikariCP</artifactId>
</dependency>

<!-- MySQL数据库驱动 -->
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
</dependency>
```

**编辑配置**

初始化表结构

```java
spring:
  ai:
    chat:
      memory:
        repository:
          jdbc:
            initialize-schema: always
```

**配置 ChatClientConfig**

```java
package io.github.atengk.ai.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatClientConfig {

    @Bean
    public ChatClient chatClient(
            ChatClient.Builder builder,
            ChatMemory chatMemory) {

        return builder
                .defaultAdvisors(
                        MessageChatMemoryAdvisor
                                .builder(chatMemory)
                                .build()
                )
                .build();
    }

}
```

**创建接口**

```java
package io.github.atengk.ai.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/memory")
@RequiredArgsConstructor
public class MemoryChatController {

    private final ChatClient chatClient;

    @GetMapping("/chat")
    public String chat(
            @RequestParam String conversationId,
            @RequestParam String message) {

        return chatClient
                .prompt()
                .user(message)
                .advisors(a ->
                        a.param(ChatMemory.CONVERSATION_ID, conversationId)
                )
                .call()
                .content();
    }

}
```

**使用接口**

```
GET /api/ai/memory/chat?conversationId=001&message=我叫阿腾
GET /api/ai/memory/chat?conversationId=001&message=我叫什么？
```

![image-20260205173520062](./assets/image-20260205173520062.png)

查看MySQL数据

![image-20260205173602743](./assets/image-20260205173602743.png)



## Tool Calling：让 AI 调用代码

## RAG：接入企业知识库

## 结构化输出与业务集成

## 架构建议与 Controller 分层

### 1️⃣ Spring AI 核心概念

- 模型抽象（Chat / Embedding / Image / Audio）
- Prompt 与 Message 体系
- Token 与上下文窗口
- 结构化输出的意义

------

### 2️⃣ ChatClient 深入使用

- System / User / Assistant Message
- Prompt Template
- 多轮对话
- 上下文管理与 Memory

------

### 3️⃣ 结构化输出

- JSON 输出约束
- 映射为 Java DTO
- 错误处理与校验策略

------

### 4️⃣ Embedding 与向量化

- 什么是 Embedding
- 向量相似度搜索
- 向量数据库选型

------

### 5️⃣ RAG（检索增强生成）

- RAG 架构原理
- 文档切分与索引
- 检索策略
- Prompt 组合方式

------

### 6️⃣ Tool Calling（工具调用）

- 工具定义
- 参数 Schema
- 模型调用流程
- 与业务系统集成模式

------

### 7️⃣ AI 评估与调优

- Evaluator 使用
- 相关性评估
- Prompt 与 RAG 优化思路

------

### 8️⃣ Spring AI 与 Spring 生态集成

- Spring Boot 自动配置
- 配置管理与多环境
- 测试策略
- 可观测性（日志 / 指标）

------

### 9️⃣ 典型应用场景

- 企业知识库
- 智能客服
- AI Copilot
- AI + 微服务架构

------

### 🔟 升级与演进

- 1.x → 2.0 迁移要点
- 模型切换策略
- 架构演进建议

------

