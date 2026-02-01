package io.github.atengk.handler;

import com.alibaba.fastjson2.JSON;
import io.github.atengk.entity.WebSocketMessage;
import io.github.atengk.enums.WebSocketMessageType;
import io.github.atengk.interceptor.WebSocketAuthInterceptor;
import io.github.atengk.service.WebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * WebSocket 核心处理器
 *
 * <p>
 * 负责处理 WebSocket 生命周期事件以及文本消息的分发：
 * 连接建立、消息接收、连接关闭、传输异常等。
 * </p>
 *
 * @author 孔余
 * @since 2026-01-30
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketHandler extends TextWebSocketHandler {

    /**
     * WebSocket 业务服务
     */
    private final WebSocketService webSocketService;

    /**
     * WebSocket 连接建立成功后的回调
     *
     * <p>
     * 从握手阶段保存的 attributes 中获取用户ID，
     * 进行二次鉴权校验，并注册 WebSocket Session。
     * </p>
     *
     * @param session 当前 WebSocket Session
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String userId = (String) session.getAttributes()
                .get(WebSocketAuthInterceptor.ATTR_USER_ID);

        if (!webSocketService.authenticate(userId)) {
            log.warn("WebSocket 连接鉴权失败，关闭连接，SessionID：{}", session.getId());
            webSocketService.closeSession(session.getId(), CloseStatus.NOT_ACCEPTABLE);
            return;
        }

        webSocketService.registerSession(userId, session);
    }

    /**
     * 处理客户端发送的文本消息
     *
     * <p>
     * 解析消息内容，根据消息类型分发到不同的处理逻辑：
     * 心跳消息或业务消息。
     * </p>
     *
     * @param session 当前 WebSocket Session
     * @param message 客户端发送的文本消息
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            // 将 JSON 文本解析为 WebSocketMessage 对象
            WebSocketMessage wsMessage =
                    JSON.parseObject(message.getPayload(), WebSocketMessage.class);

            // 根据消息类型编码获取消息类型枚举
            WebSocketMessageType messageType =
                    WebSocketMessageType.fromCode(wsMessage.getType());

            // 未识别的消息类型，直接忽略
            if (messageType == null) {
                log.warn(
                        "收到未知 WebSocket 消息类型，SessionID：{}，type：{}",
                        session.getId(),
                        wsMessage.getType()
                );
                return;
            }

            switch (messageType) {
                case HEARTBEAT:
                    // 处理心跳消息
                    webSocketService.handleHeartbeat(session);
                    break;

                case BIZ:
                    // 处理业务消息
                    webSocketService.handleBizMessage(session, wsMessage);
                    break;

                default:
                    log.warn(
                            "未处理的 WebSocket 消息类型，SessionID：{}，type：{}",
                            session.getId(),
                            messageType.getCode()
                    );
            }

        } catch (Exception e) {
            log.error("处理 WebSocket 消息异常，SessionID：{}", session.getId(), e);
        }
    }

    /**
     * WebSocket 连接关闭后的回调
     *
     * <p>
     * 清理 Session 及相关连接信息。
     * </p>
     *
     * @param session 当前 WebSocket Session
     * @param status  连接关闭状态
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        webSocketService.removeSession(session);
        log.info("WebSocket 连接关闭，SessionID：{}，状态：{}", session.getId(), status);
    }

    /**
     * WebSocket 传输异常处理
     *
     * <p>
     * 出现传输异常时，主动关闭连接并释放资源。
     * </p>
     *
     * @param session   当前 WebSocket Session
     * @param exception 异常信息
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error(
                "WebSocket 传输异常，准备关闭连接，SessionID：{}",
                session.getId(),
                exception
        );

        webSocketService.closeSession(
                session.getId(),
                CloseStatus.SERVER_ERROR
        );
    }

}
