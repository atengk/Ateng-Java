package io.github.atengk.basic;

import java.util.*;
import java.util.stream.*;

/**
 * var 局部变量类型推断示例（JDK10）
 */
public class VarExample {

    /**
     * 核心方法：var 使用
     */
    public static void varUsage() {

        // =========================
        // 1. 基础用法（自动推断类型🔥）
        // =========================
        var name = "张三";       // String
        var age = 20;           // int
        var price = 99.9;       // double

        System.out.println("name 类型：" + ((Object) name).getClass().getSimpleName());
        System.out.println("age 类型：" + ((Object) age).getClass().getSimpleName());


        // =========================
        // 2. 集合推断（推荐🔥）
        // =========================
        var list = List.of("A", "B", "C"); // List<String>

        for (var item : list) {
            System.out.println("元素：" + item);
        }


        // =========================
        // 3. Stream 使用（可读性更好）
        // =========================
        var result = list.stream()
                .map(String::toUpperCase)
                .toList();

        System.out.println("结果：" + result);


        // =========================
        // 4. Map 推断
        // =========================
        var map = Map.of("A", 1, "B", 2);

        map.forEach((k, v) -> System.out.println(k + ":" + v));


        // =========================
        // 5. for 循环
        // =========================
        for (var i = 0; i < 3; i++) {
            System.out.println("i：" + i);
        }


        // =========================
        // 6. Lambda 参数（JDK11+）
        // =========================
        list.forEach((var s) -> System.out.println("Lambda：" + s));


        // =========================
        // 7. try-with-resources（结合 var）
        // =========================
        try (var stream = list.stream()) {
            stream.forEach(System.out::println);
        }


        // =========================
        // 8. 复杂类型简化（项目常用🔥）
        // =========================
        var users = List.of(
                new User("张三", 20),
                new User("李四", 25)
        );

        var names = users.stream()
                .map(User::name)
                .toList();

        System.out.println("姓名：" + names);


        // =========================
        // 9. 注意：必须初始化
        // =========================
        // var x; ❌ 编译错误

        // =========================
        // 10. 注意：不能用于成员变量
        // =========================
        // class Test { var x = 10; } ❌

        // =========================
        // 11. 注意：避免降低可读性
        // =========================
        var obj = getUser();

        System.out.println("对象：" + obj);
    }

    /**
     * 示例方法
     */
    public static User getUser() {
        return new User("王五", 30);
    }

    /**
     * 示例 record
     */
    public record User(String name, int age) {}

    public static void main(String[] args) {
        varUsage();
    }
}
