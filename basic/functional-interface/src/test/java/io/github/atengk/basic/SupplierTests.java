package io.github.atengk.basic;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class SupplierTests {

    /**
     * 示例实体类
     */
    static class User {

        private String id;
        private LocalDateTime createTime;

        public void setId(String id) {
            this.id = id;
        }

        public void setCreateTime(LocalDateTime createTime) {
            this.createTime = createTime;
        }

        @Override
        public String toString() {
            return "User{id='" + id + "', createTime=" + createTime + '}';
        }
    }

    /**
     * 1. 基础使用
     */
    @Test
    void testBasicSupplier() {
        Supplier<String> supplier = () -> "Hello Java8";

        System.out.println("基础使用: " + supplier.get());
    }

    /**
     * 2. 延迟执行
     *
     * 只有调用 get() 时才会真正执行
     */
    @Test
    void testLazyExecution() {

        Supplier<String> timeSupplier = () -> {
            System.out.println("正在生成时间...");
            return LocalDateTime.now().toString();
        };

        System.out.println("Supplier 已创建，但未执行");
        System.out.println("调用结果: " + timeSupplier.get());
    }

    /**
     * 3. Optional 默认值
     *
     * 推荐使用 orElseGet，而不是 orElse
     * 因为 orElse 会立即执行
     */
    @Test
    void testOptionalDefaultValue() {

        String value = null;

        String result = Optional.ofNullable(value)
                .orElseGet(() -> "默认值");

        System.out.println("Optional默认值: " + result);
    }

    /**
     * 4. Stream 生成数据
     *
     * Stream.generate 本质就是接收 Supplier
     */
    @Test
    void testStreamGenerate() {

        Stream<String> stream = Stream.generate(
                () -> UUID.randomUUID().toString()
        );

        stream.limit(3)
                .forEach(s -> System.out.println("生成UUID: " + s));
    }

    /**
     * 5. 工厂模式
     *
     * 用于对象创建逻辑封装
     */
    @Test
    void testFactoryPattern() {

        Supplier<User> userFactory = () -> {
            User user = new User();
            user.setId(UUID.randomUUID().toString());
            user.setCreateTime(LocalDateTime.now());
            return user;
        };

        User user = userFactory.get();

        System.out.println("工厂创建对象: " + user);
    }
}