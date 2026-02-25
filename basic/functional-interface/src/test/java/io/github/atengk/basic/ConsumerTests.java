package io.github.atengk.basic;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class ConsumerTests {

    /**
     * 1. 基础使用
     */
    @Test
    void testBasicConsumer() {

        Consumer<String> consumer = s -> System.out.println("处理数据: " + s);

        consumer.accept("Hello Java8");
    }

    /**
     * 2. 集合遍历
     *
     * forEach 底层接收 Consumer
     */
    @Test
    void testForEachUsage() {

        List<String> list = Arrays.asList("Java", "Spring", "Boot");

        list.forEach(s -> System.out.println("遍历元素: " + s));
    }

    /**
     * 3. 方法引用
     */
    @Test
    void testMethodReference() {

        Consumer<String> printer = System.out::println;

        printer.accept("方法引用示例");
    }

    /**
     * 4. 链式执行
     *
     * Consumer 可以通过 andThen 组合
     */
    @Test
    void testAndThen() {

        Consumer<String> first =
                s -> System.out.println("第一步: " + s);

        Consumer<String> second =
                s -> System.out.println("第二步: " + s.toUpperCase());

        Consumer<String> combined = first.andThen(second);

        combined.accept("java");
    }

    /**
     * 5. 日志场景示例
     *
     * 统一处理输出逻辑
     */
    @Test
    void testLoggingScenario() {

        Consumer<String> logger =
                message -> System.out.println("[INFO] " + message);

        logger.accept("系统启动成功");
    }

}