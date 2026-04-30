package io.github.atengk.id;

import io.github.atengk.utils.id.IdUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SecureIdUtilTest {

    @Test
    void shouldGenerateSecureIds() {
        assertEquals(32, IdUtil.secureId().length());
        assertEquals(12, IdUtil.secureId(12).length());
        assertEquals(16, IdUtil.nonce().length());
        assertEquals(10, IdUtil.nonce(10).length());
    }

    @Test
    void shouldGenerateSecureTokensAndSecretKey() {
        String token = IdUtil.secureToken(16);
        assertFalse(token.contains("="));
        assertArrayEquals(IdUtil.fromBase64Url(token), IdUtil.fromBase64Url(token));
        assertFalse(IdUtil.secretKey().contains("="));
    }

    @Test
    void shouldRejectInvalidSecureArguments() {
        assertThrows(IllegalArgumentException.class, () -> IdUtil.secureId(0));
        assertThrows(IllegalArgumentException.class, () -> IdUtil.secureToken(0));
        assertThrows(IllegalArgumentException.class, () -> IdUtil.nonce(0));
    }
}
