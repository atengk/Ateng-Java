package io.github.atengk.mcp.resource;

import cn.hutool.core.util.StrUtil;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceResult;
import io.modelcontextprotocol.spec.McpSchema.TextResourceContents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.mcp.annotation.McpResource;
import org.springaicommunity.mcp.context.McpSyncRequestContext;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * MCP Resource：订单详情资源服务
 * <p>
 * 该组件用于演示在读取资源时使用请求上下文，
 * 向客户端发送日志和心跳通知。
 *
 * @author Ateng
 * @since 2026-04-23
 */
@Service
public class OrderResource {

    /**
     * 日志组件
     */
    private static final Logger log = LoggerFactory.getLogger(OrderResource.class);

    /**
     * 根据订单号读取订单详情
     *
     * @param context MCP 同步请求上下文
     * @param orderNo 订单号
     * @return 订单详情资源结果
     */
    @McpResource(
            uri = "order://{orderNo}",
            name = "orderResource",
            title = "订单详情资源",
            description = "根据订单号读取订单详情信息",
            mimeType = "text/plain"
    )
    public ReadResourceResult getOrderDetail(McpSyncRequestContext context, String orderNo) {
        log.debug("MCP资源[orderResource]被访问，orderNo={}", orderNo);

        context.info("开始读取订单详情：" + orderNo);
        context.ping();

        String content = StrUtil.format("""
                订单详情
                -------------------------
                订单号   : {}
                状态     : 已支付
                金额     : 199.00
                收货城市 : 杭州
                """, orderNo);

        log.debug("MCP资源[orderResource]返回成功，orderNo={}", orderNo);
        return new ReadResourceResult(List.of(
                new TextResourceContents(
                        "order://" + orderNo,
                        "text/plain",
                        content
                )
        ));
    }
}