package io.github.atengk.resource;

import io.github.atengk.utils.ResourceUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResourceUtilPathTest {

    @Test
    void cleanAndNormalizePath() {
        assertEquals("a/c", ResourceUtil.cleanPath("a/b/../c"));
        assertEquals("a/c", ResourceUtil.normalizePath("a//b/../c"));
        assertEquals("a", ResourceUtil.getParentPath("a/b.txt"));
    }

    @Test
    void filenameAndExtensionOperations() {
        assertEquals("b.txt", ResourceUtil.getFilename("a/b.txt"));
        assertEquals("txt", ResourceUtil.getExtension("a/b.TXT"));
        assertEquals("a/b", ResourceUtil.removeExtension("a/b.txt"));
        assertEquals("a.md", ResourceUtil.changeExtension("a.txt", ".md"));
    }

    @Test
    void classpathPrefixAndProtocol() {
        assertEquals("classpath:sample.txt", ResourceUtil.appendClasspathPrefix("sample.txt"));
        assertEquals("sample.txt", ResourceUtil.removeClasspathPrefix("classpath:sample.txt"));
        assertTrue(ResourceUtil.hasProtocol("classpath:sample.txt"));
        assertEquals("classpath", ResourceUtil.getProtocol("classpath:sample.txt").orElseThrow());
    }

    @Test
    void joinAndRelativePath() {
        assertEquals("a/b/c", ResourceUtil.joinPath("/a/", "b", "c/"));
        assertEquals("b/c.txt", ResourceUtil.relativePath("/a", "/a/b/c.txt"));
    }

    @Test
    void rejectInvalidPathArguments() {
        assertThrows(IllegalArgumentException.class, () -> ResourceUtil.cleanPath(" "));
        assertThrows(IllegalArgumentException.class, () -> ResourceUtil.changeExtension("a.txt", " "));
    }
}
