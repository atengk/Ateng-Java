package io.github.atengk.pool.controller;

import io.github.atengk.pool.service.DemoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/demo")
@RequiredArgsConstructor
public class DemoController {
    private final DemoService demoService;

    @GetMapping("/submitTasks")
    public void submitTasks() {
        demoService.submitTasks();
    }

    @GetMapping("/asyncVoidTask")
    public void asyncVoidTask() {
        demoService.asyncVoidTask();
    }

    @GetMapping("/asyncFutureTask")
    public void asyncFutureTask() {
        CompletableFuture<String> completableFuture = demoService.asyncFutureTask();
        completableFuture.thenAccept(System.out::println);
    }

}


