package io.github.atengk.zip;

import io.github.atengk.utils.ZipUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ZipUtilArchiveFormatTest {

    @TempDir
    Path tempDir;

    @Test
    void tarAndUntarShouldWork() throws Exception {
        Path source = ZipUtilTestSupport.sampleDir(tempDir);
        Path tar = tempDir.resolve("sample.tar");
        Path out = tempDir.resolve("tar-out");

        ZipUtil.tar(source, tar);
        ZipUtil.untar(tar, out);

        assertEquals("A", ZipUtilTestSupport.read(out.resolve("source/a.txt")));
    }

    @Test
    void gzipAndGunzipShouldWorkForSingleFile() throws Exception {
        Path source = ZipUtilTestSupport.text(tempDir, "g.txt", "gzip");
        Path gz = tempDir.resolve("g.txt.gz");
        Path out = tempDir.resolve("g-out.txt");

        ZipUtil.gzip(source, gz);
        ZipUtil.gunzip(gz, out);

        assertEquals("gzip", ZipUtilTestSupport.read(out));
    }

    @Test
    void tarGzTarBz2TarXzShouldExtract() throws Exception {
        Path source = ZipUtilTestSupport.sampleDir(tempDir);
        Path tgz = tempDir.resolve("a.tar.gz");
        Path tbz2 = tempDir.resolve("a.tar.bz2");
        Path txz = tempDir.resolve("a.tar.xz");

        ZipUtil.tarGz(source, tgz);
        ZipUtil.tarBz2(source, tbz2);
        ZipUtil.tarXz(source, txz);

        ZipUtil.unTarGz(tgz, tempDir.resolve("tgz-out"));
        ZipUtil.unTarBz2(tbz2, tempDir.resolve("tbz2-out"));
        ZipUtil.unTarXz(txz, tempDir.resolve("txz-out"));

        assertTrue(Files.exists(tempDir.resolve("tgz-out/source/a.txt")));
        assertTrue(Files.exists(tempDir.resolve("tbz2-out/source/a.txt")));
        assertTrue(Files.exists(tempDir.resolve("txz-out/source/a.txt")));
    }

    @Test
    void sevenZipShouldCreateAndExtract() throws Exception {
        Path source = ZipUtilTestSupport.sampleDir(tempDir);
        Path sevenZ = tempDir.resolve("sample.7z");
        Path out = tempDir.resolve("7z-out");

        ZipUtil.sevenZip(source, sevenZ);
        ZipUtil.unSevenZip(sevenZ, out);

        assertEquals("A", ZipUtilTestSupport.read(out.resolve("source/a.txt")));
    }

    @Test
    void packAndUnpackShouldUseFormat() throws Exception {
        Path source = ZipUtilTestSupport.text(tempDir, "p.txt", "pack");
        Path zip = tempDir.resolve("p.zip");
        Path out = tempDir.resolve("pack-out");

        ZipUtil.pack(source, zip, ZipUtil.ArchiveFormat.ZIP);
        ZipUtil.unpack(zip, out, ZipUtil.ArchiveFormat.ZIP);

        assertEquals("pack", ZipUtilTestSupport.read(out.resolve("p.txt")));
    }
}
