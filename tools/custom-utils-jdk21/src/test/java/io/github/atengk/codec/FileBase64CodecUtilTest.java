package io.github.atengk.codec;

import io.github.atengk.utils.codec.CodecUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileBase64CodecUtilTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldConvertFileAndBase64() throws Exception {
        Path source = tempDir.resolve("source.txt");
        Path target = tempDir.resolve("target.txt");
        Files.writeString(source, "中文 file", StandardCharsets.UTF_8);

        String base64 = CodecUtil.fileToBase64(source);
        CodecUtil.base64ToFile(base64, target);

        assertEquals("中文 file", Files.readString(target, StandardCharsets.UTF_8));
    }

    @Test
    void shouldConvertInputStreamAndBase64() throws Exception {
        ByteArrayInputStream inputStream = new ByteArrayInputStream("stream".getBytes(StandardCharsets.UTF_8));
        String base64 = CodecUtil.inputStreamToBase64(inputStream);
        assertEquals("stream", new String(CodecUtil.base64ToInputStream(base64).readAllBytes(), StandardCharsets.UTF_8));
    }

    @Test
    void shouldHandleDataUri() {
        String base64 = CodecUtil.base64Encode("hello");
        String dataUri = CodecUtil.base64ToDataUri(base64, "text/plain");

        assertTrue(CodecUtil.isDataUri(dataUri));
        assertEquals("text/plain", CodecUtil.getDataUriMimeType(dataUri));
        assertEquals(base64, CodecUtil.dataUriToBase64(dataUri));
        assertEquals(dataUri, CodecUtil.bytesToDataUri("hello".getBytes(StandardCharsets.UTF_8), "text/plain"));
    }

    @Test
    void shouldRejectInvalidDataUriAndNullFileArgs() {
        assertFalse(CodecUtil.isDataUri("abc"));
        assertThrows(IllegalArgumentException.class, () -> CodecUtil.dataUriToBase64("abc"));
        assertThrows(NullPointerException.class, () -> CodecUtil.fileToBase64(null));
        assertThrows(IllegalArgumentException.class, () -> CodecUtil.base64ToFile(null, tempDir.resolve("a.txt")));
    }
}
