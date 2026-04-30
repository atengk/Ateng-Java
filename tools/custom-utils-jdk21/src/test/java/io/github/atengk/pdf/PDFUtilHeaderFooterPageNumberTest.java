package io.github.atengk.pdf;

import io.github.atengk.pdf.testsupport.PdfTestSupport;
import io.github.atengk.utils.pdf.PDFUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PDFUtilHeaderFooterPageNumberTest {

    @TempDir
    Path dir;

    @Test
    void headerFooterAndPageNumberShouldCreatePdf() throws Exception {
        Path source = PdfTestSupport.createMultiPagePdf(dir, "source.pdf", 2);
        Path header = dir.resolve("header.pdf");
        Path footer = dir.resolve("footer.pdf");
        Path number = dir.resolve("number.pdf");
        Path both = dir.resolve("both.pdf");

        PDFUtil.addHeader(source, header, new PDFUtil.HeaderOptions("页眉", null, 820));
        PDFUtil.addFooter(header, footer, new PDFUtil.FooterOptions("页脚", null, 20));
        PDFUtil.addPageNumber(footer, number);
        PDFUtil.addHeaderFooter(number, both, new PDFUtil.HeaderOptions("H", null, 820), new PDFUtil.FooterOptions("F", null, 20));

        PdfTestSupport.assertPdfExists(both);
        assertEquals("第 1 / 2 页", PDFUtil.createPageNumberText(1, 2));
    }

    @Test
    void invalidPageNumberArgumentsShouldThrow() {
        assertThrows(IllegalArgumentException.class, () -> PDFUtil.createPageNumberText(0, 2));
        assertThrows(IllegalArgumentException.class, () -> PDFUtil.createPageNumberText(3, 2));
        assertThrows(IllegalArgumentException.class, () -> new PDFUtil.HeaderOptions("", null, 10));
    }
}
