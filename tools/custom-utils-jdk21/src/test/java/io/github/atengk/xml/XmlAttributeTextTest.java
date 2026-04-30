package io.github.atengk.xml;

import io.github.atengk.utils.xml.XmlUtil;
import io.github.atengk.utils.xml.XmlValidationException;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class XmlAttributeTextTest {

    private static final String XML = "<book id=\"B001\"><title>Java</title></book>";

    @Test
    void shouldGetAttributes() {
        Map<String, String> attrs = XmlUtil.getAttributes(XML, "book");

        assertEquals("B001", XmlUtil.getAttribute(XML, "book", "id"));
        assertTrue(XmlUtil.hasAttribute(XML, "book", "id"));
        assertEquals("B001", attrs.get("id"));
    }

    @Test
    void shouldSetAndRemoveAttribute() {
        String updated = XmlUtil.setAttribute(XML, "book", "type", "tech");
        String removed = XmlUtil.removeAttribute(updated, "book", "id");

        assertEquals("tech", XmlUtil.getAttribute(updated, "book", "type"));
        assertNull(XmlUtil.getAttribute(removed, "book", "id"));
    }

    @Test
    void shouldGetAndSetTextContent() {
        String updated = XmlUtil.setTextContent(XML, "book.title", "JDK21");

        assertEquals("Java", XmlUtil.getTextContent(XML, "book.title"));
        assertEquals("JDK21", XmlUtil.getTextContent(updated, "book.title"));
        assertThrows(XmlValidationException.class, () -> XmlUtil.setAttribute(XML, "book.missing", "a", "b"));
    }
}
