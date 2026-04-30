package io.github.atengk.resource;

import io.github.atengk.utils.ResourceUtil;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;

import static org.junit.jupiter.api.Assertions.*;

class ResourceUtilStatusTest {

    @Test
    void checkReadableStatus() {
        Resource resource = ResourceUtil.classpath("sample.txt");
        assertTrue(ResourceUtil.exists(resource));
        assertFalse(ResourceUtil.notExists(resource));
        assertTrue(ResourceUtil.isReadable(resource));
        assertFalse(ResourceUtil.isOpen(resource));
        assertTrue(ResourceUtil.hasFilename(resource));
    }

    @Test
    void checkEmptyStatus() {
        assertTrue(ResourceUtil.isEmpty(ResourceUtil.classpath("empty.txt")));
        assertTrue(ResourceUtil.isNotEmpty(ResourceUtil.classpath("sample.txt")));
    }

    @Test
    void requireMethodsThrowWhenInvalid() {
        assertThrows(NullPointerException.class, () -> ResourceUtil.requireExists(null));
        assertThrows(ResourceUtil.ResourceOperationException.class, () -> ResourceUtil.requireReadable(ResourceUtil.classpath("missing.txt")));
    }

    @Test
    void compareSameResource() {
        Resource a = ResourceUtil.classpath("sample.txt");
        Resource b = ResourceUtil.classpath("sample.txt");
        assertTrue(ResourceUtil.isSame(a, b));
    }
}
