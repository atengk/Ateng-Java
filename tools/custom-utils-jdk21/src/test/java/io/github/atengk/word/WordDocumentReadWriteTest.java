package io.github.atengk.word;

import io.github.atengk.utils.word.WordUtil;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class WordDocumentReadWriteTest {

    @TempDir
    Path tempDir;

    @Test
    void createWriteReadShouldWork() throws Exception {
        XWPFDocument document = WordUtil.create();
        WordUtil.addParagraph(document, "测试文档");
        byte[] bytes = WordUtil.writeToBytes(document);
        assertTrue(bytes.length > 0);
        XWPFDocument read = WordUtil.read(new ByteArrayInputStream(bytes));
        assertEquals("测试文档", WordUtil.extractText(read));
    }

    @Test
    void writeToFileShouldCreateParentDirectory() throws Exception {
        XWPFDocument document = WordUtil.create();
        WordUtil.addParagraph(document, "文件写入");
        Path path = tempDir.resolve("a/b/test.docx");
        WordUtil.writeToFile(document, path);
        assertTrue(path.toFile().exists());
        assertTrue(WordUtil.sizeOf(document) > 0);
    }

    @Test
    void invalidArgumentsShouldThrow() {
        assertThrows(NullPointerException.class, () -> WordUtil.read((ByteArrayInputStream) null));
        assertThrows(IllegalArgumentException.class, () -> WordUtil.checkDocxFile(Path.of("test.txt")));
        assertDoesNotThrow(() -> WordUtil.closeQuietly(null));
    }
}
