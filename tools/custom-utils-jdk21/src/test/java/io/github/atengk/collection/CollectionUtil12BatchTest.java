package io.github.atengk.collection;

import io.github.atengk.utils.CollectionUtil;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;


/**
 * CollectionUtil 第 12 类功能测试：分批处理。
 *
 * @author Ateng
 * @since 2026-04-29
 */
class CollectionUtil12BatchTest {

    @Test
    void batch() {
        List<List<User>> result = CollectionUtil.batch(users(), 2);
        System.out.println(result);
    }

    @Test
    void split() {
        List<List<User>> result = CollectionUtil.split(users(), 2);
        System.out.println(result);
    }

    @Test
    void splitArray() {
        String[] names = {"A", "B", "C", "D", "E"};
        List<List<String>> result = CollectionUtil.split(names, 2);
        System.out.println(result);
    }

    @Test
    void splitToSet() {
        String[] names = {"A", "B", "A", "C", "D"};
        List<Set<String>> result = CollectionUtil.splitToSet(names, 2);
        System.out.println(result);
    }

    @Test
    void splitTo() {
        List<LinkedHashSet<User>> result = CollectionUtil.splitTo(users(), 2, LinkedHashSet::new);
        System.out.println(result);
    }

    @Test
    void forEachBatch() {
        CollectionUtil.forEachBatch(users(), 2, batch -> System.out.println("batch=" + batch));
    }

    @Test
    void forEachBatchIndexed() {
        CollectionUtil.forEachBatchIndexed(users(), 2, (index, batch) -> System.out.println(index + " -> " + batch));
    }

    @Test
    void mapBatch() {
        List<String> result = CollectionUtil.mapBatch(users(), 2, batch -> batch.stream().map(User::name).toList());
        System.out.println(result);
    }

    @Test
    void mapEachBatch() {
        List<Integer> result = CollectionUtil.mapEachBatch(users(), 2, List::size);
        System.out.println(result);
    }

    @Test
    void needBatch() {
        boolean result = CollectionUtil.needBatch(users(), 2);
        System.out.println(result);
    }

    @Test
    void batchCount() {
        int result = CollectionUtil.batchCount(users(), 2);
        System.out.println(result);
    }

    @Test
    void batchIndex() {
        int result = CollectionUtil.batchIndex(5, 2);
        System.out.println(result);
    }

    @Test
    void batchStartIndex() {
        int result = CollectionUtil.batchStartIndex(2, 3);
        System.out.println(result);
    }

    @Test
    void batchEndIndex() {
        int result = CollectionUtil.batchEndIndex(10, 2, 3);
        System.out.println(result);
    }

    private List<User> users() {
        return Arrays.asList(
                new User(1L, "张三"),
                new User(2L, "李四"),
                new User(3L, "王五"),
                new User(4L, "赵六"),
                new User(5L, "钱七")
        );
    }

    record User(Long id, String name) {
    }
}
