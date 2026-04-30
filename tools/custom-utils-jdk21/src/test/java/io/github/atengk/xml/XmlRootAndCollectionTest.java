package io.github.atengk.xml;

import io.github.atengk.utils.xml.XmlUtil;
import io.github.atengk.utils.xml.XmlValidationException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class XmlRootAndCollectionTest {

    @Test
    void shouldHandleRootName() {
        String xml = XmlUtil.toXmlWithRoot(new TestModels.User("Ateng", 18, true), "member");

        assertEquals("member", XmlUtil.getRootName(xml));
        assertTrue(XmlUtil.hasRootName(xml, "member"));
        assertTrue(XmlUtil.renameRoot(xml, "user").startsWith("<user>"));
    }

    @Test
    void shouldWrapAndUnwrapRoot() {
        String wrapped = XmlUtil.wrapRoot("<name>Ateng</name>", "user");

        assertEquals("user", XmlUtil.getRootName(wrapped));
        assertEquals("<name>Ateng</name>", XmlUtil.unwrapRoot(wrapped));
    }

    @Test
    void shouldConvertCollection() {
        String xml = XmlUtil.toXmlList(List.of(new TestModels.User("A", 1, true), new TestModels.User("B", 2, false)), "users", "user");
        List<TestModels.User> users = XmlUtil.fromXmlList(xml, "user", TestModels.User.class);

        assertEquals(2, users.size());
        assertEquals("A", users.getFirst().getName());
        assertThrows(XmlValidationException.class, () -> XmlUtil.fromXmlWithRoot(xml, "wrong", TestModels.User.class));
    }
}
