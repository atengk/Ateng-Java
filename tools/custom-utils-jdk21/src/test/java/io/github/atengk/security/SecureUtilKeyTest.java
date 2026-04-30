package io.github.atengk.security;

import io.github.atengk.utils.security.SecurityUtil;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.security.KeyPair;

import static org.junit.jupiter.api.Assertions.*;

class SecurityUtilKeyTest {

    @Test
    void aesAndHmacKeysShouldGenerate() {
        SecretKey aesKey = SecurityUtil.generateAesKey(256);
        assertEquals("AES", aesKey.getAlgorithm());
        assertEquals(32, aesKey.getEncoded().length);
        assertEquals(32, SecurityUtil.base64ToAesKey(SecurityUtil.secretKeyToBase64(aesKey)).getEncoded().length);
        assertEquals("HmacSHA256", SecurityUtil.generateHmacSha256Key().getAlgorithm());
        assertEquals("HmacSHA512", SecurityUtil.generateHmacSha512Key().getAlgorithm());
    }

    @Test
    void keyPairShouldGenerateAndConvert() {
        KeyPair rsa = SecurityUtil.generateRsaKeyPair(2048);
        String pub = SecurityUtil.publicKeyToBase64(rsa.getPublic());
        String pri = SecurityUtil.privateKeyToBase64(rsa.getPrivate());
        assertEquals("RSA", SecurityUtil.base64ToPublicKey(pub, "RSA").getAlgorithm());
        assertEquals("RSA", SecurityUtil.base64ToPrivateKey(pri, "RSA").getAlgorithm());
        assertEquals("EC", SecurityUtil.generateEcKeyPair("secp256r1").getPublic().getAlgorithm());
        assertEquals("EdDSA", SecurityUtil.generateEd25519KeyPair().getPublic().getAlgorithm());
    }

    @Test
    void deriveAndValidateShouldHandleInvalidInput() {
        byte[] salt = SecurityUtil.salt(16);
        assertEquals(16, SecurityUtil.deriveAesKeyByPbkdf2("pwd", salt, 1000, 128).getEncoded().length);
        assertDoesNotThrow(() -> SecurityUtil.validateAesKey(new byte[16]));
        assertThrows(IllegalArgumentException.class, () -> SecurityUtil.generateAesKey(64));
        assertThrows(IllegalArgumentException.class, () -> SecurityUtil.generateRsaKeyPair(1024));
        assertThrows(IllegalArgumentException.class, () -> SecurityUtil.validateAesKey(new byte[15]));
    }
}
