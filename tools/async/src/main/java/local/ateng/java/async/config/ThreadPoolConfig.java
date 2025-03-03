package local.ateng.java.async.config;

import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 线程池配置类
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-03-03
 */
@Configuration
public class ThreadPoolConfig {

    // CPU 核心数
    private final int core = Runtime.getRuntime().availableProcessors();

    private ThreadPoolTaskExecutor executor;
    private ThreadPoolTaskExecutor myTaskExecutor;

    @Value("${spring.threads.virtual.enabled:false}") // 默认值为 false
    private boolean virtualThreadsEnabled;

    @Bean
    public Executor taskPoolExecutor() {
        myTaskExecutor = new ThreadPoolTaskExecutor();
        // 线程名称前缀
        myTaskExecutor.setThreadNamePrefix("my-task-");
        // 初始化线程池
        myTaskExecutor.initialize();
        return myTaskExecutor;
    }

    /**
     * 自定义线程池
     *
     * @return 线程池执行器
     */
//    @Bean
//    @Primary
    public ThreadPoolTaskExecutor taskExecutor() {
        executor = new ThreadPoolTaskExecutor();

        // 线程名称前缀
        executor.setThreadNamePrefix("async-task-");

        // 判断是否开启虚拟线程
        if (virtualThreadsEnabled) {
            executor.setVirtualThreads(true);

        } else {
            // 核心线程数
            executor.setCorePoolSize(core);

            // 最大线程数（适当放大以应对高并发）
            executor.setMaxPoolSize(core * 2);

            // 任务队列大小
            executor.setQueueCapacity(core * 10);

            // 任务拒绝策略（由调用者线程执行，防止任务丢失）
            executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        }

        // 初始化线程池
        executor.initialize();

        return executor;
    }

    /**
     * 在 Spring 容器销毁时关闭线程池
     */
    @PreDestroy
    public void destroy() {
        if (executor != null) {
            executor.shutdown();
        }
    }

}
