package io.github.atengk.filetype;

import io.github.atengk.utils.filetype.FileTypeCategory;
import io.github.atengk.utils.filetype.FileTypeInfo;
import io.github.atengk.utils.filetype.FileTypeUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileTypeUtilInfoTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldDetectInfoByPathFileAndInputStream() throws Exception {
        Path path = FileTypeTestSupport.writeFile(tempDir, "demo.pdf", FileTypeTestSupport.PDF_BYTES);

        FileTypeInfo pathInfo = FileTypeUtil.detectInfo(path);
        FileTypeInfo fileInfo = FileTypeUtil.detectInfo(path.toFile());
        FileTypeInfo streamInfo = FileTypeUtil.detectInfo(new ByteArrayInputStream(FileTypeTestSupport.PDF_BYTES), "demo.pdf");

        assertEquals("application/pdf", pathInfo.getMimeType());
        assertEquals("application/pdf", fileInfo.getMimeType());
        assertEquals("application/pdf", streamInfo.getMimeType());
        assertEquals(FileTypeCategory.PDF, streamInfo.getCategory());
    }

    @Test
    void shouldBuildFileTypeInfoFromExistingValues() {
        FileTypeInfo info = FileTypeUtil.toFileTypeInfo("demo.pdf", "application/pdf");

        assertEquals("demo.pdf", info.getFileName());
        assertEquals("pdf", info.getExtension());
        assertEquals("application/pdf", info.getMimeType());
        assertEquals("application/pdf", info.getMediaType());
        assertEquals(FileTypeCategory.PDF, info.getCategory());
        assertTrue(info.isKnown());
        assertTrue(info.isBinary());
        assertTrue(info.isSafe());
        assertTrue(info.isExtensionMatched());
        assertFalse(info.isDangerous());
        assertTrue(info.getDescription().contains("application/pdf"));
    }

    @Test
    void shouldDescribeUnknownAndDisplayCategory() {
        assertEquals("未知", FileTypeUtil.getDescription("application/octet-stream", "bin"));
        assertEquals("PDF", FileTypeUtil.getDisplayName(FileTypeCategory.PDF));
    }

    @Test
    void shouldRejectInvalidInfoArguments() {
        assertThrows(IllegalArgumentException.class, () -> FileTypeUtil.detectInfo((Path) null));
        assertThrows(IllegalArgumentException.class, () -> FileTypeUtil.detectInfo((java.io.File) null));
        assertThrows(IllegalArgumentException.class, () -> FileTypeUtil.detectInfo(null, "demo.pdf"));
    }
}
