package io.github.atengk.xml;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import io.github.atengk.utils.xml.XmlException;
import io.github.atengk.utils.xml.XmlParseException;
import io.github.atengk.utils.xml.XmlUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class XmlExceptionAndMapperConfigTest {

    @Test
    void shouldWrapExceptions() {
        Exception source = new IllegalStateException("bad");

        assertEquals("bad", XmlUtil.getErrorMessage(source));
        assertInstanceOf(XmlException.class, XmlUtil.parseException(source));
        assertInstanceOf(XmlException.class, XmlUtil.wrapException("wrap", source));
        assertThrows(XmlParseException.class, () -> XmlUtil.throwParseError("<user/>", source));
    }

    @Test
    void shouldHandleMapperConfig() {
        XmlMapper original = XmlUtil.getMapper();
        XmlMapper copy = XmlUtil.copyMapper();

        assertNotNull(original);
        assertNotSame(original, copy);
        assertDoesNotThrow(() -> XmlUtil.configure(mapper -> mapper.findAndRegisterModules()));
        assertDoesNotThrow(() -> XmlUtil.setMapper(XmlUtil.newSafeMapper()));
    }

    @Test
    void shouldSetCommonMapperOptions() {
        assertDoesNotThrow(() -> XmlUtil.setDefaultUseWrapper(false));
        assertDoesNotThrow(() -> XmlUtil.setIncludeNull(false));
        assertDoesNotThrow(() -> XmlUtil.setFailOnUnknownProperties(false));
        assertDoesNotThrow(() -> XmlUtil.registerJavaTimeModule());
    }
}
