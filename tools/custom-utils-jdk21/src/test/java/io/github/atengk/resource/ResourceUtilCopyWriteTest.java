package io.github.atengk.resource;

import io.github.atengk.utils.ResourceUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ResourceUtilCopyWriteTest {

    @Test
    void copyToOutputStreamAndPath(@TempDir Path tempDir) throws Exception {
        Resource resource = ResourceUtil.string("abc", StandardCharsets.UTF_8);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        assertEquals(3, ResourceUtil.copy(resource, outputStream));
        assertEquals("abc", outputStream.toString(StandardCharsets.UTF_8));

        Path target = tempDir.resolve("out/a.txt");
        ResourceUtil.copy(resource, target);
        assertEquals("abc", Files.readString(target, StandardCharsets.UTF_8));
    }

    @Test
    void saveAsAndCopyAll(@TempDir Path tempDir) throws Exception {
        Resource a = ResourceUtil.bytes("a".getBytes(StandardCharsets.UTF_8), "a.txt");
        Resource b = ResourceUtil.bytes("b".getBytes(StandardCharsets.UTF_8), "b.txt");
        Path saved = ResourceUtil.saveAs(a, tempDir, "copy.txt");
        List<Path> paths = ResourceUtil.copyAll(List.of(a, b), tempDir.resolve("batch"));
        assertTrue(Files.exists(saved));
        assertEquals(2, paths.size());
    }

    @Test
    void writeStringAndExtractClasspath(@TempDir Path tempDir) throws Exception {
        Path text = tempDir.resolve("text.txt");
        Path extracted = tempDir.resolve("sample.txt");
        ResourceUtil.writeString(ResourceUtil.classpath("sample.txt"), text, StandardCharsets.UTF_8);
        ResourceUtil.extractClasspath("sample.txt", extracted);
        assertTrue(Files.readString(text, StandardCharsets.UTF_8).contains("hello"));
        assertTrue(Files.exists(extracted));
    }

    @Test
    void rejectUnsafeSaveAsName(@TempDir Path tempDir) {
        Resource resource = ResourceUtil.string("abc", StandardCharsets.UTF_8);
        assertThrows(ResourceUtil.ResourceOperationException.class, () -> ResourceUtil.saveAs(resource, tempDir, "../a.txt"));
    }
}
