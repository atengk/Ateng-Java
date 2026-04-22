package io.github.atengk.mcp.tool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

/**
 * MCP Tool：获取城市气温
 *
 * @author Ateng
 * @since 2026-04-22
 */
@Component
public class WeatherTool {

    private static final Logger log = LoggerFactory.getLogger(WeatherTool.class);

    @McpTool(
            name = "getTemperature",
            title = "Get Current Temperature",
            description = "获取指定城市的当前气温。输入为城市名称，返回该城市当前温度信息（示例数据）"
    )
    public String getTemperature(
            @McpToolParam(description = "城市名称，例如：北京、上海", required = true)
            String city) {

        // 模拟数据（实际应为 RPC / HTTP 调用）
        String result = String.format("当前 %s 的气温是 22°C", city);

        log.debug("MCP工具[getTemperature]执行完成，参数 city={}，结果 result={}", city, result);
        return result;
    }
}