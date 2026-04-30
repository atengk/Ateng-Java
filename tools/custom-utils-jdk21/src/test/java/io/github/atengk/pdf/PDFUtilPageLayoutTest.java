package io.github.atengk.pdf;

import io.github.atengk.pdf.testsupport.PdfTestSupport;
import io.github.atengk.utils.pdf.PDFUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openpdf.text.PageSize;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PDFUtilPageLayoutTest {

    @TempDir
    Path dir;

    @Test
    void createBlankPdfShouldCreateExpectedPages() throws Exception {
        Path pdf = dir.resolve("blank.pdf");
        PDFUtil.createBlankPdf(pdf, 2);
        PdfTestSupport.assertPdfExists(pdf);
        assertEquals(2, PDFUtil.getPageCount(pdf));
    }

    @Test
    void copyRemoveRotateResizeShouldCreatePdf() throws Exception {
        Path source = PdfTestSupport.createMultiPagePdf(dir, "source.pdf", 3);
        Path copy = dir.resolve("copy.pdf");
        Path removed = dir.resolve("removed.pdf");
        Path rotated = dir.resolve("rotated.pdf");
        Path resized = dir.resolve("resized.pdf");

        PDFUtil.copyPages(source, copy, 1, 2);
        PDFUtil.removePages(source, removed, List.of(2));
        PDFUtil.rotatePage(source, rotated, 1, 90);
        PDFUtil.resizePage(source, resized, new PDFUtil.PageOptions(PageSize.A5, 20, 20, 20, 20));

        assertEquals(2, PDFUtil.getPageCount(copy));
        assertEquals(2, PDFUtil.getPageCount(removed));
        PdfTestSupport.assertPdfExists(rotated);
        PdfTestSupport.assertPdfExists(resized);
    }

    @Test
    void invalidPageRangeShouldThrow() {
        Path source = PdfTestSupport.createMultiPagePdf(dir, "source.pdf", 2);
        assertThrows(IllegalArgumentException.class, () -> PDFUtil.copyPages(source, dir.resolve("bad.pdf"), 2, 1));
        assertThrows(IllegalArgumentException.class, () -> PDFUtil.rotatePage(source, dir.resolve("bad2.pdf"), 1, 45));
    }
}
