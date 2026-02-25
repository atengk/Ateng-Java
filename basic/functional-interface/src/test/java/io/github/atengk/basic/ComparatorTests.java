package io.github.atengk.basic;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ComparatorTests {

    static class User {

        private String name;
        private Integer age;

        public User(String name, Integer age) {
            this.name = name;
            this.age = age;
        }

        public String getName() {
            return name;
        }

        public Integer getAge() {
            return age;
        }

        @Override
        public String toString() {
            return "User{name='" + name + "', age=" + age + '}';
        }
    }

    /**
     * 1. 基础排序
     */
    @Test
    void testBasicComparator() {

        List<String> list = Arrays.asList("Java", "Go", "Python");

        list.sort((a, b) -> a.length() - b.length());

        System.out.println("排序结果: " + list);
    }

    /**
     * 2. 推荐写法 comparing
     */
    @Test
    void testComparing() {

        List<User> list = Arrays.asList(
                new User("Tom", 30),
                new User("Jack", 20),
                new User("Lucy", 25)
        );

        list.sort(Comparator.comparing(User::getAge));

        System.out.println("按年龄排序: " + list);
    }

    /**
     * 3. 多字段排序
     */
    @Test
    void testMultiSort() {

        List<User> list = Arrays.asList(
                new User("Tom", 30),
                new User("Tom", 20),
                new User("Jack", 25)
        );

        list.sort(
                Comparator.comparing(User::getName)
                        .thenComparing(User::getAge)
        );

        System.out.println("多字段排序: " + list);
    }

    /**
     * 4. 倒序排序
     */
    @Test
    void testReverseOrder() {

        List<Integer> list = Arrays.asList(5, 1, 9, 3);

        list.sort(Comparator.reverseOrder());

        System.out.println("倒序结果: " + list);
    }

    /**
     * 5. Stream.sorted 使用
     */
    @Test
    void testStreamSorted() {

        List<Integer> list = Arrays.asList(3, 1, 4, 2);

        List<Integer> sorted = list.stream()
                .sorted()
                .collect(Collectors.toList());

        System.out.println("Stream排序: " + sorted);
    }

}