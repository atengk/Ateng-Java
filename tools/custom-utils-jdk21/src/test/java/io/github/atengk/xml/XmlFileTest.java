package io.github.atengk.xml;

import io.github.atengk.utils.xml.XmlUtil;
import io.github.atengk.utils.xml.XmlValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class XmlFileTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldWriteReadAndParseXmlFile() {
        Path path = tempDir.resolve("user.xml");

        XmlUtil.writeXmlFile(path, new TestModels.User("Ateng", 18, true), true);
        TestModels.User actual = XmlUtil.fromXmlFile(path, TestModels.User.class);

        assertTrue(XmlUtil.existsXmlFile(path));
        assertTrue(XmlUtil.readXmlFile(path).contains("Ateng"));
        assertEquals("Ateng", actual.getName());
    }

    @Test
    void shouldBackupXmlFile() {
        Path path = tempDir.resolve("user.xml");
        XmlUtil.writeXmlFile(path, new TestModels.User("Ateng", 18, true));

        Path backup = XmlUtil.backupXmlFile(path);

        assertTrue(XmlUtil.existsXmlFile(backup));
        assertTrue(XmlUtil.readXmlFile(backup).contains("Ateng"));
    }

    @Test
    void shouldValidateReadableAndWritable() {
        Path missing = tempDir.resolve("missing.xml");

        assertThrows(XmlValidationException.class, () -> XmlUtil.validateReadable(missing));
        assertDoesNotThrow(() -> XmlUtil.validateWritable(tempDir.resolve("new.xml")));
    }
}
