package io.github.atengk.utils;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 虚拟线程工具类。
 * <p>
 * 基于 JDK 21 Virtual Thread 封装，提供虚拟线程执行器创建、异步执行、Callable 提交、
 * 批量并行、超时控制、异常兜底、首个成功结果获取、CompletableFuture 聚合和优雅关闭等能力。
 * </p>
 * <p>
 * 适用场景：I/O 密集型任务、远程调用、批量查询、批量写入、低成本高并发任务调度。
 * 虚拟线程不适合直接替代所有 CPU 密集型并发模型；CPU 密集型任务仍建议使用受控大小的平台线程池。
 * </p>
 *
 * @author Ateng
 * @since 2026-04-27
 */
public final class VirtualThreadUtil {

    private static final Logger log = LoggerFactory.getLogger(VirtualThreadUtil.class);

    /**
     * 默认虚拟线程名前缀。
     */
    private static final String DEFAULT_THREAD_NAME_PREFIX = "virtual-util-";

    /**
     * 默认关闭等待时间。
     */
    private static final Duration DEFAULT_SHUTDOWN_TIMEOUT = Duration.ofSeconds(30);

    /**
     * 默认超时时间。
     */
    private static final Duration DEFAULT_TASK_TIMEOUT = Duration.ofSeconds(30);

    /**
     * 默认虚拟线程执行器引用。
     * <p>
     * 使用 AtomicReference 是为了在默认执行器被关闭后可按需重建，避免后续调用直接抛出拒绝执行异常。
     * </p>
     */
    private static final AtomicReference<ExecutorService> DEFAULT_EXECUTOR_REF =
            new AtomicReference<>(newVirtualThreadExecutor(DEFAULT_THREAD_NAME_PREFIX));

    /**
     * 禁止实例化工具类。
     */
    private VirtualThreadUtil() {
        throw new UnsupportedOperationException("工具类不可实例化");
    }

    /**
     * 获取默认虚拟线程执行器。
     *
     * @return 默认虚拟线程执行器
     */
    public static ExecutorService defaultExecutor() {
        ExecutorService executor = DEFAULT_EXECUTOR_REF.get();
        if (isExecutorAvailable(executor)) {
            return executor;
        }

        ExecutorService newExecutor = newVirtualThreadExecutor(DEFAULT_THREAD_NAME_PREFIX);
        if (DEFAULT_EXECUTOR_REF.compareAndSet(executor, newExecutor)) {
            log.info("默认虚拟线程执行器已重建");
            return newExecutor;
        }

        shutdownNowQuietly(newExecutor);
        return DEFAULT_EXECUTOR_REF.get();
    }

    /**
     * 创建虚拟线程工厂。
     *
     * @param threadNamePrefix 线程名前缀
     * @return 虚拟线程工厂
     */
    public static ThreadFactory virtualThreadFactory(String threadNamePrefix) {
        String prefix = StrUtil.blankToDefault(threadNamePrefix, DEFAULT_THREAD_NAME_PREFIX);
        return Thread.ofVirtual().name(prefix, 0).factory();
    }

    /**
     * 创建虚拟线程执行器。
     *
     * @return 虚拟线程执行器
     */
    public static ExecutorService newVirtualThreadExecutor() {
        return newVirtualThreadExecutor(DEFAULT_THREAD_NAME_PREFIX);
    }

    /**
     * 创建带线程名前缀的虚拟线程执行器。
     *
     * @param threadNamePrefix 线程名前缀
     * @return 虚拟线程执行器
     */
    public static ExecutorService newVirtualThreadExecutor(String threadNamePrefix) {
        return Executors.newThreadPerTaskExecutor(virtualThreadFactory(threadNamePrefix));
    }

    /**
     * 使用默认执行器异步执行无返回值任务。
     *
     * @param task 异步任务
     * @return CompletableFuture 对象
     */
    public static CompletableFuture<Void> runAsync(Runnable task) {
        return runAsync(task, defaultExecutor());
    }

    /**
     * 使用指定执行器异步执行无返回值任务。
     *
     * @param task     异步任务
     * @param executor 执行器
     * @return CompletableFuture 对象
     */
    public static CompletableFuture<Void> runAsync(Runnable task, Executor executor) {
        requireTaskAndExecutor(task, executor);
        return CompletableFuture.runAsync(task, executor);
    }

