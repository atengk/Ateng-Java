package io.github.atengk.collection;

import io.github.atengk.utils.CollectionUtil;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


/**
 * CollectionUtil 第 2 类功能测试：安全集合创建。
 *
 * @author Ateng
 * @since 2026-04-29
 */
class CollectionUtil02SafeCreateTest {

    @Test
    void emptyListIfNull() {
        List<String> names = null;
        System.out.println(CollectionUtil.emptyListIfNull(names));
    }

    @Test
    void emptySetIfNull() {
        Set<String> names = null;
        System.out.println(CollectionUtil.emptySetIfNull(names));
    }

    @Test
    void emptyMapIfNull() {
        Map<String, Integer> scoreMap = null;
        System.out.println(CollectionUtil.emptyMapIfNull(scoreMap));
    }

    @Test
    void emptyCollectionIfNull() {
        Collection<String> names = null;
        System.out.println(CollectionUtil.emptyCollectionIfNull(names));
    }

    @Test
    void newArrayList() {
        List<String> names = CollectionUtil.newArrayList();
        System.out.println(names);
    }

    @Test
    void newArrayListWithInitialCapacity() {
        List<String> names = CollectionUtil.newArrayList(3);
        names.add("A");
        names.add("B");
        System.out.println(names);
    }

    @Test
    void newArrayListWithCollection() {
        List<String> names = CollectionUtil.newArrayList(List.of("A", "B", "C"));
        System.out.println(names);
    }

    @Test
    void newArrayListWithElements() {
        List<String> names = CollectionUtil.newArrayList("A", "B", "C");
        System.out.println(names);
    }

    @Test
    void newLinkedList() {
        List<String> names = CollectionUtil.newLinkedList();
        names.add("A");
        names.add("B");
        System.out.println(names);
    }

    @Test
    void newLinkedListWithCollection() {
        List<String> names = CollectionUtil.newLinkedList(List.of("A", "B", "C"));
        System.out.println(names);
    }

    @Test
    void newLinkedListWithElements() {
        List<String> names = CollectionUtil.newLinkedList("A", "B", "C");
        System.out.println(names);
    }

    @Test
    void newHashSet() {
        Set<String> names = CollectionUtil.newHashSet();
        names.add("A");
        names.add("B");
        System.out.println(names);
    }

    @Test
    void newHashSetWithInitialCapacity() {
        Set<String> names = CollectionUtil.newHashSet(3);
        names.add("A");
        names.add("B");
        System.out.println(names);
    }

    @Test
    void newHashSetWithCollection() {
        Set<String> names = CollectionUtil.newHashSet(List.of("A", "B", "A"));
        System.out.println(names);
    }

    @Test
    void newHashSetWithElements() {
        Set<String> names = CollectionUtil.newHashSet("A", "B", "A");
        System.out.println(names);
    }

    @Test
    void newLinkedHashSet() {
        Set<String> names = CollectionUtil.newLinkedHashSet();
        names.add("B");
        names.add("A");
        System.out.println(names);
    }

    @Test
    void newLinkedHashSetWithInitialCapacity() {
        Set<String> names = CollectionUtil.newLinkedHashSet(3);
        names.add("B");
        names.add("A");
        System.out.println(names);
    }

    @Test
    void newLinkedHashSetWithCollection() {
        Set<String> names = CollectionUtil.newLinkedHashSet(List.of("B", "A", "B"));
        System.out.println(names);
    }

    @Test
    void newLinkedHashSetWithElements() {
        Set<String> names = CollectionUtil.newLinkedHashSet("B", "A", "B");
        System.out.println(names);
    }

    @Test
    void newTreeSet() {
        SortedSet<String> names = CollectionUtil.newTreeSet();
        names.add("C");
        names.add("A");
        names.add("B");
        System.out.println(names);
    }

    @Test
    void newTreeSetWithComparator() {
        SortedSet<String> names = CollectionUtil.newTreeSet(Comparator.reverseOrder());
        names.add("A");
        names.add("B");
        names.add("C");
        System.out.println(names);
    }

    @Test
    void newTreeSetWithCollection() {
        SortedSet<String> names = CollectionUtil.newTreeSet(List.of("C", "A", "B"));
        System.out.println(names);
    }

    @Test
    void newTreeSetWithComparatorAndCollection() {
        SortedSet<String> names = CollectionUtil.newTreeSet(Comparator.reverseOrder(), List.of("A", "B", "C"));
        System.out.println(names);
    }

    @Test
    void newHashMap() {
        Map<String, Integer> scoreMap = CollectionUtil.newHashMap();
        scoreMap.put("A", 90);
        scoreMap.put("B", 80);
        System.out.println(scoreMap);
    }

    @Test
    void newHashMapWithInitialCapacity() {
        Map<String, Integer> scoreMap = CollectionUtil.newHashMap(4);
        scoreMap.put("A", 90);
        scoreMap.put("B", 80);
        System.out.println(scoreMap);
    }

