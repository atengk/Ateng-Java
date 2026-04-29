package io.github.atengk.string;

import io.github.atengk.utils.StringUtil;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class StringUtilMultilineTest {

    @Test
    void testMultiline() {
        assertEquals("a\nb", StringUtil.normalizeLineSeparator("a\r\nb"));
        assertEquals("a|b", StringUtil.normalizeLineSeparator("a\r\nb", "|"));
        assertEquals("a\n ", StringUtil.removeEmptyLines("a\n\n "));
        assertEquals("a", StringUtil.removeBlankLines("a\n\n "));
        assertEquals("a\nb", StringUtil.trimLines(" a \n b "));
        assertEquals("  a\n  b", StringUtil.indentLines("a\nb", "  "));
        assertEquals("> a\n> b", StringUtil.prefixLines("a\nb", "> "));
        assertEquals("a;\nb;", StringUtil.suffixLines("a\nb", ";"));
        assertEquals(2, StringUtil.lineCount("a\nb"));
    }

}
