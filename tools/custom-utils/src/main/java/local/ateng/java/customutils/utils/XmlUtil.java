package local.ateng.java.customutils.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.springframework.util.ObjectUtils;
import org.w3c.dom.*;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.*;

/**
 * XML 工具类
 *
 * <p>提供 XML 与 JSON 的互转功能，基于 Jackson 实现，支持复杂嵌套结构。
 * 适用于数据交换、接口对接等场景。</p>
 *
 * @author 孔余
 * @since 2025-08-11
 */
public final class XmlUtil {

    /**
     * JSON 解析器
     */
    private static final ObjectMapper JSON_MAPPER;

    /**
     * XML 解析器
     */
    private static final XmlMapper XML_MAPPER;

    static {
        JSON_MAPPER = new ObjectMapper();
        XML_MAPPER = new XmlMapper();
        // 宽松模式：忽略未知字段
        XML_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * 字符集名称
     */
    private static final String DEFAULT_CHARSET = "UTF-8";

    /**
     * 禁止实例化工具类
     */
    private XmlUtil() {
        throw new UnsupportedOperationException("工具类不可实例化");
    }

    /**
     * 判断字符串是否是标准 XML 格式
     *
     * @param xmlStr 待判断的 XML 字符串
     * @return true 表示是合法的 XML，false 表示不是
     */
    public static boolean isValidXml(String xmlStr) {
        if (ObjectUtils.isEmpty(xmlStr)) {
            return false;
        }
        try {
            XmlMapper xmlMapper = new XmlMapper();
            JsonNode node = xmlMapper.readTree(xmlStr.getBytes(DEFAULT_CHARSET));
            return node != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 将 XML 字符串转换为 JSON 字符串
     *
     * @param xml XML 字符串
     * @return JSON 字符串
     */
    public static String xmlToJson(String xml) {
        if (ObjectUtils.isEmpty(xml)) {
            return null;
        }
        try {
            JsonNode node = XML_MAPPER.readTree(xml.getBytes(DEFAULT_CHARSET));
            return JSON_MAPPER.writeValueAsString(node);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to convert XML to JSON", e);
        }
    }

    /**
     * 将 JSON 字符串转换为无根标签的 XML 片段
     *
     * @param json JSON 字符串
     * @return XML 片段字符串，无根标签，输入为空返回 null
     */
    public static String jsonToXml(String json) {
        if (ObjectUtils.isEmpty(json)) {
            return null;
        }
        try {
            JsonNode jsonNode = JSON_MAPPER.readTree(json.getBytes(DEFAULT_CHARSET));
            String xml = XML_MAPPER.writeValueAsString(jsonNode);
            JsonNode rootNode = XML_MAPPER.readTree(xml.getBytes(DEFAULT_CHARSET));
            StringBuilder sb = new StringBuilder();
            rootNode.fields().forEachRemaining(entry -> {
                try {
                    sb.append(XML_MAPPER.writer().withRootName(entry.getKey())
                            .writeValueAsString(entry.getValue()));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to convert JSON to XML fragment", e);
        }
    }

    /**
     * 将 JSON 字符串转换为带自定义根标签的标准 XML
     *
     * @param json     JSON 字符串
     * @param rootName 根标签名称，不能为空
     * @return 标准 XML 字符串，输入为空返回 null
     */
    public static String jsonToXml(String json, String rootName) {
        if (ObjectUtils.isEmpty(json)) {
            return null;
        }
        if (ObjectUtils.isEmpty(rootName)) {
            throw new IllegalArgumentException("rootName cannot be null or empty");
        }
        try {
            JsonNode jsonNode = JSON_MAPPER.readTree(json.getBytes(DEFAULT_CHARSET));
            return XML_MAPPER.writer().withRootName(rootName).writeValueAsString(jsonNode);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to convert JSON to XML with root", e);
        }
    }

    /**
     * 将 JSON 字符串转换为带复杂根元素（带命名空间）的 XML
     *
     * @param json                  JSON 字符串
     * @param rootName              根标签名称，例如 "soapenv:Envelope"
     * @param namespaceDeclarations xmlns 声明字符串，例如
     *                              "xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:tic=\"http://ticket.example.com/\""
     * @return 带复杂根的 XML 字符串，输入为空返回 null
     */
    public static String jsonToXml(String json, String rootName, String namespaceDeclarations) {
        if (ObjectUtils.isEmpty(json)) {
            return null;
        }
        if (ObjectUtils.isEmpty(rootName)) {
            throw new IllegalArgumentException("rootName cannot be null or empty");
        }
        try {
            // 先转无根 XML 片段
            String innerXml = jsonToXml(json);

            // 解析前缀和本地名
            String prefix = null;
            String localName = rootName;
            if (rootName.contains(":")) {
                String[] parts = rootName.split(":", 2);
                prefix = parts[0];
                localName = parts[1];
            }

            // 拼接根节点开始标签
            StringBuilder sb = new StringBuilder();
            sb.append('<').append(rootName);
            if (!ObjectUtils.isEmpty(namespaceDeclarations)) {
                sb.append(' ').append(namespaceDeclarations.trim());
            }
            sb.append('>');

            sb.append(innerXml);

            // 拼接根节点结束标签，带上前缀
            if (prefix != null) {
                sb.append("</").append(prefix).append(":").append(localName).append('>');
            } else {
                sb.append("</").append(localName).append('>');
            }

            return sb.toString();
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to convert JSON to XML with complex root", e);
        }
    }

    /**
     * 将 XML 字符串解析为 Document（带安全设置，防 XXE 攻击）
     *
     * @param xml XML 字符串
     * @return Document 对象
     * @throws RuntimeException 解析失败时抛出
     */
    public static Document parseXml(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // 安全设置，避免 XXE 注入
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(new InputSource(new StringReader(xml)));
        } catch (Exception e) {
            throw new RuntimeException("解析 XML 失败", e);
        }
    }

    /**
     * 将 Document 转换为 XML 字符串
     *
     * @param doc Document 对象
     * @return 格式化后的 XML 字符串
     */
    public static String toXml(Document doc) {
        try {
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(doc), new StreamResult(writer));
            return writer.toString();
        } catch (Exception e) {
            throw new RuntimeException("Document 转换 XML 字符串失败", e);
        }
    }

    // ================== Map 互转 ==================

    /**
     * 将 XML 字符串解析为 Map（保留层级结构）
     *
     * @param xml XML 字符串
     * @return Map 表示的 XML 数据
     */
    public static Map<String, Object> toMap(String xml) {
        Document doc = parseXml(xml);
        Element root = doc.getDocumentElement();
        return elementToMap(root);
    }

    /**
     * 将 Map 转换为 XML 字符串
     *
     * @param rootName 根节点名称
     * @param map      Map 数据
     * @return XML 字符串
     */
    public static String toXml(String rootName, Map<String, Object> map) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.newDocument();

            Element root = doc.createElement(rootName);
            doc.appendChild(root);

            mapToElement(doc, root, map);

            return toXml(doc);
        } catch (Exception e) {
            throw new RuntimeException("Map 转换 XML 失败", e);
        }
    }

    // ================== Jackson 对象映射 ==================

    /**
     * 将 XML 字符串解析为 Java 对象
     *
     * @param xml   XML 字符串
     * @param clazz 目标类型
     * @param <T>   泛型
     * @return Java 对象
     */
    public static <T> T fromXml(String xml, Class<T> clazz) {
        try {
            return XML_MAPPER.readValue(xml, clazz);
        } catch (Exception e) {
            throw new RuntimeException("XML 转换对象失败", e);
        }
    }

    /**
     * 将 Java 对象转换为 XML 字符串
     *
     * @param obj Java 对象
     * @return XML 字符串
     */
    public static String toXml(Object obj) {
        try {
            return XML_MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("对象转换 XML 失败", e);
        }
    }

    // ================== 私有工具方法 ==================

    /** 递归将 Element 转换为 Map */
    private static Map<String, Object> elementToMap(Element element) {
        Map<String, Object> map = new LinkedHashMap<>();

        // 处理属性
        NamedNodeMap attributes = element.getAttributes();
        if (attributes != null && attributes.getLength() > 0) {
            Map<String, String> attrMap = new LinkedHashMap<>();
            for (int i = 0; i < attributes.getLength(); i++) {
                Attr attr = (Attr) attributes.item(i);
                attrMap.put(attr.getName(), attr.getValue());
            }
            map.put("_attributes", attrMap);
        }

        // 处理子节点
        NodeList nodeList = element.getChildNodes();
        boolean hasElementChild = false;
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node instanceof Element) {
                hasElementChild = true;
                Map<String, Object> childMap = elementToMap((Element) node);
                map.merge(node.getNodeName(), childMap, (oldVal, newVal) -> {
                    if (oldVal instanceof List) {
                        ((List<Object>) oldVal).add(newVal);
                        return oldVal;
                    } else {
                        List<Object> list = new ArrayList<>();
                        list.add(oldVal);
                        list.add(newVal);
                        return list;
                    }
                });
            }
        }

        if (!hasElementChild) {
            map.put("_text", element.getTextContent().trim());
        }

        return map;
    }

