package io.github.atengk.word;

import io.github.atengk.utils.word.WordUtil;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class WordDocumentPropertiesTest {

    @Test
    void setAndGetPropertiesShouldWork() {
        XWPFDocument document = WordUtil.create();
        LocalDateTime time = LocalDateTime.of(2026, 4, 30, 10, 0);
        WordUtil.setProperties(document, new WordUtil.WordProperties("标题", "Ateng", "主题", "word,poi", time));
        assertEquals("标题", WordUtil.getTitle(document));
        assertEquals("Ateng", WordUtil.getAuthor(document));
        assertEquals("主题", WordUtil.getSubject(document));
        assertEquals("word,poi", WordUtil.getKeywords(document));
        assertNotNull(WordUtil.getCreatedTime(document));
    }

    @Test
    void nullCreatedTimeShouldBeAllowed() {
        XWPFDocument document = WordUtil.create();
        WordUtil.setCreatedTime(document, null);
        assertNull(WordUtil.getCreatedTime(document));
    }

    @Test
    void nullDocumentShouldThrow() {
        assertThrows(NullPointerException.class, () -> WordUtil.setTitle(null, "标题"));
        assertThrows(NullPointerException.class, () -> WordUtil.setProperties(WordUtil.create(), null));
    }
}
