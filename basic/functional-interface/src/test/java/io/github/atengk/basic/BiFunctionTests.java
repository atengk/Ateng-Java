package io.github.atengk.basic;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

public class BiFunctionTests {

    /**
     * 1. 基础使用
     */
    @Test
    void testBasicBiFunction() {

        BiFunction<Integer, Integer, Integer> add =
                (a, b) -> a + b;

        Integer result = add.apply(10, 20);

        System.out.println("计算结果: " + result);
    }

    /**
     * 2. 字符串拼接
     */
    @Test
    void testStringConcat() {

        BiFunction<String, String, String> concat =
                (a, b) -> a + "-" + b;

        String result = concat.apply("Java", "Spring");

        System.out.println("拼接结果: " + result);
    }

    /**
     * 3. Map.compute 使用
     *
     * compute 本质接收 BiFunction
     */
    @Test
    void testMapCompute() {

        Map<String, Integer> map = new HashMap<>();
        map.put("count", 1);

        map.compute("count", (key, value) ->
                value == null ? 1 : value + 1
        );

        System.out.println("更新结果: " + map.get("count"));
    }

    /**
     * 4. 函数组合
     *
     * 先执行 BiFunction，再执行后续 Function
     */
    @Test
    void testAndThen() {

        BiFunction<Integer, Integer, Integer> multiply =
                (a, b) -> a * b;

        Integer result = multiply
                .andThen(r -> r + 10)
                .apply(5, 2);

        System.out.println("组合结果: " + result);
    }

    /**
     * 5. 金额计算示例
     */
    @Test
    void testPriceCalculation() {

        BiFunction<Double, Double, Double> calculateTotal =
                (price, quantity) -> price * quantity;

        Double total = calculateTotal.apply(99.9, 3.0);

        System.out.println("总金额: " + total);
    }

}