package io.github.atengk.pdf;

import io.github.atengk.pdf.testsupport.PdfTestSupport;
import io.github.atengk.utils.pdf.PDFUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PDFUtilFormTest {

    @TempDir
    Path dir;

    @Test
    void formMethodsShouldHandleNormalPdf() throws Exception {
        Path source = PdfTestSupport.createPdf(dir, "form-source.pdf", "form");
        Path filled = dir.resolve("filled.pdf");
        Path flat = dir.resolve("flat.pdf");

        assertFalse(PDFUtil.hasForm(source));
        assertTrue(PDFUtil.readFormFields(source).isEmpty());
        assertTrue(PDFUtil.getFormFieldNames(source).isEmpty());
        PDFUtil.fillForm(source, filled, Map.of("name", "Ateng"));
        PDFUtil.flattenForm(source, flat);

        PdfTestSupport.assertPdfExists(filled);
        PdfTestSupport.assertPdfExists(flat);
    }

    @Test
    void unsupportedFormOperationsShouldThrow() {
        Path source = PdfTestSupport.createPdf(dir, "form-source.pdf", "form");
        assertThrows(UnsupportedOperationException.class, () -> PDFUtil.renameField(source, dir.resolve("rename.pdf"), "a", "b"));
        assertThrows(UnsupportedOperationException.class, () -> PDFUtil.removeField(source, dir.resolve("remove.pdf"), "a"));
        assertThrows(IllegalArgumentException.class, () -> PDFUtil.fillForm(source, dir.resolve("bad.pdf"), null));
    }
}
