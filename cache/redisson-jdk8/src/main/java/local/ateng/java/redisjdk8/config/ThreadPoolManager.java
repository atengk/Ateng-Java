package local.ateng.java.redisjdk8.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.ObjectUtils;

import java.util.Map;
import java.util.concurrent.*;

/**
 * 全局线程池管理器
 *
 * <p>
 * 1. 自动从 ThreadPoolProperties 中读取线程池配置<br>
 * 2. 动态向 Spring 容器注册多个 ThreadPoolTaskExecutor Bean<br>
 * 3. 支持 @Async 的默认线程池选择（第一个配置项）<br>
 * 4. 可通过 getExecutor(name) 按名称获取线程池实例<br>
 * </p>
 *
 * @author 孔余
 * @since 2025-11-18
 */
@Slf4j
@Configuration
@EnableAsync
@ConditionalOnProperty(prefix = "thread-pool", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ThreadPoolManager implements AsyncConfigurer, ApplicationContextAware, SmartLifecycle {

    private final ThreadPoolProperties properties;
    private BeanDefinitionRegistry beanDefinitionRegistry;
    private final ApplicationContext applicationContext;

    public ThreadPoolManager(ThreadPoolProperties properties, ApplicationContext applicationContext) {
        this.properties = properties;
        this.applicationContext = applicationContext;
    }

    /**
     * ApplicationContext 初始化回调，用于获取 BeanDefinitionRegistry。
     * 并在容器刷新时完成所有线程池 Bean 的动态注册。
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        if (applicationContext instanceof BeanDefinitionRegistry) {
            this.beanDefinitionRegistry = (BeanDefinitionRegistry) applicationContext;
        } else if (applicationContext.getAutowireCapableBeanFactory() instanceof BeanDefinitionRegistry) {
            this.beanDefinitionRegistry =
                    (BeanDefinitionRegistry) applicationContext.getAutowireCapableBeanFactory();
        } else {
            throw new IllegalStateException("无法从 ApplicationContext 中获取 BeanDefinitionRegistry，无法动态注册线程池 Bean");
        }
        // 注册线程池Bean
        registerAllPools();
    }

    /**
     * 动态注册所有线程池 Bean
     *
     * <p>
     * 根据配置文件中的 thread-pool.pools 内容，
     * 循环注册每个 key 为线程池名的 ThreadPoolTaskExecutor Bean。
     * </p>
     */
    private void registerAllPools() {
        Map<String, ThreadPoolProperties.PoolConfig> pools = properties.getPools();
        if (pools == null || pools.isEmpty()) {
            log.warn("线程池配置为空：未找到 thread-pool.pools 配置项，当前系统不会注册任何线程池");
            return;
        }

        log.info("开始注册线程池，共计 {} 个", pools.size());

        for (Map.Entry<String, ThreadPoolProperties.PoolConfig> entry : pools.entrySet()) {
            String name = entry.getKey();
            ThreadPoolProperties.PoolConfig config = entry.getValue();

            if (beanDefinitionRegistry.containsBeanDefinition(name)) {
                log.warn("线程池 Bean [{}] 已存在，跳过注册", name);
                continue;
            }

            BeanDefinitionBuilder builder =
                    BeanDefinitionBuilder.genericBeanDefinition(ThreadPoolTaskExecutor.class,
                            () -> {
                                ThreadPoolTaskExecutor executor = createExecutor(config);
                                executor.initialize();
                                return executor;
                            });

            beanDefinitionRegistry.registerBeanDefinition(name, builder.getBeanDefinition());
            log.info("成功注册线程池 Bean：{}", name);
        }
    }

    /**
     * @Async 默认线程池选择
     *
     * <p>
     * 默认获取配置文件中第一条线程池配置，作为 @Async 未指定名称时的执行线程池。
     * </p>
     */
    @Override
    public Executor getAsyncExecutor() {
        // 如果配置不使用第一个作为默认线程池
        if (!properties.isUseFirstAsDefault()) {
            log.info("未配置默认线程池 @Async，使用 Spring 默认 AsyncExecutor");
            return new SimpleAsyncTaskExecutor();
        }

        Map<String, ThreadPoolProperties.PoolConfig> pools = properties.getPools();
        if (pools == null || pools.isEmpty()) {
            log.warn("未注册任何线程池，使用 Spring 默认 AsyncExecutor");
            return new SimpleAsyncTaskExecutor();
        }

        String defaultPoolName = pools.keySet().iterator().next();
        log.info("默认 @Async 执行线程池：{}", defaultPoolName);

        return getExecutor(defaultPoolName);
    }

    /**
     * 异步执行出现未捕获异常时的处理器
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) ->
                log.error("异步方法执行异常，方法：{}，参数：{}，异常信息：{}",
                        method.getName(), params, ex.getMessage(), ex);
    }

    /**
     * 根据 PoolConfig 构建 ThreadPoolTaskExecutor
     */
    private ThreadPoolTaskExecutor createExecutor(ThreadPoolProperties.PoolConfig config) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(config.getCoreSize());
        executor.setMaxPoolSize(config.getMaxSize());
        executor.setQueueCapacity(config.getQueueCapacity());
        executor.setKeepAliveSeconds(config.getKeepAliveSeconds());
        executor.setThreadNamePrefix(config.getThreadNamePrefix());
        executor.setAllowCoreThreadTimeOut(config.isAllowCoreThreadTimeout());
        executor.setRejectedExecutionHandler(resolveRejectedHandler(config.getRejectedPolicy()));
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(config.getAwaitTerminationSeconds());
        return executor;
    }

    /**
     * 字符串解析拒绝策略
     */
    private RejectedExecutionHandler resolveRejectedHandler(String policy) {
        if ("callerRuns".equalsIgnoreCase(policy)) {
            return new ThreadPoolExecutor.CallerRunsPolicy();
        }
        if ("discard".equalsIgnoreCase(policy)) {
            return new ThreadPoolExecutor.DiscardPolicy();
        }
        if ("discardOldest".equalsIgnoreCase(policy)) {
            return new ThreadPoolExecutor.DiscardOldestPolicy();
        }

        log.warn("拒绝策略 [{}] 未识别，将使用默认 AbortPolicy", policy);
        return new ThreadPoolExecutor.AbortPolicy();
    }

    /**
     * 根据线程池名称获取对应的 ThreadPoolTaskExecutor 实例
     *
     * @param name 线程池 Bean 名称
     */
    public ThreadPoolTaskExecutor getExecutor(String name) {
        if (!applicationContext.containsBean(name)) {
            throw new IllegalArgumentException(
                    "线程池不存在：" + name + "。请检查配置 thread-pool.pools 是否正确，或确认名称是否一致");
        }
        return applicationContext.getBean(name, ThreadPoolTaskExecutor.class);
    }

    /**
     * 优雅关闭所有线程池。
     * 根据配置文件中的线程池名称依次关闭对应线程池实例。
     */
    public void shutdownAllPools() {
        Map<String, ThreadPoolProperties.PoolConfig> pools = properties.getPools();
        if (ObjectUtils.isEmpty(pools)) {
            log.warn("线程池关闭跳过，因为未检测到任何线程池配置");
            return;
        }

        log.info("开始关闭所有线程池，共计 {} 个", pools.size());

        for (Map.Entry<String, ThreadPoolProperties.PoolConfig> entry : pools.entrySet()) {
            String poolName = entry.getKey();
            ThreadPoolTaskExecutor executor;

            try {
                executor = getExecutor(poolName);
            } catch (Exception ex) {
                log.error("关闭线程池时发现线程池不存在，名称 {}", poolName);
                continue;
            }

            if (executor == null) {
                log.warn("线程池实例为空，名称 {}", poolName);
                continue;
            }

            shutdownExecutor(poolName, executor);
        }

        log.info("所有线程池关闭完成");
    }

    /**
     * 关闭单个线程池，等待任务执行完毕后关闭。
     *
     * @param poolName 线程池名称
     * @param executor 线程池实例
     */
    private void shutdownExecutor(String poolName, ThreadPoolTaskExecutor executor) {
        ExecutorService es = executor.getThreadPoolExecutor();

        log.info("准备关闭线程池 {}", poolName);

        executor.shutdown();

        try {
            boolean terminated = es.awaitTermination(30, TimeUnit.SECONDS);
            if (terminated) {
                log.info("线程池 {} 已在超时时间内优雅关闭", poolName);
            } else {
                log.warn("线程池 {} 未在指定时间内停止，将强制关闭", poolName);
                es.shutdownNow();
            }
        } catch (InterruptedException ex) {
            log.error("线程池 {} 在等待关闭过程中被中断，将执行强制关闭", poolName);
            es.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }


    @Override
    public void start() {
    }

    @Override
    public void stop() {
        // 关闭线程池
        shutdownAllPools();
    }

    @Override
    public boolean isRunning() {
        return true;
    }

    @Override
    public int getPhase() {
        // 优先级高于 Tomcat 等 Web 容器
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }

}
