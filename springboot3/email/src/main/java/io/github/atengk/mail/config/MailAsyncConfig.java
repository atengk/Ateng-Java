package io.github.atengk.mail.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 邮件异步线程池配置。
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Configuration
public class MailAsyncConfig {

    /**
     * 邮件发送专用线程池。
     *
     * @return 邮件任务执行器
     */
    @Bean("mailTaskExecutor")
    public Executor mailTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 核心线程数：适合常规项目，避免邮件发送阻塞主业务线程
        executor.setCorePoolSize(4);

        // 最大线程数：突发批量发送时临时扩容
        executor.setMaxPoolSize(12);

        // 队列容量：根据项目邮件峰值调整，避免无限堆积
        executor.setQueueCapacity(500);

        // 空闲线程存活时间
        executor.setKeepAliveSeconds(60);

        // 线程名前缀，方便日志排查
        executor.setThreadNamePrefix("mail-send-");

        // 关闭应用时等待任务完成
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        // 队列满时由调用线程执行，形成反压，避免任务静默丢失
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        executor.initialize();

        log.info("邮件异步线程池初始化完成，corePoolSize={}，maxPoolSize={}，queueCapacity={}",
                executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());

        return executor;
    }
}
