package io.github.atengk.string;

import io.github.atengk.utils.StringUtil;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class StringUtilPathFileNameTest {

    @Test
    void testPathFileName() {
        assertEquals("demo.txt", StringUtil.getFileName("/tmp/demo.txt"));
        assertEquals("demo", StringUtil.getFileNameWithoutExtension("/tmp/demo.txt"));
        assertEquals("txt", StringUtil.getFileExtension("/tmp/demo.txt"));
        assertEquals("/tmp/demo.md", StringUtil.changeExtension("/tmp/demo.txt", "md"));
        assertEquals("/tmp/demo", StringUtil.changeExtension("/tmp/demo.txt", ""));
        assertEquals("/a/c", StringUtil.normalizePath("/a/b/../c"));
        assertEquals("/api", StringUtil.ensureStartSlash("api"));
        assertEquals("api/", StringUtil.ensureEndSlash("api"));
        assertEquals("/api", StringUtil.removeEndSlash("/api/"));
    }

}
