package io.github.atengk.xml;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.atengk.utils.xml.XmlUtil;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class XmlMapAndTreeTest {

    @Test
    void shouldConvertMapAndFlatMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", "Ateng");
        map.put("age", 18);

        String xml = XmlUtil.mapToXml(map, "user");
        Map<String, Object> actual = XmlUtil.fromXmlToMap(xml);
        Map<String, Object> flat = XmlUtil.fromXmlToFlatMap("<user><profile><name>Ateng</name></profile></user>");

        assertEquals("Ateng", actual.get("name"));
        assertEquals("Ateng", flat.get("profile.name"));
    }

    @Test
    void shouldConvertTree() {
        JsonNode node = XmlUtil.readTree("<user><name>Ateng</name><age>18</age><active>true</active></user>");

        assertEquals("Ateng", XmlUtil.getText(node, "name"));
        assertEquals(18, XmlUtil.getInt(node, "age"));
        assertTrue(XmlUtil.getBoolean(node, "active"));
        assertTrue(XmlUtil.writeTree(node).contains("Ateng"));
    }

    @Test
    void shouldConvertTreeToValueAndRejectInvalidInput() {
        JsonNode node = XmlUtil.toTree(new TestModels.User("Ateng", 18, true));
        TestModels.User actual = XmlUtil.treeToValue(node, TestModels.User.class);

        assertEquals("Ateng", actual.getName());
        assertThrows(IllegalArgumentException.class, () -> XmlUtil.readTree(""));
    }
}
