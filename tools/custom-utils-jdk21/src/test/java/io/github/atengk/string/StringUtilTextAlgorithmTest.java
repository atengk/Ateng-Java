package io.github.atengk.string;

import io.github.atengk.utils.StringUtil;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class StringUtilTextAlgorithmTest {

    @Test
    void testTextAlgorithm() {
        assertEquals("cba", StringUtil.reverse("abc"));
        assertEquals("😊a", StringUtil.reverse("a😊"));
        assertTrue(StringUtil.isPalindrome("上海自来水来自海上"));
        assertEquals(3, StringUtil.levenshteinDistance("kitten", "sitting"));
        assertEquals(1D, StringUtil.similarity("abc", "abc"));
        assertEquals("ab", StringUtil.commonPrefix("abcd", "abxy"));
        assertEquals("cd", StringUtil.commonSuffix("abcd", "xycd"));
        assertEquals("bcd", StringUtil.longestCommonSubstring("abcde", "xbcdx"));
    }

}
