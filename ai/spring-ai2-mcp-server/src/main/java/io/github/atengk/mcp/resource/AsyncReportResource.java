package io.github.atengk.mcp.resource;

import cn.hutool.core.thread.ThreadUtil;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceResult;
import io.modelcontextprotocol.spec.McpSchema.TextResourceContents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.annotation.McpResource;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

/**
 * MCP Resource：异步报表资源服务
 * <p>
 * 该组件用于演示异步读取资源的写法，
 * 仅适用于 ASYNC 类型的 MCP Server。
 *
 * @author Ateng
 * @since 2026-04-23
 */
@Service
public class AsyncReportResource {

    /**
     * 日志组件
     */
    private static final Logger log = LoggerFactory.getLogger(AsyncReportResource.class);

    /**
     * 异步读取报表内容
     *
     * @param reportId 报表编号
     * @return 异步资源结果
     */
    @McpResource(
            uri = "async-report://{reportId}",
            name = "asyncReportResource",
            title = "异步报表资源",
            description = "根据报表编号异步读取报表内容",
            mimeType = "text/plain"
    )
    public Mono<ReadResourceResult> getAsyncReport(String reportId) {
        return Mono.fromCallable(() -> {
                    log.debug("MCP资源[asyncReportResource]被访问，reportId={}", reportId);

                    // 模拟远程读取或异步 IO
                    ThreadUtil.sleep(500);

                    String content = """
                            异步报表内容
                            -------------------------
                            报表编号 : %s
                            状态     : 已生成
                            """.formatted(reportId);

                    log.debug("MCP资源[asyncReportResource]返回成功，reportId={}", reportId);
                    return new ReadResourceResult(List.of(
                            new TextResourceContents(
                                    "async-report://" + reportId,
                                    "text/plain",
                                    content
                            )
                    ));
                })
                .subscribeOn(Schedulers.boundedElastic());
    }
}