package io.github.atengk.collection;

import io.github.atengk.utils.CollectionUtil;
import org.junit.jupiter.api.Test;

import java.util.*;


/**
 * CollectionUtil 第 4 类功能测试：元素获取。
 *
 * @author Ateng
 * @since 2026-04-29
 */
class CollectionUtil04ElementGetTest {

    @Test
    void firstWithList() {
        List<String> names = List.of("A", "B", "C");
        System.out.println(CollectionUtil.first(names));
    }

    @Test
    void firstWithCollection() {
        Collection<String> names = new LinkedHashSet<>(List.of("A", "B", "C"));
        System.out.println(CollectionUtil.first(names));
    }

    @Test
    void firstWithIterable() {
        Iterable<String> names = () -> List.of("A", "B", "C").iterator();
        System.out.println(CollectionUtil.first(names));
    }

    @Test
    void firstWithIterator() {
        Iterator<String> iterator = List.of("A", "B", "C").iterator();
        System.out.println(CollectionUtil.first(iterator));
    }

    @Test
    void firstWithArray() {
        String[] names = {"A", "B", "C"};
        System.out.println(CollectionUtil.first(names));
    }

    @Test
    void firstFromArray() {
        int[] numbers = {1, 2, 3};
        System.out.println(CollectionUtil.firstFromArray(numbers));
    }

    @Test
    void lastWithList() {
        List<String> names = List.of("A", "B", "C");
        System.out.println(CollectionUtil.last(names));
    }

    @Test
    void lastWithCollection() {
        Collection<String> names = new LinkedHashSet<>(List.of("A", "B", "C"));
        System.out.println(CollectionUtil.last(names));
    }

    @Test
    void lastWithIterable() {
        Iterable<String> names = () -> List.of("A", "B", "C").iterator();
        System.out.println(CollectionUtil.last(names));
    }

    @Test
    void lastWithIterator() {
        Iterator<String> iterator = List.of("A", "B", "C").iterator();
        System.out.println(CollectionUtil.last(iterator));
    }

    @Test
    void lastWithArray() {
        String[] names = {"A", "B", "C"};
        System.out.println(CollectionUtil.last(names));
    }

    @Test
    void lastFromArray() {
        int[] numbers = {1, 2, 3};
        System.out.println(CollectionUtil.lastFromArray(numbers));
    }

    @Test
    void getWithList() {
        List<String> names = List.of("A", "B", "C");
        System.out.println(CollectionUtil.get(names, 1));
    }

    @Test
    void getWithIterable() {
        Iterable<String> names = () -> List.of("A", "B", "C").iterator();
        System.out.println(CollectionUtil.get(names, 1));
    }

    @Test
    void getWithIterator() {
        Iterator<String> iterator = List.of("A", "B", "C").iterator();
        System.out.println(CollectionUtil.get(iterator, 1));
    }

    @Test
    void getWithArray() {
        String[] names = {"A", "B", "C"};
        System.out.println(CollectionUtil.get(names, 1));
    }

    @Test
    void getFromArray() {
        int[] numbers = {1, 2, 3};
        System.out.println(CollectionUtil.getFromArray(numbers, 1));
    }

    @Test
    void getOrDefaultWithList() {
        List<String> names = List.of("A", "B", "C");
        System.out.println(CollectionUtil.getOrDefault(names, 5, "默认值"));
    }

    @Test
    void getOrDefaultWithIterable() {
        Iterable<String> names = () -> List.of("A", "B", "C").iterator();
        System.out.println(CollectionUtil.getOrDefault(names, 5, "默认值"));
    }

    @Test
    void getOrDefaultWithArray() {
        String[] names = {"A", "B", "C"};
        System.out.println(CollectionUtil.getOrDefault(names, 5, "默认值"));
    }

    @Test
    void randomWithList() {
        List<String> names = List.of("A", "B", "C");
        System.out.println(CollectionUtil.random(names));
    }

    @Test
    void randomWithCollection() {
        Collection<String> names = new LinkedHashSet<>(List.of("A", "B", "C"));
        System.out.println(CollectionUtil.random(names));
    }

    @Test
    void randomWithArray() {
        String[] names = {"A", "B", "C"};
        System.out.println(CollectionUtil.random(names));
    }

    @Test
    void randomFromArray() {
        int[] numbers = {1, 2, 3};
        System.out.println(CollectionUtil.randomFromArray(numbers));
    }

    @Test
    void firstKey() {
        Map<String, Integer> scoreMap = scoreMap();
        System.out.println(CollectionUtil.firstKey(scoreMap));
    }

    @Test
    void firstValue() {
        Map<String, Integer> scoreMap = scoreMap();
        System.out.println(CollectionUtil.firstValue(scoreMap));
    }

    @Test
    void firstEntry() {
        Map<String, Integer> scoreMap = scoreMap();
        System.out.println(CollectionUtil.firstEntry(scoreMap));
    }

    @Test
    void lastKey() {
        Map<String, Integer> scoreMap = scoreMap();
        System.out.println(CollectionUtil.lastKey(scoreMap));
    }

    @Test
    void lastValue() {
        Map<String, Integer> scoreMap = scoreMap();
        System.out.println(CollectionUtil.lastValue(scoreMap));
    }

    @Test
    void lastEntry() {
        Map<String, Integer> scoreMap = scoreMap();
        System.out.println(CollectionUtil.lastEntry(scoreMap));
    }

    @Test
    void randomKey() {
        Map<String, Integer> scoreMap = scoreMap();
        System.out.println(CollectionUtil.randomKey(scoreMap));
    }

    @Test
    void randomValue() {
        Map<String, Integer> scoreMap = scoreMap();
        System.out.println(CollectionUtil.randomValue(scoreMap));
    }

    @Test
    void randomEntry() {
        Map<String, Integer> scoreMap = scoreMap();
        System.out.println(CollectionUtil.randomEntry(scoreMap));
    }

    @Test
    void firstOptional() {
        List<String> names = List.of("A", "B", "C");
        System.out.println(CollectionUtil.firstOptional(names));
    }

    @Test
    void lastOptional() {
        List<String> names = List.of("A", "B", "C");
        System.out.println(CollectionUtil.lastOptional(names));
    }

    @Test
    void getOptional() {
        List<String> names = List.of("A", "B", "C");
        System.out.println(CollectionUtil.getOptional(names, 1));
    }

    @Test
    void randomOptional() {
        List<String> names = List.of("A", "B", "C");
        System.out.println(CollectionUtil.randomOptional(names));
    }

    private Map<String, Integer> scoreMap() {
        Map<String, Integer> scoreMap = new LinkedHashMap<>();
        scoreMap.put("A", 90);
        scoreMap.put("B", 80);
        scoreMap.put("C", 70);
        return scoreMap;
    }
}