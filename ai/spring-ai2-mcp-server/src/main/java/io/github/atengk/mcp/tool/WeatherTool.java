package io.github.atengk.mcp.tool;

import cn.hutool.core.util.StrUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
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