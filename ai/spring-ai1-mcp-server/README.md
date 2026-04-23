# Spring AI MCP Server

## 版本信息

本文档基于以下版本组合进行整理与示例说明。

| 组件                 | 版本   |
| -------------------- | ------ |
| JDK                  | 21     |
| Maven                | 3.9.12 |
| Spring Boot          | 3.5.13 |
| Spring AI MCP Server | 1.1.4  |

------

## 简介

MCP（Model Context Protocol）可以让应用以统一协议向大模型提供能力。
在 Spring AI 中，MCP Server 主要可以暴露以下四类能力：

- Tool：提供可调用的业务能力，适合执行计算、查询、处理逻辑
- Resource：提供可读取的资源内容，适合暴露系统信息、配置、详情数据
- Prompt：提供可复用的提示模板，适合生成标准化 Prompt
- Completion：提供自动补全能力，适合参数联想、资源 URI 补全

如果把它们放在一起理解：

- Tool 更像“方法调用”
- Resource 更像“只读资源读取”
- Prompt 更像“提示模板工厂”
- Completion 更像“输入辅助能力”

------

## 基础配置

### 添加依赖

这里引入 Spring AI MCP Server 的 WebMVC Starter，并通过 BOM 统一 Spring AI 版本。

```xml
<properties>
    <java.version>21</java.version>
    <spring-boot.version>3.5.13</spring-boot.version>
    <spring-ai.version>1.1.4</spring-ai.version>
</properties>

<dependencies>
    <!-- Spring AI MCP Server 依赖 -->
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-starter-mcp-server-webmvc</artifactId>
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

### 编辑配置

下面这份配置适合当前这套示例工程，使用 `STREAMABLE` 协议、同步模式，并开启 Tool、Resource、Prompt、Completion 四类能力。

```yaml
---
# MCP Server 配置
spring:
  ai:
    mcp:
      server:
        # 是否启用 MCP Server
        enabled: true

        # MCP 传输协议：
        # STREAMABLE：推荐的 HTTP MCP 协议，支持 POST/GET 和可选 SSE 流式消息
        # STATELESS：无状态 HTTP 模式，适合云原生，但不支持 sampling、ping 等双向消息能力
        protocol: STREAMABLE

        # Server 类型：
        # SYNC 只注册同步方法
        # ASYNC 只注册响应式 / 异步方法（如 Mono / Flux）
        type: SYNC

        # MCP Server 的标识名称，建议使用你的服务名或项目名
        name: ateng-mcp-server

        # MCP Server 自身版本，建议写你的项目版本，而不是 Spring AI 版本
        version: 1.0.0

        # 提供给 MCP Client 的服务说明，建议写清楚核心能力边界
        instructions: 提供数学计算、天气查询、系统运行信息、问候语模板和自动补全能力。

        # 单次 MCP 请求处理超时时间
        # 一般 15s~60s 都可以，开发阶段建议先给宽一点
        request-timeout: 30s

        # MCP 能力开关
        # 当前项目已经实现了 Tool / Resource / Prompt / Completion，建议都显式打开
        capabilities:
          tool: true
          resource: true
          prompt: true
          completion: true

        # 资源、工具、提示词变更通知开关
        # 如果服务能力基本固定，保留 true 也没问题；如果想尽量简化，也可以省略，默认就是 true
        resource-change-notification: true
        tool-change-notification: true
        prompt-change-notification: true

        # 注解扫描配置
        annotation-scanner:
          # 自动扫描 @McpTool / @McpResource / @McpPrompt / @McpComplete
          enabled: true

        # Streamable-HTTP 传输层配置
        streamable-http:
          # MCP 对外暴露的 HTTP 端点
          # 默认就是 /mcp，这里显式写出，方便客户端对接
          mcp-endpoint: /mcp

          # 服务端保活间隔
          # 为空表示关闭；开启后会定期向客户端发送 ping 检查连接状态
          # 对于 Streamable-HTTP，这个保活目前只对 SSE 监听连接生效
          keep-alive-interval: 30s

          # 是否禁止 DELETE 操作
          # 开发调试阶段通常保留 false
          # 如果你希望接口行为更保守，可以改成 true
          disallow-delete: false
```

### 配置说明

当前推荐使用 `STREAMABLE` 协议。

当前示例统一使用 `SYNC`，因为前面完成的示例大部分都是普通同步方法。
如果后续需要接入 `Mono`、`Flux` 这类响应式方法，再切换为 `ASYNC`。

------

## 定义 MCP Tool

### 作用说明

`@McpTool` 用于暴露“可执行能力”。
它适合处理带业务语义的方法，例如：

- 数学计算
- 天气查询
- 文本处理
- 长任务执行
- 动态参数分发
- 基于上下文的业务处理

Tool 更接近“功能接口”，通常会接收入参并返回执行结果。

### 典型场景

根据前面已经完成的示例，`McpTool` 常见可以分为以下几类：

#### 1. 基础计算类 Tool

适合简单、确定性的业务方法，例如整数加法。
这类 Tool 一般：

- 参数固定
- 返回结构稳定
- 无副作用
- 适合标记为只读、幂等

#### 2. 查询类 Tool

适合根据参数返回结构化结果，例如城市气温查询。
这类 Tool 推荐返回结构化对象，而不是基础类型，便于后续扩展字段。

#### 3. 动态参数 Tool

适合参数结构不固定、按 `action` 或业务类型分流处理的场景。
这类 Tool 通常不会直接写死固定参数，而是统一接收请求对象后自行解析。

#### 4. 上下文感知 Tool

适合根据调用方身份、租户、角色等上下文做差异化处理。
这类 Tool 常用于多租户、权限控制、个性化输出等场景。

#### 5. 长任务 Tool

适合执行时间较长、需要返回进度信息的场景。
例如批量处理、报表生成、复杂分析任务等。

#### 6. 异步 Tool

适合响应式调用或远程异步处理场景。
如果要使用异步 Tool，需要结合 `ASYNC` 类型的 MCP Server 一起使用。

### 开发建议

#### 返回值尽量结构化

虽然 Tool 可以直接返回基础类型，但更推荐统一返回结构化对象。
这样做有几个好处：

- 便于统一接口风格
- 便于增加 `success`、`message`、`timestamp` 等字段
- 后续扩展兼容性更好

例如前面已经统一调整为：

- `MathTool` 返回 `MathResult`
- `WeatherTool` 返回 `WeatherResult`

#### 注解中的 title 说明

`@McpTool` 的 `title` 可以写在顶层，也可以写在 `annotations` 中。
如果需要更细粒度地描述 Tool 展示信息和行为提示，可以放到 `annotations` 里。

例如：

- `title`
- `readOnlyHint`
- `destructiveHint`
- `idempotentHint`

#### 参数说明要写清楚

建议每个 `@McpToolParam` 都补充：

- description
- required

这样客户端在展示参数时会更清晰。

### 示例一：基础加法 Tool

这里放基础计算类 Tool 示例代码。

```java
package io.github.atengk.mcp.tool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Service;

/**
 * MCP Tool：整数加法工具服务
 * <p>
 * 该组件用于提供两个整数相加的能力，
 * 并以结构化结果的形式返回计算信息，
 * 便于与其他 Tool 的返回规范保持一致。
 *
 * @author Ateng
 * @since 2026-04-23
 */
@Service
public class MathTool {

    /**
     * 日志组件
     */
    private static final Logger log = LoggerFactory.getLogger(MathTool.class);

