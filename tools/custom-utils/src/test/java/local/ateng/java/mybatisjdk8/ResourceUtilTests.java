package local.ateng.java.mybatisjdk8;


import local.ateng.java.customutils.utils.ResourceUtil;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

/**
 * ResourceUtil 功能测试类
 *
 * @author Ateng
 * @since 2026-04-22
 */
public class ResourceUtilTests {

    /**
     * 字符串转 Resource 并读取
     */
    @Test
    public void test_fromString_and_read() {
        printTitle("字符串转 Resource");

        Resource resource = ResourceUtil.fromString("我叫阿腾，今年26岁", "test.txt", StandardCharsets.UTF_8);

        System.out.println("文件名：" + ResourceUtil.getFilename(resource));
        System.out.println("内容：" + ResourceUtil.readString(resource));
    }

    /**
     * ClassPath 资源读取
     */
    @Test
    public void test_classpath_read() {
        printTitle("ClassPath 资源读取");

        Resource resource = ResourceUtil.getClassPathResource("application.yml");

        System.out.println("是否存在：" + ResourceUtil.exists(resource));
        if (ResourceUtil.exists(resource)) {
            System.out.println("内容：" + substring(ResourceUtil.readString(resource), 100));
        }
    }

    /**
     * URL 资源
     */
    @Test
    public void test_url_resource() {
        printTitle("URL 资源");

        Resource resource = ResourceUtil.getUrlResource("https://raw.githubusercontent.com/atengk/Ateng-Java/refs/heads/main/README.md");

        System.out.println("类型：" + resource.getClass().getSimpleName());
        System.out.println("ContentType：" + ResourceUtil.guessContentType(resource));
    }

    /**
     * 读取为字节数组
     */
    @Test
    public void test_read_bytes() {
        printTitle("读取 byte[]");

        Resource resource = ResourceUtil.fromString("测试字节数组");

        byte[] bytes = ResourceUtil.readBytes(resource);

        System.out.println("字节长度：" + bytes.length);
    }

    /**
     * 按行读取
     */
    @Test
    public void test_read_lines() {
        printTitle("按行读取");

        Resource resource = ResourceUtil.fromString("a\nb\nc");

        List<String> lines = ResourceUtil.readLines(resource);

        System.out.println("行数：" + lines.size());
        for (String line : lines) {
            System.out.println("行：" + line);
        }
    }

    /**
     * 复制到临时文件
     */
    @Test
    public void test_copy_to_temp_file() {
        printTitle("复制到临时文件");

        Resource resource = ResourceUtil.fromString("测试文件");

        File file = ResourceUtil.copyToTempFile(resource, "demo-", ".txt");

        System.out.println("路径：" + file.getAbsolutePath());
        System.out.println("大小：" + file.length());
    }

    /**
     * 转 Path（预期失败）
     */
    @Test
    public void test_toPath_fail() {
        printTitle("普通 Resource 转 Path（失败场景）");

        Resource resource = ResourceUtil.fromString("测试");

        try {
            Path path = ResourceUtil.toPath(resource);
            System.out.println("Path：" + path);
        } catch (Exception e) {
            System.out.println("失败原因：" + e.getMessage());
        }
        // 失败原因：资源无法转换为 Path，resource=Byte array resource [resource loaded from byte array]
    }

    /**
     * 保存到目录
     */
    @Test
    public void test_save_to_dir() {
        printTitle("保存到目录");

        Resource resource = ResourceUtil.fromString("保存测试", "demo.txt", StandardCharsets.UTF_8);

        File dir = new File(System.getProperty("java.io.tmpdir"), "resource-test");

        File file = ResourceUtil.saveToDirectory(resource, dir);

        System.out.println("保存路径：" + file.getAbsolutePath());
    }

    /**
     * 资源扫描
     */
    @Test
    public void test_scan_resources() {
        printTitle("资源扫描");

        Resource[] resources = ResourceUtil.getResources("classpath*:*.yml");

        System.out.println("数量：" + resources.length);
        for (Resource r : resources) {
            System.out.println("文件：" + ResourceUtil.getFilename(r));
        }
    }

    /**
     * Properties 读取
     */
    @Test
    public void test_properties() {
        printTitle("Properties 读取");

        Resource resource = ResourceUtil.getClassPathResource("application.properties");

        if (!ResourceUtil.exists(resource)) {
            System.out.println("文件不存在，跳过");
            return;
        }

        Properties properties = ResourceUtil.loadProperties(resource);

        for (String key : properties.stringPropertyNames()) {
            System.out.println(key + "=" + properties.getProperty(key));
        }
    }

    /**
     * 元信息
     */
    @Test
    public void test_meta_info() {
        printTitle("元信息");

        Resource resource = ResourceUtil.fromString("测试内容", "demo.txt", StandardCharsets.UTF_8);

        System.out.println("文件名：" + ResourceUtil.getFilename(resource));
        System.out.println("扩展名：" + ResourceUtil.getExtension(resource));
        System.out.println("ContentType：" + ResourceUtil.getContentType(resource, "unknown"));
        System.out.println("长度：" + ResourceUtil.safeContentLength(resource));
        System.out.println("是否存在：" + ResourceUtil.exists(resource));
        System.out.println("是否可读：" + ResourceUtil.isReadable(resource));
    }

    // ======================== 工具方法 ========================

    private void printTitle(String title) {
        System.out.println("\n==================== " + title + " ====================");
    }

    private String substring(String str, int maxLen) {
        if (str == null) {
            return null;
        }
        return str.length() <= maxLen ? str : str.substring(0, maxLen) + "...";
    }
}
