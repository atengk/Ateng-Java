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
