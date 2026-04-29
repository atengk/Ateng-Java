package io.github.atengk.string;

import io.github.atengk.utils.StringUtil;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class StringUtilRegexTest {

    @Test
    void testRegex() {
        assertTrue(StringUtil.matches("123", "\\d+"));
        assertTrue(StringUtil.find("abc123", "\\d+"));
        assertEquals(List.of("12", "34"), StringUtil.findAll("ab12cd34", "\\d+"));
        assertEquals("ab#", StringUtil.replaceByRegex("ab123", "\\d+", "#"));
        assertEquals("ab", StringUtil.removeByRegex("ab123", "\\d+"));
        assertArrayEquals(new String[]{"a", "b", ""}, StringUtil.splitByRegex("a,b,", ","));
        assertTrue(StringUtil.isRegexMatch("abc", "[a-z]+"));
        assertEquals("123", StringUtil.getFirstMatch("abc123", "\\d+"));
        assertEquals("123", StringUtil.getMatchGroup("abc123", "([a-z]+)(\\d+)", 2));
    }

}
