package io.github.atengk.word;

import io.github.atengk.utils.word.WordUtil;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WordConfigObjectTest {

    @Test
    void templateConfigShouldControlClearUnusedPlaceholders() {
        XWPFDocument document = WordUtil.create();
        WordUtil.addParagraph(document, "{{name}} {{age}}");
        WordUtil.fillTemplate(document, Map.of("name", "张三"), new WordUtil.WordTemplateConfig("{{", "}}", true));
        assertTrue(WordUtil.extractAllText(document).contains("张三"));
    }

    @Test
    void imageAndExportConfigShouldCreate() {
        WordUtil.WordImage image = WordUtil.WordImage.of(WordUtilTestSupport.pngStream(), "a.png", 10, 10, "${img}");
        WordUtil.WordExportConfig exportConfig = new WordUtil.WordExportConfig("a.docx");
        assertEquals("a.png", image.fileName());
        assertEquals("a.docx", exportConfig.fileName());
    }

    @Test
    void otherConfigRecordsShouldCreate() {
        WordUtil.WordProperties properties = new WordUtil.WordProperties("t", "a", "s", "k", LocalDateTime.now());
        WordUtil.WordTextStyle textStyle = new WordUtil.WordTextStyle("宋体", 12, "000000", true, false, false);
        WordUtil.WordTableStyle tableStyle = new WordUtil.WordTableStyle("100%", true, true, "FFFFFF");
        WordUtil.WordPageConfig pageConfig = new WordUtil.WordPageConfig(1, 1, WordUtil.PageOrientation.PORTRAIT, 0, 0, 0, 0);
        assertEquals("t", properties.title());
        assertEquals(12, textStyle.fontSize());
        assertTrue(tableStyle.border());
        assertEquals(WordUtil.PageOrientation.PORTRAIT, pageConfig.orientation());
    }
}
