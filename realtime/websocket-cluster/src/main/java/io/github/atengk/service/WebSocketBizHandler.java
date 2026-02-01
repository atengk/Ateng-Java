package io.github.atengk.service;

import io.github.atengk.entity.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

/**
 * WebSocket 业务消息处理器接口
 *
 * <p>
 * 定义 WebSocket 业务消息的处理规范，
 * 每个业务处理器通过业务编码（bizCode）进行区分。
 * </p>
 *
 * @author 孔余
 * @since 2026-01-30
 */
public interface WebSocketBizHandler {

    /**
     * 判断当前处理器是否支持指定的业务编码
     *
     * @param bizCode 业务编码
     * @return true 表示支持，false 表示不支持
     */
    boolean support(String bizCode);

    /**
     * 处理 WebSocket 业务消息
     *
     * @param session 当前 WebSocket Session
     * @param message WebSocket 消息对象
     */
    void handle(WebSocketSession session, WebSocketMessage message);
}
