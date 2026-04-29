package io.github.atengk.collection;

import io.github.atengk.utils.CollectionUtil;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/**
 * CollectionUtil 第 20 类功能测试：断言与校验。
 *
 * @author Ateng
 * @since 2026-04-29
 */
class CollectionUtil20AssertValidateTest {

    @Test
    void requireNotNull() {
        List<String> result = CollectionUtil.requireNotNull(names());
        System.out.println(result);
    }

    @Test
    void requireNotEmpty() {
        List<String> result = CollectionUtil.requireNotEmpty(names());
        System.out.println(result);
    }

    @Test
    void requireSize() {
        List<String> result = CollectionUtil.requireSize(names(), 3);
        System.out.println(result);
    }

    @Test
    void requireMinSize() {
        List<String> result = CollectionUtil.requireMinSize(names(), 2);
        System.out.println(result);
    }

    @Test
    void requireMaxSize() {
        List<String> result = CollectionUtil.requireMaxSize(names(), 5);
        System.out.println(result);
    }

    @Test
    void requireSizeBetween() {
        List<String> result = CollectionUtil.requireSizeBetween(names(), 1, 5);
        System.out.println(result);
    }

    @Test
    void requireNoNullElements() {
        List<String> result = CollectionUtil.requireNoNullElements(names());
        System.out.println(result);
    }

    @Test
    void requireNoNullKeys() {
        Map<String, Integer> result = CollectionUtil.requireNoNullKeys(scoreMap());
        System.out.println(result);
    }

    @Test
    void requireNoNullValues() {
        Map<String, Integer> result = CollectionUtil.requireNoNullValues(scoreMap());
        System.out.println(result);
    }

    @Test
    void requireContains() {
        List<String> result = CollectionUtil.requireContains(names(), "A");
        System.out.println(result);
    }

    @Test
    void requireNotContains() {
        List<String> result = CollectionUtil.requireNotContains(names(), "X");
        System.out.println(result);
    }

    @Test
    void requireContainsAll() {
        List<String> result = CollectionUtil.requireContainsAll(names(), Arrays.asList("A", "B"));
        System.out.println(result);
    }

    @Test
    void requireContainsAny() {
        List<String> result = CollectionUtil.requireContainsAny(names(), Arrays.asList("X", "A"));
        System.out.println(result);
    }

    @Test
    void requireContainsKey() {
        Map<String, Integer> result = CollectionUtil.requireContainsKey(scoreMap(), "A");
        System.out.println(result);
    }

    @Test
    void requireContainsAllKeys() {
        Map<String, Integer> result = CollectionUtil.requireContainsAllKeys(scoreMap(), Arrays.asList("A", "B"));
        System.out.println(result);
    }

    @Test
    void requireAllMatch() {
        List<User> result = CollectionUtil.requireAllMatch(users(), user -> user.age() >= 18);
        System.out.println(result);
    }

    @Test
    void requireAnyMatch() {
        List<User> result = CollectionUtil.requireAnyMatch(users(), user -> "杭州".equals(user.city()));
        System.out.println(result);
    }

    @Test
    void requireNoneMatch() {
        List<User> result = CollectionUtil.requireNoneMatch(users(), user -> user.age() < 10);
        System.out.println(result);
    }

    @Test
    void requireNoDuplicate() {
        List<String> result = CollectionUtil.requireNoDuplicate(names());
        System.out.println(result);
    }

    @Test
    void requireNoDuplicateBy() {
        List<User> result = CollectionUtil.requireNoDuplicateBy(users(), User::id);
        System.out.println(result);
    }

    @Test
    void requireHasIntersection() {
        CollectionUtil.requireHasIntersection(names(), Arrays.asList("B", "X"));
        System.out.println("通过");
    }

    @Test
    void requireDisjoint() {
        CollectionUtil.requireDisjoint(names(), Arrays.asList("X", "Y"));
        System.out.println("通过");
    }

    @Test
    void requireEqualsIgnoreOrder() {
        CollectionUtil.requireEqualsIgnoreOrder(names(), Arrays.asList("C", "B", "A"));
        System.out.println("通过");
    }

    @Test
    void requireSubset() {
        CollectionUtil.requireSubset(Arrays.asList("A", "B"), names());
        System.out.println("通过");
    }

    @Test
    void requireSuperset() {
        CollectionUtil.requireSuperset(names(), Arrays.asList("A", "B"));
        System.out.println("通过");
    }

    @Test
    void requireValidIndex() {
        CollectionUtil.requireValidIndex(names(), 1);
        System.out.println("通过");
    }

    @Test
    void requireValidRange() {
        CollectionUtil.requireValidRange(names(), 0, 2);
        System.out.println("通过");
    }

    @Test
    void validNotEmpty() {
        boolean result = CollectionUtil.validNotEmpty(names());
        System.out.println(result);
    }

    @Test
    void validSizeBetween() {
        boolean result = CollectionUtil.validSizeBetween(names(), 1, 5);
        System.out.println(result);
    }

    @Test
    void validIndex() {
        String[] array = {"A", "B", "C"};
        boolean result = CollectionUtil.validIndex(array, 2);
        System.out.println(result);
    }

    @Test
    void validNoNullEntries() {
        boolean result = CollectionUtil.validNoNullEntries(scoreMap());
        System.out.println(result);
    }

    private List<String> names() {
        return Arrays.asList("A", "B", "C");
    }

    private Map<String, Integer> scoreMap() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("A", 90);
        map.put("B", 95);
        return map;
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
