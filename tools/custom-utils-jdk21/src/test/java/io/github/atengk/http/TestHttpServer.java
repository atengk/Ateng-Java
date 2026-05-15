package io.github.atengk.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

class TestHttpServer implements AutoCloseable {
    private final HttpServer server;

    TestHttpServer() throws IOException {
        this.server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        this.server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
    }

    String url(String path) {
        return "http://127.0.0.1:" + server.getAddress().getPort() + path;
    }

    TestHttpServer context(String path, Consumer<HttpExchange> handler) {
        server.createContext(path, exchange -> {
            try {
                handler.accept(exchange);
            } finally {
                exchange.close();
            }
        });
        return this;
    }

    TestHttpServer text(String path, String response) {
        return context(path, exchange -> send(exchange, 200, "text/plain; charset=UTF-8", response));
    }

    void start() {
        server.start();
    }

    @Override
    public void close() {
        server.stop(0);
    }

    static void send(HttpExchange exchange, int status, String contentType, String body) {
        try {
            byte[] bytes = body == null ? new byte[0] : body.getBytes(StandardCharsets.UTF_8);
            if (contentType != null) {
                exchange.getResponseHeaders().add("Content-Type", contentType);
            }
            exchange.sendResponseHeaders(status, bytes.length);
            if (!"HEAD".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.getResponseBody().write(bytes);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
