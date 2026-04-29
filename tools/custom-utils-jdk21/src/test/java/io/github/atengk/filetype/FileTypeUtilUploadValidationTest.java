package io.github.atengk.filetype;

import io.github.atengk.utils.filetype.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class FileTypeUtilUploadValidationTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldPassImageUploadPolicy() {
        FileTypeCheckResult result = FileTypeUtil.checkUploadType(
                new ByteArrayInputStream(FileTypeTestSupport.PNG_BYTES),
                "demo.png",
                FileTypeUtil.buildImagePolicy()
        );

        assertTrue(result.isPassed());
        assertEquals(FileTypeCategory.IMAGE, result.getFileTypeInfo().getCategory());
    }

    @Test
    void shouldFailWhenMimeTypeNotAllowed() {
        FileTypeCheckResult result = FileTypeUtil.checkUploadType(
                new ByteArrayInputStream(FileTypeTestSupport.PDF_BYTES),
                "demo.pdf",
                FileTypeUtil.buildImagePolicy()
        );

        assertFalse(result.isPassed());
        assertFalse(result.getMessages().isEmpty());
    }

    @Test
    void shouldFailWhenExtensionDoesNotMatchDetectedMimeType() {
        FileTypePolicy policy = FileTypePolicy.builder()
                .allowMimeTypes(Set.of("application/pdf"))
                .allowExtensions(Set.of("pdf", "png"))
                .checkExtensionMatch(true)
                .build();

        FileTypeCheckResult result = FileTypeUtil.checkUploadType(
                new ByteArrayInputStream(FileTypeTestSupport.PDF_BYTES),
                "demo.png",
                policy
        );

        assertFalse(result.isPassed());
        assertTrue(result.getMessages().stream().anyMatch(message -> message.contains("不匹配")));
    }

    @Test
    void shouldAssertUploadType() {
        assertDoesNotThrow(() -> FileTypeUtil.assertUploadType(
                new ByteArrayInputStream(FileTypeTestSupport.PNG_BYTES),
                "demo.png",
                FileTypeUtil.buildImagePolicy()
        ));

        assertThrows(UnsupportedFileTypeException.class, () -> FileTypeUtil.assertUploadType(
                new ByteArrayInputStream(FileTypeTestSupport.PDF_BYTES),
                "demo.pdf",
                FileTypeUtil.buildImagePolicy()
        ));
    }

    @Test
    void shouldValidateLocalFileType() throws Exception {
        Path path = FileTypeTestSupport.writeFile(tempDir, "demo.pdf", FileTypeTestSupport.PDF_BYTES);
        FileTypeCheckResult result = FileTypeUtil.validateFileType(path, FileTypeUtil.buildDocumentPolicy());

        assertTrue(result.isPassed());
    }

    @Test
    void shouldCheckAllowAndDenyHelpers() {
        assertTrue(FileTypeUtil.isAllowedMimeType("image/png", Set.of("image/*")));
        assertTrue(FileTypeUtil.isDeniedMimeType("application/x-msdownload", Set.of("application/x-msdownload")));
        assertTrue(FileTypeUtil.isAllowedExtension("pdf", Set.of("pdf")));
        assertTrue(FileTypeUtil.isDeniedExtension("exe", Set.of("exe")));
        assertTrue(FileTypeUtil.isAllowedCategory(FileTypeCategory.IMAGE, Set.of(FileTypeCategory.IMAGE)));
    }

    @Test
    void shouldCreatePresetPolicies() {
        assertNotNull(FileTypeUtil.buildImagePolicy());
        assertNotNull(FileTypeUtil.buildDocumentPolicy());
        assertNotNull(FileTypeUtil.buildMediaPolicy());
        assertNotNull(FileTypeUtil.buildArchivePolicy());
    }
}
