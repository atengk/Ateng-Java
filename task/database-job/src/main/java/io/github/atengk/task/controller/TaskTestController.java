package io.github.atengk.task.controller;

import io.github.atengk.task.entity.TaskJob;
import io.github.atengk.task.executor.TaskExecutor;
import io.github.atengk.task.service.ITaskJobService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/task")
@RequiredArgsConstructor
public class TaskTestController {

    private final TaskExecutor taskExecutor;
    private final ITaskJobService taskJobService;

    @GetMapping("/executeByCode")
    public String executeByCode(@RequestParam String code) {
        taskExecutor.executeByCode(code);
        return "执行完成";
    }

    @GetMapping("/executeByBizType")
    public String executeByBizType() {
        taskExecutor.executeByBizType("TEST_BIZ_ORDER");
        return "执行完成";
    }

    @PostMapping("/createJob")
    public String createJob() {

        TaskJob taskJob = new TaskJob();

        // 生成唯一任务编码（无-的UUID）
        taskJob.setJobCode(cn.hutool.core.util.IdUtil.fastSimpleUUID());

        // 任务信息
        taskJob.setJobName("创建订单任务");
        taskJob.setJobDesc("创建订单任务（详情）");

        // 业务类型
        taskJob.setBizType("TEST_BIZ_ORDER");
        // 业务ID（可选）
        taskJob.setBizId("TEST_BIZ_ORDER");

        // Spring 容器中的 Bean 名称
        taskJob.setBeanName("orderTaskService");

        // 要执行的方法
        taskJob.setMethodName("syncOrder");

        // 参数类型
        java.util.List<String> paramTypes =
                cn.hutool.core.collection.CollUtil.newArrayList(
                        "java.lang.Long",
                        "java.lang.String"
                );

        taskJob.setMethodParamTypes(
                cn.hutool.json.JSONUtil.toJsonStr(paramTypes)
        );

        // 参数值
        java.util.List<Object> params =
                cn.hutool.core.collection.CollUtil.newArrayList(
                        123,
                        "hello"
                );

        taskJob.setMethodParams(
                cn.hutool.json.JSONUtil.toJsonStr(params)
        );

        // 执行时间
        taskJob.setNextExecuteTime(java.time.LocalDateTime.now());

        // 重试相关
        taskJob.setRetryCount(0);
        taskJob.setMaxRetryCount(3);
        taskJob.setRetryIntervalSeconds(60);

        taskJobService.save(taskJob);

        return "创建成功，jobCode=" + taskJob.getJobCode();
    }


}
