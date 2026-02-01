package io.github.atengk.enums;

import lombok.Getter;

/**
 * WebSocket 业务编码枚举
 *
 * <p>
 * 用于区分不同业务类型的 WebSocket 消息，
 * 结合 {@link io.github.atengk.service.WebSocketBizHandler}
 * 实现按业务分发处理。
 * </p>
 *
 * @author 孔余
 * @since 2026-01-30
 */
@Getter
public enum WebSocketBizCode {

    /**
     * 发送聊天消息
     */
    CHAT_SEND("CHAT_SEND", "发送聊天消息"),

    /**
     * 接收聊天消息
     */
    CHAT_RECEIVE("CHAT_RECEIVE", "接收聊天消息"),

    /**
     * 通知推送
     */
    NOTICE_PUSH("NOTICE_PUSH", "通知推送"),

    /**
     * 通知确认
     */
    NOTICE_ACK("NOTICE_ACK", "通知确认");

    /**
     * 业务编码
     */
    private final String code;

    /**
     * 业务描述
     */
    private final String desc;

    WebSocketBizCode(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 根据编码获取业务枚举
     *
     * @param code 业务编码
     * @return 业务枚举，未匹配返回 null
     */
    public static WebSocketBizCode fromCode(String code) {
        for (WebSocketBizCode bizCode : values()) {
            if (bizCode.code.equals(code)) {
                return bizCode;
            }
        }
        return null;
    }
}
