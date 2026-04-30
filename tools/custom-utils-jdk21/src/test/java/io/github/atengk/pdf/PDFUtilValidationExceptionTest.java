package io.github.atengk.pdf;

import io.github.atengk.pdf.testsupport.PdfTestSupport;
import io.github.atengk.utils.pdf.PDFUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PDFUtilValidationExceptionTest {

    @TempDir
    Path dir;

    @Test
    void validationMethodsShouldPassForValidPdf() throws Exception {
        Path pdf = PdfTestSupport.createPdf(dir, "valid.pdf", "valid");
        PDFUtil.validatePdf(pdf);
        PDFUtil.validatePageRange(pdf, 1, 1);
        PDFUtil.validateOutputPath(dir.resolve("out.pdf"));
        PDFUtil.requirePdf(pdf);
        PDFUtil.requireExists(pdf);
        PDFUtil.requireReadable(pdf);
        PDFUtil.requireWritable(dir.resolve("writable.pdf"));
        assertNotNull(PDFUtil.wrapPdfException(new RuntimeException("x")));
    }

    @Test
    void validationMethodsShouldThrowForInvalidInput() throws Exception {
        Path txt = dir.resolve("a.txt");
        Files.writeString(txt, "not pdf");
        assertFalse(PDFUtil.isPdf(txt));
        assertThrows(IllegalArgumentException.class, () -> PDFUtil.validatePdf(txt));
        assertThrows(IllegalArgumentException.class, () -> PDFUtil.requireExists(dir.resolve("none.pdf")));
        assertThrows(IllegalArgumentException.class, () -> PDFUtil.validatePageRange(PdfTestSupport.createPdf(dir, "x.pdf", "x"), 1, 2));
    }
}
