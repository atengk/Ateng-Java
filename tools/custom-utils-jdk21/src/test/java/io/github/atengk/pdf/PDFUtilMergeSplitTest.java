package io.github.atengk.pdf;

import io.github.atengk.pdf.testsupport.PdfTestSupport;
import io.github.atengk.utils.pdf.PDFUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PDFUtilMergeSplitTest {

    @TempDir
    Path dir;

    @Test
    void mergeSplitExtractShouldCreatePdf() throws Exception {
        Path a = PdfTestSupport.createPdf(dir, "a.pdf", "a");
        Path b = PdfTestSupport.createPdf(dir, "b.pdf", "b");
        Path merged = dir.resolve("merged.pdf");
        Path appended = dir.resolve("appended.pdf");
        Path inserted = dir.resolve("inserted.pdf");
        Path range = dir.resolve("range.pdf");
        Path extracted = dir.resolve("extracted.pdf");
        Path splitDir = dir.resolve("split");

        PDFUtil.merge(List.of(a, b), merged);
        PDFUtil.append(a, b, appended);
        PDFUtil.insert(a, b, 1, inserted);
        PDFUtil.splitByRange(merged, range, 1, 1);
        PDFUtil.extractPages(merged, extracted, List.of(2));
        PDFUtil.split(merged, splitDir);

        assertEquals(2, PDFUtil.getPageCount(merged));
        assertTrue(Files.exists(splitDir.resolve("part-1.pdf")));
        PdfTestSupport.assertPdfExists(extracted);
    }

    @Test
    void mergeToBytesShouldReturnPdf() {
        Path a = PdfTestSupport.createPdf(dir, "a.pdf", "a");
        Path b = PdfTestSupport.createPdf(dir, "b.pdf", "b");
        assertTrue(PDFUtil.isPdf(PDFUtil.mergeToBytes(List.of(a, b))));
    }

    @Test
    void invalidMergeSplitArgumentsShouldThrow() {
        assertThrows(IllegalArgumentException.class, () -> PDFUtil.merge(List.of(), dir.resolve("x.pdf")));
        assertThrows(IllegalArgumentException.class, () -> PDFUtil.split(PdfTestSupport.createPdf(dir, "a.pdf", "a"), dir.resolve("out"), 0));
    }
}
