package io.github.atengk.restclient;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.HashMap;
import java.util.Map;

class RestClientApplicationTests {

    /**
     * GET 请求
     */
    @Test
    void test() {
        RestClient client = RestClient.create();

        String url = "https://jsonplaceholder.typicode.com/posts/1";
        ResponseEntity<String> response = client.get()
                .uri(url)
                .retrieve()
                .toEntity(String.class);

        System.out.println(response.getBody());
    }

    /**
     * GET 请求（带参数）
     */
    @Test
    void test1() {
        Map<String, Object> params = new HashMap<>();
        params.put("userId", 1);

        String result = RestClient.create()
                .get()
                .uri("https://jsonplaceholder.typicode.com/posts?userId={userId}", params)
                .retrieve()
                .body(String.class);

        System.out.println(result);
    }

    /**
     * POST 请求（提交 JSON）
     */
    @Test
    void test2() {
        Map<String, Object> body = new HashMap<>();
        body.put("title", "Spring Boot 3 RestClient");
        body.put("body", "This is a test post");
        body.put("userId", 1);

        String result = RestClient.create()
                .post()
                .uri("https://jsonplaceholder.typicode.com/posts")
                .body(body)
                .retrieve()
                .body(String.class);

        System.out.println(result);
    }

    /**
     * PUT 请求（更新资源）
     */
    @Test
    void test3() {
        Map<String, Object> body = new HashMap<>();
        body.put("title", "Updated Title");

        ResponseEntity<Void> response = RestClient.create()
                .put()
                .uri("https://jsonplaceholder.typicode.com/posts/{id}", 1)
                .body(body)
                .retrieve()
                .toBodilessEntity();

        System.out.println(response.getStatusCode());
    }

    /**
     * DELETE 请求
     */
    @Test
    void test4() {
        Map<String, Object> body = new HashMap<>();
        body.put("title", "Spring Boot 3 RestClient");
        body.put("body", "This is a test post");
        body.put("userId", 1);

        ResponseEntity<Void> response = RestClient.create()
                .delete()
                .uri("https://jsonplaceholder.typicode.com/posts/{id}", 1)
                .retrieve()
                .toBodilessEntity();

        System.out.println(response.getStatusCode());
    }

    /**
     * Header 与 Query 参数设置
     */
    @Test
    void test5() {
        String result = RestClient.builder()
                .defaultHeader("Authorization", "Bearer 123456")
                .build()
                .get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host("jsonplaceholder.typicode.com")
                        .path("/posts")
                        .queryParam("page", 1)
                        .queryParam("size", 10)
                        .build())
                .retrieve()
                .body(String.class);

        System.out.println(result);
    }

    /**
     * 自定义错误处理
     */
    @Test
    void test6() {
        try {
            RestClient.create()
                    .get()
                    .uri("https://api.example.com/error")
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException e) {
            System.out.println("Error: " + e.getStatusText());
            System.out.println("Body: " + e.getResponseBodyAsString());
        } catch (RestClientException e) {
            System.out.println("Connection Error: " + e.getMessage());
        }
    }


}
