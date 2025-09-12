package local.ateng.java.mybatisjdk8;

import local.ateng.java.customutils.utils.XmlUtil;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

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


    private static final String SAMPLE_XML =
            "<ns:Order xmlns:ns=\"http://example.com\">" +
                    "  <ns:Id>1001</ns:Id>" +
                    "  <ns:Item><ns:Name>Book</ns:Name><ns:Qty>2</ns:Qty></ns:Item>" +
                    "  <ns:Item><ns:Name>Pen</ns:Name><ns:Qty>5</ns:Qty></ns:Item>" +
                    "</ns:Order>";

    @Test
    public void testParseXmlAndToXml() {
        Document doc = XmlUtil.parseXml(SAMPLE_XML);
        System.out.println(doc);

        String xmlStr = XmlUtil.toXml(doc);
        System.out.println(xmlStr);
    }

    @Test
    public void testXmlToMapAndBack() {
        Map<String, Object> map = XmlUtil.toMap(SAMPLE_XML);
        System.out.println(map);

        String xml = XmlUtil.toXml("Order", map);
        System.out.println(xml);
    }

    @Test
    public void testBeanMapping() {
        // 简单 POJO
        Order order = new Order();
        order.setId(123);
        order.setName("TestOrder");

        String xml = XmlUtil.toXml(order);
        System.out.println(xml);

        Order parsed = XmlUtil.fromXml(xml, Order.class);
        System.out.println(parsed);
    }

    @Test
    public void testNodeOperations() {
        Document doc = XmlUtil.parseXml(SAMPLE_XML);
        Element root = doc.getDocumentElement();

        String id = XmlUtil.getChildText(root, "ns:Id");
        assertEquals("1001", id);

        assertNull(XmlUtil.getChildText(root, "NonExist"));
        assertTrue(XmlUtil.hasChild(root, "ns:Item"));

        Element newChild = XmlUtil.appendChild(root, "ns:Remark", "TestRemark");
        assertEquals("TestRemark", newChild.getTextContent());

        XmlUtil.setAttr(newChild, "type", "note");
        assertEquals("note", XmlUtil.getAttr(newChild, "type"));

        XmlUtil.removeAttr(newChild, "type");
        assertNull(XmlUtil.getAttr(newChild, "type"));
    }

    @Test
    public void testXPathAndNamespace() {
        Document doc = XmlUtil.parseXml(SAMPLE_XML);
        Map<String, String> nsMap = Collections.singletonMap("ns", "http://example.com");

        List<Node> items = XmlUtil.getNodesByXPath(doc, "//ns:Item", nsMap);
        assertEquals(2, items.size());

        String firstItemName = XmlUtil.getByXPath(doc, "//ns:Item[1]/ns:Name");
        assertEquals("Book", firstItemName);
    }

    @Test
    public void testAdvancedMethods() {
        String formatted = XmlUtil.formatXml(SAMPLE_XML);
        assertTrue(formatted.contains("\n"));

        String compressed = XmlUtil.compressXml(formatted);
        assertFalse(compressed.contains("\n"));

        String noNs = XmlUtil.removeNamespace(SAMPLE_XML);
        assertFalse(noNs.contains("xmlns:ns"));
        assertFalse(noNs.contains("ns:"));

        Document doc = XmlUtil.parseXml(SAMPLE_XML);
        XmlUtil.removeEmptyTextNodes(doc);
        // 验证空文本节点被移除，可检查第一个子节点文本
        Element root = doc.getDocumentElement();
        assertEquals("1001", XmlUtil.getChildText(root, "ns:Id"));

        Document cloned = XmlUtil.cloneDocument(doc);
        assertNotNull(cloned);
        assertEquals("1001", XmlUtil.getChildText(cloned.getDocumentElement(), "ns:Id"));
    }

    // ================== 测试用 POJO ==================
    public static class Order {
        private int id;
        private String name;

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

}
