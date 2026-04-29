package io.github.atengk.collection;

import io.github.atengk.utils.CollectionUtil;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;


/**
 * CollectionUtil 第 3 类功能测试：集合转换。
 *
 * @author Ateng
 * @since 2026-04-29
 */
class CollectionUtil03ConvertTest {

    @Test
    void toListArray() {
        String[] array = {"A", "B", "C"};
        List<String> result = CollectionUtil.toList(array);
        System.out.println(result);
    }

    @Test
    void toListIterable() {
        Iterable<String> iterable = Arrays.asList("A", "B", "C");
        List<String> result = CollectionUtil.toList(iterable);
        System.out.println(result);
    }

    @Test
    void toListIterator() {
        Iterator<String> iterator = Arrays.asList("A", "B", "C").iterator();
        List<String> result = CollectionUtil.toList(iterator);
        System.out.println(result);
    }

    @Test
    void toListStream() {
        Stream<String> stream = Stream.of("A", "B", "C");
        List<String> result = CollectionUtil.toList(stream);
        System.out.println(result);
    }

    @Test
    void toSet() {
        List<String> names = Arrays.asList("A", "B", "A");
        Set<String> result = CollectionUtil.toSet(names);
        System.out.println(result);
    }

    @Test
    void toLinkedHashSet() {
        List<String> names = Arrays.asList("A", "B", "A");
        Set<String> result = CollectionUtil.toLinkedHashSet(names);
        System.out.println(result);
    }

    @Test
    void toImmutableList() {
        Stream<String> stream = Stream.of("A", "B", "C");
        List<String> result = CollectionUtil.toImmutableList(stream);
        System.out.println(result);
    }

    @Test
    void toImmutableSet() {
        List<String> names = Arrays.asList("A", "B", "A");
        Set<String> result = CollectionUtil.toImmutableSet(names);
        System.out.println(result);
    }

    @Test
    void toArray() {
        List<String> names = Arrays.asList("A", "B", "C");
        String[] result = CollectionUtil.toArray(names, String[]::new);
        System.out.println(Arrays.toString(result));
    }
}
