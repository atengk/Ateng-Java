package io.github.atengk.basic;

import java.util.Optional;

/**
 * Optional 优雅判空示例（避免 NPE）
 */
public class OptionalExample {

    /**
     * 用户实体类（模拟项目中的对象）
     */
    static class User {
        private String name;
        private String email;

        public User(String name, String email) {
            this.name = name;
            this.email = email;
        }

        public String getName() {
            return name;
        }

        public String getEmail() {
            return email;
        }
    }

    /**
     * 模拟查询用户（可能返回 null）
     */
    public static User getUser(boolean exists) {
        return exists ? new User("张三", "test@example.com") : null;
    }

    /**
     * 核心方法：Optional 常用用法
     */
    public static void optionalUsage() {

        // =========================
        // 1. ofNullable（安全创建 Optional）
        // =========================
        Optional<User> userOpt = Optional.ofNullable(getUser(true));

        // =========================
        // 2. isPresent / ifPresent（避免 null 判断）
        // =========================
        userOpt.ifPresent(user -> System.out.println("用户存在：" + user.getName()));

        // =========================
        // 3. orElse（提供默认值，立即执行）
        // =========================
        User user1 = Optional.ofNullable(getUser(false))
                .orElse(new User("默认用户", "default@test.com"));

        System.out.println("orElse：" + user1.getName());

        // =========================
        // 4. orElseGet（推荐，懒加载）
        // =========================
        User user2 = Optional.ofNullable(getUser(false))
                .orElseGet(() -> {
                    System.out.println("执行默认逻辑");
                    return new User("懒加载用户", "lazy@test.com");
                });

        System.out.println("orElseGet：" + user2.getName());

        // =========================
        // 5. orElseThrow（为空直接抛异常）
        // =========================
        try {
            User user3 = Optional.ofNullable(getUser(false))
                    .orElseThrow(() -> new RuntimeException("用户不存在"));
        } catch (Exception e) {
            System.out.println("异常：" + e.getMessage());
        }

        // =========================
        // 6. map（链式调用，避免多层判空）
        // =========================
        String email = Optional.ofNullable(getUser(true))
                .map(User::getEmail)
                .map(String::toUpperCase)
                .orElse("无邮箱");

        System.out.println("邮箱：" + email);

        // =========================
        // 7. flatMap（避免 Optional 嵌套）
        // =========================
        Optional<String> emailOpt =
                Optional.ofNullable(getUser(true))
                        .flatMap(user -> Optional.ofNullable(user.getEmail()));

        System.out.println("flatMap：" + emailOpt.orElse("无"));

        // =========================
        // 8. filter（条件过滤）
        // =========================
        Optional<User> filtered =
                Optional.ofNullable(getUser(true))
                        .filter(user -> user.getName().length() > 2);

        System.out.println("filter：" + filtered.map(User::getName).orElse("不满足条件"));

        // =========================
        // 9. 链式防空（项目推荐写法）
        // =========================
        String result =
                Optional.ofNullable(getUser(true))
                        .map(User::getEmail)
                        .filter(e -> e.contains("@"))
                        .orElse("非法邮箱");

        System.out.println("链式结果：" + result);

        // =========================
        // 10. ifPresentOrElse（JDK9+）
        // =========================
        Optional.ofNullable(getUser(false))
                .ifPresentOrElse(
                        user -> System.out.println("存在：" + user.getName()),
                        () -> System.out.println("用户为空")
                );
    }

    public static void main(String[] args) {
        optionalUsage();
    }
}
