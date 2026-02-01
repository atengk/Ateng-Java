package io.github.atengk.service.impl;

import io.github.atengk.entity.WebSocketMessage;
import io.github.atengk.enums.WebSocketBizCode;
import io.github.atengk.service.WebSocketBizHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

/**
 * 通知确认业务处理器
 *
 * <p>
 * 用于处理客户端对通知类消息的确认（ACK）业务，
 * 通常用于已读回执、消息确认等场景。
 * </p>
 *
 * @author 孔余
 * @since 2026-01-30
 */
@Component
@Slf4j
public class NoticeAckBizHandler implements WebSocketBizHandler {

    /**
     * 判断是否支持当前业务编码
     *
     * @param bizCode 业务编码
     * @return true 表示支持通知确认业务
     */
    @Override
    public boolean support(String bizCode) {
        return WebSocketBizCode.NOTICE_ACK.getCode().equals(bizCode);
    }

    /**
     * 处理通知确认业务消息
     *
     * <p>
     * 当前示例仅记录日志，实际业务中可在此处进行
     * 通知状态更新、确认记录持久化等操作。
     * </p>
     *
     * @param session 当前 WebSocket Session
     * @param message WebSocket 消息对象
     */
    @Override
    public void handle(WebSocketSession session, WebSocketMessage message) {
        log.info(
                "处理通知确认消息，SessionID：{}，数据：{}",
                session.getId(),
                message.getData()
        );
    }
}
