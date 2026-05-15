package io.github.atengk.http;

import io.github.atengk.utils.http.HttpUtil;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class HttpJsonXmlTextTest {

    @Test
    void shouldSerializeSimpleJsonAndDetectContentTypes() {
        String json = HttpUtil.toJson(Map.of("name", "Ateng", "ids", List.of(1, 2)));
        assertTrue(json.contains("\"name\":\"Ateng\""));
        assertTrue(json.contains("\"ids\":[1,2]"));
        assertTrue(HttpUtil.isJson("application/json"));
        assertTrue(HttpUtil.isXml("application/xml"));
        assertTrue(HttpUtil.isText("text/plain"));
    }

    @Test
    void shouldUseCustomJsonCodec() {
        HttpUtil.JsonCodec codec = new HttpUtil.JsonCodec() {
            @Override
            public <T> T fromJson(String json, Class<T> type) {
                return type.cast(json);
            }

            @Override
            public String toJson(Object value) {
                return String.valueOf(value);
            }
        };
        assertEquals("abc", HttpUtil.fromJson("abc", codec, String.class));
    }
}
