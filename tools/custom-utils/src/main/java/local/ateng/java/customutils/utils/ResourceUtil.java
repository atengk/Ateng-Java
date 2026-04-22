package local.ateng.java.customutils.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.*;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.util.ResourceUtils;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Spring Resource 通用工具类。
 * <p>
 * 适用于项目中对 {@link Resource} 的加载、读取、转换、复制、扫描、落地等常见场景。
 *
 * @author Ateng
 * @since 2026-04-22
 */
public final class ResourceUtil {

    private static final Logger log = LoggerFactory.getLogger(ResourceUtil.class);

    private static final int DEFAULT_BUFFER_SIZE = 8 * 1024;

    /**
     * 单资源加载器：支持 classpath:、file:、http: 等常见协议。
     */
    private static final DefaultResourceLoader DEFAULT_RESOURCE_LOADER = new DefaultResourceLoader();

    /**
     * 通配符资源解析器：支持 classpath*:、classpath*:/xxx/*.xml 等扫描场景。
     */
    private static final PathMatchingResourcePatternResolver RESOURCE_PATTERN_RESOLVER =
            new PathMatchingResourcePatternResolver(DEFAULT_RESOURCE_LOADER);

    /**
     * 禁止实例化工具类。
     */
    private ResourceUtil() {
        throw new UnsupportedOperationException("工具类不可实例化");
    }


    /**
     * 获取单个资源。
     * <p>
     * 适合加载非通配符资源，例如：
     * classpath:application.yml
     * file:/data/test.txt
     * /opt/logs/a.log
     * https://example.com/demo.txt
     *
     * @param location 资源位置
     * @return Resource
     */
    public static Resource getResource(String location) {
        assertText(location, "资源位置不能为空");

        if (isPatternLocation(location)) {
            Resource[] resources = getResources(location);
            if (resources.length == 0) {
                throw new ResourceUtilException("未找到匹配的资源：" + location);
            }
            if (resources.length > 1) {
                log.warn("资源位置包含通配符且匹配到了多个资源，已返回第一个，location={}", location);
            }
            return resources[0];
        }

        return DEFAULT_RESOURCE_LOADER.getResource(location);
    }

    /**
     * 扫描并获取多个资源。
     * <p>
     * 支持：
     * classpath*:mapper/*&#47;.xml
     * classpath*:com/example/**&#47;*.yml
     * file:/opt/app/config/*&#47;.properties
     *
     * @param locationPattern 资源模式
     * @return Resource 数组，未命中时返回空数组
     */
    public static Resource[] getResources(String locationPattern) {
        if (!hasText(locationPattern)) {
            return new Resource[0];
        }
        try {
            Resource[] resources = RESOURCE_PATTERN_RESOLVER.getResources(locationPattern);
            return resources == null ? new Resource[0] : resources;
        } catch (IOException e) {
            throw new ResourceUtilException("扫描资源失败，pattern=" + locationPattern, e);
        }
    }

    /**
     * 获取 ClassPath 资源。
     *
     * @param path classpath 路径
     * @return Resource
     */
    public static Resource getClassPathResource(String path) {
        assertText(path, "Classpath 路径不能为空");
        return new ClassPathResource(normalizeClassPath(path));
    }

    /**
     * 获取文件系统资源。
     *
     * @param path 文件路径
     * @return Resource
     */
    public static Resource getFileSystemResource(String path) {
        assertText(path, "文件路径不能为空");
        return new FileSystemResource(path);
    }

    /**
     * 获取 URL 资源。
     *
     * @param url URL
     * @return Resource
     */
    public static Resource getUrlResource(String url) {
        assertText(url, "URL 不能为空");
        try {
            return new UrlResource(url);
        } catch (Exception e) {
            throw new ResourceUtilException("创建 UrlResource 失败，url=" + url, e);
        }
    }

