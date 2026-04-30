package io.github.atengk.resource;

import io.github.atengk.utils.ResourceUtil;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class ResourceUtilValidationTest {

    @Test
    void checkValidResource() {
        Resource resource = ResourceUtil.bytes("abc".getBytes(StandardCharsets.UTF_8), "a.txt");
        assertDoesNotThrow(() -> ResourceUtil.checkExists(resource));
        assertDoesNotThrow(() -> ResourceUtil.checkReadable(resource));
        assertDoesNotThrow(() -> ResourceUtil.checkNotEmpty(resource));
        assertDoesNotThrow(() -> ResourceUtil.checkMaxSize(resource, 3));
        assertDoesNotThrow(() -> ResourceUtil.checkExtension(resource, "txt"));
        assertDoesNotThrow(() -> ResourceUtil.checkFilename(resource));
        assertDoesNotThrow(() -> ResourceUtil.validate(resource, item -> ResourceUtil.checkExtension(item, "txt")));
        assertSame(resource, ResourceUtil.require(resource, ResourceUtil::isReadable, "必须可读"));
    }

    @Test
    void checkInvalidResource() {
        Resource resource = ResourceUtil.bytes("abc".getBytes(StandardCharsets.UTF_8), "a.txt");
        assertThrows(ResourceUtil.ResourceOperationException.class, () -> ResourceUtil.checkMaxSize(resource, 2));
        assertThrows(ResourceUtil.ResourceOperationException.class, () -> ResourceUtil.checkExtension(resource, "json"));
        assertThrows(ResourceUtil.ResourceOperationException.class, () -> ResourceUtil.require(resource, item -> false, "失败"));
    }

    @Test
    void checkContentType() {
        Resource resource = ResourceUtil.bytes("abc".getBytes(StandardCharsets.UTF_8), "a.txt");
        assertDoesNotThrow(() -> ResourceUtil.checkContentType(resource, "text/plain"));
        assertThrows(ResourceUtil.ResourceOperationException.class, () -> ResourceUtil.checkContentType(resource, "application/json"));
    }
}
