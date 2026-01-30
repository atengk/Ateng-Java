package io.github.atengk.controller;

import com.alibaba.fastjson2.JSONObject;
import io.github.atengk.dto.GroupMessageDTO;
import io.github.atengk.dto.PrivateMessageDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

/**
 * WebSocket 聊天消息控制器
 *
 * <p>职责说明：
 * <ul>
 *     <li>只处理业务级消息 SEND</li>
 *     <li>不处理心跳、不处理 CONNECT / SUBSCRIBE</li>
 *     <li>不信任客户端传入的 userId，一律使用 Principal</li>
 * </ul>
 *
 * <p>设计原则：
 * <ul>
 *     <li>安全校验前置（拦截器 + Controller 二次兜底）</li>
 *     <li>消息路由交由 Broker（RabbitMQ）完成</li>
 *     <li>Controller 本身保持无状态</li>
 * </ul>
 *
 * @author 孔余
 * @since 2026-01-30
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;

    /* ====================== 公共消息 ====================== */

    /**
     * 公共广播消息
     *
     * <p>说明：
     * <ul>
     *     <li>所有在线用户均可收到</li>
     *     <li>适用于公告、系统广播、弹幕等场景</li>
     * </ul>
     *
     * @param message   消息内容
     * @param principal 当前连接用户身份（来自 CONNECT 阶段绑定）
     */
    @MessageMapping("/public.send")
    public void sendPublicMessage(
            @Payload String message,
            Principal principal
    ) {
        String fromUserId = principal.getName();

        // 权限校验
        validatePublicSendPermission(fromUserId);

        log.info("【公共消息】from={} content={}", fromUserId, message);

        messagingTemplate.convertAndSend(
                "/topic/public",
                buildPublicMessage(fromUserId, message)
        );
    }

    /* ====================== 私聊消息 ====================== */

    /**
     * 私聊消息发送
     *
     * <p>说明：
     * <ul>
     *     <li>基于 user destination（/user/{id}/queue/**）</li>
     *     <li>消息由 Broker 精确投递到目标用户</li>
     * </ul>
     *
     * @param dto       私聊消息 DTO
     * @param principal 当前连接用户身份
     */
    @MessageMapping("/private.send")
    public void sendPrivateMessage(
            @Payload PrivateMessageDTO dto,
            Principal principal
    ) {
        String fromUserId = principal.getName();
        String toUserId = dto.getToUserId();

        // 业务级私聊权限校验
        validatePrivateChatPermission(fromUserId, toUserId);

        log.info("【私聊消息】from={} → to={} content={}",
                fromUserId, toUserId, dto.getContent());

        messagingTemplate.convertAndSendToUser(
                toUserId,
                "/queue/message",
                buildPrivateMessage(fromUserId, dto.getContent())
        );
    }

    /* ====================== 群聊消息 ====================== */

    /**
     * 群消息发送
     *
     * <p>说明：
     * <ul>
     *     <li>一个群对应一个 topic</li>
     *     <li>消息由 Broker 广播给所有订阅该群的成员</li>
     * </ul>
     *
     * @param dto       群消息 DTO
     * @param principal 当前连接用户身份
     */
    @MessageMapping("/group.send")
    public void sendGroupMessage(
            @Payload GroupMessageDTO dto,
            Principal principal
    ) {
        String fromUserId = principal.getName();
        String groupId = dto.getGroupId();

        // 业务级群权限校验（SUBSCRIBE 之外的二次兜底）
        validateGroupChatPermission(fromUserId, groupId);

        log.info("【群消息】group={} from={} content={}",
                groupId, fromUserId, dto.getContent());

        messagingTemplate.convertAndSend(
                "/topic/group." + groupId,
                buildGroupMessage(fromUserId, groupId, dto.getContent())
        );
    }

    /* ====================== 权限校验（示例） ====================== */

    /**
     * 校验用户是否有权限发送公共广播
     */
    private void validatePublicSendPermission(String userId) {
        // TODO: 真实项目调用 Service / DB / Redis
        if (!mockCanSendPublic(userId)) {
            log.warn("【公共消息】用户无权发送 userId={}", userId);
            throw new IllegalArgumentException("无权发送公共消息");
        }
    }

    /**
     * 模拟权限判断
     */
    private boolean mockCanSendPublic(String userId) {
        // 比如只有 userId=10001 可以发送
        return "10001".equals(userId);
    }

    /**
     * 私聊权限校验
     *
     * <p>真实场景可校验：
     * <ul>
     *     <li>是否好友</li>
     *     <li>是否被拉黑</li>
     *     <li>是否同租户 / 同组织</li>
     *     <li>是否允许陌生人私聊</li>
     * </ul>
     *
     * @param fromUserId 发送方用户 ID
     * @param toUserId   接收方用户 ID
     */
    private void validatePrivateChatPermission(String fromUserId, String toUserId) {

        if (fromUserId.equals(toUserId)) {
            throw new IllegalArgumentException("不能给自己发送私聊消息");
        }

        // mock 示例规则
        if ("10001".equals(fromUserId) && "10002".equals(toUserId)) {
            return;
        }

        log.warn("【私聊权限拒绝】from={} to={}", fromUserId, toUserId);
        throw new IllegalArgumentException("无权限向该用户发送私聊消息");
    }

    /**
     * 群聊权限校验
     *
     * <p>真实场景可校验：
     * <ul>
     *     <li>是否群成员</li>
     *     <li>是否被禁言</li>
     *     <li>群是否已解散</li>
     * </ul>
     *
     * @param userId  用户 ID
     * @param groupId 群 ID
     */
    private void validateGroupChatPermission(String userId, String groupId) {

        // mock 示例规则
        if ("group-001".equals(groupId) && ("10001".equals(userId) || "10002".equals(userId))) {
            return;
        }

        log.warn("【群聊权限拒绝】userId={} groupId={}", userId, groupId);
        throw new IllegalArgumentException("无权限向该群发送消息");
    }

    /* ====================== 消息构造 ====================== */

    /**
     * 构造公共 / 私聊消息体
     */
    private JSONObject buildPrivateMessage(String fromUserId, String content) {
        return JSONObject.of(
                "fromUserId", fromUserId,
                "content", content,
                "timestamp", System.currentTimeMillis()
        );
    }

    /**
     * 构造公共消息体
     */
    private JSONObject buildPublicMessage(String fromUserId, String content) {
        return JSONObject.of(
                "fromUserId", fromUserId,
                "content", content,
                "timestamp", System.currentTimeMillis()
        );
    }

    /**
     * 构造群消息体
     */
    private JSONObject buildGroupMessage(String fromUserId, String groupId, String content) {
        return JSONObject.of(
                "groupId", groupId,
                "fromUserId", fromUserId,
                "content", content,
                "timestamp", System.currentTimeMillis()
        );
    }
}
