package io.github.atengk.filetype;

import io.github.atengk.utils.filetype.FileTypeCategory;
import io.github.atengk.utils.filetype.FileTypeInfo;
import io.github.atengk.utils.filetype.FileTypeUtil;
import org.apache.tika.mime.MediaType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FileTypeUtilBasicDetectionTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldDetectMimeTypeByPath() throws Exception {
        Path path = FileTypeTestSupport.writeFile(tempDir, "demo.pdf", FileTypeTestSupport.PDF_BYTES);

        String mimeType = FileTypeUtil.detectMimeType(path);

        assertEquals("application/pdf", mimeType);
    }

    @Test
    void shouldDetectMimeTypeByFileInputStreamAndBytes() throws Exception {
        Path path = FileTypeTestSupport.writeFile(tempDir, "demo.png", FileTypeTestSupport.PNG_BYTES);
        File file = path.toFile();

        assertEquals("image/png", FileTypeUtil.detectMimeType(file));
        assertEquals("image/png", FileTypeUtil.detectMimeType(new ByteArrayInputStream(FileTypeTestSupport.PNG_BYTES), "demo.png"));
        assertEquals("image/png", FileTypeUtil.detectMimeType(FileTypeTestSupport.PNG_BYTES));
    }

    @Test
    void shouldDetectMediaTypeAndFileTypeInfo() throws Exception {
        Path path = FileTypeTestSupport.writeFile(tempDir, "demo.pdf", FileTypeTestSupport.PDF_BYTES);

        MediaType mediaType = FileTypeUtil.detectMediaType(path);
        FileTypeInfo info = FileTypeUtil.detectFileType(path);

        assertEquals("application", mediaType.getType());
        assertEquals("pdf", info.getExtension());
        assertEquals(FileTypeCategory.PDF, info.getCategory());
    }

    @Test
    void shouldRejectInvalidDetectionArguments() {
        assertThrows(IllegalArgumentException.class, () -> FileTypeUtil.detectMimeType((Path) null));
        assertThrows(IllegalArgumentException.class, () -> FileTypeUtil.detectMimeType((File) null));
        assertThrows(IllegalArgumentException.class, () -> FileTypeUtil.detectMimeType((ByteArrayInputStream) null));
        assertThrows(IllegalArgumentException.class, () -> FileTypeUtil.detectMimeType((byte[]) null));
        assertThrows(IllegalArgumentException.class, () -> FileTypeUtil.detectMimeType(tempDir.resolve("missing.pdf")));
    }
}
