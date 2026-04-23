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
