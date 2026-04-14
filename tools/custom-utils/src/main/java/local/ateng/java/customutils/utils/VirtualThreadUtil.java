/*
package local.ateng.java.customutils.utils;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

*/
/**
 * 虚拟线程工具类
 * <p>
 * 基于 JDK 21 Virtual Thread 封装，提供常用的异步执行、批量并行、超时控制、
 * 异常兜底、Callable 提交、首个成功结果获取、优雅关闭等能力。
 * </p>
 * <p>
 * 适用场景：
 * 1. I/O 密集型任务并发执行
 * 2. 批量请求、批量查询、批量写入
 * 3. 需要低成本创建大量并发任务的场景
 * 4. 与 CompletableFuture、ExecutorService 配合使用
 * </p>
 * <p>
 * 注意事项：
 * 1. 虚拟线程更适合 I/O 密集型任务，不建议替代所有 CPU 密集型并发模型
 * 2. 默认执行器为全局共享实例，应用关闭时建议显式调用 shutdown()
 * 3. 若在 Spring Boot 中使用，建议将自定义执行器声明为 Bean 统一管理
 * </p>
 *
 * @author Ateng
 * @since 2026-04-14
 *//*

public final class VirtualThreadUtil {

    */
/**
     * 默认线程名前缀
     *//*

    private static final String DEFAULT_THREAD_NAME_PREFIX = "virtual-util-";

    */
/**
     * 默认超时时间
     *//*

    private static final long DEFAULT_TIMEOUT = 30L;

    */
/**
     * 默认超时时间单位
     *//*

    private static final TimeUnit DEFAULT_TIMEOUT_UNIT = TimeUnit.SECONDS;

    */
/**
     * 默认虚拟线程执行器
     * <p>
     * 适合项目内简单共享使用，生产环境建议由 Spring 统一托管生命周期。
     * </p>
     *//*

    private static final ExecutorService DEFAULT_EXECUTOR = newVirtualThreadExecutor(DEFAULT_THREAD_NAME_PREFIX);

    */
/**
     * 日志对象
     *//*

    private static final Logger log = LoggerFactory.getLogger(VirtualThreadUtil.class);

    */
/**
     * 禁止实例化工具类
     *//*

    private VirtualThreadUtil() {
        throw new UnsupportedOperationException("工具类不可实例化");
    }

    */
/**
     * 获取默认虚拟线程执行器
     *
     * @return 默认虚拟线程执行器
     *//*

    public static ExecutorService defaultExecutor() {
        return DEFAULT_EXECUTOR;
    }

    */
/**
     * 创建虚拟线程工厂
     *
     * @param threadNamePrefix 线程名前缀
     * @return 虚拟线程工厂
     *//*

    public static ThreadFactory virtualThreadFactory(String threadNamePrefix) {
        String prefix = StrUtil.blankToDefault(threadNamePrefix, DEFAULT_THREAD_NAME_PREFIX);
        return Thread.ofVirtual().name(prefix, 0).factory();
    }

    */
/**
     * 创建虚拟线程执行器
     *
     * @return 虚拟线程执行器
     *//*

    public static ExecutorService newVirtualThreadExecutor() {
        return newVirtualThreadExecutor(DEFAULT_THREAD_NAME_PREFIX);
    }

    */
/**
     * 创建带线程名前缀的虚拟线程执行器
     *
     * @param threadNamePrefix 线程名前缀
     * @return 虚拟线程执行器
     *//*

    public static ExecutorService newVirtualThreadExecutor(String threadNamePrefix) {
        return java.util.concurrent.Executors.newThreadPerTaskExecutor(virtualThreadFactory(threadNamePrefix));
    }

    */
/**
     * 使用默认执行器异步执行无返回值任务
     *
     * @param task 异步任务
     *//*

    public static void runAsync(Runnable task) {
        runAsync(task, DEFAULT_EXECUTOR);
    }

    */
/**
     * 使用指定执行器异步执行无返回值任务
     *
     * @param task     异步任务
     * @param executor 执行器
     *//*

    public static void runAsync(Runnable task, Executor executor) {
        validateTask(task, executor);
        CompletableFuture.runAsync(task, executor);
    }

    */
/**
     * 使用默认执行器异步执行带返回值任务
     *
     * @param supplier 任务
     * @param <T>      返回值类型
     * @return CompletableFuture 对象
     *//*

    public static <T> CompletableFuture<T> supplyAsync(Supplier<T> supplier) {
        return supplyAsync(supplier, DEFAULT_EXECUTOR);
    }

    */
