package io.github.atengk.string;

import io.github.atengk.utils.StringUtil;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class StringUtilSubstringTest {

    @Test
    void testSubstring() {
        assertEquals("bc", StringUtil.substring("abcd", 1, 3));
        assertEquals("ab", StringUtil.safeSubstring("abcd", -1, 2));
        assertEquals("ab", StringUtil.substringBefore("ab:cd", ":"));
        assertEquals("cd", StringUtil.substringAfter("ab:cd", ":"));
        assertEquals("ab:cd", StringUtil.substringBeforeLast("ab:cd:ef", ":"));
        assertEquals("ef", StringUtil.substringAfterLast("ab:cd:ef", ":"));
        assertEquals("ab", StringUtil.left("abcd", 2));
        assertEquals("cd", StringUtil.right("abcd", 2));
        assertEquals("bc", StringUtil.mid("abcd", 1, 2));
        assertEquals("abc", StringUtil.truncate("abcdef", 3));
        assertEquals("ab...", StringUtil.truncateWithSuffix("abcdef", 5, "..."));
    }

}
