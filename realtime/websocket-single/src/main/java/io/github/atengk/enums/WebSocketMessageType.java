package io.github.atengk.enums;

import lombok.Getter;

/**
 * WebSocket 消息类型枚举
 *
 * <p>
 * 用于区分 WebSocket 消息的基础类型，
 * 如心跳消息、业务消息等。
 * </p>
 *
 * @author 孔余
 * @since 2026-01-30
 */
@Getter
public enum WebSocketMessageType {

    /**
     * 心跳消息
     */
    HEARTBEAT("HEARTBEAT", "心跳"),

    /**
     * 心跳确认消息
     */
    HEARTBEAT_ACK("HEARTBEAT_ACK", "心跳确认"),

    /**
     * 业务消息
     */
    BIZ("BIZ", "业务消息");

    /**
     * 类型编码
     */
    private final String code;

    /**
     * 类型描述
     */
    private final String desc;

    WebSocketMessageType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 根据编码获取消息类型
     *
     * @param code 类型编码
     * @return 消息类型，未匹配返回 null
     */
    public static WebSocketMessageType fromCode(String code) {
        for (WebSocketMessageType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }
}
