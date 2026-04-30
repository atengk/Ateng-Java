package io.github.atengk.xml;

import io.github.atengk.utils.xml.XmlUtil;
import io.github.atengk.utils.xml.XmlValidationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class XmlSecurityTest {

    @Test
    void shouldCheckSizeAndDepth() {
        String xml = "<a><b><c>1</c></b></a>";

        assertDoesNotThrow(() -> XmlUtil.checkXmlSize(xml, 100));
        assertThrows(XmlValidationException.class, () -> XmlUtil.checkXmlSize(xml, 1));
        assertThrows(XmlValidationException.class, () -> XmlUtil.checkDepth(xml, 2));
    }

    @Test
    void shouldUseSafeRead() {
        String xml = "<user><name>Ateng</name></user>";

        assertEquals("Ateng", XmlUtil.getText(XmlUtil.safeReadTree(xml), "name"));
        assertEquals("Ateng", XmlUtil.safeFromXml(xml, TestModels.User.class).getName());
    }

    @Test
    void shouldResetSafeMapperAndSanitize() {
        String xml = "<!ENTITY xxe SYSTEM \"file:///tmp/a\"><user/>";

        assertDoesNotThrow(XmlUtil::disableExternalEntity);
        assertDoesNotThrow(XmlUtil::disableDtd);
        assertDoesNotThrow(XmlUtil::enableSecureProcessing);
        assertFalse(XmlUtil.sanitize(xml).contains("ENTITY"));
    }
}
