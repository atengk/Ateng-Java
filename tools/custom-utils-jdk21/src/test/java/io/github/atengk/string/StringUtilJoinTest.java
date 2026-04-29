package io.github.atengk.string;

import io.github.atengk.utils.StringUtil;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class StringUtilJoinTest {

    @Test
    void testJoin() {
        assertEquals("a,b,c", StringUtil.join(",", "a", "b", "c"));
        assertEquals("a,b", StringUtil.join(List.of("a", "b"), ","));
        assertEquals("a,b", StringUtil.joinIgnoreNull(",", "a", null, "b"));
        assertEquals("a,b", StringUtil.joinIgnoreEmpty(",", "a", "", "b"));
        assertEquals("a,b", StringUtil.joinIgnoreBlank(",", "a", " ", "b"));
        assertEquals("#a,#b", StringUtil.joinWithPrefix(List.of("a", "b"), ",", "#"));
        assertEquals("a;,b;", StringUtil.joinWithSuffix(List.of("a", "b"), ",", ";"));
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        assertEquals("a=1&b=2", StringUtil.joinMap(map, "&", "="));
        assertEquals("abc", StringUtil.concat("a", null, "b", "c"));
        assertEquals("ab", StringUtil.concatIfNotBlank("a", " ", "b"));
    }

}
