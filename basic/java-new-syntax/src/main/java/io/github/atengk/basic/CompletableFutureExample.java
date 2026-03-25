package io.github.atengk.basic;

import java.util.concurrent.*;
import java.util.*;

/**
 * CompletableFuture 异步编排示例（项目核心🔥）
 */
public class CompletableFutureExample {

    /**
     * 自定义线程池（项目必须使用，避免占用公共线程池）
     */
    private static final ExecutorService EXECUTOR =
            Executors.newFixedThreadPool(4);

    /**
     * 模拟接口调用
     */
    public static String getUser() {
        sleep(100);
        return "张三";
    }

    public static int getOrderCount() {
        sleep(150);
        return 5;
    }

    public static double getBalance() {
        sleep(200);
        return 1000.0;
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 核心方法：CompletableFuture 使用
     */
    public static void completableFutureUsage() throws Exception {

        // =========================
        // 1. 异步执行（无返回值）
        // =========================
        CompletableFuture<Void> f1 =
                CompletableFuture.runAsync(() -> {
                    System.out.println("异步任务：" + Thread.currentThread().getName());
                }, EXECUTOR);

        f1.get();


        // =========================
        // 2. 异步执行（有返回值）
        // =========================
        CompletableFuture<String> f2 =
                CompletableFuture.supplyAsync(() -> getUser(), EXECUTOR);

        System.out.println("\n用户：" + f2.get());


        // =========================
        // 3. thenApply（链式转换）
        // =========================
        CompletableFuture<String> f3 =
                CompletableFuture.supplyAsync(() -> getUser(), EXECUTOR)
                        .thenApply(name -> name.toUpperCase());

        System.out.println("转换后：" + f3.get());


        // =========================
        // 4. thenAccept（消费结果）
        // =========================
        CompletableFuture<Void> f4 =
                CompletableFuture.supplyAsync(() -> getUser(), EXECUTOR)
                        .thenAccept(name -> System.out.println("消费：" + name));

        f4.get();


        // =========================
        // 5. thenRun（不关心结果）
        // =========================
        CompletableFuture<Void> f5 =
                CompletableFuture.supplyAsync(() -> getUser(), EXECUTOR)
                        .thenRun(() -> System.out.println("执行完成"));

        f5.get();


        // =========================
        // 6. thenCombine（合并两个任务🔥）
        // =========================
        CompletableFuture<String> userFuture =
                CompletableFuture.supplyAsync(() -> getUser(), EXECUTOR);

        CompletableFuture<Integer> orderFuture =
                CompletableFuture.supplyAsync(() -> getOrderCount(), EXECUTOR);

        CompletableFuture<String> combined =
                userFuture.thenCombine(orderFuture,
                        (user, order) -> user + " 订单数：" + order);

        System.out.println("\n合并结果：" + combined.get());


        // =========================
        // 7. allOf（多个任务并行🔥）
        // =========================
        CompletableFuture<String> fUser =
                CompletableFuture.supplyAsync(() -> getUser(), EXECUTOR);

        CompletableFuture<Integer> fOrder =
                CompletableFuture.supplyAsync(() -> getOrderCount(), EXECUTOR);

        CompletableFuture<Double> fBalance =
                CompletableFuture.supplyAsync(() -> getBalance(), EXECUTOR);

        CompletableFuture<Void> all =
                CompletableFuture.allOf(fUser, fOrder, fBalance);

        all.get(); // 等待全部完成

        System.out.println("\n聚合结果："
                + fUser.get() + ", "
                + fOrder.get() + ", "
                + fBalance.get());


        // =========================
        // 8. anyOf（任意一个完成）
        // =========================
        CompletableFuture<Object> any =
                CompletableFuture.anyOf(fUser, fOrder, fBalance);

        System.out.println("\n最快结果：" + any.get());


        // =========================
        // 9. 异常处理（exceptionally）
        // =========================
        CompletableFuture<String> fError =
                CompletableFuture.<String>supplyAsync(() -> {
                            throw new RuntimeException("异常测试");
                        }, EXECUTOR)
                        .exceptionally(ex -> {
                            System.out.println("捕获异常：" + ex.getMessage());
                            return "默认值";
                        });

        System.out.println("异常结果：" + fError.get());


        // =========================
        // 10. handle（统一处理结果+异常）
        // =========================
        CompletableFuture<String> fHandle =
                CompletableFuture.supplyAsync(() -> {
                            if (true) throw new RuntimeException("错误");
                            return "OK";
                        }, EXECUTOR)
                        .handle((res, ex) -> {
                            if (ex != null) {
                                return "兜底";
                            }
                            return res;
                        });

        System.out.println("handle：" + fHandle.get());


        // =========================
        // 11. 超时控制（JDK9+🔥）
        // =========================
        CompletableFuture<String> fTimeout =
                CompletableFuture.supplyAsync(() -> {
                            sleep(3000);
                            return "慢任务";
                        }, EXECUTOR)
                        .orTimeout(1, TimeUnit.SECONDS)
                        .exceptionally(ex -> "超时默认");

        System.out.println("超时结果：" + fTimeout.get());


        // =========================
        // 12. 项目实战（聚合接口🔥）
        // =========================
        Map<String, Object> result = buildUserDashboard();

        System.out.println("\n聚合接口返回：" + result);

        EXECUTOR.shutdown();
    }

    /**
     * 项目实战：接口聚合（用户中心）
     */
    public static Map<String, Object> buildUserDashboard() throws Exception {

        CompletableFuture<String> user =
                CompletableFuture.supplyAsync(CompletableFutureExample::getUser, EXECUTOR);

        CompletableFuture<Integer> order =
                CompletableFuture.supplyAsync(CompletableFutureExample::getOrderCount, EXECUTOR);

        CompletableFuture<Double> balance =
                CompletableFuture.supplyAsync(CompletableFutureExample::getBalance, EXECUTOR);

        CompletableFuture.allOf(user, order, balance).join();

        Map<String, Object> map = new HashMap<>();
        map.put("user", user.join());
        map.put("orderCount", order.join());
        map.put("balance", balance.join());

        return map;
    }

    public static void main(String[] args) throws Exception {
        completableFutureUsage();
    }
}
