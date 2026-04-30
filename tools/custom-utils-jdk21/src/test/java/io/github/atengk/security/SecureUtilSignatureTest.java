package io.github.atengk.security;

import io.github.atengk.utils.security.SecurityUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;

import static org.junit.jupiter.api.Assertions.*;

class SecurityUtilSignatureTest {

    @TempDir
    Path tempDir;

    @Test
    void rsaSignatureShouldVerify() {
        KeyPair pair = SecurityUtil.generateRsaKeyPair(2048);
        String signature = SecurityUtil.signSha256WithRsa("data", pair.getPrivate());
        assertTrue(SecurityUtil.verifySha256WithRsa("data", signature, pair.getPublic()));
        assertFalse(SecurityUtil.verifySha256WithRsa("tampered", signature, pair.getPublic()));
        assertTrue(SecurityUtil.verifySha512WithRsa("data", SecurityUtil.signSha512WithRsa("data", pair.getPrivate()), pair.getPublic()));
    }

    @Test
    void ed25519AndEcdsaShouldVerify() {
        KeyPair ed = SecurityUtil.generateEd25519KeyPair();
        byte[] edSign = SecurityUtil.signEd25519("data".getBytes(), ed.getPrivate());
        assertTrue(SecurityUtil.verifyEd25519("data".getBytes(), edSign, ed.getPublic()));
        KeyPair ec = SecurityUtil.generateEcKeyPair("secp256r1");
        byte[] ecSign = SecurityUtil.signEcdsaSha256("data".getBytes(), ec.getPrivate());
        assertTrue(SecurityUtil.verifyEcdsaSha256("data".getBytes(), ecSign, ec.getPublic()));
    }

    @Test
    void fileSignatureShouldVerify() throws Exception {
        KeyPair pair = SecurityUtil.generateRsaKeyPair(2048);
        Path file = tempDir.resolve("signed.txt");
        Files.writeString(file, "signed-content");
        byte[] signature = SecurityUtil.signFile(file, pair.getPrivate(), "SHA256withRSA");
        assertTrue(SecurityUtil.verifyFileSignature(file, signature, pair.getPublic(), "SHA256withRSA"));
    }
}