/**
     * 使用指定执行器异步执行带返回值任务
     *
     * @param supplier 任务
     * @param executor  执行器
     * @param <T>       返回值类型
     * @return CompletableFuture 对象
     *//*

    public static <T> CompletableFuture<T> supplyAsync(Supplier<T> supplier, Executor executor) {
        validateTask(supplier, executor);
        return CompletableFuture.supplyAsync(supplier, executor);
    }

    */
/**
     * 使用默认执行器提交 Callable 任务
     *
     * @param task 任务
     * @param <T>  返回值类型
     * @return CompletableFuture 对象
     *//*

    public static <T> CompletableFuture<T> submitAsync(Callable<T> task) {
        return submitAsync(task, DEFAULT_EXECUTOR);
    }

    */
/**
     * 使用指定执行器提交 Callable 任务
     *
     * @param task     任务
     * @param executor 执行器
     * @param <T>      返回值类型
     * @return CompletableFuture 对象
     *//*

    public static <T> CompletableFuture<T> submitAsync(Callable<T> task, Executor executor) {
        if (ObjectUtil.isNull(task) || ObjectUtil.isNull(executor)) {
            throw new IllegalArgumentException("任务和执行器不能为空");
        }
        return CompletableFuture.supplyAsync(() -> {
            try {
                return task.call();
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }, executor);
    }

    */
/**
     * 并行执行任务集合并收集结果
     *
     * @param tasks 任务集合
     * @param function 执行函数
     * @param <T> 输入参数类型
     * @param <R> 返回结果类型
     * @return 结果列表
     *//*

    public static <T, R> List<R> parallelExecute(List<T> tasks, Function<T, R> function) {
        return parallelExecute(tasks, function, DEFAULT_EXECUTOR);
    }

    */
/**
     * 使用指定执行器并行执行任务集合并收集结果
     *
     * @param tasks     任务集合
     * @param function   执行函数
     * @param executor   执行器
     * @param <T>        输入参数类型
     * @param <R>        返回结果类型
     * @return 结果列表
     *//*

    public static <T, R> List<R> parallelExecute(List<T> tasks, Function<T, R> function, Executor executor) {
        if (ObjectUtil.isEmpty(tasks) || ObjectUtil.isNull(function) || ObjectUtil.isNull(executor)) {
            throw new IllegalArgumentException("任务集合、执行函数和执行器不能为空");
        }
        List<CompletableFuture<R>> futureList = tasks.stream()
                .map(task -> CompletableFuture.supplyAsync(() -> function.apply(task), executor))
                .collect(Collectors.toList());
        return futureList.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
    }

    */
/**
     * 并行执行任务集合，仅关注副作用，不收集结果
     *
     * @param tasks   任务集合
     * @param consumer 执行函数
     * @param <T>     任务类型
     *//*

    public static <T> void parallelForEach(List<T> tasks, Consumer<T> consumer) {
        parallelForEach(tasks, consumer, DEFAULT_EXECUTOR);
    }

    */
/**
     * 使用指定执行器并行执行任务集合，仅关注副作用，不收集结果
     *
     * @param tasks    任务集合
     * @param consumer 执行函数
     * @param executor 执行器
     * @param <T>      任务类型
     *//*

    public static <T> void parallelForEach(List<T> tasks, Consumer<T> consumer, Executor executor) {
        if (ObjectUtil.isEmpty(tasks) || ObjectUtil.isNull(consumer) || ObjectUtil.isNull(executor)) {
            throw new IllegalArgumentException("任务集合、执行函数和执行器不能为空");
        }
        List<CompletableFuture<Void>> futureList = tasks.stream()
                .map(task -> CompletableFuture.runAsync(() -> consumer.accept(task), executor))
                .collect(Collectors.toList());
        futureList.forEach(CompletableFuture::join);
    }

    */
/**
     * 使用默认执行器并行执行任务集合并收集结果，支持返回值兜底
     *
     * @param supplier 任务
     * @param fallback 异常时兜底值
     * @param <T>      返回值类型
     * @return CompletableFuture 对象
     *//*

    public static <T> CompletableFuture<T> supplyAsyncWithFallback(Supplier<T> supplier, T fallback) {
        return supplyAsyncWithFallback(supplier, fallback, DEFAULT_EXECUTOR);
    }

    */
