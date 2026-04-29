package io.github.atengk.string;

import io.github.atengk.utils.StringUtil;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class StringUtilWhitespaceCleanTest {

    @Test
    void testWhitespaceClean() {
        assertNull(StringUtil.trim(null));
        assertEquals("abc", StringUtil.trim(" abc "));
        assertEquals("", StringUtil.trimToEmpty(null));
        assertNull(StringUtil.trimToNull("   "));
        assertEquals("abc", StringUtil.strip("\u2003abc\u2003"));
        assertEquals("", StringUtil.stripToEmpty(null));
        assertNull(StringUtil.stripToNull("\u2003"));
        assertEquals("abc", StringUtil.removeAllWhitespace(" a b\tc\n"));
        assertEquals("a b c", StringUtil.normalizeWhitespace(" a\t b\n c "));
        assertEquals("abc", StringUtil.cleanInvisibleChars("a\u200Bb\uFEFFc"));
        assertEquals("a\n\tb", StringUtil.cleanControlChars("a\u0000\n\tb"));
    }

}
