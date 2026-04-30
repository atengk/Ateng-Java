package io.github.atengk.codec;

import io.github.atengk.utils.codec.CodecUtil;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class UrlQueryCodecUtilTest {

    @Test
    void shouldBuildQueryString() {
        Map<String, Object> params = Map.of(
                "name", "张三",
                "tags", List.of("java", "jdk21"),
                "page", 1
        );
        String query = CodecUtil.buildQueryString(params);
        assertTrue(query.contains("name=%E5%BC%A0%E4%B8%89"));
        assertTrue(query.contains("tags=java"));
        assertTrue(query.contains("tags=jdk21"));
        assertTrue(query.contains("page=1"));
    }

    @Test
    void shouldSkipOrKeepNullValues() {
        Map<String, Object> params = new java.util.LinkedHashMap<>();
        params.put("a", null);
        params.put("b", "2");
        assertEquals("b=2", CodecUtil.buildQueryString(params));
        assertEquals("a=&b=2", CodecUtil.buildQueryString(params, false));
    }

    @Test
    void shouldParseQueryStringWithRepeatedParams() {
        Map<String, List<String>> params = CodecUtil.parseQueryString("?name=%E5%BC%A0%E4%B8%89&tag=java&tag=jdk21#top");
        assertEquals("张三", params.get("name").getFirst());
        assertEquals(List.of("java", "jdk21"), params.get("tag"));
    }

    @Test
    void shouldAppendQueryParamBeforeFragment() {
        String url = CodecUtil.appendQueryParam("https://example.com/api#top", "name", "张三");
        assertEquals("https://example.com/api?name=%E5%BC%A0%E4%B8%89#top", url);
    }

    @Test
    void shouldAppendMultipleQueryParams() {
        String url = CodecUtil.appendQueryParams("https://example.com/api?a=1", Map.of("b", 2, "c", "中文"));
        assertTrue(url.startsWith("https://example.com/api?a=1&"));
        assertTrue(url.contains("b=2"));
        assertTrue(url.contains("c=%E4%B8%AD%E6%96%87"));
    }

    @Test
    void shouldRemoveAndReadQueryParam() {
        String url = "https://example.com/api?a=1&b=2#top";
        assertEquals("1", CodecUtil.getQueryParam(url, "a"));
        assertTrue(CodecUtil.hasQueryParam(url, "b"));
        assertEquals("https://example.com/api?a=1#top", CodecUtil.removeQueryParam(url, "b"));
    }

    @Test
    void shouldNormalizeQueryString() {
        assertEquals("name=%E5%BC%A0%E4%B8%89&tag=java", CodecUtil.normalizeQueryString("?name=张三&tag=java&&"));
    }

    @Test
    void shouldRejectBlankUrlOrParamName() {
        assertThrows(IllegalArgumentException.class, () -> CodecUtil.appendQueryParam(" ", "a", 1));
        assertThrows(IllegalArgumentException.class, () -> CodecUtil.getQueryParam("https://example.com", " "));
    }
}
