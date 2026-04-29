package io.github.atengk.string;

import io.github.atengk.utils.StringUtil;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class StringUtilContainsTest {

    @Test
    void testContains() {
        assertTrue(StringUtil.contains("hello", "ell"));
        assertTrue(StringUtil.containsIgnoreCase("Hello", "he"));
        assertTrue(StringUtil.containsAny("abc", "x", "b"));
        assertTrue(StringUtil.containsAnyIgnoreCase("abc", "X", "B"));
        assertTrue(StringUtil.containsAll("abc", "a", "b"));
        assertTrue(StringUtil.containsNone("abc", "x", "y"));
        assertTrue(StringUtil.containsWhitespace("a b"));
        assertTrue(StringUtil.containsChinese("abc中文"));
        assertTrue(StringUtil.containsEmoji("ok😊"));
    }

}
