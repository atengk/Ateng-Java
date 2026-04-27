package io.github.atengk.controller;

import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.utils.VirtualThreadUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * 虚拟线程工具类使用示例接口。
 * <p>
 * 该 Controller 将 VirtualThreadUtil 的公开方法按执行器、基础异步、批量并行、超时兜底、Future 聚合、
 * invoke 系列和生命周期管理进行分组演示，便于在 Spring Boot 3 + JDK 21 项目中直接验证虚拟线程工具类能力。
 * </p>
 * <p>
 * 注意：生命周期相关接口会关闭执行器，仅建议在本地验证或管理接口中使用，不建议直接暴露到公网环境。
 * </p>
 *
 * @author Ateng
 * @since 2026-04-27
 */
@RestController
@RequestMapping("/api/virtual-thread")
public class VirtualThreadDemoController {

    private static final Logger log = LoggerFactory.getLogger(VirtualThreadDemoController.class);

    /**
     * 查看默认执行器和默认配置。
     *
     * @return 默认配置
     */
    @GetMapping("/metadata")
    public Dict metadata() {
        ExecutorService executor = VirtualThreadUtil.defaultExecutor();
        return Dict.create()
                .set("defaultExecutorAvailable", VirtualThreadUtil.isExecutorAvailable(executor))
                .set("defaultExecutorClass", executor.getClass().getName())
                .set("defaultTaskTimeout", VirtualThreadUtil.defaultTaskTimeout().toString())
                .set("defaultShutdownTimeout", VirtualThreadUtil.defaultShutdownTimeout().toString())
                .set("currentThread", Thread.currentThread().toString());
    }

    /**
     * 演示虚拟线程工厂和自定义执行器创建。
     *
     * @param prefix 线程名前缀
     * @return 执行器创建结果
     */
    @GetMapping("/executor")
    public Dict executor(@RequestParam(defaultValue = "demo-virtual-") String prefix) {
        String threadNamePrefix = StrUtil.blankToDefault(prefix, "demo-virtual-");
        ThreadFactory factory = VirtualThreadUtil.virtualThreadFactory(threadNamePrefix + "factory-");
        ExecutorService defaultNameExecutor = VirtualThreadUtil.newVirtualThreadExecutor();
        ExecutorService namedExecutor = VirtualThreadUtil.newVirtualThreadExecutor(threadNamePrefix + "named-");
        AtomicReference<String> factoryThreadName = new AtomicReference<>();

        try {
            Thread thread = factory.newThread(() -> factoryThreadName.set(Thread.currentThread().toString()));
            thread.start();
            joinThread(thread);

            Future<String> defaultExecutorResult = defaultNameExecutor.submit(() -> currentThread("newVirtualThreadExecutor"));
            Future<String> namedExecutorResult = namedExecutor.submit(() -> currentThread("newVirtualThreadExecutor(String)"));

            return Dict.create()
                    .set("virtualThreadFactory", factoryThreadName.get())
                    .set("newVirtualThreadExecutor", getFutureResult(defaultExecutorResult))
                    .set("newVirtualThreadExecutorWithPrefix", getFutureResult(namedExecutorResult))
                    .set("defaultNameExecutorAvailableBeforeShutdown", VirtualThreadUtil.isExecutorAvailable(defaultNameExecutor))
                    .set("namedExecutorAvailableBeforeShutdown", VirtualThreadUtil.isExecutorAvailable(namedExecutor));
        } finally {
            VirtualThreadUtil.shutdown(defaultNameExecutor);
            VirtualThreadUtil.shutdown(namedExecutor, Duration.ofSeconds(5));
        }
    }

