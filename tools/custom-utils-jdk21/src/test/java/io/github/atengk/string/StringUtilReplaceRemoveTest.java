package io.github.atengk.string;

import io.github.atengk.utils.StringUtil;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class StringUtilReplaceRemoveTest {

    @Test
    void testReplaceRemove() {
        assertEquals("aXcX", StringUtil.replace("abcb", "b", "X"));
        assertEquals("XX", StringUtil.replaceIgnoreCase("aA", "a", "X"));
        assertEquals("xbc", StringUtil.replaceFirst("abc", "a", "x"));
        assertEquals("abx", StringUtil.replaceLast("abc", "c", "x"));
        Map<String, String> map = new LinkedHashMap<>();
        map.put("a", "x");
        map.put("b", "y");
        assertEquals("xyc", StringUtil.replaceEach("abc", map));
        assertEquals("ac", StringUtil.remove("abc", "b"));
        assertEquals("c", StringUtil.removeIgnoreCase("aBc", "b"));
        assertEquals("bc", StringUtil.removePrefix("abc", "a"));
        assertEquals("ab", StringUtil.removeSuffix("abc", "c"));
        assertEquals("bc", StringUtil.removeStartIgnoreCase("Abc", "a"));
        assertEquals("ab", StringUtil.removeEndIgnoreCase("abC", "c"));
    }

}
