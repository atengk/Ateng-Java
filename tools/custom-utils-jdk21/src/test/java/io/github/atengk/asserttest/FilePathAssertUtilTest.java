package io.github.atengk.asserttest;

import io.github.atengk.utils.AssertUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FilePathAssertUtilTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldPassWhenFilePathRulesAreValid() throws Exception {
        Path file = tempDir.resolve("test.txt");
        Files.writeString(file, "hello");

        Path notExists = tempDir.resolve("not-exists.txt");

        assertSame(file, AssertUtil.exists(file, "路径必须存在"));
        assertSame(notExists, AssertUtil.notExists(notExists, "路径必须不存在"));
        assertSame(file, AssertUtil.isFile(file, "必须是文件"));
        assertSame(tempDir, AssertUtil.isDirectory(tempDir, "必须是目录"));
        assertSame(file, AssertUtil.readable(file, "必须可读"));
        assertSame(file, AssertUtil.writable(file, "必须可写"));
        assertSame(file, AssertUtil.fileSizeLessThan(file, 10, "文件过大"));
        assertSame(file, AssertUtil.fileSizeBetween(file, 1, 10, "文件大小不在范围内"));
    }

    @Test
    void shouldThrowWhenFilePathRulesAreInvalid() throws Exception {
        Path file = tempDir.resolve("test.txt");
        Files.writeString(file, "hello");

        Path notExists = tempDir.resolve("not-exists.txt");

        assertThrows(IllegalArgumentException.class, () -> AssertUtil.exists(notExists, "路径必须存在"));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.notExists(file, "路径必须不存在"));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.isFile(tempDir, "必须是文件"));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.isDirectory(file, "必须是目录"));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.fileSizeLessThan(file, 5, "文件过大"));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.fileSizeBetween(file, 6, 10, "文件大小不在范围内"));
    }

    @Test
    void shouldThrowWhenFileSizeArgumentsAreIllegal() throws Exception {
        Path file = tempDir.resolve("test.txt");
        Files.writeString(file, "hello");

        assertThrows(IllegalArgumentException.class, () -> AssertUtil.fileSizeLessThan(file, -1, "文件过大"));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.fileSizeBetween(file, -1, 10, "文件大小不在范围内"));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.fileSizeBetween(file, 10, 1, "文件大小不在范围内"));
    }

    @Test
    void shouldHandleReadableAndWritableChecks() throws Exception {
        Path file = tempDir.resolve("test.txt");
        Files.writeString(file, "hello");

        assertDoesNotThrow(() -> AssertUtil.readable(file, "必须可读"));
        assertDoesNotThrow(() -> AssertUtil.writable(file, "必须可写"));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.readable(null, "必须可读"));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.writable(null, "必须可写"));
    }
}