    /**
     * 演示 runAsync、supplyAsync、submitAsync 的默认执行器和指定执行器用法。
     *
     * @return 基础异步执行结果
     */
    @GetMapping("/async/basic")
    public Dict asyncBasic() {
        ExecutorService executor = VirtualThreadUtil.newVirtualThreadExecutor("demo-basic-");
        List<String> runMessages = new CopyOnWriteArrayList<>();

        try {
            CompletableFuture<Void> runDefault = VirtualThreadUtil.runAsync(() -> {
                sleep(Duration.ofMillis(50));
                runMessages.add(currentThread("runAsync(Runnable)"));
            });
            CompletableFuture<Void> runCustom = VirtualThreadUtil.runAsync(() -> {
                sleep(Duration.ofMillis(50));
                runMessages.add(currentThread("runAsync(Runnable, Executor)"));
            }, executor);

            CompletableFuture<String> supplyDefault = VirtualThreadUtil.supplyAsync(() -> currentThread("supplyAsync(Supplier)"));
            CompletableFuture<String> supplyCustom = VirtualThreadUtil.supplyAsync(() -> currentThread("supplyAsync(Supplier, Executor)"), executor);
            CompletableFuture<String> submitDefault = VirtualThreadUtil.submitAsync(() -> currentThread("submitAsync(Callable)"));
            CompletableFuture<String> submitCustom = VirtualThreadUtil.submitAsync(() -> currentThread("submitAsync(Callable, Executor)"), executor);

            VirtualThreadUtil.allOfVoid(List.of(runDefault, runCustom));
            List<String> results = VirtualThreadUtil.allOf(List.of(supplyDefault, supplyCustom, submitDefault, submitCustom));

            return Dict.create()
                    .set("runAsync", runMessages)
                    .set("asyncResults", results);
        } finally {
            VirtualThreadUtil.shutdown(executor, Duration.ofSeconds(5));
        }
    }

    /**
     * 演示批量并行执行和副作用遍历。
     *
     * @param count 任务数量
     * @return 批量并行执行结果
     */
    @GetMapping("/parallel")
    public Dict parallel(@RequestParam(defaultValue = "5") int count) {
        int size = safeCount(count);
        List<Integer> numbers = numbers(size);
        ExecutorService executor = VirtualThreadUtil.newVirtualThreadExecutor("demo-parallel-");
        List<String> sideEffects = new CopyOnWriteArrayList<>();

        try {
            List<String> executeDefault = VirtualThreadUtil.parallelExecute(numbers,
                    item -> currentThread("parallelExecute-default-" + item));
            List<String> executeCustom = VirtualThreadUtil.parallelExecute(numbers,
                    item -> currentThread("parallelExecute-custom-" + item), executor);

            List<String> fallbackDefault = VirtualThreadUtil.parallelExecuteWithFallback(numbers,
                    item -> mayFail(item, "parallelExecuteWithFallback-default-"), "fallback-default");
            List<String> fallbackCustom = VirtualThreadUtil.parallelExecuteWithFallback(numbers,
                    item -> mayFail(item, "parallelExecuteWithFallback-custom-"), "fallback-custom", executor);

            VirtualThreadUtil.parallelForEach(numbers,
                    item -> sideEffects.add(currentThread("parallelForEach-default-" + item)));
            VirtualThreadUtil.parallelForEach(numbers,
                    item -> sideEffects.add(currentThread("parallelForEach-custom-" + item)), executor);

            return Dict.create()
                    .set("parallelExecute", executeDefault)
                    .set("parallelExecuteWithExecutor", executeCustom)
                    .set("parallelExecuteWithFallback", fallbackDefault)
                    .set("parallelExecuteWithFallbackAndExecutor", fallbackCustom)
                    .set("parallelForEach", sideEffects);
        } finally {
            VirtualThreadUtil.shutdown(executor, Duration.ofSeconds(5));
        }
    }