/**
     * 使用指定执行器并行执行任务集合并收集结果，支持返回值兜底
     *
     * @param supplier 任务
     * @param fallback 异常时兜底值
     * @param executor  执行器
     * @param <T>       返回值类型
     * @return CompletableFuture 对象
     *//*

    public static <T> CompletableFuture<T> supplyAsyncWithFallback(Supplier<T> supplier, T fallback, Executor executor) {
        validateTask(supplier, executor);
        return CompletableFuture.supplyAsync(supplier, executor)
                .exceptionally(ex -> {
                    log.warn("虚拟线程任务执行失败，返回兜底值", ex);
                    return fallback;
                });
    }

    */
/**
     * 使用默认执行器异步执行并设置超时时间
     *
     * @param supplier 任务
     * @param timeout  超时时间
     * @param timeUnit 时间单位
     * @param <T>      返回值类型
     * @return 返回结果
     *//*

    public static <T> T supplyAsyncWithTimeout(Supplier<T> supplier, long timeout, TimeUnit timeUnit) {
        return supplyAsyncWithTimeout(supplier, timeout, timeUnit, DEFAULT_EXECUTOR);
    }

    */
/**
     * 使用指定执行器异步执行并设置超时时间
     *
     * @param supplier 任务
     * @param timeout  超时时间
     * @param timeUnit 时间单位
     * @param executor 执行器
     * @param <T>      返回值类型
     * @return 返回结果
     *//*

    public static <T> T supplyAsyncWithTimeout(Supplier<T> supplier, long timeout, TimeUnit timeUnit, Executor executor) {
        if (ObjectUtil.isNull(supplier) || ObjectUtil.isNull(timeUnit) || ObjectUtil.isNull(executor)) {
            throw new IllegalArgumentException("任务、时间单位和执行器不能为空");
        }
        if (timeout <= 0) {
            throw new IllegalArgumentException("超时时间必须大于 0");
        }
        CompletableFuture<T> future = CompletableFuture.supplyAsync(supplier, executor);
        try {
            return future.get(timeout, timeUnit);
        } catch (TimeoutException e) {
            future.cancel(true);
            log.warn("虚拟线程任务执行超时，timeout={}, timeUnit={}", timeout, timeUnit);
            throw new RuntimeException("虚拟线程任务执行超时", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("虚拟线程任务执行被中断", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("虚拟线程任务执行失败", e.getCause() == null ? e : e.getCause());
        }
    }

    */
/**
     * 等待所有任务完成并聚合结果
     *
     * @param futures 异步任务集合
     * @param <T>     返回值类型
     * @return 结果列表
     *//*

    public static <T> List<T> allOf(List<CompletableFuture<T>> futures) {
        if (ObjectUtil.isEmpty(futures)) {
            throw new IllegalArgumentException("任务集合不能为空");
        }
        CompletableFuture<Void> allFuture = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        allFuture.join();
        return futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
    }

    */
/**
     * 任意一个任务完成即返回
     *
     * @param futures 异步任务集合
     * @param <T>     返回值类型
     * @return 首个完成任务的结果
     *//*

    @SuppressWarnings("unchecked")
    public static <T> T anyOf(List<CompletableFuture<T>> futures) {
        if (ObjectUtil.isEmpty(futures)) {
            throw new IllegalArgumentException("任务集合不能为空");
        }
        CompletableFuture<Object> anyFuture = CompletableFuture.anyOf(futures.toArray(new CompletableFuture[0]));
        return (T) anyFuture.join();
    }

    */
/**
     * 获取首个成功完成的结果，忽略中途失败任务
     *
     * @param suppliers 任务集合
     * @param <T>       返回值类型
     * @return 首个成功结果对应的 CompletableFuture
     *//*

    public static <T> CompletableFuture<T> firstSuccess(List<Supplier<T>> suppliers) {
        return firstSuccess(suppliers, DEFAULT_EXECUTOR);
    }

    */
