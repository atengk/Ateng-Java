package io.github.atengk.security;

import io.github.atengk.utils.security.SecurityUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SecurityUtilConfigCryptoTest {

    @Test
    void configValueShouldRoundTrip() {
        byte[] key = SecurityUtil.generateAesKey(128).getEncoded();
        String encrypted = SecurityUtil.encryptConfigValue("db-password", key);
        assertTrue(SecurityUtil.isEncryptedValue(encrypted));
        assertEquals("db-password", SecurityUtil.decryptConfigValue(encrypted, key));
        assertEquals("plain", SecurityUtil.decryptIfNecessary("plain", key));
        assertEquals("db-password", SecurityUtil.decryptDataSourcePassword(SecurityUtil.encryptDataSourcePassword("db-password", key), key));
    }

    @Test
    void wrapUnwrapAndRotateShouldWork() {
        byte[] oldKey = SecurityUtil.generateAesKey(128).getEncoded();
        byte[] newKey = SecurityUtil.generateAesKey(128).getEncoded();
        String encrypted = SecurityUtil.encryptConfigValue("value", oldKey);
        String rotated = SecurityUtil.rotateMasterKey(encrypted, oldKey, newKey);
        assertEquals("value", SecurityUtil.decryptConfigValue(rotated, newKey));
        assertEquals("abc", SecurityUtil.unwrapEncryptedValue(SecurityUtil.wrapEncryptedValue("abc")));
    }

    @Test
    void masterKeyShouldLoadFromSystemProperty() {
        byte[] key = SecurityUtil.generateAesKey(128).getEncoded();
        String old = System.getProperty("secure.master-key");
        try {
            System.setProperty("secure.master-key", SecurityUtil.base64Encode(key));
            assertArrayEquals(key, SecurityUtil.loadMasterKey());
        } finally {
            if (old == null) {
                System.clearProperty("secure.master-key");
            } else {
                System.setProperty("secure.master-key", old);
            }
        }
    }
}