    /**
     * 演示异步兜底和超时控制。
     *
     * @return 超时和兜底执行结果
     */
    @GetMapping("/timeout")
    public Dict timeout() {
        ExecutorService executor = VirtualThreadUtil.newVirtualThreadExecutor("demo-timeout-");

        try {
            CompletableFuture<String> fallbackDefault = VirtualThreadUtil.supplyAsyncWithFallback(() -> {
                throw new IllegalStateException("模拟默认执行器任务失败");
            }, "fallback-default");
            CompletableFuture<String> fallbackCustom = VirtualThreadUtil.supplyAsyncWithFallback(() -> {
                throw new IllegalStateException("模拟指定执行器任务失败");
            }, "fallback-custom", executor);

            CompletableFuture<String> orTimeoutDefault = VirtualThreadUtil.supplyAsyncOrTimeout(() -> {
                sleep(Duration.ofMillis(30));
                return currentThread("supplyAsyncOrTimeout-default");
            }, Duration.ofSeconds(1));
            CompletableFuture<String> orTimeoutCustom = VirtualThreadUtil.supplyAsyncOrTimeout(() -> {
                sleep(Duration.ofMillis(30));
                return currentThread("supplyAsyncOrTimeout-custom");
            }, Duration.ofSeconds(1), executor);

            CompletableFuture<String> completeOnTimeoutDefault = VirtualThreadUtil.supplyAsyncCompleteOnTimeout(() -> {
                sleep(Duration.ofMillis(200));
                return currentThread("supplyAsyncCompleteOnTimeout-default-original");
            }, Duration.ofMillis(50), "timeout-fallback-default");
            CompletableFuture<String> completeOnTimeoutCustom = VirtualThreadUtil.supplyAsyncCompleteOnTimeout(() -> {
                sleep(Duration.ofMillis(200));
                return currentThread("supplyAsyncCompleteOnTimeout-custom-original");
            }, Duration.ofMillis(50), "timeout-fallback-custom", executor);

            String timeoutDefaultByUnit = VirtualThreadUtil.supplyAsyncWithTimeout(
                    () -> currentThread("supplyAsyncWithTimeout-long-default"), 1, TimeUnit.SECONDS);
            String timeoutCustomByUnit = VirtualThreadUtil.supplyAsyncWithTimeout(
                    () -> currentThread("supplyAsyncWithTimeout-long-custom"), 1, TimeUnit.SECONDS, executor);
            String timeoutDefaultByDuration = VirtualThreadUtil.supplyAsyncWithTimeout(
                    () -> currentThread("supplyAsyncWithTimeout-duration-default"), Duration.ofSeconds(1));
            String timeoutCustomByDuration = VirtualThreadUtil.supplyAsyncWithTimeout(
                    () -> currentThread("supplyAsyncWithTimeout-duration-custom"), Duration.ofSeconds(1), executor);

            return Dict.create()
                    .set("supplyAsyncWithFallback", VirtualThreadUtil.join(fallbackDefault))
                    .set("supplyAsyncWithFallbackAndExecutor", VirtualThreadUtil.join(fallbackCustom))
                    .set("supplyAsyncOrTimeout", VirtualThreadUtil.join(orTimeoutDefault))
                    .set("supplyAsyncOrTimeoutAndExecutor", VirtualThreadUtil.join(orTimeoutCustom))
                    .set("supplyAsyncCompleteOnTimeout", VirtualThreadUtil.join(completeOnTimeoutDefault))
                    .set("supplyAsyncCompleteOnTimeoutAndExecutor", VirtualThreadUtil.join(completeOnTimeoutCustom))
                    .set("supplyAsyncWithTimeoutLong", timeoutDefaultByUnit)
                    .set("supplyAsyncWithTimeoutLongAndExecutor", timeoutCustomByUnit)
                    .set("supplyAsyncWithTimeoutDuration", timeoutDefaultByDuration)
                    .set("supplyAsyncWithTimeoutDurationAndExecutor", timeoutCustomByDuration);
        } finally {
            VirtualThreadUtil.shutdown(executor, Duration.ofSeconds(5));
        }
    }

