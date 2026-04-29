package io.github.atengk.collection;

import io.github.atengk.utils.CollectionUtil;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;


/**
 * CollectionUtil 第 10 类功能测试：排序处理。
 *
 * @author Ateng
 * @since 2026-04-29
 */
class CollectionUtil10SortTest {

    @Test
    void sortAsc() {
        List<Integer> result = CollectionUtil.sortAsc(numbers());
        System.out.println(result);
    }

    @Test
    void sortDesc() {
        List<Integer> result = CollectionUtil.sortDesc(numbers());
        System.out.println(result);
    }

    @Test
    void sort() {
        List<User> result = CollectionUtil.sort(users(), Comparator.comparingInt(User::age));
        System.out.println(result);
    }

    @Test
    void sortByAsc() {
        List<User> result = CollectionUtil.sortByAsc(users(), User::age);
        System.out.println(result);
    }

    @Test
    void sortByDesc() {
        List<User> result = CollectionUtil.sortByDesc(users(), User::age);
        System.out.println(result);
    }

    @Test
    void sortByIntAsc() {
        List<User> result = CollectionUtil.sortByIntAsc(users(), User::age);
        System.out.println(result);
    }

    @Test
    void sortByLongDesc() {
        List<User> result = CollectionUtil.sortByLongDesc(users(), User::id);
        System.out.println(result);
    }

    @Test
    void sortByDoubleAsc() {
        List<User> result = CollectionUtil.sortByDoubleAsc(users(), user -> user.amount().doubleValue());
        System.out.println(result);
    }

    @Test
    void sortNullsFirst() {
        List<Integer> result = CollectionUtil.sortNullsFirst(numbers(), Comparator.naturalOrder());
        System.out.println(result);
    }

    @Test
    void reverse() {
        List<User> result = CollectionUtil.reverse(users());
        System.out.println(result);
    }

    @Test
    void sortInPlace() {
        List<User> users = new ArrayList<>(users());
        boolean result = CollectionUtil.sortInPlace(users, Comparator.comparingInt(User::age).reversed());
        System.out.println(result + " -> " + users);
    }

    @Test
    void topByDesc() {
        List<User> result = CollectionUtil.topByDesc(users(), 2, User::amount);
        System.out.println(result);
    }

    @Test
    void sortDistinctAsc() {
        List<Integer> result = CollectionUtil.sortDistinctAsc(numbers());
        System.out.println(result);
    }

    private List<Integer> numbers() {
        return Arrays.asList(3, 1, 2, 3, null);
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
