package io.github.atengk.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 群消息 DTO
 *
 * <p>
 * 表示客户端向指定群组发送的一条消息，
 * 通常由服务端转发给群内所有在线成员。
 * </p>
 *
 * @author 孔余
 * @since 2026-01-30
 */
@Data
public class GroupMessageDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 群 ID
     */
    private String groupId;

    /**
     * 消息内容
     */
    private String content;
}
