package io.github.atengk.string;

import io.github.atengk.utils.StringUtil;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class StringUtilPadAlignTest {

    @Test
    void testPadAlign() {
        assertEquals("aaa", StringUtil.repeat("a", 3));
        assertEquals("00a", StringUtil.leftPad("a", 3, "0"));
        assertEquals("a00", StringUtil.rightPad("a", 3, "0"));
        assertEquals("0a0", StringUtil.center("a", 3, "0"));
        assertEquals("00a", StringUtil.padStart("a", 3, "0"));
        assertEquals("a00", StringUtil.padEnd("a", 3, "0"));
        assertEquals("0012", StringUtil.zeroPad("12", 4));
        assertEquals("ab  ", StringUtil.fixedLength("ab", 4));
        assertEquals("ab", StringUtil.fixedLength("abcd", 2));
        assertEquals("ab  ", StringUtil.alignLeft("ab", 4));
        assertEquals("  ab", StringUtil.alignRight("ab", 4));
    }

}
