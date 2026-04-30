package io.github.atengk.word;

import io.github.atengk.utils.word.WordUtil;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WordTableTest {

    @Test
    void tableCrudShouldWork() {
        XWPFDocument document = WordUtil.create();
        XWPFTable table = WordUtil.addTable(document, 2, 2);
        WordUtil.setCellText(WordUtil.getCell(table, 0, 0), "A");
        assertEquals("A", WordUtil.getCellText(table.getRow(0).getCell(0)));
        WordUtil.addRow(table);
        assertEquals(3, table.getRows().size());
        WordUtil.removeRow(table, 2);
        assertEquals(2, table.getRows().size());
    }

    @Test
    void fillTableAndTemplateRowShouldWork() {
        XWPFDocument document = WordUtil.create();
        XWPFTable table = WordUtil.addTable(document, 1, 2);
        WordUtil.fillTable(table, List.of(List.of("姓名", "年龄"), List.of("张三", 18)));
        assertEquals("张三", WordUtil.getCellText(table.getRow(1).getCell(0)));
        WordUtil.setCellText(table.getRow(1).getCell(0), "${name}");
        WordUtil.setCellText(table.getRow(1).getCell(1), "${age}");
        WordUtil.fillTableByTemplateRow(table, 1, List.of(Map.of("name", "李四", "age", 20)));
        assertEquals("李四", WordUtil.getCellText(table.getRow(1).getCell(0)));
    }

    @Test
    void tableInvalidIndexShouldThrow() {
        XWPFDocument document = WordUtil.create();
        assertThrows(IndexOutOfBoundsException.class, () -> WordUtil.getTable(document, 0));
        XWPFTable table = WordUtil.addTable(document, 1, 1);
        assertThrows(IndexOutOfBoundsException.class, () -> WordUtil.getCell(table, 1, 0));
    }
}
