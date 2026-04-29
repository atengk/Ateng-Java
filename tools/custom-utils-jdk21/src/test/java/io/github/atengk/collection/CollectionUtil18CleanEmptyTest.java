package io.github.atengk.collection;

import io.github.atengk.utils.CollectionUtil;
import org.junit.jupiter.api.Test;

import java.util.*;


/**
 * CollectionUtil 第 18 类功能测试：空值清理。
 *
 * @author Ateng
 * @since 2026-04-29
 */
class CollectionUtil18CleanEmptyTest {

    @Test
    void removeNullToList() {
        List<String> result = CollectionUtil.removeNullToList(names());
        System.out.println(result);
    }

    @Test
    void removeNullToSet() {
        Set<String> result = CollectionUtil.removeNullToSet(names());
        System.out.println(result);
    }

    @Test
    void removeNullToArray() {
        String[] array = {"A", null, "B"};
        String[] result = CollectionUtil.removeNullToArray(array, String[]::new);
        System.out.println(Arrays.toString(result));
    }

    @Test
    void removeEmptyStringToList() {
        List<String> result = CollectionUtil.removeEmptyStringToList(names());
        System.out.println(result);
    }

    @Test
    void removeBlankStringToList() {
        List<String> result = CollectionUtil.removeBlankStringToList(names());
        System.out.println(result);
    }

    @Test
    void removeNullAndEmptyStringToList() {
        List<String> result = CollectionUtil.removeNullAndEmptyStringToList(names());
        System.out.println(result);
    }

    @Test
    void removeNullAndBlankStringToList() {
        List<String> result = CollectionUtil.removeNullAndBlankStringToList(names());
        System.out.println(result);
    }

    @Test
    void removeEmptyValueToList() {
        List<Object> result = CollectionUtil.removeEmptyValueToList(values());
        System.out.println(result);
    }

    @Test
    void removeEmptyValueToSet() {
        Set<Object> result = CollectionUtil.removeEmptyValueToSet(values());
        System.out.println(result);
    }

    @Test
    void removeNullAndEmptyCollectionToList() {
        List<List<String>> list = Arrays.asList(List.of("A"), Collections.emptyList(), null, List.of("B"));
        List<List<String>> result = CollectionUtil.removeNullAndEmptyCollectionToList(list);
        System.out.println(result);
    }

    @Test
    void removeNullAndEmptyMapToList() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("A", 1);
        List<Map<String, Integer>> list = Arrays.asList(map, Collections.emptyMap(), null);
        List<Map<String, Integer>> result = CollectionUtil.removeNullAndEmptyMapToList(list);
        System.out.println(result);
    }

    @Test
    void removeNullKeyToMap() {
        Map<String, Object> result = CollectionUtil.removeNullKeyToMap(valueMap());
        System.out.println(result);
    }

    @Test
    void removeNullValueToMap() {
        Map<String, Object> result = CollectionUtil.removeNullValueToMap(valueMap());
        System.out.println(result);
    }

    @Test
    void removeNullEntryToMap() {
        Map<String, Object> result = CollectionUtil.removeNullEntryToMap(valueMap());
        System.out.println(result);
    }

    @Test
    void removeEmptyValueToMap() {
        Map<String, Object> result = CollectionUtil.removeEmptyValueToMap(valueMap());
        System.out.println(result);
    }

    @Test
    void trimStringToList() {
        List<String> result = CollectionUtil.trimStringToList(names());
        System.out.println(result);
    }

    @Test
    void trimAndRemoveBlankToList() {
        List<String> result = CollectionUtil.trimAndRemoveBlankToList(names());
        System.out.println(result);
    }

    @Test
    void isEmptyValue() {
        boolean result = CollectionUtil.isEmptyValue(Optional.empty());
        System.out.println(result);
    }

    @Test
    void isNotEmptyValue() {
        boolean result = CollectionUtil.isNotEmptyValue("A");
        System.out.println(result);
    }

    @Test
    void trimAndRemoveBlankInPlace() {
        List<String> names = new ArrayList<>(names());
        boolean result = CollectionUtil.trimAndRemoveBlankInPlace(names);
        System.out.println(result + " -> " + names);
    }

    private List<String> names() {
        return Arrays.asList(" A ", null, "", " ", "B");
    }

    private List<Object> values() {
        return Arrays.asList("A", null, "", " ", Collections.emptyList(), Collections.emptyMap(), Optional.empty(), "B");
    }

    private Map<String, Object> valueMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("A", "value");
        map.put("B", null);
        map.put("C", "");
        map.put(null, "nullKey");
        return map;
    }
}
