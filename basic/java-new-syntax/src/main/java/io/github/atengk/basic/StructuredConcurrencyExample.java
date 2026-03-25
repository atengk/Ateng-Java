package io.github.atengk.basic;

import java.util.concurrent.*;
import java.util.*;

/**
 * Structured Concurrency 示例（JDK21🔥）
 */
public class StructuredConcurrencyExample {

    /**
     * 模拟接口调用
     */
    public static String getUser() {
        sleep(100);
        return "张三";
    }

    public static int getOrder() {
        sleep(150);
        return 5;
    }

    public static double getBalance() {
        sleep(200);
        return 1000.0;
    }

    public static String errorTask() {
        sleep(100);
        throw new RuntimeException("任务异常");
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 核心方法：结构化并发使用
     */
    public static void structuredUsage() throws Exception {

        // =========================
        // 1. 基础用法（fork + join🔥）
        // =========================
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {

            // fork 子任务（自动使用虚拟线程）
            var userTask = scope.fork(StructuredConcurrencyExample::getUser);
            var orderTask = scope.fork(StructuredConcurrencyExample::getOrder);
            var balanceTask = scope.fork(StructuredConcurrencyExample::getBalance);

            // 等待所有任务完成
            scope.join();

            // 如果有异常则抛出
            scope.throwIfFailed();

            // 获取结果
            System.out.println("结果："
                    + userTask.get() + ", "
                    + orderTask.get() + ", "
                    + balanceTask.get());
        }


        // =========================
        // 2. 失败快速取消（核心🔥）
        // =========================
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {

            var t1 = scope.fork(StructuredConcurrencyExample::getUser);
            var t2 = scope.fork(StructuredConcurrencyExample::errorTask); // 异常任务
            var t3 = scope.fork(StructuredConcurrencyExample::getBalance);

            scope.join();

            // 任一失败，其他任务自动取消
            scope.throwIfFailed();

            System.out.println(t1.get() + "," + t2.get() + "," + t3.get());

        } catch (Exception e) {
            System.out.println("\n失败自动取消：" + e.getMessage());
        }


        // =========================
        // 3. 任意成功返回（ShutdownOnSuccess🔥）
        // =========================
        try (var scope = new StructuredTaskScope.ShutdownOnSuccess<String>()) {

            scope.fork(() -> {
                sleep(200);
                return "慢任务";
            });

            scope.fork(() -> {
                sleep(100);
                return "快任务";
            });

            scope.join();

            // 返回最先成功的结果
            String result = scope.result();

            System.out.println("\n最快结果：" + result);
        }


        // =========================
        // 4. 超时控制
        // =========================
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {

            var task = scope.fork(() -> {
                sleep(3000);
                return "慢任务";
            });

            try {
                // 最多等待1秒
                scope.joinUntil(java.time.Instant.now().plusSeconds(1));
            } catch (TimeoutException e) {
                System.out.println("\n超时，取消任务");
                scope.shutdown();
            }

            // 等待任务真正结束（很关键）
            scope.join();

            if (task.state() == StructuredTaskScope.Subtask.State.SUCCESS) {
                System.out.println(task.get());
            } else {
                System.out.println("任务未成功完成，状态：" + task.state());
            }
        }


        // =========================
        // 5. 项目实战：接口聚合（推荐🔥）
        // =========================
        Map<String, Object> dashboard = buildDashboard();

        System.out.println("\n聚合接口返回：" + dashboard);
    }

    /**
     * 项目实战：结构化并发实现接口聚合
     */
    public static Map<String, Object> buildDashboard() throws Exception {

        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {

            var user = scope.fork(StructuredConcurrencyExample::getUser);
            var order = scope.fork(StructuredConcurrencyExample::getOrder);
            var balance = scope.fork(StructuredConcurrencyExample::getBalance);

            scope.join();
            scope.throwIfFailed();

            return Map.of(
                    "user", user.get(),
                    "order", order.get(),
                    "balance", balance.get()
            );
        }
    }

    public static void main(String[] args) throws Exception {
        structuredUsage();
    }
}
