package io.github.atengk.utils.xml;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.w3c.dom.*;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 基于 Jackson XML 的通用 XML 静态工具类。
 *
 * @author Ateng
 * @since 2026-04-30
 */
public final class XmlUtil {

    /**
     * 默认字符集。
     */
    public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    /**
     * 默认 XML 版本。
     */
    public static final String DEFAULT_XML_VERSION = "1.0";

    /**
     * 默认缩进空格数。
     */
    public static final int DEFAULT_INDENT = 2;

    /**
     * 默认根节点名称。
     */
    public static final String DEFAULT_ROOT_NAME = "root";

    /**
     * 默认集合元素节点名称。
     */
    public static final String DEFAULT_ITEM_NAME = "item";

    /**
     * XML 声明前缀。
     */
    public static final String XML_DECLARATION_PREFIX = "<?xml";

    /**
     * CDATA 开始标记。
     */
    public static final String CDATA_PREFIX = "<![CDATA[";

    /**
     * CDATA 结束标记。
     */
    public static final String CDATA_SUFFIX = "]]>";

    /**
     * SOAP Envelope 节点名。
     */
    public static final String SOAP_ENVELOPE = "Envelope";

    /**
     * SOAP Header 节点名。
     */
    public static final String SOAP_HEADER = "Header";

    /**
     * SOAP Body 节点名。
     */
    public static final String SOAP_BODY = "Body";

    /**
     * SOAP Fault 节点名。
     */
    public static final String SOAP_FAULT = "Fault";

    private static final Object MAPPER_LOCK = new Object();
    private static final Pattern XML_DECLARATION_PATTERN = Pattern.compile("^\\s*<\\?xml\\s+([^?]+)\\?>", Pattern.CASE_INSENSITIVE);
    private static final Pattern XML_ENCODING_PATTERN = Pattern.compile("encoding\\s*=\\s*['\"]([^'\"]+)['\"]", Pattern.CASE_INSENSITIVE);
    private static final Pattern XML_VERSION_PATTERN = Pattern.compile("version\\s*=\\s*['\"]([^'\"]+)['\"]", Pattern.CASE_INSENSITIVE);
    private static final Pattern DANGEROUS_DECLARATION_PATTERN = Pattern.compile("(?is)<!DOCTYPE.*?>|<!ENTITY.*?>");

