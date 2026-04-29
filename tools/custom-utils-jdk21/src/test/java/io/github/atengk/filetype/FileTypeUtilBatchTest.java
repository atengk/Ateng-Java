package io.github.atengk.filetype;

import io.github.atengk.utils.filetype.FileTypeCategory;
import io.github.atengk.utils.filetype.FileTypeInfo;
import io.github.atengk.utils.filetype.FileTypeUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileTypeUtilBatchTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldDetectBatchAndDetectInfoBatch() throws Exception {
        Path pdf = FileTypeTestSupport.writeFile(tempDir, "demo.pdf", FileTypeTestSupport.PDF_BYTES);
        Path png = FileTypeTestSupport.writeFile(tempDir, "demo.png", FileTypeTestSupport.PNG_BYTES);

        Map<Path, String> batch = FileTypeUtil.detectBatch(List.of(pdf, png));
        List<FileTypeInfo> infos = FileTypeUtil.detectInfoBatch(List.of(pdf, png));

        assertEquals(2, batch.size());
        assertEquals("application/pdf", batch.get(pdf));
        assertEquals(2, infos.size());
    }

    @Test
    void shouldGroupCountAndFilter() {
        FileTypeInfo pdf = FileTypeUtil.toFileTypeInfo("demo.pdf", "application/pdf");
        FileTypeInfo png = FileTypeUtil.toFileTypeInfo("demo.png", "image/png");
        FileTypeInfo exe = FileTypeUtil.toFileTypeInfo("demo.exe", "application/x-msdownload");
        FileTypeInfo unknown = FileTypeUtil.toFileTypeInfo("demo.bin", "application/octet-stream");
        List<FileTypeInfo> infos = List.of(pdf, png, exe, unknown);

        assertEquals(1, FileTypeUtil.groupByCategory(infos).get(FileTypeCategory.PDF).size());
        assertEquals(1, FileTypeUtil.groupByMimeType(infos).get("image/png").size());
        assertEquals(Long.valueOf(1), FileTypeUtil.countByCategory(infos).get(FileTypeCategory.IMAGE));
        assertEquals(1, FileTypeUtil.filterByCategory(infos, FileTypeCategory.PDF).size());
        assertEquals(1, FileTypeUtil.filterByMimeType(infos, "image/png").size());
        assertEquals(1, FileTypeUtil.filterDangerousFiles(infos).size());
        assertEquals(1, FileTypeUtil.filterUnknownFiles(infos).size());
    }

    @Test
    void shouldHandleEmptyBatchCollections() {
        assertTrue(FileTypeUtil.detectBatch(null).isEmpty());
        assertTrue(FileTypeUtil.detectInfoBatch(null).isEmpty());
        assertTrue(FileTypeUtil.groupByCategory(null).isEmpty());
        assertTrue(FileTypeUtil.groupByMimeType(null).isEmpty());
        assertTrue(FileTypeUtil.countByCategory(null).isEmpty());
        assertTrue(FileTypeUtil.filterByCategory(null, FileTypeCategory.PDF).isEmpty());
        assertTrue(FileTypeUtil.filterByMimeType(null, "application/pdf").isEmpty());
        assertTrue(FileTypeUtil.filterDangerousFiles(null).isEmpty());
        assertTrue(FileTypeUtil.filterUnknownFiles(null).isEmpty());
    }
}
