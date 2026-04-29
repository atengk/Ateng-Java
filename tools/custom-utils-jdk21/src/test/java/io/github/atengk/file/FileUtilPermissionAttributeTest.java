package io.github.atengk.file;

import io.github.atengk.utils.FileUtil;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class FileUtilPermissionAttributeTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldReadAttributesAndOwner() throws Exception {
        Path file = Files.writeString(tempDir.resolve("attr.txt"), "a");
        assertNotNull(FileUtil.getAttributes(file));
        assertNotNull(FileUtil.getOwner(file));
        assertTrue(FileUtil.setReadable(file, true));
        assertTrue(FileUtil.setWritable(file, true));
    }

    @Test
    void shouldHandlePosixPermissionsWhenSupported() throws Exception {
        Path file = Files.writeString(tempDir.resolve("posix.txt"), "a");
        Assumptions.assumeTrue(Files.getFileAttributeView(file, PosixFileAttributeView.class) != null);
        Set<PosixFilePermission> permissions = FileUtil.getPermissions(file);
        assertFalse(permissions.isEmpty());
        assertEquals(file, FileUtil.setPermissions(file, permissions));
    }

    @Test
    void shouldSetLastModifiedAttribute() throws Exception {
        Path file = Files.writeString(tempDir.resolve("basic.txt"), "a");
        FileUtil.setAttribute(file, "basic:lastModifiedTime", java.nio.file.attribute.FileTime.fromMillis(0));
        assertEquals(0, Files.getLastModifiedTime(file).toMillis());
    }
}
