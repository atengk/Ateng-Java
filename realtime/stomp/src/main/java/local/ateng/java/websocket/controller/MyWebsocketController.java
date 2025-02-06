package local.ateng.java.websocket.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/websocket")
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class MyWebsocketController {
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 广播消息
     * 客户端发送给/app/public消息，服务端通过/topic/public广播给所有订阅该路径的客户端
     *
     * @param message
     * @return
     */
    @MessageMapping("/public")
    @SendTo("/topic/public")
    public String sendToAll(String message) {
        log.info("客户端发送了一条消息: {}", message);
        return "Hello, " + message + "!";
    }

    /**
     * 点对点消息
     * 发送消息给指定用户
     *
     * @param userId
     * @param message
     */
    @MessageMapping("/user/{userId}")
    public void sendToUser(@DestinationVariable String userId, String message) {
        log.info("客户端用户[{}]发送了一条消息: {}", userId, message);
        // 表示消息将被发送到当前用户的队列 /user/{userId}/queue/user
        messagingTemplate.convertAndSendToUser(userId, "/queue/user", "Hello, " + message + "!");
    }

    /**
     * 发送消息给指定用户组
     *
     * @param message
     */
    @GetMapping("/sendToUserGroup")
    public void sendToUserGroup(String message) {
        List<String> list = List.of("10001", "10002", "10003");
        list.forEach(userId -> {
            // 表示消息将被发送到当前用户的队列 /user/{userId}/queue/user
            messagingTemplate.convertAndSendToUser(userId, "/queue/user", "Hello, " + message + "!");
        });
    }

}