    /** 递归将 Map 转换为 Element */
    private static void mapToElement(Document doc, Element parent, Map<String, Object> map) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if ("_attributes".equals(key) && value instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, String> attrs = (Map<String, String>) value;
                for (Map.Entry<String, String> attr : attrs.entrySet()) {
                    parent.setAttribute(attr.getKey(), attr.getValue());
                }
            } else if ("_text".equals(key)) {
                parent.setTextContent(String.valueOf(value));
            } else if (value instanceof Map) {
                Element child = doc.createElement(key);
                parent.appendChild(child);
                mapToElement(doc, child, (Map<String, Object>) value);
            } else if (value instanceof List) {
                for (Object item : (List<?>) value) {
                    Element child = doc.createElement(key);
                    parent.appendChild(child);
                    if (item instanceof Map) {
                        mapToElement(doc, child, (Map<String, Object>) item);
                    } else {
                        child.setTextContent(String.valueOf(item));
                    }
                }
            } else {
                Element child = doc.createElement(key);
                child.setTextContent(String.valueOf(value));
                parent.appendChild(child);
            }
        }
    }

    // ================== 节点 & 属性操作 ==================

    /**
     * 获取指定元素的子元素文本
     *
     * @param parent 父节点
     * @param tagName 子节点名称
     * @return 子节点文本，若不存在返回 null
     */
    public static String getChildText(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() == 0) {
            return null;
        }
        return nodes.item(0).getTextContent();
    }

    /**
     * 获取元素的属性值
     *
     * @param element 元素
     * @param attrName 属性名
     * @return 属性值，若不存在返回 null
     */
    public static String getAttr(Element element, String attrName) {
        return element.hasAttribute(attrName) ? element.getAttribute(attrName) : null;
    }

    /**
     * 设置元素的属性
     *
     * @param element 元素
     * @param attrName 属性名
     * @param attrValue 属性值
     */
    public static void setAttr(Element element, String attrName, String attrValue) {
        element.setAttribute(attrName, attrValue);
    }

    /**
     * 删除元素的属性
     *
     * @param element 元素
     * @param attrName 属性名
     */
    public static void removeAttr(Element element, String attrName) {
        element.removeAttribute(attrName);
    }

    // ================== 元素构建相关 ==================

    /**
     * 创建带命名空间的元素
     *
     * @param doc Document
     * @param qName 节点名（支持前缀，如 ns:user）
     * @param namespaceURI 命名空间 URI
     * @return 新建元素
     */
    public static Element createElementNS(Document doc, String qName, String namespaceURI) {
        return doc.createElementNS(namespaceURI, qName);
    }

    /**
     * 在父节点下新增子节点
     *
     * @param parent 父节点
     * @param tagName 子节点名
     * @param text 子节点文本（可为 null）
     * @return 新增的子节点
     */
    public static Element appendChild(Element parent, String tagName, String text) {
        Document doc = parent.getOwnerDocument();
        Element child = doc.createElement(tagName);
        if (text != null) {
            child.setTextContent(text);
        }
        parent.appendChild(child);
        return child;
    }

    // ================== XPath 查询 ==================

    /**
     * 使用 XPath 查询节点文本
     *
     * @param doc Document
     * @param expression XPath 表达式
     * @return 节点文本，若未找到返回 null
     */
    public static String getByXPath(Document doc, String expression) {
        try {
            javax.xml.xpath.XPath xpath = javax.xml.xpath.XPathFactory.newInstance().newXPath();
            return xpath.evaluate(expression, doc);
        } catch (Exception e) {
            throw new RuntimeException("XPath 查询失败: " + expression, e);
        }
    }

    // ================== 其他辅助 ==================

    /**
     * 判断元素下是否存在指定子节点
     *
     * @param parent 父节点
     * @param tagName 子节点名
     * @return true 存在，false 不存在
     */
    public static boolean hasChild(Element parent, String tagName) {
        return parent.getElementsByTagName(tagName).getLength() > 0;
    }

    /**
     * 克隆节点（深拷贝）
     *
     * @param node 节点
     * @param doc 目标 Document
     * @return 克隆后的节点
     */
    public static Node cloneNode(Node node, Document doc) {
        return doc.importNode(node, true);
    }

    /**
     * 合并一个元素到目标元素下
     *
     * @param target 目标元素
     * @param source 源元素
     */
    public static void mergeElement(Element target, Element source) {
        Document doc = target.getOwnerDocument();
        Node imported = doc.importNode(source, true);
        target.appendChild(imported);
    }

    // ================== 高级辅助方法 ==================

    /**
     * 格式化 XML 字符串（带缩进，便于调试）
     *
     * @param xml XML 字符串
     * @return 格式化后的 XML
     * @throws RuntimeException 格式化失败
     */
    public static String formatXml(String xml) {
        try {
            Document doc = parseXml(xml);
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(doc), new StreamResult(writer));
            return writer.toString();
        } catch (Exception e) {
            throw new RuntimeException("XML 格式化失败", e);
        }
    }

    /**
     * 压缩 XML（去除多余空格、换行，适合发送报文）
     *
     * @param xml XML 字符串
     * @return 压缩后的 XML
     */
    public static String compressXml(String xml) {
        return xml.replaceAll(">\\s+<", "><").trim();
    }

    /**
     * 移除 XML 中的命名空间前缀
     *
     * <p>在对接接口时，某些返回 XML 带命名空间前缀，处理麻烦，可用此方法清理</p>
     *
     * @param xml 原始 XML
     * @return 移除前缀后的 XML
     */
    public static String removeNamespace(String xml) {
        // 移除 xmlns 声明
        xml = xml.replaceAll("xmlns(:\\w+)?=\"[^\"]*\"", "");
        // 移除前缀，如 <ns:Order> -> <Order>
        xml = xml.replaceAll("<(/?)(\\w+):", "<$1");
        return xml;
    }

    /**
     * 根据 XPath 查询节点列表（支持命名空间）
     *
     * @param doc Document
     * @param expression XPath 表达式
     * @param nsMap 命名空间映射，如 {"ns":"http://example.com"}
     * @return 节点列表，未找到返回空列表
     */
    public static List<Node> getNodesByXPath(Document doc, String expression, Map<String, String> nsMap) {
        try {
            javax.xml.xpath.XPath xpath = javax.xml.xpath.XPathFactory.newInstance().newXPath();
            if (nsMap != null && !nsMap.isEmpty()) {
                xpath.setNamespaceContext(new NamespaceContext() {
                    @Override
                    public String getNamespaceURI(String prefix) {
                        return nsMap.getOrDefault(prefix, XMLConstants.NULL_NS_URI);
                    }
                    @Override
                    public String getPrefix(String namespaceURI) { return null; }
                    @Override
                    public Iterator getPrefixes(String namespaceURI) { return null; }
                });
            }
            NodeList nodes = (NodeList) xpath.evaluate(expression, doc, javax.xml.xpath.XPathConstants.NODESET);
            List<Node> result = new ArrayList<>();
            for (int i = 0; i < nodes.getLength(); i++) {
                result.add(nodes.item(i));
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException("XPath 查询失败: " + expression, e);
        }
    }

    /**
     * 清理空文本节点（去除换行和空格文本节点）
     *
     * <p>在构建或合并 XML 时，经常需要删除无用空节点</p>
     *
     * @param node 根节点
     */
    public static void removeEmptyTextNodes(Node node) {
        NodeList children = node.getChildNodes();
        for (int i = children.getLength() - 1; i >= 0; i--) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.TEXT_NODE) {
                if (child.getTextContent().trim().isEmpty()) {
                    node.removeChild(child);
                }
            } else {
                removeEmptyTextNodes(child);
            }
        }
    }

    /**
     * 深度复制 Document（生成新副本，避免修改原对象）
     *
     * @param doc 原 Document
     * @return 新 Document 副本
     */
    public static Document cloneDocument(Document doc) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document newDoc = builder.newDocument();
            Node imported = newDoc.importNode(doc.getDocumentElement(), true);
            newDoc.appendChild(imported);
            return newDoc;
        } catch (Exception e) {
            throw new RuntimeException("Document 克隆失败", e);
        }
    }

}

