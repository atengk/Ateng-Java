package io.github.atengk.pdf;

import io.github.atengk.pdf.testsupport.PdfTestSupport;
import io.github.atengk.utils.pdf.PDFUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PDFUtilSignatureTest {

    @TempDir
    Path dir;

    @Test
    void imageSignatureAndSealShouldCreatePdf() throws Exception {
        Path source = PdfTestSupport.createPdf(dir, "source.pdf", "signature");
        Path image = PdfTestSupport.createImage(dir, "seal.png");
        Path seal = dir.resolve("seal.pdf");
        Path signImage = dir.resolve("sign-image.pdf");
        PDFUtil.SealOptions options = new PDFUtil.SealOptions(image, 1, 50, 50, 40, 40);

        PDFUtil.addSealImage(source, seal, options);
        PDFUtil.addSignatureImage(source, signImage, options);

        PdfTestSupport.assertPdfExists(seal);
        PdfTestSupport.assertPdfExists(signImage);
        assertFalse(PDFUtil.hasSignature(source));
        assertTrue(PDFUtil.getSignatures(source).isEmpty());
    }

    @Test
    void realDigitalSignatureShouldBeExplicitlyUnsupported() {
        Path source = PdfTestSupport.createPdf(dir, "source.pdf", "signature");
        assertThrows(UnsupportedOperationException.class, () -> PDFUtil.sign(source, dir.resolve("signed.pdf"), new PDFUtil.SignOptions("reason", "location")));
        assertThrows(UnsupportedOperationException.class, () -> PDFUtil.verifySignature(source));
    }
}
