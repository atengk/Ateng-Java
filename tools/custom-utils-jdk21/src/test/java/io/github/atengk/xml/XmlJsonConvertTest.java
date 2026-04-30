package io.github.atengk.xml;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.atengk.utils.xml.XmlParseException;
import io.github.atengk.utils.xml.XmlUtil;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class XmlJsonConvertTest {

    @Test
    void shouldConvertXmlToJson() {
        String json = XmlUtil.xmlToJson("<user><name>Ateng</name></user>");

        assertTrue(json.contains("Ateng"));
        assertTrue(XmlUtil.xmlToJson("<user><name>Ateng</name></user>", true).contains("\n"));
    }

    @Test
    void shouldConvertJsonToXmlAndNode() {
        String xml = XmlUtil.jsonToXml("{\"name\":\"Ateng\"}", "user");
        JsonNode node = XmlUtil.xmlToJsonNode(xml);

        assertTrue(xml.contains("Ateng"));
        assertEquals("Ateng", XmlUtil.getText(node, "name"));
        assertTrue(XmlUtil.jsonNodeToXml(node).contains("Ateng"));
    }

    @Test
    void shouldConvertMapAliases() {
        Map<String, Object> map = XmlUtil.xmlToMap("<user><name>Ateng</name></user>");
        String xml = XmlUtil.mapToXml(map, "user");

        assertEquals("Ateng", map.get("name"));
        assertTrue(xml.contains("Ateng"));
        assertThrows(XmlParseException.class, () -> XmlUtil.jsonToXml("{"));
    }
}
