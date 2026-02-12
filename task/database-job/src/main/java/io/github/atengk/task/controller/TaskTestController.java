package io.github.atengk.task.controller;

import io.github.atengk.task.executor.TaskExecutor;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/task")
@RequiredArgsConstructor
public class TaskTestController {

    private final TaskExecutor taskExecutor;

    @GetMapping("/execute")
    public String execute(@RequestParam String code) {
        taskExecutor.executeByCode(code);
        return "执行完成";
    }

}
