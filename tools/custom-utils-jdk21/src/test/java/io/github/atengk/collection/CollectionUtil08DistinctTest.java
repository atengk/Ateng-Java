package io.github.atengk.collection;

import io.github.atengk.utils.CollectionUtil;
import org.junit.jupiter.api.Test;

import java.util.*;


/**
 * CollectionUtil 第 8 类功能测试：去重处理。
 *
 * @author Ateng
 * @since 2026-04-29
 */
class CollectionUtil08DistinctTest {

    @Test
    void distinct() {
        List<String> result = CollectionUtil.distinct(names());
        System.out.println(result);
    }

    @Test
    void distinctToSet() {
        Set<String> result = CollectionUtil.distinctToSet(names());
        System.out.println(result);
    }

    @Test
    void distinctBy() {
        List<User> result = CollectionUtil.distinctBy(users(), User::name);
        System.out.println(result);
    }

    @Test
    void distinctByKeepLast() {
        List<User> result = CollectionUtil.distinctByKeepLast(users(), User::name);
        System.out.println(result);
    }

    @Test
    void distinctNotNull() {
        List<String> result = CollectionUtil.distinctNotNull(names());
        System.out.println(result);
    }

    @Test
    void distinctByNotNullKey() {
        List<User> result = CollectionUtil.distinctByNotNullKey(users(), User::city);
        System.out.println(result);
    }

    @Test
    void distinctByPredicate() {
        List<User> result = CollectionUtil.distinctByPredicate(users(), (left, right) -> Objects.equals(left.name(), right.name()));
        System.out.println(result);
    }

    @Test
    void hasDuplicate() {
        boolean result = CollectionUtil.hasDuplicate(names());
        System.out.println(result);
    }

    @Test
    void hasDuplicateBy() {
        boolean result = CollectionUtil.hasDuplicateBy(users(), User::name);
        System.out.println(result);
    }

    @Test
    void duplicateElements() {
        List<String> result = CollectionUtil.duplicateElements(names());
        System.out.println(result);
    }

    @Test
    void duplicateKeys() {
        List<String> result = CollectionUtil.duplicateKeys(users(), User::name);
        System.out.println(result);
    }

    @Test
    void distinctInPlace() {
        List<String> names = new ArrayList<>(names());
        boolean result = CollectionUtil.distinctInPlace(names);
        System.out.println(result + " -> " + names);
    }

    private List<String> names() {
        return Arrays.asList("A", "B", "A", null, "C", "B");
    }

    private List<User> users() {
        return Arrays.asList(
                new User(1L, "张三", 18, "杭州"),
                new User(2L, "李四", 25, "上海"),
                new User(3L, "张三", 30, "杭州"),
                new User(4L, "赵六", 28, null)
        );
    }

    record User(Long id, String name, int age, String city) {
    }
}
