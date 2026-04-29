package io.github.atengk.file;

import io.github.atengk.utils.FileUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileUtilPathHandlingTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldHandleBasicPathOperations() throws Exception {
        Path path = tempDir.resolve("a/../b/test.txt");
        assertEquals("test.txt", FileUtil.getFileName(path));
        assertEquals("test", FileUtil.getBaseName(path));
        assertEquals("txt", FileUtil.getExtName(path));
        assertEquals("test.md", FileUtil.getFileName(FileUtil.changeExtName(path, "md")));
        assertEquals("test", FileUtil.getFileName(FileUtil.removeExtName(path)));
        assertTrue(FileUtil.toAbsolutePath(path).isAbsolute());
    }

    @Test
    void shouldJoinAndNormalizePath() {
        Path joined = FileUtil.joinPath(tempDir.toString(), "a", "..", "b");
        assertTrue(joined.toString().endsWith("b"));
        assertFalse(FileUtil.normalizePath("a/../b").contains(".."));
    }

    @Test
    void shouldRejectBlankPath() {
        assertThrows(IllegalArgumentException.class, () -> FileUtil.toPath(" "));
        assertThrows(NullPointerException.class, () -> FileUtil.toFile(null));
    }
}
