package io.github.atengk.resource;

import io.github.atengk.utils.ResourceUtil;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ResourceUtilSecurityTest {

    @Test
    void validateSafePathAndFilename() {
        assertTrue(ResourceUtil.isSafePath("a/b.txt"));
        assertFalse(ResourceUtil.isSafePath("../secret.txt"));
        assertTrue(ResourceUtil.isPathTraversal("a/../secret.txt"));
        assertEquals("a_b.txt", ResourceUtil.cleanFilename("a/b.txt"));
        assertDoesNotThrow(() -> ResourceUtil.checkFilenameSafe("a.txt"));
        assertThrows(ResourceUtil.ResourceOperationException.class, () -> ResourceUtil.checkSafePath("../a.txt"));
    }

    @Test
    void extensionWhitelistAndBlacklist() {
        Resource resource = ResourceUtil.bytes("abc".getBytes(StandardCharsets.UTF_8), "a.txt");
        assertTrue(ResourceUtil.isAllowedExtension(resource, ".txt"));
        assertFalse(ResourceUtil.isAllowedExtension(resource, "json"));
        assertTrue(ResourceUtil.isDeniedExtension(resource, "txt"));
        assertDoesNotThrow(() -> ResourceUtil.checkAllowedExtension(resource, "txt"));
    }

    @Test
    void trustedUrl() {
        Resource resource = ResourceUtil.url("https://example.com/a.txt");
        assertDoesNotThrow(() -> ResourceUtil.checkTrustedUrl(resource, List.of("example.com")));
        assertThrows(ResourceUtil.ResourceOperationException.class, () -> ResourceUtil.checkTrustedUrl(resource, List.of("other.example")));
    }

    @Test
    void sanitizeLocation() {
        assertEquals("classpath:a/c.txt", ResourceUtil.sanitizeLocation(" classpath:a/b/../c.txt "));
    }
}
