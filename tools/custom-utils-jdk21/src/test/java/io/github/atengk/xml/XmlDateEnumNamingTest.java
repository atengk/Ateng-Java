package io.github.atengk.xml;

import io.github.atengk.utils.xml.XmlUtil;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class XmlDateEnumNamingTest {

    @Test
    void shouldFormatAndParseDateTime() {
        XmlUtil.setDateTimeFormat("yyyy-MM-dd HH:mm:ss");
        LocalDateTime time = LocalDateTime.of(2026, 4, 30, 10, 20, 30);

        String text = XmlUtil.formatDateTime(time);
        LocalDateTime parsed = XmlUtil.parseDateTime(text);

        assertEquals("2026-04-30 10:20:30", text);
        assertEquals(time, parsed);
    }

    @Test
    void shouldUseNamingStrategies() {
        try {
            XmlUtil.useSnakeCase();
            String snake = XmlUtil.toXml(new TestModels.Person("Ateng", null));
            XmlUtil.useCamelCase();
            String camel = XmlUtil.toXml(new TestModels.Person("Ateng", null));

            assertTrue(snake.contains("first_name"));
            assertTrue(camel.contains("firstName"));
        } finally {
            XmlUtil.useCamelCase();
        }
    }

    @Test
    void shouldSetDateEnumAndIncludeOptions() {
        assertDoesNotThrow(() -> XmlUtil.setDateFormat("yyyy-MM-dd"));
        assertDoesNotThrow(() -> XmlUtil.setEnumAsString(true));
        assertDoesNotThrow(() -> XmlUtil.setEnumUsingToString(false));
        assertDoesNotThrow(XmlUtil::includeNonNull);
        assertDoesNotThrow(XmlUtil::includeAlways);
        assertDoesNotThrow(XmlUtil::ignoreUnknownProperties);
        assertNotNull(XmlUtil.enableXmlText());
        assertNotNull(XmlUtil.enableXmlAttribute());
    }
}
