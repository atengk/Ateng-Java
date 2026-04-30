package io.github.atengk.xml;

import io.github.atengk.utils.xml.XmlUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class XmlSoapTest {

    private static final String SOAP = """
            <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
              <soap:Header><token>abc</token></soap:Header>
              <soap:Body><ping><name>Ateng</name></ping></soap:Body>
            </soap:Envelope>
            """;

    @Test
    void shouldDetectSoapAndReadBodyHeader() {
        assertTrue(XmlUtil.isSoap(SOAP));
        assertTrue(XmlUtil.getSoapHeader(SOAP).contains("token"));
        assertTrue(XmlUtil.getSoapBody(SOAP).contains("ping"));
    }

    @Test
    void shouldWrapAndUnwrapSoap() {
        String soap = XmlUtil.wrapSoapEnvelope("<ping><name>Ateng</name></ping>");

        assertTrue(XmlUtil.isSoap(soap));
        assertTrue(XmlUtil.unwrapSoapEnvelope(soap).contains("Ateng"));
    }

    @Test
    void shouldDetectSoapFault() {
        String fault = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:Body><soap:Fault><faultstring>bad</faultstring></soap:Fault></soap:Body></soap:Envelope>";

        assertTrue(XmlUtil.hasSoapFault(fault));
        assertTrue(XmlUtil.getSoapFault(fault).contains("bad"));
        assertFalse(XmlUtil.isSoap("<user/>"));
    }
}
