package io.github.atengk.collection;

import io.github.atengk.utils.CollectionUtil;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.*;
import java.util.stream.Stream;


/**
 * CollectionUtil 第 19 类功能测试：安全遍历。
 *
 * @author Ateng
 * @since 2026-04-29
 */
class CollectionUtil19SafeForEachTest {

    @Test
    void forEach() {
        CollectionUtil.forEach(users(), user -> System.out.println(user));
    }

    @Test
    void forEachNotNull() {
        String[] array = {"A", null, "B"};
        CollectionUtil.forEachNotNull(array, item -> System.out.println(item));
    }

    @Test
    void forEachIndexed() {
        CollectionUtil.forEachIndexed(users(), (user, index) -> System.out.println(index + " -> " + user));
    }

    @Test
    void forEachReverse() {
        CollectionUtil.forEachReverse(users(), user -> System.out.println(user));
    }

    @Test
    void forEachReverseIndexed() {
        String[] array = {"A", "B", "C"};
        CollectionUtil.forEachReverseIndexed(array, (item, index) -> System.out.println(index + " -> " + item));
    }

    @Test
    void forEachWhile() {
        int result = CollectionUtil.forEachWhile(users(), user -> {
            System.out.println(user);
            return user.age() < 30;
        });
        System.out.println(result);
    }

    @Test
    void forEachIndexedWhile() {
        int result = CollectionUtil.forEachIndexedWhile(users(), (user, index) -> {
            System.out.println(index + " -> " + user);
            return index < 1;
        });
        System.out.println(result);
    }

    @Test
    void forEachSafely() {
        int result = CollectionUtil.forEachSafely(users(), user -> {
            if (user.age() > 20) {
                throw new IllegalStateException("模拟异常");
            }
            System.out.println(user);
        }, (user, exception) -> System.out.println(user + " -> " + exception.getMessage()));
        System.out.println(result);
    }

    @Test
    void forEachCollectFailures() {
        List<User> result = CollectionUtil.forEachCollectFailures(users(), user -> {
            if (user.age() > 20) {
                throw new IllegalStateException("模拟异常");
            }
            System.out.println(user);
        });
        System.out.println(result);
    }

    @Test
    void forEachEntry() {
        CollectionUtil.forEachEntry(scoreMap(), (key, value) -> System.out.println(key + "=" + value));
    }

    @Test
    void forEachKey() {
        CollectionUtil.forEachKey(scoreMap(), key -> System.out.println(key));
    }

    @Test
    void forEachValue() {
        CollectionUtil.forEachValue(scoreMap(), value -> System.out.println(value));
    }

    @Test
    void forEachEntryIndexed() {
        CollectionUtil.forEachEntryIndexed(scoreMap(), (entry, index) -> System.out.println(index + " -> " + entry));
    }

    @Test
    void replaceEach() {
        List<User> result = CollectionUtil.replaceEach(users(), user -> new User(user.id(), user.name() + "_新", user.age()));
        System.out.println(result);
    }

    @Test
    void replaceEachInPlace() {
        List<String> names = new ArrayList<>(Arrays.asList("A", "B", "C"));
        boolean result = CollectionUtil.replaceEachInPlace(names, item -> item + "_新");
        System.out.println(result + " -> " + names);
    }

    @Test
    void forEachCount() {
        int result = CollectionUtil.forEachCount(users(), user -> System.out.println(user));
        System.out.println(result);
    }

    private List<User> users() {
        return Arrays.asList(
                new User(1L, "张三", 18),
                new User(2L, "李四", 25),
                new User(3L, "王五", 30)
        );
    }

    private Map<String, Integer> scoreMap() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("A", 90);
        map.put("B", 95);
        return map;
    }

    record User(Long id, String name, int age) {
    }
}
