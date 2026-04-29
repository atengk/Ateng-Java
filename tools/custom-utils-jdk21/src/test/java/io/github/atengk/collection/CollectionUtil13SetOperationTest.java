package io.github.atengk.collection;

import io.github.atengk.utils.CollectionUtil;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * CollectionUtil 第 13 类功能测试：集合运算。
 *
 * @author Ateng
 * @since 2026-04-29
 */
class CollectionUtil13SetOperationTest {

    @Test
    void union() {
        Set<String> result = CollectionUtil.union(first(), second());
        System.out.println(result);
    }

    @Test
    void unionArray() {
        Set<String> result = CollectionUtil.union(new String[]{"A", "B"}, new String[]{"B", "C"});
        System.out.println(result);
    }

    @Test
    void intersection() {
        Set<String> result = CollectionUtil.intersection(first(), second());
        System.out.println(result);
    }

    @Test
    void intersectionArray() {
        Set<String> result = CollectionUtil.intersection(new String[]{"A", "B"}, new String[]{"B", "C"});
        System.out.println(result);
    }

    @Test
    void difference() {
        Set<String> result = CollectionUtil.difference(first(), second());
        System.out.println(result);
    }

    @Test
    void symmetricDifference() {
        Set<String> result = CollectionUtil.symmetricDifference(first(), second());
        System.out.println(result);
    }

    @Test
    void hasIntersection() {
        boolean result = CollectionUtil.hasIntersection(first(), second());
        System.out.println(result);
    }

    @Test
    void disjoint() {
        boolean result = CollectionUtil.disjoint(first(), List.of("X"));
        System.out.println(result);
    }

    @Test
    void containsAnyElement() {
        boolean result = CollectionUtil.containsAnyElement(first(), second());
        System.out.println(result);
    }

    @Test
    void containsAllElements() {
        boolean result = CollectionUtil.containsAllElements(first(), List.of("A", "B"));
        System.out.println(result);
    }

    @Test
    void equalsIgnoreOrder() {
        boolean result = CollectionUtil.equalsIgnoreOrder(first(), Arrays.asList("C", "B", "A", "A"));
        System.out.println(result);
    }

    @Test
    void equalsIgnoreOrderWithCount() {
        boolean result = CollectionUtil.equalsIgnoreOrderWithCount(first(), Arrays.asList("A", "B", "C", "A"));
        System.out.println(result);
    }

    @Test
    void elementCount() {
        Map<String, Long> result = CollectionUtil.elementCount(first());
        System.out.println(result);
    }

    @Test
    void keyCount() {
        Map<String, Long> result = CollectionUtil.keyCount(users(), User::city);
        System.out.println(result);
    }

    @Test
    void isSubset() {
        boolean result = CollectionUtil.isSubset(List.of("A", "B"), first());
        System.out.println(result);
    }

    @Test
    void isSuperset() {
        boolean result = CollectionUtil.isSuperset(first(), List.of("A", "B"));
        System.out.println(result);
    }

    @Test
    void removeAllToList() {
        List<String> result = CollectionUtil.removeAllToList(first(), second());
        System.out.println(result);
    }

    @Test
    void retainAllToList() {
        List<String> result = CollectionUtil.retainAllToList(first(), second());
        System.out.println(result);
    }

    private List<String> first() {
        return Arrays.asList("A", "B", "C", "A");
    }

    private List<String> second() {
        return Arrays.asList("B", "C", "D");
    }

    private List<User> users() {
        return Arrays.asList(
                new User(1L, "张三", "杭州"),
                new User(2L, "李四", "上海"),
                new User(3L, "王五", "杭州")
        );
    }

    record User(Long id, String name, String city) {
    }
}
