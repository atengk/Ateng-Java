package io.github.atengk.basic;

import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Stream 并行流示例（parallelStream）
 */
public class ParallelStreamExample {

    /**
     * 模拟耗时操作（如：远程调用 / DB 查询）
     */
    public static int slowTask(int num) {
        try {
            Thread.sleep(100); // 模拟耗时
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return num * 2;
    }

    /**
     * 核心方法：对比串行流 vs 并行流
     */
    public static void parallelStreamUsage() {

        // 构造测试数据
        List<Integer> list = IntStream.rangeClosed(1, 10).boxed().toList();

        // =========================
        // 1. 串行流（默认）
        // =========================
        long start1 = System.currentTimeMillis();

        List<Integer> result1 = list.stream()
                .map(ParallelStreamExample::slowTask)
                .collect(Collectors.toList());

        long end1 = System.currentTimeMillis();

        System.out.println("串行流结果：" + result1);
        System.out.println("串行耗时：" + (end1 - start1) + " ms");


        // =========================
        // 2. 并行流（parallelStream）
        // =========================
        long start2 = System.currentTimeMillis();

        List<Integer> result2 = list.parallelStream()
                .map(ParallelStreamExample::slowTask)
                .collect(Collectors.toList());

        long end2 = System.currentTimeMillis();

        System.out.println("\n并行流结果：" + result2);
        System.out.println("并行耗时：" + (end2 - start2) + " ms");


        // =========================
        // 3. 并行流线程演示
        // =========================
        System.out.println("\n线程执行情况：");

        list.parallelStream().forEach(i -> {
            System.out.println("值：" + i + " -> 线程：" + Thread.currentThread().getName());
        });


        // =========================
        // 4. 注意：并行流顺序问题
        // =========================
        System.out.println("\nforEach（无序）：");
        list.parallelStream().forEach(System.out::print);

        System.out.println("\nforEachOrdered（有序）：");
        list.parallelStream().forEachOrdered(System.out::print);


        // =========================
        // 5. reduce 聚合（并行安全）
        // =========================
        int sum = list.parallelStream()
                .reduce(0, Integer::sum);

        System.out.println("\n\n并行求和：" + sum);


        // =========================
        // 6. 错误示例：共享变量（线程不安全）
        // =========================
        List<Integer> unsafeList = new ArrayList<>();

        try {
            list.parallelStream().forEach(i -> unsafeList.add(i)); // 非线程安全
        } catch (Exception e) {
            System.out.println("\n并发异常：" + e);
        }

        System.out.println("unsafeList size：" + unsafeList.size());


        // =========================
        // 7. 正确写法：使用收集器（线程安全）
        // =========================
        List<Integer> safeList = list.parallelStream()
                .collect(Collectors.toList());

        System.out.println("safeList size：" + safeList.size());


        // =========================
        // 8. 自定义线程池（不推荐直接用 parallelStream）
        // =========================
        // ❗ parallelStream 默认使用 ForkJoinPool.commonPool
        // ❗ 项目中建议使用 CompletableFuture + 自定义线程池替代
        System.out.println("\n默认线程池：" + ForkJoinPool.commonPool());
    }

    public static void main(String[] args) {
        parallelStreamUsage();
    }
}
