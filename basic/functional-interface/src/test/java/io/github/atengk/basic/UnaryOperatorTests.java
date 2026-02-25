package io.github.atengk.basic;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

public class UnaryOperatorTests {

    /**
     * 1. 基础使用
     */
    @Test
    void testBasicUnaryOperator() {

        UnaryOperator<String> toUpper =
                s -> s.toUpperCase();

        String result = toUpper.apply("java");

        System.out.println("转换结果: " + result);
    }

    /**
     * 2. 数值处理
     */
    @Test
    void testNumberProcess() {

        UnaryOperator<Integer> doubleValue =
                n -> n * 2;

        System.out.println("处理结果: " + doubleValue.apply(10));
    }

    /**
     * 3. Stream 使用
     */
    @Test
    void testStreamUsage() {

        List<String> list = Arrays.asList("java", "spring", "boot");

        List<String> result = list.stream()
                .map(String::toUpperCase)
                .collect(Collectors.toList());

        System.out.println("转换集合: " + result);
    }

    /**
     * 5. 数据标准化示例
     */
    @Test
    void testNormalizeScenario() {

        UnaryOperator<User> normalizeUser =
                user -> {
                    user.setUsername(user.getUsername().trim().toLowerCase());
                    return user;
                };

        User user = new User();
        user.setUsername("  ADMIN  ");

        normalizeUser.apply(user);

        System.out.println("标准化后用户名: " + user.getUsername());
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