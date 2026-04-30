package io.github.atengk.word;

import io.github.atengk.utils.word.WordUtil;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WordWatermarkTest {

    @Test
    void textWatermarkShouldAddHeader() {
        XWPFDocument document = WordUtil.create();
        WordUtil.addTextWatermark(document, "内部资料");
        assertFalse(WordUtil.getHeaders(document).isEmpty());
        assertTrue(WordUtil.extractHeaderTexts(document).contains("内部资料"));
    }

    @Test
    void imageWatermarkAndConfidentialMarkShouldWork() throws Exception {
        XWPFDocument document = WordUtil.create();
        WordUtil.addImageWatermark(document, WordUtilTestSupport.pngStream(), "w.png");
        WordUtil.addConfidentialMark(document, "保密");
        assertFalse(WordUtil.getHeaders(document).isEmpty());
    }

    @Test
    void clearWatermarkAndBackgroundShouldWork() {
        XWPFDocument document = WordUtil.create();
        WordUtil.addTextWatermark(document, "水印");
        WordUtil.clearWatermark(document);
        WordUtil.setBackgroundColor(document, "FFFFFF");
        assertNotNull(document.getDocument().getBackground());
        assertThrows(IllegalArgumentException.class, () -> WordUtil.setBackgroundColor(document, "white"));
    }
}
