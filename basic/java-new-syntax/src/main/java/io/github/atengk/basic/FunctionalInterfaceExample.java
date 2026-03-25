package io.github.atengk.basic;

import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;

/**
 * 函数式接口实战封装（Function / Consumer / Predicate）
 */
public class FunctionalInterfaceExample {

    /**
     * 实体类（模拟项目对象）
     */
    static class User {
        private String name;
        private int age;
        private boolean active;

        public User(String name, int age, boolean active) {
            this.name = name;
            this.age = age;
            this.active = active;
        }

        public String getName() { return name; }
        public int getAge() { return age; }
        public boolean isActive() { return active; }

        @Override
        public String toString() {
            return name + "(" + age + "," + active + ")";
        }
    }

    // =========================
    // 1. Predicate 封装（通用过滤）
    // =========================
    public static <T> List<T> filter(List<T> list, Predicate<T> predicate) {
        // 根据条件过滤集合（通用工具方法）
        return list.stream()
                .filter(predicate)
                .toList();
    }

    // =========================
    // 2. Function 封装（通用转换）
    // =========================
    public static <T, R> List<R> map(List<T> list, Function<T, R> function) {
        // 集合转换（如 Entity -> DTO）
        return list.stream()
                .map(function)
                .toList();
    }

    // =========================
    // 3. Consumer 封装（批量处理）
    // =========================
    public static <T> void forEach(List<T> list, Consumer<T> consumer) {
        // 批量执行操作（如日志、发送消息等）
        list.forEach(consumer);
    }

    // =========================
    // 4. Predicate 组合（复杂条件）
    // =========================
    public static Predicate<User> buildUserFilter() {
        // 年龄 > 18 且 active = true
        return u -> u.getAge() > 18 && u.isActive();
    }

    // =========================
    // 5. Function 链式组合
    // =========================
    public static Function<User, String> buildUserMapper() {
        // name -> upper -> 拼接
        return ((Function<User, String>) User::getName)
                .andThen(String::toUpperCase)
                .andThen(name -> "USER:" + name);
    }

    // =========================
    // 6. 批处理模板（项目常用）
    // =========================
    public static <T, R> List<R> process(
            List<T> list,
            Predicate<T> filter,
            Function<T, R> mapper,
            Consumer<R> after) {

        return list.stream()
                .filter(filter)     // 过滤
                .map(mapper)        // 转换
                .peek(after)        // 后置处理（日志/埋点）
                .toList();
    }

    /**
     * 核心方法：演示函数式接口封装使用
     */
    public static void functionalUsage() {

        // 构造数据
        List<User> users = List.of(
                new User("张三", 20, true),
                new User("李四", 16, true),
                new User("王五", 30, false),
                new User("赵六", 25, true)
        );

        // =========================
        // 1. Predicate 过滤
        // =========================
        List<User> adults = filter(users, u -> u.getAge() >= 18);

        System.out.println("成年人：" + adults);


        // =========================
        // 2. Function 转换
        // =========================
        List<String> names = map(users, User::getName);

        System.out.println("姓名列表：" + names);


        // =========================
        // 3. Consumer 批处理
        // =========================
        forEach(users, u -> System.out.println("处理用户：" + u));


        // =========================
        // 4. Predicate 组合
        // =========================
        List<User> validUsers = filter(users, buildUserFilter());

        System.out.println("有效用户：" + validUsers);


        // =========================
        // 5. Function 链式处理
        // =========================
        List<String> result = map(users, buildUserMapper());

        System.out.println("转换结果：" + result);


        // =========================
        // 6. 组合使用（项目核心写法）
        // =========================
        List<String> processed = process(
                users,
                u -> u.getAge() > 18 && u.isActive(), // 条件
                User::getName,                        // 转换
                name -> System.out.println("日志：" + name) // 后处理
        );

        System.out.println("最终结果：" + processed);


        // =========================
        // 7. 分组 + 函数接口（进阶）
        // =========================
        Map<Boolean, List<User>> group =
                users.stream()
                        .collect(Collectors.partitioningBy(User::isActive));

        System.out.println("分组：" + group);
    }

    public static void main(String[] args) {
        functionalUsage();
    }
}