    @Test
    void newHashMapWithMap() {
        Map<String, Integer> sourceMap = Map.of("A", 90, "B", 80);
        Map<String, Integer> scoreMap = CollectionUtil.newHashMap(sourceMap);
        System.out.println(scoreMap);
    }

    @Test
    void newLinkedHashMap() {
        Map<String, Integer> scoreMap = CollectionUtil.newLinkedHashMap();
        scoreMap.put("B", 80);
        scoreMap.put("A", 90);
        System.out.println(scoreMap);
    }

    @Test
    void newLinkedHashMapWithInitialCapacity() {
        Map<String, Integer> scoreMap = CollectionUtil.newLinkedHashMap(4);
        scoreMap.put("B", 80);
        scoreMap.put("A", 90);
        System.out.println(scoreMap);
    }

    @Test
    void newLinkedHashMapWithMap() {
        Map<String, Integer> sourceMap = new LinkedHashMap<>();
        sourceMap.put("B", 80);
        sourceMap.put("A", 90);
        Map<String, Integer> scoreMap = CollectionUtil.newLinkedHashMap(sourceMap);
        System.out.println(scoreMap);
    }

    @Test
    void newConcurrentHashMap() {
        ConcurrentHashMap<String, Integer> scoreMap = CollectionUtil.newConcurrentHashMap();
        scoreMap.put("A", 90);
        scoreMap.put("B", 80);
        System.out.println(scoreMap);
    }

    @Test
    void newConcurrentHashMapWithInitialCapacity() {
        ConcurrentHashMap<String, Integer> scoreMap = CollectionUtil.newConcurrentHashMap(4);
        scoreMap.put("A", 90);
        scoreMap.put("B", 80);
        System.out.println(scoreMap);
    }

    @Test
    void newConcurrentHashMapWithMap() {
        Map<String, Integer> sourceMap = Map.of("A", 90, "B", 80);
        ConcurrentHashMap<String, Integer> scoreMap = CollectionUtil.newConcurrentHashMap(sourceMap);
        System.out.println(scoreMap);
    }

    @Test
    void immutableListWithElements() {
        List<String> names = CollectionUtil.immutableList("A", "B", "C");
        System.out.println(names);
    }

    @Test
    void immutableListWithCollection() {
        List<String> names = CollectionUtil.immutableList(List.of("A", "B", "C"));
        System.out.println(names);
    }

    @Test
    void immutableSetWithElements() {
        Set<String> names = CollectionUtil.immutableSet("A", "B", "C");
        System.out.println(names);
    }

    @Test
    void immutableSetWithCollection() {
        Set<String> names = CollectionUtil.immutableSet(List.of("A", "B", "C"));
        System.out.println(names);
    }

    @Test
    void immutableMap() {
        Map<String, Integer> scoreMap = CollectionUtil.immutableMap(Map.of("A", 90, "B", 80));
        System.out.println(scoreMap);
    }

    @Test
    void mutableListWithElements() {
        List<String> names = CollectionUtil.mutableList("A", null, "B");
        System.out.println(names);
    }

    @Test
    void mutableListWithCollection() {
        List<String> names = CollectionUtil.mutableList(Arrays.asList("A", null, "B"));
        System.out.println(names);
    }

    @Test
    void mutableSetWithElements() {
        Set<String> names = CollectionUtil.mutableSet("A", null, "B", "A");
        System.out.println(names);
    }

    @Test
    void mutableSetWithCollection() {
        Set<String> names = CollectionUtil.mutableSet(Arrays.asList("A", null, "B", "A"));
        System.out.println(names);
    }

    @Test
    void mutableMap() {
        Map<String, Integer> sourceMap = new HashMap<>();
        sourceMap.put("A", 90);
        sourceMap.put(null, 0);
        Map<String, Integer> scoreMap = CollectionUtil.mutableMap(sourceMap);
        System.out.println(scoreMap);
    }

    @Test
    void orderedList() {
        List<String> names = CollectionUtil.orderedList("B", "A", "C");
        System.out.println(names);
    }

    @Test
    void orderedSetWithElements() {
        Set<String> names = CollectionUtil.orderedSet("B", "A", "B", "C");
        System.out.println(names);
    }

    @Test
    void orderedSetWithCollection() {
        Set<String> names = CollectionUtil.orderedSet(List.of("B", "A", "B", "C"));
        System.out.println(names);
    }

    @Test
    void orderedMap() {
        Map<String, Integer> sourceMap = new LinkedHashMap<>();
        sourceMap.put("B", 80);
        sourceMap.put("A", 90);
        Map<String, Integer> scoreMap = CollectionUtil.orderedMap(sourceMap);
        System.out.println(scoreMap);
    }
}