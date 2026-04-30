package io.github.atengk.security;

import io.github.atengk.utils.security.SecurityUtil;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SecurityUtilJwtTest {

    @Test
    void jwtShouldCreateVerifyAndParse() {
        String token = SecurityUtil.createAccessToken("user-1", Map.of("role", "admin"), "secret", Duration.ofMinutes(5));
        assertTrue(SecurityUtil.verifyToken(token, "secret"));
        assertFalse(SecurityUtil.verifyToken(token, "bad"));
        assertEquals("user-1", SecurityUtil.getSubject(token));
        assertEquals("admin", SecurityUtil.getClaim(token, "role"));
        assertNotNull(SecurityUtil.getIssuedAt(token));
        assertNotNull(SecurityUtil.getExpireTime(token));
        assertFalse(SecurityUtil.isExpired(token));
    }

    @Test
    void bearerAndRefreshShouldWork() {
        String token = SecurityUtil.createRefreshToken("user-1", "secret", Duration.ofMinutes(5));
        String bearer = SecurityUtil.buildBearerToken(token);
        assertEquals(token, SecurityUtil.extractBearerToken(bearer));
        String refreshed = SecurityUtil.refreshToken(token, "secret", Duration.ofMinutes(10));
        assertTrue(SecurityUtil.verifyToken(refreshed, "secret"));
    }

    @Test
    void jwtShouldRejectInvalidInput() {
        assertThrows(IllegalArgumentException.class, () -> SecurityUtil.extractBearerToken("Basic abc"));
        assertThrows(IllegalArgumentException.class, () -> SecurityUtil.createToken(Map.of(), "secret", Duration.ZERO));
        assertFalse(SecurityUtil.verifyToken("a.b.c", "secret"));
    }
}
