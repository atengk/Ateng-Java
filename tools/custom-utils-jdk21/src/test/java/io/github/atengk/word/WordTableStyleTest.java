package io.github.atengk.word;

import io.github.atengk.utils.word.WordUtil;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WordTableStyleTest {

    @Test
    void tableStyleShouldApply() {
        XWPFTable table = WordUtil.addTable(WordUtil.create(), 3, 3);
        WordUtil.setTableWidth(table, "100%");
        WordUtil.setColumnWidth(table, 0, "2400");
        WordUtil.setCellBackgroundColor(table.getRow(0).getCell(0), "#FF0000");
        WordUtil.setHeaderRowStyle(table.getRow(0));
        WordUtil.setAlternateRowStyle(table);
        assertEquals("100%", table.getWidth());
    }

    @Test
    void mergeCellsShouldNotThrow() {
        XWPFTable table = WordUtil.addTable(WordUtil.create(), 3, 3);
        assertDoesNotThrow(() -> WordUtil.mergeCellsHorizontal(table, 0, 0, 1));
        assertDoesNotThrow(() -> WordUtil.mergeCellsVertical(table, 2, 0, 2));
        assertDoesNotThrow(() -> WordUtil.setTableBorders(table));
        assertDoesNotThrow(() -> WordUtil.clearTableBorders(table));
    }

    @Test
    void invalidColorAndMergeShouldThrow() {
        XWPFTable table = WordUtil.addTable(WordUtil.create(), 1, 1);
        assertThrows(IllegalArgumentException.class, () -> WordUtil.setCellBackgroundColor(table.getRow(0).getCell(0), "red"));
        assertThrows(IllegalArgumentException.class, () -> WordUtil.mergeCellsHorizontal(table, 0, 1, 0));
    }
}
