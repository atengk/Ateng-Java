package io.github.atengk.string;

import io.github.atengk.utils.StringUtil;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class StringUtilAbbreviateTest {

    @Test
    void testAbbreviate() {
        assertEquals("ab...", StringUtil.abbreviate("abcdef", 5));
        assertEquals("ab--", StringUtil.abbreviate("abcdef", 4, "--"));
        assertEquals("a...f", StringUtil.abbreviateMiddle("abcdef", 5));
        assertEquals("ab--f", StringUtil.abbreviateMiddle("abcdef", 5, "--"));
        assertEquals("ab...", StringUtil.ellipsis("abcdef", 5));
        assertEquals("hello...", StringUtil.preview("hello\nworld", 8));
        assertEquals("你好", StringUtil.limitLength("你好世界", 2));
        assertEquals("hello...", StringUtil.linePreview("hello\nworld", 8));
        assertEquals("'abc'", StringUtil.wrap("abc", "'"));
        assertEquals("[abc]", StringUtil.wrap("abc", "[", "]"));
        assertEquals("abc", StringUtil.unwrap("'abc'", "'"));
        assertEquals("abc", StringUtil.unwrap("[abc]", "[", "]"));
    }

}
