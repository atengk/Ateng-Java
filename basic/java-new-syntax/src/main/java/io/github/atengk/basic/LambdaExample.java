package io.github.atengk.basic;

import java.util.*;
import java.util.function.Function;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Lambda 表达式 + 方法引用示例
 */
public class LambdaExample {

    /**
     * 核心方法：Lambda + 方法引用常用写法
     */
    public static void lambdaUsage() {

        // =========================
        // 1. Lambda 基础写法
        // =========================
        List<String> list = List.of("apple", "banana", "orange");

        list.forEach(s -> System.out.println("Lambda：" + s));


        // =========================
        // 2. 方法引用（替代 Lambda）
        // =========================
        list.forEach(System.out::println);


        // =========================
        // 3. 排序（Comparator）
        // =========================
        List<String> sorted = list.stream()
                .sorted((a, b) -> a.compareTo(b)) // Lambda
                .toList();

        System.out.println("排序：" + sorted);

        // 方法引用写法（更简洁）
        List<String> sorted2 = list.stream()
                .sorted(String::compareTo)
                .toList();

        System.out.println("排序（方法引用）：" + sorted2);


        // =========================
        // 4. Function（函数式接口）
        // =========================
        Function<String, Integer> lengthFunc = s -> s.length();

        int len = lengthFunc.apply("hello");
        System.out.println("长度：" + len);

        // 方法引用
        Function<String, Integer> lengthFunc2 = String::length;
        System.out.println("长度（方法引用）：" + lengthFunc2.apply("world"));


        // =========================
        // 5. Predicate（条件判断）
        // =========================
        Predicate<String> isLong = s -> s.length() > 5;

        list.stream()
                .filter(isLong)
                .forEach(System.out::println);


        // =========================
        // 6. Consumer（消费型接口）
        // =========================
        Consumer<String> printer = s -> System.out.println("消费：" + s);

        list.forEach(printer);


        // =========================
        // 7. 构造方法引用
        // =========================
        List<User> users = list.stream()
                .map(User::new) // 等价于 s -> new User(s)
                .toList();

        System.out.println("构造方法引用：" + users);


        // =========================
        // 8. 静态方法引用
        // =========================
        List<Integer> numbers = List.of(-1, 2, -3, 4);

        List<Integer> absList = numbers.stream()
                .map(Math::abs)
                .toList();

        System.out.println("绝对值：" + absList);


        // =========================
        // 9. 实例方法引用（特定对象）
        // =========================
        String prefix = "前缀-";

        List<String> newList = list.stream()
                .map(prefix::concat) // 等价于 s -> prefix.concat(s)
                .toList();

        System.out.println("拼接：" + newList);


        // =========================
        // 10. 项目常见写法（链式处理）
        // =========================
        List<String> result = list.stream()
                .filter(s -> s.length() > 5)
                .map(String::toUpperCase)
                .sorted()
                .toList();

        System.out.println("链式处理：" + result);
    }

    /**
     * 示例实体类
     */
    static class User {
        private String name;

        public User(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return "User{name='" + name + "'}";
        }
    }

    public static void main(String[] args) {
        lambdaUsage();
    }
}
