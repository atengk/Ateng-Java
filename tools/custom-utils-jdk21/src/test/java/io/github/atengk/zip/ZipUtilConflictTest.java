package io.github.atengk.zip;

import io.github.atengk.exception.ZipUtilException;
import io.github.atengk.utils.ZipUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ZipUtilConflictTest {

    @TempDir
    Path tempDir;

    @Test
    void resolveConflictStrategiesShouldWork() throws Exception {
        Path file = ZipUtilTestSupport.text(tempDir, "a.txt", "old");

        assertThrows(ZipUtilException.class, () -> ZipUtil.resolveConflict(file, ZipUtil.ConflictStrategy.FAIL));
        assertNull(ZipUtil.skipIfExists(file));
        assertEquals(file, ZipUtil.overwriteIfExists(file));
        assertNotEquals(file, ZipUtil.renameIfExists(file));
    }

    @Test
    void backupIfExistsShouldMoveOldFile() throws Exception {
        Path file = ZipUtilTestSupport.text(tempDir, "backup.txt", "old");

        Path result = ZipUtil.backupIfExists(file);

        assertEquals(file, result);
        assertFalse(Files.exists(file));
        assertTrue(Files.list(tempDir).anyMatch(p -> p.getFileName().toString().startsWith("backup.txt.bak")));
    }

    @Test
    void unzipWithRenameStrategyShouldKeepExistingFile() throws Exception {
        Path source = ZipUtilTestSupport.text(tempDir, "a.txt", "new");
        Path zip = tempDir.resolve("conflict.zip");
        Path out = tempDir.resolve("out");
        ZipUtil.zip(source, zip);
        ZipUtilTestSupport.text(out, "a.txt", "old");

        ZipUtil.unzipWithConflictStrategy(zip, out, ZipUtil.ConflictStrategy.RENAME);

        assertTrue(Files.exists(out.resolve("a.txt")));
        assertTrue(Files.exists(out.resolve("a(1).txt")));
    }
}