    /**
     * 获取 URL 资源。
     *
     * @param url URL
     * @return Resource
     */
    public static Resource getUrlResource(URL url) {
        if (url == null) {
            throw new ResourceUtilException("URL 不能为空");
        }
        try {
            return new UrlResource(url);
        } catch (Exception e) {
            throw new ResourceUtilException("创建 UrlResource 失败，url=" + url, e);
        }
    }

    /**
     * 获取 URI 资源。
     *
     * @param uri URI
     * @return Resource
     */
    public static Resource getUrlResource(URI uri) {
        if (uri == null) {
            throw new ResourceUtilException("URI 不能为空");
        }
        try {
            return new UrlResource(uri);
        } catch (Exception e) {
            throw new ResourceUtilException("创建 UrlResource 失败，uri=" + uri, e);
        }
    }

    /**
     * 获取字节数组资源。
     *
     * @param bytes 字节数组
     * @return Resource
     */
    public static Resource getByteArrayResource(byte[] bytes) {
        return getByteArrayResource(bytes, null);
    }

    /**
     * 获取字节数组资源，并可指定文件名。
     *
     * @param bytes    字节数组
     * @param filename 文件名
     * @return Resource
     */
    public static Resource getByteArrayResource(byte[] bytes, String filename) {
        if (bytes == null) {
            throw new ResourceUtilException("字节数组不能为空");
        }

        final byte[] copy = bytes.clone();
        if (!hasText(filename)) {
            return new ByteArrayResource(copy);
        }

        return new ByteArrayResource(copy) {
            @Override
            public String getFilename() {
                return filename;
            }
        };
    }

    /**
     * 获取输入流资源。
     * <p>
     * 注意：InputStreamResource 通常只能读取一次。
     *
     * @param inputStream 输入流
     * @return Resource
     */
    public static Resource getInputStreamResource(InputStream inputStream) {
        return getInputStreamResource(inputStream, null);
    }

    /**
     * 获取输入流资源，并可指定文件名。
     * <p>
     * 注意：InputStreamResource 通常只能读取一次。
     *
     * @param inputStream 输入流
     * @param filename    文件名
     * @return Resource
     */
    public static Resource getInputStreamResource(InputStream inputStream, String filename) {
        if (inputStream == null) {
            throw new ResourceUtilException("输入流不能为空");
        }

        InputStreamResource resource = new InputStreamResource(inputStream) {
            @Override
            public String getFilename() {
                return filename;
            }
        };
        return resource;
    }

    /**
     * 将字符串内容转为资源，默认使用 UTF-8。
     *
     * @param content 字符串内容
     * @return Resource
     */
    public static Resource fromString(String content) {
        return fromString(content, StandardCharsets.UTF_8);
    }

    /**
     * 将字符串内容转为资源。
     *
     * @param content 字符串内容
     * @param charset 字符集
     * @return Resource
     */
    public static Resource fromString(String content, Charset charset) {
        if (content == null) {
            throw new ResourceUtilException("字符串内容不能为空");
        }
        Charset useCharset = getCharset(charset);
        return new ByteArrayResource(content.getBytes(useCharset));
    }

    /**
     * 将字符串内容转为带文件名的资源。
     *
     * @param content  字符串内容
     * @param filename 文件名
     * @param charset  字符集
     * @return Resource
     */
    public static Resource fromString(String content, String filename, Charset charset) {
        if (content == null) {
            throw new ResourceUtilException("字符串内容不能为空");
        }
        Charset useCharset = getCharset(charset);
        byte[] bytes = content.getBytes(useCharset);
        return getByteArrayResource(bytes, filename);
    }

    /**
     * 读取资源为字节数组。
     *
     * @param resource 资源
     * @return 字节数组
     */
    public static byte[] readBytes(Resource resource) {
        assertResource(resource);
        try (InputStream inputStream = resource.getInputStream()) {
            return toByteArray(inputStream);
        } catch (IOException e) {
            throw new ResourceUtilException("读取资源字节失败，resource=" + getDescription(resource), e);
        }
    }

