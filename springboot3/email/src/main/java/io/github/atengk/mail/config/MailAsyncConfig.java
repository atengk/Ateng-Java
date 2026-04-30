package io.github.atengk.mail.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 邮件异步线程池配置
 *
 * @author Ateng
 * @since 2026-04-30
 */
@EnableAsync
@Configuration
public class MailAsyncConfig implements AsyncConfigurer {

    private static final Logger log = LoggerFactory.getLogger(MailAsyncConfig.class);

    /**
     * 邮件发送线程池
     *
     * @return 邮件发送线程池
     */
    @Bean("mailTaskExecutor")
    public ThreadPoolTaskExecutor mailTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 核心线程数，保持少量常驻线程处理日常邮件
        executor.setCorePoolSize(4);

        // 最大线程数，邮件发送高峰期最多扩展到 12 个线程
        executor.setMaxPoolSize(12);

        // 队列容量，避免瞬时大量邮件直接打满线程
        executor.setQueueCapacity(500);

        // 线程空闲存活时间，单位秒
        executor.setKeepAliveSeconds(60);

        // 线程名前缀，便于日志排查
        executor.setThreadNamePrefix("mail-send-");

        // 队列满时由提交任务的线程执行，避免任务直接丢失
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // 应用关闭时等待异步任务执行完成
        executor.setWaitForTasksToCompleteOnShutdown(true);

        // 最大等待时间，单位秒
        executor.setAwaitTerminationSeconds(30);

        executor.initialize();
        return executor;
    }

    /**
     * 默认异步执行器
     *
     * @return 异步执行器
     */
    @Override
    public Executor getAsyncExecutor() {
        return mailTaskExecutor();
    }

    /**
     * 异步异常处理器
     *
     * @return 异步异常处理器
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new MailAsyncExceptionHandler();
    }

    /**
     * 邮件异步异常处理器
     *
     * @author Ateng
     * @since 2026-04-30
     */
    private static class MailAsyncExceptionHandler implements AsyncUncaughtExceptionHandler {

        /**
         * 处理异步异常
         *
         * @param throwable 异常对象
         * @param method    方法对象
         * @param objects   方法参数
         */
        @Override
        public void handleUncaughtException(Throwable throwable, Method method, Object... objects) {
            log.error("邮件异步任务执行异常，方法：{}，原因：{}", method.getName(), throwable.getMessage(), throwable);
        }
    }
}