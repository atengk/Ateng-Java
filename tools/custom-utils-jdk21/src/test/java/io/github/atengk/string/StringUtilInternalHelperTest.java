package io.github.atengk.string;

import io.github.atengk.utils.StringUtil;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class StringUtilInternalHelperTest {

    @Test
    void testInternalHelper() {
        String value = "abc";
        assertSame(value, StringUtil.requireNotNull(value, "不能为空"));
        assertEquals(value, StringUtil.requireNotBlank(value, "不能为空白"));
        assertThrows(IllegalArgumentException.class, () -> StringUtil.requireNotNull(null, "不能为空"));
        assertThrows(IllegalArgumentException.class, () -> StringUtil.requireNotBlank(" ", "不能为空白"));
        assertTrue(StringUtil.isIndexValid("abc", 1));
        assertFalse(StringUtil.isIndexValid("abc", 3));
        assertTrue(StringUtil.isRangeValid("abc", 0, 3));
        assertFalse(StringUtil.isRangeValid("abc", 2, 4));
        assertArrayEquals(new char[]{'a', 'b'}, StringUtil.toCharArray("ab"));
        assertTrue(StringUtil.isCharChinese('中'));
        assertTrue(StringUtil.isCharAscii('A'));
        assertTrue(StringUtil.isCharEmoji('☀'));
    }

}
