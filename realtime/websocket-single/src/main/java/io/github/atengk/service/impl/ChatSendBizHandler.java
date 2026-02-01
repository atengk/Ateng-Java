package io.github.atengk.service.impl;

import io.github.atengk.entity.WebSocketMessage;
import io.github.atengk.enums.WebSocketBizCode;
import io.github.atengk.service.WebSocketBizHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

/**
 * 聊天消息发送业务处理器
 *
 * <p>
 * 负责处理聊天发送相关的 WebSocket 业务消息，
 * 仅对指定的聊天发送业务编码进行处理。
 * </p>
 *
 * @author 孔余
 * @since 2026-01-30
 */
@Component
@Slf4j
public class ChatSendBizHandler implements WebSocketBizHandler {

    /**
     * 判断是否支持当前业务编码
     *
     * @param bizCode 业务编码
     * @return true 表示支持聊天发送业务
     */
    @Override
    public boolean support(String bizCode) {
        return WebSocketBizCode.CHAT_SEND.getCode().equals(bizCode);
    }

    /**
     * 处理聊天发送业务消息
     *
     * <p>
     * 当前示例仅记录日志，实际业务中可在此处进行
     * 消息持久化、转发、推送等操作。
     * </p>
     *
     * @param session 当前 WebSocket Session
     * @param message WebSocket 消息对象
     */
    @Override
    public void handle(WebSocketSession session, WebSocketMessage message) {
        log.info(
                "处理聊天发送消息，SessionID：{}，数据：{}",
                session.getId(),
                message.getData()
        );
    }
}
