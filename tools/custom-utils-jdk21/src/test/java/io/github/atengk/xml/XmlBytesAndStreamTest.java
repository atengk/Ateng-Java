package io.github.atengk.xml;

import io.github.atengk.utils.xml.XmlUtil;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class XmlBytesAndStreamTest {

    @Test
    void shouldConvertBytesWithCharset() {
        TestModels.User user = new TestModels.User("张三", 20, true);

        byte[] bytes = XmlUtil.toXmlBytes(user, StandardCharsets.UTF_8);
        TestModels.User actual = XmlUtil.fromXmlBytes(bytes, StandardCharsets.UTF_8, TestModels.User.class);

        assertEquals("张三", actual.getName());
        assertEquals(StandardCharsets.UTF_8, XmlUtil.detectCharset(XmlUtil.addXmlDeclaration(new String(bytes, StandardCharsets.UTF_8)).getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void shouldReadAndWriteStream() {
        TestModels.User user = new TestModels.User("Ateng", 18, true);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        XmlUtil.writeXml(outputStream, user);
        String xml = XmlUtil.readXml(new ByteArrayInputStream(outputStream.toByteArray()));
        TestModels.User actual = XmlUtil.fromXml(new StringReader(xml), TestModels.User.class);

        assertEquals("Ateng", actual.getName());
    }

    @Test
    void shouldReadAndWriteWriter() {
        StringWriter writer = new StringWriter();

        XmlUtil.writeXml(writer, new TestModels.User("Ateng", 18, true));

        assertTrue(XmlUtil.readXml(new StringReader(writer.toString())).contains("Ateng"));
        assertThrows(IllegalArgumentException.class, () -> XmlUtil.fromXmlBytes(new byte[0], TestModels.User.class));
    }
}
