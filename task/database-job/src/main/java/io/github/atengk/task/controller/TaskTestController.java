package io.github.atengk.task.controller;

import io.github.atengk.task.entity.TaskJob;
import io.github.atengk.task.executor.TaskExecutor;
import io.github.atengk.task.service.ITaskJobService;
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
    private final ITaskJobService taskJobService;

    @GetMapping("/execute")
    public String execute(@RequestParam String code) {
        TaskJob taskJob = taskJobService
                .lambdaQuery()
                .eq(TaskJob::getJobCode, code)
                .one();
        taskExecutor.execute(taskJob);
        return "执行完成";
    }

}
