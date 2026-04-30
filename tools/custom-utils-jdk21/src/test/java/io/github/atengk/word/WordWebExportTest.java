package io.github.atengk.word;

import io.github.atengk.utils.word.WordUtil;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WordWebExportTest {

    @Test
    void downloadHeadersShouldWork() {
        Map<String, String> headers = WordUtil.buildDownloadHeaders("测试文档");
        assertEquals("application/vnd.openxmlformats-officedocument.wordprocessingml.document", headers.get("Content-Type"));
        assertTrue(headers.get("Content-Disposition").contains("filename*"));
        assertTrue(WordUtil.buildDownloadFileName("a").endsWith(".docx"));
    }

    @Test
    void exportTemplateToResponseShouldWork() throws Exception {
        XWPFDocument template = WordUtil.create();
        WordUtil.addParagraph(template, "${name}");
        byte[] bytes = WordUtil.writeToBytes(template);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Map<String, String> headers = WordUtil.exportTemplateToResponse(new ByteArrayInputStream(bytes), Map.of("name", "张三"), outputStream, "out.docx");
        assertFalse(outputStream.toByteArray().length == 0);
        assertTrue(headers.containsKey("Content-Disposition"));
    }

    @Test
    void exportBytesAndTempFileShouldWork() throws Exception {
        XWPFDocument document = WordUtil.create();
        WordUtil.addParagraph(document, "导出");
        byte[] bytes = WordUtil.exportToBytes(document);
        Path temp = WordUtil.exportToTempFile(document, "导出.docx");
        assertTrue(bytes.length > 0);
        assertTrue(temp.toFile().exists());
    }
}
