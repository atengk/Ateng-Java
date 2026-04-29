package io.github.atengk.collection;

import io.github.atengk.utils.CollectionUtil;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.function.BinaryOperator;


/**
 * CollectionUtil 第 5 类功能测试：添加与合并。
 *
 * @author Ateng
 * @since 2026-04-29
 */
class CollectionUtil05AddMergeTest {

    @Test
    void add() {
        List<String> names = new ArrayList<>(List.of("A"));
        boolean result = CollectionUtil.add(names, "B");
        System.out.println(result + " -> " + names);
    }

    @Test
    void addIfNotNull() {
        List<String> names = new ArrayList<>(List.of("A"));
        boolean result = CollectionUtil.addIfNotNull(names, null);
        System.out.println(result + " -> " + names);
    }

    @Test
    void addIfAbsent() {
        List<String> names = new ArrayList<>(List.of("A"));
        boolean result = CollectionUtil.addIfAbsent(names, "B");
        System.out.println(result + " -> " + names);
    }

    @Test
    void addIfNotNullAndAbsent() {
        List<String> names = new ArrayList<>(List.of("A"));
        boolean result = CollectionUtil.addIfNotNullAndAbsent(names, "B");
        System.out.println(result + " -> " + names);
    }

    @Test
    void addFirst() {
        List<String> names = new ArrayList<>(List.of("B", "C"));
        boolean result = CollectionUtil.addFirst(names, "A");
        System.out.println(result + " -> " + names);
    }

    @Test
    void addLast() {
        List<String> names = new ArrayList<>(List.of("A", "B"));
        boolean result = CollectionUtil.addLast(names, "C");
        System.out.println(result + " -> " + names);
    }

    @Test
    void addAt() {
        List<String> names = new ArrayList<>(List.of("A", "C"));
        boolean result = CollectionUtil.addAt(names, 1, "B");
        System.out.println(result + " -> " + names);
    }

    @Test
    void addAllWithElements() {
        List<String> names = new ArrayList<>(List.of("A"));
        boolean result = CollectionUtil.addAll(names, "B", "C");
        System.out.println(result + " -> " + names);
    }

    @Test
    void addAllWithCollection() {
        List<String> names = new ArrayList<>(List.of("A"));
        boolean result = CollectionUtil.addAll(names, List.of("B", "C"));
        System.out.println(result + " -> " + names);
    }

    @Test
    void addAllWithIterable() {
        List<String> names = new ArrayList<>(List.of("A"));
        Iterable<String> iterable = () -> List.of("B", "C").iterator();
        boolean result = CollectionUtil.addAll(names, iterable);
        System.out.println(result + " -> " + names);
    }

    @Test
    void addAllWithIterator() {
        List<String> names = new ArrayList<>(List.of("A"));
        Iterator<String> iterator = List.of("B", "C").iterator();
        boolean result = CollectionUtil.addAll(names, iterator);
        System.out.println(result + " -> " + names);
    }

    @Test
    void addAllIfNotNullWithElements() {
        List<String> names = new ArrayList<>(List.of("A"));
        boolean result = CollectionUtil.addAllIfNotNull(names, "B", null, "C");
        System.out.println(result + " -> " + names);
    }

    @Test
    void addAllIfNotNullWithCollection() {
        List<String> names = new ArrayList<>(List.of("A"));
        boolean result = CollectionUtil.addAllIfNotNull(names, Arrays.asList("B", null, "C"));
        System.out.println(result + " -> " + names);
    }

    @Test
    void addAllIfAbsentWithElements() {
        List<String> names = new ArrayList<>(List.of("A", "B"));
        boolean result = CollectionUtil.addAllIfAbsent(names, "B", "C", "D");
        System.out.println(result + " -> " + names);
    }

    @Test
    void addAllIfAbsentWithCollection() {
        List<String> names = new ArrayList<>(List.of("A", "B"));
        boolean result = CollectionUtil.addAllIfAbsent(names, List.of("B", "C", "D"));
        System.out.println(result + " -> " + names);
    }

