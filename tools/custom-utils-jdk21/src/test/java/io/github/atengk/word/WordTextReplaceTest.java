package io.github.atengk.word;

import io.github.atengk.utils.word.WordUtil;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WordTextReplaceTest {

    @Test
    void replaceTextShouldWorkInParagraphAndTable() {
        XWPFDocument document = WordUtilTestSupport.sampleDocument();
        WordUtil.replaceAllText(document, Map.of("${name}", "张三", "${age}", "18"));
        assertTrue(WordUtil.containsText(document, "张三"));
        assertTrue(WordUtil.containsText(document, "18"));
    }

    @Test
    void countTextShouldHandleEmpty() {
        XWPFDocument document = WordUtil.create();
        WordUtil.addParagraph(document, "a a a");
        assertEquals(3, WordUtil.countText(document, "a"));
        assertEquals(0, WordUtil.countText(document, ""));
    }

    @Test
    void replaceNullSearchTextShouldThrow() {
        assertThrows(NullPointerException.class, () -> WordUtil.replaceText(WordUtil.create(), null, "x"));
    }
}
