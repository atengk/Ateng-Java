package io.github.atengk.basic;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * 虚拟线程示例（JDK21）
 */
public class VirtualThreadExample {

    /**
     * 模拟耗时任务（如：远程调用 / IO）
     */
    public static String task(int i) {
        try {
            Thread.sleep(100); // 模拟 IO 阻塞（虚拟线程非常适合）
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return "任务" + i + " -> " + Thread.currentThread();
    }

    /**
     * 核心方法：虚拟线程使用
     */
    public static void virtualThreadUsage() throws Exception {

        // =========================
        // 1. 创建虚拟线程（最简单🔥）
        // =========================
        Thread.startVirtualThread(() -> {
            System.out.println("虚拟线程：" + Thread.currentThread());
        });


        // =========================
        // 2. Thread.ofVirtual 创建
        // =========================
        Thread t = Thread.ofVirtual()
                .name("vt-", 1)
                .start(() -> System.out.println("自定义虚拟线程：" + Thread.currentThread()));

        t.join();


        // =========================
        // 3. 批量任务（对比传统线程池）
        // =========================
        List<Thread> threads = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            int index = i;
            Thread vt = Thread.startVirtualThread(() -> {
                String result = task(index);
                System.out.println(result);
            });
            threads.add(vt);
        }

        // 等待完成
        for (Thread thread : threads) {
            thread.join();
        }


        // =========================
        // 4. 使用 Executor（推荐🔥）
        // =========================
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {

            List<Future<String>> futures = new ArrayList<>();

            for (int i = 0; i < 10; i++) {
                int index = i;
                futures.add(executor.submit(() -> task(index)));
            }

            for (Future<String> future : futures) {
                System.out.println("结果：" + future.get());
            }
        }


        // =========================
        // 5. 对比：传统线程池（资源重）
        // =========================
        ExecutorService pool = Executors.newFixedThreadPool(3);

        List<Future<String>> list = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            int index = i;
            list.add(pool.submit(() -> task(index)));
        }

        for (Future<String> f : list) {
            System.out.println("传统线程池：" + f.get());
        }

        pool.shutdown();


        // =========================
        // 6. 高并发场景（虚拟线程优势🔥）
        // =========================
        long start = System.currentTimeMillis();

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {

            List<Future<?>> tasks = new ArrayList<>();

            for (int i = 0; i < 1000; i++) {
                tasks.add(executor.submit(() -> task(1)));
            }

            for (Future<?> f : tasks) {
                f.get();
            }
        }

        long end = System.currentTimeMillis();

        System.out.println("1000任务耗时：" + (end - start) + " ms");


        // =========================
        // 7. 注意：虚拟线程适合 IO，不适合 CPU 密集
        // =========================
        System.out.println("\n建议：");
        System.out.println("✔ IO 密集：虚拟线程");
        System.out.println("✔ CPU 密集：传统线程池");


        // =========================
        // 8. 项目实战（接口并发调用🔥）
        // =========================
        List<String> results = callApis();

        System.out.println("\n接口聚合：" + results);
    }

    /**
     * 项目实战：并发调用多个接口
     */
    public static List<String> callApis() throws Exception {

        List<String> result = new ArrayList<>();

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {

            List<Future<String>> futures = List.of(
                    executor.submit(() -> task(1)),
                    executor.submit(() -> task(2)),
                    executor.submit(() -> task(3))
            );

            for (Future<String> f : futures) {
                result.add(f.get());
            }
        }

        return result;
    }

    public static void main(String[] args) throws Exception {
        virtualThreadUsage();
    }
}