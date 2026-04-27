# Spring AI

## Spring AI 核心体系

### Spring AI 能力边界

### Spring AI 核心抽象

### Model API 体系

### ChatClient 体系

### Vector Store 体系

### Advisor 体系

### RAG 体系

### Tool Calling 体系

### MCP 体系

### Evaluation 体系

### Observability 体系

## 模型接入能力

### Chat Model

### Embedding Model

### Image Model

### Audio Transcription Model

### Text To Speech Model

### Moderation Model

### 模型参数配置

### 模型响应元数据

### 模型原生能力扩展

### 多模型切换策略

## ChatClient

### ChatClient 基础调用

### 同步调用

### 流式调用

### System Message

### User Message

### Assistant Message

### Prompt Template

### Prompt 参数填充

### 默认 System Prompt

### 默认 User Prompt

### ChatResponse 获取

### ChatClientResponse 获取

### Entity 结构化返回

### ResponseEntity 结构化返回

### ChatClient 复用与封装

## Prompt 工程

### Prompt 设计原则

### System Prompt 设计

### User Prompt 设计

### RAG Prompt 设计

### Tool Calling Prompt 设计

### MCP Prompt 设计

### Prompt Template Renderer

### Prompt 模板管理

### Prompt 参数安全处理

### Prompt Injection 防护

### Prompt 版本管理

## 结构化输出

### POJO 输出映射

### Record 输出映射

### 泛型集合输出映射

### StructuredOutputConverter

### BeanOutputConverter

### MapOutputConverter

### ListOutputConverter

### Native Structured Output

### JSON Schema 约束

### 结构化输出异常处理

### 结构化输出重试策略

## Message 与 Media

### Message 类型体系

### UserMessage

### SystemMessage

### AssistantMessage

### ToolResponseMessage

### Message Metadata

### Media 输入

### 多模态消息

### 消息序列构造

### 消息上下文传递

## Chat Memory

### Chat Memory 与 Chat History 区别

### ChatMemory 抽象

### ChatMemoryRepository 抽象

### MessageWindowChatMemory

### InMemoryChatMemoryRepository

### JdbcChatMemoryRepository

### CassandraChatMemoryRepository

### Neo4jChatMemoryRepository

### ConversationId 设计

### 多轮会话上下文管理

### 历史消息窗口控制

### Chat Memory 清理策略

## Advisor

### Advisor 执行机制

### Advisor 链式调用

### Advisor 顺序控制

### Advisor 参数传递

### MessageChatMemoryAdvisor

### PromptChatMemoryAdvisor

### VectorStoreChatMemoryAdvisor

### QuestionAnswerAdvisor

### RetrievalAugmentationAdvisor

### SimpleLoggerAdvisor

### 自定义 Advisor

### Advisor 与 ChatClient 集成

## Embedding

### EmbeddingModel 基础使用

### 文本向量化

### Document 向量化

### 批量 Embedding

### Embedding 维度设计

### Embedding 模型选择

### MetadataMode

### Embedding 异常处理

### Embedding 调用优化

### Embedding 与 Milvus Collection 维度一致性

## Document 数据模型

### Document 内容结构

### Document Metadata

### Document ID 设计

### 文档来源字段

### 文档权限字段

### 文档租户字段

### 文档版本字段

### 文档更新时间字段

### Document 与业务数据关联

### Document 序列化与反序列化

## ETL 数据工程

### ETL Pipeline

### Document Reader

### Document Transformer

### Document Writer

### 文档读取

### 文档清洗

### 文档切分

### 文档增强

### 文档入库

### 文档更新

### 文档删除

### 文档去重

### 入库质量校验

## 文本切分

### TokenTextSplitter

### Chunk 大小设计

### Chunk Overlap 设计

### 长文本切分

### Markdown 文档切分

### 段落级切分

### 元数据继承

### 切分结果质量评估

## Vector Store

### VectorStore 抽象

### Document 写入

### Document 删除

### Similarity Search

### SearchRequest

### TopK 配置

### Similarity Threshold 配置

