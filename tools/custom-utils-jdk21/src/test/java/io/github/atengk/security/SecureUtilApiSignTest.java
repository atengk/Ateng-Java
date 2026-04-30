package io.github.atengk.security;

import io.github.atengk.utils.security.SecurityUtil;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SecurityUtilApiSignTest {

    @Test
    void apiSignShouldVerify() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("b", "2");
        params.put("a", "1");
        String sign = SecurityUtil.signByHmacSha256(params, "secret");
        assertTrue(SecurityUtil.verifyHmacSha256Sign(params, "secret", sign));
        assertEquals("a=1&b=2", SecurityUtil.buildQuerySignText(params));
    }

    @Test
    void timestampNonceAndReplayShouldWork() {
        assertTrue(SecurityUtil.verifyTimestamp(System.currentTimeMillis(), 5));
        HashSet<String> used = new HashSet<>();
        String nonce = SecurityUtil.nonce();
        assertFalse(SecurityUtil.isReplayRequest(nonce, System.currentTimeMillis(), Duration.ofSeconds(5), used));
        assertTrue(SecurityUtil.isReplayRequest(nonce, System.currentTimeMillis(), Duration.ofSeconds(5), used));
    }

    @Test
    void canonicalRequestShouldWork() {
        String request = SecurityUtil.canonicalizeRequest("post", "/api", Map.of("b", "2", "a", "1"), Map.of("X-App", "demo"), "body".getBytes());
        assertTrue(request.startsWith("POST\n/api\na=1&b=2"));
        assertTrue(SecurityUtil.canonicalizeHeaders(Map.of("B", "2", "a", "1")).contains("a:1"));
    }
}
