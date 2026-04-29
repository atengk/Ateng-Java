package io.github.atengk.string;

import io.github.atengk.utils.StringUtil;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class StringUtilIndexTest {

    @Test
    void testIndex() {
        assertEquals(1, StringUtil.indexOf("abcabc", "b"));
        assertEquals(4, StringUtil.indexOf("abcabc", "b", 2));
        assertEquals(0, StringUtil.indexOfIgnoreCase("Abc", "a"));
        assertEquals(3, StringUtil.lastIndexOf("abcabc", "a"));
        assertEquals(3, StringUtil.lastIndexOfIgnoreCase("abcAbc", "A"));
        assertEquals(3, StringUtil.ordinalIndexOf("ab-ab-ab", "ab", 2));
        assertEquals(3, StringUtil.countMatches("ababab", "ab"));
        assertEquals(1, StringUtil.firstIndexOfAny("abc", "c", "b"));
        assertEquals(4, StringUtil.lastIndexOfAny("abcabc", "a", "b"));
    }

}
