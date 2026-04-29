package io.github.atengk.collection;

import io.github.atengk.utils.CollectionUtil;
import org.junit.jupiter.api.Test;

import java.util.*;


/**
 * CollectionUtil 第 7 类功能测试：映射转换。
 *
 * @author Ateng
 * @since 2026-04-29
 */
class CollectionUtil07MapConvertTest {

    @Test
    void map() {
        List<String> result = CollectionUtil.map(users(), User::name);
        System.out.println(result);
    }

    @Test
    void mapNotNull() {
        List<String> result = CollectionUtil.mapNotNull(usersWithNull(), User::name);
        System.out.println(result);
    }

    @Test
    void mapIndexed() {
        List<String> result = CollectionUtil.mapIndexed(users(), (user, index) -> index + ":" + user.name());
        System.out.println(result);
    }

    @Test
    void mapToSet() {
        Set<String> result = CollectionUtil.mapToSet(users(), User::city);
        System.out.println(result);
    }

    @Test
    void mapTo() {
        Set<String> result = CollectionUtil.mapTo(users(), User::name, LinkedHashSet::new);
        System.out.println(result);
    }

    @Test
    void flatMap() {
        List<String> result = CollectionUtil.flatMap(users(), user -> Arrays.asList(user.name().split("")));
        System.out.println(result);
    }

    @Test
    void extractToList() {
        List<Long> result = CollectionUtil.extractToList(users(), User::id);
        System.out.println(result);
    }

    @Test
    void extractNotNullToList() {
        List<String> result = CollectionUtil.extractNotNullToList(usersWithNull(), User::name);
        System.out.println(result);
    }

    @Test
    void convertToList() {
        List<String> result = CollectionUtil.convertToList(users(), User::name);
        System.out.println(result);
    }

    @Test
    void mapValues() {
        Map<Long, User> userMap = userMap();
        Map<Long, String> result = CollectionUtil.mapValues(userMap, User::name);
        System.out.println(result);
    }

    @Test
    void mapKeys() {
        Map<Long, User> userMap = userMap();
        Map<String, User> result = CollectionUtil.mapKeys(userMap, String::valueOf);
        System.out.println(result);
    }

    @Test
    void mapEntryToList() {
        Map<Long, User> userMap = userMap();
        List<String> result = CollectionUtil.mapEntryToList(userMap, (id, user) -> id + "-" + user.name());
        System.out.println(result);
    }

    private Map<Long, User> userMap() {
        Map<Long, User> map = new LinkedHashMap<>();
        for (User user : users()) {
            map.put(user.id(), user);
        }
        return map;
    }

    private List<User> users() {
        return Arrays.asList(
                new User(1L, "张三", 18, "杭州"),
                new User(2L, "李四", 25, "上海"),
                new User(3L, "王五", 30, "杭州")
        );
    }

    private List<User> usersWithNull() {
        return Arrays.asList(
                new User(1L, "张三", 18, "杭州"),
                new User(2L, null, 25, "上海"),
                new User(3L, "王五", 30, "杭州")
        );
    }

    record User(Long id, String name, int age, String city) {
    }
}
