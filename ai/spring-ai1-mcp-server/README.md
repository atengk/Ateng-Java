# Spring AI MCP Server

## 版本信息

| 组件                 | 版本   |
| -------------------- | ------ |
| JDK                  | 21     |
| Maven                | 3.9.12 |
| SpringBoot           | 3.5.10 |
| Spring AI MCP Server | 1.1.2  |


------

## 基础配置

**添加依赖**

```xml
<properties>
    <spring-ai.version>1.1.2</spring-ai.version>
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

**编辑配置**

```yaml
---
# MCP Server 配置
spring:
  ai:
    mcp:
      server:
        enabled: true
        protocol: sse
        name: ateng-mcp-server
        version: 1.0.0
```

## 基础使用

## 定义 MCP Tool

MCP Tool 用于向模型暴露 **可调用的方法能力**。

**数学计算**

```java
package io.github.atengk.mcp.tool;

import org.springaicommunity.mcp.annotation.McpTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * MCP Tool 示例：数学计算
 */
@Component
public class MathTool {

    private static final Logger log = LoggerFactory.getLogger(MathTool.class);

    @McpTool(
            name = "add",
            title = "Addition Tool",
            description = "计算两个整数的和，仅用于无副作用的基础数学运算"
    )
    public int add(int a, int b) {
        log.debug("MCP Tool [add] invoked, a={}, b={}", a, b);

        int result = safeAdd(a, b);

        log.debug("MCP Tool [add] result={}", result);
        return result;
    }

    /**
     * 安全加法，防止整数溢出
     */
    private int safeAdd(int a, int b) {
        try {
            return Math.addExact(a, b);
        } catch (ArithmeticException ex) {
            log.warn("MCP Tool [add] overflow detected, a={}, b={}", a, b);
            throw new IllegalArgumentException("整数相加发生溢出");
        }
    }
}

```

**说明**

- `@McpTool`
  - `name`：暴露给模型的工具名称
  - `description`：模型理解工具用途的重要信息
- 方法参数会自动转换为 MCP Tool 的参数 schema
- 返回值会自动序列化为 MCP 响应

**获取天气**

```java
package io.github.atengk.mcp.tool;

import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

@Component
public class WeatherTool {

    @McpTool(
            name = "getTemperature",
            title = "Get Current Temperature",
            description = "获取指定城市的当前气温"
    )
    public String getTemperature(
            @McpToolParam(
                    description = "城市名称",
                    required = true
            ) String city) {

        // 这里一般是真实的业务逻辑 / RPC / HTTP
        return String.format("当前 %s 的气温是 22°C", city);
    }
}
```

------

## 定义 MCP Resource

Resource 用于向模型暴露 **只读数据源**，适合：

- 配置
- 文档
- 系统状态
- 静态信息

**示例：定义 Resource**

```java
package io.github.atengk.mcp.resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.mcp.annotation.McpResource;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.time.Instant;

/**
 * MCP Resource：系统运行信息
 * <p>
 * 提供 MCP Server 的基础运行状态，仅用于只读查询。
 */
@Component
public class SystemResource {

    private static final Logger log = LoggerFactory.getLogger(SystemResource.class);

    @McpResource(
            uri = "system://runtime/info",
            name = "systemRuntimeInfo",
            title = "System Runtime Information",
            description = "提供 MCP Server 的运行状态、启动时间及 JVM 基础信息，仅用于只读查询"
    )
    public String systemInfo() {
        log.debug("MCP Resource accessed: uri=system://runtime/info");

        String info = buildSystemInfo();

        log.debug("MCP Resource response generated, length={}", info.length());
        return info;
    }

    /**
     * 构建系统运行信息（只读）
     */
    private String buildSystemInfo() {
        long uptime = ManagementFactory.getRuntimeMXBean().getUptime();

        return """
                MCP Server Runtime Status
                -------------------------
                Status      : RUNNING
                Start Time  : %s
                Uptime      : %d ms
                JVM Name    : %s
                """.formatted(
                Instant.now(),
                uptime,
                ManagementFactory.getRuntimeMXBean().getVmName()
        );
    }
}
```

**Resource 访问方式**

模型会通过 `uri` 读取 Resource 内容，而不是“调用方法”。

------

## 定义 MCP Prompt

Prompt 用于向模型提供 **可复用的提示模板**。

**示例：定义 Prompt**

```java
package io.github.atengk.mcp.prompt;

import org.springaicommunity.mcp.annotation.McpPrompt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * MCP Prompt 示例：问候语生成
 */
@Component
public class GreetingPrompt {

    private static final Logger log = LoggerFactory.getLogger(GreetingPrompt.class);

    @McpPrompt(
            name = "greeting",
            title = "Greeting Prompt",
            description = "根据用户名生成一段友好、自然的问候提示语，用于引导模型输出问候内容"
    )
    public String greeting(String name) {
        log.debug("MCP Prompt [greeting] invoked, name={}", name);

        String prompt = "请用友好、自然的语气向用户 " + name + " 打招呼，可以适当加入寒暄或祝福语。";

        log.debug("MCP Prompt [greeting] generated prompt={}", prompt);
        return prompt;
    }
}

```

Prompt 会作为 **结构化 Prompt 能力** 暴露给 MCP Client。

------

