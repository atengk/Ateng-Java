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
