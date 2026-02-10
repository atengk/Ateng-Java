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
