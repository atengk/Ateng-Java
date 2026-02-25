package io.github.atengk.basic;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.BinaryOperator;

public class BinaryOperatorTests {

    /**
     * 1. 基础使用
     */
    @Test
    void testBasicBinaryOperator() {

        BinaryOperator<Integer> add =
                (a, b) -> a + b;

        Integer result = add.apply(10, 20);

        System.out.println("计算结果: " + result);
    }

    /**
     * 2. Stream 聚合
     *
     * reduce 本质接收 BinaryOperator
     */
    @Test
    void testStreamReduce() {

        List<Integer> list = Arrays.asList(1, 2, 3, 4);

        Integer sum = list.stream()
                .reduce(0, (a, b) -> a + b);

        System.out.println("累计结果: " + sum);
    }

    /**
     * 3. 求最大值
     */
    @Test
    void testMaxValue() {

        List<Integer> list = Arrays.asList(5, 9, 3, 7);

        Optional<Integer> max = list.stream()
                .reduce(BinaryOperator.maxBy(Comparator.naturalOrder()));

        System.out.println("最大值: " + max.orElse(null));
    }

    /**
     * 4. Map.merge 示例
     */
    @Test
    void testMapMerge() {

        java.util.Map<String, Integer> map = new java.util.HashMap<>();

        map.merge("count", 1, (oldVal, newVal) -> oldVal + newVal);
        map.merge("count", 1, (oldVal, newVal) -> oldVal + newVal);

        System.out.println("合并结果: " + map.get("count"));
    }

    /**
     * 5. 金额累计示例
     */
    @Test
    void testAmountAggregation() {

        List<Double> prices = Arrays.asList(99.9, 199.9, 299.9);

        Double total = prices.stream()
                .reduce(0.0, Double::sum);

        System.out.println("总金额: " + total);
    }

}