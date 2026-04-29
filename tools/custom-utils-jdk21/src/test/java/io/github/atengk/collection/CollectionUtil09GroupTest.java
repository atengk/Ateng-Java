package io.github.atengk.collection;

import io.github.atengk.utils.CollectionUtil;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.*;


/**
 * CollectionUtil 第 9 类功能测试：分组处理。
 *
 * @author Ateng
 * @since 2026-04-29
 */
class CollectionUtil09GroupTest {

    @Test
    void groupBy() {
        Map<String, List<User>> result = CollectionUtil.groupBy(users(), User::city);
        System.out.println(result);
    }

    @Test
    void groupByToSet() {
        Map<Integer, Set<User>> result = CollectionUtil.groupByToSet(users(), User::age);
        System.out.println(result);
    }

    @Test
    void groupMapping() {
        Map<String, List<String>> result = CollectionUtil.groupMapping(users(), User::city, User::name);
        System.out.println(result);
    }

    @Test
    void groupMappingToSet() {
        Map<String, Set<Integer>> result = CollectionUtil.groupMappingToSet(users(), User::city, User::age);
        System.out.println(result);
    }

    @Test
    void groupByTo() {
        Map<String, LinkedHashSet<User>> result = CollectionUtil.groupByTo(users(), User::city, LinkedHashSet::new);
        System.out.println(result);
    }

    @Test
    void groupCount() {
        Map<String, Long> result = CollectionUtil.groupCount(users(), User::city);
        System.out.println(result);
    }

    @Test
    void groupSumInt() {
        Map<String, Integer> result = CollectionUtil.groupSumInt(users(), User::city, User::age);
        System.out.println(result);
    }

    @Test
    void groupSumLong() {
        Map<String, Long> result = CollectionUtil.groupSumLong(users(), User::city, User::id);
        System.out.println(result);
    }

    @Test
    void groupSumDouble() {
        Map<String, Double> result = CollectionUtil.groupSumDouble(users(), User::city, user -> user.amount().doubleValue());
        System.out.println(result);
    }

    @Test
    void groupSumBigDecimal() {
        Map<String, BigDecimal> result = CollectionUtil.groupSumBigDecimal(users(), User::city, User::amount);
        System.out.println(result);
    }

    @Test
    void partition() {
        Map<Boolean, List<User>> result = CollectionUtil.partition(users(), user -> user.age() >= 25);
        System.out.println(result);
    }

    private List<User> users() {
        return Arrays.asList(
                new User(1L, "张三", 18, "杭州", new BigDecimal("10.50")),
                new User(2L, "李四", 25, "上海", new BigDecimal("20.00")),
                new User(3L, "王五", 30, "杭州", new BigDecimal("30.25"))
        );
    }

    record User(Long id, String name, int age, String city, BigDecimal amount) {
    }
}
