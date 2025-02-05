package local.ateng.java.websocket.controller;

import local.ateng.java.websocket.service.WebSocketService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashSet;
import java.util.List;

@RestController
@RequestMapping("/ws")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class WebSocketController {
    private final WebSocketService webSocketService;

    @GetMapping("/sendToAll")
    public void sendToAll(String path, String message) {
        webSocketService.sendToAll(path, message);
    }

    @GetMapping("/sendToGroup")
    public void sendToGroup(String path, String userIds, String message) {
        webSocketService.sendToGroup(path, new HashSet<>(List.of(userIds.split(","))), message);
    }

    @GetMapping("/sendToUser")
    public void sendToUser(String path, String userId, String message) {
        webSocketService.sendToUser(path, userId, message);
    }
}
