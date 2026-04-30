package io.github.atengk.pdf;

import io.github.atengk.pdf.testsupport.PdfTestSupport;
import io.github.atengk.utils.pdf.PDFUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PDFUtilRenderPreviewTest {

    @TempDir
    Path dir;

    @Test
    void previewShouldReturnEmptyListWithoutRenderer() {
        Path pdf = PdfTestSupport.createPdf(dir, "preview.pdf", "preview");
        assertTrue(PDFUtil.createPreview(pdf, dir.resolve("preview-dir")).isEmpty());
    }

    @Test
    void rendererMethodsShouldBeExplicitlyUnsupported() {
        Path pdf = PdfTestSupport.createPdf(dir, "preview.pdf", "preview");
        assertThrows(UnsupportedOperationException.class, () -> PDFUtil.renderPageToImage(pdf, 1, dir.resolve("1.png")));
        assertThrows(UnsupportedOperationException.class, () -> PDFUtil.renderFirstPageToImage(pdf, dir.resolve("first.png")));
        assertThrows(UnsupportedOperationException.class, () -> PDFUtil.renderPagesToImages(pdf, dir.resolve("pages")));
        assertThrows(UnsupportedOperationException.class, () -> PDFUtil.createThumbnail(pdf, dir.resolve("thumb.png")));
        assertThrows(UnsupportedOperationException.class, () -> PDFUtil.createPreviewImages(pdf, dir.resolve("preview"), 1));
    }
}
