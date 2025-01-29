package local.ateng.java.http.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@RestController("/web-client")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class WebClientController {
    private final WebClient.Builder webClientBuilder;

    /**
     * 发起异步请求
     * 异步请求是 WebClient 的核心特性，适合高并发场景。
     * 当你发起请求时，WebClient 会返回一个 Mono（表示单个值）或者 Flux（表示多个值），可以进一步链式调用其他操作。
     *
     * @return
     */
    @GetMapping("/async")
    public Mono<String> getDataAsync() {
        WebClient webClient = webClientBuilder.baseUrl("https://jsonplaceholder.typicode.com")
                .build();
        return webClient.get()
                .uri("/posts/1")
                .retrieve()
                .bodyToMono(String.class);
    }

}

