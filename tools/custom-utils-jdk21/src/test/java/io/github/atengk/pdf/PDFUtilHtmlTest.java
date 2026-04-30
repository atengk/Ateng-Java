package io.github.atengk.pdf;

import io.github.atengk.pdf.testsupport.PdfTestSupport;
import io.github.atengk.utils.pdf.PDFUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PDFUtilHtmlTest {

    @TempDir
    Path dir;

    @Test
    void htmlMethodsShouldCreatePdf() throws Exception {
        String html = "<h1>Title</h1><p>Hello &amp; PDF</p>";
        Path htmlFile = PdfTestSupport.createHtml(dir, "index.html");
        Path pdf = dir.resolve("html.pdf");
        Path rendered = dir.resolve("rendered.pdf");
        Path filePdf = dir.resolve("file.pdf");
        Path urlPdf = dir.resolve("url.pdf");
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        PDFUtil.htmlToPdf(html, pdf);
        PDFUtil.htmlToPdf(html, out);
        PDFUtil.renderHtmlTemplate(html, rendered);
        assertTrue(PDFUtil.isPdf(PDFUtil.htmlToPdfBytes(html)));
        assertTrue(PDFUtil.isPdf(PDFUtil.renderHtmlTemplateToBytes(html)));
        PDFUtil.htmlFileToPdf(htmlFile, filePdf);
        PDFUtil.urlToPdf(htmlFile.toUri().toString(), urlPdf);

        PdfTestSupport.assertPdfExists(pdf);
        PdfTestSupport.assertPdfExists(rendered);
        PdfTestSupport.assertPdfExists(filePdf);
        PdfTestSupport.assertPdfExists(urlPdf);
        assertTrue(PDFUtil.isPdf(out.toByteArray()));
    }

    @Test
    void invalidHtmlArgumentsShouldThrow() {
        assertThrows(IllegalArgumentException.class, () -> PDFUtil.htmlToPdf(null, dir.resolve("x.pdf")));
        assertThrows(UnsupportedOperationException.class, () -> PDFUtil.urlToPdf("https://example.com", dir.resolve("x.pdf")));
    }
}
