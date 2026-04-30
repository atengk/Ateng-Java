package io.github.atengk.pdf;

import io.github.atengk.pdf.testsupport.PdfTestSupport;
import io.github.atengk.utils.pdf.PDFUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PDFUtilTemplateTest {

    @TempDir
    Path dir;

    @Test
    void templateMethodsShouldCreatePdf() throws Exception {
        Map<String, Object> data = Map.of("编号", "A001", "金额", 100);
        Path template = dir.resolve("template.pdf");
        Path contract = dir.resolve("contract.pdf");
        Path report = dir.resolve("report.pdf");
        Path invoice = dir.resolve("invoice.pdf");
        Path certificate = dir.resolve("certificate.pdf");

        PDFUtil.renderTemplate(document -> PDFUtil.addParagraph(document, "template"), data, template);
        assertTrue(PDFUtil.isPdf(PDFUtil.renderTemplateToBytes(document -> PDFUtil.addParagraph(document, "template"), data)));
        PDFUtil.renderContract(data, contract);
        PDFUtil.renderReport(data, report);
        PDFUtil.renderInvoice(data, invoice);
        PDFUtil.renderCertificate(data, certificate);
        assertTrue(PDFUtil.isPdf(PDFUtil.templateToPdfBytes("模板", data)));
        PDFUtil.templateToPdf("模板", data, dir.resolve("template-data.pdf"));

        PdfTestSupport.assertPdfExists(contract);
        PdfTestSupport.assertPdfExists(report);
        PdfTestSupport.assertPdfExists(invoice);
        PdfTestSupport.assertPdfExists(certificate);
    }

    @Test
    void invalidTemplateArgumentsShouldThrow() {
        assertThrows(IllegalArgumentException.class, () -> PDFUtil.templateToPdf("", Map.of(), dir.resolve("x.pdf")));
        assertThrows(IllegalArgumentException.class, () -> PDFUtil.renderContract(null, dir.resolve("x.pdf")));
    }
}