    /**
     * 使用默认执行器异步执行带返回值任务。
     *
     * @param supplier 任务
     * @param <T>      返回值类型
     * @return CompletableFuture 对象
     */
    public static <T> CompletableFuture<T> supplyAsync(Supplier<T> supplier) {
        return supplyAsync(supplier, defaultExecutor());
    }

    /**
     * 使用指定执行器异步执行带返回值任务。
     *
     * @param supplier 任务
     * @param executor 执行器
     * @param <T>      返回值类型
     * @return CompletableFuture 对象
     */
    public static <T> CompletableFuture<T> supplyAsync(Supplier<T> supplier, Executor executor) {
        requireTaskAndExecutor(supplier, executor);
        return CompletableFuture.supplyAsync(supplier, executor);
    }

    /**
     * 使用默认执行器提交 Callable 任务。
     *
     * @param task 任务
     * @param <T>  返回值类型
     * @return CompletableFuture 对象
     */
    public static <T> CompletableFuture<T> submitAsync(Callable<T> task) {
        return submitAsync(task, defaultExecutor());
    }

    /**
     * 使用指定执行器提交 Callable 任务。
     *
     * @param task     任务
     * @param executor 执行器
     * @param <T>      返回值类型
     * @return CompletableFuture 对象
     */
    public static <T> CompletableFuture<T> submitAsync(Callable<T> task, Executor executor) {
        requireTaskAndExecutor(task, executor);
        return CompletableFuture.supplyAsync(() -> callUnchecked(task), executor);
    }

    /**
     * 使用默认执行器并行执行任务集合并按输入顺序收集结果。
     *
     * @param tasks    任务集合
     * @param function 执行函数
     * @param <T>      输入参数类型
     * @param <R>      返回结果类型
     * @return 结果列表
     */
    public static <T, R> List<R> parallelExecute(List<T> tasks, Function<T, R> function) {
        return parallelExecute(tasks, function, defaultExecutor());
    }

    /**
     * 使用指定执行器并行执行任务集合并按输入顺序收集结果。
     *
     * @param tasks    任务集合
     * @param function 执行函数
     * @param executor 执行器
     * @param <T>      输入参数类型
     * @param <R>      返回结果类型
     * @return 结果列表
     */
    public static <T, R> List<R> parallelExecute(List<T> tasks, Function<T, R> function, Executor executor) {
        if (CollUtil.isEmpty(tasks)) {
            return Collections.emptyList();
        }
        requireTaskAndExecutor(function, executor);

        List<CompletableFuture<R>> futures = tasks.stream()
                .map(task -> CompletableFuture.supplyAsync(() -> function.apply(task), executor))
                .toList();
        return allOf(futures);
    }

    /**
     * 使用默认执行器并行执行任务集合，异常时返回统一兜底值。
     *
     * @param tasks    任务集合
     * @param function 执行函数
     * @param fallback 异常兜底值
     * @param <T>      输入参数类型
     * @param <R>      返回结果类型
     * @return 结果列表
     */
    public static <T, R> List<R> parallelExecuteWithFallback(List<T> tasks, Function<T, R> function, R fallback) {
        return parallelExecuteWithFallback(tasks, function, fallback, defaultExecutor());
    }

    /**
     * 使用指定执行器并行执行任务集合，异常时返回统一兜底值。
     *
     * @param tasks    任务集合
     * @param function 执行函数
     * @param fallback 异常兜底值
     * @param executor 执行器
     * @param <T>      输入参数类型
     * @param <R>      返回结果类型
     * @return 结果列表
     */
    public static <T, R> List<R> parallelExecuteWithFallback(List<T> tasks, Function<T, R> function, R fallback, Executor executor) {
        if (CollUtil.isEmpty(tasks)) {
            return Collections.emptyList();
        }
        requireTaskAndExecutor(function, executor);

        List<CompletableFuture<R>> futures = tasks.stream()
                .map(task -> CompletableFuture.supplyAsync(() -> function.apply(task), executor)
                        .exceptionally(ex -> {
                            log.warn("虚拟线程并行任务执行失败，返回兜底值，task={}", task, unwrapCompletionException(ex));
                            return fallback;
                        }))
                .toList();
        return allOf(futures);
    }

