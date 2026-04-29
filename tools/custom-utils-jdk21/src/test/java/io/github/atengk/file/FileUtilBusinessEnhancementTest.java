package io.github.atengk.file;

import io.github.atengk.utils.FileUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FileUtilBusinessEnhancementTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldReadAndCopyClasspathResource() throws Exception {
        assertEquals("resource-content\n", FileUtil.readClasspathFile("sample-resource.txt", StandardCharsets.UTF_8));
        Path target = tempDir.resolve("resource.txt");
        FileUtil.copyResourceToFile("sample-resource.txt", target);
        assertEquals("resource-content\n", Files.readString(target));
        assertTrue(Files.exists(FileUtil.getClasspathPath("sample-resource.txt")));
    }

    @Test
    void shouldSerializeAndDeserializeObject() throws Exception {
        Path file = tempDir.resolve("object.bin");
        SampleData data = new SampleData("Ateng", 1);
        FileUtil.writeObject(file, data);
        SampleData read = FileUtil.readObject(file, SampleData.class);
        assertEquals(data.name, read.name);
        assertEquals(data.version, read.version);
    }

    @Test
    void shouldSplitMergeTailAndDateDir() throws Exception {
        Path source = Files.writeString(tempDir.resolve("big.txt"), "line1\nline2\nline3");
        List<Path> parts = FileUtil.splitFile(source, tempDir.resolve("parts"), 5);
        assertTrue(parts.size() >= 2);
        Path merged = tempDir.resolve("merged.txt");
        FileUtil.mergeFiles(parts, merged);
        assertTrue(FileUtil.sameContent(source, merged));
        assertEquals(List.of("line2", "line3"), FileUtil.tail(source, 2, StandardCharsets.UTF_8));
        assertTrue(Files.isDirectory(FileUtil.createDateDir(tempDir.resolve("date"))));
    }

    @Test
    void shouldWaitForFileAndClearExpiredFiles() throws Exception {
        Path file = tempDir.resolve("wait.txt");
        assertFalse(FileUtil.waitForFile(file, Duration.ofMillis(20), Duration.ofMillis(5)));
        Files.writeString(file, "a");
        assertTrue(FileUtil.waitForFile(file, Duration.ofMillis(20), Duration.ofMillis(5)));
        FileUtil.setLastModifiedTime(file, Instant.now().minus(Duration.ofDays(2)));
        assertEquals(1, FileUtil.clearExpiredFiles(tempDir, Duration.ofDays(1)));
    }

    private record SampleData(String name, int version) implements Serializable {
    }
}
