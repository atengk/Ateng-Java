package io.github.atengk.filetype;

import io.github.atengk.utils.filetype.FileTypeUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileTypeUtilHttpContentTypeTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldGetContentType() throws Exception {
        Path path = FileTypeTestSupport.writeFile(tempDir, "demo.pdf", FileTypeTestSupport.PDF_BYTES);

        assertEquals("application/pdf", FileTypeUtil.getContentType(path));
        assertEquals("application/pdf", FileTypeUtil.getContentType("demo.pdf", null));
        assertEquals("application/octet-stream", FileTypeUtil.getDefaultContentType());
    }

    @Test
    void shouldEvaluatePreviewAndInlineDisplay() {
        assertEquals("application/pdf", FileTypeUtil.getPreviewContentType("application/pdf"));
        assertEquals("application/octet-stream", FileTypeUtil.getPreviewContentType("application/zip"));
        assertTrue(FileTypeUtil.isPreviewable("application/pdf", "pdf"));
        assertTrue(FileTypeUtil.isInlineDisplayable("image/png"));
        assertFalse(FileTypeUtil.isInlineDisplayable("application/zip"));
        assertTrue(FileTypeUtil.isDownloadOnly("application/zip", "zip"));
    }

    @Test
    void shouldBuildSafeDownloadFileName() {
        assertEquals("demo.pdf", FileTypeUtil.getSafeDownloadFileName("../demo.pdf", "application/pdf"));
        assertEquals("download.pdf", FileTypeUtil.getSafeDownloadFileName("", "application/pdf"));
        assertEquals("demo.pdf", FileTypeUtil.getSafeDownloadFileName("demo", "application/pdf"));
        assertEquals("pdf", FileTypeUtil.getRecommendedExtension("application/pdf"));
    }

    @Test
    void shouldHandleUnknownContentTypeFallback() {
        assertEquals("application/custom", FileTypeUtil.getContentType("demo.unknown_ext", "application/custom"));
        assertEquals("application/octet-stream", FileTypeUtil.getContentType("demo.unknown_ext", null));
    }
}
