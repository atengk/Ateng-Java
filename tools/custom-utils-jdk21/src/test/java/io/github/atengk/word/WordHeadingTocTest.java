package io.github.atengk.word;

import io.github.atengk.utils.word.WordUtil;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WordHeadingTocTest {

    @Test
    void headingShortcutShouldWork() {
        XWPFDocument document = WordUtil.create();
        WordUtil.addHeading1(document, "一级标题");
        WordUtil.addHeading2(document, "二级标题");
        WordUtil.addHeading3(document, "三级标题");
        assertEquals(3, WordUtil.getParagraphs(document).size());
        assertEquals("Heading1", WordUtil.getParagraphs(document).getFirst().getStyle());
    }

    @Test
    void tocAndNumberedHeadingShouldNotThrow() {
        XWPFDocument document = WordUtil.create();
        XWPFParagraph toc = WordUtil.addToc(document);
        assertNotNull(toc);
        assertDoesNotThrow(() -> WordUtil.markTocDirty(document));
        assertNotNull(WordUtil.addNumberedHeading(document, "编号标题", 1));
    }

    @Test
    void invalidHeadingLevelShouldThrow() {
        assertThrows(IllegalArgumentException.class, () -> WordUtil.addHeading(WordUtil.create(), "错误", 0));
        assertThrows(IllegalArgumentException.class, () -> WordUtil.applyHeadingStyle(WordUtilTestSupport.paragraph("x"), 10));
    }
}
