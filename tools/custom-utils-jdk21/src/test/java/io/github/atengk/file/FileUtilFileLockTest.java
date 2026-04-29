package io.github.atengk.file;

import io.github.atengk.utils.FileUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileUtilFileLockTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldLockAndTryLock() throws Exception {
        Path lockFile = tempDir.resolve("test.lock");
        try (FileUtil.FileLockHandle handle = FileUtil.lock(lockFile)) {
            assertTrue(handle.isValid());
            assertTrue(FileUtil.isLocked(lockFile));
            assertTrue(FileUtil.tryLock(lockFile).isEmpty());
        }
        assertFalse(FileUtil.isLocked(lockFile));
    }

    @Test
    void shouldReadAndWriteWithLock() throws Exception {
        Path file = tempDir.resolve("locked.txt");
        FileUtil.writeWithLock(file, new byte[]{1, 2, 3});
        assertArrayEquals(new byte[]{1, 2, 3}, FileUtil.readWithLock(file));
    }

    @Test
    void shouldRunCallableWithLock() throws Exception {
        Path file = tempDir.resolve("callable.lock");
        String result = FileUtil.withLock(file, () -> "ok");
        assertEquals("ok", result);
    }
}
