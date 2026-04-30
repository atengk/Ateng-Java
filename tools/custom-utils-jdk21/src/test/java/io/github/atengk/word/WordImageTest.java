package io.github.atengk.word;

import io.github.atengk.utils.word.WordUtil;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WordImageTest {

    @Test
    void imageTypeShouldRecognizeSupportedFormats() {
        assertTrue(WordUtil.isSupportedImage("a.png"));
        assertTrue(WordUtil.isSupportedImage("a.jpg"));
        assertFalse(WordUtil.isSupportedImage("a.svg"));
    }

    @Test
    void addImageShouldWork() throws Exception {
        XWPFDocument document = WordUtil.create();
        WordUtil.addImage(document, WordUtilTestSupport.pngStream(), "a.png", 10, 10);
        assertFalse(WordUtil.extractImages(document).isEmpty());
    }

    @Test
    void invalidImageShouldThrow() {
        assertThrows(IllegalArgumentException.class, () -> WordUtil.getPictureType("a.svg"));
        assertThrows(IllegalArgumentException.class, () -> WordUtil.addImage(WordUtil.create(), WordUtilTestSupport.pngStream(), "a.png", 0, 10));
    }
}
