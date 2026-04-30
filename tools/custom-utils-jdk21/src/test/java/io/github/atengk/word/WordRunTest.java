package io.github.atengk.word;

import io.github.atengk.utils.word.WordUtil;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WordRunTest {

    @Test
    void runCrudShouldWork() {
        XWPFParagraph paragraph = WordUtilTestSupport.paragraph("");
        XWPFRun run = WordUtil.addRun(paragraph, "A");
        WordUtil.setRunText(run, "B");
        assertEquals("B", WordUtil.getRunText(run));
        WordUtil.clearRun(run);
        assertTrue(WordUtil.isBlankRun(run));
    }

    @Test
    void mergeRunsShouldKeepText() {
        XWPFParagraph paragraph = WordUtilTestSupport.paragraph("");
        WordUtil.clearParagraph(paragraph);
        WordUtil.addRun(paragraph, "${na");
        WordUtil.addRun(paragraph, "me}");
        WordUtil.normalizeRuns(paragraph);
        assertEquals(1, WordUtil.getRuns(paragraph).size());
        assertEquals("${name}", WordUtil.getParagraphText(paragraph));
    }

    @Test
    void runNullShouldThrow() {
        assertThrows(NullPointerException.class, () -> WordUtil.getRunText(null));
        assertThrows(NullPointerException.class, () -> WordUtil.copyRunStyle(null, WordUtilTestSupport.paragraph("").createRun()));
    }
}
