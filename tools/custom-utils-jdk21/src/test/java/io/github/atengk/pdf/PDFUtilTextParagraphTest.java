package io.github.atengk.pdf;

import io.github.atengk.pdf.testsupport.PdfTestSupport;
import io.github.atengk.utils.pdf.PDFUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PDFUtilTextParagraphTest {

    @TempDir
    Path dir;

    @Test
    void textMethodsShouldCreateReadablePdf() throws Exception {
        Path pdf = dir.resolve("text.pdf");
        PDFUtil.writeToFile(pdf, document -> {
            PDFUtil.addTitle(document, "标题");
            PDFUtil.addSubTitle(document, "副标题");
            PDFUtil.addParagraph(document, "段落");
            PDFUtil.addText(document, "文本");
            PDFUtil.addBlankLine(document);
            PDFUtil.addBlankLines(document, 2);
            PDFUtil.addList(document, List.of("a", "b"));
            PDFUtil.addOrderedList(document, List.of("1", "2"));
            PDFUtil.addAnchor(document, "open", "https://example.com");
            PDFUtil.addSeparator(document);
        });
        PdfTestSupport.assertPdfExists(pdf);
    }

    @Test
    void factoryMethodsShouldRejectNull() {
        assertThrows(IllegalArgumentException.class, () -> PDFUtil.createParagraph(null));
        assertThrows(IllegalArgumentException.class, () -> PDFUtil.createPhrase(null));
        assertThrows(IllegalArgumentException.class, () -> PDFUtil.createChunk(null));
        assertThrows(IllegalArgumentException.class, () -> PDFUtil.addBlankLines(PDFUtil.createDocument(), 0));
    }
}
