package io.github.atengk.word;

import io.github.atengk.utils.word.WordUtil;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;

import java.io.ByteArrayInputStream;
import java.util.Base64;

final class WordUtilTestSupport {

    private WordUtilTestSupport() {
    }

    static XWPFDocument sampleDocument() {
        XWPFDocument document = WordUtil.create();
        WordUtil.addParagraph(document, "姓名：${name}");
        XWPFTable table = WordUtil.addTable(document, 2, 2);
        WordUtil.setCellText(table.getRow(0).getCell(0), "键");
        WordUtil.setCellText(table.getRow(0).getCell(1), "值");
        WordUtil.setCellText(table.getRow(1).getCell(0), "年龄");
        WordUtil.setCellText(table.getRow(1).getCell(1), "${age}");
        return document;
    }

    static XWPFParagraph paragraph(String text) {
        return WordUtil.addParagraph(WordUtil.create(), text);
    }

    static ByteArrayInputStream pngStream() {
        return new ByteArrayInputStream(Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/p9sAAAAASUVORK5CYII="));
    }
}
