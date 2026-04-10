package io.github.atengk.basic.async;


import java.util.concurrent.Callable;
import java.util.concurrent.StructuredTaskScope;

/**
 * 异步任务执行工具（StructuredTaskScope + ScopedValue）
 * <p>
 * 用于解决线程池场景下上下文丢失问题，实现请求级上下文自动传播
 *
 * @author Ateng
 * @since 2026-04-10
 */
public final class AsyncExecutor {

    private AsyncExecutor() {
    }

    /**
     * 执行两个并行任务，并返回组合结果
     *
     * @param taskA 任务A
     * @param taskB 任务B
     * @return 组合结果
     */
    public static <A, B> Result<A, B> run(Callable<A> taskA, Callable<B> taskB) {

        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {

            StructuredTaskScope.Subtask<A> a = scope.fork(taskA);
            StructuredTaskScope.Subtask<B> b = scope.fork(taskB);

            scope.join();
            scope.throwIfFailed();

            return new Result<>(a.get(), b.get());
        } catch (Exception e) {
            throw new RuntimeException("异步任务执行失败", e);
        }
    }

    /**
     * 组合结果
     */
    public record Result<A, B>(A a, B b) {
    }
}
