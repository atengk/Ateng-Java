package io.github.atengk.xml;

import io.github.atengk.utils.xml.XmlUtil;
import io.github.atengk.utils.xml.XmlValidationException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class XmlQueryAndExtractTest {

    private static final String XML = "<user><name>Ateng</name><roles><role>admin</role><role>dev</role></roles></user>";

    @Test
    void shouldGetValueAndValues() {
        assertEquals("Ateng", XmlUtil.getValue(XML, "user.name"));
        assertEquals(List.of("admin", "dev"), XmlUtil.getValues(XML, "user.roles.role"));
    }

    @Test
    void shouldCheckPathAndExtractMap() {
        Map<String, String> result = XmlUtil.extractToMap(XML, List.of("user.name", "user.roles.role"));

        assertTrue(XmlUtil.containsPath(XML, "user.name"));
        assertTrue(XmlUtil.containsValue(XML, "user.name", "Ateng"));
        assertEquals("Ateng", result.get("user.name"));
    }

    @Test
    void shouldThrowWhenRequiredValueMissing() {
        assertThrows(XmlValidationException.class, () -> XmlUtil.getRequiredValue(XML, "user.missing"));
        assertThrows(IllegalArgumentException.class, () -> XmlUtil.getValue(XML, ""));
    }
}
