package io.github.atengk.diff;

import io.github.atengk.utils.diff.DiffUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileDiffUtilTest {
    @TempDir
    Path tempDir;

    @Test
    void shouldDiffFiles() throws Exception {
        Path oldFile = tempDir.resolve("old.txt");
        Path newFile = tempDir.resolve("new.txt");
        Files.writeString(oldFile, "a\nb", StandardCharsets.UTF_8);
        Files.writeString(newFile, "a\nc", StandardCharsets.UTF_8);
        assertTrue(DiffUtil.diffFile(oldFile, newFile).hasDiff());
        assertTrue(DiffUtil.diffFileLines(oldFile, newFile).hasDiff());
        assertTrue(DiffUtil.diffFileText(oldFile, newFile, StandardCharsets.UTF_8).hasDiff());
        assertTrue(DiffUtil.diffFileBytes(oldFile, newFile).hasDiff());
        assertTrue(DiffUtil.hasFileChanged(oldFile, newFile));
        assertTrue(DiffUtil.hasFileContentChanged(oldFile, newFile));
        assertTrue(DiffUtil.diffFileHash(oldFile, newFile).hasDiff());
    }

    @Test
    void shouldDiffDirectories() throws Exception {
        Path oldDir = tempDir.resolve("old");
        Path newDir = tempDir.resolve("new");
        Files.createDirectories(oldDir);
        Files.createDirectories(newDir);
        Files.writeString(oldDir.resolve("same.txt"), "same");
        Files.writeString(newDir.resolve("same.txt"), "same");
        Files.writeString(oldDir.resolve("remove.txt"), "old");
        Files.writeString(newDir.resolve("add.txt"), "new");
        Files.writeString(oldDir.resolve("mod.txt"), "old");
        Files.writeString(newDir.resolve("mod.txt"), "new");
        assertEquals(1, DiffUtil.getAddedFiles(oldDir, newDir).size());
        assertEquals(1, DiffUtil.getRemovedFiles(oldDir, newDir).size());
        assertEquals(1, DiffUtil.getModifiedFiles(oldDir, newDir).size());
        assertEquals(3, DiffUtil.diffDirectory(oldDir, newDir).items().size());
    }

    @Test
    void shouldValidateInvalidFile() {
        assertThrows(IllegalArgumentException.class, () -> DiffUtil.diffFile(tempDir.resolve("missing.txt"), tempDir.resolve("missing2.txt")));
    }
}