/**
     * 获取首个成功完成的结果，忽略中途失败任务
     *
     * @param suppliers 任务集合
     * @param executor   执行器
     * @param <T>        返回值类型
     * @return 首个成功结果对应的 CompletableFuture
     *//*

    public static <T> CompletableFuture<T> firstSuccess(List<Supplier<T>> suppliers, Executor executor) {
        if (ObjectUtil.isEmpty(suppliers) || ObjectUtil.isNull(executor)) {
            throw new IllegalArgumentException("任务集合和执行器不能为空");
        }
        CompletableFuture<T> result = new CompletableFuture<>();
        AtomicInteger remaining = new AtomicInteger(suppliers.size());

        for (Supplier<T> supplier : suppliers) {
            if (ObjectUtil.isNull(supplier)) {
                if (remaining.decrementAndGet() == 0 && !result.isDone()) {
                    result.completeExceptionally(new RuntimeException("所有任务都执行失败"));
                }
                continue;
            }
            CompletableFuture.supplyAsync(supplier, executor)
                    .whenComplete((value, throwable) -> {
                        if (throwable == null) {
                            result.complete(value);
                            return;
                        }

                        if (remaining.decrementAndGet() == 0 && !result.isDone()) {
                            result.completeExceptionally(new RuntimeException("所有任务都执行失败", throwable));
                        }
                    });
        }

        return result;
    }

    */
/**
     * 异步链式调用
     *
     * @param firstTask 第一个任务
     * @param nextTask  下一个任务
     * @param <T>       第一个任务返回值类型
     * @param <R>       下一个任务返回值类型
     * @return CompletableFuture 对象
     *//*

    public static <T, R> CompletableFuture<R> chain(Supplier<T> firstTask, Function<T, R> nextTask) {
        return chain(firstTask, nextTask, DEFAULT_EXECUTOR);
    }

    */
/**
     * 使用指定执行器进行异步链式调用
     *
     * @param firstTask 第一个任务
     * @param nextTask  下一个任务
     * @param executor  执行器
     * @param <T>       第一个任务返回值类型
     * @param <R>       下一个任务返回值类型
     * @return CompletableFuture 对象
     *//*

    public static <T, R> CompletableFuture<R> chain(Supplier<T> firstTask, Function<T, R> nextTask, Executor executor) {
        if (ObjectUtil.isNull(firstTask) || ObjectUtil.isNull(nextTask) || ObjectUtil.isNull(executor)) {
            throw new IllegalArgumentException("任务和执行器不能为空");
        }
        return CompletableFuture.supplyAsync(firstTask, executor)
                .thenApplyAsync(nextTask, executor);
    }

    */
/**
     * 使用默认执行器执行 Callable 集合并等待全部完成
     *
     * @param tasks 任务集合
     * @param <T>   返回值类型
     * @return 结果列表
     *//*

    public static <T> List<T> invokeAll(List<Callable<T>> tasks) {
        return invokeAll(tasks, DEFAULT_EXECUTOR);
    }

    */
/**
     * 使用指定执行器执行 Callable 集合并等待全部完成
     *
     * @param tasks     任务集合
     * @param executor  执行器
     * @param <T>       返回值类型
     * @return 结果列表
     *//*

    public static <T> List<T> invokeAll(List<Callable<T>> tasks, ExecutorService executor) {
        if (ObjectUtil.isEmpty(tasks) || ObjectUtil.isNull(executor)) {
            throw new IllegalArgumentException("任务集合和执行器不能为空");
        }
        try {
            List<Future<T>> futures = executor.invokeAll(tasks);
            return futures.stream().map(future -> {
                try {
                    return future.get();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("虚拟线程任务执行被中断", e);
                } catch (ExecutionException e) {
                    throw new RuntimeException("虚拟线程任务执行失败", e.getCause() == null ? e : e.getCause());
                }
            }).collect(Collectors.toList());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("批量任务执行被中断", e);
        }
    }

    */
/**
     * 使用默认执行器执行 Callable 集合并等待全部完成，支持超时
     *
     * @param tasks   任务集合
     * @param timeout 超时时间
     * @param unit    时间单位
     * @param <T>     返回值类型
     * @return 结果列表
     *//*

    public static <T> List<T> invokeAll(List<Callable<T>> tasks, long timeout, TimeUnit unit) {
        return invokeAll(tasks, timeout, unit, DEFAULT_EXECUTOR);
    }

    */
