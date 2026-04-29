package io.github.atengk.collection;

import io.github.atengk.utils.CollectionUtil;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/**
 * CollectionUtil 第 17 类功能测试：字符串拼接。
 *
 * @author Ateng
 * @since 2026-04-29
 */
class CollectionUtil17JoinTest {

    @Test
    void joinCollection() {
        String result = CollectionUtil.join(names(), ",");
        System.out.println(result);
    }

    @Test
    void joinArray() {
        String[] array = {"A", "B", "C"};
        String result = CollectionUtil.join(array, ",");
        System.out.println(result);
    }

    @Test
    void joinWithPrefixSuffix() {
        String result = CollectionUtil.join(names(), ",", "[", "]");
        System.out.println(result);
    }

    @Test
    void joinNotNull() {
        String result = CollectionUtil.joinNotNull(namesWithNull(), ",");
        System.out.println(result);
    }

    @Test
    void joinNotBlank() {
        String result = CollectionUtil.joinNotBlank(Arrays.asList("A", " ", "B", ""), ",");
        System.out.println(result);
    }

    @Test
    void joinBy() {
        String result = CollectionUtil.joinBy(users(), ",", User::name);
        System.out.println(result);
    }

    @Test
    void joinByNotNull() {
        String result = CollectionUtil.joinByNotNull(users(), ",", User::city);
        System.out.println(result);
    }

    @Test
    void joinByNotBlank() {
        String result = CollectionUtil.joinByNotBlank(users(), ",", User::name);
        System.out.println(result);
    }

    @Test
    void joinByWithPrefixSuffix() {
        String result = CollectionUtil.joinBy(users(), ",", "[", "]", User::name);
        System.out.println(result);
    }

    @Test
    void joinWrapped() {
        String result = CollectionUtil.joinWrapped(names(), ",", "'", "'");
        System.out.println(result);
    }

    @Test
    void joinByWrapped() {
        String result = CollectionUtil.joinByWrapped(users(), ",", "(", ")", User::id);
        System.out.println(result);
    }

    @Test
    void joinSqlIn() {
        String result = CollectionUtil.joinSqlIn(names());
        System.out.println(result);
    }

    @Test
    void joinSqlInBy() {
        String result = CollectionUtil.joinSqlInBy(users(), User::name);
        System.out.println(result);
    }

    @Test
    void joinMap() {
        String result = CollectionUtil.joinMap(scoreMap(), "&", "=");
        System.out.println(result);
    }

    @Test
    void joinMapNotNull() {
        Map<String, Integer> map = new LinkedHashMap<>(scoreMap());
        map.put("X", null);
        String result = CollectionUtil.joinMapNotNull(map, "&", "=");
        System.out.println(result);
    }

    @Test
    void joinMapBy() {
        String result = CollectionUtil.joinMapBy(scoreMap(), ",", (key, value) -> key + ":" + value);
        System.out.println(result);
    }

    @Test
    void commaJoin() {
        String result = CollectionUtil.commaJoin(names());
        System.out.println(result);
    }

    @Test
    void commaJoinNotNull() {
        String result = CollectionUtil.commaJoinNotNull(namesWithNull());
        System.out.println(result);
    }

    @Test
    void commaJoinNotBlank() {
        String result = CollectionUtil.commaJoinNotBlank(Arrays.asList("A", " ", "B", ""));
        System.out.println(result);
    }

    private List<String> names() {
        return Arrays.asList("A", "B", "C");
    }

    private List<String> namesWithNull() {
        return Arrays.asList("A", null, "B", "C");
    }

    private List<User> users() {
        return Arrays.asList(
                new User(1L, "张三", "杭州"),
                new User(2L, "李四", "上海"),
                new User(3L, "王五", "杭州")
        );
    }

    private Map<String, Integer> scoreMap() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("A", 90);
        map.put("B", 95);
        return map;
    }

    record User(Long id, String name, String city) {
    }
}
