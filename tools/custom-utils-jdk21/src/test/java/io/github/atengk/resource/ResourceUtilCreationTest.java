package io.github.atengk.resource;

import io.github.atengk.utils.ResourceUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ResourceUtilCreationTest {

    @Test
    void createClasspathResource() {
        Resource resource = ResourceUtil.classpath("sample.txt");
        assertTrue(ResourceUtil.exists(resource));
        assertEquals("sample.txt", resource.getFilename());
    }

    @Test
    void createFileResource(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("demo.txt");
        Files.writeString(file, "demo", StandardCharsets.UTF_8);
        assertTrue(ResourceUtil.exists(ResourceUtil.file(file)));
        assertTrue(ResourceUtil.exists(ResourceUtil.file(file.toFile())));
        assertTrue(ResourceUtil.exists(ResourceUtil.file(file.toString())));
    }

    @Test
    void createMemoryResource() {
        Resource bytes = ResourceUtil.bytes("abc".getBytes(StandardCharsets.UTF_8), "a.txt");
        Resource string = ResourceUtil.string("中文", StandardCharsets.UTF_8);
        assertEquals("a.txt", bytes.getFilename());
        assertEquals("中文", ResourceUtil.readString(string));
    }

    @Test
    void createSupplierStreamResourceCanReadRepeatedly() {
        Resource resource = ResourceUtil.stream(() -> new ByteArrayInputStream("abc".getBytes(StandardCharsets.UTF_8)), "a.txt");
        assertEquals("abc", ResourceUtil.readString(resource));
        assertEquals("abc", ResourceUtil.readString(resource));
    }

    @Test
    void rejectInvalidCreationArguments() {
        assertThrows(IllegalArgumentException.class, () -> ResourceUtil.of(" "));
        assertThrows(NullPointerException.class, () -> ResourceUtil.bytes(null));
        assertThrows(ResourceUtil.ResourceOperationException.class, () -> ResourceUtil.url("not a url"));
    }
}
