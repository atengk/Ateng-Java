package io.github.atengk.file;

import io.github.atengk.utils.FileUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FileUtilFileTypeTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldIdentifyCommonTypes() throws Exception {
        Path image = tempDir.resolve("a.png");
        Files.writeString(image, "fake");
        assertTrue(FileUtil.isImage(image));
        assertTrue(FileUtil.isPdf(tempDir.resolve("a.pdf")));
        assertTrue(FileUtil.isExcel(tempDir.resolve("a.xlsx")));
        assertTrue(FileUtil.isWord(tempDir.resolve("a.docx")));
        assertTrue(FileUtil.isArchive(tempDir.resolve("a.zip")));
        assertTrue(FileUtil.hasExtName(image));
        assertTrue(FileUtil.matchExtName(image, List.of("png")));
        assertNotNull(FileUtil.getMimeType(image));
    }

    @Test
    void shouldIdentifyMediaAndText() {
        assertTrue(FileUtil.isVideo(Path.of("a.mp4")));
        assertTrue(FileUtil.isAudio(Path.of("a.mp3")));
        assertTrue(FileUtil.isText(Path.of("a.json")));
    }

    @Test
    void shouldRejectEmptyExtCollection() {
        assertThrows(IllegalArgumentException.class, () -> FileUtil.matchExtName(Path.of("a.txt"), List.of()));
    }
}
