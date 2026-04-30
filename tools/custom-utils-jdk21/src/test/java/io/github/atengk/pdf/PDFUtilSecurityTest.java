package io.github.atengk.pdf;

import io.github.atengk.pdf.testsupport.PdfTestSupport;
import io.github.atengk.utils.pdf.PDFUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PDFUtilSecurityTest {

    @TempDir
    Path dir;

    @Test
    void encryptDecryptAndPasswordCheckShouldWork() throws Exception {
        Path source = PdfTestSupport.createPdf(dir, "source.pdf", "secret");
        Path encrypted = dir.resolve("encrypted.pdf");
        Path decrypted = dir.resolve("decrypted.pdf");

        PDFUtil.encrypt(source, encrypted, "123456");
        assertTrue(PDFUtil.isEncrypted(encrypted));
        assertTrue(PDFUtil.checkPassword(encrypted, "123456"));
        assertFalse(PDFUtil.checkPassword(encrypted, "wrong"));
        PDFUtil.decrypt(encrypted, decrypted, "123456");
        PdfTestSupport.assertPdfExists(decrypted);
    }

    @Test
    void protectMethodsShouldCreatePdf() throws Exception {
        Path source = PdfTestSupport.createPdf(dir, "source.pdf", "secret");
        Path protectedPdf = dir.resolve("protected.pdf");
        Path noPrint = dir.resolve("no-print.pdf");
        Path noCopy = dir.resolve("no-copy.pdf");
        Path noModify = dir.resolve("no-modify.pdf");

        PDFUtil.protect(source, protectedPdf, new PDFUtil.ProtectOptions("u", "o", true, false, false));
        PDFUtil.setPrintAllowed(source, noPrint, false);
        PDFUtil.setCopyAllowed(source, noCopy, false);
        PDFUtil.setModifyAllowed(source, noModify, false);

        assertTrue(PDFUtil.isEncrypted(protectedPdf));
        PdfTestSupport.assertPdfExists(noPrint);
        PdfTestSupport.assertPdfExists(noCopy);
        PdfTestSupport.assertPdfExists(noModify);
    }

    @Test
    void invalidSecurityArgumentsShouldThrow() {
        assertThrows(IllegalArgumentException.class, () -> new PDFUtil.EncryptOptions("", "owner", 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new PDFUtil.ProtectOptions("", "owner", true, true, true));
    }
}
