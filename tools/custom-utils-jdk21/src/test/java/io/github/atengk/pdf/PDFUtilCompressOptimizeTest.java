package io.github.atengk.pdf;

import io.github.atengk.pdf.testsupport.PdfTestSupport;
import io.github.atengk.utils.pdf.PDFUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PDFUtilCompressOptimizeTest {

    @TempDir
    Path dir;

    @Test
    void compressOptimizeShouldCreatePdf() throws Exception {
        Path source = PdfTestSupport.createPdf(dir, "source.pdf", "compress");
        Path compressed = dir.resolve("compressed.pdf");
        Path images = dir.resolve("images.pdf");
        Path unused = dir.resolve("unused.pdf");
        Path optimized = dir.resolve("optimized.pdf");

        PDFUtil.compress(source, compressed);
        PDFUtil.compressImages(compressed, images, 0.8f);
        PDFUtil.removeUnusedObjects(images, unused);
        PDFUtil.optimize(unused, optimized);

        PdfTestSupport.assertPdfExists(optimized);
        assertTrue(PDFUtil.getFileSize(optimized) > 0);
        assertEquals(0D, PDFUtil.estimateCompressRatio(source, new PDFUtil.CompressOptions(true, true)));
    }

    @Test
    void invalidCompressArgumentsShouldThrow() {
        Path source = PdfTestSupport.createPdf(dir, "source.pdf", "compress");
        assertThrows(IllegalArgumentException.class, () -> PDFUtil.compressImages(source, dir.resolve("bad.pdf"), 0));
        assertThrows(UnsupportedOperationException.class, () -> PDFUtil.linearize(source, dir.resolve("linear.pdf")));
    }
}
