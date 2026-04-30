package io.github.atengk.utils.thread;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.LongFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * JDK 21 虚拟线程工具类。
 *
 * @author Ateng
 * @since 2026-04-30
 */
public final class VirtualThreadUtil {

    private static final AtomicLong NAME_COUNTER = new AtomicLong(1);
    private static final AtomicReference<VirtualThreadConfig> DEFAULT_CONFIG = new AtomicReference<>(VirtualThreadConfig.defaults());
    private static final CopyOnWriteArrayList<VirtualThreadContextProvider> CONTEXT_PROVIDERS = new CopyOnWriteArrayList<>();

    private VirtualThreadUtil() {
        throw new UnsupportedOperationException("VirtualThreadUtil cannot be instantiated");
    }

    /**
     * 启动一个虚拟线程执行任务。
     *
     * @param task 执行任务
     * @return 已启动的虚拟线程
     */
    public static Thread start(Runnable task) {
        return Thread.startVirtualThread(requireRunnable(task));
    }

    /**
     * 使用指定线程名启动一个虚拟线程执行任务。
     *
     * @param name 线程名
     * @param task 执行任务
     * @return 已启动的虚拟线程
     */
    public static Thread start(String name, Runnable task) {
        return Thread.ofVirtual().name(normalizeName(name)).start(requireRunnable(task));
    }

    /**
     * 使用线程名前缀和序号启动一个虚拟线程执行任务。
     *
     * @param prefix 线程名前缀
     * @param index  线程名序号
     * @param task   执行任务
     * @return 已启动的虚拟线程
     */
    public static Thread start(String prefix, int index, Runnable task) {
        if (index < 0) {
            throw new IllegalArgumentException("index must not be negative");
        }
        return start(name(prefix, index), task);
    }

    /**
     * 创建一个未启动的虚拟线程。
     *
     * @param task 执行任务
     * @return 未启动的虚拟线程
     */
    public static Thread unstarted(Runnable task) {
        return Thread.ofVirtual().unstarted(requireRunnable(task));
    }

    /**
     * 使用指定线程名创建一个未启动的虚拟线程。
     *
     * @param name 线程名
     * @param task 执行任务
     * @return 未启动的虚拟线程
     */
    public static Thread unstarted(String name, Runnable task) {
        return Thread.ofVirtual().name(normalizeName(name)).unstarted(requireRunnable(task));
    }

    /**
     * 创建默认虚拟线程工厂。
     *
     * @return 虚拟线程工厂
     */
    public static ThreadFactory factory() {
        return Thread.ofVirtual().factory();
    }

    /**
     * 创建带线程名前缀的虚拟线程工厂。
     *
     * @param prefix 线程名前缀
     * @return 虚拟线程工厂
     */
    public static ThreadFactory factory(String prefix) {
        return factory(prefix, 1);
    }

    /**
     * 创建带线程名前缀和起始序号的虚拟线程工厂。
     *
     * @param prefix     线程名前缀
     * @param startIndex 起始序号
     * @return 虚拟线程工厂
     */
    public static ThreadFactory factory(String prefix, long startIndex) {
        if (startIndex < 0) {
            throw new IllegalArgumentException("startIndex must not be negative");
        }
        return Thread.ofVirtual().name(normalizePrefix(prefix) + "-", startIndex).factory();
    }

    /**
     * 创建每个任务一个虚拟线程的执行器。
     *
     * @return 虚拟线程执行器
     */
    public static ExecutorService newExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    /**
     * 创建带线程名前缀的虚拟线程执行器。
     *
     * @param namePrefix 线程名前缀
     * @return 虚拟线程执行器
     */
    public static ExecutorService newExecutor(String namePrefix) {
        return Executors.newThreadPerTaskExecutor(factory(namePrefix));
    }

    /**
     * 使用指定线程工厂创建每任务一个线程的执行器。
     *
     * @param threadFactory 线程工厂
     * @return 执行器
     */
    public static ExecutorService newExecutor(ThreadFactory threadFactory) {
        return Executors.newThreadPerTaskExecutor(Objects.requireNonNull(threadFactory, "threadFactory must not be null"));
    }

    /**
     * 创建带线程名前缀的命名虚拟线程执行器。
     *
     * @param namePrefix 线程名前缀
     * @return 虚拟线程执行器
     */
    public static ExecutorService newNamedExecutor(String namePrefix) {
        return newExecutor(namePrefix);
    }

    /**
     * 创建可关闭的虚拟线程执行器。
     *
     * @return 虚拟线程执行器
     */
    public static ExecutorService newCloseableExecutor() {
        return newExecutor();
    }

    /**
     * 正常关闭执行器。
     *
     * @param executor 执行器
     */
    public static void shutdown(ExecutorService executor) {
        Objects.requireNonNull(executor, "executor must not be null").shutdown();
    }

    /**
     * 静默关闭执行器。
     *
     * @param executor 执行器
     */
    public static void shutdownQuietly(ExecutorService executor) {
        if (executor != null) {
            try {
                executor.shutdown();
            } catch (RuntimeException ignored) {
                // 静默关闭，不向外传播异常。
            }
        }
    }

    /**
     * 立即尝试关闭执行器。
     *
     * @param executor 执行器
     * @return 等待执行的任务集合
     */
    public static List<Runnable> shutdownNow(ExecutorService executor) {
        return Objects.requireNonNull(executor, "executor must not be null").shutdownNow();
    }

    /**
     * 等待执行器关闭完成。
     *
     * @param executor 执行器
     * @param timeout  超时时间
     * @return 是否在超时时间内关闭完成
     * @throws InterruptedException 当前线程被中断时抛出
     */
    public static boolean awaitTermination(ExecutorService executor, Duration timeout) throws InterruptedException {
        Objects.requireNonNull(executor, "executor must not be null");
        Duration actualTimeout = requirePositiveDuration(timeout, "timeout");
        return executor.awaitTermination(actualTimeout.toNanos(), TimeUnit.NANOSECONDS);
    }

    /**
     * 关闭执行器并等待关闭完成。
     *
     * @param executor 执行器
     * @param timeout  超时时间
     * @return 是否在超时时间内关闭完成
     * @throws InterruptedException 当前线程被中断时抛出
     */
    public static boolean shutdownAndAwait(ExecutorService executor, Duration timeout) throws InterruptedException {
        shutdown(executor);
        return awaitTermination(executor, timeout);
    }

    /**
     * 使用虚拟线程异步执行无返回任务。
     *
     * @param task 执行任务
     * @return 异步结果
     */
    public static CompletableFuture<Void> runAsync(Runnable task) {
        return completable(task);
    }

