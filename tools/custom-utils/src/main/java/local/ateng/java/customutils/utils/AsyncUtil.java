package local.ateng.java.customutils.utils;

import org.springframework.util.ObjectUtils;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 异步操作工具类
 * <p>
 * 基于 Java8 CompletableFuture 封装，提供常用的异步执行方法。
 * 适用于需要异步执行任务、并行处理数据、聚合结果等场景。
 * </p>
 * <p>
 * 使用说明：
 * 1. 可用于异步执行无返回值任务
 * 2. 可用于异步执行带返回值任务
 * 3. 可用于并行批量执行任务并收集结果
 * 4. 提供异常捕获机制，避免异步线程异常导致阻塞
 * <p>
 * 注意事项：
 * - 所有方法均依赖线程池，请根据项目场景配置合理的线程池参数
 * - 建议统一在 Spring 配置类中定义线程池，注入到工具类中使用
 *
 * @author 孔余
 * @since 2025-09-12
 */
public class AsyncUtil {

    /**
     * 默认线程池核心线程数
     */
    private static final int DEFAULT_CORE_POOL_SIZE = 10;

    /**
     * 默认线程池最大线程数
     */
    private static final int DEFAULT_MAX_POOL_SIZE = 20;

    /**
     * 默认线程池队列容量
     */
    private static final int DEFAULT_QUEUE_CAPACITY = 100;

    /**
     * 默认线程存活时间（秒）
     */
    private static final int DEFAULT_KEEP_ALIVE = 60;

    /**
     * 全局线程池
     * <p>
     * 在生产项目中建议通过 Spring 配置注入，而不是直接使用此默认线程池
     * </p>
     */
    private static final ExecutorService EXECUTOR = new ThreadPoolExecutor(
            DEFAULT_CORE_POOL_SIZE,
            DEFAULT_MAX_POOL_SIZE,
            DEFAULT_KEEP_ALIVE,
            TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(DEFAULT_QUEUE_CAPACITY),
            Executors.defaultThreadFactory(),
            new ThreadPoolExecutor.AbortPolicy()
    );

    /**
     * 异步执行无返回值任务
     *
     * @param task 异步任务
     */
    public static void runAsync(Runnable task) {
        if (ObjectUtils.isEmpty(task)) {
            return;
        }
        CompletableFuture.runAsync(task, EXECUTOR);
    }

    /**
     * 异步执行带返回值任务
     *
     * @param supplier 提供返回值的任务
     * @param <T>      返回值类型
     * @return CompletableFuture 对象，可通过 get() 获取返回值
     */
    public static <T> CompletableFuture<T> supplyAsync(Supplier<T> supplier) {
        if (ObjectUtils.isEmpty(supplier)) {
            throw new IllegalArgumentException("异步任务不能为空");
        }
        return CompletableFuture.supplyAsync(supplier, EXECUTOR);
    }

    /**
     * 并行执行任务集合并收集结果
     *
     * @param tasks 任务集合
     * @param <T>   输入参数类型
     * @param <R>   返回结果类型
     * @return 结果列表
     */
    public static <T, R> List<R> parallelExecute(List<T> tasks, Function<T, R> function) {
        if (ObjectUtils.isEmpty(tasks) || ObjectUtils.isEmpty(function)) {
            throw new IllegalArgumentException("任务集合和执行函数不能为空");
        }
        List<CompletableFuture<R>> futureList = tasks.stream()
                .map(task -> CompletableFuture.supplyAsync(() -> function.apply(task), EXECUTOR))
                .collect(Collectors.toList());
        return futureList.stream().map(CompletableFuture::join).collect(Collectors.toList());
    }

