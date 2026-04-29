package io.github.atengk.string;

import io.github.atengk.utils.StringUtil;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class StringUtilCountTest {

    @Test
    void testCount() {
        assertEquals(3, StringUtil.length("abc"));
        assertEquals(6, StringUtil.byteLength("中文", StandardCharsets.UTF_8));
        assertEquals(2, StringUtil.charLength("😊a"));
        assertEquals(3, StringUtil.countChars("banana", 'a'));
        assertEquals(2, StringUtil.countChinese("中文abc"));
        assertEquals(3, StringUtil.countLetters("ab1中"));
        assertEquals(2, StringUtil.countDigits("a12b"));
        assertEquals(2, StringUtil.countUpperCase("ABcd"));
        assertEquals(2, StringUtil.countLowerCase("ABcd"));
        assertEquals(3, StringUtil.countLines("a\nb\n"));
    }

}