    /**
     * 演示 CompletableFuture 聚合、任意完成、首个成功和链式调用。
     *
     * @return Future 操作结果
     */
    @GetMapping("/future")
    public Dict future() {
        ExecutorService executor = VirtualThreadUtil.newVirtualThreadExecutor("demo-future-");
        List<String> voidMessages = new CopyOnWriteArrayList<>();

        try {
            List<CompletableFuture<String>> allFutures = List.of(
                    VirtualThreadUtil.supplyAsync(() -> currentThread("allOf-1")),
                    VirtualThreadUtil.supplyAsync(() -> currentThread("allOf-2"), executor)
            );
            List<String> allOf = VirtualThreadUtil.allOf(allFutures);

            CompletableFuture<Void> voidOne = VirtualThreadUtil.runAsync(() -> voidMessages.add(currentThread("allOfVoid-1")));
            CompletableFuture<Void> voidTwo = VirtualThreadUtil.runAsync(() -> voidMessages.add(currentThread("allOfVoid-2")), executor);
            VirtualThreadUtil.allOfVoid(List.of(voidOne, voidTwo));

            String anyOf = VirtualThreadUtil.anyOf(List.of(
                    VirtualThreadUtil.supplyAsync(() -> {
                        sleep(Duration.ofMillis(150));
                        return currentThread("anyOf-slow");
                    }),
                    VirtualThreadUtil.supplyAsync(() -> {
                        sleep(Duration.ofMillis(30));
                        return currentThread("anyOf-fast");
                    }, executor)
            ));

            String firstSuccessDefault = VirtualThreadUtil.join(VirtualThreadUtil.firstSuccess(List.of(
                    failedSupplier("firstSuccess-default-failed"),
                    () -> currentThread("firstSuccess-default-success")
            )));
            String firstSuccessCustom = VirtualThreadUtil.join(VirtualThreadUtil.firstSuccess(List.of(
                    failedSupplier("firstSuccess-custom-failed"),
                    () -> currentThread("firstSuccess-custom-success")
            ), executor));

            String chainDefault = VirtualThreadUtil.join(VirtualThreadUtil.chain(
                    () -> "chain-default",
                    value -> currentThread(value + "-next")
            ));
            String chainCustom = VirtualThreadUtil.join(VirtualThreadUtil.chain(
                    () -> "chain-custom",
                    value -> currentThread(value + "-next"),
                    executor
            ));

            CompletableFuture<String> joinedFuture = VirtualThreadUtil.supplyAsync(() -> currentThread("join"), executor);
            String join = VirtualThreadUtil.join(joinedFuture);

            return Dict.create()
                    .set("allOf", allOf)
                    .set("allOfVoid", voidMessages)
                    .set("anyOf", anyOf)
                    .set("firstSuccess", firstSuccessDefault)
                    .set("firstSuccessAndExecutor", firstSuccessCustom)
                    .set("chain", chainDefault)
                    .set("chainAndExecutor", chainCustom)
                    .set("join", join);
        } finally {
            VirtualThreadUtil.shutdown(executor, Duration.ofSeconds(5));
        }
    }

