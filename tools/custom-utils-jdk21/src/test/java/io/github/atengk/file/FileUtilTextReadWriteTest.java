package io.github.atengk.file;

import io.github.atengk.utils.FileUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FileUtilTextReadWriteTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldWriteReadAndAppendText() throws Exception {
        Path file = tempDir.resolve("text.txt");
        FileUtil.writeUtf8String(file, "line1\nline2");
        FileUtil.appendString(file, "\nline3", StandardCharsets.UTF_8);
        assertEquals(List.of("line1", "line2", "line3"), FileUtil.readUtf8Lines(file));
        assertEquals("line1", FileUtil.readFirstLine(file, StandardCharsets.UTF_8).orElseThrow());
        assertEquals(List.of("line2", "line3"), FileUtil.readLastLines(file, 2, StandardCharsets.UTF_8));
    }

    @Test
    void shouldReplaceText() throws Exception {
        Path file = tempDir.resolve("replace.txt");
        FileUtil.writeUtf8String(file, "hello java");
        FileUtil.replaceText(file, "java", "jdk", StandardCharsets.UTF_8);
        assertEquals("hello jdk", FileUtil.readUtf8String(file));
    }

    @Test
    void shouldRejectInvalidReadLastLines() {
        Path file = tempDir.resolve("missing.txt");
        assertThrows(IllegalArgumentException.class, () -> FileUtil.readLastLines(file, -1, StandardCharsets.UTF_8));
    }
}
