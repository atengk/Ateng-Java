package io.github.atengk.resource;

import io.github.atengk.utils.ResourceUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ResourceUtilConvertTest {

    @Test
    void convertToCommonTypes(@TempDir Path tempDir) throws Exception {
        Path path = tempDir.resolve("a.txt");
        Files.writeString(path, "abc", StandardCharsets.UTF_8);
        Resource resource = ResourceUtil.file(path);
        assertArrayEquals("abc".getBytes(StandardCharsets.UTF_8), ResourceUtil.toBytes(resource));
        assertEquals("abc", ResourceUtil.toString(resource, StandardCharsets.UTF_8));
        assertEquals(path.toFile(), ResourceUtil.toFile(resource));
        assertEquals(path, ResourceUtil.toPath(resource));
        assertNotNull(ResourceUtil.toUrl(resource));
        assertNotNull(ResourceUtil.toUri(resource));
    }

    @Test
    void convertToResourceWrappers() {
        Resource source = ResourceUtil.bytes("abc".getBytes(StandardCharsets.UTF_8), "a.txt");
        ByteArrayResource byteArrayResource = ResourceUtil.toByteArrayResource(source);
        InputStreamResource inputStreamResource = ResourceUtil.toInputStreamResource(source);
        assertEquals("a.txt", byteArrayResource.getFilename());
        assertTrue(inputStreamResource.isOpen());
    }

    @Test
    void convertToFileSystemResource(@TempDir Path tempDir) throws Exception {
        Path path = tempDir.resolve("a.txt");
        Files.writeString(path, "abc", StandardCharsets.UTF_8);
        FileSystemResource resource = ResourceUtil.toFileSystemResource(ResourceUtil.file(path));
        assertTrue(resource.exists());
    }

    @Test
    void createMultipartAndHttpEntity() {
        Resource resource = ResourceUtil.bytes("abc".getBytes(StandardCharsets.UTF_8), "a.txt");
        ResourceUtil.ResourceMultipartFile multipartFile = ResourceUtil.toMultipartFile(resource);
        ResourceUtil.ResourceHttpResponse response = ResourceUtil.toHttpEntity(resource);
        assertEquals("a.txt", multipartFile.getOriginalFilename());
        assertEquals(3, multipartFile.getSize());
        assertEquals(200, response.getStatus());
    }

    @Test
    void tryToFileReturnsEmptyForMemoryResource() {
        assertTrue(ResourceUtil.tryToFile(ResourceUtil.string("abc", StandardCharsets.UTF_8)).isEmpty());
    }
}
