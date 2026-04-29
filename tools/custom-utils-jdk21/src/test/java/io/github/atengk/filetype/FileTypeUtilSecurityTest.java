package io.github.atengk.filetype;

import io.github.atengk.utils.filetype.DangerousFileTypeException;
import io.github.atengk.utils.filetype.FileTypeCheckResult;
import io.github.atengk.utils.filetype.FileTypeUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FileTypeUtilSecurityTest {

    @Test
    void shouldIdentifyDangerousTypes() {
        assertTrue(FileTypeUtil.isDangerousExtension("exe"));
        assertTrue(FileTypeUtil.isDangerousMimeType("application/x-msdownload"));
        assertTrue(FileTypeUtil.isDangerousFile("application/x-msdownload", "exe"));
        assertTrue(FileTypeUtil.isExecutableFile("application/x-msdownload", "exe"));
        assertTrue(FileTypeUtil.isScriptFile("application/x-sh", "sh"));
    }

    @Test
    void shouldIdentifyDisguisedFiles() {
        assertTrue(FileTypeUtil.isMimeSpoofing("demo.jpg", "application/pdf"));
        assertTrue(FileTypeUtil.isDisguisedFile("demo.jpg.exe", "image/jpeg"));
        assertTrue(FileTypeUtil.isDoubleExtensionDangerous("demo.pdf.exe"));
        assertTrue(FileTypeUtil.containsDangerousExtension("demo.pdf.exe"));
    }

    @Test
    void shouldPassSafeFileType() {
        FileTypeCheckResult result = FileTypeUtil.checkSafeFileType("demo.pdf", "application/pdf");

        assertTrue(result.isPassed());
        assertDoesNotThrow(() -> FileTypeUtil.assertSafeFileType("demo.pdf", "application/pdf"));
    }

    @Test
    void shouldRejectUnsafeFileType() {
        FileTypeCheckResult result = FileTypeUtil.checkSafeFileType("demo.pdf.exe", "application/pdf");

        assertFalse(result.isPassed());
        assertThrows(DangerousFileTypeException.class, () -> FileTypeUtil.assertSafeFileType("demo.pdf.exe", "application/pdf"));
    }

    @Test
    void shouldHandleSecurityBoundaryValues() {
        assertFalse(FileTypeUtil.isDangerousExtension(null));
        assertFalse(FileTypeUtil.isDangerousMimeType(null));
        assertFalse(FileTypeUtil.isMimeSpoofing(null, "application/pdf"));
        assertFalse(FileTypeUtil.isDoubleExtensionDangerous("demo.pdf"));
    }
}
