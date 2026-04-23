package io.github.atengk.mcp.completion;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import io.modelcontextprotocol.spec.McpSchema.CompleteRequest;
import org.springaicommunity.mcp.annotation.McpComplete;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * MCP Completion：旅行参数补全服务
 *
 * @author Ateng
 * @since 2026-04-23
 */
@Service
public class TravelCompletion {

    /**
     * 城市列表
     */
    private static final List<String> CITY_LIST = List.of(
            "北京", "上海", "深圳", "杭州", "广州", "成都"
    );

    /**
     * 国家列表
     */
    private static final List<String> COUNTRY_LIST = List.of(
            "中国", "日本", "韩国", "美国", "英国", "法国"
    );

    /**
     * 交通方式列表
     */
    private static final List<String> TRANSPORT_LIST = List.of(
            "飞机", "高铁", "火车", "自驾", "公交"
    );

    /**
     * 根据当前参数名和值返回不同补全结果
     *
     * @param argument 补全请求参数
     * @return 候选结果列表
     */
    @McpComplete(prompt = "travelPlanner")
    public List<String> completeTravelArgument(CompleteRequest.CompleteArgument argument) {
        String argName = argument.name();
        String prefix = StrUtil.blankToDefault(argument.value(), "");

        if ("city".equals(argName)) {
            return CITY_LIST.stream()
                    .filter(city -> StrUtil.isBlank(prefix) || StrUtil.startWith(city, prefix))
                    .limit(10)
                    .toList();
        }

        if ("country".equals(argName)) {
            return COUNTRY_LIST.stream()
                    .filter(country -> StrUtil.isBlank(prefix) || StrUtil.startWith(country, prefix))
                    .limit(10)
                    .toList();
        }

        if ("transport".equals(argName)) {
            return TRANSPORT_LIST.stream()
                    .filter(transport -> StrUtil.isBlank(prefix) || StrUtil.startWith(transport, prefix))
                    .limit(10)
                    .toList();
        }

        return CollUtil.newArrayList();
    }
}