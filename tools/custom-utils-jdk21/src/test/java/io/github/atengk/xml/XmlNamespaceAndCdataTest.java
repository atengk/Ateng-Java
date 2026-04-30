package io.github.atengk.xml;

import io.github.atengk.utils.xml.XmlUtil;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class XmlNamespaceAndCdataTest {

    @Test
    void shouldHandleNamespace() {
        String xml = "<p:user xmlns:p=\"urn:test\"><p:name>Ateng</p:name></p:user>";
        Map<String, String> namespaces = XmlUtil.getNamespaces(xml);

        assertTrue(XmlUtil.hasNamespace(xml));
        assertEquals("urn:test", namespaces.get("p"));
        assertEquals("p", XmlUtil.getNamespacePrefix(xml, "urn:test"));
        assertTrue(XmlUtil.removeNamespaces(xml).contains("<user>"));
    }

    @Test
    void shouldRenameNamespaceAndAddNamespace() {
        String xml = "<user xmlns=\"urn:old\"><name>Ateng</name></user>";
        String renamed = XmlUtil.renameNamespace(xml, "urn:old", "urn:new");
        String withNamespace = XmlUtil.toXmlWithNamespace(new TestModels.User("Ateng", 18, true), "urn:test");

        assertTrue(renamed.contains("urn:new"));
        assertTrue(withNamespace.contains("xmlns=\"urn:test\""));
    }

    @Test
    void shouldHandleCdata() {
        String cdata = XmlUtil.wrapCdata("a]]>b");
        String xml = "<user><bio><![CDATA[old]]></bio></user>";
        String replaced = XmlUtil.replaceCdata(xml, "user.bio", "new");

        assertTrue(XmlUtil.isCdata(cdata));
        assertTrue(XmlUtil.containsCdata(replaced));
        assertEquals("new", XmlUtil.getValue(replaced, "user.bio"));
    }
}
