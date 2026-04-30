package io.github.atengk.pdf;

import io.github.atengk.pdf.testsupport.PdfTestSupport;
import io.github.atengk.utils.pdf.PDFUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PDFUtilMetadataTest {

    @TempDir
    Path dir;

    @Test
    void metadataShouldBeWrittenAndRead() throws Exception {
        Path pdf = dir.resolve("meta.pdf");
        PDFUtil.writeToFile(pdf, PDFUtil.createA4PageOptions(), new PDFUtil.MetadataOptions("标题", "作者", "主题", "关键字", "创建者"), document -> PDFUtil.addParagraph(document, "meta"));
        assertEquals("标题", PDFUtil.readTitle(pdf));
        assertEquals("作者", PDFUtil.readAuthor(pdf));
        assertEquals("主题", PDFUtil.readSubject(pdf));
        assertEquals("关键字", PDFUtil.readKeywords(pdf));

        Path changed = dir.resolve("changed.pdf");
        PDFUtil.setMetadata(pdf, changed, new PDFUtil.MetadataOptions("新标题", "新作者", "新主题", "新关键字", "新创建者"));
        assertEquals("新标题", PDFUtil.readTitle(changed));

        Path cleared = dir.resolve("cleared.pdf");
        PDFUtil.clearMetadata(changed, cleared);
        PdfTestSupport.assertPdfExists(cleared);
    }

    @Test
    void documentMetadataMethodsShouldRejectBlank() {
        assertThrows(IllegalArgumentException.class, () -> PDFUtil.setTitle(PDFUtil.createDocument(), ""));
        assertThrows(IllegalArgumentException.class, () -> PDFUtil.setAuthor(PDFUtil.createDocument(), ""));
        assertThrows(IllegalArgumentException.class, () -> PDFUtil.setSubject(PDFUtil.createDocument(), ""));
        assertThrows(IllegalArgumentException.class, () -> PDFUtil.setKeywords(PDFUtil.createDocument(), ""));
        assertThrows(IllegalArgumentException.class, () -> PDFUtil.setCreator(PDFUtil.createDocument(), ""));
    }
}
