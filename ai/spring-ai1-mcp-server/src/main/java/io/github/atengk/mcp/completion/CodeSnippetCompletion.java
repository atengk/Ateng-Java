package io.github.atengk.mcp.completion;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import io.modelcontextprotocol.spec.McpSchema.CompleteResult;
import org.springaicommunity.mcp.annotation.McpComplete;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * MCP Completion：代码片段补全服务
 *
 * @author Ateng
 * @since 2026-04-23
 */
@Service
public class CodeSnippetCompletion {

    /**
     * 预置代码片段列表
     */
    private static final List<String> SNIPPET_LIST = List.of(
            "public static void main(String[] args) {}",
            "@RestController",
            "@Service",
            "@Component",
            "@GetMapping(\"/list\")",
            "@PostMapping(\"/save\")",
            "private static final Logger log = LoggerFactory.getLogger(Xxx.class);"
    );

    /**
     * 根据输入前缀补全代码片段
     *
     * @param prefix 当前输入前缀
     * @return 补全结果对象
     */
    @McpComplete(prompt = "codeAssistant")
    public CompleteResult completeCodeSnippet(String prefix) {
        String actualPrefix = StrUtil.blankToDefault(prefix, "");

        List<String> matches = SNIPPET_LIST.stream()
                .filter(item -> StrUtil.isBlank(actualPrefix) || StrUtil.startWithIgnoreCase(item, actualPrefix))
                .limit(5)
                .toList();

        return new CompleteResult(
                new CompleteResult.CompleteCompletion(
                        CollUtil.newArrayList(matches),
                        matches.size(),
                        false
                )
        );
    }
}