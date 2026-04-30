package io.github.atengk.word;

import io.github.atengk.utils.word.WordUtil;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WordStyleTest {

    @Test
    void runAndParagraphStyleShouldApply() {
        XWPFParagraph paragraph = WordUtilTestSupport.paragraph("样式");
        XWPFRun run = paragraph.getRuns().getFirst();
        WordUtil.setFont(run, "宋体");
        WordUtil.setFontSize(run, 14);
        WordUtil.setFontColor(run, "000000");
        WordUtil.setBold(run, true);
        WordUtil.setItalic(run, true);
        WordUtil.setUnderline(run, true);
        WordUtil.setParagraphAlign(paragraph, ParagraphAlignment.CENTER);
        WordUtil.setParagraphIndent(paragraph, 420);
        WordUtil.setLineSpacing(paragraph, 1.5);
        WordUtil.setParagraphSpacing(paragraph, 100, 100);
        assertTrue(run.isBold());
        assertEquals(ParagraphAlignment.CENTER, paragraph.getAlignment());
    }

    @Test
    void copyAndDefaultStyleShouldWork() {
        XWPFDocument document = WordUtil.create();
        XWPFParagraph source = WordUtil.addParagraph(document, "源");
        XWPFParagraph target = WordUtil.addParagraph(document, "目标");
        WordUtil.setParagraphAlign(source, ParagraphAlignment.RIGHT);
        WordUtil.copyParagraphStyle(source, target);
        WordUtil.applyDefaultTextStyle(document);
        WordUtil.applyHeadingStyle(target, 2);
        assertEquals("Heading2", target.getStyle());
    }

    @Test
    void invalidStyleShouldThrow() {
        XWPFRun run = WordUtilTestSupport.paragraph("x").getRuns().getFirst();
        assertThrows(IllegalArgumentException.class, () -> WordUtil.setFontSize(run, 0));
        assertThrows(IllegalArgumentException.class, () -> WordUtil.setFontColor(run, "000"));
        assertThrows(IllegalArgumentException.class, () -> WordUtil.setLineSpacing(WordUtilTestSupport.paragraph("x"), 0));
    }
}
