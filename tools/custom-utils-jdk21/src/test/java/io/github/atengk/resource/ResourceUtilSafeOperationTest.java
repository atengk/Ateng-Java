package io.github.atengk.resource;

import io.github.atengk.utils.ResourceUtil;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class ResourceUtilSafeOperationTest {

    @Test
    void tryReadAndTryMetadata() {
        Resource resource = ResourceUtil.bytes("abc".getBytes(StandardCharsets.UTF_8), "a.txt");
        assertTrue(ResourceUtil.tryReadBytes(resource).isPresent());
        assertTrue(ResourceUtil.tryReadString(resource, StandardCharsets.UTF_8).isPresent());
        assertTrue(ResourceUtil.tryContentLength(resource).isPresent());
        assertTrue(ResourceUtil.tryLastModified(resource).isEmpty());
    }

    @Test
    void tryGetFileUrlUri() {
        Resource resource = ResourceUtil.classpath("sample.txt");
        assertTrue(ResourceUtil.tryGetFile(resource).isPresent());
        assertTrue(ResourceUtil.tryGetUrl(resource).isPresent());
        assertTrue(ResourceUtil.tryGetUri(resource).isPresent());
        assertTrue(ResourceUtil.tryGetFile(ResourceUtil.string("abc", StandardCharsets.UTF_8)).isEmpty());
    }

    @Test
    void sneakyAndExceptionFactory() {
        Resource resource = ResourceUtil.bytes("abc".getBytes(StandardCharsets.UTF_8), "a.txt");
        assertArrayEquals("abc".getBytes(StandardCharsets.UTF_8), ResourceUtil.sneakyReadBytes(resource));
        assertNotNull(ResourceUtil.wrapException(new IllegalStateException("x")));
        assertNotNull(ResourceUtil.resourceNotFound("x"));
        assertNotNull(ResourceUtil.resourceNotReadable(resource));
    }

    @Test
    void tryReadReturnsEmptyOnFailure() {
        assertTrue(ResourceUtil.tryReadBytes(ResourceUtil.classpath("missing.txt")).isEmpty());
        assertTrue(ResourceUtil.tryReadString(ResourceUtil.classpath("missing.txt"), StandardCharsets.UTF_8).isEmpty());
    }
}