    /**
     * 计算两个整数的和
     * <p>
     * 该方法只处理整数加法，不涉及外部调用，
     * 且不会对系统状态产生任何影响。
     *
     * @param a 第一个整数
     * @param b 第二个整数
     * @return 加法结果对象
     */
    @McpTool(
            name = "add",
            title = "整数加法",
            description = "计算两个整数的和，返回结构化结果，无副作用，不涉及外部系统调用",
            annotations = @McpTool.McpAnnotations(
                    title = "整数加法工具",
                    readOnlyHint = true,
                    destructiveHint = false,
                    idempotentHint = true
            )
    )
    public MathResult add(
            @McpToolParam(description = "第一个整数", required = true) int a,
            @McpToolParam(description = "第二个整数", required = true) int b) {

        // 执行安全加法，避免整数溢出
        int result = safeAdd(a, b);

        MathResult mathResult = new MathResult(
                a,
                b,
                result,
                "ADD",
                true,
                "计算成功"
        );

        log.debug("MCP工具[add]执行完成，参数a={}，b={}，结果result={}", a, b, mathResult);
        return mathResult;
    }

    /**
     * 安全执行整数加法
     * <p>
     * 如果相加结果超出 int 范围，
     * 则抛出业务友好的异常信息。
     *
     * @param a 第一个整数
     * @param b 第二个整数
     * @return 加法结果
     */
    private int safeAdd(int a, int b) {
        try {
            return Math.addExact(a, b);
        } catch (ArithmeticException ex) {
            log.warn("MCP工具[add]发生整数溢出，参数a={}，b={}", a, b);
            throw new IllegalArgumentException("整数相加发生溢出");
        }
    }

    /**
     * MCP Tool：加法结果对象
     * <p>
     * 用于封装整数加法后的结构化返回结果。
     *
     * @param a         第一个整数
     * @param b         第二个整数
     * @param result    计算结果
     * @param operation 运算类型
     * @param success   是否计算成功
     * @param message   结果说明
     * @author Ateng
     * @since 2026-04-23
     */
    public record MathResult(
            Integer a,
            Integer b,
            Integer result,
            String operation,
            Boolean success,
            String message
    ) {
    }
}
```

### 示例二：天气查询 Tool

这里放结构化查询类 Tool 示例代码。

```java
package io.github.atengk.mcp.tool;

import cn.hutool.core.util.StrUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Service;

/**
 * MCP Tool：城市气温查询工具服务
 * <p>
 * 该组件用于根据城市名称返回当前气温信息，
 * 并以结构化结果的形式返回查询结果，
 * 便于与其他 Tool 的返回规范保持一致。
 *
 * @author Ateng
 * @since 2026-04-23
 */
@Service
public class WeatherTool {

    /**
     * 日志组件
     */
    private static final Logger log = LoggerFactory.getLogger(WeatherTool.class);

    /**
     * 查询指定城市的当前气温
     * <p>
     * 该方法根据传入的城市名称，
     * 返回包含城市、温度、单位、摘要、执行状态和结果说明的结构化对象。
     *
     * @param city 城市名称
     * @return 天气结果对象
     */
    @McpTool(
            name = "getTemperature",
            title = "城市气温查询",
            description = "获取指定城市的当前气温，返回结构化天气信息",
            annotations = @McpTool.McpAnnotations(
                    title = "城市气温查询工具",
                    readOnlyHint = true,
                    destructiveHint = false,
                    idempotentHint = true
            )
    )
    public WeatherResult getTemperature(
            @McpToolParam(description = "城市名称，例如：北京、上海", required = true) String city) {

        // 校验城市名称，避免传入空值或空白字符串
        if (StrUtil.isBlank(city)) {
            log.warn("MCP工具[getTemperature]参数非法，city为空");
            throw new IllegalArgumentException("城市名称不能为空");
        }

        // 这里使用固定数据模拟天气查询结果，实际项目中可接入第三方天气服务
        WeatherResult result = new WeatherResult(
                city,
                22,
                "C",
                StrUtil.format("当前{}的气温为22°C", city),
                true,
                "查询成功"
        );

        log.debug("MCP工具[getTemperature]执行完成，参数city={}，结果result={}", city, result);
        return result;
    }

    /**
     * MCP Tool：天气结果对象
     * <p>
     * 用于封装城市气温查询后的结构化返回结果。
     *
     * @param city        城市名称
     * @param temperature 当前气温
     * @param unit        温度单位
     * @param summary     天气摘要说明
     * @param success     是否查询成功
     * @param message     结果说明
     * @author Ateng
     * @since 2026-04-23
     */
    public record WeatherResult(
            String city,
            Integer temperature,
            String unit,
            String summary,
            Boolean success,
            String message
    ) {
    }
}
```

### 示例三：动态参数 Tool

这里放动态参数分发 Tool 示例代码。

```java
package io.github.atengk.mcp.tool;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * MCP Tool：动态参数处理工具服务
 * <p>
 * 该组件用于演示不固定参数结构的 Tool 写法，
 * 适合根据 action 动态分发处理逻辑的场景。
 *
 * @author Ateng
 * @since 2026-04-23
 */
@Service
public class DynamicDispatchTool {

    /**
     * 日志组件
     */
    private static final Logger log = LoggerFactory.getLogger(DynamicDispatchTool.class);

    /**
     * 动态处理工具请求
     * <p>
     * 该方法直接接收完整的 Tool 请求对象，
     * 适合参数结构不固定或需要自行解析参数的场景。
     *
     * @param request MCP Tool 请求对象
     * @return Tool 调用结果
     */
    @McpTool(
            name = "dynamicDispatch",
            title = "动态分发处理",
            description = "根据 action 动态处理请求，适合不固定参数结构的场景"
    )
    public CallToolResult dynamicDispatch(CallToolRequest request) {
        Map<String, Object> args = request.arguments();
        String action = Convert.toStr(args.get("action"));
        String text = Convert.toStr(args.get("text"));

        log.debug("MCP工具[dynamicDispatch]执行，action={}，args={}", action, args);

        if (StrUtil.equalsIgnoreCase(action, "upper")) {
            return CallToolResult.builder()
                    .addTextContent(StrUtil.toUpperCase(StrUtil.blankToDefault(text, "")))
                    .build();
        }

        if (StrUtil.equalsIgnoreCase(action, "lower")) {
            return CallToolResult.builder()
                    .addTextContent(StrUtil.toLowerCase(StrUtil.blankToDefault(text, "")))
                    .build();
        }

        if (StrUtil.equalsIgnoreCase(action, "summary")) {
            String result = StrUtil.format("共接收到 {} 个参数，参数内容：{}", args != null ? args.size() : 0, args);
            return CallToolResult.builder()
                    .addTextContent(result)
                    .build();
        }

        return CallToolResult.builder()
                .addTextContent("不支持的 action，可选值：upper、lower、summary")
                .build();
    }
}

```

### 示例四：上下文感知 Tool

这里放基于上下文处理的 Tool 示例代码。

```java
package io.github.atengk.mcp.tool;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.mcp.annotation.McpMeta;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Service;

/**
 * MCP Tool：上下文感知工具服务
 * <p>
 * 该组件用于演示通过 McpMeta 读取请求元数据，
 * 并根据用户上下文返回不同结果。
 *
 * @author Ateng
 * @since 2026-04-23
 */
@Service
public class ContextAwareTool {

    /**
     * 日志组件
     */
    private static final Logger log = LoggerFactory.getLogger(ContextAwareTool.class);

