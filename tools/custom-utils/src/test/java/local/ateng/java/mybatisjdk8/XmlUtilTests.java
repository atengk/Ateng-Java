package local.ateng.java.mybatisjdk8;

import local.ateng.java.customutils.utils.XmlUtil;
import org.junit.jupiter.api.Test;

public class XmlUtilTests {

    @Test
    void isValidXml() {
        String xml1 = "<name>Tom</name><age>18</age>";
        String xml2 = "<root><name>Tom</name><age>18</age></root>";
        String xml3 = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:tic=\"http://ticket.example.com/\"><name>Tom</name><age>18</age></soapenv:Envelope>";
        String xml4 = "<soapenv xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:tic=\"http://ticket.example.com/\"><name>Tom</name><age>18</age></soapenv>";

        System.out.println(XmlUtil.isValidXml(xml1)); // 返回 "Tom"
        System.out.println(XmlUtil.isValidXml(xml2)); // 返回 {"name":"Tom","age":"18"}
        System.out.println(XmlUtil.isValidXml(xml3)); // 返回 {"name":"Tom","age":"18"}
        System.out.println(XmlUtil.isValidXml(xml4)); // 返回 {"name":"Tom","age":"18"}

    }

    @Test
    void xmlToJson() {
        String xml1 = "<name>Tom</name><age>18</age>";
        String xml2 = "<root><name>Tom</name><age>18</age></root>";
        String xml3 = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:tic=\"http://ticket.example.com/\"><name>Tom</name><age>18</age></soapenv:Envelope>";
        String xml4 = "<soapenv xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:tic=\"http://ticket.example.com/\"><name>Tom</name><age>18</age></soapenv>";

        System.out.println(XmlUtil.xmlToJson(xml1)); // 返回 "Tom"
        System.out.println(XmlUtil.xmlToJson(xml2)); // 返回 {"name":"Tom","age":"18"}
        System.out.println(XmlUtil.xmlToJson(xml3)); // 返回 {"name":"Tom","age":"18"}
        System.out.println(XmlUtil.xmlToJson(xml4)); // 返回 {"name":"Tom","age":"18"}
    }

    @Test
    void jsonToXml() {
        String json = "{\"name\":\"Tom\",\"age\":18}";

        // 1. 无根标签的 XML 片段
        System.out.println(XmlUtil.jsonToXml(json));

        // 2. 有简单根标签的标准 XML
        System.out.println(XmlUtil.jsonToXml(json, "root"));

        // 3. 有复杂带命名空间的根标签的 XML
        String ns = "xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:tic=\"http://ticket.example.com/\"";
        System.out.println(XmlUtil.jsonToXml(json, "soapenv:Envelope", ns));
        System.out.println(XmlUtil.jsonToXml(json, "soapenv", ns));

    }

}
