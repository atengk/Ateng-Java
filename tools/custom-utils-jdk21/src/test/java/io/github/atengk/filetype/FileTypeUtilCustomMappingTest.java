package io.github.atengk.filetype;

import io.github.atengk.utils.filetype.FileTypeUtil;
import org.apache.tika.config.TikaConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileTypeUtilCustomMappingTest {

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        FileTypeUtil.clearCustomMimeMappings();
    }

    @AfterEach
    void tearDown() {
        FileTypeUtil.clearCustomMimeMappings();
    }

    @Test
    void shouldRegisterAndReadCustomMappings() {
        FileTypeUtil.registerCustomMimeMapping("biz", "application/x-biz");

        assertEquals("application/x-biz", FileTypeUtil.getCustomMimeTypeByExtension("biz"));
        assertEquals("biz", FileTypeUtil.getCustomExtensionByMimeType("application/x-biz"));
        assertTrue(FileTypeUtil.getSupportedCustomMimeTypes().contains("application/x-biz"));
        assertEquals("application/x-biz", FileTypeUtil.getMimeTypeByExtension("biz"));
        assertEquals("biz", FileTypeUtil.getExtensionByMimeType("application/x-biz"));
    }

    @Test
    void shouldClearCustomMappings() {
        FileTypeUtil.registerCustomMimeMapping("biz", "application/x-biz");
        FileTypeUtil.clearCustomMimeMappings();

        assertEquals("", FileTypeUtil.getCustomMimeTypeByExtension("biz"));
        assertEquals("", FileTypeUtil.getCustomExtensionByMimeType("application/x-biz"));
        assertTrue(FileTypeUtil.getSupportedCustomMimeTypes().isEmpty());
    }

    @Test
    void shouldRejectInvalidCustomMappingArguments() {
        assertThrows(IllegalArgumentException.class, () -> FileTypeUtil.registerCustomMimeMapping("", "application/x-biz"));
        assertThrows(IllegalArgumentException.class, () -> FileTypeUtil.registerCustomMimeMapping("biz", "invalid"));
    }

    @Test
    void shouldUseDefaultTikaConfigForDetection() throws Exception {
        Path path = FileTypeTestSupport.writeFile(tempDir, "demo.pdf", FileTypeTestSupport.PDF_BYTES);
        TikaConfig config = TikaConfig.getDefaultConfig();

        assertEquals("application/pdf", FileTypeUtil.detectMimeType(path, config));
        assertEquals("application/pdf", FileTypeUtil.detectMimeType(new ByteArrayInputStream(FileTypeTestSupport.PDF_BYTES), "demo.pdf", config));
    }

    @Test
    void shouldRejectInvalidTikaConfigArguments() {
        assertThrows(IllegalArgumentException.class, () -> FileTypeUtil.loadTikaConfig(null));
        assertThrows(IllegalArgumentException.class, () -> FileTypeUtil.loadTikaConfig(tempDir.resolve("missing.xml")));
        assertThrows(IllegalArgumentException.class, () -> FileTypeUtil.detectMimeType(tempDir.resolve("missing.pdf"), TikaConfig.getDefaultConfig()));
        assertThrows(IllegalArgumentException.class, () -> FileTypeUtil.detectMimeType(new ByteArrayInputStream(FileTypeTestSupport.PDF_BYTES), "demo.pdf", null));
    }
}
