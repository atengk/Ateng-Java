package io.github.atengk.maputil;

import io.github.atengk.utils.MapUtil;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class StringParamMapUtilTest {

    @Test
    void shouldBuildQueryStringsAndJoin() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("b", 2);
        map.put("a", "hello world");
        map.put("c", null);

        assertEquals("b=2&a=hello world&c=", MapUtil.toQueryString(map));
        assertEquals("b=2&a=hello+world&c=", MapUtil.toQueryString(map, true));
        assertEquals("a=hello world&b=2&c=", MapUtil.toSortedQueryString(map));
        assertEquals("b:2|a:hello world|c:", MapUtil.join(map, "|", ":"));
        assertEquals("a=hello world&b=2", MapUtil.toSignString(map));
        assertEquals(map.toString(), MapUtil.toLogString(map));
        assertEquals("{}", MapUtil.toLogString(null));
    }

    @Test
    void shouldParseQueryString() {
        Map<String, String> parsed = MapUtil.parseQueryString("?a=hello+world&b=2&c");

        assertEquals("hello world", parsed.get("a"));
        assertEquals("2", parsed.get("b"));
        assertEquals("", parsed.get("c"));
        assertTrue(MapUtil.parseQueryString(" ").isEmpty());
    }
}
