# SpringBoot2 线程池配置和使用



## 编辑配置文件

### 添加一个线程池

```yaml
---
# 线程池配置
thread-pool:
  # 是否启用线程池功能
  enabled: true
  # 是否将第一个线程池作为默认 @Async 线程池
  use-first-as-default: false
  pools:
    # 默认线程池
    defaultPool:
      core-size: 8
      max-size: 16
      queue-capacity: 200
      keep-alive-seconds: 60
      thread-name-prefix: async-default-
      allow-core-thread-timeout: false
      rejected-policy: callerRuns
      await-termination-seconds: 30
```

### 添加多个线程池

```yaml
---
# 线程池配置
thread-pool:
  # 是否启用线程池功能
  enabled: true
  # 是否将第一个线程池作为默认 @Async 线程池
  use-first-as-default: false
  pools:
    # 默认线程池
    defaultPool:
      core-size: 8
      max-size: 16
      queue-capacity: 200
      keep-alive-seconds: 60
      thread-name-prefix: async-default-
      allow-core-thread-timeout: false
      rejected-policy: callerRuns
      await-termination-seconds: 30
    # CPU 密集型线程池
    cpuPool:
      core-size: 4
      max-size: 8
      queue-capacity: 100
      keep-alive-seconds: 30
      thread-name-prefix: cpu-exec-
      allow-core-thread-timeout: true
      rejected-policy: callerRuns
      await-termination-seconds: 30
    # IO 密集型线程池
    ioPool:
      core-size: 16
      max-size: 32
      queue-capacity: 1000
      keep-alive-seconds: 60
      thread-name-prefix: io-exec-
      allow-core-thread-timeout: false
      rejected-policy: callerRuns
      await-termination-seconds: 30
```

## 配置属性文件

```java
package io.github.atengk.pool.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 全局线程池配置属性类
 *
 * <p>
 * 通过 thread-pool 前缀绑定线程池配置，用于统一管理多个自定义线程池。
 * 所有线程池以 Map 形式存储，key 作为线程池名称，value 为线程池的参数配置。
 * 应用启动时由 ThreadPoolManager 动态注册对应的线程池 Bean。
 * </p>
 *
 * @author 孔余
 * @since 2025-11-18
 */
@Data
@Component
@ConfigurationProperties(prefix = "thread-pool")
public class ThreadPoolProperties {

    /**
     * 是否启用线程池功能，false 时不创建任何线程池
     */
    private boolean enabled = true;

    /**
     * 是否使用第一个线程池作为默认 @Async 线程池
     */
    private boolean useFirstAsDefault = false;

    /**
     * 线程池配置集合
     *
     * <p>
     * key 表示线程池名称，value 为对应的线程池参数配置。
     * 配置文件中按顺序定义的第一个线程池视为默认线程池。
     * </p>
     */
    private Map<String, PoolConfig> pools = new LinkedHashMap<>();

    /**
     * 单个线程池的详细参数配置
     *
     * <p>用于构建 ThreadPoolTaskExecutor 实例。</p>
     */
    @Data
    public static class PoolConfig {

        /**
         * 核心线程数
         *
         * <p>
         * 线程池维护的最小常驻线程数，即使空闲也不会被销毁。
         * </p>
         */
        private int coreSize;

        /**
         * 最大线程数
         *
         * <p>
         * 当任务量超过核心线程时允许扩容到的最大线程数。
         * </p>
         */
        private int maxSize;

        /**
         * 队列容量
         *
         * <p>
         * 任务队列的最大长度，用于存放待执行任务。
         * 队列已满时会触发拒绝策略。
         * </p>
         */
        private int queueCapacity;

        /**
         * 空闲线程最大存活时间（秒）
         *
         * <p>
         * 超出核心线程数的多余线程在空闲超过此时间后被回收。
         * </p>
         */
        private int keepAliveSeconds;

        /**
         * 线程名称前缀
         *
         * <p>
         * 用于区分线程池来源，便于排查日志。
         * 如：async-default-、io-pool-。
         * </p>
         */
        private String threadNamePrefix;

        /**
         * 是否允许核心线程超时回收
         *
         * <p>
         * 默认核心线程不会被回收，开启后可减少资源占用。
         * </p>
         */
        private boolean allowCoreThreadTimeout;

        /**
         * 线程池任务拒绝策略
         *
         * <p>当线程池无法接收新任务（队列已满且线程数达到最大值）时采用的处理策略。
         * 支持以下几种值：</p>
         *
         * <ul>
         *     <li><b>callerRuns</b> - 由调用线程执行该任务，既不会抛弃也不会抛异常，适合控制系统负载。</li>
         *     <li><b>abort</b> - 默认策略，抛出 {@link java.util.concurrent.RejectedExecutionException} 异常，表示任务提交失败。</li>
         *     <li><b>discard</b> - 丢弃当前任务，不抛异常。</li>
         *     <li><b>discardOldest</b> - 丢弃队列中最旧的任务，然后尝试提交当前任务。</li>
         * </ul>
         *
         * <p>注意：配置不识别的值将默认使用 <b>abort</b> 策略。</p>
         */
        private String rejectedPolicy;

        /**
         * 服务关闭前最大等待时间（秒）
         *
         * <p>
         * Spring 容器关闭时线程池等待任务完成的最长时间。
         * </p>
         */
        private int awaitTerminationSeconds;
    }
}

```

