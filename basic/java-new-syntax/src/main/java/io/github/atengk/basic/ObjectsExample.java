package io.github.atengk.basic;

import java.util.*;
import java.util.Objects;

/**
 * Objects 工具类示例（JDK8+ 增强）
 */
public class ObjectsExample {

    /**
     * 示例实体
     */
    static class User {
        private String name;
        private Integer age;

        public User(String name, Integer age) {
            this.name = name;
            this.age = age;
        }

        public String getName() { return name; }
        public Integer getAge() { return age; }

        @Override
        public String toString() {
            return name + "(" + age + ")";
        }
    }

    /**
     * 核心方法：Objects 常用操作
     */
    public static void objectsUsage() {

        // =========================
        // 1. 判空（isNull / nonNull）
        // =========================
        User user = null;

        System.out.println("isNull：" + Objects.isNull(user));
        System.out.println("nonNull：" + Objects.nonNull(user));


        // =========================
        // 2. requireNonNull（强制非空🔥）
        // =========================
        try {
            Objects.requireNonNull(user, "用户不能为空");
        } catch (Exception e) {
            System.out.println("异常：" + e.getMessage());
        }


        // =========================
        // 3. requireNonNullElse（默认值）
        // =========================
        User defaultUser = new User("默认", 0);

        User u1 = Objects.requireNonNullElse(user, defaultUser);

        System.out.println("默认值：" + u1);


        // =========================
        // 4. requireNonNullElseGet（懒加载）
        // =========================
        User u2 = Objects.requireNonNullElseGet(user, () -> {
            System.out.println("执行创建默认用户");
            return new User("懒加载", 1);
        });

        System.out.println("懒加载：" + u2);


        // =========================
        // 5. equals（安全比较🔥）
        // =========================
        String a = null;
        String b = "test";

        System.out.println("equals：" + Objects.equals(a, b)); // 不会 NPE


        // =========================
        // 6. deepEquals（集合比较）
        // =========================
        List<String> list1 = List.of("A", "B");
        List<String> list2 = List.of("A", "B");

        System.out.println("deepEquals：" + Objects.deepEquals(list1, list2));


        // =========================
        // 7. hash（生成 hashCode）
        // =========================
        int hash = Objects.hash("A", 1);

        System.out.println("hash：" + hash);


        // =========================
        // 8. compare（比较器）
        // =========================
        int cmp = Objects.compare(10, 20, Integer::compareTo);

        System.out.println("compare：" + cmp);


        // =========================
        // 9. Stream 判空（项目常用🔥）
        // =========================
        List<User> users = Arrays.asList(
                new User("张三", 20),
                null,
                new User("李四", 25)
        );

        List<User> filtered = users.stream()
                .filter(Objects::nonNull)
                .toList();

        System.out.println("过滤 null：" + filtered);


        // =========================
        // 10. 项目实战：参数校验🔥
        // =========================
        createUser("王五", 30);

        try {
            createUser(null, 10);
        } catch (Exception e) {
            System.out.println("参数异常：" + e.getMessage());
        }
    }

    /**
     * 模拟创建用户（参数校验）
     */
    public static User createUser(String name, Integer age) {

        // 参数校验（推荐写法）
        Objects.requireNonNull(name, "name 不能为空");
        Objects.requireNonNull(age, "age 不能为空");

        return new User(name, age);
    }

    public static void main(String[] args) {
        objectsUsage();
    }
}
