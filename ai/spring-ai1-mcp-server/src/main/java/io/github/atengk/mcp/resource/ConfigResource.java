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
