package io.github.atengk.basic;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public class BiConsumerTests {

    /**
     * 1. 基础使用
     */
    @Test
    void testBasicBiConsumer() {

        BiConsumer<String, Integer> printer =
                (name, age) ->
                        System.out.println("姓名: " + name + ", 年龄: " + age);

        printer.accept("张三", 20);
    }

    /**
     * 2. Map 遍历
     *
     * Map.forEach 本质接收 BiConsumer
     */
    @Test
    void testMapForEach() {

        Map<String, Integer> map = new HashMap<>();
        map.put("Java", 1);
        map.put("Spring", 2);

        map.forEach((key, value) ->
                System.out.println("Key: " + key + ", Value: " + value));
    }

    /**
     * 3. 方法引用
     */
    @Test
    void testMethodReference() {

        BiConsumer<String, String> logger =
                BiConsumerTests::printLog;

        logger.accept("INFO", "系统启动成功");
    }

    static void printLog(String level, String message) {
        System.out.println("[" + level + "] " + message);
    }

    /**
     * 4. 链式执行
     */
    @Test
    void testAndThen() {

        BiConsumer<String, Integer> first =
                (name, age) ->
                        System.out.println("基础信息: " + name);

        BiConsumer<String, Integer> second =
                (name, age) ->
                        System.out.println("年龄信息: " + age);

        BiConsumer<String, Integer> combined =
                first.andThen(second);

        combined.accept("李四", 25);
    }

    /**
     * 5. 数据更新示例
     */
    @Test
    void testUpdateScenario() {

        BiConsumer<User, Integer> updateAge =
                (user, newAge) -> user.setAge(newAge);

        User user = new User();
        user.setName("王五");
        user.setAge(18);

        updateAge.accept(user, 30);

        System.out.println("更新后年龄: " + user.getAge());
    }

    static class User {

        private String name;
        private Integer age;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getAge() {
            return age;
        }

        public void setAge(Integer age) {
            this.age = age;
        }
    }

}