### Filter Expression

### 元数据过滤

### 检索结果处理

### 向量库可移植 API

## Milvus Vector Store

### MilvusVectorStore

### Milvus 连接配置

### Milvus Collection 设计

### Milvus Database 设计

### Milvus Field 设计

### Embedding Dimension 配置

### Schema 初始化

### Index Type 配置

### Metric Type 配置

### COSINE 相似度

### L2 距离

### IP 内积

### Auto ID 策略

### 业务 ID 策略

### Metadata 字段映射

### MilvusVectorStore Bean 使用

## Milvus 高级检索

### MilvusSearchRequest

### Native Expression

### Search Params JSON

### IVF_FLAT 参数

### nprobe 参数

### Metadata Filter 与 Native Expression 优先级

### 原生 Milvus Client 访问

### 混合条件检索

### 权限过滤检索

### 多租户过滤检索

### 检索性能调优

## RAG 基础

### RAG 基本流程

### 用户问题向量化

### Milvus 相似度检索

### 检索上下文构造

### Prompt 上下文注入

### 模型回答生成

### 来源引用返回

### 无上下文回答策略

### 低置信度回答策略

## RAG Advisor

### QuestionAnswerAdvisor

### QuestionAnswerAdvisor 检索配置

### QuestionAnswerAdvisor Prompt Template

### QuestionAnswerAdvisor 动态过滤

### RetrievalAugmentationAdvisor

### VectorStoreDocumentRetriever

### ContextualQueryAugmenter

### Empty Context 处理

### RAG Advisor 与 ChatClient 集成

## Modular RAG

### Query Transformation

### RewriteQueryTransformer

### CompressionQueryTransformer

### TranslationQueryTransformer

### MultiQueryExpander

### Document Retrieval

### VectorStoreDocumentRetriever

### Document Join

### ConcatenationDocumentJoiner

### Document Post Processing

### Reranking

### Context Compression

### AdvancedTransformer

### CompressionQueryTransformer

### TranslationQueryTransformer

### MultiQueryExpander

### Document Retrieval

### VectorStoreDocumentRetriever

### Document Join

RAG Flow

## 多轮 RAG

### Chat Memory 与 RAG 组合

### 历史问题改写

### 独立问题生成

### 多轮上下文压缩

### ConversationId 与检索过滤

### 会话级知识上下文

### 用户级知识上下文

### 长会话 Token 控制

### 历史消息与检索文档冲突处理

## Tool Calling

### Tool Calling 基础

### @Tool 注解

### Tool 参数描述

### Tool 返回值设计

### Tool Context

### Tool 回调

### Function Callback

### 动态工具注册

### 工具调用选择

### 工具调用异常处理

### 工具调用权限控制

### 工具调用审计日志

### 只读工具与写操作工具边界

## MCP Client

### MCP Client 基础概念

### MCP Client 自动配置

### MCP Client 连接管理

### MCP Client 同步模式

### MCP Client 异步模式

### MCP Client Stdio Transport

### MCP Client SSE Transport

### MCP Client Streamable HTTP Transport

### MCP Server 发现

### MCP Tool Discovery

### MCP Tool Execution

### MCP Resource Access

### MCP Prompt Access

### MCP Roots

### MCP Sampling

### MCP Logging Notification

### MCP Progress Notification

### MCP Elicitation

### MCP Client 超时配置

### MCP Client 错误处理

### MCP Client 与 ChatClient 集成

### MCP Client 与 Tool Calling 集成

## MCP Client Annotations

### MCP Client 注解扫描

### @McpLogging

### @McpSampling

### @McpElicitation

### @McpProgress

### Client Name 绑定

### 多 MCP Client 处理

### 注解处理器配置

### 注解回调异常处理

## MCP Server

### MCP Server 基础概念

### MCP Server 自动配置

### MCP Server Stdio Transport

### MCP Server SSE Transport

### MCP Server Streamable HTTP Transport

### MCP Tool 暴露

### MCP Resource 暴露

### MCP Prompt 暴露

### MCP Completion