    @Test
    void addAllIfNotNullAndAbsentWithElements() {
        List<String> names = new ArrayList<>(List.of("A", "B"));
        boolean result = CollectionUtil.addAllIfNotNullAndAbsent(names, "B", null, "C", "D");
        System.out.println(result + " -> " + names);
    }

    @Test
    void addAllIfNotNullAndAbsentWithCollection() {
        List<String> names = new ArrayList<>(List.of("A", "B"));
        boolean result = CollectionUtil.addAllIfNotNullAndAbsent(names, Arrays.asList("B", null, "C", "D"));
        System.out.println(result + " -> " + names);
    }

    @Test
    void put() {
        Map<String, Integer> scoreMap = new LinkedHashMap<>();
        boolean result = CollectionUtil.put(scoreMap, "A", 90);
        System.out.println(result + " -> " + scoreMap);
    }

    @Test
    void putIfKeyNotNull() {
        Map<String, Integer> scoreMap = new LinkedHashMap<>();
        boolean result = CollectionUtil.putIfKeyNotNull(scoreMap, null, 90);
        System.out.println(result + " -> " + scoreMap);
    }

    @Test
    void putIfValueNotNull() {
        Map<String, Integer> scoreMap = new LinkedHashMap<>();
        boolean result = CollectionUtil.putIfValueNotNull(scoreMap, "A", null);
        System.out.println(result + " -> " + scoreMap);
    }

    @Test
    void putIfNotNull() {
        Map<String, Integer> scoreMap = new LinkedHashMap<>();
        boolean result = CollectionUtil.putIfNotNull(scoreMap, "A", 90);
        System.out.println(result + " -> " + scoreMap);
    }

    @Test
    void putIfAbsent() {
        Map<String, Integer> scoreMap = new LinkedHashMap<>();
        scoreMap.put("A", 90);
        boolean result = CollectionUtil.putIfAbsent(scoreMap, "A", 100);
        System.out.println(result + " -> " + scoreMap);
    }

    @Test
    void putAll() {
        Map<String, Integer> scoreMap = new LinkedHashMap<>();
        boolean result = CollectionUtil.putAll(scoreMap, Map.of("A", 90, "B", 80));
        System.out.println(result + " -> " + scoreMap);
    }

    @Test
    void putAllIfKeyNotNull() {
        Map<String, Integer> scoreMap = new LinkedHashMap<>();
        Map<String, Integer> sourceMap = new LinkedHashMap<>();
        sourceMap.put("A", 90);
        sourceMap.put(null, 0);
        boolean result = CollectionUtil.putAllIfKeyNotNull(scoreMap, sourceMap);
        System.out.println(result + " -> " + scoreMap);
    }

    @Test
    void putAllIfValueNotNull() {
        Map<String, Integer> scoreMap = new LinkedHashMap<>();
        Map<String, Integer> sourceMap = new LinkedHashMap<>();
        sourceMap.put("A", 90);
        sourceMap.put("B", null);
        boolean result = CollectionUtil.putAllIfValueNotNull(scoreMap, sourceMap);
        System.out.println(result + " -> " + scoreMap);
    }

    @Test
    void putAllIfNotNull() {
        Map<String, Integer> scoreMap = new LinkedHashMap<>();
        Map<String, Integer> sourceMap = new LinkedHashMap<>();
        sourceMap.put("A", 90);
        sourceMap.put(null, 0);
        sourceMap.put("B", null);
        boolean result = CollectionUtil.putAllIfNotNull(scoreMap, sourceMap);
        System.out.println(result + " -> " + scoreMap);
    }

    @Test
    void putAllIfAbsent() {
        Map<String, Integer> scoreMap = new LinkedHashMap<>();
        scoreMap.put("A", 90);
        boolean result = CollectionUtil.putAllIfAbsent(scoreMap, Map.of("A", 100, "B", 80));
        System.out.println(result + " -> " + scoreMap);
    }

