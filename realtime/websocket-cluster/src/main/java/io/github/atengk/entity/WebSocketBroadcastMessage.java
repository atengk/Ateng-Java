package io.github.atengk.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Set;

/**
 * WebSocket 集群广播消息实体
 *
 * <p>
 * 用于 WebSocket 集群节点之间通过 MQ 传递消息，
 * 消息由发送节点投递，接收节点根据消息内容
 * 决定是否向本地 WebSocket Session 转发。
 * </p>
 *
 * <p>
 * 典型使用场景：
 * <ul>
 *     <li>跨节点广播消息</li>
 *     <li>跨节点定向推送给指定用户</li>
 *     <li>集群内节点间消息同步</li>
 * </ul>
 * </p>
 *
 * <p>
 * 注意：
 * <ul>
 *     <li>消息仅用于节点间通信，不直接暴露给前端</li>
 *     <li>实际 WebSocket 发送内容由 payload 承载</li>
 * </ul>
 * </p>
 *
 * @author 孔余
 * @since 2026-01-30
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class WebSocketBroadcastMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 消息来源节点标识
     *
     * <p>
     * 用于区分消息的发送节点，
     * 接收节点可据此避免处理自身发送的消息。
     * </p>
     */
    private String fromNode;

    /**
     * 实际 WebSocket 消息内容
     *
     * <p>
     * 该字段为最终推送给 WebSocket 客户端的消息体，
     * 通常为 JSON 字符串。
     * </p>
     */
    private String payload;

    /**
     * 目标用户集合
     *
     * <p>
     * 当集合为空或为 {@code null} 时，
     * 表示广播给所有在线用户。
     * </p>
     *
     * <p>
     * 当集合不为空时，
     * 仅向指定 userId 对应的 WebSocket Session 推送。
     * </p>
     */
    private Set<String> targetUsers;
}
