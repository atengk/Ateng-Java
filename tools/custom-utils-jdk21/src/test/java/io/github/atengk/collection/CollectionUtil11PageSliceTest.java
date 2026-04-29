package io.github.atengk.collection;

import io.github.atengk.utils.CollectionUtil;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;


/**
 * CollectionUtil 第 11 类功能测试：分页与切片。
 *
 * @author Ateng
 * @since 2026-04-29
 */
class CollectionUtil11PageSliceTest {

    @Test
    void page() {
        List<String> result = CollectionUtil.page(names(), 2, 3);
        System.out.println(result);
    }

    @Test
    void pageByOffset() {
        List<String> result = CollectionUtil.pageByOffset(names(), 2, 3);
        System.out.println(result);
    }

    @Test
    void slice() {
        List<String> result = CollectionUtil.slice(names(), 1, 5);
        System.out.println(result);
    }

    @Test
    void limit() {
        List<String> result = CollectionUtil.limit(names(), 3);
        System.out.println(result);
    }

    @Test
    void limitOffset() {
        List<String> result = CollectionUtil.limit(names(), 2, 3);
        System.out.println(result);
    }

    @Test
    void skip() {
        List<String> result = CollectionUtil.skip(names(), 4);
        System.out.println(result);
    }

    @Test
    void head() {
        List<String> result = CollectionUtil.head(names(), 2);
        System.out.println(result);
    }

    @Test
    void tail() {
        List<String> result = CollectionUtil.tail(names(), 2);
        System.out.println(result);
    }

    @Test
    void window() {
        List<String> result = CollectionUtil.window(names(), 3, 3);
        System.out.println(result);
    }

    @Test
    void pageArray() {
        String[] array = {"A", "B", "C", "D", "E", "F"};
        List<String> result = CollectionUtil.page(array, 2, 3);
        System.out.println(result);
    }

    @Test
    void pageOffset() {
        int result = CollectionUtil.pageOffset(3, 10);
        System.out.println(result);
    }

    @Test
    void pageCount() {
        int result = CollectionUtil.pageCount(95, 10);
        System.out.println(result);
    }

    @Test
    void hasPreviousPage() {
        boolean result = CollectionUtil.hasPreviousPage(2);
        System.out.println(result);
    }

    @Test
    void hasNextPage() {
        boolean result = CollectionUtil.hasNextPage(95, 2, 10);
        System.out.println(result);
    }

    @Test
    void isValidPage() {
        boolean result = CollectionUtil.isValidPage(95, 10, 10);
        System.out.println(result);
    }

    private List<String> names() {
        return Arrays.asList("A", "B", "C", "D", "E", "F", "G");
    }
}