    /**
     * 使用默认执行器并行遍历任务集合，仅关注副作用。
     *
     * @param tasks    任务集合
     * @param consumer 执行函数
     * @param <T>      任务类型
     */
    public static <T> void parallelForEach(List<T> tasks, Consumer<T> consumer) {
        parallelForEach(tasks, consumer, defaultExecutor());
    }

    /**
     * 使用指定执行器并行遍历任务集合，仅关注副作用。
     *
     * @param tasks    任务集合
     * @param consumer 执行函数
     * @param executor 执行器
     * @param <T>      任务类型
     */
    public static <T> void parallelForEach(List<T> tasks, Consumer<T> consumer, Executor executor) {
        if (CollUtil.isEmpty(tasks)) {
            return;
        }
        requireTaskAndExecutor(consumer, executor);

        List<CompletableFuture<Void>> futures = tasks.stream()
                .map(task -> CompletableFuture.runAsync(() -> consumer.accept(task), executor))
                .toList();
        allOfVoid(futures);
    }

    /**
     * 使用默认执行器异步执行任务，异常时返回兜底值。
     *
     * @param supplier 任务
     * @param fallback 异常兜底值
     * @param <T>      返回值类型
     * @return CompletableFuture 对象
     */
    public static <T> CompletableFuture<T> supplyAsyncWithFallback(Supplier<T> supplier, T fallback) {
        return supplyAsyncWithFallback(supplier, fallback, defaultExecutor());
    }

    /**
     * 使用指定执行器异步执行任务，异常时返回兜底值。
     *
     * @param supplier 任务
     * @param fallback 异常兜底值
     * @param executor 执行器
     * @param <T>      返回值类型
     * @return CompletableFuture 对象
     */
    public static <T> CompletableFuture<T> supplyAsyncWithFallback(Supplier<T> supplier, T fallback, Executor executor) {
        requireTaskAndExecutor(supplier, executor);
        return CompletableFuture.supplyAsync(supplier, executor)
                .exceptionally(ex -> {
                    log.warn("虚拟线程任务执行失败，返回兜底值", unwrapCompletionException(ex));
                    return fallback;
                });
    }

    /**
     * 使用默认执行器异步执行任务，超时后让 CompletableFuture 异常完成。
     *
     * @param supplier 任务
     * @param timeout  超时时间
     * @param <T>      返回值类型
     * @return CompletableFuture 对象
     */
    public static <T> CompletableFuture<T> supplyAsyncOrTimeout(Supplier<T> supplier, Duration timeout) {
        return supplyAsyncOrTimeout(supplier, timeout, defaultExecutor());
    }

