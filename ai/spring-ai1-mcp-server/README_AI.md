# Spring AI 1.x MCP 开发

本文说明 Spring AI 1.x 中 MCP（Model Context Protocol）的基础概念和开发边界，重点覆盖 MCP 的协议定位、Client / Server 角色，以及 Tools、Resources、Prompts 三类核心能力。MCP 用于标准化 AI 应用与外部工具、资源和业务系统之间的交互方式，Spring AI 通过 MCP Boot Starters 和注解能力支持 MCP Server 与 MCP Client 的开发。([Home](https://docs.spring.io/spring-ai/reference/guides/getting-started-mcp.html))

## 文档概述

本部分用于说明文档的目标、适用版本和技术范围。正式开发 MCP 功能前，需要先明确本文解决什么问题、面向哪些版本，以及哪些内容属于 MCP 开发范畴，避免把 MCP、普通 REST 接口、Spring AI Tool Calling 和业务 Service 混在一起设计。

### 文档目标

本文目标是提供一份面向 Spring Boot 开发者的 Spring AI 1.x MCP 开发说明，帮助开发者理解 MCP 的核心概念，并能够在实际项目中完成 MCP Server 和 MCP Client 的基础开发。

本文主要关注以下目标：

| 目标                    | 说明                                                         |
| ----------------------- | ------------------------------------------------------------ |
| 理解 MCP 基础概念       | 明确 MCP 协议、Client、Server、Tools、Resources、Prompts 的职责 |
| 明确 Spring AI MCP 定位 | 理解 Spring AI 如何通过 Boot Starter 和注解简化 MCP 开发     |
| 支持 MCP Server 开发    | 将业务方法、数据资源和提示词模板暴露给 MCP Client            |
| 支持 MCP Client 开发    | 在 AI 应用中连接 MCP Server，并调用外部工具或读取上下文      |
| 支持大模型集成          | 将 MCP 能力接入 ChatClient，使模型具备外部系统访问能力       |
| 支持工程化落地          | 为后续配置、封装、验证和排查提供统一基础                     |

本文不是 MCP 协议规范的完整翻译，也不是 Spring AI 所有模块的完整说明。本文更关注 Java / Spring Boot 项目中如何理解和落地 MCP 开发。

### 适用版本

本文适用于 Spring AI 1.x 版本体系，示例和说明默认基于 Spring Boot 3.x 项目。Spring AI 1.x 文档中已提供 MCP Client Boot Starter、MCP Server Boot Starter、MCP Utilities 等相关模块；当前 Spring AI 1.x 稳定文档中也包含 1.0.x 和 1.1.x 版本入口。([Home](https://docs.spring.io/spring-ai/reference/1.0/api/mcp/mcp-client-boot-starter-docs.html))

建议版本范围如下：

| 组件         | 建议版本                       | 说明                                                 |
| ------------ | ------------------------------ | ---------------------------------------------------- |
| JDK          | 17+                            | Spring Boot 3.x 项目的基础运行要求                   |
| Spring Boot  | 3.x                            | 建议与所选 Spring AI 版本的官方兼容范围保持一致      |
| Spring AI    | 1.x                            | 本文围绕 Spring AI 1.x MCP 能力编写                  |
| 构建工具     | Maven 3.9+ / Gradle 8+         | 用于依赖管理、编译、测试和打包                       |
| MCP 通信方式 | STDIO、SSE、Streamable HTTP 等 | 具体支持能力取决于所选 MCP Starter 和 Spring AI 版本 |

项目中建议通过 `spring-ai-bom` 管理 Spring AI 相关依赖版本，避免 MCP Client、MCP Server、模型接入、Tool Calling 等模块之间出现版本不一致。

### 技术范围

本文覆盖 Spring AI 1.x MCP 开发中的基础概念和核心开发边界，包括 MCP Server 能力暴露、MCP Client 连接调用、与大模型调用链路的关系，以及后续业务封装和配置验证的基础说明。Spring AI MCP 基于 MCP Java SDK，并通过 Spring Boot Starter 简化 MCP 应用开发。([Home](https://docs.spring.io/spring-ai/reference/1.0/api/mcp/mcp-overview.html?utm_source=chatgpt.com))

本文覆盖内容如下：

| 范围            | 内容                                                        |
| --------------- | ----------------------------------------------------------- |
| MCP 基础概念    | 协议定位、Client / Server 角色、Tools / Resources / Prompts |
| MCP Server 开发 | Tool 注册、Resource 暴露、Prompt 定义、服务启动配置         |
| MCP Client 开发 | Server 连接配置、工具发现、工具调用、资源读取、Prompt 获取  |
| 大模型集成      | ChatClient 接入、Tool Calling、上下文传递与结果处理         |
| 工程封装        | MCP 服务封装、业务方法设计、请求响应结构、异常处理          |
| 配置验证        | application 配置、多 MCP Server 配置、超时配置、功能联调    |

本文不重点展开以下内容：

| 不展开内容             | 说明                                                   |
| ---------------------- | ------------------------------------------------------ |
| MCP 底层协议完整规范   | 实际开发优先使用 Spring AI MCP Starter 和 MCP Java SDK |
| 大模型厂商完整接入流程 | 只说明 MCP 与大模型调用链路相关的部分                  |
| 复杂 Agent 编排系统    | 本文重点是 MCP 能力开发，不展开完整 Agent 平台设计     |
| 企业权限系统完整方案   | 只说明 MCP 开发中需要关注的权限和安全边界              |

## MCP 基础概念

本部分介绍 MCP 的核心概念。MCP 的价值在于把大模型访问外部系统这件事标准化，让 AI 应用能够以统一方式发现工具、调用工具、读取资源和使用提示词模板，而不是为每个外部系统单独编写一套私有适配逻辑。

### MCP 协议定位

MCP 是一种面向 AI 应用的上下文与能力接入协议，用于标准化大模型应用与外部工具、资源、文件系统、数据库、业务 API 等系统之间的交互方式。Spring AI 文档将 MCP 描述为 AI 模型与外部工具和资源之间的标准化交互协议，并支持多种传输机制。([Home](https://docs.spring.io/spring-ai/reference/1.0/api/mcp/mcp-overview.html?utm_source=chatgpt.com))

从工程开发角度看，MCP 可以理解为大模型应用与外部系统之间的协议适配层。大模型应用不直接关心订单系统、库存系统、文件系统或知识库系统的私有接口，而是通过 MCP Client 连接 MCP Server，再由 MCP Server 按统一协议暴露可调用能力和可读取上下文。

典型调用链路如下：

```text
用户问题
  ↓
AI 应用 / ChatClient
  ↓
MCP Client
  ↓
MCP Server
  ↓
Spring Boot 业务服务 / 数据库 / 第三方系统
  ↓
Tool 执行结果 / Resource 内容 / Prompt 内容
  ↓
大模型生成最终响应
```

MCP 主要解决三类问题：

| 问题                     | MCP 处理方式                  |
| ------------------------ | ----------------------------- |
| 模型如何调用外部业务能力 | 通过 Tools 暴露可执行函数     |
| 模型如何读取外部上下文   | 通过 Resources 暴露可读取数据 |
| 模型如何复用标准提示词   | 通过 Prompts 暴露模板化提示词 |

在 Spring AI 1.x 中，MCP 不是用来替代 Spring Bean、REST Controller 或业务 Service 的。更合理的定位是：业务能力仍然由 Spring Service 实现，MCP Server 负责把其中适合给 AI 应用使用的能力包装成标准化 MCP 能力。

### Client 与 Server 角色

MCP 采用 Client / Server 架构。MCP Client 通常运行在 AI 应用侧，负责连接 MCP Server、完成协议初始化、能力协商、工具发现、资源访问和 Prompt 获取；MCP Server 通常运行在能力提供方，负责暴露 Tools、Resources 和 Prompts，并处理来自 Client 的请求。Spring AI 的 MCP Java SDK 架构中也明确包含 Client / Server 层、Session 层和 Transport 层。([Home](https://docs.spring.io/spring-ai/reference/1.0/api/mcp/mcp-overview.html?utm_source=chatgpt.com))

MCP Client 的职责如下：

| 职责       | 说明                                           |
| ---------- | ---------------------------------------------- |
| 建立连接   | 根据配置连接一个或多个 MCP Server              |
| 协议初始化 | 完成协议版本协商、能力协商和会话初始化         |
| 发现能力   | 获取 Server 暴露的 Tools、Resources 和 Prompts |
| 发起调用   | 调用 Tool、读取 Resource 或获取 Prompt         |
| 处理结果   | 将 MCP Server 返回结果转为模型上下文或业务响应 |
| 管理异常   | 处理连接失败、调用超时、协议错误和工具执行失败 |

MCP Server 的职责如下：

| 职责          | 说明                                                |
| ------------- | --------------------------------------------------- |
| 暴露 Tool     | 将可执行的业务方法暴露为 MCP Tool                   |
| 暴露 Resource | 将文档、文件、配置、数据库内容等暴露为 MCP Resource |
| 暴露 Prompt   | 将标准提示词模板暴露为 MCP Prompt                   |
| 处理请求      | 接收 MCP Client 请求并调用本地业务逻辑              |
| 返回结果      | 按 MCP 协议返回工具执行结果、资源内容或 Prompt 内容 |
| 管理连接      | 处理客户端连接、能力协商、日志和通知等协议行为      |

在实际项目中，一个应用可以只作为 MCP Server，也可以只作为 MCP Client，也可以同时具备两种角色。例如：

| 应用类型                  | 示例                                                         |
| ------------------------- | ------------------------------------------------------------ |
| 只作为 MCP Server         | 订单 MCP Server，暴露订单查询、订单状态变更等能力            |
| 只作为 MCP Client         | 客服 AI 助手，连接订单、库存、物流等多个 MCP Server          |
| 同时作为 Client 和 Server | 企业 AI 中台，一边消费外部 MCP 能力，一边向其他系统暴露统一能力 |

### Tools、Resources 与 Prompts

Tools、Resources 和 Prompts 是 MCP Server 暴露给 MCP Client 的三类核心能力。它们分别对应可执行操作、可读取上下文和可复用提示词模板。Spring AI MCP 支持通过 Boot Starters 和注解方式创建 MCP tools、resources 和 prompts，并可与 Spring Boot 应用集成。([Home](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-overview.html?utm_source=chatgpt.com))

三者区别如下：

| 类型      | 核心定位         | 典型场景                                     | 是否通常有业务副作用 |
| --------- | ---------------- | -------------------------------------------- | -------------------- |
| Tools     | 可执行函数       | 查询订单、创建工单、发送通知、调用内部 API   | 可能有               |
| Resources | 可读取上下文     | 读取文档、配置、文件、数据库记录、知识库内容 | 通常没有             |
| Prompts   | 可复用提示词模板 | 代码审查模板、SQL 优化模板、故障排查模板     | 没有                 |

Tools 适合封装明确的业务动作。一个 Tool 应该有清晰的名称、描述、输入参数和返回结果。例如“根据订单号查询订单详情”“根据用户 ID 查询用户权益”“创建售后工单”等都适合设计为 Tool。

Resources 适合暴露只读上下文。它更像是模型可以访问的资料入口，例如接口文档、系统配置、日志片段、数据库元信息、业务规则说明等。Resource 应尽量避免执行有副作用的业务动作。

Prompts 适合沉淀可复用的提示词模板。比如团队内部统一的代码审查模板、异常分析模板、SQL 优化模板、需求拆解模板等，都可以设计为 Prompt，让不同 AI 应用复用相同提示词结构。

选择原则如下：

| 需求                         | 推荐类型        | 示例                                    |
| ---------------------------- | --------------- | --------------------------------------- |
| 需要执行业务方法并返回结果   | Tool            | 根据订单号查询订单详情                  |
| 需要让模型读取上下文资料     | Resource        | 读取项目接口文档                        |
| 需要复用标准提示词结构       | Prompt          | 生成代码审查提示词                      |
| 需要先读取资料再执行操作     | Resource + Tool | 先读取审批规则，再调用审批工具          |
| 需要固定提示词并触发工具调用 | Prompt + Tool   | 使用故障排查 Prompt 后调用日志查询 Tool |

在 Spring AI 1.x MCP 开发中，建议按以下方式建模：

| 业务内容               | 建模方式                                            |
| ---------------------- | --------------------------------------------------- |
| 查询类接口             | 优先设计为 Tool                                     |
| 创建、修改、删除类接口 | 可以设计为 Tool，但必须补充权限、参数校验和审计日志 |
| 文档、配置、知识库内容 | 优先设计为 Resource                                 |
| 团队标准提示词         | 优先设计为 Prompt                                   |
| 复杂业务流程           | 拆分为 Prompt + 多个 Tool，避免单个 Tool 过于庞大   |

总体原则是：Tool 负责“做事”，Resource 负责“提供上下文”，Prompt 负责“提供表达和推理模板”。这样可以让 MCP Server 的能力边界更清晰，也便于后续进行权限控制、日志审计、异常处理和多 Server 编排。

## 开发环境准备

本部分用于说明 Spring AI 1.x MCP 开发前需要准备的基础环境，包括 JDK、Spring Boot、Spring AI BOM、模型依赖、MCP Client / Server 依赖，以及推荐的工程目录结构。Spring AI 官方文档当前标识的稳定分支包含 1.1.6 和 1.0.7；如果项目要求严格使用 Spring AI 1.0.x，可以锁定 1.0.7，如果允许使用 1.x 最新稳定能力，可以使用 1.1.6。([Home](https://docs.spring.io/spring-ai/reference/guides/getting-started-mcp.html))

### JDK 与 Spring Boot 版本要求

Spring AI 1.x 项目建议基于 JDK 17+ 和 Spring Boot 3.x 构建。Spring AI 官方文档说明其支持 Spring Boot 3.4.x 和 3.5.x，因此新项目建议优先选择 Spring Boot 3.4.x 或 3.5.x，避免使用过旧的 Spring Boot 3.0 / 3.1 版本。([Home](https://docs.spring.io/spring-ai/reference/getting-started.html?utm_source=chatgpt.com))

| 组件        | 建议版本                | 说明                                                        |
| ----------- | ----------------------- | ----------------------------------------------------------- |
| JDK         | 17+                     | Spring Boot 3.x 基础要求，生产环境建议使用 JDK 17 或 JDK 21 |
| Spring Boot | 3.4.x / 3.5.x           | 与 Spring AI 1.x 官方支持范围保持一致                       |
| Spring AI   | 1.0.7 / 1.1.6           | 根据项目稳定性要求选择 1.0.x 或 1.1.x                       |
| Maven       | 3.9+                    | 用于依赖管理、编译、测试和打包                              |
| IDE         | IntelliJ IDEA / VS Code | 建议开启 Maven 自动导入和 Spring Boot 配置提示              |

本地环境可以通过以下命令确认版本。

```bash
java -version
mvn -version
```

`java -version` 用于确认当前 JDK 版本，`mvn -version` 用于确认 Maven 版本以及 Maven 运行时使用的 JDK。如果 Maven 显示的 Java 版本和系统 `java -version` 不一致，需要检查 `JAVA_HOME` 和 IDE 的 Maven Runner 配置。

### Spring AI 依赖配置

Spring AI 建议通过 `spring-ai-bom` 统一管理依赖版本。BOM 会声明当前 Spring AI 发行版本推荐使用的依赖版本，业务项目只需要在 `dependencyManagement` 中引入 BOM，后续添加具体 Spring AI 模块时通常不需要再单独指定版本。Spring AI 1.0.0 及之后版本已发布到 Maven Central，普通稳定版项目不需要额外配置 Spring Milestone 或 Snapshot 仓库。([Home](https://docs.spring.io/spring-ai/reference/getting-started.html?utm_source=chatgpt.com))

下面的 Maven 配置用于统一 Spring Boot 和 Spring AI 依赖版本，适合作为 MCP 示例工程的基础 `pom.xml`。

```xml
<project>
    <modelVersion>4.0.0</modelVersion>

    <groupId>io.github.atengk</groupId>
    <artifactId>spring-ai-mcp-demo</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>

    <properties>
        <!-- Java 编译版本，建议与运行环境保持一致 -->
        <java.version>17</java.version>

        <!-- Spring AI 版本：严格 1.0.x 可使用 1.0.7，使用 1.x 较新稳定能力可使用 1.1.6 -->
        <spring-ai.version>1.1.6</spring-ai.version>

        <!-- Hutool 工具类版本，可按企业依赖基线调整 -->
        <hutool.version>5.8.36</hutool.version>
    </properties>

    <parent>
        <!-- Spring Boot 父工程负责管理 Spring Boot 生态依赖版本 -->
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.5.0</version>
        <relativePath/>
    </parent>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <!-- Spring AI BOM 负责统一 Spring AI 相关模块版本 -->
                <groupId>org.springframework.ai</groupId>
                <artifactId>spring-ai-bom</artifactId>
                <version>${spring-ai.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <dependencies>
        <dependency>
            <!-- Spring Boot Web 基础能力，用于 HTTP 接口、健康检查和 WebMVC MCP Server -->
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <dependency>
            <!-- Spring Boot 参数校验能力，用于请求参数、Tool 入参和业务 DTO 校验 -->
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <dependency>
            <!-- Spring AI OpenAI 模型接入示例，用于 ChatClient 与大模型联调 -->
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-starter-model-openai</artifactId>
        </dependency>

        <dependency>
            <!-- Hutool 常用工具类，适合处理字符串、集合、JSON、日期等基础逻辑 -->
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
            <version>${hutool.version}</version>
        </dependency>

        <dependency>
            <!-- Lombok 简化 DTO、VO、配置属性类等样板代码 -->
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <dependency>
            <!-- Spring Boot 测试依赖，用于 MCP Server、MCP Client 和业务 Service 单元测试 -->
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

如果项目只是开发 MCP Server，并不需要直接调用大模型，可以暂时不引入 `spring-ai-starter-model-openai`。如果项目需要通过 `ChatClient` 调用模型并自动使用 MCP Tools，则需要引入具体模型 Starter，例如 OpenAI、Azure OpenAI、Ollama、Anthropic 等。Spring AI OpenAI 文档中，`spring-ai-starter-model-openai` 是启用 OpenAI Chat Client 自动配置的 Starter。([Home](https://docs.spring.io/spring-ai/reference/api/chat/openai-chat.html?utm_source=chatgpt.com))

### MCP 相关依赖配置

Spring AI MCP 分为 Client Starter 和 Server Starter。MCP Client Starter 负责连接一个或多个 MCP Server，并支持 STDIO、SSE、Streamable HTTP、Stateless Streamable HTTP 等传输方式；MCP Server Starter 负责把 Spring Boot 应用中的 Tools、Resources、Prompts 暴露为标准 MCP 能力。([Home](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-client-boot-starter-docs.html))

MCP Client 常用依赖如下。

| 依赖                                   | 适用场景                                                     |
| -------------------------------------- | ------------------------------------------------------------ |
| `spring-ai-starter-mcp-client`         | 标准 MCP Client，支持 STDIO、SSE、Streamable HTTP 等连接方式 |
| `spring-ai-starter-mcp-client-webflux` | 基于 WebFlux 的 MCP Client，适合生产环境中的异步 HTTP / SSE / Streamable HTTP 调用 |

MCP Server 常用依赖如下。

| 依赖                                   | 适用场景                                             | 关键配置                                   |
| -------------------------------------- | ---------------------------------------------------- | ------------------------------------------ |
| `spring-ai-starter-mcp-server`         | STDIO MCP Server，适合命令行、本地进程、桌面工具集成 | `spring.ai.mcp.server.stdio=true`          |
| `spring-ai-starter-mcp-server-webmvc`  | WebMVC MCP Server，适合普通 Spring MVC 服务          | `spring.ai.mcp.server.protocol=STREAMABLE` |
| `spring-ai-starter-mcp-server-webflux` | WebFlux MCP Server，适合响应式服务                   | `spring.ai.mcp.server.protocol=STREAMABLE` |

Spring AI MCP Server 文档说明，WebMVC Server 可通过 `spring-ai-starter-mcp-server-webmvc` 支持 SSE、Streamable HTTP 和 Stateless 模式；STDIO Server 使用 `spring-ai-starter-mcp-server`，并通过 `spring.ai.mcp.server.stdio=true` 启用。([Home](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-server-boot-starter-docs.html))

下面的依赖适合开发一个基于 WebMVC 的 MCP Server。

```xml
<dependencies>
    <dependency>
        <!-- WebMVC MCP Server：通过 HTTP 暴露 Tools、Resources、Prompts -->
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-starter-mcp-server-webmvc</artifactId>
    </dependency>
</dependencies>
```

下面的依赖适合开发一个 MCP Client，并在 ChatClient 中使用远程 MCP Server 暴露的 Tools。

```xml
<dependencies>
    <dependency>
        <!-- MCP Client：连接一个或多个 MCP Server，并集成 Spring AI ToolCallback 机制 -->
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-starter-mcp-client</artifactId>
    </dependency>

    <dependency>
        <!-- 模型接入示例：用于 ChatClient 调用大模型 -->
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-starter-model-openai</artifactId>
    </dependency>
</dependencies>
```

如果 Server 和 Client 放在同一个示例工程中，可以同时引入 Server Starter 和 Client Starter；如果是生产项目，建议拆分为独立服务，避免同一个应用既暴露 MCP 能力又消费自身 MCP 能力，导致职责不清。

MCP Server 最小配置示例如下。

```yaml
server:
  # MCP Server HTTP 服务端口
  port: 8080

spring:
  application:
    # 当前 MCP Server 应用名称
    name: spring-ai-mcp-server

  ai:
    mcp:
      server:
        # 启用 MCP Server
        enabled: true

        # 使用 Streamable HTTP 协议，适合独立服务部署和多客户端连接
        protocol: STREAMABLE

        # MCP Server 名称，便于 Client 识别
        name: spring-ai-mcp-server

        # MCP Server 版本
        version: 1.0.0
```

MCP Client 最小配置示例如下。

```yaml
server:
  # MCP Client 示例应用端口
  port: 8081

spring:
  application:
    # 当前 MCP Client 应用名称
    name: spring-ai-mcp-client

  ai:
    openai:
      # 大模型 API Key 建议从环境变量读取，避免写死在配置文件中
      api-key: ${OPENAI_API_KEY}
      chat:
        options:
          # 示例模型名称，按实际模型供应商和账号权限调整
          model: gpt-4o-mini

    mcp:
      client:
        # 启用 MCP Client
        enabled: true

        # MCP Client 名称
        name: spring-ai-mcp-client

        # MCP 请求超时时间
        request-timeout: 30s

        # SYNC 适合普通阻塞式 Spring MVC 应用；ASYNC 适合响应式应用
        type: SYNC

        streamable-http:
          connections:
            business-server:
              # 远程 MCP Server 地址
              url: http://localhost:8080

              # Streamable HTTP 默认端点，一般为 /mcp
              endpoint: /mcp
```

MCP Client 官方配置中，通用配置前缀为 `spring.ai.mcp.client`，默认启用 Client，默认请求超时为 `20s`，Client 类型默认为 `SYNC`，并且默认启用与 Spring AI Tool Callback 框架的集成。([Home](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-client-boot-starter-docs.html))

### 项目结构说明

MCP 开发建议按 Server 和 Client 职责拆分目录。MCP Server 负责暴露能力，重点放在 Tool、Resource、Prompt 和业务 Service；MCP Client 负责连接 Server、调用工具、读取资源，并与 ChatClient 集成。示例工程可以使用单模块结构快速验证，也可以使用多模块结构模拟生产部署。

单模块示例结构如下，适合学习和本地验证。

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
│   │   │                   │   └── McpConfig.java
│   │   │                   ├── server
│   │   │                   │   ├── tool
│   │   │                   │   │   └── OrderTool.java
│   │   │                   │   ├── resource
│   │   │                   │   │   └── OrderResource.java
│   │   │                   │   └── prompt
│   │   │                   │       └── OrderPrompt.java
│   │   │                   ├── client
│   │   │                   │   └── McpChatService.java
│   │   │                   ├── service
│   │   │                   │   ├── OrderService.java
│   │   │                   │   └── impl
│   │   │                   │       └── OrderServiceImpl.java
│   │   │                   ├── model
│   │   │                   │   ├── dto
│   │   │                   │   │   └── OrderQueryDTO.java
│   │   │                   │   └── vo
│   │   │                   │       └── OrderInfoVO.java
│   │   │                   └── controller
│   │   │                       └── ChatController.java
│   │   └── resources
│   │       ├── application.yml
│   │       └── prompts
│   │           └── order-analysis-template.md
│   └── test
│       └── java
│           └── io
│               └── github
│                   └── atengk
│                       └── mcp
│                           └── SpringAiMcpApplicationTests.java
```

多模块结构如下，适合生产工程或团队协作。Server 和 Client 可以独立部署，依赖公共 DTO、VO 和异常模型。

```text
spring-ai-mcp-parent
├── pom.xml
├── spring-ai-mcp-common
│   ├── pom.xml
│   └── src/main/java/io/github/atengk/mcp/common
│       ├── dto
│       ├── vo
│       ├── enums
│       └── exception
├── spring-ai-mcp-server
│   ├── pom.xml
│   └── src/main/java/io/github/atengk/mcp/server
│       ├── SpringAiMcpServerApplication.java
│       ├── tool
│       ├── resource
│       ├── prompt
│       ├── service
│       └── config
└── spring-ai-mcp-client
    ├── pom.xml
    └── src/main/java/io/github/atengk/mcp/client
        ├── SpringAiMcpClientApplication.java
        ├── controller
        ├── service
        ├── config
        └── chat
```

推荐包结构说明如下。

| 目录              | 说明                                                         |
| ----------------- | ------------------------------------------------------------ |
| `tool`            | 存放 MCP Tool 定义，封装可被模型调用的业务动作               |
| `resource`        | 存放 MCP Resource 定义，封装可读取的文档、配置、规则和上下文 |
| `prompt`          | 存放 MCP Prompt 定义，封装可复用提示词模板                   |
| `service`         | 存放业务服务，不直接依赖 MCP 协议细节                        |
| `client` / `chat` | 存放 MCP Client 调用和 ChatClient 集成逻辑                   |
| `model/dto`       | 存放请求参数对象                                             |
| `model/vo`        | 存放响应结果对象                                             |
| `config`          | 存放 MCP、ChatClient、模型参数、超时等配置                   |
| `controller`      | 提供本地测试接口或业务入口                                   |

工程设计上建议保持一条清晰边界：`service` 只处理业务逻辑，`tool/resource/prompt` 只负责 MCP 能力暴露，`client/chat` 只负责消费 MCP 能力并接入大模型。这样后续调整协议、替换模型、拆分服务或增加权限控制时，不需要大范围改动业务代码。

## MCP Server 开发

本部分说明 MCP Server 在 Spring Boot 应用中的开发方式。MCP Server 的职责是把当前应用中的业务能力、只读资源和提示词模板暴露给 MCP Client。Spring AI MCP Server Boot Starter 支持自动配置工具、资源和提示词，也支持 STDIO、SSE、Streamable HTTP、Stateless 等传输模式；通过注解方式可以直接将 Spring Bean 方法注册为 MCP 能力。([Home](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-server-boot-starter-docs.html))

### 服务端功能定位

MCP Server 应定位为“AI 应用可调用能力的提供方”，而不是普通 Controller 的替代品。业务系统内部仍然通过 Service、Repository、Mapper 等分层实现业务逻辑，MCP Server 只负责把适合被 AI 应用使用的能力以标准化协议暴露出去。

典型职责如下：

| 职责           | 说明                                                         |
| -------------- | ------------------------------------------------------------ |
| 暴露业务工具   | 将订单查询、库存查询、日志检索、通知发送等业务能力暴露为 Tool |
| 暴露上下文资源 | 将文档、配置、规则、文件、数据库元信息等只读内容暴露为 Resource |
| 暴露提示词模板 | 将代码审查、故障排查、SQL 优化等模板暴露为 Prompt            |
| 处理协议交互   | 由 Spring AI MCP Starter 处理协议初始化、能力注册和请求转发  |
| 控制安全边界   | 对敏感工具增加参数校验、权限控制、审计日志和只读限制         |

MCP Server 的核心设计原则是“能力聚合、边界清晰、结果可解释”。Tool 不应该直接暴露高风险的底层操作，例如任意 SQL 执行、任意文件删除、任意系统命令执行等；如果确实需要提供变更类能力，应当在业务 Service 中完成参数校验、权限判断、幂等控制和日志审计。

推荐的 Server 端调用链路如下：

```text
MCP Client 请求
  ↓
Spring AI MCP Server Starter
  ↓
@McpTool / @McpResource / @McpPrompt 注解方法
  ↓
业务 Service
  ↓
数据库 / 缓存 / 文件 / 第三方接口
  ↓
标准 MCP 响应
```

Spring AI MCP Server 注解支持 `@McpTool`、`@McpResource`、`@McpPrompt` 和 `@McpComplete`，并且 Spring Boot 自动配置会扫描带有 MCP 注解的 Bean，创建对应规范并注册到 MCP Server。([Home](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-server-boot-starter-docs.html))

### Tool 定义与注册

Tool 用于暴露可执行的业务动作，适合查询订单、查询用户、创建工单、发送通知、执行诊断等有明确输入输出的操作。Spring AI MCP Server 中可以使用 `@McpTool` 标记方法，并使用 `@McpToolParam` 描述参数，框架会自动生成参数 JSON Schema。([Home](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-annotations-server.html))

文件位置：`src/main/java/io/github/atengk/mcp/server/tool/OrderTool.java`

下面的代码将订单查询能力注册为 MCP Tool，MCP Client 可以通过工具名称 `query_order` 调用该能力。

```java
package io.github.atengk.mcp.server.tool;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.mcp.server.service.OrderService;
import io.github.atengk.mcp.server.vo.OrderInfoVO;
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

    private final OrderService orderService;

    @McpTool(
            name = "query_order",
            description = "根据订单号查询订单基础信息，适用于客服、售后和订单状态分析场景"
    )
    public OrderInfoVO queryOrder(
            @McpToolParam(description = "订单号，例如 ORDER_10001", required = true)
            String orderNo) {
        if (StrUtil.isBlank(orderNo)) {
            log.warn("订单查询失败：订单号为空");
            throw new IllegalArgumentException("订单号不能为空");
        }

        log.info("开始执行 MCP 订单查询，订单号：{}", orderNo);
        OrderInfoVO orderInfo = orderService.queryOrder(orderNo);
        log.info("MCP 订单查询完成，订单号：{}，状态：{}", orderNo, orderInfo.getStatus());
        return orderInfo;
    }
}
```

Tool 设计时需要重点关注名称、描述、参数和返回值。名称应稳定且语义清晰，描述应告诉模型何时使用该工具，参数描述应说明格式、示例和约束，返回值应尽量使用结构化对象，避免返回含义不清的长字符串。

| 设计项        | 建议                                                         |
| ------------- | ------------------------------------------------------------ |
| `name`        | 使用英文小写和下划线，例如 `query_order`、`search_product_stock` |
| `description` | 说明工具用途、适用场景和限制条件                             |
| 参数          | 使用 `@McpToolParam` 补充描述和是否必填                      |
| 返回值        | 优先返回 DTO / VO，便于模型理解结构                          |
| 日志          | 记录工具调用入口、关键参数、结果状态和异常原因               |
| 安全          | 修改类操作必须做权限、幂等、参数白名单和审计                 |

如果 Tool 是只读操作，可以在描述中明确“只查询，不修改数据”。如果 Tool 会产生副作用，例如创建工单、发送消息、修改状态，应在工具描述、业务 Service 和审计日志中同时体现该风险。

### Resource 定义与暴露

Resource 用于暴露可读取上下文，适合文档、配置、规则、文件内容、数据库元信息等只读数据。Spring AI MCP Server 可以使用 `@McpResource` 通过 URI 模板暴露资源，例如 `order-rule://{scene}`、`config://{key}`、`doc://{name}`。官方文档说明 `@McpResource` 用于通过 URI templates 提供资源访问。([Home](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-annotations-server.html))

文件位置：`src/main/java/io/github/atengk/mcp/server/resource/OrderResource.java`

下面的代码将订单场景规则暴露为 MCP Resource，适合让模型在分析订单前先读取业务规则。

```java
package io.github.atengk.mcp.server.resource;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.mcp.annotation.McpResource;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 订单 MCP 资源。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Component
public class OrderResource {

    private static final Map<String, String> ORDER_RULE_MAP = MapUtil.<String, String>builder()
            .put("refund", "退款规则：已支付订单可申请退款，已发货订单需要先完成退货审核。")
            .put("delivery", "发货规则：普通商品 48 小时内发货，预售商品以商品页承诺时间为准。")
            .put("after_sale", "售后规则：签收后 7 天内支持无理由退货，特殊商品除外。")
            .build();

    @McpResource(
            uri = "order-rule://{scene}",
            name = "订单业务规则",
            description = "根据业务场景读取订单相关规则，例如 refund、delivery、after_sale"
    )
    public String getOrderRule(String scene) {
        String ruleScene = StrUtil.blankToDefault(scene, "refund");
        String rule = ORDER_RULE_MAP.get(ruleScene);

        if (StrUtil.isBlank(rule)) {
            log.warn("读取 MCP 订单规则失败：未找到场景，scene={}", ruleScene);
            return "未找到对应订单规则，请检查 scene 参数。";
        }

        log.info("读取 MCP 订单规则成功，scene={}", ruleScene);
        return rule;
    }
}
```

Resource 通常不建议产生业务副作用。它更适合作为模型的“资料来源”，为后续 Tool 调用提供依据。例如模型先读取 `order-rule://refund`，理解退款规则后，再决定是否调用 `query_order` 或 `create_after_sale_ticket`。

Resource 设计建议如下：

| 设计项 | 建议                                                    |
| ------ | ------------------------------------------------------- |
| URI    | 使用清晰协议前缀，例如 `doc://`、`config://`、`rule://` |
| 内容   | 优先返回短文本、JSON 或结构化内容                       |
| 权限   | 敏感资源需要在读取前做权限判断                          |
| 副作用 | 默认只读，不做修改、发送、删除等动作                    |
| 粒度   | 不要一次返回过大的文档，可按主题或 ID 拆分              |

### Prompt 定义与使用

Prompt 用于暴露可复用提示词模板，适合沉淀团队统一的分析模板、审查模板、故障排查模板和业务问答模板。Spring AI MCP Server 使用 `@McpPrompt` 定义 Prompt，并通过 `@McpArg` 声明 Prompt 参数；官方示例中 Prompt 方法可返回 `GetPromptResult`，其中包含描述和消息列表。([Home](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-annotations-server.html))

文件位置：`src/main/java/io/github/atengk/mcp/server/prompt/OrderPrompt.java`

下面的代码定义了一个订单分析 Prompt，客户端可以传入订单号和问题类型，生成统一的分析提示词。

```java
package io.github.atengk.mcp.server.prompt;

import cn.hutool.core.util.StrUtil;
import io.modelcontextprotocol.spec.McpSchema.GetPromptResult;
import io.modelcontextprotocol.spec.McpSchema.PromptMessage;
import io.modelcontextprotocol.spec.McpSchema.Role;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import org.springframework.ai.mcp.annotation.McpArg;
import org.springframework.ai.mcp.annotation.McpPrompt;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 订单 MCP 提示词。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Component
public class OrderPrompt {

    @McpPrompt(
            name = "order_analysis",
            description = "生成订单问题分析提示词，适用于客服、售后和履约异常分析场景"
    )
    public GetPromptResult orderAnalysis(
            @McpArg(name = "orderNo", description = "订单号", required = true)
            String orderNo,
            @McpArg(name = "questionType", description = "问题类型，例如 refund、delivery、after_sale", required = false)
            String questionType) {
        String type = StrUtil.blankToDefault(questionType, "general");
        String message = StrUtil.format("""
                请基于订单号 {} 分析用户问题。

                问题类型：{}

                分析要求：
                1. 先判断是否需要读取订单业务规则 Resource。
                2. 如需订单详情，请调用 query_order 工具。
                3. 输出时需要说明判断依据、当前状态、建议动作。
                4. 如果信息不足，需要明确列出缺失信息，不要编造订单状态。
                """, orderNo, type);

        return new GetPromptResult(
                "订单问题分析模板",
                List.of(new PromptMessage(Role.USER, new TextContent(message)))
        );
    }
}
```

Prompt 不直接执行业务动作，它负责提供稳定的表达结构。复杂业务场景可以组合使用 Prompt、Resource 和 Tool：Prompt 规定分析步骤，Resource 提供业务规则，Tool 查询或操作业务数据。

Prompt 设计建议如下：

| 设计项 | 建议                                                 |
| ------ | ---------------------------------------------------- |
| 名称   | 使用稳定英文名，例如 `order_analysis`、`code_review` |
| 参数   | 使用 `@McpArg` 描述参数含义和必填性                  |
| 内容   | 明确分析步骤、工具使用条件和输出格式                 |
| 边界   | 明确禁止编造结果，要求信息不足时说明缺失项           |
| 复用   | 把团队高频提示词沉淀为 Prompt，避免散落在业务代码中  |

### 服务端启动与连接配置

MCP Server 启动方式取决于传输协议。Spring AI MCP Server 支持 STDIO、SSE、Streamable HTTP 和 Stateless，其中 Streamable HTTP 使用 HTTP POST / GET，并可结合 SSE 处理多条服务端消息；官方文档也说明 Streamable HTTP 替代 SSE 作为推荐的 HTTP 传输方式之一。([Home](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-server-boot-starter-docs.html))

基于 WebMVC + Streamable HTTP 的配置如下。

文件位置：`src/main/resources/application.yml`

```yaml
server:
  # MCP Server 监听端口
  port: 8080

spring:
  application:
    # 服务名称，便于日志和注册中心识别
    name: spring-ai-mcp-server

  ai:
    mcp:
      server:
        # 启用 MCP Server
        enabled: true

        # MCP Server 名称，Client 可用于识别服务来源
        name: order-mcp-server

        # MCP Server 版本
        version: 1.0.0

        # 服务端类型：SYNC 适合普通 Spring MVC 阻塞式业务；ASYNC 适合 WebFlux/Reactor
        type: SYNC

        # 使用 Streamable HTTP 协议
        protocol: STREAMABLE

        annotation-scanner:
          # 启用 MCP 注解扫描，默认开启；显式配置便于文档化
          enabled: true

        capabilities:
          # 是否暴露 Tool 能力
          tool: true

          # 是否暴露 Resource 能力
          resource: true

          # 是否暴露 Prompt 能力
          prompt: true

          # 是否暴露补全能力
          completion: true
```

启动命令如下。

```bash
mvn spring-boot:run
```

启动后，MCP Client 通常通过 `http://localhost:8080/mcp` 连接该服务。`/mcp` 是 Streamable HTTP Client 配置中的默认 endpoint；如果 Server 自定义了路径，需要 Client 同步调整。([Home](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-client-boot-starter-docs.html))

## MCP Client 开发

本部分说明 MCP Client 的开发方式。MCP Client 运行在 AI 应用侧，负责连接一个或多个 MCP Server，并将远程 Server 暴露的 Tools、Resources 和 Prompts 接入本地业务流程或 ChatClient 调用链路。Spring AI MCP Client Boot Starter 支持多连接管理、自动初始化、STDIO / SSE / Streamable HTTP 等传输方式，并可与 Spring AI 工具执行框架集成。([Home](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-client-boot-starter-docs.html))

### 客户端功能定位

MCP Client 应定位为“AI 应用访问外部能力的协议客户端”。它不直接实现订单查询、日志检索、文档读取等业务能力，而是连接对应的 MCP Server，完成能力发现、调用和结果处理。

典型职责如下：

| 职责        | 说明                                                         |
| ----------- | ------------------------------------------------------------ |
| 连接 Server | 根据配置连接一个或多个 MCP Server                            |
| 发现工具    | 获取远程 Server 暴露的 Tool 列表                             |
| 调用工具    | 根据模型决策或业务逻辑调用远程 Tool                          |
| 读取资源    | 按 URI 读取远程 Resource 内容                                |
| 使用 Prompt | 获取远程 Prompt 模板并传入参数                               |
| 接入模型    | 将 MCP Tools 转为 Spring AI ToolCallback 后提供给 ChatClient |
| 处理异常    | 处理连接失败、调用超时、工具不存在、参数错误等问题           |

MCP Client 可以直接使用 `McpSyncClient` / `McpAsyncClient` 调用，也可以让 Spring AI 自动生成 `ToolCallbackProvider`，再将 MCP Tools 交给 ChatClient。官方文档说明，自动配置后可以注入 `List<McpSyncClient>` 或 `List<McpAsyncClient>`，并且在 tool callback 开启时会提供 `ToolCallbackProvider` 用于接入 Spring AI 工具执行框架。([Home](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-client-boot-starter-docs.html))

### MCP Server 连接配置

MCP Client 可以同时连接多个 MCP Server。Spring AI MCP Client 的通用配置前缀是 `spring.ai.mcp.client`，常用配置包括 `enabled`、`name`、`version`、`initialized`、`request-timeout`、`type` 和 `toolcallback.enabled`；其中 `request-timeout` 默认是 `20s`，`type` 默认是 `SYNC`。([Home](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-client-boot-starter-docs.html))

文件位置：`src/main/resources/application.yml`

下面的配置连接一个订单 MCP Server，并启用 ToolCallback 自动集成。

```yaml
server:
  # MCP Client 示例应用端口
  port: 8081

spring:
  application:
    # 当前 AI 应用名称
    name: spring-ai-mcp-client

  ai:
    openai:
      # 模型 API Key 建议使用环境变量，避免写死
      api-key: ${OPENAI_API_KEY}
      chat:
        options:
          # 示例模型，按实际账号和模型供应商调整
          model: gpt-4o-mini

    mcp:
      client:
        # 启用 MCP Client
        enabled: true

        # Client 名称
        name: order-ai-client

        # Client 版本
        version: 1.0.0

        # 启动时初始化 MCP Client
        initialized: true

        # MCP 请求超时时间
        request-timeout: 30s

        # SYNC 适合 Spring MVC；ASYNC 适合 WebFlux/Reactor
        type: SYNC

        toolcallback:
          # 默认开启；开启后可注入 ToolCallbackProvider 给 ChatClient 使用
          enabled: true

        streamable-http:
          connections:
            order-server:
              # MCP Server 基础地址
              url: http://localhost:8080

              # Streamable HTTP 默认端点
              endpoint: /mcp
```

如果需要连接多个 MCP Server，可以继续在 `connections` 下增加不同名称的连接。每个连接会创建一个 MCP Client 实例；Spring AI 标准 MCP Client Starter 支持同时连接多个 STDIO、SSE、Streamable HTTP 和 Stateless Streamable HTTP Server。([Home](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-client-boot-starter-docs.html))

### Tool 调用流程

Tool 调用有两种常见方式：一种是直接使用 `McpSyncClient` 调用远程 Tool；另一种是通过 `ToolCallbackProvider` 将 MCP Tools 交给 ChatClient，由模型根据上下文决定是否调用。官方 Java MCP SDK 示例中，Client 支持 `listTools()` 发现工具，并通过 `callTool(new CallToolRequest(...))` 调用工具。([Home](https://docs.spring.io/spring-ai-mcp/reference/mcp.html))

直接调用流程如下：

```text
注入 McpSyncClient
  ↓
listTools() 查看远程工具
  ↓
构造 CallToolRequest
  ↓
callTool() 执行远程工具
  ↓
解析 CallToolResult
```

文件位置：`src/main/java/io/github/atengk/mcp/client/service/McpToolInvokeService.java`

下面的代码直接调用订单 MCP Server 暴露的 `query_order` 工具。

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

    public String queryOrder(String orderNo) {
        if (CollUtil.isEmpty(mcpSyncClients)) {
            log.error("MCP Tool 调用失败：未找到可用 MCP Client");
            throw new IllegalStateException("未找到可用 MCP Client");
        }

        McpSyncClient client = mcpSyncClients.get(0);
        log.info("开始调用 MCP Tool，tool=query_order，orderNo={}", orderNo);

        McpSchema.CallToolResult result = client.callTool(
                new McpSchema.CallToolRequest(
                        "query_order",
                        Map.of("orderNo", orderNo)
                )
        );

        String resultJson = JSONUtil.toJsonStr(result);
        log.info("MCP Tool 调用完成，tool=query_order，result={}", resultJson);
        return resultJson;
    }
}
```

如果要让大模型自动决定是否调用工具，可以注入 `ToolCallbackProvider` 并交给 ChatClient。Spring AI MCP 入门示例中，`ChatClient` 可以通过 `.toolCallbacks(mcpTools)` 使用 MCP 工具。([Home](https://docs.spring.io/spring-ai/reference/guides/getting-started-mcp.html))

文件位置：`src/main/java/io/github/atengk/mcp/client/service/McpChatService.java`

下面的代码将 MCP Tools 交给 ChatClient，由模型在回答订单问题时自动调用远程工具。

```java
package io.github.atengk.mcp.client.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Service;

/**
 * MCP 大模型对话服务。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class McpChatService {

    private final ChatClient chatClient;
    private final ToolCallbackProvider mcpTools;

    public String askOrderQuestion(String question) {
        log.info("开始执行 MCP 大模型问答，问题：{}", question);

        String content = chatClient
                .prompt(question)
                .toolCallbacks(mcpTools)
                .call()
                .content();

        log.info("MCP 大模型问答完成");
        return content;
    }
}
```

Tool 调用注意事项如下：

| 场景               | 处理建议                                           |
| ------------------ | -------------------------------------------------- |
| 工具不存在         | 先通过 `listTools()` 验证工具名称                  |
| 参数不匹配         | 检查 Tool 的 `inputSchema` 和 `@McpToolParam` 描述 |
| 调用超时           | 调整 `spring.ai.mcp.client.request-timeout`        |
| 多 Server 工具重名 | 使用清晰命名，或引入工具名前缀策略                 |
| 变更类 Tool        | 增加确认机制、权限校验和审计日志                   |

### Resource 读取流程

Resource 读取用于从 MCP Server 获取上下文资料，例如业务规则、文档内容、配置项、文件片段等。Java MCP SDK 示例中，Client 可以通过 `listResources()` 获取资源列表，并通过 `readResource(new ReadResourceRequest("resource://uri"))` 读取具体资源。([Home](https://docs.spring.io/spring-ai-mcp/reference/mcp.html))

Resource 读取流程如下：

```text
注入 McpSyncClient
  ↓
listResources() 查看资源列表
  ↓
构造 ReadResourceRequest
  ↓
readResource() 读取资源
  ↓
将资源内容作为模型上下文或业务结果使用
```

文件位置：`src/main/java/io/github/atengk/mcp/client/service/McpResourceReadService.java`

下面的代码读取订单退款规则资源 `order-rule://refund`。

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

    public String readOrderRule(String scene) {
        if (CollUtil.isEmpty(mcpSyncClients)) {
            log.error("MCP Resource 读取失败：未找到可用 MCP Client");
            throw new IllegalStateException("未找到可用 MCP Client");
        }

        String resourceUri = "order-rule://" + scene;
        McpSyncClient client = mcpSyncClients.get(0);

        log.info("开始读取 MCP Resource，uri={}", resourceUri);
        McpSchema.ReadResourceResult result = client.readResource(
                new McpSchema.ReadResourceRequest(resourceUri)
        );

        String resultJson = JSONUtil.toJsonStr(result);
        log.info("MCP Resource 读取完成，uri={}", resourceUri);
        return resultJson;
    }
}
```

Resource 读取结果通常不直接返回给用户，而是作为模型上下文参与后续回答。例如先读取退款规则，再把规则内容和用户问题一起交给 ChatClient，最后由模型生成解释性答案。

### Prompt 使用流程

Prompt 使用用于从 MCP Server 获取标准化提示词模板。Java MCP SDK 示例中，Client 可以通过 `listPrompts()` 获取可用 Prompt 列表，并通过 `getPrompt(new GetPromptRequest("greeting", Map.of(...)))` 获取具体 Prompt 内容。([Home](https://docs.spring.io/spring-ai-mcp/reference/mcp.html))

Prompt 使用流程如下：

```text
注入 McpSyncClient
  ↓
listPrompts() 查看 Prompt 列表
  ↓
构造 GetPromptRequest
  ↓
getPrompt() 获取 Prompt 内容
  ↓
将 Prompt 内容交给 ChatClient 或业务流程使用
```

文件位置：`src/main/java/io/github/atengk/mcp/client/service/McpPromptUseService.java`

下面的代码获取 `order_analysis` Prompt，并传入订单号和问题类型。

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

    public String getOrderAnalysisPrompt(String orderNo, String questionType) {
        if (CollUtil.isEmpty(mcpSyncClients)) {
            log.error("MCP Prompt 获取失败：未找到可用 MCP Client");
            throw new IllegalStateException("未找到可用 MCP Client");
        }

        McpSyncClient client = mcpSyncClients.get(0);
        log.info("开始获取 MCP Prompt，prompt=order_analysis，orderNo={}", orderNo);

        McpSchema.GetPromptResult result = client.getPrompt(
                new McpSchema.GetPromptRequest(
                        "order_analysis",
                        Map.of(
                                "orderNo", orderNo,
                                "questionType", questionType
                        )
                )
        );

        String resultJson = JSONUtil.toJsonStr(result);
        log.info("MCP Prompt 获取完成，prompt=order_analysis");
        return resultJson;
    }
}
```

Prompt 适合沉淀团队标准工作流。实际业务中可以先获取 Prompt，再结合 Resource 和 Tool 形成完整链路：Prompt 定义分析步骤，Resource 提供规则上下文，Tool 执行业务查询，ChatClient 负责生成最终回答。

整体推荐流程如下：

```text
用户问题
  ↓
获取 MCP Prompt：确定分析模板
  ↓
读取 MCP Resource：补充业务规则或文档上下文
  ↓
调用 MCP Tool：查询或处理业务数据
  ↓
ChatClient 汇总上下文并生成答案
```

在生产项目中，建议把 MCP Client 调用封装在独立 Service 中，不要在 Controller 或业务编排代码中直接散落 `callTool()`、`readResource()`、`getPrompt()` 调用。这样可以统一处理日志、超时、异常、重试、权限和返回值转换。

## 与大模型集成

本部分说明如何将 MCP Client 获取到的工具、资源和提示词接入大模型调用链路。Spring AI 中推荐通过 `ChatClient` 作为上层调用入口，它提供同步、流式调用、Prompt 构造、结构化返回和 Tool Calling 集成能力；MCP Client 暴露的远程工具可以通过 Spring AI 的 Tool Callback 机制交给模型使用。([Home](https://docs.spring.io/spring-ai/reference/api/chatclient.html?utm_source=chatgpt.com))

### ChatClient 接入方式

`ChatClient` 是 Spring AI 中面向聊天模型的高层 API，通常通过自动配置生成的 `ChatClient.Builder` 创建。它可以设置 system message、user message、模型参数、Advisor、ToolCallback 等内容，适合封装业务侧的 AI 调用入口。([Home](https://docs.spring.io/spring-ai/reference/api/chatclient.html?utm_source=chatgpt.com))

在 MCP 场景中，`ChatClient` 一般承担三类职责：

| 职责           | 说明                                                   |
| -------------- | ------------------------------------------------------ |
| 接收用户问题   | 将用户输入转换为模型 Prompt                            |
| 注入 MCP Tools | 将 MCP Server 暴露的工具交给模型选择调用               |
| 整合上下文     | 将 Resource 内容、Prompt 模板、Tool 结果组织成最终回答 |

推荐调用链路如下：

```text
用户请求
  ↓
Controller
  ↓
业务 Service
  ↓
读取 MCP Prompt / Resource
  ↓
ChatClient 注入 MCP Tools
  ↓
模型自动判断是否调用 Tool
  ↓
返回最终结果
```

文件位置：`src/main/java/io/github/atengk/mcp/client/config/ChatClientConfig.java`

下面的配置类用于创建统一的 `ChatClient`，在这里设置系统提示词和默认模型行为。

```java
package io.github.atengk.mcp.client.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ChatClient 配置。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Configuration
public class ChatClientConfig {

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder
                .defaultSystem("""
                        你是企业业务助手，需要基于 MCP 工具、资源和提示词回答问题。
                        如果需要业务数据，优先调用可用工具；如果信息不足，需要明确说明缺失信息。
                        不要编造订单、用户、库存、规则等业务数据。
                        """)
                .build();
    }
}
```

文件位置：`src/main/java/io/github/atengk/mcp/client/service/McpAiService.java`

下面的接口定义 AI 业务调用入口，Controller 不直接操作 `ChatClient` 和 MCP Client。

```java
package io.github.atengk.mcp.client.service;

import io.github.atengk.mcp.client.vo.McpChatResponseVO;

/**
 * MCP AI 服务。
 *
 * @author Ateng
 * @since 2026-05-11
 */
public interface McpAiService {

    /**
     * 执行业务问答。
     *
     * @param question 用户问题
     * @return 问答结果
     */
    McpChatResponseVO chat(String question);
}
```

文件位置：`src/main/java/io/github/atengk/mcp/client/service/impl/McpAiServiceImpl.java`

下面的实现类将 MCP Tools 注入 `ChatClient`，使模型可以在回答时自动调用远程 MCP Server 暴露的工具。

```java
package io.github.atengk.mcp.client.service.impl;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.mcp.client.service.McpAiService;
import io.github.atengk.mcp.client.vo.McpChatResponseVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Service;

/**
 * MCP AI 服务实现。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class McpAiServiceImpl implements McpAiService {

    private final ChatClient chatClient;
    private final ToolCallbackProvider mcpTools;

    /**
     * 执行业务问答。
     *
     * @param question 用户问题
     * @return 问答结果
     */
    @Override
    public McpChatResponseVO chat(String question) {
        if (StrUtil.isBlank(question)) {
            log.warn("MCP AI 问答失败：问题为空");
            throw new IllegalArgumentException("问题不能为空");
        }

        log.info("开始执行 MCP AI 问答，question={}", question);

        String answer = chatClient
                .prompt()
                .user(question)
                .toolCallbacks(mcpTools)
                .call()
                .content();

        log.info("MCP AI 问答完成");
        return McpChatResponseVO.builder()
                .question(question)
                .answer(answer)
                .build();
    }
}
```

这种方式适合模型自主判断是否需要调用工具。例如用户询问“帮我查一下 ORDER_10001 的退款状态”，模型可以根据工具描述选择调用 `query_order`，再基于工具返回结果生成自然语言答复。

### Function Calling 与 Tool 调用

Spring AI 的 Tool Calling 通过 `ToolCallback` 抽象工具定义和执行逻辑。工具定义用于告诉模型“什么时候、如何调用工具”，执行逻辑用于真正处理工具调用。MCP 工具可以通过 Spring AI MCP Utilities 或 MCP Client Boot Starter 转换为 Spring AI Tool Callback，从而进入 `ChatClient` 的工具调用链路。([Home](https://docs.spring.io/spring-ai/reference/api/tools.html?utm_source=chatgpt.com))

MCP 与 Function Calling 的关系可以理解为：

| 层级            | 作用                                           |
| --------------- | ---------------------------------------------- |
| MCP Server Tool | 远程暴露业务能力                               |
| MCP Client      | 发现并调用远程 MCP Tool                        |
| ToolCallback    | 将 MCP Tool 适配为 Spring AI 可识别的工具      |
| ChatClient      | 将工具定义发送给模型，并执行模型选择的工具调用 |
| 大模型          | 根据用户问题和工具描述判断是否调用工具         |

典型执行流程如下：

```text
用户问题
  ↓
ChatClient 构造 Prompt
  ↓
发送可用 Tool 定义给模型
  ↓
模型判断需要调用某个 Tool
  ↓
Spring AI 执行 ToolCallback
  ↓
ToolCallback 调用 MCP Client
  ↓
MCP Client 调用 MCP Server Tool
  ↓
Tool 结果返回模型
  ↓
模型生成最终回答
```

如果业务希望绕过模型决策，直接由代码调用 MCP Tool，也可以通过 `McpSyncClient` 主动执行。Java MCP SDK 支持工具发现和工具执行，典型 API 包括 `listTools()` 和 `callTool(...)`。([Home](https://docs.spring.io/spring-ai-mcp/reference/mcp.html?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/mcp/client/service/DirectMcpToolService.java`

下面的服务类演示直接调用 MCP Tool，适合后端业务流程明确知道需要调用哪个工具的场景。

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
 * 直接 MCP Tool 调用服务。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DirectMcpToolService {

    private final List<McpSyncClient> mcpSyncClients;

    /**
     * 直接查询订单信息。
     *
     * @param orderNo 订单号
     * @return MCP Tool 原始响应 JSON
     */
    public String queryOrder(String orderNo) {
        if (CollUtil.isEmpty(mcpSyncClients)) {
            log.error("直接调用 MCP Tool 失败：未找到 MCP Client");
            throw new IllegalStateException("未找到 MCP Client");
        }

        log.info("开始直接调用 MCP Tool，tool=query_order，orderNo={}", orderNo);

        McpSchema.CallToolResult result = mcpSyncClients.get(0).callTool(
                new McpSchema.CallToolRequest(
                        "query_order",
                        Map.of("orderNo", orderNo)
                )
        );

        String resultJson = JSONUtil.toJsonStr(result);
        log.info("直接调用 MCP Tool 完成，tool=query_order");
        return resultJson;
    }
}
```

两种调用方式的选择建议如下：

| 调用方式                            | 适用场景                                   |
| ----------------------------------- | ------------------------------------------ |
| `ChatClient + ToolCallbackProvider` | 让模型根据问题自主判断是否调用工具         |
| `McpSyncClient.callTool()`          | 后端流程明确知道要调用哪个工具             |
| `McpAsyncClient.callTool()`         | WebFlux、响应式或高并发异步调用场景        |
| 先直接调用 Tool，再交给 ChatClient  | 业务必须先获取数据，再由模型负责解释和总结 |

实际项目中，不建议所有调用都交给模型自动决策。对于支付、审批、删除、状态变更等敏感操作，应由业务代码先完成权限校验和二次确认，再决定是否调用 MCP Tool。

### 上下文传递与结果处理

MCP 集成大模型时，不能只关注 Tool 调用，还需要处理 Resource、Prompt 和 Tool 结果之间的上下文传递。MCP Client 支持资源读取、Prompt 获取和工具执行，Java MCP SDK 中包含 `listResources()`、`readResource(...)`、`listPrompts()`、`getPrompt(...)`、`callTool(...)` 等标准操作。([Home](https://docs.spring.io/spring-ai-mcp/reference/mcp.html?utm_source=chatgpt.com))

推荐的上下文处理流程如下：

```text
用户问题
  ↓
根据场景读取 MCP Prompt
  ↓
根据业务类型读取 MCP Resource
  ↓
组合 system / user 上下文
  ↓
注入 MCP Tools
  ↓
调用 ChatClient
  ↓
解析模型结果
  ↓
返回统一业务响应
```

文件位置：`src/main/java/io/github/atengk/mcp/client/service/impl/McpContextAiServiceImpl.java`

下面的实现类演示“读取业务规则 Resource + 注入 MCP Tools + 调用 ChatClient”的完整流程。

```java
package io.github.atengk.mcp.client.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import io.github.atengk.mcp.client.service.McpAiService;
import io.github.atengk.mcp.client.vo.McpChatResponseVO;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 带上下文的 MCP AI 服务实现。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class McpContextAiServiceImpl implements McpAiService {

    private final ChatClient chatClient;
    private final ToolCallbackProvider mcpTools;
    private final List<McpSyncClient> mcpSyncClients;

    /**
     * 执行业务问答。
     *
     * @param question 用户问题
     * @return 问答结果
     */
    @Override
    public McpChatResponseVO chat(String question) {
        if (StrUtil.isBlank(question)) {
            log.warn("MCP 上下文问答失败：问题为空");
            throw new IllegalArgumentException("问题不能为空");
        }

        String ruleContext = readOrderRuleContext("refund");
        log.info("开始执行 MCP 上下文问答，question={}", question);

        String answer = chatClient
                .prompt()
                .system(system -> system.text("""
                        你是订单业务助手。
                        以下是当前业务规则上下文，请优先基于这些规则回答：
                        {ruleContext}
                        """).param("ruleContext", ruleContext))
                .user(question)
                .toolCallbacks(mcpTools)
                .call()
                .content();

        log.info("MCP 上下文问答完成");
        return McpChatResponseVO.builder()
                .question(question)
                .context(ruleContext)
                .answer(answer)
                .build();
    }

    /**
     * 读取订单规则上下文。
     *
     * @param scene 业务场景
     * @return 规则上下文
     */
    private String readOrderRuleContext(String scene) {
        if (CollUtil.isEmpty(mcpSyncClients)) {
            log.warn("读取 MCP Resource 失败：未找到 MCP Client，将使用空上下文");
            return "";
        }

        String uri = "order-rule://" + scene;
        McpSchema.ReadResourceResult result = mcpSyncClients.get(0).readResource(
                new McpSchema.ReadResourceRequest(uri)
        );

        String context = JSONUtil.toJsonStr(result);
        log.info("读取 MCP Resource 成功，uri={}", uri);
        return context;
    }
}
```

结果处理建议如下：

| 处理项        | 建议                                                 |
| ------------- | ---------------------------------------------------- |
| Tool 返回值   | 优先使用结构化 JSON，避免难解析的长文本              |
| Resource 内容 | 控制长度，只传递与问题相关的上下文                   |
| Prompt 模板   | 明确输出格式、判断步骤和禁止编造要求                 |
| 模型结果      | 可使用 `entity()` 映射为结构化对象，便于后续业务处理 |
| 异常结果      | 统一转换为业务错误码，不直接暴露底层协议异常         |
| 日志          | 记录请求、上下文来源、工具名称、耗时和异常原因       |

如果业务接口要求返回结构化对象，可以让模型直接输出指定 Java 类型。`ChatClient` 支持通过 `entity()` 将模型输出映射为目标对象，也支持返回 `ChatResponse` 以获取响应元数据。([Home](https://docs.spring.io/spring-ai/reference/api/chatclient.html?utm_source=chatgpt.com))

## 接口与业务封装

本部分说明如何把 MCP 调用能力封装成稳定的业务接口。实际项目中不建议 Controller 直接操作 `McpSyncClient`、`ToolCallbackProvider` 或 `ChatClient`，否则后续增加权限、日志、重试、限流、异常处理和多 Server 路由时会比较困难。推荐使用 Controller、Service、DTO、VO、异常处理器分层封装。

### MCP 服务封装

MCP 服务封装的目标是隐藏底层协议细节，对上层业务提供稳定方法。例如上层只关心“发起订单问答”，不应该关心 ToolCallback 如何注入、Resource URI 如何拼接、MCP Client 列表如何选择。

推荐分层如下：

| 层级                | 职责                                               |
| ------------------- | -------------------------------------------------- |
| Controller          | 接收 HTTP 请求，做基础参数校验                     |
| Application Service | 编排 Prompt、Resource、Tool 和 ChatClient          |
| MCP Gateway Service | 封装 `callTool()`、`readResource()`、`getPrompt()` |
| Business Service    | 处理具体业务逻辑                                   |
| Exception Handler   | 统一异常转换和日志记录                             |

文件位置：`src/main/java/io/github/atengk/mcp/client/service/McpGatewayService.java`

下面的接口封装 MCP 底层访问能力，供业务编排层调用。

```java
package io.github.atengk.mcp.client.service;

import java.util.Map;

/**
 * MCP 网关服务。
 *
 * @author Ateng
 * @since 2026-05-11
 */
public interface McpGatewayService {

    /**
     * 调用 MCP Tool。
     *
     * @param toolName 工具名称
     * @param arguments 工具参数
     * @return 工具调用结果 JSON
     */
    String callTool(String toolName, Map<String, Object> arguments);

    /**
     * 读取 MCP Resource。
     *
     * @param uri 资源 URI
     * @return 资源内容 JSON
     */
    String readResource(String uri);

    /**
     * 获取 MCP Prompt。
     *
     * @param promptName Prompt 名称
     * @param arguments Prompt 参数
     * @return Prompt 内容 JSON
     */
    String getPrompt(String promptName, Map<String, Object> arguments);
}
```

文件位置：`src/main/java/io/github/atengk/mcp/client/service/impl/McpGatewayServiceImpl.java`

下面的实现类统一处理 MCP Client 获取、Tool 调用、Resource 读取、Prompt 获取和日志记录。

```java
package io.github.atengk.mcp.client.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import io.github.atengk.mcp.client.service.McpGatewayService;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * MCP 网关服务实现。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class McpGatewayServiceImpl implements McpGatewayService {

    private final List<McpSyncClient> mcpSyncClients;

    /**
     * 调用 MCP Tool。
     *
     * @param toolName 工具名称
     * @param arguments 工具参数
     * @return 工具调用结果 JSON
     */
    @Override
    public String callTool(String toolName, Map<String, Object> arguments) {
        if (StrUtil.isBlank(toolName)) {
            throw new IllegalArgumentException("工具名称不能为空");
        }

        McpSyncClient client = getDefaultClient();
        Map<String, Object> safeArguments = MapUtil.emptyIfNull(arguments);

        log.info("开始调用 MCP Tool，toolName={}，arguments={}", toolName, safeArguments);
        McpSchema.CallToolResult result = client.callTool(
                new McpSchema.CallToolRequest(toolName, safeArguments)
        );

        log.info("MCP Tool 调用完成，toolName={}", toolName);
        return JSONUtil.toJsonStr(result);
    }

    /**
     * 读取 MCP Resource。
     *
     * @param uri 资源 URI
     * @return 资源内容 JSON
     */
    @Override
    public String readResource(String uri) {
        if (StrUtil.isBlank(uri)) {
            throw new IllegalArgumentException("资源 URI 不能为空");
        }

        McpSyncClient client = getDefaultClient();

        log.info("开始读取 MCP Resource，uri={}", uri);
        McpSchema.ReadResourceResult result = client.readResource(
                new McpSchema.ReadResourceRequest(uri)
        );

        log.info("MCP Resource 读取完成，uri={}", uri);
        return JSONUtil.toJsonStr(result);
    }

    /**
     * 获取 MCP Prompt。
     *
     * @param promptName Prompt 名称
     * @param arguments Prompt 参数
     * @return Prompt 内容 JSON
     */
    @Override
    public String getPrompt(String promptName, Map<String, Object> arguments) {
        if (StrUtil.isBlank(promptName)) {
            throw new IllegalArgumentException("Prompt 名称不能为空");
        }

        McpSyncClient client = getDefaultClient();
        Map<String, Object> safeArguments = MapUtil.emptyIfNull(arguments);

        log.info("开始获取 MCP Prompt，promptName={}，arguments={}", promptName, safeArguments);
        McpSchema.GetPromptResult result = client.getPrompt(
                new McpSchema.GetPromptRequest(promptName, safeArguments)
        );

        log.info("MCP Prompt 获取完成，promptName={}", promptName);
        return JSONUtil.toJsonStr(result);
    }

    /**
     * 获取默认 MCP Client。
     *
     * @return MCP Client
     */
    private McpSyncClient getDefaultClient() {
        if (CollUtil.isEmpty(mcpSyncClients)) {
            log.error("MCP 操作失败：未找到可用 MCP Client");
            throw new IllegalStateException("未找到可用 MCP Client");
        }
        return mcpSyncClients.get(0);
    }
}
```

### 业务方法设计

业务方法设计时，应避免把 MCP 协议对象直接暴露到 Controller 或前端响应中。推荐把 MCP 的 Tool、Resource、Prompt 调用结果转换成业务 VO，或者将其作为上下文交给模型处理后再返回业务结果。

推荐方法设计如下：

| 方法类型            | 示例                              | 说明                               |
| ------------------- | --------------------------------- | ---------------------------------- |
| AI 问答方法         | `chat(McpChatRequestDTO request)` | 面向用户自然语言问题               |
| Tool 直接调用方法   | `queryOrder(String orderNo)`      | 面向明确业务流程                   |
| Resource 上下文方法 | `readRule(String scene)`          | 面向规则、文档读取                 |
| Prompt 模板方法     | `buildPrompt(String orderNo)`     | 面向提示词复用                     |
| 组合编排方法        | `analyzeOrderIssue(...)`          | 组合 Prompt、Resource、Tool 和模型 |

文件位置：`src/main/java/io/github/atengk/mcp/client/service/OrderAiApplicationService.java`

下面的接口定义订单 AI 场景的业务编排方法。

```java
package io.github.atengk.mcp.client.service;

import io.github.atengk.mcp.client.dto.OrderIssueAnalyzeDTO;
import io.github.atengk.mcp.client.vo.McpChatResponseVO;

/**
 * 订单 AI 应用服务。
 *
 * @author Ateng
 * @since 2026-05-11
 */
public interface OrderAiApplicationService {

    /**
     * 分析订单问题。
     *
     * @param request 分析请求
     * @return 分析结果
     */
    McpChatResponseVO analyzeOrderIssue(OrderIssueAnalyzeDTO request);
}
```

文件位置：`src/main/java/io/github/atengk/mcp/client/service/impl/OrderAiApplicationServiceImpl.java`

下面的实现类演示业务编排：先读取规则，再将规则上下文和用户问题交给 `ChatClient`，同时注入 MCP Tools。

```java
package io.github.atengk.mcp.client.service.impl;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.mcp.client.dto.OrderIssueAnalyzeDTO;
import io.github.atengk.mcp.client.service.McpGatewayService;
import io.github.atengk.mcp.client.service.OrderAiApplicationService;
import io.github.atengk.mcp.client.vo.McpChatResponseVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Service;

/**
 * 订单 AI 应用服务实现。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderAiApplicationServiceImpl implements OrderAiApplicationService {

    private final ChatClient chatClient;
    private final ToolCallbackProvider mcpTools;
    private final McpGatewayService mcpGatewayService;

    /**
     * 分析订单问题。
     *
     * @param request 分析请求
     * @return 分析结果
     */
    @Override
    public McpChatResponseVO analyzeOrderIssue(OrderIssueAnalyzeDTO request) {
        if (request == null || StrUtil.isBlank(request.getQuestion())) {
            throw new IllegalArgumentException("分析问题不能为空");
        }

        String scene = StrUtil.blankToDefault(request.getScene(), "refund");
        String ruleContext = mcpGatewayService.readResource("order-rule://" + scene);

        log.info("开始分析订单问题，orderNo={}，scene={}", request.getOrderNo(), scene);

        String answer = chatClient
                .prompt()
                .system(system -> system.text("""
                        你是订单业务助手，请基于以下业务规则分析用户问题。
                        业务规则：
                        {ruleContext}

                        回答要求：
                        1. 如果需要订单详情，可以调用可用 MCP Tool。
                        2. 输出当前判断依据、处理建议和缺失信息。
                        3. 不要编造工具没有返回的数据。
                        """).param("ruleContext", ruleContext))
                .user(user -> user.text("""
                        订单号：{orderNo}
                        用户问题：{question}
                        """)
                        .param("orderNo", request.getOrderNo())
                        .param("question", request.getQuestion()))
                .toolCallbacks(mcpTools)
                .call()
                .content();

        log.info("订单问题分析完成，orderNo={}", request.getOrderNo());

        return McpChatResponseVO.builder()
                .question(request.getQuestion())
                .context(ruleContext)
                .answer(answer)
                .build();
    }
}
```

### 请求参数与响应结构

接口层建议使用 DTO 接收请求参数，使用 VO 返回响应结果。DTO 负责参数校验，VO 负责对外输出，避免直接暴露 MCP SDK 的原始对象。这样后续即使替换 MCP Client、调整 Tool 名称或修改 Resource URI，也不会影响前端或调用方。

文件位置：`src/main/java/io/github/atengk/mcp/client/dto/OrderIssueAnalyzeDTO.java`

下面的 DTO 用于接收订单问题分析请求。

```java
package io.github.atengk.mcp.client.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 订单问题分析请求。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Data
public class OrderIssueAnalyzeDTO {

    /**
     * 订单号。
     */
    @NotBlank(message = "订单号不能为空")
    private String orderNo;

    /**
     * 问题场景，例如 refund、delivery、after_sale。
     */
    private String scene;

    /**
     * 用户问题。
     */
    @NotBlank(message = "用户问题不能为空")
    private String question;
}
```

文件位置：`src/main/java/io/github/atengk/mcp/client/vo/McpChatResponseVO.java`

下面的 VO 用于返回大模型回答、上下文和原始问题。

```java
package io.github.atengk.mcp.client.vo;

import lombok.Builder;
import lombok.Data;

/**
 * MCP 问答响应。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Data
@Builder
public class McpChatResponseVO {

    /**
     * 用户问题。
     */
    private String question;

    /**
     * 使用的上下文内容。
     */
    private String context;

    /**
     * 大模型回答。
     */
    private String answer;
}
```

文件位置：`src/main/java/io/github/atengk/mcp/client/vo/ApiResult.java`

下面的统一响应结构用于包装接口返回值。

```java
package io.github.atengk.mcp.client.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一接口响应。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResult<T> {

    /**
     * 是否成功。
     */
    private Boolean success;

    /**
     * 响应编码。
     */
    private String code;

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
     * @return 响应结果
     */
    public static <T> ApiResult<T> success(T data) {
        return new ApiResult<>(true, "0", "操作成功", data);
    }

    /**
     * 失败响应。
     *
     * @param code 响应编码
     * @param message 响应消息
     * @param <T> 数据类型
     * @return 响应结果
     */
    public static <T> ApiResult<T> fail(String code, String message) {
        return new ApiResult<>(false, code, message, null);
    }
}
```

文件位置：`src/main/java/io/github/atengk/mcp/client/controller/OrderAiController.java`

下面的 Controller 提供订单问题分析接口，外部系统只需要调用业务接口，不需要理解 MCP 协议细节。

```java
package io.github.atengk.mcp.client.controller;

import io.github.atengk.mcp.client.dto.OrderIssueAnalyzeDTO;
import io.github.atengk.mcp.client.service.OrderAiApplicationService;
import io.github.atengk.mcp.client.vo.ApiResult;
import io.github.atengk.mcp.client.vo.McpChatResponseVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 订单 AI 接口。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ai/order")
public class OrderAiController {

    private final OrderAiApplicationService orderAiApplicationService;

    /**
     * 分析订单问题。
     *
     * @param request 请求参数
     * @return 分析结果
     */
    @PostMapping("/analyze")
    public ApiResult<McpChatResponseVO> analyze(@Valid @RequestBody OrderIssueAnalyzeDTO request) {
        return ApiResult.success(orderAiApplicationService.analyzeOrderIssue(request));
    }
}
```

接口调用示例如下。

```bash
curl -X POST 'http://localhost:8081/api/ai/order/analyze' \
  -H 'Content-Type: application/json' \
  -d '{
    "orderNo": "ORDER_10001",
    "scene": "refund",
    "question": "这个订单能不能退款？请给出判断依据。"
  }'
```

该命令向 MCP Client 应用发起订单分析请求。`orderNo` 表示订单号，`scene` 表示要读取的业务规则场景，`question` 表示用户的自然语言问题。接口内部会读取对应 MCP Resource，并通过 `ChatClient` 携带 MCP Tools 完成分析。

响应示例：

```json
{
  "success": true,
  "code": "0",
  "message": "操作成功",
  "data": {
    "question": "这个订单能不能退款？请给出判断依据。",
    "context": "...",
    "answer": "根据当前退款规则和订单状态，该订单..."
  }
}
```

### 异常处理

MCP 调用涉及远程连接、协议处理、模型调用、参数校验和业务执行，异常来源较多。建议使用全局异常处理器统一转换异常结果，避免将 MCP SDK 的底层异常、模型供应商错误或堆栈信息直接暴露给前端。

常见异常类型如下：

| 异常类型      | 典型原因                            | 处理建议                           |
| ------------- | ----------------------------------- | ---------------------------------- |
| 参数异常      | 请求参数为空、格式错误              | 返回 `400` 类业务错误              |
| MCP 连接异常  | Server 未启动、地址错误、网络不可达 | 返回服务不可用，并记录 Server 地址 |
| MCP 调用超时  | Tool 执行慢、模型调用慢、网络延迟   | 调整超时，必要时增加重试           |
| Tool 执行异常 | 参数不合法、业务数据不存在          | 转换为业务错误消息                 |
| 模型调用异常  | API Key 错误、模型限流、模型不可用  | 返回 AI 服务异常                   |
| 结果解析异常  | 模型未按预期格式返回                | 增加输出格式约束或兜底解析         |

文件位置：`src/main/java/io/github/atengk/mcp/client/handler/GlobalExceptionHandler.java`

下面的全局异常处理器统一处理参数异常和运行时异常，并记录中文日志。

```java
package io.github.atengk.mcp.client.handler;

import io.github.atengk.mcp.client.vo.ApiResult;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
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
     * 处理请求体参数校验异常。
     *
     * @param exception 参数校验异常
     * @return 统一响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResult<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("请求参数不合法");

        log.warn("请求参数校验失败：{}", message);
        return ApiResult.fail("400", message);
    }

    /**
     * 处理普通参数异常。
     *
     * @param exception 参数异常
     * @return 统一响应
     */
    @ExceptionHandler({IllegalArgumentException.class, ConstraintViolationException.class})
    public ApiResult<Void> handleIllegalArgumentException(Exception exception) {
        log.warn("请求参数错误：{}", exception.getMessage());
        return ApiResult.fail("400", exception.getMessage());
    }

    /**
     * 处理非法状态异常。
     *
     * @param exception 状态异常
     * @return 统一响应
     */
    @ExceptionHandler(IllegalStateException.class)
    public ApiResult<Void> handleIllegalStateException(IllegalStateException exception) {
        log.error("服务状态异常：{}", exception.getMessage(), exception);
        return ApiResult.fail("503", exception.getMessage());
    }

    /**
     * 处理未知异常。
     *
     * @param exception 未知异常
     * @return 统一响应
     */
    @ExceptionHandler(Exception.class)
    public ApiResult<Void> handleException(Exception exception) {
        log.error("系统异常：{}", exception.getMessage(), exception);
        return ApiResult.fail("500", "系统繁忙，请稍后重试");
    }
}
```

生产环境建议进一步补充以下处理：

| 能力     | 说明                                                      |
| -------- | --------------------------------------------------------- |
| 超时配置 | 为 MCP Client、模型调用、HTTP 客户端分别设置合理超时      |
| 重试策略 | 只对网络抖动、临时不可用等可恢复异常做有限重试            |
| 熔断限流 | 防止 MCP Server 或模型服务异常拖垮主业务系统              |
| 审计日志 | 对变更类 Tool 记录调用人、参数、结果和时间                |
| 脱敏处理 | 日志中避免输出身份证号、手机号、Token、API Key 等敏感数据 |
| 降级响应 | MCP Server 不可用时返回可理解的业务提示，而不是堆栈信息   |

## 配置说明

本部分说明 Spring AI 1.x MCP 开发中常用的 `application.yml` 配置方式，重点包括 MCP Server 配置、MCP Client 配置、多个 MCP Server 连接配置，以及超时和连接参数。Spring AI MCP Server Boot Starter 支持 STDIO、SSE、Streamable HTTP、Stateless 等协议，并支持同步和异步模式；MCP Client Boot Starter 支持多个命名连接，并可与 Spring AI Tool Callback 机制集成。([Home](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-server-boot-starter-docs.html))

### application 配置项

`application.yml` 通常分为三部分：Spring Boot 应用配置、模型配置、MCP 配置。MCP Server 侧重点是暴露工具、资源和提示词；MCP Client 侧重点是连接 Server、设置超时时间、启用 ToolCallback，并将远程 MCP Tool 接入 `ChatClient`。Spring AI 官方文档中，MCP Server 能力包括 Tools、Resources、Prompts、Completion、Logging、Progress、Ping 等，并且 Tools、Resources、Prompts 等能力默认启用。([Home](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-server-boot-starter-docs.html))

MCP Server 示例配置如下。

文件位置：`src/main/resources/application.yml`

```yaml
server:
  # MCP Server 服务端口
  port: 8080

spring:
  application:
    # 当前 MCP Server 应用名称
    name: spring-ai-mcp-server

  ai:
    mcp:
      server:
        # 是否启用 MCP Server
        enabled: true

        # MCP Server 名称，便于 Client 识别服务来源
        name: order-mcp-server

        # MCP Server 版本
        version: 1.0.0

        # 服务端类型：SYNC 适合 Spring MVC 阻塞式业务；ASYNC 适合 WebFlux 响应式业务
        type: SYNC

        # WebMVC Streamable HTTP 模式
        protocol: STREAMABLE

        # 是否启用 stdio；WebMVC HTTP 服务一般保持 false
        stdio: false

        capabilities:
          # 是否暴露 Tool 能力
          tool: true

          # 是否暴露 Resource 能力
          resource: true

          # 是否暴露 Prompt 能力
          prompt: true

          # 是否暴露参数补全能力
          completion: true
```

上面的配置适用于 `spring-ai-starter-mcp-server-webmvc`。如果使用 STDIO 类型的 MCP Server，应使用 `spring-ai-starter-mcp-server`，并配置 `spring.ai.mcp.server.stdio=true`；如果使用 WebMVC Streamable HTTP，则使用 `spring-ai-starter-mcp-server-webmvc`，并配置 `spring.ai.mcp.server.protocol=STREAMABLE`。([Home](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-server-boot-starter-docs.html))

MCP Client 示例配置如下。

文件位置：`src/main/resources/application.yml`

```yaml
server:
  # MCP Client 应用端口
  port: 8081

spring:
  application:
    # 当前 MCP Client 应用名称
    name: spring-ai-mcp-client

  ai:
    openai:
      # 模型 API Key 建议通过环境变量注入，避免写死到配置文件
      api-key: ${OPENAI_API_KEY}
      chat:
        options:
          # 示例模型名称，按实际模型服务调整
          model: gpt-4o-mini

    mcp:
      client:
        # 是否启用 MCP Client
        enabled: true

        # MCP Client 名称
        name: order-ai-client

        # MCP Client 版本
        version: 1.0.0

        # 是否在创建时初始化 MCP Client
        initialized: true

        # MCP Client 请求超时时间，默认值为 20s
        request-timeout: 30s

        # Client 类型：SYNC 或 ASYNC；同一应用内不能混用同步和异步 Client
        type: SYNC

        toolcallback:
          # 是否启用 MCP Tool 与 Spring AI ToolCallback 的集成
          enabled: true

        streamable-http:
          connections:
            order-server:
              # MCP Server 基础地址
              url: http://localhost:8080

              # Streamable HTTP 默认端点
              endpoint: /mcp
```

MCP Client 的公共配置前缀是 `spring.ai.mcp.client`，常用配置包括 `enabled`、`name`、`version`、`initialized`、`request-timeout`、`type`、`toolcallback.enabled` 等；其中 `request-timeout` 默认值为 `20s`，`type` 默认值为 `SYNC`，`toolcallback.enabled` 默认启用。([Home](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-client-boot-starter-docs.html))

常用配置项说明如下。

| 配置项                                       | 所属端 | 说明                       | 常用值                             |
| -------------------------------------------- | ------ | -------------------------- | ---------------------------------- |
| `spring.ai.mcp.server.enabled`               | Server | 是否启用 MCP Server        | `true`                             |
| `spring.ai.mcp.server.name`                  | Server | MCP Server 名称            | `order-mcp-server`                 |
| `spring.ai.mcp.server.version`               | Server | MCP Server 版本            | `1.0.0`                            |
| `spring.ai.mcp.server.type`                  | Server | 服务端 API 类型            | `SYNC` / `ASYNC`                   |
| `spring.ai.mcp.server.protocol`              | Server | HTTP 传输协议              | `SSE` / `STREAMABLE` / `STATELESS` |
| `spring.ai.mcp.server.stdio`                 | Server | 是否启用 STDIO             | `true` / `false`                   |
| `spring.ai.mcp.server.capabilities.tool`     | Server | 是否暴露 Tool 能力         | `true`                             |
| `spring.ai.mcp.server.capabilities.resource` | Server | 是否暴露 Resource 能力     | `true`                             |
| `spring.ai.mcp.server.capabilities.prompt`   | Server | 是否暴露 Prompt 能力       | `true`                             |
| `spring.ai.mcp.client.enabled`               | Client | 是否启用 MCP Client        | `true`                             |
| `spring.ai.mcp.client.initialized`           | Client | 是否启动时初始化 Client    | `true`                             |
| `spring.ai.mcp.client.request-timeout`       | Client | MCP 请求超时时间           | `20s` / `30s`                      |
| `spring.ai.mcp.client.type`                  | Client | Client 类型                | `SYNC` / `ASYNC`                   |
| `spring.ai.mcp.client.toolcallback.enabled`  | Client | 是否启用 ToolCallback 集成 | `true`                             |

### 多 MCP Server 配置

MCP Client 可以同时连接多个 MCP Server。Spring AI MCP Client Boot Starter 支持多个命名传输连接，每个连接会创建对应的 MCP Client 实例；标准 Client Starter 支持 STDIO、SSE、Streamable HTTP 和 Stateless Streamable HTTP 连接。([Home](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-client-boot-starter-docs.html))

多个 Streamable HTTP Server 的配置如下。

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  ai:
    mcp:
      client:
        enabled: true
        name: enterprise-ai-client
        request-timeout: 30s
        type: SYNC

        toolcallback:
          # 启用后，多个 Server 暴露的 Tool 会进入 Spring AI ToolCallback 体系
          enabled: true

        streamable-http:
          connections:
            order-server:
              # 订单 MCP Server
              url: http://localhost:8080
              endpoint: /mcp

            inventory-server:
              # 库存 MCP Server
              url: http://localhost:8082
              endpoint: /mcp

            logistics-server:
              # 物流 MCP Server，如果服务端自定义端点，需要同步修改 endpoint
              url: http://localhost:8083
              endpoint: /mcp
```

官方配置中，Streamable HTTP 连接位于 `spring.ai.mcp.client.streamable-http.connections` 下，`connections.[name].url` 表示 Server 基础地址，`connections.[name].endpoint` 表示端点后缀，默认值为 `/mcp`。([Home](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-client-boot-starter-docs.html))

多个 Server 接入时，需要重点关注工具命名冲突。比如订单服务和售后服务都提供 `query_order`，模型可能无法准确判断应该调用哪个工具。建议采用以下命名策略：

| 场景     | 推荐命名                   |
| -------- | -------------------------- |
| 订单查询 | `order_query_detail`       |
| 库存查询 | `inventory_query_stock`    |
| 物流查询 | `logistics_query_tracking` |
| 售后创建 | `aftersale_create_ticket`  |
| 用户查询 | `user_query_profile`       |

也可以通过 Spring AI MCP Client 的 Tool 过滤和工具名前缀能力处理冲突；MCP Client Boot Starter 文档明确包含 Tool filtering 和可自定义 Tool name prefix generation，用于选择性引入工具和避免命名冲突。([Home](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-client-boot-starter-docs.html))

如果同时需要连接本地 STDIO Server 和远程 HTTP Server，可以组合配置。STDIO 通常适合本地进程型工具，例如文件系统、命令行工具、桌面集成；HTTP 模式更适合独立部署的业务服务。

```yaml
spring:
  ai:
    mcp:
      client:
        enabled: true
        type: SYNC
        request-timeout: 30s

        stdio:
          connections:
            local-file-server:
              # 本地 MCP Server 启动命令
              command: npx
              args:
                - -y
                - "@modelcontextprotocol/server-filesystem"
                - "/Users/ateng/workspace"

        streamable-http:
          connections:
            order-server:
              # 远程订单 MCP Server
              url: http://localhost:8080
              endpoint: /mcp
```

在 Windows 环境下，如果 STDIO 命令是 `npx`、`npm`、`python`、`pip`、`mvn`、`gradle` 或自定义 `.cmd` / `.bat` 脚本，通常需要使用 `cmd.exe /c` 包装命令，因为 Java `ProcessBuilder` 不能直接执行批处理文件。([Home](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-client-boot-starter-docs.html))

### 超时与连接参数

MCP 调用涉及 Client 到 Server 的网络通信、Server 业务处理、模型推理和工具结果回传，因此必须设置合理的超时时间。Spring AI MCP Client 的 `request-timeout` 用于控制 MCP Client 请求超时，默认值为 `20s`；如果 Tool 内部需要调用数据库、第三方接口或大模型，应额外在业务层和 HTTP 客户端层设置超时。([Home](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-client-boot-starter-docs.html))

推荐配置如下。

```yaml
spring:
  ai:
    mcp:
      client:
        # 普通查询类工具建议 10s-30s；复杂工具可适当放宽
        request-timeout: 30s

    openai:
      chat:
        options:
          # 根据业务质量和响应速度要求调整
          temperature: 0.2
```

不同场景的超时建议如下。

| 场景                 | 建议超时       | 说明                                       |
| -------------------- | -------------- | ------------------------------------------ |
| 本地 STDIO 工具      | `5s` - `15s`   | 本地工具通常响应较快                       |
| 内网 HTTP MCP Server | `10s` - `30s`  | 适合订单、库存、配置查询等常规业务         |
| 外部 API 查询        | `30s` - `60s`  | 受第三方接口影响，需要更长等待时间         |
| 大文件或大文档读取   | `30s` - `60s`  | 建议分页或分片读取，不建议一次读取过大内容 |
| 变更类 Tool          | `10s` - `30s`  | 同时需要幂等、确认和审计                   |
| 大模型联调接口       | `30s` - `120s` | 模型响应受上下文长度和工具调用次数影响     |

连接参数设计建议如下：

| 参数                   | 建议                                                         |
| ---------------------- | ------------------------------------------------------------ |
| `type`                 | Spring MVC 项目使用 `SYNC`，WebFlux 项目使用 `ASYNC`         |
| `endpoint`             | Streamable HTTP 默认使用 `/mcp`，自定义时 Client 和 Server 必须一致 |
| `request-timeout`      | 按业务工具耗时设置，避免无限等待                             |
| `toolcallback.enabled` | 需要大模型自动调用 MCP Tool 时开启                           |
| Server 协议            | 新项目优先使用 `STREAMABLE`，旧项目兼容时再考虑 `SSE`        |
| Server 能力开关        | 不需要暴露的能力应关闭，减少攻击面和模型误用概率             |

Spring AI 文档说明，Streamable HTTP 支持 HTTP POST / GET，并可结合 SSE 处理多条服务端消息；该协议替代 SSE 作为推荐的 HTTP 传输方式之一。([Home](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-server-boot-starter-docs.html))

## 功能验证

本部分说明 MCP Server、MCP Client 和大模型联调的验证方式。建议按“先 Server、再 Client、最后模型”的顺序验证，避免模型调用失败时无法判断问题到底来自模型配置、MCP 连接、工具注册还是业务方法。

### MCP Server 启动验证

MCP Server 启动验证的目标是确认服务已正常启动，并且 Tools、Resources、Prompts 已被 Spring AI MCP Server 自动扫描和注册。Spring AI MCP Server Boot Starter 支持注解式开发和自动 Bean 扫描注册，因此启动日志中通常可以观察到 MCP Server 初始化和能力注册相关信息。([Home](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-server-boot-starter-docs.html))

启动 MCP Server。

```bash
mvn spring-boot:run
```

如果使用多环境配置，可以指定 profile。

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=server
```

验证重点如下：

| 验证项        | 期望结果                                               |
| ------------- | ------------------------------------------------------ |
| 应用端口      | `8080` 正常监听                                        |
| 启动日志      | 无 Bean 创建失败、端口冲突、配置绑定失败               |
| MCP 协议      | `spring.ai.mcp.server.protocol=STREAMABLE` 生效        |
| Tool 注册     | `@McpTool` 方法所在 Bean 已被 Spring 容器扫描          |
| Resource 注册 | `@McpResource` URI 模板无冲突                          |
| Prompt 注册   | `@McpPrompt` 名称无冲突                                |
| 业务依赖      | Tool 调用依赖的 Service、Mapper、Repository 可正常注入 |

常见启动失败原因如下：

| 问题        | 原因                            | 处理方式                                                   |
| ----------- | ------------------------------- | ---------------------------------------------------------- |
| 端口被占用  | `8080` 已被其他进程使用         | 修改 `server.port` 或关闭占用进程                          |
| Tool 未注册 | 类未被 Spring 扫描              | 检查包路径、`@Component`、启动类扫描范围                   |
| 注解不生效  | MCP Server Starter 未引入       | 检查是否引入 `spring-ai-starter-mcp-server-webmvc`         |
| 协议不匹配  | Server 配置和 Client 配置不一致 | Server 使用 `STREAMABLE` 时，Client 使用 `streamable-http` |
| Bean 冲突   | 同名 Tool、Prompt 或重复 Bean   | 修改名称并保持唯一                                         |

如果项目引入了 Actuator，可以通过健康检查确认 Spring Boot 应用状态。

```bash
curl -i http://localhost:8080/actuator/health
```

该命令只验证 Spring Boot 应用健康状态，不等价于完整 MCP 协议验证。完整 MCP 验证仍然建议通过 MCP Client 或 MCP Inspector 发起能力发现和工具调用。

### MCP Client 调用验证

MCP Client 调用验证的目标是确认 Client 能够连接 MCP Server，并可以完成 Tool 调用、Resource 读取和 Prompt 获取。Spring AI MCP Client Boot Starter 支持多个命名连接，并可自动初始化 Client；如果启用了 `toolcallback.enabled`，还可以把 MCP Tools 接入 Spring AI Tool Callback 体系。([Home](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-client-boot-starter-docs.html))

启动 MCP Client。

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=client
```

验证配置中的 Server 地址是否正确。

```yaml
spring:
  ai:
    mcp:
      client:
        streamable-http:
          connections:
            order-server:
              url: http://localhost:8080
              endpoint: /mcp
```

如果前面已经封装了业务接口，可以直接通过 HTTP 接口验证 Client 调用链路。

```bash
curl -X POST 'http://localhost:8081/api/ai/order/analyze' \
  -H 'Content-Type: application/json' \
  -d '{
    "orderNo": "ORDER_10001",
    "scene": "refund",
    "question": "这个订单是否支持退款？请说明依据。"
  }'
```

验证重点如下：

| 验证项        | 期望结果                                            |
| ------------- | --------------------------------------------------- |
| Client 初始化 | 应用启动时没有 MCP Client 初始化异常                |
| Server 连接   | `url` 和 `endpoint` 正确                            |
| Tool 调用     | 能够调用 `query_order` 等远程工具                   |
| Resource 读取 | 能够读取 `order-rule://refund` 等资源               |
| Prompt 获取   | 能够获取 `order_analysis` 等 Prompt                 |
| 超时配置      | 慢工具能按预期超时，不会无限等待                    |
| 日志输出      | 能看到工具名称、资源 URI、Prompt 名称和调用结果摘要 |

如果需要绕过模型直接验证 Tool，可以提供一个内部测试接口，例如 `/api/mcp/tool/query-order`，由后端直接调用 `McpSyncClient.callTool()`。这种方式可以排除模型决策带来的不确定性，快速确认 MCP Server 和 MCP Client 是否连通。

### 大模型联调验证

大模型联调验证的目标是确认 `ChatClient` 能够携带 MCP Tools 完成推理，并在需要时自动调用远程工具。Spring AI MCP 入门示例中，`ChatClient` 可以通过 `.toolCallbacks(mcpTools)` 注入 MCP Tools，然后调用 `.call().content()` 获取模型响应。([Home](https://docs.spring.io/spring-ai/reference/guides/getting-started-mcp.html))

联调前需要确认以下条件：

| 条件         | 说明                                                      |
| ------------ | --------------------------------------------------------- |
| 模型 API Key | 例如 `OPENAI_API_KEY` 已正确配置                          |
| 模型 Starter | 已引入对应模型依赖，例如 `spring-ai-starter-model-openai` |
| MCP Client   | 已成功连接 MCP Server                                     |
| ToolCallback | `spring.ai.mcp.client.toolcallback.enabled=true`          |
| 工具描述     | Tool 的 `description` 足够清晰，便于模型判断何时调用      |
| 工具返回值   | Tool 返回结构化结果，便于模型继续生成答案                 |

建议使用明确包含工具触发条件的问题测试。

```bash
curl -X POST 'http://localhost:8081/api/ai/order/analyze' \
  -H 'Content-Type: application/json' \
  -d '{
    "orderNo": "ORDER_10001",
    "scene": "refund",
    "question": "请查询这个订单当前状态，并判断是否满足退款规则。"
  }'
```

预期链路如下：

```text
用户问题
  ↓
Controller 接收请求
  ↓
Service 读取订单退款规则 Resource
  ↓
ChatClient 注入 MCP Tools
  ↓
模型判断需要调用 query_order
  ↓
MCP Client 调用 MCP Server Tool
  ↓
Tool 返回订单信息
  ↓
模型结合规则和订单信息生成回答
```

联调时建议在日志中观察以下内容：

| 日志点            | 说明                             |
| ----------------- | -------------------------------- |
| 请求入口日志      | 确认接口收到用户问题             |
| Resource 读取日志 | 确认读取了正确的规则上下文       |
| Tool 调用日志     | 确认模型触发了正确工具           |
| Tool 返回日志     | 确认工具返回结构化业务结果       |
| 模型响应日志      | 确认最终回答已生成               |
| 异常日志          | 记录模型错误、MCP 错误或业务异常 |

如果模型没有调用工具，优先检查 Tool 描述是否清晰、用户问题是否具备工具触发条件、`ToolCallbackProvider` 是否正确注入，以及 `toolcallback.enabled` 是否开启。Spring AI MCP Client 文档明确该开关用于启用 MCP Tool 与 Spring AI 工具执行框架的集成。([Home](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-client-boot-starter-docs.html))

### 常见问题排查

MCP 问题排查建议按“配置、连接、注册、调用、模型”五层逐步定位。不要一开始就假设是大模型问题；很多联调失败实际来自 Server 未启动、端点配置错误、Tool 未注册、参数结构不匹配或超时时间过短。

常见问题如下。

| 问题                   | 可能原因                                             | 处理方式                                                     |
| ---------------------- | ---------------------------------------------------- | ------------------------------------------------------------ |
| MCP Client 启动失败    | Server 地址错误、端点错误、Server 未启动             | 检查 `url`、`endpoint`、Server 端口和启动日志                |
| Tool 没有被模型调用    | Tool 描述不清晰、ToolCallback 未启用、问题不需要工具 | 检查 `toolcallback.enabled`，优化 Tool 名称和描述            |
| Tool 调用参数为空      | 参数名与 Tool 入参不一致                             | 检查 `@McpToolParam`、方法参数名和输入 Schema                |
| Resource 读取失败      | URI 拼接错误、Resource 模板不匹配                    | 检查 Resource URI，例如 `order-rule://refund`                |
| Prompt 获取失败        | Prompt 名称错误、参数缺失                            | 检查 `@McpPrompt` 名称和 `@McpArg` 必填参数                  |
| 请求超时               | Tool 执行慢、网络慢、模型响应慢                      | 调整 `request-timeout`，优化业务查询                         |
| 多 Server 工具混淆     | 多个 Server 暴露同名 Tool                            | 使用工具名前缀或更明确的 Tool 命名                           |
| Windows STDIO 无法启动 | `npx`、`npm` 等命令实际是 `.cmd` 批处理              | 使用 `cmd.exe /c` 包装命令                                   |
| WebMVC / WebFlux 冲突  | 依赖选择和项目 Web 栈不一致                          | Spring MVC 项目使用 WebMVC Starter，响应式项目使用 WebFlux Starter |
| 生产环境偶发失败       | 网络抖动、Server 重启、模型限流                      | 增加超时、重试、熔断、降级和审计日志                         |

排查顺序建议如下：

```text
1. 检查依赖是否正确
   ↓
2. 检查 application.yml 配置是否绑定成功
   ↓
3. 启动 MCP Server，确认端口和协议
   ↓
4. 启动 MCP Client，确认连接和初始化
   ↓
5. 直接调用 MCP Tool / Resource / Prompt
   ↓
6. 再接入 ChatClient 验证模型自动调用
   ↓
7. 最后处理多 Server、超时、权限和生产稳定性问题
```

生产环境还需要补充以下治理能力：

| 治理项     | 说明                                                     |
| ---------- | -------------------------------------------------------- |
| 权限控制   | 变更类 Tool 必须校验用户身份和操作权限                   |
| 审计日志   | 记录工具调用人、参数、结果、耗时和异常                   |
| 参数白名单 | 避免模型传入危险参数或越权参数                           |
| 结果脱敏   | Tool 返回前脱敏手机号、身份证号、Token 等敏感信息        |
| 限流熔断   | 防止模型频繁调用 Tool 压垮业务服务                       |
| 降级策略   | MCP Server 不可用时返回可理解的业务提示                  |
| 版本管理   | Server Tool、Resource、Prompt 变更时记录版本和兼容性说明 |

总体上，MCP 配置和验证应保持一个原则：先保证协议连接稳定，再验证工具、资源和提示词，最后接入大模型自动决策。这样可以把问题拆成可定位的多个小环节，避免联调时所有异常都堆叠在模型调用层。

## 示例工程

本部分给出一个最小可运行的 Spring AI 1.x MCP 示例工程设计，用于验证 MCP Server 暴露能力、MCP Client 连接调用，以及 `ChatClient` 携带 MCP Tools 完成大模型联调。示例采用双应用结构：`mcp-server` 负责暴露订单 Tool、规则 Resource 和分析 Prompt；`mcp-client` 负责连接 Server，并提供 HTTP 接口给外部调用。Spring AI MCP Boot Starters 会自动包含 MCP 注解模块，并且注解扫描默认开启，因此使用 `@McpTool`、`@McpResource`、`@McpPrompt` 的 Bean 可以被自动注册为 MCP 能力。([Home](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-annotations-overview.html))

### 工程目录结构

示例工程采用多模块结构，便于模拟生产环境中 MCP Server 和 MCP Client 独立部署的方式。Server 和 Client 分离后，可以清晰区分“能力提供方”和“能力消费方”，也方便后续将订单、库存、物流等能力拆成多个 MCP Server。

```text
spring-ai-mcp-example
├── pom.xml
├── spring-ai-mcp-common
│   ├── pom.xml
│   └── src/main/java/io/github/atengk/mcp/common
│       ├── dto
│       │   └── OrderIssueAnalyzeDTO.java
│       └── vo
│           ├── ApiResult.java
│           ├── McpChatResponseVO.java
│           └── OrderInfoVO.java
├── spring-ai-mcp-server
│   ├── pom.xml
│   └── src/main
│       ├── java/io/github/atengk/mcp/server
│       │   ├── SpringAiMcpServerApplication.java
│       │   ├── service
│       │   │   ├── OrderService.java
│       │   │   └── impl
│       │   │       └── OrderServiceImpl.java
│       │   ├── tool
│       │   │   └── OrderTool.java
│       │   ├── resource
│       │   │   └── OrderRuleResource.java
│       │   └── prompt
│       │       └── OrderPrompt.java
│       └── resources
│           └── application.yml
└── spring-ai-mcp-client
    ├── pom.xml
    └── src/main
        ├── java/io/github/atengk/mcp/client
        │   ├── SpringAiMcpClientApplication.java
        │   ├── controller
        │   │   └── OrderAiController.java
        │   ├── service
        │   │   ├── OrderAiService.java
        │   │   └── impl
        │   │       └── OrderAiServiceImpl.java
        │   └── handler
        │       └── GlobalExceptionHandler.java
        └── resources
            └── application.yml
```

模块职责如下：

| 模块                   | 职责                                                   |
| ---------------------- | ------------------------------------------------------ |
| `spring-ai-mcp-common` | 存放 DTO、VO、统一响应结构等公共对象                   |
| `spring-ai-mcp-server` | 暴露 MCP Tool、Resource、Prompt                        |
| `spring-ai-mcp-client` | 连接 MCP Server，接入 `ChatClient`，提供业务 HTTP 接口 |
| `OrderTool`            | 暴露订单查询工具                                       |
| `OrderRuleResource`    | 暴露订单规则资源                                       |
| `OrderPrompt`          | 暴露订单分析提示词                                     |
| `OrderAiServiceImpl`   | 编排 Resource、ToolCallback 和 ChatClient              |

### 核心代码说明

本示例的核心链路是：用户调用 Client 的订单分析接口，Client 读取 Server 暴露的订单规则 Resource，然后通过 `ChatClient` 注入 MCP Tools，让模型在需要时自动调用 `query_order` 工具，最后返回分析结果。Spring AI MCP Server 注解中，`@McpTool` 用于定义 Tool，`@McpResource` 用于通过 URI 模板暴露 Resource，`@McpPrompt` 用于生成 Prompt 消息。([Home](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-annotations-server.html))

父工程依赖管理如下。

文件位置：`pom.xml`

```xml
<project>
    <modelVersion>4.0.0</modelVersion>

    <groupId>io.github.atengk</groupId>
    <artifactId>spring-ai-mcp-example</artifactId>
    <version>1.0.0</version>
    <packaging>pom</packaging>

    <modules>
        <module>spring-ai-mcp-common</module>
        <module>spring-ai-mcp-server</module>
        <module>spring-ai-mcp-client</module>
    </modules>

    <properties>
        <!-- Java 编译版本 -->
        <java.version>17</java.version>

        <!-- Spring AI 1.x 示例版本，可按项目基线调整为 1.0.7 或 1.1.x 稳定版本 -->
        <spring-ai.version>1.1.6</spring-ai.version>

        <!-- Hutool 工具类版本 -->
        <hutool.version>5.8.36</hutool.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <!-- Spring AI BOM，统一管理 Spring AI 相关依赖版本 -->
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

MCP Server 模块需要引入 WebMVC Server Starter。Spring AI 文档中说明，WebMVC Streamable HTTP Server 使用 `spring-ai-starter-mcp-server-webmvc`，并通过 `spring.ai.mcp.server.protocol=STREAMABLE` 启用 Streamable HTTP。([Home](https://docs.spring.io/spring-ai/reference/1.1/api/mcp/mcp-streamable-http-server-boot-starter-docs.html?utm_source=chatgpt.com))

文件位置：`spring-ai-mcp-server/pom.xml`

```xml
<dependencies>
    <dependency>
        <!-- Web 基础能力，用于启动 HTTP MCP Server -->
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <dependency>
        <!-- Spring AI MCP Server WebMVC Starter -->
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-starter-mcp-server-webmvc</artifactId>
    </dependency>

    <dependency>
        <!-- Hutool 工具类 -->
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>${hutool.version}</version>
    </dependency>

    <dependency>
        <!-- Lombok 简化样板代码 -->
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

MCP Server 配置如下。

文件位置：`spring-ai-mcp-server/src/main/resources/application.yml`

```yaml
server:
  # MCP Server 端口
  port: 8080

spring:
  application:
    # MCP Server 应用名称
    name: spring-ai-mcp-server

  ai:
    mcp:
      server:
        # 启用 MCP Server
        enabled: true

        # MCP Server 名称
        name: order-mcp-server

        # MCP Server 版本
        version: 1.0.0

        # Spring MVC 阻塞式服务使用 SYNC
        type: SYNC

        # 使用 Streamable HTTP
        protocol: STREAMABLE

        annotation-scanner:
          # MCP 注解扫描默认开启，这里显式声明便于维护
          enabled: true
```

MCP Server 启动类如下。

文件位置：`spring-ai-mcp-server/src/main/java/io/github/atengk/mcp/server/SpringAiMcpServerApplication.java`

```java
package io.github.atengk.mcp.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring AI MCP Server 启动类。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@SpringBootApplication(scanBasePackages = "io.github.atengk.mcp")
public class SpringAiMcpServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringAiMcpServerApplication.class, args);
    }
}
```

订单查询 Tool 如下。该工具只做查询，不修改业务数据，适合作为模型自动调用的只读工具。

文件位置：`spring-ai-mcp-server/src/main/java/io/github/atengk/mcp/server/tool/OrderTool.java`

```java
package io.github.atengk.mcp.server.tool;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.mcp.common.vo.OrderInfoVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 订单 MCP 工具。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Component
public class OrderTool {

    @McpTool(
            name = "query_order",
            description = "根据订单号查询订单基础信息，只读操作，不会修改订单数据"
    )
    public OrderInfoVO queryOrder(
            @McpToolParam(description = "订单号，例如 ORDER_10001", required = true)
            String orderNo) {
        if (StrUtil.isBlank(orderNo)) {
            log.warn("订单查询失败：订单号为空");
            throw new IllegalArgumentException("订单号不能为空");
        }

        log.info("开始查询订单信息，orderNo={}", orderNo);

        return OrderInfoVO.builder()
                .orderNo(orderNo)
                .status("PAID")
                .amount("199.00")
                .paidTime(LocalDateTime.now().minusHours(2))
                .remark("示例订单：已支付，未发货")
                .build();
    }
}
```

订单规则 Resource 如下。Resource 用于提供只读业务规则，适合在模型分析前作为上下文注入。

文件位置：`spring-ai-mcp-server/src/main/java/io/github/atengk/mcp/server/resource/OrderRuleResource.java`

```java
package io.github.atengk.mcp.server.resource;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.mcp.annotation.McpResource;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 订单规则 MCP 资源。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Slf4j
@Component
public class OrderRuleResource {

    private static final Map<String, String> RULE_MAP = MapUtil.<String, String>builder()
            .put("refund", "退款规则：已支付且未发货订单可以直接申请退款；已发货订单需要先退货审核。")
            .put("delivery", "发货规则：普通商品 48 小时内发货，预售商品以商品页承诺时间为准。")
            .put("after_sale", "售后规则：签收后 7 天内支持无理由退货，特殊商品除外。")
            .build();

    @McpResource(
            uri = "order-rule://{scene}",
            name = "订单业务规则",
            description = "根据场景读取订单业务规则，支持 refund、delivery、after_sale"
    )
    public String readRule(String scene) {
        String ruleScene = StrUtil.blankToDefault(scene, "refund");
        String rule = RULE_MAP.get(ruleScene);

        if (StrUtil.isBlank(rule)) {
            log.warn("读取订单规则失败：未知场景，scene={}", ruleScene);
            return "未找到对应订单规则。";
        }

        log.info("读取订单规则成功，scene={}", ruleScene);
        return rule;
    }
}
```

订单分析 Prompt 如下。Prompt 用于沉淀标准分析模板，不直接执行业务动作。

文件位置：`spring-ai-mcp-server/src/main/java/io/github/atengk/mcp/server/prompt/OrderPrompt.java`

```java
package io.github.atengk.mcp.server.prompt;

import cn.hutool.core.util.StrUtil;
import io.modelcontextprotocol.spec.McpSchema.GetPromptResult;
import io.modelcontextprotocol.spec.McpSchema.PromptMessage;
import io.modelcontextprotocol.spec.McpSchema.Role;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import org.springframework.ai.mcp.annotation.McpArg;
import org.springframework.ai.mcp.annotation.McpPrompt;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 订单 MCP 提示词。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@Component
public class OrderPrompt {

    @McpPrompt(
            name = "order_analysis",
            description = "生成订单问题分析提示词，适用于退款、发货和售后场景"
    )
    public GetPromptResult orderAnalysis(
            @McpArg(name = "orderNo", description = "订单号", required = true)
            String orderNo,
            @McpArg(name = "scene", description = "业务场景", required = false)
            String scene) {
        String ruleScene = StrUtil.blankToDefault(scene, "refund");
        String message = StrUtil.format("""
                请分析订单问题。

                订单号：{}
                业务场景：{}

                要求：
                1. 如需订单详情，请调用 query_order 工具。
                2. 如需业务规则，请读取 order-rule://{} 资源。
                3. 回答时说明判断依据、当前状态、处理建议。
                4. 信息不足时必须说明缺失信息，不要编造业务数据。
                """, orderNo, ruleScene, ruleScene);

        return new GetPromptResult(
                "订单问题分析模板",
                List.of(new PromptMessage(Role.USER, new TextContent(message)))
        );
    }
}
```

MCP Client 模块需要引入 Client Starter 和模型 Starter。MCP Client Boot Starter 支持多个命名连接、自动初始化、STDIO / SSE / Streamable HTTP 等传输方式，并能与 Spring AI 工具执行框架集成。([Home](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-client-boot-starter-docs.html?utm_source=chatgpt.com))

文件位置：`spring-ai-mcp-client/pom.xml`

```xml
<dependencies>
    <dependency>
        <!-- Web 接口能力 -->
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <dependency>
        <!-- 参数校验能力 -->
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <dependency>
        <!-- Spring AI MCP Client Starter -->
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-starter-mcp-client</artifactId>
    </dependency>

    <dependency>
        <!-- OpenAI 模型接入示例，可替换为企业实际模型 Starter -->
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-starter-model-openai</artifactId>
    </dependency>

    <dependency>
        <!-- Hutool 工具类 -->
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>${hutool.version}</version>
    </dependency>

    <dependency>
        <!-- Lombok 简化样板代码 -->
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

MCP Client 配置如下。`streamable-http.connections.order-server.url` 指向 MCP Server 地址，`endpoint` 默认通常为 `/mcp`，需要和 Server 端协议一致。

文件位置：`spring-ai-mcp-client/src/main/resources/application.yml`

```yaml
server:
  # MCP Client 业务接口端口
  port: 8081

spring:
  application:
    # MCP Client 应用名称
    name: spring-ai-mcp-client

  ai:
    openai:
      # 模型密钥从环境变量读取
      api-key: ${OPENAI_API_KEY}
      chat:
        options:
          # 示例模型，可替换为企业实际模型
          model: gpt-4o-mini
          temperature: 0.2

    mcp:
      client:
        # 启用 MCP Client
        enabled: true

        # 启动时初始化 MCP Client
        initialized: true

        # MCP Client 名称
        name: order-ai-client

        # 请求超时时间
        request-timeout: 30s

        # Spring MVC 应用使用 SYNC
        type: SYNC

        toolcallback:
          # 启用 MCP Tool 与 Spring AI ToolCallback 集成
          enabled: true

        streamable-http:
          connections:
            order-server:
              # MCP Server 地址
              url: http://localhost:8080

              # Streamable HTTP 端点
              endpoint: /mcp
```

Client 启动类如下。

文件位置：`spring-ai-mcp-client/src/main/java/io/github/atengk/mcp/client/SpringAiMcpClientApplication.java`

```java
package io.github.atengk.mcp.client;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring AI MCP Client 启动类。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@SpringBootApplication(scanBasePackages = "io.github.atengk.mcp")
public class SpringAiMcpClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringAiMcpClientApplication.class, args);
    }
}
```

Client 侧业务服务如下。它先读取规则 Resource，再把规则作为上下文交给 `ChatClient`，同时通过 `ToolCallbackProvider` 注入 MCP Tools。`ChatClient` 与 Tool Callback 结合后，模型可以根据问题和工具描述决定是否调用远程 MCP Tool。([Home](https://docs.spring.io/spring-ai/reference/guides/getting-started-mcp.html?utm_source=chatgpt.com))

文件位置：`spring-ai-mcp-client/src/main/java/io/github/atengk/mcp/client/service/impl/OrderAiServiceImpl.java`

```java
package io.github.atengk.mcp.client.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import io.github.atengk.mcp.client.service.OrderAiService;
import io.github.atengk.mcp.common.dto.OrderIssueAnalyzeDTO;
import io.github.atengk.mcp.common.vo.McpChatResponseVO;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Service;

import java.util.List;

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

    private final ChatClient.Builder chatClientBuilder;
    private final ToolCallbackProvider mcpTools;
    private final List<McpSyncClient> mcpSyncClients;

    /**
     * 分析订单问题。
     *
     * @param request 请求参数
     * @return 分析结果
     */
    @Override
    public McpChatResponseVO analyze(OrderIssueAnalyzeDTO request) {
        if (request == null || StrUtil.isBlank(request.getQuestion())) {
            log.warn("订单问题分析失败：问题为空");
            throw new IllegalArgumentException("用户问题不能为空");
        }

        String scene = StrUtil.blankToDefault(request.getScene(), "refund");
        String ruleContext = readRuleContext(scene);

        log.info("开始执行订单 AI 分析，orderNo={}，scene={}", request.getOrderNo(), scene);

        String answer = chatClientBuilder.build()
                .prompt()
                .system(system -> system.text("""
                        你是订单业务助手。
                        请基于以下订单规则和可用 MCP 工具回答用户问题。

                        订单规则：
                        {ruleContext}

                        要求：
                        1. 如需订单状态，请调用 query_order 工具。
                        2. 回答必须包含判断依据和处理建议。
                        3. 工具或规则没有提供的信息，不要编造。
                        """).param("ruleContext", ruleContext))
                .user(user -> user.text("""
                        订单号：{orderNo}
                        用户问题：{question}
                        """)
                        .param("orderNo", request.getOrderNo())
                        .param("question", request.getQuestion()))
                .toolCallbacks(mcpTools)
                .call()
                .content();

        log.info("订单 AI 分析完成，orderNo={}", request.getOrderNo());

        return McpChatResponseVO.builder()
                .question(request.getQuestion())
                .context(ruleContext)
                .answer(answer)
                .build();
    }

    /**
     * 读取规则上下文。
     *
     * @param scene 业务场景
     * @return 规则上下文
     */
    private String readRuleContext(String scene) {
        if (CollUtil.isEmpty(mcpSyncClients)) {
            log.warn("读取订单规则失败：未找到 MCP Client，使用空上下文");
            return "";
        }

        String uri = "order-rule://" + scene;
        McpSchema.ReadResourceResult result = mcpSyncClients.get(0).readResource(
                new McpSchema.ReadResourceRequest(uri)
        );

        log.info("读取订单规则成功，uri={}", uri);
        return JSONUtil.toJsonStr(result);
    }
}
```

业务接口如下。

文件位置：`spring-ai-mcp-client/src/main/java/io/github/atengk/mcp/client/controller/OrderAiController.java`

```java
package io.github.atengk.mcp.client.controller;

import io.github.atengk.mcp.client.service.OrderAiService;
import io.github.atengk.mcp.common.dto.OrderIssueAnalyzeDTO;
import io.github.atengk.mcp.common.vo.ApiResult;
import io.github.atengk.mcp.common.vo.McpChatResponseVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 订单 AI 接口。
 *
 * @author Ateng
 * @since 2026-05-11
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ai/order")
public class OrderAiController {

    private final OrderAiService orderAiService;

    /**
     * 分析订单问题。
     *
     * @param request 请求参数
     * @return 分析结果
     */
    @PostMapping("/analyze")
    public ApiResult<McpChatResponseVO> analyze(@Valid @RequestBody OrderIssueAnalyzeDTO request) {
        return ApiResult.success(orderAiService.analyze(request));
    }
}
```

### 运行步骤

运行前需要准备 JDK 17+、Maven 3.9+，并确保模型 API Key 已配置。Server 和 Client 需要分别启动，启动顺序建议先 Server 后 Client，否则 Client 初始化时可能连接不到 MCP Server。

第一步，编译整个工程。

```bash
mvn clean package -DskipTests
```

该命令在父工程目录执行，会编译 `common`、`server`、`client` 三个模块。`-DskipTests` 用于跳过测试，便于先完成本地启动验证。

第二步，启动 MCP Server。

```bash
mvn -pl spring-ai-mcp-server spring-boot:run
```

启动成功后，Server 监听 `8080` 端口，并通过 Streamable HTTP 暴露 MCP 服务。Spring AI MCP Server 支持 WebMVC Streamable HTTP Server，并通过 `spring-ai-starter-mcp-server-webmvc` 与 `spring.ai.mcp.server.protocol=STREAMABLE` 配置启用。([Home](https://docs.spring.io/spring-ai/reference/1.1/api/mcp/mcp-streamable-http-server-boot-starter-docs.html?utm_source=chatgpt.com))

第三步，配置模型 API Key。

```bash
export OPENAI_API_KEY="你的模型APIKey"
```

如果使用 Windows PowerShell，可以使用以下命令。

```powershell
$env:OPENAI_API_KEY="你的模型APIKey"
```

第四步，启动 MCP Client。

```bash
mvn -pl spring-ai-mcp-client spring-boot:run
```

Client 启动后会连接 `http://localhost:8080/mcp`。MCP Client 的 Streamable HTTP 连接配置使用 `spring.ai.mcp.client.streamable-http.connections`，其中 `connections.[name].url` 是 Server 基础地址，`connections.[name].endpoint` 是端点后缀，默认值为 `/mcp`。([Home](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-client-boot-starter-docs.html?utm_source=chatgpt.com))

第五步，调用业务接口。

```bash
curl -X POST 'http://localhost:8081/api/ai/order/analyze' \
  -H 'Content-Type: application/json' \
  -d '{
    "orderNo": "ORDER_10001",
    "scene": "refund",
    "question": "请查询这个订单当前状态，并判断是否支持退款。"
  }'
```

预期响应结构如下。

```json
{
  "success": true,
  "code": "0",
  "message": "操作成功",
  "data": {
    "question": "请查询这个订单当前状态，并判断是否支持退款。",
    "context": "...order-rule://refund...",
    "answer": "根据订单规则和查询到的订单状态..."
  }
}
```

### 测试用例

测试建议分为三层：Server 能力测试、Client 连接测试、大模型联调测试。这样可以快速判断问题是出在 MCP Server、MCP Client，还是模型工具调用链路。

Server 启动测试用例：

| 测试项        | 操作                        | 预期结果                               |
| ------------- | --------------------------- | -------------------------------------- |
| Server 启动   | 启动 `spring-ai-mcp-server` | 端口 `8080` 正常监听                   |
| Tool 注册     | 检查启动日志                | `OrderTool` 被 Spring 容器扫描         |
| Resource 注册 | 检查启动日志                | `OrderRuleResource` 被 Spring 容器扫描 |
| Prompt 注册   | 检查启动日志                | `OrderPrompt` 被 Spring 容器扫描       |
| 协议配置      | 检查配置                    | `protocol=STREAMABLE` 生效             |

Client 连接测试用例：

| 测试项        | 操作                        | 预期结果                           |
| ------------- | --------------------------- | ---------------------------------- |
| Client 启动   | 启动 `spring-ai-mcp-client` | 无 MCP 连接异常                    |
| Server 地址   | 检查 `url` 和 `endpoint`    | 指向 `http://localhost:8080/mcp`   |
| ToolCallback  | 检查配置                    | `toolcallback.enabled=true`        |
| Resource 读取 | 调用分析接口                | 日志出现读取 `order-rule://refund` |
| Tool 调用     | 提问要求查询订单状态        | 日志出现调用 `query_order`         |

接口测试用例一：退款判断。

```bash
curl -X POST 'http://localhost:8081/api/ai/order/analyze' \
  -H 'Content-Type: application/json' \
  -d '{
    "orderNo": "ORDER_10001",
    "scene": "refund",
    "question": "这个订单现在能退款吗？请说明判断依据。"
  }'
```

预期结果：

| 检查点      | 说明                             |
| ----------- | -------------------------------- |
| HTTP 状态   | 返回 `200`                       |
| `success`   | 返回 `true`                      |
| `context`   | 包含退款规则上下文               |
| `answer`    | 说明订单状态、退款规则和处理建议 |
| Server 日志 | 出现 `query_order` 工具调用日志  |

接口测试用例二：发货规则咨询。

```bash
curl -X POST 'http://localhost:8081/api/ai/order/analyze' \
  -H 'Content-Type: application/json' \
  -d '{
    "orderNo": "ORDER_10002",
    "scene": "delivery",
    "question": "这个订单什么时候发货？如果还没发货应该怎么处理？"
  }'
```

预期结果：

| 检查点   | 说明                                                 |
| -------- | ---------------------------------------------------- |
| Resource | 读取 `order-rule://delivery`                         |
| Tool     | 根据问题判断是否调用 `query_order`                   |
| Answer   | 回答发货规则、当前状态和建议动作                     |
| 日志     | Client 有 Resource 读取日志，Server 有 Tool 查询日志 |

接口测试用例三：参数校验。

```bash
curl -X POST 'http://localhost:8081/api/ai/order/analyze' \
  -H 'Content-Type: application/json' \
  -d '{
    "orderNo": "",
    "scene": "refund",
    "question": ""
  }'
```

预期结果：

```json
{
  "success": false,
  "code": "400",
  "message": "订单号不能为空",
  "data": null
}
```

如果实际返回的是 Spring Boot 默认错误结构，说明还没有接入前面章节中的 `GlobalExceptionHandler`，需要补充统一异常处理器。

大模型联调测试重点如下：

| 检查项       | 说明                                                   |
| ------------ | ------------------------------------------------------ |
| 模型密钥     | `OPENAI_API_KEY` 已正确设置                            |
| 模型依赖     | Client 已引入模型 Starter                              |
| ToolCallback | `ToolCallbackProvider` 能正常注入                      |
| 工具触发     | 用户问题明确包含“查询订单状态”“判断能否退款”等触发条件 |
| 日志链路     | Controller、Resource 读取、Tool 调用、模型回答均有日志 |

排查顺序建议如下：

```text
1. 先启动 MCP Server，确认无异常
2. 再启动 MCP Client，确认连接成功
3. 先测试 Resource 读取，再测试 Tool 调用
4. 最后测试 ChatClient 自动调用工具
5. 如果模型不调用工具，优先优化 Tool 名称、description 和用户问题
```

该示例工程的最小闭环是：`OrderRuleResource` 提供规则，`OrderTool` 提供订单查询，`OrderAiServiceImpl` 通过 `ChatClient` 注入 MCP Tools 并生成回答。后续扩展时，可以按相同模式增加库存 MCP Server、物流 MCP Server、售后 MCP Server，并在 Client 侧通过多 Server 配置统一接入。