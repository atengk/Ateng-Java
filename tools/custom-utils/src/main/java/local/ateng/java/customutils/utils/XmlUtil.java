package local.ateng.java.customutils.utils;

import org.w3c.dom.*;

import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.util.*;

/**
 * XML 工具类，提供对 XML 文件的常用操作，包括读取、写入、解析、创建等。
 *
 * @author Ateng
 * @since 2025-07-19
 */
public final class XmlUtil {

    /**
     * 禁止实例化工具类
     */
    private XmlUtil() {
        throw new UnsupportedOperationException("工具类不可实例化");
    }

    /**
     * 解析 XML 文件并返回 Document 对象
     *
     * @param file XML 文件路径
     * @return Document 对象，表示 XML 文件的 DOM 树
     * @throws Exception 如果解析过程中发生错误
     */
    public static Document parseXml(String file) throws Exception {
        // 获取 DocumentBuilderFactory 实例
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();

        // 解析文件并返回 Document 对象
        return builder.parse(new File(file));
    }

    /**
     * 将 Document 对象写入到指定文件
     *
     * @param document Document 对象
     * @param file     输出的 XML 文件路径
     * @throws Exception 如果写入过程中发生错误
     */
    public static void writeXml(Document document, String file) throws Exception {
        // 创建 TransformerFactory 实例
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();

        // 设置输出格式
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

        // 输出到指定文件
        StreamResult result = new StreamResult(new File(file));
        DOMSource source = new DOMSource(document);
        transformer.transform(source, result);
    }

    /**
     * 获取指定 XML 元素的文本内容
     *
     * @param element XML 元素
     * @return 元素的文本内容
     */
    public static String getElementText(Element element) {
        // 返回元素的文本内容
        return element.getTextContent().trim();
    }

    /**
     * 根据标签名称获取 XML 文档中的所有元素
     *
     * @param document XML 文档对象
     * @param tagName  标签名称
     * @return 包含指定标签名称元素的 NodeList
     */
    public static NodeList getElementsByTagName(Document document, String tagName) {
        // 返回文档中指定标签名称的所有元素
        return document.getElementsByTagName(tagName);
    }

    /**
     * 创建一个新的 XML 元素
     *
     * @param document XML 文档对象
     * @param tagName  元素标签名称
     * @param text     内容
     * @return 创建的 Element 对象
     */
    public static Element createElement(Document document, String tagName, String text) {
        // 创建元素并设置文本内容
        Element element = document.createElement(tagName);
        element.setTextContent(text);
        return element;
    }

    /**
     * 将多个节点追加到父节点下
     *
     * @param parent 父节点
     * @param nodes  需要追加的节点集合
     */
    public static void appendChildNodes(Node parent, List<Node> nodes) {
        for (Node node : nodes) {
            parent.appendChild(node);
        }
    }