    private static volatile XmlMapper xmlMapper = createConfiguredMapper(true);
    private static final ObjectMapper JSON_MAPPER = createJsonMapper();
    private static final AtomicReference<DateTimeFormatter> DATE_TIME_FORMATTER = new AtomicReference<>(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

    private XmlUtil() {
        throw new UnsupportedOperationException("XmlUtil 不允许实例化");
    }

    /**
     * 将对象转换为 XML 字符串。
     *
     * @param data 待转换对象
     * @return XML 字符串
     */
    public static String toXml(Object data) {
        return toXml(data, false);
    }

    /**
     * 将对象转换为 XML 字符串。
     *
     * @param data   待转换对象
     * @param pretty 是否格式化输出
     * @return XML 字符串
     */
    public static String toXml(Object data, boolean pretty) {
        requireNonNull(data, "data");
        try {
            return pretty ? xmlMapper.writerWithDefaultPrettyPrinter().writeValueAsString(data) : xmlMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            throw new XmlMappingException("对象转换为 XML 失败", e);
        }
    }

    /**
     * 将对象转换为格式化 XML 字符串。
     *
     * @param data 待转换对象
     * @return 格式化 XML 字符串
     */
    public static String toPrettyXml(Object data) {
        return toXml(data, true);
    }

    /**
     * 将 XML 字符串转换为指定对象。
     *
     * @param xml   XML 字符串
     * @param clazz 目标类型
     * @param <T>   目标类型
     * @return 目标对象
     */
    public static <T> T fromXml(String xml, Class<T> clazz) {
        requireNonBlank(xml, "xml");
        requireNonNull(clazz, "clazz");
        try {
            return xmlMapper.readValue(xml, clazz);
        } catch (JsonProcessingException e) {
            throw new XmlParseException("XML 转换为对象失败", e);
        }
    }

    /**
     * 将 XML 字符串转换为泛型对象。
     *
     * @param xml           XML 字符串
     * @param typeReference 泛型类型引用
     * @param <T>           目标类型
     * @return 目标对象
     */
    public static <T> T fromXml(String xml, TypeReference<T> typeReference) {
        requireNonBlank(xml, "xml");
        requireNonNull(typeReference, "typeReference");
        try {
            return xmlMapper.readValue(xml, typeReference);
        } catch (JsonProcessingException e) {
            throw new XmlParseException("XML 转换为泛型对象失败", e);
        }
    }

    /**
     * 将 XML 字符串转换为 JavaType 对象。
     *
     * @param xml      XML 字符串
     * @param javaType Jackson JavaType
     * @param <T>      目标类型
     * @return 目标对象
     */
    public static <T> T fromXml(String xml, JavaType javaType) {
        requireNonBlank(xml, "xml");
        requireNonNull(javaType, "javaType");
        try {
            return xmlMapper.readValue(xml, javaType);
        } catch (JsonProcessingException e) {
            throw new XmlParseException("XML 转换为 JavaType 对象失败", e);
        }
    }

    /**
     * 使用 Jackson 将对象转换为另一种对象类型。
     *
     * @param source 源对象
     * @param clazz  目标类型
     * @param <T>    目标类型
     * @return 目标对象
     */
    public static <T> T convert(Object source, Class<T> clazz) {
        requireNonNull(source, "source");
        requireNonNull(clazz, "clazz");
        return xmlMapper.convertValue(source, clazz);
    }

    /**
     * 使用 Jackson 将对象转换为泛型对象。
     *
     * @param source        源对象
     * @param typeReference 泛型类型引用
     * @param <T>           目标类型
     * @return 目标对象
     */
    public static <T> T convert(Object source, TypeReference<T> typeReference) {
        requireNonNull(source, "source");
        requireNonNull(typeReference, "typeReference");
        return xmlMapper.convertValue(source, typeReference);
    }

    /**
     * 将对象转换为 UTF-8 XML 字节数组。
     *
     * @param data 待转换对象
     * @return XML 字节数组
     */
    public static byte[] toXmlBytes(Object data) {
        return toXmlBytes(data, DEFAULT_CHARSET);
    }

    /**
     * 将对象转换为指定字符集的 XML 字节数组。
     *
     * @param data    待转换对象
     * @param charset 字符集
     * @return XML 字节数组
     */
    public static byte[] toXmlBytes(Object data, Charset charset) {
        requireNonNull(charset, "charset");
        return toXml(data).getBytes(charset);
    }

    /**
     * 从 UTF-8 字节数组解析对象。
     *
     * @param bytes XML 字节数组
     * @param clazz 目标类型
     * @param <T>   目标类型
     * @return 目标对象
     */
    public static <T> T fromXmlBytes(byte[] bytes, Class<T> clazz) {
        return fromXmlBytes(bytes, DEFAULT_CHARSET, clazz);
    }

    /**
     * 从指定字符集字节数组解析对象。
     *
     * @param bytes   XML 字节数组
     * @param charset 字符集
     * @param clazz   目标类型
     * @param <T>     目标类型
     * @return 目标对象
     */
    public static <T> T fromXmlBytes(byte[] bytes, Charset charset, Class<T> clazz) {
        requireNonEmpty(bytes, "bytes");
        requireNonNull(charset, "charset");
        return fromXml(new String(bytes, charset), clazz);
    }

    /**
     * 从 UTF-8 字节数组解析泛型对象。
     *
     * @param bytes         XML 字节数组
     * @param typeReference 泛型类型引用
     * @param <T>           目标类型
     * @return 目标对象
     */
    public static <T> T fromXmlBytes(byte[] bytes, TypeReference<T> typeReference) {
        requireNonEmpty(bytes, "bytes");
        return fromXml(new String(bytes, DEFAULT_CHARSET), typeReference);
    }

    /**
     * 尝试从 XML 声明中识别字符集。
     *
     * @param bytes XML 字节数组
     * @return 字符集，无法识别时返回 UTF-8
     */
    public static Charset detectCharset(byte[] bytes) {
        requireNonEmpty(bytes, "bytes");
        String head = new String(bytes, 0, Math.min(bytes.length, 256), StandardCharsets.ISO_8859_1);
        Matcher matcher = XML_ENCODING_PATTERN.matcher(head);
        if (!matcher.find()) {
            return DEFAULT_CHARSET;
        }
        try {
            return Charset.forName(matcher.group(1));
        } catch (Exception e) {
            return DEFAULT_CHARSET;
        }
    }

    /**
     * 统一 XML 声明中的编码信息。
     *
     * @param xml     XML 字符串
     * @param charset 字符集
     * @return 处理后的 XML 字符串
     */
    public static String normalizeEncoding(String xml, Charset charset) {
        requireNonBlank(xml, "xml");
        requireNonNull(charset, "charset");
        return hasXmlDeclaration(xml) ? setXmlEncoding(xml, charset) : addXmlDeclaration(xml, charset);
    }

    /**
     * 从输入流解析对象。
     *
     * @param inputStream XML 输入流
     * @param clazz       目标类型
     * @param <T>         目标类型
     * @return 目标对象
     */
    public static <T> T fromXml(InputStream inputStream, Class<T> clazz) {
        requireNonNull(inputStream, "inputStream");
        requireNonNull(clazz, "clazz");
        try {
            return xmlMapper.readValue(inputStream, clazz);
        } catch (IOException e) {
            throw new XmlParseException("输入流转换为对象失败", e);
        }
    }

    /**
     * 从输入流解析泛型对象。
     *
     * @param inputStream   XML 输入流
     * @param typeReference 泛型类型引用
     * @param <T>           目标类型
     * @return 目标对象
     */
    public static <T> T fromXml(InputStream inputStream, TypeReference<T> typeReference) {
        requireNonNull(inputStream, "inputStream");
        requireNonNull(typeReference, "typeReference");
        try {
            return xmlMapper.readValue(inputStream, typeReference);
        } catch (IOException e) {
            throw new XmlParseException("输入流转换为泛型对象失败", e);
        }
    }

    /**
     * 从字符流解析对象。
     *
     * @param reader XML 字符流
     * @param clazz  目标类型
     * @param <T>    目标类型
     * @return 目标对象
     */
    public static <T> T fromXml(Reader reader, Class<T> clazz) {
        requireNonNull(reader, "reader");
        requireNonNull(clazz, "clazz");
        try {
            return xmlMapper.readValue(reader, clazz);
        } catch (IOException e) {
            throw new XmlParseException("字符流转换为对象失败", e);
        }
    }

    /**
     * 将对象写入输出流。
     *
     * @param outputStream 输出流
     * @param data         待写入对象
     */
    public static void writeXml(OutputStream outputStream, Object data) {
        writeXml(outputStream, data, false);
    }

    /**
     * 将对象写入输出流。
     *
     * @param outputStream 输出流
     * @param data         待写入对象
     * @param pretty       是否格式化输出
     */
    public static void writeXml(OutputStream outputStream, Object data, boolean pretty) {
        requireNonNull(outputStream, "outputStream");
        byte[] bytes = toXml(data, pretty).getBytes(DEFAULT_CHARSET);
        try {
            outputStream.write(bytes);
        } catch (IOException e) {
            throw new UncheckedIOException("写入 XML 输出流失败", e);
        }
    }

    /**
     * 将对象写入字符输出流。
     *
     * @param writer 字符输出流
     * @param data   待写入对象
     */
    public static void writeXml(Writer writer, Object data) {
        requireNonNull(writer, "writer");
        try {
            writer.write(toXml(data));
        } catch (IOException e) {
            throw new UncheckedIOException("写入 XML 字符流失败", e);
        }
    }

    /**
     * 读取输入流为 XML 字符串。
     *
     * @param inputStream XML 输入流
     * @return XML 字符串
     */
    public static String readXml(InputStream inputStream) {
        requireNonNull(inputStream, "inputStream");
        try {
            return new String(inputStream.readAllBytes(), DEFAULT_CHARSET);
        } catch (IOException e) {
            throw new UncheckedIOException("读取 XML 输入流失败", e);
        }
    }

    /**
     * 读取字符流为 XML 字符串。
     *
     * @param reader XML 字符流
     * @return XML 字符串
     */
    public static String readXml(Reader reader) {
        requireNonNull(reader, "reader");
        try {
            StringBuilder builder = new StringBuilder();
            char[] buffer = new char[4096];
            int len;
            while ((len = reader.read(buffer)) != -1) {
                builder.append(buffer, 0, len);
            }
            return builder.toString();
        } catch (IOException e) {
            throw new UncheckedIOException("读取 XML 字符流失败", e);
        }
    }

    /**
     * 从 XML 文件解析对象。
     *
     * @param file  XML 文件
     * @param clazz 目标类型
     * @param <T>   目标类型
     * @return 目标对象
     */
    public static <T> T fromXmlFile(File file, Class<T> clazz) {
        requireNonNull(file, "file");
        return fromXmlFile(file.toPath(), clazz);
    }

    /**
     * 从 XML 文件路径解析对象。
     *
     * @param path  XML 文件路径
     * @param clazz 目标类型
     * @param <T>   目标类型
     * @return 目标对象
     */
    public static <T> T fromXmlFile(Path path, Class<T> clazz) {
        validateReadable(path);
        try (InputStream inputStream = Files.newInputStream(path)) {
            return fromXml(inputStream, clazz);
        } catch (IOException e) {
            throw new UncheckedIOException("读取 XML 文件失败", e);
        }
    }

    /**
     * 从 XML 文件路径解析泛型对象。
     *
     * @param path          XML 文件路径
     * @param typeReference 泛型类型引用
     * @param <T>           目标类型
     * @return 目标对象
     */
    public static <T> T fromXmlFile(Path path, TypeReference<T> typeReference) {
        validateReadable(path);
        try (InputStream inputStream = Files.newInputStream(path)) {
            return fromXml(inputStream, typeReference);
        } catch (IOException e) {
            throw new UncheckedIOException("读取 XML 文件失败", e);
        }
    }

    /**
     * 将对象写入 XML 文件。
     *
     * @param path XML 文件路径
     * @param data 待写入对象
     */
    public static void writeXmlFile(Path path, Object data) {
        writeXmlFile(path, data, false);
    }

    /**
     * 将对象写入 XML 文件。
     *
     * @param path   XML 文件路径
     * @param data   待写入对象
     * @param pretty 是否格式化输出
     */
    public static void writeXmlFile(Path path, Object data, boolean pretty) {
        requireNonNull(path, "path");
        try {
            Path parent = path.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(path, toXml(data, pretty), DEFAULT_CHARSET);
        } catch (IOException e) {
            throw new UncheckedIOException("写入 XML 文件失败", e);
        }
    }

    /**
     * 读取 XML 文件内容。
     *
     * @param path XML 文件路径
     * @return XML 字符串
     */
    public static String readXmlFile(Path path) {
        validateReadable(path);
        try {
            return Files.readString(path, DEFAULT_CHARSET);
        } catch (IOException e) {
            throw new UncheckedIOException("读取 XML 文件内容失败", e);
        }
    }

    /**
     * 判断 XML 文件是否存在且可读。
     *
     * @param path XML 文件路径
     * @return 是否存在且可读
     */
    public static boolean existsXmlFile(Path path) {
        return path != null && Files.isRegularFile(path) && Files.isReadable(path);
    }

    /**
     * 生成 XML 文件备份。
     *
     * @param path XML 文件路径
     * @return 备份文件路径
     */
    public static Path backupXmlFile(Path path) {
        validateReadable(path);
        Path backup = path.resolveSibling(path.getFileName() + ".bak");
        try {
            return Files.copy(path, backup, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("备份 XML 文件失败", e);
        }
    }

    /**
     * 指定根节点名称序列化对象。
     *
     * @param data     待转换对象
     * @param rootName 根节点名称
     * @return XML 字符串
     */
    public static String toXmlWithRoot(Object data, String rootName) {
        return toXmlWithRoot(data, rootName, false);
    }

    /**
     * 指定根节点名称序列化对象。
     *
     * @param data     待转换对象
     * @param rootName 根节点名称
     * @param pretty   是否格式化输出
     * @return XML 字符串
     */
    public static String toXmlWithRoot(Object data, String rootName, boolean pretty) {
        requireNonNull(data, "data");
        requireNonBlank(rootName, "rootName");
        try {
            return pretty
                    ? xmlMapper.writerWithDefaultPrettyPrinter().withRootName(rootName).writeValueAsString(data)
                    : xmlMapper.writer().withRootName(rootName).writeValueAsString(data);
        } catch (JsonProcessingException e) {
            throw new XmlMappingException("指定根节点转换 XML 失败", e);
        }
    }

    /**
     * 校验根节点名称后反序列化 XML。
     *
     * @param xml      XML 字符串
     * @param rootName 根节点名称
     * @param clazz    目标类型
     * @param <T>      目标类型
     * @return 目标对象
     */
    public static <T> T fromXmlWithRoot(String xml, String rootName, Class<T> clazz) {
        validateRootName(xml, rootName);
        return fromXml(xml, clazz);
    }

    /**
     * 获取 XML 根节点名称。
     *
     * @param xml XML 字符串
     * @return 根节点名称
     */
    public static String getRootName(String xml) {
        Document document = parseDocument(xml, false);
        return document.getDocumentElement().getNodeName();
    }

    /**
     * 判断 XML 根节点名称是否匹配。
     *
     * @param xml      XML 字符串
     * @param rootName 根节点名称
     * @return 是否匹配
     */
    public static boolean hasRootName(String xml, String rootName) {
        requireNonBlank(rootName, "rootName");
        return rootName.equals(getRootName(xml));
    }

    /**
     * 替换 XML 根节点名称。
     *
     * @param xml      XML 字符串
     * @param rootName 新根节点名称
     * @return 处理后的 XML 字符串
     */
    public static String renameRoot(String xml, String rootName) {
        requireNonBlank(rootName, "rootName");
        Document document = parseDocument(xml, true);
        document.renameNode(document.getDocumentElement(), document.getDocumentElement().getNamespaceURI(), rootName);
        return documentToString(document, false, DEFAULT_INDENT, true);
    }

    /**
     * 为 XML 包装根节点。
     *
     * @param xml      XML 字符串
     * @param rootName 根节点名称
     * @return 处理后的 XML 字符串
     */
    public static String wrapRoot(String xml, String rootName) {
        requireNonBlank(xml, "xml");
        requireNonBlank(rootName, "rootName");
        return '<' + rootName + '>' + removeXmlDeclaration(xml).trim() + "</" + rootName + '>';
    }

    /**
     * 移除最外层根节点并返回内部 XML。
     *
     * @param xml XML 字符串
     * @return 内部 XML 字符串
     */
    public static String unwrapRoot(String xml) {
        Document document = parseDocument(xml, true);
        StringBuilder builder = new StringBuilder();
        NodeList children = document.getDocumentElement().getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                builder.append(nodeToString(child, false, DEFAULT_INDENT, true));
            } else if (child.getNodeType() == Node.TEXT_NODE && !child.getTextContent().isBlank()) {
                builder.append(escapeText(child.getTextContent()));
            }
        }
        return builder.toString();
    }

    /**
     * 将集合转换为指定根节点和元素节点的 XML。
     *
     * @param list     集合数据
     * @param rootName 根节点名称
     * @param itemName 元素节点名称
     * @param <T>      元素类型
     * @return XML 字符串
     */
    public static <T> String toXmlList(Collection<T> list, String rootName, String itemName) {
        requireNonNull(list, "list");
        requireNonBlank(rootName, "rootName");
        requireNonBlank(itemName, "itemName");
        StringBuilder builder = new StringBuilder();
        builder.append('<').append(rootName).append('>');
        for (T item : list) {
            builder.append(renameRoot(toXml(item), itemName));
        }
        builder.append("</").append(rootName).append('>');
        return builder.toString();
    }

