# Spring AI 2.x MCP 开发

本文用于说明 Spring AI 2.x 中 MCP 的基础概念、适用范围和开发边界。MCP 全称为 Model Context Protocol，是一种让 AI 模型以标准化方式访问外部工具、资源和上下文的协议；Spring AI 通过 MCP Boot Starters、MCP Java SDK 和注解模型提供 MCP Client 与 MCP Server 两侧的开发支持。([Home](https://docs.spring.io/spring-ai/reference/2.0-SNAPSHOT/api/mcp/mcp-overview.html))

## 文档概述

本章节用于说明本文档的编写目标、适用版本和技术范围，帮助开发人员在正式进入 MCP Server、MCP Client 和大模型集成开发前，先明确整体开发边界。

### 文档目标

本文档目标是指导开发人员基于 Spring AI 2.x 完成 MCP 相关功能开发，包括 MCP 基础概念理解、服务端能力暴露、客户端能力调用，以及与 Spring AI 大模型调用链路的集成。

通过本文档，开发人员应能够理解以下内容：

1. MCP 在大模型应用中的定位。
2. MCP Client 与 MCP Server 的职责划分。
3. Tools、Resources、Prompts 三类核心能力的用途。
4. Spring AI 2.x 中 MCP 相关 Starter 的使用方向。
5. MCP 能力如何接入 ChatClient 和 Tool Calling 调用链路。
6. MCP 功能如何在业务系统中进行封装、配置和验证。

本文档不以协议规范翻译为目标，而是面向 Java 与 Spring Boot 工程实践，重点说明如何在实际项目中组织依赖、配置、代码结构和调用流程。

### 适用版本

本文适用于 Spring AI 2.x MCP 开发场景。根据 Spring AI 2.0.0-SNAPSHOT 文档，Spring AI 2.0 仍属于开发版本，同时官方页面列出了 2.0.0-M6 作为 Preview 版本，因此实际项目应固定使用明确的 Spring AI 版本，并以对应版本的 Reference 文档为准。([Home](https://docs.spring.io/spring-ai/reference/2.0-SNAPSHOT/api/mcp/mcp-overview.html))

建议技术基线如下：

| 技术项       | 建议版本                    | 说明                                                      |
| ------------ | --------------------------- | --------------------------------------------------------- |
| JDK          | 17 或更高                   | 适配 Spring Boot 3.x / 4.x 项目基线                       |
| Spring Boot  | 3.x / 4.x                   | 根据 Spring AI 2.x 依赖要求选择                           |
| Spring AI    | 2.x                         | 使用 2.0.0-M 系列或 2.0.0-SNAPSHOT 时需关注变更           |
| MCP Java SDK | 1.0.x                       | Spring AI 2.0 文档说明其 MCP Java SDK 依赖升级到 1.0.x 线 |
| 构建工具     | Maven / Gradle              | 推荐通过 Spring AI BOM 统一管理依赖版本                   |
| 通信方式     | STDIO、SSE、Streamable HTTP | 根据本地进程、远程服务、Servlet 或 WebFlux 场景选择       |

Spring AI 2.0 对 MCP 相关传输实现有重要变化：部分 WebFlux、WebMVC 相关 MCP transport artifact 从 `io.modelcontextprotocol.sdk` 迁移到 `org.springframework.ai`，如果项目直接引用这些 transport 类，需要同步调整依赖坐标和 import；如果只使用 Spring AI Starter 和自动配置，通常只需要更新依赖配置。([Home](https://docs.spring.io/spring-ai/reference/2.0-SNAPSHOT/api/mcp/mcp-overview.html))

### 技术范围

本文覆盖 Spring AI 2.x MCP 开发中的基础能力和工程落地内容，主要包括 MCP 基础概念、MCP Server 开发、MCP Client 开发、大模型集成、配置管理、业务封装和功能验证。

本文涉及的主要技术范围如下：

| 范围            | 内容                                                         |
| --------------- | ------------------------------------------------------------ |
| MCP 基础概念    | MCP 协议定位、Client 与 Server 角色、Tools、Resources、Prompts |
| MCP Server 开发 | Tool 定义、Resource 暴露、Prompt 定义、启动配置              |
| MCP Client 开发 | Server 连接配置、Tool 调用、Resource 读取、Prompt 使用       |
| 大模型集成      | ChatClient 接入、Tool Calling、上下文传递、结果处理          |
| 配置说明        | application 配置、多 MCP Server、超时与连接参数              |
| 业务封装        | MCP 服务封装、业务方法设计、请求响应结构、异常处理           |
| 功能验证        | Server 启动验证、Client 调用验证、大模型联调验证             |

本文不重点展开以下内容：

| 不包含内容           | 说明                                                        |
| -------------------- | ----------------------------------------------------------- |
| MCP 协议底层完整实现 | Spring AI 已通过 MCP Java SDK 和 Starter 屏蔽大部分底层细节 |
| 大模型私有化部署     | 本文只关注 Spring AI 与 MCP 的调用链路                      |
| 复杂 Agent 编排框架  | 本文只说明 MCP 与 ChatClient、Tool Calling 的基础集成       |
| 前端交互页面         | 默认以后端服务、接口调用和测试验证为主                      |

Spring AI 在 MCP 集成中提供 Client Starters 和 Server Starters，其中 Client 侧包含 `spring-ai-starter-mcp-client`、`spring-ai-starter-mcp-client-webflux`，Server 侧包含 STDIO、WebMVC、WebFlux 等不同传输方式对应的 Starter。([Home](https://docs.spring.io/spring-ai/reference/2.0-SNAPSHOT/api/mcp/mcp-overview.html))

## MCP 基础概念

本章节用于说明 MCP 的核心概念。理解这些概念后，后续开发 MCP Server 和 MCP Client 时可以更清楚地判断哪些能力应该设计成工具，哪些内容应该设计成资源，哪些提示词流程应该封装成 Prompt。

### MCP 协议定位

MCP 是大模型应用与外部系统之间的标准化连接协议。它用于让 AI 模型通过统一接口访问数据库、API、文件系统、业务系统和其他外部服务，从而避免每个系统都单独设计一套非标准的模型接入方式。([Home](https://docs.spring.io/spring-ai/reference/2.0-SNAPSHOT/api/mcp/mcp-overview.html))

在一个典型的大模型应用中，MCP 位于模型调用链路和外部业务系统之间，可以理解为“大模型能力接入层”。

整体关系如下：

| 层级           | 作用                                                  |
| -------------- | ----------------------------------------------------- |
| 大模型         | 理解用户意图，生成调用计划，组织最终回答              |
| Spring AI 应用 | 负责 ChatClient、Prompt、Tool Calling、业务流程编排   |
| MCP Client     | 连接 MCP Server，发现并调用 Tools、Resources、Prompts |
| MCP Server     | 将业务能力、数据资源、提示词模板暴露给客户端          |
| 外部系统       | 数据库、文件系统、HTTP API、知识库、内部业务服务等    |

MCP 不替代 HTTP、RPC、数据库驱动或消息队列。它的重点是为大模型应用提供统一的能力发现、能力描述和能力调用协议。对于 Spring AI 项目来说，MCP 的价值在于让业务系统可以通过标准方式向模型暴露能力，同时让模型应用可以通过标准客户端接入多个外部能力提供方。

Spring AI 的 MCP Java SDK 架构分为 Client/Server 层、Session 层和 Transport 层。Client/Server 层处理应用逻辑和协议操作，Session 层管理通信状态，Transport 层负责消息传输和 JSON-RPC 序列化。([Home](https://docs.spring.io/spring-ai/reference/2.0-SNAPSHOT/api/mcp/mcp-overview.html))

### Client 与 Server 角色

MCP 采用 Client 与 Server 的角色模型。MCP Server 负责提供能力，MCP Client 负责连接 Server 并消费这些能力。两者通过协议初始化、能力协商、工具发现、资源读取和提示词交互完成通信。

MCP Client 的主要职责如下：

| 职责         | 说明                                                   |
| ------------ | ------------------------------------------------------ |
| 建立连接     | 通过 STDIO、SSE、Streamable HTTP 等方式连接 MCP Server |
| 协议协商     | 与 Server 完成协议版本和能力协商                       |
| 工具发现     | 获取 Server 暴露的 Tools 列表                          |
| 工具调用     | 根据模型或业务流程需要调用指定 Tool                    |
| 资源访问     | 读取 Server 暴露的 Resource 内容                       |
| Prompt 交互  | 获取 Server 提供的 Prompt 模板                         |
| 生命周期管理 | 处理连接初始化、关闭、异常和资源清理                   |

Spring AI MCP Client Boot Starter 支持同步和异步客户端实现，支持多个命名 transport，并可以与 Spring AI 的工具执行框架集成。启用工具回调后，MCP Server 中注册的 Tools 可以通过 `ToolCallbackProvider` 提供给 Spring AI 调用链路。([Home](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-client-boot-starter-docs.html?utm_source=chatgpt.com))

MCP Server 的主要职责如下：

| 职责         | 说明                                        |
| ------------ | ------------------------------------------- |
| 暴露工具     | 将业务方法、查询能力、外部接口封装为 Tool   |
| 暴露资源     | 将文件、文档、配置、知识内容封装为 Resource |
| 暴露提示词   | 将可复用的提示词模板封装为 Prompt           |
| 执行业务逻辑 | 接收客户端请求，执行业务逻辑并返回结果      |
| 能力声明     | 向客户端声明当前 Server 支持的协议能力      |
| 安全控制     | 对参数、权限、数据范围和敏感操作进行校验    |

Spring AI MCP Server 支持 STDIO、Streamable HTTP、Stateless Streamable HTTP、SSE 等传输方式，并支持同步与异步 API。服务端可通过注解方式声明 `@McpTool`、`@McpResource`、`@McpPrompt` 等能力，减少底层协议代码。([Home](https://docs.spring.io/spring-ai/reference/2.0-SNAPSHOT/api/mcp/mcp-overview.html))

### Tools、Resources 与 Prompts

Tools、Resources 和 Prompts 是 MCP Server 暴露给 MCP Client 的三类核心能力。它们分别对应“可执行动作”“可读取上下文”和“可复用提示词模板”。

三者区别如下：

| 类型      | 核心作用         | 是否执行操作 | 典型场景                                   |
| --------- | ---------------- | ------------ | ------------------------------------------ |
| Tools     | 暴露可执行函数   | 是           | 查询订单、创建工单、调用搜索接口、执行计算 |
| Resources | 暴露可读取上下文 | 通常否       | 读取文档、配置、文件、知识库内容           |
| Prompts   | 暴露提示词模板   | 通常否       | 代码审查模板、故障排查模板、SQL 优化模板   |

Tools 适合封装“动作”。当大模型需要调用外部能力完成某个任务时，通常会使用 Tool。例如查询用户信息、检索商品、创建任务、调用天气接口、执行运维检查等。Tool 应该具备明确的名称、描述、输入参数和返回结构，这些信息会影响模型是否能够在合适场景下选择正确工具。

Resources 适合封装“上下文”。当大模型需要读取外部资料、文档、配置或知识内容时，可以通过 Resource 获取。Resource 通常用于只读场景，不建议承载高风险写操作。例如读取接口说明、系统配置、业务规则、帮助文档、知识库片段等。

Prompts 适合封装“提示词流程”。当某类任务具有固定提示词结构时，可以将其定义为 Prompt。例如代码审查 Prompt、问题排查 Prompt、数据库优化 Prompt、接口文档生成 Prompt 等。这样客户端可以按名称获取模板，并根据参数生成最终发送给模型的消息内容。

在 Spring AI 2.x 中，可以将三类能力对应到以下开发对象：

| MCP 对象 | Spring AI 开发关注点                              |
| -------- | ------------------------------------------------- |
| Tool     | Java 方法、参数 DTO、返回对象、异常处理、工具描述 |
| Resource | URI、MIME 类型、读取逻辑、权限控制、缓存策略      |
| Prompt   | 模板名称、变量参数、消息结构、适用业务场景        |

实际开发时可以遵循以下设计原则：需要模型触发业务动作的能力设计为 Tool；只需要提供上下文的数据设计为 Resource；具有固定任务模板和复用价值的提示词设计为 Prompt。这样可以保持 MCP Server 的职责清晰，也便于客户端做工具过滤、权限控制和调用审计。

## 开发环境准备

本章节用于说明 Spring AI 2.x MCP 项目的基础开发环境，包括 JDK、Spring Boot、Spring AI BOM、MCP Starter 依赖以及推荐的工程目录结构。Spring AI 2.x 当前属于 Preview / Snapshot 线，官方文档中列出了 `2.0.0-M6` 作为 Preview 版本，`2.0.0-SNAPSHOT` 作为 Snapshot 版本，因此实际项目中建议固定明确版本，不建议在生产环境中直接使用浮动快照版本。([Home](https://docs.spring.io/spring-ai/reference/2.0-SNAPSHOT/api/mcp/mcp-overview.html))

### JDK 与 Spring Boot 版本要求

Spring AI 项目应基于 Spring Boot 3.x 技术栈开发。当前 Spring AI 官方 Getting Started 文档说明 Spring AI 支持 Spring Boot `3.4.x` 和 `3.5.x`，因此 Spring AI 2.x MCP 项目建议优先选择 Spring Boot `3.5.x`，如需兼容已有项目，也可以使用 Spring Boot `3.4.x`。([Home](https://docs.spring.io/spring-ai/reference/getting-started.html?utm_source=chatgpt.com))

推荐版本如下：

| 环境项      | 推荐版本                    | 说明                                      |
| ----------- | --------------------------- | ----------------------------------------- |
| JDK         | 17 或更高                   | Spring Boot 3.x 基线环境，推荐使用 JDK 21 |
| Spring Boot | 3.4.x / 3.5.x               | 建议新项目使用 3.5.x                      |
| Spring AI   | 2.0.0-M6 / 2.0.0-SNAPSHOT   | 推荐优先使用明确的 Milestone 版本         |
| Maven       | 3.9.x 或更高                | 用于构建 Spring Boot 项目                 |
| Gradle      | 8.x 或更高                  | 如项目使用 Gradle 构建                    |
| IDE         | IntelliJ IDEA 2024.x 或更高 | 需支持 Java 17+ 和 Spring Boot 3.x        |

开发前可以先检查本地 JDK 与 Maven 版本。

```bash
# 查看 JDK 版本，建议使用 17 或 21
java -version

# 查看 Maven 版本，建议使用 3.9.x 或更高
mvn -version
```

如果项目使用 JDK 21，`pom.xml` 中可以统一声明 Java 版本。

```xml
<properties>
    <!-- Java 编译版本，建议 Spring AI 2.x 新项目使用 JDK 21 -->
    <java.version>21</java.version>

    <!-- Spring AI 2.x 版本，生产项目建议固定为明确版本 -->
    <spring-ai.version>2.0.0-M6</spring-ai.version>
</properties>
```

### Spring AI 依赖配置

Spring AI 官方建议通过 `spring-ai-bom` 统一管理 Spring AI 相关依赖版本。BOM 只负责依赖版本管理，不替代 Spring Boot Parent，也不直接引入具体模块；具体使用 OpenAI、Ollama、Anthropic、MCP 等能力时，仍然需要按需引入对应 Starter。([Home](https://docs.spring.io/spring-ai/reference/getting-started.html))

以下配置放在项目根目录 `pom.xml` 中，用于统一管理 Spring Boot 与 Spring AI 版本。

```xml
<project>
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <!-- 使用 Spring Boot Parent 管理 Spring Boot、插件和常见依赖版本 -->
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.5.0</version>
        <relativePath/>
    </parent>

    <groupId>io.github.atengk</groupId>
    <artifactId>spring-ai-mcp-demo</artifactId>
    <version>1.0.0</version>
    <name>spring-ai-mcp-demo</name>
    <description>Spring AI 2.x MCP 示例工程</description>

    <properties>
        <!-- Java 编译版本 -->
        <java.version>21</java.version>

        <!-- Spring AI 版本，按实际项目锁定 -->
        <spring-ai.version>2.0.0-M6</spring-ai.version>

        <!-- Hutool 工具包版本，用于常见字符串、集合、JSON、日期等工具处理 -->
        <hutool.version>5.8.36</hutool.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <!-- Spring AI BOM 统一管理 Spring AI 相关依赖版本 -->
                <groupId>org.springframework.ai</groupId>
                <artifactId>spring-ai-bom</artifactId>
                <version>${spring-ai.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>
</project>
```

如果使用 `2.0.0-SNAPSHOT`，需要额外配置 Snapshot 仓库；如果使用正式 Release 或已发布的 Milestone 版本，通常直接使用 Maven Central 即可。Spring AI 官方文档说明 1.0.0 及之后版本可从 Maven Central 获取，Snapshot 版本需要额外配置 Snapshot 仓库。([Home](https://docs.spring.io/spring-ai/reference/getting-started.html?utm_source=chatgpt.com))

```xml
<repositories>
    <repository>
        <!-- Spring Snapshot 仓库，仅在使用 SNAPSHOT 版本时需要 -->
        <id>spring-snapshots</id>
        <name>Spring Snapshots</name>
        <url>https://repo.spring.io/snapshot</url>
        <releases>
            <enabled>false</enabled>
        </releases>
    </repository>

    <repository>
        <!-- Central Portal Snapshot 仓库，仅在使用 SNAPSHOT 版本时需要 -->
        <id>central-portal-snapshots</id>
        <name>Central Portal Snapshots</name>
        <url>https://central.sonatype.com/repository/maven-snapshots/</url>
        <releases>
            <enabled>false</enabled>
        </releases>
        <snapshots>
            <enabled>true</enabled>
        </snapshots>
    </repository>
</repositories>
```

基础依赖可以先引入 Web、Validation、Lombok、Hutool 和具体大模型 Starter。下面以 OpenAI 接入为例，实际项目可以替换为 Ollama、Anthropic、Azure OpenAI、DeepSeek 等模型 Starter。

```xml
<dependencies>
    <dependency>
        <!-- Spring Web，用于提供 HTTP API、MCP WebMVC Server 或业务接口 -->
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <dependency>
        <!-- 参数校验，用于 Controller、DTO、Tool 入参校验 -->
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <dependency>
        <!-- Spring AI OpenAI 模型 Starter，按实际模型供应商替换 -->
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-starter-model-openai</artifactId>
    </dependency>

    <dependency>
        <!-- Hutool 工具包，用于字符串、集合、JSON、日期等通用处理 -->
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>${hutool.version}</version>
    </dependency>

    <dependency>
        <!-- Lombok，用于简化 DTO、VO、配置类代码 -->
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <dependency>
        <!-- Spring Boot 测试依赖，用于 MCP Client、MCP Server 和业务服务测试 -->
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

基础模型配置可以先放在 `src/main/resources/application.yml` 中。这里仅给出最小配置，MCP Client 与 MCP Server 的完整配置会在后续章节展开。

```yaml
server:
  # 当前 Spring Boot 应用端口
  port: 8080

spring:
  application:
    # 应用名称，用于日志、监控和 MCP 标识
    name: spring-ai-mcp-demo

  ai:
    openai:
      # OpenAI API Key，建议通过环境变量注入，避免写死在配置文件中
      api-key: ${OPENAI_API_KEY}
      chat:
        options:
          # 默认聊天模型，按实际模型供应商和账号权限调整
          model: gpt-4o-mini
          # 采样温度，业务场景通常建议设置为较低值
          temperature: 0.2
```

### MCP 相关依赖配置

Spring AI 2.x 提供 MCP Client Starter 和 MCP Server Starter。Client 侧可以使用标准 Client Starter 或 WebFlux Client Starter；Server 侧可以按传输方式选择 STDIO、WebMVC 或 WebFlux Starter。官方文档说明，`spring-ai-starter-mcp-client` 支持 STDIO、Servlet Streamable HTTP、Stateless Streamable HTTP 和 SSE；`spring-ai-starter-mcp-client-webflux` 支持 WebFlux 传输实现。([Home](https://docs.spring.io/spring-ai/reference/2.0-SNAPSHOT/api/mcp/mcp-overview.html))

MCP Client 依赖适用于“当前应用需要连接外部 MCP Server”的场景。

```xml
<dependencies>
    <dependency>
        <!-- MCP Client 标准 Starter，支持 STDIO、SSE、Streamable HTTP 等连接方式 -->
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-starter-mcp-client</artifactId>
    </dependency>
</dependencies>
```

如果当前项目是响应式 WebFlux 项目，或生产环境需要使用 WebFlux 方式连接 SSE / Streamable HTTP MCP Server，可以使用 WebFlux Client Starter。Spring AI MCP Client 文档也建议生产部署中优先考虑 WebFlux-based SSE 和 Streamable HTTP 连接。([Home](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-client-boot-starter-docs.html?utm_source=chatgpt.com))

```xml
<dependencies>
    <dependency>
        <!-- MCP Client WebFlux Starter，适用于响应式项目或 WebFlux 传输场景 -->
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-starter-mcp-client-webflux</artifactId>
    </dependency>
</dependencies>
```

MCP Server 依赖适用于“当前应用需要向外暴露 MCP Tools、Resources、Prompts”的场景。STDIO 模式常用于本地进程集成，WebMVC / WebFlux 模式常用于服务化部署。Spring AI MCP 概览文档列出了 STDIO、WebMVC、WebFlux 三类 Server Starter 及其对应协议配置。([Home](https://docs.spring.io/spring-ai/reference/2.0-SNAPSHOT/api/mcp/mcp-overview.html))

```xml
<dependencies>
    <dependency>
        <!-- MCP Server STDIO Starter，适用于本地进程方式暴露 MCP Server -->
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-starter-mcp-server</artifactId>
    </dependency>
</dependencies>
<dependencies>
    <dependency>
        <!-- MCP Server WebMVC Starter，适用于基于 Spring MVC 的 HTTP MCP Server -->
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-starter-mcp-server-webmvc</artifactId>
    </dependency>
</dependencies>
<dependencies>
    <dependency>
        <!-- MCP Server WebFlux Starter，适用于响应式 HTTP MCP Server -->
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-starter-mcp-server-webflux</artifactId>
    </dependency>
</dependencies>
```

如果同一个工程同时作为 MCP Client 和 MCP Server，可以同时引入 Client 与 Server Starter。但生产项目中更推荐拆分为独立模块或独立服务，避免模型调用应用和工具暴露应用职责过重。

```xml
<dependencies>
    <dependency>
        <!-- MCP Client：连接外部 MCP Server 并消费工具能力 -->
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-starter-mcp-client</artifactId>
    </dependency>

    <dependency>
        <!-- MCP Server：通过 WebMVC 暴露当前系统的 MCP 能力 -->
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-starter-mcp-server-webmvc</artifactId>
    </dependency>
</dependencies>
```

Spring AI 2.0 对 MCP transport 依赖有迁移变化：`mcp-spring-webflux` 和 `mcp-spring-webmvc` 已从 MCP Java SDK 迁移到 Spring AI 项目本身，依赖 groupId 从 `io.modelcontextprotocol.sdk` 调整为 `org.springframework.ai`；如果使用 Spring AI Starter 和 BOM 管理依赖，通常不需要手动指定这些底层 transport 依赖版本。([Home](https://docs.spring.io/spring-ai/reference/2.0-SNAPSHOT/api/mcp/mcp-overview.html))

Spring AI 2.0 还要求 MCP Java SDK 使用 `1.0.0 RC1` 或更高版本，项目如果显式声明 MCP Java SDK 版本，需要同步升级到 1.0.x 线；如果完全依赖 Spring AI Starter 自动配置，只需要更新 `pom.xml` 或 `build.gradle` 中的依赖坐标即可。([Home](https://docs.spring.io/spring-ai/reference/2.0-SNAPSHOT/api/mcp/mcp-overview.html))

### 项目结构说明

Spring AI MCP 项目可以采用单体结构，也可以采用多模块结构。学习、验证和小型项目可以使用单模块；生产项目建议将 MCP Client、MCP Server、业务领域服务和公共模型拆分清楚，避免后续扩展时出现职责混乱。

单模块结构适合快速验证 MCP Server、MCP Client 和 ChatClient 集成流程。

```text
spring-ai-mcp-demo
├── pom.xml
├── src
│   ├── main
│   │   ├── java
│   │   │   └── io
│   │   │       └── github
│   │   │           └── atengk
│   │   │               └── mcp
│   │   │                   ├── SpringAiMcpApplication.java
│   │   │                   ├── config
│   │   │                   │   ├── ChatClientConfig.java
│   │   │                   │   └── McpConfig.java
│   │   │                   ├── controller
│   │   │                   │   └── ChatController.java
│   │   │                   ├── mcp
│   │   │                   │   ├── client
│   │   │                   │   │   └── McpClientService.java
│   │   │                   │   └── server
│   │   │                   │       ├── tool
│   │   │                   │       │   └── WeatherTool.java
│   │   │                   │       ├── resource
│   │   │                   │       │   └── DocumentResource.java
│   │   │                   │       └── prompt
│   │   │                   │           └── ReviewPrompt.java
│   │   │                   ├── service
│   │   │                   │   ├── ChatService.java
│   │   │                   │   └── impl
│   │   │                   │       └── ChatServiceImpl.java
│   │   │                   ├── model
│   │   │                   │   ├── dto
│   │   │                   │   ├── vo
│   │   │                   │   └── entity
│   │   │                   └── common
│   │   │                       ├── exception
│   │   │                       └── result
│   │   └── resources
│   │       ├── application.yml
│   │       └── mcp-servers.json
│   └── test
│       └── java
│           └── io
│               └── github
│                   └── atengk
│                       └── mcp
│                           ├── McpClientTests.java
│                           └── McpServerTests.java
```

主要目录职责如下：

| 目录                         | 说明                                               |
| ---------------------------- | -------------------------------------------------- |
| `config`                     | Spring AI、ChatClient、MCP 相关 Bean 配置          |
| `controller`                 | 对外提供 HTTP 调试接口或业务 API                   |
| `mcp/client`                 | MCP Client 调用封装，负责连接和调用外部 MCP Server |
| `mcp/server/tool`            | MCP Tool 定义，封装可执行业务能力                  |
| `mcp/server/resource`        | MCP Resource 定义，封装只读资源内容                |
| `mcp/server/prompt`          | MCP Prompt 定义，封装可复用提示词模板              |
| `service`                    | 业务服务层，隔离 Controller、MCP 和模型调用逻辑    |
| `model/dto`                  | 请求参数对象                                       |
| `model/vo`                   | 响应视图对象                                       |
| `common`                     | 通用返回结构、异常、常量、工具类                   |
| `resources/application.yml`  | Spring Boot、Spring AI、MCP 配置                   |
| `resources/mcp-servers.json` | STDIO MCP Server 外部连接配置                      |

多模块结构适合生产项目，尤其适合一个系统既要暴露 MCP Server，又要作为 MCP Client 调用外部工具的场景。

```text
spring-ai-mcp-platform
├── pom.xml
├── mcp-common
│   ├── pom.xml
│   └── src/main/java/io/github/atengk/mcp/common
│       ├── exception
│       ├── result
│       └── util
├── mcp-server
│   ├── pom.xml
│   └── src/main/java/io/github/atengk/mcp/server
│       ├── tool
│       ├── resource
│       ├── prompt
│       └── config
├── mcp-client
│   ├── pom.xml
│   └── src/main/java/io/github/atengk/mcp/client
│       ├── config
│       ├── service
│       └── adapter
├── ai-chat-service
│   ├── pom.xml
│   └── src/main/java/io/github/atengk/mcp/chat
│       ├── controller
│       ├── service
│       └── model
└── mcp-test
    ├── pom.xml
    └── src/test/java/io/github/atengk/mcp/test
```

多模块职责建议如下：

| 模块              | 职责                                  |
| ----------------- | ------------------------------------- |
| `mcp-common`      | 公共 DTO、VO、异常、返回结构、工具类  |
| `mcp-server`      | 暴露 MCP Tools、Resources、Prompts    |
| `mcp-client`      | 连接外部 MCP Server，封装调用逻辑     |
| `ai-chat-service` | 集成 ChatClient、大模型调用和业务接口 |
| `mcp-test`        | 编写联调测试、端到端测试和示例用例    |

开发环境准备完成后，项目至少应满足以下条件：

1. JDK、Maven 或 Gradle 版本可正常构建 Spring Boot 3.x 项目。
2. `spring-ai-bom` 已统一管理 Spring AI 依赖版本。
3. 已根据项目角色选择 MCP Client、MCP Server 或两者的 Starter。
4. 已完成基础 `application.yml` 配置。
5. 已建立清晰的 `mcp/client`、`mcp/server/tool`、`mcp/server/resource`、`mcp/server/prompt` 目录。
6. 如果使用 `2.0.0-SNAPSHOT`，已正确配置 Snapshot 仓库并检查 Maven 镜像规则。

## MCP Server 开发

本章节用于说明 MCP Server 的开发方式。MCP Server 的核心职责是把当前系统中的业务能力、只读资源和提示词模板暴露给 MCP Client，使大模型应用可以通过标准协议发现和调用这些能力。Spring AI MCP Server Boot Starter 支持工具、资源、提示词的自动配置，也支持 STDIO、SSE、Streamable HTTP、Stateless Streamable HTTP 等多种服务端协议形态。官方文档说明，MCP Server Boot Starter 会自动配置 tools、resources、prompts，并支持基于注解的服务端开发与自动扫描注册。([Home](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-server-boot-starter-docs.html))

### 服务端功能定位

MCP Server 不直接负责“大模型对话”，它更适合定位为“大模型可调用能力提供方”。在实际项目中，MCP Server 通常封装已有业务服务，例如订单查询、文档读取、配置查询、故障排查、数据分析、运维检查等能力，然后通过 MCP 协议暴露给 MCP Client。

MCP Server 的职责边界如下：

| 职责          | 说明                                       |
| ------------- | ------------------------------------------ |
| 暴露 Tool     | 将可执行的业务动作封装为模型可调用工具     |
| 暴露 Resource | 将只读数据、文档、配置、知识内容封装为资源 |
| 暴露 Prompt   | 将可复用提示词模板封装为标准 Prompt        |
| 参数校验      | 对工具入参、资源 URI、Prompt 参数进行校验  |
| 权限控制      | 控制哪些数据和操作可以被 MCP Client 访问   |
| 结果封装      | 返回结构化、可解释、可被模型继续处理的结果 |
| 运行治理      | 处理日志、异常、超时、审计和连接协议配置   |

Spring AI MCP Server 默认支持同步和异步两类服务端模式。同步模式使用 `McpSyncServer`，适合传统阻塞式业务调用；异步模式使用 `McpAsyncServer`，适合 WebFlux、响应式数据库、流式处理等非阻塞场景。同步服务端只会注册同步方法，异步服务端只会注册 Reactor 返回类型的方法，方法类型不匹配时会被过滤。([Home](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-server-boot-starter-docs.html))

在普通 Spring Boot 业务项目中，建议优先使用同步模式：

```yaml
spring:
  ai:
    mcp:
      server:
        # 同步服务端，适合传统 Spring MVC 和阻塞式业务服务
        type: SYNC
        annotation-scanner:
          # 使用 @McpTool、@McpResource、@McpPrompt 注解自动注册能力
          enabled: true
```

MCP Server 能力设计建议如下：

| 能力类型 | 适合封装的内容                             | 不建议封装的内容                         |
| -------- | ------------------------------------------ | ---------------------------------------- |
| Tool     | 查询、计算、创建任务、调用业务接口         | 无权限控制的删除、支付、审批等高风险操作 |
| Resource | 文档、配置、只读业务规则、知识库内容       | 需要复杂事务处理或写入的数据操作         |
| Prompt   | 代码审查、故障分析、SQL 优化、文档生成模板 | 一次性、不复用的临时提示词               |

### Tool 定义与注册

Tool 用于暴露可执行能力。Spring AI MCP Server 注解模型提供 `@McpTool` 和 `@McpToolParam`，可以将普通 Spring Bean 方法声明为 MCP Tool，并自动生成参数 Schema。官方文档说明，`@McpTool` 用于标记 MCP 工具实现方法，`@McpToolParam` 用于描述工具参数。([Home](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-annotations-server.html))

下面示例定义一个订单查询 Tool，用于根据订单号查询订单摘要信息。实际项目中应将 Tool 方法保持轻量，把核心业务逻辑放到 Service 层。

文件位置：`src/main/java/io/github/atengk/mcp/server/tool/OrderTool.java`

```java
package io.github.atengk.mcp.server.tool;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.mcp.server.model.vo.OrderSummaryVO;
import io.github.atengk.mcp.server.service.OrderQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

/**
 * 订单 MCP 工具。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTool {

    private final OrderQueryService orderQueryService;

    /**
     * 根据订单号查询订单摘要。
     *
     * @param orderNo 订单号
     * @return 订单摘要
     */
    @McpTool(
            name = "order_query_summary",
            description = "根据订单号查询订单摘要信息，返回订单状态、金额、收货人和创建时间",
            annotations = @McpTool.McpAnnotations(
                    title = "订单摘要查询",
                    readOnlyHint = true,
                    destructiveHint = false,
                    idempotentHint = true
            )
    )
    public OrderSummaryVO queryOrderSummary(
            @McpToolParam(description = "订单号，例如：ORD202605110001", required = true)
            String orderNo) {

        if (StrUtil.isBlank(orderNo)) {
            log.warn("MCP订单查询失败，订单号为空");
            throw new IllegalArgumentException("订单号不能为空");
        }

        log.info("开始执行MCP订单摘要查询，订单号：{}", orderNo);
        return orderQueryService.querySummary(orderNo);
    }
}
```

文件位置：`src/main/java/io/github/atengk/mcp/server/service/OrderQueryService.java`

```java
package io.github.atengk.mcp.server.service;

import io.github.atengk.mcp.server.model.vo.OrderSummaryVO;

/**
 * 订单查询服务。
 *
 * @author Ateng
 * @since 2026-05-11
 */
public interface OrderQueryService {

    /**
     * 查询订单摘要。
     *
     * @param orderNo 订单号
     * @return 订单摘要
     */
    OrderSummaryVO querySummary(String orderNo);
}
```

文件位置：`src/main/java/io/github/atengk/mcp/server/service/impl/OrderQueryServiceImpl.java`

```java
package io.github.atengk.mcp.server.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.mcp.server.model.vo.OrderSummaryVO;
import io.github.atengk.mcp.server.service.OrderQueryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 订单查询服务实现。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Service
public class OrderQueryServiceImpl implements OrderQueryService {

    /**
     * 查询订单摘要。
     *
     * @param orderNo 订单号
     * @return 订单摘要
     */
    @Override
    public OrderSummaryVO querySummary(String orderNo) {
        if (StrUtil.isBlank(orderNo)) {
            throw new IllegalArgumentException("订单号不能为空");
        }

        log.info("查询订单摘要数据，订单号：{}", orderNo);

        // 示例数据，实际项目中应替换为数据库或业务接口查询
        return OrderSummaryVO.builder()
                .orderNo(orderNo)
                .status("PAID")
                .amount(new BigDecimal("199.90"))
                .receiverName("张三")
                .createdTime(DateUtil.now())
                .build();
    }
}
```

文件位置：`src/main/java/io/github/atengk/mcp/server/model/vo/OrderSummaryVO.java`

```java
package io.github.atengk.mcp.server.model.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单摘要响应对象。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Data
@Builder
public class OrderSummaryVO {

    /**
     * 订单号。
     */
    private String orderNo;

    /**
     * 订单状态。
     */
    private String status;

    /**
     * 订单金额。
     */
    private BigDecimal amount;

    /**
     * 收货人姓名。
     */
    private String receiverName;

    /**
     * 创建时间。
     */
    private String createdTime;
}
```

Tool 注册方式建议优先使用注解扫描。只要 `OrderTool` 是 Spring Bean，并且开启 `spring.ai.mcp.server.annotation-scanner.enabled=true`，Spring AI 会自动扫描带有 MCP 注解的 Bean，并将其注册到 MCP Server 中。官方文档说明，自动配置会扫描 MCP 注解 Bean，创建对应 specification，并注册到 MCP Server。([Home](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-server-boot-starter-docs.html))

Tool 设计时需要注意以下几点：

1. `name` 应稳定、唯一、语义明确，建议使用业务前缀，例如 `order_query_summary`。
2. `description` 应说明工具何时使用、输入什么、返回什么。
3. 查询类工具建议设置 `readOnlyHint=true` 和 `idempotentHint=true`。
4. 涉及新增、修改、删除的工具必须增加权限校验和审计日志。
5. Tool 返回对象应尽量结构化，不建议只返回长文本。
6. Tool 方法内不建议写复杂业务逻辑，核心逻辑应下沉到 Service 层。

### Resource 定义与暴露

Resource 用于暴露只读上下文，例如配置、文档、业务规则、知识库内容、系统说明等。Spring AI MCP Server 注解模型提供 `@McpResource`，可以通过 URI 模板定义资源访问方式。官方文档说明，`@McpResource` 用于通过 URI template 提供资源访问能力，方法可以直接返回字符串，也可以返回 `ReadResourceResult`。([Home](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-annotations-server.html))

下面示例将业务规则文档暴露为 MCP Resource，客户端可以通过 `biz-rule://order-refund` 读取订单退款规则。

文件位置：`src/main/java/io/github/atengk/mcp/server/resource/BizRuleResource.java`

```java
package io.github.atengk.mcp.server.resource;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.mcp.annotation.McpResource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 业务规则 MCP 资源。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Component
public class BizRuleResource {

    private static final Map<String, String> RULE_MAP = MapUtil.<String, String>builder()
            .put("order-refund", """
                    # 订单退款规则
                    
                    1. 已支付订单支持发起退款。
                    2. 已发货订单需要先完成退货审核。
                    3. 虚拟商品发货后默认不支持自动退款。
                    4. 大额退款需要进入人工审核流程。
                    """)
            .put("invoice", """
                    # 发票开具规则
                    
                    1. 已支付订单可以申请发票。
                    2. 企业发票需要提供纳税人识别号。
                    3. 红冲发票需要关联原发票号码。
                    """)
            .build();

    /**
     * 根据规则编码读取业务规则。
     *
     * @param code 规则编码
     * @return MCP 资源读取结果
     */
    @McpResource(
            uri = "biz-rule://{code}",
            name = "业务规则资源",
            description = "根据业务规则编码读取对应的 Markdown 规则说明"
    )
    public McpSchema.ReadResourceResult readBizRule(String code) {
        if (StrUtil.isBlank(code)) {
            log.warn("读取MCP业务规则失败，规则编码为空");
            throw new IllegalArgumentException("规则编码不能为空");
        }

        String content = RULE_MAP.get(code);
        if (StrUtil.isBlank(content)) {
            log.warn("未找到MCP业务规则，规则编码：{}", code);
            content = "# 未找到业务规则\n\n当前规则编码不存在，请检查 Resource URI。";
        }

        String uri = "biz-rule://" + code;
        log.info("读取MCP业务规则资源，URI：{}", uri);

        return new McpSchema.ReadResourceResult(List.of(
                new McpSchema.TextResourceContents(uri, "text/markdown", content)
        ));
    }
}
```

Resource 适合承载“模型需要参考，但不应该执行动作”的内容。与 Tool 相比，Resource 更强调上下文读取，不应承担写入、审批、支付、删除等动作。

Resource URI 设计建议如下：

| URI 示例                   | 说明                 |
| -------------------------- | -------------------- |
| `biz-rule://order-refund`  | 读取订单退款规则     |
| `config://payment-timeout` | 读取支付超时配置说明 |
| `doc://api/order-create`   | 读取订单创建接口文档 |
| `kb://faq/refund`          | 读取知识库 FAQ       |

Resource 暴露时需要注意以下几点：

1. URI 应清晰表达资源类型和资源标识。
2. MIME 类型应尽量准确，例如 `text/plain`、`text/markdown`、`application/json`。
3. 不要通过 Resource 暴露敏感配置，例如密钥、Token、数据库密码。
4. 内容较长时应考虑摘要化、分页或拆分多个资源。
5. 如果资源来自数据库或远程接口，应增加缓存和超时控制。

### Prompt 定义与使用

Prompt 用于暴露可复用的提示词模板。与 Tool 不同，Prompt 本身通常不执行业务动作，而是根据参数生成一组消息内容，供 MCP Client 或大模型应用继续使用。Spring AI MCP Server 注解模型提供 `@McpPrompt` 和 `@McpArg`，用于定义 Prompt 名称、描述和参数。官方文档说明，`@McpPrompt` 用于生成 AI 交互所需的 prompt messages。([Home](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-annotations-server.html))

下面示例定义一个订单问题分析 Prompt，用于根据订单号和用户问题生成分析提示词。

文件位置：`src/main/java/io/github/atengk/mcp/server/prompt/OrderPromptProvider.java`

```java
package io.github.atengk.mcp.server.prompt;

import cn.hutool.core.util.StrUtil;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.mcp.annotation.McpArg;
import org.springframework.ai.mcp.annotation.McpPrompt;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 订单 MCP 提示词模板。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Component
public class OrderPromptProvider {

    /**
     * 生成订单问题分析提示词。
     *
     * @param orderNo 订单号
     * @param question 用户问题
     * @return MCP Prompt 结果
     */
    @McpPrompt(
            name = "order_issue_analysis",
            description = "根据订单号和用户问题生成订单问题分析提示词"
    )
    public McpSchema.GetPromptResult orderIssueAnalysis(
            @McpArg(name = "orderNo", description = "订单号", required = true)
            String orderNo,
            @McpArg(name = "question", description = "用户提出的订单相关问题", required = true)
            String question) {

        if (StrUtil.hasBlank(orderNo, question)) {
            log.warn("生成MCP订单问题分析Prompt失败，订单号或问题为空");
            throw new IllegalArgumentException("订单号和问题不能为空");
        }

        log.info("生成MCP订单问题分析Prompt，订单号：{}", orderNo);

        String prompt = """
                你是电商订单问题分析助手，请基于订单号和用户问题进行分析。
                
                分析要求：
                1. 先判断问题类型，例如支付、退款、发货、物流、发票、售后。
                2. 再说明需要调用哪些工具或读取哪些资源。
                3. 如果信息不足，明确列出需要补充的字段。
                4. 回答必须结构化，避免直接编造订单事实。
                
                订单号：%s
                
                用户问题：%s
                """.formatted(orderNo, question);

        return new McpSchema.GetPromptResult(
                "订单问题分析提示词",
                List.of(new McpSchema.PromptMessage(
                        McpSchema.Role.USER,
                        new McpSchema.TextContent(prompt)
                ))
        );
    }
}
```

Prompt 设计时建议关注以下几点：

1. Prompt 名称应表达任务类型，例如 `order_issue_analysis`。
2. Prompt 参数应尽量少而明确。
3. Prompt 内容应写清模型角色、输入、分析步骤、输出格式和约束。
4. Prompt 不应包含实时业务数据，实时数据应通过 Tool 或 Resource 获取。
5. Prompt 可以与 Tool、Resource 配合使用，例如先读取规则资源，再调用订单查询 Tool。

### 服务端启动与连接配置

MCP Server 的启动方式取决于所选传输协议。Spring AI MCP Server Boot Starter 支持 STDIO、SSE WebMVC、Streamable HTTP WebMVC、Stateless WebMVC、SSE WebFlux、Streamable HTTP WebFlux、Stateless WebFlux 等类型。不同类型需要选择不同 Starter，并设置对应配置项。官方文档列出了 `spring-ai-starter-mcp-server`、`spring-ai-starter-mcp-server-webmvc`、`spring-ai-starter-mcp-server-webflux` 以及对应协议配置。([Home](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-server-boot-starter-docs.html))

如果当前服务作为 HTTP MCP Server 对外提供能力，推荐使用 WebMVC 或 WebFlux 模式。下面示例使用 WebMVC + Streamable HTTP。

```xml
<dependency>
    <!-- MCP Server WebMVC Starter，用于通过 HTTP 暴露 MCP Server -->
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-mcp-server-webmvc</artifactId>
</dependency>
```

文件位置：`src/main/resources/application.yml`

```yaml
server:
  # MCP Server HTTP 端口
  port: 8081

spring:
  application:
    # MCP Server 应用名称
    name: order-mcp-server

  ai:
    mcp:
      server:
        # 同步服务端，适合传统 Spring MVC 项目
        type: SYNC

        # 使用 Streamable HTTP 协议，适合服务化部署
        protocol: STREAMABLE

        annotation-scanner:
          # 自动扫描 @McpTool、@McpResource、@McpPrompt
          enabled: true

        capabilities:
          # 是否暴露 Tools 能力
          tool: true
          # 是否暴露 Resources 能力
          resource: true
          # 是否暴露 Prompts 能力
          prompt: true
```

如果使用 STDIO 模式，通常适用于本地进程方式接入，例如 Claude Desktop 或本地 MCP Host 启动当前 Java 进程。STDIO 模式需要使用标准 MCP Server Starter，并开启 `spring.ai.mcp.server.stdio=true`。官方文档说明，STDIO 协议通过标准输入输出通信，并通过 `spring.ai.mcp.server.stdio=true` 启用。([Home](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-server-boot-starter-docs.html))

```xml
<dependency>
    <!-- MCP Server STDIO Starter，用于本地进程方式暴露 MCP Server -->
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-mcp-server</artifactId>
</dependency>
```

文件位置：`src/main/resources/application-stdio.yml`

```yaml
spring:
  application:
    name: order-mcp-stdio-server

  ai:
    mcp:
      server:
        # 启用 STDIO 服务端
        stdio: true
        type: SYNC
        annotation-scanner:
          enabled: true
```

启动 HTTP MCP Server：

```bash
# 打包项目
mvn clean package -DskipTests

# 启动 WebMVC Streamable HTTP MCP Server
java -jar target/order-mcp-server.jar
```

启动 STDIO MCP Server：

```bash
# 使用 stdio 配置启动本地 MCP Server 进程
java -jar target/order-mcp-server.jar --spring.profiles.active=stdio
```

服务端启动后，应重点确认以下内容：

1. 应用启动日志中没有 MCP 注解扫描异常。
2. Tool、Resource、Prompt 所在类都被 Spring 扫描为 Bean。
3. `spring.ai.mcp.server.type` 与方法返回类型匹配。
4. `spring.ai.mcp.server.protocol` 与客户端连接方式匹配。
5. 如果使用 Stateless 模式，不要在方法中使用双向上下文参数。官方文档说明，Stateless Server 不支持 bidirectional operations，使用 `McpSyncRequestContext` 或 `McpAsyncRequestContext` 的方法会被忽略。([Home](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-annotations-server.html))

## MCP Client 开发

本章节用于说明 MCP Client 的开发方式。MCP Client 的核心职责是连接一个或多个 MCP Server，发现服务端暴露的 Tools、Resources、Prompts，并将这些能力提供给业务系统或 Spring AI ChatClient 使用。Spring AI MCP Client Boot Starter 支持多个客户端实例、自动初始化、STDIO / SSE / Streamable HTTP 传输、工具回调集成、工具过滤和生命周期管理。([Home](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-client-boot-starter-docs.html))

### 客户端功能定位

MCP Client 是大模型应用侧的协议客户端。它不负责实现具体业务能力，而是负责连接 MCP Server、发现服务端能力、调用工具、读取资源、获取 Prompt，并将结果交给业务服务或大模型调用链路继续处理。

MCP Client 的职责如下：

| 职责          | 说明                                                   |
| ------------- | ------------------------------------------------------ |
| 连接服务端    | 通过 STDIO、SSE、Streamable HTTP 等方式连接 MCP Server |
| 初始化会话    | 完成协议协商、能力协商和客户端初始化                   |
| 发现能力      | 获取服务端 Tools、Resources、Prompts                   |
| 调用 Tool     | 传入结构化参数并获取工具执行结果                       |
| 读取 Resource | 根据资源 URI 读取服务端暴露的资源内容                  |
| 使用 Prompt   | 根据 Prompt 名称和参数获取提示词消息                   |
| 集成模型      | 将 MCP Tools 注册为 Spring AI ToolCallback             |
| 管理生命周期  | 处理连接关闭、超时、异常和资源释放                     |

Spring AI MCP Client 默认是同步客户端，适合传统 Spring MVC 和阻塞调用场景；如果项目是响应式应用，可以配置 `spring.ai.mcp.client.type=ASYNC`。官方文档说明，Client 类型可以是 `SYNC` 或 `ASYNC`，但不能混合同步和异步客户端。([Home](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-client-boot-starter-docs.html))

### MCP Server 连接配置

MCP Client 可以连接多个 MCP Server。Spring AI MCP Client 支持 STDIO、SSE、Streamable HTTP、Stateless Streamable HTTP 等连接方式。标准 Client Starter 支持 STDIO、SSE、Streamable HTTP 和 Stateless Streamable HTTP；生产环境中，官方文档建议优先考虑基于 WebFlux 的 SSE 和 Streamable HTTP 连接。([Home](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-client-boot-starter-docs.html))

MCP Client 依赖如下：

```xml
<dependency>
    <!-- MCP Client 标准 Starter，支持 STDIO、SSE、Streamable HTTP 等连接方式 -->
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-mcp-client</artifactId>
</dependency>
```

如果项目使用响应式调用或希望使用 WebFlux 传输，可以使用 WebFlux Client Starter：

```xml
<dependency>
    <!-- MCP Client WebFlux Starter，适用于响应式项目和 WebFlux 传输 -->
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-mcp-client-webflux</artifactId>
</dependency>
```

连接 Streamable HTTP MCP Server 的配置如下。Spring AI 文档说明，Streamable HTTP 配置前缀为 `spring.ai.mcp.client.streamable-http`，连接项包含 `url` 和默认端点 `/mcp`。([Home](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-client-boot-starter-docs.html))

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  application:
    name: ai-chat-client

  ai:
    mcp:
      client:
        # 启用 MCP Client
        enabled: true

        # MCP Client 名称，会参与客户端初始化信息
        name: ai-chat-client

        # MCP Client 版本
        version: 1.0.0

        # 请求超时时间
        request-timeout: 30s

        # 客户端类型：SYNC 或 ASYNC
        type: SYNC

        toolcallback:
          # 开启后，MCP Server 中的 Tools 会转换为 Spring AI ToolCallback
          enabled: true

        streamable-http:
          connections:
            order-server:
              # MCP Server 基础地址
              url: http://localhost:8081
              # Streamable HTTP 端点，默认是 /mcp
              endpoint: /mcp
```

连接 SSE MCP Server 的配置如下。Spring AI 文档说明，SSE 配置前缀为 `spring.ai.mcp.client.sse`，连接项包含基础 `url` 和默认 `/sse` 端点；如果拿到完整 SSE URL，应拆分为 base url 和 endpoint。([Home](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-client-boot-starter-docs.html))

```yaml
spring:
  ai:
    mcp:
      client:
        sse:
          connections:
            order-sse-server:
              # 只配置协议、主机、端口作为基础地址
              url: http://localhost:8081
              # SSE 端点，默认是 /sse
              sse-endpoint: /sse
```

连接 STDIO MCP Server 可以直接在 `application.yml` 中配置命令，也可以使用 `mcp-servers.json`。Spring AI 文档说明，STDIO 配置前缀为 `spring.ai.mcp.client.stdio`，支持 `servers-configuration` 指向外部 JSON 配置文件。([Home](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-client-boot-starter-docs.html))

```yaml
spring:
  ai:
    mcp:
      client:
        stdio:
          root-change-notification: true
          connections:
            local-order-server:
              # 启动本地 MCP Server 的命令
              command: java
              args:
                - -jar
                - /opt/mcp/order-mcp-server.jar
                - --spring.profiles.active=stdio
              env:
                # 示例环境变量，生产环境不要在仓库中提交真实密钥
                APP_ENV: dev
```

也可以使用外部 JSON 文件：

```yaml
spring:
  ai:
    mcp:
      client:
        stdio:
          # 使用 Claude Desktop 格式的 MCP Server 配置文件
          servers-configuration: classpath:mcp-servers.json
```

文件位置：`src/main/resources/mcp-servers.json`

```json
{
  "mcpServers": {
    "local-order-server": {
      "command": "java",
      "args": [
        "-jar",
        "/opt/mcp/order-mcp-server.jar",
        "--spring.profiles.active=stdio"
      ],
      "env": {
        "APP_ENV": "dev"
      }
    }
  }
}
```

### Tool 调用流程

MCP Tool 调用通常有两种方式：一种是直接通过 `McpSyncClient` 调用服务端 Tool；另一种是通过 Spring AI 的 `ToolCallbackProvider` 把 MCP Tool 接入 ChatClient。当前章节先说明直接调用流程，后续“大模型集成”章节再重点说明 ChatClient 接入。

直接调用流程如下：

1. 注入 `List<McpSyncClient>`。
2. 从客户端获取服务端工具列表。
3. 根据工具名称选择目标 Tool。
4. 构造 `CallToolRequest`。
5. 调用 `callTool`。
6. 解析 `CallToolResult`。

MCP Java SDK 文档给出了同步客户端的标准调用方式，包括 `listTools()`、`callTool()`、`listResources()`、`readResource()`、`listPrompts()` 和 `getPrompt()`。([Home](https://docs.spring.io/spring-ai-mcp/reference/mcp.html?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/mcp/client/service/McpToolInvokeService.java`

```java
package io.github.atengk.mcp.client.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * MCP Tool 调用服务。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class McpToolInvokeService {

    private final List<McpSyncClient> mcpSyncClients;

    /**
     * 调用订单摘要查询 Tool。
     *
     * @param orderNo 订单号
     * @return Tool 调用结果
     */
    public McpSchema.CallToolResult queryOrderSummary(String orderNo) {
        McpSyncClient client = getFirstClient();

        log.info("开始获取MCP工具列表");
        McpSchema.ListToolsResult toolsResult = client.listTools();
        log.info("MCP工具列表获取完成，工具数量：{}", toolsResult.tools().size());

        String toolName = "order_query_summary";
        boolean exists = toolsResult.tools()
                .stream()
                .anyMatch(tool -> toolName.equals(tool.name()));

        if (!exists) {
            log.warn("MCP工具不存在，工具名称：{}", toolName);
            throw new IllegalStateException("MCP工具不存在：" + toolName);
        }

        Map<String, Object> arguments = Map.of("orderNo", orderNo);
        log.info("开始调用MCP工具，工具名称：{}，参数：{}", toolName, JSONUtil.toJsonStr(arguments));

        return client.callTool(new McpSchema.CallToolRequest(toolName, arguments));
    }

    /**
     * 获取第一个 MCP 同步客户端。
     *
     * @return MCP 同步客户端
     */
    private McpSyncClient getFirstClient() {
        if (CollUtil.isEmpty(mcpSyncClients)) {
            throw new IllegalStateException("未发现可用的MCP同步客户端");
        }
        return mcpSyncClients.getFirst();
    }
}
```

如果只需要把 MCP Server 暴露的 Tools 接入 Spring AI Tool Calling，可以直接注入 `SyncMcpToolCallbackProvider`。官方文档说明，开启 tool callback 后，所有 MCP Client 注册的 MCP Tools 会作为 `ToolCallbackProvider` 提供，并可以通过 `getToolCallbacks()` 获取。([Home](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-client-boot-starter-docs.html))

文件位置：`src/main/java/io/github/atengk/mcp/client/service/McpToolCallbackService.java`

```java
package io.github.atengk.mcp.client.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Service;

/**
 * MCP ToolCallback 查询服务。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class McpToolCallbackService {

    private final SyncMcpToolCallbackProvider toolCallbackProvider;

    /**
     * 获取 MCP ToolCallback 数组。
     *
     * @return ToolCallback 数组
     */
    public ToolCallback[] getToolCallbacks() {
        ToolCallback[] callbacks = toolCallbackProvider.getToolCallbacks();
        log.info("获取MCP工具回调完成，工具数量：{}", callbacks.length);
        return callbacks;
    }
}
```

Tool 调用注意事项：

1. 多个 MCP Server 可能存在同名工具，建议保留默认工具名前缀生成策略。
2. 不建议在业务代码中硬编码大量工具名称，可以建立工具名称常量类。
3. 调用结果应做结构化解析，不要默认把结果当作纯文本。
4. 高风险工具调用前应由业务层增加二次确认或权限判断。
5. 超时、连接失败、工具不存在都应转换为业务可识别异常。

### Resource 读取流程

Resource 读取用于获取 MCP Server 暴露的只读上下文。典型流程是先通过 `listResources()` 获取可用资源列表，再根据资源 URI 调用 `readResource()` 读取内容。MCP Java SDK 文档中的同步客户端示例包含 `listResources()` 和 `readResource(new ReadResourceRequest("resource://uri"))` 的调用方式。([Home](https://docs.spring.io/spring-ai-mcp/reference/mcp.html?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/mcp/client/service/McpResourceReadService.java`

```java
package io.github.atengk.mcp.client.service;

import cn.hutool.core.collection.CollUtil;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * MCP Resource 读取服务。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class McpResourceReadService {

    private final List<McpSyncClient> mcpSyncClients;

    /**
     * 读取订单退款规则资源。
     *
     * @return 资源读取结果
     */
    public McpSchema.ReadResourceResult readOrderRefundRule() {
        return readResource("biz-rule://order-refund");
    }

    /**
     * 根据 URI 读取 MCP Resource。
     *
     * @param uri 资源 URI
     * @return 资源读取结果
     */
    public McpSchema.ReadResourceResult readResource(String uri) {
        McpSyncClient client = getFirstClient();

        log.info("开始读取MCP资源，URI：{}", uri);
        McpSchema.ReadResourceResult result = client.readResource(
                new McpSchema.ReadResourceRequest(uri)
        );

        log.info("读取MCP资源完成，URI：{}，内容数量：{}", uri, result.contents().size());
        return result;
    }

    /**
     * 获取第一个 MCP 同步客户端。
     *
     * @return MCP 同步客户端
     */
    private McpSyncClient getFirstClient() {
        if (CollUtil.isEmpty(mcpSyncClients)) {
            throw new IllegalStateException("未发现可用的MCP同步客户端");
        }
        return mcpSyncClients.getFirst();
    }
}
```

Resource 读取常见场景如下：

| 场景             | 示例 URI                   | 用途                   |
| ---------------- | -------------------------- | ---------------------- |
| 读取业务规则     | `biz-rule://order-refund`  | 给模型提供业务判断依据 |
| 读取接口文档     | `doc://api/order-create`   | 给模型提供接口调用说明 |
| 读取系统配置说明 | `config://payment-timeout` | 给模型解释配置含义     |
| 读取知识库内容   | `kb://faq/refund`          | 给模型提供问答依据     |

Resource 读取注意事项：

1. Resource URI 应由业务服务层统一管理，不建议散落在 Controller 中。
2. Resource 内容进入模型上下文前，应控制长度和敏感信息。
3. 如果资源内容较长，应在业务层进行摘要、分段或检索。
4. 对外部系统资源读取应设置超时、缓存和降级策略。
5. 读取失败时应返回明确错误，避免模型基于空上下文编造答案。

### Prompt 使用流程

Prompt 使用用于从 MCP Server 获取标准化提示词模板。典型流程是先通过 `listPrompts()` 获取可用 Prompt，再通过 `getPrompt()` 按名称和参数获取具体消息内容。MCP Java SDK 文档中的同步客户端示例包含 `listPrompts()` 和 `getPrompt(new GetPromptRequest("greeting", Map.of(...)))` 的调用方式。([Home](https://docs.spring.io/spring-ai-mcp/reference/mcp.html?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/mcp/client/service/McpPromptUseService.java`

```java
package io.github.atengk.mcp.client.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * MCP Prompt 使用服务。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class McpPromptUseService {

    private final List<McpSyncClient> mcpSyncClients;

    /**
     * 获取订单问题分析 Prompt。
     *
     * @param orderNo 订单号
     * @param question 用户问题
     * @return Prompt 获取结果
     */
    public McpSchema.GetPromptResult getOrderIssueAnalysisPrompt(String orderNo, String question) {
        McpSyncClient client = getFirstClient();

        String promptName = "order_issue_analysis";
        Map<String, Object> arguments = Map.of(
                "orderNo", orderNo,
                "question", question
        );

        log.info("开始获取MCP Prompt，名称：{}，参数：{}", promptName, JSONUtil.toJsonStr(arguments));

        McpSchema.GetPromptResult result = client.getPrompt(
                new McpSchema.GetPromptRequest(promptName, arguments)
        );

        log.info("获取MCP Prompt完成，名称：{}，消息数量：{}", promptName, result.messages().size());
        return result;
    }

    /**
     * 获取第一个 MCP 同步客户端。
     *
     * @return MCP 同步客户端
     */
    private McpSyncClient getFirstClient() {
        if (CollUtil.isEmpty(mcpSyncClients)) {
            throw new IllegalStateException("未发现可用的MCP同步客户端");
        }
        return mcpSyncClients.getFirst();
    }
}
```

Prompt 使用流程建议如下：

1. 业务层根据场景确定 Prompt 名称。
2. 组装 Prompt 参数，例如订单号、问题描述、语言、输出格式。
3. 调用 `getPrompt()` 获取标准消息。
4. 将 Prompt 消息转换为 Spring AI ChatClient 可使用的输入。
5. 如需实时数据，应先调用 Tool 或读取 Resource，再与 Prompt 一起进入模型上下文。

Prompt 适合用于固定任务流程，例如：

| Prompt 名称            | 使用场景         |
| ---------------------- | ---------------- |
| `order_issue_analysis` | 分析订单相关问题 |
| `code_review_java`     | Java 代码审查    |
| `sql_optimize`         | SQL 优化建议     |
| `incident_analysis`    | 故障排查分析     |
| `api_doc_generate`     | 接口文档生成     |

MCP Client 开发完成后，建议先通过业务测试类直接验证 Tool、Resource、Prompt 三类能力，再接入 ChatClient。这样可以把协议连接问题、服务端能力暴露问题和大模型调用问题分开排查，降低联调复杂度。

## 与大模型集成

本章节用于说明 MCP Client 获取到的工具能力如何接入 Spring AI 大模型调用链路。Spring AI 的 `ChatClient` 提供流式 API 和同步 API，可以通过自动配置的 `ChatClient.Builder` 创建客户端实例；模型调用时可以传入用户消息、系统提示词、工具回调、上下文参数，并通过 `call().content()`、`call().chatResponse()`、`call().entity()` 等方式获取结果。([Home](https://docs.spring.io/spring-ai/reference/api/chatclient.html))

### ChatClient 接入方式

`ChatClient` 是 Spring AI 面向大模型调用的主要入口。MCP 与大模型集成时，通常不是直接让业务代码调用 MCP Tool，而是先由 MCP Client 将 MCP Server 暴露的工具转换为 Spring AI 的 `ToolCallback`，再将这些 `ToolCallback` 注册到 `ChatClient` 中。Spring AI MCP Client Boot Starter 支持将 MCP Server 中注册的工具自动提供为 `ToolCallbackProvider`，并支持多个 MCP Client 实例、多个传输连接、工具过滤和生命周期管理。([Home](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-client-boot-starter-docs.html?utm_source=chatgpt.com))

推荐在配置类中同时创建两个 `ChatClient`：

1. `mcpChatClient`：默认携带 MCP ToolCallback，适合需要调用外部工具的业务场景。
2. `plainChatClient`：不携带 MCP ToolCallback，适合普通问答、摘要、改写等不需要工具调用的场景。

这样可以避免所有模型请求默认都带上 MCP 工具，降低误调用工具的概率。

文件位置：`src/main/java/io/github/atengk/mcp/chat/config/ChatClientConfig.java`

下面的配置类创建两个 `ChatClient` Bean，其中 `mcpChatClient` 会把 MCP Client 自动发现的工具注册为默认工具回调。

```java
package io.github.atengk.mcp.chat.config;

import cn.hutool.core.util.ArrayUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring AI ChatClient 配置。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Configuration
public class ChatClientConfig {

    /**
     * 创建携带 MCP ToolCallback 的 ChatClient。
     *
     * @param builder ChatClient 构建器
     * @param toolCallbackProviderProvider MCP 工具回调提供器
     * @return 携带 MCP 工具的 ChatClient
     */
    @Bean
    @Qualifier("mcpChatClient")
    public ChatClient mcpChatClient(
            ChatClient.Builder builder,
            ObjectProvider<SyncMcpToolCallbackProvider> toolCallbackProviderProvider) {

        ToolCallback[] toolCallbacks = loadMcpToolCallbacks(toolCallbackProviderProvider);

        log.info("初始化携带MCP工具的ChatClient，工具数量：{}", toolCallbacks.length);

        return builder
                .defaultSystem("""
                        你是一个企业业务助手。
                        你可以在需要时调用系统提供的 MCP 工具查询业务数据、读取业务规则或辅助分析问题。
                        当工具返回结果不足以回答问题时，需要明确说明缺少哪些信息，禁止编造业务事实。
                        """)
                .defaultToolCallbacks(toolCallbacks)
                .build();
    }

    /**
     * 创建不携带 MCP ToolCallback 的普通 ChatClient。
     *
     * @param builder ChatClient 构建器
     * @return 普通 ChatClient
     */
    @Bean
    @Qualifier("plainChatClient")
    public ChatClient plainChatClient(ChatClient.Builder builder) {
        log.info("初始化普通ChatClient，不加载MCP工具");
        return builder
                .defaultSystem("""
                        你是一个企业知识助手。
                        请基于用户输入进行回答，不要声明已经查询外部系统。
                        如果问题需要实时业务数据，请提示用户启用 MCP 工具调用。
                        """)
                .build();
    }

    /**
     * 加载 MCP ToolCallback。
     *
     * @param toolCallbackProviderProvider MCP 工具回调提供器
     * @return MCP ToolCallback 数组
     */
    private ToolCallback[] loadMcpToolCallbacks(
            ObjectProvider<SyncMcpToolCallbackProvider> toolCallbackProviderProvider) {

        SyncMcpToolCallbackProvider provider = toolCallbackProviderProvider.getIfAvailable();
        if (provider == null) {
            log.warn("未发现SyncMcpToolCallbackProvider，MCP工具不会注册到ChatClient");
            return new ToolCallback[0];
        }

        ToolCallback[] toolCallbacks = provider.getToolCallbacks();
        if (ArrayUtil.isEmpty(toolCallbacks)) {
            log.warn("未发现可用的MCP工具，ChatClient将以无工具模式运行");
            return new ToolCallback[0];
        }

        return toolCallbacks;
    }
}
```

如果只希望在某一次调用中启用 MCP 工具，而不是作为默认工具注册，可以在调用 `ChatClient` 时通过 `.toolCallbacks(...)` 临时传入工具。Spring AI 官方文档说明，`ChatClient` 支持在运行时通过 `.toolCallbacks()` 传入工具，也支持在构建阶段通过 `.defaultToolCallbacks()` 配置默认工具。([Home](https://docs.spring.io/spring-ai/reference/api/tools.html))

### Function Calling 与 Tool 调用

在 Spring AI 中，Function Calling 更准确地落地为 Tool Calling。模型并不会直接访问 Java 方法、数据库或外部 API；模型只会根据工具名称、描述和参数 Schema 请求调用某个工具，真正的工具执行由应用程序完成，然后应用程序再把工具执行结果返回给模型，模型基于工具结果生成最终回答。Spring AI 官方文档也明确说明：模型只能请求工具调用并提供参数，应用程序负责执行工具调用并返回结果，这是重要的安全边界。([Home](https://docs.spring.io/spring-ai/reference/api/tools.html))

MCP Tool 接入 Spring AI 后，整体流程如下：

| 步骤 | 说明                                                         |
| ---- | ------------------------------------------------------------ |
| 1    | MCP Server 暴露 Tool，例如 `order_query_summary`             |
| 2    | MCP Client 连接 MCP Server 并发现 Tool                       |
| 3    | Spring AI MCP Client Starter 将 MCP Tool 转换为 `ToolCallback` |
| 4    | `ChatClient` 将 Tool 定义随请求发送给模型                    |
| 5    | 模型判断是否需要调用工具                                     |
| 6    | 应用程序根据模型请求执行对应 `ToolCallback`                  |
| 7    | MCP Client 调用 MCP Server Tool                              |
| 8    | Tool 返回结果后，Spring AI 将结果传回模型                    |
| 9    | 模型基于工具结果生成最终自然语言回答                         |

Spring AI 的 Tool Calling 抽象中，工具由 `ToolCallback` 表示，`ChatClient` 和 `ChatModel` 都可以接收 `ToolCallback` 列表；框架内部通过 `ToolCallingManager` 管理工具执行生命周期。([Home](https://docs.spring.io/spring-ai/reference/api/tools.html))

文件位置：`src/main/java/io/github/atengk/mcp/chat/service/McpAiChatService.java`

下面的接口封装大模型问答能力，业务层不直接感知 `ChatClient` 和 MCP ToolCallback 细节。

```java
package io.github.atengk.mcp.chat.service;

import io.github.atengk.mcp.chat.model.dto.McpChatRequestDTO;
import io.github.atengk.mcp.chat.model.vo.McpChatResponseVO;

/**
 * MCP 大模型对话服务。
 *
 * @author Ateng
 * @since 2026-05-11
 */
public interface McpAiChatService {

    /**
     * 执行对话。
     *
     * @param request 请求参数
     * @return 对话响应
     */
    McpChatResponseVO chat(McpChatRequestDTO request);
}
```

文件位置：`src/main/java/io/github/atengk/mcp/chat/service/impl/McpAiChatServiceImpl.java`

下面的实现类根据请求参数判断是否启用 MCP 工具，并通过 `toolContext` 传递租户、用户、业务场景等上下文信息。

```java
package io.github.atengk.mcp.chat.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.mcp.chat.exception.McpBusinessException;
import io.github.atengk.mcp.chat.model.dto.McpChatRequestDTO;
import io.github.atengk.mcp.chat.model.vo.McpChatResponseVO;
import io.github.atengk.mcp.chat.service.McpAiChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * MCP 大模型对话服务实现。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class McpAiChatServiceImpl implements McpAiChatService {

    @Qualifier("mcpChatClient")
    private final ChatClient mcpChatClient;

    @Qualifier("plainChatClient")
    private final ChatClient plainChatClient;

    /**
     * 执行对话。
     *
     * @param request 请求参数
     * @return 对话响应
     */
    @Override
    public McpChatResponseVO chat(McpChatRequestDTO request) {
        checkRequest(request);

        TimeInterval timer = DateUtil.timer();
        String conversationId = StrUtil.blankToDefault(request.getConversationId(), IdUtil.fastSimpleUUID());
        boolean toolEnabled = Boolean.TRUE.equals(request.getToolEnabled());

        log.info("开始执行AI对话，会话ID：{}，启用MCP工具：{}", conversationId, toolEnabled);

        try {
            ChatClient targetClient = toolEnabled ? mcpChatClient : plainChatClient;
            Map<String, Object> toolContext = buildToolContext(request, conversationId);

            String answer = targetClient.prompt()
                    .system(systemSpec -> systemSpec.text(buildSystemPrompt(request)))
                    .user(userSpec -> userSpec
                            .text(request.getMessage())
                            .metadata("conversationId", conversationId)
                            .metadata("businessScene", StrUtil.blankToDefault(request.getBusinessScene(), "default")))
                    .toolContext(toolContext)
                    .call()
                    .content();

            long costMillis = timer.interval();
            log.info("AI对话执行完成，会话ID：{}，耗时：{}ms", conversationId, costMillis);

            return McpChatResponseVO.builder()
                    .conversationId(conversationId)
                    .answer(answer)
                    .toolEnabled(toolEnabled)
                    .businessScene(StrUtil.blankToDefault(request.getBusinessScene(), "default"))
                    .costMillis(costMillis)
                    .build();
        } catch (Exception e) {
            log.error("AI对话执行失败，会话ID：{}，启用MCP工具：{}", conversationId, toolEnabled, e);
            throw new McpBusinessException("AI对话执行失败，请稍后重试或检查MCP Server连接状态");
        }
    }

    /**
     * 校验请求参数。
     *
     * @param request 请求参数
     */
    private void checkRequest(McpChatRequestDTO request) {
        if (request == null) {
            throw new McpBusinessException("请求参数不能为空");
        }
        if (StrUtil.isBlank(request.getMessage())) {
            throw new McpBusinessException("用户消息不能为空");
        }
    }

    /**
     * 构建系统提示词。
     *
     * @param request 请求参数
     * @return 系统提示词
     */
    private String buildSystemPrompt(McpChatRequestDTO request) {
        String businessScene = StrUtil.blankToDefault(request.getBusinessScene(), "通用业务问答");

        return """
                你是企业业务助手，当前业务场景：%s。
                
                回答要求：
                1. 如果问题涉及订单、业务规则、配置、文档或实时数据，可以调用已提供的 MCP 工具。
                2. 如果工具返回的信息不足，需要说明缺少哪些信息。
                3. 不要伪造订单状态、金额、用户身份、审批结果等业务事实。
                4. 回答尽量结构化，必要时使用编号说明处理过程。
                """.formatted(businessScene);
    }

    /**
     * 构建工具上下文。
     *
     * @param request 请求参数
     * @param conversationId 会话ID
     * @return 工具上下文
     */
    private Map<String, Object> buildToolContext(McpChatRequestDTO request, String conversationId) {
        return MapUtil.<String, Object>builder()
                .put("conversationId", conversationId)
                .put("tenantId", StrUtil.blankToDefault(request.getTenantId(), "default"))
                .put("operatorId", StrUtil.blankToDefault(request.getOperatorId(), "anonymous"))
                .put("businessScene", StrUtil.blankToDefault(request.getBusinessScene(), "default"))
                .build();
    }
}
```

这里使用 `toolContext` 传递租户、操作者、会话 ID 等信息。Spring AI 官方文档说明，`ToolContext` 可以在工具执行时提供额外上下文，并且这些上下文不会发送给 AI 模型；如果默认选项和运行时选项都设置了 `toolContext`，最终上下文会合并，运行时参数优先。([Home](https://docs.spring.io/spring-ai/reference/api/tools.html))

### 上下文传递与结果处理

与 MCP 集成时，上下文通常分为三类：模型上下文、工具上下文和业务上下文。三者用途不同，不能混用。

| 上下文类型 | 传递方式                                    | 是否进入模型 | 适用内容                               |
| ---------- | ------------------------------------------- | ------------ | -------------------------------------- |
| 模型上下文 | `system()`、`user()`、Prompt、Resource 内容 | 是           | 用户问题、业务规则、文档摘要、回答约束 |
| 工具上下文 | `toolContext()`                             | 否           | 租户 ID、操作者 ID、权限范围、traceId  |
| 业务上下文 | Service 方法参数、DTO 字段、认证信息        | 视情况       | 会话 ID、业务场景、用户身份、来源系统  |

处理结果时，建议不要只返回模型文本，还应返回会话 ID、是否启用工具、业务场景、耗时、错误信息等元数据，便于前端展示、日志追踪和问题排查。

请求对象用于接收前端或业务系统传入的对话参数。

文件位置：`src/main/java/io/github/atengk/mcp/chat/model/dto/McpChatRequestDTO.java`

```java
package io.github.atengk.mcp.chat.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * MCP 对话请求参数。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Data
public class McpChatRequestDTO {

    /**
     * 会话ID，为空时由服务端自动生成。
     */
    private String conversationId;

    /**
     * 用户消息。
     */
    @NotBlank(message = "用户消息不能为空")
    private String message;

    /**
     * 是否启用 MCP 工具。
     */
    private Boolean toolEnabled = Boolean.TRUE;

    /**
     * 租户ID。
     */
    private String tenantId;

    /**
     * 操作人ID。
     */
    private String operatorId;

    /**
     * 业务场景。
     */
    private String businessScene;
}
```

响应对象用于统一返回模型回答和调用元数据。

文件位置：`src/main/java/io/github/atengk/mcp/chat/model/vo/McpChatResponseVO.java`

```java
package io.github.atengk.mcp.chat.model.vo;

import lombok.Builder;
import lombok.Data;

/**
 * MCP 对话响应结果。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Data
@Builder
public class McpChatResponseVO {

    /**
     * 会话ID。
     */
    private String conversationId;

    /**
     * 模型回答内容。
     */
    private String answer;

    /**
     * 是否启用 MCP 工具。
     */
    private Boolean toolEnabled;

    /**
     * 业务场景。
     */
    private String businessScene;

    /**
     * 调用耗时，单位毫秒。
     */
    private Long costMillis;
}
```

如果需要拿到更完整的模型响应，可以使用 `chatResponse()` 或 `chatClientResponse()`。Spring AI 文档说明，`content()` 返回字符串内容，`chatResponse()` 返回包含 generation 和元数据的完整响应，`chatClientResponse()` 还可以访问 Advisor 执行期间使用的上下文数据。([Home](https://docs.spring.io/spring-ai/reference/api/chatclient.html))

## 接口与业务封装

本章节用于说明如何将 MCP 与大模型调用封装为稳定的业务接口。实际项目中不建议让 Controller 直接操作 `ChatClient`、`McpSyncClient` 或 `ToolCallbackProvider`，而应通过 Service 层屏蔽底层协议和模型调用细节。

### MCP 服务封装

MCP 服务封装的目标是隔离三类变化：

1. MCP Server 地址、协议和工具名称变化。
2. Spring AI ChatClient、ToolCallback、McpClient API 变化。
3. 业务侧请求参数、权限控制、日志审计和异常处理变化。

推荐分层如下：

| 层级         | 职责                                                       |
| ------------ | ---------------------------------------------------------- |
| Controller   | 提供 HTTP 接口，接收参数并返回统一结果                     |
| Service      | 编排大模型调用、MCP 工具调用、上下文构造                   |
| MCP Adapter  | 直接操作 `McpSyncClient`、`McpAsyncClient` 或 ToolCallback |
| Model DTO/VO | 定义请求和响应结构                                         |
| Exception    | 统一处理业务异常、模型异常、MCP 调用异常                   |

对外接口可以先提供一个通用对话接口，后续再根据业务场景拆分为订单助手、运维助手、文档助手等专用接口。

文件位置：`src/main/java/io/github/atengk/mcp/chat/controller/McpChatController.java`

下面的 Controller 提供一个统一的 MCP 对话接口，内部调用 `McpAiChatService` 完成业务编排。

```java
package io.github.atengk.mcp.chat.controller;

import io.github.atengk.mcp.chat.common.ApiResult;
import io.github.atengk.mcp.chat.model.dto.McpChatRequestDTO;
import io.github.atengk.mcp.chat.model.vo.McpChatResponseVO;
import io.github.atengk.mcp.chat.service.McpAiChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * MCP 对话接口。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ai/mcp")
public class McpChatController {

    private final McpAiChatService mcpAiChatService;

    /**
     * 执行 MCP 增强对话。
     *
     * @param request 请求参数
     * @return 对话响应
     */
    @PostMapping("/chat")
    public ApiResult<McpChatResponseVO> chat(@Valid @RequestBody McpChatRequestDTO request) {
        log.info("收到MCP对话请求，业务场景：{}，启用工具：{}", request.getBusinessScene(), request.getToolEnabled());
        return ApiResult.success(mcpAiChatService.chat(request));
    }
}
```

统一返回结构用于规范接口响应格式。

文件位置：`src/main/java/io/github/atengk/mcp/chat/common/ApiResult.java`

```java
package io.github.atengk.mcp.chat.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 接口统一响应结构。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResult<T> {

    /**
     * 响应编码。
     */
    private Integer code;

    /**
     * 响应消息。
     */
    private String message;

    /**
     * 响应数据。
     */
    private T data;

    /**
     * 成功响应。
     *
     * @param data 响应数据
     * @param <T> 数据类型
     * @return 统一响应
     */
    public static <T> ApiResult<T> success(T data) {
        return new ApiResult<>(200, "操作成功", data);
    }

    /**
     * 失败响应。
     *
     * @param code 响应编码
     * @param message 响应消息
     * @param <T> 数据类型
     * @return 统一响应
     */
    public static <T> ApiResult<T> fail(Integer code, String message) {
        return new ApiResult<>(code, message, null);
    }
}
```

### 业务方法设计

业务方法设计时，应避免把“用户原始输入”直接传给模型后返回结果。更稳妥的方式是先明确业务场景，再构造系统提示词、工具上下文和模型输入。

推荐业务方法分为三类：

| 方法类型     | 示例                  | 说明                              |
| ------------ | --------------------- | --------------------------------- |
| 通用对话     | `chat()`              | 根据用户输入决定是否调用 MCP 工具 |
| 专用问答     | `askOrderQuestion()`  | 固定业务场景和工具范围            |
| 直接工具调用 | `queryOrderSummary()` | 不经过模型，直接调用 MCP Tool     |

通用对话适合开放式问题，专用问答适合业务流程明确的问题，直接工具调用适合系统内部明确知道要调用哪个工具的场景。

如果需要提供订单问题专用方法，可以在 Service 层新增方法，而不是让前端拼接复杂 Prompt。

文件位置：`src/main/java/io/github/atengk/mcp/chat/model/dto/OrderAiQuestionDTO.java`

```java
package io.github.atengk.mcp.chat.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 订单 AI 问题请求参数。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Data
public class OrderAiQuestionDTO {

    /**
     * 订单号。
     */
    @NotBlank(message = "订单号不能为空")
    private String orderNo;

    /**
     * 用户问题。
     */
    @NotBlank(message = "用户问题不能为空")
    private String question;

    /**
     * 操作人ID。
     */
    private String operatorId;

    /**
     * 租户ID。
     */
    private String tenantId;
}
```

文件位置：`src/main/java/io/github/atengk/mcp/chat/service/OrderAiService.java`

```java
package io.github.atengk.mcp.chat.service;

import io.github.atengk.mcp.chat.model.dto.OrderAiQuestionDTO;
import io.github.atengk.mcp.chat.model.vo.McpChatResponseVO;

/**
 * 订单 AI 服务。
 *
 * @author Ateng
 * @since 2026-05-11
 */
public interface OrderAiService {

    /**
     * 咨询订单问题。
     *
     * @param request 请求参数
     * @return AI 响应
     */
    McpChatResponseVO askOrderQuestion(OrderAiQuestionDTO request);
}
```

文件位置：`src/main/java/io/github/atengk/mcp/chat/service/impl/OrderAiServiceImpl.java`

下面的实现类把订单号和问题组装成更明确的业务输入，再复用通用 MCP 对话服务。

```java
package io.github.atengk.mcp.chat.service.impl;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.mcp.chat.exception.McpBusinessException;
import io.github.atengk.mcp.chat.model.dto.McpChatRequestDTO;
import io.github.atengk.mcp.chat.model.dto.OrderAiQuestionDTO;
import io.github.atengk.mcp.chat.model.vo.McpChatResponseVO;
import io.github.atengk.mcp.chat.service.McpAiChatService;
import io.github.atengk.mcp.chat.service.OrderAiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 订单 AI 服务实现。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderAiServiceImpl implements OrderAiService {

    private final McpAiChatService mcpAiChatService;

    /**
     * 咨询订单问题。
     *
     * @param request 请求参数
     * @return AI 响应
     */
    @Override
    public McpChatResponseVO askOrderQuestion(OrderAiQuestionDTO request) {
        if (request == null || StrUtil.hasBlank(request.getOrderNo(), request.getQuestion())) {
            throw new McpBusinessException("订单号和用户问题不能为空");
        }

        log.info("开始处理订单AI问题，订单号：{}", request.getOrderNo());

        String message = """
                请分析以下订单问题。
                
                订单号：%s
                
                用户问题：%s
                
                处理要求：
                1. 优先调用订单查询相关 MCP 工具获取真实订单信息。
                2. 如涉及退款、发票、售后规则，可以读取对应 MCP Resource。
                3. 如果无法确认订单事实，需要明确提示人工核查。
                """.formatted(request.getOrderNo(), request.getQuestion());

        McpChatRequestDTO chatRequest = new McpChatRequestDTO();
        chatRequest.setMessage(message);
        chatRequest.setToolEnabled(true);
        chatRequest.setTenantId(request.getTenantId());
        chatRequest.setOperatorId(request.getOperatorId());
        chatRequest.setBusinessScene("订单问题分析");

        return mcpAiChatService.chat(chatRequest);
    }
}
```

### 请求参数与响应结构

对外接口建议使用明确 DTO，不建议直接暴露 Spring AI 或 MCP SDK 的内部对象。原因是 SDK 对象结构复杂，并且可能随着 Spring AI 版本升级发生变化；业务接口应保持稳定。

通用对话接口定义如下：

| 项           | 说明               |
| ------------ | ------------------ |
| 接口路径     | `/api/ai/mcp/chat` |
| 请求方式     | `POST`             |
| Content-Type | `application/json` |
| 用途         | 执行 MCP 增强对话  |

请求示例：

```json
{
  "conversationId": "conv-10001",
  "message": "帮我查询订单 ORD202605110001 的状态，并说明是否可以退款",
  "toolEnabled": true,
  "tenantId": "tenant-a",
  "operatorId": "user-1001",
  "businessScene": "订单售后咨询"
}
```

响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "conversationId": "conv-10001",
    "answer": "订单 ORD202605110001 当前状态为已支付，金额为 199.90 元。根据退款规则，已支付订单支持发起退款；如果订单已发货，则需要先完成退货审核。",
    "toolEnabled": true,
    "businessScene": "订单售后咨询",
    "costMillis": 1842
  }
}
```

curl 调用示例：

```bash
curl -X POST 'http://localhost:8080/api/ai/mcp/chat' \
  -H 'Content-Type: application/json' \
  -d '{
    "conversationId": "conv-10001",
    "message": "帮我查询订单 ORD202605110001 的状态，并说明是否可以退款",
    "toolEnabled": true,
    "tenantId": "tenant-a",
    "operatorId": "user-1001",
    "businessScene": "订单售后咨询"
  }'
```

参数设计建议如下：

| 字段             | 是否必填 | 说明                               |
| ---------------- | -------- | ---------------------------------- |
| `conversationId` | 否       | 会话 ID，为空时服务端生成          |
| `message`        | 是       | 用户输入内容                       |
| `toolEnabled`    | 否       | 是否启用 MCP 工具，默认启用        |
| `tenantId`       | 否       | 租户 ID，用于工具上下文和权限控制  |
| `operatorId`     | 否       | 操作人 ID，用于审计和权限控制      |
| `businessScene`  | 否       | 业务场景，用于系统提示词和日志分类 |

响应结构建议包含以下字段：

| 字段             | 说明                             |
| ---------------- | -------------------------------- |
| `conversationId` | 用于多轮会话、日志追踪和前端关联 |
| `answer`         | 模型最终回答                     |
| `toolEnabled`    | 当前请求是否启用 MCP 工具        |
| `businessScene`  | 当前业务场景                     |
| `costMillis`     | 请求耗时，便于排查慢调用         |

### 异常处理

MCP 与大模型集成后，异常来源会比普通接口更多，至少包括参数校验异常、模型调用异常、MCP 连接异常、工具不存在异常、工具执行异常、资源读取异常、Prompt 获取异常和业务权限异常。异常处理不应直接把底层堆栈返回给前端，而应转成稳定的业务错误码和错误消息。

建议异常分类如下：

| 异常类型      | 典型原因                       | 处理方式              |
| ------------- | ------------------------------ | --------------------- |
| 参数异常      | 用户消息为空、订单号为空       | 返回 400              |
| MCP 连接异常  | Server 未启动、URL 错误、超时  | 返回 502 或业务错误码 |
| Tool 调用异常 | 工具不存在、参数 Schema 不匹配 | 返回工具调用失败      |
| Resource 异常 | URI 不存在、读取失败           | 返回资源读取失败      |
| 模型异常      | API Key 错误、限流、模型超时   | 返回模型服务异常      |
| 权限异常      | 租户不匹配、无操作权限         | 返回 403              |
| 未知异常      | 代码缺陷或不可预期错误         | 返回 500              |

文件位置：`src/main/java/io/github/atengk/mcp/chat/exception/McpBusinessException.java`

```java
package io.github.atengk.mcp.chat.exception;

import lombok.Getter;

/**
 * MCP 业务异常。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Getter
public class McpBusinessException extends RuntimeException {

    /**
     * 错误编码。
     */
    private final Integer code;

    /**
     * 创建 MCP 业务异常。
     *
     * @param message 错误消息
     */
    public McpBusinessException(String message) {
        super(message);
        this.code = 500;
    }

    /**
     * 创建 MCP 业务异常。
     *
     * @param code 错误编码
     * @param message 错误消息
     */
    public McpBusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}
```

文件位置：`src/main/java/io/github/atengk/mcp/chat/exception/GlobalExceptionHandler.java`

下面的全局异常处理器统一处理参数校验、业务异常和未知异常，避免底层 MCP 或模型异常直接暴露给调用方。

```java
package io.github.atengk.mcp.chat.exception;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.mcp.chat.common.ApiResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理 MCP 业务异常。
     *
     * @param e 业务异常
     * @return 统一响应
     */
    @ExceptionHandler(McpBusinessException.class)
    public ApiResult<Void> handleMcpBusinessException(McpBusinessException e) {
        log.warn("MCP业务异常：{}", e.getMessage());
        return ApiResult.fail(e.getCode(), e.getMessage());
    }

    /**
     * 处理请求体参数校验异常。
     *
     * @param e 参数校验异常
     * @return 统一响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResult<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = getFieldErrorMessage(e.getBindingResult().getFieldError());
        log.warn("请求参数校验失败：{}", message);
        return ApiResult.fail(400, message);
    }

    /**
     * 处理绑定参数校验异常。
     *
     * @param e 参数绑定异常
     * @return 统一响应
     */
    @ExceptionHandler(BindException.class)
    public ApiResult<Void> handleBindException(BindException e) {
        String message = getFieldErrorMessage(e.getBindingResult().getFieldError());
        log.warn("请求参数绑定失败：{}", message);
        return ApiResult.fail(400, message);
    }

    /**
     * 处理 AI 非瞬时异常。
     *
     * @param e AI 异常
     * @return 统一响应
     */
    @ExceptionHandler(NonTransientAiException.class)
    public ApiResult<Void> handleNonTransientAiException(NonTransientAiException e) {
        log.error("AI模型调用失败：{}", e.getMessage(), e);
        return ApiResult.fail(502, "AI模型调用失败，请检查模型配置、密钥或请求参数");
    }

    /**
     * 处理未知异常。
     *
     * @param e 未知异常
     * @return 统一响应
     */
    @ExceptionHandler(Exception.class)
    public ApiResult<Void> handleException(Exception e) {
        log.error("系统未知异常", e);
        return ApiResult.fail(500, "系统繁忙，请稍后重试");
    }

    /**
     * 获取字段校验错误消息。
     *
     * @param fieldError 字段错误
     * @return 错误消息
     */
    private String getFieldErrorMessage(FieldError fieldError) {
        if (fieldError == null) {
            return "请求参数不合法";
        }

        String defaultMessage = fieldError.getDefaultMessage();
        if (StrUtil.isNotBlank(defaultMessage)) {
            return defaultMessage;
        }

        if (CollUtil.isNotEmpty(fieldError.getCodes())) {
            return fieldError.getField() + "参数不合法";
        }

        return "请求参数不合法";
    }
}
```

异常处理注意事项：

1. MCP Server 不可用时，不要让模型继续编造业务结果，应直接返回明确错误或提示切换为无工具模式。
2. Tool 调用失败时，需要记录工具名称、会话 ID、业务场景、操作者 ID，但不要记录敏感参数。
3. 大模型调用异常应与 MCP 工具异常区分，便于定位是模型服务问题还是 MCP Server 问题。
4. 生产环境日志中不要输出 API Key、Token、身份证号、手机号等敏感信息。
5. 对写操作 Tool，应增加业务权限、幂等控制和人工确认流程，不能只依赖模型判断。

## 配置说明

本章节用于统一说明 MCP Server、MCP Client、大模型和业务接口相关配置。Spring AI MCP Client Boot Starter 支持多客户端实例、自动初始化、多种传输方式、ToolCallback 集成、工具过滤和生命周期管理；MCP Server Boot Starter 支持工具、资源、提示词自动配置，以及 STDIO、SSE、Streamable HTTP、Stateless Streamable HTTP 等协议模式。([Home](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-client-boot-starter-docs.html))

### application 配置项

`application.yml` 是 Spring AI MCP 项目的核心配置文件。建议按“应用基础配置、模型配置、MCP Server 配置、MCP Client 配置、日志配置”进行分组，避免后续多环境配置时混乱。

文件位置：`src/main/resources/application.yml`

下面的配置适合一个同时具备 MCP Client 和 MCP Server 能力的示例工程。生产环境中更建议将 Client 应用和 Server 应用拆成两个服务。

```yaml
server:
  # 当前应用端口
  port: 8080

spring:
  application:
    # 应用名称，用于日志、MCP Client 名称和服务识别
    name: spring-ai-mcp-demo

  ai:
    openai:
      # 大模型 API Key，必须通过环境变量注入，不能写死到代码仓库
      api-key: ${OPENAI_API_KEY}
      chat:
        options:
          # 默认聊天模型，按实际供应商和账号权限调整
          model: gpt-4o-mini
          # 低温度适合业务问答和工具调用场景
          temperature: 0.2

    mcp:
      server:
        # 是否启用 MCP Server，默认由 Starter 自动装配
        enabled: true

        # 服务端名称，建议与 spring.application.name 保持一致或体现业务域
        name: order-mcp-server

        # 服务端版本，建议与应用版本同步
        version: 1.0.0

        # 服务端类型：SYNC 或 ASYNC
        # SYNC 只注册同步 MCP 注解方法；ASYNC 只注册响应式方法
        type: SYNC

        # WebMVC / WebFlux 模式下的协议：SSE、STREAMABLE、STATELESS
        # STDIO 模式不依赖该配置
        protocol: STREAMABLE

        # STDIO 模式开关，HTTP 模式下保持 false 或不配置
        stdio: false

        annotation-scanner:
          # 启用 MCP 注解扫描，自动注册 @McpTool、@McpResource、@McpPrompt
          enabled: true

        capabilities:
          # 是否暴露 Tool 能力
          tool: true
          # 是否暴露 Resource 能力
          resource: true
          # 是否暴露 Prompt 能力
          prompt: true
          # 是否暴露自动补全能力
          completion: false

      client:
        # 是否启用 MCP Client
        enabled: true

        # MCP Client 名称
        name: ai-chat-client

        # MCP Client 版本
        version: 1.0.0

        # 是否在创建客户端时自动初始化
        initialized: true

        # 客户端类型：SYNC 或 ASYNC；同一个应用中不应混用
        type: SYNC

        # MCP 请求超时时间，默认值是 20s
        request-timeout: 30s

        # 是否启用 root change notification
        root-change-notification: true

        toolcallback:
          # 启用后，MCP Server 暴露的 Tools 会转换为 Spring AI ToolCallback
          enabled: true

        streamable-http:
          connections:
            order-server:
              # Streamable HTTP MCP Server 基础地址
              url: http://localhost:8081
              # Streamable HTTP 端点，默认是 /mcp
              endpoint: /mcp

management:
  endpoints:
    web:
      exposure:
        # 暴露健康检查端点，便于本地验证和容器探针使用
        include: health,info
  endpoint:
    health:
      # 显示详细健康信息，生产环境可按安全要求关闭
      show-details: always

logging:
  level:
    # 当前项目日志级别
    io.github.atengk: debug
    # Spring AI 相关日志，联调时可设置为 debug
    org.springframework.ai: info
    # MCP SDK 日志，排查协议连接问题时可临时改为 debug
    io.modelcontextprotocol: info
```

常用配置项说明如下：

| 配置项                                            | 说明                       | 建议值                            |
| ------------------------------------------------- | -------------------------- | --------------------------------- |
| `spring.ai.mcp.server.type`                       | MCP Server 类型            | `SYNC` 或 `ASYNC`                 |
| `spring.ai.mcp.server.protocol`                   | HTTP Server 协议           | `SSE`、`STREAMABLE`、`STATELESS`  |
| `spring.ai.mcp.server.stdio`                      | 是否启用 STDIO Server      | 本地进程模式设为 `true`           |
| `spring.ai.mcp.server.annotation-scanner.enabled` | 是否自动扫描 MCP 注解      | `true`                            |
| `spring.ai.mcp.server.capabilities.tool`          | 是否暴露 Tools             | 按业务需要配置                    |
| `spring.ai.mcp.server.capabilities.resource`      | 是否暴露 Resources         | 按业务需要配置                    |
| `spring.ai.mcp.server.capabilities.prompt`        | 是否暴露 Prompts           | 按业务需要配置                    |
| `spring.ai.mcp.client.type`                       | MCP Client 类型            | `SYNC` 或 `ASYNC`                 |
| `spring.ai.mcp.client.request-timeout`            | MCP Client 请求超时        | `20s` 到 `60s`                    |
| `spring.ai.mcp.client.toolcallback.enabled`       | 是否启用 ToolCallback 集成 | 需要接入 ChatClient 时设为 `true` |

MCP Client 官方配置说明中，`spring.ai.mcp.client.enabled` 默认启用，`name` 默认是 `spring-ai-mcp-client`，`version` 默认是 `1.0.0`，`initialized` 默认启用，`request-timeout` 默认是 `20s`，`type` 默认是 `SYNC`，`toolcallback.enabled` 默认启用。([Home](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-client-boot-starter-docs.html))

MCP Server 官方文档说明，STDIO 使用 `spring-ai-starter-mcp-server` 并设置 `spring.ai.mcp.server.stdio=true`；WebMVC / WebFlux 的 SSE、Streamable HTTP、Stateless 模式分别通过 `spring.ai.mcp.server.protocol=SSE`、`STREAMABLE`、`STATELESS` 控制。([Home](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-server-boot-starter-docs.html))

### 多 MCP Server 配置

实际项目中，一个 AI 应用通常需要连接多个 MCP Server。例如订单 MCP Server 提供订单查询，知识库 MCP Server 提供文档检索，运维 MCP Server 提供系统检查。Spring AI MCP Client 支持多个命名连接，每个连接会创建对应的 MCP Client 实例。标准 MCP Client Starter 支持 STDIO、SSE、Streamable HTTP 和 Stateless Streamable HTTP，并且每个 MCP Server 连接会创建一个新的 MCP Client 实例。([Home](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-client-boot-starter-docs.html))

文件位置：`src/main/resources/application-multi-mcp.yml`

下面的配置同时连接三个 Streamable HTTP MCP Server。

```yaml
spring:
  ai:
    mcp:
      client:
        enabled: true
        type: SYNC
        request-timeout: 30s
        toolcallback:
          enabled: true

        streamable-http:
          connections:
            order-server:
              # 订单 MCP Server
              url: http://localhost:8081
              endpoint: /mcp

            knowledge-server:
              # 知识库 MCP Server
              url: http://localhost:8082
              endpoint: /mcp

            ops-server:
              # 运维 MCP Server
              url: http://localhost:8083
              endpoint: /mcp
```

如果同时存在 SSE MCP Server，可以增加 `sse.connections` 配置。SSE 配置中需要将完整 URL 拆成基础地址 `url` 和路径 `sse-endpoint`；官方文档说明，SSE 默认端点是 `/sse`，Streamable HTTP 默认端点是 `/mcp`。([Home](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-client-boot-starter-docs.html))

```yaml
spring:
  ai:
    mcp:
      client:
        sse:
          connections:
            event-server:
              # SSE MCP Server 基础地址
              url: http://localhost:8090
              # SSE 端点，默认是 /sse
              sse-endpoint: /sse
```

如果使用本地 STDIO MCP Server，可以使用内联配置或外部 JSON 文件。官方文档说明，STDIO 配置前缀是 `spring.ai.mcp.client.stdio`，支持 `servers-configuration` 指向 Claude Desktop 格式的 JSON 文件，也支持 `connections.[name].command`、`args`、`env` 直接配置。([Home](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-client-boot-starter-docs.html))

```yaml
spring:
  ai:
    mcp:
      client:
        stdio:
          # 使用外部 JSON 文件维护本地 MCP Server
          servers-configuration: classpath:mcp-servers.json
```

文件位置：`src/main/resources/mcp-servers.json`

```json
{
  "mcpServers": {
    "local-order-server": {
      "command": "java",
      "args": [
        "-jar",
        "/opt/mcp/order-mcp-server.jar",
        "--spring.profiles.active=stdio"
      ],
      "env": {
        "APP_ENV": "dev"
      }
    },
    "filesystem-server": {
      "command": "npx",
      "args": [
        "-y",
        "@modelcontextprotocol/server-filesystem",
        "/data/documents"
      ]
    }
  }
}
```

多 MCP Server 配置时需要注意工具命名冲突。不同 Server 可能暴露同名 Tool，例如多个服务都有 `search`、`query`、`get_config`。Spring AI MCP Client 支持工具名称前缀生成和工具过滤能力，用于选择性包含或排除工具，并避免工具命名冲突。([Home](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-client-boot-starter-docs.html))

多 Server 建议命名规范如下：

| Server 名称        | Tool 命名前缀 | 示例 Tool                  |
| ------------------ | ------------- | -------------------------- |
| `order-server`     | `order_`      | `order_query_summary`      |
| `knowledge-server` | `kb_`         | `kb_search_document`       |
| `ops-server`       | `ops_`        | `ops_check_service_health` |
| `config-server`    | `config_`     | `config_get_property`      |

### 超时与连接参数

MCP 调用链路通常比普通 HTTP 接口更长，可能包含模型推理、工具选择、MCP Client 调用、MCP Server 执行业务逻辑、结果返回和模型二次生成。因此超时配置应分层设置，不能只设置一个全局超时时间。

建议按以下层级配置：

| 层级            | 配置内容                               | 建议值                         |
| --------------- | -------------------------------------- | ------------------------------ |
| MCP Client      | `spring.ai.mcp.client.request-timeout` | `20s` 到 `60s`                 |
| 模型调用        | 模型供应商自身 timeout / retry 配置    | 根据供应商设置                 |
| 业务接口        | Controller 或网关超时                  | 应大于模型 + MCP 总耗时        |
| MCP Server Tool | 业务方法内部超时                       | 查询类 3s 到 10s，分析类可更长 |
| HTTP 网关       | Nginx / Gateway timeout                | 应覆盖最长合法调用             |

推荐配置如下：

```yaml
spring:
  ai:
    mcp:
      client:
        # MCP Client 单次请求超时时间
        request-timeout: 30s

    openai:
      chat:
        options:
          # 业务问答建议低温度，减少不稳定输出
          temperature: 0.2

server:
  servlet:
    session:
      # 会话超时配置，与 MCP 请求超时无直接关系
      timeout: 30m
```

如果某些工具本身执行较慢，例如知识库检索、日志分析、报表生成，应在工具实现内部增加业务级超时和日志。不要把所有 MCP 请求超时都调得过大，否则模型调用会长时间阻塞。

文件位置：`src/main/java/io/github/atengk/mcp/common/config/AsyncTimeoutConfig.java`

下面的配置类定义一个业务线程池，适合在 Tool 内部执行可控的异步任务或带超时的外部接口调用。

```java
package io.github.atengk.mcp.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * MCP 业务线程池配置。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Configuration
public class AsyncTimeoutConfig {

    /**
     * 创建 MCP 业务线程池。
     *
     * @return 业务线程池
     */
    @Bean(destroyMethod = "shutdown")
    public ExecutorService mcpBusinessExecutor() {
        log.info("初始化MCP业务线程池");
        return Executors.newFixedThreadPool(8);
    }
}
```

文件位置：`src/main/java/io/github/atengk/mcp/server/tool/SlowQueryTool.java`

下面的 Tool 示例演示如何对慢查询增加业务超时，避免 MCP 调用长期阻塞。

```java
package io.github.atengk.mcp.server.tool;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 慢查询 MCP 工具。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SlowQueryTool {

    private final ExecutorService mcpBusinessExecutor;

    /**
     * 执行带超时控制的慢查询。
     *
     * @param keyword 查询关键字
     * @return 查询结果
     */
    @McpTool(
            name = "demo_slow_query",
            description = "演示带业务超时控制的慢查询工具"
    )
    public String slowQuery(
            @McpToolParam(description = "查询关键字", required = true)
            String keyword) {

        try {
            log.info("开始执行MCP慢查询，关键字：{}", keyword);

            return mcpBusinessExecutor
                    .submit(() -> doSlowQuery(keyword))
                    .get(5, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            log.warn("MCP慢查询超时，关键字：{}", keyword);
            throw new IllegalStateException("查询超时，请缩小查询范围或稍后重试");
        } catch (Exception e) {
            log.error("MCP慢查询失败，关键字：{}", keyword, e);
            throw new IllegalStateException("查询失败，请稍后重试");
        }
    }

    /**
     * 模拟慢查询逻辑。
     *
     * @param keyword 查询关键字
     * @return 查询结果
     */
    private String doSlowQuery(String keyword) {
        return "查询完成，关键字：" + keyword;
    }
}
```

超时配置建议如下：

1. 查询类 Tool 应设置较短超时，避免模型长时间等待。
2. 写操作 Tool 应设置幂等键、业务锁和审计日志。
3. 多 MCP Server 场景下，不同 Server 可拆分不同应用配置文件。
4. MCP Client 超时应小于网关超时，避免网关先断开而后端仍在执行。
5. 排查期间可将 `org.springframework.ai` 和 `io.modelcontextprotocol` 日志临时调整为 `debug`。

## 功能验证

本章节用于说明 MCP Server、MCP Client 和大模型联调的验证方式。建议按“先 Server、再 Client、最后大模型”的顺序验证，避免把协议连接问题、工具注册问题和模型工具调用问题混在一起。

### MCP Server 启动验证

MCP Server 启动验证的目标是确认服务端可以正常启动，并且 Tool、Resource、Prompt 能被 Spring AI 自动扫描注册。MCP Server Boot Starter 支持注解式服务端开发，自动扫描带有 MCP 注解的 Bean，创建对应 specification，并注册到 MCP Server。([Home](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-server-boot-starter-docs.html))

启动服务端：

```bash
# 编译项目
mvn clean package -DskipTests

# 启动 HTTP MCP Server
java -jar target/order-mcp-server.jar --spring.profiles.active=server
```

启动后检查健康状态：

```bash
# 检查 Spring Boot 应用健康状态
curl http://localhost:8081/actuator/health
```

预期返回：

```json
{
  "status": "UP"
}
```

服务端启动后重点检查以下内容：

| 检查项       | 预期结果                                                     |
| ------------ | ------------------------------------------------------------ |
| 应用进程     | 正常启动，无端口冲突                                         |
| Spring Bean  | Tool、Resource、Prompt 类被扫描为 Bean                       |
| MCP 注解扫描 | `annotation-scanner.enabled=true`                            |
| Server 类型  | `SYNC` 方法匹配 `SYNC` Server，响应式方法匹配 `ASYNC` Server |
| 协议配置     | HTTP 模式使用 `STREAMABLE`、`SSE` 或 `STATELESS`             |
| 能力开关     | `capabilities.tool/resource/prompt` 未被误关闭               |

如果需要在启动时打印本地关键配置，可以增加一个配置检查器。

文件位置：`src/main/java/io/github/atengk/mcp/server/runner/McpServerConfigCheckRunner.java`

下面的启动检查器用于在应用启动后打印 MCP Server 的关键配置，便于确认当前服务运行模式。

```java
package io.github.atengk.mcp.server.runner;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * MCP Server 配置检查器。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Component
public class McpServerConfigCheckRunner implements ApplicationRunner {

    @Value("${spring.application.name:unknown}")
    private String applicationName;

    @Value("${spring.ai.mcp.server.type:SYNC}")
    private String serverType;

    @Value("${spring.ai.mcp.server.protocol:}")
    private String protocol;

    @Value("${spring.ai.mcp.server.stdio:false}")
    private Boolean stdio;

    @Value("${spring.ai.mcp.server.annotation-scanner.enabled:true}")
    private Boolean annotationScannerEnabled;

    /**
     * 应用启动后打印 MCP Server 配置。
     *
     * @param args 启动参数
     */
    @Override
    public void run(ApplicationArguments args) {
        log.info("MCP Server启动配置检查，应用：{}，类型：{}，协议：{}，STDIO：{}，注解扫描：{}",
                applicationName, serverType, protocol, stdio, annotationScannerEnabled);
    }
}
```

### MCP Client 调用验证

MCP Client 调用验证的目标是确认客户端能够连接 MCP Server，并成功执行 Tool、读取 Resource、获取 Prompt。Spring AI MCP Client Boot Starter 支持自动初始化客户端、多个命名 transport、MCP ToolCallback 集成以及客户端生命周期管理。([Home](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-client-boot-starter-docs.html))

建议先使用测试类直接验证 MCP Client，不要一开始就接入大模型。这样可以确认问题是否出在 MCP 连接层。

文件位置：`src/test/java/io/github/atengk/mcp/client/McpClientInvokeTests.java`

下面的测试类依次验证 Tool 列表、Tool 调用、Resource 读取和 Prompt 获取。

```java
package io.github.atengk.mcp.client;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

/**
 * MCP Client 调用验证测试。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@SpringBootTest
class McpClientInvokeTests {

    @Autowired
    private List<McpSyncClient> mcpSyncClients;

    @Autowired(required = false)
    private SyncMcpToolCallbackProvider toolCallbackProvider;

    /**
     * 验证 MCP Tool 列表。
     */
    @Test
    void listTools() {
        McpSyncClient client = getFirstClient();

        McpSchema.ListToolsResult result = client.listTools();
        log.info("MCP工具列表：{}", JSONUtil.toJsonStr(result.tools()));

        Assertions.assertTrue(CollUtil.isNotEmpty(result.tools()), "MCP工具列表不能为空");
    }

    /**
     * 验证 MCP Tool 调用。
     */
    @Test
    void callOrderTool() {
        McpSyncClient client = getFirstClient();

        McpSchema.CallToolResult result = client.callTool(
                new McpSchema.CallToolRequest(
                        "order_query_summary",
                        Map.of("orderNo", "ORD202605110001")
                )
        );

        log.info("MCP工具调用结果：{}", JSONUtil.toJsonStr(result));
        Assertions.assertNotNull(result, "MCP工具调用结果不能为空");
    }

    /**
     * 验证 MCP Resource 读取。
     */
    @Test
    void readResource() {
        McpSyncClient client = getFirstClient();

        McpSchema.ReadResourceResult result = client.readResource(
                new McpSchema.ReadResourceRequest("biz-rule://order-refund")
        );

        log.info("MCP资源读取结果：{}", JSONUtil.toJsonStr(result));
        Assertions.assertNotNull(result, "MCP资源读取结果不能为空");
        Assertions.assertTrue(CollUtil.isNotEmpty(result.contents()), "MCP资源内容不能为空");
    }

    /**
     * 验证 MCP Prompt 获取。
     */
    @Test
    void getPrompt() {
        McpSyncClient client = getFirstClient();

        McpSchema.GetPromptResult result = client.getPrompt(
                new McpSchema.GetPromptRequest(
                        "order_issue_analysis",
                        Map.of(
                                "orderNo", "ORD202605110001",
                                "question", "这个订单是否可以退款？"
                        )
                )
        );

        log.info("MCP Prompt获取结果：{}", JSONUtil.toJsonStr(result));
        Assertions.assertNotNull(result, "MCP Prompt结果不能为空");
        Assertions.assertTrue(CollUtil.isNotEmpty(result.messages()), "MCP Prompt消息不能为空");
    }

    /**
     * 验证 MCP ToolCallback 自动注册。
     */
    @Test
    void listToolCallbacks() {
        Assertions.assertNotNull(toolCallbackProvider, "SyncMcpToolCallbackProvider不能为空");

        ToolCallback[] callbacks = toolCallbackProvider.getToolCallbacks();
        log.info("MCP ToolCallback数量：{}", callbacks.length);

        Assertions.assertTrue(callbacks.length > 0, "MCP ToolCallback不能为空");
    }

    /**
     * 获取第一个 MCP 同步客户端。
     *
     * @return MCP 同步客户端
     */
    private McpSyncClient getFirstClient() {
        Assertions.assertTrue(CollUtil.isNotEmpty(mcpSyncClients), "MCP同步客户端不能为空");
        return mcpSyncClients.getFirst();
    }
}
```

执行测试：

```bash
# 确认 MCP Server 已启动后执行客户端测试
mvn test -Dtest=McpClientInvokeTests
```

验证通过的标准如下：

1. `listTools()` 能拿到工具列表。
2. `callOrderTool()` 能成功调用 `order_query_summary`。
3. `readResource()` 能读取 `biz-rule://order-refund`。
4. `getPrompt()` 能获取 `order_issue_analysis`。
5. `listToolCallbacks()` 能拿到 Spring AI ToolCallback。

### 大模型联调验证

大模型联调验证的目标是确认 ChatClient 能够在对话中使用 MCP Tool。此时应重点观察两类结果：一是模型是否正确判断需要调用工具，二是工具返回结果是否被模型正确组织成最终回答。

建议先提供一个专门的调试接口。

文件位置：`src/main/java/io/github/atengk/mcp/chat/controller/McpDebugController.java`

下面的调试接口用于验证 ChatClient 与 MCP ToolCallback 的联调结果。

```java
package io.github.atengk.mcp.chat.controller;

import io.github.atengk.mcp.chat.common.ApiResult;
import io.github.atengk.mcp.chat.model.dto.McpChatRequestDTO;
import io.github.atengk.mcp.chat.model.vo.McpChatResponseVO;
import io.github.atengk.mcp.chat.service.McpAiChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * MCP 联调验证接口。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/debug/mcp")
public class McpDebugController {

    private final McpAiChatService mcpAiChatService;

    /**
     * 验证订单查询工具调用。
     *
     * @param request 请求参数
     * @return 对话响应
     */
    @PostMapping("/chat")
    public ApiResult<McpChatResponseVO> chat(@Valid @RequestBody McpChatRequestDTO request) {
        log.info("开始执行MCP大模型联调验证，业务场景：{}", request.getBusinessScene());
        return ApiResult.success(mcpAiChatService.chat(request));
    }
}
```

联调请求示例：

```bash
curl -X POST 'http://localhost:8080/api/debug/mcp/chat' \
  -H 'Content-Type: application/json' \
  -d '{
    "message": "请查询订单 ORD202605110001 的状态和金额，并判断是否可以直接退款",
    "toolEnabled": true,
    "tenantId": "tenant-a",
    "operatorId": "debug-user",
    "businessScene": "MCP联调验证"
  }'
```

预期响应结构：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "conversationId": "e5c3f4d9d2a04c8caef00f31a76a4d9b",
    "answer": "订单 ORD202605110001 当前状态为 PAID，金额为 199.90 元。根据订单退款规则，已支付订单通常支持发起退款；如果订单已发货，则需要先完成退货审核。",
    "toolEnabled": true,
    "businessScene": "MCP联调验证",
    "costMillis": 2360
  }
}
```

大模型联调时建议开启以下日志：

```yaml
logging:
  level:
    # 查看 Spring AI 调用链路
    org.springframework.ai: debug
    # 查看 MCP 协议和客户端连接信息
    io.modelcontextprotocol: debug
    # 当前业务代码日志
    io.github.atengk: debug
```

大模型联调通过标准如下：

| 验证点            | 预期结果                                     |
| ----------------- | -------------------------------------------- |
| ChatClient 初始化 | 携带 MCP ToolCallback 的 ChatClient 正常创建 |
| ToolCallback 数量 | 数量大于 0                                   |
| 模型请求          | 请求中包含可用工具定义                       |
| 工具选择          | 模型能根据问题选择订单查询 Tool              |
| 工具执行          | MCP Server 能收到并执行 Tool 调用            |
| 最终回答          | 模型基于工具结果回答，而不是编造业务数据     |
| 异常处理          | MCP 失败时返回明确错误或降级提示             |

### 常见问题排查

MCP 问题排查建议按层级处理：先确认依赖，再确认配置，再确认连接，再确认工具注册，最后排查模型是否选择工具。

| 问题                   | 常见原因                                         | 处理方式                                                     |
| ---------------------- | ------------------------------------------------ | ------------------------------------------------------------ |
| MCP Server 启动失败    | Starter 依赖不匹配、端口冲突、配置错误           | 检查 `pom.xml`、端口、`spring.ai.mcp.server.protocol`        |
| Tool 未注册            | 类不是 Spring Bean、注解扫描关闭、方法类型不匹配 | 检查 `@Component`、`annotation-scanner.enabled`、`SYNC/ASYNC` |
| Resource 读取失败      | URI 错误、资源不存在、MIME 类型异常              | 检查 Resource URI 和服务端日志                               |
| Prompt 获取失败        | Prompt 名称错误、参数缺失                        | 检查 `@McpPrompt` 名称和参数                                 |
| Client 连接失败        | URL、endpoint、协议不匹配                        | 检查 `/mcp`、`/sse`、`STREAMABLE`、`SSE` 配置                |
| ToolCallback 为空      | `toolcallback.enabled=false` 或服务端无 Tool     | 开启 ToolCallback，并确认 `listTools()` 有结果               |
| 模型不调用工具         | 工具描述不清晰、问题不需要工具、模型能力不支持   | 优化 Tool 名称和描述，检查模型 Tool Calling 支持             |
| 工具调用参数错误       | 参数名不匹配、Schema 描述不清晰                  | 检查 `@McpToolParam` 和方法参数名                            |
| 调用超时               | MCP Server 慢、外部接口慢、模型等待过长          | 调整 `request-timeout`，给 Tool 增加业务超时                 |
| Windows STDIO 启动失败 | `npx`、`npm`、`mvn` 是 `.cmd` 脚本               | 使用 `cmd.exe /c` 包装命令                                   |

Windows 使用 STDIO 时要特别注意。官方文档说明，Windows 下 `npx`、`npm`、`node`、`mvn`、`gradle` 等命令通常是批处理文件，Java `ProcessBuilder` 不能直接执行这些 `.cmd` 文件，需要使用 `cmd.exe /c` 包装。([Home](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-client-boot-starter-docs.html))

Windows STDIO 配置示例：

```json
{
  "mcpServers": {
    "filesystem": {
      "command": "cmd.exe",
      "args": [
        "/c",
        "npx",
        "-y",
        "@modelcontextprotocol/server-filesystem",
        "C:\\Users\\ateng\\Desktop"
      ]
    }
  }
}
```

Linux / macOS STDIO 配置示例：

```json
{
  "mcpServers": {
    "filesystem": {
      "command": "npx",
      "args": [
        "-y",
        "@modelcontextprotocol/server-filesystem",
        "/Users/ateng/Desktop"
      ]
    }
  }
}
```

排查顺序建议如下：

1. 使用 `mvn dependency:tree` 检查 Spring AI 和 MCP 依赖是否冲突。
2. 启动 MCP Server，确认应用健康状态为 `UP`。
3. 启动 MCP Client，确认没有连接超时或 endpoint 404。
4. 使用测试类执行 `listTools()`，确认服务端工具已暴露。
5. 使用测试类执行 `callTool()`，确认工具可以直接调用。
6. 使用测试类读取 Resource 和 Prompt。
7. 检查 `SyncMcpToolCallbackProvider` 是否能返回 ToolCallback。
8. 最后再通过 ChatClient 发起大模型请求，观察模型是否选择工具。

依赖排查命令：

```bash
# 查看 Spring AI 相关依赖
mvn dependency:tree | grep spring-ai

# 查看 MCP Java SDK 相关依赖
mvn dependency:tree | grep modelcontextprotocol

# 查看项目是否存在多个 Spring AI 版本
mvn dependency:tree -Dincludes=org.springframework.ai
```

端口与接口排查命令：

```bash
# 检查端口是否被占用
lsof -i :8081

# 检查服务健康状态
curl http://localhost:8081/actuator/health

# 查看应用启动日志中的 MCP 相关信息
grep -i "mcp" logs/app.log
```

最终验证通过后，建议将以下内容纳入项目交付检查：

1. MCP Server 和 MCP Client 配置文件已按环境拆分。
2. Tool、Resource、Prompt 均有测试用例覆盖。
3. Tool 描述、参数描述清晰，能够帮助模型正确选择工具。
4. MCP Client 超时、模型调用超时、网关超时已统一规划。
5. 生产环境日志不输出密钥、Token、身份证号、手机号等敏感信息。
6. 写操作 Tool 已增加权限校验、幂等控制和审计日志。

## 示例工程

本章节给出一个最小可运行的 Spring AI 2.x MCP 示例工程。示例采用双模块结构：`mcp-order-server` 负责暴露 MCP Tool、Resource、Prompt；`ai-chat-client` 负责连接 MCP Server，并通过 `ChatClient` 调用大模型和 MCP 工具。Spring AI MCP Server Starter 支持工具、资源、提示词自动配置和注解扫描，MCP Client Starter 支持多个 MCP Server 连接、STDIO / SSE / Streamable HTTP 传输以及 ToolCallback 集成。([Home](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-server-boot-starter-docs.html?utm_source=chatgpt.com))

### 工程目录结构

示例工程采用 Maven 多模块结构，便于将 MCP Server 和 MCP Client 的职责拆开。生产项目中也建议采用类似拆分方式，避免服务端工具暴露逻辑和客户端模型调用逻辑耦合。

```text
spring-ai-mcp-example
├── pom.xml
├── mcp-order-server
│   ├── pom.xml
│   └── src
│       ├── main
│       │   ├── java
│       │   │   └── io/github/atengk/mcp/server
│       │   │       ├── OrderMcpServerApplication.java
│       │   │       ├── model
│       │   │       │   └── vo
│       │   │       │       └── OrderSummaryVO.java
│       │   │       ├── prompt
│       │   │       │   └── OrderPromptProvider.java
│       │   │       ├── resource
│       │   │       │   └── OrderRuleResource.java
│       │   │       └── tool
│       │   │           └── OrderInfoTool.java
│       │   └── resources
│       │       └── application.yml
│       └── test
│           └── java
│               └── io/github/atengk/mcp/server
│                   └── OrderMcpServerApplicationTests.java
└── ai-chat-client
    ├── pom.xml
    └── src
        ├── main
        │   ├── java
        │   │   └── io/github/atengk/mcp/client
        │   │       ├── AiChatClientApplication.java
        │   │       ├── common
        │   │       │   └── ApiResult.java
        │   │       ├── config
        │   │       │   └── ChatClientConfig.java
        │   │       ├── controller
        │   │       │   └── OrderChatController.java
        │   │       ├── model
        │   │       │   ├── dto
        │   │       │   │   └── OrderChatRequestDTO.java
        │   │       │   └── vo
        │   │       │       └── OrderChatResponseVO.java
        │   │       └── service
        │   │           ├── OrderChatService.java
        │   │           └── impl
        │   │               └── OrderChatServiceImpl.java
        │   └── resources
        │       └── application.yml
        └── test
            └── java
                └── io/github/atengk/mcp/client
                    ├── McpClientInvokeTests.java
                    └── OrderChatServiceTests.java
```

模块职责如下：

| 模块               | 端口   | 职责                                                  |
| ------------------ | ------ | ----------------------------------------------------- |
| `mcp-order-server` | `8081` | 暴露订单查询 Tool、订单规则 Resource、订单分析 Prompt |
| `ai-chat-client`   | `8080` | 连接 MCP Server，注册 ToolCallback，提供 AI 对话接口  |

### 核心代码说明

本示例只保留最小闭环代码：服务端暴露一个订单查询 Tool、一个订单规则 Resource、一个订单分析 Prompt；客户端通过 Streamable HTTP 连接服务端，并将 MCP ToolCallback 注入到 `ChatClient` 中。

根工程 `pom.xml` 用于统一管理 Spring Boot、Spring AI、Hutool 和模块版本。

文件位置：`pom.xml`

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>io.github.atengk</groupId>
    <artifactId>spring-ai-mcp-example</artifactId>
    <version>1.0.0</version>
    <packaging>pom</packaging>

    <modules>
        <module>mcp-order-server</module>
        <module>ai-chat-client</module>
    </modules>

    <properties>
        <!-- Java 编译版本 -->
        <java.version>21</java.version>

        <!-- Spring Boot 版本，按项目实际依赖基线固定 -->
        <spring-boot.version>3.5.0</spring-boot.version>

        <!-- Spring AI 2.x 版本，生产项目应固定明确版本 -->
        <spring-ai.version>2.0.0-M6</spring-ai.version>

        <!-- Hutool 工具包版本 -->
        <hutool.version>5.8.36</hutool.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <!-- Spring Boot BOM -->
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-dependencies</artifactId>
                <version>${spring-boot.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>

            <dependency>
                <!-- Spring AI BOM，统一管理 Spring AI 依赖版本 -->
                <groupId>org.springframework.ai</groupId>
                <artifactId>spring-ai-bom</artifactId>
                <version>${spring-ai.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>
</project>
```

`mcp-order-server` 模块用于暴露 MCP Server。Spring AI MCP Server 官方文档说明，Streamable HTTP Server 可以通过 WebMVC Starter 启用，并通过 `spring.ai.mcp.server.protocol=STREAMABLE` 配置协议。([Home](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-server-boot-starter-docs.html?utm_source=chatgpt.com))

文件位置：`mcp-order-server/pom.xml`

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>io.github.atengk</groupId>
        <artifactId>spring-ai-mcp-example</artifactId>
        <version>1.0.0</version>
    </parent>

    <artifactId>mcp-order-server</artifactId>

    <dependencies>
        <dependency>
            <!-- WebMVC MCP Server Starter，通过 HTTP 暴露 MCP Server -->
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-starter-mcp-server-webmvc</artifactId>
        </dependency>

        <dependency>
            <!-- Actuator，用于健康检查 -->
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>

        <dependency>
            <!-- Hutool 工具包 -->
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
            <version>${hutool.version}</version>
        </dependency>

        <dependency>
            <!-- Lombok 简化模型和构造器代码 -->
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <dependency>
            <!-- Spring Boot 测试依赖 -->
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

服务端配置使用 Streamable HTTP 协议，并开启 MCP 注解扫描。MCP 注解模块提供 `@McpTool`、`@McpResource`、`@McpPrompt`，可以减少服务端注册样板代码。([Home](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-annotations-overview.html?utm_source=chatgpt.com))

文件位置：`mcp-order-server/src/main/resources/application.yml`

```yaml
server:
  # MCP Server 服务端口
  port: 8081

spring:
  application:
    # MCP Server 应用名称
    name: mcp-order-server

  ai:
    mcp:
      server:
        # 同步服务端，适合传统 Spring MVC 阻塞式业务调用
        type: SYNC

        # Streamable HTTP 协议，客户端通过 /mcp 端点连接
        protocol: STREAMABLE

        annotation-scanner:
          # 自动扫描 @McpTool、@McpResource、@McpPrompt
          enabled: true

        capabilities:
          # 暴露工具能力
          tool: true
          # 暴露资源能力
          resource: true
          # 暴露提示词能力
          prompt: true

management:
  endpoints:
    web:
      exposure:
        # 暴露健康检查端点
        include: health,info

logging:
  level:
    io.github.atengk: debug
    org.springframework.ai: info
    io.modelcontextprotocol: info
```

服务端启动类用于启动 MCP Server 模块。

文件位置：`mcp-order-server/src/main/java/io/github/atengk/mcp/server/OrderMcpServerApplication.java`

```java
package io.github.atengk.mcp.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 订单 MCP Server 启动类。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@SpringBootApplication
public class OrderMcpServerApplication {

    /**
     * 应用入口。
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(OrderMcpServerApplication.class, args);
    }
}
```

订单摘要对象作为 Tool 的结构化返回结果。

文件位置：`mcp-order-server/src/main/java/io/github/atengk/mcp/server/model/vo/OrderSummaryVO.java`

```java
package io.github.atengk.mcp.server.model.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单摘要响应对象。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Data
@Builder
public class OrderSummaryVO {

    /**
     * 订单号。
     */
    private String orderNo;

    /**
     * 订单状态。
     */
    private String status;

    /**
     * 订单金额。
     */
    private BigDecimal amount;

    /**
     * 是否允许退款。
     */
    private Boolean refundable;

    /**
     * 订单说明。
     */
    private String remark;
}
```

订单查询 Tool 用于模拟查询订单状态、金额和退款标识。实际项目中应将示例数据替换为数据库、RPC 或业务 HTTP 接口。

文件位置：`mcp-order-server/src/main/java/io/github/atengk/mcp/server/tool/OrderInfoTool.java`

```java
package io.github.atengk.mcp.server.tool;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.mcp.server.model.vo.OrderSummaryVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 订单信息 MCP 工具。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Component
public class OrderInfoTool {

    /**
     * 根据订单号查询订单摘要。
     *
     * @param orderNo 订单号
     * @return 订单摘要
     */
    @McpTool(
            name = "order_query_summary",
            description = "根据订单号查询订单摘要，返回订单状态、金额和是否允许退款"
    )
    public OrderSummaryVO queryOrderSummary(
            @McpToolParam(description = "订单号，例如：ORD202605110001", required = true)
            String orderNo) {

        if (StrUtil.isBlank(orderNo)) {
            log.warn("MCP订单查询失败，订单号为空");
            throw new IllegalArgumentException("订单号不能为空");
        }

        log.info("执行MCP订单查询，订单号：{}", orderNo);

        if (StrUtil.equalsIgnoreCase(orderNo, "ORD202605110001")) {
            return OrderSummaryVO.builder()
                    .orderNo(orderNo)
                    .status("PAID")
                    .amount(new BigDecimal("199.90"))
                    .refundable(true)
                    .remark("订单已支付，未发货，支持直接退款")
                    .build();
        }

        return OrderSummaryVO.builder()
                .orderNo(orderNo)
                .status("UNKNOWN")
                .amount(BigDecimal.ZERO)
                .refundable(false)
                .remark("未查询到订单，请检查订单号")
                .build();
    }
}
```

订单规则 Resource 用于向模型提供只读业务规则。Resource 适合放业务规则、接口说明、帮助文档等上下文内容。

文件位置：`mcp-order-server/src/main/java/io/github/atengk/mcp/server/resource/OrderRuleResource.java`

```java
package io.github.atengk.mcp.server.resource;

import cn.hutool.core.util.StrUtil;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.mcp.annotation.McpResource;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 订单规则 MCP 资源。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Component
public class OrderRuleResource {

    /**
     * 读取订单规则。
     *
     * @param code 规则编码
     * @return 资源内容
     */
    @McpResource(
            uri = "order-rule://{code}",
            name = "订单业务规则",
            description = "读取订单相关业务规则，例如退款规则、发票规则、售后规则"
    )
    public McpSchema.ReadResourceResult readOrderRule(String code) {
        if (StrUtil.isBlank(code)) {
            log.warn("读取订单规则失败，规则编码为空");
            throw new IllegalArgumentException("规则编码不能为空");
        }

        String uri = "order-rule://" + code;
        String content = switch (code) {
            case "refund" -> """
                    # 订单退款规则
                    
                    1. 已支付且未发货订单支持直接退款。
                    2. 已发货订单需要先发起退货申请。
                    3. 已完成订单超过售后期后不支持自助退款。
                    4. 大额退款需要人工审核。
                    """;
            case "invoice" -> """
                    # 订单发票规则
                    
                    1. 已支付订单支持申请发票。
                    2. 企业发票需要提供纳税人识别号。
                    3. 红冲发票需要关联原发票号码。
                    """;
            default -> """
                    # 未找到规则
                    
                    当前规则编码不存在，请检查 Resource URI。
                    """;
        };

        log.info("读取订单规则资源，URI：{}", uri);

        return new McpSchema.ReadResourceResult(List.of(
                new McpSchema.TextResourceContents(uri, "text/markdown", content)
        ));
    }
}
```

订单 Prompt 用于生成固定结构的订单问题分析提示词。

文件位置：`mcp-order-server/src/main/java/io/github/atengk/mcp/server/prompt/OrderPromptProvider.java`

```java
package io.github.atengk.mcp.server.prompt;

import cn.hutool.core.util.StrUtil;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.mcp.annotation.McpArg;
import org.springframework.ai.mcp.annotation.McpPrompt;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 订单 MCP 提示词提供器。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Component
public class OrderPromptProvider {

    /**
     * 生成订单问题分析提示词。
     *
     * @param orderNo 订单号
     * @param question 用户问题
     * @return Prompt 结果
     */
    @McpPrompt(
            name = "order_issue_analysis",
            description = "根据订单号和用户问题生成订单问题分析提示词"
    )
    public McpSchema.GetPromptResult orderIssueAnalysis(
            @McpArg(name = "orderNo", description = "订单号", required = true)
            String orderNo,
            @McpArg(name = "question", description = "用户问题", required = true)
            String question) {

        if (StrUtil.hasBlank(orderNo, question)) {
            log.warn("生成订单分析Prompt失败，订单号或问题为空");
            throw new IllegalArgumentException("订单号和问题不能为空");
        }

        log.info("生成订单分析Prompt，订单号：{}", orderNo);

        String prompt = """
                你是订单业务分析助手，请根据订单号和用户问题进行分析。
                
                订单号：%s
                用户问题：%s
                
                分析要求：
                1. 优先调用订单查询工具获取真实订单信息。
                2. 如涉及退款，读取 order-rule://refund 规则。
                3. 如涉及发票，读取 order-rule://invoice 规则。
                4. 回答时明确说明依据，不要编造订单事实。
                """.formatted(orderNo, question);

        return new McpSchema.GetPromptResult(
                "订单问题分析",
                List.of(new McpSchema.PromptMessage(
                        McpSchema.Role.USER,
                        new McpSchema.TextContent(prompt)
                ))
        );
    }
}
```

`ai-chat-client` 模块用于连接 MCP Server，并提供大模型对话接口。MCP Client Starter 支持 Streamable HTTP 连接配置，默认端点为 `/mcp`，开启 ToolCallback 后可以通过 `SyncMcpToolCallbackProvider` 获取 MCP Tools 对应的 `ToolCallback`。([Home](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-client-boot-starter-docs.html?utm_source=chatgpt.com))

文件位置：`ai-chat-client/pom.xml`

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>io.github.atengk</groupId>
        <artifactId>spring-ai-mcp-example</artifactId>
        <version>1.0.0</version>
    </parent>

    <artifactId>ai-chat-client</artifactId>

    <dependencies>
        <dependency>
            <!-- Web 接口，用于提供 AI 对话 API -->
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <dependency>
            <!-- 参数校验 -->
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <dependency>
            <!-- OpenAI 模型 Starter，可按实际供应商替换 -->
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-starter-model-openai</artifactId>
        </dependency>

        <dependency>
            <!-- MCP Client Starter，用于连接 MCP Server -->
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-starter-mcp-client</artifactId>
        </dependency>

        <dependency>
            <!-- Actuator，用于健康检查 -->
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>

        <dependency>
            <!-- Hutool 工具包 -->
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
            <version>${hutool.version}</version>
        </dependency>

        <dependency>
            <!-- Lombok 简化模型和构造器代码 -->
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <dependency>
            <!-- Spring Boot 测试依赖 -->
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

客户端配置连接 `mcp-order-server` 的 Streamable HTTP 端点，并开启 MCP ToolCallback 集成。

文件位置：`ai-chat-client/src/main/resources/application.yml`

```yaml
server:
  # AI Client 服务端口
  port: 8080

spring:
  application:
    name: ai-chat-client

  ai:
    openai:
      # 模型密钥通过环境变量传入
      api-key: ${OPENAI_API_KEY}
      chat:
        options:
          # 按实际模型供应商和权限调整
          model: gpt-4o-mini
          temperature: 0.2

    mcp:
      client:
        # 启用 MCP Client
        enabled: true

        # 客户端名称
        name: ai-chat-client

        # 客户端版本
        version: 1.0.0

        # 创建客户端时自动初始化
        initialized: true

        # 同步客户端
        type: SYNC

        # MCP 调用超时时间
        request-timeout: 30s

        toolcallback:
          # 将 MCP Tool 转换为 Spring AI ToolCallback
          enabled: true

        streamable-http:
          connections:
            order-server:
              # MCP Server 基础地址
              url: http://localhost:8081
              # Streamable HTTP 端点
              endpoint: /mcp

management:
  endpoints:
    web:
      exposure:
        include: health,info

logging:
  level:
    io.github.atengk: debug
    org.springframework.ai: info
    io.modelcontextprotocol: info
```

客户端启动类用于启动 AI 对话服务。

文件位置：`ai-chat-client/src/main/java/io/github/atengk/mcp/client/AiChatClientApplication.java`

```java
package io.github.atengk.mcp.client;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * AI Chat Client 启动类。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@SpringBootApplication
public class AiChatClientApplication {

    /**
     * 应用入口。
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(AiChatClientApplication.class, args);
    }
}
```

ChatClient 配置类将 MCP ToolCallback 注册为默认工具。Spring AI 的工具调用文档说明，运行时可通过 `toolCallbacks()` 传入工具，也可以在构建客户端时通过 `defaultToolCallbacks()` 配置默认工具。([Home](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-client-boot-starter-docs.html?utm_source=chatgpt.com))

文件位置：`ai-chat-client/src/main/java/io/github/atengk/mcp/client/config/ChatClientConfig.java`

```java
package io.github.atengk.mcp.client.config;

import cn.hutool.core.util.ArrayUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ChatClient 配置。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Configuration
public class ChatClientConfig {

    /**
     * 创建携带 MCP 工具的 ChatClient。
     *
     * @param builder ChatClient 构建器
     * @param providerProvider MCP ToolCallback 提供器
     * @return ChatClient
     */
    @Bean
    public ChatClient chatClient(
            ChatClient.Builder builder,
            ObjectProvider<SyncMcpToolCallbackProvider> providerProvider) {

        ToolCallback[] toolCallbacks = loadToolCallbacks(providerProvider);

        log.info("初始化ChatClient，MCP工具数量：{}", toolCallbacks.length);

        return builder
                .defaultSystem("""
                        你是订单业务助手。
                        当用户询问订单状态、退款、发票、售后等问题时，可以调用可用的 MCP 工具查询真实信息。
                        如果工具结果不足，请明确说明缺少信息，不要编造订单事实。
                        """)
                .defaultToolCallbacks(toolCallbacks)
                .build();
    }

    /**
     * 加载 MCP ToolCallback。
     *
     * @param providerProvider MCP ToolCallback 提供器
     * @return ToolCallback 数组
     */
    private ToolCallback[] loadToolCallbacks(ObjectProvider<SyncMcpToolCallbackProvider> providerProvider) {
        SyncMcpToolCallbackProvider provider = providerProvider.getIfAvailable();
        if (provider == null) {
            log.warn("未发现MCP ToolCallback提供器");
            return new ToolCallback[0];
        }

        ToolCallback[] callbacks = provider.getToolCallbacks();
        if (ArrayUtil.isEmpty(callbacks)) {
            log.warn("未加载到MCP工具");
            return new ToolCallback[0];
        }

        return callbacks;
    }
}
```

接口返回结构用于统一封装响应。

文件位置：`ai-chat-client/src/main/java/io/github/atengk/mcp/client/common/ApiResult.java`

```java
package io.github.atengk.mcp.client.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 接口统一响应结构。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResult<T> {

    /**
     * 响应编码。
     */
    private Integer code;

    /**
     * 响应消息。
     */
    private String message;

    /**
     * 响应数据。
     */
    private T data;

    /**
     * 成功响应。
     *
     * @param data 响应数据
     * @param <T> 数据类型
     * @return 统一响应
     */
    public static <T> ApiResult<T> success(T data) {
        return new ApiResult<>(200, "操作成功", data);
    }

    /**
     * 失败响应。
     *
     * @param code 响应编码
     * @param message 响应消息
     * @param <T> 数据类型
     * @return 统一响应
     */
    public static <T> ApiResult<T> fail(Integer code, String message) {
        return new ApiResult<>(code, message, null);
    }
}
```

请求对象用于接收订单对话参数。

文件位置：`ai-chat-client/src/main/java/io/github/atengk/mcp/client/model/dto/OrderChatRequestDTO.java`

```java
package io.github.atengk.mcp.client.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 订单对话请求参数。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Data
public class OrderChatRequestDTO {

    /**
     * 订单号。
     */
    @NotBlank(message = "订单号不能为空")
    private String orderNo;

    /**
     * 用户问题。
     */
    @NotBlank(message = "用户问题不能为空")
    private String question;
}
```

响应对象用于返回模型回答和订单号。

文件位置：`ai-chat-client/src/main/java/io/github/atengk/mcp/client/model/vo/OrderChatResponseVO.java`

```java
package io.github.atengk.mcp.client.model.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 订单对话响应结果。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Data
@Builder
public class OrderChatResponseVO {

    /**
     * 订单号。
     */
    private String orderNo;

    /**
     * 模型回答。
     */
    private String answer;
}
```

业务服务接口用于屏蔽底层 ChatClient 调用细节。

文件位置：`ai-chat-client/src/main/java/io/github/atengk/mcp/client/service/OrderChatService.java`

```java
package io.github.atengk.mcp.client.service;

import io.github.atengk.mcp.client.model.dto.OrderChatRequestDTO;
import io.github.atengk.mcp.client.model.vo.OrderChatResponseVO;

/**
 * 订单对话服务。
 *
 * @author Ateng
 * @since 2026-05-11
 */
public interface OrderChatService {

    /**
     * 咨询订单问题。
     *
     * @param request 请求参数
     * @return 订单对话响应
     */
    OrderChatResponseVO ask(OrderChatRequestDTO request);
}
```

业务服务实现类负责构造模型输入，并触发 ChatClient 调用。模型会根据工具描述自行决定是否调用 MCP Tool。

文件位置：`ai-chat-client/src/main/java/io/github/atengk/mcp/client/service/impl/OrderChatServiceImpl.java`

```java
package io.github.atengk.mcp.client.service.impl;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.mcp.client.model.dto.OrderChatRequestDTO;
import io.github.atengk.mcp.client.model.vo.OrderChatResponseVO;
import io.github.atengk.mcp.client.service.OrderChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/**
 * 订单对话服务实现。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderChatServiceImpl implements OrderChatService {

    private final ChatClient chatClient;

    /**
     * 咨询订单问题。
     *
     * @param request 请求参数
     * @return 订单对话响应
     */
    @Override
    public OrderChatResponseVO ask(OrderChatRequestDTO request) {
        if (request == null || StrUtil.hasBlank(request.getOrderNo(), request.getQuestion())) {
            throw new IllegalArgumentException("订单号和用户问题不能为空");
        }

        log.info("开始处理订单AI问答，订单号：{}", request.getOrderNo());

        String userMessage = """
                请处理以下订单问题。
                
                订单号：%s
                
                用户问题：%s
                
                要求：
                1. 优先调用订单查询工具获取订单真实状态。
                2. 如涉及退款规则，请结合订单规则进行判断。
                3. 回答需要简洁、明确，并说明判断依据。
                """.formatted(request.getOrderNo(), request.getQuestion());

        String answer = chatClient.prompt()
                .user(userMessage)
                .call()
                .content();

        return OrderChatResponseVO.builder()
                .orderNo(request.getOrderNo())
                .answer(answer)
                .build();
    }
}
```

Controller 提供 HTTP 调用入口。

文件位置：`ai-chat-client/src/main/java/io/github/atengk/mcp/client/controller/OrderChatController.java`

```java
package io.github.atengk.mcp.client.controller;

import io.github.atengk.mcp.client.common.ApiResult;
import io.github.atengk.mcp.client.model.dto.OrderChatRequestDTO;
import io.github.atengk.mcp.client.model.vo.OrderChatResponseVO;
import io.github.atengk.mcp.client.service.OrderChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 订单 AI 对话接口。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/order/chat")
public class OrderChatController {

    private final OrderChatService orderChatService;

    /**
     * 咨询订单问题。
     *
     * @param request 请求参数
     * @return 订单对话响应
     */
    @PostMapping("/ask")
    public ApiResult<OrderChatResponseVO> ask(@Valid @RequestBody OrderChatRequestDTO request) {
        log.info("收到订单AI咨询请求，订单号：{}", request.getOrderNo());
        return ApiResult.success(orderChatService.ask(request));
    }
}
```

### 运行步骤

运行前需要准备 JDK、Maven 和模型 API Key。示例默认使用 OpenAI Starter，如果实际项目使用 Ollama、DeepSeek、Azure OpenAI 或其他模型，需要替换客户端模块中的模型 Starter 和 `spring.ai.*` 模型配置。

第一步，设置模型 API Key。

```bash
# Linux / macOS
export OPENAI_API_KEY='你的模型API Key'

# Windows PowerShell
$env:OPENAI_API_KEY='你的模型API Key'
```

第二步，在根目录编译工程。

```bash
cd spring-ai-mcp-example

# 编译全部模块
mvn clean package -DskipTests
```

第三步，启动 MCP Server。

```bash
# 启动订单 MCP Server，端口 8081
java -jar mcp-order-server/target/mcp-order-server-1.0.0.jar
```

检查服务端健康状态。

```bash
curl http://localhost:8081/actuator/health
```

预期结果：

```json
{
  "status": "UP"
}
```

第四步，启动 AI Chat Client。

```bash
# 启动 AI Client，端口 8080
java -jar ai-chat-client/target/ai-chat-client-1.0.0.jar
```

检查客户端健康状态。

```bash
curl http://localhost:8080/actuator/health
```

第五步，调用订单 AI 对话接口。

```bash
curl -X POST 'http://localhost:8080/api/order/chat/ask' \
  -H 'Content-Type: application/json' \
  -d '{
    "orderNo": "ORD202605110001",
    "question": "这个订单现在是什么状态？是否可以退款？"
  }'
```

预期响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "orderNo": "ORD202605110001",
    "answer": "订单 ORD202605110001 当前状态为 PAID，金额为 199.90 元。根据工具返回结果，该订单已支付且未发货，支持直接退款。"
  }
}
```

如果模型没有调用工具，通常需要检查以下内容：

1. `ai-chat-client` 是否成功连接 `mcp-order-server`。
2. `spring.ai.mcp.client.toolcallback.enabled` 是否为 `true`。
3. `OrderInfoTool` 是否被 Spring 扫描为 Bean。
4. MCP Server 是否开启了 `annotation-scanner.enabled=true`。
5. Tool 的 `name` 和 `description` 是否足够清晰。
6. 当前模型是否支持 Tool Calling。

### 测试用例

测试建议分三层：先测试 MCP Server 启动，再测试 MCP Client 能否直接调用 MCP Server，最后测试 ChatClient 是否能完成大模型联调。

服务端启动测试用于确认 Spring 上下文可以正常启动。

文件位置：`mcp-order-server/src/test/java/io/github/atengk/mcp/server/OrderMcpServerApplicationTests.java`

```java
package io.github.atengk.mcp.server;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 订单 MCP Server 启动测试。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@SpringBootTest
class OrderMcpServerApplicationTests {

    /**
     * 验证 Spring 上下文启动。
     */
    @Test
    void contextLoads() {
    }
}
```

客户端 MCP 直连测试用于确认 `ai-chat-client` 可以连接 MCP Server，并直接调用 Tool、Resource、Prompt。执行该测试前，需要先启动 `mcp-order-server`。

文件位置：`ai-chat-client/src/test/java/io/github/atengk/mcp/client/McpClientInvokeTests.java`

```java
package io.github.atengk.mcp.client;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

/**
 * MCP Client 调用测试。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@SpringBootTest
class McpClientInvokeTests {

    @Autowired
    private List<McpSyncClient> mcpSyncClients;

    @Autowired
    private SyncMcpToolCallbackProvider toolCallbackProvider;

    /**
     * 验证 MCP 工具列表。
     */
    @Test
    void listTools() {
        McpSyncClient client = getFirstClient();
        McpSchema.ListToolsResult result = client.listTools();

        log.info("MCP工具列表：{}", JSONUtil.toJsonStr(result.tools()));
        Assertions.assertTrue(CollUtil.isNotEmpty(result.tools()), "MCP工具列表不能为空");
    }

    /**
     * 验证订单查询 Tool。
     */
    @Test
    void callOrderSummaryTool() {
        McpSyncClient client = getFirstClient();

        McpSchema.CallToolResult result = client.callTool(
                new McpSchema.CallToolRequest(
                        "order_query_summary",
                        Map.of("orderNo", "ORD202605110001")
                )
        );

        log.info("MCP订单查询结果：{}", JSONUtil.toJsonStr(result));
        Assertions.assertNotNull(result, "MCP工具调用结果不能为空");
    }

    /**
     * 验证订单规则 Resource。
     */
    @Test
    void readRefundRuleResource() {
        McpSyncClient client = getFirstClient();

        McpSchema.ReadResourceResult result = client.readResource(
                new McpSchema.ReadResourceRequest("order-rule://refund")
        );

        log.info("MCP资源读取结果：{}", JSONUtil.toJsonStr(result));
        Assertions.assertTrue(CollUtil.isNotEmpty(result.contents()), "MCP资源内容不能为空");
    }

    /**
     * 验证订单分析 Prompt。
     */
    @Test
    void getOrderIssuePrompt() {
        McpSyncClient client = getFirstClient();

        McpSchema.GetPromptResult result = client.getPrompt(
                new McpSchema.GetPromptRequest(
                        "order_issue_analysis",
                        Map.of(
                                "orderNo", "ORD202605110001",
                                "question", "这个订单是否可以退款？"
                        )
                )
        );

        log.info("MCP Prompt结果：{}", JSONUtil.toJsonStr(result));
        Assertions.assertTrue(CollUtil.isNotEmpty(result.messages()), "MCP Prompt消息不能为空");
    }

    /**
     * 验证 MCP ToolCallback 注册。
     */
    @Test
    void listToolCallbacks() {
        ToolCallback[] callbacks = toolCallbackProvider.getToolCallbacks();

        log.info("MCP ToolCallback数量：{}", callbacks.length);
        Assertions.assertTrue(callbacks.length > 0, "MCP ToolCallback不能为空");
    }

    /**
     * 获取第一个 MCP 同步客户端。
     *
     * @return MCP 同步客户端
     */
    private McpSyncClient getFirstClient() {
        Assertions.assertTrue(CollUtil.isNotEmpty(mcpSyncClients), "MCP同步客户端不能为空");
        return mcpSyncClients.getFirst();
    }
}
```

业务服务测试用于确认 `OrderChatService` 能通过 ChatClient 完成模型调用。该测试依赖模型 API Key 和正在运行的 MCP Server。

文件位置：`ai-chat-client/src/test/java/io/github/atengk/mcp/client/OrderChatServiceTests.java`

```java
package io.github.atengk.mcp.client;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.mcp.client.model.dto.OrderChatRequestDTO;
import io.github.atengk.mcp.client.model.vo.OrderChatResponseVO;
import io.github.atengk.mcp.client.service.OrderChatService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 订单 AI 对话服务测试。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@SpringBootTest
class OrderChatServiceTests {

    @Autowired
    private OrderChatService orderChatService;

    /**
     * 验证订单 AI 问答。
     */
    @Test
    void askOrderQuestion() {
        OrderChatRequestDTO request = new OrderChatRequestDTO();
        request.setOrderNo("ORD202605110001");
        request.setQuestion("这个订单现在是什么状态？是否可以退款？");

        OrderChatResponseVO response = orderChatService.ask(request);

        log.info("订单AI问答结果：{}", response);
        Assertions.assertNotNull(response, "订单AI问答响应不能为空");
        Assertions.assertTrue(StrUtil.isNotBlank(response.getAnswer()), "模型回答不能为空");
    }
}
```

测试执行顺序建议如下：

```bash
# 1. 编译工程
mvn clean package -DskipTests

# 2. 启动 MCP Server
java -jar mcp-order-server/target/mcp-order-server-1.0.0.jar

# 3. 在另一个终端执行 MCP Client 直连测试
mvn test -pl ai-chat-client -Dtest=McpClientInvokeTests

# 4. 确认 OPENAI_API_KEY 已配置后，执行大模型联调测试
mvn test -pl ai-chat-client -Dtest=OrderChatServiceTests
```

测试通过标准如下：

| 测试项                           | 预期结果                             |
| -------------------------------- | ------------------------------------ |
| `OrderMcpServerApplicationTests` | MCP Server 上下文正常启动            |
| `listTools()`                    | 能获取 `order_query_summary` 工具    |
| `callOrderSummaryTool()`         | 能成功调用订单查询 Tool              |
| `readRefundRuleResource()`       | 能读取 `order-rule://refund`         |
| `getOrderIssuePrompt()`          | 能获取 `order_issue_analysis` Prompt |
| `listToolCallbacks()`            | 能获取 MCP ToolCallback              |
| `askOrderQuestion()`             | 模型能基于 MCP 工具结果回答订单问题  |

示例工程完成后，就具备了 Spring AI 2.x MCP 的最小闭环：MCP Server 暴露能力，MCP Client 发现能力，ChatClient 注册工具，大模型在对话中按需调用工具，并将工具结果组织成最终业务回答。