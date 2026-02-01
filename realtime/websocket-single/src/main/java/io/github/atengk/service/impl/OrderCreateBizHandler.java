package io.github.atengk.service.impl;

import io.github.atengk.constants.WebSocketBizCodeConstants;
import io.github.atengk.entity.WebSocketMessage;
import io.github.atengk.service.WebSocketBizHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

/**
 * 订单创建业务处理器
 *
 * <p>
 * 用于处理订单创建相关的 WebSocket 业务消息，
 * 当客户端发送订单创建指令时由该处理器进行处理。
 * </p>
 *
 * @author 孔余
 * @since 2026-01-30
 */
@Component
@Slf4j
public class OrderCreateBizHandler implements WebSocketBizHandler {

    /**
     * 判断是否支持当前业务编码
     *
     * @param bizCode 业务编码
     * @return true 表示支持订单创建业务
     */
    @Override
    public boolean support(String bizCode) {
        return WebSocketBizCodeConstants.ORDER_CREATE.equals(bizCode);
    }

    /**
     * 处理订单创建业务消息
     *
     * <p>
     * 当前示例仅记录日志，实际业务中可在此处完成
     * 订单创建、校验、状态初始化等核心逻辑。
     * </p>
     *
     * @param session 当前 WebSocket Session
     * @param message WebSocket 消息对象
     */
    @Override
    public void handle(WebSocketSession session, WebSocketMessage message) {
        log.info(
                "处理订单创建消息，SessionID：{}，数据：{}",
                session.getId(),
                message.getData()
        );
    }
}
