package local.ateng.java.http.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class WebClientService {

    private final WebClient.Builder webClientBuilder;

    public String getDataFromApi() {
        WebClient webClient = webClientBuilder.baseUrl("https://jsonplaceholder.typicode.com")
                .build();

        return webClient.get()
                .uri("/posts/1") // 请求路径
                .retrieve() // 发起请求并获取响应
                .bodyToMono(String.class) // 响应体转换为 String
                .block(); // 阻塞直到响应返回
    }

    public String getDataFromApiHeader() {
        WebClient webClient = webClientBuilder.baseUrl("https://jsonplaceholder.typicode.com")
                .build();

        return webClient.get()
                .uri("/posts/1")
                .header("Authorization", "Bearer token")
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    public String getDataFromApiParam() {
        WebClient webClient = webClientBuilder.baseUrl("https://jsonplaceholder.typicode.com")
                .build();

        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/posts")
                        .queryParam("userId", 2)
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    public String getDataFromApiOnError() {
        WebClient webClient = webClientBuilder.baseUrl("https://jsonplaceholder.typicode.com")
                .build();

        return webClient.get()
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
    }

}

