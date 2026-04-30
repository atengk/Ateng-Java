package io.github.atengk.pdf;

import io.github.atengk.pdf.testsupport.PdfTestSupport;
import io.github.atengk.utils.pdf.PDFUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PDFUtilWatermarkTest {

    @TempDir
    Path dir;

    @Test
    void watermarkMethodsShouldCreatePdf() throws Exception {
        Path source = PdfTestSupport.createPdf(dir, "source.pdf", "source");
        Path text = dir.resolve("text-watermark.pdf");
        Path tiled = dir.resolve("tiled-watermark.pdf");
        Path page = dir.resolve("page-watermark.pdf");
        Path image = PdfTestSupport.createImage(dir, "wm.png");
        Path imageWatermark = dir.resolve("image-watermark.pdf");
        Path removed = dir.resolve("removed.pdf");

        PDFUtil.addTextWatermark(source, text, "水印");
        PDFUtil.addTiledWatermark(text, tiled, "平铺");
        PDFUtil.addWatermarkToPage(tiled, page, 1, new PDFUtil.WatermarkOptions("指定页", null, null, 0.2f, 30, null, null, false));
        PDFUtil.addImageWatermark(page, imageWatermark, image);
        PDFUtil.removeWatermark(imageWatermark, removed);

        PdfTestSupport.assertPdfExists(removed);
    }

    @Test
    void invalidWatermarkShouldThrow() {
        assertNotNull(PDFUtil.createWatermarkOptions());
        assertThrows(IllegalArgumentException.class, () -> new PDFUtil.WatermarkOptions(null, null, null, 0.5f, 0, null, null, false));
        assertThrows(IllegalArgumentException.class, () -> new PDFUtil.WatermarkOptions("x", null, null, 2f, 0, null, null, false));
    }
}
