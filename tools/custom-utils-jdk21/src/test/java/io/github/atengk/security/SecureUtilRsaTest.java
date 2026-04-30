package io.github.atengk.security;

import io.github.atengk.utils.security.SecurityUtil;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;

import static org.junit.jupiter.api.Assertions.*;

class SecurityUtilRsaTest {

    @Test
    void rsaOaepShouldRoundTrip() {
        KeyPair pair = SecurityUtil.generateRsaKeyPair(2048);
        String cipher = SecurityUtil.rsaEncryptByPublicKey("hello", pair.getPublic());
        assertEquals("hello", SecurityUtil.rsaDecryptByPrivateKey(cipher, pair.getPrivate()));
    }

    @Test
    void rsaPkcs1AndBlockShouldRoundTrip() {
        KeyPair pair = SecurityUtil.generateRsaKeyPair(2048);
        byte[] data = "a".repeat(300).getBytes(StandardCharsets.UTF_8);
        byte[] encrypted = SecurityUtil.rsaPkcs1Encrypt(data, pair.getPublic());
        assertArrayEquals(data, SecurityUtil.rsaPkcs1Decrypt(encrypted, pair.getPrivate()));
        assertTrue(SecurityUtil.getMaxRsaEncryptBlockSize(pair.getPublic(), "PKCS1") > 0);
    }

    @Test
    void hybridEncryptionShouldRoundTrip() {
        KeyPair pair = SecurityUtil.generateRsaKeyPair(2048);
        byte[] data = "hybrid-data".getBytes(StandardCharsets.UTF_8);
        String encrypted = SecurityUtil.encryptByKeyPair(data, pair.getPublic());
        assertArrayEquals(data, SecurityUtil.decryptByKeyPair(encrypted, pair.getPrivate()));
    }

    @Test
    void rsaShouldRejectInvalidKey() {
        assertThrows(IllegalArgumentException.class, () -> SecurityUtil.getMaxRsaEncryptBlockSize(SecurityUtil.generateAesKey(128), "PKCS1"));
    }
}
