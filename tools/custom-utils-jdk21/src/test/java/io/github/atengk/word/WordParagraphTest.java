package io.github.atengk.word;

import io.github.atengk.utils.word.WordUtil;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WordParagraphTest {

    @Test
    void paragraphCrudShouldWork() {
        XWPFDocument document = WordUtil.create();
        WordUtil.addParagraph(document, "第一段");
        WordUtil.insertParagraph(document, 0, "插入段");
        assertEquals("插入段", WordUtil.getFirstParagraph(document).getText());
        assertEquals("第一段", WordUtil.getLastParagraph(document).getText());
        WordUtil.removeParagraph(document, 0);
        assertEquals(1, WordUtil.getParagraphs(document).size());
    }

    @Test
    void paragraphTextShouldHandleBlank() {
        XWPFParagraph paragraph = WordUtilTestSupport.paragraph("");
        assertTrue(WordUtil.isBlankParagraph(paragraph));
        WordUtil.appendParagraphText(paragraph, "abc");
        assertFalse(WordUtil.isBlankParagraph(paragraph));
    }

    @Test
    void invalidParagraphIndexShouldThrow() {
        XWPFDocument document = WordUtil.create();
        assertThrows(IndexOutOfBoundsException.class, () -> WordUtil.removeParagraph(document, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> WordUtil.insertParagraph(document, 2, "错误"));
    }
}
