package io.github.atengk.zip;

import io.github.atengk.utils.zip.ZipUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ZipUtilEncodingMetadataTest {

    @TempDir
    Path tempDir;

    @Test
    void charsetMethodsShouldUseGivenEncoding() throws Exception {
        Path source = ZipUtilTestSupport.text(tempDir, "中文.txt", "内容");
        Path zip = tempDir.resolve("charset.zip");
        Path out = tempDir.resolve("charset-out");

        ZipUtil.zipWithCharset(source, zip, StandardCharsets.UTF_8);
        ZipUtil.unzipWithCharset(zip, out, StandardCharsets.UTF_8);

        assertEquals("内容", ZipUtilTestSupport.read(out.resolve("中文.txt")));
    }

    @Test
    void zipCommentShouldBeStoredAndRead() throws Exception {
        Path source = ZipUtilTestSupport.text(tempDir, "comment.txt", "comment");
        Path zip = tempDir.resolve("comment.zip");
        ZipUtil.zip(source, zip);

        ZipUtil.setZipComment(zip, "备注");

        assertEquals("备注", ZipUtil.getZipComment(zip));
    }

    @Test
    void normalizeEncodingShouldDefaultToUtf8() {
        assertEquals(StandardCharsets.UTF_8, ZipUtil.normalizeEncoding(null));
    }
}