    /**
     * 根据请求元数据执行上下文感知处理
     * <p>
     * 该方法会从 MCP 请求元数据中读取用户信息，
     * 并返回带上下文标识的结构化结果。
     *
     * @param keyword 查询关键字
     * @param meta    MCP 请求元数据
     * @return 上下文处理结果
     */
    @McpTool(
            name = "contextAwareSearch",
            title = "上下文感知查询",
            description = "根据请求元数据中的用户信息，返回带上下文的查询结果",
            annotations = @McpTool.McpAnnotations(
                    title = "上下文感知查询工具",
                    readOnlyHint = true,
                    destructiveHint = false,
                    idempotentHint = true
            )
    )
    public ContextSearchResult contextAwareSearch(
            @McpToolParam(description = "查询关键字", required = true) String keyword,
            McpMeta meta) {

        String userId = Convert.toStr(meta.get("userId"), "anonymous");
        String userRole = Convert.toStr(meta.get("userRole"), "guest");
        String tenantId = Convert.toStr(meta.get("tenantId"), "default");

        log.debug("MCP工具[contextAwareSearch]执行，keyword={}，userId={}，userRole={}，tenantId={}",
                keyword, userId, userRole, tenantId);

        String scope = StrUtil.equalsIgnoreCase(userRole, "admin") ? "全部数据范围" : "当前租户数据范围";
        String summary = StrUtil.format(
                "用户[{}]以角色[{}]在租户[{}]下查询关键字[{}]，当前生效范围：{}",
                userId, userRole, tenantId, keyword, scope
        );

        return new ContextSearchResult(
                keyword,
                userId,
                userRole,
                tenantId,
                scope,
                true,
                summary
        );
    }

    /**
     * MCP Tool：上下文查询结果对象
     *
     * @param keyword  查询关键字
     * @param userId   用户标识
     * @param userRole 用户角色
     * @param tenantId 租户标识
     * @param scope    生效范围
     * @param success  是否成功
     * @param message  结果说明
     * @author Ateng
     * @since 2026-04-23
     */
    public record ContextSearchResult(
            String keyword,
            String userId,
            String userRole,
            String tenantId,
            String scope,
            Boolean success,
            String message
    ) {
    }
}

```

### 示例五：长任务 Tool

这里放带进度反馈的长任务 Tool 示例代码。

```java
package io.github.atengk.mcp.tool;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springaicommunity.mcp.context.McpSyncRequestContext;
import org.springframework.stereotype.Service;

/**
 * MCP Tool：长任务处理工具服务
 * <p>
 * 该组件用于演示在 Tool 执行过程中发送日志、进度和心跳，
 * 适合处理耗时任务。
 *
 * @author Ateng
 * @since 2026-04-23
 */
@Service
public class LongRunningTool {

    /**
     * 日志组件
     */
    private static final Logger log = LoggerFactory.getLogger(LongRunningTool.class);

    /**
     * 执行长任务并回传进度
     * <p>
     * 该方法会模拟一个分阶段执行的任务，
     * 在执行过程中向客户端发送进度通知。
     *
     * @param context  MCP 同步请求上下文
     * @param taskName 任务名称
     * @param seconds  模拟执行时长
     * @return 长任务执行结果
     */
    @McpTool(
            name = "runLongTask",
            title = "长任务执行",
            description = "执行一个带进度反馈的长任务",
            annotations = @McpTool.McpAnnotations(
                    title = "长任务执行工具",
                    readOnlyHint = true,
                    destructiveHint = false,
                    idempotentHint = false
            )
    )
    public LongTaskResult runLongTask(
            McpSyncRequestContext context,
            @McpToolParam(description = "任务名称", required = true) String taskName,
            @McpToolParam(description = "模拟执行秒数，建议 1 到 10", required = true) int seconds) {

        if (seconds <= 0) {
            log.warn("MCP工具[runLongTask]参数非法，seconds={}", seconds);
            throw new IllegalArgumentException("seconds 必须大于 0");
        }

        log.debug("MCP工具[runLongTask]开始执行，taskName={}，seconds={}", taskName, seconds);
        context.info("任务开始执行：" + taskName);
        context.ping();

        Object progressToken = context.request().progressToken();
        if (ObjectUtil.isNotNull(progressToken)) {
            context.progress(0);
        }

        for (int i = 1; i <= seconds; i++) {
            ThreadUtil.sleep(1000);
            int progress = i * 100 / seconds;

            log.debug("MCP工具[runLongTask]执行中，taskName={}，progress={}%", taskName, progress);

            if (ObjectUtil.isNotNull(progressToken)) {
                context.progress(progress);
            }
        }

        context.info("任务执行完成：" + taskName);

        LongTaskResult result = new LongTaskResult(
                taskName,
                seconds,
                100,
                true,
                StrUtil.format("任务 [{}] 已执行完成，共耗时 {} 秒", taskName, seconds)
        );

        log.debug("MCP工具[runLongTask]执行完成，result={}", result);
        return result;
    }

    /**
     * MCP Tool：长任务结果对象
     *
     * @param taskName 任务名称
     * @param seconds  执行时长
     * @param progress 最终进度
     * @param success  是否成功
     * @param message  结果说明
     * @author Ateng
     * @since 2026-04-23
     */
    public record LongTaskResult(
            String taskName,
            Integer seconds,
            Integer progress,
            Boolean success,
            String message
    ) {
    }
}

```

### 示例六：异步 Tool

这里放异步 Tool 示例代码。

```java
package io.github.atengk.mcp.tool;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * MCP Tool：异步报表生成工具服务
 * <p>
 * 该组件用于演示响应式 Tool 写法，
 * 适合在 ASYNC 类型的 MCP Server 中使用。
 *
 * @author Ateng
 * @since 2026-04-23
 */
@Service
public class AsyncReportTool {

    /**
     * 日志组件
     */
    private static final Logger log = LoggerFactory.getLogger(AsyncReportTool.class);

