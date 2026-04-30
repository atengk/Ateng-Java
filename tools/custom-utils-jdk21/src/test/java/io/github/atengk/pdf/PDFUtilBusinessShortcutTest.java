package io.github.atengk.pdf;

import io.github.atengk.pdf.testsupport.PdfTestSupport;
import io.github.atengk.utils.pdf.PDFUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PDFUtilBusinessShortcutTest {

    @TempDir
    Path dir;

    @Test
    void shortcutMethodsShouldCreatePdf() throws Exception {
        Path simple = dir.resolve("simple.pdf");
        Path text = dir.resolve("text.pdf");
        Path table = dir.resolve("table.pdf");
        Path image = PdfTestSupport.createImage(dir, "image.png");
        Path imagePdf = dir.resolve("image.pdf");
        Path watermarked = dir.resolve("watermarked.pdf");
        Path protectedPdf = dir.resolve("protected.pdf");
        Path merged = dir.resolve("merged.pdf");
        Path stamped = dir.resolve("stamped.pdf");

        PDFUtil.createSimplePdf("标题", "内容", simple);
        PDFUtil.createTextPdf(List.of("a", "b"), text);
        PDFUtil.createTablePdf("表格", List.of("编号", "姓名"), PdfTestSupport.rows(), table);
        PDFUtil.createImagePdf(List.of(image), imagePdf);
        PDFUtil.createWatermarkedPdf(simple, watermarked, "水印");
        PDFUtil.createProtectedPdf(simple, protectedPdf, "123456");
        PDFUtil.createMergedPdf(List.of(simple, text), merged);
        PDFUtil.createStampedPdf(simple, stamped, new PDFUtil.SealOptions(image, 1, 40, 40, 30, 30));

        PdfTestSupport.assertPdfExists(simple);
        PdfTestSupport.assertPdfExists(text);
        PdfTestSupport.assertPdfExists(table);
        PdfTestSupport.assertPdfExists(imagePdf);
        PdfTestSupport.assertPdfExists(watermarked);
        PdfTestSupport.assertPdfExists(protectedPdf);
        PdfTestSupport.assertPdfExists(merged);
        PdfTestSupport.assertPdfExists(stamped);
    }

    @Test
    void shortcutMethodsShouldRejectInvalidArguments() {
        assertThrows(IllegalArgumentException.class, () -> PDFUtil.createSimplePdf("", "x", dir.resolve("x.pdf")));
        assertThrows(IllegalArgumentException.class, () -> PDFUtil.createTextPdf(List.of(), dir.resolve("x.pdf")));
        assertThrows(IllegalArgumentException.class, () -> PDFUtil.createImagePdf(List.of(), dir.resolve("x.pdf")));
    }
}
