package io.github.atengk.zip;

import io.github.atengk.utils.zip.ZipUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ZipUtilPasswordZipTest {

    @TempDir
    Path tempDir;

    @Test
    void zipWithAesShouldRequirePasswordForExtraction() throws Exception {
        Path source = ZipUtilTestSupport.text(tempDir, "secret.txt", "secret");
        Path zip = tempDir.resolve("secret.zip");
        Path out = tempDir.resolve("out");

        ZipUtil.zipWithAes(source, zip, "123456".toCharArray());

        assertTrue(ZipUtil.isEncryptedZip(zip));
        assertTrue(ZipUtil.checkPassword(zip, "123456".toCharArray()));
        assertFalse(ZipUtil.checkPassword(zip, "bad".toCharArray()));
        ZipUtil.unzipWithPassword(zip, out, "123456".toCharArray());
        assertEquals("secret", ZipUtilTestSupport.read(out.resolve("secret.txt")));
    }

    @Test
    void zipWithStandardEncryptionShouldCreateEncryptedZip() throws Exception {
        Path source = ZipUtilTestSupport.text(tempDir, "standard.txt", "standard");
        Path zip = tempDir.resolve("standard.zip");

        ZipUtil.zipWithStandardEncryption(source, zip, "abc".toCharArray());

        assertTrue(ZipUtil.requiresPassword(zip));
    }

    @Test
    void changePasswordShouldUseNewPassword() throws Exception {
        Path source = ZipUtilTestSupport.text(tempDir, "change.txt", "change");
        Path zip = tempDir.resolve("change.zip");
        ZipUtil.zipWithPassword(source, zip, "old".toCharArray());

        ZipUtil.changePassword(zip, "old".toCharArray(), "new".toCharArray());

        assertFalse(ZipUtil.checkPassword(zip, "old".toCharArray()));
        assertTrue(ZipUtil.checkPassword(zip, "new".toCharArray()));
    }

    @Test
    void passwordMethodsShouldRejectEmptyPassword() throws Exception {
        Path source = ZipUtilTestSupport.text(tempDir, "a.txt", "a");
        assertThrows(IllegalArgumentException.class, () -> ZipUtil.zipWithPassword(source, tempDir.resolve("a.zip"), new char[0]));
    }
}
