package io.github.atengk.pdf;

import io.github.atengk.pdf.testsupport.PdfTestSupport;
import io.github.atengk.utils.pdf.PDFUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PDFUtilReadExtractTest {

    @TempDir
    Path dir;

    @Test
    void readAndExtractShouldWork() throws Exception {
        Path pdf = PdfTestSupport.createPdf(dir, "read.pdf", "hello extract");
        assertEquals(1, PDFUtil.readPageCount(pdf));
        assertNotNull(PDFUtil.readMetadata(pdf));
        assertNotNull(PDFUtil.readPageSize(pdf, 1));
        assertTrue(PDFUtil.extractText(pdf).contains("hello"));
        assertTrue(PDFUtil.extractText(pdf, 1).contains("hello"));
        assertTrue(PDFUtil.extractText(pdf, 1, 1).contains("hello"));
        assertTrue(PDFUtil.hasText(pdf));
        assertFalse(PDFUtil.isScannedPdf(pdf));
        assertTrue(PDFUtil.extractImages(pdf, dir.resolve("images")).isEmpty());
    }

    @Test
    void invalidReadArgumentsShouldThrow() {
        Path pdf = PdfTestSupport.createPdf(dir, "read.pdf", "hello");
        assertThrows(IllegalArgumentException.class, () -> PDFUtil.readPageSize(pdf, 2));
        assertThrows(IllegalArgumentException.class, () -> PDFUtil.extractText(pdf, 0));
    }
}