    /**
     * 演示 invokeAll 和 invokeAny 的全部重载用法。
     *
     * @param count 任务数量
     * @return invoke 系列执行结果
     */
    @GetMapping("/invoke")
    public Dict invoke(@RequestParam(defaultValue = "3") int count) {
        int size = safeCount(count);
        ExecutorService executor = VirtualThreadUtil.newVirtualThreadExecutor("demo-invoke-");

        try {
            List<String> invokeAllDefault = VirtualThreadUtil.invokeAll(callableTasks("invokeAll-default", size));
            List<String> invokeAllCustom = VirtualThreadUtil.invokeAll(callableTasks("invokeAll-custom", size), executor);
            List<String> invokeAllTimeoutDefault = VirtualThreadUtil.invokeAll(callableTasks("invokeAll-timeout-default", size), 1, TimeUnit.SECONDS);
            List<String> invokeAllTimeoutCustom = VirtualThreadUtil.invokeAll(callableTasks("invokeAll-timeout-custom", size), 1, TimeUnit.SECONDS, executor);
            List<String> invokeAllDurationDefault = VirtualThreadUtil.invokeAll(callableTasks("invokeAll-duration-default", size), Duration.ofSeconds(1));
            List<String> invokeAllDurationCustom = VirtualThreadUtil.invokeAll(callableTasks("invokeAll-duration-custom", size), Duration.ofSeconds(1), executor);

            String invokeAnyDefault = VirtualThreadUtil.invokeAny(callableTasks("invokeAny-default", size));
            String invokeAnyCustom = VirtualThreadUtil.invokeAny(callableTasks("invokeAny-custom", size), executor);
            String invokeAnyTimeoutDefault = VirtualThreadUtil.invokeAny(callableTasks("invokeAny-timeout-default", size), 1, TimeUnit.SECONDS);
            String invokeAnyTimeoutCustom = VirtualThreadUtil.invokeAny(callableTasks("invokeAny-timeout-custom", size), 1, TimeUnit.SECONDS, executor);
            String invokeAnyDurationDefault = VirtualThreadUtil.invokeAny(callableTasks("invokeAny-duration-default", size), Duration.ofSeconds(1));
            String invokeAnyDurationCustom = VirtualThreadUtil.invokeAny(callableTasks("invokeAny-duration-custom", size), Duration.ofSeconds(1), executor);

            return Dict.create()
                    .set("invokeAll", invokeAllDefault)
                    .set("invokeAllAndExecutor", invokeAllCustom)
                    .set("invokeAllTimeout", invokeAllTimeoutDefault)
                    .set("invokeAllTimeoutAndExecutor", invokeAllTimeoutCustom)
                    .set("invokeAllDuration", invokeAllDurationDefault)
                    .set("invokeAllDurationAndExecutor", invokeAllDurationCustom)
                    .set("invokeAny", invokeAnyDefault)
                    .set("invokeAnyAndExecutor", invokeAnyCustom)
                    .set("invokeAnyTimeout", invokeAnyTimeoutDefault)
                    .set("invokeAnyTimeoutAndExecutor", invokeAnyTimeoutCustom)
                    .set("invokeAnyDuration", invokeAnyDurationDefault)
                    .set("invokeAnyDurationAndExecutor", invokeAnyDurationCustom);
        } finally {
            VirtualThreadUtil.shutdown(executor, Duration.ofSeconds(5));
        }
    }

    /**
     * 演示关闭自定义执行器。
     *
     * @return 关闭结果
     */
    @PostMapping("/lifecycle/custom-shutdown")
    public Dict customShutdown() {
        ExecutorService executor = VirtualThreadUtil.newVirtualThreadExecutor("demo-shutdown-");
        boolean before = VirtualThreadUtil.isExecutorAvailable(executor);
        VirtualThreadUtil.shutdown(executor);
        boolean after = VirtualThreadUtil.isExecutorAvailable(executor);

        return Dict.create()
                .set("method", "shutdown(ExecutorService)")
                .set("availableBeforeShutdown", before)
                .set("availableAfterShutdown", after);
    }

    /**
     * 演示带等待时间关闭自定义执行器。
     *
     * @return 关闭结果
     */
    @PostMapping("/lifecycle/custom-shutdown-duration")
    public Dict customShutdownDuration() {
        ExecutorService executor = VirtualThreadUtil.newVirtualThreadExecutor("demo-shutdown-duration-");
        boolean before = VirtualThreadUtil.isExecutorAvailable(executor);
        VirtualThreadUtil.shutdown(executor, Duration.ofSeconds(3));
        boolean after = VirtualThreadUtil.isExecutorAvailable(executor);

        return Dict.create()
                .set("method", "shutdown(ExecutorService, Duration)")
                .set("availableBeforeShutdown", before)
                .set("availableAfterShutdown", after);
    }

    /**
     * 演示关闭默认执行器。
     *
     * @return 关闭结果
     */
    @PostMapping("/lifecycle/default-shutdown")
    public Dict defaultShutdown() {
        ExecutorService beforeExecutor = VirtualThreadUtil.defaultExecutor();
        boolean before = VirtualThreadUtil.isExecutorAvailable(beforeExecutor);
        VirtualThreadUtil.shutdown();

        ExecutorService afterExecutor = VirtualThreadUtil.defaultExecutor();
        boolean after = VirtualThreadUtil.isExecutorAvailable(afterExecutor);

        return Dict.create()
                .set("method", "shutdown()")
                .set("availableBeforeShutdown", before)
                .set("availableAfterDefaultExecutorRebuild", after)
                .set("note", "默认执行器关闭后再次调用 defaultExecutor() 会自动重建");
    }

