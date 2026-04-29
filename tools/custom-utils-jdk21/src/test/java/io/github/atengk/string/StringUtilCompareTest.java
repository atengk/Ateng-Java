package io.github.atengk.string;

import io.github.atengk.utils.StringUtil;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class StringUtilCompareTest {

    @Test
    void testCompare() {
        assertTrue(StringUtil.equals(null, null));
        assertFalse(StringUtil.equals(null, "a"));
        assertTrue(StringUtil.equals("abc", "abc"));
        assertTrue(StringUtil.equalsIgnoreCase("abc", "ABC"));
        assertTrue(StringUtil.equalsAny("b", "a", "b"));
        assertTrue(StringUtil.equalsAnyIgnoreCase("B", "a", "b"));
        assertEquals(0, StringUtil.compare("a", "a"));
        assertTrue(StringUtil.compare(null, "a") < 0);
        assertTrue(StringUtil.compare(null, "a", false) > 0);
        assertEquals(0, StringUtil.compareIgnoreCase("a", "A"));
        assertTrue(StringUtil.equalsNormalized("a  b", "a b"));
        assertTrue(StringUtil.equalsTrimmed(" a ", "a"));
    }

}