    /**
     * 使用指定线程名异步执行无返回任务。
     *
     * @param name 线程名
     * @param task 执行任务
     * @return 异步结果
     */
    public static CompletableFuture<Void> runAsync(String name, Runnable task) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        start(name, () -> completeRunnable(future, task));
        return future;
    }

    /**
     * 使用虚拟线程异步执行有返回任务。
     *
     * @param task 执行任务
     * @param <T>  返回类型
     * @return 异步结果
     */
    public static <T> CompletableFuture<T> supplyAsync(Callable<T> task) {
        CompletableFuture<T> future = new CompletableFuture<>();
        start(() -> completeCallable(future, task));
        return future;
    }

    /**
     * 使用指定线程名异步执行有返回任务。
     *
     * @param name 线程名
     * @param task 执行任务
     * @param <T>  返回类型
     * @return 异步结果
     */
    public static <T> CompletableFuture<T> supplyAsync(String name, Callable<T> task) {
        CompletableFuture<T> future = new CompletableFuture<>();
        start(name, () -> completeCallable(future, task));
        return future;
    }

    /**
     * 提交无返回任务并返回 Future。
     *
     * @param task 执行任务
     * @return Future 结果
     */
    public static Future<Void> future(Runnable task) {
        return future(toCallable(task));
    }

    /**
     * 提交有返回任务并返回 Future。
     *
     * @param task 执行任务
     * @param <T>  返回类型
     * @return Future 结果
     */
    public static <T> Future<T> future(Callable<T> task) {
        FutureTask<T> futureTask = new FutureTask<>(requireCallable(task));
        start(futureTask);
        return futureTask;
    }

    /**
     * 使用虚拟线程创建 CompletableFuture。
     *
     * @param task 执行任务
     * @return 异步结果
     */
    public static CompletableFuture<Void> completable(Runnable task) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        start(() -> completeRunnable(future, task));
        return future;
    }

    /**
     * 使用虚拟线程创建 CompletableFuture。
     *
     * @param supplier 结果提供器
     * @param <T>      返回类型
     * @return 异步结果
     */
    public static <T> CompletableFuture<T> completable(Supplier<T> supplier) {
        Objects.requireNonNull(supplier, "supplier must not be null");
        return supplyAsync(supplier::get);
    }

    /**
     * 使用指定执行器创建 CompletableFuture。
     *
     * @param executor 执行器
     * @param supplier 结果提供器
     * @param <T>      返回类型
     * @return 异步结果
     */
    public static <T> CompletableFuture<T> completable(Executor executor, Supplier<T> supplier) {
        Objects.requireNonNull(executor, "executor must not be null");
        Objects.requireNonNull(supplier, "supplier must not be null");
        return CompletableFuture.supplyAsync(supplier, executor);
    }

    /**
     * 批量执行任务并等待全部完成。
     *
     * @param tasks 任务集合
     * @param <T>   返回类型
     * @return Future 结果集合
     * @throws InterruptedException 当前线程被中断时抛出
     */
    public static <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        List<Callable<T>> actualTasks = copyCallables(tasks);
        try (ExecutorService executor = newExecutor()) {
            return executor.invokeAll(actualTasks);
        }
    }

    /**
     * 批量执行任务并设置整体超时时间。
     *
     * @param tasks   任务集合
     * @param timeout 超时时间
     * @param <T>     返回类型
     * @return Future 结果集合
     * @throws InterruptedException 当前线程被中断时抛出
     */
    public static <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, Duration timeout) throws InterruptedException {
        List<Callable<T>> actualTasks = copyCallables(tasks);
        Duration actualTimeout = requirePositiveDuration(timeout, "timeout");
        try (ExecutorService executor = newExecutor()) {
            return executor.invokeAll(actualTasks, actualTimeout.toNanos(), TimeUnit.NANOSECONDS);
        }
    }

    /**
     * 执行多个任务并返回最快成功的结果。
     *
     * @param tasks 任务集合
     * @param <T>   返回类型
     * @return 最快成功结果
     * @throws InterruptedException 当前线程被中断时抛出
     * @throws ExecutionException   任务执行失败时抛出
     */
    public static <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        List<Callable<T>> actualTasks = copyCallables(tasks);
        try (ExecutorService executor = newExecutor()) {
            return executor.invokeAny(actualTasks);
        }
    }

    /**
     * 执行多个任务并在超时时间内返回最快成功结果。
     *
     * @param tasks   任务集合
     * @param timeout 超时时间
     * @param <T>     返回类型
     * @return 最快成功结果
     * @throws InterruptedException 当前线程被中断时抛出
     * @throws ExecutionException   任务执行失败时抛出
     * @throws TimeoutException     执行超时时抛出
     */
    public static <T> T invokeAny(Collection<? extends Callable<T>> tasks, Duration timeout) throws InterruptedException, ExecutionException, TimeoutException {
        List<Callable<T>> actualTasks = copyCallables(tasks);
        Duration actualTimeout = requirePositiveDuration(timeout, "timeout");
        try (ExecutorService executor = newExecutor()) {
            return executor.invokeAny(actualTasks, actualTimeout.toNanos(), TimeUnit.NANOSECONDS);
        }
    }

    /**
     * 批量提交任务并返回 Future 集合。
     *
     * @param tasks 任务集合
     * @param <T>   返回类型
     * @return Future 集合
     */
    public static <T> List<Future<T>> submitAll(Collection<? extends Callable<T>> tasks) {
        return copyCallables(tasks).stream().map(VirtualThreadUtil::future).collect(Collectors.toList());
    }

    /**
     * 批量执行无返回任务。
     *
     * @param tasks 任务集合
     * @throws Exception 任务执行失败时抛出
     */
    public static void runAll(Collection<? extends Runnable> tasks) throws Exception {
        List<Callable<Void>> callables = copyRunnables(tasks).stream().map(VirtualThreadUtil::toCallable).collect(Collectors.toList());
        for (Future<Void> future : invokeAll(callables)) {
            future.get();
        }
    }

    /**
     * 批量执行无返回任务并设置超时时间。
     *
     * @param tasks   任务集合
     * @param timeout 超时时间
     * @throws Exception 任务执行失败或超时时抛出
     */
    public static void runAll(Collection<? extends Runnable> tasks, Duration timeout) throws Exception {
        List<Callable<Void>> callables = copyRunnables(tasks).stream().map(VirtualThreadUtil::toCallable).collect(Collectors.toList());
        for (Future<Void> future : invokeAll(callables, timeout)) {
            if (future.isCancelled()) {
                throw new TimeoutException("task timeout");
            }
            future.get();
        }
    }

    /**
     * 并发转换集合数据。
     *
     * @param source 源数据集合
     * @param mapper 转换函数
     * @param <T>    源数据类型
     * @param <R>    返回类型
     * @return 转换结果集合
     * @throws Exception 转换失败时抛出
     */
    public static <T, R> List<R> map(Collection<T> source, Function<T, R> mapper) throws Exception {
        Objects.requireNonNull(mapper, "mapper must not be null");
        List<T> values = copyValues(source, "source");
        List<Callable<R>> tasks = values.stream().map(value -> (Callable<R>) () -> mapper.apply(value)).collect(Collectors.toList());
        List<R> results = new ArrayList<>(tasks.size());
        for (Future<R> future : invokeAll(tasks)) {
            results.add(future.get());
        }
        return results;
    }

    /**
     * 并发转换集合数据并设置超时时间。
     *
     * @param source  源数据集合
     * @param mapper  转换函数
     * @param timeout 超时时间
     * @param <T>     源数据类型
     * @param <R>     返回类型
     * @return 转换结果集合
     * @throws Exception 转换失败或超时时抛出
     */
    public static <T, R> List<R> map(Collection<T> source, Function<T, R> mapper, Duration timeout) throws Exception {
        Objects.requireNonNull(mapper, "mapper must not be null");
        List<T> values = copyValues(source, "source");
        List<Callable<R>> tasks = values.stream().map(value -> (Callable<R>) () -> mapper.apply(value)).collect(Collectors.toList());
        List<R> results = new ArrayList<>(tasks.size());
        for (Future<R> future : invokeAll(tasks, timeout)) {
            if (future.isCancelled()) {
                throw new TimeoutException("task timeout");
            }
            results.add(future.get());
        }
        return results;
    }

    /**
     * 并发遍历处理集合。
     *
     * @param source   源数据集合
     * @param consumer 消费函数
     * @param <T>      源数据类型
     * @throws Exception 处理失败时抛出
     */
    public static <T> void forEach(Collection<T> source, Consumer<T> consumer) throws Exception {
        Objects.requireNonNull(consumer, "consumer must not be null");
        map(source, value -> {
            consumer.accept(value);
            return null;
        });
    }

    /**
     * 并发遍历处理集合并设置超时时间。
     *
     * @param source   源数据集合
     * @param consumer 消费函数
     * @param timeout  超时时间
     * @param <T>      源数据类型
     * @throws Exception 处理失败或超时时抛出
     */
    public static <T> void forEach(Collection<T> source, Consumer<T> consumer, Duration timeout) throws Exception {
        Objects.requireNonNull(consumer, "consumer must not be null");
        map(source, value -> {
            consumer.accept(value);
            return null;
        }, timeout);
    }

    /**
     * 在指定超时时间内执行无返回任务。
     *
     * @param task    执行任务
     * @param timeout 超时时间
     * @throws Exception 任务执行失败或超时时抛出
     */
    public static void runWithTimeout(Runnable task, Duration timeout) throws Exception {
        callWithTimeout(toCallable(task), timeout);
    }

    /**
     * 在指定超时时间内执行有返回任务。
     *
     * @param task    执行任务
     * @param timeout 超时时间
     * @param <T>     返回类型
     * @return 执行结果
     * @throws Exception 任务执行失败或超时时抛出
     */
    public static <T> T callWithTimeout(Callable<T> task, Duration timeout) throws Exception {
        Duration actualTimeout = requirePositiveDuration(timeout, "timeout");
        Future<T> future = future(task);
        try {
            return future.get(actualTimeout.toNanos(), TimeUnit.NANOSECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw e;
        } catch (ExecutionException e) {
            throwException(unwrap(e));
            return null;
        }
    }

    /**
     * 在指定超时时间内执行结果提供器。
     *
     * @param supplier 结果提供器
     * @param timeout  超时时间
     * @param <T>      返回类型
     * @return 执行结果
     * @throws Exception 任务执行失败或超时时抛出
     */
    public static <T> T supplyWithTimeout(Supplier<T> supplier, Duration timeout) throws Exception {
        Objects.requireNonNull(supplier, "supplier must not be null");
        return callWithTimeout(supplier::get, timeout);
    }

    /**
     * 执行任务并在超时时返回默认值。
     *
     * @param supplier     结果提供器
     * @param defaultValue 默认值
     * @param timeout      超时时间
     * @param <T>          返回类型
     * @return 执行结果或默认值
     */
    public static <T> T completeOnTimeout(Supplier<T> supplier, T defaultValue, Duration timeout) {
        try {
            return supplyWithTimeout(supplier, timeout);
        } catch (TimeoutException e) {
            return defaultValue;
        } catch (Exception e) {
            throw rethrow(e);
        }
    }

    /**
     * 执行任务并在超时时抛出 TimeoutException。
     *
     * @param supplier 结果提供器
     * @param timeout  超时时间
     * @param <T>      返回类型
     * @return 执行结果
     * @throws TimeoutException 执行超时时抛出
     */
    public static <T> T orTimeout(Supplier<T> supplier, Duration timeout) throws TimeoutException {
        try {
            return supplyWithTimeout(supplier, timeout);
        } catch (TimeoutException e) {
            throw e;
        } catch (Exception e) {
            throw rethrow(e);
        }
    }

    /**
     * 等待 Future 并在超时时取消任务。
     *
     * @param future  Future 对象
     * @param timeout 超时时间
     * @return 是否发生超时取消
     * @throws InterruptedException 当前线程被中断时抛出
     * @throws ExecutionException   任务执行失败时抛出
     */
    public static boolean cancelOnTimeout(Future<?> future, Duration timeout) throws InterruptedException, ExecutionException {
        Objects.requireNonNull(future, "future must not be null");
        Duration actualTimeout = requirePositiveDuration(timeout, "timeout");
        try {
            future.get(actualTimeout.toNanos(), TimeUnit.NANOSECONDS);
            return false;
        } catch (TimeoutException e) {
            future.cancel(true);
            return true;
        }
    }

    /**
     * 带超时时间等待 Future 结果。
     *
     * @param future  Future 对象
     * @param timeout 超时时间
     * @param <T>     返回类型
     * @return Future 结果
     * @throws InterruptedException 当前线程被中断时抛出
     * @throws ExecutionException   任务执行失败时抛出
     * @throws TimeoutException     等待超时时抛出
     */
    public static <T> T await(Future<T> future, Duration timeout) throws InterruptedException, ExecutionException, TimeoutException {
        return get(future, timeout);
    }

    /**
     * 静默等待 Future 结果。
     *
     * @param future  Future 对象
     * @param timeout 超时时间
     * @param <T>     返回类型
     * @return 成功结果可选对象
     */
    public static <T> Optional<T> awaitQuietly(Future<T> future, Duration timeout) {
        try {
            return Optional.ofNullable(await(future, timeout));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * 安全执行无返回任务。
     *
     * @param task 执行任务
     * @return 是否执行成功
     */
    public static boolean runSafe(Runnable task) {
        return runSafe(task, null);
    }

    /**
     * 安全执行无返回任务并处理异常。
     *
     * @param task         执行任务
     * @param errorHandler 异常处理器
     * @return 是否执行成功
     */
    public static boolean runSafe(Runnable task, Consumer<Throwable> errorHandler) {
        try {
            requireRunnable(task).run();
            return true;
        } catch (Throwable e) {
            if (errorHandler != null) {
                errorHandler.accept(e);
            }
            return false;
        }
    }

    /**
     * 安全执行有返回任务。
     *
     * @param task 执行任务
     * @param <T>  返回类型
     * @return 成功结果可选对象
     */
    public static <T> Optional<T> callSafe(Callable<T> task) {
        try {
            return Optional.ofNullable(requireCallable(task).call());
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * 安全执行有返回任务并在异常时返回默认值。
     *
     * @param task         执行任务
     * @param defaultValue 默认值
     * @param <T>          返回类型
     * @return 执行结果或默认值
     */
    public static <T> T callSafe(Callable<T> task, T defaultValue) {
        try {
            return requireCallable(task).call();
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * 安全执行有返回任务并在异常时执行回退函数。
     *
     * @param task     执行任务
     * @param fallback 回退函数
     * @param <T>      返回类型
     * @return 执行结果或回退结果
     */
    public static <T> T callSafe(Callable<T> task, Function<Throwable, T> fallback) {
        Objects.requireNonNull(fallback, "fallback must not be null");
        try {
            return requireCallable(task).call();
        } catch (Throwable e) {
            return fallback.apply(e);
        }
    }

    /**
     * 解包常见包装异常。
     *
     * @param throwable 异常对象
     * @return 解包后的异常对象
     */
    public static Throwable unwrap(Throwable throwable) {
        Throwable current = Objects.requireNonNull(throwable, "throwable must not be null");
        while ((current instanceof ExecutionException || current instanceof CompletionException) && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    /**
     * 判断异常是否为超时异常。
     *
     * @param throwable 异常对象
     * @return 是否为超时异常
     */
    public static boolean isTimeout(Throwable throwable) {
        return unwrapOrNull(throwable) instanceof TimeoutException;
    }

    /**
     * 判断异常是否为中断异常。
     *
     * @param throwable 异常对象
     * @return 是否为中断异常
     */
    public static boolean isInterrupted(Throwable throwable) {
        return unwrapOrNull(throwable) instanceof InterruptedException;
    }

    /**
     * 将异常转换为运行时异常。
     *
     * @param throwable 异常对象
     * @return 运行时异常
     */
    public static RuntimeException rethrow(Throwable throwable) {
        Throwable actual = unwrap(throwable);
        if (actual instanceof RuntimeException runtimeException) {
            return runtimeException;
        }
        if (actual instanceof Error error) {
            throw error;
        }
        return new RuntimeException(actual);
    }

    /**
     * 批量执行任务并收集成功结果。
     *
     * @param tasks 任务集合
     * @param <T>   返回类型
     * @return 成功结果集合
     */
    public static <T> List<T> collect(Collection<? extends Callable<T>> tasks) {
        return collectSuccess(tasks);
    }

    /**
     * 批量执行任务并收集全部结果。
     *
     * @param tasks 任务集合
     * @param <T>   返回类型
     * @return 批量任务结果
     */
    public static <T> VirtualBatchResult<T> collectAll(Collection<? extends Callable<T>> tasks) {
        List<Callable<T>> actualTasks = copyCallables(tasks);
        Instant start = Instant.now();
        List<Callable<VirtualTaskResult<T>>> wrappedTasks = new ArrayList<>(actualTasks.size());
        for (int i = 0; i < actualTasks.size(); i++) {
            int index = i;
            Callable<T> task = actualTasks.get(i);
            wrappedTasks.add(() -> executeAsResult(index, task));
        }
        try {
            List<VirtualTaskResult<T>> results = new ArrayList<>(wrappedTasks.size());
            for (Future<VirtualTaskResult<T>> future : invokeAll(wrappedTasks)) {
                results.add(future.get());
            }
            return new VirtualBatchResult<>(results, Duration.between(start, Instant.now()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw rethrow(e);
        }
    }

    /**
     * 批量执行任务并只收集成功结果。
     *
     * @param tasks 任务集合
     * @param <T>   返回类型
     * @return 成功结果集合
     */
    public static <T> List<T> collectSuccess(Collection<? extends Callable<T>> tasks) {
        return collectAll(tasks).getSuccessValues();
    }

    /**
     * 批量执行任务并只收集失败结果。
     *
     * @param tasks 任务集合
     * @param <T>   返回类型
     * @return 失败结果集合
     */
    public static <T> List<VirtualTaskResult<T>> collectFailure(Collection<? extends Callable<T>> tasks) {
        return collectAll(tasks).getFailureResults();
    }

    /**
     * 带超时时间批量执行任务并收集全部结果。
     *
     * @param tasks   任务集合
     * @param timeout 超时时间
     * @param <T>     返回类型
     * @return 批量任务结果
     */
    public static <T> VirtualBatchResult<T> collectWithTimeout(Collection<? extends Callable<T>> tasks, Duration timeout) {
        List<Callable<T>> actualTasks = copyCallables(tasks);
        requirePositiveDuration(timeout, "timeout");
        Instant start = Instant.now();
        List<Callable<VirtualTaskResult<T>>> wrappedTasks = new ArrayList<>(actualTasks.size());
        for (int i = 0; i < actualTasks.size(); i++) {
            int index = i;
            Callable<T> task = actualTasks.get(i);
            wrappedTasks.add(() -> executeAsResult(index, task));
        }
        try {
            List<Future<VirtualTaskResult<T>>> futures = invokeAll(wrappedTasks, timeout);
            List<VirtualTaskResult<T>> results = new ArrayList<>(futures.size());
            for (int i = 0; i < futures.size(); i++) {
                Future<VirtualTaskResult<T>> future = futures.get(i);
                if (future.isCancelled()) {
                    results.add(VirtualTaskResult.timeout(i, new TimeoutException("task timeout"), Duration.between(start, Instant.now())));
                } else {
                    results.add(future.get());
                }
            }
            return new VirtualBatchResult<>(results, Duration.between(start, Instant.now()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw rethrow(e);
        }
    }

    /**
     * 批量执行任务并按成功失败分组。
     *
     * @param tasks 任务集合
     * @param <T>   返回类型
     * @return 批量任务结果
     */
    public static <T> VirtualBatchResult<T> partition(Collection<? extends Callable<T>> tasks) {
        return collectAll(tasks);
    }

    /**
     * 执行多个任务并返回第一个成功结果。
     *
     * @param tasks 任务集合
     * @param <T>   返回类型
     * @return 第一个成功结果
     */
    public static <T> T firstSuccess(Collection<? extends Callable<T>> tasks) {
        return collectSuccess(tasks).stream().findFirst().orElseThrow(() -> new NoSuchElementException("no successful task"));
    }

    /**
     * 执行多个任务并返回第一个成功结果，全部失败时返回默认值。
     *
     * @param tasks        任务集合
     * @param defaultValue 默认值
     * @param <T>          返回类型
     * @return 第一个成功结果或默认值
     */
    public static <T> T firstSuccessOrDefault(Collection<? extends Callable<T>> tasks, T defaultValue) {
        return collectSuccess(tasks).stream().findFirst().orElse(defaultValue);
    }

    /**
     * 限制并发数执行无返回任务。
     *
     * @param tasks       任务集合
     * @param concurrency 最大并发数
     * @throws Exception 任务执行失败时抛出
     */
    public static void runWithLimit(Collection<? extends Runnable> tasks, int concurrency) throws Exception {
        List<Callable<Void>> callables = copyRunnables(tasks).stream().map(VirtualThreadUtil::toCallable).collect(Collectors.toList());
        callWithLimit(callables, concurrency);
    }

    /**
     * 限制并发数执行有返回任务。
     *
     * @param tasks       任务集合
     * @param concurrency 最大并发数
     * @param <T>         返回类型
     * @return 返回结果集合
     * @throws Exception 任务执行失败时抛出
     */
    public static <T> List<T> callWithLimit(Collection<? extends Callable<T>> tasks, int concurrency) throws Exception {
        int permits = requirePositive(concurrency, "concurrency");
        Semaphore semaphore = new Semaphore(permits);
        List<Callable<T>> wrapped = copyCallables(tasks).stream().map(task -> (Callable<T>) () -> withSemaphore(semaphore, task)).collect(Collectors.toList());
        List<T> results = new ArrayList<>(wrapped.size());
        for (Future<T> future : invokeAll(wrapped)) {
            results.add(future.get());
        }
        return results;
    }

    /**
     * 限制并发数转换集合数据。
     *
     * @param source      源数据集合
     * @param mapper      转换函数
     * @param concurrency 最大并发数
     * @param <T>         源数据类型
     * @param <R>         返回类型
     * @return 转换结果集合
     * @throws Exception 转换失败时抛出
     */
    public static <T, R> List<R> mapWithLimit(Collection<T> source, Function<T, R> mapper, int concurrency) throws Exception {
        Objects.requireNonNull(mapper, "mapper must not be null");
        int permits = requirePositive(concurrency, "concurrency");
        Semaphore semaphore = new Semaphore(permits);
        List<T> values = copyValues(source, "source");
        List<Callable<R>> tasks = values.stream().map(value -> (Callable<R>) () -> withSemaphore(semaphore, () -> mapper.apply(value))).collect(Collectors.toList());
        List<R> results = new ArrayList<>(tasks.size());
        for (Future<R> future : invokeAll(tasks)) {
            results.add(future.get());
        }
        return results;
    }

    /**
     * 限制并发数遍历处理集合。
     *
     * @param source      源数据集合
     * @param consumer    消费函数
     * @param concurrency 最大并发数
     * @param <T>         源数据类型
     * @throws Exception 处理失败时抛出
     */
    public static <T> void forEachWithLimit(Collection<T> source, Consumer<T> consumer, int concurrency) throws Exception {
        Objects.requireNonNull(consumer, "consumer must not be null");
        mapWithLimit(source, value -> {
            consumer.accept(value);
            return null;
        }, concurrency);
    }

    /**
     * 使用指定许可数的信号量执行无返回任务。
     *
     * @param permits 许可数
     * @param task    执行任务
     * @throws InterruptedException 获取许可时被中断抛出
     */
    public static void withSemaphore(int permits, Runnable task) throws InterruptedException {
        withSemaphore(new Semaphore(requirePositive(permits, "permits")), task);
    }

    /**
     * 使用指定信号量执行无返回任务。
     *
     * @param semaphore 信号量
     * @param task      执行任务
     * @throws InterruptedException 获取许可时被中断抛出
     */
    public static void withSemaphore(Semaphore semaphore, Runnable task) throws InterruptedException {
        Objects.requireNonNull(semaphore, "semaphore must not be null");
        Runnable actualTask = requireRunnable(task);
        semaphore.acquire();
        try {
            actualTask.run();
        } finally {
            semaphore.release();
        }
    }

    /**
     * 使用指定信号量执行有返回任务。
     *
     * @param semaphore 信号量
     * @param task      执行任务
     * @param <T>       返回类型
     * @return 执行结果
     * @throws Exception 任务执行失败时抛出
     */
    public static <T> T withSemaphore(Semaphore semaphore, Callable<T> task) throws Exception {
        Objects.requireNonNull(semaphore, "semaphore must not be null");
        Callable<T> actualTask = requireCallable(task);
        semaphore.acquire();
        try {
            return actualTask.call();
        } finally {
            semaphore.release();
        }
    }

    /**
     * 限制并发数执行任务。
     *
     * @param tasks       任务集合
     * @param concurrency 最大并发数
     * @param <T>         返回类型
     * @return 返回结果集合
     * @throws Exception 任务执行失败时抛出
     */
    public static <T> List<T> rateLimit(Collection<? extends Callable<T>> tasks, int concurrency) throws Exception {
        return callWithLimit(tasks, concurrency);
    }

    /**
     * 注册上下文提供者。
     *
     * @param provider 上下文提供者
     */
    public static void registerContextProvider(VirtualThreadContextProvider provider) {
        CONTEXT_PROVIDERS.addIfAbsent(Objects.requireNonNull(provider, "provider must not be null"));
    }

    /**
     * 移除上下文提供者。
     *
     * @param provider 上下文提供者
     * @return 是否移除成功
     */
    public static boolean unregisterContextProvider(VirtualThreadContextProvider provider) {
        return CONTEXT_PROVIDERS.remove(Objects.requireNonNull(provider, "provider must not be null"));
    }

    /**
     * 清空上下文提供者。
     */
    public static void clearContextProviders() {
        CONTEXT_PROVIDERS.clear();
    }

    /**
     * 包装无返回任务并传递已注册的上下文。
     *
     * @param task 执行任务
     * @return 包装后的任务
     */
    public static Runnable wrap(Runnable task) {
        Runnable actualTask = requireRunnable(task);
        Object context = captureContext();
        return () -> {
            restoreContext(context);
            try {
                actualTask.run();
            } finally {
                clearContext();
            }
        };
    }

    /**
     * 包装有返回任务并传递已注册的上下文。
     *
     * @param task 执行任务
     * @param <T>  返回类型
     * @return 包装后的任务
     */
    public static <T> Callable<T> wrap(Callable<T> task) {
        Callable<T> actualTask = requireCallable(task);
        Object context = captureContext();
        return () -> {
            restoreContext(context);
            try {
                return actualTask.call();
            } finally {
                clearContext();
            }
        };
    }

    /**
     * 批量包装任务并传递已注册的上下文。
     *
     * @param tasks 任务集合
     * @param <T>   返回类型
     * @return 包装后的任务集合
     */
    public static <T> List<Callable<T>> wrap(Collection<? extends Callable<T>> tasks) {
        return copyCallables(tasks).stream().map(VirtualThreadUtil::wrap).collect(Collectors.toList());
    }

    /**
     * 捕获当前线程上下文。
     *
     * @return 上下文快照
     */
    public static Object captureContext() {
        List<ContextEntry> entries = new ArrayList<>(CONTEXT_PROVIDERS.size());
        for (VirtualThreadContextProvider provider : CONTEXT_PROVIDERS) {
            entries.add(new ContextEntry(provider, provider.capture()));
        }
        return new ContextSnapshot(entries);
    }

    /**
     * 将上下文快照恢复到当前线程。
     *
     * @param context 上下文快照
     */
    public static void restoreContext(Object context) {
        if (context == null) {
            return;
        }
        if (!(context instanceof ContextSnapshot snapshot)) {
            throw new IllegalArgumentException("context must be captured by VirtualThreadUtil.captureContext");
        }
        for (ContextEntry entry : snapshot.entries()) {
            entry.provider().restore(entry.context());
        }
    }

    /**
     * 清理已注册上下文提供者对应的当前线程上下文。
     */
    public static void clearContext() {
        for (VirtualThreadContextProvider provider : CONTEXT_PROVIDERS) {
            provider.clear();
        }
    }

    /**
     * 带上下文执行无返回任务。
     *
     * @param task 执行任务
     */
    public static void runWithContext(Runnable task) {
        wrap(task).run();
    }

    /**
     * 带上下文执行有返回任务。
     *
     * @param task 执行任务
     * @param <T>  返回类型
     * @return 执行结果
     * @throws Exception 任务执行失败时抛出
     */
    public static <T> T callWithContext(Callable<T> task) throws Exception {
        return wrap(task).call();
    }

    /**
     * 包装任务并传递已注册上下文，名称保留用于适配 MDC 场景。
     *
     * @param task 执行任务
     * @return 包装后的任务
     */
    public static Runnable withMdc(Runnable task) {
        return wrap(task);
    }

    /**
     * 包装任务并传递已注册上下文，名称保留用于适配 MDC 场景。
     *
     * @param task 执行任务
     * @param <T>  返回类型
     * @return 包装后的任务
     */
    public static <T> Callable<T> withMdc(Callable<T> task) {
        return wrap(task);
    }

    /**
     * 创建适合注册为 Spring TaskExecutor Bean 的虚拟线程执行器。
     *
     * @return 虚拟线程执行器
     */
    public static ExecutorService taskExecutor() {
        return newExecutor(defaultThreadNamePrefix());
    }

    /**
     * 创建适合注册为 Spring TaskExecutor Bean 的命名虚拟线程执行器。
     *
     * @param namePrefix 线程名前缀
     * @return 虚拟线程执行器
     */
    public static ExecutorService taskExecutor(String namePrefix) {
        return newExecutor(namePrefix);
    }

    /**
     * 创建适合注册为 Spring Async Executor Bean 的虚拟线程执行器。
     *
     * @return 虚拟线程执行器
     */
    public static ExecutorService asyncExecutor() {
        return newExecutor("vt-async");
    }

    /**
     * 创建应用级虚拟线程执行器。
     *
     * @return 虚拟线程执行器
     */
    public static ExecutorService applicationExecutor() {
        return newExecutor("vt-app");
    }

    /**
     * 创建调度任务执行器。
     *
     * @return 调度任务执行器
     */
    public static ScheduledExecutorService scheduledExecutor() {
        return Executors.newScheduledThreadPool(defaultConcurrency(), factory("vt-scheduled"));
    }

    /**
     * 装饰无返回任务并传递上下文。
     *
     * @param task 执行任务
     * @return 装饰后的任务
     */
    public static Runnable decorate(Runnable task) {
        return wrap(task);
    }

    /**
     * 装饰有返回任务并传递上下文。
     *
     * @param task 执行任务
     * @param <T>  返回类型
     * @return 装饰后的任务
     */
    public static <T> Callable<T> decorate(Callable<T> task) {
        return wrap(task);
    }

    /**
     * 包装请求上下文任务。
     *
     * @param task 执行任务
     * @return 包装后的任务
     */
    public static Runnable withRequestContext(Runnable task) {
        return wrap(task);
    }

    /**
     * 包装安全上下文任务。
     *
     * @param task 执行任务
     * @return 包装后的任务
     */
    public static Runnable withSecurityContext(Runnable task) {
        return wrap(task);
    }

    /**
     * 包装租户上下文任务。
     *
     * @param task 执行任务
     * @return 包装后的任务
     */
    public static Runnable withTenantContext(Runnable task) {
        return wrap(task);
    }

    /**
     * 并发执行所有结果提供器并返回结果。
     *
     * @param suppliers 结果提供器集合
     * @param <T>       返回类型
     * @return 返回结果集合
     * @throws Exception 任务执行失败时抛出
     */
    public static <T> List<T> allOf(Collection<? extends Supplier<T>> suppliers) throws Exception {
        return parallel(suppliers);
    }

    /**
     * 并发执行结果提供器并返回任意一个完成结果。
     *
     * @param suppliers 结果提供器集合
     * @param <T>       返回类型
     * @return 任意一个完成结果
     * @throws Exception 任务执行失败时抛出
     */
    public static <T> T anyOf(Collection<? extends Supplier<T>> suppliers) throws Exception {
        List<Callable<T>> tasks = copySuppliers(suppliers).stream().map(supplier -> (Callable<T>) supplier::get).collect(Collectors.toList());
        return invokeAny(tasks);
    }

    /**
     * 多任务竞速并返回最快成功结果。
     *
     * @param tasks 任务集合
     * @param <T>   返回类型
     * @return 最快成功结果
     * @throws Exception 任务执行失败时抛出
     */
    public static <T> T race(Collection<? extends Callable<T>> tasks) throws Exception {
        return invokeAny(tasks);
    }

    /**
     * 顺序执行结果提供器。
     *
     * @param suppliers 结果提供器集合
     * @param <T>       返回类型
     * @return 返回结果集合
     */
    public static <T> List<T> sequence(Collection<? extends Supplier<T>> suppliers) {
        List<Supplier<T>> actualSuppliers = copySuppliers(suppliers);
        List<T> results = new ArrayList<>(actualSuppliers.size());
        for (Supplier<T> supplier : actualSuppliers) {
            results.add(supplier.get());
        }
        return results;
    }

    /**
     * 并行执行结果提供器。
     *
     * @param suppliers 结果提供器集合
     * @param <T>       返回类型
     * @return 返回结果集合
     * @throws Exception 任务执行失败时抛出
     */
    public static <T> List<T> parallel(Collection<? extends Supplier<T>> suppliers) throws Exception {
        List<Callable<T>> tasks = copySuppliers(suppliers).stream().map(supplier -> (Callable<T>) supplier::get).collect(Collectors.toList());
        List<T> results = new ArrayList<>(tasks.size());
        for (Future<T> future : invokeAll(tasks)) {
            results.add(future.get());
        }
        return results;
    }

    /**
     * 并行映射集合数据。
     *
     * @param source 源数据集合
     * @param mapper 映射函数
     * @param <T>    源数据类型
     * @param <R>    返回类型
     * @return 映射结果集合
     * @throws Exception 映射失败时抛出
     */
    public static <T, R> List<R> parallelMap(Collection<T> source, Function<T, R> mapper) throws Exception {
        return map(source, mapper);
    }

    /**
     * 并行映射并展开集合结果。
     *
     * @param source 源数据集合
     * @param mapper 映射函数
     * @param <T>    源数据类型
     * @param <R>    返回类型
     * @return 展开后的结果集合
     * @throws Exception 映射失败时抛出
     */
    public static <T, R> List<R> parallelFlatMap(Collection<T> source, Function<T, Collection<R>> mapper) throws Exception {
        List<Collection<R>> collections = map(source, mapper);
        List<R> results = new ArrayList<>();
        for (Collection<R> collection : collections) {
            if (collection != null) {
                results.addAll(collection);
            }
        }
        return results;
    }

    /**
     * 串联执行两个任务。
     *
     * @param first 第一个任务
     * @param next  下一个任务
     * @param <T>   第一个任务返回类型
     * @param <R>   最终返回类型
     * @return 最终结果
     */
    public static <T, R> R then(Supplier<T> first, Function<T, R> next) {
        Objects.requireNonNull(first, "first must not be null");
        Objects.requireNonNull(next, "next must not be null");
        return next.apply(first.get());
    }

    /**
     * 并行执行两个任务并组合结果。
     *
     * @param first    第一个任务
     * @param second   第二个任务
     * @param combiner 结果组合函数
     * @param <A>      第一个任务返回类型
     * @param <B>      第二个任务返回类型
     * @param <R>      最终返回类型
     * @return 组合结果
     * @throws Exception 任务执行失败时抛出
     */
    public static <A, B, R> R combine(Supplier<A> first, Supplier<B> second, BiFunction<A, B, R> combiner) throws Exception {
        Objects.requireNonNull(first, "first must not be null");
        Objects.requireNonNull(second, "second must not be null");
        Objects.requireNonNull(combiner, "combiner must not be null");
        CompletableFuture<A> firstFuture = completable(first);
        CompletableFuture<B> secondFuture = completable(second);
        return combiner.apply(firstFuture.get(), secondFuture.get());
    }

    /**
     * 取消单个任务。
     *
     * @param future Future 对象
     * @return 是否取消成功
     */
    public static boolean cancel(Future<?> future) {
        return future != null && future.cancel(true);
    }

    /**
     * 批量取消任务。
     *
     * @param futures Future 集合
     * @return 成功发起取消的数量
     */
    public static int cancelAll(Collection<? extends Future<?>> futures) {
        List<? extends Future<?>> actualFutures = copyValues(futures, "futures");
        int count = 0;
        for (Future<?> future : actualFutures) {
            if (cancel(future)) {
                count++;
            }
        }
        return count;
    }

    /**
     * 静默取消单个任务。
     *
     * @param future Future 对象
     * @return 是否取消成功
     */
    public static boolean cancelQuietly(Future<?> future) {
        try {
            return cancel(future);
        } catch (RuntimeException e) {
            return false;
        }
    }

    /**
     * 静默批量取消任务。
     *
     * @param futures Future 集合
     * @return 成功发起取消的数量
     */
    public static int cancelAllQuietly(Collection<? extends Future<?>> futures) {
        try {
            return cancelAll(futures);
        } catch (RuntimeException e) {
            return 0;
        }
    }

    /**
     * 中断指定线程。
     *
     * @param thread 线程对象
     */
    public static void interrupt(Thread thread) {
        Objects.requireNonNull(thread, "thread must not be null").interrupt();
    }

    /**
     * 静默中断指定线程。
     *
     * @param thread 线程对象
     */
    public static void interruptQuietly(Thread thread) {
        if (thread != null) {
            try {
                thread.interrupt();
            } catch (RuntimeException ignored) {
                // 静默中断，不向外传播异常。
            }
        }
    }

    /**
     * 判断任务是否已取消。
     *
     * @param future Future 对象
     * @return 是否已取消
     */
    public static boolean isCancelled(Future<?> future) {
        return future != null && future.isCancelled();
    }

    /**
     * 判断任务是否已完成。
     *
     * @param future Future 对象
     * @return 是否已完成
     */
    public static boolean isDone(Future<?> future) {
        return future != null && future.isDone();
    }

    /**
     * 判断当前线程是否已中断。
     *
     * @return 是否已中断
     */
    public static boolean isInterrupted() {
        return Thread.currentThread().isInterrupted();
    }

    /**
     * 检查当前线程中断状态，已中断时抛出 InterruptedException。
     *
     * @throws InterruptedException 当前线程已中断时抛出
     */
    public static void checkInterrupted() throws InterruptedException {
        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedException("current thread has been interrupted");
        }
    }

    /**
     * 等待线程执行完成。
     *
     * @param thread 线程对象
     * @throws InterruptedException 当前线程被中断时抛出
     */
    public static void join(Thread thread) throws InterruptedException {
        Objects.requireNonNull(thread, "thread must not be null").join();
    }

    /**
     * 带超时时间等待线程执行完成。
     *
     * @param thread  线程对象
     * @param timeout 超时时间
     * @return 线程是否已结束
     * @throws InterruptedException 当前线程被中断时抛出
     */
    public static boolean join(Thread thread, Duration timeout) throws InterruptedException {
        Objects.requireNonNull(thread, "thread must not be null");
        Duration actualTimeout = requirePositiveDuration(timeout, "timeout");
        thread.join(actualTimeout.toMillis());
        return !thread.isAlive();
    }

    /**
     * 等待多个线程执行完成。
     *
     * @param threads 线程集合
     * @throws InterruptedException 当前线程被中断时抛出
     */
    public static void joinAll(Collection<? extends Thread> threads) throws InterruptedException {
        for (Thread thread : copyValues(threads, "threads")) {
            join(thread);
        }
    }

    /**
     * 带整体超时时间等待多个线程执行完成。
     *
     * @param threads 线程集合
     * @param timeout 超时时间
     * @return 是否全部结束
     * @throws InterruptedException 当前线程被中断时抛出
     */
    public static boolean joinAll(Collection<? extends Thread> threads, Duration timeout) throws InterruptedException {
        List<? extends Thread> actualThreads = copyValues(threads, "threads");
        Duration actualTimeout = requirePositiveDuration(timeout, "timeout");
        long deadline = System.nanoTime() + actualTimeout.toNanos();
        for (Thread thread : actualThreads) {
            long remaining = deadline - System.nanoTime();
            if (remaining <= 0) {
                return false;
            }
            thread.join(TimeUnit.NANOSECONDS.toMillis(remaining));
            if (thread.isAlive()) {
                return false;
            }
        }
        return true;
    }

    /**
     * 获取 Future 结果。
     *
     * @param future Future 对象
     * @param <T>    返回类型
     * @return Future 结果
     * @throws InterruptedException 当前线程被中断时抛出
     * @throws ExecutionException   任务执行失败时抛出
     */
    public static <T> T get(Future<T> future) throws InterruptedException, ExecutionException {
        return Objects.requireNonNull(future, "future must not be null").get();
    }

    /**
     * 带超时时间获取 Future 结果。
     *
     * @param future  Future 对象
     * @param timeout 超时时间
     * @param <T>     返回类型
     * @return Future 结果
     * @throws InterruptedException 当前线程被中断时抛出
     * @throws ExecutionException   任务执行失败时抛出
     * @throws TimeoutException     等待超时时抛出
     */
    public static <T> T get(Future<T> future, Duration timeout) throws InterruptedException, ExecutionException, TimeoutException {
        Objects.requireNonNull(future, "future must not be null");
        Duration actualTimeout = requirePositiveDuration(timeout, "timeout");
        return future.get(actualTimeout.toNanos(), TimeUnit.NANOSECONDS);
    }

    /**
     * 获取 Future 结果，失败时返回默认值。
     *
     * @param future       Future 对象
     * @param defaultValue 默认值
     * @param <T>          返回类型
     * @return Future 结果或默认值
     */
    public static <T> T getOrDefault(Future<T> future, T defaultValue) {
        try {
            return get(future);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * 获取 Future 结果，失败时返回 null。
     *
     * @param future Future 对象
     * @param <T>    返回类型
     * @return Future 结果或 null
     */
    public static <T> T getOrNull(Future<T> future) {
        return getOrDefault(future, null);
    }

    /**
     * 等待全部 Future 完成。
     *
     * @param futures Future 集合
     * @throws InterruptedException 当前线程被中断时抛出
     * @throws ExecutionException   任务执行失败时抛出
     */
    public static void awaitAll(Collection<? extends Future<?>> futures) throws InterruptedException, ExecutionException {
        for (Future<?> future : copyValues(futures, "futures")) {
            future.get();
        }
    }

    /**
     * 等待任意一个 Future 完成。
     *
     * @param futures Future 集合
     * @return 最先完成的 Future
     * @throws InterruptedException 当前线程被中断时抛出
     */
    public static Future<?> awaitAny(Collection<? extends Future<?>> futures) throws InterruptedException {
        List<? extends Future<?>> actualFutures = copyValues(futures, "futures");
        if (actualFutures.isEmpty()) {
            throw new IllegalArgumentException("futures must not be empty");
        }
        while (true) {
            for (Future<?> future : actualFutures) {
                if (future.isDone()) {
                    return future;
                }
            }
            Thread.sleep(1L);
        }
    }

    /**
     * 判断当前线程是否为虚拟线程。
     *
     * @return 是否为虚拟线程
     */
    public static boolean isVirtual() {
        return Thread.currentThread().isVirtual();
    }

    /**
     * 判断指定线程是否为虚拟线程。
     *
     * @param thread 线程对象
     * @return 是否为虚拟线程
     */
    public static boolean isVirtual(Thread thread) {
        return Objects.requireNonNull(thread, "thread must not be null").isVirtual();
    }

    /**
     * 判断当前线程是否为平台线程。
     *
     * @return 是否为平台线程
     */
    public static boolean isPlatform() {
        return !isVirtual();
    }

    /**
     * 判断指定线程是否为平台线程。
     *
     * @param thread 线程对象
     * @return 是否为平台线程
     */
    public static boolean isPlatform(Thread thread) {
        return !isVirtual(thread);
    }

    /**
     * 获取当前线程名称。
     *
     * @return 当前线程名称
     */
    public static String currentThreadName() {
        return Thread.currentThread().getName();
    }

    /**
     * 获取当前线程信息。
     *
     * @return 当前线程信息
     */
    public static String currentThreadInfo() {
        return dumpCurrent();
    }

    /**
     * 获取当前线程 ID。
     *
     * @return 当前线程 ID
     */
    public static long currentThreadId() {
        return Thread.currentThread().threadId();
    }

    /**
     * 获取指定线程诊断信息。
     *
     * @param thread 线程对象
     * @return 线程诊断信息
     */
    public static String dump(Thread thread) {
        Thread actualThread = Objects.requireNonNull(thread, "thread must not be null");
        return "Thread{name='" + actualThread.getName()
                + "', id=" + actualThread.threadId()
                + ", virtual=" + actualThread.isVirtual()
                + ", state=" + actualThread.getState()
                + ", interrupted=" + actualThread.isInterrupted()
                + "}";
    }

    /**
     * 获取当前线程诊断信息。
     *
     * @return 当前线程诊断信息
     */
    public static String dumpCurrent() {
        return dump(Thread.currentThread());
    }

    /**
     * 打印当前线程信息。
     */
    public static void logCurrent() {
        System.out.println(dumpCurrent());
    }

    /**
     * 打印指定线程信息。
     *
     * @param thread 线程对象
     */
    public static void logThread(Thread thread) {
        System.out.println(dump(thread));
    }

    /**
     * 生成默认序号线程名。
     *
     * @param prefix 线程名前缀
     * @return 线程名
     */
    public static String name(String prefix) {
        return name(prefix, NAME_COUNTER.getAndIncrement());
    }

    /**
     * 使用线程名前缀和序号生成线程名。
     *
     * @param prefix 线程名前缀
     * @param index  序号
     * @return 线程名
     */
    public static String name(String prefix, long index) {
        if (index < 0) {
            throw new IllegalArgumentException("index must not be negative");
        }
        return normalizePrefix(prefix) + "-" + index;
    }

    /**
     * 根据业务类型和业务 ID 生成线程名。
     *
     * @param businessType 业务类型
     * @param bizId        业务 ID
     * @return 线程名
     */
    public static String nameOf(String businessType, String bizId) {
        return "vt-" + sanitizeNamePart(businessType) + "-" + sanitizeNamePart(bizId);
    }

    /**
     * 根据模块名生成线程名前缀。
     *
     * @param module 模块名
     * @return 线程名前缀
     */
    public static String prefix(String module) {
        return "vt-" + sanitizeNamePart(module);
    }

    /**
     * 创建线程名生成器。
     *
     * @param prefix 线程名前缀
     * @return 线程名生成器
     */
    public static Supplier<String> newNameFactory(String prefix) {
        AtomicLong counter = new AtomicLong(1);
        String actualPrefix = normalizePrefix(prefix);
        return () -> name(actualPrefix, counter.getAndIncrement());
    }

    /**
     * 创建基于序号函数的线程名生成器。
     *
     * @param prefix 线程名前缀
     * @return 线程名生成函数
     */
    public static LongFunction<String> newIndexedNameFactory(String prefix) {
        String actualPrefix = normalizePrefix(prefix);
        return index -> name(actualPrefix, index);
    }

    /**
     * 规范化线程名。
     *
     * @param name 线程名
     * @return 规范化后的线程名
     */
    public static String normalizeName(String name) {
        String normalized = Objects.requireNonNull(name, "name must not be null").trim().replaceAll("\\s+", "-");
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        return normalized;
    }

    /**
     * 获取默认线程名前缀。
     *
     * @return 默认线程名前缀
     */
    public static String defaultPrefix() {
        return defaultThreadNamePrefix();
    }

    /**
     * 统计无返回任务耗时。
     *
     * @param task 执行任务
     * @return 执行耗时
     */
    public static Duration measure(Runnable task) {
        Instant start = Instant.now();
        requireRunnable(task).run();
        return Duration.between(start, Instant.now());
    }

    /**
     * 执行有返回任务并统计耗时。
     *
     * @param task 执行任务
     * @param <T>  返回类型
     * @return 带耗时的结果
     * @throws Exception 任务执行失败时抛出
     */
    public static <T> TimedResult<T> measure(Callable<T> task) throws Exception {
        Instant start = Instant.now();
        T value = requireCallable(task).call();
        return new TimedResult<>(value, Duration.between(start, Instant.now()));
    }

    /**
     * 执行无返回任务并回调耗时。
     *
     * @param task     执行任务
     * @param consumer 耗时消费者
     */
    public static void timed(Runnable task, Consumer<Duration> consumer) {
        Objects.requireNonNull(consumer, "consumer must not be null");
        Duration duration = measure(task);
        consumer.accept(duration);
    }

    /**
     * 执行有返回任务并回调结果和耗时。
     *
     * @param task     执行任务
     * @param consumer 结果和耗时消费者
     * @param <T>      返回类型
     * @return 执行结果
     * @throws Exception 任务执行失败时抛出
     */
    public static <T> T timed(Callable<T> task, BiConsumer<T, Duration> consumer) throws Exception {
        Objects.requireNonNull(consumer, "consumer must not be null");
        TimedResult<T> result = measure(task);
        consumer.accept(result.getValue(), result.getDuration());
        return result.getValue();
    }

    /**
     * 批量执行任务并返回统计信息。
     *
     * @param tasks 任务集合
     * @param <T>   返回类型
     * @return 统计信息
     */
    public static <T> VirtualTaskStats stats(Collection<? extends Callable<T>> tasks) {
        return collectAll(tasks).stats();
    }

    /**
     * 统计成功任务数量。
     *
     * @param results 任务结果集合
     * @param <T>     结果类型
     * @return 成功数量
     */
    public static <T> long countSuccess(Collection<VirtualTaskResult<T>> results) {
        return copyValues(results, "results").stream().filter(VirtualTaskResult::isSuccess).count();
    }

    /**
     * 统计失败任务数量。
     *
     * @param results 任务结果集合
     * @param <T>     结果类型
     * @return 失败数量
     */
    public static <T> long countFailure(Collection<VirtualTaskResult<T>> results) {
        return copyValues(results, "results").stream().filter(VirtualTaskResult::isFailure).count();
    }

    /**
     * 统计超时任务数量。
     *
     * @param results 任务结果集合
     * @param <T>     结果类型
     * @return 超时数量
     */
    public static <T> long countTimeout(Collection<VirtualTaskResult<T>> results) {
        return copyValues(results, "results").stream().filter(VirtualTaskResult::isTimeout).count();
    }

    /**
     * 生成批量任务摘要。
     *
     * @param results 任务结果集合
     * @param <T>     结果类型
     * @return 批量任务摘要
     */
    public static <T> VirtualTaskStats summary(Collection<VirtualTaskResult<T>> results) {
        List<VirtualTaskResult<T>> actualResults = copyValues(results, "results");
        int success = (int) actualResults.stream().filter(VirtualTaskResult::isSuccess).count();
        int failure = (int) actualResults.stream().filter(VirtualTaskResult::isFailure).count();
        int timeout = (int) actualResults.stream().filter(VirtualTaskResult::isTimeout).count();
        int cancelled = (int) actualResults.stream().filter(VirtualTaskResult::isCancelled).count();
        Duration total = actualResults.stream().map(VirtualTaskResult::getDuration).reduce(Duration.ZERO, Duration::plus);
        return new VirtualTaskStats(actualResults.size(), success, failure, timeout, cancelled, total);
    }

    /**
     * 将 Callable 转换为 Runnable。
     *
     * @param callable Callable 对象
     * @return Runnable 对象
     */
    public static Runnable toRunnable(Callable<?> callable) {
        Callable<?> actualCallable = requireCallable(callable);
        return () -> {
            try {
                actualCallable.call();
            } catch (Exception e) {
                throw rethrow(e);
            }
        };
    }

    /**
     * 将 Runnable 转换为 Callable。
     *
     * @param runnable Runnable 对象
     * @return Callable 对象
     */
    public static Callable<Void> toCallable(Runnable runnable) {
        Runnable actualRunnable = requireRunnable(runnable);
        return () -> {
            actualRunnable.run();
            return null;
        };
    }

    /**
     * 将 Callable 转换为 Supplier。
     *
     * @param callable Callable 对象
     * @param <T>      返回类型
     * @return Supplier 对象
     */
    public static <T> Supplier<T> toSupplier(Callable<T> callable) {
        Callable<T> actualCallable = requireCallable(callable);
        return () -> {
            try {
                return actualCallable.call();
            } catch (Exception e) {
                throw rethrow(e);
            }
        };
    }

    /**
     * 将 Future 转换为 CompletableFuture。
     *
     * @param future Future 对象
     * @param <T>    返回类型
     * @return CompletableFuture 对象
     */
    public static <T> CompletableFuture<T> toCompletableFuture(Future<T> future) {
        Objects.requireNonNull(future, "future must not be null");
        CompletableFuture<T> completableFuture = new CompletableFuture<>();
        start(() -> {
            try {
                completableFuture.complete(future.get());
            } catch (CancellationException e) {
                completableFuture.cancel(true);
            } catch (Exception e) {
                completableFuture.completeExceptionally(unwrap(e));
            }
        });
        return completableFuture;
    }

    /**
     * 批量转换任务为 Future。
     *
     * @param tasks 任务集合
     * @param <T>   返回类型
     * @return Future 集合
     */
    public static <T> List<Future<T>> toFutures(Collection<? extends Callable<T>> tasks) {
        return submitAll(tasks);
    }

    /**
     * 包装 Supplier 并传递上下文。
     *
     * @param supplier Supplier 对象
     * @param <T>      返回类型
     * @return 包装后的 Supplier
     */
    public static <T> Supplier<T> wrapSupplier(Supplier<T> supplier) {
        Objects.requireNonNull(supplier, "supplier must not be null");
        return toSupplier(wrap((Callable<T>) supplier::get));
    }

    /**
     * 包装 Function 并传递上下文。
     *
     * @param function Function 对象
     * @param <T>      入参类型
     * @param <R>      返回类型
     * @return 包装后的 Function
     */
    public static <T, R> Function<T, R> wrapFunction(Function<T, R> function) {
        Objects.requireNonNull(function, "function must not be null");
        Object context = captureContext();
        return value -> {
            restoreContext(context);
            try {
                return function.apply(value);
            } finally {
                clearContext();
            }
        };
    }

    /**
     * 包装 Consumer 并传递上下文。
     *
     * @param consumer Consumer 对象
     * @param <T>      入参类型
     * @return 包装后的 Consumer
     */
    public static <T> Consumer<T> wrapConsumer(Consumer<T> consumer) {
        Objects.requireNonNull(consumer, "consumer must not be null");
        Object context = captureContext();
        return value -> {
            restoreContext(context);
            try {
                consumer.accept(value);
            } finally {
                clearContext();
            }
        };
    }

    /**
     * 获取默认超时时间。
     *
     * @return 默认超时时间
     */
    public static Duration defaultTimeout() {
        return config().getTimeout();
    }

    /**
     * 获取默认并发数。
     *
     * @return 默认并发数
     */
    public static int defaultConcurrency() {
        return config().getConcurrency();
    }

    /**
     * 获取默认线程名前缀。
     *
     * @return 默认线程名前缀
     */
    public static String defaultThreadNamePrefix() {
        return config().getThreadNamePrefix();
    }

    /**
     * 创建使用默认线程名前缀的虚拟线程执行器。
     *
     * @return 虚拟线程执行器
     */
    public static ExecutorService defaultExecutor() {
        return newExecutor(defaultThreadNamePrefix());
    }

    /**
     * 设置默认超时时间。
     *
     * @param timeout 默认超时时间
     */
    public static void setDefaultTimeout(Duration timeout) {
        VirtualThreadConfig current = config();
        DEFAULT_CONFIG.set(VirtualThreadConfig.builder()
                .timeout(timeout)
                .concurrency(current.getConcurrency())
                .threadNamePrefix(current.getThreadNamePrefix())
                .build());
    }

    /**
     * 设置默认并发数。
     *
     * @param concurrency 默认并发数
     */
    public static void setDefaultConcurrency(int concurrency) {
        VirtualThreadConfig current = config();
        DEFAULT_CONFIG.set(VirtualThreadConfig.builder()
                .timeout(current.getTimeout())
                .concurrency(concurrency)
                .threadNamePrefix(current.getThreadNamePrefix())
                .build());
    }

    /**
     * 设置默认线程名前缀。
     *
     * @param prefix 默认线程名前缀
     */
    public static void setDefaultThreadNamePrefix(String prefix) {
        VirtualThreadConfig current = config();
        DEFAULT_CONFIG.set(VirtualThreadConfig.builder()
                .timeout(current.getTimeout())
                .concurrency(current.getConcurrency())
                .threadNamePrefix(prefix)
                .build());
    }

    /**
     * 获取当前默认配置。
     *
     * @return 当前默认配置
     */
    public static VirtualThreadConfig config() {
        return DEFAULT_CONFIG.get();
    }

    /**
     * 创建虚拟线程配置构建器。
     *
     * @return 配置构建器
     */
    public static VirtualThreadConfig.Builder builder() {
        return VirtualThreadConfig.builder();
    }

    /**
     * 重置默认配置。
     */
    public static void resetConfig() {
        DEFAULT_CONFIG.set(VirtualThreadConfig.defaults());
    }

    private static void completeRunnable(CompletableFuture<Void> future, Runnable task) {
        try {
            requireRunnable(task).run();
            future.complete(null);
        } catch (Throwable e) {
            future.completeExceptionally(e);
        }
    }

    private static <T> void completeCallable(CompletableFuture<T> future, Callable<T> task) {
        try {
            future.complete(requireCallable(task).call());
        } catch (Throwable e) {
            future.completeExceptionally(e);
        }
    }

    private static <T> VirtualTaskResult<T> executeAsResult(int index, Callable<T> task) {
        Instant start = Instant.now();
        try {
            return VirtualTaskResult.success(index, task.call(), Duration.between(start, Instant.now()));
        } catch (CancellationException e) {
            return VirtualTaskResult.cancelled(index, e, Duration.between(start, Instant.now()));
        } catch (Exception e) {
            if (e instanceof TimeoutException) {
                return VirtualTaskResult.timeout(index, e, Duration.between(start, Instant.now()));
            }
            return VirtualTaskResult.failure(index, e, Duration.between(start, Instant.now()));
        }
    }

    private static Runnable requireRunnable(Runnable task) {
        return Objects.requireNonNull(task, "task must not be null");
    }

    private static <T> Callable<T> requireCallable(Callable<T> task) {
        return Objects.requireNonNull(task, "task must not be null");
    }

    private static Duration requirePositiveDuration(Duration duration, String name) {
        Duration actual = Objects.requireNonNull(duration, name + " must not be null");
        if (actual.isZero() || actual.isNegative()) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return actual;
    }

    private static int requirePositive(int value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return value;
    }

    private static <T> List<T> copyValues(Collection<? extends T> values, String name) {
        Objects.requireNonNull(values, name + " must not be null");
        List<T> copy = new ArrayList<>(values.size());
        int index = 0;
        for (T value : values) {
            if (value == null) {
                throw new IllegalArgumentException(name + " contains null element at index " + index);
            }
            copy.add(value);
            index++;
        }
        return copy;
    }

    private static <T> List<Callable<T>> copyCallables(Collection<? extends Callable<T>> tasks) {
        return copyValues(tasks, "tasks");
    }

    private static List<Runnable> copyRunnables(Collection<? extends Runnable> tasks) {
        return copyValues(tasks, "tasks");
    }

    private static <T> List<Supplier<T>> copySuppliers(Collection<? extends Supplier<T>> suppliers) {
        return copyValues(suppliers, "suppliers");
    }

    private static String normalizePrefix(String prefix) {
        return normalizeName(prefix);
    }

    private static String sanitizeNamePart(String value) {
        return normalizeName(value).replaceAll("[^a-zA-Z0-9_.-]", "-");
    }

    private static Throwable unwrapOrNull(Throwable throwable) {
        return throwable == null ? null : unwrap(throwable);
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> void throwException(Throwable throwable) throws E {
        throw (E) throwable;
    }

    private record ContextEntry(VirtualThreadContextProvider provider, Object context) {
    }

    private record ContextSnapshot(List<ContextEntry> entries) {
        private ContextSnapshot(List<ContextEntry> entries) {
            this.entries = Collections.unmodifiableList(new ArrayList<>(entries));
        }
    }
}
