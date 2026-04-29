package io.github.atengk.string;

import io.github.atengk.utils.StringUtil;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class StringUtilPrefixSuffixTest {

    @Test
    void testPrefixSuffix() {
        assertTrue(StringUtil.startsWith("abcdef", "abc"));
        assertTrue(StringUtil.startsWithIgnoreCase("Abcdef", "abc"));
        assertTrue(StringUtil.startsWithAny("abcdef", "x", "ab"));
        assertTrue(StringUtil.endsWith("abcdef", "def"));
        assertTrue(StringUtil.endsWithIgnoreCase("abcdef", "DEF"));
        assertTrue(StringUtil.endsWithAny("abcdef", "x", "ef"));
        assertTrue(StringUtil.hasPrefix("abcdef", "abc"));
        assertTrue(StringUtil.hasSuffix("abcdef", "def"));
    }

}
