package io.github.atengk.word;

import io.github.atengk.utils.word.WordUtil;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WordExtractTest {

    @Test
    void extractAllTextShouldIncludeParagraphTableHeaderFooter() {
        XWPFDocument document = WordUtil.create();
        WordUtil.addParagraph(document, "正文");
        XWPFTable table = WordUtil.addTable(document, 1, 1);
        WordUtil.setCellText(table.getRow(0).getCell(0), "表格");
        WordUtil.addHeader(document, "页眉");
        WordUtil.addFooter(document, "页脚");
        String text = WordUtil.extractAllText(document);
        assertTrue(text.contains("正文"));
        assertTrue(text.contains("表格"));
        assertTrue(text.contains("页眉"));
        assertTrue(text.contains("页脚"));
    }

    @Test
    void extractAroundKeywordShouldWork() {
        XWPFDocument document = WordUtil.create();
        WordUtil.addParagraph(document, "abcdefg");
        assertEquals("cde", WordUtil.extractTextAroundKeyword(document, "d", 1));
        assertEquals("", WordUtil.extractTextAroundKeyword(document, "x", 1));
    }

    @Test
    void extractInvalidRangeShouldThrow() {
        assertThrows(IllegalArgumentException.class, () -> WordUtil.extractTextAroundKeyword(WordUtil.create(), "x", -1));
    }
}
