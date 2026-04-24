# Spring AI Alibaba 1.1.2.2 使用文档大纲

依据 Spring AI Alibaba 官方版本页、组件目录和 v1.1.2.2 Release Notes 整理；只保留 Spring AI Alibaba 特有能力，不展开 Spring AI 通用能力。([Spring AI Alibaba][1])

## 1. 版本与依赖管理

### 1.1 版本定位

### 1.2 BOM 依赖管理

### 1.3 核心模块选择

### 1.4 1.1.2.2 升级注意事项

## 2. DashScope / 通义模型生态接入

### 2.1 DashScope Starter 接入

### 2.2 DashScope ChatModel

### 2.3 DashScope 推理模型 Reasoning Content

### 2.4 DashScope 多模态模型

### 2.5 DashScope 图像生成

### 2.6 DashScope 语音能力

## 3. Agent Framework

### 3.1 ReactAgent

### 3.2 Agent 调用方式

### 3.3 Agent 状态管理

### 3.4 Agent 结构化输出

### 3.5 Agent 流式消息

### 3.6 Agent 执行配置

## 4. Tools 增强能力

### 4.1 ToolContext

### 4.2 ToolContextHelper

### 4.3 工具拦截器

### 4.4 工具错误处理

### 4.5 异步工具执行

### 4.6 并行工具执行

### 4.7 Tool returnDirect

## 5. Hooks 与 Interceptors

### 5.1 AgentHook

### 5.2 ModelHook

### 5.3 MessagesModelHook

### 5.4 ModelInterceptor

### 5.5 ToolInterceptor

### 5.6 Flow Agent Hooks

### 5.7 内置 Hook

## 6. Memory 与上下文工程

### 6.1 短期记忆

### 6.2 长期记忆

### 6.3 消息裁剪

### 6.4 消息删除

### 6.5 消息摘要

### 6.6 动态 System Prompt

### 6.7 上下文隔离

### 6.8 上下文传递

## 7. Agent Skills

### 7.1 Skills 核心概念

### 7.2 SKILL.md 规范

### 7.3 FileSystemSkillRegistry

### 7.4 SkillsAgentHook

### 7.5 read_skill 工具

### 7.6 渐进式披露

### 7.7 在 ReactAgent 中使用 Skills

### 7.8 在 Graph 中使用 Skills

### 7.9 SkillPromptAugmentAdvisor

## 8. Multi-agent Patterns

### 8.1 SequentialAgent

### 8.2 ParallelAgent

### 8.3 LlmRoutingAgent

### 8.4 SupervisorAgent

### 8.5 Subagent

### 8.6 Agent Tool

### 8.7 Handoffs

### 8.8 Loop Agent

### 8.9 Custom FlowAgent

### 8.10 多 Agent 并行执行

### 8.11 多 Agent 结果聚合

## 9. Workflow

### 9.1 Graph-based Workflow

### 9.2 RAG Workflow

### 9.3 SQL Agent Workflow

### 9.4 自定义工作流

### 9.5 确定性节点与 Agent 节点混合编排

## 10. RAG 增强

### 10.1 Two-step RAG

### 10.2 Agentic RAG

### 10.3 Hybrid RAG

### 10.4 查询改写

### 10.5 检索校验

### 10.6 答案验证

## 11. Human-in-the-Loop

### 11.1 人工审批

### 11.2 工具调用审批

### 11.3 approve / edit / reject 决策

### 11.4 中断恢复

### 11.5 基于 Checkpointer 的恢复流程

## 12. A2A 分布式智能体

### 12.1 A2A Server

### 12.2 A2A Registry

### 12.3 A2A Discovery

### 12.4 Nacos 注册与发现

### 12.5 A2aRemoteAgent

### 12.6 AgentCard 配置

### 12.7 远程 Agent 调用

## 13. AgentScope 集成

### 13.1 AgentScope Starter

### 13.2 AgentScopeAgent

### 13.3 AgentScope ReActAgent 接入

### 13.4 在 SAA Graph 中编排 AgentScope Agent

## 14. Graph Core

### 14.1 StateGraph

### 14.2 State / Node / Edge

### 14.3 KeyStrategy

### 14.4 状态序列化

### 14.5 条件边

### 14.6 并行条件边

### 14.7 并行分支聚合策略

### 14.8 批量 addEdge

### 14.9 子图

### 14.10 图执行取消

## 15. Graph 持久化

### 15.1 Checkpointer

### 15.2 MemorySaver

### 15.3 RedisSaver

### 15.4 PostgreSqlSaver

### 15.5 MongoDbSaver

### 15.6 ThreadId

### 15.7 StateSnapshot

### 15.8 State History

### 15.9 Time Travel

### 15.10 容错恢复

## 16. Graph Streaming

### 16.1 节点流式输出

### 16.2 Agent 节点流式输出

### 16.3 完整输出事件

### 16.4 流式状态追踪

## 17. Graph MCP 能力

### 17.1 MCP 节点

### 17.2 MCP 工具分配

### 17.3 AgentToolNode

### 17.4 异步 MCP 工具调用

### 17.5 MCP 与 Graph 工作流集成

## 18. Built-in Nodes

### 18.1 LlmNode

### 18.2 AgentNode

### 18.3 ToolNode

### 18.4 AgentToolNode

### 18.5 自定义节点

## 19. Graph 可观测性

### 19.1 Graph Observation Starter

### 19.2 Micrometer 集成

### 19.3 OpenTelemetry 集成

### 19.4 执行链路追踪

### 19.5 节点级监控

## 20. Sandbox

### 20.1 SAA Sandbox 模块

### 20.2 安全隔离执行

### 20.3 工具沙箱运行

### 20.4 Python / Shell 工具执行

## 21. Studio / Agent Chat UI

### 21.1 嵌入式模式

### 21.2 独立运行模式

### 21.3 Agent 调试

### 21.4 Chat UI 配置

### 21.5 DeepResearch 示例接入

## 22. Admin 平台

### 22.1 可视化 Agent 开发

### 22.2 可观测管理

### 22.3 MCP 管理

### 22.4 Flow UI

### 22.5 Admin 部署

## 23. Nacos 集成

### 23.1 A2A Nacos Starter

### 23.2 Config Nacos Starter

### 23.3 动态配置

### 23.4 模型热更新

### 23.5 NacosReactAgentBuilder

## 24. 多模态与语音 Agent

### 24.1 Voice Agent

### 24.2 STT 接入

### 24.3 TTS 接入

### 24.4 实时 WebSocket 语音交互

### 24.5 多模态输入

### 24.6 多模态工具返回

### 24.7 ToolMultimodalResult

## 25. 示例工程

### 25.1 Chatbot

### 25.2 DeepResearch

### 25.3 Documentation Examples

### 25.4 Multi-agent Examples

### 25.5 Graph Examples

### 25.6 A2A Examples

## 26. 生产实践

### 26.1 模块选型

### 26.2 Agent 设计边界

### 26.3 上下文长度控制

### 26.4 多 Agent 拆分原则

### 26.5 持久化策略

### 26.6 工具安全策略

### 26.7 可观测性接入

### 26.8 性能优化

## 27. 常见问题

### 27.1 Spring AI Alibaba 与 Spring AI 的边界

### 27.2 版本兼容问题

### 27.3 DashScope 配置问题

### 27.4 Agent 执行异常

### 27.5 Graph 状态异常

### 27.6 多 Agent 路由异常

### 27.7 Nacos 注册发现异常

[1]: https://java2ai.com/docs/versions "版本说明 | Spring AI Alibaba"


