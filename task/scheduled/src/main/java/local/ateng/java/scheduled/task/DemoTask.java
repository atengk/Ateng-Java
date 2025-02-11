package local.ateng.java.scheduled.task;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 定时任务示例
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-02-11
 */
@Component
@Slf4j
public class DemoTask {

    /**
     * 固定间隔时间：任务会按照指定的固定时间间隔执行，并且程序启动立即运行一次
     */
    @Scheduled(fixedRate = 5000) // 每隔 5 秒执行一次
    @Async
    public void fixedRateTask() {
        log.info("[固定间隔时间] 执行任务...，线程：{}", Thread.currentThread());
    }

    /**
     * 固定延迟时间：任务在执行完成后，等待指定的延迟时间再开始下一次执行，并且程序启动立即运行一次
     */
    @Scheduled(fixedDelay = 5000) // 上一个任务执行完后 5 秒执行下一次
    @Async
    public void fixedDelayTask() {
        log.info("[固定延迟时间] 执行任务...");
    }

    /**
     * 延迟执行：任务在启动时延迟指定时间后执行
     */
    @Scheduled(initialDelay = 10000, fixedRate = 5000) // 10 秒后开始执行，之后每隔 5 秒执行一次
    @Async
    public void initialDelayTask() {
        log.info("[延迟执行][固定间隔时间] 执行任务...");
    }

    /**
     * Cron 表达式
     */
    @Scheduled(cron = "0 0/5 9-17 * * MON-FRI") // 每周一到周五，9:00 到 17:00 每隔 5 分钟执行一次
    @Async
    public void cronTask() {
        log.info("[Cron 表达式] 执行任务...");
    }

}
