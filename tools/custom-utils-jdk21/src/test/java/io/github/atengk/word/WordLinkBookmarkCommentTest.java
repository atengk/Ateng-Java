package io.github.atengk.word;

import io.github.atengk.utils.word.WordUtil;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WordLinkBookmarkCommentTest {

    @Test
    void hyperlinkShouldBeAdded() {
        XWPFDocument document = WordUtil.create();
        XWPFParagraph paragraph = document.createParagraph();
        WordUtil.addHyperlink(paragraph, "OpenAI", "https://example.com");
        assertFalse(WordUtil.getHyperlinks(document).isEmpty());
    }

    @Test
    void bookmarkShouldBeFoundAndRemoved() {
        XWPFDocument document = WordUtil.create();
        XWPFParagraph paragraph = WordUtil.addParagraph(document, "书签段落");
        WordUtil.addBookmark(paragraph, "bk1");
        assertTrue(WordUtil.findBookmark(document, "bk1").isPresent());
        WordUtil.removeBookmark(document, "bk1");
        assertTrue(WordUtil.findBookmark(document, "bk1").isEmpty());
    }

    @Test
    void commentShouldBeAddedAndRemoved() {
        XWPFDocument document = WordUtil.create();
        WordUtil.addComment(document, "这里需要确认");
        assertEquals(1, WordUtil.getComments(document).size());
        WordUtil.removeComments(document);
        assertTrue(WordUtil.getComments(document).isEmpty());
    }
}
