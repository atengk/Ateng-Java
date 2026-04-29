package io.github.atengk.filetype;

import io.github.atengk.utils.filetype.FileTypeCategory;
import io.github.atengk.utils.filetype.FileTypeUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.*;

class FileTypeUtilCategoryTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldIdentifyCommonFileTypes() {
        assertTrue(FileTypeUtil.isImage("image/png"));
        assertTrue(FileTypeUtil.isAudio("audio/mpeg"));
        assertTrue(FileTypeUtil.isVideo("video/mp4"));
        assertTrue(FileTypeUtil.isText("text/plain"));
        assertTrue(FileTypeUtil.isPdf("application/pdf"));
        assertTrue(FileTypeUtil.isOffice("application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
        assertTrue(FileTypeUtil.isWord("application/msword"));
        assertTrue(FileTypeUtil.isExcel("application/vnd.ms-excel"));
        assertTrue(FileTypeUtil.isPowerPoint("application/vnd.ms-powerpoint"));
        assertTrue(FileTypeUtil.isArchive("application/zip"));
    }

    @Test
    void shouldIdentifySpecificTextLikeTypes() {
        assertTrue(FileTypeUtil.isCode("text/x-java-source", "java"));
        assertTrue(FileTypeUtil.isJson("application/json", ""));
        assertTrue(FileTypeUtil.isXml("application/xml", ""));
        assertTrue(FileTypeUtil.isCsv("text/csv", ""));
        assertTrue(FileTypeUtil.isMarkdown("text/plain", "md"));
        assertTrue(FileTypeUtil.isFont("font/woff2"));
        assertTrue(FileTypeUtil.isEmail("message/rfc822"));
        assertTrue(FileTypeUtil.isExecutable("application/x-msdownload", "exe"));
        assertTrue(FileTypeUtil.isScript("application/x-sh", "sh"));
        assertTrue(FileTypeUtil.isDatabaseFile("application/vnd.sqlite3", "sqlite"));
    }

    @Test
    void shouldMapToBusinessCategory() throws Exception {
        Path path = FileTypeTestSupport.writeFile(tempDir, "demo.png", FileTypeTestSupport.PNG_BYTES);

        assertEquals(FileTypeCategory.IMAGE, FileTypeUtil.getCategory("image/png"));
        assertEquals(FileTypeCategory.PDF, FileTypeUtil.getCategory("application/pdf", "pdf"));
        assertEquals(FileTypeCategory.IMAGE, FileTypeUtil.getCategory(path));
        assertEquals("图片", FileTypeUtil.getCategoryName("image/png"));
        assertTrue(FileTypeUtil.isCategory("image/png", FileTypeCategory.IMAGE));
        assertTrue(FileTypeUtil.isAnyCategory("image/png", EnumSet.of(FileTypeCategory.IMAGE, FileTypeCategory.PDF)));
    }

    @Test
    void shouldHandleUnknownAndNullCategoryValues() {
        assertEquals(FileTypeCategory.UNKNOWN, FileTypeUtil.getCategory((String) null));
        assertEquals("未知", FileTypeUtil.getDisplayName(null));
        assertFalse(FileTypeUtil.isCategory("image/png", null));
        assertFalse(FileTypeUtil.isAnyCategory("image/png", null));
    }
}
