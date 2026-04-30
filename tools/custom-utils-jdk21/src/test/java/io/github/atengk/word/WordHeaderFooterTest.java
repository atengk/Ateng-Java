package io.github.atengk.word;

import io.github.atengk.utils.word.WordUtil;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFFooter;
import org.apache.poi.xwpf.usermodel.XWPFHeader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WordHeaderFooterTest {

    @Test
    void headerFooterTextShouldWork() {
        XWPFDocument document = WordUtil.create();
        XWPFHeader header = WordUtil.addHeader(document, "页眉");
        XWPFFooter footer = WordUtil.addFooter(document, "页脚");
        WordUtil.setHeaderText(header, "新页眉");
        WordUtil.setFooterText(footer, "新页脚");
        assertEquals("新页眉", WordUtil.extractHeaderTexts(document).getFirst());
        assertEquals("新页脚", WordUtil.extractFooterTexts(document).getFirst());
    }

    @Test
    void headerFooterImageAndPageNumberShouldWork() throws Exception {
        XWPFDocument document = WordUtil.create();
        WordUtil.addHeaderImage(document, WordUtilTestSupport.pngStream(), "h.png", 10, 10);
        WordUtil.addFooterImage(document, WordUtilTestSupport.pngStream(), "f.png", 10, 10);
        WordUtil.addPageNumberFooter(document);
        assertFalse(WordUtil.getHeaders(document).isEmpty());
        assertFalse(WordUtil.getFooters(document).isEmpty());
    }

    @Test
    void clearHeaderFooterShouldNotThrow() {
        XWPFDocument document = WordUtil.create();
        WordUtil.addHeader(document, "页眉");
        WordUtil.addFooter(document, "页脚");
        assertDoesNotThrow(() -> WordUtil.clearHeaders(document));
        assertDoesNotThrow(() -> WordUtil.clearFooters(document));
    }
}
