package io.github.atengk.mcp.completion;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import org.springframework.ai.mcp.annotation.McpComplete;
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