    /**
     * 异步执行并处理异常
     *
     * @param supplier 异步任务
     * @param fallback 异常时的默认值
     * @param <T>      返回值类型
     * @return CompletableFuture 对象
     */
    public static <T> CompletableFuture<T> supplyAsyncWithFallback(Supplier<T> supplier, T fallback) {
        if (ObjectUtils.isEmpty(supplier)) {
            throw new IllegalArgumentException("异步任务不能为空");
        }
        return CompletableFuture.supplyAsync(supplier, EXECUTOR)
                .exceptionally(ex -> fallback);
    }

    /**
     * 异步执行并设置超时时间
     *
     * @param supplier 异步任务
     * @param timeout  超时时间
     * @param timeUnit 时间单位
     * @param <T>      返回值类型
     * @return 返回结果
     */
    public static <T> T supplyAsyncWithTimeout(Supplier<T> supplier, long timeout, TimeUnit timeUnit) {
        if (ObjectUtils.isEmpty(supplier)) {
            throw new IllegalArgumentException("异步任务不能为空");
        }
        CompletableFuture<T> future = CompletableFuture.supplyAsync(supplier, EXECUTOR);
        try {
            return future.get(timeout, timeUnit);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new RuntimeException("异步任务执行超时", e);
        } catch (Exception e) {
            throw new RuntimeException("异步任务执行失败", e);
        }
    }

    /**
     * 等待所有任务完成并聚合结果
     *
     * @param futures 异步任务集合
     * @param <T>     返回值类型
     * @return 结果列表
     */
    public static <T> List<T> allOf(List<CompletableFuture<T>> futures) {
        if (ObjectUtils.isEmpty(futures)) {
            throw new IllegalArgumentException("任务集合不能为空");
        }
        CompletableFuture<Void> allFuture = CompletableFuture
                .allOf(futures.toArray(new CompletableFuture[0]));
        allFuture.join();
        return futures.stream().map(CompletableFuture::join).collect(Collectors.toList());
    }

    /**
     * 任意一个任务完成即返回
     *
     * @param futures 异步任务集合
     * @param <T>     返回值类型
     * @return 首个完成任务的结果
     */
    public static <T> T anyOf(List<CompletableFuture<T>> futures) {
        if (ObjectUtils.isEmpty(futures)) {
            throw new IllegalArgumentException("任务集合不能为空");
        }
        CompletableFuture<Object> anyFuture = CompletableFuture
                .anyOf(futures.toArray(new CompletableFuture[0]));
        return (T) anyFuture.join();
    }

    /**
     * 异步链式调用
     *
     * @param firstTask 第一个任务
     * @param nextTask  下一个任务
     * @param <T>       第一个任务返回值类型
     * @param <R>       下一个任务返回值类型
     * @return CompletableFuture 对象
     */
    public static <T, R> CompletableFuture<R> chain(Supplier<T> firstTask, Function<T, R> nextTask) {
        if (ObjectUtils.isEmpty(firstTask) || ObjectUtils.isEmpty(nextTask)) {
            throw new IllegalArgumentException("任务不能为空");
        }
        return CompletableFuture.supplyAsync(firstTask, EXECUTOR)
                .thenApplyAsync(nextTask, EXECUTOR);
    }

