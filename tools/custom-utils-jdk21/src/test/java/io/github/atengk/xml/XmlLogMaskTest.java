package io.github.atengk.xml;

import io.github.atengk.utils.xml.XmlUtil;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class XmlLogMaskTest {

    private static final String XML = "<user><name>Ateng</name><password>123456</password><phone>13800138000</phone></user>";

    @Test
    void shouldCreateSummary() {
        String summary = XmlUtil.summary("<user>   <name>Ateng</name> </user>", 12);

        assertTrue(summary.endsWith("..."));
        assertThrows(IllegalArgumentException.class, () -> XmlUtil.summary(XML, -1));
    }

    @Test
    void shouldMaskByPathAndTagName() {
        String maskedByPath = XmlUtil.mask(XML, Map.of("user.password", "***"));
        String maskedByTag = XmlUtil.maskByTagName(XML, List.of("phone"));

        assertTrue(maskedByPath.contains("***"));
        assertTrue(maskedByTag.contains("******"));
    }

    @Test
    void shouldRemoveSensitiveNodesAndCreateSafeLog() {
        String removed = XmlUtil.removeSensitiveNodes(XML, List.of("user.password"));
        String safe = XmlUtil.safeLogXml(XML);

        assertFalse(removed.contains("password"));
        assertFalse(safe.contains("123456"));
        assertFalse(safe.contains("13800138000"));
    }
}
