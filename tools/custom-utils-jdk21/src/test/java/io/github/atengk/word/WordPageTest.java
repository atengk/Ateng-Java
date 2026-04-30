package io.github.atengk.word;

import io.github.atengk.utils.word.WordUtil;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WordPageTest {

    @Test
    void pageConfigShouldApply() {
        XWPFDocument document = WordUtil.create();
        WordUtil.setA4Page(document);
        WordUtil.setPageOrientation(document, WordUtil.PageOrientation.LANDSCAPE);
        WordUtil.setPageMargin(document, 1000, 1000, 1000, 1000);
        assertTrue(document.getDocument().getBody().isSetSectPr());
    }

    @Test
    void breaksAndPageNumberShouldWork() {
        XWPFDocument document = WordUtil.create();
        XWPFParagraph paragraph = WordUtil.addParagraph(document, "分页");
        WordUtil.addPageBreak(paragraph);
        WordUtil.addBlankLine(document, 2);
        WordUtil.addPageNumber(document);
        assertFalse(WordUtil.getFooters(document).isEmpty());
    }

    @Test
    void invalidPageConfigShouldThrow() {
        assertThrows(IllegalArgumentException.class, () -> WordUtil.setPageSize(WordUtil.create(), 0, 1));
        assertThrows(IllegalArgumentException.class, () -> WordUtil.setPageMargin(WordUtil.create(), -1, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> WordUtil.addBlankLine(WordUtil.create(), -1));
    }
}