    /**
     * 读取资源为字符串，默认使用 UTF-8。
     *
     * @param resource 资源
     * @return 字符串内容
     */
    public static String readString(Resource resource) {
        return readString(resource, StandardCharsets.UTF_8);
    }

    /**
     * 读取资源为字符串。
     *
     * @param resource 资源
     * @param charset  字符集
     * @return 字符串内容
     */
    public static String readString(Resource resource, Charset charset) {
        assertResource(resource);
        Charset useCharset = getCharset(charset);
        try (InputStream inputStream = resource.getInputStream()) {
            return toString(inputStream, useCharset);
        } catch (IOException e) {
            throw new ResourceUtilException("读取资源文本失败，resource=" + getDescription(resource), e);
        }
    }

    /**
     * 读取资源为字符串列表，默认使用 UTF-8。
     *
     * @param resource 资源
     * @return 行列表
     */
    public static List<String> readLines(Resource resource) {
        return readLines(resource, StandardCharsets.UTF_8);
    }

    /**
     * 读取资源为字符串列表。
     *
     * @param resource 资源
     * @param charset  字符集
     * @return 行列表
     */
    public static List<String> readLines(Resource resource, Charset charset) {
        assertResource(resource);
        Charset useCharset = getCharset(charset);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), useCharset))) {
            List<String> lines = new ArrayList<String>();
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
            return lines;
        } catch (IOException e) {
            throw new ResourceUtilException("读取资源行失败，resource=" + getDescription(resource), e);
        }
    }

    /**
     * 读取资源为 Properties，默认使用 ISO-8859-1 兼容原生 Properties 规范。
     * <p>
     * 如果项目中的 properties 文件明确是 UTF-8，可使用 {@link #loadProperties(Resource, Charset)}。
     *
     * @param resource 资源
     * @return Properties
     */
    public static Properties loadProperties(Resource resource) {
        assertResource(resource);
        Properties properties = new Properties();
        try (InputStream inputStream = resource.getInputStream()) {
            properties.load(inputStream);
            return properties;
        } catch (IOException e) {
            throw new ResourceUtilException("读取 Properties 失败，resource=" + getDescription(resource), e);
        }
    }

    /**
     * 读取资源为 Properties，使用指定字符集。
     *
     * @param resource 资源
     * @param charset  字符集
     * @return Properties
     */
    public static Properties loadProperties(Resource resource, Charset charset) {
        assertResource(resource);
        Charset useCharset = getCharset(charset);
        Properties properties = new Properties();
        try (Reader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), useCharset))) {
            properties.load(reader);
            return properties;
        } catch (IOException e) {
            throw new ResourceUtilException("读取 Properties 失败，resource=" + getDescription(resource), e);
        }
    }

    /**
     * 将资源复制到输出流。
     *
     * @param resource     资源
     * @param outputStream 输出流
     */
    public static void copy(Resource resource, OutputStream outputStream) {
        assertResource(resource);
        if (outputStream == null) {
            throw new ResourceUtilException("输出流不能为空");
        }

        try (InputStream inputStream = resource.getInputStream()) {
            copy(inputStream, outputStream);
        } catch (IOException e) {
            throw new ResourceUtilException("复制资源到输出流失败，resource=" + getDescription(resource), e);
        }
    }

    /**
     * 将资源复制到文件。
     *
     * @param resource 资源
     * @param file     文件
     * @return 目标文件
     */
    public static File copyToFile(Resource resource, File file) {
        assertResource(resource);
        if (file == null) {
            throw new ResourceUtilException("目标文件不能为空");
        }

        ensureParentDir(file);
        try (InputStream inputStream = resource.getInputStream();
             OutputStream outputStream = new FileOutputStream(file)) {
            copy(inputStream, outputStream);
            return file;
        } catch (IOException e) {
            throw new ResourceUtilException("复制资源到文件失败，target=" + file.getAbsolutePath(), e);
        }
    }

    /**
     * 将资源复制到文件。
     *
     * @param resource 资源
     * @param path     文件路径
     * @return 目标 Path
     */
    public static Path copyToFile(Resource resource, Path path) {
        assertResource(resource);
        if (path == null) {
            throw new ResourceUtilException("目标路径不能为空");
        }
        try {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (InputStream inputStream = resource.getInputStream()) {
                Files.copy(inputStream, path, StandardCopyOption.REPLACE_EXISTING);
            }
            return path;
        } catch (IOException e) {
            throw new ResourceUtilException("复制资源到文件失败，target=" + path, e);
        }
    }

    /**
     * 将资源复制到临时文件。
     *
     * @param resource 资源
     * @param prefix   文件前缀
     * @param suffix   文件后缀
     * @return 临时文件
     */
    public static File copyToTempFile(Resource resource, String prefix, String suffix) {
        assertResource(resource);
        String usePrefix = hasText(prefix) ? prefix : "resource-";
        String useSuffix = hasText(suffix) ? suffix : ".tmp";
        try {
            File tempFile = File.createTempFile(usePrefix, useSuffix);
            tempFile.deleteOnExit();
            return copyToFile(resource, tempFile);
        } catch (IOException e) {
            throw new ResourceUtilException("复制资源到临时文件失败，resource=" + getDescription(resource), e);
        }
    }

    /**
     * 将 Resource 尽量转换为 File。
     * <p>
     * 仅适用于真正的文件型资源，例如 FileSystemResource、ClassPathResource(文件模式) 等。
     * 如果资源不在文件系统中，会抛出异常。
     *
     * @param resource 资源
     * @return File
     */
    public static File toFile(Resource resource) {
        assertResource(resource);
        try {
            return resource.getFile();
        } catch (IOException e) {
            throw new ResourceUtilException("当前资源不能直接转换为 File，resource=" + getDescription(resource), e);
        }
    }

    /**
     * 将 Resource 尽量转换为 Path。
     *
     * @param resource 资源
     * @return Path
     */
    public static Path toPath(Resource resource) {
        assertResource(resource);
        try {
            return resource.getFile().toPath();
        } catch (IOException e) {
            throw new ResourceUtilException("资源无法转换为 Path，resource=" + getDescription(resource), e);
        }
    }

    /**
     * 获取资源的 URL。
     *
     * @param resource 资源
     * @return URL
     */
    public static URL toUrl(Resource resource) {
        assertResource(resource);
        try {
            return resource.getURL();
        } catch (IOException e) {
            throw new ResourceUtilException("获取资源 URL 失败，resource=" + getDescription(resource), e);
        }
    }

    /**
     * 获取资源的 URI。
     *
     * @param resource 资源
     * @return URI
     */
    public static URI toUri(Resource resource) {
        assertResource(resource);
        try {
            return resource.getURI();
        } catch (IOException e) {
            throw new ResourceUtilException("获取资源 URI 失败，resource=" + getDescription(resource), e);
        }
    }

    /**
     * 获取资源描述信息。
     *
     * @param resource 资源
     * @return 描述
     */
    public static String getDescription(Resource resource) {
        if (resource == null) {
            return "null";
        }
        try {
            return resource.getDescription();
        } catch (Exception e) {
            return resource.getClass().getName();
        }
    }

    /**
     * 获取资源文件名。
     *
     * @param resource 资源
     * @return 文件名
     */
    public static String getFilename(Resource resource) {
        assertResource(resource);
        return resource.getFilename();
    }

    /**
     * 获取资源扩展名。
     *
     * @param resource 资源
     * @return 扩展名，未获取到时返回空字符串
     */
    public static String getExtension(Resource resource) {
        String filename = getFilename(resource);
        return getExtension(filename);
    }

    /**
     * 获取文件名的扩展名。
     *
     * @param filename 文件名
     * @return 扩展名，未获取到时返回空字符串
     */
    public static String getExtension(String filename) {
        if (!hasText(filename)) {
            return "";
        }
        int index = filename.lastIndexOf('.');
        if (index < 0 || index >= filename.length() - 1) {
            return "";
        }
        return filename.substring(index + 1);
    }

    /**
     * 获取不带扩展名的文件名。
     *
     * @param filename 文件名
     * @return 不带扩展名的文件名
     */
    public static String getFilenameWithoutExtension(String filename) {
        if (!hasText(filename)) {
            return filename;
        }
        int index = filename.lastIndexOf('.');
        if (index <= 0) {
            return filename;
        }
        return filename.substring(0, index);
    }

    /**
     * 判断资源是否存在。
     *
     * @param resource 资源
     * @return 是否存在
     */
    public static boolean exists(Resource resource) {
        return resource != null && resource.exists();
    }

    /**
     * 判断资源是否可读。
     *
     * @param resource 资源
     * @return 是否可读
     */
    public static boolean isReadable(Resource resource) {
        return resource != null && resource.isReadable();
    }

    /**
     * 判断资源是否打开状态。
     *
     * @param resource 资源
     * @return 是否打开
     */
    public static boolean isOpen(Resource resource) {
        return resource != null && resource.isOpen();
    }

    /**
     * 判断资源是否是文件型资源。
     *
     * @param resource 资源
     * @return 是否为文件
     */
    public static boolean isFile(Resource resource) {
        if (resource == null) {
            return false;
        }
        try {
            return resource.isFile();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 判断资源是否为 ClassPath 资源。
     *
     * @param resource 资源
     * @return 是否为 ClassPathResource
     */
    public static boolean isClassPathResource(Resource resource) {
        return resource instanceof ClassPathResource;
    }

    /**
     * 判断资源是否为 FileSystemResource。
     *
     * @param resource 资源
     * @return 是否为 FileSystemResource
     */
    public static boolean isFileSystemResource(Resource resource) {
        return resource instanceof FileSystemResource;
    }

    /**
     * 判断资源是否为 UrlResource。
     *
     * @param resource 资源
     * @return 是否为 UrlResource
     */
    public static boolean isUrlResource(Resource resource) {
        return resource instanceof UrlResource;
    }

    /**
     * 判断资源是否为空。
     *
     * @param resource 资源
     * @return true：为空
     */
    public static boolean isEmpty(Resource resource) {
        return resource == null || !resource.exists();
    }

    /**
     * 获取资源内容长度。
     *
     * @param resource 资源
     * @return 内容长度
     */
    public static long contentLength(Resource resource) {
        assertResource(resource);
        try {
            return resource.contentLength();
        } catch (IOException e) {
            throw new ResourceUtilException("获取资源长度失败，resource=" + getDescription(resource), e);
        }
    }

    /**
     * 获取资源最后修改时间。
     *
     * @param resource 资源
     * @return 最后修改时间
     */
    public static long lastModified(Resource resource) {
        assertResource(resource);
        try {
            return resource.lastModified();
        } catch (IOException e) {
            throw new ResourceUtilException("获取资源最后修改时间失败，resource=" + getDescription(resource), e);
        }
    }

    /**
     * 通过相对路径解析资源。
     *
     * @param resource     基础资源
     * @param relativePath 相对路径
     * @return 解析后的资源
     */
    public static Resource createRelative(Resource resource, String relativePath) {
        assertResource(resource);
        assertText(relativePath, "相对路径不能为空");
        try {
            return resource.createRelative(relativePath);
        } catch (IOException e) {
            throw new ResourceUtilException("创建相对资源失败，base=" + getDescription(resource) + ", relativePath=" + relativePath, e);
        }
    }

    /**
     * 将资源转换为可重复读取的字节数组资源。
     *
     * @param resource 资源
     * @return ByteArrayResource
     */
    public static Resource toRepeatableResource(Resource resource) {
        return getByteArrayResource(readBytes(resource), getFilename(resource));
    }

    /**
     * 将资源复制到字符串资源，默认 UTF-8。
     *
     * @param resource 资源
     * @return String 内容
     */
    public static String readText(Resource resource) {
        return readString(resource, StandardCharsets.UTF_8);
    }

    /**
     * 将资源转换为临时文件并返回文件对象。
     * <p>
     * 适合在需要 File 的第三方 API 中临时使用。
     *
     * @param resource 资源
     * @param prefix   临时文件前缀
     * @param suffix   临时文件后缀
     * @return 临时文件
     */
    public static File toTempFile(Resource resource, String prefix, String suffix) {
        return copyToTempFile(resource, prefix, suffix);
    }

    /**
     * 直接打开资源输入流。
     *
     * @param resource 资源
     * @return 输入流
     */
    public static InputStream getInputStream(Resource resource) {
        assertResource(resource);
        try {
            return resource.getInputStream();
        } catch (IOException e) {
            throw new ResourceUtilException("打开资源输入流失败，resource=" + getDescription(resource), e);
        }
    }

    /**
     * 计算资源是否可以安全转换为文件路径。
     *
     * @param resource 资源
     * @return true：可转为文件
     */
    public static boolean canConvertToFile(Resource resource) {
        if (resource == null) {
            return false;
        }
        try {
            resource.getFile();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * 资源是否包含通配符。
     *
     * @param location 资源位置
     * @return true：包含通配符
     */
    public static boolean isPatternLocation(String location) {
        if (!hasText(location)) {
            return false;
        }
        return location.startsWith("classpath*:") || location.indexOf('*') >= 0 || location.indexOf('?') >= 0;
    }

    /**
     * 将输入流转换为字节数组。
     *
     * @param inputStream 输入流
     * @return 字节数组
     */
    public static byte[] toByteArray(InputStream inputStream) {
        if (inputStream == null) {
            throw new ResourceUtilException("输入流不能为空");
        }
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            copy(inputStream, outputStream);
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new ResourceUtilException("输入流转字节数组失败", e);
        } finally {
            closeQuietly(outputStream);
        }
    }

    /**
     * 将字节数组转为字符串，默认 UTF-8。
     *
     * @param bytes 字节数组
     * @return 字符串
     */
    public static String toString(byte[] bytes) {
        return toString(bytes, StandardCharsets.UTF_8);
    }

    /**
     * 将字节数组转为字符串。
     *
     * @param bytes   字节数组
     * @param charset 字符集
     * @return 字符串
     */
    public static String toString(byte[] bytes, Charset charset) {
        if (bytes == null) {
            return null;
        }
        Charset useCharset = getCharset(charset);
        return new String(bytes, useCharset);
    }

    /**
     * 将输入流转为字符串，默认 UTF-8。
     *
     * @param inputStream 输入流
     * @return 字符串
     */
    public static String toString(InputStream inputStream) {
        return toString(inputStream, StandardCharsets.UTF_8);
    }

    /**
     * 将输入流转为字符串。
     *
     * @param inputStream 输入流
     * @param charset     字符集
     * @return 字符串
     */
    public static String toString(InputStream inputStream, Charset charset) {
        if (inputStream == null) {
            throw new ResourceUtilException("输入流不能为空");
        }
        Charset useCharset = getCharset(charset);
        try (Reader reader = new InputStreamReader(new BufferedInputStream(inputStream), useCharset);
             BufferedReader bufferedReader = new BufferedReader(reader);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            char[] buffer = new char[DEFAULT_BUFFER_SIZE];
            int len;
            StringBuilder sb = new StringBuilder();
            while ((len = bufferedReader.read(buffer)) != -1) {
                sb.append(buffer, 0, len);
            }
            return sb.toString();
        } catch (IOException e) {
            throw new ResourceUtilException("输入流转字符串失败", e);
        }
    }

    /**
     * 将输入流复制到输出流。
     *
     * @param inputStream  输入流
     * @param outputStream 输出流
     */
    public static void copy(InputStream inputStream, OutputStream outputStream) {
        if (inputStream == null) {
            throw new ResourceUtilException("输入流不能为空");
        }
        if (outputStream == null) {
            throw new ResourceUtilException("输出流不能为空");
        }

        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        int len;
        try {
            while ((len = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, len);
            }
            outputStream.flush();
        } catch (IOException e) {
            throw new ResourceUtilException("流复制失败", e);
        }
    }

    /**
     * 将 Reader 复制到 Writer。
     *
     * @param reader Reader
     * @param writer Writer
     */
    public static void copy(Reader reader, Writer writer) {
        if (reader == null) {
            throw new ResourceUtilException("Reader 不能为空");
        }
        if (writer == null) {
            throw new ResourceUtilException("Writer 不能为空");
        }

        char[] buffer = new char[DEFAULT_BUFFER_SIZE];
        int len;
        try {
            while ((len = reader.read(buffer)) != -1) {
                writer.write(buffer, 0, len);
            }
            writer.flush();
        } catch (IOException e) {
            throw new ResourceUtilException("字符流复制失败", e);
        }
    }

    /**
     * 读取资源并猜测内容类型。
     *
     * @param resource 资源
     * @return 内容类型，获取失败返回 null
     */
    public static String guessContentType(Resource resource) {
        if (resource == null) {
            return null;
        }

        String filename = resource.getFilename();
        if (hasText(filename)) {
            String contentType = URLConnection.guessContentTypeFromName(filename);
            if (hasText(contentType)) {
                return contentType;
            }
        }

        try (InputStream inputStream = resource.getInputStream()) {
            return URLConnection.guessContentTypeFromStream(inputStream);
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * 资源是否为可直接读取的普通文件。
     *
     * @param resource 资源
     * @return true：可作为普通文件读取
     */
    public static boolean isRegularFile(Resource resource) {
        if (resource == null) {
            return false;
        }
        if (!exists(resource)) {
            return false;
        }
        try {
            File file = resource.getFile();
            return file.exists() && file.isFile();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 资源转换为绝对文件路径字符串。
     *
     * @param resource 资源
     * @return 文件绝对路径
     */
    public static String toAbsolutePath(Resource resource) {
        return toFile(resource).getAbsolutePath();
    }

    /**
     * 资源转换为绝对 URI 字符串。
     *
     * @param resource 资源
     * @return URI 字符串
     */
    public static String toUriString(Resource resource) {
        return toUri(resource).toString();
    }

    /**
     * 将资源保存到指定目录，文件名默认取资源原始文件名。
     *
     * @param resource 资源
     * @param dir      目标目录
     * @return 保存后的文件
     */
    public static File saveToDirectory(Resource resource, File dir) {
        assertResource(resource);
        if (dir == null) {
            throw new ResourceUtilException("目标目录不能为空");
        }
        if (!dir.exists() && !dir.mkdirs()) {
            throw new ResourceUtilException("创建目标目录失败，dir=" + dir.getAbsolutePath());
        }
        if (!dir.isDirectory()) {
            throw new ResourceUtilException("目标不是目录，dir=" + dir.getAbsolutePath());
        }

        String filename = getFilename(resource);
        if (!hasText(filename)) {
            filename = "resource-" + System.currentTimeMillis();
        }

        File target = new File(dir, filename);
        return copyToFile(resource, target);
    }

    /**
     * 将资源复制到指定目录，文件名默认取资源原始文件名。
     *
     * @param resource 资源
     * @param dir      目标目录
     * @return 保存后的 Path
     */
    public static Path saveToDirectory(Resource resource, Path dir) {
        assertResource(resource);
        if (dir == null) {
            throw new ResourceUtilException("目标目录不能为空");
        }
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new ResourceUtilException("创建目标目录失败，dir=" + dir, e);
        }

        String filename = getFilename(resource);
        if (!hasText(filename)) {
            filename = "resource-" + System.currentTimeMillis();
        }

        return copyToFile(resource, dir.resolve(filename));
    }

    /**
     * 安全获取资源内容长度，失败返回 -1。
     *
     * @param resource 资源
     * @return 长度
     */
    public static long safeContentLength(Resource resource) {
        try {
            return contentLength(resource);
        } catch (Exception e) {
            return -1L;
        }
    }

    /**
     * 安全获取资源最后修改时间，失败返回 -1。
     *
     * @param resource 资源
     * @return 最后修改时间
     */
    public static long safeLastModified(Resource resource) {
        try {
            return lastModified(resource);
        } catch (Exception e) {
            return -1L;
        }
    }

    /**
     * 关闭资源。
     *
     * @param closeable 关闭对象
     */
    public static void closeQuietly(Closeable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (IOException e) {
            log.debug("关闭资源失败，忽略，type={}", closeable.getClass().getName(), e);
        }
    }

    /**
     * 兼容性地将资源转为原始 Resource 类型。
     *
     * @param resource 资源
     * @return 资源本身
     */
    public static Resource identity(Resource resource) {
        return resource;
    }

    /**
     * 校验资源不能为空且必须存在。
     *
     * @param resource 资源
     */
    public static void assertExists(Resource resource) {
        assertResource(resource);
        if (!resource.exists()) {
            throw new ResourceUtilException("资源不存在，resource=" + getDescription(resource));
        }
    }

    /**
     * 获取资源内容类型，若无法判断则返回默认值。
     *
     * @param resource     资源
     * @param defaultValue 默认值
     * @return 内容类型
     */
    public static String getContentType(Resource resource, String defaultValue) {
        String contentType = guessContentType(resource);
        if (hasText(contentType)) {
            return contentType;
        }
        return defaultValue;
    }

    /**
     * 判断字符串是否有内容。
     *
     * @param text 文本
     * @return true：有内容
     */
    private static boolean hasText(String text) {
        return text != null && text.trim().length() > 0;
    }

    /**
     * 校验字符串不能为空。
     *
     * @param text    文本
     * @param message 异常信息
     */
    private static void assertText(String text, String message) {
        if (!hasText(text)) {
            throw new ResourceUtilException(message);
        }
    }

    /**
     * 校验资源对象不能为空。
     *
     * @param resource 资源
     */
    private static void assertResource(Resource resource) {
        if (resource == null) {
            throw new ResourceUtilException("资源不能为空");
        }
    }

    /**
     * 规范化字符集，避免外部传入 null。
     *
     * @param charset 字符集
     * @return 可用字符集
     */
    private static Charset getCharset(Charset charset) {
        return charset == null ? StandardCharsets.UTF_8 : charset;
    }

    /**
     * 确保父目录存在。
     *
     * @param file 文件
     */
    private static void ensureParentDir(File file) {
        File parent = file.getAbsoluteFile().getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new ResourceUtilException("创建父目录失败，parent=" + parent.getAbsolutePath());
        }
    }

    /**
     * 去除 classpath: 前缀并规范化路径。
     *
     * @param path 路径
     * @return 规范化后的路径
     */
    private static String normalizeClassPath(String path) {
        String result = path.trim();
        if (result.startsWith(ResourceUtils.CLASSPATH_URL_PREFIX)) {
            result = result.substring(ResourceUtils.CLASSPATH_URL_PREFIX.length());
        }
        while (result.startsWith("/")) {
            result = result.substring(1);
        }
        return result;
    }

    /**
     * 判断是否是通配符路径。
     *
     * @param location 资源位置
     * @return true：包含通配符
     */
    private static boolean isPatternLocationInternal(String location) {
        return location != null && (location.startsWith("classpath*:") || location.indexOf('*') >= 0 || location.indexOf('?') >= 0);
    }

    /**
     * 自定义运行时异常，统一资源操作失败的异常包装。
     */
    public static class ResourceUtilException extends RuntimeException {

        public ResourceUtilException(String message) {
            super(message);
        }

        public ResourceUtilException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}