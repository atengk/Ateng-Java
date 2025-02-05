package local.ateng.java.websocket.config;

import local.ateng.java.websocket.handler.MyWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class WebSocketConfig implements WebSocketConfigurer {
    private final MyWebSocketHandler myWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // /ws路径的WebSocket
        registry.addHandler(myWebSocketHandler, "/ws").setAllowedOrigins("*");
        // /ws/demo路径的WebSocket
        registry.addHandler(myWebSocketHandler, "/ws/demo").setAllowedOrigins("*");
    }
}
