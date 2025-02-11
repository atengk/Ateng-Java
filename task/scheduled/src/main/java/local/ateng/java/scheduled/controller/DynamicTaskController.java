package local.ateng.java.scheduled.controller;

import local.ateng.java.scheduled.service.DynamicTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/schedule")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class DynamicTaskController {
    private final DynamicTaskService taskService;

    @PostMapping("/startFixedRate")
    public String startFixedRateTask(long startTime, long period) {
        taskService.startFixedRateTask(startTime, period);
        return "固定间隔任务启动成功，初始时间：" + startTime + "秒，周期：" + period + " 秒";
    }

    @PostMapping("/startCron")
    public String startCronTask(String cron) {
        taskService.startCronTask(cron);
        return "CRON 任务启动成功，表达式：" + cron;
    }

    @DeleteMapping("/stop")
    public String stopTask() {
        taskService.stopTask();
        return "任务已停止";
    }

    @GetMapping("/status")
    public Boolean getTaskStatus() {
        return taskService.getTaskStatus();
    }

}