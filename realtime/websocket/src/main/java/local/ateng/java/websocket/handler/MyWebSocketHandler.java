package local.ateng.java.websocket.handler;

import local.ateng.java.websocket.service.WebSocketService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * WebSocket处理器，用于处理WebSocket连接的建立、消息的接收和发送，以及连接的关闭。
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-02-05
 */
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class MyWebSocketHandler extends TextWebSocketHandler {
    private final WebSocketService webSocketService;

    /**
     * 当WebSocket连接建立后调用的方法。
     *
     * @param session 当前WebSocket会话
     * @throws Exception 如果发生错误
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 将会话添加到WebSocketService中管理
        webSocketService.addSession(session);
    }

    /**
     * 处理收到的文本消息。
     *
     * @param session 当前WebSocket会话
     * @param message 收到的文本消息
     * @throws Exception 如果发生错误
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // 获取消息内容
        String payload = message.getPayload();
        // 返回消息
        session.sendMessage(new TextMessage("服务端收到了你的消息，当前时间戳：" + System.currentTimeMillis() + "，你发送的消息是：" + payload));
    }

    /**
     * 当WebSocket连接关闭后调用的方法。
     *
     * @param session 当前WebSocket会话
     * @param status  关闭状态
     * @throws Exception 如果发生错误
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        // 从WebSocketService中移除关闭的会话
        webSocketService.removeSession(session);
    }

}