    /**
     * 使用指定执行器异步执行任务，超时后让 CompletableFuture 异常完成。
     *
     * @param supplier 任务
     * @param timeout  超时时间
     * @param executor 执行器
     * @param <T>      返回值类型
     * @return CompletableFuture 对象
     */
    public static <T> CompletableFuture<T> supplyAsyncOrTimeout(Supplier<T> supplier, Duration timeout, Executor executor) {
        requireTaskAndExecutor(supplier, executor);
        requirePositiveDuration(timeout, "超时时间必须大于 0");
        return CompletableFuture.supplyAsync(supplier, executor)
                .orTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS);
    }

    /**
     * 使用默认执行器异步执行任务，超时后返回兜底值。
     *
     * @param supplier 任务
     * @param timeout  超时时间
     * @param fallback 超时或异常兜底值
     * @param <T>      返回值类型
     * @return CompletableFuture 对象
     */
    public static <T> CompletableFuture<T> supplyAsyncCompleteOnTimeout(Supplier<T> supplier, Duration timeout, T fallback) {
        return supplyAsyncCompleteOnTimeout(supplier, timeout, fallback, defaultExecutor());
    }

    /**
     * 使用指定执行器异步执行任务，超时后返回兜底值。
     *
     * @param supplier 任务
     * @param timeout  超时时间
     * @param fallback 超时或异常兜底值
     * @param executor 执行器
     * @param <T>      返回值类型
     * @return CompletableFuture 对象
     */
    public static <T> CompletableFuture<T> supplyAsyncCompleteOnTimeout(Supplier<T> supplier, Duration timeout, T fallback, Executor executor) {
        requireTaskAndExecutor(supplier, executor);
        requirePositiveDuration(timeout, "超时时间必须大于 0");
        return CompletableFuture.supplyAsync(supplier, executor)
                .completeOnTimeout(fallback, timeout.toMillis(), TimeUnit.MILLISECONDS)
                .exceptionally(ex -> {
                    log.warn("虚拟线程任务执行失败，返回兜底值", unwrapCompletionException(ex));
                    return fallback;
                });
    }

    /**
     * 使用默认执行器同步等待异步任务结果，支持超时并尝试中断任务。
     *
     * @param supplier 任务
     * @param timeout  超时时间
     * @param unit     时间单位
     * @param <T>      返回值类型
     * @return 返回结果
     */
    public static <T> T supplyAsyncWithTimeout(Supplier<T> supplier, long timeout, TimeUnit unit) {
        return supplyAsyncWithTimeout(supplier, timeout, unit, defaultExecutor());
    }

    /**
     * 使用指定执行器同步等待异步任务结果，支持超时并尝试中断任务。
     *
     * @param supplier 任务
     * @param timeout  超时时间
     * @param unit     时间单位
     * @param executor 执行器
     * @param <T>      返回值类型
     * @return 返回结果
     */
    public static <T> T supplyAsyncWithTimeout(Supplier<T> supplier, long timeout, TimeUnit unit, ExecutorService executor) {
        requireTaskAndExecutor(supplier, executor);
        requirePositiveTimeout(timeout, unit);
        Future<T> future = executor.submit(supplier::get);
        try {
            return future.get(timeout, unit);
        } catch (TimeoutException e) {
            future.cancel(true);
            log.warn("虚拟线程任务执行超时，timeout={}，unit={}", timeout, unit);
            throw new RuntimeException("虚拟线程任务执行超时", e);
        } catch (InterruptedException e) {
            future.cancel(true);
            Thread.currentThread().interrupt();
            throw new RuntimeException("虚拟线程任务执行被中断", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("虚拟线程任务执行失败", unwrapExecutionException(e));
        }
    }

    /**
     * 使用默认执行器同步等待异步任务结果，支持超时并尝试中断任务。
     *
     * @param supplier 任务
     * @param timeout  超时时间
     * @param <T>      返回值类型
     * @return 返回结果
     */
    public static <T> T supplyAsyncWithTimeout(Supplier<T> supplier, Duration timeout) {
        return supplyAsyncWithTimeout(supplier, timeout, defaultExecutor());
    }

    /**
     * 使用指定执行器同步等待异步任务结果，支持超时并尝试中断任务。
     *
     * @param supplier 任务
     * @param timeout  超时时间
     * @param executor 执行器
     * @param <T>      返回值类型
     * @return 返回结果
     */
    public static <T> T supplyAsyncWithTimeout(Supplier<T> supplier, Duration timeout, ExecutorService executor) {
        requirePositiveDuration(timeout, "超时时间必须大于 0");
        return supplyAsyncWithTimeout(supplier, timeout.toMillis(), TimeUnit.MILLISECONDS, executor);
    }

    /**
     * 等待所有 CompletableFuture 完成并按输入顺序聚合结果。
     *
     * @param futures 异步任务集合
     * @param <T>     返回值类型
     * @return 结果列表
     */
    public static <T> List<T> allOf(List<CompletableFuture<T>> futures) {
        if (CollUtil.isEmpty(futures)) {
            return Collections.emptyList();
        }
        try {
            try {
            CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
        } catch (CompletionException e) {
            throw new RuntimeException("虚拟线程任务执行失败", unwrapCompletionException(e));
        }
            return futures.stream()
                    .map(VirtualThreadUtil::join)
                    .toList();
        } catch (CompletionException e) {
            throw new RuntimeException("虚拟线程任务执行失败", unwrapCompletionException(e));
        }
    }

    /**
     * 等待所有 CompletableFuture 完成。
     *
     * @param futures 异步任务集合
     */
    public static void allOfVoid(List<CompletableFuture<Void>> futures) {
        if (CollUtil.isEmpty(futures)) {
            return;
        }
        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
    }

    /**
     * 任意一个任务完成即返回结果。
     *
     * @param futures 异步任务集合
     * @param <T>     返回值类型
     * @return 首个完成任务的结果
     */
    @SuppressWarnings("unchecked")
    public static <T> T anyOf(List<CompletableFuture<T>> futures) {
        if (CollUtil.isEmpty(futures)) {
            throw new IllegalArgumentException("任务集合不能为空");
        }
        return (T) CompletableFuture.anyOf(futures.toArray(CompletableFuture[]::new)).join();
    }

    /**
     * 获取首个成功完成的结果，忽略中途失败任务。
     *
     * @param suppliers 任务集合
     * @param <T>       返回值类型
     * @return 首个成功结果对应的 CompletableFuture
     */
    public static <T> CompletableFuture<T> firstSuccess(List<Supplier<T>> suppliers) {
        return firstSuccess(suppliers, defaultExecutor());
    }

    /**
     * 获取首个成功完成的结果，忽略中途失败任务。
     *
     * @param suppliers 任务集合
     * @param executor  执行器
     * @param <T>       返回值类型
     * @return 首个成功结果对应的 CompletableFuture
     */
    public static <T> CompletableFuture<T> firstSuccess(List<Supplier<T>> suppliers, Executor executor) {
        if (CollUtil.isEmpty(suppliers) || ObjectUtil.isNull(executor)) {
            throw new IllegalArgumentException("任务集合和执行器不能为空");
        }

        CompletableFuture<T> result = new CompletableFuture<>();
        AtomicInteger remaining = new AtomicInteger(suppliers.size());
        List<CompletableFuture<T>> runningFutures = new CopyOnWriteArrayList<>();

        for (Supplier<T> supplier : suppliers) {
            if (ObjectUtil.isNull(supplier)) {
                completeFirstSuccessIfAllFailed(result, remaining, new IllegalArgumentException("任务不能为空"));
                continue;
            }

            CompletableFuture<T> future = CompletableFuture.supplyAsync(supplier, executor);
            runningFutures.add(future);
            future.whenComplete((value, throwable) -> {
                if (throwable == null) {
                    if (result.complete(value)) {
                        cancelFutures(runningFutures);
                    }
                    return;
                }
                completeFirstSuccessIfAllFailed(result, remaining, throwable);
            });
        }

        return result;
    }

    /**
     * 异步链式调用。
     *
     * @param firstTask 第一个任务
     * @param nextTask  下一个任务
     * @param <T>       第一个任务返回值类型
     * @param <R>       下一个任务返回值类型
     * @return CompletableFuture 对象
     */
    public static <T, R> CompletableFuture<R> chain(Supplier<T> firstTask, Function<T, R> nextTask) {
        return chain(firstTask, nextTask, defaultExecutor());
    }

    /**
     * 使用指定执行器异步链式调用。
     *
     * @param firstTask 第一个任务
     * @param nextTask  下一个任务
     * @param executor  执行器
     * @param <T>       第一个任务返回值类型
     * @param <R>       下一个任务返回值类型
     * @return CompletableFuture 对象
     */
    public static <T, R> CompletableFuture<R> chain(Supplier<T> firstTask, Function<T, R> nextTask, Executor executor) {
        if (ObjectUtil.hasNull(firstTask, nextTask, executor)) {
            throw new IllegalArgumentException("任务和执行器不能为空");
        }
        return CompletableFuture.supplyAsync(firstTask, executor)
                .thenApplyAsync(nextTask, executor);
    }

    /**
     * 使用默认执行器执行 Callable 集合并等待全部完成。
     *
     * @param tasks 任务集合
     * @param <T>   返回值类型
     * @return 结果列表
     */
    public static <T> List<T> invokeAll(List<Callable<T>> tasks) {
        return invokeAll(tasks, defaultExecutor());
    }

    /**
     * 使用指定执行器执行 Callable 集合并等待全部完成。
     *
     * @param tasks    任务集合
     * @param executor 执行器
     * @param <T>      返回值类型
     * @return 结果列表
     */
    public static <T> List<T> invokeAll(List<Callable<T>> tasks, ExecutorService executor) {
        if (CollUtil.isEmpty(tasks)) {
            return Collections.emptyList();
        }
        if (ObjectUtil.isNull(executor)) {
            throw new IllegalArgumentException("执行器不能为空");
        }
        try {
            List<Future<T>> futures = executor.invokeAll(tasks);
            return collectFutureResults(futures);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("批量任务执行被中断", e);
        }
    }

    /**
     * 使用默认执行器执行 Callable 集合并等待全部完成，支持超时。
     *
     * @param tasks   任务集合
     * @param timeout 超时时间
     * @param unit    时间单位
     * @param <T>     返回值类型
     * @return 结果列表
     */
    public static <T> List<T> invokeAll(List<Callable<T>> tasks, long timeout, TimeUnit unit) {
        return invokeAll(tasks, timeout, unit, defaultExecutor());
    }

    /**
     * 使用指定执行器执行 Callable 集合并等待全部完成，支持超时。
     *
     * @param tasks    任务集合
     * @param timeout  超时时间
     * @param unit     时间单位
     * @param executor 执行器
     * @param <T>      返回值类型
     * @return 结果列表
     */
    public static <T> List<T> invokeAll(List<Callable<T>> tasks, long timeout, TimeUnit unit, ExecutorService executor) {
        if (CollUtil.isEmpty(tasks)) {
            return Collections.emptyList();
        }
        if (ObjectUtil.isNull(executor)) {
            throw new IllegalArgumentException("执行器不能为空");
        }
        requirePositiveTimeout(timeout, unit);
        try {
            List<Future<T>> futures = executor.invokeAll(tasks, timeout, unit);
            return collectFutureResults(futures);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("批量任务执行被中断", e);
        }
    }

    /**
     * 使用默认执行器执行 Callable 集合并等待全部完成，支持超时。
     *
     * @param tasks   任务集合
     * @param timeout 超时时间
     * @param <T>     返回值类型
     * @return 结果列表
     */
    public static <T> List<T> invokeAll(List<Callable<T>> tasks, Duration timeout) {
        return invokeAll(tasks, timeout, defaultExecutor());
    }

    /**
     * 使用指定执行器执行 Callable 集合并等待全部完成，支持超时。
     *
     * @param tasks    任务集合
     * @param timeout  超时时间
     * @param executor 执行器
     * @param <T>      返回值类型
     * @return 结果列表
     */
    public static <T> List<T> invokeAll(List<Callable<T>> tasks, Duration timeout, ExecutorService executor) {
        requirePositiveDuration(timeout, "超时时间必须大于 0");
        return invokeAll(tasks, timeout.toMillis(), TimeUnit.MILLISECONDS, executor);
    }

    /**
     * 使用默认执行器执行 Callable 集合，返回首个成功结果。
     *
     * @param tasks 任务集合
     * @param <T>   返回值类型
     * @return 首个成功结果
     */
    public static <T> T invokeAny(List<Callable<T>> tasks) {
        return invokeAny(tasks, defaultExecutor());
    }

    /**
     * 使用指定执行器执行 Callable 集合，返回首个成功结果。
     *
     * @param tasks    任务集合
     * @param executor 执行器
     * @param <T>      返回值类型
     * @return 首个成功结果
     */
    public static <T> T invokeAny(List<Callable<T>> tasks, ExecutorService executor) {
        if (CollUtil.isEmpty(tasks) || ObjectUtil.isNull(executor)) {
            throw new IllegalArgumentException("任务集合和执行器不能为空");
        }
        try {
            return executor.invokeAny(tasks);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("批量任务执行被中断", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("所有任务执行失败", unwrapExecutionException(e));
        }
    }

    /**
     * 使用默认执行器执行 Callable 集合，返回首个成功结果，支持超时。
     *
     * @param tasks   任务集合
     * @param timeout 超时时间
     * @param unit    时间单位
     * @param <T>     返回值类型
     * @return 首个成功结果
     */
    public static <T> T invokeAny(List<Callable<T>> tasks, long timeout, TimeUnit unit) {
        return invokeAny(tasks, timeout, unit, defaultExecutor());
    }

    /**
     * 使用指定执行器执行 Callable 集合，返回首个成功结果，支持超时。
     *
     * @param tasks    任务集合
     * @param timeout  超时时间
     * @param unit     时间单位
     * @param executor 执行器
     * @param <T>      返回值类型
     * @return 首个成功结果
     */
    public static <T> T invokeAny(List<Callable<T>> tasks, long timeout, TimeUnit unit, ExecutorService executor) {
        if (CollUtil.isEmpty(tasks) || ObjectUtil.isNull(executor)) {
            throw new IllegalArgumentException("任务集合和执行器不能为空");
        }
        requirePositiveTimeout(timeout, unit);
        try {
            return executor.invokeAny(tasks, timeout, unit);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("批量任务执行被中断", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("所有任务执行失败", unwrapExecutionException(e));
        } catch (TimeoutException e) {
            throw new RuntimeException("批量任务执行超时", e);
        }
    }

    /**
     * 使用默认执行器执行 Callable 集合，返回首个成功结果，支持超时。
     *
     * @param tasks   任务集合
     * @param timeout 超时时间
     * @param <T>     返回值类型
     * @return 首个成功结果
     */
    public static <T> T invokeAny(List<Callable<T>> tasks, Duration timeout) {
        return invokeAny(tasks, timeout, defaultExecutor());
    }

    /**
     * 使用指定执行器执行 Callable 集合，返回首个成功结果，支持超时。
     *
     * @param tasks    任务集合
     * @param timeout  超时时间
     * @param executor 执行器
     * @param <T>      返回值类型
     * @return 首个成功结果
     */
    public static <T> T invokeAny(List<Callable<T>> tasks, Duration timeout, ExecutorService executor) {
        requirePositiveDuration(timeout, "超时时间必须大于 0");
        return invokeAny(tasks, timeout.toMillis(), TimeUnit.MILLISECONDS, executor);
    }

    /**
     * 安全 join CompletableFuture，并统一异常包装。
     *
     * @param future CompletableFuture 对象
     * @param <T>    返回值类型
     * @return 返回结果
     */
    public static <T> T join(CompletableFuture<T> future) {
        if (ObjectUtil.isNull(future)) {
            throw new IllegalArgumentException("CompletableFuture 不能为空");
        }
        try {
            return future.join();
        } catch (CompletionException e) {
            throw new RuntimeException("虚拟线程任务执行失败", unwrapCompletionException(e));
        } catch (CancellationException e) {
            throw new RuntimeException("虚拟线程任务已取消", e);
        }
    }

    /**
     * 等待虚拟线程执行器优雅关闭。
     *
     * @param executorService 执行器
     */
    public static void shutdown(ExecutorService executorService) {
        shutdown(executorService, DEFAULT_SHUTDOWN_TIMEOUT);
    }

    /**
     * 等待虚拟线程执行器优雅关闭。
     *
     * @param executorService 执行器
     * @param timeout         关闭等待时间
     */
    public static void shutdown(ExecutorService executorService, Duration timeout) {
        if (ObjectUtil.isNull(executorService)) {
            return;
        }
        requirePositiveDuration(timeout, "关闭等待时间必须大于 0");

        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                log.warn("虚拟线程执行器未在限定时间内关闭，准备强制关闭，timeout={}ms", timeout.toMillis());
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
            log.warn("虚拟线程执行器关闭被中断，已尝试强制关闭", e);
        }
    }

    /**
     * 关闭默认执行器。
     */
    public static void shutdown() {
        shutdown(DEFAULT_EXECUTOR_REF.get(), DEFAULT_SHUTDOWN_TIMEOUT);
    }

    /**
     * 立即关闭默认执行器。
     */
    public static void shutdownNow() {
        shutdownNowQuietly(DEFAULT_EXECUTOR_REF.get());
    }

    /**
     * 判断执行器是否可用。
     *
     * @param executor 执行器
     * @return 是否可用
     */
    public static boolean isExecutorAvailable(ExecutorService executor) {
        return ObjectUtil.isNotNull(executor) && !executor.isShutdown() && !executor.isTerminated();
    }

    /**
     * 获取默认任务超时时间。
     *
     * @return 默认任务超时时间
     */
    public static Duration defaultTaskTimeout() {
        return DEFAULT_TASK_TIMEOUT;
    }

    /**
     * 获取默认关闭等待时间。
     *
     * @return 默认关闭等待时间
     */
    public static Duration defaultShutdownTimeout() {
        return DEFAULT_SHUTDOWN_TIMEOUT;
    }

    /**
     * 执行 Callable 并包装受检异常。
     *
     * @param task Callable 任务
     * @param <T>  返回值类型
     * @return 返回结果
     */
    private static <T> T callUnchecked(Callable<T> task) {
        try {
            return task.call();
        } catch (Exception e) {
            throw new CompletionException(e);
        }
    }

    /**
     * 聚合 Future 结果。
     *
     * @param futures Future 集合
     * @param <T>     返回值类型
     * @return 结果列表
     */
    private static <T> List<T> collectFutureResults(List<Future<T>> futures) {
        if (CollUtil.isEmpty(futures)) {
            return Collections.emptyList();
        }
        List<T> result = new ArrayList<>(futures.size());
        for (Future<T> future : futures) {
            try {
                result.add(future.get());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("虚拟线程任务执行被中断", e);
            } catch (ExecutionException e) {
                throw new RuntimeException("虚拟线程任务执行失败", unwrapExecutionException(e));
            } catch (CancellationException e) {
                throw new RuntimeException("虚拟线程任务被取消或执行超时", e);
            }
        }
        return result;
    }

    /**
     * 所有 firstSuccess 任务失败时完成异常。
     *
     * @param result    结果 Future
     * @param remaining 剩余任务数
     * @param throwable 异常
     * @param <T>       返回值类型
     */
    private static <T> void completeFirstSuccessIfAllFailed(CompletableFuture<T> result, AtomicInteger remaining, Throwable throwable) {
        if (remaining.decrementAndGet() == 0 && !result.isDone()) {
            result.completeExceptionally(new RuntimeException("所有任务都执行失败", unwrapCompletionException(throwable)));
        }
    }

    /**
     * 取消未完成任务。
     *
     * @param futures Future 集合
     */
    private static void cancelFutures(List<? extends Future<?>> futures) {
        if (CollUtil.isEmpty(futures)) {
            return;
        }
        for (Future<?> future : futures) {
            if (ObjectUtil.isNotNull(future) && !future.isDone()) {
                future.cancel(true);
            }
        }
    }

    /**
     * 立即关闭执行器并忽略异常。
     *
     * @param executorService 执行器
     */
    private static void shutdownNowQuietly(ExecutorService executorService) {
        if (ObjectUtil.isNull(executorService)) {
            return;
        }
        try {
            executorService.shutdownNow();
        } catch (RuntimeException e) {
            log.warn("虚拟线程执行器强制关闭失败", e);
        }
    }

    /**
     * 校验任务和执行器。
     *
     * @param task     任务
     * @param executor 执行器
     */
    private static void requireTaskAndExecutor(Object task, Executor executor) {
        if (ObjectUtil.isNull(task) || ObjectUtil.isNull(executor)) {
            throw new IllegalArgumentException("任务和执行器不能为空");
        }
    }

    /**
     * 校验超时时间。
     *
     * @param timeout 超时时间
     * @param unit    时间单位
     */
    private static void requirePositiveTimeout(long timeout, TimeUnit unit) {
        if (ObjectUtil.isNull(unit)) {
            throw new IllegalArgumentException("时间单位不能为空");
        }
        if (timeout <= 0) {
            throw new IllegalArgumentException("超时时间必须大于 0");
        }
    }

    /**
     * 校验 Duration。
     *
     * @param duration Duration 对象
     * @param message  异常信息
     */
    private static void requirePositiveDuration(Duration duration, String message) {
        if (ObjectUtil.isNull(duration) || duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException(StrUtil.blankToDefault(message, "时间必须大于 0"));
        }
    }

    /**
     * 解包 CompletionException。
     *
     * @param throwable 异常
     * @return 原始异常
     */
    private static Throwable unwrapCompletionException(Throwable throwable) {
        if (throwable instanceof CompletionException && ObjectUtil.isNotNull(throwable.getCause())) {
            return throwable.getCause();
        }
        return throwable;
    }

    /**
     * 解包 ExecutionException。
     *
     * @param exception 异常
     * @return 原始异常
     */
    private static Throwable unwrapExecutionException(ExecutionException exception) {
        return ObjectUtil.isNull(exception.getCause()) ? exception : exception.getCause();
    }
}
