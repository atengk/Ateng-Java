# Apache Tika

**Apache Tika** 是一个开源的 Java 库，被誉为文件处理领域的“瑞士军刀”。它的核心使命是：**从各种格式的文档中检测类型并提取元数据和文本内容。**

无论你交给它的是 PDF、Word、Excel、图片、音频还是视频，Tika 都能通过统一的接口告诉你：“这是什么类型的文件”以及“文件里写了什么”。

- [官网地址](https://tika.apache.org/)



## 基础配置

### 添加依赖

```xml
<properties>
    <tika.version>3.2.3</tika.version>
</properties>
<dependencies>
    <!-- Apache Tika 检测库 -->
    <dependency>
        <groupId>org.apache.tika</groupId>
        <artifactId>tika-core</artifactId>
        <version>${tika.version}</version>
    </dependency>
    <!-- Apache Tika 解析内容库 -->
    <dependency>
        <groupId>org.apache.tika</groupId>
        <artifactId>tika-parsers-standard-package</artifactId>
        <version>${tika.version}</version>
    </dependency>
</dependencies>
```

### 创建 TikaUtil 

```java
package io.github.atengk.tika.util;

import org.apache.tika.Tika;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Apache Tika 工具类
 * <p>
 * 提供文件类型检测、文本内容提取、元数据解析等能力
 *
 * @author Ateng
 * @since 2026-02-09
 */
public final class TikaUtil {

    private static final Logger log = LoggerFactory.getLogger(TikaUtil.class);

    /**
     * 默认最大文本提取长度
     */
    private static final int DEFAULT_MAX_CONTENT_LENGTH = 100_000;

    /**
     * 线程安全的 Tika 实例
     */
    private static final Tika TIKA = new Tika();

    /**
     * 自动检测解析器
     */
    private static final AutoDetectParser PARSER = new AutoDetectParser();

    private TikaUtil() {
    }

    /* ========================= type ========================= */

    /**
     * 检测文件 MIME 类型
     *
     * @param file 文件对象
     * @return MIME 类型，失败返回 null
     */
    public static String detect(File file) {
        if (file == null) {
            return null;
        }
        try {
            return TIKA.detect(file);
        } catch (Exception e) {
            log.warn("Detect file type failed: {}", file.getAbsolutePath(), e);
            return null;
        }
    }

    /**
     * 检测字节数据 MIME 类型
     *
     * @param data 文件字节数据
     * @return MIME 类型，失败返回 null
     */
    public static String detect(byte[] data) {
        if (data == null || data.length == 0) {
            return null;
        }
        try {
            return TIKA.detect(data);
        } catch (Exception e) {
            log.warn("Detect byte[] type failed", e);
            return null;
        }
    }

    /**
     * 检测输入流 MIME 类型
     *
     * @param inputStream 输入流
     * @return MIME 类型，失败返回 null
     */
    public static String detect(InputStream inputStream) {
        if (inputStream == null) {
            return null;
        }
        try {
            return TIKA.detect(inputStream);
        } catch (Exception e) {
            log.warn("Detect InputStream type failed", e);
            return null;
        }
    }

    /**
     * 是否为图片类型
     *
     * @param mimeType MIME 类型
     * @return 是否为图片
     */
    public static boolean isImage(String mimeType) {
        return mimeType != null && mimeType.startsWith("image/");
    }

    /**
     * 是否需要 OCR 处理
     *
     * @param mimeType MIME 类型
     * @return 是否需要 OCR
     */
    public static boolean needOcr(String mimeType) {
        if (mimeType == null) {
            return false;
        }
        return isImage(mimeType);
    }

    /**
     * 是否为音频类型
     *
     * @param mimeType MIME 类型
     * @return 是否为音频
     */
    public static boolean isAudio(String mimeType) {
        return mimeType != null && mimeType.startsWith("audio/");
    }

    /**
     * 是否为视频类型
     *
     * @param mimeType MIME 类型
     * @return 是否为视频
     */
    public static boolean isVideo(String mimeType) {
        return mimeType != null && mimeType.startsWith("video/");
    }

    /**
     * 是否为 PDF
     *
     * @param mimeType MIME 类型
     * @return 是否为 PDF
     */
    public static boolean isPdf(String mimeType) {
        return "application/pdf".equals(mimeType);
    }

    /**
     * 是否为 Office 文档
     *
     * @param mimeType MIME 类型
     * @return 是否为 Office 文档
     */
    public static boolean isOffice(String mimeType) {
        if (mimeType == null) {
            return false;
        }
        return mimeType.startsWith("application/msword")
                || mimeType.startsWith("application/vnd.ms-")
                || mimeType.startsWith("application/vnd.openxmlformats-officedocument");
    }

    /**
     * 是否为可解析文本类型
     *
     * @param mimeType MIME 类型
     * @return 是否可能包含正文文本
     */
    public static boolean isTextual(String mimeType) {
        if (mimeType == null) {
            return false;
        }
        return mimeType.startsWith("text/")
                || isPdf(mimeType)
                || isOffice(mimeType);
    }

    /**
     * 校验 MIME 类型是否在白名单中
     *
     * @param mimeType MIME 类型
     * @param allowed  允许的 MIME 类型集合
     * @return 是否允许
     */
    public static boolean isAllowed(String mimeType, Set<String> allowed) {
        if (mimeType == null || allowed == null || allowed.isEmpty()) {
            return false;
        }
        return allowed.contains(mimeType);
    }

    /**
     * 校验文件扩展名与 MIME 是否匹配
     *
     * @param file     文件
     * @param mimeType MIME 类型
     * @return 是否匹配
     */
    public static boolean isExtensionMatch(File file, String mimeType) {
        if (file == null || mimeType == null) {
            return false;
        }
        String name = file.getName().toLowerCase();

        if (name.endsWith(".pdf")) {
            return isPdf(mimeType);
        }
        if (name.endsWith(".docx") || name.endsWith(".doc")) {
            return isOffice(mimeType);
        }
        if (name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg")) {
            return isImage(mimeType);
        }
        return true;
    }

    /**
     * 是否为可安全解析文件
     *
     * @param file        文件
     * @param allowedMime 允许的 MIME 类型
     * @param maxBytes    最大文件大小
     * @return 是否可解析
     */
    public static boolean canParse(File file, Set<String> allowedMime, long maxBytes) {
        if (isEmpty(file) || isTooLarge(file, maxBytes)) {
            return false;
        }
        String mimeType = detect(file);
        return isAllowed(mimeType, allowedMime) && isExtensionMatch(file, mimeType);
    }


    /* ========================= text ========================= */

    /**
     * 提取文件文本内容
     *
     * @param file 文件对象
     * @return 文本内容，失败返回空字符串
     */
    public static String parseText(File file) {
        if (file == null) {
            return "";
        }
        try (InputStream inputStream = new FileInputStream(file)) {
            return parseText(inputStream, DEFAULT_MAX_CONTENT_LENGTH);
        } catch (Exception e) {
            log.warn("Parse text from file failed: {}", file.getAbsolutePath(), e);
            return "";
        }
    }

    /**
     * 提取字节数据文本内容
     *
     * @param data 文件字节数据
     * @return 文本内容，失败返回空字符串
     */
    public static String parseText(byte[] data) {
        if (data == null || data.length == 0) {
            return "";
        }
        try (InputStream inputStream = new ByteArrayInputStream(data)) {
            return parseText(inputStream, DEFAULT_MAX_CONTENT_LENGTH);
        } catch (Exception e) {
            log.warn("Parse text from byte[] failed", e);
            return "";
        }
    }

    /**
     * 提取输入流文本内容
     *
     * @param inputStream      输入流
     * @param maxContentLength 最大提取字符数，< 0 表示不限制
     * @return 文本内容，失败返回空字符串
     */
    public static String parseText(InputStream inputStream, int maxContentLength) {
        if (inputStream == null) {
            return "";
        }

        try {
            int limit = maxContentLength < 0 ? -1 : maxContentLength;
            BodyContentHandler handler = new BodyContentHandler(limit);
            Metadata metadata = new Metadata();
            ParseContext context = new ParseContext();

            PARSER.parse(inputStream, handler, metadata, context);

            return handler.toString();
        } catch (Exception e) {
            log.warn("Parse text from InputStream failed", e);
            return "";
        }
    }

    /* ========================= metadata ========================= */

    /**
     * 解析文件元数据
     *
     * @param file 文件对象
     * @return 元数据 Map，失败返回空 Map
     */
    public static Map<String, String> parseMetadata(File file) {
        if (file == null) {
            return Collections.emptyMap();
        }
        try (InputStream inputStream = new FileInputStream(file)) {
            return parseMetadata(inputStream);
        } catch (Exception e) {
            log.warn("Parse metadata from file failed: {}", file.getAbsolutePath(), e);
            return Collections.emptyMap();
        }
    }

    /**
     * 获取指定元数据值
     *
     * @param file 文件
     * @param key  元数据 key
     * @return 元数据值，不存在返回 null
     */
    public static String getMetadata(File file, String key) {
        if (file == null || key == null) {
            return null;
        }
        Map<String, String> metadata = parseMetadata(file);
        return metadata.get(key);
    }

    /**
     * 解析字节数据元数据
     *
     * @param data 文件字节数据
     * @return 元数据 Map，失败返回空 Map
     */
    public static Map<String, String> parseMetadata(byte[] data) {
        if (data == null || data.length == 0) {
            return Collections.emptyMap();
        }
        try (InputStream inputStream = new ByteArrayInputStream(data)) {
            return parseMetadata(inputStream);
        } catch (Exception e) {
            log.warn("Parse metadata from byte[] failed", e);
            return Collections.emptyMap();
        }
    }

    /**
     * 解析输入流元数据
     *
     * @param inputStream 输入流
     * @return 元数据 Map，失败返回空 Map
     */
    public static Map<String, String> parseMetadata(InputStream inputStream) {
        if (inputStream == null) {
            return Collections.emptyMap();
        }
        try {
            BodyContentHandler handler = new BodyContentHandler(-1);
            Metadata metadata = new Metadata();
            ParseContext context = new ParseContext();

            PARSER.parse(inputStream, handler, metadata, context);

            return toMap(metadata);
        } catch (Exception e) {
            log.warn("Parse metadata from InputStream failed", e);
            return Collections.emptyMap();
        }
    }

    /* ========================= full ========================= */

    /**
     * 同时解析文本内容和元数据
     *
     * @param file 文件对象
     * @return 解析结果，失败返回 null
     */
    public static TikaResult parseAll(File file) {
        if (file == null) {
            return null;
        }
        try (InputStream inputStream = new FileInputStream(file)) {
            return parseAll(inputStream, DEFAULT_MAX_CONTENT_LENGTH);
        } catch (Exception e) {
            log.warn("Parse all from file failed: {}", file.getAbsolutePath(), e);
            return null;
        }
    }

    /**
     * 同时解析文本内容和元数据
     *
     * @param inputStream      输入流
     * @param maxContentLength 最大提取字符数
     * @return 解析结果，失败返回 null
     */
    public static TikaResult parseAll(InputStream inputStream, int maxContentLength) {
        if (inputStream == null) {
            return null;
        }

        try {
            int limit = maxContentLength < 0 ? -1 : maxContentLength;
            BodyContentHandler handler = new BodyContentHandler(limit);
            Metadata metadata = new Metadata();
            ParseContext context = new ParseContext();

            PARSER.parse(inputStream, handler, metadata, context);

            return new TikaResult(handler.toString(), toMap(metadata));
        } catch (Exception e) {
            log.warn("Parse all from InputStream failed", e);
            return null;
        }
    }

    /* ========================= helper ========================= */

    private static Map<String, String> toMap(Metadata metadata) {
        if (metadata == null || metadata.size() == 0) {
            return Collections.emptyMap();
        }
        Map<String, String> map = new HashMap<>(metadata.size());
        for (String name : metadata.names()) {
            map.put(name, metadata.get(name));
        }
        return map;
    }

    /* ========================= result ========================= */

    /**
     * Tika 解析结果封装
     */
    public static final class TikaResult {

        private final String content;
        private final Map<String, String> metadata;

        public TikaResult(String content, Map<String, String> metadata) {
            this.content = content;
            this.metadata = metadata;
        }

        public String getContent() {
            return content;
        }

        public Map<String, String> getMetadata() {
            return metadata;
        }
    }

    /* ========================= size ========================= */

    /**
     * 是否为空文件
     *
     * @param file 文件
     * @return 是否为空
     */
    public static boolean isEmpty(File file) {
        return file == null || !file.exists() || file.length() == 0;
    }

    /**
     * 是否超过最大文件大小
     *
     * @param file     文件
     * @param maxBytes 最大字节数
     * @return 是否超限
     */
    public static boolean isTooLarge(File file, long maxBytes) {
        if (file == null || maxBytes <= 0) {
            return false;
        }
        return file.length() > maxBytes;
    }

}
```



## 使用测试

```java
package io.github.atengk.tika;

import io.github.atengk.tika.util.TikaUtil;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Apache Tika 工具类测试
 */
public class TikaTests {

    /**
     * 测试文件类型检测（File）
     */
    @Test
    void testDetectByFile() {
        File file = new File("D:\\Temp\\pdf\\demo_simple.pdf");

        String mimeType = TikaUtil.detect(file);

        System.out.println("MIME Type (File): " + mimeType);
    }

    /**
     * 测试文件是否为图片类型
     */
    @Test
    void testisImage() {
        File file = new File("D:\\temp\\demo.pdf");

        String mimeType = TikaUtil.detect(file);
        boolean isImage = TikaUtil.isImage(mimeType);

        System.out.println("MIME Type isImage: " + isImage);
    }

    /**
     * 测试文件类型检测（byte[]）
     */
    @Test
    void testDetectByBytes() {
        byte[] data = "Hello Tika".getBytes(StandardCharsets.UTF_8);

        String mimeType = TikaUtil.detect(data);

        System.out.println("MIME Type (byte[]): " + mimeType);
    }

    /**
     * 测试文本内容提取（File）
     */
    @Test
    void testParseTextByFile() {
        File file = new File("D:\\Temp\\word\\demo.docx");

        String content = TikaUtil.parseText(file);

        System.out.println("Text Content (File):");
        System.out.println(content);
    }

    /**
     * 测试文本内容提取（byte[]）
     */
    @Test
    void testParseTextByBytes() {
        byte[] data = "Apache Tika Test Content".getBytes(StandardCharsets.UTF_8);

        String content = TikaUtil.parseText(data);

        System.out.println("Text Content (byte[]):");
        System.out.println(content);
    }

    /**
     * 测试元数据解析（File）
     */
    @Test
    void testParseMetadataByFile() {
        File file = new File("D:\\Temp\\word\\demo.docx");

        Map<String, String> metadata = TikaUtil.parseMetadata(file);

        System.out.println("Metadata (File):");
        metadata.forEach((key, value) ->
                System.out.println(key + " = " + value)
        );
    }

    /**
     * 测试同时解析文本和元数据
     */
    @Test
    void testParseAll() {
        File file = new File("D:\\Temp\\word\\demo.docx");

        TikaUtil.TikaResult result = TikaUtil.parseAll(file);

        if (result == null) {
            System.out.println("Parse result is null");
            return;
        }

        System.out.println("Full Parse Result:");
        System.out.println("---- Content ----");
        System.out.println(result.getContent());

        System.out.println("---- Metadata ----");
        result.getMetadata().forEach((key, value) ->
                System.out.println(key + " = " + value)
        );
    }

    /**
     * 测试超长文本限制
     */
    @Test
    void testParseTextWithLimit() {
        File file = new File("test-files/large.pdf");

        String content = TikaUtil.parseText(file);

        System.out.println("Limited Text Length: " + content.length());
    }

    /**
     * 测试异常场景（文件不存在）
     */
    @Test
    void testFileNotExists() {
        File file = new File("test-files/not-exists.pdf");

        String mimeType = TikaUtil.detect(file);
        String content = TikaUtil.parseText(file);
        Map<String, String> metadata = TikaUtil.parseMetadata(file);

        System.out.println("MIME Type: " + mimeType);
        System.out.println("Content: " + content);
        System.out.println("Metadata size: " + metadata.size());
    }
}

```

