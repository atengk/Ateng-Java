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