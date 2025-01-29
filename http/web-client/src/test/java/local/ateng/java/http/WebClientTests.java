package local.ateng.java.http;

import io.netty.channel.ChannelOption;
import local.ateng.java.http.service.WebClientService;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@SpringBootTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class WebClientTests {
    private final WebClient.Builder webClientBuilder;

    @Test
    public void test01() {
        // 发起同步请求（阻塞）
        WebClient webClient = webClientBuilder.baseUrl("https://jsonplaceholder.typicode.com")
                .build();
        String result = webClient.get()
                .uri("/posts/1") // 请求路径
                .retrieve() // 发起请求并获取响应
                .bodyToMono(String.class) // 响应体转换为 String
                .block();// 阻塞直到响应返回
        System.out.println(result);
    }

    @Test
    public void test02() {
        // 请求参数
        WebClient webClient = webClientBuilder.baseUrl("https://jsonplaceholder.typicode.com")
                .build();
        String result = webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/posts")
                        .queryParam("userId", 2)
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .block();
        System.out.println(result);
    }

    @Test
    public void test03() {
        // 错误处理
        WebClient webClient = webClientBuilder.baseUrl("https://jsonplaceholder.typicode.com")
                .build();
        String result = webClient.get()
                .uri("/posts/999")  // Non-existent resource
                .retrieve()
                .onStatus(status -> status.value() == 404, clientResponse -> {
                    // 处理 404 错误
                    return Mono.error(new RuntimeException("Resource not found"));
                })
                .bodyToMono(String.class)
                .onErrorResume(e -> {
                    // 其他错误处理
                    return Mono.just("An error occurred: " + e.getMessage());
                })
                .block();
        System.out.println(result);
    }

    @Test
    public void test04() {
        WebClient webClient = webClientBuilder
                .baseUrl("https://jsonplaceholder.typicode.com")
                .clientConnector(new ReactorClientHttpConnector(
                        HttpClient.create()
                                .responseTimeout(Duration.ofSeconds(5))  // 设置超时
                                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000))) // 设置连接超时
                .build();
        String result = webClient.get()
                .uri("/posts/1")
                .retrieve()
                .bodyToMono(String.class)
                .block();
        System.out.println(result);
    }


}