### MCP Logging

### MCP Capability Negotiation

### MCP Server 会话管理

### MCP Server 与 Spring Bean 集成

## MCP Server Annotations

### @McpTool

### @McpResource

### @McpPrompt

### @McpComplete

### McpSyncServerExchange

### McpAsyncServerExchange

### McpTransportContext

### McpMeta

### JSON Schema 自动生成

### 注解扫描范围配置

### MCP 注解异常处理

## Function 与 Tool 互通

### Spring AI Function Callback

### MCP Tool 到 Function Callback 转换

### Function Callback 到 MCP Tool 转换

### ToolHelper

### JSON Schema 生成

### 参数类型转换

### 工具返回值转换

### Tool Calling 与 MCP 的边界

## 权限与数据隔离

### Metadata 权限模型

### 用户级数据隔离

### 租户级数据隔离

### 部门级数据隔离

### 文档级权限过滤

### RAG 检索前过滤

### RAG 结果后置校验

### Tool Calling 权限校验

### MCP Tool 权限校验

### 敏感字段脱敏

## Evaluation

### Evaluation 基础

### EvaluationRequest

### EvaluationResponse

### Evaluator

### RelevancyEvaluator

### FactCheckingEvaluator

### RAG 检索质量评估

### RAG 回答相关性评估

### Groundedness 评估

### Hallucination 检测

### 来源引用完整性评估

### 测试问题集设计

### 回归评估

### 模型替换前后评估

## Observability

### ChatClient Observability

### Advisor Observability

### ChatModel Observability

### EmbeddingModel Observability

### ImageModel Observability

### VectorStore Observability

### Tool Calling Observability

### MCP Observability

### Metrics

### Tracing

### Token Usage 统计

### Prompt 日志脱敏

### Completion 日志脱敏

### 检索日志记录

### 调用链路排查

## 错误处理

### 模型调用异常

### 流式响应异常

### 结构化输出异常

### Embedding 调用异常

### VectorStore 写入异常

### Milvus 检索异常

### Advisor 执行异常

### Tool Calling 异常

### MCP Client 连接异常

### MCP Tool 调用异常

### RAG 无结果异常

### Token 超限异常

### 限流与重试

## 性能优化

### ChatClient 调用优化

### 流式响应优化

### Embedding 批量处理

### 文档切分参数优化

### Milvus Index 调优

### Milvus Search Params 调优

### TopK 调优

### Similarity Threshold 调优

### RAG 上下文压缩

### Token 使用优化

### Tool Calling 超时控制

### MCP Client 超时控制

### 并发调用控制

## 典型应用场景

### 普通知识库问答

### 多轮知识库问答

### 带权限的企业知识库问答

### 带来源引用的 RAG 问答

### 文档摘要

### 文档问答

### 业务数据查询助手

### Tool Calling 业务助手

### MCP 外部工具助手

### 结构化信息抽取

### 内容审核

### 语音转文本

### 文本转语音

### 文生图

## 综合案例：Milvus 知识库问答

### 文档数据模型设计

### Milvus Collection 设计

### Metadata 权限字段设计

### 文档入库流程

### 文档更新流程

### 文档删除流程

### 用户问题处理流程

### 相似度检索流程

### RAG Prompt 构造

### 来源引用返回

### 流式回答返回

### 检索质量评估

## 综合案例：MCP 增强型 AI 助手

### MCP Client 连接设计

### MCP Tool 发现流程

### MCP Tool 调用流程

### MCP Resource 读取流程

### MCP Prompt 使用流程

### ChatClient 与 MCP 集成

### Tool Calling 与 MCP 集成

### RAG 与 MCP 组合

### MCP 调用权限控制

### MCP 调用日志记录

### MCP 异常兜底处理

## 综合案例：企业 AI 助手

### 用户意图识别

### 多轮会话管理

### Milvus 知识库检索

### Tool Calling 业务查询

### MCP 外部系统访问

### 结构化输出

### 权限过滤

### 来源引用

### 流式响应

### 调用链路观测

### 效果评估与优化
