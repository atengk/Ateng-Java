package io.github.atengk.http;

import io.github.atengk.utils.http.HttpUtil;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.WebSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;

import static org.junit.jupiter.api.Assertions.*;

class HttpWebSocketSseTest {

    @Test
    void shouldReadSseMessages() throws Exception {
        try (TestHttpServer server = new TestHttpServer()) {
            server.context("/sse", exchange -> TestHttpServer.send(exchange, 200, "text/event-stream", "data: hello\n\ndata: world\n\n")).start();
            List<String> messages = new ArrayList<>();
            HttpUtil.readSse(server.url("/sse"), HttpUtil.defaultRequestOptions(), messages::add);
            assertEquals(List.of("hello", "world"), messages);
        }
    }

    @Test
    void shouldCreateWebSocketFutureType() {
        WebSocket.Listener listener = new WebSocket.Listener() {
            @Override
            public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                webSocket.request(1);
                return null;
            }
        };
        assertThrows(Exception.class, () -> HttpUtil.webSocket(new URI("ws://127.0.0.1:1/ws"), listener).join());
    }
}