/**
     * 使用指定执行器执行 Callable 集合并等待全部完成，支持超时
     *
     * @param tasks   任务集合
     * @param timeout 超时时间
     * @param unit    时间单位
     * @param executor 执行器
     * @param <T>     返回值类型
     * @return 结果列表
     *//*

    public static <T> List<T> invokeAll(List<Callable<T>> tasks, long timeout, TimeUnit unit, ExecutorService executor) {
        if (ObjectUtil.isEmpty(tasks) || ObjectUtil.isNull(unit) || ObjectUtil.isNull(executor)) {
            throw new IllegalArgumentException("任务集合、时间单位和执行器不能为空");
        }
        if (timeout <= 0) {
            throw new IllegalArgumentException("超时时间必须大于 0");
        }
        try {
            List<Future<T>> futures = executor.invokeAll(tasks, timeout, unit);
            return futures.stream().map(future -> {
                try {
                    return future.get();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("虚拟线程任务执行被中断", e);
                } catch (ExecutionException e) {
                    throw new RuntimeException("虚拟线程任务执行失败", e.getCause() == null ? e : e.getCause());
                } catch (CancellationException e) {
                    throw new RuntimeException("虚拟线程任务执行超时", e);
                }
            }).collect(Collectors.toList());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("批量任务执行被中断", e);
        }
    }

    */
/**
     * 使用默认执行器执行 Callable 集合，返回首个成功结果
     *
     * @param tasks 任务集合
     * @param <T>   返回值类型
     * @return 首个成功结果
     *//*

    public static <T> T invokeAny(List<Callable<T>> tasks) {
        return invokeAny(tasks, DEFAULT_EXECUTOR);
    }

    */
/**
     * 使用指定执行器执行 Callable 集合，返回首个成功结果
     *
     * @param tasks    任务集合
     * @param executor 执行器
     * @param <T>      返回值类型
     * @return 首个成功结果
     *//*

    public static <T> T invokeAny(List<Callable<T>> tasks, ExecutorService executor) {
        if (ObjectUtil.isEmpty(tasks) || ObjectUtil.isNull(executor)) {
            throw new IllegalArgumentException("任务集合和执行器不能为空");
        }
        try {
            return executor.invokeAny(tasks);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("批量任务执行被中断", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("所有任务执行失败", e.getCause() == null ? e : e.getCause());
        }
    }

    */
/**
     * 使用默认执行器执行 Callable 集合，返回首个成功结果，支持超时
     *
     * @param tasks   任务集合
     * @param timeout 超时时间
     * @param unit    时间单位
     * @param <T>     返回值类型
     * @return 首个成功结果
     *//*

    public static <T> T invokeAny(List<Callable<T>> tasks, long timeout, TimeUnit unit) {
        return invokeAny(tasks, timeout, unit, DEFAULT_EXECUTOR);
    }

    */
/**
     * 使用指定执行器执行 Callable 集合，返回首个成功结果，支持超时
     *
     * @param tasks    任务集合
     * @param timeout  超时时间
     * @param unit     时间单位
     * @param executor 执行器
     * @param <T>      返回值类型
     * @return 首个成功结果
     *//*

    public static <T> T invokeAny(List<Callable<T>> tasks, long timeout, TimeUnit unit, ExecutorService executor) {
        if (ObjectUtil.isEmpty(tasks) || ObjectUtil.isNull(unit) || ObjectUtil.isNull(executor)) {
            throw new IllegalArgumentException("任务集合、时间单位和执行器不能为空");
        }
        if (timeout <= 0) {
            throw new IllegalArgumentException("超时时间必须大于 0");
        }
        try {
            return executor.invokeAny(tasks, timeout, unit);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("批量任务执行被中断", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("所有任务执行失败", e.getCause() == null ? e : e.getCause());
        } catch (TimeoutException e) {
            throw new RuntimeException("批量任务执行超时", e);
        }
    }

    */
/**
     * 等待虚拟线程执行器优雅关闭
     *
     * @param executorService 执行器
     *//*

    public static void shutdown(ExecutorService executorService) {
        if (ObjectUtil.isNull(executorService)) {
            return;
        }
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(DEFAULT_TIMEOUT, DEFAULT_TIMEOUT_UNIT)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    */
/**
     * 关闭默认执行器
     *//*

    public static void shutdown() {
        shutdown(DEFAULT_EXECUTOR);
    }

    */
/**
     * 校验任务和执行器
     *
     * @param task     任务
     * @param executor 执行器
     *//*

    private static void validateTask(Object task, Executor executor) {
        if (ObjectUtil.isNull(task) || ObjectUtil.isNull(executor)) {
            throw new IllegalArgumentException("任务和执行器不能为空");
        }
    }
}*/
