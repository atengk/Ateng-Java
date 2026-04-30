package io.github.atengk.zip;

import io.github.atengk.utils.zip.ZipUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ZipUtilEntryManageTest {

    @TempDir
    Path tempDir;

    @Test
    void addReplaceRenameRemoveAndCopyEntryShouldWork() throws Exception {
        Path zip = tempDir.resolve("entry.zip");
        ZipUtil.zipText("a.txt", "A", zip);
        ZipUtil.addTextEntry(zip, "b.txt", "B");
        assertTrue(ZipUtil.entryExists(zip, "b.txt"));

        Path replacement = ZipUtilTestSupport.text(tempDir, "new.txt", "NEW");
        ZipUtil.replaceEntry(zip, "b.txt", replacement);
        ZipUtil.renameEntry(zip, "b.txt", "renamed.txt");
        assertTrue(ZipUtil.entryExists(zip, "renamed.txt"));

        Path copiedZip = tempDir.resolve("copied.zip");
        ZipUtil.copyEntry(zip, "renamed.txt", copiedZip);
        assertTrue(ZipUtil.entryExists(copiedZip, "renamed.txt"));

        ZipUtil.removeEntry(zip, "renamed.txt");
        assertFalse(ZipUtil.entryExists(zip, "renamed.txt"));
    }

    @Test
    void addEntriesShouldAcceptMultipleFiles() throws Exception {
        Path zip = tempDir.resolve("multi-entry.zip");
        Path one = ZipUtilTestSupport.text(tempDir, "one.txt", "1");
        Path two = ZipUtilTestSupport.text(tempDir, "two.txt", "2");

        ZipUtil.addEntries(zip, List.of(one, two));

        assertEquals(2, ZipUtil.listFileEntries(zip).size());
    }

    @Test
    void removeMissingEntryShouldNotBreakZip4jSemantics() throws Exception {
        Path zip = tempDir.resolve("missing-entry.zip");
        ZipUtil.zipText("a.txt", "A", zip);
        assertDoesNotThrow(() -> ZipUtil.removeEntry(zip, "missing.txt"));
    }
}