## 配置线程池管理器

```java
package io.github.atengk.pool.config;

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

```

## 使用异步任务

### 创建Service

```java
package io.github.atengk.pool.service;

import io.github.atengk.pool.config.ThreadPoolManager;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class DemoService {

    private final ThreadPoolManager poolManager;

    /**
     * 自定义选择线程池使用
     */
    public void submitTasks() {
        // 获取默认异步线程池
        poolManager.getExecutor("defaultPool").execute(() -> System.out.println(Thread.currentThread().getName() + " - default pool"));

        // 获取 CPU 密集型线程池
        poolManager.getExecutor("cpuPool").execute(() -> System.out.println(Thread.currentThread().getName() + " - CPU pool"));

        // 获取 IO 密集型线程池
        poolManager.getExecutor("ioPool").execute(() -> System.out.println(Thread.currentThread().getName() + " - IO pool"));
    }

    /**
     * 异步执行任务，无返回值。
     * <p>适用于不关心任务结果，只希望在后台执行的场景。</p>
     */
    @Async("defaultPool") // 指定线程池名称，也可以不写，使用默认 @Async 线程池
    public void asyncVoidTask() {
        try {
            System.out.println(Thread.currentThread().getName() + " - 开始执行异步 void 任务");
            Thread.sleep(2000); // 模拟耗时操作
            System.out.println(Thread.currentThread().getName() + " - 异步 void 任务执行完成");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("异步 void 任务被中断: " + e.getMessage());
        }
    }

    /**
     * 异步执行任务，返回 CompletableFuture。
     * <p>适用于需要获取异步任务结果或组合多个异步任务的场景。</p>
     *
     * @return CompletableFuture<String> 任务完成后的结果
     */
    @Async
    public CompletableFuture<String> asyncFutureTask() {
        try {
            System.out.println(Thread.currentThread().getName() + " - 开始执行异步 CompletableFuture 任务");
            Thread.sleep(3000); // 模拟耗时操作
            String result = "异步任务完成";
            System.out.println(Thread.currentThread().getName() + " - 异步 CompletableFuture 任务执行完成: " + result);
            return CompletableFuture.completedFuture(result);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("异步 CompletableFuture 任务被中断: " + e.getMessage());
            CompletableFuture<String> failedFuture = new CompletableFuture<>();
            failedFuture.completeExceptionally(e);
            return failedFuture;
        }
    }

}

```

### 创建Controller

```java
package io.github.atengk.pool.controller;

import io.github.atengk.pool.service.DemoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/demo")
@RequiredArgsConstructor
public class DemoController {
    private final DemoService demoService;

    @GetMapping("/submitTasks")
    public void submitTasks() {
        demoService.submitTasks();
    }

    @GetMapping("/asyncVoidTask")
    public void asyncVoidTask() {
        demoService.asyncVoidTask();
    }

    @GetMapping("/asyncFutureTask")
    public void asyncFutureTask() {
        CompletableFuture<String> completableFuture = demoService.asyncFutureTask();
        completableFuture.thenAccept(System.out::println);
    }

}



```

