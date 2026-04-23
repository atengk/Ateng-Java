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