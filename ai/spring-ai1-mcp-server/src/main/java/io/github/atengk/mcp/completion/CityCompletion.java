package io.github.atengk.mcp.completion;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import io.modelcontextprotocol.spec.McpSchema.CompleteRequest;
import io.modelcontextprotocol.spec.McpSchema.CompleteResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.mcp.annotation.McpComplete;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * MCP Completion：城市名称补全服务
 * <p>
 * 该组件用于在 Prompt 调用过程中，根据用户当前输入的内容，
 * 返回可选的城市名称候选项，提升交互体验。
 *
 * @author Ateng
 * @since 2026-04-23
 */
@Service
public class CityCompletion {

    /**
     * 日志组件
     */
    private static final Logger log = LoggerFactory.getLogger(CityCompletion.class);

    /**
     * 预置城市列表
     * <p>
     * 实际项目中也可以改为从数据库、配置中心或缓存中加载。
     */
    private static final List<String> CITY_LIST = List.of(
            "北京", "上海", "深圳", "杭州", "广州", "成都",
            "重庆", "武汉", "西安", "苏州", "南京", "天津"
    );

    /**
     * 根据输入前缀补全城市名称
     * <p>
     * 该方法会根据当前参数值进行前缀匹配，
     * 最多返回 10 条候选结果。
     *
     * @param argument 补全请求参数，包含参数名和当前输入值
     * @return 补全结果
     */
    @McpComplete(prompt = "greeting")
    public CompleteResult completeCityName(CompleteRequest.CompleteArgument argument) {
        // 当前补全的参数名
        String argName = argument.name();

        // 当前输入值，空值时转为空字符串，避免空指针问题
        String prefix = StrUtil.blankToDefault(argument.value(), "");

        log.debug("MCP补全[greeting]被触发，参数名={}, 前缀={}", argName, prefix);

        // 按输入前缀进行匹配；如果前缀为空，则返回前 10 条城市数据
        List<String> matches = CITY_LIST.stream()
                .filter(city -> StrUtil.isBlank(prefix) || StrUtil.startWith(city, prefix))
                .limit(10)
                .toList();

        log.debug("MCP补全[greeting]返回候选数量={}", matches.size());

        // hasMore=false 表示当前结果已经返回完，没有更多候选项
        return new CompleteResult(
                new CompleteResult.CompleteCompletion(
                        CollUtil.newArrayList(matches),
                        matches.size(),
                        false
                )
        );
    }
}