    /**
     * 演示立即关闭默认执行器。
     *
     * @return 关闭结果
     */
    @PostMapping("/lifecycle/default-shutdown-now")
    public Dict defaultShutdownNow() {
        ExecutorService beforeExecutor = VirtualThreadUtil.defaultExecutor();
        boolean before = VirtualThreadUtil.isExecutorAvailable(beforeExecutor);

        Future<?> runningTask = beforeExecutor.submit(() -> {
            log.info("默认执行器 shutdownNow 演示任务开始执行");
            sleep(Duration.ofSeconds(10));
            log.info("默认执行器 shutdownNow 演示任务执行结束");
        });
        VirtualThreadUtil.shutdownNow();

        ExecutorService afterExecutor = VirtualThreadUtil.defaultExecutor();
        boolean after = VirtualThreadUtil.isExecutorAvailable(afterExecutor);

        return Dict.create()
                .set("method", "shutdownNow()")
                .set("availableBeforeShutdownNow", before)
                .set("runningTaskDoneOrCancelled", runningTask.isDone() || runningTask.isCancelled())
                .set("availableAfterDefaultExecutorRebuild", after)
                .set("note", "默认执行器立即关闭后再次调用 defaultExecutor() 会自动重建");
    }

    /**
     * 构建 Callable 示例任务。
     *
     * @param prefix 任务前缀
     * @param count  任务数量
     * @return Callable 集合
     */
    private List<Callable<String>> callableTasks(String prefix, int count) {
        List<Callable<String>> tasks = new ArrayList<>(count);
        for (int i = 1; i <= count; i++) {
            int index = i;
            tasks.add(() -> {
                sleep(Duration.ofMillis(20L * index));
                return currentThread(prefix + "-" + index);
            });
        }
        return tasks;
    }

    /**
     * 构建数字集合。
     *
     * @param count 数量
     * @return 数字集合
     */
    private List<Integer> numbers(int count) {
        List<Integer> numbers = new ArrayList<>(count);
        for (int i = 1; i <= count; i++) {
            numbers.add(i);
        }
        return numbers;
    }

    /**
     * 构建失败 Supplier。
     *
     * @param name 任务名称
     * @return 失败 Supplier
     */
    private Supplier<String> failedSupplier(String name) {
        return () -> {
            throw new IllegalStateException(name);
        };
    }

    /**
     * 模拟部分任务失败。
     *
     * @param item   任务编号
     * @param prefix 任务前缀
     * @return 任务结果
     */
    private String mayFail(Integer item, String prefix) {
        if (item != null && item % 2 == 0) {
            throw new IllegalStateException("模拟偶数任务失败，item=" + item);
        }
        return currentThread(prefix + item);
    }

    /**
     * 获取当前线程信息。
     *
     * @param label 标签
     * @return 当前线程信息
     */
    private String currentThread(String label) {
        return label + " | " + Thread.currentThread();
    }

    /**
     * 等待线程结束。
     *
     * @param thread 线程
     */
    private void joinThread(Thread thread) {
        try {
            thread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("等待虚拟线程结束被中断", e);
        }
    }

    /**
     * 获取 Future 结果。
     *
     * @param future Future 对象
     * @param <T>    返回值类型
     * @return Future 结果
     */
    private <T> T getFutureResult(Future<T> future) {
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("获取 Future 结果被中断", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("获取 Future 结果失败", e.getCause() == null ? e : e.getCause());
        }
    }

    /**
     * 安全限制任务数量。
     *
     * @param count 原始数量
     * @return 限制后的数量
     */
    private int safeCount(int count) {
        return Math.max(1, Math.min(count, 20));
    }

    /**
     * 睡眠指定时间。
     *
     * @param duration 睡眠时间
     */
    private void sleep(Duration duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("虚拟线程任务休眠被中断", e);
        }
    }
}