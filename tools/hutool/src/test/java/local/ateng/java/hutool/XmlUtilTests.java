package local.ateng.java.hutool;

import cn.hutool.core.util.XmlUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.xpath.XPathConstants;

/**
 * XML工具-XmlUtil
 * 简化XML的创建、读和写的过程
 *
 * @author 孔余
 * @since 2024-02-23 10:27
 */
public class XmlUtilTests {
    String jsonStr = "{\"returnsms\":{\"returnstatus\":\"Success（成功）\",\"message\":\"ok\",\"remainpoint\":1490,\"taskID\":885,\"successCounts\":1,\"user\":{\"name\":\"ateng\",\"age\":24}}}";
    String xmlStr = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
            "\n" +
            "<returnsms> \n" +
            "  <returnstatus>Success（成功）</returnstatus>  \n" +
            "  <message>ok</message>  \n" +
            "  <remainpoint>1490</remainpoint>  \n" +
            "  <taskID>885</taskID>  \n" +
            "  <successCounts>1</successCounts>\n" +
            "  <user>\n" +
            "    <name>ateng</name>\n" +
            "    <age>24</age>\n" +
            "  </user>\n" +
            "</returnsms>";

    /**
     * 除XML文本中的无效字符
     */
    @Test
    void test00() {
        String xml = XmlUtil.cleanInvalid(xmlStr);
        System.out.println(xml);
        //System.out.println(XmlUtil.toStr(XmlUtil.readXML(xmlStr)));
    }

    /**
     * 通过XPath方式读取XML节点等信息
     */
    @Test
    void test01() {
        Document document = XmlUtil.readXML(xmlStr);
        Object value = XmlUtil.getByXPath("/returnsms/message/text()", document, XPathConstants.STRING);
        System.out.println(value);
    }

    @Test
    void test02() {
        Document document = XmlUtil.readXML(xmlStr);
        Object value = XmlUtil.getByXPath("/returnsms/user/name/text()", document, XPathConstants.STRING);
        System.out.println(value);
    }

    /**
     * 将XML转换为JSON
     */
    @Test
    void test03() {
        String str = JSONUtil.parseFromXml(xmlStr).toJSONString(0);
        System.out.println(str);
    }

    /**
     * 将JSON转换为XML
     */
    @Test
    void test04() {
        String str = JSONUtil.toXmlStr(new JSONObject(jsonStr));
        System.out.println(str);
        String json = "{\"name\":\"Tom\",\"age\":18}";
        System.out.println(JSONUtil.toXmlStr(new JSONObject(json)));

    }

    /**
     * 对XML对新增和获取
     */
    @Test
    void test05() {
        // 读取XML
        Document document = XmlUtil.readXML(xmlStr);
        // 获取根元素
        Element root = document.getDocumentElement();
        // 添加新元素
        Element newElement = document.createElement("newElement");
        newElement.setTextContent("This is a new element");
        root.appendChild(newElement);
        // 创建子元素并添加到根元素下
        Element childElement = document.createElement("child");
        root.appendChild(childElement);
        // 创建孙子元素并添加到子元素下
        Element grandChildElement = document.createElement("grandchild");
        grandChildElement.setTextContent("This is a grandchild element");
        childElement.appendChild(grandChildElement);
        // 获取指定元素
        Element messageElement = (Element) XmlUtil.getByXPath("/returnsms/message", root, XPathConstants.NODE);
        System.out.println("Message: " + messageElement.getTextContent());
        // 输出XML内容
        String xmlContent = XmlUtil.toStr(document);
        System.out.println(xmlContent);

//        String str = JSONUtil.parseFromXml(xmlContent).toJSONString(4);
//        System.out.println(str);

    }
}
