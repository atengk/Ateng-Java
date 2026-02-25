package io.github.atengk.basic;

import org.junit.jupiter.api.Test;

import java.util.function.BiPredicate;

public class BiPredicateTests {

    /**
     * 1. 基础使用
     */
    @Test
    void testBasicBiPredicate() {

        BiPredicate<String, String> equals =
                (a, b) -> a.equals(b);

        System.out.println("判断结果: " + equals.test("Java", "Java"));
    }

    /**
     * 2. 数值比较
     */
    @Test
    void testNumberCompare() {

        BiPredicate<Integer, Integer> greaterThan =
                (a, b) -> a > b;

        System.out.println("是否大于: " + greaterThan.test(10, 5));
    }

    /**
     * 3. 条件组合
     */
    @Test
    void testBiPredicateCompose() {

        BiPredicate<String, String> notNull =
                (a, b) -> a != null && b != null;

        BiPredicate<String, String> equals =
                String::equals;

        BiPredicate<String, String> condition =
                notNull.and(equals);

        System.out.println("组合判断: " + condition.test("Spring", "Spring"));
    }

    /**
     * 4. 权限校验示例
     */
    @Test
    void testPermissionScenario() {

        BiPredicate<User, String> hasRole =
                (user, role) -> user.getRole().equals(role);

        User user = new User();
        user.setRole("ADMIN");

        System.out.println("是否有权限: " + hasRole.test(user, "ADMIN"));
    }

    static class User {

        private String role;

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }
    }


}