    /**
     * 将 XML 转换为对象列表。
     *
     * @param xml       XML 字符串
     * @param itemClass 元素类型
     * @param <T>       元素类型
     * @return 对象列表
     */
    public static <T> List<T> fromXmlList(String xml, Class<T> itemClass) {
        return fromXmlList(xml, null, itemClass);
    }

    /**
     * 根据元素节点名称将 XML 转换为对象列表。
     *
     * @param xml       XML 字符串
     * @param itemName  元素节点名称，为空时读取直接子元素
     * @param itemClass 元素类型
     * @param <T>       元素类型
     * @return 对象列表
     */
    public static <T> List<T> fromXmlList(String xml, String itemName, Class<T> itemClass) {
        requireNonBlank(xml, "xml");
        requireNonNull(itemClass, "itemClass");
        Document document = parseDocument(xml, true);
        List<T> result = new ArrayList<>();
        NodeList nodes = document.getDocumentElement().getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE && (itemName == null || itemName.isBlank() || itemName.equals(node.getNodeName()))) {
                result.add(fromXml(nodeToString(node, false, DEFAULT_INDENT, true), itemClass));
            }
        }
        return result;
    }

    /**
     * 将 XML 转换为数组。
     *
     * @param xml        XML 字符串
     * @param arrayClass 数组类型
     * @param <T>        数组类型
     * @return 数组对象
     */
    public static <T> T fromXmlArray(String xml, Class<T> arrayClass) {
        requireNonBlank(xml, "xml");
        requireNonNull(arrayClass, "arrayClass");
        return fromXml(xml, arrayClass);
    }

    /**
     * 将数组转换为指定根节点和元素节点的 XML。
     *
     * @param array    数组数据
     * @param rootName 根节点名称
     * @param itemName 元素节点名称
     * @param <T>      元素类型
     * @return XML 字符串
     */
    public static <T> String toXmlArray(T[] array, String rootName, String itemName) {
        requireNonNull(array, "array");
        return toXmlList(Arrays.asList(array), rootName, itemName);
    }

    /**
     * 构造 Jackson 集合类型。
     *
     * @param collectionClass 集合类型
     * @param itemClass       元素类型
     * @return Jackson JavaType
     */
    public static JavaType collectionType(Class<? extends Collection> collectionClass, Class<?> itemClass) {
        requireNonNull(collectionClass, "collectionClass");
        requireNonNull(itemClass, "itemClass");
        return xmlMapper.getTypeFactory().constructCollectionType(collectionClass, itemClass);
    }

    /**
     * 构造 Jackson Map 类型。
     *
     * @param keyClass   键类型
     * @param valueClass 值类型
     * @return Jackson JavaType
     */
    public static JavaType mapType(Class<?> keyClass, Class<?> valueClass) {
        requireNonNull(keyClass, "keyClass");
        requireNonNull(valueClass, "valueClass");
        return xmlMapper.getTypeFactory().constructMapType(LinkedHashMap.class, keyClass, valueClass);
    }

    /**
     * 将 Map 转换为指定根节点 XML。
     *
     * @param map      Map 数据
     * @param rootName 根节点名称
     * @return XML 字符串
     */
    public static String toXml(Map<String, Object> map, String rootName) {
        return toXmlWithRoot(map, rootName);
    }

    /**
     * 将 XML 转换为 Map。
     *
     * @param xml XML 字符串
     * @return Map 数据
     */
    public static Map<String, Object> fromXmlToMap(String xml) {
        return fromXmlToMap(xml, true);
    }

    /**
     * 将 XML 转换为 Map。
     *
     * @param xml           XML 字符串
     * @param preserveOrder 是否保留字段顺序
     * @return Map 数据
     */
    public static Map<String, Object> fromXmlToMap(String xml, boolean preserveOrder) {
        requireNonBlank(xml, "xml");
        try {
            TypeReference<Map<String, Object>> type = new TypeReference<>() {
            };
            Map<String, Object> map = xmlMapper.readValue(xml, type);
            return preserveOrder ? new LinkedHashMap<>(map) : map;
        } catch (JsonProcessingException e) {
            throw new XmlParseException("XML 转换为 Map 失败", e);
        }
    }

    /**
     * 将 XML 转换为扁平 Map。
     *
     * @param xml XML 字符串
     * @return 扁平 Map
     */
    public static Map<String, Object> fromXmlToFlatMap(String xml) {
        return flattenMap(fromXmlToMap(xml), "");
    }

    /**
     * 将对象转换为扁平 Map。
     *
     * @param data 对象数据
     * @return 扁平 Map
     */
    public static Map<String, Object> toFlatMap(Object data) {
        requireNonNull(data, "data");
        Map<String, Object> map = xmlMapper.convertValue(data, new TypeReference<>() {
        });
        return flattenMap(map, "");
    }

    /**
     * 将扁平 Map 构造为 XML。
     *
     * @param map      扁平 Map
     * @param rootName 根节点名称
     * @return XML 字符串
     */
    public static String fromFlatMap(Map<String, Object> map, String rootName) {
        requireNonNull(map, "map");
        requireNonBlank(rootName, "rootName");
        Map<String, Object> nested = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            putNested(nested, entry.getKey(), entry.getValue());
        }
        return toXml(nested, rootName);
    }

    /**
     * 将 XML 读取为 Jackson JsonNode。
     *
     * @param xml XML 字符串
     * @return JsonNode 节点
     */
    public static JsonNode readTree(String xml) {
        requireNonBlank(xml, "xml");
        try {
            return xmlMapper.readTree(xml);
        } catch (JsonProcessingException e) {
            throw new XmlParseException("XML 读取为 JsonNode 失败", e);
        }
    }

    /**
     * 从输入流读取为 Jackson JsonNode。
     *
     * @param inputStream XML 输入流
     * @return JsonNode 节点
     */
    public static JsonNode readTree(InputStream inputStream) {
        requireNonNull(inputStream, "inputStream");
        try {
            return xmlMapper.readTree(inputStream);
        } catch (IOException e) {
            throw new XmlParseException("输入流读取为 JsonNode 失败", e);
        }
    }

    /**
     * 将 JsonNode 写为 XML。
     *
     * @param node JsonNode 节点
     * @return XML 字符串
     */
    public static String writeTree(JsonNode node) {
        return writeTree(node, false);
    }

    /**
     * 将 JsonNode 写为 XML。
     *
     * @param node   JsonNode 节点
     * @param pretty 是否格式化输出
     * @return XML 字符串
     */
    public static String writeTree(JsonNode node, boolean pretty) {
        requireNonNull(node, "node");
        return toXmlWithRoot(node, DEFAULT_ROOT_NAME, pretty);
    }

    /**
     * 将对象转换为 JsonNode。
     *
     * @param data 对象数据
     * @return JsonNode 节点
     */
    public static JsonNode toTree(Object data) {
        requireNonNull(data, "data");
        return xmlMapper.valueToTree(data);
    }

    /**
     * 将 JsonNode 转换为对象。
     *
     * @param node  JsonNode 节点
     * @param clazz 目标类型
     * @param <T>   目标类型
     * @return 目标对象
     */
    public static <T> T treeToValue(JsonNode node, Class<T> clazz) {
        requireNonNull(node, "node");
        requireNonNull(clazz, "clazz");
        try {
            return xmlMapper.treeToValue(node, clazz);
        } catch (JsonProcessingException e) {
            throw new XmlMappingException("JsonNode 转换为对象失败", e);
        }
    }

    /**
     * 将 JsonNode 转换为泛型对象。
     *
     * @param node          JsonNode 节点
     * @param typeReference 泛型类型引用
     * @param <T>           目标类型
     * @return 目标对象
     */
    public static <T> T treeToValue(JsonNode node, TypeReference<T> typeReference) {
        requireNonNull(node, "node");
        requireNonNull(typeReference, "typeReference");
        return xmlMapper.convertValue(node, typeReference);
    }

    /**
     * 根据路径获取文本值。
     *
     * @param node JsonNode 节点
     * @param path 节点路径
     * @return 文本值，不存在时返回 null
     */
    public static String getText(JsonNode node, String path) {
        JsonNode target = findNode(node, path);
        return target == null || target.isMissingNode() || target.isNull() ? null : target.asText();
    }

    /**
     * 根据路径获取整数值。
     *
     * @param node JsonNode 节点
     * @param path 节点路径
     * @return 整数值，不存在或无法转换时返回 null
     */
    public static Integer getInt(JsonNode node, String path) {
        JsonNode target = findNode(node, path);
        return target == null || target.isMissingNode() || !target.canConvertToInt() ? null : target.asInt();
    }

    /**
     * 根据路径获取布尔值。
     *
     * @param node JsonNode 节点
     * @param path 节点路径
     * @return 布尔值，不存在时返回 null
     */
    public static Boolean getBoolean(JsonNode node, String path) {
        JsonNode target = findNode(node, path);
        return target == null || target.isMissingNode() || target.isNull() ? null : target.asBoolean();
    }

    /**
     * 根据路径获取字符串值。
     *
     * @param xml  XML 字符串
     * @param path 节点路径
     * @return 字符串值，不存在时返回 null
     */
    public static String getValue(String xml, String path) {
        Element element = findElementByPath(parseDocument(xml, true), path);
        return element == null ? null : element.getTextContent();
    }

    /**
     * 根据路径获取多个字符串值。
     *
     * @param xml  XML 字符串
     * @param path 节点路径
     * @return 字符串值列表
     */
    public static List<String> getValues(String xml, String path) {
        List<Element> elements = findElementsByPath(parseDocument(xml, true), path);
        List<String> result = new ArrayList<>();
        for (Element element : elements) {
            result.add(element.getTextContent());
        }
        return result;
    }

    /**
     * 根据路径获取必填字符串值。
     *
     * @param xml  XML 字符串
     * @param path 节点路径
     * @return 字符串值
     */
    public static String getRequiredValue(String xml, String path) {
        String value = getValue(xml, path);
        if (value == null || value.isBlank()) {
            throw new XmlValidationException("XML 必填节点不存在或为空: " + path);
        }
        return value;
    }

    /**
     * 判断路径是否存在。
     *
     * @param xml  XML 字符串
     * @param path 节点路径
     * @return 是否存在
     */
    public static boolean containsPath(String xml, String path) {
        return findElementByPath(parseDocument(xml, true), path) != null;
    }

    /**
     * 判断指定路径值是否匹配。
     *
     * @param xml   XML 字符串
     * @param path  节点路径
     * @param value 期望值
     * @return 是否匹配
     */
    public static boolean containsValue(String xml, String path, String value) {
        return Objects.equals(getValue(xml, path), value);
    }

    /**
     * 批量提取指定路径的字段值。
     *
     * @param xml   XML 字符串
     * @param paths 节点路径集合
     * @return 字段值列表
     */
    public static List<String> extract(String xml, Collection<String> paths) {
        requireNonNull(paths, "paths");
        List<String> result = new ArrayList<>();
        for (String path : paths) {
            result.add(getValue(xml, path));
        }
        return result;
    }

    /**
     * 批量提取指定路径的字段值为 Map。
     *
     * @param xml   XML 字符串
     * @param paths 节点路径集合
     * @return 字段值 Map
     */
    public static Map<String, String> extractToMap(String xml, Collection<String> paths) {
        requireNonNull(paths, "paths");
        Map<String, String> result = new LinkedHashMap<>();
        for (String path : paths) {
            result.put(path, getValue(xml, path));
        }
        return result;
    }

    /**
     * 格式化 XML 字符串。
     *
     * @param xml XML 字符串
     * @return 格式化 XML 字符串
     */
    public static String format(String xml) {
        return format(xml, DEFAULT_INDENT);
    }

    /**
     * 按指定缩进格式化 XML 字符串。
     *
     * @param xml    XML 字符串
     * @param indent 缩进空格数
     * @return 格式化 XML 字符串
     */
    public static String format(String xml, int indent) {
        if (indent < 0) {
            throw new IllegalArgumentException("indent 不能小于 0");
        }
        Document document = parseDocument(xml, true);
        return documentToString(document, true, indent, true);
    }

    /**
     * 压缩 XML 字符串，移除标签之间的多余空白。
     *
     * @param xml XML 字符串
     * @return 压缩后的 XML 字符串
     */
    public static String compact(String xml) {
        requireNonBlank(xml, "xml");
        validateWellFormed(xml);
        return xml.trim().replaceAll(">\\s+<", "><");
    }

    /**
     * 去除 XML 前后空白。
     *
     * @param xml XML 字符串
     * @return 处理后的 XML 字符串
     */
    public static String trim(String xml) {
        return xml == null ? null : xml.trim();
    }

    /**
     * 移除 XML 声明。
     *
     * @param xml XML 字符串
     * @return 处理后的 XML 字符串
     */
    public static String removeXmlDeclaration(String xml) {
        requireNonBlank(xml, "xml");
        return XML_DECLARATION_PATTERN.matcher(xml).replaceFirst("").trim();
    }

    /**
     * 添加默认 XML 声明。
     *
     * @param xml XML 字符串
     * @return 处理后的 XML 字符串
     */
    public static String addXmlDeclaration(String xml) {
        return addXmlDeclaration(xml, DEFAULT_CHARSET);
    }

    /**
     * 添加指定编码的 XML 声明。
     *
     * @param xml     XML 字符串
     * @param charset 字符集
     * @return 处理后的 XML 字符串
     */
    public static String addXmlDeclaration(String xml, Charset charset) {
        requireNonBlank(xml, "xml");
        requireNonNull(charset, "charset");
        String body = removeXmlDeclaration(xml);
        return buildXmlDeclaration(DEFAULT_XML_VERSION, charset) + body;
    }

    /**
     * 标准化 XML 字符串。
     *
     * @param xml XML 字符串
     * @return 标准化后的 XML 字符串
     */
    public static String normalize(String xml) {
        return compact(removeXmlDeclaration(xml));
    }

    /**
     * 转义 XML 文本。
     *
     * @param text 原始文本
     * @return 转义后的文本
     */
    public static String escapeText(String text) {
        if (text == null) {
            return null;
        }
        StringBuilder builder = new StringBuilder(text.length());
        for (char c : text.toCharArray()) {
            switch (c) {
                case '&' -> builder.append("&amp;");
                case '<' -> builder.append("&lt;");
                case '>' -> builder.append("&gt;");
                case '\"' -> builder.append("&quot;");
                case '\'' -> builder.append("&apos;");
                default -> builder.append(c);
            }
        }
        return builder.toString();
    }

    /**
     * 反转义 XML 文本。
     *
     * @param text 转义文本
     * @return 原始文本
     */
    public static String unescapeText(String text) {
        if (text == null) {
            return null;
        }
        return text.replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&apos;", "'")
                .replace("&amp;", "&");
    }

    /**
     * 判断是否包含 XML 声明。
     *
     * @param xml XML 字符串
     * @return 是否包含 XML 声明
     */
    public static boolean hasXmlDeclaration(String xml) {
        return xml != null && XML_DECLARATION_PATTERN.matcher(xml).find();
    }

    /**
     * 获取 XML 版本。
     *
     * @param xml XML 字符串
     * @return XML 版本，不存在时返回 null
     */
    public static String getXmlVersion(String xml) {
        return findDeclarationValue(xml, XML_VERSION_PATTERN);
    }

    /**
     * 获取 XML 声明中的编码。
     *
     * @param xml XML 字符串
     * @return 编码名称，不存在时返回 null
     */
    public static String getXmlEncoding(String xml) {
        return findDeclarationValue(xml, XML_ENCODING_PATTERN);
    }

    /**
     * 修改 XML 声明编码。
     *
     * @param xml     XML 字符串
     * @param charset 字符集
     * @return 处理后的 XML 字符串
     */
    public static String setXmlEncoding(String xml, Charset charset) {
        requireNonBlank(xml, "xml");
        requireNonNull(charset, "charset");
        String declaration = buildXmlDeclaration(getXmlVersion(xml) == null ? DEFAULT_XML_VERSION : getXmlVersion(xml), charset);
        return declaration + removeXmlDeclaration(xml);
    }

    /**
     * 修改 XML 版本。
     *
     * @param xml     XML 字符串
     * @param version XML 版本
     * @return 处理后的 XML 字符串
     */
    public static String setXmlVersion(String xml, String version) {
        requireNonBlank(xml, "xml");
        requireNonBlank(version, "version");
        Charset charset = getXmlEncoding(xml) == null ? DEFAULT_CHARSET : Charset.forName(getXmlEncoding(xml));
        return buildXmlDeclaration(version, charset) + removeXmlDeclaration(xml);
    }

    /**
     * 构建默认 XML 声明。
     *
     * @return XML 声明
     */
    public static String buildXmlDeclaration() {
        return buildXmlDeclaration(DEFAULT_XML_VERSION, DEFAULT_CHARSET);
    }

    /**
     * 构建指定版本和编码的 XML 声明。
     *
     * @param version XML 版本
     * @param charset 字符集
     * @return XML 声明
     */
    public static String buildXmlDeclaration(String version, Charset charset) {
        requireNonBlank(version, "version");
        requireNonNull(charset, "charset");
        return "<?xml version=\"" + version + "\" encoding=\"" + charset.name() + "\"?>";
    }

    /**
     * 判断字符串是否为格式良好的 XML。
     *
     * @param text 待判断文本
     * @return 是否为 XML
     */
    public static boolean isXml(String text) {
        return isWellFormed(text);
    }

    /**
     * 判断 XML 是否格式良好。
     *
     * @param xml XML 字符串
     * @return 是否格式良好
     */
    public static boolean isWellFormed(String xml) {
        if (xml == null || xml.isBlank()) {
            return false;
        }
        try {
            parseDocument(xml, true);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    /**
     * 判断 XML 是否为空或无有效内容。
     *
     * @param xml XML 字符串
     * @return 是否为空 XML
     */
    public static boolean isBlankXml(String xml) {
        return xml == null || xml.isBlank() || removeXmlDeclaration(xml).isBlank();
    }

    /**
     * 校验 XML 格式良好。
     *
     * @param xml XML 字符串
     */
    public static void validateWellFormed(String xml) {
        parseDocument(xml, true);
    }

    /**
     * 校验根节点名称。
     *
     * @param xml      XML 字符串
     * @param rootName 根节点名称
     */
    public static void validateRootName(String xml, String rootName) {
        requireNonBlank(rootName, "rootName");
        String actual = getRootName(xml);
        if (!rootName.equals(actual)) {
            throw new XmlValidationException("XML 根节点不匹配，期望: " + rootName + "，实际: " + actual);
        }
    }

    /**
     * 校验 XML 内容非空。
     *
     * @param xml XML 字符串
     */
    public static void validateNotBlank(String xml) {
        requireNonBlank(xml, "xml");
    }

    /**
     * 校验 XML 文件可读。
     *
     * @param path XML 文件路径
     */
    public static void validateReadable(Path path) {
        requireNonNull(path, "path");
        if (!Files.isRegularFile(path) || !Files.isReadable(path)) {
            throw new XmlValidationException("XML 文件不存在或不可读: " + path);
        }
    }

    /**
     * 校验 XML 文件可写。
     *
     * @param path XML 文件路径
     */
    public static void validateWritable(Path path) {
        requireNonNull(path, "path");
        Path parent = path.toAbsolutePath().getParent();
        if (parent != null && (!Files.exists(parent) || !Files.isWritable(parent))) {
            throw new XmlValidationException("XML 文件父目录不可写: " + parent);
        }
        if (Files.exists(path) && !Files.isWritable(path)) {
            throw new XmlValidationException("XML 文件不可写: " + path);
        }
    }

    /**
     * 使用 XSD 字符串校验 XML。
     *
     * @param xml XML 字符串
     * @param xsd XSD 字符串
     */
    public static void validateByXsd(String xml, String xsd) {
        requireNonBlank(xml, "xml");
        requireNonBlank(xsd, "xsd");
        validateByXsd(new StreamSource(new StringReader(xml)), new StreamSource(new StringReader(xsd)));
    }

    /**
     * 使用 XSD 文件校验 XML。
     *
     * @param xml     XML 字符串
     * @param xsdPath XSD 文件路径
     */
    public static void validateByXsd(String xml, Path xsdPath) {
        requireNonBlank(xml, "xml");
        validateReadable(xsdPath);
        try (InputStream xsdInput = Files.newInputStream(xsdPath)) {
            validateByXsd(new StreamSource(new StringReader(xml)), new StreamSource(xsdInput));
        } catch (IOException e) {
            throw new UncheckedIOException("读取 XSD 文件失败", e);
        }
    }

    /**
     * 使用输入流校验 XML。
     *
     * @param xmlStream XML 输入流
     * @param xsdStream XSD 输入流
     */
    public static void validateByXsd(InputStream xmlStream, InputStream xsdStream) {
        requireNonNull(xmlStream, "xmlStream");
        requireNonNull(xsdStream, "xsdStream");
        validateByXsd(new StreamSource(xmlStream), new StreamSource(xsdStream));
    }

    /**
     * 使用 XSD 字符串校验 XML 并返回布尔结果。
     *
     * @param xml XML 字符串
     * @param xsd XSD 字符串
     * @return 是否校验通过
     */
    public static boolean isValidByXsd(String xml, String xsd) {
        try {
            validateByXsd(xml, xsd);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    /**
     * 获取 XSD 校验错误列表。
     *
     * @param xml XML 字符串
     * @param xsd XSD 字符串
     * @return 校验错误列表
     */
    public static List<String> getXsdValidationErrors(String xml, String xsd) {
        requireNonBlank(xml, "xml");
        requireNonBlank(xsd, "xsd");
        List<String> errors = new ArrayList<>();
        try {
            SchemaFactory schemaFactory = secureSchemaFactory();
            Schema schema = schemaFactory.newSchema(new StreamSource(new StringReader(xsd)));
            Validator validator = schema.newValidator();
            validator.setErrorHandler(new ErrorHandler() {
                @Override
                public void warning(SAXParseException exception) {
                    errors.add(exception.getMessage());
                }

                @Override
                public void error(SAXParseException exception) {
                    errors.add(exception.getMessage());
                }

                @Override
                public void fatalError(SAXParseException exception) {
                    errors.add(exception.getMessage());
                }
            });
            validator.validate(new StreamSource(new StringReader(xml)));
        } catch (Exception e) {
            errors.add(e.getMessage());
        }
        return errors;
    }

    /**
     * 判断 XML 是否包含命名空间。
     *
     * @param xml XML 字符串
     * @return 是否包含命名空间
     */
    public static boolean hasNamespace(String xml) {
        return !getNamespaces(xml).isEmpty();
    }

    /**
     * 获取 XML 命名空间映射。
     *
     * @param xml XML 字符串
     * @return 前缀与命名空间 URI 的映射
     */
    public static Map<String, String> getNamespaces(String xml) {
        Document document = parseDocument(xml, true);
        Map<String, String> result = new LinkedHashMap<>();
        collectNamespaces(document.getDocumentElement(), result);
        return result;
    }

    /**
     * 移除 XML 命名空间。
     *
     * @param xml XML 字符串
     * @return 处理后的 XML 字符串
     */
    public static String removeNamespaces(String xml) {
        Document source = parseDocument(xml, true);
        Document target = newDocument();
        Node copied = copyWithoutNamespace(source.getDocumentElement(), target);
        target.appendChild(copied);
        return documentToString(target, false, DEFAULT_INDENT, true);
    }

    /**
     * 替换命名空间 URI。
     *
     * @param xml    XML 字符串
     * @param oldUri 原命名空间 URI
     * @param newUri 新命名空间 URI
     * @return 处理后的 XML 字符串
     */
    public static String renameNamespace(String xml, String oldUri, String newUri) {
        requireNonBlank(xml, "xml");
        requireNonBlank(oldUri, "oldUri");
        requireNonBlank(newUri, "newUri");
        return xml.replace(oldUri, newUri);
    }

    /**
     * 根据前缀获取命名空间 URI。
     *
     * @param xml    XML 字符串
     * @param prefix 命名空间前缀，默认命名空间使用空字符串
     * @return 命名空间 URI，不存在时返回 null
     */
    public static String getNamespaceUri(String xml, String prefix) {
        return getNamespaces(xml).get(prefix == null ? "" : prefix);
    }

    /**
     * 根据 URI 获取命名空间前缀。
     *
     * @param xml XML 字符串
     * @param uri 命名空间 URI
     * @return 命名空间前缀，不存在时返回 null
     */
    public static String getNamespacePrefix(String xml, String uri) {
        requireNonBlank(uri, "uri");
        return getNamespaces(xml).entrySet().stream()
                .filter(entry -> uri.equals(entry.getValue()))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    /**
     * 指定命名空间 URI 序列化对象。
     *
     * @param data         待转换对象
     * @param namespaceUri 命名空间 URI
     * @return XML 字符串
     */
    public static String toXmlWithNamespace(Object data, String namespaceUri) {
        requireNonNull(data, "data");
        requireNonBlank(namespaceUri, "namespaceUri");
        String xml = toXml(data);
        Document document = parseDocument(xml, true);
        document.getDocumentElement().setAttribute("xmlns", namespaceUri);
        return documentToString(document, false, DEFAULT_INDENT, true);
    }

    /**
     * 将文本包装为 CDATA。
     *
     * @param text 原始文本
     * @return CDATA 文本
     */
    public static String wrapCdata(String text) {
        return CDATA_PREFIX + escapeCdataEnd(text == null ? "" : text) + CDATA_SUFFIX;
    }

    /**
     * 去除 CDATA 包装。
     *
     * @param text CDATA 文本
     * @return 原始文本
     */
    public static String unwrapCdata(String text) {
        if (!isCdata(text)) {
            return text;
        }
        return text.substring(CDATA_PREFIX.length(), text.length() - CDATA_SUFFIX.length());
    }

    /**
     * 判断文本是否为 CDATA。
     *
     * @param text 文本
     * @return 是否为 CDATA
     */
    public static boolean isCdata(String text) {
        return text != null && text.startsWith(CDATA_PREFIX) && text.endsWith(CDATA_SUFFIX);
    }

    /**
     * 判断 XML 是否包含 CDATA。
     *
     * @param xml XML 字符串
     * @return 是否包含 CDATA
     */
    public static boolean containsCdata(String xml) {
        return xml != null && xml.contains(CDATA_PREFIX) && xml.contains(CDATA_SUFFIX);
    }

    /**
     * 替换指定路径的 CDATA 内容。
     *
     * @param xml   XML 字符串
     * @param path  节点路径
     * @param value 新文本
     * @return 处理后的 XML 字符串
     */
    public static String replaceCdata(String xml, String path, String value) {
        Document document = parseDocument(xml, true);
        Element element = findElementByPath(document, path);
        if (element == null) {
            throw new XmlValidationException("XML 节点不存在: " + path);
        }
        while (element.hasChildNodes()) {
            element.removeChild(element.getFirstChild());
        }
        CDATASection section = document.createCDATASection(escapeCdataEnd(value == null ? "" : value));
        element.appendChild(section);
        return documentToString(document, false, DEFAULT_INDENT, true);
    }

    /**
     * 转义文本中的 CDATA 结束标记。
     *
     * @param text 原始文本
     * @return 处理后的文本
     */
    public static String escapeCdataEnd(String text) {
        return text == null ? null : text.replace(CDATA_SUFFIX, "]]]]><![CDATA[>");
    }

    /**
     * 将 XML 转换为 JSON 字符串。
     *
     * @param xml XML 字符串
     * @return JSON 字符串
     */
    public static String xmlToJson(String xml) {
        return xmlToJson(xml, false);
    }

    /**
     * 将 XML 转换为 JSON 字符串。
     *
     * @param xml    XML 字符串
     * @param pretty 是否格式化输出
     * @return JSON 字符串
     */
    public static String xmlToJson(String xml, boolean pretty) {
        JsonNode node = xmlToJsonNode(xml);
        try {
            return pretty ? JSON_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(node) : JSON_MAPPER.writeValueAsString(node);
        } catch (JsonProcessingException e) {
            throw new XmlMappingException("XML 转 JSON 失败", e);
        }
    }

    /**
     * 将 JSON 字符串转换为 XML。
     *
     * @param json JSON 字符串
     * @return XML 字符串
     */
    public static String jsonToXml(String json) {
        return jsonToXml(json, DEFAULT_ROOT_NAME);
    }

    /**
     * 将 JSON 字符串转换为指定根节点 XML。
     *
     * @param json     JSON 字符串
     * @param rootName 根节点名称
     * @return XML 字符串
     */
    public static String jsonToXml(String json, String rootName) {
        requireNonBlank(json, "json");
        requireNonBlank(rootName, "rootName");
        try {
            JsonNode node = JSON_MAPPER.readTree(json);
            return toXmlWithRoot(node, rootName);
        } catch (JsonProcessingException e) {
            throw new XmlParseException("JSON 转 XML 失败", e);
        }
    }

    /**
     * 将 XML 转换为 JsonNode。
     *
     * @param xml XML 字符串
     * @return JsonNode 节点
     */
    public static JsonNode xmlToJsonNode(String xml) {
        return readTree(xml);
    }

    /**
     * 将 JsonNode 转换为 XML。
     *
     * @param node JsonNode 节点
     * @return XML 字符串
     */
    public static String jsonNodeToXml(JsonNode node) {
        return writeTree(node);
    }

    /**
     * 将 XML 转换为 Map。
     *
     * @param xml XML 字符串
     * @return Map 数据
     */
    public static Map<String, Object> xmlToMap(String xml) {
        return fromXmlToMap(xml);
    }

    /**
     * 将 Map 转换为 XML。
     *
     * @param map      Map 数据
     * @param rootName 根节点名称
     * @return XML 字符串
     */
    public static String mapToXml(Map<String, Object> map, String rootName) {
        return toXml(map, rootName);
    }

    /**
     * 禁用外部实体解析并重置为安全 XmlMapper。
     */
    public static void disableExternalEntity() {
        setMapper(newSafeMapper());
    }

    /**
     * 禁用 DTD 并重置为安全 XmlMapper。
     */
    public static void disableDtd() {
        setMapper(newSafeMapper());
    }

    /**
     * 启用安全 XML 处理配置。
     */
    public static void enableSecureProcessing() {
        setMapper(newSafeMapper());
    }

    /**
     * 校验 XML 最大字节数。
     *
     * @param xml      XML 字符串
     * @param maxBytes 最大字节数
     */
    public static void checkXmlSize(String xml, long maxBytes) {
        requireNonBlank(xml, "xml");
        if (maxBytes < 0) {
            throw new IllegalArgumentException("maxBytes 不能小于 0");
        }
        long size = xml.getBytes(DEFAULT_CHARSET).length;
        if (size > maxBytes) {
            throw new XmlValidationException("XML 内容超过最大字节数限制: " + size + " > " + maxBytes);
        }
    }

    /**
     * 校验 XML 最大嵌套深度。
     *
     * @param xml      XML 字符串
     * @param maxDepth 最大深度
     */
    public static void checkDepth(String xml, int maxDepth) {
        if (maxDepth < 1) {
            throw new IllegalArgumentException("maxDepth 必须大于 0");
        }
        Document document = parseDocument(xml, true);
        int depth = maxDepth(document.getDocumentElement(), 1);
        if (depth > maxDepth) {
            throw new XmlValidationException("XML 嵌套深度超过限制: " + depth + " > " + maxDepth);
        }
    }

    /**
     * 使用安全配置反序列化 XML。
     *
     * @param xml   XML 字符串
     * @param clazz 目标类型
     * @param <T>   目标类型
     * @return 目标对象
     */
    public static <T> T safeFromXml(String xml, Class<T> clazz) {
        requireNonBlank(xml, "xml");
        requireNonNull(clazz, "clazz");
        try {
            return newSafeMapper().readValue(sanitize(xml), clazz);
        } catch (JsonProcessingException e) {
            throw new XmlParseException("安全模式 XML 转对象失败", e);
        }
    }

    /**
     * 使用安全配置读取 JsonNode。
     *
     * @param xml XML 字符串
     * @return JsonNode 节点
     */
    public static JsonNode safeReadTree(String xml) {
        requireNonBlank(xml, "xml");
        try {
            return newSafeMapper().readTree(sanitize(xml));
        } catch (JsonProcessingException e) {
            throw new XmlParseException("安全模式读取 JsonNode 失败", e);
        }
    }

    /**
     * 清理潜在危险 XML 声明内容。
     *
     * @param xml XML 字符串
     * @return 清理后的 XML 字符串
     */
    public static String sanitize(String xml) {
        requireNonBlank(xml, "xml");
        return DANGEROUS_DECLARATION_PATTERN.matcher(xml).replaceAll("");
    }

    /**
     * 将底层异常转换为统一 XML 异常。
     *
     * @param e 原始异常
     * @return XML 异常
     */
    public static XmlException parseException(Exception e) {
        requireNonNull(e, "e");
        if (isParseException(e)) {
            return new XmlParseException(getErrorMessage(e), e);
        }
        if (isMappingException(e)) {
            return new XmlMappingException(getErrorMessage(e), e);
        }
        return new XmlException(getErrorMessage(e), e);
    }

    /**
     * 提取异常可读信息。
     *
     * @param e 异常对象
     * @return 异常信息
     */
    public static String getErrorMessage(Exception e) {
        if (e == null) {
            return null;
        }
        return e.getMessage() == null ? e.getClass().getName() : e.getMessage();
    }

    /**
     * 判断是否为 XML 解析异常。
     *
     * @param e 异常对象
     * @return 是否为解析异常
     */
    public static boolean isParseException(Exception e) {
        return e instanceof XmlParseException || e instanceof JsonProcessingException || e instanceof SAXException;
    }

    /**
     * 判断是否为 XML 对象映射异常。
     *
     * @param e 异常对象
     * @return 是否为映射异常
     */
    public static boolean isMappingException(Exception e) {
        return e instanceof XmlMappingException || e instanceof com.fasterxml.jackson.databind.JsonMappingException;
    }

    /**
     * 包装为 XML 工具类异常。
     *
     * @param message 异常信息
     * @param e       原始异常
     * @return XML 异常
     */
    public static XmlException wrapException(String message, Exception e) {
        return new XmlException(message, e);
    }

    /**
     * 抛出 XML 解析异常。
     *
     * @param xml XML 字符串
     * @param e   原始异常
     */
    public static void throwParseError(String xml, Exception e) {
        String prefix = xml == null ? "null" : summary(xml, 120);
        throw new XmlParseException("XML 解析失败，摘要: " + prefix, e);
    }

    /**
     * 获取默认 XmlMapper。
     *
     * @return 默认 XmlMapper
     */
    public static XmlMapper getMapper() {
        return xmlMapper;
    }

    /**
     * 获取默认配置的 XmlMapper 副本。
     *
     * @return XmlMapper 副本
     */
    public static XmlMapper copyMapper() {
        return xmlMapper.copy();
    }

    /**
     * 替换默认 XmlMapper。
     *
     * @param mapper 新 XmlMapper
     */
    public static void setMapper(XmlMapper mapper) {
        requireNonNull(mapper, "mapper");
        synchronized (MAPPER_LOCK) {
            xmlMapper = mapper;
        }
    }

    /**
     * 自定义默认 XmlMapper 配置。
     *
     * @param customizer 配置回调
     */
    public static void configure(Consumer<XmlMapper> customizer) {
        requireNonNull(customizer, "customizer");
        synchronized (MAPPER_LOCK) {
            customizer.accept(xmlMapper);
        }
    }

    /**
     * 创建新的 XmlMapper。
     *
     * @return 新 XmlMapper
     */
    public static XmlMapper newMapper() {
        return createConfiguredMapper(false);
    }

    /**
     * 创建新的安全 XmlMapper。
     *
     * @return 安全 XmlMapper
     */
    public static XmlMapper newSafeMapper() {
        return createConfiguredMapper(true);
    }

    /**
     * 注册 Jackson 模块。
     *
     * @param module Jackson 模块
     */
    public static void registerModule(Module module) {
        requireNonNull(module, "module");
        xmlMapper.registerModule(module);
    }

    /**
     * 批量注册 Jackson 模块。
     *
     * @param modules Jackson 模块集合
     */
    public static void registerModules(Collection<Module> modules) {
        requireNonNull(modules, "modules");
        for (Module module : modules) {
            registerModule(module);
        }
    }

    /**
     * 设置集合是否默认包装。
     *
     * @param enabled 是否启用默认包装
     */
    public static void setDefaultUseWrapper(boolean enabled) {
        synchronized (MAPPER_LOCK) {
            XmlMapper mapper = XmlMapper.builder().defaultUseWrapper(enabled).build();
            applyDefaultMapperConfig(mapper);
            xmlMapper = mapper;
        }
    }

    /**
     * 设置是否输出 null 字段。
     *
     * @param enabled 是否输出 null 字段
     */
    public static void setIncludeNull(boolean enabled) {
        xmlMapper.setSerializationInclusion(enabled ? JsonInclude.Include.ALWAYS : JsonInclude.Include.NON_NULL);
    }

    /**
     * 设置未知字段是否反序列化报错。
     *
     * @param enabled 是否报错
     */
    public static void setFailOnUnknownProperties(boolean enabled) {
        xmlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, enabled);
    }

    /**
     * 注册 Java 时间模块。
     */
    public static void registerJavaTimeModule() {
        registerModule(new JavaTimeModule());
    }

    /**
     * 设置 Date 类型日期格式。
     *
     * @param pattern 日期格式
     */
    public static void setDateFormat(String pattern) {
        requireNonBlank(pattern, "pattern");
        xmlMapper.setDateFormat(new SimpleDateFormat(pattern));
    }

    /**
     * 设置 LocalDateTime 默认格式。
     *
     * @param pattern 日期时间格式
     */
    public static void setDateTimeFormat(String pattern) {
        requireNonBlank(pattern, "pattern");
        DATE_TIME_FORMATTER.set(DateTimeFormatter.ofPattern(pattern));
    }

    /**
     * 设置默认时区。
     *
     * @param zoneId 时区
     */
    public static void setTimeZone(ZoneId zoneId) {
        requireNonNull(zoneId, "zoneId");
        xmlMapper.setTimeZone(TimeZone.getTimeZone(zoneId));
    }

    /**
     * 设置枚举按名称输出。
     *
     * @param enabled 是否启用
     */
    public static void setEnumAsString(boolean enabled) {
        xmlMapper.configure(SerializationFeature.WRITE_ENUMS_USING_TO_STRING, !enabled);
        xmlMapper.configure(DeserializationFeature.READ_ENUMS_USING_TO_STRING, !enabled);
    }

    /**
     * 设置枚举按 toString 输出。
     *
     * @param enabled 是否启用
     */
    public static void setEnumUsingToString(boolean enabled) {
        xmlMapper.configure(SerializationFeature.WRITE_ENUMS_USING_TO_STRING, enabled);
        xmlMapper.configure(DeserializationFeature.READ_ENUMS_USING_TO_STRING, enabled);
    }

    /**
     * 格式化 LocalDateTime。
     *
     * @param time 日期时间
     * @return 日期时间字符串
     */
    public static String formatDateTime(LocalDateTime time) {
        requireNonNull(time, "time");
        return DATE_TIME_FORMATTER.get().format(time);
    }

    /**
     * 解析 LocalDateTime。
     *
     * @param text 日期时间字符串
     * @return LocalDateTime
     */
    public static LocalDateTime parseDateTime(String text) {
        requireNonBlank(text, "text");
        return LocalDateTime.parse(text, DATE_TIME_FORMATTER.get());
    }

    /**
     * 设置字段命名策略。
     *
     * @param strategy 字段命名策略
     */
    public static void setPropertyNamingStrategy(PropertyNamingStrategy strategy) {
        xmlMapper.setPropertyNamingStrategy(strategy);
    }

    /**
     * 使用下划线命名策略。
     */
    public static void useSnakeCase() {
        setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    }

    /**
     * 使用驼峰命名策略。
     */
    public static void useCamelCase() {
        setPropertyNamingStrategy(PropertyNamingStrategies.LOWER_CAMEL_CASE);
    }

    /**
     * 使用短横线命名策略。
     */
    public static void useKebabCase() {
        setPropertyNamingStrategy(PropertyNamingStrategies.KEBAB_CASE);
    }

    /**
     * 忽略未知字段。
     */
    public static void ignoreUnknownProperties() {
        setFailOnUnknownProperties(false);
    }

    /**
     * 仅输出非空字段。
     */
    public static void includeNonNull() {
        setIncludeNull(false);
    }

    /**
     * 输出所有字段。
     */
    public static void includeAlways() {
        setIncludeNull(true);
    }

    /**
     * 确认当前 XmlMapper 支持文本节点映射。
     *
     * @return 当前 XmlMapper
     */
    public static XmlMapper enableXmlText() {
        return xmlMapper;
    }

    /**
     * 确认当前 XmlMapper 支持属性节点映射。
     *
     * @return 当前 XmlMapper
     */
    public static XmlMapper enableXmlAttribute() {
        return xmlMapper;
    }

    /**
     * 获取指定节点属性。
     *
     * @param xml      XML 字符串
     * @param path     节点路径
     * @param attrName 属性名
     * @return 属性值，不存在时返回 null
     */
    public static String getAttribute(String xml, String path, String attrName) {
        requireNonBlank(attrName, "attrName");
        Element element = findElementByPath(parseDocument(xml, true), path);
        return element == null || !element.hasAttribute(attrName) ? null : element.getAttribute(attrName);
    }

    /**
     * 获取指定节点所有属性。
     *
     * @param xml  XML 字符串
     * @param path 节点路径
     * @return 属性 Map
     */
    public static Map<String, String> getAttributes(String xml, String path) {
        Element element = findElementByPath(parseDocument(xml, true), path);
        if (element == null) {
            return Collections.emptyMap();
        }
        Map<String, String> result = new LinkedHashMap<>();
        NamedNodeMap attrs = element.getAttributes();
        for (int i = 0; i < attrs.getLength(); i++) {
            Node attr = attrs.item(i);
            result.put(attr.getNodeName(), attr.getNodeValue());
        }
        return result;
    }

    /**
     * 判断指定节点属性是否存在。
     *
     * @param xml      XML 字符串
     * @param path     节点路径
     * @param attrName 属性名
     * @return 是否存在
     */
    public static boolean hasAttribute(String xml, String path, String attrName) {
        return getAttribute(xml, path, attrName) != null;
    }

    /**
     * 设置指定节点属性。
     *
     * @param xml      XML 字符串
     * @param path     节点路径
     * @param attrName 属性名
     * @param value    属性值
     * @return 处理后的 XML 字符串
     */
    public static String setAttribute(String xml, String path, String attrName, String value) {
        requireNonBlank(attrName, "attrName");
        Document document = parseDocument(xml, true);
        Element element = findElementByPath(document, path);
        if (element == null) {
            throw new XmlValidationException("XML 节点不存在: " + path);
        }
        element.setAttribute(attrName, value == null ? "" : value);
        return documentToString(document, false, DEFAULT_INDENT, true);
    }

    /**
     * 移除指定节点属性。
     *
     * @param xml      XML 字符串
     * @param path     节点路径
     * @param attrName 属性名
     * @return 处理后的 XML 字符串
     */
    public static String removeAttribute(String xml, String path, String attrName) {
        requireNonBlank(attrName, "attrName");
        Document document = parseDocument(xml, true);
        Element element = findElementByPath(document, path);
        if (element != null) {
            element.removeAttribute(attrName);
        }
        return documentToString(document, false, DEFAULT_INDENT, true);
    }

    /**
     * 获取指定节点文本内容。
     *
     * @param xml  XML 字符串
     * @param path 节点路径
     * @return 文本内容
     */
    public static String getTextContent(String xml, String path) {
        return getValue(xml, path);
    }

    /**
     * 设置指定节点文本内容。
     *
     * @param xml   XML 字符串
     * @param path  节点路径
     * @param value 文本内容
     * @return 处理后的 XML 字符串
     */
    public static String setTextContent(String xml, String path, String value) {
        Document document = parseDocument(xml, true);
        Element element = findElementByPath(document, path);
        if (element == null) {
            throw new XmlValidationException("XML 节点不存在: " + path);
        }
        element.setTextContent(value == null ? "" : value);
        return documentToString(document, false, DEFAULT_INDENT, true);
    }

    /**
     * 判断是否为 SOAP 报文。
     *
     * @param xml XML 字符串
     * @return 是否为 SOAP 报文
     */
    public static boolean isSoap(String xml) {
        if (!isWellFormed(xml)) {
            return false;
        }
        Document document = parseDocument(xml, true);
        String localName = document.getDocumentElement().getLocalName();
        return SOAP_ENVELOPE.equals(localName) || SOAP_ENVELOPE.equals(document.getDocumentElement().getNodeName());
    }

    /**
     * 获取 SOAP Body 内容。
     *
     * @param xml SOAP XML 字符串
     * @return SOAP Body 内部 XML，不存在时返回 null
     */
    public static String getSoapBody(String xml) {
        Element body = findFirstElementByLocalName(parseDocument(xml, true).getDocumentElement(), SOAP_BODY);
        return body == null ? null : innerXml(body);
    }

    /**
     * 获取 SOAP Header 内容。
     *
     * @param xml SOAP XML 字符串
     * @return SOAP Header 内部 XML，不存在时返回 null
     */
    public static String getSoapHeader(String xml) {
        Element header = findFirstElementByLocalName(parseDocument(xml, true).getDocumentElement(), SOAP_HEADER);
        return header == null ? null : innerXml(header);
    }

    /**
     * 将 Body 内容包装为 SOAP Envelope。
     *
     * @param bodyXml Body 内部 XML
     * @return SOAP XML 字符串
     */
    public static String wrapSoapEnvelope(String bodyXml) {
        requireNonBlank(bodyXml, "bodyXml");
        return "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:Body>"
                + removeXmlDeclaration(bodyXml) + "</soap:Body></soap:Envelope>";
    }

    /**
     * 移除 SOAP Envelope 并返回 Body 内容。
     *
     * @param soapXml SOAP XML 字符串
     * @return Body 内部 XML
     */
    public static String unwrapSoapEnvelope(String soapXml) {
        String body = getSoapBody(soapXml);
        if (body == null) {
            throw new XmlValidationException("SOAP Body 不存在");
        }
        return body;
    }

    /**
     * 获取 SOAP Fault 内容。
     *
     * @param soapXml SOAP XML 字符串
     * @return SOAP Fault 内部 XML，不存在时返回 null
     */
    public static String getSoapFault(String soapXml) {
        Element fault = findFirstElementByLocalName(parseDocument(soapXml, true).getDocumentElement(), SOAP_FAULT);
        return fault == null ? null : nodeToString(fault, false, DEFAULT_INDENT, true);
    }

    /**
     * 判断是否存在 SOAP Fault。
     *
     * @param soapXml SOAP XML 字符串
     * @return 是否存在 SOAP Fault
     */
    public static boolean hasSoapFault(String soapXml) {
        return getSoapFault(soapXml) != null;
    }

    /**
     * 生成 XML 摘要。
     *
     * @param xml XML 字符串
     * @return XML 摘要
     */
    public static String summary(String xml) {
        return summary(xml, 512);
    }

    /**
     * 按指定长度生成 XML 摘要。
     *
     * @param xml       XML 字符串
     * @param maxLength 最大长度
     * @return XML 摘要
     */
    public static String summary(String xml, int maxLength) {
        if (xml == null) {
            return null;
        }
        if (maxLength < 0) {
            throw new IllegalArgumentException("maxLength 不能小于 0");
        }
        String normalized = xml.replaceAll("\\s+", " ").trim();
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength) + "...";
    }

    /**
     * 按路径脱敏字段。
     *
     * @param xml   XML 字符串
     * @param paths 节点路径集合
     * @return 脱敏后的 XML
     */
    public static String mask(String xml, Collection<String> paths) {
        requireNonNull(paths, "paths");
        Map<String, String> masks = new LinkedHashMap<>();
        for (String path : paths) {
            masks.put(path, "******");
        }
        return mask(xml, masks);
    }

    /**
     * 按路径和指定脱敏值脱敏字段。
     *
     * @param xml         XML 字符串
     * @param pathMaskMap 路径与脱敏值映射
     * @return 脱敏后的 XML
     */
    public static String mask(String xml, Map<String, String> pathMaskMap) {
        requireNonNull(pathMaskMap, "pathMaskMap");
        Document document = parseDocument(xml, true);
        for (Map.Entry<String, String> entry : pathMaskMap.entrySet()) {
            Element element = findElementByPath(document, entry.getKey());
            if (element != null) {
                element.setTextContent(entry.getValue());
            }
        }
        return documentToString(document, false, DEFAULT_INDENT, true);
    }

    /**
     * 按标签名脱敏字段。
     *
     * @param xml      XML 字符串
     * @param tagNames 标签名集合
     * @return 脱敏后的 XML
     */
    public static String maskByTagName(String xml, Collection<String> tagNames) {
        requireNonNull(tagNames, "tagNames");
        Document document = parseDocument(xml, true);
        for (String tagName : tagNames) {
            NodeList nodes = document.getElementsByTagName(tagName);
            for (int i = 0; i < nodes.getLength(); i++) {
                nodes.item(i).setTextContent("******");
            }
        }
        return documentToString(document, false, DEFAULT_INDENT, true);
    }

    /**
     * 移除敏感节点。
     *
     * @param xml   XML 字符串
     * @param paths 节点路径集合
     * @return 处理后的 XML
     */
    public static String removeSensitiveNodes(String xml, Collection<String> paths) {
        requireNonNull(paths, "paths");
        Document document = parseDocument(xml, true);
        for (String path : paths) {
            Element element = findElementByPath(document, path);
            if (element != null && element.getParentNode() != null) {
                element.getParentNode().removeChild(element);
            }
        }
        return documentToString(document, false, DEFAULT_INDENT, true);
    }

    /**
     * 生成适合日志输出的安全 XML。
     *
     * @param xml XML 字符串
     * @return 安全 XML 摘要
     */
    public static String safeLogXml(String xml) {
        if (xml == null) {
            return null;
        }
        String masked = maskByTagName(xml, List.of("password", "pwd", "token", "secret", "idCard", "phone"));
        return summary(masked);
    }

    private static XmlMapper createConfiguredMapper(boolean safe) {
        XmlMapper mapper = XmlMapper.builder().defaultUseWrapper(false).build();
        applyDefaultMapperConfig(mapper);
        return mapper;
    }

    private static ObjectMapper createJsonMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return mapper;
    }

    private static void applyDefaultMapperConfig(XmlMapper mapper) {
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    private static Document parseDocument(String xml, boolean namespaceAware) {
        requireNonBlank(xml, "xml");
        try {
            DocumentBuilderFactory factory = secureDocumentBuilderFactory(namespaceAware);
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setErrorHandler(new ErrorHandler() {
                @Override
                public void warning(SAXParseException exception) throws SAXException {
                    throw exception;
                }

                @Override
                public void error(SAXParseException exception) throws SAXException {
                    throw exception;
                }

                @Override
                public void fatalError(SAXParseException exception) throws SAXException {
                    throw exception;
                }
            });
            return builder.parse(new InputSource(new StringReader(xml)));
        } catch (ParserConfigurationException | IOException | SAXException e) {
            throw new XmlParseException("XML 格式错误", e);
        }
    }

    private static Document newDocument() {
        try {
            return secureDocumentBuilderFactory(true).newDocumentBuilder().newDocument();
        } catch (ParserConfigurationException e) {
            throw new XmlException("创建 XML Document 失败", e);
        }
    }

    private static DocumentBuilderFactory secureDocumentBuilderFactory(boolean namespaceAware) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(namespaceAware);
        trySetFeature(factory, XMLConstants.FEATURE_SECURE_PROCESSING, true);
        trySetFeature(factory, "http://apache.org/xml/features/disallow-doctype-decl", true);
        trySetFeature(factory, "http://xml.org/sax/features/external-general-entities", false);
        trySetFeature(factory, "http://xml.org/sax/features/external-parameter-entities", false);
        trySetFeature(factory, "http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setExpandEntityReferences(false);
        return factory;
    }

    private static void trySetFeature(DocumentBuilderFactory factory, String feature, boolean value) {
        try {
            factory.setFeature(feature, value);
        } catch (ParserConfigurationException ignored) {
            // 当前 JDK XML 实现不支持该特性时忽略，其他安全特性仍会生效。
        }
    }

    private static TransformerFactory secureTransformerFactory() {
        TransformerFactory factory = TransformerFactory.newInstance();
        try {
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
        } catch (Exception ignored) {
            // 当前 JDK XML 实现不支持该属性时忽略。
        }
        return factory;
    }

    private static SchemaFactory secureSchemaFactory() {
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        try {
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        } catch (Exception ignored) {
            // 当前 JDK XML 实现不支持该属性时忽略。
        }
        return factory;
    }

    private static void validateByXsd(Source xmlSource, Source xsdSource) {
        try {
            Schema schema = secureSchemaFactory().newSchema(xsdSource);
            Validator validator = schema.newValidator();
            validator.validate(xmlSource);
        } catch (SAXException | IOException e) {
            throw new XmlValidationException("XSD 校验失败", e);
        }
    }

    private static String documentToString(Document document, boolean indent, int indentAmount, boolean omitDeclaration) {
        return nodeToString(document, indent, indentAmount, omitDeclaration);
    }

    private static String nodeToString(Node node, boolean indent, int indentAmount, boolean omitDeclaration) {
        try {
            Transformer transformer = secureTransformerFactory().newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, DEFAULT_CHARSET.name());
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, omitDeclaration ? "yes" : "no");
            if (indent) {
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", String.valueOf(indentAmount));
            } else {
                transformer.setOutputProperty(OutputKeys.INDENT, "no");
            }
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(node), new StreamResult(writer));
            return writer.toString().trim();
        } catch (TransformerException e) {
            throw new XmlException("XML 节点转换字符串失败", e);
        }
    }

    private static String findDeclarationValue(String xml, Pattern pattern) {
        if (xml == null) {
            return null;
        }
        Matcher matcher = XML_DECLARATION_PATTERN.matcher(xml);
        if (!matcher.find()) {
            return null;
        }
        Matcher valueMatcher = pattern.matcher(matcher.group(1));
        return valueMatcher.find() ? valueMatcher.group(1) : null;
    }

    private static JsonNode findNode(JsonNode node, String path) {
        requireNonNull(node, "node");
        requireNonBlank(path, "path");
        String pointer = path.startsWith("/") ? path : "/" + path.replace('.', '/');
        return node.at(pointer);
    }

    private static Element findElementByPath(Document document, String path) {
        List<Element> elements = findElementsByPath(document, path);
        return elements.isEmpty() ? null : elements.getFirst();
    }

    private static List<Element> findElementsByPath(Document document, String path) {
        requireNonNull(document, "document");
        requireNonBlank(path, "path");
        String normalized = path.replace('.', '/');
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        String[] parts = Arrays.stream(normalized.split("/"))
                .filter(part -> !part.isBlank())
                .toArray(String[]::new);
        if (parts.length == 0) {
            return Collections.emptyList();
        }
        Element root = document.getDocumentElement();
        int index = localOrNodeNameEquals(root, parts[0]) ? 1 : 0;
        List<Element> current = new ArrayList<>();
        current.add(root);
        for (int i = index; i < parts.length; i++) {
            List<Element> next = new ArrayList<>();
            for (Element element : current) {
                NodeList children = element.getChildNodes();
                for (int j = 0; j < children.getLength(); j++) {
                    Node child = children.item(j);
                    if (child.getNodeType() == Node.ELEMENT_NODE && localOrNodeNameEquals(child, parts[i])) {
                        next.add((Element) child);
                    }
                }
            }
            current = next;
            if (current.isEmpty()) {
                return Collections.emptyList();
            }
        }
        return current;
    }

    private static boolean localOrNodeNameEquals(Node node, String expected) {
        return expected.equals(node.getNodeName()) || expected.equals(node.getLocalName());
    }

    private static void collectNamespaces(Element element, Map<String, String> result) {
        NamedNodeMap attributes = element.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Node node = attributes.item(i);
            String name = node.getNodeName();
            if ("xmlns".equals(name)) {
                result.put("", node.getNodeValue());
            } else if (name.startsWith("xmlns:")) {
                result.put(name.substring("xmlns:".length()), node.getNodeValue());
            }
        }
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                collectNamespaces((Element) child, result);
            }
        }
    }

    private static Node copyWithoutNamespace(Node source, Document target) {
        switch (source.getNodeType()) {
            case Node.ELEMENT_NODE -> {
                String name = source.getLocalName() == null ? source.getNodeName() : source.getLocalName();
                Element element = target.createElement(name);
                NamedNodeMap attrs = source.getAttributes();
                for (int i = 0; i < attrs.getLength(); i++) {
                    Attr attr = (Attr) attrs.item(i);
                    if (!attr.getName().startsWith("xmlns")) {
                        String attrName = attr.getLocalName() == null ? attr.getName() : attr.getLocalName();
                        element.setAttribute(attrName, attr.getValue());
                    }
                }
                NodeList children = source.getChildNodes();
                for (int i = 0; i < children.getLength(); i++) {
                    element.appendChild(copyWithoutNamespace(children.item(i), target));
                }
                return element;
            }
            case Node.CDATA_SECTION_NODE -> {
                return target.createCDATASection(source.getTextContent());
            }
            case Node.TEXT_NODE -> {
                return target.createTextNode(source.getTextContent());
            }
            default -> {
                return target.createTextNode(source.getTextContent() == null ? "" : source.getTextContent());
            }
        }
    }

    private static Element findFirstElementByLocalName(Element root, String localName) {
        if (localOrNodeNameEquals(root, localName)) {
            return root;
        }
        NodeList children = root.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                Element found = findFirstElementByLocalName((Element) child, localName);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static String innerXml(Element element) {
        StringBuilder builder = new StringBuilder();
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE || child.getNodeType() == Node.CDATA_SECTION_NODE) {
                builder.append(nodeToString(child, false, DEFAULT_INDENT, true));
            } else if (child.getNodeType() == Node.TEXT_NODE && !child.getTextContent().isBlank()) {
                builder.append(escapeText(child.getTextContent()));
            }
        }
        return builder.toString();
    }

    private static Map<String, Object> flattenMap(Map<String, Object> map, String prefix) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = prefix.isEmpty() ? entry.getKey() : prefix + '.' + entry.getKey();
            if (entry.getValue() instanceof Map<?, ?> nested) {
                Map<String, Object> typed = new LinkedHashMap<>();
                for (Map.Entry<?, ?> nestedEntry : nested.entrySet()) {
                    typed.put(String.valueOf(nestedEntry.getKey()), nestedEntry.getValue());
                }
                result.putAll(flattenMap(typed, key));
            } else {
                result.put(key, entry.getValue());
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static void putNested(Map<String, Object> map, String dottedPath, Object value) {
        requireNonBlank(dottedPath, "dottedPath");
        String[] parts = dottedPath.split("\\.");
        Map<String, Object> current = map;
        for (int i = 0; i < parts.length - 1; i++) {
            Object child = current.get(parts[i]);
            if (!(child instanceof Map<?, ?>)) {
                child = new LinkedHashMap<String, Object>();
                current.put(parts[i], child);
            }
            current = (Map<String, Object>) child;
        }
        current.put(parts[parts.length - 1], value);
    }

    private static int maxDepth(Element element, int depth) {
        int max = depth;
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                max = Math.max(max, maxDepth((Element) child, depth + 1));
            }
        }
        return max;
    }

    private static void requireNonNull(Object value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " 不能为 null");
        }
    }

    private static void requireNonBlank(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " 不能为空");
        }
    }

    private static void requireNonEmpty(byte[] value, String name) {
        if (value == null || value.length == 0) {
            throw new IllegalArgumentException(name + " 不能为空");
        }
    }
}