    /**
     * 将 XML 文档转换为字符串形式
     *
     * @param document XML 文档对象
     * @return XML 文档的字符串表示
     * @throws TransformerException 如果转换过程中发生错误
     */
    public static String documentToString(Document document) throws TransformerException {
        // 创建 Transformer 实例
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();

        // 将文档转换为字符串
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(document), new StreamResult(writer));
        return writer.toString();
    }

    /**
     * 从 XML 字符串解析为 Document 对象
     *
     * @param xmlString XML 字符串
     * @return 解析后的 Document 对象
     * @throws Exception 如果解析过程中发生错误
     */
    public static Document parseXmlFromString(String xmlString) throws Exception {
        // 获取 DocumentBuilderFactory 实例
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();

        // 将字符串转换为输入流
        InputStream is = new ByteArrayInputStream(xmlString.getBytes("UTF-8"));

        // 解析 XML 字符串并返回 Document 对象
        return builder.parse(is);
    }

    /**
     * 获取 XML 元素的属性值
     *
     * @param element       元素
     * @param attributeName 属性名称
     * @return 属性值
     */
    public static String getAttributeValue(Element element, String attributeName) {
        // 获取属性值
        return element.getAttribute(attributeName);
    }

    /**
     * 获取 XML 文档中指定元素的子元素
     *
     * @param element 父元素
     * @param tagName 子元素标签名称
     * @return 子元素集合
     */
    public static NodeList getChildElementsByTagName(Element element, String tagName) {
        // 获取并返回子元素
        return element.getElementsByTagName(tagName);
    }

    /**
     * 判断 XML 文件是否有效（通过检查是否可以成功解析）
     *
     * @param file XML 文件路径
     * @return true 如果文件有效，false 如果文件无效
     */
    public static boolean isValidXml(String file) {
        try {
            parseXml(file);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 将 Map 转换为 XML 字符串
     * 支持嵌套结构
     *
     * @param map 需要转换的 Map 对象
     * @param rootElement 根元素名称
     * @return XML 字符串
     * @throws Exception 如果转换过程中发生错误
     */
    public static String mapToXml(Map<String, Object> map, String rootElement) throws Exception {
        // 创建 DocumentBuilderFactory 和 DocumentBuilder
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();

        // 创建一个新的 XML 文档
        Document document = builder.newDocument();

        // 创建根元素
        Element root = document.createElement(rootElement);
        document.appendChild(root);

        // 遍历 Map 并将键值对转换为 XML 元素
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            // 创建元素并设置文本内容或递归添加子元素
            Element element = document.createElement(key);

            if (value instanceof Map) {
                // 如果值是嵌套的 Map，递归调用 mapToXmlElement
                element.appendChild(mapToXmlElement(document, (Map<String, Object>) value));
            } else {
                // 否则直接设置值
                element.setTextContent(String.valueOf(value));
            }

            // 将元素附加到根元素
            root.appendChild(element);
        }

        // 转换 Document 为字符串
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

        // 将 Document 转换为字符串
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(document), new StreamResult(writer));
        return writer.toString();
    }

    /**
     * 递归地将 Map 转换为 XML 元素
     *
     * @param document XML 文档对象
     * @param map 需要转换的 Map
     * @return 生成的 XML 元素
     */
    private static Element mapToXmlElement(Document document, Map<String, Object> map) {
        // 遍历 Map 并将每个键值对转换为 XML 子元素
        Element element = document.createElement("nestedElement");  // 使用动态标签，不固定为 "nestedElement"

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            Element subElement = document.createElement(key);

            if (value instanceof Map) {
                // 如果值是嵌套的 Map，递归调用
                subElement.appendChild(mapToXmlElement(document, (Map<String, Object>) value));
            } else {
                // 否则直接设置值
                subElement.setTextContent(String.valueOf(value));
            }

            // 将子元素附加到当前元素
            element.appendChild(subElement);
        }

        return element;
    }

    /**
     * 将 XML 字符串转换为 Map
     * 支持嵌套结构
     *
     * @param xml XML 字符串
     * @return 转换后的 Map 对象
     * @throws Exception 如果解析过程中发生错误
     */
    public static Map<String, Object> xmlToMap(String xml) throws Exception {
        // 创建 DocumentBuilderFactory 和 DocumentBuilder
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();

        // 将 XML 字符串解析为 Document 对象
        InputStream is = new ByteArrayInputStream(xml.getBytes("UTF-8"));
        Document document = builder.parse(is);

        // 获取根元素
        Element rootElement = document.getDocumentElement();

        // 将 XML 元素转换为 Map
        return elementToMap(rootElement);
    }

    /**
     * 将 XML 元素转换为 Map
     * 支持嵌套结构
     *
     * @param element XML 元素
     * @return 转换后的 Map 对象
     */
    private static Map<String, Object> elementToMap(Element element) {
        Map<String, Object> map = new HashMap<>();

        // 获取元素的所有子元素
        NodeList nodeList = element.getChildNodes();

        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);

            // 只处理元素节点（忽略文本节点等）
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                String key = node.getNodeName();
                String value = node.getTextContent().trim();

                // 递归处理嵌套元素
                if (node.getChildNodes().getLength() > 1) {
                    map.put(key, elementToMap((Element) node)); // 递归嵌套元素
                } else {
                    map.put(key, value);
                }
            }
        }
        return map;
    }

}
