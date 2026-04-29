package io.github.atengk.file;

import io.github.atengk.utils.FileUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class FileUtilByteStreamTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldReadWriteAndAppendBytes() throws Exception {
        Path file = tempDir.resolve("bytes.bin");
        FileUtil.writeBytes(file, new byte[]{1, 2});
        FileUtil.appendBytes(file, new byte[]{3});
        assertArrayEquals(new byte[]{1, 2, 3}, FileUtil.readBytes(file));
    }

    @Test
    void shouldCopyStreamsAndTransferFile() throws Exception {
        ByteArrayInputStream input = new ByteArrayInputStream("abc".getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        assertEquals(3, FileUtil.copyStream(input, output));
        assertEquals("abc", output.toString(StandardCharsets.UTF_8));

        Path source = tempDir.resolve("source.txt");
        Path target = tempDir.resolve("target.txt");
        FileUtil.writeUtf8String(source, "hello");
        assertEquals(5, FileUtil.transferTo(source, target));
        assertEquals("hello", FileUtil.readUtf8String(target));
    }

    @Test
    void shouldWriteFromStreamAndReadToStream() throws Exception {
        Path file = tempDir.resolve("stream.txt");
        FileUtil.writeFromStream(new ByteArrayInputStream("data".getBytes(StandardCharsets.UTF_8)), file, true);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        assertEquals(4, FileUtil.readToStream(file, output));
        assertEquals("data", output.toString(StandardCharsets.UTF_8));
    }
}
