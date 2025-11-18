package local.ateng.java.async.config;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.lang.reflect.Method;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 通用线程池配置（企业级）
 * <p>
 * 说明：
 * 1. 适用于 SpringBoot2 的所有 @Async 异步任务
 * 2. 兼顾 CPU / IO 混合型任务的常规业务场景
 * 3. 包含安全关闭、线程超时回收、拒绝策略、异常日志捕获等功能
 *
 * @author Ateng
 * @since 2025-03-03
 */
@Slf4j
@Configuration
public class ThreadPoolConfig implements AsyncConfigurer {

    /**
     * CPU 核数，用于计算线程池默认大小。
     */
    private final int cpu = Runtime.getRuntime().availableProcessors();

    private ThreadPoolTaskExecutor executor;

    /**
     * 通用业务线程池。
     *
     * @return ThreadPoolTaskExecutor
     */
    @Bean
    @Primary
    public ThreadPoolTaskExecutor taskExecutor() {
        executor = new ThreadPoolTaskExecutor();

        /*
         * 线程数量配置
         * 通用线程池通常包含 IO 任务，因此适度放大核心线程数。
         */
        executor.setCorePoolSize(cpu * 2);
        executor.setMaxPoolSize(cpu * 4);

        /*
         * 队列容量
         * 容量可根据业务量调整，2000 足以覆盖大多数场景。
         */
        executor.setQueueCapacity(2000);

        /*
         * 空闲线程存活时间（秒）
         * 允许核心线程超时销毁，可降低系统闲时资源占用。
         */
        executor.setKeepAliveSeconds(60);
        executor.setAllowCoreThreadTimeOut(true);

        /*
         * 线程名称前缀，方便排查线程池问题。
         */
        executor.setThreadNamePrefix("async-common-");

        /*
         * 拒绝策略
         * CallerRunsPolicy：任务由调用线程执行，保证不丢任务。
         */
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        /*
         * 线程池关闭策略
         */
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);

        executor.initialize();
        log.info("【ThreadPool】通用线程池初始化完成，核心线程：{}，最大线程：{}", cpu * 2, cpu * 4);

        return executor;
    }

    /**
     * 捕获 @Async void 方法的未捕获异常。
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new AsyncUncaughtExceptionHandler() {
            @Override
            public void handleUncaughtException(Throwable ex, Method method, Object... params) {
                log.error("【Async Exception】方法：{}，异常：{}", method.getName(), ex.getMessage(), ex);
            }
        };
    }

    /**
     * 应用关闭时安全销毁线程池。
     */
    @PreDestroy
    public void destroy() {
        if (executor != null) {
            log.info("【ThreadPool】通用线程池正在关闭...");
            executor.shutdown();
        }
    }
}
