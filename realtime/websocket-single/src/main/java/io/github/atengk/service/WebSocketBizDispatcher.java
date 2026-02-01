package io.github.atengk.service;

import io.github.atengk.entity.WebSocketMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;

/**
 * WebSocket 业务消息分发器
 *
 * <p>
 * 负责根据业务编码（bizCode），从已注册的 WebSocketBizHandler 中
 * 查找能够处理该业务的处理器，并进行消息分发。
 * </p>
 *
 * @author 孔余
 * @since 2026-01-30
 */
@Slf4j
@Component
public class WebSocketBizDispatcher {

    /**
     * 所有 WebSocket 业务处理器
     *
     * <p>
     * 由 Spring 自动注入，实现 WebSocketBizHandler 接口的 Bean
     * 都会被收集到该列表中。
     * </p>
     */
    private final List<WebSocketBizHandler> handlers;

    /**
     * 构造方法
     *
     * @param handlers WebSocket 业务处理器集合
     */
    public WebSocketBizDispatcher(List<WebSocketBizHandler> handlers) {
        this.handlers = handlers;
    }

    /**
     * 分发 WebSocket 业务消息
     *
     * <p>
     * 根据业务编码查找支持该编码的处理器，
     * 找到后立即处理消息并返回。
     * </p>
     *
     * @param session 当前 WebSocket Session
     * @param bizCode  业务编码
     * @param message  WebSocket 消息对象
     * @return true 表示已成功处理，false 表示未找到对应处理器
     */
    public boolean dispatch(WebSocketSession session, String bizCode, Object message) {
        for (WebSocketBizHandler handler : handlers) {
            if (handler.support(bizCode)) {
                handler.handle(session, (WebSocketMessage) message);
                return true;
            }
        }

        return false;
    }
}
