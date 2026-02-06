package io.github.atengk.mcp.tool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.mcp.annotation.McpTool;
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
