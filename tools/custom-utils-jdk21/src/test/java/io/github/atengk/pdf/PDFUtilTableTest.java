package io.github.atengk.pdf;

import io.github.atengk.pdf.testsupport.PdfTestSupport;
import io.github.atengk.utils.pdf.PDFUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openpdf.text.Element;
import org.openpdf.text.pdf.PdfPTable;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PDFUtilTableTest {

    @TempDir
    Path dir;

    @Test
    void tableMethodsShouldCreatePdf() throws Exception {
        Path pdf = dir.resolve("table.pdf");
        PDFUtil.writeToFile(pdf, document -> {
            PdfPTable table = PDFUtil.createTable(new float[]{1, 2});
            PDFUtil.addHeaderRow(table, List.of("编号", "姓名"));
            PDFUtil.addRow(table, List.of("1", "张三"));
            PDFUtil.addRows(table, List.of(List.of("2", "李四")));
            table.addCell(PDFUtil.createEmptyCell());
            table.addCell(PDFUtil.createMergedCell("合并", 1));
            PDFUtil.setTableWidth(table, 90);
            PDFUtil.setCellPadding(PDFUtil.createCell("x"), 2);
            PDFUtil.setCellAlign(PDFUtil.createCell("x"), Element.ALIGN_CENTER, Element.ALIGN_MIDDLE);
            PDFUtil.addTable(document, table);
            PDFUtil.addTable(document, List.of("编号", "姓名"), PdfTestSupport.rows());
            PDFUtil.addBeanTable(document, List.of("a", "bb"), List.of(PDFUtil.createTableColumn("长度", value -> String.valueOf(value.length()))));
        });
        PdfTestSupport.assertPdfExists(pdf);
    }

    @Test
    void invalidTableArgumentsShouldThrow() {
        assertThrows(IllegalArgumentException.class, () -> PDFUtil.createTable(0));
        assertThrows(NullPointerException.class, () -> PDFUtil.addTable(null, PDFUtil.createTable(1)));
        assertThrows(IllegalArgumentException.class, () -> PDFUtil.createMergedCell("x", 0));
        assertThrows(IllegalArgumentException.class, () -> PDFUtil.setTableWidth(PDFUtil.createTable(1), 101));
    }
}
