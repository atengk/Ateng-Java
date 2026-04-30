package io.github.atengk.security;

import io.github.atengk.utils.security.SecurityUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.cert.X509Certificate;

import static org.junit.jupiter.api.Assertions.*;

class SecurityUtilPemCertificateTest {

    private static final String CERT_PEM = """
            -----BEGIN CERTIFICATE-----
            MIIDETCCAfmgAwIBAgIIQKoRevpDZlwwDQYJKoZIhvcNAQEMBQAwNzELMAkGA1UE
            BhMCQ04xDjAMBgNVBAoTBUF0ZW5nMRgwFgYDVQQDEw9TZWN1cmVVdGlsIFRlc3Qw
            HhcNMjYwNDMwMDIwNjA4WhcNMzYwNDI3MDIwNjA4WjA3MQswCQYDVQQGEwJDTjEO
            MAwGA1UEChMFQXRlbmcxGDAWBgNVBAMTD1NlY3VyZVV0aWwgVGVzdDCCASIwDQYJ
            KoZIhvcNAQEBBQADggEPADCCAQoCggEBAKtnTvirCBud8Stbsjh3jEyvLpGsuIHy
            hkP7a9g/dD7Sb1RV53LhRV2yOwX6ypH+vi2w670+wSMvEUr5pIxfCUQLjri/t8Tv
            MxPJraaZFYyvx119QXNwMm0dBzOcBbmwXfgEqBJHhlvP/754R30VAYNiz4Ir5HY1
            cejrlfYpjdzUJFNTnVDCeMcc8k9bv5X6fV6a3643Tk7hCy4O785gN8mkByuOa7Vc
            +Fm/Wkfr8M1J4qRCzXw2k7uQWEhFjDqKBEdBe2zLVcBuhhlx+d5z2Ak8EVpZl9N3
            THQ0BPvt6znDOPurKagipmRLo8C1u162GKOWsC0WXs2qd1xCVuvj6f8CAwEAAaMh
            MB8wHQYDVR0OBBYEFK7mJ0WBh8LBV1enkL0zTITtCYMhMA0GCSqGSIb3DQEBDAUA
            A4IBAQAHaztqfq/gtslqtYMG8QjXKfihPQswrSQfIHKlMCoQcm1FfQqlbCaDkzlv
            tGpkmFgClw4iDhHw5I1JFaoewX8cf275Y9Natnj2ZHNxxDajDisPu56ZU2WmjdBo
            9WRevd7OGBXROlWHoJTNSh01qT98Frmy+3r4VmOElGH8sR7+/gI1djeZQVb6UoQG
            tjsXwk7yvsHZchFsymtWdz9Jz3rIUw6XyMmnTgB2fkogJzbPIgK84q1JzU2HhXVn
            GlqUoX4HU8wkdoItEMdctooNQgkB3mi6RV/uw36PYtGIEvpjpRNQ0FL8ebCiiJrQ
            Ab57Z65MvQHP9WvtqGkWY/0ZHMxo
            -----END CERTIFICATE-----
            """;

    @TempDir
    Path tempDir;

    @Test
    void pemKeyShouldRoundTrip() throws Exception {
        KeyPair pair = SecurityUtil.generateRsaKeyPair(2048);
        String publicPem = SecurityUtil.publicKeyToPem(pair.getPublic());
        String privatePem = SecurityUtil.privateKeyToPem(pair.getPrivate());
        assertEquals("PUBLIC KEY", SecurityUtil.detectPemType(publicPem));
        assertEquals("RSA", SecurityUtil.pemToPublicKey(publicPem, "RSA").getAlgorithm());
        assertEquals("RSA", SecurityUtil.pemToPrivateKey(privatePem, "RSA").getAlgorithm());
        Path pem = tempDir.resolve("public.pem");
        SecurityUtil.writePem(pem, "PUBLIC KEY", pair.getPublic().getEncoded());
        assertEquals("RSA", SecurityUtil.readPublicKeyPem(pem, "RSA").getAlgorithm());
    }

    @Test
    void certificateShouldLoadAndInspect() throws Exception {
        X509Certificate certificate = SecurityUtil.pemToCertificate(CERT_PEM);
        assertTrue(SecurityUtil.getSubject(certificate).contains("SecurityUtil Test"));
        assertEquals(SecurityUtil.getSubject(certificate), SecurityUtil.getIssuer(certificate));
        assertFalse(SecurityUtil.isExpired(certificate));
        assertTrue(SecurityUtil.isSelfSigned(certificate));
        assertTrue(SecurityUtil.verifyCertificate(certificate, certificate));
        assertNotNull(SecurityUtil.getSerialNumber(certificate));
        assertTrue(SecurityUtil.getNotAfter(certificate).isAfter(SecurityUtil.getNotBefore(certificate)));
        Path certFile = tempDir.resolve("test.crt");
        Files.writeString(certFile, CERT_PEM);
        assertEquals(1, SecurityUtil.loadCertificateChain(certFile).size());
        assertNotNull(SecurityUtil.loadX509Certificate(certFile));
        assertNotNull(SecurityUtil.readCertificatePem(certFile));
    }

    @Test
    void keyStoreShouldLoadAliases() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(null, "changeit".toCharArray());
        Path file = tempDir.resolve("empty.p12");
        try (var output = Files.newOutputStream(file)) {
            keyStore.store(output, "changeit".toCharArray());
        }
        KeyStore loaded = SecurityUtil.loadPkcs12(file, "changeit");
        assertTrue(SecurityUtil.listAliases(loaded).isEmpty());
        assertFalse(SecurityUtil.containsAlias(loaded, "missing"));
        assertThrows(IllegalArgumentException.class, () -> SecurityUtil.getCertificate(loaded, "missing"));
    }
}