    /**
     * 异步生成报表
     * <p>
     * 该方法返回 Mono，适合异步或非阻塞处理场景。
     *
     * @param reportName 报表名称
     * @param days       统计天数
     * @return 异步报表结果
     */
    @McpTool(
            name = "generateAsyncReport",
            title = "异步报表生成",
            description = "异步生成指定时间范围的统计报表",
            annotations = @McpTool.McpAnnotations(
                    title = "异步报表生成工具",
                    readOnlyHint = true,
                    destructiveHint = false,
                    idempotentHint = true
            )
    )
    public Mono<AsyncReportResult> generateAsyncReport(
            @McpToolParam(description = "报表名称", required = true) String reportName,
            @McpToolParam(description = "统计天数", required = true) int days) {

        return Mono.fromCallable(() -> {
                    if (days <= 0) {
                        log.warn("MCP工具[generateAsyncReport]参数非法，days={}", days);
                        throw new IllegalArgumentException("days 必须大于 0");
                    }

                    log.debug("MCP工具[generateAsyncReport]开始执行，reportName={}，days={}", reportName, days);

                    // 模拟异步计算或远程调用
                    ThreadUtil.sleep(800);

                    AsyncReportResult result = new AsyncReportResult(
                            reportName,
                            days,
                            StrUtil.format("{}-REPORT-{}", StrUtil.upperFirst(reportName), System.currentTimeMillis()),
                            true,
                            StrUtil.format("报表 [{}] 已生成，统计范围 {} 天", reportName, days)
                    );

                    log.debug("MCP工具[generateAsyncReport]执行完成，result={}", result);
                    return result;
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * MCP Tool：异步报表结果对象
     *
     * @param reportName 报表名称
     * @param days       统计天数
     * @param reportId   报表编号
     * @param success    是否成功
     * @param message    结果说明
     * @author Ateng
     * @since 2026-04-23
     */
    public record AsyncReportResult(
            String reportName,
            Integer days,
            String reportId,
            Boolean success,
            String message
    ) {
    }
}
```

------

## 定义 MCP Resource

### 作用说明

`@McpResource` 用于暴露“可读取资源”。
它更适合表达“读取某个资源内容”，而不是“执行一个功能”。

适合的资源类型包括：

- 系统运行信息
- 配置项
- 用户资料
- 安全数据
- 订单详情
- 报表内容

### 典型场景

根据前面完成的示例，`McpResource` 常见有以下几类：

#### 1. 固定资源

适合读取固定 URI 的资源内容，例如系统运行信息。
比如：

- 当前服务运行状态
- JVM 信息
- 启动时间
- 服务版本

#### 2. 基于 URI 模板的资源

适合通过 URI 变量读取不同内容，例如：

- `config://{key}`
- `user-profile://{username}`
- `order://{orderNo}`

这种方式很适合把资源按 URI 语义进行统一管理。

#### 3. 结构化资源

适合返回 JSON、文本详情等结构化内容。
如果资源内容需要更明确的 MIME 类型，推荐返回 `ReadResourceResult`。

#### 4. 权限感知资源

适合根据元数据判断调用人是否有权限访问资源内容。
常见于：

- 安全数据
- 租户资源
- 内部管理信息

#### 5. 异步资源

适合远程读取、异步 IO 或响应式数据源读取场景。
如果使用异步资源，需要配合 `ASYNC` 类型的 MCP Server。

### 开发建议

#### title 的位置说明

`@McpResource` 的 `title` 是写在顶层的，不是在 `annotations` 里。
这一点和 `@McpTool` 不完全一样，需要特别注意。

可以理解为：

- `name`：程序化名称
- `title`：更适合展示给客户端的名称
- `description`：资源说明

#### 什么时候直接返回 String

如果资源内容很简单，只是返回一段普通文本，那么直接返回 `String` 就可以。

#### 什么时候返回 ReadResourceResult

如果你希望：

- 自定义 MIME 类型
- 返回多个资源内容片段
- 更清晰地描述资源输出

那么更推荐返回 `ReadResourceResult`。

### 示例一：系统运行信息资源

这里放固定系统资源示例代码。

```java
package io.github.atengk.mcp.resource;

import cn.hutool.core.date.DateUtil;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceResult;
import io.modelcontextprotocol.spec.McpSchema.TextResourceContents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.mcp.annotation.McpResource;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.time.Instant;
import java.util.List;

/**
 * MCP Resource：系统运行信息资源服务
 * <p>
 * 该组件用于向 MCP Client 提供当前服务的运行信息，
 * 包括运行状态、当前时间、运行时长以及 JVM 名称。
 *
 * @author Ateng
 * @since 2026-04-23
 */
@Service
public class SystemResource {

    /**
     * 日志组件
     */
    private static final Logger log = LoggerFactory.getLogger(SystemResource.class);

    /**
     * 读取系统运行信息资源
     * <p>
     * 该方法会组装当前 MCP Server 的运行状态信息，
     * 并以文本资源的形式返回给客户端。
     *
     * @return 系统运行信息资源结果
     */
    @McpResource(
            uri = "system://runtime/info",
            name = "systemRuntimeInfo",
            title = "系统运行信息",
            description = "获取 MCP Server 的运行状态、启动时间、运行时长及 JVM 信息（只读）"
    )
    public ReadResourceResult systemInfo() {
        log.debug("MCP资源[systemRuntimeInfo]被访问");

        // 构建系统运行信息文本
        String info = buildSystemInfo();

        log.debug("MCP资源[systemRuntimeInfo]返回成功，内容长度={}", info.length());
        return new ReadResourceResult(List.of(
                new TextResourceContents("system://runtime/info", "text/plain", info)
        ));
    }

    /**
     * 构建系统运行信息文本内容
     * <p>
     * 主要包含当前时间、服务运行时长和 JVM 名称。
     *
     * @return 系统运行信息文本
     */
    private String buildSystemInfo() {
        // 获取 JVM 已运行时长，单位为毫秒
        long uptime = ManagementFactory.getRuntimeMXBean().getUptime();

        // 获取当前时间并格式化为常见日期时间格式
        Instant now = Instant.now();
        String startTime = DateUtil.formatDateTime(DateUtil.date(now.toEpochMilli()));

        // 返回格式化后的系统信息文本
        return """
                MCP Server Runtime Status
                -------------------------
                Status      : RUNNING
                Current Time: %s
                Uptime      : %d ms
                JVM Name    : %s
                """.formatted(
                startTime,
                uptime,
                ManagementFactory.getRuntimeMXBean().getVmName()
        );
    }
}
```

### 示例二：配置项资源

这里放基于配置键读取内容的资源示例代码。

```java
package io.github.atengk.mcp.resource;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.mcp.annotation.McpResource;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * MCP Resource：系统配置资源服务
 * <p>
 * 该组件用于根据配置键读取系统配置值，
 * 适合演示最简单的字符串资源返回方式。
 *
 * @author Ateng
 * @since 2026-04-23
 */
@Service
public class ConfigResource {

    /**
     * 日志组件
     */
    private static final Logger log = LoggerFactory.getLogger(ConfigResource.class);

    /**
     * 模拟配置数据
     */
    private static final Map<String, String> CONFIG_MAP = MapUtil.<String, String>builder()
            .put("app.name", "spring-ai-mcp-server")
            .put("app.env", "dev")
            .put("feature.weather.enabled", "true")
            .build();

    /**
     * 根据配置键读取配置值
     *
     * @param key 配置键
     * @return 配置值
     */
    @McpResource(
            uri = "config://{key}",
            name = "configResource",
            title = "系统配置资源",
            description = "根据配置键读取系统配置值",
            mimeType = "text/plain"
    )
    public String getConfig(String key) {
        log.debug("MCP资源[configResource]被访问，key={}", key);

        String value = CONFIG_MAP.get(key);
        if (StrUtil.isBlank(value)) {
            log.warn("MCP资源[configResource]未找到配置项，key={}", key);
            return StrUtil.format("未找到配置项：{}", key);
        }

        log.debug("MCP资源[configResource]返回成功，key={}，value={}", key, value);
        return value;
    }
}

```

### 示例三：用户资料资源

这里放结构化 JSON 资源示例代码。

```java
package io.github.atengk.mcp.resource;

import cn.hutool.json.JSONUtil;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceResult;
import io.modelcontextprotocol.spec.McpSchema.TextResourceContents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.mcp.annotation.McpResource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * MCP Resource：用户资料资源服务
 * <p>
 * 该组件用于根据用户名返回结构化的用户资料信息，
 * 适合演示 JSON 资源返回方式。
 *
 * @author Ateng
 * @since 2026-04-23
 */
@Service
public class UserProfileResource {

    /**
     * 日志组件
     */
    private static final Logger log = LoggerFactory.getLogger(UserProfileResource.class);

    /**
     * 根据用户名读取用户资料
     *
     * @param username 用户名
     * @return 用户资料资源结果
     */
    @McpResource(
            uri = "user-profile://{username}",
            name = "userProfileResource",
            title = "用户资料资源",
            description = "根据用户名返回结构化的用户资料信息",
            mimeType = "application/json"
    )
    public ReadResourceResult getUserProfile(String username) {
        log.debug("MCP资源[userProfileResource]被访问，username={}", username);

        Map<String, Object> profile = Map.of(
                "username", username,
                "nickname", username + "_nick",
                "age", 26,
                "city", "杭州",
                "status", "ACTIVE"
        );

        String content = JSONUtil.toJsonStr(profile);

        log.debug("MCP资源[userProfileResource]返回成功，username={}", username);
        return new ReadResourceResult(List.of(
                new TextResourceContents(
                        "user-profile://" + username,
                        "application/json",
                        content
                )
        ));
    }
}
```

### 示例四：安全数据资源

这里放带权限控制的资源示例代码。

```java
package io.github.atengk.mcp.resource;

import cn.hutool.core.convert.Convert;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceResult;
import io.modelcontextprotocol.spec.McpSchema.TextResourceContents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.mcp.annotation.McpMeta;
import org.springaicommunity.mcp.annotation.McpResource;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * MCP Resource：安全数据资源服务
 * <p>
 * 该组件用于演示通过请求元数据进行权限判断，
 * 只有具备指定访问级别的调用方才能读取真实数据。
 *
 * @author Ateng
 * @since 2026-04-23
 */
@Service
public class SecureDataResource {

    /**
     * 日志组件
     */
    private static final Logger log = LoggerFactory.getLogger(SecureDataResource.class);

    /**
     * 根据资源编号读取安全数据
     *
     * @param id   资源编号
     * @param meta MCP 请求元数据
     * @return 安全数据资源结果
     */
    @McpResource(
            uri = "secure-data://{id}",
            name = "secureDataResource",
            title = "安全数据资源",
            description = "根据访问级别读取安全数据",
            mimeType = "text/plain"
    )
    public ReadResourceResult getSecureData(String id, McpMeta meta) {
        String requestingUser = Convert.toStr(meta.get("requestingUser"), "anonymous");
        String accessLevel = Convert.toStr(meta.get("accessLevel"), "guest");

        log.debug("MCP资源[secureDataResource]被访问，id={}，requestingUser={}，accessLevel={}",
                id, requestingUser, accessLevel);

        String content;
        if (!"admin".equalsIgnoreCase(accessLevel)) {
            log.warn("MCP资源[secureDataResource]访问被拒绝，id={}，requestingUser={}", id, requestingUser);
            content = "Access denied";
        } else {
            content = "机密数据内容：合同编号=" + id + "，当前状态=已生效";
        }

        return new ReadResourceResult(List.of(
                new TextResourceContents(
                        "secure-data://" + id,
                        "text/plain",
                        content
                )
        ));
    }
}
```

### 示例五：订单详情资源

这里放基于上下文读取的资源示例代码。

```java
package io.github.atengk.mcp.resource;

import cn.hutool.core.util.StrUtil;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceResult;
import io.modelcontextprotocol.spec.McpSchema.TextResourceContents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.mcp.annotation.McpResource;
import org.springaicommunity.mcp.context.McpSyncRequestContext;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * MCP Resource：订单详情资源服务
 * <p>
 * 该组件用于演示在读取资源时使用请求上下文，
 * 向客户端发送日志和心跳通知。
 *
 * @author Ateng
 * @since 2026-04-23
 */
@Service
public class OrderResource {

    /**
     * 日志组件
     */
    private static final Logger log = LoggerFactory.getLogger(OrderResource.class);

    /**
     * 根据订单号读取订单详情
     *
     * @param context MCP 同步请求上下文
     * @param orderNo 订单号
     * @return 订单详情资源结果
     */
    @McpResource(
            uri = "order://{orderNo}",
            name = "orderResource",
            title = "订单详情资源",
            description = "根据订单号读取订单详情信息",
            mimeType = "text/plain"
    )
    public ReadResourceResult getOrderDetail(McpSyncRequestContext context, String orderNo) {
        log.debug("MCP资源[orderResource]被访问，orderNo={}", orderNo);

        context.info("开始读取订单详情：" + orderNo);
        context.ping();

        String content = StrUtil.format("""
                订单详情
                -------------------------
                订单号   : {}
                状态     : 已支付
                金额     : 199.00
                收货城市 : 杭州
                """, orderNo);

        log.debug("MCP资源[orderResource]返回成功，orderNo={}", orderNo);
        return new ReadResourceResult(List.of(
                new TextResourceContents(
                        "order://" + orderNo,
                        "text/plain",
                        content
                )
        ));
    }
}
```

### 示例六：异步资源

这里放异步资源示例代码。

```java
package io.github.atengk.mcp.resource;

import cn.hutool.core.thread.ThreadUtil;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceResult;
import io.modelcontextprotocol.spec.McpSchema.TextResourceContents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.mcp.annotation.McpResource;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

/**
 * MCP Resource：异步报表资源服务
 * <p>
 * 该组件用于演示异步读取资源的写法，
 * 仅适用于 ASYNC 类型的 MCP Server。
 *
 * @author Ateng
 * @since 2026-04-23
 */
@Service
public class AsyncReportResource {

    /**
     * 日志组件
     */
    private static final Logger log = LoggerFactory.getLogger(AsyncReportResource.class);

    /**
     * 异步读取报表内容
     *
     * @param reportId 报表编号
     * @return 异步资源结果
     */
    @McpResource(
            uri = "async-report://{reportId}",
            name = "asyncReportResource",
            title = "异步报表资源",
            description = "根据报表编号异步读取报表内容",
            mimeType = "text/plain"
    )
    public Mono<ReadResourceResult> getAsyncReport(String reportId) {
        return Mono.fromCallable(() -> {
                    log.debug("MCP资源[asyncReportResource]被访问，reportId={}", reportId);

                    // 模拟远程读取或异步 IO
                    ThreadUtil.sleep(500);

                    String content = """
                            异步报表内容
                            -------------------------
                            报表编号 : %s
                            状态     : 已生成
                            """.formatted(reportId);

                    log.debug("MCP资源[asyncReportResource]返回成功，reportId={}", reportId);
                    return new ReadResourceResult(List.of(
                            new TextResourceContents(
                                    "async-report://" + reportId,
                                    "text/plain",
                                    content
                            )
                    ));
                })
                .subscribeOn(Schedulers.boundedElastic());
    }
}
```

------

## 定义 MCP Prompt

### 作用说明

`@McpPrompt` 用于暴露“可复用提示模板”。
它的主要职责不是直接执行业务，而是为模型或客户端生成标准化 Prompt 内容。

适合的场景包括：

- 问候语模板
- 写作模板
- 个性化简介模板
- 文章大纲模板
- 本地化提示模板

### 典型场景

根据前面完成的示例，`McpPrompt` 常见可以整理为以下几类：

#### 1. 单参数 Prompt

适合最基础的 Prompt 生成场景，例如根据用户名生成问候语。
这是最简单、最常见的一类。

#### 2. 固定模板 Prompt

适合没有外部参数，直接返回一段标准提示模板的场景。
例如：

- 固定写作模板
- 固定代码审查模板
- 固定周报模板

#### 3. 可选参数 Prompt

适合部分参数可传可不传的场景。
例如简介生成：

- 姓名必填
- 年龄可选
- 兴趣可选
- 语气可选

#### 4. 多消息组合 Prompt

适合需要把角色设定、输出目标、限制条件拆开描述的场景。
这种方式通常比单条大字符串更清晰。

#### 5. 上下文感知 Prompt

适合根据语言、地区、租户、身份等元数据生成不同提示内容的场景。
例如国际化、本地化、按角色定制提示等。

### 开发建议

#### title 直接写在 @McpPrompt 顶层

`@McpPrompt` 的 `title` 是直接写在注解顶层的。
这一点和 `@McpResource` 一样，比较直观。

#### 参数统一使用 @McpArg

Prompt 参数建议统一使用 `@McpArg`，并补充：

- name
- description
- required

#### Prompt 结果尽量语义清晰

返回 `GetPromptResult` 时，建议标题、消息内容都保持清晰统一。
如果文档整体以中文为主，建议 `title` 和结果名称也统一为中文。

### 示例一：问候语 Prompt

这里放基础问候语 Prompt 示例代码。

```java
package io.github.atengk.mcp.prompt;

import cn.hutool.core.util.StrUtil;
import io.modelcontextprotocol.spec.McpSchema.GetPromptResult;
import io.modelcontextprotocol.spec.McpSchema.PromptMessage;
import io.modelcontextprotocol.spec.McpSchema.Role;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.mcp.annotation.McpArg;
import org.springaicommunity.mcp.annotation.McpPrompt;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * MCP Prompt：问候语生成服务
 * <p>
 * 该组件用于根据用户传入的姓名，
 * 生成一段自然、友好的问候提示内容，
 * 供 MCP Client 或大模型继续使用。
 *
 * @author Ateng
 * @since 2026-04-23
 */
@Service
public class GreetingPrompt {

    /**
     * 日志组件
     */
    private static final Logger log = LoggerFactory.getLogger(GreetingPrompt.class);

    /**
     * 生成问候提示内容
     * <p>
     * 该方法会根据传入的用户名，
     * 组装一段适合大模型使用的问候提示语。
     *
     * @param name 用户名
     * @return MCP Prompt 返回结果
     */
    @McpPrompt(
            name = "greeting",
            title = "问候提示词",
            description = "根据用户名生成一段自然、友好的问候提示语"
    )
    public GetPromptResult greeting(
            @McpArg(name = "name", description = "用户名", required = true) String name) {

        log.debug("MCP提示[greeting]执行，参数name={}", name);

        // 生成给模型使用的提示内容
        String message = StrUtil.format(
                "请用自然、友好的语气向用户“{}”打招呼，可以适当加入寒暄或祝福语。",
                name
        );

        log.debug("MCP提示[greeting]生成完成");

        // 返回标准的 Prompt 结果，供 MCP Client 或模型继续处理
        return new GetPromptResult(
                "问候提示词",
                List.of(new PromptMessage(Role.USER, new TextContent(message)))
        );
    }
}
```

### 示例二：固定写作模板 Prompt

这里放固定模板 Prompt 示例代码。

```java
package io.github.atengk.mcp.prompt;

import io.modelcontextprotocol.spec.McpSchema.GetPromptResult;
import io.modelcontextprotocol.spec.McpSchema.PromptMessage;
import io.modelcontextprotocol.spec.McpSchema.Role;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.mcp.annotation.McpPrompt;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * MCP Prompt：固定写作模板服务
 * <p>
 * 该组件用于提供一套固定的写作提示模板，
 * 适合不需要入参的通用 Prompt 场景。
 *
 * @author Ateng
 * @since 2026-04-23
 */
@Service
public class FixedWritingPrompt {

    /**
     * 日志组件
     */
    private static final Logger log = LoggerFactory.getLogger(FixedWritingPrompt.class);

    /**
     * 返回固定写作提示模板
     *
     * @return MCP Prompt 返回结果
     */
    @McpPrompt(
            name = "fixedWritingPrompt",
            title = "固定写作模板",
            description = "提供一套固定的中文写作提示模板"
    )
    public GetPromptResult fixedWritingPrompt() {
        log.debug("MCP提示[fixedWritingPrompt]执行");

        String message = """
                你是一名专业的中文写作助手，请严格遵循以下要求：
                1. 表达准确、简洁、自然
                2. 先给结论，再补充细节
                3. 输出结构清晰，必要时分点说明
                4. 如果涉及代码，请补充关键注释
                """;

        log.debug("MCP提示[fixedWritingPrompt]生成完成");
        return new GetPromptResult(
                "固定写作模板",
                List.of(new PromptMessage(Role.USER, new TextContent(message)))
        );
    }
}
```

### 示例三：个性化简介 Prompt

这里放可选参数 Prompt 示例代码。

```java
package io.github.atengk.mcp.prompt;

import cn.hutool.core.util.StrUtil;
import io.modelcontextprotocol.spec.McpSchema.GetPromptResult;
import io.modelcontextprotocol.spec.McpSchema.PromptMessage;
import io.modelcontextprotocol.spec.McpSchema.Role;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.mcp.annotation.McpArg;
import org.springaicommunity.mcp.annotation.McpPrompt;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * MCP Prompt：个性化简介生成服务
 * <p>
 * 该组件用于根据用户的可选信息，
 * 动态生成个性化简介提示内容。
 *
 * @author Ateng
 * @since 2026-04-23
 */
@Service
public class ProfileIntroPrompt {

    /**
     * 日志组件
     */
    private static final Logger log = LoggerFactory.getLogger(ProfileIntroPrompt.class);

    /**
     * 生成个性化简介提示内容
     *
     * @param name      用户名
     * @param age       年龄
     * @param interests 兴趣爱好
     * @param tone      语气风格
     * @return MCP Prompt 返回结果
     */
    @McpPrompt(
            name = "profileIntro",
            title = "个性化简介提示词",
            description = "根据姓名、年龄、兴趣和语气风格生成个性化简介提示内容"
    )
    public GetPromptResult profileIntro(
            @McpArg(name = "name", description = "用户名", required = true) String name,
            @McpArg(name = "age", description = "年龄", required = false) Integer age,
            @McpArg(name = "interests", description = "兴趣爱好，多个可用逗号分隔", required = false) String interests,
            @McpArg(name = "tone", description = "语气风格，例如：正式、轻松、活泼", required = false) String tone) {

        log.debug("MCP提示[profileIntro]执行，name={}，age={}，interests={}，tone={}", name, age, interests, tone);

        StringBuilder message = new StringBuilder();
        message.append("请根据以下信息生成一段自然、简洁的个人简介。").append("\n");
        message.append("姓名：").append(name).append("\n");

        if (age != null) {
            message.append("年龄：").append(age).append("\n");
        }

        if (StrUtil.isNotBlank(interests)) {
            message.append("兴趣：").append(interests).append("\n");
        }

        if (StrUtil.isNotBlank(tone)) {
            message.append("语气要求：").append(tone).append("\n");
        } else {
            message.append("语气要求：自然、友好").append("\n");
        }

        message.append("要求输出 100 字以内，适合作为个人资料简介。");

        log.debug("MCP提示[profileIntro]生成完成");
        return new GetPromptResult(
                "个性化简介提示词",
                List.of(new PromptMessage(Role.USER, new TextContent(message.toString())))
        );
    }
}
```

### 示例四：文章大纲 Prompt

这里放多消息组合 Prompt 示例代码。

```java
package io.github.atengk.mcp.prompt;

import cn.hutool.core.util.StrUtil;
import io.modelcontextprotocol.spec.McpSchema.GetPromptResult;
import io.modelcontextprotocol.spec.McpSchema.PromptMessage;
import io.modelcontextprotocol.spec.McpSchema.Role;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.mcp.annotation.McpArg;
import org.springaicommunity.mcp.annotation.McpPrompt;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * MCP Prompt：文章大纲生成服务
 * <p>
 * 该组件用于通过多条消息组合的方式，
 * 生成结构更清晰的文章大纲提示内容。
 *
 * @author Ateng
 * @since 2026-04-23
 */
@Service
public class ArticleOutlinePrompt {

    /**
     * 日志组件
     */
    private static final Logger log = LoggerFactory.getLogger(ArticleOutlinePrompt.class);

    /**
     * 生成文章大纲提示内容
     *
     * @param topic    文章主题
     * @param audience 目标读者
     * @return MCP Prompt 返回结果
     */
    @McpPrompt(
            name = "articleOutline",
            title = "文章大纲提示词",
            description = "根据主题和目标读者生成结构化文章大纲提示内容"
    )
    public GetPromptResult articleOutline(
            @McpArg(name = "topic", description = "文章主题", required = true) String topic,
            @McpArg(name = "audience", description = "目标读者，例如：初学者、后端开发者、产品经理", required = true) String audience) {

        log.debug("MCP提示[articleOutline]执行，topic={}，audience={}", topic, audience);

        String assistantMessage = """
                你是一名资深内容策划顾问，
                擅长把复杂主题拆解成清晰、可执行的文章结构。
                """;

        String userMessage = StrUtil.format("""
                请围绕主题“{}”生成一份文章大纲。
                目标读者：{}
                输出要求：
                1. 给出文章标题
                2. 给出一级、二级结构
                3. 每一部分补充一句核心说明
                4. 整体结构清晰、适合技术文章写作
                """, topic, audience);

        log.debug("MCP提示[articleOutline]生成完成");
        return new GetPromptResult(
                "文章大纲提示词",
                List.of(
                        new PromptMessage(Role.ASSISTANT, new TextContent(assistantMessage)),
                        new PromptMessage(Role.USER, new TextContent(userMessage))
                )
        );
    }
}
```

### 示例五：本地化 Prompt

这里放基于元数据生成提示内容的 Prompt 示例代码。

```java
package io.github.atengk.mcp.prompt;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import io.modelcontextprotocol.spec.McpSchema.GetPromptResult;
import io.modelcontextprotocol.spec.McpSchema.PromptMessage;
import io.modelcontextprotocol.spec.McpSchema.Role;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.mcp.annotation.McpArg;
import org.springaicommunity.mcp.annotation.McpMeta;
import org.springaicommunity.mcp.annotation.McpPrompt;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * MCP Prompt：本地化提示生成服务
 * <p>
 * 该组件用于根据请求元数据中的语言和地区信息，
 * 动态生成本地化提示内容。
 *
 * @author Ateng
 * @since 2026-04-23
 */
@Service
public class LocalizedPrompt {

    /**
     * 日志组件
     */
    private static final Logger log = LoggerFactory.getLogger(LocalizedPrompt.class);

    /**
     * 根据语言和地区生成本地化提示内容
     *
     * @param topic 主题
     * @param meta  MCP 请求元数据
     * @return MCP Prompt 返回结果
     */
    @McpPrompt(
            name = "localizedPrompt",
            title = "本地化提示词",
            description = "根据请求元数据中的语言和地区生成本地化提示内容"
    )
    public GetPromptResult localizedPrompt(
            @McpArg(name = "topic", description = "主题", required = true) String topic,
            McpMeta meta) {

        String language = Convert.toStr(meta.get("language"), "zh-CN");
        String region = Convert.toStr(meta.get("region"), "CN");

        log.debug("MCP提示[localizedPrompt]执行，topic={}，language={}，region={}", topic, language, region);

        String message;
        if (StrUtil.startWithIgnoreCase(language, "en")) {
            message = StrUtil.format(
                    "Please generate a concise and natural prompt about [{}], suitable for users in region [{}].",
                    topic,
                    region
            );
        } else {
            message = StrUtil.format(
                    "请围绕“{}”生成一段简洁、自然的提示内容，适用于地区“{}”的用户。",
                    topic,
                    region
            );
        }

        log.debug("MCP提示[localizedPrompt]生成完成");
        return new GetPromptResult(
                "本地化提示词",
                List.of(new PromptMessage(Role.USER, new TextContent(message)))
        );
    }
}
```

------

## 定义 MCP Completion

### 作用说明

`@McpComplete` 用于提供自动补全能力。
它本身不是独立的业务功能，而是服务于 Prompt 参数补全或 Resource URI 补全。

这一类能力很适合改善交互体验，例如：

- 输入城市名时自动联想
- 输入国家名时自动联想
- 根据不同参数返回不同候选值
- 对资源 URI 中的变量做补全

### 注解特点

`@McpComplete` 和前面几个注解不一样，它本身比较轻量。
它没有 `title`、`description`、`name` 这些属性，核心只围绕两个目标：

- `prompt`
- `uri`

并且两者只能二选一，不能同时使用。

### 典型场景

根据前面完成的示例，`McpComplete` 常见有以下几类：

#### 1. 最基础的前缀补全

适合根据用户输入前缀返回候选列表。
例如：

- 城市名称补全
- 国家名称补全

#### 2. 按参数名分流补全

适合一个 Prompt 中有多个参数，每个参数需要不同补全逻辑的场景。
例如：

- `city`
- `country`
- `transport`

此时可以根据当前参数名决定补全哪一组数据。

#### 3. 增强型补全结果

适合希望补全结果更完整的场景。
除了候选值，还可以返回：

- 总数
- 是否还有更多结果

#### 4. Resource URI 补全

适合给 Resource URI 模板变量提供补全值。
例如：

- `config://{key}`

这种场景和 Prompt 参数补全不同，但也非常实用。

### 开发建议

#### 简单场景直接返回 List

如果只是普通联想列表，那么直接返回 `List<String>` 就够了。

#### 复杂场景使用 CompleteArgument

如果要区分“当前补全的是哪个参数”，推荐使用 `CompleteRequest.CompleteArgument`。
这样可以根据参数名动态分发逻辑。

#### 需要更多元信息时返回 CompleteResult

如果想显式告诉客户端补全总数、是否还有更多结果，可以返回 `CompleteResult`。

### 示例一：城市名称补全

这里放基础前缀补全示例代码。

```java
package io.github.atengk.mcp.completion;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import io.modelcontextprotocol.spec.McpSchema.CompleteRequest;
import io.modelcontextprotocol.spec.McpSchema.CompleteResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.mcp.annotation.McpComplete;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * MCP Completion：城市名称补全服务
 * <p>
 * 该组件用于在 Prompt 调用过程中，根据用户当前输入的内容，
 * 返回可选的城市名称候选项，提升交互体验。
 *
 * @author Ateng
 * @since 2026-04-23
 */
@Service
public class CityCompletion {

    /**
     * 日志组件
     */
    private static final Logger log = LoggerFactory.getLogger(CityCompletion.class);

    /**
     * 预置城市列表
     * <p>
     * 实际项目中也可以改为从数据库、配置中心或缓存中加载。
     */
    private static final List<String> CITY_LIST = List.of(
            "北京", "上海", "深圳", "杭州", "广州", "成都",
            "重庆", "武汉", "西安", "苏州", "南京", "天津"
    );

    /**
     * 根据输入前缀补全城市名称
     * <p>
     * 该方法会根据当前参数值进行前缀匹配，
     * 最多返回 10 条候选结果。
     *
     * @param argument 补全请求参数，包含参数名和当前输入值
     * @return 补全结果
     */
    @McpComplete(prompt = "greeting")
    public CompleteResult completeCityName(CompleteRequest.CompleteArgument argument) {
        // 当前补全的参数名
        String argName = argument.name();

        // 当前输入值，空值时转为空字符串，避免空指针问题
        String prefix = StrUtil.blankToDefault(argument.value(), "");

        log.debug("MCP补全[greeting]被触发，参数名={}, 前缀={}", argName, prefix);

        // 按输入前缀进行匹配；如果前缀为空，则返回前 10 条城市数据
        List<String> matches = CITY_LIST.stream()
                .filter(city -> StrUtil.isBlank(prefix) || StrUtil.startWith(city, prefix))
                .limit(10)
                .toList();

        log.debug("MCP补全[greeting]返回候选数量={}", matches.size());

        // hasMore=false 表示当前结果已经返回完，没有更多候选项
        return new CompleteResult(
                new CompleteResult.CompleteCompletion(
                        CollUtil.newArrayList(matches),
                        matches.size(),
                        false
                )
        );
    }
}
```

### 示例二：国家名称补全

这里放基础国家补全示例代码。

```java
package io.github.atengk.mcp.completion;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import org.springaicommunity.mcp.annotation.McpComplete;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * MCP Completion：国家名称补全服务
 *
 * @author Ateng
 * @since 2026-04-23
 */
@Service
public class CountryCompletion {

    /**
     * 预置国家列表
     */
    private static final List<String> COUNTRY_LIST = List.of(
            "中国", "日本", "韩国", "美国", "英国", "法国",
            "德国", "加拿大", "澳大利亚", "新加坡"
    );

    /**
     * 根据输入前缀补全国家名称
     *
     * @param prefix 当前输入前缀
     * @return 国家名称候选列表
     */
    @McpComplete(prompt = "travelPlanner")
    public List<String> completeCountryName(String prefix) {
        if (StrUtil.isBlank(prefix)) {
            return CollUtil.newArrayList();
        }

        return COUNTRY_LIST.stream()
                .filter(country -> StrUtil.startWith(country, prefix))
                .limit(10)
                .toList();
    }
}
```

### 示例三：旅行参数补全

这里放按参数名分流补全示例代码。

```java
package io.github.atengk.mcp.completion;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import io.modelcontextprotocol.spec.McpSchema.CompleteRequest;
import org.springaicommunity.mcp.annotation.McpComplete;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * MCP Completion：旅行参数补全服务
 *
 * @author Ateng
 * @since 2026-04-23
 */
@Service
public class TravelCompletion {

    /**
     * 城市列表
     */
    private static final List<String> CITY_LIST = List.of(
            "北京", "上海", "深圳", "杭州", "广州", "成都"
    );

    /**
     * 国家列表
     */
    private static final List<String> COUNTRY_LIST = List.of(
            "中国", "日本", "韩国", "美国", "英国", "法国"
    );

    /**
     * 交通方式列表
     */
    private static final List<String> TRANSPORT_LIST = List.of(
            "飞机", "高铁", "火车", "自驾", "公交"
    );

    /**
     * 根据当前参数名和值返回不同补全结果
     *
     * @param argument 补全请求参数
     * @return 候选结果列表
     */
    @McpComplete(prompt = "travelPlanner")
    public List<String> completeTravelArgument(CompleteRequest.CompleteArgument argument) {
        String argName = argument.name();
        String prefix = StrUtil.blankToDefault(argument.value(), "");

        if ("city".equals(argName)) {
            return CITY_LIST.stream()
                    .filter(city -> StrUtil.isBlank(prefix) || StrUtil.startWith(city, prefix))
                    .limit(10)
                    .toList();
        }

        if ("country".equals(argName)) {
            return COUNTRY_LIST.stream()
                    .filter(country -> StrUtil.isBlank(prefix) || StrUtil.startWith(country, prefix))
                    .limit(10)
                    .toList();
        }

        if ("transport".equals(argName)) {
            return TRANSPORT_LIST.stream()
                    .filter(transport -> StrUtil.isBlank(prefix) || StrUtil.startWith(transport, prefix))
                    .limit(10)
                    .toList();
        }

        return CollUtil.newArrayList();
    }
}
```

### 示例四：代码片段补全

这里放增强型补全结果示例代码。

```java
package io.github.atengk.mcp.completion;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import io.modelcontextprotocol.spec.McpSchema.CompleteResult;
import org.springaicommunity.mcp.annotation.McpComplete;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * MCP Completion：代码片段补全服务
 *
 * @author Ateng
 * @since 2026-04-23
 */
@Service
public class CodeSnippetCompletion {

    /**
     * 预置代码片段列表
     */
    private static final List<String> SNIPPET_LIST = List.of(
            "public static void main(String[] args) {}",
            "@RestController",
            "@Service",
            "@Component",
            "@GetMapping(\"/list\")",
            "@PostMapping(\"/save\")",
            "private static final Logger log = LoggerFactory.getLogger(Xxx.class);"
    );

    /**
     * 根据输入前缀补全代码片段
     *
     * @param prefix 当前输入前缀
     * @return 补全结果对象
     */
    @McpComplete(prompt = "codeAssistant")
    public CompleteResult completeCodeSnippet(String prefix) {
        String actualPrefix = StrUtil.blankToDefault(prefix, "");

        List<String> matches = SNIPPET_LIST.stream()
                .filter(item -> StrUtil.isBlank(actualPrefix) || StrUtil.startWithIgnoreCase(item, actualPrefix))
                .limit(5)
                .toList();

        return new CompleteResult(
                new CompleteResult.CompleteCompletion(
                        CollUtil.newArrayList(matches),
                        matches.size(),
                        false
                )
        );
    }
}
```

### 示例五：配置键补全

这里放 Resource URI 补全示例代码。

```java
package io.github.atengk.mcp.completion;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import org.springaicommunity.mcp.annotation.McpComplete;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * MCP Completion：配置键补全服务
 *
 * @author Ateng
 * @since 2026-04-23
 */
@Service
public class ConfigKeyCompletion {

    /**
     * 预置配置键列表
     */
    private static final List<String> CONFIG_KEY_LIST = List.of(
            "app.name",
            "app.env",
            "server.port",
            "spring.application.name",
            "feature.weather.enabled",
            "feature.mcp.enabled"
    );

    /**
     * 对资源 URI 中的 key 变量进行补全
     *
     * @param prefix 当前输入前缀
     * @return 配置键候选列表
     */
    @McpComplete(uri = "config://{key}")
    public List<String> completeConfigKey(String prefix) {
        if (StrUtil.isBlank(prefix)) {
            return CollUtil.newArrayList(CONFIG_KEY_LIST);
        }

        return CONFIG_KEY_LIST.stream()
                .filter(key -> StrUtil.startWith(key, prefix))
                .limit(10)
                .toList();
    }
}
```

------

## 开发规范建议

为了让整套 MCP Server 示例更统一，建议保持以下风格。

### 1. 返回结果尽量结构化

对于 Tool，优先返回结构化对象，而不是基础类型。
例如：

- `MathResult`
- `WeatherResult`

### 2. title、name、description 各司其职

建议统一理解为：

- `name`：程序化标识，偏内部使用
- `title`：展示名称，偏客户端展示
- `description`：功能说明，偏语义描述

### 3. 注释风格保持统一

建议：

- 类注释说明职责
- 方法注释说明参数与返回值
- 关键逻辑加简洁行内注释
- 不写过度冗长的解释

### 4. 日志保持专业、简洁

建议记录：

- 方法是否被调用
- 核心输入参数
- 结果是否成功
- 异常或非法参数情况

### 5. 统一中文展示风格

如果文档和示例都以中文为主，建议：

- `title` 尽量使用中文
- 日志信息尽量使用中文
- 结果说明字段尽量使用中文

------

## 小结

到这里，Spring AI MCP Server 的四类核心能力就已经覆盖完成：

- Tool：对外提供功能能力
- Resource：对外提供只读资源
- Prompt：对外提供提示模板
- Completion：对外提供自动补全

在实际项目中，通常可以这样分工：

- Tool 负责“做事”
- Resource 负责“给数据”
- Prompt 负责“组织提示”
- Completion 负责“辅助输入”

