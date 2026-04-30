package io.github.atengk.resource;

import io.github.atengk.utils.ResourceUtil;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class ResourceUtilMetadataTest {

    @Test
    void readBasicMetadata() {
        Resource resource = ResourceUtil.classpath("sample.txt");
        assertEquals("sample.txt", ResourceUtil.getFilename(resource));
        assertEquals("txt", ResourceUtil.getExtension(resource));
        assertEquals("sample", ResourceUtil.getBaseName(resource));
        assertTrue(ResourceUtil.getContentLength(resource) > 0);
        assertNotNull(ResourceUtil.getDescription(resource));
        assertNotNull(ResourceUtil.getUrl(resource));
        assertNotNull(ResourceUtil.getUri(resource));
    }

    @Test
    void getDefaultFilenameAndContentType() {
        Resource resource = ResourceUtil.string("abc", StandardCharsets.UTF_8);
        assertEquals("default.txt", ResourceUtil.getFilename(resource, "default.txt"));
        assertEquals("application/octet-stream", ResourceUtil.getContentType(resource));
    }

    @Test
    void inferCharset() {
        Resource resource = ResourceUtil.bytes(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF, 'a'}, "bom.txt");
        assertEquals(StandardCharsets.UTF_8, ResourceUtil.getCharset(resource));
    }

    @Test
    void rejectMetadataForNullResource() {
        assertThrows(NullPointerException.class, () -> ResourceUtil.getDescription(null));
    }
}
