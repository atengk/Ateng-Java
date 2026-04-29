package io.github.atengk.file;

import io.github.atengk.utils.FileUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileUtilTemporaryFileTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldWriteAndDeleteTempFile() throws Exception {
        Path tempFile = FileUtil.writeTempFile("fu-", ".tmp", new byte[]{1});
        assertTrue(Files.exists(tempFile));
        assertTrue(FileUtil.deleteTempFile(tempFile));
    }

    @Test
    void shouldHandleCacheFiles() throws Exception {
        Path cache = FileUtil.createCacheFile(tempDir.resolve("cache"), "a.txt");
        assertTrue(Files.exists(cache));
        FileUtil.cleanCacheDir(tempDir.resolve("cache"));
        assertTrue(FileUtil.isEmptyDir(tempDir.resolve("cache")));
    }

    @Test
    void shouldCleanExpiredTempFilesAndMoveTemp() throws Exception {
        Path old = Files.writeString(tempDir.resolve("old.tmp"), "a");
        FileUtil.setLastModifiedTime(old, Instant.now().minus(Duration.ofDays(2)));
        assertEquals(1, FileUtil.cleanExpiredTempFiles(tempDir, Duration.ofDays(1)));
        Path tempFile = Files.writeString(tempDir.resolve("new.tmp"), "x");
        Path target = tempDir.resolve("target.txt");
        FileUtil.moveTempToTarget(tempFile, target);
        assertTrue(Files.exists(target));
    }
}
