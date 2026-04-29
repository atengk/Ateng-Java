package io.github.atengk.string;

import io.github.atengk.utils.StringUtil;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class StringUtilSplitTest {

    @Test
    void testSplit() {
        assertArrayEquals(new String[]{"a", "b", ""}, StringUtil.split("a,b,", ","));
        assertEquals(List.of("a", "b"), StringUtil.splitToList("a,b", ","));
        assertArrayEquals(new String[]{"a", "b"}, StringUtil.splitTrim(" a , b ", ","));
        assertEquals(List.of("a", "b"), StringUtil.splitTrimToList(" a , b ", ","));
        assertEquals(List.of("ab", "cd", "e"), StringUtil.splitByLength("abcde", 2));
        assertEquals(List.of("a", "b"), StringUtil.splitLines("a\nb"));
        assertArrayEquals(new String[]{"a", "b:c"}, StringUtil.splitFirst("a:b:c", ":"));
        assertArrayEquals(new String[]{"a:b", "c"}, StringUtil.splitLast("a:b:c", ":"));
        assertEquals(Set.of("a", "b"), StringUtil.splitToSet("a,b,a", ","));
        assertArrayEquals(new String[]{"a", "b"}, StringUtil.splitToArray("a,b", ","));
    }

}
