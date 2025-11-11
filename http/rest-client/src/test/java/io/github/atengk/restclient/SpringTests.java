package io.github.atengk.restclient;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

@SpringBootTest
public class SpringTests {
    @Autowired
    private RestClient restClient;

    @Test
    void test() {
        ResponseEntity<String> response = restClient.get()
                .uri("/posts/1")
                .retrieve()
                .toEntity(String.class);

        System.out.println(response.getBody());
    }


}
