package io.github.atengk.word;

import io.github.atengk.utils.word.WordUtil;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WordMergeSplitTest {

    @Test
    void mergeAndAppendShouldWork() {
        XWPFDocument one = WordUtil.create();
        WordUtil.addParagraph(one, "一");
        XWPFDocument two = WordUtil.create();
        WordUtil.addParagraph(two, "二");
        XWPFDocument merged = WordUtil.merge(List.of(one, two));
        assertTrue(WordUtil.extractAllText(merged).contains("一"));
        assertTrue(WordUtil.extractAllText(merged).contains("二"));
    }

    @Test
    void splitAndExtractShouldWork() {
        XWPFDocument document = WordUtil.create();
        WordUtil.addParagraph(document, "A");
        WordUtil.addParagraph(document, "B");
        assertEquals(2, WordUtil.splitByParagraph(document).size());
        assertEquals("A", WordUtil.extractText(WordUtil.extractParagraphs(document, 0, 1)));
    }

    @Test
    void invalidExtractRangeShouldThrow() {
        XWPFDocument document = WordUtil.create();
        WordUtil.addParagraph(document, "A");
        assertThrows(IllegalArgumentException.class, () -> WordUtil.extractParagraphs(document, 1, 0));
        assertDoesNotThrow(() -> WordUtil.copyStyles(document, WordUtil.create()));
    }
}
