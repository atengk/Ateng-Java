package io.github.atengk.security;

import io.github.atengk.utils.security.SecurityUtil;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SecurityUtilSensitiveDataCryptoTest {

    @Test
    void sensitiveFieldsShouldRoundTrip() {
        byte[] key = SecurityUtil.generateAesKey(128).getEncoded();
        String mobile = SecurityUtil.encryptMobile("13812345678", key);
        assertEquals("13812345678", SecurityUtil.decryptMobile(mobile, key));
        String email = SecurityUtil.encryptEmail("a@example.com", key);
        assertEquals("a@example.com", SecurityUtil.decryptEmail(email, key));
        String idCard = SecurityUtil.encryptIdCard("110101199001011234", key);
        assertEquals("110101199001011234", SecurityUtil.decryptIdCard(idCard, key));
        String bankCard = SecurityUtil.encryptBankCard("6222000000000000", key);
        assertEquals("6222000000000000", SecurityUtil.decryptBankCard(bankCard, key));
    }

    @Test
    void jsonFieldsShouldRoundTrip() {
        byte[] key = SecurityUtil.generateAesKey(128).getEncoded();
        String json = "{\"mobile\":\"13812345678\",\"name\":\"Ateng\"}";
        String encrypted = SecurityUtil.encryptJsonFields(json, Set.of("mobile"), key);
        assertNotEquals(json, encrypted);
        assertEquals(json, SecurityUtil.decryptJsonFields(encrypted, Set.of("mobile"), key));
    }

    @Test
    void fieldCryptoShouldRejectBadInput() {
        byte[] key = SecurityUtil.generateAesKey(128).getEncoded();
        assertThrows(IllegalArgumentException.class, () -> SecurityUtil.encryptField("a", "", key));
    }
}
