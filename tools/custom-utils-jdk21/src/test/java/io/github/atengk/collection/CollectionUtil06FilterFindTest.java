package io.github.atengk.collection;

import io.github.atengk.utils.CollectionUtil;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;


/**
 * CollectionUtil 第 6 类功能测试：过滤与查找。
 *
 * @author Ateng
 * @since 2026-04-29
 */
class CollectionUtil06FilterFindTest {

    @Test
    void filter() {
        List<User> result = CollectionUtil.filter(users(), user -> user.age() >= 20);
        System.out.println(result);
    }

    @Test
    void filterToSet() {
        Set<User> result = CollectionUtil.filterToSet(users(), user -> "杭州".equals(user.city()));
        System.out.println(result);
    }

    @Test
    void reject() {
        List<User> result = CollectionUtil.reject(users(), user -> user.age() >= 20);
        System.out.println(result);
    }

    @Test
    void findFirst() {
        User result = CollectionUtil.findFirst(users(), user -> "杭州".equals(user.city()));
        System.out.println(result);
    }

    @Test
    void findLast() {
        User result = CollectionUtil.findLast(users(), user -> user.age() >= 20);
        System.out.println(result);
    }

    @Test
    void findFirstOptional() {
        Optional<User> result = CollectionUtil.findFirstOptional(users(), user -> user.age() >= 20);
        System.out.println(result);
    }

    @Test
    void anyMatch() {
        boolean result = CollectionUtil.anyMatch(users(), user -> "杭州".equals(user.city()));
        System.out.println(result);
    }

    @Test
    void allMatch() {
        boolean result = CollectionUtil.allMatch(users(), user -> user.age() >= 18);
        System.out.println(result);
    }

    @Test
    void noneMatch() {
        boolean result = CollectionUtil.noneMatch(users(), user -> user.age() < 10);
        System.out.println(result);
    }

    @Test
    void countMatches() {
        long result = CollectionUtil.countMatches(users(), user -> user.age() >= 20);
        System.out.println(result);
    }

    @Test
    void indexOf() {
        int result = CollectionUtil.indexOf(users(), user -> "上海".equals(user.city()));
        System.out.println(result);
    }

    @Test
    void contains() {
        List<User> users = users();
        boolean result = CollectionUtil.contains(users, users.get(0));
        System.out.println(result);
    }

    private List<User> users() {
        return Arrays.asList(
                new User(1L, "张三", 18, "杭州"),
                new User(2L, "李四", 25, "上海"),
                new User(3L, "王五", 30, "杭州")
        );
    }

    record User(Long id, String name, int age, String city) {
    }
}
