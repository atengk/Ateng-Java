package local.ateng.java.customutils.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.springframework.util.ObjectUtils;

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
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    /**
     * XML 解析器
     */
    private static final XmlMapper XML_MAPPER = new XmlMapper();

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

}

