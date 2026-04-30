package io.github.atengk.desensitized;

import io.github.atengk.utils.desensitized.DesensitizedUtil;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LogSceneDesensitizedTest {

    @Test
    void shouldMaskLogContent() {
        assertEquals("login mobile=138****5678", DesensitizedUtil.log("login mobile=13812345678"));
        assertEquals("request email=t**t@example.com", DesensitizedUtil.requestLog("request email=test@example.com"));
        assertEquals("response card=6222***********2020", DesensitizedUtil.responseLog("response card=6222020202020202020"));
        assertEquals("error id=110***********1234", DesensitizedUtil.exceptionMessage("error id=110101199001011234"));
        assertEquals("trace mobile=138****5678", DesensitizedUtil.stackTrace("trace mobile=13812345678"));
    }

    @Test
    void shouldMaskHeaderAndParam() {
        assertEquals("Bearer abcd****ijkl", DesensitizedUtil.header("Authorization", "Bearer abcdefghijkl"));
        assertEquals("138****5678", DesensitizedUtil.param("mobile", "13812345678"));

        Map<String, Object> headers = DesensitizedUtil.headers(Map.of("Authorization", "Bearer abcdefghijkl", "TraceId", "trace-001"));
        assertEquals("Bearer abcd****ijkl", headers.get("Authorization"));
        assertEquals("trace-001", headers.get("TraceId"));

        Map<String, Object> params = DesensitizedUtil.params(Map.of("idCard", "110101199001011234"));
        assertEquals("110***********1234", params.get("idCard"));
    }

    @Test
    void shouldMaskBodyAndHandleBoundaryLogValue() {
        assertEquals("body mobile=138****5678", DesensitizedUtil.body("body mobile=13812345678"));
        assertNull(DesensitizedUtil.log(null));
        assertTrue(DesensitizedUtil.headers(null).isEmpty());
        assertTrue(DesensitizedUtil.params(null).isEmpty());
    }
}
