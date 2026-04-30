package io.github.atengk.xml;

import com.fasterxml.jackson.core.type.TypeReference;
import io.github.atengk.utils.xml.XmlParseException;
import io.github.atengk.utils.xml.XmlUtil;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class XmlObjectConvertTest {

    @Test
    void shouldConvertObjectToXmlAndBack() {
        TestModels.User user = new TestModels.User("Ateng", 18, true);

        String xml = XmlUtil.toXml(user);
        TestModels.User actual = XmlUtil.fromXml(xml, TestModels.User.class);

        assertTrue(xml.contains("<name>Ateng</name>"));
        assertEquals("Ateng", actual.getName());
        assertEquals(18, actual.getAge());
        assertTrue(actual.getActive());
    }

    @Test
    void shouldConvertWithTypeReferenceAndConvertObject() {
        String xml = "<root><name>Ateng</name><age>18</age></root>";

        Map<String, Object> map = XmlUtil.fromXml(xml, new TypeReference<>() {});
        TestModels.User user = XmlUtil.convert(map, TestModels.User.class);

        assertEquals("Ateng", map.get("name"));
        assertEquals("Ateng", user.getName());
    }

    @Test
    void shouldRejectNullOrInvalidXml() {
        assertThrows(IllegalArgumentException.class, () -> XmlUtil.toXml(null));
        assertThrows(IllegalArgumentException.class, () -> XmlUtil.fromXml("", TestModels.User.class));
        assertThrows(XmlParseException.class, () -> XmlUtil.fromXml("<user>", TestModels.User.class));
    }
}
