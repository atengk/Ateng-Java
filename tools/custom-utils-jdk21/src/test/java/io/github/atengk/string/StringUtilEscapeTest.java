package io.github.atengk.string;

import io.github.atengk.utils.StringUtil;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class StringUtilEscapeTest {

    @Test
    void testEscape() {
        assertEquals("\\%abc\\_", StringUtil.escapeSqlLike("%abc_"));
        assertEquals("a\\\"b", StringUtil.escapeJson("a\"b"));
        assertEquals("a\"b", StringUtil.unescapeJson("a\\\"b"));
        assertEquals("\"abc\"", StringUtil.quote("abc"));
        assertEquals("abc", StringUtil.unquote("\"abc\""));
        assertEquals("'a\\'b'", StringUtil.singleQuote("a'b"));
        assertEquals("\"a\\\"b\"", StringUtil.doubleQuote("a\"b"));
        assertEquals("abc", StringUtil.removeQuotes("`a'b\"c`"));
    }

}
