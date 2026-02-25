package io.github.atengk.basic;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class PredicateTests {

    /**
     * 1. 基础使用
     */
    @Test
    void testBasicPredicate() {

        Predicate<String> isEmpty =
                s -> s == null || s.isEmpty();

        System.out.println("判断结果: " + isEmpty.test(""));
    }

    /**
     * 2. Stream 过滤
     *
     * filter 本质接收 Predicate
     */
    @Test
    void testStreamFilter() {

        List<String> list = Arrays.asList("Java", "", "Spring", "Boot");

        List<String> result = list.stream()
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        System.out.println("过滤结果: " + result);
    }

    /**
     * 3. 条件组合
     */
    @Test
    void testPredicateCompose() {

        Predicate<String> notEmpty =
                s -> s != null && !s.isEmpty();

        Predicate<String> longerThan3 =
                s -> s.length() > 3;

        Predicate<String> condition =
                notEmpty.and(longerThan3);

        System.out.println("组合判断: " + condition.test("Java"));
    }

    /**
     * 4. 参数校验示例
     */
    @Test
    void testValidationScenario() {

        Predicate<User> usernameValid =
                user -> user.getUsername() != null && user.getUsername().length() >= 4;

        User user = new User();
        user.setUsername("admin");

        System.out.println("用户名是否合法: " + usernameValid.test(user));
    }

    static class User {

        private String username;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }
    }

}