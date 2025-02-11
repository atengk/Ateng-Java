package local.ateng.java.scheduled.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.SimpleAsyncTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ScheduledFuture;

@Service
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class DynamicTaskService {
    private final SimpleAsyncTaskScheduler taskScheduler;
    private ScheduledFuture<?> currentTask;

    @EventListener(ApplicationReadyEvent.class)
    @Async
    public void init() {
        // 设置任务默认时间
        this.startCronTask("0/5 * * * * ?");
    }

    /**
     * 启动一个定时任务（固定间隔时间）
     */
    public void startFixedRateTask(long startTime, long period) {
        stopTask(); // 先停止当前任务
        Runnable task = () -> executeTask();
        currentTask = taskScheduler.scheduleAtFixedRate(task, Instant.now().plusSeconds(startTime), Duration.ofSeconds(period));
        log.info("任务已启动！执行周期：{} 秒", period);
    }

    /**
     * 启动一个 CRON 任务
     */
    public void startCronTask(String cronExpression) {
        stopTask(); // 先停止当前任务
        Runnable task = () -> executeTask();
        currentTask = taskScheduler.schedule(task, new CronTrigger(cronExpression));
        log.info("任务已启动！CRON 表达式：{}", cronExpression);
    }

    /**
     * 执行任务内容
     */
    public void executeTask() {
        log.info("任务开始执行，线程：{}", Thread.currentThread());
        try {
            // 模拟任务执行
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        log.info("执行完成！线程：{}", Thread.currentThread());
    }

    /**
     * 停止当前任务
     */
    public void stopTask() {
        if (currentTask != null && !currentTask.isCancelled()) {
            currentTask.cancel(false);
            log.info("当前任务已停止！");
        }
    }

    /**
     * 获取当前任务状态
     */
    public Boolean getTaskStatus() {
        if (currentTask == null || currentTask.isCancelled()) {
            return false;
        }
        return true;
    }
}
