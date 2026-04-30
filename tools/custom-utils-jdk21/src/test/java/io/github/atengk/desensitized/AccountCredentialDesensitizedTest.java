package io.github.atengk.desensitized;

import io.github.atengk.utils.desensitized.DesensitizedUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AccountCredentialDesensitizedTest {

    @Test
    void shouldMaskAccountAndCredential() {
        assertEquals("a***n", DesensitizedUtil.username("admin"));
        assertEquals("**", DesensitizedUtil.account("ab"));
        assertEquals("l*****r", DesensitizedUtil.loginName("lk_user"));
        assertEquals("***", DesensitizedUtil.password("secret"));
        assertEquals("***", DesensitizedUtil.salt("abc"));
        assertEquals("abcd********mnop", DesensitizedUtil.token("abcdefghijklmnop"));
        assertEquals("abcd********mnop", DesensitizedUtil.accessToken("abcdefghijklmnop"));
        assertEquals("abcd********mnop", DesensitizedUtil.refreshToken("abcdefghijklmnop"));
        assertEquals("abcd********mnop", DesensitizedUtil.secretKey("abcdefghijklmnop"));
        assertEquals("abcd********mnop", DesensitizedUtil.apiKey("abcdefghijklmnop"));
        assertEquals("abcd********mnop", DesensitizedUtil.appKey("abcdefghijklmnop"));
        assertEquals("abcd********mnop", DesensitizedUtil.appSecret("abcdefghijklmnop"));
        assertEquals("abcd********mnop", DesensitizedUtil.clientSecret("abcdefghijklmnop"));
        assertEquals("Bearer abcd****ijkl", DesensitizedUtil.authorization("Bearer abcdefghijkl"));
        assertEquals("sid=***; token=***", DesensitizedUtil.cookie("sid=abc; token=xyz"));
        assertEquals("abcd********mnop", DesensitizedUtil.sessionId("abcdefghijklmnop"));
    }

    @Test
    void shouldHandleBoundaryCredentialValue() {
        assertNull(DesensitizedUtil.username(null));
        assertEquals("", DesensitizedUtil.password(""));
        assertEquals("abc", DesensitizedUtil.token("abc"));
    }
}
