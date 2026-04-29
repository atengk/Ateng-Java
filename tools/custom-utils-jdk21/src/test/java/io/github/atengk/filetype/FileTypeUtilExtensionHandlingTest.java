package io.github.atengk.filetype;

import io.github.atengk.utils.filetype.FileTypeUtil;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FileTypeUtilExtensionHandlingTest {

    @Test
    void shouldReadAndNormalizeExtensions() {
        assertEquals("pdf", FileTypeUtil.getExtension("report.PDF"));
        assertEquals("gz", FileTypeUtil.getExtension(Path.of("/tmp/demo.tar.gz")));
        assertEquals("jpg", FileTypeUtil.normalizeExtension(".JPG"));
        assertTrue(FileTypeUtil.hasExtension("demo.txt"));
        assertFalse(FileTypeUtil.hasExtension("README"));
    }

    @Test
    void shouldHandleFileNameWithoutExtension() {
        assertEquals("/tmp/demo", FileTypeUtil.getFileNameWithoutExtension("/tmp/demo.pdf"));
        assertEquals("README", FileTypeUtil.getFileNameWithoutExtension("README"));
        assertEquals("", FileTypeUtil.getFileNameWithoutExtension(null));
    }

    @Test
    void shouldMapExtensionAndMimeType() {
        assertEquals("application/pdf", FileTypeUtil.getMimeTypeByExtension("pdf"));
        assertEquals("pdf", FileTypeUtil.getExtensionByMimeType("application/pdf"));
        assertTrue(FileTypeUtil.isExtensionMatchedMimeType("demo.pdf", "application/pdf"));
        assertTrue(FileTypeUtil.isExtensionMatchedMimeTypeByExtension(".png", "image/png"));
        assertFalse(FileTypeUtil.isExtensionMatchedMimeType("demo.jpg", "application/pdf"));
    }

    @Test
    void shouldHandleDoubleExtensions() {
        assertTrue(FileTypeUtil.isDoubleExtension("backup.tar.gz"));
        assertFalse(FileTypeUtil.isDoubleExtension("demo.pdf"));
        assertEquals("gz", FileTypeUtil.getLastExtension("backup.tar.gz"));
        assertEquals(List.of("tar", "gz"), FileTypeUtil.getAllExtensions("backup.tar.gz"));
    }

    @Test
    void shouldHandleExtensionBoundaryValues() {
        assertEquals("", FileTypeUtil.getExtension((String) null));
        assertEquals("", FileTypeUtil.getExtension(""));
        assertEquals("", FileTypeUtil.getExtension(".gitignore"));
        assertEquals("", FileTypeUtil.getExtension("demo."));
        assertFalse(FileTypeUtil.isExtensionMatchedMimeType(null, "application/pdf"));
    }
}
