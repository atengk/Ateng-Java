package io.github.atengk.mcp.tool;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.ai.mcp.annotation.context.McpSyncRequestContext;
import org.springframework.stereotype.Service;

/**
 * MCP Tool：长任务处理工具服务
 * <p>
 * 该组件用于演示在 Tool 执行过程中发送日志、进度和心跳，
 * 适合处理耗时任务。
 *
 * @author Ateng
 * @since 2026-04-23
 */
@Service
public class LongRunningTool {

    /**
     * 日志组件
     */
    private static final Logger log = LoggerFactory.getLogger(LongRunningTool.class);

    /**
     * 执行长任务并回传进度
     * <p>
     * 该方法会模拟一个分阶段执行的任务，
     * 在执行过程中向客户端发送进度通知。
     *
     * @param context  MCP 同步请求上下文
     * @param taskName 任务名称
     * @param seconds  模拟执行时长
     * @return 长任务执行结果
     */
    @McpTool(
            name = "runLongTask",
            title = "长任务执行",
            description = "执行一个带进度反馈的长任务",
            annotations = @McpTool.McpAnnotations(
                    title = "长任务执行工具",
                    readOnlyHint = true,
                    destructiveHint = false,
                    idempotentHint = false
            )
    )
    public LongTaskResult runLongTask(
            McpSyncRequestContext context,
            @McpToolParam(description = "任务名称", required = true) String taskName,
            @McpToolParam(description = "模拟执行秒数，建议 1 到 10", required = true) int seconds) {

        if (seconds <= 0) {
            log.warn("MCP工具[runLongTask]参数非法，seconds={}", seconds);
            throw new IllegalArgumentException("seconds 必须大于 0");
        }

        log.debug("MCP工具[runLongTask]开始执行，taskName={}，seconds={}", taskName, seconds);
        context.info("任务开始执行：" + taskName);
        context.ping();

        Object progressToken = context.request().progressToken();
        if (ObjectUtil.isNotNull(progressToken)) {
            context.progress(0);
        }

        for (int i = 1; i <= seconds; i++) {
            ThreadUtil.sleep(1000);
            int progress = i * 100 / seconds;

            log.debug("MCP工具[runLongTask]执行中，taskName={}，progress={}%", taskName, progress);

            if (ObjectUtil.isNotNull(progressToken)) {
                context.progress(progress);
            }
        }

        context.info("任务执行完成：" + taskName);

        LongTaskResult result = new LongTaskResult(
                taskName,
                seconds,
                100,
                true,
                StrUtil.format("任务 [{}] 已执行完成，共耗时 {} 秒", taskName, seconds)
        );

        log.debug("MCP工具[runLongTask]执行完成，result={}", result);
        return result;
    }

    /**
     * MCP Tool：长任务结果对象
     *
     * @param taskName 任务名称
     * @param seconds  执行时长
     * @param progress 最终进度
     * @param success  是否成功
     * @param message  结果说明
     * @author Ateng
     * @since 2026-04-23
     */
    public record LongTaskResult(
            String taskName,
            Integer seconds,
            Integer progress,
            Boolean success,
            String message
    ) {
    }
}
