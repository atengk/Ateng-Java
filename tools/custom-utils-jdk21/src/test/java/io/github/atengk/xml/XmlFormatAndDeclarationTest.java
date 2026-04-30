package io.github.atengk.xml;

import io.github.atengk.utils.xml.XmlUtil;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class XmlFormatAndDeclarationTest {

    @Test
    void shouldFormatCompactAndNormalize() {
        String xml = "<user><name>Ateng</name></user>";

        assertTrue(XmlUtil.format(xml).contains(System.lineSeparator()) || XmlUtil.format(xml).contains("\n"));
        assertEquals(xml, XmlUtil.compact("<user>  <name>Ateng</name> </user>"));
        assertEquals(xml, XmlUtil.normalize("<?xml version=\"1.0\" encoding=\"UTF-8\"?><user><name>Ateng</name></user>"));
    }

    @Test
    void shouldHandleXmlDeclaration() {
        String xml = XmlUtil.addXmlDeclaration("<user/>", StandardCharsets.UTF_8);

        assertTrue(XmlUtil.hasXmlDeclaration(xml));
        assertEquals("1.0", XmlUtil.getXmlVersion(xml));
        assertEquals("UTF-8", XmlUtil.getXmlEncoding(xml));
        assertEquals("<user/>", XmlUtil.removeXmlDeclaration(xml));
    }

    @Test
    void shouldEscapeAndRejectBadIndent() {
        String escaped = XmlUtil.escapeText("<a&b>");

        assertEquals("&lt;a&amp;b&gt;", escaped);
        assertEquals("<a&b>", XmlUtil.unescapeText(escaped));
        assertThrows(IllegalArgumentException.class, () -> XmlUtil.format("<user/>", -1));
    }
}
