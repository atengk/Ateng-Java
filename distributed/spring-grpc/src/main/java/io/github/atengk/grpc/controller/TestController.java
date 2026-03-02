package io.github.atengk.grpc.controller;

import io.github.atengk.grpc.client.GreeterClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    private final GreeterClient greeterClient;

    public TestController(GreeterClient greeterClient) {
        this.greeterClient = greeterClient;
    }

    @GetMapping("/hello")
    public String hello(@RequestParam String name) {
        return greeterClient.sayHello(name);
    }
}