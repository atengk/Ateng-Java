package io.github.atengk.http;

import io.github.atengk.utils.http.HttpUtil;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class HttpUriAndParameterTest {

    @Test
    void shouldBuildUrlQueryAndPathParams() {
        String query = HttpUtil.toQueryString(Map.of("name", "张三", "tag", List.of("a", "b")));
        assertTrue(query.contains("name=%E5%BC%A0%E4%B8%89"));
        assertTrue(query.contains("tag=a"));
        assertTrue(query.contains("tag=b"));

        String url = HttpUtil.appendQuery("http://example.com/api", Map.of("q", "hello world"));
        assertEquals("http://example.com/api?q=hello%20world", url);

        String path = HttpUtil.replacePathParams("/user/{id}/file/{name}", Map.of("id", 1, "name", "测试.txt"));
        assertEquals("/user/1/file/%E6%B5%8B%E8%AF%95.txt", path);

        assertEquals("http://example.com/a/b", HttpUtil.joinUrl("http://example.com/", "/a/", "b"));
        assertTrue(HttpUtil.isValidUrl("https://example.com"));
        assertEquals("a_b_c.txt", HttpUtil.sanitizeFileName("a/b:c.txt"));
    }
}