    /**
     * 优雅关闭线程池
     * <p>
     * 在项目关闭时调用，确保任务执行完成后再关闭
     * </p>
     */
    public static void shutdown() {
        EXECUTOR.shutdown();
        try {
            if (!EXECUTOR.awaitTermination(30, TimeUnit.SECONDS)) {
                EXECUTOR.shutdownNow();
            }
        } catch (InterruptedException e) {
            EXECUTOR.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }


    /**
     * 构建固定大小线程池
     *
     * @param poolSize   线程池大小
     * @param threadName 线程名前缀
     * @return 固定大小线程池实例
     */
    public static ExecutorService buildFixedThreadPool(int poolSize, String threadName) {
        return new ThreadPoolExecutor(
                poolSize,
                poolSize,
                0L,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(),
                new NamedThreadFactory(threadName),
                new ThreadPoolExecutor.AbortPolicy()
        );
    }

    /**
     * 构建缓存线程池
     *
     * @param threadName 线程名前缀
     * @return 缓存线程池实例
     */
    public static ExecutorService buildCachedThreadPool(String threadName) {
        return new ThreadPoolExecutor(
                0,
                Integer.MAX_VALUE,
                60L,
                TimeUnit.SECONDS,
                new SynchronousQueue<>(),
                new NamedThreadFactory(threadName),
                new ThreadPoolExecutor.AbortPolicy()
        );
    }

    /**
     * 构建单线程线程池
     *
     * @param threadName 线程名前缀
     * @return 单线程线程池实例
     */
    public static ExecutorService buildSingleThreadPool(String threadName) {
        return new ThreadPoolExecutor(
                1,
                1,
                0L,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(),
                new NamedThreadFactory(threadName),
                new ThreadPoolExecutor.AbortPolicy()
        );
    }

    /**
     * 构建自定义线程池
     *
     * @param corePoolSize    核心线程数
     * @param maxPoolSize     最大线程数
     * @param keepAliveTime   非核心线程存活时间
     * @param timeUnit        时间单位
     * @param queueCapacity   队列容量
     * @param threadName      线程名前缀
     * @param rejectedHandler 拒绝策略
     * @return 自定义线程池实例
     */
    public static ExecutorService buildCustomThreadPool(
            int corePoolSize,
            int maxPoolSize,
            long keepAliveTime,
            TimeUnit timeUnit,
            int queueCapacity,
            String threadName,
            RejectedExecutionHandler rejectedHandler) {
        return new ThreadPoolExecutor(
                corePoolSize,
                maxPoolSize,
                keepAliveTime,
                timeUnit,
                new LinkedBlockingQueue<>(queueCapacity),
                new NamedThreadFactory(threadName),
                rejectedHandler
        );
    }

    /**
     * 使用自定义线程池执行异步任务（带返回值）
     *
     * @param supplier 异步任务
     * @param executor 指定线程池
     * @param <T>      返回值类型
     * @return CompletableFuture 对象
     */
    public static <T> CompletableFuture<T> supplyAsyncWithExecutor(Supplier<T> supplier, Executor executor) {
        if (ObjectUtils.isEmpty(supplier) || ObjectUtils.isEmpty(executor)) {
            throw new IllegalArgumentException("任务与线程池不能为空");
        }
        return CompletableFuture.supplyAsync(supplier, executor);
    }

    /**
     * 使用自定义线程池执行并行任务集合
     *
     * @param tasks    任务集合
     * @param executor 指定线程池
     * @param <T>      返回值类型
     * @return 结果集合
     */
    public static <T> List<T> parallelExecuteWithExecutor(List<Supplier<T>> tasks, Executor executor) {
        if (ObjectUtils.isEmpty(tasks) || ObjectUtils.isEmpty(executor)) {
            throw new IllegalArgumentException("任务集合与线程池不能为空");
        }
        List<CompletableFuture<T>> futures = tasks.stream()
                .map(task -> CompletableFuture.supplyAsync(task, executor))
                .collect(Collectors.toList());
        return futures.stream().map(CompletableFuture::join).collect(Collectors.toList());
    }

    /**
     * 优雅关闭线程池
     *
     * @param executorService 线程池实例
     */
    public static void shutdown(ExecutorService executorService) {
        if (ObjectUtils.isEmpty(executorService)) {
            return;
        }
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 自定义线程工厂，支持设置线程名前缀
     */
    private static class NamedThreadFactory implements ThreadFactory {
        private final String threadNamePrefix;
        private final AtomicInteger threadIndex = new AtomicInteger(1);

        NamedThreadFactory(String threadNamePrefix) {
            this.threadNamePrefix = threadNamePrefix;
        }

        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, threadNamePrefix + "-" + threadIndex.getAndIncrement());
        }
    }

}
