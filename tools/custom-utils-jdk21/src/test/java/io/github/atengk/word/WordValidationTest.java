package io.github.atengk.word;

import io.github.atengk.utils.word.WordUtil;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class WordValidationTest {

    @TempDir
    Path tempDir;

    @Test
    void fileAndTypeValidationShouldWork() throws Exception {
        Path path = tempDir.resolve("a.docx");
        Files.writeString(path, "x");
        assertTrue(WordUtil.isDocx("a.docx"));
        assertFalse(WordUtil.isDocx("a.doc"));
        assertDoesNotThrow(() -> WordUtil.checkFileSize(path, 10));
        assertThrows(IllegalArgumentException.class, () -> WordUtil.checkFileSize(path, 0));
    }

    @Test
    void tableAndCellValidationShouldWork() {
        XWPFDocument document = WordUtil.create();
        XWPFTable table = WordUtil.addTable(document, 1, 1);
        assertDoesNotThrow(() -> WordUtil.checkTableIndex(document, 0));
        assertDoesNotThrow(() -> WordUtil.checkCellIndex(table, 0, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> WordUtil.checkTableIndex(document, 1));
    }

    @Test
    void safeTextShouldCleanInvalidXmlChars() {
        assertEquals("ab", WordUtil.cleanInvalidXmlChars("a\u0000b"));
        assertEquals("", WordUtil.toSafeText(null));
        assertThrows(NullPointerException.class, () -> WordUtil.checkDocument(null));
    }
}
