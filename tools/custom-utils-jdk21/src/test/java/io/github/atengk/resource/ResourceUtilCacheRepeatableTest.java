package io.github.atengk.resource;

import io.github.atengk.utils.ResourceUtil;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class ResourceUtilCacheRepeatableTest {

    @Test
    void cacheResource() {
        Resource resource = ResourceUtil.bytes("abc".getBytes(StandardCharsets.UTF_8), "a.txt");
        Resource cached = ResourceUtil.cache(resource);
        assertEquals("abc", ResourceUtil.readString(cached));
        assertArrayEquals("abc".getBytes(StandardCharsets.UTF_8), ResourceUtil.toCachedBytes(resource));
        ResourceUtil.clearAllCache();
    }

    @Test
    void cacheIfNecessaryForOpenResource() {
        InputStreamResource source = new InputStreamResource(new ByteArrayInputStream("abc".getBytes(StandardCharsets.UTF_8)));
        assertFalse(ResourceUtil.isRepeatable(source));
        Resource cached = ResourceUtil.cacheIfNecessary(source);
        assertTrue(ResourceUtil.isRepeatable(cached));
        assertEquals("abc", ResourceUtil.readString(cached));
    }

    @Test
    void wrapRepeatableKeepsRepeatableResource() {
        Resource resource = ResourceUtil.bytes("abc".getBytes(StandardCharsets.UTF_8), "a.txt");
        assertSame(resource, ResourceUtil.wrapRepeatable(resource));
    }

    @Test
    void clearCacheByKey() {
        Resource resource = ResourceUtil.bytes("abc".getBytes(StandardCharsets.UTF_8), "a.txt");
        ResourceUtil.toCachedBytes(resource);
        assertDoesNotThrow(() -> ResourceUtil.clearCache(resource.getDescription()));
    }
}
