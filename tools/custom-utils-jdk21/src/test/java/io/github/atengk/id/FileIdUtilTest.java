package io.github.atengk.id;

import io.github.atengk.utils.id.IdUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FileIdUtilTest {

    @Test
    void shouldGenerateFileIdAndName() {
        assertTrue(IdUtil.isUuidSimple(IdUtil.fileId()));
        String fileName = IdUtil.fileName("report.xlsx");
        assertTrue(fileName.endsWith(".xlsx"));
        assertEquals(32 + 5, fileName.length());
    }

    @Test
    void shouldGenerateFileNameWithPrefixAndKey() {
        String fileName = IdUtil.fileName("用户 头像", "a.png");
        assertTrue(fileName.startsWith("用户_头像_"));
        assertTrue(fileName.endsWith(".png"));
        String key = IdUtil.fileKey("avatar", "a.png");
        assertTrue(key.startsWith("avatar/"));
        assertTrue(key.endsWith(".png"));
    }

    @Test
    void shouldGenerateTempAndExportFileNames() {
        assertTrue(IdUtil.tempFileName("zip").matches("^tmp_[0-9a-f]{32}\\.zip$"));
        assertTrue(IdUtil.exportFileName("用户导出", ".xlsx").matches("^用户导出_\\d{14}\\.xlsx$"));
    }

    @Test
    void shouldRejectInvalidFileArguments() {
        assertThrows(IllegalArgumentException.class, () -> IdUtil.fileName(null));
        assertThrows(IllegalArgumentException.class, () -> IdUtil.fileName("p", "   "));
        assertThrows(IllegalArgumentException.class, () -> IdUtil.tempFileName(""));
        assertThrows(IllegalArgumentException.class, () -> IdUtil.exportFileName("", "xlsx"));
    }
}
