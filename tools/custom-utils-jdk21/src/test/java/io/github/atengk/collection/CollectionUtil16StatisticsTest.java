package io.github.atengk.collection;

import io.github.atengk.utils.CollectionUtil;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;


/**
 * CollectionUtil 第 16 类功能测试：统计计算。
 *
 * @author Ateng
 * @since 2026-04-29
 */
class CollectionUtil16StatisticsTest {

    @Test
    void count() {
        long result = CollectionUtil.count(users());
        System.out.println(result);
    }

    @Test
    void sumInt() {
        int result = CollectionUtil.sumInt(users(), User::age);
        System.out.println(result);
    }

    @Test
    void sumIntToLong() {
        long result = CollectionUtil.sumIntToLong(users(), User::age);
        System.out.println(result);
    }

    @Test
    void sumLong() {
        long result = CollectionUtil.sumLong(users(), User::id);
        System.out.println(result);
    }

    @Test
    void sumDouble() {
        double result = CollectionUtil.sumDouble(users(), user -> user.amount().doubleValue());
        System.out.println(result);
    }

    @Test
    void sumBigDecimal() {
        BigDecimal result = CollectionUtil.sumBigDecimal(users(), User::amount);
        System.out.println(result);
    }

    @Test
    void averageInt() {
        OptionalDouble result = CollectionUtil.averageInt(users(), User::age);
        System.out.println(result);
    }

    @Test
    void averageLong() {
        OptionalDouble result = CollectionUtil.averageLong(users(), User::id);
        System.out.println(result);
    }

    @Test
    void averageDouble() {
        OptionalDouble result = CollectionUtil.averageDouble(users(), user -> user.amount().doubleValue());
        System.out.println(result);
    }

    @Test
    void averageBigDecimal() {
        Optional<BigDecimal> result = CollectionUtil.averageBigDecimal(users(), User::amount, 2, RoundingMode.HALF_UP);
        System.out.println(result);
    }

    @Test
    void min() {
        Optional<User> result = CollectionUtil.min(users(), Comparator.comparingInt(User::age));
        System.out.println(result);
    }

    @Test
    void max() {
        Optional<User> result = CollectionUtil.max(users(), Comparator.comparingInt(User::age));
        System.out.println(result);
    }

    @Test
    void minBy() {
        Optional<User> result = CollectionUtil.minBy(users(), User::amount);
        System.out.println(result);
    }

    @Test
    void maxBy() {
        Optional<User> result = CollectionUtil.maxBy(users(), User::amount);
        System.out.println(result);
    }

    @Test
    void minInt() {
        OptionalInt result = CollectionUtil.minInt(users(), User::age);
        System.out.println(result);
    }

    @Test
    void maxInt() {
        OptionalInt result = CollectionUtil.maxInt(users(), User::age);
        System.out.println(result);
    }

    @Test
    void minBigDecimal() {
        Optional<BigDecimal> result = CollectionUtil.minBigDecimal(users(), User::amount);
        System.out.println(result);
    }

    @Test
    void maxBigDecimal() {
        Optional<BigDecimal> result = CollectionUtil.maxBigDecimal(users(), User::amount);
        System.out.println(result);
    }

    @Test
    void summarizeInt() {
        IntSummaryStatistics result = CollectionUtil.summarizeInt(users(), User::age);
        System.out.println(result);
    }

    @Test
    void summarizeDouble() {
        DoubleSummaryStatistics result = CollectionUtil.summarizeDouble(users(), user -> user.amount().doubleValue());
        System.out.println(result);
    }

    private List<User> users() {
        return Arrays.asList(
                new User(1L, "张三", 18, new BigDecimal("10.50")),
                new User(2L, "李四", 25, new BigDecimal("20.00")),
                new User(3L, "王五", 30, new BigDecimal("30.25"))
        );
    }

    record User(Long id, String name, int age, BigDecimal amount) {
    }
}
