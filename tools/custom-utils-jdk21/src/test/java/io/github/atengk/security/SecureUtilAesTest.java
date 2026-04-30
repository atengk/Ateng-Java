package io.github.atengk.security;

import io.github.atengk.utils.security.SecurityUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.crypto.SecretKey;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class SecurityUtilAesTest {

    @TempDir
    Path tempDir;

    @Test
    void aesGcmShouldRoundTrip() {
        byte[] key = SecurityUtil.generateAesKey(256).getEncoded();
        String cipher = SecurityUtil.aesGcmEncrypt("明文", key);
        assertEquals("明文", SecurityUtil.aesGcmDecrypt(cipher, key));
        String base64Key = SecurityUtil.base64Encode(key);
        String base64Cipher = SecurityUtil.aesGcmEncryptToBase64("text", base64Key);
        assertEquals("text", SecurityUtil.aesGcmDecryptFromBase64(base64Cipher, base64Key));
    }

    @Test
    void aesCbcAndEcbShouldRoundTrip() {
        byte[] key = SecurityUtil.generateAesKey(128).getEncoded();
        byte[] iv = SecurityUtil.iv(16);
        byte[] cbc = SecurityUtil.aesCbcEncrypt("abc".getBytes(), key, iv);
        assertArrayEquals("abc".getBytes(), SecurityUtil.aesCbcDecrypt(cbc, key, iv));
        byte[] ecb = SecurityUtil.aesEcbEncrypt("abc".getBytes(), key);
        assertArrayEquals("abc".getBytes(), SecurityUtil.aesEcbDecrypt(ecb, key));
    }

    @Test
    void fileEncryptionShouldRoundTrip() throws Exception {
        SecretKey key = SecurityUtil.generateAesKey(128);
        Path source = tempDir.resolve("source.txt");
        Path encrypted = tempDir.resolve("source.enc");
        Path decrypted = tempDir.resolve("source.dec.txt");
        Files.writeString(source, "file-content");
        SecurityUtil.encryptFile(source, encrypted, key.getEncoded());
        SecurityUtil.decryptFile(encrypted, decrypted, key.getEncoded());
        assertEquals("file-content", Files.readString(decrypted));
    }

    @Test
    void aesShouldRejectBadInput() {
        assertThrows(IllegalArgumentException.class, () -> SecurityUtil.aesGcmEncrypt("a", new byte[3]));
        assertThrows(IllegalArgumentException.class, () -> SecurityUtil.splitIvAndCipher(new byte[2], 2));
    }
}
