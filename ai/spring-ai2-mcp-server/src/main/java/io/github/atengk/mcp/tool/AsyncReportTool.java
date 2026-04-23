package io.github.atengk.mcp.tool;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * MCP Tool：异步报表生成工具服务
 * <p>
 * 该组件用于演示响应式 Tool 写法，
 * 适合在 ASYNC 类型的 MCP Server 中使用。
 *
 * @author Ateng
 * @since 2026-04-23
 */
@Service
public class AsyncReportTool {

    /**
     * 日志组件
     */
    private static final Logger log = LoggerFactory.getLogger(AsyncReportTool.class);

    /**
     * 异步生成报表
     * <p>
     * 该方法返回 Mono，适合异步或非阻塞处理场景。
     *
     * @param reportName 报表名称
     * @param days       统计天数
     * @return 异步报表结果
     */
    @McpTool(
            name = "generateAsyncReport",
            title = "异步报表生成",
            description = "异步生成指定时间范围的统计报表",
            annotations = @McpTool.McpAnnotations(
                    title = "异步报表生成工具",
                    readOnlyHint = true,
                    destructiveHint = false,
                    idempotentHint = true
            )
    )
    public Mono<AsyncReportResult> generateAsyncReport(
            @McpToolParam(description = "报表名称", required = true) String reportName,
            @McpToolParam(description = "统计天数", required = true) int days) {

        return Mono.fromCallable(() -> {
                    if (days <= 0) {
                        log.warn("MCP工具[generateAsyncReport]参数非法，days={}", days);
                        throw new IllegalArgumentException("days 必须大于 0");
                    }

                    log.debug("MCP工具[generateAsyncReport]开始执行，reportName={}，days={}", reportName, days);

                    // 模拟异步计算或远程调用
                    ThreadUtil.sleep(800);

                    AsyncReportResult result = new AsyncReportResult(
                            reportName,
                            days,
                            StrUtil.format("{}-REPORT-{}", StrUtil.upperFirst(reportName), System.currentTimeMillis()),
                            true,
                            StrUtil.format("报表 [{}] 已生成，统计范围 {} 天", reportName, days)
                    );

                    log.debug("MCP工具[generateAsyncReport]执行完成，result={}", result);
                    return result;
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * MCP Tool：异步报表结果对象
     *
     * @param reportName 报表名称
     * @param days       统计天数
     * @param reportId   报表编号
     * @param success    是否成功
     * @param message    结果说明
     * @author Ateng
     * @since 2026-04-23
     */
    public record AsyncReportResult(
            String reportName,
            Integer days,
            String reportId,
            Boolean success,
            String message
    ) {
    }
}