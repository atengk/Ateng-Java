package io.github.atengk.xml;

import io.github.atengk.utils.xml.XmlUtil;
import io.github.atengk.utils.xml.XmlValidationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class XmlValidationAndXsdTest {

    private static final String XSD = """
            <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema">
              <xs:element name="user">
                <xs:complexType>
                  <xs:sequence>
                    <xs:element name="name" type="xs:string"/>
                  </xs:sequence>
                </xs:complexType>
              </xs:element>
            </xs:schema>
            """;

    @Test
    void shouldValidateBasicXml() {
        assertTrue(XmlUtil.isXml("<user><name>Ateng</name></user>"));
        assertFalse(XmlUtil.isXml("<user>"));
        assertDoesNotThrow(() -> XmlUtil.validateWellFormed("<user/>"));
    }

    @Test
    void shouldValidateByXsd() {
        String valid = "<user><name>Ateng</name></user>";
        String invalid = "<user><age>18</age></user>";

        assertDoesNotThrow(() -> XmlUtil.validateByXsd(valid, XSD));
        assertTrue(XmlUtil.isValidByXsd(valid, XSD));
        assertFalse(XmlUtil.isValidByXsd(invalid, XSD));
        assertFalse(XmlUtil.getXsdValidationErrors(invalid, XSD).isEmpty());
    }

    @Test
    void shouldValidateBlankAndRoot() {
        assertFalse(XmlUtil.isBlankXml("<user/>"));
        assertThrows(IllegalArgumentException.class, () -> XmlUtil.validateNotBlank(""));
        assertThrows(XmlValidationException.class, () -> XmlUtil.validateRootName("<user/>", "order"));
    }
}
