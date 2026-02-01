package io.github.atengk.entity;

import lombok.Data;

/**
 * WebSocket 消息实体
 *
 * <p>
 * 用于客户端与服务端之间统一的数据传输结构，
 * 支持不同类型的业务消息。
 * </p>
 *
 * @author 孔余
 * @since 2026-01-30
 */
@Data
public class WebSocketMessage {

    /**
     * 消息类型
     *
     * <p>
     * 用于区分业务消息、心跳消息、系统消息等
     * </p>
     */
    private String type;

    /**
     * 业务状态码
     *
     * <p>
     * 用于标识消息处理结果或业务场景
     * </p>
     */
    private String code;

    /**
     * 消息数据体
     *
     * <p>
     * 具体业务数据，由不同消息类型决定
     * </p>
     */
    private Object data;
}
