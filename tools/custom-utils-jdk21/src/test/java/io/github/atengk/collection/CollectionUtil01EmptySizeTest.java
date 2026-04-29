package io.github.atengk.collection;

import io.github.atengk.utils.CollectionUtil;
import org.junit.jupiter.api.Test;

import java.util.*;


/**
 * CollectionUtil 第 1 类功能测试：判空与规模判断。
 *
 * @author Ateng
 * @since 2026-04-29
 */
class CollectionUtil01EmptySizeTest {

    @Test
    void isEmptyCollection() {
        List<String> names = new ArrayList<>();
        boolean result = CollectionUtil.isEmpty(names);
        System.out.println(result);
    }

    @Test
    void isEmptyMap() {
        Map<String, Integer> map = new LinkedHashMap<>();
        boolean result = CollectionUtil.isEmpty(map);
        System.out.println(result);
    }

    @Test
    void isEmptyArray() {
        String[] array = {};
        boolean result = CollectionUtil.isEmpty(array);
        System.out.println(result);
    }

    @Test
    void isNotEmpty() {
        List<String> names = Arrays.asList("A", "B", "C");
        boolean result = CollectionUtil.isNotEmpty(names);
        System.out.println(result);
    }

    @Test
    void sizeCollection() {
        List<String> names = Arrays.asList("A", "B", "C");
        int result = CollectionUtil.size(names);
        System.out.println(result);
    }

    @Test
    void sizeMap() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("A", 90);
        map.put("B", 95);
        int result = CollectionUtil.size(map);
        System.out.println(result);
    }

    @Test
    void sizeArray() {
        String[] array = {"A", "B", "C"};
        int result = CollectionUtil.size(array);
        System.out.println(result);
    }

    @Test
    void hasSize() {
        List<String> names = Arrays.asList("A", "B", "C");
        boolean result = CollectionUtil.hasSize(names, 3);
        System.out.println(result);
    }

    @Test
    void isSingle() {
        List<String> names = List.of("A");
        boolean result = CollectionUtil.isSingle(names);
        System.out.println(result);
    }

    @Test
    void isMulti() {
        List<String> names = Arrays.asList("A", "B", "C");
        boolean result = CollectionUtil.isMulti(names);
        System.out.println(result);
    }
}
