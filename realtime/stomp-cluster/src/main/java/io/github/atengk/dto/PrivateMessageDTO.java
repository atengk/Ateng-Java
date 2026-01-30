package io.github.atengk.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 私聊消息 DTO
 *
 * <p>
 * 表示客户端向指定用户发送的一条私聊消息。
 * </p>
 *
 * @author 孔余
 * @since 2026-01-30
 */
@Data
public class PrivateMessageDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 接收方用户 ID
     */
    private String toUserId;

    /**
     * 消息内容
     */
    private String content;
}
