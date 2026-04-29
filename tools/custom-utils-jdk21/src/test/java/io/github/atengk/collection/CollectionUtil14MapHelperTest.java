package io.github.atengk.collection;

import io.github.atengk.utils.CollectionUtil;
import org.junit.jupiter.api.Test;

import java.util.*;


/**
 * CollectionUtil 第 14 类功能测试：Map 辅助。
 *
 * @author Ateng
 * @since 2026-04-29
 */
class CollectionUtil14MapHelperTest {

    @Test
    void get() {
        User result = CollectionUtil.get(userMap(), 1L);
        System.out.println(result);
    }

    @Test
    void getOrDefault() {
        Integer result = CollectionUtil.getOrDefault(scoreMap(), "X", 0);
        System.out.println(result);
    }

    @Test
    void getOptional() {
        Optional<User> result = CollectionUtil.getOptional(userMap(), 2L);
        System.out.println(result);
    }

    @Test
    void getFirstNotNull() {
        Integer result = CollectionUtil.getFirstNotNull(scoreMap(), "C", "B");
        System.out.println(result);
    }

    @Test
    void getValues() {
        List<User> result = CollectionUtil.getValues(userMap(), Arrays.asList(1L, 3L));
        System.out.println(result);
    }

    @Test
    void containsKey() {
        boolean result = CollectionUtil.containsKey(userMap(), 1L);
        System.out.println(result);
    }

    @Test
    void containsAnyKey() {
        boolean result = CollectionUtil.containsAnyKey(userMap(), Arrays.asList(9L, 1L));
        System.out.println(result);
    }

    @Test
    void subMap() {
        Map<Long, User> result = CollectionUtil.subMap(userMap(), Arrays.asList(1L, 3L));
        System.out.println(result);
    }

    @Test
    void excludeKeys() {
        Map<Long, User> result = CollectionUtil.excludeKeys(userMap(), List.of(2L));
        System.out.println(result);
    }

    @Test
    void filterMapByKey() {
        Map<Long, User> result = CollectionUtil.filterMapByKey(userMap(), key -> key > 2L);
        System.out.println(result);
    }

    @Test
    void filterMapByValue() {
        Map<Long, User> result = CollectionUtil.filterMapByValue(userMap(), user -> "杭州".equals(user.city()));
        System.out.println(result);
    }

    @Test
    void findFirstEntry() {
        Map.Entry<Long, User> result = CollectionUtil.findFirstEntry(userMap(), entry -> entry.getKey() == 2L);
        System.out.println(result);
    }

    @Test
    void keySet() {
        Set<Long> result = CollectionUtil.keySet(userMap());
        System.out.println(result);
    }

    @Test
    void valueList() {
        List<User> result = CollectionUtil.valueList(userMap());
        System.out.println(result);
    }

    @Test
    void entryList() {
        List<Map.Entry<Long, User>> result = CollectionUtil.entryList(userMap());
        System.out.println(result);
    }

    @Test
    void invertMap() {
        Map<Integer, String> result = CollectionUtil.invertMap(scoreMap());
        System.out.println(result);
    }

    @Test
    void invertMapToList() {
        Map<Integer, List<String>> result = CollectionUtil.invertMapToList(scoreMap());
        System.out.println(result);
    }

    @Test
    void sortMapByKeyDesc() {
        Map<Long, User> result = CollectionUtil.sortMapByKeyDesc(userMap());
        System.out.println(result);
    }

    @Test
    void sortMapByValue() {
        Map<String, Integer> result = CollectionUtil.sortMapByValue(scoreMap(), Comparator.naturalOrder());
        System.out.println(result);
    }

    @Test
    void pageMap() {
        Map<Long, User> result = CollectionUtil.pageMap(userMap(), 1, 2);
        System.out.println(result);
    }

    @Test
    void removeNullValues() {
        Map<String, Integer> map = new LinkedHashMap<>(scoreMap());
        map.put("X", null);
        boolean result = CollectionUtil.removeNullValues(map);
        System.out.println(result + " -> " + map);
    }

    private Map<Long, User> userMap() {
        Map<Long, User> map = new LinkedHashMap<>();
        map.put(1L, new User(1L, "张三", "杭州"));
        map.put(2L, new User(2L, "李四", "上海"));
        map.put(3L, new User(3L, "王五", "杭州"));
        return map;
    }

    private Map<String, Integer> scoreMap() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("A", 90);
        map.put("B", 95);
        map.put("C", 95);
        return map;
    }

    record User(Long id, String name, String city) {
    }
}
