package io.github.atengk.pdf;

import io.github.atengk.pdf.testsupport.PdfTestSupport;
import io.github.atengk.utils.pdf.PDFUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openpdf.text.Document;
import org.openpdf.text.pdf.PdfContentByte;
import org.openpdf.text.pdf.PdfWriter;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PDFUtilImageGraphicsTest {

    @TempDir
    Path dir;

    @Test
    void imageMethodsShouldCreatePdf() throws Exception {
        Path image = PdfTestSupport.createImage(dir, "image.png");
        Path pdf = dir.resolve("image.pdf");
        PDFUtil.writeToFile(pdf, document -> {
            PDFUtil.addImage(document, image);
            PDFUtil.addImage(document, PDFUtil.createQrCodeImage("hello"));
            PDFUtil.addImageFitWidth(document, image, 40);
            PDFUtil.addImageFitPage(document, image);
            PDFUtil.addLogo(document, image);
            PDFUtil.addSeal(document, image);
        });
        PdfTestSupport.assertPdfExists(pdf);
    }

    @Test
    void graphicsMethodsShouldDrawOnCanvas() throws Exception {
        Path pdf = dir.resolve("graphics.pdf");
        try (OutputStream out = Files.newOutputStream(pdf)) {
            Document document = PDFUtil.createDocument();
            PdfWriter writer = PdfWriter.getInstance(document, out);
            document.open();
            PdfContentByte canvas = writer.getDirectContent();
            PDFUtil.addLine(canvas, 20, 20, 120, 20);
            PDFUtil.addRectangle(canvas, 20, 40, 80, 30);
            PDFUtil.addCircle(canvas, 60, 100, 20);
            document.close();
        }
        PdfTestSupport.assertPdfExists(pdf);
    }

    @Test
    void invalidImageArgumentsShouldThrow() {
        assertThrows(IllegalArgumentException.class, () -> PDFUtil.createQrCodeImage(""));
        assertThrows(IllegalArgumentException.class, () -> PDFUtil.addImageFitWidth(PDFUtil.createDocument(), dir.resolve("none.png"), 10));
    }
}
