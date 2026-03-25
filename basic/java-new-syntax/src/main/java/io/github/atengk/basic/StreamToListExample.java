package io.github.atengk.basic;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Stream.toList() 示例（JDK16+）
 */
public class StreamToListExample {

    /**
     * 核心方法：toList 使用
     */
    public static void toListUsage() {

        List<String> list = List.of("a", "b", "c");

        // =========================
        // 1. 传统写法（对比）
        // =========================
        List<String> oldList = list.stream()
                .map(String::toUpperCase)
                .collect(Collectors.toList());

        System.out.println("旧写法：" + oldList);


        // =========================
        // 2. 新写法（推荐🔥）
        // =========================
        List<String> newList = list.stream()
                .map(String::toUpperCase)
                .toList();

        System.out.println("新写法：" + newList);


        // =========================
        // 3. 不可变集合（重要⚠️）
        // =========================
        try {
            newList.add("D"); // ❌ 会抛异常
        } catch (Exception e) {
            System.out.println("不可变：" + e);
        }


        // =========================
        // 4. 与 collect 的区别
        // =========================
        List<String> mutableList = list.stream()
                .collect(Collectors.toList()); // 可变

        mutableList.add("D"); // ✅ 正常

        System.out.println("可变集合：" + mutableList);


        // =========================
        // 5. 转可变 List（推荐写法）
        // =========================
        List<String> copy = new ArrayList<>(list.stream().toList());

        copy.add("D");

        System.out.println("转换为可变：" + copy);


        // =========================
        // 6. 链式操作（项目常用🔥）
        // =========================
        List<Integer> result = list.stream()
                .filter(s -> !s.isBlank())
                .map(String::length)
                .sorted()
                .toList();

        System.out.println("链式结果：" + result);


        // =========================
        // 7. 结合 Optional
        // =========================
        List<String> safeList = Optional.ofNullable(list)
                .orElse(List.of())
                .stream()
                .toList();

        System.out.println("安全转换：" + safeList);


        // =========================
        // 8. 空流处理
        // =========================
        List<String> empty = List.<String>of().stream().toList();

        System.out.println("空集合：" + empty);


        // =========================
        // 9. 项目实战：DTO 转换🔥
        // =========================
        List<User> users = List.of(
                new User("张三"),
                new User("李四")
        );

        List<String> names = users.stream()
                .map(User::name)
                .toList();

        System.out.println("姓名列表：" + names);


        // =========================
        // 10. 总结：推荐使用 toList()
        // =========================
        System.out.println("\n推荐：默认使用 toList()");
        System.out.println("需要可变集合 → new ArrayList<>(...)");
    }

    /**
     * 示例 record（JDK16+）
     */
    public record User(String name) {}

    public static void main(String[] args) {
        toListUsage();
    }
}