    @Test
    void mergeToList() {
        List<String> result = CollectionUtil.mergeToList(List.of("A", "B"), List.of("C", "D"));
        System.out.println(result);
    }

    @Test
    void mergeToListIfNotNull() {
        List<String> result = CollectionUtil.mergeToListIfNotNull(Arrays.asList("A", null), List.of("B", "C"));
        System.out.println(result);
    }

    @Test
    void mergeToSet() {
        Set<String> result = CollectionUtil.mergeToSet(List.of("A", "B"), List.of("B", "C"));
        System.out.println(result);
    }

    @Test
    void mergeToLinkedHashSet() {
        Set<String> result = CollectionUtil.mergeToLinkedHashSet(List.of("B", "A"), List.of("B", "C"));
        System.out.println(result);
    }

    @Test
    void mergeToLinkedHashSetIfNotNull() {
        Set<String> result = CollectionUtil.mergeToLinkedHashSetIfNotNull(Arrays.asList("B", null, "A"), List.of("B", "C"));
        System.out.println(result);
    }

    @Test
    void mergeToMap() {
        Map<String, Integer> result = CollectionUtil.mergeToMap(Map.of("A", 90), Map.of("B", 80));
        System.out.println(result);
    }

    @Test
    void mergeToLinkedHashMap() {
        Map<String, Integer> firstMap = new LinkedHashMap<>();
        firstMap.put("A", 90);
        Map<String, Integer> secondMap = new LinkedHashMap<>();
        secondMap.put("B", 80);
        Map<String, Integer> result = CollectionUtil.mergeToLinkedHashMap(firstMap, secondMap);
        System.out.println(result);
    }

    @Test
    void mergeToLinkedHashMapIfNotNull() {
        Map<String, Integer> firstMap = new LinkedHashMap<>();
        firstMap.put("A", 90);
        firstMap.put(null, 0);
        Map<String, Integer> secondMap = new LinkedHashMap<>();
        secondMap.put("B", 80);
        secondMap.put("C", null);
        Map<String, Integer> result = CollectionUtil.mergeToLinkedHashMapIfNotNull(firstMap, secondMap);
        System.out.println(result);
    }

    @Test
    void mergeWithTwoCollectionsAndSupplier() {
        List<String> result = CollectionUtil.merge(List.of("A", "B"), List.of("C", "D"), ArrayList::new);
        System.out.println(result);
    }

    @Test
    void mergeWithSupplierAndCollections() {
        LinkedHashSet<String> result = CollectionUtil.merge(LinkedHashSet::new, List.of("B", "A"), List.of("B", "C"));
        System.out.println(result);
    }

    @Test
    void mergeWithTwoMapsAndSupplier() {
        LinkedHashMap<String, Integer> result = CollectionUtil.merge(Map.of("A", 90), Map.of("B", 80), LinkedHashMap::new);
        System.out.println(result);
    }

    @Test
    void mergeWithSupplierAndMaps() {
        LinkedHashMap<String, Integer> result = CollectionUtil.merge(LinkedHashMap::new, Map.of("A", 90), Map.of("B", 80));
        System.out.println(result);
    }

    @Test
    void mergeMapWithTwoMaps() {
        Map<String, Integer> firstMap = Map.of("A", 90, "B", 80);
        Map<String, Integer> secondMap = Map.of("B", 10, "C", 70);
        Map<String, Integer> result = CollectionUtil.mergeMap(firstMap, secondMap, Integer::sum);
        System.out.println(result);
    }

    @Test
    void mergeMapWithSupplier() {
        Map<String, Integer> firstMap = Map.of("A", 90, "B", 80);
        Map<String, Integer> secondMap = Map.of("B", 10, "C", 70);
        BinaryOperator<Integer> mergeFunction = Integer::sum;
        LinkedHashMap<String, Integer> result = CollectionUtil.mergeMap(LinkedHashMap::new, mergeFunction, firstMap, secondMap);
        System.out.println(result);
    }
}